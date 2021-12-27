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

package org.titmuss.softsqueeze;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.InetAddress;

import javax.swing.JApplet;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.titmuss.softsqueeze.audio.AudioDecoder;
import org.titmuss.softsqueeze.audio.Player;
import org.titmuss.softsqueeze.config.Config;
import org.titmuss.softsqueeze.config.ConfigDialog;
import org.titmuss.softsqueeze.config.ConfigListener;
import org.titmuss.softsqueeze.config.ConfigPopup;
import org.titmuss.softsqueeze.display.LcdDisplay;
import org.titmuss.softsqueeze.net.CliConnection;
import org.titmuss.softsqueeze.net.Protocol;
import org.titmuss.softsqueeze.net.ProtocolListener;
import org.titmuss.softsqueeze.net.SSHTunnel;
import org.titmuss.softsqueeze.platform.Platform;
import org.titmuss.softsqueeze.skin.Skin;
import org.titmuss.softsqueeze.util.Util;

import com.l2fprod.gui.nativeskin.NativeSkin;

/**
 * 
 * @author richard
 */
public class Softsqueeze implements ConfigListener, ProtocolListener {
	private static final Logger logger = Logger.getLogger("softsqueeze");

	private static final String MIN_SLIMSERVER_VERSION = "7.9";

	private static final String MAX_SLIMSERVER_VERSION = "9.9";
	
	private boolean isSlimserverCheck = false;
	
	private JApplet applet;

	private Protocol wire;

	private Player player;
	
	private CliConnection cli;

	private LcdDisplay lcddisplay;

	private ConfigDialog configDialog;
	
	private Skin skin;

	private boolean skinLock = false;
	
	private int firmware;

	private final static int INIT = 0;

	private final static int CONNECTED = 1;

	private final static int DISCONNECTED = 2;

	private int state = INIT;

	private SSHTunnel sshTunnel;

	static {
		// Install the native look and feel
		try {
		    if (!GraphicsEnvironment.isHeadless()) {		    
		        String nativeLF = UIManager.getSystemLookAndFeelClassName();
		        UIManager.setLookAndFeel(nativeLF);
		    }
		} catch (InstantiationException e) {
		} catch (ClassNotFoundException e) {
		} catch (UnsupportedLookAndFeelException e) {
		} catch (IllegalAccessException e) {
		}
	}
	
	/**
	 * Creates a new instance of SoftSqueeze.
	 */
	public Softsqueeze() {
		this(null);
	}

	/**
	 * Creates a new instance of SoftSqueeze.
	 */
	public Softsqueeze(JApplet applet) {
		this.applet = applet;

		try {
			/* enable debug logging */
			String logProperty = Config.getProperty("log");
			String logNames[] = Util.split(logProperty, ",");
			for (int i = 0; i < logNames.length; i++) {
				Logger l = Logger.getLogger(logNames[i]);
				l.setLevel((Level) Level.DEBUG);
			}

			/*
			String os = System.getProperty("os.name");
			String javaVersion = System.getProperty("java.version");
			if (!Platform.JRE_1_5_PLUS
					&& (os.indexOf("Windows") != -1 || os.indexOf("Linux") != -1))
				ConfigDialog.showOnceDialog(
						"checkjava5.0",
						"You are running SoftSqueeze using Java "+javaVersion+". The audio quality will be improved if you upgrade\n to Java 5.0. This can be downloaded from http://java.sun.com/downloads",
						"Please upgrade to Java 5.0");
			*/
			
			boolean hasMP3Plugin = AudioDecoder.isMP3PluginInstalled(); 			
			if (!Config.getBooleanProperty("has.mp3plugin") && hasMP3Plugin)
			    Config.putProperty("audio.mp3decoder", AudioDecoder.getDefaultMP3Decoder());		
			Config.putBooleanProperty("has.mp3plugin", hasMP3Plugin);

			wire = new Protocol();
			cli = new CliConnection();
			player = new Player(this);

			if (!Config.isHeadless()) {
				// Ensure jawt is loaded for nativeskinlib
				if (NativeSkin.isSupported()) {
					System.loadLibrary("jawt");
				}
				
				lcddisplay = new LcdDisplay(this);
			    	openSkins();
			}

			//wire.addProtocolListener("i2cc", this); FIXME
			wire.addProtocolListener("vers", this);
			wire.addProtocolListener("serv", this);
			wire.sendDiscoveryRequest(2, 23, Platform.parseMacAddress(Config
					.getProperty("macaddress")));

			// Ask for settings if we have no IP address
			if (Config.getSlimServerAddress().equals("")) {
			    if (Config.isHeadless()) {
			        System.err.println("Please include the Squeezebox Server address on the command line. e.g. -Dslimserver=localhost");
			        System.exit(-1);
			    }
			    else {
			        openConfigDialog();
			    }
			}
			Config.addConfigListener(this);

			connect();
		} catch (Exception e) {
			logger.warn("Exception", e);
		}
	}

