//
// Copyright 2015-2025 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nzilbb.ag.util.AnchorComparatorWithStructure;
import nzilbb.ag.util.AnnotationComparatorByOrdinal;
import nzilbb.ag.util.AnnotationsByAnchor;
import nzilbb.ag.util.LayerHierarchyTraversal;
import nzilbb.ag.util.LayerTraversal;
import nzilbb.util.ClonedProperty;
import nzilbb.util.IO;
import nzilbb.util.Timers;

/**
 * Linguistic annotation graph.
 * <p>An annotation graph is a collection of {@link Annotation}s (edges) joined by {@link
 * Anchor}s (nodes) which may or may not have temporal/character offsets. Superimposed
 * over this temporally anchored graph is another heirarchical graph, where annotations
 * are nodes and edges are child-to-parent links. 
 * <figure>
 *   <img src="doc-files/annotation-graph-example.svg">
 *   <figcaption>An example of a heirarchical annotation graph</figcaption>
 * </figure>
 * <p>In addition to containing the nodes/edges, this class inherits from {@link
 * Annotation} so that it can: 
 * <ul>
 *   <li>be the root node of the annotation hierarchy - i.e. be the parent of annotations
 * at the top of the layer hierarchy</li> 
 *   <li>have start/end anchors</li>
 * </ul>
 * <p>In addition to this, the graph also has:
 * <ul>
 *  <li>a corpus attribute representing the collection to which it belongs (see 
 *   {@link #getCorpus()}, {@link #setCorpus(String)}),</li> 
 *  <li>definitions of a {@link Schema} defining annotation {@link Layer}s and their hierarchy</li>
 * </ul>
 * <p>It is recommended that other graph attributes are represented as annotations that
 * 'tag' the whole graph, and that speakers/participants are also represented as such
 * annotations, on a "participant" layer, which is the parent of a "turn" layer which
 * defines speaker turns. 
 * <p>This class can also represent graph fragments (sub-graphs).  If this is a whole
 * graph, {@link Graph#getGraph()} == <var>this</var>, but if it's a fragment, then 
 * {@link Graph#getGraph()} != <var>this</var>. The {@link #isFragment()} convenience method
 * captures this principle. The annotations in a graph fragment have the fragment object
 * (not the whole-graph object) set as their {@link Graph#getGraph()}. 
 * @author Robert Fromont robert.fromont@canterbury.ac.nz
 */
@SuppressWarnings("serial")
public class Graph extends Annotation {
  /**
   * New ID seed.
   */
  protected long lastId = 0;   
  /**
   * Generates a unique ID.
   * @return A new ID that is unique within this graph.
   */
  protected String newId() {
    String newId = null;
    do { newId = Long.toString(++lastId, Character.MAX_RADIX); }
    while(getAnnotationsById().containsKey(newId) || getAnchors().containsKey(newId));
    return newId;
  } // end of newId()

  /**
   * The ID of the end anchor of the last annotation added using
   * {@link #addAnnotation(Annotation)}, or the last anchor added using
   * {@link #addAnchor(Anchor)}.
   */
  protected String lastAddedAnchorId;
  /**
   * Getter for lastAddedAnchorId: The ID of the end anchor of the last annotation added
   * using {@link #addAnnotation(Annotation)}, or the last anchor added using 
   * {@link #addAnchor(Anchor)}. 
   * @return The ID of the end anchor of the last annotation added using
   * {@link #addAnnotation(Annotation)}, or the last anchor added using
   * {@link #addAnchor(Anchor)}.
   */
  public String getLastAddedAnchorId() { return lastAddedAnchorId; }

  // keep a list of annotations whose anchors haven't been added yet
  private HashMap<String,Vector<Annotation>> unknownStartAnchor
  = new HashMap<String,Vector<Annotation>>();
  private HashMap<String,Vector<Annotation>> unknownEndAnchor
  = new HashMap<String,Vector<Annotation>>();
  // keep a list of orphaned annotations, keyed on layerId
  HashMap<String,LinkedHashSet<Annotation>> orphans
  = new HashMap<String,LinkedHashSet<Annotation>>();

  /**
   * Getter for corpus: The name of the corpus the graph belongs to.
   * @return The name of the corpus the graph belongs to.
   */
  @Deprecated
  public String getCorpus() { try { return (String)get("corpus"); } catch(ClassCastException exception) {return null;} }
  /**
   * Setter for corpus.
   * @param corpus The name of the corpus the graph belongs to.
   * @return <var>this</var>.
   */
  @Deprecated
  public Graph setCorpus(String corpus) { put("corpus", corpus); return this; }

  /**
   * Getter for <i>anchors</i>: Map of anchors (graph nodes) keyed by id. 
   * <p>In the underlying map, this is stored as the "children" attribute of a "layers" attribute.
   * @return Map of anchors (graph nodes) keyed by id.
   */
  @ClonedProperty @SuppressWarnings("unchecked")
  public LinkedHashMap<String,Anchor> getAnchors() { 
    if (!containsKey("anchors")) {
      setAnchors(new LinkedHashMap<String,Anchor>());
    }
    try { 
      return (LinkedHashMap<String,Anchor>)get("anchors"); 
    } catch(ClassCastException exception) {
      return null;
    } 
  }
  /**
   * Setter for <i>anchors</i>. 
   * <p>In the underlying map, this is stored as the "children" attribute of a "layers" attribute.
   * @return <var>this</var>.
   * @param anchors Map of anchors (graph nodes) keyed by id.
   */
  public Graph setAnchors(LinkedHashMap<String,Anchor> anchors) { put("anchors", anchors); return this; }
   
  /**
   * Granularity of offsets - e.g. 0.001 if Anchor offsets are always set to the the
   * nearest millisecond, or null for no particular granularity. 
   * @see #getOffsetGranularity()
   * @see #setOffsetGranularity(Double)
   */
  protected Double offsetGranularity;
  /**
   * Getter for {@link #getOffsetGranularity()}: Granularity of offsets - e.g. 0.001 if Anchor
   * offsets are always set to the the nearest millisecond, or null for no particular
   * granularity. 
   * @return Granularity of offsets - e.g. 0.001 if Anchor offsets are always set to the
   * the nearest millisecond, or null for no particular granularity. 
   */
  public Double getOffsetGranularity() { return offsetGranularity; }
  /**
   * Setter for {@link #getOffsetGranularity()}.g. 0.001 if Anchor offsets are always set to
   * the the nearest millisecond, or null for no particular granularity. 
   * @param newOffsetGranularity Granularity of offsets - e.g. 0.001 if Anchor offsets
   * are always set to the the nearest millisecond, or null for no particular
   * granularity. 
   * @return <var>this</var>.
   */
  public Graph setOffsetGranularity(Double newOffsetGranularity) {
    offsetGranularity = newOffsetGranularity;
    // need to re-index anchors by offset
    offsetIndex = new HashMap<Double,LinkedHashSet<Anchor>>();
    for (Anchor a : getAnchors().values()) {
      indexAnchor(a);
    } // next anchor
    
    return this;
  }

  /**
   * The units for anchor offsets - e.g. "s" for seconds, "char" for characters, etc.
   * Preferably the value should be one of the Constants.UNIT_... constants. The default
   * value is {@link Constants#UNIT_SECONDS}. 
   * @see #getOffsetUnits()
   * @see #setOffsetUnits(String)
   * @see Constants#UNIT_SECONDS
   * @see Constants#UNIT_CHARACTERS
   */
  protected String offsetUnits = Constants.UNIT_SECONDS;
  /**
   * Getter for {@link #offsetUnits}: The units for anchor offsets - e.g. "s" for
   * seconds, "char" for characters, etc.  Preferably the value should be one of the
   * Constants.UNIT_... constants. The default value is {@link Constants#UNIT_SECONDS}. 
   * @return The units for anchor offsets - e.g. "s" for seconds, "char" for characters,
   * etc.  Preferably the value should be one of the Constants.UNIT_... constants. 
   */
  public String getOffsetUnits() { return offsetUnits; }
  /**
   * Setter for {@link #offsetUnits}.g. "s" for seconds, "char" for characters, etc.
   * Preferably the value should be one of the Constants.UNIT_... constants. 
   * @param newOffsetUnits The units for anchor offsets - e.g. "s" for seconds, "char"
   * for characters, etc.  Preferably the value should be one of the
   * Constants.UNIT_... constants. 
   * @return <var>this</var>.
   */
  public Graph setOffsetUnits(String newOffsetUnits) { offsetUnits = newOffsetUnits; return this; }

  // Attributes stored outside HashMap, so that JSONifying the HashMap doesn't result in infinite recursion

  /**
   * Map of annotations (graph edges) keyed by id.
   * @see #getAnnotationsById()
   * @see #setAnnotationsById(LinkedHashMap)
   */
  protected LinkedHashMap<String,Annotation> annotationsById = new LinkedHashMap<String,Annotation>();
  /**
   * Getter for {@link #annotationsById}: Map of annotations (graph edges) keyed by id.
   * @return Map of annotations (graph edges) keyed by id.
   */
  public LinkedHashMap<String,Annotation> getAnnotationsById() { return annotationsById; }
  /**
   * Setter for {@link #annotationsById}.
   * @param newAnnotationsById Map of annotations (graph edges) keyed by id.
   * @return <var>this</var>.
   */
  public Graph setAnnotationsById(LinkedHashMap<String,Annotation> newAnnotationsById) { annotationsById = newAnnotationsById; return this; }
   
  /**
   * The layer definitions and their interrelations.
   * @see #getSchema()
   * @see #setSchema(Schema)
   */
  protected Schema schema = new Schema();
  /**
   * Getter for {@link #schema}: The layer definitions and their interrelations.
   * @return The layer definitions and their interrelations.
   */
  @ClonedProperty
  public Schema getSchema() { return schema; }
  /**
   * Setter for {@link #schema}.
   * @param newSchema The layer definitions and their interrelations.
   * @return <var>this</var>.
   */
  public Graph setSchema(Schema newSchema) { schema = newSchema; return this; }

  /**
   * Timers for debugging and optimization.
   * @see #getTimers()
   * @see #setTimers(Timers)
   */
  protected Timers timers;
  /**
   * Getter for {@link #timers}: Timers for debugging and optimization.
   * @return Timers for debugging and optimization.
   */
  public Timers getTimers() { return timers; }
  /**
   * Setter for {@link #timers}.
   * @param newTimers Timers for debugging and optimization.
   * @return <var>this</var>.
   */
  public Graph setTimers(Timers newTimers) { timers = newTimers; return this; }

  /**
   * An optional provider for media associated with the graph.
   * @see #getMediaProvider()
   * @see #setMediaProvider(GraphMediaProvider)
   */
  protected GraphMediaProvider mediaProvider;
  /**
   * Getter for {@link #mediaProvider}.
   * @return An optional provider for media associated with the graph.
   */
  public GraphMediaProvider getMediaProvider() { return mediaProvider; }
  /**
   * Setter for {@link #mediaProvider}.
   * @param mediaProvider An optional provider for media associated with the graph.
   * @return <var>this</var>.
   */
  public Graph setMediaProvider(GraphMediaProvider mediaProvider) { this.mediaProvider = mediaProvider; return this; }
   
