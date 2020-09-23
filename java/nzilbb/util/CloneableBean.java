//
// Copyright 2016-2019 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 * Functions for serializing/deserializing classes that have annotated at least some bean
 * property getters with @{@link ClonedProperty}. 
 * @author Robert Fromont robert@fromont.net.nz
 */
public interface CloneableBean {
   
   /**
    * Keys for attributes that are cloned/serialized - i.e. when a subclass object is cloned, only
    * these attributes are copied into the clone. 
    * @return A set of attributes whose values are cloned.
    */
   default public Set<String> getClonedAttributes() {
      TreeSet<String> properties = new TreeSet<String>();
      Method[] methods = getClass().getMethods();
      for (Method method : methods) {
	 ClonedProperty annotation = method.getAnnotation(ClonedProperty.class);
	 if (annotation != null) {
            String getterName = method.getName();
            String property = getterName.substring(3,4).toLowerCase() + getterName.substring(4);
            properties.add(property);
         } // ClonedProperty getter
      } // next method
      return properties;
   } // end of getTrackedAttributes()

   /**
    * Serializes the bean as a JsonObject.
    * @return A JSON representation of the bean.
    */
   @SuppressWarnings({"rawtypes"})
   default public JsonObject toJson() {      
      JsonObjectBuilder json = Json.createObjectBuilder();
      // look for getters annotated with @ClonedProperty
      Method[] methods = getClass().getMethods();
      for (Method method : methods) {
	 ClonedProperty annotation = method.getAnnotation(ClonedProperty.class);
	 if (annotation != null) {
            // add the value to the JSON object
            try {
               Object value = method.invoke(this);
               if (value != null) {
                  String key = method.getName().substring(3,4).toLowerCase()
                     + method.getName().substring(4);
                  Class parameterClass = method.getReturnType();
                  if (parameterClass.equals(String.class)) {
                     json = json.add(key, (String)value);
                  } else if (parameterClass.equals(Integer.class)) {
                     json = json.add(key, (Integer)value);
                  } else if (parameterClass.equals(int.class)) {
                     json = json.add(key, (int)value);
                  } else if (parameterClass.equals(Double.class)) {
                     json = json.add(key, (Double)value);
                  } else if (parameterClass.equals(double.class)) {
                     json = json.add(key, (Double)value);
                  } else if (parameterClass.equals(Long.class)) {
                     json = json.add(key, (Long)value);
                  } else if (parameterClass.equals(long.class)) {
                     json = json.add(key, (long)value);
                  } else if (parameterClass.equals(Boolean.class)) {
                     json = json.add(key, (Boolean)value);
                  } else if (parameterClass.equals(boolean.class)) {
                     json = json.add(key, (boolean)value);
                  } else if (parameterClass.equals(URL.class)) {
                     json = json.add(key, value.toString());
                  } else if (value instanceof CloneableBean) {
                     json = json.add(key, ((CloneableBean)value).toJson());
                  } else if (value instanceof List) {
                     JsonArrayBuilder list = Json.createArrayBuilder();
                     for (Object e : (List)value) {
                        list.add(e.toString());
                     }
                     json = json.add(key, list);
                  } else if (value instanceof Map) {
                     JsonObjectBuilder map = Json.createObjectBuilder();
                     for (Object k : ((Map)value).keySet()) {
                        Object v = ((Map)value).get(k);
                        if (v instanceof CloneableBean) {
                           map.add(k.toString(), ((CloneableBean)v).toJson());
                        } else {
                           map.add(k.toString(), v.toString());
                        }
                     }
                     json = json.add(key, map);
                  }
                  // ignore any other types
               } // value isn't null
            } catch(IllegalAccessException exception) {
               System.err.println(
                  "CloneableBean.toJsonObject - can't " + method.getName() + ": " + exception);
            } catch(InvocationTargetException exception) {
               System.err.println(
                  "CloneableBean.toJsonObject - can't " + method.getName() + ": " + exception);
            }
         } // ClonedProperty getter
      } // next method
      return json.build();
   } // end of toJson()
   
