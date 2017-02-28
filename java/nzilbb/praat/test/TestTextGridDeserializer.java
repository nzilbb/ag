//
// Copyright 2015-2016 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.praat.test;
	      
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
import nzilbb.praat.*;

public class TestTextGridDeserializer
{
   @Test public void utterance() 
      throws Exception
   {
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
      TextGridDeserializer deserializer = new TextGridDeserializer();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(6, deserializer.configure(configuration, schema).size());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      //for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
      assertEquals(2, defaultParamaters.size());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);
      
      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("test_utterance.TextGrid", g.getId());

      // participants     
      Annotation[] who = g.list("who");
      assertEquals(2, who.length);
      assertEquals("participant", who[0].getLabel());
      assertEquals(g, who[0].getParent());
      assertEquals("interviewer", who[1].getLabel());
      assertEquals(g, who[1].getParent());
      
      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(20, turns.length);
      assertEquals(new Double(0.0), turns[0].getStart().getOffset());
      assertEquals(new Double(44.255), turns[0].getEnd().getOffset());
      assertEquals("participant", turns[0].getLabel());
      assertEquals(who[0], turns[0].getParent());
      
      assertEquals(new Double(44.255), turns[1].getStart().getOffset());
      assertEquals(new Double(45.505), turns[1].getEnd().getOffset());
      assertEquals("interviewer", turns[1].getLabel());
      assertEquals(who[1], turns[1].getParent());

      assertEquals(new Double(45.505), turns[2].getStart().getOffset());
      assertEquals(new Double(107.804), turns[2].getEnd().getOffset());
      assertEquals("participant", turns[2].getLabel());

      assertEquals(new Double(107.804), turns[3].getStart().getOffset());
      assertEquals(new Double(110.335), turns[3].getEnd().getOffset());
      assertEquals("interviewer", turns[3].getLabel());

      assertEquals(new Double(110.335), turns[4].getStart().getOffset());
      assertEquals(new Double(181.652), turns[4].getEnd().getOffset());
      assertEquals("participant", turns[4].getLabel());

      assertEquals(new Double(181.652), turns[5].getStart().getOffset());
      assertEquals(new Double(183.995), turns[5].getEnd().getOffset());
      assertEquals("interviewer", turns[5].getLabel());

      assertEquals(new Double(183.995), turns[6].getStart().getOffset());
      assertEquals(new Double(185.339), turns[6].getEnd().getOffset());
      assertEquals("participant", turns[6].getLabel());

      assertEquals(new Double(185.339), turns[7].getStart().getOffset());
      assertEquals(new Double(189.464), turns[7].getEnd().getOffset());
      assertEquals("interviewer", turns[7].getLabel());

      assertEquals(new Double(189.464), turns[8].getStart().getOffset());
      assertEquals(new Double(191.682), turns[8].getEnd().getOffset());
      assertEquals("participant", turns[8].getLabel());

      assertEquals(new Double(191.682), turns[9].getStart().getOffset());
      assertEquals(new Double(197.181), turns[9].getEnd().getOffset());
      assertEquals("interviewer", turns[9].getLabel());

      assertEquals(new Double(197.181), turns[10].getStart().getOffset());
      assertEquals(new Double(199.213), turns[10].getEnd().getOffset());
      assertEquals("participant", turns[10].getLabel());

      assertEquals(new Double(199.213), turns[11].getStart().getOffset());
      assertEquals(new Double(205.415), turns[11].getEnd().getOffset());
      assertEquals("interviewer", turns[11].getLabel());

      // simultaneous speech
      // the textgrid has two itervals for this which have been joined into one turn
      assertEquals(new Double(205.415), turns[12].getStart().getOffset());
      assertEquals(new Double(220.696), turns[12].getEnd().getOffset());
      assertEquals("participant", turns[12].getLabel());
      // simultaneous speech
      assertEquals(new Double(214.822), turns[13].getStart().getOffset());
      assertEquals(new Double(218.29), turns[13].getEnd().getOffset());
      assertEquals("interviewer", turns[13].getLabel());

      assertEquals(new Double(220.696), turns[14].getStart().getOffset());
      assertEquals(new Double(223.227), turns[14].getEnd().getOffset());
      assertEquals("interviewer", turns[14].getLabel());

