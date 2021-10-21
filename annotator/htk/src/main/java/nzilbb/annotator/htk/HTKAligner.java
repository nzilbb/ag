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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.script.*;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
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
        setStatus("Unpacking " + file);
        URL urlSource = getClass().getResource(file);
        File fDestination = getWorkingDirectory();
        String[] pathElements = file.split("/");
        for (String element : pathElements) {
          if (!fDestination.exists()) fDestination.mkdir();
          fDestination = new File(fDestination, element);
        } // next path element
	    
        InputStream isSource = urlSource.openStream();
        FileOutputStream osDestination = new FileOutputStream(fDestination);
        IO.Pump(isSource, osDestination);
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
      // do nothing
    } finally {
      setRunning(false);
    }
    return graph;
  }

  /**
   * Force-aligns the given fragments.
   * @param graphs A stream of fragments.
   * @param consumer A consumer for receiving the graphs once they're transformed.
   * @throws TransformationException
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  @Override
  public void transformTranscripts(Stream<Graph> graphs, Consumer<Graph> consumer)
    throws TransformationException, InvalidConfigurationException {

    // TODO Problem: how to detect simultaneous speech?
    
    List<Graph> transcripts = graphs.collect(Collectors.toList());
    setPercentComplete(0);
    int soFar = 0;
    for (Graph transcript : transcripts) {
      if (isCancelling()) break;
      transform(transcript);
      consumer.accept(transcript);
      if (isCancelling()) break;
      setPercentComplete((int)((double)(soFar * 100) / (double)transcripts.size()));
    } // next transcript
    setPercentComplete(100);
  } // end of transformTranscripts()

} // end of class HTKAligner
