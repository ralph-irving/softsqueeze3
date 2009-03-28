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
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.Socket;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;

import org.apache.log4j.Logger;
import org.titmuss.softsqueeze.Softsqueeze;
import org.titmuss.softsqueeze.config.Config;
import org.titmuss.softsqueeze.config.ConfigListener;
import org.titmuss.softsqueeze.config.ConfigPopup;
import org.titmuss.softsqueeze.net.Protocol;
import org.titmuss.softsqueeze.net.ProtocolListener;
import org.titmuss.softsqueeze.visualizer.Visualizer;
import org.titmuss.softsqueeze.visualizer.VisualizerSpectrumAnalyser;
import org.titmuss.softsqueeze.visualizer.VisualizerVUMeter;


/**
 * 
 * @author richard
 */
public class Player implements ProtocolListener, AudioBufferListener, ConfigListener {
	private static final Logger logger = Logger.getLogger("player");

	private static final Logger vlogger = Logger.getLogger("player.verbose");

	public static final int DEFAULT_BUFFER_SIZE = 128000;

	private Softsqueeze squeeze;
	
	private Socket audioSocket;

	private PushbackInputStream audioInputStream;

	private AudioMixer audioMixer;
	
	private AudioDecoder audioDecoder;
	
	private AudioStream audioStream;
	
	private AudioBuffer decoderBuffer;
	
	private AudioBuffer outputBuffer;
	
	private byte format;
	
	private int crossfade;

	private int state;

	private boolean autostart;
	
	private boolean directStream;

	private long statusTime;
	
	private Object lock = new Object();
	
	private boolean running = true;
	
    private int pcmSampleSize;

    private int pcmSampleRate;

    private int pcmChannels;

    private boolean pcmEndian;

    private int autostartThreshold;

    private byte transitionPeriod;

    private byte transitionType;

    private boolean loopSong;
    
    private float replayGain;

	private int interval;
    
    private String ipaddr;
    
    private int port;
    
    private String httpHeaders;

    private float leftLevel = 0;

    private float rightLevel = 0;

    private Visualizer visualizer;
    
    private byte[] visualizerFrame = new byte[0];
    
    private String lastMetaData = null;

    
	public final static int DISCONNECTED = 0;

	public final static int CONNECTED = 1;

	public final static int BUFFERING = 2;

	public final static int PLAYING = 3;

	public final static int PAUSED = 4;

	private final static int DECODER_BUFFER_SIZE = 1048576;
	
	private final static int OUTPUT_BUFFER_SIZE = 10*2*44100*4;
	
	private final static int OUTPUT_BUFFER_THRESHOLD = 1; //882000; // 0.25 secs

	
	
	public Player(Softsqueeze squeeze) {
		this.squeeze = squeeze;
		state = DISCONNECTED;
		
		squeeze.getProtocol().addProtocolListener("strm", this);
		squeeze.getProtocol().addProtocolListener("cont", this);
		squeeze.getProtocol().addProtocolListener("body", this);
		squeeze.getProtocol().addProtocolListener("audg", this);
		squeeze.getProtocol().addProtocolListener("visu", this);
		squeeze.getProtocol().addProtocolListener("stat", this);
		squeeze.getProtocol().addProtocolListener("visg", this);
		 
		Config.addConfigListener(this);

		outputBuffer = new AudioBuffer(OUTPUT_BUFFER_SIZE);
        outputBuffer.addListener(this);

        /*
        try {
            FileOutputStream os = new FileOutputStream("outputBuffer.pcm");
            outputBuffer.setOutputStream(os);
        } catch (FileNotFoundException e) {
        }
        */
        
        initMixer();
		
		Thread t = new Thread(new PlayerStatus(), "Player Status");
		t.setDaemon(true);
		t.start();
	}

    /* (non-Javadoc)
     * @see com.slim.softsqueeze.ConfigListener#configSet(java.lang.String, java.lang.String)
     */
    public void configSet(String key, String value) {
        if (key.equals("audio.mixer") || key.equals("audio.lineBufferSize")) {
            initMixer();
        }
    }
    
