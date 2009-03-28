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

package org.titmuss.softsqueeze.visualizer;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.titmuss.softsqueeze.Softsqueeze;
import org.titmuss.softsqueeze.display.LcdDisplay;
import org.titmuss.softsqueeze.net.Protocol;


/**
 * @author Richard Titmuss
 *
 * VU Meter - Simple visualizer that shows the root-mean-square (RMS)
 */
public class VisualizerVUMeter extends Visualizer {
	private static final Logger logger = Logger.getLogger("visualizer");

	private static final int SAMPLE_WINDOW = 1024;
	
	private static final int DIGITAL_RMS_MAP[] = new int[] {
	        0, 10, 57, 159, 326, 570, 900, 1323, 1847, 2480, 3227
	};

    private static final int ANALOG_RMS_MAP[] = new int[] {
            0, 1, 4, 8, 14, 22, 33, 46, 62, 81, 102, 128, 156, 188, 223, 
            262, 305, 352, 403, 459, 518, 582, 650, 724, 801, 884, 972, 
            1064, 1162, 1264, 1372, 1486, 1605, 1729, 1859, 1995, 2136, 
            2284, 2437, 2596, 2761, 2933, 3111, 3295, 3485, 3682, 3885, 4096
    };

    
    private static final int RENDER_DIGITAL = 0;

    private static final int RENDER_ANALOG = 1;
    
    private static final int RENDER_CUSTOM = 2;
    
    private int renderStyle;
    
    private int damping;
    
    private boolean barReversed[] = new boolean[2];

    private static Image analogImage;

    
    public VisualizerVUMeter(Softsqueeze squeeze, byte[] buf, int off, int len) {
        super(squeeze, buf, off, len);
    
        int p = 0;

        isMono = (visParam[p++] == 1);
        
        switch (visParam[p++]) {
        case 0:
        	renderStyle = RENDER_DIGITAL;
        	break;
        case 1:
        	renderStyle = RENDER_ANALOG;
            analogImage = null;
        	break;
        case 2:
        	renderStyle = RENDER_CUSTOM;
        	break;
        }        	
        
        channelPosition[0] = visParam[p++];
        barWidth[0] = Math.abs(visParam[p]);
        barReversed[0] = (visParam[p++] < 0);
        barVal[0] = new int[1];
        barLvl[0] = new int[1];
        
        if (!isMono) {
            channelPosition[1] = visParam[p++];
            barWidth[1] = Math.abs(visParam[p]);
            barReversed[1] = (visParam[p++] < 0);
            barVal[1] = new int[1];                    
            barLvl[1] = new int[1];   
        }
        else {
            channelPosition[1] = -1;
        }

        damping = (p < visParam.length) ? visParam[p++] : 0;
        displayRate = (p < visParam.length) ? visParam[p++] : 30;
    }

    protected void visualize(byte[] buf, int offset, int bufLen) {
        int numAccumulated = 0;
        
        int numRead = Math.min(bufLen - offset, SAMPLE_WINDOW);
        int numSamples = numRead / (2 * sampleSize);
        
        int sampleAccumulator[] = new int[2];

        for (int j=0; j<numSamples; j++) {
            int sample = getSample(offset) >> 8;
            int sampleSq = sample * sample;
            sampleAccumulator[0] += sampleSq & 0x0000ffff;
            offset += 2;
            
            sample = getSample(offset) >> 8;
            sampleSq = sample * sample;
            sampleAccumulator[1] += sampleSq & 0x0000ffff;
            offset += 2;
        }
        
        numAccumulated += numSamples;
        
        if (isMono) {
            sampleAccumulator[0] += sampleAccumulator[1];
            sampleAccumulator[0] /= (2 * numAccumulated);
        }
        else {
            sampleAccumulator[0] /= numAccumulated;
            sampleAccumulator[1] /= numAccumulated;
        }
        
        barVal[0][0] = sampleAccumulator[0];
        barVal[1][0] = sampleAccumulator[1];        
    }
    
