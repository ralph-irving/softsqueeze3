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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * 
 * @author richard
 */
public class Protocol {
	private static Logger logger = Logger.getLogger("slimproto");

	private static HashMap discoveredServers = new HashMap();

	private static int threadCount = 0;
	
	private long epoch;

	private HashMap commandListeners = new HashMap();

	private HashSet connectionListeners = new HashSet();

	// Slim TCP protocol
	private TcpSocket tcpSocket;

	private boolean helosent = false;

	// Slim UDP protocol
	private UdpSocket udpSocket;

	private boolean verbose = true;

	public Protocol() {
		epoch = System.currentTimeMillis();

		udpSocket = new UdpSocket();
		udpSocket.start();
	}
	
	public void addProtocolListener(String cmd, ProtocolListener listener) {
		ArrayList l = (ArrayList)commandListeners.get(cmd);
		if (l == null) {
			l = new ArrayList();
			commandListeners.put(cmd, l);
		}
		l.add(listener);		
		connectionListeners.add(listener);
	}
	
	public void removeProtocolListener(String cmd, ProtocolListener listener) {
		ArrayList l = (ArrayList)commandListeners.get(cmd);
		if (l == null)
			return;
		l.remove(listener);
		connectionListeners.add(listener);
	}

	/**
	 * Create a new connection to the slim server using the given address and
	 * port.
	 */
	public void connect(InetAddress addr, int port) {
	    if (addr == null || port < 0) {
	        logger.debug("Invalid server parameters");
	        return;
	    }
	    
		tcpSocket = new TcpSocket(addr, port);
		tcpSocket.start();
	}

	/**
	 * Returns true if connected to the slim server.
	 */
	public boolean isConnected() {
		if (tcpSocket == null)
			return false;
		return tcpSocket.isConnected();
	}

	/**
	 * Returns the number of jiffies since the player started.
	 */
	public int getJiffies() {
		return (int) (System.currentTimeMillis() - epoch);
	}

	/**
	 * Returns a hash map with the discovered slim servers.
	 */
	public static HashMap getDiscoveredServers() {
		return discoveredServers;
	}

	/**
	 * Send a slim server discovery request.
	 */
	public void sendDiscoveryRequest(int deviceID, int revision,
			byte[] macaddress) {
		byte args[] = new byte[18];
		args[0] = 'd';
		args[2] = (byte) deviceID;
		args[3] = (byte) revision;
		System.arraycopy(macaddress, 0, args, 12, 6);

		try {
			InetAddress broadcast = InetAddress.getByName("255.255.255.255");
			DatagramPacket p = new DatagramPacket(args, 18, broadcast, 3483);
			udpSocket.send(p);
		} catch (IOException e) {
			logger.error("Sending broadcast packet", e);
		}
	}

	/**
	 * Send a hello message to the slim server.
	 */
	public void sendHELO(int deviceID, int revision, byte[] macaddress,
			boolean isGraphics, boolean isReconnect) {
		if (!isConnected())
			return;

		try {
			byte args[] = new byte[10];
			args[0] = (byte) deviceID;
			args[1] = (byte) revision;
			System.arraycopy(macaddress, 0, args, 2, 6);

			int channelList = 0;
			if (isGraphics)
				channelList |= 0x8000;
			if (isReconnect)
				channelList |= 0x4000;
			packN2(args, 8, channelList);

			sendCommand("HELO", args);
			helosent = true;
		} catch (IOException e) {
			logger.debug("Exception in sendHelo", e);
		}
	}

	/**
	 * Send a IR command to the slim server.
	 */
	public void sendIR(int format, int noBits, int irCode) {
		if (!isConnected() || !helosent)
			return;

		try {
			byte args[] = new byte[10];
			packN4(args, 0, getJiffies());
			args[4] = (byte) format;
			args[5] = (byte) noBits;
			packN4(args, 6, irCode);

			sendCommand("IR  ", args);
		} catch (IOException e) {
			logger.debug("Exception in sendIR", e);
		}
	}

