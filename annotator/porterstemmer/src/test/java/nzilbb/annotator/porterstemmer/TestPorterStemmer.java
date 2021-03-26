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

package nzilbb.annotator.porterstemmer;
	      
import org.junit.*;
import static org.junit.Assert.*;

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
import nzilbb.annotator.porterstemmer.PorterStemmer;

public class TestPorterStemmer {
   
   @Test public void transform() throws Exception {

      Graph g = graph();
      Schema schema = g.getSchema();
      PorterStemmer annotator = new PorterStemmer();
      annotator.setSchema(schema);
      
      // stem to a new layer
      annotator.setTaskParameters("tokenLayerId=word&stemLayerId=porterstem"); // TODO include language layers
      
      assertEquals("token layer",
                   "word", annotator.getTokenLayerId());
      assertNull("transcript language layer",
                 annotator.getTranscriptLanguageLayerId());
      assertNull("phrase language layer",
                 annotator.getPhraseLanguageLayerId());
      assertEquals("stem layer",
                   "porterstem", annotator.getStemLayerId());
      assertNotNull("stem layer was created",
                    schema.getLayer(annotator.getStemLayerId()));
      assertEquals("stem layer child of word",
                   "word", schema.getLayer(annotator.getStemLayerId()).getParentId());
      assertEquals("stem layer not aligned",
                   Constants.ALIGNMENT_NONE,
                   schema.getLayer(annotator.getStemLayerId()).getAlignment());
      assertEquals("stem layer type correct",
                   Constants.TYPE_STRING,
                   schema.getLayer(annotator.getStemLayerId()).getType());
      String[] layers = annotator.getRequiredLayers();
      assertEquals("1 required layer: "+Arrays.asList(layers),
                   1, layers.length);
      assertEquals("required layer correct "+Arrays.asList(layers),
                   "word", layers[0]);
      layers = annotator.getOutputLayers();
      assertEquals("1 output layer: "+Arrays.asList(layers),
                   1, layers.length);
      assertEquals("output layer correct "+Arrays.asList(layers),
                   "porterstem", layers[0]);
      
      Annotation firstWord = g.first("word");
      assertEquals("double check the first word is what we think it is: "+firstWord,
                   "I", firstWord.getLabel());
      
      assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                   8, g.all("word").length);
      assertEquals("double check there are no stems: "+Arrays.asList(g.all("porterstem")),
                   0, g.all("porterstem").length);
   
      // run the annotator
      annotator.transform(g);
      List<String> stemLabels = Arrays.stream(g.all("porterstem"))
         .map(annotation->annotation.getLabel()).collect(Collectors.toList());
      assertEquals("one stem per token: "+stemLabels,
                   8, stemLabels.size());
      Iterator<String> stems = stemLabels.iterator();
      assertEquals("down-case",
                   "i", stems.next());
      assertEquals("doesn't handle irregular verbs",
                   "sang", stems.next());
      assertEquals("and", stems.next());
      assertEquals("handles regular verbs",
                   "walk", stems.next());
      assertEquals("about", stems.next());
      assertEquals("my", stems.next());
      assertEquals("handles compounds",
                   "blog-post", stems.next());
      assertEquals("lazili", stems.next());