  // Methods:
      
  /**
   * Default constructor.
   */
  public Graph() {
    graph = this;
    setLayerId(getSchema().getRoot().getId());
  } // end of constructor
   
  /**
   * Determines whether this is a fragment of a larger graph (true) or the whole graph (false).
   * @return false if {@link Graph#getGraph()} == <var>this</var>, true otherwise.
   */
  public boolean isFragment() {
    return !(graph == this);
  } // end of isFragment()

  /** Static format so we don't keep creating and destroying one */
  private static DecimalFormat offsetFormat = new DecimalFormat(
    // force the locale to something with . as the decimal separator
    "0.000", new DecimalFormatSymbols(Locale.UK));
   
  /**
   * Computes a graph fragment ID, given a graph ID and bounding offsets.
   * @param graphId
   * @param startOffset
   * @param endOffset
   * @return The ID of the fragment, formatted
   * <var>graphId-without-extension</var>__<var>startOffset</var>-<var>endOffset</var> 
   */
  public static String FragmentId(String graphId, Double startOffset, Double endOffset) {
    try {
      return IO.WithoutExtension(graphId)
        + "__" + offsetFormat.format(startOffset) 
        + "-" + offsetFormat.format(endOffset);
    } catch (Throwable t) {
      return IO.WithoutExtension(graphId)
        + "__" + startOffset
        + "-" + endOffset;
    }
  } // end of FragmentId()
  
  /**
   * Computes a graph fragment ID, given a graph and bounding offsets.
   * @param graph
   * @param startOffset
   * @param endOffset
   * @return The ID of the fragment, formatted
   * <var>graphId-without-extension</var>__<var>startOffset</var>-<var>endOffset</var> 
   */
  public static String FragmentId(Graph graph, Double startOffset, Double endOffset) {
    try {
      return IO.WithoutExtension(graph.getId())
        + "__" + offsetFormat.format(startOffset) 
        + "-" + offsetFormat.format(endOffset);
    } catch (Throwable t) {
      return IO.WithoutExtension(graph.getId())
        + "__" + startOffset
        + "-" + endOffset;
    }
  } // end of FragmentId()
   
  /**
   * Computes a graph fragment ID, given a graph and bounding anchors.
   * @param graph
   * @param start
   * @param end
   * @return The ID of the fragment, formatted
   * <var>graphId</var>__<var>startOffset</var>-<var>endOffset</var> 
   */
  public static String FragmentId(Graph graph, Anchor start, Anchor end) {
    return FragmentId(graph, start.getOffset(), end.getOffset());
  } // end of FragmentId()
      
  /**
   * Deduces the graph name, start time, and end time from the given fragment ID.
   * @param fragmentId The fragment, formatted
   * <var>graphId</var>__<var>startOffset</var>-<var>endOffset</var> 
   * @return An array with three elements (or null if the fragmentId is not correctly formatted):
   *  <ul>
   *   <li> 0 : the ID of the original graph </li>
   *   <li> 1 : original start offset of the fragment </li>
   *   <li> 2 : original end offset of the fragment </li>
   *  </ul>
   */
  public static String[] ParseFragmentId(String fragmentId) {
    Pattern idPattern = Pattern.compile("^(.*)__([0-9.]+)-([0-9.]+)(\\.[a-zA-Z]*)?$");
    Matcher idMatcher = idPattern.matcher(fragmentId);
    if (idMatcher.matches()) {
      String[] parts = {
        idMatcher.group(1),
        idMatcher.group(2),
        idMatcher.group(3)
      };
      return parts;
    } else {
      return null;
    }
  } // end of FragmentId()
      
  /**
   * Creates a fragment of this graph, copying into it annotations that fall within the
   * given bounds, on the given layers.  
   * Ancestors which do not fall into the interval are also added to the fragment, so
   * that turns/speakers/etc. are accessible, but their anchors are not added to the
   * fragment. 
   * <p>The ID of the new fragment is
   * <var>graphId</var>__<var>startOffset</var>-<var>endOffset</var> 
   * @param startOffset The start offset for annotations to include in the fragment.
   * @param endOffset The end offset for annotations to include in the fragment.
   * @param layerIds A list of IDs of layers to include in the fragment.
   * @return A graph fragment containing the annotations that fall within the given
   * bounds, on the given layers. 
   */
  public Graph getFragment(double startOffset, double endOffset, String[] layerIds) {
    Graph fragment = new Graph();
    if (mediaProvider != null) {
      fragment.setMediaProvider(mediaProvider.providerForGraph(fragment));
    }
    fragment.graph = this;
    fragment.getSchema().copyLayerIdsFrom(graph.getSchema());
    if (getId() != null) {
      fragment.setId(FragmentId(this, startOffset, endOffset));
    }
    HashSet<String> layerSet = new HashSet<String>();
    for (String l : layerIds) layerSet.add(l);
    for (Layer layer : getLayersTopDown()) { // ensure parents are added before children	 
      if (layerSet.contains(layer.getId())) {
        if (layer.getId().equals(getSchema().getRoot().getId())) continue;
        fragment.addLayer((Layer)layer.clone());
        
        // is it a transcript attribute layer?
        if ((layer.getParentId() == null
             || layer.getParentId().equals(getSchema().getRoot().getId()))
            && layer.getAlignment() == Constants.ALIGNMENT_NONE
            && !layer.getId().equals(getSchema().getParticipantLayerId())) {
          for (Annotation annotation : all(layer.getId())) {
            Annotation attribute = (Annotation)annotation.clone();
            attribute.setParentId(fragment.getId());
            fragment.addAnnotation(attribute);
          } // next attribute
          
        } else { // not a transcript attribute layer
          
          for (Annotation annotation : all(layer.getId())) {
            Double min = annotation.getStart().getOffsetMin();
            Double max = annotation.getEnd().getOffsetMax();
            if (min != null && min >= startOffset
                && max != null && max <= endOffset) {
              ensureAllAncestorsPresentInFragment(annotation, fragment);
              
              // add the anchors
              fragment.addAnchor((Anchor)annotation.getStart().clone());
              fragment.addAnchor((Anchor)annotation.getEnd().clone());
              
              // add the annotation
              fragment.addAnnotation((Annotation)annotation.clone());
            }
          } // next annotation
          
        } // not a transcript attribute layer
      } // layer exists
    } // next layer

    // ensure that there's an anchor at the end offset
    // (so that the last anchor matches the name, and serializers can pad if necessary)
    fragment.getOrCreateAnchorAt(endOffset);

    return fragment;
  } // end of getFragment()

  /**
   * Creates a fragment of this graph, copying into it the given annotation and its descendants,
   * on the given layers. Ancestors which do not fall into the interval are also added to the
   * fragment, so that turns/speakers/etc. are accessible, but their anchors are not added to the
   * fragment. If the defining annotation's layer is not included in <var>layerIds</var>, the
   * annotation is not included in the resulting fragment.
   * <p>The ID of the new fragment is
   * <var>graphId</var>__<var>fragment.start.offset</var>-<var>fragment.end.offset</var> 
   * @param definingAnnotation The annotation that defines fragment membership.
   * @param layerIds A list of IDs of layers to include in the fragment.
   * @return A graph fragment containing the annotations that fall within the given bounds, on
   * the given layers. 
   */
  public Graph getFragment(Annotation definingAnnotation, String[] layerIds) {
    final LinkedHashSet<String> layerIdSet = new LinkedHashSet<String>();
    for (String id : layerIds) layerIdSet.add(id);
    Graph fragment = new Graph();
    if (mediaProvider != null) {
      fragment.setMediaProvider(mediaProvider.providerForGraph(fragment));
    }
    fragment.graph = this;
    fragment.getSchema().copyLayerIdsFrom(graph.getSchema());
    Layer layer = getLayer(definingAnnotation.getLayerId());
    if (layer != null) {
      fragment.addAnchor((Anchor)definingAnnotation.getStart().clone());
      fragment.addAnchor((Anchor)definingAnnotation.getEnd().clone());
	 
      ensureAllAncestorsPresentInFragment(definingAnnotation, fragment);
      if (layerIdSet.contains(definingAnnotation.getLayerId())) {
        // add the layer
        fragment.addLayer((Layer)layer.clone());
        // and the annotation
        fragment.addAnnotation((Annotation)definingAnnotation.clone());
      }
    }
    Anchor firstAnchor = definingAnnotation.getStart();
    Anchor lastAnchor = definingAnnotation.getEnd();
    
    // add other layers, top-down
    LayerHierarchyTraversal<Vector<String>> topDownLayers
      = new LayerHierarchyTraversal<Vector<String>>(new Vector<String>(), getSchema()) {
          protected void pre(Layer layer) {
            if (layerIdSet.contains(layer.getId())) {
              result.add(layer.getId());
            }
          }
        };
    
    for (String layerId : topDownLayers.getResult()) {
      if (layerId.equals(getSchema().getRoot().getId())) continue;
      layer = getLayer(layerId);
      if (layer != null) {
        fragment.addLayer((Layer)layer.clone());
        // is it a transcript attribute layer?
        if ((layer.getParentId() == null
             || layer.getParentId().equals(getSchema().getRoot().getId()))
            && layer.getAlignment() == Constants.ALIGNMENT_NONE
            && !layer.getId().equals(getSchema().getParticipantLayerId())) {
          for (Annotation annotation : all(layer.getId())) {
            Annotation attribute = (Annotation)annotation.clone();
            attribute.setParentId(fragment.getId());
            fragment.addAnnotation(attribute);
          } // next attribute
          
        } else { // not a transcript attribute layer
          
          // TODO this could be more efficient by traversing definingAnnotation first
          // TODO and then using definingAnnotation.all(layerId) for any remaining layers
          for (Annotation annotation : definingAnnotation.all(layerId)) {
            ensureAllAncestorsPresentInFragment(annotation, fragment);
            // add the anchors that fit
            if (fragment.getAnchor(annotation.getStart().getId()) == null
                && (annotation.getStart().getOffset() == null
                    || definingAnnotation.includesOffset(annotation.getStart().getOffset()))) {
              fragment.addAnchor((Anchor)annotation.getStart().clone());
            }
            if (fragment.getAnchor(annotation.getEnd().getId()) == null
                && (annotation.getEnd().getOffset() == null
                    || definingAnnotation.includesOffset(annotation.getEnd().getOffset())
                    // 'includesOffset' == false if annotation.end.offset == end.offset, so:
                    || annotation.getEnd().getOffset().equals(lastAnchor.getOffset()))) {
              fragment.addAnchor((Anchor)annotation.getEnd().clone());
            }
            if (fragment.getAnnotation(annotation.getId()) == null) {
              // add the annotation
              fragment.addAnnotation((Annotation)annotation.clone());
            }
          } // next annotation
        } // not a transcript attribute layer
      } // layer exists
    } // next layer
      
    if (getId() != null) {
      fragment.setId(FragmentId(this, firstAnchor, lastAnchor));
    }
    return fragment;
  } // end of getFragment()

