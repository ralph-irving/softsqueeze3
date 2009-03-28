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

package org.titmuss.softsqueeze.platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * 
 * @author richard
 */
public class LinuxPlatform extends Platform {

	/**
	 * Returns the host mac address.
	 */
	protected String _getHostMacAddress() {
		try {
			Process p = null;
			try {
				p = Runtime.getRuntime().exec("ifconfig");
			} catch (IOException unknown) {
				p = Runtime.getRuntime().exec("/sbin/ifconfig");
			}

			BufferedReader is = new BufferedReader(new InputStreamReader(p
					.getInputStream()));
			StringBuffer sb = new StringBuffer();

			String line = is.readLine();
			while (line != null) {
				sb.append(line);
				sb.append(" ");
				line = is.readLine();
			}
			is.close();

			int status = p.waitFor();
			if (status != 0) {
				logger.warn("ifconfig returned s=" + status + " "
						+ sb.toString());
				return null;
			}

			Pattern macPattern = Pattern
					.compile(".*(\\w\\w:\\w\\w:\\w\\w:\\w\\w:\\w\\w:\\w\\w).*");
			Matcher macMatcher = macPattern.matcher(sb.toString());
			if (macMatcher.matches()) {
				return macMatcher.group(1);
			}

			logger.warn("Cannot find mac address " + sb.toString());
			return null;
		} catch (IOException e) {
			logger.error("Error in ifconfig", e);
			return null;
		} catch (InterruptedException e) {
			logger.error("Error in ifconfig", e);
			return null;
		}
	}
}