	public void sendButton(int code) {
		if (!isConnected() || !helosent)
			return;

		try {
			byte args[] = new byte[8];
			packN4(args, 0, getJiffies());
			packN4(args, 4, code);

			sendCommand("BUTN", args);
		} catch (IOException e) {
			logger.debug("Exception in sendBUTN", e);
		}
	}

	/**
	 * Send a KNOB command to the Squeezebox Server.
	 * @param sync 
	 */
	public void sendKnob(int position, int sync) {
		if (!isConnected() || !helosent)
			return;

		try {
			byte args[] = new byte[9];
			packN4(args, 0, getJiffies());
			packN4(args, 4, position);
			args[8] = (byte) sync;
			
			sendCommand("KNOB", args);
		} catch (IOException e) {
			logger.debug("Exception in sendIR", e);
		}	    
	}

	/**
	 * Send an ANIC command to the slim server.
	 */
	public void sendANIC() {
		if (!isConnected() || !helosent)
			return;

		try {
			sendCommand("ANIC", new byte[0]);
		} catch (IOException e) {
			logger.debug("Exception in sendIR", e);
		}
	}
	
	/**
	 * Send a status update to the slim server.
	 */
	public void sendStat(String code, byte crlf, byte masInit, byte masMode,
			int rptr, int wptr, long bytesRx, byte wirelessSignal, 
			int outputBufferSize, int outputBufferFullness, long elapsedMilliseconds, int timestamp) {
		if (!isConnected() || !helosent)
			return;

		try {
			byte args[] = new byte[51];
			System.arraycopy(code.getBytes(), 0, args, 0, 4);
			args[4] = crlf;
			args[5] = masInit;
			args[6] = masMode;
			packN4(args, 7, rptr);
			packN4(args, 11, wptr);
			packN8(args, 15, bytesRx);
			packN2(args, 23, wirelessSignal);
			packN4(args, 25, getJiffies());
			packN4(args, 29, outputBufferSize);
			packN4(args, 33, outputBufferFullness);
			packN4(args, 37, (int)(elapsedMilliseconds/1000));
			packN4(args, 41, 0); // voltage
			packN4(args, 43, (int)elapsedMilliseconds);
			packN4(args, 47, timestamp);

			sendCommand("STAT", args);
		} catch (IOException e) {
			logger.debug("Exception in sendStat", e);
		}
	}

	/**
	 * Send disconnection
	 * 
	 * 0    connection closed normally (FIN)
	 * 1    connection reset by local host
	 * 2    connection reset by remote host
	 * 3    unreachable
	 * 4    timed out
	 *  
	 * @param metadata
	 */
	public void sendDsco(int code) {
		if (!isConnected() || !helosent)
			return;

		try {
			byte args[] = new byte[1];
			args[0] = (byte) code;

			sendCommand("DSCO", args);
		} catch (IOException e) {
			logger.debug("Exception in sendDsco", e);
		}
	}
	
	/**
	 * Sent stream headers
	 */
	public void sendBody(String body) {
		if (!isConnected() || !helosent)
			return;

	    try {
	        sendCommand("BODY", body.getBytes());
		} catch (IOException e) {
			logger.debug("Exception in sendMeta", e);
		}
	}

	/**
	 * Sent stream headers
	 */
	public void sendResp(String headers) {
		if (!isConnected() || !helosent)
			return;

	    try {
	        sendCommand("RESP", headers.getBytes());
		} catch (IOException e) {
			logger.debug("Exception in sendMeta", e);
		}
	}

	/**
	 * Sent stream meta-data
	 */
	public void sendMeta(String metadata) {
		if (!isConnected() || !helosent)
			return;

	    try {
	        sendCommand("META", metadata.getBytes());
		} catch (IOException e) {
			logger.debug("Exception in sendMeta", e);
		}
	}
	
	/**
	 * Send a bye command.
	 */
	public void sendBye() {
		if (!isConnected() || !helosent)
			return;

		try {
			sendCommand("BYE!", new byte[1]);
			tcpSocket.close();
			tcpSocket = null;
		} catch (IOException e) {
			logger.debug("Exception in sendBye", e);
		}
	}

