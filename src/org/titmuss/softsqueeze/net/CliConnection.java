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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;



/**
 * @author Richard Titmuss
 *
 */
public class CliConnection {
    private final static Logger logger = Logger.getLogger("cli");
    
    private final static int DISCONNECTED = 0;

    private final static int CONNECTED = 1;

    private Socket socket;
    
    private BufferedReader socketIn;

    private PrintWriter socketOut;

	private LinkedList sendQueue = new LinkedList();
	
	private LinkedList readQueue = new LinkedList();
	
	private HashMap listeners = new HashMap();
	
	private HashMap listenFilters = new HashMap();
	
	private Object lock = new Object();
	
	private InetAddress serverAddress;
	
	private int serverPort;
	
	private int toState = DISCONNECTED;
	
	private int inState = DISCONNECTED;

	private CliSend cliSend;
	
	private CliRead cliRead;
    
    public CliConnection() {        
        cliSend = new CliSend();
        cliSend.start();
        
        cliRead = new CliRead();
        cliRead.start();
    }
    
    /**
     * Connect the cli to the Squeezebox Server at the given address and port.
     * 
     * @param addr
     * @param port
     */
    public void connect(InetAddress addr, int port) throws IOException {
        synchronized (lock) {
            disconnect();

            while (inState == CONNECTED) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                }
            }
            
            serverAddress = addr;
            serverPort = port;
            
