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
import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import org.apache.log4j.Logger;
import org.titmuss.softsqueeze.config.Config;
import org.titmuss.softsqueeze.visualizer.Visualizer;


/**
 * Audio output stage.
 * 
 * @author Richard Titmuss
 */
public class AudioMixer implements Runnable, AudioBufferListener {
	private static final Logger vlogger = Logger.getLogger("javasound.verbose");

	private static final Logger logger = Logger.getLogger("javasound");

	private static int audioLineCount = 0;

	private Mixer mixer;
	
	private SourceDataLine line;
	
	private FloatControl gainControl;

	private AudioBuffer audioBuffer;
	
    private AudioFormat audioFormat;			    

	private double leftLevel = 1.0;

	private double rightLevel = 1.0;
	
	private double replayGain = 1.0;

	private byte[] buf;
	
	private int lineSize;

	private int bufSize = 0;

	private int bufLen = 0;
	
	private int bufCount = 0;
	
    private int frameSize;
    
    private float frameRate;
    
    private long framePositionOffset = 0;
    
	private Visualizer visualizer;
	
	private final static int PAUSE = 0;

	private final static int FLUSH = 1;

	private final static int PLAY = 2;

	private final static int STOP = 3;
	
	private final static int RESET = 4;
	
	private volatile int inState = PAUSE;

	private volatile int toState = PAUSE;
	
	private float dB = 0;
	
	private Thread mixerThread;
	
	private long lastFramePos = 0;

    private long writeTime = 0;
    
    private long readTime = 0;
    
    private long bufDuration = 0;

    private int slowStart = 0;

	private int skipFrames = 0;
    
	
	/**
	 * Create an audio mixer.
	 * 
	 * @param audioBuffer audio buffer.
	 * @throws IOException
	 * 
	 * @throws AudioException
	 * @throws AudioException
	 * @throws LineUnavailableException
	 */
	public AudioMixer(AudioBuffer audioBuffer) throws AudioException, LineUnavailableException {
		this.audioBuffer = audioBuffer;
		audioBuffer.addListener(this);

		initLine();
		
		/* start mixer thread */
		mixerThread = new Thread(this, "AudioMixer-"+audioLineCount++);
		mixerThread.setDaemon(true);
		mixerThread.setPriority(Thread.MAX_PRIORITY-2);
		mixerThread.start();
	}
	
	private void initLine() throws AudioException, LineUnavailableException {
		/* create java sound mixer */
		String mixerName = Config.getProperty("audio.mixer");
		Mixer.Info mixerInfo = getMixerInfo(mixerName);
		
		if (mixerInfo == null) {
			logger.error("JavaSoundPlayer: mixer not found: " + mixerName);
			throw new AudioException("Cannot find mixer " + mixerName);
		}		
		
		logger.debug("MIXER: "+mixerInfo.getName());
		mixer = AudioSystem.getMixer(mixerInfo);

		/* create java sound line */
		int lineBufferSize = Config.getIntegerProperty("audio.lineBufferSize");
		
		audioFormat = audioBuffer.getAudioFormat();
		DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat,
		        lineBufferSize);
		
		line = (SourceDataLine) mixer.getLine(lineInfo);
		line.open(audioFormat, lineBufferSize);
		framePositionOffset = 0;

		if (logger.isDebugEnabled()) {
			javax.sound.sampled.Control[] ctls = line.getControls();
			for (int i = 0; i < ctls.length; i++)
				logger.debug("Control: " + ctls[i]);
		}
		
		gainControl = (FloatControl) (line.getControl(FloatControl.Type.MASTER_GAIN));
		gainControl.setValue(dB);
		
		lineSize = line.getBufferSize();
        frameSize = audioFormat.getFrameSize();
        frameRate = audioFormat.getFrameRate();
        
        buf = new byte[lineSize];
		bufSize = buf.length;
		logger.debug("LINE BUFFER SIZE: " + lineSize);
		
