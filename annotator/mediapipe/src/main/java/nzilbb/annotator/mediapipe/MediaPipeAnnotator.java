//
// Copyright 2025 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.annotator.mediapipe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
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
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
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
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.UsesGraphStore;
import nzilbb.ag.util.DefaultOffsetGenerator;
import nzilbb.ag.ql.QL;
import nzilbb.ag.util.Merger;
import nzilbb.util.Execution;
import nzilbb.util.IO;
import nzilbb.util.Timers;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Mediapipe annotator integrates with 
 * <a href="https://github.com/google-ai-edge/mediapipe">mediapipe</a>
 * for processing of video to extract face landmark data.
 * <p> The annotator can save an annotated <tt>.webm</tt> file and annotated frame image
 * annotations, and also saves instantaneous
 * <a href="https://ai.google.dev/edge/mediapipe/solutions/vision/face_landmarker">blendshape</a>
 * score annotations, i.e. facial features that can be used to determine facial expression.
 * @author Robert Fromont robert@fromont.net.nz
 */
@UsesFileSystem @UsesGraphStore
public class MediaPipeAnnotator extends Annotator {
  /** Get the minimum version of the nzilbb.ag API supported by the annotator.*/
  public String getMinimumApiVersion() { return "1.2.3"; }

  // In LaBB-CAT the Tomcat user may have a read-only home directory,
  // so cache files etc. need to be stored somewhere else,
  // specified by the MPLCONFIGDIR environment variable
  // https://matplotlib.org/stable/install/environment_variables_faq.html#envvar-MPLCONFIGDIR
  File MPLCONFIGDIR = null;
  
  // pip needs a lot of temporary file space to install some dependencies
  // it uses the TMPDIR environment variable to determine where
  File TMPDIR = null;
  
  // flag accessible to unit test for keeping generted PNG/MP4
  boolean keepGeneratedMedia = false;
  
  /**
   * Python environment name.
   * @see #getEnvironmentName()
   * @see #setEnvironmentName(String)
   */
  protected String environmentName = "mp_env";
  /**
   * Getter for {@link #environmentName}: Python environment name.
   * @return Python environment name.
   */
  public String getEnvironmentName() { return environmentName; }
  /**
   * Setter for {@link #environmentName}: Python environment name.
   * @param newEnvironmentName Python environment name.
   */
  public MediaPipeAnnotator setEnvironmentName(String newEnvironmentName) { environmentName = newEnvironmentName; return this; }

  /**
   * URL to download the landmarker task from.
   * <p> Default is https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task
   * @see #getTaskUrl()
   * @see #setTaskUrl(String)
   */
  protected String taskUrl = "https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task";
  /**
   * Getter for {@link #taskUrl}: URL to download the landmarker task from.
   * <p> Default is https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task
   * @return URL to download the landmarker task from.
   */
  public String getTaskUrl() { return taskUrl; }
  /**
   * Setter for {@link #taskUrl}: URL to download the landmarker task from.
   * @param newTaskUrl URL to download the landmarker task from.
   */
  public MediaPipeAnnotator setTaskUrl(String newTaskUrl) { taskUrl = newTaskUrl; return this; }
  
  /**
   * The num_faces configuration option: The maximum number of faces that can be detected
   * by the the FaceLandmarker. Smoothing is only applied when num_faces is set to 1. 
   * @see #getNumFaces()
   * @see #setNumFaces(Integer)
   */
  protected Integer numFaces = 1;
  /**
   * Getter for {@link #numFaces}: The num_faces configuration option: The maximum number 
   * of faces that can be detected by the the FaceLandmarker. Smoothing is only applied 
   * when num_faces is set to 1 (the default value).  
   * @return The num_faces configuration option: The maximum number of faces that can be 
   * detected by the the FaceLandmarker. Smoothing is only applied when num_faces is set to 1. 
   */
  public Integer getNumFaces() { return numFaces; }
  /**
   * Setter for {@link #numFaces}: The num_faces configuration option: The maximum number 
   * of faces that can be detected by the the FaceLandmarker. Smoothing is only applied 
   * when num_faces is set to 1. 
   * @param newNumFaces The num_faces configuration option: The maximum number of faces 
   * that can be detected by the the FaceLandmarker. Smoothing is only applied when 
   * num_faces is set to 1.
   */
  public MediaPipeAnnotator setNumFaces(Integer newNumFaces) { numFaces = newNumFaces; return this; }
  
  /**
   * The min_face_detection_confidence configuration option: The minimum confidence score
   * for the face detection to be considered successful. 
   * @see #getMinFaceDetectionConfidence()
   * @see #setMinFaceDetectionConfidence(Double)
   */
  protected Double minFaceDetectionConfidence = 0.5;
  /**
   * Getter for {@link #minFaceDetectionConfidence}: The min_face_detection_confidence
   * configuration option: The minimum confidence score for the face detection to be
   * considered successful. The default is 0.5.
   * @return The min_face_detection_confidence configuration option: The minimum
   * confidence score for the face detection to be considered successful. 
   */
  public Double getMinFaceDetectionConfidence() { return minFaceDetectionConfidence; }
  /**
   * Setter for {@link #minFaceDetectionConfidence}: The min_face_detection_confidence
   * configuration option: The minimum confidence score for the face detection to be
   * considered successful. 
   * @param newMinFaceDetectionConfidence The min_face_detection_confidence configuration
   * option: The minimum confidence score for the face detection to be considered
   * successful. 
   */
  public MediaPipeAnnotator setMinFaceDetectionConfidence(Double newMinFaceDetectionConfidence) { minFaceDetectionConfidence = newMinFaceDetectionConfidence; return this; }

