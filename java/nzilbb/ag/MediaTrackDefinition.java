//
// Copyright 2015-2020 New Zealand Institute of Language, Brain and Behaviour, 
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

import org.json.JSONObject;

/**
 * Definition of a possible media track that a graph might be associated with.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class MediaTrackDefinition
{
   // Attributes:
   
   /**
    * The file suffix associated with the track.
    * @see #getSuffix()
    * @see #setSuffix(String)
    */
   protected String suffix;
   /**
    * Getter for {@link #suffix}: The file suffix associated with the track.
    * @return The file suffix associated with the track.
    */
   public String getSuffix() { return suffix; }
   /**
    * Setter for {@link #suffix}: The file suffix associated with the track.
    * @param newSuffix The file suffix associated with the track.
    */
   public MediaTrackDefinition setSuffix(String newSuffix) { suffix = newSuffix; return this; }

   /**
    * The description of the track.
    * @see #getDescription()
    * @see #setDescription(String)
    */
   protected String description;
   /**
    * Getter for {@link #description}: The description of the track.
    * @return The description of the track.
    */
   public String getDescription() { return description; }
   /**
    * Setter for {@link #description}: The description of the track.
    * @param newDescription The description of the track.
    */
   public MediaTrackDefinition setDescription(String newDescription) { description = newDescription; return this; }
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public MediaTrackDefinition()
   {
   } // end of constructor

   /**
    * Constructor from attribute values.
    * @param suffix The file suffix associated with the track.
    * @param description The description of the track.
    */
   public MediaTrackDefinition(String suffix, String description)
   {
      setSuffix(suffix);
      setDescription(description);
   } // end of constructor
   
   /**
    * Constructor from JSON.
    * @param json A JSON representation of the object.
    */
   public MediaTrackDefinition(JSONObject json)
   {
      setSuffix(json.optString("suffix"));
      setDescription(json.optString("description"));
   } // end of constructor
} // end of class MediaTrackDefinition
