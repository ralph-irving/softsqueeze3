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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;
import javax.sound.sampled.spi.FormatConversionProvider;

import javazoom.spi.vorbis.sampled.file.VorbisEncoding;

import org.apache.log4j.Logger;
import org.kc7bfi.jflac.sound.spi.FlacEncoding;
import org.titmuss.softsqueeze.config.Config;



/**
 * Audio decoder for mp3 and flac.
 * 
 * @author Richard Titmuss
 */
public class AudioDecoder implements Runnable {
	private static final Logger vlogger = Logger.getLogger("javasound.verbose");
	
	private static final Logger logger = Logger.getLogger("javasound");

	private static final int DEFAULT_BUFFER_SIZE = 128000;

	public static final AudioFormat.Encoding MPEG1L3 = new Encoding("MPEG1L3"); 

	public static final AudioFormat.Encoding FLAC = FlacEncoding.FLAC; 

	public static final AudioFormat.Encoding OGG = VorbisEncoding.VORBISENC; 

	private static final HashMap MP3_DECODERS = new HashMap();
	
	private static int audioDecoderCount = 0;
	
    private AudioInputStream audioInputStream;
    
    private AudioBuffer outputBuffer;
    
    private long outputBufferPtr;
    
	private int lineBufferSize;

	private int sampleSizeInBits = 16;

	private boolean bigEndian = false;

	private Object lock = new Object();
	
	private boolean paused = true;

	private boolean playing = true;
	
	private boolean flush = false;

    private byte buf[] = new byte[DEFAULT_BUFFER_SIZE];
	
    private int bufLen = 0;
    
