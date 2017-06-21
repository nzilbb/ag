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
import nzilbb.configure.ParameterSet;

/**
 * Converts media from one format to another.
 * @author Robert Fromont robert@fromont.net.nz
 */

public interface IMediaConverter
{
   /**
    * Configure the converter.  This might include executable paths, conversion parameters, etc.
    * <p>This method can be invoked with an empty parameter set, to discover what (if any)
    *  parameters are required. If parameters are returned, and user interaction is possible, 
    *  then the user may be presented with an interface for setting/confirming these parameters. 
    * @param configuration The configuration for the converter. 
    * @return A list of configuration parameters must be set before the converter can be used.
    * @throws MediaException If an error occurs.
    */
   public ParameterSet configure(ParameterSet configuration) throws MediaException;
   
   /**
    * Determines whether this converter supports conversion between the given types.
    * @param sourceType The MIME type of the source media.
    * @param destinationType The MIME type of the destination format.
    * @return true if the converter can convert from the sourceType to the destinationType, false otherwise.
    * @throws MediaException If an error occurs.
    */
   public boolean conversionSupported(String sourceType, String destinationType) throws MediaException;

   /**
    * Starts conversion.
    * @param sourceType The MIME type of the source media.
    * @param source The source file.
    * @param destinationType The MIME type of the destination format.
    * @param destination The destination file.
    * @return A thread that is processing the media.
    * @throws MediaException If an error occurs.
    */
   public MediaThread start(String sourceType, File source, String destinationType, File destination) throws MediaException;
   
} // end of IMediaConverter