		logger.debug("**** MIXER AUDIO FORMAT "+audioFormat);
	}
	
	
	public void setVolume(double leftLevel, double rightLevel) {
	    this.leftLevel = leftLevel;
	    this.rightLevel = rightLevel;
	    setVolume();
	}
	
	
	private void setVolume() {	    
	    double gain = (leftLevel + rightLevel) / 2;
	    gain *= replayGain;
	    
		dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
		gainControl.setValue(dB);
		
	    logger.debug("gain=" + gain + " replayGain=" + replayGain + " dB=" + dB + " max=" + gainControl.getMaximum());
	}
	
	public void setVisualizer(Visualizer newVisualizer) {
	    if (visualizer != null)
	        visualizer.stop();

	    if (newVisualizer != null) {
	        newVisualizer.setAudioFormat(audioFormat);
	        newVisualizer.init();
	        if (inState == PLAY)
	        		newVisualizer.play();
	    }
	    
	    this.visualizer = newVisualizer;	    
	}
	
	public long getElapsedMilliseconds() {
	    long framePos = line.getFramePosition() - framePositionOffset; 
	    if (framePos < lastFramePos) {
	        lastFramePos = framePos;
	        framePos += (2^31);
	    }
	    else {
	        lastFramePos = framePos;
	    }
	        
	    return (long) ((double)framePos / frameRate * 1000);
	}
	
	/**
	 * Stop the audio line.
	 */
	public synchronized void pause(int interval) {
		if (!line.isRunning()) 
		    return;
		
		logger.debug("pause line inState=" + inState + ", interval=" + interval);
		line.stop();

		if (interval != 0) {
			try {
				Thread.sleep(interval+2000);
			} catch (InterruptedException e) {
				logger.debug("pause sleep interrupted");
			} finally{
				line.start();
			}
		} else
			toState = PAUSE;

		notifyAll();
	}

	/**
	 * Start the audio line. Data written to the line will
	 * now be played.
	 */
	public synchronized void play(long atTime) {
	    if (line.isRunning())
	        return;
	    
	    logger.debug("play line inState=" + inState);

		toState = PLAY;

		if (atTime != 0) {
			long interval = atTime - System.currentTimeMillis();
			// Only sleep if we are not already too late and within 2s
			// Don't bother trying to deal with the jiffies-wrap-around case
			while (toState == PLAY && interval > 0 && interval < 2000) {
				try {
					notifyAll();	// let the mixer thread start filling the line buffer
					wait(interval);
				} catch (InterruptedException e) {
				}
				interval = atTime - System.currentTimeMillis();
			}
		}

		line.start();			
		
		notifyAll();
	}
	
	/**
	 * Stops playback and close the audio line.
	 * @throws AudioException
	 */
	public synchronized void stop() {
		logger.debug("stop line inState=" + inState);			
		line.stop();
		
		toState = STOP;
		notifyAll();
		
		if (visualizer != null)
		    visualizer.stop();
	}

	public synchronized void reset() {
		logger.debug("reset line inState=" + inState);			

		toState = RESET;
		notifyAll();
	}
	
	/**
	 * Stop and flush audio from the line and buffers.
	 * @throws IOException
	 */
	public synchronized void flush() throws IOException {
	    if (inState != PLAY && inState != PAUSE)
	        return;
	    
	    logger.debug("flush line inState=" + inState);
	    line.stop();
	    
	    toState = FLUSH;
	    notifyAll();
		mixerThread.interrupt(); // Stop audio buffer blocking 
	    
	    while (inState != PAUSE) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
	    }
	}

	public synchronized void drain() {
	    if (inState != PLAY)
	        return;
	    
	    logger.debug("drain line inState=" + inState);
	    
	    line.drain();
		line.stop();
		skipFrames = 0;

		toState = PAUSE;
		notifyAll();		
		mixerThread.interrupt(); // Stop audio buffer blocking 
	}

	public synchronized void skipAhead(int msInterval) {
		logger.debug("skipAhead " + msInterval + " frames left to skip=" + skipFrames);
		if (skipFrames > 0 || inState != PLAY)
			return;			// not playing or not finished previous skip yet

		skipFrames = (int)(frameRate * msInterval / 1000);
	}
	
    /**
     * Read a sample from the audio stream and write it to the Line.
     */
    public void run() {
        try {
    		logger.debug("audio mixer started");
    
    		int n = 0;
    		while (toState != STOP) {
    		    if (toState == FLUSH) {
    		        flushSamples();
    		    }
    			if (toState == PAUSE || toState == FLUSH) {
    				logger.debug("audio mixer paused (stopping player) available="+audioBuffer.available());
    				/* now paused, stop the audio line */
    				line.stop();
    				if (visualizer != null)
    				    visualizer.pause();
    				
    				synchronized (this) {
    				    inState = PAUSE;
    				    notifyAll();
    				
    				    /* spin until player is unpaused */
    					logger.debug("audio mixer paused (waiting)");
    					while (toState != PLAY) {
    					    try {
    					        wait();
    					    }
    					    catch (InterruptedException e) {
    					    }
    					    
    					    /* flush can be called during a pause */
    					    if (toState == FLUSH)
    					        flushSamples();
    					}
    					
    					inState = PLAY;
    					notifyAll();
    				}
    				
    				logger.debug("audio mixer playing available="+audioBuffer.available());
    				/* about to play, audio line started by play() */
    				bufDuration = 0;
    				slowStart = 4096;
    				// line.start();
    				if (visualizer != null)
    				    visualizer.play();
    			}    			
    			if (toState == RESET) {
    				synchronized (this) {
    				    toState = inState;
    				    logger.debug("audio mixer reset state = " + inState);

    				    line.stop();
    				    line.close();
						skipFrames = 0;
    				    initLine();
    				    
    				    if (inState == PLAY)
    				        line.start();
    				    
    				    if (visualizer != null)
    				        visualizer.setAudioFormat(audioFormat);
        				continue;
    				}
    			}

    			n = playSamples();

    			if (n == 0) {
    			    try {
        			    Thread.sleep(50);
    			    }
    			    catch (InterruptedException e) {
    			    }
    			}
    		}
    		logger.debug("audio mixer stopped");
    
    		/* close the audio line */
    		line.flush();
    		line.close();
    		
			synchronized (this) {
			    inState = STOP;
			    notifyAll();
			}
    	} catch (Exception e) {
    		logger.warn("audio mixer exception ", e);
    	}
    }
	
    private void flushSamples() throws IOException {
        logger.debug("flushing line");
        
        line.stop();
        line.flush();
        bufLen = 0;
		skipFrames = 0;
        audioBuffer.flush();
        framePositionOffset = line.getFramePosition();
        
        if (visualizer != null)
            visualizer.flush();
    }
    
     
    /**
     * Read data from the audioBuffer and write it to the line.
     * 
     * @return number of bytes written to line.
     * @throws AudioException
     */
    private int playSamples() throws AudioException {
        /*
         * Try to keep the javasound audio buffer at least 75% full,
         * by writing (bufferSize/4 + available) bytes to the line
         */
        
    	int lineAvail = line.available();
        int fillLen;

        boolean fillBuf = slowStart < lineSize;

		do {
			fillLen = buf.length - bufLen;
			if (!line.isRunning() && (fillLen + bufLen) > lineAvail)
				fillLen = lineAvail - bufLen;
			else if (fillBuf)
				fillLen = Math.min(slowStart, fillLen);

			int br = 0;
			if (fillLen > 0) {
				try {
					br = audioBuffer.read(buf, bufLen, fillLen);
					if (br < 0)
						return br; /* eof */
					
					bufLen += br;

					if (br < fillLen)
						logger.debug("playFrame: short read br=" + br + " fillLen=" + fillLen);
				}
				catch (IOException e) {
					br = 0; // read interrupted in drain
				}
			}
			if (vlogger.isDebugEnabled())
				vlogger.debug("playFrame: bytes read=" + br
						+ " bufLen=" + bufLen + " fillLen=" + fillLen
						+ " available=" + ((int)((lineAvail/(float)lineSize)*100.0)) + "%");

			if (skipFrames > 0) {
				int skipBytes = skipFrames * frameSize;
				if (skipBytes > bufLen)
					skipBytes = bufLen - (bufLen % frameSize);
				if (skipBytes > 0) {
					if (skipBytes < bufLen) {
						System.arraycopy(buf, skipBytes, buf, 0, bufLen - skipBytes);
						bufLen -= skipBytes;
					} else
						bufLen = 0;
					int skippedFrames = skipBytes / frameSize;
					skipFrames -= skippedFrames;
					framePositionOffset -= skippedFrames;
				}
			}

		} while (skipFrames > 0);
        
        readTime = System.currentTimeMillis();
        long readElapsed = readTime - writeTime;
        
        int bw = 0;
        if (inState == PLAY || inState == PAUSE) {		        
            /* Write the buffer to the line, this may block if the line if full */
            bw = line.write(buf, 0, bufLen);
            if (bw < 0)
                return bw; /* line closed */
            
            writeTime = System.currentTimeMillis();
            
            if (vlogger.isDebugEnabled())
                vlogger.debug("playFrame: bytes written=" + bw
                        + ",open:" + line.isOpen());
            
            if (visualizer != null)
                visualizer.write(buf, 0, bw);
        }
        else {
            bw = 0;
        }
        
        // Keep any bytes left over in the buffer
        if (bw < bufLen) {
            System.arraycopy(buf, bw, buf, 0, bufLen - bw);
            if (vlogger.isDebugEnabled())
                vlogger.warn("playFrame did not write (" + bw
                        + ") the same number of bytes as read (" + bufLen
                        + ")");
        }
        bufLen -= bw;
        bufCount += bw;
        
        if (fillBuf) {
        	logger.debug("playFrame: fill buffer. available=" + line.available() + " lineSize=" + lineSize + " fillLen=" + fillLen + " bw=" + bw + " slowStart=" + slowStart);
        	slowStart = Math.min(slowStart * 2, lineSize);
        }	
        
        if (bufDuration > 0) {
            long writeElapsed = writeTime - readTime; 
            
            if (vlogger.isDebugEnabled())
                vlogger.debug("readTook=" + readElapsed + "ms writeTook=" + writeElapsed + "ms bufferedData=" + bufDuration + "ms");
            
            if (writeElapsed > bufDuration + 150) // allow 150ms for os buffer + timing errors
                logger.debug("Detected delay writing to audio buffer readTook=" + readElapsed + "ms writeTook=" + writeElapsed + "ms bufferedData=" + bufDuration + "ms");
        }                
        bufDuration = (long) (((line.getBufferSize()-line.available()) / frameSize / frameRate) * 1000);    			    
        
        return bw;
    }

	private Mixer.Info getMixerInfo(String mixerName) {
		if (mixerName == null)
			return AudioSystem.getMixer(null).getMixerInfo();

		Mixer.Info[] aInfos = AudioSystem.getMixerInfo();
		for (int i = 0; i < aInfos.length; i++) {
			if (aInfos[i].getName().equals(mixerName)) {
				return aInfos[i];
			}
		}
		return null;
	}


	/**
	 * @return list of java sound audio mixers.
	 */
	public static String[] getJavaSoundMixers() {
		Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();

		ArrayList mixers = new ArrayList();
		mixer: for (int i = 0; i < mixerInfos.length; i++) {
			Mixer mixer = AudioSystem.getMixer(mixerInfos[i]);
			Line.Info infos[] = mixer.getSourceLineInfo();
			logger.debug("found mixer " + mixerInfos[i].getName()
					+ " supports " + infos.length + " lines");

			for (int j = 0; j < infos.length; j++) {
				if (!(infos[j] instanceof DataLine.Info))
					continue;

				mixers.add(mixerInfos[i].getName());
				continue mixer;
			}
		}
		return (String[]) mixers.toArray(new String[mixers.size()]);
	}

	
	/**
     * @return the default audio mixer.
     */
	public static String getDefaultJavaSoundMixer() {
	    Mixer.Info info = AudioSystem.getMixer(null).getMixerInfo();
	    return info.getName();
	}

    /* (non-Javadoc)
     * @see com.slim.softsqueeze.audio.AudioBufferListener#bufferEvent(com.slim.softsqueeze.audio.AudioBuffer, int)
     */
    public synchronized void bufferEvent(AudioEvent event) {
        switch (event.getId()) {
        	case AudioEvent.BUFFER_SET_AUDIO_FORMAT:
        	    reset();
        		break;

        	case AudioEvent.BUFFER_PLAYING:
        	    framePositionOffset = line.getFramePosition();
        		break;

        	case AudioEvent.BUFFER_SET_REPLAYGAIN:
        	    replayGain = event.getReplayGain();
    	    	setVolume();
    	    	break;
    	}
    }	
}