  /**
   * Creates a fragment of this graph, copying into it annotations that fall within the
   * bounds of the given <var>bounds</var> annotation,  
   * and which are descendants of the given <var>ancestor</var> annotation, 
   * on the given layers. 
   * Ancestors which do not fall into the interval are also added to the fragment, so
   * that turns/speakers/etc. are accessible, but their anchors are not added to the
   * fragment. 
   * <p>The ID of the new fragment is
   * <var>graphId</var>__<var>fragment.start.offset</var>-<var>fragment.end.offset</var> 
   * @param bounds Annotation that defines the offset bounds (start/end time) of the
   * included annotations 
   * @param ancestor Annotation that is an ancestor of all annotations to be included.
   * @param layerIds A list of IDs of layers to include in the fragment.
   * @return A graph fragment containing the annotations that fall within the given
   * bounds, on the given layers. 
   */
  public Graph getFragment(Annotation bounds, Annotation ancestor, String[] layerIds) {
    Graph fragment = new Graph();
    if (mediaProvider != null) {
      fragment.setMediaProvider(mediaProvider.providerForGraph(fragment));
    }
    fragment.graph = this;
    fragment.getSchema().setParticipantLayerId(graph.getSchema().getParticipantLayerId());
    fragment.getSchema().setTurnLayerId(graph.getSchema().getTurnLayerId());
    fragment.getSchema().setUtteranceLayerId(graph.getSchema().getUtteranceLayerId());
    fragment.getSchema().setWordLayerId(graph.getSchema().getWordLayerId());
    fragment.getSchema().setEpisodeLayerId(graph.getSchema().getEpisodeLayerId());
    fragment.getSchema().setCorpusLayerId(graph.getSchema().getCorpusLayerId());
    if (bounds.getAnchored()) {
      double startOffset = bounds.getStart().getOffset();
      double endOffset = bounds.getEnd().getOffset();
      for (String layerId : layerIds) {
        if (layerId.equals(getSchema().getRoot().getId())) continue;
        Layer layer = getLayer(layerId);
        if (layer != null) {
          fragment.addLayer((Layer)layer.clone());
        
          // is it a transcript attribute layer?
          if ((layer.getParentId() == null
               || layer.getParentId().equals(getSchema().getRoot().getId()))
              && layer.getAlignment() == Constants.ALIGNMENT_NONE
              && !layer.getId().equals(getSchema().getParticipantLayerId())) {
            for (Annotation annotation : all(layer.getId())) {
              Annotation attribute = (Annotation)annotation.clone();
              attribute.setParentId(fragment.getId());
              fragment.addAnnotation(attribute);
            } // next attribute

          } else { // not a transcript attribute
            
            for (Annotation annotation : all(layerId)) {
              Double min = annotation.getStart().getOffsetMin();
              Double max = annotation.getEnd().getOffsetMax();
              if (min != null && min >= startOffset
                  && max != null && max <= endOffset
                  && annotation.getAncestors().contains(ancestor)) {
                ensureAllAncestorsPresentInFragment(annotation, fragment);
                // add the anchors
                fragment.addAnchor((Anchor)annotation.getStart().clone());
                fragment.addAnchor((Anchor)annotation.getEnd().clone());
                // add the annotation
                fragment.addAnnotation((Annotation)annotation.clone());
              }
            } // next annotation
          } // not a transcript attribute
        } // layer exists
      } // next layer
	 
      Anchor firstAnchor = bounds.getStart();
      Anchor lastAnchor = bounds.getEnd();
      if (getId() != null) {
        fragment.setId(FragmentId(this, firstAnchor, lastAnchor));
      }
    }
    return fragment;
  } // end of getFragment()

  /**
   * Ensures all the given annotation's ancestors are in the given fragment, and that they're
   * ordinal minima are correct. 
   * @param annotation
   * @param fragment
   */
  protected void ensureAllAncestorsPresentInFragment(Annotation annotation, Graph fragment) {
    // have ancestors been added? Top-down check
    Iterator<Annotation> ancestors 
      = new LinkedList<Annotation>(annotation.getAncestors()).descendingIterator();
    while(ancestors.hasNext()) {
      Annotation ancestor = ancestors.next();
      if (!ancestor.getLayerId().equals(schema.getRoot().getId())) { // not the graph
        if (fragment.getAnnotation(ancestor.getId()) == null) { // add the parent
          if (!fragment.schema.getLayers().containsKey(ancestor.getLayerId())) {
            // add the ancestor's layer
            fragment.addLayer((Layer)getLayer(ancestor.getLayerId()).clone());
          }
          // ensure it's ordinal ends up correct
          ensureMinimumOrdinalSpecified(ancestor, fragment);
          // add the ancestor (but not its anchors)
          fragment.addAnnotation((Annotation)ancestor.clone());
        }
      }
    } // next ancestor
    ensureMinimumOrdinalSpecified(annotation, fragment);
  } // end of ensureAllAncestorsPresentInFragment()
   
  /**
   * Ensures that the minimum ordinal for the parent is set for this annotation.
   * @param graphAnnotation
   * @param fragment
   */
  protected void ensureMinimumOrdinalSpecified(Annotation graphAnnotation, Graph fragment) {
    // ensure it's ordinal ends up correct
    Annotation ancestorParent = fragment.getAnnotation(graphAnnotation.getParentId());
    if (ancestorParent == null 
        && graphAnnotation.getLayer().getParentId().equals(schema.getRoot().getId())) {
      ancestorParent = fragment;
    }
    if (!ancestorParent.ordinalMinima.containsKey(graphAnnotation.getLayerId())) {
      ancestorParent.ordinalMinima.put(
        graphAnnotation.getLayerId(), graphAnnotation.getOrdinal());
    }
  } // end of ensureMinimumOrdinalSpecified()

  /**
   * Retrieves an annotation given an id.
   * @param id ID of the annotation.
   * @return The identified annotation, if it's in the graph, and null otherwise.
   */
  public Annotation getAnnotation(String id) {
    Annotation annotation = getAnnotationsById().get(id);
    if (annotation == null && getId() != null && getId().equals(id)) {
      // special case - a top-level annotation is looking for its parent, which is the
      // graph itself 
      annotation = this;
    }
    return annotation;
  } // end of getAnnotation()

  /**
   * Adds an annotation to the graph.
   * <p>If either of the anchor IDs is null, and the layer is aligned, then default anchors are
   * created for the annotation. This allows
   * <code>addAnnotation(new Annotation(null, label, layerId))</code>
   * to be repeatedly invoked, allowing anchor offsets to be set later.
   * @param annotation The annotation to add to the graph.
   * @return The annotation.
   */
  public Annotation addAnnotation(Annotation annotation) {

    if (annotation.getId() == null) {
      if (timers != null) timers.start("Graph.addAnnotation: create ID");
      annotation.setId(newId());
      annotation.create();
	 
      // does the annotation have children?
      if (annotation.getAnnotations().size() > 0) {
        // set the parentId for all children, now their parent has an ID
        for (SortedSet<Annotation> children : annotation.getAnnotations().values()) {
          for (Annotation child : children) {
            child.setParentId(annotation.getId());
          }
        } // next child layer
      }
      if (timers != null) timers.end("Graph.addAnnotation: create ID");
    }
    // set graph after the id is definitely set
    if (timers != null) timers.start("Graph.addAnnotation: setGraph");
    annotation.setGraph(this);
    if (timers != null) timers.end("Graph.addAnnotation: setGraph");

    // add to annotations collection
    getAnnotationsById().put(annotation.getId(), annotation);

    if (annotation.getParent() == null
        && annotation.getLayerId().equals(getSchema().getRoot().getId())) {
      annotation.setParentId(getId());
    }

    // add to the parent's collection
    if (annotation.getParent() != null) { // this ensures it's in the parent's child collection
      if (timers != null) timers.start("Graph.addAnnotation: add to parent");
      annotation.setParent(annotation.getParent(), false);
      if (annotation.getParent() == this
          && annotation.getLayer() != null // we know what the alignment should be
          && annotation.getLayer().getAlignment() == Constants.ALIGNMENT_NONE) {
        // graph tags are given anchor IDs here
        SortedSet<Anchor> anchors = getSortedAnchors();
        if (anchors.size() > 0) {
          annotation.setStartId(anchors.first().getId());
          annotation.setEndId(anchors.last().getId());
        }
      }
      if (timers != null) timers.end("Graph.addAnnotation: add to parent");
    } else { // keep track of orphans
      if (timers != null) timers.start("Graph.addAnnotation: track orphans");
      if (!orphans.containsKey(annotation.getLayerId())) {
        orphans.put(annotation.getLayerId(), new LinkedHashSet<Annotation>());
      }
      orphans.get(annotation.getLayerId()).add(annotation);
      if (timers != null) timers.end("Graph.addAnnotation: track orphans");
    }
    // find any children that might have already been added
    if (annotation.getLayer() != null) {
      if (timers != null) timers.start("Graph.addAnnotation: find children");
      Layer layer = annotation.getLayer();
      for (Layer childLayer : layer.getChildren().values()) {
        if (orphans.containsKey(childLayer.getId())) {
          Iterator<Annotation> orphansOnLayer = orphans.get(childLayer.getId()).iterator();
          while (orphansOnLayer.hasNext()) {
            Annotation orphan = orphansOnLayer.next();
            if (orphan.getParentId() != null
                && orphan.getParentId().equals(annotation.getId())) {
              orphan.setParent(annotation, false);
              orphansOnLayer.remove();
            }
          } // next possible child annotation
          if (orphans.get(childLayer.getId()).size() == 0) orphans.remove(childLayer.getId());
        } // there are orphaned children on this layer
      } // next child layer
      if (timers != null) timers.end("Graph.addAnnotation: find children");

      // also set the parent if it's a child of "transcript"
      if (layer.getParentId() == null || layer.getParentId().equals("transcript")) {
        if (timers != null) timers.start("Graph.addAnnotation: setParent = graph");
        annotation.setParent(this, false);
        if (timers != null) timers.end("Graph.addAnnotation: setParent = graph");
      }
    } 

    // check anchors
    if (annotation.getLayer() == null // we don't know what the alignment should be
        || annotation.getLayer().getAlignment() != Constants.ALIGNMENT_NONE) { // or it's aligned
      // should have an anchor
      if (timers != null) timers.start("Graph.addAnnotation: check anchors");
      if (annotation.getStartId() == null) { // no anchor, so create one
        if (lastAddedAnchorId != null) {
          annotation.setStart(getAnchor(lastAddedAnchorId));
        } else { // there was no last end anchor, so create a new anchor
          annotation.setStart(addAnchor(new Anchor()));
        }
      } else if (annotation.getStart() == null) {
        // there's a startId, but it references an anchor we don't yet know about
        if (!unknownStartAnchor.containsKey(annotation.getStartId())) {
          unknownStartAnchor.put(annotation.getStartId(), new Vector<Annotation>());
        }
        unknownStartAnchor.get(annotation.getStartId()).add(annotation);
      }
      if (annotation.getEndId() == null) { // no anchor, so create one
        annotation.setEnd(addAnchor(new Anchor()));
      } else if (annotation.getEnd() == null) {
        // there's a endId, but it references an anchor we don't yet know about
        if (!unknownEndAnchor.containsKey(annotation.getEndId())) {
          unknownEndAnchor.put(annotation.getEndId(), new Vector<Annotation>());
        }
        unknownEndAnchor.get(annotation.getEndId()).add(annotation);
      }
      lastAddedAnchorId = annotation.getEndId();
      if (timers != null) timers.end("Graph.addAnnotation: check anchors");
    }
    return annotation;
  } // end of addAnnotation()

