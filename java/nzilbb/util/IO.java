//
// Copyright 2016-2019 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Base64;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.net.URLClassLoader;

/**
 * Helper functions for Input/Output operations.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class IO
{
   // Methods:

   /**
    * If the given file exists, it's renamed to a filename with the last modification timestamp included.
    * @param file The file to backup.
    * @throws IOException On IO error.
    */
   public static void Backup(File file)
      throws IOException
   {
      if (file.exists())
      {
         // there's and older version of the file, take a backup
         File backup = new File(
            file.getParentFile(), file.getName()  + ".bak"
            + new SimpleDateFormat("-yyyy-MM-dd-HH-mm-ss")
            .format(new java.util.Date(file.lastModified())) + "." + Extension(file));
         if (!file.renameTo(backup))
         {
            Copy(file, backup);
         }
      }
   } // end of Backup()

   /**
    * Determines the file extension (not including the dot) of the given file.
    * @param file The file.
    * @return The extension (not including the dot) of the given file.
    */
   public static String Extension(File file)
   {
      if (file.getName().indexOf('.') < 0) return "";
      return file.getName().substring(file.getName().lastIndexOf('.') + 1);
   } // end of Extension()

   /**
    * Determines the name of the given file without extension (not including the dot).
    * @param file The file.
    * @return The name of the given file without extension (not including the dot).
    */
   public static String WithoutExtension(File file)
   {
      return file.getName().replaceAll("\\.[^.]*","");
   } // end of Extension()
   
   /**
    * Copies a file.
    * @param source The original file.
    * @param destination The new file.
    * @throws IOException On file IO error.
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
    * @throws IOException On file IO error.
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
    * @throws IOException On file IO error.
    */
   public static long SaveUrlConnectionToFile(URLConnection connection, File file)
      throws IOException
   {
      FileOutputStream output = new FileOutputStream(file);
      InputStream input = connection.getInputStream();
      return Pump(input, output);
   } // end of SaveUrlConnectionToFile()

   /**
    * Reads the contents of the given InputStream into a String, assuming it's UTF-8 encoded text.
    * <p>The InputStream is closed by the this method.
    * @param input
    * @return The contents of the given InputStream.
    * @throws IOException
    */
   public static String InputStreamToString(InputStream input)
      throws IOException
   {
      StringBuilder content = new StringBuilder();
      BufferedReader reader = new BufferedReader(
         new InputStreamReader(input, StandardCharsets.UTF_8));
      String line = reader.readLine();
      while (line != null)
      {
         if (content.length() > 0) content.append("\n");
         content.append(line);
      
         line = reader.readLine();
      } // next line
      reader.close();
      return content.toString();
   } // end of InputStreamToString()
   
   /**
    * Copy all data from an input stream to an output stream.
    * @param input Source of data.
    * @param output Destination for data.
    * @return The number of bytes copied.
    * @throws IOException On file IO error.
    */
   public static long Pump(InputStream input, OutputStream output)
      throws IOException
   {
      return Pump(input, output, true);
   }
   /**
    * Copy all data from an input stream to an output stream.
    * @param input Source of data.
    * @param output Destination for data.
    * @param closeStreams true if the streams should be closed after the data is exhausted, false otherwise.
    * @return The number of bytes copied.
    * @throws IOException On file IO error.
    */
   public static long Pump(InputStream input, OutputStream output, boolean closeStreams)
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
      if (closeStreams)
      {
         output.close();
         input.close();
      }

      return totalBytes;
   } // end of Pump()
   
   /**
    * Scans the given jar file for instances of a particular class/interface.
    * The implementors must be registered in the jar file, by way of a manifest attribute named
    * after the class (with dots converted to hyphens to meet the attribute name requirements of
    * jar manifests). e.g. if <var>c</var> = <code>nzilbb.ag.serialize.IDeserializer</code> and
    * <var>file</var> contains a class called <code>nzilbb.praat.TextGridDeserializer</code> that
    * implements <code>nzilbb.ag.serialize.IDeserializer</code>, in order to be returned by this
    * method, <var>file</var> must also have a manifest attribute called 
    * <q>nzilbb-ag-serialize-IDeserializer</q> whose value is <q>nzilbb.praat.TextGridDeserializer</q>.
    * <p>There can be multiple implementing classes registered in the <var>file</var>; the value of
    * the manifest attribute is assumed to be a space-delimited list.
    * @param file The jar file.
    * @param parentLoader The parent class loader.
    * @param c The class/interface to search for.
    * @return A list of objects that implement the given class/interface, which may be empty.
    * @throws IOException On file IO error.
    */
   @SuppressWarnings({"rawtypes","unchecked"})
   public static Vector FindImplementorsInJar(File file, ClassLoader parentLoader, Class c)
      throws IOException
   {
      Vector implementors = new Vector();
      try
      {
         JarFile jar = new JarFile(file);
         URL[] url = new URL[] { file.toURI().toURL() };
         Manifest manifest = jar.getManifest();
         if (manifest != null)
         {
            Attributes attributes = manifest.getMainAttributes();
            Object convertersAtt = attributes.get(new Attributes.Name(c.getName().replace('.','-')));
            if (convertersAtt != null)
            {
               for (String className : convertersAtt.toString().split(" "))
               {
                  URLClassLoader classLoader = URLClassLoader.newInstance(url, parentLoader);
                  try
                  {
                     Object instance = classLoader.loadClass(className).getDeclaredConstructor().newInstance();
                     if (c.isInstance(instance))
                     {
                        implementors.add(instance);
                     }
                  }
                  catch(Throwable t) {}
               } // next class
            }
         }
      }
      catch(MalformedURLException x) {}
      return implementors;
   } // end of FindImplementorsInJar()
   
   /**
    * Determines the jar file that the given class comes from.
    * @param c The class implemented.
    * @return The jar file the given class implementation comes from, or null if it's not from a jar file.
    */
   @SuppressWarnings("rawtypes")
   public static File JarFileOfClass(Class c)
   {
      URL url = c.getResource(c.getSimpleName() + ".class");
      String sUrl = url.toString();
      if (!sUrl.startsWith("jar:"))
      {
         return null;
      }
      else
      {
         int iUriStart = 4;
         int iUriEnd = sUrl.indexOf("!");
         String sFileUri = sUrl.substring(iUriStart, iUriEnd);
         try
         {
            return new File(new URI(sFileUri));
         }
         catch(URISyntaxException exception)
         {
            return null;
         }
      }
   } // end of JarFileOfClass()

   /**
    * Recursively deletes a directory.
    * @param dir The directory to delete.
    * @return true if the the directory was successfully deleted, false otherwise.
    */
   public static boolean RecursivelyDelete(File dir)
   {
      if (dir.isDirectory())
      {
         for (File file : dir.listFiles())
         {
            if (!RecursivelyDelete(file)) return false;
         }
      }
      return dir.delete();
   } // end of RecursivelyDelete()

   /**
    * Encodes the content of the given url content as a BASE64-encoded string.
    * @param url A URL to the content to be encoded.
    * @return A BASE64-encoded representation of the content.
    * @throws IOException
    */
   public static String Base64Encode(String url)
      throws IOException
   {
      return Base64Encode(new URL(url).openStream());
   }

   /**
    * Encodes the content of the given url content as a BASE64-encoded string.
    * @param url A URL to the content to be encoded.
    * @return A BASE64-encoded representation of the content.
    * @throws IOException
    */
   public static String Base64Encode(URL url)
      throws IOException
   {
      return Base64Encode(url.openStream());
   }
  
   /**
    * Encodes the given content as a BASE64-encoded string.
    * @param content The content to encode.
    * @return A BASE64-encoded representation of the content.
    * @throws IOException
    */
   public static String Base64Encode(InputStream content)
      throws IOException
   {
      ByteArrayOutputStream base64Out = new ByteArrayOutputStream();
      OutputStream bytesOut = Base64.getEncoder().wrap(base64Out);
      byte[] buffer = new byte[1024];
      int byteCount = content.read(buffer);
      while (byteCount >= 0)
      {
         bytesOut.write(buffer, 0, byteCount);
         byteCount = content.read(buffer);
      } // read next chunk
      content.close();
      bytesOut.close();
      return base64Out.toString();
   } // end of base64EncodeFile()

   /**
    * Converts the given string into a version that's safe for a file name or URL.
    * @param s The possibly unsafe string.
    * @return The given string with characters that are unsafe for file names or URLs removed.
    */
   public static String SafeFileNameUrl(String s)
   {
      if (s == null) return "";
      return s.replaceAll("[\\\\\\?\\*\\+\\$]", "")
         .replaceAll("[\\|\\:\\!\\>\\<\\=\\^ ]", "_")	
         .replaceAll("@","-at-")
         .replaceAll("&","-amp-");
   } // end of SafeFileNameUrl()

   
} // end of class IO
