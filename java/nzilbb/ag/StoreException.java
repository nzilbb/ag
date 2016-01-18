//
// Copyright 2015 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of LaBB-CAT.
//
//    LaBB-CAT is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 2 of the License, or
//    (at your option) any later version.
//
//    LaBB-CAT is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with LaBB-CAT; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package nzilbb.ag;

/**
 * An exception ocurring during an annotation store operation.
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class StoreException
   extends Exception
{
   /**
    * Default constructor.
    */
   public StoreException()
   {
   } // end of constructor
   /**
    * Constructor with message.
    * @param message
    */
   public StoreException(String message)
   {
      super(message);
   } // end of constructor
   /**
    * Constructor with cause.
    * @param cause
    */
   public StoreException(Throwable cause)
   {
      super(cause);
   } // end of constructor
   /**
    * Constructor with message and cause.
    * @param message
    * @param cause
    */
   public StoreException(String message, Throwable cause)
   {
      super(message, cause);
   } // end of constructor
} // end of class StoreException
