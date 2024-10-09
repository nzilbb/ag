//
// Copyright 2016-2024 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.annotator.basservices;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
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
import java.util.regex.PatternSyntaxException;
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
import nzilbb.encoding.PhonemeTranslator;
import nzilbb.encoding.ValidLabelsDefinitions;
import nzilbb.encoding.XSAMPA2DISC;
import nzilbb.formatter.praat.TextGridSerialization;
import nzilbb.util.Execution;
import nzilbb.util.IO;
import nzilbb.media.wav.FragmentExtractor;
import nzilbb.media.MediaThread;
import nzilbb.bas.BAS;
import nzilbb.bas.BASResponse;
import nzilbb.util.ISO639;

/**
 * BAS web services annotator.  This uses BAS services for various annotation tasks.
 * <p> <a href="https://clarin.phonetik.uni-muenchen.de/BASWebServices/interface">
 *  https://clarin.phonetik.uni-muenchen.de/BASWebServices/interface</a> 
 * <p> For service discovery, links are like 
 *  <a href="http://clarin.phonetik.uni-muenchen.de/BASWebServices/BAS_Webservices.cmdi.xml">
 *  http://clarin.phonetik.uni-muenchen.de/BASWebServices/BAS_Webservices.cmdi.xml</a>
 * @author Robert Fromont robert@fromont.net.nz
 */
@UsesFileSystem @UsesGraphStore
public class BASAnnotator extends Annotator {
  /** Get the minimum version of the nzilbb.ag API supported by the annotator.*/
  public String getMinimumApiVersion() { return "1.0.6"; }
  
  /**
   * URL for the G2P (graphemes to phonemes) service.
   * @see #getG2pUrl()
   * @see #setG2pUrl(String)
   */
  protected String g2pUrl;
  /**
   * Getter for {@link #g2pUrl}: URL for the G2P (graphemes to phonemes) service.
   * @return URL for the G2P (graphemes to phonemes) service.
   */
  public String getG2pUrl() { return g2pUrl; }
  /**
   * Setter for {@link #g2pUrl}: URL for the G2P (graphemes to phonemes) service.
   * @param newG2pUrl URL for the G2P (graphemes to phonemes) service.
   */
  public BASAnnotator setG2pUrl(String newG2pUrl) { g2pUrl = newG2pUrl; return this; }
  
  /**
   * URL for the MAUSBasic (forced alignment) service.
   * @see #getMausBasicUrl()
   * @see #setMausBasicUrl(String)
   */
  protected String mausBasicUrl;
  /**
   * Getter for {@link #mausBasicUrl}: URL for the MAUSBasic (forced alignment) service.
   * @return URL for the MAUSBasic (forced alignment) service.
   */
  public String getMausBasicUrl() { return mausBasicUrl; }
  /**
   * Setter for {@link #mausBasicUrl}: URL for the MAUSBasic (forced alignment) service.
   * @param newMausBasicUrl URL for the MAUSBasic (forced alignment) service.
   */
  public BASAnnotator setMausBasicUrl(String newMausBasicUrl) { mausBasicUrl = newMausBasicUrl; return this; }
  
  /**
   * The user has accepted the BAS Terms of Usage.
   * @see #getTou()
   * @see #setTou(boolean)
   */
  protected boolean tou = false;
  /**
   * Getter for {@link #tou}: The user has accepted the BAS Terms of Usage.
   * @return The user has accepted the BAS Terms of Usage.
   */
  public boolean getTou() { return tou; }
  /**
   * Setter for {@link #tou}: The user has accepted the BAS Terms of Usage.
   * @param newTou The user has accepted the BAS Terms of Usage.
   */
  public BASAnnotator setTou(boolean newTou) { tou = newTou; return this; }
    
  /**
   * ID of the layer that determines the language of the whole transcript.
   * @see #getTranscriptLanguageLayerId()
   * @see #setTranscriptLanguageLayerId(String)
   */
  protected String transcriptLanguageLayerId;
  /**
   * Getter for {@link #transcriptLanguageLayerId}: ID of the layer that determines the
   * language of the whole transcript. 
   * @return ID of the layer that determines the language of the whole transcript.
   */
  public String getTranscriptLanguageLayerId() { return transcriptLanguageLayerId; }
  /**
   * Setter for {@link #transcriptLanguageLayerId}: ID of the layer that determines the
   * language of the whole transcript. 
   * @param newTranscriptLanguageLayerId ID of the layer that determines the language of
   * the whole transcript. 
   */
  public BASAnnotator setTranscriptLanguageLayerId(String newTranscriptLanguageLayerId) {
    if (newTranscriptLanguageLayerId != null // empty string means null
        && newTranscriptLanguageLayerId.trim().length() == 0) {
      newTranscriptLanguageLayerId = null;
    }
    transcriptLanguageLayerId = newTranscriptLanguageLayerId;
    return this;
  }
  
  /**
   * Regular expression for matching which languages to target.
   * @see #getTargetLanguagePattern()
   * @see #setTargetLanguagePattern(String)
   */
  protected String targetLanguagePattern;
  /**
   * Getter for {@link #targetLanguagePattern}: Regular expression for matching which
   * languages to target. 
   * @return Regular expression for matching which languages to target.
   */
  public String getTargetLanguagePattern() { return targetLanguagePattern; }
  /**
   * Setter for {@link #targetLanguagePattern}: Regular expression for matching which
   * languages to target. 
   * @param newTargetLanguagePattern Regular expression for matching which languages to target.
   */
  public BASAnnotator setTargetLanguagePattern(String newTargetLanguagePattern) { targetLanguagePattern = newTargetLanguagePattern; return this; }
    
