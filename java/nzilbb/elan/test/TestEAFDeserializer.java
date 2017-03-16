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

package nzilbb.elan.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.SortedSet;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.io.File;
import java.net.URL;
import nzilbb.configure.ParameterSet;
import nzilbb.configure.Parameter;
import nzilbb.ag.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.elan.*;

public class TestEAFDeserializer
{
   @Test public void utterance() 
      throws Exception
   {
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
      EAFDeserializer deserializer = new EAFDeserializer();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(8, deserializer.configure(configuration, schema).size());
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
      assertEquals("transcriber", "Robert", g.my("scribe").getLabel());
      assertEquals("language", "eng", g.my("lang").getLabel());
      assertEquals("version date", "2017-03-16T10:58:10-03:00", g.my("version_date").getLabel());

      // participants     
      Annotation[] who = g.list("who");
      assertEquals(2, who.length);
      assertEquals("interviewer", who[0].getLabel());
      assertEquals(g, who[0].getParent());
      assertEquals("participant", who[1].getLabel());
      assertEquals(g, who[1].getParent());
      
      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(12, turns.length);
      assertEquals(new Double(4.675), turns[0].getStart().getOffset());
      assertEquals(new Double(14.889000000000001), turns[0].getEnd().getOffset());
      assertEquals("participant", turns[0].getLabel());
      assertEquals(who[1], turns[0].getParent());
      
      assertEquals(new Double(14.889000000000001), turns[1].getStart().getOffset());
      assertEquals(new Double(23.170), turns[1].getEnd().getOffset());
      assertEquals("interviewer", turns[1].getLabel());
      assertEquals(who[0], turns[1].getParent());

      assertEquals(new Double(17.983), turns[2].getStart().getOffset());
      assertEquals(new Double(140.366), turns[2].getEnd().getOffset());
      assertEquals("participant", turns[2].getLabel());

      assertEquals(new Double(140.366), turns[3].getStart().getOffset());
      assertEquals(new Double(159.457), turns[3].getEnd().getOffset());
      assertEquals("interviewer", turns[3].getLabel());

      assertEquals(new Double(159.457), turns[4].getStart().getOffset());
      assertEquals(new Double(282.871), turns[4].getEnd().getOffset());
      assertEquals("participant", turns[4].getLabel());

      assertEquals(new Double(282.871), turns[5].getStart().getOffset());
      assertEquals(new Double(283.96500000000003), turns[5].getEnd().getOffset());
      assertEquals("interviewer", turns[5].getLabel());

      assertEquals(new Double(283.96500000000003), turns[6].getStart().getOffset());
      assertEquals(new Double(310.60200000000003), turns[6].getEnd().getOffset());
      assertEquals("participant", turns[6].getLabel());

      assertEquals("simultaneous speech",
		   new Double(284.84000000000003), turns[7].getStart().getOffset());
      assertEquals("simultaneous speech",
		   new Double(285.34000000000003), turns[7].getEnd().getOffset());
      assertEquals("interviewer", turns[7].getLabel());

      assertEquals(new Double(310.60200000000003), turns[8].getStart().getOffset());
      assertEquals(new Double(311.071), turns[8].getEnd().getOffset());
      assertEquals("interviewer", turns[8].getLabel());

      assertEquals(new Double(311.071), turns[9].getStart().getOffset());
      assertEquals(new Double(316.258), turns[9].getEnd().getOffset());
      assertEquals("participant", turns[9].getLabel());

      assertEquals(new Double(316.258), turns[10].getStart().getOffset());
      assertEquals(new Double(317.195), turns[10].getEnd().getOffset());
      assertEquals("interviewer", turns[10].getLabel());

      assertEquals(new Double(317.195), turns[11].getStart().getOffset());
      assertEquals(new Double(321.240), turns[11].getEnd().getOffset());
      assertEquals("participant", turns[11].getLabel());

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(148, utterances.length);
      assertEquals(new Double(4.675), utterances[0].getStart().getOffset());
      assertEquals(new Double(6.752), utterances[0].getEnd().getOffset());
      assertEquals("participant", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals(new Double(6.752), utterances[1].getStart().getOffset());
      assertEquals(new Double(10.515), utterances[1].getEnd().getOffset());
      assertEquals("participant", utterances[1].getParent().getLabel());
      assertEquals(turns[0], utterances[1].getParent());

      assertEquals(new Double(14.889000000000001), utterances[4].getStart().getOffset());
      assertEquals(new Double(15.639000000000001), utterances[4].getEnd().getOffset());
      assertEquals("interviewer", utterances[4].getParent().getLabel());
      assertEquals(turns[1], utterances[4].getParent());

      Annotation[] words = g.list("word");
      String[] wordLabels = {
	 ". rest", "of", "that", "side", "of", "the", "family", "so", "he --",
	 "generously", "agreed", "that", "she", "could", "go", "with", "him", "but", 
	 "there", "were", "so", "many", "people", "there", 
	 "that", "nothing", "was", "done", "constructively"
      };
      for (int i = 0; i < wordLabels.length; i++)
      {
	 assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
	 assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
	 	      i+1, words[i].getOrdinal());
	 assertEquals(turns[0].getId(), words[i].getParentId());
      }

      // comment
      Annotation[] comments = g.list("comment");
      assertEquals("unclear", comments[0].getLabel());
      assertEquals("whatever", comments[0].getStart().endOf("word").iterator().next().getLabel());
      assertEquals("and", comments[0].getEnd().startOf("word").iterator().next().getLabel());

      assertEquals(1, comments.length);

      // noise
      Annotation[] noises = g.list("noise");
      assertEquals("click", noises[0].getLabel());
      assertEquals("um --", noises[0].getStart().endOf("word").iterator().next().getLabel());
      assertEquals(new Double(132.992), noises[0].getEnd().getOffset());

      assertEquals(1, noises.length);

      // pronounce
      Annotation[] pronounce = g.list("pronounce");
      assertEquals("f{mli", pronounce[0].getLabel());
      assertTrue(pronounce[0].tags(words[6]));
      assertEquals(1, pronounce.length);

      // lexical
      Annotation[] lexical = g.list("lexical");
      assertEquals("agrees", lexical[0].getLabel());
      assertEquals("agreed", lexical[0].my("word").getLabel());
      assertEquals(1, lexical.length);
   }

