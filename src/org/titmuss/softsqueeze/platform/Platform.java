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

package org.titmuss.softsqueeze.platform;

import java.applet.AppletContext;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;

import org.apache.log4j.Logger;
import org.titmuss.softsqueeze.Applet;
import org.titmuss.softsqueeze.util.Util;

import com.Ostermiller.util.Browser;

/**
 * 
 * @author richard
 */
public class Platform {
	protected static Logger logger = Logger.getLogger("platform");

	public static final boolean JRE_1_3_PLUS;

	public static final boolean JRE_1_4_PLUS;

	public static final boolean JRE_1_5_PLUS;

	public static final boolean JRE_1_6_PLUS;

	private static Platform instance;

	private static BasicService basicService;

	private static AppletContext appletContext;

	private byte[] macAddress;

	/**
	 * Detect JVM version see:
	 * http://www.javaworld.com/javaqa/2003-05/02-qa-0523-version.html
	 */
	static {
		boolean temp = false;
		try {
			StrictMath.abs(1.0);
			temp = true;
		} catch (Error ignore) {
		}
		JRE_1_3_PLUS = temp;

		if (temp) {
			temp = false;
			try {
				" ".subSequence(0, 0);
				temp = true;
			} catch (NoSuchMethodError ignore) {
			}
		}
		JRE_1_4_PLUS = temp;

		if (temp) {
			temp = false;
			try {
				" ".codePointAt(0);
				temp = true;
			} catch (NoSuchMethodError ignore) {
			}
		}
		JRE_1_5_PLUS = temp;
		
		if (temp) {
			temp = false;
			try {
				" ".isEmpty();
				temp = true;
			} catch (NoSuchMethodError ignore) {
			}
		}
		JRE_1_6_PLUS = temp;
	}

	/**
	 * Initialise platform class.
	 */
	public static Platform init() {
		return init(null);
	}

	/**
	 * Initialise platform class in Applet.
	 */
	public static Platform init(Applet applet) {
		if (instance != null)
			return instance;

		String os = System.getProperty("os.name");
		if (!Platform.JRE_1_4_PLUS) {
			/*
			 * sub-classes use regex which is not supported in jre1.3. I don't
			 * have the time to rewrite this at the moment!
			 */
			instance = new Platform();
		} else if (os.indexOf("Windows") != -1) {
			instance = new WindowsPlatform();
		} else if (os.indexOf("Linux") != -1) {
			instance = new LinuxPlatform();
		} else if (os.indexOf("Mac OS X") != -1) {
			instance = new MacPlatform();
		} else {
			instance = new Platform();
		}

		if (applet != null) { // applet?
			appletContext = applet.getAppletContext();
		} else {
			try {
				// jnlp application?
				basicService = (BasicService) ServiceManager
						.lookup("javax.jnlp.BasicService");
			} catch (UnavailableServiceException e) {
				logger.debug("JNLP basic service unavailable");
				// jnlp not supported, use ostermiller-utils
				Browser.init();
			}
		}

		logger.debug("os='" + os + "' platform=" + instance);
		return instance;
	}

	/**
	 * Return the hosts mac address.
	 */
	public static byte[] getMacAddress() {
		if (instance == null)
			throw new IllegalArgumentException("Platform not initialised");
		return instance._getMacAddress();
	}

	/**
	 * Return a mac address as a string.
	 */
	public static String macAddressToString(byte macAddress[]) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < macAddress.length; i++) {
			buf.append(Integer.toString((macAddress[i] >> 4) & 0x0F, 16));
			buf.append(Integer.toString(macAddress[i] & 0x0F, 16));
			if (i < macAddress.length - 1)
				buf.append(":");
		}
		return buf.toString();
	}

	/**
	 * Parse a string mac address into a byte array.
	 */
	public static byte[] parseMacAddress(String mac) {
		byte macAddress[] = new byte[6];

		String hex[] = Util.split(mac, ":-");
		for (int i = 0; i < Math.min(hex.length, macAddress.length); i++) {
			macAddress[i] = (byte) Integer.parseInt(hex[i], 16);
		}
		return macAddress;
	}

	public static boolean displayUrl(URL url) {
		return Platform._displayUrl(url);
	}

	public static boolean displayUrl(String url) {
		try {
			return displayUrl(new URL(url));
		} catch (MalformedURLException e) {
			logger.warn("Malformed URL: " + url);
			return false;
		}
	}

	protected Platform() {
	}

	protected byte[] _getMacAddress() {
		if (macAddress != null)
			return macAddress;

		/*
		 * jre1.3 does not support the Preferences class, so it cannot store the
		 * random mac addresses. To keep the player mac the same generate a mac
		 * based on the ip address.
		 */
		if (!Platform.JRE_1_4_PLUS) {
			try {
				InetAddress localaddr = InetAddress.getLocalHost();
				byte ipAddress[] = localaddr.getAddress();
				macAddress = new byte[6];
				for (int i = 0; i < 4; i++)
					macAddress[i + 2] = ipAddress[i];
				logger.debug("using ipaddress as mac address: "
						+ macAddressToString(macAddress));
				return macAddress;
			} catch (UnknownHostException e) {
			}
		}

		/*
		 * new random mac address. if this clashes with a real mac, then we are
		 * really unlucky!
		 */
		macAddress = new byte[6];
		for (int i = 0; i < macAddress.length; i++) {
			macAddress[i] = (byte) (Math.random() * 255);
		}
		logger.debug("using random mac address: "
				+ macAddressToString(macAddress));

		return macAddress;
	}

	/**
	 * Returns the host mac address.
	 */
	protected String _getHostMacAddress() {
		return null;
	}

	/**
	 * Display a URL on system browser.
	 */
	protected static boolean _displayUrl(URL url) {
		try {
			if (appletContext != null) { // applet?
				logger.debug("using applet showDocument");
				appletContext.showDocument(url, "softsqueeze");
				return true;
			}

			if (basicService != null) { // jnlp application?
				logger.debug("using jnlp showDocument");
				return basicService.showDocument(url);
			}

			logger.debug("using ostermiller-utils displayURL");
			Browser.displayURL(url.toString());
			return true;
		} catch (IOException e) {
			logger.error("Error displaying url=" + url, e);
		}
		return false;
	}

}