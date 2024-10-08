//
// Copyright 2020-2023 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.annotator.cmudict;
	      
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
import nzilbb.annotator.cmudict.CMUDictionaryTagger;
import nzilbb.annotator.cmudict.CMUDictionary;
import nzilbb.sql.derby.DerbyConnectionFactory;

public class TestCMUDictionaryTagger {

  static CMUDictionaryTagger annotator = new CMUDictionaryTagger();
   
  @BeforeClass
  public static void install() throws Exception {

    System.out.println("Installing lexicon if necessary...");

    // find the current directory
    File dir = dir();

    // set the schema
    annotator.setSchema(graph().getSchema());

    // set the working directory
    annotator.setWorkingDirectory(dir);

    // use derby for relational database
    annotator.setRdbConnectionFactory(new DerbyConnectionFactory(dir));

    // set the annotator configuration, which will install the lexicon the first time (only)
    annotator.setConfig(annotator.getConfig());
      
    System.out.println("Lexicon installed.");
  }
	 
  public static File dir() throws Exception { 
    URL urlThisClass = TestCMUDictionaryTagger.class.getResource(
      TestCMUDictionaryTagger.class.getSimpleName() + ".class");
    File fThisClass = new File(urlThisClass.toURI());
    return fThisClass.getParentFile();
  }
   
