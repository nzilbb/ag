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
package nzilbb.ag;

/**
 * An exception ocurring during an annotation store operation because the operation is disallowed.
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class PermissionException
   extends Exception
{
   
   /**
    * The user ID that doesn't have permission, if available.
    * @see #getUser()
    * @see #setUser(String)
    */
   protected String user;
   /**
    * Getter for {@link #user}: The user ID that doesn't have permission, if available.
    * @return The user ID that doesn't have permission, if available.
    */
   public String getUser() { return user; }
   /**
    * Setter for {@link #user}: The user ID that doesn't have permission, if available.
    * @param newUser The user ID that doesn't have permission, if available.
    */
   public void setUser(String newUser) { user = newUser; }

   /**
    * Default constructor.
    */
   public PermissionException()
   {
   } // end of constructor
   /**
    * Constructor with message.
    * @param message The error message.
    */
   public PermissionException(String message)
   {
      super(message);
   } // end of constructor
   /**
    * Constructor with cause.
    * @param cause The root cause of the error.
    */
   public PermissionException(Throwable cause)
   {
      super(cause);
   } // end of constructor
   /**
    * Constructor with message and cause.
    * @param message The error message.
    * @param cause The root cause of the error.
    */
   public PermissionException(String message, Throwable cause)
   {
      super(message, cause);
   } // end of constructor

   /**
    * Constructor with message.
    * @param user The user ID.
    * @param message The error message.
    */
   public PermissionException(String user, String message)
   { 
      super("" + user + ": " + message);
      setUser(user);
   } // end of constructor
   /**
    * Constructor with message and cause.
    * @param user The user ID.
    * @param message The error message.
    * @param cause The root cause of the error.
    */
   public PermissionException(String user, String message, Throwable cause)
   {
      super("" + user + ": " + message, cause);
      setUser(user);
   } // end of constructor

} // end of class PermissionException
