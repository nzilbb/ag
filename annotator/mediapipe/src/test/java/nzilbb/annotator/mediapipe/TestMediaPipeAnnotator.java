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
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;
import nzilbb.ag.Anchor;
import nzilbb.ag.Annotation;
import nzilbb.ag.Change;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.GraphMediaProvider;
import nzilbb.ag.Layer;
import nzilbb.ag.MediaFile;
import nzilbb.ag.MediaTrackDefinition;
import nzilbb.ag.PermissionException;
import nzilbb.ag.Schema;
import nzilbb.ag.StoreException;
import nzilbb.ag.automation.Dictionary;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.UsesRelationalDatabase;
import nzilbb.sql.derby.DerbyConnectionFactory;
import nzilbb.util.Execution;
import nzilbb.util.IO;

/**
 * Tests for the mediapipe integration annotator.
 * <p> Before these tests can work, Python 3 must be installed on the test machine:
 */
public class TestMediaPipeAnnotator {

  static MediaPipeAnnotator annotator = new MediaPipeAnnotator();

  /** Set up initial MFA configuration */
  @BeforeClass
  public static void config() throws Exception {
    
    System.out.println("Verify MediaPipe configuration...");
    
    // find the current directory
    File dir = dir();
    
    // set the schema
    annotator.setSchema(graph().getSchema());
    
    // set the working directory
    annotator.setWorkingDirectory(dir);
    
    // set the graph store
    annotator.setStore(new GraphStoreHarness());
    
    // if (annotator.getStatusObservers().size() == 0) {
    //   annotator.getStatusObservers().add(status->System.out.println(status));
    // }
    // set the annotator configuration
    annotator.setConfig("");

    // to create annotations within the graph instead of using graph store
    annotator.createWithStore = false;
    
    System.out.println("OK.");
  }

  /** Infer the directory of the tests. */
  public static File dir() throws Exception { 
    URL urlThisClass = TestMediaPipeAnnotator.class.getResource(
      TestMediaPipeAnnotator.class.getSimpleName() + ".class");
    File fThisClass = new File(urlThisClass.toURI());
    return fThisClass.getParentFile();
  }

  /** Ensure Python integration and medipipe installation works. */
  @Test public void installation() throws Exception {
    // if (annotator.getStatusObservers().size() == 0) {
    //   annotator.getStatusObservers().add(status->System.out.println(status));
    // }

    assertEquals("mp_env", annotator.getEnvironmentName());
    Execution cmd = annotator.executeInEnvironment("python3 --version");
    assertEquals("No stderr", "", cmd.stderr());
    assertTrue("Version info returned", cmd.stdout().startsWith("Python 3"));

    assertTrue("task file has been downloaded",
               new File(annotator.getWorkingDirectory(), "face_landmarker.task").exists());
    assertTrue("script file has been unpacked",
               new File(annotator.getWorkingDirectory(),
                        "blendshapes-"+annotator.getVersion()+".py").exists());
  }   

  /** Ensure invalid task parameters raise errors. */
  @Test public void invalidTaskParameters() throws Exception {
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    try {
      annotator.setTaskParameters(
        "jawForward=word"); // non-top-level layer
      fail("Non-top level layer is invalid for blendshape scores");
    } catch(InvalidConfigurationException exception) {
    }
    try {
      annotator.setTaskParameters(
        "jawForward="+schema.getParticipantLayerId()); // top-level layer but a system layer
      fail("System layer is invalid for blendshape scores");
    } catch(InvalidConfigurationException exception) {
    }
    try {
      annotator.setTaskParameters(
        "annotatedImageLayerId=word"); // non-top-level layer
      fail("Non-top level layer is invalid for annotated images");
    } catch(InvalidConfigurationException exception) {
    }
    try {
      annotator.setTaskParameters(
        "annotatedImageLayerId="+schema.getParticipantLayerId()); // top-level layer but a system layer
      fail("System layer is invalid for annotated images");
    } catch(InvalidConfigurationException exception) {
    }
    try {
      annotator.setTaskParameters(
        "annotatedImageLayerId=frame&resultLayerId=frame"); // output layers the same
      fail("annotatedImageLayerId and resultLayerId are the same");
    } catch(InvalidConfigurationException exception) {
    }
  }
    