  /**
   * The min_face_presence_confidence configuration option: The minimum confidence score
   * of face presence score in the face landmark detection. 
   * @see #getMinFacePresenceConfidence()
   * @see #setMinFacePresenceConfidence(Double)
   */
  protected Double minFacePresenceConfidence = 0.5;
  /**
   * Getter for {@link #minFacePresenceConfidence}: The min_face_presence_confidence
   * configuration option: The minimum confidence score of face presence score in the face
   * landmark detection.  The default is 0.5.
   * @return The min_face_presence_confidence configuration option: The minimum confidence
   * score of face presence score in the face landmark detection. 
   */
  public Double getMinFacePresenceConfidence() { return minFacePresenceConfidence; }
  /**
   * Setter for {@link #minFacePresenceConfidence}: The min_face_presence_confidence
   * configuration option: The minimum confidence score of face presence score in the face
   * landmark detection. 
   * @param newMinFacePresenceConfidence The min_face_presence_confidence configuration
   * option: The minimum confidence score of face presence score in the face landmark
   * detection. 
   */
  public MediaPipeAnnotator setMinFacePresenceConfidence(Double newMinFacePresenceConfidence) { minFacePresenceConfidence = newMinFacePresenceConfidence; return this; }

  /**
   * The min_tracking_confidence configuration option: The minimum confidence score for
   * the face tracking to be considered successful. 
   * @see #getMinTrackingConfidence()
   * @see #setMinTrackingConfidence(Double)
   */
  protected Double minTrackingConfidence = 0.5;
  /**
   * Getter for {@link #minTrackingConfidence}: The min_tracking_confidence configuration
   * option: The minimum confidence score for the face tracking to be considered
   * successful.  The default is 0.5.
   * @return The min_tracking_confidence configuration option: The minimum confidence
   * score for the face tracking to be considered successful. 
   */
  public Double getMinTrackingConfidence() { return minTrackingConfidence; }
  /**
   * Setter for {@link #minTrackingConfidence}: The min_tracking_confidence configuration
   * option: The minimum confidence score for the face tracking to be considered
   * successful. 
   * @param newMinTrackingConfidence The min_tracking_confidence configuration option: The
   * minimum confidence score for the face tracking to be considered successful. 
   */
  public MediaPipeAnnotator setMinTrackingConfidence(Double newMinTrackingConfidence) { minTrackingConfidence = newMinTrackingConfidence; return this; }

  static final String[] blendshapeCategories = {
    "_neutral","browDownLeft","browDownRight","browInnerUp","browOuterUpLeft","browOuterUpRight",
    "cheekPuff","cheekSquintLeft","cheekSquintRight",
    "eyeBlinkLeft","eyeBlinkRight","eyeLookDownLeft","eyeLookDownRight","eyeLookInLeft",
    "eyeLookInRight","eyeLookOutLeft","eyeLookOutRight","eyeLookUpLeft","eyeLookUpRight",
    "eyeSquintLeft","eyeSquintRight","eyeWideLeft","eyeWideRight",
    "jawForward","jawLeft","jawOpen","jawRight",
    "mouthClose","mouthDimpleLeft","mouthDimpleRight",
    "mouthFrownLeft","mouthFrownRight","mouthFunnel",
    "mouthLeft","mouthLowerDownLeft","mouthLowerDownRight",
    "mouthPressLeft","mouthPressRight","mouthPucker","mouthRight",
    "mouthRollLower","mouthRollUpper","mouthShrugLower","mouthShrugUpper",
    "mouthSmileLeft","mouthSmileRight",
    "mouthStretchLeft","mouthStretchRight",
    "mouthUpperUpLeft","mouthUpperUpRight",
    "noseSneerLeft","noseSneerRight"
  };
  
  /**
   * Returns a list of possible categories.
   * @return A list of possible categories.
   */
  public List<String> getBlendshapeCategories() {
    return Arrays.asList(blendshapeCategories);
  } // end of getBlendshapeCategories()
  
  /**
   * Returns a list of possible categories.
   * @return A list of possible categories.
   */
  public List<MediaTrackDefinition> getMediaTracks() {
    if (getStore() != null) {
      try {
        return Arrays.asList(getStore().getMediaTracks());
      } catch(Exception exception) {}
    } 
    return new Vector<MediaTrackDefinition>();
  } // end of getBlendshapeCategories()
  
  /**
   * A map of blend shape categories to layer IDs. 
   * @see #getBlendShapeLayerIds()
   * @see #setBlendShapeLayerIds(Map)
   */
  protected Map<String,String> blendshapeLayerIds = new TreeMap<String,String>();
  /**
   * Getter for {@link #blendshapeLayerIds}: A map of blend shape categories to layer IDs. 
   * @return A map of blend shape categories to layer IDs. 
   */
  public Map<String,String> getBlendshapeLayerIds() { return blendshapeLayerIds; }
  /**
   * Setter for {@link #blendshapeLayerIds}: A map of blend shape categories to layer IDs. 
   * @param newBlendshapeLayerIds A map of blend shape categories to layer IDs. 
   */
  public MediaPipeAnnotator setBlendshapeLayerIds(Map<String,String> newBlendshapeLayerIds) { blendshapeLayerIds = newBlendshapeLayerIds; return this; }
  
