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

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import nzilbb.util.IO;

/**
 * An InputStream with an associated Name
 * @author Robert Fromont robert@fromont.net.nz
 */
public class NamedStream
{
   // Attributes:

   /**
    * The input stream
    * @see #getStream()
    * @see #setStream(InputStream)
    */
   protected InputStream isStream;
   /**
    * Getter for {@link #isStream}: The input stream
    * @return The input stream
    */
   public InputStream getStream() { return isStream; }
   /**
    * Setter for {@link #isStream}: The input stream
    * @param isNewStream The input stream
    */
   public void setStream(InputStream isNewStream) { isStream = isNewStream; }

   /**
    * The name of the stream
    * @see #getName()
    * @see #setName(String)
    */
   protected String sName;
   /**
    * Getter for {@link #sName}: The name of the stream
    * @return The name of the stream
    */
   public String getName() { return sName; }
   /**
    * Setter for {@link #sName}: The name of the stream
    * @param sNewName The name of the stream
    */
   public void setName(String sNewName) { sName = sNewName; }

   
   /**
    * Optional MIME Type for the stream.
    * @see #getMimeType()
    * @see #setMimeType(String)
    */
   protected String sMimeType;
   /**
    * Getter for {@link #sMimeType}: Optional MIME Type for the stream.
    * @return Optional MIME Type for the stream.
    */
   public String getMimeType() { return sMimeType; }
   /**
    * Setter for {@link #sMimeType}: Optional MIME Type for the stream.
    * @param sNewMimeType Optional MIME Type for the stream.
    */
   public void setMimeType(String sNewMimeType) { sMimeType = sNewMimeType; }

      
   // Methods:
      
   /**
    * Default constructor
    */
   public NamedStream()
   {
   } // end of constructor

   /**
    * Constructor
    * @param stream
    * @param name
    */
   public NamedStream(InputStream stream, String name)
   {
      setStream(stream);
      setName(name);
   } // end of constructor

   /**
    * Constructor
    * @param stream
    * @param name
    */
   public NamedStream(InputStream stream, String name, String mimeType)
   {
      setStream(stream);
      setName(name);
      setMimeType(mimeType);
   } // end of constructor

   /**
    * Constructor
    * @param stream file
    */
   public NamedStream(File f)
      throws java.io.FileNotFoundException
   {
      setStream(new FileInputStream(f));
      setName(f.getName());
   } // end of constructor

   
   /**
    * Saves the named stream as a file into the given directory.
    * @param directory
    * @return The number of bytes saved.
    * @throws IOException
    */
   public long save(File directory)
    throws IOException
   {
      if (directory == null) throw new IOException("Directory is null.");
      if (!directory.exists()) throw new IOException("Does not exist: " + directory.getPath());
      if (!directory.isDirectory()) throw new IOException("Not a directory: " + directory.getPath());
      File f = new File(directory, getName());
      FileOutputStream out = new FileOutputStream(f);
      return IO.Pump(getStream(), out);      
   } // end of save()

} // end of class NamedStream