	/**
	 * Send a command to slim server
	 */
	private void sendCommand(String cmd, byte[] args) throws IOException {
		int len = args.length;
		byte buf[] = new byte[len + 8];

		System.arraycopy(cmd.getBytes(), 0, buf, 0, 4);
		packN4(buf, 4, len);
		System.arraycopy(args, 0, buf, 8, args.length);

		tcpSocket.write(buf);

		logger.debug("tcp send: " + cmd + " length=" + len);
	}

	private void socketConnected(TcpSocket socket) {
	    if (tcpSocket != socket)
	        return;
	    
		logger.debug("command socket connected");
		for (Iterator i=connectionListeners.iterator(); i.hasNext(); ) {
		    ProtocolListener p = (ProtocolListener) i.next();
		    p.slimprotoConnected();
		}
	}

	private void socketDisconnected(TcpSocket socket) {
	    if (tcpSocket != socket)
	        return;
	    
		logger.debug("command socket disconnected");
		for (Iterator i=connectionListeners.iterator(); i.hasNext(); ) {
		    ProtocolListener p = (ProtocolListener) i.next();
		    p.slimprotoDisconnected();
		}
	}
	
	private void socketCommand(byte buf[], int offset, int len) {
		String cmd = new String(buf, offset, 4);
		offset += 4;

		if (logger.isDebugEnabled()) {
		    if (verbose) {
		        StringBuffer str = new StringBuffer();
		        str.append("tcp recv: "+cmd+" ");
		        for (int i = offset; i < len; i++) {
		            str.append(Integer
		                    .toString((buf[i] & 0xFF), 16));
		            str.append(" ");
		        }
		        logger.debug(str.toString());
		    }
		    else {
		        logger.debug("tcp recv: "+cmd+" length="+len);
		    }
		}

		ArrayList l = (ArrayList)commandListeners.get(cmd);
		if (l == null)
			return;
		
		for (Iterator j=l.iterator(); j.hasNext(); ) {
			ProtocolListener p = (ProtocolListener) j.next();
			p.slimprotoCmd(cmd, buf, offset, len);
		}	
	}
	
	
	/**
	 * This thread manages the connection to the slim server, and processes any
	 * incoming commands.
	 */
	private class TcpSocket extends Thread {
		private InetAddress addr;

		private int port;

		private Socket socket;

		private boolean connected = false;

		private boolean closeSocket = false;

		TcpSocket(InetAddress addr, int port) {
			super("SlimTCP-" + (threadCount++));
			this.addr = addr;
			this.port = port;
		}

		boolean isConnected() {
			return connected;
		}

		void close() throws IOException {
			closeSocket = true;
			if (socket != null)
				socket.close(); // Force read error
		}

		void write(byte buf[]) throws IOException {
			OutputStream stream = socket.getOutputStream();
			stream.write(buf);
		}

		// read until len bytes or end of stream
		int blockingRead(InputStream stream, byte[] b, int off, int len)  throws IOException {
			int total = 0; // bytes read so far
		    while (total < len) {
		        int ok = stream.read(b, off + total, len-total);
		        if (ok < 0)
					// end of stream reached
		            return ok;
				// otherwise ok is number of bytes read
	            total += ok;
	        }
	        return total;
		}

		public synchronized void run() {
			byte buf[] = new byte[4048];
			int ok, len;

			logger.debug("SlimTCP thread started");
			while (!closeSocket) {
				while (!connected) {
					logger.debug("connecting command socket " + addr + ":" + port);
					try {
						socket = new Socket(addr, port);
						socket.setTcpNoDelay(true);
						
						connected = true;
						helosent = false;
					} catch (IOException e) {
						logger.debug("cannot connect to " + addr + ":" + port);
					}

					if (!connected) {
						try {
							Thread.sleep(5000); // retry in 5 seconds
						} catch (InterruptedException e) {
						}
					}
				}

				socketConnected(this);
	
				while (true) {
					try {
						InputStream stream = socket.getInputStream();

						ok = blockingRead(stream, buf, 0, 2);
						if (ok < 0) {
							logger.debug("end of stream detected reading header");
							break; // end of stream
						}
						len = unpackN2(buf, 0);
						ok = blockingRead(stream, buf, 0, len);
						if (ok < 0 || ok != len) {
							logger.debug("end of stream detected reading frame");
							break; // end of stream
						}

						socketCommand(buf, 0, len);
					} catch (IOException e) {
						logger.debug("ioexception reading from slimproto", e);
						break; // end of stream
					} catch (Exception e) {
						logger.error("Exception processing frame ", e);
					}
				}
				logger.debug("command socket disconnected");

				connected = false;
				helosent = false;
				socketDisconnected(this);
				notifyAll();
			}

			try {
				logger.debug("closing command socket");
				socket.close();
			} catch (IOException e) {
				logger.error("Exception closing command socket", e);
			}
		}
	}

