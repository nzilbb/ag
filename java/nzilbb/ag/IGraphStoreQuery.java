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
package nzilbb.ag;

import java.util.Iterator;
import java.util.Vector;
import java.util.function.Consumer;
import nzilbb.util.MonitorableSeries;

/**
 * Interface for querying an annotation graph store, a database of graphs.
 * <p>In order to easily support access via scripting in other languages, methods that return
 * lists use arrays rather than collection classes. 
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
    * @param id The ID of the participant, which could be their name or their database annotation
    * ID. 
    * @return An annotation representing the participant, or null if the participant was not found.
    * @throws StoreException
    * @throws PermissionException
    */
   public Annotation getParticipant(String id)
      throws StoreException, PermissionException;

   /**
    * Counts the number of participants that match a particular pattern.
    * @param expression An expression that determines which participants match.
    * <p> The expression language is currently not well defined, but expressions such as the
    * following can be used: 
    * <ul>
    *  <li><code>id MATCHES 'Ada.+'</code></li>
    *  <li><code>'CC' IN labels('corpus')</code></li>
    *  <li><code>'en' IN labels('participant_languages')</code></li>
    *  <li><code>'en' IN labels('transcript_language')</code></li>
    *  <li><code>id NOT MATCHES 'Ada.+' AND my('corpus').label = 'CC'</code></li>
    *  <li><code>list('transcript_rating').length &gt; 2</code></li>
    *  <li><code>list('participant_rating').length = 0</code></li>
    *  <li><code>'labbcat' NOT IN annotators('transcript_rating')</code></li>
    *  <li><code>my('participant_gender').label = 'NA'</code></li>
    * </ul>
    * @return The number of matching participants.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public int countMatchingParticipantIds(String expression)
      throws StoreException, PermissionException; 
   
   /**
    * Gets a list of IDs of participants that match a particular pattern.
    * @param expression An expression that determines which participants match.
    * <p> The expression language is currently not well defined, but expressions such as the
    * following can be used: 
    * <ul>
    *  <li><code>id MATCHES 'Ada.+'</code></li>
    *  <li><code>'CC' IN labels('corpus')</code></li>
    *  <li><code>'en' IN labels('participant_languages')</code></li>
    *  <li><code>'en' IN labels('transcript_language')</code></li>
    *  <li><code>id NOT MATCHES 'Ada.+' AND my('corpus').label = 'CC'</code></li>
    *  <li><code>list('transcript_rating').length &gt; 2</code></li>
    *  <li><code>list('participant_rating').length = 0</code></li>
    *  <li><code>'labbcat' NOT IN annotators('transcript_rating')</code></li>
    *  <li><code>my('participant_gender').label = 'NA'</code></li>
    * </ul>
    * @return A list of participant IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getMatchingParticipantIds(String expression)
      throws StoreException, PermissionException; 

   /**
    * Gets a list of IDs of participants that match a particular pattern.
    * @param expression An expression that determines which participants match.
    * <p> The expression language is currently not well defined, but expressions such as the
    * following can be used: 
    * <ul>
    *  <li><code>id MATCHES 'Ada.+'</code></li>
    *  <li><code>'CC' IN labels('corpus')</code></li>
    *  <li><code>'en' IN labels('participant_languages')</code></li>
    *  <li><code>'en' IN labels('transcript_language')</code></li>
    *  <li><code>id NOT MATCHES 'Ada.+' AND my('corpus').label = 'CC'</code></li>
    *  <li><code>list('transcript_rating').length &gt; 2</code></li>
    *  <li><code>list('participant_rating').length = 0</code></li>
    *  <li><code>'labbcat' NOT IN annotators('transcript_rating')</code></li>
    *  <li><code>my('participant_gender').label = 'NA'</code></li>
    * </ul>
    * @param pageLength The maximum number of IDs to return, or null to return all.
    * @param pageNumber The zero-based page number to return, or null to return the first page.
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
    * <p> The expression language is currently not well defined, but expressions such as the following can be used:
    * <ul>
    *  <li><code>id MATCHES 'Ada.+'</code></li>
    *  <li><code>'Robert' IN labels('who')</code></li>
    *  <li><code>my('corpus').label IN ('CC', 'IA', 'MU')</code></li>
    *  <li><code>my('episode').label = 'Ada Aitcheson'</code></li>
    *  <li><code>my('transcript_scribe').label = 'Robert'</code></li>
    *  <li><code>my('participant_languages').label = 'en'</code></li>
    *  <li><code>my('noise').label = 'bell'</code></li>
    *  <li><code>'en' IN labels('transcript_languages')</code></li>
    *  <li><code>'en' IN labels('participant_languages')</code></li>
    *  <li><code>'bell' IN labels('noise')</code></li>
    *  <li><code>list('transcript_languages').length gt; 1</code></li>
    *  <li><code>list('participant_languages').length gt; 1</code></li>
    *  <li><code>list('transcript').length gt; 100</code></li>
    *  <li><code>'Robert' IN annotators('transcript_rating')</code></li>
    *  <li><code>id NOT MATCHES 'Ada.+' AND my('corpus').label = 'CC' AND 'Robert' IN labels('who')</code></li>
    * </ul>
    * @return The number of matching graphs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public int countMatchingGraphIds(String expression)
      throws StoreException, PermissionException; 
   
   /**
    * Gets a list of IDs of graphs that match a particular pattern.
    * @param expression An expression that determines which graphs match.
    * <p> The expression language is currently not well defined, but expressions such as the following can be used:
    * <ul>
    *  <li><code>id MATCHES 'Ada.+'</code></li>
    *  <li><code>'Robert' IN labels('who')</code></li>
    *  <li><code>my('corpus').label IN ('CC', 'IA', 'MU')</code></li>
    *  <li><code>my('episode').label = 'Ada Aitcheson'</code></li>
    *  <li><code>my('transcript_scribe').label = 'Robert'</code></li>
    *  <li><code>my('participant_languages').label = 'en'</code></li>
    *  <li><code>my('noise').label = 'bell'</code></li>
    *  <li><code>'en' IN labels('transcript_languages')</code></li>
    *  <li><code>'en' IN labels('participant_languages')</code></li>
    *  <li><code>'bell' IN labels('noise')</code></li>
    *  <li><code>list('transcript_languages').length gt; 1</code></li>
    *  <li><code>list('participant_languages').length gt; 1</code></li>
    *  <li><code>list('transcript').length gt; 100</code></li>
    *  <li><code>'Robert' IN annotators('transcript_rating')</code></li>
    *  <li><code>id NOT MATCHES 'Ada.+' AND my('corpus').label = 'CC' AND 'Robert' IN labels('who')</code></li>
    * </ul>
    * @return A list of graph IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getMatchingGraphIds(String expression)
      throws StoreException, PermissionException; 

   /**
    * Gets a list of IDs of graphs that match a particular pattern.
    * @param expression An expression that determines which graphs match.
    * <p> The expression language is currently not well defined, but expressions such as the following can be used:
    * <ul>
    *  <li><code>id MATCHES 'Ada.+'</code></li>
    *  <li><code>'Robert' IN labels('who')</code></li>
    *  <li><code>my('corpus').label IN ('CC', 'IA', 'MU')</code></li>
    *  <li><code>my('episode').label = 'Ada Aitcheson'</code></li>
    *  <li><code>my('transcript_scribe').label = 'Robert'</code></li>
    *  <li><code>my('participant_languages').label = 'en'</code></li>
    *  <li><code>my('noise').label = 'bell'</code></li>
    *  <li><code>'en' IN labels('transcript_languages')</code></li>
    *  <li><code>'en' IN labels('participant_languages')</code></li>
    *  <li><code>'bell' IN labels('noise')</code></li>
    *  <li><code>list('transcript_languages').length gt; 1</code></li>
    *  <li><code>list('participant_languages').length gt; 1</code></li>
    *  <li><code>list('transcript').length gt; 100</code></li>
    *  <li><code>'Robert' IN annotators('transcript_rating')</code></li>
    *  <li><code>id NOT MATCHES 'Ada.+' AND my('corpus').label = 'CC' AND 'Robert' IN labels('who')</code></li>
    * </ul>
    * @param pageLength The maximum number of IDs to return, or null to return all.
    * @param pageNumber The zero-based page number to return, or null to return the first page.
    * @return A list of graph IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getMatchingGraphIdsPage(String expression, Integer pageLength, Integer pageNumber)
      throws StoreException, PermissionException; 

   /**
    * <p>Gets a list of IDs of graphs that match a particular pattern.</p>
    * <p>The results can be exhaustive, by omitting pageLength and pageNumber, or they
    * can be a subset (a 'page') of results, by given pageLength and pageNumber values.</p>
    * <p>The order of the list can be specified.  If ommitted, the graphs are listed in ID
    * order.</p> 
    * @param expression An expression that determines which graphs match.
    * <p> The expression language is currently not well defined, but expressions such as the following can be used:
    * <ul>
    *  <li><code>id MATCHES 'Ada.+'</code></li>
    *  <li><code>'Robert' IN labels('who')</code></li>
    *  <li><code>my('corpus').label IN ('CC', 'IA', 'MU')</code></li>
    *  <li><code>my('episode').label = 'Ada Aitcheson'</code></li>
    *  <li><code>my('transcript_scribe').label = 'Robert'</code></li>
    *  <li><code>my('participant_languages').label = 'en'</code></li>
    *  <li><code>my('noise').label = 'bell'</code></li>
    *  <li><code>'en' IN labels('transcript_languages')</code></li>
    *  <li><code>'en' IN labels('participant_languages')</code></li>
    *  <li><code>'bell' IN labels('noise')</code></li>
    *  <li><code>list('transcript_languages').length gt; 1</code></li>
    *  <li><code>list('participant_languages').length gt; 1</code></li>
    *  <li><code>list('transcript').length gt; 100</code></li>
    *  <li><code>'Robert' IN annotators('transcript_rating')</code></li>
    *  <li><code>id NOT MATCHES 'Ada.+' AND my('corpus').label = 'CC' AND 'Robert' IN labels('who')</code></li>
    * </ul>
    * @param pageLength The maximum number of IDs to return, or null to return all.
    * @param pageNumber The zero-based page number to return, or null to return the first page.
    * @param order The ordering for the list of IDs, a string containing a comma-separated list of
    * expressions, which may be appended by " ASC" or " DESC", or null for graph ID order. 
    * @return A list of graph IDs.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public String[] getMatchingGraphIdsPage(String expression, Integer pageLength, Integer pageNumber, String order)
      throws StoreException, PermissionException; 

   /**
    * Counts the number of annotations that match a particular pattern.
    * @param expression An expression that determines which participants match.
    * <p> The expression language is currently not well defined, but expressions such as the following can be used:
    * <ul>
    *  <li><code>id = 'ew_0_456'</code></li>
    *  <li><code>label NOT MATCHES 'th[aeiou].*'</code></li>
    *  <li><code>layer.id = 'orthography' AND my('who').label = 'Robert' AND
    * my('utterances').start.offset = 12.345</code></li> 
    *  <li><code>graph.id = 'AdaAicheson-01.trs' AND layer.id = 'orthography' AND start.offset
    * &gt; 10.5</code></li> 
    * </ul>
    * <p><em>NB</em> all expressions must match by either id or layer.id.
    * @return The number of matching annotations.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public int countMatchingAnnotations(String expression)
      throws StoreException, PermissionException; 

   /**
    * Gets a list of annotations that match a particular pattern.
    * @param expression An expression that determines which graphs match.
    * <p> The expression language is currently not well defined, but expressions such as the following can be used:
    * <ul>
    *  <li><code>id = 'ew_0_456'</code></li>
    *  <li><code>label NOT MATCHES 'th[aeiou].*'</code></li>
    *  <li><code>my('who').label = 'Robert' AND my('utterances').start.offset = 12.345</code></li>
    *  <li><code>graph.id = 'AdaAicheson-01.trs' AND layer.id = 'orthography' AND start.offset
    * &gt; 10.5</code></li> 
    *  <li><code>previous.id = 'ew_0_456'</code></li>
    * </ul>
    * <p><em>NB</em> all expressions must match by either id or layer.id.
    * @return A list of matching {@link Annotation}s.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public Annotation[] getMatchingAnnotations(String expression)
      throws StoreException, PermissionException; 

   /**
    * Gets a list of annotations that match a particular pattern.
    * @param expression An expression that determines which graphs match.
    * <p> The expression language is currently not well defined, but expressions such as the
    * following can be used: 
    * <ul>
    *  <li><code>id = 'ew_0_456'</code></li>
    *  <li><code>label NOT MATCHES 'th[aeiou].*'</code></li>
    *  <li><code>my('who').label = 'Robert' AND my('utterances').start.offset = 12.345</code></li>
    *  <li><code>graph.id = 'AdaAicheson-01.trs' AND layer.id = 'orthography' AND start.offset
    * &gt; 10.5</code></li> 
    *  <li><code>previous.id = 'ew_0_456'</code></li>
    * </ul>
    * <p><em>NB</em> all expressions must match by either id or layer.id.
    * @param pageLength The maximum number of annotations to return, or null to return all.
    * @param pageNumber The zero-based page number to return, or null to return the first page.
    * @return A list of matching {@link Annotation}s.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public Annotation[] getMatchingAnnotationsPage(String expression, Integer pageLength, Integer pageNumber)
      throws StoreException, PermissionException; 

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
    * @param pageNumber The zero-based page number to return, or null to return the first page.
    * @return A (possibly empty) array of annotations.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    * @throws GraphNotFoundException If the graph was not found in the store.
    */
   public Annotation[] getAnnotations(String id, String layerId, Integer pageLength, Integer pageNumber)
      throws StoreException, PermissionException, GraphNotFoundException;
   
   /**
    * Gets the annotations on given layers for a set of match IDs.
    * @param matchIds An iterator that supplies match IDs - these may be the contents of
    * the MatchId column in exported search results, token URLs, or annotation IDs. 
    * @param layerIds The layer IDs of the layers to get.
    * @param targetOffset Which token to get the annotations of; 0 means the match target
    * itself, 1 means the token after the target, -1 means the token before the target, etc. 
    * @param annotationsPerLayer The number of annotations per layer to get; if there's a
    * smaller number of annotations available, the unfilled array elements will be null.
    * @param consumer A consumer for handling the resulting
    * annotations. Consumer.accept() will be invoked once for each element returned by the
    * <var>matchIds</var> iterator, with an array of {@link Annotation} objects. The size
    * of this array will be <var>layerIds.length</var> * <var>annotationsPerLayer</var>,
    * and will be filled in with the available annotations for each layer; when
    * annotations are not available, null is supplied.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    */
   public void getMatchAnnotations(Iterator<String> matchIds, String[] layerIds, int targetOffset, int annotationsPerLayer, Consumer<Annotation[]> consumer)
      throws StoreException, PermissionException;
   
   /**
    * Gets the given anchors in the given graph.
    * @param id The ID of the graph.
    * @param anchorIds An array of anchor IDs.
    * @return A (possibly empty) array of anchors.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    * @throws GraphNotFoundException If the graph was not found in the store.
    */
   public Anchor[] getAnchors(String id, String[] anchorIds)
      throws StoreException, PermissionException, GraphNotFoundException;

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
    * @param layerIds The IDs of the layers to load, or null if only graph data is required.
    * @return The identified graph.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    * @throws GraphNotFoundException If the graph was not found in the store.
    */
   public Graph getGraph(String id, String[] layerIds) 
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
    * @param layerIds The IDs of the layers to load, or null if only graph data is required.
    * @return The identified graph fragment.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    * @throws GraphNotFoundException If the graph was not found in the store.
    */
   public Graph getFragment(String graphId, String annotationId, String[] layerIds) 
      throws StoreException, PermissionException, GraphNotFoundException;
   
   /**
    * Gets a fragment of a graph, given its ID and the start/end offsets that define the 
    * desired fragment, and containing only the given layers.
    * @param graphId The ID of the graph.
    * @param start The start offset of the fragment.
    * @param end The end offset of the fragment.
    * @param layerIds The IDs of the layers to load, or null if only graph data is required.
    * @return The identified graph fragment.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    * @throws GraphNotFoundException If the graph was not found in the store.
    */
   public Graph getFragment(String graphId, double start, double end, String[] layerIds) 
      throws StoreException, PermissionException, GraphNotFoundException;
   
   /**
    * Gets a series of fragments, given the series' ID, and only the given layers.
    * @param seriesId The ID of the series.
    * @param layerIds The IDs of the layers to load, or null if only graph data is required.
    * @return An enumeratable series of fragments.
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    * @throws GraphNotFoundException If the graph was not found in the store.
    */
   public MonitorableSeries<Graph> getFragmentSeries(String seriesId, String[] layerIds) 
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
    * @param mimeType The MIME type of the media, which may include parameters for type
    * conversion, e.g. "text/wav; samplerate=16000".
    * @return A URL to the given media for the given graph, or null if the given media doesn't
    * exist. 
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    * @throws GraphNotFoundException If the graph was not found in the store.
    */
   public String getMedia(String id, String trackSuffix, String mimeType) 
      throws StoreException, PermissionException, GraphNotFoundException;

   /**
    * Gets a given media track for a given graph.
    * @param id The graph ID.
    * @param trackSuffix The track suffix of the media - see {@link MediaTrackDefinition#suffix}.
    * @param mimeType The MIME type of the media, which may include parameters for type
    * conversion, e.g. "text/wav; samplerate=16000"
    * @param startOffset The start offset of the media sample, or null for the start of the whole
    * recording. 
    * @param endOffset The end offset of the media sample, or null for the end of the whole
    * recording. 
    * @return A URL to the given media for the given graph, or null if the given media doesn't
    * exist. 
    * @throws StoreException If an error occurs.
    * @throws PermissionException If the operation is not permitted.
    * @throws GraphNotFoundException If the graph was not found in the store.
    */
   public String getMedia(String id, String trackSuffix, String mimeType, Double startOffset, Double endOffset) 
      throws StoreException, PermissionException, GraphNotFoundException;

   /**
    * Get a list of documents associated with the episode of the given graph.
    * @param id The graph ID.
    * @return List of URLs to documents.
    * @throws StoreException If an error prevents the media from being saved.
    * @throws PermissionException If saving the media is not permitted.
    * @throws GraphNotFoundException If the graph doesn't exist.
    */
   public MediaFile[] getEpisodeDocuments(String id)
      throws StoreException, PermissionException, GraphNotFoundException;

} // end of interface IGraphStoreQuery
