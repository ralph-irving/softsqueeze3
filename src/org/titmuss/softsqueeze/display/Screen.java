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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Transparency;


class Screen {
    private Image renderBuf;

    private Image renderOut;	
    
    private int offset;
    
	private char tranType;
	
	private int tranDelay;

	private int frameX;

	private int frameY;

	private int frameWidth;

	private int frameHeight;

	private int tranX;

	private int tranY;

	private int incX;

	private int incY;

    private boolean animate = false;

    
    protected Screen(int offset, GraphicsConfiguration gc) {
        this.offset = offset;
        
        renderBuf = gc.createCompatibleImage(LcdDisplay.SCREEN_WIDTH, LcdDisplay.SCREEN_HEIGHT, Transparency.BITMASK);
        renderOut = gc.createCompatibleImage(LcdDisplay.SCREEN_WIDTH, LcdDisplay.SCREEN_HEIGHT, Transparency.BITMASK);	        
    }
    
    protected void renderFrame(Frame frame, int offset, Color[] colours) {
        Image tmp = renderBuf;
        renderBuf = renderOut;
        renderOut = tmp;
        
        frame.render(renderBuf.getGraphics(), LcdDisplay.SCREEN_WIDTH, offset, colours);
    }
    
    protected void killAnimation() {
        animate = false;	        
    }
	
	protected void updateScreen(Graphics2D g) {
        if (!animate) {
            g.setClip(offset, 0, LcdDisplay.SCREEN_WIDTH, LcdDisplay.SCREEN_HEIGHT);
            g.drawImage(renderBuf, offset, 0, null);
        }
        else {
            doUpdateScreen(g, renderBuf, renderOut, offset);
        }
	}
	
	protected synchronized int setTransition(char transition, int param) {
        tranType = transition;
        frameX = 0;
        tranX = 0;
        incX = 0;
        frameY = 0;
        tranY = 0;
        incY = 0;
        frameWidth = LcdDisplay.SCREEN_WIDTH;
        frameHeight = LcdDisplay.SCREEN_HEIGHT;
        
        switch (transition) {
        case 'l':
            animate = true;
            tranDelay = 5; // ms
            frameX = -LcdDisplay.SCREEN_WIDTH;
            tranX = 0;
            incX = 1;
            break;
            
        case 'r':
            animate = true;
            tranDelay = 5; // ms
            frameX = LcdDisplay.SCREEN_WIDTH;
            tranX = 0;
            incX = -1;
            break;
        
        case 'u':
            animate = true;
            tranDelay = 10; // ms
            frameY = -param;
            tranY = 0;
            incY = 1;
            frameHeight = param;
            break;
            
        case 'd':
            animate = true;
            tranDelay = 10; // ms
            frameY = param;
            tranY = 0;
            incY = -1;            
            frameHeight = param;
            break;

        case 'L':
            animate = true;
            tranDelay = 15; // ms
            frameX = 8;
            tranX = 0;
            incX = -1;
            break;
            
        case 'R':
            animate = true;
            tranDelay = 15; // ms
            frameX = -8;
            tranX = 0;
            incX = 1;
            break;
            
        case 'U':
            animate = true;
            tranDelay = 20; // ms
            frameY = -8;
            tranY = 0;
            incY = 1;
            break;
            
        case 'D':
            animate = true;
            tranDelay = 20; // ms
            frameY = 8;
            tranY = 0;
            incY = -1;
            break;

        default:
            animate = false;
            tranType = 'c';
        }

        return tranDelay;
	}
	
	protected synchronized boolean animate() {
        if (tranType == 'c')
            return false;
        
        if (frameX < -100 || frameX > 100) {
            frameX += incX * 12;
            tranX += incX * 12;
        }
        else if (frameX < -50 || frameX > 50) {
            frameX += incX * 4;
            tranX += incX * 4;
        }
        else if (frameX < -25 || frameX > 25) {
            frameX += incX * 2;
            tranX += incX * 2;
        }
        else {
            frameX += incX * 1;
            tranX += incX * 1;	                    
        }
        
        if (frameY < -16 || frameY > 16) {
            frameY += incY * 2;
            tranY += incY * 2;
        }
        else {
            frameY += incY * 1;
            tranY += incY * 1;
        }
        
        if (frameX * incX > 0) frameX = 0;
        if (frameY * incY > 0) frameY = 0;
        
        if (frameX == 0 && frameY == 0) {
            tranType = 'c';
            return true;
        }
        
        return false;
	}
	
	private synchronized void doUpdateScreen(Graphics2D g, Image renderBuf, Image renderTran, int offset) {
        switch (tranType) {
        
        case 'l':
        case 'r':
            g.setClip(offset, 0, LcdDisplay.SCREEN_WIDTH, LcdDisplay.SCREEN_HEIGHT);
            g.drawImage(renderBuf, frameX + offset, frameY, null);
            g.drawImage(renderTran, tranX + offset, tranY, null);
            break;

        case 'u':
        case 'd':
            int clipHeight = LcdDisplay.SCREEN_HEIGHT-frameHeight;
            
            g.setClip(offset, 0, LcdDisplay.SCREEN_WIDTH, clipHeight);
            g.drawImage(renderBuf, offset, 0, null);

            g.setClip(offset, clipHeight, LcdDisplay.SCREEN_WIDTH, LcdDisplay.SCREEN_HEIGHT);
	        g.drawImage(renderBuf,
	                	frameX + offset, frameY+clipHeight, 
	                	frameX + offset + frameWidth, frameY+clipHeight+frameHeight,
	                	0, clipHeight, 
	                	frameWidth, clipHeight+frameHeight,
	                	null);

	        g.drawImage(renderTran,
	                	tranX + offset, tranY+clipHeight, 
	                	tranX + offset + frameWidth, tranY+clipHeight+frameHeight,
	                	0, clipHeight, 
	                	frameWidth, clipHeight+frameHeight,
	                	null);
            break;
            
        case 'L':
        case 'R':
        case 'U':
        case 'D':
            // Bump animations
            g.setClip(offset, 0, LcdDisplay.SCREEN_WIDTH, LcdDisplay.SCREEN_HEIGHT);
	        g.drawImage(renderBuf, frameX + offset, frameY, null);	        
            break;
            
        default:
            // Simply draw frame
            g.setClip(offset, 0, LcdDisplay.SCREEN_WIDTH, LcdDisplay.SCREEN_HEIGHT);
            g.drawImage(renderBuf, offset, 0, null);
        }	    
	}

}