//
// Copyright 2015-2016 New Zealand Institute of Language, Brain and Behaviour, 
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
import java.io.IOException;
import nzilbb.ag.Graph;
import nzilbb.ag.Schema;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;

/**
 * Interface for deserializing a graph from streams of data.
 * <p>Deserialization takes place in the following phases:
 * <ol>
 *  <li>Configure deserializer using {@link #configure(ParameterSet,Schema)}</li>
 *  <li>Load serialized form using {@link #load(NamedStream[],Schema)}, which returns a list of parameters that should be set.</li>
 *  <li>Set deserialization parameters using {@link #setParameters(ParameterSet)}</li>
 *  <li>Generation graph(s) using {@link #deserialize()}</li>
 *  <li>Possibly display or log warnings returned by {@link #getWarnings()}</li>
 * </ol>
 * TODO it should be possible to not start with a schema.
 * @author Robert Fromont robert@fromont.net.nz
 */
public interface IDeserializer
{
   /**
    * Returns the deserializer's descriptor
    * @return The deserializer's descriptor
    */
   public SerializationDescriptor getDescriptor();

   /**
    * Sets parameters for deserializer as a whole.  This might include database connection
    * parameters, locations of supporting files, standard layer mappings, etc.
    * <p>When the deserializer is installed, this method should be invoked with an empty parameter
    *  set, to discover what (if any) general configuration is required. If parameters are
    *  returned, and user interaction is possible, then the user may be presented with an
    *  interface for setting/confirming these parameters.  Once the parameters are set, this
    *  method can be invoked again with the required values. 
    * @param configuration The general configuration for the deserializer. 
    * @param schema The layer schema, definining layers and the way they interrelate.
    * @return A list of configuration parameters (still) must be set before
    *  {@link IDeserializer#setParameters(ParameterSet)} can be invoked. If this is an empty list, 
    *  {@link IDeserializer#setParameters(ParameterSet)} can be invoked. If it's not an empty list,
    *  this method must be invoked again with the returned parameters' values set.
    */
   public ParameterSet configure(ParameterSet configuration, Schema schema);

   /**
    * Loads the serialized form of the graph, using the given set of named streams.
    * @param streams A list of named streams that contain all the
    *  transcription/annotation data required, and possibly (a) stream(s) for the media annotated.
    * @param schema The layer schema, definining layers and the way they interrelate.
    * @return A list of parameters that require setting before {@link IDeserializer#deserialize()}
    *  can be invoked. This may be an empty list, and may include parameters with the value
    *  already set to a workable default. If there are parameters, and user interaction is
    *  possible, then the user may be presented with an interface for setting/confirming these
    *  parameters, before they are then passed to {@link IDeserializer#setParameters(ParameterSet)}.
    * @throws SerializationException If the graph could not be loaded.
    * @throws IOException On IO error.
    * @throws SerializerNotConfiguredException If the configuration is not sufficient for deserialization.
    */
   public ParameterSet load(NamedStream[] streams, Schema schema) throws SerializationException, IOException, SerializerNotConfiguredException;

   /**
    * Sets parameters for a given deserialization operation, after loading the serialized form
    * of the graph. This might include mappings from format-specific objects like tiers to graph
    * layers, etc.
    * @param parameters The configuration for a given deserialization operation.
    * @throws SerializationParametersMissingException If not all required parameters have a value.
    */
   public void setParameters(ParameterSet parameters) throws SerializationParametersMissingException;


   /**
    * Deserializes the serialized data, generating one or more {@link Graph}s.
    * <p>Many data formats will only yield one graph (e.g. Transcriber
    * transcript or Praat textgrid), however there are formats that
    * are capable of storing multiple transcripts in the same file
    * (e.g. AGTK, Transana XML export), which is why this method
    * returns a list.
    * @return A list of valid (if incomplete) {@link Graph}s. 
    * @throws SerializerNotConfiguredException if the object has not been configured.
    * @throws SerializationParametersMissingException if the parameters for this particular graph have not been set.
    * @throws SerializationException if errors occur during deserialization.
    */
   public Graph[] deserialize() 
      throws SerializerNotConfiguredException, SerializationParametersMissingException, SerializationException;

   /**
    * Returns any warnings that may have arisen during the last execution of {@link #deserialize()}.
    * @return A possibly empty list of warnings.
    */
   public String[] getWarnings();


} // end of interface IDeserialize
