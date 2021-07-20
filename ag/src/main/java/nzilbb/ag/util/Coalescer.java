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

/**
 * 'Coalesces' a graph by joining together annotations on a given layer, where the
 * annotations are contiguous, have the same parent, and the same label.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class Coalescer implements GraphTransformer {
  
  /**
   * ID of the layer on which contiguous peers with the same label will be joined.
   * @see #getLayerId()
   * @see #setLayerId(String)
   */
  protected String layerId;
  /**
   * Getter for {@link #layerId}: ID of the layer on which contiguous peers with the same
   * label will be joined.  
   * @return ID of the layer on which contiguous peers with the same label will be joined.
   */
  public String getLayerId() { return layerId; }
  /**
   * Setter for {@link #layerId}: ID of the layer on which contiguous peers with the same
   * label will be joined. 
   * @param newLayerId ID of the layer on which contiguous peers with the same label will
   * be joined. 
   */
  public Coalescer setLayerId(String newLayerId) { layerId = newLayerId; return this; }
  
  /**
   * Minimum amount of time between two peers with the same parent, with no intervening
   * peers, for which the inter-annotation pause counts as a not congituous. If the
   * pause is shorter than this, the annotations can merged into one. Default is 0.0; 
   * @see #getMinimumPauseLength()
   * @see #setMinimumPauseLength(Double)
   */
  protected Double minimumPauseLength = 0.0;
  /**
   * Getter for {@link #minimumPauseLength}: Minimum amount of time between two peers with
   * the same parent, with no intervening peers, for which the inter-annotation pause
   * counts as a not congituous. If the pause is shorter than this, the annotations can
   * merged into one.
   * @return Minimum amount of time between two peers with the same parent, with no
   * intervening peers, for which the inter-annotation pause counts as a not
   * congituous. If the pause is shorter than this, the annotations can merged into one.
   */
  public Double getMinimumPauseLength()
  {
    if (minimumPauseLength == null) minimumPauseLength = 0.0;
    return minimumPauseLength;
  }
  /**
   * Setter for {@link #minimumPauseLength}: Minimum amount of time between two peers with
   * the same parent, with no intervening peers, for which the inter-annotation pause
   * counts as a not congituous. If the pause is shorter than this, the annotations can
   * merged into one. 
   * @param newMinimumPauseLength Minimum amount of time between two peers with the same
   * parent, with no intervening peers, for which the inter-annotation pause counts as a
   * not congituous. If the pause is shorter than this, the annotations can merged into one.
   */
  public Coalescer setMinimumPauseLength(Double newMinimumPauseLength) { minimumPauseLength = newMinimumPauseLength; return this; }
      
  // Methods:
   
  /**
   * Default constructor.
   */
  public Coalescer() {
  } // end of constructor

  /**
   * Transforms the graph.
   * @param graph The graph to transform.
   * @return The changes introduced by the tranformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph transform(Graph graph) throws TransformationException {

    // join subsequent peers with the same label...
    // for each parent
    for (Annotation parent : graph.all(graph.getLayer(layerId).getParentId())) {
      Annotation[] peers = parent.annotations(layerId);
      // go back through all the peers, looking for a peer with the same label that is
      // joined to, or overlaps, this one

      for (int i = peers.length - 2; i >= 0; i--) {
        Annotation preceding = peers[i];
        Annotation following = peers[i + 1];
        boolean mergeAnnotations = false;
        if (preceding.getEnd().equals(following.getStart())) {
          // linked peers
          mergeAnnotations = true;
        } else if (preceding.getEnd().getOffset() != null
                   && following.getStart().getOffset() != null) {
          if (preceding.getEnd().getOffset() >= following.getStart().getOffset()) {
            // temporally continuous peers
            mergeAnnotations = true;
          } else if (getMinimumPauseLength() > 0
                     && preceding.getEnd().getOffset() + getMinimumPauseLength()
                     >= following.getStart().getOffset()) {
            // there is a short enough pause between two peers of the same label
            // but there also must be no intervening annotations
            if (graph.overlappingAnnotations(
                  preceding.getEnd(), following.getStart(), layerId)
                .length == 0) {
              mergeAnnotations = true;
            }
          }
        }
        if (mergeAnnotations) {
          mergeAnnotations(preceding, following);
        }
      } // next preceding peer
      
    } // next parent

    return graph;
  }
   
  /**
   * Moves all of the children of the following peer into the preceding peer, sets the
   * end of the preceding to the end of the following, and marks the following for
   * deletion. 
   * @param preceding The preceding, surviving annotation.
   * @param following The following annotation, which will be deleted.
   */
  public void mergeAnnotations(Annotation preceding, Annotation following) {
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
  } // end of mergeAnnotations()
  
} // end of class Coalescer
