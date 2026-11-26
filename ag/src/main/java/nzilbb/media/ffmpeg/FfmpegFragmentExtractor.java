//
// Copyright 2026 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.media.ffmpeg;

import java.util.Locale;
import java.util.Vector;
import java.io.File;
import java.text.MessageFormat;
import nzilbb.util.Execution;
import nzilbb.media.MediaConverter;
import nzilbb.media.MediaException;
import nzilbb.media.MediaThread;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;

/**
 * MediaConverter that extracts portions of media samples using the ffmpeg.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class FfmpegFragmentExtractor implements MediaConverter {
  
  /**
   * Executable file for ffmpeg.
   * @see #getFfmpeg()
   * @see #setFfmpeg(File)
   */
  protected File ffmpeg;
  /**
   * Getter for {@link #ffmpeg}: Executable file for ffmpeg.
   * @return Executable file for ffmpeg.
   */
  public File getFfmpeg() { return ffmpeg; }
  /**
   * Setter for {@link #ffmpeg}: Executable file for ffmpeg.
   * @param newFfmpeg Executable file for ffmpeg.
   */
  public void setFfmpeg(File newFfmpeg) { ffmpeg = newFfmpeg; }
  
  /**
   * Command line for processing.
   * @see #getCommandLine()
   * @see #setCommandLine(String)
   */
  protected String commandLine = null;
  /**
   * Getter for {@link #commandLine}: Command line for processing.
   * @return Command line for processing - default value is "-loglevel error -nostdin -strict experimental -i {0} -ss {2,number,0.000} -to {3,number,0.000} {1}".
   */
  public String getCommandLine()
  {
    if (commandLine == null) return "-loglevel error -nostdin -strict experimental -i {0} -ss {2,number,0.000} -to {3,number,0.000} {1}";
    return commandLine; }
  /**
   * Setter for {@link #commandLine}: Command line for processing.
   * @param newCommandLine Command line for processing.
   */
  public void setCommandLine(String newCommandLine) { commandLine = newCommandLine; }
  
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
  public FfmpegFragmentExtractor setStart(Double start) { this.start = start; return this; }

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
  public FfmpegFragmentExtractor setEnd(Double end) { this.end = end; return this; }

  /**
   * Default constructor.
   */
  public FfmpegFragmentExtractor() {
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
    Parameter ffmpegPath = configuration.get("ffmpegPath");
    if (ffmpegPath == null) {
      ffmpegPath = new Parameter(
        "ffmpegPath", File.class, "ffmpeg Path", "Directory where ffmpeg is installed", true);
      configuration.addParameter(ffmpegPath);
    }
    if (getFfmpeg() != null) {
      ffmpegPath.setValue(getFfmpeg().getParentFile());
    }
    
    if (ffmpegPath.getValue() != null) {
      File exe = new File((File)ffmpegPath.getValue(), "ffmpeg");
      if (!exe.exists()) exe = new File((File)ffmpegPath.getValue(), "ffmpeg.exe");
      if (!exe.exists()) {
        ffmpegPath.setValue(null);
      } else {
        setFfmpeg(exe);
      }
    }
    
    Parameter commandLine = configuration.get("commandLine");
    if (commandLine == null) {
      commandLine = new Parameter(
        "commandLine", String.class, "Command line",
        "Command line arguments for format conversion, where {0} is the input file and {1} is the output file, {2} is the start time in seconds, and {3} is the end time in seconds.",
        false);
      configuration.addParameter(commandLine);
    }
    if (getCommandLine() != null) {
      commandLine.setValue(getCommandLine());
    }
    try { commandLine.apply(this); }
    catch(Exception exception) { throw new MediaException(exception); }
    
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
    if (end == null) {
      end = new Parameter("end", Double.class, "End", "End time in seconds", true);
      configuration.addParameter(end);
    }
    if (getEnd() != null && end.getValue() == null) {
      end.setValue(getEnd());
    }
    configuration.apply(this);
    return configuration;
  }
  
  /**
   * Determines whether this converter supports conversion between the given types.
   * @param sourceType The MIME type of the source media.
   * @param destinationType The MIME type of the destination format.
   * @return true if the converter can convert from the sourceType to
   * the destinationType, false otherwise. 
   * @throws MediaException If an error occurs.
   */
  public boolean conversionSupported(String sourceType, String destinationType)
    throws MediaException {
    return (sourceType.startsWith("video/") || sourceType.startsWith("audio/"))
      && (destinationType.startsWith("video/") && destinationType.startsWith("audio/"));
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
    if (getFfmpeg() == null) throw new MediaException("ffmpeg location not specified.");
    if (!getFfmpeg().exists()) {
      throw new MediaException("ffmpeg program doesn exist: " + getFfmpeg().getPath());
    }
    Vector<String> arguments = new Vector<String>();
    Object[] params = { source.getPath(), destination.getPath(), getStart(), getEnd() };
    for (String arg : getCommandLine().split(" ")) {
      if (arg.startsWith("{")) {
        arguments.add(new MessageFormat(arg, Locale.UK).format(params));
      } else {
        arguments.add(arg);
      }
    } // next argument
    final File finalDestination = destination;
    MediaThread thread = new MediaThread(new Execution(getFfmpeg(), arguments) {
        public void run() {
          // run the execution
          super.run();
          
          // then check the output file
          if (finalDestination.length() == 0) {
            // something failed, ensure there's no 0 byte file left
            finalDestination.delete(); 
          }
        }
      });
    thread.start();
    return thread;
  }  
} // end of class FfmpegFragmentExtractor
