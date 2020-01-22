//
// Copyright 2016 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.util.Vector;
import java.util.SortedSet;
import nzilbb.ag.*;

/**
 * Ensures the structure of the graph is normalized.
 * <p>In a normalized graph:
 * <ul>
 *  <li>contiguous speaker turns by the same speaker are joined into one turn</li>
 *  <li>ordinal values are set so that they run from 1-<i>n</i> where <i>n</i> is the number of children in a given parent, for words</li>
 *  <li>words do not share anchors with turns nor utterances</li>
 *  <li>turn and utterance labels are the participant names</li>
 *  <li>Optionally, if the graph has a single un-named speaker, then the speaker renamed to be the same as the espisode name </li>
 *  <li>ensures that utterances within a turn are chained together</li>
 *  <li>TODO: fills in missing utterances</li>
 *  <li>TODO: fills in missing turns</li>
 * </ul>
 * For this transformer to work, the {@link Schema} of the graph must have its {@link Schema#turnLayerId}, {@link Schema#utteranceLayerId}, {@link Schema#wordLayerId}, and {@link Schema#episodeLayerId} set.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class Normalizer
  implements IGraphTransformer
{
   // Attributes:

   
   /**
    * The maximum length of a label, or null if there's no limit. Default is 255.
    * @see #getMaxLabelLength()
    * @see #setMaxLabelLength(Integer)
    */
   protected Integer maxLabelLength = 255;
   /**
    * Getter for {@link #maxLabelLength}: The maximum length of a label, or null if there's no limit.
    * @return The maximum length of a label, or null if there's no limit.
    */
   public Integer getMaxLabelLength() { return maxLabelLength; }
   /**
    * Setter for {@link #maxLabelLength}: The maximum length of a label, or null if there's no limit.
    * @param newMaxLabelLength The maximum length of a label, or null if there's no limit.
    */
   public Normalizer setMaxLabelLength(Integer newMaxLabelLength) { maxLabelLength = newMaxLabelLength; return this; }

   /**
    * Minimum amount of time between two turns by the same speaker, with no intervening speaker, for which the inter-turn pause counts as a turn change boundary. If the pause is shorter than this, the turns are merged into one. Default is 0.0;
    * @see #getMinimumTurnPauseLength()
    * @see #setMinimumTurnPauseLength(Double)
    */
   protected Double minimumTurnPauseLength = 0.0;
   /**
    * Getter for {@link #minimumTurnPauseLength}: Minimum amount of time between two turns by the same speaker, with no intervening speaker, for which the inter-turn pause counts as a turn change boundary. If the pause is shorter than this, the turns are merged into one.
    * @return Minimum amount of time between two turns by the same speaker, with no intervening speaker, for which the inter-turn pause counts as a turn change boundary. If the pause is shorter than this, the turns are merged into one.
    */
   public Double getMinimumTurnPauseLength()
   {
      if (minimumTurnPauseLength == null) minimumTurnPauseLength = 0.0;
      return minimumTurnPauseLength;
   }
   /**
    * Setter for {@link #minimumTurnPauseLength}: Minimum amount of time between two turns by the same speaker, with no intervening speaker, for which the inter-turn pause counts as a turn change boundary. If the pause is shorter than this, the turns are merged into one.
    * @param newMinimumTurnPauseLength Minimum amount of time between two turns by the same speaker, with no intervening speaker, for which the inter-turn pause counts as a turn change boundary. If the pause is shorter than this, the turns are merged into one.
    */
   public Normalizer setMinimumTurnPauseLength(Double newMinimumTurnPauseLength) { minimumTurnPauseLength = newMinimumTurnPauseLength; return this; }

      
   // Methods:
   
   /**
    * Default constructor.
    */
   public Normalizer()
   {
   } // end of constructor

   /**
    * Transforms the graph.
    * @param graph The graph to transform.
    * @return The changes introduced by the tranformation.
    * @throws TransformationException If the transformation cannot be completed.
    */
   public Vector<Change> transform(Graph graph) throws TransformationException
   {
      Schema schema = graph.getSchema();
      if (schema.getParticipantLayerId() == null) 
	 throw new TransformationException(this, "No participant layer specified.");
      if (schema.getTurnLayerId() == null) 
	 throw new TransformationException(this, "No turn layer specified.");
      if (schema.getUtteranceLayerId() == null) 
	 throw new TransformationException(this, "No utterance layer specified.");
      if (schema.getWordLayerId() == null) 
	 throw new TransformationException(this, "No word layer specified.");

      if (graph.getTracker() == null) graph.trackChanges();
      Vector<Change> changes = new Vector<Change>();

      if (schema.getEpisodeLayerId() != null)
      {
	 Annotation[] episode = graph.list(schema.getEpisodeLayerId());
	 Annotation[] participants = graph.list(schema.getParticipantLayerId());
	 if (participants.length == 1 && episode.length > 0)
	 {
	    Annotation onlyParticipant = participants[0];
	    // if the only participant has no name
	    if (onlyParticipant.getLabel() == null || onlyParticipant.getLabel().length() == 0)
	    { // name them after the episode
	       changes.addAll( // record changes for:
		  onlyParticipant.setLabel(episode[0].getLabel()));
	    }
	 }
      } // episode layer set

      // ensure turns and utterances are labelled with participant labels
      for (Annotation participant : graph.list(schema.getParticipantLayerId()))
      {
	 for (Annotation turn : participant.getAnnotations(schema.getTurnLayerId()))
	 {
	    if (!participant.getLabel().equals(turn.getLabel()))
	    {
	       changes.addAll( // record changes for:
		  turn.setLabel(participant.getLabel()));
	    }
	    Annotation lastUtterance = null;
	    for (Annotation utterance : turn.getAnnotations(schema.getUtteranceLayerId()))
	    {
	       if (!participant.getLabel().equals(utterance.getLabel()))
	       {
		  changes.addAll( // record changes for:
		     utterance.setLabel(participant.getLabel()));
	       }
	       // check utterances are chained together
	       if (lastUtterance != null 
		   && !utterance.getStartId().equals(lastUtterance.getEndId()))
	       { // not chained
		  // change the last utterance end
		  Anchor newEnd = utterance.getStart();
		  // change all annotations with this turn that end there
		  // to instead end at the start of this utterance
		  // this will include lastUtterance itself, and any words/phones it might 
		  // share anchors with
		  for (Annotation ending : lastUtterance.getEnd().getEndingAnnotations())
		  {
		     if (turn == ending.my(schema.getTurnLayerId()))
		     {
			changes.addAll( // record changes for:
			   ending.setEnd(newEnd));
		     }
		  } // next ending annotation
	       }
	       lastUtterance = utterance;
	    } // next utterance
	 } // next turn
      } // next participant

      // join subsequent turns by the same speaker...
      // for each participant (assumed to be parent of turn)
      for (Annotation participant : graph.list(schema.getParticipantLayerId()))
      {
	 Annotation[] turns = participant.annotations(schema.getTurnLayerId());
	 // go back through all the turns, looking for a turn for the same speaker that is
	 // joined to, or overlaps, this one

	 for (int i = turns.length - 2; i >= 0; i--)
	 {
	    Annotation preceding = turns[i];
	    Annotation following = turns[i + 1];
	    boolean mergeTurns = false;
	    if (preceding.getEnd().getOffset() != null
		&& following.getStart().getOffset() != null)
	    {
	       if (preceding.getEnd().getOffset() >= following.getStart().getOffset())
	       {
		  mergeTurns = true;
	       }
	       else if (getMinimumTurnPauseLength() > 0
			&& preceding.getEnd().getOffset() + getMinimumTurnPauseLength() >= following.getStart().getOffset())
	       { // there is a short enough pause between two turns of the same participant
		  // but there also must be no intervening speakers
		  if (graph.overlappingAnnotations(
			 preceding.getEnd(), following.getStart(), schema.getTurnLayerId())
		      .length == 0)
		  {
		     mergeTurns = true;
		  }
	       }
	    }
	    if (mergeTurns)
	    {
	       changes.addAll( // record changes for:}
		  mergeTurns(preceding, following));
	    }
	 } // next preceding turn

      } // next turn parent

      // disconnect words from turns and utterances
      for (Annotation word : graph.list(schema.getWordLayerId()))
      {
	 // check start anchor
	 if (word.getStart().isStartOn(schema.getTurnLayerId())
	     || word.getStart().isEndOn(schema.getTurnLayerId())
	     || word.getStart().isStartOn(schema.getUtteranceLayerId())
	     || word.getStart().isEndOn(schema.getUtteranceLayerId()))
	 { // disconnect start
	    final Anchor oldStart = word.getStart();

	    // create a new anchor
	    final Anchor newStart = new Anchor(
	       null, word.getStart().getOffset(),
	       // if the word end has the same confidence, just copy it, otherwise use "default"
	       oldStart.getConfidence() != null
	       && oldStart.getConfidence().equals(word.getEnd().getConfidence())?
	       oldStart.getConfidence():Constants.CONFIDENCE_DEFAULT);
	    graph.addAnchor(newStart);
	    changes.addAll(newStart.getChanges());

	    // assign it to the word and any descendants that use it
	    LayerTraversal<Vector<Change>> descendantTraverser = new LayerTraversal<Vector<Change>>(changes, word)
	    {
	       protected void pre(Annotation annotation)
	       {
		  if (annotation.getStart().equals(oldStart))
		  {
		     getResult().addAll( // record changes for:
			annotation.setStart(newStart));
		  }
	       }
	    };
	 } // disconnect start
	 
	 // check end anchor
	 if (word.getEnd().isStartOn(schema.getTurnLayerId())
	     || word.getEnd().isEndOn(schema.getTurnLayerId())
	     || word.getEnd().isStartOn(schema.getUtteranceLayerId())
	     || word.getEnd().isEndOn(schema.getUtteranceLayerId()))
	 { // disconnect end
	    final Anchor oldEnd = word.getEnd();

	    // create a new anchor
	    final Anchor newEnd = new Anchor(
	       null, word.getEnd().getOffset(),
	       // if the word start has the same confidence, just copy it, otherwise use "default"
	       word.getEnd().getConfidence() != null
	       && oldEnd.getConfidence().equals(word.getStart().getConfidence())?
	       oldEnd.getConfidence():Constants.CONFIDENCE_DEFAULT); 
	    graph.addAnchor(newEnd);
	    changes.addAll(newEnd.getChanges());

	    // assign it to the word and any descendants that use it
	    LayerTraversal<Vector<Change>> descendantTraverser = new LayerTraversal<Vector<Change>>(changes, word)
	    {
	       protected void pre(Annotation annotation)
	       {
		  if (annotation.getEnd().equals(oldEnd))
		  {
		     getResult().addAll( // record changes for:
			annotation.setEnd(newEnd));
		  }
	       }
	    };
	 } // disconnect end
      } // next word

      if (maxLabelLength != null)
      {
	 // ensure no annotation has a label longer than the limit
	 for (Annotation a : graph.getAnnotationsById().values())
	 {
	    if (a.getLabel().length() > maxLabelLength.intValue())
	    {
	       // truncate the label TODO: split annotation in two
	       changes.addAll( // record changes for:
		  a.setLabel(a.getLabel().substring(0,maxLabelLength.intValue())));		  
	    }
	 } // next annotation
      }
      

      return changes;
   }

   
   /**
    * Moves all of the children of the following turn into the preceding turn, set the the end of the preceding to the end of the following, and marks the following for deletion.
    * @param preceding The preceding, surviving turn.
    * @param following The following turn, which will be deleted.
    * @return The changes for this merge.
    */
   public Vector<Change> mergeTurns(Annotation preceding, Annotation following)
   {
      Vector<Change> changes = new Vector<Change>();

      // set anchor
      if (preceding.getEnd().getOffset() == null
	  || following.getEnd().getOffset() == null
	  || preceding.getEnd().getOffset() < following.getEnd().getOffset()) 
      {
	 changes.addAll( // record changes for:
	    preceding.setEnd(following.getEnd()));
      }
      Vector<Annotation> toRemove = new Vector<Annotation>();

      // for each child layer
      for (String childLayerId : following.getAnnotations().keySet())
      {
	 // move everything from following to preceding
	 int ordinal = 1;
	 if (preceding.getAnnotations().containsKey(childLayerId))
	 {
	    ordinal = preceding.getAnnotations().get(childLayerId).size() + 1;
	 }
	 for (Annotation child : following.annotations(childLayerId))
	 {
	    changes.addAll( // record changes for:
	       child.setParent(preceding));
	    changes.addAll( // record changes for:
	       child.setOrdinal(ordinal++));
	 } // next child annotation
      } // next child layer

      changes.add( // record changes for:
	 following.destroy());
      return changes;
   } // end of joinTurns()


} // end of class Normalizer
