//
// Copyright 2016-2023 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import nzilbb.ag.*;
import nzilbb.ag.cli.Transform;
import nzilbb.ag.serialize.SerializationException;
import nzilbb.ag.serialize.SerializationParametersMissingException;
import nzilbb.ag.serialize.SerializerNotConfiguredException;
import nzilbb.ag.serialize.json.JSONSerialization;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.editpath.*;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;

/**
 * Merges an edited version of a graph into the original version of that graph.
 * <p>The merger assumes that {@link #editedGraph} and the <var>graph</var> passed to
 * {@link #transform(Graph)} really are versions of the same graph. {@link #editedGraph} needn't
 * contain all the layers that <var>graph</var> does, but it must have a valid layer hierarchy;
 * e.g. if it's a fragment representing an utterance, it must nevertheless have (possibly
 * reconstructed) turn and participant annotations to link the utterance to the graph itself (if
 * <var>graph</var> does). For more detailed assumption information, see
 * {@link #transform(Graph)}.
 * <p>TODO handle graphs with null offset anchors.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Merges changes from one JSON-encoded annotation graph into another")
public class Merger extends Transform implements GraphTransformer {
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
  public Merger setErrors(Vector<String> newErrors) { errors = newErrors; return this; }
   
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
  @Switch("Whether a log of messages should be printed")
  public Merger setDebug(boolean newDebug) { debug = newDebug; return this; }

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
  protected Merger setLog(Vector<String> newLog) { log = newLog; return this; }

  /**
   * Listeners for log messages.
   * @see #getLogObservers()
   * @see #setLogObservers(List)
   */
  protected List<Consumer<String>> logObservers = new Vector<Consumer<String>>();
  /**
   * Getter for {@link #logObservers}: Listeners for log messages.
   * @return Listeners for log messages.
   */
  public List<Consumer<String>> getLogObservers() { return logObservers; }
  /**
   * Setter for {@link #logObservers}: Listeners for log messages.
   * @param newLogObservers Listeners for log messages.
   */
  public Merger setLogObservers(List<Consumer<String>> newLogObservers) { logObservers = newLogObservers; return this; }
   
  /**
   * The edited version of the graph.
   * @see #getEditedGraph()
   * @see #setEditedGraph(Graph)
   */
  protected Graph editedGraph;
  /**
   * Getter for {@link #editedGraph}: The edited version of the graph.
   * @return The edited version of the graph.
   */
  public Graph getEditedGraph() { return editedGraph; }
  /**
   * Setter for {@link #editedGraph}: The edited version of the graph.
   * @param newEditedGraph The edited version of the graph.
   */
  public Merger setEditedGraph(Graph newEditedGraph) { editedGraph = newEditedGraph; return this; }
   
  /**
   * The maximum length of a list used for mapping annotations of the same layer but in different
   * graphs to each other. 
   * <p>For word and segment layers, the arrays used for shortest-edit-path computation 
   * can get very very large. In order to avoid running out of memory, we split the lists into
   * overlapping chunks and map overlapping chunks. This doesn't strictly give us a guaranteed 
   * shortest edit path, but it works for realistic cases and terminates in a realistic time 
   * with a realistic amount of memory available.
   * <p>Default is 500. A value of 0 means don't used overlapping chunks.
   * @see #getArraySizeLimit()
   * @see #setArraySizeLimit(int)
   */
  protected int arraySizeLimit = 500;
  /**
   * Getter for {@link #arraySizeLimit}: The maximum length of a list used for mapping
   * annotations of the same layer but in different graphs to each other. 
   * <p>Default is 500. A value of 0 means don't used overlapping chunks.
   * @return The maximum length of a list used for mapping annotations of the same layer but in
   * different graphs to each other. 
   */
  public int getArraySizeLimit() { return arraySizeLimit; }
  /**
   * Setter for {@link #arraySizeLimit}: The maximum length of a list used for mapping
   * annotations of the same layer but in different graphs to each other. 
   * @param newArraySizeLimit The maximum length of a list used for mapping annotations of the
   * same layer but in different graphs to each other. A value of 0 means don't used overlapping
   * chunks. 
   */
  public Merger setArraySizeLimit(int newArraySizeLimit) { arraySizeLimit = newArraySizeLimit; return this; }

  /**
   * Initial duration of early/layer children, out of order annotations, etc. Default value is 0.00001.
   * @see #getSmidgin()
   * @see #setSmidgin(double)
   */
  protected double smidgin = 0.00001;
  /**
   * Getter for {@link #smidgin}: Initial duration of early/layer children, out of order
   * annotations, etc. 
   * @return Initial duration of early/layer children, out of order annotations, etc.
   */
  public double getSmidgin() { return smidgin; }
  /**
   * Setter for {@link #smidgin}: Initial duration of early/layer children, out of order annotations, etc.
   * @param newSmidgin Initial duration of early/layer children, out of order annotations, etc.
   */
  public Merger setSmidgin(double newSmidgin) { smidgin = newSmidgin; return this; }

  /**
   * Whether to ignore label confidence and force label changes (true), or only change labels
   * when the edited confidence is equal to or higher than the original confidence. Default is
   * false. 
   * @see #getIgnoreLabelConfidence()
   * @see #setIgnoreLabelConfidence(boolean)
   */
  protected boolean ignoreLabelConfidence = false;
  /**
   * Getter for {@link #ignoreLabelConfidence}: Whether to ignore label confidence and force
   * label changes (true), or only change labels when the edited confidence is equal to or higher
   * than the original confidence. 
   * @return Whether to ignore label confidence and force label changes (true), or only change
   * labels when the edited confidence is equal to or higher than the original
   * confidence. Default is false. 
   */
  public boolean getIgnoreLabelConfidence() { return ignoreLabelConfidence; }
  /**
   * Setter for {@link #ignoreLabelConfidence}: Whether to ignore label confidence and force
   * label changes (true), or only change labels when the edited confidence is equal to or higher
   * than the original confidence. 
   * @param newIgnoreLabelConfidence Whether to ignore label confidence and force label changes
   * (true), or only change labels when the edited confidence is equal to or higher than the
   * original confidence. 
   */
  public Merger setIgnoreLabelConfidence(boolean newIgnoreLabelConfidence) { ignoreLabelConfidence = newIgnoreLabelConfidence; return this; }
   
  /**
   * Wether to ignore offset confidence and force offset changes (true), or only change offsets
   * when the edited offset is equal to or higher than the original confidence (false).  The
   * default is false. 
   * @see #getIgnoreOffsetConfidence()
   * @see #setIgnoreOffsetConfidence(boolean)
   */
  protected boolean ignoreOffsetConfidence = false;
  /**
   * Getter for {@link #ignoreOffsetConfidence}: Wether to ignore offset confidence and force
   * offset changes (true), or only change offsets when the edited offset is equal to or higher
   * than the original confidence (false). 
   * @return Wether to ignore offset confidence and force offset changes (true), or only change
   * offsets when the edited offset is equal to or higher than the original confidence (false).
   * The default is false. 
   */
  public boolean getIgnoreOffsetConfidence() { return ignoreOffsetConfidence; }
  /**
   * Setter for {@link #ignoreOffsetConfidence}: Wether to ignore offset confidence and force
   * offset changes (true), or only change offsets when the edited offset is equal to or higher
   * than the original confidence (false). 
   * @param newIgnoreOffsetConfidence Wether to ignore offset confidence and force offset changes
   * (true), or only change offsets when the edited offset is equal to or higher than the
   * original confidence (false). 
   */
  public Merger setIgnoreOffsetConfidence(boolean newIgnoreOffsetConfidence) { ignoreOffsetConfidence = newIgnoreOffsetConfidence; return this; }

  /**
   * Set of IDs of layers for which annotations may not be added, changed, or deleted.
   * @see #getNoChangeLayers()
   */
  protected HashSet<String> noChangeLayers = new HashSet<String>();
  /**
   * Getter for {@link #noChangeLayers}: Set of IDs of layers for which annotations may
   * not be added, changed, or deleted. 
   * @return Set of IDs of layers for which annotations may not be added, changed, or deleted.
   */
  public HashSet<String> getNoChangeLayers() { return noChangeLayers; }
   
  /**
   * The validator to use after merge is complete, or null to not validate the graph after
   * merge. Default value is a {@link Validator} created with its default constructor. 
   * @see #getValidator()
   * @see #setValidator(Validator)
   */
  protected Validator validator = new Validator();
  /**
   * Getter for {@link #validator}: The validator to use after merge is complete, or null to not
   * validate the graph after merge. 
   * @return The validator to use after merge is complete, or null to not validate the graph
   * after merge. Default value is a {@link Validator} created with its default constructor. 
   */
  public Validator getValidator() { return validator; }
  /**
   * Setter for {@link #validator}: The validator to use after merge is complete, or null to not
   * validate the graph after merge. 
   * @param newValidator The validator to use after merge is complete, or null to not validate
   * the graph after merge. 
   */
  public Merger setValidator(Validator newValidator) { validator = newValidator; return this; }

  /** Layer shema being used */
  private Schema schema = null;

  /** Default edit-path comparator for annotations */
  private EditComparator<Annotation> defaultComparator
  = new EditComparator<Annotation>() {
      int NO_WAY = 200; // weight for ensuring they don't map to each other
      MinimumEditPathString stringComparator = new MinimumEditPathString();
      MinimumEditPathString stringComparatorAvoidSubstitution = new MinimumEditPathString(
        new DefaultEditComparator<Character>(1, 1, 2)); // change is more costly
      
      /**
       * Compares two sequence elements, and evaluates the distance between them.
       * @param from The element from the source sequence, which may be null,
       * @param to The element from the destination sequence, which may be null.
       * @return An edit step between the two elements. {@link EditStep#getFrom()} is set to
       * <var>from</var>, {@link EditStep#getTo()} is set to <var>to</var>,
       * {@link EditStep#getDistance()} is set to the computed edit distance between these two 
       * elements,and {@link EditStep#getOperation()} is set to either
       * <var>EditStep.StepOperation.NONE</var> or <var>EditStep.StepOperation.CHANGE</var>. 
       */
      public EditStep<Annotation> compare(Annotation a1, Annotation a2) {
        EditStep<Annotation> step = new EditStep<Annotation>(
          a1, a2, 0, EditStep.StepOperation.NONE);
        if (a1 == null) {
          if (a2 != null) {
            step.setStepDistance(1);
            step.setOperation(EditStep.StepOperation.CHANGE);
          }
          // if both are null, we fall through to the return, which amounts to no change
        } else if (a2 == null) {
          step.setStepDistance(1);
          step.setOperation(EditStep.StepOperation.CHANGE);
        } else { // two annotations to compare
          int iWeight = 0;

          if (HasCounterpart(a1) || HasCounterpart(a2)) { // already mapped
            if (!HasCounterpart(a1) || !HasCounterpart(a2) || GetCounterpart(a1) != a2) {
              // not mapped to each other
              iWeight += NO_WAY; // definitely don't want to map them
            }
            // else they're already mapped together, so iWeight = 0 is good.
          } else { // not already mapped
            Layer layer = a1.getLayer();
            // check labels (ignoring punctuation etc.)
            if (!a1.getLabel().equals(a2.getLabel())) {
              // ignore punctuation and case by default
              String s1 = a1.getLabel().replaceAll("[^\\p{javaLetter}\\p{javaDigit}]","").toLowerCase();
              String s2 = a2.getLabel().replaceAll("[^\\p{javaLetter}\\p{javaDigit}]","").toLowerCase();
              if (s1.length() <= 0 || s2.length() <= 0 
                  || (layer.containsKey("@type") 
                      && layer.getType().equals("ipa"))) { // phonological layer TODO formalise layer types
                s1 = a1.getLabel();
                s2 = a2.getLabel(); // for all-punctuation annotations
              }
              double distance = s1.length() <= 2.0 || s2.length() <= 2.0?
                // really short strings don't as easily allow subsitutions
                stringComparatorAvoidSubstitution.levenshteinDistance(s1, s2)
                // but longer strings use standard edit costs
                :stringComparator.levenshteinDistance(s1, s2);
              double magnifier = 1.0; // magnify this because anchor offsets also contribute
              // really short words have to be really similar
              if (s1.length() <= 2.0 || s2.length() <= 2.0) magnifier = 3.0; 
              iWeight += (distance * magnifier);
            } // check labels

            // don't compare anchors for graph tag layers (i.e. unaligned children of graph)
            // nor for tags of graph tags layers
            // (i.e. unaligned children of unaligned children of graph)
            LinkedHashSet<Layer> layerLineage = new LinkedHashSet<Layer>();
            layerLineage.add(layer);
            layerLineage.addAll(layer.getAncestors());
            boolean graphTagLayer = !layer.getId().equals("transcript"); // TODO schema.root.id
            for (Layer l : layerLineage) {
              if (l.getAlignment() != Constants.ALIGNMENT_NONE
                  && !l.getId().equals("transcript")) { // TODO schema.root.id
                graphTagLayer = false;
                break;
              }
            } // next layer in lineage
            if (!graphTagLayer) {
              // an instant cannot map to a non-instant
              if (a1.getInstantaneous() != a2.getInstantaneous()) {
                iWeight += NO_WAY;
              } else { // neither an instant, or both an instant
                // are all offsets available?
                if (a1.getAnchored() && a2.getAnchored()) {
                  // distance is as important as the reliability of the least reliable anchors
                  // i.e. if a1 & a2 have matching alignments
                  // (both default, both user-aligned, etc.)
                  // then the weight of the alignment is as heavy as the alignment
                  // but if a1 has default alignments and a2 has user-alignments,
                  // then importance is low because this is probably an alignment update
                  // or an unaligned update of aligned annotations
                  // alternatively, if the annotation has a mixture of trustworthyness,
                  // weight will be higher
                  // TODO for word layer, instead of comparing anchor offsets, penalize cases where the words' utterances don't overlap. That way, offset differences don't matter much within the utterance
                  double dImportance = Math.min(
                    (double)(GetConfidence(a1.getStart()) + GetConfidence(a1.getEnd())),
                    (double)(GetConfidence(a2.getStart()) + GetConfidence(a2.getEnd())))
                    // divided by CONFIDENCE_MANUAL, to make it near 1
                    / (double)(Constants.CONFIDENCE_MANUAL * 2);
                  // however, for "word" and "phone", which are frequently merged between aligned
                  // and unaligned versions, and which should be merged by label only 
                  // we ignore anchors
                  if (a1.getLayerId().equals(schema.getWordLayerId()) // word layer
                      // or (probably) phone layer
                      || (layer.getParentId().equals(schema.getWordLayerId())
                          && layer.getAlignment() == Constants.ALIGNMENT_INTERVAL)) {
                    dImportance = 0.0;
                  }			
                  // instantaneous annotations need to have more similar offsets than intervals
                  if (a1.getInstantaneous()) { // && a2.getInstantaneous(), but we know it must be
                    dImportance *= 2.0;
                  }
                  Double dDistance = a1.maxPairedDistance(a2);
                  if (dDistance != null && dDistance != 0) {	
                    // we want to ensure that overlapping annotations are selected over non-overlapping ones
                    if (dImportance > 0) {
                      if (dDistance > 0) { // no overlap
                        // prefer overlap over none
                        iWeight += (int)(dDistance * dImportance * 2);
                      } else {  // overlap
                        // when choosing between fragments of a split-up annotation, choose the 
                        // fragment that overlaps the most
                        double dOverlapMagnitude = Math.abs(a1.distance(a2))
                          // but if the length difference is great, make it high cost anyway
                          / ((a1.getDuration() + a2.getDuration())/2);
                        dOverlapMagnitude *= 3; // soften the impact of this magically
                        iWeight += (int)
                          (Math.min(
                            (double)NO_WAY, // make sure this tops out at NO_WAY, to avoid overflow
                            Math.abs((-dDistance * dImportance / dOverlapMagnitude))));
                      }
                    } else { 
                      // while distance doesn't contribute to the weight, REALLY BIG distances shouldn't map
                      if (dDistance > 0) { // no overlap
                        if (dDistance > 30) {
                          iWeight += NO_WAY;
                        } else if (Math.abs(a1.getDuration() - a2.getDuration()) > 10) {
                          // words differ in length by this much

                          iWeight += NO_WAY;
                        }
                      } else { // overlap - should be too different at all
                        if (-dDistance > 10)  iWeight += NO_WAY;
                      }
                    }
                  } // distant annotation
                } // all offsets are available
              } // neither an instant, or both an instant
            } // not a graph tag
          } // not already mapped
          
          step.setStepDistance(iWeight);
          if (!a1.getLabel().equals(a2.getLabel())) { // label would actually change
            step.setOperation(EditStep.StepOperation.CHANGE);
          }
        }
        return step;
      }
      
      /**
       * The distance for deleting the given element.
       * @param from The element that would be deleted, which may be null.
       * @return An edit step with {@link EditStep#getDistance()} set to the distance for
       * deleting the given element. {@link EditStep#getFrom()} is set to <var>from</var>, and
       * {@link EditStep#getOperation()} is set to <var>EditStep.StepOperation.DELETE</var> 
       */
      public EditStep<Annotation> delete(Annotation a1) {
        return new EditStep<Annotation>(a1, null, 1, EditStep.StepOperation.DELETE);
      }

      /**
       * The distance for inserting the given element.
       * @param from The element that would be inserted, which may be null.
       * @return An edit step with {@link EditStep#getDistance()} set to the distance for
       * inserting the given element. {@link EditStep#getTo()} is set to <var>to</var>, and
       * {@link EditStep#getOperation()} is set to <var>EditStep.StepOperation.INSERT</var> 
       */
      public EditStep<Annotation> insert(Annotation a2) {
        return new EditStep<Annotation>(null, a2, 1, EditStep.StepOperation.INSERT);
      }
    };

  protected HashSet<Anchor> dummyAnchors;
  protected HashSet<Anchor> dummyEditedAnchors;
   
  // Methods:
   
  /**
   * Default constructor.
   */
  public Merger() {
  } // end of constructor
   
  /**
   * Constructor with edited graph.
   * @param editedGraph The edited version of the graph.
   */
  public Merger(Graph editedGraph) {
    setEditedGraph(editedGraph);
  } // end of constructor
   
  // GraphTransformer method
   
  /**
   * Merges {@link #editedGraph} into the given graph.  
   * The changes are detected and applied to the <var>graph</var>, which is returned.
   * <p>Assumptions:
   * <ul>
   *  <li> {@link #editedGraph} represents a possibly partial 
   *       (i.e. with a subset of layers) version <var>graph</var> with some changes
   *       applied to it.</li> 
   *  <li> {@link #editedGraph} is valid and has a valid layer hierarchy (e.g. no orphaned
   *       utterances) - e.g. that {@link Validator} has been applied to it before merging.</li>
   *  <li> {@link #editedGraph} has no proposed changes (i.e. {@link Graph#commit()} has
   *       been called)</li> 
   *  <li> Participants are identified in both graphs using turn annotation labels,
   *   and this is done using the same annotation labels.</li>
   * </ul>
   * The IDs of Anchors and Annotations in <var>editedGraph</var> are not assumed to correspond
   * to IDs in <var>graph</var>. Only changes of equal or higher condifence
   * will be applied, so that automatic changes do not override prior manual ones.
   * Once merging is finished, <var>graph</var> may be in an invalid state, and should be made 
   * valid using {@link Validator}.
   * @param graph The graph to merge changes into.
   * @return The changes introduced by the tranformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph transform(Graph graph) throws TransformationException {
    if (debug) setLog(new Vector<String>());
    
    setErrors(new Vector<String>());
    schema = graph.getSchema();
    if (graph == editedGraph) return graph;
    if (editedGraph == null) {
      throw new TransformationException(
        this, "Edited graph is no set.", new NullPointerException());
    }

    ChangeTracker originalTracker = graph.getTracker();
    if (originalTracker == null) {
       // we must have a change tracker, because we might want to roll back changes
       graph.trackChanges();
    }
    // the bounds of the fragment cannot change
    Anchor originalGraphStart = null;
    Double originalStartOffset = null;
    Anchor originalGraphEnd = null;
    Double originalEndOffset = null;
    if (graph.isFragment()) {
      SortedSet<Anchor> anchors = graph.getAnchorsOrderedByStructure();
      if (anchors.size() > 0) {
        originalGraphStart = anchors.first();
        originalStartOffset = originalGraphStart.getOffset();
        originalGraphEnd = anchors.last();
        originalEndOffset = originalGraphEnd.getOffset();
      }
    }

    // ensure that all annotations have an anchor
    dummyAnchors = new HashSet<Anchor>();
    for (Annotation a : graph.getAnnotationsById().values()) {
      if (a.getStart() == null) {
        Anchor dummy = new Anchor(a.getStartId(), null, Constants.CONFIDENCE_NONE);
        dummyAnchors.add(dummy);
        graph.addAnchor(dummy);
      }
      if (a.getEnd() == null) {
        Anchor dummy = new Anchor(a.getEndId(), null, Constants.CONFIDENCE_NONE);
        dummyAnchors.add(dummy);
        graph.addAnchor(dummy);
      }
    }
    dummyEditedAnchors = new HashSet<Anchor>();
    for (Annotation a : editedGraph.getAnnotationsById().values()) {
      if (a.getStart() == null) {
        Anchor dummy = new Anchor(a.getStartId(), null, Constants.CONFIDENCE_NONE);
        dummyEditedAnchors.add(dummy);
        editedGraph.addAnchor(dummy);
      }
      if (a.getEnd() == null) {
        Anchor dummy = new Anchor(a.getEndId(), null, Constants.CONFIDENCE_NONE);
        dummyEditedAnchors.add(dummy);
        editedGraph.addAnchor(dummy);
      }
    }

    // ensure changes are ignored for selected layers
    for (String layerId : getNoChangeLayers()) {
      if (graph.getLayer(layerId) != null) 
        graph.getLayer(layerId).put("@noChange", Boolean.TRUE);
      if (editedGraph.getLayer(layerId) != null) 
        editedGraph.getLayer(layerId).put("@noChange", Boolean.TRUE);
    }

    Vector<Layer> topDownLayersInEditedGraph = graph.getLayersTopDown();
    Iterator<Layer> iLayersTopDown = topDownLayersInEditedGraph.iterator();
    while (iLayersTopDown.hasNext()) {
      Layer layer = iLayersTopDown.next();
      if (editedGraph.getLayer(layer.getId()) == null
          || layer.getId().equals("transcript")) { // TODO schema.root.id
        iLayersTopDown.remove();
      }
    } // next layer

    // phase 1. - map annotations in graph to annotations in editedGraph horizontally
    log("phase 1: map annotations");
    String rootLayerId = graph.getSchema().getRoot().getId();

    // map graphs together manually, to help top-level parent determination
    SetCounterparts(graph, editedGraph);

    // if participant/turn/utterance layers match perfectly, probably it's just a graph
    // reloaded after annotation of some other layer, so we map word tokens by utterance
    // instead of graph-wide, which saves resources and time, and reduces the risk of
    // weird mappings
    HashSet<String> alreadyMapped = new HashSet<String>();
    if (schema.getParticipantLayerId() != null
        && schema.getTurnLayerId() != null
        && schema.getUtteranceLayerId() != null
        && schema.getWordLayerId() != null) { // participant/turn/utterance layers are set

      // this doesn't work if  all words are unanchored
      Optional<Annotation> semiachoredUneditedWord = graph.every(schema.getWordLayerId())
        .filter(w -> w.getStart().getOffset() != null || w.getEnd().getOffset() != null)
        .findAny();
      Optional<Annotation> semiachoredEditedWord = editedGraph.every(schema.getWordLayerId())
        .filter(w -> w.getStart().getOffset() != null || w.getEnd().getOffset() != null)
        .findAny();
      if (semiachoredUneditedWord.isPresent() && semiachoredEditedWord.isPresent()) {

        // map participants
        mapByParents(schema.getParticipantLayer(), graph);
        alreadyMapped.add(schema.getParticipantLayerId());
        Optional<Annotation> unmappedUneditedParticipant
          = graph.every(schema.getParticipantLayerId())
          .filter(t -> !t.containsKey("@other"))
          .findAny();
        Optional<Annotation> unmappedEditedParticipant
          = editedGraph.every(schema.getParticipantLayerId())
          .filter(t -> !t.containsKey("@other"))
          .findAny();
        // are participants perfectly mapped?
        if (!unmappedUneditedParticipant.isPresent() && !unmappedEditedParticipant.isPresent()) {
        
          // map turns
          mapByParents(schema.getTurnLayer(), graph);
          alreadyMapped.add(schema.getTurnLayerId());
          Optional<Annotation> unmappedUneditedTurn = graph.every(schema.getTurnLayerId())
            .filter(t -> !t.containsKey("@other"))
            .findAny();
          Optional<Annotation> unmappedEditedTurn = editedGraph.every(schema.getTurnLayerId())
            .filter(t -> !t.containsKey("@other"))
            .findAny();
          // are turns perfectly mapped?
          if (!unmappedUneditedTurn.isPresent() && !unmappedEditedTurn.isPresent()) {
          
            // map utterances
            mapByParents(schema.getUtteranceLayer(), graph);
            alreadyMapped.add(schema.getUtteranceLayerId());
            Optional<Annotation> unmappedUneditedUtterance
              = graph.every(schema.getUtteranceLayerId())
              .filter(t -> !t.containsKey("@other"))
              .findAny();
            Optional<Annotation> unmappedEditedUtterance
              = editedGraph.every(schema.getUtteranceLayerId())
              .filter(t -> !t.containsKey("@other"))
              .findAny();
            // are utterances perfectly mapped?
            if (!unmappedUneditedUtterance.isPresent() && !unmappedEditedUtterance.isPresent()) {

              // // map words by turn, which allows words to move utterances
              // mapByParents(schema.getWordLayer(), graph);
              // alreadyMapped.add(schema.getWordLayerId());
              // System.err.println("WORDS MAPPED BY TURN " + graph.getId()); // TODO remove this

              // there's no re-partitioning of utterances in the edited graph - it probably
              // just contains new/adjusted annotations on other layers, so we map words by
              // utterance instead of by turn, saving time and memory

              // link utterances to their words
              graph.assignWordsToUtterances();
              editedGraph.assignWordsToUtterances();

              // all words have null offsets, this doesn't work

              // map words by utterance
              boolean thereWereUtteranceWords = false;
              for (Annotation utterance : graph.all(schema.getUtteranceLayerId())) {
                TreeSet<Annotation> uneditedWords 
                  = new TreeSet<Annotation>(new AnnotationComparatorByOrdinal());
                List<Annotation> words = (List<Annotation>)utterance.get("@words");
                if (words != null) {
                  uneditedWords.addAll(words);
              
                  Annotation editedUtterance = (Annotation)utterance.get("@other");
                  TreeSet<Annotation> editedWords 
                    = new TreeSet<Annotation>(new AnnotationComparatorByOrdinal());
                  words = (List<Annotation>)editedUtterance.get("@words");
                  if (words != null) {
                    editedWords.addAll(words);
                  
                    mapAnnotationsForMerge(
                      schema.getWordLayer(), uneditedWords, editedWords, rootLayerId);
                    thereWereUtteranceWords = true;
                  } // there were edited words
                
                  // don't need utterance/word assignment any more
                  editedUtterance.remove("@words");
                } // there were unedited words

                // don't need utterance/word assignment any more
                utterance.remove("@words");
              } // next parent
              if (thereWereUtteranceWords) {
                alreadyMapped.add(schema.getWordLayerId());
                log("words mapped by utterance");
                // patch up cases where words migrated from one utterance to the next/previous
                graph.every(schema.getWordLayerId())
                  .filter(t -> !t.containsKey("@other"))
                  .forEach(word -> {
                      // has the word migrated to the previous utterance?
                      Annotation previousWord = word.getPrevious();
                      if (previousWord != null && previousWord.containsKey("@other")) {
                        Annotation previousEditedWord = (Annotation)previousWord.get("@other");
                        Annotation nextEditedWord = previousEditedWord.getNext();
                        if (nextEditedWord != null // the previous edited word has a next
                            && nextEditedWord.getLabel().equals(word.getLabel())) { // labels match
                          SetCounterparts(word, nextEditedWord);
                          return;
                        }
                      } // there's a previous word
                      // has the word migrated to the next utterances?
                      Annotation nextWord = word.getNext();
                      if (nextWord != null && nextWord.containsKey("@other")) {
                        Annotation nextEditedWord = (Annotation)nextWord.get("@other");
                        Annotation previousEditedWord = nextEditedWord.getPrevious();
                        if (previousEditedWord != null // the next edited word has a previous
                            && previousEditedWord.getLabel().equals(word.getLabel())) { //labels match
                          SetCounterparts(word, previousEditedWord);
                        }
                      } // there's a next word
                    });
                // // do a final pass mapping by turn, to allow words to migrate to neighboring utterances
                // mapByParents(schema.getWordLayer(), graph);
                
              }
            
            } // utterances are perfectly mapped
          } // turns are perfectly mapped
        } // participants are perfectly mapped
      } // at least one word has an anchor with an offset
    } // participant/turn/utterance/word layers are set
    
    for (Layer layer : topDownLayersInEditedGraph) {

      // we may have already mapped this layer above
      if (alreadyMapped.contains(layer.getId())) continue;
      
      // if turn/utterance/word have not already been mapped above then
      // for direct descendents of 'turn' (i.e. 'utterance' and 'word')
      // allow mapping across the the whole graph, so that if the edits involve partitioning
      // turns differently, utterances/words can migrate to a neighboring turn
      
      if ((schema.getTurnLayerId() != null
           && schema.getTurnLayerId().equals(layer.getParentId()))
          // (or it's a top level layer)
          || layer.getParentId() == null) {

        TreeSet<Annotation> uneditedAnnotations 
          = new TreeSet<Annotation>(new AnnotationComparatorByOrdinal());
        uneditedAnnotations.addAll(Arrays.asList(graph.all(layer.getId())));
        
        TreeSet<Annotation> editedAnnotations 
          = new TreeSet<Annotation>(new AnnotationComparatorByOrdinal());
        editedAnnotations.addAll(Arrays.asList(editedGraph.all(layer.getId())));
	 
        mapAnnotationsForMerge(layer, uneditedAnnotations, editedAnnotations, rootLayerId);
      } else { // otherwise, only allow peers to map - i.e. annotations with the mapped parents
        mapByParents(layer, graph);
      } // not a turn child layer (utterance or word)
    } // next layer

    // phase 2. - reconcile unmapped annotations
    log("phase 2: reconcile unmapped annotations");
      
    for (Layer layer : topDownLayersInEditedGraph) {
      if (!getNoChangeLayers().contains(layer.getId())) {
        // creation/destruction is allowed on this layer
         createDestroyAnnotationsForMerge(layer, graph);
      } else {
        log("Skipping ", layer);
      }
    } // next layer

    // phase 3. - compute label deltas
    log("phase 3: label deltas");

    for (Layer layer : topDownLayersInEditedGraph) {
      if (!getNoChangeLayers().contains(layer.getId())) {
        // changing labels is allowed on this layer
         computeLabelDeltasForMerge(layer, graph);
      } else {
        log("Skipping ", layer);
      }
    } // next layer

    // TODO link utterances to words, and if a word's old utterance bounds are incompatible with the new bounds, unset word child offsets
    // TODO i.e. if the segment alignments were really bad, and then the utterance boundaries are refined,
    // TODO the new utterance boundaries take precedence over the old segment boundaries

    // phase 4. - compute anchor deltas horizontally
    log("phase 4: anchor deltas");
    // use the coarsest granularity of the graphs when comparing offsets
    if (graph.getOffsetGranularity() != null) {
      if (editedGraph.getOffsetGranularity() == null
          || editedGraph.getOffsetGranularity() < graph.getOffsetGranularity()) {
        editedGraph.setOffsetGranularity(graph.getOffsetGranularity());
      }
    }
    // construct a bottom up list of layers, to ensure children unshare from parents, 
    // not vice-versa
    // and also layers that are parents first
    // - so word before turn  before phone/pos before language/utterance
    Vector<Layer> bottomUpLeavesLastInEditedGraph = new Vector<Layer>();
    ListIterator<Layer> liLayersTopDown = topDownLayersInEditedGraph.listIterator();
    while (liLayersTopDown.hasNext()) liLayersTopDown.next(); // move to the end
    // add parent layers
    while (liLayersTopDown.hasPrevious()) {
      Layer layer = liLayersTopDown.previous();
      if (layer.getChildren().size() > 0) bottomUpLeavesLastInEditedGraph.add(layer);
    } // previous layer
    while (liLayersTopDown.hasNext()) liLayersTopDown.next(); // move to the end
    // add childless layers
    while (liLayersTopDown.hasPrevious()) {
      Layer layer = liLayersTopDown.previous();
      if (layer.getChildren().size() == 0) bottomUpLeavesLastInEditedGraph.add(layer);
    } // previous layer
    for (Layer layer : bottomUpLeavesLastInEditedGraph) {
      // there's no need to compute anchor changes for unaligned layers
      if (layer.getAlignment() != Constants.ALIGNMENT_NONE) {
         computeAnchorDeltasForMerge(layer, graph);
      }
    } // next layer
    // untag annotations tagged during this phase
    for (Annotation a : graph.getAnnotationsById().values()) {
      a.remove("@computeAnchorDeltasForMerge");
    }

    // phase 5. - check new order by offset, and check new containment
    log("phase 5: check hierarchy");

    // in this phase out-of-order children are detected and fixed
    // also the edited t-including of children is checked in the original graph, and
    // children moved accordingly
    // also 'partition' peer layers are checked, to ensures that t-includes relationships
    // that existed before merge exist afterwards - e.g. words are in the same utterance
    // in both versions of the graph
    // top-down to ensure that words are checked before segments, otherwise segments
    // are internally ok, and then the words get changed afterwards

    for (Layer layer : graph.getLayersTopDown()) { // all layers in graph 
       checkChildrenForMerge(layer, graph);
    } // next layer

    // remove any dummy anchors before validation
    // (for merging fragments, it's important they're not there)
    for (Anchor dummy : dummyAnchors) graph.getAnchors().remove(dummy.getId());
    for (Anchor dummy : dummyEditedAnchors) editedGraph.getAnchors().remove(dummy.getId());

    if (validator != null) {
      log("phase 6: validate");
      validator.setDebug(getDebug());
      try {
        validator.transform(graph);
      } catch (TransformationException x) {
        // record details before passing exception up the stack...
        log("validation failed: " + x);
        if (log != null) log.addAll(validator.getLog());
        errors.addAll(validator.getErrors());
        throw x;
      }
      if (log != null) log.addAll(validator.getLog());
      errors.addAll(validator.getErrors());
      log("phase 6 finished");
    } else {
      log("phase 6: no validator");
    }

    // remove any new but unreferenced anchors
    for (Anchor a : new Vector<Anchor>(graph.getAnchors().values())) {
      if (a.getChange() == Change.Operation.Create
          && a.getStartingAnnotations().size() == 0
          && a.getEndingAnnotations().size() == 0) {
         a.destroy();
        graph.getAnchors().remove(a.getId());
      }
    } // next anchor

    // unlink counterparts, so that either graph can be garbage-collected with the other still referenced
    for (Annotation a : graph.getAnnotationsById().values()) UnsetCounterparts(a);

    // remove layer tags
    for (String layerId : getNoChangeLayers()) {
      if (graph.getLayer(layerId) != null) 
        graph.getLayer(layerId).remove("@noChange");
      if (editedGraph.getLayer(layerId) != null) 
        editedGraph.getLayer(layerId).remove("@noChange");
    }

    // ensure the graph bounds haven't changed
    if (originalStartOffset != null
        && !originalStartOffset.equals(originalGraphStart.getOffset())) {
      originalGraphStart.setOffset(originalStartOffset);
    }
    if (originalEndOffset != null
        && !originalEndOffset.equals(originalGraphEnd.getOffset())) {
      originalGraphEnd.setOffset(originalEndOffset);
    }

    // remove tracker if we added it
    if (originalTracker == null) {
       graph.setTracker(null);
    }
    return graph;
  }
  
  /**
   * Convenience function that maps together unedited and edited annotations on a give
   * layer, by parent annotation. 
   * @param layer The layer to map
   * @param graph The graph the unedited parents come from (edited parents are the
   * "@other" attribute of each parent)
   */
  public void mapByParents(Layer layer, Graph graph) throws TransformationException {
    for (Annotation parent : graph.all(layer.getParentId())) {
      if (parent.containsKey("@other")) {
        TreeSet<Annotation> uneditedAnnotations 
          = new TreeSet<Annotation>(new AnnotationComparatorByOrdinal());
        SortedSet<Annotation> annotations = parent.getAnnotations(layer.getId());
        if (annotations != null) {
          uneditedAnnotations.addAll(annotations);
        
          Annotation editedParent = (Annotation)parent.get("@other");
          TreeSet<Annotation> editedAnnotations 
            = new TreeSet<Annotation>(new AnnotationComparatorByOrdinal());
          annotations = editedParent.getAnnotations(layer.getId());
          if (annotations != null) {
            editedAnnotations.addAll(annotations);
        
            mapAnnotationsForMerge(
              layer, uneditedAnnotations, editedAnnotations, graph.getSchema().getRoot().getId());
          } // there were edited children
        } // there were unedited children
      } // the parent has a counterpart in the edited graph
    } // next parent
  } // end of mapByParents()
   
  /**
   * PHASE 1: Maps annotations from another fragment to annotations in this fragment, in order to
   * then compute change deltas. 
   * <p> Corresponding annotations in each graph are linked by having the "@other" attribute set.
   * <p> Only annotations with the same participant (if any) can be linked as counterparts.
   * @param layer Layer definition to use.
   * @param these Annotations from one layer in the graph to be merged into.
   * @param those Annotations from the same layer in {@link #editedGraph}.
   * @param rootLayerId The layer ID of the root layer - e.g. "transcript"
   * @throws TransformationException On error.
   */
  public void mapAnnotationsForMerge(
    Layer layer, SortedSet<Annotation> these, SortedSet<Annotation> those, String rootLayerId)
    throws TransformationException {
    HashMap<String,Vector<Annotation>> theseByParticipant
      = new HashMap<String,Vector<Annotation>>();
    HashMap<String,Vector<Annotation>> thoseByParticipant
      = new HashMap<String,Vector<Annotation>>();
    // check if this layer has participant assigned
    boolean splitByParticipant = false;
    // if the schema specifies a turn layer
    if (layer.getId().equals(schema.getTurnLayerId())) {
      splitByParticipant = true;
    } else {
      if (schema.getTurnLayerId() != null
          // and that layer is present in editedGraph
          && editedGraph.getLayer(schema.getTurnLayerId()) != null) {
        for (Layer ancestor : layer.getAncestors()) {
          if (ancestor.getId().equals(schema.getTurnLayerId())) {
            splitByParticipant = true;
            break;
          }
        } // next ancestor
      } // turn layer is defined for this schema
    }
    if (!splitByParticipant) { // just two straight lists
      theseByParticipant.put("", new Vector<Annotation>(these));
      thoseByParticipant.put("", new Vector<Annotation>(those));
    } else { // split annotations out by participant
      // annotations from different participants are never paired, so split the lists by 
      // participant and then map them 
      // - this way, simultaneous speech doesn't turn into a whole bunch of
      //   unnecessary adds and deletes
      // - this should improve memory usage too, because, with luck,
      //   the two lists to get minimum edit path from are shorter in each case
	 
      for (Annotation an : these) {
        String who = "";
        Annotation turn = layer.getId().equals(schema.getTurnLayerId())?
          an:an.first(schema.getTurnLayerId());
        if (turn != null) who = turn.getLabel();
        if (!theseByParticipant.containsKey(who)) {
          theseByParticipant.put(who, new Vector<Annotation>());
        }
        theseByParticipant.get(who).add(an);
      } // next annotation
      for (Annotation an : those) {
        String who = "";
        Annotation turn = layer.getId().equals(schema.getTurnLayerId())?
          an:an.first(schema.getTurnLayerId());
        if (turn != null) who = turn.getLabel();
        if (!thoseByParticipant.containsKey(who)) {
          thoseByParticipant.put(who, new Vector<Annotation>());
        }
        thoseByParticipant.get(who).add(an);
      } // next annotation
      // ensure both collections have the same keys
      for (String who : theseByParticipant.keySet()) 
        if (!thoseByParticipant.containsKey(who)) 
          thoseByParticipant.put(who, new Vector<Annotation>());
      for (String who : thoseByParticipant.keySet()) 
        if (!theseByParticipant.containsKey(who)) 
          theseByParticipant.put(who, new Vector<Annotation>());
    } // split out annotations by participant

    for (String who : theseByParticipant.keySet()) {
      Vector<Annotation> theseForWho = theseByParticipant.get(who);
      Vector<Annotation> thoseForWho = thoseByParticipant.get(who);

      // if it's a graph tag layer, sort annotations by label
      // this ensures, e.g., that mapping the "who" layer lines the speakers up by name
      if ((layer.getParentId() == null || rootLayerId.equals(layer.getParentId()))
          && layer.getAlignment() == Constants.ALIGNMENT_NONE) {
        AnnotationComparatorByOrdinal byLabel = new AnnotationComparatorByOrdinal() {
            public int compare(Annotation o1, Annotation o2) {
              int labelComparison = o1.getLabel().compareTo(o2.getLabel());
              if (labelComparison != 0) return labelComparison;
              return super.compare(o1, o2);
            }
          };
        TreeSet<Annotation> annotationsByLabel = new TreeSet<Annotation>(byLabel);
        annotationsByLabel.addAll(theseByParticipant.get(who));
        theseForWho = new Vector<Annotation>(annotationsByLabel);
        annotationsByLabel.clear();
        annotationsByLabel.addAll(thoseByParticipant.get(who));
        thoseForWho = new Vector<Annotation>(annotationsByLabel);
      }

      // break collections into overlapping chunks to conserve memory

      // two lists of lists of annotations
      Vector<Vector<Annotation>> theses = new Vector<Vector<Annotation>>();
      Vector<Vector<Annotation>> thoses = new Vector<Vector<Annotation>>();
	 
      // if we're under size, or we're not doing chunking, just process the two given lists
      boolean overlappingChunking = getArraySizeLimit() > 0 
        && theseForWho.size() > arraySizeLimit && thoseForWho.size() > arraySizeLimit;
      if (!overlappingChunking) {
        theses.add(theseForWho);
        thoses.add(thoseForWho);
      } else { // oversize
        // break the lists into proportionate chunks that overlap
        int iNumberOfDiscreteChunks = (theseForWho.size() + (arraySizeLimit-1)) / arraySizeLimit;
        iNumberOfDiscreteChunks = Math.max(
          iNumberOfDiscreteChunks, (thoseForWho.size() + (arraySizeLimit-1)) / arraySizeLimit);
        theses = breakIntoOverlappingChunks(theseForWho, iNumberOfDiscreteChunks);
        thoses = breakIntoOverlappingChunks(thoseForWho, iNumberOfDiscreteChunks);	    
        assert theses.size() == thoses.size() 
          : "theses.size() == thoses.size(): " 
          + theseForWho.size() + " (" + theses.size() + ") " 
          + thoseForWho.size() + " (" + thoses.size() + ") - " + iNumberOfDiscreteChunks;	 
      } // oversize list

      // for each pair of lists
      Enumeration<Vector<Annotation>> enThese = theses.elements();
      Enumeration<Vector<Annotation>> enThose = thoses.elements();
      while (enThese.hasMoreElements()) {
        // map the list elements
        Vector<Annotation> theseAnnotations = enThese.nextElement();
        Vector<Annotation> thoseAnnotations = enThose.nextElement();

        if (overlappingChunking) {
          // the purpose of using overlapping chunks is to enable mapping trailing elements
          // from one chunk to leading elements in the next chunk. in order to prevent
          // internal unmapped elements from mapping on a second pass, the already-mapped
          // section at the beginning of each list is removed
          // - i.e. all leading annotations up to the last mapped one
          removeLeadingMappedAnnotations(theseAnnotations);
          removeLeadingMappedAnnotations(thoseAnnotations);
        }

        // find minimum edit distance
        MinimumEditPath<Annotation> mp = new MinimumEditPath<Annotation>(defaultComparator);
        List<EditStep<Annotation>> path = mp.minimumEditPath(theseAnnotations, thoseAnnotations);
        // introduce mapped annotations to each other
        if (layer.containsKey("@noChange") // no change layer
            || (layer.getParentId() != null // or word child layer - segments resist deletion
                && layer.getParentId().equals(schema.getWordLayerId()))) {
          log("Collapsing edit path for " + layer);
          mp.collapse(path, false);
        }
        log("PATH " + layer);
        for (EditStep<Annotation> step : path) {
          if (step.getFrom() != null && step.getTo() != null) {
            SetCounterparts(step.getFrom(), step.getTo());
          }
          log(step.getFrom(), " ", step.getOperation(), " ", step.getTo(), " - ", step.getStepDistance());
        }
      } // next chunk pair
    } // next who
  } // end of mapAnnotationsForMerge()
   
  /**
   * PHASE 2: Create new annotations we don't have that exist in the other graph, and mark
   * annotations that don't exist in the other graph for deletion.
   * <p>This method assumes that {@link #mapAnnotationsForMerge(Layer,SortedSet,SortedSet,String)}
   * has already been called and thus annotations have had their "@other" attributes set
   * appropriately.
   * <p>New annotations are also given new anchors, and if surrounding annotations (on the same
   * layer) share anchors in the edited version, then corresponding surrounding annotations in
   * the original will be linked to the new anchors.
   * @param layer The layer to traverse.
   * @param graph The graph to add changes to.
   * @throws TransformationException On error.
   */
  protected void createDestroyAnnotationsForMerge(Layer layer, Graph graph)
    throws TransformationException {
    String layerId = layer.getId();
    // unmapped annotations in graph are for deletion
    graph.every(layer.getId())
      .filter(((Predicate<Annotation>)(Merger::HasCounterpart)).negate())
      .forEach(an -> {
          log(layerId, ": Deleting ", an);
          an.destroy();
        }); // next annotation

    // might need these later:
    String saturatedParentLayerId = layer.getSaturated()?layer.getParentId():null;
    HashSet<String> thisAndsaturatedParentLayerId = new HashSet<String>();
    thisAndsaturatedParentLayerId.add(layerId);
    if (saturatedParentLayerId != null) thisAndsaturatedParentLayerId.add(saturatedParentLayerId);
    HashSet<String> thisLayerId = new HashSet<String>();
    thisLayerId.add(layerId);
      
    // unmapped annotations of theirs are for addition 
    Annotation anLastOriginal = null;
    for (Annotation anEdited : editedGraph.all(layerId)) {
      if (!HasCounterpart(anEdited)) {
        // create a new annotation
        Annotation newAnnotation = new Annotation(null, anEdited.getLabel(), layerId);
        newAnnotation.setConfidence(anEdited.getConfidence());
        newAnnotation.put("@other", anEdited);
        newAnnotation.create();

        // anchor it...
        Anchor start = new Anchor(anEdited.getStart());

        start.create();
        if (GetConfidence(anEdited.getStart()) < Constants.CONFIDENCE_AUTOMATIC) {
          // mark for realignment
          SetConfidence(start, Constants.CONFIDENCE_NONE);
        }

        Anchor end = new Anchor(anEdited.getEnd());
        end.create();
        if (GetConfidence(anEdited.getEnd()) < Constants.CONFIDENCE_AUTOMATIC) {
          // mark for realignment
          SetConfidence(end, Constants.CONFIDENCE_NONE);
        }
        if (anEdited.getInstantaneous()) { // instantaneous annotation
          end = start;
        } else { // annotation with duration
          // don't look for links for instants, because unlinking them from unlinked annotations later is tricky

          // is the start anchor shared in the edited structure?
          Optional<Annotation> otherParallel = anEdited.getStart().startingAnnotations()
            .filter(a -> a != anEdited)
            .filter(a -> a.getStartId().equals(a.getOriginalStartId())) // isn't the same any more
            .filter(Merger::HasCounterpart)
            .map(a -> GetCounterpart(a))
            .findAny();
          if (otherParallel.isPresent()) {
            start = otherParallel.get().getStart();
            log(layerId, ": ", newAnnotation.getLabel(), 
                " sharing start with linked ", otherParallel.get());
          }

          // is the end anchor shared in the edited structure
          otherParallel = anEdited.getEnd().endingAnnotations()
            .filter(a -> a != anEdited)
            .filter(a -> a.getEndId().equals(a.getOriginalEndId())) // isn't the same any more
            .filter(Merger::HasCounterpart)
            .map(a -> GetCounterpart(a))
            .findAny();
          if (otherParallel.isPresent()) {
            end = otherParallel.get().getEnd();
            log(layerId, ":", newAnnotation.getLabel(),
                " sharing end with linked ", otherParallel.get());
          }
        } // annotation with duration
        if (start.getId() == null) {
          graph.addAnchor(start);
        }
        newAnnotation.setStartId(start.getId());
        if (end.getId() == null) {
          graph.addAnchor(end);
        }
        newAnnotation.setEndId(end.getId());

        // set parent/peer annotations
        Annotation editedParent = anEdited.getParent();
        if (editedParent != null && HasCounterpart(editedParent)) { // counterpart parent set
          Annotation otherParent = GetCounterpart(editedParent);
          newAnnotation.setParentId(otherParent.getId());
        } else { // counterpart parent not set
          // for turns, look for the participant with the same label
          if (layerId.equals(schema.getTurnLayerId()) 
              && schema.getParticipantLayerId() != null) {
            Optional<Annotation> participant = graph.every(schema.getParticipantLayerId())
              .filter(p -> p.getLabel().equals(newAnnotation.getLabel()))
              .findAny();
            if (participant.isPresent()) {
              newAnnotation.setParentId(participant.get().getId());
            }
          }
        } // counterpart parent not set
	    
        // relink previous annotation to the new anchors? (aligned layers only)
        // in the edited version of the graph, 
        // does this annotation share an anchor with some prior annotation?	    
        if (layer.getAlignment() == Constants.ALIGNMENT_INTERVAL) {
          // look for one that's not necessarily the very last one
          Optional<Annotation> anPreviousOriginal = anEdited.getStart()
            .endingAnnotations(layerId)
            .filter(a -> a != anEdited)
            .filter(Merger::HasCounterpart)
            .map(Merger::GetCounterpart) // now annotation in original graph
            // avoid bridging a gap across a parent annotation that's not being deleted
            .filter(previousOriginal -> {
                Annotation previousOriginalParent = previousOriginal.getParent();
                if (previousOriginalParent != null
                    && HasCounterpart(editedParent)) {
                  Annotation originalParent = GetCounterpart(editedParent);
                  if (previousOriginalParent != originalParent) { // not peers
                    Annotation originalParentPrevious = originalParent.getPrevious();
                    if (originalParentPrevious != null
                        && previousOriginalParent != originalParentPrevious
                        && Annotation.NotDestroyed(originalParentPrevious)) {
                      // there is an intervening annotation on the parent layer
                      log("Don't link previous: ", previousOriginal,
                          " across intervening parent: ", originalParentPrevious,
                          " to: ", anEdited);
                      return false;
                    }
                  }
                }
                return true;
              })
            .findAny();
          if (anPreviousOriginal.isPresent()) {
            Annotation anPreviousEditedOther = anPreviousOriginal.get();
            if (GetConfidence(anPreviousEditedOther.getEnd()) > GetConfidence(start)
                && anPreviousEditedOther.getEnd().getOffset() != null
                && (start.getOffset() == null
                    || anPreviousEditedOther.getEnd().getOffset().doubleValue()
                    != start.getOffset().doubleValue())) {
              log(layerId, ": Use offset of end of prior: ", anLastOriginal,
                  " (end ", anPreviousEditedOther.getEnd(), ")",
                  " for start of new: ", newAnnotation,
                  " (start ", newAnnotation.getStart(), ")");
              int newStatus = Math.min(
                GetConfidence(graph.getAnchor(anPreviousEditedOther.getOriginalEndId())),
                GetConfidence(start));
              if (newStatus <= Constants.CONFIDENCE_DEFAULT) {
                newStatus = Math.max(
                  GetConfidence(graph.getAnchor(anPreviousEditedOther.getOriginalEndId())),
                  GetConfidence(start));
              }
              if (GetConfidence(end) > Constants.CONFIDENCE_DEFAULT 
                  && end.getOffset() <= anPreviousEditedOther.getEnd().getOffset()) {
                newStatus = Constants.CONFIDENCE_NONE;
              }
              if (start.getChange() == Change.Operation.Create
                  && newStatus > Constants.CONFIDENCE_AUTOMATIC) {
                // using a new anchor, and replacing a manual anchor
                // set the original attributes instead of a delta
                // this is so it can't later be reverted to its 'new' values
                // TODO was this necessary? start.commit();
                start.create();
              }
              start.setOffset(anPreviousEditedOther.getEnd().getOffset());
              SetConfidence(start, newStatus);
            } // previous more reliable
            if (anPreviousEditedOther.getEnd() != start) {			
              log(layerId, ": Share anchor with prior: ", anLastOriginal,
                  " and new: ", newAnnotation, " using ", start);
              changeEndWithRelatedAnnotations(
                anPreviousEditedOther, start,
                // we don't re-link a related parent annotation - we might be inserting a new child
                thisAndsaturatedParentLayerId);
            }
          } // anPreviousOriginal.isPresent()
        } // interval layer  
        graph.addAnnotation(newAnnotation);
        log(layerId, ": Adding ", newAnnotation);
        SetCounterparts(newAnnotation, anEdited);
      } else { // existing annotation
        // check whether this anchor should be unlinked from prior ones that are not linked
        // in the edited graph
        Annotation anOriginal = GetCounterpart(anEdited);
        anOriginal.getStart().endingAnnotations(layerId)
          .filter(Annotation::NotDestroyed)
          .filter(Merger::HasCounterpart)
          // linked in the edited graph?
          .filter(a -> GetCounterpart(a).getEnd() != anEdited.getStart())
          .collect(Collectors.toList()) // add to new collection to avoid concurrent modification
          .forEach(anOriginalLinkedPrior -> {
              // unlink the prior annotation from this one
              final Anchor originalStart = anOriginal.getStart();
              Anchor newPriorEndAnchor = new Anchor(anOriginal.getStart());
              newPriorEndAnchor.create();
              graph.addAnchor(newPriorEndAnchor);
              log(layerId, ": Unsharing end of prior: ", 
                  anOriginalLinkedPrior, " and start of ", anOriginal);
              changeEndWithRelatedAnnotations(anOriginalLinkedPrior, newPriorEndAnchor, thisLayerId);
	       
            });
	    
        if (anLastOriginal != null && anLastOriginal.getChange() == Change.Operation.Create) {
          // mapped to an original annotation, and the last one was new
          // relink this annotation to new anchor?
          // in the edited version of the graph,
          // does this annotation share an anchor with the last annotation?	    
          if (GetCounterpart(anLastOriginal).getEnd() == anEdited.getStart()
              && anLastOriginal.getEnd() != anOriginal.getStart()) {
            // we prefer to use an original anchor (i.e. anOriginal.start)

            // however we avoid creating an inappropriate parallel annotation
            if (anLastOriginal.getStart() != anOriginal.getStart()
                // but also will use last.end when parent start sharing mismatches
                && (anEdited.getStart() == anEdited.getParent().getStart()
                    || anOriginal.getStart() != anOriginal.getParent().getStart())) {
              log(layerId, ": Share anchor with next: ",
                  anLastOriginal, " and ", anOriginal);

              boolean bLastIsAnInstant = anLastOriginal.getInstantaneous(); // TODO or maybe just by offset?
              // check we're not creating an instant or backward annotation
              if (anLastOriginal.getStart().getOriginalOffset() != null
                  && anOriginal.getStart().getOriginalOffset() != null
                  && anLastOriginal.getStart().getOriginalOffset()
                  >= anOriginal.getStart().getOriginalOffset()
                  && !bLastIsAnInstant) {
                if (GetConfidence(anLastOriginal.getStart()) // TODO should be original confidence, but we don't track that!
                    < GetConfidence(anOriginal.getStart())
                    // don't go back before parent start
                    && anLastOriginal.getParent().getStart() != null
                    && anLastOriginal.getParent().getStart().getOffset() != null
                    && anLastOriginal.getParent().getStart().getOffset()
                    <= anOriginal.getStart().getOffset() - smidgin) {
                  double dNewOffset = anOriginal.getStart().getOffset() - smidgin;
                  log(layerId, ": Moving start of : ", 
                      anLastOriginal, " (",
                      GetConfidence(anLastOriginal.getStart()), "vs",
                      GetConfidence(anOriginal.getStart()), ") to ",
                      dNewOffset, " to avoid non-positive length for ", anLastOriginal);
                  anLastOriginal.getStart().setOffset(dNewOffset);
                  SetConfidence(anLastOriginal.getStart(), Constants.CONFIDENCE_NONE);
                } else if (GetConfidence(anLastOriginal.getStart()) // TODO should be original confidence, but we don't track that!
                           > GetConfidence(anOriginal.getStart())
                           // keeping offsets is important only for anchors that have been aligned
                           || GetConfidence(anOriginal.getStart()) <= Constants.CONFIDENCE_DEFAULT) {
                  double dNewOffset = anOriginal.getStart().getOffset() + smidgin;
                  log(layerId, ": Moving start of : ", anOriginal, " to ",
                      dNewOffset, " (",
                      GetConfidence(anLastOriginal.getStart()), "vs",
                      GetConfidence(anOriginal.getStart()), ") to avoid non-positive length for ", anLastOriginal);
                  anOriginal.getStart().setOffset(dNewOffset);
                  SetConfidence(anOriginal.getStart(), Constants.CONFIDENCE_NONE);
                } else {
                  log(layerId, ": Creating non-positive length for ", anLastOriginal,
                      " because both anchors are high confidence: ",
                      GetConfidence(anLastOriginal.getStart()), "vs",
                      GetConfidence(anOriginal.getStart()));
                }
              }

              // ensure that the end anchor for the last annotation is updated
              // we don't re-link a related parent annotation - we might be inserting a new child
              changeEndWithRelatedAnnotations(
                anLastOriginal, anOriginal.getStart(), saturatedParentLayerId);
              if (bLastIsAnInstant) {
                changeStartWithRelatedAnnotations(anLastOriginal, anOriginal.getStart());
              }
            } // anLastOriginal.getStart() != anOriginal.getStart()
            else { // anLastOriginal.getStart() == anOriginal.getStart()
              // we don't generally do the following because it screws up annotation
              // insertion when the new, low-status anchors are far from the
              // old, high-status anchors
              log(layerId, ": Share anchor with previous: ", anLastOriginal, " and ", anOriginal);
              // we don't re-link a related parent annotation - we might be inserting a new child
              changeStartWithRelatedAnnotations(
                anOriginal, anLastOriginal.getEnd(), saturatedParentLayerId);
            } // anLastOriginal.getStart() == anOriginal.getStart()		  
          } // last annotation and this one are (now) linked	    
        } // mapped, and the last one was a change
      } // not a new annotation
	 
      // copy ordinals - these will be updated later, but this ensures that annotations
      // come out in order for following operations, despite crazy anchor values
      // TODO test case for this - insert Anew before Aold, they have same ordinals but Anew.start > original Aold.start
      Annotation anOriginal = GetCounterpart(anEdited);
      if (anOriginal.getOrdinal() != anEdited.getOrdinal()) {
        log(layerId, ": changing ordinal of: ", anOriginal,
            " from ", anOriginal.getOrdinal(), " to ", anEdited.getOrdinal());
        anOriginal.setOrdinal(anEdited.getOrdinal());
      }
   
      anLastOriginal = anOriginal;
    } // next edited annotation
  } // end of createDeleteAnnotationsForMerge()
   
  /**
   * Break the given list into overlapping chunks using the given chunk size.
   * @param list The list to break up.
   * @param iNumberOfDiscreteChunks Number of chunks the results list of chunks should have
   * @return A list of chunks
   */
  protected Vector<Vector<Annotation>> breakIntoOverlappingChunks(
    Vector<Annotation> list, int iNumberOfDiscreteChunks) {
    Vector<Vector<Annotation>> vv = new Vector<Vector<Annotation>>();
    Vector<Annotation> va = new Vector<Annotation>();
    vv.add(va);
    Vector<Annotation> vb = null;
    double iChunkSize = ((double)list.size()) / iNumberOfDiscreteChunks;
    double iChangeSize = iChunkSize / 2;
    int i = 0;
    Iterator<Annotation> it = list.iterator();
    while (it.hasNext()) {
      Annotation an = it.next();
      if (i > vv.size() * iChangeSize) {
        if (vb == null) {
          vb = new Vector<Annotation>();
          vv.add(vb);
        } else {
          va = vb;
          vb = new Vector<Annotation>();
          vv.add(vb);
        }
      }
      i++;
	 
      va.add(an);
      if (vb != null) vb.add(an);
    } // next annotation
    vv.remove(vv.lastElement());  // remove the last half-chunk
    return vv;
  } // end of breakIntoOverlappingChunks()

  /**
   * Removes leading annotations from the list, up to and including the last mapped annotation
   * (i.e. annotation with a non-null counterpart)
   * @param annotations A list of annotations
   */
  public void removeLeadingMappedAnnotations(Vector<Annotation> annotations) {
    if (annotations.size() == 0) return;

    // Start from the end
    ListIterator<Annotation> iAnnotations = annotations.listIterator(annotations.size()-1);
    iAnnotations.next();

    // go backwards until a mapped annotation is found
    while (iAnnotations.hasPrevious()) {
      if (HasCounterpart(iAnnotations.previous())) {
        iAnnotations.remove();
        break;
      }
    }

    // remove all annotations from here until the beginning
    while (iAnnotations.hasPrevious()) {
      iAnnotations.previous();
      iAnnotations.remove();
    }
  } // end of removeLeadingMappedAnnotations()

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
   * Sets the Start Anchor of the given annotation, and also the start anchors of related
   * annotations that start in the same place. 
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
    log("changeStartWithRelatedAnnotations ", 
        annotation, " to ", newStartAnchor,
        (layerIdsToExclude.size() > 0?" excluding layers ":""), layerIdsToExclude);
      
    Anchor aOriginalStart = annotation.getStart();
    Anchor aOriginalEnd = annotation.getEnd();      
    annotation.setStart(newStartAnchor);
    if (aOriginalStart == aOriginalEnd) {
      log(annotation, " is instantaneous, changing both anchors.");
      annotation.setEnd(newStartAnchor);
    }
      
    final Layer layer = annotation.getGraph().getLayer(annotation.getLayerId());
      
    // change parallel annotations
    aOriginalStart.startingAnnotations()
      .filter(a -> a != annotation)
      .filter(a -> !a.getLayerId().equals(annotation.getLayerId()))
      .filter(a -> !layerIdsToExclude.contains(a.getLayerId()))
      .filter(a -> aOriginalStart.getId().equals(a.getStartId()))
      .filter(a -> {
          // do they have a relationship that would actually preclude sharing?
          Layer otherLayer = annotation.getGraph().getLayer(a.getLayerId());
          if (layer != null && otherLayer != null) {
            if (layer.getParentId().equals(otherLayer.getId())) {
              // other is parent layer to this
              
              // this belongs to another parent
              if (!a.getId().equals(annotation.getParentId())) return false;
              
            } else if (otherLayer.getParentId().equals(layer.getId())) {
              // this is parent layer to other
	       
              // other belongs to another parent
              if (!annotation.getId().equals(a.getParentId())) return false;              
            }
          }
          return true;
        })
      .collect(Collectors.toList()) // add to new collection to avoid concurrent modification
      .forEach(a -> {
          boolean bInstant = a.getInstantaneous();
          log("Changing start", (bInstant?" and end":""), " of related annotation ", a, 
              " to ", newStartAnchor);
          a.setStart(newStartAnchor);
          if (bInstant) {
            a.setEnd(newStartAnchor);
          }
        });

    if (!layerIdsToExclude.contains(annotation.getLayerId())) {
      // also change end anchor of annotations on the same layer
      layerIdsToExclude.add(annotation.getLayerId()); // prevents infinite recursion
      aOriginalStart.endingAnnotations(annotation.getLayerId())
        .filter(Annotation::NotDestroyed)
        .filter(a -> a.getEndId().equals(aOriginalStart.getId()))
        .filter(a -> {
            if (!a.getStartId().equals(a.getEndId())
                && newStartAnchor.getId().equals(a.getStartId())) {
              log("Not changing end of related annotation ", a, " to avoid creating new instant");
              return false;
            }
            return true;})
        .filter(a -> {
            if (a.getParentId() == null
                || !a.getParentId().equals(annotation.getParentId())) {
              log("Not changing end of related annotation ", a, " - different parents");
              return false;
            }
            return true;
          })
        .collect(Collectors.toList()) // add to new collection to avoid concurrent modification
        .forEach(a -> {
            log("Changing end of previous linked annotation ", a, " to ", newStartAnchor);
            changeEndWithRelatedAnnotations(a, newStartAnchor);
          });
    } // not excluding annotation's own layer
  } // end of changeStartWithRelatedAnnotations()
  
  /**
   * Sets the End Anchor of the given annotation, and also the end anchors of related annotations
   * that end in the same place. 
   * @param annotation The annotation whose end anchor should be changed.
   * @param newEndAnchor The new end anchor.
   */
  public void changeEndWithRelatedAnnotations(Annotation annotation, Anchor newEndAnchor) {
    changeEndWithRelatedAnnotations(annotation, newEndAnchor, new HashSet<String>());
  }
  /**
   * Sets the End Anchor of the given annotation, and also the end anchors of related annotations
   * that end in the same place. 
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
   * Sets the End Anchor of the given annotation, and also the end anchors of related annotations
   * that end in the same place. 
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

    aOriginalEnd.endingAnnotations()
      .filter(a -> a != annotation)
      .filter(a -> !a.getLayerId().equals(annotation.getLayerId()))
      .filter(a -> !layerIdsToExclude.contains(a.getLayerId()))
      .filter(a -> {
          // do they have a relationship that would actually preclude sharing?
          Layer otherLayer = annotation.getGraph().getLayer(a.getLayerId());
          if (layer != null && otherLayer != null) {
            if (layer.getParentId().equals(otherLayer.getId())) {
              // other is parent layer to this
              
              // this belongs to another parent
              if (!a.getId().equals(annotation.getParentId())) return false;
              
            } else if (otherLayer.getParentId().equals(layer.getId())) {
              // this is parent layer to other
              
              // other belongs to another parent
              if (!annotation.getId().equals(a.getParentId())) return false;
            }
          }
          return true;
        })
      .collect(Collectors.toList()) // add to new collection to avoid concurrent modification
      .forEach(a -> {
          boolean bInstant = a.getInstantaneous();
          log("Changing end", (bInstant?" and start":""), " of related annotation ", a,
              " to ", newEndAnchor);
          a.setEnd(newEndAnchor);
          if (bInstant) {
            a.setStart(newEndAnchor);
          }
        });
    if (!layerIdsToExclude.contains(annotation.getLayerId())) {
      // also change start anchor of annotations on the same layer
      layerIdsToExclude.add(annotation.getLayerId()); // prevents infinite recursion
      boolean bRelatedAnnotations = aOriginalEnd.startingAnnotations(annotation.getLayerId())
        .filter(Annotation::NotDestroyed)
        .findAny().isPresent();
      if (bRelatedAnnotations) {
        aOriginalEnd.startingAnnotations(annotation.getLayerId())
          .filter(Annotation::NotDestroyed)
          .filter(anNext -> {
              if (!anNext.getStartId().equals(anNext.getEndId())
                  && newEndAnchor.getId().equals(anNext.getEndId())) {
                log("Not changing end of related annotation ",
                    anNext, " to avoid creating new instant");
                return false; // if not shared in the other graph, not shared here
              }
              return true;
            })
          .collect(Collectors.toList()) // add to new collection to avoid concurrent modification
          .forEach(anNext -> {
              log("Changing start of next linked annotation ", anNext, " to ", newEndAnchor);
              changeStartWithRelatedAnnotations(anNext, newEndAnchor, layerIdsToExclude);
            });
      } else {
        boolean bDeletedRelatedAnnotations
          = aOriginalEnd.startingAnnotations(annotation.getLayerId())
          .filter(Annotation::Destroyed)
          .findAny().isPresent();
        if (bDeletedRelatedAnnotations) {
          // all the 'next' annotations on the same layer are deleted
          // ensure that annotations that start here on *other* layers come with us
          // find one related annotation on another layer
          Optional<Annotation> anNext = aOriginalEnd.startingAnnotations()
            .filter(a -> !layerIdsToExclude.contains(a.getLayerId()))
            .findAny(); // one should be sufficient to bring the rest along
          if (anNext.isPresent()) {
            log("Next has been deleted, using ",
                anNext.get(), " to bring starting annotations too");
            changeStartWithRelatedAnnotations(anNext.get(), newEndAnchor, layerIdsToExclude);
          }
        }
      } // there are no 'next' starting annotations
    }
  } // end of changeStartWithRelatedAnnotations()

  /**
   * PHASE 3: Check annotation labels against their counterparts, and sets
   * deltas on our annotations accordingly.
   * @param layer The layer to traverse.
   * @param graph The graph to make changes in.
   * @throws TransformationException On error.
   */
  protected void computeLabelDeltasForMerge(Layer layer, Graph graph)
    throws TransformationException {
    graph.every(layer.getId())
      .filter(Merger::HasCounterpart)
      .forEach(an -> {
          Annotation anEdited = GetCounterpart(an);
          // check for label change
          if (GetConfidence(anEdited) >= GetConfidence(an)) {
            an.setLabel(anEdited.getLabel());
          }
        });
  } // end of computeLabelDeltasForMerge()

  /**
   * PHASE 4: Check anchors of the annotations on the given layer against their counterparts, and
   * sets deltas on our anchors accordingly.
   * @param layer The layer to traverse.
   * @param graph The graph to change.
   * @throws TransformationException On error.
   */
  protected void computeAnchorDeltasForMerge(Layer layer, Graph graph)
    throws TransformationException {
    String layerId = layer.getId();

    // if there are no anchor offset changes, then confidence-only changes are ignored
    // but if there are any offset changes, all anchor confidences are upgraded where appropriate
    // this ensures that forced alignment correction of an utterance sets all segment anchors
    // to manual confidence, but if they're annotating segments rather than re-aligning them,
    // the segment anchors keep their previous confidence level
    boolean thereWereConfidentOffsetChanges = false;
    Vector<Runnable> deferredConfidenceOnlyAnchorChanges = new Vector<Runnable>();

    // check for anchor changes between mapped annotations
    Annotation anLastOriginal = null;
    // traverse the edited version of the graph, to ensure we're all in the new order
    TreeSet<Annotation> editedAnnotations = new TreeSet<Annotation>(
      new AnnotationComparatorByOrdinal());
    editedAnnotations.addAll(Arrays.asList(editedGraph.all(layer.getId())));
    for (Annotation anEdited : editedAnnotations) {
      // get our mapped annotation
      Annotation anOriginal = GetCounterpart(anEdited);

      // an original may not have been added because it was forbidden by noChangeLayers
      if (anOriginal == null) continue;

      assert anOriginal.getChange() != Change.Operation.Destroy
        : "anOriginal.getChange() != Change.Operation.Destroy - " + anOriginal;
	 
      // start anchor...
      if (!dummyAnchors.contains(anOriginal.getStart())) {
        boolean bCheckStartAnchorOffset = true;
	    
        // there may be reasons to relink the annotation to another anchor
        // - i.e. it's linked differently in the edited graph
	    
        boolean bChanged = false;
        // change for linking to a parallel annotations
        Optional<Annotation> anLinkedOriginalParallel = anEdited.getStart().startingAnnotations()
          .filter(Annotation::NotDestroyed)
          .filter(a -> a != anEdited)
          .filter(Merger::HasCounterpart)
          .map(Merger::GetCounterpart) // now linked parallel in original graph
          .filter(a -> a.getStart() != null)
          .filter(a -> anOriginal.getStart() != a.getStart()
                  && GetConfidence(anOriginal.getStart()) <= GetConfidence(a.getStart()))
          // only annotations that have been through this phase
          .filter(a -> a.containsKey("@computeAnchorDeltasForMerge"))
          // don't share this anchor if there's an annotation connected
          // that we shouldn't share with
          .filter(a -> layer.getSaturated()
                  || !a.getStart().startingAnnotations(layer.getParentId()).findAny().isPresent())
          .findAny();
        if (anLinkedOriginalParallel.isPresent()) { // link this annotation to the parallel one
          log(layerId, ": Share start anchor of ", anOriginal, 
              " with parallel ", anLinkedOriginalParallel.get());
          // ensure that the end anchor for the last annotation is updated
          if (anEdited.getInstantaneous()) {
            anOriginal.setEnd(anLinkedOriginalParallel.get().getStart());
          }
          anOriginal.setStart(anLinkedOriginalParallel.get().getStart()); // TODO changeStartWithRelatedAnnotations()?
          bChanged = true;
        } // there is a linked original parallel annotation
        if (!bChanged) {          
          // or maybe its shared for us but *not* shared for them
          for (Annotation anParallel : anOriginal.getStart().startingAnnotations()
                 .filter(Annotation::NotDestroyed)
                 .filter(a -> a != anOriginal)
                 .filter(a -> !a.getLayerId().equals(anOriginal.getLayerId()))
                 .filter(a -> a.getStart() == anOriginal.getStart()) // not already changed
                 .filter(Merger::HasCounterpart)
                 .collect(Collectors.toList())) {
            Annotation anEditedParallel = GetCounterpart(anParallel);
            // not shared in the edite graph
            if (anEdited.getStart() != anEditedParallel.getStart()
                // and there is some confidence in the edited anchors
                && GetConfidence(anEdited.getStart()) > Constants.CONFIDENCE_NONE
                && GetConfidence(anEditedParallel.getStart()) > Constants.CONFIDENCE_NONE) {
              // SHOULD they share? Is there a saturated relationship between the layers
              // (annotation in the child layer)
              Layer parallelLayer = graph.getLayer(anEditedParallel.getLayerId());
              if ((layer.getParentId().equals(anEditedParallel.getLayerId())
                   && layer.getSaturated())
                  || (parallelLayer != null && parallelLayer.getParentId().equals(layerId)
                      && parallelLayer.getSaturated())) {
                if (!layer.getParentId().equals(anEditedParallel.getLayerId())) {// parent layer
                  bCheckStartAnchorOffset = false;
                  log(layerId, ": For ", anOriginal,
                      " assuming that start of ", anParallel, " is correct");
                  // i.e. if the edited graph has disconnection between words and segments
                  // then the segment alignments trump the word aligments
                  break; // stop looking
                }
                // otherwise it's a child - simply don't unlink it, but allow it to change
                continue; // next parallel...
              }
              // new anchor - copy original, then below we might fix up the offset
              Anchor newStart = new Anchor(anOriginal.getStart());
              newStart.create();
              graph.addAnchor(newStart);
              // we change related annotations, but not those related to the parallel layer
              HashSet<String> relatedToParallel = new HashSet<String>();
              relatedToParallel.addAll(parallelLayer.getChildren().keySet());
              relatedToParallel.add(parallelLayer.getParentId());
              relatedToParallel.add(parallelLayer.getId());
              // instant?
              if (anEdited.getInstantaneous()) {
                changeEndWithRelatedAnnotations(anOriginal, newStart);
              }
              changeStartWithRelatedAnnotations(anOriginal, newStart, relatedToParallel);
              log(layerId, ": Different start anchor for ", anOriginal,
                  " unshared from ", anParallel, ": new anchor at ", newStart.getOffset());
              bChanged = true;
              bCheckStartAnchorOffset = true;
              break;
            } // edited and parallel are not linked
          } // next parallel
        } // !bChanged
        Anchor delta = null;
        Runnable relinkUnrelatedAnnotations = null;
        // are the offsets/confidences different?
        if (bCheckStartAnchorOffset) {
          boolean differentOffset
            = compare(anEdited.getStart(), anOriginal.getStart()) != 0;
          boolean higherConfidence
            = GetConfidence(anEdited.getStart()) > GetConfidence(anOriginal.getStart());
          boolean sameOrHigherConfidence
            = GetConfidence(anEdited.getStart()) >= GetConfidence(anOriginal.getStart());
          if (differentOffset && sameOrHigherConfidence) thereWereConfidentOffsetChanges = true;
          if (ignoreOffsetConfidence
              || higherConfidence || (differentOffset && sameOrHigherConfidence)) {
            // theirs is more trustworthy
            // look in the edited graph for a new start-anchor canditate
            // by looking at the end-anchor of previous annotations on this layer
            Optional<Anchor> matchingMergedAnchor = anEdited.getStart()
              .endingAnnotations(layer.getId())
              // skip instantaneous annotations
              .filter(a -> a != anEdited)
              // skip unmerged annotations from a neighboring fragments
              // (TODO not sure there can be any)
              .filter(Merger::HasCounterpart)
              .map(Merger::GetCounterpart)
              // skip it if there's an intervening annotation on this layer 
              // or the parent layer (e.g. "unclear")
              .filter(anOriginalPrevious -> {
                  Annotation anOriginalPreviousNext = anOriginalPrevious.getNext();
                  if (anOriginalPreviousNext != null
                      && Annotation.NotDestroyed(anOriginalPreviousNext)
                      && anOriginalPreviousNext.equals(anOriginal.getPrevious())) {
                    log("Don't link: ", anOriginalPrevious,
                        " across intervening: ", anOriginalPreviousNext,
                        " to: ", anOriginal);
                    return false;
                  }
                  return true;
                })
              // avoid bridging a gap across a parent annotation that's not being deleted
              .filter(anOriginalPrevious -> {
                  Annotation originalPreviousParent = anOriginalPrevious.getParent();
                  Annotation originalParent = anOriginal.getParent();
                  if (originalPreviousParent != originalParent) { // not peers
                    Annotation originalParentPrevious = originalParent.getPrevious();
                    if (originalParentPrevious != null
                        && originalPreviousParent != originalParentPrevious
                        && Annotation.NotDestroyed(originalParentPrevious)) {
                      // there is an intervening annotation on the parent layer
                      log("Don't link previous original: ", anOriginalPrevious,
                          " across intervening parent: ", originalParentPrevious,
                          " to: ", anOriginal);
                      return false;
                    }
                  }
                  return true;
                })
              // avoid bridging a gap across an annotation from a non-merging layer
              // that's not being deleted
              .filter(anOriginalPrevious -> {
                  Optional<Annotation> bridge = anOriginal.getStart().endingAnnotations()
                    .filter(Annotation::NotDestroyed)
                    .filter(anOriginalEndsHere -> // bridges the gap
                            anOriginalEndsHere.getStart() == anOriginalPrevious.getEnd())
                    .filter(
                      anOriginalEndsHere -> // editd graph doesn't include the layer
                      editedGraph.getSchema().getLayer(anOriginalEndsHere.getLayerId()) == null)
                    .findAny();
                  if (bridge.isPresent()) {
                      // there is an intervening annotation on a non-merging layer
                      log("Don't link previous original: ", anOriginalPrevious,
                          " across intervening annotation: ", bridge.get(),
                          " to: ", anOriginal);
                      return false;
                  }
                  return true;
                })
              .filter(anOriginalPrevious -> {
                  // is the new original end anchor offset the same time as the edited start anchor?
                  // (we compare by offset because anchors don't have counterparts we can check)
                  if (compare(anOriginalPrevious.getEnd(), anEdited.getStart()) == 0) {
                    log(layerId, ": Different start anchor for ", anOriginal,
                        ": linking to shared end of ", anOriginalPrevious);
                    return true;
                  }
                  return false;
                })
              .map(anOriginalPrevious -> anOriginalPrevious.getEnd())
              .findAny();
            if (!matchingMergedAnchor.isPresent()) {
              // try for parallel annotations on another layer
              matchingMergedAnchor = anEdited.getStart().startingAnnotations()
                .filter(a -> a != anEdited) // skip ourselves
                .filter(a -> !a.getLayerId().equals(anEdited.getLayerId())) // not on our layer
                // skip unmerged annotations from a neighboring fragments (TODO not sure there can be any)
                .filter(Merger::HasCounterpart)
                .map(Merger::GetCounterpart) // now parallel from original graph
                .filter(a -> a.getStart() != null)
                // no use linking to the very same anchor
                .filter(a -> a.getStart() != anOriginal.getStart())
                // offset should be the same
                .filter(a -> {
                    Annotation anEditedParallel = GetCounterpart(a);
                    if (compare(a.getStart(), anEditedParallel.getStart()) != 0
                        // unless our offset is less trustworthy than theirs
                        && GetConfidence(anEditedParallel.getStart())
                        >= Constants.CONFIDENCE_AUTOMATIC
                        && GetConfidence(a.getStart())
                        <= GetConfidence(anEditedParallel.getStart())) {
                      return false;
                    }
                    log(layerId, ": Different start anchor for ", anOriginal,
                        ": linking to shared start of ", a);
                    return true;
                  })
                .map(anOriginalParallel -> anOriginalParallel.getStart())
                .findAny();
            } // matchingMergedAnchor == null
            if (matchingMergedAnchor.isPresent()) { // use the existing anchor
              final Anchor finalMatchingMergedAnchor = matchingMergedAnchor.get();
              // this, and all parallel annotation on *unrelated* layers come with us
              anOriginal.getStart().startingAnnotations()
                .filter(a -> a != anOriginal)
                // unrelated layer?
                .filter(a -> {
                    Layer otherLayer = a.getLayer();
                    if (!layer.getParentId().equals(otherLayer.getId())
                        && !otherLayer.getParentId().equals(layerId)) {
                      // if the layer is known to the edited graph,
                      // only re-link if they share anchors in the edited graph too
                      if (editedGraph.getLayer(a.getLayerId()) != null) {
                        Annotation anEditedParallel = GetCounterpart(a);
                        if (anEditedParallel == null
                            || anEditedParallel.getStart() != anEdited.getStart()) 
                          return false;
                      } else { // non-edited layer
                        // if it's already been changed, skip it
                        if (a.getStart() != anOriginal.getStart()) return false;
                      }
                    }
                    return true;
                  })
                .collect(Collectors.toList()) // add to new collection to avoid concurrent mod.
                .forEach(a -> {
                    log(layerId, ": Different start anchor for ", anOriginal,
                        ": linking parallel ", a, " too");
                    if (a.getInstantaneous()) {
                      a.setEnd(finalMatchingMergedAnchor); 
                    }
                    a.setStart(finalMatchingMergedAnchor);
                  });
              anOriginal.setStart(finalMatchingMergedAnchor);
            } else { // no matchingMergedAnchor
              if (!dummyEditedAnchors.contains(anEdited.getStart())) {
                delta = new Anchor(anEdited.getStart());
              }
              relinkUnrelatedAnnotations = () -> {
                // create a new anchor for unrelated annotations that link to this one
                Anchor newAnchor = new Anchor(anOriginal.getStart());
                newAnchor.create();
                graph.addAnchor(newAnchor);
                newAnchor = anOriginal.getStart().endingAnnotations()
                .filter(Annotation::NotDestroyed)
                .filter(previousAnnotation -> previousAnnotation != anOriginal)
                .filter(Merger::HasCounterpart)
                .map(previousAnnotation ->  {
                    Layer otherLayer = previousAnnotation.getLayer();
                    Annotation editedPreviousAnnotation = GetCounterpart(previousAnnotation);
                    // only if they're not linked in the edited graph
                    if (editedPreviousAnnotation.getEnd() != anEdited.getStart()) {
                      Optional<Annotation> originalPrevious2
                        = editedPreviousAnnotation.getEnd().endingAnnotations()
                        .filter(Merger::HasCounterpart)
                        .map(Merger::GetCounterpart) // now annotation in original graph
                        .filter(a -> a.getEnd() != anOriginal.getStart())
                        .findAny();
                      if (originalPrevious2.isPresent()) {
                        // found a different but linked anchor via the edited graph structure
                        log(layerId, ": Found end anchor linked via ", originalPrevious2.get());
                        return originalPrevious2.get();
                      }
                    }
                    return null;
                  })
                .filter(Objects::nonNull)
                .map(originalPrevious2 -> originalPrevious2.getEnd())
                .findAny().orElse(newAnchor);
                
                final Anchor finalNewAnchor = newAnchor;
                // (endingAnnotations() can't be used because it causes concurrent mod issues...)
                anOriginal.getStart().getEndingAnnotations().stream()
                .filter(Annotation::NotDestroyed)
                .filter(previousAnnotation -> previousAnnotation != anOriginal)
                .collect(Collectors.toList()) // add to new collection to avoid concurrent mod.
                .forEach(previousAnnotation -> {
                    // check for other possible end anchor, by following the edited graph structure
                    Annotation editedPreviousAnnotation = GetCounterpart(previousAnnotation);
                    if (editedPreviousAnnotation != null) {
                      // only if they're not linked in the edited graph
                      if (editedPreviousAnnotation.getEnd() != anEdited.getStart()) {
                        // next annotation that's parallel to this parallel annotation
                        log(layerId, ": Different start anchor for ", anOriginal, 
                            ": new anchor for ending ", previousAnnotation, " - ", finalNewAnchor);
                        changeEndWithRelatedAnnotations(previousAnnotation, finalNewAnchor);
                      } // they shouldn't be linked
                    } // there is a corresponding edited parallel annotation
                  });
                
                // do the same for annotations that start here
                newAnchor = anOriginal.getStart().startingAnnotations()
                .filter(Annotation::NotDestroyed)
                .filter(parallelAnnotation -> parallelAnnotation != anOriginal) // not ourselves
                .filter(parallelAnnotation -> parallelAnnotation != anOriginal.getParent()) // not our parent
                .filter(Merger::HasCounterpart)
                .map(parallelAnnotation -> {
                    Annotation editedParallelAnnotation = GetCounterpart(parallelAnnotation);
                    // only if they're not linked in the edited graph
                    if (editedParallelAnnotation.getStart() != anEdited.getStart()) {
                      for (Annotation editedParallel2 
                             : editedParallelAnnotation.getStart().getStartingAnnotations()) {
                        Annotation originalParallel2 = GetCounterpart(editedParallel2);
                        if (originalParallel2 == null) continue;
                        if (originalParallel2.getStart() != anOriginal.getStart())
                        { // found a different but linked anchor via the edited graph structure
                          log(layerId, ": Found start anchor linked via ", originalParallel2);
                          return originalParallel2;
                        }
                      } // next annotation that's parallel to this parallel annotation
                    } // they shouldn't be linked
                    return null; 
                  })
                .filter(Objects::nonNull)
                .map(originalParallel2 -> originalParallel2.getStart())
                .findAny().orElse(newAnchor);
                
                final Anchor finalNewAnchor2 = newAnchor;
                anOriginal.getStart().startingAnnotations()
                .filter(Annotation::NotDestroyed)
                // not ourselves
                .filter(parallelAnnotation -> parallelAnnotation != anOriginal)
                // not our parent
                .filter(parallelAnnotation -> parallelAnnotation != anOriginal.getParent()) 
                .filter(Merger::HasCounterpart)
                .collect(Collectors.toList()) // add to new collection to avoid concurrent mod.
                .forEach(parallelAnnotation -> {
                    // check for other possible start anchor,
                    // by following the edited graph structure
                    Annotation editedParallelAnnotation = GetCounterpart(parallelAnnotation);
                    if (editedParallelAnnotation != null) {
                      // only if they're not linked in the edited graph
                      if (editedParallelAnnotation.getStart() != anEdited.getStart()) {
                        parallelAnnotation.setStart(finalNewAnchor2);
                        log(layerId, ": Different start anchor for ", anOriginal,
                            ": new anchor for starting ", parallelAnnotation);
                      } // they shouldn't be linked
                    } // there is a corresponding edited parallel annotation
                  });
              }; // change
            } // no matchingMergedAnchor
            // are we changing this anchor?
            if (delta == null) {
              if (relinkUnrelatedAnnotations != null) relinkUnrelatedAnnotations.run();
            } else { // there is a delta to apply
              if (differentOffset) {
                relinkUnrelatedAnnotations.run();
                SetConfidence(anOriginal.getStart(), GetConfidence(delta));
                anOriginal.getStart().setOffset(delta.getOffset());
                log(layerId, ": Different start anchor for ", anOriginal,
                    ": changing offset to ", delta.getOffset());
              } else {
                final Runnable finalRelinkUnrelatedAnnotations = relinkUnrelatedAnnotations;
                final Anchor finalDelta = delta;
                deferredConfidenceOnlyAnchorChanges.add(() -> {
                    finalRelinkUnrelatedAnnotations.run();
                    SetConfidence(anOriginal.getStart(), GetConfidence(finalDelta));
                    log(layerId, ": Same start anchor but higher confidence for ", anOriginal,
                        ": changing confidence to ", finalDelta.getConfidence(), " (deferred)");
                  });
              }
            } // there is a delta to apply
          } // theirs is more trustworthy
        } // bCheckStartAnchorOffset
	    
        // is there a previous annotation?
        if (anLastOriginal != null
            // are the offsets the same in our graph?
            && compare(anLastOriginal.getEnd(), anOriginal.getStart()) == 0) {
          // previous annotation ending where this one starts
          // do they share anchors in the edited version of the graph?
          Annotation anLastEdited = GetCounterpart(anLastOriginal);
          // skip it if there's an intervening annotation on this layer (e.g. "unclear")
          boolean interveningOriginal = false;
          Annotation anOriginalPreviousNext = anLastOriginal.getNext();
          if (anOriginalPreviousNext != null
              && anOriginalPreviousNext.getChange() != Change.Operation.Destroy
              && anOriginalPreviousNext.equals(anOriginal.getPrevious())) {
            log("Don't link last: ", anLastOriginal,
                " across intervening: ", anOriginalPreviousNext,
                " to: ", anOriginal);
            interveningOriginal = true;
          }
          // the intervening original might be on the parent layer
          if (anOriginal.getParent() != null && anLastOriginal.getParent() != null
              && !anOriginal.getParent().equals(anLastOriginal.getParent())) {
            // we're crossing from one parent to another
            anOriginalPreviousNext = anLastOriginal.getParent().getNext();
            if (anOriginalPreviousNext != null
                && anOriginalPreviousNext.getChange() != Change.Operation.Destroy
                && anOriginalPreviousNext.equals(anOriginal.getParent().getPrevious())) {
              log("Don't link last: ", anLastOriginal,
                  " across intervening parent: ", anOriginalPreviousNext,
                  " to: ", anOriginal);
              interveningOriginal = true;
            }
          }
          // or on a layer that's not in the edited version of the graph
          final Annotation finalLastOriginal = anLastOriginal;
          Optional<Annotation> bridge = anOriginal.getStart().endingAnnotations()
            .filter(Annotation::NotDestroyed)
            .filter(anOriginalEndsHere -> // bridges the gap
                    anOriginalEndsHere.getStart() == finalLastOriginal.getEnd())
            .filter(anOriginalEndsHere -> // edited graph doesn't include the layer
                    editedGraph.getSchema().getLayer(anOriginalEndsHere.getLayerId()) == null)
            .findAny();
          if (bridge.isPresent()) {
            log("Don't link last: ", anLastOriginal,
                " across intervening annotation: ", bridge.get(),
                " to: ", anOriginal);
            interveningOriginal = true;
          }
          if (anLastEdited.getEnd() == anEdited.getStart()
              // are they currently two separate anchors?
              && anLastOriginal.getEnd() != anOriginal.getStart()
              // no reason not to link them?
              && !interveningOriginal) {
            log(layerId, ": Share anchors between ", anLastOriginal, " and ", anOriginal);
            // ensure that the end anchor for the last annotation is updated
            changeEndWithRelatedAnnotations(anLastOriginal, anOriginal.getStart());
          }
          // do they *not* share anchors in the edited version of the graph?
          else if (anLastEdited.getEnd() != anEdited.getStart()
                   // are they currently sharing anchors?
                   && anLastOriginal.getEnd() == anOriginal.getStart()) {
            // not sharing in editedGraph
            log(layerId, ": Un-share anchors between ", anLastOriginal, " and ", anOriginal);
            // create a new anchor for unrelated annotations that link to this one 
            // (this will include anLastOriginal)
            Anchor newAnchor = new Anchor(anOriginal.getStart());
            newAnchor.create();
            graph.addAnchor(newAnchor);
            newAnchor = anOriginal.getStart().endingAnnotations()
              .filter(Annotation::NotDestroyed)
              .filter(a -> a != anOriginal) // instantaneous
              .filter(Merger::HasCounterpart)
              .map(previousAnnotation -> {
                  Layer otherLayer = previousAnnotation.getLayer();
                  // check for other possible end anchor, by following the edited graph structure
                  Annotation editedPreviousAnnotation = GetCounterpart(previousAnnotation);
                  // only if they're not linked in the edited graph
                  if (editedPreviousAnnotation.getEnd() != anEdited.getStart()) {
                    Optional<Annotation> originalPrevious2 
                      = editedPreviousAnnotation.getEnd().endingAnnotations()
                      .filter(Merger::HasCounterpart)
                      .map(Merger::GetCounterpart) // now annotation in original graph
                      .filter(a -> a.getEnd() != anOriginal.getStart())
                      .findAny();
                    if (originalPrevious2.isPresent()) {
                      // found a different but linked anchor via the edited graph structure
                      log(layerId, ": Found end anchor linked via ", originalPrevious2.get());
                      return originalPrevious2.get();
                    }
                  }
                  return null;
                })
              .filter(Objects::nonNull)
              .map(originalPrevious2 -> originalPrevious2.getEnd())
              .findAny().orElse(newAnchor);

            final Anchor finalNewAnchor = newAnchor;
            // (endingAnnotations() can't be used because it causes concurrent mod issues...)
            anOriginal.getStart().getEndingAnnotations().stream()
              .filter(Annotation::NotDestroyed)
              .filter(a -> a != anOriginal) // instantaneous
              .filter(Merger::HasCounterpart)
              .forEach(previousAnnotation -> {
                  // check for other possible end anchor, by following the edited graph structure
                  Annotation editedPreviousAnnotation = GetCounterpart(previousAnnotation);
                  // only if they're not linked in the edited graph
                  if (editedPreviousAnnotation.getEnd() != anEdited.getStart()) {
                    log(layerId, ": Different start anchor for ", anOriginal,
                        ": new anchor for ending ", previousAnnotation, " -- ", finalNewAnchor);
                    changeEndWithRelatedAnnotations(previousAnnotation, finalNewAnchor);
                  }
                });
          } // not sharing in editedGraph
        } // previous annotation ending where this one starts

      } // start is not a dummy anchor

      if (!dummyAnchors.contains(anOriginal.getEnd())) {
        // end anchor
        if (anEdited.getInstantaneous()) { // instantaneous annotation
          if (!anOriginal.getInstantaneous()) {
            anOriginal.setEnd(anOriginal.getStart());
            log(layerId, ": Forcing instantaneity on ", anOriginal);
          }
        } else {
          assert anOriginal.getEnd() != null : "anOriginal.getEnd() != null: " + anOriginal;
          assert anEdited.getEnd() != null : "anEdited.getEnd() != null: " + anEdited;
          boolean bCheckEndAnchorOffset = true;
	       
          // there may be reasons to relink the
          // annotation to another anchor - i.e. it's linked differently in the edited graph
          boolean bChanged = false;
          Optional<Annotation> anLinkedOriginalParallel2 = anEdited.getEnd().endingAnnotations()
            .filter(Annotation::NotDestroyed)
            .filter(a -> a != anEdited)
            .filter(Merger::HasCounterpart)
            .map(Merger::GetCounterpart) // now linked parallel in original graph
            .filter(a -> a.getEnd() != null)
            .filter(a -> anOriginal.getEnd() != a.getEnd()
                    && GetConfidence(anOriginal.getEnd()) <= GetConfidence(a.getEnd()))
            // only link to annotations that have been through this phase
            .filter(a -> a.containsKey("@computeAnchorDeltasForMerge"))
            // don't share this anchor if there's an annotation connected that we
            // shouldn't share with
            .filter(a -> layer.getSaturated() 
                    || !a.getEnd().endingAnnotations(layer.getParentId()).findAny().isPresent())
            .findAny();
          if (anLinkedOriginalParallel2.isPresent()) {
            log(layerId, ": Share end anchor of ", anOriginal, 
                " with parallel ", anLinkedOriginalParallel2.get());
            // ensure that the end anchor for the last annotation is updated
            if (anEdited.getInstantaneous()) {
              anOriginal.setStart(anLinkedOriginalParallel2.get().getEnd());
            }
            anOriginal.setEnd(anLinkedOriginalParallel2.get().getEnd());
            bChanged = true;
          } // there is a linked original parallel annotation
          if (!bChanged) {
            // or maybe its shared for us but *not* shared for them
            for (Annotation anParallel : anOriginal.getEnd().endingAnnotations()
                   .filter(Annotation::NotDestroyed)
                   .filter(a -> a != anOriginal)
                   .filter(a -> !a.getLayerId().equals(anOriginal.getLayerId()))
                   .filter(Merger::HasCounterpart)
                   .collect(Collectors.toList())) {
              Annotation anEditedParallel = GetCounterpart(anParallel);
              if (anEdited.getEnd() != anEditedParallel.getEnd()
                  && !dummyEditedAnchors.contains(anEdited.getEnd())) {
                // SHOULD they share? Is there a saturated relationship between the layers
                // (annotation in the child layer)
                Layer parallelLayer = graph.getLayer(anEditedParallel.getLayerId());
                if ((layer.getParentId().equals(anEditedParallel.getLayerId())
                     && layer.getSaturated())
                    || (parallelLayer != null && parallelLayer.getParentId().equals(layerId)
                        && parallelLayer.getSaturated())) {
                  if (!layer.getParentId().equals(anEditedParallel.getLayerId())) {
                    // parent layer 
                    bCheckEndAnchorOffset = false;
                    log(layerId, ": For ", anOriginal,
                        " assuming that end of ", anParallel, " is correct");
                    // i.e. if the edited graph has disconnection between words and segments
                    // then the segment alignments trump the word aligments
                  }
                  // otherwise it's a child - simply don't unlink it, but allow it to change
                  continue; // next parallel...
                }
		
                // new anchor - copy the original, then later we'll check the offset
                Anchor newAnchor = new Anchor(anOriginal.getEnd());
                newAnchor.create();
                graph.addAnchor(newAnchor);
		
                // we change related annotations, but not those related to the parallel layer
                HashSet<String> relatedToParallel = new HashSet<String>();
                relatedToParallel.addAll(parallelLayer.getChildren().keySet());
                relatedToParallel.add(parallelLayer.getParentId());
                changeEndWithRelatedAnnotations(anOriginal, newAnchor, relatedToParallel);
                log(layerId, ": Different end anchor for ", anOriginal,
                    " unshared from ", anParallel, ": new anchor at ", newAnchor.getOffset());
                bChanged = true;
                bCheckEndAnchorOffset = true;
                break;
              } // they don't share in the edited graph
            } // next possible parallel
          } // !bChanged
	       
          Anchor delta = null;
          if (bCheckEndAnchorOffset) {
            boolean differentOffset
              = compare(anEdited.getEnd(), anOriginal.getEnd()) != 0;
            boolean higherConfidence
              = GetConfidence(anEdited.getEnd()) > GetConfidence(anOriginal.getEnd());
            boolean sameOrHigherConfidence
              = GetConfidence(anEdited.getEnd()) >= GetConfidence(anOriginal.getEnd());
            if (differentOffset && sameOrHigherConfidence) thereWereConfidentOffsetChanges = true;
            if (ignoreOffsetConfidence
                || higherConfidence || (differentOffset && sameOrHigherConfidence)) {
              // try for parallel annotations on another layer
              Optional<Anchor> matchingMergedAnchor = anEdited.getEnd().endingAnnotations()
                // skip ourselves
                .filter(a -> a != anEdited) 
                // not on our own layer
                .filter(a-> !a.getLayerId().equals(anEdited.getLayerId())) 
                // skip unmerged annotations from a neighboring fragments
                // (TODO not sure there can be any)
                .filter(Merger::HasCounterpart)
                .map(Merger::GetCounterpart) // now the parallel annotation in the original graph
                // no use linking to the very same anchor
                .filter(a -> a.getEnd() != anOriginal.getEnd())
                // offset should be the same
                .filter(a -> {
                    Annotation anEditedParallel = GetCounterpart(a);
                    if (compare(a.getEnd(), anEditedParallel.getEnd()) == 0
                        // unless our offset is less trustworthy than theirs
                        || GetConfidence(anEditedParallel.getEnd()) < Constants.CONFIDENCE_AUTOMATIC
                        || GetConfidence(a.getEnd()) > GetConfidence(anEditedParallel.getEnd())) {
                      log(layerId, ": Different end anchor for ", anOriginal,
                          ": linking to shared end of ", a);
                      return true;
                    }
                    return false;                        
                  })
                .map(anOriginalParallel -> anOriginalParallel.getEnd())
                .findAny();
              
              if (matchingMergedAnchor.isPresent()) {
                // this, and all parallel annotation on *unrelated* layers come with us
                anOriginal.getEnd().endingAnnotations()
                  .filter(a -> a != anOriginal)
                  // unrelated layer?
                  .filter(a -> !layer.getParentId().equals(a.getLayerId())
                          && a.getLayer().getParentId().equals(layerId))
                  .collect(Collectors.toList()) // add to new collection to avoid concurrent mod.
                  .forEach(an -> {
                      log(layerId, ": Different end anchor for ", anOriginal,
                          ": linking parallel ", an, " too");
                      if (an.getInstantaneous()) { // instant
                        an.setStart(matchingMergedAnchor.get());
                      }
                      an.setEnd(matchingMergedAnchor.get());
                    });
                anOriginal.setEnd(matchingMergedAnchor.get());
              } else { // no already-merged anchor we can use
                // we might need a new anchor, if this is also the start anchor for
                // another annotation with a different edited offset
                
                boolean bSplitFromFollowing = anOriginal.getEnd().startingAnnotations(layerId)
                  .filter(Merger::HasCounterpart)
                  .map(Merger::GetCounterpart) // now annotation in edite graph
                  .filter(a -> a.getStart() != anEdited.getEnd())
                  .findAny().isPresent();
		
                if (!dummyEditedAnchors.contains(anEdited.getEnd())) {
                  if (bSplitFromFollowing) {
                    delta = new Anchor(anEdited.getEnd());
                    delta.create();
                  } else {
                    // change this anchor
                    delta = new Anchor(anEdited.getEnd());
                  }
                }
              } // change the anchor
            } // theirs is more trustworthy than ours
		  
            // applying change to anchor?
            if (delta != null) {
              if (delta.getChange() != Change.Operation.Create) {
                if (differentOffset) {
                  SetConfidence(anOriginal.getEnd(), GetConfidence(delta));
                  anOriginal.getEnd().setOffset(delta.getOffset());
                  log(layerId, ": Different end anchor for ", anOriginal,
                      ": changing offset to  ", delta.getOffset());
                } else {
                  final Anchor finalDelta = delta;
                  deferredConfidenceOnlyAnchorChanges.add(() -> {
                      SetConfidence(anOriginal.getEnd(), GetConfidence(finalDelta));
                      
                      log(layerId, ": Same end anchor but higher confidence for ", anOriginal,
                          ": changing confidence to  ", finalDelta.getConfidence(),
                          " (deferred)");
                    });
                }
              } else {
                Anchor newAnchor = delta;
                graph.addAnchor(newAnchor);
		
                Set<String> excludeLayers = new HashSet<String>();
                if (layer.getSaturated()) excludeLayers.add(layerId);
                changeEndWithRelatedAnnotations(anOriginal, newAnchor, excludeLayers);
                log(layerId, ": Different end anchor for ", anOriginal,
                    ": new anchor at ", delta.getOffset());
              }
            } // there's a delta
          } // bCheckEndAnchorOffset
	       
          // check for reversed anchors
          if (anOriginal.getEnd().getOffset() != null && anOriginal.getStart().getOffset() != null
              && anOriginal.getEnd().getOffset() < anOriginal.getStart().getOffset()) {
            // is the start anchor for realignment anyway?
            if (GetConfidence(anOriginal.getStart()) == Constants.CONFIDENCE_NONE) {
              log(layerId, ": Reversed anchors: ", anOriginal,
                  ": start is soft, so moving before end");
              // reset the offset
              double dNewOffset = anOriginal.getEnd().getOffset() - smidgin;
              if (anLastOriginal != null) { // halfway through the previous
                dNewOffset = anLastOriginal.getStart().getOffset()
                  + ((anOriginal.getEnd().getOffset() 
                      - anLastOriginal.getStart().getOffset())/2);
              }
              anOriginal.getStart().setOffset(dNewOffset);
              SetConfidence(anOriginal.getStart(), Constants.CONFIDENCE_NONE);
            }
          }
        } // not an instananeous annotation
      } // end is not a dummy anchor

      anOriginal.put("@computeAnchorDeltasForMerge", Boolean.TRUE);
      anLastOriginal = anOriginal;	 
    } // next edited annotation
    
    // if there were any offset changes
    if (thereWereConfidentOffsetChanges) {
      // update the confidences of anchors that have higher confidence but unchanged offset
      for (Runnable confidenceOnlyChange : deferredConfidenceOnlyAnchorChanges) {
        confidenceOnlyChange.run();
      }
    } 
  } // end of computeAnchorDeltasForMerge()

  /**
   * PHASE 5: Checks that, for each parent, the children are in the order (by offset)
   * specified in the edited graph, if <var>peersOverlap</var> is false, and that children
   * are t-included in their parents, if the <var>parentIncludes</var> is true.  
   * <p>Also 'partition' peer layers are checked, to ensures that t-includes relationships
   * that existed before merge exist afterwards - e.g. words are in the same utterance,
   * and phones are in the same syllable
   * in both versions of the graph.
   * A layer is a 'partitition' of this <var>layer</var> iff:
   * <ul>
   *  <li>layer.peers == true</li>
   *  <li>layer.peersOverlap == false</li>
   *  <li>layer.parentIncludes == true</li>
   *  <li>partition != layer</li>
   *  <li>partition.parentIncludes == true</li>
   *  <li>partition.parentId == layer.parentId</li>
   *  <li>partition.alignment == ALIGNMENT_INTERVAL</li>
   *  <li>partition.peers == true</li>
   *  <li>partition.peersOverlap == false</li>
   *  <li>partition.saturated == true</li>
   * </ul>
   * In cases where child anchors are out of order (outside the bounds of their parents,
   * or earlier than preceding children) their anchors are changed to force them inside,
   * giving priority to higher alignment-status offsets.
   * @param layer The (child) layer to check.
   * @param graph The graph to check.
   * @throws TransformationException On error.
   */
  protected void checkChildrenForMerge(Layer layer, Graph graph) throws TransformationException {
    log("checkChildrenForMerge ", layer.getId(),
        " peers:", layer.getPeers(), " peers overlap:", layer.getPeersOverlap());
    Layer parentLayer = layer.getParent();
    if (parentLayer == null) return; // top level layer
    String layerId = layer.getId();
    String parentLayerId = parentLayer.getId();

    boolean editedGraphHasChildLayer = editedGraph.getLayer(layerId) != null;
    boolean editedGraphHasParentLayer = editedGraph.getLayer(parentLayerId) != null;
    if (editedGraphHasChildLayer && editedGraphHasParentLayer
        // only allow tag annotations to move parents (for now)
        && layer.getAlignment() == Constants.ALIGNMENT_NONE) {
      // edited graph has both parent and child layer
      // detect parent changes
      graph.every(layerId)
        .filter(Merger::HasCounterpart)
        .filter(child -> GetCounterpart(child).getParent() != null)
        .filter(child -> HasCounterpart(GetCounterpart(child).getParent()))
        .forEach(child -> {
            Annotation editedChild = GetCounterpart(child);
            Annotation editedParent = editedChild.getParent();
            Annotation editedParentCounterpart = GetCounterpart(editedParent);
            Annotation originalParent = child.getParent();
            if (originalParent == null || originalParent.getId() == null
                || !originalParent.getId().equals(editedParentCounterpart.getId())) {
              // parent has changed
              log(layerId, ": Parent ", originalParent, " changed to ", editedParentCounterpart);
              child.setParent(editedParentCounterpart);
            } // parent has changed
          }); // next child
    } // edited graph has both parent and child layer
    
    // check anchors between children and parents
    if (layer.getPeers() && !layer.getPeersOverlap()) {
      TreeSet<String> partitionIds = new TreeSet<String>();
      if (layer.getParentIncludes() && editedGraphHasChildLayer) {
        // parentIncludes, so can be partitioned
        // identify partition layers
        for (Layer peerLayer : parentLayer.getChildren().values()) {
          if (!peerLayer.getId().equals(layer.getId())
              && peerLayer.equals(editedGraph.getLayer(peerLayer.getId()))
              && peerLayer.getParentIncludes()
              && peerLayer.getAlignment() == Constants.ALIGNMENT_INTERVAL
              && peerLayer.getPeers()
              && !peerLayer.getPeersOverlap()
              && peerLayer.getSaturated()) {
            partitionIds.add(peerLayer.getId());
            log("partition layer: ", peerLayer.getId());
          }
        } // next peer layer
      } // parentIncludes, so can be partitioned

      // whether or not to disallow anchor sharing between children and 
      //  - other children (except consecutive children), and
      //  - parent-layer annotations (except first/last children)
      // i.e. ensures that segment annotations only share end/start anchors with neighbors,
      // and the first segment in a word only shares its start anchor with that word and no others
      // and the last segment in a word only shares its end anchor with that word and no others
      // and that otherwise segments don't share anchors with any words
      // ...and that this only affects relationships between different-parent layers
      // (transcription/segment, or utterance/transcription)
      // not between relationships of the same scope (turn/utterance)
      // ...and that this only affects saturated relationships
      boolean bNoInterSharingForChildren = layer.getSaturated() 
        && layer.getAlignment() != Constants.ALIGNMENT_NONE
        // essentially targeting segment and utterance: TODO maybe remove these?
        // && (parentLayerId.equals(schema.getWordLayerId())
        // 	|| parentLayerId.equals(schema.getTurnLayerId()))
        ;
      HashSet<String> bothLayers = new HashSet<String>();
      bothLayers.add(layerId);
      bothLayers.add(parentLayerId);
      Annotation anLastOriginalParentsLastChild = null;
      // for each parent in the edited graph
      for (Annotation anParent : editedGraph.all(parentLayerId)) {
        Annotation anOriginalParent = GetCounterpart(anParent);
        log(layerId, ": Parent ", anOriginalParent);
        // there may be no original parent because noChangeLayers forbade its addition
        if (anOriginalParent == null) continue;

        TreeSet<Annotation> byOrdinalOrOffset
          = new TreeSet<Annotation>(new AnnotationComparatorByOrdinal());
        HashSet<Annotation> myChildren = new HashSet<Annotation>();
        if (editedGraphHasChildLayer) {
          // edited graph includes child layer, so use its annotations to get to the originals
          byOrdinalOrOffset.addAll(anParent.getAnnotations(layerId));
          for (Annotation an : byOrdinalOrOffset) myChildren.add(GetCounterpart(an));
        } else { // edited graph doesn't include child layer, so use the original graph directly
          byOrdinalOrOffset.addAll(anOriginalParent.getAnnotations(layerId));
          for (Annotation an : byOrdinalOrOffset) myChildren.add(an);
        }

        // gather up partitions, so we can sprinkle their annotations throughout the children
        HashMap<String,Annotation> currentPartition = new HashMap<String,Annotation>();
        HashMap<String,Iterator<Annotation>> partitionIterators
          = new HashMap<String,Iterator<Annotation>>();
        for (String partitionLayerId : partitionIds) {
          // get an iterator through the partitions
          Iterator<Annotation> i = anParent.getAnnotations(partitionLayerId).iterator();
          if (i.hasNext()) {
            partitionIterators.put(partitionLayerId, i);
            // set current partition
            currentPartition.put(partitionLayerId, i.next());
          }
        } // next partition layer
        
        SortedSet<Annotation> children = byOrdinalOrOffset;
        // special case:
        // if the child layer is in the original only
        if (!editedGraphHasChildLayer
            // and the relationship is saturated
            && layer.getSaturated()) {
          // then we trust that the children are in the correct order 
          // and share end/start anchors correctly
          // instead of trusting their anchors,
          // which might have been messed with by the parent anchor changing
          // (e.g. parent = word with changed default anchoring,
          //  and child = syllables with original child anchoring)
          children = new AnnotationChain(myChildren);

          // any children that are not in the chain shouldn't be there!
          Iterator<Annotation> iMyChildren = myChildren.iterator();
          while (iMyChildren.hasNext()) {
            Annotation an = iMyChildren.next();
            if (!children.contains(an)) {
              iMyChildren.remove();
            }
          }
        } // special case where layer isn't in editedGraph and is saturated

        // create an ordered (by child order) set of anchors
        LinkedList<Anchor> anchors = new LinkedList<Anchor>();
        // if the children must be t-included...
        if (layer.getParentIncludes()) {
          // start with an immovable anchor at the beginning of the parent
          anchors.add(new Anchor(null, anOriginalParent.getStart().getOffset(), 
                                 Integer.MAX_VALUE));
        }
        // add all (original) child anchors
        Annotation lastChild = null;
        for (Annotation anChild : removeDeleted(children)) {
          Annotation anOriginalChild = anChild;
          if (editedGraphHasChildLayer) {
            // edited graph includes child layer, so use its annotations to get to the originals
            anOriginalChild = GetCounterpart(anChild);
          }
          log(layerId, ": Child ", anOriginalChild); // TODO comment out
          // there may be no counterpart in the original graph
          // because adding was forbidden by noChangeLayers
          if (anOriginalChild == null) continue;

          // check for new partition anchor
          Double minStart = anChild.getStart().getOffsetMin();
          Double maxEnd = anChild.getEnd().getOffsetMax();
          log(layerId, ": Edited child between ", minStart, " and ", maxEnd); // TODO comment out
          if (minStart != null && maxEnd != null) {
            for (String partitionLayerId : partitionIds) {
              Iterator<Annotation> i = partitionIterators.get(partitionLayerId);
              if (i == null) continue;
              Annotation currentPartitionEdited = currentPartition.get(partitionLayerId);
              Annotation currentPartitionOriginal = GetCounterpart(currentPartitionEdited);
              assert currentPartitionOriginal != null : "currentPartitionOriginal != null: "
                 + currentPartitionEdited.getId() + " " + currentPartitionEdited.getLabel()
                 + " ("+partitionLayerId+")";
              // assume that partition layer already saturates parent, and add end anchors
              // into the collection as appropriate
              double midPoint = minStart + ((maxEnd-minStart) / 2);
              while (!currentPartitionEdited.includesOffset(midPoint)) {
                Anchor possibleBoundary = new Anchor(
                  null, currentPartitionOriginal.getEnd().getOffset(), 
                  Integer.MAX_VALUE);
			
                log("Partition end: ", currentPartitionOriginal,
                    " ", currentPartitionOriginal.getEnd());
                if (!i.hasNext()) {
                   log("No more partitions on: ", partitionLayerId);
                   break;
                }
                currentPartitionEdited = i.next();
                currentPartition.put(partitionLayerId, currentPartitionEdited);
                if (currentPartitionEdited.includes(anChild)) {
                  anchors.add(possibleBoundary);
                } else {
                  log("Skipping partition as ", currentPartitionEdited,
                      " doesn't include edited version of ",
                      anChild, " between ", minStart, "-", maxEnd);
                }
              } // next non-including partition		  
            } // next partition layer	    
          } // child bounds are known

          // start anchor
          String sShareLastAnchorReason = null;
          String sNewAnchorReason = null;
          if (bNoInterSharingForChildren) {
            Vector<Annotation> childrenStartingHere 
              = removeDeleted(anOriginalChild.getStart().startOf(layerId));
            Vector<Annotation> childrenEndingHere 
              = removeDeleted(anOriginalChild.getStart().endOf(layerId));
            if (lastChild == null) { // first child
              if (childrenStartingHere.size() > 1) 
                sNewAnchorReason = "first child shares anchor with other child annotation start - "
                  + "[" + childrenStartingHere.elementAt(0).getId() + "]"
                  + childrenStartingHere.elementAt(0).getLabel()
                  + " and " 
                  + "[" + childrenStartingHere.elementAt(1).getId() + "]"
                  + childrenStartingHere.elementAt(1).getLabel();
              else if (childrenEndingHere.size() == 1
                       && childrenEndingHere.firstElement() != anLastOriginalParentsLastChild
                       // and not sharing start with parent
                       && !anOriginalParent.getStartId().equals(anOriginalChild.getStartId())) 
                sNewAnchorReason = "first child shares anchor with other child annotation end - "
                  + childrenEndingHere;
              else if (childrenEndingHere.size() > 2) 
                sNewAnchorReason = "first child shares anchor with multiple child annotation ends";
            } else { // not first child
              // any parent-layer annotation starts here
              if (childrenStartingHere.size() > 1) 
                sShareLastAnchorReason = "child shares anchor with other child annotation start";
              else if (childrenEndingHere.size() == 1
                       && childrenEndingHere.firstElement() != lastChild) 
                sShareLastAnchorReason = "child shares anchor with other child annotation end - "
                  + childrenEndingHere;
              else if (childrenEndingHere.size() > 2) 
                sShareLastAnchorReason = "child shares anchor with multiple child annotation ends";
            } // not first child
            if (sNewAnchorReason == null && sShareLastAnchorReason == null) {
              long parentsStartingHere = anOriginalChild.getStart()
                .startingAnnotations(parentLayerId).count();
              long parentsEndingHere  = anOriginalChild.getStart()
                .endingAnnotations(parentLayerId).count();
              if (lastChild == null) { // first child
                if (parentsStartingHere == 1
                    && anOriginalChild.getStart()
                    .startingAnnotations(parentLayerId).findAny().get() != anOriginalParent)
                  sNewAnchorReason = "first child shares anchor with parent layer annotation but not parent";
                else if (parentsStartingHere > 2)
                  sNewAnchorReason = "first child shares start anchor with multiple parent layer annotation starts";
                else if (parentsEndingHere > 1)
                  sNewAnchorReason = "first child shares anchor with multiple parent layer annotation ends";
              } else { // not first child
                // any parent-layer annotation starts here
                if (parentsStartingHere > 0)
                  sShareLastAnchorReason = "child shares anchor with parent layer annotation start";
                // any parent-layer annotation ends here
                else if (parentsEndingHere > 0)
                  sNewAnchorReason = "child shares anchor with parent layer annotation end";
              } // not first child
            } // sNewAnchorReason and sShareLastAnchorReason still null
          } // bNoInterSharingForChildren
          if (sShareLastAnchorReason != null) {
             changeStartWithRelatedAnnotations(anOriginalChild, lastChild.getEnd(), bothLayers);
            log(layerId, ": Using end of previous child: ",
                anOriginalChild, ": ", sShareLastAnchorReason);
          } else if (sNewAnchorReason != null) {
            Anchor anNewAnchor = new Anchor(anOriginalChild.getStart());
            anNewAnchor.create();
            graph.addAnchor(anNewAnchor);
            changeStartWithRelatedAnnotations(anOriginalChild, anNewAnchor, bothLayers);
            log(layerId, ": New start anchor for: ", anOriginalChild, ": ", sNewAnchorReason);
          }
          assert anchors != null : "anchors != null";
          assert anOriginalChild != null : "anOriginalChild != null";
          if (!anchors.contains(anOriginalChild.getStart())) {
            anchors.add(anOriginalChild.getStart());
          }
	       
          // end anchor
          sNewAnchorReason = null;
          if (bNoInterSharingForChildren) {
            long childrenStartingHere = anOriginalChild.getEnd().startingAnnotations(layerId)
              .filter(Annotation::NotDestroyed)
              .count();
            long childrenEndingHere = anOriginalChild.getEnd().endingAnnotations(layerId)
              .filter(Annotation::NotDestroyed)
              .count();
            Annotation anLastOriginalChild = children.last();
            if (editedGraphHasChildLayer) {
              // edited graph includes child layer, children contains the edited version
              anLastOriginalChild = GetCounterpart(anLastOriginalChild);
            }	       
            if (anOriginalChild != anLastOriginalChild) { // not last child
              // any parent-layer annotation starts here
              if (childrenEndingHere > 1) 
                sNewAnchorReason = "child shares anchor with other child annotation end";
              else if (childrenStartingHere > 2) 
                sNewAnchorReason = "child shares anchor with multiple child annotation starts";
            } else { // last child
              if (childrenEndingHere > 1) 
                sNewAnchorReason = "last child shares anchor with other child annotation end";
              else if (childrenStartingHere > 2) 
                sNewAnchorReason = "last child shares anchor with multiple child annotation starts";
            } // last child
            if (sNewAnchorReason == null) {
              long parentsStartingHere = anOriginalChild.getEnd()
                .startingAnnotations(parentLayerId).count();
              long parentsEndingHere = anOriginalChild.getEnd()
                .endingAnnotations(parentLayerId).count();
              if (anOriginalChild != anLastOriginalChild) { // not last child
                if (parentsEndingHere > 0)
                  sNewAnchorReason = "child shares anchor with parent layer annotation end";
                if (parentsStartingHere > 0)
                  sNewAnchorReason = "child shares anchor with parent layer annotation start - " + anLastOriginalChild;
              } else { // last child
                if (parentsEndingHere == 1
                    && anOriginalChild.getEnd()
                    .endingAnnotations(parentLayerId).findAny().get() != anOriginalParent)
                  sNewAnchorReason = "last child shares anchor with parent layer annotation but not parent";
                else if (parentsEndingHere > 2)
                  sNewAnchorReason = "last child shares anchor with multiple parent layer annotation ends";
                else if (parentsStartingHere > 1)
                  sNewAnchorReason = "last child shares anchor with multiple parent layer annotation starts";
              } // last child
            } // sNewAnchorReason still null
          } // bNoInterSharingForChildren
	       
          if (sNewAnchorReason != null) {
            Anchor anNewAnchor =  new Anchor(anOriginalChild.getEnd());
            anNewAnchor.create();
            graph.addAnchor(anNewAnchor);
            changeEndWithRelatedAnnotations(anOriginalChild, anNewAnchor, bothLayers);
            log(layerId, ": New end anchor for: ", anOriginalChild, ": ", sNewAnchorReason);
          }
	       
          if (!anchors.contains(anOriginalChild.getEnd())) {
            anchors.add(anOriginalChild.getEnd());
          }
	       
          lastChild = anOriginalChild;
        } // next child
        // if the children must be t-included...
        if (layer.getParentIncludes()) {  // end with an immovable anchor at the end of the parent
          anchors.add(new Anchor(null, anOriginalParent.getEnd().getOffset(), 
                                 Integer.MAX_VALUE));
        }

        // the anchors in our ordered set must be in offset order - i.e.
        // for each anchor, anchor.offset >= predecessor.offset
        // we force this to be true by moving errant anchors, prioritising by alignment status
        ListIterator<Anchor> itAnchors = anchors.listIterator();
        Anchor predecessor = null;
        while (itAnchors.hasNext()) {
          Anchor anchor = itAnchors.next();
          log(layerId, ": anchor: ", anchor, " (", predecessor, ")"); // TODO comment out
          if (anchor.getOffset() == null) continue; // ignore anchors with no offset
          if (predecessor != null && predecessor.getOffset() != null) {
            if (anchor.getOffset() < predecessor.getOffset()) { // out of order
              log(layerId, ": Out of order: ", anchor, " (", predecessor, ")"); // TODO comment out
              // which has the higher status?
              if (GetConfidence(anchor) <= GetConfidence(predecessor)
                  // but we also don't want to run past the end of the parent
                  && (!(layer.getParentIncludes() // ...if the layer is parentIncludes
                        && anOriginalParent.getEnd().getOffset() != null)
                      || anchor.getOffset() < anOriginalParent.getEnd().getOffset())) {
                // anchor.confidence < predecessor.confidence
                // easy case - just change this anchor and keep going
                // does unwinding the delta help?
                if (anchor.getOriginalOffset() >= predecessor.getOffset()) {
                  // old value was ok, so just use that
                  anchor.rollback();
                  log(layerId, ": Out of order, reverting change: ", anchor, " (", predecessor, ")");
                } else { 
                  // have to make up a new offset - make it slightly more than the predecessor
                  // and mark it for default alignment
                  double dOriginalOffset = anchor.getOffset();
                  double dNewOffset = predecessor.getOffset() + smidgin;
                  anchor.setOffset(dNewOffset);
                  SetConfidence(anchor, Constants.CONFIDENCE_NONE);
                  log(layerId, ": Out of order; changing offset: ", anchor, " (", predecessor, ")");
                  // the offset is moving forward, so ending child annotations will be reset
                  anchor.startingAnnotations(layerId)
                    .filter(Annotation::NotDestroyed)
                    .filter(a -> myChildren.contains(a)) // ignore non-children
                    .forEach(anStartingHere -> {
                        if (anStartingHere.getEnd().getOffset() != null
                            && anStartingHere.getEnd().getOffset() < dNewOffset) {
                          // if the end anchor is in the past, it will need moving too
                          anStartingHere.getEnd().setOffset(dNewOffset + smidgin);
                          SetConfidence(anStartingHere.getEnd(), Constants.CONFIDENCE_NONE);
                          log(layerId, ": Out of order, changing offset of end anchor: ",
                              anStartingHere);
                        }
                        resetChildAnchorsBefore(anStartingHere, dNewOffset);
                      }); // next annotation that start here
                }
              } else { // anchor.status > predecessor.status
                // more tricky case, we have to go backwards, resetting anchors of lower status
                // until either we reach our offset, or an anchor with higher status
                boolean bChangeCurrentAnchor = false;
                boolean bRevertWouldSolve = true;
                int iChangeCount = 0;
                double dEndOffset = anchor.getOffset();
                double dFutureOriginalOffset = anchor.getOffset();
                double dLowestOriginalOffset = anchor.getOffset();
                while (itAnchors.hasPrevious()) {
                  predecessor = itAnchors.previous();
                  if (predecessor == null) continue; // skip if there's none
                  if (predecessor == anchor) continue; // skip the one we just got
                  if (predecessor.getOffset() == null) continue; // skip no-offset anchors

                  dLowestOriginalOffset = dLowestOriginalOffset;
                  dLowestOriginalOffset = dLowestOriginalOffset;
                  if (predecessor.getOriginalOffset() != null) {
                     dLowestOriginalOffset = Math.min(
                        dLowestOriginalOffset, predecessor.getOriginalOffset());
                  }

                  // if we get to anchor that has a higher confidence than anchor.confidence, we stop
                  if (GetConfidence(predecessor) > GetConfidence(anchor)
                      && predecessor.getOriginalOffset() != null) {
                    if (predecessor.getOriginalOffset() > dFutureOriginalOffset)
                      bRevertWouldSolve = false;
                    bChangeCurrentAnchor = true; // change anchor as well as those prior to it
                    itAnchors.next(); // reset iterator so that next = first anchor to change
                    break;
                  }
                  // if we've gone far enough back
                  else if (predecessor.getOffset() < anchor.getOffset()
                           && predecessor.getOriginalOffset() != null) {
                    if (predecessor.getOriginalOffset() > dFutureOriginalOffset) bRevertWouldSolve = false;
                    itAnchors.next(); // reset iterator so that next = first anchor to change
                    break;
                  } else {
                    // TODO check if simply reverting would fix
                    if (GetConfidence(predecessor) == GetConfidence(anchor)
                        && predecessor != anchor) {
                      bChangeCurrentAnchor = false; // change anchor as well as those prior to it
                    }
                    // would reverting to the original offset help?
                    if (predecessor.getOriginalOffset() != null
                        && predecessor.getOriginalOffset() > dFutureOriginalOffset) {
                       bRevertWouldSolve = false;
                    }
                    dFutureOriginalOffset = predecessor.getOffset();
                    iChangeCount++;
                  }
                } // previous anchor
                // if we hit the beginning of the list
                if (!itAnchors.hasPrevious()) {
                  itAnchors.next(); // don't change the first anchor
                  bChangeCurrentAnchor = true; // change anchor as well as those prior to it
                  log(layerId, ": Out of order, hit beginning of list: ", anchor, " (", predecessor, ")");
                }
                // now itAnchors.next() is the first prior anchor to change
                // and precedessor = the anchor before that
                if (predecessor == null) continue; // skip if there's none
                if (predecessor.getOffset() == null) continue; // skip no-offset anchors
                double dStartOffset = predecessor.getOffset();
                // if there's an original offset that's lower than this, we can't revert
                if (dStartOffset > dLowestOriginalOffset) bRevertWouldSolve = false;
                double dDuration = dEndOffset - dStartOffset;
                if (dDuration <= 0.0) {
                  dDuration = iChangeCount * smidgin;
                  log(layerId, ": Out of order, nudging ",
                      iChangeCount, " forward from: ", dStartOffset);
                } else {
                  log(layerId, ": Out of order, resetting ", iChangeCount,
                      " anchors between: ", dStartOffset, " and ", dEndOffset);
                }
			
                // move forward through the iterator until we hit anchor
                int iChange = 1;
                Anchor resetChildrenBefore = null;
                while (itAnchors.hasNext()) { // (we should never get to the end)
                  Anchor anchorToChange = itAnchors.next();
                  if (anchorToChange.getOffset() == null) continue;
                  if (bRevertWouldSolve) {
                    if (bChangeCurrentAnchor // if some prior anchors have status >= anchor.status
                        // or all the prior anchors are lower status, so anchor doesn't change
                        || anchorToChange != anchor) {
                      anchorToChange.rollback();
                    }
                    log(layerId, ": Out of order, reverting previous anchor (",
                        anchorToChange, "): ", anchor, " (", predecessor, ")");
                  } else {
                    // if some prior anchors have status >= anchor.status
                    if (bChangeCurrentAnchor
                        // or all the prior anchors are lower status, so anchor doesn't change
                        || anchorToChange != anchor) {
                      // offset a little more than the last one
                      double dOriginalOffset = anchorToChange.getOffset();
                      double dNewOffset = dStartOffset + (iChange * dDuration / iChangeCount);
                      anchorToChange.setOffset(dNewOffset);
                      SetConfidence(anchorToChange, Constants.CONFIDENCE_NONE);
				 
                      if (resetChildrenBefore != null) {
                        // the last loop involved moving an offset forward.
                        // the offset is moving forward,
                        // so starting child annotations will be reset
                        final Anchor before = resetChildrenBefore;
                        resetChildrenBefore.startingAnnotations(layerId)
                          .filter(a -> myChildren.contains(a)) // ignore non-children
                          .forEach(a -> resetChildAnchorsBefore(a, before.getOffset()));
                      }
                      resetChildrenBefore = null;

                      if (dOriginalOffset > dNewOffset) {
                        // the offset is moving back, so ending child annotations will be reset
                        anchorToChange.endingAnnotations(layerId)
                          .forEach(a -> resetChildAnchorsAfter(a, dNewOffset));
                      } else if (dOriginalOffset < dNewOffset) {
                        resetChildrenBefore = anchorToChange;
                        // the offset is moving forward, so starting child annotations
                        // will be reset
                        // we defer this until after the next loop, in case the end
                        // anchor needs changing first
                      } // dOriginalOffset < dNewOffset
                      log(layerId, ": Out of order, updated previous anchor (",
                          anchorToChange, "): ", anchor, " (", predecessor, ")");
                    }
                  }
                  if (anchorToChange == anchor) break; // back where we were, so break out
                  // move to next anchor...
                  predecessor = anchorToChange;
                  iChange++;
                } // next anchor to change
			
                if (resetChildrenBefore != null) {
                  // the last loop involved moving an offset forward.
                  // the offset is moving forward, so starting child annotations will be reset
                  final Anchor before = resetChildrenBefore;
                  resetChildrenBefore.startingAnnotations(layerId)
                    .filter(a -> myChildren.contains(a)) // ignore non-children
                    .forEach(a -> resetChildAnchorsBefore(a, before.getOffset()));
                }
                resetChildrenBefore = null;
                // now predecessor is the anchor before anchor, 
                // and itAnchors.next() is the anchor after anchor
                // so we're back to where we were
              } // anchor.status > predecessor.status
            } // out of order
          } // there is a predeccesor to compare to
          predecessor = anchor;	       
        } // next anchor
	    
        if (layer.getSaturated() && children.size() > 0) {
          Annotation anOriginalChild = children.first();
          if (editedGraphHasChildLayer) {
            // edited graph includes child layer, children contains the edited version
            anOriginalChild = GetCounterpart(anOriginalChild);
          }
          if (anOriginalChild != null) {
            if (anOriginalParent.getStart() != anOriginalChild.getStart()
                // but not if we reconstructed the parent's anchor
                && !dummyAnchors.contains(anOriginalParent.getStart())) {
               changeStartWithRelatedAnnotations(
                 anOriginalParent, anOriginalChild.getStart(), layerId);
              log(layerId, ": Share start of  first child ", anOriginalChild, 
                  " with parent ", anOriginalParent);
            }
          } // anOriginalChild != null
          
          anOriginalChild = children.last();
          if (editedGraphHasChildLayer) {
            // edited graph includes child layer, children contains the edited version
            anOriginalChild = GetCounterpart(anOriginalChild);
          }
          if (anOriginalChild != null) {
            if (anOriginalParent.getEnd() != anOriginalChild.getEnd()
                // but not if we reconstructed the parent's anchor
                && !dummyAnchors.contains(anOriginalParent.getEnd())) {
               changeEndWithRelatedAnnotations(
                 anOriginalParent, anOriginalChild.getEnd(), layerId);
              log(layerId, ": Share end of last child ", anOriginalChild, 
                  " with parent ", anOriginalParent);
            }
          } // anOriginalChild != null
        } // saturated and there are children
          
        anLastOriginalParentsLastChild = lastChild;
      } // next parent
    } // peers && !peersOverlap
  }

   
  /**
   * Removes elements from the collection that are marked for deletion.
   * @param collection The collection to use
   * @return A new collection, all the elements of <var>collection</var> except those where
   * {@link TrackedMap#getChange()} is {@link Change}.Operation.Destroy. 
   */
  public Vector<Annotation> removeDeleted(Collection<Annotation> collection) {
    Vector<Annotation> annotations = new Vector<Annotation>();
    for (Annotation a : collection) {
      if (a.getChange() != Change.Operation.Destroy) {
        annotations.add(a);
      }
    }
    return annotations;
  } // end of removeDeleted()


  /**
   * Resets the anchors of the children of the given annotation. After this method, all children
   * on a given layer  will be s-included (i.e. chained from the start anchor to the end anchor),
   * and all anchors that previously had an offset at or before the threshold will have the
   * offset set to null and the confidence set to {@link Constants#CONFIDENCE_NONE}. All changed
   * anchors are new anchors. 
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
   * Compare two anchors, evaluating them as equal if the difference is less than 
   * {@link Graph#offsetGranularity}.
   * @param a1 The first anchor.
   * @param a2 The second anchor.
   * @return 0 if the anchore offsets are the same, or the difference is less than 
   * {@link Graph#offsetGranularity}, 999 if a1.offset is null, -999 if a2.offset is null, or
   * the value of Double.compareTo(Double) otherwise. 
   */
  protected int compare(Anchor a1, Anchor a2) {
    Double d1 = a1.getOffset();
    Double d2 = a2.getOffset();
    if (d1 == null) return 999;
    if (d2 == null) return -999;
    return editedGraph.compareOffsets(d1,d2);
  }

  /**
   * Gets the confidence rating of a given anchor. If the anchor offset is not set, 
   * {@link Constants#CONFIDENCE_NONE} is returned. If no Integer confidence attribute is
   * present, {@link Constants#CONFIDENCE_MANUAL} is returned. 
   * @param o The anchor to get the confidence rating for.
   * @return The confidence rating of a given object, or {@link Constants#CONFIDENCE_MANUAL} if
   * it could not be determined. 
   */
  protected static int GetConfidence(Anchor o) {
    if (o.getOffset() == null) return Constants.CONFIDENCE_NONE;
    return Utility.getConfidence(o);
  } // end of GetConfidence()

  /**
   * Gets the confidence rating of a given object.  If no Integer confidence attribute is
   * present, {@link Constants#CONFIDENCE_MANUAL} is returned. 
   * @param o The annotation to get the confidence rating for.
   * @return The confidence rating of a given object, or {@link Constants#CONFIDENCE_MANUAL} if
   * it could not be determined. 
   */
  protected static int GetConfidence(Annotation o) {
    return Utility.getConfidence(o);
  } // end of GetConfidence()
   
  /**
   * Sets the confidence of a given object.
   * @param o The object to set the confidence rating for (most likely an {@link Annotation} or
   * {@link Anchor}) 
   * @param confidence The confidence rating of a given object.
   */
  protected static void SetConfidence(TrackedMap o, int confidence) {
    o.setConfidence(confidence);
  } // end of SetConfidence()

   
  /**
   * Determines whether the given annotation has a mapped counterpart in the other graph.
   * @param annotation The annotation to test (or null).
   * @return true if the annotation has an "@other" attribute, false otherwise.
   */
  protected static boolean HasCounterpart(Annotation annotation) {
    if (annotation == null) return false;
    return annotation.containsKey("@other");
  } // end of HasCounterpart()

  /**
   * Gets the given annotation's mapped counterpart in the other graph.
   * @param annotation The annotation to get the counterpart of (or null).
   * @return The annotation in the other graph that has been mapped to the given annotation, or
   * null if no mapping has been made. 
   */
  protected static Annotation GetCounterpart(Annotation annotation) {
    if (annotation == null) return null;
    return (Annotation)annotation.get("@other");
  } // end of GetCounterpart()

  /**
   * Maps the given annotations to each other.
   * @param a1 An annotation.
   * @param a2 The mapped conterpart of <var>a1</var>.
   */
  protected static void SetCounterparts(Annotation a1, Annotation a2) {
    a1.put("@other", a2);
    a2.put("@other", a1);
  } // end of SetCounterparts()

  /**
   * Removes mapping between the given annotation and its counterpart.
   * @param annotation One of the counterpart annotations ({@link #GetCounterpart(Annotation)}
   * will be called to determine the other.). 
   */
  protected static void UnsetCounterparts(Annotation annotation) {
    Annotation other = GetCounterpart(annotation);
    if (other != null) other.remove("@other");
    annotation.remove("@other");
  } // end of SetCounterparts()

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
            .append("(")
            .append(annotation.getStart()).append("[").append(annotation.getStartId()).append("]")
            .append("-")
            .append(annotation.getEnd()).append("[").append(annotation.getEndId()).append("]")
            .append(")");
        } else if (m instanceof Anchor) {
          Anchor anchor = (Anchor)m;
          s.append("[").append(anchor.getId()).append("]").append(anchor.getOffset());
        } else {
          s.append(m.toString());
        }
      }
      String message = s.toString();
      log.add(message);
      for (Consumer<String> observer : logObservers) observer.accept(message);
    }
  } // end of log()

  /**
   * JSON file from command line.
   */
  protected File edited;
  /**
   * Getter for {@link #edited}: JSON file from command line.
   * @return JSON file from command line.
   */
  public File getEdited() { return edited; }
  /**
   * Setter for {@link #edited}: JSON file from command line.
   * @param editedGraphJSONFile The edited version of the graph.
   */
  @Switch(value="File name of JSON file containing edited graph", compulsory=true)
  public Merger setEdited(File editedGraphJSONFile)
    throws FileNotFoundException, IOException, SerializationException,
    SerializerNotConfiguredException, SerializationParametersMissingException {
    edited = editedGraphJSONFile;
    JSONSerialization s = new JSONSerialization();
    s.configure(s.configure(new ParameterSet(), null), null);
    ParameterSet parameters = s.load(
      nzilbb.ag.serialize.util.Utility.OneNamedStreamArray(
        new NamedStream(editedGraphJSONFile)), null);
    s.setParameters(parameters); // run with default values
    Graph[] graphs = s.deserialize();
    setEditedGraph(graphs[0]);
    return this;
  }

  /** Command line interface entrypoint: reads JSON-encoded transcripts from stdin,
   * generates default anchor offsets, and writes them to stdout. */
  public static void main(String argv[]) {
    Merger cli = new Merger();
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
} // end of class Merger