    public void initMixer() {
        try {
            if (audioMixer != null)
                audioMixer.stop();
            
            audioMixer = new AudioMixer(outputBuffer);
            audioMixer.setVolume(leftLevel, rightLevel);
		    audioMixer.setVisualizer(visualizer);
            
            if (state == PLAYING)
                audioMixer.play(0);
        }
        catch (AudioException e) {
            logger.error("Changing mixer", e);
        } catch (LineUnavailableException e) {
		    ConfigPopup.showErrorDialog("Sorry SoftSqueeze could not find a sound card on your computer.", "No Sound Card Detected");
        }
    }
    

	/* (non-Javadoc)
	 * @see com.slim.softsqueeze.ProtocolListener#slimprotoCmd(java.lang.String, byte[], int, int)
	 */
	public void slimprotoCmd(String cmd, byte[] buf, int off, int len) {
		if (cmd.equals("strm")) {
			char scmd = parseStream(buf, off, len);

			try {
				switch (scmd) {
					
					case 's': // start
						connect();
						autostart();
						break;
					case 'u': // unpause
						start(interval);
						break;
					case 'p': // pause
						pause(interval);
						break;
					case 'q': // quit			        
						disconnect();
						break;
					case 'f': // flush			        
						flush();
						break;
					case 't': // status
						sendStatus("STMt", interval);
						break;
					case 'a': // status
						skipAhead(interval);
						break;
					default:
						logger.warn("Unknown strm command " + scmd);
			    }
			} catch (IOException e) {
			    logger.error("strm IO error", e);
			} catch (AudioException e) {
			    logger.error("strm Audio error", e);
            }
		}
		else if (cmd.equals("stat")) {
			try {
				sendStatus("stat");
			} catch (IOException e) {
			}
		}
		else if (cmd.equals("cont")) {
		    try {
                int metaint = Protocol.unpackN4(buf, off);
                loopSong = ((buf[off + 4] & 0x01) == 0x01);
                cont(metaint);
            } catch (AudioException e) {
                logger.error("cont Audio error", e);
            }
		}
		else if (cmd.equals("body")) {
		    try {
                int length = Protocol.unpackN4(buf, off);
                body(length);
            } catch (IOException e) {
                logger.error("cont Audio error", e);
            }
		}
		else if (cmd.equals("audg")) {
		    int oldLeft = Protocol.unpackN4(buf, off);
		    int oldRight = Protocol.unpackN4(buf, off+4);
		    // off+8; digital volume control
		    // off+9; preamp
		    leftLevel = Protocol.unpackFixedPoint(buf, off+10);
		    rightLevel = Protocol.unpackFixedPoint(buf, off+14);
		    
		    logger.debug("audg oldLeft=" + oldLeft + " oldRight=" + oldRight + " newLeft=" + leftLevel + " newRight=" + rightLevel);
		    
		    audioMixer.setVolume(leftLevel, rightLevel);
		}
		else if (cmd.equals("visu")) {
		    int frameLen = len - off;

		    // Ignore visu frame if this is a repeat of the last frame
		    if (frameLen == visualizerFrame.length) {
		        int i = 0;
		        while (i < visualizerFrame.length) {
		            if (buf[off+i] != visualizerFrame[i++])
		                break;
		        }
		        if (frameLen == i)
		            return;
		    }

		    visualizerFrame = new byte[frameLen];
		    System.arraycopy(buf, off, visualizerFrame, 0, frameLen);
		    
		    // Change visualizer
		    int visType = buf[off];
		    switch (visType) {
		    case 1:
		        visualizer = new VisualizerVUMeter(squeeze, buf, off, len);
		        break;
		    case 2:
		        visualizer = new VisualizerSpectrumAnalyser(squeeze, buf, off, len);
		        break;
		    default:
		        visualizer = null;
		    }

		    audioMixer.setVisualizer(visualizer);
		}
		else if (cmd.equals("visg")) {
			VisualizerVUMeter.uploadGraphic(buf, off, len);
		}
	}

