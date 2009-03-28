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

 
class SongInfo {
    private int id;
    private String title;
    private String genre;
    private int genreId;
    private String artist;
    private int artistId;
    private String composer;
    private String band;
    private String conductor;
    private String album;
    private int albumId;
    private double duration;
    private int disc;
    private int discCount;
    private int trackNum;
    private int year;
    private int bpm;
    private String comment;
    private String type;
    private String bitrate;
    private boolean hasCoverart = false;
    private boolean hasCoverartThumb = false;
    private String url;
    
    
    public SongInfo() {
    }

    /**
     * @return Returns the id.
     */
    public int getId() {
        return id;
    }

    /**
     * @param id The id to st.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return Returns the album.
     */
    public String getAlbum() {
        return album;
    }
    /**
     * @param album The album to set.
     */
    public void setAlbum(String album) {
        this.album = album;
    }
    /**
     * @return Returns the albumId.
     */
    public int getAlbumId() {
        return albumId;
    }
    /**
     * @param albumId The albumId to set.
     */
    public void setAlbumId(int albumId) {
        this.albumId = albumId;
    }
    /**
     * @return Returns the artist.
     */
    public String getArtist() {
        return artist;
    }
    /**
     * @param artist The artist to set.
     */
    public void setArtist(String artist) {
        this.artist = artist;
    }
    /**
     * @return Returns the artistId.
     */
    public int getArtistId() {
        return artistId;
    }
    /**
     * @param artistId The artistId to set.
     */
    public void setArtistId(int artistId) {
        this.artistId = artistId;
    }
    /**
     * @return Returns the band.
     */
    public String getBand() {
        return band;
    }
    /**
     * @param band The band to set.
     */
    public void setBand(String band) {
        this.band = band;
    }
    /**
     * @return Returns the bitrate.
     */
    public String getBitrate() {
        return bitrate;
    }
    /**
     * @param bitrate The bitrate to set.
     */
    public void setBitrate(String bitrate) {
        this.bitrate = bitrate;
    }
    /**
     * @return Returns the bpm.
     */
    public int getBpm() {
        return bpm;
    }
    /**
     * @param bpm The bpm to set.
     */
    public void setBpm(int bpm) {
        this.bpm = bpm;
    }
    /**
     * @return Returns the comment.
     */
    public String getComment() {
        return comment;
    }
    /**
     * @param comment The comment to set.
     */
    public void setComment(String comment) {
        this.comment = comment;
    }
    /**
     * @return Returns the composer.
     */
    public String getComposer() {
        return composer;
    }
    /**
     * @param composer The composer to set.
     */
    public void setComposer(String composer) {
        this.composer = composer;
    }
    /**
     * @return Returns the conductor.
     */
    public String getConductor() {
        return conductor;
    }
    /**
     * @param conductor The conductor to set.
     */
    public void setConductor(String conductor) {
        this.conductor = conductor;
    }
    /**
     * @return Returns the disc.
     */
    public int getDisc() {
        return disc;
    }
    /**
     * @param disc The disc to set.
     */
    public void setDisc(int disc) {
        this.disc = disc;
    }
    /**
     * @return Returns the discCount.
     */
    public int getDiscCount() {
        return discCount;
    }
    /**
     * @param discCount The discCount to set.
     */
    public void setDiscCount(int discCount) {
        this.discCount = discCount;
    }
    /**
     * @return Returns the duration.
     */
    public double getDuration() {
        return duration;
    }
    /**
     * @param duration The duration to set.
     */
    public void setDuration(double duration) {
        this.duration = duration;
    }
    /**
     * @return Returns the genre.
     */
    public String getGenre() {
        return genre;
    }
    /**
     * @param genre The genre to set.
     */
    public void setGenre(String genre) {
        this.genre = genre;
    }
    /**
     * @return Returns the genreId.
     */
    public int getGenreId() {
        return genreId;
    }
    /**
     * @param genreId The genreId to set.
     */
    public void setGenreId(int genreId) {
        this.genreId = genreId;
    }
    /**
     * @return Returns the hasCoverart.
     */
    public boolean isHasCoverart() {
        return hasCoverart;
    }
    /**
     * @param hasCoverart The hasCoverart to set.
     */
    public void setHasCoverart(boolean hasCoverart) {
        this.hasCoverart = hasCoverart;
    }
    /**
     * @return Returns the hasCoverartThumb.
     */
    public boolean isHasCoverartThumb() {
        return hasCoverartThumb;
    }
    /**
     * @param hasCoverartThumb The hasCoverartThumb to set.
     */
    public void setHasCoverartThumb(boolean hasCoverartThumb) {
        this.hasCoverartThumb = hasCoverartThumb;
    }
    /**
     * @return Returns the title.
     */
    public String getTitle() {
        return title;
    }
    /**
     * @param title The title to set.
     */
    public void setTitle(String title) {
        this.title = title;
    }
    /**
     * @return Returns the trackNum.
     */
    public int getTrackNum() {
        return trackNum;
    }
    /**
     * @param trackNum The trackNum to set.
     */
    public void setTrackNum(int trackNum) {
        this.trackNum = trackNum;
    }
    /**
     * @return Returns the type.
     */
    public String getType() {
        return type;
    }
    /**
     * @param type The type to set.
     */
    public void setType(String type) {
        this.type = type;
    }
    /**
     * @return Returns the url.
     */
    public String getUrl() {
        return url;
    }
    /**
     * @param url The url to set.
     */
    public void setUrl(String url) {
        this.url = url;
    }
    /**
     * @return Returns the year.
     */
    public int getYear() {
        return year;
    }
    /**
     * @param year The year to set.
     */
    public void setYear(int year) {
        this.year = year;
    }
    public String toString() {
        return title + " " + artist + " " + album;
    }
}