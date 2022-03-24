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
package nzilbb.annotator.htk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.script.*;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.UsesGraphStore;
import nzilbb.ag.serialize.GraphDeserializer;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.util.DefaultOffsetGenerator;
import nzilbb.ag.util.Merger;
import nzilbb.configure.ParameterSet;
import nzilbb.encoding.CMU2DISC;
import nzilbb.encoding.DISC2CMU;
import nzilbb.encoding.DISC2HTK;
import nzilbb.encoding.HTK2DISC;
import nzilbb.encoding.PhonemeTranslator;
import nzilbb.formatter.htk.mlf.MlfDeserializer;
import nzilbb.util.Execution;
import nzilbb.util.IO;

/**
 * Annotator that uses HTK to force-align given graphs.
 * @author Robert Fromont robert@fromont.net.nz
 */
@UsesFileSystem @UsesGraphStore
public class HTKAligner extends Annotator {
  /** Get the minimum version of the nzilbb.ag API supported by the annotator.*/
  public String getMinimumApiVersion() { return "1.0.5"; }
  
  /**
   * Path to HTK tools.
   * @see #getHtkPath()
   * @see #setHtkPath(String)
   */
  protected String htkPath;
  /**
   * Getter for {@link #htkPath}: Path to HTK tools.
   * @return Path to HTK tools.
   */
  public String getHtkPath() { return htkPath; }
  /**
   * Setter for {@link #htkPath}: Path to HTK tools.
   * @param newHtkPath Path to HTK tools.
   */
  public HTKAligner setHtkPath(String newHtkPath) { htkPath = newHtkPath; return this; }
  
  /**
   * User ID for the HTK website. This may be used for downloading/installing HTK.
   * @see #getHtkUserId()
   * @see #setHtkUserId(String)
   */
  protected String htkUserId;
  /**
   * Getter for {@link #htkUserId}: User ID for the HTK website.
   * @return User ID for the HTK website.
   */
  public String getHtkUserId() { return htkUserId; }
  /**
   * Setter for {@link #htkUserId}: User ID for the HTK website.
   * @param newHtkUserId User ID for the HTK website.
    */
  public HTKAligner setHtkUserId(String newHtkUserId) { htkUserId = newHtkUserId; return this; }
  
  /**
   * Password for the HTK website. This may be used for downloading/installing HTK.
   * @see #getHtkPassword()
   * @see #setHtkPassword(String)
   */
  protected String htkPassword;
  /**
   * Getter for {@link #htkPassword}: Password for the HTK website.
   * @return Password for the HTK website.
   */
  public String getHtkPassword() { return htkPassword; }
  /**
   * Setter for {@link #htkPassword}: Password for the HTK website.
   * @param newHtkPassword Password for the HTK website.
   */
  public HTKAligner setHtkPassword(String newHtkPassword) { htkPassword = newHtkPassword; return this; }
  
  /**
   * Characters that mark pauses in speech in the transcript.
   * @see #getPauseMarkers()
   * @see #setPauseMarkers(String)
   */
  protected String pauseMarkers = "-";
  /**
   * Getter for {@link #pauseMarkers}: Characters that mark pauses in speech in the transcript.
   * @return Characters that mark pauses in speech in the transcript.
   */
  public String getPauseMarkers() { return pauseMarkers; }
  /**
   * Setter for {@link #pauseMarkers}: Characters that mark pauses in speech in the transcript.
   * @param newPauseMarkers Characters that mark pauses in speech in the transcript.
   */
  public HTKAligner setPauseMarkers(String newPauseMarkers) { pauseMarkers = newPauseMarkers; return this; }

  /**
   * Patterns on the noise layer that HTK should model for.
   * @see #getNoisePatterns()
   * @see #setNoisePatterns(String)
   */
  protected String noisePatterns = "laugh.* unclear .*noise.*";
  /**
   * Getter for {@link #noisePatterns}: Patterns on the noise layer that HTK should model for.
   * @return Patterns on the noise layer that HTK should model for.
   */
  public String getNoisePatterns() { return noisePatterns; }
  /**
   * Setter for {@link #noisePatterns}: Patterns on the noise layer that HTK should model for.
   * @param newNoisePatterns Patterns on the noise layer that HTK should model for.
   */
  public HTKAligner setNoisePatterns(String newNoisePatterns) { noisePatterns = newNoisePatterns; return this; }
  
  /**
   * The layer ID for marking main participants.
   * @see #getMainParticipantLayerId()
   * @see #setMainParticipantLayerId(String)
   */
  protected String mainParticipantLayerId = "main_participant";
  /**
   * Getter for {@link #mainParticipantLayerId}: The layer ID for marking main participants.
   * @return The layer ID for marking main participants.
   */
  public String getMainParticipantLayerId() { return mainParticipantLayerId; }
  /**
   * Setter for {@link #mainParticipantLayerId}: The layer ID for marking main participants.
   * @param newMainParticipantLayerId The layer ID for marking main participants.
   */
  public HTKAligner setMainParticipantLayerId(String newMainParticipantLayerId) { mainParticipantLayerId = newMainParticipantLayerId; return this; }
  
  /**
   * How to group main-participant utterances for training, when a single transcript is
   * selected for alignment. Possible value are:
   * <dl>
   *  <dt> Speaker </dt>    <dd> All utterances by main-participant in the whole graph store
   *                             are collected together for training before alignment. </dd>
   *  <dt> Transcript </dt> <dd> Only the utterances within the selected transcript are
   *                             used for training. </dd>
   * </dl>
   * @see #getMainUtteranceGrouping()
   * @see #setMainUtteranceGrouping(String)
   */
  protected String mainUtteranceGrouping = "Speaker";
  /**
   * Getter for {@link #mainUtteranceGrouping}: How to group main-participant utterances
   * for training, when a single transcript is selected for alignment. 
   * @return How to group main-participant utterances for training, when a single
   * transcript is selected for alignment.
   */
  public String getMainUtteranceGrouping() { return mainUtteranceGrouping; }
  /**
   * Setter for {@link #mainUtteranceGrouping}: How to group main-participant utterances
   * for training, when a single transcript is selected for alignment. 
   * @param newMainUtteranceGrouping How to group main-participant utterances for
   * training, when a single transcript is selected for alignment.
   */
  public HTKAligner setMainUtteranceGrouping(String newMainUtteranceGrouping) { mainUtteranceGrouping = newMainUtteranceGrouping; return this; }

  /**
   * How to group non-main-participant utterances for training, when a single transcript
   *  is selected for alignment. Possible value are:
   *  <dt> No Aligned </dt> <dd> Non-main-participant utterances are not aligned. </dd>
   *  <dt> Transcript </dt> <dd> Only the utterances within the selected transcript are
   *                             used for training. </dd>
   * @see #getOtherUtteranceGrouping()
   * @see #setOtherUtteranceGrouping(String)
   */
  protected String otherUtteranceGrouping = "Not Aligned";
  /**
   * Getter for {@link #otherUtteranceGrouping}: How to group non-main-participant
   * utterances for training, when a single transcript is selected for alignment. 
   * @return How to group non-main-participant utterances for training, when a single
   * transcript is selected for alignment.
   */
  public String getOtherUtteranceGrouping() { return otherUtteranceGrouping; }
  /**
   * Setter for {@link #otherUtteranceGrouping}: How to group non-main-participant
   * utterances for training, when a single transcript is selected for alignment. 
   * @param newOtherUtteranceGrouping How to group non-main-participant utterances for
   * training, when a single transcript is selected for alignment.
   */
  public HTKAligner setOtherUtteranceGrouping(String newOtherUtteranceGrouping) { otherUtteranceGrouping = newOtherUtteranceGrouping; return this; }
  
  /**
   * Whether manual alignments should be overwritten (true) or not (false).
   * @see #getIgnoreAlignmentStatuses()
   * @see #setIgnoreAlignmentStatuses(Boolean)
   */
  protected Boolean ignoreAlignmentStatuses = Boolean.FALSE;
  /**
   * Getter for {@link #ignoreAlignmentStatuses}: Whether manual alignments should be
   * overwritten (true) or not (false). 
   * @return Whether manual alignments should be overwritten (true) or not (false).
   */
  public Boolean getIgnoreAlignmentStatuses() { return ignoreAlignmentStatuses; }
  /**
   * Setter for {@link #ignoreAlignmentStatuses}: Whether manual alignments should be
   * overwritten (true) or not (false). 
   * @param newIgnoreAlignmentStatuses Whether manual alignments should be overwritten (true) or not (false).
   */
  public HTKAligner setIgnoreAlignmentStatuses(Boolean newIgnoreAlignmentStatuses) { ignoreAlignmentStatuses = newIgnoreAlignmentStatuses; return this; }
  
  /**
   * Whether to use pre-trained P2FA models (true) or not (false).
   * @see #getUseP2FA()
   * @see #setUseP2FA(Boolean)
   */
  protected Boolean useP2FA = Boolean.FALSE;
  /**
   * Getter for {@link #useP2FA}: Whether to use pre-trained P2FA models (true) or not (false).
   * @return Whether to use pre-trained P2FA models (true) or not (false).
   */
  public Boolean getUseP2FA() { return useP2FA; }
  /**
   * Setter for {@link #useP2FA}: Whether to use pre-trained P2FA models (true) or not (false).
   * @param newUseP2FA Whether to use pre-trained P2FA models (true) or not (false).
   */
  public HTKAligner setUseP2FA(Boolean newUseP2FA) { useP2FA = newUseP2FA; return this; }
  
  /**
   * Sample rate in Hz to resample to, if any.
   * @see #getSampleRate()
   * @see #setSampleRate(Integer)
   */
  protected Integer sampleRate = null;
  /**
   * Getter for {@link #sampleRate}: Sample rate in Hz to resample to, if any.
   * @return Sample rate in Hz to resample to, if any.
   */
  public Integer getSampleRate() { return sampleRate; }
  /**
   * Setter for {@link #sampleRate}: Sample rate in Hz to resample to, if any.
   * @param newSampleRate Sample rate in Hz to resample to, if any.
   */
  public HTKAligner setSampleRate(Integer newSampleRate) { sampleRate = newSampleRate; return this; }
  
  /**
   * Regular expression for matching the participant ID of the participant in the left
   * audio channel. 
   * @see #getLeftPattern()
   * @see #setLeftPattern(String)
   */
  protected String leftPattern = "";
  /**
   * Getter for {@link #leftPattern}: Regular expression for matching the participant ID
   * of the participant in the left audio channel. 
   * @return Regular expression for matching the participant ID of the participant in the
   * left audio channel. 
   */
  public String getLeftPattern() { return leftPattern; }
  /**
   * Setter for {@link #leftPattern}: Regular expression for matching the participant ID
   * of the participant in the left audio channel. 
   * @param newLeftPattern Regular expression for matching the participant ID of the
   * participant in the left audio channel. 
   */
  public HTKAligner setLeftPattern(String newLeftPattern) { leftPattern = newLeftPattern; return this; }
  
  /**
   * Regular expression for matching the participant ID of the participant in the right
   * audio channel. 
   * @see #getRightPattern()
   * @see #setRightPattern(String)
   */
  protected String rightPattern = "";
  /**
   * Getter for {@link #rightPattern}: Regular expression for matching the participant ID
   * of the participant in the right audio channel. 
   * @return Regular expression for matching the participant ID of the participant in the
   * right audio channel. 
   */
  public String getRightPattern() { return rightPattern; }
  /**
   * Setter for {@link #rightPattern}: Regular expression for matching the participant ID
   * of the participant in the right audio channel. 
   * @param newRightPattern Regular expression for matching the participant ID of the
   * participant in the right audio channel. 
   */
  public HTKAligner setRightPattern(String newRightPattern) { rightPattern = newRightPattern; return this; }
  
  /**
   * Layer ID of the primary transcription token layer.
   * @see #getOrthographyLayerId()
   * @see #setOrthographyLayerId(String)
   */
  protected String orthographyLayerId;
  /**
   * Getter for {@link #orthographyLayerId}: Layer ID of the primary transcription token layer.
   * @return Layer ID of the primary transcription token layer.
   */
  public String getOrthographyLayerId() { return orthographyLayerId; }
  /**
   * Setter for {@link #orthographyLayerId}: Layer ID of the primary transcription token layer.
   * @param newOrthographyLayerId Layer ID of the primary transcription token layer.
   */
  public HTKAligner setOrthographyLayerId(String newOrthographyLayerId) { orthographyLayerId = newOrthographyLayerId; return this; }
  
  /**
   * Layer ID of the tag layer that identifies the phonemic transcription of each word.
   * @see #getPronunciationLayerId()
   * @see #setPronunciationLayerId(String)
   */
  protected String pronunciationLayerId;
  /**
   * Getter for {@link #pronunciationLayerId}: Layer ID of the tag layer that identifies the
   * phonemic transcription of each word. 
   * @return Layer ID of the tag layer that identifies the phonemic transcription of each word.
   */
  public String getPronunciationLayerId() { return pronunciationLayerId; }
  /**
   * Setter for {@link #pronunciationLayerId}: Layer ID of the tag layer that identifies the
   * phonemic transcription of each word. 
   * @param newPronunciationLayerId Layer ID of the tag layer that identifies the phonemic
   * transcription of each word. 
   */
  public HTKAligner setPronunciationLayerId(String newPronunciationLayerId) { pronunciationLayerId = newPronunciationLayerId; return this; }
  
  /**
   * Layer ID of the layer that includes noise annotations.
   * @see #getNoiseLayerId()
   * @see #setNoiseLayerId(String)
   */
  protected String noiseLayerId;
  /**
   * Getter for {@link #noiseLayerId}: Layer ID of the layer that includes noise annotations.
   * @return Layer ID of the layer that includes noise annotations.
   */
  public String getNoiseLayerId() { return noiseLayerId; }
  /**
   * Setter for {@link #noiseLayerId}: Layer ID of the layer that includes noise annotations.
   * @param newNoiseLayerId Layer ID of the layer that includes noise annotations.
   */
  public HTKAligner setNoiseLayerId(String newNoiseLayerId) { noiseLayerId = newNoiseLayerId; return this; }
  
  /**
   * Layer Id of the layer used to tag utterances with a time stamp when they are aligned.
   * @see #getUtteranceTagLayerId()
   * @see #setUtteranceTagLayerId(String)
   */
  protected String utteranceTagLayerId;
  /**
   * Getter for {@link #utteranceTagLayerId}: Layer Id of the layer used to tag utterances
   * with a time stamp when they are aligned. 
   * @return Layer Id of the layer used to tag utterances with a time stamp when they are aligned.
   */
  public String getUtteranceTagLayerId() { return utteranceTagLayerId; }
  /**
   * Setter for {@link #utteranceTagLayerId}: Layer Id of the layer used to tag
   * utterances with a time stamp when they are aligned. 
   * @param newUtteranceTagLayerId Layer Id of the layer used to tag utterances with a
   * time stamp when they are aligned. 
   */
  public HTKAligner setUtteranceTagLayerId(String newUtteranceTagLayerId) { utteranceTagLayerId = newUtteranceTagLayerId; return this; }
  
  /**
   * Layer Id of the participant attribute used to tag participant with a time stamp when
   * their utterances are aligned. 
   * @see #getParticipantTagLayerId()
   * @see #setParticipantTagLayerId(String)
   */
  protected String participantTagLayerId;
  /**
   * Getter for {@link #participantTagLayerId}: Layer Id of the participant attribute used
   * to tag participant with a time stamp when their utterances are aligned. 
   * @return Layer Id of the participant attribute used to tag participant with a time
   * stamp when their utterances are aligned. 
   */
  public String getParticipantTagLayerId() { return participantTagLayerId; }
  /**
   * Setter for {@link #participantTagLayerId}: Layer Id of the participant attribute used
   * to tag participant with a time stamp when their utterances are aligned. 
   * @param newParticipantTagLayerId Layer Id of the participant attribute used to tag
   * participant with a time stamp when their utterances are aligned. 
   */
  public HTKAligner setParticipantTagLayerId(String newParticipantTagLayerId) { participantTagLayerId = newParticipantTagLayerId; return this; }
  
  /**
   * Layer ID of the layer that receives word alignments.
   * @see #getWordAlignmentLayerId()
   * @see #setWordAlignmentLayerId(String)
   */
  protected String wordAlignmentLayerId;
  /**
   * Getter for {@link #wordAlignmentLayerId}: Layer ID of the layer that receives word alignments.
   * @return Layer ID of the layer that receives word alignments.
   */
  public String getWordAlignmentLayerId() { return wordAlignmentLayerId; }
  /**
   * Setter for {@link #wordAlignmentLayerId}: Layer ID of the layer that receives word alignments.
   * @param newWordAlignmentLayerId Layer ID of the layer that receives word alignments.
   */
  public HTKAligner setWordAlignmentLayerId(String newWordAlignmentLayerId) { wordAlignmentLayerId = newWordAlignmentLayerId; return this; }
  