  /**
   * Adds an anchor to the graph.
   * @param anchor The anchor to add to the graph.
   * @return The anchor.
   */
  public Anchor addAnchor(Anchor anchor) {
    anchor.setGraph(this);
    if (anchor.getId() == null) {
      anchor.setId(newId());
      anchor.create();
    }

    // add to anchors collection
    getAnchors().put(anchor.getId(), anchor);

    // add to offset index
    indexAnchor(anchor);

    // look for annotations referencing that anchor
    String id = anchor.getId();
    if (unknownStartAnchor.containsKey(id)) {
      for (Annotation annotation : unknownStartAnchor.get(id)) {
        if (annotation.getStartId() != null && annotation.getStartId().equals(id)) {
          // it still references this anchor
          annotation.setStart(anchor);
        }
      } // next annotation
      unknownStartAnchor.remove(id);
    }
    if (unknownEndAnchor.containsKey(id)) {
      for (Annotation annotation : unknownEndAnchor.get(id)) {
        if (annotation.getEndId() != null && annotation.getEndId().equals(id)) {
          // it still references this anchor
          annotation.setEnd(anchor);
        }
      } // next annotation
      unknownEndAnchor.remove(id);
    }
    lastAddedAnchorId = anchor.getId();
    return anchor;
  } // end of addAnnotation()

  /**
   * Retrieves an anchor given an id.
   * @param id The ID of the anchor.
   * @return The identified anchor, if it's in the graph, and null otherwise.
   */
  public Anchor getAnchor(String id) {
    return getAnchors().get(id);
  } // end of getAnchor()

  protected HashMap<Double,LinkedHashSet<Anchor>> offsetIndex
  = new HashMap<Double,LinkedHashSet<Anchor>>();  
  /**
   * Ensure the given anchor is in the offset index.
   * @param anchor The anchor to index.
   */
  void indexAnchor(Anchor anchor) {
    if (anchor.getOffset() == null) return;
    Double q = quantumOffset(anchor.getOffset());
    if (!offsetIndex.containsKey(q)) offsetIndex.put(q, new LinkedHashSet<Anchor>());
    offsetIndex.get(q).add(anchor);
  } // end of indexAnchor()

  /**
   * Gets an anchor at the given offset.
   * @param offset The anchor offset.
   * @return An anchor that has the given offset, or null if there isn't one in the graph.
   * @see #getOrCreateAnchorAt(double)
   */
  public Anchor getAnchorAt(double offset) {
    Double q = quantumOffset(offset);
    if (offsetIndex.containsKey(q)) {
      for (Anchor anchor : offsetIndex.get(q)) {
        // check the offset is still this one
        if (q.equals(quantumOffset(anchor.getOffset()))) { 
          return anchor;
        }
      } // next anchor indexed at this offset
    }
    return null;
  } // end of getAnchorAt()

  /**
   * Gets an anchor at the given offset. If there isn't already one in the graph, one is created.
   * @param offset The anchor offset.
   * @return An anchor that has the given offset.
   * @see #getAnchorAt(double)
   * @see #createAnchorAt(double)
   */
  public Anchor getOrCreateAnchorAt(double offset) {
    Anchor anchor = getAnchorAt(offset);
    if (anchor == null) {
      anchor = new Anchor();
      anchor.setOffset(offset);
      addAnchor(anchor);
    }
    return anchor;
  } // end of getOrCreateAnchorAt()

  /**
   * Gets an anchor at the given offset. If there isn't already one in the graph, one is created.
   * <p> This convenience method allows, for example, the creation of an anchor with
   * a confidence value in one step:
   * <pre>
   * Anchor anchor = graph.getOrCreateAnchorAt(456.789, Constants.CONFIDENCE_AUTOMATIC);
   * </pre>
   * @param offset The anchor offset.
   * @param confidence Confidence rating.
   * @return An anchor that has the given offset.
   * @see #getAnchorAt(double)
   * @see #createAnchorAt(double,Integer)
   */
  public Anchor getOrCreateAnchorAt(double offset, Integer confidence) {
    Anchor anchor = getAnchorAt(offset);
    if (anchor == null) {
      anchor = new Anchor();
      anchor.setOffset(offset);
      anchor.setConfidence(confidence);
      addAnchor(anchor);
    }
    return anchor;
  } // end of getAnchorAt()

  /**
   * Creates an anchor at the given offset.
   * @param offset The anchor offset.
   * @return A new anchor that has the given offset.
   * @see #getAnchorAt(double)
   * @see #getOrCreateAnchorAt(double)
   */
  public Anchor createAnchorAt(double offset) {
    Anchor anchor = new Anchor();
    anchor.setOffset(offset);
    addAnchor(anchor);
    return anchor;
  } // end of createAnchorAt()

  /**
   * Creates an anchor at the given offset.
   * <p> This convenience method allows, for example, the creation of an anchor with
   * a confidence value in one step:
   * <pre>
   * Anchor anchor = graph.createAnchorAt(456.789, Constants.CONFIDENCE_AUTOMATIC);
   * </pre>
   * @param offset The anchor offset.
   * @param confidence Confidence rating.
   * @return A new anchor that has the given offset.
   * @see #getAnchorAt(double)
   * @see #getOrCreateAnchorAt(double,Integer)
   */
  public Anchor createAnchorAt(double offset, Integer confidence) {
    Anchor anchor = new Anchor();
    anchor.setOffset(offset);
    anchor.setConfidence(confidence);
    addAnchor(anchor);
    return anchor;
  } // end of getAnchorAt()

  /**
   * Returns the anchors sorted by offset. This includes only anchors for which the offset
   * is actually set. 
   * @return The anchors sorted by offset.
   */
  public SortedSet<Anchor> getSortedAnchors() {
    TreeSet<Anchor> sortedAnchors = new TreeSet<Anchor>();
    for (Anchor a : getAnchors().values()) {
      if (a.getOffset() != null) {
        sortedAnchors.add(a);
      }
    }
    return sortedAnchors;
  } // end of getSortedAnchors()
   
  /**
   * Returns all anchors, in the best order given their offset and annotation links. This
   * ordering requires much graph traversal to find the best comparison between anchors,
   * so is computationally expensive and should be used sparingly. 
   * @return An ordered set of all anchors in the graph.
   */
  public SortedSet<Anchor> getAnchorsOrderedByStructure() {
    TreeSet<Anchor> sortedAnchors = new TreeSet<Anchor>(new AnchorComparatorWithStructure());
    sortedAnchors.addAll(getAnchors().values());
    return sortedAnchors;
  } // end of getAnchorsOrderedByStructure()
   
  /**
   * Increments all (set) anchor offsets by the given amount.
   * @param offset
   */
  public void shiftAnchors(double offset) {
    if (offset != 0.0) {
      for (Anchor a : getAnchors().values()) {
        if (a.getOffset() != null) {
          a.setOffset(a.getOffset() + offset);
        }
      } // next anchor
    }
  } // end of shiftAnchors()
   
  /**
   * Compares two offsets, taking {@link #getOffsetGranularity()} into account.
   * @param o1 The first offset to compare.
   * @param o2 The second offset to compare.
   * @return 0 if the two offsets are within {@link #getOffsetGranularity()} of each other, or
   * a negative number if o1 &lt; o2, and otherwise a positive number. 
   */
  public int compareOffsets(double o1, double o2) {
    if (offsetGranularity != null) {
      if (Math.abs(o1 - o2) < offsetGranularity) return 0;
    }
    if (o1 < o2) return -1;
    if (o1 > o2) return 1;
    return 0;
  } // end of compareOffsets()
  
  /**
   * Returns the given offset rounded to the nearest 'quantum' as determined by 
   * {@link #getOffsetGranularity()}
   * @param offset
   * @return The given offset, rounded to the nearest {@link #getOffsetGranularity()} unit.
   */
  public Double quantumOffset(Double offset) {
    if (offset == null) return null;
    if (offsetGranularity == null) return offset;
    return offset - Math.IEEEremainder(offset + offsetGranularity/2, offsetGranularity);
  } // end of quantumOffset()
   
  /**
   * Adds a layer definition.
   * @param layer The layer to add.
   */
  public void addLayer(Layer layer) {
    getSchema().addLayer(layer);
  } // end of addLayer()
   
  /**
   * Get the definition of the given layer ID.
   * @param layerId The given layer ID.
   * @return The definition of the given layer ID.
   */
  public Layer getLayer(String layerId) {
    return getSchema().getLayer(layerId);
  } // end of getLayer()
   
  /**
   * Get a list of layers, ordered top down.
   * @return A list of layers, ordered top down.
   */
  public Vector<Layer> getLayersTopDown() {
    LayerHierarchyTraversal<Vector<Layer>> topDown = new LayerHierarchyTraversal<Vector<Layer>>(
      new Vector<Layer>(), getSchema()) {
        protected void pre(Layer layer) {
          result.add(layer);
        }
      };
    return topDown.getResult();
  } // end of getLayersTopDown()

  // TODO convenience methods:
   
  /**
   * Returns the labels of the annotations on the given layer, as an array of Strings.
   * @param layerId The given layer ID.
   * @return The labels of the annotations on the given layer, as an array of Strings.
   */
  public String[] labels(String layerId) {
    Annotation[] annotations = all(layerId);
    String[] labels = new String[annotations.length];
    int i = 0;
    for (Annotation annotation : annotations) {
      labels[i++] = annotation.getLabel();
    }
    return labels;
  } // end of labels()

  /*
    annotationsAt : function(offset, layerId) 
    {
    var annotations = [];
    var layers = layerId?{layerId:this.layers[layerId]}:this.layers;
    for (var l in layers)
    {
    var layer = layers[l];
    for (var a in layer.annotations)
    {
    var annotation = layer.annotations[a];
    if (annotation.includesOffset(offset)) annotations.push(annotation);
    if (annotation.start.offset > offset) break; // assuming the list is sorted, we can stop now
    } // next annotation
    } // next layer
    return (annotations.length > 0)?annotations:null;
    },
  */

  // annotation creation
   
  /**
   * Creates a tag annotation.
   * @param toTag The annotation to tag, which can be the parent, or a child of the correct parent.
   * @param layerId The tag layer ID.
   * @param label The tag label.
   * @return The tag annotation created.
   */
  public Annotation createTag(Annotation toTag, String layerId, String label) {
    return toTag.createTag(layerId, label);
  } // end of createTag()
  
  /**
   * Creates a tag and adds it to the graph.
   * <p>This method has the same effect as:
   * <code><var>graph</var>.addAnnotation(
   *       <var>graph</var>.creatTag(<var>toTag</var>, <var>layerId</var>,
   *                                 <var>label</var>));</code>  
   * @param toTag The annotation to tag.
   * @param layerId The tag layer ID.
   * @param label The tag label.
   * @return The tag annotation created.
   * @see #createTag(Annotation,String,String)
   * @deprecated  use {@link #createTag(Annotation,String,String)} instead.
   */
  @Deprecated
  public Annotation addTag(Annotation toTag, String layerId, String label) {
    return addAnnotation(createTag(toTag, layerId, label));
  } // end of addTag()
  
