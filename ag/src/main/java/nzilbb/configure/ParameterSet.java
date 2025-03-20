//
// Copyright 2015-2019 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.util.LinkedHashMap;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.annotation.Annotation;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;

/**
 * Set of parameters, being a map to Parameters keyed on parameter name.  Uses LinkedHashMap so
 * that iteration order can be controlled, as it's insertion-order. 
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class ParameterSet extends LinkedHashMap<String,Parameter> {   
  // Methods:
  
  /**
   * Default constructor
   */
  public ParameterSet() {
  } // end of constructor
  
  /**
   * Adds a parameter to the set.
   * @param parameter The parameter to add.
   * @return The parameter added.
   */
  public Parameter addParameter(Parameter parameter) {
    put(parameter.getName(), parameter);
    return parameter;
  } // end of addParameter()
  
  /**
   * Builder-pattern method for adding a parameter to the set.
   * @param parameter The parameter to add.
   * @return This ParameterSet.
   */
  public ParameterSet parameter(Parameter parameter) {
    addParameter(parameter);
    return this;
  } // end of parameter()
  
  /**
   * Invokes {@link Parameter#apply(Object)} for all parameters in the collection.
   * @param bean The object whose bean attribute should be set.
   */
  public void apply(Object bean) {
    for (Parameter parameter : values()) {
      try {
        parameter.apply(bean);
      } catch(Exception exception) {}
    }
  } // end of apply()  
  
  /**
   * Adds parameters to this set which correspond to any fields of the class of the given object
   * annotated as {@link ParameterField}s. 
   * @param bean The object whose class may have {@link ParameterField} attributes.
   * @return A reference to this set.
   */
  @SuppressWarnings("rawtypes")
  public ParameterSet addParameters(Object bean) {
    Class c = bean.getClass();
    for (Field field : c.getDeclaredFields()) {
      if (field.isAnnotationPresent(ParameterField.class)) {
        ParameterField annotation = field.getAnnotation(ParameterField.class);
        if (!containsKey(field.getName())) { // parameter is not present
          // so add it
          String label = annotation.label();
          if (label.length() == 0) label = field.getName();
          Parameter p = new Parameter(
            field.getName(), field.getType(), label, annotation.value(),
            annotation.required());
          p.extractValue(bean);
          addParameter(p);
        } // parameter is not present           
      } // ParameterField
    } // next field
    return this;
  } // end of addParameters()
  
  /**
   * Returns a list of {@link Parameter}s that are marked as required, but which have no value set.
   * @return A possibly empty list of parameters that should have a value but don't.
   */
  public ParameterSet unsetRequiredParameters() {
    ParameterSet unset = new ParameterSet();
    for (Parameter p : values()) {
      if (p.getRequired() && p.getValue() == null) {
        unset.addParameter(p);
      }
    }
    return unset;
  } // end of unsetRequiredParameters()

  /**
   * Returns a list of {@link Parameter}s that have a collection of possible values, but the
   * assigned value is not among them.
   * @return A possibly empty list of parameters that have a value not in {@link Parameter#possibleValues}.
   */
  public ParameterSet invalidValueParameters() {
    ParameterSet invalid = new ParameterSet();
    for (Parameter p : values()) {
      if (p.getPossibleValues() != null
          && p.getValue() != null
          && !p.getPossibleValues().contains(p.getValue())) {
        invalid.addParameter(p);
      }
    }
    return invalid;
  } // end of invalidValueParameters()

  /**
   * Serializes the set as a JsonArray.
   * @return A JSON representation of the parameters.
   */
  public JsonArray toJson() {
    JsonArrayBuilder parameters = Json.createArrayBuilder();
    for (Parameter p : values()) {
      parameters.add(p.toJson());
    }
    return parameters.build();
  }

  /**
   * Deserializes the set from a JsonArray.
   * @return A JSON representation of the parameters.
   */
  public ParameterSet fromJson(JsonArray json) {
    for (int p = 0; p < json.size(); p++) {
      addParameter((Parameter)new Parameter().fromJson(json.getJsonObject(p)));
    } 
    return this;
  }

} // end of class ParameterSet
