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
package nzilbb.annotator.reaper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
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
import nzilbb.ag.serialize.GraphDeserializer;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.util.DefaultOffsetGenerator;
import nzilbb.ag.util.Merger;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.formatter.est.TrackDeserializer;
import nzilbb.media.MediaConverter;
import nzilbb.media.MediaThread;
import nzilbb.media.wav.FragmentExtractor;
import nzilbb.media.wav.Resampler;
import nzilbb.util.Execution;
import nzilbb.util.IO;

/**
 * Reaper annotator, which executes
 * <a href="https://github.com/google/REAPER">REAPER</a>)
 * for F0 estimation.
 * <p> The annotator saves the resulting <tt>.f0</tt> and <tt>.pm</tt> files, and also
 * parses the <tt>.f0</tt> file and saves instantaneous f0 estimation annotations
 * throughout the annotation graph.
 * @author Robert Fromont robert@fromont.net.nz
 */
@UsesFileSystem @UsesGraphStore
public class ReaperAnnotator extends Annotator {
  /** Get the minimum version of the nzilbb.ag API supported by the annotator.*/
  public String getMinimumApiVersion() { return "1.0.7"; }

  /**
   * F0 Frame Interval - the -e command line option, specifies the output frame interval
   * for F0 (default .005 s). 
   * @see #getF0FrameInterval()
   * @see #setF0FrameInterval(Double)
   */
  protected Double f0FrameInterval;
  /**
   * Getter for {@link #f0FrameInterval}: F0 Frame Interval - the -e command line option,
   * specifies the output frame interval for F0 (default .005 s). 
   * @return F0 Frame Interval - the -e command line option, specifies the output frame
   * interval for F0 (default .005 s). 
   */
  public Double getF0FrameInterval() { return f0FrameInterval; }
  /**
   * Setter for {@link #f0FrameInterval}: F0 Frame Interval - the -e command line option,
   * specifies the output frame interval for F0 (default .005 s). 
   * @param newF0FrameInterval F0 Frame Interval - the -e command line option, specifies
   * the output frame interval for F0 (default .005 s). 
   */
  public ReaperAnnotator setF0FrameInterval(Double newF0FrameInterval) { f0FrameInterval = newF0FrameInterval; return this; }

  /**
   * Minimum F0 - the -m command line option, minimum f0 to look for
   * @see #getMinF0()
   * @see #setMinF0(Integer)
   */
  protected Integer minF0;
  /**
   * Getter for {@link #minF0}: Minimum F0 - the -m command line option, minimum f0 to look for
   * @return Minimum F0 - the -m command line option, minimum f0 to look for
   */
  public Integer getMinF0() { return minF0; }
  /**
   * Setter for {@link #minF0}: Minimum F0 - the -m command line option, minimum f0 to look for
   * @param newMinF0 Minimum F0 - the -m command line option, minimum f0 to look for
   */
  public ReaperAnnotator setMinF0(Integer newMinF0) { minF0 = newMinF0; return this; }

  /**
   * Maximum F0 - the -x command line option, maximum f0 to look for
   * @see #getMaxF0()
   * @see #setMaxF0(Integer)
   */
  protected Integer maxF0;
  /**
   * Getter for {@link #maxF0}: Maximum F0 - the -x command line option, maximum f0 to look for
   * @return Maximum F0 - the -x command line option, maximum f0 to look for
   */
  public Integer getMaxF0() { return maxF0; }
  /**
   * Setter for {@link #maxF0}: Maximum F0 - the -x command line option, maximum f0 to look for
   * @param newMaxF0 Maximum F0 - the -x command line option, maximum f0 to look for
   */
  public ReaperAnnotator setMaxF0(Integer newMaxF0) { maxF0 = newMaxF0; return this; }

  /**
   * Hilbert Transform - the -t command line option, enables a Hilbert transform that may
   * reduce phase distortion. 
   * @see #getHilbertTransform()
   * @see #setHilbertTransform(boolean)
   */
  protected boolean hilbertTransform = false;
  /**
   * Getter for {@link #hilbertTransform}: Hilbert Transform - the -t command line option,
   * enables a Hilbert transform that may reduce phase distortion. 
   * @return Hilbert Transform - the -t command line option, enables a Hilbert transform
   * that may reduce phase distortion. 
   */
  public boolean getHilbertTransform() { return hilbertTransform; }
  /**
   * Setter for {@link #hilbertTransform}: Hilbert Transform - the -t command line option,
   * enables a Hilbert transform that may reduce phase distortion. 
   * @param newHilbertTransform Hilbert Transform - the -t command line option, enables a
   * Hilbert transform that may reduce phase distortion. 
   */
  public ReaperAnnotator setHilbertTransform(boolean newHilbertTransform) { hilbertTransform = newHilbertTransform; return this; }

