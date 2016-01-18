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

import java.util.Vector;
import nzilbb.ag.Graph;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;

/**
 * Interface for deserializing a graph.
 * <p>Deserialization might be from a relational database, a file or set of files in a particular format, etc. There are derived interfaces that specify the possible sources for serialized data, e.g. {@link IStoreDeserializer}
 * <p>Deserialization takes place in the following phases:
 * <ol>
 *  <li>Configure deserializer using {@link #configure(ParameterSet)}</li>
 *  <li>Load serialized form using one the derived interface's load() method (e.g. {@link IStoreDeserializer#load(String)}), which returns a list of parameters that should be set.</li>
 *  <li>Set deserialization parameters using {@link #setParameters(ParameterSet)}</li>
 *  <li>Generation graph(s) using {@link #deserialize()}</li>
 *  <li>Possibly display or log warnings returned by {@link #getWarnings()}</li>
 * </ol>
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
    * Sets parameters for deserializer as a whole.  This might include database connection parameters, locations of supporting files, etc.
    * @param configuration
    * @throws DeserializerNotConfiguredException
    */
   public void configure(ParameterSet configuration) throws DeserializerNotConfiguredException;

   /**
    * Sets parameters for a given deserialization operation, after loading the serialized form of the graph. This might include mappings from format-specific objects like tiers to graph layers, etc.
    * @param parameters
    * @throws DeserializationParametersMissingException
    */
   public void setParameters(ParameterSet parameters) throws DeserializationParametersMissingException;

   /**
    * Deserializes the serialized data, generating one or more {@link Graph}s.
    * <p>Many data formats will only yield one graph (e.g. Transcriber
    * transcript or Praat textgrid), however there are formats that
    * are capable of storing multiple transcripts in the same file
    * (e.g. AGTK, Transana XML export), which is why this method
    * returns a list.
    * @return A list of valid (if incomplete) {@link Graph}s. 
    * @throws DeserializerNotConfiguredException if the object has not been configured.
    * @throws DeserializationParametersMissingException if the parameters for this particular graph have not been set.
    * @throws DeserializationException if errors occur during deserialization.
    */
   public Vector<Graph> deserialize() 
      throws DeserializerNotConfiguredException, DeserializationParametersMissingException, DeserializationException;

   /**
    * Returns any warnings that may have arisen during the last execution of {@link #deserialize()}.
    * @return A possibly empty list of warnings.
    */
   public Vector<String> getWarnings();


} // end of interface IDeserialize
