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
import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.cloud.speech.v2.AutoDetectDecodingConfig;
import com.google.cloud.speech.v2.BatchRecognizeRequest;
import com.google.cloud.speech.v2.BatchRecognizeFileMetadata;
import com.google.cloud.speech.v2.BatchRecognizeResponse;
import com.google.cloud.speech.v2.BatchRecognizeFileResult;
import com.google.cloud.speech.v2.BatchRecognizeResults;
import com.google.cloud.speech.v2.CreateRecognizerRequest;
import com.google.cloud.speech.v2.DeleteRecognizerRequest;
import com.google.cloud.speech.v2.OperationMetadata;
import com.google.cloud.speech.v2.RecognitionConfig;
import com.google.cloud.speech.v2.RecognitionOutputConfig;
import com.google.cloud.speech.v2.InlineOutputConfig;
import com.google.cloud.speech.v2.RecognitionFeatures;
import com.google.cloud.speech.v2.RecognizeRequest;
import com.google.cloud.speech.v2.RecognizeResponse;
import com.google.cloud.speech.v2.Recognizer;
import com.google.cloud.speech.v2.SpeechClient;
import com.google.cloud.speech.v2.SpeechRecognitionAlternative;
import com.google.cloud.speech.v2.SpeechRecognitionResult;
import com.google.cloud.speech.v2.SpeechSettings;
import com.google.cloud.speech.v2.WordInfo;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.google.protobuf.ByteString;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import nzilbb.ag.*;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.Transcriber;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.media.wav.WAV;
import nzilbb.util.Execution;
import nzilbb.util.IO;
import com.google.auth.oauth2.ServiceAccountCredentials;
//import com.google.api.gax.core.FixedCredentialsProvider;
//import com.google.auth.Credentials;
//https://alicejli.github.io/java-cloud-bom/google-cloud-speech/index.html
//https://cloud.google.com/speech-to-text/v2/docs/how-to

/**
 * Automatic transcriber that uses Google's
 * <a target="google-speech-to-text" href="https://cloud.google.com/speech-to-text/">
 * Speech to Text </a> to transcribe recordings.
 * <p> To set up local access to the Speech to Text API:
 * <ol>
 *  <li><tt>sudo snap install --classic google-cloud-cli</tt></li>
 *  <li><tt>gcloud init</tt></li>
 *  <li><tt>gcloud auth application-default login</tt></li>
 * </ol>
 * @author Robert Fromont robert@fromont.net.nz
 */
@UsesFileSystem
public class GoogleTranscriber extends Transcriber {

  /**
   * Google Cloud Storage Project ID
   * @see #getProjectId()
   * @see #setProjectId(String)
   */
  protected String projectId = null;
  /**
   * Getter for {@link #projectId}: Google Cloud Storage Project ID
   * @return Google Cloud Storage Project ID
   */
  public String getProjectId() { return projectId; }
  /**
   * Setter for {@link #projectId}: Google Cloud Storage Project ID
   * @param newProjectId Google Cloud Storage Project ID
   */
  public GoogleTranscriber setProjectId(String newProjectId) {
    projectId = newProjectId;
    if (projectId != null && projectId.trim().length() == 0) projectId = null;
    return this;
  }
   
  /**
   * Name of the Google Cloud Storage bucket for uploading speech recordings to.
   * @see #getBucketName()
   * @see #setBucketName(String)
   */
  protected String bucketName = null;
  /**
   * Getter for {@link #bucketName}: Name of the Google Cloud Storage bucket for
   * uploading speech recordings to. 
   * @return Name of the Google Cloud Storage bucket for uploading speech recordings to.
   */
  public String getBucketName() {
    if (bucketName == null) {
      return "nzilbb-transcriber";
    }
    return bucketName;
  }
  /**
   * Setter for {@link #bucketName}: Name of the Google Cloud Storage bucket for
   * uploading speech recordings to. 
   * @param newBucketName Name of the Google Cloud Storage bucket for uploading speech
   * recordings to. 
   */
  public GoogleTranscriber setBucketName(String newBucketName) { bucketName = newBucketName; return this; }
  