  /**
   * The track suffix that the video to analyse should come from.
   * @see #getInputTrackSuffix()
   * @see #setInputTrackSuffix(String)
   */
  protected String inputTrackSuffix = "";
  /**
   * Getter for {@link #inputTrackSuffix}: The track suffix that the video to analyse
   * should come from. 
   * @return The track suffix that the video to analyse should come from.
   */
  public String getInputTrackSuffix() { return inputTrackSuffix; }
  /**
   * Setter for {@link #inputTrackSuffix}: The track suffix that the video to analyse
   * should come from. 
   * @param newInputTrackSuffix The track suffix that the video to analyse should come from.
   */
  public MediaPipeAnnotator setInputTrackSuffix(String newInputTrackSuffix) { inputTrackSuffix = newInputTrackSuffix; return this; }
  
  /**
   * If the full detection result for each frame is required, they will be saved as
   * JSON-encoded files on the layer with this ID. 
   * @see #getResultLayerId()
   * @see #setResultLayerId(String)
   */
  protected String resultLayerId;
  /**
   * Getter for {@link #resultLayerId}: If the full detection result for each frame is
   * required, they will be saved as JSON-encoded files on the layer with this ID. 
   * @return If the full detection result for each frame is required, they will be saved
   * as JSON-encoded files on the layer with this ID. q
   */
  public String getResultLayerId() { return resultLayerId; }
  /**
   * Setter for {@link #resultLayerId}: If the full detection result for each frame is
   * required, they will be saved as JSON-encoded files on the layer with this ID. 
   * @param newResultLayerId If the full detection result for each frame is required, they
   * will be saved as JSON-encoded files on the layer with this ID. 
   */
  public MediaPipeAnnotator setResultLayerId(String newResultLayerId) { resultLayerId = newResultLayerId; return this; }
  
  /**
   * If a video file annotated with facial landmarks is desired, it should be saved with
   * this track suffix. Empty string or null disables annotated video production. 
   * @see #getOutputTrackSuffix()
   * @see #setOutputTrackSuffix(String)
   */
  protected String outputTrackSuffix = "";
  /**
   * Getter for {@link #outputTrackSuffix}: If a video file annotated with facial
   * landmarks is desired, it should be saved with this track suffix. Empty string or null
   * disables annotated video production. 
   * @return If a video file annotated with facial landmarks is desired, it should be
   * saved with this track suffix. Empty string or null disables annotated video
   * production. 
   */
  public String getOutputTrackSuffix() { return outputTrackSuffix; }
  /**
   * Setter for {@link #outputTrackSuffix}: If a video file annotated with facial
   * landmarks is desired, it should be saved with this track suffix. Empty string or null
   * disables annotated video production. 
   * @param newOutputTrackSuffix If a video file annotated with facial landmarks is
   * desired, it should be saved with this track suffix. Empty string or null disables
   * annotated video production. 
   */
  public MediaPipeAnnotator setOutputTrackSuffix(String newOutputTrackSuffix) { outputTrackSuffix = newOutputTrackSuffix; return this; }
  
  /**
   * If frame images annotated with facial landmarks are desired, the images will be
   * saved on a layer with this ID. 
   * @see #getAnnotatedImageLayerId()
   * @see #setAnnotatedImageLayerId(String)
   */
  protected String annotatedImageLayerId = "";
  /**
   * Getter for {@link #annotatedImageLayerId}: If frame images annotated with facial
   * landmarks are desired, the images will be saved on a layer with this ID. 
   * @return If frame images annotated with facial landmarks are desired, the images
   * will be saved on a layer with this ID. 
   */
  public String getAnnotatedImageLayerId() { return annotatedImageLayerId; }
  /**
   * Setter for {@link #annotatedImageLayerId}: If frame images annotated with facial
   * landmarks are desired, the images will be saved on a layer with this ID. 
   * @param newAnnotatedImageLayerId If frame images annotated with facial landmarks are
   * desired, the images will be saved on a layer with this ID. 
   */
  public MediaPipeAnnotator setAnnotatedImageLayerId(String newAnnotatedImageLayerId) { annotatedImageLayerId = newAnnotatedImageLayerId; return this; }
  
  /**
   * Whether to include the face landmarks tesselation on the annotated images/video.
   * @see #getPaintTesselation()
   * @see #setPaintTesselation(Boolean)
   */
  protected Boolean paintTesselation;
  /**
   * Getter for {@link #paintTesselation}: Whether to include the face landmarks
   * tesselation on the annotated images/video. 
   * @return Whether to include the face landmarks tesselation on the annotated images/video.
   */
  public Boolean getPaintTesselation() {
    return Optional.ofNullable(paintTesselation).orElse(Boolean.FALSE);
  }
  /**
   * Setter for {@link #paintTesselation}: Whether to include the face landmarks
   * tesselation on the annotated images/video. 
   * @param newPaintTesselation Whether to include the face landmarks tesselation on the
   * annotated images/video. 
   */
  public MediaPipeAnnotator setPaintTesselation(Boolean newPaintTesselation) { paintTesselation = newPaintTesselation; return this; }

  /**
   * Whether to include the face contours on the annotated images/video.
   * @see #getPaintContours()
   * @see #setPaintContours(Boolean)
   */
  protected Boolean paintContours;
  /**
   * Getter for {@link #paintContours}: Whether to include the face contours on the
   * annotated images/video. 
   * @return Whether to include the face contours on the annotated images/video.
   */
  public Boolean getPaintContours() {
    return Optional.ofNullable(paintContours).orElse(Boolean.FALSE);
  }
  /**
   * Setter for {@link #paintContours}: Whether to include the face contours on the
   * annotated images/video. 
   * @param newPaintContours Whether to include the face contours on the annotated images/video.
   */
  public MediaPipeAnnotator setPaintContours(Boolean newPaintContours) { paintContours = newPaintContours; return this; }
  