  /** Ensure valid task parameters don't raise errors, and change the schema when appropriate. */
  @Test public void validTaskParameters() throws Exception {
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    annotator.setTaskParameters(
      "jawForward=jawForward" // partial set of layers
      +"&jawLeft=jawLeft"
      +"&jawOpen=jawOpen"
      +"&jawRight=jawRight"
      +"&mouthClose="         // some layers specified but unset
      +"&mouthDimpleLeft="
      +"&annotatedImageLayerId=frames"
      +"&outputTrackSuffix=-face-landmarks"
      +"&paintTesselation=on"
      +"&paintContours=on"
      +"&paintIrises=on"
      );
    
    assertEquals("outputTrackSuffix", "-face-landmarks", annotator.getOutputTrackSuffix());
    
    assertEquals("annotatedImageLayerId", "frames", annotator.getAnnotatedImageLayerId());
    Layer layer = annotator.getSchema().getLayer("frames");
    assertNotNull("frames layer created", layer);
    assertEquals("frames alignment", Constants.ALIGNMENT_INSTANT, layer.getAlignment());
    assertEquals("frames type", "image/png", layer.getType());
    assertTrue("frames peers", layer.getPeers());
    assertEquals("frames parent", schema.getRoot().getId(), layer.getParentId());

    assertEquals("jawForward layer",
                 "jawForward", annotator.getBlendshapeLayerIds().get("jawForward"));
    layer = annotator.getSchema().getLayer("jawForward");
    assertNotNull("jawForward layer created", layer);
    assertEquals("jawForward alignment", Constants.ALIGNMENT_INSTANT, layer.getAlignment());
    assertEquals("jawForward type", Constants.TYPE_NUMBER, layer.getType());
    assertTrue("jawForward peers", layer.getPeers());
    assertEquals("jawForward parent", schema.getRoot().getId(), layer.getParentId());
    assertEquals("jawForward category copied from frames",
                 "media-frame-category", layer.getCategory());
    assertEquals("jawLeft layer",
                 "jawLeft", annotator.getBlendshapeLayerIds().get("jawLeft"));
    layer = annotator.getSchema().getLayer("jawLeft");
    assertNotNull("jawLeft layer created", layer);
    assertEquals("jawLeft alignment", Constants.ALIGNMENT_INSTANT, layer.getAlignment());
    assertEquals("jawLeft type", Constants.TYPE_NUMBER, layer.getType());
    assertTrue("jawLeft peers", layer.getPeers());
    assertEquals("jawLeft parent", schema.getRoot().getId(), layer.getParentId());
    assertEquals("jawLeft category copied from frames",
                 "media-frame-category", layer.getCategory());
    assertEquals("jawOpen layer",
                 "jawOpen", annotator.getBlendshapeLayerIds().get("jawOpen"));
    layer = annotator.getSchema().getLayer("jawOpen");
    assertNotNull("jawOpen layer created", layer);
    assertEquals("jawOpen alignment", Constants.ALIGNMENT_INSTANT, layer.getAlignment());
    assertEquals("jawOpen type", Constants.TYPE_NUMBER, layer.getType());
    assertTrue("jawOpen peers", layer.getPeers());
    assertEquals("jawOpen parent", schema.getRoot().getId(), layer.getParentId());
    assertEquals("jawOpen category copied from frames",
                 "media-frame-category", layer.getCategory());
    assertEquals("jawForward layer",
                 "jawRight", annotator.getBlendshapeLayerIds().get("jawRight"));
    layer = annotator.getSchema().getLayer("jawRight");
    assertNotNull("jawRight layer created", layer);
    assertEquals("jawRight alignment", Constants.ALIGNMENT_INSTANT, layer.getAlignment());
    assertEquals("jawRight type", Constants.TYPE_NUMBER, layer.getType());
    assertTrue("jawRight peers", layer.getPeers());
    assertEquals("jawRight parent", schema.getRoot().getId(), layer.getParentId());
    assertEquals("jawRight category copied from frames",
                 "media-frame-category", layer.getCategory());
    assertNull("no mouthClose layer id",
                 annotator.getBlendshapeLayerIds().get("mouthClose"));
    assertNull("no mouthClose layer", annotator.getSchema().getLayer("mouthClose"));
    assertNull("no mouthDimpleLeft layer id",
               annotator.getBlendshapeLayerIds().get("mouthDimpleLeft"));
    assertNull("no mouthDimpleLeft layer", annotator.getSchema().getLayer("mouthDimpleLeft"));

    assertTrue("paintTesselation", annotator.getPaintTesselation());
    assertTrue("paintContours", annotator.getPaintContours());
    assertTrue("paintIrises", annotator.getPaintIrises());

    Set<String> requiredLayers = new HashSet<String>(Arrays.asList(annotator.getRequiredLayers()));
    assertEquals("Correct number of required layers: " + requiredLayers,
                 0, requiredLayers.size());

    Set<String> outputLayers = new HashSet<String>(Arrays.asList(annotator.getOutputLayers()));
    assertEquals("Correct number of output layers: " + outputLayers,
                 5, outputLayers.size());
    assertTrue("jawForward output", outputLayers.contains("jawForward"));
    assertTrue("jawLeft output", outputLayers.contains("jawLeft"));
    assertTrue("jawOpen output", outputLayers.contains("jawOpen"));
    assertTrue("jawRight output", outputLayers.contains("jawRight"));
    assertTrue("annotatedImageLayerId output", outputLayers.contains("frames"));

  }

