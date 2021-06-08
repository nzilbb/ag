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

package nzilbb.annotator.mor;
	      
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
import nzilbb.ag.util.DefaultOffsetGenerator;

public class TestMorTagger {
  
  static MorTagger annotator = new MorTagger();
  
  @BeforeClass
  public static void install() throws Exception {
    
    System.out.println("Installing mor/grammar if necessary...");
    
    // find the current directory
    File dir = dir();
    
    // set the schema
    annotator.setSchema(graph().getSchema());
    
    // set the working directory
    annotator.setWorkingDirectory(dir);
    
    // set the annotator configuration, which will install the models the first time (only)
    annotator.setConfig(annotator.getConfig());
    
    System.out.println("MOR installed.");
  }
  
  public static File dir() throws Exception { 
    URL urlThisClass = TestMorTagger.class.getResource(
      TestMorTagger.class.getSimpleName() + ".class");
    File fThisClass = new File(urlThisClass.toURI());
    return fThisClass.getParentFile();
  }
  
  @Test public void defaultParameters() throws Exception {
    
    Graph g = graph();
    g.trackChanges();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // use default configuration
    annotator.setTaskParameters(null);
    
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("transcript language layer",
                 "transcript_language", annotator.getLanguagesLayerId());
    assertEquals("mor layer",
                 "mor", annotator.getMorLayerId());
    assertNotNull("mor layer was created",
                  schema.getLayer(annotator.getMorLayerId()));
    assertEquals("mor layer child of word",
                 "word", schema.getLayer(annotator.getMorLayerId()).getParentId());
    assertEquals("mor layer aligned",
                 Constants.ALIGNMENT_INTERVAL,
                 schema.getLayer(annotator.getMorLayerId()).getAlignment());
    assertEquals("mor layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getMorLayerId()).getType());
    assertTrue("mor layer allows peers", // contractions like "I'll" might have two tags
                schema.getLayer(annotator.getMorLayerId()).getPeers());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("Required layer: "+requiredLayers,
                 2, requiredLayers.size());
    assertTrue("word required "+requiredLayers,
               requiredLayers.contains("word"));
    assertTrue("transcript_language required "+requiredLayers,
               requiredLayers.contains("transcript_language"));
    String outputLayers[] = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                 1, outputLayers.length);
    assertEquals("output layer correct "+Arrays.asList(outputLayers),
                 "mor", outputLayers[0]);
    
    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "I'll", firstWord.getLabel());
      
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 9, g.all("word").length);
    assertEquals("double check there are no mores: "+Arrays.asList(g.all("mor")),
                 0, g.all("mor").length);
    // run the annotator
    annotator.transform(g);
    List<Annotation> morAnnotations = Arrays.stream(g.all("mor"))
      .collect(Collectors.toList());
    assertEquals("Correct number of tokens (disfluent w~ is skipped) "+morAnnotations,
                 12, morAnnotations.size());
    Iterator<Annotation> mores = morAnnotations.iterator();
    assertEquals("I'll", "pro:sub|I", mores.next().getLabel());
    assertEquals("I'll", "mod|will", mores.next().getLabel());
    assertEquals("sing", "v|sing", mores.next().getLabel());
    assertEquals("and", "coord|and", mores.next().getLabel());
    assertEquals("walk", "n|walk", mores.next().getLabel());
    assertEquals("walk", "v|walk", mores.next().getLabel());
    assertEquals("about", "adv|about", mores.next().getLabel());
    assertEquals("about", "prep|about", mores.next().getLabel());
    assertEquals("my", "det:poss|my", mores.next().getLabel());
    assertEquals("my", "co|my", mores.next().getLabel());
    assertEquals("blogging-morting", "?|bloggingmorting", mores.next().getLabel());
    assertEquals("lazily", "adv|laze&dadj-Y-LY", mores.next().getLabel());

    mores = morAnnotations.iterator();
    String[] wordLabels = {
      "I'll", "I'll", 
      "sing", "and",
      "walk", "walk",
      "about", "about",
      "my", "my",
      "blogging-morting",
      "lazily"
    };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("Tag " + i + " should tag " + wordLabels[i],
                   wordLabels[i], mores.next().first("word").getLabel());
    }
    assertEquals("I'll has two tags", 2, firstWord.all("mor").length);
    
    // add a word
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("new")
                    .setStart(g.getOrCreateAnchorAt(90)).setEnd(g.getOrCreateAnchorAt(100))
                    .setParent(g.first("turn")));
    
    // change a word
    firstWord.setLabel("John");
    
    // run the annotator again
    annotator.transform(g);
    g.commit(); // have to commit to remove old tags
    List<String> morLabels = Arrays.stream(g.all("mor"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("- 1 + 1 = same number of mor as before: "+morLabels,
                 12, morLabels.size());
    Iterator<String> morLs = morLabels.iterator();
    assertEquals("John", "n:prop|John", morLs.next());
    assertEquals("sing", "v|sing", morLs.next());
    assertEquals("and", "coord|and", morLs.next());
    assertEquals("walk", "n|walk", morLs.next());
    assertEquals("walk", "v|walk", morLs.next());
    assertEquals("about", "adv|about", morLs.next());
    assertEquals("about", "prep|about", morLs.next());
    assertEquals("my", "det:poss|my", morLs.next());
    assertEquals("my", "co|my", morLs.next());
    assertEquals("blogging-morting", "?|bloggingmorting", morLs.next());
    assertEquals("lazily", "adv|laze&dadj-Y-LY", morLs.next());
    assertEquals("new", "adj|new", morLs.next());

    assertEquals("John has one tag", 1, firstWord.all("mor").length);

  }   

  @Test public void setInvalidTaskParameters() throws Exception {
    
    try {
      annotator.setTaskParameters(
        // doesn't exist in the schema:
        "tokenLayerId=orthography"
        +"&languagesLayerId=transcript_language"
        +"&morLayerId=mor");
      fail("Should fail with nonexistent tokenLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        // doesn't exist in the schema:
        +"&languagesLayerId=language"
        +"&morLayerId=mor");
      fail("Should fail with nonexistent languagesLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&languagesLayerId=transcript_language"
        // same as token layer:
        +"&morLayerId=word");
      fail("Should fail with morLayerId = word");
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
      new Layer("transcript_language", "Overall Language")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("participant", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("participant").setParentIncludes(true),
      new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true));
    // annotate a graph
    Graph g = new Graph()
      .setSchema(schema);
    g.setId("unit-test");
    Anchor start = g.getOrCreateAnchorAt(1, Constants.CONFIDENCE_MANUAL);
    Anchor end = g.getOrCreateAnchorAt(100, Constants.CONFIDENCE_MANUAL);
    g.addAnnotation(
      new Annotation().setLayerId("participant").setLabel("someone")
      .setStart(start).setEnd(end));
    g.addAnnotation(
      new Annotation().setLayerId("transcript_language").setLabel("en")
      .setStart(start).setEnd(end));
    Annotation turn = g.addAnnotation(
      new Annotation().setLayerId("turn").setLabel("someone")
      .setStart(start).setEnd(end)
      .setParent(g.first("participant")));
    g.addAnnotation(
      new Annotation().setLayerId("utterance").setLabel("someone")
      .setStart(start).setEnd(end)
      .setParent(turn));
    
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("I'll")
                    .setStart(g.getOrCreateAnchorAt(10, Constants.CONFIDENCE_MANUAL)).setEnd(
                      g.getOrCreateAnchorAt(20, Constants.CONFIDENCE_MANUAL))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("sing")
                    .setStart(g.getOrCreateAnchorAt(20)).setEnd(
                      g.getOrCreateAnchorAt(30, Constants.CONFIDENCE_MANUAL))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("and")
                    .setStart(g.getOrCreateAnchorAt(30)).setEnd(
                      g.getOrCreateAnchorAt(40, Constants.CONFIDENCE_MANUAL))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("w~")
                    .setStart(g.getOrCreateAnchorAt(40)).setEnd(
                      g.getOrCreateAnchorAt(45, Constants.CONFIDENCE_MANUAL))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("walk")
                    .setStart(g.getOrCreateAnchorAt(45)).setEnd(
                      g.getOrCreateAnchorAt(50, Constants.CONFIDENCE_MANUAL))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("about")
                    .setStart(g.getOrCreateAnchorAt(50)).setEnd(
                      g.getOrCreateAnchorAt(60, Constants.CONFIDENCE_MANUAL))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("my")
                    .setStart(g.getOrCreateAnchorAt(60)).setEnd(
                      g.getOrCreateAnchorAt(70, Constants.CONFIDENCE_MANUAL))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("blogging-morting")
                    .setStart(g.getOrCreateAnchorAt(70)).setEnd(
                      g.getOrCreateAnchorAt(80, Constants.CONFIDENCE_MANUAL))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("lazily")
                    .setStart(g.getOrCreateAnchorAt(80)).setEnd(
                      g.getOrCreateAnchorAt(90, Constants.CONFIDENCE_MANUAL))
                    .setParent(turn));
    return g;
  } // end of graph()
  
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.annotator.mor.TestMorTagger");
  }
}
