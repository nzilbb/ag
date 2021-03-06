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

import java.util.Vector;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;

/**
 * Graph deserializer that retrieves identified graphs from a store such as a relational database.
 * @see GraphDeserializer
 * @author Robert Fromont robert@fromont.net.nz
 */
public interface StoreGraphDeserializer extends GraphDeserializer {
   
   /**
    * Loads the serialized form of the graph, using the given identifier.
    * @param graphId Identifier for the graph to serialize.
    * @return A list of parameters that require setting before
    * {@link GraphDeserializer#deserialize()} can be invoked. This may be an empty list, and may
    * include parameters with the value already set to a workable default. If there are
    * parameters, and user interaction is possible, then the user may be presented with an
    * interface for setting/confirming these parameters, before they are then passed to
    * {@link GraphDeserializer#setParameters(ParameterSet)}. 
    * @throws Exception If the graph could not be loaded.
    */
   public ParameterSet load(String graphId) throws Exception;
   
} // end of interface IStoreDeserializer
