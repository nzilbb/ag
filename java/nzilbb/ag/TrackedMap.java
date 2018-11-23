//
// Copyright 2015-2017 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.Set;
import java.util.TreeSet;
import java.util.Arrays;
import java.util.Date;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import org.json.IJSONableBean;

/**
 * Base class for annotation graph classes, which allows registered attributes to have their changes and original values tracked.
 * <p>Annotation graph classes (in particular {@link Annotation} and {@link Anchor} are defined as Maps not just to allow tracking of changes to registered attributes (key/value pairs), but also to allow them to easily be informally extended or tagged at runtime. This is with two particular possibilities in mind:
 * <ul>
 *  <li>An annotation is defined as a directed graph edge with a <var>label</var> and a <var>type</var> (which in this implementation is actually <var>layerId</var>), and an anchor is a  graph node with an optional <var>offset</var>, but there is scope of an annotation to include other attributes - not alternative annotation labels, but rather extra information about the annotation itself, which might include provenance, authorship, etc.  In particular, nzilbb.ag annotation graphs include, for both annotations and anchors, an indication of <var>confidence</var>; in nzilbb.ag all anchors are given offsets, but some offsets are more certain than others - an offset may have been manually aligned by a human annotator (high confidence), or may have been arrived at by an automated forced-alignment Process (lower confidence), or may simply have been calculated by linear interpolation between two more certain anchors (very low confidence).  Rather than include <var>confidence</var> as a formal attribute of the Annotation class, this is implemented by setting an attribute on the anchor (the same notion also applies to anchors), which is processed by nzilbb.ag annotators, but can be ignored by other processing. Such 'sticky' attributes, which should ideally be serialized if possible, for storage or transfer, are assumed to have keys that start with an alphabetic character - e.g. <var>"confidence"</var> - in contrast to 'transient' attributes mentioned below.</li>
 *  <li>During processing, it may be desirable or necessary to tag annotations or anchors in some way, e.g. to mark them as 'already visited' or 'already processed' by some traversal process, or to link together entities during merging of graphs, etc. This can be easily achieved by simply setting ad-hoc attributes on the entity as required.  Such 'transient' attributes, which should ideally not be serialized for storage or transfer, are assumed to have keys that start with a non-alphabetic character - e.g. <var>"@otherGraphConterpart</var> - in contrast to 'sticky' attributes mentioned above.</li>
 * </ul>
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class TrackedMap
   extends LinkedHashMap<String,Object>
   implements Cloneable, IJSONableBean
{
   // Attributes stored in HashMap:

   /**
    * Keys for attributes that are change-tracked - i.e. when a new value is set for any of these attributes, the original value is saved in the map with the given key prefixed by "original" - e.g. if "label" is changed, then "originalLabel" will contain its original value.
    */
   private static final TreeSet<String> trackedAttributes = new TreeSet<String>();
   /**
    * Keys for attributes that are change-tracked - i.e. when a new value is set for any of these attributes, the original value is saved in the map with the given key prefixed by "original" - e.g. if "label" is changed, then "originalLabel" will contain its original value.
    * @return A set of attributes whose changes are tracked.
    */
   public Set<String> getTrackedAttributes()
   {
      return trackedAttributes;
   } // end of getTrackedAttributes()

   /**
    * Keys for attributes that are cloned - i.e. when a subclass object is cloned, only these attributes are copied into the clone.
    * @return A set of attributes whose values are cloned.
    */
   public Set<String> getClonedAttributes()
   {
      return trackedAttributes;
   } // end of getTrackedAttributes()

   /**
    * IJSONableBean implementation.
    * @return The names of fields to be obtained from the object.
    */
   public String[] JSONAttributes()
   {
      return getClonedAttributes().toArray(new String[0]);
   }

   /**
    * The object's identifier.
    */
   protected String id;
   /**
    * Getter for <i>id</i>: The object's identifier.
    * @return The object's identifier.
    */
   public String getId() { return id; }
   /**
    * Setter for <i>id</i>: The object's identifier.
    * @param id The object's identifier.
    */
   public void setId(String id) { this.id = id; }
   
   /**
    * Confidence rating.
    * @see #getConfidence()
    * @see #setConfidence(Integer)
    */
   protected Integer confidence;
   /**
    * Getter for {@link #confidence}: Confidence rating.
    * @return Confidence rating.
    */
   public Integer getConfidence() { return confidence; }
   /**
    * Setter for {@link #confidence}: Confidence rating.
    * @param newConfidence Confidence rating.
    */
   public void setConfidence(Integer newConfidence) { confidence = newConfidence; }


   /**
    * Name of the person or system that created or changed this entity.
    * @see #getAnnotator()
    * @see #setAnnotator(String)
    */
   protected String annotator;
   /**
    * Getter for {@link #annotator}: Name of the person or system that created or changed this entity.
    * @return Name of the person or system that created or changed this entity.
    */
   public String getAnnotator() { return annotator; }
   /**
    * Setter for {@link #annotator}: Name of the person or system that created or changed this entity.
    * @param newAnnotator Name of the person or system that created or changed this entity.
    */
   public void setAnnotator(String newAnnotator) { annotator = newAnnotator; }

   /**
    * Date/time this entity was created or changed.
    * @see #getWhen()
    * @see #setWhen(Date)
    */
   protected Date when;
   /**
    * Getter for {@link #when}: Date/time this entity was created or changed.
    * @return Date/time this entity was created or changed.
    */
   public Date getWhen() { return when; }
   /**
    * Setter for {@link #when}: Date/time this entity was created or changed.
    * @param newWhen Date/time this entity was created or changed.
    */
   public void setWhen(Date newWhen) { when = newWhen; }

   // Methods:
   
   /**
    * Default constructor
    */
   public TrackedMap()
   {
   } // end of constructor
   
   /**
    * Gets the original value of the given key, before any subsequent calls to {@link #put(String,Object)}, since the object was created or {@link #commit()} was called.
    * @param key The attribute name.
    * @return The original label.
    */
   protected Object getOriginal(String key)
   {
      String originalValueKey = "original" + key.substring(0,1).toUpperCase() + key.substring(1);
      if (containsKey(originalValueKey))
      {
	 return super.get(originalValueKey);
      }
      else
      {
	 try
	 {
	    Method getter = getter(this, key);
	    return getter.invoke(this);
	 }
	 catch(IllegalAccessException exception)
	 {
	    System.err.println("getOriginal [" + this + "] - " + key);
	 }
	 catch(InvocationTargetException exception)
	 {
	    System.err.println("getOriginal [" + this + "] - " + key);
	 }
	 return null;
      }
   } // end of getOriginalLabel()

   /**
    * Registers a change to a tracked attribute, by setting the "original..." attribute, and returning a corresponding change for the given attribute.
    * @param key
    * @param value
    * @return Returns an Update change.
    */
   protected Change registerChange(String key, Object value)
   {
      String originalValueKey = "original" + key.substring(0,1).toUpperCase() + key.substring(1);
      if (!containsKey(originalValueKey)) // only if it's not already registered...
      {
	 try
	 {
	    Method getter = getter(this, key);
	    assert getter != null : "registerChange: getter != null : " + key + ", " + value + " - " + this.getClass().getName();
	    put(originalValueKey, getter.invoke(this));
	 }
	 catch(NullPointerException exception)
	 {
	    System.err.println("registerChange [" + this + "] - " + key + " = " + value + " :: " + exception);
	    exception.printStackTrace(System.err);
	 }
	 catch(IllegalAccessException exception)
	 {
	    System.err.println("registerChange [" + this + "] - " + key + " = " + value + " :: " + exception);
	 }
	 catch(InvocationTargetException exception)
	 {
	    System.err.println("registerChange [" + this + "] - " + key + " = " + value + " :: " + exception.getCause());
	    if (exception.getCause() != null)
	    {
	       exception.getCause().printStackTrace(System.err);
	    }
	 }
      }
      return new Change(Change.Operation.Update, this, key, value);
   } // end of registerChange()

   /**
    * Marks the object for deletion.
    * <p>This method is not called "delete()" because "delete" is a reserved word in Javascript, and we want these objects to be manipulable from Javascript.
    * @return The changes made during this operation.
    */
   public Change destroy()
   {
      put("@destroy", Boolean.TRUE);
      return new Change(Change.Operation.Destroy, this);
   } // end of destroy()

   /**
    * Marks the object for creation.
    * @return The changes made during this operation.
    */
   public Change create()
   {
      put("@create", Boolean.TRUE);
      return new Change(Change.Operation.Create, this);
   } // end of create()

   /**
    * Commits object's changes, if any.  The effect of this is to set original values of tracked attributes to be the same as their current values, and to remove any {@link #create()}/{@link #destroy()} tag.
    * @see #getTrackedAttributes()
    */
   public void commit()
   {
      for (String key : getTrackedAttributes())
      {
	 String originalValueKey = "original" + key.substring(0,1).toUpperCase() + key.substring(1);
	 remove(originalValueKey);
      }
      remove("@destroy");
      remove("@create");
   }

   /**
    * Rolls back changes since the object was create or {@link #commit()} was last called. The effect of this is to reset tracked attributes to be the same as their original values, and to remove any {@link #destroy()} tag. If it has been tagged for {@link #create()}, the tag remains in place.
    * @see #getTrackedAttributes()
    */
   public void rollback()
   {
      for (String key : getTrackedAttributes())
      {
	 String originalValueKey = "original" + key.substring(0,1).toUpperCase() + key.substring(1);
	 
	 if (containsKey(originalValueKey))
	 {
	    try
	    {
	       // set the current value to the original value
	       setter(this, key).invoke(this, super.get(originalValueKey));
	       // remove the original key
	       remove(originalValueKey);
	    }
	    catch(IllegalAccessException exception) {}
	    catch(InvocationTargetException exception) {}
	 }
      }
      remove("@destroy");
   } // end of rollback()

   /**
    * Rolls back an individual attribute change.
    * @param key The name of the attribute to roll back.
    * @see #getTrackedAttributes()
    */
   public void rollback(String key)
   {
      String originalValueKey = "original" + key.substring(0,1).toUpperCase() + key.substring(1);
	 
      if (containsKey(originalValueKey))
      { 
	 try
	 {
	    // set the current value to the original value
	    setter(this, key).invoke(this, super.get(originalValueKey));
	    // remove the original key
	    remove(originalValueKey);
	 }
	 catch(IllegalAccessException exception) {}
	 catch(InvocationTargetException exception) {}
      }
   } // end of rollback()
   
   /**
    * Determines how the object has changed since it was originally defined, or since {@link #commit()} was last called.
    * @return How/whether the object has been changed.
    * @see #getTrackedAttributes()
    */
   public Change.Operation getChange()
   {
      if (containsKey("@destroy")) return Change.Operation.Destroy;
      if (containsKey("@create")) return Change.Operation.Create;
      for (String key : getTrackedAttributes())
      {
	 String originalValueKey = "original" + key.substring(0,1).toUpperCase() + key.substring(1);
	 if (containsKey(originalValueKey)) return Change.Operation.Update;
      } // next tracked attribute
      return Change.Operation.NoChange;
   } // end of getChange()
   
   /**
    * Produces a list of individual changes for the object.
    * @return A list of individual changes for the object.
    */
   public Vector<Change> getChanges()
   {
      Vector<Change> changes = new Vector<Change>();
      Change.Operation operation = getChange();
      switch (operation)
      {
	 case Destroy:
	 {
	    // if this has also been marked for creation, then it doesn't exist yet
	    // so no delete (or any other) operation is required
	    if (!containsKey("@create"))
	    { // it's not (also) being created, so delete operation is returned
	       changes.add(new Change(Change.Operation.Destroy, this));
	    }
	    break;
	 }
	 case Create:
	 {
	    // add a create operation first
	    changes.add(new Change(Change.Operation.Create, this));
	    // add all attributes as updates
	    for (String key : getTrackedAttributes())
	    {
	       try
	       {
		  Method getter = getter(this, key);
		  Object value = getter.invoke(this);
		  if (value != null)
		  {
		     changes.add(new Change(Change.Operation.Update, this, key, value));
		  }
	       }
	       catch(Exception exception)
	       {
		  System.err.println("TrackedMap.getChanges(): " + getId() + ": " + exception);
		  exception.printStackTrace(System.err);
	       }
	    } // next tracked attribute
	    break;
	 }
	 case Update:
	 {
	    // add only attributes that have changed
	    for (String key : getTrackedAttributes())
	    {
	       String originalValueKey = "original" + key.substring(0,1).toUpperCase() + key.substring(1);
	       Method getter = getter(this, key);
	       try
	       {
		  Object value = getter.invoke(this);
		  if (containsKey(originalValueKey))
		  {
		     Object originalValue = super.get(originalValueKey);
		     if ((value == null && originalValue != null)
			 || (value != null && originalValue == null)
			 || (value != null && !value.equals(originalValue)))
		     {
			changes.add(new Change(Change.Operation.Update, this, key, value));
		     }
		  }
	       }
	       catch(Exception exception)
	       {
		  System.err.println("TrackedMap.getChanges(): " + getId() + ": " + exception);
		  exception.printStackTrace(System.err);
	       }
	    } // next tracked attribute
	    break;
	 }
      }
      return changes;
   } // end of getChanges()

   /**
    * Override of Map's put method to allow tracking of selected keys.
    * @param key The attribute name.
    * @param value The attribute value.
    * @return The previous value associated with key.
    */
   public Change checkForChange(String key, Object value)
      throws UnsupportedOperationException, ClassCastException, NullPointerException, IllegalArgumentException
   {
      Change change = null;
      if (getTrackedAttributes().contains(key))
      { // tracked key
	 String originalValueKey = "original" + key.substring(0,1).toUpperCase() + key.substring(1);
	 Method getter = getter(this, key);
	 try
	 {
	    Object currentValue = getter.invoke(this);
	    if (currentValue != null // current value is null
		&& !currentValue.equals(value)) // or they're different
	    { // value is changing
	       change = new Change(Change.Operation.Update, this, key, value);
	       // if this is the first time the value is being changed
	       if (!containsKey(originalValueKey))
	       { // remember the original value
		  super.put(originalValueKey, currentValue);
	       }
	    }
	 }
	 catch(IllegalAccessException x1) {}
	 catch(InvocationTargetException x2) {}
	 
      }
      return change;
   } // end of put()
   
   /**
    * Overrides Map method so that if scripting environments like JSTL's EL exclusively use get() for retrieving attributes, bean attributes will be available.
    * @param key The attribute key.
    * @return The attribute value.
    */
   public Object get(Object key)
   {
      Object value = super.get(key);
      if (value == null) // there's no value in the map
      {
	 String keyString = key.toString();
	 // ...and we're not calling get("original...") for the original value of a tracked attribute
	 if (!keyString.startsWith("original"))
	 {
	    Method getter = getter(this, keyString);
	    // ...and there's a getter
	    if (getter != null)
	    { // use the getter
	       try { value = getter.invoke(this); }
	       catch(IllegalAccessException x1) {}
	       catch(InvocationTargetException x2) {}
	    }
	 }
      }
      return value;
   } // end of get()

   // Map overrides

   /**
    * Override of Map's clone method, to copy only tracked attributes plus "id".
    * @return A copy of the object, including only the values of the tracked attributes.
    */
   public Object clone()
   {
      try
      {
	 TrackedMap copy = getClass().getDeclaredConstructor().newInstance();
	 copy.cloneAttributesFrom(this, null);
	 return copy;
      }
      catch(Exception exception)
      {
	 System.err.println("TrackedMap.clone(): Could not instantiate " + getId() + ": " + exception);
	 exception.printStackTrace(System.err);
	 return null;
      }
   } // end of clone()
   
   /**
    * Override of Map's clone method, to copy only tracked attributes plus "id".
    * @return A copy of the object, including only the values of the tracked attributes.
    */
   public void cloneAttributesFrom(TrackedMap other, String except)
   {
      try
      {
	 for (String key : getClonedAttributes())
	 { // copy tracked attributes
	    if (key.equals(except)) continue;	    
	    Method getter = getter(other, key);
	    Method setter = setter(this, key);
	    setter.invoke(this, getter.invoke(other));
	 }
      }
      catch(Exception exception)
      {
	 System.err.println("TrackedMap.cloneAttributesFrom(): Could not copy " + other.getId() + ": " + exception);
	 exception.printStackTrace(System.err);
      }
   } // end of clone()

   // java.lang.Object overrides:
      
   /**
    * Computes a hashCode for the object. 
    * <p>Map (base class) has a very mutable hashCode, but we don't want Annotation hashcodes changing whenever arbitrary elements change (otherwise they get lost in hash-based collections, etc.).  So this implementation returns the hashcode of the annotation's id.
    * @return Object's hashCode.
    */
   public int hashCode()
   {
      if (id != null)
      {
	 return id.hashCode();
      }
      else
      { // id isn't set, so we return the map's hashCode and hope for the best
	 return super.hashCode();
      }
   } // end of hashCode()
   
   /**
    * Indicates whether an object is "equal to" this one.  Equality is determined solely by the value of {@link #getId()}
    * @param obj The object being compared to.
    * @return true if obj is a ChangedTrackedMap with the same id as this one, false otherwise.
    */
   public boolean equals(Object obj)
   {
      if (obj == null) return false;
      if (obj == this) return true;
      // is it the same class (will be a subclass of TrackedMap, so we don't use instanceof)
      if (obj.getClass().getName().equals(getClass().getName()))
      {
	 TrackedMap other = (TrackedMap)obj;
	 if (getId() != null && other.getId() != null)
	 {
	    return other.getId().equals(getId());
	 }
      }
      return false;
   } // end of equals()

   /**
    * Access the class's getter for the given attribute.
    * @param object
    * @param key
    * @return The getter, or null if there isn't one.
    */
   public Method getter(TrackedMap object, String key)
   {
      try
      {
	 String getterName = "get" + key.substring(0,1).toUpperCase() + key.substring(1);
	 return object.getClass().getMethod(getterName);
      }
      catch(Throwable exception)
      {
	 // System.err.println("getter [" + object + "] - " + key + " :: " + exception);
	 // exception.printStackTrace(System.err);
	 return null;
      }	 
   } // end of getter()

   /**
    * Access the class's getter for the given attribute.
    * @param object
    * @param key
    * @return The getter, or null if there isn't one.
    */
   public static Method setter(TrackedMap object, String key)
   {
      try
      {
	 String setterName = "set" + key.substring(0,1).toUpperCase() + key.substring(1);
	 try
	 {
	    return object.getClass().getMethod(setterName, String.class); // labels, etc.
	 }
	 catch(NoSuchMethodException x1)
	 {
	    try
	    {
	       return object.getClass().getMethod(setterName, int.class); // Annotation.ordinal
	    }
	    catch(NoSuchMethodException x2)
	    {
	       try
	       {
		  return object.getClass().getMethod(setterName, Integer.class); // confidence
	       }
	       catch(NoSuchMethodException x3)
	       {
		  try
		  {
		     return object.getClass().getMethod(setterName, boolean.class); // Layer.peers, etc...
		  }
		  catch(NoSuchMethodException x4)
		  {
		     try
		     {
			return object.getClass().getMethod(setterName, LinkedHashMap.class); // Layer.validLabels
		     }
		     catch(NoSuchMethodException x5)
		     {
			return object.getClass().getMethod(setterName, Double.class); // Anchor.offset
		     }
		  }
	       }
	    }
	 }
      }
      catch(Throwable exception)
      {
	 System.out.println("" + exception);
	 return null;
      }	 
   } // end of getter()
   
} // end of class Annotation