  /**
   * Which BAS service is being used - "MAUSBasic" (forced alignment) or "G2P" (graphemes
   * to phonemes tagging). 
   * @see #getService()
   * @see #setService(String)
   */
  protected String service;
  /**
   * Getter for {@link #service}: Which BAS service is being used - "MAUSBasic" (forced
   * alignment) or "G2P" (graphemes to phonemes tagging). 
   * @return Which BAS service is being used - "MAUSBasic" (forced alignment) or "G2P"
   * (graphemes to phonemes tagging). 
   */
  public String getService() { return service; }
  /**
   * Setter for {@link #service}: Which BAS service is being used - "MAUSBasic" (forced
   * alignment) or "G2P" (graphemes to phonemes tagging). 
   * @param newService Which BAS service is being used - "MAUSBasic" (forced alignment) or
   * "G2P" (graphemes to phonemes tagging). 
   */
  public BASAnnotator setService(String newService) { service = newService; return this; }
  
  /**
   * Language to send to the MAUSBasic web service, null or "" to use the transcript's language.
   * @see #getForceLanguageMAUSBasic()
   * @see #setForceLanguageMAUSBasic(String)
   */
  protected String forceLanguageMAUSBasic;
  /**
   * Getter for {@link #forceLanguageMAUSBasic}: Language to send to the MAUSBasic web
   * service, null or "" to use the transcript's language. 
   * @return Language to send to the MAUSBasic web service, null or "" to use the
   * transcript's language. 
   */
  public String getForceLanguageMAUSBasic() { return forceLanguageMAUSBasic; }
  /**
   * Setter for {@link #forceLanguageMAUSBasic}: Language to send to the MAUSBasic web
   * service, null or "" to use the transcript's language. 
   * @param newForceLanguageMAUSBasic Language to send to the MAUSBasic web service, null
   * or "" to use the transcript's language. 
   */
  public BASAnnotator setForceLanguageMAUSBasic(String newForceLanguageMAUSBasic) { forceLanguageMAUSBasic = newForceLanguageMAUSBasic; return this; }

  /**
   * Language to send to the G2O web service, null or "" to use the transcript's language.
   * @see #getForceLanguageG2P()
   * @see #setForceLanguageG2P(String)
   */
  protected String forceLanguageG2P;
  /**
   * Getter for {@link #forceLanguageG2P}: Language to send to the G2O web service, null
   * or "" to use the transcript's language. 
   * @return Language to send to the G2O web service, null or "" to use the transcript's language.
   */
  public String getForceLanguageG2P() { return forceLanguageG2P; }
  /**
   * Setter for {@link #forceLanguageG2P}: Language to send to the G2O web service, null
   * or "" to use the transcript's language. 
   * @param newForceLanguageG2P Language to send to the G2O web service, null or "" to use
   * the transcript's language. 
   */
  public BASAnnotator setForceLanguageG2P(String newForceLanguageG2P) { forceLanguageG2P = newForceLanguageG2P; return this; }
  
  /**
   * The encoding to use for final phoneme labels.
   * @see #getPhonemeEncoding()
   * @see #setPhonemeEncoding(String)
   */
  protected String phonemeEncoding;
  /**
   * Getter for {@link #phonemeEncoding}: The encoding to use for final phoneme labels.
   * @return The encoding to use for final phoneme labels.
   */
  public String getPhonemeEncoding() { return phonemeEncoding; }
  /**
   * Setter for {@link #phonemeEncoding}: The encoding to use for final phoneme labels.
   * @param newPhonemeEncoding The encoding to use for final phoneme labels.
   */
  public BASAnnotator setPhonemeEncoding(String newPhonemeEncoding) { phonemeEncoding = newPhonemeEncoding; return this; }
  
  /**
   * Whether to include stress marking in G2P tags.
   * @see #getWordStress()
   * @see #setWordStress(boolean)
   */
  protected boolean wordStress;
  /**
   * Getter for {@link #wordStress}: Whether to include stress marking in G2P tags.
   * @return Whether to include stress marking in G2P tags.
   */
  public boolean getWordStress() { return wordStress; }
  /**
   * Setter for {@link #wordStress}: Whether to include stress marking in G2P tags.
   * @param newWordStress Whether to include stress marking in G2P tags.
   */
  public BASAnnotator setWordStress(boolean newWordStress) { wordStress = newWordStress; return this; }

  /**
   * Whether to include syllable marking in G2P tags.
   * @see #getSyllabification()
   * @see #setSyllabification(boolean)
   */
  protected boolean syllabification;
  /**
   * Getter for {@link #syllabification}: Whether to include syllable marking in G2P tags.
   * @return Whether to include syllable marking in G2P tags.
   */
  public boolean getSyllabification() { return syllabification; }
  /**
   * Setter for {@link #syllabification}: Whether to include syllable marking in G2P tags.
   * @param newSyllabification Whether to include syllable marking in G2P tags.
   */
  public BASAnnotator setSyllabification(boolean newSyllabification) { syllabification = newSyllabification; return this; }

  /**
   * Whether manual alignments should be overwritten (true) or not (false).
   * @see #getIgnoreAlignmentStatuses()
   * @see #setIgnoreAlignmentStatuses(boolean)
   */
  protected boolean ignoreAlignmentStatuses = Boolean.FALSE;
  /**
   * Getter for {@link #ignoreAlignmentStatuses}: Whether manual alignments should be
   * overwritten (true) or not (false). 
   * @return Whether manual alignments should be overwritten (true) or not (false).
   */
  public boolean getIgnoreAlignmentStatuses() { return ignoreAlignmentStatuses; }
  /**
   * Setter for {@link #ignoreAlignmentStatuses}: Whether manual alignments should be
   * overwritten (true) or not (false). 
   * @param newIgnoreAlignmentStatuses Whether manual alignments should be overwritten (true) or not (false).
   */
  public BASAnnotator setIgnoreAlignmentStatuses(boolean newIgnoreAlignmentStatuses) { ignoreAlignmentStatuses = newIgnoreAlignmentStatuses; return this; }
  
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
  public BASAnnotator setOrthographyLayerId(String newOrthographyLayerId) { orthographyLayerId = newOrthographyLayerId; return this; }
  
