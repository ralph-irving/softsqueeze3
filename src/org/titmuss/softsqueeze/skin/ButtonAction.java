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

import java.awt.AWTEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import org.titmuss.softsqueeze.config.Config;
import org.titmuss.softsqueeze.platform.Platform;
import org.w3c.dom.Element;


/**
 * Emulate IR button
 */
public class ButtonAction extends Action {

	public ButtonAction(Skin skin, Element e) {
		super(skin, e);
	}

	public void mouseReleased(MouseEvent e) {
		fire(e);
	}

	public void keyPressed(KeyEvent e) {
	    fire(e);
	}


	private void fire(AWTEvent e) {
		if (arg[0].equalsIgnoreCase("quit")) {
			squeeze.exit();
		}
		else if (arg[0].equalsIgnoreCase("setVisible") && arg[1].equalsIgnoreCase("softsqueeze.config")) {
			squeeze.openConfigDialog();
		}
		else if (arg[0].equalsIgnoreCase("setVisible")) {
			SkinWindow window = skin.getSkinWindow(arg[1]);
			if (window == null) {
				logger.warn("Visible toggle on non-existant frame " + arg[1]);
				return;
			}
			boolean visible = !window.isVisible();
			if (arg.length > 2)
			    visible = arg[2].equalsIgnoreCase("true");
			window.setVisible(visible);
		}
		else if (arg[0].equalsIgnoreCase("openURL")) {
			String url = arg[1];
			int slimserverPos = url.indexOf("${slimserver}");
			if (slimserverPos > 0) {
				int httpport = Config.getServerWebPort();
				String slimserver = Config.getSlimServerAddress() + ":"	+ httpport;
				url = url.substring(0, slimserverPos) + slimserver
						+ url.substring(slimserverPos + 13);
			}

			int macPos = url.indexOf("${player}");
			if (macPos > 0) {
				String mac = Config.getProperty("macaddress");
				url = url.substring(0, macPos) + mac
						+ url.substring(macPos + 9);
			}

			Platform.displayUrl(url);
		}
		else if (arg[0].equalsIgnoreCase("iconify")) {
			SkinWindow window = skin.getSkinWindow(getWindowForEvent(e));
			window.setIconified(true);
		}
		else {
			logger.warn("Button action not recognised: " + arg[0]);
		}
	}
}
