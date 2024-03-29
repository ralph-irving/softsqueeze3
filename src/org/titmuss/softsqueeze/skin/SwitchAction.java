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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import org.w3c.dom.Element;


/**
 * Emulate IR button
 */
public class SwitchAction extends Action implements ActionListener {

	private int ircode; // ir button code

	public SwitchAction(Skin skin, Element e) {
		super(skin, e);

		if (arg[1].startsWith("0x"))
		    ircode = Integer.parseInt(arg[1].substring(2), 16);
		else
		    ircode = Integer.parseInt(arg[1]);
	}


	public void mousePressed(MouseEvent e) {
		logger.debug("Button pressed " + ircode);
		squeeze.sendButton(ircode | 0x00010000);
	}

	public void mouseReleased(MouseEvent e) {
		logger.debug("Button released " + ircode);
		squeeze.sendButton(ircode | 0x00020000);
	}

    /* (non-Javadoc)
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    public void keyPressed(KeyEvent e) {
		logger.debug("Key pressed " + ircode);
		squeeze.sendButton(ircode | 0x00010000);
    }


	public void actionPerformed(ActionEvent arg0) {
		logger.debug("Action " + ircode);
		squeeze.sendButton(ircode | 0x00010000);
	}
}