  /**
   * Layer ID of pronunciation tag output layer.
   * @see #getPronunciationLayerId()
   * @see #setPronunciationLayerId(String)
   */
  protected String pronunciationLayerId;
  /**
   * Getter for {@link #pronunciationLayerId}: Layer ID of pronunciation tag output layer.
   * @return Layer ID of pronunciation tag output layer.
   */
  public String getPronunciationLayerId() { return pronunciationLayerId; }
  /**
   * Setter for {@link #pronunciationLayerId}: Layer ID of pronunciation tag output layer.
   * @param newPronunciationLayerId Layer ID of pronunciation tag output layer.
   */
  public BASAnnotator setPronunciationLayerId(String newPronunciationLayerId) { pronunciationLayerId = newPronunciationLayerId; return this; }
  
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
  public BASAnnotator setUtteranceTagLayerId(String newUtteranceTagLayerId) { utteranceTagLayerId = newUtteranceTagLayerId; return this; }
  
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
  public BASAnnotator setParticipantTagLayerId(String newParticipantTagLayerId) { participantTagLayerId = newParticipantTagLayerId; return this; }
  
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
  public BASAnnotator setWordAlignmentLayerId(String newWordAlignmentLayerId) { wordAlignmentLayerId = newWordAlignmentLayerId; return this; }
  
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
  public BASAnnotator setPhoneAlignmentLayerId(String newPhoneAlignmentLayerId) { phoneAlignmentLayerId = newPhoneAlignmentLayerId; return this; }
  
  /**
   * Default constructor.
   */
  public BASAnnotator() {
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
    String config = null;
    // load configuration, if any
    File f = new File(getWorkingDirectory(), getAnnotatorId() + ".cfg");
    if (f.exists()) {
      try {
        config = IO.InputStreamToString(new FileInputStream(f));
        beanPropertiesFromQueryString(config);
      } catch(IOException exception) {}
    }
    return config;
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

      if (!tou) {
        throw new InvalidConfigurationException(this, "You must accept the BAS Terms of Service");
      }
      
      // persist configuration
      PrintWriter writer = new PrintWriter(
        new File(getWorkingDirectory(), getAnnotatorId() + ".cfg"), "UTF-8");
      writer.print("g2pUrl="+URLEncoder.encode(g2pUrl, "UTF-8")
                   +"&mausBasicUrl="+URLEncoder.encode(mausBasicUrl, "UTF-8")
                   +"&tou="+tou);
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
   * is invalid.
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
    if (service != null && service.length() == 0)
      service = null;
    if (phonemeEncoding != null && phonemeEncoding.length() == 0)
      phonemeEncoding = null;
    if (orthographyLayerId != null && orthographyLayerId.length() == 0)
      orthographyLayerId = null;
    if (pronunciationLayerId != null && pronunciationLayerId.length() == 0)
      pronunciationLayerId = null;
    if (utteranceTagLayerId != null && utteranceTagLayerId.length() == 0)
      utteranceTagLayerId = null;
    if (participantTagLayerId != null && participantTagLayerId.length() == 0)
      participantTagLayerId = null;
    if (wordAlignmentLayerId != null && wordAlignmentLayerId.length() == 0)
      wordAlignmentLayerId = null;
    if (phoneAlignmentLayerId != null && phoneAlignmentLayerId.length() == 0)
      phoneAlignmentLayerId = null;
    if (targetLanguagePattern != null && targetLanguagePattern.length() == 0)
      targetLanguagePattern = null;
    if (forceLanguageMAUSBasic != null && forceLanguageMAUSBasic.length() == 0)
      forceLanguageMAUSBasic = null;
    if (forceLanguageG2P != null && forceLanguageG2P.length() == 0)
      forceLanguageG2P = null;

    // validation...
      
    if (service == null)
      throw new InvalidConfigurationException(this, "BAS web service not specified.");
    // there must be at least one source layer
    String[] requiredLayers = getRequiredLayers();
    if (requiredLayers.length == 0) {
      throw new InvalidConfigurationException(this, "There are no source layers specified.");
    }
    if (transcriptLanguageLayerId != null
        && schema.getLayer(transcriptLanguageLayerId) == null) 
      throw new InvalidConfigurationException(
        this, "Transcript language layer not found: " + transcriptLanguageLayerId);
    if (orthographyLayerId == null)
      throw new InvalidConfigurationException(this, "Orthography layer not specified.");
    if (schema.getLayer(orthographyLayerId) == null)
      throw new InvalidConfigurationException(
        this, "Orthography layer not found: " + orthographyLayerId);
    if (phonemeEncoding == null)
      throw new InvalidConfigurationException(this, "Phoneme encoding not specified.");
    if (targetLanguagePattern != null) {
      if (forceLanguageG2P == null && transcriptLanguageLayerId == null) {
        throw new InvalidConfigurationException(
          this, "If target language pattern is set, transcript layer must also be set");
      }
      try {
        Pattern.compile(targetLanguagePattern);
      } catch(PatternSyntaxException exception) {
        throw new InvalidConfigurationException(
          this, "Target language pattern invalid: " + targetLanguagePattern, exception);
      }
    }
    if ("G2P".equals(service)) {
      if (pronunciationLayerId == null) 
        throw new InvalidConfigurationException(this, "Pronunciation layer not specified.");
      if (forceLanguageG2P == null && transcriptLanguageLayerId == null) {
        throw new InvalidConfigurationException(
          this, "If transcript language is to be used, transcript layer must be set");
      }
    } else if ("MAUSBasic".equals(service)) {
      if (wordAlignmentLayerId == null)
        throw new InvalidConfigurationException(
          this, "Word alignment layer not specified.");
      if (phoneAlignmentLayerId == null)
        throw new InvalidConfigurationException(
          this, "Phone alignment layer not specified.");
      if (forceLanguageMAUSBasic == null && transcriptLanguageLayerId == null) {
        throw new InvalidConfigurationException(
          this, "If transcript language is to be used, transcript layer must be set");
      }
    } else { // service none of the above
      throw new InvalidConfigurationException(
        this, "Invalid service: " + service);
    }
    
    if (utteranceTagLayerId != null) {
      Layer utteranceTagLayer = schema.getLayer(utteranceTagLayerId);
      if (utteranceTagLayer == null) {
        schema.addLayer(
          new Layer(utteranceTagLayerId)
          .setAlignment(Constants.ALIGNMENT_INTERVAL)
          .setPeers(true).setPeersOverlap(false).setSaturated(false)
          .setParentId(schema.getTurnLayerId())
          .setDescription("BAS annotation time."));
      } else if (utteranceTagLayerId.equals(orthographyLayerId)
                 || utteranceTagLayerId.equals(pronunciationLayerId)
                 || utteranceTagLayerId.equals(wordAlignmentLayerId)
                 || utteranceTagLayerId.equals(schema.getWordLayerId())
                 || utteranceTagLayerId.equals(schema.getTurnLayerId())
                 || utteranceTagLayerId.equals(schema.getUtteranceLayerId())
                 || utteranceTagLayerId.equals(phoneAlignmentLayerId)
                 || utteranceTagLayerId.equals(participantTagLayerId)
                 || !utteranceTagLayer.getParentId().equals(schema.getTurnLayerId())) {
        throw new InvalidConfigurationException(
          this, "Invalid utterance tag layer: " + utteranceTagLayerId);
      } else if (utteranceTagLayer.getAlignment() != Constants.ALIGNMENT_INTERVAL) {
        utteranceTagLayer.setAlignment(Constants.ALIGNMENT_INTERVAL);
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
          .setDescription("BAS participant annotation time."));
      } else if (participantTagLayerId.equals(orthographyLayerId)
                 || participantTagLayerId.equals(pronunciationLayerId)
                 || participantTagLayerId.equals(wordAlignmentLayerId)
                 || participantTagLayerId.equals(schema.getWordLayerId())
                 || participantTagLayerId.equals(schema.getTurnLayerId())
                 || participantTagLayerId.equals(schema.getUtteranceLayerId())
                 || participantTagLayerId.equals(phoneAlignmentLayerId)
                 || participantTagLayerId.equals(utteranceTagLayerId)
                 || !participantTagLayer.getParentId().equals(schema.getTurnLayerId())) {
        throw new InvalidConfigurationException(
          this, "Invalid participant tag layer: " + participantTagLayerId);
      } else if (participantTagLayer.getAlignment() != Constants.ALIGNMENT_INTERVAL) {
        participantTagLayer.setAlignment(Constants.ALIGNMENT_INTERVAL);
      }
    } // participantTagLayerId != null

