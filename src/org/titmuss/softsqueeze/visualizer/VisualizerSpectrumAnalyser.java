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

package org.titmuss.softsqueeze.visualizer;

import jnt.FFT.ComplexFloatFFT_Radix2;

import org.titmuss.softsqueeze.Softsqueeze;
import org.titmuss.softsqueeze.display.LcdDisplay;


/**
 * @author Richard Titmuss
 *
 */
public class VisualizerSpectrumAnalyser extends Visualizer {

    private static final int POWER_MAP[] = new int[] {
            0, 362, 2048, 5643, 11585, 20238, 31925, 46935, 65536, 87975, 114486, 
            145290, 180595, 220603, 265506, 315488, 370727, 431397, 497664, 
            569690, 647634, 731649, 821886, 918490, 1021605, 1131370, 1247924, 
            1371400, 1501931, 1639645, 1784670, 1937131
          }; 

	private final static double TWOPI = 6.283185307179586476925286766;

    private final static int CHUNK_SIZE = 512;

    private static final int COLUMNS_PER_FRAME = LcdDisplay.FRAME_WIDTH;

    private static final int MIN_FFT_INPUT_SAMPLES = 128;

    private static final int MIN_SUBBANDS = 32;
    
    private static final int MAX_SAMPLE_WINDOW = 1024;


    private double filterWindow[];

    private double preemphasis[];

    private int channelWidth[] = new int[2];
    
    private int subbandsInBar[] = new int[2];

    private int numBars[] = new int[2];

    private boolean clipSubbands[] = new boolean[2];

    private int numSubbands;

    private int sampleWindow;
    
    private int numWindows = 0;
    
    private boolean bandwidthHalf = false;
    
    private double preemphasisDbPerKhz;
    
    private int samplesInChunk = 0;
    
    private int numReads = 0;

    private ComplexFloatFFT_Radix2 fft;
    

