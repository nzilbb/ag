//
// Copyright 2022 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.transcriber.whisper;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
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
import nzilbb.ag.util.Merger;
import nzilbb.configure.ParameterSet;
import nzilbb.formatter.webvtt.VttSerialization;
import nzilbb.util.Execution;
import nzilbb.util.IO;

/**
 * Automatic transcriber that uses the
 * <a href="https://github.com/openai/whisper">Whisper</a> 
 * ASR system to perform speech-to-text.
 * @author Robert Fromont robert@fromont.net.nz
 */
@UsesFileSystem
public class WhisperTranscriber extends Transcriber {

  File whisperExe;
   
  /**
   * Default constructor.
   */
  public WhisperTranscriber() {      
    whisperExe = Execution.Which("whisper");
  } // end of constructor

  /**
   * <b> Get the minimum version of the nzilbb.ag API supported by the serializer. </b>
   * @return Minimum version of the nzilbb.ag API supported by the serializer.
   * @see Constants#VERSION
   */
  public String getMinimumApiVersion() {
    return "1.0.7";
  }

  /**
   * Model to use. Default is "medium.en".
   * @see #getModel()
   * @see #setModel(String)
   */
  protected String model = "medium.en";
  /**
   * Getter for {@link #model}: Model to use. Default is "medium.en".
   * @return Model to use.
   */
  public String getModel() { return model; }
  /**
   * Setter for {@link #model}: Model to use.
   * @param newModel Model to use.
   */
  public WhisperTranscriber setModel(String newModel) { model = newModel; return this; }
  
  /**
   * Language spoken in the audio, specify None to perform language detection (default: None)
   * @see #getLanguage()
   * @see #setLanguage(String)
   */
  protected String language;
  /**
   * Getter for {@link #language}: Language spoken in the audio, specify None to perform
   * language detection (default: None) 
   * @return Language spoken in the audio, specify None to perform language detection
   * (default: None) 
   */
  public String getLanguage() { return language; }
  /**
   * Setter for {@link #language}: Language spoken in the audio, specify None to perform
   * language detection (default: None) 
   * @param newLanguage Language spoken in the audio, specify None to perform language
   * detection (default: None) 
   */
  public WhisperTranscriber setLanguage(String newLanguage) { language = newLanguage; return this; }

  /**
   * Temperature to use for sampling (default: 0)
   * @see #getTemperature()
   * @see #setTemperature(Double)
   */
  protected Double temperature;
  /**
   * Getter for {@link #temperature}: Temperature to use for sampling (default: 0)
   * @return Temperature to use for sampling (default: 0)
   */
  public Double getTemperature() { return temperature; }
  /**
   * Setter for {@link #temperature}: Temperature to use for sampling (default: 0)
   * @param newTemperature Temperature to use for sampling (default: 0)
   */
  public WhisperTranscriber setTemperature(Double newTemperature) { temperature = newTemperature; return this; }
  
  /**
   * Number of candidates when sampling with non-zero temperature (default: 5)
   * @see #getBestOf()
   * @see #setBestOf(Integer)
   */
  protected Integer bestOf;
  /**
   * Getter for {@link #bestOf}: Number of candidates when sampling with non-zero
   * temperature (default: 5) 
   * @return Number of candidates when sampling with non-zero temperature (default: 5)
   */
  public Integer getBestOf() { return bestOf; }
  /**
   * Setter for {@link #bestOf}: Number of candidates when sampling with non-zero
   * temperature (default: 5) 
   * @param newBestOf Number of candidates when sampling with non-zero temperature (default: 5)
   */
  public WhisperTranscriber setBestOf(Integer newBestOf) { bestOf = newBestOf; return this; }

  /**
   * Number of beams in beam search, only applicable when temperature is zero (default: 5)
   * @see #getBeamSize()
   * @see #setBeamSize(Integer)
   */
  protected Integer beamSize;
  /**
   * Getter for {@link #beamSize}: Number of beams in beam search, only applicable when
   * temperature is zero (default: 5) 
   * @return Number of beams in beam search, only applicable when temperature is zero (default: 5)
   */
  public Integer getBeamSize() { return beamSize; }
  /**
   * Setter for {@link #beamSize}: Number of beams in beam search, only applicable when
   * temperature is zero (default: 5) 
   * @param newBeamSize Number of beams in beam search, only applicable when temperature
   * is zero (default: 5) 
   */
  public WhisperTranscriber setBeamSize(Integer newBeamSize) { beamSize = newBeamSize; return this; }

