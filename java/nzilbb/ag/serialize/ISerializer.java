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
package nzilbb.ag.serialize;

import java.util.Vector;
import nzilbb.ag.Graph;
import nzilbb.ag.Schema;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;

/**
 * Interface for serializing a graph to streams of data.
 * <p>Serialization takes place in the following phases:
 * <ol>
 *  <li>Configure serializer using {@link #configure(ParameterSet,Schema)}</li>
 *  <li>Determine which (if any) layers are required for the serialization by calling
 *   {@link #getRequiredLayers()}, which returns a list of layer IDs.</li>
 *  <li>Serialize the graph using {@link #serialize(Graph[],String[])}</li>
 *  <li>Possibly display or log warnings returned by {@link #getWarnings()}</li>
 * </ol>
 * @author Robert Fromont robert@fromont.net.nz
 */
public interface ISerializer
{
   /**
    * Returns the deserializer's descriptor
    * @return The deserializer's descriptor
    */
   public SerializationDescriptor getDescriptor();

   /**
    * Sets parameters for serializer as a whole.  This might include database connection
    * parameters, locations of supporting files, standard layer mappings, etc.
    * <p>When the serializer is installed, this method should be invoked with an empty parameter
    *  set, to discover what (if any) general configuration is required. If parameters are
    *  returned, and user interaction is possible, then the user may be presented with an
    *  interface for setting/confirming these parameters. This always returns th}e required parameters, 
    *  whether or not they are fulfilled.
    * @param configuration The general configuration for the serializer. 
    * @param schema The layer schema, definining layers and the way they interrelate.
    * @return A list of configuration parameters (still) must be set before
    *  {@link ISerializer#getRequiredLayers()} can be called. If this is an empty list,
    *  {@link ISerializer#getRequiredLayers()} can be called. If it's not an empty list,
    *  this method must be invoked again with the returned parameters' values set.
    */
   public ParameterSet configure(ParameterSet configuration, Schema schema);

   /**
    * Determines which layers, if any, must be present in the graph that will be serialized.
    * @return A list of IDs of layers that must be present in the graph that will be serialized.
    * @throws SerializationParametersMissingException If not all required parameters have a value.
    */
   public String[] getRequiredLayers() throws SerializationParametersMissingException;

   /**
    * Serializes the given graph, generating one or more {@link NamedStream}s.
    * <p>Many data formats will only yield one stream (e.g. Transcriber transcript or Praat
    *  textgrid), however there are formats that use multiple files for the same transcript
    *  (e.g. XWaves, EmuR), which is why this method returns a list. There are formats that
    *  are capable of storing multiple transcripts in the same file (e.g. AGTK, Transana XML
    *  export), which is why this method accepts a list.
    * @param graphs The graphs to serialize.
    * @param layerIds The IDs of the layers to include, or null for all layers.
    * @return A list of named streams that contain the serialization in the given format. 
    * @throws SerializerNotConfiguredException if the object has not been configured.
    * @throws SerializationException if errors occur during deserialization.
    */
   public NamedStream[] serialize(Graph[] graphs, String[] layerIds) 
      throws SerializerNotConfiguredException, SerializationException;

   /**
    * Returns any warnings that may have arisen during the last execution of {@link #serialize(Graph[],String[])}.
    * @return A possibly empty list of warnings.
    */
   public String[] getWarnings();


} // end of interface IDeserialize