    public VisualizerSpectrumAnalyser(Softsqueeze squeeze, byte[] buf, int off, int len) {
        super(squeeze, buf, off, len);
        
        isMono = (visParam[0] == 1);        
        bandwidthHalf = (visParam[1] == 1);
        preemphasisDbPerKhz = visParam[2] >> 16;
        
        channelPosition[0] = visParam[3];
        channelWidth[0] = visParam[4];
        channelFlipped[0] = (visParam[5] > 0);
        barWidth[0] = visParam[6];
        barSpacing[0] = visParam[7];
        clipSubbands[0] = (visParam[8] > 0);
        barIntensity[0] = visParam[9];
        capIntensity[0] = visParam[10];
        
        if (!isMono) {
            channelPosition[1] = visParam[11];
            channelWidth[1] = visParam[12];
            channelFlipped[1] = (visParam[13] > 0);
            barWidth[1] = visParam[14];
            barSpacing[1] = visParam[15];
            clipSubbands[1] = (visParam[16] > 0);
            barIntensity[1] = visParam[17];
            capIntensity[1] = visParam[18];
        }
        else {
            channelPosition[1] = -1;
        }
        
        
    	for (int ch = 0; ch < 2; ch++) {
    		if (channelWidth[ch] > COLUMNS_PER_FRAME)
    		    channelWidth[ch] = COLUMNS_PER_FRAME;
    	
    		if ((channelPosition[ch] + channelWidth[ch]) > COLUMNS_PER_FRAME)
    			channelPosition[ch] = COLUMNS_PER_FRAME - channelWidth[ch];
    	}

    	// Approximate the number of subbands we'll display based
    	// on the width available and the size of the histogram
    	// bars.
    	int barSize = barWidth[0] + barSpacing[0];
    	numSubbands = channelWidth[0] / barSize;

    	// Calculate the integer component of the log2 of the num_subbands
    	int l2int = 0;
    	int shiftsubbands = numSubbands;
    	while (shiftsubbands != 1) {
    		l2int++;
    		shiftsubbands >>= 1;
    	}

    	// The actual number of subbands is the largest power
    	// of 2 smaller than the specified width.
    	numSubbands = 1 << l2int;

    	// In the case where we're going to clip the higher
    	// frequency bands, we choose the next highest
    	// power of 2. 
    	if (clipSubbands[0]) {
    		numSubbands <<= 1;
    	}

    	// The number of histogram bars we'll display is nominally
    	// the number of subbands we'll compute.
    	numBars[0] = numSubbands;

    	// Though we may have to compute more subbands to meet
    	// a minimum and average them into the histogram bars.
    	if (numSubbands < MIN_SUBBANDS) {
    		subbandsInBar[0] = MIN_SUBBANDS / numSubbands;
    		numSubbands = MIN_SUBBANDS;
    	}
    	else {
    		subbandsInBar[0] = 1;
    	}

    	// If we're clipping off the higher subbands we cut down
    	// the actual number of bars based on the width available.
    	if (clipSubbands[0]) {
    		numBars[0] = channelWidth[0] / barSize;
    	}

        barVal[0] = new int[ numBars[0] ];
        barLvl[0] = new int[ numBars[0] ];

    	// Since we now have a fixed number of subbands, we choose
    	// values for the second channel based on these.
    	if (!isMono) {
    		barSize = barWidth[1] + barSpacing[1];
    		numBars[1] = channelWidth[1] / barSize;
    		subbandsInBar[1] = 1;
    		// If we have enough space for all the subbands, great.
    		if (numBars[1] > numSubbands) {
    			numBars[1] = numSubbands;  
    		}

    		// If not, we find the largest factor of the
    		// number of subbands that we can show.
    		else if (!clipSubbands[1]) {
    			int s = numSubbands;
    			subbandsInBar[1] = 1;
    			while (s > numBars[1]) {
    				s >>= 1;
    				subbandsInBar[1]++;
    			}
    			numBars[1] = s;
    		}

            barVal[1] = new int[ numBars[1] ];
            barLvl[1] = new int[ numBars[1] ];
    	}

    	// Calculate the number of samples we'll need to send in as
    	// input to the FFT. If we're halving the bandwidth (by
    	// averaging adjacent samples), we're going to need twice
    	// as many.
    	sampleWindow = numSubbands * 2;
    	fft = new ComplexFloatFFT_Radix2(sampleWindow);
    	
    	if (sampleWindow < MIN_FFT_INPUT_SAMPLES) {
    		numWindows = MIN_FFT_INPUT_SAMPLES / sampleWindow;
    	}
    	else {
    		numWindows = 1;
    	}

    	samplesInChunk = CHUNK_SIZE / (2 * 2);
    	if (bandwidthHalf) {
    		samplesInChunk >>= 1;
    	}
    	
    	if (sampleWindow <= samplesInChunk) {
    		numReads = 1;
    	}
    	else {
    		numReads = sampleWindow / samplesInChunk;
    	}
    	
    	logger.debug("numSubbands = " + numSubbands);
    	logger.debug("sampleWindow = " + sampleWindow);
    	logger.debug("samplesInChunk = " + samplesInChunk);
    	logger.debug("numReads = " + numReads);
    	logger.debug("numWindows = " + numWindows);
    	logger.debug("bandwidthHalf = " + bandwidthHalf);
    	logger.debug("subbandsInBar = " + subbandsInBar[0]);
    	
		// Compute the Hamming window. This could be precomputed.
		double const1 = 0.54;
		double const2 = 0.46;
		filterWindow = new double[MAX_SAMPLE_WINDOW];
		for (int w = 0; w < sampleWindow; w++) {
			filterWindow[w] = const1 - (const2 * Math.cos( TWOPI * (double)w / (double)sampleWindow ));
		}

		// Compute the preemphasis
		double subbandWidth = 22.05 / numSubbands;
		double freqSum = 0;
		double scaleDB = 0;
		preemphasis = new double[numSubbands];
		for (int s = 0; s < numSubbands; s++) {
			while (freqSum > 1) {
				freqSum -= 1;
				scaleDB += preemphasisDbPerKhz;
			}
			if (scaleDB != 0) {
			    preemphasis[s] = Math.pow(10, (scaleDB / 10.0));
			}
			else {
			    preemphasis[s] = 1;
			}
			freqSum += subbandWidth;
		}

    }
    