	/* (non-Javadoc)
	 * @see com.slim.softsqueeze.ProtocolListener#slimprotoConnected()
	 */
	public void slimprotoConnected() {
        // When reconnecting to the SqueezeCenter ensure any existing
	    // streams are closed.
	    try {
            disconnect();
        } catch (IOException e) {
        }

        audioMixer.setVisualizer(visualizer);
	}

	/* (non-Javadoc)
	 * @see com.slim.softsqueeze.ProtocolListener#slimprotoDisconnected()
	 */
	public void slimprotoDisconnected() {
	    audioMixer.setVisualizer(null);
	}


	/**
	 * Connect to the audio stream
	 * 
	 * @throws IOException
	 * @throws AudioException
	 */
	private void connect() throws IOException, AudioException {
	    logger.debug("connect: state " + state);
	    
	    synchronized (lock) {
	        if (audioSocket != null)
	            audioSocket.close();
	        
	        logger.debug("http connect: ipaddr=" + ipaddr + " port=" + port);
	        audioSocket = new Socket(ipaddr, port);
	        
	        int tcpwindow = Config.getIntegerProperty("audio.tcpwindowsize");
	        if (tcpwindow > 0) {
	            audioSocket.setReceiveBufferSize(tcpwindow);
	            Config.putIntegerProperty("audio.tcpwindowsize", audioSocket.getReceiveBufferSize());
	        }
	        
	        audioInputStream = new PushbackInputStream(audioSocket.getInputStream(), DEFAULT_BUFFER_SIZE);
	        OutputStream audioOutputStream = audioSocket.getOutputStream();
	        sendStatus("STMc");

	        // Work around for SqueezeCenter bug
	        if (httpHeaders.endsWith("\r\n\r\n\r\n"))
	            httpHeaders = httpHeaders.substring(0, httpHeaders.length()-2);
	        
	        logger.debug("http write headers: " + httpHeaders);
	        audioOutputStream.write(httpHeaders.getBytes());
	        audioOutputStream.flush();

	        byte buf[] = new byte[2048];
	        int bufLen = 0;
	        while (bufLen < buf.length) {
	            int n = audioInputStream.read(buf, bufLen, buf.length - bufLen);
	            if (n == -1) {
	                logger.debug("end of stream reading headers: " + bufLen);
	                break; // end of stream
	            }
	            
	            bufLen += n;
	        }

	        if (bufLen == 0) {
	            squeeze.getProtocol().sendDsco(3);
	            throw new AudioException("Cannot read http headers");
	        }
	        
            int i = 0;
            while (i < bufLen - 5) {
                if (buf[i] == '\r' && buf[i + 1] == '\n' && buf[i+2] == '\r' && buf[i+3] == '\n')
                    break;
                i++;
            }
            i += 4;

            logger.debug("http read header: (len " + i + "/" + bufLen + ") " + new String(buf, 0, i));

            audioInputStream.unread(buf);
            audioInputStream.skip(i);
	        sendStatus("STMh");

	        state = CONNECTED;
	        
	        if (directStream) {
	            squeeze.getProtocol().sendResp(new String(buf, 0, i));
	        }
	        else {
	            initStream(-1);
	        }
	    }
	}
	
	/**
	 * Continuing with the stream connection. This is used to play the
	 * stream with direct streaming.
	 *  
	 * @param metaint icy meta data interval
	 * @throws AudioException
	 */
	private void cont(int metaint) throws AudioException {
	    switch (state) {
	    case CONNECTED:
	        break;
	    
	    case BUFFERING:
	    case PLAYING:
	    case PAUSED:
	        return;
	       
	    default:
	        throw new IllegalStateException("Cannot cont in state: " + state);
	    }

	    logger.debug("http cont: metaint=" + metaint + " loop=" + loopSong);
	    initStream(metaint);
	}
	
