//
// Copyright 2026 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.transcriber.whisperx;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
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
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.Transcriber;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.util.Merger;
import nzilbb.configure.ParameterSet;
import nzilbb.formatter.whisper.WhisperDeserializer;
import nzilbb.util.Execution;
import nzilbb.util.IO;

/**
 * Automatic transcriber that uses the
 * <a href="https://arxiv.org/abs/2303.00747">WhisperX</a> 
 * ASR system to perform speech-to-text and diarize it.
 * @author Robert Fromont robert@fromont.net.nz
 */
@UsesFileSystem
public class WhisperXTranscriber extends Transcriber {
  
  File whisperXExe;
  
  /**
   * Default constructor.
   */
  public WhisperXTranscriber() {      
  } // end of constructor
  
  /**
   * <b> Get the minimum version of the nzilbb.ag API supported by the serializer. </b>
   * @return Minimum version of the nzilbb.ag API supported by the serializer.
   * @see Constants#VERSION
   */
  public String getMinimumApiVersion() {
    return "1.4.0";
  }

  /**
   * Model to use. Default is "medium".
   * @see #getModel()
   * @see #setModel(String)
   */
  protected String model = "medium";
  /**
   * Getter for {@link #model}: Model to use. Default is "medium".
   * @return Model to use.
   */
  public String getModel() { return model; }
  /**
   * Setter for {@link #model}: Model to use.
   * @param newModel Model to use.
   */
  public WhisperXTranscriber setModel(String newModel) { model = newModel; return this; }
  
  /**
   * Directory for finding the model.
   * @see #getModelDir()
   * @see #setModelDir(String)
   */
  protected String modelDir;
  /**
   * Getter for {@link #modelDir}: Directory for finding the model.
   * @return Directory for finding the model.
   */
  public String getModelDir() { return modelDir; }
  /**
   * Setter for {@link #modelDir}: Directory for finding the model.
   * @param newModelDir Directory for finding the model.
    */
  public WhisperXTranscriber setModelDir(String newModelDir) { modelDir = newModelDir; return this; }
  
  /**
   * Language spoken in the audio; specify null to use the language of
   * the given graph, or fall back to performing language detection.
   * @see #getLanguage()
   * @see #setLanguage(String)
   */
  protected String language;
  /**
   * Getter for {@link #language}: Language spoken in the audio.
   * @return Language spoken in the audio.
   */
  public String getLanguage() { return language; }
  /**
   * Setter for {@link #language}: Language spoken in the audio;
   * specify null to use the language of the given graph, or fall back
   * to performing language detection.
   * @param newLanguage Language spoken in the audio.
   */
  public WhisperXTranscriber setLanguage(String newLanguage) { language = newLanguage; return this; }
  
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
  public WhisperXTranscriber setTemperature(Double newTemperature) { temperature = newTemperature; return this; }
  
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
  public WhisperXTranscriber setBestOf(Integer newBestOf) { bestOf = newBestOf; return this; }

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
  public WhisperXTranscriber setBeamSize(Integer newBeamSize) { beamSize = newBeamSize; return this; }

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
  public WhisperXTranscriber setPatience(Double newPatience) { patience = newPatience; return this; }

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
  public WhisperXTranscriber setLengthPenalty(Integer newLengthPenalty) { lengthPenalty = newLengthPenalty; return this; }

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
  public WhisperXTranscriber setFp16(Boolean newFp16) { fp16 = newFp16; return this; }

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
  public WhisperXTranscriber setTemperatureIncrementOnFallback(Double newTemperatureIncrementOnFallback) { temperatureIncrementOnFallback = newTemperatureIncrementOnFallback; return this; }

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
  public WhisperXTranscriber setCompressionRatioThreshold(Double newCompressionRatioThreshold) { compressionRatioThreshold = newCompressionRatioThreshold; return this; }

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
  public WhisperXTranscriber setLogprobThreshold(Double newLogprobThreshold) { logprobThreshold = newLogprobThreshold; return this; }

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
  public WhisperXTranscriber setNoSpeechThreshold(Double newNoSpeechThreshold) { noSpeechThreshold = newNoSpeechThreshold; return this; }
  
