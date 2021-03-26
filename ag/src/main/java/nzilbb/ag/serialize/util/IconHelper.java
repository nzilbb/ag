//
// Copyright 2016 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag.serialize.util;

import java.net.URL;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import nzilbb.ag.serialize.*;

/**
 * Helper functions for dealing with (de)serializer icons.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class IconHelper
{
   // Methods:

   
   /**
    * Ensures the icon file for the given descriptor has been extracted to the given directory.
    * @param descriptor Descriptor of serialization.
    * @param directory Directory to unpack the icon into if it's not already there.
    * @return The icon file.
    * @throws IOException On file IO error.
    */
   public static File EnsureIconFileExists(SerializationDescriptor descriptor, File directory)
      throws IOException
   {
      File iconFile = new File(directory, IconFilename(descriptor));
      if (!iconFile.exists())
      { // extract the icon
	 ExtractFile(descriptor.getIcon(), iconFile);
      }
      return iconFile;
   } // end of EnsureIconFileExists()

   /**
    * Transforms the given descriptor's MIME type name into something that is safe to use as a file name.
    * @param descriptor Descriptor of serialization.
    * @return The descriptor's MIME type name transformed into something that is safe to use as a file name.
    */
   public static String IconFilename(SerializationDescriptor descriptor)
   {
      String mimeType = descriptor.getMimeType();
      String iconExtension = descriptor.getIcon().toString().replaceAll(".*(\\.[^.]*)$","$1");
      return (mimeType + iconExtension).replaceAll("[^A-Za-z0-9.]+", "-");
   } // end of FilenameSafeMimeType()
   
   /**
    * Extracts the configuration applet for the given layer manager from the given jar file
    * @param jarUrl The URL of the JAR.
    * @param fDestination Destination file.
    * @throws IOException On file IO error.
    */
   public static void ExtractFile(URL jarUrl, File fDestination)
      throws IOException
   {
      File fConfigJar = null;
      InputStream jarStream = jarUrl.openStream();
      if (jarStream != null)
      {
	 FileOutputStream outStream = new FileOutputStream(fDestination);
	 PumpStream(jarStream, outStream);
	 jarStream.close();
	 outStream.close();
      } // there is a config applet
   } // end of ExtractFile()

   /**
    * Reads all data from an input stream and writes it to an output stream.  Once finished, neither stream is closed.
    * @param i Input stream.
    * @param o Output stream.
    * @throws IOException On IO error.
    */
   public static void PumpStream(InputStream i, OutputStream o)
      throws IOException
   {
      byte[] buffer = new byte[1024];
      int bytesRead = i.read(buffer);
      while(bytesRead >= 0)
      {
	 o.write(buffer, 0, bytesRead);
	 bytesRead = i.read(buffer);
      } // next chunk of data
   } // end of PumpStream()
} // end of class IconHelper
