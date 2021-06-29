//
// Copyright 2015-2021 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Vector;
import nzilbb.ag.*;

/**
 * Validates Graph structure.
 * <p> TODO Validating the entire structure takes way too long for long transcripts 
 * (e.g. an hour-long interview with several layers can take five minutes to validate)
 * so validation needs to be smarter.  This could involve checking the changes that have been made
 * to the graph and trying to localize validation accordingly. e.g. if the only change is that 
 * word tags have been added, then only the tag layer (and its children if any) need be validated -
 * no default anchor computation, etc. is necessary.
 * <p> TODO validate Layer.validLabels
 * @author Robert Fromont robert@fromont.net.nz
 */
public class Validator implements GraphTransformer {
   
  /**
   * Fatal errors raised during the last {@link #transform(Graph)}.
   * @see #getErrors()
   * @see #setErrors(Vector)
   */
  protected Vector<String> errors;
  /**
   * Getter for {@link #errors}: Fatal errors raised during the last {@link #transform(Graph)}.
   * @return Fatal errors raised during the last {@link #transform(Graph)}.
   */
  public Vector<String> getErrors() { return errors; }
  /**
   * Setter for {@link #errors}: Fatal errors raised during the last {@link #transform(Graph)}.
   * @param newErrors Fatal errors raised during the last {@link #transform(Graph)}.
   */
  public Validator setErrors(Vector<String> newErrors) { errors = newErrors; return this; }
   
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
  public Validator setDebug(boolean newDebug) { debug = newDebug; return this; }

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
  protected Validator setLog(Vector<String> newLog) { log = newLog; return this; }

  /**
   * Value to assume in the case of Anchors that have no value for "confidence".
   * <p> The default value is {@link Constants#CONFIDENCE_MANUAL} - i.e. anchors with no
   * "confidence" attribute are assumed to be manually aligned (high confidence). 
   * @see #getDefaultAnchorConfidence()
   * @see #setDefaultAnchorConfidence(Integer)
   * @see Constants#CONFIDENCE_MANUAL
   */
  protected int defaultAnchorConfidence = Constants.CONFIDENCE_MANUAL;
  /**
   * Getter for {@link #defaultAnchorConfidence}: Value to assume in the case of Anchors
   * that have no value for "confidence". 
   * @return Value to assume in the case of Anchors that have no value for "confidence".
   */
  public Integer getDefaultAnchorConfidence() { return defaultAnchorConfidence; }
  /**
   * Setter for {@link #defaultAnchorConfidence}: Value to assume in the case of Anchors
   * that have no value for "confidence". 
   * @param newDefaultAnchorConfidence Value to assume in the case of Anchors that have no
   * value for "confidence". 
   */
  public Validator setDefaultAnchorConfidence(Integer newDefaultAnchorConfidence) { defaultAnchorConfidence = newDefaultAnchorConfidence; return this; }
   
  /**
   * The confidence threshold for default anchor offset computation, or null to skip
   * default offset computation. 
   * <p> If this is not null, then anchors with null offset, or with the "confidence"
   * attribute set to equal or below this value, may have their offset set to a default,
   * computed value. The default value is determined by linear inperpolation between
   * parent start/end anchors, or between anchors with higher confidence. 
   * <p> The default value is {@link Constants#CONFIDENCE_DEFAULT}.
   * @see #getDefaultOffsetThreshold()
   * @see #setDefaultOffsetThreshold(Integer)
   * @see #defaultAnchorConfidence
   */
  protected Integer defaultOffsetThreshold = Integer.valueOf(Constants.CONFIDENCE_DEFAULT);
  /**
   * Getter for {@link #defaultOffsetThreshold}: The confidence threshold for default anchor offset computation, or null to skip default offset computation.
   * @return The confidence threshold for default anchor offset computation, or null to skip default offset computation.
   */
  public Integer getDefaultOffsetThreshold() { return defaultOffsetThreshold; }
  /**
   * Setter for {@link #defaultOffsetThreshold}: The confidence threshold for default anchor offset computation, or null to skip default offset computation.
   * @param newDefaultOffsetThreshold The confidence threshold for default anchor offset computation, or null to skip default offset computation.
   */
  public Validator setDefaultOffsetThreshold(Integer newDefaultOffsetThreshold) { defaultOffsetThreshold = newDefaultOffsetThreshold; return this; }
   
  /**
   * Whether to validate all annotations on all layers (true) or perform a 'smart' validation tries to validate only parts of the graph that have actually changed (false - the default).
   * @see #getFullValidation()
   * @see #setFullValidation(boolean)
   */
  protected boolean fullValidation = false;
  /**
   * Getter for {@link #fullValidation}: Whether to validate all annotations on all layers (true) or perform a 'smart' validation tries to validate only parts of the graph that have actually changed (false - the default).
   * @return Whether to validate all annotations on all layers (true) or perform a 'smart' validation tries to validate only parts of the graph that have actually changed (false - the default).
   */
  public boolean getFullValidation() { return fullValidation; }
  /**
   * Setter for {@link #fullValidation}: Whether to validate all annotations on all layers (true) or perform a 'smart' validation tries to validate only parts of the graph that have actually changed (false - the default).
   * @param newFullValidation Whether to validate all annotations on all layers (true) or perform a 'smart' validation tries to validate only parts of the graph that have actually changed (false - the default).
   */
  public Validator setFullValidation(boolean newFullValidation) { fullValidation = newFullValidation; return this; }

  /**
   * Maximum allowed label length, or null (the default) for no limit.
   * @see #getMaxLabelLength()
   * @see #setMaxLabelLength(Integer)
   */
  protected Integer maxLabelLength = null;
  /**
   * Getter for {@link #maxLabelLength}: Maximum allowed label length, or null (the default) for no limit.
   * @return Maximum allowed label length, or null for no limit.
   */
  public Integer getMaxLabelLength() { return maxLabelLength; }
  /**
   * Setter for {@link #maxLabelLength}: Maximum allowed label length, or null for no limit.
   * @param newMaxLabelLength Maximum allowed label length, or null for no limit.
   */
  public Validator setMaxLabelLength(Integer newMaxLabelLength) { maxLabelLength = newMaxLabelLength; return this; }
   
  // Methods:
   
  /**
   * Default constructor.
   */
  public Validator() {
  } // end of constructor

  // GraphTransformer method

