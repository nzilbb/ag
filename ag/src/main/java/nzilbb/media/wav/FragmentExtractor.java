//
// Copyright 2019-2020 New Zealand Institute of Language, Brain and Behaviour, 
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
 * MediaConverter that extracts portions of sound samples using the javax.sound.sampled API, with 
 * optional resampling.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class FragmentExtractor implements MediaConverter {
  // Attributes:   
  
  /**
   * Sample rate.
   * @see #getSampleRate()
   * @see #setSampleRate(Integer)
   */
  protected Integer sampleRate = null;
  /**
   * Getter for {@link #sampleRate}: Sample rate.
   * @return Sample rate.
   */
  public Integer getSampleRate() { return sampleRate; }
  /**
   * Setter for {@link #sampleRate}: Sample rate.
   * @param newSampleRate Sample rate.
   * @return this
   */
  public FragmentExtractor setSampleRate(Integer newSampleRate) { sampleRate = newSampleRate; return this; }
  
  /**
   * Start time of fragment.
   * @see #getStart()
   * @see #setStart(Double)
   */
  protected Double start;
  /**
   * Getter for {@link #start}.
   * @return Start time of fragment.
   */
  public Double getStart() { return start; }
  /**
   * Setter for {@link #start}.
   * @param start Start time of fragment.
   * @return <var>this</var>.
   */
  public FragmentExtractor setStart(Double start) { this.start = start; return this; }

  /**
   * End time of fragment.
   * @see #getEnd()
   * @see #setEnd(Double)
   */
  protected Double end;
  /**
   * Getter for {@link #end}.
   * @return End time of fragment.
   */
  public Double getEnd() { return end; }
  /**
   * Setter for {@link #end}.
   * @param end End time of fragment.
   * @return <var>this</var>.
   */
  public FragmentExtractor setEnd(Double end) { this.end = end; return this; }

  // Methods:
   
  /**
   * Default constructor.
   */
  public FragmentExtractor() {
  } // end of constructor

  // MediaConverter methods
   
  /**
   * Configure the converter.  This might include executable paths, conversion parameters, etc.
   * <p>This method can be invoked with an empty parameter set, to discover what (if any)
   *  parameters are required. If parameters are returned, and user interaction is possible, 
   *  then the user may be presented with an interface for setting/confirming these parameters. 
   * @param configuration The configuration for the converter. 
   * @return A list of configuration parameters must be set before the converter can be used.
   * @throws MediaException If an error occurs.
   */
  public ParameterSet configure(ParameterSet configuration) throws MediaException {
    Parameter sampleRate = configuration.get("sampleRate");
    if (sampleRate == null) {
      sampleRate = new Parameter(
        "sampleRate", Integer.class, "Sample rate", "Sample rate in Hz", false);
      configuration.addParameter(sampleRate);
    }
    if (getSampleRate() != null && sampleRate.getValue() == null) {
      sampleRate.setValue(getSampleRate());
    }
    
    Parameter start = configuration.get("start");
    if (start == null) {
      start = new Parameter(
        "start", Double.class, "Start", "Start time in seconds", true);
      configuration.addParameter(start);
    }
    if (getStart() != null && start.getValue() == null) {
      start.setValue(getStart());
    }
    
    Parameter end = configuration.get("end");
    if (end == null && end.getValue() == null) {
      end = new Parameter("end", Double.class, "End", "End time in seconds", true);
      configuration.addParameter(end);
    }
    if (getEnd() != null) {
      end.setValue(getEnd());
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
      AudioFormat format = audioInputStream.getFormat();
      if (getSampleRate() != null) {
         format = new AudioFormat(getSampleRate(), 16, format.getChannels(), true, true);
      }
      audioInputStream = AudioSystem.getAudioInputStream(format, audioInputStream);

      // ensure that the end time is not past the end of this file
      if (getEnd() != null || getStart() != null) {
        long frames = audioInputStream.getFrameLength();
        if (frames > 0) {
          double durationInSeconds = ((double)frames) / format.getFrameRate();
          if (getStart() != null && getStart() > durationInSeconds) {
            setStart(durationInSeconds);
          }
          if (getEnd() != null && getEnd() > durationInSeconds) {
            setEnd(durationInSeconds);
          }
        }
      }
      
      final AudioInputStream stream
        = new TruncatingAudioInputStream(audioInputStream, getStart(), getEnd());
      
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
   
} // end of class FragmentExtractor