  /**
   * Specifies the length (in seconds) of audio processed at once. 30
   * seconds is a balanced default for processing time
   * vs. accuracy. Smaller chunks can yield more precise boundaries,
   * while larger chunks can speed up processing. 
   * @see #getChunkSize()
   * @see #setChunkSize(Integer)
   */
  protected Integer chunkSize;
  /**
    * Getter for {@link #chunkSize}: Specifies the length (in seconds)
    * of audio processed at once. 
    * @return Specifies the length (in seconds) of audio processed at once. 
    */
  public Integer getChunkSize() { return chunkSize; }
  /**
   * Setter for {@link #chunkSize}: Specifies the length (in seconds)
   * of audio processed at once. 30 seconds is a balanced default for
   * processing time vs. accuracy. Smaller chunks can yield more
   * precise boundaries, while larger chunks can speed up processing. 
   * @param newChunkSize Specifies the length (in seconds) of audio processed at once.
   */
  public WhisperXTranscriber setChunkSize(Integer newChunkSize) { chunkSize = newChunkSize; return this; }
  
  /**
   * Whether to assign speaker labels to utterances (diarization) or
   * not. The defauls is <tt>true</tt>.
   * @see #getDiarize()
   * @see #setDiarize(Boolean)
   * @see #setHuggingFaceToken()
   */
  protected Boolean diarize = Boolean.TRUE;
  /**
   * Getter for {@link #diarize}: Whether to assign speaker labels to
   * utterances (diarization) or not. The defauls is <tt>true</tt>.
   * @return Whether to assign speaker labels to utterances (diarization) or not.
   */
  public Boolean getDiarize() { return diarize; }
  /**
   * Setter for {@link #diarize}: Whether to assign speaker labels to
   * utterances (diarization) or not. 
   * @param newDiarize Whether to assign speaker labels to utterances (diarization) or not.
   */
  public WhisperXTranscriber setDiarize(Boolean newDiarize) { diarize = newDiarize; return this; }

  /**
   * Diarization requires models from huggingface.co, which requires
   * an authentication token. 
   * <p> See <a href="https://huggingface.co/docs/hub/security-tokens">
   *  https://huggingface.co/docs/hub/security-tokens</a>
   * <p> An alternative to setting the token programmatically is to
   * use the HUGGINGFACE_TOKEN environment variable.
   * @see #getHuggingFaceToken()
   * @see #setHuggingFaceToken(String)
   */
  protected String huggingFaceToken;
  /**
   * Getter for {@link #huggingFaceToken}: Authentication token for
   * downloading diarization models.
   * @return Authentication token for downloading diarization models.
   */
  public String getHuggingFaceToken() { return huggingFaceToken; }
  /**
   * Setter for {@link #huggingFaceToken}: Diarization requires models
   * from huggingface.co, which requires an authentication token.
   * <p> See <a href="https://huggingface.co/docs/hub/security-tokens">
   *  https://huggingface.co/docs/hub/security-tokens</a>
   * <p> An alternative to setting the token programmatically is to
   * use the HUGGINGFACE_TOKEN environment variable.
   * @param newHuggingFaceToken Authentication token for
   * downloading diarization models.
   */
  public WhisperXTranscriber setHuggingFaceToken(String newHuggingFaceToken) { huggingFaceToken = newHuggingFaceToken; return this; }
  
  /**
   * Minimum number of speakers for diarization.
   * @see #getMinSpeakers()
   * @see #setMinSpeakers(Integer)
   */
  protected Integer minSpeakers;
  /**
   * Getter for {@link #minSpeakers}: Minimum number of speakers for diarization.
   * @return Minimum number of speakers for diarization.
   */
  public Integer getMinSpeakers() { return minSpeakers; }
  /**
   * Setter for {@link #minSpeakers}: Minimum number of speakers for diarization.
   * @param newMinSpeakers Minimum number of speakers for diarization.
   */
  public WhisperXTranscriber setMinSpeakers(Integer newMinSpeakers) { minSpeakers = newMinSpeakers; return this; }

  /**
   * Maximum number of speakers for diarization.
   * @see #getMaxSpeakers()
   * @see #setMaxSpeakers(Integer)
   */
  protected Integer maxSpeakers;
  /**
   * Getter for {@link #maxSpeakers}: Maximum number of speakers for diarization.
   * @return Maximum number of speakers for diarization.
   */
  public Integer getMaxSpeakers() { return maxSpeakers; }
  /**
   * Setter for {@link #maxSpeakers}: Maximum number of speakers for diarization.
   * @param newMaxSpeakers Maximum number of speakers for diarization.
   */
  public WhisperXTranscriber setMaxSpeakers(Integer newMaxSpeakers) { maxSpeakers = newMaxSpeakers; return this; }

