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

package org.titmuss.softsqueeze.debug;

import org.apache.log4j.Logger;

/**
 * @author Richard Titmuss
 *
 * Capture debug information from a stream, including a finger print
 * of the start of the stream and the stream length.
 */
public class StreamDebug {
    private final static Logger logger = Logger.getLogger("audiobuffer");
    
    private final static int MAX_BYTES = 100;
    
    private int cnt = 0; /* non-zero byte count */
    private int idx = 0; /* bytes of stream read */
    private int nzb = 0; /* ptr to last non-zero byte */
    
    private String name;
    private StringBuffer str = new StringBuffer();
    
    /**
     * @return true if stream debug is enabled. 
     */
    public static boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }
    
    public StreamDebug(String name) {
        this.name = name;
        
        str.append(name);
        str.append(": ");
    }
    
    public void write(byte buf[], int ptr, int len) {
        while (cnt < MAX_BYTES && ptr < len) {
            int b = buf[ptr++] & 0xFF;
            idx++;
            
            if (b != 0 && b != 0xFF) {
                if (nzb+1 != idx) {
                    str.append("[");
                    str.append(Integer.toHexString(idx));
                    str.append("]");
                }
                
                str.append(Integer.toHexString(b&0xFF));
                str.append(" ");
                
                cnt++;
                nzb = idx;
                
                if (cnt == MAX_BYTES) {
                    logger.debug(str.toString());
                }
            }
        }
        
        idx += len - ptr;
    }
    
    public void close() {
        if (cnt < MAX_BYTES)
            logger.debug(str.toString());
        
        logger.debug(name+": stream length = "+idx);
    }
}
