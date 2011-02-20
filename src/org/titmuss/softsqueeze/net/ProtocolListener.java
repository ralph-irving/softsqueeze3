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
package org.titmuss.softsqueeze.net;

/**
 * @author richard
 *
 * ProtocolListeners are called when commands are received from the
 * Squeezebox Server. 
 */
public interface ProtocolListener {
	/**
	 * Called when a slimproto command is received.
	 * 
	 * @param cmd the slimproto command
	 * @param buf command buffer
	 * @param off offset into command buffer
	 * @param len length of data in the command buffer
	 */
	public void slimprotoCmd(String cmd, byte buf[], int off, int len);
	
	public void slimprotoConnected();
	
	public void slimprotoDisconnected();
}
