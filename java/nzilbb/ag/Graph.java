//
// Copyright 2015-2016 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.util.List;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Vector;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Locale;
import java.util.Iterator;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import nzilbb.ag.util.LayerTraversal;
import nzilbb.ag.util.LayerHierarchyTraversal;
import nzilbb.ag.util.AnchorComparatorWithStructure;

/**
 * Linguistic annotation graph.
 * <p>An annotation graph is a collection of {@link Annotation}s (edges) joined by {@link Anchor}s (nodes) which may or may not have temporal/character offsets. Superimposed over this temporallay anchored graph is another heirarchical graph, where annotations are nodes and edges are child-to-parent links.
 * <p>In addition to containing the nodes/edges, this class inherits from {@link Annotation} so that it can:
 * <ul>
 *   <li>be the root node of the annotation hierarchy - i.e. be the parent of annotations at the top of the layer hierarchy</li>
 *   <li>have start/end anchors</li>
 * </ul>
 * <p>In addition to this, the graph also has:
 * <ul>
 *  <li>a corpus attribute representing the collection to which it belongs (see {@link #getCorpus()}, {@link #setCorpus(String)}),</li>
 *  <li>definitions of annotation {@link Layer}s and their hierarchy</li>
 * </ul>
 * <p>It is recommended that other graph attributes are represented as annotations that 'tag' the whole graph, and that speakers/participants are also represented as such annotations, on a "participant" layer, which is the parent of a "turn" layer which defines speaker turns.
 * <p>This class can also represent graph fragments (sub-graphs).  If this is a whole graph, {@link Annotation#graph} == <var>this</var>, but if it's a fragment, then {@link Annotation#graph} != <var>this</var>. The {@link #isFragment()} convenience method captures this principle. The annotations in a graph fragment have the fragment object (not the whole-graph object) set as their {@link Annotation#graph}.
 * @author Robert Fromont robert.fromont@canterbury.ac.nz
 */
