//
// Copyright 2017-2020 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.util.Vector;
import java.io.File;
import nzilbb.util.Execution;
import nzilbb.media.MediaConverter;
import nzilbb.media.MediaException;
import nzilbb.media.MediaThread;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;

/**
 * MediaConverter implemented by executing ffmpeg
 * @author Robert Fromont robert@fromont.net.nz
 */

public class FfmpegConverter
  implements MediaConverter
{
   // Attributes:
   
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
    * Command line for conversion.
    * @see #getConversionCommandLine()
    * @see #setConversionCommandLine(String)
    */
   protected String conversionCommandLine = null;
   /**
    * Getter for {@link #conversionCommandLine}: Command line for conversion.
    * @return Command line for conversion - default value is "-loglevel error -nostdin -strict experimental -i {0} -strict experimental {1}".
    */
   public String getConversionCommandLine()
   {
      if (conversionCommandLine == null) return "-loglevel error -nostdin -strict experimental -i {0} -strict experimental {1}";
      return conversionCommandLine; }
   /**
    * Setter for {@link #conversionCommandLine}: Command line for conversion.
    * @param newConversionCommandLine Command line for conversion.
    */
   public void setConversionCommandLine(String newConversionCommandLine) { conversionCommandLine = newConversionCommandLine; }

   
   // Methods:
   
   /**
    * Default constructor.
    */
   public FfmpegConverter()
   {
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
   public ParameterSet configure(ParameterSet configuration) throws MediaException
   {
      Parameter ffmpegPath = configuration.get("ffmpegPath");
      if (ffmpegPath == null)
      {
	 ffmpegPath = new Parameter(
	    "ffmpegPath", File.class, "ffmpeg Path", "Directory where ffmpeg is installed", true);
	 configuration.addParameter(ffmpegPath);
      }
      if (getFfmpeg() != null)
      {
	 ffmpegPath.setValue(getFfmpeg().getParentFile());
      }

      if (ffmpegPath.getValue() != null)
      {
	 File exe = new File((File)ffmpegPath.getValue(), "ffmpeg");
	 if (!exe.exists()) exe = new File((File)ffmpegPath.getValue(), "ffmpeg.exe");
	 if (!exe.exists())
	 {
	    ffmpegPath.setValue(null);
	 }
	 else
	 {
	    setFfmpeg(exe);
	 }
      }

      Parameter commandLine = configuration.get("conversionCommandLine");
      if (commandLine == null)
      {
	 commandLine = new Parameter(
	    "conversionCommandLine", String.class, "Conversion command line",
	    "Command line arguments for format conversion, where {0} is the input file and {1} is the output file.",
	    false);
	 configuration.addParameter(commandLine);
      }
      if (conversionCommandLine != null)
      {
	 commandLine.setValue(conversionCommandLine);
      }
      try { commandLine.apply(this); }
      catch(Exception exception) { throw new MediaException(exception); }
      
      return configuration;
   }
   
   /**
    * Determines whether this converter supports conversion between the given types.
    * @param sourceType The MIME type of the source media.
    * @param destinationType The MIME type of the destination format.
    * @return true if the converter can convert from the sourceType to the destinationType, false otherwise.
    * @throws MediaException If an error occurs.
    */
   public boolean conversionSupported(String sourceType, String destinationType) throws MediaException
   {
      return true;
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
   public MediaThread start(String sourceType, File source, String destinationType, File destination) throws MediaException
   {
      if (getFfmpeg() == null) throw new MediaException("ffmpeg location not specified.");
      if (!getFfmpeg().exists()) throw new MediaException("ffmpeg program doesn exist: " + getFfmpeg().getPath());
      Vector<String> vArguments = new Vector<String>();
      for (String arg : getConversionCommandLine().split(" "))
      {
	 if (arg.equals("{0}"))
	 {
	    vArguments.add(source.getPath());
	 }
	 else if (arg.equals("{1}"))
	 {
	    vArguments.add(destination.getPath());
	 }
	 else
	 {
	    vArguments.add(arg);
	 }
      } // next argument
      final File finalDestination = destination;
      MediaThread thread = new MediaThread(new Execution(getFfmpeg(), vArguments) {
	    public void run()
	    {
	       // run the execution
	       super.run();
	       
	       // then check the output file
	       if (finalDestination.length() == 0)
	       {
		  // something failed, ensure there's no 0 byte file left
		  finalDestination.delete(); 
	       }
	    }
	 });
      thread.start();
      return thread;
   }
   
} // end of class FfmpegConverter