  /**
   * Layer ID of the layer that receives phone alignments and labels.
   * @see #getPhoneAlignmentLayerId()
   * @see #setPhoneAlignmentLayerId(String)
   */
  protected String phoneAlignmentLayerId;
  /**
   * Getter for {@link #phoneAlignmentLayerId}: Layer ID of the layer that receives phone
   * alignments and labels. 
   * @return Layer ID of the layer that receives phone alignments and labels.
   */
  public String getPhoneAlignmentLayerId() { return phoneAlignmentLayerId; }
  /**
   * Setter for {@link #phoneAlignmentLayerId}: Layer ID of the layer that receives phone
   * alignments and labels. 
   * @param newPhoneAlignmentLayerId Layer ID of the layer that receives phone alignments
   * and labels. 
   */
  public HTKAligner setPhoneAlignmentLayerId(String newPhoneAlignmentLayerId) { phoneAlignmentLayerId = newPhoneAlignmentLayerId; return this; }
  
  /**
   * Layer ID of the layer that receives phone acoustic scores.
   * @see #getScoreLayerId()
   * @see #setScoreLayerId(String)
   */
  protected String scoreLayerId;
  /**
   * Getter for {@link #scoreLayerId}: Layer ID of the layer that receives phone acoustic scores.
   * @return Layer ID of the layer that receives phone acoustic scores.
   */
  public String getScoreLayerId() { return scoreLayerId; }
  /**
   * Setter for {@link #scoreLayerId}: Layer ID of the layer that receives phone acoustic scores.
   * @param newScoreLayerId Layer ID of the layer that receives phone acoustic scores.
   */
  public HTKAligner setScoreLayerId(String newScoreLayerId) { scoreLayerId = newScoreLayerId; return this; }
  
  /**
   * Percentage of overlap with other speech, above which the utterance is ignored.
   * @see #getOverlapThreshold()
   * @see #setOverlapThreshold(Double)
   */
  protected Integer overlapThreshold = Integer.valueOf(5);
  /**
   * Getter for {@link #overlapThreshold}: Percentage of overlap with other speech, above
   * which the utterance is ignored. 
   * @return Percentage of overlap with other speech, above which the utterance is ignored.
   */
  public Integer getOverlapThreshold() { return overlapThreshold; }
  /**
   * Setter for {@link #overlapThreshold}: Percentage of overlap with other speech, above
   * which the utterance is ignored. 
   * @param newOverlapThreshold Percentage of overlap with other speech, above which the
   * utterance is ignored. 
   */
  public HTKAligner setOverlapThreshold(Integer newOverlapThreshold) { overlapThreshold = newOverlapThreshold; return this; }
  
  /**
   * What should happen with working files after training/alignment is finished.
   * <ul>
   *  <li> 100 - always delete files regardless of result </li>
   *  <li> 75 - delete files only when training succeeded </li>
   *  <li> 25 - delete files only when training failed </li>
   *  <li> 0 - never delete files </li>
   * </ul>
   * @see #getCleanupOption()
   * @see #setCleanupOption(Integer)
   */
  protected Integer cleanupOption = Integer.valueOf(75);
  /**
   * Getter for {@link #cleanupOption}: What should happen with working files after
   * training/alignment is finished. 
   * @return What should happen with working files after training/alignment is finished.
   */
  public Integer getCleanupOption() { return cleanupOption; }
  /**
   * Setter for {@link #cleanupOption}: What should happen with working files after
   * training/alignment is finished. 
   * @param newCleanupOption What should happen with working files after
   * training/alignment is finished. 
   */
  public HTKAligner setCleanupOption(Integer newCleanupOption) { cleanupOption = newCleanupOption; return this; }
  
  /**
   * The training session ID.
   * @see #getSessionName()
   * @see #setSessionName(String)
   */
  protected String sessionName;
  /**
   * Getter for {@link #sessionName}: The training session ID.
   * @return The training session ID.
   */
  public String getSessionName() { return sessionName; }
  /**
   * Setter for {@link #sessionName}: The training session ID.
   * @param newSessionName The training session ID.
   */
  public HTKAligner setSessionName(String newSessionName) { sessionName = newSessionName; return this; }
  