  /** Facial feature score annotations are created. */
  @Test public void featureScoreAnnotations() throws Exception {
    // if (annotator.getStatusObservers().size() == 0) {
    //   annotator.getStatusObservers().add(status->System.out.println(status));
    // }
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    annotator.setTaskParameters(
      "jawForward=jawForwardLayer" // layer names aren't feature names
      +"&jawLeft=jawLeftLayer"
      +"&jawOpen=jawOpenLayer"
      +"&jawRight=jawRightLayer");
    assertEquals("jawForward layer",
                 "jawForwardLayer", annotator.getBlendshapeLayerIds().get("jawForward"));
    assertEquals("jawLeft layer",
                 "jawLeftLayer", annotator.getBlendshapeLayerIds().get("jawLeft"));
    assertEquals("jawOpen layer",
                 "jawOpenLayer", annotator.getBlendshapeLayerIds().get("jawOpen"));
    assertEquals("jawRight layer",
                 "jawRightLayer", annotator.getBlendshapeLayerIds().get("jawRight"));
    assertEquals("outputTrackSuffix not set", "", annotator.getOutputTrackSuffix());
    assertEquals("annotatedImageLayerId not set", "", annotator.getAnnotatedImageLayerId());
    assertFalse("paintTesselation", annotator.getPaintTesselation());
    assertFalse("paintContours", annotator.getPaintContours());
    assertFalse("paintIrises", annotator.getPaintIrises());

    g.trackChanges();
    annotator.transform(g);
        
    Annotation[] scores = g.all("jawForwardLayer");
    assertTrue("There are jawForward scores", scores.length > 0);
    System.out.println("jawForward: " + scores[0].getStart() + ": " + scores[0]);
    assertEquals("Score annotations have medium confidence",
                 50, (int)scores[0].getConfidence());
    assertEquals("Score anchors have high confidence",
                 100, (int)scores[0].getStart().getConfidence());
    System.out.println("jawForward: " + scores[0].getStart() + ": " + scores[0]);
    scores = g.all("jawLeftLayer");
    assertTrue("There are jawLeft scores", scores.length > 0);
    System.out.println("jawLeft: " + scores[0].getStart() + ": " + scores[0]);
    scores = g.all("jawOpenLayer");
    assertTrue("There are jawOpen scores", scores.length > 0);
    System.out.println("jawOpen: " + scores[0].getStart() + ": " + scores[0]);
    scores = g.all("jawRightLayer");
    assertTrue("There are jawRight scores", scores.length > 0);
    System.out.println("jawRight: " + scores[0].getStart() + ": " + scores[0]);

    assertNull("no annotated video generated",
               ((GraphStoreHarness)annotator.getStore()).id);
  }
  
