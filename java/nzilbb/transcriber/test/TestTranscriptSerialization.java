//
// Copyright 2016-2019 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.transcriber.test;
	      
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
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.editpath.EditStep;
import nzilbb.editpath.MinimumEditPath;
import nzilbb.transcriber.*;

// TODO add a test for turns with no speaker assigned.
public class TestTranscriptSerialization
{
   @Test public void basicConversion() 
      throws Exception
   {
      Schema schema = new Schema("who", "turn", "utterance", "word",
	 new Layer("transcriber", "Transcribers", 0, true, true, true),
	 new Layer("transcript_language", "Graph language", 0, false, false, true),
	 new Layer("transcript_scribe", "Transcriber", 0, false, false, true),
	 new Layer("transcript_program", "Program", 0, false, false, true),
	 new Layer("transcript_airdate", "Air Date", 0, false, false, true),
	 new Layer("transcript_version", "Version", 0, false, false, true),
	 new Layer("transcript_version_date", "Version Date", 0, false, false, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("participant_check", "Check", 0, false, false, true, "who", true),
	 new Layer("gender", "Gender", 0, false, false, true, "who", true),
	 new Layer("participant_dialect", "Dialect", 0, false, false, true, "who", true),
	 new Layer("participant_accent", "Accent", 0, false, false, true, "who", true),
	 new Layer("participant_scope", "Scope", 0, false, false, true, "who", true),
	 new Layer("topic", "Topic", 2, true, false, false),
	 new Layer("comment", "Comment", 2, true, false, true),
	 new Layer("noise", "Noise", 2, true, false, true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("entities", "Entities", 2, true, false, false, "turn", true),
	 new Layer("language", "Language", 2, true, false, false, "turn", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true),
	 new Layer("lexical", "Lexical", 0, true, false, false, "word", true),
	 new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
      schema.getLayer("gender").getValidLabels().put("M","Male");
      schema.getLayer("gender").getValidLabels().put("F","Female");

      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "test.trs")) };
      
      // create deserializer
      TranscriptSerialization deserializer = new TranscriptSerialization();

      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals("Configuration parameters" + configuration, 18, deserializer.configure(configuration, schema).size());      
      assertEquals("topic", "topic", 
		   ((Layer)configuration.get("topicLayer").getValue()).getId());
      assertEquals("comment", "comment", 
		   ((Layer)configuration.get("commentLayer").getValue()).getId());
      assertEquals("noise", "noise", 
		   ((Layer)configuration.get("noiseLayer").getValue()).getId());
      assertEquals("language", "language", 
		   ((Layer)configuration.get("languageLayer").getValue()).getId());
      assertEquals("lexical", "lexical", 
		   ((Layer)configuration.get("lexicalLayer").getValue()).getId());
      assertEquals("pronounce", "pronounce", 
		   ((Layer)configuration.get("pronounceLayer").getValue()).getId());
      assertEquals("entities", "entities", 
		   ((Layer)configuration.get("entityLayer").getValue()).getId());
      assertEquals("transcript_scribe", "transcript_scribe", 
		   ((Layer)configuration.get("scribeLayer").getValue()).getId());
      assertEquals("transcript_version", "transcript_version", 
		   ((Layer)configuration.get("versionLayer").getValue()).getId());
      assertEquals("transcript_version_date", "transcript_version_date", 
		   ((Layer)configuration.get("versionDateLayer").getValue()).getId());
      assertEquals("transcript_program", "transcript_program", 
		   ((Layer)configuration.get("programLayer").getValue()).getId());
      assertEquals("transcript_airdate", "transcript_airdate", 
		   ((Layer)configuration.get("airDateLayer").getValue()).getId());
      assertEquals("transcript_language", "transcript_language", 
		   ((Layer)configuration.get("transcriptLanguageLayer").getValue()).getId());
      assertEquals("participant_check", "participant_check", 
		   ((Layer)configuration.get("participantCheckLayer").getValue()).getId());
      assertEquals("gender", "gender", 
		   ((Layer)configuration.get("genderLayer").getValue()).getId());
      assertEquals("participant_dialect", "participant_dialect", 
		   ((Layer)configuration.get("dialectLayer").getValue()).getId());
      assertEquals("participant_accent", "participant_accent", 
		   ((Layer)configuration.get("accentLayer").getValue()).getId());
      assertEquals("participant_scope", "participant_scope", 
		   ((Layer)configuration.get("scopeLayer").getValue()).getId());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      //for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(0, defaultParamaters.size());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("test.trs", g.getId());
      String[] transcribers = g.labels("transcript_scribe"); 
      assertEquals(1, transcribers.length);
      assertEquals("Robert Fromont", transcribers[0]);
      String[] languages = g.labels("transcript_language"); 
      assertEquals(1, languages.length);
      assertEquals("en", languages[0]);

      assertEquals("7", g.my("transcript_version").getLabel());
      assertEquals("130825", g.my("transcript_version_date").getLabel());

      // participants     
      assertEquals(2, g.list("who").length);
      assertEquals("Interviewer", g.getAnnotation("spk1").getLabel());
      assertEquals("who", g.getAnnotation("spk1").getLayerId());
      assertEquals("mop03-2b", g.getAnnotation("spk2").getLabel());
      assertEquals("who", g.getAnnotation("spk2").getLayerId());

      // participant meta data
      assertEquals("Gender label is normalized",
		   "F", g.getAnnotation("spk1").my("gender").getLabel());
      assertEquals("Gender label is normalized",
		   "M", g.getAnnotation("spk2").my("gender").getLabel());
      assertEquals("no", g.getAnnotation("spk1").my("participant_check").getLabel());
      assertEquals("yes", g.getAnnotation("spk2").my("participant_check").getLabel());
      assertEquals("native", g.getAnnotation("spk1").my("participant_dialect").getLabel());
      assertEquals("native", g.getAnnotation("spk2").my("participant_dialect").getLabel());
      assertEquals("ame", g.getAnnotation("spk1").my("participant_accent").getLabel());
      assertEquals("nze", g.getAnnotation("spk2").my("participant_accent").getLabel());
      assertEquals("local", g.getAnnotation("spk1").my("participant_scope").getLabel());
      assertEquals("local", g.getAnnotation("spk2").my("participant_scope").getLabel());


      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(26, turns.length);
      assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
      assertEquals(Double.valueOf(23.563), turns[0].getEnd().getOffset());
      assertEquals("mop03-2b", turns[0].getLabel());
      assertEquals(g.getAnnotation("spk2"), turns[0].getParent());
      assertEquals(Double.valueOf(302.834), turns[24].getStart().getOffset());
      assertEquals(Double.valueOf(304.334), turns[24].getEnd().getOffset());
      assertEquals("Interviewer", turns[24].getLabel());
      assertEquals(g.getAnnotation("spk1"), turns[24].getParent());

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(140, utterances.length);
      assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
      assertEquals(Double.valueOf(5.75), utterances[0].getEnd().getOffset());
      assertEquals("mop03-2b", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals(Double.valueOf(5.75), utterances[1].getStart().getOffset());
      assertEquals(Double.valueOf(6.907), utterances[1].getEnd().getOffset());
      assertEquals("mop03-2b", utterances[1].getParent().getLabel());

      
      Annotation[] words = g.list("word");
      assertEquals(Double.valueOf(0), words[0].getStart().getOffset());
      // System.out.println("" + Arrays.asList(Arrays.copyOfRange(words, 0, 10)));
      assertEquals("and", words[0].getLabel());
      assertEquals("ah .", words[1].getLabel());
      assertEquals("Cyril", words[2].getLabel());
      assertEquals("would", words[3].getLabel());
      assertEquals("arrive", words[4].getLabel());
      assertEquals("at", words[5].getLabel());
      assertEquals("the", words[6].getLabel());
      assertEquals("door", words[7].getLabel());
      assertEquals(Double.valueOf(5.75), words[7].getEnd().getOffset());

      // topic
      Annotation[] topics = g.list("topic");
      assertEquals(9, topics.length);

      assertEquals(Double.valueOf(0.0), topics[0].getStart().getOffset());
      assertEquals(Double.valueOf(23.563), topics[0].getEnd().getOffset());
      assertEquals("teen-decadeplus-friend-general", topics[0].getLabel());
      assertEquals(g, topics[0].getParent());

      assertEquals(Double.valueOf(23.563), topics[1].getStart().getOffset());
      assertEquals(Double.valueOf(183.995), topics[1].getEnd().getOffset());
      assertEquals("teen-decadeplus-me-event", topics[1].getLabel());
      assertEquals(g, topics[1].getParent());

      assertEquals(Double.valueOf(295.115), topics[8].getStart().getOffset());
      assertEquals(Double.valueOf(306.920), topics[8].getEnd().getOffset());
      assertEquals("teen-decadeplus-friend-general", topics[8].getLabel());
      assertEquals(g, topics[8].getParent());

      // noise
      Annotation[] noises = g.list("noise");
      assertEquals(4, noises.length);

      assertEquals(Double.valueOf(174.168), noises[0].getStart().getOffsetMin());
      assertEquals(Double.valueOf(177.59), noises[0].getEnd().getOffsetMax());
      assertEquals("and -", noises[0].getStart().endOf("word").iterator().next().getLabel());
      assertEquals("interviewer: clears throat", noises[0].getLabel());
      assertEquals("--", noises[0].getEnd().startOf("word").iterator().next().getLabel());
      assertEquals(g, noises[0].getParent());

      assertEquals(Double.valueOf(185.339), noises[1].getStart().getOffset());
      assertEquals(Double.valueOf(185.339), noises[1].getEnd().getOffset());
      assertEquals("both laugh", noises[1].getLabel());
      assertEquals(g, noises[1].getParent());

      assertEquals(Double.valueOf(233.727), noises[3].getStart().getOffsetMin());
      assertEquals(Double.valueOf(236.946), noises[3].getEnd().getOffsetMax());
      assertEquals("microphone movement noise", noises[3].getLabel());
      assertEquals(g, noises[3].getParent());

      // comment
      Annotation[] comments = g.list("comment");
      assertEquals(6, comments.length);

      assertEquals(Double.valueOf(55.444), comments[0].getStart().getOffsetMin());
      assertEquals(Double.valueOf(60.101), comments[0].getEnd().getOffsetMax());
      assertEquals("in", comments[0].getStart().endOf("word").iterator().next().getLabel());
      assertEquals("unclear", comments[0].getLabel());
      assertEquals("--", comments[0].getEnd().startOf("word").iterator().next().getLabel());
      assertEquals(g, comments[0].getParent());

      // TODO should test these
      assertEquals(0, g.list("entities").length);
      assertEquals(0, g.list("language").length);
      assertEquals(0, g.list("lexical").length);
      assertEquals(0, g.list("pronounce").length);

   }

   @Test public void transcriptOnly() 
      throws Exception
   {
      Schema schema = new Schema("who", "turn", "utterance", "word",
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true));

      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "test.trs")) };
      
      // create deserializer
      TranscriptSerialization deserializer = new TranscriptSerialization();

      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals("Configuration parameters" + configuration, 18, deserializer.configure(configuration, schema).size());      
      assertNull("topic", configuration.get("topicLayer").getValue());
      assertNull("comment", configuration.get("commentLayer").getValue());
      assertNull("noise", configuration.get("noiseLayer").getValue());
      assertNull("language", configuration.get("languageLayer").getValue());
      assertNull("lexical", configuration.get("lexicalLayer").getValue());
      assertNull("pronounce", configuration.get("pronounceLayer").getValue());
      assertNull("entities", configuration.get("entityLayer").getValue());
      assertNull("transcript_scribe", configuration.get("scribeLayer").getValue());
      assertNull("transcript_version", configuration.get("versionLayer").getValue());
      assertNull("transcript_version_date", configuration.get("versionDateLayer").getValue());
      assertNull("transcript_program", configuration.get("programLayer").getValue());
      assertNull("transcript_airdate", configuration.get("airDateLayer").getValue());
      assertNull("transcript_language", configuration.get("transcriptLanguageLayer").getValue());
      assertNull("participant_check", configuration.get("participantCheckLayer").getValue());
      assertNull("gender", configuration.get("genderLayer").getValue());
      assertNull("participant_dialect", configuration.get("dialectLayer").getValue());
      assertNull("participant_accent", configuration.get("accentLayer").getValue());
      assertNull("participant_scope", configuration.get("scopeLayer").getValue());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      //for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(0, defaultParamaters.size());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("test.trs", g.getId());

      // participants     
      assertEquals(2, g.list("who").length);
      assertEquals("Interviewer", g.getAnnotation("spk1").getLabel());
      assertEquals("who", g.getAnnotation("spk1").getLayerId());
      assertEquals("mop03-2b", g.getAnnotation("spk2").getLabel());
      assertEquals("who", g.getAnnotation("spk2").getLayerId());

      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(26, turns.length);
      assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
      assertEquals(Double.valueOf(23.563), turns[0].getEnd().getOffset());
      assertEquals("mop03-2b", turns[0].getLabel());
      assertEquals(g.getAnnotation("spk2"), turns[0].getParent());
      assertEquals(Double.valueOf(302.834), turns[24].getStart().getOffset());
      assertEquals(Double.valueOf(304.334), turns[24].getEnd().getOffset());
      assertEquals("Interviewer", turns[24].getLabel());
      assertEquals(g.getAnnotation("spk1"), turns[24].getParent());

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(140, utterances.length);
      assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
      assertEquals(Double.valueOf(5.75), utterances[0].getEnd().getOffset());
      assertEquals("mop03-2b", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals(Double.valueOf(5.75), utterances[1].getStart().getOffset());
      assertEquals(Double.valueOf(6.907), utterances[1].getEnd().getOffset());
      assertEquals("mop03-2b", utterances[1].getParent().getLabel());
      
      Annotation[] words = g.list("word");
      assertEquals(Double.valueOf(0), words[0].getStart().getOffset());
      // System.out.println("" + Arrays.asList(Arrays.copyOfRange(words, 0, 10)));
      assertEquals("and", words[0].getLabel());
      assertEquals("ah .", words[1].getLabel());
      assertEquals("Cyril", words[2].getLabel());
      assertEquals("would", words[3].getLabel());
      assertEquals("arrive", words[4].getLabel());
      assertEquals("at", words[5].getLabel());
      assertEquals("the", words[6].getLabel());
      assertEquals("door", words[7].getLabel());
      assertEquals(Double.valueOf(5.75), words[7].getEnd().getOffset());

   }

   @Test public void basicSerialization()
      throws Exception
   {
      Schema schema = new Schema(
         "who", "turn", "utterance", "word",
         new Layer("version_date", "Date")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true),
         new Layer("version", "Version number")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true),
         new Layer("version_date", "Version date")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true),
         new Layer("scribe", "Author")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true),
         new Layer("episode", "Episode")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true),
         new Layer("transcript_language", "Transcript Language")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true),
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
         new Layer("topic", "Topic")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true)
         .setPeersOverlap(false)
         .setSaturated(false),
         new Layer("who", "Participants")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(true)
         .setPeersOverlap(true)
         .setSaturated(true),
         new Layer("gender", "Gender")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true)
         .setParentId("who"),
         new Layer("participant_check", "Check")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true)
         .setParentId("who"),
         new Layer("dialect", "Dialect")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true)
         .setParentId("who"),
         new Layer("accent", "Accent")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true)
         .setParentId("who"),
         new Layer("scope", "Scope")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true)
         .setParentId("who"),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
         new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
         new Layer("language", "Other Language")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true)
         .setPeersOverlap(false)
         .setSaturated(false)
         .setParentId("turn"),
         new Layer("entity", "Named Entity")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true)
         .setPeersOverlap(false)
         .setSaturated(false)
         .setParentId("turn"),
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
         .setId("discrete_turns")
         .setSchema(schema);
      graph.addAnchor(new Anchor("a0", 0.0));
      graph.addAnchor(new Anchor("a15", 15.0));
      // transcript attributes
      graph.addAnnotation(new Annotation("l", "en-NZ", "transcript_language", "a0", "a15"));
      graph.addAnnotation(new Annotation("by", "robert", "scribe", "a0", "a15"));
      graph.addAnnotation(new Annotation("v", "1", "version", "a0", "a15"));
      graph.addAnnotation(new Annotation("date", "20191211", "version_date", "a0", "a15"));
      // participants
      graph.addAnnotation(new Annotation("p1", "p1", "who", "a0", "a15"));
      graph.addAnnotation(new Annotation("p2", "p2", "who", "a0", "a15"));
      // participant attributes
      graph.addAnnotation(new Annotation("g1", "M", "gender", "a0", "a15", "p1"));
      graph.addAnnotation(new Annotation("g2", "F", "gender", "a0", "a15", "p2"));
      graph.addAnnotation(new Annotation("check", "Y", "participant_check", "a0", "a15", "p1"));
      graph.addAnnotation(new Annotation("dialect", "Y", "dialect", "a0", "a15", "p1"));
      graph.addAnnotation(new Annotation("accent", "N", "accent", "a0", "a15", "p2"));
      graph.addAnnotation(new Annotation("scope", "N", "scope", "a0", "a15", "p2"));
      // turns
      graph.addAnchor(new Anchor("a5", 5.0));
      graph.addAnchor(new Anchor("a10", 10.0));
      graph.addAnnotation(new Annotation("t1-1", "p1", "turn", "a0", "a5", "p1"));
      graph.addAnnotation(new Annotation("t1-2", "p1", "turn", "a5", "a10", "p1"));
      graph.addAnnotation(new Annotation("t1-3", "p1", "turn", "a10", "a15", "p1"));
      graph.addAnnotation(new Annotation("t2", "p2", "turn", "a5", "a10", "p2"));
      // utterances
      graph.addAnnotation(new Annotation("u1-1", "p1", "utterance", "a0", "a5", "t1-1"));
      graph.addAnnotation(new Annotation("u1-2", "p1", "utterance", "a5", "a10", "t1-2"));
      graph.addAnnotation(new Annotation("u2-1", "p2", "utterance", "a5", "a10", "t2"));
      graph.addAnnotation(new Annotation("u1-3", "p1", "utterance", "a10", "a15", "t1-3"));
      // topic
      graph.addAnnotation(new Annotation("topic", "testing", "topic", "a0", "a10"));

      // words
      graph.addAnnotation(new Annotation("w1-1", "w1-1", "word", 
                                         graph.addAnchor(new Anchor("a1", 1.0)).getId(),
                                         graph.addAnchor(new Anchor("a2", 2.0)).getId(),
                                         "t1-1"));
      graph.addAnnotation(new Annotation("w1-2", "w1-2", "word", 
                                         "a2",
                                         graph.addAnchor(new Anchor("a3", 3.0)).getId(),
                                         "t1-1"));
      graph.addAnnotation(new Annotation("w1-3", "w1-3", "word", 
                                         "a3",
                                         graph.addAnchor(new Anchor("a4", 4.0)).getId(),
                                         "t1-1"));
      
      graph.addAnnotation(new Annotation("w1-6", "w1-6", "word", 
                                         graph.addAnchor(new Anchor("a6", 6.0)).getId(),
                                         graph.addAnchor(new Anchor("a7", 7.0)).getId(),
                                         "t1-2"));
      graph.addAnnotation(new Annotation("w1-7", "w1-7", "word", 
                                         "a7", 
                                         graph.addAnchor(new Anchor("a8", 8.0)).getId(),
                                         "t1-2"));
      graph.addAnnotation(new Annotation("w1-8", "w1-8", "word", 
                                         "a8",
                                         graph.addAnchor(new Anchor("a9", 9.0)).getId(),
                                         "t1-2"));
      

      graph.addAnnotation(new Annotation("w1-11", "w1-11", "word", 
                                         graph.addAnchor(new Anchor("a11", 11.0)).getId(),
                                         graph.addAnchor(new Anchor("a12", 12.0)).getId(),
                                         "t1-3"));
      graph.addAnnotation(new Annotation("w1-12", "w1-12", "word", 
                                         "a12",
                                         graph.addAnchor(new Anchor("a13", 13.0)).getId(),
                                         "t1-3"));
      graph.addAnnotation(new Annotation("w1-13", "w1-13", "word", 
                                         "a13",
                                         graph.addAnchor(new Anchor("a14", 14.0)).getId(),
                                         "t1-3"));
      
      graph.addAnnotation(new Annotation("w2-6.5", "w2-6.5", "word", 
                                         graph.addAnchor(new Anchor("a6.5", 6.5)).getId(),
                                         graph.addAnchor(new Anchor("a7.5", 7.5)).getId(),
                                         "t2"));
      graph.addAnnotation(new Annotation("w2-7.5", "w2-7.5", "word", 
                                         "a7.5",
                                         graph.addAnchor(new Anchor("a8.5", 8.5)).getId(),
                                         "t2"));

      // add some non-speech annotations
      graph.addAnnotation(new Annotation("comment1", "preamble", "comment", "a0", "a1"));
      graph.addAnnotation(new Annotation("noise1", "throat-clear", "noise", "a4", "a5"));
      graph.addAnnotation(new Annotation("lex1", "lex-1-6", "lexical", "a6", "a7", "w1-6"));
      graph.addAnnotation(new Annotation("lex2", "lex-1-7", "lexical", "a7", "a8", "w1-7"));
      graph.addAnnotation(new Annotation("pron1", "pron-1-7", "pronounce", "a7", "a8", "w1-7"));
      graph.addAnnotation(new Annotation("pron2", "pron-1-8", "pronounce", "a8", "a9", "w1-7"));
      
      graph.addAnnotation(new Annotation("lUS", "en-US", "language", "a11", "a13", "t1-3"));
      graph.addAnnotation(new Annotation("ent1", "animal", "entity", "a12", "a14", "t1-3"));

      // add a media handler to test MEDIA_DESCRIPTOR
      graph.setMediaProvider(new IGraphMediaProvider() {
            public MediaFile[] getAvailableMedia() throws StoreException, PermissionException
            {
               MediaFile[] media = {
                  new MediaFile().setMimeType("audio/wav").setName("test.wav")
               };
               return media;
            }
            public String getMedia(String trackSuffix, String mimeType)
               throws StoreException, PermissionException
            { return "file://test.wav"; }
            public IGraphMediaProvider providerForGraph(Graph graph)
            { return this; }
         });  

      // add orthography tags that should not be used because orthography is not selected
      for (Annotation word : graph.list("word"))
      {
         graph.addTag(word, "orthography", word.getLabel()+"-orthography");
      }
      
      // create serializer
      TranscriptSerialization serializer = new TranscriptSerialization();
      
      // general configuration
      ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(18, serializer.configure(configuration, schema).size());
      assertEquals("topic", "topic", 
		   ((Layer)configuration.get("topicLayer").getValue()).getId());
      assertEquals("comment", "comment", 
		   ((Layer)configuration.get("commentLayer").getValue()).getId());
      assertEquals("noise", "noise", 
		   ((Layer)configuration.get("noiseLayer").getValue()).getId());
      assertEquals("language", "language", 
		   ((Layer)configuration.get("languageLayer").getValue()).getId());
      assertEquals("lexical", "lexical", 
		   ((Layer)configuration.get("lexicalLayer").getValue()).getId());
      assertEquals("pronounce", "pronounce", 
		   ((Layer)configuration.get("pronounceLayer").getValue()).getId());
      assertEquals("entity", "entity", 
		   ((Layer)configuration.get("entityLayer").getValue()).getId());
      assertEquals("scribe", "scribe", 
		   ((Layer)configuration.get("scribeLayer").getValue()).getId());
      assertEquals("version", "version", 
		   ((Layer)configuration.get("versionLayer").getValue()).getId());
      assertEquals("version_date", "version_date", 
		   ((Layer)configuration.get("versionDateLayer").getValue()).getId());
      assertEquals("program", "episode", 
		   ((Layer)configuration.get("programLayer").getValue()).getId());
      assertNull("air date", 
                 configuration.get("airDateLayer").getValue());
      assertEquals("transcript language", "transcript_language", 
		   ((Layer)configuration.get("transcriptLanguageLayer").getValue()).getId());
      assertEquals("participant check", "participant_check", 
                 ((Layer)configuration.get("participantCheckLayer").getValue()).getId());
      assertEquals("gender", "gender", 
		   ((Layer)configuration.get("genderLayer").getValue()).getId());
      assertEquals("dialect", "dialect", 
		   ((Layer)configuration.get("dialectLayer").getValue()).getId());
      assertEquals("accent", "accent", 
		   ((Layer)configuration.get("accentLayer").getValue()).getId());
      assertEquals("scope", "scope", 
		   ((Layer)configuration.get("scopeLayer").getValue()).getId());

      LinkedHashSet<String> needLayers = new LinkedHashSet<String>(
         Arrays.asList(serializer.getRequiredLayers()));
      assertEquals("Needed layers: " + needLayers,
                   21, needLayers.size());
      assertTrue(needLayers.contains("who"));
      assertTrue(needLayers.contains("turn"));
      assertTrue(needLayers.contains("utterance"));
      assertTrue(needLayers.contains("word"));
      assertTrue(needLayers.contains("pronounce"));
      assertTrue(needLayers.contains("lexical"));
      assertTrue(needLayers.contains("comment"));
      assertTrue(needLayers.contains("noise"));
      assertTrue(needLayers.contains("topic"));
      assertTrue(needLayers.contains("language"));
      assertTrue(needLayers.contains("transcript_language"));
      assertTrue(needLayers.contains("entity"));
      assertTrue(needLayers.contains("scribe"));
      assertTrue(needLayers.contains("version"));
      assertTrue(needLayers.contains("version_date"));
      assertTrue(needLayers.contains("episode"));
      assertTrue(needLayers.contains("gender"));
      assertTrue(needLayers.contains("participant_check"));
      assertTrue(needLayers.contains("scope"));
      assertTrue(needLayers.contains("dialect"));
      assertTrue(needLayers.contains("accent"));
	 
      // serialize
      final Vector<SerializationException> exceptions = new Vector<SerializationException>();
      final Vector<NamedStream> streams = new Vector<NamedStream>();
      String[] layers = {"word"}; // ignored, actually
      serializer.serialize(Utility.OneGraphSpliterator(graph), layers,
                           stream -> streams.add(stream),
                           warning -> System.out.println(warning),
                           exception -> exceptions.add(exception));
      if (exceptions.size() > 0) fail(""+exceptions);

      streams.elementAt(0).save(dir);

      // test using diff
      File result = new File(dir, graph.getId() + ".trs");
      String differences = diff(new File(dir, "expected_" + graph.getId() + ".trs"), result);
      if (differences != null)
      {
         fail(differences);
      }
      else
      {
         result.delete();
      }
   }
   
   @Test public void simultaneousSpeechSerialization()
      throws Exception
   {
      Schema schema = new Schema(
         "who", "turn", "utterance", "word",
         new Layer("version_date", "Date")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true),
         new Layer("version", "Version number")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true),
         new Layer("version_date", "Version date")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true),
         new Layer("scribe", "Author")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true),
         new Layer("episode", "Episode")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true),
         new Layer("transcript_language", "Transcript Language")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true),
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
         new Layer("topic", "Topic")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true)
         .setPeersOverlap(false)
         .setSaturated(false),
         new Layer("who", "Participants")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(true)
         .setPeersOverlap(true)
         .setSaturated(true),
         new Layer("gender", "Gender")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true)
         .setParentId("who"),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
         new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
         new Layer("language", "Other Language")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true)
         .setPeersOverlap(false)
         .setSaturated(false)
         .setParentId("turn"),
         new Layer("entity", "Named Entity")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true)
         .setPeersOverlap(false)
         .setSaturated(false)
         .setParentId("turn"),
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
         .setId("simultaneous_speech")
         .setSchema(schema);
      graph.addAnchor(new Anchor("a0", 0.0));
      graph.addAnchor(new Anchor("a15", 15.0));
      // transcript attributes
      graph.addAnnotation(new Annotation("l", "en-NZ", "transcript_language", "a0", "a15"));
      graph.addAnnotation(new Annotation("by", "robert", "scribe", "a0", "a15"));
      graph.addAnnotation(new Annotation("v", "1", "version", "a0", "a15"));
      graph.addAnnotation(new Annotation("date", "20191211", "version_date", "a0", "a15"));
      // participants
      graph.addAnnotation(new Annotation("p1", "p1", "who", "a0", "a15"));
      graph.addAnnotation(new Annotation("p2", "p2", "who", "a0", "a15"));
      // participant attributes
      graph.addAnnotation(new Annotation("g1", "M", "gender", "a0", "a15", "p1"));
      graph.addAnnotation(new Annotation("g2", "F", "gender", "a0", "a15", "p2"));
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
      // topic
      graph.addAnnotation(new Annotation("topic", "testing", "topic", "a0", "a10", "t1"));

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

      // add some non-speech annotations
      graph.addAnnotation(new Annotation("comment1", "preamble", "comment", "a0", "a1"));
      graph.addAnnotation(new Annotation("noise1", "throat-clear", "noise", "a4", "a5"));
      graph.addAnnotation(new Annotation("lex1", "lex-1-6", "lexical", "a6", "a7", "w1-6"));
      graph.addAnnotation(new Annotation("lex2", "lex-1-7", "lexical", "a7", "a8", "w1-7"));
      graph.addAnnotation(new Annotation("pron1", "pron-1-7", "pronounce", "a7", "a8", "w1-7"));
      graph.addAnnotation(new Annotation("pron2", "pron-1-8", "pronounce", "a8", "a9", "w1-7"));
      
      graph.addAnnotation(new Annotation("lUS", "en-US", "language", "a11", "a13"));
      graph.addAnnotation(new Annotation("ent1", "animal", "entity", "a12", "a14"));

      // add a media handler to test MEDIA_DESCRIPTOR
      graph.setMediaProvider(new IGraphMediaProvider() {
            public MediaFile[] getAvailableMedia() throws StoreException, PermissionException
            {
               MediaFile[] media = {
                  new MediaFile().setMimeType("audio/wav").setName("test.wav")
               };
               return media;
            }
            public String getMedia(String trackSuffix, String mimeType)
               throws StoreException, PermissionException
            { return "file://test.wav"; }
            public IGraphMediaProvider providerForGraph(Graph graph)
            { return this; }
         });  

      // add orthography tags that should not be used because orthography is not selected
      for (Annotation word : graph.list("word"))
      {
         graph.addTag(word, "orthography", word.getLabel()+"-orthography");
      }
      
      // create serializer
      TranscriptSerialization serializer = new TranscriptSerialization();
      
      // general configuration
      ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(18, serializer.configure(configuration, schema).size());
      assertEquals("topic", "topic", 
		   ((Layer)configuration.get("topicLayer").getValue()).getId());
      assertEquals("comment", "comment", 
		   ((Layer)configuration.get("commentLayer").getValue()).getId());
      assertEquals("noise", "noise", 
		   ((Layer)configuration.get("noiseLayer").getValue()).getId());
      assertEquals("language", "language", 
		   ((Layer)configuration.get("languageLayer").getValue()).getId());
      assertEquals("lexical", "lexical", 
		   ((Layer)configuration.get("lexicalLayer").getValue()).getId());
      assertEquals("pronounce", "pronounce", 
		   ((Layer)configuration.get("pronounceLayer").getValue()).getId());
      assertEquals("entity", "entity", 
		   ((Layer)configuration.get("entityLayer").getValue()).getId());
      assertEquals("scribe", "scribe", 
		   ((Layer)configuration.get("scribeLayer").getValue()).getId());
      assertEquals("version", "version", 
		   ((Layer)configuration.get("versionLayer").getValue()).getId());
      assertEquals("version_date", "version_date", 
		   ((Layer)configuration.get("versionDateLayer").getValue()).getId());
      assertEquals("program", "episode", 
		   ((Layer)configuration.get("programLayer").getValue()).getId());
      assertNull("air date", 
                 configuration.get("airDateLayer").getValue());
      assertEquals("transcript language", "transcript_language", 
		   ((Layer)configuration.get("transcriptLanguageLayer").getValue()).getId());
      assertNull("participant check", 
                 configuration.get("participantCheckLayer").getValue());
      assertEquals("gender", "gender", 
		   ((Layer)configuration.get("genderLayer").getValue()).getId());
      assertNull("dialect", 
                 configuration.get("dialectLayer").getValue());
      assertNull("accent", 
                 configuration.get("accentLayer").getValue());
      assertNull("scope", 
                 configuration.get("scopeLayer").getValue());

      LinkedHashSet<String> needLayers = new LinkedHashSet<String>(
         Arrays.asList(serializer.getRequiredLayers()));
      assertEquals("Needed layers: " + needLayers,
                   17, needLayers.size());
      assertTrue(needLayers.contains("who"));
      assertTrue(needLayers.contains("turn"));
      assertTrue(needLayers.contains("utterance"));
      assertTrue(needLayers.contains("word"));
      assertTrue(needLayers.contains("pronounce"));
      assertTrue(needLayers.contains("lexical"));
      assertTrue(needLayers.contains("comment"));
      assertTrue(needLayers.contains("noise"));
      assertTrue(needLayers.contains("topic"));
      assertTrue(needLayers.contains("language"));
      assertTrue(needLayers.contains("transcript_language"));
      assertTrue(needLayers.contains("entity"));
      assertTrue(needLayers.contains("scribe"));
      assertTrue(needLayers.contains("version"));
      assertTrue(needLayers.contains("version_date"));
      assertTrue(needLayers.contains("episode"));
      assertTrue(needLayers.contains("gender"));
	 
      // serialize
      final Vector<SerializationException> exceptions = new Vector<SerializationException>();
      final Vector<NamedStream> streams = new Vector<NamedStream>();
      String[] layers = {"word"}; // ignored, actually
      serializer.serialize(Utility.OneGraphSpliterator(graph), layers,
                           stream -> streams.add(stream),
                           warning -> System.out.println(warning),
                           exception -> exceptions.add(exception));
      if (exceptions.size() > 0) fail(""+exceptions);

      streams.elementAt(0).save(dir);

      // test using diff
      File result = new File(dir, graph.getId() + ".trs");
      String differences = diff(new File(dir, "expected_" + graph.getId() + ".trs"), result);
      if (differences != null)
      {
         fail(differences);
      }
      else
      {
         result.delete();
      }
   }
   
   /**
    * Diffs two files.
    * @param expected
    * @param actual
    * @return null if the files are the same, and a String describing differences if not.
    */
   public String diff(File expected, File actual)
   {
      StringBuffer d = new StringBuffer();
      
      try
      {
         // compare with what we expected
         Vector<String> actualLines = new Vector<String>();
         BufferedReader reader = new BufferedReader(new FileReader(actual));
         String line = reader.readLine();
         while (line != null)
         {
            actualLines.add(line);
            line = reader.readLine();
         }
         Vector<String> expectedLines = new Vector<String>();
         reader = new BufferedReader(new FileReader(expected));
         line = reader.readLine();
         while (line != null)
         {
            expectedLines.add(line);
            line = reader.readLine();
         }
         MinimumEditPath<String> comparator = new MinimumEditPath<String>();
         List<EditStep<String>> path = comparator.minimumEditPath(expectedLines, actualLines);
         for (EditStep<String> step : path)
         {
            switch (step.getOperation())
            {
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
      }
      catch(Exception exception)
      {
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
      org.junit.runner.JUnitCore.main("nzilbb.ag.test.TestTranscriptSerialization");
   }
}
