//
// Copyright 2015-2025 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Vector;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import nzilbb.ag.Layer;
import nzilbb.util.CloneableBean;
import nzilbb.util.ClonedProperty;

/**
 * A parameter that needs to be set for a some operation or configuration.
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("rawtypes")
public class Parameter implements CloneableBean {
   
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
  @ClonedProperty
  public String getName() { return name; }
  /**
   * Setter for {@link #name}: The paramater's name.
   * @param newName The paramater's name.
   */
  public Parameter setName(String newName) { name = newName; return this; }

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
  @ClonedProperty
  public String getLabel() { if (label != null ) { return label; } else { return getName(); } }
  /**
   * Setter for {@link #label}: A label that might be presented to a user.
   * @param newLabel A label that might be presented to a user.
   */
  public Parameter setLabel(String newLabel) { label = newLabel; return this; }

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
  @ClonedProperty
  public String getHint() { if (hint != null) { return hint; } else { return getLabel(); } }
  /**
   * Setter for {@link #hint}: A text hint that might be displayed to a user.
   * @param newHint A text hint that might be displayed to a user.
   */
  public Parameter setHint(String newHint) { hint = newHint; return this; }

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
  @ClonedProperty
  public Class getType() { return type; }
  /**
   * Setter for {@link #type}: The type of the parameter.
   * @param newType The type of the parameter.
   */
  public Parameter setType(Class newType) { type = newType; return this; }

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
   * <p> If <var>newWalue</var> is a String but {@link #getType()} is not
   * <tt>java.lang.String</tt>, this method will endevor to coerce a value of the correct type.
   */
  public Parameter setValue(Object newValue) {
    value = newValue;
    if (newValue instanceof String && type != null && !type.equals(String.class)) {
      // attempt to coerce a value of the correct type from the given string
      String string = (String)newValue;
      if (type.equals(Integer.class) || type.equals(int.class)) {
        try {
          value = new Integer(string);
        } catch(Exception exception) {}
      } else if (type.equals(Double.class) || type.equals(double.class)) {
        try {
          value = new Double(string);
        } catch(Exception exception) {}
      } else if (type.equals(Long.class) || type.equals(long.class)) {
        try {
          value = new Long(string);
        } catch(Exception exception) {}
      } else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
        if (string.equalsIgnoreCase("true") || string.equalsIgnoreCase("t")
            || string.equalsIgnoreCase("yes") || string.equalsIgnoreCase("y")
            || string.equalsIgnoreCase("on") || string.equalsIgnoreCase("1")) {
          value = Boolean.TRUE;
        } else {
          value = Boolean.FALSE;
        }
      } else if (type.equals(Layer.class)) {
        // ID only of Layers
        value = new Layer(string, string);
      }
    }
    return this;
  }
   
  /**
   * Whether the parameter is required (true) or optional (false - the default).
   * @see #getRequired()
   * @see #setRequired(boolean)
   */
  protected boolean required = false;
  /**
   * Getter for {@link #required}: Whether the parameter is required (true) or optional
   * (false - the default). 
   * @return Whether the parameter is required (true) or optional (false).
   */
  @ClonedProperty
  public boolean getRequired() { return required; }
  /**
   * Setter for {@link #required}: Whether the parameter is required (true) or optional (false).
   * @param newRequired Whether the parameter is required (true) or optional (false).
   */
  public Parameter setRequired(boolean newRequired) { required = newRequired; return this; }

  /**
   * A list of possible values for {@link #value}, or null if the possible values is not
   * a closed set. 
   * @see #getPossibleValues()
   * @see #setPossibleValues(Collection)
   */
  protected Collection possibleValues;
  /**
   * Getter for {@link #possibleValues}: A list of possible values for {@link #value}, or
   * null if the possible values is not a closed set. 
   * @return A list of possible values for {@link #value}, or null if the possible values
   * is not a closed set. 
   */
  public Collection getPossibleValues() { return possibleValues; }
  /**
   * Setter for {@link #possibleValues}: A list of possible values for {@link #value}, or
   * null if the possible values is not a closed set. 
   * @param newPossibleValues A list of possible values for {@link #value}, or null if
   * the possible values is not a closed set. 
   */
  public Parameter setPossibleValues(Collection newPossibleValues) { possibleValues = newPossibleValues; return this; }
  /**
   * Array getter for {@link #possibleValues}: A list of possible values for {@link
   * #value}, or null if the possible values is not a closed set. 
   * @return An array of possible values for {@link #value}, or null if the possible
   * values is not a closed set. 
   */
  @SuppressWarnings("unchecked")
  public Object[] getPossibleValuesArray() { 
    if (possibleValues == null) return null;
    return possibleValues.toArray(new Object[0]); 
  }
   
  // Methods:
   
  /**
   * Default constructor.
   */
  public Parameter() {
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
  public Parameter(
    String name, Class type, String label, String hint, boolean required, Object value) {
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
  public Parameter(String name, String label, String hint, boolean required, Object value) {
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
  public Parameter(String name, Class type, String label, String hint, boolean required) {
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
  public Parameter(String name, Class type, String label, String hint, Object value) {
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
  public Parameter(String name, String label, String hint, Object value) {
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
  public Parameter(String name, Class type, String label, String hint) {
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
  public Parameter(String name, Class type, String label) {
    setName(name);
    setType(type);
    setLabel(label);
  } // end of constructor
   
  /**
   * Constructor from attributes.
   * @param name The paramater's name.
   * @param type The type of the parameter.
   */
  public Parameter(String name, Class type) {
    setName(name);
    setType(type);
  } // end of constructor
   
  /**
   * Constructor from attributes.
   * @param name The paramater's name.
   * @param value The value (or default value) of the parameter.
   */
  public Parameter(String name, Object value) {
    setName(name);
    setType(value.getClass());
    setValue(value);
  } // end of constructor
   
  /**
   * Add a value to {@link #possibleValues}.
   * @param value The possible value to add.
   */
  @SuppressWarnings("unchecked")
  public void addPossibleValue(Object value) {
    getPossibleValues().add(value);
  } // end of addPossibleValue()

  /**
   * Called at the end of the default implementation of {@link #toJson()}, this method
   * allows the Parameter to use a String to represent Layer values.
   * @param json The JSON object being built.
   * @return The builder with any added properties.
   */
  @Override
  public JsonObjectBuilder addExtraJsonAttributes(JsonObjectBuilder json) {
    if (type.equals(String.class)) {
      if (value != null) json.add("value", (String)value);
      if (possibleValues != null && possibleValues.size() > 0) {
        JsonArrayBuilder possibilities = Json.createArrayBuilder();          
        for (String p : (Collection<String>)possibleValues) possibilities.add(p);
        json.add("possibleValues", possibilities);
      }
    } else if (type.equals(Integer.class)) {
      if (value != null) json.add("value", (Integer)value);
      if (possibleValues != null && possibleValues.size() > 0) {
        JsonArrayBuilder possibilities = Json.createArrayBuilder();          
        for (Integer p : (Collection<Integer>)possibleValues) possibilities.add(p);
        json.add("possibleValues", possibilities);
      }
    } else if (type.equals(int.class)) {
      if (value != null) json.add("value", (int)value);
      if (possibleValues != null && possibleValues.size() > 0) {
        JsonArrayBuilder possibilities = Json.createArrayBuilder();          
        for (Integer p : (Collection<Integer>)possibleValues) possibilities.add(p);
        json.add("possibleValues", possibilities);
      }
    } else if (type.equals(Double.class)) {
      if (value != null) json.add("value", (Double)value);
      if (possibleValues != null && possibleValues.size() > 0) {
        JsonArrayBuilder possibilities = Json.createArrayBuilder();          
        for (Double p : (Collection<Double>)possibleValues) possibilities.add(p);
        json.add("possibleValues", possibilities);
      }
    } else if (type.equals(double.class)) {
      if (value != null) json.add("value", (Double)value);
      if (possibleValues != null && possibleValues.size() > 0) {
        JsonArrayBuilder possibilities = Json.createArrayBuilder();          
        for (Double p : (Collection<Double>)possibleValues) possibilities.add(p);
        json.add("possibleValues", possibilities);
      }
    } else if (type.equals(Long.class)) {
      if (value != null) json.add("value", (Long)value);
      if (possibleValues != null && possibleValues.size() > 0) {
        JsonArrayBuilder possibilities = Json.createArrayBuilder();          
        for (Long p : (Collection<Long>)possibleValues) possibilities.add(p);
        json.add("possibleValues", possibilities);
      }
    } else if (type.equals(long.class)) {
      if (value != null) json.add("value", (long)value);
      if (possibleValues != null && possibleValues.size() > 0) {
        JsonArrayBuilder possibilities = Json.createArrayBuilder();          
        for (Long p : (Collection<Long>)possibleValues) possibilities.add(p);
        json.add("possibleValues", possibilities);
      }
    } else if (type.equals(Boolean.class)) {
      if (value != null) json.add("value", (Boolean)value);
    } else if (type.equals(boolean.class)) {
      if (value != null) json.add("value", (boolean)value);
    } else if (type.equals(Layer.class)) {
      // ID only of Layers
      if (value != null) json.add("value", ((Layer)value).getId());
      if (possibleValues != null && possibleValues.size() > 0) {
        JsonArrayBuilder possibilities = Json.createArrayBuilder();
        for (Layer p : (Collection<Layer>)possibleValues) possibilities.add(p.getId());
        json.add("possibleValues", possibilities);
        }
    }
    return json;
  } // end of addExtraJsonAttributes()

  /**
   * Initializes the bean with the given JSON representation. 
   * This method parses "value" and "possibleValues" before invoking the default
   * implementation to parse the rest.
   * @param json
   * @return A reference to this bean.
   */
  @SuppressWarnings({"rawtypes","unchecked"})
  @Override
  public CloneableBean fromJson(JsonObject json) {
    // call default meethod first, to ensure that 'type' is set
    CloneableBean.super.fromJson(json);

    // now parse value/possibleValues
    if (type.equals(String.class)) {
      if (json.containsKey("value")) setValue(json.getString("value"));
      if (json.containsKey("possibleValues")) {
        JsonArray possibilities = json.getJsonArray("possibleValues");
        Vector<String> possibleValues = new Vector<String>();
        for (int p = 0; p < possibilities.size(); p++) {
          possibleValues.add(possibilities.getString(p));
        } 
        setPossibleValues(possibleValues);
      }
    } else if (type.equals(Integer.class)) {
      if (json.containsKey("value")) setValue(json.getInt("value"));
      if (json.containsKey("possibleValues")) {
        JsonArray possibilities = json.getJsonArray("possibleValues");
        Vector<Integer> possibleValues = new Vector<Integer>();
        for (int p = 0; p < possibilities.size(); p++) {
          possibleValues.add(possibilities.getInt(p));
        } 
        setPossibleValues(possibleValues);
      }
    } else if (type.equals(int.class)) {
      if (json.containsKey("value")) setValue(json.getInt("value"));
      if (json.containsKey("possibleValues")) {
        JsonArray possibilities = json.getJsonArray("possibleValues");
        Vector<Integer> possibleValues = new Vector<Integer>();
        for (int p = 0; p < possibilities.size(); p++) {
          possibleValues.add(possibilities.getInt(p));
        } 
        setPossibleValues(possibleValues);
      }
    } else if (type.equals(Double.class)) {
      if (json.containsKey("value")) setValue(json.getJsonNumber("value").doubleValue());
      if (json.containsKey("possibleValues")) {
        JsonArray possibilities = json.getJsonArray("possibleValues");
        Vector<Double> possibleValues = new Vector<Double>();
        for (int p = 0; p < possibilities.size(); p++) {
          possibleValues.add(possibilities.getJsonNumber(p).doubleValue());
        } 
        setPossibleValues(possibleValues);
      }
    } else if (type.equals(double.class)) {
      if (json.containsKey("value")) setValue(json.getJsonNumber("value").doubleValue());
      if (json.containsKey("possibleValues")) {
        JsonArray possibilities = json.getJsonArray("possibleValues");
        Vector<Double> possibleValues = new Vector<Double>();
        for (int p = 0; p < possibilities.size(); p++) {
          possibleValues.add(possibilities.getJsonNumber(p).doubleValue());
        } 
        setPossibleValues(possibleValues);
      }
    } else if (type.equals(Long.class)) {
      if (json.containsKey("value")) setValue(json.getJsonNumber("value").longValue());
      if (json.containsKey("possibleValues")) {
        JsonArray possibilities = json.getJsonArray("possibleValues");
        Vector<Long> possibleValues = new Vector<Long>();
        for (int p = 0; p < possibilities.size(); p++) {
          possibleValues.add(possibilities.getJsonNumber(p).longValue());
        } 
        setPossibleValues(possibleValues);
      }
    } else if (type.equals(long.class)) {
      if (json.containsKey("value")) setValue(json.getJsonNumber("value").longValue());
      if (json.containsKey("possibleValues")) {
        JsonArray possibilities = json.getJsonArray("possibleValues");
        Vector<Long> possibleValues = new Vector<Long>();
        for (int p = 0; p < possibilities.size(); p++) {
          possibleValues.add(possibilities.getJsonNumber(p).longValue());
        } 
        setPossibleValues(possibleValues);
      }
    } else if (type.equals(Boolean.class)) {
      if (json.containsKey("value")) setValue(json.getBoolean("value"));
    } else if (type.equals(boolean.class)) {
      if (json.containsKey("value")) setValue(json.getBoolean("value"));
    } else if (type.equals(Layer.class)) {
      // ID only of Layers
      if (json.containsKey("value")) {
        setValue(new Layer(json.getString("value"), json.getString("value")));
      }
      if (json.containsKey("possibleValues")) {
        JsonArray possibilities = json.getJsonArray("possibleValues");
        Vector<Layer> possibleValues = new Vector<Layer>();
        for (int p = 0; p < possibilities.size(); p++) {
          possibleValues.add(new Layer(possibilities.getString(p), possibilities.getString(p)));
        } 
        setPossibleValues(possibleValues);
      }
    }
    return this;
  }

  /**
   * Sets the value of the attribute named after {@link #name} of the given bean with the
   * parameter's {@link value}. 
   * <p>e.g. if the parameter's name is "foo" and it's value is "bar", then the effect
   * of this method is the same as invoking <code>bean.setFoo("bar")</code>.
   * @param bean The object whose bean attribute should be set.
   * @throws NoSuchMethodException If the bean has no setter named after {@link #name}.
   * @throws SecurityException On error.
   * @throws InvocationTargetException On error.
   * @throws IllegalAccessException If the setter is no <code>public</code>.
   */
  public void apply(Object bean)
    throws NoSuchMethodException, SecurityException, IllegalAccessException,
    InvocationTargetException {
    Method setter = bean.getClass().getMethod(
      "set" + name.substring(0,1).toUpperCase() + name.substring(1), type);
    setter.invoke(bean, value);
  } // end of apply()

  /**
   * Sets the {@link value} of the parameter with the value of the attribute named after 
   * {@link #name} of the given bean. 
   * <p>e.g. if the parameter's name is "foo" and the bean has a getter called "getFoo()", then
   * the effect of this method is the same as invoking <code>setValue(bead.getFoo())</code>, if
   * the value is not null.
   * @param bean The object whose bean attribute should be set.
   * @return The value that was set.
   */
  public Object extractValue(Object bean) {
    Object value = null;
    try {
      Method getter = bean.getClass().getMethod(
        "get" + name.substring(0,1).toUpperCase() + name.substring(1));
      value = getter.invoke(bean);
      if (value != null) setValue(value);
    } catch(NoSuchMethodException exception) {
      System.out.println(exception.toString());
    } catch(SecurityException exception) {
      System.out.println(exception.toString());
    } catch(IllegalAccessException exception) {
      System.out.println(exception.toString());
    } catch(InvocationTargetException exception) {
      System.out.println(exception.toString());
    }
    return value;
  } // end of extractValue()

  /**
   * String representation of the parameter.
   * @return String representation of the parameter.
   */
  public String toString() {
    return getName() + " ("+getType().getSimpleName()+") = " + getValue();
  } // end of toString()
  
} // end of class SerializationParameter
