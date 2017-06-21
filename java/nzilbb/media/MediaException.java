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

/**
 * Exception during media processing.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class MediaException
  extends java.lang.Exception
{
   // Attributes:
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public MediaException()
   {
   } // end of constructor
   /**
    * Constructor with message.
    * @param message The error message.
    */
   public MediaException(String message)
   {
      super(message);
   } // end of constructor
   /**
    * Constructor with cause.
    * @param cause The root cause of the error.
    */
   public MediaException(Throwable cause)
   {
      super(cause);
   } // end of constructor
   /**
    * Constructor with message and cause.
    * @param message The error message.
    * @param cause The root cause of the error.
    */
   public MediaException(String message, Throwable cause)
   {
      super(message, cause);
   } // end of constructor
} // end of class MediaException
