//
// Copyright 2015-2016 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of LaBB-CAT.
//
//    LaBB-CAT is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 2 of the License, or
//    (at your option) any later version.
//
//    LaBB-CAT is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with LaBB-CAT; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package nzilbb.configure;

import java.util.Collection;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * A parameter that needs to be set for a some operation or configuration.
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("rawtypes")
public class Parameter
{
   // Attributes:

   /**
    * The paramater's name.
    * @see #getName()
    * @see #setName(String)
    */
   protected String name;
   /**
    * Getter for {@link #name}: The paramater's name.
    * @return The paramater's name.
    */
   public String getName() { return name; }
   /**
    * Setter for {@link #name}: The paramater's name.
    * @param newName The paramater's name.
    */
   public void setName(String newName) { name = newName; }

   /**
    * A label that might be presented to a user.
    * @see #getLabel()
    * @see #setLabel(String)
    */
   protected String label;
   /**
    * Getter for {@link #label}: A label that might be presented to a user.
    * @return A label that might be presented to a user.
    */
   public String getLabel() { if (label != null ) { return label; } else { return getName(); } }
   /**
    * Setter for {@link #label}: A label that might be presented to a user.
    * @param newLabel A label that might be presented to a user.
    */
   public void setLabel(String newLabel) { label = newLabel; }

   /**
    * A text hint that might be displayed to a user.
    * @see #getHint()
    * @see #setHint(String)
    */
   protected String hint;
   /**
    * Getter for {@link #hint}: A text hint that might be displayed to a user.
    * @return A text hint that might be displayed to a user.
    */
   public String getHint() { if (hint != null) { return hint; } else { return getLabel(); } }
   /**
    * Setter for {@link #hint}: A text hint that might be displayed to a user.
    * @param newHint A text hint that might be displayed to a user.
    */
   public void setHint(String newHint) { hint = newHint; }

   /**
    * The type of the parameter.
    * @see #getType()
    * @see #setType(Class)
    */
   protected Class type;
   /**
    * Getter for {@link #type}: The type of the parameter.
    * @return The type of the parameter.
    */
   public Class getType() { return type; }
   /**
    * Setter for {@link #type}: The type of the parameter.
    * @param newType The type of the parameter.
    */
   public void setType(Class newType) { type = newType; }

   /**
    * The value (or default value) of the parameter.
    * @see #getValue()
    * @see #setValue(Object)
    */
   protected Object value;
   /**
    * Getter for {@link #value}: The value (or default value) of the parameter.
    * @return The value (or default value) of the parameter.
    */
   public Object getValue() { return value; }
   /**
    * Setter for {@link #value}: The value (or default value) of the parameter.
    * @param newValue The value (or default value) of the parameter.
    */
   public void setValue(Object newValue) { value = newValue; }

   
   /**
    * Whether the parameter is required (true) or optional (false - the default).
    * @see #getRequired()
    * @see #setRequired(boolean)
    */
   protected boolean required = false;
   /**
    * Getter for {@link #required}: Whether the parameter is required (true) or optional (false - the default).
    * @return Whether the parameter is required (true) or optional (false).
    */
   public boolean getRequired() { return required; }
   /**
    * Setter for {@link #required}: Whether the parameter is required (true) or optional (false).
    * @param newRequired Whether the parameter is required (true) or optional (false).
    */
   public void setRequired(boolean newRequired) { required = newRequired; }


   /**
    * A list of possible values for {@link #value}, or null if the possible values is not a closed set.
    * @see #getPossibleValues()
    * @see #setPossibleValues(Collection)
    */
   protected Collection possibleValues;
   /**
    * Getter for {@link #possibleValues}: A list of possible values for {@link #value}, or null if the possible values is not a closed set.
    * @return A list of possible values for {@link #value}, or null if the possible values is not a closed set.
    */
   public Collection getPossibleValues() { return possibleValues; }
   /**
    * Setter for {@link #possibleValues}: A list of possible values for {@link #value}, or null if the possible values is not a closed set.
    * @param newPossibleValues A list of possible values for {@link #value}, or null if the possible values is not a closed set.
    */
   public void setPossibleValues(Collection newPossibleValues) { possibleValues = newPossibleValues; }
   /**
    * Array getter for {@link #possibleValues}: A list of possible values for {@link #value}, or null if the possible values is not a closed set.
    * @return An array of possible values for {@link #value}, or null if the possible values is not a closed set.
    */
   @SuppressWarnings("unchecked")
   public Object[] getPossibleValuesArray() 
   { 
      if (possibleValues == null) return null;
      return possibleValues.toArray(new Object[0]); 
   }
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public Parameter()
   {
   } // end of constructor

