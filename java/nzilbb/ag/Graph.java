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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Vector;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Locale;
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
 * <p>In addition to this, the graphy also has:
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
    * A map of layer definitions, keyed on layerId.
    * @see #getLayers()
    * @see #setLayers(LinkedHashMap)
    */
   protected LinkedHashMap<String,Layer> layers = new LinkedHashMap<String,Layer>();
   /**
    * Getter for {@link #layers}: A map of layer definitions, keyed on layerId.
    * @return A map of layer definitions, keyed on layerId.
    */
   public LinkedHashMap<String,Layer> getLayers() { return layers; }
   /**
    * Setter for {@link #layers}: A map of layer definitions, keyed on layerId.
    * @param newLayers A map of layer definitions, keyed on layerId.
    */
   public void setLayers(LinkedHashMap<String,Layer> newLayers) { layers = newLayers; }   
   
   // Methods:
      
   /**
    * Default constructor.
    */
   public Graph()
   {
      graph = this;
      // create the top-level 'whole graph' layer that is the descendant of all actual annotation layers
      addLayer(new Layer("graph", "The graph as a whole", 2, false, false, true));
      setLayerId("graph");
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
	       if (!fragment.layers.containsKey(ancestor.getLayerId()))
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
	       if (!fragment.layers.containsKey(ancestor.getLayerId()))
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
		  if (!fragment.layers.containsKey(anc.getLayerId()))
		  { // add the ancestor's layer
		     fragment.addLayer(getLayer(anc.getLayerId()));
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
    * @param annotation
    */
   public void addAnnotation(Annotation annotation)
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

      // all annotations are taken to be 'children' of the whole graph
      getAnnotations(annotation.getLayerId()).add(annotation);

      if (annotation.getLayer() != null && annotation.getLayer().getParentId().equals("graph"))
      {
	 annotation.setParentId(getId());
      }
      else
      {
	 // add to the parent's collection
	 if (annotation.getParent() != null)
	 { // this ensures it's in the parent's child collection
	    annotation.setParent(annotation.getParent());
	 }	 
      }
      // find any children that might have already been added
      if (annotation.getLayer() != null)
      {
	 for (Layer childLayer : annotation.getLayer().getChildren().values())
	 {
	    SortedSet<Annotation> newChildren = new TreeSet<Annotation>();
	    // gather up new children
	    for (Annotation otherAnnotation : getAnnotations(childLayer.getId()))
	    {
	       if (otherAnnotation.getParentId().equals(annotation.getId()))
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
      } 
   } // end of addAnnotation()

   /**
    * Adds an anchor to the graph.
    * @param anchor
    */
   public void addAnchor(Anchor anchor)
   {
      anchor.setGraph(this);
      if (anchor.getId() == null)
      {
	 anchor.create();
	 anchor.setId("" + (++lastId));
      }

      // add to anchors collection
      getAnchors().put(anchor.getId(), anchor);
   } // end of addAnnotation()

   /**
    * Retrieves an anchor given an id.
    * @param id
    * @return The identified anchor, if it's in the graph, and null otherwise.
    */
   public Anchor getAnchor(String id)
   {
      return getAnchors().get(id);
   } // end of getAnchor()

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
    * @param layer
    */
   @SuppressWarnings("unchecked")
   public void addLayer(Layer layer)
   {
      getLayers().put(layer.getId(), layer);
      if (layer.getParentId() != null)
      {
	 layer.setParent(getLayer(layer.getParentId()));
      }
      else
      { // top level layer
	 // add it to layers.children
	 if (!containsKey("layers")) put("layers", new LinkedHashMap<String,Object>());
	 LinkedHashMap<String,Object> layers = (LinkedHashMap<String,Object>)get("layers");
	 if (!layers.containsKey("children")) layers.put("children", new LinkedHashMap<String, Layer>());
	 LinkedHashMap<String, Layer> children = (LinkedHashMap<String, Layer>)layers.get("children");
	 children.put(layer.getId(), layer);
      }

      // now check whether any child layers have already been added, and ensure they are added as children
      for (Layer otherLayer : getLayers().values())
      {
	 if (otherLayer.getParentId() != null
	     && otherLayer.getParentId().equals(layer.getId()))
	 {
	    otherLayer.setParent(layer);
	 }
      }
   } // end of addLayer()

   
   /**
    * Get the definition of the given layer id.
    * @param layerId
    * @return The definition of the given layer id.
    */
   public Layer getLayer(String layerId)
   {
      return getLayers().get(layerId);
   } // end of getLayer()

   
   /**
    * Get a list of layers, ordered top down.
    * @return A list of layers, ordered top down.
    */
   public Vector<Layer> getLayersTopDown()
   {
      LayerHierarchyTraversal<Vector<Layer>> topDown = new LayerHierarchyTraversal<Vector<Layer>>(
	 new Vector<Layer>(), this)
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
    * @param layerId
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
    * @param toTag
    * @param layerId
    * @param label
    * @return The tag annotation created.
    */
   public Annotation createTag(Annotation toTag, String layerId, String label)
   {
      return toTag.createTag(layerId, label);
   } // end of createTag()

   /**
    * Creates a spanning annotation from the beginning of the start annotation to the ending of the end annotation.
    * @param from
    * @param to
    * @param layerId
    * @param label
    * @param parent
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
    * @param from
    * @param to
    * @param layerId
    * @param label
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
    * Getter for <i>startId</i>: ID of the anchor with the lowest offset.
    * @return ID of the anchor with the lowest offset.
    */
   public String getStartId() { if (getAnchors().size() > 0) { return getSortedAnchors().first().getId(); } else { return null; } }

   /**
    * Getter for <i>endId</i>: ID of the anchor with the highest offset.
    * @return ID of the anchor with the highest offset.
    */
   public String getEndId() { if (getAnchors().size() > 0) { return getSortedAnchors().last().getId(); } else { return null; } }

   /**
    * Access the child annotations on a given layer.
    * <p>If this is a top-level layer, this collection is also accessible in the Annotation's map with a key named after <var>layerId</var> - e.g. this.annotations("turn") == this.get("turn"). The only exception is when <var>layerId is a reserved word - i.e. "id" or one of the keys registered in {@link #getTrackedAttributes()}</var>
    * @param layerId
    * @return The child annotations on the given layer.
    */
   public Vector<Annotation> getAnnotations(String layerId)
   {
      if (!getAnnotations().containsKey(layerId))
      {
	 // add the child collection to children
	 getAnnotations().put(layerId, new Vector<Annotation>());
	 // also create an attribute named after the layer, as long as it's not otherwise in use
	 if (!layerId.equals("id") && !getTrackedAttributes().contains(layerId))
	 {
	    // and only if it's a top-level layer
	    if (getLayer(layerId) != null && getLayerId().equals(getLayer(layerId).getParentId()))
	    {
	       put(layerId, getAnnotations().get(layerId));
	    }
	 }
      }
      return getAnnotations().get(layerId);
   } // end of getAnnotations()

   // TrackedMap methods

   /**
    * Commits object's changes, if any.  The effect of this is to commit all anchors and annotations.
    */
   public void commit()
   {
      super.commit();
      for (Anchor a : getAnchors().values()) a.commit();
      for (Annotation a : getAnnotationsById().values()) a.commit();
   }
   /**
    * Rolls back changes since the object was create or {@link #commit()} was last called. The effect of this is to rollback all anchors and annotations.
    * @see #getTrackedAttributes()
    */
   public void rollback()
   {
      super.rollback();
      for (Anchor a : getAnchors().values()) a.rollback();
      for (Annotation a : getAnnotationsById().values()) a.rollback();
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
