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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
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
import nzilbb.ag.serialize.GraphDeserializer;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.util.DefaultOffsetGenerator;
import nzilbb.ag.util.Merger;
import nzilbb.configure.ParameterSet;
import nzilbb.encoding.CMU2DISC;
import nzilbb.encoding.DISC2CMU;
import nzilbb.encoding.PhonemeTranslator;
import nzilbb.formatter.htk.mlf.MlfDeserializer;
import nzilbb.util.Execution;
import nzilbb.util.IO;

/**
 * Annotator that used HTK to force-align given graphs.
 * @author Robert Fromont robert@fromont.net.nz
 */
// Migration notes:
@UsesFileSystem
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
   * Default constructor.
   */
  public HTKAligner() {
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
    if (htkPath == null) {
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
      if (HVite == null || !HVite.exists()) {
        boolean canCompileSource = Execution.Which("make") != null;
        if (canCompileSource && htkUserId != null && htkPassword != null) {
          // don't have HTK path but maybe we can download/build/install it
          
          // TODO download for Windows too
          // https://htk.eng.cam.ac.uk/ftp/software/htk-3.3-windows-binary.zip
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
              } else {
                setStatus("Sorry, could not build HTK from source code.");
              }
            }
          } finally { // no matter what, delete our working files
            IO.RecursivelyDelete(htkSourceDirectory);
          }
        } else { // can't build
          throw new InvalidConfigurationException(this, "No path to HTK.");
        }
      } // bad htkPath
      
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
    if (noiseLayerId != null && noiseLayerId.length() > 0 && schema.getLayer(noiseLayerId) == null)
      throw new InvalidConfigurationException(this, "Noise layer not found: " + noiseLayerId);

    if (utteranceTagLayerId != null && utteranceTagLayerId.length() > 0) {
      Layer utteranceTagLayer = schema.getLayer(utteranceTagLayerId);
      if (utteranceTagLayer == null) {
        schema.addLayer(
          new Layer(utteranceTagLayerId)
          .setAlignment(Constants.ALIGNMENT_INTERVAL)
          .setPeers(true).setPeersOverlap(false)
          .setParentId(schema.getTurnLayerId()));
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
    if (participantTagLayerId != null && participantTagLayerId.length() > 0) {
      Layer participantTagLayer = schema.getLayer(participantTagLayerId);
      if (participantTagLayer == null) {
        schema.addLayer(
          new Layer(participantTagLayerId)
          .setAlignment(Constants.ALIGNMENT_INTERVAL)
          .setPeers(true).setPeersOverlap(false)
          .setParentId(schema.getTurnLayerId()));
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
        .setPeers(true).setPeersOverlap(false)
        .setParentId(schema.getTurnLayerId());
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
        .setPeers(true).setPeersOverlap(false)
        .setParentId(schema.getTurnLayerId());
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
    } else if (!phoneAlignmentLayer.getParentId().equals(schema.getWordLayerId())
               && !phoneAlignmentLayer.getParentId().equals(schema.getTurnLayerId())) {
      throw new InvalidConfigurationException(
        this, "Phone alignment layer "+phoneAlignmentLayerId
        +" must be a child of "+schema.getWordLayerId()
        +" or " + schema.getTurnLayerId()
        +" but is a child of " + phoneAlignmentLayer.getParentId());
    }

    // P2FA requires 11,025Hz sample rate, and doesn't provide scores (or can it? TODO)
    if (useP2FA) {
      sampleRate = Integer.valueOf(11025);
      scoreLayerId = null;
    }

    if (scoreLayerId != null && scoreLayerId.length() > 0) {
      Layer scoreLayer = schema.getLayer(scoreLayerId);
      if (scoreLayer == null) {
        if (phoneAlignmentLayer.getParentId().equals(schema.getWordLayerId())) { // segment layer
          // segment tag layer
          schema.addLayer(
            new Layer(scoreLayerId)
            .setAlignment(Constants.ALIGNMENT_NONE)
            .setPeers(false)
            .setParentId(phoneAlignmentLayer.getId()));
        } else {
          // 'phrase' layer - i.e. aligned child of turn
          schema.addLayer(
            new Layer(scoreLayerId)
            .setAlignment(Constants.ALIGNMENT_INTERVAL)
            .setPeers(true)
            .setParentId(schema.getTurnLayerId()));
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
      }
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
    if (noiseLayerId != null) requiredLayers.add(noiseLayerId);
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
   
  /**
   * Transforms the graph. In this case, the graph is simply summarized, by counting all
   * tokens of each word type, and printing out the result to stdout.
   * @param graph The graph to transform.
   * @return The changes introduced by the tranformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph transform(Graph graph) throws TransformationException {
    setRunning(true);
    try {
      if (useP2FA) {
        // tranformTranscripts but
      }
    } finally {
      setRunning(false);
    }
    return graph;
  }

  private HTK htk;

  /**
   * Force-aligns the given fragments.
   * @param graphs A stream of fragments.
   * @param consumer A consumer for receiving the graphs once they're transformed.
   * @throws TransformationException
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  @Override
  public void transformGraphs(Stream<Graph> graphs, Consumer<Graph> consumer)
    throws TransformationException, InvalidConfigurationException {
    setPercentComplete(0);
    setRunning(true);

    try {
      
      PhonemeTranslator phonemesToHtk = new PhonemeTranslator(); // (default no translation)
      PhonemeTranslator htkToPhonemes = phonemesToHtk;
      boolean discDictionary = "D".equals(schema.getLayer(pronunciationLayerId).get("subtype"));
      boolean discOutput = "D".equals(schema.getLayer(phoneAlignmentLayerId).get("subtype"));

      List<Graph> fragments = null;
      TransformationException failure = null;
      try {
        
        if (!useP2FA) { // train & align
          
          // create initial file structure
          
          // create input files
          
          // if there are still some utterances
          
          // step 1
          // step 2
          // step 3 is record audio, which we've already done
          // step 4
          // step 5 is extract features from audio, which we've already done
          // step 6
          // step 7
          // step 8
          
          // get word alignments
          
        } else { // useP2FA
          
          // create initial file structure
          createSessionWorkingDir();
          
          // convert output to DISC?
          if (discDictionary) {
            setStatus("discDictionary");
            phonemesToHtk = new DISC2CMU().setDefaultStress("1");
            htkToPhonemes = new CMU2DISC();
          } else {
            if (discOutput) {
              setStatus("discOutput");
              htkToPhonemes = new CMU2DISC();
            }
          }
          
          // create input files
          fragments = createInputFiles(graphs, phonemesToHtk);
          
          // force align
          forceAlign();
        }
        
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
      
      setPercentComplete(100);
    } finally {
      setRunning(false);
    }
  } // end of transformTranscripts()

  /** The working directory for this training session. */
  protected String sessionName = "htk";
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
  /** Pause markers */
  protected HashMap<String,String> htPauseMarkers;
  /** Compiled noise patterns */
  protected HashMap<String,Pattern> noisePatternsMap;
  /** Left channel participant pattern */
  protected Pattern leftPatternRegex;
  /** Right channel participant pattern */
  protected Pattern rightPatternRegex;
  
  /**
   * Provides the working directory for this training session.
   * @return The working directory for temporary files.
   * @throws TransformationException if the directory couldn't be created.
   */
  protected File createSessionWorkingDir() throws TransformationException{
    sessionWorkingDir = new File(
      getWorkingDirectory(),
      sessionName + "-"
      + new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss").format(new java.util.Date()));
    if (!sessionWorkingDir.mkdirs()) {
      throw new TransformationException(
        this, "Failed to create working directory: " + sessionWorkingDir.getPath());
    }
    logFile = new File(sessionWorkingDir, "training.log");
    getStatusObservers().add(status -> {
        try {
          PrintWriter out = new PrintWriter(new FileOutputStream(logFile, true));
          out.println(status);
          out.close();
        } catch(IOException exception) {
        }
      });
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
      for (String sPauseMarker : pauseMarkers.split(" ")) {
        htPauseMarkers.put(sPauseMarker, "SILENCE");
      }
      
      StringTokenizer stNoisePatterns = new StringTokenizer(noisePatterns);
      noisePatternsMap = new HashMap<String,Pattern>();
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
      
      leftPatternRegex = leftPattern == null || leftPattern.length() == 0?
        null:Pattern.compile(leftPattern);
      rightPatternRegex = rightPattern == null || rightPattern.length() == 0?
        null:Pattern.compile(rightPattern);
      
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
                "Fragment has an undefined start time and will be ignored: \""+fragment.getId()+"\"");
              return;
            }
            if (fragment.getEnd().getOffset() == null) {
              setStatus(
                "Fragment has an undefined end time and will be ignored: \""+fragment.getId()+"\"");
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
                "Fragment has no words and will be ignored: "
                + "\"" + fragment.getId() + "\"");
              return;
            }
            
            // check whether the whole transcript has audio
            // (we don't ask the fragment directly for audio, because that would create a
            //  new sample file, and we don't want it yet)
            String audioUrl = fragment.sourceGraph().getMediaProvider().getMedia("", "audio/wav");
            if (audioUrl == null) {
              setStatus(
                "Fragment has no media available and will be ignored: \""+fragment+"\"");
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
            
            utterances.add(fragment);
            
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
          throw new Exception("The word '" + sWord + "' has no pronunciation defined");
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
      if (noiseLayerId != null && noiseLayerId.length() > 0) {
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
   * @throws Exception
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
      IO.Rename(fTemp, fWav);
      
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
        "S", "SILENCE", 0.0, 5.0,
        macros, hmmdefs, alignedWordsMlf,
        wordsMlf, 
        trainingScp, 
        dictionaryFile, 
        phones);
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
   * @throws Exception
   */
  public void updateAlignments(
    PhonemeTranslator htkToPhonemes, boolean useP2FACorrection,
    List<Graph> originalFragments, Consumer<Graph> consumer)
    throws TransformationException {
    try {
      
      // scan the alignments, updating transcript words
      setStatus("Update word and phoneme alignments");
      
      // need a list of noise tokens - i.e. uppcase versions of the noise 'phones'
      HashSet<String> noiseIds = new HashSet<String>();
      for (String sNoisePhone : noisePatternsMap.keySet()) {
        noiseIds.add(sNoisePhone.toUpperCase());
      }
      
      // label for tagging utterance/participant as aligned
      String sTimestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new java.util.Date());
      LinkedHashSet<String> participantIds = new LinkedHashSet<String>();
      
      // TODO this should be in saveTranscript
      // // get a list of word tag layers (so anchor sharing can be fixed up after saving)
      // PreparedStatement sqlWordTagLayers = connection.prepareStatement(
      //    "SELECT layer_id FROM layer WHERE alignment = 0 AND scope = 'W'");
      // Vector<Integer> vWordTagLayers = new Vector<Integer>();
      // ResultSet rsWordTagLayers = sqlWordTagLayers.executeQuery();
      // while (rsWordTagLayers.next()) {
      //    vWordTagLayers.add(rsWordTagLayers.getInt("layer_id"));
      // }
      // rsWordTagLayers.close();
      // sqlWordTagLayers.close();
      // // prepare tag fixup statement for later
      // PreparedStatement sqlFixTagAnchors = connection.prepareStatement(
      //    "UPDATE annotation_layer_? SET start_anchor_id = ?, end_anchor_id = ? WHERE word_annotation_id = ?");
      
      // read the graphs out of the file...
      
      // get/configure deserializer
      ParameterSet configuration = new ParameterSet();
      GraphDeserializer deserializer = new MlfDeserializer();
      configuration = deserializer.configure(configuration, schema);
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
      
      Vector<String> ids = new Vector<String>();
      Vector<String> dependentLayerIds = new Vector<String>();
      ids.add(schema.getTurnLayerId());
      ids.add(schema.getUtteranceLayerId());
      ids.add(schema.getWordLayerId());
      if (utteranceTagLayerId != null) ids.add(utteranceTagLayerId);
      if (phoneAlignmentLayerId != null) ids.add(phoneAlignmentLayerId);
      if (scoreLayerId != null) ids.add(scoreLayerId);
      // also include any aligned word layers, so that their anchors can be validated
      // with the new alignments
      DefaultOffsetGenerator defaultOffsetGenerator = new DefaultOffsetGenerator();
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
      String[] layerIds = ids.toArray(new String[0]);
      
      // create a map of IDs to original fragments
      HashMap<String,Graph> idToFragment = new HashMap<String,Graph>();
      for (Graph f : originalFragments) idToFragment.put(f.getId(), f);
      
      // for each utterance alignment...
      for (Graph alignedFragment : alignedFragments) {
        
        // anchors start from zero, which they don't in the database
        alignedFragment.shiftAnchors((Double)alignedFragment.get("@startTime"));
        
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
          for (Annotation word : alignedFragment.all(schema.getWordLayerId())) {
            word.setParent(editedTurn);
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
          
          // merge the current database utterance with the incoming aligned utterance
          Merger merger = new Merger(alignedFragment);
          // but don't allow changes to system layers
          merger.getNoChangeLayers().add(schema.getParticipantLayerId());
          merger.getNoChangeLayers().add(schema.getTurnLayerId());
          merger.getNoChangeLayers().add(schema.getUtteranceLayerId());
          merger.getNoChangeLayers().add(schema.getWordLayerId());
          fragment.trackChanges();
          // merge changes
          merger.transform(fragment);
          
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
   * Escapes quotes in the given string for inclusion in QL or SQL queries.
   * @param s The string to escape.
   * @return The given string, with quotes escapeed.
   */
  private String esc(String s) {
    if (s == null) return "";
    return s.replace("\\","\\\\").replace("'","\\'");
  } // end of esc()

} // end of class HTKAligner
