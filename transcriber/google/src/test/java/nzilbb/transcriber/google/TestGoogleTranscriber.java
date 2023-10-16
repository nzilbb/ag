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

package nzilbb.transcriber.google;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
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
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.StoreException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.util.IO;

public class TestGoogleTranscriber {
  // UPDATE THE FOLLOWING SETTINGS TO MATCH YOUR GOOGLE CLOUD RESOURCES:
  static final String projectId = "glassy-outcome-308619";
  static final String bucketName = "junit-test-transcriber";

  static GoogleTranscriber transcriber = new GoogleTranscriber();

  @BeforeClass
  public static void install() throws Exception {
    
    System.out.println("Configuring module...");
    
    // find the current directory
    File dir = dir();

    // set the schema
    transcriber.setSchema(getSchema());
    
    // set the working directory
    transcriber.setWorkingDirectory(dir);
    
    // set the annotator configuration
    String config = "projectId="+projectId+"&bucketName="+bucketName;
    // set Google API key file if it exists
    File key = new File(new File(System.getProperty("user.home")), "GoogleTranscriber-key.json");
    if (key.exists()) {
      System.out.println("Using API key: " + key.getAbsolutePath());
      config += "&keyPath="+key.getAbsolutePath();
    }
    transcriber.setConfig(config);
    
    transcriber.getStatusObservers().add(s->System.err.println(s));
    System.out.println("Configured.");
  }

  public static File dir() throws Exception { 
    URL urlThisClass = TestGoogleTranscriber.class.getResource(
      TestGoogleTranscriber.class.getSimpleName() + ".class");
    File fThisClass = new File(urlThisClass.toURI());
    return fThisClass.getParentFile();
  }

  /** Test transcription of a single short utterance. */
  @Test public void utterance() throws Exception {

    // setup
    File audio = new File(dir(), "utterance.wav");
    transcriber.setSchema(getSchema());
    
    Graph transcript = new Graph();
    transcript.setSchema(getSchema());
    transcript.setId(IO.WithoutExtension(audio));
    transcript.setSchema((Schema)transcriber.getSchema().clone());
    transcript.createTag(transcript, "transcript_language", "en-NZ");

    // transcribe the audio
    transcriber.transcribe(audio, transcript);

    // check the graph

    Annotation[] participants = transcript.all("participant");
    assertEquals("One participant " + Arrays.asList(participants), 1, participants.length);
    assertEquals("Participant name", "utterance", participants[0].getLabel());

    Annotation[] turns = transcript.all("turn");
    assertEquals("One turn " + Arrays.asList(turns), 1, turns.length);
    assertEquals("Turn label", "utterance", turns[0].getLabel());

    Annotation[] utterances = transcript.all("utterance");
    assertTrue("Several utterances " + Arrays.asList(utterances), utterances.length > 0);
    for (Annotation utterance : utterances) {
      assertNotNull("Label set " + utterance, utterance.getLabel());
      assertNotNull("Start set " + utterance, utterance.getStart());
      assertNotNull("End set " + utterance, utterance.getEnd());
      assertTrue("Duration is positive " + utterance.getStart() + "-" + utterance.getEnd(),
                 utterance.getDuration() > 0);
      assertNotNull("Confidence set " + utterance, utterance.getConfidence());
      System.out.println(
        transcript.getId() + " ("+utterance.getStart()+"-"+utterance.getEnd()+"): \""
        +utterance.getLabel()
        +"\"");
    }

    Annotation[] words = transcript.all("word");
    assertTrue("At least word " + Arrays.asList(words), words.length > 0);
    for (Annotation word : words) {
      assertNotNull("Label set " + word, word.getLabel());
      assertNotNull("Start set " + word, word.getStart());
      assertNotNull("End set " + word, word.getEnd());
      // System.out.println(""+word + " " + word.getStart() + "-" + word.getEnd());
      assertTrue("Duration is not negative " + word.getStart() + "-" + word.getEnd(),
                 word.getDuration() >= 0);
      assertNotNull("Confidence set " + word, word.getConfidence());
      assertEquals("Auto confidence " + word,
                   (long)Constants.CONFIDENCE_AUTOMATIC, (long)word.getConfidence());
    }
  }   

  /** Test transcription of a long, multi-utterance recording. */
  @Test public void wordlist() throws Exception {

    // setup
    File audio = new File(dir(), "wordlist.wav");
    transcriber.setSchema(getSchema());
    
    Graph transcript = new Graph();
    transcript.setSchema(getSchema());
    transcript.setId(IO.WithoutExtension(audio));
    transcript.setSchema((Schema)transcriber.getSchema().clone());

    // transcribe the audio
    transcriber.transcribe(audio, transcript);

    // check the graph

    Annotation[] participants = transcript.all("participant");
    assertEquals("One participant " + Arrays.asList(participants), 1, participants.length);
    assertEquals("Participant name", "wordlist", participants[0].getLabel());

    Annotation[] turns = transcript.all("turn");
    assertEquals("One turn " + Arrays.asList(turns), 1, turns.length);
    assertEquals("Turn label", "wordlist", turns[0].getLabel());

    Annotation[] utterances = transcript.all("utterance");
    assertTrue("Several utterances " + Arrays.asList(utterances), utterances.length > 0);
    for (Annotation utterance : utterances) {
      assertNotNull("Label set " + utterance, utterance.getLabel());
      assertNotNull("Start set " + utterance, utterance.getStart());
      assertNotNull("End set " + utterance, utterance.getEnd());
      assertTrue("Duration is positive " + utterance.getStart() + "-" + utterance.getEnd(),
                 utterance.getDuration() > 0);
      assertNotNull("Confidence set " + utterance, utterance.getConfidence());
      System.out.println(
        transcript.getId() + " ("+utterance.getStart()+"-"+utterance.getEnd()+"): \""
        +utterance.getLabel()
        +"\"");
    }

    Annotation[] words = transcript.all("word");
    assertTrue("At least word " + Arrays.asList(words), words.length > 0);
    for (Annotation word : words) {
      assertNotNull("Label set " + word, word.getLabel());
      assertNotNull("Start set " + word, word.getStart());
      assertNotNull("End set " + word, word.getEnd());
      // System.out.println(""+word + " " + word.getStart() + "-" + word.getEnd());
      assertTrue("Duration is not negative " + word.getStart() + "-" + word.getEnd(),
                 word.getDuration() >= 0);
      assertNotNull("Confidence set " + word, word.getConfidence());
      assertEquals("Auto confidence " + word,
                   (long)Constants.CONFIDENCE_AUTOMATIC, (long)word.getConfidence());
    }
  }   

  /**
   * Returns a schema to use.
   * @return The schema for the graph.
   */
  public static Schema getSchema() throws Exception {
    return new Schema(
      "participant", "turn", "utterance", "word",
      new Layer("transcript_language", "Language").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("participant", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("participant").setParentIncludes(true),
      new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("htk", "HTK tag").setAlignment(Constants.ALIGNMENT_INTERVAL)
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
  }
  
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.transcriber.google.TestGoogleTranscriber");
  }
}