	/**
	 * Sent the stream body to the SqueezeCenter. This is used to parse
	 * playlists for direct streaming.
	 * 
	 * @param length
	 * @throws IOException
	 */
	private void body(int length) throws IOException {
	    switch (state) {
	    case CONNECTED:
	        break;
	        
	    default:
	        throw new IllegalStateException("Cannot body in state: " + state);
	    }
	    
	    logger.debug("http body: length=" + length);
	    
	    int bufLen = (length > 0) ? length : Integer.MAX_VALUE;	    
	    byte buf[] = new byte[4096];
	    
	    int n = audioInputStream.read(buf, 0, Math.min(bufLen, buf.length));
	    while (n > 0 && bufLen > 0) {
	        bufLen -= n;
	        squeeze.getProtocol().sendBody(new String(buf, 0, n));
	        
	        n = audioInputStream.read(buf, 0, Math.min(bufLen, buf.length));
	    }
        squeeze.getProtocol().sendBody(""); // end of body
	}
	
	/**
	 * Once we are connected, and headers parsed this initialises the stream
	 * 
	 * @param metaint
	 * @throws AudioException
	 */
	private void initStream(int metaint) throws AudioException {	    
	    synchronized (lock) {	        
	        logger.debug("initStream: state " + state);
	        
	        AudioFormat audioFormat;
			if (format == 'm') { // mp3
				audioFormat = new AudioFormat(AudioDecoder.MPEG1L3, 44100f,
						AudioSystem.NOT_SPECIFIED, 2, AudioSystem.NOT_SPECIFIED,
						AudioSystem.NOT_SPECIFIED, false);
			} else if (format == 'f') { // flac
				audioFormat = new AudioFormat(AudioDecoder.FLAC, 44100f,
						AudioSystem.NOT_SPECIFIED, 2, AudioSystem.NOT_SPECIFIED,
						AudioSystem.NOT_SPECIFIED, false);
			} else if (format == 'p') { // pcm
				float pcmSampleRates[] = {
					11025f, 22050f, 32000f, 44100f, 48000f, 
					8000f, 12000f, 16000f, 24000f, 96000f
				};
				int pcmSampleWidths[] = {
				  8, 16, 24, 32
				};
				
				float sampleRate = pcmSampleRates[pcmSampleRate];
				int sizeInBits = pcmSampleWidths[pcmSampleSize];
				
				audioFormat = new AudioFormat(
							AudioFormat.Encoding.PCM_SIGNED,
							sampleRate,
							16,
							pcmChannels, 
							pcmChannels * (sizeInBits / 8),
							sampleRate,
							pcmEndian);				
			} else if (format == 'o') { // ogg
				/* one step less to non-hardcoded format */
				audioFormat = new AudioFormat(AudioDecoder.OGG,	44100f,
						AudioSystem.NOT_SPECIFIED, 2, AudioSystem.NOT_SPECIFIED,
						AudioSystem.NOT_SPECIFIED, false);
			} else {
				throw new AudioException("Unknown audio format requested: " + format);
			}

	        decoderBuffer = new AudioBuffer(DECODER_BUFFER_SIZE);	        
	        decoderBuffer.setAudioFormat(audioFormat);
	        decoderBuffer.addListener(this);
	        
	        /*
	        try {
	            FileOutputStream os = new FileOutputStream("decoderBuffer-"+System.currentTimeMillis()+".mp3");
	            decoderBuffer.setOutputStream(os);
	        } catch (FileNotFoundException e) {
	        }
	        */
	        
	        decoderBuffer.addWriteEvent(autostartThreshold, new AudioEvent(decoderBuffer, AudioEvent.BUFFER_THRESHOLD));
	        outputBuffer.addWriteEvent(OUTPUT_BUFFER_THRESHOLD, new AudioEvent(outputBuffer, AudioEvent.BUFFER_THRESHOLD));
	        
	        if (loopSong)
	            decoderBuffer.setRepeat(true);
	        
	        audioStream = new AudioStream(audioInputStream, decoderBuffer, metaint);
	        audioDecoder = new AudioDecoder(decoderBuffer, outputBuffer, replayGain);
	        
	        audioDecoder.play();
	        
	        state = BUFFERING;
	        lock.notifyAll();
	    }
	}

