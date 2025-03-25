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
import nzilbb.ag.util.Merger;
import nzilbb.util.Execution;
import nzilbb.util.IO;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

/**
 * Mediapipe annotator integrates with 
 * <a href="https://github.com/google-ai-edge/mediapipe">mediapipe</a>
 * for processing of video to extract face landmark data.
 * <p> The annotator saves an annotated <tt>.mp4</tt> file, and also saves instantaneous
 * <a href="https://ai.google.dev/edge/mediapipe/solutions/vision/face_landmarker">blendshape</a>
 * score annotations, i.e. facial features that can be used to determine facial expression.
 * @author Robert Fromont robert@fromont.net.nz
 */
@UsesFileSystem @UsesGraphStore
public class MediaPipeAnnotator extends Annotator {
  /** Get the minimum version of the nzilbb.ag API supported by the annotator.*/
  public String getMinimumApiVersion() { return "1.0.7"; }
  
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
   * Default constructor.
   */
  public MediaPipeAnnotator() {
     // This is the kind of schema we'd like (set here for testing purposes):
    Schema schema = new Schema("who", "turn", "utterance", "word");
    for (String category : blendshapeCategories) {
      schema.addLayer(
        new Layer(category, category + " score")
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

      // extract scripts
      String script = "blendshapes.py";
      setStatus("Unpacking " + script);
      URL urlSource = getClass().getResource(script);
      setStatus("urlSource " + urlSource);
      File destination = new File(getWorkingDirectory(), script);
      if (!destination.exists()) {
        IO.SaveInputStreamToFile​(urlSource.openStream(), destination);
        new Execution()
          .setExe("chmod")
          .arg("u+x").arg(destination.getPath())
          .setWorkingDirectory(getWorkingDirectory())
          .run();
      }
      
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
        setStatus("Cannot run 'venv' module - status: " + cmd.getProcess().exitValue());
        throw new InvalidConfigurationException(
          this, "Cannot run 'venv' module; Please install support for python environments."
          +"\ne.g. on Ubuntu: apt install python3.10-venv");
      }
      setStatus(cmd.getInput().toString());
      
      // install mediapipe 
      cmd = executeInEnvironment("pip install mediapipe");
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
      Execution chmod = new Execution()
        .setExe("chmod")
        .arg("u+x").arg(environmentName+"/bin/activate")
        .setWorkingDirectory(getWorkingDirectory());        
      chmod.run();
      chmod = new Execution()
        .setExe("chmod")
        .arg("u+x").arg(script.getPath())
        .setWorkingDirectory(getWorkingDirectory());
      chmod.run();
      Execution cmd = new Execution()
        .setExe("bash") // TODO windows interpreter
        .arg(script.getPath())
        .setWorkingDirectory(getWorkingDirectory());
      cmd.getStdoutObservers().add(m->setStatus(m));
      cmd.getStderrObservers().add(m->setStatus(m));
      cmd.run();
      return cmd;
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

    if (parameters != null) {
      beanPropertiesFromQueryString(parameters);
    }

    // parse/validate category score layer ID parameters
    
    // clear any previous blendshape layer IDs
    for (String category : blendshapeLayerIds.keySet()) blendshapeLayerIds.put(category, null);
    
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
            schema.addLayer(
              new Layer(layerId)
              .setAlignment(Constants.ALIGNMENT_INSTANT)
              .setPeers(true).setPeersOverlap(false).setSaturated(false)
              .setParentId(schema.getRoot().getId())
              .setType(Constants.TYPE_NUMBER)
              .setDescription(category + " score"));
          } else if (categoryLayer.getParent() == null
                     || !categoryLayer.getParent().getId().equals(schema.getRoot().getId())) {
            throw new InvalidConfigurationException(
              this, category + " layer ("+layerId+") must be a span layer, but parent layer is "
              + categoryLayer.getParent());
          }      
        } // a value is specified
      } // this is a category layer parameter
    } // next parameter

    // TODO a layer for annotated frame images
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
    // TODO a layer for annotated frame images
    return blendshapeLayerIds.values().stream()
      .filter(Objects::nonNull)
      .filter(id->id.length() > 0)
      .toArray(String[]::new);
  }

  Execution python = null;
  
  /**
   * If reaper is running, kills the process.
   */
  public void cancel() {
    if (python != null && python.getProcess() != null) {
      if (!isCancelling()) { // first time
        python.getProcess().destroy();
      } else { // multiple cancel requests, kill -9
        python.getProcess().destroyForcibly();
      }
    }
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
    try {
      // this annotator only works on full graphs, not fragments
      if (transcript.isFragment()) throw new TransformationException(
        this, "mediapipe can currently only be run on full graphs, not graph fragments.");

      // is there a video to analyse?
      File video = null;
      for (MediaFile media : transcript.getMediaProvider().getAvailableMedia()) {
        if (media.getMimeType().startsWith("video/")
            && media.getFile() != null
            && media.getFile().exists()) {
          video = media.getFile();
          break;
        } // if f0 file exists
      } // next media file
      
      if (video == null) {
        setStatus("There is no video for " + transcript.getId());
      } else { // found video
        setStatus("Processing " + video.getName());

        // create an ID for the results (so that other graphs can be processed in parallel)
        String id = IO.WithoutExtension(transcript.getId());

        Execution cmd = executeInEnvironment(
          "./blendshapes.py '"+video.getPath()+"' '"+id+"' "
          +numFaces+" "
          +minFaceDetectionConfidence+" "
          +minFacePresenceConfidence+" "
          +minTrackingConfidence);
        if (cmd.getProcess().exitValue() > 0) {
          setStatus("Could not execute blendshapes.py - status: " + cmd.getProcess().exitValue());
          throw new TransformationException(
            this, "Could not execute blendshapes.py: " + cmd.getProcess().exitValue());
        }
        
        File csv = new File(getWorkingDirectory(), id+".csv");
        if (!csv.exists()) {
          setStatus("No scores output by blendshapes.py.");
          throw new TransformationException(this, "No scores output by blendshapes.py.");
        }
        try {
          // which scores are we after?
          TreeMap<String,String> categoryLayers = new TreeMap<String,String>();
          for (String category : blendshapeLayerIds.keySet()) {
            String layerId = blendshapeLayerIds.get(category);
            if (layerId != null) categoryLayers.put(category, layerId);
          } // next possible category        
          
          // read scores
          CSVParser parser = new CSVParser(
            new FileReader(csv), CSVFormat.RFC4180.withFirstRecordAsHeader());
          for (CSVRecord record : parser) {
            String offset = record.get("offset");
            Anchor anchor = transcript.getOrCreateAnchorAt​(Double.parseDouble(offset));
            for (String category : categoryLayers.keySet()) {
              String layerId = blendshapeLayerIds.get(category);
              String label = record.get(category);
              transcript.createAnnotation​(anchor, anchor, layerId, label, transcript);
            } // next possible category
          } // next record
        } finally {
          csv.delete();
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
    }
    return transcript;
  }
  
} // end of class MediaPipeAnnotator