  /**
   * For postprocessing of the transcript, the minimum inter-word
   * pause length, in seconds, before a pause counts as a 'short
   * pause'. The default value is 0.35. 
   * @see #minMediumPauseLength
   * @see #minLongPauseLength
   * @see #shortPauseLabel
   * @see #getMinShortPauseLength()
   * @see #setMinShortPauseLength(Double)
   */
  protected Double minShortPauseLength = 0.35;
  /**
   * Getter for {@link #minShortPauseLength}: The minimum inter-word
   * pause length, in seconds, before a pause counts as a 'short
   * pause'. The default values is 0.35.
   * @return The minimum inter-word pause length, in seconds, before a
   * pause counts as a 'short pause'. 
   * @see #getShortPauseLabel()
   */
  public Double getMinShortPauseLength() { return minShortPauseLength; }
  /**
   * Setter for {@link #minShortPauseLength}: The minimum inter-word
   * pause length, in seconds, before a pause counts as a 'short
   * pause'.
   * @param newMinShortPauseLength The minimum inter-word pause
   * length, in seconds, before a pause counts as a 'short pause'. 
   */
  public WhisperXTranscriber setMinShortPauseLength(Double newMinShortPauseLength) { minShortPauseLength = newMinShortPauseLength; return this; }
  
  /**
   * For postprocessing of the transcript, the minimum inter-word
   * pause length, in seconds, before a pause counts as a 'medium
   * pause'. The default value is 0.7. 
   * @see #minShortPauseLength
   * @see #minLongPauseLength
   * @see #mediumPauseLabel
   * @see #getMinMediumPauseLength()
   * @see #setMinMediumPauseLength(Double)
   */
  protected Double minMediumPauseLength = 0.7;
  /**
   * Getter for {@link #minMediumPauseLength}: The minimum inter-word
   * pause length, in seconds, before a pause counts as a 'medium pause'.
   * The default value is 0.7. 
   * @return The minimum inter-word pause length, in seconds, before
   * a pause counts as a 'medium pause'. 
   * @see #getMediumPauseLabel()
   */
  public Double getMinMediumPauseLength() { return minMediumPauseLength; }
  /**
   * Setter for {@link #minMediumPauseLength}: The minimum inter-word
   * pause length, in seconds, before a pause counts as a 'medium pause'. 
   * @param newMinMediumPauseLength The minimum inter-word pause
   * length, in seconds, before a pause counts as a 'medium pause'. 
   */
  public WhisperXTranscriber setMinMediumPauseLength(Double newMinMediumPauseLength) { minMediumPauseLength = newMinMediumPauseLength; return this; }
  
  /**
   * For postprocessing of the transcript, the minimum inter-word
   * pause length, in seconds, before a pause counts as a 'long
   * pause'. The default value is 1.4. 
   * @see #minShortPauseLength
   * @see #minMediumPauseLength
   * @see #longPauseLabel
   * @see #getMinLongPauseLength()
   * @see #setMinLongPauseLength(Double)
   */
  protected Double minLongPauseLength = 1.4;
  /**
   * Getter for {@link #minLongPauseLength}: The minimum inter-word
   * pause length, in seconds, before a pause counts as a 'long pause'.
   * The default value is 1.4. 
   * @return The minimum inter-word pause length, in seconds, before a
   * pause counts as a 'long pause'. 
   * @see #getLongPauseLabel()
   */
  public Double getMinLongPauseLength() { return minLongPauseLength; }
  /**
   * Setter for {@link #minLongPauseLength}: The minimum inter-word
   * pause length, in seconds, before a pause counts as a 'long pause'. 
   * @param newMinLongPauseLength The minimum inter-word pause length,
   * in seconds, before a pause counts as a 'long pause'. 
   */
  public WhisperXTranscriber setMinLongPauseLength(Double newMinLongPauseLength) { minLongPauseLength = newMinLongPauseLength; return this; }

