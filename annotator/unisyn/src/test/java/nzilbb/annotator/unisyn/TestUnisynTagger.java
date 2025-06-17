//
// Copyright 2023 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.annotator.unisyn;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import nzilbb.ag.Anchor;
import nzilbb.ag.Annotation;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.automation.Dictionary;
import nzilbb.ag.automation.DictionaryException;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.UsesRelationalDatabase;
import nzilbb.sql.derby.DerbyConnectionFactory;

public class TestUnisynTagger {
  
  static UnisynTagger annotator = new UnisynTagger();
  
  @BeforeClass
  public static void install() throws Exception {

    // output all statuses
    //annotator.getStatusObservers().add(s->System.out.println(s));

    System.out.println("Installing lexicon...");

    // find the current directory
    File dir = dir();

    // set the schema
    annotator.setSchema(graph().getSchema());

    // use derby for relational database
    annotator.setRdbConnectionFactory(new DerbyConnectionFactory(dir));

    // load a-z.csv as lexicon
    File file = new File(dir, "test.unisyn");
    String error = annotator.loadLexicon(file.getName(), file);
    if (error.length() > 0) {
      fail(error);
    }
    // loading is in a separate thread
    while (annotator.getRunning()) {
      try {Thread.sleep(100);} catch(Exception exception) {}
    }
    
    System.out.println("Lexicon installed.");
  }
	 
  public static File dir() throws Exception { 
    URL urlThisClass = TestUnisynTagger.class.getResource(
      TestUnisynTagger.class.getSimpleName() + ".class");
    File fThisClass = new File(urlThisClass.toURI());
    return fThisClass.getParentFile();
  }

  /** Ensure default configuration works. */
  @Test public void defaultParameters() throws Exception {

    // ensure there's only one lexicon
    try { annotator.deleteLexicon("temp"); } catch (Throwable t) {}

    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);

    // use default configuration
    annotator.setTaskParameters(null);
    