    if ("G2P".equals(service)) {
      Layer pronunciationLayer = schema.getLayer(pronunciationLayerId);
      if (pronunciationLayer == null) {
        pronunciationLayer = new Layer(pronunciationLayerId)
          .setAlignment(Constants.ALIGNMENT_NONE)
          .setPeers(false).setPeersOverlap(false).setSaturated(true)
          .setParentId(schema.getWordLayerId())
          .setDescription(service + " pronunciation.")
          .setType(Constants.TYPE_IPA);
        schema.addLayer(pronunciationLayer);
      } else { // pronunciationLayer != null
        if (pronunciationLayer.getType() != Constants.TYPE_IPA) {
          pronunciationLayer.setType(Constants.TYPE_IPA);
        }
        if (!pronunciationLayer.getParentId().equals(schema.getWordLayerId())) {
          throw new InvalidConfigurationException(
            this, "Pronunciation layer "+pronunciationLayerId
            +" must be a "+schema.getWordLayerId()
            +" layer but is a " + pronunciationLayer.getParentId() + " layer");
        }
      } // pronunciationLayer != null
      // set valid labels
      List<Map<String,Object>> validLabelsDefinition = new Vector<Map<String,Object>>();
      if ("disc".equals(phonemeEncoding)) { // DISC
        ValidLabelsDefinitions.AddDISCDefinitions(validLabelsDefinition);
      } else if ("sampa".equals(phonemeEncoding)) { // SAMPA
        ValidLabelsDefinitions.AddSAMPADefinitions(validLabelsDefinition);
      } else if ("x-sampa".equals(phonemeEncoding)) { // X-SAMPA
        ValidLabelsDefinitions.AddXSAMPADefinitions(validLabelsDefinition);
      } else if ("ipa".equals(phonemeEncoding)) { // IPA
        // no definitions sorry
      } else if ("arpabet".equals(phonemeEncoding)) { // ARPAbet
        ValidLabelsDefinitions.AddARPAbetDefinitions(validLabelsDefinition);
      } else { // MAUS SAMPA
        ValidLabelsDefinitions.AddMausSAMPADefinitions(validLabelsDefinition);
      }
      if (validLabelsDefinition.size() > 0) {
        // for LaBB-CAT:
        pronunciationLayer.put("validLabelsDefinition", validLabelsDefinition);
        // for general use
        pronunciationLayer.setValidLabels(
          ValidLabelsDefinitions.ValidLabelsFromDefinition(validLabelsDefinition));
      }
    } else if ("MAUSBasic".equals(service)) {
      Layer wordAlignmentLayer = schema.getLayer(wordAlignmentLayerId);
      if (wordAlignmentLayer == null) {
        wordAlignmentLayer = new Layer(wordAlignmentLayerId)
          .setAlignment(Constants.ALIGNMENT_INTERVAL)
          .setPeers(true).setPeersOverlap(false).setSaturated(false)
          .setParentId(schema.getTurnLayerId())
          .setDescription(service + " word alignments.");
        schema.addLayer(wordAlignmentLayer);
      } else if (wordAlignmentLayerId.equals(schema.getTurnLayerId())
                 || wordAlignmentLayerId.equals(schema.getUtteranceLayerId())
                 || wordAlignmentLayerId.equals(phoneAlignmentLayerId)
                 || wordAlignmentLayerId.equals(utteranceTagLayerId)
                 || !wordAlignmentLayer.getParentId().equals(schema.getTurnLayerId())) {
        throw new InvalidConfigurationException(
          this, "Invalid word alignment layer: " + wordAlignmentLayerId);
      } else if (wordAlignmentLayer.getAlignment() != Constants.ALIGNMENT_INTERVAL) {
        wordAlignmentLayer.setAlignment(Constants.ALIGNMENT_INTERVAL);
      }
    
      Layer phoneAlignmentLayer = schema.getLayer(phoneAlignmentLayerId);
      if (phoneAlignmentLayer == null) {
        phoneAlignmentLayer = new Layer(phoneAlignmentLayerId)
          .setAlignment(Constants.ALIGNMENT_INTERVAL)
          .setPeers(true).setPeersOverlap(false).setSaturated(false)
          .setParentId(schema.getTurnLayerId())
          .setDescription(service + " phone alignments.")
          .setType(Constants.TYPE_IPA);
        schema.addLayer(phoneAlignmentLayer);
      } else if (phoneAlignmentLayerId.equals(wordAlignmentLayerId)
                 || phoneAlignmentLayerId.equals(schema.getWordLayerId())
                 || phoneAlignmentLayerId.equals(schema.getTurnLayerId())
                 || phoneAlignmentLayerId.equals(schema.getUtteranceLayerId())
                 || phoneAlignmentLayerId.equals(utteranceTagLayerId)) {
        throw new InvalidConfigurationException(
          this, "Invalid phone alignment layer: " + phoneAlignmentLayerId);
      } else { // phoneAlignmentLayer is set and not a system layer
        if (phoneAlignmentLayer.getAlignment() != Constants.ALIGNMENT_INTERVAL) {
          phoneAlignmentLayer.setAlignment(Constants.ALIGNMENT_INTERVAL);
        }
        if (phoneAlignmentLayer.getType() != Constants.TYPE_IPA) {
          phoneAlignmentLayer.setType(Constants.TYPE_IPA);
        }
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
      // set valid labels
      List<Map<String,Object>> validLabelsDefinition = new Vector<Map<String,Object>>();
      if ("disc".equals(phonemeEncoding)) { // DISC
        ValidLabelsDefinitions.AddDISCDefinitions(validLabelsDefinition);
      } else { // MAUS SAMPA
        ValidLabelsDefinitions.AddMausSAMPADefinitions(validLabelsDefinition);
      }
      // for LaBB-CAT:
      phoneAlignmentLayer.put("validLabelsDefinition", validLabelsDefinition);
      // for general use
      phoneAlignmentLayer.setValidLabels(
        ValidLabelsDefinitions.ValidLabelsFromDefinition(validLabelsDefinition));
    } // MAUSBasic
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
    if (schema.getUtteranceLayerId() != null)
      requiredLayers.add(schema.getUtteranceLayerId());
    
    if (schema.getWordLayerId() != null) requiredLayers.add(schema.getWordLayerId());
    
    if (orthographyLayerId != null) {
      requiredLayers.add(orthographyLayerId);
    } else {
      throw new InvalidConfigurationException(this, "Orthography layer is not set.");
    }
    
    if (transcriptLanguageLayerId != null) {
      requiredLayers.add(transcriptLanguageLayerId);
    }

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
    if ("G2P".equals(service)) {
      if (pronunciationLayerId != null) outputLayers.add(pronunciationLayerId);
    } else if ("MAUSBasic".equals(service)) {
      if (wordAlignmentLayerId != null
          // avoid a dependency loop: bas needs orthography, which needs word, which needs bas...
          && !wordAlignmentLayerId.equals(schema.getWordLayerId())) {
        Layer wordAlignmentLayer = schema.getLayer(wordAlignmentLayerId);
        if (wordAlignmentLayer != null
            && !schema.getWordLayerId().equals(wordAlignmentLayer.getParentId())
            && wordAlignmentLayer.getAlignment() != Constants.ALIGNMENT_NONE) {
          outputLayers.add(wordAlignmentLayerId);
        } // not a word tag layer
      } // not the word layer
      if (phoneAlignmentLayerId != null) outputLayers.add(phoneAlignmentLayerId);
    }
    return outputLayers.toArray(new String[0]);
  }

