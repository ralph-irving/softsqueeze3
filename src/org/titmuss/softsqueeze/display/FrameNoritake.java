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

package org.titmuss.softsqueeze.display;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.titmuss.softsqueeze.net.Protocol;


/**
 * @author Richard Titmuss
 *
 */
public class FrameNoritake implements Frame {
	private static final Logger logger = Logger.getLogger("graphics");

	public static final int VFD_X_OFFSET = 40;

	public static final int VFD_Y_OFFSET = 8;

	private byte frame[] = new byte[320 * 2];

	private byte cgram[] = new byte[256 * 8];

	private int cgramAddr = 0;

	private int ddramAddr = 0;

	private int entryMode = 0x01;

	private boolean setDdram = true;
	
	private boolean fontLoaded = false;


	
	public FrameNoritake(LcdDisplay display, byte buf[], int start, int len) {
	    if (!fontLoaded) {
	        loadFont();
	        fontLoaded = true;
	    }
	    
		for (int i = start; i < len; i += 2) {
			int code = Protocol.unpackN2(buf, i);

			if ((code & 0xFF00) == 0x00) {
				logger
						.warn("vfd delay request by server - this is not implemented");
			} else if ((code & 0xFF00) == 0x0200) {
				if ((code & 0x80) == 0x80) { // Sets DDRAM address
					ddramAddr = code & 0x7F;
					setDdram = true;
					logger.debug("ddram address set " + ddramAddr);
				} else if ((code & 0x40) == 0x40) { // Sets CGRAM address
					cgramAddr = code & 0x3F;
					setDdram = false;
					logger.debug("cgram address set " + cgramAddr);
				} else if ((code & 0x20) == 0x20) { // Function set
				    int brightness = buf[i + 7];
				    display.setBrightness(brightness);
					i += 6; // Skip extra control chars
					logger.debug("function set - brightness " + brightness);
				} else if ((code & 0x10) == 0x10) { // Cursor or display shift
					logger.warn("cursor or display shift - not implmented");
				} else if ((code & 0x08) == 0x08) { // Display ON/OFF
					boolean displayOn = ((code & 0x04) == 0x04);
					display.setDisplayOn(displayOn);
					//// cursorOn = ((code&0x02) == 0x02);
					logger.debug("display/cursor on/off");
				} else if ((code & 0x04) == 0x04) { // Entry mode set
					entryMode = code & 0x03;
					logger.debug("entryMode set to " + entryMode + " (code "
							+ Integer.toString(code, 2) + ")");
				} else if ((code & 0x02) == 0x02) { // Cursor home
					ddramAddr = 0;
					setDdram = true;
					logger.debug("cursor home");
				} else if ((code & 0x01) == 0x01) { // Display clear
					for (int j = 0; j < frame.length; j++)
						frame[j] = 0x00;
					ddramAddr = 0;
					entryMode = 0x02;
					logger.debug("display clear");
				} else {
					logger.warn("vfd unrecognised instruction");
				}
			} else if ((code & 0xFF00) == 0x0300) {
				if (setDdram) {
					// copy character 'code' into frame buffer
					// pointed to by ddramAddr ...
					int c = (code & 0xFF) * 8;
					int ptr = 0;
					if (ddramAddr < 0x40) {
						ptr += ddramAddr * 12;
						for (int j = 0; j < 5; j++) {
							frame[ptr] = cgram[c++];
							ptr += 2;
						}
					} else {
						ptr += 1 + (ddramAddr - 0x40) * 12;
						for (int j = 0; j < 5; j++) {
							frame[ptr] = (byte) ((cgram[c++] & 0xFF) >> 1);
							ptr += 2;
						}
					}

					if (entryMode == 0x00) {
						ddramAddr--;
						if (ddramAddr < 0)
							ddramAddr = 0x68 - 1;
					} else if (entryMode == 0x01) {
						logger.debug("vfd shift - not implemented");
					} else if (entryMode == 0x02)
						ddramAddr++;
					if (ddramAddr >= 0x68)
						ddramAddr = 0;
					else if (entryMode == 0x03)
						logger.debug("vfd shift - not implemented");
				} else { // setCgram
					// need to rotate character grid ...
					byte c = (byte) (code & 0xFF);
					int idx = (int) (cgramAddr / 8.0) * 8 + 4;
					for (int j = 0; j < 5; j++, idx--) {
						cgram[idx] <<= 1;
						cgram[idx] |= (c & 0x01);
						c >>= 1;
					}
					//logger.debug("cgram["+cgramAddr+"] = "+(code&0xFF));
					if (entryMode == 0x00) {
						cgramAddr--;
						if (cgramAddr < 0)
							cgramAddr = 0x3F;
					} else if (entryMode == 0x01) {
						logger.debug("vfd shift - not implemented");
					} else if (entryMode == 0x02)
						cgramAddr++;
					if (cgramAddr >= 0x3F)
						cgramAddr = 0;
					else if (entryMode == 0x03)
						logger.debug("vfd shift - not implemented");
				}
			} else {
				logger.warn("vfd invalid code " + Integer.toString(code, 16));
			}
		}
	}

    
	/**
	 * Set the text on the display. Used by the 'firmware' while not connected
	 * to the slim server.
	 */
	public FrameNoritake(String line1, String line2) {
	    if (!fontLoaded) {
	        loadFont();
	        fontLoaded = true;
	    }
	    
		logger.debug("setText. line1=" + line1 + " line2=" + line2);
		byte bytes1[] = line1.getBytes();
		byte bytes2[] = line2.getBytes();

		for (int j = 0; j < frame.length; j++)
			frame[j] = 0x00;

		for (int i = 0; i < bytes1.length; i++) {
			int c = bytes1[i] * 8;
			int ptr = i * 12;
			for (int j = 0; j < 5; j++) {
				frame[ptr] = cgram[c++];
				ptr += 2;
			}
		}

		for (int i = 0; i < bytes2.length; i++) {
			int c = bytes2[i] * 8;
			int ptr = 1 + i * 12;
			for (int j = 0; j < 5; j++) {
				frame[ptr] = cgram[c++];
				ptr += 2;
			}
		}
	}