      assertEquals(new Double(229.852), turns[15].getStart().getOffset());
      assertEquals(new Double(285.864), turns[15].getEnd().getOffset());
      assertEquals("participant", turns[15].getLabel());

      assertEquals(new Double(285.864), turns[16].getStart().getOffset());
      assertEquals(new Double(295.115), turns[16].getEnd().getOffset());
      assertEquals("interviewer", turns[16].getLabel());

      assertEquals(new Double(295.115), turns[17].getStart().getOffset());
      assertEquals(new Double(302.834), turns[17].getEnd().getOffset());
      assertEquals("participant", turns[17].getLabel());

      assertEquals(new Double(302.834), turns[18].getStart().getOffset());
      assertEquals(new Double(304.334), turns[18].getEnd().getOffset());
      assertEquals("interviewer", turns[18].getLabel());

      assertEquals(new Double(304.334), turns[19].getStart().getOffset());
      assertEquals(new Double(306.92), turns[19].getEnd().getOffset());
      assertEquals("participant", turns[19].getLabel());

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(139, utterances.length);
      assertEquals(new Double(0.0), utterances[0].getStart().getOffset());
      assertEquals(new Double(5.75), utterances[0].getEnd().getOffset());
      assertEquals("participant", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals(new Double(5.75), utterances[1].getStart().getOffset());
      assertEquals(new Double(6.907), utterances[1].getEnd().getOffset());
      assertEquals("participant", utterances[1].getParent().getLabel());
      assertEquals(turns[0], utterances[1].getParent());

      assertEquals(new Double(44.255), utterances[21].getStart().getOffset());
      assertEquals(new Double(45.505), utterances[21].getEnd().getOffset());
      assertEquals("interviewer", utterances[21].getParent().getLabel());
      assertEquals(turns[1], utterances[21].getParent());

      Annotation[] words = g.list("word");
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

      // comment
      Annotation[] comments = g.list("comment");
      assertEquals("unclear", comments[0].getLabel());
      assertEquals("in", comments[0].getStart().endOf("word").iterator().next().getLabel());
      assertEquals("-- and", comments[0].getEnd().startOf("word").iterator().next().getLabel());

      assertEquals("break in recording", comments[3].getLabel());
      assertEquals("there", comments[3].getStart().endOf("word").iterator().next().getLabel());
      assertEquals("no following words: " + comments[3].getEnd().startOf("word"),
		   0, comments[3].getEnd().startOf("word").size());

      assertEquals("participant: sits down", comments[4].getLabel());
      assertEquals("interviewer: unclear", comments[5].getLabel());

      assertEquals(6, comments.length);

      // noise
      Annotation[] noises = g.list("noise");
      assertEquals("throatclear", noises[0].getLabel());
      assertEquals(words[1].getEnd(), noises[0].getStart());
      assertEquals(words[2].getStart(), noises[0].getEnd());

      assertEquals("interviewer: clears throat", noises[1].getLabel());
      assertEquals("and -", noises[1].getStart().endOf("word").iterator().next().getLabel());
      assertEquals("-- learned", noises[1].getEnd().startOf("word").iterator().next().getLabel());

      assertEquals("both laugh", noises[2].getLabel());
      assertEquals("that ?", noises[2].getStart().endOf("word").iterator().next().getLabel());
      assertEquals("well", noises[2].getEnd().startOf("word").iterator().next().getLabel()); // TODO cross-speaker link ok?

      assertEquals("cough", noises[3].getLabel());
      assertEquals("microphone movement noise", noises[4].getLabel());

      // pronounce
      Annotation[] pronounce = g.list("pronounce");
      assertEquals("sIr@l", pronounce[0].getLabel());
      assertTrue(pronounce[0].tags(words[2]));
      assertEquals(1, pronounce.length);

      // lexical
      Annotation[] lexical = g.list("lexical");
      assertEquals("often", lexical[0].getLabel());
      assertEquals("o~", lexical[0].my("word").getLabel());
      assertEquals(1, lexical.length);
   }