  /**
   * Creates an annotation that subdivides the given annotation.
   * <p> The first time this is called for any given annotation, a tag is created - i.e. a
   * new annotation that shares both start and end annotations with the given
   * annotation. Subsequent calls for the same annotation will chain new annotations
   * across the given annotation, using {@link #insertAfter(Annotation,String,String)}.
   * <p> Peer-layered tags for the last pre-existing annotation will have their end anchors 
   * updated, so they continue to share end anchors as before.
   * @param toSubdivide The annotation to subdivide.
   * @param layerId The new annotation's layer ID.
   * @param label The new annotation's label.
   * @return The annotation just created.
   */
  public Annotation createSubdivision(Annotation toSubdivide, String layerId, String label) {
    List<Annotation> existingTags = toSubdivide.getEnd().endOf(layerId)
      .stream().filter(a->a.getChange() != Change.Operation.Destroy)
      .collect(Collectors.toList());
    if (existingTags.size() == 0) { // there are no subdividers yet
      return createTag(toSubdivide, layerId, label);
    } else { // there's aleady at least one subdivider, so insert another after it
      Annotation before = existingTags.iterator().next();
      List<Annotation> peerLayerTags = Collections.EMPTY_LIST;
      if (before.getLayer() != null && before.getLayer().getParentId() != null) {
        final String parentLayerId = before.getLayer().getParentId();
        // identify peer-layered tags
        peerLayerTags = Arrays.stream(before.allTags())
          .filter(a -> a.getChange() != Change.Operation.Destroy) // not deleted annotations
          .filter(a -> a.getLayer() != null) // valid layer
          .filter(a -> !a.getLayerId().equals(layerId)) // not the layer we're adding to
          .filter(a -> !a.getLayerId().equals(toSubdivide.getLayerId())) // not the layer we're annotating
          .filter(a -> parentLayerId.equals(a.getLayer().getParentId())) // peer layer
          .collect(Collectors.toList());
      }
      Annotation after = insertAfter(before, layerId, label);
      // ensure peer-layered tags continue to share end anchor
      for (Annotation tag : peerLayerTags) {
        tag.setEnd(before.getEnd());
      }
      return after;
    }
  } // end of createSubdivision()  

  /**
   * Creates a spanning annotation from the beginning of the start annotation to the
   * ending of the end annotation. 
   * @param from The first annotation to span.
   * @param to The last annotation to span.
   * @param layerId The layer ID of the resulting annotation.
   * @param label The label of the resulting annotation.
   * @param parent The new annotation's parent (or a child of the correct parent).
   * @return The new annotation.
   */
  public Annotation createSpan(
    Annotation from, Annotation to, String layerId, String label, Annotation parent) {
    // first check whether this is the actual parent, or whether it's a child of the parent
    Layer spanLayer = getLayer(layerId);
    Layer parentLayer = getLayer(parent.getLayerId());
    String parentId = parent.getId();
    if (spanLayer != null
        && spanLayer.getParentId() != null
        && !spanLayer.getParentId().equals(parent.getLayerId())
        && parentLayer != null
        && spanLayer.getParentId().equals(parentLayer.getParentId())) {
      // parent of 'parent' is the real parent
      parentId = parent.getParentId();
    }
    
    // create the span
    Annotation span = new Annotation(null, label, layerId, from.getStartId(), to.getEndId(), parentId);

    if (parent.getAnnotations().containsKey(layerId)) { // parent has children on this layer
      // set ordinal before adding to the graph,
      // so that parent.correctOrdinals is not called during setParent
      span.setOrdinal(parent.getAnnotations().get(layerId).size()
                      + parent.ordinalMinimum(getLayerId())
                      + 1);
    }
    
    // add it to the graph
    getGraph().addAnnotation(span);
    return span;
  } // end of createSpan()
   
  /**
   * Creates a spanning annotation from the beginning of the start annotation to the ending of
   * the end annotation, and adds it to the graph.
   * <p>This method has the same effect as:
   * <code><var>graph</var>.addAnnotation(
   *       <var>graph</var>createSpan(<var>from</var>, <var>to</var>, <var>layerId</var>,
   *                                  <var>label</var>, <var>parent</var>));</code> 
   * @param from The first annotation to span.
   * @param to The last annotation to span.
   * @param layerId The layer ID of the resulting annotation.
   * @param label The label of the resulting annotation.
   * @param parent The new annotation's parent.
   * @return The new annotation.
   * @see #createSpan(Annotation,Annotation,String,String,Annotation)
   * @deprecated Use {@link #createSpan(Annotation,Annotation,String,String,Annotation)}
   */
  @Deprecated
  public Annotation addSpan(
    Annotation from, Annotation to, String layerId, String label, Annotation parent) {
    return addAnnotation(createSpan(from, to, layerId, label, parent));
  } // end of addSpan()
   
  /**
   * Creates a spanning annotation from the beginning of the start annotation to the
   * ending of the end annotation. The parent annotation is guessed, if possible, from
   * <var>from</var> or <var>to</var> 
   * @param from The first annotation to span.
   * @param to The last annotation to span.
   * @param layerId The layer ID of the resulting annotation.
   * @param label The label of the resulting annotation.
   * @return The new annotation.
   */
  public Annotation createSpan(Annotation from, Annotation to, String layerId, String label) {
    Layer spanLayer = getLayer(layerId);
    Annotation parent = null; // TODO can't we just use from.first(spanLayer.getParentId()) || to.first(spanLayer.getParentId()); ??
    if (spanLayer.getParentId().equals(from.getLayer().getParentId())) {
      // "from" layer and span layer share a parent
      parent = from.getParent();
    } else if (!from.getParentId().equals(to.getParentId()) 
             && spanLayer.getParentId().equals(to.getLayer().getParentId())) {
      // "to" layer and span layer share a parent
      parent = to.getParent();
    } else if (spanLayer.getParentId().equals(from.getLayerId())) {
      // unusual but possible case - the span should be the child of "from"
      parent = from;
    } else {
      // look for a parent in the ancestors of the annotations
      LinkedHashSet<Annotation> ancestors = from.getAncestors();
      if (!from.getParentId().equals(to.getParentId())) {
        ancestors.addAll(to.getAncestors());
      }
      for (Annotation ancestor : ancestors) {
        if (ancestor.getLayerId().equals(spanLayer.getParentId())) {
          parent = ancestor;
          break;
        }
      } // next ancestor
    }
    return createSpan(from, to, layerId, label, parent);
  } // end of createSpan()

  /**
   * Creates a spanning annotation from the beginning of the start annotation to the ending of
   * the end annotation and adds it to the graph. The parent annotation is guessed, if possible,
   * from <var>from</var> or <var>to</var>.
   * <p>This method has the same effect as:
   * <code><var>graph</var>.addAnnotation(
   *       <var>graph</var>.createSpan(<var>from</var>, <var>to</var>, <var>layerId</var>,
   *                                   <var>label</var>));</code>
   * @param from The first annotation to span.
   * @param to The last annotation to span.
   * @param layerId The layer ID of the resulting annotation.
   * @param label The label of the resulting annotation.
   * @return The new annotation.
   * @see #createSpan(Annotation,Annotation,String,String)
   * @deprecated Use {@link #createSpan(Annotation,Annotation,String,String)}
   */
  @Deprecated
  public Annotation addSpan(Annotation from, Annotation to, String layerId, String label) {
    return addAnnotation(createSpan(from, to, layerId, label));
  } // end of addSpan()

  /**
   * Creates an annotation starting at <var>from</var> and ending at <var>to</var>.
   * @param from The start anchor.
   * @param to The end anchor.
   * @param layerId The layer ID of the resulting annotation.
   * @param label The label of the resulting annotation.
   * @param parent The new annotation's parent.
   * @return The new annotation.
   */
  public Annotation createAnnotation(
    Anchor from, Anchor to, String layerId, String label, Annotation parent) {
    if (from.getId() == null) getGraph().addAnchor(from);
    if (to.getId() == null) getGraph().addAnchor(to);
    if (parent.getId() == null) {
      if (parent != getGraph() ) {
        getGraph().addAnnotation(parent);
      } else {
        throw new NullPointerException("Null graph ID");
      }
    }
    Annotation span = new Annotation(null, label, layerId, from.getId(), to.getId(), parent.getId());
    
    if (parent.getAnnotations().containsKey(layerId)) { // parent has children on this layer
      // set ordinal before adding to the graph,
      // so that parent.correctOrdinals is not called during setParent
      if (timers != null) timers.start("Graph.createAnnotation: setOrdinal");
      span.setOrdinal(parent.getAnnotations().get(layerId).size()
                      + parent.ordinalMinimum(getLayerId())
                      + 1);
      if (timers != null) timers.end("Graph.createAnnotation: setOrdinal");
    }
    
    getGraph().addAnnotation(span);
    return span;
  } // end of createAnnotation()

  /**
   * Creates an annotation starting at <var>from</var> and ending at <var>to</var>, and adds it
   * to the graph.
   * <p>This method has the same effect as:
   * <code><var>graph</var>.addAnnotation(
   *       <var>graph</var>.createAnnotation(<var>from</var>, <var>to</var>, <var>layerId</var>, <var>label</var>, <var>parent</var>));</code>
   * @param from The start anchor.
   * @param to The end anchor.
   * @param layerId The layer ID of the resulting annotation.
   * @param label The label of the resulting annotation.
   * @param parent The new annotation's parent.
   * @return The new annotation.
   * @deprecated Use {@link #createAnnotation(Anchor,Anchor,String,String,Annotation)}
   */
  @Deprecated
  public Annotation addAnnotation(
    Anchor from, Anchor to, String layerId, String label, Annotation parent) {
    return addAnnotation(createAnnotation(from, to, layerId, label, parent));
  } // end of createAnnotation()
  
  /**
   * Creates an annotation chained after the given annotation. This creates a new anchor,
   * which becomes <var>before.end</var> and the new annotation's start, and the previous
   * <var>before.end</var> becomes the new annotation's end. 
   * <p> The new anchor, and the new annotation, are both added to the graph.
   * @param before The annotation before the new annotation to add. 
   * If <var>layerId</var> == <var>before.layerId</var>, the new annotation's
   * <var>parentId</var> is set to <var>before.parentId</var>. Otherwise, it's the
   * caller's responsibility to assign a parent to the new annotation.
   * @param layerId The new annotation's layer ID.
   * @param label The new annotation's label.
   * @return The new annotation.
   */
  public Annotation insertAfter(Annotation before, String layerId, String label) {
    Annotation after = new Annotation(
      null, label, layerId,
      addAnchor(new Anchor()).getId(), before.getEndId());
    before.setEndId(after.getStartId());
    addAnnotation(after);
    if (layerId.equals(before.getLayerId())) {
      after.setParent(before.getParent(), false);
      after.setOrdinal(before.getOrdinal() + 1);
    }
    return after;
  } // end of insertAfter()
  
