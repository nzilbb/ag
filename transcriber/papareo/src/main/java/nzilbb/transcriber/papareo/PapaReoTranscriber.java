//
// Copyright 2023 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.transcriber.papareo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import nzilbb.ag.*;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.Transcriber;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.util.Merger;
import nzilbb.configure.ParameterSet;
import nzilbb.formatter.webvtt.VttSerialization;
import nzilbb.papareo.PapaReo;
import nzilbb.util.Execution;
import nzilbb.util.IO;

/**
 * Automatic transcriber that uses thethe
 * <a href="https://api.papareo.io/docs">Papa Reo web API</a>
 * published by <a href="https://tehiku.nz/">Te Hiku Media</a>
 * to perform speech-to-text.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class PapaReoTranscriber extends Transcriber {

  /**
   * Default constructor.
   */
  public PapaReoTranscriber() {      
  } // end of constructor

  /**
   * <b> Get the minimum version of the nzilbb.ag API supported by the serializer. </b>
   * @return Minimum version of the nzilbb.ag API supported by the serializer.
   * @see Constants#VERSION
   */
  public String getMinimumApiVersion() {
    return "1.1.0";
  }

  /**
   * API access token.
   * <p> There is no getter, in order to keep the token secret.
   * @see #setToken(String)
   */
  private String token;
  /**
   * Setter for {@link #token}: API access token.
   * <p> This is declared final to prevent access to the token via inheritance.
   * @param newToken API access token.
   */
  public final PapaReoTranscriber setToken(String newToken) {
    // only set the token if it's not blank.
    if (newToken != null && newToken.length() > 0) {
      token = newToken;
    }
    return this;
  }
  
  /**
   * Whether to set PapaReo debug messages as status messages.
   * @see #getDebug()
   * @see #setDebug(boolean)
   */
  protected boolean debug = false;
  /**
   * Getter for {@link #debug}: Whether to set PapaReo debug messages as status messages.
   * @return Whether to set PapaReo debug messages as status messages.
   */
  public boolean getDebug() { return debug; }
  /**
   * Setter for {@link #debug}: Whether to set PapaReo debug messages as status messages.
   * @param newDebug Whether to set PapaReo debug messages as status messages.
   */
  public PapaReoTranscriber setDebug(boolean newDebug) { debug = newDebug; return this; }
    
  /**
   * Provides the overall configuration of the annotator. 
   * @return The overall configuration of the annotator, which will be passed to the
   * <i> config/index.html </i> configuration web-app, if any. This configuration may be
   * null, or a string that serializes the annotators configuration state in any encoding
   * the implementor prefers. The resulting string must be interpretable by the
   * <i> config/index.html </i> web-app. 
   * @see #setConfig(String)
   * @see #beanPropertiesToQueryString()
   */
  @Override
  public String getConfig() {
    return beanPropertiesToQueryString();
  }
  
  /**
   * Specifies the overall configuration of the transcriber, and runs any processing
   * required to install the transcriber. 
   * <p> This processing is assumed to be synchronous (this method doesn't return until
   * it's complete) and long-running, so the {@link MonitorableTask} methods should
   * provide a way for the caller to monitor/cancel processing - i.e. the Transcriber class
   * should provide an indication of progress by calling
   * {@link Transcriber#setPercentComplete(Integer)} and should regularly check 
   * {@link Transcriber#isCancelling()} to determine if installation should be stopped.
   * <p> If the user should provide information before this method is called, a 
   * <tt> config </tt> web-app must be provided to implement the user interface, which sets
   * any required configuration by invoking methods of the transcriber as required, and
   * invoking <tt> setConfig </tt> when configuration is ready.
   * <p> If the configuration needs to be persistent between installing the transcriber the
   * first time and subsequently upgrading it, then it is the transcriber's responsibility
   * to serialize it in a form which can be retrieved for a later call to {@link #getConfig()}.
   * @throws InvalidConfigurationException
   * @see #getConfig()
   * @see #beanPropertiesFromQueryString(String)
   */
  @Override
  public void setConfig(String config) throws InvalidConfigurationException {
    setRunning(true);
    setPercentComplete(0);
    setStatus(""); // clear any residual status from the last run...      
    beanPropertiesFromQueryString(config);

    // check a valid token is available
    PapaReo papaReo = new PapaReo().setToken(token);
    if (!papaReo.hasToken()) throw new InvalidConfigurationException(
      this,
      "No access token is set. Please obtain a valid access token (https://papareo.io/docs)"
      +" and then set this transcriber's 'token' attribute,"
      +" or set the papareo.token system property"
      +" or the PAPAREO_TOKEN environment variable.");
    
    setPercentComplete(100);
    setRunning(false);
  }

  /**
   *  Runs any processing required to uninstall the transcriber. 
   */
  @Override
  public void uninstall() { }

  /**
   * Specify whether the transcriber needs the audio to be split into utterance chunks
   * before {@link #transcribe(File,Graph)} is called. 
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
  public boolean getDiarizationRequired() {
    return false;
  }

  /**
   * Transcribes the given audio file, saving the resulting transcript in the given
   * graph. 
   * @param speech An audio file containing the speech to transcribe.
   * @param transcript The annotation graph that should contain the transcription. 
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
  @Override
  public Graph transcribe(File speech, Graph transcript) throws Exception {

    setRunning(true);
    try {
      setPercentComplete(0);
      PapaReo papaReo = new PapaReo().setToken(token);
      if (debug) papaReo.setDebug(m -> setStatus(m));
        
      if (!papaReo.hasToken()) throw new Exception(
        "No access token is set. Please obtain a valid access token (https://papareo.io/docs)");
      
      // TODO check whether diarization is already done
      // TODO if so, transcribe each utterance separately

      // start the transcription task
      String taskId = papaReo.transcribeLarge(new FileInputStream(speech));
      setStatus("task_id: " + taskId);

      setPercentComplete(30);
      
      // wait for it to finish
      String status = papaReo.transcribeLargeStatus(taskId);
      int patience = 60; // wait for up to this number of seconds
      while (("STARTED".equals(status) || "PENDING".equals(status))
             && !isCancelling()) {
        setStatus("waiting: " + status);
        try {Thread.sleep(1000);} catch(Exception exception) {}
        status = papaReo.transcribeLargeStatus(taskId);
      }
      setStatus("final status: " + status);
      
      if (isCancelling()) { // we've been asked to cancel
        // cancel the transcription
        String message = papaReo.transcribeLargeCancel(taskId);
        setStatus("Cancelled: " + message);
      } else {
        setPercentComplete(60);
        
        // get VTT file stream
        InputStream stream = papaReo.transcribeLargeDownload(taskId);

        setPercentComplete(70);
        // parse transcript stream
        VttSerialization deserializer = new VttSerialization();
        // default configuration
        deserializer.configure(new ParameterSet(), transcript.getSchema());
        // load transcript
        ParameterSet parameters = deserializer.load(
          Utility.OneNamedStreamArray​(
            new NamedStream(stream, IO.WithoutExtension(speech) + ".vtt")),
          transcript.getSchema());
        // default parameters
        deserializer.setParameters(parameters);
        // parse transcript
        Graph[] graphs = deserializer.deserialize();
        setPercentComplete(90);
        Graph graphFromVtt = graphs[0];        
        graphFromVtt.setId(transcript.getId());
        // if we already had a speaker...
        String participantLayerId = transcript.getSchema().getParticipantLayerId();
        Annotation participant = transcript.first(participantLayerId);
        String speakerName = IO.WithoutExtension(speech);
        if (participant != null) speakerName = participant.getLabel();
        graphFromVtt.first(participantLayerId).setLabel(speakerName);
        
        // ensure turn labels are correspondingly changed
        for (Annotation t : graphFromVtt.all(transcript.getSchema().getTurnLayerId())) {
          t.setLabel(speakerName);
        }
        // utterance labels should be the transcript, not the speaker name, so leave them as is
        
        // merge into given transcript
        Merger merger = new Merger(graphFromVtt);
        merger.transform(transcript);
        setPercentComplete(100);
        
      }
    } finally {
      setRunning(false);
    }

    return transcript;
  }
    
} // end of class PapaReoTranscriber
