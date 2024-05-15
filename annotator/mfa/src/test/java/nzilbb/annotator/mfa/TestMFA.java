//
// Copyright 2021 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.annotator.mfa;
	      
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
import nzilbb.sql.derby.DerbyConnectionFactory;
import nzilbb.util.IO;

public class TestMFA {
  static final String condaPath = "/opt/conda/bin";
  static final String mfaEnvironment = "aligner";

  static MFA annotator = new MFA();

  /** Set up initial MFA configuration */
  @BeforeClass
  public static void config() throws Exception {
    
    System.out.println("Verify MFA configuration...");
    
    // find the current directory
    File dir = dir();
    
    // set the schema
    annotator.setSchema(graph().getSchema());
    System.out.println("mfaPath: " + annotator.inferMfaPath(condaPath, mfaEnvironment));;
    
    
    // set the working directory
    annotator.setWorkingDirectory(dir);
    
    // not setting the graph store, sorry
    
    // set the annotator configuration, which will install the lexicon the first time (only)
    annotator.setConfig("");
    
    //annotator.getStatusObservers().add(status->System.out.println(status));
    System.out.println("OK.");
  }

  /** Infer the directory of the tests. */
  public static File dir() throws Exception { 
    URL urlThisClass = TestMFA.class.getResource(
      TestMFA.class.getSimpleName() + ".class");
    File fThisClass = new File(urlThisClass.toURI());
    return fThisClass.getParentFile();
  }

  /** Ensure validDictionaryNames method works. */
  @Test public void validDictionaryNames() throws Exception {
        annotator.getStatusObservers().add(status->System.out.println(status));

    Collection<String> names = annotator.validDictionaryNames();
    assertTrue("validDictionaryNames contains english_mfa " + names,
               names.contains("english_mfa")); // 2.0.0rc3 was "english"
    assertTrue("validDictionaryNames contains german_prosodylab " + names,
               names.contains("german_prosodylab"));
    assertFalse("validDictionaryNames contains no blank entries " + names,
               names.contains(""));
  }   

  /** Ensure validDictionaryNames method works. */
  @Test public void validAcousticModels() throws Exception {
    Collection<String> names = annotator.validAcousticModels();
    assertTrue("validAcousticModels contains english_mfa " + names,
               names.contains("english_mfa")); // 2.0.0rc3 was "english"
    assertTrue("validAcousticModels contains spanish " + names,
               names.contains("spanish_mfa")); // 2.0.0rc3 was "spanish"
    assertFalse("validAcousticModels contains no blank entries " + names,
               names.contains(""));
  }   