    protected void visualize(byte[] buf, int offset, int bufLen) {
        float avgPower[] = new float[numSubbands * 2];

		// To reduce noise, we do multiple windows of samples
		for (int w = 0; w < numWindows; w++) {
	        float fftData[] = new float[sampleWindow * 2];

			// Read samples in chunks
	        int fftPtr = 0;
	        for (int r = 0; r < numReads; r++) {
	            int numRead = Math.min(bufLen - offset, CHUNK_SIZE);
	            if (numRead < CHUNK_SIZE)
	                break;
	            
	            // Pack the channels so that they represent
	            // the real and imaginary parts of the input
	            // sequence.        
	            for (int s = 0; s < samplesInChunk; s++) {
	                if (bandwidthHalf) {
	                    int sample = getSample(offset);
	                    sample += getSample(offset + 4);
	                    sample >>= 1;
	                    fftData[fftPtr++] = (float)(filterWindow[s] * sample);
	                    
	                    sample = getSample(offset + 2);
	                    sample += getSample(offset + 6);
	                    sample >>= 1;
	                    fftData[fftPtr++] = (float)(filterWindow[s] * sample);
	                    
	                    offset += 8;
	                }
	                else {
	                    int sample = getSample(offset);
	                    fftData[fftPtr++] = (float)(filterWindow[s] * sample);
	                    
	                    sample = getSample(offset + 2);
	                    fftData[fftPtr++] = (float)(filterWindow[s] * sample);
	                    
	                    offset += 4;
	                }		        
	            }
	        }
		    
	        /* This can be used to test the fft ... 
	        double freq = (Math.PI*16) / 256;	        
	        float ampl = ((int)Math.pow(2, 16))/2;	        
	        for (int i = 0; i < sampleWindow; i++) {
	            fftData[(i*2)+0] = (ampl * (float) Math.sin(i*freq)) + (ampl * (float) Math.cos(i*freq));
	            fftData[(i*2)+1] = (ampl * (float) Math.sin(i*freq)) + (ampl * (float) Math.cos(i*freq));
	        } 
			*/
	        
			// Perform the complex to complex FFT. The result
			// is N complex values in the frequency domain that
			// need to be separated into two N/2 signals for
			// each of the channels.
	        fft.transform(fftData);

			// Extract the two separate frequency domain signals
			// and keep track of the power per bin.
			int avgPtr = 0;
			for (int s = 1; s <= numSubbands; s++) {
			    int ck = s * 2;
			    int cnk = (sampleWindow * 2) - ck;
			    
			    float r = (fftData[ck] + fftData[cnk]) / 2;
			    float i = (fftData[ck+1] - fftData[cnk+1]) / 2;			    
			    avgPower[avgPtr++] += (float) ((r * r + i * i) / numWindows);
			    
			    r = (fftData[cnk+1] + fftData[ck+1]) / 2;
			    i = (fftData[cnk] - fftData[ck]) / 2;
			    avgPower[avgPtr++] += (float) ((r * r + i * i) / numWindows);
			}
		}

    	int prePtr = 0;
    	int avgPtr = 0;     	
    	for (int p = 0; p < numSubbands; p++) {
    	    long product = (long) (avgPower[avgPtr] * preemphasis[prePtr]);
    	    product >>= 16;
    	    avgPower[avgPtr++] = (int) product;
    	    
    	    product = (long) (avgPower[avgPtr] * preemphasis[prePtr]);
    	    product >>= 16;
    	    avgPower[avgPtr++] = (int) product;
    	    
    	    prePtr ++;
    	}
    	
    	for (int ch = 0; ch < ((isMono)?1:2); ch++) {
    	    int powerSum = 0;
    	    int inBar = 0;
    	    int currBar = 0;
    	    
    	    avgPtr = (ch == 0) ? 0 : 1;
    	    
    	    for (int s = 0; s < numSubbands; s++) {
    	        // Average out the power for all subbands represented
    	        // by a bar.
    	        powerSum += avgPower[avgPtr] / subbandsInBar[ch]; 
    	        if (isMono)
    	            powerSum += avgPower[avgPtr+1] / subbandsInBar[ch];
    	        
    	        if (++inBar == subbandsInBar[ch]) {    	        
    	            if (isMono)
    	                powerSum >>= 2;

    	            powerSum <<= 3; // FIXME scaling
    	            
                    int val = 0;
    	            for (int i = 31; i > 0; i--) {
    	                if (powerSum >= POWER_MAP[i]) {
    	                    val = i;
    	                    break;
    	                }
    	            }
                    barVal[ch][currBar++] = val;
    	            
    	            if (currBar == numBars[ch])
    	                break;
    	            
    	            inBar = 0;
    	            powerSum = 0;
    	        }
    	        avgPtr += 2;
    	    }
    	}
    }
    
}