   @Test public void utterance_word() 
      throws Exception
   {
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
      EAFDeserializer deserializer = new EAFDeserializer();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(8, deserializer.configure(configuration, schema).size());
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

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
      assertEquals(6, defaultParamaters.size());
      assertEquals("utterance mapping", "utterance", 
		   ((Layer)defaultParamaters.get("tier0").getValue()).getId());
      assertEquals("utterance mapping", "utterance", 
		   ((Layer)defaultParamaters.get("tier1").getValue()).getId());
      assertEquals("noise mapping", "noise", 
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

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("test_utterance_word.eaf", g.getId());

      // attributes
      assertNull("no transcriber", g.my("scribe"));
      assertNull("no language specified", g.my("lang"));
      assertEquals("version date", "2017-03-16T11:20:04-03:00", g.my("version_date").getLabel());

      // participants     
      Annotation[] who = g.list("who");
      assertEquals(2, who.length);
      assertEquals("interviewer", who[0].getLabel());
      assertEquals(g, who[0].getParent());
      assertEquals("participant", who[1].getLabel());
      assertEquals(g, who[1].getParent());
      
      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(12, turns.length);
      assertEquals(new Double(4.675), turns[0].getStart().getOffset());
      assertEquals(new Double(14.889000000000001), turns[0].getEnd().getOffset());
      assertEquals("participant", turns[0].getLabel());
      assertEquals(who[1], turns[0].getParent());
      
      assertEquals(new Double(14.889000000000001), turns[1].getStart().getOffset());
      assertEquals(new Double(23.170), turns[1].getEnd().getOffset());
      assertEquals("interviewer", turns[1].getLabel());
      assertEquals(who[0], turns[1].getParent());

      assertEquals(new Double(17.983), turns[2].getStart().getOffset());
      assertEquals(new Double(140.366), turns[2].getEnd().getOffset());
      assertEquals("participant", turns[2].getLabel());

      assertEquals(new Double(140.366), turns[3].getStart().getOffset());
      assertEquals(new Double(159.457), turns[3].getEnd().getOffset());
      assertEquals("interviewer", turns[3].getLabel());

      assertEquals(new Double(159.457), turns[4].getStart().getOffset());
      assertEquals(new Double(282.871), turns[4].getEnd().getOffset());
      assertEquals("participant", turns[4].getLabel());

      assertEquals(new Double(282.871), turns[5].getStart().getOffset());
      assertEquals(new Double(283.96500000000003), turns[5].getEnd().getOffset());
      assertEquals("interviewer", turns[5].getLabel());

      assertEquals(new Double(283.96500000000003), turns[6].getStart().getOffset());
      assertEquals(new Double(310.60200000000003), turns[6].getEnd().getOffset());
      assertEquals("participant", turns[6].getLabel());

      assertEquals("simultaneous speech",
		   new Double(284.84000000000003), turns[7].getStart().getOffset());
      assertEquals("simultaneous speech",
		   new Double(285.34000000000003), turns[7].getEnd().getOffset());
      assertEquals("interviewer", turns[7].getLabel());

      assertEquals(new Double(310.60200000000003), turns[8].getStart().getOffset());
      assertEquals(new Double(311.071), turns[8].getEnd().getOffset());
      assertEquals("interviewer", turns[8].getLabel());

      assertEquals(new Double(311.071), turns[9].getStart().getOffset());
      assertEquals(new Double(316.258), turns[9].getEnd().getOffset());
      assertEquals("participant", turns[9].getLabel());

      assertEquals(new Double(316.258), turns[10].getStart().getOffset());
      assertEquals(new Double(317.195), turns[10].getEnd().getOffset());
      assertEquals("interviewer", turns[10].getLabel());

      assertEquals(new Double(317.195), turns[11].getStart().getOffset());
      assertEquals(new Double(321.240), turns[11].getEnd().getOffset());
      assertEquals("participant", turns[11].getLabel());

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(148, utterances.length);
      assertEquals(new Double(4.675), utterances[0].getStart().getOffset());
      assertEquals(new Double(6.752), utterances[0].getEnd().getOffset());
      assertEquals("participant", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals(new Double(6.752), utterances[1].getStart().getOffset());
      assertEquals(new Double(10.515), utterances[1].getEnd().getOffset());
      assertEquals("participant", utterances[1].getParent().getLabel());
      assertEquals(turns[0], utterances[1].getParent());

      assertEquals(new Double(14.889000000000001), utterances[4].getStart().getOffset());
      assertEquals(new Double(15.639000000000001), utterances[4].getEnd().getOffset());
      assertEquals("interviewer", utterances[4].getParent().getLabel());
      assertEquals(turns[1], utterances[4].getParent());

      Annotation[] words = g.list("word");
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
      for (int i = 0; i < wordLabels.length; i++)
      {
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
      Annotation[] comments = g.list("comment");
      assertEquals(1, comments.length);
      assertEquals("unclear", comments[0].getLabel());
      assertEquals(new Double(102.164), comments[0].getStart().getOffset());
      assertEquals(new Double(102.424), comments[0].getEnd().getOffset());

      // noise
      Annotation[] noises = g.list("noise");
      assertEquals(1, noises.length);
      assertEquals("click", noises[0].getLabel());
      assertEquals(new Double(129.612), noises[0].getStart().getOffset());
      assertEquals(new Double(130.242), noises[0].getEnd().getOffset());

      // pronounce
      Annotation[] pronounce = g.list("pronounce");
      assertEquals(0, pronounce.length);

      // lexical
      Annotation[] lexical = g.list("lexical");
      assertEquals(0, lexical.length);
   }

   @Test public void utterance_word_phone() 
      throws Exception
   {
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
      EAFDeserializer deserializer = new EAFDeserializer();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(8, deserializer.configure(configuration, schema).size());
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
      assertEquals("phone mapping default", "utterance", 
		   ((Layer)defaultParamaters.get("tier4").getValue()).getId());
      assertEquals("phone mapping default", "utterance", 
		   ((Layer)defaultParamaters.get("tier5").getValue()).getId());
      // but we set it 'manually'
      defaultParamaters.get("tier4").setValue(schema.getLayer("phone"));
      defaultParamaters.get("tier5").setValue(schema.getLayer("phone"));

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);
      
      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("test_utterance_word_phone.eaf", g.getId());

      // attributes
      assertNull("no transcriber", g.my("scribe"));
      assertNull("no language specified", g.my("lang"));
      assertEquals("version date", "2017-03-16T11:45:39-03:00", g.my("version_date").getLabel());

      // participants     
      Annotation[] who = g.list("who");
      assertEquals(2, who.length);
      assertEquals("interviewer", who[0].getLabel());
      assertEquals(g, who[0].getParent());
      assertEquals("participant", who[1].getLabel());
      assertEquals(g, who[1].getParent());
      
      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(12, turns.length);
      assertEquals(new Double(4.675), turns[0].getStart().getOffset());
      assertEquals(new Double(14.889000000000001), turns[0].getEnd().getOffset());
      assertEquals("participant", turns[0].getLabel());
      assertEquals(who[1], turns[0].getParent());
      
      assertEquals(new Double(14.889000000000001), turns[1].getStart().getOffset());
      assertEquals(new Double(23.170), turns[1].getEnd().getOffset());
      assertEquals("interviewer", turns[1].getLabel());
      assertEquals(who[0], turns[1].getParent());

      assertEquals(new Double(17.983), turns[2].getStart().getOffset());
      assertEquals(new Double(140.366), turns[2].getEnd().getOffset());
      assertEquals("participant", turns[2].getLabel());

      assertEquals(new Double(140.366), turns[3].getStart().getOffset());
      assertEquals(new Double(159.457), turns[3].getEnd().getOffset());
      assertEquals("interviewer", turns[3].getLabel());

      assertEquals(new Double(159.457), turns[4].getStart().getOffset());
      assertEquals(new Double(282.871), turns[4].getEnd().getOffset());
      assertEquals("participant", turns[4].getLabel());

      assertEquals(new Double(282.871), turns[5].getStart().getOffset());
      assertEquals(new Double(283.96500000000003), turns[5].getEnd().getOffset());
      assertEquals("interviewer", turns[5].getLabel());

      assertEquals(new Double(283.96500000000003), turns[6].getStart().getOffset());
      assertEquals(new Double(310.60200000000003), turns[6].getEnd().getOffset());
      assertEquals("participant", turns[6].getLabel());

      assertEquals("simultaneous speech",
		   new Double(284.84000000000003), turns[7].getStart().getOffset());
      assertEquals("simultaneous speech",
		   new Double(285.34000000000003), turns[7].getEnd().getOffset());
      assertEquals("interviewer", turns[7].getLabel());

      assertEquals(new Double(310.60200000000003), turns[8].getStart().getOffset());
      assertEquals(new Double(311.071), turns[8].getEnd().getOffset());
      assertEquals("interviewer", turns[8].getLabel());

      assertEquals(new Double(311.071), turns[9].getStart().getOffset());
      assertEquals(new Double(316.258), turns[9].getEnd().getOffset());
      assertEquals("participant", turns[9].getLabel());

      assertEquals(new Double(316.258), turns[10].getStart().getOffset());
      assertEquals(new Double(317.195), turns[10].getEnd().getOffset());
      assertEquals("interviewer", turns[10].getLabel());

      assertEquals(new Double(317.195), turns[11].getStart().getOffset());
      assertEquals(new Double(321.240), turns[11].getEnd().getOffset());
      assertEquals("participant", turns[11].getLabel());

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(148, utterances.length);
      assertEquals(new Double(4.675), utterances[0].getStart().getOffset());
      assertEquals(new Double(6.752), utterances[0].getEnd().getOffset());
      assertEquals("participant", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals(new Double(6.752), utterances[1].getStart().getOffset());
      assertEquals(new Double(10.515), utterances[1].getEnd().getOffset());
      assertEquals("participant", utterances[1].getParent().getLabel());
      assertEquals(turns[0], utterances[1].getParent());

      assertEquals(new Double(14.889000000000001), utterances[4].getStart().getOffset());
      assertEquals(new Double(15.639000000000001), utterances[4].getEnd().getOffset());
      assertEquals("interviewer", utterances[4].getParent().getLabel());
      assertEquals(turns[1], utterances[4].getParent());

      Annotation[] words = g.list("word");
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
      for (int i = 0; i < wordLabels.length; i++)
      {
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
      Annotation[] comments = g.list("comment");
      assertEquals(0, comments.length);

      // noise
      Annotation[] noises = g.list("noise");
      assertEquals(0, noises.length);

      // pronounce
      Annotation[] pronounce = g.list("pronounce");
      assertEquals(0, pronounce.length);

      // lexical
      Annotation[] lexical = g.list("lexical");
      assertEquals(0, lexical.length);
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
      org.junit.runner.JUnitCore.main("nzilbb.praat.test.TestEAFDeserializer");
   }
}
