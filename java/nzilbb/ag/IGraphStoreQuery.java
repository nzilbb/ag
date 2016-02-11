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
package nzilbb.ag;

import java.util.Vector;

/**
 * Interface for querying an annotation graph store, a database of graphs.
 * <p>In order to easily support access via scripting in other languages, methods that return lists use arrays rather than collection classes.
 * @author Robert Fromont robert@fromont.net.nz
 */
public interface IGraphStoreQuery
{
   
   /**
    * Gets the store's ID.
    * @return The annotation store's ID.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String getId() throws StoreException, PermissionException;
   
   /**
    * Gets a list of layer IDs (annotation 'types').
    * @return A list of layer IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getLayerIds() throws StoreException, PermissionException; 
   
   /**
    * Gets a list of layer definitions.
    * @return A list of layer definitions.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public Layer[] getLayers() throws StoreException, PermissionException;

   /**
    * Gets a layer definition.
    * @param id ID of the layer to get the definition for.
    * @return The definition of the given layer.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public Layer getLayer(String id) throws StoreException, PermissionException;   

   /**
    * Gets a list of corpus IDs.
    * @return A list of corpus IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getCorpusIds() throws StoreException, PermissionException; 

   /**
    * Gets a list of participant IDs.
    * @return A list of participant IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getParticipantIds() throws StoreException, PermissionException; 

   /**
    * Gets a list of graph IDs.
    * @return A list of graph IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getGraphIds() throws StoreException, PermissionException; 

   /**
    * Gets a list of graph IDs in the given corpus.
    * @param corpus A corpus ID.
    * @return A list of graph IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getGraphIdsInCorpus(String corpus) throws StoreException, PermissionException; 

   /**
    * Gets a list of IDs of graphs that include the given participant.
    * @param participant A participant ID.
    * @return A list of graph IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getGraphIdsWithParticipant(String participant) throws StoreException, PermissionException; 
   
   /**
    * Gets a graph given its ID.
    * @param id The given graph ID.
    * @return The identified graph.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    * @throws GraphNotFoundException If the graph was not found in the store.
    */
   public Graph getGraph(String id) throws StoreException, PermissionException, GraphNotFoundException;

   /**
    * Gets a graph given its ID, containing only the given layers.
    * @param id The given graph ID.
    * @param layerIds The IDs of the layers to load, or null if only graph data is required.
    * @return The identified graph.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    * @throws GraphNotFoundException If the graph was not found in the store.
    */
   public Graph getGraph(String id, String[] layerIds) throws StoreException, PermissionException, GraphNotFoundException;

   

} // end of interface IGraphStoreQuery
