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
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.ImageObserver;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.titmuss.softsqueeze.Softsqueeze;
import org.titmuss.softsqueeze.config.Config;
import org.titmuss.softsqueeze.config.ConfigListener;
import org.titmuss.softsqueeze.net.Protocol;
import org.titmuss.softsqueeze.net.ProtocolListener;
import org.titmuss.softsqueeze.visualizer.Visualizer;


/**
 * 
 * @author richard
 */
public class LcdDisplay implements ProtocolListener, ConfigListener, ImageObserver, Runnable {
	private static final Logger logger = Logger.getLogger("graphics");

	public static final int DISPLAY_NORITAKE = 0;

	public static final int DISPLAY_SQUEEZEBOXG = 1;

	public static final int DISPLAY_SQUEEZEBOX2 = 2;
			
	public static final int TOTAL_SCREENS = 2;
	
	public static final int SCREEN_WIDTH = 320;
	
	public static final int SCREEN_HEIGHT = 32; 
	
	public static final int FRAME_WIDTH = SCREEN_WIDTH * 2;
	
	public static final int FRAME_HEIGHT = SCREEN_HEIGHT; 
	
	private Softsqueeze squeeze;
	
	private int model;
	
	private int maxBrightness;

	private int tranDelay;
	
	private int brightness = 0;
	
	private boolean displayOn = true;

	private HashSet listeners = new HashSet();

	
	// render constants 
	private final static Color DEFAULT_FGCOLOUR = new Color(0x32EA1A);

	private final static Color DEFAULT_BGCOLOUR = Color.BLACK;

	private Color colours[] = new Color[4];
	
	private AlphaComposite compositeIn;

	private AlphaComposite compositeOver;

	
	// rendered frame
    private Image renderLive;
	
	private Image renderVis;
		
	private Screen[] screens = new Screen[TOTAL_SCREENS];

	
	/** Creates a new instance of Vfcd */
	public LcdDisplay(Softsqueeze squeeze) {
	    this.squeeze = squeeze;
	    
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	    GraphicsDevice gs = ge.getDefaultScreenDevice();
	    GraphicsConfiguration gc = gs.getDefaultConfiguration();
	    
	    // Create an image that supports transparent pixels
	    renderLive = gc.createCompatibleImage(FRAME_WIDTH, FRAME_HEIGHT, Transparency.OPAQUE);
	    renderVis = gc.createCompatibleImage(FRAME_WIDTH, FRAME_HEIGHT, Transparency.BITMASK);
		
	    for (int i=0; i<TOTAL_SCREENS; i++)
	        screens[i] = new Screen(i * SCREEN_WIDTH, gc);
	    
		Thread t = new Thread(this, "Display animation");
		t.setDaemon(true);
		t.start();
	    
		squeeze.getProtocol().addProtocolListener("grfd", this);
		squeeze.getProtocol().addProtocolListener("grfe", this);
		squeeze.getProtocol().addProtocolListener("grff", this);
		squeeze.getProtocol().addProtocolListener("grfb", this);
		squeeze.getProtocol().addProtocolListener("vfdc", this);
		
		Config.addConfigListener(this);
		setEmulation(Config.getProperty("skin.displayemulation"));
	}

	/* (non-Javadoc)
     * @see com.slim.softsqueeze.ConfigListener#configSet(java.lang.String, java.lang.String)
     */
    public void configSet(String key, String value) {
        if (key.equals("displayemulation"))
            setEmulation(value);
    }