  /**
   * Optional patience value to use in beam decoding, as in
   * https://arxiv.org/abs/2204.05424, the default (0.0) is equivalent to not using
   * patience (default: 0.0) 
   * @see #getPatience()
   * @see #setPatience(Double)
   */
  protected Double patience;
  /**
   * Getter for {@link #patience}: Optional patience value to use in beam decoding, as in
   * https://arxiv.org/abs/2204.05424, the default (0.0) is equivalent to not using
   * patience (default: 0.0) 
   * @return Optional patience value to use in beam decoding, as in
   * https://arxiv.org/abs/2204.05424, the default (0.0) is equivalent to not using
   * patience (default: 0.0) 
   */
  public Double getPatience() { return patience; }
  /**
   * Setter for {@link #patience}: Optional patience value to use in beam decoding, as in
   * https://arxiv.org/abs/2204.05424, the default (0.0) is equivalent to not using
   * patience (default: 0.0) 
   * @param newPatience Optional patience value to use in beam decoding, as in
   * https://arxiv.org/abs/2204.05424, the default (0.0) is equivalent to not using
   * patience (default: 0.0) 
   */
  public WhisperTranscriber setPatience(Double newPatience) { patience = newPatience; return this; }

  /**
   * Optional token length penalty coefficient (alpha) as in
   * https://arxiv.org/abs/1609.08144, uses simple length normalization by default
   * (default: None) 
   * @see #getLengthPenalty()
   * @see #setLengthPenalty(Integer)
   */
  protected Integer lengthPenalty;
  /**
   * Getter for {@link #lengthPenalty}: Optional token length penalty coefficient (alpha)
   * as in https://arxiv.org/abs/1609.08144, uses simple length normalization by default
   * (default: None) 
   * @return Optional token length penalty coefficient (alpha) as in
   * https://arxiv.org/abs/1609.08144, uses simple length normalization by default
   * (default: None) 
   */
  public Integer getLengthPenalty() { return lengthPenalty; }
  /**
   * Setter for {@link #lengthPenalty}: Optional token length penalty coefficient (alpha)
   * as in https://arxiv.org/abs/1609.08144, uses simple length normalization by default
   * (default: None) 
   * @param newLengthPenalty Optional token length penalty coefficient (alpha) as in
   * https://arxiv.org/abs/1609.08144, uses simple length normalization by default
   * (default: None) 
   */
  public WhisperTranscriber setLengthPenalty(Integer newLengthPenalty) { lengthPenalty = newLengthPenalty; return this; }
  
  /**
   * Comma-separated list of token ids to suppress during sampling; '-1' will suppress
   * most special characters except common punctuations (default: -1) 
   * @see #getSuppressTokens()
   * @see #setSuppressTokens(String)
   */
  protected String suppressTokens;
  /**
   * Getter for {@link #suppressTokens}: Comma-separated list of token ids to suppress
   * during sampling; '-1' will suppress most special characters except common
   * punctuations (default: -1) 
   * @return Comma-separated list of token ids to suppress during sampling; '-1' will
   * suppress most special characters except common punctuations (default: -1) 
   */
  public String getSuppressTokens() { return suppressTokens; }
  /**
   * Setter for {@link #suppressTokens}: Comma-separated list of token ids to suppress
   * during sampling; '-1' will suppress most special characters except common
   * punctuations (default: -1) 
   * @param newSuppressTokens Comma-separated list of token ids to suppress during
   * sampling; '-1' will suppress most special characters except common punctuations
   * (default: -1) 
   */
  public WhisperTranscriber setSuppressTokens(String newSuppressTokens) { suppressTokens = newSuppressTokens; return this; }

