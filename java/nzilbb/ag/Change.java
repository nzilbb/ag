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

import java.lang.reflect.InvocationTargetException;

/**
 * Defines a single change to a TrackedMap object.
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("overrides")
public class Change
{
   // enums:

   /** 
    * The change operation on a given object - i.e. whether and how the object has changed
    * since it was first defined, or since the object's {@link TrackedMap#commit()}}
    * method was last called.
    * @see TrackedMap#commit()
    * @see TrackedMap#rollback()
    * @see TrackedMap#getChange()
    */
   public enum Operation { 
      /** No change is being made. */
      NoChange, 
      /** The object is being deleted. */
      Destroy, 
      /** The object is being created. */
      Create, 
      /** Some attribute(s) of the object are being changed. */
      Update };

   // Attributes:
   
   /**
    * The operation of this change.
    * @see #getOperation()
    * @see #setOperation(Operation)
    */
   protected Operation operation = Operation.NoChange;
   /**
    * Getter for {@link #operation}: The operation of this change.
    * @return The operation of this change.
    */
   public Operation getOperation() { return operation; }
   /**
    * Setter for {@link #operation}: The operation of this change.
    * @param newOperation The operation of this change.
    */
   public Change setOperation(Operation newOperation) { operation = newOperation; return this; }

   /**
    * The object on which the change is made.
    * @see #getObject()
    * @see #setObject(TrackedMap)
    */
   protected TrackedMap object;
   /**
    * Getter for {@link #object}: The object on which the change is made.
    * @return The object on which the change is made.
    */
   public TrackedMap getObject() { return object; }
   /**
    * Setter for {@link #object}: The object on which the change is made.
    * @param newObject The object on which the change is made.
    */
   public Change setObject(TrackedMap newObject) { object = newObject; return this; }

   /**
    * The attribute name which will be changed, if the change is an Update.
    * @see #getKey()
    * @see #setKey(String)
    */
   protected String key;
   /**
    * Getter for {@link #key}: The attribute name which will be changed, if the change is
    * an Update. 
    * @return The attribute name which will be changed, if the change is an Update.
    */
   public String getKey() { return key; }
   /**
    * Setter for {@link #key}: The attribute name which will be changed, if the change is
    * an Update. 
    * @param newKey The attribute name which will be changed, if the change is an Update.
    */
   public Change setKey(String newKey) { key = newKey; return this; }

   /**
    * The new value for the attribute identified by {@link #getKey()}.
    * @see #getValue()
    * @see #setValue(Object)
    */
   protected Object value;
   /**
    * Getter for {@link #value}: The new value for the attribute identified by {@link #getKey()}.
    * @return The new value for the attribute identified by {@link #getKey()}.
    */
   public Object getValue() { return value; }
   /**
    * Setter for {@link #value}: The new value for the attribute identified by {@link #getKey()}.
    * @param newValue The new value for the attribute identified by {@link #getKey()}.
    */
   public Change setValue(Object newValue) { value = newValue; return this; }
   
   /**
    * The old value for the attribute defined by {@link #getKey()}
    * @see #getOldValue()
    * @see #setOldValue(Object)
    */
   protected Object oldValue;
   /**
    * Getter for {@link #oldValue}: The old value for the attribute defined by {@link #getKey()}
    * @return The old value for the attribute defined by {@link #getKey()}
    */
   public Object getOldValue() { return oldValue; }
   /**
    * Setter for {@link #oldValue}: The old value for the attribute defined by {@link #getKey()}
    * @param newOldValue The old value for the attribute defined by {@link #getKey()}
    */
   public Change setOldValue(Object newOldValue) { oldValue = newOldValue; return this; }
   
   // Methods:
   
   /**
    * Default constructor
    */
   public Change()
   {
   } // end of constructor

   /**
    * Change Operation constructor
    * @param object The object on which the change is made.
    * @param operation The operation of this change.
    */
   public Change(Operation operation, TrackedMap object)
   {
      setObject(object);
      setOperation(operation);
   } // end of constructor

   /**
    * Key/value constructor
    * @param object The object on which the change is made.
    * @param operation The operation of this change.
    * @param key The attribute name which will be changed, if the change is an Update.
    * @param value The new value for the attribute identified by <var>key</var>.
    * @param value The old value for the attribute identified by <var>key</var>.
    */
   public Change(Operation operation, TrackedMap object, String key, Object value, Object oldValue)
   {
      setObject(object);
      setOperation(operation);
      setKey(key);
      setValue(value);
      setOldValue(oldValue);
   } // end of constructor
   
   /**
    * Applies the change to the object.
    */
   public void apply(TrackedMap object)
   {
      switch (getOperation())
      {
	 case Update:
            try
            {
               TrackedMap.setter(object, key).invoke(object, value);
            }
            catch(IllegalAccessException x) {}
            catch(InvocationTargetException x) {}
            break;
	 case Destroy: getObject().destroy(); break;
	 case Create: getObject().create(); break;
      }
   } // end of apply()

   /**
    * Rolls back the change to the object.
    */
   public void rollback()
   {
      ChangeTracker tracker = object.getTracker();
      object.setTracker(null); // prevent change tracking for rollback
      switch (getOperation())
      {
	 case Update:
            try
            {
               TrackedMap.setter(object, key).invoke(object, oldValue); 
            }
            catch(IllegalAccessException x) {}
            catch(InvocationTargetException x) {}
            break;
	 case Destroy: getObject().create(); break;
	 case Create: getObject().destroy(); break;
      }
      object.setTracker(tracker);
   } // end of apply()

   // java.lang.Object overrides:
   
   /**
    * A string representation of the object.
    * @return A string representation of the object.
    */
   public String toString()
   {
      return ""+operation + " " + object.getId()
	 + (operation == Operation.Update?
            ": " + key + " = " + value + " (was "+oldValue+")":"");
   } // end of toString()   
   
   /**
    * Equality test.
    * @param o The other object to compare with.
    * @return true if the object is a Change with the same operation, key, and value on
    * the same object as this one. 
    */
   public boolean equals(Object o)
   {
      if (this == o) return true;
      if (o instanceof Change)
      {
	 Change other = (Change)o;
	 return getOperation() == other.getOperation()
	    && getObject().equals(other.getObject())
	    && (
	       (getKey() == null && other.getKey() == null)
	       || (
		  getKey().equals(other.getKey())
		  && (
		     (getValue() == null && other.getValue() == null)
		     || getValue().equals(other.getValue())
		     )
		  )
	       );
      }
      return false;
   } // end of equals()
   
} // end of class Delta
