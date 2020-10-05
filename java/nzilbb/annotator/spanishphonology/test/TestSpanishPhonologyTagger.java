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

package nzilbb.annotator.spanishphonology.test;
	      
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
import nzilbb.annotator.spanishphonology.SpanishPhonologyTagger;

public class TestSpanishPhonologyTagger {
   
   @Test public void transform() throws Exception {
      
      Graph g = graph();
      Schema schema = g.getSchema();
      SpanishPhonologyTagger annotator = new SpanishPhonologyTagger();
      annotator.setSchema(schema);
      
      // stem to a new layer
      annotator.setTaskParameters("tokenLayerId=word&phonemeLayerId=pronunciation&locale=es_MX");
      
      assertEquals("token layer",
                   "word", annotator.getTokenLayerId());
      assertNull("transcript language layer",
                 annotator.getTranscriptLanguageLayerId());
      assertNull("phrase language layer",
                 annotator.getPhraseLanguageLayerId());
      assertEquals("phonemes layer",
                   "pronunciation", annotator.getPhonemeLayerId());
      assertNotNull("stem layer was created",
                    schema.getLayer(annotator.getPhonemeLayerId()));
      assertEquals("phoneme layer child of word",
                   "word", schema.getLayer(annotator.getPhonemeLayerId()).getParentId());
      assertEquals("phoneme layer not aligned",
                   Constants.ALIGNMENT_NONE,
                   schema.getLayer(annotator.getPhonemeLayerId()).getAlignment());
      assertEquals("phonemes layer type correct",
                   Constants.TYPE_IPA,
                   schema.getLayer(annotator.getPhonemeLayerId()).getType());
      String[] layers = annotator.getRequiredLayers();
      assertEquals("1 required layer: "+Arrays.asList(layers),
                   1, layers.length);
      assertEquals("required layer correct "+Arrays.asList(layers),
                   "word", layers[0]);
      layers = annotator.getOutputLayers();
      assertEquals("1 output layer: "+Arrays.asList(layers),
                   1, layers.length);
      assertEquals("output layer correct "+Arrays.asList(layers),
                   "pronunciation", layers[0]);
      
      Annotation firstWord = g.first("word");
      assertEquals("double check the first word is what we think it is: "+firstWord,
                   "A", firstWord.getLabel());
      
      assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                   10, g.all("word").length);
      assertEquals("double check there are no phonemes: "+Arrays.asList(g.all("pronunciation")),
                   0, g.all("pronunciation").length);
   
      // run the annotator
      annotator.transform(g);
      List<String> phonemeLabels = Arrays.stream(g.all("pronunciation"))
         .map(annotation->annotation.getLabel()).collect(Collectors.toList());
      assertEquals("one phoneme per token: "+phonemeLabels,
                   10, phonemeLabels.size());
      Iterator<String> phonemes = phonemeLabels.iterator();
      assertEquals("a", phonemes.next());
      assertEquals("es_MX",
                   "xasinta", phonemes.next());
      assertEquals("le", phonemes.next());
      assertEquals("da", phonemes.next());
      assertEquals("es_MX",
                   "beɾɣwensa", phonemes.next());
      assertEquals("seɲalaɾ", phonemes.next());
      assertEquals("el", phonemes.next());
      assertEquals("deðo", phonemes.next());
      assertEquals("es_MX",
                   "asja", phonemes.next());
      assertEquals("aβaxo", phonemes.next());

