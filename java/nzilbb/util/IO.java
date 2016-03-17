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
package nzilbb.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Helper functions for Input/Output operations.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class IO
{
   // Methods:

   /**
    * Determines the file extension (not including the dot) of the given file.
    * @param file
    * @return The extension (not including the dot) of the given file.
    */
   public static String Extension(File file)
   {
      if (file.getName().indexOf('.') < 0) return "";
      return file.getName().substring(file.getName().lastIndexOf('.') + 1);
   } // end of Extension()

   /**
    * Determines the name of the given file without extension (not including the dot).
    * @param file
    * @return The name of the given file without extension (not including the dot).
    */
   public static String WithoutExtension(File file)
   {
      return file.getName().replaceAll("\\.[^.]*","");
   } // end of Extension()
   
   /**
    * Copies a file.
    * @param source
    * @param destination
    * @throws IOException
    */
   public static void Copy(File source, File destination)
    throws IOException
   {
      SaveUrlToFile(source.toURI().toURL(), destination);
   } // end of copy()

   
   /**
    * Saves the content of a URL to a file.
    * @param url The URL of the content.
    * @param file The file to save the content to.
    * @return The number size of the content in bytes.
    * @throws IOException
    */
   public static long SaveUrlToFile(URL url, File file)
    throws IOException
   {
      return SaveUrlConnectionToFile(url.openConnection(), file);
   } // end of SaveUrlToFile()

   /**
    * Saves the content of a URL to a file.
    * @param connection The URL connection to the content.
    * @param file The file to save the content to.
    * @return The number size of the content in bytes.
    * @throws IOException
    */
   public static long SaveUrlConnectionToFile(URLConnection connection, File file)
    throws IOException
   {
      FileOutputStream output = new FileOutputStream(file);
      InputStream input = connection.getInputStream();
      return Pump(input, output);
   } // end of SaveUrlConnectionToFile()
   
   /**
    * Copy all data from an input stream to an output stream.
    * @param input Source of data.
    * @param output Destination for data.
    * @return The number of bytes copied.
    * @throws IOException
    */
   public static long Pump(InputStream input, OutputStream output)
    throws IOException
   {
      long totalBytes = 0;
      
      byte[] buffer = new byte[1024];
      int bytesRead = input.read(buffer);
      while (bytesRead >= 0)
      {
	 totalBytes += bytesRead;
	 output.write(buffer, 0, bytesRead);
	 bytesRead = input.read(buffer);
      } // next chunk	
      output.flush();
      output.close();
      input.close();

      return totalBytes;
   } // end of Pump()

   
} // end of class IO
