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
package nzilbb.annotator.mfa;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
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
import nzilbb.formatter.praat.TextGridSerialization;
import nzilbb.util.Execution;
import nzilbb.util.IO;

/**
 * Annotator that uses the 
 * <a href="https://montrealcorpustools.github.io/Montreal-Forced-Aligner/">
    Montreal Forced Aligner
 * </a> (MFA) to force-align given graphs.
 * @author Robert Fromont robert@fromont.net.nz
 */
@UsesFileSystem @UsesGraphStore
public class MFA extends Annotator {
  /** Get the minimum version of the nzilbb.ag API supported by the annotator.*/
  public String getMinimumApiVersion() { return "1.0.5"; }
  
  /**
   * Path to the mfa executable. 
   * @see #getMfaPath()
   * @see #setMfaPath(String)
   */
  protected String mfaPath;
  /**
   * Getter for {@link #mfaPath}: Path to the mfa executable.
   * @return Path to the mfa executable.
   */
  public String getMfaPath() { return mfaPath; }
  /**
   * Setter for {@link #mfaPath}: Path to the mfa executable.
   * @param newMfaPath Path to the mfa executable.
    */
  public MFA setMfaPath(String newMfaPath) { mfaPath = newMfaPath; return this; }
    
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
  public MFA setIgnoreAlignmentStatuses(Boolean newIgnoreAlignmentStatuses) { ignoreAlignmentStatuses = newIgnoreAlignmentStatuses; return this; }
  
  /**
   * The name of the dictionary, MFA should use pretrained models and download a
   * dictionary. null for train-and-align. 
   * @see #getDictionaryName()
   * @see #setDictionaryName(String)
   */
  protected String dictionaryName;
  /**
   * Getter for {@link #dictionaryName}: The name of the dictionary, MFA should use
   * pretrained models and download a dictionary. null for train-and-align.  
   * @return The name of the dictionary, MFA should use pretrained models and download a
   * dictionary. null for train-and-align. 
   */
  public String getDictionaryName() { return dictionaryName; }
  /**
   * Setter for {@link #dictionaryName}: The name of the dictionary, MFA should use
   * pretrained models and download a dictionary. null for train-and-align. 
   * @param newDictionaryName The name of the dictionary, MFA should use pretrained models
   * and download a dictionary. null for train-and-align. 
   */
  public MFA setDictionaryName(String newDictionaryName) { dictionaryName = newDictionaryName; return this; }
  
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
  public MFA setOrthographyLayerId(String newOrthographyLayerId) { orthographyLayerId = newOrthographyLayerId; return this; }
  
  /**
   * Layer ID of the tag layer that identifies the phonemic transcription of each word,
   * for train-and-align mode.
   * @see #getPronunciationLayerId()
   * @see #setPronunciationLayerId(String)
   * @see #dictionaryName
   */
  protected String pronunciationLayerId;
  /**
   * Getter for {@link #pronunciationLayerId}: Layer ID of the tag layer that identifies the
   * phonemic transcription of each word, for train-and-align mode.
   * @return Layer ID of the tag layer that identifies the phonemic transcription of each word,
   * for train-and-align mode.
   */
  public String getPronunciationLayerId() { return pronunciationLayerId; }
  /**
   * Setter for {@link #pronunciationLayerId}: Layer ID of the tag layer that identifies the
   * phonemic transcription of each word, for train-and-align mode. 
   * @param newPronunciationLayerId Layer ID of the tag layer that identifies the phonemic
   * transcription of each word, for train-and-align mode.
   */
  public MFA setPronunciationLayerId(String newPronunciationLayerId) { pronunciationLayerId = newPronunciationLayerId; return this; }
  
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
  public MFA setUtteranceTagLayerId(String newUtteranceTagLayerId) { utteranceTagLayerId = newUtteranceTagLayerId; return this; }
  
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
  public MFA setParticipantTagLayerId(String newParticipantTagLayerId) { participantTagLayerId = newParticipantTagLayerId; return this; }
  
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
  public MFA setWordAlignmentLayerId(String newWordAlignmentLayerId) { wordAlignmentLayerId = newWordAlignmentLayerId; return this; }
  
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
  public MFA setPhoneAlignmentLayerId(String newPhoneAlignmentLayerId) { phoneAlignmentLayerId = newPhoneAlignmentLayerId; return this; }
  
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
  public MFA setSessionName(String newSessionName) { sessionName = newSessionName; return this; }
  