      // add a word
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("allá")
                      .setStart(g.getOrCreateAnchorAt(90)).setEnd(g.getOrCreateAnchorAt(100))
                      .setParent(g.first("turn")));

      // change a word
      firstWord.setLabel("para");
      
      // run the annotator again
      annotator.transform(g);
      phonemeLabels = Arrays.stream(g.all("pronunciation"))
         .map(annotation->annotation.getLabel()).collect(Collectors.toList());
      assertEquals("one more phoneme: "+phonemeLabels,
                   11, phonemeLabels.size());
      phonemes = phonemeLabels.iterator();
      assertEquals("changed label not re-annotated",
                   "a", phonemes.next());
      assertEquals("previous phoneme unchanged", "xasinta", phonemes.next());
      assertEquals("previous phoneme unchanged", "le", phonemes.next());
      assertEquals("previous phoneme unchanged", "da", phonemes.next());
      assertEquals("previous phoneme unchanged", "beɾɣwensa", phonemes.next());
      assertEquals("previous phoneme unchanged", "seɲalaɾ", phonemes.next());
      assertEquals("previous phoneme unchanged", "el", phonemes.next());
      assertEquals("previous phoneme unchanged", "deðo", phonemes.next());
      assertEquals("previous phoneme unchanged", "asja", phonemes.next());
      assertEquals("previous phoneme unchanged", "aβaxo", phonemes.next());
      assertEquals("new token has phoneme",
                   "aʎa", phonemes.next());

   }
   
   @Test public void defaultParameters() throws Exception {
      
      Graph g = graph();
      // tag the graph as being in Spanish Spanis
      g.addTag(g, "transcript_language", "es-ES");
      Schema schema = g.getSchema();
      SpanishPhonologyTagger annotator = new SpanishPhonologyTagger();
      annotator.setSchema(schema);
      
      // use default configuration
      annotator.setTaskParameters(null);
      
      assertEquals("token layer",
                   "word", annotator.getTokenLayerId());
      assertEquals("transcript language layer",
                   "transcript_language", annotator.getTranscriptLanguageLayerId());
      assertEquals("phrase language layer",
                   "lang", annotator.getPhraseLanguageLayerId());
      assertEquals("phoneme layer",
                   "phonemes", annotator.getPhonemeLayerId());
      assertNotNull("phoneme layer was created",
                    schema.getLayer(annotator.getPhonemeLayerId()));
      assertEquals("phoneme layer child of word",
                    "word", schema.getLayer(annotator.getPhonemeLayerId()).getParentId());
      assertEquals("phoneme layer not aligned",
                   Constants.ALIGNMENT_NONE,
                   schema.getLayer(annotator.getPhonemeLayerId()).getAlignment());
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
                   "phonemes", outputLayers[0]);

      Annotation firstWord = g.first("word");
      assertEquals("double check the first word is what we think it is: "+firstWord,
                   "A", firstWord.getLabel());
      
      assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                   10, g.all("word").length);
      assertEquals("double check there are no phonemes: "+Arrays.asList(g.all("phonemes")),
                   0, g.all("phonemes").length);
      // run the annotator
      annotator.transform(g);
      List<String> phonemeLabels = Arrays.stream(g.all("phonemes"))
         .map(annotation->annotation.getLabel()).collect(Collectors.toList());
      assertEquals("one phoneme per token: "+phonemeLabels,
                   10, phonemeLabels.size());
      Iterator<String> phonemes = phonemeLabels.iterator();
      assertEquals("one phoneme per token: "+phonemeLabels,
                   10, phonemeLabels.size());
      assertEquals("a", phonemes.next());
      assertEquals("es_ES",
                   "xaθinta", phonemes.next());
      assertEquals("le", phonemes.next());
      assertEquals("da", phonemes.next());
      assertEquals("es_ES",
                   "beɾɣwenθa", phonemes.next());
      assertEquals("seɲalaɾ", phonemes.next());
      assertEquals("el", phonemes.next());
      assertEquals("deðo", phonemes.next());
      assertEquals("es_ES",
                   "aθja", phonemes.next());
      assertEquals("aβaxo", phonemes.next());
      
   }
   
   @Test public void nonSpanish() throws Exception {
      
      Graph g = graph();
      
      // tag the graph as being in Engligh
      g.addTag(g, "transcript_language", "en");
      
      Schema schema = g.getSchema();
      SpanishPhonologyTagger annotator = new SpanishPhonologyTagger();
      annotator.setSchema(schema);
      
      // phoneme to a new layer
      annotator.setTaskParameters(
         "tokenLayerId=word&phonemeLayerId=phonemes&locale=es_MX"
         +"&transcriptLanguageLayerId=transcript_language&phraseLanguageLayerId=lang");
      
      assertEquals("token layer",
                   "word", annotator.getTokenLayerId());
      assertEquals("transcript language layer",
                   "transcript_language", annotator.getTranscriptLanguageLayerId());
      assertEquals("phrase language layer",
                   "lang", annotator.getPhraseLanguageLayerId());
      assertEquals("phoneme xlayer",
                   "phonemes", annotator.getPhonemeLayerId());
      assertNotNull("phoneme layer was created",
                    schema.getLayer(annotator.getPhonemeLayerId()));
      assertEquals("phoneme layer child of word",
                   "word", schema.getLayer(annotator.getPhonemeLayerId()).getParentId());
      assertEquals("phoneme layer not aligned",
                   Constants.ALIGNMENT_NONE,
                   schema.getLayer(annotator.getPhonemeLayerId()).getAlignment());
      Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
         .collect(Collectors.toSet());
      assertEquals("3 required layers: "+requiredLayers,
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
                   "phonemes", outputLayers[0]);
      
      Annotation firstWord = g.first("word");
      assertEquals("double check the first word is what we think it is: "+firstWord,
                   "A", firstWord.getLabel());
      
      assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                   10, g.all("word").length);
      assertEquals("double check there are no phonemes: "+Arrays.asList(g.all("phonemes")),
                   0, g.all("phonemes").length);
      
      // run the annotator
      annotator.transform(g);
      List<String> phonemeLabels = Arrays.stream(g.all("phonemes"))
         .map(annotation->annotation.getLabel()).collect(Collectors.toList());
      assertEquals("no annotations token: "+phonemeLabels,
                   0, phonemeLabels.size());
   }

   @Test public void mostlyNonSpanish() throws Exception {

      Graph g = graph();

      // tag the graph as being in English
      g.addTag(g, "transcript_language", "en-NZ");
      
      // tag some words as being in Spanish
      g.addAnnotation(new Annotation().setLayerId("lang").setLabel("es-MX")
                      .setStart(g.getOrCreateAnchorAt(40))
                      // 40."da".50."vergüenza".60."señalar".65."el".70
                      .setEnd(g.getOrCreateAnchorAt(70))
                      .setParent(g.first("turn")));
      
      Schema schema = g.getSchema();
      SpanishPhonologyTagger annotator = new SpanishPhonologyTagger();
      annotator.setSchema(schema);
      
      // phoneme to a new layer
      annotator.setTaskParameters(
         "tokenLayerId=word&phonemeLayerId=phonemes"
         +"&transcriptLanguageLayerId=transcript_language&phraseLanguageLayerId=lang");
      
      assertEquals("token layer",
                   "word", annotator.getTokenLayerId());
      assertEquals("transcript language layer",
                   "transcript_language", annotator.getTranscriptLanguageLayerId());
      assertEquals("phrase language layer",
                   "lang", annotator.getPhraseLanguageLayerId());
      assertEquals("phoneme layer",
                   "phonemes", annotator.getPhonemeLayerId());
      assertNotNull("phoneme layer was created",
                    schema.getLayer(annotator.getPhonemeLayerId()));
      assertEquals("phoneme layer child of word",
                   "word", schema.getLayer(annotator.getPhonemeLayerId()).getParentId());
      assertEquals("phoneme layer not aligned",
                   Constants.ALIGNMENT_NONE,
                   schema.getLayer(annotator.getPhonemeLayerId()).getAlignment());
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
                   "phonemes", outputLayers[0]);
      
      assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                   10, g.all("word").length);
      assertEquals("double check there are no phonemes: "+Arrays.asList(g.all("phonemes")),
                   0, g.all("phonemes").length);
   
      // run the annotator
      annotator.transform(g);
      List<String> phonemeLabels = Arrays.stream(g.all("phonemes"))
         .map(annotation->annotation.getLabel()).collect(Collectors.toList());
      assertEquals("no annotations token: "+phonemeLabels,
                   4, phonemeLabels.size());
      Iterator<String> phonemes = phonemeLabels.iterator();
      assertEquals("da", phonemes.next());
      assertEquals("beɾɣwenθa", phonemes.next());
      assertEquals("seɲalaɾ", phonemes.next());
      assertEquals("el", phonemes.next());
   }
      
   @Test public void mostlySpanish() throws Exception {
      
      Graph g = graph();
      
      // tag the graph as being in Spanish
      g.addTag(g, "transcript_language", "es"); 

      // tag some words as being in Te Reo Māori
      g.addAnnotation(new Annotation().setLayerId("lang").setLabel("mi")
                      .setStart(g.getOrCreateAnchorAt(40))
                      // 40."da".50."vergüenza".60."señalar".65."el".70
                      .setEnd(g.getOrCreateAnchorAt(70))
                      .setParent(g.first("turn")));

      // tag some as being in Argentine Spanish
      g.addAnnotation(new Annotation().setLayerId("lang").setLabel("es-AR")
                      .setStart(g.getOrCreateAnchorAt(20))
                      // 20."Jacinta".30."le".40
                      .setEnd(g.getOrCreateAnchorAt(40))
                      .setParent(g.first("turn")));
      
      Schema schema = g.getSchema();
      SpanishPhonologyTagger annotator = new SpanishPhonologyTagger();
      annotator.setSchema(schema);
      
      // phoneme to a new layer
      annotator.setTaskParameters(
         "tokenLayerId=word&phonemeLayerId=phonemes"
         +"&transcriptLanguageLayerId=transcript_language&phraseLanguageLayerId=lang");
      
      assertEquals("token layer",
                   "word", annotator.getTokenLayerId());
      assertEquals("transcript language layer",
                   "transcript_language", annotator.getTranscriptLanguageLayerId());
      assertEquals("phrase language layer",
                   "lang", annotator.getPhraseLanguageLayerId());
      assertEquals("phoneme layer",
                   "phonemes", annotator.getPhonemeLayerId());
      assertNotNull("phoneme layer was created",
                    schema.getLayer(annotator.getPhonemeLayerId()));
      assertEquals("phoneme layer child of word",
                   "word", schema.getLayer(annotator.getPhonemeLayerId()).getParentId());
      assertEquals("phoneme layer not aligned",
                   Constants.ALIGNMENT_NONE,
                   schema.getLayer(annotator.getPhonemeLayerId()).getAlignment());
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
                   "phonemes", outputLayers[0]);
      
      assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                   10, g.all("word").length);
      assertEquals("double check there are no phonemes: "+Arrays.asList(g.all("phonemes")),
                   0, g.all("phonemes").length);
   
      // run the annotator
      annotator.transform(g);
      List<String> phonemeLabels = Arrays.stream(g.all("phonemes"))
         .map(annotation->annotation.getLabel()).collect(Collectors.toList());
      assertEquals("no annotations token: "+phonemeLabels,
                   6, phonemeLabels.size());
      Iterator<String> phonemes = phonemeLabels.iterator();
      assertEquals("a", phonemes.next());
      assertEquals("Other variety", "xaθinta", phonemes.next());
      assertEquals("Other variety", "le", phonemes.next());
      assertEquals("deðo", phonemes.next());
      assertEquals("aθja", phonemes.next());
      assertEquals("aβaxo", phonemes.next());
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
      
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("A")
                           .setStart(g.getOrCreateAnchorAt(10)).setEnd(g.getOrCreateAnchorAt(20))
                           .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("Jacinta")
                      .setStart(g.getOrCreateAnchorAt(20)).setEnd(g.getOrCreateAnchorAt(30))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("le")
                      .setStart(g.getOrCreateAnchorAt(30)).setEnd(g.getOrCreateAnchorAt(40))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("da")
                      .setStart(g.getOrCreateAnchorAt(40)).setEnd(g.getOrCreateAnchorAt(50))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("vergüenza")
                      .setStart(g.getOrCreateAnchorAt(50)).setEnd(g.getOrCreateAnchorAt(60))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("señalar")
                      .setStart(g.getOrCreateAnchorAt(60)).setEnd(g.getOrCreateAnchorAt(65))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("el")
                      .setStart(g.getOrCreateAnchorAt(65)).setEnd(g.getOrCreateAnchorAt(70))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("dedo")
                      .setStart(g.getOrCreateAnchorAt(70)).setEnd(g.getOrCreateAnchorAt(75))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("hacia")
                      .setStart(g.getOrCreateAnchorAt(75)).setEnd(g.getOrCreateAnchorAt(80))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("abajo")
                      .setStart(g.getOrCreateAnchorAt(80)).setEnd(g.getOrCreateAnchorAt(90))
                      .setParent(turn));
      return g;
   } // end of graph()
   

   public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nzilbb.annotator.spanishphonology.test.TestSpanishPhonologyTagger");
   }
}
