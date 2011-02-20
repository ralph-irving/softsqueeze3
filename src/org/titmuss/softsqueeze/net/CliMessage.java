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

import java.util.ArrayList;
import java.util.Iterator;


public class CliMessage {
    private static final String globalCommands[] = new String[] {
            "login",
            "debug",
            "listen",
            "pref",
            "exit",
            "player",
            "rescan",
            "wipecache",
            "info",
            "genres",
            "artists",
            "albums",
            "songinfo",
            "titles",
            "songs",
            "playlists"
    };

    private char msg[];
    private int idx;
    
    private String playerid;
    private String command;
    private ArrayList parameters = new ArrayList();
    
    /**
     * Create a new global cli message.
     * @param command
     */
    public CliMessage(String command) {
        this(null, command);
    }

    /**
     * Create a player specific cli message.
     * 
     * @param playerid
     * @param command
     */
    public CliMessage(String playerid, String command) {
        if (command == null)
            throw new IllegalArgumentException("Command cannot be null");

        this.playerid = playerid;
        this.command = command;
    }

    private CliMessage(char msg[]) {
        this.msg = msg;
        
        playerid = parseNextParameter();
        for (int i=0; i<globalCommands.length; i++) {
            if (playerid.equals(globalCommands[i])) {
                command = playerid;
                playerid = null;
                break;
            }
        }
        
        if (command == null)
            command = parseNextParameter();
        
        while (idx < msg.length) {
            parameters.add(parseNextParameter());
        }
    }
    
    
    public static CliMessage parseMessage(String msg) {
        return new CliMessage(msg.toCharArray());
    }
    
    /**
     * Add a parameter to the cli message.
     * @param parameter
     * @return
     */
    public CliMessage addParameter(String parameter) {
        parameters.add(parameter);
        return this;
    }
    
    /**
     * Add a tag value pair to the cli message.
     * @param tag
     * @param value
     * @return
     */
    public CliMessage addParameter(String tag, String value) {
        parameters.add(tag + ":" + value);
        return this;
    }
    
    /**
     * Return the command for the cli message.
     * @return
     */
    public String getCommand() {
        return command;
    }
    
    /**
     * Return the playerid, if any, for this cli message.
     * @return
     */
    public String getPlayerid() {
        return playerid;
    }
    
    /**
     * Return an iterator over the parameters in this cli message.
     * @return
     */
    public CliParameterIterator getParameterIterator() {
        return new CliParameterIterator(parameters);        
    }
    
    /**
     * Return the cli message as a string (ready to send to the Squeezebox Server).
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
    
        if (playerid != null) {
            urlEncode(buf, playerid);
            buf.append(" ");
        }
        
        buf.append(command);

        for (Iterator i=parameters.iterator(); i.hasNext(); ) {
            String str = (String) i.next();

            buf.append(" ");
            urlEncode(buf, str);
        }
        
        return buf.toString();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (! (obj instanceof CliMessage) )
            return false;
        
        CliMessage msg = (CliMessage) obj;

        if (playerid == null)
            return msg.playerid == null && command.equals(msg.command);
        else
            return playerid.equals(msg.playerid) && command.equals(msg.command);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return command.hashCode();
    }
    
    private static void urlEncode(StringBuffer buf, String s) {
        char c[] = s.toCharArray();
        
        for (int i=0; i<c.length; i++) {
            if (c[i] <= 31 || c[i] >= 0x7F) {
                buf.append('%');
                buf.append(Integer.toString((int)c[i], 16).toUpperCase());
            }
            else {
                switch (c[i]) {
                	// Reserverd characters
                	case '$':
                	case '&':
                	case '+':
                	case ',':
                	case '/':
                	case ':':
                	case ';':
                	case '=':
                	case '?':
                	case '@':
                	
                	// Unsafe characters
                	case ' ':
                	case '"':
                	case '<':
                	case '>':
                	case '#':
                	case '%':
                	case '{':
                	case '}':
                	case '|':
                	case '\\':
                	case '^':
                	case '~':
                	case '[':
                	case ']':
                	case '`':
                	    buf.append('%');
                        buf.append(Integer.toString((int)c[i], 16).toUpperCase());                	    
                	    break;
                	    
                	default:
                	    buf.append(c[i]);
                }
            }
        }        
    }
    
    private String parseNextParameter() {
        StringBuffer tok = new StringBuffer();
        while (idx < msg.length) {
            char c = msg[idx++];
            switch (c) {
            case ' ':
                return tok.toString();
                
            case '%':
                c = (char) ((convertHexDigit(msg[idx++]) << 4) | convertHexDigit(msg[idx++]));
                tok.append(c);
                break;

            default:
                tok.append(c);
            }
        }
        
        return tok.toString();
    }
    
    private static byte convertHexDigit(char b) {
        if ((b >= '0') && (b <= '9')) {
            return (byte) (b - '0');
        }
        if ((b >= 'a') && (b <= 'f')) {
            return (byte) (b - 'a' + 10);
        }
        if ((b >= 'A') && (b <= 'F')) {
            return (byte) (b - 'A' + 10);
        }
        return 0;
    }
}
