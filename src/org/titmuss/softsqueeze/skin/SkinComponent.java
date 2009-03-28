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

import java.awt.Dimension;

import javax.swing.JComponent;

import org.titmuss.softsqueeze.platform.Platform;
import org.w3c.dom.Element;


public abstract class SkinComponent extends SkinObject {
	protected String tooltip;

	public SkinComponent(Skin skin, Element e) {
		super(skin, e);

		tooltip = parseStringAttribute(e, "tooltip", null);
	}

	public void dispose() {
	}
	
	public JComponent addToPanel(JComponent panel, Element e) {
		int x = parseIntAttribute(e, "x", 0);
		int y = parseIntAttribute(e, "y", 0);

		JComponent c = createComponent();

		if (width > 0 && height > 0)
			c.setPreferredSize(new Dimension(width, height));
		c.setBorder(null);
		c.setLocation(x, y);
		if (Platform.JRE_1_4_PLUS)
			c.setFocusable(false);
		if (tooltip == null)
			c.setToolTipText(tooltip);

		panel.add(c);

		return c;
	}

	public abstract JComponent createComponent();
}

