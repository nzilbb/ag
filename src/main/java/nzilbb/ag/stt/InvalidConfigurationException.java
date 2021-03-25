//
// Copyright 2015-2021 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag.stt;

/**
 * Thrown when an Transcriber configuration is invalid.
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class InvalidConfigurationException extends Exception {
   
   // Attributes:

   
   /**
    * The transcriber that was misconfigured.
    * @see #getTranscriber()
    * @see #setTranscriber(Transcriber)
    */
   protected Transcriber transcriber;
   /**
    * Getter for {@link #transcriber}: The transcriber that was misconfigured.
    * @return The transcriber that was misconfigured.
    */
   public Transcriber getTranscriber() { return transcriber; }
   /**
    * Setter for {@link #transcriber}: The transcriber that was misconfigured.
    * @param newTranscriber The transcriber that was misconfigured.
    */
   public InvalidConfigurationException setTranscriber(Transcriber newTranscriber) { transcriber = newTranscriber; return this; }
   
   // Methods:
   
   /**
    * Constructor.
    * @param transcriber The transcriber rasing the exception.
    */
   public InvalidConfigurationException(Transcriber transcriber) {
      setTranscriber(transcriber);
   } // end of constructor
   /**
    * Constructor with message.
    * @param transcriber The transcriber rasing the exception.
    * @param message The error message.
    */
   public InvalidConfigurationException(Transcriber transcriber, String message) {
      super(message);
      setTranscriber(transcriber);
   } // end of constructor
   /**
    * Constructor with cause.
    * @param transcriber The transcriber rasing the exception.
    * @param cause The root cause of the error.
    */
   public InvalidConfigurationException(Transcriber transcriber, Throwable cause) {
      super(cause);
      setTranscriber(transcriber);
   } // end of constructor
   /**
    * Constructor with message and cause.
    * @param transcriber The transcriber rasing the exception.
    * @param message The error message.
    * @param cause The root cause of the error.
    */
   public InvalidConfigurationException(Transcriber transcriber, String message, Throwable cause) {
      super(message, cause);
      setTranscriber(transcriber);
   } // end of constructor
} // end of class InvalidConfigurationException
