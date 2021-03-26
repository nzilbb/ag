//
// Copyright 2017-2019 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.formatter.elan;
	      
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
import nzilbb.formatter.elan.*;

public class TestEAFSerialization {
   
   @Test public void utterance()  throws Exception {
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("scribe", "Author", 0, true, true, true),
	 new Layer("version_date", "Date", 0, true, true, true),
	 new Layer("lang", "Language", 0, true, true, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("comment", "Comment", 2, true, false, true),
	 new Layer("noise", "Noise", 2, true, false, true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true),
	 new Layer("lexical", "Lexical", 0, true, false, false, "word", true),
	 new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "test_utterance.eaf")) };
      
      // create deserializer
      EAFSerialization deserializer = new EAFSerialization();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(10, configuration.size());
      assertEquals("comment", "comment", 
		   ((Layer)configuration.get("commentLayer").getValue()).getId());
      assertEquals("pronounce", "pronounce", 
		   ((Layer)configuration.get("pronounceLayer").getValue()).getId());
      assertEquals("lexical", "lexical", 
		   ((Layer)configuration.get("lexicalLayer").getValue()).getId());
      assertEquals("noise", "noise", 
		   ((Layer)configuration.get("noiseLayer").getValue()).getId());
      assertEquals("author", "scribe", 
		   ((Layer)configuration.get("authorLayer").getValue()).getId());
      assertEquals("version_date", "version_date", 
		   ((Layer)configuration.get("dateLayer").getValue()).getId());
      assertEquals("language", "lang", 
		   ((Layer)configuration.get("languageLayer").getValue()).getId());
      assertEquals("useConventions", Boolean.TRUE, 
		   (Boolean)configuration.get("useConventions").getValue());
      assertEquals("ignoreBlankAnnotations", Boolean.TRUE, 
		   (Boolean)configuration.get("ignoreBlankAnnotations").getValue());
      assertEquals("minimumTurnPauseLength", Double.valueOf(0.0), 
		   (Double)configuration.get("minimumTurnPauseLength").getValue());
      
      configuration.get("minimumTurnPauseLength").setValue(Double.valueOf(0.5));
      assertEquals(10, deserializer.configure(configuration, schema).size());
      assertEquals("customize minimumTurnPauseLength", Double.valueOf(0.5), 
		   deserializer.getMinimumTurnPauseLength());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
      assertEquals(2, defaultParamaters.size());
      assertEquals("utterance mapping", "utterance", 
		   ((Layer)defaultParamaters.get("tier0").getValue()).getId());
      assertEquals("utterance mapping", "utterance", 
		   ((Layer)defaultParamaters.get("tier1").getValue()).getId());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);
      
      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("test_utterance.eaf", g.getId());

      // attributes
      assertEquals("transcriber", "Robert", g.first("scribe").getLabel());
      assertEquals("language", "eng", g.first("lang").getLabel());
      assertEquals("version date", "2017-08-28T16:48:05-03:00", g.first("version_date").getLabel());

      // participants     
      Annotation[] who = g.all("who");
      assertEquals(2, who.length);
      assertEquals("interviewer", who[0].getLabel());
      assertEquals(g, who[0].getParent());
      assertEquals("participant", who[1].getLabel());
      assertEquals(g, who[1].getParent());
      
      // turns
      Annotation[] turns = g.all("turn");
      assertEquals(13, turns.length);
      assertEquals(Double.valueOf(4.675), turns[0].getStart().getOffset());
      assertEquals(Double.valueOf(6.752), turns[0].getEnd().getOffset());
      assertEquals("participant", turns[0].getLabel());
      assertEquals(who[1], turns[0].getParent());
      
      assertEquals("turn after long pause not merged back to turn before pause",
		   Double.valueOf(7.658), turns[1].getStart().getOffset());
      assertEquals("turn after pause merged back to turn before pause",
		   Double.valueOf(14.889000000000001), turns[1].getEnd().getOffset());
      assertEquals("participant", turns[1].getLabel());
      assertEquals(who[1], turns[1].getParent());
      
      assertEquals(Double.valueOf(14.889000000000001), turns[2].getStart().getOffset());
      assertEquals(Double.valueOf(23.170), turns[2].getEnd().getOffset());
      assertEquals("interviewer", turns[2].getLabel());
      assertEquals(who[0], turns[2].getParent());

      assertEquals(Double.valueOf(17.983), turns[3].getStart().getOffset());
      assertEquals(Double.valueOf(140.366), turns[3].getEnd().getOffset());
      assertEquals("participant", turns[3].getLabel());

      assertEquals(Double.valueOf(140.366), turns[4].getStart().getOffset());
      assertEquals(Double.valueOf(159.457), turns[4].getEnd().getOffset());
      assertEquals("interviewer", turns[4].getLabel());

      assertEquals(Double.valueOf(159.457), turns[5].getStart().getOffset());
      assertEquals(Double.valueOf(282.871), turns[5].getEnd().getOffset());
      assertEquals("participant", turns[5].getLabel());

      assertEquals(Double.valueOf(282.871), turns[6].getStart().getOffset());
      assertEquals(Double.valueOf(283.96500000000003), turns[6].getEnd().getOffset());
      assertEquals("interviewer", turns[6].getLabel());

      assertEquals(Double.valueOf(283.96500000000003), turns[7].getStart().getOffset());
      assertEquals(Double.valueOf(310.60200000000003), turns[7].getEnd().getOffset());
      assertEquals("participant", turns[7].getLabel());

      assertEquals("simultaneous speech",
		   Double.valueOf(284.84000000000003), turns[8].getStart().getOffset());
      assertEquals("simultaneous speech",
		   Double.valueOf(285.34000000000003), turns[8].getEnd().getOffset());
      assertEquals("interviewer", turns[8].getLabel());

      assertEquals(Double.valueOf(310.60200000000003), turns[9].getStart().getOffset());
      assertEquals(Double.valueOf(311.071), turns[9].getEnd().getOffset());
      assertEquals("interviewer", turns[9].getLabel());

      assertEquals(Double.valueOf(311.071), turns[10].getStart().getOffset());
      assertEquals(Double.valueOf(316.258), turns[10].getEnd().getOffset());
      assertEquals("participant", turns[10].getLabel());

      assertEquals(Double.valueOf(316.258), turns[11].getStart().getOffset());
      assertEquals(Double.valueOf(317.195), turns[11].getEnd().getOffset());
      assertEquals("interviewer", turns[11].getLabel());

      assertEquals(Double.valueOf(317.195), turns[12].getStart().getOffset());
      assertEquals(Double.valueOf(320.757), turns[12].getEnd().getOffset());
      assertEquals("participant", turns[12].getLabel());

      // utterances
      Annotation[] utterances = g.all("utterance");
      assertEquals(147, utterances.length);
      assertEquals(Double.valueOf(4.675), utterances[0].getStart().getOffset());
      assertEquals(Double.valueOf(6.752), utterances[0].getEnd().getOffset());
      assertEquals("participant", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals(Double.valueOf(7.658), utterances[1].getStart().getOffset());
      assertEquals("turn after pause merged back to turn before pause",
		   Double.valueOf(10.348), utterances[1].getEnd().getOffset());
      assertEquals("participant", utterances[1].getParent().getLabel());
      assertEquals("turn after long pause not merged back to turn before pause",
		   turns[1], utterances[1].getParent());

      assertEquals(Double.valueOf(14.889000000000001), utterances[4].getStart().getOffset());
      assertEquals(Double.valueOf(15.639000000000001), utterances[4].getEnd().getOffset());
      assertEquals("interviewer", utterances[4].getParent().getLabel());
      assertEquals(turns[2], utterances[4].getParent());

      Annotation[] words = g.all("word");
      String[] wordLabels = {
	 ". rest", "of", "that", "side", "of", "the", "famly", "so", "he --"
      };
      for (int i = 0; i < wordLabels.length; i++) {
	 assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
	 assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
	 	      i+1, words[i].getOrdinal());
	 assertEquals(turns[0].getId(), words[i].getParentId());
      }
      String[] wordLabelsAfterPause = {
	 "generously", "agreed", "that", "she", "could", "go", "with", "him", "but", 
	 "there", "were", "so", "many", "people", "there", 
	 "that", "nothing", "was", "done", "constructively"
      };
      for (int i = 0; i < wordLabelsAfterPause.length; i++) {
	 assertEquals("word labels " + (i+wordLabels.length), wordLabelsAfterPause[i], words[i+wordLabels.length].getLabel());
	 assertEquals("Correct ordinal: " + i + " " + words[i+wordLabels.length].getLabel(), 
	 	      i+1, words[i+wordLabels.length].getOrdinal());
	 assertEquals(turns[1].getId(), words[i+wordLabels.length].getParentId());
      }

      // comment
      Annotation[] comments = g.all("comment");
      assertEquals("unclear", comments[0].getLabel());
      assertEquals("whatever", comments[0].getStart().endOf("word").iterator().next().getLabel());
      assertEquals("and", comments[0].getEnd().startOf("word").iterator().next().getLabel());

      assertEquals(1, comments.length);

      // noise
      Annotation[] noises = g.all("noise");
      assertEquals("click", noises[0].getLabel());
      assertEquals("um --", noises[0].getStart().endOf("word").iterator().next().getLabel());
      assertEquals(Double.valueOf(132.992), noises[0].getEnd().getOffset());

      assertEquals(1, noises.length);

      // ensure order of word tags isn't important
      
      // pronounce
      Annotation[] pronounce = g.all("pronounce");
      assertEquals("f{mli", pronounce[0].getLabel());
      assertEquals("famly", pronounce[0].first("word").getLabel());
      assertEquals("@grid", pronounce[1].getLabel());
      assertEquals("agreed", pronounce[1].first("word").getLabel());
      assertEquals(2, pronounce.length);

      // lexical
      Annotation[] lexical = g.all("lexical");
      assertEquals("family", lexical[0].getLabel());
      assertEquals("famly", lexical[0].first("word").getLabel());
      assertEquals("agrees", lexical[1].getLabel());
      assertEquals("agreed", lexical[1].first("word").getLabel());
      assertEquals(2, lexical.length);

      // check all annotations have 'manual' confidence
      for (Annotation a : g.getAnnotationsById().values()) {
         assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }
   }

   @Test public void utterance_word()  throws Exception {
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("scribe", "Author", 0, true, true, true),
	 new Layer("version_date", "Date", 0, true, true, true),
	 new Layer("lang", "Language", 0, true, true, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("comment", "Comment", 2, true, false, true),
	 new Layer("noise", "Noise", 2, true, false, true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true),
	 new Layer("lexical", "Lexical", 0, true, false, false, "word", true),
	 new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "test_utterance_word.eaf")) };
      
      // create deserializer
      EAFSerialization deserializer = new EAFSerialization();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(10, deserializer.configure(configuration, schema).size());
      assertEquals("comment", "comment", 
		   ((Layer)configuration.get("commentLayer").getValue()).getId());
      assertEquals("pronounce", "pronounce", 
		   ((Layer)configuration.get("pronounceLayer").getValue()).getId());
      assertEquals("lexical", "lexical", 
		   ((Layer)configuration.get("lexicalLayer").getValue()).getId());
      assertEquals("noise", "noise", 
		   ((Layer)configuration.get("noiseLayer").getValue()).getId());
      assertEquals("author", "scribe", 
		   ((Layer)configuration.get("authorLayer").getValue()).getId());
      assertEquals("version_date", "version_date", 
		   ((Layer)configuration.get("dateLayer").getValue()).getId());
      assertEquals("language", "lang", 
		   ((Layer)configuration.get("languageLayer").getValue()).getId());
      assertEquals("useConventions", Boolean.TRUE, 
		   (Boolean)configuration.get("useConventions").getValue());
      assertEquals("ignoreBlankAnnotations", Boolean.TRUE, 
		   (Boolean)configuration.get("ignoreBlankAnnotations").getValue());
      assertEquals("minimumTurnPauseLength", Double.valueOf(0.0), 
		   (Double)configuration.get("minimumTurnPauseLength").getValue());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
      assertEquals(6, defaultParamaters.size());
      assertEquals("utterance mapping", "utterance", 
		   ((Layer)defaultParamaters.get("tier0").getValue()).getId());
      assertEquals("utterance mapping", "utterance", 
		   ((Layer)defaultParamaters.get("tier1").getValue()).getId());
      assertEquals("noise mapping - case mismatch", "noise", 
		   ((Layer)defaultParamaters.get("tier2").getValue()).getId());
      assertEquals("comment mapping", "comment", 
		   ((Layer)defaultParamaters.get("tier3").getValue()).getId());
      assertEquals("word mapping", "word", 
		   ((Layer)defaultParamaters.get("tier4").getValue()).getId());
      assertEquals("word mapping", "word", 
		   ((Layer)defaultParamaters.get("tier5").getValue()).getId());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);
      
      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }
      
      assertEquals("test_utterance_word.eaf", g.getId());

      // attributes
      assertNull("no transcriber", g.first("scribe"));
      assertNull("no language specified", g.first("lang"));
      assertEquals("version date", "2017-03-16T11:20:04-03:00", g.first("version_date").getLabel());

      // participants     
      Annotation[] who = g.all("who");
      assertEquals(2, who.length);
      assertEquals("interviewer", who[0].getLabel());
      assertEquals(g, who[0].getParent());
      assertEquals("participant", who[1].getLabel());
      assertEquals(g, who[1].getParent());
      assertNotNull("participant anchors set", who[0].getStart());
      assertNotNull("participant anchors set", who[0].getEnd());
      assertNotNull("participant anchors set", who[1].getStart());
      assertNotNull("participant anchors set", who[1].getEnd());
      
      // turns
      Annotation[] turns = g.all("turn");
      assertEquals(12, turns.length);
      assertEquals(Double.valueOf(4.675), turns[0].getStart().getOffset());
      assertEquals(Double.valueOf(14.889000000000001), turns[0].getEnd().getOffset());
      assertEquals("participant", turns[0].getLabel());
      assertEquals(who[1], turns[0].getParent());
      
      assertEquals(Double.valueOf(14.889000000000001), turns[1].getStart().getOffset());
      assertEquals(Double.valueOf(23.170), turns[1].getEnd().getOffset());
      assertEquals("interviewer", turns[1].getLabel());
      assertEquals(who[0], turns[1].getParent());

      assertEquals(Double.valueOf(17.983), turns[2].getStart().getOffset());
      assertEquals(Double.valueOf(140.366), turns[2].getEnd().getOffset());
      assertEquals("participant", turns[2].getLabel());

      assertEquals(Double.valueOf(140.366), turns[3].getStart().getOffset());
      assertEquals(Double.valueOf(159.457), turns[3].getEnd().getOffset());
      assertEquals("interviewer", turns[3].getLabel());

      assertEquals(Double.valueOf(159.457), turns[4].getStart().getOffset());
      assertEquals(Double.valueOf(282.871), turns[4].getEnd().getOffset());
      assertEquals("participant", turns[4].getLabel());

      assertEquals(Double.valueOf(282.871), turns[5].getStart().getOffset());
      assertEquals(Double.valueOf(283.96500000000003), turns[5].getEnd().getOffset());
      assertEquals("interviewer", turns[5].getLabel());

      assertEquals(Double.valueOf(283.96500000000003), turns[6].getStart().getOffset());
      assertEquals(Double.valueOf(310.60200000000003), turns[6].getEnd().getOffset());
      assertEquals("participant", turns[6].getLabel());

      assertEquals("simultaneous speech",
		   Double.valueOf(284.84000000000003), turns[7].getStart().getOffset());
      assertEquals("simultaneous speech",
		   Double.valueOf(285.34000000000003), turns[7].getEnd().getOffset());
      assertEquals("interviewer", turns[7].getLabel());

      assertEquals(Double.valueOf(310.60200000000003), turns[8].getStart().getOffset());
      assertEquals(Double.valueOf(311.071), turns[8].getEnd().getOffset());
      assertEquals("interviewer", turns[8].getLabel());

      assertEquals(Double.valueOf(311.071), turns[9].getStart().getOffset());
      assertEquals(Double.valueOf(316.258), turns[9].getEnd().getOffset());
      assertEquals("participant", turns[9].getLabel());

      assertEquals(Double.valueOf(316.258), turns[10].getStart().getOffset());
      assertEquals(Double.valueOf(317.195), turns[10].getEnd().getOffset());
      assertEquals("interviewer", turns[10].getLabel());

      assertEquals(Double.valueOf(317.195), turns[11].getStart().getOffset());
      assertEquals(Double.valueOf(321.240), turns[11].getEnd().getOffset());
      assertEquals("participant", turns[11].getLabel());

      // utterances
      Annotation[] utterances = g.all("utterance");
      assertEquals(148, utterances.length);
      assertEquals(Double.valueOf(4.675), utterances[0].getStart().getOffset());
      assertEquals(Double.valueOf(6.752), utterances[0].getEnd().getOffset());
      assertEquals("participant", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals(Double.valueOf(6.752), utterances[1].getStart().getOffset());
      assertEquals(Double.valueOf(10.515), utterances[1].getEnd().getOffset());
      assertEquals("participant", utterances[1].getParent().getLabel());
      assertEquals(turns[0], utterances[1].getParent());

      assertEquals(Double.valueOf(14.889000000000001), utterances[4].getStart().getOffset());
      assertEquals(Double.valueOf(15.639000000000001), utterances[4].getEnd().getOffset());
      assertEquals("interviewer", utterances[4].getParent().getLabel());
      assertEquals(turns[1], utterances[4].getParent());

      Annotation[] words = g.all("word");
      String[] wordLabels = {
	 "  . rest", "of", "that", "side", "of", "the", "family", "so", "he -- ",
	 " generously", "agreed", "that", "she", "could", "go", "with", "him", "but ", 
	 " there", "were", "so", "many", "people", "there ", 
	 " that", "nothing", "was", "done", "constructively "
      };
      Double[] wordStarts = {
	 4.675, 4.917, 5.074, 5.279, 5.539, 5.611, 5.665, 6.112, 6.414,
	 6.752, 8.512, 8.872, 9.077, 9.222, 9.412, 9.662, 9.962, 10.202, 
	 10.515, 10.791, 11.067, 11.343, 11.619, 11.895, 
	 12.171, 12.451, 12.811, 13.031, 13.346
      };
      Double[] wordEnds = {
	 4.917, 5.074, 5.279, 5.539, 5.611, 5.665, 6.112, 6.414, 6.752, 
	 8.512, 8.872, 9.077, 9.222, 9.412, 9.662, 9.902000000000001, 10.202, 10.515,
	 10.791, 11.067, 11.343, 11.619, 11.895, 12.171,
	 12.411, 12.811, 12.991, 13.346, 14.889000000000001
      };
      for (int i = 0; i < wordLabels.length; i++) {
	 assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
	 assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
	 	      i+1, words[i].getOrdinal());
	 assertEquals("word starts " + i + " " + wordLabels[i],
		      wordStarts[i], words[i].getStart().getOffset());
	 assertEquals("word ends " + i + " " + wordLabels[i],
		      wordEnds[i], words[i].getEnd().getOffset());
	 assertEquals(turns[0].getId(), words[i].getParentId());
      }

      // comment
      Annotation[] comments = g.all("comment");
      assertEquals(1, comments.length);
      assertEquals("unclear", comments[0].getLabel());
      assertEquals(Double.valueOf(102.164), comments[0].getStart().getOffset());
      assertEquals(Double.valueOf(102.424), comments[0].getEnd().getOffset());

      // noise
      Annotation[] noises = g.all("noise");
      assertEquals(1, noises.length);
      assertEquals("click", noises[0].getLabel());
      assertEquals(Double.valueOf(129.612), noises[0].getStart().getOffset());
      assertEquals(Double.valueOf(130.242), noises[0].getEnd().getOffset());

      // pronounce
      Annotation[] pronounce = g.all("pronounce");
      assertEquals(0, pronounce.length);

      // lexical
      Annotation[] lexical = g.all("lexical");
      assertEquals(0, lexical.length);
      
      // check all annotations have 'manual' confidence
      for (Annotation a : g.getAnnotationsById().values()) {
         assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }
   }

   @Test public void utterance_word_phone() throws Exception {
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("scribe", "Author", 0, true, true, true),
	 new Layer("version_date", "Date", 0, true, true, true),
	 new Layer("lang", "Language", 0, true, true, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("comment", "Comment", 2, true, false, true),
	 new Layer("noise", "Noise", 2, true, false, true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true),
	 new Layer("lexical", "Lexical", 0, true, false, false, "word", true),
	 new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true),
	 new Layer("phone", "Phone", 2, true, false, true, "word", true));
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "test_utterance_word_phone.eaf")) };
      
      // create deserializer
      EAFSerialization deserializer = new EAFSerialization();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(10, deserializer.configure(configuration, schema).size());
      assertEquals("comment", "comment", 
		   ((Layer)configuration.get("commentLayer").getValue()).getId());
      assertEquals("pronounce", "pronounce", 
		   ((Layer)configuration.get("pronounceLayer").getValue()).getId());
      assertEquals("lexical", "lexical", 
		   ((Layer)configuration.get("lexicalLayer").getValue()).getId());
      assertEquals("noise", "noise", 
		   ((Layer)configuration.get("noiseLayer").getValue()).getId());
      assertEquals("author", "scribe", 
		   ((Layer)configuration.get("authorLayer").getValue()).getId());
      assertEquals("version_date", "version_date", 
		   ((Layer)configuration.get("dateLayer").getValue()).getId());
      assertEquals("language", "lang", 
		   ((Layer)configuration.get("languageLayer").getValue()).getId());
      assertEquals("useConventions", Boolean.TRUE, 
		   (Boolean)configuration.get("useConventions").getValue());
      assertEquals("ignoreBlankAnnotations", Boolean.TRUE, 
		   (Boolean)configuration.get("ignoreBlankAnnotations").getValue());
      assertEquals("minimumTurnPauseLength", Double.valueOf(0.0), 
		   (Double)configuration.get("minimumTurnPauseLength").getValue());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
      assertEquals(6, defaultParamaters.size());
      assertEquals("utterance mapping", "utterance", 
		   ((Layer)defaultParamaters.get("tier0").getValue()).getId());
      assertEquals("utterance mapping", "utterance", 
		   ((Layer)defaultParamaters.get("tier1").getValue()).getId());
      assertEquals("word mapping", "word", 
		   ((Layer)defaultParamaters.get("tier2").getValue()).getId());
      assertEquals("word mapping", "word", 
		   ((Layer)defaultParamaters.get("tier3").getValue()).getId());

      // phones tiers doesn't automatically map to phone layer, because their names don't match
      assertNull("phone mapping default",
                 defaultParamaters.get("tier4").getValue());
      assertNull("phone mapping default",
                 defaultParamaters.get("tier5").getValue());
      // but we set it 'manually'
      defaultParamaters.get("tier4").setValue(schema.getLayer("phone"));
      defaultParamaters.get("tier5").setValue(schema.getLayer("phone"));

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);
      
      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }
      
      assertEquals("test_utterance_word_phone.eaf", g.getId());

      // attributes
      assertNull("no transcriber", g.first("scribe"));
      assertNull("no language specified", g.first("lang"));
      assertEquals("version date", "2017-03-16T11:45:39-03:00", g.first("version_date").getLabel());

      // participants     
      Annotation[] who = g.all("who");
      assertEquals(2, who.length);
      assertEquals("interviewer", who[0].getLabel());
      assertEquals(g, who[0].getParent());
      assertEquals("participant", who[1].getLabel());
      assertEquals(g, who[1].getParent());
      
      // turns
      Annotation[] turns = g.all("turn");
      assertEquals(12, turns.length);
      assertEquals(Double.valueOf(4.675), turns[0].getStart().getOffset());
      assertEquals(Double.valueOf(14.889000000000001), turns[0].getEnd().getOffset());
      assertEquals("participant", turns[0].getLabel());
      assertEquals(who[1], turns[0].getParent());
      
      assertEquals(Double.valueOf(14.889000000000001), turns[1].getStart().getOffset());
      assertEquals(Double.valueOf(23.170), turns[1].getEnd().getOffset());
      assertEquals("interviewer", turns[1].getLabel());
      assertEquals(who[0], turns[1].getParent());

      assertEquals(Double.valueOf(17.983), turns[2].getStart().getOffset());
      assertEquals(Double.valueOf(140.366), turns[2].getEnd().getOffset());
      assertEquals("participant", turns[2].getLabel());

      assertEquals(Double.valueOf(140.366), turns[3].getStart().getOffset());
      assertEquals(Double.valueOf(159.457), turns[3].getEnd().getOffset());
      assertEquals("interviewer", turns[3].getLabel());

      assertEquals(Double.valueOf(159.457), turns[4].getStart().getOffset());
      assertEquals(Double.valueOf(282.871), turns[4].getEnd().getOffset());
      assertEquals("participant", turns[4].getLabel());

      assertEquals(Double.valueOf(282.871), turns[5].getStart().getOffset());
      assertEquals(Double.valueOf(283.96500000000003), turns[5].getEnd().getOffset());
      assertEquals("interviewer", turns[5].getLabel());

      assertEquals(Double.valueOf(283.96500000000003), turns[6].getStart().getOffset());
      assertEquals(Double.valueOf(310.60200000000003), turns[6].getEnd().getOffset());
      assertEquals("participant", turns[6].getLabel());

      assertEquals("simultaneous speech",
		   Double.valueOf(284.84000000000003), turns[7].getStart().getOffset());
      assertEquals("simultaneous speech",
		   Double.valueOf(285.34000000000003), turns[7].getEnd().getOffset());
      assertEquals("interviewer", turns[7].getLabel());

      assertEquals(Double.valueOf(310.60200000000003), turns[8].getStart().getOffset());
      assertEquals(Double.valueOf(311.071), turns[8].getEnd().getOffset());
      assertEquals("interviewer", turns[8].getLabel());

      assertEquals(Double.valueOf(311.071), turns[9].getStart().getOffset());
      assertEquals(Double.valueOf(316.258), turns[9].getEnd().getOffset());
      assertEquals("participant", turns[9].getLabel());

      assertEquals(Double.valueOf(316.258), turns[10].getStart().getOffset());
      assertEquals(Double.valueOf(317.195), turns[10].getEnd().getOffset());
      assertEquals("interviewer", turns[10].getLabel());

      assertEquals(Double.valueOf(317.195), turns[11].getStart().getOffset());
      assertEquals(Double.valueOf(321.240), turns[11].getEnd().getOffset());
      assertEquals("participant", turns[11].getLabel());

      // utterances
      Annotation[] utterances = g.all("utterance");
      assertEquals(148, utterances.length);
      assertEquals(Double.valueOf(4.675), utterances[0].getStart().getOffset());
      assertEquals(Double.valueOf(6.752), utterances[0].getEnd().getOffset());
      assertEquals("participant", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals(Double.valueOf(6.752), utterances[1].getStart().getOffset());
      assertEquals(Double.valueOf(10.515), utterances[1].getEnd().getOffset());
      assertEquals("participant", utterances[1].getParent().getLabel());
      assertEquals(turns[0], utterances[1].getParent());

      assertEquals(Double.valueOf(14.889000000000001), utterances[4].getStart().getOffset());
      assertEquals(Double.valueOf(15.639000000000001), utterances[4].getEnd().getOffset());
      assertEquals("interviewer", utterances[4].getParent().getLabel());
      assertEquals(turns[1], utterances[4].getParent());

      Annotation[] words = g.all("word");
      String[] wordLabels = {
	 "  . rest", "of", "that", "side", "of", "the", "family", "so", "he -- ",
	 " generously", "agreed", "that", "she", "could", "go", "with", "him", "but ", 
	 " there", "were", "so", "many", "people", "there ", 
	 " that", "nothing", "was", "done", "constructively "
      };
      Double[] wordStarts = {
	 4.675, 4.917, 5.074, 5.279, 5.539, 5.611, 5.665, 6.112, 6.414,
	 6.752, 8.512, 8.872, 9.077, 9.222, 9.412, 9.662, 9.962, 10.202, 
	 10.515, 10.791, 11.067, 11.343, 11.619, 11.895, 
	 12.171, 12.451, 12.811, 13.031, 13.346
      };
      Double[] wordEnds = {
	 4.917, 5.074, 5.279, 5.539, 5.611, 5.665, 6.112, 6.414, 6.752, 
	 8.512, 8.872, 9.077, 9.222, 9.412, 9.662, 9.902000000000001, 10.202, 10.515,
	 10.791, 11.067, 11.343, 11.619, 11.895, 12.171,
	 12.411, 12.811, 12.991, 13.346, 14.889000000000001
      };
      for (int i = 0; i < wordLabels.length; i++) {
	 assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
	 assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
	 	      i+1, words[i].getOrdinal());
	 assertEquals("word starts " + i + " " + wordLabels[i],
		      wordStarts[i], words[i].getStart().getOffset());
	 assertEquals("word ends " + i + " " + wordLabels[i],
		      wordEnds[i], words[i].getEnd().getOffset());
	 assertEquals(turns[0].getId(), words[i].getParentId());
      }

      // comment
      Annotation[] comments = g.all("comment");
      assertEquals(0, comments.length);

      // noise
      Annotation[] noises = g.all("noise");
      assertEquals(0, noises.length);

      // pronounce
      Annotation[] pronounce = g.all("pronounce");
      assertEquals(0, pronounce.length);

      // lexical
      Annotation[] lexical = g.all("lexical");
      assertEquals(0, lexical.length);
      
      // check all annotations have 'manual' confidence
      for (Annotation a : g.getAnnotationsById().values()) {
         assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }
   }

   /**
    * This tests that it's possible to deserialize without reference to a turn/utternance/word hierarchy.
    * In this case the utterances are simple 'freeform' annotations that are not tokenized.
    */
   @Test public void freeform_keep_empty_utterances()  throws Exception {
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("scribe", "Author", 0, true, true, true),
	 new Layer("version_date", "Date", 0, true, true, true),
	 new Layer("lang", "Language", 0, true, true, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("i", "Interviewer", 2, true, false, true),
	 new Layer("p", "Participant", 2, true, false, true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true));
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "test_utterance.eaf")) };
      
      // create deserializer
      EAFSerialization deserializer = new EAFSerialization();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(10, configuration.size());
      assertNull("comment", 
		 configuration.get("commentLayer").getValue());
      assertNull("pronounce",
		   configuration.get("pronounceLayer").getValue());
      assertNull("lexical",
		 configuration.get("lexicalLayer").getValue());
      assertNull("noise",
		 configuration.get("noiseLayer").getValue());
      assertEquals("author", "scribe", 
		   ((Layer)configuration.get("authorLayer").getValue()).getId());
      assertEquals("version_date", "version_date", 
		   ((Layer)configuration.get("dateLayer").getValue()).getId());
      assertEquals("language", "lang", 
		   ((Layer)configuration.get("languageLayer").getValue()).getId());
      assertEquals("useConventions", Boolean.TRUE, 
		   (Boolean)configuration.get("useConventions").getValue());
      assertEquals("ignoreBlankAnnotations", Boolean.TRUE, 
		   (Boolean)configuration.get("ignoreBlankAnnotations").getValue());
      assertEquals("minimumTurnPauseLength", Double.valueOf(0.0), 
		   (Double)configuration.get("minimumTurnPauseLength").getValue());
      
      configuration.get("useConventions").setValue(Boolean.FALSE);
      configuration.get("ignoreBlankAnnotations").setValue(Boolean.FALSE);
      assertEquals(10, deserializer.configure(configuration, schema).size());
      assertEquals("customize useConventions", Boolean.FALSE, 
		   deserializer.getUseConventions());
      assertEquals("customize ignoreBlankAnnotations", Boolean.FALSE, 
		   deserializer.getIgnoreBlankAnnotations());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
      assertEquals(2, defaultParamaters.size());
      assertEquals("utterance mapping", "i", 
		   ((Layer)defaultParamaters.get("tier0").getValue()).getId());
      assertEquals("utterance mapping", "p", 
		   ((Layer)defaultParamaters.get("tier1").getValue()).getId());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);
      
      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }
      
      assertEquals("test_utterance.eaf", g.getId());

      // attributes
      assertEquals("transcriber", "Robert", g.first("scribe").getLabel());
      assertEquals("language", "eng", g.first("lang").getLabel());
      assertEquals("version date", "2017-08-28T16:48:05-03:00", g.first("version_date").getLabel());

      // participants     
      assertEquals(0, g.all("who").length);
      
      // turns
      assertEquals(0, g.all("turn").length);
      
      // interviewer
      Annotation[] utterances = g.all("i");
      assertEquals(17, utterances.length);

      assertEquals(Double.valueOf(14.889000000000001), utterances[0].getStart().getOffset());
      assertEquals(Double.valueOf(15.639000000000001), utterances[0].getEnd().getOffset());
      assertEquals(" right - ", utterances[0].getLabel());
      assertEquals(g, utterances[0].getParent());

      // participant
      utterances = g.all("p");
      assertEquals(131, utterances.length);

      assertEquals(Double.valueOf(4.675), utterances[0].getStart().getOffset());
      assertEquals(Double.valueOf(6.752), utterances[0].getEnd().getOffset());
      assertEquals("  . rest of that side of the famly[f{mli](family) so he -- ", utterances[0].getLabel());
      assertEquals(g, utterances[0].getParent());
      
      // check all annotations have 'manual' confidence
      for (Annotation a : g.getAnnotationsById().values()) {
         assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }
   }

   /**
    * This tests that it's possible to deserialize without reference to a turn/utternance/word hierarchy.
    * In this case the utterances are simple 'freeform' annotations that are not tokenized.
    */
   @Test public void freeform_ignore_empty_utterances()  throws Exception {
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("scribe", "Author", 0, true, true, true),
	 new Layer("version_date", "Date", 0, true, true, true),
	 new Layer("lang", "Language", 0, true, true, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("i", "Interviewer", 2, true, false, true),
	 new Layer("p", "Participant", 2, true, false, true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true));
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "test_utterance.eaf")) };
      
      // create deserializer
      EAFSerialization deserializer = new EAFSerialization();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(10, configuration.size());
      assertNull("comment", 
		 configuration.get("commentLayer").getValue());
      assertNull("pronounce",
		   configuration.get("pronounceLayer").getValue());
      assertNull("lexical",
		 configuration.get("lexicalLayer").getValue());
      assertNull("noise",
		 configuration.get("noiseLayer").getValue());
      assertEquals("author", "scribe", 
		   ((Layer)configuration.get("authorLayer").getValue()).getId());
      assertEquals("version_date", "version_date", 
		   ((Layer)configuration.get("dateLayer").getValue()).getId());
      assertEquals("language", "lang", 
		   ((Layer)configuration.get("languageLayer").getValue()).getId());
      assertEquals("useConventions", Boolean.TRUE, 
		   (Boolean)configuration.get("useConventions").getValue());
      assertEquals("ignoreBlankAnnotations", Boolean.TRUE, 
		   (Boolean)configuration.get("ignoreBlankAnnotations").getValue());
      assertEquals("minimumTurnPauseLength", Double.valueOf(0.0), 
		   (Double)configuration.get("minimumTurnPauseLength").getValue());
      
      configuration.get("useConventions").setValue(Boolean.FALSE);
      assertEquals(10, deserializer.configure(configuration, schema).size());
      assertEquals("customize useConventions", Boolean.FALSE, 
		   deserializer.getUseConventions());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
      assertEquals(2, defaultParamaters.size());
      assertEquals("utterance mapping", "i", 
		   ((Layer)defaultParamaters.get("tier0").getValue()).getId());
      assertEquals("utterance mapping", "p", 
		   ((Layer)defaultParamaters.get("tier1").getValue()).getId());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);
      
      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }
      
      assertEquals("test_utterance.eaf", g.getId());

      // attributes
      assertEquals("transcriber", "Robert", g.first("scribe").getLabel());
      assertEquals("language", "eng", g.first("lang").getLabel());
      assertEquals("version date", "2017-08-28T16:48:05-03:00", g.first("version_date").getLabel());

      // participants     
      assertEquals(0, g.all("who").length);
      
      // turns
      assertEquals(0, g.all("turn").length);
      
      // interviewer
      Annotation[] utterances = g.all("i");
      assertEquals(17, utterances.length);

      assertEquals(Double.valueOf(14.889000000000001), utterances[0].getStart().getOffset());
      assertEquals(Double.valueOf(15.639000000000001), utterances[0].getEnd().getOffset());
      assertEquals(" right - ", utterances[0].getLabel());
      assertEquals(g, utterances[0].getParent());

      // participant
      utterances = g.all("p");
      assertEquals(130, utterances.length);

      assertEquals(Double.valueOf(4.675), utterances[0].getStart().getOffset());
      assertEquals(Double.valueOf(6.752), utterances[0].getEnd().getOffset());
      assertEquals("  . rest of that side of the famly[f{mli](family) so he -- ", utterances[0].getLabel());
      assertEquals(g, utterances[0].getParent());
      
      // check all annotations have 'manual' confidence
      for (Annotation a : g.getAnnotationsById().values()) {
         assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

   }

   @Test public void basicSerialization() throws Exception {
      Schema schema = new Schema(
         "who", "turn", "utterance", "word",
         new Layer("version_date", "Date")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true),
         new Layer("scribe", "Transcriber")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true),
         new Layer("lang", "Language")
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
         .setId("simultaneous_speech")
         .setSchema(schema);
      graph.addAnchor(new Anchor("a0", 0.0));
      graph.addAnchor(new Anchor("a15", 15.0));
      // language
      graph.addAnnotation(new Annotation("l", "en-NZ", "lang", "a0", "a15"));
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

      // add some comments, noises, lexical and pronounce tags
      // conventions are not (currently) reversed when serializing (i.e. they're only
      // supported during deserialization)
      graph.addAnnotation(new Annotation("comment1", "preamble", "comment", "a0", "a1"));
      graph.addAnnotation(new Annotation("noise1", "throat-clear", "noise", "a4", "a5"));
      graph.addAnnotation(new Annotation("lex1", "lex-1-6", "lexical", "a6", "a7", "w1-6"));
      graph.addAnnotation(new Annotation("lex2", "lex-1-7", "lexical", "a7", "a8", "w1-7"));
      graph.addAnnotation(new Annotation("pron1", "pron-1-7", "pronounce", "a7", "a8", "w1-7"));
      graph.addAnnotation(new Annotation("pron2", "pron-1-8", "pronounce", "a8", "a9", "w1-7"));

      // add a media handler to test MEDIA_DESCRIPTOR
      graph.setMediaProvider(new GraphMediaProvider() {
            public MediaFile[] getAvailableMedia() throws StoreException, PermissionException {
               MediaFile[] media = {
                  new MediaFile().setMimeType("audio/wav").setUrl("file://test.wav")
               };
               return media;
            }
            public String getMedia(String trackSuffix, String mimeType)
               throws StoreException, PermissionException
            { return "file://test.wav"; }
            public GraphMediaProvider providerForGraph(Graph graph)
            { return this; }
         });  

      // add orthography tags that should not be used because orthography is not selected
      for (Annotation word : graph.all("word")) {
         graph.addTag(word, "orthography", word.getLabel()+"-orthography");
      }
      
      // create serializer
      EAFSerialization serializer = new EAFSerialization();
      
      // general configuration
      ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(10, serializer.configure(configuration, schema).size());
      assertEquals("comment", "comment", 
		   ((Layer)configuration.get("commentLayer").getValue()).getId());
      assertEquals("pronounce", "pronounce", 
		   ((Layer)configuration.get("pronounceLayer").getValue()).getId());
      assertEquals("lexical", "lexical", 
		   ((Layer)configuration.get("lexicalLayer").getValue()).getId());
      assertEquals("noise", "noise", 
		   ((Layer)configuration.get("noiseLayer").getValue()).getId());
      assertEquals("author", "scribe", 
		   ((Layer)configuration.get("authorLayer").getValue()).getId());
      assertEquals("version_date", "version_date", 
		   ((Layer)configuration.get("dateLayer").getValue()).getId());
      assertEquals("language", "lang", 
		   ((Layer)configuration.get("languageLayer").getValue()).getId());
      assertEquals("useConventions", Boolean.TRUE, 
		   (Boolean)configuration.get("useConventions").getValue());
      assertEquals("ignoreBlankAnnotations", Boolean.TRUE, 
		   (Boolean)configuration.get("ignoreBlankAnnotations").getValue());
      assertEquals("minimumTurnPauseLength", Double.valueOf(0.0), 
		   (Double)configuration.get("minimumTurnPauseLength").getValue());

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
      String[] layers = {"word"}; 
      serializer.serialize(Utility.OneGraphSpliterator(graph), layers,
                           stream -> streams.add(stream),
                           warning -> System.out.println(warning),
                           exception -> exceptions.add(exception));
      if (exceptions.size() > 0) fail(""+exceptions);

      streams.elementAt(0).save(dir);

      // test using diff
      File result = new File(dir, graph.getId() + ".eaf");
      String differences = diff(new File(dir, "expected_" + graph.getId() + ".eaf"), result);
      if (differences != null) {
         fail(differences);
      } else {
         result.delete();
      }
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
      EAFSerialization serializer = new EAFSerialization();
      
      // general configuration
      ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(10, serializer.configure(configuration, schema).size());
      assertEquals("comment", "comment", 
		   ((Layer)configuration.get("commentLayer").getValue()).getId());
      assertEquals("pronounce", "pronounce", 
		   ((Layer)configuration.get("pronounceLayer").getValue()).getId());
      assertEquals("lexical", "lexical", 
		   ((Layer)configuration.get("lexicalLayer").getValue()).getId());
      assertEquals("noise", "noise", 
		   ((Layer)configuration.get("noiseLayer").getValue()).getId());
      assertEquals("author", "scribe", 
		   ((Layer)configuration.get("authorLayer").getValue()).getId());
      assertNull("version_date",
                 configuration.get("dateLayer").getValue());
      assertNull("language", 
                 configuration.get("languageLayer").getValue());
      assertEquals("useConventions", Boolean.TRUE, 
		   (Boolean)configuration.get("useConventions").getValue());
      assertEquals("ignoreBlankAnnotations", Boolean.TRUE, 
		   (Boolean)configuration.get("ignoreBlankAnnotations").getValue());
      assertEquals("minimumTurnPauseLength", Double.valueOf(0.0), 
		   (Double)configuration.get("minimumTurnPauseLength").getValue());
      
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
      String[] layers = {"word","scribe","utterance"}; 
      serializer.serialize(Arrays.spliterator(graphs), layers,
                           stream -> streams.add(stream),
                           warning -> System.out.println(warning),
                           exception -> exceptions.add(exception));
      if (exceptions.size() > 0) fail(""+exceptions);

      streams.elementAt(0).save(dir);

      // test using diff
      File result = new File(dir, "serialize_utterance_word.eaf");
      String differences = diff(new File(dir, "expected_serialize_utterance_word.eaf"), result);
      if (differences != null) {
         fail(differences);
      } else {
         result.delete();
      }
   }

   @Test public void serializeTags() throws Exception {
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
         new Layer("orthography", "Orthography", 1, false, false, true, "word", true),
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
      
      // change the ID
      graphs[0].setId("serialize_utterance_word_orthography");
      
      // create serializer
      EAFSerialization serializer = new EAFSerialization();
      
      // general configuration
      ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(10, serializer.configure(configuration, schema).size());
      assertEquals("comment", "comment", 
		   ((Layer)configuration.get("commentLayer").getValue()).getId());
      assertEquals("pronounce", "pronounce", 
		   ((Layer)configuration.get("pronounceLayer").getValue()).getId());
      assertEquals("lexical", "lexical", 
		   ((Layer)configuration.get("lexicalLayer").getValue()).getId());
      assertEquals("noise", "noise", 
		   ((Layer)configuration.get("noiseLayer").getValue()).getId());
      assertEquals("author", "scribe", 
		   ((Layer)configuration.get("authorLayer").getValue()).getId());
      assertNull("version_date",
                 configuration.get("dateLayer").getValue());
      assertNull("language", 
                 configuration.get("languageLayer").getValue());
      assertEquals("useConventions", Boolean.TRUE, 
		   (Boolean)configuration.get("useConventions").getValue());
      assertEquals("ignoreBlankAnnotations", Boolean.TRUE, 
		   (Boolean)configuration.get("ignoreBlankAnnotations").getValue());
      assertEquals("minimumTurnPauseLength", Double.valueOf(0.0), 
		   (Double)configuration.get("minimumTurnPauseLength").getValue());
      
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
      String[] layers = {"word","orthography","scribe","utterance"}; 
      serializer.serialize(Arrays.spliterator(graphs), layers,
                           stream -> streams.add(stream),
                           warning -> System.out.println(warning),
                           exception -> exceptions.add(exception));
      if (exceptions.size() > 0) fail(""+exceptions);

      streams.elementAt(0).save(dir);

      // test using diff
      File result = new File(dir, "serialize_utterance_word_orthography.eaf");
      String differences = diff(new File(dir, "expected_serialize_utterance_word_orthography.eaf"), result);
      if (differences != null) {
         fail(differences);
      } else {
         result.delete();
      }
   }

   @Test public void serializeNoWordTokens() throws Exception {
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

      // change the ID
      graphs[0].setId("serialize_utterance_no_word");
      
      // create serializer
      EAFSerialization serializer = new EAFSerialization();
      
      // general configuration
      ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(10, serializer.configure(configuration, schema).size());
      assertEquals("comment", "comment", 
		   ((Layer)configuration.get("commentLayer").getValue()).getId());
      assertEquals("pronounce", "pronounce", 
		   ((Layer)configuration.get("pronounceLayer").getValue()).getId());
      assertEquals("lexical", "lexical", 
		   ((Layer)configuration.get("lexicalLayer").getValue()).getId());
      assertEquals("noise", "noise", 
		   ((Layer)configuration.get("noiseLayer").getValue()).getId());
      assertEquals("author", "scribe", 
		   ((Layer)configuration.get("authorLayer").getValue()).getId());
      assertNull("version_date",
                 configuration.get("dateLayer").getValue());
      assertNull("language", 
                 configuration.get("languageLayer").getValue());
      assertEquals("useConventions", Boolean.TRUE, 
		   (Boolean)configuration.get("useConventions").getValue());
      assertEquals("ignoreBlankAnnotations", Boolean.TRUE, 
		   (Boolean)configuration.get("ignoreBlankAnnotations").getValue());
      assertEquals("minimumTurnPauseLength", Double.valueOf(0.0), 
		   (Double)configuration.get("minimumTurnPauseLength").getValue());
      
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
      String[] layers = {"utterance"}; 
      serializer.serialize(Arrays.spliterator(graphs), layers,
                           stream -> streams.add(stream),
                           warning -> System.out.println(warning),
                           exception -> exceptions.add(exception));
      if (exceptions.size() > 0) fail(""+exceptions);

      streams.elementAt(0).save(dir);

      // test using diff
      File result = new File(dir, "serialize_utterance_no_word.eaf");
      String differences = diff(new File(dir, "expected_serialize_utterance_no_word.eaf"), result);
      if (differences != null) {
         fail(differences);
      } else {
         result.delete();
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
      org.junit.runner.JUnitCore.main("nzilbb.formatter.elan.TestEAFSerialization");
   }
}
