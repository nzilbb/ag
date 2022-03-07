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

package nzilbb.annotator.basservices;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;
import nzilbb.ag.Anchor;
import nzilbb.ag.Annotation;
import nzilbb.ag.Change;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.GraphMediaProvider;
import nzilbb.ag.Layer;
import nzilbb.ag.MediaFile;
import nzilbb.ag.PermissionException;
import nzilbb.ag.Schema;
import nzilbb.ag.StoreException;
import nzilbb.ag.automation.Dictionary;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.UsesRelationalDatabase;
import nzilbb.util.IO;

public class TestBASAnnotator {

  static BASAnnotator annotator = new BASAnnotator();

  /** Infer the directory of the tests. */
  public static File dir() throws Exception { 
    URL urlThisClass = TestBASAnnotator.class.getResource(
      TestBASAnnotator.class.getSimpleName() + ".class");
    File fThisClass = new File(urlThisClass.toURI());
    return fThisClass.getParentFile();
  }

  
  /** Ensure default (null) task parameters return an error. */
  @Test public void defaultParameters() throws Exception {
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    //annotator.getStatusObservers().add(status->System.out.println(status));
    
    try { // use default configuration
      annotator.setTaskParameters(null);
      fail("Should fail with default configuration");
    } catch (InvalidConfigurationException x) {
    }
  }   

  /** Ensure valid MAUSBasic parameters don't raise errors, and change the schema when
   * appropriate.*/ 
  @Test public void MAUSBasicParameters() throws Exception {
    
    Graph g = graph();
    Schema schema = g.getSchema();
    // set phonemes layer to type=ipa so we can test the type is copied
    schema.getLayer("phonemes").setType(Constants.TYPE_IPA);
    annotator.setSchema(schema);

    // configure for system layer update
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&service=MAUSBasic"
      +"&phonemeEncoding=disc"
      +"&targetLanguagePattern=en-NZ"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&utteranceTagLayerId=mausBasic"
      +"&participantTagLayerId="
      +"&wordAlignmentLayerId=word"
      +"&phoneAlignmentLayerId=segment");
    