    /* (non-Javadoc)
	 * @see com.slim.softsqueeze.ProtocolListener#slimprotoCmd(java.lang.String, byte[], int, int)
	 */
    public void slimprotoCmd(String cmd, byte[] buf, int off, int len) {
        if (cmd.equals("grfd")) {
            int offset = Protocol.unpackN2(buf, off);
            
            switch (model) {
            case DISPLAY_SQUEEZEBOXG: {
                Frame frame = new FrameD(buf, off + 2, (len - off - 2), false );
                renderDisplay(cmd, 'c', 0, frame, offset, 320);
                break;
            }
            case DISPLAY_SQUEEZEBOX2: {
                char transition = (char)(buf[off+2]);
                int tParam = buf[off+3];
                int width = (len - off - 4) / 2;
                
                Frame frame = new FrameD(buf, off + 4, (len - off - 4), true );
                renderDisplay(cmd, 'c', tParam, frame, offset, width);
                break;
            }
            }
        }
        else if (cmd.equals("grfe")) {
            int offset = Protocol.unpackN2(buf, off);
            char transition = (char)(buf[off+2]);
            int tParam = buf[off+3];
            int width = (len - off - 4) / 4;

            Frame frame = new FrameE(buf, off + 4, (len - off -4) );
            renderDisplay(cmd, transition, tParam, frame, offset, width);
        }
        else if (cmd.equals("grff")) {
            int offset = Protocol.unpackN2(buf, off);
            char transition = (char)(buf[off+2]);
            int tParam = buf[off+3];
            int width = (len - off - 4) / 8;

            Frame frame = new FrameF(buf, off + 4, (len - off - 4) );
            renderDisplay(cmd, transition, tParam, frame, offset, width);
        }
        else if (cmd.equals("grfb")) {
            setBrightness(Protocol.unpackN2(buf, off));
        }
        else if (cmd.equals("vfdc")) {
            Frame frame = new FrameNoritake(this, buf, off, len);
            renderDisplay(cmd, 'c', 0, frame, 0, 320);
        }
    }

	/* (non-Javadoc)
	 * @see com.slim.softsqueeze.ProtocolListener#slimprotoConnected()
	 */
	public void slimprotoConnected() {
		clearDisplay();
	}

	/* (non-Javadoc)
	 * @see com.slim.softsqueeze.ProtocolListener#slimprotoDisconnected()
	 */
	public void slimprotoDisconnected() {
		clearDisplay();
	    setText("Problem: Lost contact with Slim Server.",
	    		"Check the software is running."
	            );
	}

    /**
     * Update the visualizer data.
     * 
     * @param visualizer
     */
    public void updateVisualizer(Visualizer visualizer) {
        visualizer.render(renderVis.getGraphics(), colours);
        updateDisplay();
    }

    /**
     * Set the display type.
     * @param displayType
     */
    private void setEmulation(String displayType) {	    
        if (displayType.equalsIgnoreCase("Noritake")){
            model = DISPLAY_NORITAKE;
            maxBrightness = 32;
        }	    
        else if (displayType.equalsIgnoreCase("Graphics")) {
            model = DISPLAY_SQUEEZEBOXG;
            maxBrightness = 32;
        }
        else {
            model = DISPLAY_SQUEEZEBOX2;
            maxBrightness = 8;
        }
        
        setColour(DEFAULT_FGCOLOUR, DEFAULT_BGCOLOUR);
        setBrightness(maxBrightness);
    }

    
    public void setText(String str1, String str2) {
	    setBrightness(maxBrightness);
	    Frame frame = new FrameNoritake(str1, str2);
	    
        renderDisplay("vfdc", 'c', 0, frame, 0, SCREEN_WIDTH);	    
    }
    
    /**
     * @param b set the display brighness; 0 - maxBrightness
     */
    public void setBrightness(int b) {
        if (model == DISPLAY_SQUEEZEBOX2) {
            switch (b) {
            case 65535:
                b = 0;
                break;
            case 0:
                b = 1;
                break;
            case 1:
                b = 2;
                break;
            case 3:
                b = 4;
                break;
            case 4:
                b = 8;
                break;
            default:
                b = 8;
            }
        }
        
        brightness = Math.min(b, maxBrightness); 

        float f = 1 - (float)Math.pow(1 - ((float)brightness / (float)maxBrightness), 2);        
        compositeIn = AlphaComposite.getInstance(AlphaComposite.SRC_IN, f);
        compositeOver = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, f);
        logger.debug("brightness=" + brightness + " f="+f);

