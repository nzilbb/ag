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
package nzilbb.ag.serialize;

import java.util.Spliterator;
import java.util.Vector;
import java.util.function.Consumer;
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
 *  <li>Serialize the graph using {@link #serialize(Spliterator,String[],Consumer,Consumer,Consumer)}</li>
 *  <li>Possibly display or log warnings consumed during serialize() by 
 *      {@link #serialize(Spliterator,String[],Consumer,Consumer,Consumer)}</li>
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
    *  interface for setting/confirming these parameters. This always returns the required
    *  parameters, whether or not they are fulfilled.
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
    * Possible values for cardinality (given <var>N</var> {@link Graph}s, how many 
    * {@link nzilbb.ag.serialize.util.NamedStream}s are produced) are:
    * <ul>
    *  <li><b>NToOne</b>: only one {@link NamedStream} is generated, regardless of the
    * number of {@link Graph}s to serialize.</li>
    *  <li><b>NToN</b>: for each {@link Graph} serialized, exactly one {@link NamedStream}
    *   is generated.</li>
    *  <li><b>NToM</b>: the number of {@link Graph}s to serialize is unrelated to the
    *   number of {@link NamedStream} generated.</li>
    * </ul>
    * @see #getCardinality()
    */
   public enum Cardinality { NToOne, NToN, NToM }
   
   /**
    * Determines the cardinality between graphs and serialized streams.
    * <p>This can be useful to know when deciding, for example, whether a given
    * serialization with produce a single stream that can be returned directly,
    * vs. multiple streams that should be zipped into a single stream result.
    * @return The cardinality between graphs and serialized streams.
    */
   default public Cardinality getCardinality()
   {
      return Cardinality.NToM;
   }
   
   /**
    * Serializes the given series of graphs, generating one or more {@link NamedStream}s.
    * <p>Many data formats will only yield one stream per graph (e.g. Transcriber
    * transcript or Praat textgrid), however there are formats that use multiple files for
    * the same transcript (e.g. XWaves, EmuR), and others still that will produce one
    * stream from many Graphs (e.g. CSV).
    * <p>The method is synchronous in the sense that it should not return until all graphs
    * have been serialized.
    * @param graphs The graphs to serialize.
    * @param layerIds The IDs of the layers to include, or null for all layers.
    * @param consumer The consumer receiving the streams.
    * @param warnings A consumer for (non-fatal) warning messages.
    * @param errors A consumer for (fatal) error messages.
    * @throws SerializerNotConfiguredException if the object has not been configured.
    */
   public void serialize(Spliterator<Graph> graphs, String[] layerIds, Consumer<NamedStream> consumer, Consumer<String> warnings, Consumer<SerializationException> errors) 
      throws SerializerNotConfiguredException;
   
   /**
    * Determines how far through the serialization is.
    * @return An integer between 0 and 100 (inclusive), or null if progress can not be calculated.
    */
   public Integer getPercentComplete();

   /**
    * Cancel the serialization in course (if any).
    */
   public void cancel();

} // end of interface IDeserialize