    assertEquals("lexicon",
                 "test.unisyn", annotator.getLexicon());
    assertEquals("field",
                 "pron_disc", annotator.getField());
    assertEquals("token layer",
                 "orthography", annotator.getTokenLayerId());
    assertEquals("transcript language layer",
                 "transcript_language", annotator.getTranscriptLanguageLayerId());
    assertEquals("phrase language layer",
                 "lang", annotator.getPhraseLanguageLayerId());
    assertNull("phone layer",
               annotator.getPhoneLayerId());
    assertEquals("tag layer",
                 "phonemes", annotator.getTagLayerId());
    assertNotNull("tag layer was created",
                  schema.getLayer(annotator.getTagLayerId()));
    assertEquals("tag layer child of word",
                 "word", schema.getLayer(annotator.getTagLayerId()).getParentId());
    assertEquals("pronunciation layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getTagLayerId()).getAlignment());
    assertEquals("pronunciation layer type correct",
                 Constants.TYPE_IPA,
                 schema.getLayer(annotator.getTagLayerId()).getType());
    assertTrue("stripSyllStress",
               annotator.getStripSyllStress());
    assertFalse("firstVariantOnly=false",
                annotator.getFirstVariantOnly());
    assertTrue("pronunciation layer allows peers (firstVariantOnly=false)",
               schema.getLayer(annotator.getTagLayerId()).getPeers());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("3 required layer: "+requiredLayers,
                 3, requiredLayers.size());
    assertTrue("orthography required "+requiredLayers,
               requiredLayers.contains("orthography"));
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
                 "The", firstWord.getLabel());
      
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 9, g.all("word").length);
    assertEquals("double check there are no pronunciations: "+Arrays.asList(g.all("phonemes")),
                 0, g.all("phonemes").length);
    // run the annotator
    annotator.transform(g);
    List<String> pronLabels = Arrays.stream(g.all("phonemes"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("Correct number of tokens "+pronLabels,
                 8, pronLabels.size());
    Iterator<String> prons = pronLabels.iterator();
    assertEquals("D@", prons.next());
    assertEquals("Not 'the', as three entries collapse to one after stripping stress",
                 "kwIk", prons.next());
    assertEquals("br6n", prons.next());
    // no entry for fox
    assertEquals("_Vmps", prons.next());
    assertEquals("5v@r", prons.next());
    assertEquals("D@", prons.next());
    assertEquals("l1zi", prons.next());
    assertEquals("d$g", prons.next());

    // ensure parents are on word layer
    for (Annotation pron : g.all("phonemes")) {
      Annotation parent = pron.getParent();
      assertNotNull("parent set: " + pron, parent);
      assertEquals("parent on correct layer: " + pron, "word", parent.getLayerId());
    }

    // add a word
    Annotation newWord = g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("a")
      .setStart(g.getOrCreateAnchorAt(90)).setEnd(g.getOrCreateAnchorAt(100))
      .setParent(g.first("turn")));
    g.createTag(newWord, "orthography", "a");

    // change a word
    firstWord.setLabel("dog");
      
    // run the annotator again
    annotator.transform(g);
    pronLabels = Arrays.stream(g.all("phonemes"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("two more pronunciation: "+pronLabels,
                 11, pronLabels.size());
    prons = pronLabels.iterator();
    assertEquals("First word is still tagged, because existing tags aren't removed",
                 "D@", prons.next());
    assertEquals("kwIk", prons.next());
    assertEquals("br6n", prons.next());
    // no entry for fox
    assertEquals("_Vmps", prons.next());
    assertEquals("5v@r", prons.next());
    assertEquals("D@", prons.next());
    assertEquals("l1zi", prons.next());
    assertEquals("d$g", prons.next());
    assertEquals("New token",
                 "1", prons.next());
    assertEquals("Multiple entries - 2",
                 "@", prons.next());
    assertEquals("Multiple entries - 3",
                 "Q", prons.next());

  }   

  /** Test explicitly set parameters take effect. */
  @Test public void setTaskParameters() throws Exception {
      
    Graph g = graph();
    // tag the graph as being in New Zealand English
    g.addTag(g, "transcript_language", "en-NZ");
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // use specified configuration
    annotator.setTaskParameters(
      "tokenLayerId=word"
      +"&transcriptLanguageLayerId="   // no transcript language layer
      +"&phraseLanguageLayerId="       // no phrase language layer
      +"&tagLayerId=frequency"         // non-default layer
      +"&lexicon=test.unisyn"
      +"&field=frequency"
      +"&firstVariantOnly=on"
      +"&strip=");
      
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertNull("transcript language layer",
               annotator.getTranscriptLanguageLayerId());
    assertNull("phrase language layer",
               annotator.getPhraseLanguageLayerId());
    assertNull("phone layer",
               annotator.getPhoneLayerId());
    assertEquals("tag layer",
                 "frequency", annotator.getTagLayerId());
    assertNotNull("tag layer was created",
                  schema.getLayer(annotator.getTagLayerId()));
    assertEquals("tag layer child of word",
                 "word", schema.getLayer(annotator.getTagLayerId()).getParentId());
    assertEquals("tag layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getTagLayerId()).getAlignment());
    assertEquals("tag layer type correct",
                 Constants.TYPE_NUMBER,
                 schema.getLayer(annotator.getTagLayerId()).getType());
    assertEquals("lexicon",
                 "test.unisyn", annotator.getLexicon());
    assertEquals("field",
                 "frequency", annotator.getField());
    assertFalse("tag layer disallows peers (firstVariantOnly=true)",
                schema.getLayer(annotator.getTagLayerId()).getPeers());
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
                 "frequency", outputLayers[0]);

    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "The", firstWord.getLabel());
      
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 9, g.all("word").length);
    assertEquals("double check there are no frequencies: "+Arrays.asList(g.all("frequency")),
                 0, g.all("freuency").length);
    // run the annotator
    annotator.transform(g);
    List<String> freqLabels = Arrays.stream(g.all("frequency"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("Correct number of tokens "+freqLabels,
                 8, freqLabels.size());
    Iterator<String> freqs = freqLabels.iterator();
    assertEquals("First entry for 'the'",
                 "16006650", freqs.next());
    assertEquals("Not second entry for 'the'",
                 "35052", freqs.next());
    assertEquals("18347", freqs.next());
    assertEquals("2025", freqs.next());
    assertEquals("420898", freqs.next());
    assertEquals("16006650", freqs.next());
    assertEquals("3802", freqs.next());
    assertEquals("44393", freqs.next());

  }   

  /** Test validation. */
  @Test public void setInvalidTaskParameters() throws Exception {
      
    try {
      annotator.setTaskParameters(
        // doesn't exist in the schema
        "tokenLayerId=nonexistent"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&tagLayerId=unisyn"
        +"&firstVariantOnly=on"
        +"&lexicon=test.unisyn&field=pron_disc");
      fail("Should fail with nonexistent tokenLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        // doesn't exist in the schema
        +"&transcriptLanguageLayerId=language"
        +"&phraseLanguageLayerId=lang"
        +"&tagLayerId=unisyn"
        +"&firstVariantOnly=on"
        +"&lexicon=test.unisyn&field=pron_disc");
      fail("Should fail with nonexistent transcriptLanguageLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        // doesn't exist in the schema
        +"&phraseLanguageLayerId=language"
        +"&tagLayerId=unisyn"
        +"&firstVariantOnly=on"
        +"&lexicon=test.unisyn&field=pron_disc");
      fail("Should fail with nonexistent phraseLanguageLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        // same as token layer
        +"&tagLayerId=word"
        +"&firstVariantOnly=on"
        +"&lexicon=test.unisyn&field=pron_disc");
      fail("Should fail with pronunciationLayerId = tokenLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&tagLayerId=unisyn"
        +"&firstVariantOnly=on"
        // not specified
        +"&lexicon="
        +"field=pron_disc");
      fail("Should fail with no lexicon");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&tagLayerId=unisyn"
        +"&firstVariantOnly=on"
        +"&lexicon=test.unisyn"
        // not specified
        +"&field=");
      fail("Should fail with no lexicon");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&tagLayerId=unisyn"
        +"&firstVariantOnly=on"
        // nonexistent
        +"&dictionary=nonexistent"
        +"&field=pron_disc");
      fail("Should fail with nonexistent lexicon");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&tagLayerId=unisyn"
        +"&firstVariantOnly=on"
        +"&lexicon=test.unisyn"
        // invalid
        +"&field=nonexistent");
      fail("Should fail with invalid key field");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&tagLayerId=unisyn"
        +"&firstVariantOnly=on"
        +"&lexicon=test.unisyn&field=pron_disc"
        // invalid regular expression
        +"&targetLanguagePattern=*");
      fail("Should fail with invalid targetLanguage pattern");
    } catch (InvalidConfigurationException x) {
    }

    // set firstVariantOnly = false for a layer that doesn't allow peers 
    annotator.getSchema().addLayer(
      new Layer("frequency")
      .setAlignment(Constants.ALIGNMENT_NONE)
      // no peers allowed
      .setPeers(false)
      .setParentId(annotator.getSchema().getWordLayerId()));
    annotator.setTaskParameters(
      "tokenLayerId=word"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&phraseLanguageLayerId=lang"
      +"&tagLayerId=frequency"
      // all variants
      +"&firstVariantOnly=false"
      +"&lexicon=test.unisyn&field=frequency"
      +"&targetLanguagePattern=");
    // no exception is thrown, but firstVariantOnly is now true
    assertTrue("firstVariantOnly has been corrected", annotator.getFirstVariantOnly());
    
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&tagLayerId=unisyn"
        +"&firstVariantOnly=on"
        +"&lexicon=test.unisyn&field=pron_disc"
        +"&targetLanguagePattern=en.*"
        +"&phoneLayerId=doesn't-exits");
      fail("Should fail with invalid phoneLayerId");
    } catch (InvalidConfigurationException x) {
    }
  }   

  /** Test that language-specific tagging works when the whole transcript is not targeted */
  @Test public void languageSelection() throws Exception {
      
    Graph g = graph();

    // tag the graph as being in Te Reo Māori
    g.addTag(g, "transcript_language", "mi");
      
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
      
    // use default configuration
    annotator.setTaskParameters(
      "tokenLayerId=word"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&phraseLanguageLayerId=lang"
      +"&targetLanguagePattern=en.*"
      +"&tagLayerId=unisyn"
      +"&lexicon=test.unisyn"
      +"&field=pron_disc"
      +"&firstVariantOnly=false"
      +"&phoneLayerId=");
    
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("transcript language layer",
                 "transcript_language", annotator.getTranscriptLanguageLayerId());
    assertEquals("phrase language layer",
                 "lang", annotator.getPhraseLanguageLayerId());
    assertEquals("target language",
                 "en.*", annotator.getTargetLanguagePattern());
    assertNull("phone layer - empty string deserialized as null",
               annotator.getPhoneLayerId());
    assertEquals("tag layer",
                 "unisyn", annotator.getTagLayerId());
    assertFalse("firstVariantOnly=false",
                annotator.getFirstVariantOnly());
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
                 "unisyn", outputLayers[0]);

    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "The", firstWord.getLabel());
      
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 9, g.all("word").length);
    assertEquals("double check there are no pronunciations: "+Arrays.asList(g.all("phonemes")),
                 0, g.all("phonemes").length);
    // run the annotator
    annotator.transform(g);
    List<String> pronLabels = Arrays.stream(g.all("phonemes"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("No tokens annotated "+pronLabels,
                 0, pronLabels.size());
    
    // ensure parents are on word layer
    for (Annotation pron : g.all("phonemes")) {
      Annotation parent = pron.getParent();
      assertNotNull("parent set: " + pron, parent);
      assertEquals("parent on correct layer: " + pron, "word", parent.getLayerId());
    }

  }   

  /** Test that language-specific tagging works when only phrases are targeted, and also
   * that the strip setting works */
  @Test public void mostlyNonEnglishAndDontStrip() throws Exception {
      
    Graph g = graph();
    
    // tag the graph as being in Te Reo Māori
    g.addTag(g, "transcript_language", "mi");
    
    // tag some words as being in NZ English
    g.addAnnotation(new Annotation().setLayerId("lang").setLabel("en-NZ")
                    .setStart(g.getOrCreateAnchorAt(40))
                    // 40."fox".45."jumps".50."over".60."the".70
                    .setEnd(g.getOrCreateAnchorAt(70))
                    .setParent(g.first("turn")));
      
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
      
    // use default configuration
    annotator.setTaskParameters(
      "tokenLayerId=word"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&phraseLanguageLayerId=lang"
      +"&targetLanguagePattern=en.*"
      +"&tagLayerId=unisyn"
      +"&lexicon=test.unisyn"
      +"&field=pron_disc"
      +"&firstVariantOnly=false"
      ); // don't strip primary stress and syllable markers
      
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("transcript language layer",
                 "transcript_language", annotator.getTranscriptLanguageLayerId());
    assertEquals("phrase language layer",
                 "lang", annotator.getPhraseLanguageLayerId());
    assertEquals("target language",
                 "en.*", annotator.getTargetLanguagePattern());
    assertNull("phone layer",
               annotator.getPhoneLayerId());
    assertEquals("tag layer",
                 "unisyn", annotator.getTagLayerId());
    assertFalse("stripSyllStress",
                annotator.getStripSyllStress());
    assertFalse("firstVariantOnly=false",
                annotator.getFirstVariantOnly());
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
                 "unisyn", outputLayers[0]);

    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 9, g.all("word").length);
    assertEquals("double check there are no pronunciations: "+Arrays.asList(g.all("phonemes")),
                 0, g.all("unisyn").length);
    // run the annotator
    annotator.transform(g);
    List<String> pronLabels = Arrays.stream(g.all("unisyn"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("Correct number of annotations "+pronLabels,
                 4, pronLabels.size());
    Iterator<String> prons = pronLabels.iterator();
    assertEquals("_'Vmps", prons.next());
    assertEquals("'5-v@r", prons.next());
    assertEquals("D@", prons.next());
    assertEquals("D'@", prons.next());
  }   

  /** Test that language-specific tagging works when all but some phrases are targeted,
   * and POS tagging works. */
  @Test public void mostlyEnglishAndPos() throws Exception {
      
    Graph g = graph();
      
    // tag the graph as being in English - strictly it should be the ISO code,
    // but "English" should also work
    g.addTag(g, "transcript_language", "en");

    // tag some words as being in Te Reo Māori
    g.addAnnotation(new Annotation().setLayerId("lang").setLabel("mi")
                    .setStart(g.getOrCreateAnchorAt(40))
                    // 40."fox".45."jumps".50."over".60."the".70
                    .setEnd(g.getOrCreateAnchorAt(70))
                    .setParent(g.first("turn")));

    // tag some as being in NZ English
    g.addAnnotation(new Annotation().setLayerId("lang").setLabel("en-NZ")
                    .setStart(g.getOrCreateAnchorAt(20))
                    // 20."quick".30."brown".40
                    .setEnd(g.getOrCreateAnchorAt(40))
                    .setParent(g.first("turn")));
      
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
      
    // use default configuration
    annotator.setTaskParameters(
      "tokenLayerId=word"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&phraseLanguageLayerId=lang"
      +"&targetLanguagePattern=en.*" // include en-NZ
      +"&tagLayerId=pos"
      +"&lexicon=test.unisyn"
      +"&field=pos"
      +"&firstVariantOnly=false"
      +"&caseSensitive=false"
      +"&stripSyllStress=on");
      
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("transcript language layer",
                 "transcript_language", annotator.getTranscriptLanguageLayerId());
    assertEquals("phrase language layer",
                 "lang", annotator.getPhraseLanguageLayerId());
    assertEquals("target language",
                 "en.*", annotator.getTargetLanguagePattern()); // includes en-NZ
    assertNull("phone layer",
               annotator.getPhoneLayerId());
    assertEquals("tag layer",
                 "pos", annotator.getTagLayerId());
    assertNotNull("tag layer was created",
                  schema.getLayer(annotator.getTagLayerId()));
    assertEquals("tag layer child of word",
                 "word", schema.getLayer(annotator.getTagLayerId()).getParentId());
    assertEquals("tag layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getTagLayerId()).getAlignment());
    assertEquals("tag layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getTagLayerId()).getType());
    assertFalse("firstVariantOnly=false",
                annotator.getFirstVariantOnly());
    assertTrue("tag layer allows peers (firstVariantOnly=false)",
               schema.getLayer(annotator.getTagLayerId()).getPeers());
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
                 "pos", outputLayers[0]);

    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 9, g.all("word").length);
    // run the annotator
    annotator.transform(g);
    List<String> posLabels = Arrays.stream(g.all("pos"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("Correct number of annotations "+posLabels,
                 14, posLabels.size());
    Iterator<String> pos = posLabels.iterator();
    assertEquals("The POS",
                 "DT", pos.next());
    assertEquals("Quick - Multiple pronunciations POS 1",
                 "JJ", pos.next());
    assertEquals("Quick - Multiple pronunciations POS 2",
                 "NNP", pos.next());
    assertEquals("Quick - Multiple pronunciations POS 3",
                 "NN", pos.next());
    assertEquals("Quick - Multiple pronunciations POS 4",
                 "RB", pos.next());
    assertEquals("Brown - Multiple pronunciations POS 1",
                 "JJ", pos.next());
    assertEquals("Brown - Multiple pronunciations POS 2",
                 "NNP", pos.next());
    assertEquals("Brown - Multiple pronunciations POS 3",
                 "NN", pos.next());
    assertEquals("Brown - Multiple pronunciations POS 4",
                 "VB", pos.next());
    assertEquals("Brown - Multiple pronunciations POS 5",
                 "VBP", pos.next());
    // skip "fox jumps over the"
    assertEquals("JJ", pos.next());
    assertEquals("Dog - Multiple pronunciations POS 1",
                 "NN", pos.next());
    assertEquals("dog - Multiple pronunciations POS 2",
                 "VBP", pos.next());
    assertEquals("Dog - Multiple pronunciations POS 3",
                 "VB", pos.next());

  }

  /** Test syllable recovery. */
  @Test public void syllableRecovery() throws Exception {
      
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);

    // annotator.getStatusObservers().add(s -> System.out.println(s));
    
    // use specified configuration
    annotator.setTaskParameters(
      "tokenLayerId=word"
      +"&transcriptLanguageLayerId="   // no transcript language layer
      +"&phraseLanguageLayerId="       // no phrase language layer
      +"&tagLayerId=syllable"          // non-default layer
      +"&lexicon=test.unisyn"
      +"&field=pron_disc"
      +"&phoneLayerId=phone");
    
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertNull("transcript language layer",
               annotator.getTranscriptLanguageLayerId());
    assertNull("phrase language layer",
               annotator.getPhraseLanguageLayerId());
    assertEquals("phone layer",
                 "phone", annotator.getPhoneLayerId());
    assertEquals("tag layer",
                 "syllable", annotator.getTagLayerId());
    assertFalse("stripSyllStress",
                annotator.getStripSyllStress());
    assertNotNull("tag layer was created",
                  schema.getLayer(annotator.getTagLayerId()));
    assertEquals("tag layer child of word",
                 "word", schema.getLayer(annotator.getTagLayerId()).getParentId());
    assertEquals("tag layer aligned",
                 Constants.ALIGNMENT_INTERVAL,
                 schema.getLayer(annotator.getTagLayerId()).getAlignment());
    assertEquals("tag layer type correct",
                 Constants.TYPE_IPA,
                 schema.getLayer(annotator.getTagLayerId()).getType());
    assertEquals("lexicon",
                 "test.unisyn", annotator.getLexicon());
    assertEquals("field",
                 "pron_disc", annotator.getField());
    assertTrue("tag layer allows peers",
               schema.getLayer(annotator.getTagLayerId()).getPeers());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("2 required layer: "+requiredLayers,
                 2, requiredLayers.size());
    assertTrue("phone required "+requiredLayers,
               requiredLayers.contains("phone"));
    assertTrue("word required "+requiredLayers,
               requiredLayers.contains("word"));
    String outputLayers[] = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                 1, outputLayers.length);
    assertEquals("output layer correct "+Arrays.asList(outputLayers),
                 "syllable", outputLayers[0]);
    
    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "The", firstWord.getLabel());
    
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 9, g.all("word").length);
    assertEquals("double check there are no syllables: "+Arrays.asList(g.all("syllable")),
                 0, g.all("freuency").length);
    // run the annotator
    assertEquals("phone layer",
                 "phone", annotator.phoneLayerId);
    annotator.transform(g);
    Annotation[] syllables = g.all("syllable");
    assertEquals("Correct number of tokens "+Arrays.asList(syllables),
                 10, syllables.length);
    String[] syllableLabels = {
      "D'@", "kw'Ik", "br'6n",
      // no entry for fox
      "_'Vmps",
      "'5", "v@r",
      "D@",
      "l'1z", "i",
      "d'$g"
    };
    String[] parentLabels = {
      "The", "quick", "brown",
      // no entry for fox
      "jumps",
      "over", "over",
      "the",
      "lazy", "lazy",
      "dog"
    };
    String[] startPhones = {
      "D", "k", "b",
      // no entry for fox
      "_",
      "5", "v",
      "D",
      "l", "I", // i->I
      "d"
    };
    String[] endPhones = {
      "@", "k", "n",
      // no entry for fox
      "s",
      "5", "@", // non-rhotic endiing
      "i", // @ -> i
      "z", "I", // i->I
      "g"
    };
    for (int i = 0; i < syllables.length; i++) {
      assertEquals("label " + i, syllableLabels[i], syllables[i].getLabel());
      assertEquals("parent layer " + i, "word", syllables[i].getParent().getLayerId());
      assertEquals("parent " + i, parentLabels[i], syllables[i].getParent().getLabel());
      assertEquals(
        "start phone " + i,
        startPhones[i],
        syllables[i].getStart().startOf("phone").iterator().next().getLabel());
    }
  }   

  /** Test syllable recovery using orthography. */
  @Test public void syllableRecoveryUsingOrthography() throws Exception {
      
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);

    // annotator.getStatusObservers().add(s -> System.out.println(s));
    
    // use specified configuration
    annotator.setTaskParameters(
      "tokenLayerId=orthography"
      +"&transcriptLanguageLayerId="   // no transcript language layer
      +"&phraseLanguageLayerId="       // no phrase language layer
      +"&tagLayerId=syllable"          // non-default layer
      +"&lexicon=test.unisyn"
      +"&field=pron_disc"
      +"&phoneLayerId=phone");
    
    assertEquals("token layer orthography",
                 "orthography", annotator.getTokenLayerId());
    assertNull("transcript language layer",
               annotator.getTranscriptLanguageLayerId());
    assertNull("phrase language layer",
               annotator.getPhraseLanguageLayerId());
    assertEquals("phone layer",
                 "phone", annotator.getPhoneLayerId());
    assertEquals("tag layer",
                 "syllable", annotator.getTagLayerId());
    assertFalse("stripSyllStress",
                annotator.getStripSyllStress());
    assertNotNull("tag layer was created",
                  schema.getLayer(annotator.getTagLayerId()));
    assertEquals("tag layer child of word",
                 "word", schema.getLayer(annotator.getTagLayerId()).getParentId());
    assertEquals("tag layer aligned",
                 Constants.ALIGNMENT_INTERVAL,
                 schema.getLayer(annotator.getTagLayerId()).getAlignment());
    assertEquals("tag layer type correct",
                 Constants.TYPE_IPA,
                 schema.getLayer(annotator.getTagLayerId()).getType());
    assertEquals("lexicon",
                 "test.unisyn", annotator.getLexicon());
    assertEquals("field",
                 "pron_disc", annotator.getField());
    assertTrue("tag layer allows peers",
               schema.getLayer(annotator.getTagLayerId()).getPeers());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("2 required layer: "+requiredLayers,
                 2, requiredLayers.size());
    assertTrue("phone required "+requiredLayers,
               requiredLayers.contains("phone"));
    assertTrue("orthography required "+requiredLayers,
               requiredLayers.contains("orthography"));
    String outputLayers[] = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                 1, outputLayers.length);
    assertEquals("output layer correct "+Arrays.asList(outputLayers),
                 "syllable", outputLayers[0]);
    
    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "The", firstWord.getLabel());
    
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 9, g.all("word").length);
    assertEquals("double check there are no syllables: "+Arrays.asList(g.all("syllable")),
                 0, g.all("freuency").length);
    // run the annotator
    assertEquals("phone layer",
                 "phone", annotator.phoneLayerId);
    annotator.transform(g);
    Annotation[] syllables = g.all("syllable");
    assertEquals("Correct number of tokens "+Arrays.asList(syllables),
                 10, syllables.length);
    String[] syllableLabels = {
      "D'@", "kw'Ik", "br'6n",
      // no entry for fox
      "_'Vmps",
      "'5", "v@r",
      "D@",
      "l'1z", "i",
      "d'$g"
    };
    String[] parentLabels = {
      "The", "quick", "brown",
      // no entry for fox
      "jumps",
      "over", "over",
      "the",
      "lazy", "lazy",
      "dog"
    };
    String[] startPhones = {
      "D", "k", "b",
      // no entry for fox
      "_",
      "5", "v",
      "D",
      "l", "I", // i->I
      "d"
    };
    String[] endPhones = {
      "@", "k", "n",
      // no entry for fox
      "s",
      "5", "@", // non-rhotic endiing
      "i", // @ -> i
      "z", "I", // i->I
      "g"
    };
    for (int i = 0; i < syllables.length; i++) {
      assertEquals("label " + i, syllableLabels[i], syllables[i].getLabel());
      assertEquals("parent layer " + i, "word", syllables[i].getParent().getLayerId());
      assertEquals("parent " + i, parentLabels[i], syllables[i].getParent().getLabel());
      assertEquals(
        "start phone " + i,
        startPhones[i],
        syllables[i].getStart().startOf("phone").iterator().next().getLabel());
    }
  }   

  /** Test syllable recovery to Unisyn labels from DISC phones. */
  @Test public void syllableRecoveryMismatchedEncoding() throws Exception {
      
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);

    // annotator.getStatusObservers().add(s -> System.out.println(s));
    
    // use specified configuration
    annotator.setTaskParameters(
      "tokenLayerId=word"
      +"&transcriptLanguageLayerId="   // no transcript language layer
      +"&phraseLanguageLayerId="       // no phrase language layer
      +"&tagLayerId=syllable"          // non-default layer
      +"&lexicon=test.unisyn"
      +"&field=pron_orig"
      +"&phoneLayerId=phone");
    
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertNull("transcript language layer",
               annotator.getTranscriptLanguageLayerId());
    assertNull("phrase language layer",
               annotator.getPhraseLanguageLayerId());
    assertEquals("phone layer",
                 "phone", annotator.getPhoneLayerId());
    assertEquals("tag layer",
                 "syllable", annotator.getTagLayerId());
    assertFalse("stripSyllStress",
                annotator.getStripSyllStress());
    assertNotNull("tag layer was created",
                  schema.getLayer(annotator.getTagLayerId()));
    assertEquals("tag layer child of word",
                 "word", schema.getLayer(annotator.getTagLayerId()).getParentId());
    assertEquals("tag layer aligned",
                 Constants.ALIGNMENT_INTERVAL,
                 schema.getLayer(annotator.getTagLayerId()).getAlignment());
    assertEquals("tag layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getTagLayerId()).getType());
    assertEquals("lexicon",
                 "test.unisyn", annotator.getLexicon());
    assertEquals("field",
                 "pron_orig", annotator.getField());
    assertTrue("tag layer allows peers",
               schema.getLayer(annotator.getTagLayerId()).getPeers());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("2 required layer: "+requiredLayers,
                 2, requiredLayers.size());
    assertTrue("phone required "+requiredLayers,
               requiredLayers.contains("phone"));
    assertTrue("word required "+requiredLayers,
               requiredLayers.contains("word"));
    String outputLayers[] = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                 1, outputLayers.length);
    assertEquals("output layer correct "+Arrays.asList(outputLayers),
                 "syllable", outputLayers[0]);

    // can get dictionary for this configuration with null ID
    assertNotNull("configuration dictionary available by passing null ID",
                  annotator.getDictionary(null));
    
    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "The", firstWord.getLabel());
    
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 9, g.all("word").length);
    assertEquals("double check there are no syllables: "+Arrays.asList(g.all("syllable")),
                 0, g.all("freuency").length);
    // run the annotator
    assertEquals("phone layer",
                 "phone", annotator.phoneLayerId);
    annotator.transform(g);
    Annotation[] syllables = g.all("syllable");
    assertEquals("Correct number of tokens "+Arrays.asList(syllables),
                 10, syllables.length);
    String[] syllableLabels = {
      "{ dh @ }", // uses the first pronunciation as the labels don't match
      "{ k w * i k }", "{ b r * ow n }",
      // no entry for fox
      "{ jh * uh m p }> s >",
      "{ * ou ", " v @r r }",
      "{ dh @ }", // uses the first pronunciation as the labels don't match
      "{ l * ei z }", "> ii >",
      "{ d * oo g }"
    };
    String[] parentLabels = {
      "The", "quick", "brown",
      // no entry for fox
      "jumps",
      "over", "over",
      "the",
      "lazy", "lazy",
      "dog"
    };
    String[] startPhones = {
      "D", "k", "b",
      // no entry for fox
      "_",
      "5", "v",
      "D",
      "l", "I", // i->I
      "d"
    };
    String[] endPhones = {
      "@", "k", "n",
      // no entry for fox
      "s",
      "5", "@", // non-rhotic endiing
      "i", // @ -> i
      "z", "I", // i->I
      "g"
    };
    for (int i = 0; i < syllables.length; i++) {
      assertEquals("label " + i, syllableLabels[i], syllables[i].getLabel());
      assertEquals("parent " + i, parentLabels[i], syllables[i].getParent().getLabel());
      assertEquals(
        "start phone " + i,
        startPhones[i],
        syllables[i].getStart().startOf("phone").iterator().next().getLabel());
    }
  }   

  /** Test that lexicons and corresponding dictionaries can be added and removed. */
  @Test public void lexiconManagement() throws Exception {

    // ensure there's no leftover version of the extra lexicon
    String error = annotator.deleteLexicon("temp");
    if (error != null) System.out.println(error);

    // starting condition: one lexicon with three fields
    List<String> ids = annotator.getDictionaryIds();
    assertEquals("Correct number of dictionaries: " + ids,
                 6, ids.size());
    String[] expectedIds = {
      "test.unisyn:wordform->pron_orig",
      "test.unisyn:wordform->pron_disc",
      "test.unisyn:wordform->enriched_orthography",
      "test.unisyn:wordform->pos",
      "test.unisyn:wordform->frequency",
      "test.unisyn:wordform->syllable_count"
    };
    for (String expected : expectedIds) {      
      assertTrue(expected, ids.contains(expected));
    }
    
    // add a lexicon, using first-space delimiter
    File file = new File(dir(), "temp.sampa");
    error = annotator.loadLexicon("temp", file);
    assertEquals("loadLexicon returns no error", "", error);
    // loading is in a separate thread
    while (annotator.getRunning()) {
      try {Thread.sleep(100);} catch(Exception exception) {}
    }

    // it's available as a dictionary
    ids = annotator.getDictionaryIds();
    assertEquals("New dictionaries present: " + ids,
                 12, ids.size());
    String[] moreExpectedIds = {
      "test.unisyn:wordform->pron_orig",
      "test.unisyn:wordform->pron_disc",
      "test.unisyn:wordform->enriched_orthography",
      "test.unisyn:wordform->pos",
      "test.unisyn:wordform->frequency",
      "test.unisyn:wordform->syllable_count",
      "temp:wordform->pron_orig",
      "temp:wordform->pron_disc",
      "temp:wordform->enriched_orthography",
      "temp:wordform->pos",
      "temp:wordform->frequency",
      "temp:wordform->syllable_count"
    };
    for (String expected : moreExpectedIds) {      
      assertTrue(expected, ids.contains(expected));
    }

    // can remove lexicons
    assertEquals("Can delete lexicon", "", annotator.deleteLexicon("temp"));
    ids = annotator.getDictionaryIds();
    assertEquals("Lexicon (and its dictionaries) were deleted: " + ids,
                 6, ids.size());
  }   

  /** Test CRUD operations on lexicon phoneme mappings. */
  @Test public void mappingManagement() throws Exception {

    // ensure there's no leftover version of the extra lexicon
    String error = annotator.deleteLexicon("temp");
    if (error != null) System.out.println(error);

    // add a lexicon, using first-space delimiter
    File file = new File(dir(), "temp.sampa");
    error = annotator.loadLexicon("temp", file);
    assertEquals("loadLexicon returns no error", "", error);
    // loading is in a separate thread
    while (annotator.getRunning()) {
      try {Thread.sleep(100);} catch(Exception exception) {}
    }

    try {
      // check mappings were created
      Collection<Map<String,String>> mappings = annotator.readDiscMappings("temp");
      assertNotNull("mappings returned", mappings);
      assertEquals(
        "right number of default mappings: " + mappings,
        /* should be 39 but is */20/* because Derby can't do case-sentivity properly*/,
        mappings.size());

      // delete all mappings
      String lastMapping = null;
      for (Map<String,String> mapping : mappings) {
        assertNull("Could delete mapping: " + mapping.get("phoneme_orig"),
                   annotator.deleteDiscMapping("temp", mapping.get("phoneme_orig")));
        lastMapping = mapping.get("phoneme_orig");
      }
      mappings = annotator.readDiscMappings("temp");
      assertEquals("There are now no mappings - " + mappings,
                   0, mappings.size());
      assertNotNull("deleting nonexisting mapping returns error",
                    annotator.deleteDiscMapping("temp", lastMapping));

      // create a mapping
      assertNull("mapping creation without error",
                 annotator.createDiscMapping("temp", "orig", "disc", "testing"));
      mappings = annotator.readDiscMappings("temp");
      assertEquals("There is now one mapping", 1, mappings.size());
      Map<String,String> mapping = mappings.iterator().next();
      assertEquals("correct phoneme_orig", "orig", mapping.get("phoneme_orig"));
      assertEquals("correct phoneme_disc", "disc", mapping.get("phoneme_disc"));
      assertEquals("correct note", "testing", mapping.get("note"));

      // cannot create the same mapping again
      assertNotNull("cannot create mapping that already exists",
                    annotator.createDiscMapping("temp", "orig", "disc2", "testing2"));

      // update mapping
      assertNull("can update existing mapping",
                 annotator.updateDiscMapping("temp", "orig", "disc-u", "testing-updated"));
      mappings = annotator.readDiscMappings("temp");
      assertEquals("There is still one mapping", 1, mappings.size());
      mapping = mappings.iterator().next();
      assertEquals("correct phoneme_orig", "orig", mapping.get("phoneme_orig"));
      assertEquals("correct phoneme_disc", "disc-u", mapping.get("phoneme_disc"));
      assertEquals("correct note", "testing-updated", mapping.get("note"));

      assertNotNull("cannot update nonexistent mapping",
                    annotator.updateDiscMapping("temp", "orig2", "disc2", "testing2"));
      
      assertNull("second mapping creation without error",
                 annotator.createDiscMapping("temp", "orig2", "disc2", "testing2"));
      mappings = annotator.readDiscMappings("temp");
      assertEquals("There are now two mappings", 2, mappings.size());
      for (Map<String,String> m : mappings) {
        String orig = m.get("phoneme_orig");
        if (orig.equals("orig")) {
          assertEquals("correct phoneme_disc 1", "disc-u", m.get("phoneme_disc"));
          assertEquals("correct note 1", "testing-updated", m.get("note"));
        } else { // orig2
          assertEquals("correct phoneme_disc 2", "disc2", m.get("phoneme_disc"));
          assertEquals("correct note 2", "testing2", m.get("note"));
        }
      } // next mapping

      assertNull("can update existing mapping 2",
                 annotator.updateDiscMapping("temp", "orig2", "disc2-u", "testing2-updated"));
      mappings = annotator.readDiscMappings("temp");
      assertEquals("There are still two mappings", 2, mappings.size());
      for (Map<String,String> m : mappings) {
        String orig = m.get("phoneme_orig");
        if (orig.equals("orig")) {
          assertEquals("correct phoneme_disc 1", "disc-u", m.get("phoneme_disc"));
          assertEquals("correct note 1", "testing-updated", m.get("note"));
        } else { // orig2
          assertEquals("correct phoneme_disc 2", "disc2-u", m.get("phoneme_disc"));
          assertEquals("correct note 2", "testing2-updated", m.get("note"));
        }
      } // next mapping
      
    } finally {
      // remove lexicon
      annotator.deleteLexicon("temp");
    }

    // no mapping now available
    Collection<Map<String,String>> mappings = annotator.readDiscMappings("temp");
    assertEquals("There are now no mappings - " + mappings,
                 0, mappings.size());
  }   

  /** Test whole-layer generation uses GraphStore.tagMatchingAnnotations correctly,
   * including language filtering. */
  @Test public void transformTranscriptsWithLanguageFiltering() {
    GraphStoreHarness store = new GraphStoreHarness();
    Graph g = graph();
    Schema schema = g.getSchema();
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&targetLanguagePattern=en.*"
        +"&tagLayerId=unisyn"
        +"&lexicon=test.unisyn"
        +"&field=pron_disc"
        +"&firstVariantOnly=false");
      
      // call tagMatchingAnnotations
      annotator.transformTranscripts(store, null);
    } catch(Exception exception) {
      fail(""+exception);
    }

    // check the right calls were made to the graph store
    assertEquals("aggregateMatchingAnnotations operation",
                 "DISTINCT", store.aggregateMatchingAnnotationsOperation);
    assertEquals(
      "aggregateMatchingAnnotations expression",
      "layer.id == 'word'"
      +" && /en.*/.test(first('lang').label ?? first('transcript_language').label)",
      store.aggregateMatchingAnnotationsExpression);
    
    assertEquals("tagMatchingAnnotations num labels: " + store.tagMatchingAnnotationsLabels,
                 2, store.tagMatchingAnnotationsLabels.size());
    assertEquals(
      "tagMatchingAnnotations layerId quick",
      "kw'Ik", store.tagMatchingAnnotationsLabels.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label ?? first('transcript_language').label)"
        +" && label == 'quick'"));
    assertEquals(
      "tagMatchingAnnotations layerId brown",
      "br'6n", store.tagMatchingAnnotationsLabels.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label ?? first('transcript_language').label)"
        +" && label == 'brown'"));
    
    assertEquals("tagMatchingAnnotations num layerIds: " + store.tagMatchingAnnotationsLayerIds,
                 2, store.tagMatchingAnnotationsLayerIds.size());
    assertEquals(
      "tagMatchingAnnotations layerId quick",
      "unisyn", store.tagMatchingAnnotationsLayerIds.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label ?? first('transcript_language').label)"
        +" && label == 'quick'"));
    assertEquals(
      "tagMatchingAnnotations layerId brown",
      "unisyn", store.tagMatchingAnnotationsLayerIds.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label ?? first('transcript_language').label)"
        +" && label == 'brown'"));
    
    assertEquals("tagMatchingAnnotations num confidences: "
                 + store.tagMatchingAnnotationsConfidences,
                 2, store.tagMatchingAnnotationsConfidences.size());
    assertEquals(
      "tagMatchingAnnotations layerId quick",
      Integer.valueOf(50), store.tagMatchingAnnotationsConfidences.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label ?? first('transcript_language').label)"
        +" && label == 'quick'"));
    assertEquals(
      "tagMatchingAnnotations layerId brown",
      Integer.valueOf(50), store.tagMatchingAnnotationsConfidences.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label ?? first('transcript_language').label)"
        +" && label == 'brown'"));
  }

  /** Test whole-layer generation uses GraphStore.tagMatchingAnnotations correctly,
   * including partial language filtering. */
  @Test public void transformTranscriptsWithPartialLanguageFiltering() {
    GraphStoreHarness store = new GraphStoreHarness();
    Graph g = graph();
    Schema schema = g.getSchema();
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId="
        +"&targetLanguagePattern=en.*"
        +"&tagLayerId=unisyn"
        +"&lexicon=test.unisyn"
        +"&field=pron_disc"
        +"&firstVariantOnly=false");
      
      // call tagMatchingAnnotations
      annotator.transformTranscripts(store, null);
    } catch(Exception exception) {
      fail("trancript lang only: "+exception);
    }

    // check the right calls were made to the graph store
    assertEquals("aggregateMatchingAnnotations operation",
                 "DISTINCT", store.aggregateMatchingAnnotationsOperation);
    assertEquals(
      "trancript lang only: aggregateMatchingAnnotations expression",
      "layer.id == 'word'"
      +" && /en.*/.test(first('transcript_language').label)",
      store.aggregateMatchingAnnotationsExpression);
    
    assertEquals("tagMatchingAnnotations num labels: " + store.tagMatchingAnnotationsLabels,
                 2, store.tagMatchingAnnotationsLabels.size());
    assertEquals(
      "trancript lang only: tagMatchingAnnotations layerId quick",
      "kw'Ik", store.tagMatchingAnnotationsLabels.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('transcript_language').label)"
        +" && label == 'quick'"));
    assertEquals(
      "trancript lang only: tagMatchingAnnotations layerId brown",
      "br'6n", store.tagMatchingAnnotationsLabels.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('transcript_language').label)"
        +" && label == 'brown'"));
    
    assertEquals("tagMatchingAnnotations num layerIds: " + store.tagMatchingAnnotationsLayerIds,
                 2, store.tagMatchingAnnotationsLayerIds.size());
    assertEquals(
      "trancript lang only: tagMatchingAnnotations layerId quick",
      "unisyn", store.tagMatchingAnnotationsLayerIds.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('transcript_language').label)"
        +" && label == 'quick'"));
    assertEquals(
      "trancript lang only: tagMatchingAnnotations layerId brown",
      "unisyn", store.tagMatchingAnnotationsLayerIds.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('transcript_language').label)"
        +" && label == 'brown'"));
    
    assertEquals("trancript lang only: tagMatchingAnnotations num confidences: "
                 + store.tagMatchingAnnotationsConfidences,
                 2, store.tagMatchingAnnotationsConfidences.size());
    assertEquals(
      "trancript lang only: tagMatchingAnnotations layerId quick",
      Integer.valueOf(50), store.tagMatchingAnnotationsConfidences.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('transcript_language').label)"
        +" && label == 'quick'"));
    assertEquals(
      "trancript lang only: tagMatchingAnnotations layerId brown",
      Integer.valueOf(50), store.tagMatchingAnnotationsConfidences.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('transcript_language').label)"
        +" && label == 'brown'"));

    store = new GraphStoreHarness();
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId="
        +"&phraseLanguageLayerId=lang"
        +"&targetLanguagePattern=en.*"
        +"&tagLayerId=unisyn"
        +"&lexicon=test.unisyn"
        +"&field=pron_disc"
        +"&firstVariantOnly=false");
      
      // call tagMatchingAnnotations
      annotator.transformTranscripts(store, null);
    } catch(Exception exception) {
      fail("phrase lang only: "+exception);
    }

    // check the right calls were made to the graph store
    assertEquals("phrase lang only: aggregateMatchingAnnotations operation",
                 "DISTINCT", store.aggregateMatchingAnnotationsOperation);
    assertEquals(
      "phrase lang only: aggregateMatchingAnnotations expression",
      "layer.id == 'word'"
      +" && /en.*/.test(first('lang').label)",
      store.aggregateMatchingAnnotationsExpression);
    
    assertEquals(
      "phrase lang only: tagMatchingAnnotations num labels: " + store.tagMatchingAnnotationsLabels,
      2, store.tagMatchingAnnotationsLabels.size());
    assertEquals(
      "tagMatchingAnnotations layerId quick",
      "kw'Ik", store.tagMatchingAnnotationsLabels.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label)"
        +" && label == 'quick'"));
    assertEquals(
      "phrase lang only: tagMatchingAnnotations layerId brown",
      "br'6n", store.tagMatchingAnnotationsLabels.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label)"
        +" && label == 'brown'"));
    
    assertEquals(
      "phrase lang only: tagMatchingAnnotations num layerIds: "
      + store.tagMatchingAnnotationsLayerIds,
      2, store.tagMatchingAnnotationsLayerIds.size());
    assertEquals(
      "phrase lang only: tagMatchingAnnotations layerId quick",
      "unisyn", store.tagMatchingAnnotationsLayerIds.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label)"
        +" && label == 'quick'"));
    assertEquals(
      "phrase lang only: tagMatchingAnnotations layerId brown",
      "unisyn", store.tagMatchingAnnotationsLayerIds.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label)"
        +" && label == 'brown'"));
    
    assertEquals("phrase lang only: tagMatchingAnnotations num confidences: "
                 + store.tagMatchingAnnotationsConfidences,
                 2, store.tagMatchingAnnotationsConfidences.size());
    assertEquals(
      "phrase lang only: tagMatchingAnnotations layerId quick",
      Integer.valueOf(50), store.tagMatchingAnnotationsConfidences.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label)"
        +" && label == 'quick'"));
    assertEquals(
      "phrase lang only: tagMatchingAnnotations layerId brown",
      Integer.valueOf(50), store.tagMatchingAnnotationsConfidences.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label)"
        +" && label == 'brown'"));
  }

  /** Test whole-layer generation uses GraphStore.tagMatchingAnnotations correctly,
   * excluding language filtering. */
  @Test public void transformTranscriptsWithoutLanguageFiltering() {
    GraphStoreHarness store = new GraphStoreHarness();
    Graph g = graph();
    Schema schema = g.getSchema();
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&targetLanguagePattern="
        +"&tagLayerId=unisyn"
        +"&lexicon=test.unisyn"
        +"&field=pron_disc"
        +"&firstVariantOnly=false"
        +"&stripSyllStress=true");
      
      // call tagMatchingAnnotations
      annotator.transformTranscripts(store, null);
    } catch(Exception exception) {
      fail(""+exception);
    }

    // check the right calls were made to the graph store
    assertEquals("aggregateMatchingAnnotations operation",
                 "DISTINCT", store.aggregateMatchingAnnotationsOperation);
    assertEquals(
      "aggregateMatchingAnnotations expression",
      "layer.id == 'word'",
      store.aggregateMatchingAnnotationsExpression);
    
    assertEquals("tagMatchingAnnotations num labels: " + store.tagMatchingAnnotationsLabels,
                 2, store.tagMatchingAnnotationsLabels.size());
    assertEquals(
      "tagMatchingAnnotations layerId quick",
      "kwIk", store.tagMatchingAnnotationsLabels.get(
        "layer.id == 'word'"
        +" && label == 'quick'"));
    assertEquals(
      "tagMatchingAnnotations layerId brown",
      "br6n", store.tagMatchingAnnotationsLabels.get(
        "layer.id == 'word'"
        +" && label == 'brown'"));
    
    assertEquals("tagMatchingAnnotations num layerIds: " + store.tagMatchingAnnotationsLayerIds,
                 2, store.tagMatchingAnnotationsLayerIds.size());
    assertEquals(
      "tagMatchingAnnotations layerId quick",
      "unisyn", store.tagMatchingAnnotationsLayerIds.get(
        "layer.id == 'word'"
        +" && label == 'quick'"));
    assertEquals(
      "tagMatchingAnnotations layerId brown",
      "unisyn", store.tagMatchingAnnotationsLayerIds.get(
        "layer.id == 'word'"
        +" && label == 'brown'"));
    
    assertEquals("tagMatchingAnnotations num confidences: "
                 + store.tagMatchingAnnotationsConfidences,
                 2, store.tagMatchingAnnotationsConfidences.size());
    assertEquals(
      "tagMatchingAnnotations layerId quick",
      Integer.valueOf(50), store.tagMatchingAnnotationsConfidences.get(
        "layer.id == 'word'"
        +" && label == 'quick'"));
    assertEquals(
      "tagMatchingAnnotations layerId brown",
      Integer.valueOf(50), store.tagMatchingAnnotationsConfidences.get(
        "layer.id == 'word'"
        +" && label == 'brown'"));
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
      new Layer("orthography", "Orthography").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true),
      new Layer("unisyn", "Pronunciation").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true),
      new Layer("phone", "Speech sounds").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true));
    // annotate a graph
    Graph g = new Graph()
      .setSchema(schema);
    g.setId("unit-test");
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
      
    Annotation the1 =
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("The")
                      .setStart(g.getOrCreateAnchorAt(10)).setEnd(g.getOrCreateAnchorAt(20))
                      .setParent(turn));
    Annotation quick =
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("quick")
                      .setStart(g.getOrCreateAnchorAt(20)).setEnd(g.getOrCreateAnchorAt(30))
                      .setParent(turn));
    Annotation brown =
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("brown")
                      .setStart(g.getOrCreateAnchorAt(30)).setEnd(g.getOrCreateAnchorAt(40))
                      .setParent(turn));
    Annotation fox =
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("fox")
                      .setStart(g.getOrCreateAnchorAt(40)).setEnd(g.getOrCreateAnchorAt(45))
                      .setParent(turn));
    Annotation jumps =
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("jumps")
                      .setStart(g.getOrCreateAnchorAt(45)).setEnd(g.getOrCreateAnchorAt(50))
                      .setParent(turn));
    Annotation over =
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("over")
                      .setStart(g.getOrCreateAnchorAt(50)).setEnd(g.getOrCreateAnchorAt(60))
                      .setParent(turn));
    Annotation the2 =
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("the")
                      .setStart(g.getOrCreateAnchorAt(60)).setEnd(g.getOrCreateAnchorAt(70))
                      .setParent(turn));
    Annotation lazy =
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("lazy")
                      .setStart(g.getOrCreateAnchorAt(70)).setEnd(g.getOrCreateAnchorAt(80))
                      .setParent(turn));
    Annotation dog =
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("dog")
                      .setStart(g.getOrCreateAnchorAt(80)).setEnd(g.getOrCreateAnchorAt(90))
                      .setParent(turn));

    // orthography
    g.createTag(the1, "orthography", "the");
    g.createTag(quick, "orthography", "quick");
    g.createTag(brown, "orthography", "brown");
    g.createTag(fox, "orthography", "fox");
    g.createTag(jumps, "orthography", "jumps");
    g.createTag(over, "orthography", "over");
    g.createTag(the2, "orthography", "the");
    g.createTag(lazy, "orthography", "lazy");
    g.createTag(dog, "orthography", "dog");

    // phones for syllable recovery
    
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("D")
                    .setStart(g.getOrCreateAnchorAt(10)).setEnd(g.getOrCreateAnchorAt(15))
                    .setParent(the1));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("@")
                    .setStart(g.getOrCreateAnchorAt(15)).setEnd(g.getOrCreateAnchorAt(20))
                    .setParent(the1));

    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("k")
                    .setStart(g.getOrCreateAnchorAt(20)).setEnd(g.getOrCreateAnchorAt(22))
                    .setParent(quick));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("w")
                    .setStart(g.getOrCreateAnchorAt(22)).setEnd(g.getOrCreateAnchorAt(25))
                    .setParent(quick));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("I")
                    .setStart(g.getOrCreateAnchorAt(25)).setEnd(g.getOrCreateAnchorAt(27))
                    .setParent(quick));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("k")
                    .setStart(g.getOrCreateAnchorAt(27)).setEnd(g.getOrCreateAnchorAt(30))
                    .setParent(quick));

    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("b")
                    .setStart(g.getOrCreateAnchorAt(30)).setEnd(g.getOrCreateAnchorAt(32))
                    .setParent(brown));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("r")
                    .setStart(g.getOrCreateAnchorAt(32)).setEnd(g.getOrCreateAnchorAt(35))
                    .setParent(brown));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("6")
                    .setStart(g.getOrCreateAnchorAt(35)).setEnd(g.getOrCreateAnchorAt(37))
                    .setParent(brown));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("n")
                    .setStart(g.getOrCreateAnchorAt(37)).setEnd(g.getOrCreateAnchorAt(40))
                    .setParent(brown));
    
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("f")
                    .setStart(g.getOrCreateAnchorAt(40)).setEnd(g.getOrCreateAnchorAt(41))
                    .setParent(fox));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("$")
                    .setStart(g.getOrCreateAnchorAt(41)).setEnd(g.getOrCreateAnchorAt(42))
                    .setParent(fox));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("k")
                    .setStart(g.getOrCreateAnchorAt(42)).setEnd(g.getOrCreateAnchorAt(43))
                    .setParent(fox));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("s")
                    .setStart(g.getOrCreateAnchorAt(43)).setEnd(g.getOrCreateAnchorAt(45))
                    .setParent(fox));
    
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("_")
                    .setStart(g.getOrCreateAnchorAt(45)).setEnd(g.getOrCreateAnchorAt(46))
                    .setParent(jumps));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("V")
                    .setStart(g.getOrCreateAnchorAt(46)).setEnd(g.getOrCreateAnchorAt(47))
                    .setParent(jumps));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("m")
                    .setStart(g.getOrCreateAnchorAt(47)).setEnd(g.getOrCreateAnchorAt(48))
                    .setParent(jumps));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("p")
                    .setStart(g.getOrCreateAnchorAt(48)).setEnd(g.getOrCreateAnchorAt(49))
                    .setParent(jumps));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("s")
                    .setStart(g.getOrCreateAnchorAt(49)).setEnd(g.getOrCreateAnchorAt(50))
                    .setParent(jumps));

    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("5")
                    .setStart(g.getOrCreateAnchorAt(50)).setEnd(g.getOrCreateAnchorAt(53))
                    .setParent(over));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("v")
                    .setStart(g.getOrCreateAnchorAt(53)).setEnd(g.getOrCreateAnchorAt(57))
                    .setParent(over));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("@")
                    .setStart(g.getOrCreateAnchorAt(57)).setEnd(g.getOrCreateAnchorAt(60))
                    .setParent(over));

    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("D")
                    .setStart(g.getOrCreateAnchorAt(60)).setEnd(g.getOrCreateAnchorAt(65))
                    .setParent(the2));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("i") // not @
                    .setStart(g.getOrCreateAnchorAt(65)).setEnd(g.getOrCreateAnchorAt(70))
                    .setParent(the2));

    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("l")
                    .setStart(g.getOrCreateAnchorAt(70)).setEnd(g.getOrCreateAnchorAt(72))
                    .setParent(lazy));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("4") // CHOICE not FACE
                    .setStart(g.getOrCreateAnchorAt(72)).setEnd(g.getOrCreateAnchorAt(75))
                    .setParent(lazy));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("z")
                    .setStart(g.getOrCreateAnchorAt(75)).setEnd(g.getOrCreateAnchorAt(77))
                    .setParent(lazy));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("I") // not i
                    .setStart(g.getOrCreateAnchorAt(77)).setEnd(g.getOrCreateAnchorAt(80))
                    .setParent(lazy));
    
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("d")
                    .setStart(g.getOrCreateAnchorAt(80)).setEnd(g.getOrCreateAnchorAt(83))
                    .setParent(dog));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("$")
                    .setStart(g.getOrCreateAnchorAt(83)).setEnd(g.getOrCreateAnchorAt(86))
                    .setParent(dog));
    g.addAnnotation(new Annotation().setLayerId("phone").setLabel("g")
                    .setStart(g.getOrCreateAnchorAt(87)).setEnd(g.getOrCreateAnchorAt(90))
                    .setParent(dog));

    return g;
  } // end of graph()

  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.annotator.flatlexicon.TestUnisynTagger");
  }
}
