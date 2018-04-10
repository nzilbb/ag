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
   public String getId()
      throws StoreException, PermissionException;
   
   /**
    * Gets a list of layer IDs (annotation 'types').
    * @return A list of layer IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getLayerIds()
      throws StoreException, PermissionException; 
   
   /**
    * Gets a list of layer definitions.
    * @return A list of layer definitions.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public Layer[] getLayers()
      throws StoreException, PermissionException;

   /**
    * Gets the layer schema.
    * @return A schema defining the layers and how they relate to each other.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public Schema getSchema()
      throws StoreException, PermissionException;

   /**
    * Gets a layer definition.
    * @param id ID of the layer to get the definition for.
    * @return The definition of the given layer.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public Layer getLayer(String id)
      throws StoreException, PermissionException;   

   /**
    * Gets a list of corpus IDs.
    * @return A list of corpus IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getCorpusIds()
      throws StoreException, PermissionException; 
   
   /**
    * Gets a list of participant IDs.
    * @return A list of participant IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getParticipantIds()
      throws StoreException, PermissionException; 

   /**
    * Gets the participant record specified by the given identifier.
    * @param id The ID of the participant, which could be their name or their database annotation ID.
    * @return An annotation representing the participant, or null if the participant was not found.
    * @throws StoreException
    * @throws PermissionException
    */
   public Annotation getParticipant(String id)
      throws StoreException, PermissionException;

   /**
    * Counts the number of participants that match a particular pattern.
    * @param expression An expression that determines which participants match.
    * @return The number of matching participants.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public int countMatchingParticipantIds(String expression)
      throws StoreException, PermissionException; 
   
   /**
    * Gets a list of IDs of participants that match a particular pattern.
    * @param expression An expression that determines which participants match.
    * @return A list of participant IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getMatchingParticipantIds(String expression)
      throws StoreException, PermissionException; 

   /**
    * Gets a list of IDs of participants that match a particular pattern.
    * @param expression An expression that determines which participants match.
    * @param pageLength The maximum number of IDs to return, or null to return all.
    * @param pageNumber The page number to return, or null to return the first page.
    * @return A list of participant IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getMatchingParticipantIdsPage(String expression, Integer pageLength, Integer pageNumber)
      throws StoreException, PermissionException; 

   /**
    * Gets a list of graph IDs.
    * @return A list of graph IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getGraphIds()
      throws StoreException, PermissionException; 
   
   /**
    * Gets a list of graph IDs in the given corpus.
    * @param id A corpus ID.
    * @return A list of graph IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getGraphIdsInCorpus(String id)
      throws StoreException, PermissionException; 

   /**
    * Gets a list of IDs of graphs that include the given participant.
    * @param id A participant ID.
    * @return A list of graph IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getGraphIdsWithParticipant(String id)
      throws StoreException, PermissionException; 

   /**
    * Counts the number of graphs that match a particular pattern.
    * @param expression An expression that determines which graphs match.
    * @return The number of matching graphs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public int countMatchingGraphIds(String expression)
      throws StoreException, PermissionException; 
   
   /**
    * Gets a list of IDs of graphs that match a particular pattern.
    * @param expression An expression that determines which graphs match.
    * @return A list of graph IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getMatchingGraphIds(String expression)
      throws StoreException, PermissionException; 

   /**
    * Gets a list of IDs of graphs that match a particular pattern.
    * @param expression An expression that determines which graphs match.
    * @param pageLength The maximum number of IDs to return, or null to return all.
    * @param pageNumber The page number to return, or null to return the first page.
    * @return A list of graph IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getMatchingGraphIdsPage(String expression, Integer pageLength, Integer pageNumber)
      throws StoreException, PermissionException; 

   /**
    * Gets a list of IDs of graphs that match a particular pattern.
    * @param expression An expression that determines which graphs match.
    * @param pageLength The maximum number of IDs to return, or null to return all.
    * @param pageNumber The page number to return, or null to return the first page.
    * @param order The ordering for the list of IDs, a string containing a comma-separated list of epxressions, which may be appended by " ASC" or " DESC", or null for graph ID order.
    * @return A list of graph IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getMatchingGraphIdsPage(String expression, Integer pageLength, Integer pageNumber, String order)
      throws StoreException, PermissionException; 

   /**
    * Gets a graph given its ID.
    * @param id The given graph ID.
    * @return The identified graph.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    * @throws GraphNotFoundException If the graph was not found in the store.
    */
   public Graph getGraph(String id) 
      throws StoreException, PermissionException, GraphNotFoundException;

   /**
    * Gets a graph given its ID, containing only the given layers.
    * @param id The given graph ID.
    * @param layerId The IDs of the layers to load, or null if only graph data is required.
    * @return The identified graph.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    * @throws GraphNotFoundException If the graph was not found in the store.
    */
   public Graph getGraph(String id, String[] layerId) 
      throws StoreException, PermissionException, GraphNotFoundException;

   /**
    * Gets a fragment of a graph, given its ID and the ID of an annotation in it that defines the 
    * desired fragment.
    * @param graphId The ID of the graph.
    * @param annotationId The ID of an annotation that defines the bounds of the fragment.
    * @return The identified graph fragment.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    * @throws GraphNotFoundException If the graph was not found in the store.
    */
   public Graph getFragment(String graphId, String annotationId) 
      throws StoreException, PermissionException, GraphNotFoundException;

   /**
    * Gets a fragment of a graph, given its ID and the ID of an annotation in it that defines the 
    * desired fragment, and containing only the given layers.
    * @param graphId The ID of the graph.
    * @param annotationId The ID of an annotation that defines the bounds of the fragment.
    * @param layerId The IDs of the layers to load, or null if only graph data is required.
    * @return The identified graph fragment.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    * @throws GraphNotFoundException If the graph was not found in the store.
    */
   public Graph getFragment(String graphId, String annotationId, String[] layerId) 
      throws StoreException, PermissionException, GraphNotFoundException;
   
   /**
    * Gets the number of annotations on the given layer of the given graph.
    * @param id The ID of the graph.
    * @param layerId The ID of the layer.
    * @return A (possibly empty) array of annotations.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    * @throws GraphNotFoundException If the graph was not found in the store.
    */
   public long countAnnotations(String id, String layerId)
      throws StoreException, PermissionException, GraphNotFoundException;

   /**
    * Gets the annotations on the given layer of the given graph.
    * @param id The ID of the graph.
    * @param layerId The ID of the layer.
    * @return A (possibly empty) array of annotations.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    * @throws GraphNotFoundException If the graph was not found in the store.
    */
   public Annotation[] getAnnotations(String id, String layerId)
      throws StoreException, PermissionException, GraphNotFoundException;

   /**
    * Gets the annotations on the given layer of the given graph.
    * @param id The ID of the graph.
    * @param layerId The ID of the layer.
    * @param pageLength The maximum number of IDs to return, or null to return all.
    * @param pageNumber The page number to return, or null to return the first page.
    * @return A (possibly empty) array of annotations.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    * @throws GraphNotFoundException If the graph was not found in the store.
    */
   public Annotation[] getAnnotations(String id, String layerId, Integer pageLength, Integer pageNumber)
      throws StoreException, PermissionException, GraphNotFoundException;

   /**
    * Gets the given anchors in the given graph.
    * @param id The ID of the graph.
    * @param anchorIds An array of anchor IDs.
    * @return A (possibly empty) array of anchors.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    * @throws GraphNotFoundException If the graph was not found in the store.
    */
   public Anchor[] getAnnotations(String id, String[] anchorIds)
      throws StoreException, PermissionException, GraphNotFoundException;

   /**
    * List the predefined media tracks available for transcripts.
    * @return An ordered list of media track definitions.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted. 
    */
   public MediaTrackDefinition[] getMediaTracks() 
      throws StoreException, PermissionException;
   
   /**
    * List the media available for the given graph.
    * @param id The graph ID.
    * @return List of media files available for the given graph.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    * @throws GraphNotFoundException If the graph was not found in the store.
    */
   public MediaFile[] getAvailableMedia(String id) 
      throws StoreException, PermissionException, GraphNotFoundException;

   /**
    * Gets a given media track for a given graph.
    * @param id The graph ID.
    * @param trackSuffix The track suffix of the media - see {@link MediaTrackDefinition#suffix}.
    * @param mimeType The MIME type of the media.
    * @return A URL to the given media for the given graph, or null if the given media doesn't exist.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    * @throws GraphNotFoundException If the graph was not found in the store.
    */
   public String getMedia(String id, String trackSuffix, String mimeType) 
      throws StoreException, PermissionException, GraphNotFoundException;
   
} // end of interface IGraphStoreQuery
