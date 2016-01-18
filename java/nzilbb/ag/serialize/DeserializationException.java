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
package nzilbb.ag.serialize;

import java.util.LinkedHashMap;
/**
 * Thrown when {@link IDeserializer#deserialize()} could not complete due to fatal errors.
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class DeserializationException
   extends Exception
{
   public enum ErrorType { Other };

   // Attributes:
   
   /**
    * Collection of fatal errors that occurred during deserialization. The key is the error type, and the value is a displayable description of the error.
    * @see #getErrors()
    * @see #setErrors(LinkedHashMap)
    */
   protected LinkedHashMap<ErrorType,String> errors = new LinkedHashMap<ErrorType,String>();
   /**
    * Getter for {@link #errors}: Collection of fatal errors that occurred during deserialization. The key is the error type, and the value is a displayable description of the error.
    * @return Collection of fatal errors that occurred during deserialization. The key is the error type, and the value is a displayable description of the error.
    */
   public LinkedHashMap<ErrorType,String> getErrors() { return errors; }
   /**
    * Setter for {@link #errors}: Collection of fatal errors that occurred during deserialization. The key is the error type, and the value is a displayable description of the error.
    * @param newErrors Collection of fatal errors that occurred during deserialization. The key is the error type, and the value is a displayable description of the error.
    */
   public void setErrors(LinkedHashMap<ErrorType,String> newErrors) { errors = newErrors; }
   
   // Methods:
   
   /**
    * Default constructor
    */
   public DeserializationException()
   {
   } // end of constructor

   
   /**
    * Adds an error. If the given type of error has already been added, the description is appended to the existing description.
    * @param type
    * @param description
    */
   public void addError(ErrorType type, String description)
   {
      if (!errors.containsKey(type))
      {
	 errors.put(type, description);
      }
      else
      { // one of those errors already occurred - add to it's description
	 errors.put(type, errors.get(type) + "\n" + description);
      }
   } // end of addError()

} // end of class DeserializationException
