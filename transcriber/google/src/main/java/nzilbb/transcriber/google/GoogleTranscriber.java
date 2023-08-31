//
// Copyright 2021-2023 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.transcriber.google;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.speech.v1p1beta1.LongRunningRecognizeMetadata;
import com.google.cloud.speech.v1p1beta1.LongRunningRecognizeResponse;
import com.google.cloud.speech.v1p1beta1.RecognitionAudio;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig;
import com.google.cloud.speech.v1p1beta1.RecognizeResponse;
import com.google.cloud.speech.v1p1beta1.SpeakerDiarizationConfig;
import com.google.cloud.speech.v1p1beta1.SpeechClient;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionResult;
import com.google.cloud.speech.v1p1beta1.WordInfo;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.protobuf.ByteString;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import nzilbb.ag.*;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.Transcriber;
import nzilbb.util.Execution;
import nzilbb.util.IO;
//import com.google.api.gax.core.FixedCredentialsProvider;
//import com.google.auth.Credentials;

/**
 * Automatic transcriber that uses Google's
 * <a target="google-speech-to-text" href="https://cloud.google.com/speech-to-text/">
 * Speech to Text </a> to transcribe recordings.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class GoogleTranscriber extends Transcriber {

  /**
   * Google Cloud Storage Project ID
   * @see #getProjectId()
   * @see #setProjectId(String)
   */
  protected String projectId = "glassy-outcome-308619"; // TODO remove
  /**
   * Getter for {@link #projectId}: Google Cloud Storage Project ID
   * @return Google Cloud Storage Project ID
   */
  public String getProjectId() { return projectId; }
  /**
   * Setter for {@link #projectId}: Google Cloud Storage Project ID
   * @param newProjectId Google Cloud Storage Project ID
   */
  public GoogleTranscriber setProjectId(String newProjectId) { projectId = newProjectId; return this; }
   
  /**
   * Name of the Google Cloud Storage bucket for uploading speech recordings to.
   * @see #getBucketName()
   * @see #setBucketName(String)
   */
  protected String bucketName = "nzilbb-test"; // TODO remove  
  /**
   * Getter for {@link #bucketName}: Name of the Google Cloud Storage bucket for
   * uploading speech recordings to. 
   * @return Name of the Google Cloud Storage bucket for uploading speech recordings to.
   */
  public String getBucketName() { return bucketName; }
  /**
   * Setter for {@link #bucketName}: Name of the Google Cloud Storage bucket for
   * uploading speech recordings to. 
   * @param newBucketName Name of the Google Cloud Storage bucket for uploading speech
   * recordings to. 
   */
  public GoogleTranscriber setBucketName(String newBucketName) { bucketName = newBucketName; return this; }

  /**
   * Default constructor.
   */
  public GoogleTranscriber() {      
  } // end of constructor

  /**
   * <b> Get the minimum version of the nzilbb.ag API supported by the serializer. </b>
   * @return Minimum version of the nzilbb.ag API supported by the serializer.
   * @see Constants#VERSION
   */
  public String getMinimumApiVersion() {
    return "1.0.0";
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
   * Google Speech doesn it's own diarization.
   * @return true.
   */
  public boolean getDiarizationRequired() {
    return false;
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
            return !name.equals(getAnnotatorId() + ".cfg");
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

    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(speech);
    AudioFormat format = audioInputStream.getFormat();
    long frames = audioInputStream.getFrameLength();
    double duration = 0;
    if (frames > 0) {
      duration = ((double)frames) / format.getFrameRate(); 
    } else {
      System.err.println("Could not determine duration from audio: " + speech.getName());
    }
    // add anchors
    Anchor graphStart = transcript.getOrCreateAnchorAt(0.0, Constants.CONFIDENCE_MANUAL);
    Anchor graphEnd = transcript.getOrCreateAnchorAt(duration, Constants.CONFIDENCE_MANUAL);
      
    // add participant if required
    Annotation participant = transcript.first(schema.getParticipantLayerId());
    if (participant == null) {
      participant = new Annotation()
        .setLayerId(schema.getParticipantLayerId())
        .setLabel(transcript.getId())
        .setStartId(graphStart.getId())
        .setEndId(graphEnd.getId());
      participant.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
      transcript.addAnnotation(participant);
    } else {
      participant
        .setStartId(graphStart.getId())
        .setEndId(graphEnd.getId());
    }
    // add turn if required
    Annotation turn = participant.first(schema.getTurnLayerId());
    if (turn == null) {
      turn = transcript.createTag(
        participant, schema.getTurnLayerId(), participant.getLabel());
    }
    turn.setConfidence(Constants.CONFIDENCE_AUTOMATIC);

    Annotation utterance = turn.first(schema.getUtteranceLayerId());      
    if (utterance == null) {
      utterance = transcript.createTag(
        turn, schema.getUtteranceLayerId(), turn.getLabel());
    }

    // Upload file to Google Cloud Storage 
    Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
    BlobId blobId = BlobId.of(bucketName, speech.getName());
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
    Blob blob = storage.create(blobInfo, Files.readAllBytes(speech.toPath()));
    String uri = "gs://"+blob.getBucket()+"/"+blob.getName();

    // Instantiate a client
    // SpeechSettings speechSettings = // TODO auth configuration via the app
    //    SpeechSettings.newBuilder()
    //    .setCredentialsProvider(FixedCredentialsProvider.create(myCredentials))
    //    .build();
    try (SpeechClient speechClient = SpeechClient.create(/*speechSettings*/)) {
         
      // Builds the sync recognize request
      RecognitionConfig config =
        RecognitionConfig.newBuilder()
        // .setEncoding(AudioEncoding.LINEAR16) // TODO format.get??()
        .setSampleRateHertz((int)format.getSampleRate())
        .setLanguageCode("en-US") // TODO get from the graph?
        .setEnableWordTimeOffsets(true)
        .setDiarizationConfig(
          SpeakerDiarizationConfig.newBuilder()
          .setEnableSpeakerDiarization(true)               
          .setMinSpeakerCount(2) // TODO make these configurable
          .setMaxSpeakerCount(2)
          .build())
        .build();
      RecognitionAudio audio = RecognitionAudio.newBuilder().setUri(uri).build();
         
      // Performs speech recognition on the audio file
      // Use non-blocking call for getting file transcription
      OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response =
        speechClient.longRunningRecognizeAsync(config, audio);
         
      while (!response.isDone()) {
        System.out.println("Waiting for response...");
        Thread.sleep(10000);
      }

      LongRunningRecognizeResponse longRunningRecognizeResponse = response.get();
      SpeechRecognitionAlternative alternative =
        longRunningRecognizeResponse
        .getResults(longRunningRecognizeResponse.getResultsCount() - 1)
        .getAlternatives(0);         
      int confidence = (int)(alternative.getConfidence() * 100);
         
      // alternative.getTranscript() gives the full text
      System.out.println("Transcription: " + alternative.getTranscript());
         
      // but Google also provides individual words, with alignment, so we'll use those
      // instead
      for (WordInfo word : alternative.getWordsList()) {
        int speaker = word.getSpeakerTag();
        System.out.println(word.toString());
        Annotation token = new Annotation()
          .setLayerId(schema.getWordLayerId())
          .setLabel(word.getWord())
          .setParentId(turn.getId());
        double startSeconds
          = word.getStartTime().getSeconds()
          + word.getStartTime().getNanos() * 0.000000001;
        Anchor start = transcript.getOrCreateAnchorAt(
          startSeconds, Constants.CONFIDENCE_AUTOMATIC);
        token.setStartId(start.getId());
        double endSeconds
          = word.getEndTime().getSeconds()
          + word.getEndTime().getNanos() * 0.000000001;
        Anchor end = transcript.getOrCreateAnchorAt(
          endSeconds, Constants.CONFIDENCE_AUTOMATIC);
        token.setEndId(end.getId());
        token.setConfidence(confidence);

        // TODO the words can be doubled and instantaneous, so check for that
        // TODO e.g. thank:280.0-280.5 you:280.5-280.5 thank:280.0-280.5 you:280.5-280.5
        // TODO maybe eliminate duplicates, and un-anchor instantaneous words?
            
        transcript.addAnnotation(token);
            
        // TODO automatically break into utterances based on pauses between words
        // TODO automatically break into turns based on speaker tag
      } // next word

    } finally {
      // delete the file from Google Cloud Storage
      storage.delete(blobId);
    }      
            
    return transcript;
  }
   
} // end of class GoogleTranscriber