  /**
   * For postprocessing of the transcript, the string to append to the
   * word before a short pause. If an inter-word pause has a duration between
   * {@link #getMinShortPauseLength()} and {@link #getMinMediumPauseLength()}, then
   * the word before the pause will have this string appended to its
   * label (after a space).
   * @see #getShortPauseLabel()
   * @see #setShortPauseLabel(String)
   */
  protected String shortPauseLabel = "(.)";
  /**
   * Getter for {@link #shortPauseLabel}: The string to append to the
   * word before a short pause.  
   * If an inter-word pause has a duration between
   * {@link #getMinShortPauseLength()} and {@link #getMinMediumPauseLength()}, then
   * the word before the pause will have this string appended to its
   * label (after a space).
   * If null or empty, no short pauses are labelled.
   * @return The string to append to the word before a short pause.
   */
  public String getShortPauseLabel() { return shortPauseLabel; }
  /**
   * Setter for {@link #shortPauseLabel}: The string to append to the
   * word before a short pause. 
   * If an inter-word pause has a duration between
   * {@link #getMinShortPauseLength()} and {@link #getMinMediumPauseLength()}, then
   * the word before the pause will have this string appended to its
   * label (after a space).
   * @param newShortPauseLabel The string to append to the word before a short pause.
   */
  public WhisperXTranscriber setShortPauseLabel(String newShortPauseLabel) { shortPauseLabel = newShortPauseLabel; return this; }

  /**
   * For postprocessing of the transcript, the string to append to the
   * word before a medium pause. If an inter-word pause has a duration between
   * {@link #getMinMediumPauseLength()} and {@link #getMinLongPauseLength()}, then
   * the word before the pause will have this string appended to its
   * label (after a space).
   * @see #getMediumPauseLabel()
   * @see #setMediumPauseLabel(String)
   */
  protected String mediumPauseLabel = "(..)";
  /**
   * Getter for {@link #mediumPauseLabel}: The string to append to the
   * word before a medium pause. 
   * If an inter-word pause has a duration between
   * {@link #getMinMediumPauseLength()} and {@link #getMinLongPauseLength()}, then
   * the word before the pause will have this string appended to its
   * label (after a space).
   * If null or empty, no medium pauses are labelled.
   * @return The string to append to the word before a medium pause.
   */
  public String getMediumPauseLabel() { return mediumPauseLabel; }
  /**
   * Setter for {@link #mediumPauseLabel}: The string to append to the
   * word before a medium pause. 
   * If an inter-word pause has a duration between
   * {@link #getMinMediumPauseLength()} and {@link #getMinLongPauseLength()}, then
   * the word before the pause will have this string appended to its
   * label (after a space).
   * @param newMediumPauseLabel The string to append to the word before a medium pause.
   */
  public WhisperXTranscriber setMediumPauseLabel(String newMediumPauseLabel) { mediumPauseLabel = newMediumPauseLabel; return this; }
  
  /**
   * For postprocessing of the transcript, the string to append to the
   * word before a long pause. If an inter-word pause has a duration longer than
   * {@link #getMinLongPauseLength()}, then the word before the pause
   * will have this string appended to its 
   * label (after a space).
   * @see #getLongPauseLabel()
   * @see #setLongPauseLabel(String)
   */
  protected String longPauseLabel = "(...)";
  /**
   * Getter for {@link #longPauseLabel}: The string to append to the
   * word before a long pause. 
   * If an inter-word pause has a duration longer than
   * {@link #getMinLongPauseLength()}, then the word before the pause
   * will have this string appended to its 
   * If null or empty, no long pauses are labelled.
   * @return The string to append to the word before a long pause.
   */
  public String getLongPauseLabel() { return longPauseLabel; }
  /**
   * Setter for {@link #longPauseLabel}: The string to append to the
   * word before a long pause. 
   * If an inter-word pause has a duration longer than
   * {@link #getMinLongPauseLength()}, then the word before the pause
   * will have this string appended to its 
   * @param newLongPauseLabel The string to append to the word before a long pause.
   */
  public WhisperXTranscriber setLongPauseLabel(String newLongPauseLabel) { longPauseLabel = newLongPauseLabel; return this; }
  
