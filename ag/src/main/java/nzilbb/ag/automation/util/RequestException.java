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
package nzilbb.ag.automation.util;

import java.net.URI;

/**
 * An error that occurred during a RequestRouter request.
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class RequestException extends Exception {
   
   // Attributes:
   
   /**
    * The request method.
    * @see #getMethod()
    */
   protected String method;
   /**
    * Getter for {@link #method}: The request method.
    * @return The request method.
    */
   public String getMethod() { return method; }

   /**
    * The request URI.
    * @see #getUri()
    */
   protected URI uri;
   /**
    * Getter for {@link #uri}: The request URI.
    * @return The request URI.
    */
   public URI getUri() { return uri; }

   /**
    * The HTTP status code to return.
    * @see #getHttpStatus()
    */
   protected int httpStatus = 400;
   /**
    * Getter for {@link #httpStatus}: The HTTP status code to return.
    * @return The HTTP status code to return.
    */
   public int getHttpStatus() { return httpStatus; }
   /**
    * Setter for {@link #httpStatus}: The HTTP status code to return.
    * @param newHttpStatus The HTTP status code to return.
    */
   public RequestException setHttpStatus(int newHttpStatus) { httpStatus = newHttpStatus; return this; }
   
   // Methods:
   
   /**
    * Constructor.
    * @param httpStatus The HTTP status code to return.
    * @param message The error message.
    * @param method The request method.
    * @param uri The request URI.
    * @param cause The root cause of the exception.
    */
   public RequestException(int httpStatus, String message, String method, URI uri, Throwable cause) {
      super(message, cause);
      this.httpStatus = httpStatus;
      this.method = method;
      this.uri = uri;
   } // end of constructor
   
   /**
    * Constructor.
    * @param httpStatus The HTTP status code to return.
    * @param method The request method.
    * @param uri The request URI.
    * @param cause The root cause of the exception.
    */
   public RequestException(int httpStatus, String method, URI uri, Throwable cause) {
      super(cause.getMessage(), cause);
      this.httpStatus = httpStatus;
      this.method = method;
      this.uri = uri;
   } // end of constructor
   
   /**
    * Constructor.
    * @param httpStatus The HTTP status code to return.
    * @param message The error message.
    * @param method The request method.
    * @param uri The request URI.
    */
   public RequestException(int httpStatus, String message, String method, URI uri) {
      super(message);
      this.httpStatus = httpStatus;
      this.method = method;
      this.uri = uri;
   } // end of constructor

   
   /**
    * String representation of the object.
    * @return String representation of the object.
    */
   public String toString() {
      return "" + httpStatus + ": " + getMessage() + " for " + method + " " + uri;
   } // end of toString()

} // end of class RequestException
