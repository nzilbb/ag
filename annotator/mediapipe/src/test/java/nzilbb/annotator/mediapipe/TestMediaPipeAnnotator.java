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
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
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
import nzilbb.ag.PermissionException;
import nzilbb.ag.Schema;
import nzilbb.ag.StoreException;
import nzilbb.ag.automation.Dictionary;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.UsesRelationalDatabase;
import nzilbb.sql.derby.DerbyConnectionFactory;
import nzilbb.util.IO;
import nzilbb.util.Execution;

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
    
    // not setting the graph store, sorry
    
    if (annotator.getStatusObservers().size() == 0) {
      annotator.getStatusObservers().add(status->System.out.println(status));
    }
    // set the annotator configuration
    annotator.setConfig("");
    
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
               new File(annotator.getWorkingDirectory(), "blendshapes.py").exists());
  }   

  /** Ensure valid task parameters don't raise errors, and change the schema when appropriate. */
  @Test public void setValidTaskParameters() throws Exception {
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    annotator.setTaskParameters(
      "jawForward=jawForward" // partial set of layers
      +"&jawLeft=jawLeft"
      +"&jawOpen=jawOpen"
      +"&jawRight=jawRight"
      +"&mouthClose="         // some layers specified but unset
      +"&mouthDimpleLeft=");
    Layer layer = annotator.getSchema().getLayer("jawForward");
    assertNotNull("jawForward layer created", layer);
    assertEquals("jawForward alignment", Constants.ALIGNMENT_INSTANT, layer.getAlignment());
    assertEquals("jawForward type", Constants.TYPE_NUMBER, layer.getType());
    assertTrue("jawForward peers", layer.getPeers());
    assertEquals("jawForward parent", schema.getRoot().getId(), layer.getParentId());
    layer = annotator.getSchema().getLayer("jawLeft");
    assertNotNull("jawLeft layer created", layer);
    assertEquals("jawLeft alignment", Constants.ALIGNMENT_INSTANT, layer.getAlignment());
    assertEquals("jawLeft type", Constants.TYPE_NUMBER, layer.getType());
    assertTrue("jawLeft peers", layer.getPeers());
    assertEquals("jawLeft parent", schema.getRoot().getId(), layer.getParentId());
    layer = annotator.getSchema().getLayer("jawOpen");
    assertNotNull("jawOpen layer created", layer);
    assertEquals("jawOpen alignment", Constants.ALIGNMENT_INSTANT, layer.getAlignment());
    assertEquals("jawOpen type", Constants.TYPE_NUMBER, layer.getType());
    assertTrue("jawOpen peers", layer.getPeers());
    assertEquals("jawOpen parent", schema.getRoot().getId(), layer.getParentId());
    layer = annotator.getSchema().getLayer("jawRight");
    assertNotNull("jawRight layer created", layer);
    assertEquals("jawRight alignment", Constants.ALIGNMENT_INSTANT, layer.getAlignment());
    assertEquals("jawRight type", Constants.TYPE_NUMBER, layer.getType());
    assertTrue("jawRight peers", layer.getPeers());
    assertEquals("jawRight parent", schema.getRoot().getId(), layer.getParentId());
    assertNull("no mouthClose layer", annotator.getSchema().getLayer("mouthClose"));
    assertNull("no mouthDimpleLeft layer", annotator.getSchema().getLayer("mouthDimpleLeft"));

    Set<String> requiredLayers = new HashSet<String>(Arrays.asList(annotator.getRequiredLayers()));
    assertEquals("Correct number of required layers: " + requiredLayers,
                 0, requiredLayers.size());

    Set<String> outputLayers = new HashSet<String>(Arrays.asList(annotator.getOutputLayers()));
    assertEquals("Correct number of output layers: " + outputLayers,
                 4, outputLayers.size());
    assertTrue("jawForward output", outputLayers.contains("jawForward"));
    assertTrue("jawLeft output", outputLayers.contains("jawLeft"));
    assertTrue("jawOpen output", outputLayers.contains("jawOpen"));
    assertTrue("jawRight output", outputLayers.contains("jawRight"));

  }

  /** Facial feature score annotations are created. */
  @Test public void featureScoreAnnotations() throws Exception {
    if (annotator.getStatusObservers().size() == 0) {
      annotator.getStatusObservers().add(status->System.out.println(status));
    }
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    annotator.setTaskParameters(
      "jawForward=jawForward" // partial set of layers
      +"&jawLeft=jawLeft"
      +"&jawOpen=jawOpen"
      +"&jawRight=jawRight"
      +"&mouthClose="         // some layers specified but unset
      +"&mouthDimpleLeft=");

    g.trackChanges();
    annotator.transform(g);
        
    Annotation[] scores = g.all("jawForward");
    assertTrue("There are jawForward scores", scores.length > 0);
    System.out.println("jawForward: " + scores[0].getStart() + ": " + scores[0]);
    scores = g.all("jawLeft");
    assertTrue("There are jawLeft scores", scores.length > 0);
    System.out.println("jawLeft: " + scores[0].getStart() + ": " + scores[0]);
    scores = g.all("jawOpen");
    assertTrue("There are jawOpen scores", scores.length > 0);
    System.out.println("jawOpen: " + scores[0].getStart() + ": " + scores[0]);
    scores = g.all("jawRight");
    assertTrue("There are jawRight scores", scores.length > 0);
    System.out.println("jawRight: " + scores[0].getStart() + ": " + scores[0]);
  }
  
  /**
   * Returns a graph for annotating.
   * @return The graph for testing with.
   */
  public static Graph graph() throws Exception {
    Schema schema = new Schema(
      "participant", "turn", "utterance", "word",
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
    g.setId("test");
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
