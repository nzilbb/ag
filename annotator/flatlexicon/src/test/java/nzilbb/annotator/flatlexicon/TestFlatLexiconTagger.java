//
// Copyright 2022 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.annotator.flatlexicon;
	      
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
import nzilbb.sql.derby.DerbyConnectionFactory;

public class TestFlatLexiconTagger {
  
  static FlatLexiconTagger annotator = new FlatLexiconTagger();
  
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
    File file = new File(dir, "a-z.csv");
    String error = annotator.loadLexicon(
      file.getName(), ",", "", "", "Word,Pronunciation,Frequency", true, file);
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
    URL urlThisClass = TestFlatLexiconTagger.class.getResource(
      TestFlatLexiconTagger.class.getSimpleName() + ".class");
    File fThisClass = new File(urlThisClass.toURI());
    return fThisClass.getParentFile();
  }

  /** Ensure default configuration works. */
  @Test public void defaultParameters() throws Exception {
      
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);

    // use default configuration
    annotator.setTaskParameters(null);
    
    assertEquals("dictionary",
                 "a-z.csv:Word->Pronunciation", annotator.getDictionary());
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
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getTagLayerId()).getType());
    assertEquals("strip",
                 "", annotator.getStrip());
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
                 11, pronLabels.size());
    Iterator<String> prons = pronLabels.iterator();
    assertEquals("Variant order is order encountered in file",
                 "ð ə", prons.next());
    assertEquals("Multiple pronunciations",
                 "ð i:", prons.next());
    assertEquals("k w ɪ k", prons.next());
    assertEquals("b r aʊ n", prons.next());
    assertEquals("f ɒ k s", prons.next());
    assertEquals("ʤ ʌ m p s", prons.next());
    assertEquals("'əʊ - v ə", prons.next());
    assertEquals("ð ə", prons.next());
    assertEquals("ð i:", prons.next());
    assertEquals("l 'eɪ - z i:", prons.next());
    assertEquals("d ɒ g", prons.next());

    // add a word
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("dog")
                    .setStart(g.getOrCreateAnchorAt(90)).setEnd(g.getOrCreateAnchorAt(100))
                    .setParent(g.first("turn")));

    // change a word
    firstWord.setLabel("a");
      
    // run the annotator again
    annotator.transform(g);
    pronLabels = Arrays.stream(g.all("phonemes"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("one more pronunciation: "+pronLabels,
                 12, pronLabels.size());
    prons = pronLabels.iterator();
    assertEquals("First word is still tagged, because existing tags aren't removed",
                 "ð ə", prons.next());
    assertEquals("First word is still tagged, because existing tags aren't removed",
                 "ð i:", prons.next());
    assertEquals("k w ɪ k", prons.next());
    assertEquals("b r aʊ n", prons.next());
    assertEquals("f ɒ k s", prons.next());
    assertEquals("ʤ ʌ m p s", prons.next());
    assertEquals("'əʊ - v ə", prons.next());
    assertEquals("ð ə", prons.next());
    assertEquals("ð i:", prons.next());
    assertEquals("l 'eɪ - z i:", prons.next());
    assertEquals("d ɒ g", prons.next());
    assertEquals("New token",
                 "d ɒ g", prons.next());

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
      +"&dictionary=a-z.csv:Word->Frequency"
      +"&firstVariantOnly=on"
      +"&strip=");
      
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertNull("transcript language layer",
               annotator.getTranscriptLanguageLayerId());
    assertNull("phrase language layer",
               annotator.getPhraseLanguageLayerId());
    assertNull("target language",
               annotator.getTargetLanguagePattern());
    assertFalse("case/accent sensitivity",
                annotator.getExactMatch());
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
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getTagLayerId()).getType());
    assertEquals("lexicon",
                 "a-z.csv:Word->Frequency", annotator.getDictionary());
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
                 9, freqLabels.size());
    Iterator<String> freqs = freqLabels.iterator();
    assertEquals("First entry for 'the'",
                 "0", freqs.next());
    assertEquals("Not second entry for 'the'",
                 "2", freqs.next());
    assertEquals("3", freqs.next());
    assertEquals("4", freqs.next());
    assertEquals("5", freqs.next());
    assertEquals("6", freqs.next());
    assertEquals("0", freqs.next());
    assertEquals("7", freqs.next());
    assertEquals("8", freqs.next());

  }   

  /** Test validation. */
  @Test public void setInvalidTaskParameters() throws Exception {
      
    try {
      annotator.setTaskParameters(
        // doesn't exist in the schema
        "tokenLayerId=orthography"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&tagLayerId=phonemes"
        +"&firstVariantOnly=on"
        +"&dictionary=a-z.csv:Word->Pronunciation");
      fail("Should fail with nonexistent tokenLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        // doesn't exist in the schema
        +"&transcriptLanguageLayerId=language"
        +"&phraseLanguageLayerId=lang"
        +"&tagLayerId=phonemes"
        +"&firstVariantOnly=on"
        +"&dictionary=a-z.csv:Word->Pronunciation");
      fail("Should fail with nonexistent transcriptLanguageLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        // doesn't exist in the schema
        +"&phraseLanguageLayerId=language"
        +"&tagLayerId=phonemes"
        +"&firstVariantOnly=on"
        +"&dictionary=a-z.csv:Word->Pronunciation");
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
        +"&dictionary=a-z.csv:Word->Pronunciation");
      fail("Should fail with pronunciationLayerId = tokenLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&tagLayerId=phonemes"
        +"&firstVariantOnly=on"
        // not specified
        +"&dictionary=");
      fail("Should fail with no dictionary");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&tagLayerId=phonemes"
        +"&firstVariantOnly=on"
        // nonexistent
        +"&dictionary=nonexistent:Word->Pronunciation");
      fail("Should fail with nonexistent lexicon");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&tagLayerId=phonemes"
        +"&firstVariantOnly=on"
        +"&dictionary=a-z.csv:nonexistent->Pronunciation");
      fail("Should fail with nonexistent key field");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&tagLayerId=phonemes"
        +"&firstVariantOnly=on"
        +"&dictionary=a-z.csv:Word->nonexistent");
      fail("Should fail with nonexistent value field");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&tagLayerId=phonemes"
        +"&firstVariantOnly=on"
        +"&dictionary=a-z.csv:Word->Pronunciation"
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
      +"&dictionary=a-z.csv:Word->Frequency"
      +"&targetLanguagePattern=");
    // no exception is thrown, but firstVariantOnly is now true
    assertTrue("firstVariantOnly has been corrected", annotator.getFirstVariantOnly());

    // set caseSensitive instead of exactMatch - for backwards compatibility
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
      +"&dictionary=a-z.csv:Word->Frequency"
      +"&targetLanguagePattern="
      +"&caseSensitive=on");
    // no exception is thrown, but firstVariantOnly is now true
    assertTrue("Backwards compatibility: caseSensitive used instead of exactMatch",
               annotator.getExactMatch());
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
      +"&tagLayerId=phonemes"
      +"&dictionary=a-z.csv:Word->Pronunciation"
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
                 "phonemes", annotator.getTagLayerId());
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
    assertEquals("No tokens annotated "+pronLabels,
                 0, pronLabels.size());
  }   

  /** Test that language-specific tagging works when only phrases are targeted, and also
   * that the strip setting works */
  @Test public void mostlyNonEnglishAndStrip() throws Exception {
      
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
      +"&tagLayerId=phonemes"
      +"&dictionary=a-z.csv:Word->Pronunciation"
      +"&firstVariantOnly=false"
      +"&strip=]'-"); // strip primary stress and syllable markers
      
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("transcript language layer",
                 "transcript_language", annotator.getTranscriptLanguageLayerId());
    assertEquals("phrase language layer",
                 "lang", annotator.getPhraseLanguageLayerId());
    assertEquals("target language",
                 "en.*", annotator.getTargetLanguagePattern());
    assertEquals("tag layer",
                 "phonemes", annotator.getTagLayerId());
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
    assertEquals("strip",
                 "]'-", annotator.getStrip());
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
                 "phonemes", outputLayers[0]);

    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 9, g.all("word").length);
    assertEquals("double check there are no pronunciations: "+Arrays.asList(g.all("phonemes")),
                 0, g.all("phonemes").length);
    // run the annotator
    annotator.transform(g);
    List<String> pronLabels = Arrays.stream(g.all("phonemes"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("Correct number of annotations "+pronLabels,
                 5, pronLabels.size());
    Iterator<String> prons = pronLabels.iterator();
    assertEquals("f ɒ k s", prons.next());
    assertEquals("ʤ ʌ m p s", prons.next());
    assertEquals("əʊ v ə", prons.next());
    assertEquals("variant order is order encountered in file - ð ə first",
                 "ð ə", prons.next());
    assertEquals("variant order is order encountered in file - ð i: second",
                 "ð i:", prons.next());

  }   

  /** Test that language-specific tagging works when all but some phrases are targeted */
  @Test public void mostlyEnglish() throws Exception {
      
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
      +"&targetLanguagePattern=en.*"
      +"&tagLayerId=phonemes"
      +"&dictionary=a-z.csv:Word->Pronunciation"
      +"&firstVariantOnly=false"
      +"&exactMatch=false"
      +"&strip=");
      
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("transcript language layer",
                 "transcript_language", annotator.getTranscriptLanguageLayerId());
    assertEquals("phrase language layer",
                 "lang", annotator.getPhraseLanguageLayerId());
    assertEquals("target language",
                 "en.*", annotator.getTargetLanguagePattern());
    assertEquals("tag layer",
                 "phonemes", annotator.getTagLayerId());
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
    assertFalse("exactMatch=false",
                annotator.getExactMatch());
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
                 9, g.all("word").length);
    assertEquals("double check there are no pronunciations: "+Arrays.asList(g.all("phonemes")),
                 0, g.all("phonemes").length);
    // run the annotator
    annotator.transform(g);
    List<String> pronLabels = Arrays.stream(g.all("phonemes"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("Correct number of annotations "+pronLabels,
                 6, pronLabels.size());
    Iterator<String> prons = pronLabels.iterator();
    assertEquals("Variant order is order encountered in file",
                 "ð ə", prons.next());
    assertEquals("Multiple pronunciations",
                 "ð i:", prons.next());
    assertEquals("NZE phrase is tagged",
                 "k w ɪ k", prons.next());
    assertEquals("NZE phrase is tagged",
                 "b r aʊ n", prons.next());
    assertEquals("'fox jumps over the' is skipped",
                 "l 'eɪ - z i:", prons.next());
    assertEquals("d ɒ g", prons.next());

  }
   
  /** Test that case-sensitive matching works. */
  /*TODO Case sensitivity doesn't work with Derby @Test */
  public void exactMatch() throws Exception {
      
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
      +"&targetLanguagePattern=en.*"
      +"&tagLayerId=phonemes"
      +"&dictionary=a-z.csv:Word->Pronunciation"
      +"&firstVariantOnly=false"
      +"&exactMatch=on"
      +"&strip=");
      
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("transcript language layer",
                 "transcript_language", annotator.getTranscriptLanguageLayerId());
    assertEquals("phrase language layer",
                 "lang", annotator.getPhraseLanguageLayerId());
    assertEquals("target language",
                 "en.*", annotator.getTargetLanguagePattern());
    assertEquals("tag layer",
                 "phonemes", annotator.getTagLayerId());
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
    assertTrue("exactMatch=true",
                annotator.getExactMatch());
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
                 9, g.all("word").length);
    assertEquals("double check there are no pronunciations: "+Arrays.asList(g.all("phonemes")),
                 0, g.all("phonemes").length);
    // run the annotator
    annotator.transform(g);
    List<String> pronLabels = Arrays.stream(g.all("phonemes"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("Correct number of annotations "+pronLabels,
                 4, pronLabels.size());
    Iterator<String> prons = pronLabels.iterator();
    // "The" is not tagged because case doesn't match dictionary "the"
    assertEquals("NZE phrase is tagged",
                 "k w ɪ k", prons.next());
    assertEquals("NZE phrase is tagged",
                 "b r aʊ n", prons.next());
    assertEquals("'fox jumps over the' is skipped",
                 "l 'eɪ - z i:", prons.next());
    assertEquals("d ɒ g", prons.next());

  }
   
  /** Test that lexicons and corresponding dictionaries can be added and removed. */
  @Test public void lexiconManagement() throws Exception {

    // ensure there's no leftover version of the extra lexicon
    String error = annotator.deleteLexicon("dict");
    if (error != null) System.out.println(error);

    // starting condition: one lexicon with three fields
    List<String> ids = annotator.getDictionaryIds();
    assertEquals("Correct number of dictionaries: " + ids,
                 6, ids.size());
    assertTrue("a-z.csv:Word->Pronunciation",
               ids.contains("a-z.csv:Word->Pronunciation"));
    assertTrue("a-z.csv:Word->Frequency",
               ids.contains("a-z.csv:Word->Frequency"));
    assertTrue("a-z.csv:Pronunciation->Word",
               ids.contains("a-z.csv:Pronunciation->Word"));
    assertTrue("a-z.csv:Pronunciation->Frequency",
               ids.contains("a-z.csv:Pronunciation->Frequency"));
    assertTrue("a-z.csv:Frequency->Word",
               ids.contains("a-z.csv:Frequency->Word"));
    assertTrue("a-z.csv:Frequency->Pronunciation",
               ids.contains("a-z.csv:Frequency->Pronunciation"));

    // add a lexicon, using first-space delimiter
    File file = new File(dir(), "a-z.dict");
    error = annotator.loadLexicon(
      "dict", " - ", "", "", "type - phonemes", false, file);
    assertEquals("loadLexicon returns no error", "", error);
    // loading is in a separate thread
    while (annotator.getRunning()) {
      try {Thread.sleep(100);} catch(Exception exception) {}
    }

    // it's available as a dictionary
    ids = annotator.getDictionaryIds();
    assertEquals("New dictionaries present: " + ids,
                 8, ids.size());
    assertTrue("a-z.csv:Word->Pronunciation",
               ids.contains("a-z.csv:Word->Pronunciation"));
    assertTrue("a-z.csv:Word->Frequency",
               ids.contains("a-z.csv:Word->Frequency"));
    assertTrue("a-z.csv:Pronunciation->Word",
               ids.contains("a-z.csv:Pronunciation->Word"));
    assertTrue("a-z.csv:Pronunciation->Frequency",
               ids.contains("a-z.csv:Pronunciation->Frequency"));
    assertTrue("a-z.csv:Frequency->Word",
               ids.contains("a-z.csv:Frequency->Word"));
    assertTrue("a-z.csv:Frequency->Pronunciation",
               ids.contains("a-z.csv:Frequency->Pronunciation"));
    assertTrue("dict:type->phonemes",
               ids.contains("dict:type->phonemes"));
    assertTrue("dict:phonemes->type",
               ids.contains("dict:phonemes->type"));

    // dictionary works
    Dictionary dictionary = annotator.getDictionary("dict:type->phonemes");
    FlatLexicon lexicon = (FlatLexicon)dictionary;
    // TODO case sensitivity not supported with Derby
    // lexicon.setExactMatch(true);
    // List<String> entries = dictionary.lookup("QUÍCK");
    // assertEquals("entry returned, case-insentitive, accent sensitive",
    //              1, entries.size());
    // assertEquals("entry correct", "k w ɪ k", entries.get(0));

    // while we've got a dictionary, check the case/accent sensitivity
    // NB with the test RDBMS used for testing - Derby - accent sensitivity can't
    // be tested, so we look internally at the pre-translation SQL used for queries
    lexicon.setExactMatch(true);
    assertTrue("MySQL for case-sensitive:" + lexicon.rawQuery,
               lexicon.rawQuery.endsWith("WHERE \"type\" = BINARY ? ORDER BY serial"));
    // Can't support case-sensitivity for Derby as well as MySQL, so we prioritise MySQL
    assertTrue("Derby SQL for case-sensitive: " + lexicon.translatedQuery,
               lexicon.translatedQuery.endsWith("WHERE \"type\" = ? ORDER BY serial"));
    lexicon.setExactMatch(false);
    assertTrue("MySQL for case-insensitive: " + lexicon.rawQuery,
               lexicon.rawQuery.endsWith("WHERE \"type\" = ? ORDER BY serial"));
    assertTrue("Derby SQL for case-insensitive: " + lexicon.translatedQuery,
               lexicon.translatedQuery.endsWith("WHERE \"type\" = ? ORDER BY serial"));
    
    lexicon.setExactMatch(false);
    List<String> entries = dictionary.lookup("THE");
    assertEquals("first line is not ignored", 2, entries.size());
    assertEquals("first entry correct (order is serial)", "ð ə", entries.get(0));
    assertEquals("second entry correct", "ð i:", entries.get(1));

    // specific Polish example
    entries = dictionary.lookup("życia");
    assertEquals("Polish entry returned",
                 1, entries.size());
    assertEquals("Polish entry correct", "'ʐɨ-ʨa", entries.get(0));
    
    // can remove lexicons
    assertEquals("Can delete lexicon", "", annotator.deleteLexicon("dict"));
    ids = annotator.getDictionaryIds();
    assertEquals("Lexicon (and its dictionaries) were deleted: " + ids,
                 6, ids.size());
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
        +"&tagLayerId=phonemes"
        +"&dictionary=a-z.csv:Word->Pronunciation"
        +"&firstVariantOnly=false"
        // Not exact match, so it's DISTINCT instead of DISTINCT BINARY,
        // and == instead of === below
        //+"&exactMatch=on"
        +"&strip=");
      
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
      "k w ɪ k", store.tagMatchingAnnotationsLabels.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label ?? first('transcript_language').label)"
        +" && label == 'quick'"));
    assertEquals(
      "tagMatchingAnnotations layerId brown",
      "b r aʊ n", store.tagMatchingAnnotationsLabels.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label ?? first('transcript_language').label)"
        +" && label == 'brown'"));
    
    assertEquals("tagMatchingAnnotations num layerIds: " + store.tagMatchingAnnotationsLayerIds,
                 2, store.tagMatchingAnnotationsLayerIds.size());
    assertEquals(
      "tagMatchingAnnotations layerId quick",
      "phonemes", store.tagMatchingAnnotationsLayerIds.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label ?? first('transcript_language').label)"
        +" && label == 'quick'"));
    assertEquals(
      "tagMatchingAnnotations layerId brown",
      "phonemes", store.tagMatchingAnnotationsLayerIds.get(
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
        +"&tagLayerId=phonemes"
        +"&dictionary=a-z.csv:Word->Pronunciation"
        +"&firstVariantOnly=false"
        +"&exactMatch=on"
        +"&strip=");
      
      // call tagMatchingAnnotations
      annotator.transformTranscripts(store, null);
    } catch(Exception exception) {
      fail("trancript lang only: "+exception);
    }
    
    // check the right calls were made to the graph store
    assertEquals("aggregateMatchingAnnotations operation",
                 "DISTINCT BINARY", store.aggregateMatchingAnnotationsOperation);
    assertEquals(
      "trancript lang only: aggregateMatchingAnnotations expression",
      "layer.id == 'word'"
      +" && /en.*/.test(first('transcript_language').label)",
      store.aggregateMatchingAnnotationsExpression);
    
    assertEquals("tagMatchingAnnotations num labels: " + store.tagMatchingAnnotationsLabels,
                 2, store.tagMatchingAnnotationsLabels.size());
    assertEquals(
      "trancript lang only: tagMatchingAnnotations layerId quick",
      "k w ɪ k", store.tagMatchingAnnotationsLabels.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('transcript_language').label)"
        +" && label === 'quick'"));
    assertEquals(
      "trancript lang only: tagMatchingAnnotations layerId brown",
      "b r aʊ n", store.tagMatchingAnnotationsLabels.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('transcript_language').label)"
        +" && label === 'brown'"));
    
    assertEquals("tagMatchingAnnotations num layerIds: " + store.tagMatchingAnnotationsLayerIds,
                 2, store.tagMatchingAnnotationsLayerIds.size());
    assertEquals(
      "trancript lang only: tagMatchingAnnotations layerId quick",
      "phonemes", store.tagMatchingAnnotationsLayerIds.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('transcript_language').label)"
        +" && label === 'quick'"));
    assertEquals(
      "trancript lang only: tagMatchingAnnotations layerId brown",
      "phonemes", store.tagMatchingAnnotationsLayerIds.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('transcript_language').label)"
        +" && label === 'brown'"));
    
    assertEquals("trancript lang only: tagMatchingAnnotations num confidences: "
                 + store.tagMatchingAnnotationsConfidences,
                 2, store.tagMatchingAnnotationsConfidences.size());
    assertEquals(
      "trancript lang only: tagMatchingAnnotations layerId quick",
      Integer.valueOf(50), store.tagMatchingAnnotationsConfidences.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('transcript_language').label)"
        +" && label === 'quick'"));
    assertEquals(
      "trancript lang only: tagMatchingAnnotations layerId brown",
      Integer.valueOf(50), store.tagMatchingAnnotationsConfidences.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('transcript_language').label)"
        +" && label === 'brown'"));

    store = new GraphStoreHarness();
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId="
        +"&phraseLanguageLayerId=lang"
        +"&targetLanguagePattern=en.*"
        +"&tagLayerId=phonemes"
        +"&dictionary=a-z.csv:Word->Pronunciation"
        +"&firstVariantOnly=false"
        +"&exactMatch=on"
        +"&strip=");
      
      // call tagMatchingAnnotations
      annotator.transformTranscripts(store, null);
    } catch(Exception exception) {
      fail("phrase lang only: "+exception);
    }

    // check the right calls were made to the graph store
    assertEquals("phrase lang only: aggregateMatchingAnnotations operation",
                 "DISTINCT BINARY", store.aggregateMatchingAnnotationsOperation);
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
      "k w ɪ k", store.tagMatchingAnnotationsLabels.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label)"
        +" && label === 'quick'"));
    assertEquals(
      "phrase lang only: tagMatchingAnnotations layerId brown",
      "b r aʊ n", store.tagMatchingAnnotationsLabels.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label)"
        +" && label === 'brown'"));
    
    assertEquals(
      "phrase lang only: tagMatchingAnnotations num layerIds: "
      + store.tagMatchingAnnotationsLayerIds,
      2, store.tagMatchingAnnotationsLayerIds.size());
    assertEquals(
      "phrase lang only: tagMatchingAnnotations layerId quick",
      "phonemes", store.tagMatchingAnnotationsLayerIds.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label)"
        +" && label === 'quick'"));
    assertEquals(
      "phrase lang only: tagMatchingAnnotations layerId brown",
      "phonemes", store.tagMatchingAnnotationsLayerIds.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label)"
        +" && label === 'brown'"));
    
    assertEquals("phrase lang only: tagMatchingAnnotations num confidences: "
                 + store.tagMatchingAnnotationsConfidences,
                 2, store.tagMatchingAnnotationsConfidences.size());
    assertEquals(
      "phrase lang only: tagMatchingAnnotations layerId quick",
      Integer.valueOf(50), store.tagMatchingAnnotationsConfidences.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label)"
        +" && label === 'quick'"));
    assertEquals(
      "phrase lang only: tagMatchingAnnotations layerId brown",
      Integer.valueOf(50), store.tagMatchingAnnotationsConfidences.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label)"
        +" && label === 'brown'"));
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
        +"&tagLayerId=phonemes"
        +"&dictionary=a-z.csv:Word->Pronunciation"
        +"&firstVariantOnly=false"
        +"&exactMatch=on"
        +"&strip=");
      
      // call tagMatchingAnnotations
      annotator.transformTranscripts(store, null);
    } catch(Exception exception) {
      fail(""+exception);
    }

    // check the right calls were made to the graph store
    assertEquals("aggregateMatchingAnnotations operation",
                 "DISTINCT BINARY", store.aggregateMatchingAnnotationsOperation);
    assertEquals(
      "aggregateMatchingAnnotations expression",
      "layer.id == 'word'",
      store.aggregateMatchingAnnotationsExpression);
    
    assertEquals("tagMatchingAnnotations num labels: " + store.tagMatchingAnnotationsLabels,
                 2, store.tagMatchingAnnotationsLabels.size());
    assertEquals(
      "tagMatchingAnnotations layerId quick",
      "k w ɪ k", store.tagMatchingAnnotationsLabels.get(
        "layer.id == 'word'"
        +" && label === 'quick'"));
    assertEquals(
      "tagMatchingAnnotations layerId brown",
      "b r aʊ n", store.tagMatchingAnnotationsLabels.get(
        "layer.id == 'word'"
        +" && label === 'brown'"));
    
    assertEquals("tagMatchingAnnotations num layerIds: " + store.tagMatchingAnnotationsLayerIds,
                 2, store.tagMatchingAnnotationsLayerIds.size());
    assertEquals(
      "tagMatchingAnnotations layerId quick",
      "phonemes", store.tagMatchingAnnotationsLayerIds.get(
        "layer.id == 'word'"
        +" && label === 'quick'"));
    assertEquals(
      "tagMatchingAnnotations layerId brown",
      "phonemes", store.tagMatchingAnnotationsLayerIds.get(
        "layer.id == 'word'"
        +" && label === 'brown'"));
    
    assertEquals("tagMatchingAnnotations num confidences: "
                 + store.tagMatchingAnnotationsConfidences,
                 2, store.tagMatchingAnnotationsConfidences.size());
    assertEquals(
      "tagMatchingAnnotations layerId quick",
      Integer.valueOf(50), store.tagMatchingAnnotationsConfidences.get(
        "layer.id == 'word'"
        +" && label === 'quick'"));
    assertEquals(
      "tagMatchingAnnotations layerId brown",
      Integer.valueOf(50), store.tagMatchingAnnotationsConfidences.get(
        "layer.id == 'word'"
        +" && label === 'brown'"));
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
      new Layer("phonemes", "Pronunciation").setAlignment(Constants.ALIGNMENT_NONE)
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
    org.junit.runner.JUnitCore.main("nzilbb.annotator.flatlexicon.TestFlatLexiconTagger");
  }
}
