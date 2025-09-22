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

package nzilbb.annotator.stanfordner;
	      
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
//import nzilbb.annotator.stanfordpos.StanfordPosTagger;

public class TestStanfordNERecognizer {
  
  static StanfordNERecognizer annotator = new StanfordNERecognizer();
  
  @BeforeClass
  public static void install() throws Exception {
    
    System.out.println("Installing classifiers if necessary...");
    
    // find the current directory
    File dir = dir();
    
    // set the schema
    annotator.setSchema(graph().getSchema());
    
    // set the working directory
    annotator.setWorkingDirectory(dir);
    
    // set the annotator configuration to install the classifiers the first time (only)
    annotator.setConfig(annotator.getConfig());
    
    System.out.println("Classifiers installed.");
  }
  
  public static File dir() throws Exception { 
    URL urlThisClass = TestStanfordNERecognizer.class.getResource(
      TestStanfordNERecognizer.class.getSimpleName() + ".class");
    File fThisClass = new File(urlThisClass.toURI());
    return fThisClass.getParentFile();
  }

  // /** Ensures that if there's an existing POS layer with an incorrect configuration, 
  //  * it's corrected. */
  // @Test public void existingLayerCorrected() throws Exception {
    
  //   Graph g = graph();
  //   Schema schema = g.getSchema();    
  //   // add POS layer with incorrect configuration (the one maybe created by LaBB-CAT)
  //   schema.addLayer(
  //     new Layer("pos", "Incorrectly configured POS layer")
  //     .setType(Constants.TYPE_NUMBER)
  //     .setAlignment(Constants.ALIGNMENT_NONE)
  //     .setPeers(false)
  //     .setPeersOverlap(true)
  //     .setSaturated(false)
  //     .setParentId("word"));
  //   annotator.setSchema(schema);
  //   annotator.setTaskParameters(null);
  //   assertEquals("pos layer",
  //                "pos", annotator.getPosLayerId());
  //   assertEquals("pos layer aligned",
  //                Constants.ALIGNMENT_INTERVAL,
  //                schema.getLayer(annotator.getPosLayerId()).getAlignment());
  //   assertEquals("pos layer type correct",
  //                Constants.TYPE_STRING,
  //                schema.getLayer(annotator.getPosLayerId()).getType());
  //   assertTrue("pos layer peers",
  //              schema.getLayer(annotator.getPosLayerId()).getPeers());
  //   assertTrue("pos layer included",
  //              schema.getLayer(annotator.getPosLayerId()).getParentIncludes());
  //   assertFalse("pos layer peers don't overlap",
  //              schema.getLayer(annotator.getPosLayerId()).getPeersOverlap());
  //   assertTrue("pos layer saturated",
  //              schema.getLayer(annotator.getPosLayerId()).getSaturated());
  // }

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
    assertEquals("entity layer",
                 "namedEntity", annotator.getEntityLayerId());
    Layer entityLayer = schema.getLayer(annotator.getEntityLayerId());
    assertNotNull("entity layer was created",
                  entityLayer);
    assertEquals("entity layer child of word",
                 "word", entityLayer.getParentId());
    assertEquals("entity layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 entityLayer.getAlignment());
    assertEquals("entity layer type correct",
                 Constants.TYPE_STRING,
                 entityLayer.getType());
    assertFalse("entity layer peers",
                entityLayer.getPeers());
    assertTrue("entity layer included",
               entityLayer.getParentIncludes());
    assertFalse("entity layer peers don't overlap",
               entityLayer.getPeersOverlap());
    assertTrue("entity layer saturated",
               entityLayer.getSaturated());
    assertEquals("classifier: english.all.3class.distsim.crf.ser.gz",
                 "english.all.3class.distsim.crf.ser.gz", annotator.getClassifier());
    assertFalse("entity layer disallows peers",
                entityLayer.getPeers());
    assertTrue("entity layer has no valid labels defined",
               entityLayer.getValidLabels().size() == 0);
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
                 "namedEntity", outputLayers[0]);
    