  /**
   * Path to Google API key json file.
   * @see #getKeyPath()
   * @see #setKeyPath(String)
   */
  protected String keyPath = null;
  /**
   * Getter for {@link #keyPath}: Path to Google API key json file. 
   * @return Path to Google API key json file.
   */
  public String getKeyPath() {
    return keyPath;
  }
  /**
   * Setter for {@link #keyPath}: Path to Google API key json file. 
   * @param newKeyPath Path to Google API key json file.
   */
  public GoogleTranscriber setKeyPath(String newKeyPath) {
    keyPath = newKeyPath;
    if (keyPath != null && keyPath.trim().length() == 0) keyPath = null;
    return this;
  }
  
  /**
   * Number of seconds between checks whether the recognition job has finished. Default is 10.
   * @see #getPollingIntervalSeconds()
   * @see #setPollingIntervalSeconds(int)
   */
  protected int pollingIntervalSeconds = 10;
  /**
   * Getter for {@link #pollingIntervalSeconds}: Number of seconds between checks whether
   * the recognition job has finished.  Default is 10.
   * @return Number of seconds between checks whether the recognition job has finished.
   */
  public int getPollingIntervalSeconds() { return pollingIntervalSeconds; }
  /**
   * Setter for {@link #pollingIntervalSeconds}: Number of seconds between checks whether the recognition job has finished.
   * @param newPollingIntervalSeconds Number of seconds between checks whether the recognition job has finished.
   */
  public GoogleTranscriber setPollingIntervalSeconds(int newPollingIntervalSeconds) { pollingIntervalSeconds = newPollingIntervalSeconds; return this; }
  
  /**
   * Layer ID for tag of transcript language. Default is "transcript_language".
   * @see #getTranscriptLanguageLayerId()
   * @see #setTranscriptLanguageLayerId(String)
   */
  protected String transcriptLanguageLayerId = "transcript_language";
  /**
   * Getter for {@link #transcriptLanguageLayerId}: Layer ID for tag of transcript
   * language.  Default is "transcript_language".
   * @return Layer ID for tag of transcript language.
   */
  public String getTranscriptLanguageLayerId() { return transcriptLanguageLayerId; }
  /**
   * Setter for {@link #transcriptLanguageLayerId}: Layer ID for tag of transcript language.
   * @param newTranscriptLanguageLayerId Layer ID for tag of transcript language.
   */
  public GoogleTranscriber setTranscriptLanguageLayerId(String newTranscriptLanguageLayerId) {
    if (newTranscriptLanguageLayerId != null && newTranscriptLanguageLayerId.length() == 0) {
      newTranscriptLanguageLayerId = null;
    }
    transcriptLanguageLayerId = newTranscriptLanguageLayerId;
    return this;
  }
  
  /**
   * Default language to assume. Default value is "en-US".
   * @see #getDefaultLanguage()
   * @see #setDefaultLanguage(String)
   */
  protected String defaultLanguage = "en-US";
  /**
   * Getter for {@link #defaultLanguage}: Default language to assume. Default value is "en-US".
   * @return Default language to assume.
   */
  public String getDefaultLanguage() { return defaultLanguage; }
  /**
    * Setter for {@link #defaultLanguage}: Default language to assume.
    * @param newDefaultLanguage Default language to assume.
    */
  public GoogleTranscriber setDefaultLanguage(String newDefaultLanguage) { defaultLanguage = newDefaultLanguage; return this; }
  
  /**
   * Recognition model to use. Default is "latest_long".
   * @see #getModel()
   * @see #setModel(String)
   */
  protected String model = "latest_long";
  /**
   * Getter for {@link #model}: Recognition model to use. Default is "latest_long".
   * @return Recognition model to use.
   */
  public String getModel() { return model; }
  /**
   * Setter for {@link #model}: Recognition model to use.
   * @param newModel Recognition model to use.
   */
  public GoogleTranscriber setModel(String newModel) { model = newModel; return this; }
  
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
    return "1.1.2";
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

