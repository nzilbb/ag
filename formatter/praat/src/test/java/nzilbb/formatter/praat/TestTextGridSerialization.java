//
// Copyright 2015-2023 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.formatter.praat;
	      
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
import java.util.SortedSet;
import java.util.Vector;
import nzilbb.ag.*;
import nzilbb.ag.serialize.SerializationException;
import nzilbb.ag.serialize.json.JSONSerialization;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.editpath.EditStep;
import nzilbb.editpath.MinimumEditPath;
import nzilbb.formatter.praat.*;

public class TestTextGridSerialization {
   
  @Test public void utterance_conventions()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("noise", "Noise", 2, true, false, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("lexical", "Lexical", 0, true, false, false, "word", true),
      new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
      
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "test_utterance.TextGrid")) };
      
    // create deserializer
    TextGridSerialization deserializer = new TextGridSerialization();
      
    // general configuration
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    configuration.get("useConventions").setValue(Boolean.TRUE);
    //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals(8, deserializer.configure(configuration, schema).size());

    // load the stream
    ParameterSet defaultParamaters = deserializer.load(streams, schema);
    //for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
    assertEquals(2, defaultParamaters.size());

    // configure the deserialization
    deserializer.setParameters(defaultParamaters);
      
    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
    assertEquals("No warnings",
                 0, deserializer.getWarnings().length);
      
    assertEquals("test_utterance.TextGrid", g.getId());

    // participants     
    Annotation[] who = g.all("who");
    assertEquals(2, who.length);
    assertEquals("participant", who[0].getLabel());
    assertEquals(g, who[0].getParent());
    assertNotNull("participants are anchored to the graph", who[0].getStart());
    assertNotNull("participants are anchored to the graph", who[0].getEnd());
    assertEquals("interviewer", who[1].getLabel());
    assertEquals(g, who[1].getParent());
      
    // turns
    Annotation[] turns = g.all("turn");
    assertEquals(20, turns.length);
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    assertEquals(Double.valueOf(44.255), turns[0].getEnd().getOffset());
    assertEquals("participant", turns[0].getLabel());
    assertEquals(who[0], turns[0].getParent());
      
    assertEquals(Double.valueOf(44.255), turns[1].getStart().getOffset());
    assertEquals(Double.valueOf(45.505), turns[1].getEnd().getOffset());
    assertEquals("interviewer", turns[1].getLabel());
    assertEquals(who[1], turns[1].getParent());

    assertEquals(Double.valueOf(45.505), turns[2].getStart().getOffset());
    assertEquals(Double.valueOf(107.804), turns[2].getEnd().getOffset());
    assertEquals("participant", turns[2].getLabel());

    assertEquals(Double.valueOf(107.804), turns[3].getStart().getOffset());
    assertEquals(Double.valueOf(110.335), turns[3].getEnd().getOffset());
    assertEquals("interviewer", turns[3].getLabel());

    assertEquals(Double.valueOf(110.335), turns[4].getStart().getOffset());
    assertEquals(Double.valueOf(181.652), turns[4].getEnd().getOffset());
    assertEquals("participant", turns[4].getLabel());

    assertEquals(Double.valueOf(181.652), turns[5].getStart().getOffset());
    assertEquals(Double.valueOf(183.995), turns[5].getEnd().getOffset());
    assertEquals("interviewer", turns[5].getLabel());

    assertEquals(Double.valueOf(183.995), turns[6].getStart().getOffset());
    assertEquals(Double.valueOf(185.339), turns[6].getEnd().getOffset());
    assertEquals("participant", turns[6].getLabel());

    assertEquals(Double.valueOf(185.339), turns[7].getStart().getOffset());
    assertEquals(Double.valueOf(189.464), turns[7].getEnd().getOffset());
    assertEquals("interviewer", turns[7].getLabel());

    assertEquals(Double.valueOf(189.464), turns[8].getStart().getOffset());
    assertEquals(Double.valueOf(191.682), turns[8].getEnd().getOffset());
    assertEquals("participant", turns[8].getLabel());

    assertEquals(Double.valueOf(191.682), turns[9].getStart().getOffset());
    assertEquals(Double.valueOf(197.181), turns[9].getEnd().getOffset());
    assertEquals("interviewer", turns[9].getLabel());

    assertEquals(Double.valueOf(197.181), turns[10].getStart().getOffset());
    assertEquals(Double.valueOf(199.213), turns[10].getEnd().getOffset());
    assertEquals("participant", turns[10].getLabel());

    assertEquals(Double.valueOf(199.213), turns[11].getStart().getOffset());
    assertEquals(Double.valueOf(205.415), turns[11].getEnd().getOffset());
    assertEquals("interviewer", turns[11].getLabel());

    // simultaneous speech
    // the textgrid has two itervals for this which have been joined into one turn
    assertEquals(Double.valueOf(205.415), turns[12].getStart().getOffset());
    assertEquals(Double.valueOf(220.696), turns[12].getEnd().getOffset());
    assertEquals("participant", turns[12].getLabel());
    // simultaneous speech
    assertEquals(Double.valueOf(214.822), turns[13].getStart().getOffset());
    assertEquals(Double.valueOf(218.29), turns[13].getEnd().getOffset());
    assertEquals("interviewer", turns[13].getLabel());

    assertEquals(Double.valueOf(220.696), turns[14].getStart().getOffset());
    assertEquals(Double.valueOf(223.227), turns[14].getEnd().getOffset());
    assertEquals("interviewer", turns[14].getLabel());

    assertEquals(Double.valueOf(229.852), turns[15].getStart().getOffset());
    assertEquals(Double.valueOf(285.864), turns[15].getEnd().getOffset());
    assertEquals("participant", turns[15].getLabel());

    assertEquals(Double.valueOf(285.864), turns[16].getStart().getOffset());
    assertEquals(Double.valueOf(295.115), turns[16].getEnd().getOffset());
    assertEquals("interviewer", turns[16].getLabel());

    assertEquals(Double.valueOf(295.115), turns[17].getStart().getOffset());
    assertEquals(Double.valueOf(302.834), turns[17].getEnd().getOffset());
    assertEquals("participant", turns[17].getLabel());

    assertEquals(Double.valueOf(302.834), turns[18].getStart().getOffset());
    assertEquals(Double.valueOf(304.334), turns[18].getEnd().getOffset());
    assertEquals("interviewer", turns[18].getLabel());

    assertEquals(Double.valueOf(304.334), turns[19].getStart().getOffset());
    assertEquals(Double.valueOf(306.92), turns[19].getEnd().getOffset());
    assertEquals("participant", turns[19].getLabel());

    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals(139, utterances.length);
    assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
    assertEquals(Double.valueOf(5.75), utterances[0].getEnd().getOffset());
    assertEquals("participant", utterances[0].getParent().getLabel());
    assertEquals(turns[0], utterances[0].getParent());

    assertEquals(Double.valueOf(5.75), utterances[1].getStart().getOffset());
    assertEquals(Double.valueOf(6.907), utterances[1].getEnd().getOffset());
    assertEquals("participant", utterances[1].getParent().getLabel());
    assertEquals(turns[0], utterances[1].getParent());

    assertEquals(Double.valueOf(44.255), utterances[21].getStart().getOffset());
    assertEquals(Double.valueOf(45.505), utterances[21].getEnd().getOffset());
    assertEquals("interviewer", utterances[21].getParent().getLabel());
    assertEquals(turns[1], utterances[21].getParent());

    for (Annotation u : utterances) {
      assertNotNull("Utterance start time set: " + u + "("+u.getStart()+"-"+u.getEnd()+")",
                    u.getStart().getOffset());
      assertNotEquals("Utterance start time confidence: "+u+"("+u.getStart()+"-"+u.getEnd()+")",
                      Long.valueOf(Constants.CONFIDENCE_MANUAL), u.getStart().getConfidence());
      assertNotNull("Utterance end time set: " + u + "("+u.getStart()+"-"+u.getEnd()+")",
                    u.getEnd().getOffset());
      assertNotEquals("Utterance end time confidence: "+u+"("+u.getStart()+"-"+u.getEnd()+")",
                      Long.valueOf(Constants.CONFIDENCE_MANUAL), u.getEnd().getConfidence());
    }

    Annotation[] words = g.all("word");
    String[] wordLabels = { // NB we have a c-unit layer, so terminators are stripped off 
      "and", "ah .", "Cyril", "would", "arrive", "at", "the", "door",
      "with", "this", "letter", "for", "Mum", 
      "and", "and", "then", "there", "was", "a", "message .", 
      "and", "I", "think", "they", "both", "had", "telephones ."
    };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
      assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
                   i+1, words[i].getOrdinal());
      assertEquals(turns[0].getId(), words[i].getParentId());
    }

    // comment
    Annotation[] comments = g.all("comment");
    assertEquals("unclear", comments[0].getLabel());
    assertEquals("in", comments[0].getStart().endOf("word").iterator().next().getLabel());
    assertEquals("--", comments[0].getEnd().startOf("word").iterator().next().getLabel());

    assertEquals("break in recording", comments[3].getLabel());
    assertEquals("there", comments[3].getStart().endOf("word").iterator().next().getLabel());
    assertEquals("no following words: " + comments[3].getEnd().startOf("word"),
                 0, comments[3].getEnd().startOf("word").size());

    assertEquals("participant: sits down", comments[4].getLabel());
    assertEquals("interviewer: unclear", comments[5].getLabel());

    assertEquals(6, comments.length);

    // noise
    Annotation[] noises = g.all("noise");
    assertEquals("throatclear", noises[0].getLabel());
    assertEquals(words[1].getEnd(), noises[0].getStart());
    assertEquals(words[2].getStart(), noises[0].getEnd());

    assertEquals("interviewer: clears throat", noises[1].getLabel());
    assertEquals("and -", noises[1].getStart().endOf("word").iterator().next().getLabel());
    assertEquals("--", noises[1].getEnd().startOf("word").iterator().next().getLabel());

    assertEquals("both laugh", noises[2].getLabel());
    assertEquals("that ?", noises[2].getStart().endOf("word").iterator().next().getLabel());
    assertEquals("well", noises[2].getEnd().startOf("word").iterator().next().getLabel()); // TODO cross-speaker link ok?

    assertEquals("cough", noises[3].getLabel());
    assertEquals("microphone movement noise", noises[4].getLabel());

    // pronounce
    Annotation[] pronounce = g.all("pronounce");
    assertEquals("sIr@l", pronounce[0].getLabel());
    assertTrue(pronounce[0].tags(words[2]));
    assertEquals("o", pronounce[1].getLabel());
    assertEquals("o~", pronounce[1].first("word").getLabel());
    assertEquals(2, pronounce.length);

    // lexical
    Annotation[] lexical = g.all("lexical");
    assertEquals("Cyril", lexical[0].getLabel());
    assertTrue(lexical[0].tags(words[2]));
    assertEquals("often", lexical[1].getLabel());
    assertEquals("o~", lexical[1].first("word").getLabel());
    assertEquals(2, lexical.length);
      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }
  }

  @Test public void utterance_utf8_no_conventions()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("noise", "Noise", 2, true, false, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("lexical", "Lexical", 0, true, false, false, "word", true),
      new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "test_utterance_utf-8.TextGrid")) };
      
    // create deserializer
    TextGridSerialization deserializer = new TextGridSerialization();
      
    // general configuration
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    configuration.get("useConventions").setValue(Boolean.FALSE);
    //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals(8, deserializer.configure(configuration, schema).size());

    // load the stream
    ParameterSet defaultParamaters = deserializer.load(streams, schema);
    //for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
    assertEquals(2, defaultParamaters.size());

    // configure the deserialization
    deserializer.setParameters(defaultParamaters);
      
    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
    assertEquals("No warnings",
                 0, deserializer.getWarnings().length);
      
    assertEquals("test_utterance_utf-8.TextGrid", g.getId());

    // participants     
    Annotation[] who = g.all("who");
    assertEquals(2, who.length);
    assertEquals("participant", who[0].getLabel());
    assertEquals(g, who[0].getParent());
    assertNotNull("participants are anchored to the graph", who[0].getStart());
    assertNotNull("participants are anchored to the graph", who[0].getEnd());
    assertNotNull("participants are anchored in time", who[0].getStart().getOffset());
    assertNotNull("participants are anchored in time", who[0].getEnd().getOffset());
    assertEquals("interviewer", who[1].getLabel());
    assertEquals(g, who[1].getParent());
      
    // turns
    Annotation[] turns = g.all("turn");
    assertEquals(20, turns.length);
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    assertEquals(Double.valueOf(44.255), turns[0].getEnd().getOffset());
    assertEquals("participant", turns[0].getLabel());
    assertEquals(who[0], turns[0].getParent());
      
    for (Annotation u : g.all("utterance")) {
      assertNotNull("Utterance start time set: " + u + "("+u.getStart()+"-"+u.getEnd()+")",
                    u.getStart().getOffset());
      assertNotEquals("Utterance start time confidence: "+u+"("+u.getStart()+"-"+u.getEnd()+")",
                      Long.valueOf(Constants.CONFIDENCE_MANUAL), u.getStart().getConfidence());
      assertNotNull("Utterance end time set: " + u + "("+u.getStart()+"-"+u.getEnd()+")",
                    u.getEnd().getOffset());
      assertNotEquals("Utterance end time confidence: "+u+"("+u.getStart()+"-"+u.getEnd()+")",
                      Long.valueOf(Constants.CONFIDENCE_MANUAL), u.getEnd().getConfidence());
    }

    Annotation[] words = g.all("word");
    String[] wordLabels = { // NB we have a c-unit layer, so terminators are stripped off 
      "and", "äh .", "[throatclear]", "Cyril[sIr@l]", "would", "arrive", "at", "the", "door",
      "with", "this", "letter", "for", "Mum", 
      "and", "and", "then", "there", "was", "a", "message .", 
      "and", "I", "think", "they", "both", "had", "telephones ."
    };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
      assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
                   i+1, words[i].getOrdinal());
      assertEquals(turns[0].getId(), words[i].getParentId());
    }
      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }
  }

  @Test public void utterance_utf16()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("noise", "Noise", 2, true, false, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("lexical", "Lexical", 0, true, false, false, "word", true),
      new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "test_utterance_utf-16.TextGrid")) };
      
    // create deserializer
    TextGridSerialization deserializer = new TextGridSerialization();
      
    // general configuration
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    configuration.get("useConventions").setValue(Boolean.TRUE);
    //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals(8, deserializer.configure(configuration, schema).size());

    // load the stream
    ParameterSet defaultParamaters = deserializer.load(streams, schema);
    //for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
    assertEquals(2, defaultParamaters.size());

    // configure the deserialization
    deserializer.setParameters(defaultParamaters);
      
    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
    assertEquals("No warnings",
                 0, deserializer.getWarnings().length);
      
    assertEquals("test_utterance_utf-16.TextGrid", g.getId());

    // participants     
    Annotation[] who = g.all("who");
    assertEquals(2, who.length);
    assertEquals("participant", who[0].getLabel());
    assertEquals(g, who[0].getParent());
    assertNotNull("participants are anchored to the graph", who[0].getStart());
    assertNotNull("participants are anchored to the graph", who[0].getEnd());
    assertNotNull("participants are anchored in time", who[0].getStart().getOffset());
    assertNotNull("participants are anchored in time", who[0].getEnd().getOffset());
    assertEquals("interviewer", who[1].getLabel());
    assertEquals(g, who[1].getParent());
      
    // turns
    Annotation[] turns = g.all("turn");
    assertEquals(20, turns.length);
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    assertEquals(Double.valueOf(44.255), turns[0].getEnd().getOffset());
    assertEquals("participant", turns[0].getLabel());
    assertEquals(who[0], turns[0].getParent());
      
    Annotation[] words = g.all("word");
    String[] wordLabels = { // NB we have a c-unit layer, so terminators are stripped off 
      "and", "äh .", "Cyril", "would", "arrive", "at", "the", "door",
      "with", "this", "letter", "for", "Mum", 
      "and", "and", "then", "there", "was", "a", "message .", 
      "and", "I", "think", "they", "both", "had", "telephones ."
    };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
      assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
                   i+1, words[i].getOrdinal());
      assertEquals(turns[0].getId(), words[i].getParentId());
    }
      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }
  }

  @Test public void utterance_latin1()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("noise", "Noise", 2, true, false, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("lexical", "Lexical", 0, true, false, false, "word", true),
      new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "test_utterance_latin1.TextGrid")) };
      
    // create deserializer
    TextGridSerialization deserializer = new TextGridSerialization();
      
    // general configuration
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    configuration.get("useConventions").setValue(Boolean.TRUE);
    //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals(8, deserializer.configure(configuration, schema).size());

    // load the stream
    ParameterSet defaultParamaters = deserializer.load(streams, schema);
    //for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
    assertEquals(2, defaultParamaters.size());

    // configure the deserialization
    deserializer.setParameters(defaultParamaters);
      
    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
    assertEquals("No warnings",
                 0, deserializer.getWarnings().length);
      
    assertEquals("test_utterance_latin1.TextGrid", g.getId());

    // participants     
    Annotation[] who = g.all("who");
    assertEquals(2, who.length);
    assertEquals("participant", who[0].getLabel());
    assertEquals(g, who[0].getParent());
    assertNotNull("participants are anchored to the graph", who[0].getStart());
    assertNotNull("participants are anchored to the graph", who[0].getEnd());
    assertNotNull("participants are anchored in time", who[0].getStart().getOffset());
    assertNotNull("participants are anchored in time", who[0].getEnd().getOffset());
    assertEquals("interviewer", who[1].getLabel());
    assertEquals(g, who[1].getParent());
      
    // turns
    Annotation[] turns = g.all("turn");
    assertEquals(20, turns.length);
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    assertEquals(Double.valueOf(44.255), turns[0].getEnd().getOffset());
    assertEquals("participant", turns[0].getLabel());
    assertEquals(who[0], turns[0].getParent());
      
    Annotation[] words = g.all("word");
    String[] wordLabels = { // NB we have a c-unit layer, so terminators are stripped off 
      "and", "äh .", "Cyril", "would", "arrive", "at", "the", "door",
      "with", "this", "letter", "for", "Mum", 
      "and", "and", "then", "there", "was", "a", "message .", 
      "and", "I", "think", "they", "both", "had", "telephones ."
    };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
      assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
                   i+1, words[i].getOrdinal());
      assertEquals(turns[0].getId(), words[i].getParentId());
    }
      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }
  }

  @Test public void utterance_word()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("noise", "Noise", 2, true, false, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      // tiers are called "phone ..." but layer is called "segment",
      // but they should map by default despite the different names
      new Layer("segment", "Phones", 2, true, true, true, "word", true),
      new Layer("lexical", "Lexical", 0, true, false, false, "word", true),
      new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));

    // word layer can have a transcriber module as manager
    // make sure that doesn't interfere with tier/layer mapping
    schema.getLayer("word").put("layer_manager_id", "nzilbb.transcriber.deepspeech");
      
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "test_utterance_word.TextGrid")) };
      
    // create deserializer
    TextGridSerialization deserializer = new TextGridSerialization();
      
    // general configuration
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals(8, deserializer.configure(configuration, schema).size());

    // load the stream
    ParameterSet defaultParamaters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
    assertEquals("word tier is mapped by default",
                 "word", ((Layer)defaultParamaters.get("tier2").getValue()).getId());
    assertEquals("other word tier is mapped by default",
                 "word", ((Layer)defaultParamaters.get("tier2").getValue()).getId());
    assertEquals(6, defaultParamaters.size());

    // configure the deserialization
    deserializer.setParameters(defaultParamaters);
      
    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
    assertEquals("No warnings",
                 0, deserializer.getWarnings().length);
      
    assertEquals("test_utterance_word.TextGrid", g.getId());

    // participants     
    Annotation[] who = g.all("who");
    assertEquals(2, who.length);
    assertEquals("participant", who[0].getLabel());
    assertEquals(g, who[0].getParent());
    assertNotNull("participants are anchored to the graph", who[0].getStart());
    assertNotNull("participants are anchored to the graph", who[0].getEnd());
    assertNotNull("participants are anchored in time", who[0].getStart().getOffset());
    assertNotNull("participants are anchored in time", who[0].getEnd().getOffset());
    assertEquals("interviewer", who[1].getLabel());
    assertEquals(g, who[1].getParent());
      
    // turns
    Annotation[] turns = g.all("turn");
    assertEquals(20, turns.length);
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    assertEquals(Double.valueOf(44.255), turns[0].getEnd().getOffset());
    assertEquals("participant", turns[0].getLabel());
    assertEquals(who[0], turns[0].getParent());
      
    assertEquals(Double.valueOf(44.255), turns[1].getStart().getOffset());
    assertEquals(Double.valueOf(45.505), turns[1].getEnd().getOffset());
    assertEquals("interviewer", turns[1].getLabel());
    assertEquals(who[1], turns[1].getParent());

    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals(139, utterances.length);
    assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
    assertEquals(Double.valueOf(5.75), utterances[0].getEnd().getOffset());
    assertEquals("participant", utterances[0].getParent().getLabel());
    assertEquals(turns[0], utterances[0].getParent());

    assertEquals(Double.valueOf(5.75), utterances[1].getStart().getOffset());
    assertEquals(Double.valueOf(6.907), utterances[1].getEnd().getOffset());
    assertEquals("participant", utterances[1].getParent().getLabel());
    assertEquals(turns[0], utterances[1].getParent());

    assertEquals(Double.valueOf(44.255), utterances[21].getStart().getOffset());
    assertEquals(Double.valueOf(45.505), utterances[21].getEnd().getOffset());
    assertEquals("interviewer", utterances[21].getParent().getLabel());
    assertEquals(turns[1], utterances[21].getParent());

    Annotation[] words = g.all("word");
    String[] wordLabels = { // NB we have a c-unit layer, so terminators are stripped off 
      "and", "ah .", "Cyril", "would", "arrive", "at", "the", "door",
      "with", "this", "letter", "for", "Mum", 
      "and", "and", "then", "there", "was", "a", "message .", 
      "and", "I", "think", "they", "both", "had", "telephones ."
    };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
      assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
                   i+1, words[i].getOrdinal());
      assertEquals(turns[0].getId(), words[i].getParentId());
    }

    // no convention annotations, because the utterances are not tokenized
    assertEquals("no conventional comments", 0, g.all("comment").length);
    assertEquals("no conventional noises", 0, g.all("noise").length);
    assertEquals("no conventional pronounce annotations", 0, g.all("pronounce").length);
    assertEquals("no conventional lexical annotations", 0, g.all("lexical").length);

    // phones
    Annotation[] phones = g.all("segment");
    assertEquals("phones", 13, phones.length);

    assertEquals("phone", "I", phones[0].getLabel());
    assertEquals("phone parent", "is", phones[0].getParent().getLabel());
    assertEquals("phone", "z", phones[1].getLabel());
    assertEquals("phone parent", "is", phones[1].getParent().getLabel());

    assertEquals("phone simultaneous speech", "$", phones[2].getLabel());
    assertEquals("phone parent simultaneous speech", "or", phones[2].getParent().getLabel());

    assertEquals("phone simultaneous speech", "s", phones[3].getLabel());
    assertEquals("phone parent simultaneous speech", "some", phones[3].getParent().getLabel());

    assertEquals("phone simultaneous speech", "V", phones[4].getLabel());
    assertEquals("phone parent simultaneous speech", "some", phones[4].getParent().getLabel());

    assertEquals("phone simultaneous speech", "m", phones[5].getLabel());
    assertEquals("phone parent simultaneous speech", "some", phones[5].getParent().getLabel());

    assertEquals("phone simultaneous speech", "n", phones[6].getLabel());
    assertEquals("phone parent simultaneous speech", "and", phones[6].getParent().getLabel());
      
    // interviewer

    assertEquals("phone simultaneous speech", "j", phones[7].getLabel());
    assertEquals("phone parent simultaneous speech", "yeah", phones[7].getParent().getLabel());

    assertEquals("phone simultaneous speech", "8", phones[8].getLabel());
    assertEquals("phone parent simultaneous speech", "yeah", phones[8].getParent().getLabel());

    assertEquals("phone simultaneous speech", "j", phones[9].getLabel());
    assertEquals("phone parent simultaneous speech", "yeah", phones[9].getParent().getLabel());

    assertEquals("phone simultaneous speech", "8", phones[10].getLabel());
    assertEquals("phone parent simultaneous speech", "yeah", phones[10].getParent().getLabel());

    assertEquals("phone simultaneous speech", "j", phones[11].getLabel());
    assertEquals("phone parent simultaneous speech", "yeah --", phones[11].getParent().getLabel());

    assertEquals("phone simultaneous speech", "8", phones[12].getLabel());
    assertEquals("phone parent simultaneous speech", "yeah --", phones[12].getParent().getLabel());
  }

  @Test public void utterance_word_ignorePhones()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("phone", "Phones", 2, true, true, true, "word", true));
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "test_utterance_word.TextGrid")) };
      
    // create deserializer
    TextGridSerialization deserializer = new TextGridSerialization();
      
    // general configuration
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals(8, deserializer.configure(configuration, schema).size());

    // load the stream
    ParameterSet defaultParamaters = deserializer.load(streams, schema);
    //for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
    assertEquals(6, defaultParamaters.size());

    // configure the deserialization
    defaultParamaters.get("tier4").setValue(null);
    defaultParamaters.get("tier5").setValue(null);
    deserializer.setParameters(defaultParamaters);
      
    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
    assertEquals("No warnings: " + Arrays.asList(deserializer.getWarnings()),
                 0, deserializer.getWarnings().length);
      
    assertEquals("test_utterance_word.TextGrid", g.getId());

    // participants     
    Annotation[] who = g.all("who");
    assertEquals(2, who.length);
    assertEquals("participant", who[0].getLabel());
    assertEquals(g, who[0].getParent());
    assertNotNull("participants are anchored to the graph", who[0].getStart());
    assertNotNull("participants are anchored to the graph", who[0].getEnd());
    assertNotNull("participants are anchored in time", who[0].getStart().getOffset());
    assertNotNull("participants are anchored in time", who[0].getEnd().getOffset());
    assertEquals("interviewer", who[1].getLabel());
    assertEquals(g, who[1].getParent());
      
    // turns
    Annotation[] turns = g.all("turn");
    assertEquals(20, turns.length);
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    assertEquals(Double.valueOf(44.255), turns[0].getEnd().getOffset());
    assertEquals("participant", turns[0].getLabel());
    assertEquals(who[0], turns[0].getParent());
      
    assertEquals(Double.valueOf(44.255), turns[1].getStart().getOffset());
    assertEquals(Double.valueOf(45.505), turns[1].getEnd().getOffset());
    assertEquals("interviewer", turns[1].getLabel());
    assertEquals(who[1], turns[1].getParent());

    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals(139, utterances.length);
    assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
    assertEquals(Double.valueOf(5.75), utterances[0].getEnd().getOffset());
    assertEquals("participant", utterances[0].getParent().getLabel());
    assertEquals(turns[0], utterances[0].getParent());

    assertEquals(Double.valueOf(5.75), utterances[1].getStart().getOffset());
    assertEquals(Double.valueOf(6.907), utterances[1].getEnd().getOffset());
    assertEquals("participant", utterances[1].getParent().getLabel());
    assertEquals(turns[0], utterances[1].getParent());

    assertEquals(Double.valueOf(44.255), utterances[21].getStart().getOffset());
    assertEquals(Double.valueOf(45.505), utterances[21].getEnd().getOffset());
    assertEquals("interviewer", utterances[21].getParent().getLabel());
    assertEquals(turns[1], utterances[21].getParent());

    Annotation[] words = g.all("word");
    String[] wordLabels = { // NB we have a c-unit layer, so terminators are stripped off 
      "and", "ah .", "Cyril", "would", "arrive", "at", "the", "door",
      "with", "this", "letter", "for", "Mum", 
      "and", "and", "then", "there", "was", "a", "message .", 
      "and", "I", "think", "they", "both", "had", "telephones ."
    };
    for (int i = 0; i < wordLabels.length; i++)
    {
      assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
      assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
                   i+1, words[i].getOrdinal());
      assertEquals(turns[0].getId(), words[i].getParentId());
    }

    // no convention annotations, because the utterances are not tokenized
    assertEquals("no conventional comments", 0, g.all("comment").length);
    assertEquals("no conventional noises", 0, g.all("noise").length);
    assertEquals("no conventional pronounce annotations", 0, g.all("pronounce").length);
    assertEquals("no conventional lexical annotations", 0, g.all("lexical").length);

    // phones
    assertEquals("no phones", 0, g.all("phone").length);
      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }
  }

  @Test public void word_only()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("noise", "Noise", 2, true, false, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("phone", "Phones", 2, true, true, true, "word", true),
      new Layer("lexical", "Lexical", 0, true, false, false, "word", true),
      new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "test_utterance_word.TextGrid")) };
      
    // create deserializer
    TextGridSerialization deserializer = new TextGridSerialization();
      
    // general configuration
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals(8, deserializer.configure(configuration, schema).size());
      
    // load the stream
    ParameterSet defaultParamaters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
    assertEquals(6, defaultParamaters.size());

    // ignore all but word tiers
    defaultParamaters.get("tier0").setValue(null); // participant
    defaultParamaters.get("tier1").setValue(null); // interviewer
    defaultParamaters.get("tier4").setValue(null); // phone - participant
    defaultParamaters.get("tier5").setValue(null); // phone - interviewer
    deserializer.setParameters(defaultParamaters);
      
    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
    assertEquals("No warnings",
                 0, deserializer.getWarnings().length);
      
    assertEquals("test_utterance_word.TextGrid", g.getId());

    // participants     
    Annotation[] who = g.all("who");
    assertEquals("Correct number of speakers: " + Arrays.asList(who),
                 2, who.length);
    assertEquals("participant", who[0].getLabel());
    assertEquals(g, who[0].getParent());
    assertEquals("interviewer", who[1].getLabel());
    assertEquals(g, who[1].getParent());
    assertNotNull("participants are anchored to the graph", who[0].getStart());
    assertNotNull("participants are anchored to the graph", who[0].getEnd());
    assertNotNull("participants are anchored in time", who[0].getStart().getOffset());
    assertNotNull("participants are anchored in time", who[0].getEnd().getOffset());
    assertEquals("interviewer", who[1].getLabel());
    assertEquals(g, who[1].getParent());
      
    // turns
    Annotation[] turns = g.all("turn");
    assertEquals(20, turns.length);
    assertEquals("participant", turns[0].getLabel());
    assertEquals(who[0], turns[0].getParent());
    assertEquals("interviewer", turns[1].getLabel());
    assertEquals(who[1], turns[1].getParent());

    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals(20, utterances.length);
    assertEquals("participant", utterances[0].getParent().getLabel());
    assertEquals(turns[0], utterances[0].getParent());
    assertEquals("interviewer", utterances[1].getParent().getLabel());
    assertEquals(turns[1], utterances[1].getParent());

    Annotation[] words = g.all("word");
    String[] wordLabels = { // NB we have a c-unit layer, so terminators are stripped off 
      "and", "ah .", "Cyril", "would", "arrive", "at", "the", "door",
      "with", "this", "letter", "for", "Mum", 
      "and", "and", "then", "there", "was", "a", "message .", 
      "and", "I", "think", "they", "both", "had", "telephones ."
    };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
      assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
                   i+1, words[i].getOrdinal());
      assertEquals(turns[0].getId(), words[i].getParentId());
    }

    // no convention annotations, because the utterances are not tokenized
    assertEquals("no conventional comments", 0, g.all("comment").length);
    assertEquals("no conventional noises", 0, g.all("noise").length);
    assertEquals("no conventional pronounce annotations", 0, g.all("pronounce").length);
    assertEquals("no conventional lexical annotations", 0, g.all("lexical").length);

    // phones
    Annotation[] phones = g.all("phone");
    assertEquals("phones", 0, phones.length);

      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }
  }

  @Test public void turn_utterance_word()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("noise", "Noise", 2, true, false, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("lexical", "Lexical", 0, true, false, false, "word", true),
      new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "test_turn_utterance_word.TextGrid")) };
      
    // create deserializer
    TextGridSerialization deserializer = new TextGridSerialization();
      
    // general configuration
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals(8, deserializer.configure(configuration, schema).size());

    // load the stream
    ParameterSet defaultParamaters = deserializer.load(streams, schema);
    //for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
    assertEquals(6, defaultParamaters.size());

    // configure the deserialization
    deserializer.setParameters(defaultParamaters);
      
    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
    assertEquals("No warnings",
                 0, deserializer.getWarnings().length);
      
    assertEquals("test_turn_utterance_word.TextGrid", g.getId());

    // participants     
    Annotation[] who = g.all("who");
    assertEquals(2, who.length);
    assertEquals("participant", who[0].getLabel());
    assertEquals(g, who[0].getParent());
    assertNotNull("participants are anchored to the graph", who[0].getStart());
    assertNotNull("participants are anchored to the graph", who[0].getEnd());
    assertNotNull("participants are anchored in time", who[0].getStart().getOffset());
    assertNotNull("participants are anchored in time", who[0].getEnd().getOffset());
    assertEquals("interviewer", who[1].getLabel());
    assertEquals(g, who[1].getParent());
      
    // turns
    Annotation[] turns = g.all("turn");
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    assertEquals(Double.valueOf(44.255), turns[0].getEnd().getOffset());
    assertEquals("participant", turns[0].getLabel());
    assertEquals(who[0], turns[0].getParent());
      
    assertEquals(Double.valueOf(44.255), turns[1].getStart().getOffset());
    assertEquals(Double.valueOf(45.505), turns[1].getEnd().getOffset());
    assertEquals("interviewer", turns[1].getLabel());
    assertEquals(who[1], turns[1].getParent());

    assertEquals(Double.valueOf(45.505), turns[2].getStart().getOffset());
    assertEquals(Double.valueOf(107.804), turns[2].getEnd().getOffset());
    assertEquals("participant", turns[2].getLabel());

    assertEquals(Double.valueOf(107.804), turns[3].getStart().getOffset());
    assertEquals(Double.valueOf(110.335), turns[3].getEnd().getOffset());
    assertEquals("interviewer", turns[3].getLabel());

    assertEquals(Double.valueOf(110.335), turns[4].getStart().getOffset());
    assertEquals(Double.valueOf(181.652), turns[4].getEnd().getOffset());
    assertEquals("participant", turns[4].getLabel());

    assertEquals(Double.valueOf(181.652), turns[5].getStart().getOffset());
    assertEquals(Double.valueOf(183.995), turns[5].getEnd().getOffset());
    assertEquals("interviewer", turns[5].getLabel());

    assertEquals(Double.valueOf(183.995), turns[6].getStart().getOffset());
    assertEquals(Double.valueOf(185.339), turns[6].getEnd().getOffset());
    assertEquals("participant", turns[6].getLabel());

    assertEquals(Double.valueOf(185.339), turns[7].getStart().getOffset());
    assertEquals(Double.valueOf(189.464), turns[7].getEnd().getOffset());
    assertEquals("interviewer", turns[7].getLabel());

    assertEquals(Double.valueOf(189.464), turns[8].getStart().getOffset());
    assertEquals(Double.valueOf(191.682), turns[8].getEnd().getOffset());
    assertEquals("participant", turns[8].getLabel());

    assertEquals(Double.valueOf(191.682), turns[9].getStart().getOffset());
    assertEquals(Double.valueOf(197.181), turns[9].getEnd().getOffset());
    assertEquals("interviewer", turns[9].getLabel());

    assertEquals(Double.valueOf(197.181), turns[10].getStart().getOffset());
    assertEquals(Double.valueOf(199.213), turns[10].getEnd().getOffset());
    assertEquals("participant", turns[10].getLabel());

    assertEquals(Double.valueOf(199.213), turns[11].getStart().getOffset());
    assertEquals(Double.valueOf(205.415), turns[11].getEnd().getOffset());
    assertEquals("interviewer", turns[11].getLabel());

    // simultaneous speech
    // the textgrid has two itervals for this which have been joined into one turn
    assertEquals(Double.valueOf(205.415), turns[12].getStart().getOffset());
    assertEquals(Double.valueOf(220.696), turns[12].getEnd().getOffset());
    assertEquals("participant", turns[12].getLabel());
    // simultaneous speech
    assertEquals(Double.valueOf(214.822), turns[13].getStart().getOffset());
    assertEquals(Double.valueOf(218.29), turns[13].getEnd().getOffset());
    assertEquals("interviewer", turns[13].getLabel());

    assertEquals(Double.valueOf(220.696), turns[14].getStart().getOffset());
    assertEquals(Double.valueOf(223.227), turns[14].getEnd().getOffset());
    assertEquals("interviewer", turns[14].getLabel());

    assertEquals(Double.valueOf(229.852), turns[15].getStart().getOffset());
    assertEquals(Double.valueOf(285.864), turns[15].getEnd().getOffset());
    assertEquals("participant", turns[15].getLabel());

    assertEquals(Double.valueOf(285.864), turns[16].getStart().getOffset());
    assertEquals(Double.valueOf(295.115), turns[16].getEnd().getOffset());
    assertEquals("interviewer", turns[16].getLabel());

    assertEquals(Double.valueOf(295.115), turns[17].getStart().getOffset());
    assertEquals(Double.valueOf(302.834), turns[17].getEnd().getOffset());
    assertEquals("participant", turns[17].getLabel());

    assertEquals(Double.valueOf(302.834), turns[18].getStart().getOffset());
    assertEquals(Double.valueOf(304.334), turns[18].getEnd().getOffset());
    assertEquals("interviewer", turns[18].getLabel());

    assertEquals(Double.valueOf(304.334), turns[19].getStart().getOffset());
    assertEquals(Double.valueOf(306.92), turns[19].getEnd().getOffset());
    assertEquals("participant", turns[19].getLabel());

    assertEquals(20, turns.length);

    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals(139, utterances.length);
    assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
    assertEquals(Double.valueOf(5.75), utterances[0].getEnd().getOffset());
    assertEquals("participant", utterances[0].getParent().getLabel());
    assertEquals(turns[0], utterances[0].getParent());

    assertEquals(Double.valueOf(5.75), utterances[1].getStart().getOffset());
    assertEquals(Double.valueOf(6.907), utterances[1].getEnd().getOffset());
    assertEquals("participant", utterances[1].getParent().getLabel());
    assertEquals(turns[0], utterances[1].getParent());

    assertEquals(Double.valueOf(44.255), utterances[21].getStart().getOffset());
    assertEquals(Double.valueOf(45.505), utterances[21].getEnd().getOffset());
    assertEquals("interviewer", utterances[21].getParent().getLabel());
    assertEquals(turns[1], utterances[21].getParent());

    Annotation[] words = g.all("word");
    String[] wordLabels = {
      "and", "ah .", "Cyril", "would", "arrive", "at", "the", "door",
      "with", "this", "letter", "for", "Mum", 
      "and", "and", "then", "there", "was", "a", "message .", 
      "and", "I", "think", "they", "both", "had", "telephones ."
    };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
      assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
                   i+1, words[i].getOrdinal());
      assertEquals(turns[0].getId(), words[i].getParentId());
    }

    // check the simultaneous speech worked out all right
    words = turns[13].annotations("word");
    String[] simultaneousSpeech = {"yeah","yeah", "yeah --", "well", "that's", "right"};
    for (int i = 0; i < simultaneousSpeech.length; i++) {
      assertEquals("simultaneous speech " + i, simultaneousSpeech[i], words[i].getLabel());
      assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
                   i+1, words[i].getOrdinal());
    }
    assertEquals("simultaneous speech", simultaneousSpeech.length, words.length);

    // no convention annotations, because the utterances are not tokenized
    assertEquals("no conventional comments", 0, g.all("comment").length);
    assertEquals("no conventional noises", 0, g.all("noise").length);
    assertEquals("no conventional pronounce annotations", 0, g.all("pronounce").length);
    assertEquals("no conventional lexical annotations", 0, g.all("lexical").length);
      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }
  }

  @Test public void speaker_word()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("topic", "Topic", 2, true, false, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("noise", "Noise", 2, true, false, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("entity", "Named Entities", 2, true, false, false, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("lexical", "Lexical", 0, true, false, false, "word", true),
      new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "test_speaker_word.TextGrid")) };
      
    // create deserializer
    TextGridSerialization deserializer = new TextGridSerialization();
      
    // general configuration
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals(8, deserializer.configure(configuration, schema).size());

    // load the stream
    ParameterSet defaultParamaters = deserializer.load(streams, schema);
    //for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
    assertEquals(4, defaultParamaters.size());

    // configure the deserialization
    deserializer.setParameters(defaultParamaters);
      
    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
    assertEquals("No warnings",
                 0, deserializer.getWarnings().length);
      
    assertEquals("test_speaker_word.TextGrid", g.getId());

    // participants     
    Annotation[] who = g.all("who");
    assertEquals(2, who.length);
    assertEquals("participant", who[0].getLabel());
    assertEquals(g, who[0].getParent());
    assertNotNull("participants are anchored to the graph", who[0].getStart());
    assertNotNull("participants are anchored to the graph", who[0].getEnd());
    assertNotNull("participants are anchored in time", who[0].getStart().getOffset());
    assertNotNull("participants are anchored in time", who[0].getEnd().getOffset());
    assertEquals("interviewer", who[1].getLabel());
    assertEquals(g, who[1].getParent());
      
    // turns
    Annotation[] turns = g.all("turn");
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    assertEquals(Double.valueOf(44.255), turns[0].getEnd().getOffset());
    assertEquals("participant", turns[0].getLabel());
    assertEquals(who[0], turns[0].getParent());
      
    assertEquals(Double.valueOf(44.255), turns[1].getStart().getOffset());
    assertEquals(Double.valueOf(45.505), turns[1].getEnd().getOffset());
    assertEquals("interviewer", turns[1].getLabel());
    assertEquals(who[1], turns[1].getParent());

    assertEquals(Double.valueOf(45.505), turns[2].getStart().getOffset());
    assertEquals(Double.valueOf(107.804), turns[2].getEnd().getOffset());
    assertEquals("participant", turns[2].getLabel());

    assertEquals(Double.valueOf(107.804), turns[3].getStart().getOffset());
    assertEquals(Double.valueOf(110.335), turns[3].getEnd().getOffset());
    assertEquals("interviewer", turns[3].getLabel());

    assertEquals(Double.valueOf(110.335), turns[4].getStart().getOffset());
    assertEquals(Double.valueOf(181.652), turns[4].getEnd().getOffset());
    assertEquals("participant", turns[4].getLabel());

    assertEquals(Double.valueOf(181.652), turns[5].getStart().getOffset());
    assertEquals(Double.valueOf(183.995), turns[5].getEnd().getOffset());
    assertEquals("interviewer", turns[5].getLabel());

    assertEquals(Double.valueOf(183.995), turns[6].getStart().getOffset());
    assertEquals(Double.valueOf(185.339), turns[6].getEnd().getOffset());
    assertEquals("participant", turns[6].getLabel());

    assertEquals(Double.valueOf(185.339), turns[7].getStart().getOffset());
    assertEquals(Double.valueOf(189.464), turns[7].getEnd().getOffset());
    assertEquals("interviewer", turns[7].getLabel());

    assertEquals(Double.valueOf(189.464), turns[8].getStart().getOffset());
    assertEquals(Double.valueOf(191.682), turns[8].getEnd().getOffset());
    assertEquals("participant", turns[8].getLabel());

    assertEquals(Double.valueOf(191.682), turns[9].getStart().getOffset());
    assertEquals(Double.valueOf(197.181), turns[9].getEnd().getOffset());
    assertEquals("interviewer", turns[9].getLabel());

    assertEquals(Double.valueOf(197.181), turns[10].getStart().getOffset());
    assertEquals(Double.valueOf(199.213), turns[10].getEnd().getOffset());
    assertEquals("participant", turns[10].getLabel());

    assertEquals(Double.valueOf(199.213), turns[11].getStart().getOffset());
    assertEquals(Double.valueOf(205.415), turns[11].getEnd().getOffset());
    assertEquals("interviewer", turns[11].getLabel());

    assertEquals(Double.valueOf(205.415), turns[12].getStart().getOffset());
    assertEquals(Double.valueOf(220.696), turns[12].getEnd().getOffset());
    assertEquals("participant", turns[12].getLabel());

    assertEquals(Double.valueOf(220.696), turns[13].getStart().getOffset());
    assertEquals(Double.valueOf(223.227), turns[13].getEnd().getOffset());
    assertEquals("interviewer", turns[13].getLabel());

    assertEquals(Double.valueOf(229.852), turns[14].getStart().getOffset());
    assertEquals(Double.valueOf(285.864), turns[14].getEnd().getOffset());
    assertEquals("participant", turns[14].getLabel());

    assertEquals(Double.valueOf(285.864), turns[15].getStart().getOffset());
    assertEquals(Double.valueOf(295.115), turns[15].getEnd().getOffset());
    assertEquals("interviewer", turns[15].getLabel());

    assertEquals(Double.valueOf(295.115), turns[16].getStart().getOffset());
    assertEquals(Double.valueOf(302.834), turns[16].getEnd().getOffset());
    assertEquals("participant", turns[16].getLabel());

    assertEquals(Double.valueOf(302.834), turns[17].getStart().getOffset());
    assertEquals(Double.valueOf(304.334), turns[17].getEnd().getOffset());
    assertEquals("interviewer", turns[17].getLabel());

    assertEquals(Double.valueOf(304.334), turns[18].getStart().getOffset());
    assertEquals(Double.valueOf(306.92), turns[18].getEnd().getOffset());
    assertEquals("participant", turns[18].getLabel());

    assertEquals(19, turns.length);

    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals(138, utterances.length);
    assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
    assertEquals(Double.valueOf(5.75), utterances[0].getEnd().getOffset());
    assertEquals("participant", utterances[0].getParent().getLabel());
    assertEquals(turns[0], utterances[0].getParent());

    assertEquals(Double.valueOf(5.75), utterances[1].getStart().getOffset());
    assertEquals(Double.valueOf(6.907), utterances[1].getEnd().getOffset());
    assertEquals("participant", utterances[1].getParent().getLabel());
    assertEquals(turns[0], utterances[1].getParent());

    assertEquals(Double.valueOf(44.255), utterances[21].getStart().getOffset());
    assertEquals(Double.valueOf(45.505), utterances[21].getEnd().getOffset());
    assertEquals("interviewer", utterances[21].getParent().getLabel());
    assertEquals(turns[1], utterances[21].getParent());

    for (Annotation u : utterances) {
      assertNotNull("Utterance start time set: " + u + "("+u.getStart()+"-"+u.getEnd()+")",
                    u.getStart().getOffset());
      assertNotEquals("Utterance start time confidence: "+u+"("+u.getStart()+"-"+u.getEnd()+")",
                      Long.valueOf(Constants.CONFIDENCE_MANUAL), u.getStart().getConfidence());
      assertNotNull("Utterance end time set: " + u + "("+u.getStart()+"-"+u.getEnd()+")",
                    u.getEnd().getOffset());
      assertNotEquals("Utterance end time confidence: "+u+"("+u.getStart()+"-"+u.getEnd()+")",
                      Long.valueOf(Constants.CONFIDENCE_MANUAL), u.getEnd().getConfidence());
    }

    Annotation[] words = g.all("word");
    String[] wordLabels = {
      "and", "ah .", "Cyril", "would", "arrive", "at", "the", "door",
      "with", "this", "letter", "for", "Mum", 
      "and", "and", "then", "there", "was", "a", "message .", 
      "and", "I", "think", "they", "both", "had", "telephones ."
    };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
      assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
                   i+1, words[i].getOrdinal());
      assertEquals(turns[0].getId(), words[i].getParentId());
    }

    words = turns[13].annotations("word");
    String[] simultaneousSpeech = {"ah", "yeah", "just", "turn", "left", "there"};
    for (int i = 0; i < simultaneousSpeech.length; i++) {
      assertEquals("other speaker words " + i, simultaneousSpeech[i], words[i].getLabel());
      assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
                   i+1, words[i].getOrdinal());
    }
    assertEquals("other speaker words", simultaneousSpeech.length, words.length);

    // no convention annotations, because the utterances are not tokenized
    assertEquals("no conventional comments", 0, g.all("comment").length);
    assertEquals("no conventional noises", 0, g.all("noise").length);
    assertEquals("no conventional pronounce annotations", 0, g.all("pronounce").length);
    assertEquals("no conventional lexical annotations", 0, g.all("lexical").length);

    // topic
    Annotation[] topics = g.all("topic");
    assertEquals(2, topics.length);
    assertEquals(Double.valueOf(79.726), topics[0].getStart().getOffset());
    assertEquals(Double.valueOf(107.804), topics[0].getEnd().getOffset());
    assertEquals("hospital", topics[0].getLabel());
    assertEquals(g, topics[0].getParent());

    assertEquals(Double.valueOf(124.1557142857143), topics[1].getStart().getOffset());
    assertEquals(Double.valueOf(181.652), topics[1].getEnd().getOffset());
    assertEquals("barry", topics[1].getLabel());
    assertEquals(g, topics[1].getParent());

    // named entity
    Annotation[] entities = g.all("entity");
    assertEquals(6, entities.length);
    assertEquals("person", entities[0].getLabel());
    assertEquals("Cyril", entities[0].tagsOn("word")[0].getLabel());

    assertEquals("person", entities[1].getLabel());
    assertEquals("Molly", entities[1].tagsOn("word")[0].getLabel());

    assertEquals("person", entities[2].getLabel());
    assertEquals("Molly", entities[2].tagsOn("word")[0].getLabel());

    assertEquals("person", entities[3].getLabel());
    assertEquals("Molly", entities[3].tagsOn("word")[0].getLabel());

    assertEquals("person", entities[4].getLabel());
    assertEquals("Molly", entities[4].tagsOn("word")[0].getLabel());

    assertEquals("place", entities[5].getLabel());
    assertEquals("Ohakia .", entities[5].tagsOn("word")[0].getLabel());
      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }
  }

  @Test public void basFragment()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("noise", "Noise", 2, true, false, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("phone", "Phones", 2, true, true, true, "word", true),
      new Layer("lexical", "Lexical", 0, true, false, false, "word", true),
      new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "fragment__1_890-3_830.TextGrid")) };
      
    // create deserializer
    TextGridSerialization deserializer = new TextGridSerialization();
      
    // general configuration
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals("Default utteranceThreshold",
                 Double.valueOf(0.5), configuration.get("utteranceThreshold").getValue());
    // no utterance inference
    configuration.get("utteranceThreshold").setValue(Double.valueOf(0.0));
    assertEquals(8, deserializer.configure(configuration, schema).size());
    assertEquals("utteranceThreshold",
                 Double.valueOf(0.0), deserializer.getUtteranceThreshold());

    // load the stream
    ParameterSet parameters = deserializer.load(streams, schema);
    //for (Parameter p : parameters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
    assertEquals(3, parameters.size());

    parameters.get("tier0").setValue(schema.getWordLayer()); // ORT
    parameters.get("tier1").setValue(null); // KAN
    parameters.get("tier2").setValue(schema.getLayer("phone")); // MAU TODO configurable
    // for (Parameter p : parameters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());

    // configure the deserialization
    deserializer.setParameters(parameters);
      
    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
    assertEquals("Two warnings (about orphans)",
                 2, deserializer.getWarnings().length);
      
    assertEquals("fragment__1_890-3_830.TextGrid", g.getId());

    // participants     
    Annotation[] who = g.all("who");
    assertEquals(1, who.length);
    assertEquals("ORT", who[0].getLabel());
    assertEquals(g, who[0].getParent());
    assertNotNull("participants are anchored to the graph", who[0].getStart());
    assertNotNull("participants are anchored to the graph", who[0].getEnd());
    assertNotNull("participants are anchored in time", who[0].getStart().getOffset());
    assertNotNull("participants are anchored in time", who[0].getEnd().getOffset());
      
    // turns
    Annotation[] turns = g.all("turn");
    assertEquals(1, turns.length);
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    assertEquals(Double.valueOf(1.93), turns[0].getEnd().getOffset());
    assertEquals("ORT", turns[0].getLabel());
    assertEquals(who[0], turns[0].getParent());

    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals(1, utterances.length);
    assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
    assertEquals(Double.valueOf(1.93), utterances[0].getEnd().getOffset());
    assertEquals("ORT", utterances[0].getParent().getLabel());
    assertEquals(turns[0], utterances[0].getParent());

    Annotation[] words = g.all("word");
    String[] wordLabels = { // NB we have a c-unit layer, so terminators are stripped off 
      "so", "what", "is", "your", "name"
    };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
      assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
                   i+1, words[i].getOrdinal());
      assertEquals(turns[0].getId(), words[i].getParentId());
    }

    // no convention annotations, because the utterances are not tokenized
    assertEquals("no conventional comments", 0, g.all("comment").length);
    assertEquals("no conventional noises", 0, g.all("noise").length);
    assertEquals("no conventional pronounce annotations", 0, g.all("pronounce").length);
    assertEquals("no conventional lexical annotations", 0, g.all("lexical").length);

    // phones
    Annotation[] phones = g.all("phone");
    assertEquals("phones", 14, phones.length);

    // participant

    assertEquals("phone", "s", phones[0].getLabel());
    assertEquals("phone parent", "so", phones[0].getParent().getLabel());
    assertEquals("phone", "@U", phones[1].getLabel());
    assertEquals("phone parent", "so", phones[1].getParent().getLabel());

    assertEquals("phone - word doesn't quite t-include phone", "w", phones[2].getLabel());
    assertEquals("phone parent - doesn't t-include", "what", phones[2].getParent().getLabel());
    assertEquals("phone parent - share anchors despite offset mismatch",
                 phones[2].getParent().getStart(), phones[2].getStart());
    assertEquals("phone", "Q", phones[3].getLabel());
    assertEquals("phone parent", "what", phones[3].getParent().getLabel());
    assertEquals("phone", "t", phones[4].getLabel());
    assertEquals("phone parent", "what", phones[4].getParent().getLabel());

    assertEquals("phone", "I", phones[5].getLabel());
    assertEquals("phone parent", "is", phones[5].getParent().getLabel());
    assertEquals("phone", "z", phones[6].getLabel());
    assertEquals("phone parent", "is", phones[6].getParent().getLabel());

    assertEquals("phone", "j", phones[7].getLabel());
    assertEquals("phone parent", "your", phones[7].getParent().getLabel());
    assertEquals("phone", "@", phones[8].getLabel());
    assertEquals("phone parent", "your", phones[8].getParent().getLabel());

    assertEquals("phone", "n", phones[9].getLabel());
    assertEquals("phone parent", "name", phones[9].getParent().getLabel());
    assertEquals("phone", "eI", phones[10].getLabel());
    assertEquals("phone parent", "name", phones[10].getParent().getLabel());
    assertEquals("phone", "m", phones[11].getLabel());
    assertEquals("phone parent", "name", phones[11].getParent().getLabel());

    // orphans
    assertEquals("orphan phone", "<p:>", phones[12].getLabel());
    assertNull("no phone parent", phones[12].getParent());

    assertEquals("orphan phone", "<p:>", phones[13].getLabel());
    assertNull("no phone parent", phones[13].getParent());
      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }
  }

  /** Test MFA-style full TextGrid, with words/phones tier, 
   * ensuring participant ID and utterances are inferred. */
  @Test public void mfa()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      // tiers are called "phones" but layer is called "segment",
      // but they should map by default despite the different names
      new Layer("segment", "Phones").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true));

    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "test_mfa.TextGrid")) };
      
    // create deserializer
    TextGridSerialization deserializer = new TextGridSerialization();
      
    // general configuration
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals(8, deserializer.configure(configuration, schema).size());

    // load the stream
    ParameterSet defaultParamaters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
    assertEquals("words tier is mapped by default",
                 "word", ((Layer)defaultParamaters.get("tier0").getValue()).getId());
    assertEquals("phones tier is mapped by default",
                 "segment", ((Layer)defaultParamaters.get("tier1").getValue()).getId());
    assertEquals(2, defaultParamaters.size());

    // configure the deserialization
    deserializer.setParameters(defaultParamaters);
      
    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];
    
    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
    assertEquals("No warnings",
                 0, deserializer.getWarnings().length);
    
    assertEquals("test_mfa.TextGrid", g.getId());
    
    // participants     
    Annotation[] who = g.all("who");
    assertEquals(1, who.length);
    assertEquals("Participant is named after file",
                 "test_mfa", who[0].getLabel());
    assertEquals(g, who[0].getParent());
      
    // turns
    Annotation[] turns = g.all("turn");
    assertEquals("Multiple turns", 19, turns.length);
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    assertEquals(Double.valueOf(2.2390000000000043), turns[0].getEnd().getOffset());
    assertEquals("test_mfa", turns[0].getLabel());
    assertEquals(who[0], turns[0].getParent());
      
    assertEquals(Double.valueOf(2.75), turns[1].getStart().getOffset());
    assertEquals(Double.valueOf(41.81199999999998), turns[1].getEnd().getOffset());
    assertEquals("test_mfa", turns[1].getLabel());
    assertEquals(who[0], turns[1].getParent());

    // utterances
    Annotation[] utterances = g.all("utterance");    
    assertEquals("Multiple utterances", turns.length, utterances.length);
    // one utterance per turn
    for (int i = 0; i < turns.length; i++) {      
      assertEquals("utterance " + i + " start",
                   turns[i].getStartId(), utterances[i].getStartId());
      assertEquals("utterance " + i + " end",
                   turns[i].getEndId(), utterances[i].getEndId());
      assertEquals("utterance " + i + " label",
                   turns[i].getLabel(), utterances[i].getLabel());
      assertEquals("utterance " + i + " parent",
                   turns[i].getId(), utterances[i].getParentId());
    }

    Annotation[] words = g.all("word");
    String[] wordLabels = {
      "wsa's", "a", "labarapa", "alb", "kal", "kal", "appaal", "pa", "avarybaby",
      "a", "asab", "pa", "pall", "sksaal", "basprakps", "wsal", "a", "walp", "al" 
    };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
      if (i < 9) { // first utterance/turn
        assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
                     i+1, words[i].getOrdinal());
        assertEquals(turns[0].getId(), words[i].getParentId());
      } else { // second utterance/turn
        assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
                     i-8, words[i].getOrdinal());
        assertEquals(turns[1].getId(), words[i].getParentId());
      }
    }

    // phones
    Annotation[] phones = g.all("segment");
    assertEquals("number of phones", 2325, phones.length);

    assertEquals("phone", "s", phones[0].getLabel());
    assertEquals("phone parent", "wsa's", phones[0].getParent().getLabel());
    assertEquals("phone", "a", phones[1].getLabel());
    assertEquals("phone parent", "wsa's", phones[1].getParent().getLabel());
  }

  /** Test that a TextGrid with only annotation intervals, no turns/utterances/words, can
   * be deserialized.  */
  @Test public void intervals_only()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("comment", "Comment")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false),
      new Layer("noise", "Noise")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false),
      new Layer("emotion", "Emotions")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterances")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("lexical", "Lexical")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true),
      new Layer("pronounce", "Pronounce")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true));
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "test_intervals_only.TextGrid")) };
    
    // create deserializer
    TextGridSerialization deserializer = new TextGridSerialization();
    
    // general configuration
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals(8, deserializer.configure(configuration, schema).size());
    
    // load the stream
    ParameterSet defaultParamaters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
    assertEquals(1, defaultParamaters.size());

    // configure the deserialization
    deserializer.setParameters(defaultParamaters);
    
    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];
    
    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
    assertEquals("No warnings", 0, deserializer.getWarnings().length);
      
    assertEquals("test_intervals_only.TextGrid", g.getId());

    // no standard layers
    assertNull("No participant layer in the schema", g.getLayer("who"));
    assertNull("No turn layer in the schema", g.getLayer("turn"));
    assertNull("No utterance layer in the schema", g.getLayer("utterance"));
    assertNull("No word layer in the schema", g.getLayer("word"));
    assertEquals(0, g.all("who").length);
    assertEquals(0, g.all("turn").length);
    assertEquals(0, g.all("utterance").length);
    assertEquals(0, g.all("word").length);
      
    // intervals
    Annotation[] annotations = g.all("emotion");
    assertEquals(17, annotations.length);

    assertEquals(Double.valueOf(0.0), annotations[0].getStart().getOffset());
    assertEquals(Double.valueOf(17.361156398104264), annotations[0].getEnd().getOffset());
    assertEquals("neutral", annotations[0].getLabel());
    assertEquals(g, annotations[0].getParent());
    
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }
  }

  @Test public void performance()  throws Exception {
    Schema schema = new Schema(
      "participant", "turn", "utterance", "word",
      new Layer("participant", "Participants", 0, true, true, true),
      new Layer("middle-750", "middle-750", 2, true, false, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "participant", true),
      new Layer("disfluency", "disfluency", 2, true, false, false, "turn", true),
      new Layer("IntS", "IntS", 2, true, false, false, "turn", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("segment", "Phones", 2, true, false, true, "word", true),
      new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "performance.TextGrid")) };
      
    // create deserializer
    TextGridSerialization deserializer = new TextGridSerialization();
    deserializer.setTimers(new nzilbb.util.Timers());
      
    // general configuration
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals(8, deserializer.configure(configuration, schema).size());

    // load the stream
    ParameterSet defaultParamaters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
    assertEquals(8, defaultParamaters.size());

    // configure the deserialization
    deserializer.setParameters(defaultParamaters);
      
    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
    assertEquals("Four warnings (about orphans) " + deserializer.getWarnings(),
                 4, deserializer.getWarnings().length);

    assertEquals("performance.TextGrid", g.getId());

    assertTrue("Deserialization too slow:\n" + deserializer.getTimers().toString(),
               2000 > deserializer.getTimers().getTotals().get("deserialize"));
    //System.out.println("Timers: " + deserializer.getTimers());
      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }
  }
  
  @Test public void serialize_utterance_word()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("noise", "Noise", 2, true, false, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("phone", "Phones", 2, true, true, true, "word", true),
      new Layer("lexical", "Lexical", 0, true, false, false, "word", true),
      new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
    File dir = getDir();
    // access file
    NamedStream[] jsonStreams = { new NamedStream(new File(dir, "serialize_utterance_word.json")) };
      
    // deserialize graph from JSON
    JSONSerialization json = new JSONSerialization();
    json.configure(json.configure(new ParameterSet(), schema), schema);
    json.setParameters(json.load(jsonStreams, schema));
    Graph[] graphs = json.deserialize();

    // create serializer
    TextGridSerialization serializer = new TextGridSerialization();
      
    // general configuration
    ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
    //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals(8, serializer.configure(configuration, schema).size());

    String[] needLayers = serializer.getRequiredLayers();
    assertEquals(4, needLayers.length);
    assertEquals("who", needLayers[0]);
    assertEquals("turn", needLayers[1]);
    assertEquals("utterance", needLayers[2]);
    assertEquals("word", needLayers[3]);
	 
    // serialize
    final Vector<SerializationException> exceptions = new Vector<SerializationException>();
    final Vector<NamedStream> streams = new Vector<NamedStream>();
    serializer.serialize(Arrays.spliterator(graphs), null,
                         stream -> streams.add(stream),
                         warning -> System.out.println(warning),
                         exception -> exceptions.add(exception));
    streams.elementAt(0).save(dir);

    // test using diff
    File expected = new File(dir, "expected_serialize_utterance_word.TextGrid");
    File actual = new File(dir, "serialize_utterance_word.TextGrid");
    String differences = diff(expected, actual);
    if (differences != null) {
      fail(differences);
    } else {
      actual.delete();
    }
  }

  @Test public void serialize_fragment_utterance_word()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("noise", "Noise", 2, true, false, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("phone", "Phones", 2, true, true, true, "word", true),
      new Layer("f1", "First Formant", 1, true, true, false, "phone", false),
      new Layer("lexical", "Lexical", 0, true, false, false, "word", true),
      new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
    File dir = getDir();
    // access file
    NamedStream[] jsonStreams = { new NamedStream(new File(dir, "serialize_utterance_word.json")) };
      
    // deserialize graph from JSON
    JSONSerialization json = new JSONSerialization();
    json.configure(json.configure(new ParameterSet(), schema), schema);
    json.setParameters(json.load(jsonStreams, schema));
    Graph[] graphs = json.deserialize();

    // extract fragment
    double fragmentFrom = 212.4;
    double fragmentTo = 216.36333; // exactly on the offset of the last anchor
    String [] layerIds = { "utterance", "word", "phone" };
    Graph fragment = graphs[0].getFragment(fragmentFrom, fragmentTo, layerIds);
    fragment.shiftAnchors(-fragmentFrom);
    assertEquals("serialize_utterance_word__212.400-216.363", fragment.getId());
    Graph[] fragments = { fragment };

    // create serializer
    TextGridSerialization serializer = new TextGridSerialization();
      
    // general configuration
    ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
    //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals(8, serializer.configure(configuration, schema).size());

    String[] needLayers = serializer.getRequiredLayers();
    assertEquals(4, needLayers.length);
    assertEquals("who", needLayers[0]);
    assertEquals("turn", needLayers[1]);
    assertEquals("utterance", needLayers[2]);
    assertEquals("word", needLayers[3]);
	 
    // serialize
    final Vector<SerializationException> exceptions = new Vector<SerializationException>();
    final Vector<NamedStream> streams = new Vector<NamedStream>();
    serializer.serialize(Arrays.spliterator(fragments), layerIds,
                         stream -> streams.add(stream),
                         warning -> System.out.println(warning),
                         exception -> exceptions.add(exception));
    assertEquals(1, streams.size());
    streams.elementAt(0).save(dir);

    // test using diff
    File expected = new File(dir, "expected_serialize_utterance_word__212.4-216.36333.TextGrid");
    File actual = new File(dir, "serialize_utterance_word__212.400-216.363.TextGrid");
    String differences = diff(expected, actual);
    if (differences != null) {
      fail(differences);
    } else {
      actual.delete();
    }
  }

  @Test public void serialize_selected_layer()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("noise", "Noise", 2, true, false, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("phone", "Phones", 2, true, true, true, "word", true),
      new Layer("f1", "First Formant", 1, true, true, false, "phone", false),
      new Layer("lexical", "Lexical", 0, true, false, false, "word", true),
      new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
    File dir = getDir();
    // access file
    NamedStream[] jsonStreams = { new NamedStream(new File(dir, "serialize_utterance_word.json")) };
      
    // deserialize graph from JSON
    JSONSerialization json = new JSONSerialization();
    json.configure(json.configure(new ParameterSet(), schema), schema);
    json.setParameters(json.load(jsonStreams, schema));
    Graph[] graphs = json.deserialize();

    // extract fragment
    double fragmentFrom = 212.4;
    double fragmentTo = 216.36333; // exactly on the offset of the last anchor
    String [] layerIds = { "utterance", "word", "phone" };
    Graph fragment = graphs[0].getFragment(fragmentFrom, fragmentTo, layerIds);
    fragment.shiftAnchors(-fragmentFrom);
    assertEquals("serialize_utterance_word__212.400-216.363", fragment.getId());
    Graph[] fragments = { fragment };

    // create serializer
    TextGridSerialization serializer = new TextGridSerialization();
      
    // general configuration
    ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
    //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals(8, serializer.configure(configuration, schema).size());

    String[] needLayers = serializer.getRequiredLayers();
    assertEquals(4, needLayers.length);
    assertEquals("who", needLayers[0]);
    assertEquals("turn", needLayers[1]);
    assertEquals("utterance", needLayers[2]);
    assertEquals("word", needLayers[3]);
	 
    String[] selectedLayers = { "phone" };
    // serialize
    final Vector<SerializationException> exceptions = new Vector<SerializationException>();
    final Vector<NamedStream> streams = new Vector<NamedStream>();
    serializer.serialize(Arrays.spliterator(fragments), selectedLayers,
                         stream -> streams.add(stream),
                         warning -> System.out.println(warning),
                         exception -> exceptions.add(exception));
    streams.elementAt(0).setName("selected_layers__212.400-216.363.TextGrid");
    streams.elementAt(0).save(dir);

    // test using diff
    File expected = new File(dir, "expected_selected_layers__212.400-216.363.TextGrid");
    File actual = new File(dir, "selected_layers__212.400-216.363.TextGrid");
    String differences = diff(expected, actual);
    if (differences != null) {
      fail(differences);
    } else {
      actual.delete();
    }
  }

  @Test public void serialize_selected_layers_including_empty()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("noise", "Noise", 2, true, false, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("phone", "Phones", 2, true, true, true, "word", true),
      new Layer("f1", "First Formant", 1, true, true, false, "phone", false),
      new Layer("lexical", "Lexical", 0, true, false, false, "word", true),
      new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
    File dir = getDir();
    // access file
    NamedStream[] jsonStreams = { new NamedStream(new File(dir, "serialize_utterance_word.json")) };
      
    // deserialize graph from JSON
    JSONSerialization json = new JSONSerialization();
    json.configure(json.configure(new ParameterSet(), schema), schema);
    json.setParameters(json.load(jsonStreams, schema));
    Graph[] graphs = json.deserialize();

    // extract fragment
    double fragmentFrom = 0.0;
    double fragmentTo = 44.255; // exactly on the offset of the last anchor
    String [] layerIds = { "utterance", "word", "phone", "f1" };
    Graph fragment = graphs[0].getFragment(fragmentFrom, fragmentTo, layerIds);
    fragment.shiftAnchors(-fragmentFrom);
    assertEquals("serialize_utterance_word__0.000-44.255", fragment.getId());
    Graph[] fragments = { fragment };

    // create serializer
    TextGridSerialization serializer = new TextGridSerialization();
      
    // general configuration
    ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
    //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    // test invalid peer overlap allowance too
    // i.e. in "Cyril would", "would" starts on its own tier because it starts before "Cyril" ends
    configuration.get("allowPeerOverlap").setValue(Boolean.TRUE);
    assertEquals(8, serializer.configure(configuration, schema).size());

    String[] needLayers = serializer.getRequiredLayers();
    assertEquals(4, needLayers.length);
    assertEquals("who", needLayers[0]);
    assertEquals("turn", needLayers[1]);
    assertEquals("utterance", needLayers[2]);
    assertEquals("word", needLayers[3]);
	 
    String[] selectedLayers = { "word", "phone", "f1" };
    // serialize
    final Vector<SerializationException> exceptions = new Vector<SerializationException>();
    final Vector<NamedStream> streams = new Vector<NamedStream>();
    serializer.serialize(Arrays.spliterator(fragments), selectedLayers,
                         stream -> streams.add(stream),
                         warning -> System.out.println(warning),
                         exception -> exceptions.add(exception));
    streams.elementAt(0).setName("selected_layers__0.000-44.255.TextGrid");
    streams.elementAt(0).save(dir);

    // test using diff
    File expected = new File(dir, "expected_selected_layers__0.000-44.255.TextGrid");
    File actual = new File(dir, "selected_layers__0.000-44.255.TextGrid");
    String differences = diff(expected, actual);
    if (differences != null) {
      fail(differences);
    } else {
      actual.delete();
    }
  }

  @Test public void serialize_fragment_trailing_utterance_word() throws Exception { // TODO test serialization of point tiers 
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("noise", "Noise", 2, true, false, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("phone", "Phones", 2, true, true, true, "word", true),
      new Layer("lexical", "Lexical", 0, true, false, false, "word", true),
      new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
    File dir = getDir();
    // access file
    NamedStream[] jsonStreams = { new NamedStream(new File(dir, "serialize_utterance_word.json")) };
      
    // deserialize graph from JSON
    JSONSerialization json = new JSONSerialization();
    json.configure(json.configure(new ParameterSet(), schema), schema);
    json.setParameters(json.load(jsonStreams, schema));
    Graph[] graphs = json.deserialize();

    // extract fragment
    double fragmentFrom = 212.4;
    double fragmentTo = 216.5; // between anchors
    String[] layerIds = { "utterance", "word", "phone"};
    Graph fragment = graphs[0].getFragment(fragmentFrom, fragmentTo, layerIds);
    fragment.shiftAnchors(-fragmentFrom);
    assertEquals("serialize_utterance_word__212.400-216.500", fragment.getId());
    Graph[] fragments = { fragment };

    // create serializer
    TextGridSerialization serializer = new TextGridSerialization();
      
    // general configuration
    ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
    //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals("Default utteranceThreshold",
                 Double.valueOf(0.5), configuration.get("utteranceThreshold").getValue());
    assertEquals(8, serializer.configure(configuration, schema).size());

    String[] needLayers = serializer.getRequiredLayers();
    assertEquals(4, needLayers.length);
    assertEquals("who", needLayers[0]);
    assertEquals("turn", needLayers[1]);
    assertEquals("utterance", needLayers[2]);
    assertEquals("word", needLayers[3]);
	 
    // serialize
    final Vector<SerializationException> exceptions = new Vector<SerializationException>();
    final Vector<NamedStream> streams = new Vector<NamedStream>();
    serializer.serialize(Arrays.spliterator(fragments), null,
                         stream -> streams.add(stream),
                         warning -> System.out.println(warning),
                         exception -> exceptions.add(exception));
    streams.elementAt(0).save(dir);

    // test using diff
    File expected = new File(dir, "expected_serialize_utterance_word__212.4-216.500.TextGrid");
    File actual = new File(dir, "serialize_utterance_word__212.400-216.500.TextGrid");
    String differences = diff(expected, actual);
    if (differences != null) {
      fail(differences);
    } else {
      actual.delete();
    }
  }   
   
  /**
   * Diffs two files.
   * @param expected
   * @param actual
   * @return null if the files are the same, and a String describing differences if not.
   */
  public String diff(File expected, File actual) {
    StringBuffer d = new StringBuffer();
      
    try {
      // compare with what we expected
      Vector<String> actualLines = new Vector<String>();
      BufferedReader reader = new BufferedReader(new FileReader(actual));
      String line = reader.readLine();
      while (line != null) {
        actualLines.add(line);
        line = reader.readLine();
      }
      Vector<String> expectedLines = new Vector<String>();
      reader = new BufferedReader(new FileReader(expected));
      line = reader.readLine();
      while (line != null) {
        expectedLines.add(line);
        line = reader.readLine();
      }
      MinimumEditPath<String> comparator = new MinimumEditPath<String>();
      List<EditStep<String>> path = comparator.minimumEditPath(expectedLines, actualLines);
      for (EditStep<String> step : path) {
        switch (step.getOperation()) {
          case CHANGE:
            d.append("\n"+expected.getPath()+":"+(step.getFromIndex()+1)+": Expected:\n" 
                     + step.getFrom() 
                     + "\n"+actual.getPath()+":"+(step.getToIndex()+1)+": Found:\n" + step.getTo());
            break;
          case DELETE:
            d.append("\n"+expected.getPath()+":"+(step.getFromIndex()+1)+": Deleted:\n" 
                     + step.getFrom()
                     + "\n"+actual.getPath()+":"+(step.getToIndex()+1)+": Missing");
            break;
          case INSERT:
            d.append("\n"+expected.getPath()+":"+(step.getFromIndex()+1)+": Missing" 
                     + "\n"+actual.getPath()+":"+(step.getToIndex()+1)+": Inserted:\n" 
                     + step.getTo());
            break;
        }
      } // next step
    } catch(Exception exception) {
      d.append("\n" + exception);
    }
    if (d.length() > 0) return d.toString();
    return null;
  } // end of diff()

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
    org.junit.runner.JUnitCore.main("nzilbb.formatter.praat.TestTextGridSerialization");
  }
}
