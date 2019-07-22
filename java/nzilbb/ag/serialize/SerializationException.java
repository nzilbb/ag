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
import java.lang.StringBuilder;
/**
 * Thrown when {@link IDeserializer#deserialize()} could not complete due to fatal errors.
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class SerializationException
  extends Exception
{
  public enum ErrorType { InvalidDocument, Alignment, Tokenization, Other };

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
  public SerializationException()
  {
  } // end of constructor

  /**
   * Constructor.
   * @param cause The causing error.
   */
  public SerializationException(Throwable cause)
  {
    super(cause);
    addError(ErrorType.Other, cause.getMessage());
  } // end of constructor

  /**
   * Constructor.
   * @param message The error message.
   */
  public SerializationException(String message)
  {
    super(message);
    addError(ErrorType.Other, message);
  } // end of constructor
   
  /**
   * Constructor.
   * @param message The error message.
   */
  public SerializationException(ErrorType type, String message)
  {
    super(message);
    addError(type, message);
  } // end of constructor
   
  /**
   * Adds an error. If the given type of error has already been added, the description is appended to the existing description.
   * @param type The error type.
   * @param description The error's description.
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

  
  /**
   * Represents the exception with all its errors as a String.
   * @return A String representation of the exception with all its errors.
   */
  @Override public String toString()
  {
    StringBuilder s = new StringBuilder();
    s.append(super.toString());
    for (String error : errors.values())
    {
      if (!error.equals(getMessage()))
      {
        s.append("\n");
        s.append(error);
      }
    } // next error
    return s.toString();
  } // end of toString()

} // end of class SerializationException