  /**
   * Suppress High-pass filter - the -s command line option, suppress applying high pass
   * filter at 80Hz (rumble-removal highpass filter) 
   * @see #getSuppressHighPassFilter()
   * @see #setSuppressHighPassFilter(boolean)
   */
  protected boolean suppressHighPassFilter = false;
  /**
   * Getter for {@link #suppressHighPassFilter}: Suppress High-pass filter - the -s
   * command line option, suppress applying high pass filter at 80Hz (rumble-removal
   * highpass filter) 
   * @return Suppress High-pass filter - the -s command line option, suppress applying
   * high pass filter at 80Hz (rumble-removal highpass filter) 
   */
  public boolean getSuppressHighPassFilter() { return suppressHighPassFilter; }
  /**
   * Setter for {@link #suppressHighPassFilter}: Suppress High-pass filter - the -s
   * command line option, suppress applying high pass filter at 80Hz (rumble-removal
   * highpass filter) 
   * @param newSuppressHighPassFilter Suppress High-pass filter - the -s command line
   * option, suppress applying high pass filter at 80Hz (rumble-removal highpass filter) 
   */
  public ReaperAnnotator setSuppressHighPassFilter(boolean newSuppressHighPassFilter) { suppressHighPassFilter = newSuppressHighPassFilter; return this; }
  
  /**
   * The ID of the layer to save f0 measurement annotations to, or null to not add annotations.
   * @see #getF0LayerId()
   * @see #setF0LayerId(String)
   */
  protected String f0LayerId;
  /**
   * Getter for {@link #f0LayerId}: The ID of the layer to save f0 measurement annotations
   * to, or null to not add annotations. 
   * @return The ID of the layer to save f0 measurement annotations to, or null to not add
   * annotations. 
   */
  public String getF0LayerId() { return f0LayerId; }
  /**
   * Setter for {@link #f0LayerId}: The ID of the layer to save f0 measurement annotations
   * to, or null to not add annotations. 
   * @param newF0LayerId The ID of the layer to save f0 measurement annotations to, or
   * null to not add annotations. 
   */
  public ReaperAnnotator setF0LayerId(String newF0LayerId) { f0LayerId = newF0LayerId; return this; }
  
  /**
   * Default constructor.
   */
  public ReaperAnnotator() {
    setSchema( // This is the kind of schema we'd like (set here for testing purposes):
      new Schema(
        "who", "turn", "utterance", "word",
        new Layer("f0", "Pitch")
        .setAlignment(Constants.ALIGNMENT_INSTANT)
        .setPeers(true).setPeersOverlap(false).setSaturated(false)));
  } // end of constructor
  
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

    if (parameters != null) {
      beanPropertiesFromQueryString(parameters);
    }
    
    // validation...

    if (f0LayerId != null && f0LayerId.trim().length() == 0) f0LayerId = null;
    