        updateDisplay();			
    }

    /**
     * @param displayOn set the display on or off.
     */
    public void setDisplayOn(boolean displayOn) {
        this.displayOn = displayOn;
        
        updateDisplay();
    }
    
    /**
     * Set the foreground colour for the display.
     */
    public void setColour(Color fg, Color bg) {
        colours[3] = new Color(
                (int)(fg.getRed()),
                (int)(fg.getGreen()),
                (int)(fg.getBlue())
        );
        colours[2] = new Color(
                (int)(colours[3].getRed() * 0.80f),
                (int)(colours[3].getGreen() * 0.80f),
                (int)(colours[3].getBlue() * 0.80f)
        );
        colours[1] = new Color(
                (int)(colours[2].getRed() * 0.60f),
                (int)(colours[2].getGreen() * 0.60f),
                (int)(colours[2].getBlue() * 0.60f)
        );
        colours[0] = bg; // background
        
        updateDisplay();
    }

    

	/**
	 * Clear framebuffer
	 */
	public void clearDisplay() {
	    renderDisplay("grfe", 'c', 0, new FrameE(new byte[0], 0, 0), 0, FRAME_WIDTH);
	}


	public void addListener(LcdDisplayListener panel) {
		listeners.add(panel);		
		updateDisplay();
	}

	public void removeListener(LcdDisplayListener panel) {
		listeners.remove(panel);
	}

	public boolean isDisplayOn() {
		return displayOn;
	}

	/**
	 * @return display brightness, range 0-255.
	 */
	public int getBrightness() {
		return brightness;
	}

	public int getMaxBrightness() {
		return maxBrightness;
	}
	
	public int getModel() {
	    return model;
	}

    /**
     * @param cmd
     * @param transition
     * @param tParam TODO
     * @param width TODO
     * @param frame2
     */
    private void renderDisplay(String cmd, char transition, int tParam, Frame frame, int offset, int width) {
        logger.debug("renderDisplay cmd="+cmd+" tran="+transition+" tParam="+tParam+" width="+width);
        
        int newTranDelay = 0;
        if (offset == 0 && width == FRAME_WIDTH) {
            // double width frame - both screens
            screens[0].renderFrame(frame, 0, colours);
            screens[0].setTransition(transition, tParam);
            screens[1].renderFrame(frame, SCREEN_WIDTH, colours);
            newTranDelay = screens[1].setTransition(transition, tParam);
        }
        else if (offset == 0 && width <= SCREEN_WIDTH) {
            // screen 1 only
            screens[0].renderFrame(frame, 0, colours);
            newTranDelay = screens[0].setTransition(transition, tParam);
        }
        else if (offset == SCREEN_WIDTH*2 && width <= SCREEN_WIDTH) {
            // screen 2 only
            screens[1].renderFrame(frame, 0, colours);
            newTranDelay = screens[1].setTransition(transition, tParam);
        }
        else {
            logger.error("Cannot render display offset=" + offset + " width=" + width);
            return;
        }
        
        if (newTranDelay > 0) {
        	synchronized(this) {
        		tranDelay = newTranDelay;
        		notifyAll();
        	}
        }
        updateDisplay();
    }

    protected void animiationComplete() {
        squeeze.sendANIC();
    }
    
    /**
     * Composite the renders, and update the display.
     */
	protected synchronized void updateDisplay() {
		logger.debug("request display repaint ...");
		
		if (listeners.size() == 0)
		    return;
		
		Graphics2D g = (Graphics2D)renderLive.getGraphics();
		g.setColor(colours[0]);
	    g.fillRect(0, 0, LcdDisplay.FRAME_WIDTH, LcdDisplay.FRAME_HEIGHT);
		
	    if (displayOn) {
	        g.setComposite(compositeOver);
	        g.drawImage(renderVis, 0, 0, this);
	        
	        g.setComposite(compositeOver);
	        screens[1].updateScreen(g);
	        screens[0].updateScreen(g);
	    }
	    
		for (Iterator i = listeners.iterator(); i.hasNext();) {
			((LcdDisplayListener) i.next()).updateDisplay(this, renderLive);
		}
	}

	public boolean imageUpdate(Image arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
		return false;
	}
	
	public synchronized void run() {
	    while (true) {
	        try {
	        	while (tranDelay == 0) {
	        		try {
	        			wait();
	        		} catch (InterruptedException e) {
	        		}
	        	}

	            while (tranDelay > 0) {
	                try {
	                    wait(tranDelay);
	                } catch (InterruptedException e) {
	                }
	                
	                if (screens[0].animate()) {
	                	animiationComplete();
	                	tranDelay = 0;
	                }
	                if (screens[1].animate()) {
	                	animiationComplete();
	                	tranDelay = 0;
	                }
	                updateDisplay();
	            }
	        }
	        catch (Exception e) {
	            logger.error("Error in graphics transition", e);
	        }
	    }
	}	
}