  /**
   * Whether to perform inference in fp16; True by default (default: True)
   * @see #getFp16()
   * @see #setFp16(Boolean)
   */
  protected Boolean fp16;
  /**
   * Getter for {@link #fp16}: Whether to perform inference in fp16; True by default
   * (default: True) 
   * @return Whether to perform inference in fp16; True by default (default: True)
   */
  public Boolean getFp16() { return fp16; }
  /**
   * Setter for {@link #fp16}: Whether to perform inference in fp16; True by default
   * (default: True) 
   * @param newFp16 Whether to perform inference in fp16; True by default (default: True)
   */
  public WhisperTranscriber setFp16(Boolean newFp16) { fp16 = newFp16; return this; }

  /**
   * Temperature to increase when falling back when the decoding fails to meet either of
   * the thresholds below (default: 0.2) 
   * @see #getTemperatureIncrementOnFallback()
   * @see #setTemperatureIncrementOnFallback(Double)
   */
  protected Double temperatureIncrementOnFallback;
  /**
   * Getter for {@link #temperatureIncrementOnFallback}: Temperature to increase when
   * falling back when the decoding fails to meet either of the thresholds below (default:
   * 0.2) 
   * @return Temperature to increase when falling back when the decoding fails to meet
   * either of the thresholds below (default: 0.2) 
   */
  public Double getTemperatureIncrementOnFallback() { return temperatureIncrementOnFallback; }
  /**
   * Setter for {@link #temperatureIncrementOnFallback}: Temperature to increase when
   * falling back when the decoding fails to meet either of the thresholds below (default:
   * 0.2) 
   * @param newTemperatureIncrementOnFallback Temperature to increase when falling back when the decoding fails to meet either of the thresholds below (default: 0.2)
   */
  public WhisperTranscriber setTemperatureIncrementOnFallback(Double newTemperatureIncrementOnFallback) { temperatureIncrementOnFallback = newTemperatureIncrementOnFallback; return this; }

  /**
   * If the gzip compression ratio is higher than this value, treat the decoding as failed
   * (default: 2.4) 
   * @see #getCompressionRatioThreshold()
   * @see #setCompressionRatioThreshold(Double)
   */
  protected Double compressionRatioThreshold;
  /**
   * Getter for {@link #compressionRatioThreshold}: If the gzip compression ratio is
   * higher than this value, treat the decoding as failed (default: 2.4) 
   * @return If the gzip compression ratio is higher than this value, treat the decoding
   * as failed (default: 2.4) 
   */
  public Double getCompressionRatioThreshold() { return compressionRatioThreshold; }
  /**
   * Setter for {@link #compressionRatioThreshold}: If the gzip compression ratio is
   * higher than this value, treat the decoding as failed (default: 2.4) 
   * @param newCompressionRatioThreshold If the gzip compression ratio is higher than this
   * value, treat the decoding as failed (default: 2.4) 
   */
  public WhisperTranscriber setCompressionRatioThreshold(Double newCompressionRatioThreshold) { compressionRatioThreshold = newCompressionRatioThreshold; return this; }

  /**
   * If the average log probability is lower than this value, treat the decoding as failed
   * (default: -1.0) 
   * @see #getLogprobThreshold()
   * @see #setLogprobThreshold(Double)
   */
  protected Double logprobThreshold;
  /**
   * Getter for {@link #logprobThreshold}: If the average log probability is lower than
   * this value, treat the decoding as failed (default: -1.0) 
   * @return If the average log probability is lower than this value, treat the decoding
   * as failed (default: -1.0) 
   */
  public Double getLogprobThreshold() { return logprobThreshold; }
  /**
   * Setter for {@link #logprobThreshold}: If the average log probability is lower than
   * this value, treat the decoding as failed (default: -1.0) 
   * @param newLogprobThreshold If the average log probability is lower than this value,
   * treat the decoding as failed (default: -1.0) 
   */
  public WhisperTranscriber setLogprobThreshold(Double newLogprobThreshold) { logprobThreshold = newLogprobThreshold; return this; }

