//
// Copyright 2020 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag.automation;

/**
 * Thrown when an Annotator configuration is invalid.
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class DictionaryException extends Exception {
   
   // Methods:

   /**
    * The dictionary.
    * @see #getDictionary()
    */
   protected Dictionary dictionary;
   /**
    * Getter for {@link #dictionary}: The dictionary.
    * @return The dictionary.
    */
   public Dictionary getDictionary() { return dictionary; }
   
   /**
    * Constructor with message.
    * @param dictionary The dictionary raising the exception.
    * @param message The error message.
    */
   public DictionaryException(Dictionary dictionary, String message) {
      super(message);
      this.dictionary = dictionary;
   } // end of constructor
   /**
    * Constructor with cause.
    * @param dictionary The dictionary raising the exception.
    * @param cause The root cause of the error.
    */
   public DictionaryException(Dictionary dictionary, Throwable cause) {
      super(cause);
      this.dictionary = dictionary;
   } // end of constructor
   /**
    * Constructor with message and cause.
    * @param dictionary The dictionary raising the exception.
    * @param message The error message.
    * @param cause The root cause of the error.
    */
   public DictionaryException(Dictionary dictionary, String message, Throwable cause) {
      super(message, cause);
      this.dictionary = dictionary;
   } // end of constructor
   
} // end of class DictionaryException
