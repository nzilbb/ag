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

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * An InputStream for one-off reading of a temporary file. 
 * When the stream is closed or finalized, the file is deleted.
 * This implementation uses a FileInputStream member, rather than inheriting from FileInputStream,
 * so that the stream is not opened until it's actually going to be read.  This ensures that a
 * huge collection of TempFileInputStream objects can be handled without running out of open file
 * handles.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class TempFileInputStream
   extends InputStream
{
   // Attributes:

   /** The underlying FileInputStream */
   protected FileInputStream fis;
   
   /**
    * The temporary file for deletion.
    * @see #getTempFile()
    * @see #setTempFile(File)
    */
   protected File fTempFile;
   /**
    * Getter for {@link #fTempFile}: The temporary file for deletion.
    * @return The temporary file for deletion.
    */
   public File getTempFile() { return fTempFile; }
   /**
    * Setter for {@link #fTempFile}: The temporary file for deletion.
    * <br>Side-effect: deleteOnExit() is called on the file.
    * @param fNewTempFile The temporary file for deletion.
    */
   public void setTempFile(File fNewTempFile) 
   { 
      fTempFile = fNewTempFile; 
      fTempFile.deleteOnExit();
   }

   
   /**
    * Whether to delete the file when close() is called or not. Default is TRUE.
    * @see #getDeleteOnClose()
    * @see #setDeleteOnClose(Boolean)
    */
   protected Boolean bDeleteOnClose = Boolean.TRUE;
   /**
    * Getter for {@link #bDeleteOnClose}: Whether to delete the file when close() is called or not. Default is TRUE.
    * @return Whether to delete the file when close() is called or not. Default is TRUE.
    */
   public Boolean getDeleteOnClose() { return bDeleteOnClose; }
   /**
    * Setter for {@link #bDeleteOnClose}: Whether to delete the file when close() is called or not. Default is TRUE.
    * @param bNewDeleteOnClose Whether to delete the file when close() is called or not. Default is TRUE.
    */
   public void setDeleteOnClose(Boolean bNewDeleteOnClose) { bDeleteOnClose = bNewDeleteOnClose; }
   
   // Methods:
   
   /**
    * Constructor. Doesn't open the file; that doesn't happen until some data is wanted.
    * <br>Side-effect: deleteOnExit() is called on the file.
    * @param file The file.
    * @throws FileNotFoundException If the file doesn't exist.
    */
   public TempFileInputStream(File file)
      throws FileNotFoundException
   {
      if (!file.exists()) throw new FileNotFoundException(file.getName());
      setTempFile(file);
   } // end of constructor

   
   /**
    * Ensures the FileInputStream has been initialised.
    * @throws FileNotFoundException If the temporary file no longer exists.
    */
   protected void checkFileInputStream()
      throws FileNotFoundException
   {
      if (fis == null) fis = new FileInputStream(getTempFile());
   } // end of checkFileInputStream()


   /** Overridden to delete the file after closing the stream
    * @throws IOException On IO error.
    */
   public void close()
      throws IOException
   {
      try { fis.close(); } catch(IOException exception) {}
      if (bDeleteOnClose) fTempFile.delete();
      fis = null;
   }

   /** Overridden to pass through the call to {@link #fis}
    * @throws IOException On IO error.
    */
   public int read()
      throws IOException
   {
      checkFileInputStream();
      return fis.read();
   }
   /** Overridden to pass through the call to {@link #fis} 
    * @throws IOException On IO error.
    */
   public int read(byte[] b)
      throws IOException
   {
      checkFileInputStream();
      return fis.read(b);
   }
   /** Overridden to pass through the call to {@link #fis} 
    * @throws IOException On IO error.
    */
   public int read(byte[] b, int off, int len)
      throws IOException
   {
      checkFileInputStream();
      return fis.read(b, off, len);
   }
   /** Overridden to pass through the call to {@link #fis} 
    * @throws IOException On IO error.
    */
   public long skip(long n)
      throws IOException
   {
      checkFileInputStream();
      return fis.skip(n);
   }
   /** Overridden to pass through the call to {@link #fis} 
    * @throws IOException On IO error.
    */
   public int available()
      throws IOException
   {
      checkFileInputStream();
      return fis.available();
   }
   /** Overridden to pass through the call to {@link #fis} 
    */
   public void mark(int readlimit)
   {
      try { checkFileInputStream(); } catch(FileNotFoundException exception) {}
      fis.mark(readlimit);
   }
   /** Overridden to pass through the call to {@link #fis} 
    * @throws IOException On IO error.
    */
   public void reset()
      throws IOException
   {
      checkFileInputStream();
      fis.reset();
   }
   /** Overridden to pass through the call to {@link #fis} */
   public boolean markSupported()
   {
      try { checkFileInputStream(); } catch(FileNotFoundException exception) {}
      return fis.markSupported();
   }
} // end of class TempFileInputStream