  /**
   * If the probability of the <|nospeech|> token is higher than this value AND the
   * decoding has failed due to logprobThreshold, consider the segment as silence
   * (default: 0.6) 
   * @see #getNoSpeechThreshold()
   * @see #setNoSpeechThreshold(Double)
   */
  protected Double noSpeechThreshold;
  /**
   * Getter for {@link #noSpeechThreshold}: If the probability of the <|nospeech|> token
   * is higher than this value AND the decoding has failed due to logprobThreshold,
   * consider the segment as silence (default: 0.6) 
   * @return If the probability of the <|nospeech|> token is higher than this value AND
   * the decoding has failed due to logprobThreshold, consider the segment as silence
   * (default: 0.6) 
   */
  public Double getNoSpeechThreshold() { return noSpeechThreshold; }
  /**
   * Setter for {@link #noSpeechThreshold}: If the probability of the <|nospeech|> token
   * is higher than this value AND the decoding has failed due to logprobThreshold,
   * consider the segment as silence (default: 0.6) 
   * @param newNoSpeechThreshold If the probability of the <|nospeech|> token is higher
   * than this value AND the decoding has failed due to logprobThreshold, consider the
   * segment as silence (default: 0.6) 
   */
  public WhisperTranscriber setNoSpeechThreshold(Double newNoSpeechThreshold) { noSpeechThreshold = newNoSpeechThreshold; return this; }
  
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

    // TODO try to install whisper if it isn't installed?
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
      if (whisperExe == null) throw new Exception("Whisper is not installed.");
      
      // TODO check whether diarization is already done
      // TODO if so, transcribe each utterance separately
      
      // run whisper recognizer
      Execution whisper = new Execution()
        .setExe(whisperExe)
        .env("HOME", getWorkingDirectory().getPath()) // for ~/.cache/whisper/...
        .arg("--model").arg(model);
      if (language != null) {
        whisper.arg("--language").arg(language);
      } else if (model.endsWith(".en")) {
        whisper.arg("--language").arg("en");
      }
      if (temperature != null) whisper.arg("--temperature").arg(""+temperature);
      if (bestOf != null) whisper.arg("--best_of").arg(""+bestOf);
      if (beamSize != null) whisper.arg("--beam_size").arg(""+beamSize);
      if (patience != null) whisper.arg("--patience").arg(""+patience);
      if (lengthPenalty != null) whisper.arg("--length_penalty").arg(""+lengthPenalty);
      if (suppressTokens != null && suppressTokens.trim().length() > 0)
        whisper.arg("--suppress_tokens").arg(suppressTokens);
      if (fp16 != null) whisper.arg("--fp16").arg(""+fp16);
      if (temperatureIncrementOnFallback != null)
        whisper.arg("--temperature_increment_on_fallback").arg(""+temperatureIncrementOnFallback);
      if (compressionRatioThreshold != null)
        whisper.arg("--compression_ratio_threshold").arg(""+compressionRatioThreshold);
      if (logprobThreshold != null) whisper.arg("--logprob_threshold").arg(""+logprobThreshold);
      if (noSpeechThreshold != null) whisper.arg("--no_speeech_threshold").arg(""+noSpeechThreshold);
      // output files to temporary directory so they can't overwrite anything important
      Path dir = Files.createTempDirectory("WhisperTranscriber");
      whisper.arg("--output_dir").arg(dir.toString());
      whisper.arg("--output_format").arg("vtt");
      // specify audio last
      whisper.arg(speech.getPath());
      setStatus("Running whisper on " + speech.getName() + " ...");
      whisper.getStderrObservers().add(err->setStatus(err));
      whisper.run();
      setStatus("Execution of whisper finished.");
      setPercentComplete(50);
      
