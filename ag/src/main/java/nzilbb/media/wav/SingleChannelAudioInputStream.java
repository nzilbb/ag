//
// Copyright 2025 New Zealand Institute of Language, Brain and Behaviour, 
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
 * Input stream that extracts a single selected channel from another input stream
 * - i.e. read operations only return data for one stream (e.g. left channel only
 * of a stereo recording).
 * @author Robert Fromont robert@fromont.net.nz
 */
public class SingleChannelAudioInputStream extends AudioInputStream {

  private int numSourceChannels = 0;
  private int sourceBytesToDestBytes = 0;
  
  /**
   * The input stream that this is to truncate
   */
  private AudioInputStream inputStream;
  /**
   * InputStream accessor 
   * @return The input stream that this is to truncate
   */
  public AudioInputStream getInputStream() { return inputStream; }
  /**
   * InputStream mutator
   * @param newInputStream The input stream that this is to truncate
   */
  public SingleChannelAudioInputStream setInputStream(AudioInputStream newInputStream) {
    inputStream = newInputStream;
    return this;
  }
  
  /**
   * Which channel to pass through, e.g. 0 for left, 1 for right.
   * @see #getChannel()
   * @see #setChannel(int)
   */
  protected int channel = 0;
  /**
   * Getter for {@link #channel}: Which channel to pass through,
   * e.g. 0 for left, 1 for right. 
   * @return Which channel to pass through, e.g. 0 for left, 1 for right.
   */
  public int getChannel() { return channel; }
  /**
   * Setter for {@link #channel}: Which channel to pass through,
   * e.g. 0 for left, 1 for right. 
   * @param newChannel Which channel to pass through, e.g. 0 for left, 1 for right.
   */
  public SingleChannelAudioInputStream setChannel(int newChannel) {
    channel = newChannel; return this; }
  
  /**
   * Constructor.
   * @audioInputStream The input stream to filter.
   * @param channel Which channel to pass through, e.g. 0 for left, 1 for right.
   */
  public SingleChannelAudioInputStream(AudioInputStream audioInputStream, int channel)
    throws IOException {
    super(new ByteArrayInputStream(new byte[0]),
          new AudioFormat(
            audioInputStream.getFormat().getEncoding(),
            audioInputStream.getFormat().getSampleRate(),
            audioInputStream.getFormat().getSampleSizeInBits(),
            1, // one channel
            audioInputStream.getFormat().getFrameSize() // size for one channel
            / audioInputStream.getFormat().getChannels(),
            audioInputStream.getFormat().getFrameRate(),
            audioInputStream.getFormat().isBigEndian()),
          AudioSystem.NOT_SPECIFIED);    
    setInputStream(audioInputStream);
    setChannel(Math.min(channel, audioInputStream.getFormat().getChannels()-1));
    numSourceChannels = audioInputStream.getFormat().getChannels();
  } // end of constructor
  
  public int read() throws IOException {
    return getInputStream().read(); // TODO can't read one byte!
  }

  public int read(byte[] data, int offset, int length) throws IOException {

    // read data for all channels from the source stream
    byte[] sourceData = new byte[length * numSourceChannels];
    
    // read all channels from the stream
    int iBytesRead = inputStream.read(sourceData, 0, sourceData.length);

    // copy only the selected channel into the buffer
    // for each frame in the output data
    for (int frameStartByte = 0; frameStartByte < length; frameStartByte += frameSize) {
      // copy the targeted source channel's frame
      for (int b = 0; b < frameSize; b++) {
        data[offset + frameStartByte + b]
          = sourceData[frameStartByte*numSourceChannels + channel*frameSize + b];
      } // next byte in the channel's frame
    } // next frame
    
    return iBytesRead / numSourceChannels;
  }
  
  // Passthrough AudioInputStream methods
  
  /**
   * Obtains the length of the audio data contained in the file, expressed in sample frames.
   * @return the number of sample frames of audio data in the file
   */
  public long getFrameLength() {
    return getInputStream().getFrameLength();
  }

  /**
   * Skips over and discards a specified number of bytes from this
   * audio input stream.
   */
  public long skip(long lLength) throws IOException {
    return getInputStream().skip(lLength*numSourceChannels);
  }

  /**
   * Returns the maximum number of bytes that can be read (or skipped
   * over) from this audio input stream without blocking.
   */
  public int available() throws IOException {
    return getInputStream().available();
  }

  /**
   * Closes this audio input stream and releases any system resources
   * associated with the stream.
   */
  public void close() throws IOException {
    getInputStream().close();
  }

  /**
   * Marks the current position in this audio input stream.
   */
  public void mark(int nReadLimit) {
    getInputStream().mark(nReadLimit);
  }

  /**
   * Repositions this audio input stream to the position it had at the
   * time its mark method was last invoked.
   */
  public void reset() throws IOException {
    getInputStream().reset();
  }

  /**
   * Tests whether this audio input stream supports the mark and reset
   * methods.
   */
  public boolean markSupported() {
    return getInputStream().markSupported();
  }

} // end of class SingleChannelAudioInputStream
