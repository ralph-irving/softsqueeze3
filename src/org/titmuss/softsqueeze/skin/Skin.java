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

package org.titmuss.softsqueeze.skin;

import java.awt.AWTException;
import java.awt.Container;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.KeyStroke;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.titmuss.softsqueeze.Softsqueeze;
import org.titmuss.softsqueeze.config.Config;
import org.titmuss.softsqueeze.platform.Platform;
import org.titmuss.softsqueeze.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * 
 * @author richard
 */
public class Skin {
	private static Logger logger = Logger.getLogger("skin");

	private Softsqueeze squeeze;

	private String skinname;
	
	private String skindir;

	private HashMap components = new HashMap();

	private HashMap containers = new HashMap();

	private HashMap windows = new LinkedHashMap();
	
	private HashMap actionsRGB = new HashMap();
	
	private HashMap actionsKey = new HashMap();
	
	private Action knobAction;
	
	private String configPrefix;
	
	private Element config;
	
	private int deviceid;

    private TrayIcon trayIcon = null;


	public Skin(Softsqueeze squeeze) {
		this(squeeze, "excession");
	}

	/** Creates a new instance of Slimpy */
	public Skin(Softsqueeze squeeze, String skin) {
		this.squeeze = squeeze;
		
		skinname = skin;
		skindir = "/skin/" + skinname + "/";		
		configPrefix = "skin." + skinname + ".";
		deviceid = Config.getIntegerProperty("deviceid");
		
		loadSkin(skinname + ".xml");

		for (Iterator i = windows.values().iterator(); i.hasNext();) {
			SkinWindow window = (SkinWindow) i.next();

			Container container = window.createContainer();
			if (container == null)
			    continue;
			
			containers.put(window.getId(), container);
		}
		
		createSystemTray();
	}

	public URL getResource(String res) {
	    if (res.startsWith("/"))
		res = res.substring(1);
	    return getClass().getResource(skindir + res);
	}
	
	public Softsqueeze getSoftSqueeze() {
		return squeeze;
	}

	public String getSkinName() {
		return skinname;
	}

	public int getDeviceId() {
		return deviceid;	
	}

	public void dispose() {
		for (Iterator i = containers.values().iterator(); i.hasNext();) {
			Container window = (Container) i.next();
			if (window instanceof Window)
				((Window) window).dispose();
		}
		for (Iterator i = components.values().iterator(); i.hasNext(); ) {
			SkinComponent c = (SkinComponent) i.next();
			c.dispose();
		}
		if (trayIcon != null) {
	         SystemTray tray = SystemTray.getSystemTray();
	         tray.remove(trayIcon);
		}
	}

	public Container getContainer(String id) {
		return (Window) containers.get(id);
	}

	public Iterator containerIterator() {
		return containers.entrySet().iterator();
	}

	public SkinWindow getSkinWindow(String id) {
	    id = var(id);
		return (SkinWindow) windows.get(id);
	}

	public SkinComponent getSkinComponent(String id) {
	    id = var(id);
		return (SkinComponent) components.get(id);
	}

	public SkinWindow getSkinWindow(Container window) {
		for (Iterator i=containers.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			if (e.getValue() == window) {
				return getSkinWindow((String)e.getKey());
			}
		}
		return null;
	}

	public boolean evalBoolean(String bool) {
	    String str[] = Util.split(bool, "=");
	    
	    if (str.length > 1) {
	        return var(str[0]).equals(str[1]);
	    }
	    else {
	        return var(str[0]).equalsIgnoreCase("true");
	    }
	}
		
	private String var(String str) {
	    if (str.startsWith("$")) {
	        String var = str.substring(2, str.length()-1);
	        str = Config.getProperty(configPrefix + var);
	    }	    
	    return str;
	}
	
	public Action getActionRGB(int rgb) {
	    return (Action) actionsRGB.get(new Integer(rgb));
	}
	
	public Action getActionKey(KeyStroke key) {
	    return (Action) actionsKey.get(key);
	}
	
	public Action getKnobAction() {
	    return knobAction;
	}
	
	public String getConfigPrefix() {
	    return configPrefix;
	}
	
	public Element getSkinConfig() {
	    return config;
	}
	
