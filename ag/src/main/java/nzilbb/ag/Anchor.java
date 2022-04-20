//
// Copyright 2015-2022 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import javax.json.JsonObject;
import nzilbb.util.ClonedProperty;

/**
 * Annotation graph anchor - a node of the graph.
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class Anchor extends TrackedMap implements Comparable<Anchor> {
  // NB if this is updated, please also update the @return javadoc attribute on getTrackedAttributes()
  private static String[] aTrackedAttributes = {"offset"};
  /**
   * Keys for attributes that are change-tracked - i.e. when a new value is set for any
   * of these attributes, and {@link TrackedMap#getTracker()} is set, the change is registered.
   * <p>HashSet is used for this because there's only one tracked attribute, so iteration
   * order is unimportant.
   */
  protected static final Set<String> trackedAttributes = new HashSet<String>(java.util.Arrays.asList(aTrackedAttributes));

  /**
   * Keys for attributes that are change-tracked - i.e. when a new value is set for any
   * of these attributes, and {@link TrackedMap#getTracker()} is set, the change is registered.
   * @return "offset"
   */
  public Set<String> getTrackedAttributes() {
    return trackedAttributes;
  } // end of getTrackedAttributes()

  // Attributes stored in HashMap:

  /**
   * The anchor's time/character offset.
   */
  protected Double offset;
  /**
   * Getter for <i>offset</i>: The anchor's time/character offset.
   * @return The anchor's time/character offset.
   */
  @ClonedProperty @TrackedProperty
  public Double getOffset() { return offset; }
  /**
   * Setter for <i>offset</i>: The anchor's time/character offset.
   * @param offset The anchor's time/character offset.
   * @return A reference to this object. 
   */
  public synchronized Anchor setOffset(Double offset) {
    if (offset != null && offset.isNaN()) offset = null;

    Change change = registerChange("offset", offset);

    this.offset = offset;

    if (offset != null || change != null) {
      if (graph != null) {
        // add this to offsetIndex
        graph.indexAnchor(this);
	    
        // reset indices of related layers
        for (String layerId : startOf.keySet()) graph.indicesByLayer.remove(layerId);
        for (String layerId : endOf.keySet()) graph.indicesByLayer.remove(layerId);
      }
    }
      
    return this;
  }

  // Attributes stored outside HashMap, so that JSONifying the HashMap doesn't
  // result in infinite recursion
   
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
  public void setGraph(Graph newGraph) {
    graph = newGraph;
    if (graph != null) {
      setTracker(graph.getTracker());
    } else {
      setTracker(null);
    }
  }
   
  /**
   * Map of annotations that start with this anchor, keyed on layer id.
   * @see #getStartOf()
   * @see #setStartOf(LinkedHashMap)
   */
  protected LinkedHashMap<String,LinkedHashSet<Annotation>> startOf = new LinkedHashMap<String,LinkedHashSet<Annotation>>();
  /**
   * Getter for {@link #startOf}: Map of annotations that start with this anchor, keyed
   * on layer id. 
   * @return Map of annotations that start with this anchor, keyed on layer id.
   */
  public LinkedHashMap<String,LinkedHashSet<Annotation>> getStartOf() { return startOf; }
  /**
   * Setter for {@link #startOf}: Map of annotations that start with this anchor, keyed
   * on layer id. 
   * @param newStartOf Map of annotations that start with this anchor, keyed on layer id.
   */
  public void setStartOf(LinkedHashMap<String,LinkedHashSet<Annotation>> newStartOf) { startOf = newStartOf; }

  /**
   * Map of annotations that end with this anchor, keyed on layer id.
   * @see #getEndOf()
   * @see #setEndOf(LinkedHashMap)
   */
  protected LinkedHashMap<String,LinkedHashSet<Annotation>> endOf = new LinkedHashMap<String,LinkedHashSet<Annotation>>();
  /**
   * Getter for {@link #endOf}: Map of annotations that end with this anchor, keyed on layer id.
   * @return Map of annotations that end with this anchor, keyed on layer id.
   */
  public LinkedHashMap<String,LinkedHashSet<Annotation>> getEndOf() { return endOf; }
  /**
   * Setter for {@link #endOf}: Map of annotations that end with this anchor, keyed on layer id.
   * @param newEndOf Map of annotations that end with this anchor, keyed on layer id.
   */
  public void setEndOf(LinkedHashMap<String,LinkedHashSet<Annotation>> newEndOf) { endOf = newEndOf; }   
   
  // Methods:
      
  /**
   * Default constructor.
   */
  public Anchor() {
    put("startOf", getStartOf()); // TODO these violate the principle of having only simple values
    put("endOf", getEndOf());
  } // end of constructor

  /**
   * Basic constructor.
   * @param id The anchor's identifier.
   * @param offset The anchor's time/character offset.
   */
  public Anchor(String id, Double offset) {
    setId(id);
    setOffset(offset);
    put("startOf", getStartOf()); // TODO these violate the principle of having only simple values
    put("endOf", getEndOf());
  } // end of constructor

  /**
   * Constructor with confidence.
   * <p>This convenience constructor allows, for example, the creation of an anchor with
   * a confidence value in one step:
   * <pre>
   * Anchor anchor = new Anchor("123", 456.789, Constants.CONFIDENCE_AUTOMATIC);
   * </pre>
   * @param id The anchor's identifier.
   * @param offset The anchor's time/character offset.
   * @param confidence Confidence rating.
   */
  public Anchor(String id, Double offset, Integer confidence) {
    setId(id);
    setOffset(offset);
    setConfidence(confidence);
    put("startOf", getStartOf());
    put("endOf", getEndOf());
  } // end of constructor

  /**
   * Copy constructor.  This copies all attributes of the anchor <em>except</em>
   * <var>id</var>, tracked original values (<var>originalOffset</var>), and attributes
   * whose keys do not begin with an alphanumeric (by convention these are transient
   * attributes), the intention being to create a new anchor that has the same
   * characteristics as <var>other</var> (<var>offset</var>, <var>confidence</var>,
   * etc.), but which is a different anchor with different (initially, no) graph
   * linkages. 
   * @param other The anchor to copy.
   */
  public Anchor(Anchor other) {
    putAll(other);
    Vector<String> keysToRemove = new Vector<String>();
    for (String key : keySet()) {
      if (key.length() > 0 && !Character.isLetterOrDigit(key.charAt(0))) {
        // starts with non-alphanumeric
        keysToRemove.add(key);
      }
    } // next key
    for (String key : keysToRemove) remove(key);
    put("startOf", getStartOf());
    put("endOf", getEndOf());
    clonePropertiesFrom(other, "id");
  } // end of constructor

  /**
   * JSON constructor.
   * <p>All attributes are copied, including those that are not bean attributes; other
   * are stored as map values.
   * @param json JSON representation of the anchor.
   */
  public Anchor(JsonObject json) {
    fromJson(json);
  } // end of constructor

  /**
   * Setter for <i>id</i>: The anchor's identifier.
   * @param id The anchor's identifier.
   */
  public Anchor setId(String id) {
    LinkedHashSet<Annotation> starting = getStartingAnnotations();
    LinkedHashSet<Annotation> ending = getEndingAnnotations();

    // remove the old id from the graph index
    String oldId = getId();
    if (getGraph() != null) getGraph().getAnchors().remove(oldId);
    super.setId(id);
    // add the new id from the graph index
    if (getGraph() != null) getGraph().getAnchors().put(id, this);
      
    // update all annotations that use the anchor
    for (Annotation startsHere : starting) {
      // check it still uses this anchor
      if (startsHere.getStartId() != null
          && startsHere.getStartId().equals(oldId)) {
        startsHere.setStartId(id);
      }
    } // next annotation
    for (Annotation endsHere : ending) {
      // check it still uses this anchor
      if (endsHere.getEndId() != null
          && endsHere.getEndId().equals(oldId)) {
        endsHere.setEndId(id);
      }
    } // next annotation
    return this;
  }   

  /**
   * Gets the original offset of the anchor, before any subsequent calls to 
   * {@link #setOffset(Double)}, since the object was created. 
   * @return The original offset.
   */
  public Double getOriginalOffset() {
    if (tracker != null) {
      Optional<Change> change = tracker.getChange(id, "offset");
      if (change.isPresent()) {
        return (Double)change.get().getOldValue();
      }
    }
    return getOffset(); 
  }
      
  /**
   * Accesses a list of annotations on a given layer that start with this anchor.
   * <p> This may include annotations that have been marked for destruction.
   * @param layerId The given layer ID.
   * @return A list of annotations on the given layer that start here.
   */
  public LinkedHashSet<Annotation> startOf(String layerId) {
    if (!getStartOf().containsKey(layerId)) {
      getStartOf().put(layerId, new LinkedHashSet<Annotation>());
    }
    LinkedHashSet<Annotation> annotations = getStartOf().get(layerId);
    // check they still really are linked
    Iterator<Annotation> iAnnotations = annotations.iterator();
    LinkedHashSet<Annotation> startsToSet = new LinkedHashSet<Annotation>();
    String id = getId();
    while (iAnnotations.hasNext()) {
      Annotation a = iAnnotations.next();
      if (a instanceof Graph) continue;
      if (a.getStartId() == null) continue;
      if (!a.getStartId().equals(id)) {
        iAnnotations.remove();
        if (a.getStart() != null) {
          startsToSet.add(a);
        }
      }
    } // next annotation
    for (Annotation a : startsToSet) {
      if (!a.getStart().startOf.containsKey(a.getLayerId())) {
        a.getStart().startOf.put(a.getLayerId(), new LinkedHashSet<Annotation>());
      }
      a.getStart().startOf.get(a.getLayerId()).add(a);
    }
    return annotations;
  } // end of startOf()

  /**
   * Determines whether the anchor is the start of an annotation on the given layer.
   * @param layerId The ID of the layer to test for.
   * @return true if the anchor is the start of an annotation on the given layer, and
   * false otherwise. 
   */
  public boolean isStartOn(String layerId) {
    if (!getStartOf().containsKey(layerId)) return false;
    return getStartOf().get(layerId).size() > 0;
  } // end of isStartOn()

  /**
   * Determines whether the anchor is the start of an annotation on the given layer.
   * @param layerId The ID of the layer to test for.
   * @return true if the anchor is the start of an annotation on the given layer, and
   * false otherwise. 
   */
  public boolean isEndOn(String layerId) {
    if (!getEndOf().containsKey(layerId)) return false;
    return getEndOf().get(layerId).size() > 0;
  } // end of isEndOn()

  /**
   * Accesses a list of annotations on all layers that start with this anchor.
   * <p> This may include annotations that have been marked for destruction.
   * @return A list of annotations that start here.
   */
  public LinkedHashSet<Annotation> getStartingAnnotations() {
    LinkedHashSet<Annotation> startingHere = new LinkedHashSet<Annotation>();      
    for (String layerId : getStartOf().keySet()) {
      startingHere.addAll(startOf(layerId));
    }
    return startingHere;
  } // end of getStartingAnnotations()

  /**
   * Accesses a list of annotations on a given layer that end with this anchor.
   * <p> This may include annotations that have been marked for destruction.
   * @param layerId The given layer ID.
   * @return A list of annotations on the given layer that end here.
   */
  public LinkedHashSet<Annotation> endOf(String layerId) {
    if (!getEndOf().containsKey(layerId)) {
      getEndOf().put(layerId, new LinkedHashSet<Annotation>());
    }
    LinkedHashSet<Annotation> annotations = getEndOf().get(layerId);
    // check they still really are linked
    Iterator<Annotation> iAnnotations = annotations.iterator();
    String id = getId();
    LinkedHashSet<Annotation> endsToSet = new LinkedHashSet<Annotation>();
    while (iAnnotations.hasNext()) {
      Annotation a = iAnnotations.next();
      if (a instanceof Graph) continue;
      if (a.getEndId() == null) continue;
      if (!a.getEndId().equals(id)) {
        iAnnotations.remove();
        if (a.getEnd() != null) {
          endsToSet.add(a);
        }
      }
    } // next annotation
    for (Annotation a : endsToSet) {
      if (!a.getEnd().endOf.containsKey(a.getLayerId())) {
        a.getEnd().endOf.put(a.getLayerId(), new LinkedHashSet<Annotation>());
      }
      a.getEnd().endOf.get(a.getLayerId()).add(a);
    }
    return annotations;
  } // end of endOf()

  /**
   * Accesses a list of annotations on all layers that end with this anchor.
   * <p> This may include annotations that have been marked for destruction.
   * @return A list of annotations that end here.
   */
  public LinkedHashSet<Annotation> getEndingAnnotations() {
    LinkedHashSet<Annotation> endingHere = new LinkedHashSet<Annotation>();      
    for (String layerId : getEndOf().keySet()) {
      endingHere.addAll(endOf(layerId));
    }
    return endingHere;
  } // end of getEndingAnnotations()

  /**
   * Sets the start of all annotations that start here, to the given anchor.
   * @param newStart The new start anchor
   */
  public void moveStartingAnnotations(Anchor newStart) {
    for (Annotation starting : getStartingAnnotations()) {
      starting.setStart(newStart);
    } // next annotion ending here
  } // end of moveStartingAnnotations()
   
  /**
   * Sets the end of all annotations that end here, to the given anchor.
   * @param newEnd The new end anchor
   */
  public void moveEndingAnnotations(Anchor newEnd) {
    for (Annotation ending : getEndingAnnotations()) {
      ending.setEnd(newEnd);
    } // next annotion ending here
  } // end of moveEndingAnnotations()
  
  /**
   * Determines whether the anchor is the start or end of any annotation.
   * @return true if the anchor is the start or end of any annotation not marked for destruction.
   */
  public boolean isLinked() {
    for (String layerId : getStartOf().keySet()) {
      for (Annotation a : startOf(layerId)) {
        if (a.getChange() != Change.Operation.Destroy
            && a.getStart() == this) {
          return true;
        }
      }
    }
    for (String layerId : getEndOf().keySet()) {
      for (Annotation a : endOf(layerId)) {
        if (a.getChange() != Change.Operation.Destroy
            && a.getEnd() == this) {
          return true;
        }
      }
    }
    return false;
  } // end of isLinked()

  /**
   * Returns the minimum possible offset for this anchor.  If the offset is set,
   * the minimum possible offset is the same as {@link #getOffset()}. Otherwise
   * the annotation graph is traversed to find the anchor with highest preceding offset.
   * <p> If the graph is traversed, the resulting offset is cached in an attribute called 
   * <q>"@offsetMin"</q>.
   * @return The minimum possible offset for this anchor, or null if the it can't be determined
   * from the anchor or its graph.
   */
  public Double getOffsetMin() {
    if (getOffset() != null) return getOffset();
    if (containsKey("@offsetMin")) return (Double)get("@offsetMin");

    // go backward through the graph to find all following set offsets
    Double dGreatestPrecedingOffset = null;
    for(Annotation a : getEndingAnnotations()) {
      if (!a.getInstantaneous()) {
        Double dOffsetMin = a.getStart().getOffsetMin();
        if (dOffsetMin != null
            && (dGreatestPrecedingOffset == null
                || dGreatestPrecedingOffset < dOffsetMin)) {
          dGreatestPrecedingOffset = dOffsetMin;
        }
      }
    }
    put("@offsetMin", dGreatestPrecedingOffset);
    return dGreatestPrecedingOffset;
  } // end of getOffsetMin()

  /**
   * Returns the maximum possible offset for this anchor.  If the offset is set,
   * the maximum possible offset is the same as {@link #getOffset()}. Otherwise
   * the annotation graph is traversed to find the anchor with lowest following offset.
   * <p> If the graph is traversed, the resultinf offset is cached in an attribute called 
   * <q>"@offsetMax"</q>.
   * @return The maximum possible offset for this anchor, or null if the it can't be determined
   * from the anchor or its graph.
   */
  public Double getOffsetMax() {
    if (getOffset() != null) return getOffset();
    if (containsKey("@offsetMax")) return (Double)get("@offsetMax");
    // go forward through the graph to find all following set offsets
    Double dLeastFollowingOffset = null;
    for(Annotation a : getStartingAnnotations()) {
      if (!a.getInstantaneous()) {
        Double dOffsetMax = a.getEnd().getOffsetMax();
        if (dOffsetMax != null
            && (dLeastFollowingOffset == null
                || dLeastFollowingOffset > dOffsetMax)) {
          dLeastFollowingOffset = dOffsetMax;
        }
      }
    }
    put("@offsetMax", dLeastFollowingOffset);
    return dLeastFollowingOffset;
  } // end of getOffsetMax()
   
  /**
   * Gets all anchors that are linked to this anchor via annotations <em>before</em> this
   * one in the graph. 
   * <p>This set excludes this anchor and includes instantaneous anchors.
   * @return A set of all anchors that are linked to this anchor and prior to it, ordered
   * by reverse-chain order. 
   */
  public LinkedHashSet<Anchor> getPreceding() {
    Vector<Anchor> anchors = new Vector<Anchor>();
    anchors.add(this);      
    for (int i = 0; i < anchors.size(); i++) {
      Anchor anchor = anchors.elementAt(i);
      for(Annotation annotation : anchor.getEndingAnnotations()) {
        Anchor next = annotation.getStart();
        if (!anchors.contains(next)) { // haven't already visited this node
          anchors.add(next);
        }
      } // next annotation that starts here
    } // next anchor

      // ensure this anchor is not in the set
    LinkedHashSet<Anchor> preceding = new LinkedHashSet<Anchor>(anchors);
    preceding.remove(this);
    return preceding;
  } // end of getPrecedingAnchors()

  /**
   * Determines whether the given anchor is linked to this anchor via annotations
   * <em>before</em> this one in the graph. 
   * @param other The other anchor.
   * @return True if the other anchor precedes this one in a chain, false otherwise.
   */
  public boolean follows(Anchor other) {
    return other.precedes(this);
  }
  
  /**
   * Determines whether the given anchor is linked to this anchor via annotations
   * <em>after</em> this one in the graph. 
   * @param other The other anchor.
   * @return True if the other anchor follows this one in a chain, false otherwise.
   */
  public boolean precedes(Anchor other) {
    if (this == other) return false;

    Vector<Anchor> anchors = new Vector<Anchor>();
    anchors.add(this);      
    for (int i = 0; i < anchors.size(); i++) {
      Anchor anchor = anchors.elementAt(i);
      for(Annotation annotation : anchor.getStartingAnnotations()) {
        Anchor next = annotation.getEnd();
        if (next == other) {
          return true;
        }
        if (next != null && !anchors.contains(next)) { // haven't already visited this node
          anchors.add(next);
        }
      } // next annotation that starts here
    } // next anchor

    return false;
  } // end of getPrecedingAnchors()

  /**
   * Gets all anchors that are linked to this anchor via annotations <em>after</em> this
   * one in the graph. 
   * <p>This set excludes this anchor and includes instantaneous anchors.
   * @return A set of all anchors that are linked to this anchor and following it,
   * ordered by chain order. 
   */
  public LinkedHashSet<Anchor> getFollowing() {
    Vector<Anchor> anchors = new Vector<Anchor>();
    anchors.add(this);      
    for (int i = 0; i < anchors.size(); i++) {
      Anchor anchor = anchors.elementAt(i);
      for(Annotation annotation : anchor.getStartingAnnotations()) {
        Anchor next = annotation.getEnd();
        if (!anchors.contains(next)) { // haven't already visited this node
          anchors.add(next);
        }
      } // next annotation that starts here
    } // next anchor

      // ensure this anchor is not in the set
    LinkedHashSet<Anchor> following = new LinkedHashSet<Anchor>(anchors);
    following.remove(this);
    return following;
  } // end of getPrecedingAnchors()

  /**
   * Finds an annotation for which this is the start anchor and the given anchor is the
   * end anchor.
   * @param end The returned annotation's end anchor.
   * @return An annotation for which this is the start anchor and the given anchor is the
   * end anchor, or null if no such annotation exists. 
   */
  public Annotation annotationTo(Anchor end) {
    // for each layer in StartAnnotations
    for (String layerId : startOf.keySet()) {
      Annotation a = annotationTo(end, layerId);
      if (a != null) return a;
    } // next layer
	 
    return null;
  } // end of annotationTo()

  /**
   * Finds an annotation for which this is the start anchor and the given anchor is the
   * end anchor, on the given layer 
   * @param end The returned annotation's end anchor.
   * @param layerId The returned annotation's layer.
   * @return An annotation for which this is the start anchor and the given anchor is the
   * end anchor, or null if no such annotation exists. 
   */
  public Annotation annotationTo(Anchor end, String layerId) {
    if (startOf.get(layerId) == null) return null;
    // for each annotation for this layer
    for (Annotation a : startOf.get(layerId)) {
      // is the end anchor the same as endAnchor?
      if (a.getEnd() != null && a.getEnd().equals(end)) return a;
    } // next annotation
	 
    return null;
  } // end of annotationTo()

  // java.lang.Object overrides:

  /**
   * A string representation of the object.
   * @return A string representation of the object.
   */
  public String toString() {
    if (getOffset() != null)
      return ""+getOffset();
    else
      return "["+getId()+"]";
  } // end of toString()

  // Comparable implementation
   
  /**
   * Compares two anchors. By default, this is by the values of {@link #getOffset()}.  If
   * either anchor has an unset anchor, or their anchors are equal, comparison is by
   * {@link #getId()}. 
   * @param o The other anchor.
   * @return A negative integer, zero, or a positive integer as this anchor has an offset
   * that is less than, equal to, or greater than the offset of the specified anchor. If
   * either anchor has an unset anchor, or their anchors are equal, comparison is by
   * {@link #getId()}. 
   */
  public int compareTo(Anchor o) {
    if (this.equals(o)) return 0;
    if (getOffset() == null || o.getOffset() == null || getOffset().equals(o.getOffset())) {
      if (getId() == null) return -1;
      return getId().compareTo(o.getId());
    } else {
      return getOffset().compareTo(o.getOffset());
    }
  } // end of compareTo()   

} // end of class Anchor