    // run the annotator
    // annotator.getStatusObservers().add(status -> System.out.println(status));
    annotator.transform(g);
    List<Annotation> entityAnnotations = Arrays.stream(g.all("namedEntity"))
      .collect(Collectors.toList());
    assertEquals("Correct number of tokens "+entityAnnotations,
                 3, entityAnnotations.size());
    Iterator<Annotation> entities = entityAnnotations.iterator();
    assertEquals("Barack", "PERSON", entities.next().getLabel());
    assertEquals("Obama", "PERSON", entities.next().getLabel());
    assertEquals("Hawaii", "LOCATION", entities.next().getLabel());

    entities = entityAnnotations.iterator();
    String[] wordLabels = { "Barack", "Obama", "Hawaii." };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("Tag " + i + " should tag " + wordLabels[i],
                   wordLabels[i], entities.next().first("word").getLabel());
    }
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

    // use specified configuration
    annotator.setTaskParameters(
      "tokenLayerId=orth"
      +"&tokenExclusionPattern="
      +"&chunkLayerId=utterance"       // non-default layer
      +"&transcriptLanguageLayerId="   // no transcript language layer
      +"&phraseLanguageLayerId="       // no phrase language layer
      +"&entityLayerId=stanfordentity"       // non-default layer
      +"&classifier=english.muc.7class.distsim.crf.ser.gz"); // non-default tagger
        
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
    assertEquals("entity layer",
                 "stanfordentity", annotator.getEntityLayerId());
    assertNotNull("entity layer was created",
                  schema.getLayer(annotator.getEntityLayerId()));
    assertEquals("entity layer child of word",
                 "word", schema.getLayer(annotator.getEntityLayerId()).getParentId());
    assertEquals("entity layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getEntityLayerId()).getAlignment());
    assertEquals("entity layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getEntityLayerId()).getType());
    assertFalse("entity layer disallows peers",
                schema.getLayer(annotator.getEntityLayerId()).getPeers());
    assertTrue("entity layer included",
               schema.getLayer(annotator.getEntityLayerId()).getParentIncludes());
    assertFalse("entity layer peers don't overlap",
               schema.getLayer(annotator.getEntityLayerId()).getPeersOverlap());
    assertTrue("entity layer saturated",
               schema.getLayer(annotator.getEntityLayerId()).getSaturated());
    assertEquals("classifier: english-bidirectional-distsim.tagger",
                 "english.muc.7class.distsim.crf.ser.gz", annotator.getClassifier());
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
                 "stanfordentity", outputLayers[0]);
    
    // run the annotator
    annotator.transform(g);
    List<Annotation> entityAnnotations = Arrays.stream(g.all("stanfordentity"))
      .collect(Collectors.toList());
    assertEquals("Correct number of tokens "+entityAnnotations,
                 4, entityAnnotations.size());
    Iterator<Annotation> entities = entityAnnotations.iterator();
    assertEquals("Barack", "PERSON", entities.next().getLabel());
    assertEquals("Obama", "PERSON", entities.next().getLabel());
    assertEquals("Hawaii", "LOCATION", entities.next().getLabel());
    assertEquals("2008", "DATE", entities.next().getLabel());

    entities = entityAnnotations.iterator();
    String[] wordLabels = { "Barack", "Obama", "Hawaii.", "2008." };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("Tag " + i + " should tag " + wordLabels[i],
                   wordLabels[i], entities.next().first("word").getLabel());
    }
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
      +"&tokenExclusionPattern=.*ii.*" // exclude Hawaii
      +"&chunkLayerId=utterance"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&phraseLanguageLayerId=lang"
      +"&entityLayerId=entity"
      +"&classifier=english.muc.7class.distsim.crf.ser.gz"); // non-default tagger
    
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("token exclusion pattern",
                 ".*ii.*", annotator.getTokenExclusionPattern());
    assertEquals("chunk layer",
                 "utterance", annotator.getChunkLayerId());
    assertEquals("transcript language layer",
                 "transcript_language", annotator.getTranscriptLanguageLayerId());
    assertEquals("phrase language layer",
                 "lang", annotator.getPhraseLanguageLayerId());
    assertEquals("entity layer",
                 "entity", annotator.getEntityLayerId());
    assertNotNull("entity layer was created",
                  schema.getLayer(annotator.getEntityLayerId()));
    assertEquals("entity layer child of word",
                 "word", schema.getLayer(annotator.getEntityLayerId()).getParentId());
    assertEquals("entity layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getEntityLayerId()).getAlignment());
    assertEquals("entity layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getEntityLayerId()).getType());
    assertFalse("entity layer peers",
               schema.getLayer(annotator.getEntityLayerId()).getPeers());
    assertTrue("entity layer included",
               schema.getLayer(annotator.getEntityLayerId()).getParentIncludes());
    assertFalse("entity layer peers don't overlap",
               schema.getLayer(annotator.getEntityLayerId()).getPeersOverlap());
    assertTrue("entity layer saturated",
               schema.getLayer(annotator.getEntityLayerId()).getSaturated());
    assertEquals("classifier: english.muc.7class.distsim.crf.ser.gz",
                 "english.muc.7class.distsim.crf.ser.gz", annotator.getClassifier());
    assertFalse("entity layer disallows peers",
                schema.getLayer(annotator.getEntityLayerId()).getPeers());
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
                 "entity", outputLayers[0]);
    
    // run the annotator
    // annotator.getStatusObservers().add(status -> System.out.println(status));
    annotator.transform(g);
    List<Annotation> entityAnnotations = Arrays.stream(g.all("entity"))
      .collect(Collectors.toList());
    assertEquals("Correct number of tokens "+entityAnnotations,
                 3, entityAnnotations.size());
    Iterator<Annotation> entities = entityAnnotations.iterator();
    assertEquals("Barack", "PERSON", entities.next().getLabel());
    assertEquals("Obama", "PERSON", entities.next().getLabel());
    assertEquals("2008.", "DATE", entities.next().getLabel());

    entities = entityAnnotations.iterator();
    String[] wordLabels = { "Barack", "Obama", "2008." };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("Tag " + i + " should tag " + wordLabels[i],
                   wordLabels[i], entities.next().first("word").getLabel());
    }
  }   

  /** Target specific language - transcript is other language. */
  @Test public void targetLanguageTranscriptMismatch() throws Exception {
    
    Graph g = graph();
    g.trackChanges();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);

    // tag the graph as being in Te Reo Māori
    g.createTag(g, "transcript_language", "mi");
    
    // use specified configuration
    annotator.setTaskParameters(
      "tokenLayerId=word"
      +"&tokenExclusionPattern=.*~"
      +"&targetLanguagePattern=en.*"
      +"&chunkLayerId=utterance"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&phraseLanguageLayerId=lang"
      +"&entityLayerId=entity"
      +"&classifier=english.muc.7class.distsim.crf.ser.gz"); // non-default tagger
    
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("target language pattern",
                 "en.*", annotator.getTargetLanguagePattern());
      
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 13, g.all("word").length);
    assertEquals("double check there are no entityes: "+Arrays.asList(g.all("entity")),
                 0, g.all("entity").length);
    // run the annotator
    annotator.transform(g);
    List<Annotation> entityAnnotations = Arrays.stream(g.all("entity"))
      .collect(Collectors.toList());
    assertEquals("No tags "+entityAnnotations, 0, entityAnnotations.size());
  }   

  /** Target specific language - transcript has no language. */
  @Test public void targetLanguageTranscriptNoLanguage() throws Exception {
    
    Graph g = graph();
    g.trackChanges();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);

    // use specified configuration
    annotator.setTaskParameters(
      "tokenLayerId=word"
      +"&tokenExclusionPattern=.*~"
      +"&targetLanguagePattern=en.*"
      +"&chunkLayerId=utterance"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&phraseLanguageLayerId=lang"
      +"&entityLayerId=entity"
      +"&classifier=english.muc.7class.distsim.crf.ser.gz"); // non-default tagger
    
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("target language pattern",
                 "en.*", annotator.getTargetLanguagePattern());
      
    // run the annotator
    annotator.transform(g);
    List<Annotation> entityAnnotations = Arrays.stream(g.all("entity"))
      .collect(Collectors.toList());
    assertEquals("No tags "+entityAnnotations, 0, entityAnnotations.size());
  }   

  /** Target specific language - transcript is other language, but there are phrase tags. */
  @Test public void targetLanguageTranscriptMismatchWithMatchingPhrases() throws Exception {
    
    Graph g = graph();
    g.trackChanges();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);

    // tag the graph as being in Te Reo Māori
    g.createTag(g, "transcript_language", "mi");

    // tag some words as some other language - "President Barack Obama"
    Annotation es = g.createAnnotation(g.getOrCreateAnchorAt(10), g.getOrCreateAnchorAt(40),
                                         "lang", "es-AR",
                                       g.first("turn"));
    Annotation[] esWords = es.all("word");
    assertEquals("words tagged as Spanish - " + Arrays.asList(esWords),
                 3, esWords.length);
    
    // tag some words as English - "[w~] was born in Hawaii"
    Annotation lang = g.createAnnotation(g.getOrCreateAnchorAt(40), g.getOrCreateAnchorAt(80),
                                         "lang", "en-NZ",
                                         g.first("turn"));
    Annotation[] enWords = lang.all("word");
    assertEquals("words tagged as English - " + Arrays.asList(enWords),
                 5, enWords.length);
    assertEquals("English word has lang tag " + Arrays.asList(enWords[0].all("lang")),
                 lang, enWords[0].first("lang"));

    // use specified configuration
    annotator.setTaskParameters(
      "tokenLayerId=word"
      +"&tokenExclusionPattern=.*~"
      +"&targetLanguagePattern=en.*"
      +"&chunkLayerId=utterance"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&phraseLanguageLayerId=lang"
      +"&entityLayerId=entity"
      +"&classifier=english.muc.7class.distsim.crf.ser.gz"); // non-default tagger
    
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("target language pattern",
                 "en.*", annotator.getTargetLanguagePattern());
      
    // run the annotator
    // annotator.getStatusObservers().add(status -> System.out.println(status));
    annotator.transform(g);
    List<Annotation> entityAnnotations = Arrays.stream(g.all("entity"))
      .collect(Collectors.toList());
    assertEquals("Correct number of tokens "+entityAnnotations,
                 1, entityAnnotations.size());
    Iterator<Annotation> entities = entityAnnotations.iterator();
    assertEquals("Hawaii", "LOCATION", entities.next().getLabel());

    entities = entityAnnotations.iterator();
    String[] wordLabels = { "Hawaii." };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("Tag " + i + " should tag " + wordLabels[i],
                   wordLabels[i], entities.next().first("word").getLabel());
    }
  }   

  /** Target specific language - transcript is right language, 
   * but there are mismatching phrase tags. */
  @Test public void targetLanguageTranscriptMatchWithMismatchingPhrases() throws Exception {
    
    Graph g = graph();
    g.trackChanges();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);

    // tag the graph as being in Te Reo Māori
    g.createTag(g, "transcript_language", "en-US");

    // tag some words as some other language - "President Barck Obama"
    Annotation es = g.createAnnotation(g.getOrCreateAnchorAt(10), g.getOrCreateAnchorAt(40),
                                         "lang", "es-AR",
                                         g.first("turn"));
    Annotation[] esWords = es.all("word");
    assertEquals("words tagged as Spanish - " + Arrays.asList(esWords),
                 3, esWords.length);
    
    // tag some words as Te Reo Māori - "[w~] was born in Hawaii"
    Annotation lang = g.createAnnotation(g.getOrCreateAnchorAt(40), g.getOrCreateAnchorAt(80),
                                         "lang", "mi",
                                         g.first("turn"));
    Annotation[] miWords = lang.all("word");
    assertEquals("words tagged as Te Reo Māori - " + Arrays.asList(miWords),
                 5, miWords.length);
    assertEquals("Te Reo Māori word has lang tag " + Arrays.asList(miWords[0].all("lang")),
                 lang, miWords[0].first("lang"));

    // use specified configuration
    annotator.setTaskParameters(
      "tokenLayerId=word"
      +"&tokenExclusionPattern=.*~"
      +"&targetLanguagePattern=en.*"
      +"&chunkLayerId=utterance"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&phraseLanguageLayerId=lang"
      +"&entityLayerId=entity"
      +"&classifier=english.muc.7class.distsim.crf.ser.gz"); // non-default tagger
    
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("target language pattern",
                 "en.*", annotator.getTargetLanguagePattern());
      
    // run the annotator
    // annotator.getStatusObservers().add(status -> System.out.println(status));
    annotator.transform(g);
    List<Annotation> entityAnnotations = Arrays.stream(g.all("entity"))
      .collect(Collectors.toList());
    assertEquals("Correct number of tokens "+entityAnnotations,
                 1, entityAnnotations.size());
    Iterator<Annotation> entities = entityAnnotations.iterator();
    assertEquals("2008", "DATE", entities.next().getLabel());

    entities = entityAnnotations.iterator();
    String[] wordLabels = { "2008." };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("Tag " + i + " should tag " + wordLabels[i],
                   wordLabels[i], entities.next().first("word").getLabel());
    }
  }   

  /** There are phrases tagged in a particular language, but no targetLanguagePattern. */
  @Test public void phraseLanguageTagsButNoTargetLanguage() throws Exception {
    
    Graph g = graph();
    g.trackChanges();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);

    // tag the graph as being in US English
    g.createTag(g, "transcript_language", "en-US");

    // tag some words as some other language - "President Barakc Obama"
    Annotation es = g.createAnnotation(g.getOrCreateAnchorAt(10), g.getOrCreateAnchorAt(40),
                                       "lang", "es-AR",
                                       g.first("turn"));
    Annotation[] esWords = es.all("word");
    assertEquals("words tagged as Spanish - " + Arrays.asList(esWords),
                 3, esWords.length);
    
    // tag some words as Te Reo Māori - "[w~] was born in Hawaii"
    Annotation lang = g.createAnnotation(g.getOrCreateAnchorAt(40), g.getOrCreateAnchorAt(80),
                                         "lang", "mi",
                                         g.first("turn"));
    Annotation[] miWords = lang.all("word");
    assertEquals("words tagged as Te Reo Māori - " + Arrays.asList(miWords),
                 5, miWords.length);
    assertEquals("Te Reo Māori word has lang tag " + Arrays.asList(miWords[0].all("lang")),
                 lang, miWords[0].first("lang"));
    
    // use specified configuration
    annotator.setTaskParameters(
      "tokenLayerId=word"
      +"&tokenExclusionPattern=.*~"
      +"&targetLanguagePattern=" // no target language
      +"&chunkLayerId=utterance"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&phraseLanguageLayerId=lang"
      +"&entityLayerId=entity"
      +"&classifier=english.muc.7class.distsim.crf.ser.gz"); // non-default tagger
    
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertNull("no target language pattern",
               annotator.getTargetLanguagePattern());
      
    // run the annotator
    annotator.transform(g);
    List<Annotation> entityAnnotations = Arrays.stream(g.all("entity"))
      .collect(Collectors.toList());
    assertEquals("Correct number of tokens "+entityAnnotations,
                 4, entityAnnotations.size());
    Iterator<Annotation> entities = entityAnnotations.iterator();
    assertEquals("Barack", "PERSON", entities.next().getLabel());
    assertEquals("Obama", "PERSON", entities.next().getLabel());
    assertEquals("Hawaii", "LOCATION", entities.next().getLabel());
    assertEquals("2008", "DATE", entities.next().getLabel());

    entities = entityAnnotations.iterator();
    String[] wordLabels = { "Barack", "Obama", "Hawaii.", "2008." };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("Tag " + i + " should tag " + wordLabels[i],
                   wordLabels[i], entities.next().first("word").getLabel());
    }
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

    g.createSubdivision(turn, schema.getWordLayerId(), "\"President -");
    g.createSubdivision(turn, schema.getWordLayerId(), "Barack");
    g.createSubdivision(turn, schema.getWordLayerId(), "Obama");
    g.createSubdivision(turn, schema.getWordLayerId(), "w~\"");
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
    assertEquals("entity layer",
                 "namedEntity", annotator.getEntityLayerId());
    assertNotNull("entity layer was created",
                  schema.getLayer(annotator.getEntityLayerId()));
    assertEquals("entity layer child of word",
                 "word", schema.getLayer(annotator.getEntityLayerId()).getParentId());
    assertEquals("entity layer aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getEntityLayerId()).getAlignment());
    assertEquals("entity layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getEntityLayerId()).getType());
    assertFalse("entity layer disallows peers",
                schema.getLayer(annotator.getEntityLayerId()).getPeers());
    assertTrue("entity layer included",
               schema.getLayer(annotator.getEntityLayerId()).getParentIncludes());
    assertFalse("entity layer peers don't overlap",
               schema.getLayer(annotator.getEntityLayerId()).getPeersOverlap());
    assertTrue("entity layer saturated",
               schema.getLayer(annotator.getEntityLayerId()).getSaturated());
    assertEquals("classifier: english.all.3class.distsim.crf.ser.gz",
                 "english.all.3class.distsim.crf.ser.gz", annotator.getClassifier());
    assertFalse("entity layer disallows peers",
                schema.getLayer(annotator.getEntityLayerId()).getPeers());
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
                 "namedEntity", outputLayers[0]);
    
    // run the annotator
    annotator.transform(g);
    List<Annotation> entityAnnotations = Arrays.stream(g.all("namedEntity"))
      .collect(Collectors.toList());
    assertEquals("Correct number of tokens "+entityAnnotations,
                 2, entityAnnotations.size());
    Iterator<Annotation> entities = entityAnnotations.iterator();
    assertEquals("Barack", "PERSON", entities.next().getLabel());
    assertEquals("Obama", "PERSON", entities.next().getLabel());

    entities = entityAnnotations.iterator();
    String[] wordLabels = { "Barack", "Obama" };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("Tag " + i + " should tag " + wordLabels[i],
                   wordLabels[i], entities.next().first("word").getLabel());
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
        +"&entityLayerId=stanfordentity"
        +"&classifier=english.all.3class.distsim.crf.ser.gz");
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
        +"&entityLayerId=stanfordentity"
        +"&classifier=english.all.3class.distsim.crf.ser.gz");
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
        +"&entityLayerId=stanfordentity"
        +"&classifier=english.all.3class.distsim.crf.ser.gz");
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
        +"&entityLayerId=word"
        +"&classifier=english.all.3class.distsim.crf.ser.gz");
      fail("Should fail with entityLayerId = word");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&tokenExclusionPattern="
        +"&chunkLayerId=utterance"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&entityLayerId=entity"
        // nonexistent classifier
        +"&classifier=nonexistent.crf.ser.gz");
      fail("Should fail with classifier = nonexistent.tagger");
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
        +"&entityLayerId=entity"
        +"&classifier=english.all.3class.distsim.crf.ser.gz");
      fail("Should fail with classifier = nonexistent.tagger");
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
        +"&entityLayerId=entity"
        +"&classifier=english.all.3class.distsim.crf.ser.gz");
      fail("Should fail with invalid token exclusion regular expression");
    } catch (InvalidConfigurationException x) {
    }    
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        // invalid regular expression
        +"&targetLanguagePattern=["
        +"&chunkLayerId=utterance"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&entityLayerId=entity"
        +"&classifier=english.all.3class.distsim.crf.ser.gz");
      fail("Should fail with invalid target language regular expression");
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
    g.setId("TestStanfordNERecognizer");
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
    
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("President")
                    .setStart(g.getOrCreateAnchorAt(10)).setEnd(g.getOrCreateAnchorAt(20))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("Barack")
                    .setStart(g.getOrCreateAnchorAt(20)).setEnd(g.getOrCreateAnchorAt(30))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("Obama")
                    .setStart(g.getOrCreateAnchorAt(30)).setEnd(g.getOrCreateAnchorAt(40))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("w~")
                    .setStart(g.getOrCreateAnchorAt(40)).setEnd(g.getOrCreateAnchorAt(45))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("was")
                    .setStart(g.getOrCreateAnchorAt(45)).setEnd(g.getOrCreateAnchorAt(50))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("born")
                    .setStart(g.getOrCreateAnchorAt(50)).setEnd(g.getOrCreateAnchorAt(60))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("in")
                    .setStart(g.getOrCreateAnchorAt(60)).setEnd(g.getOrCreateAnchorAt(70))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("Hawaii.")
                    .setStart(g.getOrCreateAnchorAt(70)).setEnd(g.getOrCreateAnchorAt(80))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("He")
                    .setStart(g.getOrCreateAnchorAt(80)).setEnd(g.getOrCreateAnchorAt(90))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("was")
                    .setStart(g.getOrCreateAnchorAt(80)).setEnd(g.getOrCreateAnchorAt(90))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("elected")
                    .setStart(g.getOrCreateAnchorAt(90)).setEnd(g.getOrCreateAnchorAt(92))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("in")
                    .setStart(g.getOrCreateAnchorAt(92)).setEnd(g.getOrCreateAnchorAt(94))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("2008.")
                    .setStart(g.getOrCreateAnchorAt(94)).setEnd(g.getOrCreateAnchorAt(96))
                    .setParent(turn));
    return g;
  } // end of graph()
  
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.annotator.stanfordner.TestStanfordNERecognizer");
  }
}
