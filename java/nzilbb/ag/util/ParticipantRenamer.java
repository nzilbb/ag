//
// Copyright 2016-2020 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag.util;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import nzilbb.ag.*;

/**
 * Renames participants in a graph.
 * <p>Annotations are re-labelled on the participant layer, the turn layer, and the utterance layer.
 * <p>For this transformer to work, the {@link Schema} of the graph must have its {@link Schema#participantLayerId}, {@link Schema#turnLayerId}, and {@link Schema#utteranceLayerId} set.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class ParticipantRenamer
   implements GraphTransformer
{
   // Attributes:
   
   /**
    * A map from old speaker names to new speaker names.
    * @see #getOldNameToNewName()
    * @see #setOldNameToNewName(HashMap)
    */
   protected HashMap<String,String> oldNameToNewName = new HashMap<String,String>();
   /**
    * Getter for {@link #oldNameToNewName}: A map from old speaker names to new speaker names.
    * @return A map from old speaker names to new speaker names.
    */
   public HashMap<String,String> getOldNameToNewName() { return oldNameToNewName; }
   /**
    * Setter for {@link #oldNameToNewName}: A map from old speaker names to new speaker names.
    * @param newOldNameToNewName A map from old speaker names to new speaker names.
    */
   public ParticipantRenamer setOldNameToNewName(HashMap<String,String> newOldNameToNewName) { oldNameToNewName = newOldNameToNewName; return this; }

   
   // Methods:
   
   /**
    * Default constructor.
    */
   public ParticipantRenamer()
   {
   } // end of constructor

   /**
    * Convenience constructor for renaming a single participant.
    * @param oldName The participant's current name
    * @param newName The new name the participant should have after {@link #transform(Graph)}.
    */
   public ParticipantRenamer(String oldName, String newName)
   {
      rename(oldName, newName);
   } // end of constructor

   
   /**
    * Configures the transformer to rename a speaker.
    * @param oldName The participant's current name
    * @param newName The new name the participant should have after {@link #transform(Graph)}.
    */
   public void rename(String oldName, String newName)
   {
      if (!oldName.equals(newName))
      {
	 getOldNameToNewName().put(oldName, newName);
      }
   } // end of rename()


   /**
    * Transforms the graph.
    * @param graph The graph to transform.
    * @return The changes introduced by the tranformation.
    * @throws TransformationException If the transformation cannot be completed.
    */
   public Graph transform(Graph graph) throws TransformationException
   {
      Schema schema = graph.getSchema();
      if (schema.getParticipantLayerId() == null) 
	 throw new TransformationException(this, "No participant layer specified.");
      if (schema.getTurnLayerId() == null) 
	 throw new TransformationException(this, "No turn layer specified.");
      if (schema.getUtteranceLayerId() == null) 
	 throw new TransformationException(this, "No utterance layer specified.");

      if (getOldNameToNewName().size() > 0)
      { // there is actually some work to do
	 for (Annotation participant : graph.list(schema.getParticipantLayerId()))
	 {
	    if (getOldNameToNewName().containsKey(participant.getLabel()))
	    {
	       String newLabel = getOldNameToNewName().get(participant.getLabel());
               participant.setLabel(newLabel);
	       for (Annotation turn : participant.getAnnotations(schema.getTurnLayerId()))
	       {
                  turn.setLabel(newLabel);
		  for (Annotation utterance : turn.getAnnotations(schema.getUtteranceLayerId()))
		  {
                     utterance.setLabel(newLabel);
		  } // next utterance
	       } // next turn
	    } // changing name
	 } // next participant
      } // there are name changes
      return graph;
   }

} // end of class ParticipantRenamer