  /**
   * Creates an annotation chained before the given annotation. This creates a new anchor,
   * which becomes <var>after.start</var> and the new annotation's end, and the previous
   * <var>after.start</var> becomes the new annotation's start. 
   * <p> The new anchor, and the new annotation, are both added to the graph.
   * @param after The annotation after the new annotation to add. 
   * If <var>layerId</var> == <var>before.layerId</var>, the new annotation's
   * <var>parentId</var> is set to <var>before.parentId</var>. Otherwise, it's the
   * caller's responsibility to assign a parent to the new annotation.
   * @param layerId The new annotation's layer ID.
   * @param label The new annotation's label.
   * @return The new annotation.
   */
  public Annotation insertBefore(Annotation after, String layerId, String label) {
    Annotation before = new Annotation(
      null, label, layerId,
      after.getStartId(), addAnchor(new Anchor()).getId());
    after.setStartId(before.getEndId());
    addAnnotation(before);
    if (layerId.equals(after.getLayerId())) {
      before.setParent(after.getParent(), false);
      before.setOrdinal(after.getOrdinal());
    }
    return before;
  } // end of insertBefore()
  
  /**
   * Marks all annotations on the given layer for destruction/deletion.
   * @param layerId The ID of the layer of annotations to delete.
   * @return true if some annotations were destroyed, false otherwise
   */
  public boolean destroyAll(String layerId) {
    Layer layer = getLayer(layerId);
    boolean thereWereAnnotations = false;
    if (layer != null) {
      // for each parent layer annotation
      for (Annotation parent : all(layer.getParentId())) {
        if (parent.getAnnotations().containsKey(layerId)) {
          for (Annotation child : parent.getAnnotations().get(layerId)) {
            // all annotations are going, no peers need resetting, so use bulkDestroy
            child.bulkDestroy();
            thereWereAnnotations = true;
          } // next child
        } // parent has children on this layer
      } // next parent annotation
      if (orphans.containsKey(layerId)) { // there are orphans on this layer
        // destroy them too
        for (Annotation orphan : orphans.get(layerId)) {
          orphan.bulkDestroy();
          thereWereAnnotations = true;
        } // next orphan
      }
    } // layer is valid
    return thereWereAnnotations;
  } // end of destroyAll()

  // query methods
   
  /**
   * Returns a list of annotations that overlap with the given offset interval
   * @param annotation Annotation whose bounds define the interval.
   * @param layerId Layer to match.
   * @return A possibly empty array of annotations.
   */
  public Annotation[] overlappingAnnotations(Annotation annotation, String layerId)
  {
    return overlappingAnnotations(annotation.getStart(), annotation.getEnd(), layerId);
  } // end of overlappingAnnotations()
  /**
   * Returns a list of annotations that overlap with the offset interval defined by the
   * given anchors. 
   * @param start Start anchor.
   * @param end End anchor.
   * @param layerId Layer to match.
   * @return A possibly empty array of annotations.
   */
  public Annotation[] overlappingAnnotations(Anchor start, Anchor end, String layerId) {
    if (start == null || end == null || start.getOffset() == null || end.getOffset() == null) {
      return null;
    }
    return overlappingAnnotations(start.getOffset(), end.getOffset(), layerId);
  } // end of overlappingAnnotations()
  /**
   * Returns a list of annotations that overlap with the given offset interval.
   * @param start Start offset of the interval.
   * @param end End offset of the interval.
   * @param layerId Layer to match.
   * @return A possibly empty array of annotations.
   */
  public Annotation[] overlappingAnnotations(double start, double end, String layerId) {
    Vector<Annotation> matches = new Vector<Annotation>();
    for (Annotation annotation : all(layerId)) {
      if (annotation.getAnchored()
          && annotation.getEnd().getOffset() > start
          && annotation.getStart().getOffset() < end) {
        matches.add(annotation);
      }
    }
    return matches.toArray(new Annotation[0]);
  } // end of overlappingAnnotations()

  // Annotation overrides

  /**
   * Keys for attributes that are change-tracked - in the case of Graph, there are none.
   * @return An empty set.
   */
  public Set<String> getTrackedAttributes() { return new HashSet<String>(); }

  /**
   * Setter for <i>id</i>: The annotation's identifier.
   * @param id The annotation's identifier.
   */
  public Graph setId(String id) {
    // we don't want the graph or child re-hooking...
    this.id = id;
    return this;
  }

  /**
   * Getter for <i>startId</i>: ID of the anchor with the lowest offset.
   * @return ID of the anchor with the lowest offset.
   */
  public String getStartId() {
    Anchor earliest = null;
    int depth = Integer.MAX_VALUE;
    for (Anchor a : getAnchors().values()) {
      // if its the first one
      if (earliest == null 
          // or the offset is set and our current candidate has no offset
          || (a.getOffset() != null && earliest.getOffset() == null)
          // or both offsets are set and this offset is earlier
          || (a.getOffset() != null && earliest.getOffset() != null 
              && a.getOffset() < earliest.getOffset())
          // or neither offset is set and this ID is lower
          || (a.getOffset() == null && earliest.getOffset() == null
              && a.getId().compareTo(earliest.getId()) < 0)) {
        earliest = a;
        depth = depth(a);
      } else if (earliest.getOffset() != null && earliest.getOffset().equals(a.getOffset())) {
        // same offset
        // go with the anchor of the shallowest annotation
        int thisDepth = depth(a);
        if (thisDepth < depth) {
          earliest = a;
          depth = thisDepth;
        }
      }
    }
    if (earliest == null) return null;
    return earliest.getId();
  }
   
  /**
   * Getter for <i>endId</i>: ID of the anchor with the highest offset.
   * @return ID of the anchor with the highest offset.
   */
  public String getEndId() {
    Anchor latest = null;
    int depth = Integer.MAX_VALUE;
    for (Anchor a : getAnchors().values()) {
      assert a != null : "a != null";
      // if its the first one
      if (latest == null 
          // or the offset is set and our current candidate has no offset
          || (a.getOffset() != null && latest.getOffset() == null)
          // or both offsets are set and this offset is earlier
          || (a.getOffset() != null && latest.getOffset() != null 
              && a.getOffset() > latest.getOffset())
          // or neither offset is set and this ID is higher
          || (a.getOffset() == null && latest.getOffset() == null
              && a.getId().compareTo(latest.getId()) > 0)) {
        latest = a;
        depth = depth(a);
      } else if (latest.getOffset() != null && latest.getOffset().equals(a.getOffset())) {
        // same offset
        // go with the anchor of the shallowest annotation
        int thisDepth = depth(a);
        if (thisDepth <= depth) { // using <= means later anchors in the collection are preferred
          latest = a;
          depth = thisDepth;
        }
      }
    }
    if (latest == null) return null;
    return latest.getId();
  }
   
  /**
   * Returns the depth of an anchor - i.e. the depth of the shallowest non-aligned
   * associated Annotation. 
   * @param a The anchor to get the depth of.
   * @return The depth of the shallowest non-aligned associated Annotation.
   */
  protected int depth(Anchor a) {
    int depth = Integer.MAX_VALUE;
    for (String layerId : a.getStartOf().keySet()) {
      Layer layer = getLayer(layerId);
      if (layer != null && layer.getAlignment() != Constants.ALIGNMENT_NONE) {
        for (Annotation s : a.getStartOf().get(layerId)) {
          depth = Math.min(depth, s.getAncestors().size());
        }
      }
    }
    for (String layerId : a.getEndOf().keySet()) {
      Layer layer = getLayer(layerId);
      if (layer != null && layer.getAlignment() != Constants.ALIGNMENT_NONE) {
        for (Annotation s : a.getEndOf().get(layerId)) {
          depth = Math.min(depth, s.getAncestors().size());
        }
      }
    }
    return depth;
  } // end of depth()

  /**
   * Getter for <i>start</i>: The anchor with the lowest offset.
   * @return The anchor with the lowest offset.
   */
  public Anchor getStart() { return getAnchors().get(getStartId()); }

  /**
   * Getter for <i>end</i>: The anchor with the highest offset.
   * @return The anchor with the highest offset.
   */
  public Anchor getEnd() { return getAnchors().get(getEndId()); }

  /**
   * Gets a single related annotation on the given layer.
   * <p>"Related" means that <var>layerId</var> identifies the parent layer, an ancestor layer,
   * a child of an ancestor, or a child layer.
   * <p>This utility method makes navigating the layer hierarchy easier and with less prior
   * knowledge of it. e.g.:
   * <ul>
   *  <li><code>word.first("turn")</code> for the (parent) turn</li>
   *  <li><code>phone.first("turn")</code> for the (grandparent) turn</li>
   *  <li><code>word.first("POS")</code> for the (first) part of speech annotation</li>
   *  <li><code>phone.first("POS")</code> for the (first) part of speech annotation, which
   *    is neither an ancestor nor descendant, but rather is a child of an ancestor
   *    (<code>phone.first("word")</code>)</li> 
   *  <li><code>word.first("who")</code> for the speaker</li>
   *  <li><code>word.first("transcript")</code> for the transcript</li>
   *  <li><code>word.first("utterance")</code> for the utterance, which is neither an
   *    ancestor nor descendant, but rather is a child of an ancestor
   *    (<code>word.first("turn")</code>)</li> 
   *  <li><code>word.first("corpus")</code> for the graph's corpus, which is neither an
   *    ancestor nor descendant, but rather is a child of an ancestor
   *    (<code>word.first("transcript")</code>)</li> 
   * </ul>
   * <p>{@link #setGraph(Graph)} must have been previously called, and the graph must
   * have a correct layer hierarchy for this method to work correctly. 
   * <p>This override of {@link Annotation#first(String)} ensures that even orphaned annotations 
   * on the given layer are returned.
   * @param layerId The layer of the desired annotation.
   * @return The related annotation (or the first one if there are many), or null if none
   * could be found on the given layer. 
   */
  public Annotation first(final String layerId) {
    Annotation my = super.first(layerId);
    if (my == null && orphans.containsKey(layerId)) {
      Iterator<Annotation> orphansOnLayer = orphans.get(layerId).iterator();
      while (orphansOnLayer.hasNext()) {
        Annotation orphan = orphansOnLayer.next();
        // double-check it's still an orphan
        if (orphan.getParent() == null) {
          return orphan;
        } else {
          orphansOnLayer.remove();
        }
      } // next orphan
    }
    return my;
  }
   
