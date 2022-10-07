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

package nzilbb.transcriber.whisper;
	      
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

public class TestWhisperTranscriber {

  static WhisperTranscriber transcriber = new WhisperTranscriber();
  
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
    transcriber.setConfig(transcriber.getConfig());
    
    transcriber.getStatusObservers().add(s->System.err.println(s));
    System.out.println("Configured.");
  }

  public static File dir() throws Exception { 
    URL urlThisClass = TestWhisperTranscriber.class.getResource(
      TestWhisperTranscriber.class.getSimpleName() + ".class");
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
    assertEquals("One utterance " + Arrays.asList(utterances), 1, utterances.length);
    assertEquals("Utterance start", Double.valueOf(0.0), utterances[0].getStart().getOffset());
    assertEquals("Utterance end", Double.valueOf(5.6), utterances[0].getEnd().getOffset());

    Annotation[] words = transcript.all("word");
    assertTrue("At least word " + Arrays.asList(words), words.length > 0);
    for (Annotation utterance : utterances) {
      System.out.println(
        transcript.getId() + " ("+utterance.getStart()+"-"+utterance.getEnd()+"): \""
        +Arrays.stream(utterance.all("word"))
        .map(w->w.getLabel())
        .collect(Collectors.joining(" "))
        +"\"");
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
    assertTrue("Multiple utterances " + Arrays.asList(utterances), utterances.length > 1);

    Annotation[] words = transcript.all("word");
    assertTrue("At least word " + Arrays.asList(words), words.length > 0);
    for (Annotation utterance : utterances) {
      System.out.println(
        transcript.getId() + " ("+utterance.getStart()+"-"+utterance.getEnd()+"): \""
        +Arrays.stream(utterance.all("word"))
        .map(w->w.getLabel())
        .collect(Collectors.joining(" "))
        +"\"");
    }
  }   

  /**
   * Returns a schema to use.
   * @return The schema for the graph.
   */
  public static Schema getSchema() throws Exception {
    return new Schema(
      "participant", "turn", "utterance", "word",
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
    org.junit.runner.JUnitCore.main("nzilbb.transcriber.whisper.TestWhisperTranscriber");
  }
}