    /**
     * Disconnect from the stream
     * @throws IOException
     */
    public void disconnect() throws IOException {
        logger.debug("disconnect: state " + state);
        
        synchronized (lock) {
            if (state == DISCONNECTED)
                return;

            if (state > BUFFERING) {
                audioDecoder.stop();
                decoderBuffer.close();
                audioStream.stop();
                audioMixer.flush();     
            }

            audioSocket.close();
            
            audioInputStream = null;
            audioSocket = null;
            audioStream = null;
            decoderBuffer = null;
            audioDecoder = null;
            
            if (autostart)
                sendStatus("STMf");
            
            state = DISCONNECTED;
            lock.notifyAll();
        }
    }
	
    /**
     * Flush the buffering stream
     * @throws IOException
     * 
     * @throws IOException
     */
    private void flush() throws IOException {
        logger.debug("flush: state " + state);

        synchronized (lock) {
            if (state == DISCONNECTED)
                return;
            
            audioDecoder.flush();
            
            audioStream = null;
            decoderBuffer = null;
            audioDecoder = null;
            
            state = PLAYING; // playout existing track
            lock.notifyAll();
        }
    }
    
	/**
	 * Start, or unpause the player.
	 * @throws IOException
	 * @throws AudioException
	 */
	private void autostart() throws IOException, AudioException {
	    logger.debug("autostart: state " + state);
	    
	    switch (state) {
	    case DISCONNECTED:
	        throw new IllegalStateException("Cannot autostart in disconnected state");
	        
	    case CONNECTED:
	    case PLAYING:
	        return;
	        
	    case PAUSED:
	        unpause();
	        return;
	    }
	    
	    // don't autostart until remaining audio in playout buffer < crossfade timeout
	    /* FIXME
	    if (playoutBuffer != null) {
            int avail = playoutBuffer.available();
            float frameRate = playoutBuffer.getAudioFormat().getFrameRate();
            int frameSize = playoutBuffer.getAudioFormat().getFrameSize();
            int channels = playoutBuffer.getAudioFormat().getChannels();
        
	        float remaining = avail / channels / frameRate / frameSize;	
	        if (crossfade < remaining) {
	            logger.debug("autostart: wait before crossfade remaining = " + remaining + " crossfade = " + crossfade);
	            return;	        
	        }
	    }
	    */
	    
	    long fullness = getBufferFullness();
	    logger.debug("autostart: fullness=" + fullness + " threshold=" + autostartThreshold);
	    
	    if (fullness < autostartThreshold)
	        return;
	    
	    if (!autostart) {
	        sendStatus("STMl");
	    }
	    else {
	        start(0);
	    }
	}
	
	/**
	 * Start the player.
	 * @throws IOException
	 * @throws AudioException
	 */
	private void start(int atJiffies) throws IOException, AudioException {
	    logger.debug("start: state " + state + ", atJiffies=" + atJiffies);
	    
	    synchronized (lock) {
	        if (state == PLAYING)
	            return;

	        if (state == DISCONNECTED)
	            connect();
	        
	        audioMixer.play(atJiffies != 0 ? squeeze.getProtocol().getEpoch() + atJiffies : 0);
	        // sendStatus("STMa"); /* not used by Squeezebox2 */ 
	        
	        state = PLAYING;
	        lock.notifyAll();		
	    }
	}
	
	/**
	 * Unpause the player.
	 * @throws IOException
	 */
	private void unpause() throws IOException {
	    logger.debug("start: state " + state);
	    
	    synchronized (lock) {
	        audioMixer.play(0);
	        sendStatus("STMr");
	        
	        state = PLAYING;
	        lock.notifyAll();
	    }
	}
	
	/**
	 * Pause the player.
	 * @throws IOException
	 */
	private void pause(int interval) throws IOException {
		logger.debug("pause: state " + state + ", interval " + interval);
		
		synchronized (lock) {
			if (state != PLAYING)
				return;
			
			audioMixer.pause(interval);

			if (interval == 0) {
				sendStatus("STMp");
				state = PAUSED;
			}

			lock.notifyAll();
		}
	}

	
	/**
	 * Pause the player.
	 * @throws IOException
	 */
	private void skipAhead(int interval) throws IOException {
	    logger.debug("skipahead: interval " + interval);
	    
	    synchronized (lock) {
	        if (state != PLAYING)
	            return;
	        
	        audioMixer.skipAhead(interval);

	        lock.notifyAll();
	    }
	}

	
	private void sendStatus(String status) throws IOException {
		sendStatus(status, 0);
	}

