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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.sound.sampled.AudioFormat;

import org.apache.log4j.Logger;


/**
 * Audio Buffer implementation.
 * 
 * @author Richard Titmuss
 */
public class AudioBuffer extends InputStream {
	private static final Logger logger = Logger.getLogger("audiobuffer.verbose");
	
	public static AudioFormat DEFAULT_AUDIO_FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED, 
            44100, /* sample rate */
            16, /* sample size in bits, */
            2, /* channels */
            4, /* frame size in bytes */
            44100, /* frame rate */
            false /* big endian */
          	);

	private long bufferSize = 262144;
	
	private long readPtr = 0;
	
	private long writePtr = 0;
	
	private boolean closed = false;
	
	private boolean loopBuffer = false;
	
	private long loopPtr = 0;
	
	private boolean flush = false;
	
	private boolean underrun = true;
	
	private HashSet listeners = new HashSet();
	
	private AudioFormat audioFormat;
	
	private SortedMap writeEvents = new TreeMap();
	
	private SortedMap readEvents = new TreeMap();
	
	private byte[] buf;
	
	private OutputStream copy;
	
	
	public AudioBuffer(int bufferSize) {
		this.bufferSize = bufferSize;
		buf = new byte[bufferSize];
		audioFormat = DEFAULT_AUDIO_FORMAT;
	}
	
	public synchronized AudioFormat getAudioFormat() {
	    return this.audioFormat;
	}
	
	public synchronized void setAudioFormat(AudioFormat audioFormat) {
	    if (this.audioFormat.matches(audioFormat))
	        return;
	    
	    this.audioFormat = audioFormat;
	    addReadEvent(0, new AudioEvent(this, AudioEvent.BUFFER_SET_AUDIO_FORMAT));
	}
	
	public synchronized void setOutputStream(OutputStream copy) {
	    this.copy = copy;
	}
	
	public synchronized void addListener(AudioBufferListener l) {
		listeners.add(l);
	}
	
	public synchronized void removeListener(AudioBufferListener l) {
		listeners.remove(l);
	}
	
	/**
	 * Add a write event in offset bytes. Once the readPtr has
	 * passed this mark a buffer event of the given type occurs.
	 *   
	 * @param event
	 * @param offset
	 */
	public synchronized void addWriteEvent(long offset, AudioEvent event) {
	    long readPos = writePtr + offset;
	    
	    List events = (List) writeEvents.get(new Long(readPos));
	    if (events == null) {
	        events = new ArrayList();
	        writeEvents.put(new Long(readPos), events);
	    }
	    events.add(event);
	}
	
	/**
	 * Add a read event in offset bytes. Once the readPtr has
	 * passed this mark a buffer event of the given type occurs.
	 *   
	 * @param event
	 * @param offset
	 */
	public synchronized void addReadEvent(long offset, AudioEvent event) {
	    long readPos = writePtr + offset;

	    List events = (List) readEvents.get(new Long(readPos));
	    if (events == null) {
	        events = new ArrayList();
	        readEvents.put(new Long(readPos), events);
	    }
	    events.add(event);
	}

	public synchronized void sendEvent( AudioEvent event) {
		for (Iterator i = listeners.iterator(); i.hasNext();)
			((AudioBufferListener) i.next()).bufferEvent(event);
	}

	
	/**
	 * Set the buffer to loop once the buffer is closed.
	 * @param loop
	 */
	public synchronized void setRepeat(boolean repeat) {
	    loopPtr = writePtr;
	    loopBuffer = repeat;
	}
	
	public synchronized boolean isRepeat() {
	    return loopBuffer;
	}
	
	/**
	 * @return Returns the read pointer position in buffer.
	 */
	public synchronized long getReadPtr() {
		return readPtr % bufferSize;
	}
	
	/**
	 * @return Returns the write pointer position in buffer.
	 */
	public synchronized long getWritePtr() {
		return writePtr % bufferSize;
	}

    /**
     * @return Returns the total number of bytes read from the buffer.
     */
    public long getReadCount() {
        return readPtr;
    }
    
    /**
     * @return Returns the total number of bytes written to the buffer.
     */
    public  long getWriteCount() {
        return writePtr;
    }
	
    /**
     * @return Returns the buffer size.
     */
	public synchronized int getBufferSize() {
		return (int)bufferSize;
	}

	public synchronized int available() throws IOException {
		int avail = (int)(writePtr - readPtr);
		
		if (logger.isDebugEnabled())
			logger.debug("avil R=" + readPtr + " W=" + writePtr + " A="	+ avail);    
		return avail;
	}
	
	public synchronized int freeSpace() {
		int free = (int)(bufferSize - writePtr + readPtr);

		logger.debug("free R=" + readPtr + " W=" + writePtr + " F=" + free);
		return free;
	}
	
	public synchronized void close() throws IOException {
	    if (closed)
	    		return;
	    
	    closed = true;
	    notifyAll();
		
		for (Iterator i = listeners.iterator(); i.hasNext();)
			((AudioBufferListener) i.next()).bufferEvent(new AudioEvent(this, AudioEvent.BUFFER_CLOSED));
		
		if (copy != null)
		    copy.close();
	}
	
	public int write(InputStream stream) throws IOException {
		int free, ptr, buflen;
		synchronized (this) {
			free = freeSpace();
			while (free <= 0) {
				/* If buffer is full, block waiting for reader */
				if (closed)
					return -1;

				logger.debug("buf W=" + writePtr + " R=" + readPtr
						+ " Buffer full, waiting ...");

				for (Iterator i = listeners.iterator(); i.hasNext();)
					((AudioBufferListener) i.next()).bufferEvent(
							new AudioEvent(this, AudioEvent.BUFFER_FULL));

				flush = false;

				try {
					wait();
				} catch (InterruptedException e) {
					throw new IOException("Interrupted wait during fill()");
				}

				if (flush)
					return 0;

				free = freeSpace();
			}

			if (closed)
				return -1;

		    ptr = (int)(writePtr % bufferSize);
		    buflen = Math.min(free, (int)(bufferSize-ptr) );
		}
		
		int n = stream.read(buf, ptr, buflen);

		if (copy != null && n > 0)
		    copy.write(buf, ptr, n);
		
		synchronized (this) {
			if (logger.isDebugEnabled())
				logger.debug("buf W=" + writePtr + " R=" + readPtr + " F=" + free+ " #=" + n);
		
			// Check for eos
			if (n == -1)
			    close();
			else
				writePtr += n;

			// Wake any readers
			notifyAll();

			// Notify listeners when an event mark is passed
			while (!writeEvents.isEmpty()) {
			    Long writePos = (Long)writeEvents.firstKey();
			    if (writePtr < writePos.longValue())
			        break;

    		    	    List events = (List) writeEvents.remove(writePos);
			    for (Iterator j = events.iterator(); j.hasNext(); ) {
			        AudioEvent event = ((AudioEvent)j.next());

			        for (Iterator i = listeners.iterator(); i.hasNext();) {
			            ((AudioBufferListener) i.next()).bufferEvent(event);
			        }
			    }
			}

			return n;
		}
	}
	
	public synchronized int write(byte b[], int off, int len) throws IOException {
		while (freeSpace() < (len-off)) {
	        if (closed)
	            return -1;
	        
	        if (logger.isDebugEnabled())
	        		logger.debug("buf R=" + readPtr + " W=" + writePtr
	        				+ " Buffer full, waiting ...");

			for (Iterator i = listeners.iterator(); i.hasNext();)
				((AudioBufferListener) i.next()).bufferEvent(new AudioEvent(this, AudioEvent.BUFFER_FULL));

	        flush = false;
	        
	        try {
	        		wait();
            } catch (InterruptedException e) {
				throw new IOException("Interrupted wait during write()");
            }
            
            if (flush)
                return 0;
	    }
	    
	    if (closed)
	        return -1;

	    int ptr = (int)(writePtr % bufferSize);	    	
	    int n = Math.min(len, (int)(bufferSize-ptr) );
	    System.arraycopy(b, off, buf, ptr, n);
	    
	    int r = len - off - n;
	    if (r > 0) {
	        System.arraycopy(b, off+n, buf, 0, r);
	        n += r;
	    }

		if (copy != null)
		    copy.write(b, off, n);

	    // Wake any readers
	    writePtr += n;
	    notifyAll();

		// Notify listeners when an event mark is passed
		while (!writeEvents.isEmpty()) {
		    Long writePos = (Long)writeEvents.firstKey();
		    if (writePtr < writePos.longValue())
		        break;
    
		    List events = (List) writeEvents.remove(writePos);
		    for (Iterator j = events.iterator(); j.hasNext(); ) {
		        AudioEvent event = ((AudioEvent)j.next());

		        for (Iterator i = listeners.iterator(); i.hasNext();) {
		            ((AudioBufferListener) i.next()).bufferEvent(event);
		        }
		    }
		}

	    return n;
	}

	public int read() throws IOException {

		byte b[] = new byte[1];
		int ok = read(b, 0, 1);
		return (ok < 0) ? -1 : b[0];
	}
	
	public int read(byte b[]) throws IOException {
		return read(b, 0, b.length);
	}

	public synchronized int read(byte b[], int off, int len) throws IOException {
	    int size = available();
		while (size == 0) {
		    if (loopBuffer && closed && readPtr == writePtr) {
		        readPtr = loopPtr;
 
				for (Iterator i = listeners.iterator(); i.hasNext();)
					((AudioBufferListener) i.next()).bufferEvent(new AudioEvent(this, AudioEvent.BUFFER_REPEAT));
				break;
		    }
		    
			if (!underrun && readPtr == writePtr) {
				logger.debug("audio buffer underrun");
				underrun = true;
				for (Iterator i = listeners.iterator(); i.hasNext();)
					((AudioBufferListener) i.next()).bufferEvent(new AudioEvent(this, AudioEvent.BUFFER_UNDERRUN));
			}

			if (closed)
				return -1;
			
			if (logger.isDebugEnabled())
				logger.debug("buf R=" + readPtr + " W=" + writePtr
						+ " Buffer empty, waiting ...");
			
			try {
		        wait();
			} catch (InterruptedException e) {
				throw new IOException("Interrupted wait during read()");
			}
			
			size = available();
		}
		underrun = false;

		len = Math.min(size, len);
		
		int ptr = (int)(readPtr % bufferSize);
		int n = Math.min(len, (int)(bufferSize-ptr) );
		if (n < 0)
		    return -1;
		
		System.arraycopy(buf, ptr, b, off, n);
		
		int r = len - off - n;
		if (r > 0 && size > n) {
			System.arraycopy(buf, 0, b, off+n, r);
			n += r;
		}
		    
		// Unblock a writer
	    readPtr += n;		
		notifyAll();

		if (logger.isDebugEnabled())
			logger.debug("buf R=" + readPtr + " W=" + writePtr + " #=" + n);

		// Notify listeners when an event mark is passed
		while (!readEvents.isEmpty()) {
		    Long readPos = (Long)readEvents.firstKey();
		    if (readPtr < readPos.longValue())
		        break;
    
		    List events = (List) readEvents.remove(readPos);
		    for (Iterator j = events.iterator(); j.hasNext(); ) {
		        AudioEvent event = ((AudioEvent)j.next());

		        for (Iterator i = listeners.iterator(); i.hasNext();) {
		            ((AudioBufferListener) i.next()).bufferEvent(event);
		        }
		    }
		}
		
		return n;
	}

	public synchronized long skip(long n) throws IOException {
		int num = Math.min(available(), (int) n);
		readPtr += num;
		return num;
	}
	
	public synchronized void flush() throws IOException {
		readPtr = writePtr = 0;
		flush = true;
		notifyAll();
	}
	
	/**
	 * Set the write ptr to ptr, flushing the remaining data from
	 * the stream.
	 * @param ptr
	 * @throws IOException
	 */
	public synchronized void flush(long ptr) throws IOException {
	    writePtr = ptr;
	    if (readPtr >= writePtr) {
	        readPtr = writePtr;
	        flush = true;
	    }
		notifyAll();
	}	
	
	public boolean markSupported() {
		return false;
	}
	
	public void mark(int readlimit) {
		logger.error("mark called "+readlimit);
	}
	
	public void reset() throws IOException {
		logger.error("reset called");
	}
}

