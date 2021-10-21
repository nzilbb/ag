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
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import nzilbb.ag.Anchor;
import nzilbb.ag.Annotation;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.automation.Dictionary;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.UsesRelationalDatabase;
import nzilbb.sql.derby.DerbyConnectionFactory;

public class TestHTKAligner {

  static HTKAligner annotator = new HTKAligner();
  
  @BeforeClass
  public static void install() throws Exception {
    
    System.out.println("Installing HTK if necessary...");
    
    // find the current directory
    File dir = dir();
    
    // set the schema
    annotator.setSchema(graph().getSchema());
    
    // set the working directory
    annotator.setWorkingDirectory(dir);
    
    // use derby for relational database
    //TODO annotator.setRdbConnectionFactory(new DerbyConnectionFactory(dir));
    
    // set the annotator configuration, which will install the lexicon the first time (only)
    annotator.setConfig(annotator.getConfig());
    
    System.out.println("Installed.");
  }
  
  public static File dir() throws Exception { 
    URL urlThisClass = TestHTKAligner.class.getResource(
      TestHTKAligner.class.getSimpleName() + ".class");
    File fThisClass = new File(urlThisClass.toURI());
    return fThisClass.getParentFile();
  }
  
  @Test public void defaultParameters() throws Exception {
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    try { // use default configuration
      annotator.setTaskParameters(null);
      fail("Should fail with default configuration");
    } catch (InvalidConfigurationException x) {
    }
  }   
  
  @Test public void setValidParameters() throws Exception {
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&pronunciationLayerId=phonemes"
      +"&noiseLayerId="
      +"&utteranceTagLayerId=htk"
      +"&participantTagLayerId="
      +"&wordAlignmentLayerId=word"
      +"&phoneAlignmentLayerId=segment"
      +"&scoreLayerId="
      +"&overlapThreshold=5"
      +"&cleanupOption=75"
      +"&noisePatterns=laugh.* unclear .*noise.*"
      +"&leftPattern="
      +"&rightPattern="
      +"&pauseMarkers=-");
    
    // layers are created as required
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&pronunciationLayerId=phonemes"
      +"&noiseLayerId="
      +"&utteranceTagLayerId=utterance_htk" // nonexistent
      +"&participantTagLayerId=participant_htk" // nonexistent
      +"&wordAlignmentLayerId=word_alignment"
      +"&phoneAlignmentLayerId=phone"
      +"&scoreLayerId=score"
      +"&overlapThreshold=5"
      +"&cleanupOption=75"
      +"&noisePatterns=laugh.* unclear .*noise.*"
      +"&leftPattern="
      +"&rightPattern="
      +"&pauseMarkers=-");
    Layer layer = annotator.getSchema().getLayer("utterance_htk");
    assertNotNull("utterance_htk layer created", layer);
    layer = annotator.getSchema().getLayer("participant_htk");
    assertNotNull("participant_htk layer created", layer);
    layer = annotator.getSchema().getLayer("word_alignment");
    assertNotNull("word_alignment layer created", layer);
    layer = annotator.getSchema().getLayer("phone");
    assertNotNull("phone layer created", layer);
    layer = annotator.getSchema().getLayer("score");
    assertNotNull("score layer created", layer);
  }   
  
  @Test public void setInvalidTaskParameters() throws Exception {
    
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=orthography" // nonexistent
        +"&pronunciationLayerId=phonemes"
        +"&noiseLayerId="
        +"&utteranceTagLayerId=htk"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=segment"
        +"&scoreLayerId="
        +"&overlapThreshold=5"
        +"&cleanupOption=75"
        +"&noisePatterns=laugh.* unclear .*noise.*"
        +"&leftPattern="
        +"&rightPattern="
        +"&pauseMarkers=-");
      fail("Should fail with nonexistent orthographyLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=word"
        +"&pronunciationLayerId=phonology" // nonexistent
        +"&noiseLayerId="
        +"&utteranceTagLayerId=htk"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=segment"
        +"&scoreLayerId="
        +"&overlapThreshold=5"
        +"&cleanupOption=75"
        +"&noisePatterns=laugh.* unclear .*noise.*"
        +"&leftPattern="
        +"&rightPattern="
        +"&pauseMarkers=-");
      fail("Should fail with nonexistent pronunciationLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=word"
        +"&pronunciationLayerId=phonemes"
        +"&noiseLayerId=noise" // nonexistent
        +"&utteranceTagLayerId=htk"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=segment"
        +"&scoreLayerId="
        +"&overlapThreshold=5"
        +"&cleanupOption=75"
        +"&noisePatterns=laugh.* unclear .*noise.*"
        +"&leftPattern="
        +"&rightPattern="
        +"&pauseMarkers=-");
      fail("Should fail with nonexistent noiseLayerId");
    } catch (InvalidConfigurationException x) {
    }
  }
  
  /**
   * Returns a graph for annotating.
   * @return The graph for testing with.
   */
  public static Graph graph() {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("participant", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("participant").setParentIncludes(true),
      new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("htk", "HTK tag").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("phonemes", "Phonemic transcription").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true),
      new Layer("segment", "Phones").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true));
    // annotate a graph
    Graph g = new Graph()
      .setSchema(schema);
    Anchor start = g.getOrCreateAnchorAt(1);
    Anchor end = g.getOrCreateAnchorAt(100);
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
    
    Annotation testing = g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("testing")
      .setStart(g.getOrCreateAnchorAt(10)).setEnd(g.getOrCreateAnchorAt(20))
      .setParent(turn));
    Annotation one = g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("one")
      .setStart(g.getOrCreateAnchorAt(20)).setEnd(g.getOrCreateAnchorAt(30))
      .setParent(turn));
    Annotation two = g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("two")
      .setStart(g.getOrCreateAnchorAt(30)).setEnd(g.getOrCreateAnchorAt(40))
      .setParent(turn));
    Annotation three = g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("three")
      .setStart(g.getOrCreateAnchorAt(40)).setEnd(g.getOrCreateAnchorAt(45))
      .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("phonemes").setLabel("T EH1 S T IH0 NG")
                    .setParent(testing));
    g.addAnnotation(new Annotation().setLayerId("phonemes").setLabel("W AH1 N")
                    .setParent(one));
    g.addAnnotation(new Annotation().setLayerId("phonemes").setLabel("HH W AH1 N")
                    .setParent(one));
    g.addAnnotation(new Annotation().setLayerId("phonemes").setLabel("T UW1")
                    .setParent(two));
    g.addAnnotation(new Annotation().setLayerId("phonemes").setLabel("TH R IY1")
                    .setParent(three));
    return g;
  } // end of graph()
  
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.annotator.htk.TestHTKAligner");
  }
}