	public void render(Graphics g, int width, int offset, Color color[]) {
	    Graphics2D g2 = (Graphics2D)g;
	    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
	    g2.fillRect(0, 0, LcdDisplay.SCREEN_WIDTH, LcdDisplay.SCREEN_HEIGHT);

	    g2.setComposite(AlphaComposite.SrcOver);
		g.setColor(color[3]);

		int x = VFD_X_OFFSET;
		int y = VFD_Y_OFFSET;
		int col = 0;
		for (int i = 0; i < frame.length; i++) {
			int xcol = x + (col++);
			
			byte dots = frame[i];
			for (int j = 7; j >= 0; j--) {
				if ((dots & 0x01) > 0)
					g.fillRect(xcol, y + j, 1, 1);
				dots >>= 1;
			}

			dots = frame[++i];
			for (int j = 7; j >= 0; j--) {
				if ((dots & 0x01) > 0)
					g.fillRect(xcol, y + j + 8, 1, 1);
				dots >>= 1;
			}
		}
	}



	private void loadFont() {
		try {
			InputStream is = getClass().getResourceAsStream("vfdfont.txt");
			if (is == null) {
				logger.warn("Cannot load VfDisplay font");
				return;
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(is));

			int cgramAddr = 0;

			String line = br.readLine();
			while (line != null) {
				line = line.trim();

				if (line.equals("") || line.startsWith("#")) {
					line = br.readLine();
					continue;
				}
				if (line.startsWith("-") || line.startsWith("X")) {
					for (int i = 0; i < 5; i++) {
						if (line.charAt(i) == 'X')
							cgram[cgramAddr + i] |= 0x01;
						cgram[cgramAddr + i] <<= 1;
					}
				} else {
					int idx = Integer.parseInt(line.substring(0, 2), 16);
					cgramAddr = idx * 8;
				}

				line = br.readLine();
			}
		} catch (IOException e) {
			logger.warn("Cannot load VfDisplay font", e);
		}
	}
	
}