    // layers are created as required
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&service=MAUSBasic"
      +"&phonemeEncoding=disc"
      +"&targetLanguagePattern=en-NZ"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&utteranceTagLayerId=utterance_bas"     // nonexistent
      +"&participantTagLayerId=participant_bas" // nonexistent
      +"&wordAlignmentLayerId=word_alignment"   // nonexistent
      +"&phoneAlignmentLayerId=phone");         // nonexistent
    Layer layer = annotator.getSchema().getLayer("utterance_bas");
    assertNotNull("utterance_bas layer created", layer);
    layer = annotator.getSchema().getLayer("participant_bas");
    assertNotNull("participant_bas layer created", layer);
    //TODO layer = annotator.getSchema().getLayer("word_alignment");
    // assertNotNull("word_alignment layer created", layer);
    layer = annotator.getSchema().getLayer("phone");
    assertNotNull("phone layer created", layer);
    assertEquals("phone layer type", Constants.TYPE_IPA, layer.getType());
    assertTrue("phone layer peers", layer.getPeers());
  }   

  /** Ensure that invalid task parameters generate errors. */
  @Test public void setInvalidTaskParameters() throws Exception {
    
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=nonexistent"
        +"&service=MAUSBasic"
        +"&phonemeEncoding=disc"
        +"&targetLanguagePattern=en-NZ"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&utteranceTagLayerId=mausBasic"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=segment");
      fail("Should fail with nonexistent orthographyLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=word"
        +"&service=MAUSBasic"
        +"&phonemeEncoding=disc"
        +"&targetLanguagePattern=en-NZ"
        +"&transcriptLanguageLayerId=nonexistent"
        +"&utteranceTagLayerId=mausBasic"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=segment");
      fail("Should fail with nonexistent transcriptLanguageLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=word"
        +"&service="
        +"&phonemeEncoding=disc"
        +"&targetLanguagePattern=en-NZ"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&utteranceTagLayerId=mausBasic"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=segment");
      fail("Should fail with no service");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=word"
        +"&service=invalid"
        +"&phonemeEncoding=disc"
        +"&targetLanguagePattern=en-NZ"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&utteranceTagLayerId=mausBasic"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=segment");
      fail("Should fail with invalid service");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=word"
        +"&service=MAUSBasic"
        +"&phonemeEncoding=disc"
        +"&targetLanguagePattern=en-NZ"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&utteranceTagLayerId=mausBasic"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId="
        +"&phoneAlignmentLayerId=segment");
      fail("Should fail with no wordAlignmentLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=word"
        +"&service=MAUSBasic"
        +"&phonemeEncoding=disc"
        +"&targetLanguagePattern=en-NZ"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&utteranceTagLayerId=mausBasic"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=");
      fail("Should fail with no phoneAlignmentLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=word"
        +"&service=MAUSBasic"
        +"&phonemeEncoding=disc"
        +"&targetLanguagePattern=*"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&utteranceTagLayerId=mausBasic"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=phone");
      fail("Should fail with invalid targetLanguagePattern regular expression");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=word"
        +"&service=G2P"
        +"&phonemeEncoding=disc"
        +"&targetLanguagePattern=en-NZ"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&utteranceTagLayerId=mausBasic"
        +"&pronunciationLayerId=");
      fail("Should fail with no pronunciationLayerId");
    } catch (InvalidConfigurationException x) {
    }
  }

  /** Ensure MAUSBasic produces alignments. */ 
  @Test public void MAUSBasicTranformFragmentsDISC() throws Exception {
    
    Graph f = fragment();
    Schema schema = f.getSchema();
    annotator.setSchema(schema);
    // annotator.getStatusObservers().add(status->System.out.println(status));

    assertNotNull("fragment has a language",
                  f.sourceGraph().first("transcript_language"));
    assertEquals("fragment language correct",
                 "en-NZ", f.sourceGraph().first("transcript_language").getLabel());
    assertEquals("text is correct",
                 "saved up some money he bought property",
                 Arrays.stream(f.labels(schema.getWordLayerId()))
                 .collect(Collectors.joining(" ")).trim());
    
    // configure for system layer update
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&service=MAUSBasic"
      +"&phonemeEncoding=disc"
      +"&targetLanguagePattern=en-NZ"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&utteranceTagLayerId=mausBasic"
      +"&participantTagLayerId="
      +"&wordAlignmentLayerId=word"
      +"&phoneAlignmentLayerId=segment");
    
    final Vector<Graph> results = new Vector<Graph>();
    annotator.transformFragments(
      Arrays.stream(new Graph[] { f }), graph -> { results.add(graph); });
    
    assertEquals("One utterance " + results, 1, results.size());
    Graph aligned = results.elementAt(0);
    assertTrue("Original graph is edited", f == aligned);
    
    Annotation[] words = aligned.all("word");
    assertEquals("Seven words " + Arrays.asList(words), 7, words.length);
    
    Annotation[] phones = aligned.all("segment");
    assertEquals("24 phones " + Arrays.asList(phones), 24, phones.length);
    String[] labels = {
      "s", "1", "v", "d",
      "@", "p",
      "s", "@", "m",
      "m", "@", "n", "i",
      "i",
      "b", "$", "t",
      "p", "r", "Q", "p", "@", "L", "i" };
    for (int p = 0; p < phones.length; p++) {      
      assertEquals("DISC phone label " + p, labels[p], phones[p].getLabel());
      assertEquals("Phone label confidence " + p + " " + phones[p],
                   Constants.CONFIDENCE_AUTOMATIC,
                   phones[p].getConfidence().intValue());
      if (p > 0) { // first phone might coincide with start and be CONFIDENCE_MANUAL
        assertEquals("Phone start confidence " + p + " " + phones[p].getStartId(),
                     Constants.CONFIDENCE_AUTOMATIC,
                     phones[p].getStart().getConfidence().intValue());
      }
      if (p < phones.length - 1) { // last phone might coincide with end and be CONFIDENCE_MANUAL
        assertEquals("Phone end confidence " + p + " " + phones[p].getEndId(),
                     Constants.CONFIDENCE_AUTOMATIC,
                     phones[p].getEnd().getConfidence().intValue());
      }
      if (phones[p].getStart().startOf("word").size() == 0) { // not a word boundary
        assertEquals("Phone start shared with previous end " + p,
                     phones[p-1].getEnd(), phones[p].getStart());
      }
    } // next phone    
  }   

  /** Test alignment of full graph works, and encoding phones as SAMPA. */
  @Test public void graphTransformMAUSBasicSampa() throws Exception {
    // annotator.getStatusObservers().add(status->System.out.println(status));
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    assertNotNull("fragment has a language",
                  g.sourceGraph().first("transcript_language"));
    assertEquals("fragment language correct",
                 "en-NZ", g.first("transcript_language").getLabel());
    assertEquals("text is correct",
                 "saved up some money he bought property",
                 Arrays.stream(g.labels(schema.getWordLayerId()))
                 .collect(Collectors.joining(" ")).trim());
    
    // configure for system layer update
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&service=MAUSBasic"
      +"&phonemeEncoding=sampa"
      +"&targetLanguagePattern=en-NZ"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&utteranceTagLayerId=utterance_bas"
      +"&participantTagLayerId=participant_bas"
      +"&wordAlignmentLayerId=word"
      +"&phoneAlignmentLayerId=segment");
    Layer layer = annotator.getSchema().getLayer("utterance_bas");
    assertNotNull("utterance_bas layer created", layer);
    layer = annotator.getSchema().getLayer("participant_bas");
    assertNotNull("participant_bas layer created", layer);

    g.trackChanges();
    annotator.transform(g);
        
    Annotation[] words = g.all("word");
    assertEquals("Seven words " + Arrays.asList(words), 7, words.length);
    
    Annotation[] phones = g.all("segment");
    assertEquals("24 phones " + Arrays.asList(phones), 24, phones.length);
    String[] labels = {
      "s", "{I", "v", "d",
      "6", "p",
      "s", "6", "m",
      "m", "6", "n", "i:",
      "i:",
      "b", "o:", "t",
      "p", "r", "O", "p", "@", "4", "i:" };
    for (int p = 0; p < phones.length; p++) {      
      assertEquals("SAMPA phone label " + p, labels[p], phones[p].getLabel());
      assertEquals("Phone label confidence " + p + " " + phones[p],
                   Constants.CONFIDENCE_AUTOMATIC,
                   phones[p].getConfidence().intValue());
      if (p > 0) { // first phone might coincide with start and be CONFIDENCE_MANUAL
        assertEquals("Phone start confidence " + p + " " + phones[p].getStartId(),
                     Constants.CONFIDENCE_AUTOMATIC,
                     phones[p].getStart().getConfidence().intValue());
      }
      if (p < phones.length - 1) { // last phone might coincide with end and be CONFIDENCE_MANUAL
        assertEquals("Phone end confidence " + p + " " + phones[p].getEndId(),
                     Constants.CONFIDENCE_AUTOMATIC,
                     phones[p].getEnd().getConfidence().intValue());
      }
      if (phones[p].getStart().startOf("word").size() == 0) { // not a word boundary
        assertEquals("Phone start shared with previous end " + p,
                     phones[p-1].getEnd(), phones[p].getStart());
      }
    } // next phone    
  }   

  /** Test adding alignments independent of original word alignments. */
  @Test public void alignToPhraseLayers() throws Exception {
    Graph f = fragment();
    Schema schema = f.getSchema();
    annotator.setSchema(schema);
    // annotator.getStatusObservers().add(status->System.out.println(status));
    
    assertNotNull("fragment has a language",
                  f.sourceGraph().first("transcript_language"));
    assertEquals("fragment language correct",
                 "en-NZ", f.sourceGraph().first("transcript_language").getLabel());
    assertEquals("text is correct",
                 "saved up some money he bought property",
                 Arrays.stream(f.labels(schema.getWordLayerId()))
                 .collect(Collectors.joining(" ")).trim());
    
    // configure for system layer update
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&service=MAUSBasic"
      +"&phonemeEncoding=disc"
      +"&targetLanguagePattern=" // targetLanguagePattern can be blank = all languages
      +"&transcriptLanguageLayerId=transcript_language"
      +"&utteranceTagLayerId=bas_maus"
      +"&participantTagLayerId="
      +"&wordAlignmentLayerId=bas_word"
      +"&phoneAlignmentLayerId=bas_phone");
    
    Layer layer = annotator.getSchema().getLayer("bas_maus");
    assertNotNull("bas_maus layer created", layer);
    assertEquals("bas_maus phrase layer",
                 "turn", layer.getParentId());    
    assertFalse("bas_maus not saturated",
                layer.getSaturated());    
    layer = annotator.getSchema().getLayer("bas_word");
    assertNotNull("bas_word layer created", layer);
    assertEquals("bas_word phrase layer",
                 "turn", layer.getParentId());    
    assertFalse("bas_word not saturated",
                layer.getSaturated());    
    layer = annotator.getSchema().getLayer("bas_phone");
    assertNotNull("bas_phone layer created", layer);
    assertEquals("phone layer type", Constants.TYPE_IPA, layer.getType());
    assertEquals("bas_phone phrase layer",
                 "turn", layer.getParentId());    
    assertFalse("bas_phone not saturated",
                layer.getSaturated());
    
    final Vector<Graph> results = new Vector<Graph>();
    annotator.transformFragments(
      Arrays.stream(new Graph[] { f }), graph -> { results.add(graph); });
    
    assertEquals("One utterance " + results, 1, results.size());
    Graph aligned = results.elementAt(0);
    assertTrue("Original graph is edited", f == aligned);
    
    Annotation[] words = aligned.all("word");
    assertEquals("Seven words " + Arrays.asList(words), 7, words.length);
    Annotation[] basWords = aligned.all("bas_word");
    assertEquals("One BAS word token per original word token " + Arrays.asList(basWords),
                 words.length, basWords.length);
    Double[] wordStarts = { 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0 };
    for (int w = 0; w < words.length; w++) {
      assertEquals("Word start " + w + " ("+words[w]+") unchanged",
                   wordStarts[w], words[w].getStart().getOffset());
      assertEquals("BAS word label " + w + " correct",
                   basWords[w].getLabel(), words[w].getLabel());
    } // next word      
    
    Annotation[] phones = aligned.all("bas_phone");
    assertEquals("24 phones " + Arrays.asList(phones), 24, phones.length);
    String[] labels = {
      "s", "1", "v", "d",
      "@", "p",
      "s", "@", "m",
      "m", "@", "n", "i",
      "i",
      "b", "$", "t",
      "p", "r", "Q", "p", "@", "L", "i" };
    for (int p = 0; p < phones.length; p++) {      
      assertEquals("DISC phone label " + p, labels[p], phones[p].getLabel());
      if (p > 0) { // first phone might coincide with start and be CONFIDENCE_MANUAL
        assertEquals("Phone start confidence " + p + " " + phones[p].getStartId(),
                     Constants.CONFIDENCE_AUTOMATIC,
                     phones[p].getStart().getConfidence().intValue());
      }
      if (p < phones.length - 1) { // last phone might coincide with end and be CONFIDENCE_MANUAL
        assertEquals("Phone end confidence " + p + " " + phones[p].getEndId(),
                     Constants.CONFIDENCE_AUTOMATIC,
                     phones[p].getEnd().getConfidence().intValue());
      }
      if (phones[p].getStart().startOf("bas_word").size() == 0) { // not a word boundary
        assertEquals("Phone start shared with previous end " + p,
                     phones[p-1].getEnd(), phones[p].getStart());
      }
    } // next phone    
  }   

  /** Ensure G2P produces pronunciations and they're correctly translated to DISC. */ 
  @Test public void G2PDISC() throws Exception {
    
    Graph f = fragment();
    Schema schema = f.getSchema();
    annotator.setSchema(schema);
    // annotator.getStatusObservers().add(status->System.out.println(status));

    assertNotNull("fragment has a language",
                  f.sourceGraph().first("transcript_language"));
    assertEquals("fragment language correct",
                 "en-NZ", f.sourceGraph().first("transcript_language").getLabel());
    assertEquals("text is correct",
                 "saved up some money he bought property",
                 Arrays.stream(f.labels(schema.getWordLayerId()))
                 .collect(Collectors.joining(" ")).trim());
    
    // configure for system layer update
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&service=G2P"
      +"&phonemeEncoding=disc"
      +"&targetLanguagePattern=en-NZ"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&pronunciationLayerId=phonemes");
    
    final Vector<Graph> results = new Vector<Graph>();
    annotator.transformFragments(
      Arrays.stream(new Graph[] { f }), graph -> { results.add(graph); });
    
    assertEquals("One utterance " + results, 1, results.size());
    Graph aligned = results.elementAt(0);
    assertTrue("Original graph is edited", f == aligned);
    
    Annotation[] words = aligned.all("word");
    assertEquals("Seven words " + Arrays.asList(words), 7, words.length);
    
    Annotation[] pronunciations = aligned.all("phonemes");
    assertEquals("One pronunciation per word " + Arrays.asList(pronunciations),
                 words.length, pronunciations.length);
    String[] labels = {
      "s1vd", "@p", "s@m", "m@ni", "hi", "b$t", "prQp@Li" };
    for (int p = 0; p < pronunciations.length; p++) {      
      assertEquals("DISC pronunciation label " + p, labels[p], pronunciations[p].getLabel());
      assertEquals("Pronunciation label confidence " + p + " " + pronunciations[p],
                   Constants.CONFIDENCE_AUTOMATIC,
                   pronunciations[p].getConfidence().intValue());
      assertTrue("Pronunctiation tags word " + p + " " + pronunciations[p],
                 pronunciations[p].tags(words[p]));
    } // next phone    
  }   

  /** Ensure G2P creates pronunciations in IPA, with a new layer. */ 
  @Test public void G2PIPA() throws Exception {
    
    Graph f = fragment();
    Schema schema = f.getSchema();
    annotator.setSchema(schema);
    // annotator.getStatusObservers().add(status->System.out.println(status));

    assertNotNull("fragment has a language",
                  f.sourceGraph().first("transcript_language"));
    assertEquals("fragment language correct",
                 "en-NZ", f.sourceGraph().first("transcript_language").getLabel());
    assertEquals("text is correct",
                 "saved up some money he bought property",
                 Arrays.stream(f.labels(schema.getWordLayerId()))
                 .collect(Collectors.joining(" ")).trim());
    
    // configure for system layer update
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&service=G2P"
      +"&targetLanguagePattern=" // targetLanguagePattern can be blank = all languages
      +"&phonemeEncoding=ipa"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&pronunciationLayerId=bas_ipa");
    Layer layer = annotator.getSchema().getLayer("bas_ipa");
    assertNotNull("bas_ipa layer created", layer);
    assertEquals("bas_ipa word layer", "word", layer.getParentId());    
    assertTrue("bas_ipa saturated", layer.getSaturated());    
    assertEquals("bas_ipa layer type", Constants.TYPE_IPA, layer.getType());
    assertFalse("bas_ipa no peers", layer.getPeers());
    
    final Vector<Graph> results = new Vector<Graph>();
    annotator.transformFragments(
      Arrays.stream(new Graph[] { f }), graph -> { results.add(graph); });
    
    assertEquals("One utterance " + results, 1, results.size());
    Graph aligned = results.elementAt(0);
    assertTrue("Original graph is edited", f == aligned);
    
    Annotation[] words = aligned.all("word");
    assertEquals("Seven words " + Arrays.asList(words), 7, words.length);
    
    Annotation[] pronunciations = aligned.all("bas_ipa");
    assertEquals("One pronunciation per word " + Arrays.asList(pronunciations),
                 words.length, pronunciations.length);
    String[] labels = {
      "s æɪ v d", "ɐ p", "s ɐ m", "m ɐ n iː", "h iː", "b oː t", "p ɹ ɔ p ə ɾ iː" };
    for (int p = 0; p < pronunciations.length; p++) {      
      assertEquals("DISC pronunciation label " + p, labels[p], pronunciations[p].getLabel());
      assertEquals("Pronunciation label confidence " + p + " " + pronunciations[p],
                   Constants.CONFIDENCE_AUTOMATIC,
                   pronunciations[p].getConfidence().intValue());
      assertTrue("Pronunctiation tags word " + p + " " + pronunciations[p],
                 pronunciations[p].tags(words[p]));
    } // next phone    
  }   

  /** targetLanguagePattern filters out unwanted languages. */ 
  @Test public void targetLanguagePattern() throws Exception {
    
    Graph f = fragment();
    Schema schema = f.getSchema();
    annotator.setSchema(schema);
    // annotator.getStatusObservers().add(status->System.out.println(status));

    assertNotNull("fragment has a language",
                  f.sourceGraph().first("transcript_language"));
    assertEquals("fragment language correct",
                 "en-NZ", f.sourceGraph().first("transcript_language").getLabel());
    assertEquals("text is correct",
                 "saved up some money he bought property",
                 Arrays.stream(f.labels(schema.getWordLayerId()))
                 .collect(Collectors.joining(" ")).trim());
    
    // configure for system layer update
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&service=G2P"
      +"&targetLanguagePattern=es.*" // doesn't match our language
      +"&phonemeEncoding=ipa"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&pronunciationLayerId=bas_ipa");
    Layer layer = annotator.getSchema().getLayer("bas_ipa");
    assertNotNull("bas_ipa layer created", layer);
    assertEquals("bas_ipa word layer", "word", layer.getParentId());    
    assertTrue("bas_ipa saturated", layer.getSaturated());    
    assertEquals("bas_ipa layer type", Constants.TYPE_IPA, layer.getType());
    assertFalse("bas_ipa no peers", layer.getPeers());
    
    final Vector<Graph> results = new Vector<Graph>();
    annotator.transformFragments(
      Arrays.stream(new Graph[] { f }), graph -> { results.add(graph); });
    
    assertEquals("No utterances " + results, 0, results.size());
    
    Annotation[] words = f.all("word");
    assertEquals("Seven words " + Arrays.asList(words), 7, words.length);
    
    Annotation[] pronunciations = f.all("bas_ipa");
    assertEquals("No pronunciations " + Arrays.asList(pronunciations),
                 0, pronunciations.length);
  }   

  /**
   * Returns a fragment for annotating.
   * @return The graph for testing with.
   */
  public static Graph fragment() throws Exception {
    Graph g = graph();
    Schema schema = g.getSchema();
    Graph f = g.getFragment(
      10.0, 20.0, (String[])schema.getLayers().keySet().toArray(new String[0]));
    return f;
  }
  
  /**
   * Returns a graph for annotating.
   * @return The graph for testing with.
   */
  public static Graph graph() throws Exception {
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
      new Layer("mausBasic", "MAUSBasic tag").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("phonemes", "Phonemic transcription").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true), // ARPABET layer
      new Layer("segment", "Phones").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true)
      .setType(Constants.TYPE_IPA));
    // annotate a graph
    Graph g = new Graph()
      .setSchema(schema);
    g.setId("Test.TextGrid");
    Anchor start = g.getOrCreateAnchorAt(10, Constants.CONFIDENCE_MANUAL);
    Anchor end = g.getOrCreateAnchorAt(20, Constants.CONFIDENCE_MANUAL);
    g.addAnnotation(
      new Annotation().setLayerId("transcript_language").setLabel("en-NZ")
      .setStart(start).setEnd(end));
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
    
    g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("saved")
      .setStart(g.getOrCreateAnchorAt(11, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(12, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn).setOrdinal(1));
    g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("up")
      .setStart(g.getOrCreateAnchorAt(12, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(13, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn).setOrdinal(2));
    g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("some")
      .setStart(g.getOrCreateAnchorAt(13, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(14, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn).setOrdinal(3));
    g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("money")
      .setStart(g.getOrCreateAnchorAt(14, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(15, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn).setOrdinal(4));
    g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("he")
      .setStart(g.getOrCreateAnchorAt(15, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(16, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn).setOrdinal(5));
    g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("bought")
      .setStart(g.getOrCreateAnchorAt(16, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(17, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn).setOrdinal(6));
    g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("property")
      .setStart(g.getOrCreateAnchorAt(17, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(18, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn).setOrdinal(7));
    
    // access to test media
    final File tempWav = File.createTempFile("TestBASAnnotator-", ".wav");
    IO.Copy(new File(dir(), "test.wav"), tempWav);
    g.setMediaProvider(new GraphMediaProvider() {
        public MediaFile[] getAvailableMedia() throws StoreException, PermissionException {
          try {
            return new MediaFile[] { new MediaFile(tempWav) };
          } catch (Exception x) {
            throw new StoreException(x);
          }
        }
        public String getMedia(String trackSuffix, String mimeType) 
          throws StoreException, PermissionException {
          try {
            return getAvailableMedia()[0].getFile().toURL().toString();
          } catch (Exception x) {
            throw new StoreException(x);
          }
        }
        public GraphMediaProvider providerForGraph(Graph graph) {
          return this;
        }
      });
    
    return g;
  } // end of graph()

  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.annotator.basservices.TestBASAnnotator");
  }
}
