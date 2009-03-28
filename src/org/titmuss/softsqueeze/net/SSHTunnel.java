/*
 *   SoftSqueeze Copyright (c) 2004 Richard Titmuss
 *
 *   This file is part of SoftSqueeze.
 *
 *   SoftSqueeze is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   SoftSqueeze is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with SoftSqueeze; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package org.titmuss.softsqueeze.net;

import java.io.File;
import java.io.IOException;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.apache.log4j.Logger;
import org.titmuss.softsqueeze.config.Config;
import org.titmuss.softsqueeze.skin.OptionPanel;
import org.titmuss.softsqueeze.util.Util;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.ProxySOCKS5;
import com.jcraft.jsch.Session;

public class SSHTunnel {
	private static Logger logger = Logger.getLogger("ssh");

	private JSch jsch = null;

	private Session session1;

	private Session session2;

	public SSHTunnel() {
		try {
			jsch = new JSch();

			File dotSsh = new File(System.getProperty("user.home"), ".ssh");
			if (!dotSsh.exists())
				dotSsh.mkdirs();
			File knownHosts = new File(dotSsh, "known_hosts");
			jsch.setKnownHosts(knownHosts.getAbsolutePath());

			String privatekey = Config.getProperty("sshprivatekey");
			if (privatekey.length() > 0) {
				logger.debug("Setting ssh identity " + privatekey);
				jsch.addIdentity(privatekey);
			}
		} catch (JSchException e) {
			logger.error("Cannot initialise jsch", e);
		}
	}

	public void connect() throws IOException {
		try {
			if (session1 != null && session1.isConnected())
				return;

			SshUserInfo userInfo = new SshUserInfo();
			String slimserver = Config.getProperty("slimserver");
			String sshserver = Config.getProperty("sshserver");
			int port = Config.getIntegerProperty("sshport");
			String forward = Config.getProperty("sshforwardports");

			String sshProxy = Config.getSSHProxy();
			String sshProxyHost = Config.getSSHProxyHost();
			int sshProxyPort = Config.getSSHProxyPort();

			if (slimserver.equals(sshserver))
			    slimserver = "127.0.0.1";

			String username = userInfo.getUsername();
			if (username == null) {
				userInfo.promptUsernameAndPassword("Username and password for "
						+ slimserver);
				username = userInfo.getUsername();
			}

			/*
			 * A bug in jsch makes the control channel block when two ports are
			 * forwarded in a single session. As a workaround we open two
			 * sessions for now ...
			 * 
			 * and also open 9001 to support the web browser
			 */

			int slimport = Config.getSlimProtoPort();
			int httpport = Config.getServerHttpPort();
			int webport = Config.getServerWebPort();
			
			session1 = jsch.getSession(username, sshserver, port);
			session1.setUserInfo(userInfo);
			if (sshProxy.equals("Socks 5")) {
				session1.setProxy(new ProxySOCKS5(sshProxyHost, sshProxyPort));
			} else if (sshProxy.equals("HTTP")) {
				session1.setProxy(new ProxyHTTP(sshProxyHost, sshProxyPort));
			}

			session1.connect();
			session1.setPortForwardingL(slimport, slimserver, slimport);
			session1.setPortForwardingL(webport, slimserver, httpport);

			// forward addition ports with the control channel
			String forwards[] = Util.split(forward, ",");
			for (int i = 0; i < forwards.length; i++) {
				int fport = Integer.parseInt(forwards[i]);
				if (fport == slimport || fport == httpport)
					continue;
				session1.setPortForwardingL(fport, slimserver, fport);
			}

			session2 = jsch.getSession(username, sshserver, port);
			session2.setUserInfo(userInfo);			
			userInfo.passwordSet = true; // Password is correct
			if (sshProxy.equals("Socks 5")) {
				session2.setProxy(new ProxySOCKS5(sshProxyHost, sshProxyPort));
			} else if (sshProxy.equals("HTTP")) {
			    session2.setProxy(new ProxyHTTP(sshProxyHost, sshProxyPort));
			}

			session2.connect();
			session2.setPortForwardingL(httpport, slimserver, httpport);
		} catch (JSchException e) {
			logger.error("Cannot connect ssh tunnel", e);
			throw new IOException(e.getMessage());
		}
	}

	public void disconnect() {
		if (session1 != null) {
			session1.disconnect();
			session1 = null;
		}
		if (session2 != null) {
			session2.disconnect();
			session2 = null;
		}
	}

	private class SshUserInfo implements com.jcraft.jsch.UserInfo {
		String username;

		char password[];

		char passphrase[];

		boolean passwordSet = false;

		JTextField usernameField = new JTextField(20);

		JPasswordField passwordField = new JPasswordField(20);

		JPasswordField passphraseField = new JPasswordField(20);

		JCheckBox storePassword = new JCheckBox();

		public SshUserInfo() {
			username = Config.getProperty("sshusername");
			if (username != null && username.equals(""))
				username = null;

			password = Config.getPassword("sshpassword");
			if (password != null && password.equals(""))
				password = null;
			if (password != null)
				passwordSet = true;
		}

		public String getUsername() {
			return username;
		}

		public String getPassword() {
			return new String(password);
		}

		public String getPassphrase() {
			return new String(passphrase);
		}

		public void showMessage(String message) {
			JOptionPane.showMessageDialog(null, "SoftSqueeze: " + message);
		}

		public boolean promptYesNo(String str) {
			Object[] options = { "yes", "no" };
			int result = JOptionPane.showOptionDialog(null, str,
					"SoftSqueeze: Warning", JOptionPane.DEFAULT_OPTION,
					JOptionPane.WARNING_MESSAGE, null, options, options[0]);
			return result == 0;
		}

		public boolean promptPassphrase(String message) {
			OptionPanel ob = new OptionPanel();
			ob.add(new JLabel("Passphrase"), passphraseField);

			int result = JOptionPane.showConfirmDialog(null, ob,
					"SoftSqueeze: " + message, JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION) {
				passphrase = passphraseField.getPassword();
				return true;
			} else {
				return false;
			}
		}

		public boolean promptUsernameAndPassword(String message) {
			OptionPanel ob = new OptionPanel();
			ob.add(new JLabel("Username"), usernameField);
			ob.add(new JLabel("Password"), passwordField);
			if (Config.getBooleanProperty("enablestorepassword"))
				ob.add(new JLabel("Store password"), storePassword);

			int result = JOptionPane.showConfirmDialog(null, ob,
					"SoftSqueeze: " + message, JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION) {
				username = usernameField.getText();
				password = passwordField.getPassword();

				Config.putProperty("sshusername", username);
				if (storePassword.isSelected())
					Config.putPassword("sshpassword", password);
				else
					Config.putPassword("sshpassword", null);

				passwordSet = true;
				return true;
			} else {
				return false;
			}
		}

		public boolean promptPassword(String message) {
			if (passwordSet) {
				passwordSet = false;
				return true;
			}

			OptionPanel ob = new OptionPanel();
			ob.add(new JLabel("Password"), passwordField);
			if (Config.getBooleanProperty("enablestorepassword"))
				ob.add(new JLabel("Store password?"), storePassword);

			int result = JOptionPane.showConfirmDialog(null, ob,
					"SoftSqueeze: " + message, JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION) {
				password = passwordField.getPassword();
				if (storePassword.isSelected())
					Config.putPassword("sshpassword", password);
				else
					Config.putPassword("sshpassword", null);

				return true;
			} else {
				return false;
			}
		}
	}

}