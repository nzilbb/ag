//
// Copyright 2015-2022 New Zealand Institute of Language, Brain and Behaviour, 
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

/**
 * Interface for querying and updating an annotation graph store, a database of
 * transcripts represented as Annotation {@link Graph}s.
 * <p>This interface inherits the <em>read-only</em> operations of {@link GraphStoreQuery}
 * and adds some <em>write</em> operations for updating data.
 * <p>In order to easily support access via scripting in other languages, methods that
 * return lists use arrays rather than collection classes.
 * @author Robert Fromont robert@fromont.net.nz
 */
public interface GraphStore extends GraphStoreQuery {
   
  /** Synonym for {@link #saveTranscript(Graph)}. */
  @Deprecated default public boolean saveGraph(Graph transcript)
    throws StoreException, PermissionException, GraphNotFoundException {
    return saveTranscript(transcript);
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
    throws StoreException, PermissionException, GraphNotFoundException;

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
  public String createAnnotation(String id, String fromId, String toId, String layerId, String label, Integer confidence, String parentId)
    throws StoreException, PermissionException, GraphNotFoundException;
  // TODO label can be an expression identifying the parent. e.g. "'em_0_123'" or "layer.id = 'orthography' AND label = 'example'"

  /**
   * Identifies a list of annotations that match a particular pattern, and tags them on
   * the given layer with the given label. All pre-existing tags are deleted.
   * @param expression An expression that determines which annotations match.
   * <p> The expression language is loosely based on JavaScript; expressions such as the
   * following can be used: 
   * <ul>
   *  <li><code>layer.id == 'orthography' &amp;&amp; label == 'word'</code></li>
   *  <li><code>first('language').label == 'en' &amp;&amp; layer.id == 'orthography' &amp;&amp; label == 'word'</code></li> 
   * </ul>
   * <p><em>NB</em> all expressions must match by either id or layer.id.
   * @param layerId The layer ID of the resulting annotation.
   * @param label The label of the resulting annotation.
   * @param confidence The confidence rating.
   * @return The number of new annotations added.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public int tagMatchingAnnotations(String expression, String layerId, String label, Integer confidence)
    throws StoreException, PermissionException;
  
  /**
   * Destroys the annotation with the given ID.
   * @param id The ID of the transcript.
   * @param annotationId The annotation's ID.
   */
  public void destroyAnnotation(String id, String annotationId)
    throws StoreException, PermissionException, GraphNotFoundException;   
   
  /**
   * Saves a participant, and all its tags, to the database.  The participant is
   * represented by an Annotation that isn't assumed to be part of a transcript.
   * @param participant
   * @return true if changes were saved, false if there were no changes to save.
   * @throws StoreException If an error prevents the participant from being saved.
   * @throws PermissionException If saving the participant is not permitted.
   */
  public boolean saveParticipant(Annotation participant)
    throws StoreException, PermissionException;

  /**
   * Saves the given media for the given transcript
   * @param id The transcript ID
   * @param trackSuffix The track suffix of the media - see {@link MediaTrackDefinition#suffix}.
   * @param mediaUrl A URL to the media content.
   * @throws StoreException If an error prevents the media from being saved.
   * @throws PermissionException If saving the media is not permitted.
   * @throws GraphNotFoundException If the transcript doesn't exist.
   */
  public void saveMedia(String id, String trackSuffix, String mediaUrl)
    throws StoreException, PermissionException, GraphNotFoundException;

  /**
   * Saves the given source file for the given transcript.
   * @param id The transcript ID
   * @param url A URL to the transcript.
   * @throws StoreException If an error prevents the media from being saved.
   * @throws PermissionException If saving the media is not permitted.
   * @throws GraphNotFoundException If the transcript doesn't exist.
   */
  public void saveSource(String id, String url)
    throws StoreException, PermissionException, GraphNotFoundException;

  /**
   * Saves the given document for the episode of the given transcript.
   * @param id The transcript ID
   * @param url A URL to the document.
   * @throws StoreException If an error prevents the media from being saved.
   * @throws PermissionException If saving the media is not permitted.
   * @throws GraphNotFoundException If the transcript doesn't exist.
   */
  public void saveEpisodeDocument(String id, String url)
    throws StoreException, PermissionException, GraphNotFoundException;

  /** Synonym for {@link #deleteTranscript(String)}. */
  @Deprecated default public void deleteGraph(String id)
    throws StoreException, PermissionException, GraphNotFoundException {
    deleteTranscript(id);
  }
   
  /**
   * Deletes the given transcript, and all associated files.
   * @param id The ID transcript to delete.
   * @throws StoreException If an error prevents the transcript from being saved.
   * @throws PermissionException If saving the transcript is not permitted.
   * @throws GraphNotFoundException If the transcript doesn't exist.
   */
  public void deleteTranscript(String id)
    throws StoreException, PermissionException, GraphNotFoundException;
   
  /**
   * Deletes the given participant, and all associated meta-data.
   * @param id The ID participant to delete.
   * @throws StoreException If an error prevents the transcript from being saved.
   * @throws PermissionException If saving the transcript is not permitted.
   * @throws GraphNotFoundException If the transcript doesn't exist.
   */
  public void deleteParticipant(String id)
    throws StoreException, PermissionException, GraphNotFoundException;
   
} // end of interface GraphStore
