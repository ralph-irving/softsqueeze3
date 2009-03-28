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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;

public class OptionPanel extends JPanel {
	private static final long serialVersionUID = -2071303285614480187L;

	private GridBagLayout gridbag;

	private GridBagConstraints c;

	public OptionPanel() {
		gridbag = new GridBagLayout();
		c = new GridBagConstraints();
		c.insets = new Insets(0, 2, 0, 2);
		setLayout(gridbag);
	}

	public Component add(Component comp) {
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints(comp, c);
		super.add(comp);

		return comp;
	}

	public Component add(Component label, Component option) {
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(label, c);
		super.add(label);

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints(option, c);
		super.add(option);

		return option;
	}

	public Component add(Component label, Component option, Component button) {
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(label, c);
		super.add(label);

		c.gridwidth = GridBagConstraints.RELATIVE;
		c.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints(option, c);
		super.add(option);

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints(button, c);
		super.add(button);

		return option;
	}

}