   /**
    * Initializes the bean with the given JSON representation.
    * @param json
    * @return A reference to this bean.
    */
   @SuppressWarnings({"rawtypes","unchecked"})
   default public CloneableBean fromJson(JsonObject json) {
      for (String key : json.keySet()) {
         if (!json.isNull(key)) {
            // is there a setter and an annotated getter?
            Object value = json.get(key);
            Method setter = setter(key);
            Method getter = getter(key);
            ClonedProperty annotation = getter==null?null:getter.getAnnotation(ClonedProperty.class);
            if (annotation != null && setter != null) { // is a bean attribute
               if (value instanceof JsonObject) { // complex object value
                  JsonObject objectValue = (JsonObject)value;
                  // what type are we expecting?
                  Class type = setter.getParameterTypes()[0];
                  boolean isMap = false;
                  for (Class i : type.getInterfaces()) {
                     if (i.equals(Map.class)) isMap = true;
                  }
                  if (isMap) {
                     try {
                        Map map = (Map)type.getConstructor().newInstance();
                        for (String k : objectValue.keySet()) {
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
                     } catch(Exception exception) {
                        System.err.println(
                           "CloneableBean.fromJsonObject - can't set complex " + key
                           + ": " + exception);
                     }
                  }
               } // complex object value
               else if (value instanceof JsonArray) { // list value
                  JsonArray objectValue = (JsonArray)value;
                  // what type are we expecting?
                  Class type = setter.getParameterTypes()[0];
                  boolean isList = false;
                  for (Class i : type.getInterfaces()) {
                     if (i.equals(List.class)) isList = true;
                  }
                  if (isList) {
                     try {
                        List list = (List)type.getConstructor().newInstance();
                        for (Object v : objectValue) {
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
                           list.add(v);
                        } // next key/value pair
                        setter.invoke(this, list); 
                     } catch(Exception exception) {
                        System.err.println(
                           "CloneableBean.fromJsonObject - can't set complex " + key
                           + ": " + exception);
                     }
                  }
               } // complex object value
               else
               { // simple value
                  Class parameterClass = setter.getParameterTypes()[0];
                  if (value instanceof JsonString) {
                     if (parameterClass.equals(URL.class)) {
                        try {
                           value = new URL(((JsonString)value).getString());
                        } catch(MalformedURLException exception) {
                           value = ((JsonString)value).getString();
                        }
                     } else {
                        value = ((JsonString)value).getString();
                     }
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
                  try {
                     setter.invoke(this, value);
                  } catch(Exception exception) {
                     System.err.println(
                        "CloneableBean.fromJsonObject - can't set "
                        + key + " ("+value.getClass().getName()+"): " + exception);
                  }
               } // simple value
            } // is a bean attribute
         } // value is not null
      } // next attribute
      return this;
   } // end of fromJson()

   /**
    * Copies the other bean's cloned properties to this bean.
    */
   default public void clonePropertiesFrom(CloneableBean other, String except) {
      try
      {
         for (String key : getClonedAttributes()) { // copy tracked attributes
            if (key.equals(except)) continue;	    
            Method getter = getter(key);
            Method setter = setter(key);
            setter.invoke(this, getter.invoke(other));
         }
      } catch(Exception exception) {
         System.err.println(
            "CloneableBean.clonePropertiesFrom(): Could not copy " + other + ": " + exception);
         exception.printStackTrace(System.err);
      }
   } // end of clonePropertiesFrom()

   /**
    * Access the class's getter for the given attribute.
    * @param key
    * @return The getter, or null if there isn't one.
    */
   default public Method getter(String key) {
      try {
         String getterName = "get" + key.substring(0,1).toUpperCase() + key.substring(1);
         return getClass().getMethod(getterName);
      } catch(Throwable exception) {
         return null;
      }	 
   } // end of getter()

   /**
    * Access the class's setter for the given attribute.
    * @param key
    * @return The setter, or null if there isn't one.
    */
   default public Method setter(String key) {
      try {
         String setterName = "set" + key.substring(0,1).toUpperCase() + key.substring(1);
         try {
            return getClass().getMethod(setterName, String.class); // labels, etc.
         } catch(NoSuchMethodException x1) {
            try {
               return getClass().getMethod(setterName, int.class); // Annotation.ordinal
            } catch(NoSuchMethodException x2) {
               try {
                  return getClass().getMethod(setterName, Integer.class); // confidence
               } catch(NoSuchMethodException x3) {
                  try {
                     return getClass().getMethod(setterName, boolean.class); // Layer.peers, etc...
                  } catch(NoSuchMethodException x4) {
                     try {
                        return getClass().getMethod(setterName, LinkedHashMap.class); // Layer.validLabels
                     } catch(NoSuchMethodException x5) {
                        try {
                           return getClass().getMethod(setterName, Vector.class); // SerializationDescriptor.fileSuffixes
                        } catch(NoSuchMethodException x6) {
                           try {
                              return getClass().getMethod(setterName, URL.class); // SerializationDescriptor.icon
                           } catch(NoSuchMethodException x7) {
                              return getClass().getMethod(setterName, Double.class); // Anchor.offset
                           }
                        }
                     }
                  }
               }
            }
         }
      } catch(Throwable exception) {
         return null;
      }	 
   } // end of setter()

} // end of class CloneableBean