  /**
   * For postprocessing of the transcript, maximum utterance duration
   * to target (seconds). Longer utterances will be split on longer
   * inter-word pauses. 15s by default.
   * @see #getMaxUtteranceDuration()
   * @see #setMaxUtteranceDuration(Double)
   */
  protected Double maxUtteranceDuration = 15.0;
  /**
   * Getter for {@link #maxUtteranceDuration}: Maximum utterance
   * duration to target. Longer utterances will be split on longer
   * inter-word pauses. 15s by default.
   * @return Maximum utterance duration to target (seconds). Longer utterances
   * will be split on longer inter-word pauses. 
   */
  public Double getMaxUtteranceDuration() { return maxUtteranceDuration; }
  /**
   * Setter for {@link #maxUtteranceDuration}: Maximum utterance
   * duration to target. Longer utterances will be split on longer
   * inter-word pauses.
   * @param newMaxUtteranceDuration Maximum utterance duration to
   * target. Longer utterances will be split on longer inter-word
   * pauses. 
   */
  public WhisperXTranscriber setMaxUtteranceDuration(Double newMaxUtteranceDuration) { maxUtteranceDuration = newMaxUtteranceDuration; return this; }
  
  /**
   * For postprocessing of the transcript, maximum number of seconds
   * to subtract from the start time and add to the end time of each
   * utterance, to allow for alignment errors of first/last word in
   * each segment. The default is 0.5s.
   * @see #getUtterancePadding()
   * @see #setUtterancePadding(Double)
   */
  protected Double utterancePadding = 0.5;
  /**
   * Getter for {@link #utterancePadding}: Maximum number of seconds to
   * subtract from the start time and add to the end time of each utterance,
   * to allow for alignment errors of first/last word in each segment.
   * The default is 0.5s.
   * @return Maximum number of seconds to subtract from the start time and
   * add to the end time of each utterance, to allow for alignment errors
   * of first/last word in each segment.
   */
  public Double getUtterancePadding() { return utterancePadding; }
  /**
   * Setter for {@link #utterancePadding}: Maximum number of seconds to
   * subtract from the start time and add to the end time of each utterance,
   * to allow for alignment errors of first/last word in each segment.
   * @param newUtterancePadding Maximum number of seconds to subtract
   * from the start time and add to the end time of each utterance, to
   * allow for alignment errors of first/last word in each segment.
   */
  public WhisperXTranscriber setUtterancePadding(Double newUtterancePadding) { utterancePadding = newUtterancePadding; return this; }
  
  /**
   * Layer for the document language.
   * @see #getLanguageLayer()
   * @see #setLanguageLayer(Layer)
   */
  protected Layer languageLayer;
  /**
   * Getter for {@link #languageLayer}: Layer for the document language.
   * @return Layer for the document language.
   */
  public Layer getLanguageLayer() { return languageLayer; }
  /**
   * Setter for {@link #languageLayer}: Layer for the document language.
   * @param newLanguageLayer Layer for the document language.
   */
  public WhisperXTranscriber setLanguageLayer(Layer newLanguageLayer) { languageLayer = newLanguageLayer; return this; }