  /** Annotated video is created. */
  @Test public void annotatedVideo() throws Exception {
    // if (annotator.getStatusObservers().size() == 0) {
    //   annotator.getStatusObservers().add(status->System.out.println(status));
    // }
    // annotator.keepGeneratedMedia = true; 
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);

    // test the annotator knows about media track definitions
    List<MediaTrackDefinition> tracks = annotator.getMediaTracks();
    assertNotNull("media tracks returned", tracks);
    assertEquals("right number of tracks returned", 3, tracks.size());
    assertEquals("first track correct", "", tracks.get(0).getSuffix());
    assertEquals("second track correct", "-B", tracks.get(1).getSuffix());
    assertEquals("third track correct", "-face-landmarks", tracks.get(2).getSuffix());

    String outputTrackSuffix = "-facial-features";
    
    annotator.setTaskParameters(
      "outputTrackSuffix="+outputTrackSuffix
      +"&paintTesselation=on"
      );
    assertTrue("paintTesselation", annotator.getPaintTesselation());
    assertFalse("paintContours", annotator.getPaintContours());
    assertFalse("paintIrises", annotator.getPaintIrises());
    g.trackChanges();
    assertEquals("outputTrackSuffix set", outputTrackSuffix, annotator.getOutputTrackSuffix());
    annotator.transform(g);   