	private void sendStatus(String status, int timestamp) throws IOException {
		byte crlf = (byte) 0; // debug, not used by server
		byte masInit = format;
		byte masMode = (byte) 1; // debug, not used by server

		/*
         * To get sync working Softsqueeze starts decoding as soon as the stream
         * is connected. The Squeezebox 2 however only starts decoding when the
         * buffer threshold is reached. To allow the 5in5 rule to work for
         * internet radio we must report the total write count while buffering.
         */
		int decoderFullness ;
		if (decoderBuffer == null) {
		    decoderFullness = 0;
		    logger.debug("buffer null state " + state);
		}
		else if (state == BUFFERING) {
		    decoderFullness = (int) decoderBuffer.getWriteCount();
		    logger.debug("write count " + decoderBuffer.getWriteCount() + " available " + decoderBuffer.available());
		}
		else {
		    decoderFullness = decoderBuffer.available();
			vlogger.debug("write count " + decoderBuffer.getWriteCount() + " available " + decoderBuffer.available());
		}
		
		long bytesRx = (decoderBuffer == null) ? 0 : decoderBuffer.getWriteCount();
		byte signal = (byte) 0xFF; // wired squeezebox
		int outputFullness = outputBuffer.available();
		long elapsedMilliseconds = audioMixer.getElapsedMilliseconds();

		statusTime = System.currentTimeMillis();
		
		if (vlogger.isDebugEnabled()) {
		    if (decoderBuffer != null)
		        logger.debug("decode: "+((decoderBuffer.available()/(float)decoderBuffer.getBufferSize())*100.0)+" avail="+decoderBuffer.available()+" size="+decoderBuffer.getBufferSize());
		    logger.debug("output: "+((outputBuffer.available()/(float)outputBuffer.getBufferSize())*100.0)+" avail="+outputBuffer.available()+" size="+outputBuffer.getBufferSize());
		    vlogger.debug("status=" + status + " fullness=" + decoderFullness + " bytesRx=" + bytesRx + " elapsedMilliseconds=" + elapsedMilliseconds);		    
		}
		else if (!status.equals("STMt") && logger.isDebugEnabled()) {
		    logger.debug("status=" + status + " fullness=" + decoderFullness + " bytesRx=" + bytesRx + " elapsedMilliseconds=" + elapsedMilliseconds);
		}
		
		squeeze.getProtocol().sendStat(status, crlf, masInit, masMode, 
		        DECODER_BUFFER_SIZE, decoderFullness, bytesRx, signal, 
		        OUTPUT_BUFFER_SIZE, outputFullness, elapsedMilliseconds, timestamp);
	}

	private class PlayerStatus implements Runnable {

	    private PlayerStatus() {
		}

		public void run() {
		    synchronized (lock) {
		        while (running) {
		            while (state != BUFFERING && state != PLAYING) {
		                try {			                
		                    lock.wait();
		                } catch (InterruptedException e) {
		                }
		            }
		            
		            long timeout = statusTime - System.currentTimeMillis() + 1000;
		            while ( (state == PLAYING || state == BUFFERING) && timeout > 0) {
		                try {
		                    lock.wait(timeout);
		                } catch (InterruptedException e) {
		                }
		                timeout = statusTime - System.currentTimeMillis() + 1000;
		            }
		            
		            try {
		                sendStatus("STMt");
		            } catch (IOException e) {
		                logger.error("PlayerStatus IO Error", e);
		            }
		        }
		    }
		}
	}
	
