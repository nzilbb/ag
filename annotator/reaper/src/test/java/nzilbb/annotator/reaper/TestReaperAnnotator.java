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
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
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
import nzilbb.ag.PermissionException;
import nzilbb.ag.Schema;
import nzilbb.ag.StoreException;
import nzilbb.ag.TransformationException;
import nzilbb.ag.automation.Dictionary;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.UsesRelationalDatabase;
import nzilbb.util.IO;

public class TestReaperAnnotator {

  /** Infer the directory of the tests. */
  public static File dir() throws Exception { 
    URL urlThisClass = TestReaperAnnotator.class.getResource(
      TestReaperAnnotator.class.getSimpleName() + ".class");
    File fThisClass = new File(urlThisClass.toURI());
    return fThisClass.getParentFile();
  }
  
  /** Ensure default (null) task parameters return no error, and processing is successful. */
  @Test public void defaultParameters() throws Exception {

    ReaperAnnotator annotator = new ReaperAnnotator();   
    GraphStoreHarness store = new GraphStoreHarness(dir());
    Graph g = graph(store);
    Schema schema = g.getSchema();
    annotator.setStore(store);
    annotator.setSchema(schema);
    annotator.setWorkingDirectory(dir());
    // annotator.getStatusObservers().add(status->System.out.println(status));
    
    annotator.setTaskParameters(null);
    assertNull("f0LayerId unset", annotator.getF0LayerId());
    assertNull("f0FrameInterval unset", annotator.getF0FrameInterval());
    assertNull("minF0 unset", annotator.getMinF0());
    assertNull("maxF0 unset", annotator.getMaxF0());
    assertFalse("hilbertTransform unset", annotator.getHilbertTransform());
    assertFalse("suppressHighPassFilter unset", annotator.getHilbertTransform());

    annotator.transform(g);
    assertTrue("f0 file exists", store.savedMedia.keySet().contains("Test.f0"));
    assertTrue("pm file exists", store.savedMedia.keySet().contains("Test.pm"));
  }   

  /** Ensure valid parameters don't raise errors, and change the schema when
   * appropriate.*/ 
  @Test public void parameters() throws Exception {

    ReaperAnnotator annotator = new ReaperAnnotator();
    GraphStoreHarness store = new GraphStoreHarness(dir());
    Graph g = graph(store);
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    annotator.setWorkingDirectory(dir());

    // set all parameters
    annotator.setTaskParameters(
      "f0FrameInterval=0.1"
      +"&minF0=100"
      +"&maxF0=10000"
      +"&hilbertTransform=on"
      +"&suppressHighPassFilter=true"
      +"&f0LayerId=pitch");

    assertEquals("f0LayerId set", "pitch", annotator.getF0LayerId());
    Layer layer = annotator.getSchema().getLayer("pitch");
    assertNotNull("pitch layer created", layer);
    assertEquals("phone layer type", Constants.TYPE_NUMBER, layer.getType());
    assertEquals("phone instants", Constants.ALIGNMENT_INSTANT, layer.getAlignment());
    assertEquals("phone layer parent", schema.getRoot().getId(), layer.getParentId());
    assertEquals("f0FrameInterval set", Double.valueOf(0.1), annotator.getF0FrameInterval());
    assertEquals("minF0 set", Integer.valueOf(100), annotator.getMinF0());
    assertEquals("maxF0 set", Integer.valueOf(10000), annotator.getMaxF0());
    assertTrue("hilbertTransform set", annotator.getHilbertTransform());
    assertTrue("suppressHighPassFilter set", annotator.getHilbertTransform());
  }   

  /** Ensure that invalid task parameters generate errors. */
  @Test public void setInvalidTaskParameters() throws Exception {
    
    ReaperAnnotator annotator = new ReaperAnnotator();
    GraphStoreHarness store = new GraphStoreHarness(dir());
    Graph g = graph(store);
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    annotator.setWorkingDirectory(dir());
    try {
      annotator.setTaskParameters(
        "f0LayerId=word"); // not child of graph
      fail("Should fail with non-span layer configured");
    } catch (InvalidConfigurationException x) {
      //System.out.println(""+x);
    }
  }

  /** Ensure it only runs on full graphs, not fragments. */
  @Test public void fullGraphOnly() throws Exception {

    ReaperAnnotator annotator = new ReaperAnnotator();   
    GraphStoreHarness store = new GraphStoreHarness(dir());
    Graph f = fragment(store);
    Schema schema = f.getSchema();
    annotator.setStore(store);
    annotator.setSchema(schema);
    annotator.setWorkingDirectory(dir());
    annotator.getStatusObservers().add(status->System.out.println(status));
    
    annotator.setTaskParameters(null);
    assertNull("f0LayerId unset", annotator.getF0LayerId());
    assertNull("f0FrameInterval unset", annotator.getF0FrameInterval());
    assertNull("minF0 unset", annotator.getMinF0());
    assertNull("maxF0 unset", annotator.getMaxF0());
    assertFalse("hilbertTransform unset", annotator.getHilbertTransform());
    assertFalse("suppressHighPassFilter unset", annotator.getHilbertTransform());

    try {
      annotator.transform(f);
      fail("Should not transform fragments");
    } catch(TransformationException exception) {
    }
    assertFalse("f0 file doesn't exist", store.savedMedia.keySet().contains("Test.f0"));
    assertFalse("pm file doesn't exist", store.savedMedia.keySet().contains("Test.pm"));
  }   

