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

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.MemoryImageSource;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

import org.titmuss.softsqueeze.config.Config;
import org.titmuss.softsqueeze.config.ConfigListener;
import org.titmuss.softsqueeze.platform.Platform;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.l2fprod.gui.nativeskin.NativeSkin;
import com.l2fprod.gui.region.ImageRegion;


public class SkinWindow extends SkinObject implements ConfigListener {
	private ImageIcon icon;

	private String owner;

	private String configid;
	
	private boolean visible;
	
	private boolean iconfied;
	
	private boolean fullscreen;

	private int x, y;
	
	private int sx, sy;

	private String child = null;

	private boolean fullscreenDisplayMode = false;
	
	private DisplayMode oldDisplayMode;

	private Image windowmask = null;

	private static HashSet hiddenWindows = new HashSet();
	
	private static Container fullscreenWindow = null;
	
	private static Cursor transparentCursor = null;


	
	public SkinWindow(Skin skin, Element e) {
		super(skin, e);
		Config.addConfigListener(this);

		icon = parseImageAttribute(e, "icon", null);
		owner = parseStringAttribute(e, "owner", null);
		visible = parseBooleanAttribute(e, "visible", true);
		fullscreen = parseBooleanAttribute(e, "fullscreen", false);
		windowmask = parseBufferedImageAttribute(e, "windowmask", null);

		configid = parseStringAttribute(e, "configid", id);
		if (! configid.startsWith(skin.getSkinName()))
		    configid = skin.getSkinName() + "." + configid;

		sx = x = parseIntAttribute(e, "x", 100);
		sy = y = parseIntAttribute(e, "y", 100);

		NodeList kids = e.getChildNodes();
		for (int i = 0; i < kids.getLength(); i++) {
			Node n = (Node) kids.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE)
				continue;
			child = parseStringAttribute((Element) n, "id", child);
		}

