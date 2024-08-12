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

package nzilbb.annotator.phonemetranscoder;
	      
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
import nzilbb.ag.automation.InvalidConfigurationException;

public class TestPhonemeTranscoder {

  static PhonemeTranscoder annotator = new PhonemeTranscoder();
   
  /** Ensure default (null) task parameters return an error. */
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

  /** Ensure that invalid task parameters generate errors. */
  @Test public void setInvalidTaskParameters() throws Exception {
      
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);

    try {
      annotator.setTaskParameters(
        // doesn't exist in the schema
        "{\"sourceLayerId\":\"orthography\","
        +"\"transcriptLanguageLayerId\":\"transcript_language\","
        +"\"phraseLanguageLayerId\":\"lang\","
        +"\"destinationLayerId\":\"cmudict\","
        +"\"translation\":\"DISC2CMU\"}");
      fail("Should fail with nonexistent sourceLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "{\"sourceLayerId\":\"disc\","
        // doesn't exist in the schema
        +"\"transcriptLanguageLayerId\":\"language\","
        +"\"phraseLanguageLayerId\":\"lang\","
        +"\"destinationLayerId\":\"cmudict\","
        +"\"translation\":\"DISC2CMU\"}");
      fail("Should fail with nonexistent transcriptLanguageLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "{\"sourceLayerId\":\"disc\","
        +"\"transcriptLanguageLayerId\":\"transcript_language\","
        // doesn't exist in the schema
        +"\"phraseLanguageLayerId\":\"language\","
        +"\"destinationLayerId\":\"cmudict\","
        +"\"translation\":\"DISC2CMU\"}");
      fail("Should fail with nonexistent phraseLanguageLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "{\"sourceLayerId\":\"disc\","
        +"\"transcriptLanguageLayerId\":\"transcript_language\","
        +"\"phraseLanguageLayerId\":\"lang\","
        // same as source layer
        +"\"destinationLayerId\":\"disc\","
        +"\"translation\":\"DISC2CMU\"}");
      fail("Should fail with destinationLayerId = sourceLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "{\"sourceLayerId\":\"disc\","
        +"\"transcriptLanguageLayerId\":\"transcript_language\","
        +"\"phraseLanguageLayerId\":\"lang\","
        +"\"destinationLayerId\":\"someLayer\","
        // invalid translation
        +"\"translation\":\"invalid-translation\"}");
      fail("Should fail with destinationLayerId = sourceLayerId");
    } catch (InvalidConfigurationException x) {
    }
  }

  /** Ensure valid task parameters are accepted and processed correctly, and annotator works. */
  @Test public void basicTranscoding() throws Exception {
    
    Graph g = graph();
    // tag the graph as being in New Zealand English
    g.addTag(g, "transcript_language", "en-NZ");
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // use specified configuration
    annotator.setTaskParameters(
      "{\"sourceLayerId\":\"disc\","
      +"\"transcriptLanguageLayerId\":\"\","   // no transcript language layer
      +"\"phraseLanguageLayerId\":\"\","       // no phrase language layer
      +"\"destinationLayerId\":\"cmu\","   // non-default layer
      +"\"translation\":\"DISC2CMU\"}");          // CMU ARPAbet encoding
    
    assertEquals("source layer",
                 "disc", annotator.getSourceLayerId());
    assertNull("transcript language layer",
               annotator.getTranscriptLanguageLayerId());
    assertNull("phrase language layer",
               annotator.getPhraseLanguageLayerId());
    assertEquals("destination layer",
                 "cmu", annotator.getDestinationLayerId());
    assertNotNull("destination layer was created",
                  schema.getLayer(annotator.getDestinationLayerId()));
    assertEquals("destination layer child of word",
                 "word", schema.getLayer(annotator.getDestinationLayerId()).getParentId());
    assertEquals("destination layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getDestinationLayerId()).getAlignment());
    assertEquals("destination layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getDestinationLayerId()).getType());
    assertEquals("translation",
                 "DISC2CMU", annotator.getTranslation());
    assertTrue("destination layer allows peers",
               schema.getLayer(annotator.getDestinationLayerId()).getPeers());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("1 required layer: "+requiredLayers,
                 1, requiredLayers.size());
    assertTrue("disc required "+requiredLayers,
               requiredLayers.contains("disc"));
    String outputLayers[] = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                 1, outputLayers.length);
    assertEquals("output layer correct "+Arrays.asList(outputLayers),
                 "cmu", outputLayers[0]);
    
    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "there'll", firstWord.getLabel());
      
    assertEquals("double check there are sources: "+Arrays.asList(g.all("disc")),
                 6, g.all("disc").length);
    assertEquals("double check there are no destinations: "+Arrays.asList(g.all("cmu")),
                 0, g.all("cmu").length);
    // run the annotator
    annotator.transform(g);
    List<String> pronLabels = Arrays.stream(g.all("cmu"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("Correct number of tags "+pronLabels,
                 6, pronLabels.size());
    Iterator<String> prons = pronLabels.iterator();
    assertEquals("DH EH2 R IH0 L", prons.next());
    assertEquals("B IY2", prons.next());
    assertEquals("First variant", "EY2", prons.next());
    assertEquals("Second variant", "AH2", prons.next());
    assertEquals("Third variant", "IH0", prons.next());
    assertEquals("T R AE2 N S K R IH2 P SH IH0 N", prons.next());

    // add a word
    Annotation newWord = g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("new")
      .setStart(g.getOrCreateAnchorAt(90)).setEnd(g.getOrCreateAnchorAt(100))
      .setParent(g.first("turn")));
    newWord.createTag("disc", "nju");

    // change a word
    firstWord.setLabel("won't");
      
    // run the annotator again
    annotator.transform(g);
    pronLabels = Arrays.stream(g.all("cmu"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("one more destination: "+pronLabels,
                 7, pronLabels.size());
    prons = pronLabels.iterator();
    assertEquals("changed label not re-annotated", // TODO do we really want this??
                 "DH EH2 R IH0 L", prons.next());
    assertEquals("previous pron unchanged", "B IY2", prons.next());
    assertEquals("previous pron unchanged", "EY2", prons.next());
    assertEquals("previous pron unchanged", "AH2", prons.next());
    assertEquals("previous pron unchanged", "IH0", prons.next());
    assertEquals("previous pron unchanged", "T R AE2 N S K R IH2 P SH IH0 N", prons.next());
    assertEquals("new word is tagged",
                 "N Y UW2", prons.next());

  }

  /** 
   * Ensure custom mapping of source to destination characters, copying unknown
   * characters, works.
   */
  @Test public void customTranscodingWithCopy() throws Exception {
    
    Graph g = graph();
    // tag the graph as being in New Zealand English
    g.addTag(g, "transcript_language", "en-NZ");
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // use specified configuration
    annotator.setTaskParameters(
      "{\"sourceLayerId\":\"word\","
      +"\"transcriptLanguageLayerId\":\"\","
      +"\"phraseLanguageLayerId\":\"\","
      +"\"destinationLayerId\":\"custom\","
      +"\"translation\":\"custom\","
      +"\"copyCharacters\":\"true\","
      +"\"custom\":["
      +"{\"source\":\"th\",\"destination\":\"ð\"},"
      +"{\"source\":\"er\",\"destination\":\"ɛǝ\"},"
      +"{\"source\":\"e\",\"destination\":\"i\"},"
      +"{\"source\":\"a\",\"destination\":\"ǝ\"},"
      +"{\"source\":\"tio\",\"destination\":\"ʃ\"},"
      +"{\"source\":\"ll\",\"destination\":\"l\"},"
      +"{\"source\":\"'\",\"destination\":\"\"}"
      +"]}");          // CMU ARPAbet encoding
    
    assertEquals("source layer",
                 "word", annotator.getSourceLayerId());
    assertNull("transcript language layer",
               annotator.getTranscriptLanguageLayerId());
    assertNull("phrase language layer",
               annotator.getPhraseLanguageLayerId());
    assertEquals("destination layer",
                 "custom", annotator.getDestinationLayerId());
    assertNotNull("destination layer was created",
                  schema.getLayer(annotator.getDestinationLayerId()));
    assertEquals("destination layer child of word",
                 "word", schema.getLayer(annotator.getDestinationLayerId()).getParentId());
    assertEquals("destination layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getDestinationLayerId()).getAlignment());
    assertEquals("destination layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getDestinationLayerId()).getType());
    assertTrue("destination layer allows peers",
               schema.getLayer(annotator.getDestinationLayerId()).getPeers());
    assertEquals("translation",
                 "custom", annotator.getTranslation());
    assertTrue("copy characters",
               annotator.getCopyCharacters());
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
                 "custom", outputLayers[0]);
    
    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "there'll", firstWord.getLabel());
      
    assertEquals("double check there are sources: "+Arrays.asList(g.all("word")),
                 4, g.all("word").length);
    assertEquals("double check there are no destinations: "+Arrays.asList(g.all("custom")),
                 0, g.all("custom").length);
    // run the annotator
    annotator.transform(g);
    List<String> pronLabels = Arrays.stream(g.all("custom"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("Correct number of tags "+pronLabels,
                 4, pronLabels.size());
    Iterator<String> prons = pronLabels.iterator();
    assertEquals("ðɛǝil", prons.next());
    assertEquals("bi", prons.next());
    assertEquals("ǝ", prons.next());
    assertEquals("trǝnscripʃn", prons.next());
    assertFalse("no extra tags", prons.hasNext());

    // check parents
    Annotation words[] = g.all("word");
    Annotation tags[] = g.all("custom");
    for (int w = 0; w < words.length; w++) {
      assertEquals("Parent of tag " + w, words[w], tags[w].getParent());
    }
  }
  
  /** 
   * Ensure custom mapping of source to destination characters, ignoring unknown
   * characters, works. 
   */
  @Test public void customTranscodingWithoutCopy() throws Exception {
    
    Graph g = graph();
    // tag the graph as being in New Zealand English
    g.addTag(g, "transcript_language", "en-NZ");
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // use specified configuration
    annotator.setTaskParameters(
      "{\"sourceLayerId\":\"word\","
      +"\"transcriptLanguageLayerId\":\"\","
      +"\"phraseLanguageLayerId\":\"\","
      +"\"destinationLayerId\":\"custom\","
      +"\"translation\":\"custom\","
      +"\"copyCharacters\":\"false\","
      +"\"customDelimiter\":\"\"," // explicitly no delimiter
      +"\"custom\":["
      +"{\"source\":\"th\",\"destination\":\"ð\"},"
      +"{\"source\":\"er\",\"destination\":\"ɛǝ\"},"
      +"{\"source\":\"e\",\"destination\":\"i\"},"
      +"{\"source\":\"a\",\"destination\":\"ǝ\"},"
      +"{\"source\":\"tio\",\"destination\":\"ʃ\"},"
      +"{\"source\":\"ll\",\"destination\":\"l\"},"
      +"{\"source\":\"'\",\"destination\":\"\"}"
      +"]}");          // CMU ARPAbet encoding
    
    assertEquals("source layer",
                 "word", annotator.getSourceLayerId());
    assertNull("transcript language layer",
               annotator.getTranscriptLanguageLayerId());
    assertNull("phrase language layer",
               annotator.getPhraseLanguageLayerId());
    assertEquals("destination layer",
                 "custom", annotator.getDestinationLayerId());
    assertNotNull("destination layer was created",
                  schema.getLayer(annotator.getDestinationLayerId()));
    assertEquals("destination layer child of word",
                 "word", schema.getLayer(annotator.getDestinationLayerId()).getParentId());
    assertEquals("destination layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getDestinationLayerId()).getAlignment());
    assertEquals("destination layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getDestinationLayerId()).getType());
    assertTrue("destination layer allows peers",
               schema.getLayer(annotator.getDestinationLayerId()).getPeers());
    assertEquals("translation",
                 "custom", annotator.getTranslation());
    assertFalse("don't copy characters",
                annotator.getCopyCharacters());
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
                 "custom", outputLayers[0]);
    
    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "there'll", firstWord.getLabel());
      
    assertEquals("double check there are sources: "+Arrays.asList(g.all("word")),
                 4, g.all("word").length);
    assertEquals("double check there are no destinations: "+Arrays.asList(g.all("custom")),
                 0, g.all("custom").length);
    // run the annotator
    annotator.transform(g);
    List<String> pronLabels = Arrays.stream(g.all("custom"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("Correct number of tags "+pronLabels,
                 4, pronLabels.size());
    Iterator<String> prons = pronLabels.iterator();
    assertEquals("ðɛǝil", prons.next());
    assertEquals("i", prons.next());
    assertEquals("ǝ", prons.next());
    assertEquals("ǝʃ", prons.next());
    
    // check parents
    Annotation words[] = g.all("word");
    Annotation tags[] = g.all("custom");
    for (int w = 0; w < words.length; w++) {
      assertEquals("Parent of tag " + w, words[w], tags[w].getParent());
    }
  }

  /** 
   * Ensure custom mapping of source to destination characters, with a delimiter, works. 
   */
  @Test public void customTranscodingWithoutDelimiter() throws Exception {
    
    Graph g = graph();
    // tag the graph as being in New Zealand English
    g.addTag(g, "transcript_language", "en-NZ");
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // use specified configuration
    annotator.setTaskParameters(
      "{\"sourceLayerId\":\"word\","
      +"\"transcriptLanguageLayerId\":\"\","
      +"\"phraseLanguageLayerId\":\"\","
      +"\"destinationLayerId\":\"custom\","
      +"\"translation\":\"custom\","
      +"\"copyCharacters\":\"false\","
      +"\"customDelimiter\":\" \"," // space delimiter
      +"\"custom\":["
      +"{\"source\":\"th\",\"destination\":\"ð\"},"
      +"{\"source\":\"er\",\"destination\":\"ɛǝ\"},"
      +"{\"source\":\"e\",\"destination\":\"i\"},"
      +"{\"source\":\"a\",\"destination\":\"ǝ\"},"
      +"{\"source\":\"tio\",\"destination\":\"ʃ\"},"
      +"{\"source\":\"ll\",\"destination\":\"l\"},"
      +"{\"source\":\"'\",\"destination\":\"\"}"
      +"]}");          // CMU ARPAbet encoding
    
    assertEquals("source layer",
                 "word", annotator.getSourceLayerId());
    assertNull("transcript language layer",
               annotator.getTranscriptLanguageLayerId());
    assertNull("phrase language layer",
               annotator.getPhraseLanguageLayerId());
    assertEquals("destination layer",
                 "custom", annotator.getDestinationLayerId());
    assertNotNull("destination layer was created",
                  schema.getLayer(annotator.getDestinationLayerId()));
    assertEquals("destination layer child of word",
                 "word", schema.getLayer(annotator.getDestinationLayerId()).getParentId());
    assertEquals("destination layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getDestinationLayerId()).getAlignment());
    assertEquals("destination layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getDestinationLayerId()).getType());
    assertTrue("destination layer allows peers",
               schema.getLayer(annotator.getDestinationLayerId()).getPeers());
    assertEquals("translation",
                 "custom", annotator.getTranslation());
    assertFalse("don't copy characters",
                annotator.getCopyCharacters());
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
                 "custom", outputLayers[0]);
    
    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "there'll", firstWord.getLabel());
      
    assertEquals("double check there are sources: "+Arrays.asList(g.all("word")),
                 4, g.all("word").length);
    assertEquals("double check there are no destinations: "+Arrays.asList(g.all("custom")),
                 0, g.all("custom").length);
    // run the annotator
    annotator.transform(g);
    List<String> pronLabels = Arrays.stream(g.all("custom"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("Correct number of tags "+pronLabels,
                 4, pronLabels.size());
    Iterator<String> prons = pronLabels.iterator();
    assertEquals("ð ɛǝ i l", prons.next());
    assertEquals("i", prons.next());
    assertEquals("ǝ", prons.next());
    assertEquals("ǝ ʃ", prons.next());
    
    // check parents
    Annotation words[] = g.all("word");
    Annotation tags[] = g.all("custom");
    for (int w = 0; w < words.length; w++) {
      assertEquals("Parent of tag " + w, words[w], tags[w].getParent());
    }
  }

  /** Ensure tagger does nothing if the transcript is in a non-matching language */
  @Test public void nonEnglish() throws Exception {
      
    Graph g = graph();
    // tag the graph as being in Te Reo Māori
    g.addTag(g, "transcript_language", "mi");
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // use specified configuration
    annotator.setTaskParameters(
      "{\"sourceLayerId\":\"disc\","
      +"\"transcriptLanguageLayerId\":\"transcript_language\","
      +"\"phraseLanguageLayerId\":\"lang\","
      +"\"language\":\"en.*\","
      +"\"destinationLayerId\":\"cmu\","
      +"\"translation\":\"DISC2CMU\"}");
    
    assertEquals("source layer",
                 "disc", annotator.getSourceLayerId());
    assertNotNull("transcript language layer",
                  annotator.getTranscriptLanguageLayerId());
    assertNotNull("phrase language layer",
                  annotator.getPhraseLanguageLayerId());
    assertEquals("destination layer",
                 "cmu", annotator.getDestinationLayerId());
    assertNotNull("destination layer was created",
                  schema.getLayer(annotator.getDestinationLayerId()));
    assertEquals("destination layer child of word",
                 "word", schema.getLayer(annotator.getDestinationLayerId()).getParentId());
    assertEquals("destination layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getDestinationLayerId()).getAlignment());
    assertEquals("destination layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getDestinationLayerId()).getType());
    assertEquals("translation",
                 "DISC2CMU", annotator.getTranslation());
    assertTrue("destination layer allows peers",
               schema.getLayer(annotator.getDestinationLayerId()).getPeers());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("3 required layer: "+requiredLayers,
                 3, requiredLayers.size());
    assertTrue("disc required "+requiredLayers,
               requiredLayers.contains("disc"));
    assertTrue("transcript language required "+requiredLayers,
               requiredLayers.contains("transcript_language"));
    assertTrue("phrase language required "+requiredLayers,
               requiredLayers.contains("lang"));
    String outputLayers[] = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                 1, outputLayers.length);
    assertEquals("output layer correct "+Arrays.asList(outputLayers),
                 "cmu", outputLayers[0]);
    
    assertEquals("double check there are sources: "+Arrays.asList(g.all("disc")),
                 6, g.all("disc").length);
    assertEquals("double check there are no destinations: "+Arrays.asList(g.all("cmu")),
                 0, g.all("cmu").length);
    // run the annotator
    annotator.transform(g);
    assertEquals("there are no output annotations: "+Arrays.asList(g.all("cmu")),
                 0, g.all("cmu").length);

  }   

  /** Ensure only phrases in matching language are tagged */
  @Test public void mostlyNonEnglish() throws Exception {
      
    Graph g = graph();
      
    // tag the graph as being in Te Reo Māori
    g.addTag(g, "transcript_language", "mi");

    // tag some words as being in NZ English
    g.addAnnotation(new Annotation().setLayerId("lang").setLabel("en-NZ")
                    .setStart(g.getOrCreateAnchorAt(30))
                    // 30."a".40."transcription".50
                    .setEnd(g.getOrCreateAnchorAt(50))
                    .setParent(g.first("turn")));
      
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
      
    // use specified configuration
    annotator.setTaskParameters(
      "{\"sourceLayerId\":\"disc\","
      +"\"transcriptLanguageLayerId\":\"transcript_language\","
      +"\"phraseLanguageLayerId\":\"lang\","
      +"\"language\":\"en.*\","
      +"\"destinationLayerId\":\"cmu\","
      +"\"translation\":\"DISC2CMU\"}");
    
    assertEquals("source layer",
                 "disc", annotator.getSourceLayerId());
    assertNotNull("transcript language layer",
                  annotator.getTranscriptLanguageLayerId());
    assertNotNull("phrase language layer",
                  annotator.getPhraseLanguageLayerId());
    assertEquals("destination layer",
                 "cmu", annotator.getDestinationLayerId());
    assertNotNull("destination layer was created",
                  schema.getLayer(annotator.getDestinationLayerId()));
    assertEquals("destination layer child of word",
                 "word", schema.getLayer(annotator.getDestinationLayerId()).getParentId());
    assertEquals("destination layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getDestinationLayerId()).getAlignment());
    assertEquals("destination layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getDestinationLayerId()).getType());
    assertEquals("translation",
                 "DISC2CMU", annotator.getTranslation());
    assertTrue("destination layer allows peers",
               schema.getLayer(annotator.getDestinationLayerId()).getPeers());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("3 required layer: "+requiredLayers,
                 3, requiredLayers.size());
    assertTrue("disc required "+requiredLayers,
               requiredLayers.contains("disc"));
    assertTrue("transcript language required "+requiredLayers,
               requiredLayers.contains("transcript_language"));
    assertTrue("phrase language required "+requiredLayers,
               requiredLayers.contains("lang"));
    String outputLayers[] = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                 1, outputLayers.length);
    assertEquals("output layer correct "+Arrays.asList(outputLayers),
                 "cmu", outputLayers[0]);
    
    assertEquals("double check there are sources: "+Arrays.asList(g.all("disc")),
                 6, g.all("disc").length);
    assertEquals("double check there are no destinations: "+Arrays.asList(g.all("cmu")),
                 0, g.all("cmu").length);

    // run the annotator
    annotator.transform(g);
    List<String> pronLabels = Arrays.stream(g.all("cmu"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("Correct number of annotations "+pronLabels,
                 4, pronLabels.size());
    Iterator<String> prons = pronLabels.iterator();
    assertEquals("First variant", "EY2", prons.next());
    assertEquals("Second variant", "AH2", prons.next());
    assertEquals("Third variant", "IH0", prons.next());
    assertEquals("T R AE2 N S K R IH2 P SH IH0 N", prons.next());

  }   

  @Test public void mostlyEnglish() throws Exception {
      
    Graph g = graph();
      
    g.addTag(g, "transcript_language", "en-NZ"); 

    // tag some words as being in Te Reo Māori
    g.addAnnotation(new Annotation().setLayerId("lang").setLabel("mi")
                    .setStart(g.getOrCreateAnchorAt(30))
                    // 30."a".40."transcription".50
                    .setEnd(g.getOrCreateAnchorAt(50))
                    .setParent(g.first("turn")));

    Schema schema = g.getSchema();
    annotator.setSchema(schema);
      
    // use specified configuration
    annotator.setTaskParameters(
      "{\"sourceLayerId\":\"disc\","
      +"\"transcriptLanguageLayerId\":\"transcript_language\","
      +"\"phraseLanguageLayerId\":\"lang\","
      +"\"language\":\"en.*\","
      +"\"destinationLayerId\":\"cmu\","
      +"\"translation\":\"DISC2CMU\"}");
    
    assertEquals("source layer",
                 "disc", annotator.getSourceLayerId());
    assertNotNull("transcript language layer",
                  annotator.getTranscriptLanguageLayerId());
    assertNotNull("phrase language layer",
                  annotator.getPhraseLanguageLayerId());
    assertEquals("destination layer",
                 "cmu", annotator.getDestinationLayerId());
    assertNotNull("destination layer was created",
                  schema.getLayer(annotator.getDestinationLayerId()));
    assertEquals("destination layer child of word",
                 "word", schema.getLayer(annotator.getDestinationLayerId()).getParentId());
    assertEquals("destination layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getDestinationLayerId()).getAlignment());
    assertEquals("destination layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getDestinationLayerId()).getType());
    assertEquals("translation",
                 "DISC2CMU", annotator.getTranslation());
    assertTrue("destination layer allows peers",
               schema.getLayer(annotator.getDestinationLayerId()).getPeers());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("3 required layer: "+requiredLayers,
                 3, requiredLayers.size());
    assertTrue("disc required "+requiredLayers,
               requiredLayers.contains("disc"));
    assertTrue("transcript language required "+requiredLayers,
               requiredLayers.contains("transcript_language"));
    assertTrue("phrase language required "+requiredLayers,
               requiredLayers.contains("lang"));
    String outputLayers[] = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                 1, outputLayers.length);
    assertEquals("output layer correct "+Arrays.asList(outputLayers),
                 "cmu", outputLayers[0]);
    
    assertEquals("double check there are sources: "+Arrays.asList(g.all("disc")),
                 6, g.all("disc").length);
    assertEquals("double check there are no destinations: "+Arrays.asList(g.all("cmu")),
                 0, g.all("cmu").length);

    // run the annotator
    annotator.transform(g);
    List<String> pronLabels = Arrays.stream(g.all("cmu"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("Correct number of annotations "+pronLabels,
                 2, pronLabels.size());
    Iterator<String> prons = pronLabels.iterator();
    assertEquals("DH EH2 R IH0 L", prons.next());
    assertEquals("B IY2", prons.next());

  }
   
  /**
   * Returns a graph for annotating.
   * @return The graph for testing with.
   */
  public static Graph graph() {
    Schema schema = new Schema(
      "participant", "turn", "utterance", "word",
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
      .setParentId("turn").setParentIncludes(true),
      new Layer("disc", "DISC-encoded phonemes").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true)
      .setType("ipa"));
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
      
    Annotation therell = g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("there'll")
      .setStart(g.getOrCreateAnchorAt(10)).setEnd(g.getOrCreateAnchorAt(20))
      .setParent(turn));
    therell.createTag("disc", "D8r@l");
    Annotation be = g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("be")
      .setStart(g.getOrCreateAnchorAt(20)).setEnd(g.getOrCreateAnchorAt(30))
      .setParent(turn));
    be.createTag("disc", "bi");
    Annotation a = g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("a")
      .setStart(g.getOrCreateAnchorAt(30)).setEnd(g.getOrCreateAnchorAt(40))
      .setParent(turn));
    a.createTag("disc", "1");
    a.createTag("disc", "V");
    a.createTag("disc", "@");
    Annotation transcription = g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("transcription")
      .setStart(g.getOrCreateAnchorAt(40)).setEnd(g.getOrCreateAnchorAt(50))
      .setParent(turn));
    transcription.createTag("disc", "tr{nskrIpS@n");
    return g;
  } // end of graph()
   
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.annotator.phonemetranscoder.TestPhonemeTranscoder");
  }
}