	static {
	    MP3_DECODERS.put("Java MP3 Plugin", new String[] {
	            "com.sun.media.codec.audio.mp3.JS_MP3FileReader",
	            "com.sun.media.codec.audio.mp3.JS_MP3ConversionProvider"
	    });
	    MP3_DECODERS.put("JLayer", new String[] {
	            "javazoom.spi.mpeg.sampled.file.MpegAudioFileReader",
	            "javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider"
	    });
	}

	
	public AudioDecoder(AudioBuffer decoderBuffer, AudioBuffer outputBuffer, float replayGain)
	throws AudioException {
	    
	    this.outputBuffer = outputBuffer;
	    this.outputBufferPtr = outputBuffer.getWriteCount();
	    logger.debug("new decoder. ptr="+outputBufferPtr);
	    
	    lineBufferSize = Config.getIntegerProperty("player.lineBufferSize");
	    
	    try {
	        AudioFormat audioFormat;
	        /*
	         * The decoder buffer might be (nearly) empty here. This can 
	         * cause the mp3 decoders to NPE, so lets spin to make sure
	         * we have some data in the buffer.  
	         */
	        int maxSpin = 5;
	        while (--maxSpin > 0 && decoderBuffer.available() < 1024) {
	            try {
		      Thread.sleep(100);
                    } catch (InterruptedException e1) {
                      }
		}

	        /*
	         * We need to use the mp3 decoder getInputStream to find out what audio
	         * format is used in the mp3.
	         * 
	         * The JFlac SPI is broken because it blocks if given an mp3 to decode
	         * in getAudioInputStream, so we call the mp3spi directly here.
	         */
	        if (decoderBuffer.getAudioFormat().getEncoding() == MPEG1L3) {
                String mp3decoder = Config.getProperty("audio.mp3decoder");

                Class mp3fileReaderClass = Class.forName(((String[]) MP3_DECODERS.get(mp3decoder))[0]);
                AudioFileReader fileReader = (AudioFileReader) mp3fileReaderClass.newInstance();

		      audioInputStream = fileReader.getAudioInputStream(new BufferedInputStream(decoderBuffer));
		      audioFormat = audioInputStream.getFormat();
	        }	        
	        else {
	            audioFormat = decoderBuffer.getAudioFormat();	        
	            audioInputStream = new AudioInputStream(decoderBuffer, audioFormat,
                    AudioSystem.NOT_SPECIFIED);
	        }	        
	        
	        /*
	         * We may need to set up audio conversion if the audio format is not
	         * directly supported by Java Sound.
	         */
	        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, lineBufferSize);
	        boolean isSupportedDirectly = AudioSystem.isLineSupported(info);
	        if (!isSupportedDirectly) {
	            logger.debug("conversion required for " + audioFormat + " ["
	                    + audioFormat.getClass().getName() + "]");

	            AudioFormat	targetFormat = new AudioFormat(
	    				AudioFormat.Encoding.PCM_SIGNED,
	    				audioFormat.getSampleRate(),
	    				sampleSizeInBits,
	    				audioFormat.getChannels(),
	    				audioFormat.getChannels() * (sampleSizeInBits / 8),
	    				audioFormat.getSampleRate(),
	    				bigEndian);
	            
	            /*
	             * Applets won't use the Sever Provider Interface that allows
	             * Java sound to automatically work out the format conversions,
	             * so for common cases we need to handle this ourselfs.
	             */
	            if (audioFormat.getEncoding().toString().startsWith("MPEG")) {
	                String mp3decoder = Config.getProperty("audio.mp3decoder");
	                
	                Class mp3decoderClass = Class.forName(((String[]) MP3_DECODERS.get(mp3decoder))[1]);
	                FormatConversionProvider fcp =
	                    (FormatConversionProvider)mp3decoderClass.newInstance();
	                
	                audioInputStream = fcp.getAudioInputStream(targetFormat, audioInputStream);
	            } else if (audioFormat.getEncoding().equals(FLAC)) {
	                audioInputStream = new org.kc7bfi.jflac.sound.spi.FlacFormatConvertionProvider()
	                .getAudioInputStream(targetFormat, audioInputStream);
	            } else {
	                // unknown, so try using the spi
	                audioInputStream = AudioSystem.getAudioInputStream(
	                        targetFormat, audioInputStream);
	            }
	            
	            logger.debug("CONVERSION PROVIDER: "+audioInputStream);	            
	        }

            outputBuffer.setAudioFormat(audioInputStream.getFormat());
	        outputBuffer.addReadEvent(0, new AudioEvent(outputBuffer, AudioEvent.BUFFER_PLAYING));
	        outputBuffer.addReadEvent(0, new AudioEvent(outputBuffer, AudioEvent.BUFFER_SET_REPLAYGAIN, replayGain));
	        
	    } catch (ClassNotFoundException e) {
	        logger.error("MP3 Decoder Unavailable", e);
	        throw new AudioException("MP3 Decoder is unavailable");
	    } catch (InstantiationException e) {
	        logger.error("MP3 Decoder Unavailable", e);
	        throw new AudioException("MP3 Decoder is unavailable");
	    } catch (IllegalAccessException e) {
	        logger.error("MP3 Decoder Unavailable", e);
	        throw new AudioException("MP3 Decoder is unavailable");
	    } catch (UnsupportedAudioFileException e) {
	        logger.error("Unsupported Audio Stream", e);
	        throw new AudioException("Unsupported Audio Stream");
        } catch (IOException e) {
	        logger.error("IOException", e);
	        throw new AudioException("IOException", e);
        }

