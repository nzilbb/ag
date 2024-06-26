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

  /** Ensures that if there's an existing POS layer with an incorrect configuration, 
   * it's corrected. */
  @Test public void existingLayerCorrected() throws Exception {
    
    Graph g = graph();
    Schema schema = g.getSchema();    
    // add POS layer with incorrect configuration (the one maybe created by LaBB-CAT)
    schema.addLayer(
      new Layer("pos", "Incorrectly configured POS layer")
      .setType(Constants.TYPE_NUMBER)
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false)
      .setPeersOverlap(true)
      .setSaturated(false)
      .setParentId("word"));
    annotator.setSchema(schema);
    annotator.setTaskParameters(null);
    assertEquals("pos layer",
                 "pos", annotator.getPosLayerId());
    assertEquals("pos layer aligned",
                 Constants.ALIGNMENT_INTERVAL,
                 schema.getLayer(annotator.getPosLayerId()).getAlignment());
    assertEquals("pos layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getPosLayerId()).getType());
    assertTrue("pos layer peers",
               schema.getLayer(annotator.getPosLayerId()).getPeers());
    assertTrue("pos layer included",
               schema.getLayer(annotator.getPosLayerId()).getParentIncludes());
    assertFalse("pos layer peers don't overlap",
               schema.getLayer(annotator.getPosLayerId()).getPeersOverlap());
    assertTrue("pos layer saturated",
               schema.getLayer(annotator.getPosLayerId()).getSaturated());
  }

  /** Annotation with default settings works as expected. */
  @Test public void defaultParameters() throws Exception {
    
    Graph g = graph();
    g.trackChanges();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // use default configuration
    annotator.setTaskParameters(null);
    
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("token exclusion pattern",
                 "", annotator.getTokenExclusionPattern());
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
    assertEquals("pos layer aligned",
                 Constants.ALIGNMENT_INTERVAL,
                 schema.getLayer(annotator.getPosLayerId()).getAlignment());
    assertEquals("pos layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getPosLayerId()).getType());
    assertTrue("pos layer peers",
               schema.getLayer(annotator.getPosLayerId()).getPeers());
    assertTrue("pos layer included",
               schema.getLayer(annotator.getPosLayerId()).getParentIncludes());
    assertFalse("pos layer peers don't overlap",
               schema.getLayer(annotator.getPosLayerId()).getPeersOverlap());
    assertTrue("pos layer saturated",
               schema.getLayer(annotator.getPosLayerId()).getSaturated());
    assertEquals("model: english-caseless-left3words-distsim.tagger",
                 "english-caseless-left3words-distsim.tagger", annotator.getModel());
    assertTrue("pos layer allows peers", // contractions like "I'll" might have two tags
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
                 "I'll", firstWord.getLabel());
      
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 9, g.all("word").length);
    assertEquals("double check there are no poses: "+Arrays.asList(g.all("pos")),
                 0, g.all("pos").length);
    // run the annotator
    annotator.transform(g);
    List<Annotation> posAnnotations = Arrays.stream(g.all("pos"))
      .collect(Collectors.toList());
    assertEquals("Correct number of tokens "+posAnnotations,
                 13, posAnnotations.size());
    Iterator<Annotation> poses = posAnnotations.iterator();
    assertEquals("I'll", "PRP", poses.next().getLabel());
    assertEquals("I'll", "MD", poses.next().getLabel());
    assertEquals("sing", "VB", poses.next().getLabel());
    assertEquals("and", "CC", poses.next().getLabel());
    assertEquals("w~:w (fragment)", "IN", poses.next().getLabel());
    assertEquals("w~:SYMBOL (fragment)", "SYM", poses.next().getLabel());
    assertEquals("walk", "VB", poses.next().getLabel());
    assertEquals("about (different from default model)",
                 "IN", poses.next().getLabel());
    assertEquals("my", "PRP$", poses.next().getLabel());
    assertEquals("blogging-posting:blogging (OOD - different from default model)",
                 "NN", poses.next().getLabel());
    assertEquals("blogging-posting Hyphen",
                 "HYPH", poses.next().getLabel());
    assertEquals("blogging-posting:posting",
                 "VBG", poses.next().getLabel());
    assertEquals("lazily", "RB", poses.next().getLabel());

    poses = posAnnotations.iterator();
    String[] wordLabels = {
      "I'll", "I'll", // I + 'll
      "sing", "and",
      "w~", "w~", // w + ~
      "walk", "about", "my",
      "blogging-posting", "blogging-posting", "blogging-posting", // blogging + - + posting
      "lazily"
    };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("Tag " + i + " should tag " + wordLabels[i],
                   wordLabels[i], poses.next().first("word").getLabel());
    }
    Annotation[] children = firstWord.all("pos");
    assertEquals("I'll has two tags", 2, children.length);

    // check child anchors are chained
    assertEquals("anchor chaining - I'll/PRP", firstWord.getStartId(), children[0].getStartId());
    assertEquals("anchor chaining - PRP/MD",   children[0].getEndId(), children[1].getStartId());
    assertEquals("anchor chaining - MD/I'll",  children[1].getEndId(), firstWord.getEndId());
    
    // add a word
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("new")
                    .setStart(g.getOrCreateAnchorAt(90)).setEnd(g.getOrCreateAnchorAt(100))
                    .setParent(g.first("turn")));
    
    // change a word
    firstWord.setLabel("John");
    
    // run the annotator again
    annotator.transform(g);
    g.commit(); // have to commit to remove old tags
    List<String> posLabels = Arrays.stream(g.all("pos"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("- 1 + 1 = same number of pos as before: "+posLabels,
                 13, posLabels.size());
    Iterator<String> posLs = posLabels.iterator();
    assertEquals("John (updated POS)", "NNP", posLs.next());
    assertEquals("sing", "NNP", posLs.next());
    assertEquals("and", "CC", posLs.next());
    assertEquals("w~:w", "NNP", posLs.next());
    assertEquals("w~:~", "SYM", posLs.next());
    assertEquals("walk", "NNP", posLs.next());
    assertEquals("about", "IN", posLs.next());
    assertEquals("my", "PRP$", posLs.next());
    assertEquals("blogging-posting:blogging (OOD)", "NN", posLs.next());
    assertEquals("blogging-posting:- (OOD)", "HYPH", posLs.next());
    assertEquals("blogging-posting:posting (OOD)", "VBG", posLs.next());
    assertEquals("lazily", "RB", posLs.next());
    assertEquals("new", "JJ", posLs.next());

    children = firstWord.all("pos");
    assertEquals("John has one tag", 1, children.length);
    assertEquals("John's tag shares start anchors", firstWord.getStart(), children[0].getStart());
    assertEquals("John's tag shares end anchors", firstWord.getEnd(), children[0].getEnd());

  }   

  /** Ensure explicit parameters work, as well as taking tokens from a layer other than
   * the schema word layer. */
  @Test public void setTaskParametersAndUseOrthLayer() throws Exception {
    
    Graph g = graph();
    g.trackChanges();
    // tag the graph as being in New Zealand English
    g.addTag(g, "transcript_language", "en-NZ");
    Schema schema = g.getSchema();
    
    // tag based on orthpraphy tag layer
    schema.addLayer(
      new Layer("orth", "Orthography")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false)
      .setParentId("word"));
    for (Annotation w : g.all("word")) {
      if (!w.getLabel().endsWith("~")) { // skip "w~"
        g.createTag(w, "orth", w.getLabel().toLowerCase());
      } else {
        g.createTag(w, "orth", "");
      }
    } // next token
    
    annotator.setSchema(schema);
    g.setId("setTaskParameters");

    // use specified configuration
    annotator.setTaskParameters(
      "tokenLayerId=orth"
      +"&tokenExclusionPattern="
      +"&chunkLayerId=utterance"       // non-default layer
      +"&transcriptLanguageLayerId="   // no transcript language layer
      +"&phraseLanguageLayerId="       // no phrase language layer
      +"&posLayerId=stanfordpos"       // non-default layer
      +"&model=english-bidirectional-distsim.tagger"); // non-default tagger
    
    assertFalse("double-check utterances are available",
                g.all("utterance").length == 0);
    
    assertEquals("token layer",
                 "orth", annotator.getTokenLayerId());
    assertEquals("token exclusion pattern",
                 "", annotator.getTokenExclusionPattern());
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
    assertEquals("pos layer aligned",
                 Constants.ALIGNMENT_INTERVAL,
                 schema.getLayer(annotator.getPosLayerId()).getAlignment());
    assertEquals("pos layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getPosLayerId()).getType());
    assertTrue("pos layer allows peers",  // contractions like "I'll" might have two tags
                schema.getLayer(annotator.getPosLayerId()).getPeers());
    assertTrue("pos layer included",
               schema.getLayer(annotator.getPosLayerId()).getParentIncludes());
    assertFalse("pos layer peers don't overlap",
               schema.getLayer(annotator.getPosLayerId()).getPeersOverlap());
    assertTrue("pos layer saturated",
               schema.getLayer(annotator.getPosLayerId()).getSaturated());
    assertEquals("model: english-bidirectional-distsim.tagger",
                 "english-bidirectional-distsim.tagger", annotator.getModel());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("2 required layer: "+requiredLayers,
                 2, requiredLayers.size());
    assertTrue("utterance required "+requiredLayers,
               requiredLayers.contains("utterance"));
    assertTrue("orth required "+requiredLayers,
               requiredLayers.contains("orth"));
    String outputLayers[] = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                 1, outputLayers.length);
    assertEquals("output layer correct "+Arrays.asList(outputLayers),
                 "stanfordpos", outputLayers[0]);
    
    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "I'll", firstWord.getLabel());
    
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 9, g.all("word").length);
    assertEquals("double check there are no poses: "+Arrays.asList(g.all("stanfordpos")),
                 0, g.all("stanfordpos").length);
    // run the annotator
    annotator.transform(g);
    List<Annotation> posAnnotations = Arrays.stream(g.all("stanfordpos"))
      .collect(Collectors.toList());
    // for (Annotation pos : posAnnotations) System.out.println(""+pos + " - " + pos.getParent());
    assertEquals("Correct number of tokens "+posAnnotations,
                 11, posAnnotations.size());
    Iterator<Annotation> poses = posAnnotations.iterator();
    assertEquals("I'll", "PRP", poses.next().getLabel());
    assertEquals("I'll", "MD", poses.next().getLabel());
    assertEquals("sing", "VB", poses.next().getLabel());
    assertEquals("and", "CC", poses.next().getLabel());
    assertEquals("walk", "VB", poses.next().getLabel());
    assertEquals("about (different from default model)",
                 "IN", poses.next().getLabel());
    assertEquals("my", "PRP$", poses.next().getLabel());
    assertEquals("blogging-posting:blogging (OOD - different from default model)",
                 "NN", poses.next().getLabel());
    assertEquals("blogging-posting Hyphen",
                 "HYPH", poses.next().getLabel());
    assertEquals("blogging-posting:posting",
                 "VBG", poses.next().getLabel());
    assertEquals("lazily", "RB", poses.next().getLabel());

    poses = posAnnotations.iterator();
    String[] wordLabels = {
      "I'll", "I'll", // I + 'll
      "sing", "and",
      "walk", "about", "my",
      "blogging-posting", "blogging-posting", "blogging-posting", // blogging + - + posting
      "lazily"
    };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("Tag " + i + " should tag " + wordLabels[i],
                   wordLabels[i], poses.next().first("word").getLabel());
    }
    
    assertEquals("I'll has two tags", 2, firstWord.all("stanfordpos").length);
  }   

  /** Ensure tokens can be excluded by regular expression. */
  @Test public void tokenExclusionPattern() throws Exception {
    
    Graph g = graph();
    g.trackChanges();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // use specified configuration
    annotator.setTaskParameters(
      "tokenLayerId=word"
      +"&tokenExclusionPattern=.*~"
      +"&chunkLayerId=utterance"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&phraseLanguageLayerId=lang"
      +"&posLayerId=pos"
      +"&model=english-bidirectional-distsim.tagger"); // non-default tagger
    
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("token exclusion pattern",
                 ".*~", annotator.getTokenExclusionPattern());
    assertEquals("chunk layer",
                 "utterance", annotator.getChunkLayerId());
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
    assertEquals("pos layer aligned",
                 Constants.ALIGNMENT_INTERVAL,
                 schema.getLayer(annotator.getPosLayerId()).getAlignment());
    assertEquals("pos layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getPosLayerId()).getType());
    assertTrue("pos layer peers",
               schema.getLayer(annotator.getPosLayerId()).getPeers());
    assertTrue("pos layer included",
               schema.getLayer(annotator.getPosLayerId()).getParentIncludes());
    assertFalse("pos layer peers don't overlap",
               schema.getLayer(annotator.getPosLayerId()).getPeersOverlap());
    assertTrue("pos layer saturated",
               schema.getLayer(annotator.getPosLayerId()).getSaturated());
    assertEquals("model: english-bidirectional-distsim.tagger",
                 "english-bidirectional-distsim.tagger", annotator.getModel());
    assertTrue("pos layer allows peers", // contractions like "I'll" might have two tags
                schema.getLayer(annotator.getPosLayerId()).getPeers());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("4 required layer: "+requiredLayers,
                 4, requiredLayers.size());
    assertTrue("utterance required "+requiredLayers,
               requiredLayers.contains("utterance"));
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
                 "I'll", firstWord.getLabel());
      
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 9, g.all("word").length);
    assertEquals("double check there are no poses: "+Arrays.asList(g.all("pos")),
                 0, g.all("pos").length);
    // run the annotator
    annotator.transform(g);
    List<Annotation> posAnnotations = Arrays.stream(g.all("pos"))
      .collect(Collectors.toList());
    assertEquals("Correct number of tokens "+posAnnotations,
                 11, posAnnotations.size());
    Iterator<Annotation> poses = posAnnotations.iterator();
    assertEquals("I'll", "PRP", poses.next().getLabel());
    assertEquals("I'll", "MD", poses.next().getLabel());
    assertEquals("sing", "VB", poses.next().getLabel());
    assertEquals("and", "CC", poses.next().getLabel());
    // w~ skipped
    assertEquals("walk", "VB", poses.next().getLabel());
    assertEquals("about (different from default model)",
                 "IN", poses.next().getLabel());
    assertEquals("my", "PRP$", poses.next().getLabel());
    assertEquals("blogging-posting:blogging (OOD - different from default model)",
                 "NN", poses.next().getLabel());
    assertEquals("blogging-posting Hyphen",
                 "HYPH", poses.next().getLabel());
    assertEquals("blogging-posting:posting",
                 "VBG", poses.next().getLabel());
    assertEquals("lazily", "RB", poses.next().getLabel());

    poses = posAnnotations.iterator();
    String[] wordLabels = {
      "I'll", "I'll", // I + 'll
      "sing", "and",
      // w~ skipped
      "walk", "about", "my",
      "blogging-posting", "blogging-posting", "blogging-posting", // blogging + - + posting
      "lazily"
    };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("Tag " + i + " should tag " + wordLabels[i],
                   wordLabels[i], poses.next().first("word").getLabel());
    }
    assertEquals("I'll has two tags", 2, firstWord.all("pos").length);
    
    // add a word
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("new")
                    .setStart(g.getOrCreateAnchorAt(90)).setEnd(g.getOrCreateAnchorAt(100))
                    .setParent(g.first("turn")));
    
  }   

  /** Ensure raw ONZE-style transcription can be used as input - i.e. including pause
   * annotations "." and "-" */
  @Test public void rawWordLayer() throws Exception {
    
    Schema schema = graph().getSchema();
    Graph g = new Graph().setSchema(schema);
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

    g.createSubdivision(turn, schema.getWordLayerId(), "\"The -");
    g.createSubdivision(turn, schema.getWordLayerId(), "quick");
    g.createSubdivision(turn, schema.getWordLayerId(), "brown");
    g.createSubdivision(turn, schema.getWordLayerId(), "fox,\"");
    g.createSubdivision(turn, schema.getWordLayerId(), "he");
    g.createSubdivision(turn, schema.getWordLayerId(), "said,");
    g.createSubdivision(turn, schema.getWordLayerId(), "\"wouldn't");
    g.createSubdivision(turn, schema.getWordLayerId(), "jump .");
    g.createSubdivision(turn, schema.getWordLayerId(), "over");
    g.createSubdivision(turn, schema.getWordLayerId(), "anything!\"");
    
    g.trackChanges();
    
    // use default configuration
    annotator.setSchema(schema);
    annotator.setTaskParameters(null);
    
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("token exclusion pattern",
                 "", annotator.getTokenExclusionPattern());
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
    assertEquals("pos layer aligned",
                 Constants.ALIGNMENT_INTERVAL,
                 schema.getLayer(annotator.getPosLayerId()).getAlignment());
    assertEquals("pos layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getPosLayerId()).getType());
    assertTrue("pos layer peers",
               schema.getLayer(annotator.getPosLayerId()).getPeers());
    assertTrue("pos layer included",
               schema.getLayer(annotator.getPosLayerId()).getParentIncludes());
    assertFalse("pos layer peers don't overlap",
               schema.getLayer(annotator.getPosLayerId()).getPeersOverlap());
    assertTrue("pos layer saturated",
               schema.getLayer(annotator.getPosLayerId()).getSaturated());
    assertEquals("model: english-caseless-left3words-distsim.tagger",
                 "english-caseless-left3words-distsim.tagger", annotator.getModel());
    assertTrue("pos layer allows peers", // contractions like "I'll" might have two tags
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
    
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 10, g.all("word").length);
    assertEquals("double check there are no poses: "+Arrays.asList(g.all("pos")),
                 0, g.all("pos").length);
    
    // run the annotator
    annotator.transform(g);
    List<Annotation> posAnnotations = Arrays.stream(g.all("pos"))
      .collect(Collectors.toList());
    assertEquals("Correct number of tokens "+posAnnotations,
                 20, posAnnotations.size());
    Iterator<Annotation> poses = posAnnotations.iterator();
    assertEquals("\"", "``", poses.next().getLabel());
    assertEquals("the", "DT", poses.next().getLabel());
    assertEquals("-", "HYPH", poses.next().getLabel());
    assertEquals("quick", "JJ", poses.next().getLabel());
    assertEquals("brown", "JJ", poses.next().getLabel());
    assertEquals("fox", "NN", poses.next().getLabel());
    assertEquals(",", ",", poses.next().getLabel());
    assertEquals("\"", "''", poses.next().getLabel());
    assertEquals("he", "PRP", poses.next().getLabel());
    assertEquals("said", "VBD", poses.next().getLabel());
    assertEquals(",", ",", poses.next().getLabel());
    assertEquals("\"", "''", poses.next().getLabel());
    assertEquals("would", "MD", poses.next().getLabel());
    assertEquals("n't", "RB", poses.next().getLabel());
    assertEquals("jump", "VB", poses.next().getLabel());
    assertEquals(".", ".", poses.next().getLabel());
    assertEquals("over", "IN", poses.next().getLabel());
    assertEquals("anything", "NN", poses.next().getLabel());
    assertEquals("!", ".", poses.next().getLabel());
    assertEquals("\"", "''", poses.next().getLabel());

    poses = posAnnotations.iterator();
    String[] wordLabels = {
      "\"The -", "\"The -", "\"The -",
      "quick",
      "brown",
      "fox,\"", "fox,\"", "fox,\"",
      "he",
      "said,", "said,",
      "\"wouldn't", "\"wouldn't", "\"wouldn't",
      "jump .", "jump .",
      "over",
      "anything!\"", "anything!\"", "anything!\""
    };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("Tag " + i + " should tag " + wordLabels[i],
                   wordLabels[i], poses.next().first("word").getLabel());
    }
  }   

  /** Ensure task parameters are validated. */
  @Test public void setInvalidTaskParameters() throws Exception {
    
    try {
      annotator.setTaskParameters(
        // doesn't exist in the schema:
        "tokenLayerId=orthography"
        +"&tokenExclusionPattern="
        +"&chunkLayerId=utterance"
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
        +"&tokenExclusionPattern="
        +"&chunkLayerId=utterance"
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
        +"&tokenExclusionPattern="
        +"&chunkLayerId=utterance"
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
        +"&tokenExclusionPattern="
        +"&chunkLayerId=utterance"
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
        +"&tokenExclusionPattern="
        +"&chunkLayerId=utterance"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&posLayerId=pos"
        // nonexistent model
        +"&model=nonexistent.tagger");
      fail("Should fail with model = nonexistent.tagger");
    } catch (InvalidConfigurationException x) {
    }    
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&tokenExclusionPattern="
        // nonexistent chunk layer
        +"&chunkLayerId=line"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&posLayerId=pos"
        +"&model=english-bidirectional-distsim.tagger");
      fail("Should fail with model = nonexistent.tagger");
    } catch (InvalidConfigurationException x) {
    }    
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        // invalid regular expression
        +"&tokenExclusionPattern=["
        +"&chunkLayerId=utterance"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&posLayerId=pos"
        +"&model=english-bidirectional-distsim.tagger");
      fail("Should fail with invalid regular expression");
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
    
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("I'll")
                    .setStart(g.getOrCreateAnchorAt(10)).setEnd(g.getOrCreateAnchorAt(20))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("sing")
                    .setStart(g.getOrCreateAnchorAt(20)).setEnd(g.getOrCreateAnchorAt(30))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("and")
                    .setStart(g.getOrCreateAnchorAt(30)).setEnd(g.getOrCreateAnchorAt(40))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("w~")
                    .setStart(g.getOrCreateAnchorAt(40)).setEnd(g.getOrCreateAnchorAt(45))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("walk")
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
