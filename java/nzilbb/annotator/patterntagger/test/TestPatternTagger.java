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
import nzilbb.ag.TransformationException;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.util.DefaultOffsetGenerator;
import nzilbb.ag.util.SimpleTokenizer;
import nzilbb.annotator.patterntagger.PatternTagger;
import nzilbb.sql.derby.DerbySQLTranslator;

public class TestPatternTagger {

   /* There's no default configuration */
   @Test public void defaultParameters() throws Exception {
      PatternTagger annotator = new PatternTagger();

      Graph g = pauseGraph();
      Schema schema = g.getSchema();
      annotator.setSchema(schema);
      
      // use default configuration
      try {
         annotator.setTaskParameters(null);
         fail("there is no possible default parameter, should fail with null parameters");
      } catch (InvalidConfigurationException x) {
      }
   }   

   /* Test parameter validation */
   @Test public void setInvalidTaskParameters() throws Exception {
      PatternTagger annotator = new PatternTagger();
      
      Graph g = pauseGraph();
      Schema schema = g.getSchema();
      annotator.setSchema(schema);

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
            +"\"phraseLanguageLayerId\":\"language\","
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
            +"\"phraseLanguageLayerId\":\"lang\","
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

   /** 
    * Test partial JSON parameters. 
    */ 
   @Test public void partialParameters() throws Exception {
      PatternTagger annotator = new PatternTagger();
      
      Graph g = pauseGraph();
      Schema schema = g.getSchema();
      annotator.setSchema(schema);
      
      // use specified configuration
      annotator.setTaskParameters(
         "{\"sourceLayerId\":\"word\","
         +"\"destinationLayerId\":\"pause\","
         +"\"mappings\":[]}");
      
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
                   null, annotator.getLanguage());
      assertFalse("deleteOnNoMatch ok",
                  annotator.getDeleteOnNoMatch());
      Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
         .collect(Collectors.toSet());
      assertEquals("2 required layer: "+requiredLayers,
                   2, requiredLayers.size());
      assertTrue("word required "+requiredLayers,
                 requiredLayers.contains("word"));
      assertTrue("turn required "+requiredLayers,
                 requiredLayers.contains("turn"));
      String outputLayers[] = annotator.getOutputLayers();
      assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                   1, outputLayers.length);
      assertEquals("output layer correct "+Arrays.asList(outputLayers),
                   "pause", outputLayers[0]);
      
   }
   
   /* Test basic word-tagging use case. */
   @Test public void basicTagging() throws Exception {
      PatternTagger annotator = new PatternTagger();
      
      Graph g = pauseGraph();
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
      assertFalse("output layer disallows peers",
                  schema.getLayer(annotator.getDestinationLayerId()).getPeers());
      assertEquals("language ok",
                   "", annotator.getLanguage());
      assertFalse("deleteOnNoMatch ok",
                 annotator.getDeleteOnNoMatch());
      Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
         .collect(Collectors.toSet());
      assertEquals("2 required layer: "+requiredLayers,
                   2, requiredLayers.size());
      assertTrue("word required "+requiredLayers,
                 requiredLayers.contains("word"));
      assertTrue("turn required "+requiredLayers,
                 requiredLayers.contains("turn"));
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
   
   /* Test "Copy from layer: "... labelling */
   @Test public void copyFromLayer() throws Exception {
      PatternTagger annotator = new PatternTagger();
      
      Graph g = pauseGraph();
      
      // pre-add a layer for label sources
      Schema schema = g.getSchema();
      schema.addLayer(
         new Layer("index")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setParentId(schema.getWordLayerId())
         .setType(Constants.TYPE_NUMBER));
      int i = 0;
      for (Annotation word : g.all("word")) g.createTag(word, "index", ""+(++i));
      
      g.trackChanges();
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
         +" {\"pattern\":\"mm+\",\"label\":\"Copy from layer: index\"}," // one copied
         +" {\"pattern\":\"a+h+\",\"label\":\"ah\"}"                     // one not copied
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
      assertEquals("3 required layer: "+requiredLayers,
                   3, requiredLayers.size());
      assertTrue("turn required "+requiredLayers,
                 requiredLayers.contains("turn"));
      assertTrue("word required "+requiredLayers,
                 requiredLayers.contains("word"));
      assertTrue("index required "+requiredLayers,
                 requiredLayers.contains("index"));
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
      assertEquals("Annotation correct", "4", annotations[1].getLabel());
      assertEquals("Token correct", "mmmm", annotations[1].first("word").getLabel());
      assertEquals("Marked for creation",
                   Change.Operation.Create, annotations[1].getChange());

   }
   
   /* Test captured group substitution */
   @Test public void capturedGroupLabelling() throws Exception {
      PatternTagger annotator = new PatternTagger();
      
      Graph g = pauseGraph();
      Schema schema = g.getSchema();     
      g.trackChanges();
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
         +" {\"pattern\":\"(m)m+\",\"label\":\"filled pause starting: $1\"},"
         +" {\"pattern\":\"(a)+(h)+\",\"label\":\"filled pause: $1$2\"}"
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
      assertEquals("2 required layer: "+requiredLayers,
                   2, requiredLayers.size());
      assertTrue("word required "+requiredLayers,
                 requiredLayers.contains("word"));
      assertTrue("turn required "+requiredLayers,
                 requiredLayers.contains("turn"));
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
      assertEquals("Captured groups substituted correctly",
                   "filled pause: ah", annotations[0].getLabel());
      assertEquals("Token correct", "aah", annotations[0].first("word").getLabel());
      assertEquals("Marked for creation",
                   Change.Operation.Create, annotations[0].getChange());
      assertEquals("Captured group substituted correctly",
                   "filled pause starting: m", annotations[1].getLabel());
      assertEquals("Token correct", "mmmm", annotations[1].first("word").getLabel());
      assertEquals("Marked for creation",
                   Change.Operation.Create, annotations[1].getChange());

   }
   
   /* Test deleteOnNoMatch setting */
   @Test public void deleteOnNoMatch() throws Exception {
      PatternTagger annotator = new PatternTagger();
      
      Graph g = pauseGraph();
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
      assertEquals("2 required layer: "+requiredLayers,
                   2, requiredLayers.size());
      assertTrue("word required "+requiredLayers,
                 requiredLayers.contains("word"));
      assertTrue("turn required "+requiredLayers,
                 requiredLayers.contains("turn"));
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
      g.createTag(fifthWord, "pause", "manually-added");
      
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
   
   /* Test sensitivity to temporal language tags */
   @Test public void phraseLanguageTags() throws Exception {
      PatternTagger annotator = new PatternTagger();
      
      Graph g = pauseGraph();
      // tag the graph as being in Spanish
      g.createTag(g, "transcript_language", "es");
      
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
      assertEquals("4 required layer: "+requiredLayers,
                   4, requiredLayers.size());
      assertTrue("word required "+requiredLayers,
                 requiredLayers.contains("word"));
      assertTrue("transcript language required "+requiredLayers,
                 requiredLayers.contains("transcript_language"));
      assertTrue("phrase lanaguage required "+requiredLayers,
                 requiredLayers.contains("lang"));
      assertTrue("turn required "+requiredLayers,
                 requiredLayers.contains("turn"));
      String outputLayers[] = annotator.getOutputLayers();
      assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                   1, outputLayers.length);
      assertEquals("output layer correct "+Arrays.asList(outputLayers),
                   "pause", outputLayers[0]);

      Annotation firstWord = g.first("word");
      assertEquals("double check the first word is what we think it is: "+firstWord,
                   "aah", firstWord.getLabel());
      
      // tag the first word as being in New Zealand English
      g.createTag(firstWord, "lang", "en-NZ");
      
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
   
   /* Test sensitivity to transcript language meta-data */
   @Test public void transcriptLanguageTags() throws Exception {
      PatternTagger annotator = new PatternTagger();
      
      Graph g = pauseGraph();
      // tag the graph as being in New Zealand English
      g.createTag(g, "transcript_language", "en-NZ");
      
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
      assertEquals("4 required layer: "+requiredLayers,
                   4, requiredLayers.size());
      assertTrue("word required "+requiredLayers,
                 requiredLayers.contains("word"));
      assertTrue("transcript language required "+requiredLayers,
                 requiredLayers.contains("transcript_language"));
      assertTrue("phrase lanaguage required "+requiredLayers,
                 requiredLayers.contains("lang"));
      assertTrue("turn required "+requiredLayers,
                 requiredLayers.contains("turn"));
      String outputLayers[] = annotator.getOutputLayers();
      assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                   1, outputLayers.length);
      assertEquals("output layer correct "+Arrays.asList(outputLayers),
                   "pause", outputLayers[0]);

      Annotation firstWord = g.first("word");
      assertEquals("double check the first word is what we think it is: "+firstWord,
                   "aah", firstWord.getLabel());
      
      // tag the first word as being in Spanish
      g.createTag(firstWord, "lang", "es");
      
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
    * Returns a graph with filled pauses for tagging.
    * @return The graph for testing with.
    */
   public static Graph pauseGraph() {
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
      g.setId("pauseGraph");
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
   } // end of pauseGraph()
   
   /* Test tagging of multiple words */
   @Test public void phraseTagging() throws Exception {
      PatternTagger annotator = new PatternTagger();
      
      Graph g = spanGraph();
      g.trackChanges();
      Schema schema = g.getSchema();
      
      // pre-add the output layer (there's another test that for its creation)
      schema.addLayer(
         new Layer("story")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true)
         .setParentId(schema.getTurnLayerId())
         .setType(Constants.TYPE_STRING));
      
      annotator.setSchema(schema);
      
      // use specified configuration
      annotator.setTaskParameters(
         "{\"sourceLayerId\":\"word\","
         +"\"transcriptLanguageLayerId\":null," // no transcript language layer
         +"\"phraseLanguageLayerId\":\"\","     // null phrase language layer
         +"\"language\":\"\","
         +"\"deleteOnNoMatch\":\"false\","
         +"\"destinationLayerId\":\"story\","
         +"\"mappings\":["
         // NB the graph token "after" is actually "after," - it should work with partial words
         +" {\"pattern\":\"once upon a time .* happily ever after\",\"label\":\"story\"}"
         +"]}");
      
      assertEquals("token layer",
                   "word", annotator.getSourceLayerId());
      assertNull("transcript language layer",
                 annotator.getTranscriptLanguageLayerId());
      assertNull("phrase language layer",
                 annotator.getPhraseLanguageLayerId());
      assertEquals("output layer",
                   "story", annotator.getDestinationLayerId());
      assertNotNull("output layer was created",
                    schema.getLayer(annotator.getDestinationLayerId()));
      assertEquals("output layer child of turn",
                    "turn", schema.getLayer(annotator.getDestinationLayerId()).getParentId());
      assertEquals("output layer aligned",
                   Constants.ALIGNMENT_INTERVAL,
                   schema.getLayer(annotator.getDestinationLayerId()).getAlignment());
      assertEquals("output layer type correct",
                   Constants.TYPE_STRING,
                   schema.getLayer(annotator.getDestinationLayerId()).getType());
      assertTrue("output layer allows peers",
                 schema.getLayer(annotator.getDestinationLayerId()).getPeers());
      assertEquals("language ok",
                   "", annotator.getLanguage());
      assertFalse("deleteOnNoMatch ok",
                  annotator.getDeleteOnNoMatch());
      Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
         .collect(Collectors.toSet());
      assertEquals("2 required layer: "+requiredLayers,
                   2, requiredLayers.size());
      assertTrue("word required "+requiredLayers,
                 requiredLayers.contains("word"));
      assertTrue("turn required "+requiredLayers,
                 requiredLayers.contains("turn"));
      String outputLayers[] = annotator.getOutputLayers();
      assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                   1, outputLayers.length);
      assertEquals("output layer correct "+Arrays.asList(outputLayers),
                   "story", outputLayers[0]);

      Annotation firstWord = g.first("word");
      assertEquals("double check the first word is what we think it is: "+firstWord,
                   "ok,", firstWord.getLabel());
      
      assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                   37, g.all("word").length);
      assertEquals("double check there are no annotations: "+Arrays.asList(g.all("story")),
                   0, g.all("story").length);
      // run the annotator
      annotator.transform(g);
      Annotation[] annotations = g.all("story");
      assertEquals("Correct number of tokens "+Arrays.asList(annotations),
                   1, annotations.length);
      Annotation tag = annotations[0];
      assertEquals("Annotation correct", "story", tag.getLabel());
      assertEquals("Marked for creation",
                   Change.Operation.Create, tag.getChange());
      
      String tagged = Arrays.stream(tag.all("word")) // stream of Annotation
         .map(annotation->annotation.getLabel()) // stream of String
         .collect(Collectors.toList()) // List of String
         .toString(); // concatenate elements
      
      assertEquals("Correct tokens tagged",
                   "[once, upon, a, time, there, was, a, jester, who, told, a, great, joke, "
                   +"and, then, everyone, lived, happily, ever, after,]", tagged);
   }

   /* Test tagging of multiple words, where labels are generated using captured groups */
   @Test public void phraseTaggingWithCapturedGroups() throws Exception {
      PatternTagger annotator = new PatternTagger();
      
      Graph g = spanGraph();
      g.trackChanges();
      Schema schema = g.getSchema();
      
      // pre-add the output layer (there's another test that for its creation)
      schema.addLayer(
         new Layer("story")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true)
         .setParentId(schema.getTurnLayerId())
         .setType(Constants.TYPE_STRING));
      
      annotator.setSchema(schema);
      
      // use specified configuration
      annotator.setTaskParameters(
         "{\"sourceLayerId\":\"word\","
         +"\"transcriptLanguageLayerId\":null," // no transcript language layer
         +"\"phraseLanguageLayerId\":\"\","     // null phrase language layer
         +"\"language\":\"\","
         +"\"deleteOnNoMatch\":\"false\","
         +"\"destinationLayerId\":\"story\","
         +"\"mappings\":["
         +" {\"pattern\":\"once upon a time (\\\\w+) (\\\\w+) (\\\\w+) (\\\\w+)"
         +" (.+) happily ever after\",\"label\":\"story about $3 $4\"}"
         +"]}");
      
      assertEquals("token layer",
                   "word", annotator.getSourceLayerId());
      assertNull("transcript language layer",
                 annotator.getTranscriptLanguageLayerId());
      assertNull("phrase language layer",
                 annotator.getPhraseLanguageLayerId());
      assertEquals("output layer",
                   "story", annotator.getDestinationLayerId());
      assertNotNull("output layer was created",
                    schema.getLayer(annotator.getDestinationLayerId()));
      assertEquals("output layer child of turn",
                    "turn", schema.getLayer(annotator.getDestinationLayerId()).getParentId());
      assertEquals("output layer aligned",
                   Constants.ALIGNMENT_INTERVAL,
                   schema.getLayer(annotator.getDestinationLayerId()).getAlignment());
      assertEquals("output layer type correct",
                   Constants.TYPE_STRING,
                   schema.getLayer(annotator.getDestinationLayerId()).getType());
      assertTrue("output layer allows peers",
                 schema.getLayer(annotator.getDestinationLayerId()).getPeers());
      assertEquals("language ok",
                   "", annotator.getLanguage());
      assertFalse("deleteOnNoMatch ok",
                  annotator.getDeleteOnNoMatch());
      Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
         .collect(Collectors.toSet());
      assertEquals("2 required layer: "+requiredLayers,
                   2, requiredLayers.size());
      assertTrue("word required "+requiredLayers,
                 requiredLayers.contains("word"));
      assertTrue("turn required "+requiredLayers,
                 requiredLayers.contains("turn"));
      String outputLayers[] = annotator.getOutputLayers();
      assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                   1, outputLayers.length);
      assertEquals("output layer correct "+Arrays.asList(outputLayers),
                   "story", outputLayers[0]);

      Annotation firstWord = g.first("word");
      assertEquals("double check the first word is what we think it is: "+firstWord,
                   "ok,", firstWord.getLabel());
      
      assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                   37, g.all("word").length);
      assertEquals("double check there are no annotations: "+Arrays.asList(g.all("story")),
                   0, g.all("story").length);
      // run the annotator
      annotator.transform(g);
      Annotation[] annotations = g.all("story");
      assertEquals("Correct number of tokens "+Arrays.asList(annotations),
                   1, annotations.length);
      Annotation tag = annotations[0];
      assertEquals("Annotation correct", "story about a jester", tag.getLabel());
      assertEquals("Marked for creation",
                   Change.Operation.Create, tag.getChange());
      
      String tagged = Arrays.stream(tag.all("word")) // stream of Annotation
         .map(annotation->annotation.getLabel()) // stream of String
         .collect(Collectors.toList()) // List of String
         .toString(); // concatenate elements
      
      assertEquals("Correct tokens tagged",
                   "[once, upon, a, time, there, was, a, jester, who, told, a, great, joke, "
                   +"and, then, everyone, lived, happily, ever, after,]", tagged);
   }

   /** Test the creation of a phrase-spanning output layer, and non-wordId input layer. */ 
   @Test public void phraseTaggingNewLayer() throws Exception {
      PatternTagger annotator = new PatternTagger();
      
      Graph g = spanGraph();
      g.trackChanges();
      Schema schema = g.getSchema();
      
      // pre-add a different input layer
      schema.addLayer(
         new Layer("orthography")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setParentId(schema.getWordLayerId())
         .setType(Constants.TYPE_STRING));
      for (Annotation word : g.all("word")) g.createTag(word, "orthography", word.getLabel());
      
      annotator.setSchema(schema);
      
      // use specified configuration
      annotator.setTaskParameters(
         "{\"sourceLayerId\":\"orthography\","
         +"\"destinationLayerId\":\"story\","
         +"\"destinationLayerParentId\":\"turn\","
         +"\"mappings\":["
         +" {\"pattern\":\"once upon a time .* happily ever after\",\"label\":\"story\"}"
         +"]}");
      
      assertEquals("token layer",
                   "orthography", annotator.getSourceLayerId());
      assertNull("transcript language layer",
                 annotator.getTranscriptLanguageLayerId());
      assertNull("phrase language layer",
                 annotator.getPhraseLanguageLayerId());
      assertEquals("output layer",
                   "story", annotator.getDestinationLayerId());
      assertNotNull("output layer was created",
                    schema.getLayer(annotator.getDestinationLayerId()));
      assertEquals("output layer child of turn",
                   "turn", schema.getLayer(annotator.getDestinationLayerId()).getParentId());
      assertEquals("output layer aligned",
                   Constants.ALIGNMENT_INTERVAL,
                   schema.getLayer(annotator.getDestinationLayerId()).getAlignment());
      assertEquals("output layer type correct",
                   Constants.TYPE_STRING,
                   schema.getLayer(annotator.getDestinationLayerId()).getType());
      assertTrue("output layer allows peers",
                 schema.getLayer(annotator.getDestinationLayerId()).getPeers());
      assertEquals("language ok",
                   null, annotator.getLanguage());
      assertFalse("deleteOnNoMatch ok",
                  annotator.getDeleteOnNoMatch());
      Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
         .collect(Collectors.toSet());
      assertEquals("3 required layer: "+requiredLayers,
                   3, requiredLayers.size());
      assertTrue("orthography required "+requiredLayers,
                 requiredLayers.contains("orthography"));
      assertTrue("turn required "+requiredLayers,
                 requiredLayers.contains("turn"));
      assertTrue("word required "+requiredLayers,
                 requiredLayers.contains("word"));
      String outputLayers[] = annotator.getOutputLayers();
      assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                   1, outputLayers.length);
      assertEquals("output layer correct "+Arrays.asList(outputLayers),
                   "story", outputLayers[0]);
      
      Annotation firstWord = g.first("word");
      assertEquals("double check the first word is what we think it is: "+firstWord,
                   "ok,", firstWord.getLabel());
      
      assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                   37, g.all("word").length);
      assertEquals("double check there are no annotations: "+Arrays.asList(g.all("story")),
                   0, g.all("story").length);
      // run the annotator
      annotator.transform(g);
      Annotation[] annotations = g.all("story");
      assertEquals("Correct number of tokens "+Arrays.asList(annotations),
                   1, annotations.length);
      Annotation tag = annotations[0];
      assertEquals("Annotation correct", "story", tag.getLabel());
      assertEquals("Marked for creation",
                   Change.Operation.Create, tag.getChange());
      
      String tagged = Arrays.stream(tag.all("word")) // stream of Annotation
         .map(annotation->annotation.getLabel()) // stream of String
         .collect(Collectors.toList()) // List of String
         .toString(); // concatenate elements
      
      assertEquals("Correct tokens tagged",
                   "[once, upon, a, time, there, was, a, jester, who, told, a, great, joke, "
                   +"and, then, everyone, lived, happily, ever, after,]", tagged);
   }
   
   /* Test tagging across turn boundaries */
   @Test public void spanTagging() throws Exception {
      PatternTagger annotator = new PatternTagger();
      
      Graph g = spanGraph();
      g.trackChanges();
      Schema schema = g.getSchema();
      
      // pre-add the output layer (there's another test that for its creation)
      schema.addLayer(
         new Layer("joke")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true)
         .setParentId(schema.getRoot().getId())
         .setType(Constants.TYPE_STRING));
      
      annotator.setSchema(schema);
      
      // use specified configuration
      annotator.setTaskParameters(
         "{\"sourceLayerId\":\"word\","
         +"\"transcriptLanguageLayerId\":null," // no transcript language layer
         +"\"phraseLanguageLayerId\":\"\","     // null phrase language layer
         +"\"language\":\"\","
         +"\"deleteOnNoMatch\":\"false\","
         +"\"destinationLayerId\":\"joke\","
         +"\"mappings\":["
         +" {\"pattern\":\"knock knock .* who\\\\?\",\"label\":\"joke\"}"
         +"]}");      
      assertEquals("token layer",
                   "word", annotator.getSourceLayerId());
      assertNull("transcript language layer",
                 annotator.getTranscriptLanguageLayerId());
      assertNull("phrase language layer",
                 annotator.getPhraseLanguageLayerId());
      assertEquals("output layer",
                   "joke", annotator.getDestinationLayerId());
      assertNotNull("output layer was created",
                    schema.getLayer(annotator.getDestinationLayerId()));
      assertEquals("output layer child of turn",
                    schema.getRoot().getId(),
                   schema.getLayer(annotator.getDestinationLayerId()).getParentId());
      assertEquals("output layer aligned",
                   Constants.ALIGNMENT_INTERVAL,
                   schema.getLayer(annotator.getDestinationLayerId()).getAlignment());
      assertEquals("output layer type correct",
                   Constants.TYPE_STRING,
                   schema.getLayer(annotator.getDestinationLayerId()).getType());
      assertTrue("output layer allows peers",
                 schema.getLayer(annotator.getDestinationLayerId()).getPeers());
      assertEquals("language ok",
                   "", annotator.getLanguage());
      assertFalse("deleteOnNoMatch ok",
                  annotator.getDeleteOnNoMatch());
      Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
         .collect(Collectors.toSet());
      assertEquals("3 required layer: "+requiredLayers,
                   3, requiredLayers.size());
      assertTrue("word required "+requiredLayers,
                 requiredLayers.contains("word"));
      assertTrue("turn required "+requiredLayers,
                 requiredLayers.contains("turn"));
      assertTrue("graph required "+requiredLayers,
                 requiredLayers.contains("graph"));
      String outputLayers[] = annotator.getOutputLayers();
      assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                   1, outputLayers.length);
      assertEquals("output layer correct "+Arrays.asList(outputLayers),
                   "joke", outputLayers[0]);

      Annotation firstWord = g.first("word");
      assertEquals("double check the first word is what we think it is: "+firstWord,
                   "ok,", firstWord.getLabel());
      
      assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                   37, g.all("word").length);
      assertEquals("double check there are no annotations: "+Arrays.asList(g.all("joke")),
                   0, g.all("joke").length);
      // run the annotator
      annotator.transform(g);
      Annotation[] annotations = g.all("joke");
      assertEquals("Correct number of tokens "+Arrays.asList(annotations),
                   1, annotations.length);
      Annotation tag = annotations[0];
      assertEquals("Annotation correct", "joke", tag.getLabel());
      assertEquals("Marked for creation",
                   Change.Operation.Create, tag.getChange());
      
      String tagged = Arrays.stream(tag.all("word")) // stream of Annotation
         .map(annotation->annotation.getLabel()) // stream of String
         .collect(Collectors.toList()) // List of String
         .toString(); // concatenate elements
      
      assertEquals("Correct tokens tagged",
                   "[knock, knock, who's, there?, dejav, dejav, who?]", tagged);
   }
   
   /**
    * Returns a graph with word-spans for annotating.
    * @return The graph for testing with.
    */
   public static Graph spanGraph() {
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
         new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false)
         .setParentId("turn").setParentIncludes(true));
      // annotate a graph
      Graph g = new Graph()
         .setSchema(schema);
      g.setId("spanGraph");
      Anchor start = g.getOrCreateAnchorAt(1);
      Anchor aStory = g.getOrCreateAnchorAt(10);
      Anchor aKnockKnock = g.getOrCreateAnchorAt(20);
      Anchor aWhosThere = g.getOrCreateAnchorAt(30);
      Anchor aDejav = g.getOrCreateAnchorAt(40);
      Anchor aDejavWho = g.getOrCreateAnchorAt(50);
      Anchor aPunchline = g.getOrCreateAnchorAt(60);
      Anchor end = g.getOrCreateAnchorAt(70);
      Annotation jester = g.addAnnotation(
         new Annotation().setLayerId("participant").setLabel("jester")
         .setStart(start).setEnd(end));
      Annotation king = g.addAnnotation(
         new Annotation().setLayerId("participant").setLabel("king")
         .setStart(start).setEnd(end));
      
      Annotation story = g.addAnnotation(
         new Annotation().setLayerId("turn").setLabel("jester")
         .setStart(start).setEnd(aStory)
         .setParent(jester));
      Annotation ok = g.addAnnotation(
         new Annotation().setLayerId("turn").setLabel("king")
         .setStart(aStory).setEnd(aKnockKnock)
         .setParent(king));
      Annotation knockKnock = g.addAnnotation(
         new Annotation().setLayerId("turn").setLabel("jester")
         .setStart(aKnockKnock).setEnd(aWhosThere)
         .setParent(jester));
      Annotation whosThere = g.addAnnotation(
         new Annotation().setLayerId("turn").setLabel("king")
         .setStart(aWhosThere).setEnd(aDejav)
         .setParent(king));
      Annotation dejav = g.addAnnotation(
         new Annotation().setLayerId("turn").setLabel("jester")
         .setStart(aDejav).setEnd(aDejavWho)
         .setParent(jester));
      Annotation dejavWho = g.addAnnotation(
         new Annotation().setLayerId("turn").setLabel("king")
         .setStart(aDejavWho).setEnd(aPunchline)
         .setParent(king));
      Annotation punchline = g.addAnnotation(
         new Annotation().setLayerId("turn").setLabel("jester")
         .setStart(aPunchline).setEnd(end)
         .setParent(jester));
      g.createTag(story, "utterance",
               "ok, once upon a time there was a jester who told a great joke"
               +" and then everyone lived happily ever after, do you want to hear it?");
      g.createTag(ok, "utterance",         "ok");
      g.createTag(knockKnock, "utterance", "knock knock");
      g.createTag(whosThere, "utterance",  "who's there?");
      g.createTag(dejav, "utterance",      "dejav");
      g.createTag(dejavWho, "utterance",   "dejav who?");
      g.createTag(punchline, "utterance",  "knock knock");

      try {
         new SimpleTokenizer("utterance", "word").transform(g);
         new DefaultOffsetGenerator().transform(g);
      } catch(TransformationException exception) {
         fail("Could not create test graph: " + exception);
      }
      g.commit();
      
      return g;
   } // end of pauseGraph()
   
   public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nzilbb.annotator.patterntagger.test.TestPatternTagger");
   }
}
