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

import javax.swing.JApplet;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import org.titmuss.softsqueeze.config.Config;
import org.titmuss.softsqueeze.platform.Platform;

/**
 * 
 * @author richard
 */
public class Applet extends JApplet {
	private static final long serialVersionUID = -3156958951392220859L;

	private Softsqueeze squeeze;

	private boolean ok = false;

	public Applet() {
	}

	public void init() {
		try {
			if (!Platform.JRE_1_3_PLUS) {
				getContentPane().add(
						new JLabel("Please upgrade to Java 1.3",
								SwingConstants.CENTER));
			}

			Platform.init(this);
			Config.init(this);
			ok = true;
		} catch (NoClassDefFoundError e) {
			getContentPane().add(
					new JLabel("Failed to find SoftSqueeze libraries.",
							SwingConstants.CENTER));
		}
	}

	public void start() {
		if (!ok)
			return;

		try {
			squeeze = new Softsqueeze(this);
		} catch (NoClassDefFoundError e) {
			getContentPane().add(
					new JLabel("Failed to find SoftSqueeze libraries.",
							SwingConstants.CENTER));
		}
	}

	public void stop() {
		if (!ok)
			return;

		if (squeeze != null) {
			squeeze.exit();
			squeeze = null;
		}
	}

	public void destroy() {
	}
}