	/**
	 * Thread listening for Slim UDP commands.
	 */
	private class UdpSocket extends Thread {
		private DatagramSocket socket;

		UdpSocket() {
			super("SlimUDP-" + (threadCount++));

			try {
				/*
				 * FIXME: this should bind to 3483, but this won't work when
				 * softsqueeze is running on the same machine as the slim
				 * server.
				 */
				socket = new DatagramSocket(); //3483
				logger.debug("SlimUDP socket open");
			} catch (IOException e) {
				logger.error("Error with udp socket", e);
			}
		}

		void send(DatagramPacket p) throws IOException {
			socket.send(p);
		}

		public void run() {
			logger.debug("SlimUDP thread started");

			try {
				byte[] buf = new byte[1024];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);

				while (true) {
					socket.receive(packet);

					if (verbose) {
						StringBuffer str = new StringBuffer();
						str.append("udp recv: ");
						for (int i = 0; i < packet.getLength(); i++) {
							str.append(Integer.toString((buf[i] & 0xFF), 16));
							str.append(" ");
						}
						logger.debug(str.toString());
					}

					if (buf[0] == 'D') {
						/* discovery resposne */
						String servername = new String(buf, 1, packet
								.getLength()).trim();
						discoveredServers.put(servername, packet.getAddress()
								.getHostAddress());
					} else {
					    socketCommand(buf, 0, buf.length);
					}
				}
			} catch (IOException e) {
				logger.error("Error with udp socket", e);
			}
		}
	}

	public static int unpackN2(byte[] buf, int pos) {
		return ((buf[pos++] & 0xFF) << 8) | (buf[pos] & 0xFF);
	}

	public static int unpackN4(byte[] buf, int pos) {
		return ((buf[pos++] & 0xFF) << 24) | ((buf[pos++] & 0xFF) << 16)
				| ((buf[pos++] & 0xFF) << 8) | (buf[pos] & 0xFF);
	}

    public static float unpackFixedPoint(byte[] buf, int pos) {
        int v = unpackN4(buf, pos);        
        return ((v & 0xFFFF0000) >> 16) + ((v & 0xFFFF) / (float)0xFFFF); 
    }

	public static void packN2(byte[] buf, int pos, int arg) {
		buf[pos++] = (byte) ((arg >> 8) & 0xFF);
		buf[pos] = (byte) ((arg >> 0) & 0xFF);
	}

    public static void packN4(byte[] buf, int pos, int arg) {
		buf[pos++] = (byte) ((arg >> 24) & 0xFF);
		buf[pos++] = (byte) ((arg >> 16) & 0xFF);
		buf[pos++] = (byte) ((arg >> 8) & 0xFF);
		buf[pos] = (byte) ((arg >> 0) & 0xFF);
	}

	public static void packN8(byte[] buf, int pos, long arg) {
		buf[pos++] = (byte) ((arg >> 56) & 0xFF);
		buf[pos++] = (byte) ((arg >> 48) & 0xFF);
		buf[pos++] = (byte) ((arg >> 40) & 0xFF);
		buf[pos++] = (byte) ((arg >> 32) & 0xFF);
		buf[pos++] = (byte) ((arg >> 24) & 0xFF);
		buf[pos++] = (byte) ((arg >> 16) & 0xFF);
		buf[pos++] = (byte) ((arg >> 8) & 0xFF);
		buf[pos++] = (byte) ((arg >> 0) & 0xFF);
	}

	public long getEpoch() { return epoch;}
}
