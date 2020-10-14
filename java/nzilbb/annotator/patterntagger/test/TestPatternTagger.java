//
// Copyright 2020 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.annotator.patterntagger.test;
	      
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
import nzilbb.ag.Change;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.annotator.patterntagger.PatternTagger;
import nzilbb.sql.derby.DerbySQLTranslator;

public class TestPatternTagger {

   public static File dir() throws Exception { 
      URL urlThisClass = TestPatternTagger.class.getResource(
         TestPatternTagger.class.getSimpleName() + ".class");
      File fThisClass = new File(urlThisClass.toURI());
      return fThisClass.getParentFile();
   }
   
   @Test public void defaultParameters() throws Exception {
      PatternTagger annotator = new PatternTagger();

      Graph g = graph();
      Schema schema = g.getSchema();
      annotator.setSchema(schema);
      
      // use default configuration
      try {
         annotator.setTaskParameters(null);
         fail("there is no possible default parameter, should fail with null parameters");
      } catch (InvalidConfigurationException x) {
      }
   }   

   @Test public void setInvalidTaskParameters() throws Exception {
      PatternTagger annotator = new PatternTagger();
      
      try {
         annotator.setTaskParameters(
            // doesn't exist in the schema:
            "{\"sourceLayerId\":\"nonexistent\","
            +"\"transcriptLanguageLayerId\":\"transcript_language\","
            +"\"phraseLanguageLayerId\":\"language\","
            +"\"language\":\"en.*\","
            +"\"deleteOnNoMatch\":\"false\","
            +"\"destinationLayerId\":\"test\","
            +"\"mappings\":["
            +" {\"pattern\":\"mm+\",\"label\":\"mm\"},"
            +" {\"pattern\":\"a+h+\",\"label\":\"ah\"}"
            +"]}");
         fail("Should fail with nonexistent sourceLayerId");
      } catch (InvalidConfigurationException x) {
      }
      try {
         annotator.setTaskParameters(
            "{\"sourceLayerId\":\"word\","
            // doesn't exist in the schema:
            +"\"transcriptLanguageLayerId\":\"nonexistent\","
            +"\"phraseLanguageLayerId\":\"language\","
            +"\"language\":\"en.*\","
            +"\"deleteOnNoMatch\":\"false\","
            +"\"destinationLayerId\":\"test\","
            +"\"mappings\":["
            +" {\"pattern\":\"mm+\",\"label\":\"mm\"},"
            +" {\"pattern\":\"a+h+\",\"label\":\"ah\"}"
            +"]}");
         fail("Should fail with nonexistent transcriptLanguageLayerId");
      } catch (InvalidConfigurationException x) {
      }
      try {
         annotator.setTaskParameters(
            "{\"sourceLayerId\":\"word\","
            +"\"transcriptLanguageLayerId\":\"transcript_language\","
            // doesn't exist in the schema:
            +"\"phraseLanguageLayerId\":\"nonexistent\","
            +"\"language\":\"en.*\","
            +"\"deleteOnNoMatch\":\"false\","
            +"\"destinationLayerId\":\"test\","
            +"\"mappings\":["
            +" {\"pattern\":\"mm+\",\"label\":\"mm\"},"
            +" {\"pattern\":\"a+h+\",\"label\":\"ah\"}"
            +"]}");
         fail("Should fail with nonexistent phraseLanguageLayerId");
      } catch (InvalidConfigurationException x) {
      }
      try {
         annotator.setTaskParameters(
            "{\"sourceLayerId\":\"word\","
            +"\"transcriptLanguageLayerId\":\"transcript_language\","
            +"\"phraseLanguageLayerId\":\"nonexistent\","
            // invalid regular expression:
            +"\"language\":\"*\","
            +"\"deleteOnNoMatch\":\"false\","
            +"\"destinationLayerId\":\"test\","
            +"\"mappings\":["
            +" {\"pattern\":\"mm+\",\"label\":\"mm\"},"
            +" {\"pattern\":\"a+h+\",\"label\":\"ah\"}"
            +"]}");
         fail("Should fail with invalid language regular expression");
      } catch (InvalidConfigurationException x) {
      }
      try {
         annotator.setTaskParameters(
            "{\"sourceLayerId\":\"word\","
            +"\"transcriptLanguageLayerId\":\"transcript_language\","
            +"\"phraseLanguageLayerId\":\"nonexistent\","
            +"\"language\":\"en.*\","
            +"\"deleteOnNoMatch\":\"false\","
            +"\"destinationLayerId\":\"test\","
            +"\"mappings\":["
            // invalid regular expression:
            +" {\"pattern\":\"+\",\"label\":\"mm\"},"
            +" {\"pattern\":\"a+h+\",\"label\":\"ah\"}"
            +"]}");
         fail("Should fail with invalid pattern regular expression");
      } catch (InvalidConfigurationException x) {
      }
      try {
         annotator.setTaskParameters(
            "{\"sourceLayerId\":\"word\","
            +"\"transcriptLanguageLayerId\":\"transcript_language\","
            +"\"phraseLanguageLayerId\":\"language\","
            +"\"language\":\"en.*\","
            +"\"deleteOnNoMatch\":\"false\","
            +"\"destinationLayerId\":\"word\","
            +"\"mappings\":["
            +" {\"pattern\":\"mm+\",\"label\":\"mm\"},"
            +" {\"pattern\":\"a+h+\",\"label\":\"ah\"}"
            +"]}");
         fail("Should fail with destinationLayerId = sourceLayerId");
      } catch (InvalidConfigurationException x) {
      }

   }   

