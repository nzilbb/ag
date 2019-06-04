//
// Copyright 2016-2019 New Zealand Institute of Language, Brain and Behaviour, 
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
import java.util.Collection;
import java.util.Vector;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.List;
import java.util.LinkedList;

import nzilbb.editpath.*;
import nzilbb.ag.*;

/**
 * Merges an editer version of a graph into the original version of that graph.
 * <p>The merger assumes that {@link #editedGraph} and the <var>graph</var> passed to
 * {@link #transform(Graph)} really are versions of the same graph. {@link #editedGraph} needn't
 * contain all the layers that <var>graph</var> does, but it must have a valid layer hierarchy;
 * e.g. if it's a fragment representing an utterance, it must nevertheless have (possibly
 * reconstructed) turn and participant annotations to link the utterance to the graph itself (if
 * <var>graph</var> does). For more detailed assumption information, see {@link #transform(Graph)}.
 * <p>TODO handle graphs with null offset anchors.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class Merger
  implements IGraphTransformer
{
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
  public void setErrors(Vector<String> newErrors) { errors = newErrors; }
   
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
  public void setDebug(boolean newDebug) { debug = newDebug; }

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
  protected void setLog(Vector<String> newLog) { log = newLog; }
   
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
  public void setEditedGraph(Graph newEditedGraph) { editedGraph = newEditedGraph; }

   
  /**
   * The maximum length of a list used for mapping annotations of the same layer but in different graphs to each other.
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
   * Getter for {@link #arraySizeLimit}: The maximum length of a list used for mapping annotations of the same layer but in different graphs to each other.
   * Default is 500. A value of 0 means don't used overlapping chunks.
   * @return The maximum length of a list used for mapping annotations of the same layer but in different graphs to each other.
   */
  public int getArraySizeLimit() { return arraySizeLimit; }
  /**
   * Setter for {@link #arraySizeLimit}: The maximum length of a list used for mapping annotations of the same layer but in different graphs to each other.
   * @param newArraySizeLimit The maximum length of a list used for mapping annotations of the same layer but in different graphs to each other. A value of 0 means don't used overlapping chunks.
   */
  public void setArraySizeLimit(int newArraySizeLimit) { arraySizeLimit = newArraySizeLimit; }


  /**
   * Initial duration of early/layer children, out of order annotations, etc. Default value is 0.00001.
   * @see #getSmidgin()
   * @see #setSmidgin(double)
   */
  protected double smidgin = 0.00001;
  /**
   * Getter for {@link #smidgin}: Initial duration of early/layer children, out of order annotations, etc.
   * @return Initial duration of early/layer children, out of order annotations, etc.
   */
  public double getSmidgin() { return smidgin; }
  /**
   * Setter for {@link #smidgin}: Initial duration of early/layer children, out of order annotations, etc.
   * @param newSmidgin Initial duration of early/layer children, out of order annotations, etc.
   */
  public void setSmidgin(double newSmidgin) { smidgin = newSmidgin; }

  /**
   * Whether to ignore label confidence and force label changes (true), or only change labels when the edited confidence is equal to or higher than the original confidence. Default is false.
   * @see #getIgnoreLabelConfidence()
   * @see #setIgnoreLabelConfidence(boolean)
   */
  protected boolean ignoreLabelConfidence = false;
  /**
   * Getter for {@link #ignoreLabelConfidence}: Whether to ignore label confidence and force label changes (true), or only change labels when the edited confidence is equal to or higher than the original confidence.
   * @return Whether to ignore label confidence and force label changes (true), or only change labels when the edited confidence is equal to or higher than the original confidence. Default is false.
   */
  public boolean getIgnoreLabelConfidence() { return ignoreLabelConfidence; }
  /**
   * Setter for {@link #ignoreLabelConfidence}: Whether to ignore label confidence and force label changes (true), or only change labels when the edited confidence is equal to or higher than the original confidence.
   * @param newIgnoreLabelConfidence Whether to ignore label confidence and force label changes (true), or only change labels when the edited confidence is equal to or higher than the original confidence.
   */
  public void setIgnoreLabelConfidence(boolean newIgnoreLabelConfidence) { ignoreLabelConfidence = newIgnoreLabelConfidence; }

   
  /**
   * Wether to ignore offset confidence and force offset changes (true), or only change offsets when the edited offset is equal to or higher than the original confidence (false).  The default is false.
   * @see #getIgnoreOffsetConfidence()
   * @see #setIgnoreOffsetConfidence(boolean)
   */
  protected boolean ignoreOffsetConfidence = false;
  /**
   * Getter for {@link #ignoreOffsetConfidence}: Wether to ignore offset confidence and force offset changes (true), or only change offsets when the edited offset is equal to or higher than the original confidence (false).
   * @return Wether to ignore offset confidence and force offset changes (true), or only change offsets when the edited offset is equal to or higher than the original confidence (false).  The default is false.
   */
  public boolean getIgnoreOffsetConfidence() { return ignoreOffsetConfidence; }
  /**
   * Setter for {@link #ignoreOffsetConfidence}: Wether to ignore offset confidence and force offset changes (true), or only change offsets when the edited offset is equal to or higher than the original confidence (false).
   * @param newIgnoreOffsetConfidence Wether to ignore offset confidence and force offset changes (true), or only change offsets when the edited offset is equal to or higher than the original confidence (false).
   */
  public void setIgnoreOffsetConfidence(boolean newIgnoreOffsetConfidence) { ignoreOffsetConfidence = newIgnoreOffsetConfidence; }


  /**
   * When comparing anchor offsets, differences below this threshold are ignored.
   * <p>This is useful during merge of graphs that come from two different annotation tools,
   * where one tool has a higher anchor granularity than the other, or when reimporting a graph
   * with default anchors (with maximum granularity) which have been forced to the granurality
   * of a particular tool (e.g. Praat saves offsets to the nearest millisecond).
   * <p>i.e. if an anchor is exported as 3.33333333333 and re-imported as 3.333 then it counts
   * as equal, if the threshold is set to 0.001
   * @see #getOffsetComparisonThreshold()
   * @see #setOffsetComparisonThreshold(Double)
   * @see #compare(Anchor,Anchor)
   */
  protected Double offsetComparisonThreshold;
  /**
   * Getter for {@link #offsetComparisonThreshold}: When comparing anchor offsets, differences below this threshold are ignored.
   * @return When comparing anchor offsets, differences below this threshold are ignored.
   */
  public Double getOffsetComparisonThreshold() { return offsetComparisonThreshold; }
  /**
   * Setter for {@link #offsetComparisonThreshold}: When comparing anchor offsets, differences
   * below this threshold are ignored.
   * <p> A (small) tolerance of 0.00000000001 is automatically added to the threshold to ensure
   * that decimals inaccurately represented as doubles don't produce false-inequalities
   * - e.g. if the intended threshold is 0.0005 and a difference is 0.000500000000001,
   * this is probably due to floating-point rounding error, and so this slight excess
   * is tolerated in {@link #compare(Anchor,Anchor)}.

   * @param newOffsetComparisonThreshold When comparing anchor offsets, differences below this threshold are ignored.
   */
  public void setOffsetComparisonThreshold(Double newOffsetComparisonThreshold) 
  { 
    if (newOffsetComparisonThreshold == null) offsetComparisonThreshold = null;
    else offsetComparisonThreshold = newOffsetComparisonThreshold + 0.00000000001; 
  }

  /**
   * Set of IDs of layers for which annotations may not be added, changed, or deleted.
   * @see #getNoChangeLayers()
   */
  protected HashSet<String> noChangeLayers = new HashSet<String>();
  /**
   * Getter for {@link #noChangeLayers}: Set of IDs of layers for which annotations may not be added, changed, or deleted.
   * @return Set of IDs of layers for which annotations may not be added, changed, or deleted.
   */
  public HashSet<String> getNoChangeLayers() { return noChangeLayers; }
   

  /**
   * The validator to use after merge is complete, or null to not validate the graph after merge. Default value is a {@link Validator} created with its default constructor.
   * @see #getValidator()
   * @see #setValidator(Validator)
   */
  protected Validator validator = new Validator();
  /**
   * Getter for {@link #validator}: The validator to use after merge is complete, or null to not validate the graph after merge.
   * @return The validator to use after merge is complete, or null to not validate the graph after merge. Default value is a {@link Validator} created with its default constructor.
   */
  public Validator getValidator() { return validator; }
  /**
   * Setter for {@link #validator}: The validator to use after merge is complete, or null to not validate the graph after merge.
   * @param newValidator The validator to use after merge is complete, or null to not validate the graph after merge.
   */
  public void setValidator(Validator newValidator) { validator = newValidator; }

  /** Layer shema being used */
  private Schema schema = null;

  /** Default edit-path comparator for annotations */
  private IEditComparator<Annotation> defaultComparator = new IEditComparator<Annotation>()
                                                          {
                                                            int NO_WAY = 200; // weight for ensuring they don't map to each other
                                                            MinimumEditPathString stringComparator = new MinimumEditPathString();
                                                            MinimumEditPathString stringComparatorAvoidSubstitution = new MinimumEditPathString(
                                                              new DefaultEditComparator<Character>(1, 1, 2)); // change is more costly

                                                            /**
                                                             * Compares two sequence elements, and evaluates the distance between them.
                                                             * @param from The element from the source sequence, which may be null,
                                                             * @param to The element from the destination sequence, which may be null.
                                                             * @return An edit step between the two elements. {@link EditStep#getFrom()} is set to <var>from</var>, {@link EditStep#getTo()} is set to <var>to</var>, {@link EditStep#getDistance()} is set to the computed edit distance between these two elements, and {@link EditStep#getOperation()} is set to either <var>EditStep.StepOperation.NONE</var> or <var>EditStep.StepOperation.CHANGE</var>.
                                                             */
                                                            public EditStep<Annotation> compare(Annotation a1, Annotation a2)
                                                            {
                                                              EditStep<Annotation> step = new EditStep<Annotation>(
                                                                a1, a2, 0, EditStep.StepOperation.NONE);
                                                              if (a1 == null)
                                                              {
                                                                if (a2 != null)
                                                                {
                                                                  step.setStepDistance(1);
                                                                  step.setOperation(EditStep.StepOperation.CHANGE);
                                                                }
                                                                // if both are null, we fall through to the return, which amounts to no change
                                                              }
                                                              else if (a2 == null)
                                                              {
                                                                step.setStepDistance(1);
                                                                step.setOperation(EditStep.StepOperation.CHANGE);
                                                              }
                                                              else 
                                                              { // two annotations to compare
                                                                int iWeight = 0;

                                                                if (hasCounterpart(a1) || hasCounterpart(a2))
                                                                { // already mapped
                                                                  if (!hasCounterpart(a1) || !hasCounterpart(a2) || getCounterpart(a1) != a2)
                                                                  { // not mapped to each other
                                                                    iWeight += NO_WAY; // definitely don't want to map them
                                                                  }
                                                                  // else they're already mapped together, so iWeight = 0 is good.
                                                                }
                                                                else
                                                                { // not already mapped
                                                                  Layer layer = a1.getLayer();
                                                                  // check labels (ignoring punctuation etc.)
                                                                  if (!a1.getLabel().equals(a2.getLabel()))
                                                                  {
                                                                    // ignore punctuation and case by default
                                                                    String s1 = a1.getLabel().replaceAll("[^\\p{javaLetter}\\p{javaDigit}]","").toLowerCase();
                                                                    String s2 = a2.getLabel().replaceAll("[^\\p{javaLetter}\\p{javaDigit}]","").toLowerCase();
                                                                    if (s1.length() <= 0 || s2.length() <= 0 
                                                                        || (layer.containsKey("@type") 
                                                                            && layer.get("@type").equals("D"))) // phonological layer TODO formalise layer types
                                                                    {
                                                                      s1 = a1.getLabel();
                                                                      s2 = a2.getLabel(); // for all-punctuation annotations
                                                                    }
                                                                    int iDistance = s1.length() <= 2 || s2.length() <= 2?
                                                                      // really short strings don't as easily allow subsitutions
                                                                      stringComparatorAvoidSubstitution.levenshteinDistance(s1, s2)
                                                                      // but longer strings use standard edit costs
                                                                      :stringComparator.levenshteinDistance(s1, s2);
                                                                    int iMagnifier = 1; // magnify this because anchor offsets also contribute
                                                                    // really short words have to be really similar
                                                                    if (s1.length() <= 2 || s2.length() <= 2) iMagnifier = 3; 
                                                                    iWeight += (iDistance * iMagnifier);
                                                                  } // check labels

                                                                  // don't compare anchors for graph tag layers (i.e. unaligned children of graph)
                                                                  // nor for tags of graph tags layers (i.e. unaligned children of unaligned children of graph)
                                                                  LinkedHashSet<Layer> layerLineage = new LinkedHashSet<Layer>();
                                                                  layerLineage.add(layer);
                                                                  layerLineage.addAll(layer.getAncestors());
                                                                  boolean graphTagLayer = !layer.getId().equals("graph");
                                                                  for (Layer l : layerLineage)
                                                                  {
                                                                    if (l.getAlignment() != Constants.ALIGNMENT_NONE
                                                                        && !l.getId().equals("graph"))
                                                                    {
                                                                      graphTagLayer = false;
                                                                      break;
                                                                    }
                                                                  } // next layer in lineage
                                                                  if (!graphTagLayer)
                                                                  {
                                                                    // an instant cannot map to a non-instant
                                                                    if (a1.getInstantaneous() != a2.getInstantaneous())
                                                                    {
                                                                      iWeight += NO_WAY;
                                                                    }
                                                                    else
                                                                    { // neither an instant, or both an instant
                                                                      // are all offsets available?
                                                                      if (a1.getAnchored() && a2.getAnchored())
                                                                      {
                                                                        // distance is as important as the reliability of the least reliable anchors
                                                                        // i.e. if a1 & a2 have matching alignments (both default, both user-aligned, etc.)
                                                                        // then the weight of the alignment is as heavy as the alignment
                                                                        // but if a1 has default alignments and a2 has user-alignments, then importance is low
                                                                        // because this is probably an alignment update or an unaligned update of aligned annotations
                                                                        // alternatively, if the annotation has a mixture of trustworthyness, weight will be higher
                                                                        double dImportance = Math.min(
                                                                          (double)(getConfidence(a1.getStart()) + getConfidence(a1.getEnd())),
                                                                          (double)(getConfidence(a2.getStart()) + getConfidence(a2.getEnd())))
                                                                          // divided by CONFIDENCE_MANUAL, to make it near 1
                                                                          / (double)(Constants.CONFIDENCE_MANUAL * 2);
                                                                        // however, for "word" and "phone", which are frequently merged between aligned
                                                                        // and unaligned versions, and which should be merged by label only 
                                                                        // we ignore anchors
                                                                        if (a1.getLayerId().equals(schema.getWordLayerId()) // word layer
                                                                            // or (probably) phone layer
                                                                            || (layer.getParentId().equals(schema.getWordLayerId())
                                                                                && layer.getAlignment() == Constants.ALIGNMENT_INTERVAL))
                                                                        {
                                                                          dImportance = 0.01;
                                                                        }			
                                                                        // instantaneous annotations need to have more similar offsets than intervals
                                                                        if (a1.getInstantaneous()) // && a2.getInstantaneous(), but we know it must be
                                                                        {
                                                                          dImportance *= 2.0;
                                                                        }
                                                                        Double dDistance = a1.maxPairedDistance(a2);
                                                                        if (dDistance != null && dDistance != 0)
                                                                        {	
                                                                          // we want to ensure that overlapping annotations are selected over non-overlapping ones
                                                                          if (dImportance > 0)
                                                                          {
                                                                            if (dDistance > 0)
                                                                            { // no overlap
                                                                              // prefer overlap over none
                                                                              iWeight += (int)(dDistance * dImportance * 2);
                                                                            }
                                                                            else
                                                                            {  // overlap
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
                                                                          }
                                                                          else 
                                                                          { 
                                                                            // while distance doesn't contribute to the weight, REALLY BIG distances shouldn't map
                                                                            if (dDistance > 0)
                                                                            { // no overlap
                                                                              if (dDistance > 30)
                                                                              {
                                                                                iWeight += NO_WAY;
                                                                              }
                                                                              else if (Math.abs(a1.getDuration() - a2.getDuration()) > 10) // words differ in length by this much
                                                                              {
                                                                                iWeight += NO_WAY;
                                                                              }
                                                                            }
                                                                            else
                                                                            { // overlap - should be too different at all
                                                                              if (-dDistance > 10)  iWeight += NO_WAY;
                                                                            }
                                                                          }
                                                                        } // distant annotation
                                                                      } // all offsets are available
                                                                    } // neither an instant, or both an instant
                                                                  } // not a graph tag
                                                                } // not already mapped

                                                                step.setStepDistance(iWeight);
                                                                if (!a1.getLabel().equals(a2.getLabel())) 
                                                                { // label would actually change
                                                                  step.setOperation(EditStep.StepOperation.CHANGE);
                                                                }
                                                              }
                                                              return step;
                                                            }
      
                                                            /**
                                                             * The distance for deleting the given element.
                                                             * @param from The element that would be deleted, which may be null.
                                                             * @return An edit step with {@link EditStep#getDistance()} set to the distance for deleting the given element. {@link EditStep#getFrom()} is set to <var>from</var>, and {@link EditStep#getOperation()} is set to <var>EditStep.StepOperation.DELETE</var>
                                                             */
                                                            public EditStep<Annotation> delete(Annotation a1)
                                                            {
                                                              return new EditStep<Annotation>(a1, null, 1, EditStep.StepOperation.DELETE);
                                                            }

                                                            /**
                                                             * The distance for inserting the given element.
                                                             * @param from The element that would be inserted, which may be null.
                                                             * @return An edit step with {@link EditStep#getDistance()} set to the distance for inserting the given element. {@link EditStep#getTo()} is set to <var>to</var>, and {@link EditStep#getOperation()} is set to <var>EditStep.StepOperation.INSERT</var>
                                                             */
                                                            public EditStep<Annotation> insert(Annotation a2)
                                                            {
                                                              return new EditStep<Annotation>(null, a2, 1, EditStep.StepOperation.INSERT);
                                                            }
    };

  protected HashSet<Anchor> dummyAnchors;
  protected HashSet<Anchor> dummyEditedAnchors;
   
  // Methods:
   
  /**
   * Default constructor.
   */
  public Merger()
  {
  } // end of constructor
   
  /**
   * Constructor with edited graph.
   * @param editedGraph The edited version of the graph.
   */
  public Merger(Graph editedGraph)
  {
    setEditedGraph(editedGraph);
  } // end of constructor
   
  // IGraphTransformer method
   
  /**
   * Merges {@link #editedGraph} into the given graph.  
   * The changes are detected and applied to the <var>graph</var>, and returned in a vector of 
   * {@link Change} objects.
   * <p>Assumptions:
   * <ul>
   *  <li><var>editedGraph</var> represents a possibly partial 
   *   (i.e. with a subset of layers) version <var>graph</var> with some changes applied to it.</li>
   *  <li>{@link #editedGraph} is valid and has a valid layer hierarchy (e.g. no orphaned utterances)
   *   - e.g. that {@link Validator} has been applied to it before merging.</li>
   *  <li>{@link #editedGraph} has no proposed changes (i.e. {@link Graph#commit()} has been called)</li>
   *  <li>Participants are identified in both graphs using turn annotation labels,
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
  public Vector<Change> transform(Graph graph) 
    throws TransformationException
  {
    Vector<Change> changes = new Vector<Change>();
    if (debug) setLog(new Vector<String>());
    setErrors(new Vector<String>());
    schema = graph.getSchema();
    if (graph == editedGraph) return changes;
    if (editedGraph == null) throw new TransformationException(this, "Edited graph is no set.", new NullPointerException());

    // ensure that all annotations have an anchor
    dummyAnchors = new HashSet<Anchor>();
    for (Annotation a : graph.getAnnotationsById().values())
    {
      if (a.getStart() == null)
      {
        Anchor dummy = new Anchor(a.getStartId(), null, Constants.CONFIDENCE_NONE);
        dummyAnchors.add(dummy);
        graph.addAnchor(dummy);
      }
      if (a.getEnd() == null)
      {
        Anchor dummy = new Anchor(a.getEndId(), null, Constants.CONFIDENCE_NONE);
        dummyAnchors.add(dummy);
        graph.addAnchor(dummy);
      }
    }
    dummyEditedAnchors = new HashSet<Anchor>();
    for (Annotation a : editedGraph.getAnnotationsById().values())
    {
      if (a.getStart() == null)
      {
        Anchor dummy = new Anchor(a.getStartId(), null, Constants.CONFIDENCE_NONE);
        dummyEditedAnchors.add(dummy);
        editedGraph.addAnchor(dummy);
      }
      if (a.getEnd() == null)
      {
        Anchor dummy = new Anchor(a.getEndId(), null, Constants.CONFIDENCE_NONE);
        dummyEditedAnchors.add(dummy);
        editedGraph.addAnchor(dummy);
      }
    }

    // ensure changes are ignored for selected layers
    for (String layerId : getNoChangeLayers())
    {
      if (graph.getLayer(layerId) != null) 
        graph.getLayer(layerId).put("@noChange", Boolean.TRUE);
      if (editedGraph.getLayer(layerId) != null) 
        editedGraph.getLayer(layerId).put("@noChange", Boolean.TRUE);
    }

    Vector<Layer> topDownLayersInEditedGraph = graph.getLayersTopDown();
    Iterator<Layer> iLayersTopDown = topDownLayersInEditedGraph.iterator();
    while (iLayersTopDown.hasNext())
    {
      Layer layer = iLayersTopDown.next();
      if (editedGraph.getLayer(layer.getId()) == null
          || layer.getId().equals("graph"))
      {
        iLayersTopDown.remove();
      }
    } // next layer

      // phase 1. - map annotations in graph to annotations in editedGraph horizontally
    log("phase 1: map annotations");

    // map graphs together manually, to help top-level parent determination
    setCounterparts(graph, editedGraph); 
    for (Layer layer : topDownLayersInEditedGraph)
    {
      TreeSet<Annotation> uneditedAnnotations 
        = new TreeSet<Annotation>(new AnnotationComparatorByOrdinal());
      uneditedAnnotations.addAll(Arrays.asList(graph.list(layer.getId())));
	 
      TreeSet<Annotation> editedAnnotations 
        = new TreeSet<Annotation>(new AnnotationComparatorByOrdinal());
      editedAnnotations.addAll(Arrays.asList(editedGraph.list(layer.getId())));
	 
      // (no changes to track:)
      mapAnnotationsForMerge(layer, uneditedAnnotations, editedAnnotations); 
    } // next layer

      // phase 2. - reconcile unmapped annotations
    log("phase 2: reconcile unmapped annotations");
      
    for (Layer layer : topDownLayersInEditedGraph)
    {
      if (!getNoChangeLayers().contains(layer.getId()))
      { // creation/destruction is allowed on this layer
        changes.addAll( // track changes of:
          createDestroyAnnotationsForMerge(layer, graph));
      }
      else
      {
        log("Skipping ", layer);
      }
    } // next layer

      // phase 3. - compute label deltas
    log("phase 3: label deltas");

    for (Layer layer : topDownLayersInEditedGraph)
    {
      if (!getNoChangeLayers().contains(layer.getId()))
      { // changing labels is allowed on this layer
        changes.addAll( // track changes of:
          computeLabelDeltasForMerge(layer, graph));
      }
      else
      {
        log("Skipping ", layer);
      }
    } // next layer

      // phase 4. - compute anchor deltas horizontally
    log("phase 4: anchor deltas");
    // take into account the granularities of the graphs when comparing offsets
    if (graph.getOffsetGranularity() != null || editedGraph.getOffsetGranularity() != null)
    {
      if (graph.getOffsetGranularity() == null)
      {
        setOffsetComparisonThreshold(editedGraph.getOffsetGranularity() / 2);
      }
      else if (editedGraph.getOffsetGranularity() == null)
      {
        setOffsetComparisonThreshold(graph.getOffsetGranularity() / 2);
      }
      else
      {
        setOffsetComparisonThreshold(
          Math.max(Math.abs(graph.getOffsetGranularity()), 
                   Math.abs(editedGraph.getOffsetGranularity())) / 2);
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
    while (liLayersTopDown.hasPrevious())
    {
      Layer layer = liLayersTopDown.previous();
      if (layer.getChildren().size() > 0) bottomUpLeavesLastInEditedGraph.add(layer);
    } // previous layer
    while (liLayersTopDown.hasNext()) liLayersTopDown.next(); // move to the end
    // add childless layers
    while (liLayersTopDown.hasPrevious())
    {
      Layer layer = liLayersTopDown.previous();
      if (layer.getChildren().size() == 0) bottomUpLeavesLastInEditedGraph.add(layer);
    } // previous layer
    for (Layer layer : bottomUpLeavesLastInEditedGraph)
    {
      // there's no need to compute anchor changes for unaligned layers
      if (layer.getAlignment() != Constants.ALIGNMENT_NONE)
      {
        changes.addAll( // track changes of:
          computeAnchorDeltasForMerge(layer, graph));
      }
    } // next layer
      // untag annotations tagged during this phase
    for (Annotation a : graph.getAnnotationsById().values()) a.remove("@computeAnchorDeltasForMerge");

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

    for (Layer layer : graph.getLayersTopDown()) // all layers in graph
    {
      changes.addAll( // track changes of:
        checkChildrenForMerge(layer, graph));
    } // next layer

    // remove any dummy anchors before validation
    // (for merging fragments, it's important they're not there)
    for (Anchor dummy : dummyAnchors) graph.getAnchors().remove(dummy.getId());
    for (Anchor dummy : dummyEditedAnchors) editedGraph.getAnchors().remove(dummy.getId());

    if (validator != null)
    {
      log("phase 6: validate");
      validator.setDebug(getDebug());
      changes.addAll(
        validator.transform(graph));
      if (log != null) log.addAll(validator.getLog());
      errors.addAll(validator.getErrors());
    }
    else
    {
      log("phase 6: no validator");
    }

    // remove any new but unreferenced anchors
    for (Anchor a : new Vector<Anchor>(graph.getAnchors().values()))
    {
      if (a.getChange() == Change.Operation.Create
          && a.getStartingAnnotations().size() == 0
          && a.getEndingAnnotations().size() == 0)
      {
        changes.add( // track changes of:
          a.destroy());
        graph.getAnchors().remove(a.getId());
      }
    } // next anchor

    // unlink counterparts, so that either graph can be garbage-collected with the other still referenced
    for (Annotation a : graph.getAnnotationsById().values()) unsetCounterparts(a);

    // remove layer tags
    for (String layerId : getNoChangeLayers())
    {
      if (graph.getLayer(layerId) != null) 
        graph.getLayer(layerId).remove("@noChange");
      if (editedGraph.getLayer(layerId) != null) 
        editedGraph.getLayer(layerId).remove("@noChange");
    }

    return changes;
  }
   
  /**
   * PHASE 1: Maps annotations from another fragment to annotations in this fragment, in order to then compute change deltas.
   * <p>Corresponding annotations in each graph are linked by having the "@other" attribute set.
   * <p>Only annotations with the same participant (if any) can be linked as counterparts.
   * @param layer Layer definition to use.
   * @param these Annotations from one layer in the graph to be merged into.
   * @param those Annotations from the same layer in {@link #editedGraph}.
   * @throws TransformationException On error.
   */
  public void mapAnnotationsForMerge(Layer layer, SortedSet<Annotation> these, SortedSet<Annotation> those)
    throws TransformationException
  {
    HashMap<String,Vector<Annotation>> theseByParticipant = new HashMap<String,Vector<Annotation>>();
    HashMap<String,Vector<Annotation>> thoseByParticipant = new HashMap<String,Vector<Annotation>>();
    // check if this layer has participant assigned
    boolean splitByParticipant = false;
    // if the schema specifies a turn layer
    if (layer.getId().equals(schema.getTurnLayerId()))
    {
      splitByParticipant = true;
    }
    else 
    {
      if (schema.getTurnLayerId() != null
          // and that layer is present in editedGraph
          && editedGraph.getLayer(schema.getTurnLayerId()) != null)
      {
        for (Layer ancestor : layer.getAncestors())
        {
          if (ancestor.getId().equals(schema.getTurnLayerId()))
          {
            splitByParticipant = true;
            break;
          }
        } // next ancestor
      } // turn layer is defined for this schema
    }
    if (!splitByParticipant)
    { // just two straight lists
      theseByParticipant.put("", new Vector<Annotation>(these));
      thoseByParticipant.put("", new Vector<Annotation>(those));
    }
    else
    { // split annotations out by participant
      // annotations from different participants are never paired, so split the lists by 
      // participant and then map them 
      // - this way, simultaneous speech doesn't turn into a whole bunch of unnecessary adds and deletes
      // - this should improve memory usage too, because, with luck, the two lists to get minimum edit
      //   path from are shorter in each case
	 
      for (Annotation an : these)
      {
        String who = "";
        Annotation turn = layer.getId().equals(schema.getTurnLayerId())?
          an:an.my(schema.getTurnLayerId());
        if (turn != null) who = turn.getLabel();
        if (!theseByParticipant.containsKey(who))
        {
          theseByParticipant.put(who, new Vector<Annotation>());
        }
        theseByParticipant.get(who).add(an);
      } // next annotation
      for (Annotation an : those)
      {
        String who = "";
        Annotation turn = layer.getId().equals(schema.getTurnLayerId())?
          an:an.my(schema.getTurnLayerId());
        if (turn != null) who = turn.getLabel();
        if (!thoseByParticipant.containsKey(who))
        {
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

    for (String who : theseByParticipant.keySet()) 
    {
      Vector<Annotation> theseForWho = theseByParticipant.get(who);
      Vector<Annotation> thoseForWho = thoseByParticipant.get(who);

      // if it's a graph tag layer, sort annotations by label
      // this ensures, e.g., that mapping the "who" layer lines the speakers up by name
      if (layer.getParentId().equals("graph") && layer.getAlignment() == Constants.ALIGNMENT_NONE)
      {
        AnnotationComparatorByOrdinal byLabel = new AnnotationComparatorByOrdinal() {
            public int compare(Annotation o1, Annotation o2)
            {
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
      if (!overlappingChunking)
      {
        theses.add(theseForWho);
        thoses.add(thoseForWho);
      }
      else
      { // oversize
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
      while (enThese.hasMoreElements())
      {
        // map the list elements
        Vector<Annotation> theseAnnotations = enThese.nextElement();
        Vector<Annotation> thoseAnnotations = enThose.nextElement();

        if (overlappingChunking)
        {
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
        if (layer.containsKey("@noChange"))
        {
          log("Collapsing edit path for " + layer);
          mp.collapse(path);
        }
        log("PATH " + layer);
        for (EditStep<Annotation> step : path)
        {
          if (step.getFrom() != null && step.getTo() != null)
          {
            setCounterparts(step.getFrom(), step.getTo());
          }
          log(step.getFrom(), " ", step.getOperation(), " ", step.getTo());
        }
      } // next chunk pair
    } // next who
  } // end of mapAnnotationsForMerge()
   
  /**
   * PHASE 2: Create new annotations we don't have that exist in the other graph, and mark annotations that
   * don't exist in the other graph for deletion.
   * <p>This method assumes that {@link #mapAnnotationsForMerge(Layer,SortedSet,SortedSet)} has already
   * been called and thus annotations have had their "@other" attributes set appropriately.
   * <p>New annotations are also given new anchors, and if surrounding annotations (on the same layer)
   * share anchors in the edited version, then corresponding surrounding annotations in the original
   * will be linked to the new anchors.
   * @param layer The layer to traverse.
   * @param graph The graph to add changes to.
   * @return The resulting Create/Destroy changes.
   * @throws TransformationException On error.
   */
  protected Vector<Change> createDestroyAnnotationsForMerge(Layer layer, Graph graph)
    throws TransformationException
  {
    Vector<Change> changes = new Vector<Change>();
    String layerId = layer.getId();
    // unmapped annotations in graph are for deletion
    for (Annotation an : graph.list(layer.getId()))
    {
      if (!hasCounterpart(an))
      {
        log(layerId, ": Deleting ", an);
        changes.add( // track changes of:
          an.destroy());
      }
    } // next annotation

    // might need these later:
    String saturatedParentLayerId = layer.getSaturated()?layer.getParentId():null;
    HashSet<String> thisAndsaturatedParentLayerId = new HashSet<String>();
    thisAndsaturatedParentLayerId.add(layerId);
    if (saturatedParentLayerId != null) thisAndsaturatedParentLayerId.add(saturatedParentLayerId);
    HashSet<String> thisLayerId = new HashSet<String>();
    thisLayerId.add(layerId);
      
    // unmapped annotations of theirs are for addition 
    Annotation anLastOriginal = null;
    for (Annotation anEdited : editedGraph.list(layerId))
    {
      if (!hasCounterpart(anEdited))
      {
        // create a new annotation
        Annotation newAnnotation = new Annotation(null, anEdited.getLabel(), layerId);
        newAnnotation.setConfidence(anEdited.getConfidence());
        newAnnotation.put("@other", anEdited);
        newAnnotation.create();

        // anchor it...
        Anchor start = new Anchor(anEdited.getStart());

        start.create();
        if (getConfidence(anEdited.getStart()) < Constants.CONFIDENCE_AUTOMATIC)
        { // mark for realignment
          setConfidence(start, Constants.CONFIDENCE_NONE);
        }

        Anchor end = new Anchor(anEdited.getEnd());
        end.create();
        if (getConfidence(anEdited.getEnd()) < Constants.CONFIDENCE_AUTOMATIC)
        { // mark for realignment
          setConfidence(end, Constants.CONFIDENCE_NONE);
        }
        if (anEdited.getInstantaneous())
        { // instantaneous annotation
          end = start;
        }
        else
        { // annotation with duration
          // don't look for links for instants, because unlinking them from unlinked annotations later is tricky

          // is the start anchor shared in the edited structure
          for (Annotation anParallel : anEdited.getStart().getStartingAnnotations())
          {
            if (anParallel == anEdited) continue;
            if (anParallel.getStartId() != anParallel.getOriginalStartId()) continue; // isn't the same anchor any more
            if (hasCounterpart(anParallel))
            {
              Annotation otherParallel = getCounterpart(anParallel);
              start = otherParallel.getStart();
              log(layerId, ": ", newAnnotation.getLabel(), 
                  " sharing start with linked ", otherParallel);
              break;
            }
          }

          // is the end anchor shared in the edited structure
          for (Annotation anParallel : anEdited.getEnd().getEndingAnnotations())
          {
            if (anParallel == anEdited) continue;	       
            if (anParallel.getEndId() != anParallel.getOriginalEndId()) continue; // isn't the same anchor any more
            if (hasCounterpart(anParallel))
            {
              Annotation otherParallel = getCounterpart(anParallel);
              end = otherParallel.getEnd();
              log(layerId, ":", newAnnotation.getLabel(),
                  " sharing end with linked ", otherParallel);
              break;
            }
          } // next ending annotation
        } // annotation with duration
        if (start.getId() == null)
        {
          graph.addAnchor(start);
          changes.addAll( // track changes for:
            start.getChanges());
        }
        newAnnotation.setStartId(start.getId());
        if (end.getId() == null)
        {
          graph.addAnchor(end);
          changes.addAll( // track changes for:
            end.getChanges());
        }
        newAnnotation.setEndId(end.getId());

        // set parent/peer annotations
        Annotation editedParent = anEdited.getParent();
        if (editedParent != null && hasCounterpart(editedParent))
        { // counterpart parent set
          Annotation otherParent = getCounterpart(editedParent);
          newAnnotation.setParentId(otherParent.getId());
        } // counterpart parent set
        else
        { // counterpart parent not set
          // for turns, look for the participant with the same label
          if (layerId.equals(schema.getTurnLayerId()) 
              && schema.getParticipantLayerId() != null)
          {
            for (Annotation participant : graph.list(schema.getParticipantLayerId()))
            {
              if (participant.getLabel().equals(newAnnotation.getLabel()))
              {
                newAnnotation.setParentId(participant.getId());
                break;
              }
            } // next participant
          }
        } // counterpart parent not set
	    
        // relink previous annotation to the new anchors? (aligned layers only)
        // in the edited version of the graph, 
        // does this annotation share an anchor with some prior annotation?	    
        if (layer.getAlignment() == Constants.ALIGNMENT_INTERVAL)
        {
          // look for one that's not necessarily the very last one
          for (Annotation anPreviousEdited : anEdited.getStart().endOf(layerId))
          {
            if (anPreviousEdited == anEdited) continue;
            if (hasCounterpart(anPreviousEdited))
            {
              Annotation anPreviousEditedOther = getCounterpart(anPreviousEdited);
              if (getConfidence(anPreviousEditedOther.getEnd()) > getConfidence(start)
                  && anPreviousEditedOther.getEnd().getOffset() != null
                  && (start.getOffset() == null
                      || anPreviousEditedOther.getEnd().getOffset().doubleValue()
                      != start.getOffset().doubleValue()))
              {
                log(layerId, ": Use offset of end of prior: ", anLastOriginal,
                    " (end ", anPreviousEditedOther.getEnd(), ")",
                    " for start of new: ", newAnnotation,
                    " (start ", newAnnotation.getStart(), ")");
                int newStatus = Math.min(
                  getConfidence(graph.getAnchor(anPreviousEditedOther.getOriginalEndId())),
                  getConfidence(start));
                if (newStatus <= Constants.CONFIDENCE_DEFAULT) 
                {
                  newStatus = Math.max(
                    getConfidence(graph.getAnchor(anPreviousEditedOther.getOriginalEndId())),
                    getConfidence(start));
                }
                if (getConfidence(end) > Constants.CONFIDENCE_DEFAULT 
                    && end.getOffset() <= anPreviousEditedOther.getEnd().getOffset())
                {
                  newStatus = Constants.CONFIDENCE_NONE;
                }
                if (start.getChange() == Change.Operation.Create
                    && newStatus > Constants.CONFIDENCE_AUTOMATIC)
                { // using a new anchor, and replacing a manual anchor
                  // set the original attributes instead of a delta
                  // this is so it can't later be reverted to its 'new' values
                  start.commit();
                  start.create();
                }
                changes.addAll( // track changes of:
                  start.setOffset(
                    anPreviousEditedOther.getEnd().getOffset()));
                setConfidence(start, newStatus);
              } // previous more reliable
              if (anPreviousEditedOther.getEnd() != start)
              {			
                log(layerId, ": Share anchor with prior: ", anLastOriginal,
                    " and new: ", newAnnotation, " using ", start);
                changeEndWithRelatedAnnotations(
                  anPreviousEditedOther, start,
                  // we don't re-link a related parent annotation - we might be inserting a new child
                  thisAndsaturatedParentLayerId);
                break;
              }
            } // has counterpart
          } // next previous edited annotation
        } // interval layer
	    
        graph.addAnnotation(newAnnotation);
        log(layerId, ": Adding ", newAnnotation);
        changes.addAll( // track changes for
          newAnnotation.getChanges());
        setCounterparts(newAnnotation, anEdited);
      } // new annotation
      else
      { // existing annotation
        // check whether this anchor should be unlinked from prior ones that are not linked
        // in the edited graph
        Annotation anOriginal = getCounterpart(anEdited);
        // copy the vector so that we don't get concurrent modification problems
        Vector<Annotation> vEndAnnotations = new Vector<Annotation>(anOriginal.getStart().endOf(layerId)); 
        for (Annotation anOriginalLinkedPrior : vEndAnnotations)
        {
          // no edited counterpart?
          if (!hasCounterpart(anOriginalLinkedPrior)) continue;
          // linked in the edited graph?
          if (getCounterpart(anOriginalLinkedPrior).getEnd() == anEdited.getStart()) continue;
          // unlink the prior annotation from this one
          final Anchor originalStart = anOriginal.getStart();
          Anchor newPriorEndAnchor = new Anchor(anOriginal.getStart());
          newPriorEndAnchor.create();
          graph.addAnchor(newPriorEndAnchor);
          changes.addAll(newPriorEndAnchor.getChanges());
          log(layerId, ": Unsharing end of prior: ", 
              anOriginalLinkedPrior, " and start of ", anOriginal);
          // identify which annotations we DON'T want to change the anchor of
          // LayerTraversal<Vector<Annotation>> revert = new LayerTraversal<Vector<Annotation>>(
          // 	  new Vector<Annotation>(), anOriginal)
          // {
          // 	  protected void pre(Annotation annotation)
          // 	  {
          // 	     if (annotation.getStart().equals(originalStart)) result.add(annotation);
          // 	  }
          // };
          changes.addAll( // record changes for:
            changeEndWithRelatedAnnotations(anOriginalLinkedPrior, newPriorEndAnchor, thisLayerId));
          // for (Annotation a : revert.getResult())
          // {
          // 	  log(
          // 	     layerId+": Keeping original start anchor for: " 
          // 	     + logAnnotation(a) + " - " + logAnchor(originalStart));
          // 	  changes.addAll( // record changes for:
          // 	     a.setStart(originalStart));
          // } // next annotation to revert
	       
        } // next prior linked annotation
	    
        if (anLastOriginal != null && anLastOriginal.getChange() == Change.Operation.Create)
        { // mapped to an original annotation, and the last one was new
          // relink this annotation to new anchor?
          // in the edited version of the graph,
          // does this annotation share an anchor with the last annotation?	    
          if (getCounterpart(anLastOriginal).getEnd() == anEdited.getStart()
              && anLastOriginal.getEnd() != anOriginal.getStart())
          {
            // we prefer to use an original anchor (i.e. anOriginal.start)

            // however we avoid creating an inappropriate parallel annotation
            if (anLastOriginal.getStart() != anOriginal.getStart()
                // but also will use last.end when parent start sharing mismatches
                && (anEdited.getStart() == anEdited.getParent().getStart()
                    || anOriginal.getStart() != anOriginal.getParent().getStart()))
            {
              log(layerId, ": Share anchor with next: ",
                  anLastOriginal, " and ", anOriginal);

              boolean bLastIsAnInstant = anLastOriginal.getInstantaneous(); // TODO or maybe just by offset?
              // check we're not creating an instant or backward annotation
              if (anLastOriginal.getStart().getOriginalOffset() != null
                  && anOriginal.getStart().getOriginalOffset() != null
                  && anLastOriginal.getStart().getOriginalOffset()
                  >= anOriginal.getStart().getOriginalOffset()
                  && !bLastIsAnInstant)
              {
                if (getConfidence(anLastOriginal.getStart()) // TODO should be original confidence, but we don't track that!
                    < getConfidence(anOriginal.getStart())
                    // don't go back before parent start
                    && anLastOriginal.getParent().getStart().getOffset()
                    <= anOriginal.getStart().getOffset() - smidgin)
                {
                  double dNewOffset = anOriginal.getStart().getOffset() - smidgin;
                  log(layerId, ": Moving start of : ", 
                      anLastOriginal, " (",
                      getConfidence(anLastOriginal.getStart()), "vs",
                      getConfidence(anOriginal.getStart()), ") to ",
                      dNewOffset, " to avoid non-positive length for ", anLastOriginal);
                  anLastOriginal.getStart().setOffset(dNewOffset);
                  setConfidence(anLastOriginal.getStart(), Constants.CONFIDENCE_NONE);
                }
                else
                {
                  double dNewOffset = anOriginal.getStart().getOffset() + smidgin;
                  log(layerId, ": Moving start of : ", anOriginal, " to ",
                      dNewOffset, " to avoid non-positive length for ", anLastOriginal);
                  anOriginal.getStart().setOffset(dNewOffset);
                  setConfidence(anOriginal.getStart(), Constants.CONFIDENCE_NONE);
                }
              }

              // ensure that the end anchor for the last annotation is updated
              // we don't re-link a related parent annotation - we might be inserting a new child
              changeEndWithRelatedAnnotations(
                anLastOriginal, anOriginal.getStart(), saturatedParentLayerId);
              if (bLastIsAnInstant)
              {
                changeStartWithRelatedAnnotations(anLastOriginal, anOriginal.getStart());
              }
            } // anLastOriginal.getStart() != anOriginal.getStart()
            else
            { // anLastOriginal.getStart() == anOriginal.getStart()
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
      Annotation anOriginal = getCounterpart(anEdited);
      if (anOriginal.getOrdinal() != anEdited.getOrdinal())
      {
        log(layerId, ": changing ordinal of: ", anOriginal,
            " from ", anOriginal.getOrdinal(), " to ", anEdited.getOrdinal());
        anOriginal.setOrdinal(anEdited.getOrdinal());
      }
   
      anLastOriginal = anOriginal;
    } // next edited annotation
    return changes;
  } // end of createDeleteAnnotationsForMerge()
   
  /**
   * Break the given list into overlapping chunks using the given chunk size.
   * @param list The list to break up.
   * @param iNumberOfDiscreteChunks Number of chunks the results list of chunks should have
   * @return A list of chunks
   */
  protected Vector<Vector<Annotation>> breakIntoOverlappingChunks(Vector<Annotation> list, int iNumberOfDiscreteChunks)
  {
    Vector<Vector<Annotation>> vv = new Vector<Vector<Annotation>>();
    Vector<Annotation> va = new Vector<Annotation>();
    vv.add(va);
    Vector<Annotation> vb = null;
    double iChunkSize = ((double)list.size()) / iNumberOfDiscreteChunks;
    double iChangeSize = iChunkSize / 2;
    int i = 0;
    Iterator<Annotation> it = list.iterator();
    while (it.hasNext())
    {
      Annotation an = it.next();
      if (i > vv.size() * iChangeSize)
      {
        if (vb == null)
        {
          vb = new Vector<Annotation>();
          vv.add(vb);
        }
        else
        {
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
  public void removeLeadingMappedAnnotations(Vector<Annotation> annotations)
  {
    if (annotations.size() == 0) return;

    // Start from the end
    ListIterator<Annotation> iAnnotations = annotations.listIterator(annotations.size()-1);
    iAnnotations.next();

    // go backwards until a mapped annotation is found
    while (iAnnotations.hasPrevious())
    {
      if (hasCounterpart(iAnnotations.previous()))
      {
        iAnnotations.remove();
        break;
      }
    }

    // remove all annotations from here until the beginning
    while (iAnnotations.hasPrevious())
    {
      iAnnotations.previous();
      iAnnotations.remove();
    }
  } // end of removeLeadingMappedAnnotations()

  /**
   * Sets the Start Anchor of the given annotation, and also the start anchors of related annotations that start in the same place.
   * @param annotation The annotation to change the start anchor of.
   * @param newStartAnchor The new start anchor.
   * @return The changes made during this operation.
   */
  public Vector<Change> changeStartWithRelatedAnnotations(Annotation annotation, Anchor newStartAnchor)
  {
    return changeStartWithRelatedAnnotations(annotation, newStartAnchor, new HashSet<String>());
  }
  /**
   * Sets the Start Anchor of the given annotation, and also the start anchors of related annotations that start in the same place.
   * @param annotation The annotation whose start anchor will be changed.
   * @param newStartAnchor The new start anchor.
   * @param layerIdToExclude A layer to exclude when updating related annotations.
   * @return The changes made during this operation.
   */
  public Vector<Change> changeStartWithRelatedAnnotations(Annotation annotation, Anchor newStartAnchor, String layerIdToExclude)
  {
    HashSet<String> exclude = new HashSet<String>();
    if (layerIdToExclude != null) exclude.add(layerIdToExclude);
    return changeStartWithRelatedAnnotations(annotation, newStartAnchor, exclude);
  }
  /**
   * Sets the StartAnchor of the given annotation, and also the start anchors of related annotations that start in the same place.
   * @param annotation The annotation whose start anchor will be changed.
   * @param newStartAnchor The new start anchor.
   * @param layerIdsToExclude Layers to exclude when updating related annotations.
   * @return The changes made during this operation.
   */
  public Vector<Change> changeStartWithRelatedAnnotations(Annotation annotation, Anchor newStartAnchor, Set<String> layerIdsToExclude)
  {
    Vector<Change> changes = new Vector<Change>();
    log("changeStartWithRelatedAnnotations ", 
        annotation, " to ", newStartAnchor,
        (layerIdsToExclude.size() > 0?" excluding layers ":""), layerIdsToExclude);
      
    Anchor aOriginalStart = annotation.getStart();
    Anchor aOriginalEnd = annotation.getEnd();      
    changes.addAll( // record changes generated by:
      annotation.setStart(newStartAnchor));
    if (aOriginalStart == aOriginalEnd)
    {
      log(annotation, " is instantaneous, changing both anchors.");
      changes.addAll( // record changes generated by:
        annotation.setEnd(newStartAnchor));
    }
      
    Layer layer = annotation.getGraph().getLayer(annotation.getLayerId());
      
    // change parallel annotations
    for (Annotation anOther : aOriginalStart.getStartingAnnotations())
    {
      if (anOther == annotation) continue;
      if (anOther.getLayerId() == annotation.getLayerId()) continue;
      if (layerIdsToExclude.contains(anOther.getLayerId())) continue;
      // do they have a relationship that would actually preclude sharing?
      Layer otherLayer = annotation.getGraph().getLayer(anOther.getLayerId());
      if (layer != null && otherLayer != null)
      {
        if (layer.getParentId().equals(otherLayer.getId()))
        { // other is parent layer to this
          if (!layer.getSaturated()) continue; // sparse

          // this belongs to another parent
          if (!anOther.getId().equals(annotation.getParentId())) continue;
        }
        else if (otherLayer.getParentId().equals(layer.getId()))
        { // this is parent layer to other
          if (!otherLayer.getSaturated()) continue; // sparse
	       
          // this belongs to another parent
          if (!annotation.getId().equals(anOther.getParentId())) continue;
        }
      }
      boolean bInstant = anOther.getInstantaneous();
      log("Changing start", (bInstant?" and end":""), " of related annotation ", anOther, 
          " to ", newStartAnchor);
      changes.addAll( // record changes generated by:
        anOther.setStart(newStartAnchor));
      if (bInstant)
      {
        changes.addAll( // record changes generated by:
          anOther.setEnd(newStartAnchor));
      }
    } // next parallel anchor starting here

    if (!layerIdsToExclude.contains(annotation.getLayerId()))
    {
      // also change end anchor of annotations on the same layer
      layerIdsToExclude.add(annotation.getLayerId()); // prevents infinite recursion
      Vector<Annotation> vRelatedAnnotations = removeDeleted(
        aOriginalStart.endOf(annotation.getLayerId()));
      for (Annotation anPrevious : vRelatedAnnotations)
      {
        // only if it really still follows
        if (!anPrevious.getEndId().equals(aOriginalStart.getId())) continue;

        if (!anPrevious.getStartId().equals(anPrevious.getEndId())
            && newStartAnchor.getId().equals(anPrevious.getStartId()))
        {
          log("Not changing end of related annotation ", anPrevious,
              " to avoid creating new instant");
          continue;
        }
        if (!anPrevious.getParentId().equals(annotation.getParentId()))
        {
          log("Not changing end of related annotation ", anPrevious,
              " - different parents");
          continue;
        }
        log("Changing end of previous linked annotation ", anPrevious,
            " to ", newStartAnchor);
        changes.addAll( // record changes generated by:
          changeEndWithRelatedAnnotations(anPrevious, newStartAnchor));
      } // next ending annotation
    } // not excluding annotation's own layer
    return changes;
  } // end of changeStartWithRelatedAnnotations()

  /**
   * Sets the End Anchor of the given annotation, and also the end anchors of related annotations that end in the same place.
   * @param annotation The annotation whose end anchor should be changed.
   * @param newEndAnchor The new end anchor.
   * @return The changes made during this operation.
   */
  public Vector<Change> changeEndWithRelatedAnnotations(Annotation annotation, Anchor newEndAnchor)
  {
    return changeEndWithRelatedAnnotations(annotation, newEndAnchor, new HashSet<String>());
  }
  /**
   * Sets the End Anchor of the given annotation, and also the end anchors of related annotations that end in the same place.
   * @param annotation The annotation whose end anchor should be changed.
   * @param newEndAnchor The new end anchor.
   * @param layerIdToExclude A layer to exclude when updating related annotations.
   * @return The changes made during this operation.
   */
  public Vector<Change> changeEndWithRelatedAnnotations(Annotation annotation, Anchor newEndAnchor, String layerIdToExclude)
  {
    HashSet<String> exclude = new HashSet<String>();
    if (layerIdToExclude != null) exclude.add(layerIdToExclude);
    return changeEndWithRelatedAnnotations(annotation, newEndAnchor, exclude);
  }
  /**
   * Sets the End Anchor of the given annotation, and also the end anchors of related annotations that end in the same place.
   * @param annotation The annotation whose end anchor should be changed.
   * @param newEndAnchor The new end anchor.
   * @param layerIdsToExclude Layers to exclude when updating related annotations.
   * @return The changes made during this operation.
   */
  public Vector<Change> changeEndWithRelatedAnnotations(Annotation annotation, Anchor newEndAnchor, Set<String> layerIdsToExclude)
  {
    Vector<Change> changes = new Vector<Change>();
    log("changeEndWithRelatedAnnotations ", annotation, " to ", newEndAnchor,
        (layerIdsToExclude.size() > 0?" excluding layers ":""), layerIdsToExclude);
    Anchor aOriginalEnd = annotation.getEnd();
    Anchor aOriginalStart = annotation.getStart();
    changes.addAll( // record changes generated by:
      annotation.setEnd(newEndAnchor));
    if (aOriginalStart == aOriginalEnd)
    {
      log(annotation, " is instantaneous, changing both anchors.");
      changes.addAll( // record changes generated by:
        annotation.setStart(newEndAnchor));
    }

    Layer layer = annotation.getGraph().getLayer(annotation.getLayerId());

    for (Annotation anOther : aOriginalEnd.getEndingAnnotations())
    {
      if (anOther == annotation) continue;
      if (anOther.getLayerId().equals(annotation.getLayerId())) continue;
      if (layerIdsToExclude.contains(anOther.getLayerId())) continue;

      // do they have a relationship that would actually preclude sharing?
      Layer otherLayer = annotation.getGraph().getLayer(anOther.getLayerId());
      if (layer != null && otherLayer != null)
      {
        if (layer.getParentId().equals(otherLayer.getId()))
        { // other is parent layer to this
          if (!layer.getSaturated()) continue; // sparse

          // this belongs to another parent
          if (!anOther.getId().equals(annotation.getParentId())) continue;
        }
        else if (otherLayer.getParentId().equals(layer.getId()))
        { // this is parent layer to other
          if (!otherLayer.getSaturated()) continue; // sparse
	       
          // this belongs to another parent
          if (!annotation.getId().equals(anOther.getParentId())) continue;
        }
      }
      boolean bInstant = anOther.getInstantaneous();
      log("Changing end", (bInstant?" and start":""), " of related annotation ", anOther,
          " to ", newEndAnchor);
      changes.addAll( // record changes generated by:
        anOther.setEnd(newEndAnchor));
      if (bInstant)
      {
        changes.addAll( // record changes generated by:
          anOther.setStart(newEndAnchor));
      }
    } // next parallel anchor starting here
    if (!layerIdsToExclude.contains(annotation.getLayerId()))
    {
      // also change start anchor of annotations on the same layer
      layerIdsToExclude.add(annotation.getLayerId()); // prevents infinite recursion
      Vector<Annotation> vRelatedAnnotations = removeDeleted(
        aOriginalEnd.startOf(annotation.getLayerId()));
      // vRelatedAnnotations.addAll(aOriginalEnd.getDeltaStartAnnotationsLayer(annotation.getLayerId()));
      if (vRelatedAnnotations.size() > 0)
      {
        for (Annotation anNext : vRelatedAnnotations)
        {
          // only if it really still follows
          if (!anNext.getStartId().equals(aOriginalEnd.getId())) continue;
          if (!anNext.getStartId().equals(anNext.getEndId())
              && newEndAnchor.getId().equals(anNext.getEndId()))
          {
            log("Not changing end of related annotation ", anNext, " to avoid creating new instant");
            continue; // if not shared in the other graph, not shared here
          }
          log("Changing start of next linked annotation ", anNext, " to ", newEndAnchor);
          changes.addAll( // log the following change:
            changeStartWithRelatedAnnotations(anNext, newEndAnchor, layerIdsToExclude));
        } // next starting annotation
	    
        if (vRelatedAnnotations.size() == 0)
        { // all the 'next' annotations on the same layer are deleted
          // ensure that annotations that start here on *other* layers come with us
          // find one related annotation on another layer
          vRelatedAnnotations.addAll(aOriginalEnd.getStartingAnnotations());
          for (Annotation anNext : vRelatedAnnotations)
          {
            if (layerIdsToExclude.contains(anNext.getLayerId())) continue;
            log("Next has been deleted, using ", anNext, " to bring starting annotations too");
            changes.addAll( // log the following change:
              changeStartWithRelatedAnnotations(anNext, newEndAnchor, layerIdsToExclude));
            break; // one should be sufficient to bring the rest along
          } // next starting annotation	       
        }  // all the 'next' annotations on the same layer are deleted
      } // there are 'next' starting annotations
    }
    return changes;
  } // end of changeStartWithRelatedAnnotations()

  /**
   * PHASE 3: Check annotation labels against their counterparts, and sets
   * deltas on our annotations accordingly.
   * @param layer The layer to traverse.
   * @param graph The graph to make changes in.
   * @return The resulting label changes.
   * @throws TransformationException On error.
   */
  protected Vector<Change> computeLabelDeltasForMerge(Layer layer, Graph graph)
    throws TransformationException
  {
    Vector<Change> changes = new Vector<Change>();
    for (Annotation an : graph.list(layer.getId()))
    {
      Annotation anEdited = getCounterpart(an);
      if (anEdited == null) continue;
      // check for label change
      if (getConfidence(anEdited) >= getConfidence(an))
      {
        changes.addAll( // track changes (if any) for:
          an.setLabel(anEdited.getLabel()));
      }
    }
    return changes;
  } // end of computeLabelDeltasForMerge()

  /**
   * PHASE 4: Check anchors of the annotations on the given layer against their counterparts, and
   * sets deltas on our anchors accordingly.
   * @param layer The layer to traverse.
   * @param graph The graph to change.
   * @return The resulting anchor changes.
   * @throws TransformationException On error.
   */
  protected Vector<Change> computeAnchorDeltasForMerge(Layer layer, Graph graph)
    throws TransformationException
  {
    Vector<Change> changes = new Vector<Change>();

    String layerId = layer.getId();

    // check for anchor changes between mapped annotations
    Annotation anLastOriginal = null;
    // traverse the edited version of the graph, to ensure we're all in the new order
    TreeSet<Annotation> editedAnnotations = new TreeSet<Annotation>(new AnnotationComparatorByOrdinal());
    editedAnnotations.addAll(Arrays.asList(editedGraph.list(layer.getId())));
    for (Annotation anEdited : editedAnnotations)
    {
      // get our mapped annotation
      Annotation anOriginal = getCounterpart(anEdited);

      // an original may not have been added because it was forbidden by noChangeLayers
      if (anOriginal == null) continue;

      assert anOriginal.getChange() != Change.Operation.Destroy : "anOriginal.getChange() != Change.Operation.Destroy - " + anOriginal;
	 
      // start anchor...
      if (!dummyAnchors.contains(anOriginal.getStart()))
      {
        boolean bCheckStartAnchorOffset = true;
	    
        // there may be reasons to relink the annotation to another anchor
        // - i.e. it's linked differently in the edited graph
	    
        boolean bChanged = false;
        // change for linking to a parallel annotations
        for (Annotation anParallel : removeDeleted(anEdited.getStart().getStartingAnnotations()))
        {
          if (anParallel == anEdited) continue;		  
          if (hasCounterpart(anParallel))
          {
            Annotation anLinkedOriginalParallel = getCounterpart(anParallel);
            if (anOriginal.getStart() != anLinkedOriginalParallel.getStart()
                && getConfidence(anOriginal.getStart()) 
                <= getConfidence(anLinkedOriginalParallel.getStart()))
            { // link this annotation to the parallel one
              // only link to annotations that have been through this phase
              if (!anLinkedOriginalParallel.containsKey("@computeAnchorDeltasForMerge")) continue;
              // don't share this anchor if there's an annotation connected that we shouldn't share with
              if (layer.getSaturated() 
                  || anLinkedOriginalParallel.getStart().startOf(layer.getParentId()).size() == 0)
              {
                log(layerId, ": Share start anchor of ", anOriginal, 
                    " with parallel ", anLinkedOriginalParallel);
                // ensure that the end anchor for the last annotation is updated
                if (anEdited.getInstantaneous())
                {
                  changes.addAll( // track changes of:
                    anOriginal.setEnd(anLinkedOriginalParallel.getStart()));
                }
                changes.addAll( // track changes of:
                  anOriginal.setStart(anLinkedOriginalParallel.getStart())); // TODO changeStartWithRelatedAnnotations()?
                bChanged = true;
                break;
              } // bShare
            } // link this annotation to the parallel one
          } // their-parallel has a counterpart
        } // next parallel their-annotation
        if (!bChanged)
        {
          // or maybe its shared for us but *not* shared for them
          for (Annotation anParallel : removeDeleted(anOriginal.getStart().getStartingAnnotations()))
          {
            if (anParallel == anOriginal) continue;
            if (anParallel.getLayerId() == anOriginal.getLayerId()) continue;
            if (anParallel.getStart() != anOriginal.getStart()) continue; // aready changed
            if (hasCounterpart(anParallel))
            {
              Annotation anEditedParallel = getCounterpart(anParallel);
              // not shared in the edite graph
              if (anEdited.getStart() != anEditedParallel.getStart()
                  // and there is some confidence in the edited anchors
                  && getConfidence(anEdited.getStart()) > Constants.CONFIDENCE_NONE
                  && getConfidence(anEditedParallel.getStart()) > Constants.CONFIDENCE_NONE)
              {
                // SHOULD they share? Is there a saturated relationship between the layers (annotation in the child layer)
                Layer parallelLayer = graph.getLayer(anEditedParallel.getLayerId());
                if ((layer.getParentId().equals(anEditedParallel.getLayerId())
                     && layer.getSaturated())
                    || (parallelLayer != null && parallelLayer.getParentId().equals(layerId)
                        && parallelLayer.getSaturated()))
                {
                  if (!layer.getParentId().equals(anEditedParallel.getLayerId())) // parent layer
                  {
                    bCheckStartAnchorOffset = false;
                    log(layerId, ": For ", anOriginal,
                        " assuming that start of ", anParallel, " is correct");
                    // i.e. if the edited graph has disconnection between words and segments
                    // then the segment alignments trump the word aligments
                  }
                  // otherwise it's a child - simply don't unlink it, but allow it to change
                  continue; // next parallel...
                }
                // new anchor - copy original, then below we might fix up the offset
                Anchor newStart = new Anchor(anOriginal.getStart());
                newStart.create();
                graph.addAnchor(newStart);
                changes.addAll( // track changes of:
                  newStart.getChanges());
                // we change related annotations, but not those related to the parallel layer
                HashSet<String> relatedToParallel = new HashSet<String>();
                relatedToParallel.addAll(parallelLayer.getChildren().keySet());
                relatedToParallel.add(parallelLayer.getParentId());
                relatedToParallel.add(parallelLayer.getId());
                // instant?
                if (anEdited.getInstantaneous())
                {
                  changes.addAll( // track changes of:
                    changeEndWithRelatedAnnotations(anOriginal, newStart));
                }
                changes.addAll( // track changes of:
                  changeStartWithRelatedAnnotations(anOriginal, newStart, relatedToParallel));
                log(layerId, ": Different start anchor for ", anOriginal,
                    " unshared from ", anParallel, ": new anchor at ", newStart.getOffset());
                bChanged = true;
                bCheckStartAnchorOffset = true;
                break;
              } // edited and parallel are not linked
            } // their-parallel has a counterpart
          } // next parallel
        } // !bChanged
        Anchor delta = null;
        // are the offsets different?
        if (bCheckStartAnchorOffset
            && compare(anEdited.getStart(), anOriginal.getStart()) != 0)
        {
          if (ignoreOffsetConfidence
              || getConfidence(anEdited.getStart()) >= getConfidence(anOriginal.getStart()))
          { // theirs is more trustworthy
            // look in the edited graph for a new start-anchor canditate
            // by looking at the end-anchor of previous annotations on this layer
            Anchor matchingMergedAnchor = null;
            for (Annotation anEditedPrevious : anEdited.getStart().endOf(layer.getId()))
            {
              // skip instantaneous annotations
              if (anEditedPrevious == anEdited) continue; 
              Annotation anOriginalPrevious = getCounterpart(anEditedPrevious);
              // skip unmerged annotations from a neighboring fragments (TODO not sure there can be any)
              if (anOriginalPrevious == null) continue; 
              assert anOriginalPrevious.getEnd() != null 
                : "anOriginalPrevious.getEnd() != null - " + anOriginalPrevious;
              // is the new original end anchor offset the same time as the edited start anchor?
              // (we compare by offset because anchors don't have counterparts we can check)
              if (compare(anOriginalPrevious.getEnd(), anEdited.getStart()) == 0)
              {
                matchingMergedAnchor = anOriginalPrevious.getEnd();
                log(layerId, ": Different start anchor for ", anOriginal,
                    ": linking to shared end of ", anOriginalPrevious);
                break;
              }
            } // next possible end anchor annotation
            if (matchingMergedAnchor == null)
            {
              // try for parallel annotations on another layer
              for (Annotation anEditedParallel : anEdited.getStart().getStartingAnnotations())
              {		     
                if (anEditedParallel == anEdited) continue; // skip ourselves
                if (anEditedParallel.getLayerId() == anEdited.getLayerId()) continue; // on our own layer
                Annotation anOriginalParallel = getCounterpart(anEditedParallel);
                // skip unmerged annotations from a neighboring fragments (TODO not sure there can be any)
                if (anOriginalParallel == null) continue; 
                assert anOriginalParallel.getStart() != null 
                  : "anOriginalParallel.getStart() != null - " + anOriginalParallel;
                // no use linking to the very same anchor
                if (anOriginalParallel.getStart() == anOriginal.getStart()) continue;
                // offset should be the same
                if (compare(anOriginalParallel.getStart(), anEditedParallel.getStart()) != 0
                    // unless our offset is less trustworthy than theirs
                    && getConfidence(anEditedParallel.getStart()) >= Constants.CONFIDENCE_AUTOMATIC
                    && getConfidence(anOriginalParallel.getStart()) <= getConfidence(anEditedParallel.getStart())) continue;
                matchingMergedAnchor = anOriginalParallel.getStart();
                log(layerId, ": Different start anchor for ", anOriginal,
                    ": linking to shared start of ", anOriginalParallel);
                break;
              } // next possible start anchor annotation
            } // matchingMergedAnchor == null
            if (matchingMergedAnchor != null)
            { // use the existing anchor
              // this, and all parallel annotation on *unrelated* layers come with us
              for (Annotation an : anOriginal.getStart().getStartingAnnotations())
              {
                if (an == anOriginal) continue;
                // unrelated layer?
                Layer otherLayer = an.getLayer();
                if (!layer.getParentId().equals(otherLayer.getId())
                    && !otherLayer.getParentId().equals(layerId))
                {
                  // if the layer is known to the edited graph,
                  // only re-link if they share anchors in the edited graph too
                  if (editedGraph.getLayer(an.getLayerId()) != null)
                  {
                    Annotation anEditedParallel = getCounterpart(an);
                    if (anEditedParallel == null
                        || anEditedParallel.getStart() != anEdited.getStart()) 
                      continue;
                  }
                  else // non-edited layer
                  {
                    // if it's already been changed, skip it
                    if (an.getStart() != anOriginal.getStart()) continue;
                  }
                  log(layerId, ": Different start anchor for ", anOriginal,
                      ": linking parallel ", an, " too");
                  if (an.getInstantaneous()) 
                  {
                    changes.addAll( // track changes of:
                      an.setEnd(matchingMergedAnchor)); 
                  }
                  changes.addAll( // track changes of:
                    an.setStart(matchingMergedAnchor));
                } // unrelated layer
              } // next annotation starting here
              changes.addAll( // track changes of:
                anOriginal.setStart(matchingMergedAnchor));
            } // found a counterpart anchor
            else
            {
              if (!dummyEditedAnchors.contains(anEdited.getStart()))
              {
                delta = new Anchor(anEdited.getStart());
              }
		     
              // create a new anchor for unrelated annotations that link to this one
              Anchor newAnchor = new Anchor(anOriginal.getStart());
              newAnchor.create();
              graph.addAnchor(newAnchor);
              changes.addAll( // track changes of:
                newAnchor.getChanges());
              for (Annotation previousAnnotation 
                     : removeDeleted(anOriginal.getStart().getEndingAnnotations()))
              {
                if (previousAnnotation == anOriginal) continue; // instantaneous
                Layer otherLayer = previousAnnotation.getLayer();
                // check for other possible end anchor, by following the edited graph structure
                Annotation editedPreviousAnnotation = getCounterpart(previousAnnotation);
                if (editedPreviousAnnotation != null)
                {
                  // only if they're not linked in the edited graph
                  if (editedPreviousAnnotation.getEnd() != anEdited.getStart())
                  {
                    for (Annotation editedPrevious2 : editedPreviousAnnotation.getEnd().getEndingAnnotations())
                    {
                      if (!hasCounterpart(editedPrevious2)) continue;
                      Annotation originalPrevious2 = getCounterpart(editedPrevious2);
                      if (originalPrevious2.getEnd() != anOriginal.getStart())
                      { // found a different but linked anchor via the edited graph structure
                        newAnchor = originalPrevious2.getEnd();
                        log(layerId, ": Found end anchor linked via ", originalPrevious2);
                        break;
                      }
                    } // next annotation that's parallel to this parallel annotation
                    log(layerId, ": Different start anchor for ", anOriginal, 
                        ": new anchor for ending ", previousAnnotation, " - ", newAnchor);
                    changes.addAll( // track changes of:
                      previousAnnotation.setEnd(newAnchor));
                  } // they shouldn't be linked
                } // there is a corresponding edited parallel annotation
              } // next anchor using this as an end anchor
		     
              // do the same for annotations that start here
              for (Annotation parallelAnnotation : removeDeleted(anOriginal.getStart().getStartingAnnotations()))
              {
                if (parallelAnnotation == anOriginal) continue; // not ourselves
                if (parallelAnnotation == anOriginal.getParent()) continue; // not our parent
                Layer otherLayer = parallelAnnotation.getLayer();
                // check for other possible start anchor, by following the edited graph structure
                Annotation editedParallelAnnotation = getCounterpart(parallelAnnotation);
                if (editedParallelAnnotation != null)
                {
                  // only if they're not linked in the edited graph
                  if (editedParallelAnnotation.getStart() != anEdited.getStart())
                  {
                    for (Annotation editedParallel2 
                           : editedParallelAnnotation.getStart().getStartingAnnotations())
                    {
                      Annotation originalParallel2 = getCounterpart(editedParallel2);
                      if (originalParallel2 == null) continue;
                      if (originalParallel2.getStart() != anOriginal.getStart())
                      { // found a different but linked anchor via the edited graph structure
                        newAnchor = originalParallel2.getStart();
                        log(layerId, ": Found start anchor linked via ", originalParallel2);
                        break;
                      }
                    } // next annotation that's parallel to this parallel annotation
                    parallelAnnotation.setStart(newAnchor);
                    log(layerId, ": Different start anchor for ", anOriginal,
                        ": new anchor for starting ", parallelAnnotation);
                  } // they shouldn't be linked
                } // there is a corresponding edited parallel annotation
              } // next anchor using this as an end anchor
            } 
            // are we changing this anchor?
            if (delta != null)
            {
              anOriginal.getStart().setOffset(delta.getOffset());
              setConfidence(anOriginal.getStart(), getConfidence(delta));
              log(layerId, ": Different start anchor for ", anOriginal,
                  ": changing offset to ", delta.getOffset());
            } // there is a delta to apply
          } // theirs is more trustworthy
        } // offsets are different
	    
        // is there a previous annotation?
        if (anLastOriginal != null
            // are the offsets the same in our graph?
            && compare(anLastOriginal.getEnd(), anOriginal.getStart()) == 0)
        { // previous annotation ending where this one starts
          // do they share anchors in the edited version of the graph?
          Annotation anLastEdited = getCounterpart(anLastOriginal);
          if (anLastEdited.getEnd() == anEdited.getStart()
              // are they currently two separate anchors?
              && anLastOriginal.getEnd() != anOriginal.getStart())
          {
            log(layerId, ": Share anchors between ", anLastOriginal, " and ", anOriginal);
            // ensure that the end anchor for the last annotation is updated
            changes.addAll( // track changes of:
              changeEndWithRelatedAnnotations(anLastOriginal, anOriginal.getStart()));
          }
          // do they *not* share anchors in the edited version of the graph?
          else if (anLastEdited.getEnd() != anEdited.getStart()
                   // are they currently sharing anchors?
                   && anLastOriginal.getEnd() == anOriginal.getStart())
          { // not sharing in editedGraph
            log(layerId, ": Un-share anchors between ", anLastOriginal, " and ", anOriginal);
            // create a new anchor for unrelated annotations that link to this one 
            // (this will include anLastOriginal)
            Anchor newAnchor = new Anchor(anOriginal.getStart());
            newAnchor.create();
            graph.addAnchor(newAnchor);
            changes.addAll( // track changes of:
              newAnchor.getChanges());
            for (Annotation previousAnnotation 
                   : removeDeleted(anOriginal.getStart().getEndingAnnotations()))
            {
              if (previousAnnotation == anOriginal) continue; // instantaneous
              Layer otherLayer = previousAnnotation.getLayer();
              // check for other possible end anchor, by following the edited graph structure
              Annotation editedPreviousAnnotation = getCounterpart(previousAnnotation);
              if (editedPreviousAnnotation != null)
              {
                // only if they're not linked in the edited graph
                if (editedPreviousAnnotation.getEnd() != anEdited.getStart())
                {
                  for (Annotation editedPrevious2 
                         : editedPreviousAnnotation.getEnd().getEndingAnnotations())
                  {
                    Annotation originalPrevious2 = getCounterpart(editedPrevious2);
                    if (originalPrevious2.getEnd() != anOriginal.getStart())
                    { // found a different but linked anchor via the edited graph structure
                      newAnchor = originalPrevious2.getEnd();
                      log(layerId, ": Found end anchor linked via ", originalPrevious2);
                      break;
                    }
                  } // next annotation that's parallel to this parallel annotation
                  log(layerId, ": Different start anchor for ", anOriginal,
                      ": new anchor for ending ", previousAnnotation, " -- ", newAnchor);
                  changes.addAll( // track changes of:
                    changeEndWithRelatedAnnotations(previousAnnotation, newAnchor));
                } // they shouldn't be linked
              } // there is a corresponding edited parallel annotation
            } // next anchor using this as an end anchor	       
          } // not sharing in editedGraph
        } // previous annotation ending where this one starts

      } // start is not a dummy anchor

      if (!dummyAnchors.contains(anOriginal.getEnd()))
      {
        // end anchor
        if (anEdited.getInstantaneous())
        { // instantaneous annotation
          if (!anOriginal.getInstantaneous())
          {
            anOriginal.setEnd(anOriginal.getStart());
            log(layerId, ": Forcing instantaneity on ", anOriginal);
          }
        }
        else
        {
          assert anOriginal.getEnd() != null : "anOriginal.getEnd() != null: " + anOriginal;
          assert anEdited.getEnd() != null : "anEdited.getEnd() != null: " + anEdited;
          boolean bCheckEndAnchorOffset = true;
	       
          // there may be reasons to relink the
          // annotation to another anchor - i.e. it's linked differently in the edited graph
          boolean bChanged = false;
          for (Annotation anParallel : anEdited.getEnd().getEndingAnnotations())
          {
            if (anParallel == anEdited) continue;		  
            if (hasCounterpart(anParallel))
            {
              Annotation anLinkedOriginalParallel = getCounterpart(anParallel);
              if (anOriginal.getEnd() != anLinkedOriginalParallel.getEnd()
                  && getConfidence(anOriginal.getEnd())
                  <= getConfidence(anLinkedOriginalParallel.getEnd()))
              { // link this annotation to the parallel one
                // only link to annotations that have been through this phase
                if (!anLinkedOriginalParallel.containsKey("@computeAnchorDeltasForMerge")) continue;
                // don't share this anchor if there's an annotation connected that we shouldn't share with
                if (layer.getSaturated() 
                    || anLinkedOriginalParallel.getEnd().endOf(layer.getParentId()).size() == 0)
                {
                  log(layerId, ": Share end anchor of ", anOriginal, 
                      " with parallel ", anLinkedOriginalParallel);
                  // ensure that the end anchor for the last annotation is updated
                  if (anEdited.getInstantaneous()) anOriginal.setStart(anLinkedOriginalParallel.getEnd());
                  anOriginal.setEnd(anLinkedOriginalParallel.getEnd());
                  bChanged = true;
                  break;
                } // bShare
              }
            } // their-parallel has a counterpart
          } // next parallel their-annotation
          if (!bChanged)
          {
            // or maybe its shared for us but *not* shared for them
            for (Annotation anParallel : removeDeleted(anOriginal.getEnd().getEndingAnnotations()))
            {
              if (anParallel == anOriginal) continue;		  
              if (anParallel.getLayerId() == anOriginal.getLayerId()) continue;		  
              if (hasCounterpart(anParallel))
              {
                Annotation anEditedParallel = getCounterpart(anParallel);
                if (anEdited.getEnd() != anEditedParallel.getEnd()
                    && !dummyEditedAnchors.contains(anEdited.getEnd()))
                {
                  // SHOULD they share? Is there a saturated relationship between the layers (annotation in the child layer)
                  Layer parallelLayer = graph.getLayer(anEditedParallel.getLayerId());
                  if ((layer.getParentId().equals(anEditedParallel.getLayerId())
                       && layer.getSaturated())
                      || (parallelLayer != null && parallelLayer.getParentId().equals(layerId)
                          && parallelLayer.getSaturated()))
                  {
                    if (!layer.getParentId().equals(anEditedParallel.getLayerId())) // parent layer
                    {
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
                  changes.addAll( // track changes of:
                    newAnchor.getChanges());
			   
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
              } // their-parallel has a counterpart
            } // next possible parallel
          }	    
	       
          Anchor delta = null;
          if (bCheckEndAnchorOffset
              && compare(anEdited.getEnd(), anOriginal.getEnd()) != 0)
          {
            if (ignoreOffsetConfidence
                || getConfidence(anEdited.getEnd()) 
                >= getConfidence(anOriginal.getEnd()))
            {
              Anchor matchingMergedAnchor = null;
              // try for parallel annotations on another layer
              for (Annotation anEditedParallel : anEdited.getEnd().getEndingAnnotations())
              {		     
                if (anEditedParallel == anEdited) continue; // skip ourselves
                if (anEditedParallel.getLayerId() == anEdited.getLayerId()) continue; // on our own layer
                Annotation anOriginalParallel = getCounterpart(anEditedParallel);
                // skip unmerged annotations from a neighboring fragments (TODO not sure there can be any)
                if (anOriginalParallel == null) continue; 
                assert anOriginalParallel.getEnd() != null 
                  : "anOriginalParallel.getEnd() != null - " + anOriginalParallel;
                // no use linking to the very same anchor
                if (anOriginalParallel.getEnd() == anOriginal.getEnd()) continue;
                // offset should be the same
                if (compare(anOriginalParallel.getEnd(), anEditedParallel.getEnd()) != 0
                    // unless our offset is less trustworthy than theirs
                    && getConfidence(anEditedParallel.getEnd()) >= Constants.CONFIDENCE_AUTOMATIC
                    && getConfidence(anOriginalParallel.getEnd()) 
                    <= getConfidence(anEditedParallel.getEnd())) continue;
                matchingMergedAnchor = anOriginalParallel.getEnd();
                log(layerId, ": Different end anchor for ", anOriginal,
                    ": linking to shared end of ", anOriginalParallel);
                break;
              } // next possible start anchor annotation
              if (matchingMergedAnchor != null)
              {
                // this, and all parallel annotation on *unrelated* layers come with us
                for (Annotation an : anOriginal.getEnd().getEndingAnnotations())
                {
                  if (an == anOriginal) continue;
                  // unrelated layer?
                  Layer otherLayer = an.getLayer();
                  if (!layer.getParentId().equals(an.getLayerId())
                      && otherLayer.getParentId().equals(layerId))
                  {
                    log(layerId, ": Different end anchor for ", anOriginal,
                        ": linking parallel ", an, " too");
                    if (an.getInstantaneous())
                    { // instant
                      changes.addAll( // track changes of:
                        an.setStart(matchingMergedAnchor));
                    }
                    changes.addAll( // track changes of:
                      an.setEnd(matchingMergedAnchor));
                  }
                }
                changes.addAll( // track changes of:
                  anOriginal.setEnd(matchingMergedAnchor));
              }
              else
              { // no already-merged anchor we can use
                // we might need a new anchor, if this is also the start anchor for
                // another annotation with a different edited offset
			
                boolean bSplitFromFollowing = false;
                for (Annotation anFollowing : anOriginal.getEnd().startOf(layerId))
                {
                  Annotation anEditedFollowing = getCounterpart(anFollowing);
                  if (anEditedFollowing != null
                      && anEditedFollowing.getStart() != anEdited.getEnd())
                  {
                    bSplitFromFollowing = true;
                    break;
                  }
                } // next following annotation
			
                if (!dummyEditedAnchors.contains(anEdited.getEnd()))
                {
                  if (bSplitFromFollowing)
                  {
                    delta = new Anchor(anEdited.getEnd());
                    delta.create();
                  }
                  else
                  {
                    // change this anchor
                    delta = new Anchor(anEdited.getEnd());
                  }
                }
              } // change the anchor
            } // theirs is more trustworthy than ours
		  
            // applying change to anchor?
            if (delta != null)
            {
              if (delta.getChange() != Change.Operation.Create)
              {
                anOriginal.getEnd().setOffset(delta.getOffset());
                setConfidence(anOriginal.getEnd(), getConfidence(delta));
                log(layerId, ": Different end anchor for ", anOriginal,
                    ": changing offset to  ", delta.getOffset());
              }
              else // Create
              {
                Anchor newAnchor = delta;
                graph.addAnchor(newAnchor);
                changes.addAll( // track changes of:
                  newAnchor.getChanges());
			
                Set<String> excludeLayers = new HashSet<String>();
                if (layer.getSaturated()) excludeLayers.add(layerId);
                changeEndWithRelatedAnnotations(anOriginal, newAnchor, excludeLayers);
                log(layerId, ": Different end anchor for ", anOriginal,
                    ": new anchor at ", delta.getOffset());
              }
            } // there's a delta
          } // offsets are different
	       
          // check for reversed anchors
          if (anOriginal.getEnd().getOffset() != null && anOriginal.getStart().getOffset() != null
              && anOriginal.getEnd().getOffset() < anOriginal.getStart().getOffset())
          {
            // is the start anchor for realignment anyway?
            if (getConfidence(anOriginal.getStart()) == Constants.CONFIDENCE_NONE)
            {
              log(layerId, ": Reversed anchors: ", anOriginal,
                  ": start is soft, so moving before end");
              // reset the offset
              double dNewOffset = anOriginal.getEnd().getOffset() - smidgin;
              if (anLastOriginal != null)
              { // halfway through the previous
                dNewOffset = anLastOriginal.getStart().getOffset()
                  + ((anOriginal.getEnd().getOffset() 
                      - anLastOriginal.getStart().getOffset())/2);
              }
              anOriginal.getStart().setOffset(dNewOffset);
              setConfidence(anOriginal.getStart(), Constants.CONFIDENCE_NONE);
            }
          }
        } // not an instananeous annotation
      } // end is not a dummy anchor

      anOriginal.put("@computeAnchorDeltasForMerge", Boolean.TRUE);
      anLastOriginal = anOriginal;	 
    } // next edited annotation
    return changes;
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
   * @return The resulting changes.
   * @throws TransformationException On error.
   */
  protected Vector<Change> checkChildrenForMerge(Layer layer, Graph graph)
    throws TransformationException
  {
    Vector<Change> changes = new Vector<Change>();
    Layer parentLayer = layer.getParent();
    if (parentLayer == null) return changes; // top level layer
    String layerId = layer.getId();
    String parentLayerId = parentLayer.getId();

    if (layer.getPeers() && !layer.getPeersOverlap())
    {
      boolean editGraphHasChildLayer = editedGraph.getLayer(layerId) != null;
      TreeSet<String> partitionIds = new TreeSet<String>();
      if (layer.getParentIncludes() && editGraphHasChildLayer)
      { // parentIncludes, so can be partitioned
        // identify partition layers
        for (Layer peerLayer : parentLayer.getChildren().values())
        {
          if (!peerLayer.getId().equals(layer.getId())
              && peerLayer.equals(editedGraph.getLayer(peerLayer.getId()))
              && peerLayer.getParentIncludes()
              && peerLayer.getAlignment() == Constants.ALIGNMENT_INTERVAL
              && peerLayer.getPeers()
              && !peerLayer.getPeersOverlap()
              && peerLayer.getSaturated())
          {
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
      for (Annotation anParent : editedGraph.list(parentLayerId))
      {
        Annotation anOriginalParent = getCounterpart(anParent);
        log(layerId, ": Parent ", anOriginalParent);
        // there may be no original parent because noChangeLayers forbade its addition
        if (anOriginalParent == null) continue;

        TreeSet<Annotation> byOrdinalOrOffset = new TreeSet<Annotation>(new AnnotationComparatorByOrdinal());
        HashSet<Annotation> myChildren = new HashSet<Annotation>();
        if (editGraphHasChildLayer)
        { // edited graph includes child layer, so use its annotations to get to the originals
          byOrdinalOrOffset.addAll(anParent.getAnnotations(layerId));
          for (Annotation an : byOrdinalOrOffset) myChildren.add(getCounterpart(an));
        }
        else
        { // edited graph doesn't include child layer, so use the original graph directly
          byOrdinalOrOffset.addAll(anOriginalParent.getAnnotations(layerId));
          for (Annotation an : byOrdinalOrOffset) myChildren.add(an);
        }

        // gather up partitions, so we can sprinkle their annotations throughout the children
        HashMap<String,Annotation> currentPartition = new HashMap<String,Annotation>();
        HashMap<String,Iterator<Annotation>> partitionIterators = new HashMap<String,Iterator<Annotation>>();
        for (String partitionLayerId : partitionIds)
        {
          // get an iterator through the partitions
          Iterator<Annotation> i = anParent.getAnnotations(partitionLayerId).iterator();
          if (i.hasNext())
          {
            partitionIterators.put(partitionLayerId, i);
            // set current partition
            currentPartition.put(partitionLayerId, i.next());
          }
        } // next partition layer	    

        SortedSet<Annotation> children = byOrdinalOrOffset;
        // special case:
        // if the child layer is in the original only
        if (!editGraphHasChildLayer
            // and the relationship is saturated
            && layer.getSaturated())
        {
          // then we trust that the children are in the correct order 
          // and share end/start anchors correctly
          // instead of trusting their anchors, which might have been messed with by the parent anchor changing
          // (e.g. parent = word with changed default anchoring, and child = syllables with original child anchoring)
          children = new AnnotationChain(myChildren);

          // any children that are not in the chain shouldn't be there!
          Iterator<Annotation> iMyChildren = myChildren.iterator();
          while (iMyChildren.hasNext())
          {
            Annotation an = iMyChildren.next();
            if (!children.contains(an))
            {
              if (getConfidence(an) <= Constants.CONFIDENCE_AUTOMATIC)
              {
                changes.add( // record changes for:
                  an.destroy());
              }
              iMyChildren.remove();
            }
          }
        } // special case where layer isn't in editedGraph and is saturated

        // create an ordered (by child order) set of anchors
        LinkedList<Anchor> anchors = new LinkedList<Anchor>();
        // if the children must be t-included...
        if (layer.getParentIncludes())
        { // start with an immovable anchor at the beginning of the parent
          anchors.add(new Anchor(null, anOriginalParent.getStart().getOffset(), 
                                 Integer.MAX_VALUE));
        }
        // add all (original) child anchors
        Annotation lastChild = null;
        for (Annotation anChild : removeDeleted(children))
        {
          Annotation anOriginalChild = anChild;
          if (editGraphHasChildLayer)
          { // edited graph includes child layer, so use its annotations to get to the originals
            anOriginalChild = getCounterpart(anChild);
          }
          log(layerId, ": Child ", anOriginalChild); // TODO comment out
          // there may be no counterpart in the original graph
          // because adding was forbidden by noChangeLayers
          if (anOriginalChild == null) continue;

          // check for new partition anchor
          Double minStart = anChild.getStart().getOffsetMin();
          Double maxEnd = anChild.getEnd().getOffsetMax();
          log(layerId, ": Edited child between ", minStart, " and ", maxEnd); // TODO comment out
          if (minStart != null && maxEnd != null)
          {
            for (String partitionLayerId : partitionIds)
            {
              Iterator<Annotation> i = partitionIterators.get(partitionLayerId);
              if (i == null) continue;
              Annotation currentPartitionEdited = currentPartition.get(partitionLayerId);
              Annotation currentPartitionOriginal = getCounterpart(currentPartitionEdited);
              assert currentPartitionOriginal != null : "currentPartitionOriginal != null";
              // assume that partition layer already saturates parent, and add end anchors
              // into the collection as appropriate
              double midPoint = minStart + ((maxEnd-minStart) / 2);
              while (!currentPartitionEdited.includesOffset(midPoint))
              {
                Anchor possibleBoundary = new Anchor(null, currentPartitionOriginal.getEnd().getOffset(), 
                                                     Integer.MAX_VALUE);
			
                log("Partition end: ", currentPartitionOriginal, " ", currentPartitionOriginal.getEnd());
                // if (!i.hasNext())
                // {
                //    log("No more partitions on: ", partitionLayerId);
                //    break;
                // }
                currentPartitionEdited = i.next();
                currentPartition.put(partitionLayerId, currentPartitionEdited);
                if (currentPartitionEdited.includes(anChild))
                {
                  anchors.add(possibleBoundary);
                }
                else
                {
                  log("Skipping partition as ", currentPartitionEdited,
                      " doesn't include edited version of ", anChild, " between ", minStart, "-", maxEnd);
                }
              } // next non-including partition		  
            } // next partition layer	    
          } // child bounds are known

          // start anchor
          String sShareLastAnchorReason = null;
          String sNewAnchorReason = null;
          if (bNoInterSharingForChildren)
          {
            Vector<Annotation> childrenStartingHere 
              = removeDeleted(anOriginalChild.getStart().startOf(layerId));
            Vector<Annotation> childrenEndingHere 
              = removeDeleted(anOriginalChild.getStart().endOf(layerId));
            if (lastChild == null)
            { // first child
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
            } // first child
            else
            { // not first child
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
            if (sNewAnchorReason == null && sShareLastAnchorReason == null)
            {
              Vector<Annotation> parentsStartingHere 
                = new Vector<Annotation>(anOriginalChild.getStart().startOf(parentLayerId));
              Vector<Annotation> parentsEndingHere 
                = new Vector<Annotation>(anOriginalChild.getStart().endOf(parentLayerId));
              if (lastChild == null)
              { // first child
                if (parentsStartingHere.size() == 1
                    && parentsStartingHere.firstElement() != anOriginalParent) sNewAnchorReason = "first child shares anchor with parent layer annotation but not parent";
                else if (parentsStartingHere.size() > 2) sNewAnchorReason = "first child shares start anchor with multiple parent layer annotation starts";
                else if (parentsEndingHere.size() > 1) sNewAnchorReason = "first child shares anchor with multiple parent layer annotation ends";
              } // first child
              else
              { // not first child
                // any parent-layer annotation starts here
                if (parentsStartingHere.size() > 0) sShareLastAnchorReason = "child shares anchor with parent layer annotation start";
                // any parent-layer annotation ends here
                else if (parentsEndingHere.size() > 0) sNewAnchorReason = "child shares anchor with parent layer annotation end";
              } // not first child
            } // sNewAnchorReason and sShareLastAnchorReason still null
          } // bNoInterSharingForChildren
          if (sShareLastAnchorReason != null)
          {
            changes.addAll( // record changes for:
              changeStartWithRelatedAnnotations(anOriginalChild, lastChild.getEnd(), bothLayers));
            log(layerId, ": Using end of previous child: ", anOriginalChild, ": ", sShareLastAnchorReason);
          }
          else if (sNewAnchorReason != null)
          {
            Anchor anNewAnchor = new Anchor(anOriginalChild.getStart());
            anNewAnchor.create();
            graph.addAnchor(anNewAnchor);
            changes.addAll( // record changes for:
              anNewAnchor.getChanges());
            changes.addAll( // record changes for:
              changeStartWithRelatedAnnotations(anOriginalChild, anNewAnchor, bothLayers));
            log(layerId, ": New start anchor for: ", anOriginalChild, ": ", sNewAnchorReason);
          }
          assert anchors != null : "anchors != null";
          assert anOriginalChild != null : "anOriginalChild != null";
          if (!anchors.contains(anOriginalChild.getStart()))
          {
            anchors.add(anOriginalChild.getStart());
          }
	       
          // end anchor
          sNewAnchorReason = null;
          if (bNoInterSharingForChildren)
          {
            Vector<Annotation> childrenStartingHere 
              = removeDeleted(anOriginalChild.getEnd().startOf(layerId));
            Vector<Annotation> childrenEndingHere 
              = removeDeleted(anOriginalChild.getEnd().endOf(layerId));
            Annotation anLastOriginalChild = children.last();
            if (editGraphHasChildLayer)
            { // edited graph includes child layer, children contains the edited version
              anLastOriginalChild = getCounterpart(anLastOriginalChild);
            }	       
            if (anOriginalChild != anLastOriginalChild)
            { // not last child
              // any parent-layer annotation starts here
              if (childrenEndingHere.size() > 1) 
                sNewAnchorReason = "child shares anchor with other child annotation end";
              else if (childrenStartingHere.size() > 2) 
                sNewAnchorReason = "child shares anchor with multiple child annotation starts";
            } // not last child
            else
            { // last child
              if (childrenEndingHere.size() > 1) 
                sNewAnchorReason = "last child shares anchor with other child annotation end";
              else if (childrenStartingHere.size() > 2) 
                sNewAnchorReason = "last child shares anchor with multiple child annotation starts";
            } // last child
            if (sNewAnchorReason == null)
            {
              Vector<Annotation> parentsStartingHere 
                = new Vector<Annotation>(anOriginalChild.getEnd().startOf(parentLayerId));
              Vector<Annotation> parentsEndingHere
                = new Vector<Annotation>(anOriginalChild.getEnd().endOf(parentLayerId));
              if (anOriginalChild != anLastOriginalChild)
              { // not last child
                if (parentsEndingHere.size() > 0) sNewAnchorReason = "child shares anchor with parent layer annotation end";
                if (parentsStartingHere.size() > 0) sNewAnchorReason = "child shares anchor with parent layer annotation start - " + anLastOriginalChild;
              } // not last child
              else
              { // last child
                if (parentsEndingHere.size() == 1
                    && parentsEndingHere.firstElement() != anOriginalParent) sNewAnchorReason = "last child shares anchor with parent layer annotation but not parent";
                else if (parentsEndingHere.size() > 2) sNewAnchorReason = "last child shares anchor with multiple parent layer annotation ends";
                else if (parentsStartingHere.size() > 1) sNewAnchorReason = "last child shares anchor with multiple parent layer annotation starts";
              } // last child
            } // sNewAnchorReason still null
          } // bNoInterSharingForChildren
	       
          if (sNewAnchorReason != null)
          {
            Anchor anNewAnchor =  new Anchor(anOriginalChild.getEnd());
            anNewAnchor.create();
            graph.addAnchor(anNewAnchor);
            changes.addAll( // record changes for:
              anNewAnchor.getChanges());
            changes.addAll( // record changes for:
              changeEndWithRelatedAnnotations(anOriginalChild, anNewAnchor, bothLayers));
            log(layerId, ": New end anchor for: ", anOriginalChild, ": ", sNewAnchorReason);
          }
	       
          if (!anchors.contains(anOriginalChild.getEnd()))
          {
            anchors.add(anOriginalChild.getEnd());
          }
	       
          lastChild = anOriginalChild;
        } // next child
        // if the children must be t-included...
        if (layer.getParentIncludes())
        {  // end with an immovable anchor at the end of the parent
          anchors.add(new Anchor(null, anOriginalParent.getEnd().getOffset(), 
                                 Integer.MAX_VALUE));
        }

        // the anchors in our ordered set must be in offset order - i.e.
        // for each anchor, anchor.offset >= predecessor.offset
        // we force this to be true by moving errant anchors, prioritising by alignment status
        ListIterator<Anchor> itAnchors = anchors.listIterator();
        Anchor predecessor = null;
        while (itAnchors.hasNext())
        {
          Anchor anchor = itAnchors.next();
          log(layerId, ": anchor: ", anchor, " (", predecessor, ")"); // TODO comment out
          if (anchor.getOffset() == null) continue; // ignore anchors with no offset
          if (predecessor != null && predecessor.getOffset() != null)
          {
            if (anchor.getOffset() < predecessor.getOffset())
            { // out of order
              log(layerId, ": Out of order: ", anchor, " (", predecessor, ")"); // TODO comment out
              // which has the higher status?
              if (getConfidence(anchor) <= getConfidence(predecessor)
                  // but we also don't want to run past the end of the parent
                  && (!layer.getParentIncludes() // ...if the layer is parentIncludes
                      || anchor.getOffset() < anOriginalParent.getEnd().getOffset()))
              { // anchor.confidence < predecessor.confidence
                // easy case - just change this anchor and keep going
                // does unwinding the delta help?
                if (anchor.getOriginalOffset() >= predecessor.getOffset())
                { // old value was ok, so just use that
                  anchor.rollback(); // TODO track changes?
                  log(layerId, ": Out of order, reverting change: ", anchor, " (", predecessor, ")");
                }
                else
                { 
                  // have to make up a new offset - make it slightly more than the predecessor
                  // and mark it for default alignment
                  double dOriginalOffset = anchor.getOffset();
                  double dNewOffset = predecessor.getOffset() + smidgin;
                  changes.addAll( // record changes for:
                    anchor.setOffset(dNewOffset));
                  setConfidence(anchor, Constants.CONFIDENCE_NONE);
                  log(layerId, ": Out of order; changing offset: ", anchor, " (", predecessor, ")");
                  // the offset is moving forward, so ending child annotations will be reset
                  for (Annotation anStartingHere : removeDeleted(anchor.startOf(layerId)))
                  {
                    if (!myChildren.contains(anStartingHere))
                    {
                      continue; // ignore non-children
                    }
                    if (anStartingHere.getEnd().getOffset() != null
                        && anStartingHere.getEnd().getOffset() < dNewOffset)
                    {
                      // if the end anchor is in the past, it will need moving too
                      changes.addAll( // record changes for:
                        anStartingHere.getEnd().setOffset(dNewOffset + smidgin));
                      setConfidence(anStartingHere.getEnd(), Constants.CONFIDENCE_NONE);
                      log(layerId, ": Out of order, changing offset of end anchor: ", anStartingHere);
                    }
                    changes.addAll( // record changes for:
                      resetChildAnchorsBefore(anStartingHere, dNewOffset));
                  } // next annotation that start here
                }
              } // anchor.confidence <= predecessor.confidence
              else
              { // anchor.status > predecessor.status
                // more tricky case, we have to go backwards, resetting anchors of lower status
                // until either we reach our offset, or an anchor with higher status
                boolean bChangeCurrentAnchor = false;
                boolean bRevertWouldSolve = true;
                int iChangeCount = 0;
                double dEndOffset = anchor.getOffset();
                double dFutureOriginalOffset = anchor.getOffset();
                double dLowestOriginalOffset = anchor.getOffset();
                while (itAnchors.hasPrevious())
                {
                  predecessor = itAnchors.previous();
                  if (predecessor == null) continue; // skip if there's none
                  if (predecessor == anchor) continue; // skip the one we just got
                  if (predecessor.getOffset() == null) continue; // skip no-offset anchors

                  dLowestOriginalOffset = Math.min(dLowestOriginalOffset, predecessor.getOriginalOffset());

                  // if we get to anchor that has a higher confidence than anchor.confidence, we stop
                  if (getConfidence(predecessor) > getConfidence(anchor))
                  {
                    if (predecessor.getOriginalOffset() > dFutureOriginalOffset) bRevertWouldSolve = false;
                    bChangeCurrentAnchor = true; // change anchor as well as those prior to it
                    itAnchors.next(); // reset iterator so that next = first anchor to change
                    break;
                  }
                  // if we've gone far enough back
                  else if (predecessor.getOffset() < anchor.getOffset())
                  {
                    if (predecessor.getOriginalOffset() > dFutureOriginalOffset) bRevertWouldSolve = false;
                    itAnchors.next(); // reset iterator so that next = first anchor to change
                    break;
                  } // higher status anchor or reached anchor.offset
                  else
                  {
                    // TODO check if simply reverting would fix
                    if (getConfidence(predecessor) == getConfidence(anchor)
                        && predecessor != anchor)
                    {
                      bChangeCurrentAnchor = false; // change anchor as well as those prior to it
                    }
                    // would reverting to the original offset help?
                    if (predecessor.getOriginalOffset() > dFutureOriginalOffset) bRevertWouldSolve = false;
                    dFutureOriginalOffset = predecessor.getOffset();
                    iChangeCount++;
                  }
                } // previous anchor
                // if we hit the beginning of the list
                if (!itAnchors.hasPrevious())
                {
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
                if (dDuration <= 0.0)
                {
                  dDuration = iChangeCount * smidgin;
                  log(layerId, ": Out of order, nudging ", iChangeCount, " forward from: ", dStartOffset);
                }
                else
                {
                  log(layerId, ": Out of order, resetting ", iChangeCount,
                      " anchors between: ", dStartOffset, " and ", dEndOffset);
                }
			
                // move forward through the iterator until we hit anchor
                int iChange = 1;
                Anchor resetChildrenBefore = null;
                while (itAnchors.hasNext()) // (we should never get to the end)
                {
                  Anchor anchorToChange = itAnchors.next();
                  if (anchorToChange.getOffset() == null) continue;
                  if (bRevertWouldSolve)
                  {
                    if (bChangeCurrentAnchor // if some prior anchors have status >= anchor.status
                        // or all the prior anchors are lower status, so anchor doesn't change
                        || anchorToChange != anchor)
                    {
                      anchorToChange.rollback(); // TODO track changes?
                    }
                    log(layerId, ": Out of order, reverting previous anchor (",
                        anchorToChange, "): ", anchor, " (", predecessor, ")");
                  }
                  else
                  {
                    // if some prior anchors have status >= anchor.status
                    if (bChangeCurrentAnchor
                        // or all the prior anchors are lower status, so anchor doesn't change
                        || anchorToChange != anchor)
                    {
                      // offset a little more than the last one
                      double dOriginalOffset = anchorToChange.getOffset();
                      double dNewOffset = dStartOffset + (iChange * dDuration / iChangeCount);
                      changes.addAll( // record changes for:
                        anchorToChange.setOffset(dNewOffset));
                      setConfidence(anchorToChange, Constants.CONFIDENCE_NONE);
				 
                      if (resetChildrenBefore != null)
                      {
                        // the last loop involved moving an offset forward.
                        // the offset is moving forward, so starting child annotations will be reset
                        for (Annotation anStartingHere : resetChildrenBefore.startOf(layerId))
                        {
                          if (!myChildren.contains(anStartingHere)) continue; // ignore non-children
                          changes.addAll( // record changes for:
                            resetChildAnchorsBefore(anStartingHere, resetChildrenBefore.getOffset()));
                        } // next annotation that starts here
                      }
                      resetChildrenBefore = null;

                      if (dOriginalOffset > dNewOffset)
                      {
                        // the offset is moving back, so ending child annotations will be reset
                        for (Annotation anEndingHere : anchorToChange.endOf(layerId))
                        {
                          changes.addAll( // record changes for:
                            resetChildAnchorsAfter(anEndingHere, dNewOffset));
                        } // next annotation that ends here
                      } // dOriginalOffset > dNewOffset
                      else if (dOriginalOffset < dNewOffset)
                      {
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
			
                if (resetChildrenBefore != null)
                {
                  // the last loop involved moving an offset forward.
                  // the offset is moving forward, so starting child annotations will be reset
                  for (Annotation anStartingHere : resetChildrenBefore.startOf(layerId))
                  {
                    if (!myChildren.contains(anStartingHere)) continue; // ignore non-children
                    changes.addAll( // record changes for:
                      resetChildAnchorsBefore(anStartingHere, resetChildrenBefore.getOffset()));
                  } // next annotation that starts here
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
	    
        if (layer.getSaturated() && children.size() > 0)
        {
          Annotation anOriginalChild = children.first();
          if (editGraphHasChildLayer)
          { // edited graph includes child layer, children contains the edited version
            anOriginalChild = getCounterpart(anOriginalChild);
          }
          if (anOriginalChild != null)
          {
            if (anOriginalParent.getStart() != anOriginalChild.getStart()
                // but not if we reconstructed the parent's anchor
                && !dummyAnchors.contains(anOriginalParent.getStart()))
            {
              changes.addAll( // record changes for:
                changeStartWithRelatedAnnotations(anOriginalParent, anOriginalChild.getStart(), layerId));
              log(layerId, ": Share start of  first child ", anOriginalChild, 
                  " with parent ", anOriginalParent);
            }
          } // anOriginalChild != null
          
          anOriginalChild = children.last();
          if (editGraphHasChildLayer)
          { // edited graph includes child layer, children contains the edited version
            anOriginalChild = getCounterpart(anOriginalChild);
          }
          if (anOriginalChild != null)
          {
            if (anOriginalParent.getEnd() != anOriginalChild.getEnd()
                // but not if we reconstructed the parent's anchor
                && !dummyAnchors.contains(anOriginalParent.getEnd()))
            {
              changes.addAll( // record changes for:
                changeEndWithRelatedAnnotations(anOriginalParent, anOriginalChild.getEnd(), layerId));
              log(layerId, ": Share end of last child ", anOriginalChild, 
                  " with parent ", anOriginalParent);
            }
          } // anOriginalChild != null
        } // saturated and there are children
          
        anLastOriginalParentsLastChild = lastChild;
      } // next parent
    } // peers && !peersOverlap
    return changes;
  }

   
  /**
   * Removes elements from the collection that are marked for deletion.
   * @param collection The collection to use
   * @return A new collection, all the elements of <var>collection</var> except those where {@link TrackedMap#getChange()} is {@link Change}.Operation.Destroy.
   */
  public Vector<Annotation> removeDeleted(Collection<Annotation> collection)
  {
    Vector<Annotation> annotations = new Vector<Annotation>();
    for (Annotation a : collection)
    {
      if (a.getChange() != Change.Operation.Destroy)
      {
        annotations.add(a);
      }
    }
    return annotations;
  } // end of removeDeleted()


  /**
   * Resets the anchors of the children of the given
   * annotation. After this method, all children on a given layer
   * will be s-included (i.e. chained from the start anchor to the
   * end anchor), and all anchors that previously had an offset at or before the threshold 
   * will have the offset set to null and the confidence set to
   * {@link Constants#CONFIDENCE_NONE}. All changed anchors are new anchors.
   * @param parent The parent whose children should be changed.
   * @param threshold The offset before which anchors will be reset.
   * @return The changes made during this operation.
   */
  protected Vector<Change> resetChildAnchorsBefore(Annotation parent, double threshold)
  {
    log("resetChildAnchorsBefore ", parent, " ", threshold);
    Vector<Change> changes = new Vector<Change>();
    for (String childLayerId : parent.getAnnotations().keySet())
    {
      // ignore non-interval layers
      Layer childLayer = parent.getGraph().getLayer(childLayerId);
      if (childLayer == null) continue;
      if (childLayer.getAlignment() != Constants.ALIGNMENT_INTERVAL) continue;
      // ignore relationships that aren't saturated
      if (!childLayer.getSaturated()) continue;
      // ignore relationships that are not t-included
      if (!childLayer.getParentIncludes()) continue;

      Annotation lastChild = null;
      for (Annotation child : parent.getAnnotations(childLayerId))
      {
        // TODO do we need to check it's still really a child?
        if (lastChild == null)
        {
          if (!child.getStartId().equals(parent.getStartId()))
          {
            // first child - start anchor is shared with parent
            log("set first child ", child, " to share start with parent");
            changes.addAll( // record changes for:
              child.setStartId(parent.getStartId()));
          }
        }
        else
        { // not the first child
          // first deal with end of last child
          if (lastChild.getEnd().getOffset() != null
              && lastChild.getEnd().getOffset() <= threshold)
          {
            // check child.start.offset first to see if nulling can be avoided
            // if the current child start is not shared with the last child end
            if (!child.getStartId().equals(lastChild.getEndId())
                // and it's not null
                && child.getStart().getOffset() != null
                // and it's over the threshold
                && child.getStart().getOffset() > threshold)
            { // set the lastChild.end to be the same as child.start
              log("sharing end of last child ", lastChild, " with this child ", child);
              changes.addAll( // record changes for for setting the end anchor:
                lastChild.setEndId(child.getStartId()));
            }
            else
            { 
              // null lastChild's end
              log("new end for last child ", child);
              Anchor newAnchor = new Anchor();
              newAnchor.setConfidence(Constants.CONFIDENCE_NONE);
              parent.getGraph().addAnchor(newAnchor);
              changes.addAll( // record changes for new anchor:
                newAnchor.getChanges());
              changes.addAll( // and for setting the end anchor:
                lastChild.setEndId(newAnchor.getId()));
            } // null lastChild's end 

            // now that the child has been processed, do its children if appropriate
            if (!lastChild.getAnchored() || lastChild.includesOffset(threshold))
            {
              resetChildAnchorsBefore(lastChild, threshold);
            }
          } // lastChild's end is out of range

          // now deal with start of this child, which must be the same as the last child
          if (!child.getStartId().equals(lastChild.getEndId()))
          {
            log("set start of child ", child, " to be end of last child ", lastChild);
            changes.addAll( // record changes for setting the start anchor:
              child.setStartId(lastChild.getEndId()));
          }
        } // not the first child
        lastChild = child;
      } // next child

      // now check the end of the last child
      if (lastChild != null)
      { // there are children
        // the last one must share the end anchor with its parent
        if (!lastChild.getEndId().equals(parent.getEndId()))
        {
          log("set last child ", lastChild, " to share end with parent");
          changes.addAll( // record changes for setting the start anchor:
            lastChild.setStartId(parent.getEndId()));
        }
        // now that the child has been processed, do its children if appropriate
        if (!lastChild.getAnchored() || lastChild.includesOffset(threshold))
        {
          resetChildAnchorsBefore(lastChild, threshold);
        }
      }

    } // next child layer
    return changes;
  } // end of resetChildAnchorsBefore()

  /**
   * Resets the anchors of the children of the given
   * annotation. After this method, all children on a given layer
   * will be s-included (i.e. chained from the start anchor to the
   * end anchor), and all anchors that previously had an offset at or before the threshold 
   * will have the offset set to null and the confidence set to
   * @param parent The parent whose children should be changed.
   * @param threshold The offset theshold after which anchors will be reset.
   * @return The changes made during this operation.
   */
  protected Vector<Change> resetChildAnchorsAfter(Annotation parent, double threshold)
  {
    log("resetChildAnchorsAfter ", parent, " ", threshold);
    Vector<Change> changes = new Vector<Change>();
    for (String childLayerId : parent.getAnnotations().keySet())
    {
      // ignore non-interval layers
      Layer childLayer = parent.getGraph().getLayer(childLayerId);
      if (childLayer == null) continue;
      if (childLayer.getAlignment() != Constants.ALIGNMENT_INTERVAL) continue;
      // ignore relationships that aren't saturated
      if (!childLayer.getSaturated()) continue;
      // ignore relationships that are not t-included
      if (!childLayer.getParentIncludes()) continue;

      Annotation lastChild = null;
      for (Annotation child : parent.getAnnotations(childLayerId))
      {
        // TODO do we need to check it's still really a child?
        if (lastChild == null)
        {
          if (!child.getStartId().equals(parent.getStartId()))
          {	       
            log("set first child ", child, " to share start with parent");
            // first child - start anchor is shared with parent
            changes.addAll( // record changes for:
              child.setStartId(parent.getStartId()));
          }
        }
        else
        { // not the first child
          // first deal with end of last child
          if (lastChild.getEnd().getOffset() != null
              && lastChild.getEnd().getOffset() >= threshold)
          {
            // check child.start.offset first to see if nulling can be avoided
            // if the current child start is not shared with the last child end
            if (!child.getStartId().equals(lastChild.getEndId())
                // and it's not null
                && child.getStart().getOffset() != null
                // and it's over the threshold
                && child.getStart().getOffset() < threshold)
            { // set the lastChild.end to be the same as child.start
              log("sharing end of last child ", lastChild, " with this child ", child);
              changes.addAll( // record changes for for setting the end anchor:
                lastChild.setEndId(child.getStartId()));
            }
            else
            { 
              // null lastChild's end
              log("new end for last child ", child);
              Anchor newAnchor = new Anchor();
              newAnchor.setConfidence(Constants.CONFIDENCE_NONE);
              parent.getGraph().addAnchor(newAnchor);
              changes.addAll( // record changes for new anchor:
                newAnchor.getChanges());
              changes.addAll( // and for setting the end anchor:
                lastChild.setEndId(newAnchor.getId()));
            } // null lastChild's end 

            // now that the child has been processed, do its children if appropriate
            if (!lastChild.getAnchored() || lastChild.includesOffset(threshold))
            {
              resetChildAnchorsBefore(lastChild, threshold);
            }
          } // lastChild's end is out of range

          // now deal with start of this child, which must be the same as the last child
          if (!child.getStartId().equals(lastChild.getEndId()))
          {
            log("set start of child ", child, " to be end of last child ", lastChild);
            changes.addAll( // record changes for setting the start anchor:
              child.setStartId(lastChild.getEndId()));
          }
        } // not the first child
        lastChild = child;
      } // next child

      // now check the end of the last child
      if (lastChild != null)
      { // there are children
        // the last one must share the end anchor with its parent
        if (!lastChild.getEndId().equals(parent.getEndId()))
        {
          log("set last child ", lastChild, " to share end with parent");
          changes.addAll( // record changes for setting the start anchor:
            lastChild.setStartId(parent.getEndId()));
        }
        // now that the child has been processed, do its children if appropriate
        if (!lastChild.getAnchored() || lastChild.includesOffset(threshold))
        {
          resetChildAnchorsAfter(lastChild, threshold);
        }
      }

    } // next child layer
    return changes;
  } // end of resetChildAnchorsAfter()

  /**
   * Compare two anchors, evaluating them as equal if the difference is less than {@link #offsetComparisonThreshold}.
   * @param a1 The first anchor.
   * @param a2 The second anchor.
   * @return 0 if the anchore offsets are the same, or the difference is less than {@link #offsetComparisonThreshold}, 999 if a1.offset is null, -999 if a2.offset is null, or the value of Double.compareTo(Double) otherwise.
   */
  protected int compare(Anchor a1, Anchor a2)
  {
    Double d1 = a1.getOffset();
    Double d2 = a2.getOffset();
    if (d1 == null) return 999;
    if (d2 == null) return -999;
    return editedGraph.compareOffsets(d1,d2);
    // // if there's a threshold
    // if (offsetComparisonThreshold != null)
    // {
    //   if (Math.abs(d1 - d2) <= offsetComparisonThreshold) return 0;
    // }
    // // if we got this far, use straight Double comparison
    // return d1.compareTo(d2);
  }

  /**
   * Gets the confidence rating of a given anchor. If the anchor offset is not set, {@link Constants#CONFIDENCE_NONE} is returned. If no Integer confidence attribute is present, {@link Constants#CONFIDENCE_MANUAL} is returned.
   * @param o The anchor to get the confidence rating for.
   * @return The confidence rating of a given object, or {@link Constants#CONFIDENCE_MANUAL} if it could not be determined.
   */
  protected int getConfidence(Anchor o)
  {
    if (o.getOffset() == null) return Constants.CONFIDENCE_NONE;
    return Utility.getConfidence(o);
  } // end of getConfidence()

  /**
   * Gets the confidence rating of a given object.  If no Integer confidence attribute is present, {@link Constants#CONFIDENCE_MANUAL} is returned.
   * @param o The annotation to get the confidence rating for.
   * @return The confidence rating of a given object, or {@link Constants#CONFIDENCE_MANUAL} if it could not be determined.
   */
  protected int getConfidence(Annotation o)
  {      
    return Utility.getConfidence(o);
  } // end of getConfidence()
   
  /**
   * Sets the confidence of a given object.
   * @param o The object to set the confidence rating for (most likely an {@link Annotation} or {@link Anchor})
   * @param confidence The confidence rating of a given object.
   */
  protected void setConfidence(TrackedMap o, int confidence)
  {
    o.setConfidence(confidence);
  } // end of setConfidence()

   
  /**
   * Determines whether the given annotation has a mapped counterpart in the other graph.
   * @param annotation The annotation to test.
   * @return true if the annotation has an "@other" attribute, false otherwise.
   */
  protected boolean hasCounterpart(Annotation annotation)
  {
    return annotation.containsKey("@other");
  } // end of hasCounterpart()

  /**
   * Gets the given annotation's mapped counterpart in the other graph.
   * @param annotation The annotation to get the counterpart of.
   * @return The annotation in the other graph that has been mapped to the given annotation, or null if no mapping has been made.
   */
  protected Annotation getCounterpart(Annotation annotation)
  {
    return (Annotation)annotation.get("@other");
  } // end of getCounterpart()

  /**
   * Maps the given annotations to each other.
   * @param a1 An annotation.
   * @param a2 The mapped conterpart of <var>a1</var>.
   */
  protected void setCounterparts(Annotation a1, Annotation a2)
  {
    a1.put("@other", a2);
    a2.put("@other", a1);
  } // end of setCounterparts()

  /**
   * Removes mapping between the given annotation and its counterpart.
   * @param annotation One of the counterpart annotations ({@link #getCounterpart(Annotation)} will be called to determine the other.).
   */
  protected void unsetCounterparts(Annotation annotation)
  {
    Annotation other = getCounterpart(annotation);
    if (other != null) other.remove("@other");
    annotation.remove("@other");
  } // end of setCounterparts()

  /**
   * Logs a debugging message.
   * @param messages The objects making up the log message.
   */
  protected void log(Object ... messages)
  {
    if (debug)
    { // we only interpret arguments to log() if we're actually debugging...
      StringBuilder s = new StringBuilder();
      for (Object m : messages)
      {	
        if (m == null)
        {
          s.append("[null]");
        }
        else if (m instanceof Annotation)
        {
          Annotation annotation = (Annotation)m;
          s.append("[").append(annotation.getId()).append("]")
            .append(annotation.getOrdinal()).append("#")
            .append(annotation.getLabel())
            .append("(").append(annotation.getStart())
            .append("-").append(annotation.getEnd()).append(")");
        }
        else if (m instanceof Anchor)
        {
          Anchor anchor = (Anchor)m;
          s.append("[").append(anchor.getId()).append("]").append(anchor.getOffset());
        }
        else
        {
          s.append(m.toString());
        }
      }	 
      log.add(s.toString());
      System.out.println(s.toString());
    }
  } // end of log()

} // end of class Merger
