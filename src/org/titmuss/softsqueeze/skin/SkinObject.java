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
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.log4j.Logger;
import org.titmuss.softsqueeze.Softsqueeze;
import org.titmuss.softsqueeze.util.Util;
import org.w3c.dom.Element;


public abstract class SkinObject {
	protected static Logger logger = Logger.getLogger("skin");

	protected Softsqueeze squeeze;

	protected Skin skin;

	protected String id;

	protected String name;

	protected int width;

	protected int height;

	public SkinObject(Skin skin, Element e) {
		this.squeeze = skin.getSoftSqueeze();
		this.skin = skin;

		id = e.getAttribute("id");
		name = e.getAttribute("name");
		width = parseIntAttribute(e, "width", -1);
		height = parseIntAttribute(e, "height", -1);
	}

	public String getId() {
		return id;
	}

	/**
	 * Parse an integer value from the attribute <i>name </i>. Returns given
	 * default value if the attribute does not exist.
	 */
	protected int parseIntAttribute(Element e, String name, int def) {
		String val = e.getAttribute(name);
		if (val.equals(""))
			return def;
		return Integer.parseInt(val);
	}

	protected int parseHexAttribute(Element e, String name, int def) {
		String val = e.getAttribute(name);
		if (val.equals(""))
			return def;
		if (val.startsWith("0x"))
			return Integer.parseInt(val.substring(2), 16);
		else
			return Integer.parseInt(val.substring(2));
	}

	protected String parseStringAttribute(Element e, String name, String def) {
		String val = e.getAttribute(name);
		if (val.equals(""))
			return def;
		return val;
	}

	protected boolean parseBooleanAttribute(Element e, String name, boolean def) {
		String val = e.getAttribute(name);
		if (val.equals(""))
			return def;
		return val.equalsIgnoreCase("true");
	}

	protected ImageIcon parseImageAttribute(Element e, String name,
			ImageIcon def) {
		String val = e.getAttribute(name);
		if (val.equals(""))
			return def;

		logger.debug("loading image: " + val);
		java.net.URL url = skin.getResource(val);
		if (url == null) {
			logger.warn("No image: " + val);
			return null;
		}
		return new ImageIcon(url);
	}

	protected BufferedImage parseBufferedImageAttribute(Element e, String name,
			BufferedImage def) {
	    try {
	        String val = e.getAttribute(name);
	        if (val.equals(""))
	            return def;
	        
	        logger.debug("loading image: " + val);
	        java.net.URL url = skin.getResource(val);
	        if (url == null) {
	            logger.warn("No image: " + val);
	            return null;
	        }
	        return ImageIO.read(url);
	    } catch (IOException e1) {
	        logger.warn("Exception loading image " + name, e1);
	        return null;
	    }
	}	
	
	/**
	 * Parse an color value from the attribute <i>name </i>. Returns given
	 * default value if the attribute does not exist.
	 */
	protected Color parseColorAttribute(Element e, String name, Color def) {
		String val = e.getAttribute(name);
		if (val.equals(""))
			return def;
		return Color.decode(val);
	}

	/**
	 * Parse a font value from the attribute <i>name </i>. Returns given default
	 * value if the attribute does not exist.
	 */
	protected Font parseFontAttribute(Element e, String fontName,
			String sizeName, Font def) {
		String val = e.getAttribute(name);
		if (val.equals(""))
			return def;

		int size = parseIntAttribute(e, sizeName, 10);
		String fonts[] = Util.split(val, ",");
		for (int i = 0; i < fonts.length; i++) {
			Font font = new Font(fonts[i], Font.PLAIN, size);
			if (font != null)
				return font;
		}

		return def;
	}
}

