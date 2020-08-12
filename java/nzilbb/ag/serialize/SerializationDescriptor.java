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
package nzilbb.ag.serialize;

import java.net.URL;
import java.util.Vector;
import nzilbb.ag.*;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * A descriptor that describes the attributes of a serializer or deserializer.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class SerializationDescriptor {
   
   // Attributes:

   /**
    * The name of the format or storage system that is used for serialization/deserialization.
    * @see #getName()
    * @see #setName(String)
    */
   protected String name;
   /**
    * Getter for {@link #name}: The name of the format or storage system that is used for
    * serialization/deserialization. 
    * @return The name of the format or storage system that is used for
    * serialization/deserialization. 
    */
   public String getName() { return name; }
   /**
    * Setter for {@link #name}: The name of the format or storage system that is used for
    * serialization/deserialization. 
    * @param newName The name of the format or storage system that is used for
    * serialization/deserialization. 
    */
   public SerializationDescriptor setName(String newName) { name = newName; return this; }

   /**
    * The MIME type of the format.
    * @see #getMimeType()
    * @see #setMimeType(String)
    */
   protected String mimeType;
   /**
    * Getter for {@link #mimeType}: The MIME type of the format.
    * @return The MIME type of the format.
    */
   public String getMimeType() { return mimeType; }
   /**
    * Setter for {@link #mimeType}: The MIME type of the format.
    * @param newMimeType The MIME type of the format.
    */
   public SerializationDescriptor setMimeType(String newMimeType) { mimeType = newMimeType; return this; }

   /**
    * The normal file name suffixes (extensions) of this MIME type.
    * @see #getFileSuffixes()
    * @see #setFileSuffixes(Vector)
    */
   protected Vector<String> fileSuffixes = new Vector<String>();
   /**
    * Getter for {@link #fileSuffixes}: The normal file name suffixes (extensions) of this
    * MIME type. 
    * @return The normal file name suffixes (extensions) of this MIME type.
    */
   public Vector<String> getFileSuffixes() { return fileSuffixes; }
   /**
    * Setter for {@link #fileSuffixes}: The normal file name suffixes (extensions) of this
    * MIME type. 
    * @param newFileSuffixes The normal file name suffixes (extensions) of this MIME type.
    */
   public SerializationDescriptor setFileSuffixes(Vector<String> newFileSuffixes) { fileSuffixes = newFileSuffixes; return this; }
   /**
    * The version of the serializer/deserializer.
    * @see #getVersion()
    * @see #setVersion(String)
    */
   protected String version;
   /**
    * Getter for {@link #version}: The version of the serializer/deserializer.
    * @return The version of the serializer/deserializer.
    */
   public String getVersion() { return version; }
   /**
    * Setter for {@link #version}: The version of the serializer/deserializer.
    * @param newVersion The version of the serializer/deserializer.
    */
   public SerializationDescriptor setVersion(String newVersion) { version = newVersion; return this; }

   /**
    * URL to an icon for this MIME type. It is recommended to provide an icon to an SVG image. 
    * If none is explicitly provided, {@link #getIcon()} returns a generic icon.
    * @see #getIcon()
    * @see #setIcon(URL)
    */
   protected URL icon;
   /**
    * Getter for {@link #icon}: URL to an icon for this MIME type.
    * @return URL to an icon for this MIME type.
    */
   public URL getIcon() { 
      if (icon == null) {
         // no specific icon, so return a generic one
         return getClass().getResource("text-x-generic.svg");
      }
      return icon; 
   }
   /**
    * Setter for {@link #icon}: URL to an icon for this MIME type.
    * @param newIcon URL to an icon for this MIME type.
    */
   public SerializationDescriptor setIcon(URL newIcon) { icon = newIcon; return this; }

   /**
    * The (maximum) number of inputs required - e.g. the number of annotation files that
    * make up a graph. Default is 1. 
    * @see #getNumberOfInputs()
    * @see #setNumberOfInputs(int)
    */
   protected int numberOfInputs = 1;
   /**
    * Getter for {@link #numberOfInputs}: The (maximum) number of inputs required -
    * e.g. the number of annotation files that make up a graph. Default is 1. 
    * @return The (maximum) number of inputs required - e.g. the number of annotation
    * files that make up a graph.  
    */
   public int getNumberOfInputs() { return numberOfInputs; }
   /**
    * Setter for {@link #numberOfInputs}: The (maximum) number of inputs required -
    * e.g. the number of annotation files that make up a graph. 
    * @param newNumberOfInputs The (maximum) number of inputs required - e.g. the number
    * of annotation files that make up a graph. 
    */
   public SerializationDescriptor setNumberOfInputs(int newNumberOfInputs) { numberOfInputs = newNumberOfInputs; return this; }

   /**
    * Minimum version of the nzilbb.ag API supported by the serializer.
    * @see #getMinimumApiVersion()
    * @see #setMinimumApiVersion(String)
    * @see Constants#VERSION
    */
   protected String minimumApiVersion = "20160308.0920";
   /**
    * Getter for {@link #minimumApiVersion}: Minimum version of the nzilbb.ag API
    * supported by the serializer. 
    * @return Minimum version of the nzilbb.ag API supported by the serializer.
    */
   public String getMinimumApiVersion() { return minimumApiVersion; }
   /**
    * Setter for {@link #minimumApiVersion}: Minimum version of the nzilbb.ag API
    * supported by the serializer. 
    * @param newMinimumApiVersion Minimum version of the nzilbb.ag API supported by the serializer.
    */
   public SerializationDescriptor setMinimumApiVersion(String newMinimumApiVersion) { minimumApiVersion = newMinimumApiVersion; return this; }
   
   // Methods:
   
   /**
    * Default constructor
    */
   public SerializationDescriptor() {
   } // end of constructor

   /**
    * Attribute constructor.
    * @param name The name of the format or storage system that is used for
    * serialization/deserialization. 
    * @param version The version of the serializer/deserializer.
    * @param mimeType The MIME type of the format.
    * @param fileSuffixes The normal file name suffixes (extensions) of this MIME type.
    * @param minimumApiVersion Minimum version of the nzilbb.ag API supported by the serializer.
    * @param icon URL to an icon for this MIME type.
    * @param numberOfInputs The (maximum) number of inputs required - e.g. the number of
    * annotation files that make up a graph. 
    */
   public SerializationDescriptor(
      String name, String version, String mimeType, Vector<String> fileSuffixes,
      String minimumApiVersion, URL icon, int numberOfInputs) {
      setName(name);
      setVersion(version);
      setMimeType(mimeType);
      getFileSuffixes().addAll(fileSuffixes);
      setMinimumApiVersion(minimumApiVersion);
      setIcon(icon);
      setNumberOfInputs(numberOfInputs);
   } // end of constructor

   /**
    * Attribute constructor.
    * @param name The name of the format or storage system that is used for
    * serialization/deserialization. 
    * @param version The version of the serializer/deserializer.
    * @param mimeType The MIME type of the format.
    * @param fileSuffix The normal file name suffix (extension) of this MIME type.
    * @param minimumApiVersion Minimum version of the nzilbb.ag API supported by the serializer.
    * @param icon URL to an icon for this MIME type.
    * @param numberOfInputs The (maximum) number of inputs required - e.g. the number of
    * annotation files that make up a graph. 
    */
   public SerializationDescriptor(
      String name, String version, String mimeType, String fileSuffix, String minimumApiVersion,
      URL icon, int numberOfInputs) {
      setName(name);
      setVersion(version);
      setMimeType(mimeType);
      getFileSuffixes().add(fileSuffix);
      setMinimumApiVersion(minimumApiVersion);
      setIcon(icon);
      setNumberOfInputs(numberOfInputs);
   } // end of constructor

   /**
    * Attribute constructor.
    * @param name The name of the format or storage system that is used for
    * serialization/deserialization. 
    * @param version The version of the serializer/deserializer.
    * @param mimeType The MIME type of the format.
    * @param fileSuffix The normal file name suffix (extension) of this MIME type.
    * @param minimumApiVersion Minimum version of the nzilbb.ag API supported by the serializer.
    * @param icon URL to an icon for this MIME type.
    */
   public SerializationDescriptor(
      String name, String version, String mimeType, String fileSuffix, String minimumApiVersion,
      URL icon) {
      setName(name);
      setVersion(version);
      setMimeType(mimeType);
      getFileSuffixes().add(fileSuffix);
      setMinimumApiVersion(minimumApiVersion);
      setIcon(icon);
   } // end of constructor

   /**
    * Attribute constructor.
    * @param name The name of the format or storage system that is used for
    * serialization/deserialization. 
    * @param version The version of the serializer/deserializer.
    * @param mimeType The MIME type of the format.
    * @param fileSuffix The normal file name suffix (extension) of this MIME type.
    * @param icon URL to an icon for this MIME type.
    */
   public SerializationDescriptor(
      String name, String version, String mimeType, String fileSuffix, URL icon) {
      setName(name);
      setVersion(version);
      setMimeType(mimeType);
      getFileSuffixes().add(fileSuffix);
      setMinimumApiVersion(minimumApiVersion);
      setIcon(icon);
   } // end of constructor

   /**
    * Attribute constructor.
    * @param name The name of the format or storage system that is used for
    * serialization/deserialization. 
    * @param version The version of the serializer/deserializer.
    * @param mimeType The MIME type of the format.
    * @param fileSuffix The normal file name suffix (extension) of this MIME type.
    * @param minimumApiVersion Minimum version of the nzilbb.ag API supported by the serializer.
    */
   public SerializationDescriptor(
      String name, String version, String mimeType, String fileSuffix, String minimumApiVersion) {
      setName(name);
      setVersion(version);
      setMimeType(mimeType);
      getFileSuffixes().add(fileSuffix);
      setMinimumApiVersion(minimumApiVersion);
   } // end of constructor

   /**
    * Attribute constructor.
    * @param name The name of the format or storage system that is used for
    * serialization/deserialization. 
    * @param version The version of the serializer/deserializer.
    * @param mimeType The MIME type of the format.
    * @param fileSuffix The normal file name suffix (extension) of this MIME type.
    */
   public SerializationDescriptor(
      String name, String version, String mimeType, String fileSuffix) {
      setName(name);
      setVersion(version);
      setMimeType(mimeType);
      getFileSuffixes().add(fileSuffix);
   } // end of constructor

   /**
    * Attribute constructor.
    * @param name The name of the format or storage system that is used for
    * serialization/deserialization. 
    * @param version The version of the serializer/deserializer.
    */
   public SerializationDescriptor(String name, String version) {
      setName(name);
      setVersion(version);
   } // end of constructor

   /**
    * Constructor from JSON.
    * @param json A JSON representation of the object.
    */
   public SerializationDescriptor(JSONObject json) {
      setName(json.optString("name"));
      setVersion(json.optString("version"));
      if (json.has("icon")) {
         try {
            setIcon(new URL(json.getString("icon")));
         } catch(Throwable exception) {}
      }
      setName(json.optString("name"));
      setMimeType(json.optString("mimeType"));
      setMinimumApiVersion(json.optString("minimumApiVersion"));
      if (json.has("numberOfInputs")) setNumberOfInputs(json.getInt("numberOfInputs"));
      if (json.has("fileSuffixes")) {
         JSONArray array = json.getJSONArray("fileSuffixes");
         if (array != null) {
            for (int i = 0; i < array.length(); i++) {
               getFileSuffixes().add(array.getString(i));
            }
         }
      }
   } // end of constructor
   
   /**
    * Getter for {@link #fileSuffixes}: The normal file name suffixes (extensions) of this
    * MIME type. 
    * @return The normal file name suffixes (extensions) of this MIME type, as an array of String.
    */
   public String[] getFileSuffixesArray() { return fileSuffixes.toArray(new String[0]); }
   
   /**
    * Determines whether this object is equal to another.
    * @param o Other object.
    * @return true if both objects represent the same serialization descriptor (i.e. they
    * have the same {@link #name}, {@link #version}, and {@link #mimeType}), false
    * otherwise. 
    */
   public boolean equals(Object o)
   {
      if (this == o) return true;
      if (o instanceof SerializationDescriptor)
      {
         SerializationDescriptor other = (SerializationDescriptor)o;
         return name.equals(other.getName())
            && version.equals(other.getVersion())
            && mimeType.equals(other.getMimeType());
      }
      return false;
   } // end of equals()
   
   /**
    * Returns a hash code value for the object. This override ensures that descriptors for
    * which {@link #equals(Object)} returns true also have the same hash, for hash-table
    * based collections. 
    */
   public int hashCode() {
      return (name+version+mimeType).hashCode();
   } // end of hashCode()
   
   /**
    * Override that describes the serialization.
    */
   @Override
   public String toString() {
      return "\""+getName()+"\" " + getMimeType() + " ("+getVersion()+")";
   } // end of toString()
   
} // end of class SerializationDescriptor