  /**
   * Whether to include the irises on the annotated images/video.
   * @see #getPaintIrises()
   * @see #setPaintIrises(Boolean)
   */
  protected Boolean paintIrises;
  /**
   * Getter for {@link #paintIrises}: Whether to include the irises on the annotated images/video.
   * @return Whether to include the irises on the annotated images/video.
   */
  public Boolean getPaintIrises() {
    return Optional.ofNullable(paintIrises).orElse(Boolean.FALSE);
  }
  /**
   * Setter for {@link #paintIrises}: Whether to include the irises on the annotated images/video.
   * @param newPaintIrises Whether to include the irises on the annotated images/video.
   */
  public MediaPipeAnnotator setPaintIrises(Boolean newPaintIrises) { paintIrises = newPaintIrises; return this; }
  
  /**
   * ID of a transcript attribute layer to store a count of the annotated frames.
   * @see #getFrameCountLayerId()
   * @see #setFrameCountLayerId(String)
   */
  protected String frameCountLayerId;
  /**
   * Getter for {@link #frameCountLayerId}: ID of a transcript attribute layer to store a
   * count of the annotated frames. 
   * @return ID of a transcript attribute layer to store a count of the annotated frames.
   */
  public String getFrameCountLayerId() { return frameCountLayerId; }
  /**
   * Setter for {@link #frameCountLayerId}: ID of a transcript attribute layer to store a
   * count of the annotated frames. 
   * @param newFrameCountLayerId ID of a transcript attribute layer to store a count of
   * the annotated frames. 
   */
  public MediaPipeAnnotator setFrameCountLayerId(String newFrameCountLayerId) { frameCountLayerId = newFrameCountLayerId; return this; }
  
  /**
   * Default constructor.
   */
  public MediaPipeAnnotator() {
     // This is the kind of schema we'd like (set here for testing purposes):
    Schema schema = new Schema("who", "turn", "utterance", "word");
    schema.addLayer(
      new Layer("result", "JSON-encoded detection results")
      .setAlignment(Constants.ALIGNMENT_INSTANT)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setType("application/json"));
    schema.addLayer(
      new Layer("frame", "Annotated frame images")
      .setAlignment(Constants.ALIGNMENT_INSTANT)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setType("image/png"));
    for (String category : blendshapeCategories) {
      schema.addLayer(
        new Layer(category, category + " score from mediapipe")
        .setAlignment(Constants.ALIGNMENT_INSTANT)
        .setPeers(true).setPeersOverlap(false).setSaturated(false)
        .setType(Constants.TYPE_NUMBER));
      blendshapeLayerIds.put(category, null);
    }
    setSchema(schema);
  } // end of constructor
  
  /**
   * Ensures Python, venv, and mediapipe are available.
   * @throws InvalidConfigurationException
   * @see #getConfig()
   * @see #beanPropertiesFromQueryString(String)
   */ 
  public void setConfig(String config) throws InvalidConfigurationException {
    setRunning(true);
    try {
      setStatus(""); // clear any residual status from the last run...
      if (System.getProperty("os.name").startsWith("Windows")) {
        throw new InvalidConfigurationException(
          this, "Sorry, this annotator currently doesn't work on Windows systems.");
      }

      // extract scripts
      String script = "blendshapes.py";
      URL urlSource = getClass().getResource(script);
      File destination = new File(getWorkingDirectory(), "blendshapes-"+getVersion()+".py");
      setStatus("Unpacking " + destination.getName() + " from " + urlSource);
      IO.SaveInputStreamToFile​(urlSource.openStream(), destination);
      new Execution()
        .setExe("chmod")
        .arg("u+x").arg(destination.getPath())
        .setWorkingDirectory(getWorkingDirectory())
        .run();
      
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
        .arg("-m").arg("venv").arg(environmentName)
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
            .arg("-m").arg("venv").arg(environmentName)
            .setWorkingDirectory(getWorkingDirectory());
          cmd.getStdoutObservers().add(m->setStatus(m));
          cmd.getStderrObservers().add(m->setStatus(m));
          cmd.run();
        }
        if (cmd.getProcess().exitValue() > 0) {
          setStatus("Cannot install 'venv' module (" + cmd.getProcess().exitValue() + ")");
          throw new InvalidConfigurationException(
            this, "Cannot run 'venv' module; Please install support for python environments."
            +"\ne.g. on Ubuntu: apt install python3.10-venv");
        }
      }
      setStatus(cmd.getInput().toString());

      // In LaBB-CAT the Tomcat user may have a read-only home directory,
      // so cache files etc. need to be stored somewhere else,
      // specified by the MPLCONFIGDIR environment variable
      // create directory used for MPLCONFIGDIR
      // https://matplotlib.org/stable/install/environment_variables_faq.html#envvar-MPLCONFIGDIR
      MPLCONFIGDIR = new File(getWorkingDirectory(), "matplotlib");
      if (!MPLCONFIGDIR.exists()) {
        setStatus("Creating directory " + MPLCONFIGDIR.getName());
        if (!MPLCONFIGDIR.mkdir()) {
          setStatus("Could not create directory " + MPLCONFIGDIR.getPath());
        }
      }

      // pip needs a lot of temporary file space to install some dependencies
      // it uses the TMPDIR environment variable to determine where
      // we force this to be encapsulated under our working directory
      TMPDIR = new File(getWorkingDirectory(), "tmp");
      if (!TMPDIR.exists()) {
        setStatus("Creating tmp directory " + TMPDIR.getName());
        if (!TMPDIR.mkdir()) {
          setStatus("Could not create tmp directory " + TMPDIR.getPath());
        }
      }