      // parse transcript file
      File vtt = new File(dir.toFile(), IO.WithoutExtension(speech.getName()) + ".vtt"); // v2 name
      if (!vtt.exists()) vtt = new File(dir.toFile(), speech.getName() + ".vtt"); // try v1 name
      if (vtt.exists()) {
        setStatus("Parsing " + vtt.getName());
        try {
          VttSerialization deserializer = new VttSerialization();
          // default configuration
          deserializer.configure(new ParameterSet(), transcript.getSchema());
          // load transcript
          ParameterSet parameters = deserializer.load(
            Utility.OneNamedStreamArray​(vtt), transcript.getSchema());
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
          
        } finally {
          vtt.delete();
          dir.toFile().delete();
        }
      } else {
        setStatus("VTT transcript not found: " + vtt.getName());
        throw new TransformationException(this, "VTT transcript not found: " + vtt.getName());
      }
    } finally {
      setRunning(false);
    }

    return transcript;
  }

    /**
   * Transcribes all audio files in the given stream.
   * <p> Implementors may override this to provide more efficient processing in cases
   * where overhead can be saved by invoking a recogniser only once for a collection of
   * recordings, instead of one invocation per recording.
   * <p> The default implementation simply creates an empty graph and calls
   * #transcribe(File,Graph) for each speech file.
   * @param speech A stream of speech files to transcribe.
   * @param consumer A consumer for receiving the graphs once they're transcribed.
   * @throws Exception
   */
  public void transcribeFragments(Stream<File> speech, Consumer<Graph> consumer)
    throws Exception {

    setRunning(true);
    try {
      setPercentComplete(0);
      if (whisperExe == null) throw new Exception("Whisper is not installed.");
      
      List<File> wavs = speech.collect(Collectors.toList());
      
      // run whisper recognizer
      Execution whisper = new Execution()
        .setExe(whisperExe)
        .arg("--model").arg(model);
      if (language != null) {
        whisper.arg("--language").arg(language);
      } else if (model.endsWith(".en")) {
        whisper.arg("--language").arg("en");
      }
      if (temperature != null) whisper.arg("--temperature").arg(""+temperature);
      if (bestOf != null) whisper.arg("--best_of").arg(""+bestOf);
      if (beamSize != null) whisper.arg("--beam_size").arg(""+beamSize);
      if (patience != null) whisper.arg("--patience").arg(""+patience);
      if (lengthPenalty != null) whisper.arg("--length_penalty").arg(""+lengthPenalty);
      if (suppressTokens != null && suppressTokens.trim().length() > 0)
        whisper.arg("--suppress_tokens").arg(suppressTokens);
      if (fp16 != null) whisper.arg("--fp16").arg(""+fp16);
      if (temperatureIncrementOnFallback != null)
        whisper.arg("--temperature_increment_on_fallback").arg(""+temperatureIncrementOnFallback);
      if (compressionRatioThreshold != null)
        whisper.arg("--compression_ratio_threshold").arg(""+compressionRatioThreshold);
      if (logprobThreshold != null) whisper.arg("--logprob_threshold").arg(""+logprobThreshold);
      if (noSpeechThreshold != null) whisper.arg("--no_speeech_threshold").arg(""+noSpeechThreshold);
      // output files to temporary directory so they can't overwrite anything important
      Path dir = Files.createTempDirectory("WhisperTranscriber");
      whisper.arg("--output_dir").arg(dir.toString());
      whisper.arg("--output_format").arg("vtt");
      // specify audio files last
      for (File wav : wavs) whisper.arg(wav.getPath());
      setStatus("Running whisper on " + wavs.size() + " file"+(wavs.size()==1?"":"s")+" ...");
      whisper.getStderrObservers().add(err->setStatus(err));
      setPercentComplete(1);
      whisper.run();
      setStatus("Execution of whisper finished.");
      setPercentComplete(50);
      
      VttSerialization deserializer = new VttSerialization();
      // default configuration
      deserializer.configure(new ParameterSet(), getSchema());
      
      int w = 0;
      for (File wav : wavs) {      
        // parse transcript file
        File vtt = new File(dir.toFile(), IO.WithoutExtension(wav.getName()) + ".vtt"); // v2 name
        if (!vtt.exists()) vtt = new File(dir.toFile(), wav.getName() + ".vtt"); // try v1 name
        if (vtt.exists()) {
          setStatus("Parsing " + vtt.getName());
          try {
            // load transcript
            ParameterSet parameters = deserializer.load(
              Utility.OneNamedStreamArray​(vtt), getSchema());
            // default parameters
            deserializer.setParameters(parameters);
            // parse transcript
            Graph[] graphs = deserializer.deserialize();
            setPercentComplete(90);
            Graph graphFromVtt = graphs[0];        
            graphFromVtt.setId(wav.getName());
            consumer.accept(graphFromVtt);
          } finally {
            vtt.delete();
          }
          setPercentComplete(50 + w * 50 / wavs.size());
        } else {
          setStatus("VTT transcript not found: " + vtt.getName());
        }
        dir.toFile().delete();
      } // next wav file
    } finally {
      setRunning(false);
    }    
  }
    
} // end of class WhisperTranscriber