   /**
    * Constructor from attributes.
    * @param name The paramater's name.
    * @param label A label that might be presented to a user.
    * @param hint A text hint that might be displayed to a user.
    * @param type The type of the parameter.
    * @param required Whether the parameter is required (true) or optional (false).
    * @param value The value (or default value) of the parameter.
    */
   public Parameter(String name, Class type, String label, String hint, boolean required, Object value)
   {
      setName(name);
      setType(type);
      setLabel(label);
      setHint(hint);
      setRequired(required);
      setValue(value);
   } // end of constructor
   /**
    * Constructor from attributes.
    * @param name The paramater's name.
    * @param label A label that might be presented to a user.
    * @param hint A text hint that might be displayed to a user.
    * @param required Whether the parameter is required (true) or optional (false).
    * @param value The value (or default value) of the parameter.
    */
   public Parameter(String name, String label, String hint, boolean required, Object value)
   {
      setName(name);
      setType(value.getClass());
      setLabel(label);
      setHint(hint);
      setRequired(required);
      setValue(value);
   } // end of constructor
   /**
    * Constructor from attributes.
    * @param name The paramater's name.
    * @param label A label that might be presented to a user.
    * @param hint A text hint that might be displayed to a user.
    * @param type The type of the parameter.
    * @param required Whether the parameter is required (true) or optional (false).
    */
   public Parameter(String name, Class type, String label, String hint, boolean required)
   {
      setName(name);
      setType(type);
      setLabel(label);
      setHint(hint);
      setRequired(required);
   } // end of constructor
   /**
    * Constructor from attributes.
    * @param name The paramater's name.
    * @param label A label that might be presented to a user.
    * @param hint A text hint that might be displayed to a user.
    * @param type The type of the parameter.
    * @param value The value (or default value) of the parameter.
    */
   public Parameter(String name, Class type, String label, String hint, Object value)
   {
      setName(name);
      setType(type);
      setLabel(label);
      setHint(hint);
      setValue(value);
   } // end of constructor
   /**
    * Constructor from attributes.
    * @param name The paramater's name.
    * @param label A label that might be presented to a user.
    * @param hint A text hint that might be displayed to a user.
    * @param value The value (or default value) of the parameter.
    */
   public Parameter(String name, String label, String hint, Object value)
   {
      setName(name);
      setType(value.getClass());
      setLabel(label);
      setHint(hint);
      setValue(value);
   } // end of constructor
   /**
    * Constructor from attributes.
    * @param name The paramater's name.
    * @param type The type of the parameter.
    * @param label A label that might be presented to a user.
    * @param hint A text hint that might be displayed to a user.
    */
   public Parameter(String name, Class type, String label, String hint)
   {
      setName(name);
      setType(type);
      setLabel(label);
      setHint(hint);
   } // end of constructor
   /**
    * Constructor from attributes.
    * @param name The paramater's name.
    * @param type The type of the parameter.
    * @param label A label that might be presented to a user.
    */
   public Parameter(String name, Class type, String label)
   {
      setName(name);
      setType(type);
      setLabel(label);
   } // end of constructor
   /**
    * Constructor from attributes.
    * @param name The paramater's name.
    * @param type The type of the parameter.
    */
   public Parameter(String name, Class type)
   {
      setName(name);
      setType(type);
   } // end of constructor
   /**
    * Constructor from attributes.
    * @param name The paramater's name.
    * @param value The value (or default value) of the parameter.
    */
   public Parameter(String name, Object value)
   {
      setName(name);
      setType(value.getClass());
      setValue(value);
   } // end of constructor

   
   /**
    * Add a value to {@link #possibleValues}.
    * @param value The possible value to add.
    */
   @SuppressWarnings("unchecked")
   public void addPossibleValue(Object value)
   {
      getPossibleValues().add(value);
   } // end of addPossibleValue()
   
   /**
    * Sets the value of the attribute named after {@link #name} of the given bean with the parameter's {@link value}.
    * <p>e.g. if the parameter's name is "foo" and it's value is "bar", then the effect
    * of this method is the same as invoking <code>bean.setFoo("bar")</code>.
    * @param bean The object whose bean attribute should be set.
    * @throws NoSuchMethodException If the bean has no setter named after {@link #name}.
    * @throws SecurityException On error.
    * @throws InvocationTargetException On error.
    * @throws IllegalAccessException If the setter is no <code>public</code>.
    */
   public void apply(Object bean)
      throws NoSuchMethodException, SecurityException, IllegalAccessException, InvocationTargetException
   {
      Method setter = bean.getClass().getMethod("set" + name.substring(0,1).toUpperCase() + name.substring(1), type);
      setter.invoke(bean, value);
   } // end of apply()

   /**
    * Sets the {@link value} of the parameter with the value of the attribute named after {@link
    * #name} of the given bean. 
    * <p>e.g. if the parameter's name is "foo" and the bean has a getter called "getFoo()", then
    * the effect of this method is the same as invoking <code>setValue(bead.getFoo())</code>, if
    * the value is not null.
    * @param bean The object whose bean attribute should be set.
    * @return The value that was set.
    */
   public Object extractValue(Object bean)
   {
     Object value = null;
     try
     {
       Method getter = bean.getClass().getMethod("get" + name.substring(0,1).toUpperCase() + name.substring(1));
       value = getter.invoke(bean);
       if (value != null) setValue(value);
     }
     catch(NoSuchMethodException exception) {
     System.out.println(exception.toString());}
     catch(SecurityException exception) {
     System.out.println(exception.toString());}
     catch(IllegalAccessException exception) {
     System.out.println(exception.toString());}
     catch(InvocationTargetException exception) {
     System.out.println(exception.toString());}
     return value;
   } // end of extractValue()


} // end of class SerializationParameter