      // install mediapipe
      File cacheDir = new File(getWorkingDirectory(), "cache");
      if (!cacheDir.mkdir()) setStatus("Could not create pip cache directory: "+cacheDir.getPath());
      cmd = executeInEnvironment("pip install --cache-dir='"+cacheDir.getPath()+"' mediapipe");
      if (cmd.getProcess().exitValue() > 0) {
        setStatus("Cannot install mediapipe - status: " + cmd.getProcess().exitValue());
        throw new InvalidConfigurationException(
          this, "Cannot install mediapipe: " + cmd.getProcess().exitValue());
      }
      setPercentComplete(50);

      // download task if required
      File task = new File(getWorkingDirectory(), "face_landmarker.task");
      if (!task.exists()) {
        setStatus("Downloading task from " + taskUrl);
        IO.SaveUrlToFile​(new URL(taskUrl), task, p->setPercentComplete(50 + (p/2)));
      }       
      
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
   * Executes a shell command in the python environment.
   * @param command The command to run in the python environment named {@link #environmentName}
   * @return The execution, which will have already finished.
   * @throws IOException, FileNotFoundException
   */
  public Execution executeInEnvironment(String command) throws IOException, FileNotFoundException {
    setStatus("In " + environmentName + " executing: " + command);
    File script = File.createTempFile("MediaPipeAnnotator-", ".sh", getWorkingDirectory()); // TODO .bat on Windows
    script.deleteOnExit();
    try {
      PrintWriter scriptWriter = new PrintWriter(script, "UTF-8");
      try {
        scriptWriter.println("set -e");
        scriptWriter.println("source "+environmentName+"/bin/activate");
        scriptWriter.println(command);
      } finally {
        scriptWriter.close();
      }
      if (MPLCONFIGDIR == null) {
        MPLCONFIGDIR = new File(getWorkingDirectory(), "matplotlib");
      }
      if (TMPDIR == null) {
        TMPDIR = new File(getWorkingDirectory(), "tmp");
      }
      Execution py = new Execution()
        .setExe("bash") // TODO windows interpreter
        .arg(script.getPath())
        .setWorkingDirectory(getWorkingDirectory())
        // In LaBB-CAT the Tomcat user may have a read-only home directory,
        // so cache files etc. need to be stored somewhere else,
        // specified by the MPLCONFIGDIR environment variable
        .env("MPLCONFIGDIR", MPLCONFIGDIR.getPath())
        // pip needs a lot of temporary file space to install some dependencies
        // it uses the TMPDIR environment variable to determine where
        .env("TMPDIR", TMPDIR.getPath());
      py.getStdoutObservers().add(m->setStatus(m));
      py.getStderrObservers().add(m->setStatus(m));
      try {
        python.add(py);
        py.run();
        return py;
      } finally {
        python.remove(py);
      }
    } finally {
      script.delete();
    }
  } // end of executeInEnvironment()
  
  /**
   * Sets the configuration for a given annotation task.
   * @param parameters The configuration of the annotator; a value of <tt> null </tt>
   * is invalid.
   * @throws InvalidConfigurationException
   */
  public void setTaskParameters(String parameters) throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");

    // reset values
    numFaces = 1;
    minFaceDetectionConfidence = 0.5;
    minFacePresenceConfidence = 0.5;
    minTrackingConfidence = 0.5;
    inputTrackSuffix = "";
    outputTrackSuffix = "";
    resultLayerId = "";
    annotatedImageLayerId = "";
    frameCountLayerId = "";
    paintTesselation = null;
    paintContours = null;
    paintIrises = null;
    for (String category : blendshapeLayerIds.keySet()) blendshapeLayerIds.put(category, null);

    if (parameters != null) {
      beanPropertiesFromQueryString(parameters);
    }

    // parse/validate category score layer ID parameters
    if (inputTrackSuffix == null) inputTrackSuffix = "";
    if (outputTrackSuffix == null) outputTrackSuffix = "";

    if (resultLayerId == null) resultLayerId = "";
    if (annotatedImageLayerId == null) annotatedImageLayerId = "";
    if (resultLayerId.length() > 0 && resultLayerId.equals(annotatedImageLayerId)) {
      throw new InvalidConfigurationException(
        this, "resultLayer ("+resultLayerId+") cannot be the same as annotatedImageLayerId");
    }
    Layer resultLayer = null;
    if (resultLayerId.length() > 0) {
      resultLayer = schema.getLayer(resultLayerId);
      if (resultLayer == null) { // layer doesn't exist
        // create it
        schema.addLayer(
          new Layer(resultLayerId)
          .setAlignment(Constants.ALIGNMENT_INSTANT)
          .setPeers(true).setPeersOverlap(false).setSaturated(false)
          .setParentId(schema.getRoot().getId())
          .setType("application/json")
          .setDescription("Frame images annotated with JSON-encoded detection results"));        
      } else if (resultLayer.getParent() == null
                 || !resultLayer.getParent().getId().equals(schema.getRoot().getId())
                 || resultLayer.getId().equals(schema.getParticipantLayerId())
                 || resultLayer.getId().equals(schema.getTurnLayerId())
                 || resultLayer.getId().equals(schema.getUtteranceLayerId())
                 || resultLayer.getId().equals(schema.getWordLayerId())
                 || resultLayer.getId().equals(schema.getCorpusLayerId())
                 || resultLayer.getId().equals(schema.getEpisodeLayerId())) {
        throw new InvalidConfigurationException(
          this, "resultLayer ("+resultLayerId
          + ") must be a non-system span layer, but parent layer is "
          + resultLayer.getParent());
      } else {
        if (resultLayer.getAlignment() != Constants.ALIGNMENT_INSTANT) {
          resultLayer.setAlignment(Constants.ALIGNMENT_INSTANT);
        }
        if (!resultLayer.getType().equals("application/json")) {
          resultLayer.setType("application/json");
        }
      }
    }
    
    Layer annotatedImageLayer = null;
    if (annotatedImageLayerId.length() > 0) {
      annotatedImageLayer = schema.getLayer(annotatedImageLayerId);
      if (annotatedImageLayer == null) { // layer doesn't exist
        // create it
        annotatedImageLayer = schema.addLayer(
          new Layer(annotatedImageLayerId)
          .setAlignment(Constants.ALIGNMENT_INSTANT)
          .setPeers(true).setPeersOverlap(false).setSaturated(false)
          .setParentId(schema.getRoot().getId())
          .setType("image/png")
          .setDescription("Frame images annotated with facial landmarks by mediapipe"));        
        if (resultLayer != null
            && Optional.ofNullable(resultLayer.getCategory()).orElse("").length()>0) {
          // copy category of main layer to save manual work
          annotatedImageLayer.setCategory(resultLayer.getCategory());
        }
      } else if (annotatedImageLayer.getParent() == null
                 || !annotatedImageLayer.getParent().getId().equals(schema.getRoot().getId())
                 || annotatedImageLayer.getId().equals(schema.getParticipantLayerId())
                 || annotatedImageLayer.getId().equals(schema.getTurnLayerId())
                 || annotatedImageLayer.getId().equals(schema.getUtteranceLayerId())
                 || annotatedImageLayer.getId().equals(schema.getWordLayerId())
                 || annotatedImageLayer.getId().equals(schema.getCorpusLayerId())
                 || annotatedImageLayer.getId().equals(schema.getEpisodeLayerId())) {
        throw new InvalidConfigurationException(
          this, "annotatedImageLayer ("+annotatedImageLayerId
          + ") must be a non-system span layer, but parent layer is "
          + annotatedImageLayer.getParent());
      } else {
        if (annotatedImageLayer.getAlignment() != Constants.ALIGNMENT_INSTANT) {
          annotatedImageLayer.setAlignment(Constants.ALIGNMENT_INSTANT);
        }
        if (!annotatedImageLayer.getType().equals("image/png")) {
          annotatedImageLayer.setType("image/png");
        }
      }
    }
    
    if (frameCountLayerId == null) frameCountLayerId = "";
    if (frameCountLayerId.length() > 0) {
      Layer frameCountLayer = schema.getLayer(frameCountLayerId);
      if (frameCountLayer == null) { // layer doesn't exist
        // create it
        schema.addLayer(
          new Layer(frameCountLayerId)
          .setAlignment(Constants.ALIGNMENT_NONE)
          .setPeers(false).setPeersOverlap(false).setSaturated(true)
          .setParentId(schema.getRoot().getId())
          .setType(Constants.TYPE_NUMBER)
          .setDescription("Number of frames annotated by mediapipe"));        
      } else if (frameCountLayer.getParent() == null
                 || !frameCountLayer.getParent().getId().equals(schema.getRoot().getId())
                 || frameCountLayer.getAlignment() != Constants.ALIGNMENT_NONE
                 || frameCountLayer.getId().equals(schema.getParticipantLayerId())
                 || frameCountLayer.getId().equals(schema.getTurnLayerId())
                 || frameCountLayer.getId().equals(schema.getUtteranceLayerId())
                 || frameCountLayer.getId().equals(schema.getWordLayerId())
                 || frameCountLayer.getId().equals(schema.getCorpusLayerId())
                 || frameCountLayer.getId().equals(schema.getEpisodeLayerId())) {
        throw new InvalidConfigurationException(
          this, "frameCountLayer ("+frameCountLayerId
          + ") must be a non-system transcript attribute layer, but parent layer is "
          + frameCountLayer.getParent());
      }
    }
    
    // set blendshape layer IDs from query string
    for (String parameter : parameters.split("&")) {
      int equals = parameter.indexOf('=');
      if (equals <= 0) continue;
      String category = parameter.substring(0, equals);
      String layerId = parameter.substring(equals + 1);
      if (blendshapeLayerIds.containsKey(category)) {
        if (layerId.length() > 0) {
          blendshapeLayerIds.put(category, layerId);
          Layer categoryLayer = schema.getLayer(layerId);
          if (categoryLayer == null) {
            categoryLayer = schema.addLayer(
              new Layer(layerId)
              .setAlignment(Constants.ALIGNMENT_INSTANT)
              .setPeers(true).setPeersOverlap(false).setSaturated(false)
              .setParentId(schema.getRoot().getId())
              .setType(Constants.TYPE_NUMBER)
              .setDescription(category + " score from mediapipe"));
            if (resultLayer != null
                && Optional.ofNullable(resultLayer.getCategory()).orElse("").length()>0) {
              // copy category of main layer to save manual work
              categoryLayer.setCategory(resultLayer.getCategory());
            } else if (annotatedImageLayer != null
                && Optional.ofNullable(annotatedImageLayer.getCategory()).orElse("").length()>0) {
              // copy category of main layer to save manual work
              categoryLayer.setCategory(annotatedImageLayer.getCategory());
            }
            
          } else if (categoryLayer.getParent() == null
                     || !categoryLayer.getParent().getId().equals(schema.getRoot().getId())
                     || categoryLayer.getId().equals(schema.getParticipantLayerId())
                     || categoryLayer.getId().equals(schema.getTurnLayerId())
                     || categoryLayer.getId().equals(schema.getUtteranceLayerId())
                     || categoryLayer.getId().equals(schema.getWordLayerId())
                     || categoryLayer.getId().equals(schema.getCorpusLayerId())
                     || categoryLayer.getId().equals(schema.getEpisodeLayerId())) {
            throw new InvalidConfigurationException(
              this, category + " layer ("+layerId
              +") must be a non-system span layer, but parent layer is "
              + categoryLayer.getParent());
          }      
        } // a value is specified
      } // this is a category layer parameter
    } // next parameter
  }
  
