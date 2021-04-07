//
// Copyright 2021 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag.automation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.jar.JarFile;
import javax.json.Json;
import javax.json.JsonObject;
import nzilbb.ag.*;
import nzilbb.ag.util.SimpleTokenizer;
import nzilbb.util.IO;
import nzilbb.util.MonitorableTask;

/**
 * Base class for an automated transcriber module.
 * <p> When {@link #transcribe(File, Graph)} is invoked, it should transcribe the given audio
 * file and insert the corresponding transcription data into the given annotation graph.
 * <p> The typical lifecycle of an transcriber is:
 * <ol>
 *  <li> The module is installed:
 *       <ol>
 *         <li> {@link #setSchema(Schema)} is invoked. </li>
 *         <li> {@link #setWorkingDirectory(File)} is invoked. </li>
 *         <li> {@link #getConfig()} is invoked, in case the transcriber has a default
 *              configuration. </li>
 *         <li> the user is presentated with the <a href="package-summary.html#config">
 *              config web-app</a>, if any, and </li> 
 *         <li> {@link #setConfig(String)} is invoked. (if there's no config web-app,
 *              then the config string passed will be the result of the earlier 
 *              <tt>getConfig()</tt> invocation) </li>
 *       </ol>
 *  </li>
 *  <li> Transcriber may be then run one or more times:
 *       <ol>
 *         <li> {@link #setSchema(Schema)} is invoked to provide the current schema. </li>
 *         <li> {@link #setWorkingDirectory(File)} is invoked. </li>
 *         <li> {@link #getDiarizationRequired()} is called to determine if the audio
 *              need chunking before calling {@link #transcribe(File, Graph)}. </li>
 *         <li> {@link #transcribe(File, Graph) transcribe(audio, transcript)} is invoked
 *              with the speech file and a graph that should contain the transcript. </li>
 *       </ol>
 *  </li>
 *  <li> The module is uninstalled, in which case {@link #uninstall()} is invoked, which
 *       should remove all persistent data on the system. </li>
 * </ol>
 * <p> The methods below marked in <b> bold </b> are those that an Transcriber subclass
 * should implement, in addition to {@link IGraphTransformer#transform(Graph) transform(graph)}.
 * @author Robert Fromont robert@fromont.net.nz
 */
public abstract class Transcriber extends Annotator {
   
  /**
   * <b> Specify whether the transcriber needs the audio to be split into utterance chunks
   * before {@link #transcribe(File,Graph)} is called. </b>
   * <p> If the transcriber returns true when this method is called, it should assume
   * that the {@link Schema#getParticipantLayerId() participant},
   * {@link Schema#getTurnLayerId() turn} and {@link Schema#getUtteranceLayerId() utterance} 
   * layers are populated when
   * {@link #transcribe(File,Graph)} is called, and that the utterance annotations define
   * the start and end times of individual speaker utterances for transcription.
   * <p> If the transcriber returns false when this method is called, it should assume
   * that the {@link Schema#getTurnLayerId() turn} and
   * {@link Schema#getUtteranceLayerId() utterance} layers are empty when
   * {@link #transcribe(File,Graph)} is called. 
   * @re{@link Schema#getTurnLayerId() turn} true if diarization is required, false otherwise.
   */
  public abstract boolean getDiarizationRequired();

  /**
   * <b> Transcribes the given audio file, saving the resulting transcript in the given
   * graph. </b>
   * @param speech An audio file containing the speech to transcribe.
   * @param transcript The annotation graph that should contain the transcription. 
   * <p> If the transcriber's {@link #getDiarizationRequired()} returns false, the
   * annotation graph may or may not have any annotations on the
   * {@link Schema#getTurnLayerId() turn}, {@link Schema#getUtteranceLayerId() utterance}, and
   * {@link Schema#getWordLayerId() word} layers. If there are existing annotations, they
   * should be re-used if possible, or {@link Annotation#destroy()} should be called on
   * each to ensure they're removed from the graph.
   * <p> If the transcriber's {@link #getDiarizationRequired()} returns true, it should
   * be assumed that the annotation graph has annotations on the
   * {@link Schema#getParticipantLayerId() participant}, {@link Schema#getTurnLayerId() turn}, 
   * and {@link Schema#getUtteranceLayerId() utterance} layers, and that the utterance
   * annotations define the start and end times of individual speaker utterances for
   * transcription. In this case, the transcriber should fill in the labels of the given
   * utterance annotations.
   * @return The given graph. This should have annotations structured as follows:
   *  <ul>
   *   <li> Annotations on the {@link Schema#getParticipantLayerId() participant} layer,
   *        if the given transcript had no pre-existing participants. </li>
   *   <li> Annotations on the {@link Schema#getTurnLayerId() turn} layer (even if it's
   *        one big turn encompassing the whole transcript), with the parent(s) set to the
   *        corresponding participant annotations. The turn labels should match the participant
   *        labels </li> 
   *   <li> Annotations on the {@link Schema#getUtteranceLayerId() utterance} layer, with
   *        the parent(s) set to the corresponding turn annotations. The labels should be the
   *        transcript of the utterance. </li>
   *   <li> Optionally, new annotations on the {@link Schema#getWordLayerId() word}
   *        layer, representing individual word tokens with alignment information, if
   *        available.</li> 
   *  </ul>
   * @throws Exception
   */
  public abstract Graph transcribe(File speech, Graph transcript) throws Exception;
  
