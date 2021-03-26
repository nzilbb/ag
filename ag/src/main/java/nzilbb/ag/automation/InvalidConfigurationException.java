//
// Copyright 2015-2016 New Zealand Institute of Language, Brain and Behaviour, 
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

import nzilbb.ag.TransformationException;

/**
 * Thrown when an Annotator configuration is invalid.
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class InvalidConfigurationException extends TransformationException {
   
   // Attributes:
   
   /**
    * The {@link Annotator} rasing the exception.
    * @return The {@link Annotator} rasing the exception.
    */
   public Annotator getTransformer() { return (Annotator)transformer; }
   
   // Methods:
   
   /**
    * Constructor.
    * @param annotator The annotator rasing the exception.
    */
   public InvalidConfigurationException(Annotator annotator) {
      super(annotator);
   } // end of constructor
   /**
    * Constructor with message.
    * @param annotator The annotator rasing the exception.
    * @param message The error message.
    */
   public InvalidConfigurationException(Annotator annotator, String message) {
      super(annotator, message);
   } // end of constructor
   /**
    * Constructor with cause.
    * @param annotator The annotator rasing the exception.
    * @param cause The root cause of the error.
    */
   public InvalidConfigurationException(Annotator annotator, Throwable cause) {
      super(annotator, cause);
   } // end of constructor
   /**
    * Constructor with message and cause.
    * @param annotator The annotator rasing the exception.
    * @param message The error message.
    * @param cause The root cause of the error.
    */
   public InvalidConfigurationException(Annotator annotator, String message, Throwable cause) {
      super(annotator, message, cause);
   } // end of constructor
} // end of class InvalidConfigurationException