   @Test public void utterance_utf8() 
      throws Exception
   {
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
      TextGridDeserializer deserializer = new TextGridDeserializer();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(6, deserializer.configure(configuration, schema).size());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      //for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
      assertEquals(2, defaultParamaters.size());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);
      
      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("test_utterance_utf-8.TextGrid", g.getId());

      // participants     
      Annotation[] who = g.list("who");
      assertEquals(2, who.length);
      assertEquals("participant", who[0].getLabel());
      assertEquals(g, who[0].getParent());
      assertEquals("interviewer", who[1].getLabel());
      assertEquals(g, who[1].getParent());
      
      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(20, turns.length);
      assertEquals(new Double(0.0), turns[0].getStart().getOffset());
      assertEquals(new Double(44.255), turns[0].getEnd().getOffset());
      assertEquals("participant", turns[0].getLabel());
      assertEquals(who[0], turns[0].getParent());
      
      Annotation[] words = g.list("word");
      String[] wordLabels = { // NB we have a c-unit layer, so terminators are stripped off 
	 "and", "äh .", "Cyril", "would", "arrive", "at", "the", "door",
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
   }

   @Test public void utterance_utf16() 
      throws Exception
   {
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
      TextGridDeserializer deserializer = new TextGridDeserializer();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(6, deserializer.configure(configuration, schema).size());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      //for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
      assertEquals(2, defaultParamaters.size());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);
      
      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("test_utterance_utf-16.TextGrid", g.getId());

      // participants     
      Annotation[] who = g.list("who");
      assertEquals(2, who.length);
      assertEquals("participant", who[0].getLabel());
      assertEquals(g, who[0].getParent());
      assertEquals("interviewer", who[1].getLabel());
      assertEquals(g, who[1].getParent());
      
      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(20, turns.length);
      assertEquals(new Double(0.0), turns[0].getStart().getOffset());
      assertEquals(new Double(44.255), turns[0].getEnd().getOffset());
      assertEquals("participant", turns[0].getLabel());
      assertEquals(who[0], turns[0].getParent());
      
      Annotation[] words = g.list("word");
      String[] wordLabels = { // NB we have a c-unit layer, so terminators are stripped off 
	 "and", "äh .", "Cyril", "would", "arrive", "at", "the", "door",
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
   }

   @Test public void utterance_latin1() 
      throws Exception
   {
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
      TextGridDeserializer deserializer = new TextGridDeserializer();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(6, deserializer.configure(configuration, schema).size());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      //for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
      assertEquals(2, defaultParamaters.size());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);
      
      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("test_utterance_latin1.TextGrid", g.getId());

      // participants     
      Annotation[] who = g.list("who");
      assertEquals(2, who.length);
      assertEquals("participant", who[0].getLabel());
      assertEquals(g, who[0].getParent());
      assertEquals("interviewer", who[1].getLabel());
      assertEquals(g, who[1].getParent());
      
      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(20, turns.length);
      assertEquals(new Double(0.0), turns[0].getStart().getOffset());
      assertEquals(new Double(44.255), turns[0].getEnd().getOffset());
      assertEquals("participant", turns[0].getLabel());
      assertEquals(who[0], turns[0].getParent());
      
      Annotation[] words = g.list("word");
      String[] wordLabels = { // NB we have a c-unit layer, so terminators are stripped off 
	 "and", "äh .", "Cyril", "would", "arrive", "at", "the", "door",
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
   }

   @Test public void utterance_word() 
      throws Exception
   {
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
      TextGridDeserializer deserializer = new TextGridDeserializer();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(6, deserializer.configure(configuration, schema).size());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      //for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
      assertEquals(6, defaultParamaters.size());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);
      
      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("test_utterance_word.TextGrid", g.getId());

      // participants     
      Annotation[] who = g.list("who");
      assertEquals(2, who.length);
      assertEquals("participant", who[0].getLabel());
      assertEquals(g, who[0].getParent());
      assertEquals("interviewer", who[1].getLabel());
      assertEquals(g, who[1].getParent());
      
      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(20, turns.length);
      assertEquals(new Double(0.0), turns[0].getStart().getOffset());
      assertEquals(new Double(44.255), turns[0].getEnd().getOffset());
      assertEquals("participant", turns[0].getLabel());
      assertEquals(who[0], turns[0].getParent());
      