  @Test public void defaultParameters() throws Exception {
      
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
      
    // use default configuration
    annotator.setTaskParameters(null);
      
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("transcript language layer",
                 "transcript_language", annotator.getTranscriptLanguageLayerId());
    assertEquals("phrase language layer",
                 "lang", annotator.getPhraseLanguageLayerId());
    assertEquals("pronunciation layer",
                 "phonemes", annotator.getPronunciationLayerId());
    assertNotNull("pronunciation layer was created",
                  schema.getLayer(annotator.getPronunciationLayerId()));
    assertEquals("pronunciation layer child of word",
                 "word", schema.getLayer(annotator.getPronunciationLayerId()).getParentId());
    assertEquals("pronunciation layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getPronunciationLayerId()).getAlignment());
    assertEquals("pronunciation layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getPronunciationLayerId()).getType());
    assertFalse("firstVariantOnly=false",
                annotator.getFirstVariantOnly());
    assertTrue("pronunciation layer allows peers (firstVariantOnly=false)",
               schema.getLayer(annotator.getPronunciationLayerId()).getPeers());
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
                 "I", firstWord.getLabel());
      
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
    assertEquals("AY1", prons.next());
    assertEquals("S AE1 NG", prons.next());
    assertEquals("AH0 N D", prons.next());
    assertEquals("Multiple pronunciations",
                 "AE1 N D", prons.next());
    assertEquals("W AO1 K T", prons.next());
    assertEquals("AH0 B AW1 T", prons.next());
    assertEquals("M AY1", prons.next());
    assertEquals("blogging-posting skipped as it's not in the dictionary",
                 "L AE1 Z AH0 L IY0", prons.next());

    // add a word
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("new")
                    .setStart(g.getOrCreateAnchorAt(90)).setEnd(g.getOrCreateAnchorAt(100))
                    .setParent(g.first("turn")));

    // change a word
    firstWord.setLabel("we");
      
    // run the annotator again
    annotator.transform(g);
    pronLabels = Arrays.stream(g.all("phonemes"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("two more pronunciation: "+pronLabels,
                 10, pronLabels.size());
    prons = pronLabels.iterator();
    assertEquals("changed label not re-annotated", // TODO do we really want this??
                 "AY1", prons.next());
    assertEquals("previous pron unchanged", "S AE1 NG", prons.next());
    assertEquals("previous pron unchanged", "AH0 N D", prons.next());
    assertEquals("previous pron unchanged", "AE1 N D", prons.next());
    assertEquals("previous pron unchanged", "W AO1 K T", prons.next());
    assertEquals("previous pron unchanged", "AH0 B AW1 T", prons.next());
    assertEquals("previous pron unchanged", "M AY1", prons.next());
    assertEquals("previous pron unchanged", "L AE1 Z AH0 L IY0", prons.next());
    assertEquals("new token has first pronunciation",
                 "N UW1", prons.next());
    assertEquals("new token has second pronunciation",
                 "N Y UW1", prons.next());

  }   

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
      +"&pronunciationLayerId=cmudict" // non-default layer
      +"&encoding=CMU"                 // CMU ARPAbet encoding
      +"&firstVariantOnly=on");        // firstVariantOnly
      
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertNull("transcript language layer",
               annotator.getTranscriptLanguageLayerId());
    assertNull("phrase language layer",
               annotator.getPhraseLanguageLayerId());
    assertEquals("pronunciation layer",
                 "cmudict", annotator.getPronunciationLayerId());
    assertNotNull("pronunciation layer was created",
                  schema.getLayer(annotator.getPronunciationLayerId()));
    Layer pronunciationLayer = schema.getLayer(annotator.getPronunciationLayerId());
    assertEquals("pronunciation layer child of word",
                 "word", pronunciationLayer.getParentId());
    assertEquals("pronunciation layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 pronunciationLayer.getAlignment());
    assertEquals("pronunciation layer type correct",
                 Constants.TYPE_STRING,
                 pronunciationLayer.getType());
    assertTrue("pronunciation layer has valid labels defined",
               pronunciationLayer.getValidLabels().size() > 0);
    assertTrue("valid labels are ARPAbet",
               pronunciationLayer.getValidLabels().containsKey("UH0"));
    String[] nonCMUARPAbet = { "AX1", "AX2", "AX0", "AXR", "DX" };
    for (String label : nonCMUARPAbet) {
      assertFalse("valid labels exclude non-CMU labels: " + label,
                  pronunciationLayer.getValidLabels().containsKey(label));
    }
    assertEquals("encoding",
                 "CMU", annotator.getEncoding());
    assertFalse("pronunciation layer disallows peers (firstVariantOnly=true)",
                schema.getLayer(annotator.getPronunciationLayerId()).getPeers());
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
                 "cmudict", outputLayers[0]);

    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "I", firstWord.getLabel());
      
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 9, g.all("word").length);
    assertEquals("double check there are no pronunciations: "+Arrays.asList(g.all("cmudict")),
                 0, g.all("cmudict").length);
    // run the annotator
    annotator.transform(g);
    List<String> pronLabels = Arrays.stream(g.all("cmudict"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("Correct number of tokens "+pronLabels,
                 7, pronLabels.size());
    Iterator<String> prons = pronLabels.iterator();
    assertEquals("AY1", prons.next());
    assertEquals("S AE1 NG", prons.next());
    assertEquals("First pronunciation only",
                 "AH0 N D", prons.next());
    assertEquals("Second pronunciation of 'and' skipped",
                 "W AO1 K T", prons.next());
    assertEquals("AH0 B AW1 T", prons.next());
    assertEquals("M AY1", prons.next());
    assertEquals("blogging-posting skipped as it's not in the dictionary",
                 "L AE1 Z AH0 L IY0", prons.next());

    // add a word
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("new")
                    .setStart(g.getOrCreateAnchorAt(90)).setEnd(g.getOrCreateAnchorAt(100))
                    .setParent(g.first("turn")));

    // change a word
    firstWord.setLabel("we");
      
    // run the annotator again
    annotator.transform(g);
    pronLabels = Arrays.stream(g.all("cmudict"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("one more pronunciation: "+pronLabels,
                 8, pronLabels.size());
    prons = pronLabels.iterator();
    assertEquals("changed label not re-annotated", // TODO do we really want this??
                 "AY1", prons.next());
    assertEquals("previous pron unchanged", "S AE1 NG", prons.next());
    assertEquals("previous pron unchanged", "AH0 N D", prons.next());
    assertEquals("previous pron unchanged", "W AO1 K T", prons.next());
    assertEquals("previous pron unchanged", "AH0 B AW1 T", prons.next());
    assertEquals("previous pron unchanged", "M AY1", prons.next());
    assertEquals("previous pron unchanged", "L AE1 Z AH0 L IY0", prons.next());
    assertEquals("new token has first pronunciation",
                 "N UW1", prons.next());

  }   

  @Test public void DISC() throws Exception {

    try {
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
        +"&pronunciationLayerId=cmudict" // non-default layer
        +"&encoding=DISC"                // DISC encoding
        +"&firstVariantOnly=on");        // firstVariantOnly
         
      assertEquals("token layer",
                   "word", annotator.getTokenLayerId());
      assertNull("transcript language layer",
                 annotator.getTranscriptLanguageLayerId());
      assertNull("phrase language layer",
                 annotator.getPhraseLanguageLayerId());
      assertEquals("pronunciation layer",
                   "cmudict", annotator.getPronunciationLayerId());
      assertNotNull("pronunciation layer was created",
                    schema.getLayer(annotator.getPronunciationLayerId()));
      Layer pronunciationLayer = schema.getLayer(annotator.getPronunciationLayerId());
      assertEquals("pronunciation layer child of word",
                   "word", pronunciationLayer.getParentId());
      assertEquals("pronunciation layer not aligned",
                   Constants.ALIGNMENT_NONE,
                   pronunciationLayer.getAlignment());
      assertEquals("pronunciation layer type correct",
                   Constants.TYPE_IPA,
                   pronunciationLayer.getType());
      assertTrue("pronunciation layer has valid labels defined",
                 pronunciationLayer.getValidLabels().size() > 0);
      assertTrue("valid labels are DISC",
                 pronunciationLayer.getValidLabels().containsKey("@"));
      assertEquals("encoding",
                   "DISC", annotator.getEncoding());
      assertFalse("pronunciation layer disallows peers (firstVariantOnly=true)",
                  schema.getLayer(annotator.getPronunciationLayerId()).getPeers());
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
                   "cmudict", outputLayers[0]);
         
      Annotation firstWord = g.first("word");
      assertEquals("double check the first word is what we think it is: "+firstWord,
                   "I", firstWord.getLabel());
         
      assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                   9, g.all("word").length);
      assertEquals("double check there are no pronunciations: "+Arrays.asList(g.all("cmudict")),
                   0, g.all("cmudict").length);
      // run the annotator
      annotator.transform(g);
      List<String> pronLabels = Arrays.stream(g.all("cmudict"))
        .map(annotation->annotation.getLabel()).collect(Collectors.toList());
      assertEquals("Correct number of tokens "+pronLabels,
                   8, pronLabels.size());
      Iterator<String> prons = pronLabels.iterator();
      assertEquals("2", prons.next());
      assertEquals("s{N", prons.next());
      assertEquals("First pronunciation only",
                   "@nd", prons.next());
      assertEquals("Second pronunciation of 'and' skipped, hesitation tagged",
                   "w@", prons.next());
      assertEquals("w$kt", prons.next());
      assertEquals("@b6t", prons.next());
      assertEquals("m2", prons.next());
      assertEquals("blogging-posting skipped as it's not in the dictionary",
                   "l{z@l@", prons.next());
         
    } finally {
      // reset encoding for other tests
      annotator.setEncoding("CMU");
    }

  }   

  @Test public void setInvalidTaskParameters() throws Exception {
      
    try {
      annotator.setTaskParameters(
        // doesn't exist in the schema
        "tokenLayerId=orthography"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&pronunciationLayerId=cmudict"
        +"&firstVariantOnly=on");
      fail("Should fail with nonexistent tokenLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        // doesn't exist in the schema
        +"&transcriptLanguageLayerId=language"
        +"&phraseLanguageLayerId=lang"
        +"&pronunciationLayerId=cmudict"
        +"&firstVariantOnly=on");
      fail("Should fail with nonexistent transcriptLanguageLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        // doesn't exist in the schema
        +"&phraseLanguageLayerId=language"
        +"&pronunciationLayerId=cmudict"
        +"&firstVariantOnly=on");       
      fail("Should fail with nonexistent phraseLanguageLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        // same as token layer
        +"&pronunciationLayerId=word"
        +"&firstVariantOnly=on");       
      fail("Should fail with pronunciationLayerId = tokenLayerId");
    } catch (InvalidConfigurationException x) {
    }

    // set firstVariantOnly = false for a layer that doesn't allow peers 
    annotator.getSchema().addLayer(
      new Layer("cmudict")
      .setAlignment(Constants.ALIGNMENT_NONE)
      // no peers allowed
      .setPeers(false)
      .setParentId(annotator.getSchema().getWordLayerId()));
    annotator.setTaskParameters(
      "tokenLayerId=word"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&phraseLanguageLayerId=lang"
      +"&pronunciationLayerId=cmudict"
      // all variants
      +"&firstVariantOnly=false");
    // no exception is thrown, but firstVariantOnly is now true
    assertTrue("firstVariantOnly has been corrected", annotator.getFirstVariantOnly());
  }   

  @Test public void hesitationToDISC() throws Exception {

    try {
      assertNull("Not DISC", annotator.hesitationToDISC("s~"));
         
      annotator.setEncoding("DISC");
      assertNull("Non-hesitation", annotator.hesitationToDISC("blog"));
         
      assertEquals("s~", "s@", annotator.hesitationToDISC("s~"));
      assertEquals("se~", "s@", annotator.hesitationToDISC("se~"));
      assertEquals("c~", "k@", annotator.hesitationToDISC("c~"));
      assertEquals("q~", "k@", annotator.hesitationToDISC("c~"));
      assertEquals("j~", "_@", annotator.hesitationToDISC("j~"));
      assertEquals("y~", "j@", annotator.hesitationToDISC("y~"));
      assertEquals("q~", "k@", annotator.hesitationToDISC("c~"));
      assertEquals("a~", "{", annotator.hesitationToDISC("a~"));
      assertEquals("e~", "E", annotator.hesitationToDISC("e~"));
      assertEquals("i~", "I", annotator.hesitationToDISC("i~"));
      assertEquals("o~", "Q", annotator.hesitationToDISC("o~"));
      assertEquals("u~", "V", annotator.hesitationToDISC("u~"));
      assertEquals("shi~", "S@", annotator.hesitationToDISC("shi~"));
      assertEquals("ph~", "f@", annotator.hesitationToDISC("ph~"));
      assertEquals("ng~", "N@", annotator.hesitationToDISC("ng~"));
      assertEquals("th~", "T@", annotator.hesitationToDISC("th~"));
      assertEquals("ch~", "J@", annotator.hesitationToDISC("ch~"));
      assertEquals("wh~", "hw@", annotator.hesitationToDISC("wh~"));
      assertEquals("gn~", "n@", annotator.hesitationToDISC("gn~"));
      assertEquals("kn~", "n@", annotator.hesitationToDISC("kn~"));
      assertEquals("pn~", "nj@", annotator.hesitationToDISC("pn~"));
      assertEquals("ps~", "s@", annotator.hesitationToDISC("ps~"));
      assertEquals("pt~", "t@", annotator.hesitationToDISC("pt~"));
      assertEquals("wr~", "r@", annotator.hesitationToDISC("wr~"));

    } finally {
      // reset encoding for other tests
      annotator.setEncoding("CMU");
    }
  }   

  @Test public void nonEnglish() throws Exception {
      
    Graph g = graph();

    // tag the graph as being in Te Reo Māori
    g.addTag(g, "transcript_language", "mi");
      
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
      
    // use default configuration
    annotator.setTaskParameters(null);
      
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("transcript language layer",
                 "transcript_language", annotator.getTranscriptLanguageLayerId());
    assertEquals("phrase language layer",
                 "lang", annotator.getPhraseLanguageLayerId());
    assertEquals("pronunciation layer",
                 "phonemes", annotator.getPronunciationLayerId());
    assertNotNull("pronunciation layer was created",
                  schema.getLayer(annotator.getPronunciationLayerId()));
    assertEquals("pronunciation layer child of word",
                 "word", schema.getLayer(annotator.getPronunciationLayerId()).getParentId());
    assertEquals("pronunciation layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getPronunciationLayerId()).getAlignment());
    assertEquals("pronunciation layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getPronunciationLayerId()).getType());
    assertFalse("firstVariantOnly=false",
                annotator.getFirstVariantOnly());
    assertTrue("pronunciation layer allows peers (firstVariantOnly=false)",
               schema.getLayer(annotator.getPronunciationLayerId()).getPeers());
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
                 "I", firstWord.getLabel());
      
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
    annotator.setSchema(schema);
      
    // use default configuration
    annotator.setTaskParameters(null);
      
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("transcript language layer",
                 "transcript_language", annotator.getTranscriptLanguageLayerId());
    assertEquals("phrase language layer",
                 "lang", annotator.getPhraseLanguageLayerId());
    assertEquals("pronunciation layer",
                 "phonemes", annotator.getPronunciationLayerId());
    assertNotNull("pronunciation layer was created",
                  schema.getLayer(annotator.getPronunciationLayerId()));
    assertEquals("pronunciation layer child of word",
                 "word", schema.getLayer(annotator.getPronunciationLayerId()).getParentId());
    assertEquals("pronunciation layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getPronunciationLayerId()).getAlignment());
    assertEquals("pronunciation layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getPronunciationLayerId()).getType());
    assertFalse("firstVariantOnly=false",
                annotator.getFirstVariantOnly());
    assertTrue("pronunciation layer allows peers (firstVariantOnly=false)",
               schema.getLayer(annotator.getPronunciationLayerId()).getPeers());
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
                 3, pronLabels.size());
    Iterator<String> prons = pronLabels.iterator();
    assertEquals("Annotation starts with 'walked'",
                 "W AO1 K T", prons.next());
    assertEquals("AH0 B AW1 T", prons.next());
    assertEquals("M AY1", prons.next());

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
    annotator.setSchema(schema);
      
    // use default configuration
    annotator.setTaskParameters(null);
      
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("transcript language layer",
                 "transcript_language", annotator.getTranscriptLanguageLayerId());
    assertEquals("phrase language layer",
                 "lang", annotator.getPhraseLanguageLayerId());
    assertEquals("pronunciation layer",
                 "phonemes", annotator.getPronunciationLayerId());
    assertNotNull("pronunciation layer was created",
                  schema.getLayer(annotator.getPronunciationLayerId()));
    assertEquals("pronunciation layer child of word",
                 "word", schema.getLayer(annotator.getPronunciationLayerId()).getParentId());
    assertEquals("pronunciation layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getPronunciationLayerId()).getAlignment());
    assertEquals("pronunciation layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getPronunciationLayerId()).getType());
    assertFalse("firstVariantOnly=false",
                annotator.getFirstVariantOnly());
    assertTrue("pronunciation layer allows peers (firstVariantOnly=false)",
               schema.getLayer(annotator.getPronunciationLayerId()).getPeers());
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
    assertEquals("AY1", prons.next());
    assertEquals("S AE1 NG", prons.next());
    assertEquals("AH0 N D", prons.next());
    assertEquals("AE1 N D", prons.next());
    assertEquals("skipped 'walked about my'",
                 "L AE1 Z AH0 L IY0", prons.next());

  }
  
  /** Test whole-layer generation uses GraphStore.tagMatchingAnnotations correctly,
   * including language filtering. */
  @Test public void transformTranscriptsWithLanguageFiltering() {
    GraphStoreHarness store = new GraphStoreHarness();
    Graph g = graph();
    Schema schema = g.getSchema();
    try {
      annotator.setTaskParameters(null);
      
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
      "K W IH1 K", store.tagMatchingAnnotationsLabels.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label ?? first('transcript_language').label)"
        +" && label == 'quick'"));
    assertEquals(
      "tagMatchingAnnotations layerId brown",
      "B R AW1 N", store.tagMatchingAnnotationsLabels.get(
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


  /** Test dictionary registration. */
  @Test public void dictionaryRegistration() throws Exception {

    List<String> ids = annotator.getDictionaryIds();
    assertEquals("there's only one dictionary: " + ids,
                 1, ids.size());
    // (I don't actually care what the ID is)
    Dictionary dict = annotator.getDictionary(ids.iterator().next());
    assertTrue("Dictionary is the right type: " + dict.getClass().getName(),
               dict instanceof CMUDictionary);

    assertNotNull("null dictionary ID is supported",
                  annotator.getDictionary(null));
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
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("w~")
                    .setStart(g.getOrCreateAnchorAt(40)).setEnd(g.getOrCreateAnchorAt(45))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("walked")
                    .setStart(g.getOrCreateAnchorAt(45)).setEnd(g.getOrCreateAnchorAt(50))
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
    org.junit.runner.JUnitCore.main("nzilbb.annotator.cmudict.TestCMUDictionaryTagger");
  }
}