  private File venv = null;
  /**
   * Setter for {@link #workingDirectory}: A persistent directory in which files can be saved
   * and accessed. 
   * @param directory A persistent directory in which files can be saved and accessed.
   */
  @Override public Annotator setWorkingDirectory(File directory) {
    super.setWorkingDirectory(directory);
    venv = new File(directory, "whisperx-env");
    return this;
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
    setPercentComplete(10);

    whisperXExe = Execution.Which("whisperx", venv);

    // try to install whisperx if it isn't installed...
    if (whisperXExe == null) {
      
      // can we run Python?
      File python3Path = Execution.Which("python3");
      if (python3Path == null || python3Path.length() == 0) {
        throw new InvalidConfigurationException(
          this,
          "Cannot run python3; please install Python and ensure that it's on the search path.");
      }
      
      // can we use environments
      Execution cmd = new Execution()
        .setExe(python3Path)
        .arg("-m").arg("venv").arg(venv.getPath())
        .setWorkingDirectory(getWorkingDirectory());
      cmd.getStdoutObservers().add(m->setStatus(m));
      cmd.getStderrObservers().add(m->setStatus(m));
      cmd.run();
      if (cmd.getProcess().exitValue() > 0) {
        setStatus("Cannot run 'venv' module ("
                  + cmd.getProcess().exitValue() + ") attempting to install it...");
        
        // try installing python3-all-venv (we might be running in Docker, where we're allowed)
        cmd = new Execution()
          .setExe("apt")
          .arg("install").arg("-y").arg("python3-all-venv");
        cmd.getStdoutObservers().add(m->setStatus(m));
        cmd.getStderrObservers().add(m->setStatus(m));
        cmd.run();
        if (cmd.getProcess().exitValue() == 0) { // seems to have succeeded
          // try running venv again
          setStatus("Trying 'venv' again...");
          cmd = new Execution()
            .setExe(python3Path)
            .arg("-m").arg("venv").arg(venv.getPath())
            .setWorkingDirectory(getWorkingDirectory());
          cmd.getStdoutObservers().add(m->setStatus(m));
          cmd.getStderrObservers().add(m->setStatus(m));
          cmd.run();
        }
        if (cmd.getProcess().exitValue() > 0) {
          setStatus("Cannot install 'venv' module (" + cmd.getProcess().exitValue() + ")");
          throw new InvalidConfigurationException(
            this, "Cannot run 'venv' module; Please install support for python environments."
            +"\ne.g. on Ubuntu: sudo apt install python3.10-venv");
        }
      }
      setStatus(cmd.getInput().toString());
    
      // install whisperx
      File cacheDir = new File(getWorkingDirectory(), "cache");
      if (!cacheDir.exists() && !cacheDir.mkdir()) {
        setStatus("Could not create pip cache directory: "+cacheDir.getPath());
      }
      cmd = new Execution()
        .setVenv(venv)
        .setExe("pip")
        .arg("install")
        .arg("--cache-dir="+cacheDir.getPath())
        .arg("whisperx");
      cmd.getStdoutObservers().add(m->setStatus(m));
      cmd.getStderrObservers().add(m->setStatus(m));
      cmd.run();
      if (cmd.getProcess() == null) {
        setStatus("Cannot install whisperx: " + cmd.getError());
        throw new InvalidConfigurationException(
          this, "Cannot install whisperx: " + cmd.getError());
      }
      if (cmd.getProcess().exitValue() > 0) {
        setStatus("Cannot install whisperx - status: " + cmd.getProcess().exitValue());
        throw new InvalidConfigurationException(
          this, "Cannot install whisperx: " + cmd.getProcess().exitValue());
      }
    }
    
    // set the executable (which is now hopefully available)
    whisperXExe = Execution.Which("whisperx", venv);
    
    setRunning(false);
    setPercentComplete(100);
  }

  /**
   * If whisperx is running, kills the process.
   */
  @Override public void cancel() {
    python.stream()
      .filter(py -> py.getProcess() != null)
      .forEach(py -> {
          if (!isCancelling()) { // first time
            py.getProcess().destroy();
          } else { // multiple cancel requests, kill -9
            py.getProcess().destroyForcibly();
          }
        });
    super.cancel();
  } // end of cancel()

  /**
   *  Runs any processing required to uninstall the transcriber. 
   */
  @Override
  public void uninstall() { }
  
  // could be multiple scripts executing simultaneously, if they cancel, we need to kill them
  HashSet<Execution> python = new HashSet<Execution>();
  
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
      if (whisperXExe == null) throw new Exception("WhisperX is not installed.");
      
      // run whisperX recognizer
      Execution whisperX = new Execution()
        .setVenv(venv)
        .setExe(whisperXExe)
        .env("HOME", getWorkingDirectory().getPath()) // for ~/.cache/whisper/...
        .arg("--model").arg(model);

      // Whisper parameters...
      if (language != null) {
        whisperX.arg("--language").arg(language);
      } else if (languageLayer != null && transcript.first(languageLayer.getId()) != null) {
        setStatus("Using given transcript language: "
                  + transcript.first(languageLayer.getId()));
        whisperX.arg("--language").arg(transcript.first(languageLayer.getId()).getLabel());
      } else if (model.endsWith(".en")) {
        whisperX.arg("--language").arg("en");
      }
      if (temperature != null) whisperX.arg("--temperature").arg(""+temperature);
      if (bestOf != null) whisperX.arg("--best_of").arg(""+bestOf);
      if (beamSize != null) whisperX.arg("--beam_size").arg(""+beamSize);
      if (patience != null) whisperX.arg("--patience").arg(""+patience);
      if (lengthPenalty != null) whisperX.arg("--length_penalty").arg(""+lengthPenalty);
      if (fp16 != null) whisperX.arg("--fp16").arg(""+fp16);
      if (temperatureIncrementOnFallback != null)
        whisperX.arg("--temperature_increment_on_fallback")
          .arg(""+temperatureIncrementOnFallback);
      if (compressionRatioThreshold != null)
        whisperX.arg("--compression_ratio_threshold").arg(""+compressionRatioThreshold);
      if (logprobThreshold != null)
        whisperX.arg("--logprob_threshold").arg(""+logprobThreshold);
      if (noSpeechThreshold != null)
        whisperX.arg("--no_speeech_threshold").arg(""+noSpeechThreshold);

