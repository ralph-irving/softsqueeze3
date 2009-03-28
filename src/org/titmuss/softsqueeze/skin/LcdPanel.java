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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.titmuss.softsqueeze.display.LcdDisplay;
import org.titmuss.softsqueeze.display.LcdDisplayListener;
import org.w3c.dom.Element;


public class LcdPanel extends SkinComponent {
	private int scale;
	
	private int xdiv;

	private int offset;

	private Color fgColor;

	private Color bgColor;

	public LcdPanel(Skin skin, Element e) {
		super(skin, e);

		scale = parseIntAttribute(e, "scale", 1);
		xdiv = parseIntAttribute(e, "xdiv", 1);
		offset = parseIntAttribute(e, "offset", 0);
		fgColor = parseColorAttribute(e, "fgcolor", Color.green);
		bgColor = parseColorAttribute(e, "bgcolor", null);
	}

	public JComponent createComponent() {
		LcdJPanel panel = new LcdJPanel(squeeze.getLcdDisplay());
		panel.setScale(scale);
		panel.setWidth(xdiv);
		panel.setForeground(fgColor);
		if (bgColor != null)
			panel.setBackground(bgColor);

		squeeze.getLcdDisplay().setColour(fgColor, bgColor);

		return panel;
	}

	private class LcdJPanel extends JPanel implements LcdDisplayListener {
		private static final long serialVersionUID = -7331225089982532986L;

		private LcdDisplay display;

		private int scale = 1;
		
		private int xdiv = 1;

		private int width = LcdDisplay.SCREEN_WIDTH;

		private int height = LcdDisplay.SCREEN_HEIGHT;

		private Image frame;
		

		/** Creates a new instance of Vfcd */
		public LcdJPanel(LcdDisplay display) {
			this.display = display;
			setBorder(new EmptyBorder(2, 2, 2, 2));
			setOpaque(true);
		}

		public void updateDisplay(LcdDisplay display, Image frame) {
		    this.frame = frame;
		    repaint();
		}

        public int getScale() {
			return this.scale;
		}

		public void setScale(int scale) {
			this.scale = scale;
		}

		public void setWidth(int xdiv) {
			this.width = width/xdiv;
		}

		public void setBackground(Color background) {
			super.setBackground(background);
			setOpaque(true);
		}

		public Dimension getMinimumSize() {
			return getPreferredSize();
		}

		public Dimension getPreferredSize() {
			Insets insets = getInsets();
			return new Dimension(insets.left + (width * scale) + insets.right,
					insets.top + (height * scale) + insets.bottom);
		}

		public Dimension getMaximumSize() {
			return getPreferredSize();
		}

		public void addNotify() {
			super.addNotify();
			display.addListener(this);
		}

		public void removeNotify() {
			super.removeNotify();
			display.removeListener(this);
		}

		protected void paintComponent(Graphics g) {
		    if (frame != null) {
			    Graphics2D g2 = (Graphics2D) g;
		        g2.drawImage(frame, 0, 0, width*scale, height*scale, offset, 0, offset+width, height, this);
		    }
		}	
	}
}

