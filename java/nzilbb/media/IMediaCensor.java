//
// Copyright 2017 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.media;

import java.io.File;
import java.util.List;
import nzilbb.configure.ParameterSet;

/**
 * Censors media by taking a series of time points and obfuscating every other interval between them.
 * <p>The method of obfuscation is implementation dependent (e.g. silence or low-pass filter for audio, 
 * or blacking out or pixellation for video), but the result should be a media file of the same type and
 * same length.
 * @author Robert Fromont robert@fromont.net.nz
 */

public interface IMediaCensor
{
   /**
    * Configure the censor.  This might include executable paths, obfuscation parameters, etc.
    * <p>This method can be invoked with an empty parameter set, to discover what (if any)
    *  parameters are required. If parameters are returned, and user interaction is possible, 
    *  then the user may be presented with an interface for setting/confirming these parameters. 
    * @param configuration The configuration for the censor. 
    * @return A list of configuration parameters must be set before the censor can be used.
    * @throws MediaException If an error occurs.
    */
   public ParameterSet configure(ParameterSet configuration) throws MediaException;
   
   /**
    * Determines whether this censor supports censoring of media of the given type.
    * @param sourceType The MIME type of the source media.
    * @return true if the censor can censor media of sourceType, false otherwise.
    * @throws MediaException If an error occurs.
    */
   public boolean typeSupported(String sourceType) throws MediaException;

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
    * </ul>
    * @param sourceType The MIME type of the source media.
    * @param source The (old) original file.
    * @param boundaries A list of time points, in seconds, which define the boundaries of contiguous intervals. 
    * @param destination The (new) censored file.
    * @return A thread that is processing the media.
    * @throws MediaException If an error occurs.
    */
   public MediaThread start(String sourceType, File source, List<Double> boundaries, File destination) throws MediaException;
   
} // end of IMediaCensor
