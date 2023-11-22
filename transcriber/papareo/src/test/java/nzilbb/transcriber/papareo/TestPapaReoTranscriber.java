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
package nzilbb.transcriber.papareo;
	      
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

/**
 * Unit tests for the PapaReo transcriber.
 * <p> In order to test the client, you must have a valid Papa Reo access token, which can
 * be obtained from Te Hiku Media: <a href="https://papareo.io/docs">https://papareo.io/docs</a>
 * <p> Once you have a token, ensure that it's set as:
 * <ul>
 *  <li> a system property called <var>papareo.token</var>, or </li>
 *  <li> an environment variable called <var>PAPAREO_TOKEN</var>.
 * </ul>
 * <p> Using settings already in the pom.xml, the easiest way is to create a file called
 * <tt>papareo.properties</tt> in your home directory, with contents like:
 * <br><tt>papareo.token=xxxxxx-xxxx-xxxx-xxxx-xxxxxxx</tt>
 * <p> Then these tests will run.
 */
public class TestPapaReoTranscriber {

  static PapaReoTranscriber transcriber = new PapaReoTranscriber();
  
  /** Set the access token before any tests run. */
  @BeforeClass
  public static void setToken() throws Exception {
    transcriber = new PapaReoTranscriber();
  }
  
  public static File dir() throws Exception { 
    URL urlThisClass = TestPapaReoTranscriber.class.getResource(
      TestPapaReoTranscriber.class.getSimpleName() + ".class");
    File fThisClass = new File(urlThisClass.toURI());
    return fThisClass.getParentFile();
  }

  /** Test transcription of a long, multi-utterance recording. */
  @Test public void wordlist() throws Exception {

    // setup
    File audio = new File(dir(), "wordlist.wav");
    transcriber.setDebug(true); // debug messages from the PapaRep client
    transcriber.getStatusObservers().add(s -> System.out.println(s));
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
    org.junit.runner.JUnitCore.main("nzilbb.transcriber.papareo.TestPapaReoTranscriber");
  }
}