  /**
   * Gets a list of related annotations on the given layer.
   * <p>"Related" means that <var>layerId</var> identifies the parent layer, an ancestor layer,
   * a child of an ancestor, a child layer, or a desdendant layer.
   * <p>This utility method makes navigating the layer hierarchy easier and with less prior
   * knowledge of it. e.g.:
   * <ul>
   *  <li><code>word.all("turn")[0]</code> for the (parent) turn</li>
   *  <li><code>phone.all("turn")[0]</code> for the (grandparent) turn</li>
   *  <li><code>word.all("POS")</code> for all (child) part of speech annotations</li>
   *  <li><code>word.all("phone")</code> for all (child) phones in a word</li>
   *  <li><code>turn.all("phone")</code> for all (grandchild) phones in a turn</li>
   *  <li><code>phone.all("POS")</code> for the all (peer) part of speech annotation,
   *   which are neither ancestors nor descendants, but rather children of an ancestor
   *   (<code>phone.first("word")</code>)</li> 
   *  <li><code>word.all("who")[0]</code> for the (grandparent) speaker</li>
   *  <li><code>word.all("transcript")[0]</code> for the (great-grandparent) transcript</li>
   *  <li><code>word.all("utterance")[0]</code> for the (peer) utterance, which is
   *   neither an ancestor nor descendant, but rather is a child of an ancestor
   *   (<code>word.my"turn")</code>)</li> 
   *  <li><code>utterace.all("word")</code> for the utterance's (peer) words, which are
   *   neither an ancestors nor descendants, but rather are children of an ancestor
   *   (<code>utterance.my"turn")</code>)</li> 
   *  <li><code>word.all("corpus")[0]</code> for the graph's (child of grandparent)
   *   corpus, which is neither an ancestor nor descendant, but rather is a child of an
   *   ancestor (<code>word.first("transcript")</code>)</li> 
   * </ul>
   * <p>{@link #setGraph(Graph)} must have been previously called, and the graph must have a
   * correct layer hierarchy for this method to work correctly.
   * <p>This override of {@link Annotation#all(String)} ensures that even orphaned annotations 
   * on the given layer are returned.
   * @param layerId The layer of the desired annotations.
   * @return The related annotations, or an empty array if none could be found on the given layer.
   */
  public Annotation[] all(final String layerId) {
    Annotation[] annotations = super.all(layerId);
    if (orphans.containsKey(layerId)) {
      Vector<Annotation> orphansToAdd = new Vector<Annotation>();
      Iterator<Annotation> orphansOnLayer = orphans.get(layerId).iterator();
      while (orphansOnLayer.hasNext()) {
        Annotation orphan = orphansOnLayer.next();
        // double-check it's still an orphan
        if (orphan.getParent() == null) {
          orphansToAdd.add(orphan);
        } else {
          orphansOnLayer.remove();
        }
      } // next orphan
      if (orphans.get(layerId).size() == 0) orphans.remove(layerId);
      if (orphansToAdd.size() > 0) {
        Vector<Annotation> annotationList = new Vector<Annotation>(Arrays.asList(annotations));
        annotationList.addAll(orphansToAdd);
        return annotationList.toArray(new Annotation[0]);
      }
    } // there are orphans
    return annotations;
  }

  // Maintain a set of indexes to narrow down search space when looking for t-includin annotations
  // Key is the layerId, Value is a map of nearest-integer offsets to annotations that are nearby
  HashMap<String,HashMap<Integer,Set<Annotation>>> indicesByLayer = new HashMap<String,HashMap<Integer,Set<Annotation>>>();
  /**
   * Determine which annotations on the given layer are 'near' the given offset. 'Near'
   * means that the annotations t-include within 1.0 of the given offset.  
   * <p> The first time this is called for a given layer, an index is constructed so that
   * subsequent calls on the same layer return quicker. 
   * <p> The index is rebuild after any of {@link Anchor#setOffset(Double)},
   * {@link Annotation#setStartId(String)}, {@link Annotation#setEndId(String)}, 
   * or {@link Annotation#setGraph(Graph)} are called, to ensure it's up-to-date with
   * graph changes. 
   * @param layerId The layer for the returned annotations.
   * @param offset The offset to look near.
   * @return A list of annotations on the given layer which are near the given offset.
   */
  Set<Annotation> listNear(String layerId, double offset) {
    // System.out.println("listNear " + layerId + " " + offset);
    if (!indicesByLayer.containsKey(layerId)) { // build the index for this layer
      // System.out.println("Build index for " + layerId);
      HashMap<Integer,Set<Annotation>> index = new HashMap<Integer,Set<Annotation>>();
      indicesByLayer.put(layerId, index);
      for (Annotation a : all(layerId)) {
        if (a.getAnchored()) {
          // add this annotation to the index, from just before the start, to just after the end
          int from = a.getStart().getOffset().intValue();
          int to = a.getEnd().getOffset().intValue() + 1;
          // System.out.println(" " + a + " from " + from + " to " + to);
          for (int o = from; o <= to; o++)
          { // for each integer in the range
            if (!index.containsKey(o)) index.put(o, new HashSet<Annotation>());
            index.get(o).add(a);
          } // next integer in the range
        } // offsets set
      } // next annotation
    } // build the index for this layer
    HashMap<Integer,Set<Annotation>> index = indicesByLayer.get(layerId);
    HashSet<Annotation> nearby = new HashSet<Annotation>();
    if (index.containsKey((int)offset)) nearby.addAll(index.get((int)offset));
    offset = offset + 1.0;
    if (index.containsKey((int)offset)) nearby.addAll(index.get((int)offset));
    return nearby;
  } // end of listNear()
  
  /**
   * Convenience function that links each utterance to the words contained in it, via the
   * "@words" attribute.
   * <p> By default, the utterance and word layers are 'peers' - i.e. the parent of both
   * is the turn layer. This is so words easily and naturally link to each other across
   * utterance boundaries.
   * <p> However, there are circumstances where it's convenient to process words by
   * utterance instead of by turn. Under such circumstances, this method can be used to
   * efficiently link all words to the utterance that contain them.
   * <p>Each utterance is assigned a new "@words" key, which is a List&lt;Annotation&gt;,
   * which contains the words that start within the bounds of the utterance, or at the end
   * is not within the bounds of the next utterance.
   */
  public void assignWordsToUtterances() {
    if (schema.getTurnLayerId() == null
        || schema.getUtteranceLayerId() == null
        || schema.getWordLayerId() == null) { // not all the reuired layers are there
      return;
    }
    // assign words to each utterance
    for (Annotation turn : all(getSchema().getTurnLayerId())) {
      if (turn.getChange() == Change.Operation.Destroy) continue;
      Iterator<Annotation> utterances
        = new AnnotationsByAnchor(turn.getAnnotations(getSchema().getUtteranceLayerId()))
        .stream()
        .filter(u -> u.getChange() != Change.Operation.Destroy)
        .filter(u -> u.getStart().getOffset() != null)
        .iterator();
      if (!utterances.hasNext()) continue;
      Annotation currentUtterance = utterances.next();
      currentUtterance.put("@words", new Vector<Annotation>());
      Annotation nextUtterance = utterances.hasNext()?utterances.next():null;
      SortedSet<Annotation> words = turn.getAnnotations(getSchema().getWordLayerId());
      if (words != null) {
        for (Annotation word : words) {
          if (word.getChange() == Change.Operation.Destroy) continue;
          if (word.getStart() == null) continue; // ?!
          if (// the start is inside the next utterance...
            (word.getStart().getOffsetMin() != null 
             && nextUtterance != null
             && word.getStart().getOffsetMin() >= nextUtterance.getStart().getOffset())
            || // ...or the start offset is null and the end is inside the next utterance
            (word.getStart().getOffset() == null
             && word.getEnd().getOffsetMax() != null
             && nextUtterance != null
             && word.getEnd().getOffsetMax() > nextUtterance.getStart().getOffset())) {
            // check it's not an empty utterance
            do {
              // next utterance
              currentUtterance = nextUtterance;
              nextUtterance = utterances.hasNext()?utterances.next():null;
            } while (word.getStart().getOffsetMin() != null 
                     && nextUtterance != null
                     && word.getStart().getOffsetMin() >= nextUtterance.getStart().getOffset());
            currentUtterance.put("@words", new Vector<Annotation>());
          } // next utterance
          ((Vector<Annotation>)currentUtterance.get("@words")).add(word);
        } // next word
      } // there are words
    } // next turn
  } // end of assignWordsToUtterances()
  
  /**
   * Getter for <i>layer</i>: The graph's layer definition, which is by definition the
   * root of its schema. 
   * @return The annotation's layer definition.
   * @see #getSchema()
   * @see Schema#getRoot()
   */
  public Layer getLayer() { 
    return schema.getRoot();
  }
  /**
   * Getter for <i>layerId</i>: The graph's layer ID, which is by definition the root of
   * its schema. 
   * @return The annotation's layer ID.
   * @see #getSchema()
   * @see Schema#getRoot()
   */
  public String getLayerId() { 
    return schema.getRoot().getId();
  }
  /**
   * Getter for <i>label</i>: The label for the whole graph, which is defined as it's ID.
   * @return The graph's label.
   */
  public String getLabel() { 
    return getId();
  }

  /**
   * Getter for {@link #graph}, which always returns this.
   * @return this.
   */
  public Graph getGraph() { 
    return this;
  }

  /**
   * Returns the graph which this is a fragment of.
   * @return The fragment's original graph, or this, if this graph is not a fragment.
   */
  public Graph sourceGraph() { 
    if (!isFragment()) return this;
    return graph;
  }

  /**
   * Commits graphs's changes, if any.  Any annotations/anchors marked for deletion are removed.
   */
  public void commit() {
    if (tracker == null) return;
      
    // annotations
    Iterator<Annotation> iAnnotation = getAnnotationsById().values().iterator();
    while (iAnnotation.hasNext()) {
      Annotation a = iAnnotation.next();
      if (a.getChange() == Change.Operation.Destroy) {
        a.setStartId(null); // remove reference from start anchor
        a.setEndId(null); // remove reference from end anchor
        Annotation p = a.getParent();
        a.setParent(null); // remove reference from parent
        // ensure it doesn't linger in the orphans list
        if (orphans.containsKey(a.getLayerId())) orphans.get(a.getLayerId()).remove(a);
        a.setLayer(null);
        iAnnotation.remove();
      }
    } // next annotation

    // anchors
    Iterator<Anchor> iAnchor = getAnchors().values().iterator();
    while (iAnchor.hasNext()) {
      Anchor a = iAnchor.next();
      if (a.getChange() == Change.Operation.Destroy) {
        iAnchor.remove();
      }
    } // next anchor

    tracker.reset();
  }
   
  /**
   * Adds a default change tracker. From this point on, changes to the graph or any of
   * its Anchors/Annotations will be tracked and reversible. 
   */
  public void trackChanges() {
    if (tracker == null) {
      setTracker(new ChangeTracker());
      tracker.reset(); // ignore any prehistoric creates/deletes
    }
  } // end of trackChanges()

