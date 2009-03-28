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

package org.titmuss.softsqueeze.config;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.prefs.Preferences;

import org.apache.log4j.Logger;
import org.titmuss.softsqueeze.Applet;
import org.titmuss.softsqueeze.Softsqueeze;
import org.titmuss.softsqueeze.audio.AudioDecoder;
import org.titmuss.softsqueeze.audio.AudioMixer;
import org.titmuss.softsqueeze.platform.Platform;
import org.titmuss.softsqueeze.util.Util;



/**
 * 
 * @author richard
 */
public class Config {
	private static Logger logger = Logger.getLogger("config");

	private final static String CONFIG_VERSION = "5";

	private static char crypt[] = "qatu63hoxxoh0zrhd0djf5zkc6e38cpw".toCharArray();

	private static Config instance;

	private static ArrayList listeners = new ArrayList();
	
	private Applet applet;

	private Preferences prefs = null;

	private HashMap config = new HashMap();

	private HashMap defval = new HashMap();
	
	private static String slimNetwork = null;
	
	
	public static Config init() {
		return init(null);
	}

	public static Config init(Applet applet) {
		if (instance == null)
			instance = new Config(applet);
		return instance;
	}

	public static void addConfigListener(ConfigListener listener) {
		listeners.add(listener);
	}

	public static void removeConfigListener(ConfigListener listener) {
		listeners.remove(listener);
	}
	
	public static void setSlimNetworkServer(String slimNetworkAddr) {
	    slimNetwork = slimNetworkAddr;
	}
	
	public static boolean useSSH() {
		if (slimNetwork != null)
	        return false;
		return getBooleanProperty("sshtunnel");
	}
	
	public static String getSlimServerAddress() {
	    if (slimNetwork != null)
	        return slimNetwork;	   
	    if (getBooleanProperty("sshtunnel"))
			return "127.0.0.1";
	    return getProperty("slimserver");
	}

	public static String getSSHProxy() {
	    if (slimNetwork != null)
	        return "None";
	    return getProperty("sshproxy");
	}
	
	public static String getSSHProxyHost() {
	    return getProperty("sshproxyhost");
	}
	
	public static int getSSHProxyPort() {
	    return getIntegerProperty("sshproxyport");
	}
	
	/**
	 * @return the port for http streamed music
	 */
	public static int getServerHttpPort() {
	    if (slimNetwork != null)
	        return 9000;
		return getIntegerProperty("httpport");
	}

	/**
	 * When working over the ssh tunnel different ports are needed
	 * for browsing and streaming. This works around a blocking bug
	 * in the jsch library.
	 * 
	 * @return the port for web browsing and music search
	 */
	public static int getServerWebPort() {
	    if (slimNetwork != null)
	        return 9000;
	    if (Config.getBooleanProperty("sshtunnel"))
	        return getIntegerProperty("httpport") + 1;
	    return getIntegerProperty("httpport");
	}

	public static int getSlimProtoPort() {
	    if (slimNetwork != null)
	        return 3483;
		return getIntegerProperty("slimport");
	}
	
	public static char[] getPassword(String key) {
		try {
			String c[] = Util.split(getProperty(key), ",");
			char password[] = new char[c.length];
			for (int i=0; i<c.length; i++) {
				password[i] = (char)Integer.parseInt(c[i], 16);
			}
			return crypt(password);
		}
		catch (NumberFormatException e) {
			logger.error(e);
			return new char[0];
		}
	}
	
	public static void putPassword(String key, char password[]) {
		if (password == null) {
			putProperty(key, "");
			return;
		}
		
		password = crypt(password);
		StringBuffer b = new StringBuffer();
		for (int i=0; i<password.length; i++) {
			if (i > 0)
				b.append(",");
			b.append(Integer.toString(password[i], 16));
		}
		
		putProperty(key, b.toString());
	}
	
	private static char[] crypt(char password[]) {
		for (int i=0, j=0; i<password.length; i++) {
			password[i] = (char)(password[i] ^= crypt[j++]);
			if (j >= crypt.length)
				j=0;
		}
		return password;
	}
	
	public static boolean isHeadless() {
	    if (GraphicsEnvironment.isHeadless())
	        return true;
	    
	    // 'skins' backwards compatibility
	    String skin = Config.getProperty("skins");
	    if (skin == null) {	    
	    	skin = Config.getProperty("skin");
	    }
	    
	    return skin != null && skin.equalsIgnoreCase("headless");
	}

	public static String getProperty(String key) {
		if (instance == null)
			throw new IllegalStateException("Config is not initialised");

		return instance._getProperty(key);
	}

	public static boolean getBooleanProperty(String key) {
		if (instance == null)
			throw new IllegalStateException("Config is not initialised");
		return instance._getBooleanProperty(key);
	}

	public static int getIntegerProperty(String key) {
		if (instance == null)
			throw new IllegalStateException("Config is not initialised");
		return instance._getIntegerProperty(key);
	}

	public static void putProperty(String key, String val) {
		if (instance == null)
			throw new IllegalStateException("Config is not initialised");
		instance._putProperty(key, val);
	}