      // WhisperX-specific parameters
      if (chunkSize != null) whisperX.arg("--chunk_size").arg(""+chunkSize);
      if (diarize != null && diarize) {
        whisperX.arg("--diarize");
        // a hugging face token is required for downloading the diarization models
        if (huggingFaceToken == null) { // try getting it from the environment
          huggingFaceToken = System.getenv("HUGGINGFACE_TOKEN");
        }
        if (huggingFaceToken != null) {
          whisperX.arg("--hf_token").arg(huggingFaceToken);
        }
        if (minSpeakers != null) whisperX.arg("--min_speakers").arg(""+minSpeakers);
        if (maxSpeakers != null) whisperX.arg("--max_speakers").arg(""+maxSpeakers);
      }
      
      // output files to temporary directory so they can't overwrite anything important
      Path dir = Files.createTempDirectory("WhisperXTranscriber");
      whisperX.arg("--output_dir").arg(dir.toString());
      whisperX.arg("--output_format").arg("json");
      // specify audio last
      whisperX.arg(speech.getPath());
      setStatus("Running whisperX on " + speech.getName() + " ...");
      setStatus("" + whisperX.getExe() + " " + whisperX.getArguments());
      whisperX.getStderrObservers().add(err->setStatus(err));
      whisperX.run();
      setStatus("Execution of whisperX finished.");
      setPercentComplete(50);
      