  /**
   * Checks the graph structure, and makes changes in order to ensure the graph structure is valid.
   * @param graph The graph to transform.
   * @return The changes introduced by the tranformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph transform(Graph graph) throws TransformationException {
    if (debug) setLog(new Vector<String>());
    setErrors(new Vector<String>());

    boolean needMoreValidation = false;
    if (getFullValidation()) {
      needMoreValidation = true;
    } else {
      // all anchors must be available
      for (Annotation annotation : graph.getAnnotationsById().values()) {
        if (!needMoreValidation) { // haven't found any interesting changes yet
          // we don't validate if the only changes are to tag layers with no children etc.
          if (annotation.getParentId() == null) {
            needMoreValidation = true;
          } else {
            switch(annotation.getChange()) {
              case Destroy:
                // need to validate if a deleted annotation has children
                if (annotation.getAnnotations().size() > 0) {
                  log("Destroyed annotation has children: ", annotation.getId());
                  needMoreValidation = true;
                }
                break;
              case Create:
                // need to validate if it has children, 
                // or it's an aligned layer (check relationships between children)
                if (maxLabelLength != null) {
                  log("New annotation and maxLabelLength set: ", annotation.getId());
                  needMoreValidation = true;
                } else if (annotation.getLayer().getAlignment() != Constants.ALIGNMENT_NONE) {
                  log("New annotation on aligned layer: ", annotation.getId());
                  needMoreValidation = true;
                } else if (annotation.getAnnotations().size() > 0) {
                  log("New annotation has children: ", annotation.getId());
                  needMoreValidation = true;
                }
                break;
              case Update:
                if (maxLabelLength != null && annotation.get("originalLabel") != null) {
                  log("Changed annotation label and maxLabelLength set: ", annotation.getId());
                  needMoreValidation = true;
                } else if (annotation.getLayer().getAlignment() != Constants.ALIGNMENT_NONE) {
                  log("Changed annotation on aligned layer: ", annotation.getId());
                  needMoreValidation = true;
                } else if (annotation.getAnnotations().size() > 0) {
                  log("Changed annotation has children: ", annotation.getId());
                  needMoreValidation = true;
                }
                break;
            } // what type of change it is
          } // parent ID is set
        } // no interesting changes yet
      } // next annotation
      if (!needMoreValidation) { // haven't found any interesting changes yet
        for (Anchor anchor : graph.getAnchors().values()) {
          if (anchor.getChange() != Change.Operation.NoChange) {
            log("Anchor changed: ", anchor.getId());
            needMoreValidation = true;
            break;
          }
        }
      }
    }

    if (!needMoreValidation) {
      log("No changes requiring validation are required.");
      return graph;
    }

    // check label length
    checkLabels(graph);

    // check for reversed anchors
    correctReversedAnchors(graph);

    // reconcile orphans
    reconcileOrphans(graph);

    // validate hierarchy vertically
    validateHierarchy(graph);

    // generate default offsets?
    if (getDefaultOffsetThreshold() != null) {
      boolean defaultOffsets = getFullValidation();
      if (!getFullValidation()) {
        // have any anchors actually changed? if not, don't generate default offsets
        for (Anchor a : graph.getAnchors().values()) {
          if (a.getChange() != Change.Operation.NoChange
              && a.getChange() != Change.Operation.Destroy) {
            defaultOffsets = true;
            break;
          }
        }
      }
      if (defaultOffsets) {
        DefaultOffsetGenerator generator = new DefaultOffsetGenerator(
          getDefaultOffsetThreshold(), getDefaultAnchorConfidence());
        generator.setDebug(getDebug());
        generator.transform(graph);
        getErrors().addAll(generator.getErrors());
        if (generator.getDebug()) getLog().addAll(generator.getLog());
      } else {
        log("Skipping default offset generation");
      }
    } // default offset threshold
    return graph;
  }

  /**
   * Check label length.
   * @param graph
   */
  protected void checkLabels(Graph graph) // TODO check/enforce validLabels {
    if (maxLabelLength != null) {
      for (Annotation annotation : graph.getAnnotationsById().values()) {
        if (annotation.getChange() != Change.Operation.Destroy) {
          if (annotation.getLabel() != null && annotation.getLabel().length() > maxLabelLength) {
            errors.add("Label too long (>" + maxLabelLength + ") for "
                       + annotation.getLayerId()
                       + ": " + logAnnotation(annotation));
            // truncate it so it's valid
            annotation.setLabel(annotation.getLabel().substring(0, maxLabelLength));
          }
        } // not deleting the annotation
      } // next annotation
    } // maxLabelLength set
  } // end of checkLabels()
   
  /**
   * Correction of reversed anchors.
   * <p> In this phase, annotations with reversed anchors (i.e. start.offset after end.offset) 
   * are corrected. Correction means that one of the two anchors has its offset reset to 
   * <code>null</code>, so that it can be (elsewhere) interpolated to a better position. 
   * <p> In order to ensure that the best anchors within out-of-order peers are consistently
   * chosen for resetting, sets of structurally contiguous anchors are checked for offset
   * order correctness, and where an anchor has a lower offset than the last one, some
   * subset of surrounding anchors will be reset to ensure anchor offsets are ordered (or null).
   * <p> Two subsets are compared, the immediately preceding anchors, iterated backwards until
   * the earlier of the conflicting offsets is found, and the immediately following anchors,
   * iterated forward until the later of the conflicting offsets is found.
   * Which of the anchors are reset is decided as follows:
   * <ol>
   *  <li>If preceding subset contains an anchor with higher confidence than the anchor that's
   *      out of order, and the following subset doesn't, then then following subset is reset.</li>
   *  <li>If following subset contains an anchor with higher confidence, and the preceding subset
   *      doesn't, then then preceding subset is reset.</li>
   *  <li>If neither subset contains an anchor with higher confidence, then the smaller subset
   *      is reset (or the following set, if they're the same size).</li>
   *  <li>If both subsets contain an anchor with higher confidence, then a new subset for
   *      resetting is created, containing some anchors from both preceding and following subsets,
   *      starting from the proximal anchors with lowest confidence, and proceeding to higher 
   *      confidence anchors, until the anchors will be in order (or either subsets is
   *      consumed).</li> 
   * </ol>
   * <p> Anchor sets are created by traversing the descendants of the highest-level layers which
   * are aligned.
   * <p> Processing anchors in peer sets catches all reversed annotations, but also
   * catches anchors that are out of order for other reasons (e.g. overlapping peers,
   * children outside the bounds of their children, etc. 
   * @param graph The graph to process.
   */
  protected void correctReversedAnchors(Graph graph) {
    // top down, and aligned layers only
    Vector<Layer> alignedLayersTopDown = new LayerHierarchyTraversal<Vector<Layer>>(
      new Vector<Layer>(), 
      graph.getSchema()) {
        protected void pre(Layer layer) { 
          // aligned layers only
          if (layer.getAlignment() != Constants.ALIGNMENT_NONE) {
            // and only layers that don't have an ancestor already in the list
            for (Layer ancestor : layer.getAncestors()) { 
              if (result.contains(ancestor)) return;
            }
            result.add(layer); 
          }
        } // post = child before parent
      }.getResult();
      
    for (Layer layer : alignedLayersTopDown) {
      // log("Layer: ", layer);
      for (Annotation annotation : graph.all(layer.getId())) {
        if (annotation.getChange() == Change.Operation.Destroy) continue;
        // log("Annotation: ", logAnnotation(annotation));

        // we will build a list of anchors in structure order, including this annotation's
        // anchors and also that anchors of descendants which are aligned and included

        // however, we need direct child layers of this layer to be processed separately
        // e.g. "utterance" and "word" are both peer children of "turn"
        // - we need to gather up anchors of "utterance" (and all its aligned descendants)
        // and separately gather up anchors of "word" (and all its aligned descendants)
        // this ensures that the order of "turn" descendants isn't messed up by having
        // all children on one child layer followed by all children on another child layer

        for (Layer childLayer : layer.getChildren().values()) {
          // log("Child layer: ", childLayer);
          LinkedHashSet<Anchor> anchors = new LinkedHashSet<Anchor>();
          if (annotation.getStart() != null) 
            anchors.add(annotation.getStart());
          for (Annotation child : annotation.getAnnotations(childLayer.getId())) {
            if (child.getChange() == Change.Operation.Destroy) continue;
            // add anchors of aligned descendants
            LayerTraversal<LinkedHashSet<Anchor>> traversal 
              = new LayerTraversal<LinkedHashSet<Anchor>>(anchors, child) {
                  protected void pre(Annotation annotation) {
                    Layer layer = annotation.getLayer();
                    if (layer.getAlignment() != Constants.ALIGNMENT_NONE
                        && layer.getParentIncludes()
                        && !layer.getPeersOverlap()) {
                      // log("Visiting: ", annotation);
                      if (annotation.getStart() != null) 
                        getResult().add(annotation.getStart());
                    }
                  }
                  protected void post(Annotation annotation) {
                    Layer layer = annotation.getLayer();
                    if (layer.getAlignment() != Constants.ALIGNMENT_NONE
                        && layer.getParentIncludes()
                        && !layer.getPeersOverlap()) {
                      if (annotation.getEnd() != null) 
                        getResult().add(annotation.getEnd());
                    }
                  }
			
                };
          } // next child on this layer
          if (annotation.getEnd() != null) 
            anchors.add(annotation.getEnd());

          Stack<Anchor> lastOffsetAnchors = new Stack<Anchor>();
          lastOffsetAnchors.push(new Anchor(null, Double.MIN_VALUE));
          Double start = annotation.getStart()==null?null:annotation.getStart().getOffsetMin();
          if (start != null) {
            lastOffsetAnchors.peek().setOffset(start);
          }
          // having guaranteed that anchors aren't repeated by using a Set,
          // we now use LinkedList so we can iterate backwards as well as forwards
          LinkedList<Anchor> anchorList = new LinkedList<Anchor>(anchors);
          ListIterator<Anchor> anchorIterator = anchorList.listIterator();
          while (anchorIterator.hasNext()) {
            Anchor anchor = anchorIterator.next();
            // log("Checking: ", anchor, " against ", lastOffsetAnchors.peek());
            if (anchor.getOffset() != null 
                && anchor.getOffset() < lastOffsetAnchors.peek().getOffset()) { // out of order
              log("Anchors out of order: ", lastOffsetAnchors.peek(), " followed by ", anchor);
		     
              // compile two lists of reset candidates - prior anchors and following anchors
              // the shortest list will lose, and its anchors will be reset
              // the iterator is scanned until the
		     
              int anchorConfidence = Utility.getConfidence(anchor, defaultAnchorConfidence);
		     
              // go backwards until an anchor less that ours is found
              Vector<Anchor> priorAnchors = new Vector<Anchor>();
              boolean higherConfidencePrior = false;
              anchorIterator.previous(); // consume this anchor
              while (anchorIterator.hasPrevious()) {
                Anchor other = anchorIterator.previous();
                if (other.getOffset() != null) {
                  if (other.getOffset() < anchor.getOffset()) { // gone far enough back
                    break;
                  }
                  int otherConfidence = Utility.getConfidence(other, defaultAnchorConfidence);
                  if (otherConfidence > anchorConfidence) {
                    // there's a prior anchor with higher confidence than anchor
                    // and also a higher offset, so we won't be resetting prior anchors
                    if (!higherConfidencePrior) {
                      log("Higher confidence prior anchor: ", other, 
                          " confidence: ", Utility.getConfidence(other, -1));
                    }
                    higherConfidencePrior = true;
                  }
                }
                priorAnchors.add(other);
              } // previous anchor
		     
              // return the iterator to the correct position
              while (!anchorIterator.next().equals(anchor)) ;
		     
              // go forwards until an anchor greater than the lastOffset is found
              Vector<Anchor> followingAnchors = new Vector<Anchor>();
              boolean higherConfidenceFollowing = false;
              followingAnchors.add(anchor);
              while (anchorIterator.hasNext()) {
                Anchor other = anchorIterator.next();
                if (other.getOffset() != null) {
                  if (other.getOffset() > lastOffsetAnchors.peek().getOffset()) {
                    // gone far enough back
                    break;
                  }
                  int otherConfidence = Utility.getConfidence(other, defaultAnchorConfidence);
                  if (otherConfidence > anchorConfidence) {
                    // there's a following anchor with higher confidence than anchor
                    // and also a lower offset that lastOffset, 
                    // so we won't be resetting following anchors
                    if (!higherConfidenceFollowing) {
                      log("Higher confidence following anchor: ", other, 
                          " confidence: ", Utility.getConfidence(other, -1));
                    }
                    higherConfidenceFollowing = true;
                  }
                }
                followingAnchors.add(other);
              } // next anchor
		     
              Vector<Anchor> anchorsToReset = null;
              if (higherConfidencePrior) {
                if (!higherConfidenceFollowing) {
                  log("Resetting following anchors");
                  anchorsToReset = followingAnchors;
                } else {
                  log("Higher confidence anchors in both directions");
                  // there are higher confidence anchors in both directons, 
                  // so nibble away at the lists consuming the lowest confidence anchors
                  anchorsToReset = new Vector<Anchor>();
                  Vector<Anchor> currentList = priorAnchors;
                  int currentConfidence = anchorConfidence;
                  // loop while both lists have some elements
                  while (priorAnchors.size() > 0 && followingAnchors.size() > 0
                         // and the following anchors have lower offsets than the prior ones
                         && (priorAnchors.elementAt(0).getOffset() == null
                             || followingAnchors.elementAt(0).getOffset() == null
                             || priorAnchors.elementAt(0).getOffset() 
                             >= followingAnchors.elementAt(0).getOffset())) {
                    // do we need to change lists?
                    if (currentList.elementAt(0).getOffset() != null
                        && Utility.getConfidence(
                          currentList.elementAt(0), defaultAnchorConfidence) > currentConfidence) {
                      // can't pop off the current list, so pick the list that starts with
                      // the lowest confidence  
                      if (Utility.getConfidence(priorAnchors.elementAt(0), defaultAnchorConfidence)
                          < Utility.getConfidence(
                            followingAnchors.elementAt(0), defaultAnchorConfidence))
                      {
                        currentList = priorAnchors;
                      } else {
                        currentList = followingAnchors;
                      }
                      currentConfidence = Utility.getConfidence(
                        currentList.elementAt(0), defaultAnchorConfidence);
                    }
                    // consume an anchor
                    anchorsToReset.add(currentList.remove(0));
                  } // loop
                }
              } else if (higherConfidenceFollowing) {
                // (we already know that !higherConfidencePrior)
                log("Resetting prior anchors");
                anchorsToReset = priorAnchors;
              } else { // neither higherConfidencePrior nor higherConfidenceFollowing
                // so reset the shortest list
                if (priorAnchors.size() > followingAnchors.size()) {
                  log("Resetting following anchors");
                  anchorsToReset = followingAnchors;
                } else {
                  log("Resetting prior anchors");
                  anchorsToReset = priorAnchors;
                }
              }
		     
              if (anchorsToReset != null) {
                // reset anchors
                for (Anchor other : anchorsToReset) {
                  log("Resetting: ", other);
                  other
                    .setOffset(null)
                    .setConfidence(Constants.CONFIDENCE_NONE);
                }			
              }
		     
              // there's no point in returning the iterator to iterate over reset anchors...
              if (anchorsToReset != followingAnchors) {
                // return the iterator to the correct position
                while (!anchorIterator.previous().equals(anchor)) ;
                anchorIterator.next(); // consume this anchor			
              }
		     
            } // out of order
            // update the offset we're up to
            if (anchor.getOffset() != null) {
              lastOffsetAnchors.push(anchor);
            }
            while (lastOffsetAnchors.peek().getOffset() == null) {
              // offset has since been reset, fall back to prior anchor
              lastOffsetAnchors.pop();
            }
          } // next anchor
        } // next child layer
      } // next annotation
    } // next layer
  } // end of correctReversedAnchors()

  /**
   * Orphan reconciliation. 
   * <p> In this phase orphaned children are detected and deleted (if automatically generated)
   * or parents found for them (if manually annotated).
   * This fixes children with deleted parents (e.g. words of merged turns, segments of
   * deleted words), and t-included children who have been moved to another parent
   * (e.g. words of split turns). 
   * <p> Checks that children have the correct parents, for layers where
   * {@link Layer#getParentIncludes()} = true.
   * <p> This method looks for children whose parents are not valid, and attempts to 
   * identify a better parent.
   * <p> For layers that descend from "turn", a new parent with the same speaker is preferred, 
   * but changing speaker may happen if no other parent is available.
   * <p> Fully t-including parents are preferred, but one that includes only the child's
   * mid-point will be used if no other parent is available. 
   * <p>This method does not guarantee that all orphans will be assigned new parents 
   * - e.g. if no parent candidate even partially overlaps with the child, no parent can
   * be assigned.   
   * for {@link Layer#getParentIncludes()} layers, in cases like these, if there was
   * previously a parent assigned, this remains unchanged.
   * @param graph The graph to transform.
   */
  protected void reconcileOrphans(Graph graph) {
    Vector<Layer> layersTopDown = new LayerHierarchyTraversal<Vector<Layer>>(new Vector<Layer>(), graph.getSchema()) {
        protected void pre(Layer layer) { result.add(layer); } // pre = parent before child
      }.getResult();
    for (Layer childLayer : layersTopDown) {
      if (childLayer.getParent() == null) continue; // top level layer or incomplete hierarchy
      if (childLayer.getParentId().equals("transcript")) continue; // top level layer
      // log("reconcile orphans: ", parentLayer, "/", childLayer);
      reconcileOrphans(childLayer, graph);
    } // next layer
  }
  /**
   * Orphan reconciliation. 
   * <p> In this phase orphaned children are detected and deleted (if automatically generated)
   * or parents found for them (if manually annotated).
   * This fixes children with deleted parents (e.g. words of merged turns, segments of
   * deleted words), and t-included children who have been moved to another parent
   * (e.g. words of split turns). 
   * <p> Checks that children have the correct parents, for layers where
   * {@link Layer#getParentIncludes()} = true.
   * <p> This method looks for children whose parents are not valid, and attempts to 
   * identify a better parent.
   * <p> For layers that descend from "turn", a new parent with the same speaker is preferred, 
   * but changing speaker may happen if no other parent is available.
   * <p> Fully t-including parents are preferred, but one that includes only the child's
   * mid-point will be used if no other parent is available. 
   * <p> This method does not guarantee that all orphans will be assigned new parents 
   * - e.g. if no parent candidate even partially overlaps with the child, no parent can
   * be assigned. for {@link Layer#getParentIncludes()} layers, in cases like these, if there was
   * previously a parent assigned, this remains unchanged.
   * @param childLayer The child layer to check.
   * @param graph The graph to transform.
   */
  protected void reconcileOrphans(Layer childLayer, Graph graph) {
    if (childLayer.getParentIncludes()) {
      // whole/part relationship, e.g. turn/word, word/syllable, word/segment
      Layer parentLayer = childLayer.getParent();
      // TODO not turn/utterance for some reason?

      // we cannot use graph.all() because that assumes correct parent/child layer hierarchy
      // which is pare of what we're validating.
      // instead, we traverse all annotations in the graph to tease out the children, 
      // regardless of what their parent happens to be.
      String childLayerId = childLayer.getId();
      for (Annotation child : graph.getAnnotationsById().values()) {
        if (child.getChange() == Change.Operation.Destroy) continue; // ignore deleted
        if (childLayerId.equals(child.getLayerId())) {
          Annotation oldParent = child.getParent();
	       
          if (!getFullValidation()) {
            if (child.getChange() == Change.Operation.NoChange
                && oldParent != null && oldParent.getChange() == Change.Operation.NoChange) {
              // nothing changed, so skip it
              continue;
            }
          }
	       
          String parentChangeReason = null;
          if (oldParent == null) parentChangeReason = "missing";
          else if (oldParent.getChange() == Change.Operation.Destroy) parentChangeReason = "deleted";
          // or not temporally included in the parent (excluding tags)
          else if (childLayer.getAlignment() != Constants.ALIGNMENT_NONE 
                   && child.getAnchored() && oldParent.getAnchored()) {
            if(!oldParent.includesMidpointOf(child)
               // special case: instants at the end of their parent are ok
               && (child.getDuration() > 0 
                   || !child.getStart().getOffset().equals(oldParent.getEnd().getOffset()))
               // && anChild.getCounterpart() != null)) // and child is in edited graph TODO
              ) {
              // if it's linked by an anchor, it's still a valid parent
              if (!child.getStartId().equals(oldParent.getStartId())
                  && !child.getEndId().equals(oldParent.getEndId())) {
                parentChangeReason = "not including";
              }
            }
          }
          // or the parent is on the wrong layer
          if (oldParent != null && !oldParent.getLayerId().equals(parentLayer.getId())) parentChangeReason = "wrong layer";
          if (parentChangeReason != null) { // parent is gone or wrong
            if (Utility.getConfidence(child, Constants.CONFIDENCE_MANUAL)
                <= Constants.CONFIDENCE_AUTOMATIC) {
              // automatically generated, so can be deleted
              log("Deleting ", child, " - ", parentLayer.getId(),
                  " (", oldParent, " ", parentChangeReason, ")");
              child.destroy();
            } else if (child.getLayer() != null) { // cannot delete child, need a new parent
              log("Need new parent for ", child, 
                  " (", oldParent, " ", parentChangeReason, ")");
              Annotation newParent = findBestParent(child);
              if (newParent != null) {
                log("New parent for ", child, " is ", " ", newParent, 
                    " (", oldParent, " ", parentChangeReason, ")");
                child.setParent(newParent);
              } else {
                errors.add("No new parent available for " + logAnnotation(child) 
                           + " ("+child.getLayerId()+")"
                           + " but " + parentLayer.getId() 
                           + " " + logAnnotation(oldParent) 
                           + " " +  parentChangeReason + ")");
              }
            } // cannot delete child, need a new parent
          } // parent is gone or wrong
        } // is on child layer
      } // next annotation
    } // peers and not peersOverlap and parentIncludes
  }
   
  /**
   * Finds the best parent annotation for the given child annotation, giving priority to parents
   * with the same speaker, with a common (non-graph) ancestor, and t-including or at least
   * midpoint-t-including parent candidates.  A candidate is identified, but no changes are made
   * to annotation.  null may be returned, if no suitable parent can be identified.
   * @param child The child annotation.
   * @return The best parent annotation available, or null if none can be found.
   */
  protected Annotation findBestParent(Annotation child) {
    // look for a new parent
    String parentLayerId = child.getLayer().getParentId();
    LinkedHashSet<Annotation> candidates = new LinkedHashSet<Annotation>();
    Annotation newParent = null;
    Annotation nearestCandidate = null; // in case no t-including candidates are found
    // first try tags - this is fast to compute, and a pretty good indication of parenthood
    candidates.addAll(Arrays.asList(child.tagsOn(parentLayerId)));
    if (candidates.size() == 0) {
      // next try including annotations
      candidates.addAll(Arrays.asList(child.includingAnnotationsOn(parentLayerId)));
    }
    if (candidates.size() == 0) {
      // next try midpoint-including annotations
      candidates.addAll(Arrays.asList(child.midpointIncludingAnnotationsOn(parentLayerId)));
    }
    if (candidates.size() == 0) {
      // finally try linked annotations
	 
      // first start-to-start or end-to-end linkage, which might not necessarily include
      if (child.getStart() != null) 
        candidates.addAll(child.getStart().startOf(parentLayerId));
      if (child.getEnd() != null) 
        candidates.addAll(child.getEnd().endOf(parentLayerId));

      // then preceding/following linkage
      if (child.getStart() != null) 
        candidates.addAll(child.getStart().endOf(parentLayerId));
      if (child.getEnd() != null) 
        candidates.addAll(child.getEnd().startOf(parentLayerId));

      // remove deleted annotations 
      Iterator<Annotation> i = candidates.iterator();
      while (i.hasNext()) if (i.next().getChange() == Change.Operation.Destroy) i.remove();

      if (candidates.size() == 0 && child.getParent() != null) {
        // or neighboring annotations
        Annotation neighbor = child.getParent().getPrevious();
        if (neighbor != null) candidates.add(neighbor);

        neighbor = child.getParent().getNext();
        if (neighbor != null) candidates.add(neighbor);
 
        // remove deleted annotations 
        i = candidates.iterator();
        while (i.hasNext()) if (i.next().getChange() == Change.Operation.Destroy) i.remove();
      }
    }
    for (Annotation candidate : candidates) {
      if (child.getParent() != null && child.getParent() != child.getGraph()
          && child.getParent().getParent() != null && child.getParent().getParent() != child.getGraph()
          && candidate.getParent() != null && candidate.getParent() != child.getGraph()
          && child.getParent().getParent() == candidate.getParent()) {
        // candidate parent is the same as the child's current grandparent (which is not the whole graph)
        newParent = candidate;
        break; // we have a winner
      }
      if (child.getFirstCommonAncestor(candidate) != child.getGraph()) {
        // this maximises the possibility of the new parent being the same speaker, if relevant
        newParent = candidate;
        // keep looking - an equal child.grandparent = candidate.parent would be better
      }
      assert child.getAnchored()
        : "child.getAnchored() " + child.getLayerId() + ":" + child + " " + child.getStart()
        + " - " + child.getEnd();
      assert candidate.getAnchored()
        : "candidate.getAnchored() " + candidate + " " + candidate.getStart()
        + " - " + candidate.getEnd();
      if (nearestCandidate == null
          || (nearestCandidate.getAnchored()
              && child.distance(candidate) < child.distance(nearestCandidate))) {
        nearestCandidate = candidate;
      }
    } // next candidate
    if (newParent == null) newParent = nearestCandidate;
    return newParent;
  } // end of findBestParent()

  /**
   * Validate hierarchy vertically.
   * <p> Checks for inconsistencies between parents and children, which respect to how
   * their relationship is defined. 
   * @param graph The graph to transform.
   */
  protected void validateHierarchy(Graph graph) {
    Vector<Layer> layersBottomUp = new LayerHierarchyTraversal<Vector<Layer>>(
      new Vector<Layer>(), 
      new Comparator<Layer>() { // reverse default child order
        public int compare(Layer l1, Layer l2) { 
          return -LayerHierarchyTraversal.defaultComparator.compare(l1,l2); 
        } },
      graph.getSchema()) {
        protected void post(Layer layer) { result.add(layer); } // post = child before parent
      }.getResult();

    // we do two passes - first aligned layers, then tag layers, so that widening/narrowing trickles down
    Vector<Layer> layers = new Vector<Layer>();
    for (Layer l : layersBottomUp) if (l.getAlignment() != Constants.ALIGNMENT_NONE) layers.add(l);
    for (Layer l : layersBottomUp) if (l.getAlignment() == Constants.ALIGNMENT_NONE) layers.add(l);
    // for each layer, bottom up, but tag layers last
    for (Layer childLayer : layers) {
      if (childLayer.getParent() == null) continue; // top level layer or incomplete hierarchy
      validateHierarchy(childLayer, graph);
    }
  }

  /**
   * Validate hierarchy vertically.
   * <p> Checks for inconsistencies between parents and children, which respect to how
   * their relationship is defined. 
   * @param childLayer The child layer to validate.
   * @param graph The graph to transform.
   */
  protected void validateHierarchy(Layer childLayer, Graph graph) {
    if (!getFullValidation()) {
      boolean childChanged = false;
      for (Annotation child : graph.all(childLayer.getId())) {
        if (child.getChange() != Change.Operation.NoChange) {
          childChanged = true;
          break;
        }
      } // next child
      if (!childChanged) { // nothing changed, so don't bother with this
        return;
      }
    }

    Layer parentLayer = childLayer.getParent();
    // log("validate hierarchy: ", parentLayer, "/", childLayer);

    // ensure extra peers are deleted
    if (!childLayer.getPeers()) {
      // for each parent
      for (Annotation parent : graph.all(childLayer.getParentId())) { 
        if (parent.getChange() == Change.Operation.Destroy) continue; // ignore deleted annotations
	    
        int childCount = 0;
        for (Annotation child : parent.getAnnotations(childLayer.getId())) {
          if (child.getChange() == Change.Operation.Destroy) continue; // ignore deleted annotations
          if (++childCount > 1) {
            log("Deleting extra child: ", child);
            child.destroy();
            log("Deleted extra child: ", child.getChange());
          }
        } // next child
      } // next parent
    } // there are no peers allowed
	 
      // ensure ordinals are set in chronological order
    if (childLayer.getPeers() 
        // but only if chronological order is important
        // (don't check tags)
        && childLayer.getAlignment() != Constants.ALIGNMENT_NONE
        // and child are t-included inside their parent
        // (external dependencies might be in some other order)
        && childLayer.getParentIncludes()
      ) {
      // for each parent
      for (Annotation parent : graph.all(childLayer.getParentId())) {
        if (parent.getChange() == Change.Operation.Destroy) continue; // ignore deleted annotations
	    
        // we don't use AnnotationsByAnchor because it's too slow
        PeerAnnotationsByAnchor children = new PeerAnnotationsByAnchor(parent, childLayer.getId());
	    	    
        // ensure the ordinals are in chronological order, and that they are set
        int iOrdinal = parent.ordinalMinimum(childLayer.getId());
        log("Parent ", parent);
        for (Annotation child : children) {
          log("Child ", child);
          if (iOrdinal != child.getOrdinal()) {
            // set the attribute
            log("Updated ordinal for ", child, " from ", child.getOrdinal(), " to ", iOrdinal,
                " (Parent: ", parent, ")");
            child.setOrdinal(iOrdinal);
          }
          iOrdinal++; // next ordinal
        } // next child	       
      } // next parent
    } // there are peers
      
    // check anchors
      
    // for each parent
    for (Annotation parent : graph.all(childLayer.getParentId())) { 
      if (parent.getChange() == Change.Operation.Destroy) continue; // ignore deleted annotations
      // log("Parent: ", parent);
      double dLastOffset = 0.0;
      Annotation lastOffsetChild = null;
      Annotation lastChild = null;
      Annotation lastAnchoredChild = null;
	 
      for (Annotation child : parent.getAnnotations(childLayer.getId())) {
        if (child.getChange() == Change.Operation.Destroy) continue; // ignore deleted annotations
        if (!parent.getId().equals(child.getParentId())) continue; // no longer the parent
        if (child.getStart() == null) continue;
        // log(" child: ", child);
        if (!childLayer.getPeersOverlap() 
            // ignore tag layers, whose anchors will follow their parents
            && childLayer.getAlignment() != Constants.ALIGNMENT_NONE) {
          // check start anchor is not shared on the same layer (e.g. simultaneous speech)
          for (Annotation parallel : child.getStart().startOf(child.getLayerId())) {
            if (parallel.getChange() == Change.Operation.Destroy) continue; // ignore deleted
            if (parallel == child) continue; // ignore ourselves
            if (parallel.getStartId().equals(child.getStartId())) // not already split off {
              // if we get here, there's an annotation that has our start anchor.
              // this is invalid whether it belongs to the same parent or not, so we
              // create our own anchor 
              log("Split start anchor of ", child, " from ", parallel);
              Anchor newStart = new Anchor(child.getStart());
              newStart.setOffset(child.getStart().getOffset());
              graph.addAnchor(newStart);
              changeStartWithRelatedAnnotations(child, newStart);
              break; // we only need to find one of these
            } // not already split
          } // next possibly parallel annotation

          if (child.getEnd() == null) continue;
	       
          if (child.getStart().getOffset() != null) {
            if (child.getEnd().getOffset() != null) {
              // check end anchor offset before checking start anchor because any changes
              // here have to be further checked below 
              if (child.getEnd().getOffset().doubleValue() 
                  < child.getStart().getOffset().doubleValue()) {
                // 	correctSwappedAnchors(child); // TODO
              }
            } // end offset is set
		  
            if (child.getStart().getOffset() < dLastOffset) { // overlap
              if (lastChild == null) {
                errors.add("CANNOT CORRECT SEQUENTIALITY OF " 
                           + logAnnotation(child) + " last offset: " + dLastOffset);
              } else { // there is a prior annotation
                Annotation affectedGrandchild = null;
			
                // can we narrow both children without affecting their grandchildren?
                double dMidPoint = child.getStart().getOffset()
                  + ((dLastOffset - child.getStart().getOffset()) / 2);
                // check it wouldn't create an invalid lastchild...
                if (dMidPoint <= lastAnchoredChild.getStart().getOffset()) {
                  dMidPoint = lastAnchoredChild.getStart().getOffset()
                    + ((dLastOffset - lastAnchoredChild.getStart().getOffset()) / 2);
                }
                Annotation earliestChildDescendant = child.getEarliestDescendant();
                Annotation latestLastChildDescendant = lastAnchoredChild.getLatestDescendant();
                if (earliestChildDescendant != null 
                    && earliestChildDescendant.getStart().getOffset() < dMidPoint) {
                  affectedGrandchild = earliestChildDescendant;
                }
                if (affectedGrandchild == null) {
                  if (latestLastChildDescendant != null 
                      && latestLastChildDescendant.getEnd().getOffset() > dMidPoint) {
                    affectedGrandchild = latestLastChildDescendant;
                  }
                } // affectedGrandchild == null
			
                if (affectedGrandchild == null) {
                  // we can narrow the child to directly follow the last child
                  if (childLayer.getSaturated()) {
                    // it's ok for them to share anchors
                    changeStartWithRelatedAnnotations(child, lastAnchoredChild.getEnd());
                    lastAnchoredChild.getEnd()
                      .setOffset(dMidPoint)
                      .setConfidence(Constants.CONFIDENCE_DEFAULT);
                    if (child.getEnd().getOffset() == null 
                        || child.getEnd().getOffset() <= child.getStart().getOffset()) {
                      child.getEnd()
                        .setOffset(dLastOffset)
                        .setConfidence(Constants.CONFIDENCE_DEFAULT);
                    }
                  } else {
                    // they don't necessary share anchors, and it may be they they *shouldn't*
                    // e.g. consecutive words in a turn might be separated by an utterance boundary
                    lastAnchoredChild.getEnd()
                      .setOffset(dMidPoint)
                      .setConfidence(Constants.CONFIDENCE_DEFAULT);
                    child.getStart()
                      .setOffset(dMidPoint)
                      .setConfidence(Constants.CONFIDENCE_DEFAULT);
                  }
                  if (child.getEnd().getOffset() == null 
                      || child.getEnd().getOffset() <= child.getStart().getOffset()) {
                    child.getEnd()
                      .setOffset(dLastOffset)
                      .setConfidence(Constants.CONFIDENCE_DEFAULT);
                  }
                  log("Overlapping annotations: ", lastAnchoredChild, 
                      " and ", child, " (narrowed both to ", dMidPoint, ")");
                } else { // can't narrow both
                  // would narrowing this child affect grandchildren?
                  // i.e. does the child have any children that start before the end of the last child?
                  affectedGrandchild = null;
                  if (earliestChildDescendant != null 
                      && earliestChildDescendant.getStart().getOffset()
                      < lastAnchoredChild.getEnd().getOffset()) {
                    affectedGrandchild = earliestChildDescendant;
                  }
                  if (affectedGrandchild == null
                      // and we're not creating an annotation of non-positive length
                      && lastAnchoredChild.getEnd().getOffset() != null
                      && child.getEnd().getOffset() != null
                      && lastAnchoredChild.getEnd().getOffset() < child.getEnd().getOffset()) {
                    // we can narrow the child to directly follow the last child
                    log("Overlapping annotations: ", lastAnchoredChild,
                        " and ", child, " (narrowed second)");
                    // update parallel annotations as well
                    if (childLayer.getSaturated()) {
                      // it's ok for them to share anchors
                      changeStartWithRelatedAnnotations(child, lastAnchoredChild.getEnd());
                    } else {
                      // they don't necessary share anchors, and it may be they they *shouldn't*
                      // e.g. consecutive words in a turn might be separated by an utterance boundary
                      child.getStart()
                        .setOffset(lastAnchoredChild.getEnd().getOffset())
                        .setConfidence(Constants.CONFIDENCE_DEFAULT); // TODO should the status be copied too? should it be getDeltaOffset or getOffset
                    }
                  } else {
                    // would narrowing the last child affect grandchildren?
                    // i.e. does the last child have any children that end after the end
                    // of this child? 
                    affectedGrandchild = null;
                    if (latestLastChildDescendant != null
                        && latestLastChildDescendant.getEnd().getOffset()
                        > child.getStart().getOffset()) {
                      affectedGrandchild = latestLastChildDescendant;
                    }
                    if (affectedGrandchild == null
                        // and we're not creating an annotation of non-positive length
                        && child.getStart().getOffset()
                        > lastAnchoredChild.getStart().getOffset()) {
                      // we can narrow the last child to directly follow this child
                      log("Overlapping annotations: ", lastAnchoredChild, 
                          " and ", child, " (narrowed first)");
                      if (childLayer.getSaturated()) {
                        // it's ok for them to share anchors
                        changeEndWithRelatedAnnotations(lastAnchoredChild, child.getStart());
                      } else {
                        // they don't necessary share anchors, and it may be they they *shouldn't*
                        // e.g. consecutive words in a turn might be separated by an utterance boundary
                        lastAnchoredChild.getEnd()
                          .setOffset(child.getStart().getOffset())
                          .setConfidence(Constants.CONFIDENCE_DEFAULT); // TODO copy the status too?  revert to getOffset?
                      }
                    } else {
                      try {
                        log("Overlapping annotations: ", lastAnchoredChild,
                            " and ", child, " (tease both apart)");
                        teaseApart(lastAnchoredChild, child, childLayer.getSaturated());
                      } catch (Exception x) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        x.printStackTrace(pw);
                        try {
                          sw.close();
                          pw.close();
                        } catch(Exception exception) {}
                                    
                        errors.add("Overlapping annotations: "
                                   + logAnnotation(lastAnchoredChild) 
                                   + " and " + logAnnotation(child) 
                                   + " (CANNOT CORRECT: teaseApart failed: "
                                   + x.toString() + ")\n" + sw);
                      }
                    } // can't narrow first
                  } // can't narrow second
                } // can't narrow both
              } // there is a prior annotation
            } else if (childLayer.getSaturated()) {
              if (lastChild == null) { // first child
                if (child.getStart() != null && parent.getStart() != null) {
                  if (parent.getStart().getOffset() == null
                      || child.getStart().getOffset() > parent.getStart().getOffset()) {
                    // narrow parent
                    String sOriginal = logAnnotation(parent);
                    HashSet<String> layersNotToMove = new HashSet<String>();
                    // this prevents the previous parent end (which may have already been 
                    // moved) from coming with us
                    layersNotToMove.add(parentLayer.getId()); 
                    if (childLayer.getPeersOverlap()) layersNotToMove.add(child.getLayerId());
                    changeStartWithRelatedAnnotations(parent, child.getStart(), layersNotToMove);
                    log("Narrowed ", sOriginal, " -> ", parent,
                        " to remove gap before ", child);
                  } else if (!child.getStartId().equals(parent.getStartId())) {
                    changeStartWithRelatedAnnotations(child, parent.getStart());
                    log("Changed ", child, " to share start anchor with ", parent);
                  }
                }
              } else { // subsequent children
                if (child.getStart() != null &&
                    child.getStart().getOffset() != dLastOffset) {
                  // widen last child
                  changeEndWithRelatedAnnotations(
                    lastChild, child.getStart(),  
                    !childLayer.getPeersOverlap()?child.getLayerId():null);
                  log("Widened ", lastChild, " to close gap before ", child);
                }
              }
            } // Saturated
          } // start offset is set
	       
          // check end anchor is not shared on the same layer (e.g. simultaneous speech)
          for (Annotation parallel : child.getEnd().endOf(child.getLayerId())) {
            if (parallel.getChange() == Change.Operation.Destroy) continue; // ignore deleted
            if (parallel == child) continue; // ignore ourselves
            if (!parallel.getEndId().equals(parallel.getEndId())) // not already split off {
              // if we get here, there's an annotation that has our end anchor.
              // this is invalid whether it belongs to the same parent or not, so we
              // create our own anchor 
              Anchor newEnd = new Anchor(child.getEnd());
              graph.addAnchor(newEnd);
              changeEndWithRelatedAnnotations(child, newEnd);
              log("Split end anchor of ", child, " from ", parallel);
              break; // we only need to find one of these
            } // not already split
          } // next possibly parallel annotation
	       
          if (child.getEnd().getOffset() != null) {
            dLastOffset = child.getEnd().getOffset();
          }
        } // !peersOverlap
	    
        // we tolerate instantaneous annotations - HTK produces them - so don't check for it
	    
        // this after any child changes above, to avoid unnecessary widening
        if (childLayer.getParentIncludes() && child.getStart() != null) {
          if (childLayer.getAlignment() == Constants.ALIGNMENT_NONE) { // tag 
            // child must share anchors with parent
            if (child.getStartId() == null 
                || !child.getStartId().equals(parent.getStartId())) {
              log("Sharing start anchor of tag ", child, " with ", parent);
              child.setStart(parent.getStart());
            }
            if (child.getEndId() == null
                || !child.getEndId().equals(parent.getEndId())) {
              log("Sharing end anchor of tag ", child, " with ", parent);
              child.setEnd(parent.getEnd());
            }
		  
            // (although Annotation itself now ensures that unaligned children share anchors 
            //  with their parents, by assigning them directly in Annotation.setParent()
            //  and Annotation.setStart()/setEnd())
          } else { // t-included parts, not a tag
            if (parent.getStart() != null
                && child.getStart().getOffset() != null
                && parent.getStart().getOffset() != null)
            {
              // check start anchors
              if (parent.getStart().getOffset() > child.getStart().getOffset()) {
                // TODO check whether the parent is/would be an instant after this?
                String sOriginal = logAnnotation(parent);
                // widen parent
                Anchor newAnchor = child.getStart();
                if (!childLayer.getSaturated()) {
                  // sparse children - don't share anchors with them
                  newAnchor = new Anchor(child.getStart());
                  graph.addAnchor(newAnchor);
                }
                changeStartWithRelatedAnnotations(
                  parent, newAnchor, 
                  // don't change other children if it's a sequential layer
                  !childLayer.getPeersOverlap()?child.getLayerId():null);
                log("Widened ", sOriginal, " -> ", parent,
                    " to ", child.getStart().getOffset(), " to include child ", child);
              } // start anchor
            } // start anchors have offsets
		  
            if (parent.getEnd() != null
                && child.getEnd().getOffset() != null
                && parent.getEnd().getOffset() != null) {
              // check end anchors
              if (parent.getEnd().getOffset() < child.getEnd().getOffset()) {
                // widen parent
                String sOriginal = logAnnotation(parent);
			
                Anchor newAnchor = child.getEnd();
                if (!childLayer.getSaturated()) {
                  // sparse children - don't share anchors with them
                  newAnchor = new Anchor(child.getEnd());
                  graph.addAnchor(newAnchor);
                }
                changeEndWithRelatedAnnotations(
                  parent, newAnchor, 
                  // don't change other children if it's a sequential layer
                  !childLayer.getPeersOverlap()?child.getLayerId():null);
			
                log("Widened ", sOriginal, " -> ", parent,
                    " to ", child.getEnd().getOffset(), " to include child ", child);
              } // end anchor
            } // end anchors have offsets
          } // t-included parts, not a tag
        } // TIncluded
	    
        if (child.getAnchored()) {
          lastAnchoredChild = child;
        }
        lastChild = child;
	    
      } // next child
	 
      if (lastChild != null) { // there were some children
        if (childLayer.getSaturated()) { // check last anchor
          if (lastChild.getEnd() != null
              && lastChild.getEnd().getOffset() != null) {
            if (parent.getEnd() != null) {
              if(parent.getEnd().getOffset() == null
                 || parent.getEnd().getOffset() > lastChild.getEnd().getOffset()) {
                // narrow parent
                String sOriginal = logAnnotation(parent);
                Anchor originalEndAnchor = parent.getEnd();
                changeEndWithRelatedAnnotations(
                  parent, lastChild.getEnd(), lastChild.getLayerId());
                log("Narrowed ", sOriginal, " -> ", parent, " to close gap after ", lastChild);
                // check for shared following annotations on the parent's layer
                for (Annotation followingParent : originalEndAnchor.startOf(parentLayer.getId())) {
                  log("Resetting start of following annotation too ", followingParent);
                  changeStartWithRelatedAnnotations(followingParent, lastChild.getEnd());
                } // next shared following annotations on the parent's layer
              } else if (!parent.getEndId().equals(lastChild.getEndId())) {
                lastChild.setEndId(parent.getEndId());
                log("Changed ", lastChild, " to share end anchor with ", parent);
              }
            } // parent end is set
          } // end anchors are set
        } // Saturated
      } // there were children
	 
    } // next parent	 
  }

  /**
   * Sets the Start Anchor of the given annotation, and also the start anchors of related
   * annotations that start in the same place. 
   * @param annotation The annotation to change the start anchor of.
   * @param newStartAnchor The new start anchor.
   */
  public void changeStartWithRelatedAnnotations(Annotation annotation, Anchor newStartAnchor) {
    changeStartWithRelatedAnnotations(annotation, newStartAnchor, new HashSet<String>());
  }
  /**
   * Sets the Start Anchor of the given annotation, and also the start anchors of related annotations that start in the same place.
   * @param annotation The annotation whose start anchor will be changed.
   * @param newStartAnchor The new start anchor.
   * @param layerIdToExclude A layer to exclude when updating related annotations.
   */
  public void changeStartWithRelatedAnnotations(
    Annotation annotation, Anchor newStartAnchor, String layerIdToExclude) {
    HashSet<String> exclude = new HashSet<String>();
    if (layerIdToExclude != null) exclude.add(layerIdToExclude);
    changeStartWithRelatedAnnotations(annotation, newStartAnchor, exclude);
  }
  /**
   * Sets the StartAnchor of the given annotation, and also the start anchors of related
   * annotations that start in the same place. 
   * @param annotation The annotation whose start anchor will be changed.
   * @param newStartAnchor The new start anchor.
   * @param layerIdsToExclude Layers to exclude when updating related annotations.
   */
  public void changeStartWithRelatedAnnotations(
    Annotation annotation, Anchor newStartAnchor, Set<String> layerIdsToExclude) {
    log("changeStartWithRelatedAnnotations ", annotation, " to ", newStartAnchor,
        (layerIdsToExclude.size() > 0?" excluding layers ":""), layerIdsToExclude);

    Anchor aOriginalStart = annotation.getStart();
    Anchor aOriginalEnd = annotation.getEnd();      
    annotation.setStart(newStartAnchor);
    if (aOriginalStart == aOriginalEnd) {
      log(annotation, " is instantaneous, changing both anchors.");
      annotation.setEnd(newStartAnchor);
    }
      
    Layer layer = annotation.getGraph().getLayer(annotation.getLayerId());
      
    // change parallel annotations
    for (Annotation anOther : aOriginalStart.getStartingAnnotations()) {
      if (anOther == annotation) continue;
      if (anOther.getLayerId() == annotation.getLayerId()) continue;
      if (layerIdsToExclude.contains(anOther.getLayerId())) continue;
      // has it already been changed?
      assert anOther != null : "anOther != null";
      assert anOther.getStartId() != null : "anOther.getStartId() != null - " + anOther.getLayerId() + ":" + anOther.getLabel();
      if (!anOther.getStartId().equals(aOriginalStart.getId())) continue;
      // do they have a relationship that would actually preclude sharing?
      Layer otherLayer = annotation.getGraph().getLayer(anOther.getLayerId());
      if (layer != null && otherLayer != null) {
        if (layer.getParentId() != null
            && layer.getParentId().equals(otherLayer.getId())) { // other is parent layer to this
          if (!layer.getSaturated()) continue; // sparse

          // this belongs to another parent
          if (!anOther.getId().equals(annotation.getParentId())) continue;
        } else if (otherLayer.getParentId() != null
                 && otherLayer.getParentId().equals(layer.getId())) {
          // this is parent layer to other
          if (!otherLayer.getSaturated()) continue; // sparse
	       
          // this belongs to another parent
          if (!annotation.getId().equals(anOther.getParentId())) continue;
        }
      }
      // TODO // do they both have counterparts?
      // if (annotation.getCounterpart() != null && anOther.getCounterpart() != null)
      // { // and if so, do the counterparts share anchors?
      //    if (annotation.getCounterpart().getStartAnchor() != anOther.getCounterpart().getStartAnchor())
      //    {
      //       // SHOULD they share? Is there a saturated relationship between the layers (annotation in the child layer)
      //       if (relationship == null
      // 	   || relationship.getSubordinateLayerId() != annotation.getLayerId()
      // 	   || relationship.getSaturation() != Labbcat.Saturation.Saturated)
      //       {
      // 	  report.log("Not changing end of related annotation " + logAnnotation(anOther) + " as counterparts don't share");
      // 	  continue; // if not shared in the other graph, not shared here
      //       }
      //    }
      // }
      boolean bInstant = anOther.getInstantaneous();
      log("Changing start", (bInstant?" and end":""), " of related annotation ", 
          anOther, " to ", newStartAnchor);
      anOther.setStart(newStartAnchor);
      if (bInstant) {
        anOther.setEnd(newStartAnchor);
      }
    } // next parallel anchor starting here

    if (!layerIdsToExclude.contains(annotation.getLayerId())) {
      // also change end anchor of annotations on the same layer
      layerIdsToExclude.add(annotation.getLayerId()); // prevents infinite recursion
      Vector<Annotation> vRelatedAnnotations = new Vector<Annotation>();
      vRelatedAnnotations.addAll(aOriginalStart.endOf(annotation.getLayerId()));
      for (Annotation anPrevious : vRelatedAnnotations) {
        if (anPrevious.getChange() == Change.Operation.Destroy) continue;
        // only if it really still follows
        if (!anPrevious.getEndId().equals(aOriginalStart.getId())) continue;

        // TODO // do they both have counterparts?
        // if (annotation.getCounterpart() != null && anPrevious.getCounterpart() != null)
        // { // and if so, do the counterparts share anchors?
        //    if (annotation.getCounterpart().getStartAnchor() != anPrevious.getCounterpart().getEndAnchor())
        //    {
        // 	  report.log("Not changing end of related annotation " + logAnnotation(anPrevious) + " as counterparts don't share");
        // 	  continue; // if not shared in the other graph, not shared here
        //    }
        // }
        if (!anPrevious.getStartId().equals(anPrevious.getEndId())
            && newStartAnchor.getId().equals(anPrevious.getStartId())) {
          log("Not changing end of related annotation ", anPrevious,
              " to avoid creating new instant");
          continue;
        }
        if (anPrevious.getParentId() == null
            || !anPrevious.getParentId().equals(annotation.getParentId())) {
          log("Not changing end of related annotation ", anPrevious, " - different parents");
          continue;
        }
        log("Changing end of previous linked annotation ", anPrevious, " to ", newStartAnchor);
        changeEndWithRelatedAnnotations(anPrevious, newStartAnchor, layerIdsToExclude);
      } // next ending annotation
    } // not excluding annotation's own layer
  } // end of changeStartWithRelatedAnnotations()

  /**
   * Sets the End Anchor of the given annotation, and also the end anchors of related
   * annotations that end in the same place. 
   * @param annotation The annotation whose end anchor should be changed.
   * @param newEndAnchor The new end anchor.
   */
  public void changeEndWithRelatedAnnotations(Annotation annotation, Anchor newEndAnchor) {
    changeEndWithRelatedAnnotations(annotation, newEndAnchor, new HashSet<String>());
  }
  /**
   * Sets the End Anchor of the given annotation, and also the end anchors of related
   * annotations that end in the same place. 
   * @param annotation The annotation whose end anchor should be changed.
   * @param newEndAnchor The new end anchor.
   * @param layerIdToExclude A layer to exclude when updating related annotations.
   */
  public void changeEndWithRelatedAnnotations(
    Annotation annotation, Anchor newEndAnchor, String layerIdToExclude) {
    HashSet<String> exclude = new HashSet<String>();
    if (layerIdToExclude != null) exclude.add(layerIdToExclude);
    changeEndWithRelatedAnnotations(annotation, newEndAnchor, exclude);
  }
  /**
   * Sets the End Anchor of the given annotation, and also the end anchors of related
   * annotations that end in the same place. 
   * @param annotation The annotation whose end anchor should be changed.
   * @param newEndAnchor The new end anchor.
   * @param layerIdsToExclude Layers to exclude when updating related annotations.
   */
  public void changeEndWithRelatedAnnotations(
    Annotation annotation, Anchor newEndAnchor, Set<String> layerIdsToExclude) {
    log("changeEndWithRelatedAnnotations ", annotation, " to ", newEndAnchor, 
        (layerIdsToExclude.size() > 0?" excluding layers ":""), layerIdsToExclude);
    Anchor aOriginalEnd = annotation.getEnd();
    Anchor aOriginalStart = annotation.getStart();
    annotation.setEnd(newEndAnchor);
    if (aOriginalStart == aOriginalEnd) {
      log(annotation, " is instantaneous, changing both anchors.");
      annotation.setStart(newEndAnchor);
    }

    Layer layer = annotation.getGraph().getLayer(annotation.getLayerId());

    for (Annotation anOther : aOriginalEnd.getEndingAnnotations())
    {
      if (anOther == annotation) continue;
      if (anOther.getLayerId().equals(annotation.getLayerId())) continue;
      if (layerIdsToExclude.contains(anOther.getLayerId())) continue;
      // has it already been changed?
      if (!anOther.getEndId().equals(aOriginalEnd.getId())) continue;

      // do they have a relationship that would actually preclude sharing?
      Layer otherLayer = annotation.getGraph().getLayer(anOther.getLayerId());
      if (layer != null && otherLayer != null) {
        if (layer.getParentId().equals(otherLayer.getId())) { // other is parent layer to this
          if (!layer.getSaturated()) continue; // sparse

          // this belongs to another parent
          if (!anOther.getId().equals(annotation.getParentId())) continue;
        } else if (otherLayer.getParentId().equals(layer.getId())) {
          // this is parent layer to other
          if (!otherLayer.getSaturated()) continue; // sparse
	       
          // this belongs to another parent
          if (!annotation.getId().equals(anOther.getParentId())) continue;
        }
      }
      // TODO // do they both have counterparts?
      // if (annotation.getCounterpart() != null && anOther.getCounterpart() != null)
      // { // and if so, do the counterparts share anchors?
      //    if (annotation.getCounterpart().getEndAnchor() != anOther.getCounterpart().getEndAnchor())
      //    {
      //       // SHOULD they share? Is there a saturated relationship between the layers (annotation in the child layer)
      //       if (relationship == null
      // 	   || relationship.getSubordinateLayerId() != annotation.getLayerId()
      // 	   || relationship.getSaturation() != Labbcat.Saturation.Saturated)
      //       {
      // 	  report.log("Not changing end of related annotation " + logAnnotation(anOther) + " as counterparts don't share");
      // 	  continue; // if not shared in the other graph, not shared here
      //       }
      //    }
      // }
      boolean bInstant = anOther.getInstantaneous();
      log("Changing end", (bInstant?" and start":""), " of related annotation ", anOther,
          " to ", newEndAnchor);
      anOther.setEnd(newEndAnchor);
      if (bInstant) {
        anOther.setStart(newEndAnchor);
      }
    } // next parallel anchor starting here
    if (!layerIdsToExclude.contains(annotation.getLayerId())) {
      // also change start anchor of annotations on the same layer
      layerIdsToExclude.add(annotation.getLayerId()); // prevents infinite recursion
      Vector<Annotation> vRelatedAnnotations = new Vector<Annotation>();
      vRelatedAnnotations.addAll(aOriginalEnd.startOf(annotation.getLayerId()));
      // vRelatedAnnotations.addAll(aOriginalEnd.getDeltaStartAnnotationsLayer(annotation.getLayerId()));
      if (vRelatedAnnotations.size() > 0) {
        int iNonDeletedCount = 0;
        for (Annotation anNext : vRelatedAnnotations) {
          if (anNext.getChange() == Change.Operation.Destroy) continue; // ignore deleted ones
          iNonDeletedCount++;
          // only if it really still follows
          if (!anNext.getStartId().equals(aOriginalEnd.getId())) continue;
          // TODO // do they both have counterparts?
          // if (annotation.getCounterpart() != null && anNext.getCounterpart() != null)
          // { // and if so, do the counterparts share anchors?
          // 	  if (annotation.getCounterpart().getEndAnchor() != anNext.getCounterpart().getStartAnchor())
          // 	  {
          // 	     report.log("Not changing start of related annotation " + logAnnotation(anNext) + " as counterparts don't share");
          // 	     continue; // if not shared in the other graph, not shared here
          // 	  }
          // }
          if (!anNext.getStartId().equals(anNext.getEndId())
              && newEndAnchor.getId().equals(anNext.getEndId())) {
            log("Not changing end of related annotation ", anNext, " to avoid creating new instant");
            continue; // if not shared in the other graph, not shared here
          }
          log("Changing start of next linked annotation ", anNext, " to ", newEndAnchor);
          changeStartWithRelatedAnnotations(anNext, newEndAnchor, layerIdsToExclude);
        } // next starting annotation
	    
        if (iNonDeletedCount == 0) { // all the 'next' annotations on the same layer are deleted
          // ensure that annotations that start here on *other* layers come with us
          // find one related annotation on another layer
          vRelatedAnnotations.clear();
          vRelatedAnnotations.addAll(aOriginalEnd.getStartingAnnotations());
          for (Annotation anNext : vRelatedAnnotations) {
            if (layerIdsToExclude.contains(anNext.getLayerId())) continue;
            log("Next has been deleted, using ", anNext, " to bring starting annotations too");
            changeStartWithRelatedAnnotations(anNext, newEndAnchor, layerIdsToExclude);
            break; // one should be sufficient to bring the rest along
          } // next starting annotation	       
        }  // all the 'next' annotations on the same layer are deleted
      } // there are 'next' starting annotations
    }
  } // end of changeStartWithRelatedAnnotations()

  /**
   * Corrects two child-bearing annotations that incorrectly overlap.
   * @param anFirst First annotation.
   * @param anLast Last annotation.
   * @param bShareAnchors true if <var>anFirst.end</var> should be the same anchor as
   * <var>anLast.start</var> 
   */
  protected void teaseApart(Annotation anFirst, Annotation anLast, boolean bShareAnchors) {
    // there are four anchors involved, the earliest, the second earliest, the second
    // latest, and the latest we create a fifth halfway between the second earliest and
    // second latest, which will be the resulting boundary  
    // anFirst.startAnchor will be earliest, anLast.endAnchor will be latest
    // anFirst's children that occur after second earliest will be squeezed back
    // anLast's children that occur before the second latest will be squeezed forward
    // this way, the difference in boundaries is minimal, and affects the minimal number
    // of children 
    // ...however, the earliest anchor is forced to be an original anFirst anchor, because
    // this method is called after checking sequentiality of anFirst, so we ensure that
    // nothing is widened to be before it. 
      
    // get a list of parallel annotations to anFirst/anLast that should be relinked
    Vector<Annotation> vParallelStartToFirst = new Vector<Annotation>();
    for (Annotation an : anFirst.getStart().getStartingAnnotations()) {
      if (an.getChange() == Change.Operation.Destroy) continue;
      if (an != anFirst && an != anLast && an.getStartId().equals(anFirst.getStartId())) {
        vParallelStartToFirst.add(an);
        log(an, " teasing start with ", anFirst);
      }
    }
    Vector<Annotation> vParallelEndToFirst = new Vector<Annotation>();
    for (Annotation an : anFirst.getEnd().getEndingAnnotations()) {
      if (an.getChange() == Change.Operation.Destroy) continue;
      if (an != anFirst && an != anLast && an.getEndId().equals(anFirst.getEndId())) {
        vParallelEndToFirst.add(an);
        log(an, " teasing end with ", anFirst);
      }
    }
    Vector<Annotation> vParallelStartToLast = new Vector<Annotation>();
    for (Annotation an : anLast.getStart().getStartingAnnotations()) {
      if (an.getChange() == Change.Operation.Destroy) continue;
      if (an != anFirst && an != anLast && an.getStartId().equals(anLast.getStartId())) {
        vParallelStartToLast.add(an);
        log(an, " teasing start with ", anLast);
      }
    }
    Vector<Annotation> vParallelEndToLast = new Vector<Annotation>();
    for (Annotation an : anLast.getEnd().getEndingAnnotations()) {
      if (an.getChange() == Change.Operation.Destroy) continue;
      if (an != anFirst && an != anLast && an.getEndId().equals(anLast.getEndId())) {
        vParallelEndToLast.add(an);
        log(an, " teasing end with ", anLast);
      }
    }
      
    // get an ordered list of the four anchors
    TreeSet<Anchor> anchors = new TreeSet<Anchor>(
      new Comparator<Anchor>() {
        public int compare(Anchor a1, Anchor a2) {
          // allows the same element to be entered twice
          if (a1.getOffset() == null || a2.getOffset() == null) {
            if (!a1.getId().equals(a2.getId())) return a1.getId().compareTo(a2.getId());
          }
          else if (a1.getOffset() < a2.getOffset()) return -1;
          return 1;
        }
      });
    anchors.add(anFirst.getStart());
    anchors.add(anFirst.getEnd());
    if (anLast.getStart().getOffset() >= anFirst.getStart().getOffset()) {
      anchors.add(anLast.getStart());
    } else {
      anchors.add(anFirst.getStart());
    }
    if (anLast.getEnd().getOffset() != null
        && anFirst.getStart().getOffset() != null
        && anLast.getEnd().getOffset() >= anFirst.getStart().getOffset()) {
      anchors.add(anLast.getEnd());
    } else {
      anchors.add(anFirst.getEnd());
    }      
      
    // create the bounding anchors and ensure the bounds are reflected in the correct anchors
    Iterator<Anchor> i = anchors.iterator();
    Anchor a = i.next();
    Anchor aEarliest = new Anchor(a);
    anFirst.getGraph().addAnchor(aEarliest);
    a = i.next();
    double dAlmostEarliest = a.getOffset();
    a = i.next();
    double dAlmostLatest = a.getOffset();
    a = i.next();
    Anchor aLatest = new Anchor(a);
    anFirst.getGraph().addAnchor(aLatest);
 
    // find the midpoint and set the middle bounds of each annotation to that offset
    double dMidPoint = dAlmostEarliest + ((dAlmostLatest - dAlmostEarliest) / 2);
      
    Anchor aMiddle1 = new Anchor();
    aMiddle1.setOffset(dMidPoint);
    aMiddle1.setConfidence(Constants.CONFIDENCE_DEFAULT);
    anFirst.getGraph().addAnchor(aMiddle1);
    Anchor aMiddle2 = aMiddle1;
    if (!bShareAnchors) {
      aMiddle2 = new Anchor(aMiddle1);
      anFirst.getGraph().addAnchor(aMiddle2);
    }

    // report.log("Linking between ", aEarliest, " and ", aMiddle, " and ", aLatest);

    changeStartWithRelatedAnnotations(anFirst, aEarliest);
    for (Annotation an : vParallelStartToFirst) {
      if (an.getStartId().equals(an.getEndId())) {
        // instant
        an.setEnd(aEarliest);
      }
      an.setStart(aEarliest);
    }
    changeEndWithRelatedAnnotations(anFirst, aMiddle1);
    for (Annotation an : vParallelEndToFirst) {
      if (an.getStartId().equals(an.getEndId())) {
        // instant
        an.setStart(aMiddle1);
      }
      an.setEnd(aMiddle1);
    }
    changeStartWithRelatedAnnotations(anLast, aMiddle2);
    for (Annotation an : vParallelStartToLast) {
      if (an.getStartId().equals(an.getEndId())) {
        // instant
        an.setEnd(aMiddle2);
      }
      an.setStart(aMiddle2);
    }
    changeEndWithRelatedAnnotations(anLast, aLatest);
    for (Annotation an : vParallelEndToLast) {
      if (an.getStartId().equals(an.getEndId())) {
        // instant
        an.setStart(aLatest);
      }
      an.setEnd(aLatest);
    }

    log("Teased apart ", anFirst, " and ", anLast);

    // reset child anchors
    resetChildAnchorsAfter(anFirst, dAlmostEarliest);
    resetChildAnchorsBefore(anLast, dAlmostLatest);
  } // end of teaseApart()
   
  /**
   * Resets the anchors of the children of the given
   * annotation. After this method, all children on a given layer
   * will be s-included (i.e. chained from the start anchor to the
   * end anchor), and all anchors that previously had an offset at or before the threshold 
   * will have the offset set to null and the confidence set to
   * {@link Constants#CONFIDENCE_NONE}. All changed anchors are new anchors.
   * @param parent The parent whose children should be changed.
   * @param threshold The offset before which anchors will be reset.
   */
  protected void resetChildAnchorsBefore(Annotation parent, double threshold) {
    log("resetChildAnchorsBefore ", parent, " ", threshold);
    for (String childLayerId : parent.getAnnotations().keySet()) {
      // ignore non-interval layers
      Layer childLayer = parent.getGraph().getLayer(childLayerId);
      if (childLayer == null) continue;
      if (childLayer.getAlignment() != Constants.ALIGNMENT_INTERVAL) continue;
      // ignore relationships that aren't saturated
      if (!childLayer.getSaturated()) continue;
      // ignore relationships that are not t-included
      if (!childLayer.getParentIncludes()) continue;

      Annotation lastChild = null;
      for (Annotation child : parent.getAnnotations(childLayerId)) {
        // TODO do we need to check it's still really a child?
        if (lastChild == null) {
          if (!child.getStartId().equals(parent.getStartId())) {
            // first child - start anchor is shared with parent
            log("set first child ", child, " to share start with parent");
            child.setStartId(parent.getStartId());
          }
        } else { // not the first child
          // first deal with end of last child
          if (lastChild.getEnd().getOffset() != null
              && lastChild.getEnd().getOffset() <= threshold) {
            // check child.start.offset first to see if nulling can be avoided
            // if the current child start is not shared with the last child end
            if (!child.getStartId().equals(lastChild.getEndId())
                // and it's not null
                && child.getStart().getOffset() != null
                // and it's over the threshold
                && child.getStart().getOffset() > threshold) {
              // set the lastChild.end to be the same as child.start
              log("sharing end of last child ", lastChild, " with this child ", child);
              lastChild.setEndId(child.getStartId());
            } else { 
              // null lastChild's end
              log("new end for last child ", child);
              Anchor newAnchor = new Anchor();
              newAnchor.setConfidence(Constants.CONFIDENCE_NONE);
              parent.getGraph().addAnchor(newAnchor);
              lastChild.setEndId(newAnchor.getId());
            } // null lastChild's end 

            // now that the child has been processed, do its children if appropriate
            if (!lastChild.getAnchored() || lastChild.includesOffset(threshold)) {
              resetChildAnchorsBefore(lastChild, threshold);
            }
          } // lastChild's end is out of range

          // now deal with start of this child, which must be the same as the last child
          if (!child.getStartId().equals(lastChild.getEndId())) {
            log("set start of child ", child, " to be end of last child ", lastChild);
            child.setStartId(lastChild.getEndId());
          }
        } // not the first child
        lastChild = child;
      } // next child

      // now check the end of the last child
      if (lastChild != null) { // there are children
        // the last one must share the end anchor with its parent
        if (!lastChild.getEndId().equals(parent.getEndId())) {
          log("set last child ", lastChild, " to share end with parent");
          lastChild.setStartId(parent.getEndId());
        }
        // now that the child has been processed, do its children if appropriate
        if (!lastChild.getAnchored() || lastChild.includesOffset(threshold)) {
          resetChildAnchorsBefore(lastChild, threshold);
        }
      }

    } // next child layer
  } // end of resetChildAnchorsBefore()

  /**
   * Resets the anchors of the children of the given
   * annotation. After this method, all children on a given layer
   * will be s-included (i.e. chained from the start anchor to the
   * end anchor), and all anchors that previously had an offset at or before the threshold 
   * will have the offset set to null and the confidence set to
   * @param parent The parent whose children should be changed.
   * @param threshold The offset theshold after which anchors will be reset.
   */
  protected void resetChildAnchorsAfter(Annotation parent, double threshold) {
    log("resetChildAnchorsAfter ", parent, " ", threshold);
    for (String childLayerId : parent.getAnnotations().keySet()) {
      // ignore non-interval layers
      Layer childLayer = parent.getGraph().getLayer(childLayerId);
      if (childLayer == null) continue;
      if (childLayer.getAlignment() != Constants.ALIGNMENT_INTERVAL) continue;
      // ignore relationships that aren't saturated
      if (!childLayer.getSaturated()) continue;
      // ignore relationships that are not t-included
      if (!childLayer.getParentIncludes()) continue;

      Annotation lastChild = null;
      for (Annotation child : parent.getAnnotations(childLayerId)) {
        // TODO do we need to check it's still really a child?
        if (lastChild == null) {
          if (!child.getStartId().equals(parent.getStartId())) {	       
            log("set first child ", child, " to share start with parent");
            // first child - start anchor is shared with parent
            child.setStartId(parent.getStartId());
          }
        } else { // not the first child
          // first deal with end of last child
          if (lastChild.getEnd().getOffset() != null
              && lastChild.getEnd().getOffset() >= threshold) {
            // check child.start.offset first to see if nulling can be avoided
            // if the current child start is not shared with the last child end
            if (!child.getStartId().equals(lastChild.getEndId())
                // and it's not null
                && child.getStart().getOffset() != null
                // and it's over the threshold
                && child.getStart().getOffset() < threshold) {
              // set the lastChild.end to be the same as child.start
              log("sharing end of last child ", lastChild, " with this child ", child);
              lastChild.setEndId(child.getStartId());
            } else { 
              // null lastChild's end
              log("new end for last child ", child);
              Anchor newAnchor = new Anchor();
              newAnchor.setConfidence(Constants.CONFIDENCE_NONE);
              parent.getGraph().addAnchor(newAnchor);
              lastChild.setEndId(newAnchor.getId());
            } // null lastChild's end 

            // now that the child has been processed, do its children if appropriate
            if (!lastChild.getAnchored() || lastChild.includesOffset(threshold)) {
              resetChildAnchorsBefore(lastChild, threshold);
            }
          } // lastChild's end is out of range

          // now deal with start of this child, which must be the same as the last child
          if (!child.getStartId().equals(lastChild.getEndId())) {
            log("set start of child ", child, " to be end of last child ", lastChild);
            child.setStartId(lastChild.getEndId());
          }
        } // not the first child
        lastChild = child;
      } // next child

      // now check the end of the last child
      if (lastChild != null) { // there are children
        // the last one must share the end anchor with its parent
        if (!lastChild.getEndId().equals(parent.getEndId())) {
          log("set last child ", lastChild, " to share end with parent");
          lastChild.setStartId(parent.getEndId());
        }
        // now that the child has been processed, do its children if appropriate
        if (!lastChild.getAnchored() || lastChild.includesOffset(threshold)) {
          resetChildAnchorsAfter(lastChild, threshold);
        }
      }
    } // next child layer
  } // end of resetChildAnchorsAfter()

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
      System.out.println(s.toString());
    }
  } // end of log()

} // end of class Validator