  /**
   * Determines which layers the annotator requires in order to annotate a graph.
   * @return A list of layer IDs. In this case, the annotator requires no input layers
   * because the input is the transcript media, so this method returns an empty array.
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public String[] getRequiredLayers() throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");
    HashSet<String> requiredLayers = new HashSet<String>();
    return requiredLayers.toArray(new String[0]);
  }
  
  /**
   * Determines which layers the annotator will create/update/delete annotations on.
   * @return A list of layer IDs. In this case, the annotator only outputs {@link #f0LayerId}
   * if it is specified.
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public String[] getOutputLayers() throws InvalidConfigurationException {
    Vector<String> layerIds = new Vector<String>(
      blendshapeLayerIds.values().stream()
      .filter(Objects::nonNull)
      .filter(id->id.length() > 0)
      .collect(Collectors.toList()));
    // layer for results?
    if (resultLayerId != null && resultLayerId.length() > 0) {
      layerIds.add(resultLayerId);
    }    
    // layer for annotated frame images?
    if (annotatedImageLayerId != null && annotatedImageLayerId.length() > 0) {
      layerIds.add(annotatedImageLayerId);
    }    
    // layer for frame count?
    if (frameCountLayerId != null && frameCountLayerId.length() > 0) {
      layerIds.add(frameCountLayerId);
    }    
    return layerIds.toArray(String[]::new);
  }

  // could be multiple scripts executing simultaneously, if they cancel, we need to kill them
  HashSet<Execution> python = new HashSet<Execution>();
  
  /**
   * If reaper is running, kills the process.
   */
  public void cancel() {
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
   * Transforms the graph. In this case, the graph is simply summarized, by counting all
   * tokens of each word type, and printing out the result to stdout.
   * @param transcript The graph to transform.
   * @return The changes introduced by the transformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph transform(Graph transcript) throws TransformationException {
    setRunning(true);
    setPercentComplete(1);
    try {
      // this annotator only works on full graphs, not fragments
      if (transcript.isFragment()) throw new TransformationException(
        this, "mediapipe can currently only be run on full graphs, not graph fragments.");

      // is there a video to analyse?
      File video = null;
      for (MediaFile media : transcript.getMediaProvider().getAvailableMedia()) {
        if (media.getMimeType().startsWith("video/")
            && Optional.ofNullable(media.getTrackSuffix()).orElse("").equals(inputTrackSuffix)
            && media.getFile() != null
            && media.getFile().exists()) {
          video = media.getFile();
          break;
        } // if it's a video on the right track
      } // next media file
      
      if (video == null) {
        setStatus("There is no video on track \""+inputTrackSuffix+"\" for " + transcript.getId());
      } else { // found video
        setStatus("Deleting existing annotations...");
        boolean thereWereAnnotationsDeleted = false;
        for (String layerId : getOutputLayers()) {
          boolean thereWereAnnotationsDeletedOnThisLayer
            = transcript.destroyAll(layerId);
          if (getStore() != null && thereWereAnnotationsDeletedOnThisLayer) {
            // much quicker to use store.deleteMatchingAnnotations...
            getStore().deleteMatchingAnnotations(
              "layer.id == '"+QL.Esc(layerId)+"' && graph.id == '"+QL.Esc(transcript.getId())+"'");
          }
          thereWereAnnotationsDeleted = thereWereAnnotationsDeleted
            || thereWereAnnotationsDeletedOnThisLayer;
        } // next output layer
        if (thereWereAnnotationsDeleted && getStore() != null) {
          // we deleted annotations in the store so we can get rid of them locally too
          transcript.commit();
        }
        
        setStatus("Processing " + video.getName());

        // create an ID for the results (so that other graphs can be processed in parallel)
        String id = IO.WithoutExtension(transcript.getId())
          .replace("'","_"); // ensure that apostrophes won't spoil command line quoting

        setPercentComplete(1);
        String scriptName = "blendshapes-"+getVersion()+".py";
        String csvName = id + ".csv";
        File csv = new File(getWorkingDirectory(), csvName);
        String jsonPattern = resultLayerId.length()==0?"NA":id + "__{0}.json";
        if (!keepGeneratedMedia) csv.deleteOnExit();
        String webmName = outputTrackSuffix.length()==0?"NA":id + outputTrackSuffix + ".webm";
        File webm = new File(getWorkingDirectory(), webmName);
        if (!keepGeneratedMedia) webm.deleteOnExit();
        String pngPattern = annotatedImageLayerId.length()==0?"NA":id + "__{0}.png";
        setPercentComplete(1);
        boolean finishedOk = false;
        try {
          Execution cmd = executeInEnvironment(
            "./"+scriptName
            // if there are apostrophes in the video path, make sure they get through ok
            +" '"+video.getPath().replace("'","'\"'\"'")+"'"
            +" "+numFaces
            +" "+minFaceDetectionConfidence
            +" "+minFacePresenceConfidence
            +" "+minTrackingConfidence
            +" '"+csvName+"'"
            +" '"+jsonPattern+"'"
            +" '"+webmName+"'"
            +" '"+pngPattern+"'"
            +" "+getPaintTesselation()
            +" "+getPaintContours()
            +" "+getPaintIrises());
          if (cmd.getProcess().exitValue() > 0 && !isCancelling()) {
            setStatus("Could not execute "+scriptName+" - status: " + cmd.getProcess().exitValue());
            throw new TransformationException(
              this, "Could not execute "+scriptName+": " + cmd.getProcess().exitValue());
          }

          if (!isCancelling()) {
            setPercentComplete(50);
            setStatus(scriptName + " complete.");
            if (!csv.exists()) {
              setStatus("No scores output by "+scriptName+".");
              throw new TransformationException(this, "No scores output by "+scriptName+".");
            }
            boolean thereWereFaces = false;
            // which scores are we after?
            TreeMap<String,String> categoryLayers = new TreeMap<String,String>();
            for (String category : blendshapeLayerIds.keySet()) {
              String layerId = blendshapeLayerIds.get(category);
              if (layerId != null) categoryLayers.put(category, layerId);
            } // next possible category
            
            MessageFormat jsonResultFilePattern = null;
            if (!jsonPattern.equals("NA")) {
              jsonResultFilePattern = new MessageFormat(jsonPattern);
            }
            MessageFormat annotatedImageFilePattern = null;
            if (!pngPattern.equals("NA")) {
              annotatedImageFilePattern = new MessageFormat(pngPattern);
            }
            String transcriptPrefix = IO.WithoutExtension(transcript.getId());
            
            // read scores
            setStatus("Parsing blendshape data...");
            CSVParser parser = new CSVParser(
              new FileReader(csv), CSVFormat.RFC4180.withFirstRecordAsHeader());
            int r = 0;
            for (CSVRecord record : parser) {
              if (isCancelling()) break;
              thereWereFaces = true;
              String offset = record.get("offset");
              Anchor anchor = transcript.getOrCreateAnchorAt​(Double.parseDouble(offset));
              anchor.setConfidence(Constants.CONFIDENCE_MANUAL); // not unsure about the time
              for (String category : categoryLayers.keySet()) {
                String layerId = blendshapeLayerIds.get(category);
                String label = record.get(category);
                transcript.createAnnotation​(
                  anchor, anchor, layerId, label, transcript)
                  .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
              } // next possible category
              
              if (jsonResultFilePattern != null) {
                // there should be an image file for this frame
                File json = new File(
                  getWorkingDirectory(), jsonResultFilePattern.format(
                    new Object[]{ record.get("frame") }));
                if (!json.exists()) {
                  setStatus("Frame result missing: " + json.getName());
                } else {
                  Annotation blobAnnotation = transcript.createAnnotation​(
                    anchor, anchor, resultLayerId,
                    record.get("frame"), transcript);
                  blobAnnotation.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
                  if (!keepGeneratedMedia) json.deleteOnExit();
                  blobAnnotation.put("dataUrl", json.toURI().toString());
                }
              }
              
              if (annotatedImageFilePattern != null) {
                // there should be an image file for this frame
                File png = new File(
                  getWorkingDirectory(), annotatedImageFilePattern.format(
                    new Object[]{ record.get("frame") }));
                if (!png.exists()) {
                  setStatus("Frame image missing: " + png.getName());
                } else {
                  Annotation blobAnnotation = transcript.createAnnotation​(
                    anchor, anchor, annotatedImageLayerId,
                    record.get("frame"), transcript);
                  blobAnnotation.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
                  if (!keepGeneratedMedia) png.deleteOnExit();
                  blobAnnotation.put("dataUrl", png.toURI().toString());
                }
              }
              r++;
            } // next record
            if (frameCountLayerId.length() > 0) {
              transcript.createTag​(
                transcript, frameCountLayerId, ""+r)
                // high confidence because it's a direct count, not an inference
                .setConfidence(Constants.CONFIDENCE_MANUAL); 
            }
            
            if (!webmName.equals("NA")) {
              if (!webm.exists()) {
                if (thereWereFaces) {
                  setStatus("No annotated video generated by "+scriptName+".");
                  throw new TransformationException(
                    this, "No annotated video generated by "+scriptName+".");
                } else {
                  setStatus(
                    "No annotated video generated by "+scriptName+" - there were no faces found.");
                }
              } else { // webm exists
                if (getStore() == null) {
                  setStatus(
                    "Annotated video generated by "+scriptName+" but no graph store to store it in.");
                } else {
                  setStatus("Saving annotated video...");
                  getStore().saveMedia(transcript.getId(), webm.toURI().toString(), outputTrackSuffix);
                }
              }
            } // webmName set
            finishedOk = !isCancelling();
          } // not cancelling
        } finally {
          if (!keepGeneratedMedia) {
            if (csv.exists()) csv.delete();
            if (webm.exists()) webm.delete();
            // leave pngs/json where they are - they'll probably be moved during graph saving,
            // and if not, they're marked for deletion anyway.
            if (!finishedOk) { // except if we're cancelling or there was an exception
              // in which case, delete the pngs
              File[] frames = getWorkingDirectory().listFiles(
                f->f.getName().startsWith(id + "__")
                && (f.getName().endsWith(".png") || f.getName().endsWith(".json")));
              if (frames != null) {
                for (File file : frames) file.delete();
              }
            }
          } // not keeping generated media
        }
        
      } // found video        
      
    } catch (TransformationException x) {
      throw x;
    } catch (Exception x) {
      throw new TransformationException(
        this, "Error processing " + transcript.getId(), x);
    } finally {
      setRunning(false);
    }
    if (isCancelling()) {
      setStatus("Cancelled.");
    } else {
      setStatus(transcript.getId() + " complete.");
      setPercentComplete(100);
    }
    return transcript;
  }
  
} // end of class MediaPipeAnnotator