      // parse transcript file
      File json = new File(dir.toFile(), IO.WithoutExtension(speech.getName()) + ".json");
      if (json.exists()) {
        setStatus("Parsing " + json.getName());
        try {
          WhisperDeserializer deserializer = new WhisperDeserializer();
          ParameterSet config = deserializer.configure(
            new ParameterSet(), transcript.getSchema());
          // post-processing parameters
          config.get("minShortPauseLength").setValue(getMinShortPauseLength());
          config.get("minMediumPauseLength").setValue(getMinMediumPauseLength());
          config.get("minLongPauseLength").setValue(getMinLongPauseLength());
          config.get("shortPauseLabel").setValue(getShortPauseLabel());
          config.get("mediumPauseLabel").setValue(getMediumPauseLabel());
          config.get("longPauseLabel").setValue(getLongPauseLabel());
          config.get("maxUtteranceDuration").setValue(getMaxUtteranceDuration());
          config.get("utterancePadding").setValue(getUtterancePadding());
          config.get("languageLayer").setValue(getLanguageLayer());
          deserializer.configure(config, transcript.getSchema());
          
          // load transcript
          ParameterSet parameters = deserializer.load(
            Utility.OneNamedStreamArray​(json), transcript.getSchema());
          // default parameters
          deserializer.setParameters(parameters);
          // parse transcript
          Graph[] graphs = deserializer.deserialize();
          setPercentComplete(90);
          Graph graphFromJson = graphs[0];        
          graphFromJson.setId(transcript.getId());
          graphFromJson.commit();
          
          // merge into given transcript
          Merger merger = new Merger(graphFromJson);          
          merger.transform(transcript);
          setPercentComplete(100);
          
        } finally {
          json.delete();
          dir.toFile().delete();
        }
      } else {
        setStatus("JSON transcript not found: " + json.getName());
        throw new TransformationException(
          this, "JSON transcript not found: " + json.getName());
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
      if (whisperXExe == null) throw new Exception("WhisperX is not installed.");
      
      List<File> wavs = speech.collect(Collectors.toList());
      
      // run whisper recognizer
      // run whisperX recognizer
      Execution whisperX = new Execution()
        .setVenv(venv)
        .setExe(whisperXExe)
        .env("HOME", getWorkingDirectory().getPath()) // for ~/.cache/whisper/...
        .arg("--model").arg(model);

      // Whisper parameters...
      if (language != null) {
        whisperX.arg("--language").arg(language);
      } else if (model.endsWith(".en")) {
        whisperX.arg("--language").arg("en");
      }
      if (temperature != null) whisperX.arg("--temperature").arg(""+temperature);
      if (bestOf != null) whisperX.arg("--best_of").arg(""+bestOf);
      if (beamSize != null) whisperX.arg("--beam_size").arg(""+beamSize);
      if (patience != null) whisperX.arg("--patience").arg(""+patience);
      if (lengthPenalty != null) whisperX.arg("--length_penalty").arg(""+lengthPenalty);
      if (fp16 != null) whisperX.arg("--fp16").arg(""+fp16);
      if (temperatureIncrementOnFallback != null)
        whisperX.arg("--temperature_increment_on_fallback")
          .arg(""+temperatureIncrementOnFallback);
      if (compressionRatioThreshold != null)
        whisperX.arg("--compression_ratio_threshold").arg(""+compressionRatioThreshold);
      if (logprobThreshold != null)
        whisperX.arg("--logprob_threshold").arg(""+logprobThreshold);
      if (noSpeechThreshold != null)
        whisperX.arg("--no_speeech_threshold").arg(""+noSpeechThreshold);

      // WhisperX-specific parameters
      if (chunkSize != null) whisperX.arg("--chunk_size").arg(""+chunkSize);
      if (diarize != null && diarize) {
        whisperX.arg("--diarize");
        if (minSpeakers != null) whisperX.arg("--min_speakers").arg(""+minSpeakers);
        if (maxSpeakers != null) whisperX.arg("--max_speakers").arg(""+maxSpeakers);
      }
      
      // output files to temporary directory so they can't overwrite anything important
      Path dir = Files.createTempDirectory("WhisperXTranscriber");
      whisperX.arg("--output_dir").arg(dir.toString());
      whisperX.arg("--output_format").arg("json");
      // specify audio files last
      for (File wav : wavs) whisperX.arg(wav.getPath());
      setStatus("Running whisperX on " + wavs.size() + " file"+(wavs.size()==1?"":"s")+" ...");
      whisperX.getStderrObservers().add(err->setStatus(err));
      setPercentComplete(1);
      whisperX.run();
      setStatus("Execution of whisper finished.");
      setPercentComplete(50);
      
      WhisperDeserializer deserializer = new WhisperDeserializer();
      ParameterSet config = deserializer.configure(
        new ParameterSet(), getSchema());
      // post-processing parameters
      config.get("minShortPauseLength").setValue(getMinShortPauseLength());
      config.get("minMediumPauseLength").setValue(getMinMediumPauseLength());
      config.get("minLongPauseLength").setValue(getMinLongPauseLength());
      config.get("shortPauseLabel").setValue(getShortPauseLabel());
      config.get("mediumPauseLabel").setValue(getMediumPauseLabel());
      config.get("longPauseLabel").setValue(getLongPauseLabel());
      config.get("maxUtteranceDuration").setValue(getMaxUtteranceDuration());
      config.get("utterancePadding").setValue(getUtterancePadding());
      config.get("languageLayer").setValue(getLanguageLayer());
      
      int w = 0;
      for (File wav : wavs) {      
        // parse transcript file
        File json = new File(dir.toFile(), IO.WithoutExtension(wav.getName()) + ".json");
        if (json.exists()) {
          setStatus("Parsing " + json.getName());
          try {
            // load transcript
            ParameterSet parameters = deserializer.load(
              Utility.OneNamedStreamArray​(json), getSchema());
            // default parameters
            deserializer.setParameters(parameters);
            // parse transcript
            Graph[] graphs = deserializer.deserialize();
            setPercentComplete(90);
            Graph graphFromJson = graphs[0];        
            graphFromJson.setId(wav.getName());
            consumer.accept(graphFromJson);
          } finally {
            json.delete();
          }
          setPercentComplete(50 + w * 50 / wavs.size());
        } else {
          setStatus("JSON transcript not found: " + json.getName());
        }
        dir.toFile().delete();
      } // next wav file
    } finally {
      setRunning(false);
    }    
  }
    
} // end of class WhisperXTranscriber
