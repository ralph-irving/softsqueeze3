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
import java.util.Random;

import junit.framework.TestCase;

/**
 * @author Richard Titmuss
 */
public class AudioBufferTest extends TestCase {
    private int chunkSize = 10240;
    
    private int bufSize = chunkSize * 4;
    
    private byte data[] = new byte[bufSize*10];
    
    private byte tmp[] = new byte[bufSize];

    private Random r = new Random();
    
    
    
    public void testSimpleRead() {
        AudioBuffer audioBuffer = new AudioBuffer(bufSize);
        
        int len = r.nextInt(2048);
        
        try {
            int bw = audioBuffer.write(data, 0, len);
            int br = audioBuffer.read(tmp, 0, tmp.length);
            
            assertEquals(bw, br);
        }
        catch (IOException e) {
            e.printStackTrace();
        }        
    }
    
    public void testClosedRead() {
        AudioBuffer audioBuffer = new AudioBuffer(bufSize);

        try {
            audioBuffer.close();
            int br = audioBuffer.read(tmp, 0, tmp.length);
            assertTrue(br < 0);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void testClosedWriteBytes() {
        AudioBuffer audioBuffer = new AudioBuffer(bufSize);

        try {
            audioBuffer.close();
            int bw = audioBuffer.write(data, 0, 1);
            assertTrue(bw < 0);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testReadWriteBytes1() {
        AudioBuffer audioBuffer = new AudioBuffer(bufSize);
        
        try {
            int nr = 0;
            int br = 0;
            while (br < chunkSize*3.5) {
                br += audioBuffer.write(data, br, br+(chunkSize/3));
                nr++;
            }
            audioBuffer.close();
            
            int nw = 0;
            int bw = 0;
            while (true) {
                int n = audioBuffer.read(tmp, 0, chunkSize/5);
                if (n < 0)
                    break;
                nw++;
                
                boolean ok = true;
                for (int i=0; i<n; i++) {
                    ok = ok || (data[bw+i] == tmp[i]);
                }                
                assertTrue(ok);                
                bw += n;
            }
        
            assertEquals(br, bw);
            System.err.println("testReadWrite1 br="+br+" nr="+nr+" nw="+nw);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void testReadWriteBytes2() {
        AudioBuffer audioBuffer = new AudioBuffer(bufSize);
        
        try {
            int nr = 0;
            int br = 0;
            while (br < chunkSize*3.5) {
                br += audioBuffer.write(data, br, br+(int)(chunkSize*1.3));
                nr++;
            }
            audioBuffer.close();
            
            int nw = 0;
            int bw = 0;
            while (true) {
                int n = audioBuffer.read(tmp, 0, (int)(chunkSize*1.5));
                if (n < 0)
                    break;
                nw++;
                
                boolean ok = true;
                for (int i=0; i<n; i++) {
                    ok = ok || (data[bw+i] == tmp[i]);
                }                
                assertTrue(ok);                
                bw += n;
            }
        
            assertEquals(br, bw);
            System.err.println("testReadWrite2 br="+br+" nr="+nr+" nw="+nw);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    public void testThreadedReadWriteBytes() {
        AudioBuffer audioBuffer = new AudioBuffer(bufSize);
        
        new Reader(audioBuffer);
        
        try {
            int nr = 0;
            int br = 0;
            while (br < bufSize*9) {
                int n = r.nextInt((int)(chunkSize*1.9));
                br += audioBuffer.write(data, br, br+n);
                nr++;
            }
            audioBuffer.close();

            System.err.println("testThreadedReadWriteBytes br="+br+" nr="+nr);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    class Reader extends Thread {
        AudioBuffer audioBuffer;
        
        Reader(AudioBuffer audioBuffer) {
            this.audioBuffer = audioBuffer;
            
            start();
        }
        
        public void run() {
            try {
                int nw = 0;
                int bw = 0;
                while (true) {
                    int n = r.nextInt((int)(chunkSize*1.9));
                    n = audioBuffer.read(tmp, 0, n);
                    if (n < 0)
                        break;
                    nw++;
                    
                    boolean ok = true;
                    for (int i=0; i<n; i++) {
                        ok = ok || (data[bw+i] == tmp[i]);
                    }                
                    assertTrue(ok);                
                    bw += n;
                }            

                System.err.println("Reader bw="+bw+" nw="+nw);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
        
    public static void main(String[] args) {
        junit.textui.TestRunner.run(AudioBufferTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();        
        r.nextBytes(data);
    }

}
