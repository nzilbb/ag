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

import java.util.Vector;
import java.io.File;
import nzilbb.media.MediaConverter;
import nzilbb.media.MediaException;
import nzilbb.media.MediaThread;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import javax.sound.sampled.*;

/**
 * MediaConverter that extracts a given channel from audio.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class ChannelExtractor implements MediaConverter {

  /** Left channel number - 0 */
  public static final Integer LEFT = Integer.valueOf(0);
  /** Right channel number - 1 */
  public static final Integer RIGHT = Integer.valueOf(1);
  
  /**
   * Which channel to extract, e.g. 0 for left, 1 for right.
   * @see #getChannel()
   * @see #setChannel(int)
   */
  protected Integer channel = Integer.valueOf(0);
  /**
   * Getter for {@link #channel}: Which channel to extract, e.g. 0 for left, 1 for right.
   * @return Which channel to extract, e.g. 0 for left, 1 for right.
   */
  public Integer getChannel() { return channel; }
  /**
   * Setter for {@link #channel}: Which channel to extract, e.g. 0 for left, 1 for right.
   * @param newChannel Which channel to extract, e.g. 0 for left, 1 for right.
   */
  public ChannelExtractor setChannel(Integer newChannel) {
    channel = newChannel; return this; }
   
  /**
   * Default constructor.
   */
  public ChannelExtractor() {
  } // end of constructor
  
  // MediaConverter methods
  
  /**
   * Configure the converter. This converter requires "channel", the
   * channel number to extract.
   * @param configuration The configuration for the converter. 
   * @return A list of configuration parameters must be set before the
   * converter can be used. 
   * @throws MediaException If an error occurs.
   */
  public ParameterSet configure(ParameterSet configuration) throws MediaException {
    Parameter channel = configuration.get("channel");
    if (channel == null) {
      channel = new Parameter(
        "channel", Integer.class, "Channel", "Channel to extract, 0=left, 1=right", true);
      configuration.addParameter(channel);
    }
    if (getChannel() != null && channel.getValue() == null) {
      channel.setValue(Integer.valueOf(getChannel()));
    }
    configuration.apply(this);
    return configuration;
  }
   
  /**
   * Determines whether this converter supports conversion between the given types.
   * @param sourceType The MIME type of the source media.
   * @param destinationType The MIME type of the destination format.
   * @return true if the converter can convert from the sourceType to the destinationType,
   * false otherwise. 
   * @throws MediaException If an error occurs.
   */
  public boolean conversionSupported(String sourceType, String destinationType)
    throws MediaException {
    return sourceType.startsWith("audio/wav") && destinationType.startsWith("audio/wav");
  }

  /**
   * Starts conversion.
   * @param sourceType The MIME type of the source media.
   * @param source The source file.
   * @param destinationType The MIME type of the destination format.
   * @param destination The destination file.
   * @return A thread that is processing the media.
   * @throws MediaException If an error occurs.
   */
  public MediaThread start(
    String sourceType, File source, String destinationType, File destination)
    throws MediaException {
    
    final File finalDestination = destination;
    
    try {
      AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(source);
      audioInputStream = AudioSystem.getAudioInputStream(
        audioInputStream.getFormat(), audioInputStream);
      
      final AudioInputStream stream
        = new SingleChannelAudioInputStream(audioInputStream, getChannel());
      
      MediaThread thread = new MediaThread(new Runnable() {
          public void run() {
            try {
              ((MediaThread)Thread.currentThread()).setPercentComplete(1);
              
              // run the resampling
              AudioSystem.write(stream, AudioFileFormat.Type.WAVE, finalDestination);
              
              ((MediaThread)Thread.currentThread()).setPercentComplete(100);
            } catch (Throwable t) {
              ((MediaThread)Thread.currentThread()).setLastError(t);
              finalDestination.delete(); 
            }
          }
        });
      thread.start();
      return thread;
    } catch(Exception e) {
      throw new MediaException(e);
    } catch(Error er) {
      throw new MediaException(er);
    }    
  }
   
} // end of class ChannelExtractor
