//
// Copyright 2021-2025 New Zealand Institute of Language, Brain and Behaviour, 
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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
import java.util.Optional;
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
import nzilbb.encoding.DISC2IPA;
import nzilbb.encoding.IPA2DISC;
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
  public String getMinimumApiVersion() { return "1.0.7"; }

  /**
   * The version of the montreal-forced-aligner package that the aligner will attempt to
   * install if it's not already installed. (This only works inside Docker containers.) 
   * @see #getBuiltForMfaVersion()
   */
  protected final String builtForMfaVersion = "3.2.1";
  /**
   * Getter for {@link #builtForMfaVersion}: The version of the montreal-forced-aligner
   * package that the aligner will attempt to install if it's not already installed. (This
   * only works inside Docker containers.) 
   * @return The version of the montreal-forced-aligner package that the aligner will
   * attempt to install if it's not already installed. (This only works inside Docker
   * containers.) 
   */
  public String getBuiltForMfaVersion() { return builtForMfaVersion; }
  
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
   * The name of the available dictionary to use. null for train-and-align. 
   * @see #getDictionaryName()
   * @see #setDictionaryName(String)
   */
  protected String dictionaryName;
  /**
   * Getter for {@link #dictionaryName}: The name of the available dictionary to use.  
   * @return The name of the available dictionary to use. null for train-and-align. 
   */
  public String getDictionaryName() { return dictionaryName; }
  /**
   * Setter for {@link #dictionaryName}: The name of the available dictionary to use. 
   * @param newDictionaryName The name of the available dictionary to use. null for
   * train-and-align.  
   */
  public MFA setDictionaryName(String newDictionaryName) { dictionaryName = newDictionaryName; return this; }
  
  /**
   * The name of the pretrained acoustic models to use.
   * @see #getModelsName()
   * @see #setModelsName(String)
   */
  protected String modelsName;
  /**
   * Getter for {@link #modelsName}: The name of the pretrained acoustive models to use.
   * @return The name of the pretrained acoustive models to use.
   */
  public String getModelsName() { return modelsName; }
  /**
   * Setter for {@link #modelsName}: The name of the pretrained acoustive models to use.
   * @param newModelsName The name of the pretrained acoustive models to use.
   */
  public MFA setModelsName(String newModelsName) { modelsName = newModelsName; return this; }
  
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
   * Whether to use the --multilingual_ipa command line switch or not.
   * @see #getMultilingualIPA()
   * @see #setMultilingualIPA(boolean)
   */
  protected boolean multilingualIPA = false;
  /**
   * Getter for {@link #multilingualIPA}: Whether to use the --multilingual_ipa command
   * line switch or not. 
   * @return Whether to use the --multilingual_ipa command line switch or not.
   */
  public boolean getMultilingualIPA() { return multilingualIPA; }
  /**
   * Setter for {@link #multilingualIPA}: Whether to use the --multilingual_ipa command
   * line switch or not. 
   * @param newMultilingualIPA Whether to use the --multilingual_ipa command line switch or not.
   */
  public MFA setMultilingualIPA(boolean newMultilingualIPA) { multilingualIPA = newMultilingualIPA; return this; }
  
  /**
   * --beam setting given to the mfa command.
   * @see #getBeam()
   * @see #setBeam(int)
   */
  protected int beam = 10;
  /**
   * Getter for {@link #beam}: --beam setting given to the mfa command.
   * @return --beam setting given to the mfa command.
   */
  public int getBeam() { return beam; }
  /**
   * Setter for {@link #beam}: --beam setting given to the mfa command.
   * @param newBeam --beam setting given to the mfa command.
   */
  public MFA setBeam(int newBeam) { beam = newBeam; return this; }
  
  /**
   * --retry-beam setting given to the mfa command.
   * @see #getRetryBeam()
   * @see #setRetryBeam(int)
   */
  protected int retryBeam = 40;
  /**
   * Getter for {@link #retryBeam}: --retry-beam setting given to the mfa command (0 = use
   * default value). 
   * @return --retry-beam setting given to the mfa command.
   */
  public int getRetryBeam() { return retryBeam; }
  /**
   * Setter for {@link #retryBeam}: --retry-beam setting given to the mfa command (0 = use
   * default value). 
   * @param newRetryBeam --retry-beam setting given to the mfa command.
   */
  public MFA setRetryBeam(int newRetryBeam) { retryBeam = newRetryBeam; return this; }
  
  /**
   * The value for the mfa --phone_set command line parameter, which is used for training
   * new models. Default is null, meaning the parameter is not used. 
   * <p> For more information, see
   * <a href="https://montreal-forced-aligner.readthedocs.io/en/latest/user_guide/workflows/train_acoustic_model.html">
   * the documentation</a>.
   * @see #getPhoneSet()
   * @see #setPhoneSet(String)
   */
  protected String phoneSet;
  /**
   * Getter for {@link #phoneSet}: The value for the mfa --phone_set command line
   * parameter, which is used for training new models. Default is null, meaning the
   * parameter is not used. 
   * @return The value for the mfa --phone_set command line parameter, which is used for
   * training new models. Default is null, meaning the parameter is not used. 
   */
  public String getPhoneSet() { return phoneSet; }
  /**
   * Setter for {@link #phoneSet}: The value for the mfa --phone_set command line
   * parameter, which is used for training new models. Default is null, meaning the
   * parameter is not used. 
   * <p> For more information, see
   * <a href="https://montreal-forced-aligner.readthedocs.io/en/latest/user_guide/workflows/train_acoustic_model.html">
   * the documentation</a>.
   * @param newPhoneSet The value for the mfa --phone_set command line parameter, which is
   * used for training new models. Default is null, meaning the parameter is not used. 
   */
  public MFA setPhoneSet(String newPhoneSet) { phoneSet = newPhoneSet; return this; }
  
  /**
   * Whether to adapt acoustic model per speaker (false) or not (true). Influences the
   * "--uses_speaker_adaptation" command line parameter. 
   * @see #getNoSpeakerAdaptation()
   * @see #setNoSpeakerAdaptation(boolean)
   */
  protected boolean noSpeakerAdaptation = false;
  /**
   * Getter for {@link #noSpeakerAdaptation}: Whether to adapt acoustic model per speaker
   * (false) or not (true). Influences the "--uses_speaker_adaptation" command line
   * parameter. 
   * @return Whether to adapt acoustic model per speaker (false) or not (true). Influences
   * the "--uses_speaker_adaptation" command line parameter. 
   */
  public boolean getNoSpeakerAdaptation() { return noSpeakerAdaptation; }
  /**
   * Setter for {@link #noSpeakerAdaptation}: Whether to adapt acoustic model per speaker
   * (false) or not (true). Influences the "--uses_speaker_adaptation" command line
   * parameter. 
   * @param newNoSpeakerAdaptation Whether to adapt acoustic model per speaker (false) or
   * not (true). Influences the "--uses_speaker_adaptation" command line parameter. 
   */
   public MFA setNoSpeakerAdaptation(boolean newNoSpeakerAdaptation) { noSpeakerAdaptation = newNoSpeakerAdaptation; return this; }

  /**
   * If alignment fails, don't clean up files.
   * @see #getNoCleanupOnFailure()
   * @see #setNoCleanupOnFailure(boolean)
   */
  protected boolean noCleanupOnFailure = false;
  /**
   * Getter for {@link #noCleanupOnFailure}: If alignment fails, don't clean up files.
   * @return If alignment fails, don't clean up files.
   */
  public boolean getNoCleanupOnFailure() { return noCleanupOnFailure; }
  /**
   * Setter for {@link #noCleanupOnFailure}: If alignment fails, don't clean up files.
   * @param newNoCleanupOnFailure If alignment fails, don't clean up files.
   */
  public MFA setNoCleanupOnFailure(boolean newNoCleanupOnFailure) { noCleanupOnFailure = newNoCleanupOnFailure; return this; }
  
  /**
   * Whether to use PostGres (or SQLite).
   * @see #getUsePostgres()
   * @see #setUsePostgres(boolean)
   */
  protected boolean usePostgres = false;
  /**
   * Getter for {@link #usePostgres}: Whether to use PostGres (or SQLite).
   * @return Whether to use PostGres (or SQLite).
   */
  public boolean getUsePostgres() { return usePostgres; }
  /**
   * Setter for {@link #usePostgres}: Whether to use PostGres (or SQLite).
   * @param newUsePostgres Whether to use PostGres (or SQLite).
   */
  public MFA setUsePostgres(boolean newUsePostgres) { usePostgres = newUsePostgres; return this; }

  /**
   * Percentage of overlap with other speech, above which the utterance is ignored.
   * @see #getOverlapThreshold()
   * @see #setOverlapThreshold(Integer)
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
  public MFA setOverlapThreshold(Integer newOverlapThreshold) {
    if (newOverlapThreshold != null && newOverlapThreshold.intValue() == 0) {
      overlapThreshold = null;
    } else {
      overlapThreshold = newOverlapThreshold;
    }
    return this; }

  /**
   * Whether to try to output DISC symbols on the phone alignment layer.
   * @see #getDiscOutput()
   * @see #setDiscOutput(boolean)
   */
  protected boolean discOutput = false;
  /**
   * Getter for {@link #discOutput}: Whether to try to output DISC symbols on the phone
   * alignment layer. 
   * @return Whether to try to output DISC symbols on the phone alignment layer.
   */
  public boolean getDiscOutput() { return discOutput; }
  /**
   * Setter for {@link #discOutput}: Whether to try to output DISC symbols on the phone
   * alignment layer. 
   * @param newDiscOutput Whether to try to output DISC symbols on the phone alignment layer.
   */
  public MFA setDiscOutput(boolean newDiscOutput) { discOutput = newDiscOutput; return this; }

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

    // cancel kills any current processes
    getCancellationObservers().add(cancelling -> {
        if (cancelling && exe != null) {
          try {
            setStatus("Killing process " + exe.getProcess());
            exe.getProcess().destroy();
          } catch(Throwable t) {
            setStatus("Could not destroy process: " + t);
          }
        }
      });
  } // end of constructor
  
  /**
   * Try to deduce a valid MFA path given the path to conda and the name of the MFA
   * environment. It is assumed to be something like "${condaPath}/../envs/${mfaEnvironment}/bin".
   * A side-effect of this method is that, if mfa is found, {@link #setMfaPath(String)} is called.
   * @param condaPath Path to "conda" executable, e.g. "/opt/anaconda/bin"
   * @param mfaEnvironment Name of the Anaconda environment that has been created for
   * MFA, e.g. "aligner".
   * @return A valid path to MFA, or an empty string if it could not be inferred.
   */
  public String inferMfaPath(String condaPath, String mfaEnvironment) {
    try {
      File conda = new File(condaPath);
      File envs = null;
      do { // search this and ancestor directories for "envs" subdirectory
        envs = new File(conda, "envs");
        if (!envs.exists()) { // not here
          envs = null;
          // try parent directory
          conda = conda.getParentFile();
        }
      } while (envs == null && conda != null);
      
      if (envs.exists()) {
        File aligner = new File(envs, mfaEnvironment);
        if (aligner.exists()) {
          File mfaPath = new File(aligner, "bin");
          if (!mfaPath.exists()) {
            mfaPath = new File(aligner, "Scripts");
          }
          
          if (mfaPath.exists()) {
            // ensure mfa is actually there
            File mfa = new File(mfaPath, "mfa");
            if (!mfa.exists()) mfa = new File(mfaPath, "mfa.exe");
            
            if (mfa.exists()) {
              setMfaPath(mfaPath.getPath());
              return mfaPath.getPath();
            }
          }
        }
      }
    } catch(Exception t) {
      setStatus("inferMfaPath("+condaPath+", "+mfaEnvironment+"): " + t);
    }
      
    return "";
  } // end of inferMfaPath()
  
  /**
   * Lists valid values for {@link #dictionaryName}.
   * <p> This is the list returned by <tt>mfa model download dictionary</tt>
   * @return A list of valid values for {@link #dictionaryName}.
   */
  public Collection<String> validDictionaryNames() throws TransformationException {
    String dictionariesRaw = mfa(true, getWorkingDirectory(), "model", "download", "dictionary");
    String[] dictionaryLines = dictionariesRaw.split("\n");
    List<String> dictionaries = Arrays.stream(dictionaryLines)
      .map(
        s->s.replace("\r","")        // remove carriage returns used on Windows systems
        .replaceAll("^[^']*'","")    // remove leading quote and anything before it
        .replaceAll("'[,:]?.*$","")) // remove trailing quote and anything after it
      .filter(s->s.length() > 0)     // no blank lines
      .filter(s->s.indexOf(":") < 0) // not the list header
      .filter(s->!s.equals("["))     // not just an open-bracket
      .filter(s->!s.equals("]"))     // not just a close-bracket
      .filter(s->!s.equals("{"))     // not just an open-brace
      .filter(s->!s.equals("}"))     // not just a close-brace
      .sorted()
      .collect(Collectors.toList());
    return dictionaries;
  } // end of validDictionaryNames()
   
  /**
   * Lists valid values for {@link #modelsName}.
   * <p> This is the list returned by <tt>mfa model download acoustic</tt>
   * @return A list of valid values for {@link #modelsName}.
   */
  public Collection<String> validAcousticModels() throws TransformationException {
    String acousticModelsRaw = mfa(true, getWorkingDirectory(), "model", "download", "acoustic");
    String[] acousticModelLines = acousticModelsRaw.split("\n");
    List<String> acousticModels = Arrays.stream(acousticModelLines)
      .map(
        s->s.replace("\r","")        // remove carriage returns used on Windows systems
        .replaceAll("^[^']*'","")    // remove leading quote and anything before it
        .replaceAll("'[,:]?.*$","")) // remove trailing quote and anything after it
      .filter(s->s.length() > 0)     // no blank lines
      .filter(s->s.indexOf(":") < 0) // not the list header
      .filter(s->!s.equals("["))     // not just an open-bracket
      .filter(s->!s.equals("]"))     // not just a close-bracket
      .filter(s->!s.equals("{"))     // not just an open-brace
      .filter(s->!s.equals("}"))     // not just a close-brace
      .sorted()
      .collect(Collectors.toList());
    return acousticModels;
  } // end of validAcousticModels()
  
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
      if (System.getProperty("os.name").startsWith("Windows")) {
        return "condaPath=C:\\ProgramData\\Miniconda3";
      } else { // linux
        return "condaPath=/opt/conda/bin"; // TODO defaults for OSX
      }
    }
  }
  
  /**
   * Ensures MFA is available.
   * @throws InvalidConfigurationException
   * @see #getConfig()
   * @see #beanPropertiesFromQueryString(String)
   */ 
  public void setConfig(String config) throws InvalidConfigurationException {
    setRunning(true);
    logFile = new File(getWorkingDirectory(), "config.log");
    // getStatusObservers().add(status -> {
    //     if (logFile != null) {
    //       try {
    //         PrintWriter out = new PrintWriter(new FileOutputStream(logFile, true));
    //         out.println(status);
    //         out.close();
    //       } catch(IOException exception) {
    //       }
    //     } // logFile still set
    //   });
    try {
      setStatus(""); // clear any residual status from the last run...

      beanPropertiesFromQueryString(config);

      // check MFA is accessible
      setStatus("MFA version: " + mfaVersion());
      
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
   * Start an attempt to install the version of MFA indicated by {@link #builtForMfaVersion}.
   * <p> This is only likely to work inside a Docker container, where the current user has
   * superuser privileges.
   * <p> This method starts the installation process in a new thread, and returns
   * immediately. In order to follow progress, use {@link Annotator#getRunning()}, 
   * {@link Annotator#getPercentComplete()}, and {@link Annotator#getStatus()}.
   */
  public void installMfa() {
    new Thread(()->{
        setRunning(true);
        setPercentComplete(1);
        try {
          // wget https://repo.anaconda.com/miniconda/Miniconda3-py38_4.10.3-Linux-x86_64.sh
          
          URL url = new URL(
            "https://repo.anaconda.com/miniconda/Miniconda3-py38_4.10.3-Linux-x86_64.sh");
          setStatus("Attempting to download Miniconda from: " + url);
          HttpURLConnection connection = (HttpURLConnection)url.openConnection();
          InputStream input = connection.getInputStream();
          File sh = new File(getWorkingDirectory(), url.getPath().replaceAll(".*/",""));
          try {
            IO.Pump(input, new FileOutputStream(sh));
            setStatus("Downloaded " + sh.getName());
            setPercentComplete(33);
            if (isCancelling()) return;
            
            // bash Miniconda3-py38_4.10.3-Linux-x86_64.sh -b -s -p /opt/conda
            setStatus("Installing Miniconda...");
            Execution cmd = new Execution()
              .setExe("bash")
              .arg(sh.getPath())
              .arg("-b") // run install in batch mode (without manual intervention)
              .arg("-s") // skip running pre/post-link/install scripts
              .arg("-p").arg("/opt/conda") // prefix:        
              .setWorkingDirectory(getWorkingDirectory());
            cmd.getStdoutObservers().add(m->setStatus(m));
            cmd.getStderrObservers().add(m->setStatus(m));
            cmd.run();
            if (cmd.getError().length() > 0) setStatus("stderr: " + cmd.getError());
            setStatus(cmd.getInput().toString());
            setPercentComplete(66);
            if (isCancelling()) return;
            
            // /opt/conda/bin/conda create -y -n aligner -c conda-forge montreal-forced-aligner
            setStatus("Installing montreal-forced-aligner...");
            cmd = new Execution()
              .setExe("/opt/conda/bin/conda")
              .arg("create")
              .arg("-y") // yes to everything
              .arg("-n").arg("aligner") // env name
              .arg("-c").arg("conda-forge")
              .arg("montreal-forced-aligner="+builtForMfaVersion)
              .setWorkingDirectory(getWorkingDirectory());
            cmd.getStdoutObservers().add(m->setStatus(m));
            cmd.getStderrObservers().add(m->setStatus(m));
            cmd.run();
            setStatus("Installation finished.");
            setPercentComplete(100);
            
          } finally {
            sh.delete();
          }
        } catch (Throwable t) {
          setStatus("installMfa: "+t.getMessage());
        } finally {
          setRunning(false);
        }
    }).start();
  } // end of installMfa()
  
  /**
   * Returns the version of the Montreal Forced Aligner that's installed.
   * @return The currently-installed MFA version.
   * @throws InvalidConfigurationException If "mfa version" could not be executed.
   */
  public String mfaVersion() throws InvalidConfigurationException {
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
    File mfaExe = new File(mfaPath, "mfa.exe");
    if (!mfa.exists() && mfaExe.exists()) mfa = mfaExe;
    File envPath = new File(mfaPath).getParentFile();
    File condaPath = envPath.getParentFile().getParentFile();
    File condaBin = new File(condaPath, "bin");
    if (!condaBin.exists()) condaBin = new File(condaPath, "condabin");
    File conda = new File(condaBin, "conda");
    if (!conda.exists()) conda = new File(condaBin, "conda.bat");

    Execution exe = new Execution();
    if (System.getProperty("os.name").startsWith("Windows")) {
      if (!conda.exists()) {
        throw new InvalidConfigurationException(this, "Conda CLI not found: " + conda.getPath());
      }
      // use %TEMP% instead of java.io.tmpdir because java.io.tmpdir might have spaces in it,
      // while %TEMP% probably won't (e.g. C:\WINDOWS\TEMP if we're running in a service)
      exe.env("MFA_ROOT_DIR", System.getenv("TEMP"))
        .setWorkingDirectory(envPath)
        // .setExe(conda)
        .setExe("cmd").arg("/C")
        .arg(conda.getPath())
        .arg("run")
        .arg("--no-capture-output")
        .arg("-p").arg(envPath.getPath())
        .arg("--cwd").arg(envPath.getPath())
        .arg("mfa").arg("version");
      // inherit current environment
      exe.getEnvironmentVariables().putAll(System.getenv());
    } else { // non-Windows systems call mfa directly (conda run doesn't work)
      if (!mfa.exists()) {
        throw new InvalidConfigurationException(this, "MFA CLI not found: " + mfa.getPath());
      }
      exe.env("PATH", System.getenv("PATH")+pathVariableSuffix())
        .env("HOME", System.getProperty("java.io.tmpdir"))
        .env("MFA_ROOT_DIR", System.getProperty("java.io.tmpdir"))
        .setExe(mfa).arg("version");
    }
    exe.run();
    if (exe.stderr().length() > 0) { 
      setStatus("MFA could not run: " + exe.stderr().trim());
      throw new InvalidConfigurationException(
        this, "MFA could not run: " + exe.stderr().trim());
    } else { // conda ran without error
      return exe.stdout().trim();
    } // conda ran without error
  } // end of mfaVersion()
  
  /**
   * Returns what should be added to the PATH environment variable in order for the mfa
   * command to work. This depends on {@link #mfaPath} and the operating system. 
   * @return A string to append to the PATH environment variable, including the path separator.
   */
  public String pathVariableSuffix() {
    String suffix = System.getProperty("path.separator")+mfaPath;
    if (System.getProperty("os.name").startsWith("Windows")) {
      // on windows, we also need ..\Library\bin
      File mfaBin = new File(mfaPath);
      File library = new File(mfaBin.getParentFile(), "Library");
      File libraryBin = new File(library, "bin");
      suffix += System.getProperty("path.separator")+libraryBin.getPath();
    }
    return suffix;
  } // end of pathVariableSuffix()
   
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
    if (dictionaryName != null && dictionaryName.length() == 0)
      dictionaryName = null;
    if (modelsName != null && modelsName.length() == 0)
      modelsName = null;
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
    if (dictionaryName != null && modelsName == null)
      throw new InvalidConfigurationException(
        this, "If a dictionary name is specified, pretrained acoustic models must also be specified.");
    if (pronunciationLayerId != null && schema.getLayer(pronunciationLayerId) == null)
      throw new InvalidConfigurationException(
        this, "Pronunciation layer not found: " + pronunciationLayerId);

    if (utteranceTagLayerId != null) {
      Layer utteranceTagLayer = schema.getLayer(utteranceTagLayerId);
      if (utteranceTagLayer == null) {
        schema.addLayer(
          new Layer(utteranceTagLayerId)
          .setAlignment(Constants.ALIGNMENT_INTERVAL)
          .setPeers(true).setPeersOverlap(false).setSaturated(false)
          .setParentId(schema.getTurnLayerId())
          .setDescription("MFA alignment time."));
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
          .setDescription("MFA participant alignment time."));
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
               || phoneAlignmentLayerId.equals(utteranceTagLayerId)) {
      throw new InvalidConfigurationException(
        this, "Invalid phone alignment layer: " + phoneAlignmentLayerId);
    } else { // phoneAlignmentLayer is set and not a system layer
      if (phoneAlignmentLayer.getAlignment() != Constants.ALIGNMENT_INTERVAL) {
        phoneAlignmentLayer.setAlignment(Constants.ALIGNMENT_INTERVAL);
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
    if (wordAlignmentLayerId != null
        // avoid a dependency loop: mfa needs orthography, which needs word, which needs mfa...
        && !wordAlignmentLayerId.equals(schema.getWordLayerId())) {
      Layer wordAlignmentLayer = schema.getLayer(wordAlignmentLayerId);
      if (wordAlignmentLayer != null
          && !schema.getWordLayerId().equals(wordAlignmentLayer.getParentId())
          && wordAlignmentLayer.getAlignment() != Constants.ALIGNMENT_NONE) {
        outputLayers.add(wordAlignmentLayerId);
      } // not a word tag layer
    } // not the word layer
    if (phoneAlignmentLayerId != null) outputLayers.add(phoneAlignmentLayerId);
    return outputLayers.toArray(new String[0]);
  }

  // members for managing progress

  /**
   * Transforms the graph. 
   * @param graph The graph to transform.
   * @return The transformed graph.
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

      List<Graph> fragments = null;
      TransformationException failure = null;
      try {
        
        // create initial file structure
        createSessionWorkingDir();
        setPercentComplete(5);
        
        if (dictionaryName == null) { // train & align
          if (!multilingualIPA) { // (pass through IPA labels as-is)
            // convert output to DISC?
            if (discDictionary) {           
              phonemesToMfa = new DISC2IPA().setDelimiter(" ");
              mfaToPhonemes = new IPA2DISC().setDelimiter(" ");
            } else {
              if (discOutput) {
                mfaToPhonemes = new CMU2DISC();
              }
            }
          }
        } else { // pretrained
          if (discOutput) {
            if (dictionaryName.equals("english") // mfa 2.0.0rc3
                || dictionaryName.indexOf("_arpa") > 0) { // later versions
              // some pretrained models use ARPAbet
              mfaToPhonemes = new CMU2DISC();
            }
          } // discOutput
        } // pretrained
        
        // create input files
        fragments = createInputFiles(graphs, phonemesToMfa);
        setPercentComplete(15);
          
        // if there are still some utterances
        if (fragments.size() > 0) {
          
          if (!isCancelling()) {

            // as of around version 2.2, mfa uses a PostGres database, and needs to
            // start/stop the server
            // this is invisible and automatic when run by humans on the command line
            // but when automated, causes errors, so we need to disable automatic server
            // management, and explicitly start/stop the server
            // https://montreal-forced-aligner.readthedocs.io/en/latest/user_guide/server/index.html
            boolean dbServer = false;
            if (usePostgres) {
              try {
                setStatus("Setting up database...");
                // ensure other commands don't start/stop database server
                mfa(false, Optional.ofNullable(sessionWorkingDir).orElse(getWorkingDirectory()),
                    "configure", "--disable_auto_server");
                // start db server
                mfa(false, Optional.ofNullable(sessionWorkingDir).orElse(getWorkingDirectory()),
                    "server", "start", "--"+(usePostgres?"":"no_")+"use_postgres");
                dbServer = true;
              } catch (TransformationException x) {
                setStatus("DB setup failed: " + x);
              }
            }

            try {
            
              if (dictionaryName == null && modelsName == null) { // train & align          
                //mfa("validate", corpusDir.getPath(), dictionaryFile.getPath());
                setPercentComplete(30); // (up to 5 phases of 10% each arrives at 80%)
                Vector<String> parameters = new Vector<String>();
                parameters.add("train");
                // Don't parameters.add("--clean"); it deletes everything including the corpus
                if (multilingualIPA) {
                  parameters.add("--multilingual_ipa");
                }
                if (phoneSet != null && phoneSet.length() > 0) {
                  parameters.add("--phone_set");
                  parameters.add(phoneSet);
                }
                parameters.add("--output_format");
                parameters.add("long_textgrid");
                parameters.add(corpusDir.getPath());
                parameters.add(dictionaryFile.getPath());
                parameters.add(modelsDir.getPath());
                parameters.add("--output_directory");
                parameters.add(alignedDir.getPath());
                parameters.add("--beam");
                parameters.add(""+beam);
                parameters.add("--retry-beam");
                parameters.add(""+retryBeam);
                parameters.add("--"+(usePostgres?"":"no_")+"use_postgres");
                String[] paramatersArray = parameters.toArray(new String[0]);
                mfa(false, Optional.ofNullable(sessionWorkingDir).orElse(getWorkingDirectory()),
                    paramatersArray);
                setPercentComplete(80); // (up to 5 phases of 10% each arrives at 80%)
                // log contents of ${tempDir}/corpus/train_acoustic_model.log
                copyLog(new File(new File(tempDir, "corpus"), "train_acoustic_model.log"));
              } else { // pretrained
                // Have we already download the models?
                File acousticModelsZip
                  = new File(new File(
                               new File(getWorkingDirectory(), "pretrained_models"), "acoustic"),
                             modelsName+".zip");
                if (!acousticModelsZip.exists()) {
                  mfa(false, getWorkingDirectory(),
                      "model","download","acoustic", modelsName);
                } else {
                  setStatus("Acoustic models already downloaded: " + modelsName);
                }
                setPercentComplete(25);
                if (!isCancelling()) {
                  String dictionary = dictionaryFile != null?dictionaryFile.getPath():null;
                  if (dictionaryName != null) {
                    File builtInDict = new File(
                      new File(new File(
                                 getWorkingDirectory(), "pretrained_models"), "dictionary"),
                      dictionaryName+".dict");
                    if (!builtInDict.exists()) {
                      mfa(false, getWorkingDirectory(),
                          "model","download","dictionary", dictionaryName);
                    } else {
                      setStatus("Dictionary already downloaded: " + dictionaryName);
                    }
                    setPercentComplete(30);
                    dictionary = builtInDict.getPath();
                  }
                  if (!isCancelling()) {
                    mfa(
                      false, Optional.ofNullable(sessionWorkingDir).orElse(getWorkingDirectory()),
                      "align", // Don't "--clean", it deletes everything including the corpus
                      "--output_format", "long_textgrid",
                      corpusDir.getPath(), dictionary,
                      acousticModelsZip.getPath(),
                      alignedDir.getPath(),
                      "--beam", ""+beam, "--retry-beam", ""+retryBeam,
                      "--uses_speaker_adaptation", noSpeakerAdaptation?"False":"True",
                      "--"+(usePostgres?"":"no_")+"use_postgres");
                    // log contents of ${tempDir}/corpus/align.log
                    copyLog(new File(new File(tempDir, "corpus"), "align.log"));
                  } // not cancelling
                } // not cancelling
              } // pretrained
              
            } finally {
              if (dbServer) {
                try {
                  setStatus("Shutting down database server...");
                  mfa(false, Optional.ofNullable(sessionWorkingDir).orElse(getWorkingDirectory()),
                      "server", "stop", "--mode", "smart", "--use_postgres");
                } catch (TransformationException x) {
                  setStatus("DB shutdown failed: " + x);
                }
              }
            }
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
      if (failure == null || !noCleanupOnFailure) { // no failure, or cleanup regardless
        IO.RecursivelyDelete(sessionWorkingDir);
      } else {
        setStatus("Working files not cleaned up after failure: " + sessionWorkingDir.getPath());
      }
      
      if (failure != null) throw failure;
      
      setPercentComplete(100);
    } finally {
      setRunning(false);
    }
  } // end of transformTranscripts()

  // Bunch of files and resources needed by MFA:

  /** The working directory for this training session. */
  protected File sessionWorkingDir;
  /** The log for this training session. */
  protected File logFile;
  /** The temporary working files directory. */
  protected File tempDir;
  /** The corpus directory. */
  protected File corpusDir;
  /** Dictionary */
  protected Map<String,LinkedHashSet<String>> dictionary;
  /** Dictionary file */
  protected File dictionaryFile;
  /** Directory for MFA output alignments */
  protected File alignedDir;
  /** Directory for MFA output models */
  protected File modelsDir;
  Pattern errorPattern = Pattern.compile(".*error.*", Pattern.CASE_INSENSITIVE + Pattern.DOTALL);
  // MFA reports progress during training, but there are a variable umber phases that go from
  // 0% to 100%
  // We detect these percentages but can't use them to increment the annotator progress,
  // because we can't preict how many there are.
  // The following member variables are used to manage phase transitions and percent progress
  Pattern progressPattern = Pattern.compile("([0-9]+)%");
  
  /**
   * Reset any current state associated with past alignment sessions.
   */
  public void reset() {
    sessionWorkingDir = null;
    logFile = null;
    tempDir = null;
    corpusDir = null;
    alignedDir = null;
    modelsDir = null;
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
      String prefix = "mfa";
      if (utteranceTagLayerId != null) prefix = utteranceTagLayerId;
      else if (phoneAlignmentLayerId != null) prefix = phoneAlignmentLayerId;
      sessionName = prefix + "-" + hashCode();
    }
    setStatus("Session " + sessionName);
    File parentDir = new File(System.getProperty("java.io.tmpdir"));
    if (System.getProperty("os.name").startsWith("Windows") && parentDir.getPath().contains(" ")) {
      // MFA doesn't handle paths with spaces well, so we try to use a path with no spaces
      // like C:\WINDOWS\TEMP
      parentDir = new File(System.getenv("TEMP"));
    }
    sessionWorkingDir = new File(
      parentDir,
      sessionName + "-"
      + new SimpleDateFormat("yyyyMMdd-kkmmss").format(new java.util.Date()));
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
   * Creates data files MFA needs for training.
   * @param graphs Original utterances to align.
   * @param phonemesToIPA Translates phoneme labels to IPA, if necessary.
   * @return The utterances successfully processed.
   * @throws TransformationException
   */
  public List<Graph> createInputFiles(Stream<Graph> graphs, PhonemeTranslator phonemesToIPA)
    throws TransformationException {
    setStatus("Creating input files...");
    try {
      // directories
      tempDir = new File(sessionWorkingDir, "temp");
      tempDir.mkdir();
      corpusDir = new File(sessionWorkingDir, "corpus");
      corpusDir.mkdir();
      alignedDir = new File(sessionWorkingDir, "aligned");
      modelsDir = new File(sessionWorkingDir, "models");
      
      final DecimalFormat formatter = new DecimalFormat("0.0000");
      
      // start dictionary
      dictionary = new TreeMap<String,LinkedHashSet<String>>();
      
      Vector<Graph> utterances = new Vector<Graph>();
      final TreeSet<String> participants = new TreeSet<String>();
      
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
            boolean bJustAddedNoise = false; // TODO add noises and transcribe as "spn" in dict
            
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
              if (orthography == null
                  || orthography.getLabel() == null
                  || orthography.getLabel().length() == 0) { // no orthography
                // but we want to ensure MFA gives us back a word here
                // so that it doesn't link words across a gap that contains something,
                // so give it a non-word
                orthography = new Annotation(null, "spn", orthographyLayerId);
              }
                
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
                      prons.add(phonemesToIPA.apply(sPhonology));
                    }
                  } // next pronunciation
                } // pronunciation is present
              } // train/align
            } // next word
            
            if (utteranceOrthography.toString().trim().length() == 0) {
              setStatus(
                "Fragment has no orthography and will be ignored: " + "\"" + fragment + "\"");
              return;
            }

            // save .lab file and .wav file in subdirectory named after the speaker
            File speakerDir = new File(corpusDir, "unknown");
            Annotation participant = fragment.first(schema.getParticipantLayerId());
            if (participant != null && participant.getLabel().length() > 0) {
              speakerDir = new File(corpusDir, participant.getLabel());
              participants.add(participant.getLabel());
            }
            if (!speakerDir.exists()) speakerDir.mkdir();
            
            // write transcript file
            BufferedWriter transcript = new BufferedWriter(
              new OutputStreamWriter(
                new FileOutputStream(new File(speakerDir, fragment.getId() + ".lab")), "UTF-8"));
            transcript.write(utteranceOrthography.toString());
            transcript.close();
            
            // extract audio...
            String fileUrl = fragment.getMediaProvider().getMedia("", "audio/wav");
            File fTemp = new File(new URI(fileUrl));
            File fWav = new File(speakerDir, fragment.getId() + ".wav");
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

      if (participants.size() > 0) {
        // now that we know some participant IDs, we can give the session a descriptive name
        String firstParticipant = participants.iterator().next();
        renameSession(firstParticipant + (participants.size() == 1?"":"-et-al"));
      }

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
   * Changes the name of the current session, updating all relevant file members.
   * @param newName
   */
  protected void renameSession(String newName) {
    String prefix = "mfa";
    if (utteranceTagLayerId != null) prefix = utteranceTagLayerId;
    else if (phoneAlignmentLayerId != null) prefix = phoneAlignmentLayerId;
    File parentDir = new File(System.getProperty("java.io.tmpdir"));
    if (System.getProperty("os.name").startsWith("Windows")) {
      // MFA doesn't handle paths with spaces well...
      if (parentDir.getPath().contains(" ")) {
        // ...so we try to use a path with no spaces like C:\WINDOWS\TEMP
        parentDir = new File(System.getenv("TEMP"));
      }
      // ...and ensure the new name doesn't include spaces either
      newName = newName.replace(" ", "-");
    }
    File newSessionWorkingDirectory = new File(
      parentDir,
      prefix + "-" + newName
      +"-"+new SimpleDateFormat("yyyyMMdd-kkmmss").format(new java.util.Date()));
    if (sessionWorkingDir.renameTo(newSessionWorkingDirectory)) {
      sessionWorkingDir = newSessionWorkingDirectory;
      sessionName = newName;
      File oldLogFile = logFile;
      logFile = new File(getWorkingDirectory(), sessionWorkingDir.getName() + ".log");
      if (oldLogFile != null && oldLogFile.exists()) {
        oldLogFile.renameTo(logFile);
      }
      tempDir = new File(sessionWorkingDir, "temp");
      corpusDir = new File(sessionWorkingDir, "corpus");
      alignedDir = new File(sessionWorkingDir, "aligned");
      modelsDir = new File(sessionWorkingDir, "models");
      setStatus("Session name now: " + sessionName);
    } // rename successful
  } // end of renameSession()

  private Execution exe = null;
  
  /**
   * Execute an mfa command
   * @param ignoreErrors Whether to ignore errors (true), or throw an exception when
   * stderr or stdout include the word "error" (false).
   * @param mfaRootDir Which directory to use for MFA_ROOT_DIR - i.e. MFA's working directory.
   * @param args The command line arguments.
   * @return The output of the command.
   * @throws TransformationException If execution fails.
   */
  public String mfa(boolean ignoreErrors, File mfaRootDir, String... args) throws TransformationException {
    setStatus("mfa " + Arrays.stream(args).collect(Collectors.joining(" ")));

    File mfa = new File(mfaPath, "mfa");
    if (!mfa.exists()) mfa = new File(mfaPath, "mfa.exe");
    File envPath = new File(mfaPath).getParentFile();
    File condaPath = envPath.getParentFile().getParentFile();
    File condaBin = new File(condaPath, "bin");
    if (!condaBin.exists()) condaBin = new File(condaPath, "condabin");
    File conda = new File(condaBin, "conda");
    if (!conda.exists()) conda = new File(condaBin, "conda.bat");

    exe = new Execution();
    if (System.getProperty("os.name").startsWith("Windows")) {
      exe.env("MFA_ROOT_DIR", mfaRootDir.getPath())
        .setWorkingDirectory(envPath)
        .setExe("cmd").arg("/C").arg(conda.getPath()) // or: .setExe(conda)        
        .arg("run")
        .arg("--no-capture-output")
        .arg("-p").arg(envPath.getPath())
        .arg("mfa");
      // inherit current environment
      exe.getEnvironmentVariables().putAll(System.getenv());
    } else { // non-Windows systems call mfa directly (conda run doesn't work)
      exe.env("PATH", System.getenv("PATH")+pathVariableSuffix())
        .env("HOME", mfaRootDir.getPath())
        .env("MFA_ROOT_DIR", mfaRootDir.getPath())
        .setExe(mfa); // TODO -j <num_jobs>
    }

    for (String arg : args) exe.arg(arg);
    exe.getStdoutObservers().add(s-> {
        // log each line seperately
        StringTokenizer lines = new StringTokenizer(s.replaceAll("[[0-9]+m",""), "\n\r");
        while (lines.hasMoreTokens()) {
          String line = lines.nextToken().trim();
          if (line.length() > 0) { // skip blank lines
            setStatus(line);
          }
        }
      });
    exe.getStderrObservers().add(s-> {
        // is it a progress bar?
        Matcher progressMatcher = progressPattern.matcher(s);
        if (!progressMatcher.find()) {
          StringTokenizer lines = new StringTokenizer(s, "\n\r");
          while (lines.hasMoreTokens()) {
            String line = lines.nextToken().trim();
            setStatus(line);
          }
        }
      });
    exe.run();
    String stdout = exe.stdout().toString();
    if (!ignoreErrors) {
      if (errorPattern.matcher(stdout).matches()) {
        throw new TransformationException(
          this, "Error running mfa "+Arrays.stream(args).collect(Collectors.joining(" "))
          + " : " + stdout);
      }
      if (errorPattern.matcher(exe.stderr()).matches()) { // TODO check exit code - non-zero=error
        throw new TransformationException(
          this, "Error running mfa "+Arrays.stream(args).collect(Collectors.joining(" "))
          + " : " + exe.stderr());
      }
    }
    setStatus("complete: mfa " + Arrays.stream(args).collect(Collectors.joining(" ")));
    return stdout;
  } // end of mfa()
  
  /**
   * Copies the contents of the given log file into the session log.
   * @param log
   */
  public void copyLog(File log) {
    if (log != null && log.exists() && logFile != null) {
      setStatus("=== Logging contents of " + log.getName() + " ===");
      try {
        IO.Pump(new FileInputStream(log), new FileOutputStream(logFile, true));
        setStatus("=== Contents of " + log.getName() + " logged ===");
      } catch(Exception exception) {
        setStatus("=== Could not log contents of " + log.getName() + ": " + exception + " ===");
      }
    }
  } // end of copyLog()
  
  /**
   * Reads the alignments from the files output by MFA, and merges the changes into the
   * original fragments.
   * @param mfaToPhonemes Phoneme label converter to use.
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
      ids.add(schema.getWordLayerId()); // TODO need this really?
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

      // output files are in subdirectories of alignedDir
      Vector<File> outputFiles = new Vector<File>();
      File[] alignmentFiles = alignedDir.listFiles();
      if (alignmentFiles != null) {
        for (File d : alignmentFiles) {
          if (d.isFile()) { // probably unaligned.txt
            outputFiles.add(d);
          } else if (d.isDirectory()) {
            for (File f : d.listFiles()) {
              if (f.isFile()) { // presumably a TextGrid
                outputFiles.add(f);
              }
            } // next subdir file
          } // subdir
        } // next corpusDir fild
      } else { // alignmentFiles == null
        if (!alignedDir.exists()) {
          setStatus("Alignments directory doesn't exist: " + alignedDir.getPath());
        } else {
          setStatus("Alignments directory is empty: " + alignedDir.getPath());
        }
      }
      if (outputFiles.size() == 0) {
        setStatus("No alignments were produced.");
        throw new TransformationException(this, "No alignments were produced.");
      }
      
      // list all TextGrids in output directory
      for (File f : outputFiles) {
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
          // all phone anchors are automatically generated
          for (Annotation phone : alignedFragment.all(phoneAlignmentLayerId)) {
            phone.getStart().setConfidence(Constants.CONFIDENCE_AUTOMATIC);
            phone.getEnd().setConfidence(Constants.CONFIDENCE_AUTOMATIC);
          }

          // remove "spn" or empty phones (for out of dictionary words)
          alignedFragment.trackChanges();
          for (Annotation phone : alignedFragment.all(phoneAlignmentLayerId)) {
            if (phone.getLabel().equals("spn")
                || phone.getLabel().trim().length() == 0) {
              setStatus("Removing spn " + phone.getStart() + "-" + phone.getEnd());
              phone.destroy();
            }
          } // next phone

          // create unaligned start/end anchors for assigning to dummy participant/turn
          Anchor unknownStart = alignedFragment.addAnchor(new Anchor());
          Anchor unknownEnd = alignedFragment.addAnchor(new Anchor());

          alignedFragment.commit();

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
                .setLabel(participant.getLabel())
                .setStartId(unknownStart.getId())
                .setEndId(unknownEnd.getId()));
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
                .setOrdinal(turn.getOrdinal())
                .setParentId(editedParticipant.getId())
                .setStartId(unknownStart.getId())
                .setEndId(unknownEnd.getId()));
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
              // convert labels from one encoding to another
              for (Annotation phone : alignedFragment.all(phoneAlignmentLayerId)) {
                phone.setLabel(mfaToPhonemes.apply(phone.getLabel()));
              }
            }
            if (isCancelling()) break;
            
            // merge the current database utterance with the incoming aligned utterance
            alignedFragment.commit();
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
            if (merger.getLog() != null) merger.getLog().forEach(l -> setStatus(l));
           
            if (utteranceTagLayerId != null) { // add timestamp tag?
              Annotation[] timestamps = fragment.tagsOn​(utteranceTagLayerId);
              if (timestamps.length > 0) { // update the existing tag
                timestamps[0].setLabel(sTimestamp);
                timestamps[0].setConfidence(Constants.CONFIDENCE_AUTOMATIC);
              } else { // add new tag
                Annotation timestamp = new Annotation()
                  .setLayerId(utteranceTagLayerId)
                  .setLabel(sTimestamp)
                  .setStart(utterance.getStart())
                  .setEnd(utterance.getEnd())
                  .setParentId(utterance.getParentId());
                timestamp.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
                fragment.addAnnotation(timestamp);
              } // add new tag            
            }

            // ensure the utterance boundaries are unchanged (e.g. even by rounding)
            utterance.getStart().rollback();
            utterance.getEnd().rollback();

            if (dependentLayerIds.size() > 0) { // if there are aligned child layers
              // ensure their anchors are recomputed if they've got low-confidence alignments
              defaultOffsetGenerator.transform(fragment);
              
              // now scan the dependent layers and fix up any child anchors that are out of bounds
              // TODO fix merge/defaultOffsetGenerator so that this hack isn't necessary
              // TODO AnnotatorWrapperManager rolls back these changes anyway!
              for (String layerId : dependentLayerIds) {
                for (Annotation a : fragment.all(layerId)) {

                  // start anchor:
                  // was it originally connected to the parent?
                  if (!a.getStartId().equals(a.getParent().getStartId())
                      && a.getOriginalStartId().equals(a.getParent().getOriginalStartId())) {
                    a.setStart(a.getParent().getStart());
                  } else if (a.getStart().getOffset() < a.getParent().getStart().getOffset()) {
                    // is the start now too early?
                    a.setStart(a.getParent().getStart());
                  }

                  // end anchor:
                  // was it originally connected to the parent?
                  if (!a.getEndId().equals(a.getParent().getEndId())
                      && a.getOriginalEndId().equals(a.getParent().getOriginalEndId())) {
                    a.setEnd(a.getParent().getEnd());
                  } else if (a.getEnd().getOffset() > a.getParent().getEnd().getOffset()) {
                    // is the end now too late?
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
    } catch (Throwable x) {
      setStatus("updateAlignments: " + x);
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
