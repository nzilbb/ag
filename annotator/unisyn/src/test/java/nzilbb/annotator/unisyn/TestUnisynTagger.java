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
                 "word", annotator.getTokenLayerId());
    assertEquals("transcript language layer",
                 "transcript_language", annotator.getTranscriptLanguageLayerId());
    assertEquals("phrase language layer",
                 "lang", annotator.getPhraseLanguageLayerId());
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

    // add a word
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("a")
                    .setStart(g.getOrCreateAnchorAt(90)).setEnd(g.getOrCreateAnchorAt(100))
                    .setParent(g.first("turn")));

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
        "tokenLayerId=orthography"
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
      +"&firstVariantOnly=false");
    
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("transcript language layer",
                 "transcript_language", annotator.getTranscriptLanguageLayerId());
    assertEquals("phrase language layer",
                 "lang", annotator.getPhraseLanguageLayerId());
    assertEquals("target language",
                 "en.*", annotator.getTargetLanguagePattern());
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

    // dictionary works
    Dictionary dictionary = annotator.getDictionary("temp:wordform->pron_orig");
    List<String> entries = dictionary.lookup("quick");
    assertEquals("one entry returned - " + entries,
                 1, entries.size());
    assertEquals("entry correct", "\"kw@k", entries.get(0));

    entries = dictionary.lookup("the");
    assertEquals("multiple entries - " + entries, 3, entries.size());
    assertEquals("first entry", "D@", entries.get(0));
    assertEquals("second entry", "Di:", entries.get(1));
    assertEquals("third entry", "\"Di:", entries.get(2));

    // can add/remove entry
    entries = dictionary.lookup("fox");
    assertEquals("no entries - " + entries, 0, entries.size());
    
    dictionary.add("fox", "\"fQks");
    entries = dictionary.lookup("fox");
    assertEquals("one entries - " + entries, 1, entries.size());
    assertEquals("only entry", "\"fQks", entries.get(0));
    
    dictionary.remove("fox", "\"fQks");
    entries = dictionary.lookup("fox");
    assertEquals("no entries - " + entries, 0, entries.size());

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
      assertEquals("right number of default mappings: " + mappings,
                   39, mappings.size());

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
      new Layer("unisyn", "Pronunciation").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
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
      
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("The")
                    .setStart(g.getOrCreateAnchorAt(10)).setEnd(g.getOrCreateAnchorAt(20))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("quick")
                    .setStart(g.getOrCreateAnchorAt(20)).setEnd(g.getOrCreateAnchorAt(30))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("brown")
                    .setStart(g.getOrCreateAnchorAt(30)).setEnd(g.getOrCreateAnchorAt(40))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("fox")
                    .setStart(g.getOrCreateAnchorAt(40)).setEnd(g.getOrCreateAnchorAt(45))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("jumps")
                    .setStart(g.getOrCreateAnchorAt(45)).setEnd(g.getOrCreateAnchorAt(50))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("over")
                    .setStart(g.getOrCreateAnchorAt(50)).setEnd(g.getOrCreateAnchorAt(60))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("the")
                    .setStart(g.getOrCreateAnchorAt(60)).setEnd(g.getOrCreateAnchorAt(70))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("lazy")
                    .setStart(g.getOrCreateAnchorAt(70)).setEnd(g.getOrCreateAnchorAt(80))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("dog")
                    .setStart(g.getOrCreateAnchorAt(80)).setEnd(g.getOrCreateAnchorAt(90))
                    .setParent(turn));
    return g;
  } // end of graph()

  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.annotator.flatlexicon.TestUnisynTagger");
  }
}
