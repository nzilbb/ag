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

import java.util.Collection;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Vector;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.LinkedHashSet;
import java.util.SortedSet;
import java.util.Iterator;
import nzilbb.ag.util.LayerTraversal;
import nzilbb.ag.util.AnnotationComparatorByOrdinal;
import nzilbb.ag.util.AnnotationComparatorByAnchor;
/**
 * Annotation graph annotation - i.e. an edge of the graph.
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class Annotation
   extends TrackedMap
   implements Comparable<Annotation>
{
   // NB if this is updated, please also update the @return javadoc attribute on getTrackedAttributes()
   private static String[] aTrackedAttributes = {"label", "startId", "endId", "parentId", "ordinal"};
   /**
    * Keys for attributes that are change-tracked - i.e. when a new value is set for any of these attributes, the original value is saved in the map with the given key prefixed by "original" - e.g. if "label" is changed, then "originalLabel" will contain its original value.
    * <p>LinkedHashSet is used so that attributes are iterated in the order they're defined in aTrackedAttributes (which is the order shown in the documentation of {@link #getTrackedAttributes()}).
    */
   protected static final Set<String> trackedAttributes = new LinkedHashSet<String>(java.util.Arrays.asList(aTrackedAttributes));

   /**
    * Keys for attributes that are change-tracked - i.e. when a new value is set for any of these attributes, the original value is saved in the map with the given key prefixed by "original" - e.g. if "label" is changed, then "originalLabel" will contain its original value.
    * @return "label", "startId", "endId", "parentId", "ordinal"
    */
   public Set<String> getTrackedAttributes() { return trackedAttributes; }

   // NB if this is updated, please also update the @return javadoc attribute on getClonedAttributes()
   private static String[] aClonedAttributes = {"id", "layerId", "label", "startId", "endId", "parentId", "ordinal", "confidence"};
   /**
    * Keys for attributes that are cloned - i.e. when an object is cloned, only these attributes are copied into the clone.
    * <p>LinkedHashSet is used so that attributes are iterated in the order they're defined in aClonedAttributes (which is the order shown in the documentation of {@link #getClonedAttributes()}).
    */
   protected static final Set<String> clonedAttributes = new LinkedHashSet<String>(java.util.Arrays.asList(aClonedAttributes));

   /**
    * Keys for attributes that are cloned - i.e. when an object is cloned, only these attributes are copied into the clone.
    * @return "id", "layerId", "label", "startId", "endId", "parentId", "ordinal", "confidence"
    */
   public Set<String> getClonedAttributes() { return clonedAttributes; }

   // Attributes stored in HashMap:

   /**
    * The annotation's label.
    */
   protected String label;
   /**
    * Getter for <i>label</i>: The annotation's label.
    * @return The annotation's label.
    */
   public String getLabel() { return label; }
   /**
    * Setter for <i>label</i>: The annotation's label.
    * @param label The annotation's label.
    * @return A list of changes, which will be empty if the label is being set for the first time, or is already set to this value.
    */
   public Vector<Change> setLabel(String label) 
   { 
      Vector<Change> changes = new Vector<Change>();
      if (this.label != null && !this.label.equals(label))  // is it actually changing?
      {
	 changes.add(registerChange("label", label));
      }

      this.label = label; 
      return changes;
   }

   /**
    * The identifier of the annotation's layer.
    */
   protected String layerId;
   /**
    * Getter for <i>layerId</i>: The identifier of the annotation's layer.
    * @return The identifier of the annotation's layer.
    */
   public String getLayerId() { return layerId; }
   /**
    * Setter for <i>layerId</i>: The identifier of the annotation's layer.
    * @param layerId The identifier of the annotation's layer.
    */
   public void setLayerId(String layerId) { this.layerId = layerId; }

   /**
    * ID of the annotation's start anchor.
    */
   protected String startId;
   /**
    * Getter for <i>startId</i>: ID of the annotation's start anchor.
    * @return ID of the annotation's start anchor.
    */
   public String getStartId() 
   { 
      return startId; 
   }
   /**
    * Setter for <i>startId</i>: ID of the annotation's start anchor.
    * @param startId ID of the annotation's start anchor.
    * @return A list of changes, which will be empty if the startId is being set for the first time, or is already set to this value.
    */
   public synchronized Vector<Change> setStartId(String startId) 
   { 
      // unlink old start, if available
      Anchor start = getStart();
      if (start != null && !start.getId().equals(startId))
      {
	 start.startOf(getLayerId()).remove(this);
      }

      Vector<Change> changes = new Vector<Change>();
      if (this.startId != null && !this.startId.equals(startId))  // is it actually changing?
      {
	 changes.add(registerChange("startId", startId));
      }

      // set the ID
      this.startId = startId; 
      
      // introduce ourselves to the new anchor, if available
      start = getStart();
      if (start != null)
      {
	 start.startOf(getLayerId()).add(this);
	 // reset index for layer
	 graph.indicesByLayer.remove(getLayerId());
      }

      if (graph != null && !(this instanceof Graph))
      {
	 // check for unaligned children and set their startIds
	 for (String layerId : getAnnotations().keySet())
	 {
	    Layer layer = graph.getLayer(layerId);
	    if (layer == null) continue;
	    if (layer.getAlignment() == Constants.ALIGNMENT_NONE)
	    {
	       for (Annotation child : getAnnotations(layerId))
	       {
		  if (child.getChange() == Change.Operation.Destroy) continue;
		  changes.addAll( // register changes:
		     child.setStartId(startId));
	       } // next tag child
	    } // layer is not aligned
	 } // next layer
      } // we're in a graph
      
      return changes;
   }
   
   /**
    * ID of the annotation's end anchor.
    */
   protected String endId;
   /**
    * Getter for <i>endId</i>: ID of the annotation's end anchor.
    * @return ID of the annotation's end anchor.
    */
   public String getEndId() 
   {
      return endId;
   }
   /**
    * Setter for <i>endId</i>: ID of the annotation's end anchor.
    * @param endId ID of the annotation's end anchor.
    * @return A list of changes, which will be empty if the endId is being set for the first time, or is already set to this value.
    */
   public synchronized Vector<Change> setEndId(String endId) 
   { 
      // unlink old end, if available
      Anchor end = getEnd();
      if (end != null && !end.getId().equals(endId))
      {
	 end.endOf(getLayerId()).remove(this);
      }

      Vector<Change> changes = new Vector<Change>();
      if (this.endId != null && !this.endId.equals(endId))  // is it actually changing?
      {
	 changes.add(registerChange("endId", endId));
      }

      // set the ID
      this.endId = endId; 
      
      // introduce ourselves to the new anchor, if available
      end = getEnd();
      if (end != null)
      {
	 end.endOf(getLayerId()).add(this);
	 // reset index for layer
	 graph.indicesByLayer.remove(getLayerId());
      }

      // check for unaligned children and set their endIds
      if (graph != null && !(this instanceof Graph))
      {
	 // check for unaligned children and set their startIds
	 for (String layerId : getAnnotations().keySet())
	 {
	    Layer layer = graph.getLayer(layerId);
	    if (layer == null) continue;
	    if (layer.getAlignment() == Constants.ALIGNMENT_NONE)
	    {
	       for (Annotation child : getAnnotations(layerId))
	       {
		  if (child.getChange() == Change.Operation.Destroy) continue;
		  changes.addAll( // register changes:
		     child.setEndId(endId));
	       } // next tag child
	    } // layer is not aligned
	 } // next layer
      } // we're in a graph

      return changes;
   }
   
   /**
    * The annotation's parent annotation ID, if any.
    */
   protected String parentId;
   /**
    * Getter for <i>parentId</i>: The annotation's parent annotation ID, if any.
    * @return The annotation's parent annotation ID, if any.
    */
   public String getParentId() { return parentId; }
   /**
    * Setter for <i>parentId</i>: The annotation's parent annotation ID, if any.
    * @param parentId The annotation's parent annotation ID, if any.
    * @return A list of changes, which will be empty if the parentId is being set for the first time, or is already set to this value.
    */
   public synchronized Vector<Change> setParentId(String parentId) 
   { 
      Vector<Change> changes = new Vector<Change>();
      if (this.parentId != null && !this.parentId.equals(parentId))  // is it actually changing?
      {
	 changes.add(registerChange("parentId", parentId));
      }
      this.parentId = parentId;
      // if we're on an unaligned layer, set start/end Ids to match parent
      Layer layer = getLayer();
      Annotation parent = getParent();
      if (layer != null && parent != null
	  // not the top level, as Graph.getStartId()/endId() is costly, and it's not necessary
	  && !(parent instanceof Graph)
	  // unaligned (tag) layer
	  && layer.getAlignment() == Constants.ALIGNMENT_NONE)
      {
	 changes.addAll( // register changes:
	    setStartId(parent.getStartId()));
	 changes.addAll( // register changes:
	    setEndId(parent.getEndId()));
      } // layer is not aligned
      return changes;
   }
   
   /**
    * The annotation's ordinal position amongst the parent's children.  Ordinal is 1-based - i.e. the first child has ordinal = 1.
    */
   protected int ordinal = 0;
   /**
    * Getter for <i>ordinal</i>: The annotation's ordinal position amongst the parent's children.  Ordinal is 1-based - i.e. the first child has ordinal = 1.
    * @return The annotation's ordinal position amongst the parent's children.
    */
   public int getOrdinal() 
   { 
      int ordinalToReturn = 0;
      if (ordinal > 0)
      {
	 ordinalToReturn = ordinal;
      }
      else
      {
	 Annotation parent = getParent();
	 if (parent != null)
	 {
	    // get all peers before this one
	    SortedSet<Annotation> priorPeers = parent.getAnnotations(getLayerId()).headSet(this);
	    // weed out the deleted one
	    Iterator<Annotation> it = priorPeers.iterator();
	    while (it.hasNext()) 
	    {
	       Annotation p = it.next();
	       if (p.getChange() == Change.Operation.Destroy)
	       {
		  it.remove();
	       }
	    }
	    ordinalToReturn = priorPeers.size() + 1;
	    setOrdinal(ordinalToReturn);
	 }
      }
      return ordinalToReturn;
   }
   /**
    * Setter for <i>ordinal</i>: The annotation's ordinal position amongst the parent's children. Ordinal is 1-based - i.e. the first child has ordinal = 1.
    * @param ordinal The annotation's ordinal position amongst the parent's children.
    * @return A list of changes, which will be empty if the ordinal is being set for the first time, or is already set to this value.
    */
   public synchronized Vector<Change> setOrdinal(int ordinal) 
   { 
      Vector<Change> changes = new Vector<Change>();
      if (this.ordinal != ordinal)  // is it actually changing?
      {
	 if (this.ordinal != 0)
	 {
	    changes.add(registerChange("ordinal", new Integer(ordinal)));
	 }
	 this.ordinal = ordinal; 
	 Annotation parent = getParent();
	 if (parent != null)
	 {
	    changes.addAll( // record changes of:
	       correctOrdinals(parent.getAnnotations(getLayerId())));
	 }
      } // ordinal actually changing      
      return changes;
   }


   /**
    * Traverses the given list of annototations and ensures that the "ordinal" property corresponds to the index in the list.
    * @param peers
    * @return The resulting changes.
    */
   protected Vector<Change> correctOrdinals(SortedSet<Annotation> peers)
   {
      Vector<Change> changes = new Vector<Change>();
      if (peers.size() > 0) 
      {
	 int o = ordinalMinimum(peers.iterator().next().getLayerId());
	 for (Annotation peer : peers)
	 {
	    if (peer.getChange() == Change.Operation.Destroy) continue;
	    int originalOrdinal = peer.ordinal;
	    if (originalOrdinal == 0 || originalOrdinal != o)
	    {
	       changes.add(peer.registerChange("ordinal", o));
	       peer.ordinal = o; 
	    }
	    o++;
	 }
      }
      return changes;
   } // end of correctOrdinals()

   // Attributes stored outside HashMap, so that JSONifying the HashMap doesn't result in infinite recursion

   /**
    * Child annotations, keyed on layer id.
    */
   protected LinkedHashMap<String,SortedSet<Annotation>> annotations = new LinkedHashMap<String,SortedSet<Annotation>>();
   /**
    * Getter for <i>annotations</i>: Child annotations, keyed on layer id.
    * @return Child annotations, keyed on layer id.
    */
   public LinkedHashMap<String,SortedSet<Annotation>> getAnnotations() { return annotations; }
   
   /**
    * The minimum ordinals for each child layer, used for when a fragment includes only some of
    * the children of a parent (otherwise their child ordinals would be forced to start at 1).
    */
   protected HashMap<String,Integer> ordinalMinima = new HashMap<String,Integer>();
   
   /**
    * The annotation's annotation graph.
    * @see #getGraph()
    * @see #setGraph(Graph)
    */
   protected Graph graph;
   /**
    * Getter for {@link #graph}: The annotation's annotation graph.
    * @return The annotation's annotation graph.
    */
   public Graph getGraph() { return graph; }
   /**
    * Setter for {@link #graph}: The annotation's annotation graph.
    * @param newGraph The annotation's annotation graph.
    */
   public void setGraph(Graph newGraph) 
   { 
      graph = newGraph; 
      if (graph != null)
      {
	 // now we have a graph, we may be able to introduce ourselves to related objects
	 Layer layer = graph.getLayer(getLayerId());
	 if (layer != null)
	 {
	    setLayer(layer);
	 }
	 if (graph.getAnchors().containsKey(getStartId()))
	 {
	    setStart(graph.getAnchor(getStartId()));
	 }
	 if (graph.getAnchors().containsKey(getEndId()))
	 {
	    setEnd(graph.getAnchor(getEndId()));
	 }
	 // reset index for layer
	 graph.indicesByLayer.remove(getLayerId());
      }
   }
   
   /**
    * Getter for <var>parent</var>: The annotation's parent annotation, if any.
    * <em>NB:</em> An object will only be returned if {@link #setGraph(Graph)} has been called and the graph contains the parent annotation (as identified by by {@link #getParentId()}).
    * @return The annotation's parent annotation, if any.
    */
   public Annotation getParent() 
   {   
      if (graph != null)
      {
	 return graph.getAnnotation(getParentId());
      }
      else
      {
	 return null;
      }
   }
   /**
    * Setter for <var>parent</var>: The annotation's parent annotation, if any. A side-effect is that the annotation will be appended to the parent's collection of children.
    * @param newParent The annotation's parent annotation, if any.
    * @return A collection of resulting changes (which may be empty or may include an ordinal change)
    */
   public Vector<Change> setParent(Annotation newParent) 
   { 
      return setParent(newParent, true);
   }   

   /**
    * Setter for <var>parent</var>: The annotation's parent annotation, if any.
    * @param newParent The annotation's parent annotation, if any.
    * @param append true if the annotation should be added to the end of the parent's existing children, false if it should keep its current ordinal.
    * @return A collection of resulting changes (which may be empty or may include an ordinal change)
    */
   public Vector<Change> setParent(Annotation newParent, boolean append) 
   {
      Vector<Change> changes = new Vector<Change>();
      Annotation currentParent = getParent();
      if (currentParent != null && currentParent != newParent)
      {
	 if (currentParent.getAnnotations().containsKey(getLayerId()))
	 {
	    SortedSet<Annotation> currentSiblings = currentParent.getAnnotations().get(getLayerId());
	    // TODO instead of using remove, which is broken for some reason (dodgy comparator somehow??)
	    // TODO we iterate and remove ourselves	    
	    //currentSiblings.remove(this);
	    Iterator<Annotation> iCurrentSiblings = currentSiblings.iterator();
	    while (iCurrentSiblings.hasNext())
	    {
	       if (iCurrentSiblings.next() == this)
	       {
	    	  iCurrentSiblings.remove();
	    	  break;
	       }
	    } // next sibling
	 }
      }
      else if (currentParent == null && newParent != null)
      {
	 // no longer an orphan
	 if (graph != null && getLayerId() != null && graph.orphans.containsKey(getLayerId()))
	 {
	    graph.orphans.get(getLayerId()).remove(this);
	 }
      }
      if (newParent == null)
      {
	 // if it's a tag layer its anchors depend on the parent...
	 changes.addAll(
	    setParentId(null));
	 // now an orphan
	 if (graph != null && getLayerId() != null && graph.orphans.containsKey(getLayerId()))
	 {
	    graph.orphans.get(getLayerId()).add(this);
	 }
      }
      else
      {
	 changes.addAll(
	    setParentId(newParent.getId()));
	 if (!newParent.getAnnotations().containsKey(getLayerId()))
	 { // ensure the collection exists
	    newParent.getAnnotations(getLayerId());
	 } // but we use the real collection, not the copy returned by getAnnotations(String)...
	 SortedSet<Annotation> newSiblings = newParent.getAnnotations().get(getLayerId());
	 // if the collection isn't there yet, create and add it...
	 if (newSiblings == null) newSiblings = newParent.getAnnotations(getLayerId());
	 if (newSiblings != null && !newSiblings.contains(this))
	 {
	    newSiblings.add(this);
	    if (append || this.ordinal == 0)
	    {
	       changes.addAll(
		  setOrdinal(newSiblings.size() + ordinalMinimum(getLayerId()) - 1));
	    }
	 }
/* TODO this incurs a surprisingly huge performance hit 
	 Layer layer = getLayer();
	 if (layer != null && layer.getAlignment() == Constants.ALIGNMENT_NONE)
	 { // annotation must share anchors with parent
	    if (!getStartId().equals(newParent.getStartId()))
	    {
	       changes.addAll(
		  setStart(newParent.getStart()));
	    }
	    if (!getEndId().equals(newParent.getEndId()))
	    {
	       changes.addAll(
		  setEnd(newParent.getEnd()));
	    }
	 }
*/
      }      
      return changes;
   }   

   /**
    * Getter for <var>start</var>: The annotation's start anchor.
    * <em>NB:</em> An object will only be returned if {@link #setGraph(Graph)} has been called and the graph contains the anchor (as identified by by {@link #getStartId()}).
    * @return The annotation's start anchor.
    */
   public Anchor getStart() 
   { 
      if (graph != null)
      {
	 Layer layer = getLayer();
	 if (layer != null && layer.getAlignment() == Constants.ALIGNMENT_NONE)
	 { // tag layer - return parent's start anchor
	    Annotation parent = getParent();
	    if (parent != null) return parent.getStart();
	 }
	 return graph.getAnchor(getStartId());
      }
      return null;
   }

   /**
    * Setter for <var>start</var>: The annotation's start anchor.
    * @param start The annotation's start anchor.
    * @return A list of changes, which will be empty if the start anchor is being set for the first time, or is already set to this value.
    */
   public Vector<Change> setStart(Anchor start) 
   { 
      if (start == null) return new Vector<Change>(); // TODO test this behaviour
      return setStartId(start.getId());
   }

   /**
    * Getter for <var>end</var>: The annotation's end anchor.
    * <em>NB:</em> An object will only be returned if {@link #setGraph(Graph)} has been called and the graph contains the anchor (as identified by by {@link #getEndId()}).
    * @return The annotation's end anchor.
    */
   public Anchor getEnd() 
   { 
      if (graph != null)
      {
	 Layer layer = getLayer();
	 if (layer != null && layer.getAlignment() == Constants.ALIGNMENT_NONE)
	 { // tag layer - return parent's start anchor
	    Annotation parent = getParent();
	    if (parent != null) return parent.getEnd();
	 }
	 return graph.getAnchor(getEndId());
      }
      return null;
   }

   /**
    * Setter for <var>end</var>: The annotation's end anchor.
    * @param end The annotation's end anchor.
    * @return A list of changes, which will be empty if the end anchor is being set for the first time, or is already set to this value.
    */
   public Vector<Change> setEnd(Anchor end) 
   { 
      if (end == null) return new Vector<Change>(); // TODO text this behaviour
      return setEndId(end.getId());
   }
   
   /**
    * Getter for <i>layer</i>: The annotation's layer definition.
    * <em>NB:</em> An object will only be returned if {@link #setGraph(Graph)} has been called and the graph contains a definition for the annotation's layer (as returned by {@link #getLayerId()}).
    * @return The annotation's layer definition.
    */
   public Layer getLayer() 
   { 
      if (graph != null)
      {
	 return graph.getLayer(getLayerId());
      }
      else
      {
	 return null;
      }
   }
   /**
    * Setter for <i>layer</i>: The annotation's layer definition.
    * @param layer The annotation's layer definition.
    */
   public void setLayer(Layer layer) 
   { 
      if (layer != null)
      {
	 setLayerId(layer.getId());
      }
   }
   
   // Methods:
      
   /**
    * Default constructor
    */
   public Annotation()
   {
   } // end of constructor

   /**
    * Basic constructor.
    * @param id The annotation's identifier.
    * @param label The annotation's label.
    * @param layerId The identifier of the annotation's layer.
    */
   public Annotation(String id, String label, String layerId)
   {
      setId(id);
      setLabel(label);
      setLayerId(layerId);
   } // end of constructor

   /**
    * Basic constructor including anchor ids.
    * @param id The annotation's identifier.
    * @param label The annotation's label.
    * @param layerId The identifier of the annotation's layer.
    * @param startId ID of the annotation's start anchor.
    * @param endId ID of the annotation's end anchor.
    */
   public Annotation(String id, String label, String layerId, String startId, String endId)
   {
      setId(id);
      setLabel(label);
      setLayerId(layerId);
      setStartId(startId);
      setEndId(endId);
   } // end of constructor

   /**
    * Basic constructor including parent id.
    * @param id The annotation's identifier.
    * @param label The annotation's label.
    * @param layerId The identifier of the annotation's layer.
    * @param parentId The annotation's parent annotation ID.
    */
   public Annotation(String id, String label, String layerId, String parentId)
   {
      setId(id);
      setLabel(label);
      setLayerId(layerId);
      setParentId(parentId);
   } // end of constructor

   /**
    * Basic constructor including anchor and parent ids.
    * @param id The annotation's identifier.
    * @param label The annotation's label.
    * @param layerId The identifier of the annotation's layer.
    * @param startId ID of the annotation's start anchor.
    * @param endId ID of the annotation's end anchor.
    * @param parentId The annotation's parent annotation ID.
    */
   public Annotation(String id, String label, String layerId, String startId, String endId, String parentId)
   {
      setId(id);
      setLabel(label);
      setLayerId(layerId);
      setStartId(startId);
      setEndId(endId);
      setParentId(parentId);
   } // end of constructor

   /**
    * Basic constructor including anchor and parent ids and ordinal.
    * @param id The annotation's identifier.
    * @param label The annotation's label.
    * @param layerId The identifier of the annotation's layer.
    * @param startId ID of the annotation's start anchor.
    * @param endId ID of the annotation's end anchor.
    * @param parentId The annotation's parent annotation ID.
    * @param ordinal The annotation's ordinal position amongst the parent's children.
    */
   public Annotation(String id, String label, String layerId, String startId, String endId, String parentId, int ordinal)
   {
      setId(id);
      setLabel(label);
      setLayerId(layerId);
      setStartId(startId);
      setEndId(endId);
      setParentId(parentId);
      setOrdinal(ordinal);
   } // end of constructor

   /**
    * Copy constructor.  This copies all attributes of the anchor <em>except</em> <var>id</var>, tracked original values (e.g. <var>originalLabel</var>), and attributes whose keys do not begin with an alphanumeric (by convention these are transient attributes), the intention being to create a new annotation that has the same characteristics as <var>other</var> (<var>label</var>, <var>confidence</var>, etc.), but which is a different annotation with different (initially, no) graph linkages,
    * @param other The annotation to copy.
    */
   public Annotation(Annotation other)
   {
      cloneAttributesFrom(other, "id");
      putAll(other);
      Vector<String> keysToRemove = new Vector<String>();
      for (String key : keySet())
      {
	 // remove tracked original attributes
	 if (getTrackedAttributes().contains(key))
	 {
	    String originalValueKey = "original" + key.substring(0,1).toUpperCase() + key.substring(1);
	    keysToRemove.add(originalValueKey);
	 }
	 else if (key.length() > 0 && !Character.isLetterOrDigit(key.charAt(0)))
	 { // starts with non-alphanumeric
	    keysToRemove.add(key);
	 }
      } // next key
      for (String key : keysToRemove) remove(key);
   } // end of constructor


   /**
    * Setter for <i>id</i>: The annotation's identifier.
    * @param id The annotation's identifier.
    */
   public void setId(String id) 
   {
      // remove the old id from the graph index
      String oldId = getId();
      if (getGraph() != null) getGraph().getAnnotationsById().remove(oldId);
      super.setId(id);
      // add the new id from the graph index
      if (getGraph() != null) getGraph().getAnnotationsById().put(id, this);
	    
      // update all child parentIds
      for (SortedSet<Annotation> children : getAnnotations().values())
      {
	 for (Annotation child : children)
	 {
	    // check it still uses this id
	    if (child.getParentId() != null && child.getParentId().equals(oldId))
	    {
	       child.setParentId(id);
	    }
	 } // next child
      } // next child layer
   }
   
   /**
    * Gets the original label of the annotation, before any subsequent calls to {@link #setLabel(String)}, since the object was created or {@link #commit()} was called.
    * <p>This method mirrors the map key "originalLabel" created by the TrackedMap.
    * @return The original label.
    */
   public String getOriginalLabel()
   {
      try 
      { 
	 return (String)getOriginal("label"); 
      }
      catch(ClassCastException exception) 
      {
	 return getLabel();
      } 
   } // end of getOriginalLabel()
   /**
    * Gets the original startId of the annotation, before any subsequent calls to {@link #setStartId(String)}, since the object was created or {@link #commit()} was called.
    * <p>This method mirrors the map key "originalStartId" created by the TrackedMap.
    * @return The original label.
    */
   public String getOriginalStartId()
   {
      try 
      { 
	 return (String)getOriginal("startId"); 
      }
      catch(ClassCastException exception) 
      {
	 return getStartId();
      } 
   } // end of getOriginalStartId()
   /**
    * Gets the original endId of the annotation, before any subsequent calls to {@link #setEndId(String)}, since the object was created or {@link #commit()} was called.
    * <p>This method mirrors the map key "originalEndId" created by the TrackedMap.
    * @return The original endId.
    */
   public String getOriginalEndId()
   {
      try 
      { 
	 return (String)getOriginal("endId"); 
      }
      catch(ClassCastException exception) 
      {
	 return getEndId();
      } 
   } // end of getOriginalEndId()
   /**
    * Gets the original parentId of the annotation, before any subsequent calls to {@link #setParentId(String)}, since the object was created or {@link #commit()} was called.
    * <p>This method mirrors the map key "originalParenId" created by the TrackedMap.
    * @return The original parentId.
    */
   public String getOriginalParentId()
   {
      try 
      { 
	 return (String)getOriginal("parentId"); 
      }
      catch(ClassCastException exception) 
      {
	 return getParentId();
      } 
   } // end of getOriginalParentId()
   /**
    * Gets the original ordinal of the annotation, before any subsequent calls to {@link #setOrdinal(int)}, since the object was created or {@link #commit()} was called.
    * <p>This method mirrors the map key "originalOrdinal" created by the TrackedMap.
    * @return The original ordinal.
    */
   public int getOriginalOrdinal()
   {
      try 
      { 
	 return ((Integer)getOriginal("ordinal")).intValue(); 
      }
      catch(ClassCastException exception) 
      {
	 return getOrdinal();
      } 
   } // end of getOriginalOrdinal()

   
   /**
    * Returns the ordinal that has previously been explicitly assigned.
    * <p>This method differs from {@link #getOrdinal()} in that if {@link #setOrdinal(int)} has not been specifically invoked previously, {@link #getOrdinal()} will try to figure out the ordinal from the position in the parent, etc., where this method will simply return 0.
    * @return The ordinal last set by a call to {@link #setOrdinal(int)}, or 0 if it hasn't been previously called.
    */
   public int getAssignedOrdinal()
   {
      return ordinal;
   } // end of getAssignedOrdinal()
   
   /**
    * Gets a single related annotation on the given layer.
    * <p>"Related" means that <var>layerId</var> identifies the parent layer, an ancestor layer,
    * a child of an ancestor, or a child layer.
    * <p>This utility method makes navigating the layer hierarchy easier and with less prior
    * knowledge of it. e.g.:
    * <ul>
    *  <li><code>word.my("turn")</code> for the (parent) turn</li>
    *  <li><code>phone.my("turn")</code> for the (grandparent) turn</li>
    *  <li><code>word.my("POS")</code> for the (first) part of speech annotation</li>
    *  <li><code>phone.my("POS")</code> for the (first) part of speech annotation, which is neither an ancestor nor descendant, but rather is a child of an ancestor (<code>phone.my("word")</code>)</li>
    *  <li><code>word.my("who")</code> for the speaker</li>
    *  <li><code>word.my("graph")</code> for the graph</li>
    *  <li><code>word.my("utterance")</code> for the utterance, which is neither an ancestor nor descendant, but rather is a child of an ancestor (<code>word.my("turn")</code>)</li>
    *  <li><code>word.my("corpus")</code> for the graph's corpus, which is neither an ancestor nor descendant, but rather is a child of an ancestor (<code>word.my("graph")</code>)</li>
    * </ul>
    * <p>{@link #setGraph(Graph)} must have been previously called, and the graph must have a correct layer hierarchy for this method to work correctly.
    * @param layerId The layer of the desired annotation.
    * @return The related annotation (or the first one if there are many), or null if none could be found on the given layer.
    */
   public Annotation my(final String layerId)
   {
      // is it our own layer?
      if (layerId.equals(getLayerId()))
      {
	 // for now, return ourself - this is true of "graph", and is probably generally true
	 // whether it's true of, say "possible-pos" is debatable,
	 // and whether it's true of tree-stuctured layers needs to be worked through TODO
	 return this;
      }
      // is it the parent layer?
      if (getParent() != null && layerId.equals(getParent().getLayerId()))
      {
	 return getParent();
      }
      // is it an ancestor layer?
      Annotation anc = getAncestor(layerId);
      if (anc != null)
      {
	 return anc;
      }
      // is it a child layer?
      if (getAnnotations().containsKey(layerId))
      {
	 SortedSet<Annotation> children = getAnnotations(layerId);
	 if (children.size() > 0)
	 {
	    return children.first();
	 }
      }
      Layer layer = getGraph().getLayer(layerId);
      Layer commonAncestorLayer = getLayer().getFirstCommonAncestor(layer);
      if (commonAncestorLayer == null) return null; // invalid layer
      // is our layer an ancestor of layerId
      if (commonAncestorLayer.getId().equals(getLayer().getId()))
      { // we are an ancestor of the target layer
	 // annotations should come out in ordinal order when they have the same parent
	 // and in ancestor start order otherwise, where the ancestor used comes from the
	 // highest aligned layer in the hierarchy, which may be the layer itself. so:
	 // - aligned topics (parent=graph) fall back to topic offset
	 // - aligned turns (parent=who) fall back to turn offset
	 // - aligned words (parent=turn) fall back to turn offset
	 // - unaligned pos's (paren=word) fall back to turn offset
	 String root = getGraph().getSchema().getRoot().getId();
	 Layer highestAlignedLayer = layer;
	 for (Layer ancestor : layer.getAncestors())
	 {
	    if (ancestor.getId().equals(root)) break;
	    if (ancestor.getAlignment() != Constants.ALIGNMENT_NONE)
	    {
	       highestAlignedLayer = ancestor;
	    }
	 }
	 final String highestAlignedLayerId = highestAlignedLayer.getId();
	 LayerTraversal<TreeSet<Annotation>> highestAlignedTraversal 
	    = new LayerTraversal<TreeSet<Annotation>>(
	       new TreeSet<Annotation>(new AnnotationComparatorByAnchor()), this)
	    {
	       protected void pre(Annotation annotation)
	       {
		  if (annotation.getLayerId().equals(highestAlignedLayerId))
		  {
		     result.add(annotation);
		  }
	       }
	    };
	 if (layerId.equals(highestAlignedLayerId))
	 {
	    if (highestAlignedTraversal.getResult().size() == 0) return null;
	    return highestAlignedTraversal.getResult().first();
	 }
	 else
	 { // layerId != highestAlignedLayerId
	    LayerTraversal<Vector<Annotation>> descendantTraversal 
	       = new LayerTraversal<Vector<Annotation>>(new Vector<Annotation>())
	       {
		  protected void pre(Annotation annotation)
		  {
		     if (annotation.getLayerId().equals(layerId))
		     {
			result.add(annotation);
		     }
		  }
	       };
	    for (Annotation highestAlignedAncestor : highestAlignedTraversal.getResult())
	    {
	       descendantTraversal.traverseAnnotation(highestAlignedAncestor);
	    }
	    if (descendantTraversal.getResult().size() == 0) return null;
	    return descendantTraversal.getResult().firstElement();
	 }
      }
      // check for children of ancestors
      // so that word.my("utterance") works and so does word.my("corpus")
      Annotation commonAncestor = getAncestor(commonAncestorLayer.getId());
      if (commonAncestorLayer != null 
	  // common ancestor must be related - i.e. in the layer of commonAncestorLayer
	  && commonAncestor.getLayerId() == commonAncestorLayer.getId())
      {
	 // return the first child that t-includes this annotation
	 if (commonAncestor != null)
	 {
	    for (Annotation child : commonAncestor.list(layerId))
	    {
	       if (child.includes(this))
	       {
		  return child;
	       }
	    } // next child of common ancestor
	 }
      }
      return null;
   } // end of my()
   
   /**
    * Gets a list of related annotations on the given layer.
    * <p>"Related" means that <var>layerId</var> identifies the parent layer, an ancestor layer,
    * a child of an ancestor, a child layer, or a desdendant layer.
    * <p>This utility method makes navigating the layer hierarchy easier and with less prior
    * knowledge of it. e.g.:
    * <ul>
    *  <li><code>word.list("turn")[0]</code> for the (parent) turn</li>
    *  <li><code>phone.list("turn")[0]</code> for the (grandparent) turn</li>
    *  <li><code>word.list("POS")</code> for all (child) part of speech annotations</li>
    *  <li><code>word.list("phone")</code> for all (child) phones in a word</li>
    *  <li><code>turn.list("phone")</code> for all (grandchild) phones in a turn</li>
    *  <li><code>phone.list("POS")</code> for the all (peer) part of speech annotation, which are neither ancestors nor descendants, but rather children of an ancestor (<code>phone.my("word")</code>)</li>
    *  <li><code>word.list("who")[0]</code> for the (grandparent) speaker</li>
    *  <li><code>word.list("graph")[0]</code> for the (great-grandparent) graph</li>
    *  <li><code>word.list("utterance")[0]</code> for the (peer) utterance, which is neither an ancestor nor descendant, but rather is a child of an ancestor (<code>word.my"turn")</code>)</li>
    *  <li><code>utterace.list("word")</code> for the utterance's (peer) words, which are neither an ancestors nor descendants, but rather are children of an ancestor (<code>utterance.my"turn")</code>)</li>
    *  <li><code>word.list("corpus")[0]</code> for the graph's (child of grandparent) corpus, which is neither an ancestor nor descendant, but rather is a child of an ancestor (<code>word.my("graph")</code>)</li>
    * </ul>
    * <p>{@link #setGraph(Graph)} must have been previously called, and the graph must have a
    * correct layer hierarchy for this method to work correctly.
    * @param layerId The layer of the desired annotations.
    * @return The related annotations, or an empty array if none could be found on the given layer.
    */
   public Annotation[] list(final String layerId)
   {
      // is it our own layer?
      if (layerId.equals(getLayerId()))
      {
	 // for now, return ourself - this is true of "graph", and is probably generally true
	 // whether it's true of, say "possible-pos" is debatable,
	 // and whether it's true of tree-stuctured layers needs to be worked through TODO
	 Annotation[] annotations = new Annotation[1];
	 annotations[0] = this;
	 return annotations;
      }
      // is layerId a child layer?
      if (getLayer().getChildren().containsKey(layerId))
      {
	 return annotations(layerId);
      }
      Layer layer = getGraph().getLayer(layerId);
      Layer commonAncestorLayer = getLayer().getFirstCommonAncestor(layer);
      if (commonAncestorLayer == null) return new Annotation[0]; // invalid layer
      // is our layer an ancestor of layerId
      if (commonAncestorLayer.getId().equals(getLayer().getId()))
      { // we are an ancestor of the target layer
	 // annotations should come out in ordinal order when they have the same parent
	 // and in ancestor start order otherwise, where the ancestor used comes from the
	 // highest aligned layer in the hierarchy, which may be the layer itself. so:
	 // - aligned topics (parent=graph) fall back to topic offset
	 // - aligned turns (parent=who) fall back to turn offset
	 // - aligned words (parent=turn) fall back to turn offset
	 // - unaligned pos's (paren=word) fall back to turn offset
	 String root = getGraph().getSchema().getRoot().getId();
	 Layer highestAlignedLayer = layer;
	 for (Layer ancestor : layer.getAncestors())
	 {
	    if (ancestor.getId().equals(root)) break;
	    if (ancestor.getAlignment() != Constants.ALIGNMENT_NONE)
	    {
	       highestAlignedLayer = ancestor;
	    }
	 }
	 final String highestAlignedLayerId = highestAlignedLayer.getId();
	 LayerTraversal<TreeSet<Annotation>> highestAlignedTraversal 
	    = new LayerTraversal<TreeSet<Annotation>>(
	       new TreeSet<Annotation>(new AnnotationComparatorByAnchor()), this)
	    {
	       protected void pre(Annotation annotation)
	       {
		  if (annotation.getLayerId().equals(highestAlignedLayerId))
		  {
		     result.add(annotation);
		  }
	       }
	    };
	 if (layerId.equals(highestAlignedLayerId))
	 {
	    return highestAlignedTraversal.getResult().toArray(new Annotation[0]);
	 }
	 else
	 { // layerId != highestAlignedLayerId
	    LayerTraversal<Vector<Annotation>> descendantTraversal 
	       = new LayerTraversal<Vector<Annotation>>(new Vector<Annotation>())
	       {
		  protected void pre(Annotation annotation)
		  {
		     if (annotation.getLayerId().equals(layerId))
		     {
			result.add(annotation);
		     }
		  }
	       };
	    for (Annotation highestAlignedAncestor : highestAlignedTraversal.getResult())
	    {
	       descendantTraversal.traverseAnnotation(highestAlignedAncestor);
	    }
	    return descendantTraversal.getResult().toArray(new Annotation[0]);
	 }
      }
      // is it the parent layer?
      if (layerId.equals(getParentId()))
      {
	 Annotation[] annotations = new Annotation[1];
	 annotations[0] = getParent();
	 return annotations;
      }
      // is it an ancestor layer?
      Annotation ancestor = getAncestor(layerId);
      if (ancestor != null)
      {
	 Annotation[] annotations = new Annotation[1];
	 annotations[0] = ancestor;
	 return annotations;
      }
      // check for children of ancestors
      // so that word.list("utterance") works and so does word.list("corpus")
      Annotation commonAncestor = getAncestor(commonAncestorLayer.getId());
      // return the first child that t-includes this annotation
      if (commonAncestor != null 
	  // common ancestor must be related - i.e. in the layer of commonAncestorLayer
	  && commonAncestor.getLayerId() == commonAncestorLayer.getId())
      {
	 Vector<Annotation> annotations = new Vector<Annotation>();
	 for (Annotation child : commonAncestor.list(layerId))
	 {
	    if (child.includes(this) || this.includes(child))
	    {
	       annotations.add(child);
	    }
	 } // next child of common ancestor
	 if (annotations.size() > 0)
	 {
	    return annotations.toArray(new Annotation[0]);
	 }
      }
      return new Annotation[0];
   } // end of list()

   
   /**
    * Access the child annotations on a given layer.
    * <p>This method returns a new collection, with the annotations re-sorted by ordinal, on each invocation.
    * @param layerId The given layer ID.
    * @return The child annotations on the given layer, or null if <var>layerId</var> is not a child layer.
    */
   public SortedSet<Annotation> getAnnotations(String layerId)
   {
      // is it a valid child layer?
      if (getGraph() != null && !getLayer().getChildren().containsKey(layerId)) return null;

      TreeSet<Annotation> annotations = new TreeSet<Annotation>(new AnnotationComparatorByOrdinal());
      if (getAnnotations().containsKey(layerId))
      { // we already have a collection - sort it by ordinal before returning it
	 annotations.addAll(getAnnotations().get(layerId));
	 correctOrdinals(annotations);
      }
      // add the child collection to children
      getAnnotations().put(layerId, annotations);
      return annotations;
   } // end of getAnnotations()

   /**
    * The minimum ordinal for the given child layer. Used for when a fragment includes only some of
    * the children of a parent (otherwise their child ordinals would be forced to start at 1).
    * @param layerId The child layer ID.
    * @return The minimum ordinal for the given child layer, whic defaults to 1.
    */
   public int ordinalMinimum(String layerId)
   {
      if (ordinalMinima.containsKey(layerId)) return ordinalMinima.get(layerId);
      return 1;
   } // end of ordinalMinimum()
   /**
    * Set the minimum ordinal for the given child layer. Used for when a fragment includes only 
    * some of the children of a parent (otherwise their child ordinals would be forced to start
    * at 1).
    * @param layerId The child layer ID.
    * @param minimumOrdinal The minimum ordinal for the given child layer.
    */
   public void setOrdinalMinimum(String layerId, int minimumOrdinal)
   {
      ordinalMinima.put(layerId, minimumOrdinal);
   } // end of ordinalMinimum()

   /**
    * Access the child annotations on a given layer, as an array (for environments that deal better with arrays than collections).
    * @param layerId The given layer ID.
    * @return The child annotations on the given layer.
    */
   public Annotation[] annotations(String layerId)
   {
      try
      {
	 return getAnnotations(layerId).toArray(new Annotation[0]);
      }
      catch(NullPointerException exception)
      { // the layer doesn't exist
	 return null;
      }
   } // end of annotations()

   
   /**
    * Add a child annotation.
    * @param annotation The new child.
    * @return The annotation.
    */
   public Annotation addAnnotation(Annotation annotation)
   {
      getAnnotations(annotation.getLayerId()).add(annotation);
      if (annotation.getParentId() == null || !annotation.getParentId().equals(getId()))
      {
	 annotation.setParentId(getId());
      }
      return annotation;
   } // end of addAnnotation()

   
   /**
    * Returns the annotation's previous sibling by ordinal, if any.
    * @return The annotation before this one, among the parent's children, or null if the parent is not set of this is the first child.
    */
   public Annotation getPrevious()
   {
      Annotation parent = getParent();
      if (parent == null) return null;     
      SortedSet<Annotation> prior = parent.getAnnotations(getLayerId()).headSet(this);
      if (prior.size() == 0) return null;
      return prior.last();
   } // end of getPrevious()

   /**
    * Returns the annotation's next sibling by ordinal, if any.
    * @return The annotation after this one, among the parent's children, or null if the parent is not set of this is the last child.
    */
   public Annotation getNext()
   {
      Annotation parent = getParent();
      if (parent == null) return null;
      SortedSet<Annotation> subsequent = parent.getAnnotations(getLayerId()).tailSet(this);
      // the first element in subsequent is this annotation - we want the second.
      if (subsequent.size() == 1) return null;
      Iterator<Annotation> iSubsequent = subsequent.iterator();
      iSubsequent.next(); // move past first
      return iSubsequent.next();
   } // end of getPrevious()

   // query methods

   
   /**
    * Determines whether the anchors have offsets or not.
    * @return true if both #getStart() and #getEnd() anchors have non-null offsets, false otherwise.
    */
   public boolean getAnchored()
   {
      Anchor start = getStart();
      if (start == null) return false;
      if (start.getOffset() == null) return false;
      Anchor end = getEnd();
      if (end == null) return false;
      if (end.getOffset() == null) return false;
      return true;
   } // end of getUnsetOffset()

   
   /**
    * Determines whether the annotion's start/end offsets surround the given offset - i.e. whether the annotation t-includes the offset.
    * <p>A precondition is that the annotation's {@link #graph} is set.
    * @param offset The given offset.
    * @return true if getStart().getOffset() &le; offset &lt; getEnd().getOffset(), false otherwise.
    */
   public boolean includesOffset(Double offset)
   {
      if (offset == null) return false;
      Anchor start = getStart();
      if (start == null) return false;
      Double startOffset = start.getOffset();
      if (startOffset == null) return false;
      Anchor end = getEnd();
      if (end == null) return false;
      Double endOffset = end.getOffset();
      if (endOffset == null) return false;
      return startOffset.doubleValue() <= offset.doubleValue() 
	 && endOffset.doubleValue() > offset.doubleValue();
   } // end of includesOffset()

   
   /**
    * Determines whether this annotation t-includes the given annotation. Returns true if this annotation includes the other annotation's start and end offsets.
    * <p><em>NB</em> If the start and end offsets of <var>other</var> are the same as the end offset of this annotation (i.e. <var>other</var> is instantaneous at the end of this annotation), this method will return false.
    * <p>A precondition is that the annotation's {@link #graph} is set.
    * @param other The given other annotation.
    * @return true if the other annotation's duration is wholly included within this annotation's duration, and false otherwise.
    * @see #includesOffset(Double)
    */
   public boolean includes(Annotation other)
   {
      if (other.getStart() == null || other.getEnd() == null)
      {
	 return false;
      }

      // other.start must be included
      return includesOffset(other.getStart().getOffsetMin())
	 // and other end must be included
	 && (includesOffset(other.getEnd().getOffsetMax())
	     // or at least simultaneous with the end
	     || this.getEnd().getOffset().equals(other.getEnd().getOffsetMax()));
   } // end of includes()
   
   
   /**
    * Determines whether this annotation includes the midpoint of the given annotation.
    * <p><em>NB</em> If the start and end offsets of <var>other</var> are the same as the end offset of this annotation (i.e. <var>other</var> is instantaneous at the end of this annotation), this method will return false.
    * <p>A precondition is that the annotation's {@link #graph} is set.
    * @param other The given other annotation.
    * @return true if this annotation includes the midpoint of the given annotation, or if they share start/end anchors (even if there are null offsets), false otherwise.
    */
   public boolean includesMidpointOf(Annotation other)
   {
      if (!includesOffset(other.getMidpoint()))
      {
	 // special case: if the two annotations have the same anchors, even if there are unset offsets
	 // we know that this includes the midpoint of the other
	 if (getStartId().equals(other.getStartId())
	     && getEndId().equals(other.getEndId()))
	 {
	    return true;
	 }
	 else
	 {
	    return false;
	 }
      }
      else
      {
	 return true;
      }
   } // end of includesMidpointOf()

   /**
    * Determines the offset difference between this annotation and another - i.e. the minimum distance between any of the anchors.  If the annotations overlap, the returned difference will be negative, with a magnitude corresponding to the degree of overlap.
    * @param other The given other annotation.
    * @return The minimum distance between any two of the annotations' anchors, or null if any anchors are unset.
    */
   public Double distance(Annotation other)
   {
      Anchor start = getStart();
      if (start == null) return null;
      Double startOffset = start.getOffset();
      if (startOffset == null) return null;
      Anchor end = getEnd();
      if (end == null) return null;
      Double endOffset = end.getOffset();
      if (endOffset == null) return null;

      Anchor otherStart = other.getStart();
      if (otherStart == null) return null;
      Double otherStartOffset = otherStart.getOffset();
      if (otherStartOffset == null) return null;
      Anchor otherEnd = other.getEnd();
      if (otherEnd == null) return null;
      Double otherEndOffset = otherEnd.getOffset();
      if (otherEndOffset == null) return null;
      
      if (includes(other))
      {
	 return -other.getDuration();
      }
      else if (other.includes(this))
      {
	 return -getDuration();
      }
      else
      {
	 double dDifference = Math.abs(startOffset - otherEndOffset);
	 dDifference = Math.min(dDifference, Math.abs(endOffset - otherStartOffset));
	 // do they overlap?
	 if (startOffset < otherEndOffset && endOffset > otherStartOffset) dDifference *= -1;
	 return dDifference;
      }
   } // end of distance()
   
   /**
    * Determines the offset halfway between the start offset and the end offset.
    * <p>A precondition is that the annotation's {@link #graph} is set.
    * @return The offset halfway between the start offset and the end offset, or null if either anchor or offset is null.
    */
   public Double getMidpoint()
   {
      Anchor start = getStart();
      if (start == null) return null;
      Double startOffset = start.getOffset();
      if (startOffset == null) return null;
      Anchor end = getEnd();
      if (end == null) return null;
      Double endOffset = end.getOffset();
      if (endOffset == null) return null;
      return startOffset + (getDuration() / 2);
   } // end of getMidpoint()
   
   /**
    * Determines the duration of the annotation - i.e. the difference between the start offset and the end offset.
    * <p>A precondition is that the annotation's {@link #graph} is set.
    * @return The duration of the annotation, or null if either start or end anchors or offsets are null.
    */
   public Double getDuration()
   {
      Anchor start = getStart();
      if (start == null) return null;
      Double startOffset = start.getOffset();
      if (startOffset == null) return null;
      Anchor end = getEnd();
      if (end == null) return null;
      Double endOffset = end.getOffset();
      if (endOffset == null) return null;
      return endOffset - startOffset;
   } // end of getDuration()

   
   /**
    * Determines whether this is a tag of the given annotation (or vice-versa), i.e. whether the two annotations share start/end anchors.
    * @param other The given other annotation.
    * @return true if the start and end anchor IDs are the same for this and the other annotation, false otherwise.
    */
   public boolean tags(Annotation other)
   {
      return getStartId().equals(other.getStartId())
	 && getEndId().equals(other.getEndId());
   } // end of tags()

   
   /**
    * Finds all annotations on the given layer that include this annotation. This uses {@link #includes(Annotation)} to determine inclusion. Annotations marked for deletion are ignored.
    * <p>A precondition is that the annotation's {@link #graph} is set.
    * @param layerId The given layer ID.
    * @return A list of annotations on the given layer that include this annotation. This uses {@link #includes(Annotation)} to determine inclusion.
    */
   public Annotation[] includingAnnotationsOn(String layerId)
   {
      Vector<Annotation> includingAnnotations = new Vector<Annotation>();
      if (graph != null && getAnchored())
      {
	 for (Annotation other : graph.listNear(layerId, getStart().getOffset()))
	 {
	    if (other.getChange() == Change.Operation.Destroy) continue;
	    if (other == this) continue; // exclude ourselves
	    if (other.includes(this))
	    {
	       includingAnnotations.add(other);
	    }
	 } // next annotation
      }
      return includingAnnotations.toArray(new Annotation[0]);
   } // end of includingAnnotationsOn()

   /**
    * Finds all annotations on the given layer that this annotation includes. This uses {@link #includes(Annotation)} to determine inclusion. Annotations marked for deletion are ignored.
    * <p>A precondition is that the annotation's {@link #graph} is set.
    * @param layerId The given layer ID.
    * @return A list of annotations on the given layer that include this annotation. This uses {@link #includes(Annotation)} to determine inclusion.
    */
   public Annotation[] includedAnnotationsOn(String layerId)
   {
      Vector<Annotation> includedAnnotations = new Vector<Annotation>();
      if (graph != null && getAnchored())
      {
	 for (Annotation other : graph.list(layerId))
	 {
	    if (other.getChange() == Change.Operation.Destroy) continue;
	    if (other == this) continue; // exclude ourselves
	    if (this.includes(other))
	    {
	       includedAnnotations.add(other);
	    }
	 } // next annotation
      }
      return includedAnnotations.toArray(new Annotation[0]);
   } // end of includingAnnotationsOn()

   /**
    * Finds all annotations on the given layer that include the midpoint of this annotation. This uses {@link #includesMidpointOf(Annotation)} to determine inclusion. Annotations marked for deletion are ignored.
    * <p>A precondition is that the annotation's {@link #graph} is set.
    * @param layerId The given layer ID.
    * @return A list of annotations on the given layer that include this annotation's midpoint. This uses {@link #includesMidpointOf(Annotation)} to determine inclusion.
    */
   public Annotation[] midpointIncludingAnnotationsOn(String layerId)
   {
      Vector<Annotation> includingAnnotations = new Vector<Annotation>();
      if (graph != null && getAnchored())
      {
	 for (Annotation other : graph.listNear(layerId, getMidpoint()))
	 {
	    if (other.getChange() == Change.Operation.Destroy) continue;
	    if (other == this) continue; // exclude ourselves
	    if (other.includesMidpointOf(this))
	    {
	       includingAnnotations.add(other);
	    }
	 } // next annotation
      }
      return includingAnnotations.toArray(new Annotation[0]);
   } // end of includingAnnotationsOn()

   /**
    * Finds all annotations on the given layer that tag this annotation - i.e. where start and end anchors are shared. Annotations marked for deletion are ignored.
    * <p>A precondition is that the annotation's {@link #graph} is set.
    * @param layerId The given layer ID.
    * @return A list of annotations on the given layer that tag this annotation.
    */
   public Annotation[] tagsOn(String layerId)
   {
      Vector<Annotation> tags = new Vector<Annotation>();
      Anchor start = getStart();
      if (start != null)
      {
	 String endId = getEndId();
	 for (Annotation other : start.startOf(layerId))
	 {
	    if (other.getChange() == Change.Operation.Destroy) continue;
	    if (other == this) continue; // exclude ourselves
	    if (other.getEndId().equals(endId))
	    {
	       tags.add(other);
	    }
	 } // next annotation
      }
      return tags.toArray(new Annotation[0]);
   } // end of tagsOn()

   
   /**
    * Determines the first ancestor annotation this annotation has in common with the given annotation. This may return the graph itself, if there are no earlier common ancestors. "Ancestors" is inclusive in the sense that if either annotation is an ancestor of the other, it will be returned.
    * <p>A precondition is that the annotation's {@link #graph} is set.
    * @param other The other annotation.
    * @return The first ancestor annotation this annotation has in common with the given annotation, or null if {@link #graph} is not set or the graph is not complete enough to find the common ancestor.
    */
   public Annotation getFirstCommonAncestor(Annotation other)
   {
      HashSet<Annotation> ourAncestors = new HashSet<Annotation>();
      Annotation ancestor = this; // include ourselves in the list.
      do
      {
	 ourAncestors.add(ancestor);
	 ancestor = ancestor.getParent();
      }
      while (ancestor != null);
      
      ancestor = other; // include other annotation in the list.
      do
      {
	 if (ourAncestors.contains(ancestor))
	 {
	    return ancestor;
	 }
	 ancestor = ancestor.getParent();
      }
      while (ancestor != null);
      return null;
   } // end of getFirstCommonAncestor()

   
   /**
    * Returns a list of ancestor annotations (parent, grandparent, etc.).
    * @return A set of ancestor annotations, ordered by distance from this annotation (i.e. parent first, then grandparent, etc.).
    */
   public LinkedHashSet<Annotation> getAncestors()
   {
      LinkedHashSet<Annotation> ancestors = new LinkedHashSet<Annotation>();
      Annotation ancestor = getParent(); // don't include ourselves in the list.
      while (ancestor != null  
	     && !ancestors.contains(ancestor)) // (guard against cycles, just in case)
      {
	 ancestors.add(ancestor);
	 ancestor = ancestor.getParent();
      } // next ancestor
      return ancestors;
   } // end of getAncestors()

   
   /**
    * Returns the ancestor on the given layer.
    * @param layerId The layer of the returned ancestor.
    * @return The ancestor on the given layer, or null if there is no ancestor on that layer.
    */
   public Annotation getAncestor(String layerId)
   {
      HashSet<Annotation> ancestors = new HashSet<Annotation>();
      Annotation ancestor = getParent(); // don't include ourselves in the list.
      while (ancestor != null  
	     && !ancestors.contains(ancestor)) // (guard against cycles, just in case)
      {
	 if (ancestor.getLayerId().equals(layerId))
	 {
	    return ancestor;
	 }
	 ancestor = ancestor.getParent();
      } // next ancestor
      // got to the top without finding one
      return null;
   } // end of getAncestor()



   /**
    * Returns the descendant annotation with the earliest start anchor, excluding tag layers (layer where {@link Layer#getAlignment()} == {@link Constants#ALIGNMENT_NONE}) and non-included layers.
    * <p>Assumes that the annotation's graph has been set.
    * @return The highest descendant annotation with the earliest start anchor, or null if there are no descendants.
    */
   public Annotation getEarliestDescendant()
   {
      if (graph == null) return null;
      Annotation earliest = null;
      for (String layerId : getAnnotations().keySet())
      {
	 Layer layer = graph.getLayer(layerId);
	 if (layer == null) continue;
	 for (Annotation child : getAnnotations(layerId))
	 {
	    if (child.getChange() == Change.Operation.Destroy) continue;
	    if (layer.getAlignment() != Constants.ALIGNMENT_NONE
		&& layer.getParentIncludes())
	    {
	       Anchor start = child.getStart();
	       if (start == null) continue;
	       if (start.getOffset() == null) continue;	    
	       if (earliest == null
		   || child.getStart().getOffset() < earliest.getStart().getOffset())
	       {
		  earliest = child;	       
	       }
	    }
	    Annotation childsEarliest = child.getEarliestDescendant();
	    if (childsEarliest != null)
	    {
	       if (earliest == null
		   || childsEarliest.getStart().getOffset() < earliest.getStart().getOffset())
	       {
		  earliest = childsEarliest;
	       }
	    }
	 } // next child
      } // next child layer
      return earliest;
   } // end of getEarliestDescendant()

   /**
    * Returns the descendant annotation with the latest end anchor, excluding tag layers (layer where {@link Layer#getAlignment()} == {@link Constants#ALIGNMENT_NONE}) and non-included layers.
    * <p>Assumes that the annotation's graph has been set.
    * @return The highest descendant annotation with the latest end anchor, or null if there are no descendants.
    */
   public Annotation getLatestDescendant()
   {
      if (graph == null) return null;
      Annotation latest = null;
      for (String layerId : getAnnotations().keySet())
      {
	 Layer layer = graph.getLayer(layerId);
	 if (layer == null) continue;
	 for (Annotation child : getAnnotations(layerId))
	 {
	    if (child.getChange() == Change.Operation.Destroy) continue;
	    if (layer.getAlignment() != Constants.ALIGNMENT_NONE
		&& layer.getParentIncludes())
	    {
	       Anchor end = child.getEnd();
	       if (end == null) continue;
	       if (end.getOffset() == null) continue;	    
	       if (latest == null
		   || child.getEnd().getOffset() > latest.getEnd().getOffset())
	       {
		  latest = child;
	       }
	    }
	    Annotation childsLatest = child.getLatestDescendant();
	    if (childsLatest != null)
	    {
	       if (latest == null
		   || childsLatest.getEnd().getOffset() < latest.getEnd().getOffset())
	       {
		  latest = childsLatest;
	       }
	    }
	 } // next child
      } // next child layer
      return latest;
   } // end of getEarliestDescendant()


   /**
    * Returns whether the annotation is formally instantaneous or not - i.e. whether or not its start anchor and end anchor are the same.
    * <p><em>NB</em> If the anchors are different but their offsets are the same, this method will return <em>false</em>.
    * @return true if #getStartId() equals #getEndId(), false otherwise.
    */
   public boolean getInstantaneous()
   {
      assert getStartId() != null : "getStartId() != null - " + getLayerId() + " " + getLabel();
      return getStartId().equals(getEndId());
   } // end of getInstantaneous()

   /**
    * Determines the offset difference between this annotation and another
    * - i.e. the maximum distance between the start and end anchors
    * - MAX(ABS(this.start-other.start),ABS(this.end-other.end)).  
    * If the annotations overlap, the returned difference will be negative, with a magnitude as defined above.
    * @param other Annotation to compare to.
    * @return The maximum distance between starts and ends of the annotations, or null if any anchors are unset.
    */
   public Double maxPairedDistance(Annotation other)
   {
      // are there anchors to compare
      if (getStart() == null || getStart().getOffset() == null
	  || getEnd() == null || getEnd().getOffset() == null
	  || other == null
	  || other.getStart() == null || other.getStart().getOffset() == null
	  || other.getEnd() == null || other.getEnd().getOffset() == null)
      {
	 return null;
      }

      double dMyStart = getStart().getOffset();
      double dMyEnd = getEnd().getOffset();
      double dTheirStart = other.getStart().getOffset();
      double dTheirEnd = other.getEnd().getOffset();
      double dDifference = Math.max(Math.abs(dMyStart-dTheirStart), Math.abs(dMyEnd-dTheirEnd));

      // do they overlap ?
      if (dMyStart < dTheirEnd && dMyEnd > dTheirStart) dDifference *= -1;
      return dDifference;
   } // end of maxDistance()

   /* TODO
    overlaps : function(annotation) { return this.start.offset < annotation.end.offset && this.end.offset > annotation.start.offset; },

    sharesStart : function(layerId) { return this.start.startOf[layerId]; },
    sharesEnd : function(layerId) { return this.end.endOf[layerId]; },
    startsWith : function(annotation) { return this.startId == annotation.startId; },
    endsWith : function(annotation) { return this.endId == annotation.endId; },
    predecessorOf : function(annotation) { return this.endId == annotation.startId ; },
    successorOf : function(annotation) { return annotation.endId == this.startId ; },

    tagOn : function(layerId) 
    {
	var tags = [];
	for (var i in this.start.startOf[layerId])
	{
	    var other = this.start.startOf[layerId][i];
	    if (this.startsWith(other)) tags.push(other);
	} // next annotation that starts here
	return tags;
    },
   */

   // annotation methods
   
   /**
    * Tags this annotation with the given tag.
    * @param layerId The layer ID for the tag.
    * @param label The layer for the tag.
    * @return The tag annotation created.
    */
   public Annotation createTag(String layerId, String label)
   {
      if (getGraph() == null) return null;

      Annotation tag = new Annotation(null, label, layerId, getStartId(), getEndId());
      assert getGraph() != null : "getGraph() != null";
      assert tag.getLayerId() != null : "tag.getLayerId() != null";
      assert getGraph().getLayer(tag.getLayerId()) != null : "getGraph().getLayer(tag.getLayerId()) != null - " + tag.getLayerId();
      assert getGraph().getLayer(tag.getLayerId()).getParent() != null : "getGraph().getLayer(tag.getLayerId()).getParent() != null";
      assert getLayer() != null : "getLayer() != null";
      if (getGraph().getLayer(tag.getLayerId()).getParent() == getLayer())
      { // tag is child of this
	 tag.setParent(this);
	 if (getGraph().getLayer(tag.getLayerId()).getAlignment() == 0)
	 { // it's a non-aligned layer, so the anchors must match the parent
	    assert getStartId() != null : "getStartId() != null " + getLayer() + ": " + getLabel() + " - " + tag.getLayerId() + ": " + tag;
	    tag.setStartId(getStartId());
	    assert getEndId() != null : "getEndId() != null " + getLayer() + ": " + getLabel();
	    tag.setEndId(getEndId());
	 }
      }
      else if (getGraph().getLayer(tag.getLayerId()).getParent() == getLayer().getParent())
      { // this layer and tag layer share a parent
	 tag.setParent(getParent());
	 if (getGraph().getLayer(tag.getLayerId()).getAlignment() == 0)
	 { // it's a non-aligned layer, so the anchors must match the parent
	    assert getParent().getStartId() != null : "getParent().getStartId() != null " + getParent().getLayer() + ": " + getParent().getLabel();
	    tag.setStartId(getParent().getStartId());
	    assert getParent().getEndId() != null : "getParent().getEndId() != null " + getParent().getLayer() + ": " + getParent().getLabel();
	    tag.setEndId(getParent().getEndId());
	 }
      }
      else if (getGraph().getLayer(layerId).getParentId().equals("graph"))
      { // the tag layer is a top level layer, so its parent is the whole graph
	 tag.setParent(getGraph());
      }

      getGraph().addAnnotation(tag);
      return tag;
   } // end of createTag()

   // java.lang.Object overrides:
   
   /**
    * A string representation of the object.
    * @return A string representation of the object.
    */
   public String toString()
   {
      if (label == null) return "[" + getId() + "]";
      return getLabel();
   } // end of toString()   

   // Comparable method

   /**
    * Compare two annotations. 
    * <p> If both have the same parent, they're compared by ordinal.
    * <p> Otherwise, if they're both on the same layer, they're compared by parent.
    * <p> Otherwise, they're compared by id.
    * @return A negative integer, zero, or a positive integer as this annotation is before than, equal to, or after the specified annotation. 
    */ 
   public int compareTo(Annotation o)
   {
      if (this == o) return 0;
      if (this.equals(o)) return 0;
      if (getParentId() != null && getParentId().equals(o.getParentId())
	  && this.ordinal != 0 && o.ordinal != 0
	  && this.ordinal != o.ordinal)
      {
	 return new Integer(this.ordinal).compareTo(new Integer(o.ordinal));
      }
      if (getLayerId().equals(o.getLayerId())
	  && getParent() != null && o.getParent() != null
	  && getParent() != o.getParent())
      {
	 return getParent().compareTo(o.getParent());
      }
      return getId().compareTo(o.getId());
   }
   
} // end of class Annotation