	/**
	 * Returns the slimproto instance.
	 */
	public Protocol getProtocol() {
		return wire;
	}

	/**
	 * Returns the vf display instance.
	 */
	public LcdDisplay getLcdDisplay() {
		return lcddisplay;
	}

	/**
	 * @return the command line interface instace
	 */
	public CliConnection getCLI() {
		return cli;
	}


	/**
	 * Returns the applet object, or null if running in an application.
	 */
	public JApplet getApplet() {
		return applet;
	}

	/**
	 * Open the configuration dialog
	 */
	public void openConfigDialog() {
		if (configDialog == null)
		    configDialog = new ConfigDialog();		
		configDialog.showDialog(skin);
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.slim.softsqueeze.ConfigListener#configSet(java.lang.String,
	 *      java.lang.String)
	 */
	public void configSet(String key, String value) {
		if (key.equals("slimserver") || key.startsWith("ssh")
				|| key.endsWith("port") || key.equals("displayemulation")) {
			connect();
		}
		else if (key.startsWith("skin")) {
			openSkins();
			connect();
		}
	}

	/**
	 * Create/update visible skins
	 */
	public synchronized void openSkins() {
		String skinName = Config.getProperty("skin");
		
		if (skinLock)
		    return;
		
		if (skin != null) {
		    skin.dispose();
		    skin = null;
		}

		if (skinName != null && !Config.isHeadless()) {
		    skinLock = true;
		    skin = new Skin(this, skinName);
		    skinLock = false;

		    if (skin == null)
				logger.error("Cannot open skin " + skinName);		    
		}
	}

	/**
	 * (Re)Connect to the slim server.
	 */
	public void connect() {
		if (state == CONNECTED) {
			wire.sendBye();
		}

		if (sshTunnel != null) {
			sshTunnel.disconnect();
			sshTunnel = null;
		}

		if (Config.getSlimServerAddress() == null 
			|| Config.getSlimServerAddress().length() == 0)
			return;
		
		try {
			if (Config.useSSH()) {
				setDisplayText("Please wait.", "SSH connecting.");
				sshTunnel = new SSHTunnel();
				sshTunnel.connect();
			}

			setDisplayText("Please wait.", "Connecting to Slim Server.");
			InetAddress addr = InetAddress.getByName(Config
					.getSlimServerAddress());
			wire.connect(addr, Config.getSlimProtoPort());			
			//// cli.connect(addr, 9090); // FIXME configurable port
		} catch (Exception e) {
		    setDisplayText("Connection failed.", "Please check server settings.");
		    logger.warn("Exception in connect", e);
		}
	}

	public void setDisplayText(String s1, String s2) {
	    if (lcddisplay == null) {
	        System.out.println(s1+": "+s2);
	        return;
	    }
	    
	    lcddisplay.setText(s1, s2);
	}
	