        Thread t = new Thread(this, "AudioDecoder-"+(audioDecoderCount++));
	    t.setDaemon(true);
	    t.start();	    
	}

    /**
     * @return
     */
    public static String getDefaultMP3Decoder() {
        return getMP3Decoders()[0];

    }

	/**
	 * @return the available mp3 decoders.
	 */
    public static String[] getMP3Decoders() {
        ArrayList decoders = new ArrayList();
        
        for (Iterator i = MP3_DECODERS.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry) i.next();
            
            try {
                Class.forName(((String[]) e.getValue())[1]);
                decoders.add((String) e.getKey());
            } catch (ClassNotFoundException ex) {
            }
        }
        
        return (String[]) decoders.toArray(new String[decoders.size()]);
    }

    /**
     * @return true if Java MP3 Plugin is installed
     */
    public static boolean isMP3PluginInstalled() {
		boolean hasMP3Plugin = false;
		String mp3decoders[] = getMP3Decoders();
		for (int i=0; i<mp3decoders.length; i++) {
		    if (mp3decoders[i].equalsIgnoreCase("Java MP3 Plugin"))
		        return true;
		}
		return false;
    }

	/**
	 * Start audio playback.
	 */
	public void play() {
		unpause();
	}
	
	/**
	 * Stop audio playback, and close all lines.
	 */
	public void stop() {
		synchronized (lock) {
		    playing = false;
			lock.notifyAll();
		}
	}

	/**
	 * Resume audio playback.
	 */
	public void unpause() {
		synchronized (lock) {
			paused = false;
			lock.notifyAll();
		}
	}
	
	/**
	 * Pause audio playback.
	 */
	public void pause() {
		synchronized (lock) {
			paused = true;
			lock.notifyAll();
		}
	}

	/**
	 * Stop the decoder and flush the samples from the 
	 * output buffer.
	 */
	public void flush() {
		synchronized (lock) {
		    playing = false;
		    flush = true;
			lock.notifyAll();
		}	    
	}
	
	/**
	 * Read a sample from the audio stream and write it to the Line.
	 */
	public void run() {
		try {
			logger.debug("decoder started");

			int n = 0;
			while (n >= 0 && playing) {
				if (paused) {
					/* spin until player is unpaused */
					synchronized (lock) {
						logger.debug("decoder paused (waiting)");
						while (paused) {
							lock.wait();
						}
					}

					logger.debug("decoder playing");
				}

				n = decodeFrame();
			}
			
			audioInputStream.close();

			if (flush) {
			    logger.debug("decoder flushing output buffer readPtr="+outputBuffer.getReadCount()+" writePtr="+outputBuffer.getWriteCount()+" ptr="+outputBufferPtr);
			    outputBuffer.flush(outputBufferPtr);
			}

			outputBuffer.sendEvent( new AudioEvent(outputBuffer, AudioEvent.BUFFER_DECODER_STOPPED));
			logger.debug("decoder stopped");
		} catch (Exception e) {
			logger.warn("player thread exception ", e);
		}
	}

    
    private int slowStart = 2048;
    
    
	private int decodeFrame() throws IOException {
		int fillLen = buf.length - bufLen;
		
        /*	
         * If the output buffer is empty (or nearly empty) decode the buffer
         * in smaller sizes. This reduces the delay in getting pcm samples to
         * the audio mixer at the start of the track.
         */
		if (slowStart < buf.length) {
			fillLen = Math.min(slowStart, fillLen);
			logger.debug("decodeFrame: fill buffer. available=" + outputBuffer.available() + " fillLen=" + fillLen + " slowStart " + slowStart);
			slowStart = Math.min(slowStart * 2, buf.length);
		}	

		int br = audioInputStream.read(buf, bufLen, fillLen);		
	    if (br < 0)
	        return -1; /* eof */
	    
        bufLen += br;
        
	    int bw = outputBuffer.write(buf, 0, bufLen);
	    if (bw < 0)
	        return -1; /* eof */
	    
        // Keep any bytes left over in the buffer
        if (bw < bufLen) {
            System.arraycopy(buf, bw, buf, 0, bufLen - bw);
            vlogger.warn("audioDecoder did not write (" + bw
					+ ") the same number of bytes as read (" + bufLen
					+ ")");
        }
        bufLen -= bw;

	    return bw;
	}
		
	
    /*
     * In Java 1.4 the class javax.sound.sampled.AudioFormat.Encoding has protected
     * visability, so we need to sub class it.
     */
	private static class Encoding extends javax.sound.sampled.AudioFormat.Encoding {
	    protected Encoding(String name) {
	        super(name);
	    }
	}


}
