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

package nzilbb.annotator.javascript;
	      
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
import nzilbb.annotator.javascript.JavascriptAnnotator;
import nzilbb.sql.derby.DerbySQLTranslator;

public class TestJavascriptAnnotator {

   /* There's no default configuration */
   @Test public void defaultParameters() throws Exception {
      JavascriptAnnotator annotator = new JavascriptAnnotator();

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

   /* Test parameter validation */
   @Test public void setInvalidTaskParameters() throws Exception {
      JavascriptAnnotator annotator = new JavascriptAnnotator();
      
      Graph g = graph();
      Schema schema = g.getSchema();
      annotator.setSchema(schema);

      try {
         annotator.setTaskParameters(
            // doesn't exist in the schema:
            "{\"sourceLayerId\":\"nonexistent\","
            +"\"destinationLayerId\":\"cv\","
            +"\"labelMapping\":\"true\","
            +"\"transcriptLanguageLayerId\":\"\","
            +"\"phraseLanguageLayerId\":null,"
            +"\"language\":\"\","
            +"\"script\":\"replace(/[^aeiou]/g,\\\"C\\\").replace(/[aeiou]/g,\\\"V\\\")\""
            +"}");
         fail("Should fail with nonexistent sourceLayerId");
      } catch (InvalidConfigurationException x) {
      }
      try {
         annotator.setTaskParameters(
            "{\"sourceLayerId\":\"word\","
            +"\"destinationLayerId\":\"cv\","
            +"\"labelMapping\":\"true\","
            // doesn't exist in the schema
            +"\"transcriptLanguageLayerId\":\"nonexistent\","
            +"\"phraseLanguageLayerId\":null,"
            +"\"language\":\"\","
            +"\"script\":\"replace(/[^aeiou]/g,\\\"C\\\").replace(/[aeiou]/g,\\\"V\\\")\""
            +"}");
         fail("Should fail with nonexistent transcriptLanguageLayerId");
      } catch (InvalidConfigurationException x) {
      }
      try {
         annotator.setTaskParameters(
            "{\"sourceLayerId\":\"word\","
            +"\"destinationLayerId\":\"cv\","
            +"\"labelMapping\":\"true\","
            +"\"transcriptLanguageLayerId\":\"transcript_language\","
            // doesn't exist in the schema
            +"\"phraseLanguageLayerId\":\"nonexistent\","
            +"\"language\":\"\","
            +"\"script\":\"replace(/[^aeiou]/g,\\\"C\\\").replace(/[aeiou]/g,\\\"V\\\")\""
            +"}");
         fail("Should fail with nonexistent phraseLanguageLayerId");
      } catch (InvalidConfigurationException x) {
      }
      try {
         annotator.setTaskParameters(
            "{\"sourceLayerId\":\"word\","
            +"\"destinationLayerId\":\"cv\","
            +"\"labelMapping\":\"true\","
            +"\"transcriptLanguageLayerId\":\"transcript_language\","
            +"\"phraseLanguageLayerId\":\"lang\","
            // invalid regular expression:
            +"\"language\":\"*\","
            +"\"script\":\"replace(/[^aeiou]/g,\\\"C\\\").replace(/[aeiou]/g,\\\"V\\\")\""
            +"}");
         fail("Should fail with invalid language regular expression");
      } catch (InvalidConfigurationException x) {
      }
      try {
         annotator.setTaskParameters(
            "{\"sourceLayerId\":\"word\","
            +"\"destinationLayerId\":\"word\","
            +"\"labelMapping\":\"true\","
            +"\"transcriptLanguageLayerId\":\"transcript_language\","
            +"\"phraseLanguageLayerId\":\"lang\","
            +"\"language\":\"en.*\","
            +"\"script\":\"replace(/[^aeiou]/g,\\\"C\\\").replace(/[aeiou]/g,\\\"V\\\")\""
            +"}");
         fail("Should fail with destinationLayerId = sourceLayerId");
      } catch (InvalidConfigurationException x) {
      }
   }   
   
   /* Test basic word-tagging use case. */
   @Test public void labelMapping() throws Exception {
      JavascriptAnnotator annotator = new JavascriptAnnotator();
      
      Graph g = graph();
      g.trackChanges();
      Schema schema = g.getSchema();
      annotator.setSchema(schema);
      
      // use specified configuration
      annotator.setTaskParameters(
         "{\"sourceLayerId\":\"word\","
         +"\"destinationLayerId\":\"cv\","
         +"\"labelMapping\":\"true\","
         +"\"transcriptLanguageLayerId\":\"\"," // no transcript language layer
         +"\"phraseLanguageLayerId\":null,"     // null phrase language layer
         +"\"language\":\"\","
         // non-voweles are C (exclude punctuation)
         +"\"script\":\"replace(/[^aeiou',.?!]/g,\\\"C\\\")"
         // vowels are V
         +".replace(/[aeiou]/g,\\\"V\\\")"
         // anything leftover (punctuation) is removed
         +".replace(/[^CV]/g,\\\"\\\")\""
         +"}");
      
      assertEquals("source layer",
                   "word", annotator.getSourceLayerId());
      assertNull("transcript language layer",
                 annotator.getTranscriptLanguageLayerId());
      assertNull("phrase language layer",
                 annotator.getPhraseLanguageLayerId());
      assertEquals("destination layer",
                   "cv", annotator.getDestinationLayerId());
      assertNotNull("output layer was created",
                    schema.getLayer("cv"));
      assertEquals("output layer child of word",
                    "word", schema.getLayer("cv").getParentId());
      assertEquals("output layer not aligned",
                   Constants.ALIGNMENT_NONE,
                   schema.getLayer("cv").getAlignment());
      assertEquals("output layer type correct",
                   Constants.TYPE_STRING,
                   schema.getLayer("cv").getType());
      assertFalse("output layer disallows peers",
                  schema.getLayer("cv").getPeers());
      assertEquals("language ok",
                   "", annotator.getLanguage());
      assertTrue("labelMapping ok",
                 annotator.getLabelMapping());
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
                   "cv", outputLayers[0]);

      Annotation firstWord = g.first("word");
      assertEquals("double check the first word is what we think it is: "+firstWord,
                   "ok,", firstWord.getLabel());
      
      assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                   38, g.all("word").length);
      assertEquals("double check there are no annotations: "+Arrays.asList(g.all("cv")),
                   0, g.all("pause").length);
      // run the annotator
      annotator.transform(g);
      Annotation[] words = g.all("word");
      Annotation[] annotations = g.all("cv");
      // last token has no vowels or consonants, so isn't annotated
      assertEquals("One fewer annotations than tokens "+Arrays.asList(annotations),
                   37, annotations.length);
      String [] correct = {
         "VC","VCCV","VCVC","V","CVCV","CCVCV","CVC","V","CVCCVC","CCV","CVCC","V","CCVVC",
         "CVCV",
         "VCC","CCVC","VCVCCVCV","CVCVC","CVCCVCC","VCVC","VCCVC","CV","CVV","CVCC","CV","CVVC",
         "VC",
         "VC",
         "CCVCC","CCVCC",
         "CCVC","CCVCV",
         "CVCVC",
         "CVCVC","CCV",
         "CCVCC","CCVCC" // one fewer because "!" results in an empty label, which is not saved
      };
      for (int i = 0; i < annotations.length; i++) {
         assertEquals("Annotation correct: " + i + " " + words[i],
                      correct[i], annotations[i].getLabel());
         assertEquals("Token correct: " + i,
                      words[i], annotations[i].first("word"));
         assertEquals("Marked for creation: " + i + " " + words[i],
                      Change.Operation.Create, annotations[i].getChange());
      } // next check
   }

   /* Test full script use case. */
   @Test public void fullScript() throws Exception {
      JavascriptAnnotator annotator = new JavascriptAnnotator();
      
      Graph g = graph();
      g.trackChanges();
      Schema schema = g.getSchema();
      annotator.setSchema(schema);

      // create the output layer
      annotator.newLayer("cv", schema.getWordLayerId(), Constants.ALIGNMENT_NONE);
      
      // use specified configuration
      annotator.setTaskParameters(
         "{\"sourceLayerId\":\"word\","
         +"\"destinationLayerId\":\"\","
         +"\"labelMapping\":\"false\","
         +"\"transcriptLanguageLayerId\":\"\"," // no transcript language layer
         +"\"phraseLanguageLayerId\":null,"     // null phrase language layer
         +"\"language\":\"\","
         +"\"script\":\"for each (word in transcript.all(\\\"word\\\"))"
         +" word.createTag(\\\"cv\\\", word.label"
         // non-voweles are C (exclude punctuation)
         +".replace(/[^aeiou',.?!]/g,\\\"C\\\")"
         // vowels are V
         +".replace(/[aeiou]/g,\\\"V\\\")"
         // anything leftover (punctuation) is removed
         +".replace(/[^CV]/g,\\\"\\\"));\""
         +"}");
      
      assertFalse("labelMapping ok",
                 annotator.getLabelMapping());
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
                   "cv", outputLayers[0]);

      assertNotNull("output layer was created",
                    schema.getLayer("cv"));
      assertEquals("output layer child of word",
                    "word", schema.getLayer("cv").getParentId());
      assertEquals("output layer not aligned",
                   Constants.ALIGNMENT_NONE,
                   schema.getLayer("cv").getAlignment());
      assertEquals("output layer type correct",
                   Constants.TYPE_STRING,
                   schema.getLayer("cv").getType());

      Annotation firstWord = g.first("word");
      assertEquals("double check the first word is what we think it is: "+firstWord,
                   "ok,", firstWord.getLabel());
      
      assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                   38, g.all("word").length);
      assertEquals("double check there are no annotations: "+Arrays.asList(g.all("cv")),
                   0, g.all("pause").length);
      // run the annotator
      annotator.transform(g);
      Annotation[] words = g.all("word");
      Annotation[] annotations = g.all("cv");
      assertEquals("Correct number of tokens "+Arrays.asList(annotations),
                   38, annotations.length);
      String [] correct = {
         "VC","VCCV","VCVC","V","CVCV","CCVCV","CVC","V","CVCCVC","CCV","CVCC","V","CCVVC",
         "CVCV",
         "VCC","CCVC","VCVCCVCV","CVCVC","CVCCVCC","VCVC","VCCVC","CV","CVV","CVCC","CV","CVVC",
         "VC",
         "VC",
         "CCVCC","CCVCC",
         "CCVC","CCVCV",
         "CVCVC",
         "CVCVC","CCV",
         "CCVCC","CCVCC",
         "" // script doesn't avoid creating a blank-label annotation, so one is created
      };
      for (int i = 0; i < annotations.length; i++) {
         assertEquals("Annotation correct: " + i + " " + words[i],
                      correct[i], annotations[i].getLabel());
         assertEquals("Token correct: " + i,
                      words[i], annotations[i].first("word"));
         assertEquals("Marked for creation: " + i + " " + words[i],
                      Change.Operation.Create, annotations[i].getChange());
      } // next check
   }

   /* Test detection of input layers. */
   @Test public void inputLayerDetection() throws Exception {
      JavascriptAnnotator annotator = new JavascriptAnnotator();
      
      Graph g = graph();
      Schema schema = g.getSchema();
      annotator.setSchema(schema);

      // create the output layer
      annotator.newLayer("cv", schema.getWordLayerId(), Constants.ALIGNMENT_NONE);

      String[] scripts = {
         "for each (w in transcript.all(\\\"word\\\")) w.createTag('cv', 'test');",
         "for each (w in transcript.list(\\\"word\\\")) w.createTag('cv', 'test');",
         "transcript.first(\\\"word\\\").createTag('cv', 'test');",
         "transcript.last(\\\"word\\\").createTag('cv', 'test');",
         "transcript.my(\\\"word\\\").createTag('cv', 'test');",
         "for each (w in transcript.getAnnotations(\\\"word\\\")) w.createTag('cv', 'test');",
         "for each (w in transcript.annotations(\\\"word\\\")) w.createTag('cv', 'test');",
         "for each (w in transcript.includingAnnotationsOn(\\\"word\\\")) w.createTag('cv', 'test');",
         "for each (w in transcript.includedAnnotationsOn(\\\"word\\\")) w.createTag('cv', 'test');",
         "for each (w in transcript.midpointIncludingAnnotationsOn(\\\"word\\\")) w.createTag('cv', 'test');",
         "for each (w in transcript.tagsOn(\\\"word\\\")) w.createTag('cv', 'test');",
         "for each (w in transcript.getAncestor(\\\"word\\\")) w.createTag('cv', 'test');",
         "for each (w in transcript.overlappingAnnotations(transcript, \\\"word\\\")) w.createTag('cv', 'test');",
      };
         
      for (String script : scripts) {
         
         // double-quotes version
         try {
            annotator.setTaskParameters(
               "{\"sourceLayerId\":\"word\","
               +"\"destinationLayerId\":\"\","
               +"\"labelMapping\":\"false\","
               +"\"transcriptLanguageLayerId\":\"\"," // no transcript language layer
               +"\"phraseLanguageLayerId\":null,"     // null phrase language layer
               +"\"language\":\"\","
               +"\"script\":\""+script+"\""
            +"}");
         } catch (InvalidConfigurationException x) {
            fail("Double-quotes: setTaskParameters: "+script+" : "+x);
         }
         String[] requiredLayers = annotator.getRequiredLayers();
         assertEquals("Double-quotes: 1 required layer: "+script+" : "+requiredLayers,
                      1, requiredLayers.length);
         assertEquals("Double-quotes: word required: "+script+" : "+requiredLayers,
                      "word", requiredLayers[0]);

         // single-quotes version
         script = script.replace("\\\"","'");
         try {
            annotator.setTaskParameters(
               "{\"sourceLayerId\":\"word\","
               +"\"destinationLayerId\":\"\","
               +"\"labelMapping\":\"false\","
               +"\"transcriptLanguageLayerId\":\"\"," // no transcript language layer
               +"\"phraseLanguageLayerId\":null,"     // null phrase language layer
               +"\"language\":\"\","
               +"\"script\":\""+script+"\""
               +"}");
         } catch (InvalidConfigurationException x) {
            fail("Double-quotes: setTaskParameters: "+script+" : "+x);
         }
         
         requiredLayers = annotator.getRequiredLayers();
         assertEquals("Single-quotes: 1 required layer: "+script+" : "+requiredLayers,
                      1, requiredLayers.length);
         assertEquals("Single-quotes: word required: "+script+" : "+requiredLayers,
                      "word", requiredLayers[0]);
         
      } // next script
   }

   /* Test detection of output layers. */
   @Test public void outputLayerDetection() throws Exception {
      JavascriptAnnotator annotator = new JavascriptAnnotator();
      
      Graph g = graph();
      Schema schema = g.getSchema();
      annotator.setSchema(schema);

      String[] scripts = {
         "for each (w in transcript.all('word')) w.createTag(\\\"test\\\", \\\"l\\\");",
         "for each (w in transcript.all('word')) transcript.createTag(w, \\\"test\\\", \\\"l\\\");",
         "for each (w in transcript.all('word')) transcript.addTag(w, \\\"test\\\", \\\"l\\\");",
         "for each (w in transcript.all('word')) transcript.createSpan(w, w, \\\"test\\\", \\\"l\\\");",
         "for each (w in transcript.all('word')) transcript.createSpan(w, w, \\\"test\\\", \\\"l\\\", transcript);",
         "for each (w in transcript.all('word')) transcript.addSpan(w, w, \\\"test\\\", \\\"l\\\", transcript);",
         "for each (w in transcript.all('word')) transcript.createAnnotation(w.getStart(), w.getEnd(), \\\"test\\\", \\\"l\\\");",
         "for each (w in transcript.all('word')) transcript.addAnnotation(w.getStart(), w.getEnd(), \\\"test\\\", \\\"l\\\");",
      };
         
      for (String script : scripts) {
         
         // double-quotes version
         try {
            annotator.setTaskParameters(
               "{\"sourceLayerId\":\"word\","
               +"\"destinationLayerId\":\"\","
               +"\"labelMapping\":\"false\","
               +"\"transcriptLanguageLayerId\":\"\"," // no transcript language layer
               +"\"phraseLanguageLayerId\":null,"     // null phrase language layer
               +"\"language\":\"\","
               +"\"script\":\""+script+"\""
            +"}");
         } catch (InvalidConfigurationException x) {
            fail("Double-quotes: setTaskParameters: "+script+" : "+x);
         }
         String[] outputLayers = annotator.getOutputLayers();
         assertEquals("Double-quotes: 1 required layer: "+script+": "+Arrays.asList(outputLayers),
                      1, outputLayers.length);
         assertEquals("Double-quotes: word required: "+script+" : "+Arrays.asList(outputLayers),
                      "test", outputLayers[0]);

         // single-quotes version
         script = script.replace("\\\"","'");
         try {
            annotator.setTaskParameters(
               "{\"sourceLayerId\":\"word\","
               +"\"destinationLayerId\":\"\","
               +"\"labelMapping\":\"false\","
               +"\"transcriptLanguageLayerId\":\"\"," // no transcript language layer
               +"\"phraseLanguageLayerId\":null,"     // null phrase language layer
               +"\"language\":\"\","
               +"\"script\":\""+script+"\""
               +"}");
         } catch (InvalidConfigurationException x) {
            fail("Double-quotes: setTaskParameters: "+script+" : "+x);
         }
         
         outputLayers = annotator.getOutputLayers();
         assertEquals("Single-quotes: 1 required layer: "+script+": "+Arrays.asList(outputLayers),
                      1, outputLayers.length);
         assertEquals("Single-quotes: word required: "+script+" : "+Arrays.asList(outputLayers),
                      "test", outputLayers[0]);
         
      } // next script

      assertNotNull("default output layer was created",
                    schema.getLayer("test"));
      assertEquals("default output layer child of root",
                   schema.getRoot().getId(), schema.getLayer("test").getParentId());
      assertEquals("default output layer not aligned",
                   Constants.ALIGNMENT_INTERVAL,
                   schema.getLayer("test").getAlignment());
      assertEquals("default output layer type correct",
                   Constants.TYPE_STRING,
                   schema.getLayer("test").getType());
}

   /**
    * Returns a graph with word-spans for annotating.
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
      g.createTag(punchline, "utterance",  "knock knock !");

      try {
         new SimpleTokenizer("utterance", "word").transform(g);
         new DefaultOffsetGenerator().transform(g);
      } catch(TransformationException exception) {
         fail("Could not create test graph: " + exception);
      }
      g.commit();
      
      return g;
   } // end of graph()
   
   public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nzilbb.annotator.javascript.TestJavascriptAnnotator");
   }
}