    String recognizerId = "rec-"+speech.getName().replaceAll("[^a-z0-9-]","-");

    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(speech);
    AudioFormat format = audioInputStream.getFormat();
    long frames = audioInputStream.getFrameLength();
    Double duration = WAV.duration(speech);
    setStatus("Speech duration: " + duration + "s");
    // add anchors
    Anchor graphStart = transcript.getOrCreateAnchorAt(0.0, Constants.CONFIDENCE_MANUAL);
    Anchor graphEnd = duration == null?graphStart
      :transcript.getOrCreateAnchorAt(duration, Constants.CONFIDENCE_MANUAL);
    
    // add participant if required
    Annotation participant = transcript.first(schema.getParticipantLayerId());
    if (participant == null) {
      participant = transcript.createAnnotation(
        graphStart, graphEnd, schema.getParticipantLayerId(), transcript.getId(), transcript);
      participant.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
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

    // Initialize client that will be used to send requests. This client only needs to be created
    // once, and can be reused for multiple requests. After completing all of your requests, call
    // the "close" method on the client to safely clean up any remaining background
    // resources.
    SpeechSettings.Builder speechSettingsBuilder = SpeechSettings.newBuilder();
    if (keyPath != null) {
      setStatus("Using keyPath: " + keyPath);
      speechSettingsBuilder.setCredentialsProvider(new CredentialsProvider() {
          public Credentials getCredentials() {
            try {
              return ServiceAccountCredentials.fromStream(new FileInputStream(keyPath));
            } catch (Exception x) {
              setStatus("Could not access key file \""+keyPath+"\": " + x);
              return null;
            }
          }});
    }
    try (SpeechClient speechClient = SpeechClient.create(speechSettingsBuilder.build())) {
      Path path = speech.toPath();
      byte[] data = Files.readAllBytes(path);
      ByteString audioBytes = ByteString.copyFrom(data);
      
      String parent = String.format("projects/%s/locations/global", projectId);

      String language = defaultLanguage;
      // if (transcriptLanguageLayerId != null) { TODO
      //   Annotation languageTag = transcript.first(transcriptLanguageLayerId);
      //   if (languageTag != null && languageTag.getLabel().trim().length() > 0) {
      //     language = languageTag.getLabel();
      //   }
      // }
      // setStatus("Assumed language: " + language);

      // First, create a recognizer
      Recognizer recognizer = Recognizer.newBuilder()  // TODO maybe just use default recogniser "_" instead?
        .setModel(model)
        .addLanguageCodes(language)
        .build();
      
      CreateRecognizerRequest createRecognizerRequest = CreateRecognizerRequest.newBuilder() 
        .setParent(parent)
        .setRecognizerId(recognizerId)
        .setRecognizer(recognizer)
        .build();

      OperationFuture<Recognizer, OperationMetadata> operationFuture =
        speechClient.createRecognizerAsync(createRecognizerRequest);
      recognizer = operationFuture.get();
      Storage storage = null;
      BlobId blobId = null;
      try {
        
        // Next, create the transcription request
        RecognitionConfig recognitionConfig = RecognitionConfig.newBuilder()
          .setAutoDecodingConfig(AutoDetectDecodingConfig.newBuilder().build())
          .setFeatures(RecognitionFeatures.newBuilder().setEnableWordTimeOffsets(true).build())
          // TODO diarization
          .build();

        List<SpeechRecognitionResult> results = null;
        if (duration != null && duration < 60) { // can do synchronous request
          setStatus("Starting synchronous recognize reques...t");
        
          RecognizeRequest request = RecognizeRequest.newBuilder()
            .setConfig(recognitionConfig)
            .setRecognizer(recognizer.getName())
            .setContent(audioBytes)
            .build();
          
          RecognizeResponse response = speechClient.recognize(request);
          results = response.getResultsList();
        } else { // must be asynchronous via google cloud storage
          setStatus("Starting asynchronous recognize request via Google Storage...");

          // upload to google storage...
          
          if (keyPath != null) {
            storage = StorageOptions.newBuilder()
              .setProjectId(projectId)
              .setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(keyPath)))
              .build()
              .getService();
          } else {
            storage = StorageOptions.newBuilder()
              .setProjectId(projectId)
              .build()
              .getService();
          }

          // ensure bucket exists
          Bucket bucket = storage.get(bucketName);
          if (bucket == null) { // bucket doesn't exist
            // create it
            setStatus("Creating bucket: " + bucketName);
            storage.create(BucketInfo.of(bucketName));
          }
          
          // put file in bucket
          blobId = BlobId.of(getBucketName(), speech.getName());
          BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
          Blob blob = storage.create(blobInfo, Files.readAllBytes(speech.toPath()));
          String uri = "gs://"+blob.getBucket()+"/"+blob.getName();

          BatchRecognizeFileMetadata file = BatchRecognizeFileMetadata.newBuilder()
            .setUri(uri).build();
          RecognitionOutputConfig outputConfig = RecognitionOutputConfig.newBuilder()
            .setInlineResponseConfig​(InlineOutputConfig.newBuilder().build())
            .build();
          BatchRecognizeRequest request = BatchRecognizeRequest.newBuilder()
            .setConfig(recognitionConfig)
            .setRecognitionOutputConfig(outputConfig)
            .setRecognizer(recognizer.getName())
            .addFiles(file)
            .build();
          OperationFuture <BatchRecognizeResponse,OperationMetadata> operation
            = speechClient.batchRecognizeAsync​(request);
          while (!operation.isDone()) {
            setStatus("Waiting for response...");
            Thread.sleep(pollingIntervalSeconds * 1000);
          }
          BatchRecognizeResponse response = operation.get();
          Map<String,BatchRecognizeFileResult> fileResults = response.getResults();
          BatchRecognizeFileResult result = fileResults.get(uri);
          results = result.getTranscript().getResultsList();
        }
        
        setStatus("There are " + results.size() + " results");
        for (SpeechRecognitionResult result : results) {
          // There can be several alternative transcripts for a given chunk of speech. Just use the
          // first (most likely) one here.
          if (result.getAlternativesCount() > 0) {
            SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
            
            Anchor utteranceStart = null;
            Anchor utteranceEnd = null;
            for (WordInfo word : alternative.getWordsList()) {
              // int speaker = word.getSpeakerTag();
              double startSeconds
                = word.getStartOffset().getSeconds()
                + word.getStartOffset().getNanos() * 0.000000001;
              Anchor start = transcript.getOrCreateAnchorAt(
                startSeconds, Constants.CONFIDENCE_AUTOMATIC);
              if (utteranceStart == null) utteranceStart = start;
              double endSeconds
                = word.getEndOffset().getSeconds()
                + word.getEndOffset().getNanos() * 0.000000001;
              Anchor end = transcript.getOrCreateAnchorAt(
                endSeconds, Constants.CONFIDENCE_AUTOMATIC);
              utteranceEnd = end;
              
              // TODO the words can be doubled and instantaneous, so check for that
              // TODO e.g. thank:280.0-280.5 you:280.5-280.5 thank:280.0-280.5 you:280.5-280.5
              // TODO maybe eliminate duplicates, and un-anchor instantaneous words?
              
              Annotation token = transcript.createAnnotation(
                start, end, schema.getWordLayerId(), word.getWord(), turn);
              token.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
              
              // TODO automatically break into turns based on speaker tag
            } // next word
            
            if (utteranceStart != null && utteranceEnd != null) {
              Annotation utterance = transcript.createAnnotation(
                utteranceStart, utteranceEnd, schema.getUtteranceLayerId(),
                alternative.getTranscript(), turn);
              utterance.setConfidence((int)(alternative.getConfidence() * 100));
            } // add utterance
          }
        }
        setStatus("Finished transcribing " + speech.getName());
      } finally {
        speechClient.deleteRecognizerAsync(recognizer.getName()).get();
        if (storage != null && blobId != null) { // delete the file from Google Cloud Storage
          storage.delete(blobId);
        }
      }
    }
    return transcript;
  }
   
} // end of class GoogleTranscriber
