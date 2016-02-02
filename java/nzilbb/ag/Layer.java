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
import java.util.Vector;
import java.util.Set;

/**
 * Annotation graph layer.
 * <p>Each annotation belongs to a Layer, which defines what layer its parent annotation is on, 
 * and how its anchors relate to its parent annotation's anchors.
 * <p>The primary defining characteristics of a layer are:
 * <ul>
 *  <li>{@link #getParentId() parentId} - what layer parent annotations are on</li>
 *  <li>{@link #getAlignment() alignment} - whether this annotation is {@link Constants#ALIGNMENT_NONE not aligned} (i.e. simply tags the parent annotation), or defines a {@link Constants#ALIGNMENT_INSTANT instant}, or an {@link Constants#ALIGNMENT_INTERVAL interval}.</li>
 *  <li>{@link #getPeers() peers} - whether there is one child annotation per parent (e.g. orthography tags on word tokens), or children can have peers (e.g. word tokens in a turn, or different POS tags on a word token)</li>
 *  <li>{@link #getPeersOverlap peersOverlap} - if there are peers, whether they can overlap (e.g. topic tags within a graph, syntactic parse annotations within a turn) or not (e.g. word tokens within a turn, utterance partitions within a turn, turn annotations for a participant)</li>
 *  <li>{@link #getParentIncludes() parentIncludes} - whether the child annotation's anchors must have offsets between the parent's offsets (e.g. word tokens within a turn, tag annotations, syntactic parse annotations within a turn) or not (e.g. syntactic dependency annotations linking one word to another)</li>
 *  <li>{@link #getSaturated() saturated} - whether the child annotations fully cover the duration of the parent (e.g. utterance partitions within a turn, tag annotations) or not (e.g. word tokens within a turn, which may have leading, intervening, or trailing pauses)</li>
 * </ul>
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class Layer
   extends TrackedMap
{
   // NB if this is updated, please also update the @return javadoc attribute on getClonedAttributes()
   private static String[] aClonedAttributes = {"id", "parentId", "description", "alignment", "peers", "peersOverlap", "parentIncludes", "saturated"};
   /**
    * Keys for attributes that are cloned - i.e. when an object is cloned, only these attributes are copied into the clone.
    * <p>LinkedHashSet is used so that attributes are iterated in the order they're defined in aClonedAttributes (which is the order shown in the documentation of {@link #getClonedAttributes()}).
    */
   protected static final Set<String> clonedAttributes = new LinkedHashSet<String>(java.util.Arrays.asList(aClonedAttributes));

   /**
    * Keys for attributes that are cloned - i.e. when an object is cloned, only these attributes are copied into the clone.
    * @return "id", "parentId", "description", "alignment", "peers", "peersOverlap", "parentIncludes", "saturated"
    */
   public Set<String> getClonedAttributes()
   {
      return clonedAttributes;
   } // end of getTrackedAttributes()

   // Attributes stored in HashMap:
   
   /**
    * Getter for <i>parentId</i>: The layer's parent layer id
    * @return The layer's parent layer id
    */
   public String getParentId() { try { return (String)get("parentId"); } catch(ClassCastException exception) {return null;} }
   /**
    * Setter for <i>parentId</i>: The layer's parent layer id
    * @param parentId The layer's parent layer id
    */
   public void setParentId(String parentId) { put("parentId", parentId); }

   /**
    * Getter for <i>description</i>: The description of the layer.
    * @return The description of the layer.
    */
   public String getDescription() { try { return (String)get("description"); } catch(ClassCastException exception) {return null;} }
   /**
    * Setter for <i>description</i>: The description of the layer.
    * @param description The description of the layer.
    * @see Constants#ALIGNMENT_NONE
    * @see Constants#ALIGNMENT_INSTANT
    * @see Constants#ALIGNMENT_INTERVAL
    */
   public void setDescription(String description) { put("description", description); }

   /**
    * Getter for <i>alignment</i>: The layer's alignment - 0 for none, 1 for point alignment, 2 for interval alignment.
    * @return The layer's alignment - 0 for none, 1 for point alignment, 2 for interval alignment.
    * @see Constants#ALIGNMENT_NONE
    * @see Constants#ALIGNMENT_INSTANT
    * @see Constants#ALIGNMENT_INTERVAL
    */
   public int getAlignment() { try { return ((Integer)get("alignment")).intValue(); } catch(ClassCastException cc) {return 0;} catch(NullPointerException np) {return 0;} }
   /**
    * Setter for <i>alignment</i>: The layer's alignment - 0 for none, 1 for point alignment, 2 for interval alignment.
    * @param alignment The layer's alignment - 0 for none, 1 for point alignment, 2 for interval alignment.
    * @see Constants#ALIGNMENT_NONE
    * @see Constants#ALIGNMENT_INSTANT
    * @see Constants#ALIGNMENT_INTERVAL
    */
   public void setAlignment(int alignment) { put("alignment", alignment); }

   /**
    * Getter for <i>peers</i>: Whether children have peers or not.
    * @return Whether children have peers or not.
    */
   public boolean getPeers() { try { return ((Boolean)get("peers")).booleanValue(); } catch(ClassCastException cc) {return false;} catch(NullPointerException np) {return false;} }
   /**
    * Setter for <i>peers</i>: Whether children have peers or not.
    * @param peers Whether children have peers or not.
    */
   public void setPeers(boolean peers) { put("peers", peers); }

   /**
    * Getter for <i>peersOverlap</i>: Whether child peers can overlap or not.
    * @return Whether child peers can overlap or not.
    */
   public boolean getPeersOverlap() { try { return ((Boolean)get("peersOverlap")).booleanValue(); } catch(ClassCastException cc) {return false;} catch(NullPointerException np) {return false;} }
   /**
    * Setter for <i>peersOverlap</i>: Whether child peers can overlap or not.
    * @param peersOverlap Whether child peers can overlap or not.
    */
   public void setPeersOverlap(boolean peersOverlap) { put("peersOverlap", peersOverlap); }

   /**
    * Getter for <i>parentIncludes</i>: Whether the parent t-includes the child.
    * @return Whether the parent t-includes the child.
    */
   public boolean getParentIncludes() { try { return ((Boolean)get("parentIncludes")).booleanValue(); } catch(ClassCastException cc) {return false;} catch(NullPointerException np) {return false;} }
   /**
    * Setter for <i>parentIncludes</i>: Whether the parent t-includes the child.
    * @param parentIncludes Whether the parent t-includes the child.
    */
   public void setParentIncludes(boolean parentIncludes) { put("parentIncludes", parentIncludes); }

   /**
    * Getter for <i>saturated</i>: Whether children must temporally fill the entire parent duration (true) or not (false).
    * @return Whether children must temporally fill the entire parent duration (true) or not (false).
    */
   public boolean getSaturated() { try { return ((Boolean)get("saturated")).booleanValue(); } catch(ClassCastException cc) {return false;} catch(NullPointerException np) {return false;} }
   /**
    * Setter for <i>saturated</i>: Whether children must temporally fill the entire parent duration (true) or not (false).
    * @param saturated Whether children must temporally fill the entire parent duration (true) or not (false).
    */
   public void setSaturated(boolean saturated) { put("saturated", saturated); }

   
   // Attributes stored outside HashMap, so that JSONifying the HashMap doesn't result in infinite recursion


   /**
    * List of annotations on this layer.
    * @see #getAnnotations()
    * @see #setAnnotations(Vector)
    */
   protected Vector<Annotation> annotations = new Vector<Annotation>();
   /**
    * Getter for {@link #annotations}: List of annotations on this layer.
    * @return List of annotations on this layer.
    */
   public Vector<Annotation> getAnnotations() { return annotations; }
   /**
    * Setter for {@link #annotations}: List of annotations on this layer.
    * @param newAnnotations List of annotations on this layer.
    */
   public void setAnnotations(Vector<Annotation> newAnnotations) { annotations = newAnnotations; }
   

   /**
    * Getter for <i>children</i>: Child layers.
    * @return Child layers.
    */
   @SuppressWarnings("unchecked")
   public LinkedHashMap<String,Layer> getChildren() 
   {
      if (!containsKey("children"))
      {
	 setChildren(new LinkedHashMap<String,Layer>());
      }

      try 
      { 
	 return (LinkedHashMap<String,Layer>)get("children"); 
      } 
      catch(ClassCastException exception) 
      { 
	 LinkedHashMap<String,Layer> children = new LinkedHashMap<String,Layer>();
	 setChildren(children); 
	 return children; 
      } 
   }
   /**
    * Setter for <i>children</i>: Child layers.
    * @param children Child layers.
    */
   public void setChildren(LinkedHashMap<String,Layer> children) { put("children", children); }


   
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
   public void setParent(Layer newParent) 
   { 
      parent = newParent; 
      if (parent != null)
      {
	 setParentId(parent.getId());
	 parent.getChildren().put(getId(), this);
      }
   }

   // Methods:
      
   /**
    * Default constructor
    */
   public Layer()
   {
   } // end of constructor

   /**
    * Basic constructor
    * @param id The layer's identifier.
    * @param description The description of the layer.
    * @param parentId The layer's parent layer id
    * @param alignment The layer's alignment - 0 for none, 1 for point alignment, 2 for interval alignment.
    * @param peers Whether children have peers or not.
    * @param peersOverlap Whether child peers can overlap or not.
    * @param parentIncludes Whether the parent t-includes the child.
    * @param saturated Whether children must temporally fill the entire parent duration (true) or not (false).
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
    * @param alignment The layer's alignment - 0 for none, 1 for point alignment, 2 for interval alignment.
    * @param peers Whether children have peers or not.
    * @param peersOverlap Whether child peers can overlap or not.
    * @param saturated Whether children must temporally fill the entire parent duration (true) or not (false).
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
    * Access the annotations on this layer, as an array (for environments that deal better with arrays than collections).
    * @return The annotations on this layer.
    */
   public Annotation[] annotations()
   {
      return getAnnotations().toArray(new Annotation[0]);
   } // end of annotations()

   /**
    * Returns a list of ancestor layers (parent, grandparent, etc.).
    * @return A set of ancestor layers, ordered by distance from this annotation (i.e. parent first, then grandparent, etc.).
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
    * Determines the first ancestor layer this layer has in common with the given layer. This may return the graph layer itself, if there are no earlier common ancestors. "Ancestors" is inclusive in the sense that if either annotation is an ancestor of the other, it will be returned.
    * @param other The other layer.
    * @return The first ancestor layer this layer has in common with the given layer, or null if the hierarchy is incomplete and no common ancestor was found.
    */
   public Layer getFirstCommonAncestor(Layer other)
   {
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

   
   // java.lang.Object overrides:

   /**
    * A string representation of the object.
    * @return A string representation of the object.
    */
   public String toString()
   {
      if (containsKey("id"))
      {
	 return get("id").toString();
      }
      else
      { // id isn't set, so we return the map's hashCode and hope for the best
	 return "[unnamed layer]";
      }
   } // end of toString()

   /**
    * Computes a hashCode for the object. 
    * <p>Map (base class) has a very mutable hashCode, but we don't want Anchor hashcodes changing whenever arbitrary elements change (otherwise they get lost in hash-based collections, etc.).  So this implementation returns the hashcode of the anchor's id.
    * @return Object's hashCode.
    */
   public int hashCode()
   {
      if (containsKey("id"))
      {
	 return get("id").hashCode();
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
