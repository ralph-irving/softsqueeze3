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

package org.titmuss.softsqueeze.music;

import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;
import org.titmuss.softsqueeze.config.Config;
import org.titmuss.softsqueeze.net.CliConnection;
import org.titmuss.softsqueeze.net.CliListener;
import org.titmuss.softsqueeze.net.CliMessage;
import org.titmuss.softsqueeze.net.CliParameterIterator;


 
public class SongList extends AbstractTableModel implements CliListener {
    private final static Logger logger = Logger.getLogger("cli");
    
    public final static SongListButton PLAY_BUTTON = new SongListButton();
    
    public final static SongListButton ADD_BUTTON = new SongListButton();
    
    public final static SongListButton DOWN_BUTTON = new SongListButton();
    
    public final static SongListButton UP_BUTTON = new SongListButton();
    
    public final static SongListButton DELETE_BUTTON = new SongListButton();

    
    private int count = 0;
    
    private int playingSong = -1;
    
    private SongInfo[] songs = new SongInfo[0];
    
    private String playerid;
    
    private String command;

    private String search;

    private String columnName[] = new String[] {
            "Title",
            "Genre",
            "Artist",
            "Composer",
            "Band",
            "Album",
            "Duration",
            "Disc",
            "Track Num",
            "Year",
            "BPM",
            "Comment",
            "Format",
            "Bitrate",
            "play", // play
            "add", // add
            "down", // down
            "up", // up
            "delete", // delete
    };
    
    
    private final static int CHUNK_SIZE = 10;
    
    private boolean loadedChunk[] = new boolean[1]; 
    
    private CliConnection cli;
    
    
    public SongList() {
    }
    
    public void loadPlaylist(CliConnection cli, String aPlayerid) {
        this.cli = cli;
        command = "status";
        playerid = aPlayerid;
        
        invalidateSongList();
    }
    
    public void searchSongs(CliConnection cli, String searchParameters) {
        this.cli = cli;
        command = "titles";
        playerid = null;
        search = searchParameters;
        
        invalidateSongList();
    }
    
    public void clear() {
        if (count > 0) {
            count = 0;
            fireTableDataChanged();
        }        
    }

    
    private void parseSongList(CliMessage msg) {
        String tag = null;
        String val = null;;

        if (! (msg.getCommand().equals("status") || msg.getCommand().equals("titles")) )
            throw new IllegalArgumentException("Cannot build playlist with cli command " + msg.getCommand());

        CliParameterIterator iterator = msg.getParameterIterator();
        
        boolean reset = false;
        int start = Integer.parseInt(iterator.nextParameter());
        int items = Integer.parseInt(iterator.nextParameter());

        while (iterator.hasNext()) {
            tag = iterator.nextTag();
            val = iterator.nextValue();

            if (tag.equals("id"))
                break;
            
            // status command
            else if (tag.equals("playlist_cur_index"))
                playingSong = Integer.parseInt(val);
            else if (tag.equals("playlist_tracks"))
                count = Integer.parseInt(val);

            // tracks command
            else if (tag.equals("count"))
                count = Integer.parseInt(val);
        }

        if (songs.length != count) {
            reset = true;
            
            SongInfo[] tmp = new SongInfo[count];
            System.arraycopy(songs, 0, tmp, 0, Math.min(songs.length, count));
            songs = tmp;

            loadedChunk = new boolean[(count / CHUNK_SIZE) + 1];
        }
        
        int index = start;
        while (iterator.hasNext() && index < songs.length) {            
            SongInfo song = new SongInfo();
            songs[index++] = song;
            song.setId(Integer.parseInt(val));
            
            while (iterator.hasNext()) {
                tag = iterator.nextTag();
                val = iterator.nextValue();

                if (tag.equals("playlist index")) {
                }	// noop
                else if (tag.equals("id"))
                    break;
                else if (tag.equals("title"))
                    song.setTitle(val);
                else if (tag.equals("genre"))
                    song.setGenre(val);
                else if (tag.equals("genre_id"))
                    song.setGenreId(Integer.parseInt(val));
                else if (tag.equals("artist"))
                    song.setArtist(val);
                else if (tag.equals("artist_id"))
                    song.setArtistId(Integer.parseInt(val));
                else if (tag.equals("composer"))
                    song.setComposer(val);
                else if (tag.equals("band"))
                    song.setBand(val);
                else if (tag.equals("album"))
                    song.setAlbum(val);
                else if (tag.equals("album_id"))
                    song.setAlbumId(Integer.parseInt(val));
                else if (tag.equals("duration"))
                    song.setDuration(Double.parseDouble(val));
                else if (tag.equals("disc"))
                    song.setDisc(Integer.parseInt(val));
                else if (tag.equals("disccount"))
                    song.setDiscCount(Integer.parseInt(val));
                else if (tag.equals("tracknum"))
                    song.setTrackNum(Integer.parseInt(val));
                else if (tag.equals("year"))
                    song.setYear(Integer.parseInt(val));
                else if (tag.equals("bpm"))
                    song.setBpm(Integer.parseInt(val));
                else if (tag.equals("comment"))
                    song.setComment(val);
                else if (tag.equals("type"))
                    song.setType(val);
                else if (tag.equals("bitrate"))
                    song.setBitrate(val);
                else if (tag.equals("coverart"))
                    song.setHasCoverart(val.equals("1"));
                else if (tag.equals("coverartThumb"))
                    song.setHasCoverartThumb(val.equals("1"));
                else if (tag.equals("url"))
                    song.setUrl(val);
                else
                    logger.warn("UNKOWN TAG " + tag);
            }
        }
        
        fireTableDataChanged();
    }