  /**
   * Normally, a transcriber has no specific task configuration, so this implementation
   * does nothing.
   * @param parameters The configuration of the annotator, encoded in a String using
   * whatever mechanism is preferred (serialization of Properties object, JSON, etc.)
   * @throws InvalidConfigurationException
   */
  public void setTaskParameters(String parameters) throws InvalidConfigurationException {
  }
  
  /**
   * Requires participant and turn layers, and also utterance layer if 
   * {@link #getDiarizationRequired()} returns true.
   * @return A list of layer IDs.
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public String[] getRequiredLayers() throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");
    HashSet<String> requiredLayers = new HashSet<String>();
    requiredLayers.add(schema.getParticipantLayerId());
    requiredLayers.add(schema.getTurnLayerId());
    if (getDiarizationRequired()) requiredLayers.add(schema.getUtteranceLayerId());
    return requiredLayers.toArray(new String[0]);
  }
  
  /**
   * Determines which layers the annotator will create/update/delete annotations on.
   * @return A list of layer IDs.
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public String[] getOutputLayers() throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");
    if (getDiarizationRequired()) {
      return new String[] { schema.getWordLayerId() };
    } else {
      return new String[] { schema.getUtteranceLayerId(), schema.getWordLayerId() };
    }
  }

  /**
   * Transforms the graph by calling {@link #transcribe(File,Graph)} it if audio is
   * accessible and it has no words. 
   * @param transcript The graph to transform.
   * @return The given graph, transformed.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph transform(Graph transcript) throws TransformationException {
    if (transcript == null) return null; // just in case

    if (transcript.all(schema.getWordLayerId()).length == 0) { // there are no words
      GraphMediaProvider mediaProvider = transcript.getMediaProvider();
      if (mediaProvider != null) { // there is a media provider
        try {
          String mediaUrl = mediaProvider.getMedia("", "audio/wav");
          if (mediaUrl != null && mediaUrl.startsWith("file:")) { // audio is available
            try {
              File speech = new File(new URI(mediaUrl));
              try {
                
                // transcribe the speech
                transcript.trackChanges();
                transcript = transcribe(speech, transcript);
                
                // if there are utterances but no words still
                if (transcript.all(schema.getUtteranceLayerId()).length > 0
                    && transcript.all(schema.getWordLayerId()).length == 0) {
                  
                  // tokenize the utterances
                  transcript = new SimpleTokenizer(
                    transcript.getSchema().getUtteranceLayerId(),
                    transcript.getSchema().getWordLayerId())
                    .transform(transcript);
                }
                
                return transcript;
                
              } catch(Exception exception) {
                throw new TransformationException(
                  this, "Could not transcribe speech for " + transcript.getId()+ " : "
                  + exception.getMessage(), exception);
              }
            } catch(URISyntaxException exception) {
              throw new TransformationException(
                this, "Cannot get audio for " + transcript.getId() + " - URL: " + mediaUrl
                + " : " + exception.getMessage(), exception);
            }
          } // audio is available
        } catch (StoreException exception) {
          throw new TransformationException(
            this, "Cannot get media provider for " + transcript.getId()
            + " : " + exception.getMessage(), exception);
        } catch (PermissionException exception) {
          throw new TransformationException(
            this, "Not allowed to access media for " + transcript.getId()
            + " : " + exception.getMessage(), exception);
        }
      } // there is a media provider
    } // there are no words
    
    return transcript;
  }
} // end of class Transcriber
