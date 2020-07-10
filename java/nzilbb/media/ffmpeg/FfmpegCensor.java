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
import java.util.List;
import java.io.File;
import java.io.IOException;
import nzilbb.util.Execution;
import nzilbb.util.IO;
import nzilbb.media.MediaCensor;
import nzilbb.media.MediaException;
import nzilbb.media.MediaThread;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;

/**
 * MediaCensor implemented by executing ffmpeg
 * @author Robert Fromont robert@fromont.net.nz
 */

public class FfmpegCensor
  implements MediaCensor
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
    * Filter for obfuscating audio.
    * @see #getAudioFilter()
    * @see #setAudioFilter(String)
    */
   protected String audioFilter;
   /**
    * Getter for {@link #audioFilter}: Filter for obfuscating audio.
    * @return Filter for obfuscating audio. Default is "lowpass=f=120";
    */
   public String getAudioFilter()
   {
      if (audioFilter == null) return "lowpass=f=120";
      return audioFilter;
   }
   /**
    * Setter for {@link #audioFilter}: Filter for obfuscating audio.
    * @param newAudioFilter Filter for obfuscating audio.
    */
   public void setAudioFilter(String newAudioFilter) { audioFilter = newAudioFilter; }

   
   /**
    * Delete source file when finished.
    * @see #getDeleteSource()
    * @see #setDeleteSource(Boolean)
    */
   protected Boolean deleteSource;
   /**
    * Getter for {@link #deleteSource}: Delete source file when finished.
    * @return Delete source file when finished.
    */
   public Boolean getDeleteSource() { return deleteSource; }
   /**
    * Setter for {@link #deleteSource}: Delete source file when finished.
    * @param newDeleteSource Delete source file when finished.
    */
   public void setDeleteSource(Boolean newDeleteSource) { deleteSource = newDeleteSource; }

   
   // Methods:
   
   /**
    * Default constructor.
    */
   public FfmpegCensor()
   {
   } // end of constructor

   // MediaCensor methods
   
   /**
    * Configure the censor.  This might include executable paths, obfuscation parameters, etc.
    * <p>This method can be invoked with an empty parameter set, to discover what (if any)
    *  parameters are required. If parameters are returned, and user interaction is possible, 
    *  then the user may be presented with an interface for setting/confirming these parameters. 
    * @param configuration The configuration for the censor. 
    * @return A list of configuration parameters must be set before the censor can be used.
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

      Parameter audioFilterParam = configuration.get("audioFilter");
      if (audioFilterParam == null)
      {
	 audioFilterParam = new Parameter(
	    "audioFilter", String.class, "Audio Filter",
	    "Filter for obfuscating audio",
	    false);
	 configuration.addParameter(audioFilterParam);
      }
      if (audioFilter != null)
      {
	 audioFilterParam.setValue(audioFilter);
      }
      try { audioFilterParam.apply(this); }
      catch(Exception exception) { throw new MediaException(exception); }

      Parameter deleteSourceParam = configuration.get("deleteSource");
      if (deleteSourceParam == null)
      {
	 deleteSourceParam = new Parameter(
	    "deleteSource", Boolean.class, "Delete Source",
	    "Delete source file when finished",
	    false);
	 configuration.addParameter(deleteSourceParam);
      }
      if (deleteSource != null)
      {
	 deleteSourceParam.setValue(deleteSource);
      }
      try { deleteSourceParam.apply(this); }
      catch(Exception exception) { throw new MediaException(exception); }

      return configuration;
   }
   
   /**
    * Determines whether this censor supports censoring of media of the given type.
    * @param sourceType The MIME type of the source media.
    * @return true if the censor can censor media of sourceType, false otherwise.
    * @throws MediaException If an error occurs.
    */
   public boolean typeSupported(String sourceType) throws MediaException
   {
      return sourceType.startsWith("audio");
   }

   /**
    * Starts censoring.
    * <p>The <var>boundaries</var> are a list of time points in seconds,
    * which define the boundaries of contiguous intervals.
    * Every second interval (starting from the first point) will be censored. 
    * An initial uncensored portion starting at 0s is assumed, 
    * so only add a point at 0.0 if you want the first censored interval to start at 0s. e.g.
    * <ul>
    *  <li>If <var>boundaries</var> is empty, then <var>destination</var> will have no obfuscated intevals (it will be a copy of <var>source</var>).</li>
    *  <li>If <var>boundaries</var> contains one point, at 1.0, then the media from 0.0-1.0 will be uncensored, and from 1.0 onward will be obfuscated.</li>
    *  <li>If <var>boundaries</var> contains two points, at 1.0 and 2.0, then the media from 0.0-1.0 will be uncensored, from 1.0-2.0 will be obfuscated, and from 2.0 onward will be uncensored.</li>
    *  <li>If <var>boundaries</var> contains three points, at 0.0, 1.0, and 2.0, then the media from 0.0-1.0 will be obfuscated, from 1.0-2.0 will be uncensored, and from 2.0 onward will be obfuscated.</li>
    * @param sourceType The MIME type of the source media.
    * @param source The (old) original file.
    * @param boundaries A list of time points, in seconds, which define the boundaries of contiguous intervals.
    * @param destination The (new) censored file.
    * @return A thread that is processing the media.
    * @throws MediaException If an error occurs.
    */
   public MediaThread start(String sourceType, File source, List<Double> boundaries, File destination) throws MediaException
   {
      if (getFfmpeg() == null) throw new MediaException("ffmpeg location not specified.");
      if (!getFfmpeg().exists()) throw new MediaException("ffmpeg program doesn exist: " + getFfmpeg().getPath());

      final File finalSource = source;
      final File finalDestination = destination;
      
      if (boundaries.size() == 0)
      { // no points, so just copy the source to the destination
	 MediaThread thread = new MediaThread(new Runnable(){
	       public void run()
	       {
		  try
		  {
		     IO.Copy(finalSource, finalDestination);
		     ((MediaThread)Thread.currentThread()).setPercentComplete(100);
		  }
		  catch(IOException exception)
		  {
		     ((MediaThread)Thread.currentThread()).setLastError(exception);
		  }
		  if (getDeleteSource() != null && getDeleteSource())
		  {
		     finalSource.delete();
		  }
	       }
	    });
	 thread.start();
	 return thread;
      }

      // the command is something like:
      // ffmpeg -y -i original.wav -filter_complex "[0:a]atrim=0:0.5[0];[0:a]atrim=0.5:1[1];[0:a]atrim=1:1.5[2];[0:a]atrim=1.5:2[3];[0:a]atrim=2[4];[1]lowpass=f=150[1f];[3]lowpass=f=150[3f];[0][1f][2][3f][4]concat=n=5:v=0:a=1[outa]" -map [outa] censored.wav

      // make up the complex filter
      StringBuffer filter = new StringBuffer();
      int numIntervals = 0; // boundary index
      
      // first define the intervals
      double lastBoundary = 0.0;
      for (Double boundary : boundaries)
      {
	 filter.append("[0:a]atrim=");
	 filter.append(lastBoundary); // start time
	 filter.append(":");
	 filter.append(boundary); // end time
	 filter.append("[");
	 filter.append(numIntervals++); // label
	 filter.append("];");

	 lastBoundary = boundary;
      } // next boundary
      // interval for "the rest"
      filter.append("[0:a]atrim=");
      filter.append(lastBoundary); // start time
      filter.append("[");
      filter.append(numIntervals++); // label
      filter.append("];");

      // now define the obfuscations (every second interval, starting from the second)
      for (int f = 1; f < numIntervals; f += 2)
      {
	 filter.append("[");
	 filter.append(f); // input label
	 filter.append("]");
	 filter.append(audioFilter); // filter
	 filter.append("[");
	 filter.append("f" + f); // output label
	 filter.append("];");
      }

      // now define the concatenation
      for (int c = 0; c < numIntervals; c++)
      {
	 filter.append("[");
	 if (c % 2 == 0)
	 { // even index
	    filter.append(c); // unfiltered label
	 }
	 else
	 { // odd index
	    filter.append("f" + c); // filtered label
	 }
	 filter.append("]");
      }
      filter.append("concat=n=");
      filter.append(numIntervals);
      filter.append(":v=0:a=1[outa]");
      
      Vector<String> vArguments = new Vector<String>();
      vArguments.add("-y");
      vArguments.add("-i");
      vArguments.add(source.getPath());
      vArguments.add("-filter_complex");
      vArguments.add(filter.toString());
      vArguments.add("-map");
      vArguments.add("[outa]");
      vArguments.add(destination.getPath());

      System.out.println("FfmpegCensor: " + vArguments);

      // start ffmpeg
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
	       
	       if (getDeleteSource() != null && getDeleteSource())
	       {
		  finalSource.delete();
	       }
	    }
	 });
      thread.start();
      return thread;
   }
   
} // end of class FfmpegCensor