      assertEquals(new Double(44.255), turns[1].getStart().getOffset());
      assertEquals(new Double(45.505), turns[1].getEnd().getOffset());
      assertEquals("interviewer", turns[1].getLabel());
      assertEquals(who[1], turns[1].getParent());

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(139, utterances.length);
      assertEquals(new Double(0.0), utterances[0].getStart().getOffset());
      assertEquals(new Double(5.75), utterances[0].getEnd().getOffset());
      assertEquals("participant", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals(new Double(5.75), utterances[1].getStart().getOffset());
      assertEquals(new Double(6.907), utterances[1].getEnd().getOffset());
      assertEquals("participant", utterances[1].getParent().getLabel());
      assertEquals(turns[0], utterances[1].getParent());

      assertEquals(new Double(44.255), utterances[21].getStart().getOffset());
      assertEquals(new Double(45.505), utterances[21].getEnd().getOffset());
      assertEquals("interviewer", utterances[21].getParent().getLabel());
      assertEquals(turns[1], utterances[21].getParent());

      Annotation[] words = g.list("word");
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
      assertEquals("no conventional comments", 0, g.list("comment").length);
      assertEquals("no conventional noises", 0, g.list("noise").length);
      assertEquals("no conventional pronounce annotations", 0, g.list("pronounce").length);
      assertEquals("no conventional lexical annotations", 0, g.list("lexical").length);

      // phones
      Annotation[] phones = g.list("phone");
      assertEquals("phones", 13, phones.length);

      // participant

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

   @Test public void utterance_word_ignorePhones() 
      throws Exception
   {
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
      TextGridDeserializer deserializer = new TextGridDeserializer();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(6, deserializer.configure(configuration, schema).size());

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

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("test_utterance_word.TextGrid", g.getId());

      // participants     
      Annotation[] who = g.list("who");
      assertEquals(2, who.length);
      assertEquals("participant", who[0].getLabel());
      assertEquals(g, who[0].getParent());
      assertEquals("interviewer", who[1].getLabel());
      assertEquals(g, who[1].getParent());
      
      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(20, turns.length);
      assertEquals(new Double(0.0), turns[0].getStart().getOffset());
      assertEquals(new Double(44.255), turns[0].getEnd().getOffset());
      assertEquals("participant", turns[0].getLabel());
      assertEquals(who[0], turns[0].getParent());
      
      assertEquals(new Double(44.255), turns[1].getStart().getOffset());
      assertEquals(new Double(45.505), turns[1].getEnd().getOffset());
      assertEquals("interviewer", turns[1].getLabel());
      assertEquals(who[1], turns[1].getParent());

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(139, utterances.length);
      assertEquals(new Double(0.0), utterances[0].getStart().getOffset());
      assertEquals(new Double(5.75), utterances[0].getEnd().getOffset());
      assertEquals("participant", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals(new Double(5.75), utterances[1].getStart().getOffset());
      assertEquals(new Double(6.907), utterances[1].getEnd().getOffset());
      assertEquals("participant", utterances[1].getParent().getLabel());
      assertEquals(turns[0], utterances[1].getParent());

      assertEquals(new Double(44.255), utterances[21].getStart().getOffset());
      assertEquals(new Double(45.505), utterances[21].getEnd().getOffset());
      assertEquals("interviewer", utterances[21].getParent().getLabel());
      assertEquals(turns[1], utterances[21].getParent());

      Annotation[] words = g.list("word");
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
      assertEquals("no conventional comments", 0, g.list("comment").length);
      assertEquals("no conventional noises", 0, g.list("noise").length);
      assertEquals("no conventional pronounce annotations", 0, g.list("pronounce").length);
      assertEquals("no conventional lexical annotations", 0, g.list("lexical").length);

      // phones
      assertEquals("no phones", 0, g.list("phone").length);
   }

   @Test public void turn_utterance_word() 
      throws Exception
   {
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
      TextGridDeserializer deserializer = new TextGridDeserializer();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(6, deserializer.configure(configuration, schema).size());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      //for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
      assertEquals(6, defaultParamaters.size());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);
      
      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("test_turn_utterance_word.TextGrid", g.getId());

      // participants     
      Annotation[] who = g.list("who");
      assertEquals(2, who.length);
      assertEquals("participant", who[0].getLabel());
      assertEquals(g, who[0].getParent());
      assertEquals("interviewer", who[1].getLabel());
      assertEquals(g, who[1].getParent());
      
      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(new Double(0.0), turns[0].getStart().getOffset());
      assertEquals(new Double(44.255), turns[0].getEnd().getOffset());
      assertEquals("participant", turns[0].getLabel());
      assertEquals(who[0], turns[0].getParent());
      
      assertEquals(new Double(44.255), turns[1].getStart().getOffset());
      assertEquals(new Double(45.505), turns[1].getEnd().getOffset());
      assertEquals("interviewer", turns[1].getLabel());
      assertEquals(who[1], turns[1].getParent());

      assertEquals(new Double(45.505), turns[2].getStart().getOffset());
      assertEquals(new Double(107.804), turns[2].getEnd().getOffset());
      assertEquals("participant", turns[2].getLabel());

      assertEquals(new Double(107.804), turns[3].getStart().getOffset());
      assertEquals(new Double(110.335), turns[3].getEnd().getOffset());
      assertEquals("interviewer", turns[3].getLabel());

      assertEquals(new Double(110.335), turns[4].getStart().getOffset());
      assertEquals(new Double(181.652), turns[4].getEnd().getOffset());
      assertEquals("participant", turns[4].getLabel());

      assertEquals(new Double(181.652), turns[5].getStart().getOffset());
      assertEquals(new Double(183.995), turns[5].getEnd().getOffset());
      assertEquals("interviewer", turns[5].getLabel());

      assertEquals(new Double(183.995), turns[6].getStart().getOffset());
      assertEquals(new Double(185.339), turns[6].getEnd().getOffset());
      assertEquals("participant", turns[6].getLabel());

      assertEquals(new Double(185.339), turns[7].getStart().getOffset());
      assertEquals(new Double(189.464), turns[7].getEnd().getOffset());
      assertEquals("interviewer", turns[7].getLabel());

      assertEquals(new Double(189.464), turns[8].getStart().getOffset());
      assertEquals(new Double(191.682), turns[8].getEnd().getOffset());
      assertEquals("participant", turns[8].getLabel());

      assertEquals(new Double(191.682), turns[9].getStart().getOffset());
      assertEquals(new Double(197.181), turns[9].getEnd().getOffset());
      assertEquals("interviewer", turns[9].getLabel());

      assertEquals(new Double(197.181), turns[10].getStart().getOffset());
      assertEquals(new Double(199.213), turns[10].getEnd().getOffset());
      assertEquals("participant", turns[10].getLabel());

      assertEquals(new Double(199.213), turns[11].getStart().getOffset());
      assertEquals(new Double(205.415), turns[11].getEnd().getOffset());
      assertEquals("interviewer", turns[11].getLabel());

      // simultaneous speech
      // the textgrid has two itervals for this which have been joined into one turn
      assertEquals(new Double(205.415), turns[12].getStart().getOffset());
      assertEquals(new Double(220.696), turns[12].getEnd().getOffset());
      assertEquals("participant", turns[12].getLabel());
      // simultaneous speech
      assertEquals(new Double(214.822), turns[13].getStart().getOffset());
      assertEquals(new Double(218.29), turns[13].getEnd().getOffset());
      assertEquals("interviewer", turns[13].getLabel());

      assertEquals(new Double(220.696), turns[14].getStart().getOffset());
      assertEquals(new Double(223.227), turns[14].getEnd().getOffset());
      assertEquals("interviewer", turns[14].getLabel());

      assertEquals(new Double(229.852), turns[15].getStart().getOffset());
      assertEquals(new Double(285.864), turns[15].getEnd().getOffset());
      assertEquals("participant", turns[15].getLabel());

      assertEquals(new Double(285.864), turns[16].getStart().getOffset());
      assertEquals(new Double(295.115), turns[16].getEnd().getOffset());
      assertEquals("interviewer", turns[16].getLabel());

      assertEquals(new Double(295.115), turns[17].getStart().getOffset());
      assertEquals(new Double(302.834), turns[17].getEnd().getOffset());
      assertEquals("participant", turns[17].getLabel());

      assertEquals(new Double(302.834), turns[18].getStart().getOffset());
      assertEquals(new Double(304.334), turns[18].getEnd().getOffset());
      assertEquals("interviewer", turns[18].getLabel());

      assertEquals(new Double(304.334), turns[19].getStart().getOffset());
      assertEquals(new Double(306.92), turns[19].getEnd().getOffset());
      assertEquals("participant", turns[19].getLabel());

      assertEquals(20, turns.length);

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(139, utterances.length);
      assertEquals(new Double(0.0), utterances[0].getStart().getOffset());
      assertEquals(new Double(5.75), utterances[0].getEnd().getOffset());
      assertEquals("participant", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals(new Double(5.75), utterances[1].getStart().getOffset());
      assertEquals(new Double(6.907), utterances[1].getEnd().getOffset());
      assertEquals("participant", utterances[1].getParent().getLabel());
      assertEquals(turns[0], utterances[1].getParent());

      assertEquals(new Double(44.255), utterances[21].getStart().getOffset());
      assertEquals(new Double(45.505), utterances[21].getEnd().getOffset());
      assertEquals("interviewer", utterances[21].getParent().getLabel());
      assertEquals(turns[1], utterances[21].getParent());

      Annotation[] words = g.list("word");
      String[] wordLabels = {
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

      // check the simultaneous speech worked out all right
      words = turns[13].annotations("word");
      String[] simultaneousSpeech = {"yeah","yeah", "yeah --", "well", "that's", "right"};
      for (int i = 0; i < simultaneousSpeech.length; i++)
      {
	 assertEquals("simultaneous speech " + i, simultaneousSpeech[i], words[i].getLabel());
	 assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
	 	      i+1, words[i].getOrdinal());
      }
      assertEquals("simultaneous speech", simultaneousSpeech.length, words.length);

      // no convention annotations, because the utterances are not tokenized
      assertEquals("no conventional comments", 0, g.list("comment").length);
      assertEquals("no conventional noises", 0, g.list("noise").length);
      assertEquals("no conventional pronounce annotations", 0, g.list("pronounce").length);
      assertEquals("no conventional lexical annotations", 0, g.list("lexical").length);
   }

   @Test public void speaker_word() 
      throws Exception
   {
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
      TextGridDeserializer deserializer = new TextGridDeserializer();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(6, deserializer.configure(configuration, schema).size());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      //for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
      assertEquals(4, defaultParamaters.size());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);
      
      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("test_speaker_word.TextGrid", g.getId());

      // participants     
      Annotation[] who = g.list("who");
      assertEquals(2, who.length);
      assertEquals("participant", who[0].getLabel());
      assertEquals(g, who[0].getParent());
      assertEquals("interviewer", who[1].getLabel());
      assertEquals(g, who[1].getParent());
      
      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(new Double(0.0), turns[0].getStart().getOffset());
      assertEquals(new Double(44.255), turns[0].getEnd().getOffset());
      assertEquals("participant", turns[0].getLabel());
      assertEquals(who[0], turns[0].getParent());
      
      assertEquals(new Double(44.255), turns[1].getStart().getOffset());
      assertEquals(new Double(45.505), turns[1].getEnd().getOffset());
      assertEquals("interviewer", turns[1].getLabel());
      assertEquals(who[1], turns[1].getParent());

      assertEquals(new Double(45.505), turns[2].getStart().getOffset());
      assertEquals(new Double(107.804), turns[2].getEnd().getOffset());
      assertEquals("participant", turns[2].getLabel());

      assertEquals(new Double(107.804), turns[3].getStart().getOffset());
      assertEquals(new Double(110.335), turns[3].getEnd().getOffset());
      assertEquals("interviewer", turns[3].getLabel());

      assertEquals(new Double(110.335), turns[4].getStart().getOffset());
      assertEquals(new Double(181.652), turns[4].getEnd().getOffset());
      assertEquals("participant", turns[4].getLabel());

      assertEquals(new Double(181.652), turns[5].getStart().getOffset());
      assertEquals(new Double(183.995), turns[5].getEnd().getOffset());
      assertEquals("interviewer", turns[5].getLabel());

      assertEquals(new Double(183.995), turns[6].getStart().getOffset());
      assertEquals(new Double(185.339), turns[6].getEnd().getOffset());
      assertEquals("participant", turns[6].getLabel());

      assertEquals(new Double(185.339), turns[7].getStart().getOffset());
      assertEquals(new Double(189.464), turns[7].getEnd().getOffset());
      assertEquals("interviewer", turns[7].getLabel());

      assertEquals(new Double(189.464), turns[8].getStart().getOffset());
      assertEquals(new Double(191.682), turns[8].getEnd().getOffset());
      assertEquals("participant", turns[8].getLabel());

      assertEquals(new Double(191.682), turns[9].getStart().getOffset());
      assertEquals(new Double(197.181), turns[9].getEnd().getOffset());
      assertEquals("interviewer", turns[9].getLabel());

      assertEquals(new Double(197.181), turns[10].getStart().getOffset());
      assertEquals(new Double(199.213), turns[10].getEnd().getOffset());
      assertEquals("participant", turns[10].getLabel());

      assertEquals(new Double(199.213), turns[11].getStart().getOffset());
      assertEquals(new Double(205.415), turns[11].getEnd().getOffset());
      assertEquals("interviewer", turns[11].getLabel());

      assertEquals(new Double(205.415), turns[12].getStart().getOffset());
      assertEquals(new Double(220.696), turns[12].getEnd().getOffset());
      assertEquals("participant", turns[12].getLabel());

      assertEquals(new Double(220.696), turns[13].getStart().getOffset());
      assertEquals(new Double(223.227), turns[13].getEnd().getOffset());
      assertEquals("interviewer", turns[13].getLabel());

      assertEquals(new Double(229.852), turns[14].getStart().getOffset());
      assertEquals(new Double(285.864), turns[14].getEnd().getOffset());
      assertEquals("participant", turns[14].getLabel());

      assertEquals(new Double(285.864), turns[15].getStart().getOffset());
      assertEquals(new Double(295.115), turns[15].getEnd().getOffset());
      assertEquals("interviewer", turns[15].getLabel());

      assertEquals(new Double(295.115), turns[16].getStart().getOffset());
      assertEquals(new Double(302.834), turns[16].getEnd().getOffset());
      assertEquals("participant", turns[16].getLabel());

      assertEquals(new Double(302.834), turns[17].getStart().getOffset());
      assertEquals(new Double(304.334), turns[17].getEnd().getOffset());
      assertEquals("interviewer", turns[17].getLabel());

      assertEquals(new Double(304.334), turns[18].getStart().getOffset());
      assertEquals(new Double(306.92), turns[18].getEnd().getOffset());
      assertEquals("participant", turns[18].getLabel());

      assertEquals(19, turns.length);

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(138, utterances.length);
      assertEquals(new Double(0.0), utterances[0].getStart().getOffset());
      assertEquals(new Double(5.75), utterances[0].getEnd().getOffset());
      assertEquals("participant", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals(new Double(5.75), utterances[1].getStart().getOffset());
      assertEquals(new Double(6.907), utterances[1].getEnd().getOffset());
      assertEquals("participant", utterances[1].getParent().getLabel());
      assertEquals(turns[0], utterances[1].getParent());

      assertEquals(new Double(44.255), utterances[21].getStart().getOffset());
      assertEquals(new Double(45.505), utterances[21].getEnd().getOffset());
      assertEquals("interviewer", utterances[21].getParent().getLabel());
      assertEquals(turns[1], utterances[21].getParent());

      Annotation[] words = g.list("word");
      String[] wordLabels = {
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

      words = turns[13].annotations("word");
      String[] simultaneousSpeech = {"ah", "yeah", "just", "turn", "left", "there"};
      for (int i = 0; i < simultaneousSpeech.length; i++)
      {
	 assertEquals("other speaker words " + i, simultaneousSpeech[i], words[i].getLabel());
	 assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
	 	      i+1, words[i].getOrdinal());
      }
      assertEquals("other speaker words", simultaneousSpeech.length, words.length);

      // no convention annotations, because the utterances are not tokenized
      assertEquals("no conventional comments", 0, g.list("comment").length);
      assertEquals("no conventional noises", 0, g.list("noise").length);
      assertEquals("no conventional pronounce annotations", 0, g.list("pronounce").length);
      assertEquals("no conventional lexical annotations", 0, g.list("lexical").length);

      // topic
      Annotation[] topics = g.list("topic");
      assertEquals(2, topics.length);
      assertEquals(new Double(79.726), topics[0].getStart().getOffset());
      assertEquals(new Double(107.804), topics[0].getEnd().getOffset());
      assertEquals("hospital", topics[0].getLabel());
      assertEquals(g, topics[0].getParent());

      assertEquals(new Double(124.1557142857143), topics[1].getStart().getOffset());
      assertEquals(new Double(181.652), topics[1].getEnd().getOffset());
      assertEquals("barry", topics[1].getLabel());
      assertEquals(g, topics[1].getParent());

      // named entity
      Annotation[] entities = g.list("entity");
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
   }

   @Test public void basFragment() 
      throws Exception
   {
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
      TextGridDeserializer deserializer = new TextGridDeserializer();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(6, deserializer.configure(configuration, schema).size());

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

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("fragment__1_890-3_830.TextGrid", g.getId());

      // participants     
      Annotation[] who = g.list("who");
      assertEquals(1, who.length);
      assertEquals("ORT", who[0].getLabel());
      assertEquals(g, who[0].getParent());
      
      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(1, turns.length);
      assertEquals(new Double(0.0), turns[0].getStart().getOffset());
      assertEquals(new Double(1.93), turns[0].getEnd().getOffset());
      assertEquals("ORT", turns[0].getLabel());
      assertEquals(who[0], turns[0].getParent());

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(1, utterances.length);
      assertEquals(new Double(0.0), utterances[0].getStart().getOffset());
      assertEquals(new Double(1.93), utterances[0].getEnd().getOffset());
      assertEquals("ORT", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      Annotation[] words = g.list("word");
      String[] wordLabels = { // NB we have a c-unit layer, so terminators are stripped off 
	 "so", "what", "is", "your", "name"
      };
      for (int i = 0; i < wordLabels.length; i++)
      {
	 assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
	 assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
	 	      i+1, words[i].getOrdinal());
	 assertEquals(turns[0].getId(), words[i].getParentId());
      }

      // no convention annotations, because the utterances are not tokenized
      assertEquals("no conventional comments", 0, g.list("comment").length);
      assertEquals("no conventional noises", 0, g.list("noise").length);
      assertEquals("no conventional pronounce annotations", 0, g.list("pronounce").length);
      assertEquals("no conventional lexical annotations", 0, g.list("lexical").length);

      // phones
      Annotation[] phones = g.list("phone");
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


   }

   @Test public void performance() 
      throws Exception
   {
      Schema schema = new Schema(
	 "who", "turn", "utterance", "tranascript",
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("middle-750", "middle-750", 2, true, false, true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("disfluency", "disfluency", 2, true, false, false, "turn", true),
	 new Layer("IntS", "IntS", 2, true, false, false, "turn", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("transcript", "Words", 2, true, false, false, "turn", true),
	 new Layer("segments", "Phones", 2, true, false, true, "transcript", true),
	 new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "s1402a.TextGrid")) };
      
      // create deserializer
      TextGridDeserializer deserializer = new TextGridDeserializer();
      deserializer.setTimers(new nzilbb.util.Timers());
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(7, deserializer.configure(configuration, schema).size());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("param " + p.getName() + " = " + p.getValue());
      assertEquals(8, defaultParamaters.size());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);
      
      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }

      assertEquals("s1402a.TextGrid", g.getId());

      assertTrue("Deserialization too slow:\n" + deserializer.getTimers().toString(),
		 1500 > deserializer.getTimers().getTotals().get("deserialize"));
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
      org.junit.runner.JUnitCore.main("nzilbb.praat.test.TestTextGridDeserializer");
   }
}
