//
// Copyright 2025 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.transcriber.evaluator;

import java.io.File;
import java.io.FileNotFoundException;
import nzilbb.ag.*;
import nzilbb.ag.util.Merger;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.Transcriber;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.serialize.GraphDeserializer;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.annotator.orthography.OrthographyStandardizer;
import nzilbb.util.IO;

/**
 * This 'transcriber' actually uses a given Deserializer to read
 * pre-transcribed transcripts from the file system, so ASR systems
 * can be evaluated after transcription is finished.
 */
public class Pretranscribed extends Transcriber {
  /** Default constructor */
  public Pretranscribed() {
  }
  
  /**
   * The minimum version of the nzilbb.ag API supported by the transcriber.
   * @return Minimum version of the nzilbb.ag API supported by the transcriber.
   * @see Constants#VERSION
   */
  public String getMinimumApiVersion() {
    return "1.1.0";
  }

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
   * The directory where pretranscribed transcript files are to be found.
   * @see #getTranscriptDirectory()
   * @see #setTranscriptDirectory(File)
   */
  protected File transcriptDirectory;
  /**
   * Getter for {@link #transcriptDirectory}: The directory where
   * pretranscribed transcript files are to be found. 
   * @return The directory where pretranscribed transcript files are to be found.
   */
  public File getTranscriptDirectory() { return transcriptDirectory; }
  /**
   * Setter for {@link #transcriptDirectory}: The directory where
   * pretranscribed transcript files are to be found. 
   * @param newTranscriptDirectory The directory where pretranscribed
   * transcript files are to be found. 
   */
  public Pretranscribed setTranscriptDirectory(File newTranscriptDirectory) { transcriptDirectory = newTranscriptDirectory; return this; }
  
  /**
   * The {@link #GraphDeserializer} to use for reading transcripts.
   * @see #getDeserializer()
   * @see #setDeserializer(GraphDeserializer)
   */
  protected GraphDeserializer deserializer;
  /**
   * Getter for {@link #deserializer}: The {@link #GraphDeserializer}
   * to use for reading transcripts. 
   * @return The #GraphDeserializer to use for reading transcripts.
   */
  public GraphDeserializer getDeserializer() { return deserializer; }
  /**
   * Setter for {@link #deserializer}: The {@link #GraphDeserializer}
   * to use for reading transcripts. 
   * @param newDeserializer The #GraphDeserializer to use for reading transcripts.
   */
  public Pretranscribed setDeserializer(GraphDeserializer newDeserializer) { deserializer = newDeserializer; return this; }
  
  /**
   * The orthography standardizer.
   * @see #getStandardizer()
   * @see #setStandardizer(OrthographyStandardizer)
   */
  protected OrthographyStandardizer standardizer;
  /**
   * Getter for {@link #standardizer}: The orthography standardizer.
   * @return The orthography standardizer.
   */
  public OrthographyStandardizer getStandardizer() { return standardizer; }
  /**
   * Setter for {@link #standardizer}: The orthography standardizer.
   * @param newStandardizer The orthography standardizer.
   */
  public Pretranscribed setStandardizer(OrthographyStandardizer newStandardizer) { standardizer = newStandardizer; return this; }

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
   * Transcribes the given audio file, in our case, this involves
   * looking for a file that matches the transcript ID, and loading
   * that as the transcript.
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

      // look for a a transcript matching the .wav file, in the same directory
      String baseName = IO.WithoutExtension(speech);
      File pretranscription = null;
      for (String ext : deserializer.getDescriptor().getFileSuffixes()) {
        File t = new File(transcriptDirectory, baseName + ext);
        if (t.exists()) {
          pretranscription = t;
          break; // found the file
        }
      }
      setPercentComplete(10);
      if (pretranscription == null) {
        throw new FileNotFoundException("No transcript for " + speech.getName());
      }

      // load transcript file
      NamedStream[] streams = { new NamedStream(pretranscription) };
      // use default parameters for loading this file
      deserializer.setParameters(
        deserializer.load(streams, getSchema()));
      // deserialize
      setPercentComplete(70);
      Graph[] graphs = deserializer.deserialize();
      Graph pretranscribed = graphs[0];

      // standardize orthography?
      if (standardizer != null) standardizer.transform(pretranscribed);      
      
      // merge into given transcript
      Merger merger = new Merger(pretranscribed);
      merger.transform(transcript);
      setPercentComplete(100);
    } finally {
      setRunning(false);
    }
    
    return transcript;
  }
}
