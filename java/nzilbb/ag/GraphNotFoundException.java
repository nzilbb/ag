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
 * An exception ocurring if a given graph could not be located.
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class GraphNotFoundException
   extends Exception
{
   
   /**
    * ID of the graph.
    * @see #getId()
    * @see #setId(String)
    */
   protected String id;
   /**
    * Getter for {@link #id}: ID of the graph.
    * @return ID of the graph.
    */
   public String getId() { return id; }
   /**
    * Setter for {@link #id}: ID of the graph.
    * @param newId ID of the graph.
    */
   public void setId(String newId) { id = newId; }

   /**
    * Default constructor.
    */
   public GraphNotFoundException()
   {
   } // end of constructor
   /**
    * Constructor with graph ID.
    * @param id ID of the graph.
    */
   public GraphNotFoundException(String id)
   {
      super("Graph not found: " + id);
      setId(id);
   } // end of constructor
   /**
    * Constructor with message.
    * @param id ID of the graph.
    * @param message The message to show.
    */
   public GraphNotFoundException(String id, String message)
   {
      super(message);
      setId(id);
   } // end of constructor
   /**
    * Constructor with cause.
    * @param id ID of the graph.
    * @param cause The root cause of the Exception.
    */
   public GraphNotFoundException(String id, Throwable cause)
   {
      super("Graph not found: " + id, cause);
      setId(id);
   } // end of constructor
   /**
    * Constructor with message and cause.
    * @param id ID of the graph.
    * @param message The message to show.
    * @param cause The root cause of the exception.
    */
   public GraphNotFoundException(String id, String message, Throwable cause)
   {
      super(message, cause);
      setId(id);
   } // end of constructor
} // end of class StoreException