    public int count() {
        return count;
    }
    
    public boolean isPlaying(int index) {
        return index == playingSong;
    }
    
    public SongInfo getSong(int index) {
        return songs[index];
    }
    
    
    
    private void invalidateSongList() {
        // Invalidate existing songs
        for (int i=0; i<loadedChunk.length; i++)
            loadedChunk[i] = false;
        
        lazyLoad(0);
    }
    
    private void lazyLoad(int index) {
        int chunk = (int)(index/CHUNK_SIZE);
        if (loadedChunk[chunk])
            return;        
        loadedChunk[chunk] = true;
        
        cli.queueMessage(this, getCommand(chunk));
    }

    private CliMessage getCommand(int chunk) {
        CliMessage msg = new CliMessage(playerid, command);
        msg.addParameter(Integer.toString(chunk * CHUNK_SIZE));
        msg.addParameter(Integer.toString(CHUNK_SIZE));
        if (search != null)
            msg.addParameter(search);
        msg.addParameter("tag", "");
        
        return msg;
    }
    
    /* (non-Javadoc)
     * @see com.slim.softsqueeze.net.CliListener#cliReply(com.slim.softsqueeze.net.CliReply)
     */
    public void cliMessage(CliMessage reply) {
        String cmd = reply.getCommand();
        
        if (cmd.equals("status") || cmd.equals("titles"))
            parseSongList(reply);
    }

    /* (non-Javadoc)
     * @see com.slim.softsqueeze.net.CliListener#cliConnected()
     */
    public void cliConnected() {
    }

    /* (non-Javadoc)
     * @see com.slim.softsqueeze.net.CliListener#cliDisconnected()
     */
    public void cliDisconnected() {
    }
    
    
    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnName(int)
     */
    public String getColumnName(int column) {
        return columnName[column];
    }
    
    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getRowCount()
     */
    public int getRowCount() {
        return count();
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    public int getColumnCount() {
        return columnName.length;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnClass(int)
     */
    public Class getColumnClass(int columnIndex) {
        switch (columnIndex) {
        case 14:
        case 15:
        case 16:
        case 17:
        case 18:
            return SongListButton.class;
            
        default:
        	return String.class;
        }
    }
    
    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex >= count)
            return null;
        
        SongInfo song = songs[rowIndex];
        
        int chunk = rowIndex / CHUNK_SIZE;        
        if (!loadedChunk[chunk])
            lazyLoad(rowIndex);
        
        if (song == null || !loadedChunk[chunk]) {
            if (columnIndex == 0)
                return "Loading ...";
            return null;
        }
        
        switch (columnIndex) {
        case 0:
            if (playingSong == rowIndex)
                return "** "+song.getTitle();
            else
                return song.getTitle();

        case 1:
            return song.getGenre();

        case 2:
            return song.getArtist();

        case 3:
            return song.getComposer();

        case 4:
            return song.getBand();

        case 5:
            return song.getAlbum();
            
        case 6:
            return Integer.toString((int)song.getDuration());

        case 7:
            return Integer.toString(song.getDiscCount())+"/"+Integer.toString(song.getDisc());

        case 8:
            return Integer.toString(song.getTrackNum());

        case 9:
            return Integer.toString(song.getYear());

        case 10:
            return Integer.toString(song.getBpm());

        case 11:
            return song.getComment();

        case 12:
            return song.getType();

        case 13:
            return song.getBitrate();

        case 14:
            return PLAY_BUTTON;
            
        case 15:
            return ADD_BUTTON;
            
        case 16:
            return DOWN_BUTTON;
            
        case 17:
            return UP_BUTTON;
            
        case 18:
            return DELETE_BUTTON;
            
        default:
            return null;
        }
    }

    public void actionPerformed(int rowIndex, int columnIndex) {
        if (command.equals("titles"))
            columnIndex += (14-5);
        
        switch (columnIndex) {
        case 14:
            cli.queueMessage(this, new CliMessage(Config.getProperty("macaddress"), "playlistcontrol")
                    .addParameter("cmd", "load")
                    .addParameter("track_id", Integer.toString(songs[rowIndex].getId()))
                    );
            break;

        case 15:
            cli.queueMessage(this, new CliMessage(Config.getProperty("macaddress"), "playlistcontrol")
                    .addParameter("cmd", "add")
                    .addParameter("track_id", Integer.toString(songs[rowIndex].getId()))
                    );
            break;

        case 16:
        case 5: // FIXME WRONG!!
            cli.queueMessage(this, new CliMessage(playerid, "playlist")
                    .addParameter("move")
                    .addParameter(Integer.toString(rowIndex))
                    .addParameter(Integer.toString(rowIndex+1))
                    );
            invalidateSongList();
            break;

        case 17:
        case 6: // FIXME WRONG!!
            cli.queueMessage(this, new CliMessage(playerid, "playlist")
                    .addParameter("move")
                    .addParameter(Integer.toString(rowIndex))
                    .addParameter(Integer.toString(rowIndex-1))
                    );
            invalidateSongList();
            break;

        case 18:
        case 7: // FIXME WRONG!!
            cli.queueMessage(this, new CliMessage(playerid, "playlistcontrol")
                    .addParameter("cmd", "delete")
                    .addParameter("track_id", Integer.toString(songs[rowIndex].getId()))
                    );
            invalidateSongList();
            break;            
        }
    }
    
    public static class SongListButton {
    }
}