      // add a word
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("new")
                      .setStart(g.getOrCreateAnchorAt(90)).setEnd(g.getOrCreateAnchorAt(100))
                      .setParent(g.first("turn")));

      // change a word
      firstWord.setLabel("we");
      
      // run the annotator again
      annotator.transform(g);
      stemLabels = Arrays.stream(g.all("porterstem"))
         .map(annotation->annotation.getLabel()).collect(Collectors.toList());
      assertEquals("one more stem: "+stemLabels,
                   9, stemLabels.size());
      stems = stemLabels.iterator();
      assertEquals("changed label not re-annotated",
                   "i", stems.next());
      assertEquals("previous stem unchanged", "sang", stems.next());
      assertEquals("previous stem unchanged", "and", stems.next());
      assertEquals("previous stem unchanged", "walk", stems.next());
      assertEquals("previous stem unchanged", "about", stems.next());
      assertEquals("previous stem unchanged", "my", stems.next());
      assertEquals("previous stem unchanged", "blog-post", stems.next());
      assertEquals("previous stem unchanged", "lazili", stems.next());
      assertEquals("new token has stem",
                   "new", stems.next());

   }

   @Test public void defaultParameters() throws Exception {
      
      Graph g = graph();
      // tag the graph as being in New Zealand English
      g.addTag(g, "transcript_language", "en-NZ");
      Schema schema = g.getSchema();
      PorterStemmer annotator = new PorterStemmer();
      annotator.setSchema(schema);
      
      // use default configuration
      annotator.setTaskParameters(null);
      
      assertEquals("token layer",
                   "word", annotator.getTokenLayerId());
      assertEquals("transcript language layer",
                   "transcript_language", annotator.getTranscriptLanguageLayerId());
      assertEquals("phrase language layer",
                   "lang", annotator.getPhraseLanguageLayerId());
      assertEquals("stem layer",
                   "stem", annotator.getStemLayerId());
      assertNotNull("stem layer was created",
                    schema.getLayer(annotator.getStemLayerId()));
      assertEquals("stem layer child of word",
                    "word", schema.getLayer(annotator.getStemLayerId()).getParentId());
      assertEquals("stem layer not aligned",
                   Constants.ALIGNMENT_NONE,
                   schema.getLayer(annotator.getStemLayerId()).getAlignment());
      Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
         .collect(Collectors.toSet());
      assertEquals("3 required layer: "+requiredLayers,
                   3, requiredLayers.size());
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
                   "stem", outputLayers[0]);

      Annotation firstWord = g.first("word");
      assertEquals("double check the first word is what we think it is: "+firstWord,
                   "I", firstWord.getLabel());
      
      assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                   8, g.all("word").length);
      assertEquals("double check there are no stems: "+Arrays.asList(g.all("stem")),
                   0, g.all("stem").length);
      // run the annotator
      annotator.transform(g);
      List<String> stemLabels = Arrays.stream(g.all("stem"))
         .map(annotation->annotation.getLabel()).collect(Collectors.toList());
      assertEquals("one stem per token: "+stemLabels,
                   8, stemLabels.size());
      Iterator<String> stems = stemLabels.iterator();
      assertEquals("down-case",
                   "i", stems.next());
      assertEquals("doesn't handle irregular verbs",
                   "sang", stems.next());
      assertEquals("and", stems.next());
      assertEquals("handles regular verbs",
                   "walk", stems.next());
      assertEquals("about", stems.next());
      assertEquals("my", stems.next());
      assertEquals("handles compounds",
                   "blog-post", stems.next());
      assertEquals("lazili", stems.next());

      // add a word
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("new")
                      .setStart(g.getOrCreateAnchorAt(90)).setEnd(g.getOrCreateAnchorAt(100))
                      .setParent(g.first("turn")));

      // change a word
      firstWord.setLabel("we");
      
      // run the annotator again
      annotator.transform(g);
      stemLabels = Arrays.stream(g.all("stem"))
         .map(annotation->annotation.getLabel()).collect(Collectors.toList());
      assertEquals("one more stem: "+stemLabels,
                   9, stemLabels.size());
      stems = stemLabels.iterator();
      assertEquals("changed label not re-annotated",
                   "i", stems.next());
      assertEquals("previous stem unchanged", "sang", stems.next());
      assertEquals("previous stem unchanged", "and", stems.next());
      assertEquals("previous stem unchanged", "walk", stems.next());
      assertEquals("previous stem unchanged", "about", stems.next());
      assertEquals("previous stem unchanged", "my", stems.next());
      assertEquals("previous stem unchanged", "blog-post", stems.next());
      assertEquals("previous stem unchanged", "lazili", stems.next());
      assertEquals("new token has stem",
                   "new", stems.next());

   }
   
   @Test public void nonEnglish() throws Exception {

      Graph g = graph();

      // tag the graph as being in Te Reo Māori
      g.addTag(g, "transcript_language", "mi");
      
      Schema schema = g.getSchema();
      PorterStemmer annotator = new PorterStemmer();
      annotator.setSchema(schema);
      
      // stem to a new layer
      annotator.setTaskParameters(
         "tokenLayerId=word&stemLayerId=porterstem"
         +"&transcriptLanguageLayerId=transcript_language&phraseLanguageLayerId=lang");
      
      assertEquals("token layer",
                   "word", annotator.getTokenLayerId());
      assertEquals("transcript language layer",
                   "transcript_language", annotator.getTranscriptLanguageLayerId());
      assertEquals("phrase language layer",
                   "lang", annotator.getPhraseLanguageLayerId());
      assertEquals("stem layer",
                   "porterstem", annotator.getStemLayerId());
      assertNotNull("stem layer was created",
                    schema.getLayer(annotator.getStemLayerId()));
      assertEquals("stem layer child of word",
                   "word", schema.getLayer(annotator.getStemLayerId()).getParentId());
      assertEquals("stem layer not aligned",
                   Constants.ALIGNMENT_NONE,
                   schema.getLayer(annotator.getStemLayerId()).getAlignment());
      Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
         .collect(Collectors.toSet());
      assertEquals("3 required layer: "+requiredLayers,
                   3, requiredLayers.size());
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
                   "porterstem", outputLayers[0]);
      
      Annotation firstWord = g.first("word");
      assertEquals("double check the first word is what we think it is: "+firstWord,
                   "I", firstWord.getLabel());
      
      assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                   8, g.all("word").length);
      assertEquals("double check there are no stems: "+Arrays.asList(g.all("porterstem")),
                   0, g.all("porterstem").length);
   
      // run the annotator
      annotator.transform(g);
      List<String> stemLabels = Arrays.stream(g.all("porterstem"))
         .map(annotation->annotation.getLabel()).collect(Collectors.toList());
      assertEquals("no annotations token: "+stemLabels,
                   0, stemLabels.size());
   }

   @Test public void mostlyNonEnglish() throws Exception {

      Graph g = graph();

      // tag the graph as being in Te Reo Māori
      g.addTag(g, "transcript_language", "mi");

      // tag some words as being in NZ English
      g.addAnnotation(new Annotation().setLayerId("lang").setLabel("en-NZ")
                      .setStart(g.getOrCreateAnchorAt(40))
                      // 40."walked".50."about".60."my".70
                      .setEnd(g.getOrCreateAnchorAt(70))
                      .setParent(g.first("turn")));
      
      Schema schema = g.getSchema();
      PorterStemmer annotator = new PorterStemmer();
      annotator.setSchema(schema);
      
      // stem to a new layer
      annotator.setTaskParameters(
         "tokenLayerId=word&stemLayerId=porterstem"
         +"&transcriptLanguageLayerId=transcript_language&phraseLanguageLayerId=lang");
      
      assertEquals("token layer",
                   "word", annotator.getTokenLayerId());
      assertEquals("transcript language layer",
                   "transcript_language", annotator.getTranscriptLanguageLayerId());
      assertEquals("phrase language layer",
                   "lang", annotator.getPhraseLanguageLayerId());
      assertEquals("stem layer",
                   "porterstem", annotator.getStemLayerId());
      assertNotNull("stem layer was created",
                    schema.getLayer(annotator.getStemLayerId()));
      assertEquals("stem layer child of word",
                   "word", schema.getLayer(annotator.getStemLayerId()).getParentId());
      assertEquals("stem layer not aligned",
                   Constants.ALIGNMENT_NONE,
                   schema.getLayer(annotator.getStemLayerId()).getAlignment());
      Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
         .collect(Collectors.toSet());
      assertEquals("3 required layer: "+requiredLayers,
                   3, requiredLayers.size());
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
                   "porterstem", outputLayers[0]);
      
      assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                   8, g.all("word").length);
      assertEquals("double check there are no stems: "+Arrays.asList(g.all("porterstem")),
                   0, g.all("porterstem").length);
   
      // run the annotator
      annotator.transform(g);
      List<String> stemLabels = Arrays.stream(g.all("porterstem"))
         .map(annotation->annotation.getLabel()).collect(Collectors.toList());
      assertEquals("no annotations token: "+stemLabels,
                   3, stemLabels.size());
      Iterator<String> stems = stemLabels.iterator();
      assertEquals("correct start word",
                   "walk", stems.next());
      assertEquals("about", stems.next());
      assertEquals("correct end word",
                   "my", stems.next());
   }

   @Test public void mostlyEnglish() throws Exception {
      
      Graph g = graph();
      
      // tag the graph as being in English - strictly it should be the ISO code,
      // but "English" should also work
      g.addTag(g, "transcript_language", "English"); 

      // tag some words as being in Te Reo Māori
      g.addAnnotation(new Annotation().setLayerId("lang").setLabel("mi")
                      .setStart(g.getOrCreateAnchorAt(40))
                      // 40."walked".50."about".60."my".70
                      .setEnd(g.getOrCreateAnchorAt(70))
                      .setParent(g.first("turn")));

      // tag some as being in NZ English
      g.addAnnotation(new Annotation().setLayerId("lang").setLabel("en-NZ")
                      .setStart(g.getOrCreateAnchorAt(20))
                      // 20."sang".30."and".40
                      .setEnd(g.getOrCreateAnchorAt(40))
                      .setParent(g.first("turn")));
      
      Schema schema = g.getSchema();
      PorterStemmer annotator = new PorterStemmer();
      annotator.setSchema(schema);
      
      // stem to a new layer
      annotator.setTaskParameters(
         "tokenLayerId=word&stemLayerId=porterstem"
         +"&transcriptLanguageLayerId=transcript_language&phraseLanguageLayerId=lang");
      
      assertEquals("token layer",
                   "word", annotator.getTokenLayerId());
      assertEquals("transcript language layer",
                   "transcript_language", annotator.getTranscriptLanguageLayerId());
      assertEquals("phrase language layer",
                   "lang", annotator.getPhraseLanguageLayerId());
      assertEquals("stem layer",
                   "porterstem", annotator.getStemLayerId());
      assertNotNull("stem layer was created",
                    schema.getLayer(annotator.getStemLayerId()));
      assertEquals("stem layer child of word",
                   "word", schema.getLayer(annotator.getStemLayerId()).getParentId());
      assertEquals("stem layer not aligned",
                   Constants.ALIGNMENT_NONE,
                   schema.getLayer(annotator.getStemLayerId()).getAlignment());
      Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
         .collect(Collectors.toSet());
      assertEquals("3 required layer: "+requiredLayers,
                   3, requiredLayers.size());
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
                   "porterstem", outputLayers[0]);
      
      assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                   8, g.all("word").length);
      assertEquals("double check there are no stems: "+Arrays.asList(g.all("porterstem")),
                   0, g.all("porterstem").length);
   
      // run the annotator
      annotator.transform(g);
      List<String> stemLabels = Arrays.stream(g.all("porterstem"))
         .map(annotation->annotation.getLabel()).collect(Collectors.toList());
      assertEquals("no annotations token: "+stemLabels,
                   5, stemLabels.size());
      Iterator<String> stems = stemLabels.iterator();
      assertEquals("first stem correct",
                   "i", stems.next());
      assertEquals("stem for tagged-in-English words",
                   "sang", stems.next());
      assertEquals("stem for tagged-in-English words",
                   "and", stems.next());
      assertEquals("skips non-English phrase",
                   "blog-post", stems.next());
      assertEquals("last stem correct",
                   "lazili", stems.next());
   }

   /**
    * Returns a graph for annotating.
    * @return The graph for testing with.
    */
   public Graph graph() {
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
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("walked")
                      .setStart(g.getOrCreateAnchorAt(40)).setEnd(g.getOrCreateAnchorAt(50))
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
      org.junit.runner.JUnitCore.main("nzilbb.annotator.porterstemmer.TestPorterStemmer");
   }
}
