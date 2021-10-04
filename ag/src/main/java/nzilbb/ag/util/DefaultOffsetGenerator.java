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
import java.util.Collections;
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
  protected Boolean debug = Boolean.FALSE;
  /**
   * Getter for {@link #debug}: Whether a log of messages should be kept for reporting.
   * @return Whether a log of messages should be kept for reporting.
   * @see #getLog()
   * @see #log(Object...)
   */
  public Boolean getDebug() { return debug; }
  /**
   * Setter for {@link #debug}: Whether a log of messages should be kept for reporting.
   * @param newDebug Whether a log of messages should be kept for reporting.
   * @see #getLog()
   * @see #log(Object...)
   */
  @Switch("Print verbose debug logging")
  public DefaultOffsetGenerator setDebug(Boolean newDebug) { debug = newDebug; return this; }

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

    final List<String> preferredChainLayers = new Vector<String>();
    if (graph.getSchema().getWordLayer() != null) {      
      // prioritize chains through aligned word child layers so phones are generally evenly spread
      graph.getSchema().getWordLayer()
        .getChildren().values().stream()
        .filter(layer -> layer.getAlignment() > Constants.ALIGNMENT_NONE)
        .forEach(layer -> preferredChainLayers.add(layer.getId()));
      // then prioritize chains through words, so words are generally evenly spread
      preferredChainLayers.add(graph.getSchema().getWordLayerId());
    }

    final String turnLayerId = graph.getSchema().getTurnLayerId();
    if (graph.getSchema().getUtteranceLayer() != null
        && graph.getSchema().getWordLayer() != null
        && turnLayerId != null) {
      
      // first spread words evenly through utterances...

      // assign words to each utterance
      for (Annotation turn : graph.list(graph.getSchema().getTurnLayerId())) {
        Iterator<Annotation> utterances
          = new AnnotationsByAnchor(turn.getAnnotations(graph.getSchema().getUtteranceLayerId()))
          .stream().filter(u -> u.getStart().getOffset() != null).iterator();
        if (!utterances.hasNext()) continue;
        Annotation currentUtterance = utterances.next();
        currentUtterance.put("@words", new Vector<Annotation>());
        Annotation nextUtterance = utterances.hasNext()?utterances.next():null;
        for (Annotation word : turn.getAnnotations(graph.getSchema().getWordLayerId())) {
          if (word.getStart() == null) continue; // ?!
          if (// the start is inside the next utterance...
            (word.getStart().getOffset() != null 
             && nextUtterance != null
             && word.getStart().getOffset() >= nextUtterance.getStart().getOffset())
            || // ...or the start offset is null and the end is inside the next utterance
            (word.getStart().getOffset() == null
             && word.getEnd().getOffset() != null
             && nextUtterance != null
             && word.getEnd().getOffset() > nextUtterance.getStart().getOffset())) {
            // next utterance
            currentUtterance = nextUtterance;
            currentUtterance.put("@words", new Vector<Annotation>());
            nextUtterance = utterances.hasNext()?utterances.next():null;
          } // next utterance
          log("utt ", currentUtterance, " word ", word);
          ((Vector<Annotation>)currentUtterance.get("@words")).add(word);
        } // next word
      } // next turn
        
      for (Annotation utterance : graph.list(graph.getSchema().getUtteranceLayerId())) {
        if (utterance.getStart() == null
            || utterance.getStart().getOffset() == null
            || utterance.getEnd() == null
            || utterance.getEnd().getOffset() == null) continue;
        if (utterance.getChange() == Change.Operation.Destroy) continue;
        log("utterance ", utterance, " words ", utterance.get("@words"));
        
        // gather up anchors of descendants in the same turn
        final Annotation turn = utterance.first(turnLayerId);
        LinkedHashSet<Anchor> sequence = new LinkedHashSet<Anchor>();
        sequence.add( // prepend immovable sentinel
          new Anchor(null, utterance.getStart().getOffset(), getDefaultOffsetThreshold() + 1));
        sequence.add(utterance.getStart());
        boolean firstWord = true;
        Vector<Annotation> words = (Vector<Annotation>)utterance.get("@words");
        if (words == null) continue; // no words assigned to this utterance
        
        for (Annotation word : words) {
          if (word.getChange() == Change.Operation.Destroy) continue;
          log("word ", word);
          if (firstWord) {
            // the first word may be preceded by words with no offset, so look backwards
            AnchorChain wordChain = AnchorChain.ChainBackwardUntil(
              word.getStart(), preferredChainLayers,
              annotation -> // only follow annotations...
              !annotation.getLayer().isAncestor(turnLayerId) // ... that have no turn
              || annotation.first(turnLayerId) == turn, //  or are in the same turn as utterance
              anchor ->        // stop when we get beyond the bounds of the word or utterance
              (anchor.getOffset() != null
               && anchor.getOffset() <= utterance.getStart().getOffset()));
            if (wordChain.size() > 0) {
              // log("word chain: ", wordChain, "first anchor: ", wordChain.firstElement(),
              //     " utt start: ", utterance.getStart());
              // if the first anchor in the chain is before the start
              if (wordChain.firstElement().getOffset() != null
                  && wordChain.firstElement().getOffset() < utterance.getStart().getOffset()) {
                // remove it
                wordChain.remove(wordChain.firstElement());
              }
              sequence.addAll(wordChain);
            }
            
            firstWord = false;
          } // firstWord
          // add the word's start anchor
          sequence.add(word.getStart());
          // find the anchor chain that moves forward from here, until the end of the utterance
          // this will catch any words with unset offsets (not returned by utterance.list()
          // and also words with intervening noise/comment chains
          log(" word ", word, " chainForwardUntil ", utterance.getEnd());
          AnchorChain wordChain = AnchorChain.ChainForwardUntil(
            word.getStart(), preferredChainLayers,
            annotation -> // only follow annotations...
            !annotation.getLayer().isAncestor(turnLayerId) // ... that have no turn
            || annotation.first(turnLayerId) == turn, // ... or are in the same turn as utterance
            anchor ->        // stop when we get beyond the bounds of the utterance
            (anchor == word.getEnd()
             || (anchor.getOffset() != null
                 && anchor.getOffset() >= utterance.getEnd().getOffset())));
          log(" chain ", wordChain);
          if (wordChain.size() > 0) {
            // if the last anchor in the chain is past the end
            if (wordChain.lastElement().getOffset() != null
                && wordChain.lastElement().getOffset() > utterance.getEnd().getOffset()) {
              // remove it
              wordChain.remove(wordChain.lastElement());
            }
            log(" chain now ", wordChain);
            sequence.addAll(wordChain);
          }
        } // next word
        sequence.add(utterance.getEnd());
        sequence.add( // append immovable sentinel
          new Anchor(null, utterance.getEnd().getOffset(), getDefaultOffsetThreshold() + 1));
        log("utterance sequence ", sequence.toString(), " utterance end ", utterance.getEnd());

        // interpolate them
        iterpolateAnchors(sequence.iterator());

        // tag the anchors, so that the become bounds for future chaining
        // e.g. word's are evenly spread through utterances,
        // and then phones are evenly spread through words without changing word bounds
        sequence.stream().forEach(a->a.put("@offsetGenerated", Boolean.TRUE));
        
      } // next utterance
    } // utterance and word layers are defined
    
    // now iterate through all anchors, finding chains that require offsets set as we go
    for (Anchor anchor : graph.getAnchors().values()) {
      log("anchor ", anchor);
      // is this anchor part of a series of anchors that need their offsets to be generated?
      if (!boundingAnchor.test(anchor)) { // needs the offset
        // find the whole chain this anchor is part of
        AnchorChain chainBefore =
          AnchorChain.ChainBackwardUntil(anchor, preferredChainLayers, null, boundingAnchor);
        log("ChainBackwardUntil ", chainBefore.toString());
        AnchorChain chainAfter =
          AnchorChain.ChainForwardUntil(anchor, preferredChainLayers, null, boundingAnchor);
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
          if (boundingParent.isPresent() && boundingParent.get().getStart() != null) {
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
          if (boundingParent.isPresent() && boundingParent.get().getEnd() != null) {
            // append the chain with the parent's end
            chain.add(boundingParent.get().getEnd());
            log("end bound now ", boundingParent.get().getEnd(), " - ", boundingParent.get());
          }
        } // unset first offset
        
        // if there's a chain with defined bounds
        if (chain.size() > 1) {
          Optional<Double> startOffset = chain.stream()
            .filter(a -> a.getOffset() != null)
            .map(a -> a.getOffset())
            .findFirst();
          Vector<Anchor> reverseChain = new Vector<Anchor>(chain);
          Collections.reverse(reverseChain);
          Optional<Double> endOffset = reverseChain.stream()
            .filter(a -> a.getOffset() != null)
            .map(a -> a.getOffset())
            .findFirst();

          if (startOffset.isPresent() && endOffset.isPresent()) {
            
            // bookend the chain with immovable sentinels
            chain.insertElementAt(
              new Anchor(null, startOffset.get(), getDefaultOffsetThreshold() + 1), 0);
            chain.add(
              new Anchor(null, endOffset.get(), getDefaultOffsetThreshold() + 1));
            
            // iterpolate the anchor offsets between the start and the end
            iterpolateAnchors(chain.iterator());
            
            // tag the anchors, so that the become bounds for future chaining
            // e.g. word's are evenly spread through utterances,
            // and then phones are evenly spread through words without changing word bounds
            chain.stream().forEach(a->a.put("@offsetGenerated", Boolean.TRUE));
            
          } // can interpolate
        }
      } // anchor that needs interpolating
    } // next anchor

    // clear the @offsetGenerated flags
    graph.getAnchors().values().stream().forEach(a->a.remove("@offsetGenerated"));

    // ensure all offsets are set
    List<Anchor> unsetOffsets = graph.getAnchors().values().stream()
      .filter(anchor -> anchor.getChange() != Change.Operation.Destroy)
      .filter(anchor -> anchor.isLinked())
      .filter(anchor -> anchor.getOffset() == null)
      .collect(Collectors.toList());
    if (unsetOffsets.size() > 0) {
      throw new TransformationException(this, "Could not determine offsets: " + unsetOffsets);
    }

    return graph;
  }
   
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
              String message = "Could not determine bounds, starting from "
                + logAnchor(lastSetAnchor) 
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
            if (dDuration < 0) {
              String message = "Negative duration from " + logAnchor(lastSetAnchor)
                + " to " + logAnchor(nextSetAnchor);
              log("ERROR: ", message);
              //TODO make this optional? throw new TransformationException(this, message);
            } else {
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
            } // not backwards!
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
      try {
        cli.start();
      } finally {
        if (cli.getDebug()) {
          for (String message : cli.log) System.err.println(message); 
        }
      }
    }
  }

} // end of class Validator
