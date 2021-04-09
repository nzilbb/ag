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

package nzilbb.annotator.stanfordpos;
	      
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
import nzilbb.annotator.stanfordpos.StanfordPosTagger;

public class TestStanfordPosTagger {
  
  static StanfordPosTagger annotator = new StanfordPosTagger();
  
  @BeforeClass
  public static void install() throws Exception {
    
    System.out.println("Installing models if necessary...");
    
    // find the current directory
    File dir = dir();
    
    // set the schema
    annotator.setSchema(graph().getSchema());
    
    // set the working directory
    annotator.setWorkingDirectory(dir);
    
    // set the annotator configuration, which will install the models the first time (only)
    annotator.setConfig(annotator.getConfig());
    
    System.out.println("Models installed.");
  }
  
  public static File dir() throws Exception { 
    URL urlThisClass = TestStanfordPosTagger.class.getResource(
      TestStanfordPosTagger.class.getSimpleName() + ".class");
    File fThisClass = new File(urlThisClass.toURI());
    return fThisClass.getParentFile();
  }
  
  @Test public void defaultParameters() throws Exception {
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // use default configuration
    annotator.setTaskParameters(null);
    
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("chunk layer",
                 "turn", annotator.getChunkLayerId());
    assertEquals("transcript language layer",
                 "transcript_language", annotator.getTranscriptLanguageLayerId());
    assertEquals("phrase language layer",
                 "lang", annotator.getPhraseLanguageLayerId());
    assertEquals("pos layer",
                 "pos", annotator.getPosLayerId());
    assertNotNull("pos layer was created",
                  schema.getLayer(annotator.getPosLayerId()));
    assertEquals("pos layer child of word",
                 "word", schema.getLayer(annotator.getPosLayerId()).getParentId());
    assertEquals("pos layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getPosLayerId()).getAlignment());
    assertEquals("pos layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getPosLayerId()).getType());
    assertEquals("model: english-caseless-left3words-distsim.tagger",
                 "english-caseless-left3words-distsim.tagger", annotator.getModel());
    assertFalse("pos layer disallows peers",
                schema.getLayer(annotator.getPosLayerId()).getPeers());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("4 required layer: "+requiredLayers,
                 4, requiredLayers.size());
    assertTrue("turn required "+requiredLayers,
               requiredLayers.contains("turn"));
    assertTrue("word required "+requiredLayers,
               requiredLayers.contains("word"));
    assertTrue("transcript_language required "+requiredLayers,
               requiredLayers.contains("transcript_language"));
    assertTrue("lang required "+requiredLayers,
               requiredLayers.contains("lang"));
    String outputLayers[] = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                 1, outputLayers.length);
    assertEquals("output layer correct "+Arrays.asList(outputLayers),
                 "pos", outputLayers[0]);
    
    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "I", firstWord.getLabel());
      
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 9, g.all("word").length);
    assertEquals("double check there are no poses: "+Arrays.asList(g.all("pos")),
                 0, g.all("pos").length);
    // run the annotator
    annotator.transform(g);
    List<String> posLabels = Arrays.stream(g.all("pos"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("Correct number of tokens "+posLabels,
                 9, posLabels.size());
    Iterator<String> poses = posLabels.iterator();
    assertEquals("I", "PRP", poses.next());
    assertEquals("sang", "VBD", poses.next());
    assertEquals("and", "CC", poses.next());
    assertEquals("w~ (fragment)", "NNP", poses.next());
    assertEquals("walked", "VBD", poses.next());
    assertEquals("about", "RB", poses.next());
    assertEquals("my", "PRP$", poses.next());
    assertEquals("blogging-posting (OOD)", "VBG", poses.next());
    assertEquals("lazily", "RB", poses.next());
    
    // add a word
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("new")
                    .setStart(g.getOrCreateAnchorAt(90)).setEnd(g.getOrCreateAnchorAt(100))
                    .setParent(g.first("turn")));
    
    // change a word
    firstWord.setLabel("John");
    
    // run the annotator again
    annotator.transform(g);
    posLabels = Arrays.stream(g.all("pos"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("one more pos: "+posLabels,
                 10, posLabels.size());
    poses = posLabels.iterator();
    assertEquals("John (updated POS)", "NNP", poses.next());
    assertEquals("sang", "VBD", poses.next());
    assertEquals("and", "CC", poses.next());
    assertEquals("w~ (fragment)", "NNP", poses.next());
    assertEquals("walked", "VBD", poses.next());
    assertEquals("about", "RB", poses.next());
    assertEquals("my", "PRP$", poses.next());
    assertEquals("blogging-posting (OOD)", "VBG", poses.next());
    assertEquals("lazily", "RB", poses.next());
    assertEquals("new", "JJ", poses.next());

  }   

  @Test public void setTaskParameters() throws Exception {
    
    Graph g = graph();
    // tag the graph as being in New Zealand English
    g.addTag(g, "transcript_language", "en-NZ");
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    g.setId("setTaskParameters");

    // use specified configuration
    annotator.setTaskParameters(
      "tokenLayerId=word"
      +"&chunkLayerId=utterance"       // non-default layer
      +"&transcriptLanguageLayerId="   // no transcript language layer
      +"&phraseLanguageLayerId="       // no phrase language layer
      +"&posLayerId=stanfordpos"       // non-default layer
      +"&model=english-bidirectional-distsim.tagger"); // non-default tagger
    
    assertFalse("double-check utterances are available",
                g.all("utterance").length == 0);
    
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("chunk layer",
                 "utterance", annotator.getChunkLayerId());
    assertNull("transcript language layer",
               annotator.getTranscriptLanguageLayerId());
    assertNull("phrase language layer",
               annotator.getPhraseLanguageLayerId());
    assertEquals("pos layer",
                 "stanfordpos", annotator.getPosLayerId());
    assertNotNull("pos layer was created",
                  schema.getLayer(annotator.getPosLayerId()));
    assertEquals("pos layer child of word",
                 "word", schema.getLayer(annotator.getPosLayerId()).getParentId());
    assertEquals("pos layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getPosLayerId()).getAlignment());
    assertEquals("pos layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getPosLayerId()).getType());
    assertFalse("pos layer disallows peers",
                schema.getLayer(annotator.getPosLayerId()).getPeers());
    assertEquals("model: english-bidirectional-distsim.tagger",
                 "english-bidirectional-distsim.tagger", annotator.getModel());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("2 required layer: "+requiredLayers,
                 2, requiredLayers.size());
    assertTrue("utterance required "+requiredLayers,
               requiredLayers.contains("utterance"));
    assertTrue("word required "+requiredLayers,
               requiredLayers.contains("word"));
    String outputLayers[] = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                 1, outputLayers.length);
    assertEquals("output layer correct "+Arrays.asList(outputLayers),
                 "stanfordpos", outputLayers[0]);
    
    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "I", firstWord.getLabel());
    
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 9, g.all("word").length);
    assertEquals("double check there are no poses: "+Arrays.asList(g.all("pos")),
                 0, g.all("pos").length);
    // run the annotator
    annotator.transform(g);
    List<String> posLabels = Arrays.stream(g.all("stanfordpos"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("Correct number of tokens "+posLabels,
                 9, posLabels.size());
    Iterator<String> poses = posLabels.iterator();
    assertEquals("I", "PRP", poses.next());
    assertEquals("sang", "VBD", poses.next());
    assertEquals("and", "CC", poses.next());
    assertEquals("w~ (fragment)", "NNP", poses.next());
    assertEquals("walked", "VBD", poses.next());
    assertEquals("about (different from default model)",
                 "IN", poses.next());
    assertEquals("my", "PRP$", poses.next());
    assertEquals("blogging-posting (OOD - different from default model)",
                 "NN", poses.next());
    assertEquals("lazily", "RB", poses.next());
  }   

  @Test public void setInvalidTaskParameters() throws Exception {
    
    try {
      annotator.setTaskParameters(
        // doesn't exist in the schema:
        "tokenLayerId=orthography"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&posLayerId=stanfordpos"
        +"&model=english-bidirectional-distsim.tagger");
      fail("Should fail with nonexistent tokenLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        // doesn't exist in the schema:
        +"&transcriptLanguageLayerId=language"
        +"&phraseLanguageLayerId=lang"
        +"&posLayerId=stanfordpos"
        +"&model=english-bidirectional-distsim.tagger");
      fail("Should fail with nonexistent transcriptLanguageLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        // doesn't exist in the schema:
        +"&phraseLanguageLayerId=language"
        +"&posLayerId=stanfordpos"
        +"&model=english-bidirectional-distsim.tagger");
      fail("Should fail with nonexistent phraseLanguageLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        // same as token layer:
        +"&posLayerId=word"
        +"&model=english-bidirectional-distsim.tagger");
      fail("Should fail with posLayerId = word");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&posLayerId=pos"
        // nonexistent model
        +"&model=nonexistent.tagger");
      fail("Should fail with model = nonexistent.tagger");
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
      new Layer("lang", "Phrase Language").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true));
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
    
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("I")
                    .setStart(g.getOrCreateAnchorAt(10)).setEnd(g.getOrCreateAnchorAt(20))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("sang")
                    .setStart(g.getOrCreateAnchorAt(20)).setEnd(g.getOrCreateAnchorAt(30))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("and")
                    .setStart(g.getOrCreateAnchorAt(30)).setEnd(g.getOrCreateAnchorAt(40))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("w~")
                    .setStart(g.getOrCreateAnchorAt(40)).setEnd(g.getOrCreateAnchorAt(45))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("walked")
                    .setStart(g.getOrCreateAnchorAt(45)).setEnd(g.getOrCreateAnchorAt(50))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("about")
                    .setStart(g.getOrCreateAnchorAt(50)).setEnd(g.getOrCreateAnchorAt(60))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("my")
                    .setStart(g.getOrCreateAnchorAt(60)).setEnd(g.getOrCreateAnchorAt(70))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("blogging-posting")
                    .setStart(g.getOrCreateAnchorAt(70)).setEnd(g.getOrCreateAnchorAt(80))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("lazily")
                    .setStart(g.getOrCreateAnchorAt(80)).setEnd(g.getOrCreateAnchorAt(90))
                    .setParent(turn));
    return g;
  } // end of graph()
  
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.annotator.stanfordpos.TestStanfordPosTagger");
  }
}
