//
// Copyright 2017-2022 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.formatter.text;
	      
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
import nzilbb.ag.serialize.json.JSONSerialization;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.util.Normalizer;
import nzilbb.ag.util.Validator;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.editpath.EditStep;
import nzilbb.editpath.MinimumEditPath;
import nzilbb.formatter.text.*;
import nzilbb.util.Timers;

public class TestPlainTextSerialization
{
  @Test public void comment()  throws Exception {
    Schema schema = new Schema("who", "turn", "utterance", "word",
                               new Layer("scribe", "Transcriber", 0, true, true, true),
                               new Layer("subreddit", "Subreddit name", 0, false, false, true),
                               new Layer("transcript_version_date", "Version Date", 0, false, false, true),
                               new Layer("air_date", "Publication Date", 0, false, false, true),
                               new Layer("transcript_program", "Program", 0, false, false, true),
                               new Layer("url", "URL", 0, false, false, true),
                               new Layer("who", "Participants", 0, true, true, true),
                               new Layer("topic", "Topic", 2, true, false, false),
                               new Layer("comment", "Comment", 2, true, false, true),
                               new Layer("noise", "Noise", 2, true, false, true),
                               new Layer("participant_gender", "Gender", 0, false, false, true, "who", true),
                               new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
                               new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
                               new Layer("word", "Words", 2, true, false, false, "turn", true),
                               new Layer("pronounce", "Pronounce", 0, true, false, false, "word", true),
                               new Layer("lexical", "Lexical", 0, true, false, false, "word", true));

    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "comment.txt")) };
      
    // create deserializer
    PlainTextSerialization deserializer = new PlainTextSerialization();

    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals("Configuration parameters" + configuration, 11,
                 deserializer.configure(configuration, schema).size());      
    assertEquals("comment", "comment", 
                 ((Layer)configuration.get("commentLayer").getValue()).getId());
    assertEquals("noise", "noise", 
                 ((Layer)configuration.get("noiseLayer").getValue()).getId());
    assertEquals("lexical", "lexical", 
                 ((Layer)configuration.get("lexicalLayer").getValue()).getId());
    assertEquals("pronounce", "pronounce", 
                 ((Layer)configuration.get("pronounceLayer").getValue()).getId());
    assertEquals("use conventions", Boolean.TRUE, configuration.get("useConventions").getValue());
    assertEquals("maxParticipantLength",
                 Integer.valueOf(20), configuration.get("maxParticipantLength").getValue());
    assertEquals("maxHeaderLines",
                 Integer.valueOf(50), configuration.get("maxHeaderLines").getValue());
    assertEquals("participantFormat", "{0}: ",
                 configuration.get("participantFormat").getValue());
    assertEquals("metaDataFormat", "{0}={1}",
                 configuration.get("metaDataFormat").getValue());
    assertEquals("timestampFormat",
                 "HH:mm:ss.SSS",
                 configuration.get("timestampFormat").getValue());

    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals(5, defaultParameters.size());
    assertEquals("url", "url", 
                 ((Layer)defaultParameters.get("header_url").getValue()).getId());
    assertEquals("subreddit", "subreddit", 
                 ((Layer)defaultParameters.get("header_subreddit").getValue()).getId());
    assertEquals("publication date", "air_date", 
                 ((Layer)defaultParameters.get("header_air_date").getValue()).getId());
    assertEquals("version", "transcript_version_date", 
                 ((Layer)defaultParameters.get("header_version_date").getValue()).getId());
    assertEquals("gender", "participant_gender", 
                 ((Layer)defaultParameters.get("header_gender").getValue()).getId());

    // configure the deserialization
    deserializer.setParameters(defaultParameters);

    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
      
    assertEquals("comment.txt", g.getId());
    assertEquals("text units", Constants.UNIT_CHARACTERS, g.getOffsetUnits());
    assertNotNull("schema set", g.getSchema());

    // meta data
    assertEquals("graph meta data", 
                 "exmormon", g.first("subreddit").getLabel());
    assertEquals("graph meta data", 
                 "https://www.reddit.com/r/exmormon/comments/2qyr1a#cnas8zv", g.first("url").getLabel());
    assertEquals("graph meta data", 
                 "2015-01-01T00:00:00.000Z", g.first("air_date").getLabel());
    assertEquals("graph meta data", 
                 "2015-02-28T11:51:22.000Z", g.first("transcript_version_date").getLabel());

    // participants     
    Annotation[] authors = g.all("who"); 
    assertEquals(1, authors.length);
    assertEquals("YoungModern", authors[0].getLabel());
    assertEquals("?", authors[0].first("participant_gender").getLabel());

    // turns
    Annotation[] turns = g.all("turn");
    assertEquals(1, turns.length);
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    //assertEquals(Double.valueOf(23.563), turns[0].getEnd().getOffset()); // TODO
    assertEquals("YoungModern", turns[0].getLabel());
    assertEquals(g.first("who"), turns[0].getParent());

    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals(1, utterances.length);
    assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
    assertEquals("YoungModern", utterances[0].getParent().getLabel());
    assertEquals(turns[0], utterances[0].getParent());
      
    Annotation[] words = g.all("word");
    assertEquals(Double.valueOf(0), words[0].getStart().getOffset());
    // System.out.println("" + Arrays.asList(Arrays.copyOfRange(words, 0, 10)));
    assertEquals("Most", words[0].getLabel());
    assertEquals("inter-word space", Double.valueOf(5), words[0].getEnd().getOffset());
    assertEquals("next word start where last ends",
                 Double.valueOf(5), words[1].getStart().getOffset());
    assertEquals("next word linked to last", words[0].getEnd(), words[1].getStart());
    assertEquals("of", words[1].getLabel());
    assertEquals("inter-word space", Double.valueOf(8), words[1].getEnd().getOffset());
    assertEquals("next word linked to last", words[1].getEnd(), words[2].getStart());
    assertEquals("us", words[2].getLabel());
    assertEquals("inter-word space", Double.valueOf(11), words[2].getEnd().getOffset());
    assertEquals("next word linked to last", words[2].getEnd(), words[3].getStart());
    assertEquals("have", words[3].getLabel());
    assertEquals("inter-word space", Double.valueOf(16), words[3].getEnd().getOffset());
    assertEquals("next word linked to last", words[3].getEnd(), words[4].getStart());
    assertEquals("some", words[4].getLabel());
    assertEquals("inter-word space", Double.valueOf(21), words[4].getEnd().getOffset());
    assertEquals("next word linked to last", words[4].getEnd(), words[5].getStart());
    assertEquals("family", words[5].getLabel());
    assertEquals("inter-word space", Double.valueOf(28), words[5].getEnd().getOffset());
    assertEquals("next word linked to last", words[5].getEnd(), words[6].getStart());
    assertEquals("members", words[6].getLabel());
    assertEquals("inter-word space", Double.valueOf(36), words[6].getEnd().getOffset());
    assertEquals("next word linked to last", words[6].getEnd(), words[7].getStart());
    assertEquals("like", words[7].getLabel());
    assertEquals(Double.valueOf(41), words[7].getEnd().getOffset());

    assertEquals(0, g.all("entities").length);
    assertEquals(0, g.all("language").length);
    assertEquals(0, g.all("lexical").length);
      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }

  }

  @Test public void comment2()  throws Exception {
    Schema schema = new Schema("who", "turn", "utterance", "word",
                               new Layer("scribe", "Transcriber", 0, true, true, true),
                               new Layer("subreddit", "Subreddit name", 0, false, false, true),
                               new Layer("parent_id", "Parent", 0, false, false, true),
                               new Layer("publication_time", "Publication Date", 0, false, false, true),
                               new Layer("transcript_program", "Program", 0, false, false, true),
                               new Layer("url", "URL", 0, false, false, true),
                               new Layer("who", "Participants", 0, true, true, true),
                               new Layer("topic", "Topic", 2, true, false, false),
                               new Layer("comment", "Comment", 2, true, false, true),
                               new Layer("noise", "Noise", 2, true, false, true),
                               new Layer("main_participant", "Main", 0, false, false, true, "who", true),
                               new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
                               new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
                               new Layer("word", "Words", 2, true, false, false, "turn", true),
                               new Layer("pronounce", "Pronounce", 0, true, false, false, "word", true),
                               new Layer("lexical", "Lexical", 0, true, false, false, "word", true));

    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "comment2.txt")) };
      
    // create deserializer
    PlainTextSerialization deserializer = new PlainTextSerialization();

    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals("Configuration parameters" + configuration, 11, deserializer.configure(configuration, schema).size());      
    assertEquals("comment", "comment", 
                 ((Layer)configuration.get("commentLayer").getValue()).getId());
    assertEquals("noise", "noise", 
                 ((Layer)configuration.get("noiseLayer").getValue()).getId());
    assertEquals("lexical", "lexical", 
                 ((Layer)configuration.get("lexicalLayer").getValue()).getId());
    assertEquals("pronounce", "pronounce", 
                 ((Layer)configuration.get("pronounceLayer").getValue()).getId());
    assertNull("orthography", 
               configuration.get("orthographyLayer").getValue());
    assertEquals("use conventions", Boolean.TRUE, configuration.get("useConventions").getValue());
    assertEquals("maxParticipantLength",
                 Integer.valueOf(20), configuration.get("maxParticipantLength").getValue());
    assertEquals("maxHeaderLines",
                 Integer.valueOf(50), configuration.get("maxHeaderLines").getValue());
    assertEquals("participantFormat", "{0}: ",
                 configuration.get("participantFormat").getValue());
    assertEquals("metaDataFormat", "{0}={1}",
                 configuration.get("metaDataFormat").getValue());
    assertEquals("timestampFormat",
                 "HH:mm:ss.SSS",
                 configuration.get("timestampFormat").getValue());

    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals(4, defaultParameters.size());
    assertEquals("url", "url", 
                 ((Layer)defaultParameters.get("header_url").getValue()).getId());
    assertEquals("subreddit", "subreddit", 
                 ((Layer)defaultParameters.get("header_subreddit").getValue()).getId());
    assertEquals("publication date", "publication_time", 
                 ((Layer)defaultParameters.get("header_publication_time").getValue()).getId());
    assertEquals("parent_id", "parent_id", 
                 ((Layer)defaultParameters.get("header_parent_id").getValue()).getId());

    // configure the deserialization
    deserializer.setParameters(defaultParameters);

    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
      
    assertEquals("comment2.txt", g.getId());
    assertEquals("text units", Constants.UNIT_CHARACTERS, g.getOffsetUnits());

    // meta data
    assertEquals("graph meta data", 
                 "StrangerThings", g.first("subreddit").getLabel());
    assertEquals("graph meta data", 
                 "https://www.reddit.com/r/StrangerThings/comments/5rxk10#ddb3r2n", g.first("url").getLabel());
    assertEquals("graph meta data", 
                 "2017-02-04T02:47:49.000Z", g.first("publication_time").getLabel());
    assertEquals("graph meta data", 
                 "t3_5rxk10", g.first("parent_id").getLabel());

    // participants     
    Annotation[] authors = g.all("who"); 
    assertEquals(1, authors.length);
    assertEquals("tgflp", authors[0].getLabel());
    assertNotNull("start ID set", authors[0].getStartId());
    assertEquals(Double.valueOf(0.0), authors[0].getStart().getOffset());

    // tag participant as main one
    g.addLayer(schema.getLayer("main_participant"));
    g.createTag(authors[0], "main_participant", authors[0].getLabel())
      .setConfidence(Constants.CONFIDENCE_MANUAL);
      
    // turns
    Annotation[] turns = g.all("turn");
    //assertEquals(1, turns.length);
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    //assertEquals(Double.valueOf(23.563), turns[0].getEnd().getOffset()); // TODO
    assertEquals("tgflp", turns[0].getLabel());
    assertEquals(g.first("who"), turns[0].getParent());

    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals(5, utterances.length);
    assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
    assertEquals("tgflp", utterances[0].getParent().getLabel());
    assertEquals(turns[0], utterances[0].getParent());
    assertEquals(Double.valueOf(277.0), utterances[0].getEnd().getOffset());
      
    assertEquals(Double.valueOf(277.0), utterances[1].getStart().getOffset());
    assertEquals("tgflp", utterances[1].getParent().getLabel());
    assertEquals(turns[0], utterances[1].getParent());
    assertEquals(Double.valueOf(343.0), utterances[1].getEnd().getOffset());

    assertEquals(Double.valueOf(343.0), utterances[2].getStart().getOffset());
    assertEquals("tgflp", utterances[2].getParent().getLabel());
    assertEquals(turns[0], utterances[2].getParent());
    assertEquals(Double.valueOf(345.0), utterances[2].getEnd().getOffset());

    assertEquals(Double.valueOf(345.0), utterances[3].getStart().getOffset());
    assertEquals("tgflp", utterances[3].getParent().getLabel());
    assertEquals(turns[0], utterances[3].getParent());
    assertEquals(Double.valueOf(417.0), utterances[3].getEnd().getOffset());

    assertEquals(Double.valueOf(417.0), utterances[4].getStart().getOffset());
    assertEquals("tgflp", utterances[4].getParent().getLabel());
    assertEquals(turns[0], utterances[4].getParent());
    assertEquals(Double.valueOf(454.0), utterances[4].getEnd().getOffset());
      
    Annotation[] words = g.all("word");
    assertEquals(Double.valueOf(0), words[0].getStart().getOffset());
    // System.out.println("" + Arrays.asList(Arrays.copyOfRange(words, 0, 10)));
    assertEquals("Jesus", words[0].getLabel());
    assertEquals("inter-word space", Double.valueOf(6), words[0].getEnd().getOffset());
    assertEquals("next word start where last ends",
                 Double.valueOf(6), words[1].getStart().getOffset());
    assertEquals("next word linked to last", words[0].getEnd(), words[1].getStart());
    assertEquals("christ,", words[1].getLabel());
    assertEquals("inter-word space", Double.valueOf(14), words[1].getEnd().getOffset());
    assertEquals("next word linked to last", words[1].getEnd(), words[2].getStart());
    assertEquals("if", words[2].getLabel());
    assertEquals("inter-word space", Double.valueOf(17), words[2].getEnd().getOffset());
    assertEquals("next word linked to last", words[2].getEnd(), words[3].getStart());
    assertEquals("an", words[3].getLabel());
    assertEquals("inter-word space", Double.valueOf(20), words[3].getEnd().getOffset());
    assertEquals("next word linked to last", words[3].getEnd(), words[4].getStart());
    assertEquals("actor", words[4].getLabel());
    assertEquals("inter-word space", Double.valueOf(26), words[4].getEnd().getOffset());
    assertEquals("next word linked to last", words[4].getEnd(), words[5].getStart());
    assertEquals("talking", words[5].getLabel());
    assertEquals("inter-word space", Double.valueOf(34), words[5].getEnd().getOffset());
    assertEquals("next word linked to last", words[5].getEnd(), words[6].getStart());
    assertEquals("about", words[6].getLabel());
    assertEquals("inter-word space", Double.valueOf(40), words[6].getEnd().getOffset());
    assertEquals("next word linked to last", words[6].getEnd(), words[7].getStart());
    assertEquals("politics", words[7].getLabel());
    assertEquals(Double.valueOf(49), words[7].getEnd().getOffset());

    assertEquals("line boundary",
                 Double.valueOf(265), words[49].getStart().getOffset());
    assertEquals("line boundary",
                 "here", words[49].getLabel());
    assertEquals("line boundary",
                 Double.valueOf(277), words[49].getEnd().getOffset());

    assertEquals("line boundary",
                 Double.valueOf(277), words[50].getStart().getOffset());
    assertEquals("line boundary",
                 "< In -", words[50].getLabel());
    assertEquals("line boundary",
                 Double.valueOf(284), words[50].getEnd().getOffset());

    assertEquals(0, g.all("entities").length);
    assertEquals(0, g.all("language").length);
    assertEquals(0, g.all("lexical").length);

    // test validator runs
    Normalizer normalizer = new Normalizer();
    normalizer.transform(g);
    g.commit();
    g.create();
    Validator v = new Validator();
    v.transform(g);
      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }

  }

  @Test public void elicited()  throws Exception {
    Schema schema = new Schema("who", "turn", "utterance", "word",
                               new Layer("app", "App", 0, true, true, true),
                               new Layer("appVersion", "Version", 0, false, false, true),
                               new Layer("creation_date", "Version Date", 0, false, false, true),
                               new Layer("speech_migraine", "Migraine", 0, true, false, true),
                               new Layer("who", "Participants", 0, true, true, true),
                               new Layer("comment", "Comment", 2, true, false, true),
                               new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
                               new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
                               new Layer("word", "Words", 2, true, false, false, "turn", true));

    // access file
    NamedStream[] streams = {
      new NamedStream(new File(getDir(), "elicited.txt")), // transcript
      new NamedStream(new File(getDir(), "elicited.wav")) // media file gives max anchor offset
    };
      
    // create deserializer
    PlainTextSerialization deserializer = new PlainTextSerialization();

    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals("Configuration parameters" + configuration,
                 11, deserializer.configure(configuration, schema).size());      
    assertEquals("comment", "comment", 
                 ((Layer)configuration.get("commentLayer").getValue()).getId());
    assertNull("noise", configuration.get("noiseLayer").getValue());
    assertNull("lexical", configuration.get("lexicalLayer").getValue());
    assertNull("pronounce", configuration.get("pronounceLayer").getValue());
    assertEquals("use conventions", Boolean.TRUE, configuration.get("useConventions").getValue());
    assertEquals("maxParticipantLength",
                 Integer.valueOf(20), configuration.get("maxParticipantLength").getValue());
    assertEquals("maxHeaderLines",
                 Integer.valueOf(50), configuration.get("maxHeaderLines").getValue());
    assertEquals("participantFormat", "{0}: ",
                 configuration.get("participantFormat").getValue());
    assertEquals("metaDataFormat", "{0}={1}",
                 configuration.get("metaDataFormat").getValue());
    assertEquals("timestampFormat",
                 "HH:mm:ss.SSS",
                 configuration.get("timestampFormat").getValue());

    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals(6, defaultParameters.size());
    assertEquals("app", "app", 
                 ((Layer)defaultParameters.get("header_app").getValue()).getId());
    assertEquals("appVersion", "appVersion", 
                 ((Layer)defaultParameters.get("header_appVersion").getValue()).getId());
    assertNull("no appPlatform", defaultParameters.get("header_appPlatform").getValue());
    assertNull("no appDevice", defaultParameters.get("header_appDevice").getValue());
    assertEquals("creation_date", "creation_date", 
                 ((Layer)defaultParameters.get("header_creation_date").getValue()).getId());
    assertEquals("speech_migraine", "speech_migraine", 
                 ((Layer)defaultParameters.get("header_speech_migraine").getValue()).getId());

    // configure the deserialization
    deserializer.setParameters(defaultParameters);

    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
      
    assertEquals("elicited.txt", g.getId());
    assertEquals("time units", Constants.UNIT_SECONDS, g.getOffsetUnits());

    // meta data
    assertEquals("graph meta data", 
                 "Elicit Speech", g.first("app").getLabel());
    assertEquals("graph meta data", 
                 "1.0.0", g.first("appVersion").getLabel());
    assertEquals("graph meta data", 
                 "2017-01-13T15:49:55.575Z", g.first("creation_date").getLabel());
    String[] multilineAttribute = g.labels("speech_migraine");
    assertEquals("graph meta data - multiline", 
                 2, multilineAttribute.length);
    assertEquals("graph meta data - multiline", 
                 "currently have migraine", multilineAttribute[0]);
    assertEquals("graph meta data - multiline", 
                 "second value", multilineAttribute[1]);

    // participants     
    Annotation[] authors = g.all("who"); 
    assertEquals(1, authors.length);
    assertEquals("test", authors[0].getLabel());

    // turns
    Annotation[] turns = g.all("turn");
    assertEquals(1, turns.length);
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    assertEquals("turn ends at end of recording",
                 Double.valueOf(5.2941875), turns[0].getEnd().getOffset());
    assertEquals("test", turns[0].getLabel());
    assertEquals(g.first("who"), turns[0].getParent());

    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals(1, utterances.length);
    assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
    assertEquals("utterance ends at end of recording",
                 Double.valueOf(5.2941875), utterances[0].getEnd().getOffset());
    assertEquals("test", utterances[0].getParent().getLabel());
    assertEquals(turns[0], utterances[0].getParent());
      
    Annotation[] words = g.all("word");
    assertEquals(9, words.length);
    assertNull("first word anchor unset because of preceding comment",
               words[0].getStart().getOffset());
    // System.out.println("" + Arrays.asList(words));
    assertEquals("The", words[0].getLabel());
    assertNull("inter-word anchors are unset", words[0].getEnd().getOffset());
    assertEquals("next word linked to last", words[0].getEnd(), words[1].getStart());
    assertEquals("supermarket", words[1].getLabel());
    assertNull("inter-word anchors are unset", words[1].getEnd().getOffset());
    assertEquals("next word linked to last", words[1].getEnd(), words[2].getStart());
    assertEquals("chain", words[2].getLabel());
    assertNull("inter-word anchors are unset", words[2].getEnd().getOffset());
    assertEquals("next word linked to last", words[2].getEnd(), words[3].getStart());
    assertEquals("shut", words[3].getLabel());
    assertNull("inter-word anchors are unset", words[3].getEnd().getOffset());
    assertEquals("next word linked to last", words[3].getEnd(), words[4].getStart());
    assertEquals("down", words[4].getLabel());
    assertNull("inter-word anchors are unset", words[4].getEnd().getOffset());
    assertEquals("next word linked to last", words[4].getEnd(), words[5].getStart());
    assertEquals("because", words[5].getLabel());
    assertNull("inter-word anchors are unset", words[5].getEnd().getOffset());
    assertEquals("next word linked to last", words[5].getEnd(), words[6].getStart());
    assertEquals("of", words[6].getLabel());
    assertNull("inter-word anchors are unset", words[6].getEnd().getOffset());
    assertEquals("next word linked to last", words[6].getEnd(), words[7].getStart());
    assertEquals("poor", words[7].getLabel());
    assertNull("inter-word anchors are unset", words[7].getEnd().getOffset());
    assertEquals("next word linked to last", words[7].getEnd(), words[8].getStart());
    assertEquals("management", words[8].getLabel());
    assertEquals("last word ends at end of recording",
                 Double.valueOf(5.2941875), words[8].getEnd().getOffset());

    Annotation[] comments = g.all("comment");
    assertEquals(1, comments.length);
    assertEquals("Please read the following aloud:", comments[0].getLabel());
    assertEquals(Double.valueOf(0), comments[0].getStart().getOffset());

    assertNull("Hack to skip validation for texts isn't activated for transcripts", g.get("@valid"));
      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }
  }

  @Test public void invalidAudio()  throws Exception {
    Schema schema = new Schema("who", "turn", "utterance", "word",
                               new Layer("app", "App", 0, true, true, true),
                               new Layer("appVersion", "Version", 0, false, false, true),
                               new Layer("creation_date", "Version Date", 0, false, false, true),
                               new Layer("speech_migraine", "Migraine", 0, true, false, true),
                               new Layer("who", "Participants", 0, true, true, true),
                               new Layer("comment", "Comment", 2, true, false, true),
                               new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
                               new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
                               new Layer("word", "Words", 2, true, false, false, "turn", true));

    // access file
    NamedStream[] streams = {
      new NamedStream(new File(getDir(), "invalid-audio.txt")), // transcript
      new NamedStream(new File(getDir(), "invalid-audio.wav")) // invalid media file can't give max anchor offset
    };
      
    // create deserializer
    PlainTextSerialization deserializer = new PlainTextSerialization();

    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals("Configuration parameters" + configuration,
                 11, deserializer.configure(configuration, schema).size());      
    assertEquals("comment", "comment", 
                 ((Layer)configuration.get("commentLayer").getValue()).getId());
    assertNull("noise", configuration.get("noiseLayer").getValue());
    assertNull("lexical", configuration.get("lexicalLayer").getValue());
    assertNull("pronounce", configuration.get("pronounceLayer").getValue());
    assertEquals("use conventions", Boolean.TRUE, configuration.get("useConventions").getValue());
    assertEquals("maxParticipantLength",
                 Integer.valueOf(20), configuration.get("maxParticipantLength").getValue());
    assertEquals("maxHeaderLines",
                 Integer.valueOf(50), configuration.get("maxHeaderLines").getValue());
    assertEquals("participantFormat", "{0}: ",
                 configuration.get("participantFormat").getValue());
    assertEquals("metaDataFormat", "{0}={1}",
                 configuration.get("metaDataFormat").getValue());
    assertEquals("timestampFormat",
                 "HH:mm:ss.SSS",
                 configuration.get("timestampFormat").getValue());

    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals(6, defaultParameters.size());
    assertEquals("app", "app", 
                 ((Layer)defaultParameters.get("header_app").getValue()).getId());
    assertEquals("appVersion", "appVersion", 
                 ((Layer)defaultParameters.get("header_appVersion").getValue()).getId());
    assertNull("no appPlatform", defaultParameters.get("header_appPlatform").getValue());
    assertNull("no appDevice", defaultParameters.get("header_appDevice").getValue());
    assertEquals("creation_date", "creation_date", 
                 ((Layer)defaultParameters.get("header_creation_date").getValue()).getId());
    assertEquals("speech_migraine", "speech_migraine", 
                 ((Layer)defaultParameters.get("header_speech_migraine").getValue()).getId());

    // configure the deserialization
    deserializer.setParameters(defaultParameters);

    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
      
    assertEquals("invalid-audio.txt", g.getId());
    assertEquals("time units characters because media is invalid",
                 Constants.UNIT_CHARACTERS, g.getOffsetUnits());

    // meta data
    assertEquals("graph meta data", 
                 "Elicit Speech", g.first("app").getLabel());
    assertEquals("graph meta data", 
                 "1.0.0", g.first("appVersion").getLabel());
    assertEquals("graph meta data", 
                 "2017-01-13T15:49:55.575Z", g.first("creation_date").getLabel());
    String[] multilineAttribute = g.labels("speech_migraine");
    assertEquals("graph meta data - multiline", 
                 2, multilineAttribute.length);
    assertEquals("graph meta data - multiline", 
                 "currently have migraine", multilineAttribute[0]);
    assertEquals("graph meta data - multiline", 
                 "second value", multilineAttribute[1]);

    // participants     
    Annotation[] authors = g.all("who"); 
    assertEquals(1, authors.length);
    assertEquals("test", authors[0].getLabel());

    // turns
    Annotation[] turns = g.all("turn");
    assertEquals(1, turns.length);
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    assertEquals("turn ends at last character",
                 Double.valueOf(100), turns[0].getEnd().getOffset());
    assertEquals("test", turns[0].getLabel());
    assertEquals(g.first("who"), turns[0].getParent());

    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals(1, utterances.length);
    assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
    assertEquals("utterance ends at last character",
                 Double.valueOf(100), utterances[0].getEnd().getOffset());
    assertEquals("test", utterances[0].getParent().getLabel());
    assertEquals(turns[0], utterances[0].getParent());
      
    Annotation[] words = g.all("word");
    assertEquals(9, words.length);
    // System.out.println("" + Arrays.asList(words));
    assertEquals("The", words[0].getLabel());
    assertEquals("inter-word anchors",
                 Double.valueOf(39), words[0].getEnd().getOffset());
    assertEquals("next word linked to last", words[0].getEnd(), words[1].getStart());
    assertEquals("supermarket", words[1].getLabel());
    assertEquals("inter-word anchors",
                 Double.valueOf(51), words[1].getEnd().getOffset());
    assertEquals("next word linked to last", words[1].getEnd(), words[2].getStart());
    assertEquals("chain", words[2].getLabel());
    assertEquals("inter-word anchors",
                 Double.valueOf(57), words[2].getEnd().getOffset());
    assertEquals("next word linked to last", words[2].getEnd(), words[3].getStart());
    assertEquals("shut", words[3].getLabel());
    assertEquals("inter-word anchors",
                 Double.valueOf(62), words[3].getEnd().getOffset());
    assertEquals("next word linked to last", words[3].getEnd(), words[4].getStart());
    assertEquals("down", words[4].getLabel());
    assertEquals("inter-word anchors",
                 Double.valueOf(67), words[4].getEnd().getOffset());
    assertEquals("next word linked to last", words[4].getEnd(), words[5].getStart());
    assertEquals("because", words[5].getLabel());
    assertEquals("inter-word anchors",
                 Double.valueOf(75), words[5].getEnd().getOffset());
    assertEquals("next word linked to last", words[5].getEnd(), words[6].getStart());
    assertEquals("of", words[6].getLabel());
    assertEquals("inter-word anchors",
                 Double.valueOf(78), words[6].getEnd().getOffset());
    assertEquals("next word linked to last", words[6].getEnd(), words[7].getStart());
    assertEquals("poor", words[7].getLabel());
    assertEquals("inter-word anchors",
                 Double.valueOf(83), words[7].getEnd().getOffset());
    assertEquals("next word linked to last", words[7].getEnd(), words[8].getStart());
    assertEquals("management", words[8].getLabel());
    assertEquals("last word ends at last character",
                 Double.valueOf(100), words[8].getEnd().getOffset());

    Annotation[] comments = g.all("comment");
    assertEquals(1, comments.length);
    assertEquals("Please read the following aloud:", comments[0].getLabel());
    assertEquals(Double.valueOf(0), comments[0].getStart().getOffset());
      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }

  }

  @Test public void interview()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("scribe", "Transcriber", 0, true, true, true),
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("noise", "Noise", 2, true, false, true),
      new Layer("participant_gender", "Gender", 0, false, false, true, "who", true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("pronounce", "Pronounce", 0, true, false, false, "word", true),
      new Layer("lexical", "Lexical", 0, true, false, false, "word", true));
      
    // access file
    NamedStream[] streams = {
      new NamedStream(new File(getDir(), "interview.txt")) }; // transcript
      
    // create deserializer
    PlainTextSerialization deserializer = new PlainTextSerialization();

    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals("Configuration parameters" + configuration,
                 11, deserializer.configure(configuration, schema).size());      
    assertEquals("comment", "comment", 
                 ((Layer)configuration.get("commentLayer").getValue()).getId());
    assertEquals("noise", "noise", 
                 ((Layer)configuration.get("noiseLayer").getValue()).getId());
    assertEquals("lexical", "lexical", 
                 ((Layer)configuration.get("lexicalLayer").getValue()).getId());
    assertEquals("pronounce", "pronounce", 
                 ((Layer)configuration.get("pronounceLayer").getValue()).getId());
    assertEquals("use conventions", Boolean.TRUE, configuration.get("useConventions").getValue());
    assertEquals("maxParticipantLength",
                 Integer.valueOf(20), configuration.get("maxParticipantLength").getValue());
    assertEquals("maxHeaderLines",
                 Integer.valueOf(50), configuration.get("maxHeaderLines").getValue());
    assertEquals("participantFormat", "{0}: ",
                 configuration.get("participantFormat").getValue());
    assertEquals("metaDataFormat", "{0}={1}",
                 configuration.get("metaDataFormat").getValue());
    assertEquals("timestampFormat",
                 "HH:mm:ss.SSS",
                 configuration.get("timestampFormat").getValue());

    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals(0, defaultParameters.size());

    // no parameters, so configuration required - don't call deserializer.setParameters(defaultParameters);

    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];
      
    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
      
    assertEquals("interview.txt", g.getId());
    assertEquals("time units", Constants.UNIT_CHARACTERS, g.getOffsetUnits());


    // participants     
    Annotation[] authors = g.all("who"); 
    assertEquals(2, authors.length);
    assertEquals("BOM is stripped from speaker name",
                 "mop03-2b", authors[0].getLabel());
    assertEquals("Interviewer", authors[1].getLabel());

    // turns
    Annotation[] turns = g.all("turn");
    assertEquals(13, turns.length);
    // assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    // assertEquals("turn ends at end of recording",
    // 		   Double.valueOf(5.2941875), turns[0].getEnd().getOffset());
    assertEquals("BOM is stripped from speaker name",
                 "mop03-2b", turns[0].getLabel());
    assertEquals(authors[0], turns[0].getParent());
      
    assertEquals("Interviewer", turns[1].getLabel());
    assertEquals(authors[1], turns[1].getParent());

    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals(149, utterances.length);
    // assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
    // assertEquals("utterance ends at end of recording",
    // 		   Double.valueOf(5.2941875), utterances[0].getEnd().getOffset());
    // assertEquals("test", utterances[0].getParent().getLabel());
    assertEquals(turns[0], utterances[0].getParent());
    assertEquals(turns[0], utterances[1].getParent());
    assertEquals(turns[0], utterances[2].getParent());
    assertEquals(turns[0], utterances[3].getParent());
    assertEquals(turns[0], utterances[4].getParent());

    assertEquals(turns[1], utterances[5].getParent());
	    
    Annotation[] words = g.all("word");
    assertEquals(804, words.length);
    // System.out.println("" + Arrays.asList(words));
    assertEquals("This", words[0].getLabel());
    assertEquals("file", words[1].getLabel());
    assertEquals("starts", words[2].getLabel());
    assertEquals("with", words[3].getLabel());
    assertEquals("the", words[4].getLabel());
    assertEquals("UTF-8", words[5].getLabel());
    assertEquals("BOM", words[6].getLabel());
    assertEquals("Elipsis is kept, but taken as word boundary",
                 "Elipisis…", words[7].getLabel());
    assertEquals("is", words[8].getLabel());
    assertEquals("kept", words[9].getLabel());

    Annotation[] comments = g.all("comment");
    assertEquals(1, comments.length);
    assertEquals("unclear", comments[0].getLabel());

    Annotation[] noises = g.all("noise");
    assertEquals(1, noises.length);
    assertEquals("click", noises[0].getLabel());
      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }

  }

  @Test public void transcribeme()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("scribe", "Transcriber", 0, true, true, true),
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("noise", "Noise", 2, true, false, true),
      new Layer("participant_gender", "Gender", 0, false, false, true, "who", true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("pronounce", "Pronounce", 0, true, false, false, "word", true),
      new Layer("lexical", "Lexical", 0, true, false, false, "word", true));
      
    // access file
    NamedStream[] streams = {
      new NamedStream(new File(getDir(), "transcribeme.txt")) }; // transcript
      
    // create deserializer
    PlainTextSerialization deserializer = new PlainTextSerialization();

    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      
    // adjust patterns for participant and meta-data
    configuration.get("participantFormat").setValue("{0} ");
    // also don't use speech conventions
    configuration.get("useConventions").setValue(Boolean.FALSE);
	 
    assertEquals("Configuration parameters" + configuration,
                 11, deserializer.configure(configuration, schema).size());      
    assertEquals("comment", "comment", 
                 ((Layer)configuration.get("commentLayer").getValue()).getId());
    assertEquals("noise", "noise", 
                 ((Layer)configuration.get("noiseLayer").getValue()).getId());
    assertEquals("lexical", "lexical", 
                 ((Layer)configuration.get("lexicalLayer").getValue()).getId());
    assertEquals("pronounce", "pronounce", 
                 ((Layer)configuration.get("pronounceLayer").getValue()).getId());
    assertEquals("use conventions", Boolean.FALSE, configuration.get("useConventions").getValue());
    assertEquals("maxParticipantLength",
                 Integer.valueOf(20), configuration.get("maxParticipantLength").getValue());
    assertEquals("maxHeaderLines",
                 Integer.valueOf(50), configuration.get("maxHeaderLines").getValue());
    assertEquals("participantFormat", "{0} ",
                 configuration.get("participantFormat").getValue());
    assertEquals("metaDataFormat", "{0}={1}",
                 configuration.get("metaDataFormat").getValue());
    assertEquals("timestampFormat", "HH:mm:ss.SSS",
                 configuration.get("timestampFormat").getValue());

    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals(0, defaultParameters.size());

    // no parameters, so configuration required - don't call deserializer.setParameters(defaultParameters);

    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];
      
    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
      
    assertEquals("transcribeme.txt", g.getId());
    assertEquals("time units", Constants.UNIT_SECONDS, g.getOffsetUnits());

    // participants     
    Annotation[] participants = g.all("who"); 
    assertEquals(2, participants.length);
    assertEquals("S1", participants[0].getLabel());
    assertEquals("S2", participants[1].getLabel());

    // turns
    Annotation[] turns = g.all("turn");
    //assertEquals(4, turns.length);
    assertEquals("S1", turns[0].getLabel());
    assertEquals(participants[0], turns[0].getParent());
      
    //assertEquals("S2", turns[1].getLabel());
    //assertEquals(participants[1], turns[1].getParent());

    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals("Four utterances: " + Arrays.asList(utterances),
                 4, utterances.length);
    assertEquals(turns[0], utterances[0].getParent());
    assertEquals(turns[1], utterances[1].getParent());
    assertEquals(turns[2], utterances[2].getParent());
    //TODO assertEquals(turns[3], utterances[3].getParent());
	    
    Annotation[] words = g.all("word");
    assertEquals(9, words.length);
    // System.out.println("" + Arrays.asList(words));
    assertEquals("The", words[0].getLabel());
    assertEquals("quick", words[1].getLabel());
    assertEquals("Brown", words[2].getLabel());
    assertEquals("fox", words[3].getLabel());
    assertEquals("jumps", words[4].getLabel());
    assertEquals("Over", words[5].getLabel());
    assertEquals("the", words[6].getLabel());
    assertEquals("lazy", words[7].getLabel());
    assertEquals("dog.", words[8].getLabel());

    // alignment
    assertEquals(Double.valueOf(1.717), utterances[0].getStart().getOffset());
    assertEquals(Double.valueOf(4.395), utterances[1].getStart().getOffset());
    assertEquals(Double.valueOf(8.692), utterances[2].getStart().getOffset());
    assertEquals(Double.valueOf(75.835), utterances[3].getStart().getOffset());
      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }
	    
  }

  @Test public void descript()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("scribe", "Transcriber", 0, true, true, true),
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("noise", "Noise", 2, true, false, true),
      new Layer("participant_gender", "Gender", 0, false, false, true, "who", true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("pronounce", "Pronounce", 0, true, false, false, "word", true),
      new Layer("lexical", "Lexical", 0, true, false, false, "word", true));
      
    // access file
    NamedStream[] streams = {
      new NamedStream(new File(getDir(), "descript.txt")) }; // transcript
      
    // create deserializer
    PlainTextSerialization deserializer = new PlainTextSerialization();

    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      
    // adjust timestamp format
    configuration.get("timestampFormat").setValue("'['HH:mm:ss']'");
    // also don't use speech conventions
    configuration.get("useConventions").setValue(Boolean.FALSE);
	 
    assertEquals("Configuration parameters" + configuration,
                 11, deserializer.configure(configuration, schema).size());      
    assertEquals("comment", "comment", 
                 ((Layer)configuration.get("commentLayer").getValue()).getId());
    assertEquals("noise", "noise", 
                 ((Layer)configuration.get("noiseLayer").getValue()).getId());
    assertEquals("lexical", "lexical", 
                 ((Layer)configuration.get("lexicalLayer").getValue()).getId());
    assertEquals("pronounce", "pronounce", 
                 ((Layer)configuration.get("pronounceLayer").getValue()).getId());
    assertEquals("use conventions", Boolean.FALSE, configuration.get("useConventions").getValue());
    assertEquals("maxParticipantLength",
                 Integer.valueOf(20), configuration.get("maxParticipantLength").getValue());
    assertEquals("maxHeaderLines",
                 Integer.valueOf(50), configuration.get("maxHeaderLines").getValue());
    assertEquals("participantFormat", "{0}: ",
                 configuration.get("participantFormat").getValue());
    assertEquals("metaDataFormat", "{0}={1}",
                 configuration.get("metaDataFormat").getValue());
    assertEquals("timestampFormat", "'['HH:mm:ss']'",
                 configuration.get("timestampFormat").getValue());

    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    //for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals(1, defaultParameters.size());

    deserializer.setParameters(defaultParameters);

    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];
      
    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
      
    assertEquals("descript.txt", g.getId());
    assertEquals("time units", Constants.UNIT_SECONDS, g.getOffsetUnits());

    // participants     
    Annotation[] participants = g.all("who"); 
    assertEquals(1, participants.length);
    assertEquals("descript", participants[0].getLabel());

    // turns
    Annotation[] turns = g.all("turn");
    assertEquals(1, turns.length);
    assertEquals("descript", turns[0].getLabel());
    assertEquals(participants[0], turns[0].getParent());
      
    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals("Four utterances: " + Arrays.asList(utterances),
                 4, utterances.length);
    assertEquals(turns[0], utterances[0].getParent());
    assertEquals(turns[0], utterances[1].getParent());
    assertEquals(turns[0], utterances[2].getParent());
    assertEquals(turns[0], utterances[3].getParent());
	    
    Annotation[] words = g.all("word");
    // System.out.println("" + Arrays.asList(words));
    String[] expectedTokens = {
      "The", "quick", "brown", "[00:00:01]", "fox", "[00:00:02]",
      "jumps", "[00:00:03]", "over", "the", "[00:00:04]", "lazy",
      "[00:00:05]", "dog.",
      "Aquel", "biógrafo", "se", "zampó", "un", "extraño", "sándwich", "[00:00:06]", "de",
      "vodka", "y", "ajo."
    };
    assertEquals("Right number of words: " + Arrays.asList(words),
                 expectedTokens.length, words.length);
    for (int w = 0; w < expectedTokens.length; w++) {
      assertEquals("word label " + w, expectedTokens[w], words[w].getLabel());
    }

    // alignment
    assertEquals(Double.valueOf(0.000), utterances[0].getStart().getOffset());
    assertEquals(Double.valueOf(2.000), utterances[1].getStart().getOffset());
    assertEquals(Double.valueOf(4.000), utterances[2].getStart().getOffset());
    assertEquals(Double.valueOf(5.000), utterances[3].getStart().getOffset());

    assertEquals(Double.valueOf(2.000), utterances[0].getEnd().getOffset());
    assertEquals(Double.valueOf(4.000), utterances[1].getEnd().getOffset());
    assertEquals(Double.valueOf(5.000), utterances[2].getEnd().getOffset());
    assertEquals(Double.valueOf(7.000), utterances[3].getEnd().getOffset());

    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      if (a.getLayerId().equals("who")) {
        // except participant, which is automatically generated from file name
        assertEquals("Participant label has 'automatic' confidence: " + a.getLayer() + ": " + a,
                     Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC), a.getConfidence());
      } else {
        assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                     Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }
    }
	    
  }

  @Test public void cmsw()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("title", "Title", 0, true, true, true),
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true));
      
    // access file
    NamedStream[] streams = {
      new NamedStream(new File(getDir(), "cmsw-0002.txt")) }; // transcript
      
    // create deserializer
    PlainTextSerialization deserializer = new PlainTextSerialization();

    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      
    // adjust patterns for participant and meta-data
    configuration.get("participantFormat").setValue("Author(s): {0}");
    configuration.get("metaDataFormat").setValue("{0}: {1}");
    // also don't use speech conventions
    configuration.get("useConventions").setValue(Boolean.FALSE);
	 
    assertEquals("Configuration parameters" + configuration,
                 11, deserializer.configure(configuration, schema).size());      
    assertEquals("comment", "comment", 
                 ((Layer)configuration.get("commentLayer").getValue()).getId());
    assertNull("noise", configuration.get("noiseLayer").getValue());
    assertNull("lexical", configuration.get("lexicalLayer").getValue());
    assertNull("pronounce", configuration.get("pronounceLayer").getValue());
    assertEquals("use conventions", Boolean.FALSE, configuration.get("useConventions").getValue());
    assertEquals("maxParticipantLength",
                 Integer.valueOf(20), configuration.get("maxParticipantLength").getValue());
    assertEquals("maxHeaderLines",
                 Integer.valueOf(50), configuration.get("maxHeaderLines").getValue());
    assertEquals("participantFormat", "Author(s): {0}",
                 configuration.get("participantFormat").getValue());
    assertEquals("metaDataFormat", "{0}: {1}",
                 configuration.get("metaDataFormat").getValue());
    assertEquals("timestampFormat",
                 "HH:mm:ss.SSS",
                 configuration.get("timestampFormat").getValue());

    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals(2, defaultParameters.size());
    assertEquals("title", "title", 
                 ((Layer)defaultParameters.get("header_Title").getValue()).getId());
    assertNull("no document number", defaultParameters.get("header_Document ").getValue());
      
    // configure the deserialization
    deserializer.setParameters(defaultParameters);

    // no parameters, so configuration required - don't call deserializer.setParameters(defaultParameters);

    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];
      
    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
      
    assertEquals("ID", "cmsw-0002.txt", g.getId());
    assertEquals("time units", Constants.UNIT_CHARACTERS, g.getOffsetUnits());
    assertEquals("Title meta-data", "An Essay on the Scoto-English Dialect", g.first("title").getLabel());

    // participants     
    Annotation[] authors = g.all("who"); 
    assertEquals(1, authors.length);
    assertEquals("Collin, Zacharias", authors[0].getLabel());

    // turns
    Annotation[] turns = g.all("turn");
    assertEquals(1, turns.length);
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    assertEquals("turn ends at end of recording",
                 Double.valueOf(190946.0), turns[0].getEnd().getOffset());
    assertEquals("Collin, Zacharias", turns[0].getLabel());
    assertEquals(authors[0], turns[0].getParent());
      
    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals(3331, utterances.length);
    // assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
    // assertEquals("utterance ends at end of recording",
    // 		   Double.valueOf(5.2941875), utterances[0].getEnd().getOffset());
    // assertEquals("test", utterances[0].getParent().getLabel());
    for (int l = 0; l < utterances.length; l++) {
      assertEquals("turn for line " + (l+1), turns[0], utterances[l].getParent());
    } // next line
    Annotation[] words = g.all("word");
    //assertEquals(804, words.length);
    String[] checkWords = {
      "gurellough,", "you", "may", "do,", "2nd", "tense,", "D", "2196.",
      "grussyn,", "we", "did,", "3rd", "tense,", "R", "1341.",
      "gruga", "(that)", "I", "did,", "3rd", "tense,", "D", "1434.",
      "When", "the", "present", "participle", "governs", "a", "pronoun,", "it", "is"};
    for (int w = 0; w < checkWords.length; w++) {
      assertEquals("check word " + w + ": " + checkWords[w], checkWords[w], words[w].getLabel());
    } // next word

    Annotation[] comments = g.all("comment");
    assertEquals(1, comments.length);
    assertEquals("Header comment",
                 "Corpus of Modern Scottish Writing (CMSW) - www.scottishcorpus.ac.uk/cmsw/", comments[0].getLabel());

    for (Anchor a : g.getAnchors().values()) {
      if (a.getStartingAnnotations().size() + a.getEndingAnnotations().size() > 0) {
        assertNotNull("ensure all anchors have confidence: " + a + ": " + a.getEndingAnnotations() + "." + a.getStartingAnnotations(),
                      a.getConfidence());
        assertEquals("ensure all anchors have high confidence: " + a, Constants.CONFIDENCE_MANUAL, a.getConfidence().longValue());
      }
    }

    Normalizer normalizer = new Normalizer();
    normalizer.setMinimumTurnPauseLength(0.5); // TODO this should be configurable
    normalizer.transform(g);

    for (Anchor a : g.getAnchors().values()) {
      if (a.getStartingAnnotations().size() + a.getEndingAnnotations().size() > 0) {
        assertNotNull("ensure all anchors have confidence: " + a + ": " + a.getEndingAnnotations() + "." + a.getStartingAnnotations(),
                      a.getConfidence());
        assertEquals("ensure all anchors have high confidence: " + a, Constants.CONFIDENCE_MANUAL, a.getConfidence().longValue());
      }
    }
      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }

    // Validator v = new Validator();
    // v.setFullValidation(true);
    // v.transform(g);

  }

  /*@Test*/ public void speed()  throws Exception {
    Timers timers = new Timers();
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("title", "Title", 0, true, true, true),
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true));
      
    // access file
    NamedStream[] streams = {
      new NamedStream(new File(getDir(), "lockhartpapersco02lockuoft_djvu.txt")) }; // transcript
      
    // create deserializer
    PlainTextSerialization deserializer = new PlainTextSerialization();
    deserializer.setTimers(timers);

    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      
    // adjust patterns for participant and meta-data
    configuration.get("participantFormat").setValue("Author(s): {0}");
    configuration.get("metaDataFormat").setValue("{0}: {1}");
    // also don't use speech conventions, nor timstamps
    configuration.get("useConventions").setValue(Boolean.FALSE);
    configuration.get("timestampFormat").setValue(null);
	 
    assertEquals("Configuration parameters" + configuration, 10, deserializer.configure(configuration, schema).size());      
    assertEquals("comment", "comment", 
                 ((Layer)configuration.get("commentLayer").getValue()).getId());
    assertNull("noise", configuration.get("noiseLayer").getValue());
    assertNull("lexical", configuration.get("lexicalLayer").getValue());
    assertNull("pronounce", configuration.get("pronounceLayer").getValue());
    assertEquals("use conventions", Boolean.FALSE, configuration.get("useConventions").getValue());
    assertEquals("maxParticipantLength",
                 Integer.valueOf(20), configuration.get("maxParticipantLength").getValue());
    assertEquals("maxHeaderLines",
                 Integer.valueOf(50), configuration.get("maxHeaderLines").getValue());
    assertEquals("participantFormat", "Author(s): {0}",
                 configuration.get("participantFormat").getValue());
    assertEquals("metaDataFormat", "{0}: {1}",
                 configuration.get("metaDataFormat").getValue());
    assertNull("timestampFormat",
               configuration.get("timestampFormat").getValue());

    // load the stream
    timers.start("load");
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    timers.end("load");
    // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals(1, defaultParameters.size());
    assertEquals("title", "title", 
                 ((Layer)defaultParameters.get("header_Title").getValue()).getId());
      
    // configure the deserialization
    deserializer.setParameters(defaultParameters);

    // no parameters, so configuration required - don't call deserializer.setParameters(defaultParameters);

    // build the graph
    timers.start("deserialize");
    Graph[] graphs = deserializer.deserialize();
    timers.end("deserialize");
    Graph g = graphs[0];
      
    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
      
    assertEquals("ID", "lockhartpapersco02lockuoft_djvu.txt", g.getId());
    assertEquals("time units", Constants.UNIT_CHARACTERS, g.getOffsetUnits());
    assertEquals("Title meta-data", "The Lockhart papers: containing memoirs and commentaries upon the affairs of Scotland from 1702 to 1715, his secret correspondence with the son of King James the Second from 1718 to 1728, and his other political writings; also, journals and memoirs of the Young Pretender's expedition in 1745", g.first("title").getLabel());

    // participants     
    Annotation[] authors = g.all("who"); 
    assertEquals(1, authors.length);
    assertEquals("Lockhart, George", authors[0].getLabel());

    assertNotNull("Hack to skip validation for texts", g.get("@valid"));

    assertTrue("Deserialization too slow:\n" + deserializer.getTimers().toString(),
               15000 > deserializer.getTimers().getTotals().get("deserialize"));

    timers = new Timers();      
    Validator v = new Validator();
    v.setFullValidation(true);
    timers.start("validation");
    v.transform(g);
    timers.end("validation");

    assertTrue("Validation too slow:\n" + timers.toString(),
               30000 > timers.getTotals().get("deserialize"));

  }

  @Test public void serialize() throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("scribe", "Transcriber")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true)
      .setPeersOverlap(true)
      .setSaturated(true),
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true)
      .setPeersOverlap(true)
      .setSaturated(true),
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
    PlainTextSerialization serializer = new PlainTextSerialization();
      
    // general configuration
    ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals(11, serializer.configure(configuration, schema).size());

    LinkedHashSet<String> needLayers = new LinkedHashSet<String>(
      Arrays.asList(serializer.getRequiredLayers()));
    assertEquals("Needed layers: " + needLayers,
                 8, needLayers.size());
    assertTrue(needLayers.contains("who"));
    assertTrue(needLayers.contains("turn"));
    assertTrue(needLayers.contains("utterance"));
    assertTrue(needLayers.contains("word"));
    assertTrue(needLayers.contains("pronounce"));
    assertTrue(needLayers.contains("lexical"));
    assertTrue(needLayers.contains("comment"));
    assertTrue(needLayers.contains("noise"));
	 
    // serialize
    final Vector<SerializationException> exceptions = new Vector<SerializationException>();
    final Vector<NamedStream> streams = new Vector<NamedStream>();
    String[] layers = {"word","scribe"}; 
    serializer.serialize(Arrays.spliterator(graphs), layers,
                         stream -> streams.add(stream),
                         warning -> System.out.println(warning),
                         exception -> exceptions.add(exception));
    if (exceptions.size() > 0) fail(""+exceptions);

    streams.elementAt(0).save(dir);

    // test using diff
    File result = new File(dir, "serialize_utterance_word.txt");
    String differences = diff(new File(dir, "expected_serialize_utterance_word.txt"), result);
    if (differences != null) {
      fail(differences);
    } else {
      result.delete();
    }
  }

  @Test public void serializeSimultaneousSpeech() throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("comment", "Comment")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true)
      .setPeersOverlap(false)
      .setSaturated(false),
      new Layer("noise", "Noise")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true)
      .setPeersOverlap(false)
      .setSaturated(false),
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true)
      .setPeersOverlap(true)
      .setSaturated(true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("orthography", "Orthography")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false)
      .setPeersOverlap(false)
      .setSaturated(true)
      .setParentId("word"),
      new Layer("lexical", "Lexical")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false)
      .setPeersOverlap(false)
      .setSaturated(true)
      .setParentId("word"),
      new Layer("pronounce", "Pronunciation")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false)
      .setPeersOverlap(false)
      .setSaturated(true)
      .setParentId("word"));
    File dir = getDir();

    // create a graph with simultaneous speech turns

    Graph graph = new Graph()
      .setId("simultaneous_speech.eaf")
      .setSchema(schema);
    graph.addAnchor(new Anchor("a0", 0.0));
    graph.addAnchor(new Anchor("a15", 15.0));
    // participants
    graph.addAnnotation(new Annotation("p1", "p1", "who", "a0", "a15"));
    graph.addAnnotation(new Annotation("p2", "p2", "who", "a0", "a15"));
    // turns
    graph.addAnnotation(new Annotation("t1", "p1", "turn", "a0", "a15", "p1"));
    graph.addAnchor(new Anchor("a5", 5.0));
    graph.addAnchor(new Anchor("a10", 10.0));
    graph.addAnnotation(new Annotation("t2", "p2", "turn", "a5", "a10", "p2"));
    // utterances
    graph.addAnnotation(new Annotation("u1-1", "p1", "utterance", "a0", "a5", "t1"));
    graph.addAnnotation(new Annotation("u1-2", "p1", "utterance", "a5", "a10", "t1"));
    graph.addAnnotation(new Annotation("u2-1", "p2", "utterance", "a5", "a10", "t2"));
    graph.addAnnotation(new Annotation("u1-3", "p1", "utterance", "a10", "a15", "t1"));

    // words
    graph.addAnnotation(new Annotation("w1-1", "w1-1", "word", 
                                       graph.addAnchor(new Anchor("a1", 1.0)).getId(),
                                       graph.addAnchor(new Anchor("a2", 2.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("w1-2", "w1-2", "word", 
                                       "a2",
                                       graph.addAnchor(new Anchor("a3", 3.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("w1-3", "w1-3", "word", 
                                       "a3",
                                       graph.addAnchor(new Anchor("a4", 4.0)).getId(),
                                       "t1"));
      
    graph.addAnnotation(new Annotation("w1-6", "w1-6", "word", 
                                       graph.addAnchor(new Anchor("a6", 6.0)).getId(),
                                       graph.addAnchor(new Anchor("a7", 7.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("w1-7", "w1-7", "word", 
                                       "a7", 
                                       graph.addAnchor(new Anchor("a8", 8.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("w1-8", "w1-8", "word", 
                                       "a8",
                                       graph.addAnchor(new Anchor("a9", 9.0)).getId(),
                                       "t1"));
      

    graph.addAnnotation(new Annotation("w1-11", "w1-11", "word", 
                                       graph.addAnchor(new Anchor("a11", 11.0)).getId(),
                                       graph.addAnchor(new Anchor("a12", 12.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("w1-12", "w1-12", "word", 
                                       "a12",
                                       graph.addAnchor(new Anchor("a13", 13.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("w1-13", "w1-13", "word", 
                                       "a13",
                                       graph.addAnchor(new Anchor("a14", 14.0)).getId(),
                                       "t1"));
      
    graph.addAnnotation(new Annotation("w2-6.5", "w2-6.5", "word", 
                                       graph.addAnchor(new Anchor("a6.5", 6.5)).getId(),
                                       graph.addAnchor(new Anchor("a7.5", 7.5)).getId(),
                                       "t2"));
    graph.addAnnotation(new Annotation("w2-7.5", "w2-7.5", "word", 
                                       "a7.5",
                                       graph.addAnchor(new Anchor("a8.5", 8.5)).getId(),
                                       "t2"));

    // add orthography tags that should not be used because orthography is not selected
    for (Annotation word : graph.all("word"))
    {
      graph.createTag(word, "orthography", word.getLabel()+"-orthography");
    }
      
    // add some comments, noises, lexical and pronounce tags
    graph.addAnnotation(new Annotation("comment1", "some preamble", "comment", "a0", "a1"));
    graph.addAnnotation(new Annotation("noise1", "throat clear", "noise", "a4", "a5"));
    graph.addAnnotation(new Annotation("lex1", "lex-1-6", "lexical", "a6", "a7", "w1-6"));
    graph.addAnnotation(new Annotation("lex2", "lex-1-7", "lexical", "a7", "a8", "w1-7"));
    graph.addAnnotation(new Annotation("pron1", "pron-1-7", "pronounce", "a7", "a8", "w1-7"));
    graph.addAnnotation(new Annotation("pron2", "pron-1-8", "pronounce", "a8", "a9", "w1-8"));

    // create serializer
    PlainTextSerialization serializer = new PlainTextSerialization();
      
    // general configuration
    ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals(11, serializer.configure(configuration, schema).size());

    LinkedHashSet<String> needLayers = new LinkedHashSet<String>(
      Arrays.asList(serializer.getRequiredLayers()));
    assertEquals("Needed layers: " + needLayers,
                 8, needLayers.size());
    assertTrue(needLayers.contains("who"));
    assertTrue(needLayers.contains("turn"));
    assertTrue(needLayers.contains("utterance"));
    assertTrue(needLayers.contains("word"));
    assertTrue(needLayers.contains("pronounce"));
    assertTrue(needLayers.contains("lexical"));
    assertTrue(needLayers.contains("comment"));
    assertTrue(needLayers.contains("noise"));
	 
    // serialize
    final Vector<SerializationException> exceptions = new Vector<SerializationException>();
    final Vector<NamedStream> streams = new Vector<NamedStream>();
    String[] layers = {"word","lexical","pronounce"}; 
    serializer.serialize(Utility.OneGraphSpliterator(graph), layers,
                         stream -> streams.add(stream),
                         warning -> System.out.println(warning),
                         exception -> exceptions.add(exception));
    if (exceptions.size() > 0) fail(""+exceptions);

    streams.elementAt(0).save(dir);

    // test using diff
    File result = new File(dir, "simultaneous_speech.txt"); // ensure name strips extension
    String differences = diff(new File(dir, "expected_simultaneous_speech.txt"), result);
    if (differences != null) {
      fail(differences);
    } else {
      result.delete();
    }
  }

  @Test public void serializeWithTags() throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true)
      .setPeersOverlap(true)
      .setSaturated(true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("orthography", "Orthography")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false)
      .setPeersOverlap(false)
      .setSaturated(true)
      .setParentId("word"),
      new Layer("pos", "Part of Speech")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false)
      .setPeersOverlap(false)
      .setSaturated(true)
      .setParentId("word"),
      new Layer("lexical", "Lexical")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false)
      .setPeersOverlap(false)
      .setSaturated(true)
      .setParentId("word"),
      new Layer("pronounce", "Pronunciation")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false)
      .setPeersOverlap(false)
      .setSaturated(true)
      .setParentId("word"));
    File dir = getDir();
      
    // create a graph with simultaneous speech turns

    Graph graph = new Graph()
      .setId("tagged_sentence")
      .setSchema(schema);
    graph.addAnchor(new Anchor("a0", 0.0));
    graph.addAnchor(new Anchor("a5", 5.0));
    graph.addAnchor(new Anchor("a10", 10.0));
    // participants
    graph.addAnnotation(new Annotation("author", "author", "who", "a0", "a10"));
    // turns
    graph.addAnnotation(new Annotation("t1", "author", "turn", "a0", "a10", "author"));
    // utterances
    graph.addAnnotation(new Annotation("u1", "author", "utterance", "a0", "a5", "t1"));
    graph.addAnnotation(new Annotation("u2", "author", "utterance", "a5", "a10", "t1"));

    // words
    graph.addAnnotation(new Annotation("the", "The", "word",
                                       "a0",
                                       graph.addAnchor(new Anchor("a1", 1.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("quick", "'quick", "word", 
                                       "a1",
                                       graph.addAnchor(new Anchor("a2", 2.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("brown", "brown'", "word", 
                                       "a2",
                                       graph.addAnchor(new Anchor("a3", 3.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("fox", "fox", "word", 
                                       "a3",
                                       graph.addAnchor(new Anchor("a4", 4.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("jumps", "jumps -", "word", 
                                       "a4",
                                       "a5",
                                       "t1"));
      
    graph.addAnnotation(new Annotation("over", "over", "word",
                                       "a5",
                                       graph.addAnchor(new Anchor("a6", 6.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("the2", "the", "word", 
                                       "a6",
                                       graph.addAnchor(new Anchor("a7", 7.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("lazy", "lazy", "word", 
                                       "a7",
                                       graph.addAnchor(new Anchor("a8", 8.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("dog", "\"dog\"", "word", 
                                       "a8",
                                       graph.addAnchor(new Anchor("a9", 9.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation(".", ".", "word", 
                                       "a9",
                                       "a10",
                                       "t1"));
    graph.createTag(graph.getAnnotation("the"), "orthography", "the");
    graph.createTag(graph.getAnnotation("quick"), "orthography", "quick");
    graph.createTag(graph.getAnnotation("brown"), "orthography", "brown");
    graph.createTag(graph.getAnnotation("fox"), "orthography", "fox");
    graph.createTag(graph.getAnnotation("jumps"), "orthography", "jumps");
    graph.createTag(graph.getAnnotation("over"), "orthography", "over");
    graph.createTag(graph.getAnnotation("the2"), "orthography", "the");
    graph.createTag(graph.getAnnotation("lazy"), "orthography", "lazy");
    graph.createTag(graph.getAnnotation("dog"), "orthography", "dog");

    graph.createTag(graph.getAnnotation("the"), "pos", "DET");
    graph.createTag(graph.getAnnotation("quick"), "pos", "ADJ");
    graph.createTag(graph.getAnnotation("brown"), "pos", "ADJ");
    graph.createTag(graph.getAnnotation("fox"), "pos", "N");
    graph.createTag(graph.getAnnotation("jumps"), "pos", "V");
    graph.createTag(graph.getAnnotation("over"), "pos", "PREP");
    graph.createTag(graph.getAnnotation("the2"), "pos", "DET");
    graph.createTag(graph.getAnnotation("lazy"), "pos", "ADJ");
    graph.createTag(graph.getAnnotation("dog"), "pos", "N");
    graph.createTag(graph.getAnnotation("."), "pos", "PUNC");

    // add some comments, noises, lexical and pronounce tags
    graph.addAnnotation(new Annotation("comment1", "some preamble", "comment", "a0", "a1"));
    graph.addAnnotation(new Annotation("noise1", "throat clear", "noise", "a4", "a5"));
    graph.addAnnotation(new Annotation("lex1", "lex-1-6", "lexical", "a6", "a7", "w1-6"));
    graph.addAnnotation(new Annotation("lex2", "lex-1-7", "lexical", "a7", "a8", "w1-7"));
    graph.addAnnotation(new Annotation("pron1", "pron-1-7", "pronounce", "a7", "a8", "w1-7"));
    graph.addAnnotation(new Annotation("pron2", "pron-1-8", "pronounce", "a8", "a9", "w1-8"));

    // create serializer
    PlainTextSerialization serializer = new PlainTextSerialization();
      
    // general configuration
    ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    // don't use conventions
    configuration.get("useConventions").setValue(Boolean.FALSE);
    assertEquals(11, serializer.configure(configuration, schema).size());
    assertEquals("orthography", "orthography", 
                 ((Layer)configuration.get("orthographyLayer").getValue()).getId());
    assertEquals("use conventions", Boolean.FALSE, configuration.get("useConventions").getValue());
    LinkedHashSet<String> needLayers = new LinkedHashSet<String>(
      Arrays.asList(serializer.getRequiredLayers()));
    assertEquals("Needed layers doesn't include convention layers: " + needLayers,
                 4, needLayers.size());
    assertTrue(needLayers.contains("who"));
    assertTrue(needLayers.contains("turn"));
    assertTrue(needLayers.contains("utterance"));
    assertTrue(needLayers.contains("word"));
	 
	 
    // serialize
    final Vector<SerializationException> exceptions = new Vector<SerializationException>();
    final Vector<NamedStream> streams = new Vector<NamedStream>();
    String[] layers = {"word", "pos", "orthography"}; 
    serializer.serialize(Utility.OneGraphSpliterator(graph), layers,
                         stream -> streams.add(stream),
                         warning -> System.out.println(warning),
                         exception -> exceptions.add(exception));
    if (exceptions.size() > 0) fail(""+exceptions);

    streams.elementAt(0).save(dir);

    // test using diff
    File result = new File(dir, graph.getId() + ".txt");
    String differences = diff(new File(dir, "expected_" + graph.getId() + ".txt"), result);
    if (differences != null) {
      fail(differences);
    } else {
      result.delete();
    }
  }

  @Test public void blank()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("comment", "Comment")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterances")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true));
      
    // access file
    NamedStream[] streams = {
      new NamedStream(new File(getDir(), "blank.txt")), // transcript
      new NamedStream(new File(getDir(), "elicited.wav")) // media file gives max anchor offset
    };
      
    // create deserializer
    PlainTextSerialization deserializer = new PlainTextSerialization();
      
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals("Configuration parameters" + configuration,
                 11, deserializer.configure(configuration, schema).size());      
    assertEquals("comment", "comment", 
                 ((Layer)configuration.get("commentLayer").getValue()).getId());
    assertNull("noise", configuration.get("noiseLayer").getValue());
    assertNull("lexical", configuration.get("lexicalLayer").getValue());
    assertNull("pronounce", configuration.get("pronounceLayer").getValue());
    assertEquals("use conventions", Boolean.TRUE, configuration.get("useConventions").getValue());
    assertEquals("maxParticipantLength",
                 Integer.valueOf(20), configuration.get("maxParticipantLength").getValue());
    assertEquals("maxHeaderLines",
                 Integer.valueOf(50), configuration.get("maxHeaderLines").getValue());
    assertEquals("participantFormat", "{0}: ",
                 configuration.get("participantFormat").getValue());
    assertEquals("metaDataFormat", "{0}={1}",
                 configuration.get("metaDataFormat").getValue());
    assertEquals("timestampFormat",
                 "HH:mm:ss.SSS",
                 configuration.get("timestampFormat").getValue());
      
    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals(0, defaultParameters.size());
      
    // configure the deserialization
    deserializer.setParameters(defaultParameters);
      
    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];
      
    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
      
    assertEquals("blank.txt", g.getId());
    assertEquals("time units", Constants.UNIT_SECONDS, g.getOffsetUnits());

    // participants     
    Annotation[] participants = g.all("who"); 
    assertEquals("One participant", 1, participants.length);
    assertEquals("blank", participants[0].getLabel());
      
    // one empty turn
    Annotation[] turns = g.all("turn");
    assertEquals("one turn", 1, turns.length);
    assertEquals("turn is child of participant", participants[0], turns[0].getParent());

    // one empty utterance
    Annotation[] utterances = g.all("utterance");
    assertEquals("one utterance", 0, utterances.length);

    // no words
    Annotation[] words = g.all("word");
    assertEquals("no words" + Arrays.asList(words), 0, words.length);

    // start/end anchors
    assertNotEquals("There are two anchors: " + g.getAnchors(),
                    2, g.getAnchors().size());
    Anchor start = g.getStart();
    Anchor end = g.getEnd();
    assertNotEquals("Start and end are distinct: " + start,
                    start, end);
    assertEquals("Start at zero: " + start,
                 Double.valueOf(0.0), start.getOffset());
    assertEquals("Start at length of recording: " + end,
                 Double.valueOf(5.2941875), end.getOffset());
    assertEquals("Participant annotation start",
                 start, participants[0].getStart());
    assertEquals("Participant annotation end",
                 end, participants[0].getEnd());
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
    org.junit.runner.JUnitCore.main("nzilbb.formatter.text.TestPlainTextSerialization");
  }
}
