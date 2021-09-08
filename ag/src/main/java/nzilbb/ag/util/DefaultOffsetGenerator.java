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

import java.util.Arrays;
import java.util.Queue;
import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Vector;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import nzilbb.ag.*;
import nzilbb.ag.cli.Transform;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;

/**
 * Generates default anchor offsets. These are computed using linear interpolation between
 * certain offsets. What counts as <var>certain</var> depends on how 
 * {@link #defaultOffsetThreshold} is set. 
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Generates default anchor offsets for JSON-encoded annotation graphs from stdin")
public class DefaultOffsetGenerator extends Transform implements GraphTransformer {
  
  // Attributes:
   
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
  public DefaultOffsetGenerator setErrors(Vector<String> newErrors) { errors = newErrors; return this; }
   
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
  public DefaultOffsetGenerator setDebug(boolean newDebug) { debug = newDebug; return this; }

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
  protected DefaultOffsetGenerator setLog(Vector<String> newLog) { log = newLog; return this; }

  /**
   * Value to assume in the case of Anchors that have no value for "confidence".
   * <p>The default value is {@link Constants#CONFIDENCE_MANUAL} - i.e. anchors with no
   * "confidence" attribute are assumed to be manually aligned (high confidence). This
   * means that the default behaviour on a graph that does not include "confidence"
   * values on anchors is to only assign offsets to anchors that have none. 
   * @see #getDefaultAnchorConfidence()
   * @see #setDefaultAnchorConfidence(int)
   * @see Constants#CONFIDENCE_MANUAL
   */
  protected int defaultAnchorConfidence = Constants.CONFIDENCE_MANUAL;
  /**
   * Getter for {@link #defaultAnchorConfidence}: Value to assume in the case of Anchors
   * that have no value for "confidence". 
   * @return Value to assume in the case of Anchors that have no value for "confidence".
   */
  public int getDefaultAnchorConfidence() { return defaultAnchorConfidence; }
  /**
   * Setter for {@link #defaultAnchorConfidence}: Value to assume in the case of Anchors
   * that have no value for "confidence". 
   * @param newDefaultAnchorConfidence Value to assume in the case of Anchors that have
   * no value for "confidence". 
   */
  @Switch("Value to assume in the case of Anchors with no explicit confidence")
  public DefaultOffsetGenerator setDefaultAnchorConfidence(int newDefaultAnchorConfidence) { defaultAnchorConfidence = newDefaultAnchorConfidence; return this; }
   
  /**
   * The confidence threshold for default anchor offset computation.
   * <p>Anchors with null offset, with no "confidence" attribute, or with the
   * "confidence" attribute set to equal or below this value, may have their offset set
   * to a default, computed value. Generally, the default value is determined by linear
   * interpolation between parent start/end anchors, or between anchors with higher
   * confidence. 
   * <p>The default value is {@link Constants#CONFIDENCE_DEFAULT}.
   * @see #getDefaultOffsetThreshold()
   * @see #setDefaultOffsetThreshold(int)
   */
  protected int defaultOffsetThreshold = Constants.CONFIDENCE_DEFAULT;
  /**
   * Getter for {@link #defaultOffsetThreshold}: The confidence threshold for default
   * anchor offset computation, or  null to skip default offset computation.
   * @return The confidence threshold for default anchor offset computation, or null to
   * skip default offset computation. 
   */
  public int getDefaultOffsetThreshold() { return defaultOffsetThreshold; }
  /**
   * Setter for {@link #defaultOffsetThreshold}: The confidence threshold for default
   * anchor offset computation, or null to skip default offset computation. 
   * @param newDefaultOffsetThreshold The confidence threshold for default anchor offset
   * computation, or null to skip default offset computation. 
   */
  @Switch("The confidence threshold for default anchor offset computation")
  public DefaultOffsetGenerator setDefaultOffsetThreshold(int newDefaultOffsetThreshold) { defaultOffsetThreshold = newDefaultOffsetThreshold; return this; }
   
  /**
   * Value to set for <var>confidence</var> for anchors that have their offsets changed
   * by this transformer. 
   * <p>The default value is {@link Constants#CONFIDENCE_DEFAULT}.
   * @see #getConfidence()
   * @see #setConfidence(int)
   */
  protected int confidence = Constants.CONFIDENCE_DEFAULT;
  /**
   * Getter for {@link #confidence}: Value to set for <var>confidence</var> for anchors
   * that have their offsets changed by this transformer. 
   * @return Value to set for <var>confidence</var> for anchors that have their offsets
   * changed by this transformer. 
   */
  public int getConfidence() { return confidence; }
  /**
   * Setter for {@link #confidence}: Value to set for <var>confidence</var> for anchors
   * that have their offsets changed by this transformer. 
   * @param newConfidence Value to set for <var>confidence</var> for anchors that have
   * their offsets changed by this transformer. 
   */
  @Switch("Value for confidence for anchors that have their offsets changed")
  public DefaultOffsetGenerator setConfidence(int newConfidence) { confidence = newConfidence; return this; }

  // Methods:
   
  /**
   * Default constructor.
   */
  public DefaultOffsetGenerator() {
  } // end of constructor

  /**
   * Constructor with attributes.
   * @param defaultOffsetThreshold The confidence threshold for default anchor offset
   * computation, or null to skip default offset computation. 
   * @param defaultAnchorConfidence Value to assume in the case of Anchors that have no
   * value for "confidence". 
   * @throws TransformationException If the transformation cannot be completed.
   */
  public DefaultOffsetGenerator(int defaultOffsetThreshold, int defaultAnchorConfidence)
    throws TransformationException {
    setDefaultAnchorConfidence(defaultAnchorConfidence);
    setDefaultOffsetThreshold(defaultOffsetThreshold);
  } // end of constructor

  // GraphTransformer method

  /**
   * Generates default anchor offsets.
   * <p>Anchors with null offset, with no "confidence" attribute, or with the
   * "confidence" attribute set to equal or below {@link #defaultOffsetThreshold}, may
   * have their offset set to a default, computed value.  
   * <p>Strings of candidate anchors have their offsets set by linear interpolation
   * between bounding anchors. 
   * <p>Strings are determined by:
   * <ul>
   *  <li>chaining annotations together by common {@link Annotation#getStart()}/{@link
   *   Annotation#getEnd()} anchors (on any layer) - e.g. words chained together with
   *   interspersed or bounding noise annotations, or</li> 
   *  <li>chaining annotations together by successive values of {@link
   *   Annotation#getOrdinal()} on a common layer and within a common parent - e.g. words
   *   within a turn, interspersed with discontinuties in the graph (i.e. pauses).</li> 
   * </ul>
   * <p>Bounding anchors are determined by:
   * <ul>
   *  <li>having higher "confidence" than {@link #defaultOffsetThreshold} on the same
   *   layer and within a common parent - e.g. manually aligned words within a turn,</li> 
   *  <li>having higher "confidence" than {@link #defaultOffsetThreshold} on a different
   *   layer and within a common parent - e.g. manually aligned utterances within a turn
   *   partition the words in that turn, and</li> 
   *  <li>being a start or end anchor of a parent {@link #defaultOffsetThreshold} -
   *   e.g. a turn's start/end anchors bound the anchors of it's words.</li> 
   *  <li>or otherwise using the existing offsets of bounding candidate anchors.</li>
   * </ul>
   * @param graph The graph to transform.
   * @return The changes introduced by the tranformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph transform(Graph graph) throws TransformationException {
    if (debug) setLog(new Vector<String>());
    setErrors(new Vector<String>());

    Predicate<Anchor> boundingAnchor
      = anchor -> (anchor.getOffset() != null && getConfidence(anchor) > defaultOffsetThreshold)
      // @offsetGenerated is used to avoid setting the offset over and over in different chains
      || anchor.containsKey("@offsetGenerated");

    List<String> preferredChainLayers = null;
    if (graph.getSchema().getWordLayer() != null) {
      // prioritize chains through words, so words are generally evenly spread
      preferredChainLayers = Arrays.asList(graph.getSchema().getWordLayerId());
    }

    if (graph.getSchema().getUtteranceLayer() != null
        && graph.getSchema().getWordLayer() != null) {
      
      // first spread words evenly through turns
      for (Annotation utterance : graph.list(graph.getSchema().getUtteranceLayerId())) {
        if (utterance.getStart() == null
            || utterance.getStart().getOffset() == null
            || utterance.getEnd() == null
            || utterance.getEnd().getOffset() == null) continue;
        log("utterance ", utterance);
        
        // gather up anchors
        LinkedHashSet<Anchor> sequence = new LinkedHashSet<Anchor>();
        sequence.add(utterance.getStart());
        for (Annotation word : utterance.list(graph.getSchema().getWordLayerId())) {
          log("word ", word);
          // add the word's start anchor
          sequence.add(word.getStart());
          // find the anchor chain that moves forward from here, until the end of the utterance
          // this will catch any words with unset offsets (not returned by utterance.list()
          // and also words with intervening noise/comment chains
          AnchorChain wordChain = AnchorChain.ChainForwardUntil(
            word.getStart(), preferredChainLayers, anchor ->
            anchor.getOffset() != null && anchor.getOffset() >= utterance.getEnd().getOffset());
          // if the last anchor in the chain is past the end
          if (wordChain.lastElement().getOffset() != null
              && wordChain.lastElement().getOffset() > utterance.getEnd().getOffset()) {
            // remove it
            wordChain.remove(wordChain.lastElement());
          }
          sequence.addAll(wordChain);
        } // next word
        sequence.add(utterance.getEnd());
        log("utterance sequence ", sequence.toString());

        // interpolate them
        iterpolateAnchors(sequence.iterator());

        // tag the anchors, so that the become bounds for future chaining
        // e.g. word's are evenly spread through utterances,
        // and then phones are evenly spread through words without changing word bounds
        sequence.stream().forEach(a->a.put("@offsetGenerated", Boolean.TRUE));
        
      } // next utterance
    } // utterance and word layers are defined
    
    // iterate through all anchors, finding chains that require offsets set as we go
    for (Anchor anchor : graph.getAnchors().values()) {
      log("anchor ", anchor);
      // is this anchor part of a series of anchors that need their offsets to be generated?
      if (!boundingAnchor.test(anchor)) { // needs the offset
        // find the whole chain this anchor is part of
        AnchorChain chainBefore =
          AnchorChain.ChainBackwardUntil(anchor, preferredChainLayers, boundingAnchor);
        log("ChainBackwardUntil ", chainBefore.toString());
        AnchorChain chainAfter =
          AnchorChain.ChainForwardUntil(anchor, preferredChainLayers, boundingAnchor);
        log("ChainForwardUntil ", chainAfter.toString());
        
        AnchorChain chain = new AnchorChain();
        chain.addAll(chainBefore);
        chain.add(anchor);
        chain.addAll(chainAfter);
        log("chain ", chain.toString());

        // it's possible for child annotations to have no offsets but the parent to have them
        // so if the first/last anchor is the from a bounding child, add the parent anchor
        if (!boundingAnchor.test(chain.firstElement())) {
          log("unbound chain start");
          Optional<Annotation> boundingParent
            = chain.firstElement().getStartingAnnotations().stream()
            .filter(ann -> ann.getParent() != null)
            .filter(ann -> ann.getParent() != graph)
            .filter(ann -> ann.getParent().first(ann.getLayerId()) == ann)
            .map(ann -> ann.getParent())
            .findAny();
          if (boundingParent.isPresent()) {
            // prepend the chain with the parent's start
            chain.insertElementAt(boundingParent.get().getStart(), 0);
            log("start bound now ", boundingParent.get().getStart(), " - ", boundingParent.get());
          }
        } // unset first offset
        if (!boundingAnchor.test(chain.lastElement())) {
          log("unbound chain end");
          Optional<Annotation> boundingParent
            = chain.lastElement().getEndingAnnotations().stream()
            .filter(ann -> ann.getParent() != null)
            .filter(ann -> ann.getParent() != graph)
            .filter(ann -> ann.getParent().last(ann.getLayerId()) == ann)
            .map(ann -> ann.getParent())
            .findAny();
          if (boundingParent.isPresent()) {
            // append the chain with the parent's end
            chain.add(boundingParent.get().getEnd());
            log("end bound now ", boundingParent.get().getEnd(), " - ", boundingParent.get());
          }
        } // unset first offset
        
        // if there's a chain with defined bounds
        if (chain.size() > 1
            && chain.firstElement().getOffset() != null
            && chain.lastElement().getOffset() != null) {
          
          // bookend the chain with immovable sentinels
          chain.insertElementAt(
            new Anchor(null, chain.firstElement().getOffset(), getDefaultOffsetThreshold() + 1)
            , 0);
          chain.add(
            new Anchor(null, chain.lastElement().getOffset(), getDefaultOffsetThreshold() + 1));
          
          // iterpolate the anchor offsets between the start and the end
          iterpolateAnchors(chain.iterator());
          
          // tag the anchors, so that the become bounds for future chaining
          // e.g. word's are evenly spread through utterances,
          // and then phones are evenly spread through words without changing word bounds
          chain.stream().forEach(a->a.put("@offsetGenerated", Boolean.TRUE));
          
        } // can interpolate
      } // anchor that needs interpolating
    } // next anchor

    // clear the @offsetGenerated flags
    graph.getAnchors().values().stream().forEach(a->a.remove("@offsetGenerated"));

    /*
    // before going to great effort, check there are any anchors at all that might be affected
    boolean anchorsUnderThreshold = false;
    for (Anchor a : graph.getAnchors().values()) {
      if (a.getOffset() == null || getConfidence(a) <= defaultOffsetThreshold) {
        anchorsUnderThreshold = true;
        break;
      }
      // clear any cached min/max offsets - see Anchor.getOffsetMin()/getOffsetMax()
      a.remove("@offsetMin");
      a.remove("@offsetMax");
    } // next anchor
    if (!anchorsUnderThreshold) {
      log("There are no anchors with confidence <= ", defaultOffsetThreshold);
    } else {
      // we can't just sort the anchors of the graph all together before interpolation
      // because there may be independant spans that should be interpolated separately
      // e.g. the unaligned words of overlapping speech of two speakers
      // what we need to do is chunk the graph by parent annotations whose children
      // don't overlap (e.g. turns of words) and then interpolate their non-overlapping descendants
      // however, we're not going to do this for 'top level' layers - e.g. topic tags,
      // because their annotations may share anchors into other layers, and so changing those
      // offsets will disrupt the order of those other layers, being out of their context.
	 
      // traverse the layer hiercharchy to get a list of the uppermost layers that
      // are not top-level, and are peersOverlap == false      
      LayerHierarchyTraversal<LinkedHashSet<Layer>> layerTraversal 
        = new LayerHierarchyTraversal<LinkedHashSet<Layer>>(
          new LinkedHashSet<Layer>(), graph.getSchema()) {
            protected void pre(Layer child) {
              Layer parent = child.getParent();
              // if the parent is not a top-level layer
              if (parent != null // may be incomplete graph
                  && parent.getParentId() != null 
                  && !parent.getParentId().equals(schema.getRoot().getId())
                  // and children can have peers
                  && child.getPeers()
                  // and it's not a tag layer
                  && child.getAlignment() != Constants.ALIGNMENT_NONE
                  // and the parent temporally includes the children
                  && child.getParentIncludes()
                  // and we haven't already added this parent
                  && !getResult().contains(parent)) {
                // and we haven't already added an ancestor
                boolean includesAncestor = false;
                for (Layer ancestor : parent.getAncestors()) {
                  if (getResult().contains(ancestor)) {
                    includesAncestor = true;
                    break;
                  }
                }
                if (!includesAncestor) {
                  getResult().add(parent); // add the *parent* layer
                }
              } 
            }
          };
      
      // for each non-top-level parent layer
      log("layers ", layerTraversal.getResult());
      for (Layer layer : layerTraversal.getResult()) {
        // log("Layer ", layer);
        // for each parent annotation
        for (Annotation parent : graph.all(layer.getId())) {
          if (parent.getChange() == Change.Operation.Destroy) continue;
          Anchor parentStart = parent.getStart();
          Anchor parentEnd = parent.getEnd();
          
          try {
            Vector<Anchor> orderedAnchors = new Vector<Anchor>();
            // set the offsets of the descendants
            if (parentStart != null) orderedAnchors.add(parentStart);
            orderedAnchors.addAll(
              getOrderedAnchorsForDescendantsOf(parent).stream()
              .filter(a->a!=null)
              .filter(a->!orderedAnchors.contains(a))
              .collect(Collectors.toList()));
            if (parentEnd != null) orderedAnchors.add(parentEnd);
            log("parent ", parent, " anchors ", orderedAnchors);
            
            // avoid unbounded anchor chain problems by starting/ending the collection with
            // immovable start/end anchors - these come from graph.getSortedAnchors()
            // - which includes only anchors with offsets - instead of sortedAnchors
            // ensure that, no matter what, the bounding sentinels have offsets set
            Integer parentStartConfidence = parentStart==null?null:parentStart.getConfidence();
            Anchor startSentinel = null;
            if (parentStart != null && parentStart.getOffset() != null) {
              startSentinel = parentStart;
            } else {
              // use the lowest offset we find
              startSentinel = new Anchor();
              Optional<Anchor> firstAnchored = orderedAnchors.stream()
                .filter(a->a.getOffset() != null).findFirst();
              if (firstAnchored.isPresent()) {
                startSentinel.setOffset(firstAnchored.get().getOffset());
              }
            }
            startSentinel.setConfidence(getDefaultOffsetThreshold() + 1);
            orderedAnchors.insertElementAt(startSentinel, 0);
            
            Integer parentEndConfidence = parentEnd==null?null:parentEnd.getConfidence();
            Anchor endSentinel = null;
            if (parentEnd != null && parentEnd.getOffset() != null) {
              endSentinel = parentEnd;
            } else { // use the highest offset we find
              endSentinel = new Anchor();
              Optional<Anchor> lastAnchored = orderedAnchors.stream()
                .filter(a->a != null)
                .filter(a->a.getOffset() != null)
                .max((a1,a2)->a1.getOffset().compareTo(a2.getOffset()));
              if (lastAnchored.isPresent()) {
                endSentinel.setOffset(lastAnchored.get().getOffset());
              }
            }
            endSentinel.setConfidence(getDefaultOffsetThreshold() + 1);
            orderedAnchors.add(endSentinel);
    
            // crawl through the anchors looking for unset offsets
            iterpolateAnchors(orderedAnchors.iterator());
            
            // restore parent anchor confidence
            if (parentStart != null) parentStart.setConfidence(parentStartConfidence);
            if (parentEnd != null) parentEnd.setConfidence(parentEndConfidence);
            
          } catch (TransformationException x) {
            errors.add("Could not set descendant offsets for " + logAnnotation(parent) 
                       + ": " + x.getMessage());
          }
        } // next parent annotation
        // log("Layer complete ", layer);
      } // next layer
      // log("Layers complete");
    }
    */
    return graph;
  }
   
  /**
   * Gets anchors of all descendants of the given annotation, in order.
   * @param parent Parent annotation.
   * @return A list of anchors of all descendants of <var>parent</var>, ordered by offset
   * or stucture, but <em>excluding</em> the anchors of <var>parent</var> itself.
   * @throws TransformationException
   */
  protected Vector<Anchor> getOrderedAnchorsForDescendantsOf(Annotation parent)
    throws TransformationException {
    log("getAnchorsForDescendantsOf(", parent, ")");
    final Schema schema = parent.getGraph().getSchema();
    
    // we're only interested in certain child layers
    List<Layer> childLayers = parent.getLayer().getChildren().values().stream()
      .filter(layer->layer.getPeers())
      .filter(layer->layer.getAlignment() == Constants.ALIGNMENT_INTERVAL)
      .filter(layer->layer.getParentIncludes())
      .collect(Collectors.toList());
    
    Anchor parentStart = parent.getStart();
    Anchor parentEnd = parent.getEnd();
    
    // list of anchors in the order we find them
    // when adding an anchor, we remove it first, because if it's already there,
    // it's probably out of order - e.g. language phrase tags have lower ordinals than
    // the words they tag, but if they're aligned, they should come in the sequence of the
    // words, not the language tags
    Vector<Anchor> orderedAnchors = new Vector<Anchor>();
    
    // we need to traverse all child layers simultaneously
    // because some layers (e.g. utterance) partition others (e.g. words)

    Vector<Queue<Annotation>> childAnnotations = new Vector<Queue<Annotation>>();
    for (Layer layer : childLayers) {
      LinkedList<Annotation> children = new LinkedList<Annotation>(
        parent.getAnnotations(layer.getId()).stream()
        .filter(child->child.getChange() != Change.Operation.Destroy)
        .collect(Collectors.toList()));
      if (children.size() > 0) {
        childAnnotations.add(children);
      }
    } // next child layer

    Anchor waitingFor = null; // aligned end anchor we're waiting to catch up to

    // temporarily add the parent start anchor
    if (parentStart != null) orderedAnchors.add(parentStart);

    // while there are still child annotations
    while(childAnnotations.size() > 0) {
      Annotation next = null;
      // find which layer the next annotation comes from 
      for (Queue<Annotation> layer : childAnnotations) {
        Annotation candidate = layer.peek();
        if (next == null) {
          next = candidate;
        } else {
          if (next.getStart().getOffset() != null && candidate.getStart().getOffset() != null) {
            // both have start offsets so can be directly compared
            if (candidate.getStart().getOffset() < next.getStart().getOffset()) {
              // earlier starts first
              next = candidate;
            } else if (candidate.getStart().getOffset().equals(next.getStart().getOffset())) {
              Double nextOffsetMax = next.getEnd().getOffsetMax();
              Double candidateOffsetMax = candidate.getEnd().getOffsetMax();
              log("same starts ", next, " and ", candidate, " nextOffsetMax: ", nextOffsetMax, " candidateOffsetMax: ", candidateOffsetMax);
              // starts are the same, so longest first
              if (nextOffsetMax == null) {
                // no end offset counts as 'shorter'
                next = candidate;
              } else if (candidateOffsetMax != null) {
                if (candidateOffsetMax > nextOffsetMax) {
                  next = candidate;
                } else if (candidateOffsetMax.equals(nextOffsetMax) // maxes are the same
                           // but the candidate has and end offset (e.g. utterance)
                           && candidate.getEnd().getOffset() != null
                           // and the current 'next' doesn't (e.g. first word of utterance)
                           && next.getEnd().getOffset() == null) {
                  // so if it's between an utterance and a word that's the beginning of a chain
                  // to the end of the utterance, then the utterance is first
                  next = candidate;
                }
              } // else candidateOffsetMax() == null so it's 'shorter' than next
            } // else candidate.start > next.start, so no change in next
          } else { // at least one of the start offsets isn't set
            Double nextOffsetMin = next.getStart().getOffsetMin();
            Double candidateOffsetMin = candidate.getStart().getOffsetMin();
            if (nextOffsetMin != null && candidateOffsetMin != null) {
              // both have offset minima so can be directly compared
              if (candidateOffsetMin < nextOffsetMin) {
                // earlier starts first
                next = candidate;
              } else if (candidateOffsetMin.equals(nextOffsetMin)) {
                // offset minima are the same, so longest first
                Double nextOffsetMax = next.getEnd().getOffsetMax();
                Double candidateOffsetMax = candidate.getEnd().getOffsetMax();
                // starts are the same, so longest first
                if (nextOffsetMax == null) {
                  // no end offset counts as 'shorter'
                  next = candidate;
                } else if (candidateOffsetMax != null
                           && candidateOffsetMax > nextOffsetMax) {
                  next = candidate;
                } // else candidateOffsetMax() == null so it's 'shorter' than next
              } // else candidateOffsetMin > nextOffsetMin, so no change in next
            } else { // at least one of them has no offset minimum
              if (candidateOffsetMin == null) { // candidate
                next = candidate;
              }
            }
          } // at least one of the start offsets isn't set
        } // need to choose between next and candidate
      } // next layer
      log("next ", next, " - waitingFor ", waitingFor);

      // pop the next annotation off whichever queue it came from
      for (Queue<Annotation> layer : childAnnotations) {
        assert next != null : "next != null - " + layer;
        if (next == layer.peek()) {
          layer.remove();
          if (layer.size() == 0) {
            childAnnotations.remove(layer);
          }
          break;
        }
      } // next layer
      
      // ignore utterance tags - i.e. annotations simultaneous with an utterance start and end
      // e.g. utterance HTK tags that have become unlinked
      if (!next.getLayerId().equals(schema.getUtteranceLayerId())) {
        final Annotation finalNext = next;
        Optional<Annotation> simultaneousUtterance 
          = next.getStart().startOf(schema.getUtteranceLayerId()).stream()
          .filter(utt -> utt.getEnd() != null)
          .filter(utt -> utt.getEnd().getOffset() != null)
          .filter(utt -> utt.getEnd().getOffset().equals(finalNext.getEnd().getOffset()))
          .findAny();
        if (simultaneousUtterance.isPresent()) {
          log(" next ", next, " simultneousUtterance ", simultaneousUtterance.get());
          continue;
        }
      } // not an utterance
      
      // is the next annotation's start anchor after the one we're waiting for?
      if (waitingFor != null && next.getStart().getOffset() != null
          && waitingFor.getOffset() < next.getStart().getOffset()) {
        // we've passed the offset we're waiting for, so add that anchor
        orderedAnchors.remove(waitingFor); // (remove it first - last add counts)
        orderedAnchors.add(waitingFor);
        log(" reached (by start) ", waitingFor);
        waitingFor = null;
      }
      // add the next annotation's start anchor to the list
      if (next.getStart() != parentStart) { // (but not the parent start, which is always the 1st)
        orderedAnchors.remove(next.getStart()); // (remove it first - last add counts)
        orderedAnchors.add(next.getStart());
        log(" added start ", next.getStart(), " "+orderedAnchors);
        // add all descendants of this child
        orderedAnchors.addAll(
          getOrderedAnchorsForDescendantsOf(next).stream()
          .filter(a->a!=null)
          .filter(a->!orderedAnchors.contains(a))
          .collect(Collectors.toList()));
      }
      // have we been waiting for this one?
      if (next.getStart() == waitingFor) {
        log(" not waiting for ", waitingFor, " any more");
        waitingFor = null;
      }
      
      if (next.getEnd().getOffset() == null) {
        orderedAnchors.remove(next.getEnd()); // (remove it first - last add counts)
        orderedAnchors.add(next.getEnd());
        log(" added end ", next.getEnd());
      } else {
        if (waitingFor == null) {
          // wait until other offsets pass the end of this one before adding it
          waitingFor = next.getEnd();
          log(" now waitingFor ", waitingFor);
        } else {
          // we're already waiting for another offset
          if (waitingFor.getOffset() < next.getEnd().getOffset()) {
            // we've passed the offset we're waiting for, so add that anchor
            orderedAnchors.remove(waitingFor); // (remove it first - last add counts)
            orderedAnchors.add(waitingFor);
            log(" reached ", waitingFor);
            // and start waiting for the new offset
            waitingFor = next.getEnd();
            log(" now waitingFor ", waitingFor);
          } else {
            // haven't reached the offset we want yet, so add this end anchor
            orderedAnchors.remove(next.getEnd()); // (remove it first - last add counts)
            orderedAnchors.add(next.getEnd());
            log(" added anchored end ", next.getEnd());
          }
        }
      }
      
    } // next pass
    if (waitingFor != null) {
      orderedAnchors.remove(waitingFor); // (remove it first - last add counts)
      orderedAnchors.add(waitingFor);
    }
    // temporarily add the parent end anchor
    if (parentEnd != null) {
      orderedAnchors.remove(parentEnd);
      orderedAnchors.add(parentEnd);
    }

    // remove any null entries
    Iterator<Anchor> anchors = orderedAnchors.iterator();
    while (anchors.hasNext()) if (anchors.next() == null) anchors.remove();

    // if there are gaps before starts of any children,
    // look for chains of annotations bridging the gap
    // (e.g. noises, comments, but not grandchild annotations like phones)
    for (int a = 1; a < orderedAnchors.size(); a++) {
      Anchor nextAnchor = orderedAnchors.elementAt(a);
      // is the next anchor the start of a child?
      if ((isStartOf(nextAnchor, childLayers)
           // (or the end of the parent)
           || nextAnchor == parentEnd)
          // but not the end of a child?
          && !isEndOf(nextAnchor, childLayers)) { // gap between children
        
        Anchor lastAnchor = orderedAnchors.elementAt(a-1);
        
        // check for a chain of annotations in the gap
        AnnotationChain chain = new AnnotationChain(lastAnchor, nextAnchor, 4);
        if (chain.size() > 0) {
          log("Inserting chain ", chain);
          for (Annotation annotation : chain) {
            Anchor start = annotation.getStart();
            if (start != lastAnchor && !orderedAnchors.contains(start)) {
              orderedAnchors.insertElementAt(start, a++);
            }
          } // next annotation in chain
        } // there is a chain
        
      } // gap between children
    } // next anchor 

    //log("orderedAnchors: ", orderedAnchors);
    return orderedAnchors;
  } // end of getOrderedAnchorsForDescendantsOf()
  
  /**
   * Determines whether the given anchor is the start of an annotation on any of the given layers.
   * @param anchor
   * @param layers
   * @return True if the given anchor is the start of an annotation on any of the given
   * layers, false otherwise. 
   */
  protected boolean isStartOf(Anchor anchor, List<Layer> layers) {
    for (Layer layer : layers) {
      if (anchor.getStartOf().containsKey(layer.getId())) {
        if (anchor.startOf(layer.getId()).stream()
            .filter(annotation->annotation.getChange() != Change.Operation.Destroy)
            .findAny().isPresent()) {
          // found one
          return true;
        }
      } // the child layer is registered
    } // next child layer
    return false;
  } // end of isStartOf()
  
  /**
   * Determines whether the given anchor is the end of an annotation on any of the given layers.
   * @param anchor
   * @param layers
   * @return True if the given anchor is the end of an annotation on any of the given
   * layers, false otherwise. 
   */
  protected boolean isEndOf(Anchor anchor, List<Layer> layers) {
    for (Layer layer : layers) {
      if (anchor.getEndOf().containsKey(layer.getId())) {
        if (anchor.endOf(layer.getId()).stream()
            .filter(annotation->annotation.getChange() != Change.Operation.Destroy)
            .findAny().isPresent()) {
          // found one
          return true;
        } 
      } // the child layer is registered
    } // next child layer
    return false;
  } // end of isEndOf()
  
  /**
   * Interpolates any unset or low-confidence anchors in the given iterator.
   * <p> This method assumes that the first and last anchors has offsets and are high confidence.
   * @param anchors
   */
  protected void iterpolateAnchors(Iterator<Anchor> anchors) throws TransformationException {
    Anchor lastSetAnchor = null;
    Anchor firstAnchor = null;
    while (anchors.hasNext()) {
      Anchor anchor = anchors.next();
      if (firstAnchor == null) firstAnchor = anchor;
      if (anchor.getOffset() != null && getConfidence(anchor) > defaultOffsetThreshold) {
        lastSetAnchor = anchor;
        log("last set: ", lastSetAnchor);
      } else {
        if (lastSetAnchor == null) {
          String message = "Could not determine bounds, starting from " + logAnchor(firstAnchor);
          log("ERROR: ", message);
          throw new TransformationException(this, message);
        }
		  
        Vector<Anchor> unsetAnchors = new Vector<Anchor>();
        unsetAnchors.add(anchor);
        log("first unset: ", anchor);
	       
        // scan forward from here to find the next set Anchor
        Anchor nextSetAnchor = null;
        while (nextSetAnchor == null) {
          // check we haven't hit the end
          if (!anchors.hasNext()) {
            if (anchor.getOffset() != null && getConfidence(anchor) > defaultOffsetThreshold) {
              // the last one we saw actually has an offset,
              // so we use that one as the last anchor
              unsetAnchors.remove(anchor);
              nextSetAnchor = anchor;
              break;
            } else {
              String message = "Could not determine bounds, starting from " + logAnchor(lastSetAnchor) 
                + " after " + logAnchor(unsetAnchors.lastElement());
              log("ERROR: ", message);
              throw new TransformationException(this, message);
            }
          }
          anchor = anchors.next();
          if (anchor.getOffset() == null 
              || getConfidence(anchor) <= defaultOffsetThreshold) {
            // add the unset anchor to the collection
            unsetAnchors.add(anchor);
            log("unset: ", anchor);
          } else { // offset is set
            // stop
            nextSetAnchor = anchor;
            log("next set: ", nextSetAnchor);
          }
        } // next anchor
		  
        if (unsetAnchors.size() > 0) {
          // if there are no annotations between the last set anchor
          // and the first unset anchor, then give the unset anchor
          // the same offset as the last set anchor
          Anchor firstUnset = unsetAnchors.firstElement();
          assert lastSetAnchor != null : "lastSetAnchor != null";
          assert firstUnset != null : "firstUnset != null";
          if (lastSetAnchor.annotationTo(firstUnset) == null) {
            firstUnset
              .setOffset(lastSetAnchor.getOffset())
              .setConfidence(getConfidence());
            unsetAnchors.remove(firstUnset);
            lastSetAnchor = firstUnset;
            log("revised last: ", lastSetAnchor);
          }
        }
		  
        if (unsetAnchors.size() > 0) {
          // if there are no annotations between the next set anchor
          // and the last unset anchor, then give the unset anchor
          // the same offset as the next set anchor
          Anchor lastUnset = unsetAnchors.lastElement();
          if (lastUnset.annotationTo(nextSetAnchor) == null) {
            lastUnset
              .setOffset(nextSetAnchor.getOffset())
              .setConfidence(getConfidence());
            unsetAnchors.remove(lastUnset);
            nextSetAnchor = lastUnset;
            log("revised next: ", nextSetAnchor);
          }
		     
          if (unsetAnchors.size() > 0) {
            // spread out unset anchors evenly between the bounds
            double dStart = lastSetAnchor.getOffset();
            double dEnd = nextSetAnchor.getOffset();
            double dDuration = dEnd - dStart;
            double dIncrement = dDuration / (unsetAnchors.size() + 1);
            log("from: ", lastSetAnchor, " to ", nextSetAnchor,
                " duration: ", dDuration, " increment: ", dIncrement);
            int i = 0;
            for (Anchor unset : unsetAnchors) {
              i++;
              double newOffset = dStart + i * dIncrement;
              if (unset.getOffset() == null 
                  || unset.getOffset().doubleValue() != newOffset
                  // upgrade confidence even if unset.offset == newOffset
                  || getConfidence(unset) < getConfidence()) {
                log("setting: ", unset, " offset to ", newOffset);
                unset
                  .setOffset(newOffset)
                  .setConfidence(getConfidence());
              }
            } // next unset anchor
          } // unsetAnchors.size() > 0
        } // unsetAnchors.size() > 0
		  
          // update the last set anchor 
        lastSetAnchor = nextSetAnchor;
        log("last now: ", lastSetAnchor);
		  
      } // offset is not set
    } // next anchor
  } // end of iterpolateAnchors()
   
  /**
   * Gets the confidence rating of a given anchor.  If no Integer confidence attribute is
   * present, the {@link #defaultAnchorConfidence} is returned. 
   * @param anchor The anchor to get the rating of.
   * @return The confidence rating of a given annotation, or defaultAnchorConfidence if
   * it could not be determined. 
   */
  protected int getConfidence(Anchor anchor) {
    Integer c = anchor.getConfidence();
    if (c == null) return getDefaultAnchorConfidence();
    return c.intValue();
  } // end of getConfidence()
   
  /**
   * A representation of the given anchor for logging purposes.
   * @param anchor The anchor to log.
   * @return A representation of the given anchor for logging purposes.
   */
  protected String logAnchor(Anchor anchor) {
    if (anchor == null) return "[null]";
    return "[" + anchor.getId() + "]" + anchor.getOffset() + "(" + getConfidence(anchor) + ")";
  } // end of logAnnotation()

  /**
   * A representation of the given annotation for logging purposes.
   * @param annotation The annotation to log.
   * @return A representation of the given annotation for loggin purposes.
   */
  protected String logAnnotation(Annotation annotation) {
    if (annotation == null) return "[null]";
    return "[" + annotation.getId() + "]" + annotation.getOrdinal() + "#" + annotation.getLabel();
  } // end of logAnnotation()

  /**
   * Logs a debugging message.
   * @param messages The objects making up the log message.
   */
  protected void log(Object ... messages) {
    if (debug) { // we only interpret arguments to log() if we're actually debugging...
      StringBuilder s = new StringBuilder();
      for (Object m : messages) {
        if (m instanceof Annotation) {
          if (m == null) {
            s.append("[null]");
          } else {
            Annotation annotation = (Annotation)m;
            s.append("[").append(annotation.getId()).append("]")
              .append(annotation.getOrdinal()).append("#")
              .append(annotation.getLabel())
              .append("(").append(annotation.getStart())
              .append("-").append(annotation.getEnd()).append(")");
          }
        } else if (m instanceof Anchor) {
          if (m == null) {
            s.append("[null]");
          } else {
            Anchor anchor = (Anchor)m;
            s.append("[").append(anchor.getId()).append("]").append(anchor.getOffset());
          }
        } else {
          if (m == null) {
            s.append("[null]");
          } else {
            s.append(m.toString());
          }
        } 
      }	 
      log.add(s.toString());
      // System.out.println(s.toString());
    }
  } // end of log()

  /** Command line interface entrypoint: reads JSON-encoded transcripts from stdin,
   * generates default anchor offsets, and writes them to stdout. */
  public static void main(String argv[]) {
    DefaultOffsetGenerator cli = new DefaultOffsetGenerator();
    if (cli.processArguments(argv)) {
      cli.start();
    }
  }

} // end of class Validator
