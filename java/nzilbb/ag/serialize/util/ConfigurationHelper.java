//
// Copyright 2016 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag.serialize.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Map;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;

import nzilbb.ag.serialize.*;

/**
 * Helper functions for dealing with (de)serializer configurations.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class ConfigurationHelper
{
  // Methods:
   
  /**
   * Ensures the icon file for the given descriptor has been extracted to the given directory.
   * @param descriptor Descriptor of serialization.
   * @param directory Directory to unpack the icon into if it's not already there.
   * @return The icon file.
   * @throws IOException On file IO error.
   */
  public static File SaveConfiguration(SerializationDescriptor descriptor, ParameterSet configuration, File directory)
    throws IOException
  {
    File xmlFile = new File(directory, ConfigurationFilename(descriptor));
    Properties properties = new Properties();
    for (Parameter parameter : configuration.values())
    {
      if (parameter.getValue() != null)
      {
        properties.setProperty(parameter.getName(), parameter.getValue().toString());
      }
    }
    properties.storeToXML(new FileOutputStream(xmlFile), descriptor.getName());
    return xmlFile;
  } // end of EnsureIconFileExists()

  /**
   * Loads configuration for a (de)serializer from an XML file, whose path is determined by the
   * given descriptor and the given directory.
   * @param descriptor Descriptor of serialization.
   * @param directory Directory to unpack the icon into if it's not already there.
   * @param schema Layer schema for retrieval of named layers.
   * @return The XML file.
   * @throws IOException On file IO error.
   */
  public static File LoadConfiguration(SerializationDescriptor descriptor, ParameterSet configuration, File directory, Schema schema)
    throws IOException
  {
    File xmlFile = new File(directory, ConfigurationFilename(descriptor));
    if (xmlFile.exists())
    {
      Properties properties = new Properties();
      properties.loadFromXML(new FileInputStream(xmlFile));
      for (Parameter parameter : configuration.values())
      {
        if (properties.containsKey(parameter.getName()))
        {
          String value = properties.getProperty(parameter.getName());
          if (parameter.getType().equals(Layer.class))
          {
            parameter.setValue(schema.getLayer(value));
          }
          else if (parameter.getType().equals(Integer.class))
          {
            parameter.setValue(Integer.valueOf(value));
          }
          else if (parameter.getType().equals(Double.class))
          {
            parameter.setValue(Double.valueOf(value));
          }
          else if (parameter.getType().equals(Boolean.class))
          {
            parameter.setValue(Boolean.valueOf(value));
          }
          else
          { // everything else given a string
            parameter.setValue(value);
          }
        }
      } // next parameter
    }
    return xmlFile;
  } // end of LoadConfiguration()

  /**
   * Loads a (de)serialization configuratation from the given parameter map (most likely from a
   * servlet request) the icon file for the given descriptor has been extracted to the given directory.
   * @param map A parameter map specifying the parameter names and values (the value is an array
   * of String, consistent with <code>javax.serlvet.ServletRequest.getParameterMap()</code>, but
   * the value is expected to be in the first string of the array.
   * @param keyPrefix An optional prefix for each parameter key.
   * @param parameters A set of parameters to use for looking for map keys and for receiving the
   * values. 
   * @param schema Layer schema for retrieval of named layers.
   * @return The given ParameterSet, with the values specified by the map loaded.
   * @throws IOException On file IO error.
   */
  public static ParameterSet LoadParameters(Map<String,String[]> map, String keyPrefix, ParameterSet parameters, Schema schema)
    throws IOException
  {
    if (keyPrefix == null) keyPrefix = "";
    
    for (Parameter parameter : parameters.values())
    {
      String[] values = map.get(keyPrefix + parameter.getName());
      String value = (values == null || values.length == 0)?null:values[0];
      if (value == null || value.length() == 0)
      {
        if (parameter.getType().equals(Boolean.class))
        { // null boolean means "false"
          parameter.setValue(Boolean.FALSE);
        }
        else
        {
          parameter.setValue(null);
        }
      }
      else if (parameter.getType().equals(Layer.class))
      {
        parameter.setValue(schema.getLayer(value));
      }
      else if (parameter.getType().equals(Integer.class))
      {
        parameter.setValue(new Integer(value));
      }
      else if (parameter.getType().equals(Double.class))
      {
        parameter.setValue(new Double(value));
      }
      else if (parameter.getType().equals(Boolean.class))
      { // if the parameter is set, it's ticked, so TRUE
        parameter.setValue(Boolean.TRUE);
      }
      else
      { // everything else given a string
        parameter.setValue(value);
      }
    } // next parameter
    return parameters;
  } // end of LoadParameters()

  /**
   * Transforms the given descriptor's MIME type name into something that is safe to use as a file name.
   * @param descriptor Descriptor of serialization.
   * @return The descriptor's MIME type name transformed into something that is safe to use as a file name.
   */
  public static String ConfigurationFilename(SerializationDescriptor descriptor)
  {
    String mimeType = descriptor.getMimeType();
    return (mimeType + ".config.xml").replaceAll("[^A-Za-z0-9.]+", "-");
  } // end of ConfigurationFilename()
   
} // end of class IconHelper
