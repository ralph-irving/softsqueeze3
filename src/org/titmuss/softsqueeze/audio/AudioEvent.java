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
 * @author Richard Titmuss
 *
 * Event in AudioBuffer
 */
public class AudioEvent extends java.util.EventObject {
    public final static int BUFFER_CLOSED = 0;

    public final static int BUFFER_UNDERRUN = 1;

    public final static int BUFFER_REPEAT = 2;

    public final static int BUFFER_PLAYING = 3;

    public final static int BUFFER_THRESHOLD = 4;

    public final static int BUFFER_FULL = 5;

    public final static int BUFFER_SET_AUDIO_FORMAT = 6;

    public final static int BUFFER_METADATA = 7;

    public final static int BUFFER_SET_REPLAYGAIN = 8;

    public final static int BUFFER_DECODER_STOPPED = 9;

    private int id;
    
    private String metadata;
    
    private float replayGain;

    
    AudioEvent(AudioBuffer source, int id) {
        this(source, id, null);
    }
    
    AudioEvent(AudioBuffer source, int id, String metadata) {
        super(source);
        this.id = id;
        this.metadata = metadata;
    }
    
    AudioEvent(AudioBuffer source, int id, float replayGain) {
        super(source);
        this.id = id;
        this.replayGain = replayGain;
    }
    
    /**
     * @return Returns the event id.
     */
    public int getId() {
        return id;
    }
    
    /**
     * @return Returns the stream metadata.
     */
    public String getMetadata() {
        return metadata;
    }
    
    /**
     * @return Returns the replayGain.
     */
    public float getReplayGain() {
        return replayGain;
    }
}
