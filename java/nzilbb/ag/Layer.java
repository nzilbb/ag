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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Set;
import javax.json.JsonObject;
import nzilbb.ag.util.AnnotationComparatorByOrdinal;
import nzilbb.util.ClonedProperty;

/**
 * Annotation graph layer.
 * <p>Each annotation belongs to a Layer, which defines what layer its parent annotation is on, 
 * and how its anchors relate to its parent annotation's anchors.
 * <p>The primary defining characteristics of a layer are:
 * <ul>
 *  <li>{@link #getParentId() parentId} - what layer parent annotations are on</li>
 *  <li>{@link #getAlignment() alignment} - whether this annotation is 
 *      {@link Constants#ALIGNMENT_NONE not aligned} (i.e. simply tags the parent annotation), or
 *      defines a  {@link Constants#ALIGNMENT_INSTANT instant}, or an 
 *      {@link Constants#ALIGNMENT_INTERVAL interval}.</li> 
 *  <li>{@link #getPeers() peers} - whether there is one child annotation per parent
 *      (e.g. orthography tags on word tokens), or children can have peers (e.g. word tokens in a
 *      turn, or different POS tags on a word token)</li> 
 *  <li>{@link #getPeersOverlap peersOverlap} - if there are peers, whether they can overlap
 *      (e.g. topic tags within a graph, syntactic parse annotations within a turn) or not
 *      (e.g. word tokens within a turn, utterance partitions within a turn, turn annotations for
 *      a participant)</li> 
 *  <li>{@link #getParentIncludes() parentIncludes} - whether the child annotation's anchors must
 *      have offsets between the parent's offsets (e.g. word tokens within a turn, tag
 *      annotations, syntactic parse annotations within a turn) or not (e.g. syntactic dependency
 *      annotations linking one word to another)</li> 
 *  <li>{@link #getSaturated() saturated} - whether the child annotations fully cover the
 *      duration of the parent (e.g. utterance partitions within a turn, tag annotations) or not
 *      (e.g. word tokens within a turn, which may have leading, intervening, or trailing
 *      pauses)</li> 
 * </ul>
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class Layer
   extends TrackedMap
{
   // Attributes stored in HashMap:
   
   /**
    * The layer's parent layer id.
    */
   protected String parentId;
   /**
    * Getter for <i>parentId</i>: The layer's parent layer id.
    * @return The layer's parent layer id.
    */
   @ClonedProperty
   public String getParentId() { return parentId; }
   /**
    * Setter for <i>parentId</i>: The layer's parent layer id
    * @param parentId The layer's parent layer id
    */
   public Layer setParentId(String parentId) { this.parentId = parentId; return this; }

   /**
    * The description of the layer.
    */
   protected String description;
   /**
    * Getter for <i>description</i>: The description of the layer.
    * @return The description of the layer.
    */
   @ClonedProperty @TrackedProperty
   public String getDescription() { return description; }
   /**
    * Setter for <i>description</i>: The description of the layer.
    * @param description The description of the layer.
    * @see Constants#ALIGNMENT_NONE
    * @see Constants#ALIGNMENT_INSTANT
    * @see Constants#ALIGNMENT_INTERVAL
    */
   public Layer setDescription(String description) { this.description = description; return this; }

   /**
    * The layer's alignment - 0 for none, 1 for point alignment, 2 for interval alignment.
    * @see Constants#ALIGNMENT_NONE
    * @see Constants#ALIGNMENT_INSTANT
    * @see Constants#ALIGNMENT_INTERVAL
    */
   protected int alignment;
   /**
    * Getter for <i>alignment</i>: The layer's alignment - 0 for none, 1 for point alignment, 2
    * for interval alignment. 
    * @return The layer's alignment - 0 for none, 1 for point alignment, 2 for interval alignment.
    * @see Constants#ALIGNMENT_NONE
    * @see Constants#ALIGNMENT_INSTANT
    * @see Constants#ALIGNMENT_INTERVAL
    */
   @ClonedProperty @TrackedProperty
   public int getAlignment() { return alignment; }
   /**
    * Setter for <i>alignment</i>: The layer's alignment - 0 for none, 1 for point alignment, 2
    * for interval alignment. 
    * @param alignment The layer's alignment - 0 for none, 1 for point alignment, 2 for interval
    * alignment. 
    * @see Constants#ALIGNMENT_NONE
    * @see Constants#ALIGNMENT_INSTANT
    * @see Constants#ALIGNMENT_INTERVAL
    */
   public Layer setAlignment(int alignment) { this.alignment = alignment; return this; }

   /**
    * Whether children on this layer have peers or not. Defaults to <code>true</code>
    */
   protected boolean peers = true;
   /**
    * Getter for <i>peers</i>: Whether children on this layer have peers or not.
    * @return Whether children on this layer have peers or not.
    */
   @ClonedProperty @TrackedProperty
   public boolean getPeers() { return peers; }
   /**
    * Setter for <i>peers</i>: Whether children on this layer have peers or not.
    * @param peers Whether children on this layer have peers or not.
    */
   public Layer setPeers(boolean peers) { this.peers = peers; return this; }

   /**
    * Whether child peers can overlap or not. Defaults to <code>true</code>.
    */
   protected boolean peersOverlap = true;
   /**
    * Getter for <i>peersOverlap</i>: Whether child peers can overlap or not.
    * @return Whether child peers can overlap or not.
    */
   @ClonedProperty @TrackedProperty
   public boolean getPeersOverlap() { return peersOverlap; }
   /**
    * Setter for <i>peersOverlap</i>: Whether child peers can overlap or not.
    * @param peersOverlap Whether child peers can overlap or not.
    */
   public Layer setPeersOverlap(boolean peersOverlap) { this.peersOverlap = peersOverlap; return this; }

   /**
    * Whether the parent t-includes the child. Defaults to <code>true</code>.
    */
   protected boolean parentIncludes = true;
   /**
    * Getter for <i>parentIncludes</i>: Whether the parent t-includes the child.
    * @return Whether the parent t-includes the child.
    */
   @ClonedProperty @TrackedProperty
   public boolean getParentIncludes() { return parentIncludes; }
   /**
    * Setter for <i>parentIncludes</i>: Whether the parent t-includes the child.
    * @param parentIncludes Whether the parent t-includes the child.
    */
   public Layer setParentIncludes(boolean parentIncludes) { this.parentIncludes = parentIncludes; return this; }

   /**
    * Whether children on this layer must temporally fill the entire parent duration (true) or not
    * (false). Defaults to <code>true</code> 
    */
   protected boolean saturated = true;
   /**
    * Getter for <i>saturated</i>: Whether children on this layer must temporally fill the entire 
    * parent duration (true) or not (false).
    * @return Whether children must temporally fill the entire parent duration (true) or not
    * (false). 
    */
   @ClonedProperty @TrackedProperty
   public boolean getSaturated() { return saturated; }
   /**
    * Setter for <i>saturated</i>: Whether children on this layer must temporally fill the
    * entire parent duration (true) or not (false). 
    * @param saturated Whether children on this layer must temporally fill the entire
    * parent duration (true) or not (false).
    */
   public Layer setSaturated(boolean saturated) { this.saturated = saturated; return this; }
   
   /**
    * Child layers.
    */
   protected LinkedHashMap<String,Layer> children;
   /**
    * Getter for <i>children</i>: Child layers.
    * @return Child layers.
    */
   @SuppressWarnings("unchecked")
   public LinkedHashMap<String,Layer> getChildren() 
   {
      if (children == null)
      {
         children = new LinkedHashMap<String,Layer>();
      }

      return children; 
   }
   /**
    * Setter for <i>children</i>: Child layers.
    * @param children Child layers.
    */
   public Layer setChildren(LinkedHashMap<String,Layer> children) { this.children = children; return this; }
   
   /**
    * The layer's parent layer, if any.
    * @see #getParent()
    * @see #setParent(Layer)
    */
   protected Layer parent;
   /**
    * Getter for {@link #parent}: The layer's parent layer, if any.
    * @return The layer's parent layer, if any.
    */
   public Layer getParent() { return parent; }
   /**
    * Setter for {@link #parent}: The layer's parent layer, if any.
    * @param newParent The layer's parent layer, if any.
    */
   public Layer setParent(Layer newParent)
   { 
      parent = newParent; 
      if (parent != null)
      {
         setParentId(parent.getId());
         parent.getChildren().put(getId(), this);
      }
      return this;
   }

   /**
    * The type for labels on this layer.
    * <p>Either a MIME type, or one of:
    *  <ul>
    *   <li>{@link Constants#TYPE_STRING} (the default)</li>
    *   <li>{@link Constants#TYPE_IPA}</li>
    *   <li>{@link Constants#TYPE_NUMBER}</li>
    *   <li>{@link Constants#TYPE_SELECT}</li>
    *  </ul>
    */
   protected String type = Constants.TYPE_STRING;
   /**
    * Getter for {@link #type}: The type for labels on this layer.
    * <p>Either a MIME type, or one of:
    *  <ul>
    *   <li>{@link Constants#TYPE_STRING}</li>
    *   <li>{@link Constants#TYPE_IPA}</li>
    *   <li>{@link Constants#TYPE_NUMBER}</li>
    *   <li>{@link Constants#TYPE_SELECT}</li>
    *  </ul>
    * @return The type for labels on this layer.
    */
   @ClonedProperty @TrackedProperty
   public String getType() { return type; }
   /**
    * Setter for {@link #type}: The type for labels on this layer.
    * @param type The type for labels on this layer.
    */
   public Layer setType(String type) { this.type = type; return this; }
   
   /**
    * List of valid label values for this layer, or null if the layer values are not restricted.
    * <p>The 'key' is the possible label value, and each key is associated with a description of
    * the value (e.g. for displaying to users). 
    */
   protected LinkedHashMap<String,String> validLabels;
   /**
    * Getter for <tt>validLabels</tt>: List of valid label values for this layer, or null if the
    * layer values are not restricted. 
    * <p>The 'key' is the possible label value, and each key is associated with a description of
    * the value (e.g. for displaying to users). 
    * @return List of valid label values for this layer, or null if the layer values are not
    * restricted. 
    */
   @ClonedProperty @TrackedProperty
   public LinkedHashMap<String,String> getValidLabels()
   {
      if (validLabels == null) validLabels = new LinkedHashMap<String,String>();
      return validLabels;
   }
   /**
    * Setter for <tt>validLabels</tt>: List of valid label values for this layer, or null if the
    * layer values are not restricted. 
    * <p>The 'key' is the possible label value, and each key is associated with a description of
    * the value (e.g. for displaying to users). 
    * @param newValidLabels List of valid label values for this layer, or null if the layer values
    * are not restricted. 
    */
   public Layer setValidLabels(LinkedHashMap<String,String> newValidLabels) { this.validLabels = newValidLabels; return this; }
   /**
    * Getter for <tt>validLabels</tt>: List of valid label values for this layer, or null if the
    * layer values are not restricted. 
    * @return List of valid label values for this layer, or null if the layer values are not
    * restricted. 
    */
   public String[] getValidLabelsArray() 
   { 
      if (getValidLabels() == null) return null; 
      return getValidLabels().keySet().toArray(new String[0]); 
   }
   
   /**
    * Category for the layer, if any.
    * @see #getCategory()
    * @see #setCategory(String)
    */
   protected String category;
   /**
    * Getter for {@link #category}: Category for the layer, if any.
    * @return Category for the layer, if any.
    */
   @ClonedProperty @TrackedProperty
   public String getCategory() { return category; }
   /**
    * Setter for {@link #category}: Category for the layer, if any.
    * @param newCategory Category for the layer, if any.
    */
   public Layer setCategory(String newCategory) { category = newCategory; return this; }
   // Methods:
      
   /**
    * Default constructor
    */
   public Layer()
   {
   } // end of constructor

   /**
    * Bare name constructor. This is provided for 'builder pattern' style contruction, like:
    * <code>
    *  Layer layer = new Layer("phone", "Phones")
    *    .setParentId("word")
    *    .setAlignment(Constants.ALIGNMENT_INTERVAL)
    *    .setPeers(true)
    *    .setPeersOverlap(false)
    * </code>
    * @param id The layer's identifier.
    * @param description The description of the layer.
    */
   public Layer(String id, String description)
   {
      setId(id);
      setDescription(description);
   } // end of constructor

   /**
    * Bare name constructor where the name and description are the same. This is provided
    * for 'builder pattern' style contruction, like: 
    * <code>
    *  Layer layer = new Layer("phone")
    *    .setParentId("word")
    *    .setAlignment(Constants.ALIGNMENT_INTERVAL)
    *    .setPeers(true)
    *    .setPeersOverlap(false)
    * </code>
    * @param id The layer's identifier.
    */
   public Layer(String id)
   {
      setId(id);
      setDescription(id);
   } // end of constructor

   /**
    * Attribute constructor.
    * @param id The layer's identifier.
    * @param description The description of the layer.
    * @param parentId The layer's parent layer id
    * @param alignment The layer's alignment - 0 for none, 1 for point alignment, 2 for interval
    * alignment. 
    * @param peers Whether children on this layer have peers or not.
    * @param peersOverlap Whether child peers can overlap or not.
    * @param parentIncludes Whether the parent t-includes the child.
    * @param saturated Whether children on this layer must temporally fill the entire
    * parent duration (true) or not (false). 
    */
   public Layer(String id, String description, int alignment, boolean peers, boolean peersOverlap, boolean saturated, String parentId, boolean parentIncludes)
   {
      setId(id);
      setDescription(description);
      setParentId(parentId);
      setAlignment(alignment);
      setPeers(peers);
      setPeersOverlap(peersOverlap);
      setParentIncludes(parentIncludes);
      setSaturated(saturated);
   } // end of constructor

   /**
    * Top-level layer constructor.  The <var>parentId</var> is taken to be "graph".
    * @param id The layer's identifier.
    * @param description The description of the layer.
    * @param alignment The layer's alignment - 0 for none, 1 for point alignment, 2 for interval
    * alignment. 
    * @param peers Whether children on this layer have peers or not.
    * @param peersOverlap Whether child peers can overlap or not.
    * @param saturated Whether children on this layer must temporally fill the entire parent 
    * duration (true) or not (false).
    */
   public Layer(String id, String description, int alignment, boolean peers, boolean peersOverlap, boolean saturated)
   {
      setId(id);
      setDescription(description);
      setAlignment(alignment);
      setPeers(peers);
      setPeersOverlap(peersOverlap);
      setSaturated(saturated);
      if (!id.equals("graph")) setParentId("graph");
      setParentIncludes(true);
   } // end of constructor

   /**
    * JSON constructor.
    * <p>All attributes are copied, including those that are not bean attributes; other
    * are stored as map values.
    * @param json JSON representation of the layer.
    */
   public Layer(JsonObject json)
   {
      fromJson(json);
   } // end of constructor

   /**
    * Returns a list of ancestor layers (parent, grandparent, etc.).
    * @return A set of ancestor layers, ordered by distance from this annotation (i.e. parent
    * first, then grandparent, etc.). 
    */
   public LinkedHashSet<Layer> getAncestors()
   {
      LinkedHashSet<Layer> ancestors = new LinkedHashSet<Layer>();
      Layer ancestor = getParent(); // don't include ourselves in the list.
      while (ancestor != null  
             && !ancestors.contains(ancestor)) // (guard against cycles, just in case)
      {
         ancestors.add(ancestor);
         ancestor = ancestor.getParent();
      } // next ancestor
      return ancestors;
   } // end of getAncestors()
  
   /**
    * Determines whether the given layer is an ancestor of this layer.
    * @param layerId
    * @return true if the given layer is an ancestor, and false otherwise.
    */
   public boolean isAncestor(String layerId)
   {
      Layer ancestor = getParent(); // don't include ourselves.
      while (ancestor != null)
      {
         if (ancestor.getId().equals(layerId)) return true;
         ancestor = ancestor.getParent();
      } // next ancestor
      return false;
   } // end of isAncestor()
  
   /**
    * Determines the first ancestor layer this layer has in common with the given layer.
    * This may return the graph layer itself, if there are no earlier common ancestors.
    * "Ancestors" is inclusive in the sense that if either annotation is an ancestor of
    * the other, it will be returned.
    * @param other The other layer.
    * @return The first ancestor layer this layer has in common with the given layer, or
    * null if the hierarchy is incomplete and no common ancestor was found.
    */
   public Layer getFirstCommonAncestor(Layer other)
   {
      if (other == null) return null;
      HashSet<Layer> ourAncestors = new HashSet<Layer>();
      Layer ancestor = this; // include ourselves in the list.
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
    * Returns the maximum depth of the descendents of the layer. 
    * <p> e.g. 
    * <ul> 
    *  <li>if the layer has no children descendent depth = 0</li>
    *  <li>if the layer has children, but no grandchildren (i.e. no child layers have children) 
    *   descendent depth = 1</li>
    *  <li>if the layer has children, and some have grandchildren, but there are no 
    *   great-granchildren (i.e. no grandchild layers have children) descendent depth = 2</li>
    * </ul> 
    * @return The maximum descendent depth.
    */
   public int getDescendentDepth()
   {
      int depth = 0;
      for (Layer child : getChildren().values())
      {
         depth = Math.max(depth, child.getDescendentDepth() + 1);
      } // next child
      return depth;
   } // end of getDescendentDepth()

   /**
    * Determines whether the given layer is a descendant of this layer.
    * @param layerId
    * @return true if the given layer is a descendant, and false otherwise.
    */
   public boolean isDescendant(String layerId)
   {
      for (Layer child : getChildren().values())
      {
         // this this child the layer?
         if (child.getId().equals(layerId)) return true;
         // are any of this child's descendants the layer?
         if (child.isDescendant(layerId)) return true;
      } // next child
      return false;
   } // end of isDescendant()

   // java.lang.Object overrides:

   /**
    * A string representation of the object.
    * @return A string representation of the object.
    */
   public String toString()
   {
      if (getId() != null)
      {
         return getId();
      }
      else
      { // id isn't set, so we return the map's hashCode and hope for the best
         return "[unnamed layer]";
      }
   } // end of toString()

   /**
    * Computes a hashCode for the object. 
    * <p>Map (base class) has a very mutable hashCode, but we don't want Anchor hashcodes changing
    * whenever arbitrary elements change (otherwise they get lost in hash-based collections,
    * etc.).  So this implementation returns the hashcode of the anchor's id. 
    * @return Object's hashCode.
    */
   public int hashCode()
   {
      if (getId() != null)
      {
         return getId().hashCode();
      }
      else
      { // id isn't set, so we return the map's hashCode and hope for the best
         return super.hashCode();
      }
   } // end of hashCode()
   
   /**
    * Indicates whether an object is "equal to" this one.
    * @param obj The object to compare to.
    * @return true if obj is an Anchor with the same id as this one, false otherwise.
    */
   public boolean equals(Object obj)
   {
      if (obj == this) return true;
      if (obj instanceof Layer)
      {
         Layer other = (Layer)obj;
         if (getId() != null && other.getId() != null)
         {
            return other.getId().equals(getId());
         }
      }
      return false;
   } // end of equals()


} // end of class Layer