  /** F0 annotations. */
  @Test public void f0Annotations() throws Exception {

    ReaperAnnotator annotator = new ReaperAnnotator();   
    GraphStoreHarness store = new GraphStoreHarness(dir());
    Graph g = graph(store);
    Schema schema = g.getSchema();
    annotator.setStore(store);
    annotator.setSchema(schema);
    annotator.setWorkingDirectory(dir());
    //annotator.getStatusObservers().add(status->System.out.println(status));
    
    annotator.setTaskParameters("f0LayerId=pitch");
    assertEquals("f0LayerId set", "pitch", annotator.getF0LayerId());
    Layer layer = annotator.getSchema().getLayer("pitch");
    assertNotNull("pitch layer created", layer);
    assertEquals("phone layer type", Constants.TYPE_NUMBER, layer.getType());
    assertEquals("phone instants", Constants.ALIGNMENT_INSTANT, layer.getAlignment());
    assertEquals("phone layer parent", schema.getRoot().getId(), layer.getParentId());
    assertNull("f0FrameInterval not set", annotator.getF0FrameInterval());
    assertNull("minF0 not set", annotator.getMinF0());
    assertNull("maxF0 not set", annotator.getMaxF0());
    assertFalse("hilbertTransform set", annotator.getHilbertTransform());
    assertFalse("suppressHighPassFilter set", annotator.getHilbertTransform());

    annotator.transform(g);
    assertTrue("f0 file exists", store.savedMedia.keySet().contains("Test.f0"));
    assertTrue("pm file exists", store.savedMedia.keySet().contains("Test.pm"));

    Annotation[] annotations = g.all("pitch");
    assertTrue("There are annotations", annotations.length >= 289);
  }   

  /**
   * Returns a fragment for annotating.
   * @return The graph for testing with.
   */
  public static Graph fragment(GraphStoreHarness store) throws Exception {
    Graph g = graph(store);
    Schema schema = g.getSchema();
    Graph f = g.getFragment(
      10.0, 20.0, (String[])schema.getLayers().keySet().toArray(new String[0]));
    return f;
  }
  
  /**
   * Returns a graph for annotating.
   * @return The graph for testing with.
   */
  public static Graph graph(GraphStoreHarness store) throws Exception {
    
    // annotate a graph
    Graph g = new Graph()
      .setSchema(store.getSchema());
    g.setId("Test.TextGrid");
    Anchor start = g.getOrCreateAnchorAt(10, Constants.CONFIDENCE_MANUAL);
    Anchor end = g.getOrCreateAnchorAt(20, Constants.CONFIDENCE_MANUAL);
    g.addAnnotation(
      new Annotation().setLayerId("transcript_language").setLabel("en-NZ")
      .setStart(start).setEnd(end));
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
    
    g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("saved")
      .setStart(g.getOrCreateAnchorAt(11, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(12, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn).setOrdinal(1));
    g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("up")
      .setStart(g.getOrCreateAnchorAt(12, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(13, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn).setOrdinal(2));
    g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("some")
      .setStart(g.getOrCreateAnchorAt(13, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(14, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn).setOrdinal(3));
    g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("money")
      .setStart(g.getOrCreateAnchorAt(14, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(15, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn).setOrdinal(4));
    g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("he")
      .setStart(g.getOrCreateAnchorAt(15, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(16, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn).setOrdinal(5));
    g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("bought")
      .setStart(g.getOrCreateAnchorAt(16, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(17, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn).setOrdinal(6));
    g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("property")
      .setStart(g.getOrCreateAnchorAt(17, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(18, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn).setOrdinal(7));
    
    // access to test media
    final File wav = new File(dir(), "test.wav");
    g.setMediaProvider(new GraphMediaProvider() {
        public MediaFile[] getAvailableMedia() throws StoreException, PermissionException {
          try {
            Vector<MediaFile> media = new Vector<MediaFile>();
            media.add(new MediaFile(wav));
            for (File f : store.savedMedia.values()) media.add(new MediaFile(f));
            return media.toArray(new MediaFile[0]); 
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
    org.junit.runner.JUnitCore.main("nzilbb.annotator.reaper.TestReaperAnnotator");
  }
}
