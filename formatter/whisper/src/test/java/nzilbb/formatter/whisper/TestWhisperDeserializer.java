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

package nzilbb.formatter.whisper;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Vector;
import nzilbb.ag.*;
import nzilbb.ag.serialize.SerializationException;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.util.Normalizer;
import nzilbb.ag.util.Validator;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.editpath.EditStep;
import nzilbb.editpath.MinimumEditPath;
import nzilbb.util.Timers;
import nzilbb.formatter.whisper.*;

/** Test WhisperDeserializer */
public class TestWhisperDeserializer {
  /** Ensure a multi-utterance transcript is correctly deserialized and tokenized */
  @Test public void tokenized()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Speakers")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterances")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Word tokens")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true));
    
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "test.wt")) };
    
    // create deserializer
    WhisperDeserializer deserializer = new WhisperDeserializer();

    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals("Configuration parameters" + configuration, 7,
                 deserializer.configure(configuration, schema).size());      
    
    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals(0, defaultParameters.size());
    
    // configure the deserialization
    deserializer.setParameters(defaultParameters);

    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    // lines with no timestamp are ignored but warned about
    for (String warning : deserializer.getWarnings()) {
      assertEquals("Warning for comment line", "Invalid line: \"# New turn here:\"", warning);
    }
    
    assertEquals("test.wt", g.getId());
    assertEquals("time units", Constants.UNIT_SECONDS, g.getOffsetUnits());
    
    
    // participants     
    Annotation[] speakers = g.all("who"); 
    assertEquals(1, speakers.length);
    assertEquals("test.wt", speakers[0].getLabel());
    
    // turns
    Annotation[] turns = g.all("turn");
    // a turn break was added by having an utterance start after the previous utterance end
    assertEquals("Two turns", 2, turns.length);
    assertEquals("Turn 1 label is speaker name",
                 "test.wt", turns[0].getLabel());
    assertEquals("Turn 2 label is speaker name",
                 "test.wt", turns[1].getLabel());
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    assertEquals(Double.valueOf(14.240), turns[0].getEnd().getOffset());
    assertEquals(Double.valueOf(15.0), turns[1].getStart().getOffset());
    assertEquals(Double.valueOf(331.8), turns[1].getEnd().getOffset());
    
    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals("Correct number of utterances", 53, utterances.length);
    assertEquals("Utterance label is speaker name",
                 "test.wt", utterances[0].getLabel());
    assertEquals("Start time of first utterance",
                 Double.valueOf(0.0), utterances[0].getStart().getOffset());
    assertEquals("End time of first utterance",
                 Double.valueOf(10.32), utterances[0].getEnd().getOffset());
    
    assertEquals("Start time of second utterance",
                 Double.valueOf(10.32), utterances[1].getStart().getOffset());
    assertEquals("End time of second utterance",
                 Double.valueOf(14.24), utterances[1].getEnd().getOffset());

    assertEquals("Start time of last utterance",
                 Double.valueOf(311.180), utterances[utterances.length-1].getStart().getOffset());
    assertEquals("End time of last utterance",
                 Double.valueOf(331.800), utterances[utterances.length-1].getEnd().getOffset());
    
    // words
    Annotation[] words = g.all("word");
    assertEquals("Correct number of words", 696, words.length);
    String[] checkWords = {
      "The", "rest", "of", "that", "side", "of", "the", "family,", "so", "he",
      "generously", "agreed", "that", "she", "could", "go", "with", "them,"};
    for (int w = 0; w < checkWords.length; w++) {
      assertEquals("check word " + w + ": " + checkWords[w], checkWords[w], words[w].getLabel());
    } // next word
    
      // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      if (a.getLayerId().equals("who")) {
        assertEquals("Speaker has 'automatic' confidence: " + a.getLayer() + ": " + a,
                     Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC), a.getConfidence());
      } else {
        assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                     Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }
    }
  }
  
  @Test public void noWordLayer() throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", null, // no word layer
      new Layer("who", "Speakers")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterances")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true));
    
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "test.wt")) };
    
    // create deserializer
    WhisperDeserializer deserializer = new WhisperDeserializer();

    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals("Configuration wants missing word layer" + configuration, 8,
                 deserializer.configure(configuration, schema).size());      
    
    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals(0, defaultParameters.size());
    
    // configure the deserialization
    deserializer.setParameters(defaultParameters);

    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];
    
    // lines with no timestamp are ignored but warned about
    for (String warning : deserializer.getWarnings()) {
      assertEquals("Warning for comment line", "Invalid line: \"# New turn here:\"", warning);
    }
    
    assertEquals("test.wt", g.getId());
    assertEquals("time units", Constants.UNIT_SECONDS, g.getOffsetUnits());    
    
    // participants     
    Annotation[] speakers = g.all("who"); 
    assertEquals(1, speakers.length);
    assertEquals("test.wt", speakers[0].getLabel());
    
    // turns
    Annotation[] turns = g.all("turn");
    // a turn break was added by having an utterance start after the previous utterance end
    assertEquals("Two turns", 2, turns.length);
    assertEquals("Turn 1 label is speaker name",
                 "test.wt", turns[0].getLabel());
    assertEquals("Turn 2 label is speaker name",
                 "test.wt", turns[1].getLabel());
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    assertEquals(Double.valueOf(14.240), turns[0].getEnd().getOffset());
    assertEquals(Double.valueOf(15.0), turns[1].getStart().getOffset());
    assertEquals(Double.valueOf(331.8), turns[1].getEnd().getOffset());
    
    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals("Correct number of utterances", 53, utterances.length);
    assertEquals("Start time of first utterance",
                 Double.valueOf(0.0), utterances[0].getStart().getOffset());
    assertEquals("End time of first utterance",
                 Double.valueOf(10.32), utterances[0].getEnd().getOffset());
    
    assertEquals("Start time of second utterance",
                 Double.valueOf(10.32), utterances[1].getStart().getOffset());
    assertEquals("End time of second utterance",
                 Double.valueOf(14.24), utterances[1].getEnd().getOffset());

    assertEquals("Start time of last utterance",
                 Double.valueOf(311.180), utterances[utterances.length-1].getStart().getOffset());
    assertEquals("End time of last utterance",
                 Double.valueOf(331.800), utterances[utterances.length-1].getEnd().getOffset());

    assertEquals(
      "First utterance label is transcript",
      "The rest of that side of the family, so he generously agreed that she could go with them,",
      utterances[0].getLabel());
    assertEquals(
      "Second utterance label is transcript",
      "but there were so many people there that nothing was done constructively.",
      utterances[1].getLabel());
    assertEquals(
      "Last utterance label is transcript",
      "We were the oldest ones, of course I was seventeen, Barry was nineteen, and uh, he, she, Dad.",
      utterances[utterances.length-1].getLabel());
    
    // check all annotations have correct confidence    
    for (Annotation a : g.getAnnotationsById().values()) {
      if (a.getLayerId().equals("who")) {
        assertEquals("Speaker has 'automatic' confidence: " + a.getLayer() + ": " + a,
                     Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC), a.getConfidence());
      } else {
        assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                     Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }
    }
  }

  /** Ensure JSON files are parsed correctly. */
  @Test public void basicJSON()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Speakers")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterances")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Word tokens")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true));
    
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "wordlist.json")) };
    
    // create deserializer
    WhisperDeserializer deserializer = new WhisperDeserializer();

    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals("Configuration parameters" + configuration, 7,
                 deserializer.configure(configuration, schema).size());
    // defaults
    assertEquals("minShortPauseLength",
                 Double.valueOf(0.2),
                 (Double)(configuration.get("minShortPauseLength").getValue()));
    assertEquals("minMediumPauseLength",
                 Double.valueOf(0.7),
                 (Double)(configuration.get("minMediumPauseLength").getValue()));
    assertEquals("minLongPauseLength",
                 Double.valueOf(1.4),
                 (Double)(configuration.get("minLongPauseLength").getValue()));
    assertEquals("shortPauseLabel",
                 "(.)", (String)(configuration.get("shortPauseLabel").getValue()));
    assertEquals("mediumPauseLabel",
                 "(..)", (String)(configuration.get("mediumPauseLabel").getValue()));
    assertEquals("longPauseLabel",
                 "(...)", (String)(configuration.get("longPauseLabel").getValue()));
    assertEquals("maxUtteranceDuration",
                 Double.valueOf(20),
                 (Double)(configuration.get("maxUtteranceDuration").getValue()));
    // disable pause labelling etc.
    configuration.get("shortPauseLabel").setValue("");
    configuration.get("mediumPauseLabel").setValue("");
    configuration.get("longPauseLabel").setValue("");
    configuration.get("maxUtteranceDuration").setValue(null);

    deserializer.configure(configuration, schema);
    assertEquals("shortPauseLabel unset",
                 "", deserializer.getShortPauseLabel());
    assertEquals("mediumPauseLabel unset",
                 "", deserializer.getMediumPauseLabel());
    assertEquals("longPauseLabel unset",
                 "", deserializer.getLongPauseLabel());
    assertNull("maxUtteranceDuration unset",
               deserializer.getMaxUtteranceDuration());
    
    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals(0, defaultParameters.size());
    
    // configure the deserialization
    deserializer.setParameters(defaultParameters);

    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    // lines with no timestamp are ignored but warned about
    for (String warning : deserializer.getWarnings()) {
      assertEquals("Warning for comment line", "Invalid line: \"# New turn here:\"", warning);
    }
    
    assertEquals("wordlist.json", g.getId());
    assertEquals("time units", Constants.UNIT_SECONDS, g.getOffsetUnits());
    
    
    // participants     
    Annotation[] speakers = g.all("who"); 
    assertEquals("Number of speakers: " + Arrays.asList(speakers),
                 2, speakers.length);
    assertEquals("SPEAKER_01", speakers[0].getLabel());
    assertEquals("SPEAKER_00", speakers[1].getLabel());
    
    // turns
    Annotation[] turns = g.all("turn");
    // a turn break was added by having an utterance start after the previous utterance end
    assertEquals("Two turns", 9, turns.length);
    assertEquals("Turn 1 label is speaker name",
                 "SPEAKER_01", turns[0].getLabel());
    assertEquals("Turn 2 label is speaker name",
                 "SPEAKER_00", turns[1].getLabel());
    assertEquals(Double.valueOf(2.862), turns[0].getStart().getOffset());
    assertEquals(Double.valueOf(3.942), turns[0].getEnd().getOffset());
    assertEquals(Double.valueOf(5.063), turns[1].getStart().getOffset());
    assertEquals(Double.valueOf(31.85), turns[1].getEnd().getOffset());
    
    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals("Correct number of utterances", 70, utterances.length);
    assertEquals("Utterance label is speaker name",
                 "SPEAKER_01", utterances[0].getLabel());
    assertEquals("Start time of first utterance",
                 Double.valueOf(2.862), utterances[0].getStart().getOffset());
    assertEquals("End time of first utterance",
                 Double.valueOf(3.942), utterances[0].getEnd().getOffset());
    
    assertEquals("Start time of second utterance",
                 Double.valueOf(5.063), utterances[1].getStart().getOffset());
    assertEquals("End time of second utterance",
                 Double.valueOf(7.003), utterances[1].getEnd().getOffset());

    assertEquals("Start time of last utterance",
                 Double.valueOf(280.247),
                 utterances[utterances.length-1].getStart().getOffset());
    assertEquals("End time of last utterance",
                 Double.valueOf(280.507),
                 utterances[utterances.length-1].getEnd().getOffset());

    // all anchors are 'manual' confidence (even though they were automatically determined)
    for (Annotation utterance : utterances) {
      assertEquals("check utterance start confidence " + utterance.getStart(),
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL),
                   utterance.getStart().getConfidence());
      assertEquals("check utterance end confidence " + utterance.getEnd(),
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL),
                   utterance.getEnd().getConfidence());
    }
    
    // words
    Annotation[] words = g.all("word");
    assertEquals("Correct number of words", 228, words.length);
    String[] checkWords = {
      "Okay,", "go.",
      "New", "Zealand", "English", "word", "list.",
      "Number", "one,", "hit,", "hid,", "hint.",
      "Number", "two,", "boot,", "booed,", "boo,", "tune,", "dune."};
    Double[] checkStarts = {
      2.862, 3.722,
      5.063, 5.283, 5.803, 6.283, 6.643,
      7.803, 8.143, 8.824, 9.384, 10.104,
      11.384, 11.644, 12.905, 13.745, 14.885, 15.966, 17.026};
    Double[] checkEnds = {
      3.022, 3.942,
      5.263, 5.743, 6.163, 6.603, 7.003,
      8.063, 8.284, 9.044, 9.624, 10.424,
      11.604, 11.924, 13.205, 14.245, 15.325, 16.486, 17.546};
    String[] checkSpeakers = {
      "SPEAKER_01", "SPEAKER_01",
      "SPEAKER_00", "SPEAKER_00", "SPEAKER_00", "SPEAKER_00", "SPEAKER_00",
      "SPEAKER_00", "SPEAKER_00", "SPEAKER_00", "SPEAKER_00", "SPEAKER_00",
      "SPEAKER_00", "SPEAKER_00", "SPEAKER_00", "SPEAKER_00", "SPEAKER_00", "SPEAKER_00", "SPEAKER_00"};
    for (int w = 0; w < checkWords.length; w++) {
      assertEquals("check word " + w + ": " + checkWords[w],
                   checkWords[w], words[w].getLabel());
      assertEquals("check start " + w + ": " + checkWords[w],
                   checkStarts[w], words[w].getStart().getOffset());
      assertEquals("check start confidence " + w + ": " + checkWords[w],
                   Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC),
                   words[w].getStart().getConfidence());
      assertEquals("check end " + w + ": " + checkWords[w],
                   checkEnds[w], words[w].getEnd().getOffset());
      assertEquals("check end confidence " + w + ": " + checkWords[w],
                   Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC),
                   words[w].getEnd().getConfidence());
      assertEquals("check speaker " + w + ": " + checkWords[w],
                   checkSpeakers[w], words[w].getParent().getLabel());
    } // next word
    
    // check all annotations have 'automatic' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'automatic' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC), a.getConfidence());
    }
  }
  
  /** Ensure inter-word pause labelling works. */
  @Test public void interWordPauses()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Speakers")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterances")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Word tokens")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true));
    
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "wordlist.json")) };
    
    // create deserializer
    WhisperDeserializer deserializer = new WhisperDeserializer();

    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    // defaults
    assertEquals("Configuration parameters" + configuration, 7,
                 deserializer.configure(configuration, schema).size());      
    assertEquals("minShortPauseLength",
                 Double.valueOf(0.2),
                 (Double)(configuration.get("minShortPauseLength").getValue()));
    assertEquals("minMediumPauseLength",
                 Double.valueOf(0.7),
                 (Double)(configuration.get("minMediumPauseLength").getValue()));
    assertEquals("minLongPauseLength",
                 Double.valueOf(1.4),
                 (Double)(configuration.get("minLongPauseLength").getValue()));
    assertEquals("shortPauseLabel",
                 "(.)", (String)(configuration.get("shortPauseLabel").getValue()));
    assertEquals("mediumPauseLabel",
                 "(..)", (String)(configuration.get("mediumPauseLabel").getValue()));
    assertEquals("longPauseLabel",
                 "(...)", (String)(configuration.get("longPauseLabel").getValue()));
    assertEquals("maxUtteranceDuration",
                 Double.valueOf(20),
                 (Double)(configuration.get("maxUtteranceDuration").getValue()));
    
    // disable maxUtteranceDuration
    configuration.get("maxUtteranceDuration").setValue(null);

    // change thresholds
    configuration.get("minShortPauseLength").setValue(0.6);
    configuration.get("minMediumPauseLength").setValue(0.7);
    configuration.get("minLongPauseLength").setValue(0.9);
    configuration.get("longPauseLabel").setValue("({0.0##})");    
    deserializer.configure(configuration, schema);
    assertEquals("minShortPauseLength changed",
                 Double.valueOf(0.6), deserializer.getMinShortPauseLength());
    assertEquals("minMediumPauseLength changed",
                 Double.valueOf(0.7), deserializer.getMinMediumPauseLength());
    assertEquals("minLongPauseLength changed",
                 Double.valueOf(0.9), deserializer.getMinLongPauseLength());
    assertEquals("longPauseLabel changed",
                 "({0.0##})", deserializer.getLongPauseLabel());
    
    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals(0, defaultParameters.size());
    
    // configure the deserialization
    deserializer.setParameters(defaultParameters);

    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    // lines with no timestamp are ignored but warned about
    for (String warning : deserializer.getWarnings()) {
      assertEquals("Warning for comment line", "Invalid line: \"# New turn here:\"", warning);
    }
    
    assertEquals("wordlist.json", g.getId());
    assertEquals("time units", Constants.UNIT_SECONDS, g.getOffsetUnits());
    
    
    // participants     
    Annotation[] speakers = g.all("who"); 
    assertEquals("Number of speakers: " + Arrays.asList(speakers),
                 2, speakers.length);
    assertEquals("SPEAKER_01", speakers[0].getLabel());
    assertEquals("SPEAKER_00", speakers[1].getLabel());
    
    // turns
    Annotation[] turns = g.all("turn");
    // a turn break was added by having an utterance start after the previous utterance end
    assertEquals("Two turns", 9, turns.length);
    assertEquals("Turn 1 label is speaker name",
                 "SPEAKER_01", turns[0].getLabel());
    assertEquals("Turn 2 label is speaker name",
                 "SPEAKER_00", turns[1].getLabel());
    assertEquals(Double.valueOf(2.862), turns[0].getStart().getOffset());
    assertEquals(Double.valueOf(3.942), turns[0].getEnd().getOffset());
    assertEquals(Double.valueOf(5.063), turns[1].getStart().getOffset());
    assertEquals(Double.valueOf(31.85), turns[1].getEnd().getOffset());
    
    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals("Correct number of utterances", 70, utterances.length);
    assertEquals("Utterance label is speaker name",
                 "SPEAKER_01", utterances[0].getLabel());
    assertEquals("Start time of first utterance",
                 Double.valueOf(2.862), utterances[0].getStart().getOffset());
    assertEquals("End time of first utterance",
                 Double.valueOf(3.942), utterances[0].getEnd().getOffset());
    
    assertEquals("Start time of second utterance",
                 Double.valueOf(5.063), utterances[1].getStart().getOffset());
    assertEquals("End time of second utterance",
                 Double.valueOf(7.003), utterances[1].getEnd().getOffset());

    assertEquals("Start time of last utterance",
                 Double.valueOf(280.247),
                 utterances[utterances.length-1].getStart().getOffset());
    assertEquals("End time of last utterance",
                 Double.valueOf(280.507),
                 utterances[utterances.length-1].getEnd().getOffset());

    // all anchors are 'manual' confidence (even though they were automatically determined)
    for (Annotation utterance : utterances) {
      assertEquals("check utterance start confidence " + utterance.getStart(),
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL),
                   utterance.getStart().getConfidence());
      assertEquals("check utterance end confidence " + utterance.getEnd(),
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL),
                   utterance.getEnd().getConfidence());
    }
    
    // words
    Annotation[] words = g.all("word");
    assertEquals("Correct number of words", 228, words.length);
    String[] checkWords = {
      "Okay, (..)", "go.",
      "New", "Zealand", "English", "word", "list. (..)",
      "Number", "one,", "hit,", "hid,", "hint. (0.96)",
      "Number", "two, (0.981)", "boot,", "booed, (.)", "boo, (.)", "tune,", "dune. (0.96)"
    };
    Double[] checkStarts = {
      2.862, 3.722,
      5.063, 5.283, 5.803, 6.283, 6.643,
      7.803, 8.143, 8.824, 9.384, 10.104,
      11.384, 11.644, 12.905, 13.745, 14.885, 15.966, 17.026};
    Double[] checkEnds = {
      3.022, 3.942,
      5.263, 5.743, 6.163, 6.603, 7.003,
      8.063, 8.284, 9.044, 9.624, 10.424,
      11.604, 11.924, 13.205, 14.245, 15.325, 16.486, 17.546};
    String[] checkSpeakers = {
      "SPEAKER_01", "SPEAKER_01",
      "SPEAKER_00", "SPEAKER_00", "SPEAKER_00", "SPEAKER_00", "SPEAKER_00",
      "SPEAKER_00", "SPEAKER_00", "SPEAKER_00", "SPEAKER_00", "SPEAKER_00",
      "SPEAKER_00", "SPEAKER_00", "SPEAKER_00", "SPEAKER_00", "SPEAKER_00", "SPEAKER_00", "SPEAKER_00"};
    for (int w = 0; w < checkWords.length; w++) {
      assertEquals("check word " + w + ": " + checkWords[w],
                   checkWords[w], words[w].getLabel());
      assertEquals("check start " + w + ": " + checkWords[w],
                   checkStarts[w], words[w].getStart().getOffset());
      assertEquals("check start confidence " + w + ": " + checkWords[w],
                   Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC),
                   words[w].getStart().getConfidence());
      assertEquals("check end " + w + ": " + checkWords[w],
                   checkEnds[w], words[w].getEnd().getOffset());
      assertEquals("check end confidence " + w + ": " + checkWords[w],
                   Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC),
                   words[w].getEnd().getConfidence());
      assertEquals("check speaker " + w + ": " + checkWords[w],
                   checkSpeakers[w], words[w].getParent().getLabel());
    } // next word
    
    // check all annotations have 'automatic' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'automatic' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC), a.getConfidence());
    }
  }
  
  /** Ensure long utterances are split on longest pauses. */
  @Test public void maxUtteranceDuration()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Speakers")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterances")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Word tokens")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true));
    
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "wordlist.json")) };
    
    // create deserializer
    WhisperDeserializer deserializer = new WhisperDeserializer();

    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals("Configuration parameters" + configuration, 7,
                 deserializer.configure(configuration, schema).size());
    // defaults
    assertEquals("minShortPauseLength",
                 Double.valueOf(0.2),
                 (Double)(configuration.get("minShortPauseLength").getValue()));
    assertEquals("minMediumPauseLength",
                 Double.valueOf(0.7),
                 (Double)(configuration.get("minMediumPauseLength").getValue()));
    assertEquals("minLongPauseLength",
                 Double.valueOf(1.4),
                 (Double)(configuration.get("minLongPauseLength").getValue()));
    assertEquals("shortPauseLabel",
                 "(.)", (String)(configuration.get("shortPauseLabel").getValue()));
    assertEquals("mediumPauseLabel",
                 "(..)", (String)(configuration.get("mediumPauseLabel").getValue()));
    assertEquals("longPauseLabel",
                 "(...)", (String)(configuration.get("longPauseLabel").getValue()));
    assertEquals("maxUtteranceDuration",
                 Double.valueOf(20),
                 (Double)(configuration.get("maxUtteranceDuration").getValue()));
    // disable pause labelling etc.
    configuration.get("shortPauseLabel").setValue("");
    configuration.get("mediumPauseLabel").setValue("");
    configuration.get("longPauseLabel").setValue("");

    deserializer.configure(configuration, schema);
    assertEquals("shortPauseLabel unset",
                 "", deserializer.getShortPauseLabel());
    assertEquals("mediumPauseLabel unset",
                 "", deserializer.getMediumPauseLabel());
    assertEquals("longPauseLabel unset",
                 "", deserializer.getLongPauseLabel());
    assertEquals("maxUtteranceDuration set",
                 Double.valueOf(20), deserializer.getMaxUtteranceDuration());
    
    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals(0, defaultParameters.size());
    
    // configure the deserialization
    deserializer.setParameters(defaultParameters);

    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    // lines with no timestamp are ignored but warned about
    for (String warning : deserializer.getWarnings()) {
      assertEquals("Warning for comment line", "Invalid line: \"# New turn here:\"", warning);
    }
    
    assertEquals("wordlist.json", g.getId());
    assertEquals("time units", Constants.UNIT_SECONDS, g.getOffsetUnits());
    
    
    // participants     
    Annotation[] speakers = g.all("who"); 
    assertEquals("Number of speakers: " + Arrays.asList(speakers),
                 2, speakers.length);
    assertEquals("SPEAKER_01", speakers[0].getLabel());
    assertEquals("SPEAKER_00", speakers[1].getLabel());
    
    // turns
    Annotation[] turns = g.all("turn");
    // a turn break was added by having an utterance start after the previous utterance end
    assertEquals("Two turns", 9, turns.length);
    assertEquals("Turn 1 label is speaker name",
                 "SPEAKER_01", turns[0].getLabel());
    assertEquals("Turn 2 label is speaker name",
                 "SPEAKER_00", turns[1].getLabel());
    assertEquals(Double.valueOf(2.862), turns[0].getStart().getOffset());
    assertEquals(Double.valueOf(3.942), turns[0].getEnd().getOffset());
    assertEquals(Double.valueOf(5.063), turns[1].getStart().getOffset());
    assertEquals(Double.valueOf(31.85), turns[1].getEnd().getOffset());
    
    // utterances
    Annotation[] utterances = g.all("utterance");
    
    // first split should be utterance 68.465-90.549
    // on "loud" (76.106-77.687) "lout" (78.827-79.187) with a pause of 1.14
    assertEquals("Start time of utterance before first split",
                 Double.valueOf(68.465), utterances[20].getStart().getOffset());
    assertEquals("End time of utterance before first split",
                 Double.valueOf(77.687), utterances[20].getEnd().getOffset());    
    assertEquals("Start time of utterance after first split",
                 Double.valueOf(78.827), utterances[21].getStart().getOffset());
    assertEquals("End time of utterance after first split",
                 Double.valueOf(90.549), utterances[21].getEnd().getOffset());

  }
  
  /**
   * Directory for text files.
   * @see #getDir()
   * @see #setDir(File)
   */
  protected File fDir;
  /**
   * Getter for {@link #fDir}: Directory for text files.
   * @return Directory for text files.
   */
  public File getDir() { 
    if (fDir == null) {
      try {
        URL urlThisClass = getClass().getResource(getClass().getSimpleName() + ".class");
        File fThisClass = new File(urlThisClass.toURI());
        fDir = fThisClass.getParentFile();
      } catch(Throwable t) {
        System.out.println("" + t);
      }
    }
    return fDir; 
  }
  /**
   * Setter for {@link #fDir}: Directory for text files.
   * @param fNewDir Directory for text files.
   */
  public void setDir(File fNewDir) { fDir = fNewDir; }
  
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.formatter.whisper.TestWhisperDeserializer");
  }
}
