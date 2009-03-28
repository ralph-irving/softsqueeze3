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

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.titmuss.softsqueeze.net.Protocol;
import org.titmuss.softsqueeze.net.ProtocolListener;
import org.w3c.dom.Element;

/**
 * @author richard
 */
public class KnobAction extends Action implements ProtocolListener {
    private int index = 0;
    
    private int length = 0;

    private int sync = 0;

    private int flags = 0;

    private int ircode2;
    
    private int ircode3;
    
    private long lastUpdate = 0;
    
    private boolean accel = false;
    
    
	public KnobAction(Skin skin, Element e) {
		super(skin, e);
		
		if (arg[1].startsWith("0x"))
		    ircode2 = Integer.parseInt(arg[1].substring(2), 16);
		else
		    ircode2 = Integer.parseInt(arg[1]);

		if (arg[2].startsWith("0x"))
		    ircode3 = Integer.parseInt(arg[2].substring(2), 16);
		else
		    ircode3 = Integer.parseInt(arg[2]);
		
        squeeze.getProtocol().addProtocolListener("knob", this);
	}
	
    /* (non-Javadoc)
     * @see org.titmuss.softsqueeze.skin.Action#dispose()
     */
    protected void dispose() {
        squeeze.getProtocol().removeProtocolListener("knob", this);
    }
    
    /* (non-Javadoc)
     * @see org.titmuss.softsqueeze.skin.Action#isKnobAction()
     */
    public boolean isKnobAction() {
        return true;
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent e) {
        switch(e.getButton()) {
        case MouseEvent.BUTTON2:
            squeeze.sendIR(1, 1, ircode2);
            break;
        
        case MouseEvent.BUTTON3:
            squeeze.sendIR(1, 1, ircode3);            
            break;
        }
    }
    /* (non-Javadoc)
     * @see java.awt.event.MouseWheelListener#mouseWheelMoved(java.awt.event.MouseWheelEvent)
     */
    public synchronized void mouseWheelMoved(MouseWheelEvent e) {
        long t = e.getWhen() - lastUpdate;
        lastUpdate = e.getWhen();
        
        accel = (t < 20 && length > 24) ? true : false;
        int newindex = index + ((accel) ? e.getWheelRotation()*10 : e.getWheelRotation());

        if (length != 0) {
            if (t < 500 && (newindex < 0 || newindex >= length)) {
            	if (newindex < 0)
            		newindex = 0;
            	if (newindex >= length)
            		newindex = length-1;
            	
            	if (newindex != index) {
            		index = newindex;
            		squeeze.sendKnob(index, sync);
            	}
                return;
            }

            if (newindex < 0 && ((flags & 0x01) == 0))
                newindex = length-1;
            if (newindex >= length && ((flags & 0x01) == 0))
                newindex = 0;
        }

        if (newindex != index) {
            logger.debug("mouse wheel moved " + newindex + " length " + length);
            index = newindex;
            squeeze.sendKnob(index, sync);
        }
    }
	
    /* (non-Javadoc)
     * @see org.titmuss.softsqueeze.net.ProtocolListener#slimprotoCmd(java.lang.String, byte[], int, int)
     */
    public synchronized void slimprotoCmd(String cmd, byte[] buf, int off, int len) {
        index = Protocol.unpackN4(buf, off);
        if (len > 10) {
            length = Protocol.unpackN4(buf, off+4);
            sync = buf[off+8];
            flags = buf[off+9];
        }
        
        logger.debug("knob command " + index + " " + length + " " + sync + " " + flags);
    }

    /* (non-Javadoc)
     * @see org.titmuss.softsqueeze.net.ProtocolListener#slimprotoConnected()
     */
    public void slimprotoConnected() {
    }

    /* (non-Javadoc)
     * @see org.titmuss.softsqueeze.net.ProtocolListener#slimprotoDisconnected()
     */
    public void slimprotoDisconnected() {
    }
}
