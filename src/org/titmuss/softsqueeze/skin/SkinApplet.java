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

import java.awt.Container;

import javax.swing.JApplet;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class SkinApplet extends SkinWindow {
	private String child = null;

	public SkinApplet(Skin skin, Element e) {
		super(skin, e);

		NodeList kids = e.getChildNodes();
		for (int i = 0; i < kids.getLength(); i++) {
			Node n = (Node) kids.item(i);
			if (n.getNodeType() != Node.ELEMENT_NODE)
				continue;
			child = parseStringAttribute((Element) n, "id", child);
		}
	}

	public Container createContainer() {
		JApplet applet = squeeze.getApplet();
		Container contentPane = applet.getContentPane();

		applet.addKeyListener(new WindowKeyListener());

		// add component into container
		SkinComponent comp = skin.getSkinComponent(child);
		if (comp == null)
			logger.warn("Cannot find Element: " + child);
		else
			contentPane.add(comp.createComponent());

		return applet;
	}

	public void setVisible(boolean visible) {
	}

	public boolean isVisible() {
		Container window = skin.getContainer(id);
		return window.isVisible();
	}
}