  /**
   * Default constructor.
   */
  public MFA() {
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
   * Try to deduce a valid MFA path given the path to conda and the name of the MFA
   * environment. It is assumed to be something like "${condaPath}/../envs/${mfaEnvironment}/bin".
   * A side-effect of this method is that, if mfa is found, {@link #setMfaPath(String)} is called.
   * @param condaPath Path to "conda" executable, e.g. "/opt/anaconda/bin"
   * @param mfaEnvironment Name of the Anaconda environment that has been created for
   * MFA, e.g. "aligner".
   * @return A valid path to MFA, or null if it could not be inferred.
   */
  public String inferMfaPath(String condaPath, String mfaEnvironment) {
    File conda = new File(condaPath);    
    File bin = new File(conda, "bin");
    if (!bin.exists()) bin = conda; // we were given the bin directory to start with?    
    if (!bin.exists()) return null;
    if (bin.getName().equals("conda")) bin = bin.getParentFile(); // we were given conda command
    File mfaPath = new File(new File(new File(bin.getParentFile(),"envs"),mfaEnvironment),"bin");
    if (!mfaPath.exists()) return null;
    // ensure mfa is actually there
    File mfa = new File(mfaPath, "mfa");
    if (!mfa.exists()) return null;
    setMfaPath(mfaPath.getPath());
    return mfaPath.getPath();
  } // end of inferMfaPath()
  
  /**
   * Lists valid values for {@link #dictionaryName}.
   * <p> Currently this is the intersection of the lists returned by
   * <tt>mfa model download acoustic</tt> and <tt>mfa model download dictionary</tt>
   * @return A list of valid values for {@link #dictionaryName}.
   */
  public Collection<String> validDictionaryNames() throws TransformationException {
    String dictionariesRaw = mfa("model", "download", "dictionary");
    String acousticModelsRaw = mfa("model", "download", "acoustic");
    String[] dictionaryLines = dictionariesRaw.split("\n");
    String[] acousticModelLines = acousticModelsRaw.split("\n");
    Set<String> dictionaries = Arrays.stream(dictionaryLines)
      .map(s->s.trim().replaceAll("^-","").trim())
      .filter(s->s.length() > 0)
      .collect(Collectors.toSet());
    dictionaries.retainAll(Arrays.stream(acousticModelLines)
                           .map(s->s.trim().replaceAll("^-","").trim())
                           .filter(s->s.length() > 0)
                           .collect(Collectors.toSet()));
    return dictionaries;
  } // end of validDictionaryNames()
   
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
    if (mfaPath == null || mfaPath.length() == 0) {
      File whichMfa = Execution.Which("mfa");
      if (whichMfa != null) {
        mfaPath = whichMfa.getParentFile().getPath();
      }
    }
    if (mfaPath != null) {
      return "mfaPath="+mfaPath;
    } else { 
      return "condaPath=/opt/conda/bin"; // TODO defaults for Windows/OSX
    }
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

      if (mfaPath == null || mfaPath.length() == 0) {
        File whichMfa = Execution.Which("mfa");
        if (whichMfa != null) {
          mfaPath = whichMfa.getParentFile().getPath();
        }
      }
      if (mfaPath == null || mfaPath.length() == 0) {
        throw new InvalidConfigurationException(this, "Path to MFA is not set.");
      }
      File mfa = new File(mfaPath, "mfa");
      // TODO windows? if (conda != null && !conda.exists()) conda = new File(condaPath, "conda.exe");
      // try running mfa
      Execution exe = new Execution()
        .env("PATH", System.getenv("PATH")+System.getProperty("path.separator")+mfaPath)
        .setExe(mfa).arg("version");
      exe.run();
      if (exe.stderr().length() > 0) {
        throw new InvalidConfigurationException(
          this, "MFA could not run: " + exe.stderr().trim());
      } else { // conda ran without error
        setStatus("MFA version: " + exe.stdout().trim());
      } // conda ran without error
      
      // persist configuration
      PrintWriter writer = new PrintWriter(
        new File(getWorkingDirectory(), getAnnotatorId() + ".cfg"), "UTF-8");
      writer.print("mfaPath="+URLEncoder.encode(mfaPath, "UTF-8"));
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
    if (dictionaryName != null && dictionaryName.length() == 0)
      dictionaryName = null;
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

    // validation...
      
    // there must be at least one source layer
    String[] requiredLayers = getRequiredLayers();
    if (requiredLayers.length == 0) {
      throw new InvalidConfigurationException(this, "There are no source layers specified.");
    }
    if (schema.getLayer(orthographyLayerId) == null)
      throw new InvalidConfigurationException(
        this, "Orthography layer not found: " + orthographyLayerId);
    if (pronunciationLayerId == null && dictionaryName == null)
      throw new InvalidConfigurationException(
        this, "Either a pronunciation layer or a dictionary name must be specified.");
    if (pronunciationLayerId != null && schema.getLayer(pronunciationLayerId) == null)
      throw new InvalidConfigurationException(
        this, "Pronunciation layer not found: " + pronunciationLayerId);

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
        .setDescription("MFA word alignments.");
      schema.addLayer(wordAlignmentLayer);
    } else if (wordAlignmentLayerId.equals(pronunciationLayerId)
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
        .setDescription("MFA phone alignments.");
      if (pronunciationLayerId != null) {
        phoneAlignmentLayer.setType(schema.getLayer(pronunciationLayerId).getType());
      }
      schema.addLayer(phoneAlignmentLayer);
    } else if (phoneAlignmentLayerId.equals(wordAlignmentLayerId)
               || phoneAlignmentLayerId.equals(pronunciationLayerId)
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
    
    if (pronunciationLayerId != null) {
      requiredLayers.add(pronunciationLayerId);
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
    if (wordAlignmentLayerId != null) outputLayers.add(wordAlignmentLayerId);
    if (phoneAlignmentLayerId != null) outputLayers.add(phoneAlignmentLayerId);
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
      // if we're using pre-trained models, there's no training,
      // so we just call transformFragments with all utterances
      Vector<Graph> utterances = new Vector<Graph>();
      for (Annotation utterance : graph.all(schema.getUtteranceLayerId())) {
        Graph fragment = graph.getFragment(utterance, layerIds);
        fragment.trackChanges();
        utterances.add(fragment);
      }
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
    setRunning(true);
    reset();

    try {
      
      PhonemeTranslator phonemesToMfa = new PhonemeTranslator(); // (default no translation)
      PhonemeTranslator mfaToPhonemes = phonemesToMfa;
      boolean discDictionary = pronunciationLayerId != null
        && "ipa".equals(schema.getLayer(pronunciationLayerId).getType());
      boolean discOutput = "ipa".equals(schema.getLayer(phoneAlignmentLayerId).getType());

      List<Graph> fragments = null;
      TransformationException failure = null;
      try {
        
        // create initial file structure
        createSessionWorkingDir();
        setPercentComplete(5);
        
        if (dictionaryName == null) { // train & align          
          // convert output to DISC?
          if (discDictionary) {           
            phonemesToMfa = new DISC2HTK();
            mfaToPhonemes = new HTK2DISC();
          } else {
            if (discOutput) {
              mfaToPhonemes = new CMU2DISC();
            }
          }
        } else { // pretrained
          if (discOutput) { // pretrained models used ARPAbet
            mfaToPhonemes = new CMU2DISC();
          }
        }
          
        // create input files
        fragments = createInputFiles(graphs, phonemesToMfa);
        setPercentComplete(15);
          
        // if there are still some utterances
        if (fragments.size() > 0) {
          
          if (!isCancelling()) {
            if (dictionaryName == null) { // train & align          
              //mfa("validate", corpusDir.getPath(), dictionaryFile.getPath());
              setPercentComplete(30); // (up to 5 phases of 10% each arrives at 80%)
              mfa("train", "--clean", corpusDir.getPath(), dictionaryFile.getPath(),
                  alignedDir.getPath());
            } else { // pretrained
              mfa("model","download","acoustic", dictionaryName);
              setPercentComplete(25);
              if (!isCancelling()) {
                mfa("model","download","dictionary", dictionaryName);
                setPercentComplete(30);
                if (!isCancelling()) {
                  mfa("align", "--clean", corpusDir.getPath(), dictionaryName, dictionaryName,
                      alignedDir.getPath());
                } // not cancelling
              } // not cancelling
            } // pretrained
          } // not cancelling
        } // there are valid fragments
        
        if (!isCancelling()) setPercentComplete(90);

        // check number of utterances
        if (fragments.size() == 0) {
          setStatus("No utterances could be aligned.");
        } else {
          // update the transcripts where word alignments were found
          if (!isCancelling()) {
            updateAlignments(mfaToPhonemes, fragments, consumer);
            setStatus("Complete - words and phones from selected utterances are now aligned.");
          }
        }
      } catch (TransformationException x) {
        failure = x;
      }
      
      // cleanup
      IO.RecursivelyDelete(sessionWorkingDir);
      
      if (failure != null) throw failure;
      
      setPercentComplete(100);
    } finally {
      setRunning(false);
    }
  } // end of transformTranscripts()

  // Bunch of files and resources needed by HTK:

  /** The working directory for this training session. */
  protected File sessionWorkingDir;
  /** The log for this training session. */
  protected File logFile;
  /** The corpus directory. */
  protected File corpusDir;
  /** Dictionary */
  protected Map<String,LinkedHashSet<String>> dictionary;
  /** Dictionary file */
  protected File dictionaryFile;
  /** Directory for MFA output file */
  protected File alignedDir;
  Pattern errorPattern = Pattern.compile(".*[Ee][Rr][Rr][Oo][Rr].*");
  // MFA reports progress during training, but there are up to five phases that go from 0% to 100%
  // We detect these percentages and use them to increment the annotator progress,
  // but each phase represents 10% of overall progress.
  // The following member variables are used to manage phase transitions and percent progress
  Pattern progressPattern = Pattern.compile("([0-9]+)%");
  int initialPercentComplete = 0;
  int lastPhaseProgress = 0;
  int phase = 0;  
  
  /**
   * Reset any current state associated with past alignment sessions.
   */
  public void reset() {
    sessionWorkingDir = null;
    logFile = null;
    corpusDir = null;
    alignedDir = null;
    dictionary = null;
    dictionaryFile = null;
    // load condaPath from config file
    getConfig();
  } // end of reset()

  /**
   * Provides the working directory for this training session.
   * @return The working directory for temporary files.
   * @throws TransformationException if the directory couldn't be created.
   */
  protected File createSessionWorkingDir() throws TransformationException{
    if (sessionName == null) {
      sessionName = "mfa-" + hashCode();
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
      out.println("MFA log: " + sessionName);
      out.close();
    } catch(IOException exception) {
    }
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
      // corpus directory
      corpusDir = new File(sessionWorkingDir, "corpus");
      corpusDir.mkdir();
      alignedDir = new File(sessionWorkingDir, "aligned");
      
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
              if (orthography != null
                  && orthography.getLabel() != null
                  && orthography.getLabel().length() > 0) {
                
                // add word to the utterance
                if (utteranceOrthography.length() > 0) utteranceOrthography.append(" ");
                utteranceOrthography.append(orthography.getLabel());

                if (pronunciationLayerId != null) { // train/align
                  Annotation[] pronunciations = word.all(pronunciationLayerId);
                  if(pronunciations.length > 0) { // there are pronunciations
                    
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
                  } // pronunciation is present
                } // train/align
              } // orthography is present
            } // next word
            
            if (utteranceOrthography.toString().trim().length() == 0) {
              setStatus(
                "Fragment has no orthography and will be ignored: " + "\"" + fragment + "\"");
              return;
            }
            
            // write transcript file
            BufferedWriter transcript = new BufferedWriter(
              new OutputStreamWriter(
                new FileOutputStream(new File(corpusDir, fragment.getId() + ".txt")), "UTF-8"));
            transcript.write(utteranceOrthography.toString());
            transcript.close();
            
            // extract audio...
            String fileUrl = fragment.getMediaProvider().getMedia("", "audio/wav");
            File fTemp = new File(new URI(fileUrl));
            File fWav = new File(corpusDir, fragment.getId() + ".wav");
            if (fragment.isFragment()) { // fragment media is generated on the fly, so can be moved
              IO.Rename(fTemp, fWav);
            } else { // whole graph media is used in-situ (this should happen, but just in case...)
              IO.Copy(fTemp, fWav);
            }
            
            utterances.add(fragment);            
            
          } catch (Exception x) {
            setStatus("Error processing fragment : \"" + fragment + "\" : " + x);
            // TODO?? setLastException(x);
          }
        }); // next utterance

      if (pronunciationLayerId != null) {
        // write dictionary
        dictionaryFile = new File(sessionWorkingDir, sessionName + ".dict");
        BufferedWriter out = new BufferedWriter(
          new OutputStreamWriter(new FileOutputStream(dictionaryFile), "UTF-8"));
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
            
            out.write(sWord + "\t" + sEntry);
            out.newLine();
          } // next pronunciation
        } // next word              
        out.close();
      } // train/align
      
      return utterances;
    } catch (Exception x) {
      throw new TransformationException(this, x);
    }
  } // end of createInputFiles()
  
  /**
   * Execute an mfa command
   * @param args The command line arguments.
   * @return The output of the command.
   * @throws TransformationException If execution fails.
   */
  public String mfa(String... args) throws TransformationException {
    setStatus("mfa " + Arrays.stream(args).collect(Collectors.joining(" ")));

    // MFA reports progress during training, but there are up to five phases that go from
    // 0% to 100%. We detect these percentages and use them to increment the annotator progress,
    // but each phase represents 10% of overall progress.
    initialPercentComplete = getPercentComplete()==null?0:getPercentComplete();
    lastPhaseProgress = 0;
    phase = 0;
    
    Execution exe = new Execution()
      .env("PATH", System.getenv("PATH")+System.getProperty("path.separator")+mfaPath)
      .setExe(new File(mfaPath, "mfa"));
    for (String arg : args) exe.arg(arg);
    exe.getStdoutObservers().add(s->setStatus(s));
    exe.getStderrObservers().add(s-> {
        // is it a progress bar?
        Matcher progressMatcher = progressPattern.matcher(s);
        if (progressMatcher.find()) {
          // parse out the percentage
          int phaseProgress = Integer.parseInt(progressMatcher.group(1));
          // if the last percentage was greater, we've started a new phase
          if (lastPhaseProgress > phaseProgress) phase++;
          lastPhaseProgress = phaseProgress;
          // update the overallp progress
          setPercentComplete(initialPercentComplete + (phase * 10) + (phaseProgress/10));
        } else { // not progress, just pass through the message
          setStatus(s);
        }
      });
    exe.run();
    String stdout = exe.stdout().toString();
    // if (exe.stderr().length() > 0) setStatus(exe.stderr().toString());
    if (errorPattern.matcher(stdout).matches()) {
      throw new TransformationException(
        this, "Error running mfa "+Arrays.stream(args).collect(Collectors.joining(" "))
        + " : " + stdout);
    }
    if (errorPattern.matcher(exe.stderr()).matches()) {
      throw new TransformationException(
        this, "Error running mfa "+Arrays.stream(args).collect(Collectors.joining(" "))
        + " : " + exe.stderr());
    }
    setStatus("complete: mfa " + Arrays.stream(args).collect(Collectors.joining(" ")));
    return stdout;
  } // end of mfa()
  
  /**
   * Reads the alignments from the files output by HTK, and merges the changes into the
   * original fragments.
   * @param mfaToPhonemes Phoneme label converter to use.
   * @param useP2FACorrection Whether to use the P2FA alignment correction.
   * process is allocated to this step. 
   * @param originalFragments The original utterances that should be updated
   * @param consumer Where the aligned fragments are sent.
   */
  public void updateAlignments(
    PhonemeTranslator mfaToPhonemes, List<Graph> originalFragments, Consumer<Graph> consumer)
    throws TransformationException {
    try {
      
      // scan the alignments, updating transcript words
      setStatus("Update word and phoneme alignments ("+originalFragments.size()+")");

      // build layer lists from the schema
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

      // label for tagging utterance/participant as aligned
      String sTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());
      LinkedHashSet<String> participantIds = new LinkedHashSet<String>();

      // create a map of IDs to original fragments
      HashMap<String,Graph> idToFragment = new HashMap<String,Graph>();
      for (Graph f : originalFragments) idToFragment.put(IO.WithoutExtension(f.getId()), f);

      // list all TextGrids in output directory
      for (File f : alignedDir.listFiles()) {
        if (isCancelling()) break;

        if (f.getName().endsWith(".TextGrid")) { // aligned utterance
          
          // deserialize
          TextGridSerialization deserializer = new TextGridSerialization();
          ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
          configuration.get("useConventions").setValue(Boolean.FALSE);
          deserializer.configure(configuration, schema);
          NamedStream[] streams = { new NamedStream(f) };
          ParameterSet parameters = deserializer.load(streams, schema);
          parameters.get("tier0").setValue(schema.getLayer(wordAlignmentLayerId));
          parameters.get("tier1").setValue(schema.getLayer(phoneAlignmentLayerId));
          deserializer.setParameters(parameters);
          Graph[] graphs = deserializer.deserialize();
          Graph alignedFragment = graphs[0];
          
          // anchors start from zero, which they don't in the database
          String[] fragmentParts = Graph.ParseFragmentId(alignedFragment.getId());
          double startTime = Double.parseDouble(fragmentParts[1]);
          alignedFragment.shiftAnchors(startTime);
          // all anchors are automatically generated
          alignedFragment.getAnchors().values().stream().forEach(
            anchor -> anchor.setConfidence(Constants.CONFIDENCE_AUTOMATIC));
          
          try {
            
            // get the original fragment
            Graph fragment = idToFragment.get(IO.WithoutExtension(alignedFragment.getId()));
            if (fragment == null) {
              throw new TransformationException(
                this, "Original fragment not found: " + alignedFragment.getId());
            }
            
            // get ancestor annotations and add copies to the aligned fragment
            Annotation participant = fragment.first(schema.getParticipantLayerId());
            Annotation editedParticipant = alignedFragment.first(schema.getParticipantLayerId());
            if (editedParticipant == null) { // create a participant
              alignedFragment.getSchema().addLayer(
                (Layer)schema.getParticipantLayer().clone());
              editedParticipant = alignedFragment.addAnnotation(
                new Annotation()
                .setLayerId(schema.getParticipantLayerId())
                .setLabel(participant.getLabel()));
            }
            editedParticipant.setLabel(participant.getLabel());
            Annotation turn = fragment.first(schema.getTurnLayerId());
            Annotation editedTurn = alignedFragment.first(schema.getTurnLayerId());
            if (editedTurn == null) { // create a turn
              alignedFragment.getSchema().addLayer(
                (Layer)schema.getTurnLayer().clone());
              editedTurn = alignedFragment.addAnnotation(
                new Annotation()
                .setLayerId(schema.getTurnLayerId())
                .setLabel(turn.getLabel())
                .setParentId(editedParticipant.getId()));
            }
            editedTurn.setLabel(turn.getLabel());
            participantIds.add(turn.getParentId());
            Annotation utterance = fragment.first(schema.getUtteranceLayerId());
            Annotation editedUtterance = alignedFragment.first(schema.getUtteranceLayerId());
            if (editedUtterance != null) editedUtterance.setLabel(utterance.getLabel());
            
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
            
            if (mfaToPhonemes != null && phoneAlignmentLayerId != null) {
              for (Annotation phone : alignedFragment.all(phoneAlignmentLayerId)) {
                phone.setLabel(mfaToPhonemes.apply(phone.getLabel()));
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
            // don't use DefaultOffsetGenerator, so that we don't change any anchors unnecessarily
            merger.getValidator().setDefaultOffsetThreshold(null);
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
        } else if (f.getName().equals("unaligned.txt")) { // alignment failures
          tagFailures(f, idToFragment);
        } // unaligned.txt
      } // next file
      setStatus("Finished updating alignments.");
    } catch (Exception x) {
      throw new TransformationException(this, x);
    }
  } // end of updateAlignments()
  
  /**
   * Tags utterances whose alignments failed with the reason for failure.
   * @param unalignedTxt unaligned.txt file from MFA
   * @param idToFragment A map of fragment IDs to their original graphs.
   */
  protected void tagFailures(File unalignedTxt, Map<String,Graph> idToFragment) {
    if (utteranceTagLayerId != null) { // we have somewhere to log failures
      try {
        // unalignedTxt contains reasons for failure, which we save as annotations on the utterance
        // it's a tab-separated file like:
        // 719-mop03-2b-08--74-649-74-868-mop03-2b	Beam too narrow
        // 728-mop03-2b-08--79-462-79-681-mop03-2b	Beam too narrow
        BufferedReader unaligned = new BufferedReader(new FileReader(unalignedTxt));
        try {
          String line = unaligned.readLine();
          Pattern idPattern = Pattern.compile("^(.*)--([0-9]+)-([0-9]+)-([0-9]+)-([0-9]+))-(.*)$");
          while (line != null) {
            int tab = line.indexOf('\t');
            if (tab >= 0) {
              String id = line.substring(tab);
              String label = line.substring(0, tab + 1);
              // id include speaker name suffix, so we separate the parts and correct the -'s
              Matcher idMatcher = idPattern.matcher(id);
              String fragmentId = idMatcher.replaceAll("$1__$2.$3-$4.$5");
              Graph fragment = idToFragment.get(fragmentId);
              if (fragment == null) {
                setStatus("Could not identify unaligned fragment: " + id);
              } else { // fragment identified
                // tag it with the failure reason
                Annotation tag = fragment.createTag(
                  fragment.first(schema.getUtteranceLayerId()),
                  utteranceTagLayerId, "unaligned: " + label);
                tag.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
              } // fragment identified
            } // there is a tab
            
            line = unaligned.readLine();
          } // next line
        } finally {
          unaligned.close();
        }
      } catch (IOException x) {
        setStatus("Could not process " + unalignedTxt.getName() + ": " + x.getMessage());
      }
    } // utteranceTagLayerId != null
  } // end of tagFailures()
  

  /**
   * Escapes quotes in the given string for inclusion in QL or SQL queries.
   * @param s The string to escape.
   * @return The given string, with quotes escapeed.
   */
  private String esc(String s) {
    if (s == null) return "";
    return s.replace("\\","\\\\").replace("'","\\'");
  } // end of esc()

} // end of class MFA