	/**
	 * Invoked when buffer the buffer is empty on a read.
	 */
	public void bufferEvent(AudioEvent event) {
	    AudioBuffer buffer = (AudioBuffer) event.getSource();
	    try {
	        switch (event.getId()) {
	        case AudioEvent.BUFFER_UNDERRUN:
	            vlogger.debug("buffer underrun; audioStream="+audioStream+" isOpen="+((audioStream!=null && audioStream.isOpen())?"open":"closed"));
	            
	            if (buffer == decoderBuffer 
	                    && (audioStream == null || !audioStream.isOpen()))
	                sendStatus("STMd");
	            
	            else if (buffer == outputBuffer 
	                    && (audioStream == null || !audioStream.isOpen())) {
	                audioMixer.drain();
	                sendStatus("STMu");
	            }
	        break;
	        
	        case AudioEvent.BUFFER_PLAYING:
	            sendStatus("STMs");
	        break;
	        
	        case AudioEvent.BUFFER_THRESHOLD:
	            if (state == BUFFERING)
	                autostart();
	        break;

	        case AudioEvent.BUFFER_CLOSED:
	            if (buffer == decoderBuffer) {
	                squeeze.getProtocol().sendDsco(0); // connection closed normally
	            }
	            if (buffer == decoderBuffer && state == BUFFERING) {
		            logger.debug("audio stream closed while buffering, starting playback");
	                start(0);
	            }
	        break;
	        
	        case AudioEvent.BUFFER_METADATA:
	            if (!event.getMetadata().equals(lastMetaData))
	                squeeze.getProtocol().sendMeta(event.getMetadata());
	            
	            lastMetaData = event.getMetadata();
	        break;
	        
	        }
	    } catch (IOException e) {
	        logger.error("buffer underrun IO error", e);
	    } catch (AudioException e) {
	        logger.error("audio exception", e);
        }
	}

	private int getBufferFullness() throws IOException {
	    /* 
	     * Let's make sure we have some samples in the output buffer
	     * before we autostart. Otherwise we risk an audio underrun 
	     * when playback starts before the output buffer fills.
	     */
	    if (outputBuffer.available() < OUTPUT_BUFFER_THRESHOLD)
	        return 0;
	    return (int)decoderBuffer.getWriteCount();
	}
	
	private char parseStream(byte buf[], int start, int len) {
		char cmd = (char)buf[start];
		byte autostartFlag = buf[start + 1];
		autostart = (autostartFlag == '1' || autostartFlag == '3');
		directStream = (autostartFlag == '2' || autostartFlag == '3' || autostartFlag == '4');		
		format = buf[start + 2];
		pcmSampleSize = buf[start + 3] - '0';
		pcmSampleRate = buf[start + 4] - '0';
		pcmChannels = buf[start + 5] - '0';
		pcmEndian = "0".equals(new String(buf, start + 6, 1));
		autostartThreshold = (buf[start + 7] & 0xFF) * 1024;
		String spdifEnable = new String(buf, start + 8, 1);
		transitionPeriod = buf[start + 9];
		transitionType = buf[start + 10];
		loopSong = (buf[11] == '1');
		// buf[start + 12]; reserved
		
		switch (cmd) {
			case 's':
				replayGain = Protocol.unpackFixedPoint(buf, start + 14);
				if (replayGain == 0.0f)
					replayGain = 1.0f; // Use 0.00dB with no replay gain
				break;
			case 'p':
			case 'u':
			case 'a':
			case 't':
				interval = Protocol.unpackN4(buf, start + 14); 
				break;
		}
		
		// reserved
		port = Protocol.unpackN2(buf, start + 18);
		StringBuffer ipaddrBuf = new StringBuffer();
		for (int i = 0; i < 4; i++) {
			ipaddrBuf.append(Integer.toString( buf[start + 20 + i] & 0xFF));
			if (i < 3)
				ipaddrBuf.append(".");
		}
		ipaddr = ipaddrBuf.toString();
		if (ipaddr.equals("0.0.0.0"))
			ipaddr = Config.getSlimServerAddress();

		httpHeaders = new String(buf, start + 24, len - start - 24);
		logger.debug("httpRequest=" + httpHeaders);

		logger.debug("parsed strm: command=" + cmd + " format=" + format
		        + " crossfade=" + crossfade + " replygain=" + replayGain
				+ " ipaddr=" + ipaddr + " port=" + port
				+ " autostart=" + autostart + " autostartThreshold=" + autostartThreshold );

		return cmd;
	}
}
