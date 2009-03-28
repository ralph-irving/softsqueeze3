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

package org.titmuss.softsqueeze.audio;

/**
 * 
 * @author Dolf.Dijkstra
 */
public class AudioException extends java.lang.Exception {

	private static final long serialVersionUID = 3978497099899940740L;

	/**
	 * Creates a new instance of <code>PlayerException</code> without detail
	 * message.
	 */
	public AudioException() {
	}

	/**
	 * Constructs an instance of <code>PlayerException</code> with the
	 * specified detail message.
	 * 
	 * @param msg
	 *            the detail message.
	 */
	public AudioException(String msg) {
		super(msg);
	}

	public AudioException(String msg, Exception ex) {
		super(msg, ex);
	}

	public AudioException(Exception ex) {
		super(ex);
	}

}