    GraphStoreHarness store = (GraphStoreHarness)annotator.getStore();
    assertEquals("annotated video generated", g.getId(), store.id);
    assertEquals("annotated track suffix", outputTrackSuffix, store.trackSuffix);
    assertNotNull("annotated track file", store.file);
    System.out.println(store.file.getPath());
    // tidily delete the file
    if (!annotator.keepGeneratedMedia) {
      store.file.delete();
    }
  }
  
  /** Annotated frame images are created. */
  @Test public void annotatedFrameImages() throws Exception {
    // if (annotator.getStatusObservers().size() == 0) {
    //   annotator.getStatusObservers().add(status->System.out.println(status));
    // }
    // annotator.keepGeneratedMedia = true; 
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    annotator.setTaskParameters(
      "annotatedImageLayerId=frame" // doesn't already exist
      +"&frameCountLayerId=frameCount" // will be prefixed with "transcript_"
      +"&paintContours=true");
    assertEquals("annotatedImageLayerId set", "frame", annotator.getAnnotatedImageLayerId());
    
    Layer layer = annotator.getSchema().getLayer("frame");
    assertNotNull("frame layer created", layer);
    assertEquals("frame alignment", Constants.ALIGNMENT_INSTANT, layer.getAlignment());
    assertEquals("frame type", "image/png", layer.getType());
    assertTrue("frame peers", layer.getPeers());
    assertEquals("frame parent", schema.getRoot().getId(), layer.getParentId());
    assertNull("frame category unset", layer.getCategory());
    
    layer = annotator.getSchema().getLayer("transcript_frameCount");
    assertNotNull("frame count layer created", layer);
    assertEquals("frame count alignment", Constants.ALIGNMENT_NONE, layer.getAlignment());
    assertEquals("frame count type", Constants.TYPE_NUMBER, layer.getType());
    assertFalse("frame count peers", layer.getPeers());
    assertEquals("frame parent", schema.getRoot().getId(), layer.getParentId());
    Set<String> outputLayers = new HashSet<String>(Arrays.asList(annotator.getOutputLayers()));
    assertEquals("Correct number of output layers: " + outputLayers,
                 2, outputLayers.size());
    assertTrue("frame layer output", outputLayers.contains("frame"));
    assertTrue("frame count layer output", outputLayers.contains("transcript_frameCount"));
    assertFalse("paintTesselation", annotator.getPaintTesselation());
    assertTrue("paintContours", annotator.getPaintContours());
    assertFalse("paintIrises", annotator.getPaintIrises());
    
    g.trackChanges();
    annotator.transform(g);   

    Annotation[] frames = g.all("frame");
    assertTrue("There are frame annotations", frames.length > 0);
    // labels are formatted as fragment filenames: {transcript}_{layer}__{offset}.png
    try {
      Integer.parseInt(frames[0].getLabel());
    } catch(NumberFormatException exception) {
      fail("First frame label is numeric (frame number): " + frames[0] + " - " + exception);
    }
    System.out.println("first frame: " + frames[0].getStart() + ": " + frames[0]);
    assertEquals("Frame annotations have medium confidence",
                 50, (int)frames[0].getConfidence());
    assertEquals("Frame anchors have high confidence",
                 100, (int)frames[0].getStart().getConfidence());
    String dataUrl = (String)frames[0].get("dataUrl");
    assertNotNull("First frame includes URL for data", dataUrl);
    System.out.println("first frame data URL: " + dataUrl);
    File png = new File(new URI(dataUrl));
    assertTrue("File for data exists", png.exists());
    if (!annotator.keepGeneratedMedia) {
      // tidily delete all files
      for (Annotation frame : frames) {
        dataUrl = (String)frame.get("dataUrl");
        if (dataUrl != null) {
          png = new File(new URI(dataUrl));
          if (png.exists()) {
            png.delete();
          }
        }
      } // next frame
    }

    Annotation[] frameCount = g.all("transcript_frameCount");
    assertEquals("There is one frame count annotation", 1, frameCount.length);
    assertEquals("Frame count annotation is high confidence",
                 100, (int)frameCount[0].getConfidence());

    try {
      int count = Integer.parseInt(frameCount[0].getLabel());
      assertEquals("Frame count is correct",
                    frames.length, count);
    } catch(NumberFormatException exception) {
      fail("Frame count label is numeric (number of frames): " + frameCount[0]
           + " - " + exception);
    }

    assertNull("no annotated video generated",
               ((GraphStoreHarness)annotator.getStore()).id);
  }

  /** JSON results for each frame are created. */
  @Test public void jsonResults() throws Exception {
    // if (annotator.getStatusObservers().size() == 0) {
    //   annotator.getStatusObservers().add(status->System.out.println(status));
    // }
    // annotator.keepGeneratedMedia = true; 
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    annotator.setTaskParameters(
      "resultLayerId=frame" // doesn't already exist
      );
    assertEquals("resultLayerId set", "frame", annotator.getResultLayerId());
    
    Layer layer = annotator.getSchema().getLayer("frame");
    assertNotNull("frame layer created", layer);
    assertEquals("frame alignment", Constants.ALIGNMENT_INSTANT, layer.getAlignment());
    assertEquals("frame type", "application/json", layer.getType());
    assertTrue("frame peers", layer.getPeers());
    assertEquals("frame parent", schema.getRoot().getId(), layer.getParentId());
    assertNull("frame category unset", layer.getCategory());
    
    Set<String> outputLayers = new HashSet<String>(Arrays.asList(annotator.getOutputLayers()));
    assertEquals("Correct number of output layers: " + outputLayers,
                 1, outputLayers.size());
    assertTrue("frame layer output", outputLayers.contains("frame"));
    
    g.trackChanges();
    annotator.transform(g);   

    Annotation[] frames = g.all("frame");
    assertTrue("There are frame annotations", frames.length > 0);
    // labels are formatted as fragment filenames: {transcript}_{layer}__{offset}.json
    try {
      Integer.parseInt(frames[0].getLabel());
    } catch(NumberFormatException exception) {
      fail("First frame label is numeric (frame number): " + frames[0] + " - " + exception);
    }
    System.out.println("first frame: " + frames[0].getStart() + ": " + frames[0]);
    assertEquals("Frame annotations have medium confidence",
                 50, (int)frames[0].getConfidence());
    assertEquals("Frame anchors have high confidence",
                 100, (int)frames[0].getStart().getConfidence());
    String dataUrl = (String)frames[0].get("dataUrl");
    assertNotNull("First frame includes URL for data", dataUrl);
    System.out.println("first frame data URL: " + dataUrl);
    File json = new File(new URI(dataUrl));
    assertTrue("File for data exists", json.exists());
    if (!annotator.keepGeneratedMedia) {
      // tidily delete all files
      for (Annotation frame : frames) {
        dataUrl = (String)frame.get("dataUrl");
        if (dataUrl != null) {
          json = new File(new URI(dataUrl));
          if (json.exists()) {
            json.delete();
          }
        }
      } // next frame
    }
  }

  /**
   * Returns a graph for annotating.
   * @return The graph for testing with.
   */
  public static Graph graph() throws Exception {
    Schema schema = new Schema(
      "participant", "turn", "utterance", "word",
      new Layer("frames", "Frames").setAlignment(Constants.ALIGNMENT_INSTANT)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setCategory("media-frame-category"),
      new Layer("participant", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("participant").setParentIncludes(true),
      new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("mfa", "MFA tag").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("phonemes", "Phonemic transcription").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true), // ARPABET layer
      new Layer("segment", "Phones").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true)
      .setType("ipa"));
    // annotate a graph
    Graph g = new Graph()
      .setSchema(schema);
    g.setId("test.eaf");
    Anchor start = g.getOrCreateAnchorAt(10, Constants.CONFIDENCE_MANUAL);
    Anchor end = g.getOrCreateAnchorAt(14, Constants.CONFIDENCE_MANUAL);
    g.addAnnotation(
      new Annotation().setLayerId("participant").setLabel("someone")
      .setStart(start).setEnd(end));
    Annotation turn = g.addAnnotation(
      new Annotation().setLayerId("turn").setLabel("someone")
      .setStart(start).setEnd(end)
      .setParent(g.first("participant")));
    g.addAnnotation(
      new Annotation().setLayerId("utterance").setLabel("someone")
      .setStart(start).setEnd(end)
      .setParent(turn));
    
    Annotation statute = g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("statute")
      .setStart(g.getOrCreateAnchorAt(11, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(13, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("phonemes").setLabel("S T AE1 CH UW0 T")
                    .setParent(statute));

    // access to test media
    g.setMediaProvider(new GraphMediaProvider() {
        public MediaFile[] getAvailableMedia() throws StoreException, PermissionException {
          try {
            return new MediaFile[] { new MediaFile(new File(dir(), "test.mp4")) };
          } catch (Exception x) {
            throw new StoreException(x);
          }
        }
        public String getMedia(String trackSuffix, String mimeType) 
          throws StoreException, PermissionException {
          try {
            return getAvailableMedia()[0].getFile().toURL().toString();
          } catch (Exception x) {
            throw new StoreException(x);
          }
        }
        public GraphMediaProvider providerForGraph(Graph graph) {
          return this;
        }
      });
    
    return g;
  } // end of graph()

  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.annotator.mediapipe.TestMediaPipeAnnotator");
  }
}
