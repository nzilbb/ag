//
// Copyright 2016-2021 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.util.List;
import java.util.SortedSet;
import java.util.Vector;
import nzilbb.ag.*;
import nzilbb.ag.cli.Transform;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;

/**
 * Ensures the structure of the graph is normalized.
 * <p> In a normalized graph:
 * <ul>
 *  <li> contiguous speaker turns by the same speaker are joined into one turn </li>
 *  <li> ordinal values are set so that they run from 1-<i>n</i> where <i>n</i> is the
 * number of children in a given parent, for words </li> 
 *  <li> words do not share anchors with turns nor utterances </li>
 *  <li> turn and utterance labels are the participant names </li>
 *  <li> Optionally, if the graph has a single un-named speaker, then the speaker renamed
 *       to be the same as the espisode name </li> 
 *  <li> ensures that utterances within a turn are chained together </li>
 *  <li> TODO: fills in missing utterances </li>
 *  <li> TODO: fills in missing turns </li>
 * </ul>
 * For this transformer to work, the {@link Schema} of the graph must have its 
 * {@link Schema#turnLayerId}, {@link Schema#utteranceLayerId}, {@link Schema#wordLayerId}, 
 * and {@link Schema#episodeLayerId} set. 
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Normalizes JSON-encoded annotation graphs from stdin")
public class Normalizer extends Transform implements GraphTransformer {

  /**
   * The maximum length of a label, or null if there's no limit. Default is 255.
   * @see #getMaxLabelLength()
   * @see #setMaxLabelLength(Integer)
   */
  protected Integer maxLabelLength = 255;
  /**
   * Getter for {@link #maxLabelLength}: The maximum length of a label, or null if there's
   * no limit. 
   * @return The maximum length of a label, or null if there's no limit.
   */
  public Integer getMaxLabelLength() { return maxLabelLength; }
  /**
   * Setter for {@link #maxLabelLength}: The maximum length of a label, or null if there's
   * no limit. 
   * @param newMaxLabelLength The maximum length of a label, or null if there's no limit.
   */
  @Switch("The maximum length of a label")
  public Normalizer setMaxLabelLength(Integer newMaxLabelLength) { maxLabelLength = newMaxLabelLength; return this; }

  /**
   * Minimum amount of time between two turns by the same speaker, with no intervening
   * speaker, for which the inter-turn pause counts as a turn change boundary. If the
   * pause is shorter than this, the turns are merged into one. Default is 0.0; 
   * @see #getMinimumTurnPauseLength()
   * @see #setMinimumTurnPauseLength(Double)
   */
  protected Double minimumTurnPauseLength = 0.0;
  /**
   * Getter for {@link #minimumTurnPauseLength}: Minimum amount of time between two turns
   * by the same speaker, with no intervening speaker, for which the inter-turn pause
   * counts as a turn change boundary. If the pause is shorter than this, the turns are
   * merged into one. 
   * @return Minimum amount of time between two turns by the same speaker, with no
   * intervening speaker, for which the inter-turn pause counts as a turn change
   * boundary. If the pause is shorter than this, the turns are merged into one. 
   */
  public Double getMinimumTurnPauseLength()
  {
    if (minimumTurnPauseLength == null) minimumTurnPauseLength = 0.0;
    return minimumTurnPauseLength;
  }
  /**
   * Setter for {@link #minimumTurnPauseLength}: Minimum amount of time between two turns
   * by the same speaker, with no intervening speaker, for which the inter-turn pause
   * counts as a turn change boundary. If the pause is shorter than this, the turns are
   * merged into one. 
   * @param newMinimumTurnPauseLength Minimum amount of time between two turns by the same
   * speaker, with no intervening speaker, for which the inter-turn pause counts as a turn
   * change boundary. If the pause is shorter than this, the turns are merged into one. 
   */
  @Switch("Same-speaker inter-turn pauses shorter than this are merged into one turn")
  public Normalizer setMinimumTurnPauseLength(Double newMinimumTurnPauseLength) { minimumTurnPauseLength = newMinimumTurnPauseLength; return this; }
      
  // Methods:
   
  /**
   * Default constructor.
   */
  public Normalizer() {
  } // end of constructor

  /**
   * Transforms the graph.
   * @param graph The graph to transform.
   * @return The changes introduced by the tranformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph transform(Graph graph) throws TransformationException {
    Schema schema = graph.getSchema();
    if (schema.getParticipantLayerId() == null) 
      throw new TransformationException(this, "No participant layer specified.");
    if (schema.getTurnLayerId() == null) 
      throw new TransformationException(this, "No turn layer specified.");
    if (schema.getUtteranceLayerId() == null) 
      throw new TransformationException(this, "No utterance layer specified.");

    if (schema.getEpisodeLayerId() != null) {
      Annotation[] episode = graph.all(schema.getEpisodeLayerId());
      Annotation[] participants = graph.all(schema.getParticipantLayerId());
      if (participants.length == 1 && episode.length > 0) {
        Annotation onlyParticipant = participants[0];
        // if the only participant has no name
        if (onlyParticipant.getLabel() == null || onlyParticipant.getLabel().length() == 0)
        { // name them after the episode
          onlyParticipant.setLabel(episode[0].getLabel());
        }
      }
    } // episode layer set

    // ensure turns and utterances are labelled with participant labels
    for (Annotation participant : graph.all(schema.getParticipantLayerId())) {
      for (Annotation turn : participant.getAnnotations(schema.getTurnLayerId())) {
        if (!participant.getLabel().equals(turn.getLabel())) {
          turn.setLabel(participant.getLabel());
        }
        if (schema.getWordLayerId() != null) {
          Annotation lastUtterance = null;
          for (Annotation utterance : turn.getAnnotations(schema.getUtteranceLayerId())) {
            if (!participant.getLabel().equals(utterance.getLabel())) {
              utterance.setLabel(participant.getLabel());
            }
            // check utterances are chained together
            if (lastUtterance != null 
                && !utterance.getStartId().equals(lastUtterance.getEndId())) { // not chained
              // change the last utterance end
              Anchor newEnd = utterance.getStart();
              // change all annotations with this turn that end there
              // to instead end gat the start of this utterance
              // this will include lastUtterance itself, and any words/phones it might 
              // share anchors with
              for (Annotation ending : lastUtterance.getEnd().getEndingAnnotations()) {
                if (turn == ending.first(schema.getTurnLayerId())) {
                  ending.setEnd(newEnd);
                }
              } // next ending annotation
            }
            lastUtterance = utterance;
          } // next utterance
        }
      } // next turn
    } // next participant
   
    // join subsequent turns by the same speaker...
    new Coalescer()
      .setLayerId(schema.getTurnLayerId())
      .setMinimumPauseLength(getMinimumTurnPauseLength())
      .transform(graph);

    if (schema.getWordLayerId() != null) {
      // disconnect words from turns and utterances
      for (Annotation word : graph.all(schema.getWordLayerId())) {
        // check start anchor
        if (word.getStart().isStartOn(schema.getTurnLayerId())
            || word.getStart().isEndOn(schema.getTurnLayerId())
            || word.getStart().isStartOn(schema.getUtteranceLayerId())
            || word.getStart().isEndOn(schema.getUtteranceLayerId())) {
          // disconnect start
          final Anchor oldStart = word.getStart();
               
          // create a new anchor
          final Anchor newStart = new Anchor(
            null, word.getStart().getOffset(),
            // if the word end has the same confidence, just copy it, otherwise use "default"
            oldStart.getConfidence() != null
            && oldStart.getConfidence().equals(word.getEnd().getConfidence())?
            oldStart.getConfidence():Constants.CONFIDENCE_DEFAULT);
          graph.addAnchor(newStart);
               
          // assign it to the word and any descendants that use it
          LayerTraversal<Vector<Change>> descendantTraverser
            = new LayerTraversal<Vector<Change>>(new Vector<Change>(), word) {
                protected void pre(Annotation annotation) {
                  if (annotation.getStart().equals(oldStart)) {
                    annotation.setStart(newStart);
                  }
                }
              };
        } // disconnect start
            
        // check end anchor
        if (word.getEnd().isStartOn(schema.getTurnLayerId())
            || word.getEnd().isEndOn(schema.getTurnLayerId())
            || word.getEnd().isStartOn(schema.getUtteranceLayerId())
            || word.getEnd().isEndOn(schema.getUtteranceLayerId())) { // disconnect end
          final Anchor oldEnd = word.getEnd();
               
          // create a new anchor
          final Anchor newEnd = new Anchor(
            null, word.getEnd().getOffset(),
            // if the word start has the same confidence, just copy it, otherwise use "default"
            word.getEnd().getConfidence() != null
            && oldEnd.getConfidence().equals(word.getStart().getConfidence())?
            oldEnd.getConfidence():Constants.CONFIDENCE_DEFAULT); 
          graph.addAnchor(newEnd);
               
          // assign it to the word and any descendants that use it
          LayerTraversal<Vector<Change>> descendantTraverser
            = new LayerTraversal<Vector<Change>>(new Vector<Change>(), word) {
                protected void pre(Annotation annotation) {
                  if (annotation.getEnd().equals(oldEnd)) {
                    annotation.setEnd(newEnd);
                  }
                }
              };
        } // disconnect end
      } // next word
    }
    if (maxLabelLength != null) {
      // ensure no annotation has a label longer than the limit
      for (Annotation a : graph.getAnnotationsById().values()) {
        if (a.getLabel() != null
            && a.getLabel().length() > maxLabelLength.intValue()) {
          // truncate the label TODO: split annotation in two
          a.setLabel(a.getLabel().substring(0,maxLabelLength.intValue()));
        }
      } // next annotation
    }
    return graph;
  }
   
  /**
   * Moves all of the children of the following turn into the preceding turn, set the the
   * end of the preceding to the end of the following, and marks the following for
   * deletion. 
   * @param preceding The preceding, surviving turn.
   * @param following The following turn, which will be deleted.
   */
  public void mergeTurns(Annotation preceding, Annotation following) {
    // set anchor
    if (preceding.getEnd().getOffset() == null
        || following.getEnd().getOffset() == null
        || preceding.getEnd().getOffset() < following.getEnd().getOffset()) {
      preceding.setEnd(following.getEnd());
    }
    Vector<Annotation> toRemove = new Vector<Annotation>();

    // for each child layer
    for (String childLayerId : following.getAnnotations().keySet()) {
      // move everything from following to preceding
      int ordinal = 1;
      if (preceding.getAnnotations().containsKey(childLayerId)) {
        ordinal = preceding.getAnnotations().get(childLayerId).size() + 1;
      }
      for (Annotation child : following.annotations(childLayerId)) {
        // in order to prevent the annotation from checking/correcting all peer ordinals
        // which is time-consuming and unnecessary, we first unset the parent
        child.setParent(null);
        // then we set the ordinal
        child.setOrdinal(ordinal++);
        // and finally, we set the new parent, without appending (to skip the peer-checking step)
        child.setParent(preceding, false);
      } // next child annotation
    } // next child layer

    following.destroy();
  } // end of joinTurns()

  /** Command line interface entrypoint: reads JSON-encoded transcripts from stdin,
   * normalizes them, and writes them to stdout. */
  public static void main(String argv[]) {
    Normalizer cli = new Normalizer();
    if (cli.processArguments(argv)) {
      cli.start();
    }
  }

} // end of class Normalizer