	private void loadSkin(String file) {
		try {
			logger.debug("Loading skin " + file);
			URL url = getClass().getResource(skindir + file);
			if (url == null) {
				logger.warn("Skin not found: " + skindir + file);
				return;
			}			
			
			DocumentBuilderFactory xmlfactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder xmlbuilder = xmlfactory.newDocumentBuilder();
			
			/* For some unknown reason loading the skin.dtd stopped working 
			 * on osx. Did Apple do a Java update? This allows the xml builder
			 * to resolve the path correctly.
			 */
			xmlbuilder.setEntityResolver(new EntityResolver() {
				public InputSource resolveEntity(String arg0, String arg1) throws SAXException, IOException {
					if (arg1.endsWith("skin.dtd")) {
						return new InputSource(Skin.class.getResourceAsStream("/skin/skin.dtd"));
					}
					else {
						return null;
					}
				}
			});
			Document doc = xmlbuilder.parse(url.toString());
			Element xmlskin = (Element) doc.getDocumentElement();

			loadSkin(xmlskin);
		} catch (IOException e) {
		    logger.error("Cannot load skin.xml", e);
		} catch (javax.xml.parsers.ParserConfigurationException e) {
		    logger.error("Cannot load skin.xml", e);
		} catch (org.xml.sax.SAXException e) {
		    logger.error("Cannot load skin.xml", e);
		}
	}

	private void loadSkin(Element xmlskin) {
	    NodeList kids = xmlskin.getChildNodes();
	    for (int i = 0; i < kids.getLength(); i++) {
	        Node n = (Node) kids.item(i);
	        String node = n.getNodeName();
	     
	        if (node.equals("If")) {
	            String test = ((Element) n).getAttribute("test");
	            
	            if (evalBoolean(test))
	                loadSkin((Element) n);	            
	        }
	        else if (node.equals("Include")) {
	            String include = ((Element) n).getAttribute("file");
	            loadSkin(include);
	        }
	        else if (node.equals("Config")) {
	            loadConfig((Element) n);
	        }
	        else if (node.equals("Elements")) {
	            loadElement((Element) n);
	        }
	        else if (node.equals("Window")) {
	            SkinWindow c = new SkinWindow(this, (Element) n);
	            windows.put(c.getId(), c);
	        }
	        else if (node.equals("Set")) {
	        	String name = ( (Element)n).getAttribute("name");
	        	String value = ( (Element)n).getAttribute("value");
	        	
	        	if (name.equalsIgnoreCase("deviceid")) {
	        		deviceid = Integer.parseInt(value);
	        	}
	        }
	        else if (node.equals("Applet") && squeeze.getApplet() != null) {
	            SkinWindow c = new SkinApplet(this, (Element) n);
	            windows.put(c.getId(), c);
	        }
	        else if (node.equals("Action")) {
	            String script = ((Element) n).getAttribute("script");
	            
	            Action action;			    
	            if (script.startsWith("drag(")) {
	                action = new WindowAction(this, (Element) n);
	            }
	            else if (script.startsWith("ir(")) {
	                action = new IRAction(this, (Element) n);
	            }
	            else if (script.startsWith("switch(")) {
	                action = new SwitchAction(this, (Element) n);
	            }
	            else if (script.startsWith("knob(")) {
	                action = new KnobAction(this, (Element) n);
	            }
	            else {
	                action = new ButtonAction(this, (Element) n);			        
	            }
	            
	            actionsRGB.put(new Integer(action.getMaskRGB()), action);
	            addActionKey(action);
	            
	            if (action.isKnobAction())
	                knobAction = action;
	        }
	    }	    
	}

	/**
     * @param config
     */
    private void loadConfig(Element config) {
        this.config = (Element) config.cloneNode(true);
        
        NodeList kids = config.getChildNodes();
		for (int i = 0; i < kids.getLength(); i++) {
			Node n = (Node) kids.item(i);
			String node = n.getNodeName();
			
			String id = null;
			String def = null;;
			if (node.equals("Select")) {
			    id = ((Element)n).getAttribute("id");
			    
		        NodeList kids2 = ((Element) n).getChildNodes();
				for (int j = 0; j < kids2.getLength(); j++) {
				    Node option = (Node) kids2.item(j);
				    
				    if (! option.getNodeName().equals("Option"))
				        continue;
				    
				    String val = ((Element)option).getAttribute("value");
				    String isDefault = ((Element)option).getAttribute("default");
				    
				    if (j == 0)
				        def = val;
				    if (isDefault != null && isDefault.equalsIgnoreCase("true")) {
				        def = val;
				        break;
				    }
				}
			}

			if (id == null || def == null)
			    continue;
			
			String val = Config.getProperty(configPrefix + id);
			if (val == null)
			    Config.putProperty(configPrefix + id, def);
		}
    }