	/**
	 * Exit the soft squeeze player
	 */
	public void exit() {
		/* close server connection */
		if (state == CONNECTED) {
			sendIR(1, 1, 0x0000f700); /* power off */
			wire.sendBye();
		}

		if (sshTunnel != null) {
			sshTunnel.disconnect();
			sshTunnel = null;
		}

		state = DISCONNECTED;

		/* close skins */
		skin.dispose();

		/* exit, unless in applet */
		if (applet == null)
			System.exit(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.slim.softsqueeze.ProtocolListener#slimprotoCmd(java.lang.String,
	 *      byte[], int, int)
	 */
	public void slimprotoCmd(String cmd, byte[] buf, int off, int len) {
		/*
	    if (cmd.equals("i2cc")) {
			boolean setVolume = false;
			for (int i = off; i < len;) {
				if (buf[i++] != 's') {
					logger.warn("Unrecognised i2c operation "
							+ new String(buf, i, 1));
					while (i < len && buf[i++] != 's')
						;
					continue;
				}
			}
		}
		else
		*/ 
		if (cmd.equals("vers") && !isSlimserverCheck) {
			String v = new String(buf, off, len).trim();
			Config.putProperty("serverversion", v);
			
			int min = compareVersion(MIN_SLIMSERVER_VERSION, v);
			if (min > 0) {
			    ConfigPopup.showErrorDialog("You have connected to Squeezebox Server "+v+", but this version of SoftSqueeze\n requires Squeezebox Server "+MIN_SLIMSERVER_VERSION+" or greater. Please upgrade your Squeezebox Server.", "Please upgrade Squeezebox Server");
			    openConfigDialog();
			    return;
			}
			int max = compareVersion(MAX_SLIMSERVER_VERSION, v);
			if (max < 0 ) {
				ConfigPopup.showOnceDialog(
						"checkslimserver"+MAX_SLIMSERVER_VERSION,
						"",
						"This version of SoftSqueeze is optimised for Squeezebox Server "+MAX_SLIMSERVER_VERSION+" but you are running Squeezebox Server "+v+".\n",
						"Some problems may occur.");

			}
			isSlimserverCheck = true;
		}
		else if (cmd.equals("serv")) {
		    int addr = Protocol.unpackN4(buf, off);
		    
		    switch (addr) {
		    case 0:
			    Config.setSlimNetworkServer(null);
		        break;

		    case 1:
			    Config.setSlimNetworkServer("service.us.squeezenetwork.com");
		        break;
		        
		    default:
		        StringBuffer ipaddrBuf = new StringBuffer();
		    	ipaddrBuf.append(Integer.toString( (addr >> 24) & 0xFF));
		    	ipaddrBuf.append(".");
		    	ipaddrBuf.append(Integer.toString( (addr >> 24) & 0xFF));
		    	ipaddrBuf.append(".");
		    	ipaddrBuf.append(Integer.toString( (addr >> 24) & 0xFF));
		    	ipaddrBuf.append(".");
		    	ipaddrBuf.append(Integer.toString( (addr >> 24) & 0xFF));
		    	
		    	Config.setSlimNetworkServer(ipaddrBuf.toString());
		    }
		    
		    try {
                player.disconnect();
            } catch (IOException e) {
            }
		    connect();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.slim.softsqueeze.ProtocolListener#slimprotoConnected()
	 */
	public void slimprotoConnected() {
		firmware = Config.getIntegerProperty("firmwareversion");
		int deviceid = Config.getIntegerProperty("deviceid");
		if (skin != null)
			deviceid = skin.getDeviceId();
		String macAddress = Config.getProperty("macaddress");
		boolean isGraphics = (lcddisplay != null && lcddisplay.getModel() != LcdDisplay.DISPLAY_NORITAKE); 
		wire.sendHELO(deviceid, firmware, Platform.parseMacAddress(macAddress),
				isGraphics, (state != INIT));
		state = CONNECTED;
		
		sendIR(1, 1, 0x0000f701); /* power on */
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.slim.softsqueeze.ProtocolListener#slimprotoDisconnected()
	 */
	public void slimprotoDisconnected() {
		logger.warn("Lost contact with Slim Server");
		state = DISCONNECTED;
	}

	/**
	 * Send a IR command to the server
	 */
	public void sendIR(int format, int noBits, int irCode) {
		if (state != CONNECTED)
			return;

		wire.sendIR(format, noBits, irCode);
	}
	
	public void sendButton(int code) {
		if (state != CONNECTED)
			return;

		wire.sendButton(code);
	}

	/**
	 * Send a KNOB command to the server
	 * @param sync 
	 */
	public void sendKnob(int position, int sync) {
		if (state != CONNECTED)
			return;

		wire.sendKnob(position, sync);
	}
	
	/**
	 * Send an ANIC command to the server
	 */
	public void sendANIC() {
	    wire.sendANIC();
	}
	
	private static int compareVersion(String v1, String v2) {
		String s1[] = Util.split(v1, ".ab");
		String s2[] = Util.split(v2, ".ab");
		
		for (int i = 0; i < Math.min(s1.length,s2.length); i++) {
			int cmp = s1[i].compareTo(s2[i]); 
			if (cmp == 0)
				continue;
			return cmp;
		}
		return (s1.length <= s2.length) ? 0 : 1 ;
	}

	
	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		Platform.init();
		Config.init();

		new Softsqueeze();
	}
}