		/* override skin by using last window position */
		int pX = Config.getIntegerProperty(configid+".x");
		int pY = Config.getIntegerProperty(configid+".y");
		if (pX != -1 && pY != -1) {
			logger.debug("Window location from config: " + id + " @ " + pX
					+ "," + pY);
			x = pX;
			y = pY;
		}
	}

	public Container createContainer() {
		Window window;
		Container contentPane;

		if (owner == null) {
			// 'real' window, so we have an icon in the icon bar
			logger.debug("Creating JFrame for window="+id);
			
			JFrame frame = new JFrame(name);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			if (Platform.JRE_1_4_PLUS)
				frame.setUndecorated(true);

			if (icon != null)
				frame.setIconImage(icon.getImage());

			/* XXXX to test on OSX ...
			Menu menu = new Menu("SoftSqueeze");
			skin.addMenuItems(menu);
			
			MenuBar menubar = new MenuBar();
			menubar.add(menu);
			frame.setMenuBar(menubar);
			*/
			
			window = frame;
			contentPane = frame.getContentPane();
		} else {
			// dialog window, so no icon
			logger.debug("Creating JDialog for window="+id+" owner="+owner);
			JFrame ownerFrame = (JFrame) skin.getContainer(owner);

			JDialog dialog = new JDialog(ownerFrame, name, false);
			dialog.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
			if (Platform.JRE_1_4_PLUS)
				dialog.setUndecorated(true);

			window = dialog;
			contentPane = dialog.getContentPane();
		}

		window.setLocation(x, y);
		if (Platform.JRE_1_4_PLUS) {
			window.setFocusable(true);
			window.setFocusTraversalKeysEnabled(false);
		}

		if (Platform.JRE_1_5_PLUS)
			window.setAlwaysOnTop(Config.getBooleanProperty("skin.alwaysontop"));
		
		window.addKeyListener(new WindowKeyListener());

		if (Config.getBooleanProperty("skin.touchscreen"))		
			window.setCursor(getTransparentCursor());
		
		// add component into container
		SkinComponent comp = skin.getSkinComponent(child);
		if (comp == null)
			logger.warn("Cannot find Element: " + child);
		else
			contentPane.add(comp.createComponent());

		window.pack();

		String pVisible = Config.getProperty(configid+".v");
		if (pVisible != null) {
			logger.debug("Got window visible pref for " + id + "=" + visible);
			visible = pVisible.equalsIgnoreCase("true");
		}
		setVisible(visible, window);

		if (windowmask != null && NativeSkin.isSupported()) {
			try {
				ImageRegion region = new ImageRegion(windowmask);
				NativeSkin.getInstance().setWindowRegion(window, region, true);
				windowmask = null;
			} catch (Throwable e) {
				logger.error("Cannot create window mask" + e);
			}
		}
		
		return window;
	}

    /* (non-Javadoc)
	 * @see com.slim.softsqueeze.ConfigListener#configSet(java.lang.String, java.lang.String)
	 */
	public void configSet(String key, String value) {
		if (key.equals("alwaysontop")) {
			Window window = (Window)skin.getContainer(id);
			if (window != null)
				window.setAlwaysOnTop(Config.getBooleanProperty("skin.alwaysontop"));
		}		
		if (key.equals("skin.touchscreen")) {
			Window window = (Window)skin.getContainer(id);
			if (Config.getBooleanProperty("skin.touchscreen"))
			    window.setCursor(getTransparentCursor());
			else
			    window.setCursor(Cursor.getDefaultCursor());
		}
	}

	public void setVisible(boolean visible) {
		Container window = skin.getContainer(id);
		setVisible(visible, window);
	}

	private void setVisible(boolean visible, Container window) {
		if (fullscreenWindow != null && fullscreenWindow != window) {
			if (visible)
				hiddenWindows.add(window);
			else
				hiddenWindows.remove(window);
			return;
		}
			
		window.setVisible(visible);

		/* ensure window is displayed on the screen */
		if (visible && isOffScreen(window))
			window.setLocation(sx, sy);
		
		if (fullscreen && window instanceof Window) {
			fullscreen(visible, (Window) window);
		}

		Config.putProperty(configid+".v", visible ? "true" : "false");
	}

	public boolean isVisible() {
		Container window = skin.getContainer(id);
		return window.isVisible();
	}
	
	public void setIconified(boolean iconified) {		
		Container window = skin.getContainer(id);
		setIconified(iconified, window);
	}
	
	public void setIconified(boolean iconified, Container window) {
		if (Platform.JRE_1_6_PLUS
				&& SystemTray.isSupported()
				&& Config.getBooleanProperty("skin.systemtray")
				&& visible) {
			window.setVisible(!iconified); 
		}

		if (window instanceof Frame) {
			((Frame) window).setExtendedState((iconified) ? Frame.ICONIFIED : Frame.NORMAL);
		}

		this.iconfied = iconified;
	}
	
	public boolean isIconified() {
		return iconfied;
	}
	
	public static boolean isFullscreen(Container window) {
		return (fullscreenWindow == window); 
	}
	
	public void setPosition(int x, int y) {
		Config.putIntegerProperty(configid+".x", x);
		Config.putIntegerProperty(configid+".y", y);
	}
	
	private void fullscreen(boolean zoom, Window window) {
		// fullscreen mode on support on screen 0 at the moment
		GraphicsEnvironment env = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		GraphicsDevice device = (env.getScreenDevices())[0];

		if (!zoom) {
			logger.debug("leaving fullscreen");

			if (fullscreenDisplayMode) {
                if (oldDisplayMode != null)
                    device.setDisplayMode(oldDisplayMode);
                device.setFullScreenWindow(null);
                fullscreenDisplayMode = false;
                oldDisplayMode = null;
            }
			
			for (Iterator i=hiddenWindows.iterator(); i.hasNext(); ) {
				Container c = (Container)i.next();
				c.setVisible(true);
			}
			hiddenWindows.clear();
			fullscreenWindow = null;
			return;
		}

		logger.debug("going fullscreen");

		oldDisplayMode = device.getDisplayMode();

		DisplayMode modes[] = device.getDisplayModes();
		DisplayMode mode = null;
		int bitdepth = 0;
		int refreshRate = 0;

		for (int j = 0; j < modes.length; j++) {
			logger.debug("display mode: w=" + modes[j].getWidth() + " h="
					+ modes[j].getHeight() + " bitdepth="
					+ modes[j].getBitDepth() + " refreshRate="
					+ modes[j].getRefreshRate());
			if (modes[j].getWidth() == width && modes[j].getHeight() == height) {
				if (modes[j].getBitDepth() < bitdepth
						|| modes[j].getBitDepth() > oldDisplayMode
								.getBitDepth())
					continue;
				if (modes[j].getRefreshRate() < refreshRate
						|| modes[j].getRefreshRate() > oldDisplayMode
								.getRefreshRate())
					continue;

				mode = modes[j];
				bitdepth = modes[j].getBitDepth();
				refreshRate = modes[j].getRefreshRate();
			}
		}
		
		fullscreenWindow = window;
		for (Iterator i=skin.containerIterator(); i.hasNext(); ) {
			Container c = (Container)((Map.Entry)i.next()).getValue();
			if (!c.isVisible() || c == window)
				continue;
			hiddenWindows.add(c);
			c.setVisible(false);
		}

		if (mode != null) {
		    fullscreenDisplayMode = true;

			device.setFullScreenWindow(window);
			device.setDisplayMode(mode);
		} else {
			logger.debug("no suitable display mode found");
			
		    fullscreenDisplayMode = false;			
		    window.setBounds(0, 0, oldDisplayMode.getWidth(), oldDisplayMode.getHeight());
		    window.setBackground(Color.BLACK);
		}
	}

	private boolean isOffScreen(Container window) {
		int top = (int) window.getY();
		int left = (int) window.getX();
		int bottom = top + window.getHeight();
		int right = left + window.getWidth();
	
		GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		for (int i = 0; i < gs.length; i++) {
			Rectangle bounds = gs[i].getDefaultConfiguration().getBounds();

			if (bottom >= bounds.getY()
					&& top <= (bounds.getY() + bounds.getHeight())
					&& right >= bounds.getX()
					&& left <= (bounds.getX() + bounds.getWidth()))
				return false;
		}
		
		return true;
	}


	/**
     * @return transparent cursor
     */
    private Cursor getTransparentCursor() {
        if (transparentCursor != null)
            return transparentCursor;
        
		int[] pixels = new int[16 * 16];
		Image image = Toolkit.getDefaultToolkit().createImage(
		        new MemoryImageSource(16, 16, pixels, 0, 16));
		transparentCursor =
		        Toolkit.getDefaultToolkit().createCustomCursor
		            (image, new Point(0, 0), "invisiblecursor");
		
		return transparentCursor;
    }
    

	protected class WindowKeyListener implements KeyListener {
		public void keyTyped(KeyEvent e) {
			KeyStroke key = KeyStroke.getKeyStrokeForEvent(e);
			
			Action a = skin.getActionKey(key);
			if (a != null)
			    a.keyTyped(e);
		}

		public void keyPressed(KeyEvent e) {
			KeyStroke key = KeyStroke.getKeyStrokeForEvent(e);
			
			Action a = skin.getActionKey(key);
			if (a != null)
			    a.keyPressed(e);
		}

		public void keyReleased(KeyEvent e) {
			KeyStroke key = KeyStroke.getKeyStrokeForEvent(e);
			
			Action a = skin.getActionKey(key);
			if (a != null)
			    a.keyReleased(e);
		}
	}
}