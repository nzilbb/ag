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
import java.util.TreeSet;
import java.util.Vector;
import java.util.stream.Collectors;
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
   * Whether a log of messages should be kept for reporting.
   * @see #getDebug()
   * @see #setDebug(boolean)
   * @see #getLog()
   * @see #log(Object...)
   */
  protected boolean debug = false;
  /**
   * Getter for {@link #debug}: Whether a log of messages should be kept for reporting.
   * @return Whether a log of messages should be kept for reporting.
   * @see #getLog()
   * @see #log(Object...)
   */
  public boolean getDebug() { return debug; }
  /**
   * Setter for {@link #debug}: Whether a log of messages should be kept for reporting.
   * @param newDebug Whether a log of messages should be kept for reporting.
   * @see #getLog()
   * @see #log(Object...)
   */
  public Coalescer setDebug(boolean newDebug) { debug = newDebug; return this; }

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
      
  /**
   * Messages for debugging.
   * @see #getLog()
   * @see #setLog(Vector)
   */
  protected Vector<String> log;
  /**
   * Getter for {@link #log}: Messages for debugging.
   * @return Messages for debugging.
   */
  public Vector<String> getLog() { return log; }
  /**
   * Setter for {@link #log}: Messages for debugging.
   * @param newLog Messages for debugging.
   */
  protected Coalescer setLog(Vector<String> newLog) { log = newLog; return this; }

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
    if (debug) setLog(new Vector<String>());

    // join subsequent peers with the same label...
    // for each parent
    for (Annotation parent : graph.all(graph.getLayer(layerId).getParentId())) {
      if (parent.getChange() == Change.Operation.Destroy) continue;
      TreeSet<Annotation> annotations = new TreeSet<Annotation>(
        // ensure we get children in anchor order
        new AnnotationComparatorByAnchor());
      annotations.addAll(parent.getAnnotations(layerId));
      
      Annotation[] peers = annotations.stream()
        .filter(peer -> peer.getChange() != Change.Operation.Destroy)
        .collect(Collectors.toList())
        .toArray(new Annotation[0]);
      // go back through all the peers, looking for a peer with the same label that is
      // joined to, or overlaps, this one

      for (int i = peers.length - 2; i >= 0; i--) {
        Annotation preceding = peers[i];
        Annotation following = peers[i + 1];
        if (preceding.getEnd() == null || following.getStart() == null) continue;
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
        if (following.getEnd() == null) { // presumably a fragment
          mergeAnnotations = false;
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
    log("Merge ", preceding, " with ", following);
    
    Anchor originalPrecedingEnd = preceding.getEnd();
    // set anchor
    if (preceding.getEnd().getOffset() == null
        || following.getEnd().getOffset() == null
        || preceding.getEnd().getOffset() < following.getEnd().getOffset()) {
      preceding.setEnd(following.getEnd());
    }
    
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

      // saturated child layers can't have gaps
      if (preceding.getGraph().getLayer().getSaturated()) {
        // children linked to the original preceding end need re-linking to the following start
        Vector<Annotation> endingPrecedingChildren = // avoid ConcurrentModificationException
          new Vector<Annotation>(originalPrecedingEnd.endOf(childLayerId));
        for (Annotation endingPrecedingChild : endingPrecedingChildren) {
          // but only our own children
          if (endingPrecedingChild.getParentId().equals(preceding.getId())) {
            endingPrecedingChild.setEnd(following.getStart());
            
            // and any annotations on other layers that are also linked
            // (create a new collection to avoid ConcurrentModificationException)
            new Vector<Annotation>(
              originalPrecedingEnd.getEndingAnnotations()).stream()            
              .filter(ending -> !ending.getLayerId().equals(layerId))
              .filter(ending -> !ending.getLayerId().equals(childLayerId))
              .forEach(endingOtherLayer -> {
                  endingOtherLayer.setEnd(following.getStart());
              });
          } // own child
        } // next child ending here
      } // saturated child layer
    } // next child layer

    following.destroy();
  } // end of mergeAnnotations()
  
  /**
   * A representation of the given annotation for logging purposes.
   * @param annotation The annotation to log.
   * @return A representation of the given annotation for loggin purposes.
   */
  protected String logAnnotation(Annotation annotation) {
    if (annotation == null) return "[null]";
    return "[" + annotation.getId() + "]" + annotation.getOrdinal() + "#" + annotation.getLabel() + "("+annotation.getStart()+"-"+annotation.getEnd()+")";
  } // end of logAnnotation()

  /**
   * A representation of the given anchor for logging purposes.
   * @param anchor The anchor to log.
   * @return A representation of the given anchor for logging purposes.
   */
  protected String logAnchor(Anchor anchor) {
    if (anchor == null) return "[null]";
    return "[" + anchor.getId() + "]" + anchor.getOffset();
  } // end of logAnnotation()
   
  /**
   * Logs a debugging message.
   * @param messages The objects making up the log message.
   */
  protected void log(Object ... messages) {
    if (debug) { // we only interpret arguments to log() if we're actually debugging...
      StringBuilder s = new StringBuilder();
      for (Object m : messages) {
        if (m == null) {
          s.append("[null]");
        } else if (m instanceof Annotation) {
          Annotation annotation = (Annotation)m;
          s.append("[").append(annotation.getId()).append("]")
            .append(annotation.getOrdinal()).append("#")
            .append(annotation.getLabel())
            .append("(").append(annotation.getStart())
            .append("-").append(annotation.getEnd()).append(")");
        } else if (m instanceof Anchor) {
          Anchor anchor = (Anchor)m;
          s.append("[").append(anchor.getId()).append("]").append(anchor.getOffset());
        } else {
          s.append(m.toString());
        }
      }	 
      log.add(s.toString());
      System.err.println(s.toString());
    }
  } // end of log()
} // end of class Coalescer