    private void loadElement(Element e1) {
		NodeList kids = e1.getChildNodes();
		for (int j = 0; j < kids.getLength(); j++) {
			Node n = (Node) kids.item(j);
			if (n.getNodeType() != Node.ELEMENT_NODE)
				continue;

			String node = n.getNodeName();

			SkinComponent comp = null;
			if (node.equals("VfPanel"))
				comp = new LcdPanel(this, (Element) n);
			else if (node.equals("SearchPanel"))
				comp = new SearchPanel(this, (Element) n);
			else if (node.equals("Playlist"))
				comp = new Playlist(this, (Element) n);
			else if (node.equals("GroupDef"))
				comp = new SkinGroup(this, (Element) n);
			else {
				logger.warn("Unknown component: " + node);
				continue;
			}

			String id = comp.getId();
			if (components.get(id) != null)
			    logger.warn("Component " + id + " is defined multiple times");
			
			components.put(id, comp);
		}
	}

	
	private void addActionKey(Action action) {
	    String keystroke = action.getKeybinding();	  
	    if (keystroke == null)
	        return;
	    
	    String keys[] = Util.split(keystroke, ",");
	    for (int i=0; i<keys.length; i++) {
	        KeyStroke pkey = KeyStroke.getKeyStroke(keys[i]);
	        if (pkey == null)
	            logger.warn("Cannot parse keybinding " + keystroke);

	        actionsKey.put(pkey, action);
	    }
	}
	
	protected void addMenuItems(Menu menu) {
        MenuItem playItem = new MenuItem("Play");
        playItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				squeeze.sendIR(1, 1, 0x768910ef);
			}
        });
        menu.add(playItem);

        MenuItem pauseItem = new MenuItem("Pause");
        pauseItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				squeeze.sendIR(1, 1, 0x768920df);
			}
        });
        menu.add(pauseItem);

        MenuItem fwdItem = new MenuItem("Next");
        fwdItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				squeeze.sendIR(1, 1, 0x7689a05f);
			}
        });
        menu.add(fwdItem);

        MenuItem rewItem = new MenuItem("Previous");
        rewItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				squeeze.sendIR(1, 1, 0x7689c03f);
			}
        });
        menu.add(rewItem);

        menu.add(new MenuItem("-"));
        
        MenuItem hideItem = new MenuItem("Hide");
        hideItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (Iterator i=windows.values().iterator(); i.hasNext(); ) {
					SkinWindow w = (SkinWindow) i.next();					
					if (w.isVisible()) {
						w.setIconified(true);
					}
				}
			}
        });
        menu.add(hideItem); 	

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				squeeze.exit();
			}
        });
        menu.add(exitItem);		
	}
	
	public void createSystemTray() {
	     if (Platform.JRE_1_6_PLUS
	    		 	&& SystemTray.isSupported()
	    		 	&& Config.getBooleanProperty("skin.systemtray")) {
	         // get the SystemTray instance
	         SystemTray tray = SystemTray.getSystemTray();

	         // create a popup menu
	         PopupMenu popup = new PopupMenu();
	         addMenuItems(popup);
	         
	         // add menu to tray
	         Image image = Toolkit.getDefaultToolkit().getImage(getResource("icon.png"));
	         trayIcon = new TrayIcon(image, "SoftSqueeze", popup);
	         trayIcon.addMouseListener(new MouseListener() {
				public void mouseClicked(MouseEvent e) {
				}

				public void mousePressed(MouseEvent e) {
					if (e.getButton() != MouseEvent.BUTTON1) {
						return;
					}
					
					// check is any windows are visible
					for (Iterator i=windows.values().iterator(); i.hasNext(); ) {
						SkinWindow w = (SkinWindow) i.next();

						if (w.isIconified()) {
							w.setIconified(false);
						}
					}
					e.consume();
				}

				public void mouseReleased(MouseEvent e) {
				}

				public void mouseEntered(MouseEvent e) {
				}

				public void mouseExited(MouseEvent e) {
				}
	         });
	         try {
	             tray.add(trayIcon);
	         } catch (AWTException e) {
	             logger.error(e);
	         }
	     }
	}
}