    if (f0LayerId != null) {
      Layer f0Layer = schema.getLayer(f0LayerId);
      if (f0Layer == null) {
        schema.addLayer(
          new Layer(f0LayerId)
          .setAlignment(Constants.ALIGNMENT_INSTANT)
          .setPeers(true).setPeersOverlap(false).setSaturated(false)
          .setParentId(schema.getRoot().getId())
          .setType(Constants.TYPE_NUMBER)
          .setDescription("Pitch"));
      } else if (f0Layer.getParent() == null
                 || !f0Layer.getParent().getId().equals(schema.getRoot().getId())) {
        throw new InvalidConfigurationException(
          this, "f0 layer must be a span layer, but parent layer is " + f0Layer.getParent());
      }
    } // f0LayerId != null
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
    HashSet<String> outputLayers = new HashSet<String>();
    if (f0LayerId != null) outputLayers.add(f0LayerId);
    return outputLayers.toArray(new String[0]);
  }

  Execution reaper = null;
  
  /**
   * If reaper is running, kills the process.
   */
  public void cancel() {
    if (reaper != null && reaper.getProcess() != null) {
      if (!isCancelling()) { // first time
        reaper.getProcess().destroy();
      } else { // multiple cancel requests, kill -9
        reaper.getProcess().destroyForcibly();
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
        this, "Reaper can only be run on full graphs, not graph fragments.");

      if (f0LayerId != null) {
        Annotation[] annotations = transcript.all(f0LayerId);
        if (annotations.length > 0) {          
          setStatus(
            "Delete existing "+annotations.length+" on "+f0LayerId+" for " + transcript.getId());
          for (Annotation f0 : annotations) {
            f0.destroy();
          }
        } // there are existing annotations to delete
      } // f0LayerId != null

      // is the .f0 file already there?
      File f0File = null;
      for (MediaFile media : transcript.getMediaProvider().getAvailableMedia()) {
        if (media.getExtension().equals("f0")
            && media.getFile() != null
            && media.getFile().exists()) {
          f0File = media.getFile();
          break;
        } // if f0 file exists
      } // next media file

      if (f0File != null) {
        setStatus("Reaper has already been run for " + transcript.getId());
      } else { // no f0 file, so run reaper to generate it
        
        String originalWavURL = transcript.getMediaProvider().getMedia(null, "audio/wav");
        if (originalWavURL == null) {
          setStatus(transcript.getId() + " has no wav file and will be ignored");
        } else { // has wav
          File originalWav = new File(new URI(originalWavURL));
          setStatus("originalWav " + originalWav.getPath());
          
          String transcriptName = IO.WithoutExtension(transcript.getId());
          File downsampledWav = new File(
            getWorkingDirectory(), transcriptName + "_mono16kHz.wav");
          File f0 = new File(
            getWorkingDirectory(), transcriptName + ".f0");
          File pm = new File(
            getWorkingDirectory(), transcriptName + ".pm");
          try {
            
            // downsample to mono 16kHz
            ParameterSet configuration = new ParameterSet();
            configuration.addParameter(new Parameter("sampleRate", 16000));
            MediaConverter resampler = new Resampler();
            resampler.configure(configuration);
            setStatus("Downsampling to " + downsampledWav.getPath());
            MediaThread thread = resampler.start(
              "audio/wav", originalWav, "audio/wav", downsampledWav);
            // wait for resampler to finish
            thread.join();
            if (thread.getLastError() != null) {
              throw new TransformationException(
                this, "Could not resample " + originalWav.getName(), thread.getLastError());
            }

            // execute reaper
            String osName = java.lang.System.getProperty("os.name");
	    if (osName.startsWith("Windows")) osName = "Windows";
	    String exeName = getExeName();
	    File reaperExe = new File(getWorkingDirectory(), exeName);
            setStatus("Reaper " + reaperExe.getAbsolutePath());
            if (!reaperExe.exists()) {
              try {
                // unpack reaper from our own jar file
                URL url = getClass().getResource(exeName);
                setStatus("Extracting " + url + " to " + reaperExe.getAbsolutePath());
                IO.Pump(url.openStream(), new FileOutputStream(reaperExe));

                // try to mark it as executable
                setStatus("Marking as executable: " + reaperExe.getAbsolutePath());
                Execution chmod = new Execution().setExe("chmod")
                  .arg("u+x").arg(reaperExe.getAbsolutePath());
                chmod.run();
                String output = (chmod.stderr()+"\n"+chmod.stdout()).trim();
                if (output.length() > 0) setStatus("chmod: " + output);
              } catch(Exception exception) {
                setStatus("Could not extract " + reaperExe.getPath() + " : " + exception);
                throw new TransformationException(
                  this, "Could not extract " + reaperExe.getPath(), exception);
              }
            }
            if (!reaperExe.exists()) {
              throw new TransformationException(
                this, "Reaper not found: " + reaperExe.getPath());
            }
            reaper = new Execution()
              .setWorkingDirectory(getWorkingDirectory())
              .setExe(reaperExe)
              .arg("-i").arg(downsampledWav.getPath())
              .arg("-f").arg(f0.getPath())
              .arg("-p").arg(pm.getPath())
              .arg("-a"); // ASCII output files
            if (minF0 != null)           reaper.arg("-m").arg(minF0.toString());
            if (maxF0 != null)           reaper.arg("-x").arg(maxF0.toString());
            if (f0FrameInterval != null) reaper.arg("-e").arg(f0FrameInterval.toString());
            if (hilbertTransform)        reaper.arg("-t");
            if (suppressHighPassFilter)  reaper.arg("-s");

            reaper.getStderrObservers().add(err -> setStatus(err));
            reaper.getStdoutObservers().add(out -> setStatus(out));

            setStatus("Environment:");
            Map<String, String> env = System.getenv();
            for (String key : env.keySet()) {
              setStatus(key+"="+env.get(key));
            }
            setStatus("System properties:");
            Properties systemProperties = System.getProperties();
            Enumeration enuProp = systemProperties.propertyNames();
            while (enuProp.hasMoreElements()) {
              String propertyName = (String) enuProp.nextElement();
              String propertyValue = systemProperties.getProperty(propertyName);
              setStatus(propertyName + ": " + propertyValue);
            }
            setStatus(transcript.getId() + " : Running reaper...");
            reaper.run();

            if (reaper.stderr().trim().length() > 0) {
              setStatus("Reaper execution error: " + reaper.stderr());
              throw new TransformationException(this, reaper.stderr());
            } else if (reaper.getProcess() == null) {
              setStatus("Reaper could not start.");
              throw new TransformationException(this, "Reaper could not start");
            } else {
              setStatus("Reaper returned " + reaper.getProcess().exitValue());
            }
            
            if (f0.exists()) {              
              setStatus("Saving " + f0.toURI().toString());
              getStore().saveMedia(transcript.getId(), "", f0.toURI().toString());
              // remove temporary file
              f0.delete();

              // get back the file URL
              for (MediaFile media : transcript.getMediaProvider().getAvailableMedia()) {
                setStatus("File: " + media.getName());
                if (media.getExtension().equals("f0")
                    && media.getFile() != null
                    && media.getFile().exists()) {
                  f0File = media.getFile();
                  break;
                } // if f0 file exists
              } // next media file
            } // .f0
            if (pm.exists()) {
              setStatus("Saving " + pm.getName());
              getStore().saveMedia(transcript.getId(), "", pm.toURI().toString());
              // remove temporary file
              pm.delete();
            } // .pm
            
          } finally {
            downsampledWav.delete();
          }
        }
      } // has wav

      setStatus("f0File: " + f0File);

      if (!isCancelling()
          && f0LayerId != null && f0File != null) { // parse .f0 file to generate annotations
        setStatus("Loading annotations from " + f0File.getName());
        
        // create deserializer
        GraphDeserializer deserializer = new TrackDeserializer();

        // general configuration
        Schema schema = getSchema();
        ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
        deserializer.configure(configuration, schema);
        
        // load the stream
        NamedStream[] streams = { new NamedStream(f0File) };
        ParameterSet defaultParamaters = deserializer.load(streams, schema);
        Layer layer = schema.getLayer(f0LayerId);
        defaultParamaters.get("labels").setValue(layer);
        deserializer.setParameters(defaultParamaters);
        
        // build the graph from the stream
        Graph[] graphs = deserializer.deserialize();
        for (String warning : deserializer.getWarnings()) setStatus(warning);
        Graph reaperGraph = graphs[0];
        
        reaperGraph.setId(transcript.getId());
        reaperGraph.trackChanges();
        Annotation[] f0Annotations = reaperGraph.all(f0LayerId);
        for (Annotation a : f0Annotations) {
          Anchor time = transcript.getOrCreateAnchorAt(
            a.getStart().getOffset(), Constants.CONFIDENCE_AUTOMATIC);          
          transcript.createAnnotation(time, time, f0LayerId, a.getLabel(), transcript);
        } // next f0 annotation
        setStatus("Added "+f0Annotations.length+" annotations to " + transcript.getId());
        transcript.put("@valid", Boolean.TRUE); // TODO remove this workaround
      } // parse .f0 file to generate annotations

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
  
  /**
   * Compute the name of the Reaper excutable program name, which is operating system specific.
   * @return The name of the Reaper excutable program file.
   */
  public String getExeName() {
    String osName = System.getProperty("os.name");
    if (osName.startsWith("Windows")) osName = "Windows";
    return "reaper_" + osName + System.getProperty("os.arch")
      + (osName.equals("Windows")?".exe":""); 
  } // end of getExeName()
  
} // end of class ReaperAnnotator
