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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import org.titmuss.softsqueeze.config.Config;
import org.titmuss.softsqueeze.music.SongList;
import org.titmuss.softsqueeze.music.SongListTable;
import org.titmuss.softsqueeze.net.CliListener;
import org.titmuss.softsqueeze.net.CliMessage;
import org.w3c.dom.Element;


/**
 * @author Richard Titmuss
 */
public class Playlist extends SkinComponent implements AncestorListener, CliListener {
	
	protected Color fgcolor;

	protected Color bgcolor;

	protected Font font;

	private SongList songList;
	
	private long tstamp = 0;
	
	public Playlist(Skin skin, Element e) {
		super(skin, e);
		
		fgcolor = parseColorAttribute(e, "fgcolor", null);
		bgcolor = parseColorAttribute(e, "bgcolor", null);
		font = parseFontAttribute(e, "font", "fontsize", null);

		songList = new SongList();		
	}
		
	public JComponent createComponent()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBackground(bgcolor);
		panel.setForeground(fgcolor);
		panel.setFont(font);
		
		SongListTable table = new SongListTable(songList);
		table.addAncestorListener(this);

	    // FIXME dynamic columns from configuration, need to think how the
	    // table settings are stored in the configuration ...
        table.setAllColumnsVisible(false);
        table.setColumnVisible("Title", true);
        table.setColumnVisible("Album", true);
        table.setColumnVisible("Artist", true);
        table.setColumnVisible("Genre", true);
        table.setColumnVisible("Duration", true);
        table.setColumnVisible("down", true);
        table.setColumnVisible("up", true);
        table.setColumnVisible("delete", true);

		panel.add(table, BorderLayout.CENTER);		
		
		return panel;
	}
	
	
	public void ancestorAdded(AncestorEvent e) {
		logger.debug("Playlist ancestor added");
		cliConnected();
	}

	public void ancestorMoved(AncestorEvent e) {
	}

	public void ancestorRemoved(AncestorEvent e) {
		logger.debug("Playlist ancestor removed");
		squeeze.getCLI().removeFilter(new CliMessage(Config.getProperty("macaddress"), "newsong"), this);
		squeeze.getCLI().removeFilter(new CliMessage(Config.getProperty("macaddress"), "playlist"), this);
		squeeze.getCLI().removeFilter(new CliMessage(Config.getProperty("macaddress"), "playlistcontrol"), this);
	}
	
	private void refresh() {
	}

    /* (non-Javadoc)
     * @see com.slim.softsqueeze.net.CliListener#cliReply(com.slim.softsqueeze.net.CliReply)
     */
    public void cliMessage(CliMessage reply) {
        /*
         * Don't request the playlist more than once every half second, the cli commands
         * we listen to are often grouped.
         */
        long now = System.currentTimeMillis();
        if (tstamp + 500 >= now)
            return;
        tstamp = now;
        
		songList.loadPlaylist(squeeze.getCLI(), Config.getProperty("macaddress"));	    
    }

    /* (non-Javadoc)
     * @see com.slim.softsqueeze.net.CliListener#cliConnected()
     */
    public void cliConnected() {
		squeeze.getCLI().addFilter(new CliMessage(Config.getProperty("macaddress"), "newsong"), this);
		squeeze.getCLI().addFilter(new CliMessage(Config.getProperty("macaddress"), "playlist"), this);
		squeeze.getCLI().addFilter(new CliMessage(Config.getProperty("macaddress"), "playlistcontrol"), this);
		songList.loadPlaylist(squeeze.getCLI(), Config.getProperty("macaddress"));
    }

    /* (non-Javadoc)
     * @see com.slim.softsqueeze.net.CliListener#cliDisconnected()
     */
    public void cliDisconnected() {
    }
}