@SuppressWarnings("serial")
public class Graph
   extends Annotation
{
   /**
    * New ID seed.
    */
   protected long lastId = 0;

   /**
    * The ID of the end anchor of the last annotation added using {@link #addAnnotation(Annotation)}, or the last anchor added using {@link #addAnchor(Anchor)}.
    */
   protected String lastAddedAnchorId;
   /**
    * Getter for lastAddedAnchorId: The ID of the end anchor of the last annotation added using {@link #addAnnotation(Annotation)}, or the last anchor added using {@link #addAnchor(Anchor)}.
    * @return The ID of the end anchor of the last annotation added using {@link #addAnnotation(Annotation)}, or the last anchor added using {@link #addAnchor(Anchor)}.
    */
   public String getLastAddedAnchorId() { return lastAddedAnchorId; }

   // keep a list of annotations whose anchors haven't been added yet
   private HashMap<String,Vector<Annotation>> unknownStartAnchor = new HashMap<String,Vector<Annotation>>();
   private HashMap<String,Vector<Annotation>> unknownEndAnchor = new HashMap<String,Vector<Annotation>>();

   /**
    * Getter for corpus: The name of the corpus the graph belongs to.
    * @return The name of the corpus the graph belongs to.
    */
   public String getCorpus() { try { return (String)get("corpus"); } catch(ClassCastException exception) {return null;} }
   /**
    * Setter for corpus: The name of the corpus the graph belongs to.
    * @param corpus The name of the corpus the graph belongs to.
    */
   public void setCorpus(String corpus) { put("corpus", corpus); }

   /**
    * Getter for <i>anchors</i>: Map of anchors (graph nodes) keyed by id. 
    * <p>In the underlying map, this is stored as the "children" attribute of a "layers" attribute.
    * @return Map of anchors (graph nodes) keyed by id.
    */
   @SuppressWarnings("unchecked")
   public LinkedHashMap<String,Anchor> getAnchors() 
   { 
      if (!containsKey("anchors"))
      {
	 setAnchors(new LinkedHashMap<String,Anchor>());
      }
      try 
      { 
	 return (LinkedHashMap<String,Anchor>)get("anchors"); 
      } 
      catch(ClassCastException exception) 
      {
	 return null;
      } 
   }
   /**
    * Setter for <i>anchors</i>: Map of anchors (graph nodes) keyed by id. 
    * <p>In the underlying map, this is stored as the "children" attribute of a "layers" attribute.
    * @param anchors Map of anchors (graph nodes) keyed by id.
    */
   public void setAnchors(LinkedHashMap<String,Anchor> anchors) { put("anchors", anchors); }

   
   /**
    * Granularity of offsets - e.g. 0.001 if Anchor offsets are always set to the the nearest millisecond, or null for no particular granularity.
    * @see #getOffsetGranularity()
    * @see #setOffsetGranularity(Double)
    */
   protected Double offsetGranularity;
   /**
    * Getter for {@link #offsetGranularity}: Granularity of offsets - e.g. 0.001 if Anchor offsets are always set to the the nearest millisecond, or null for no particular granularity.
    * @return Granularity of offsets - e.g. 0.001 if Anchor offsets are always set to the the nearest millisecond, or null for no particular granularity.
    */
   public Double getOffsetGranularity() { return offsetGranularity; }
   /**
    * Setter for {@link #offsetGranularity}: Granularity of offsets - e.g. 0.001 if Anchor offsets are always set to the the nearest millisecond, or null for no particular granularity.
    * @param newOffsetGranularity Granularity of offsets - e.g. 0.001 if Anchor offsets are always set to the the nearest millisecond, or null for no particular granularity.
    */
   public void setOffsetGranularity(Double newOffsetGranularity) { offsetGranularity = newOffsetGranularity; }

   /**
    * The units for anchor offsets - e.g. "s" for seconds, "char" for characters, etc.  Preferably the value should be one of the Constants.UNIT_... constants. The default value is {@link Constants#UNIT_SECONDS}.
    * @see #getOffsetUnits()
    * @see #setOffsetUnits(String)
    * @see Constants#UNIT_SECONDS
    * @see Constants#UNIT_CHARACTERS
    */
   protected String offsetUnits = Constants.UNIT_SECONDS;
   /**
    * Getter for {@link #offsetUnits}: The units for anchor offsets - e.g. "s" for seconds, "char" for characters, etc.  Preferably the value should be one of the Constants.UNIT_... constants. The default value is {@link Constants#UNIT_SECONDS}.
    * @return The units for anchor offsets - e.g. "s" for seconds, "char" for characters, etc.  Preferably the value should be one of the Constants.UNIT_... constants.
    */
   public String getOffsetUnits() { return offsetUnits; }
   /**
    * Setter for {@link #offsetUnits}: The units for anchor offsets - e.g. "s" for seconds, "char" for characters, etc.  Preferably the value should be one of the Constants.UNIT_... constants.
    * @param newOffsetUnits The units for anchor offsets - e.g. "s" for seconds, "char" for characters, etc.  Preferably the value should be one of the Constants.UNIT_... constants.
    */
   public void setOffsetUnits(String newOffsetUnits) { offsetUnits = newOffsetUnits; }


   // TODO participants
   
   
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
    * Setter for {@link #annotationsById}: Map of annotations (graph edges) keyed by id.
    * @param newAnnotationsById Map of annotations (graph edges) keyed by id.
    */
   public void setAnnotationsById(LinkedHashMap<String,Annotation> newAnnotationsById) { annotationsById = newAnnotationsById; }


   
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
   public Schema getSchema() { return schema; }
   /**
    * Setter for {@link #schema}: The layer definitions and their interrelations.
    * @param newSchema The layer definitions and their interrelations.
    */
   public void setSchema(Schema newSchema) { schema = newSchema; }

   
   // Methods:
      
   /**
    * Default constructor.
    */
   public Graph()
   {
      graph = this;
      setLayerId(getSchema().getRoot().getId());
   } // end of constructor

   
   /**
    * Determines whether this is a fragment of a larger graph (true) or the whole graph (false).
    * @return false if {@link Annotation#graph} == <var>this</var>, true otherwise.
    */
   public boolean isFragment()
   {
      return !(graph == this);
   } // end of isFragment()

   /** Static format so we don't keep creating and destroying one */
   private static DecimalFormat offsetFormat = new DecimalFormat(
      // force the locale to something with . as the decimal separator
      "0.000", new DecimalFormatSymbols(Locale.UK));
   
   /**
    * Creates a fragment of this graph, copying into it annotations that fall within the given bounds, on the given layers. 
    * Ancestors which do not fall into the interval are also added to the fragment, so that turns/speakers/etc. are accessible, but their anchors are not added to the fragment.
    * <p>The ID of the new fragment is <var>graphId</var>__<var>startOffset</var>-<var>endOffset</var>
    * @param startOffset The start offset for annotations to include in the fragment.
    * @param endOffset The end offset for annotations to include in the fragment.
    * @param layerIds A list of IDs of layers to include in the fragment.
    * @return A graph fragment containing the annotations that fall within the given bounds, on the given layers.
    */
   public Graph getFragment(double startOffset, double endOffset, List<String> layerIds)
   {
      Graph fragment = new Graph();
      fragment.graph = this;
      if (getId() != null)
      {
	 fragment.setId(getId() 
			+ "__" + offsetFormat.format(startOffset) 
			+ "-" + offsetFormat.format(endOffset));
      }
      for (String layerId : layerIds)
      {
	 Layer layer = getLayer(layerId);
	 if (layer != null)
	 {
	    fragment.addLayer((Layer)layer.clone());
	    for (Annotation annotation : getAnnotations(layerId))
	    {
	       Double min = annotation.getStart().getOffsetMin();
	       Double max = annotation.getEnd().getOffsetMax();
	       if (min != null && min >= startOffset
		   && max != null && max <= endOffset)
	       {
		  // add the anchors
		  fragment.addAnchor((Anchor)annotation.getStart().clone());
		  fragment.addAnchor((Anchor)annotation.getEnd().clone());
		  // add the annotation
		  fragment.addAnnotation((Annotation)annotation.clone());
	       }
	    } // next annotation
	 } // layer exists
      } // next layer

      // now that we have the temporal slice, add missing ancestors (but not their anchors)
      Vector<Annotation> annotations = new Vector<Annotation>(fragment.getAnnotationsById().values());
      for (Annotation fragAnnotation : annotations)
      {
	 Annotation graphAnnotation = getAnnotation(fragAnnotation.getId());
	 for (Annotation ancestor : graphAnnotation.getAncestors())
	 {
	    if (!ancestor.getLayerId().equals("graph")
		&& !fragment.getAnnotationsById().containsKey(ancestor.getId()))
	    {
	       if (!fragment.schema.getLayers().containsKey(ancestor.getLayerId()))
	       { // add the ancestor's layer
		  fragment.addLayer(getLayer(ancestor.getLayerId()));
	       }
	       // add the ancestor
	       fragment.addAnnotation((Annotation)ancestor.clone());
	    }
	 }
      } // next annotation
      return fragment;
   } // end of getFragment()

   /**
    * Creates a fragment of this graph, copying into it the given annotation and its descendants, on the given layers. 
    * Ancestors which do not fall into the interval are also added to the fragment, so that turns/speakers/etc. are accessible, but their anchors are not added to the fragment.
    * <p>The ID of the new fragment is <var>graphId</var>__<var>fragment.start.offset</var>-<var>fragment.end.offset</var>
    * @param definingAnnotation The annotation that defines fragment membership.
    * @param layerIds A list of IDs of layers to include in the fragment.
    * @return A graph fragment containing the annotations that fall within the given bounds, on the given layers.
    */
   public Graph getFragment(Annotation definingAnnotation, List<String> layerIds)
   {
      Graph fragment = new Graph();
      fragment.graph = this;
      Layer layer = getLayer(definingAnnotation.getLayerId());
      if (layer != null)
      {
	 // add the layer
	 fragment.addLayer((Layer)layer.clone());
	 // add the anchors
	 fragment.addAnchor((Anchor)definingAnnotation.getStart().clone());
	 fragment.addAnchor((Anchor)definingAnnotation.getEnd().clone());
	 // add the annotation
	 fragment.addAnnotation((Annotation)definingAnnotation.clone());
      }
      // add descendants
      for (String layerId : layerIds)
      {
	 layer = getLayer(layerId);
	 if (layer != null)
	 {
	    fragment.addLayer((Layer)layer.clone());
	    for (Annotation annotation : getAnnotations(layerId))
	    {
	       if (annotation.getAncestors().contains(definingAnnotation))
	       {
		  // add the anchors
		  fragment.addAnchor((Anchor)annotation.getStart().clone());
		  fragment.addAnchor((Anchor)annotation.getEnd().clone());
		  // add the annotation
		  fragment.addAnnotation((Annotation)annotation.clone());
	       }
	    } // next annotation
	 } // layer exists
      } // next layer

      // now that we have the temporal slice, add missing ancestors (but not their anchors)
      Vector<Annotation> annotations = new Vector<Annotation>(fragment.getAnnotationsById().values());
      for (Annotation fragAnnotation : annotations)
      {
	 Annotation graphAnnotation = getAnnotation(fragAnnotation.getId());
	 for (Annotation ancestor : graphAnnotation.getAncestors())
	 {
	    if (!ancestor.getLayerId().equals("graph")
		&& !fragment.getAnnotationsById().containsKey(ancestor.getId()))
	    {
	       if (!fragment.schema.getLayers().containsKey(ancestor.getLayerId()))
	       { // add the ancestor's layer
		  fragment.addLayer(getLayer(ancestor.getLayerId()));
	       }
	       // add the ancestor
	       fragment.addAnnotation((Annotation)ancestor.clone());
	    }
	 }
      } // next annotation
      Anchor firstAnchor = fragment.getStart();
      if (getId() != null && firstAnchor != null) 
	 // (don't need to check for lastAnchor: if there's a first, there's a last)
      {
	 Anchor lastAnchor = fragment.getEnd();
	 fragment.setId(getId() 
			+ "__" + offsetFormat.format(firstAnchor.getOffset()) 
			+ "-" + offsetFormat.format(lastAnchor.getOffset()));
      }
      return fragment;
   } // end of getFragment()

   /**
    * Creates a fragment of this graph, copying into it annotations that fall within the bounds of the given <var>bounds</var> annotation, 
    * and which are descendants of the given <var>ancestor</var> annotation, 
    * on the given layers. 
    * Ancestors which do not fall into the interval are also added to the fragment, so that turns/speakers/etc. are accessible, but their anchors are not added to the fragment.
    * <p>The ID of the new fragment is <var>graphId</var>__<var>fragment.start.offset</var>-<var>fragment.end.offset</var>
    * @param bounds Annotation that defines the offset bounds (start/end time) of the included annotations
    * @param ancestor Annotation that is an ancestor of all annotations to be included.
    * @param layerIds A list of IDs of layers to include in the fragment.
    * @return A graph fragment containing the annotations that fall within the given bounds, on the given layers.
    */
   public Graph getFragment(Annotation bounds, Annotation ancestor, List<String> layerIds)
   {
      Graph fragment = new Graph();
      fragment.graph = this;
      if (bounds.getAnchored())
      {
	 double startOffset = bounds.getStart().getOffset();
	 double endOffset = bounds.getEnd().getOffset();
	 for (String layerId : layerIds)
	 {
	    Layer layer = getLayer(layerId);
	    if (layer != null)
	    {
	       fragment.addLayer((Layer)layer.clone());
	       for (Annotation annotation : getAnnotations(layerId))
	       {
		  Double min = annotation.getStart().getOffsetMin();
		  Double max = annotation.getEnd().getOffsetMax();
		  if (min != null && min >= startOffset
		      && max != null && max <= endOffset
		      && annotation.getAncestors().contains(ancestor))
		  {
		     // add the anchors
		     fragment.addAnchor((Anchor)annotation.getStart().clone());
		     fragment.addAnchor((Anchor)annotation.getEnd().clone());
		     // add the annotation
		     fragment.addAnnotation((Annotation)annotation.clone());
		  }
	       } // next annotation
	    } // layer exists
	 } // next layer
	 
	 // now that we have the temporal slice, add missing ancestors (but not their anchors)
	 Vector<Annotation> annotations = new Vector<Annotation>(fragment.getAnnotationsById().values());
	 for (Annotation fragAnnotation : annotations)
	 {
	    Annotation graphAnnotation = getAnnotation(fragAnnotation.getId());
	    for (Annotation anc : graphAnnotation.getAncestors())
	    {
	       if (!anc.getLayerId().equals("graph")
		   && !fragment.getAnnotationsById().containsKey(anc.getId()))
	       {
		  if (!fragment.schema.getLayers().containsKey(anc.getLayerId()))
		  { // add the ancestor's layer
		     fragment.addLayer((Layer)getLayer(anc.getLayerId()).clone());
		  }
		  // add the ancestor
		  fragment.addAnnotation((Annotation)anc.clone());
	       }
	    }
	 } // next annotation
	 Anchor firstAnchor = fragment.getStart();
	 if (getId() != null && firstAnchor != null) 
	    // (don't need to check for lastAnchor: if there's a first, there's a last)
	 {
	    Anchor lastAnchor = fragment.getEnd();
	    fragment.setId(getId() 
			   + "__" + offsetFormat.format(firstAnchor.getOffset()) 
			   + "-" + offsetFormat.format(lastAnchor.getOffset()));
	 }
      }
      return fragment;
   } // end of getFragment()

   
   /**
    * Retrieves an annotation given an id.
    * @param id ID of the annotation.
    * @return The identified annotation, if it's in the graph, and null otherwise.
    */
   public Annotation getAnnotation(String id)
   {
      Annotation annotation = getAnnotationsById().get(id);
      if (annotation == null && getId() != null && getId().equals(id))
      { // special case - a top-level annotation is looking for its parent, which is the graph itself
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
   public Annotation addAnnotation(Annotation annotation)
   {
      if (annotation.getId() == null)
      {
	 annotation.create();
	 annotation.setId(Long.toString(++lastId, Character.MAX_RADIX));
	 
	 // does the annotation have children?
	 if (annotation.getAnnotations().size() > 0)
	 {
	    // set the parentId for all children, now their parent has an ID
	    for (Vector<Annotation> children : annotation.getAnnotations().values())
	    {
	       for (Annotation child : children)
	       {
		  child.setParentId(annotation.getId());
	       }
	    } // next child layer
	 }
      }
      // set graph after the id is definitely set
      annotation.setGraph(this);

      // add to annotations collection
      getAnnotationsById().put(annotation.getId(), annotation);

      // add to the layer's collection
      annotation.setLayer(getLayer(annotation.getLayerId()));

      // add to the parent's collection
      if (annotation.getParent() != null)
      { // this ensures it's in the parent's child collection
	 annotation.setParent(annotation.getParent());
      }	 
      // find any children that might have already been added
      if (annotation.getLayer() != null)
      {
	 Layer layer = annotation.getLayer();
	 for (Layer childLayer : layer.getChildren().values())
	 {
	    SortedSet<Annotation> newChildren = new TreeSet<Annotation>();
	    // gather up new children
	    for (Annotation otherAnnotation : childLayer.getAnnotations())
	    {
	       if (otherAnnotation.getParentId() != null
		   && otherAnnotation.getParentId().equals(annotation.getId()))
	       {
		  newChildren.add(otherAnnotation);
	       }
	    } // next possible child annotation
	    // add them in order
	    for (Annotation child : newChildren)
	    {
	       child.setParent(annotation);
	    } // next child
	 } // next child layer

	 // also set the parent if it's a child of "graph"
	 if (layer.getParentId().equals("graph"))
	 {
	    annotation.setParent(this);
	 }
      } 

      // check anchors
      if (annotation.getLayer() == null // we don't know what the alignment should be
	  || annotation.getLayer().getAlignment() != Constants.ALIGNMENT_NONE) // or it's aligned
      { // should have an anchor
	 if (annotation.getStartId() == null)
	 { // no anchor, so create one TODO test this behaviour
	    if (lastAddedAnchorId != null)
	    {
	       annotation.setStart(getAnchor(lastAddedAnchorId));
	    }
	    else
	    { // there was no last end anchor, so create a new anchor
	       annotation.setStart(addAnchor(new Anchor()));
	    }
	 }
	 else if (annotation.getStart() == null)
	 { // there's a startId, but it references an anchor we don't yet know about
	    if (!unknownStartAnchor.containsKey(annotation.getStartId()))
	    {
	       unknownStartAnchor.put(annotation.getStartId(), new Vector<Annotation>());
	    }
	    unknownStartAnchor.get(annotation.getStartId()).add(annotation);
	 }
	 if (annotation.getEndId() == null)
	 { // no anchor, so create one TODO test this behaviour
	    annotation.setEnd(addAnchor(new Anchor()));
	 }
	 else if (annotation.getEnd() == null)
	 { // there's a endId, but it references an anchor we don't yet know about
	    if (!unknownEndAnchor.containsKey(annotation.getEndId()))
	    {
	       unknownEndAnchor.put(annotation.getEndId(), new Vector<Annotation>());
	    }
	    unknownEndAnchor.get(annotation.getEndId()).add(annotation);
	 }
	 lastAddedAnchorId = annotation.getEndId();
      }
      return annotation;
   } // end of addAnnotation()

   /**
    * Adds an anchor to the graph.
    * @param anchor The anchor to add to the graph.
    * @return The anchor.
    */
   public Anchor addAnchor(Anchor anchor)
   {
      anchor.setGraph(this);
      if (anchor.getId() == null)
      {
	 anchor.create();
	 anchor.setId("" + (++lastId));
      }

      // add to anchors collection
      getAnchors().put(anchor.getId(), anchor);

      // look for annotations referencing that anchor
      String id = anchor.getId();
      if (unknownStartAnchor.containsKey(id))
      {
	 for (Annotation annotation : unknownStartAnchor.get(id))
	 {
	    if (annotation.getStartId() != null && annotation.getStartId().equals(id))
	    { // it still references this anchor
	       annotation.setStart(anchor);
	    }
	 } // next annotation
	 unknownStartAnchor.remove(id);
      }
      if (unknownEndAnchor.containsKey(id))
      {
	 for (Annotation annotation : unknownEndAnchor.get(id))
	 {
	    if (annotation.getEndId() != null && annotation.getEndId().equals(id))
	    { // it still references this anchor
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
   public Anchor getAnchor(String id)
   {
      return getAnchors().get(id);
   } // end of getAnchor()

   
   /**
    * Gets an anchor at the given offset.
    * @param offset The anchor offset.
    * @return An anchor that has the given offset, or null if there isn't one in the graph.
    * @see #getOrCreateAnchorAt(double)
    */
   public Anchor getAnchorAt(double offset) // TODO test
   {
      for (Anchor anchor : getAnchors().values())
      {
	 if (anchor.getOffset() != null && anchor.getOffset().doubleValue() == offset)
	 {
	    return anchor;
	 }
      }
      return null;
   } // end of getAnchorAt()

   /**
    * Gets an anchor at the given offset. If there isn't already one in the graph, one is created.
    * @param offset The anchor offset.
    * @return An anchor that has the given offset.
    * @see #getAnchorAt(double)
    */
   public Anchor getOrCreateAnchorAt(double offset) // TODO test
   {
      Anchor anchor = getAnchorAt(offset);
      if (anchor == null)
      {
	 anchor = new Anchor();
	 anchor.setOffset(offset);
	 addAnchor(anchor);
      }
      return anchor;
   } // end of getAnchorAt()

   /**
    * Gets an anchor at the given offset. If there isn't already one in the graph, one is created.
    * <p>This convenience method allows, for example, the creation of an anchor with
    * a confidence value in one step:
    * <pre>
    * Anchor anchor = graph.getOrCreateAnchorAt(456.789, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC);
    * </pre>
    * @param offset The anchor offset.
    * @param key An attribute to set the value of.
    * @param value The value of the attribute identified by <var>key</var>
    * @return An anchor that has the given offset.
    * @see #getAnchorAt(double)
    */
   public Anchor getOrCreateAnchorAt(double offset, String key, Object value) // TODO test
   {
      Anchor anchor = getAnchorAt(offset);
      if (anchor == null)
      {
	 anchor = new Anchor();
	 anchor.setOffset(offset);
	 anchor.put(key, value);
	 addAnchor(anchor);
      }
      return anchor;
   } // end of getAnchorAt()

   /**
    * Returns the anchors sorted by offset. This includes only anchors for which the offset is actually set.
    * @return The anchors sorted by offset.
    */
   public SortedSet<Anchor> getSortedAnchors()
   {
      TreeSet<Anchor> sortedAnchors = new TreeSet<Anchor>();
      for (Anchor a : getAnchors().values())
      {
	 if (a.getOffset() != null)
	 {
	    sortedAnchors.add(a);
	 }
      }
      return sortedAnchors;
   } // end of getSortedAnchors()

   
   /**
    * Returns all anchors, in the best order given their offset and annotation links. This ordering requires much graph traversal to find the best comparison between anchors, so is computationally expensive and should be used sparingly.
    * @return An ordered set of all anchors in the graph.
    */
   public SortedSet<Anchor> getAnchorsOrderedByStructure()
   {
      TreeSet<Anchor> sortedAnchors = new TreeSet<Anchor>(new AnchorComparatorWithStructure());
      sortedAnchors.addAll(getAnchors().values());
      return sortedAnchors;
   } // end of getAnchorsOrderedByStructure()


   
   /**
    * Adds a layer definition.
    * @param layer The layer to add.
    */
   public void addLayer(Layer layer)
   {
      getSchema().addLayer(layer);
      annotations.put(layer.getId(), layer.getAnnotations());
   } // end of addLayer()

   
   /**
    * Get the definition of the given layer ID.
    * @param layerId The given layer ID.
    * @return The definition of the given layer ID.
    */
   public Layer getLayer(String layerId)
   {
      return getSchema().getLayer(layerId);
   } // end of getLayer()

   
   /**
    * Get a list of layers, ordered top down.
    * @return A list of layers, ordered top down.
    */
   public Vector<Layer> getLayersTopDown()
   {
      LayerHierarchyTraversal<Vector<Layer>> topDown = new LayerHierarchyTraversal<Vector<Layer>>(
	 new Vector<Layer>(), getSchema())
	 {
	    protected void pre(Layer layer)
	    {
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
   public String[] labels(String layerId)
   {
      Vector<Annotation> annotations = getAnnotations(layerId);
      String[] labels = new String[annotations.size()];
      for (int i = 0; i < annotations.size(); i++)
      {
	 labels[i] = annotations.elementAt(i).getLabel();
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
    * @param toTag The annotation to tag.
    * @param layerId The tag layer ID.
    * @param label The tag label.
    * @return The tag annotation created.
    */
   public Annotation createTag(Annotation toTag, String layerId, String label)
   {
      return toTag.createTag(layerId, label);
   } // end of createTag()

   /**
    * Creates a spanning annotation from the beginning of the start annotation to the ending of the end annotation.
    * @param from The first annotation to span.
    * @param to The last annotation to span.
    * @param layerId The layer ID of the resulting annotation.
    * @param label The label of the resulting annotation.
    * @param parent The new annotation's parent.
    * @return The new annotation.
    */
   public Annotation createSpan(Annotation from, Annotation to, String layerId, String label, Annotation parent)
   {
      Annotation span = new Annotation(null, label, layerId, from.getStartId(), to.getEndId(), parent.getId());
      getGraph().addAnnotation(span);
      return span;
   } // end of createSpan()

   
   /**
    * Creates a spanning annotation from the beginning of the start annotation to the ending of the end annotation. The parent annotation is guessed, if possible, from <var>from</var> or <var>to</var>
    * @param from The first annotation to span.
    * @param to The last annotation to span.
    * @param layerId The layer ID of the resulting annotation.
    * @param label The label of the resulting annotation.
    * @return The new annotation.
    */
   public Annotation createSpan(Annotation from, Annotation to, String layerId, String label)
   {
      Layer spanLayer = getLayer(layerId);
      Annotation parent = null;
      if (spanLayer.getParentId().equals(from.getLayer().getParentId()))
      { // "from" layer and span layer share a parent
	 parent = from.getParent();
      }
      else if (!from.getParentId().equals(to.getParentId()) 
	       && spanLayer.getParentId().equals(to.getLayer().getParentId()))
      { // "to" layer and span layer share a parent
	 parent = to.getParent();
      }
      else if (spanLayer.getParentId().equals(from.getLayerId()))
      { // unusual but possible case - the span should be the child of "from"
	 parent = from;
      }
      else
      {
	 // look for a parent in the ancestors of the annotations
	 LinkedHashSet<Annotation> ancestors = from.getAncestors();
	 if (!from.getParentId().equals(to.getParentId()))
	 {
	    ancestors.addAll(to.getAncestors());
	 }
	 for (Annotation ancestor : ancestors)
	 {
	    if (ancestor.getLayerId().equals(spanLayer.getParentId()))
	    {
	       parent = ancestor;
	       break;
	    }
	 } // next ancestor
      }
      return createSpan(from, to, layerId, label, parent);
   } // end of createSpan()


   // Annotation overrides

   /**
    * Setter for <i>id</i>: The annotation's identifier.
    * @param id The annotation's identifier.
    */
   public void setId(String id) 
   { 
      put("id", id); 
   }

   /**
    * Getter for <i>startId</i>: ID of the anchor with the lowest offset.
    * @return ID of the anchor with the lowest offset.
    */
   public String getStartId() 
   {
      Anchor earliest = null;
      for (Anchor a : getAnchors().values())
      {
	 if (a.getOffset() != null)
	 {
	    if (earliest == null || a.compareTo(earliest) < 0)
	    {
	       earliest = a;
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
   public String getEndId()
   {
      Anchor latest = null;
      for (Anchor a : getAnchors().values())
      {
	 if (a.getOffset() != null)
	 {
	    if (latest == null || a.compareTo(latest) > 0)
	    {
	       latest = a;
	    }
	 }
      }
      if (latest == null) return null;
      return latest.getId();
   }

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
    * Access the child annotations on a given layer.
    * <p>If this is a top-level layer, this collection is also accessible in the Annotation's map with a key named after <var>layerId</var> - e.g. this.annotations("turn") == this.get("turn"). The only exception is when <var>layerId is a reserved word - i.e. "id" or one of the keys registered in {@link #getTrackedAttributes()}</var>
    * @param layerId The given layer ID.
    * @return The child annotations on the given layer.
    */
   public Vector<Annotation> getAnnotations(String layerId)
   {
      if (layerId.equals("graph"))
      { // special case
	 Vector<Annotation> annotations = new Vector<Annotation>();
	 annotations.add(this);
	 return annotations;
      }
      Layer layer = getLayer(layerId);
      if (layer == null) return null;
      return layer.getAnnotations();
   } // end of getAnnotations()

   /**
    * Gets a list of related annotations on the given layer.
    * @param layerId The layer of the desired annotations.
    * @return The related annotations, or an empty array if none could be found on the given layer.
    */
   public Annotation[] list(final String layerId)
   {
      return annotations(layerId);
   }

   // TrackedMap methods

   /**
    * Commits object's changes, if any.  The effect of this is to commit all anchors and annotations.
    */
   public void commit()
   {
      super.commit();

      // anchors
      Iterator<Anchor> iAnchor = getAnchors().values().iterator();
      while (iAnchor.hasNext())
      {
	 Anchor a = iAnchor.next();
	 if (a.getChange() == Change.Operation.Destroy)
	 {
	    iAnchor.remove();
	 }
	 else
	 {
	    a.commit();
	 }
      } // next 

      // annotations
      Iterator<Annotation> iAnnotation = getAnnotationsById().values().iterator();
      while (iAnnotation.hasNext())
      {
	 Annotation a = iAnnotation.next();
	 if (a.getChange() == Change.Operation.Destroy)
	 {
	    a.setParent(null); // remove reference from parent
	    a.getLayer().getAnnotations().remove(a); // remove reference from layer
	    a.setLayer(null);
	    a.setStartId(null); // remove reference from start anchor
	    a.setEndId(null); // remove reference from end anchor
	    iAnnotation.remove();
	 }
	 else
	 {
	    a.commit();
	 }
      } // next 
   }
   /**
    * Rolls back changes since the object was create or {@link #commit()} was last called. The effect of this is to rollback all anchors and annotations.
    * @see #getTrackedAttributes()
    */
   public void rollback()
   {
      super.rollback();
      // anchors
      Iterator<Anchor> iAnchor = getAnchors().values().iterator();
      while (iAnchor.hasNext())
      {
	 Anchor a = iAnchor.next();
	 if (a.getChange() == Change.Operation.Create)
	 {
	    iAnchor.remove();
	 }
	 else
	 {
	    a.rollback();
	 }
      } // next 

      // annotations
      Iterator<Annotation> iAnnotation = getAnnotationsById().values().iterator();
      while (iAnnotation.hasNext())
      {
	 Annotation a = iAnnotation.next();
	 if (a.getChange() == Change.Operation.Create)
	 {
	    a.setParent(null);
	    a.setLayer(null);
	    iAnnotation.remove();
	 }
	 else
	 {
	    a.rollback();
	 }
      } // next 
   } // end of rollback()

   /**
    * Determines how the object has changed since it was originally defined, or since {@link #commit()} was last called.
    * @return How/whether the object has been changed.
    * @see #getTrackedAttributes()
    */
   public Change.Operation getChange()
   {
      Change.Operation operation = super.getChange();
      if (operation == Change.Operation.NoChange)
      { // check for changes to anchors or annotations
	 for (Anchor a : getAnchors().values())
	 {
	    if (a.getChange() != Change.Operation.NoChange)
	    { // something has changed so the graph change is Update
	       return Change.Operation.Update;
	    }
	 } // next anchor
	 // all annotations must be created
	 for (Annotation a : getAnnotationsById().values())
	 {
	    if (a.getChange() != Change.Operation.NoChange)
	    { // something has changed so the graph change is Update
	       return Change.Operation.Update;
	    }
	 } // next anchor
      }
      return operation;
   } // end of getChange()   

   /**
    * Produces a list of individual changes for this graph and its elements.
    * @return A list of individual changes for the object.
    */
   public Vector<Change> getChanges()
   {
      Vector<Change> changes = new Vector<Change>(); // start with graph changes
      if (super.getChange() == Change.Operation.Create)
      { 
	 // graph create before creating anchors/annotations
	 changes.addAll(super.getChanges());
	 // all anchors must be created
	 for (Anchor a : getAnchors().values())
	 {
	    a.create();
	    changes.addAll(a.getChanges());
	 } // next anchor
	 // all annotations must be created
	 LayerTraversal<Vector<Change>> createTraversal = new LayerTraversal<Vector<Change>>(changes, this, true)
	    {
	       protected void pre(Annotation a) // parents before children
	       {
		  a.create();
		  result.addAll(a.getChanges());
	       }
	       protected void except(Annotation a) { pre(a); }
	    };
      } // Create
      else if (super.getChange() == Change.Operation.Destroy)
      {
	 // all annotations must be deleted before anchors are deleted
	 LayerTraversal<Vector<Change>> deleteTraversal = new LayerTraversal<Vector<Change>>(changes, this, true)
	    {
	       protected void post(Annotation a) // parents after children
	       {
		  a.destroy();
		  result.addAll(a.getChanges());
	       }
	       protected void except(Annotation a) { post(a); }
	    };
	 // all anchors must be deleted
	 for (Anchor a : getAnchors().values())
	 {
	    a.destroy();
	    changes.addAll(a.getChanges());
	 } // next anchor
	 // graph delete after deleting anchors/annotations
	 changes.addAll(super.getChanges()); 
      }
      else
      { // not creating or deleting the graph
	 changes.addAll(super.getChanges());
	 // add anchor changes
	 for (Anchor a : getAnchors().values())
	 {
	    if (a.getChange() != Change.Operation.NoChange)
	    {
	       changes.addAll(a.getChanges());
	    }
	 } // next anchor

	 // add annotation changes
	 
	 // ensure all possible parents are created before their children
	 LayerTraversal<Vector<Change>> createTraversal = new LayerTraversal<Vector<Change>>(changes, this, true)
	    {
	       protected void pre(Annotation a) // parents created (and updated) before children
	       {
		  if (a.getChange() == Change.Operation.Create)
		  {
		     result.addAll(a.getChanges());
		  }
	       }
	       protected void except(Annotation a) { pre(a); }
	    };

	 // perform updates after creates and before deletes
	 LayerTraversal<Vector<Change>> updateTraversal = new LayerTraversal<Vector<Change>>(changes, this, true)
	    {
	       protected void pre(Annotation a) // parents created (and updated) before children
	       {
		  if (a.getChange() == Change.Operation.Update)
		  {
		     result.addAll(a.getChanges());
		  }
	       }
	       protected void except(Annotation a) { pre(a); }
	    };

	 // now perform deletes, children first
	 LayerTraversal<Vector<Change>> deleteTraversal = new LayerTraversal<Vector<Change>>(changes, this, true)
	    {
	       protected void post(Annotation a) // parents deleted after children
	       {
		  if (a.getChange() == Change.Operation.Destroy)
		  {
		     result.addAll(a.getChanges());
		  }
	       }
	       protected void except(Annotation a) { post(a); }
	    };
      }
      return changes;
   } // end of getChanges()

} // end of class Graph