	/**
	 * @param graphics
	 * @param visualizer
	 */
	public void render(Graphics g, Color col[]) {
	    Graphics2D g2 = (Graphics2D)g;
	    
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 1.0f));
	    g2.fillRect(0, 0, LcdDisplay.FRAME_WIDTH, LcdDisplay.FRAME_HEIGHT);
	    g2.setComposite(AlphaComposite.Src);

	    if (!running)
	        return;

	    int rmsMap[] = (renderStyle == RENDER_DIGITAL) ? DIGITAL_RMS_MAP : ANALOG_RMS_MAP;
	    
	    for (int ch=0; ch<2; ch++) {
	    	int val = 0;
	    	for (int i = rmsMap.length-1; i > 0; i--) {
	    		if (barVal[ch][0] >= rmsMap[i]) {
	    			val = i;
	    			break;
	    		}
	    	}
        
	    	switch (damping) {
			case 0:
				if (barLvl[ch][0] > 0)
					barLvl[ch][0]--;
				if (barLvl[ch][0] < val)
					barLvl[ch][0] = val;
				break;

			case 1:
				if (barLvl[ch][0] < val) {
					barLvl[ch][0]++;
				} else if (barLvl[ch][0] > 0) {
					barLvl[ch][0]--;
				}
				break;

			case 2:
				barLvl[ch][0] = val;
				break;
			}
		}
	    
	    if (renderStyle == RENDER_DIGITAL) {
	        renderDigital(g2, col, 0);
	        if (!isMono)
	            renderDigital(g2, col, 1);
	    }
	    else {
	        renderAnalog(g2, col, 0);
	        if (!isMono)
	            renderAnalog(g2, col, 1);
	    }

	    if (barLvl[0][0] == 0 && barLvl[1][0] == 0) {
	        synchronized (this) {
	            bufLen = 0;
	        }
	    }
	}
	 
	private void renderDigital(Graphics2D g2, Color col[], int ch) {
        int pos = channelPosition[ch];
        for (int i=0; i<barLvl[ch][0]+1; i++) {
            if (i <= 2)
                g2.setColor(col[1]);        
            else if (i <= 6)
                g2.setColor(col[2]);
            else
                g2.setColor(col[3]);
            
            g2.fillRect(pos, LcdDisplay.FRAME_HEIGHT-2-(i*3), barWidth[ch], 2);
        }        
	}
	
	private void renderAnalog(Graphics2D g2, Color col[], int ch) {
	    if (analogImage == null) {
	    	if (renderStyle != RENDER_ANALOG) {
	    		return;
	    	}	    
	    	loadImage(col);
	    }	   
	    
        int pos = channelPosition[ch];
        int offset = -barWidth[ch]*barLvl[ch][0];
        
        AffineTransform t = AffineTransform.getTranslateInstance(offset+pos, 0);
        g2.setClip(pos, 0, barWidth[ch], 32);

        if (barReversed[ch]) {
        	t.preConcatenate(AffineTransform.getScaleInstance(-1,1));
        }

        g2.drawImage(analogImage, t, null);        	
	}
	
	private void loadImage(Color col[]) {
	    try {
	        InputStream is = getClass().getResourceAsStream("vumeter.png");
	        analogImage = changeColor(ImageIO.read(is), col[3]);
	        is.close();
	    } catch (IOException e) {
	        logger.error("Cannot read analogue vu meter image", e);
	    }
	}


	public Image changeColor(BufferedImage image, Color newColor) {
	    int height = image.getHeight();
	    int width = image.getWidth();
	    
	    double r = newColor.getRed() / 255.0d;
	    double g = newColor.getGreen() / 255.0d;
	    double b = newColor.getBlue() / 255.0d;
	    
	    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	    
	    for (int y = 0; y < height; y++) {
	        for (int x = 0; x < width; x++) {	          
	            int rgba = image.getRGB(x, y);
	            int red = ((rgba >> 16) & 0xff);
	            int green = ((rgba >> 8) & 0xff);
	            int blue = (rgba & 0xff);
	            int alpha = (rgba >> 24) & 0xff;
	            
	            red *= r;
	            green *= g;
	            blue *= b;
	            
	            rgba = (alpha << 24) | (red << 16) | (green << 8) | blue;
	            img.setRGB(x, y, rgba);
	        }
	    }
	    
	    return img;
	}
	
	public static void uploadGraphic(byte[] buf, int off, int len) {
		int frameOffset = Protocol.unpackN4(buf, off); off+=4;
		int frameLength = len - off - 4;
		
		if (frameOffset == 0) {
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		    GraphicsDevice gs = ge.getDefaultScreenDevice();
		    GraphicsConfiguration gc = gs.getDefaultConfiguration();
		    
		    // Create an image that supports transparent pixels
		    analogImage = gc.createCompatibleImage(LcdDisplay.SCREEN_WIDTH*48, LcdDisplay.SCREEN_HEIGHT, Transparency.OPAQUE);
		}
		
		Graphics2D g2 = (Graphics2D) analogImage.getGraphics();
		
		Color color[] = new Color[] {
				Color.BLACK,
				new Color(0x28bb14),
				new Color(0x1e8c0f),
				new Color(0x32fa1a),				
		};
		
		for (int i = 0; i < frameLength; i++) {
			int x = (frameOffset + i) / 8;
			int y = 32 - ((frameOffset + i) % 8) * 4;
						
			byte dots = buf[off++];
			for (int j = 0; j < 8; j+=2) {
				int c = (dots >> j) & 0x03;
			
				g2.setColor(color[c]);
				g2.drawLine(x, y, x, y-1);
				y--;
			}			
		}
	}
}
