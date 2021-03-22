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
package nzilbb.stt.deepspeech;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import nzilbb.ag.*;
import nzilbb.ag.stt.InvalidConfigurationException;
import nzilbb.ag.stt.Transcriber;
import nzilbb.util.Execution;
import nzilbb.util.IO;

/**
 * Automatic transcriber that uses locally-installed
 * <a href="https://github.com/mozilla/DeepSpeech/"> DeepSpeech </a> 
 * to perform speech-to-text.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class DeepSpeechTranscriber extends Transcriber {

   File deepSpeechExe;
   
   /**
    * Default constructor.
    */
   public DeepSpeechTranscriber() {      
      deepSpeechExe = Execution.Which("deepspeech");
   } // end of constructor

   /**
    * <b> Get the minimum version of the nzilbb.ag API supported by the serializer. </b>
    * @return Minimum version of the nzilbb.ag API supported by the serializer.
    * @see Constants#VERSION
    */
   public String getMinimumApiVersion() {
      return "20210319.1109";
   }
   
   /**
    * Selected models for speech-to-text.
    * @see #getModels()
    * @see #setModels(String)
    */
   protected String models;
   /**
    * Getter for {@link #models}: Selected models for speech-to-text.
    * @return Selected models for speech-to-text.
    */
   public String getModels() { return models; }
   /**
    * Setter for {@link #models}: Selected models for speech-to-text.
    * @param newModels Selected models for speech-to-text.
    */
   public DeepSpeechTranscriber setModels(String newModels) { models = newModels; return this; }

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
      running = true;
      setPercentComplete(0);
      setStatus(""); // clear any residual status from the last run...      
      beanPropertiesFromQueryString(config);      
      setPercentComplete(100);
      running = false;
   }

   /**
    *  Runs any processing required to uninstall the transcriber. 
    */
   @Override
   public void uninstall() { }

   /**
    *  Specify whether the transcriber needs the audio to be split into utterance chunks
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
      return false; // TODO actually, true
   }

   /**
    * Which version of DeepSpeech is installed.
    * @return Which version of DeepSpeech is installed, or null if DeepSpeech is not installed.
    */
   public String getDeepSpeechVersion() {
      if (deepSpeechExe == null) return null;
      Execution deepSpeech = new Execution().setExe(deepSpeechExe).arg("--version");
      deepSpeech.run();
      return deepSpeech.getInput().toString().trim();
   }
   
   /**
    * Takes a file to containing models for speech-to-text.
    * @param file The models file.
    * @return null if upload was successful, an error message otherwise.
    */
   public String uploadModels(File file) {
      File localFile = new File(getWorkingDirectory(), file.getName());
      if (file.renameTo(file)) {
         try {
            IO.Copy(file, localFile);
         } catch(IOException exception) {
            return "Could not copy " + file.getName() + ": " + exception.getMessage();
         }
      }
      return null;
   } // end of uploadLexicon()
   
   /**
    * Lists the model files that are available for use.
    * @return A list of file names that can be selected.
    */
   public List<String> availableModels() {
      return Arrays.asList(
         getWorkingDirectory().list(new FilenameFilter() {
               public boolean accept(File dir, String name) {
                  return !name.equals(getTranscriberId() + ".cfg");
               }
            }));
   } // end of availableModels()

   /**
    * Transcribes the given audio file, saving the resulting transcript in the given
    * graph. 
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
   @Override
   public Graph transcribe(File speech, Graph transcript) throws Exception {

      if (deepSpeechExe == null) throw new Exception("DeepSpeech is not installed.");
      
      // run deepspeech recognizer
      Execution deepspeech = new Execution()
         .setExe(deepSpeechExe)
         .arg("--model").arg(new File(getWorkingDirectory(), models).getPath())
         .arg("--audio").arg(speech.getPath());
      deepspeech.run();
      
      // pass through stderr...
      System.err.println(deepspeech.getError().toString());
      System.err.println(deepspeech.getInput().toString());

      // get duration of audio...

      // deepspeech writes the duration to stderr as "...for n.nnns audio file..."
      Double duration = null;
      try {

         Pattern durationPattern = Pattern.compile("for ([0-9]+.[0-9]+)s audio file");
         Matcher durationMatcher = durationPattern.matcher(deepspeech.getError().toString());
         if (durationMatcher.find()) {
            duration = Double.parseDouble(durationMatcher.group(1));
         }
         
      } catch (Exception x) {
         System.err.println("Could not determine duration from deepspeech output: " + x);
      }      
      if (duration == null) {         
         // get it from the audio file itself
         AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(speech);
         AudioFormat format = audioInputStream.getFormat();
         long frames = audioInputStream.getFrameLength();
         if (frames > 0) {
            duration = ((double)frames) / format.getFrameRate(); 
         } else {
            System.err.println("Could not determine duration from audio: " + speech.getName());
         }
      }
      if (duration == null) {
         System.err.println("Using duration of 1s");
         duration = 1.0;
      }
      
      // add anchors
      Anchor start = transcript.getOrCreateAnchorAt(0.0, Constants.CONFIDENCE_MANUAL);
      Anchor end = transcript.getOrCreateAnchorAt(duration, Constants.CONFIDENCE_MANUAL);
      
      // add participant if required
      Annotation participant = transcript.first(schema.getParticipantLayerId());
      if (participant == null) {
         participant = new Annotation()
            .setLayerId(schema.getParticipantLayerId())
            .setLabel(transcript.getId())
            .setStartId(start.getId())
            .setEndId(end.getId());
         participant.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
         transcript.addAnnotation(participant);
      } else {
         participant
            .setStartId(start.getId())
            .setEndId(end.getId());
      }

      // add turn
      Annotation turn = participant.first(schema.getTurnLayerId());
      if (turn == null) {
         transcript.createTag(
            participant, schema.getTurnLayerId(), participant.getLabel());
      }
      turn.setConfidence(Constants.CONFIDENCE_AUTOMATIC);

      // add transcription
      String transcription = deepspeech.getInput().toString();
      Annotation utterance = turn.first(schema.getUtteranceLayerId());      
      if (utterance == null) {
         utterance = transcript.createTag(
            turn, schema.getUtteranceLayerId(), transcription);
      } else {
         utterance.setLabel(transcription);
      }
      utterance.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
      
      return transcript;
   }

} // end of class DeepSpeechTranscriber
