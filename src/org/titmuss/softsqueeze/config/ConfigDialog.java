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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.SystemTray;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.titmuss.softsqueeze.audio.AudioDecoder;
import org.titmuss.softsqueeze.audio.AudioMixer;
import org.titmuss.softsqueeze.net.Protocol;
import org.titmuss.softsqueeze.platform.Platform;
import org.titmuss.softsqueeze.skin.Skin;
import org.titmuss.softsqueeze.util.Util;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * @author richard
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class ConfigDialog implements ItemListener {
	private Logger logger = Logger.getLogger("config");

	private JTabbedPane tabbedPane;

	private DefaultComboBoxModel slimserverAddr = new DefaultComboBoxModel();

	private DefaultComboBoxModel softsqueezeSkin = new DefaultComboBoxModel(
			new String[] { "excession", "lcd", "slimdevices", "woodgrain" });

	private DefaultComboBoxModel softsqueezeDisplay = new DefaultComboBoxModel(
			new String[] { "Squeezebox2", "Graphics", "Noritake" });

    private DefaultComboBoxModel sshProxyTypeModel = new DefaultComboBoxModel(
            new String[] { "None", "HTTP", "Socks 5" } );

	private HashMap slimservers;
	
	private JComboBox slimserverAddr1 = new JComboBox(slimserverAddr);

	private JComboBox softsqueezeSkin1 = new JComboBox(softsqueezeSkin);

	private JLabel javaVersion = new JLabel();

	private JLabel softsqueezeVersion = new JLabel();

	private JLabel slimserverVersion = new JLabel();

	private JComboBox slimserverAddr2 = new JComboBox(slimserverAddr);

	private JComboBox softsqueezeSkin2 = new JComboBox(softsqueezeSkin);

	private JComboBox softsqueezeDisplay1 = new JComboBox(softsqueezeDisplay);

	private JCheckBox softsqueezeOnTop = new JCheckBox();

	private JCheckBox systemtray = new JCheckBox();

	private JCheckBox touchscreenMode = new JCheckBox();
	
	private JComboBox audioMixer = new JComboBox();

	private JComboBox audioMP3Decoder = new JComboBox();
	
	private JButton getJavaMP3Decoder = new JButton("Download Java MP3 Plugin");

	private JTextPane helpJavaMP3Decoder = new JTextPane();
	
	private JTextField audioBufferSize = new JTextField(20);

	private JCheckBox saveStream = new JCheckBox();

	private JTextField saveFolder = new JTextField(20);

	private JButton saveFolderFile = new JButton("File");

	private JTextField slimserverHttpPort = new JTextField(6);

	private JTextField slimserverSlimPort = new JTextField(6);

	private JTextField softsqueezeMacAddress = new JTextField(20);
	
	private JTextField tcpWindowSize = new JTextField(20);	

	private JCheckBox sshTunnel = new JCheckBox();

	private JTextField sshPort = new JTextField(6);

	private JComboBox sshServerAddr = new JComboBox();

	private JTextField sshUsername = new JTextField(20);

	private JPasswordField sshPassword = new JPasswordField(20);

	private JCheckBox sshStorePassword = new JCheckBox();

	private JTextField sshKeys = new JTextField(20);

	private JButton sshKeysFile = new JButton("File ...");

	private JTextField sshPortForward = new JTextField(20);
	
    private JComboBox sshProxy = new JComboBox(sshProxyTypeModel);

    private JTextField sshProxyHost = new JTextField(20);

    private JTextField sshProxyPort = new JTextField(20);

	private JCheckBox debugAudioBuffer = new JCheckBox();

	private JCheckBox debugAudioBufferVerbose = new JCheckBox();

	private JCheckBox debugConfig = new JCheckBox();

	private JCheckBox debugGraphics = new JCheckBox();

	private JCheckBox debugJavaSound = new JCheckBox();

	private JCheckBox debugJavaSoundVerbose = new JCheckBox();

	private JCheckBox debugPlatform = new JCheckBox();

	private JCheckBox debugPlayer = new JCheckBox();

	private JCheckBox debugPlayerVerbose = new JCheckBox();

	private JCheckBox debugSearch = new JCheckBox();

	private JCheckBox debugSkin = new JCheckBox();

	private JCheckBox debugSlimproto = new JCheckBox();

	private JCheckBox debugCli = new JCheckBox();

	private JCheckBox debugSoftsqueeze = new JCheckBox();

	private JCheckBox debugSsh = new JCheckBox();

	private JCheckBox debugVisualizer = new JCheckBox();

	private JTextArea console = new JTextArea(20, 60);

	private JButton clearConsole = new JButton("Clear");

	private GridBagLayout gridbag = new GridBagLayout();
	
	private GridBagConstraints c = new GridBagConstraints();
	
	private Insets ci = new Insets(1, 1, 1, 1);
	
	private JPanel skinPanel;
	
	private ArrayList selectFields = new ArrayList();

	private Skin skin;
	
	
	public ConfigDialog() {
		tabbedPane = new JTabbedPane();

		JPanel basicPanel = new JPanel(gridbag);
		basicPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.add("Basic", basicPanel);
		add(basicPanel, "Squeezebox Server Hostname", slimserverAddr1);
		separator(basicPanel);
		add(basicPanel, "SoftSqueeze Skin", softsqueezeSkin1);
		separator(basicPanel);
		add(basicPanel, "Java version", javaVersion);
		add(basicPanel, "SoftSqueeze version", softsqueezeVersion);
		add(basicPanel, "Squeezebox Server version", slimserverVersion);
		padding(basicPanel);

		skinPanel = new JPanel(gridbag);
		skinPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.add("Skin", skinPanel);
		// skin panel build to order ...

		JPanel audioPanel = new JPanel(gridbag);
		audioPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.add("Audio", audioPanel);
		add(audioPanel, "Audio mixer", audioMixer);
		add(audioPanel, "Audio buffer size", audioBufferSize);
		add(audioPanel, "MP3 Decoder", audioMP3Decoder);
		add(audioPanel, null, helpJavaMP3Decoder);
		add(audioPanel, null, getJavaMP3Decoder);
		padding(audioPanel);
		/*
		separator(audioPanel);
		if (Config.getBooleanProperty("enablesavestream")) {
			add(audioPanel, "Save stream", saveStream);
			add(audioPanel, "Save in folder", saveFolder, saveFolderFile);
		}
		if (Config.getBooleanProperty("enablesavestream"))
			makeCompactGrid(audioPanel, 6, 3, 5, 5, 2, 2);
		else
		*/

		JPanel netPanel = new JPanel(gridbag);
		netPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.add("Networking", netPanel);
		add(netPanel, "Squeezebox Server Hostname", slimserverAddr2);
		add(netPanel, "Squeezebox Server HTTP Port", slimserverHttpPort);
		add(netPanel, "Squeezebox Server SlimProto Port", slimserverSlimPort);
		separator(netPanel);
		add(netPanel, "SoftSqueeze MAC Address", softsqueezeMacAddress);
		add(netPanel, "Tcp Window Size", tcpWindowSize);
		padding(netPanel);
				
		JPanel sshPanel = new JPanel(gridbag);
		sshPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.add("SSH", sshPanel);
		add(sshPanel, "Use SSH tunnel", sshTunnel);
		add(sshPanel, "SSH server", sshServerAddr);
		add(sshPanel, "SSH port", sshPort);
		add(sshPanel, "SSH username", sshUsername);
		add(sshPanel, "SSH password", sshPassword);
		add(sshPanel, "Store password", sshStorePassword);
		add(sshPanel, "SSH private keys", sshKeys, sshKeysFile);
		add(sshPanel, "Forward additional ports", sshPortForward);
		separator(sshPanel);
		add(sshPanel, "SSH Proxy", sshProxy);
		add(sshPanel, "SSH Proxy host", sshProxyHost);
		add(sshPanel, "SSH Proxy port", sshProxyPort);
		padding(sshPanel);

		JPanel debugPanel = new JPanel(gridbag);
		debugPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.add("Debug", debugPanel);
		add(debugPanel, "Audio Buffer", debugAudioBuffer);
		add(debugPanel, "Audio Buffer (verbose)", debugAudioBufferVerbose);
		add(debugPanel, "Configuration", debugConfig);
		add(debugPanel, "Graphics", debugGraphics);
		add(debugPanel, "Java Sound", debugJavaSound);
		add(debugPanel, "Java Sound (verbose)", debugJavaSoundVerbose);
		add(debugPanel, "Platform", debugPlatform);
		add(debugPanel, "Player", debugPlayer);
		add(debugPanel, "Player (verbose)", debugPlayerVerbose);
		add(debugPanel, "Search", debugSearch);
		add(debugPanel, "Skin", debugSkin);
		add(debugPanel, "Slimproto (verbose)", debugSlimproto);
		add(debugPanel, "Cli (verbose)", debugCli);
		add(debugPanel, "SoftSqueeze", debugSoftsqueeze);
		add(debugPanel, "SSH", debugSsh);
		add(debugPanel, "Visualizer", debugVisualizer);
		padding(debugPanel);

		JPanel consolePanel = new JPanel(gridbag);
		consolePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		tabbedPane.add("Console", consolePanel);
		add(consolePanel, new JScrollPane(console,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS));
		add(consolePanel, clearConsole);

		// Dynamic ui controls
		slimserverAddr1.setEditable(true);
		slimserverAddr2.setEditable(true);
		sshTunnel.addItemListener(this);
		sshServerAddr.setEditable(true);
		sshProxy.addItemListener(this);
		saveStream.addItemListener(this);
		saveFolderFile.addActionListener(new FileAction(audioPanel,
				"Save Streams Folder", saveFolder,
				JFileChooser.DIRECTORIES_ONLY));
		sshKeysFile.addActionListener(new FileAction(sshPanel,
				"Private Key File", sshKeys, JFileChooser.FILES_ONLY));

		String[] audioMixers = AudioMixer.getJavaSoundMixers();
		for (int i = 0; i < audioMixers.length; i++) {
			audioMixer.addItem(audioMixers[i]);
		}
		String[] audioMP3Decoders = AudioDecoder.getMP3Decoders();
		for (int i=0; i < audioMP3Decoders.length; i++) {
		    audioMP3Decoder.addItem(audioMP3Decoders[i]);
		}

		helpJavaMP3Decoder.setOpaque(true);
		helpJavaMP3Decoder.setEditable(false);
		helpJavaMP3Decoder.setPreferredSize(new Dimension(200, 40));
		helpJavaMP3Decoder.setText("You do not have the 'Java MP3 Plugin' installed. This plugin is optional, but if installed it will improve SoftSqueeze's performance when playing MP3 files.");
		getJavaMP3Decoder.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
    		    Platform.displayUrl("http://softsqueeze.sourceforge.net/javamp3plugin.html");
            }
		});
		
		// Configure log4j to send log messages to console
		Logger rootLogger = Logger.getRootLogger();
		rootLogger.addAppender(new LogAppender());
	}

	private void buildSkinConfig(String prefix, Element config) {
	    selectFields.clear();
	    skinPanel.removeAll();
	    
	    /* standard config options */
		add(skinPanel, "Skin", softsqueezeSkin2);
		//add(skinPanel, "LCD Display type", softsqueezeDisplay1);
		if (Platform.JRE_1_5_PLUS) {
			add(skinPanel, "Always on top", softsqueezeOnTop);
		}
		if (Platform.JRE_1_6_PLUS && SystemTray.isSupported()) {
			add(skinPanel, "Use system tray", systemtray);
			
		}
		add(skinPanel, "Touchscreen mode", touchscreenMode);
		separator(skinPanel);
		add(skinPanel, new JLabel("Skin Preferences"));
		
		if (config == null) {
		    padding(skinPanel);
		    return;
		}
	    
		/* custom config options */
		NodeList configList = config.getChildNodes();
		for (int i=0; i<configList.getLength(); i++) {
		    Node n = configList.item(i);
		    String type = n.getNodeName();
		    
		    if (type.equals("Select")) {
			    String id = ((Element)n).getAttribute("id");
			    String value = Config.getProperty(prefix + id);
			    String selectLabel = ((Element)n).getAttribute("label");
			    
			    ArrayList optionList = new ArrayList();
			    ConfigOption defaultOption = null;
			    
		        NodeList selectList = ((Element) n).getChildNodes();
				for (int j = 0; j < selectList.getLength(); j++) {
				    Node optionNode = (Node) selectList.item(j);
				    
				    if (! optionNode.getNodeName().equals("Option"))
				        continue;
				    
				    String optionValue = ((Element)optionNode).getAttribute("value");
				    String optionLabel = Util.xmlGetText(optionNode);
				    
				    ConfigOption option = new ConfigOption(prefix + id, optionValue, optionLabel);				    
				    optionList.add(option);
				    
				    if (value != null && value.equals(optionValue))
				        defaultOption = option;
				}
				
				ConfigOption optionArray[] = (ConfigOption[])optionList.toArray(new ConfigOption[optionList.size()]);				
				JComboBox selectBox = new JComboBox(optionArray);
				selectBox.setSelectedItem(defaultOption);
				
				selectFields.add(selectBox);
				add(skinPanel, selectLabel, selectBox);
		    }
		}		

		padding(skinPanel);	    
	}
	
	private void setComponents() {
		// basic config
		slimservers = Protocol.getDiscoveredServers();
		slimserverAddr.removeAllElements();

		String slimserverAddrConfig = Config.getProperty("slimserver");
		String slimserverAddrSelected = null;
		for (Iterator i = slimservers.entrySet().iterator(); i.hasNext();) {
			Map.Entry e = (Map.Entry) i.next();
			slimserverAddr.addElement(e.getKey());
			if (e.getKey().equals(slimserverAddrConfig))
				slimserverAddrSelected = (String) e.getKey();
		}

		if (slimserverAddrSelected != null)
			slimserverAddr.setSelectedItem(slimserverAddrSelected);
		else if (!"".equals(slimserverAddrConfig)) {
			slimserverAddr.addElement(slimserverAddrConfig);
			slimserverAddr.setSelectedItem(slimserverAddrConfig);
		}

		javaVersion.setText(System.getProperty("java.version"));
		softsqueezeVersion.setText(Config.getProperty("softsqueezeversion"));
		slimserverVersion.setText(Config.getProperty("serverversion"));

		// skin config
		softsqueezeSkin.setSelectedItem(Config.getProperty("skin"));
		softsqueezeDisplay.setSelectedItem(Config
				.getProperty("skin.displayemulation"));
		softsqueezeOnTop.setSelected(Config.getBooleanProperty("skin.alwaysontop"));
		systemtray.setSelected(Config.getBooleanProperty("skin.systemtray"));
		touchscreenMode.setSelected(Config.getBooleanProperty("skin.touchscreen"));
		buildSkinConfig(skin.getConfigPrefix(), skin.getSkinConfig());
		
		// audio config
		String oldMixer = Config.getProperty("audio.mixer");
		if (oldMixer != null && oldMixer.length() > 0) {
			audioMixer.setSelectedItem(oldMixer);
		}
		String oldMP3Decoder= Config.getProperty("audio.mp3decoder");
		if (oldMP3Decoder != null && oldMP3Decoder.length() > 0) {
			audioMP3Decoder.setSelectedItem(oldMP3Decoder);
		}
		audioBufferSize.setText(Config.getProperty("audio.lineBufferSize"));
		saveStream.setSelected(Config.getBooleanProperty("savestream"));
		saveFolder.setText(Config.getProperty("savedir"));

		// networking config
		slimserverHttpPort.setText(Config.getProperty("httpport"));
		slimserverSlimPort.setText(Config.getProperty("slimport"));
		softsqueezeMacAddress.setText(Config.getProperty("macaddress"));
		tcpWindowSize.setText(Config.getProperty("audio.tcpwindowsize"));
		sshTunnel.setSelected(Config.getBooleanProperty("sshtunnel"));
		sshPort.setText(Config.getProperty("sshport"));
		sshServerAddr.removeAllItems();
		sshServerAddr.addItem("[ On Squeezebox Server ]");
		if (Config.getProperty("sshserver").equals(Config.getProperty("slimserver"))) {
		    sshServerAddr.setSelectedIndex(0);
		}
		else {
		    sshServerAddr.addItem(Config.getProperty("sshserver"));
		    sshServerAddr.setSelectedIndex(1);
		}
		sshUsername.setText(Config.getProperty("sshusername"));
		sshPassword.setText(new String(Config.getPassword("sshpassword")));
		sshStorePassword.setSelected(!Config.getProperty("sshpassword").equals(
				""));
		sshKeys.setText(Config.getProperty("sshprivatekey"));
		sshPortForward.setText(Config.getProperty("sshforwardports"));

		sshProxy.setSelectedItem(Config.getProperty("sshproxy"));
		sshProxyHost.setText(Config.getProperty("sshproxyhost"));
		sshProxyPort.setText(Config.getProperty("sshproxyport"));

		
		// debug config
		debugAudioBuffer.setSelected(isLoggerDebug("audiobuffer"));
		debugAudioBufferVerbose.setSelected(isLoggerDebug("audiobuffer.verbose"));
		debugConfig.setSelected(isLoggerDebug("config"));
		debugGraphics.setSelected(isLoggerDebug("graphics"));
		debugJavaSound.setSelected(isLoggerDebug("javasound"));
		debugJavaSoundVerbose.setSelected(isLoggerDebug("javasound.verbose"));
		debugPlatform.setSelected(isLoggerDebug("platform"));
		debugPlayer.setSelected(isLoggerDebug("player"));
		debugPlayerVerbose.setSelected(isLoggerDebug("player.verbose"));
		debugSearch.setSelected(isLoggerDebug("search"));
		debugSkin.setSelected(isLoggerDebug("skin"));
		debugSlimproto.setSelected(isLoggerDebug("slimproto"));
		debugCli.setSelected(isLoggerDebug("cli"));
		debugSoftsqueeze.setSelected(isLoggerDebug("softsqueeze"));
		debugSsh.setSelected(isLoggerDebug("ssh"));
		debugVisualizer.setSelected(isLoggerDebug("visualizer"));
		
		enableComponents();
	}
	
	public void showDialog(Skin skin) {
	    this.skin = skin;
	    
		setComponents();

		// Fix tabbed pane l&f
		setOpaqueChildren(tabbedPane, false);

		JButton resetButton = new JButton("Reset");
		resetButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int sure = JOptionPane.showConfirmDialog(null, "Are you sure you want to reset your SoftSqueeze configuration", "Reset configuration", JOptionPane.OK_CANCEL_OPTION);
				if (sure != JOptionPane.OK_OPTION)
					return;
				Config.resetProperties();				
				setComponents();
			}
		});
		
		Object options[] = {
				"OK",
				"Cancel",
				resetButton
		};
		
		int selected = JOptionPane.showOptionDialog(null,
				tabbedPane,
				"SoftSqueeze Preferences",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[0]);

		if (selected == 1 || selected == JOptionPane.CLOSED_OPTION)
			return; // cancelled
		
		// debug config
		setLoggerDebug("audiobuffer", debugAudioBuffer.isSelected());
		setLoggerDebug("audiobuffer.verbose", debugAudioBufferVerbose.isSelected());
		setLoggerDebug("config", debugConfig.isSelected());
		setLoggerDebug("graphics", debugGraphics.isSelected());
		setLoggerDebug("javasound", debugJavaSound.isSelected());
		setLoggerDebug("javasound.verbose", debugJavaSoundVerbose.isSelected());
		setLoggerDebug("platform", debugPlatform.isSelected());
		setLoggerDebug("player", debugPlayer.isSelected());
		setLoggerDebug("player.verbose", debugPlayerVerbose.isSelected());
		setLoggerDebug("search", debugSearch.isSelected());
		setLoggerDebug("skin", debugSkin.isSelected());
		setLoggerDebug("slimproto", debugSlimproto.isSelected());
		setLoggerDebug("cli", debugCli.isSelected());
		setLoggerDebug("softsqueeze", debugSoftsqueeze.isSelected());
		setLoggerDebug("ssh", debugSsh.isSelected());
		setLoggerDebug("visualizer", debugVisualizer.isSelected());

		// basic config
		String newServer = (String) slimserverAddr.getSelectedItem();		
		Config.putProperty("slimserver", newServer);
		Config.putProperty("skin", (String) softsqueezeSkin.getSelectedItem());

		// skin config
		Config.putProperty("skin.displayemulation", (String) softsqueezeDisplay
				.getSelectedItem());
		Config.putBooleanProperty("skin.alwaysontop", softsqueezeOnTop.isSelected());
		Config.putBooleanProperty("skin.systemtray", systemtray.isSelected());
		Config.putBooleanProperty("skin.touchscreen", touchscreenMode.isSelected());

		// audio config
		Config.putProperty("audio.mixer", (String) audioMixer.getSelectedItem());
		Config.putProperty("audio.mp3decoder", (String) audioMP3Decoder.getSelectedItem());
		Config.putIntegerProperty("audio.lineBufferSize", Integer.parseInt(audioBufferSize.getText()));
		Config.putBooleanProperty("savestream", saveStream.isSelected());
		Config.putProperty("savedir", saveFolder.getText());

		// networking config
		Config.putProperty("httpport", slimserverHttpPort.getText());
		Config.putProperty("slimport", slimserverSlimPort.getText());
		Config.putProperty("macaddress", softsqueezeMacAddress.getText());
		if (tcpWindowSize.getText().length() > 0)
		    Config.putIntegerProperty("audio.tcpwindowsize", Integer.parseInt(tcpWindowSize.getText()));
		
		Config.putBooleanProperty("sshtunnel", sshTunnel.isSelected());
		if (sshServerAddr.getSelectedIndex() == 0) {
			Config.putProperty("sshserver", (String) slimserverAddr.getSelectedItem());		    
		}
		else {
		    Config.putProperty("sshserver", (String) sshServerAddr.getSelectedItem());
		}
		Config.putProperty("sshport", sshPort.getText());
		Config.putProperty("sshusername", sshUsername.getText());
		if (sshStorePassword.isSelected())
			Config.putPassword("sshpassword", sshPassword.getPassword());
		else
			Config.putPassword("sshpassword", null);
		Config.putProperty("sshprivatekey", sshKeys.getText());
		Config.putProperty("sshforwardports", sshPortForward.getText());
		
		Config.putProperty("sshproxy", (String) sshProxy.getSelectedItem());
		Config.putProperty("sshproxyhost", sshProxyHost.getText());
		Config.putProperty("sshproxyport", sshProxyPort.getText());

		
		for (Iterator i=selectFields.iterator(); i.hasNext(); ) {
		    JComboBox select = (JComboBox) i.next();
		    
		    ConfigOption option = (ConfigOption) select.getSelectedItem();		    
		    Config.putProperty(option.getProperty(), option.getValue());		    
		}
		
	}

	private void add(JPanel panel, JComponent component) {
		add(panel, "", component, null);
	}
	
	private void add(JPanel panel, String label, JComponent component) {
	    add(panel, label, component, null);
	}
	
	private void add(JPanel panel, String label, JComponent component, JComponent button) {
	    JLabel jlabel = null;
	    if (label != null) {
	        if (label.length() > 0)
	            label = label + ":";
			jlabel = new JLabel(label, JLabel.TRAILING);
			jlabel.setLabelFor(component);
		}
	    
	    add(panel, jlabel, component, button, 0.0);
	}
	
	private void add(JPanel panel, JComponent label, JComponent component, JComponent button, double weighty) {
	    if (label != null) {
	        panel.add(label);
	        c.weightx = 0.0;
	        c.weighty = weighty;
	        c.gridwidth = 1;
	        c.anchor = GridBagConstraints.NORTHWEST;
	        c.fill = GridBagConstraints.NONE;
	        c.insets = ci;
	        gridbag.setConstraints(label, c);
	    }

	    panel.add(component);
	    c.weightx = 1.0;
	    c.weighty = weighty;
	    c.gridwidth = GridBagConstraints.RELATIVE;
	    c.anchor = GridBagConstraints.NORTHWEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.insets = new Insets(2, 2, 2, 2);
	    gridbag.setConstraints(component, c);

	    if (button == null)
	        button = new JLabel();
	    
	    panel.add(button);
	    c.weightx = 0.0;
	    c.weighty = weighty;
	    c.gridwidth = GridBagConstraints.REMAINDER;
	    c.anchor = GridBagConstraints.NORTHWEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.insets = new Insets(2, 2, 2, 2);
	    gridbag.setConstraints(button, c);
	}

	private void separator(JPanel panel) {
	    add(panel, new JLabel(), new JSeparator(), null, 0.0);
	}

	private void padding(JPanel panel) {
	    add(panel, (JComponent)null, new JLabel(), null, 1.0);
	}

	private static void setOpaqueChildren(JComponent comp, boolean enable) {
		comp.setOpaque(enable);
		Component[] comps = comp.getComponents();
		for (int i = 0; i < comps.length; i++) {
			Component cc = comps[i];

			if (cc instanceof JComponent && !(cc instanceof JTextComponent)
					&& !(cc instanceof JList)) {
				setOpaqueChildren((JComponent) comps[i], enable);
			}
		}
	}

	private boolean isLoggerDebug(String name) {
		Logger logger = Logger.getLogger(name);
		return logger.getLevel() == Level.DEBUG;
	}

	private void setLoggerDebug(String name, boolean debug) {
		Logger logger = Logger.getLogger(name);
		logger.setLevel(debug ? Level.DEBUG : Level.INFO);
	}

	private class LogAppender extends AppenderSkeleton {
		private Layout layout = new PatternLayout("%r [%t] %-5p %c - %m%n");

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.apache.log4j.AppenderSkeleton#append(org.apache.log4j.spi.LoggingEvent)
		 */
		protected void append(LoggingEvent event) {
			console.append(layout.format(event));
			String throwable[] = event.getThrowableStrRep();
			if (throwable != null) {
				for (int i=0; i<throwable.length; i++) {
					console.append(throwable[i]);
					console.append("\n");
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.apache.log4j.Appender#close()
		 */
		public void close() {
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.apache.log4j.Appender#requiresLayout()
		 */
		public boolean requiresLayout() {
			return true;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == sshTunnel
		        || e.getSource() == sshProxy
		        || e.getSource() == saveStream) {
			enableComponents();
		}
	}

	private void enableComponents() {
		boolean sshEnable = sshTunnel.isSelected();
		sshServerAddr.setEnabled(sshEnable);
		sshPort.setEnabled(sshEnable);
		sshUsername.setEnabled(sshEnable);
		sshPassword.setEnabled(sshEnable);
		sshStorePassword.setEnabled(sshEnable);
		sshKeys.setEnabled(sshEnable);
		sshKeysFile.setEnabled(sshEnable);
		sshPortForward.setEnabled(sshEnable);

		boolean sshProxyEnable = sshProxy.getSelectedIndex() > 0;
		sshProxy.setEnabled(sshEnable);
		sshProxyHost.setEnabled(sshEnable && sshProxyEnable);
		sshProxyPort.setEnabled(sshEnable && sshProxyEnable);
		
		boolean saveEnable = saveStream.isSelected();
		saveFolder.setEnabled(saveEnable);
		saveFolderFile.setEnabled(saveEnable);
		
		boolean hasMP3Plugin = AudioDecoder.isMP3PluginInstalled(); 			
		getJavaMP3Decoder.setVisible(!hasMP3Plugin);
		helpJavaMP3Decoder.setVisible(!hasMP3Plugin);
	}

	private class FileAction implements ActionListener {
		private JPanel panel;

		private String title;

		private JTextField textfield;

		private int mode;

		FileAction(JPanel panel, String title, JTextField textfield, int mode) {
			this.panel = panel;
			this.title = title;
			this.textfield = textfield;
		}

		public void actionPerformed(ActionEvent e) {
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle(title);
			chooser.setFileHidingEnabled(false);
			chooser.setFileSelectionMode(mode);
			int result = chooser.showOpenDialog(panel);
			if (result == JFileChooser.APPROVE_OPTION) {
				textfield.setText(chooser.getSelectedFile().getAbsolutePath());
			}

		}
	}
	
	private class ConfigOption {
	    private String property;
	    
	    private String value;
	    
	    private String label;
	    
	    ConfigOption(String property, String value, String label) {
	        this.property = property;
	        this.value = value;
	        this.label = label;
	    }
	    
	    private String getProperty() {
	        return property;
	    }
	    
	    private String getValue() {
	        return value;
	    }
	    
	    private String getLabel() {
	        return label;
	    }
	    
	    public String toString() {
	        return label;
	    }  
	}
}