   @Test public void basicTagging() throws Exception {
      PatternTagger annotator = new PatternTagger();
      
      Graph g = graph();
      g.trackChanges();
      Schema schema = g.getSchema();
      annotator.setSchema(schema);
      
      // use specified configuration
      annotator.setTaskParameters(
         "{\"sourceLayerId\":\"word\","
         +"\"transcriptLanguageLayerId\":\"\"," // no transcript language layer
         +"\"phraseLanguageLayerId\":null,"     // null phrase language layer
         +"\"language\":\"\","
         +"\"deleteOnNoMatch\":\"false\","
         +"\"destinationLayerId\":\"pause\","
         +"\"mappings\":["
         +" {\"pattern\":\"mm+\",\"label\":\"mm\"},"
         +" {\"pattern\":\"a+h+\",\"label\":\"ah\"}"
         +"]}");
      
      assertEquals("token layer",
                   "word", annotator.getSourceLayerId());
      assertNull("transcript language layer",
                 annotator.getTranscriptLanguageLayerId());
      assertNull("phrase language layer",
                 annotator.getPhraseLanguageLayerId());
      assertEquals("output layer",
                   "pause", annotator.getDestinationLayerId());
      assertNotNull("output layer was created",
                    schema.getLayer(annotator.getDestinationLayerId()));
      assertEquals("output layer child of word",
                    "word", schema.getLayer(annotator.getDestinationLayerId()).getParentId());
      assertEquals("output layer not aligned",
                   Constants.ALIGNMENT_NONE,
                   schema.getLayer(annotator.getDestinationLayerId()).getAlignment());
      assertEquals("output layer type correct",
                   Constants.TYPE_STRING,
                   schema.getLayer(annotator.getDestinationLayerId()).getType());
      assertFalse("output layer disallows peers (firstVariantOnly=true)",
                  schema.getLayer(annotator.getDestinationLayerId()).getPeers());
      assertEquals("language ok",
                   "", annotator.getLanguage());
      assertFalse("deleteOnNoMatch ok",
                 annotator.getDeleteOnNoMatch());
      Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
         .collect(Collectors.toSet());
      assertEquals("1 required layer: "+requiredLayers,
                   1, requiredLayers.size());
      assertTrue("word required "+requiredLayers,
                 requiredLayers.contains("word"));
      String outputLayers[] = annotator.getOutputLayers();
      assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                   1, outputLayers.length);
      assertEquals("output layer correct "+Arrays.asList(outputLayers),
                   "pause", outputLayers[0]);

      Annotation firstWord = g.first("word");
      assertEquals("double check the first word is what we think it is: "+firstWord,
                   "aah", firstWord.getLabel());
      
      assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                   9, g.all("word").length);
      assertEquals("double check there are no annotations: "+Arrays.asList(g.all("pause")),
                   0, g.all("pause").length);
      // run the annotator
      annotator.transform(g);
      Annotation[] annotations = g.all("pause");
      assertEquals("Correct number of tokens "+Arrays.asList(annotations),
                   2, annotations.length);
      assertEquals("Annotation correct", "ah", annotations[0].getLabel());
      assertEquals("Token correct", "aah", annotations[0].first("word").getLabel());
      assertEquals("Marked for creation",
                   Change.Operation.Create, annotations[0].getChange());
      assertEquals("Annotation correct", "mm", annotations[1].getLabel());
      assertEquals("Token correct", "mmmm", annotations[1].first("word").getLabel());
      assertEquals("Marked for creation",
                   Change.Operation.Create, annotations[1].getChange());

      // add a word
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("ahhh")
                      .setStart(g.getOrCreateAnchorAt(90)).setEnd(g.getOrCreateAnchorAt(100))
                      .setParent(g.first("turn")));

      // commit changes so we can check updating
      g.commit();

      // change a word
      firstWord.setLabel("mm");
      
      // run the annotator again
      annotator.transform(g);
      annotations = g.all("pause");
      assertEquals("one more labels: "+Arrays.asList(annotations),
                   3, annotations.length);
      assertEquals("changed label re-annotated",
                   "mm", annotations[0].getLabel());
      assertEquals("Token correct", "mm", annotations[0].first("word").getLabel());
      assertEquals("Marked for update",
                   Change.Operation.Update, annotations[0].getChange());
      assertEquals("previous pron unchanged",
                   "mm", annotations[1].getLabel());
      assertEquals("Token correct", "mmmm", annotations[1].first("word").getLabel());
      assertEquals("No change registered",
                   Change.Operation.NoChange, annotations[1].getChange());
      assertEquals("new token annotated",
                   "ah", annotations[2].getLabel());
      assertEquals("Token correct", "ahhh", annotations[2].first("word").getLabel());
      assertEquals("Marked for creation",
                   Change.Operation.Create, annotations[2].getChange());

   }
   
   @Test public void deleteOnNoMatch() throws Exception {
      PatternTagger annotator = new PatternTagger();
      
      Graph g = graph();
      g.trackChanges();
      Schema schema = g.getSchema();      
      annotator.setSchema(schema);
      
      // use specified configuration
      annotator.setTaskParameters(
         "{\"sourceLayerId\":\"word\","
         +"\"transcriptLanguageLayerId\":\"\"," // no transcript language layer
         +"\"phraseLanguageLayerId\":null,"     // null phrase language layer
         +"\"language\":\"\","
         // deleteOnNoMatch = false
         +"\"deleteOnNoMatch\":\"false\","
         +"\"destinationLayerId\":\"pause\","
         +"\"mappings\":["
         +" {\"pattern\":\"mm+\",\"label\":\"mm\"},"
         +" {\"pattern\":\"a+h+\",\"label\":\"ah\"}"
         +"]}");
      
      assertEquals("token layer",
                   "word", annotator.getSourceLayerId());
      assertNull("transcript language layer",
                 annotator.getTranscriptLanguageLayerId());
      assertNull("phrase language layer",
                 annotator.getPhraseLanguageLayerId());
      assertEquals("output layer",
                   "pause", annotator.getDestinationLayerId());
      assertNotNull("output layer was created",
                    schema.getLayer(annotator.getDestinationLayerId()));
      assertEquals("output layer child of word",
                    "word", schema.getLayer(annotator.getDestinationLayerId()).getParentId());
      assertEquals("output layer not aligned",
                   Constants.ALIGNMENT_NONE,
                   schema.getLayer(annotator.getDestinationLayerId()).getAlignment());
      assertEquals("output layer type correct",
                   Constants.TYPE_STRING,
                   schema.getLayer(annotator.getDestinationLayerId()).getType());
      assertFalse("output layer disallows peers",
                  schema.getLayer(annotator.getDestinationLayerId()).getPeers());
      assertEquals("language ok",
                   "", annotator.getLanguage());
      assertFalse("deleteOnNoMatch ok",
                 annotator.getDeleteOnNoMatch());
      Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
         .collect(Collectors.toSet());
      assertEquals("1 required layer: "+requiredLayers,
                   1, requiredLayers.size());
      assertTrue("word required "+requiredLayers,
                 requiredLayers.contains("word"));
      String outputLayers[] = annotator.getOutputLayers();
      assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                   1, outputLayers.length);
      assertEquals("output layer correct "+Arrays.asList(outputLayers),
                   "pause", outputLayers[0]);

      Annotation[] words = g.all("word");
      Annotation firstWord = words[0];
      assertEquals("double check the first word is what we think it is: "+firstWord,
                   "aah", firstWord.getLabel());
      Annotation fifthWord = words[4];
      assertEquals("double check the fifth word is what we think it is: "+fifthWord,
                   "ah...", fifthWord.getLabel());
      // tag the fifth word manually
      g.addTag(fifthWord, "pause", "manually-added");
      
      assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                   9, g.all("word").length);
      assertEquals("double check there is one annotation: "+Arrays.asList(g.all("pause")),
                   1, g.all("pause").length);
      // run the annotator
      annotator.transform(g);
      Annotation[] annotations = g.all("pause");
      assertEquals("Correct number of tokens "+Arrays.asList(annotations),
                   3, annotations.length);
      assertEquals("Annotation correct", "ah", annotations[0].getLabel());
      assertEquals("Token correct", "aah", annotations[0].first("word").getLabel());
      assertEquals("Annotation correct", "mm", annotations[1].getLabel());
      assertEquals("Token correct", "mmmm", annotations[1].first("word").getLabel());
      assertEquals("Annotation correct", "manually-added", annotations[2].getLabel());
      assertEquals("Token correct", "ah...", annotations[2].first("word").getLabel());
      assertNotEquals("Manual annotation not marked for destruction",
                      Change.Operation.Destroy, annotations[2].getChange());

      // change deleteOnNoMatch
      annotator.setTaskParameters(
         "{\"sourceLayerId\":\"word\","
         +"\"transcriptLanguageLayerId\":\"\"," // no transcript language layer
         +"\"phraseLanguageLayerId\":null,"     // null phrase language layer
         +"\"language\":\"\","
         // deleteOnNoMatch = true
         +"\"deleteOnNoMatch\":\"true\","
         +"\"destinationLayerId\":\"pause\","
         +"\"mappings\":["
         +" {\"pattern\":\"mm+\",\"label\":\"mm\"},"
         +" {\"pattern\":\"a+h+\",\"label\":\"ah\"}"
         +"]}");
      assertTrue("deleteOnNoMatch changed",
                  annotator.getDeleteOnNoMatch());
      
      annotator.transform(g);
      annotations = g.all("pause");
      assertEquals("Manual annotation still there "+Arrays.asList(annotations),
                   3, annotations.length);
      assertEquals("Annotation correct", "ah", annotations[0].getLabel());
      assertEquals("Token correct", "aah", annotations[0].first("word").getLabel());
      assertEquals("Annotation correct", "mm", annotations[1].getLabel());
      assertEquals("Token correct", "mmmm", annotations[1].first("word").getLabel());
      assertEquals("Manual annotation still there",
                   "manually-added", annotations[2].getLabel());
      assertEquals("Token correct", "ah...", annotations[2].first("word").getLabel());
      assertEquals("Manual annotation marked for destruction",
                   Change.Operation.Destroy, annotations[2].getChange());

      // double check it's deleted in the end
      g.commit();
      annotations = g.all("pause");
      assertEquals("Manual annotation deleted "+Arrays.asList(annotations),
                   2, annotations.length);
   }
   
   @Test public void phraseLanguageTags() throws Exception {
      PatternTagger annotator = new PatternTagger();
      
      Graph g = graph();
      // tag the graph as being in Spanish
      g.addTag(g, "transcript_language", "es");
      
      Schema schema = g.getSchema();
      // pre-add the output layer, but aligned
      schema.addLayer(
         new Layer("pause")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true)
         .setParentId(schema.getWordLayerId())
         .setType(Constants.TYPE_STRING));

      annotator.setSchema(schema);
      
      // use specified configuration
      annotator.setTaskParameters(
         "{\"sourceLayerId\":\"word\","
         +"\"transcriptLanguageLayerId\":\"transcript_language\","
         +"\"phraseLanguageLayerId\":\"lang\","
         +"\"language\":\"en.*\","
         +"\"deleteOnNoMatch\":\"false\","
         +"\"destinationLayerId\":\"pause\","
         +"\"mappings\":["
         +" {\"pattern\":\"mm+\",\"label\":\"mm\"},"
         +" {\"pattern\":\"a+h+\",\"label\":\"ah\"}"
         +"]}");
      
      assertEquals("token layer",
                   "word", annotator.getSourceLayerId());
      assertEquals("transcript language layer",
                   "transcript_language", annotator.getTranscriptLanguageLayerId());
      assertEquals("phrase language layer",
                   "lang", annotator.getPhraseLanguageLayerId());
      assertEquals("output layer",
                   "pause", annotator.getDestinationLayerId());
      assertNotNull("output layer was created",
                    schema.getLayer(annotator.getDestinationLayerId()));
      assertEquals("output layer child of word",
                   "word", schema.getLayer(annotator.getDestinationLayerId()).getParentId());
      assertEquals("output layer still aligned",
                   Constants.ALIGNMENT_INTERVAL,
                   schema.getLayer(annotator.getDestinationLayerId()).getAlignment());
      assertEquals("output layer type correct",
                   Constants.TYPE_STRING,
                   schema.getLayer(annotator.getDestinationLayerId()).getType());
      assertTrue("output layer still allows peers",
                 schema.getLayer(annotator.getDestinationLayerId()).getPeers());
      assertEquals("language ok",
                   "en.*", annotator.getLanguage());
      assertFalse("deleteOnNoMatch ok",
                 annotator.getDeleteOnNoMatch());
      Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
         .collect(Collectors.toSet());
      assertEquals("3 required layer: "+requiredLayers,
                   3, requiredLayers.size());
      assertTrue("word required "+requiredLayers,
                 requiredLayers.contains("word"));
      assertTrue("transcript language required "+requiredLayers,
                 requiredLayers.contains("transcript_language"));
      assertTrue("phrase lanaguage required "+requiredLayers,
                 requiredLayers.contains("lang"));
      String outputLayers[] = annotator.getOutputLayers();
      assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                   1, outputLayers.length);
      assertEquals("output layer correct "+Arrays.asList(outputLayers),
                   "pause", outputLayers[0]);

      Annotation firstWord = g.first("word");
      assertEquals("double check the first word is what we think it is: "+firstWord,
                   "aah", firstWord.getLabel());
      
      // tag the first word as being in New Zealand English
      g.addTag(firstWord, "lang", "en-NZ");
      
      assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                   9, g.all("word").length);
      assertEquals("double check there are no annotations: "+Arrays.asList(g.all("pause")),
                   0, g.all("pause").length);
      // run the annotator
      annotator.transform(g);
      Annotation[] annotations = g.all("pause");
      assertEquals("Correct number of tokens "+Arrays.asList(annotations),
                   1, annotations.length);
      assertEquals("aah not skipped",
                   "ah", annotations[0].getLabel());
      assertEquals("Token correct", "aah", annotations[0].first("word").getLabel());

   }
   
   @Test public void transcriptLanguageTags() throws Exception {
      PatternTagger annotator = new PatternTagger();
      
      Graph g = graph();
      // tag the graph as being in New Zealand English
      g.addTag(g, "transcript_language", "en-NZ");
      
      Schema schema = g.getSchema();
      annotator.setSchema(schema);
      
      // use specified configuration
      annotator.setTaskParameters(
         "{\"sourceLayerId\":\"word\","
         +"\"transcriptLanguageLayerId\":\"transcript_language\","
         +"\"phraseLanguageLayerId\":\"lang\","
         +"\"language\":\"en.*\","
         +"\"deleteOnNoMatch\":\"false\","
         +"\"destinationLayerId\":\"pause\","
         +"\"mappings\":["
         +" {\"pattern\":\"mm+\",\"label\":\"mm\"},"
         +" {\"pattern\":\"a+h+\",\"label\":\"ah\"}"
         +"]}");
      
      assertEquals("token layer",
                   "word", annotator.getSourceLayerId());
      assertEquals("transcript language layer",
                   "transcript_language", annotator.getTranscriptLanguageLayerId());
      assertEquals("phrase language layer",
                   "lang", annotator.getPhraseLanguageLayerId());
      assertEquals("output layer",
                   "pause", annotator.getDestinationLayerId());
      assertNotNull("output layer was created",
                    schema.getLayer(annotator.getDestinationLayerId()));
      assertEquals("output layer child of word",
                   "word", schema.getLayer(annotator.getDestinationLayerId()).getParentId());
      assertEquals("output layer not aligned",
                   Constants.ALIGNMENT_NONE,
                   schema.getLayer(annotator.getDestinationLayerId()).getAlignment());
      assertEquals("output layer type correct",
                   Constants.TYPE_STRING,
                   schema.getLayer(annotator.getDestinationLayerId()).getType());
      assertFalse("output layer disallows peers",
                  schema.getLayer(annotator.getDestinationLayerId()).getPeers());
      assertEquals("language ok",
                   "en.*", annotator.getLanguage());
      assertFalse("deleteOnNoMatch ok",
                 annotator.getDeleteOnNoMatch());
      Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
         .collect(Collectors.toSet());
      assertEquals("3 required layer: "+requiredLayers,
                   3, requiredLayers.size());
      assertTrue("word required "+requiredLayers,
                 requiredLayers.contains("word"));
      assertTrue("transcript language required "+requiredLayers,
                 requiredLayers.contains("transcript_language"));
      assertTrue("phrase lanaguage required "+requiredLayers,
                 requiredLayers.contains("lang"));
      String outputLayers[] = annotator.getOutputLayers();
      assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                   1, outputLayers.length);
      assertEquals("output layer correct "+Arrays.asList(outputLayers),
                   "pause", outputLayers[0]);

      Annotation firstWord = g.first("word");
      assertEquals("double check the first word is what we think it is: "+firstWord,
                   "aah", firstWord.getLabel());
      
      // tag the first word as being in Spanish
      g.addTag(firstWord, "lang", "es");
      
      assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                   9, g.all("word").length);
      assertEquals("double check there are no annotations: "+Arrays.asList(g.all("pause")),
                   0, g.all("pause").length);
      // run the annotator
      annotator.transform(g);
      Annotation[] annotations = g.all("pause");
      assertEquals("Correct number of tokens "+Arrays.asList(annotations),
                   1, annotations.length);
      assertEquals("First aah skipped",
                   "mm", annotations[0].getLabel());
      assertEquals("Token correct", "mmmm", annotations[0].first("word").getLabel());

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
      
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("aah")
                           .setStart(g.getOrCreateAnchorAt(10)).setEnd(g.getOrCreateAnchorAt(20))
                           .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("the")
                      .setStart(g.getOrCreateAnchorAt(20)).setEnd(g.getOrCreateAnchorAt(30))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("quick")
                      .setStart(g.getOrCreateAnchorAt(30)).setEnd(g.getOrCreateAnchorAt(40))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("mmmm")
                      .setStart(g.getOrCreateAnchorAt(40)).setEnd(g.getOrCreateAnchorAt(45))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("ah...")
                      .setStart(g.getOrCreateAnchorAt(45)).setEnd(g.getOrCreateAnchorAt(50))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("brown")
                      .setStart(g.getOrCreateAnchorAt(50)).setEnd(g.getOrCreateAnchorAt(60))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("fox")
                      .setStart(g.getOrCreateAnchorAt(60)).setEnd(g.getOrCreateAnchorAt(70))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("jumps")
                      .setStart(g.getOrCreateAnchorAt(70)).setEnd(g.getOrCreateAnchorAt(80))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("over")
                      .setStart(g.getOrCreateAnchorAt(80)).setEnd(g.getOrCreateAnchorAt(90))
                      .setParent(turn));
      return g;
   } // end of graph()
   
   public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nzilbb.annotator.patterntagger.test.TestPatternTagger");
   }
}
