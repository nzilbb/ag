//
// Copyright 2015-2020 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;
import nzilbb.util.CloneableBean;
import nzilbb.util.ClonedProperty;

/**
 * Base class for annotation graph classes, which encapsulates three common features of
 * these classes:
 * <ol>
 *  <li> Changes to specific attributes can be tracked and rolled back, by setting
 *   {@link #getTracker()}, which receives notification of all relevant changes.  Before 
 *   {@link #getTracker()} is set, no change registration is done, which the exception of calls
 *   to {@link #create()} and {@link #destroy()}, changes which are remembered and passed
 *   to the {@link ChangeTracker} when {@link #setTracker(ChangeTracker)} is called (this
 *   is done so that {@link Anchor}s/{@link Annotation}s can be marked for creation before
 *   being added to a {@link Graph}). </li>
 *  <li> Annotation graph classes (in particular {@link Annotation} and {@link Anchor} are
 *   defined as Maps not just to allow tracking of changes to registered attributes
 *   (key/value pairs), but also to allow them to easily be informally extended or tagged at
 *   runtime. This is with two particular possibilities in mind: 
 *   <ul>
 *    <li> An annotation is defined as a directed graph edge with a <var>label</var> and a
 *     <var>type</var> (which in this implementation is actually <var>layerId</var>), and an
 *     anchor is a  graph node with an optional <var>offset</var>, but there is scope of an
 *     annotation to include other attributes - not alternative annotation labels, but rather
 *     extra information about the annotation itself, which might include provenance,
 *     authorship, etc. This information can be set with arbitrarily named keys in the map
 *     as required.</li> 
 *    <li> During processing, it may be desirable or necessary to tag annotations or anchors
 *     in some way, e.g. to mark them as 'already visited' or 'already processed' by some
 *     traversal process, or to link together entities during merging of graphs, etc. This can
 *     be easily achieved by simply setting ad-hoc attributes on the entity as required.  Such
 *     'transient' attributes, which should ideally not be serialized for storage or transfer,
 *     are assumed to have keys that start with a non-alphabetic character -
 *     e.g. <var>"@otherGraphConterpart</var> - in contrast to 'sticky' attributes mentioned
 *     above. </li> 
 *   </ul>
 *  </li>
 *  <li> All need specific behaviour for object cloning and JSON
 *   serialization/deserialization </li> 
 * </ol>
 * <p>
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class TrackedMap
   extends LinkedHashMap<String,Object>
   implements Cloneable, CloneableBean
{
   // Attributes stored in HashMap:
   
   /**
    * Keys for attributes that are change-tracked - i.e. when a new value is set for any of
    * these attributes, and {@link #getTracker()} is set, the change is registered. 
    */
   private static final TreeSet<String> trackedAttributes = new TreeSet<String>();
   /**
    * Keys for attributes that are change-tracked - i.e. when a new value is set for any of
    * these attributes, and {@link #getTracker()} is set, the change is registered. 
    * @return A set of attributes whose changes are tracked.
    */
   public Set<String> getTrackedAttributes()
   {
      TreeSet<String> properties = new TreeSet<String>();
      Method[] methods = getClass().getMethods();
      for (Method method : methods) {
	 TrackedProperty annotation = method.getAnnotation(TrackedProperty.class);
	 if (annotation != null) {
            String setterName = method.getName();
            String property = setterName.substring(3,1).toLowerCase() + setterName.substring(4);
            properties.add(property);
         } // TrackerProperty getter
      } // next method
      return properties;
   } // end of getTrackedAttributes()
   
   /**
    * Sets the object attributes using the given JSON representation.
    * <p> All attributes are copied, including those that are not bean attributes; other
    * are stored as map values.
    * @param json A JSON-encoded string.
    * @return A reference to this object.
    */
   public TrackedMap fromJson(String json)
   {
      return fromJson(Json.createReader(new StringReader(json)).readObject());
   }
   
   /**
    * JSON-encodes the object.
    * @return A JSON representation of the object.
    */
   public String toJsonString()
   {
      return toJson().toString();
   }
   /**
    * JSON-encodes the object.
    * @return A JSON representation of the object.
    */
   @SuppressWarnings({"rawtypes"})
   public JsonObject toJson()
   {
      JsonObjectBuilder json = Json.createObjectBuilder();
      
      // first add the bean property
      for (String key : getClonedAttributes()) {
         Method getter = getter(key);
         try {
            Object value = getter.invoke(this);
            if (value != null) {
               Class parameterClass = getter.getReturnType();
               if (parameterClass.equals(String.class)) {
                  json.add(key, (String)value);
               } else if (parameterClass.equals(Integer.class)) {
                  json.add(key, (Integer)value);
               } else if (parameterClass.equals(int.class)) {
                  json.add(key, (int)value);
               } else if (parameterClass.equals(Double.class)) {
                  json.add(key, (Double)value);
               } else if (parameterClass.equals(double.class)) {
                  json.add(key, (Double)value);
               } else if (parameterClass.equals(Long.class)) {
                  json.add(key, (Long)value);
               } else if (parameterClass.equals(long.class)) {
                  json.add(key, (long)value);
               } else if (parameterClass.equals(Boolean.class)) {
                  json.add(key, (Boolean)value);
               } else if (parameterClass.equals(boolean.class)) {
                  json.add(key, (boolean)value);
               }
               // ignore any other types
            } // value isn't null
         } catch(IllegalAccessException exception) {
            System.err.println(
               "TrackedMap.toJsonString - can't set " + key + ": " + exception);
         } catch(InvocationTargetException exception) {
            System.err.println(
               "TrackedMap.toJsonString - can't set " + key + ": " + exception);
         }
      } // next bean property

      // now anything 'non-transient' in the map
      for (String key : keySet()) {
         if (Character.isLetterOrDigit(key.charAt(0))) {
            Object value = get(key);
            if (value != null) {
               Class parameterClass = value.getClass();
               if (parameterClass.equals(String.class)) {
                  json.add(key, (String)value);
               } else if (parameterClass.equals(Integer.class)) {
                  json.add(key, (Integer)value);
               } else if (parameterClass.equals(int.class)) {
                  json.add(key, (int)value);
               } else if (parameterClass.equals(Double.class)) {
                  json.add(key, (Double)value);
               } else if (parameterClass.equals(double.class)) {
                  json.add(key, (Double)value);
               } else if (parameterClass.equals(Long.class)) {
                  json.add(key, (Long)value);
               } else if (parameterClass.equals(long.class)) {
                  json.add(key, (long)value);
               } else if (parameterClass.equals(Boolean.class)) {
                  json.add(key, (Boolean)value);
               } else if (parameterClass.equals(boolean.class)) {
                  json.add(key, (boolean)value);
               }
               // ignore any other types
            } // value isn't null
         } // isn't 'transient'
      } // next bean property

      return json.build();
   }
   
   /**
    * Sets the object attributes using the given JSON representation.
    * <p>All attributes are copied, including those that are not bean attributes; other
    * are stored as map values.
    * @param json
    * @return A reference to this object.
    */
   @SuppressWarnings({"rawtypes","unchecked"})
   public TrackedMap fromJson(JsonObject json)
   {
      HashSet<String> beanAttributes = new HashSet<String>();
      for (String a : getClonedAttributes()) beanAttributes.add(a);
      
      for (String key : json.keySet())
      {
         if (!json.isNull(key))
         {
            Object value = json.get(key);
            if (beanAttributes.contains(key))
            { // is a bean attribute
               Method setter = setter(key);
               if (value instanceof JsonObject)
               { // complex object value
                  JsonObject objectValue = (JsonObject)value;
                  // what type are we expecting?
                  Class type = setter.getParameterTypes()[0];
                  boolean isMap = false;
                  for (Class i : type.getInterfaces())
                  {
                     if (i.equals(Map.class)) isMap = true;
                  }
                  if (isMap)
                  {
                     try
                     {
                        Map map = (Map)type.getConstructor().newInstance();
                        for (String k : objectValue.keySet())
                        {
                           Object v = objectValue.get(k);
                           if (v instanceof JsonString) {
                              v = ((JsonString)v).getString();
                           } else if (v instanceof JsonNumber) {
                              v = Double.valueOf(((JsonNumber)v).doubleValue());
                           } else if (v instanceof JsonValue) {
                              if (v.equals(JsonValue.TRUE)) {
                                 v = true;
                              } else if (v.equals(JsonValue.FALSE)) {
                                 v = false;
                              } else if (v.equals(JsonValue.NULL)) {
                                 v = null;
                              }
                           }
                           map.put(k, v);
                        } // next key/value pair
                        setter.invoke(this, map); 
                     }
                     catch(Exception exception)
                     {
                        System.err.println(
                           "TrackedMap.fromJson - can't set complex " + key + ": " + exception);
                     }
                  }
               } // complex object value
               else
               { // simple value
                  Class parameterClass = setter.getParameterTypes()[0];
                  if (value instanceof JsonString) {
                     value = ((JsonString)value).getString();
                  } else if (value instanceof JsonNumber) {
                     if (parameterClass.equals(Integer.class)) {
                        value = Integer.valueOf(((JsonNumber)value).intValue());
                     } else if (parameterClass.equals(int.class)) {
                        value = Integer.valueOf(((JsonNumber)value).intValue());
                     } else if (parameterClass.equals(Double.class)) {
                        value = Double.valueOf(((JsonNumber)value).doubleValue());
                     } else if (parameterClass.equals(double.class)) {
                        value = Double.valueOf(((JsonNumber)value).doubleValue());
                     } else if (parameterClass.equals(Long.class)) {
                        value = Long.valueOf(((JsonNumber)value).longValueExact());
                     } else if (parameterClass.equals(long.class)) {
                        value = Long.valueOf(((JsonNumber)value).longValueExact());
                     }
                  } else if (value instanceof JsonValue) {
                     if (value.equals(JsonValue.TRUE)) {
                        value = true;
                     } else if (value.equals(JsonValue.FALSE)) {
                        value = false;
                     } else if (value.equals(JsonValue.NULL)) {
                        value = null;
                     } 
                  }
                  try
                  {
                     setter.invoke(this, value);
                  }
                  catch(Exception exception)
                  {
                     System.err.println(
                        "TrackedMap.fromJson - can't set "
                        + key + " ("+value.getClass().getName()+"): " + exception);
                     put(key, value);
                  }
               } // simple value
            } // is a bean attribute
            else
            { // not a bean attribute
               // any map key that doesn's start with an alphanumeric is assumed to be transient
               // and thuse not copied
               if (Character.isLetterOrDigit(key.charAt(0))) {
                  // key starts with alphanumeric, so is copied into the map
                  put(key, value);
               }
            } // not a bean attribute
         } // value is not null
      } // next attribute
      return this;
   } // end of fromJsonObject()

   /**
    * Object that tracks all changes to this object.
    * @see #getTracker()
    * @see #setTracker(ChangeTracker)
    */
   protected ChangeTracker tracker;
   /**
    * Getter for {@link #getTracker()}: Object that tracks all changes to this object.
    * @return Object that tracks all changes to this object.
    */
   public ChangeTracker getTracker() { return tracker; }
   /**
    * Setter for {@link #getTracker()}: Object that tracks all changes to this object.
    * @param newTracker Object that tracks all changes to this object.
    */
   public TrackedMap setTracker(ChangeTracker newTracker)
   {
      if (tracker == null && newTracker != null)
      { // there was no previous tracker
         // have we been flagged for creation?
         if (containsKey("@create"))
         {
            // track the change
            newTracker.accept(new Change(Change.Operation.Create, this));
            // remove the flag
            remove("@create");
         }
         // have we been flagged for destruction?
         if (containsKey("@destroy"))
         {
            // track the change
            newTracker.accept(new Change(Change.Operation.Destroy, this));
            // remove the flag
            remove("@destroy");
         }
      } // there was no previous tracker
      
      tracker = newTracker;
      return this;
   }

   /**
    * The object's identifier.
    */
   protected String id;
   /**
    * Getter for <i>id</i>: The object's identifier.
    * @return The object's identifier.
    */
   @ClonedProperty
   public String getId() { return id; }
   /**
    * Setter for <i>id</i>: The object's identifier.
    * @param id The object's identifier.
    */
   public TrackedMap setId(String id) { this.id = id; return this; }
   
   /**
    * Confidence rating. 
    * <p>By convention, this is a value between 0 and 100 (inclusive), 0
    * meaning "no confidence and all" (e.g. a default value computed by interpolation),
    * 100 meaning "full confidence" (e.g. values set by a human annotator), and
    * intermediate values conveying different degrees of confidence (e.g. 50 for values
    * obtained by automatic annotation).
    * @see #getConfidence()
    * @see #setConfidence(Integer)
    */
   protected Integer confidence;
   /**
    * Getter for {@link #confidence}: Confidence rating.
    * <p>By convention, this is a value between 0 and 100 (inclusive), 0
    * meaning "no confidence and all" (e.g. a default value computed by interpolation),
    * 100 meaning "full confidence" (e.g. values set by a human annotator), and
    * intermediate values conveying different degrees of confidence (e.g. 50 for values
    * obtained by automatic annotation).
    * @return Confidence rating.
    */
   @ClonedProperty
   public Integer getConfidence() { return confidence; }
   /**
    * Setter for {@link #confidence}: Confidence rating.
    * <p>By convention, this is a value between 0 and 100 (inclusive), 0
    * meaning "no confidence and all" (e.g. a default value computed by interpolation),
    * 100 meaning "full confidence" (e.g. values set by a human annotator), and
    * intermediate values conveying different degrees of confidence (e.g. 50 for values
    * obtained by automatic annotation).
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
    * Getter for {@link #annotator}: Name of the person or system that created or changed
    * this entity. 
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
    * Gets the original value of the given (change tracked) key, before any subsequent calls to
    * its setter.
    * @param key The attribute name.
    * @return The original value, or Optional.empty() if it has not been changed or
    * {@link #getTracker()} is not set.
    */
   protected Optional<Object> getOriginal(String key)
   {
      if (tracker != null)
      {
         Optional<Change> change = tracker.getChange(id, key);
         if (change.isPresent())
         {
            return Optional.ofNullable(change.get().getOldValue());
         }
      }
      return Optional.empty();
   } // end of getOriginalLabel()

   /**
    * Registers a change to a tracked attribute, if appropriate (e.g. only if it is
    * actually changing, and only if {@link #getTracker()} is set), and returns a corresponding
    * change for the given attribute.  
    * @param key The attribute key.
    * @param value The proposed change.
    * @return Returns an Update change, or null if the value is not changing.
    */
   protected Change registerChange(String key, Object value)
   {
      if (tracker == null) return null; // nobody cares

      Change change = null;
      try
      {
         Method getter = getter(key);
         assert getter != null : "registerChange: getter != null : "
            + key + ", " + value + " - " + this.getClass().getName();
         Object oldValue = getter.invoke(this);
         if ((value != null && !value.equals(oldValue))
             || value == null && oldValue != null)
         { // only track actual changes
            change = new Change(Change.Operation.Update, this, key, value, oldValue);
            tracker.accept(change);
         }
      }
      catch(NullPointerException exception)
      {
         System.err.println(
            "registerChange [" + this + "] - " + key + " = " + value + " :: " + exception);
         exception.printStackTrace(System.err);
      }
      catch(IllegalAccessException exception)
      {
         System.err.println(
            "registerChange [" + this + "] - " + key + " = " + value + " :: " + exception);
      }
      catch(InvocationTargetException exception)
      {
         System.err.println(
            "registerChange [" + this + "] - " + key + " = " + value
            + " :: " + exception.getCause());
         if (exception.getCause() != null)
         {
            exception.getCause().printStackTrace(System.err);
         }
      }
      return change;
   } // end of registerChange()

   /**
    * Marks the object for deletion. If this is invoked before {@link #setTracker(ChangeTracker)}
    * is called, a Destroy change will nevertheless be tracked if 
    * {@link #setTracker(ChangeTracker)} is called later. 
    * <p>This method is not called "delete()" because "delete" is a reserved word in
    * Javascript, and we want these objects to be manipulable from Javascript. 
    * @return The changes made during this operation.
    */
   public Change destroy()
   {
      Change change = new Change(Change.Operation.Destroy, this);
      if (tracker == null)
      { // no tracker (yet?) so add a flag to the map
         put("@destroy", Boolean.TRUE);
      }
      else
      {
         tracker.accept(change);
      }
      return change;
   } // end of destroy()

   /**
    * Marks the object for creation. If this is invoked before {@link #setTracker(ChangeTracker)}
    * is called, a Create change will nevertheless be tracked if 
    * {@link #setTracker(ChangeTracker)} is called later. 
    * @return The changes made during this operation.
    */
   public Change create()
   {
      Change change = new Change(Change.Operation.Create, this);
      if (tracker == null)
      { // no tracker (yet?) so add a flag to the map
         put("@create", Boolean.TRUE);
      }
      else
      { 
         tracker.accept(change);
      }
      return change;
   } // end of create()

   /**
    * Rolls back changes since the object was created. 
    * The effect of this is to reset tracked attributes to be the same as their
    * original values, and to remove any {@link #destroy()} tag. If it has been tagged for
    * {@link #create()}, the tag remains in place. 
    * @see #getTrackedAttributes()
    * @throws NullPointerException If {@link #getTracker()} is not set (changes can only be
    * rolled back if there's a {@link ChangeTracker} that knows what has changed).
    */
   public void rollback()
   {
      if (tracker == null)
         throw new NullPointerException(""+id+" has no change tracker and cannot be rolled back");

      if (getChange() != Change.Operation.Create)
      {
         tracker.getChanges(id).forEach(c -> {
               c.rollback();
               tracker.reject(c);
            });
      }
   } // end of rollback()

   /**
    * Rolls back an individual attribute change.
    * @param key The name of the attribute to roll back.
    * @see #getTrackedAttributes()
    * @throws NullPointerException If {@link #getTracker()} is not set (changes can only be
    * rolled back if there's a {@link ChangeTracker} that knows what has changed).
    */
   public void rollback(String key)
   {
      if (tracker == null)
         throw new NullPointerException(""+id+" has no change tracker and cannot be rolled back");

      tracker.getChange(id, key).ifPresent(c -> {
            c.rollback();
            tracker.reject(c);
         });
   } // end of rollback()
   
   /**
    * Determines how the object has changed since it was originally defined. 
    * @return How/whether the object has been changed. If {@link #getTracker()} has not been
    * set, then only create or destroy changes can be returned.
    * @see #getTrackedAttributes()
    */
   public Change.Operation getChange()
   {
      if (tracker == null)
      { // tracker is not set (yet).
         if (containsKey("@destroy")) return Change.Operation.Destroy;
         else if (containsKey("@create")) return Change.Operation.Create;
         else return Change.Operation.NoChange;
      }      
      Optional<Change> createDestroy = tracker.getChange(id, null);
      if (createDestroy.isPresent()) return createDestroy.get().getOperation();

      return tracker.getChanges(id).size() == 0
         ?Change.Operation.NoChange
         :Change.Operation.Update;
   } // end of getChange()
   
   /**
    * Produces a list of individual changes for the object.
    * @return A list of individual changes for the object.
    */
   public List<Change> getChanges()
   {
      final Vector<Change> changes = new Vector<Change>();
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
                  Method getter = getter(key);
                  Object value = getter.invoke(this);
                  if (value != null)
                  {
                     changes.add(new Change(Change.Operation.Update, this, key, value, null));
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
            tracker.getChanges(id).forEach(
               c -> changes.add(c));
            break;
         }
      }
      return changes;
   } // end of getChanges()

   /**
    * Builder-pattern method for putting an arbitrary kay/value into the map.
    * @param key
    * @param value
    * @return this
    */
   public TrackedMap with(String key, Object value)
   {
      put(key, value);
      return this;
   } // end of with()

   // Map overrides

   /**
    * Overrides Map method so that if scripting environments like JSTL's EL exclusively
    * use get() for retrieving attributes, bean attributes will be available.
    * @param key The attribute key.
    * @return The attribute value.
    */
   public Object get(Object key)
   {
      Object value = super.get(key);
      if (value == null) // there's no value in the map
      {
         String keyString = key.toString();
         Method getter = getter(keyString);
         // ...and there's a getter
         if (getter != null)
         { // use the getter
            try { value = getter.invoke(this); }
            catch(IllegalAccessException x1) {}
            catch(InvocationTargetException x2) {}
         }
      }
      return value;
   } // end of get()
   
   /**
    * Override of Map's clone method, to copy only tracked attributes plus "id".
    * @return A copy of the object, including only the values of the tracked attributes.
    */
   public Object clone()
   {
      try
      {
         TrackedMap copy = getClass().getDeclaredConstructor().newInstance();
         copy.clonePropertiesFrom(this, null);
         return copy;
      }
      catch(Exception exception)
      {
         System.err.println(
            "TrackedMap.clone(): Could not instantiate " + getId() + ": " + exception);
         exception.printStackTrace(System.err);
         return null;
      }
   } // end of clone()
   
   // java.lang.Object overrides:
      
   /**
    * Computes a hashCode for the object. 
    * <p>Map (base class) has a very mutable hashCode, but we don't want Annotation
    * hashcodes changing whenever arbitrary elements change (otherwise they get lost in
    * hash-based collections, etc.).  So this implementation returns the hashcode of the
    * annotation's id. 
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
    * Indicates whether an object is "equal to" this one.  Equality is determined solely by
    * the value of {@link #getId()} 
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

} // end of class Annotation
