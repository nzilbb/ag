//
// Copyright 2022-2025 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.annotator.orthography;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import nzilbb.ag.*;
import nzilbb.ag.automation.util.AnnotatorDescriptor;
import nzilbb.ag.serialize.GraphDeserializer;
import nzilbb.ag.serialize.GraphSerializer;
import nzilbb.ag.serialize.SerializationDescriptor;
import nzilbb.util.IO;
import nzilbb.util.MonitorableSeries;

/**
 * Bare implementation of GraphStore for unit tests.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class GraphStoreHarness implements GraphStore {
  String aggregateMatchingAnnotationsOperation = null;
  String aggregateMatchingAnnotationsExpression = null;

  /** This should be called to get distinct words. */
  public String[] aggregateMatchingAnnotations(String operation, String expression)
    throws StoreException, PermissionException {
    aggregateMatchingAnnotationsOperation = operation;
    aggregateMatchingAnnotationsExpression = expression;
    return new String[]{"Foo\\"," 'bar'"};
  }


  LinkedHashMap<String,String> tagMatchingAnnotationsLayerIds = new LinkedHashMap<String,String>();
  LinkedHashMap<String,String> tagMatchingAnnotationsLabels = new LinkedHashMap<String,String>();
  LinkedHashMap<String,Integer> tagMatchingAnnotationsConfidences = new LinkedHashMap<String,Integer>();
  /** This should be called once for each distinct word, to set the orthography. */
  public int tagMatchingAnnotations(
    String expression, String layerId, String label, Integer confidence)
    throws StoreException, PermissionException {
    tagMatchingAnnotationsLayerIds.put(expression, layerId);
    tagMatchingAnnotationsLabels.put(expression, label);
    tagMatchingAnnotationsConfidences.put(expression, confidence);
    return 1;
  }  

  /**
   * Constructor.
   */
  public GraphStoreHarness() {
  } // end of constructor

  /**
   * Saves the given media for the given transcript
   * @param id The transcript ID
   * @param trackSuffix The track suffix of the media - see {@link MediaTrackDefinition#suffix}.
   * @param mediaUrl A URL to the media content.
   * @throws StoreException If an error prevents the media from being saved.
   * @throws PermissionException If saving the media is not permitted.
   * @throws GraphNotFoundException If the transcript doesn't exist.
   */
  public MediaFile saveMedia(String id, String trackSuffix, String mediaUrl)
    throws StoreException, PermissionException, GraphNotFoundException {
    throw new StoreException("saveMedia not supported");
  }

  /**
   * List the media available for the given transcript.
   * @param id The transcript ID.
   * @return List of media files available for the given transcript.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the transcript was not found in the store.
   */
  public MediaFile[] getAvailableMedia(String id) 
    throws StoreException, PermissionException, GraphNotFoundException {
    throw new StoreException("getAvailableMedia not supported");
  }

  /**
   * Gets the layer schema.
   * @return A schema defining the layers and how they relate to each other.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public Schema getSchema()
    throws StoreException, PermissionException {
    return new Schema(
      "participant", "turn", "utterance", "word",
      new Layer("transcript_language", "Overall Language")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("participant", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("participant").setParentIncludes(true),
      new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("orthography", "Orthography").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true),
      new Layer("segment", "Phones").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true)
      .setType(Constants.TYPE_IPA));
  }

  /**
   * Gets the store's ID.
   * @return The annotation store's ID.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String getId() throws StoreException, PermissionException {
    return getClass().getName();
  }
   
  /**
   * Gets a list of layer IDs (annotation 'types').
   * @return A list of layer IDs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String[] getLayerIds()
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  }
   
  /**
   * Gets a list of layer definitions.
   * @return A list of layer definitions.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public Layer[] getLayers()
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  }

  /**
   * Gets a layer definition.
   * @param id ID of the layer to get the definition for.
   * @return The definition of the given layer.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public Layer getLayer(String id)
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  }   

  /**
   * Gets a list of corpus IDs.
   * @return A list of corpus IDs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String[] getCorpusIds()
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  } 
   
  /**
   * Gets a list of participant IDs.
   * @return A list of participant IDs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String[] getParticipantIds()
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  } 

  /**
   * Gets the participant record specified by the given identifier.
   * @param id The ID of the participant, which could be their name or their database annotation
   * ID. 
   * @param layerIds The IDs of the participant attribute layers to load, or null if only
   * participant data is required. 
   * @return An annotation representing the participant, or null if the participant was
   * not found.
   * @throws StoreException
   * @throws PermissionException
   */
  public Annotation getParticipant(String id, String[] layerIds)
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  }

  /**
   * Counts the number of participants that match a particular pattern.
   * @param expression An expression that determines which participants match.
   * <p> The expression language is loosely based on JavaScript; expressions such as the
   * following can be used: 
   * <ul>
   *  <li><code>/Ada.+/.test(id)</code></li>
   *  <li><code>labels('corpus').includes('CC')</code></li>
   *  <li><code>labels('participant_languages').includes('en')</code></li>
   *  <li><code>labels('transcript_language').includes('en')</code></li>
   *  <li><code>!/Ada.+/.test(id) &amp;&amp; first('corpus').label == 'CC'</code></li>
   *  <li><code>all('transcript_rating').length &gt; 2</code></li>
   *  <li><code>all('participant_rating').length = 0</code></li>
   *  <li><code>!annotators('transcript_rating').includes('labbcat')</code></li>
   *  <li><code>first('participant_gender').label == 'NA'</code></li>
   * </ul>
   * @return The number of matching participants.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public int countMatchingParticipantIds(String expression)
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  } 
   
  /**
   * Gets a list of IDs of participants that match a particular pattern.
   * @param expression An expression that determines which participants match.
   * <p> The expression language is loosely based on JavaScript; expressions such as the
   * following can be used: 
   * <ul>
   *  <li><code>/Ada.+/.test(id)</code></li>
   *  <li><code>labels('corpus').includes('CC')</code></li>
   *  <li><code>labels('participant_languages').includes('en')</code></li>
   *  <li><code>labels('transcript_language').includes('en')</code></li>
   *  <li><code>!/Ada.+/.test(id) &amp;&amp; first('corpus').label == 'CC'</code></li>
   *  <li><code>all('transcript_rating').length &gt; 2</code></li>
   *  <li><code>all('participant_rating').length = 0</code></li>
   *  <li><code>!annotators('transcript_rating').includes('labbcat')</code></li>
   *  <li><code>first('participant_gender').label == 'NA'</code></li>
   * </ul>
   * @param pageLength The maximum number of IDs to return, or null to return all.
   * @param pageNumber The zero-based page number to return, or null to return the first page.
   * @return A list of participant IDs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String[] getMatchingParticipantIds(
    String expression, Integer pageLength, Integer pageNumber)
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  } 

  /**
   * Gets a list of all trancript IDs.
   * @return A list of transcript IDs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String[] getTranscriptIds()
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  } 
   
  /**
   * Gets a list of transcript IDs in the given corpus.
   * @param id A corpus ID.
   * @return A list of transcript IDs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String[] getTranscriptIdsInCorpus(String id)
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  } 

  /**
   * Gets a list of IDs of transcripts that include the given participant.
   * @param id A participant ID.
   * @return A list of transcript IDs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String[] getTranscriptIdsWithParticipant(String id)
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  } 

  /**
   * Counts the number of transcript IDs that match a particular pattern.
   * @param expression An expression that determines which transcripts match.
   * <p> The expression language is loosely based on JavaScript; expressions such as the
   * following can be used: 
   * <ul>
   *  <li><code>/Ada.+/.test(id)</code></li>
   *  <li><code>labels('who').includes('Robert')</code></li>
   *  <li><code>('CC', 'IA', 'MU').includes(first('corpus').label)</code></li>
   *  <li><code>first('episode').label == 'Ada Aitcheson'</code></li>
   *  <li><code>first('transcript_scribe').label == 'Robert'</code></li>
   *  <li><code>first('participant_languages').label == 'en'</code></li>
   *  <li><code>first('noise').label == 'bell'</code></li>
   *  <li><code>labels('transcript_languages').includes('en')</code></li>
   *  <li><code>labels('participant_languages').includes('en')</code></li>
   *  <li><code>labels('noise').includes('bell')</code></li>
   *  <li><code>all('transcript_languages').length gt; 1</code></li>
   *  <li><code>all('participant_languages').length gt; 1</code></li>
   *  <li><code>all('transcript').length gt; 100</code></li>
   *  <li><code>annotators('transcript_rating').includes('Robert')</code></li>
   *  <li><code>!/Ada.+/.test(id) &amp;&amp; first('corpus').label == 'CC' &amp;&amp;
   * labels('who').includes('Robert')</code></li> 
   * </ul>
   * @return The number of matching transcript IDs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public int countMatchingTranscriptIds(String expression)
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  } 

  /**
   * <p>Gets a list of IDs of transcript that match a particular pattern.</p>
   * <p>The results can be exhaustive, by omitting pageLength and pageNumber, or they
   * can be a subset (a 'page') of results, by given pageLength and pageNumber values.</p>
   * <p>The order of the list can be specified.  If ommitted, the transcripts are listed in ID
   * order.</p> 
   * @param expression An expression that determines which transcripts match.
   * <p> The expression language is loosely based on JavaScript; expressions such as the
   * following can be used: 
   * <ul>
   *  <li><code>/Ada.+/.test(id)</code></li>
   *  <li><code>labels('who').includes('Robert')</code></li>
   *  <li><code>('CC', 'IA', 'MU').includes(first('corpus').label)</code></li>
   *  <li><code>first('episode').label == 'Ada Aitcheson'</code></li>
   *  <li><code>first('transcript_scribe').label == 'Robert'</code></li>
   *  <li><code>first('participant_languages').label == 'en'</code></li>
   *  <li><code>first('noise').label == 'bell'</code></li>
   *  <li><code>labels('transcript_languages').includes('en')</code></li>
   *  <li><code>labels('participant_languages').includes('en')</code></li>
   *  <li><code>labels('noise').includes('bell')</code></li>
   *  <li><code>all('transcript_languages').length gt; 1</code></li>
   *  <li><code>all('participant_languages').length gt; 1</code></li>
   *  <li><code>all('transcript').length gt; 100</code></li>
   *  <li><code>annotators('transcript_rating').includes('Robert')</code></li>
   *  <li><code>!/Ada.+/.test(id) &amp;&amp; first('corpus').label == 'CC' &amp;&amp;
   * labels('who').includes('Robert')</code></li> 
   * </ul>
   * @param pageLength The maximum number of IDs to return, or null to return all.
   * @param pageNumber The zero-based page number to return, or null to return the first page.
   * @param order The ordering for the list of IDs, a string containing a comma-separated list of
   * expressions, which may be appended by " ASC" or " DESC", or null for transcript ID order. 
   * @return A list of transcript IDs.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String[] getMatchingTranscriptIds(
    String expression, Integer pageLength, Integer pageNumber, String order)
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  } 

  /**
   * Counts the number of annotations that match a particular pattern.
   * @param expression An expression that determines which participants match.
   * <p> The expression language is loosely based on JavaScript; expressions such as the
   * following can be used: 
   * <ul>
   *  <li><code>id == 'ew_0_456'</code></li>
   *  <li><code>!/th[aeiou].&#47;/.test(label)</code></li>
   *  <li><code>first('who').label == 'Robert' &amp;&amp; first('utterances').start.offset ==
   * 12.345</code></li> 
   *  <li><code>graph.id == 'AdaAicheson-01.trs' &amp;&amp; layer.id == 'orthography'
   * &amp;&amp; start.offset &gt; 10.5</code></li> 
   *  <li><code>previous.id == 'ew_0_456'</code></li>
   * </ul>
   * <p><em>NB</em> all expressions must match by either id or layer.id.
   * @return The number of matching annotations.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public int countMatchingAnnotations(String expression)
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  } 

  /**
   * Gets a list of annotations that match a particular pattern.
   * @param expression An expression that determines which annotations match.
   * <p> The expression language is loosely based on JavaScript; expressions such as the
   * following can be used: 
   * <ul>
   *  <li><code>id == 'ew_0_456'</code></li>
   *  <li><code>!/th[aeiou].&#47;/.test(label)</code></li>
   *  <li><code>first('who').label == 'Robert' &amp;&amp; first('utterances').start.offset ==
   * 12.345</code></li> 
   *  <li><code>graph.id == 'AdaAicheson-01.trs' &amp;&amp; layer.id == 'orthography'
   * &amp;&amp; start.offset 
   * &gt; 10.5</code></li> 
   *  <li><code>previous.id == 'ew_0_456'</code></li>
   * </ul>
   * <p><em>NB</em> all expressions must match by either id or layer.id.
   * @param pageLength The maximum number of annotations to return, or null to return all.
   * @param pageNumber The zero-based page number to return, or null to return the first page.
   * @return A list of matching {@link Annotation}s.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public Annotation[] getMatchingAnnotations(
    String expression, Integer pageLength, Integer pageNumber)
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  } 

  /**
   * Gets the number of annotations on the given layer of the given transcript.
   * @param id The ID of the transcript.
   * @param layerId The ID of the layer.
   * @return A (possibly empty) array of annotations.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the transcript was not found in the store.
   */
  public long countAnnotations(String id, String layerId, Integer maxOrdinal)
    throws StoreException, PermissionException, GraphNotFoundException {
    throw new StoreException("Not implemented");
  }
 
  /**
   * Gets the annotations on the given layer of the given transcript.
   * @param id The ID of the transcript.
   * @param layerId The ID of the layer.
   * @param pageLength The maximum number of IDs to return, or null to return all.
   * @param pageNumber The zero-based page number to return, or null to return the first page.
   * @return A (possibly empty) array of annotations.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the transcript was not found in the store.
   */
  public Annotation[] getAnnotations(
    String id, String layerId, Integer maxOrdinal, Integer pageLength, Integer pageNumber)
    throws StoreException, PermissionException, GraphNotFoundException {
    throw new StoreException("Not implemented");
  }
   
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
  public void getMatchAnnotations(
    Iterator<String> matchIds, String[] layerIds, int targetOffset, int annotationsPerLayer,
    Consumer<Annotation[]> consumer)
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  }
   
  /**
   * Gets the given anchors in the given transcript.
   * @param id The ID of the transcript.
   * @param anchorIds A list of anchor IDs.
   * @return A (possibly empty) array of anchors.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the transcript was not found in the store.
   */
  public Anchor[] getAnchors(String id, String[] anchorIds)
    throws StoreException, PermissionException, GraphNotFoundException {
    throw new StoreException("Not implemented");
  }
   
  /**
   * Gets a transcript given its ID.
   * @param id The given transcript ID.
   * @return The identified transcript.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the transcript was not found in the store.
   */
  public Graph getTranscript(String id) 
    throws StoreException, PermissionException, GraphNotFoundException {
    throw new StoreException("Not implemented");
  }

  /**
   * Gets a transcript given its ID, containing only the given layers.
   * @param id The given transcript ID.
   * @param layerIds The IDs of the layers to load, or null if only transcript data is required.
   * @return The identified transcript.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the transcript was not found in the store.
   */
  public Graph getTranscript(String id, String[] layerIds) 
    throws StoreException, PermissionException, GraphNotFoundException {
    throw new StoreException("Not implemented");
  }

  /**
   * Gets a fragment of a transcript, given its ID and the ID of an annotation in it that
   * defines the  desired fragment.
   * @param transcriptId The ID of the transcript.
   * @param annotationId The ID of an annotation that defines the bounds of the fragment.
   * @return The identified transcript fragment.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the transcript was not found in the store.
   */
  public Graph getFragment(String transcriptId, String annotationId) 
    throws StoreException, PermissionException, GraphNotFoundException {
    throw new StoreException("Not implemented");
  }

  /**
   * Gets a fragment of a transcript, given its ID and the ID of an annotation in it that
   * defines the desired fragment, and containing only the given layers.
   * @param transcriptId The ID of the transcript.
   * @param annotationId The ID of an annotation that defines the bounds of the fragment.
   * @param layerIds The IDs of the layers to load, or null if only transcript data is required.
   * @return The identified transcript fragment.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the transcript was not found in the store.
   */
  public Graph getFragment(String transcriptId, String annotationId, String[] layerIds) 
    throws StoreException, PermissionException, GraphNotFoundException {
    throw new StoreException("Not implemented");
  }
   
  /**
   * Gets a fragment of a transcript, given its ID and the start/end offsets that define the 
   * desired fragment, and containing only the given layers.
   * @param transcriptId The ID of the transcript.
   * @param start The start offset of the fragment.
   * @param end The end offset of the fragment.
   * @param layerIds The IDs of the layers to load, or null if only transcript data is required.
   * @return The identified transcript fragment.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the transcript was not found in the store.
   */
  public Graph getFragment(String transcriptId, double start, double end, String[] layerIds) 
    throws StoreException, PermissionException, GraphNotFoundException {
    throw new StoreException("Not implemented");
  }
   
  /**
   * Gets a series of fragments, given the series' ID, and only the given layers.
   * @param seriesId The ID of the series.
   * @param layerIds The IDs of the layers to load, or null if only transcript data is required.
   * @return An enumeratable series of fragments.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the transcript was not found in the store.
   */
  public MonitorableSeries<Graph> getFragmentSeries(String seriesId, String[] layerIds) 
    throws StoreException, PermissionException, GraphNotFoundException {
    throw new StoreException("Not implemented");
  }
   
  /**
   * List the predefined media tracks available for transcripts.
   * @return An ordered list of media track definitions.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted. 
   */
  public MediaTrackDefinition[] getMediaTracks() 
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  }
   
  /**
   * Gets a given media track for a given transcript.
   * @param id The transcript ID.
   * @param trackSuffix The track suffix of the media - see {@link MediaTrackDefinition#suffix}.
   * @param mimeType The MIME type of the media, which may include parameters for type
   * conversion, e.g. "text/wav; samplerate=16000".
   * @return A URL to the given media for the given transcript, or null if the given media doesn't
   * exist. 
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the transcript was not found in the store.
   */
  public String getMedia(String id, String trackSuffix, String mimeType) 
    throws StoreException, PermissionException, GraphNotFoundException {
    throw new StoreException("Not implemented");
  }

  /**
   * Gets a given media track for a given transcript.
   * @param id The transcript ID.
   * @param trackSuffix The track suffix of the media - see {@link MediaTrackDefinition#suffix}.
   * @param mimeType The MIME type of the media, which may include parameters for type
   * conversion, e.g. "text/wav; samplerate=16000"
   * @param startOffset The start offset of the media sample, or null for the start of the whole
   * recording. 
   * @param endOffset The end offset of the media sample, or null for the end of the whole
   * recording. 
   * @return A URL to the given media for the given transcript, or null if the given media doesn't
   * exist. 
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   * @throws GraphNotFoundException If the transcript was not found in the store.
   */
  public String getMedia(
    String id, String trackSuffix, String mimeType, Double startOffset, Double endOffset) 
    throws StoreException, PermissionException, GraphNotFoundException {
    throw new StoreException("Not implemented");
  }

  /**
   * Get a list of documents associated with the episode of the given transcript.
   * @param id The transcript ID.
   * @return List of URLs to documents.
   * @throws StoreException If an error prevents the media from being saved.
   * @throws PermissionException If saving the media is not permitted.
   * @throws GraphNotFoundException If the graph doesn't exist.
   */
  public MediaFile[] getEpisodeDocuments(String id)
    throws StoreException, PermissionException, GraphNotFoundException {
    throw new StoreException("Not implemented");
  }

  /**
   * Lists the descriptors of all registered serializers.
   * <p> Serializers are modules that export annotation structures as a specific file
   * format, e.g. Praat TextGrid, plain text, etc., so the
   * {@link SerializationDescriptor#getMimeType()} of descriptors reflects what 
   * <var>mimeType</var>s can be specified for exporting annotation data.
   * @return A list of the descriptors of all registered serializers.
   * @throws StoreException If an error prevents the operation.
   * @throws PermissionException If the operation is not permitted.
   */
  public SerializationDescriptor[] getSerializerDescriptors()
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  }
   
  /**
   * Lists the descriptors of all registered deserializers.
   * <p> Deserializers are modules that import annotation structures from a specific file
   * format, e.g. Praat TextGrid, plain text, etc.
   * @return A list of the descriptors of all registered deserializers.
   * @throws StoreException If an error prevents the operation.
   * @throws PermissionException If the operation is not permitted.
   */
  public SerializationDescriptor[] getDeserializerDescriptors()
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  }
   
  /**
   * Gets the deserializer for the given MIME type.
   * @param mimeType The MIME type.
   * @return The deserializer for the given MIME type, or null if none is registered.
   * @throws StoreException If an error prevents the operation.
   * @throws PermissionException If the operation is not permitted.
   */
  public GraphDeserializer deserializerForMimeType(String mimeType)
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  }

  /**
   * Gets the deserializer for the given file suffix (extension).
   * @param suffix The file extension.
   * @return The deserializer for the given suffix, or null if none is registered.
   * @throws StoreException If an error prevents the operation.
   * @throws PermissionException If the operation is not permitted.
   */
  public GraphDeserializer deserializerForFilesSuffix(String suffix)
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  }

  /**
   * Gets the serializer for the given MIME type.
   * @param mimeType The MIME type.
   * @return The serializer for the given MIME type, or null if none is registered.
   * @throws StoreException If an error prevents the operation.
   * @throws PermissionException If the operation is not permitted.
   */
  public GraphSerializer serializerForMimeType(String mimeType)
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  }

  /**
   * Gets the serializer for the given file suffix (extension).
   * @param suffix The file extension.
   * @return The serializer for the given suffix, or null if none is registered.
   * @throws StoreException If an error prevents the operation.
   * @throws PermissionException If the operation is not permitted.
   */
  public GraphSerializer serializerForFilesSuffix(String suffix)
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  }

  /**
   * Lists descriptors of all transcribers that are installed.
   * @return A list of descriptors of all transcribers that are installed.
   */
  public AnnotatorDescriptor[] getTranscriberDescriptors() {
    return null;
  }

    /**
   * Saves the given transcript. The graph can be partial e.g. include only some of the layers
   * that the stored version of the transcript contains.
   * <p>The graph deltas are assumed to be set correctly, so if this is a new transcript, then
   * {@link Graph#getChange()} should return Change.Operation.Create, if it's an update,
   * Change.Operation.Update, and to delete, Change.Operation.Delete.  Correspondingly,
   * all {@link Anchor}s and {@link Annotation}s should have their changes set also.  If
   * {@link Graph#getChanges()} returns no changes, no action will be taken, and this
   * method returns false.
   * <p>After this method has executed, {@link Graph#commit()} is <em>not</em> called -
   * this must be done by the caller, if they want changes to be committed.
   * @param transcript The transcript to save.
   * @return true if changes were saved, false if there were no changes to save.
   * @throws StoreException If an error prevents the transcript from being saved.
   * @throws PermissionException If saving the transcript is not permitted.
   * @throws GraphNotFoundException If the transcript doesn't exist.
   */
  public boolean saveTranscript(Graph transcript)
    throws StoreException, PermissionException, GraphNotFoundException {
    throw new StoreException("Not implemented");
  }

  /**
   * Creates an annotation starting at <var>from</var> and ending at <var>to</var>.
   * @param id The ID of the transcript.
   * @param fromId The start anchor's ID. TODO an expression identifying the start
   * anchor's ID. e.g. "'n_123'" or "start.id" or maybe something like
   * "first('segments').start.id)"
   * @param toId The end anchor's ID. TODO an expression identifying the end anchor's
   * ID. e.g. "'n_123'" or "end.id" or maybe something like "last('segments').end.id)"
   * @param layerId The layer ID of the resulting annotation.
   * @param label The label of the resulting annotation. 
   * @param confidence The confidence rating.
   * @param parentId The new annotation's parent's ID. 
   * @return The ID of the new annotation.
   */
  public String createAnnotation(
    String id, String fromId, String toId, String layerId, String label, Integer confidence,
    String parentId)
    throws StoreException, PermissionException, GraphNotFoundException {
    throw new StoreException("Not implemented");
  }
  // TODO label can be an expression identifying the parent. e.g. "'em_0_123'" or "layer.id = 'orthography' AND label = 'example'"

  /**
   * Destroys the annotation with the given ID.
   * @param id The ID of the transcript.
   * @param annotationId The annotation's ID.
   */
  public void destroyAnnotation(String id, String annotationId)
    throws StoreException, PermissionException, GraphNotFoundException {
    throw new StoreException("Not implemented");
  }   
   
  /**
   * Saves a participant, and all its tags, to the database.  The participant is
   * represented by an Annotation that isn't assumed to be part of a transcript.
   * @param participant
   * @return true if changes were saved, false if there were no changes to save.
   * @throws StoreException If an error prevents the participant from being saved.
   * @throws PermissionException If saving the participant is not permitted.
   */
  public boolean saveParticipant(Annotation participant)
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  }

  /**
   * Saves the given source file for the given transcript.
   * @param id The transcript ID
   * @param url A URL to the transcript.
   * @throws StoreException If an error prevents the media from being saved.
   * @throws PermissionException If saving the media is not permitted.
   * @throws GraphNotFoundException If the transcript doesn't exist.
   */
  public void saveSource(String id, String url)
    throws StoreException, PermissionException, GraphNotFoundException {
    throw new StoreException("Not implemented");
  }

  /**
   * Saves the given document for the episode of the given transcript.
   * @param id The transcript ID
   * @param url A URL to the document.
   * @throws StoreException If an error prevents the media from being saved.
   * @throws PermissionException If saving the media is not permitted.
   * @throws GraphNotFoundException If the transcript doesn't exist.
   */
  public MediaFile saveEpisodeDocument(String id, String url)
    throws StoreException, PermissionException, GraphNotFoundException {
    throw new StoreException("Not implemented");
  }
   
  /**
   * Deletes the given transcript, and all associated files.
   * @param id The ID transcript to delete.
   * @throws StoreException If an error prevents the transcript from being saved.
   * @throws PermissionException If saving the transcript is not permitted.
   * @throws GraphNotFoundException If the transcript doesn't exist.
   */
  public void deleteTranscript(String id)
    throws StoreException, PermissionException, GraphNotFoundException {
    throw new StoreException("Not implemented");
  }
   
  /**
   * Deletes the given participant, and all associated meta-data.
   * @param id The ID participant to delete.
   * @throws StoreException If an error prevents the transcript from being saved.
   * @throws PermissionException If saving the transcript is not permitted.
   * @throws GraphNotFoundException If the transcript doesn't exist.
   */
  public void deleteParticipant(String id)
    throws StoreException, PermissionException, GraphNotFoundException {
    throw new StoreException("Not implemented");
  }
   
  /**
   * Deletes all annotations that match a particular pattern
   * @param expression An expression that determines which annotations match.
   * <p> The expression language is loosely based on JavaScript; expressions such as the
   * following can be used: 
   * <ul>
   *  <li><code>layer.id == 'pronunciation' 
   *       &amp;&amp; first('orthography').label == 'the'</code></li>
   *  <li><code>first('language').label == 'en' &amp;&amp; layer.id == 'pronunciation' 
   *       &amp;&amp; first('orthography').label == 'the'</code></li> 
   * </ul>
   * <p><em>NB</em> all expressions must match by either id or layer.id.
   * @return The number of new annotations deleted.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public int deleteMatchingAnnotations(String expression)
    throws StoreException, PermissionException {
    throw new StoreException("Not implemented");
  }
  
  /**
   * Delete a given media or document file.
   * @param id The associated transcript ID.
   * @param fileName The media file name, e.g. {@link MediaFile#name}.
   * @throws StoreException, PermissionException, GraphNotFoundException
   */
  public void deleteMedia(String id, String fileName)
    throws StoreException, PermissionException, GraphNotFoundException{
    throw new StoreException("deleteMedia not supported");
  }

  
} // end of class GraphStoreHarness
