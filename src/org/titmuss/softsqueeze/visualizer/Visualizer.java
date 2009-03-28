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

import javax.sound.sampled.AudioFormat;

import org.apache.log4j.Logger;
import org.titmuss.softsqueeze.Softsqueeze;
import org.titmuss.softsqueeze.display.LcdDisplay;
import org.titmuss.softsqueeze.net.Protocol;


/**
 * @author Richard Titmuss
 *
 */
public abstract class Visualizer implements Runnable {
	protected static final Logger logger = Logger.getLogger("visualizer");

	protected int[] visParam;
	
    private byte buf[] = new byte[0];
    
    protected long bufEpoch;
    
    protected int bufLen;
	
	protected int channelPosition[] = new int[2];
	
    protected boolean channelFlipped[] = new boolean[2];

	protected int barWidth[] = new int[2];

	protected int barSpacing[] = new int[2];

	protected int barVal[][] = new int[2][];
	
	protected int barLvl[][] = new int[2][];

	protected int barIntensity[] = new int[2];

	protected int capIntensity[] = new int[2];
	
    protected int channels = 2;
    
    protected int sampleSize = 2;
    
    protected float frameRate = 44100.0f;
    
    protected boolean isBigEndian = false;
    
    protected boolean isMono = false;
    
    protected int displayRate = 30;
    
	private Softsqueeze squeeze;
	
	private LcdDisplay display;
	
	protected boolean running = true;

	
    public Visualizer(Softsqueeze squeeze, byte[] buf, int off, int len) {
        this.squeeze = squeeze;
        this.display = squeeze.getLcdDisplay();
    
        displayRate = 30;
        
        off++; // visType
        int visLen = buf[off++] & 0xFF;
        
        visParam = new int[visLen];
        for (int i=0; i<visLen && off < len; i++) {
            visParam[i] = Protocol.unpackN4(buf, off);
            off += 4;
        }
    }

    public synchronized void init() {
        Thread t = new Thread(this, "Visualizer");
		t.setDaemon(true);
    	running = true;
		t.start();
    }

    /**
     * Start the visualizer.
     */
    public synchronized void play() {        
    }

    /**
     * Pause the visualizer.
     */
    public synchronized void pause() {
        flush();
    }

    /**
     * Flush the visualizer buffer.
     */
    public synchronized void flush() {
        bufEpoch = 0;
        for (int i=0; i < barVal[0].length; i++) {
            barVal[0][i] = 0;
        }
        if (!isMono) {
            for (int i=0; i < barVal[1].length; i++) {
                barVal[1][i] = 0;
            }
        }
    }

    /**
     * Stop the visualizer, destroying the thread.
     */
    public synchronized void stop() {
        //flush();
        running = false;
        notifyAll();
        
        if (display != null)
        	display.updateVisualizer(this);
    }
    
    /**
     * Set the audio format.
     * @param format
     */
    public void setAudioFormat(AudioFormat format) {
        frameRate = format.getFrameRate();
        sampleSize = format.getSampleSizeInBits() / 8;
        channels = format.getChannels();
        isBigEndian = format.isBigEndian();
    }
    
    /**
     * Write audio data into the visualizer buffer.
     * @param b
     * @param offset
     * @param len
     */
    public synchronized void write(byte b[], int offset, int len) {
        if (buf.length < len)
            buf = new byte[len];
        
        System.arraycopy(b, offset, buf, 0, len);
        bufLen = len;
        bufEpoch = System.currentTimeMillis();
        notifyAll();
        
        logger.debug("write sample");
    }
    
    protected int getSample(int offset) {
        if (isBigEndian)
            return (buf[offset] << 8) | (buf[offset + 1] & 0xFF);
        else
            return (buf[offset] & 0xFF) | (buf[offset + 1] << 8);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public synchronized void run() {
        while (running) {
            while (running && bufLen == 0) {
            	logger.debug("paused");
                try {
                    wait();
                } catch (InterruptedException e1) {
                }
            	logger.debug("unpaused");
            }
            
            while (running && bufLen > 0) {
                long now = System.currentTimeMillis();                
                long offsetTime = now - bufEpoch;            
                int offset = (int)((offsetTime / 1000f) * frameRate) * channels * sampleSize;            
                logger.debug("offsetTime=" + offsetTime + " offset="+offset + " len="+bufLen);
                
                // FIXME double buffer the audio data, so we don't have to
                // synchronzie visualize(), this can block the audio thread.
                
                if (offset >= 0 && offset < bufLen) {
                    visualize(buf, offset, bufLen);
                }
                
                if (display != null)
                    display.updateVisualizer(this);
                
                try {
                    wait( (long)(2000.0 / displayRate));
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /**
     * Update the visualize values.
     * @param offset
     */
    protected abstract void visualize(byte[] buf, int offset, int len);
    
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
	    
	    boolean allZero = true;
	    
	    for (int ch=0; ch < ((isMono)?1:2); ch++) {
	        
	        if (!channelFlipped[ch]) {    	    
		        int pos = channelPosition[ch];
		        g2.setColor(col[barIntensity[ch]]);

		        for (int i=0; i<barVal[ch].length; i++) {
	                g2.fillRect(pos, LcdDisplay.FRAME_HEIGHT-2-barVal[ch][i], barWidth[ch], barVal[ch][i]+1);
	                pos += barWidth[ch] + barSpacing[ch];
	            }
	            
	            pos = channelPosition[ch];
	            g2.setColor(col[capIntensity[ch]]);

	            for (int i=0; i<barVal[ch].length; i++) {
	                if (barLvl[ch][i] > 0)
	                    barLvl[ch][i]--;		        
	                if (barLvl[ch][i] < barVal[ch][i])
	                    barLvl[ch][i] = barVal[ch][i];
	                if (barLvl[ch][i] != 0)
	                    allZero = false;
	                
	                g2.fillRect(pos, LcdDisplay.FRAME_HEIGHT-2-barLvl[ch][i], barWidth[ch], 1);
	                pos += barWidth[ch] + barSpacing[ch];
	            }
	        }
	        else {
		        int pos = channelPosition[ch];
		        g2.setColor(col[barIntensity[ch]]);

		        for (int i=barVal[ch].length-1; i>=0; i--) {
	                g2.fillRect(pos, LcdDisplay.FRAME_HEIGHT-2-barVal[ch][i], barWidth[ch], barVal[ch][i]+1);
	                pos += barWidth[ch] + barSpacing[ch];
	            }
	            
	            pos = channelPosition[ch];
	            g2.setColor(col[capIntensity[ch]]);

	            for (int i=barVal[ch].length-1; i>=0; i--) {
	                if (barLvl[ch][i] > 0)
	                    barLvl[ch][i]--;		        
	                if (barLvl[ch][i] < barVal[ch][i])
	                    barLvl[ch][i] = barVal[ch][i];	                
	                if (barLvl[ch][i] != 0)
	                    allZero = false;
	                
	                g2.fillRect(pos, LcdDisplay.FRAME_HEIGHT-2-barLvl[ch][i], barWidth[ch], 1);
	                pos += barWidth[ch] + barSpacing[ch];
	            }    	        
	        }
	    }
	    
	    if (allZero) {
	        synchronized (this) {
	            bufLen = 0;
	        }
	    }
	}
}