  /**
   * Default constructor.
   */
  public HTKAligner() {
    setSchema( // This is the kind of schema we'd like (set here for testing purposes):
      new Schema(
        "who", "turn", "utterance", "word",
        new Layer("transcript_language", "Overall Language")
        .setAlignment(Constants.ALIGNMENT_NONE)
        .setPeers(false).setPeersOverlap(false).setSaturated(true),
        new Layer("who", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
        .setPeers(true).setPeersOverlap(true).setSaturated(true),
        new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false).setSaturated(false)
        .setParentId("who").setParentIncludes(true),
        new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false).setSaturated(true)
        .setParentId("turn").setParentIncludes(true),
        new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false).setSaturated(false)
        .setParentId("turn").setParentIncludes(true),
        new Layer("phonemes", "Phonemes").setAlignment(Constants.ALIGNMENT_NONE)
        .setPeers(true).setPeersOverlap(true).setSaturated(true)
        .setParentId("word").setParentIncludes(true)
        .setType("ipa"),
        new Layer("segment", "Phones").setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false).setSaturated(true)
        .setParentId("word").setParentIncludes(true)
        .setType("ipa")));
    
  } // end of constructor
   
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
  public String getConfig() {
    // load configuration, if any
    File f = new File(getWorkingDirectory(), getAnnotatorId() + ".cfg");
    if (f.exists()) {
      try {
        beanPropertiesFromQueryString(IO.InputStreamToString(new FileInputStream(f)));
      } catch(IOException exception) {}
    }
    if (htkPath == null || htkPath.length() == 0) {
      // on linux based systems, HTK can install itself on to the path
      // so we try to find out where...
      File HVitePath = Execution.Which("HVite");
      if (HVitePath != null) {
        htkPath = HVitePath.getParentFile().getPath();
      } else {
        File fHVite = new File("/usr/local/bin/HVite");
        if (fHVite.exists()) htkPath = fHVite.getParentFile().getPath();
      }
    }
    if (htkPath != null) {
      return "htkPath="+htkPath;
    }
    return null;
  }
  
  /**
   * Ensures HTK is available.
   * @throws InvalidConfigurationException
   * @see #getConfig()
   * @see #beanPropertiesFromQueryString(String)
   */ 
  public void setConfig(String config) throws InvalidConfigurationException {
    setRunning(true);
    try {
      setStatus(""); // clear any residual status from the last run...

      beanPropertiesFromQueryString(config);

      // unpack scripts and models
      String[] aFiles = {
        // Train & Align files
        "scripts/config", "scripts/configleft", "scripts/configright", "scripts/config0", 
        "scripts/mkphones0.led", "scripts/mkphones1.led", 
        "scripts/mktri.ded", "scripts/mktri.led", "scripts/proto", 
        "scripts/sil.hed", "scripts/tree.hed",
        // p2fa files
        "p2fa/model/11025/config", "p2fa/model/11025/hmmdefs", "p2fa/model/11025/macros", 
        "p2fa/model/monophones", "p2fa/readme.txt"
      };
      for (String file : aFiles) {
        URL urlSource = getClass().getResource(file);
        File fDestination = getWorkingDirectory();
        String[] pathElements = file.split("/");
        for (String element : pathElements) {
          if (!fDestination.exists()) fDestination.mkdir();
          fDestination = new File(fDestination, element);
        } // next path element

        if (!fDestination.exists()) {
          setStatus("Unpacking " + file);
          InputStream isSource = urlSource.openStream();
          FileOutputStream osDestination = new FileOutputStream(fDestination);
          IO.Pump(isSource, osDestination);
        }
      } // next script
      setPercentComplete(10);

      File fHtkPath = htkPath == null?null:new File(htkPath);
      File HVite = fHtkPath == null?null:new File(htkPath, "HVite");
      if (HVite != null && !HVite.exists()) HVite = new File(htkPath, "HVite.exe");
      if (HVite == null || !HVite.exists()) { // don't have HTK exes
        setStatus("HTK not found");

        if (htkUserId != null && htkPassword != null) { // have user/password
          setStatus("Attempt to download HTK...");
          if (java.lang.System.getProperty("os.name").startsWith("Windows")) { // Windows
            
            // download exes...
            URL url = new URL("https://htk.eng.cam.ac.uk/ftp/software/htk-3.3-windows-binary.zip");
            setStatus("Attempting to download HTK from: " + url);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            String authorization = "Basic " + java.util.Base64.getEncoder()
              .encodeToString((htkUserId+":"+htkPassword).getBytes());
            connection.setRequestProperty("Authorization", authorization);
            InputStream input = connection.getInputStream();
            File zip = new File(getWorkingDirectory(), url.getPath().replaceAll(".*/",""));
            IO.Pump(input, new FileOutputStream(zip));
            
            setStatus("Unzipping: " + zip.getName());
            IO.Unzip(zip, getWorkingDirectory());

            fHtkPath = new File(getWorkingDirectory(), "htk");
            HVite = new File(fHtkPath, "HVite.exe");
            if (HVite.exists()) {
              htkPath = fHtkPath.getPath();
              setStatus("Download successful, htkPath: " + htkPath);
              System.out.println("Download successful, htkPath: " + htkPath);
            } else {
              setStatus("Sorry, could not download HTK.");
              throw new InvalidConfigurationException(
                this, "No path to HTK, and could not download it.");
            }            
          } else { // not windows
            boolean canCompileSource = Execution.Which("make") != null;
            if (!canCompileSource) {
              setStatus("Cannot build from source as 'make' is not available.");
            } else {
              // don't have HTK path but maybe we can download/build/install it
              
              // TODO build for Mac too
              
              // create a temp directory for working
              File htkSourceDirectory = File.createTempFile("htk_", "_src");
              htkSourceDirectory.delete();
              htkSourceDirectory.mkdir();
              
              // wget --user=... --password=... http://htk.eng.cam.ac.uk/ftp/software/HTK-3.4.1.tar.gz
              setStatus("Attempting to download HTK source code...");
              Execution cmd = new Execution()
                .setExe("wget")
                .arg("--user=" + htkUserId)
                .arg("--password=" + htkPassword)
                .arg("http://htk.eng.cam.ac.uk/ftp/software/HTK-3.4.1.tar.gz")
                .setWorkingDirectory(htkSourceDirectory);
              cmd.run();
              if (cmd.getError().length() > 0) setStatus("stderr: " + cmd.getError());
              setStatus(cmd.getInput().toString());
              
              setPercentComplete(20);
              if (isCancelling()) throw new InvalidConfigurationException(this, "Cancelled");
              
              // tar -xzf HTK-3.4.1.tar.gz
              setStatus("Extracting HTK source code...");
              cmd = new Execution()
                .setExe("tar")
                .arg("-xzf")
                .arg("HTK-3.4.1.tar.gz")
                .setWorkingDirectory(htkSourceDirectory);
              cmd.run();
              if (cmd.getError().length() > 0) setStatus("stderr: " + cmd.getError());
              setStatus(cmd.getInput().toString());
              
              setPercentComplete(30);
              if (isCancelling()) throw new InvalidConfigurationException(this, "Cancelled");
              
              // cd htk
              File htkSrc = new File(htkSourceDirectory, "htk");
              setStatus("HTK source code directory: " + htkSourceDirectory.getPath());
              try {
                
                // ./configure --without-x --disable-hslab --disable-hlmtools
                setStatus("Creating build configuration...");
                cmd = new Execution()
                  .setExe(new File(htkSrc, "configure"))
                  .arg("--without-x")
                  .arg("--disable-hslab")
                  .arg("--disable-hlmtools")
                  .setWorkingDirectory(htkSrc);
                cmd.run();
                if (cmd.getError().length() > 0) setStatus("stderr: " + cmd.getError());
                setStatus(cmd.getInput().toString());
                
                setPercentComplete(40);
                if (isCancelling()) throw new InvalidConfigurationException(this, "Cancelled");
                
                // make all
                setStatus("Building HTK...");
                cmd = new Execution()
                  .setExe("make")
                  .arg("all")
                  .setWorkingDirectory(htkSrc);
                cmd.run();
                if (cmd.getError().length() > 0) setStatus("stderr: " + cmd.getError());
                setStatus(cmd.getInput().toString());
                
                setPercentComplete(50);
                if (isCancelling()) throw new InvalidConfigurationException(this, "Cancelled");
                
                // make install
                setStatus("Installing HTK...");
                cmd = new Execution()
                  .setExe("make")
                  .arg("install")
                  .setWorkingDirectory(htkSrc);
                cmd.run();
                if (cmd.getError().length() > 0) setStatus("stderr: " + cmd.getError());
                setStatus(cmd.getInput().toString());
                
                setPercentComplete(60);
                if (isCancelling()) throw new InvalidConfigurationException(this, "Cancelled");
                
                File HVitePath = Execution.Which("HVite");
                if (HVitePath != null) {
                  setStatus("HVite: " + HVitePath.getPath());
                  fHtkPath = HVitePath.getParentFile();
                  if (fHtkPath != null) {
                    setStatus("Build successful, htkPath: " + fHtkPath.getPath());
                    htkPath = fHtkPath.getPath();
                  } else {
                    setStatus("Sorry, could not build HTK from source code.");
                    throw new InvalidConfigurationException(
                      this, "No path to HTK, and could not build it.");
                  }
                }
              } finally { // no matter what, delete our working files
                IO.RecursivelyDelete(htkSourceDirectory);
              }
            } // canCompileSource
          } // not Windows
        } else { // don't have user/password
          throw new InvalidConfigurationException(this, "No path to HTK.");
        }
      } // bad htkPath

      // persist configuration
      PrintWriter writer = new PrintWriter(
        new File(getWorkingDirectory(), getAnnotatorId() + ".cfg"), "UTF-8");
      writer.print("htkPath="+URLEncoder.encode(htkPath, "UTF-8"));
      writer.close();

      setPercentComplete(100);
    } catch (IOException ioX) {
      setStatus("ERROR: " + ioX);
      throw new InvalidConfigurationException(
        this, "Error processing file: " + ioX.getMessage(), ioX);
    } finally {
      setRunning(false);
    }
  }
   
  /**
   * Sets the configuration for a given annotation task.
   * @param parameters The configuration of the annotator; a value of <tt> null </tt>
   * will apply the default task parameters, with {@link #tokenLayerId} set to the
   * {@link Schema#wordLayerId} and {@link #stemLayerId} set to <q>stem</q>.
   * @throws InvalidConfigurationException
   */
  public void setTaskParameters(String parameters) throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");
      
    if (parameters == null) { // there is no possible default parameter
      throw new InvalidConfigurationException(this, "Parameters not set.");         
    }

    beanPropertiesFromQueryString(parameters);

    // convert empty strings into nulls
    if (orthographyLayerId != null && orthographyLayerId.length() == 0)
      orthographyLayerId = null;
    if (pronunciationLayerId != null && pronunciationLayerId.length() == 0)
      pronunciationLayerId = null;
    if (noiseLayerId != null && noiseLayerId.length() == 0)
      noiseLayerId = null;
    if (utteranceTagLayerId != null && utteranceTagLayerId.length() == 0)
      utteranceTagLayerId = null;
    if (participantTagLayerId != null && participantTagLayerId.length() == 0)
      participantTagLayerId = null;
    if (wordAlignmentLayerId != null && wordAlignmentLayerId.length() == 0)
      wordAlignmentLayerId = null;
    if (phoneAlignmentLayerId != null && phoneAlignmentLayerId.length() == 0)
      phoneAlignmentLayerId = null;
    if (scoreLayerId != null && scoreLayerId.length() == 0)
      scoreLayerId = null;
    if (leftPattern != null && leftPattern.length() == 0)
      leftPattern = null;
    if (rightPattern != null && rightPattern.length() == 0)
      rightPattern = null;
    if (pauseMarkers != null && pauseMarkers.length() == 0)
      pauseMarkers = null;
    if (noisePatterns != null && noisePatterns.length() == 0)
      noisePatterns = null;

    // validation...
      
    // there must be at least one source layer
    String[] requiredLayers = getRequiredLayers();
    if (requiredLayers.length == 0) {
      throw new InvalidConfigurationException(this, "There are no source layers specified.");
    }
    if (schema.getLayer(orthographyLayerId) == null)
      throw new InvalidConfigurationException(
        this, "Orthography layer not found: " + orthographyLayerId);
    if (schema.getLayer(pronunciationLayerId) == null)
      throw new InvalidConfigurationException(
        this, "Pronunciation layer not found: " + pronunciationLayerId);
    if (noiseLayerId != null && schema.getLayer(noiseLayerId) == null)
      throw new InvalidConfigurationException(this, "Noise layer not found: " + noiseLayerId);

    if (utteranceTagLayerId != null) {
      Layer utteranceTagLayer = schema.getLayer(utteranceTagLayerId);
      if (utteranceTagLayer == null) {
        schema.addLayer(
          new Layer(utteranceTagLayerId)
          .setAlignment(Constants.ALIGNMENT_INTERVAL)
          .setPeers(true).setPeersOverlap(false)
          .setParentId(schema.getTurnLayerId())
          .setDescription("HTK alignment time."));
      } else if (utteranceTagLayerId.equals(orthographyLayerId)
                 || utteranceTagLayerId.equals(pronunciationLayerId)
                 || utteranceTagLayerId.equals(noiseLayerId)
                 || utteranceTagLayerId.equals(wordAlignmentLayerId)
                 || utteranceTagLayerId.equals(schema.getWordLayerId())
                 || utteranceTagLayerId.equals(schema.getTurnLayerId())
                 || utteranceTagLayerId.equals(schema.getUtteranceLayerId())
                 || utteranceTagLayerId.equals(phoneAlignmentLayerId)
                 || utteranceTagLayerId.equals(participantTagLayerId)
                 || !utteranceTagLayer.getParentId().equals(schema.getTurnLayerId())
                 || utteranceTagLayer.getAlignment() != Constants.ALIGNMENT_INTERVAL) {
        throw new InvalidConfigurationException(
          this, "Invalid utterance tag layer: " + utteranceTagLayerId);
      }
    } // utteranceTagLayerId != null
    if (participantTagLayerId != null) {
      Layer participantTagLayer = schema.getLayer(participantTagLayerId);
      if (participantTagLayer == null) {
        schema.addLayer(
          new Layer(participantTagLayerId)
          .setAlignment(Constants.ALIGNMENT_INTERVAL)
          .setPeers(true).setPeersOverlap(false)
          .setParentId(schema.getTurnLayerId())
          .setDescription("HTK participant alignment time."));
      } else if (participantTagLayerId.equals(orthographyLayerId)
                 || participantTagLayerId.equals(pronunciationLayerId)
                 || participantTagLayerId.equals(noiseLayerId)
                 || participantTagLayerId.equals(wordAlignmentLayerId)
                 || participantTagLayerId.equals(schema.getWordLayerId())
                 || participantTagLayerId.equals(schema.getTurnLayerId())
                 || participantTagLayerId.equals(schema.getUtteranceLayerId())
                 || participantTagLayerId.equals(phoneAlignmentLayerId)
                 || participantTagLayerId.equals(utteranceTagLayerId)
                 || !participantTagLayer.getParentId().equals(schema.getTurnLayerId())
                 || participantTagLayer.getAlignment() != Constants.ALIGNMENT_INTERVAL) {
        throw new InvalidConfigurationException(
          this, "Invalid participant tag layer: " + participantTagLayerId);
      }
    } // participantTagLayerId != null

    Layer wordAlignmentLayer = schema.getLayer(wordAlignmentLayerId);
    if (wordAlignmentLayer == null) {
      wordAlignmentLayer = new Layer(wordAlignmentLayerId)
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false).setSaturated(false)
        .setParentId(schema.getTurnLayerId())
        .setDescription("HTK word alignments.");
      schema.addLayer(wordAlignmentLayer);
    } else if (wordAlignmentLayerId.equals(pronunciationLayerId)
               || wordAlignmentLayerId.equals(noiseLayerId)
               || wordAlignmentLayerId.equals(schema.getTurnLayerId())
               || wordAlignmentLayerId.equals(schema.getUtteranceLayerId())
               || wordAlignmentLayerId.equals(phoneAlignmentLayerId)
               || wordAlignmentLayerId.equals(utteranceTagLayerId)
               || !wordAlignmentLayer.getParentId().equals(schema.getTurnLayerId())
               || wordAlignmentLayer.getAlignment() != Constants.ALIGNMENT_INTERVAL) {
      throw new InvalidConfigurationException(
        this, "Invalid word alignment layer: " + wordAlignmentLayerId);
    }
    
    Layer phoneAlignmentLayer = schema.getLayer(phoneAlignmentLayerId);
    if (phoneAlignmentLayer == null) {
      phoneAlignmentLayer = new Layer(phoneAlignmentLayerId)
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false).setSaturated(false)
        .setParentId(schema.getTurnLayerId())
        .setDescription("HTK phone alignments.")
        .setType(schema.getLayer(pronunciationLayerId).getType());
      schema.addLayer(phoneAlignmentLayer);
    } else if (phoneAlignmentLayerId.equals(wordAlignmentLayerId)
               || phoneAlignmentLayerId.equals(pronunciationLayerId)
               || phoneAlignmentLayerId.equals(noiseLayerId)
               || phoneAlignmentLayerId.equals(schema.getWordLayerId())
               || phoneAlignmentLayerId.equals(schema.getTurnLayerId())
               || phoneAlignmentLayerId.equals(schema.getUtteranceLayerId())
               || phoneAlignmentLayerId.equals(utteranceTagLayerId)
               || phoneAlignmentLayer.getAlignment() != Constants.ALIGNMENT_INTERVAL) {
      throw new InvalidConfigurationException(
        this, "Invalid phone alignment layer: " + phoneAlignmentLayerId);
    } else { // phoneAlignmentLayer is set and not a system layer
      // check it relates to wordAlignmentLayerId correctly
      if (wordAlignmentLayerId.equals(schema.getWordLayerId())) {
        if (!phoneAlignmentLayer.getParentId().equals(schema.getWordLayerId())) {
          throw new InvalidConfigurationException(
            this, "Phone alignment layer "+phoneAlignmentLayerId
            +" must be a "+schema.getWordLayerId()
            +" layer but is a " + phoneAlignmentLayer.getParentId() + " layer");
        }
      } else { // wordAlignmentLayerId is not the standard word layer
        if (!phoneAlignmentLayer.getParentId().equals(schema.getTurnLayerId())) {
          throw new InvalidConfigurationException(
            this, "Phone alignment layer "+phoneAlignmentLayerId
            +" must be a "+schema.getTurnLayerId()
            +" layer but is a " + phoneAlignmentLayer.getParentId() + " layer");
        }
      } // wordAlignmentLayerId is not the standard word layer
    } // phoneAlignmentLayer is set and not a system layer

    // P2FA requires 11,025Hz sample rate, and doesn't provide scores (or can it? TODO)
    if (useP2FA) {
      sampleRate = Integer.valueOf(11025);
      scoreLayerId = null;
      mainUtteranceGrouping = "Transcript";
      otherUtteranceGrouping = "Transcript";
    } else {
      if (mainUtteranceGrouping == null) mainUtteranceGrouping = "Speaker";
      if (otherUtteranceGrouping == null) otherUtteranceGrouping = "Not Aligned";
    }

    if (scoreLayerId != null) {
      Layer scoreLayer = schema.getLayer(scoreLayerId);
      if (scoreLayer == null) {
        if (phoneAlignmentLayer.getParentId().equals(schema.getWordLayerId())) { // segment layer
          // segment tag layer
          schema.addLayer(
            new Layer(scoreLayerId)
            .setAlignment(Constants.ALIGNMENT_NONE)
            .setPeers(false).setSaturated(true)
            .setParentId(phoneAlignmentLayer.getId())
            .setType(Constants.TYPE_NUMBER)
            .setDescription("HTK phone confidence scores."));
        } else {
          // 'phrase' layer - i.e. aligned child of turn
          schema.addLayer(
            new Layer(scoreLayerId)
            .setAlignment(Constants.ALIGNMENT_INTERVAL)
            .setPeers(true).setSaturated(false)
            .setParentId(schema.getTurnLayerId())
            .setType(Constants.TYPE_NUMBER)
            .setDescription("HTK phone confidence scores."));
        }
      } else if (scoreLayerId.equals(orthographyLayerId)
                 || scoreLayerId.equals(pronunciationLayerId)
                 || scoreLayerId.equals(noiseLayerId)
                 || scoreLayerId.equals(wordAlignmentLayerId)
                 || scoreLayerId.equals(schema.getWordLayerId())
                 || scoreLayerId.equals(schema.getTurnLayerId())
                 || scoreLayerId.equals(schema.getUtteranceLayerId())
                 || scoreLayerId.equals(phoneAlignmentLayerId)
                 || scoreLayerId.equals(utteranceTagLayerId)
                 || !scoreLayer.getParentId().equals(schema.getTurnLayerId())
                 || scoreLayer.getAlignment() != Constants.ALIGNMENT_INTERVAL) {
        throw new InvalidConfigurationException(
          this, "Invalid participant tag layer: " + scoreLayerId);
      } else { // scoreLayerId is set and not a system layer
        // check scoreLayer relates correctly to phoneAlignmentLayer
        if (phoneAlignmentLayer.getParentId().equals(schema.getWordLayerId())) {
          if (!scoreLayer.getParentId().equals(phoneAlignmentLayerId)) {
            throw new InvalidConfigurationException(
              this, "Score layer "+scoreLayerId
              +" must be a "+phoneAlignmentLayerId
              +" layer but is a " + scoreLayer.getParentId() + " layer");
          }
        } else { // phoneAlignmentLayer is a turn child
          if (!scoreLayer.getParentId().equals(schema.getTurnLayerId())) {
            throw new InvalidConfigurationException(
              this, "Score layer "+scoreLayerId
              +" must be a phrase layer"
              +" but is a " + scoreLayer.getParentId() + " layer");
          }
        }
      } // scoreLayerId is set and not a system layer
    } // scoreLayerId != null

  }

  /**
   * Determines which layers the annotator requires in order to annotate a graph.
   * @return A list of layer IDs. In this case, the annotator only requires the schema's
   * word layer.
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public String[] getRequiredLayers() throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");
    HashSet<String> requiredLayers = new HashSet<String>();
    // layers used for chunking
    if (schema.getParticipantLayerId() != null)
      requiredLayers.add(schema.getParticipantLayerId());
    if (schema.getLayer(mainParticipantLayerId) != null)
      requiredLayers.add(mainParticipantLayerId);
    if (schema.getUtteranceLayerId() != null)
      requiredLayers.add(schema.getUtteranceLayerId());
    
    // word layer is used for detecting pause markers
    if (schema.getWordLayerId() != null) requiredLayers.add(schema.getWordLayerId());
    
    if (orthographyLayerId != null) {
      requiredLayers.add(orthographyLayerId);
    } else {
      throw new InvalidConfigurationException(this, "Orthography layer is not set.");
    }
    
    if (pronunciationLayerId != null) {
      requiredLayers.add(pronunciationLayerId);
    } else {
      throw new InvalidConfigurationException(this, "Pronunciation layer is not set.");
    }
    
    if (noiseLayerId != null)
      requiredLayers.add(noiseLayerId);
    return requiredLayers.toArray(new String[0]);
  }

  /**
   * Determines which layers the annotator will create/update/delete annotations on.
   * @return A list of layer IDs. In this case, the annotator has no task web-app for
   * specifying an output layer, and doesn't update any layers, so this method returns an
   * empty array.
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public String[] getOutputLayers() throws InvalidConfigurationException {
    HashSet<String> outputLayers = new HashSet<String>();
    if (participantTagLayerId != null) outputLayers.add(participantTagLayerId);
    if (utteranceTagLayerId != null) outputLayers.add(utteranceTagLayerId);
    if (wordAlignmentLayerId != null) outputLayers.add(wordAlignmentLayerId);
    if (phoneAlignmentLayerId != null) outputLayers.add(phoneAlignmentLayerId);
    if (scoreLayerId != null) outputLayers.add(scoreLayerId);
    return outputLayers.toArray(new String[0]);
  }

  // members for managing progress

  int batchCount = 1;
  int completedBatches = 0;
  /**
   * Sets the batch progress, and in turn, the overall progress.
   * @param percent
   */
  void setBatchPercentComplete(int percent) {
    setPercentComplete(
      Math.max(((100*completedBatches) + percent)
               / batchCount,
               1)); // percente complete should be at least 1
  } // end of setBatchPercentComplete()
   
  /**
   * Transforms the graph. In this case, the graph is simply summarized, by counting all
   * tokens of each word type, and printing out the result to stdout.
   * @param graph The graph to transform.
   * @return The changes introduced by the transformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph transform(Graph graph) throws TransformationException {
    setRunning(true);
    reset();
    try {
      Consumer<Graph> alignedFragmentConsumer = fragment->{
        // if the fragment comes from this graph
        if (graph.getId().equals(fragment.sourceGraph().getId())) {
          // apply the changes to the main graph
          graph.applyChangesFromFragment(fragment, new HashSet<String>() {{
            add(wordAlignmentLayerId);
            add(phoneAlignmentLayerId);
            add(utteranceTagLayerId);
            add(participantTagLayerId);
          }});
        } // fragment is from this graph
      };
      if (getStore() != null) {
        final Consumer<Graph> localUpdater = alignedFragmentConsumer;
        alignedFragmentConsumer = fragment->{
          if (fragment.getTracker().getChanges().size() > 0) { // save changes to database
            // only save them to the store
            // (if we update this graph as well, they'll be updated twice)
            try {
              getStore().saveTranscript(fragment);
            } catch(Exception exception) {
              throw new RuntimeException(exception);
            }
          } else {
            setStatus(fragment.getId() + ": No changes.");
          }
        };
      }
      
      // however we group utterances, we'll want fragments with all layers
      String[] layerIds = schema.getLayers().keySet().toArray(new String[0]);
      if (useP2FA) {
        
        // if we're using P2FA pre-trained models, there's no training,
        // so we just call transformFragments with all utterances
        Vector<Graph> utterances = new Vector<Graph>();
        for (Annotation utterance : graph.all(schema.getUtteranceLayerId())) {
          Graph fragment = graph.getFragment(utterance, layerIds);
          fragment.trackChanges();
          utterances.add(fragment);
        }
        batchCount = 1;
        completedBatches = 0;
        if (!isCancelling()) {
          transformFragments(utterances.stream(), alignedFragmentConsumer);
        }
        
      } else { // train and align

        // if we're given a graph to align we need to invoke transformGraphs for each
        // participant's utterances, so that models are trained for each participant
        
        // this may involve utterances from other graphs,
        // if mainUtteranceGrouping=Speaker and we have a graph store for looking up utterances

        if (getStore() == null) { // no graph store
          LinkedHashMap<String, Vector<Graph>> speakers
            = new LinkedHashMap<String, Vector<Graph>>();
          
          // first, we'll definitely be processing main participants
          for (Annotation mainParticipant : graph.all(mainParticipantLayerId)) {
            if (isCancelling()) break;
            Annotation participant = mainParticipant.first(schema.getParticipantLayerId());
            Vector<Graph> utterances = new Vector<Graph>();
            // just use annotations from the local graph
            for (Annotation utterance : participant.all(schema.getUtteranceLayerId())) {
              utterances.add(graph.getFragment(utterance, layerIds));
            } // next utterance
            speakers.put(participant.getLabel(), utterances);
          } // next main participant
          
          // secondly, process non-main participants
          if ("Transcript".equals(otherUtteranceGrouping)) { // not ignoring non-main-participants
            for (Annotation participant : graph.all(schema.getParticipantLayerId())) {
              if (isCancelling()) break;
              if (participant.first(mainParticipantLayerId) == null) { // non-main participant
                Vector<Graph> utterances = new Vector<Graph>();
                // just use annotations from the local graph
                for (Annotation utterance : participant.all(schema.getUtteranceLayerId())) {
                  utterances.add(graph.getFragment(utterance, layerIds));
                } // next utterance
                speakers.put(participant.getLabel(), utterances);
              } // non-main participant  
            } // next participant
          } // not ignoring non-main-participants
          
          if (!isCancelling()) {
            // process batches
            batchCount = speakers.size();
            completedBatches = 0;
            for (String speaker : speakers.keySet()) {
              setStatus("Aligning " + speaker);
              transformFragments(speakers.get(speaker).stream(), alignedFragmentConsumer);
              setStatus(speaker + " aligned.");
              completedBatches++;
              if (isCancelling()) break;
            } // next batch
          } // not cancelling
        } else { // graph store set
          LinkedHashMap<String, Annotation[]> speakers
            = new LinkedHashMap<String, Annotation[]>();
          
          // first, we'll definitely be processing main participants
          for (Annotation mainParticipant : graph.all(mainParticipantLayerId)) {
            if (isCancelling()) break;
            Annotation participant = mainParticipant.first(schema.getParticipantLayerId());
            Vector<Graph> utterances = new Vector<Graph>();
            setStatus("Identifying utterances of " + participant.getLabel() + " ...");
            String query = "layer.id == '"+esc(schema.getUtteranceLayerId())+"'"
              +" && first('"+esc(schema.getParticipantLayerId())+"').label"
              +" == '"+esc(participant.getLabel())+"'";
            if ("Transcript".equals(mainUtteranceGrouping)) { // only from this transcript
              query += " && graph.id == '"+esc(graph.getId())+"'";
            }
            Annotation[] allUtterances = getStore().getMatchingAnnotations(query, null, null);
            speakers.put(participant.getLabel(), allUtterances);
          } // next main participant

          // secondly, process non-main participants
          if ("Transcript".equals(otherUtteranceGrouping)) { // not ignoring non-main-participants
            for (Annotation participant : graph.all(schema.getParticipantLayerId())) {
              if (isCancelling()) break;
              if (participant.first(mainParticipantLayerId) == null) { // non-main participant
                Vector<Graph> utterances = new Vector<Graph>();
                setStatus("Identifying utterances of " + participant.getLabel() + " ...");
                String query = "layer.id == '"+esc(schema.getUtteranceLayerId())+"'"
                  +" && first('"+esc(schema.getParticipantLayerId())+"').label"
                  +" == '"+esc(participant.getLabel())+"'"
                  +" && graph.id == '"+esc(graph.getId())+"'";
                Annotation[] allUtterances = getStore().getMatchingAnnotations(query, null, null);
                speakers.put(participant.getLabel(), allUtterances);
              } // non-main participant  
            } // next participant
          } // not ignoring non-main-participants

          if (!isCancelling()) {
            // process batches
            batchCount = speakers.size();
            completedBatches = 0;
            for (String speaker : speakers.keySet()) {
              setStatus("Loading utterances of " + speaker + " ...");
              Vector<Graph> utterances = new Vector<Graph>();
              for (Annotation utterance : speakers.get(speaker)) { // each utterance annotation
                if (isCancelling()) break;
                utterances.add(
                  getStore().getFragment( // get the fragment corresponding to the utterance
                    utterance.getGraph().getId(), utterance.getId(), layerIds));
              } // next utterance
              setStatus("Aligning " + speaker);
              transformFragments(utterances.stream(), alignedFragmentConsumer);
              setStatus(speaker + " aligned.");
              completedBatches++;
              if (isCancelling()) break;
            } // next batch
          } // not cancelling
        } // graph store set
        
      } // train & align
    } catch (GraphNotFoundException x) {
      throw new TransformationException(this, x);
    } catch (PermissionException x) {
      throw new TransformationException(this, x);
    } catch (StoreException x) {
      throw new TransformationException(this, x);
    } catch (RuntimeException x) {
      throw new TransformationException(this, x.getCause());
    } finally {
      setRunning(false);
    }
    return graph;
  }

  private HTK htk;

  /**
   * Force-aligns the given utterance fragments.
   * @param graphs A stream of fragments.
   * @param consumer A consumer for receiving the graphs once they're transformed.
   * @throws TransformationException
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  @Override
  public void transformFragments(Stream<Graph> graphs, Consumer<Graph> consumer)
    throws TransformationException, InvalidConfigurationException {
    setBatchPercentComplete(0);
    setRunning(true);
    reset();

    try {
      
      PhonemeTranslator phonemesToHtk = new PhonemeTranslator(); // (default no translation)
      PhonemeTranslator htkToPhonemes = phonemesToHtk;
      boolean discDictionary = "ipa".equals(schema.getLayer(pronunciationLayerId).getType());
      boolean discOutput = "ipa".equals(schema.getLayer(phoneAlignmentLayerId).getType());

      List<Graph> fragments = null;
      TransformationException failure = null;
      try {
        
        if (!useP2FA) { // train & align
          
          // convert output to DISC?
          if (discDictionary) {           
            phonemesToHtk = new DISC2HTK();
            htkToPhonemes = new HTK2DISC();
          } else {
            if (discOutput) {
              htkToPhonemes = new CMU2DISC();
            }
          }
          
          // create initial file structure
          createSessionWorkingDir();
          createInitialFileStructure();
          setBatchPercentComplete(5);
          
          // create input files
          fragments = createInputFiles(graphs, phonemesToHtk);
          setBatchPercentComplete(15);
          
          // if there are still some utterances
          if (fragments.size() > 0) {

            if (!isCancelling()) {
              step1();
              setBatchPercentComplete(20);
              
              if (!isCancelling()) {
                step2();
                setBatchPercentComplete(30);
                
                // step 3 is record audio, which we've already done
                
                if (!isCancelling()) {
                  step4();
                  setBatchPercentComplete(40);
                  
                  // step 5 is extract features from audio, which we've already done

                  if (!isCancelling()) {
                    step6();
                    setBatchPercentComplete(50);

                    if (!isCancelling()) {
                      step7();
                      setBatchPercentComplete(60);

                      if (!isCancelling()) {                        
                        step8();
                        setBatchPercentComplete(70);

                        if (!isCancelling()) {                          
                          // at this point we can get the word alignments...
                          recognizeWordAlignments(phonemeListFile, "hmm09");
                          setBatchPercentComplete(80);
                          
                          // ...so we don't actually need to continue with triphones.
                          //step9();
                          //step10(10);
                          //recognizeWordAlignments(triphoneListFile, "hmm15");
                        } // not cancelling
                      } // not cancelling
                    } // not cancelling
                  } // not cancelling
                } // not cancelling
              } // not cancelling
            } // not cancelling
          } // there are valid fragments
          
        } else { // useP2FA
          
          // create initial file structure
          createSessionWorkingDir();
          setBatchPercentComplete(5);
          
          // convert output to DISC?
          if (discDictionary) {
            phonemesToHtk = new DISC2CMU().setDefaultStress("1");
            htkToPhonemes = new CMU2DISC();
          } else {
            if (discOutput) {
              htkToPhonemes = new CMU2DISC();
            }
          }
          
          // create input files
          fragments = createInputFiles(graphs, phonemesToHtk);
          setBatchPercentComplete(30);
          
          // if there are still some utterances
          if (fragments.size() > 0) {
            if (!isCancelling()) {
              // force align
              forceAlign();
            } // not cancelling
          } // there are valid fragments
          
        } // useP2FA

        setBatchPercentComplete(90);

        // check number of utterances
        if (fragments.size() == 0) {
          setStatus("No utterances could be aligned.");
        } else {
          // update the transcripts where word alignments were found
          if (!isCancelling())
          {
            updateAlignments(
              htkToPhonemes, sampleRate != null && sampleRate.intValue() == 11025,
              fragments, consumer);
            setStatus("Complete - words and phones from selected utterances are now aligned.");
          }
        }
      } catch (IOException x) {
        throw new TransformationException(this, x);
      } catch (TransformationException x) {
        failure = x;
      }
      
      // cleanup
      boolean cleanup = false;
      switch (cleanupOption.intValue()) {
        case 100: // always
          cleanup = true;
          break;
        case 75: // on success
          cleanup = failure == null;
          break;
        case 25: // on failure
          cleanup = failure != null;
          break;
      }
      if (cleanup) {
        IO.RecursivelyDelete(sessionWorkingDir);
      }

      // TODO result URL?

      if (failure != null) throw failure;
      
      setBatchPercentComplete(100);
    } finally {
      setRunning(false);
    }
  } // end of transformTranscripts()

  // Bunch of files and resources needed by HTK:

  /** The working directory for this training session. */
  protected File sessionWorkingDir;
  /** The log for this training session. */
  protected File logFile;
  /** The grammar. */
  protected File grammar;
  /** Word MLF file. */
  protected File wordsMlf;
  /** Aligned words MLF file. */
  protected File alignedWordsMlf;
  /** Aligned phones MLF file. */
  protected File alignedPhonesMlf;
  /** SCP file. */
  protected File scp;
  /** Training SCP file. */
  protected File trainingScp;
  /** Dictionary */
  protected Map<String,LinkedHashSet<String>> dictionary;
  /** Dictionary file */
  protected File dictionaryFile;
  /** Dictionary MLF */
  protected File dictionaryMlf;
  /** Phoneme list */
  protected Set<String> phonemeList;
  /** WDNET file */
  protected File wdnet;
  /** Phoneme list */
  protected File phonemeListFile;
  /** Phoneme list without SP (short pause) */
  protected File phonemeListNoSpFile;
  /** Phoneme list without SP (short pause) */
  protected File phonemesMlf;
  /** Phoneme list with SP (short pause) MLF */
  protected File phonemesWithSpMlf;
  /** Pause markers */
  protected HashMap<String,String> htPauseMarkers;
  /** Compiled noise patterns */
  protected HashMap<String,Pattern> noisePatternsMap;
  /** Left channel participant pattern */
  protected Pattern leftPatternRegex;
  /** Right channel participant pattern */
  protected Pattern rightPatternRegex;
  /** Triphone list file */
  protected File triphoneListFile;
  /** Triphone MLF */
  protected File triphonesMlf;
  /** Stats file */
  protected File statsFile;
  
  /**
   * Reset any current state associated with past alignment sessions.
   */
  public void reset() {
    sessionWorkingDir = null;
    logFile = null;
    grammar = null;
    wordsMlf = null;
    alignedWordsMlf = null;
    alignedPhonesMlf = null;
    scp = null;
    trainingScp = null;
    dictionary = null;
    dictionaryFile = null;
    dictionaryMlf = null;
    phonemeList = null;
    wdnet = null;
    phonemeListFile = null;
    phonemeListNoSpFile = null;
    phonemesMlf = null;
    phonemesWithSpMlf = null;
    htPauseMarkers = null;
    noisePatternsMap = null;
    leftPatternRegex = null;
    rightPatternRegex = null;
    triphoneListFile = null;
    triphonesMlf = null;
    statsFile = null;
    // load htkPath from config file
    getConfig();
  } // end of reset()

  /**
   * Sets up the initial file structure - i.e. copies fixed file templates and creates
   * required hmm subdirectories.  
   * @throws IOException
   * @throws TransformationException
   */
  public void createInitialFileStructure() throws IOException, TransformationException {
    setStatus("Creating initial file structure...");   
	 
    // create the hmm folders
    DecimalFormat formatter = new DecimalFormat("hmm00");
    for (int i = 0; i <= 15; i++) {
      File dirHmm = new File(sessionWorkingDir, formatter.format(i));
      setStatus("Creating folder: " + dirHmm.getPath());
      if (!dirHmm.mkdir()) {
        throw new TransformationException(
          this, "Failed to create working sub-directory: "+dirHmm.getPath());
      }
    } 
    
    // copy files standard files into our working directory
    // should be:
    //  mkphones0.led
    //  config0
    //  config
    //  proto
    //  sil.hed
    //  mkphones1.led
    //  mktri.led
    
    File[] aFiles = new File(getWorkingDirectory(), "scripts").listFiles();
    for (File theFile : aFiles) {
      if (theFile.isFile()) { // no directories
        // copy the file
        File destination = new File (sessionWorkingDir, theFile.getName());
        IO.Copy(theFile, destination);
      } // not a directory
    } // next file

    setStatus("Finished creating initial file structure.");
  } // end of createInitialFileStructure()

  /**
   * Provides the working directory for this training session.
   * @return The working directory for temporary files.
   * @throws TransformationException if the directory couldn't be created.
   */
  protected File createSessionWorkingDir() throws TransformationException{
    if (sessionName == null) {
      sessionName = "htk-" + hashCode();
    }
    setStatus("Session " + sessionName);
    sessionWorkingDir = new File(
      getWorkingDirectory(),
      sessionName + "-"
      + new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss").format(new java.util.Date()));
    if (!sessionWorkingDir.mkdirs()) {
      throw new TransformationException(
        this, "Failed to create working directory: " + sessionWorkingDir.getPath());
    }
    boolean addStatusObserverForLog = logFile == null; // might have already done this
    logFile = new File(getWorkingDirectory(), sessionWorkingDir.getName() + ".log");
    if (addStatusObserverForLog) {
      getStatusObservers().add(status -> {
          if (logFile != null) {
            try {
              PrintWriter out = new PrintWriter(new FileOutputStream(logFile, true));
              out.println(status);
              out.close();
            } catch(IOException exception) {
            }
          } // logFile still set
        });
    }
    try {
      PrintWriter out = new PrintWriter(logFile);
      out.println("HTKAligner log: " + sessionName);
      out.close();
    } catch(IOException exception) {
    }
    htk = new HTK(new File(htkPath), logFile);
    return sessionWorkingDir;
  } // end of getSessionWorkingDir()

  /**
   * Creates data files HTK needs for training.
   * @param graphs Original utterances to align.
   * @param phonemesToHtk Translates phoneme labels to HTK-compatible ones, if necessary.
   * @return The utterances successfully processed.
   * @throws TransformationException
   */
  public List<Graph> createInputFiles(Stream<Graph> graphs, PhonemeTranslator phonemesToHtk)
    throws TransformationException {
    try {
      // start grammar...
      
      // -all_utterances.gram
      // ( SENT-START ( 
      // <utterance> | <utterance> | <utterance>...
      // ) SENT-END )      
      grammar = new File(sessionWorkingDir, sessionName + ".gram");
      final BufferedWriter grammarOut = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(grammar), "UTF-8"));
      grammarOut.write("( SENT-START ( ");
      grammarOut.newLine();
      htPauseMarkers = new HashMap<String,String>();
      if (pauseMarkers != null) {
        for (String sPauseMarker : pauseMarkers.split(" ")) {
          if (sPauseMarker.trim().length() > 0) {
            htPauseMarkers.put(sPauseMarker, "SILENCE");
          }
        }
      }

      noisePatternsMap = new HashMap<String,Pattern>();
      if (noisePatterns != null) {
        StringTokenizer stNoisePatterns = new StringTokenizer(noisePatterns);
        while (stNoisePatterns.hasMoreTokens()) {
          String sPattern = stNoisePatterns.nextToken();
          String sName = sPattern.replaceAll("[^a-zA-Z0-9]","").toLowerCase();
          if (sName.length() == 0) sName = "noise";
          try {
            noisePatternsMap.put(sName, Pattern.compile(sPattern, Pattern.CASE_INSENSITIVE));
            setStatus("Noise pattern: " + sName + " = " + sPattern);
          } catch(Exception exception) {
            setStatus("Ignoring noise pattern: " + sName + " = " + sPattern + " : " + exception);
          }
        } // next pattern
      }
      
      leftPatternRegex = leftPattern == null?null:Pattern.compile(leftPattern);
      rightPatternRegex = rightPattern == null?null:Pattern.compile(rightPattern);
      
      // start MLF...
      wordsMlf = new File(sessionWorkingDir, sessionName + "_words.mlf");
      final BufferedWriter mlfOut = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(wordsMlf), "UTF-8"));
      mlfOut.write("#!MLF!#");
      mlfOut.newLine();
      
      // start SCPs
      scp = new File(sessionWorkingDir, sessionName + ".scp");
      final BufferedWriter scpOut = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(scp), "UTF-8"));
      trainingScp = new File(sessionWorkingDir, sessionName + "_train.scp");
      final BufferedWriter trainingScpOut = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(trainingScp), "UTF-8"));
      final String sPath = sessionWorkingDir.getPath() + File.separator;
      
      final DecimalFormat formatter = new DecimalFormat("0.0000");
      
      // start dictionary
      dictionary = new TreeMap<String,LinkedHashSet<String>>();
      
      Vector<Graph> utterances = new Vector<Graph>();
      
      graphs.forEach(fragment -> {
          try {
            
            // unaligned?
            if (fragment.getStart().getOffset() == null) {
              setStatus(
                "Fragment has an undefined start time and will be ignored: \""
                +fragment.getId()+"\"");
              return;
            }
            if (fragment.getEnd().getOffset() == null) {
              setStatus(
                "Fragment has an undefined end time and will be ignored: \""
                +fragment.getId()+"\"");
              return;
            }
            
            // simultaneous speech?
            if (overlapThreshold != null) {
              if (getStore() == null) {
                setStatus("No access to graph store, so simultaneous speech cannot be detected.");
              } else {
                Annotation utterance = fragment.first(schema.getUtteranceLayerId());
                double dAnnotationDuration = utterance.getDuration();
                // get all other utterances that overlap with this one
                String query = "graph.id == '"+esc(fragment.sourceGraph().getId())+"'"
                  +" && layer.id == '"+esc(schema.getUtteranceLayerId())+"'"
                  +" && start.offset <= " + utterance.getEnd().getOffset()
                  +" && end.offset >= " + utterance.getStart().getOffset()
                  +" && id <> '"+utterance.getId()+"'";
                Annotation[] overlappingUtterances = getStore().getMatchingAnnotations(query);
                // collect them all into a graph for comparing anchor offsets
                Graph g = new Graph(); 
                HashSet<String> anchorIds = new HashSet<String>();
                for (Annotation other : overlappingUtterances) {
                  g.addAnnotation(other);
                  anchorIds.add(other.getStartId());
                  anchorIds.add(other.getEndId());
                }
                Anchor[] anchors = getStore().getAnchors(
                  fragment.sourceGraph().getId(),
                  anchorIds.toArray(new String[anchorIds.size()]));
                for (Anchor anchor : anchors) g.addAnchor(anchor);
                
                // look for an overlap that's greater than the threshold
                for (Annotation other : overlappingUtterances) {
                  double dOverlap = -other.distance(utterance);
                  if (dOverlap / dAnnotationDuration > overlapThreshold / 100.0) {
                    setStatus(
                      "Fragment has " + formatter.format(dOverlap)
                      + "s overlap with other speakers"+
                      " ("+other.getLabel()+" "+other.getStart()+"-"+other.getEnd()+")"
                      +" and will be ignored: " + "\"" + fragment + "\"");
                    return;
                  } // overlap over threshold
                } // next overlapping utterance
              } // graph store is set
            } // overlapThreshold
            
            StringBuilder utteranceOrthography = new StringBuilder();
            boolean bJustAddedNoise = false;
            
            Annotation[] words = fragment.all(schema.getWordLayerId());
            // check for no words
            if (words.length == 0) {
              setStatus(
                "Fragment has no words and will be ignored: \"" + fragment.getId() + "\"");
              return;
            }
            
            // check whether the whole transcript has audio
            // (we don't ask the fragment directly for audio, because that would create a
            //  new sample file, and we don't want it yet)
            String audioUrl = fragment.sourceGraph().getMediaProvider().getMedia("", "audio/wav");
            if (audioUrl == null) {
              setStatus("Fragment has no media available and will be ignored: \""+fragment+"\"");
              return;
            } 
            
            // for each word
            for (Annotation word : words) {
              
              // check for orth/pron
              Annotation orthography = word.first(orthographyLayerId);
              Annotation[] pronunciations = word.all(pronunciationLayerId);
              if (pronunciations.length == 0) {
                setStatus(
                  "Fragment contains unknown word \"" + orthography.getLabel()
                  + "\" and will be ignored: \"" + fragment.getId() + "\"");
                return;
              }
              if (orthography != null
                  && orthography.getLabel() != null
                  && orthography.getLabel().length() > 0) {
                
                // add word (and neighboring noises/pause markers) to the utterance
                bJustAddedNoise = addWordToUtterance(
                  utteranceOrthography, bJustAddedNoise, orthography);
                
                // dictionary...
                
                // is the word already in the dictionary?
                if (!dictionary.containsKey(orthography.getLabel())) {
                  dictionary.put(orthography.getLabel(), new LinkedHashSet<String>());
                }
                
                Set<String> prons = dictionary.get(orthography.getLabel());
                // for each pron
                for (Annotation pronunciation : pronunciations) {
                  String sPhonology = pronunciation.getLabel();
                  if (!prons.contains(sPhonology)) {
                    prons.add(phonemesToHtk.apply(sPhonology));
                  }
                } // next pronunciation
                
              } // orth/pron are present
            } // next word
            
            if (utteranceOrthography.toString().trim().length() == 0) {
              // don't put blank utterances in grammar or MLF
              setStatus(
                "Fragment has no orthography and will be ignored: " + "\"" + fragment + "\"");
              return;
            }
            
            // write line to grammar                     
            grammarOut.newLine();
            if (utterances.size() > 0 ) grammarOut.write(" | ");
            grammarOut.write(utteranceOrthography.toString());

            // write mlf
            mlfOut.write("\"*/" + fragment.getId() + ".lab\"");
            for (String token : utteranceOrthography.toString().split(" ")) {
              mlfOut.newLine();
              mlfOut.write(token);
            } // next work token
            mlfOut.newLine();
            mlfOut.write(".");                  
            mlfOut.newLine();
            
            // write line to SCPs
            scpOut.write(sPath + fragment.getId() + ".wav " + fragment.getId() + ".mfc");
            scpOut.newLine();
            trainingScpOut.write("\"" + sPath + fragment.getId() + ".mfc\"");
            trainingScpOut.newLine();
            
            // extract audio...                     
            extractAudio(fragment, schema);

            utterances.add(fragment);            
            
          } catch (Exception x) {
            setStatus("Error processing fragment : \"" + fragment + "\" : " + x);
            // TODO?? setLastException(x);
          }
        }); // next utterance
      
      // finish grammar
      grammarOut.newLine();
      grammarOut.write(") SENT-END )");
      grammarOut.newLine();
      grammarOut.close();
      
      // finish mlf
      mlfOut.close();
      
      // finish SCPs
      scpOut.close();
      trainingScpOut.close();
      
      // write dictionaries
      dictionaryFile = new File(sessionWorkingDir, sessionName + ".dict");
      dictionaryMlf = new File(sessionWorkingDir, sessionName + "_dict.mlf");
      BufferedWriter out = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(dictionaryFile), "UTF-8"));
      out.write("SENT-END\tsil");
      out.newLine();
      out.write("SENT-START\tsil");
      out.newLine();
      out.write("SILENCE\tsil");
      out.newLine();
      for (String sName : noisePatternsMap.keySet()) {
        out.write(sName.toUpperCase() + "\t" + sName);
        out.newLine();
      }
      
      BufferedWriter outMlf = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(dictionaryMlf), "UTF-8"));
      outMlf.write("#!MLF!#");
      outMlf.newLine();
      
      // build a phoneme list as we go 
      phonemeList = new TreeSet<String>();
      
      for (String sWord : dictionary.keySet()) {
        if (isCancelling()) break;
        Set<String> pronunciations = dictionary.get(sWord);
        
        // for each pronunciation
        if (pronunciations.size() == 0) {
          throw new TransformationException(
            this, "The word '" + sWord + "' has no pronunciation defined");
        }
        for (String sPronunciation : pronunciations) {
          if (isCancelling()) break;
          
          // format the pronunciation
          String sEntry = sPronunciation;
          // and that the phonemes are separated by spaces
          StringTokenizer phonemes = new StringTokenizer(sEntry, " ");
          while (phonemes.hasMoreTokens()) phonemeList.add(phonemes.nextToken());
          
          out.write(sWord + "\t" + sEntry + " sp");
          out.newLine();
          
          outMlf.write("\"*/" + sWord + ".lab\"");
          outMlf.newLine();
          outMlf.write("sil");
          outMlf.newLine();
          StringTokenizer tokens = new StringTokenizer(sEntry, " ");
          while (tokens.hasMoreTokens()) {
            outMlf.write(tokens.nextToken());
            outMlf.newLine();
          } // next phoneme
          outMlf.write("sp");
          outMlf.newLine();
          outMlf.write("sil");
          outMlf.newLine();
          outMlf.write(".");
          outMlf.newLine();
          
        } // next pronunciation
      } // next word
      
      out.close();
      outMlf.close();
      
      return utterances;
    } catch (Exception x) {
      throw new TransformationException(this, x);
    }
  } // end of createInputFiles()
   
  /**
   * Add the given word, and any neighbouring noise/pause markers, to the given utterance
   * transcript buffer. Also prefixes digit-initial word with underscore.
   * @param utteranceOrthography
   * @param bJustAddedNoise
   * @param orthography
   * @param pauseMarkers
   * @return true if a noise annotation was the last thing added, false otherwise.
   */
  public boolean addWordToUtterance(
    StringBuilder utteranceOrthography, boolean bJustAddedNoise, Annotation orthography) {
    
    Annotation word = orthographyLayerId.equals(schema.getWordLayerId())?
      orthography:orthography.getParent();
    
    // digit-initial word with underscore
    if (Character.isDigit(orthography.getLabel().charAt(0))) {
      // prefix it with underscore
      orthography.setLabel("_" + orthography.getLabel());
    }
    
    // accumulate utterance transcipt...
    
    if (!bJustAddedNoise) {
      if (noiseLayerId != null) {
        // look for noise annotations to prepend
        LinkedHashSet<Annotation> vNoises = word.getStart().endOf(noiseLayerId);
        vNoises.addAll(word.getStart().startOf(noiseLayerId));
        for (Annotation anNoise : vNoises) {
          for (String sName : noisePatternsMap.keySet()) {
            if (noisePatternsMap.get(sName).matcher(anNoise.getLabel()).matches()) {
              if (utteranceOrthography.length() > 0) utteranceOrthography.append(" ");
              utteranceOrthography.append(sName.toUpperCase());
              break;
            } 
          } // next pattern
        } // next noise annotation
      }
    } // noiseLayerId configured
    bJustAddedNoise = false;
    
    if (utteranceOrthography.length() > 0) utteranceOrthography.append(" ");
    utteranceOrthography.append(orthography.getLabel());
    
    // look for pause markers
    for (String sMarker : htPauseMarkers.keySet()) {
      if (word.getLabel().endsWith(sMarker)) {
        utteranceOrthography.append(" ");
        utteranceOrthography.append(htPauseMarkers.get(sMarker));
      }
    } // next pause marker
    
    LinkedHashSet<Annotation> vNoises = word.getEnd().startOf(noiseLayerId);
    for (Annotation anNoise : vNoises) {
      // look for noise annotations to append
      for (String sName : noisePatternsMap.keySet()) {
        if (noisePatternsMap.get(sName).matcher(anNoise.getLabel()).matches()) {
          utteranceOrthography.append(" ");
          utteranceOrthography.append(sName.toUpperCase());
          bJustAddedNoise = true;
          break;
        }
      } // next pattern
    } // next noise annotation
    return bJustAddedNoise;
  } // end of addWordToUtterance()
  
  /**
   * Extracts audio features for the given utterance.
   * @param fragment
   * @param schema
   * @throws TransformationException
   */
  public void extractAudio(Graph fragment, Schema schema) throws TransformationException {

    try {
      File fTarget = new File(sessionWorkingDir, fragment.getId() + ".mfc");

      double dStartTime = fragment.getStart().getOffset();
      if (dStartTime < 0) dStartTime = 0;
      double dEndTime = fragment.getEnd().getOffset();
      
      String channel = "";
      if (leftPatternRegex != null || rightPatternRegex != null) {
        if (leftPatternRegex != null
            && leftPatternRegex.matcher(
              fragment.first(schema.getUtteranceLayerId()).getLabel()).matches()) {
          channel = "left";
        } else if (rightPatternRegex != null 
                   && rightPatternRegex.matcher(
                     fragment.first(schema.getUtteranceLayerId()).getLabel()).matches()) {
          channel = "right";
        }
      }
      
      String fileUrl = fragment.getMediaProvider().getMedia(
        "", "audio/wav" + (sampleRate == null?"":"; samplerate="+sampleRate));
      File fTemp = new File(new URI(fileUrl));
      File fWav = new File(
        fTarget.getParent(), fragment.getId() + "." + IO.Extension(fTemp));
      if (fragment.isFragment()) { // fragment media is generated on the fly, so can be moved
        IO.Rename(fTemp, fWav);
      } else { // whole graph media is used in-situ (this should happen, but just in case...)
        fWav = fTemp;
      }
      
      // convert WAV to MFCC
      int r = 99;
      File fConfig = null;
      setStatus(
        "Extracting features from \"" + fWav.getName() + "\" to \"" + fTarget.getName() + "\""
        +(channel.length()==0?"":" - channel: " + channel));
      if (useP2FA) {
        fConfig = new File(getP2FAModelDirectory(), "config");
      } else if (channel.length() != 0) {
        // need to pass a config file for specifying the channel to use
        fConfig = new File(sessionWorkingDir, "config"+channel);
      }
      if (fConfig != null) {
        r = htk.HCopy(fConfig, "WAV", fWav, fTarget);
      } else {
        r = htk.HCopy("WAV", fWav, fTarget);
      }
      if (r != 0) {
        throw new TransformationException(
          this, "HCopy returned: " + r + " - " + htk.getLastError());
      }
      //fWav.delete();
    } catch (Exception x) {
      throw new TransformationException(this, x);
    }
  } // end of extractAudio()
  
  /**
   * Get the directory that the P2FA files are in.
   * @return The directory that the P2FA files are in.
   */
  protected File getP2FADirectory() {
    return new File(getWorkingDirectory(), "p2fa");
  } // end of getP2FAModelDirectory()
  
  /**
   * Step 1 - the Task Grammar. 
   * <p> HTK provides a grammar definition language for specifying simple task grammars
   * such as this. It consists of a set of variable definitions followed by a regular expression
   * describing the words to recognise.
   * <p> The HTK recogniser actually requires a word network to be defined using a low
   * level notation called HTK Standard Lattice Format (SLF) in which each word instance
   * and each word-to-word transition is listed explicitly. This word network can be
   * created automatically from the grammar above using the HPARSE tool.
   * @throws TransformationException
   */
  public void step1() throws TransformationException {
    try {
      setStatus("Step 1...");
      wdnet = new File(sessionWorkingDir, sessionName + ".wdnet");
      setStatus("Calling HParse");
      int r = htk.HParse(grammar, wdnet);
      if (r != 0) {
        throw new TransformationException(
          this, "HParse returned: " + r + " - " + htk.getLastError());
      }
      setStatus("Finished step 1.");
    } catch (Exception x) {
      throw new TransformationException(this, x);
    }
  } // end of step1()

  /**
   * Step 2 - the Dictionary. 
   * <p> The first step in building a dictionary is to create a sorted list of the
   * required words. The desired training word list (wlist) could then be extracted
   * automatically from these. The dictionary itself can be built from a standard source
   * using HDMAN. The general format of each dictionary entry is <br>
   * <tt>WORD [outsym] p1 p2 p3 ....</tt> <br>
   * which means that the word WORD is pronounced as the sequence of phones p1 p2 p3
   * .... The string in square brackets specifies the string to output when that word is
   * recognised. If it is omitted then the word itself is output. If it is included but
   * empty, then nothing is output.
   * @throws TransformationException
   */
  public void step2() throws TransformationException {
    setStatus("Step 2...");
    try {
      // HDMan -m -n CarlaBaigent.mph -l dlog CarlaBaigent_.dict CarlaBaigent.dict
      // produces a phoneme list, but we can do it here:
      
      phonemeListFile = new File(sessionWorkingDir, sessionName + ".mph");
      phonemeListNoSpFile = new File(sessionWorkingDir, sessionName + "_no_sp.mph");
      setStatus("Creating phoneme list...");
      
      BufferedWriter out = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(phonemeListFile), "UTF-8"));
      out.write("sil");
      out.newLine();
      out.write("sp");
      out.newLine();
      for (String sName : noisePatternsMap.keySet()) {
        out.write(sName);
        out.newLine();
      }
      BufferedWriter outNoSp = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(phonemeListNoSpFile), "UTF-8"));
      outNoSp.write("sil");
      outNoSp.newLine();
      for (String sName : noisePatternsMap.keySet()) {
        outNoSp.write(sName);
        outNoSp.newLine();
      }
      
      for (String sPhoneme : phonemeList) {
        if (isCancelling()) break;
        out.write(sPhoneme);
        out.newLine();
        outNoSp.write(sPhoneme);
        outNoSp.newLine();
      } // next phoneme
      out.close();
      outNoSp.close();
      
      setStatus("Finished step 2.");
    } catch (Exception x) {
      throw new TransformationException(this, x);
    }
  } // end of step2()

  /**
   * Step 4 - Creating the Transcription Files. 
   * To train a set of HMMs, every file of training data must have an associated phone
   * level transcription. Since there is no hand labelled data to bootstrap a set of
   * models, a flat-start scheme will be used instead. To do this, two sets of phone
   * transcriptions will be needed. The set used initially will have no short-pause (sp)
   * models between words. Then once reasonable phone models have been generated, an sp
   * model will be inserted between words to take care of any pauses introduced by the speaker. 
   * <p> The starting point for both sets of phone transcription is an orthographic
   * transcription in HTK label format. This can be created fairly easily using a text
   * editor or a scripting language.
   * <p> The prompt labels need to be converted into path names, each word should be
   * written on a single line and each utterance should be terminated by a single period
   * on its own. The first line of the file just identifies the file as a Master Label
   * File (MLF). This is a single file containing a complete set of transcriptions. HTK
   * allows each individual transcription to be stored in its own file but it is more
   * efficient to use an MLF.
   * <p> Once the word level MLF has been created, phone level MLFs can be generated using
   * the label editor HLED. 
   * @throws TransformationException
   */
  public void step4() throws TransformationException {
    setStatus("Step 4...");
    // HLEd -l * -d CarlaBaigent.dict -i CarlaBaigent_phones.mlf mkphones0.led CarlaBaigent_words.mlf
    try {
      phonemesMlf = new File(sessionWorkingDir, sessionName + "_phones.mlf");
      File mkphones0 = new File(sessionWorkingDir, "mkphones0.led");
	 
      setStatus("Calling HLEd...");
      int r = htk.HLEd("d", dictionaryFile, phonemesMlf, mkphones0, wordsMlf);
      if (r != 0) {
        throw new TransformationException(this, "HLEd returned: " + r + " - " + htk.getLastError());
      }
      
      setStatus("Finished step 4.");
    } catch (InterruptedException x) {
      throw new TransformationException(this, x);
    } catch (IOException x) {
      throw new TransformationException(this, x);
    }
  } // end of step4()

  /**
   * Step 6 - Creating Flat Start Monophones. 
   * <p> The first step in HMM training is to define a prototype model. The parameters of
   * this model are not important, its purpose is to define the model topology. For
   * phone-based systems, a good topology to use is 3-state left-right with no skips.
   * <p> The HTK tool HCOMPV will scan a set of data files, compute the global mean and
   * variance and set all of the Gaussians in a given HMM to have the same mean and variance.
   * <p> HCOMPV has a number of options specified for it. The -f option causes a variance
   * floor macro (called vFloors) to be generated which is equal to 0.01 times the global
   * variance. This is a vector of values which will be used to set a floor on the
   * variances estimated in the subsequent steps. The -m option asks for means to be
   * computed as well as variances. Given this new prototype model stored in the directory
   * hmm0, a Master Macro File (MMF) called hmmdefs containing a copy for each of the
   * required monophone HMMs is constructed by manually copying the prototype and
   * relabeling it for each required monophone (including "sil"). The format of an MMF is
   * similar to that of an MLF and it serves a similar purpose in that it avoids having a
   * large number of individual HMM definition files. 
   * <p> The flat start monophones stored in the directory hmm0 are re-estimated using the
   * embedded re-estimation tool HEREST 
   * <p> Each time HEREST is run it performs a single re-estimation. Each new HMM set is
   * stored in a new directory. Execution of HEREST should be repeated twice more,
   * changing the name of the input and output directories (set with the options -H and
   * -M) each time, until the directory hmm3 contains the final set of initialised
   * monophone HMMs.
   * @throws TransformationException
   */
  public void step6() throws TransformationException {
    setStatus("Step 6...");

    try {
      File config = new File(sessionWorkingDir, "config");
      File hmm00 = new File(sessionWorkingDir, "hmm00");
      File proto = new File(sessionWorkingDir, "proto");
      
      setStatus("Calling HCompV...");
      int r = htk.HCompV(config, 0.01, trainingScp, hmm00, proto);
      if (r != 0) {
        throw new TransformationException(
          this, "HCompV returned: " + r + " - " + htk.getLastError());
      }
      
      if (isCancelling()) return;
      
      setStatus("Creating macros and hmmdefs");
      
      proto = new File(hmm00, "proto"); 
      BufferedReader in = new BufferedReader(
        new InputStreamReader(new FileInputStream(proto), "UTF-8"));
      File vFloors = new File(hmm00, "vFloors");
      BufferedReader inVFloors = new BufferedReader(
        new InputStreamReader(new FileInputStream(vFloors), "UTF-8"));
      
      // macros is everything in the new proto up to the line that starts ~h
      File macros = new File(hmm00, "macros");
      BufferedWriter out = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(macros), "UTF-8"));
      String sLine = in.readLine();
      while (sLine != null && !sLine.startsWith("~h") && !isCancelling()) {
        out.write(sLine);
        out.newLine();
        sLine = in.readLine();
      }
      // ...plus the contents of vFloors
      sLine = inVFloors.readLine();
      while (sLine != null && !isCancelling()) {
        out.write(sLine);
        out.newLine();
        sLine = inVFloors.readLine();
      }
      out.close();
      
      // macros is the rest of proto, repeated for each monophone
      String sHmm = "";
      sLine = in.readLine();
      while (sLine != null && !isCancelling()) {
        sHmm += sLine + "\r\n";
        sLine = in.readLine();
      }
      File hmmdefs = new File(hmm00, "hmmdefs");
      out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(hmmdefs), "UTF-8"));
      // for each monophone
      out.write("~h \"sil\""); // including sil
      out.newLine();
      out.write(sHmm);
      for (String sName : noisePatternsMap.keySet()) {
        out.write("~h \""+sName+"\""); // including noise
        out.newLine();
        out.write(sHmm);
      }
      for (String sPhoneme : phonemeList) {
        if (isCancelling()) break;
        out.write("~h \"" + sPhoneme + "\"");
        out.newLine();
        out.write(sHmm);
      } // next phoneme
      out.close();
      
      if (isCancelling()) return;
      
      // Training...
      setStatus("Calling HERest for hmm01...");
      File hmm01 = new File(sessionWorkingDir, "hmm01");
      r = htk.HERest(
        config, phonemesMlf, 250, 150, 1000, trainingScp, macros, hmmdefs, hmm01,
        phonemeListNoSpFile);
      if (r != 0) {
        throw new TransformationException(
          this, "HERest returned: " + r + " - " + htk.getLastError());
      }
      
      if (isCancelling()) return;
      
      setStatus("Calling HERest for hmm02...");
      hmmdefs = new File(hmm01, "hmmdefs");
      macros = new File(hmm01, "macros");
      File hmm02 = new File(sessionWorkingDir, "hmm02");
      r = htk.HERest(
        config, phonemesMlf, 250, 150, 1000, trainingScp, macros, hmmdefs, hmm02,
        phonemeListNoSpFile);
      if (r != 0) {
        throw new TransformationException(
          this, "HERest returned: " + r + " - " + htk.getLastError());
      }
      
      if (isCancelling()) return;
      
      setStatus("Calling HERest for hmm03...");
      hmmdefs = new File(hmm02, "hmmdefs");
      macros = new File(hmm02, "macros");
      File hmm03 = new File(sessionWorkingDir, "hmm03");
      r = htk.HERest(
        config, phonemesMlf, 250, 150, 1000, trainingScp, macros, hmmdefs, hmm03,
        phonemeListNoSpFile);
      if (r != 0) {
        throw new TransformationException(
          this, "HERest returned: " + r + " - " + htk.getLastError());
      }
      
      setStatus("Finished step 6.");
    } catch (InterruptedException x) {
      throw new TransformationException(this, x);
    } catch (IOException x) {
      throw new TransformationException(this, x);
    }
  } // end of step6()

  /**
   * Step 7 - Fixing the Silence Models.
   * <p> The previous step has generated a 3 state left-to-right HMM for each phone and
   * also a HMM for the silence model sil. The next step is to add extra transitions from
   * states 2 to 4 and from states 4 to 2 in the silence model. The idea here is to make
   * the model more robust by allowing individual states to absorb the various impulsive
   * noises in the training data. The backward skip allows this to happen without
   * committing the model to transit to the following word.
   * <p> Also, at this point, a 1 state short pause sp model should be created. This
   * should be a so-called tee-model which has a direct transition from entry to exit
   * node. This sp has its emitting state tied to the centre state of the silence model. 
   * <p>These silence models can be created in two stages:
   * <ul>
   *  <li> Use a text editor on the file hmm3/hmmdefs to copy the centre state of the sil
   *       model to make a new sp model and store the resulting MMF hmmdefs, which
   *       includes the new sp model, in the new directory hmm4. </li> 
   *  <li> Run the HMM editor HHED to add the extra transitions required and tie the sp
   *       state to the centre sil state </li> 
   * </ul>
   * HHED works in a similar way to HLED. It applies a set of commands in a script to
   * modify a set of HMMs. 
   * <p> Finally, another two passes of HEREST are applied using the phone transcriptions
   * with sp models between words. This leaves the set of monophone HMMs created so far in
   * the directory hmm7. 
   * @throws TransformationException
   */
  public void step7() throws TransformationException {
    setStatus("Step 7...");
    try {
      
      File config = new File(sessionWorkingDir, "config");
      File hmm03 = new File(sessionWorkingDir, "hmm03");
      File hmm04 = new File(sessionWorkingDir, "hmm04");
      File hmm05 = new File(sessionWorkingDir, "hmm05");
      File hmm06 = new File(sessionWorkingDir, "hmm06");
      File hmm07 = new File(sessionWorkingDir, "hmm07");
      
      setStatus("Copying macros...");
      File macros = new File(hmm04, "macros");
      IO.Copy(new File(hmm03, "macros"), macros);
      
      setStatus("Editing hmmdefs");
      File hmmdefs = new File(hmm04, "hmmdefs");
      addSpModelToHmmdefs(new File(hmm03, "hmmdefs"), hmmdefs);
      
      if (isCancelling()) return;
      
      setStatus("Calling HHEd...");
      File silhed = new File(sessionWorkingDir, "sil.hed");
      int r = htk.HHEd(macros, hmmdefs, hmm05, silhed, phonemeListFile);
      if (r != 0) {
        throw new TransformationException(
          this, "HHEd returned: " + r + " - " + htk.getLastError());
      }
      
      if (isCancelling()) return;
      
      setStatus("Calling HLEd...");
      phonemesWithSpMlf = new File(sessionWorkingDir, sessionName + "_phones_with_sp.mlf");
      File mkphones1 = new File(sessionWorkingDir, "mkphones1.led");
      r = htk.HLEd("d", dictionaryFile, phonemesWithSpMlf, mkphones1, wordsMlf);
      if (r != 0) {
        throw new TransformationException(
          this, "HLEd returned: " + r + " - " + htk.getLastError());
      }
      
      if (isCancelling()) return;
      
      // Training...
      setStatus("Calling HERest for hmm06...");
      hmmdefs = new File(hmm05, "hmmdefs");
      macros = new File(hmm05, "macros");
      r = htk.HERest(
        config, phonemesWithSpMlf, 250, 150, 1000, trainingScp, macros, hmmdefs, hmm06,
        phonemeListFile);
      if (r != 0) {
        throw new TransformationException(
          this, "HERest returned: " + r + " - " + htk.getLastError());
      }
      
      if (isCancelling()) return;
      
      setStatus("Calling HERest for hmm07...");
      hmmdefs = new File(hmm06, "hmmdefs");
      macros = new File(hmm06, "macros");
      r = htk.HERest(
        config, phonemesWithSpMlf, 250, 150, 1000, trainingScp, macros, hmmdefs, hmm07,
        phonemeListFile);
      if (r != 0) {
        throw new TransformationException(
          this, "HERest returned: " + r + " - " + htk.getLastError());
      }
      
      setStatus("Finished step 7.");
      
    } catch (InterruptedException x) {
      throw new TransformationException(this, x);
    } catch (IOException x) {
      throw new TransformationException(this, x);
    }
  } // end of step7()
  
  /**
   * Copies the hmmdefs file, adding in a sp model based on the sil model.
   * @param fSource
   * @param fDestination
   * @throws IOException
   */
  public void addSpModelToHmmdefs(File fSource, File fDestination) throws IOException {
    // fDestination is a copy of fSource, except:
    // the sil definition is copied to a new sp definition
    // but edited to become a 3-state matrix
    // copying state 3 to be state 2
    BufferedReader in = new BufferedReader(
      new InputStreamReader(new FileInputStream(fSource), "UTF-8"));
    BufferedWriter out = new BufferedWriter(
      new OutputStreamWriter(new FileOutputStream(fDestination), "UTF-8"));

    // copy across all matrices, looking for ~h "sil"
    String sLine = in.readLine();
    while (sLine != null && !isCancelling()) {
      if (!sLine.equals("~h \"sil\"")) {
        // just copy the line through
        out.write(sLine);
        out.newLine();
      } else { // process the sil lines
        // hit ~h "sil"
        // start the short-pause matrix
        String sSpMatrix = "~h \"sp\"\r\n";
        boolean bIgnoringLines = false;
	
        // copy the line through
        out.write(sLine);
        out.newLine();
        sLine = in.readLine();
        
        // until EOF or the end of the matrix
        while (sLine != null && !sLine.equalsIgnoreCase("<ENDHMM>") && !isCancelling()) {
          // copy the lines through
          out.write(sLine);
          out.newLine();
          
          // look for state
          if (sLine.equalsIgnoreCase("<NUMSTATES> 5")) {
            sSpMatrix += "<NUMSTATES> 3\r\n";
          } else if (sLine.equalsIgnoreCase("<STATE> 2")) {
            bIgnoringLines = true;
          } else if (sLine.equalsIgnoreCase("<STATE> 3")) {
            sSpMatrix += "<STATE> 2\r\n";
          } else if (sLine.equalsIgnoreCase("<STATE> 4")) {
            bIgnoringLines = true;
          } else if (sLine.equalsIgnoreCase("<TRANSP> 5")) {
            bIgnoringLines = true;
            sSpMatrix += "<TRANSP> 3\r\n";
            sSpMatrix += "  0.0 1.0 0.0\r\n";
            sSpMatrix += "  0.0 0.6 0.4\r\n";
            sSpMatrix += "  0.0 0.0 0.6\r\n";
          } else if (bIgnoringLines && sLine.startsWith("<GCONST>")) {
            bIgnoringLines = false;
          } else if (!bIgnoringLines) {
            // copy the line into the sp definition
            sSpMatrix += sLine + "\r\n";
          }

          sLine = in.readLine();
        } // next within sil
        
        if (sLine != null && sLine.length() > 0) {
          // make sure we don't drop any lines in the copying
          out.write(sLine);
          out.newLine();
        }

        // write the new sp matrix
        sSpMatrix += "<ENDHMM>";
        out.write(sSpMatrix);
        out.newLine();
	       
      } // process sil matrix
      sLine = in.readLine();
    } // next line
	 
    out.close();
  } // end of addSpModelToHmmdefs()

  /**
   * Step 8 - Realigning the Training Data. 
   * <p> As noted earlier, the dictionary contains multiple pronunciations for some words,
   * particularly function words. The phone models created so far can be used to realign
   * the training data and create new transcriptions. This can be done with a single
   * invocation of the HTK recognition tool HVITE
   * <p> This command uses the HMMs stored in hmm7 to transform the input word level
   * transcription words.mlf to the new phone level transcription aligned.mlf using the
   * pronunciations stored in the dictionary dict.The key difference between this
   * operation and the original word-to-phone mapping performed by HLED in step 4 is that
   * the recogniser considers all pronunciations for each word and outputs the
   * pronunciation that best matches the acoustic data.
   * <p> Once the new phone alignments have been created, another 2 passes of HEREST can
   * be applied to reestimate the HMM set parameters again. Assuming that this is done,
   * the final monophone HMM set will be stored in directory hmm9. 
   * @throws TransformationException
   */
  public void step8() throws TransformationException {
    setStatus("Step 8...");
    try {
	 
      File config = new File(sessionWorkingDir, "config");
      File hmm07 = new File(sessionWorkingDir, "hmm07");
      File hmm08 = new File(sessionWorkingDir, "hmm08");
      File hmm09 = new File(sessionWorkingDir, "hmm09");

      setStatus("Calling HVite...");
      File macros = new File(hmm07, "macros");
      File hmmdefs = new File(hmm07, "hmmdefs");
      alignedPhonesMlf = new File(sessionWorkingDir, sessionName + "_phones_aligned_raw.mlf");
      int r = htk.HVite(
        "SWT", "SILENCE", config, macros, hmmdefs, alignedPhonesMlf, 250.0, wordsMlf, trainingScp,
        dictionaryFile, phonemeListFile);
      if (r != 0) {
        String sError = htk.getLastError();
        // look for something like "Cannot find hmm [???-]PD[+???]"
        Pattern pCannotFindHmm = Pattern.compile(
          "Cannot find hmm \\[\\?\\?\\?-\\](.+)\\[\\+\\?\\?\\?\\]");
        Matcher mCannotFileHmm = pCannotFindHmm.matcher(sError);
        if (mCannotFileHmm.find()) {
          throw new TransformationException(
            this, "ERROR: HVite found a phone with no model: " + mCannotFileHmm.group(1));
        } else {
          throw new TransformationException(
            this, "ERROR: HVite returned: " + r + " - " + htk.getLastError());
        }
      }
    
      // The forced-alignment process can fail on some utterances
      // We have to scan the AlignedPhonemes MLF to look for missing
      // utterances and add them in from Phonemes MLF
      setStatus("Checking force-aligned phonetic transcript...");
      checkAlignedPhonemesMlf();

      if (isCancelling()) return;
    
      // Training...
      setStatus("Calling HERest for hmm08...");
      hmmdefs = new File(hmm07, "hmmdefs");
      macros = new File(hmm07, "macros");
      r = htk.HERest(
        config, alignedPhonesMlf, 250, 150, 1000, trainingScp, macros, hmmdefs, hmm08,
        phonemeListFile);
      if (r != 0) {
        throw new TransformationException(
          this, "HERest returned: " + r + " - " + htk.getLastError());
      }
    
      if (isCancelling()) return;

      setStatus("Calling HERest for hmm09...");
      hmmdefs = new File(hmm08, "hmmdefs");
      macros = new File(hmm08, "macros");
      r = htk.HERest(
        config, alignedPhonesMlf, 250, 150, 1000, trainingScp, macros, hmmdefs, hmm09,
        phonemeListFile);
      if (r != 0) {
        throw new TransformationException(
          this, "HERest returned: " + r + " - " + htk.getLastError());
      }

      setStatus("Finished step 8.");
      
    } catch (InterruptedException x) {
      throw new TransformationException(this, x);
    } catch (IOException x) {
      throw new TransformationException(this, x);
    }      
  } // end of step8()

  /**
   * Ensures that the Aligned Phoneme MLF has all the utterances.
   */
  public void checkAlignedPhonemesMlf() throws IOException {
    BufferedReader phonemesIn = new BufferedReader(
      new InputStreamReader(new FileInputStream(phonemesWithSpMlf), "UTF-8"));
    BufferedReader alignedPhonemesIn = new BufferedReader(
      new InputStreamReader(new FileInputStream(alignedPhonesMlf), "UTF-8"));
    File processedAlignedMlf = new File(sessionWorkingDir, sessionName + "_phones_aligned.mlf");
    BufferedWriter alignedPhonemesOut = new BufferedWriter(
      new OutputStreamWriter(new FileOutputStream(processedAlignedMlf), "UTF-8"));

    // skip the first line
    String sLine = phonemesIn.readLine();
    String sAlignedLine = alignedPhonemesIn.readLine();
    alignedPhonemesOut.write(sAlignedLine);
    alignedPhonemesOut.newLine();
    
    // read the files in parallel, looking for utterances
    // in phonemesIn that aren't in alignedPhonemesIn...
    
    sLine = phonemesIn.readLine();
    sAlignedLine = alignedPhonemesIn.readLine();
    while (sLine != null) {
      boolean bSkipAlignedLine = false;
      // this line must be an utterance name
      if (sLine.equals(sAlignedLine)) {
        // the aligned file has the utterance so copy it through
        while (!sAlignedLine.equals(".")) { // end of utterance
          alignedPhonemesOut.write(sAlignedLine);
          alignedPhonemesOut.newLine();
          sAlignedLine = alignedPhonemesIn.readLine();
        } // next line
        alignedPhonemesOut.write(sAlignedLine);
        alignedPhonemesOut.newLine();
        
        // skip past the utterance in the non-aligned file
        while (!sLine.equals(".")) { // end of utterance
          sLine = phonemesIn.readLine();
        } // next line
      } else { // this utterance is missing from the aligned file
        setStatus(
          "Force-alignment failed for " + sLine + " - copying utterance from "
          + phonemesWithSpMlf.getName());
        while (!sLine.equals(".")) { // end of utterance
          alignedPhonemesOut.write(sLine);
          alignedPhonemesOut.newLine();
          sLine = phonemesIn.readLine();
        } // next line
        alignedPhonemesOut.write(sLine);
        alignedPhonemesOut.newLine();
        
        bSkipAlignedLine = true;
      }
	    
      sLine = phonemesIn.readLine();
      if (!bSkipAlignedLine) {
        sAlignedLine = alignedPhonemesIn.readLine();
      }
    } // next utterance
    alignedPhonemesOut.close();
    alignedPhonemesIn.close();
    phonemesIn.close();

    alignedPhonesMlf = processedAlignedMlf;
  } // end of checkAlignedPhonemesMlf()

  /**
   * Step 9 - Making Triphones from Monophones.
   * <p> Context-dependent triphones can be made by simply cloning monophones and then
   * re-estimating using triphone transcriptions. The latter should be created first using
   * HLED because a side-effect is to generate a list of all the triphones for which there
   * is at least one example in the training data.
   * <p> That is, executing <br>
   * <tt> HLEd -n triphones1 -l '*' -i wintri.mlf mktri.led aligned.mlf </tt>
   * <br> will convert the monophone transcriptions in aligned.mlf to an equivalent set of
   * triphone transcriptions in wintri.mlf. At the same time, a list of triphones is
   * written to the file triphones1. The edit script mktri.led contains the commands <br>
   * <tt>WB sp <br>
   *     WB sil <br>
   *     TC</tt>
   * <p> The two WB commands define sp and sil as word boundary symbols. These then block
   * the addition of context in the TI command, seen in the following script, which
   * converts all phones (except word boundary symbols) to triphones . For example, <br>
   * <tt> sil th ih s sp m ae n sp ... </tt>
   * <br> becomes
   * <tt> sil th+ih th-ih+s ih-s sp m+ae m-ae+n ae-n sp ... </tt>
   * <p> This style of triphone transcription is referred to as word internal. Note that
   * some biphones will also be generated as contexts at word boundaries will sometimes
   * only include two phones. 
   * <p> The cloning of models can be done efficiently using the HMM editor HHED: <br>
   * <tt>HHEd -B -H hmm9/macros -H hmm9/hmmdefs -M hmm10
   *       mktri.hed monophones1 </tt><br>
   * where the edit script mktri.hed contains a clone command CL followed by TI commands
   * to tie all of the transition matrices in each triphone set, that is: <br>
   * <tt> CL triphones1<br>
   *      TI T_ah {(*-ah+*,ah+*,*-ah).transP} <br>
   *      TI T_ax {(*-ax+*,ax+*,*-ax).transP} <br>
   *      TI T_ey {(*-ey+*,ey+*,*-ey).transP} <br>
   *      TI T_b {(*-b+*,b+*,*-b).transP} <br>
   *      TI T_ay {(*-ay+*,ay+*,*-ay).transP} <br>
   * ...</tt>
   * <p> The file mktri.hed can be generated using the Perl script maketrihed included in
   * the HTKTutorial directory. When running the HHED command you will get warnings about
   * trying to tie transition matrices for the sil and sp models. Since neither model is
   * context-dependent there aren't actually any matrices to tie. 
   * <p> The clone command CL takes as its argument the name of the file containing the
   * list of triphones (and biphones) generated above. For each model of the form a-b+c in
   * this list, it looks for the monophone b and makes a copy of it. Each TI command takes
   * as its argument the name of a macro and a list of HMM components. The latter uses a
   * notation which attempts to mimic the hierarchical structure of the HMM parameter set
   * in which the transition matrix transP can be regarded as a sub-component of each
   * HMM. The list of items within brackets are patterns designed to match the set of
   * triphones, right biphones and left biphones for each phone. 
   * <p> Once the context-dependent models have been cloned, the new triphone set can be
   * re-estimated using HEREST. This is done as previously except that the monophone model
   * list is replaced by a triphone list and the triphone transcriptions are used in place
   * of the monophone transcriptions.  
   * <p> For the final pass of HEREST, the -s option should be used to generate a file of
   * state occupation statistics called stats. In combination with the means and
   * variances, these enable likelihoods to be calculated for clusters of states and are
   * needed during the state-clustering process described below. Re-estimation should be
   * again done twice, so that the resultant model sets will ultimately be saved in hmm12.  
   * @throws TransformationException
   */
  public void step9() throws TransformationException {
    setStatus("Step 9...");
    try {

      File config = new File(sessionWorkingDir, "config");
      File hmm09 = new File(sessionWorkingDir, "hmm09");
      File hmm10 = new File(sessionWorkingDir, "hmm10");
      File hmm11 = new File(sessionWorkingDir, "hmm11");
      File hmm12 = new File(sessionWorkingDir, "hmm12");

      triphoneListFile = new File(sessionWorkingDir, sessionName + ".tph");
      triphonesMlf = new File(sessionWorkingDir, sessionName + "_wintri.mlf");
      File mktri = new File(sessionWorkingDir, "mktri.led");
      File dictWintryMlf = new File(sessionWorkingDir, sessionName + "_dict_wintri.mlf");
    
      setStatus("Calling HLEd to generate wintri.mlf...");
      // HLEd -n "Ada Aitcheson.tph" -l * -i "Ada Aitcheson_wintri.mlf" mktri.led "Ada Aitcheson_phones_aligned.mlf"
      int r = htk.HLEd("n", triphoneListFile, triphonesMlf, mktri, alignedPhonesMlf);
      if (r != 0) {
        throw new TransformationException(this, "HLEd returned: " + r + " - " + htk.getLastError());
      }

      setStatus("Calling HLEd on dictionary MLF to generate complete triphone list in .tph...");
      // This is a deviation from the tutorial.  I've found that if the 
      // complete list of triphones isn't available now for HHed, it leads
      // to errors later where the toolkit changes its mind between 
      // alternative pronunciations of the same word.
      // So here we generate a complete list, from the dictionary MLF file
      // created in createDictionary() above
      r = htk.HLEd("n", triphoneListFile, dictWintryMlf, mktri, dictionaryMlf);
      if (r != 0) {
        throw new TransformationException(this, "HLEd returned: " + r + " - " + htk.getLastError());
      }

      setStatus("Generating mktri.hed...");
      File fMktriHed = generateMktriHed();

      // HHEd -B -H "hmm09\macros" -H "hmm09\hmmdefs" -M "hmm10" "mktri.hed" "Ada Aitcheson.mph"
      File macros = new File(hmm09, "macros");
      File hmmdefs = new File(hmm09, "hmmdefs");
      setStatus("Calling HHEd...");
      r = htk.HHEd(macros, hmmdefs, hmm10, fMktriHed, phonemeListFile);
      if (r != 0) {
        throw new TransformationException(this, "HHEd returned: " + r + " - " + htk.getLastError());
      }
    
      // HERest -C "config" -I "Ada Aitcheson_wintri.mlf" -t 250.0 150.0 1000.0 -S "Ada Aitcheson_train.scp" -H "hmm10\macros" -H "hmm10\hmmdefs" -M "hmm11" "Ada Aitcheson.tph"
      setStatus("Calling HERest for hmm11...");
      hmmdefs = new File(hmm10, "hmmdefs");
      macros = new File(hmm10, "macros");
      r = htk.HERest(
        config, triphonesMlf, 250, 150, 1000, trainingScp, macros, hmmdefs, hmm11,
        triphoneListFile);
      if (r != 0) {
        throw new TransformationException(
          this, "HERest returned: " + r + " - " + htk.getLastError());
      }

      setStatus("Calling HERest for hmm12...");
      statsFile = new File(sessionWorkingDir, sessionName + ".stats");
      hmmdefs = new File(hmm11, "hmmdefs");
      macros = new File(hmm11, "macros");
      r = htk.HERest(
        config, triphonesMlf, 250, 150, 1000, statsFile, trainingScp, macros, hmmdefs, hmm12,
        triphoneListFile);
      if (r != 0) {
        throw new TransformationException(
          this, "HERest returned: " + r + " - " + htk.getLastError());
      }

      setStatus("Finished step 9.");
      
    } catch (InterruptedException x) {
      throw new TransformationException(this, x);
    } catch (IOException x) {
      throw new TransformationException(this, x);
    }
  }
  
  /**
   * Generate mktri.hed file from .mph file
   * @return The mktri.hed script file
   */
  public File generateMktriHed() throws TransformationException {
    File fMph = phonemeListFile;
    File fMktri = new File(sessionWorkingDir, "mktri.hed");
    
    try {
      BufferedWriter out = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(fMktri), "UTF-8"));

      out.write("CL \"" + triphoneListFile.getPath().replaceAll("\\\\", "\\\\\\\\") + "\"\n");
      
      BufferedReader in = new BufferedReader(
        new InputStreamReader(new FileInputStream(fMph), "UTF-8"));
      String sLine = in.readLine();
      while(sLine != null) {
        out.write(
          "TI T_" + sLine + " {(*-" + sLine + "+*," + sLine + "+*,*-" + sLine + ").transP}\n");
        sLine = in.readLine();
      } // next chunk of data
      in.close();
      out.close();
    } catch(Exception exception) {
      throw new TransformationException(
        this, "generateMktriHed() failed: " + exception, exception);
    }
    return fMktri;
  } // end of generateMktriHed()

  /**
   * Step 10
   * @throws TransformationException
   */
  public void step10() throws TransformationException {
    setStatus("Step 10...");
    try {

      File config = new File(sessionWorkingDir, "config");
      File hmm12 = new File(sessionWorkingDir, "hmm12");
      File hmm13 = new File(sessionWorkingDir, "hmm13");
      File hmm14 = new File(sessionWorkingDir, "hmm14");
      File hmm15 = new File(sessionWorkingDir, "hmm15");
      File fulllist = new File(sessionWorkingDir, "fulllist");
      File mktrided = new File(sessionWorkingDir, "mktri.ded");
      File flog = new File(sessionWorkingDir, "flog");
      File tri = new File(sessionWorkingDir, sessionName + ".tri");
    
      setStatus("Generating full triphone list, running HDMan...");
      int r = htk.HDMan(fulllist, mktrided, flog, tri, dictionaryFile);
      if (r != 0) {
        throw new TransformationException(
          this, "HDMan returned: " + r + " - " + htk.getLastError());
      }

      setStatus("Generating tree.hed...");
      File fTreeHed = generateTreeHed();

      // HHEd.exe -H hmm12\macros -H hmm12\hmmdefs -M hmm13 tree.hed Ada Aitcheson.tph
/*
  setStatus("Calling HHEd...");
  File macros = new File(hmm12, "macros");
  File hmmdefs = new File(hmm12, "hmmdefs");
  setStatus("Calling HHEd...");
  r = htk.HHEd(macros, hmmdefs, hmm13, fTreeHed, triphoneListFile);
  if (r != 0) {
  throw new Exception("HHEd returned: " + r + " - " + htk.getLastError());
  }
*/
      setStatus("Calling HERest for hmm14...");
//	 hmmdefs = new File(hmm13, "hmmdefs");
//	 macros = new File(hmm13, "macros");
      File hmmdefs = new File(hmm12, "hmmdefs");
      File macros = new File(hmm12, "macros");
      r = htk.HERest(
        config, triphonesMlf, 250, 150, 1000, trainingScp, macros, hmmdefs, hmm14,
        triphoneListFile);
      if (r != 0) {
        throw new TransformationException(
          this, "HERest returned: " + r + " - " + htk.getLastError());
      }

      setStatus("Callingo HERest for hmm15...");
      hmmdefs = new File(hmm14, "hmmdefs");
      macros = new File(hmm14, "macros");
      r = htk.HERest(
        config, triphonesMlf, 250, 150, 1000, trainingScp, macros, hmmdefs, hmm15,
        triphoneListFile);
      if (r != 0) {
        throw new TransformationException(
          this, "HERest returned: " + r + " - " + htk.getLastError());
      }	 

      setStatus("Finished step 10.");
      
    } catch (InterruptedException x) {
      throw new TransformationException(this, x);
    } catch (IOException x) {
      throw new TransformationException(this, x);
    }
  }

  /**
   * Generate tree.hed file from template file
   * @return The tree.hed script file
   */
  public File generateTreeHed() throws TransformationException {
    File fTreeHedTemplate = new File(sessionWorkingDir, "tree.hed");
    File fFullList = new File(sessionWorkingDir, "fulllist");
    String sEscapedFullListName = fFullList.getPath().replaceAll("\\\\", "\\\\\\\\");
    File fTreeHed = new File(sessionWorkingDir, sessionName + "_tree.hed");

    try {
      // prepend with stats file
      BufferedWriter out = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(fTreeHed), "UTF-8"));
      out.write("RO 100.0  \"" + statsFile.getPath().replaceAll("\\\\", "\\\\\\\\") + "\"\n");

      // just copy the rest of the file
      BufferedReader in = new BufferedReader(
        new InputStreamReader(new FileInputStream(fTreeHedTemplate), "UTF-8"));
      String sLine = in.readLine();
      while(sLine != null) {
        if (sLine.indexOf("fulllist") >= 0) {
          // ensure that the AU "fulllist" filename gets a full path
          out.write("AU \"" + sEscapedFullListName + "\"\n");
        } else {
          out.write(sLine + "\n");
        }
        sLine = in.readLine();
      } // next chunk of data
      in.close();
      out.close();
    } catch(Exception exception) {
      throw new TransformationException(
        this, "generateTreeHed() failed: " + exception, exception);
    }	
    return fTreeHed;
  } // end of generateTreeHed()

  /**
   * Recognize Word Alignments.
   * @throws TransformationException
   */
  public void recognizeWordAlignments(File fSegmentFile, String sHmm)
    throws TransformationException {
    // modified step 8 - only call HVite
    setStatus("Word recognition");
    try {
    
      File config = new File(sessionWorkingDir, "config");
      File hmm = new File(sessionWorkingDir, sHmm); // hmm07 hmm09 hmm15
    
      setStatus("Calling HVite...");
      File macros = new File(hmm, "macros");
      File hmmdefs = new File(hmm, "hmmdefs");
      alignedWordsMlf = new File(sessionWorkingDir, sessionName + "_words_aligned.mlf");
    
      int r = htk.HVite(
        scoreLayerId != null?"N":"S", "SILENCE", config, macros, hmmdefs, alignedWordsMlf,
        250.0, wordsMlf, trainingScp, dictionaryFile, fSegmentFile);
//			   triphoneListFile);
//			   phonemeListFile);
      // after triphone HMM generation, supposed to be:
      //HVite -H hmm15/macros -H hmm15/hmmdefs -S test.scp -l '*' -i recout.mlf -w wdnet -p 0.0 -s 5.0 dict tiedlist
    
      if (r != 0) {
        String sError = htk.getLastError();
        // look for something like "Cannot find hmm [???-]PD[+???]"
        Pattern pCannotFindHmm = Pattern.compile(
          "Cannot find hmm \\[\\?\\?\\?-\\](.+)\\[\\+\\?\\?\\?\\]");
        Matcher mCannotFileHmm = pCannotFindHmm.matcher(sError);
        if (mCannotFileHmm.find()) {
          throw new TransformationException(
            this, "ERROR: HVite found a phone with no model: " + mCannotFileHmm.group(1));
        } else {
          throw new TransformationException(
            this, "HVite returned: " + r + " - " + htk.getLastError());
        }
      }

      setStatus("Finished word recognition.");
      
    } catch (InterruptedException x) {
      throw new TransformationException(this, x);
    } catch (IOException x) {
      throw new TransformationException(this, x);
    }
  } // end of recognizeWordAlignments()

  /**
   * Get the directory that the P2FA pre-trained models are in.
   * @return The directory that the P2FA pre-trained models are in.
   */
  protected File getP2FAModelDirectory() {
    return new File(new File(getP2FADirectory(), "model"), "11025");
  } // end of getP2FAModelDirectory()
  
  /**
   * Use the P2FA forced alignment command.
   * @throws TransformationException
   */
  public void forceAlign() throws TransformationException {
    setStatus("Forced alignment");
    try {
      
      setStatus("Calling HVite...");      
      File macros = new File(getP2FAModelDirectory(), "macros");
      File hmmdefs = new File(getP2FAModelDirectory(), "hmmdefs");
      File phones = new File(new File(getP2FADirectory(),"model"), "monophones");
      alignedWordsMlf = new File(sessionWorkingDir, sessionName + "_words_aligned.mlf");
      int r = htk.HVite(
        "S", "SILENCE", 0.0, 5.0, macros, hmmdefs, alignedWordsMlf, wordsMlf, trainingScp,
        dictionaryFile, phones);
      if (r != 0) {
        String sError = htk.getLastError();
        // look for something like "Cannot find hmm [???-]PD[+???]"
        Pattern pCannotFindHmm
          = Pattern.compile("Cannot find hmm \\[\\?\\?\\?-\\](.+)\\[\\+\\?\\?\\?\\]");
        Matcher mCannotFileHmm = pCannotFindHmm.matcher(sError);
        if (mCannotFileHmm.find()) {
          throw new TransformationException(
            this, "ERROR: HVite found a phone with no model: " + mCannotFileHmm.group(1));
        } else {
          throw new TransformationException(
            this, "HVite returned: " + r + " - " + htk.getLastError());
        }
      }
      
      setStatus("Finished word recognition.");
    } catch (IOException x) {
      throw new TransformationException(this, x);
    } catch (InterruptedException x) {
      throw new TransformationException(this, x);
    }
  } // end of forceAlign()
  
  /**
   * Reads the alignments from the files output by HTK, and merges the changes into the
   * original fragments.
   * @param htkToPhonemes Phoneme label converter to use.
   * @param useP2FACorrection Whether to use the P2FA alignment correction.
   * process is allocated to this step. 
   * @param originalFragments The original utterances that should be updated
   * @param consumer Where the aligned fragments are sent.
   */
  public void updateAlignments(
    PhonemeTranslator htkToPhonemes, boolean useP2FACorrection,
    List<Graph> originalFragments, Consumer<Graph> consumer)
    throws TransformationException {
    try {
      
      // scan the alignments, updating transcript words
      setStatus("Update word and phoneme alignments ("+originalFragments.size()+")");
      
      // need a list of noise tokens - i.e. uppcase versions of the noise 'phones'
      HashSet<String> noiseIds = new HashSet<String>();
      for (String sNoisePhone : noisePatternsMap.keySet()) {
        noiseIds.add(sNoisePhone.toUpperCase());
      }
      
      // label for tagging utterance/participant as aligned
      String sTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());
      LinkedHashSet<String> participantIds = new LinkedHashSet<String>();

      // read the graphs out of the file...
      
      // get/configure deserializer
      ParameterSet configuration = new ParameterSet();
      MlfDeserializer deserializer = new MlfDeserializer();
      configuration = deserializer.configure(configuration, schema);
      if (wordAlignmentLayerId != null) {
        configuration.get("wordLayer").setValue(schema.getLayer(wordAlignmentLayerId));
      } else {
        configuration.get("wordLayer").setValue(null);
      }
      if (phoneAlignmentLayerId != null) {
        configuration.get("phoneLayer").setValue(schema.getLayer(phoneAlignmentLayerId));
      } else {
        configuration.get("phoneLayer").setValue(null);
      }
      if (scoreLayerId != null) {
        configuration.get("scoreLayer").setValue(schema.getLayer(scoreLayerId));
      } else {
        configuration.get("scoreLayer").setValue(null);
      }      
      configuration.get("useP2FACorrection").setValue(Boolean.valueOf(useP2FACorrection));
      configuration.get("noiseIdentifiersString").setValue(
        noiseIds.stream().collect(Collectors.joining(" ")));
      configuration.get("noiseLayer").setValue(null);
      deserializer.configure(configuration, schema);

      deserializer.setParameters( // default parameters
        deserializer.load(
          Utility.OneNamedStreamArray(new NamedStream(alignedWordsMlf)), schema));
      
      // deserialize the MLF
      Graph[] alignedFragments = deserializer.deserialize();
      for (String s : deserializer.getWarnings()) setStatus("Error: " + s);
      setStatus("HTK aligned " + alignedFragments.length + " fragments");
      
      Vector<String> ids = new Vector<String>();
      Vector<String> dependentLayerIds = new Vector<String>();
      ids.add(schema.getTurnLayerId());
      ids.add(schema.getUtteranceLayerId());
      ids.add(schema.getWordLayerId());
      if (utteranceTagLayerId != null) ids.add(utteranceTagLayerId);
      if (wordAlignmentLayerId != null && !schema.getWordLayerId().equals(wordAlignmentLayerId)) {
        ids.add(wordAlignmentLayerId);
      }
      if (phoneAlignmentLayerId != null) ids.add(phoneAlignmentLayerId);
      if (scoreLayerId != null) ids.add(scoreLayerId);
      DefaultOffsetGenerator defaultOffsetGenerator = new DefaultOffsetGenerator();
      if (schema.getWordLayerId().equals(wordAlignmentLayerId)) {
        // also include any aligned word layers, so that their anchors can be validated
        // with the new alignments
        for (Layer childLayer : schema.getWordLayer().getChildren().values()) {
          if (childLayer.getAlignment() != Constants.ALIGNMENT_NONE
              && childLayer.getParentIncludes()
              && !ids.contains(childLayer.getId())) {
            ids.add(childLayer.getId());
            dependentLayerIds.add(childLayer.getId());
          }
        } // next child layer
        if (phoneAlignmentLayerId != null) {
          // and segment children      
          for (Layer childLayer : schema.getLayer(phoneAlignmentLayerId).getChildren().values()) {
            if (childLayer.getAlignment() != Constants.ALIGNMENT_NONE
                && childLayer.getSaturated() // TODO check this
                && childLayer.getParentIncludes()
                && !ids.contains(childLayer.getId())) {
              ids.add(childLayer.getId());
              dependentLayerIds.add(childLayer.getId());
            }
          } // next child layer
        }
      }
      String[] layerIds = ids.toArray(new String[0]);
      
      // create a map of IDs to original fragments
      HashMap<String,Graph> idToFragment = new HashMap<String,Graph>();
      for (Graph f : originalFragments) idToFragment.put(f.getId(), f);
      
      // for each utterance alignment...
      for (Graph alignedFragment : alignedFragments) {
        if (isCancelling()) break;
        
        // anchors start from zero, which they don't in the database
        alignedFragment.shiftAnchors((Double)alignedFragment.get("@startTime"));
        // (no need to set the phone anchor confidences to automatic, MlfDeserializer does that)
        
        try {
          
          // get the original fragment
          Graph fragment = idToFragment.get(alignedFragment.getId());
          if (fragment == null) {
            throw new TransformationException(
              this, "Original fragment not found: " + alignedFragment.getId());
          }
          
          // get ancestor annotations and add copies to the aligned fragment
          Annotation participant = fragment.first(schema.getParticipantLayerId());
          alignedFragment.getSchema().addLayer(
            (Layer)schema.getParticipantLayer().clone());
          alignedFragment.addAnnotation(
            new Annotation()
            .setLayerId(schema.getParticipantLayerId())
            .setId(participant.getId())
            .setLabel(participant.getLabel()));
          Annotation turn = fragment.first(schema.getTurnLayerId());
          alignedFragment.getSchema().addLayer(
            (Layer)schema.getTurnLayer().clone());
          Annotation editedTurn = alignedFragment.addAnnotation(
            new Annotation()
            .setLayerId(schema.getTurnLayerId())
            .setId(turn.getId())
            .setLabel(turn.getLabel())
            .setParentId(turn.getParentId()));
          participantIds.add(turn.getParentId());
          
          // set turn as parent of words
          if (wordAlignmentLayerId != null) {
            for (Annotation word : alignedFragment.all(wordAlignmentLayerId)) {
              word.setParent(editedTurn);
            }
          }
          
          // set turn as parent of phones, if appropriate
          if (phoneAlignmentLayerId != null
              && schema.getLayer(phoneAlignmentLayerId).getParentId()
              .equals(schema.getTurnLayerId())) {
            for (Annotation phone : alignedFragment.all(phoneAlignmentLayerId)) {
              phone.setParent(editedTurn);
            }
          }
          
          // set turn as parent of scores, if appropriate
          if (scoreLayerId != null
              && schema.getLayer(scoreLayerId).getParentId()
              .equals(schema.getTurnLayerId())) {
            for (Annotation score : alignedFragment.all(scoreLayerId)) {
              score.setParent(editedTurn);
            }
          }
          
          Annotation utterance = fragment.first(schema.getUtteranceLayerId());
          Annotation alignedUtterance = alignedFragment.first(schema.getUtteranceLayerId());
          if (alignedUtterance != null && utterance != null) {
            alignedUtterance.setLabel(utterance.getLabel());
            alignedUtterance.setParent(editedTurn);
          }
          if (htkToPhonemes != null && phoneAlignmentLayerId != null) {
            for (Annotation phone : alignedFragment.all(phoneAlignmentLayerId)) {
              phone.setLabel(htkToPhonemes.apply(phone.getLabel()));
            }
          }
          
          // tag the utterance as aligned
          if (utteranceTagLayerId != null) {
            alignedFragment.getSchema().addLayer(
              (Layer)schema.getLayer(utteranceTagLayerId).clone());
            Annotation timestamp = new Annotation()
              .setLayerId(utteranceTagLayerId)
              .setLabel(sTimestamp)
              .setStart(alignedFragment.getStart())
              .setEnd(alignedFragment.getEnd())
              .setParentId(editedTurn.getId());
            timestamp.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
            alignedFragment.addAnnotation(timestamp);
          }

          if (isCancelling()) break;

          // merge the current database utterance with the incoming aligned utterance
          Merger merger = new Merger(alignedFragment);
          // but don't allow changes to system layers
          merger.getNoChangeLayers().add(schema.getParticipantLayerId());
          merger.getNoChangeLayers().add(schema.getTurnLayerId());
          merger.getNoChangeLayers().add(schema.getUtteranceLayerId());
          merger.getNoChangeLayers().add(schema.getWordLayerId());
          merger.setIgnoreOffsetConfidence(ignoreAlignmentStatuses);
          fragment.trackChanges();
          // merge changes
          merger.transform(fragment);
          // ensure the utterance boundaries are unchanged (e.g. even by rounding)
          utterance.getStart().rollback();
          utterance.getEnd().rollback();
          
          if (dependentLayerIds.size() > 0) { // if there are aligned child layers
            // ensure their anchors are recomputed if they've got low-confidence alignments
            defaultOffsetGenerator.transform(fragment);
            
            // now scan the dependent layers and fix up any child anchors that are out of bounds
            // TODO fix merge/defaultOffsetGenerator so that this hack isn't necessary
            for (String layerId : dependentLayerIds) {
              for (Annotation a : fragment.all(layerId)) {
                // was it originally connected to the parent?
                if (!a.getStartId().equals(a.getParent().getStartId())
                    && a.getOriginalStartId().equals(a.getParent().getOriginalStartId())) {
                  a.setStart(a.getParent().getStart());
                } else if (a.getStart().getOffset() < a.getParent().getStart().getOffset()) {
                  // is the start now too early?
                  a.setStart(a.getParent().getStart());
                }
                
                // was it originally connected to the parent?
                if (!a.getEndId().equals(a.getParent().getEndId())
                    && a.getOriginalEndId().equals(a.getParent().getOriginalEndId())) {
                  a.setEnd(a.getParent().getEnd());
                } else if (a.getEnd().getOffset() > a.getParent().getEnd().getOffset()) {
                  // is the end now too early?
                  a.setEnd(a.getParent().getEnd());
                }
              } // next sub-word annotation
            } // next dependent layer
          }
          
          Set<Change> changes = fragment.getTracker().getChanges();
          if (merger.getLog() != null) for (String l : merger.getLog()) setStatus(l);
          if (isCancelling()) break;
          if (consumer != null) consumer.accept(fragment);
        } catch (Exception x) {
          setStatus("Could not process " + alignedFragment.getId() + ": " + x);
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          x.printStackTrace(pw);
          setStatus(sw.toString());
        }
      } // next aligned fragment
      
      setStatus("Finished updating alignments.");
    } catch (Exception x) {
      throw new TransformationException(this, x);
    }
  } // end of updateAlignments()

  /**
   * Lists log files from previous alignments.
   * @return A list of log file names, newest first.
   */
  public Collection<String> listLogs() {
    try {
      File[] logs = getWorkingDirectory().listFiles(new FilenameFilter() {
          public boolean accept(File dir, String name) {
            return name.endsWith(".log");
          }});
      Arrays.sort(logs, new Comparator<File>() {
          public int compare(File f1, File f2) {
            return (int)(f2.lastModified() - f1.lastModified());
          }
        });
      return Arrays.stream(logs).map(f->f.getName()).collect(Collectors.toList());
    } catch (Throwable t) {
      t.printStackTrace(System.out);
      return null;
    }
  } // end of listLogs()
  
  /**
   * Delete named log file.
   * @param log
   * @return True if the log file was deleted, false otherwise.
   */
  public boolean deleteLog(String log) {
    if (log.endsWith(".log")) {
      return new File(getWorkingDirectory(), log).delete();
    }
    return false;
  } // end of deleteLog()
  
  /**
   * Provides access to a log file.
   * @param log
   * @return The given log file, or null if it doesn't exist or isn't a log file.
   */
  public InputStream downloadLog(String log) {
    if (log.endsWith(".log")) {
      File logFile = new File(getWorkingDirectory(), log);
      if (logFile.exists()) {
        try {
          return new FileInputStream(logFile);
        } catch(Exception exception) {
        }
      }
    }
    return null;
  } // end of downloadLog()
   
  /**
   * Escapes quotes in the given string for inclusion in QL or SQL queries.
   * @param s The string to escape.
   * @return The given string, with quotes escapeed.
   */
  private String esc(String s) {
    if (s == null) return "";
    return s.replace("\\","\\\\").replace("'","\\'");
  } // end of esc()

} // end of class HTKAligner