  // members for managing progress

  /**
   * Transforms the graph. In this case, the graph is simply summarized, by counting all
   * tokens of each word type, and printing out the result to stdout.
   * @param graph The graph to transform.
   * @return The changes introduced by the transformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph transform(Graph graph) throws TransformationException {
    setRunning(true);
    utterancesSoFar = 0;
    errors = new Vector<String>();
    try {
      Consumer<Graph> alignedFragmentConsumer = fragment->{
        // if the fragment comes from this graph
        if (graph.getId().equals(fragment.sourceGraph().getId())) {
          // apply the changes to the main graph
          if ("G2P".equals(service)) {
            graph.applyChangesFromFragment(fragment, new HashSet<String>() {{
              add(pronunciationLayerId);
              add(utteranceTagLayerId);
              add(participantTagLayerId);
            }});
          } else if ("MAUSBasic".equals(service)) {
            graph.applyChangesFromFragment(fragment, new HashSet<String>() {{
              add(wordAlignmentLayerId);
              add(phoneAlignmentLayerId);
              add(utteranceTagLayerId);
              add(participantTagLayerId);
            }});
          }
          
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

      // for G2P, the word layer is enough
      Vector<String> layerList = new Vector<String>();
      layerList.addAll(Arrays.asList(getRequiredLayers()));
      layerList.addAll(Arrays.asList(getOutputLayers()));
      String[] layerIds = layerList.toArray(new String[0]);
      // If we're doing forced alignment, we want graphs with all layers, for validation
      if ("MAUSBasic".equals(service)) {         
        layerIds = schema.getLayers().keySet().toArray(new String[0]);
      }
      // just call transformFragments with all utterances
      Vector<Graph> utterances = new Vector<Graph>();
      for (Annotation utterance : graph.all(schema.getUtteranceLayerId())) {
        Graph fragment = graph.getFragment(utterance, layerIds);
        fragment.trackChanges();
        utterances.add(fragment);
      }
      utteranceCount = utterances.size();
      if (!isCancelling()) {
        transformFragments(utterances.stream(), alignedFragmentConsumer);
      }
    } catch (RuntimeException x) {
      throw new TransformationException(this, x.getCause());
    } finally {
      setRunning(false);
    }
    return graph;
  }

  int utteranceCount = -1;
  int utterancesSoFar = 0;
  int utterancesAnnotated = 0;
  Vector<String> errors = null;
  ISO639 iso639 = new ISO639(); // for standard ISO 639 language code processing
  /**
   * Force-aligns or pronunciation-tags the given utterance fragments.
   * @param graphs A stream of fragments.
   * @param consumer A consumer for receiving the graphs once they're transformed.
   * @throws TransformationException
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  @Override
  public void transformFragments(Stream<Graph> graphs, Consumer<Graph> consumer)
    throws TransformationException, InvalidConfigurationException {
    setRunning(true);
    utterancesSoFar = 0;
    utterancesAnnotated = 0;
    errors = new Vector<String>();

    try {
      
      setPercentComplete(null);
      SimpleDateFormat timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
      final BAS bas = new BAS();
      if (mausBasicUrl != null && mausBasicUrl.length() > 0) bas.setMAUSBasicUrl(mausBasicUrl);
      if (g2pUrl != null && g2pUrl.length() > 0) bas.setG2PUrl(g2pUrl);
      final DefaultOffsetGenerator defaultOffsetGenerator = new DefaultOffsetGenerator();
      final Vector<String> dependentLayerIds = new Vector<String>();
      Vector<String> ids = new Vector<String>();
      ids.add(schema.getTurnLayerId());
      ids.add(schema.getUtteranceLayerId());
      ids.add(schema.getWordLayerId());
      if (utteranceTagLayerId != null) ids.add(utteranceTagLayerId);
      if (wordAlignmentLayerId != null && !schema.getWordLayerId().equals(wordAlignmentLayerId)) {
        ids.add(wordAlignmentLayerId);
      }
      if (phoneAlignmentLayerId != null) ids.add(phoneAlignmentLayerId);
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
      
      if ("G2P".equals(service)) {
        graphs.forEach(fragment -> {
            if (isCancelling()) return;
            try {
              PhonemeTranslator translator =
                "disc".equals(phonemeEncoding)?new XSAMPA2DISC():null;
              String languageCode = forceLanguageG2P;
              String text = Arrays.stream(fragment.labels(orthographyLayerId))
                .collect(Collectors.joining(" ")).trim();
              if (text.length() == 0) {
                setStatus(fragment.getId() + " has no orthography and will be ignored.");
              } else { // has orthography
                if (transcriptLanguageLayerId != null) {
                  Annotation graphLanguage = fragment.first(transcriptLanguageLayerId);
                  if (languageCode == null // if not forcing language, we need graphLanguage
                      && (graphLanguage == null || graphLanguage.getLabel().length() == 0)) {
                    setStatus(fragment.getId()+" has no language specified and will be ignored.");
                  } else if (targetLanguagePattern != null
                             && !graphLanguage.getLabel().matches(targetLanguagePattern)) {
                    setStatus(fragment.getId() + ": language \""+graphLanguage
                              +"\" doesn't match language pattern \""+targetLanguagePattern
                              +"\", fragment will be ignored");
                    languageCode = null; // ensure it isn't sent to BAS
                  } else if (languageCode == null) {
                    languageCode = graphLanguage.getLabel();
                    // try to make it an alpha-3 code
                    languageCode = iso639.alpha3(languageCode).orElse(languageCode);
                  }
                } // infer language code
                if (languageCode != null) {
                  // send request to BAS server
                  BASResponse response = bas.G2P(
                    languageCode, text, 
                    translator != null?"sampa":phonemeEncoding, 
                    "standard", "txt", syllabification, wordStress);
                  setStatus(
                    fragment.getId() + ": " + (response.getSuccess()?"succeeded":"failed")
                    + " - " + response.getOutput());
                  if (response.getWarnings() != null && response.getWarnings().length() > 0) {
                    setStatus(fragment.getId() + ": WARNING: " + response.getWarnings());
                  }
                  if (isCancelling()) return;
                  if (response.getDownloadLink() != null) {
                    // read the results
                    String phonemes = IO.InputStreamToString​(
                      response.getDownloadLink().openConnection().getInputStream());
                    // tokenize
                    Iterator<Annotation> words =
                      Arrays.asList(fragment.all(orthographyLayerId)).iterator();	       
                    for (String label : phonemes.toString().split("\t")) {
                      if (!words.hasNext()) {
                        errors.add(
                          fragment.getId() + ": More tokens were received back than were sent");
                        fragment = null;
                        break;
                      }
                      if (translator != null) {
                        label = translator.apply(
                          label.replace(" ","") // also remove spaces
                          .replace(".","-")) // also convert syllable boundaries
                          .replace("`","")  // and remove spurious back ticks
                          .replace("\\",""); // and remove spurious backslashes
                      }
                      Annotation word = words.next();
                      // is there an existing tag?
                      Annotation tag = word.first(pronunciationLayerId);
                      if (tag != null) {
                        tag.setLabel(label);
                      } else {
                        tag = word.createTag(pronunciationLayerId, label);
                      }
                      tag.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
                    } // next tab
                    if (words.hasNext()) {
                      errors.add(
                        fragment.getId() + ": More tokens were sent than were received back");
                      fragment = null;
                    }
                    if (fragment != null) {
                      
                      // tag this utterance
                      if (utteranceTagLayerId != null) {
                        String label = response.getOutput();
                        if (label.length() > 100) label = label.substring(0,100) + " ...";
                        if (label.length() == 0) {
                          label = timestamp.format(new Date());
                        }
                        fragment.createTag(utteranceTagLayerId, label);
                      }
                      
                      consumer.accept(fragment);
                      utterancesAnnotated++;
                    }
                  } else { // no result returned
                    errors.add(fragment.getId() + ": failed: " + response.getWarnings());
                  }
                } // language code is known
              } // has orthography                  
                  
              if (utteranceCount > 0) {
                setPercentComplete(++utterancesSoFar * 100 / utteranceCount);
              }
            } catch (Exception x) {
              setStatus("Error processing fragment : \"" + fragment + "\" : " + x);
              // TODO?? setLastException(x);
            }
          }); // next utterance
        
      } else if ("MAUSBasic".equals(service)) {
        graphs.forEach(fragment -> {
            if (isCancelling()) return;
            try {
              PhonemeTranslator translator =
                "disc".equals(phonemeEncoding)?new XSAMPA2DISC():null;
              String languageCode = forceLanguageMAUSBasic;
              String text = Arrays.stream(fragment.labels(orthographyLayerId))
                .collect(Collectors.joining(" ")).trim();
              if (text.length() == 0) {
                setStatus(fragment.getId() + " has no orthography and will be ignored.");
              } else { // has orthography
                if (transcriptLanguageLayerId != null) {
                  Annotation graphLanguage = fragment.first(transcriptLanguageLayerId);
                  if (languageCode == null // if not forcing language, we need graphLanguage
                      && (graphLanguage == null || graphLanguage.getLabel().length() == 0)) {
                    setStatus(fragment.getId()+" has no language specified and will be ignored.");
                  } else if (targetLanguagePattern != null
                             && !graphLanguage.getLabel().matches(targetLanguagePattern)) {
                    setStatus(fragment.getId() + ": language \""+graphLanguage
                              +"\" doesn't match language pattern \""+targetLanguagePattern
                              +"\", fragment will be ignored");
                    languageCode = null; // ensure it isn't sent to BAS
                  } else if (languageCode == null) {
                    languageCode = graphLanguage.getLabel();
                    // try to make it an alpha-3 code
                    languageCode = iso639.alpha3(languageCode).orElse(languageCode);
                  }
                } // get language from transcript
                
                if (languageCode != null) {
                  // get audio fragment
                  
                  // source media
                  String fragmentAudioURL = fragment.getMediaProvider()
                    .getMedia(null, "audio/wav");
                  if (fragmentAudioURL == null) {
                    setStatus(fragment.getId() + " has no audio file and will be ignored");
                  } else { // has audio
                    
                    File fragmentAudio = new File(new URI(fragmentAudioURL));
                    // (don't send the fragment ID name to the server,
                    //  there's no need to leak that info)
                    File audioFile = File.createTempFile(service, ".wav");
                    IO.Copy(fragmentAudio, audioFile);
                    audioFile.deleteOnExit();
                    
                    // call BAS MAUSBasic
                    BASResponse response = bas.MAUSBasic(
                      languageCode, new FileInputStream(audioFile),
                      new ByteArrayInputStream(text.getBytes("UTF-8")));
                    
                    setStatus(
                      fragment.getId() + ": " + (response.getSuccess()?"succeeded":"failed")
                      + " - " + response.getOutput());
                    if (response.getWarnings() != null && response.getWarnings().length() > 0) {
                      setStatus(fragment.getId() + ": WARNING: " + response.getWarnings());
                    }
                    if (response.getDownloadLink() != null) {
                      setStatus(
                        fragment.getId() + ": Downloading: " + response.getDownloadLink());
                      
                      // convert TextGrid into graph
                      Schema schema = fragment.getSchema();
                      TextGridSerialization deserializer = new TextGridSerialization();
                      ParameterSet configuration = deserializer.configure(
                        new ParameterSet(), schema);
                      configuration.get("useConventions").setValue(Boolean.FALSE);
                      deserializer.configure(configuration, schema);
                      // get required parameters for deserialization
                      ParameterSet parameters = deserializer.load(
                        Utility.OneNamedStreamArray(
                          new NamedStream(
                            response.getDownloadLink().openConnection().getInputStream(),
                            fragment.getId() + ".TextGrid", "text/praat-textgrid")), schema);
                      // set parameter values
                      parameters.get("tier0").setValue(
                        schema.getLayer(wordAlignmentLayerId));  // ORT-MAU
                      parameters.get("tier1").setValue(null);    // KAN-MAU
                      parameters.get("tier2").setValue(          // MAU
                        schema.getLayer(phoneAlignmentLayerId));
                      deserializer.setParameters(parameters);
                      Graph edited = deserializer.deserialize()[0];
                      edited.trackChanges();
                      
                      // offsets in the TextGrid start at 0, but the fragment doesn't
                      edited.shiftAnchors(fragment.getStart().getOffset());
                      // all phone anchors are automatically generated
                      for (Annotation phone : edited.all(phoneAlignmentLayerId)) {
                        phone.getStart().setConfidence(Constants.CONFIDENCE_AUTOMATIC);
                        phone.getEnd().setConfidence(Constants.CONFIDENCE_AUTOMATIC);
                      }

                      // create unaligned start/end anchors for assigning edited participant/turn
                      Anchor unknownStart = edited.addAnchor(new Anchor());
                      Anchor unknownEnd = edited.addAnchor(new Anchor());
                      
                      // rename "ORT-MAU" speaker 
                      Annotation participant = fragment.first(
                        schema.getParticipantLayerId());
                      Annotation editedParticipant = edited.first(
                        schema.getParticipantLayerId());
                      if (editedParticipant == null) { // create a participant
                        edited.getSchema().addLayer(
                          (Layer)schema.getParticipantLayer().clone());
                        editedParticipant = edited.addAnnotation(
                          new Annotation()
                          .setLayerId(schema.getParticipantLayerId())
                          .setLabel(participant.getLabel())
                          .setStartId(unknownStart.getId())
                          .setEndId(unknownEnd.getId()));
                      }
                      editedParticipant.setLabel(participant.getLabel());
                      
                      Annotation turn = fragment.first(schema.getTurnLayerId());
                      Annotation editedTurn = edited.first(schema.getTurnLayerId());
                      if (editedTurn == null) { // create a turn
                        edited.getSchema().addLayer(
                          (Layer)schema.getTurnLayer().clone());
                        editedTurn = edited.addAnnotation(
                          new Annotation()
                          .setLayerId(schema.getTurnLayerId())
                          .setLabel(turn.getLabel())
                          .setOrdinal(turn.getOrdinal())
                          .setParentId(editedParticipant.getId())
                          .setStartId(unknownStart.getId())
                          .setEndId(unknownEnd.getId()));
                      }
                      editedTurn.setLabel(turn.getLabel());
                      //participantIds.add(turn.getParentId());
                      Annotation utterance = fragment.first(schema.getUtteranceLayerId());
                      Annotation editedUtterance = edited.first(schema.getUtteranceLayerId());
                      if (editedUtterance != null) editedUtterance.setLabel(utterance.getLabel());
                      
                      // set turn as parent of words
                      if (wordAlignmentLayerId != null) {
                        for (Annotation word : edited.all(wordAlignmentLayerId)) {
                          word.setParent(editedTurn);
                        }
                      }
                      
                      // set phone anchors and labels with 'automatic' confidence
                      boolean phonesAreWordChildren = edited.getLayer(phoneAlignmentLayerId)
                        .getParentId().equals(edited.getSchema().getWordLayerId());
                      for (Annotation a : edited.list(phoneAlignmentLayerId)) {
                        if ((phonesAreWordChildren && a.getParentId() == null) // phone orphan
                            || a.getLabel().equals("<p:>")) { // pause label
                          // get rid of it
                          a.destroy();
                        } else {
                          a.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
                          // convert to DISC?
                          if (translator != null) {
                            String label = translator.apply(a.getLabel())
                              .replace(":","")  // and remove length marks
                              .replace("`",""); // and remove spurious back ticks
                            if (label.length() > 0) a.setLabel(label);
                            else setStatus("Could not convert label to DISC: " + a.getLabel());
                          }
                          // set turn as parent of phones, if appropriate
                          if (schema.getLayer(phoneAlignmentLayerId).getParentId()
                              .equals(schema.getTurnLayerId())) {
                            a.setParent(editedTurn);
                          }
                        }
                      } // next phone
                      
                      edited.commit();
                      
                      // merge graph into original
                      Merger merger = new Merger(edited);
                      merger.getNoChangeLayers().add(fragment.getSchema().getParticipantLayerId());
                      merger.getNoChangeLayers().add(fragment.getSchema().getTurnLayerId());
                      merger.getNoChangeLayers().add(fragment.getSchema().getUtteranceLayerId());
                      merger.getNoChangeLayers().add(fragment.getSchema().getWordLayerId());
                      merger.setIgnoreOffsetConfidence(ignoreAlignmentStatuses);
                      // merger.setDebug(true);
                      merger.setValidator(null); // saveGraph will validate it
                      fragment.trackChanges();
                      merger.transform(fragment);
                      Set<Change> changes = fragment.getTracker().getChanges();		  
                      if (changes.size() > 0) {
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
                        } // dependentLayerIds
                        
                          // tag this utterance
                        if (utteranceTagLayerId != null) {
                          String label = response.getOutput();
                          if (label.length() > 100) label = label.substring(0,100) + " ...";
                          if (label.length() == 0) {
                            label = timestamp.format(new Date());
                          }
                          Annotation[] timestamps = fragment.tagsOn​(utteranceTagLayerId);
                          if (timestamps.length > 0) { // update the existing tag
                            timestamps[0].setLabel(label);
                            timestamps[0].setConfidence(Constants.CONFIDENCE_AUTOMATIC);
                          } else { // add new tag
                            fragment.createTag(utterance, utteranceTagLayerId, label)
                              .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
                          }
                        }
                        consumer.accept(fragment);
                        utterancesAnnotated++;
                      } else {
                        setStatus(fragment.getId() + ": No changes.");
                      }
                    } else { // no result was returned
                      errors.add(fragment.getId() + ": failed: " + response.getWarnings());
                    } 
                    audioFile.delete();
                  } // has audio
                } // language code is known
              } // has orthography
              if (utteranceCount > 0) {
                setPercentComplete(++utterancesSoFar * 100 / utteranceCount);
              }
            } catch (Exception x) {
              setStatus("Error processing fragment : \"" + fragment + "\" : " + x);
              x.printStackTrace(System.out);
              // TODO?? setLastException(x);
            }
          }); // next utterance
      } // MAUSBasic
      
      if (!isCancelling()) setPercentComplete(100);

      if (errors.size() == 0) { 
        // check number of utterances
        if (utterancesAnnotated == 0) {
          setStatus("No utterances could be annotated.");
        } else {
          // update the transcripts where word alignments were found
          if (!isCancelling()) {
            if ("G2P".equals(service)) {
              setStatus("Complete.");
            } else if ("MAUSBasic".equals(service)) {
              setStatus("Complete - words and phones from selected utterances are now aligned.");
            }
          }
        }
      } else { // errors
        throw new TransformationException(
          this, errors.stream().collect(Collectors.joining("\n")));
      }
      
      setPercentComplete(100);
    } catch (IOException x) {
      throw new TransformationException(this, x);
    } finally {
      setRunning(false);
    }
  } // end of transformTranscripts()

} // end of class BASAnnotator