            toState = CONNECTED;
            lock.notifyAll();
        }
	}
	
	/**
	 * Disconnect the cli from the Squeezebox Server.
	 */
	public void disconnect() throws IOException {
	    synchronized (lock) {
	        if (!isConnected())
	            return;

	        toState = DISCONNECTED;
	        lock.notifyAll();
	    }
	}
	
	/**
	 * @return true if the cli is connected to the Squeezebox Server
	 */
	public boolean isConnected() {
	    return inState == CONNECTED;
	}

	/**
	 * Queue a cli message to be sent to the Squeezebox Server, the listener is called with
	 * the response.
	 * 
	 * @param callback
	 * @param command
	 */
	public void queueMessage(CliListener callback, CliMessage command) {
	    synchronized (lock) {
	        sendQueue.add(new MessageEntry(command, callback));
	        lock.notifyAll();
	    }
	}
	
	/**
	 * Add a listener for cli messages that match filter.
	 * @param filter
	 * @param callback
	 */
	public void addFilter(CliMessage filter, CliListener callback) {
	    logger.debug("addFilter " + filter.toString());

	    synchronized (lock) {
	        if (listenFilters.size() == 0)
	            queueMessage(null, new CliMessage("listen").addParameter("1"));
	        
	        Integer count = (Integer) listeners.get(callback);
	        if (count == null)
	            listeners.put(callback, new Integer(1));
	        else
	            listeners.put(callback, new Integer(count.intValue() + 1));
	        
	        ArrayList l = (ArrayList) listenFilters.get(filter);
	        if (l == null) {
	            l = new ArrayList();
	            listenFilters.put(filter, l);
	        }
	        l.add(callback);
	    }
	}

	/**
	 * Remove the listener for cli messages the match filter.
	 * @param filter
	 * @param callback
	 */
	public void removeFilter(CliMessage filter, CliListener callback) {
	    logger.debug("removeFilter " + filter.toString());

	    synchronized (lock) {
	        ArrayList l = (ArrayList) listenFilters.get(filter);
	        if (l == null)
	            return;
	        
	        l.remove(callback);
	        if (l.size() == 0)
	            listenFilters.remove(filter);
	        
	        Integer count = (Integer) listeners.get(callback);
	        if (count == null || count.intValue() == 1) {
	            listeners.remove(callback);
	        }
	        else {
	            listeners.put(callback, new Integer(count.intValue() - 1));
	        }
	        
	        if (listenFilters.size() == 0)
	            queueMessage(null, new CliMessage("listen").addParameter("0"));
	    }
	}
	
	
	private class CliSend extends Thread {
	    private CliSend() {
	        super("CliSend");
	        setDaemon(true);
	    }
	    
	    public void run() {
	        while (true) {
                synchronized (lock) {
                    while (inState != CONNECTED) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e1) {
                        }
                    }
                }

	            try {
	                MessageEntry entry;
	                synchronized (lock) {
	                    while (sendQueue.isEmpty()) {
	                        try {
	                            lock.wait();
	                        } catch (InterruptedException e){
	                        }
	                    }
	                    
	                    entry = (MessageEntry) sendQueue.removeFirst();

		                if (!isConnected())
		                    continue;

	                    readQueue.add(entry);
	                }
	                	                
	                if (logger.isDebugEnabled())
	                    logger.debug("send cli msg: " + entry.getMessage());
	                socketOut.print(entry.getMessage()+"\n");
	                socketOut.flush();

	                /*
	                 * The Squeezebox Server does not seem to respond well when multiple commands are queued.
	                 * Sometimes the replies go missing, so for now just send one command at a time.
	                 */
	                synchronized (lock) {
	                    while (! readQueue.isEmpty()) {
	                        try {
	                            lock.wait();
	                        } catch (InterruptedException e) {
	                        }
	                    }
	                }
	            }
	            catch (Exception e) {
	                logger.error("Exception in CliSend", e);
	            }
	        }
	    }
	}
	
	private class CliRead extends Thread {
	    private CliRead() {
	        super("CliRead");
	        setDaemon(true);
	    }
	    
	    public void run() {
            while (true) {
                synchronized (lock) {
                    while (toState != CONNECTED) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e1) {
                        }
                    }
                }

                synchronized (lock) {
                    logger.debug("CLI connecting ...");

                    boolean connected = false;
                    while (!connected) {
                        try {
                            socket = new Socket(serverAddress, serverPort);
                            socketIn = new BufferedReader(new InputStreamReader(
                                    socket.getInputStream()));
                            socketOut = new PrintWriter(socket.getOutputStream());
                            connected = true;
                            
                        } catch (IOException e) {
                            logger.debug("Cannot connect CLI socket", e);
                        }
                        
                        if (!connected) {
                            try {
                                Thread.sleep(5000); // retry in 5 seconds
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                    
                    logger.debug("CLI connected");

                    inState = CONNECTED;
                    lock.notifyAll();

                    for (Iterator i = listeners.keySet().iterator(); i
                            .hasNext();) {
                        CliListener l = (CliListener) i.next();
                        l.cliConnected();
                    }
                }

                while (toState == CONNECTED) {
                    try {
                        String line = socketIn.readLine();
                        if (line == null)
                            break; // socket is closed
                        
                        logger.debug("got reply " + line);
                        CliMessage msg = CliMessage.parseMessage(line);

                        MessageEntry entry = null;
                        synchronized (lock) {
                            if (!readQueue.isEmpty()) {
                                entry = (MessageEntry) readQueue.getFirst();

                                if (entry.getMessage().equals(msg)) {
                                    readQueue.removeFirst();
                                    lock.notifyAll();
                                } else {
                                    entry = null;
                                }
                            }
                        }

                        if (entry != null && entry.getCallback() != null)
                            entry.getCallback().cliMessage(msg);

                        for (Iterator i = listenFilters.entrySet().iterator(); i
                                .hasNext();) {
                            Map.Entry e = (Map.Entry) i.next();
                            CliMessage filter = (CliMessage) e.getKey();

                            if (!msg.equals(filter))
                                continue;

                            ArrayList l = (ArrayList) e.getValue();
                            for (Iterator j = l.iterator(); j.hasNext();) {
                                CliListener listener = (CliListener) j.next();
                                listener.cliMessage(msg);
                            }
                        }
                    } catch (IOException e) {
                        break; // socket closed
                    } catch (Exception e) {
                        logger.error("Exception in CliRead", e);
                    }
                }

                synchronized (lock) {
                    try {
                        socketOut.close();
                        socketIn.close();
                        socket.close();
                    } catch (IOException e) {
                    }

                    logger.debug("CLI disconnected");

                    readQueue.clear();
                    listenFilters.clear();
                    
                    inState = DISCONNECTED;
                    lock.notifyAll();

                    for (Iterator i = listeners.keySet().iterator(); i
                            .hasNext();) {
                        CliListener l = (CliListener) i.next();
                        l.cliDisconnected();
                    }
                }

            }
        }
    }
	
	
	private class MessageEntry {
	    CliListener callback;
	    CliMessage message;
	    
	    MessageEntry(CliMessage command, CliListener callback) {
	        this.message = command;
	        this.callback = callback;
	    }
	    
	    CliListener getCallback() {
	        return callback;
	    }
	    
	    CliMessage getMessage() {
	        return message;
	    }
	}	
}
