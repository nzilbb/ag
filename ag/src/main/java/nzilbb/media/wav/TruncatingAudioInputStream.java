//
// Copyright 2015-2019 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of nzilbb.ag.
//
//    nzilbb.ag is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 3 of the License, or
//    (at your option) any later version.
//
//    nzilbb.ag is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with nzilbb.ag; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package nzilbb.media.wav;

import java.lang.*;
import java.lang.reflect.*;
import java.util.*;
import java.net.*;
import java.io.*;
import javax.sound.sampled.*;

/**
 * Input stream that truncates another input stream - i.e. read operations only return data
 * between the given start time and end time.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class TruncatingAudioInputStream extends AudioInputStream {
  
  // Attributes:
  
  /**
   * The input stream that this is to truncate
   */
  private AudioInputStream isInputStream;
  /**
   * InputStream accessor 
   * @return The input stream that this is to truncate
   */
  public AudioInputStream getInputStream() { return isInputStream; }
  /**
   * InputStream mutator
   * @param isNewInputStream The input stream that this is to truncate
   */
  public TruncatingAudioInputStream setInputStream(AudioInputStream isNewInputStream) { isInputStream = isNewInputStream; return this; }
  
  /**
   * Start time in bytes
   */
  protected long lStartByte = 0;
  /**
   * Start time in seconds
   */
  private double dStartSeconds = 0;
  /**
   * StartSeconds accessor 
   * @return Start time in seconds
   */
  public double getStartSeconds() { return dStartSeconds; }
  /**
   * StartSeconds mutator
   * @param dNewStartSeconds Start time in seconds
   */
  public TruncatingAudioInputStream setStartSeconds(double dNewStartSeconds) 
    throws IOException { 
    dStartSeconds = dNewStartSeconds; 
    
    // calculate start byte
    AudioFormat format = isInputStream.getFormat();
    int iBytesPerSecond = (int)(format.getFrameRate() * (float)format.getFrameSize());
    lStartByte = (long)(iBytesPerSecond * dStartSeconds);
    
    isInputStream.skip(lStartByte);
    
    return this;
  }
  
  /**
   * End time in bytes
   */
  protected long lEndByte = 0;
  /**
   * End time in seconds Can be 0 to indicate the end of the stream.
   */
  private double dEndSeconds = -1;
  /**
   * EndSeconds accessor 
   * @return End time in seconds Can be 0 to indicate the end of the stream.
   */
  public double getEndSeconds() { return dEndSeconds; }
  /**
   * EndSeconds mutator
   * @param dNewEndSeconds End time in seconds. Can be 0 to indicate the end of the stream.
   */
  public TruncatingAudioInputStream setEndSeconds(double dNewEndSeconds) { 
    dEndSeconds = dNewEndSeconds; 
    
    // calculate end byte
    AudioFormat format = isInputStream.getFormat();
    int iBytesPerSecond = (int)(format.getFrameRate() * (float)format.getFrameSize());
    lEndByte = (long)(iBytesPerSecond * dEndSeconds);
    
    lLengthBytes = lEndByte - lStartByte;
    
    return this;
  }
  
  /**
   * Length in bytes - i.e. how far to go
   */
  protected long lLengthBytes = 0;
  
  /**
   * Constructor
   */
  public TruncatingAudioInputStream(AudioInputStream audioInputStream, double dStart, double dEnd)
    throws IOException {
    super(new ByteArrayInputStream(
            new byte[0]), audioInputStream.getFormat(), AudioSystem.NOT_SPECIFIED);
    
    setInputStream(audioInputStream);
    setStartSeconds(dStart);
    setEndSeconds(dEnd);
  } // end of constructor
  
  public int read() throws IOException {
    // have we already finished?
    if (lLengthBytes <= 0 && getEndSeconds() > 0) return -1;
    int iByteRead = getInputStream().read();
    
    // decrement how many bytes to go
    if (iByteRead > 0) lLengthBytes--;
    
    return iByteRead;
  }

  public int read(byte[] abData, int nOffset, int nLength) throws IOException {
    // have we already finished?
    if (lLengthBytes <= 0 && getEndSeconds() > 0) return -1;
    
    // are we almost finished?
    if (nLength > lLengthBytes && getEndSeconds() > 0) nLength = (int)lLengthBytes;
    
    // read from the stream
    int iBytesRead = getInputStream().read(abData, nOffset, nLength);
    
    // decrement how many bytes to go
    if (iBytesRead > 0) lLengthBytes -= nLength;
    
    return iBytesRead;
  }
  
  // Passthrough AudioInputStream methods
  
  /**
   * Obtains the length of the audio data contained in the file, expressed in sample frames.
   * @return the number of sample frames of audio data in the file
   */
  public long getFrameLength() {
    // calculate size in frames, based on the length in bytes
    // and the frame size
    if (getEndSeconds() > 0) {
      return (lEndByte - lStartByte) / getFormat().getFrameSize();
    } else {
      return getInputStream().getFrameLength() - (lStartByte / getFormat().getFrameSize());
    }
  }
  
  public long skip(long lLength) throws IOException {
    return getInputStream().skip(lLength);
  }
  
  public int available() throws IOException {
    return getInputStream().available();
  }
  
  public void close() throws IOException {
    getInputStream().close();
  }
  
  public void mark(int nReadLimit) {
    getInputStream().mark(nReadLimit);
  }
  
  public void reset() throws IOException {
    getInputStream().reset();
  }
  
  public boolean markSupported() {
    return getInputStream().markSupported();
  }

} // end of class TruncatingAudioInputStream