  /** Ensure mfaVersion method works. */
  @Test public void mfaVersion() throws Exception {
    String version = annotator.mfaVersion();
    assertTrue("MFA version is 2...: " + version, version.startsWith("2"));
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

  /** Ensure valid task parameters don't raise errors, and change the schema when appropriate. */
  @Test public void setValidParameters() throws Exception {
    
    Graph g = graph();
    Schema schema = g.getSchema();
    // set phonemes layer to type=ipa so we can test the type is copied
    schema.getLayer("phonemes").setType(Constants.TYPE_IPA);
    annotator.setSchema(schema);
    
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&pronunciationLayerId=phonemes" // pronunciationLayerId set
      +"&dictionaryName="               // no dictionaryName
      +"&modelsName="                   // no modelsName
      +"&utteranceTagLayerId=mfa"
      +"&participantTagLayerId="
      +"&wordAlignmentLayerId=word"
      +"&phoneAlignmentLayerId=segment");
    assertFalse("noSpeakerAdaptation=false is default", annotator.getNoSpeakerAdaptation());
    
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&pronunciationLayerId="  // no pronunciationLayerId
      +"&dictionaryName=english_us_arpa" // dictionaryName set
      +"&modelsName=english"     // modelsName set
      +"&utteranceTagLayerId=mfa"
      +"&participantTagLayerId="
      +"&wordAlignmentLayerId=word"
      +"&phoneAlignmentLayerId=segment");
    
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&pronunciationLayerId="
      +"&dictionaryName=english_us_arpa"
      +"&modelsName=english"
      +"&noSpeakerAdaptation=true" // no speaker adaptation
      +"&utteranceTagLayerId=mfa"
      +"&participantTagLayerId="
      +"&wordAlignmentLayerId=word"
      +"&phoneAlignmentLayerId=segment");
    assertTrue("noSpeakerAdaptation=true sets attribute", annotator.getNoSpeakerAdaptation());
    
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&pronunciationLayerId="
      +"&dictionaryName=english_us_arpa"
      +"&modelsName=english"
      +"&noSpeakerAdaptation=false" // speaker adaptation
      +"&utteranceTagLayerId=mfa"
      +"&participantTagLayerId="
      +"&wordAlignmentLayerId=word"
      +"&phoneAlignmentLayerId=segment");
    assertFalse("noSpeakerAdaptation=false sets attribute", annotator.getNoSpeakerAdaptation());
    
    // layers are created as required
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&pronunciationLayerId=phonemes"
      +"&utteranceTagLayerId=utterance_mfa" // nonexistent
      +"&participantTagLayerId=participant_mfa" // nonexistent
      +"&wordAlignmentLayerId=word_alignment"
      +"&phoneAlignmentLayerId=phone");
    Layer layer = annotator.getSchema().getLayer("utterance_mfa");
    assertNotNull("utterance_mfa layer created", layer);
    layer = annotator.getSchema().getLayer("participant_mfa");
    assertNotNull("participant_mfa layer created", layer);
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
        "orthographyLayerId=orthography" // nonexistent
        +"&pronunciationLayerId=phonemes"
        +"&utteranceTagLayerId=mfa"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=segment");
      fail("Should fail with nonexistent orthographyLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=word"
        +"&pronunciationLayerId=phonology" // nonexistent
        +"&utteranceTagLayerId=mfa"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=segment");
      fail("Should fail with nonexistent pronunciationLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=word"
        +"&pronunciationLayerId=" // no pronunciationLayerId
        +"&dictionaryName="       // no dictionaryName
        +"&modelsName="           // no modelsName
        +"&utteranceTagLayerId=mfa"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=segment");
      fail("Should fail with neither pronunciationLayerId nor dictionaryName");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=word"
        +"&pronunciationLayerId="  // no pronunciationLayerId
        +"&dictionaryName=english_us_arpa" // dictionaryName
        +"&modelsName="            // but no modelsName
        +"&utteranceTagLayerId=mfa"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=segment");
      fail("Should fail with neither pronunciationLayerId nor dictionaryName");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=word"
        +"&pronunciationLayerId="  // no pronunciationLayerId
        +"&modelsName=english"     // modelsName
        +"&dictionaryName="        // but not dictionaryName
        +"&utteranceTagLayerId=mfa"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=segment");
      fail("Should fail with neither pronunciationLayerId nor dictionaryName");
    } catch (InvalidConfigurationException x) {
    }
  }

  /** Test alignment of fragment with pre-trained models/dictionary (english/english),
   * updating word token alignments and creating children. */
  @Test public void pretrainedModels() throws Exception {
    annotator.setSessionName("pretrainedModels");
    if (annotator.getStatusObservers().size() == 0) {
      annotator.getStatusObservers().add(status->System.out.println(status));
    }
    
    Graph f = fragment();
    Schema schema = f.getSchema();
    annotator.setSchema(schema);
    
    // layers are created as required
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&dictionaryName=english_us_arpa"
      +"&modelsName=english_us_arpa"
      +"&utteranceTagLayerId=utterance_mfa" // nonexistent
      +"&participantTagLayerId=participant_mfa" // nonexistent
      +"&wordAlignmentLayerId=word"
      +"&phoneAlignmentLayerId=segment");
    Layer layer = annotator.getSchema().getLayer("utterance_mfa");
    assertNotNull("utterance_mfa layer created", layer);
    layer = annotator.getSchema().getLayer("participant_mfa");
    assertNotNull("participant_mfa layer created", layer);
    
    final Vector<Graph> results = new Vector<Graph>();
    annotator.transformFragments(
      Arrays.stream(new Graph[] { f }), graph -> { results.add(graph); });
    
    assertEquals("One utterance " + results, 1, results.size());
    Graph aligned = results.elementAt(0);
    assertTrue("Original graph is edited", f == aligned);
    
    Annotation[] words = aligned.all("word");
    assertEquals("One word " + Arrays.asList(words), 1, words.length);
    Annotation word = words[0];
    assertEquals("Word label " + word, "statute", word.getLabel());
    
    Annotation[] phones = word.all("segment");
    assertEquals("Six phones " + Arrays.asList(phones), 6, phones.length);
    String[] labels = { "s", "t", "{", "J", "@", "t" };
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
      if (p > 0) {
        assertEquals("Phone start shared with previous end " + p,
                     phones[p-1].getEnd(), phones[p].getStart());
      }
    } // next phone    
  }   
  
  /** Test alignment of fragment with pre-trained IPA models/dictionary
   * (english_ipa/english_uk_ipa), updating word token alignments and creating children. */
  /*@Test*/ public void pretrainedIPAModels() throws Exception {
    annotator.setSessionName("pretrainedIPAModels");
    if (annotator.getStatusObservers().size() == 0) {
      annotator.getStatusObservers().add(status->System.out.println(status));
    }
    
    Graph f = fragment();
    Schema schema = f.getSchema();
    annotator.setSchema(schema);
    
    // layers are created as required
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&dictionaryName=english_mfa"
      +"&modelsName=english_mfa"
      +"&utteranceTagLayerId=utterance_mfa" // nonexistent
      +"&participantTagLayerId=participant_mfa" // nonexistent
      +"&wordAlignmentLayerId=word"
      +"&phoneAlignmentLayerId=segment");
    Layer layer = annotator.getSchema().getLayer("utterance_mfa");
    assertNotNull("utterance_mfa layer created", layer);
    layer = annotator.getSchema().getLayer("participant_mfa");
    assertNotNull("participant_mfa layer created", layer);
    
    final Vector<Graph> results = new Vector<Graph>();
    annotator.transformFragments(
      Arrays.stream(new Graph[] { f }), graph -> { results.add(graph); });
    
    assertEquals("One utterance " + results, 1, results.size());
    Graph aligned = results.elementAt(0);
    assertTrue("Original graph is edited", f == aligned);
    
    Annotation[] words = aligned.all("word");
    assertEquals("One word " + Arrays.asList(words), 1, words.length);
    Annotation word = words[0];
    assertEquals("Word label " + word, "statute", word.getLabel());
    
    Annotation[] phones = word.all("segment");
    assertEquals("Six phones " + Arrays.asList(phones), 6, phones.length);
    String[] labels = { "s", "t", "æ", "tʃ", "ʉː", "ʔ" };
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
      if (p > 0) {
        assertEquals("Phone start shared with previous end " + p,
                     phones[p-1].getEnd(), phones[p].getStart());
      }
    } // next phone    
  }   
  
  /** Test alignment of full graph works. */
  /* @Test */ public void graphTransform() throws Exception {
    annotator.setSessionName("graphTransform");
    annotator.getStatusObservers().add(status->System.out.println(status));
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // layers are created as required
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&dictionaryName=english_us_arpa"
      +"&modelsName=english_us_arpa"
      +"&utteranceTagLayerId=utterance_mfa" // nonexistent
      +"&participantTagLayerId=participant_mfa" // nonexistent
      +"&wordAlignmentLayerId=word"
      +"&phoneAlignmentLayerId=segment");
    Layer layer = annotator.getSchema().getLayer("utterance_mfa");
    assertNotNull("utterance_mfa layer created", layer);
    layer = annotator.getSchema().getLayer("participant_mfa");
    assertNotNull("participant_mfa layer created", layer);

    g.trackChanges();
    annotator.transform(g);
        
    Annotation[] words = g.all("word");
    assertEquals("One word " + Arrays.asList(words), 1, words.length);
    Annotation word = words[0];
    assertEquals("Word label " + word, "statute", word.getLabel());
    
    Annotation[] phones = word.all("segment");
    assertEquals("Six phones " + Arrays.asList(phones), 6, phones.length);
    String[] labels = { "s", "t", "{", "J", "@", "t" };
    for (int p = 0; p < phones.length; p++) {      
      assertEquals("DISC phone label " + p, labels[p], phones[p].getLabel());
      assertEquals("Phone is marked for addition " + p,
                   Change.Operation.Create, phones[p].getChange());
      if (p > 0) { // first phone might coincide with start and be CONFIDENCE_MANUAL
        assertEquals("Phone start confidence " + p,
                     Constants.CONFIDENCE_AUTOMATIC,
                     phones[p].getStart().getConfidence().intValue());
      }
      if (p < phones.length - 1) { // last phone might coincide with end and be CONFIDENCE_MANUAL
        assertEquals("Phone end confidence " + p,
                     Constants.CONFIDENCE_AUTOMATIC,
                     phones[p].getEnd().getConfidence().intValue());
      }
      if (!phones[p].getStart().isStartOn("word")) {
        assertEquals("Phone start is new " + p,
                     Change.Operation.Create, phones[p].getStart().getChange());
      }
      if (p > 0) {
        assertEquals("Phone start shared with previous end " + p,
                     phones[p-1].getEnd(), phones[p].getStart());
      }
    } // next phone    
    assertEquals("word/phone start shared", word.getStart(), phones[0].getStart());
    assertEquals("word/phone end shared", word.getEnd(), phones[5].getEnd());
  }   

  /** Test alignment of fragment with pre-trained models, adding alignments independent of 
   *  original word alignments. */
  /*@Test*/ public void alignToPhraseLayers() throws Exception {
    annotator.setSessionName("alignToPhraseLayers");
    //annotator.getStatusObservers().add(status->System.out.println(status));
    
    Graph f = fragment();
    Schema schema = f.getSchema();
    annotator.setSchema(schema);

    // set the word boundary confidences to a custom value so we can detect if they've changed
    final int CUSTOM_CONFIDENCE = 15;
    for (Annotation word : f.all("word")) {
      word.getStart().setConfidence(CUSTOM_CONFIDENCE);
      word.getEnd().setConfidence(CUSTOM_CONFIDENCE);
    }
    
    // layers are created as required
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&dictionaryName=english_us_arpa"
      +"&modelsName=english_us_arpa"
      +"&utteranceTagLayerId=mfa_utterance" // nonexistent
      +"&participantTagLayerId=mfa_participant" // nonexistent
      +"&wordAlignmentLayerId=mfa_word" // nonexistent
      +"&phoneAlignmentLayerId=mfa_phone"); // nonexistent
    Layer layer = annotator.getSchema().getLayer("mfa_utterance");
    assertNotNull("mfa_utterance layer created", layer);
    layer = annotator.getSchema().getLayer("mfa_participant");
    assertNotNull("mfa_participant layer created", layer);
    layer = annotator.getSchema().getLayer("mfa_word");
    assertNotNull("mfa_word layer created", layer);
    assertEquals("mfa_word phrase layer",
                 "turn", layer.getParentId());    
    assertFalse("mfa_word not saturated",
                layer.getSaturated());    
    layer = annotator.getSchema().getLayer("mfa_phone");
    assertNotNull("mfa_phone layer created", layer);
    assertEquals("phone layer type", Constants.TYPE_STRING, layer.getType());
    assertEquals("mfa_phone phrase layer",
                 "turn", layer.getParentId());    
    assertFalse("mfa_phone not saturated",
                layer.getSaturated());    

    final Vector<Graph> results = new Vector<Graph>();
    annotator.transformFragments(
      Arrays.stream(new Graph[] { f }), graph -> { results.add(graph); });
    
    assertEquals("One utterance " + results, 1, results.size());
    Graph aligned = results.elementAt(0);
    assertTrue("Original graph is edited", f == aligned);
    
    Annotation[] words = aligned.all("word");
    assertEquals("One word " + Arrays.asList(words), 1, words.length);
    Annotation word = words[0];
    assertEquals("Word label " + word, "statute", word.getLabel());
    assertEquals("Word start not changed",
                 Double.valueOf(11.0), word.getStart().getOffset());
    assertEquals("Word start confidence not changed",
                 CUSTOM_CONFIDENCE, word.getStart().getConfidence().intValue());
    assertEquals("Word end not changed",
                 Double.valueOf(13.0), word.getEnd().getOffset());
    assertEquals("Word end confidence not changed",
                 CUSTOM_CONFIDENCE, word.getEnd().getConfidence().intValue());
    
    Annotation[] phones = word.all("segment");
    assertEquals("No phones " + Arrays.asList(phones), 0, phones.length);

    Annotation[] mfa_words = aligned.all("mfa_word");
    assertEquals("One MFA word " + Arrays.asList(mfa_words), 1, mfa_words.length);
    Annotation mfa_word = mfa_words[0];
    assertEquals("MFA Word label " + mfa_word, "statute", mfa_word.getLabel());
    
    Annotation[] mfa_phones = aligned.all("mfa_phone");
    assertEquals("Six MFA phones " + Arrays.asList(mfa_phones), 6, mfa_phones.length);
    String[] labels = { "S", "T", "AE1", "CH", "UW0", "T" };
    for (int p = 0; p < mfa_phones.length; p++) {      
      assertEquals("DISC phone label " + p, labels[p], mfa_phones[p].getLabel());
      if (p > 0) { // first phone might coincide with start and be CONFIDENCE_MANUAL
        assertEquals("Phone start confidence " + p,
                     Constants.CONFIDENCE_AUTOMATIC,
                     mfa_phones[p].getStart().getConfidence().intValue());
      }
      if (p < phones.length - 1) { // last phone might coincide with end and be CONFIDENCE_MANUAL
        assertEquals("Phone end confidence " + p,
                     Constants.CONFIDENCE_AUTOMATIC,
                     mfa_phones[p].getEnd().getConfidence().intValue());
      }
      if (p > 0) {
        assertEquals("Phone start shared with previous end " + p,
                     mfa_phones[p-1].getEnd(), mfa_phones[p].getStart());
      }
    } // next phone    
    assertEquals("word/phone start shared", mfa_word.getStart(), mfa_phones[0].getStart());
    assertEquals("word/phone end shared", mfa_word.getEnd(), mfa_phones[5].getEnd());
  }

  /** Test train/align modality with no --phone_set specified. */
  /* @Test doesn't work, presumably because there's almost no data!
   */ public void trainAndAlign() throws Exception {
    annotator.setSessionName("trainAndAlign");
    if (annotator.getStatusObservers().size() == 0) {
      annotator.getStatusObservers().add(status->System.out.println(status));
    }
    
    Graph f = fragment();
    Schema schema = f.getSchema();
    annotator.setSchema(schema);    
    
    // layers are created as required
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&pronunciationLayerId=phonemes"
      +"&utteranceTagLayerId=utterance_mfa" // nonexistent
      +"&participantTagLayerId=participant_mfa" // nonexistent
      +"&wordAlignmentLayerId=word"
      +"&phoneAlignmentLayerId=segment");
    Layer layer = annotator.getSchema().getLayer("utterance_mfa");
    assertNotNull("utterance_mfa layer created", layer);
    layer = annotator.getSchema().getLayer("participant_mfa");
    assertNotNull("participant_mfa layer created", layer);

    final Vector<Graph> results = new Vector<Graph>();
    annotator.transformFragments(
      Arrays.stream(new Graph[] { f }), graph -> { results.add(graph); });

    assertEquals("One utterance " + results, 1, results.size());
    Graph aligned = results.elementAt(0);
    assertTrue("Original graph is edited", f == aligned);
    
    Annotation[] words = aligned.all("word");
    assertEquals("One word " + Arrays.asList(words), 1, words.length);
    Annotation word = words[0];
    assertEquals("Word label " + word, "statute", word.getLabel());

    // don't bother testing offsets, as they'll be rubbish
    Annotation[] phones = word.all("segment");
    assertEquals("Six phones " + Arrays.asList(phones), 6, phones.length);
    String[] labels = { "s", "t", "{", "J", "@", "t" };
    for (int p = 0; p < phones.length; p++) {      
      assertEquals("DISC phone label " + p, labels[p], phones[p].getLabel());
      if (p > 0) { // first phone might coincide with start and be CONFIDENCE_MANUAL
        assertEquals("Phone start confidence " + p,
                     Constants.CONFIDENCE_AUTOMATIC,
                     phones[p].getStart().getConfidence().intValue());
      }
      if (p < phones.length - 1) { // last phone might coincide with end and be CONFIDENCE_MANUAL
        assertEquals("Phone end confidence " + p,
                     Constants.CONFIDENCE_AUTOMATIC,
                     phones[p].getEnd().getConfidence().intValue());
      }
      if (p > 0) {
        assertEquals("Phone start shared with previous end " + p,
                     phones[p-1].getEnd(), phones[p].getStart());
      }
    } // next phone    
  }   
  
  /** Test train/align modality with --phone_set specified. */
  /*@Test doesn't work, apparently because not all ARPABET phonemes are represented in the
   * data
   */ public void trainAndAlignWithPhoneSet() throws Exception {
    annotator.setSessionName("trainAndAlign");
    annotator.getStatusObservers().add(status->System.out.println(status));
    
    Graph f = fragment();
    Schema schema = f.getSchema();
    annotator.setSchema(schema);    
    
    // layers are created as required
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&pronunciationLayerId=phonemes"
      +"&phoneSet=ARPA"
      +"&utteranceTagLayerId=utterance_mfa" // nonexistent
      +"&participantTagLayerId=participant_mfa" // nonexistent
      +"&wordAlignmentLayerId=word"
      +"&phoneAlignmentLayerId=segment");
    Layer layer = annotator.getSchema().getLayer("utterance_mfa");
    assertNotNull("utterance_mfa layer created", layer);
    layer = annotator.getSchema().getLayer("participant_mfa");
    assertNotNull("participant_mfa layer created", layer);

    final Vector<Graph> results = new Vector<Graph>();
    annotator.transformFragments(
      Arrays.stream(new Graph[] { f }), graph -> { results.add(graph); });

    assertEquals("One utterance " + results, 1, results.size());
    Graph aligned = results.elementAt(0);
    assertTrue("Original graph is edited", f == aligned);
    
    Annotation[] words = aligned.all("word");
    assertEquals("One word " + Arrays.asList(words), 1, words.length);
    Annotation word = words[0];
    assertEquals("Word label " + word, "statute", word.getLabel());

    // don't bother testing offsets, as they'll be rubbish
    Annotation[] phones = word.all("segment");
    assertEquals("Six phones " + Arrays.asList(phones), 6, phones.length);
    String[] labels = { "s", "t", "{", "J", "@", "t" };
    for (int p = 0; p < phones.length; p++) {      
      assertEquals("DISC phone label " + p, labels[p], phones[p].getLabel());
      if (p > 0) { // first phone might coincide with start and be CONFIDENCE_MANUAL
        assertEquals("Phone start confidence " + p,
                     Constants.CONFIDENCE_AUTOMATIC,
                     phones[p].getStart().getConfidence().intValue());
      }
      if (p < phones.length - 1) { // last phone might coincide with end and be CONFIDENCE_MANUAL
        assertEquals("Phone end confidence " + p,
                     Constants.CONFIDENCE_AUTOMATIC,
                     phones[p].getEnd().getConfidence().intValue());
      }
      if (p > 0) {
        assertEquals("Phone start shared with previous end " + p,
                     phones[p-1].getEnd(), phones[p].getStart());
      }
    } // next phone    
  }   
  
  /**
   * Returns a fragment for annotating.
   * @return The graph for testing with.
   */
  public static Graph fragment() throws Exception {
    Graph g = graph();
    Schema schema = g.getSchema();
    Graph f = g.getFragment(
      10.0, 14.0, (String[])schema.getLayers().keySet().toArray(new String[0]));
    return f;
  }
  
  /**
   * Returns a graph for annotating.
   * @return The graph for testing with.
   */
  public static Graph graph() throws Exception {
    Schema schema = new Schema(
      "participant", "turn", "utterance", "word",
      new Layer("participant", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("participant").setParentIncludes(true),
      new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("mfa", "MFA tag").setAlignment(Constants.ALIGNMENT_INTERVAL)
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
      .setType("ipa"));
    // annotate a graph
    Graph g = new Graph()
      .setSchema(schema);
    g.setId("BREY00538.TextGrid");
    Anchor start = g.getOrCreateAnchorAt(10, Constants.CONFIDENCE_MANUAL);
    Anchor end = g.getOrCreateAnchorAt(14, Constants.CONFIDENCE_MANUAL);
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
    
    Annotation statute = g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("statute")
      .setStart(g.getOrCreateAnchorAt(11, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(13, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("phonemes").setLabel("S T AE1 CH UW0 T")
                    .setParent(statute));

    // access to test media
    final File tempWav = File.createTempFile("TestMFA-", ".wav");
    IO.Copy(new File(dir(), "BREY00538.wav"), tempWav);
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
    org.junit.runner.JUnitCore.main("nzilbb.annotator.mfa.TestMFA");
  }
}
