//
// Copyright 2015 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag.serialize;

/**
 * A descriptor that describes the attributes of a serializer or deserializer.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class SerializationDescriptor
{
   // Attributes:

   /**
    * The name of the format or storage system that is used for serialization/deserialization.
    * @see #getName()
    * @see #setName(String)
    */
   protected String name;
   /**
    * Getter for {@link #name}: The name of the format or storage system that is used for serialization/deserialization.
    * @return The name of the format or storage system that is used for serialization/deserialization.
    */
   public String getName() { return name; }
   /**
    * Setter for {@link #name}: The name of the format or storage system that is used for serialization/deserialization.
    * @param newName The name of the format or storage system that is used for serialization/deserialization.
    */
   public void setName(String newName) { name = newName; }

   /**
    * The MIME type of the format, if any.
    * @see #getMimeType()
    * @see #setMimeType(String)
    */
   protected String mimeType;
   /**
    * Getter for {@link #mimeType}: The MIME type of the format, if any.
    * @return The MIME type of the format, if any.
    */
   public String getMimeType() { return mimeType; }
   /**
    * Setter for {@link #mimeType}: The MIME type of the format, if any.
    * @param newMimeType The MIME type of the format, if any.
    */
   public void setMimeType(String newMimeType) { mimeType = newMimeType; }

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
   public void setVersion(String newVersion) { version = newVersion; }

   
   // Methods:
   
   /**
    * Default constructor
    */
   public SerializationDescriptor()
   {
   } // end of constructor

   /**
    * Attribute constructor.
    * @param name The name of the format or storage system that is used for serialization/deserialization.
    * @param version The version of the serializer/deserializer.
    * @param mimeType The MIME type of the format, if any.
    */
   public SerializationDescriptor(String name, String version, String mimeType)
   {
      setName(name);
      setVersion(version);
      setMimeType(mimeType);
   } // end of constructor

   /**
    * Attribute constructor.
    * @param name The name of the format or storage system that is used for serialization/deserialization.
    * @param version The version of the serializer/deserializer.
    */
   public SerializationDescriptor(String name, String version)
   {
      setName(name);
      setVersion(version);
   } // end of constructor

} // end of class SerializationDescriptor
