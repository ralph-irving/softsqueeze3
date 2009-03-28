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

package org.titmuss.softsqueeze;

import javax.swing.JOptionPane;

import org.titmuss.softsqueeze.platform.Platform;

/**
 * 
 * @author richard
 */
public class Application {

	private static void error(String msg) {
		System.err.println(msg);

		String skinProperty = System.getProperty("skins", "");
		if (skinProperty.indexOf("headless") == -1) {
			JOptionPane.showMessageDialog(null, msg,
					"Error starting SoftSqueeze", JOptionPane.ERROR_MESSAGE);
		}

		System.exit(-1);
	}

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		try {
			if (!Platform.JRE_1_4_PLUS)
				error("Please upgrade to Java 1.4");
			
			Softsqueeze.main(args);
		} catch (NoClassDefFoundError e) {
			error("Failed to find SoftSqueeze libraries.");
		} catch (Exception e) {
			StringBuffer msg = new StringBuffer("Error starting SoftSqueeze: ");
			msg.append(e.getMessage());
			msg.append('\n');
			
			StackTraceElement stackTrace[] = e.getStackTrace();
			for (int i=0; i<stackTrace.length; i++) {
				msg.append(stackTrace[i].toString());
				msg.append('\n');
			}
			error(msg.toString());
		}
	}
}