	public static void putBooleanProperty(String key, boolean val) {
		if (instance == null)
			throw new IllegalStateException("Config is not initialised");
		instance._putBooleanProperty(key, val);
	}

	public static void putIntegerProperty(String key, int val) {
		if (instance == null)
			throw new IllegalStateException("Config is not initialised");
		instance._putIntegerProperty(key, val);
	}

	public static void resetProperties() {
		if (instance == null)
			throw new IllegalStateException("Config is not initialised");
		
		instance._loadProp(true);
	}
	
	private Config(Applet applet) {
		this.applet = applet;

		boolean doReset = false;
		
		if (Platform.JRE_1_4_PLUS) {
			prefs = Preferences.userNodeForPackage(Softsqueeze.class);

			/* reset softsqueeze preferences */
			String reset = System.getProperty("deletePreferences", "false");
			if (reset.equalsIgnoreCase("true"))
				doReset = true;
			if (!prefs.get("version", "0").equals(CONFIG_VERSION))
				doReset = true; // reset during upgrade
		}

		_loadProp(doReset);
	}
	
	private void _loadProp(boolean reset) {
		String macaddress = _getProperty("macaddress");		
	    if (reset) {
			logger.info("Reseting user preferences");

			try {
				prefs.removeNode();
			} catch (/* BackingStoreException */Exception e) { // for JRE1.3
			}
			prefs = Preferences.userNodeForPackage(Softsqueeze.class);
			prefs.put("version", CONFIG_VERSION);
			config.clear();
	    }
	    if (macaddress != null)
	        _putProperty("macaddress", macaddress);
	    
		Properties props = new Properties();
		try {
			InputStream propsin = getClass().getResourceAsStream(
					"/Softsqueeze.properties");
			if (propsin != null)
				props.load(propsin);
		} catch (IOException e) {
			logger.warn("Error reading property file", e);
		}
	    
		for (Iterator i=props.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry) i.next();
			
			String key = (String) e.getKey();
			if (_getProperty(key) == null)
				_putProperty(key, (String) e.getValue());
			
			// fix ssh port property in version 2.0b1 - 2.0b3
			if (key.equals("sshport") && _getProperty(key).length() == 0)
				_putProperty(key, (String) e.getValue());
		}
		
		// always override the softsqueeze version
		_putProperty("softsqueezeversion", props.getProperty("softsqueezeversion"));
                _putProperty("deviceid", props.getProperty("deviceid"));
                _putProperty("firmwareversion", props.getProperty("firmwareversion"));

		
		if (_getProperty("macaddress") == null)
			_putProperty("macaddress", Platform.macAddressToString(Platform.getMacAddress()));
			
		if (_getProperty("sshserver") == null)
			_putProperty("sshserver", _getProperty("slimserver"));

		if (_getProperty("audio.mixer") == null)
			_putProperty("audio.mixer", AudioMixer.getDefaultJavaSoundMixer());
		
		if (_getProperty("audio.mp3decoder") == null)
			_putProperty("audio.mp3decoder", AudioDecoder.getDefaultMP3Decoder());		
	}

	/**
	 * Returns a property value. The value is returned from one of: a) internal
	 * configuration if property has been used before b) system or applet
	 * property c) user preference
	 */
	private String _getProperty(String key) {
		if (config.containsKey(key))
			return (String) config.get(key);

		String val = (String) defval.get(key);
		if (Platform.JRE_1_4_PLUS)
			val = prefs.get(key, val);

		String prop = null;
		if (applet == null)
			prop = System.getProperty(key, null);
		else
			prop = applet.getParameter(key);

		val = (prop == null) ? val : prop;

		config.put(key, val);
		logger.debug("getProperty " + key + "=" + val);
		return val;
	}

	private boolean _getBooleanProperty(String key) {
		String val = _getProperty(key);
		return (val == null) ? false : val.equals("true");
	}

	private int _getIntegerProperty(String key) {
	    try {
	        String val = _getProperty(key);
	        return (val == null) ? -1 : Integer.parseInt(val);
	    }
	    catch (NumberFormatException e) {
	        return -1;
	    }
	}

	/**
	 * Store a property value.
	 */
	private void _putProperty(String key, String val) {
		logger.debug("putProperty " + key + "=" + val);
		String old = (String)config.put(key, val);
		if (Platform.JRE_1_4_PLUS)
			prefs.put(key, val);
		
		if (val.equals(old))
			return;
		
		logger.debug("property changed key="+key+" val="+val+" old="+old);
		ArrayList clone = (ArrayList)listeners.clone();
		for (Iterator i=clone.iterator(); i.hasNext(); ) {
			ConfigListener l = (ConfigListener) i.next();
			l.configSet(key, val);
		}
	}

	private void _putBooleanProperty(String key, boolean val) {
		_putProperty(key, val ? "true" : "false");
	}

	private void _putIntegerProperty(String key, int val) {
		_putProperty(key, Integer.toString(val));
	}

}