  /**
   * Applies tracked changes made in the given graph/fragment to this graph.
   * <p> <em>NB:</em> It is assumed that the updated anchors/annotations and the destroyed
   * annotations have the name IDs in both graphs. For new anchors/annotations, the IDs
   * from the fragment are not used; this graph generates new IDs, and then the
   * corresponding anchors/annotations in the fragment have their IDs changed to match
   * those from this graph. This means that a side-effect of this method is that the given
   * fragment may change. No anchors are marked for deletion.
   * @param fragment The fragment (or graph) from which the changes come.
   * @param includeLayers A set of layers specifying which annotations to apply changes
   * to, or null for updating all annotations.
   */
  public void applyChangesFromFragment(Graph fragment, Set<String> includeLayers) {
    // collect up changed objects
    HashSet<Anchor> createdAnchors = new HashSet<Anchor>();
    HashSet<Annotation> createdAnnotations = new HashSet<Annotation>();
    HashSet<Anchor> updatedAnchors = new HashSet<Anchor>();
    HashSet<Annotation> updatedAnnotations = new HashSet<Annotation>();
    HashSet<Annotation> destroyedAnnotations = new HashSet<Annotation>();
    // (we don't destroy any anchors)    
    for (Change change : fragment.getTracker().getChanges()) {
      if (change.getObject() instanceof Anchor) {
        switch(change.getObject().getChange()) {
          case Create:
            createdAnchors.add((Anchor)change.getObject());
            break;
          case Update:
            updatedAnchors.add((Anchor)change.getObject());
            break;
        }
      } else if (change.getObject() instanceof Annotation) {
        Annotation annotation = (Annotation)change.getObject();
        if (includeLayers == null || includeLayers.contains(annotation.getLayerId())) {
          switch(annotation.getChange()) {
            case Create:
              createdAnnotations.add(annotation);
              break;
            case Update:
              updatedAnnotations.add(annotation);
              break;
            case Destroy:
              destroyedAnnotations.add(annotation);
              break;
          }
        } // include this layer
      } // Annotation
    } // next change

    // anything that's created doesn't need to be updated
    updatedAnchors.removeAll(createdAnchors);
    updatedAnnotations.removeAll(createdAnnotations);

    // annotations that are deleted don't need to be updated or created
    updatedAnnotations.removeAll(destroyedAnnotations);
    createdAnnotations.removeAll(destroyedAnnotations);
    
    // create new anchors
    for (Anchor fragmentAnchor : createdAnchors) {
      Anchor graphAnchor = (Anchor)fragmentAnchor.clone();
      graphAnchor.setId(null); // need a new ID in the graph
      graphAnchor.create();
      graph.addAnchor(graphAnchor);
      // set the changed object to match the new ID
      // this will update all referencing annotations too
      fragmentAnchor.setId(graphAnchor.getId());
    } // next anchor
    
    // create new annotations
    for (Annotation fragmentAnnotation : createdAnnotations) {
      Annotation graphAnnotation = (Annotation)fragmentAnnotation.clone();
      graphAnnotation.setId(null); // need a new ID in the graph
      graphAnnotation.create();
      graph.addAnnotation(graphAnnotation);
      // set the changed object to match the new ID
      // this will update all referencing annotations too
      fragmentAnnotation.setId(graphAnnotation.getId());
    } // next annotation

    // update anchors
    for (Anchor fragmentAnchor : updatedAnchors) {
      Anchor graphAnchor = (Anchor)graph.getAnchor(fragmentAnchor.getId());
      if (graphAnchor != null) {
        graphAnchor.clonePropertiesFrom(fragmentAnchor, "id");
      } // graph anchor found
    } // next anchor

    // update annotations
    for (Annotation fragmentAnnotation : updatedAnnotations) {
      Annotation graphAnnotation = (Annotation)graph.getAnnotation(fragmentAnnotation.getId());
      if (graphAnnotation != null) {
        graphAnnotation.clonePropertiesFrom(fragmentAnnotation, "id");
      } // graph annotation found
    } // next annotation

    // no deletion of anchors

    // delete annotations
    for (Annotation fragmentAnnotation : destroyedAnnotations) {
      Annotation graphAnnotation = (Annotation)graph.getAnnotation(fragmentAnnotation.getId());
      if (graphAnnotation != null) {
          graphAnnotation.destroy();
      } // graph annotation found
    } // next annotation
    
  } // end of applyChangesFromFragment()  
   
  // TrackedMap methods

  /**
   * Ensure tracker is updated for all known annotations and anchors.
   * @param newTracker Object that tracks all changes to this object.
   */
  public TrackedMap setTracker(ChangeTracker newTracker) {
    super.setTracker(newTracker);
    getAnchors().values().forEach(
      a -> a.setTracker(newTracker));
    getAnnotationsById().values().forEach(
      a -> a.setTracker(newTracker));
    return this;
  }

  /**
   * Rolls back changes since the object was create or {@link #commit()} was last
   * called. The effect of this is to rollback all anchors and annotations. 
   * @see #getTrackedAttributes()
   */
  public void rollback() {
    if (tracker == null) return;
      
    super.rollback();
    // anchors
    Iterator<Anchor> iAnchor = getAnchors().values().iterator();
    while (iAnchor.hasNext()) {
      Anchor a = iAnchor.next();
      if (a.getChange() == Change.Operation.Create) {
        iAnchor.remove();
      } else {
        a.rollback();
      }
    } // next anchor

      // annotations
    Iterator<Annotation> iAnnotation = getAnnotationsById().values().iterator();
    while (iAnnotation.hasNext()) {
      Annotation a = iAnnotation.next();
      if (a.getChange() == Change.Operation.Create) {
        a.setStartId(null); // remove reference from start anchor
        a.setEndId(null); // remove reference from end anchor
        a.setParent(null); // remove reference from parent
        a.setLayer(null);
        iAnnotation.remove();
      } else {
        a.rollback();
      }
    } // next annotation
    tracker.reset();
  } // end of rollback()
   
  /**
   * Determines how the object has changed since it was originally defined, or since
   * {@link #commit()} was last called.
   * @return How/whether the object has been changed.
   * @see #getTrackedAttributes()
   */
  public Change.Operation getChange() {
    if (tracker == null) return Change.Operation.NoChange;
      
    Change.Operation operation = super.getChange();
    if (operation == Change.Operation.NoChange) { // check for changes to anchors or annotations
      if (tracker.hasChanges()) operation = Change.Operation.Update;
    }
    return operation;
  } // end of getChange()   

  /**
   * Produces a list of individual changes for this graph and its elements.
   * @return A list of individual changes for the object.
   */
  public List<Change> getChanges() {
    final LinkedHashSet<TrackedMap> changed = new LinkedHashSet<TrackedMap>();
    if (super.getChange() == Change.Operation.Create
        || super.getChange() == Change.Operation.Update) {
      // graph changes
      changed.add(this);
    }
    // anchor creations/updates
    getChangedAnchors()
      .filter(anchor -> anchor.getChange() != Change.Operation.Destroy)
      .forEach(anchor -> changed.add(anchor));
    // annotations
    for (Annotation annotation : getChangedAnnotationsOrdered()) {
      changed.add(annotation);
    }
    // anchor deletions
    getChangedAnchors()
      .filter(anchor -> anchor.getChange() == Change.Operation.Destroy)
      .forEach(anchor -> changed.add(anchor));
    if (super.getChange() == Change.Operation.Destroy) {
      // graph deletion at the end
      changed.add(this);
    }
    return changed.stream()
      .flatMap(a -> {
          // don't infinitely recurse for graph changes
          if (a == this) return super.getChanges().stream(); 
          return a.getChanges().stream();
        })
      .collect(Collectors.toList());
  } // end of getChanges()
  
  /**
   * A stream of anchors that have registered changes, in no particular order.
   * @return A stream of anchors that have registered changes, which will be empty if
   * {@link #tracker} is not set.
   */
  public Stream<Anchor> getChangedAnchors() {
    if (tracker == null) return Stream.empty();
    if (super.getChange() == Change.Operation.Create) { 
      // all anchors must be created
      return getAnchors().values().stream()
        .filter(a -> a.getChange() != Change.Operation.Destroy) // don't include destroyed anchors
        .peek(a -> a.create());
    } else if (super.getChange() == Change.Operation.Destroy) {
      // all anchors must be destroyed
      return getAnchors().values().stream()
        .peek(a -> a.destroy());
    }
    return tracker.getChanges().stream()
      .filter(change -> change.getObject() instanceof Anchor)
      .map(change -> (Anchor)change.getObject())
      .distinct();
  } // end of getChangedAnchors()
  
  /**
   * Produces a stream of annotations that have registered changes, in no particular order. 
   * <p> This does <em>not</em> include changes to the graph itself.
   * @return A stream of annotations that have registered changes, which will be empty if
   * {@link #tracker} is not set.
   */
  public Stream<Annotation> getChangedAnnotations() {
    if (tracker == null) return Stream.empty();
    if (super.getChange() == Change.Operation.Create) { 
      // all annotations must be created
      return getAnnotationsById().values().stream()
        .filter(a -> a.getChange() != Change.Operation.Destroy) // not destroyed annotations
        .peek(a -> a.create());
    } else if (super.getChange() == Change.Operation.Destroy) {
      // all annotations must be destroyed
      return getAnnotationsById().values().stream()
        .peek(a -> a.bulkDestroy()); // no peer ordinals need resetting so use bulkDestroy
    }
    return tracker.getChanges().stream()
      .filter(change -> change.getObject() instanceof Annotation)
      .map(change -> (Annotation)change.getObject())
      .filter(annotation -> annotation != this) // no graph changes
      .distinct();
  } // end of getChangedAnnotations()
  
  /**
   * Produces a collection of annotations that have registered changes, ordered so that
   * hierarchically higher created annotations are first and hierarchically lower deleted
   * annotations are last. 
   * @return A collection of annotations that have registered changes, ordered so that
   * created annotations are first and deleted annotations are last. 
   */
   public List<Annotation> getChangedAnnotationsOrdered() {
     final HashMap<String,List<Annotation>> layerToAnnotations
       = new HashMap<String,List<Annotation>>();
     getChangedAnnotations().forEach(annotation -> {
         if (!layerToAnnotations.containsKey(annotation.getLayerId())) {
           layerToAnnotations.put(annotation.getLayerId(), new Vector<Annotation>());
         }
         layerToAnnotations.get(annotation.getLayerId()).add(annotation);
       }); // next changed annotation

     Vector<Annotation> annotations = new Vector<Annotation>();
     
     if (getChange() != Change.Operation.Destroy) { // not deleting the graph
       // ensure all possible parents are created before their children
       new LayerHierarchyTraversal<Vector<Annotation>>(annotations, schema) {
         protected void pre(Layer layer) { // parents created before children
           if (layerToAnnotations.containsKey(layer.getId())) {
             layerToAnnotations.get(layer.getId()).stream()
               .filter(a -> a.getChange() == Change.Operation.Create)
               .forEach(a -> result.add(a));
           }
         }
       };
     } // not deleting the graph
     
     if (getChange() == Change.Operation.Update) { // not creating/deleting the graph
       // perform updates after creates and before deletes
       new LayerHierarchyTraversal<Vector<Annotation>>(annotations, schema) {
         protected void pre(Layer layer) { // parents updated before children
           if (layerToAnnotations.containsKey(layer.getId())) {
             layerToAnnotations.get(layer.getId()).stream()
               .filter(a -> a.getChange() == Change.Operation.Update)
               .forEach(a -> result.add(a));
           }
         }
       };
     } // not creating/deleting the graph
     
     if (getChange() != Change.Operation.Create) { // not creating the graph
       // now perform deletes, children first
       new LayerHierarchyTraversal<Vector<Annotation>>(annotations, schema) {
         protected void post(Layer layer) { // parents deleted after children
           if (layerToAnnotations.containsKey(layer.getId())) {
             layerToAnnotations.get(layer.getId()).stream()
               .filter(a -> a.getChange() == Change.Operation.Destroy)
               .forEach(a -> result.add(a));
           }
         }
       };
     } // not creating the graph

     return annotations;     
   } // end of getChangedAnnotationsOrdered()
    
} // end of class Graph
