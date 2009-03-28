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

package org.titmuss.softsqueeze.config;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;

import org.titmuss.softsqueeze.platform.Platform;

/**
 * @author Richard Titmuss
 *
 */
public class ConfigPopup {
	private static final String OK = "OK";
	private static final String DOWNLOAD = "Download now";
	

	public static void showOnceDialog(String configCheck, String message,
			String title) {
	    showOnceDialog(configCheck, null, message, title);
	}
	
	public static void showOnceDialog(String configCheck, String downloadUrl, 
	        String message,	String title) {
	    
	    if (Config.isHeadless()) {
	        System.out.println(message);
	        return;
	    }
	    
		if (Config.getBooleanProperty(configCheck))
			return;

		JCheckBox check = new JCheckBox("Do not show again");
		Object options1[] = { OK, check };
		Object options2[] = { DOWNLOAD, OK, check };	
		Object options[] = (downloadUrl == null) ? options1 : options2;		
		
		int b = JOptionPane.showOptionDialog(null,
					message,
					title,
					JOptionPane.OK_OPTION,
					JOptionPane.INFORMATION_MESSAGE,
					null,
					options,
					options[1]);

		if (options[b] == DOWNLOAD)
		    Platform.displayUrl(downloadUrl);
		
		Config.putBooleanProperty(configCheck, check.isSelected());
	}
	
	public static void showErrorDialog(String message, String title) {
	    if (Config.isHeadless()) {
	        System.out.println(message);
	        return;
	    }

	    JOptionPane.showMessageDialog(null,
	            message,
	            title,
	            JOptionPane.ERROR_MESSAGE);
	}
}
