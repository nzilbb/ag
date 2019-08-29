//
// Copyright 2017 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.text.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.io.File;
import java.net.URL;
import nzilbb.configure.ParameterSet;
import nzilbb.configure.Parameter;
import nzilbb.ag.*;
import nzilbb.ag.util.Validator;
import nzilbb.ag.util.Normalizer;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.util.Timers;
import nzilbb.text.*;

public class TestPlainTextDeserializer
{
   @Test public void comment() 
      throws Exception
   {
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
      PlainTextDeserializer deserializer = new PlainTextDeserializer();

      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals("Configuration parameters" + configuration, 10,
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

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("comment.txt", g.getId());
      assertEquals("text units", Constants.UNIT_CHARACTERS, g.getOffsetUnits());

      // meta data
      assertEquals("graph meta data", 
		   "exmormon", g.my("subreddit").getLabel());
      assertEquals("graph meta data", 
		   "https://www.reddit.com/r/exmormon/comments/2qyr1a#cnas8zv", g.my("url").getLabel());
      assertEquals("graph meta data", 
		   "2015-01-01T00:00:00.000Z", g.my("air_date").getLabel());
      assertEquals("graph meta data", 
		   "2015-02-28T11:51:22.000Z", g.my("transcript_version_date").getLabel());

      // participants     
      Annotation[] authors = g.list("who"); 
      assertEquals(1, authors.length);
      assertEquals("YoungModern", authors[0].getLabel());
      assertEquals("?", authors[0].my("participant_gender").getLabel());

      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(1, turns.length);
      assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
      //assertEquals(Double.valueOf(23.563), turns[0].getEnd().getOffset()); // TODO
      assertEquals("YoungModern", turns[0].getLabel());
      assertEquals(g.my("who"), turns[0].getParent());

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(1, utterances.length);
      assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
      assertEquals("YoungModern", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());
      
      Annotation[] words = g.list("word");
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

      assertEquals(0, g.list("entities").length);
      assertEquals(0, g.list("language").length);
      assertEquals(0, g.list("lexical").length);

   }

   @Test public void comment2() 
      throws Exception
   {
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
      PlainTextDeserializer deserializer = new PlainTextDeserializer();

      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals("Configuration parameters" + configuration, 10, deserializer.configure(configuration, schema).size());      
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

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("comment2.txt", g.getId());
      assertEquals("text units", Constants.UNIT_CHARACTERS, g.getOffsetUnits());

      // meta data
      assertEquals("graph meta data", 
		   "StrangerThings", g.my("subreddit").getLabel());
      assertEquals("graph meta data", 
		   "https://www.reddit.com/r/StrangerThings/comments/5rxk10#ddb3r2n", g.my("url").getLabel());
      assertEquals("graph meta data", 
		   "2017-02-04T02:47:49.000Z", g.my("publication_time").getLabel());
      assertEquals("graph meta data", 
		   "t3_5rxk10", g.my("parent_id").getLabel());

      // participants     
      Annotation[] authors = g.list("who"); 
      assertEquals(1, authors.length);
      assertEquals("tgflp", authors[0].getLabel());
      assertNotNull("start ID set", authors[0].getStartId());
      assertEquals(Double.valueOf(0.0), authors[0].getStart().getOffset());

      // tag participant as main one
      g.addLayer(schema.getLayer("main_participant"));
      g.createTag(authors[0], "main_participant", authors[0].getLabel());
      
      // turns
      Annotation[] turns = g.list("turn");
      //assertEquals(1, turns.length);
      assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
      //assertEquals(Double.valueOf(23.563), turns[0].getEnd().getOffset()); // TODO
      assertEquals("tgflp", turns[0].getLabel());
      assertEquals(g.my("who"), turns[0].getParent());

      // utterances
      Annotation[] utterances = g.list("utterance");
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
      
      Annotation[] words = g.list("word");
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

      assertEquals(0, g.list("entities").length);
      assertEquals(0, g.list("language").length);
      assertEquals(0, g.list("lexical").length);

      // test validator runs
      Normalizer normalizer = new Normalizer();
      normalizer.transform(g);
      g.commit();
      g.create();
      Validator v = new Validator();
      v.transform(g);

   }

   @Test public void elicited() 
      throws Exception
   {
      Schema schema = new Schema("who", "turn", "utterance", "word",
	 new Layer("app", "App", 0, true, true, true),
	 new Layer("appVersion", "Version", 0, false, false, true),
	 new Layer("creation_date", "Version Date", 0, false, false, true),
	 new Layer("speech_migraine", "Migraine", 0, false, false, true),
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
      PlainTextDeserializer deserializer = new PlainTextDeserializer();

      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals("Configuration parameters" + configuration, 10, deserializer.configure(configuration, schema).size());      
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

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("elicited.txt", g.getId());
      assertEquals("time units", Constants.UNIT_SECONDS, g.getOffsetUnits());

      // meta data
      assertEquals("graph meta data", 
		   "Elicit Speech", g.my("app").getLabel());
      assertEquals("graph meta data", 
		   "1.0.0", g.my("appVersion").getLabel());
      assertEquals("graph meta data", 
		   "2017-01-13T15:49:55.575Z", g.my("creation_date").getLabel());
      String[] multilineAttribute = g.labels("speech_migraine");
      assertEquals("graph meta data - multiline", 
		   2, multilineAttribute.length);
      assertEquals("graph meta data - multiline", 
		   "currently have migraine", multilineAttribute[0]);
      assertEquals("graph meta data - multiline", 
		   "second value", multilineAttribute[1]);

      // participants     
      Annotation[] authors = g.list("who"); 
      assertEquals(1, authors.length);
      assertEquals("test", authors[0].getLabel());

      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(1, turns.length);
      assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
      assertEquals("turn ends at end of recording",
		   Double.valueOf(5.2941875), turns[0].getEnd().getOffset());
      assertEquals("test", turns[0].getLabel());
      assertEquals(g.my("who"), turns[0].getParent());

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(1, utterances.length);
      assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
      assertEquals("utterance ends at end of recording",
		   Double.valueOf(5.2941875), utterances[0].getEnd().getOffset());
      assertEquals("test", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());
      
      Annotation[] words = g.list("word");
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

      Annotation[] comments = g.list("comment");
      assertEquals(1, comments.length);
      assertEquals("Please read the following aloud:", comments[0].getLabel());
      assertEquals(Double.valueOf(0), comments[0].getStart().getOffset());

      assertNull("Hack to skip validation for texts isn't activated for transcripts", g.get("@valid"));
   }

   @Test public void invalidAudio() 
      throws Exception
   {
      Schema schema = new Schema("who", "turn", "utterance", "word",
	 new Layer("app", "App", 0, true, true, true),
	 new Layer("appVersion", "Version", 0, false, false, true),
	 new Layer("creation_date", "Version Date", 0, false, false, true),
	 new Layer("speech_migraine", "Migraine", 0, false, false, true),
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
      PlainTextDeserializer deserializer = new PlainTextDeserializer();

      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals("Configuration parameters" + configuration, 10, deserializer.configure(configuration, schema).size());      
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

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("invalid-audio.txt", g.getId());
      assertEquals("time units characters because media is invalid",
		   Constants.UNIT_CHARACTERS, g.getOffsetUnits());

      // meta data
      assertEquals("graph meta data", 
		   "Elicit Speech", g.my("app").getLabel());
      assertEquals("graph meta data", 
		   "1.0.0", g.my("appVersion").getLabel());
      assertEquals("graph meta data", 
		   "2017-01-13T15:49:55.575Z", g.my("creation_date").getLabel());
      String[] multilineAttribute = g.labels("speech_migraine");
      assertEquals("graph meta data - multiline", 
		   2, multilineAttribute.length);
      assertEquals("graph meta data - multiline", 
		   "currently have migraine", multilineAttribute[0]);
      assertEquals("graph meta data - multiline", 
		   "second value", multilineAttribute[1]);

      // participants     
      Annotation[] authors = g.list("who"); 
      assertEquals(1, authors.length);
      assertEquals("test", authors[0].getLabel());

      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(1, turns.length);
      assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
      assertEquals("turn ends at last character",
		   Double.valueOf(100), turns[0].getEnd().getOffset());
      assertEquals("test", turns[0].getLabel());
      assertEquals(g.my("who"), turns[0].getParent());

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(1, utterances.length);
      assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
      assertEquals("utterance ends at last character",
		   Double.valueOf(100), utterances[0].getEnd().getOffset());
      assertEquals("test", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());
      
      Annotation[] words = g.list("word");
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

      Annotation[] comments = g.list("comment");
      assertEquals(1, comments.length);
      assertEquals("Please read the following aloud:", comments[0].getLabel());
      assertEquals(Double.valueOf(0), comments[0].getStart().getOffset());

   }

   @Test public void interview() 
      throws Exception
   {
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
      PlainTextDeserializer deserializer = new PlainTextDeserializer();

      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals("Configuration parameters" + configuration, 10, deserializer.configure(configuration, schema).size());      
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
      
      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("interview.txt", g.getId());
      assertEquals("time units", Constants.UNIT_CHARACTERS, g.getOffsetUnits());


      // participants     
      Annotation[] authors = g.list("who"); 
      assertEquals(2, authors.length);
      assertEquals("BOM is stripped from speaker name",
		   "mop03-2b", authors[0].getLabel());
      assertEquals("Interviewer", authors[1].getLabel());

      // turns
      Annotation[] turns = g.list("turn");
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
      Annotation[] utterances = g.list("utterance");
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
	    
      Annotation[] words = g.list("word");
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

      Annotation[] comments = g.list("comment");
      assertEquals(1, comments.length);
      assertEquals("unclear", comments[0].getLabel());

      Annotation[] noises = g.list("noise");
      assertEquals(1, noises.length);
      assertEquals("click", noises[0].getLabel());

   }

   @Test public void transcribeme() 
      throws Exception
   {
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
      PlainTextDeserializer deserializer = new PlainTextDeserializer();

      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      
      // adjust patterns for participant and meta-data
      configuration.get("participantFormat").setValue("{0} ");
      // also don't use speech conventions
      configuration.get("useConventions").setValue(Boolean.FALSE);
	 
      assertEquals("Configuration parameters" + configuration, 10, deserializer.configure(configuration, schema).size());      
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
      
      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("transcribeme.txt", g.getId());
      assertEquals("time units", Constants.UNIT_SECONDS, g.getOffsetUnits());


      // participants     
      Annotation[] participants = g.list("who"); 
      assertEquals(2, participants.length);
      assertEquals("S1", participants[0].getLabel());
      assertEquals("S2", participants[1].getLabel());

      // turns
      Annotation[] turns = g.list("turn");
      //assertEquals(4, turns.length);
      assertEquals("S1", turns[0].getLabel());
      assertEquals(participants[0], turns[0].getParent());
      
      //assertEquals("S2", turns[1].getLabel());
      //assertEquals(participants[1], turns[1].getParent());

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(4, utterances.length);
      assertEquals(turns[0], utterances[0].getParent());
      assertEquals(turns[1], utterances[1].getParent());
      assertEquals(turns[2], utterances[2].getParent());
      assertEquals(turns[3], utterances[3].getParent());
	    
      Annotation[] words = g.list("word");
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
	    
   }

   @Test public void cmsw() 
      throws Exception
   {
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
      PlainTextDeserializer deserializer = new PlainTextDeserializer();

      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      
      // adjust patterns for participant and meta-data
      configuration.get("participantFormat").setValue("Author(s): {0}");
      configuration.get("metaDataFormat").setValue("{0}: {1}");
      // also don't use speech conventions
      configuration.get("useConventions").setValue(Boolean.FALSE);
	 
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
      
      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("ID", "cmsw-0002.txt", g.getId());
      assertEquals("time units", Constants.UNIT_CHARACTERS, g.getOffsetUnits());
      assertEquals("Title meta-data", "An Essay on the Scoto-English Dialect", g.my("title").getLabel());

      // participants     
      Annotation[] authors = g.list("who"); 
      assertEquals(1, authors.length);
      assertEquals("Collin, Zacharias", authors[0].getLabel());

      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(1, turns.length);
      assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
      assertEquals("turn ends at end of recording",
      		   Double.valueOf(190946.0), turns[0].getEnd().getOffset());
      assertEquals("Collin, Zacharias", turns[0].getLabel());
      assertEquals(authors[0], turns[0].getParent());
      
      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(3331, utterances.length);
      // assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
      // assertEquals("utterance ends at end of recording",
      // 		   Double.valueOf(5.2941875), utterances[0].getEnd().getOffset());
      // assertEquals("test", utterances[0].getParent().getLabel());
      for (int l = 0; l < utterances.length; l++)
      {
	 assertEquals("turn for line " + (l+1), turns[0], utterances[l].getParent());
      } // next line
      Annotation[] words = g.list("word");
      //assertEquals(804, words.length);
      String[] checkWords = {
	 "gurellough,", "you", "may", "do,", "2nd", "tense,", "D", "2196.",
	 "grussyn,", "we", "did,", "3rd", "tense,", "R", "1341.",
	 "gruga", "(that)", "I", "did,", "3rd", "tense,", "D", "1434.",
	 "When", "the", "present", "participle", "governs", "a", "pronoun,", "it", "is"};
      for (int w = 0; w < checkWords.length; w++)
      {
	 assertEquals("check word " + w + ": " + checkWords[w], checkWords[w], words[w].getLabel());
      } // next word

      Annotation[] comments = g.list("comment");
      assertEquals(1, comments.length);
      assertEquals("Header comment",
		   "Corpus of Modern Scottish Writing (CMSW) - www.scottishcorpus.ac.uk/cmsw/", comments[0].getLabel());

      for (Anchor a : g.getAnchors().values())
      {
	 if (a.getStartingAnnotations().size() + a.getEndingAnnotations().size() > 0)
	 {
	    assertNotNull("ensure all anchors have confidence: " + a + ": " + a.getEndingAnnotations() + "." + a.getStartingAnnotations(),
			  a.getConfidence());
	    assertEquals("ensure all anchors have high confidence: " + a, Constants.CONFIDENCE_MANUAL, a.getConfidence().longValue());
	 }
      }

      Normalizer normalizer = new Normalizer();
      normalizer.setMinimumTurnPauseLength(0.5); // TODO this should be configurable
      normalizer.transform(g);

      for (Anchor a : g.getAnchors().values())
      {
	 if (a.getStartingAnnotations().size() + a.getEndingAnnotations().size() > 0)
	 {
	    assertNotNull("ensure all anchors have confidence: " + a + ": " + a.getEndingAnnotations() + "." + a.getStartingAnnotations(),
			  a.getConfidence());
	    assertEquals("ensure all anchors have high confidence: " + a, Constants.CONFIDENCE_MANUAL, a.getConfidence().longValue());
	 }
      }

      // Validator v = new Validator();
      // v.setFullValidation(true);
      // v.transform(g);

   }

   /*@Test*/ public void speed() 
      throws Exception
   {
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
      PlainTextDeserializer deserializer = new PlainTextDeserializer();
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
      
      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("ID", "lockhartpapersco02lockuoft_djvu.txt", g.getId());
      assertEquals("time units", Constants.UNIT_CHARACTERS, g.getOffsetUnits());
      assertEquals("Title meta-data", "The Lockhart papers: containing memoirs and commentaries upon the affairs of Scotland from 1702 to 1715, his secret correspondence with the son of King James the Second from 1718 to 1728, and his other political writings; also, journals and memoirs of the Young Pretender's expedition in 1745", g.my("title").getLabel());

      // participants     
      Annotation[] authors = g.list("who"); 
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
   public File getDir() 
   { 
      if (fDir == null)
      {
	 try
	 {
	    URL urlThisClass = getClass().getResource(getClass().getSimpleName() + ".class");
	    File fThisClass = new File(urlThisClass.toURI());
	    fDir = fThisClass.getParentFile();
	 }
	 catch(Throwable t)
	 {
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


   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.text.test.TestPlainTextDeserializer");
   }
}
