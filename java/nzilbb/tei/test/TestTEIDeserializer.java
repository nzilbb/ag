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

package nzilbb.tei.test;
	      
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
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.tei.*;

public class TestTEIDeserializer
{
   @Test public void song() 
      throws Exception
   {
      Schema schema = new Schema("who", "turn", "utterance", "word",
	 new Layer("scribe", "Transcriber", 0, true, true, true),
	 new Layer("transcript_language", "Graph language", 0, false, false, true),
	 new Layer("transcript_version_date", "Version Date", 0, false, false, true),
	 new Layer("publication_date", "Publication Date", 0, false, false, true),
	 new Layer("transcript_program", "Program", 0, false, false, true),
	 new Layer("title", "Title", 0, false, false, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("topic", "Topic", 2, true, false, false),
	 new Layer("comment", "Comment", 2, true, false, true),
	 new Layer("noise", "Noise", 2, true, false, true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("entities", "Entities", 2, true, false, false, "turn", true),
	 new Layer("lg", "Verses", 2, true, false, false, "turn", true),
	 new Layer("language", "Language", 2, true, false, false, "turn", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true),
	 new Layer("lexical", "Lexical", 0, true, false, false, "word", true));

      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "test-song.xml")) };
      
      // create deserializer
      TEIDeserializer deserializer = new TEIDeserializer();

      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals("Configuration parameters" + configuration, 12, deserializer.configure(configuration, schema).size());      
      assertEquals("comment", "comment", 
		   ((Layer)configuration.get("commentLayer").getValue()).getId());
      assertEquals("language", "language", 
		   ((Layer)configuration.get("languageLayer").getValue()).getId());
      assertEquals("lexical", "lexical", 
		   ((Layer)configuration.get("lexicalLayer").getValue()).getId());
      assertEquals("entities", "entities", 
		   ((Layer)configuration.get("entityLayer").getValue()).getId());
      assertEquals("scribe", "scribe", 
		   ((Layer)configuration.get("scribeLayer").getValue()).getId());
      assertEquals("transcript_version_date", "transcript_version_date", 
		   ((Layer)configuration.get("versionDateLayer").getValue()).getId());
      assertEquals("publication_date", "publication_date", 
		   ((Layer)configuration.get("publicationDateLayer").getValue()).getId());
      assertEquals("transcript_language", "transcript_language", 
		   ((Layer)configuration.get("transcriptLanguageLayer").getValue()).getId());
      assertNull("sex", configuration.get("sexLayer").getValue());
      assertNull("age", configuration.get("ageLayer").getValue());
      assertNull("birth", configuration.get("birthLayer").getValue());

      // load the stream
      ParameterSet defaultParameters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(2, defaultParameters.size());
      assertNull("no url layer",
		 defaultParameters.get("idnoUrl").getValue());
      assertEquals("lg", "lg", 
		   ((Layer)defaultParameters.get("lg").getValue()).getId());

      // configure the deserialization
      deserializer.setParameters(defaultParameters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("test-song.xml", g.getId());
      String[] title = g.labels("title"); 
      assertEquals(1, title.length);
      assertEquals("Everything's Gonna Be Alright", title[0]);

      // participants     
      Annotation[] author = g.all("who"); 
      assertEquals(1, author.length);
      assertEquals("Aaliyah", author[0].getLabel());
      assertNotNull("participant anchored", author[0].getStartId());
      assertNotNull("participant anchored", author[0].getEndId());

      // turns
      Annotation[] turns = g.all("turn");
      assertEquals(1, turns.length);
      assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
      //assertEquals(Double.valueOf(23.563), turns[0].getEnd().getOffset()); // TODO
      assertEquals("Aaliyah", turns[0].getLabel());
      assertEquals(g.first("who"), turns[0].getParent());

      // utterances
      Annotation[] utterances = g.all("utterance");
      assertEquals(45, utterances.length);
      assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
      assertEquals("inter-line space", Double.valueOf(10.0), utterances[0].getEnd().getOffset());
      assertEquals("Aaliyah", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals("inter-line space", Double.valueOf(10.0), utterances[1].getStart().getOffset());
      assertEquals("inter-line space", Double.valueOf(31.0), utterances[1].getEnd().getOffset());
      assertEquals("Aaliyah", utterances[1].getParent().getLabel());
      
      Annotation[] words = g.all("word");
      assertEquals(Double.valueOf(0), words[0].getStart().getOffset());
      // System.out.println("" + Arrays.asList(Arrays.copyOfRange(words, 0, 10)));
      assertEquals("[Aaliyah]", words[0].getLabel());
      assertEquals("inter-word space", Double.valueOf(10), words[0].getEnd().getOffset());
      assertEquals("next word start where last ends",
		   Double.valueOf(10), words[1].getStart().getOffset());
      assertEquals("next word linked to last", words[0].getEnd(), words[1].getStart());
      assertEquals("Yo", words[1].getLabel());
      assertEquals("inter-word space", Double.valueOf(13), words[1].getEnd().getOffset());
      assertEquals("next word linked to last", words[1].getEnd(), words[2].getStart());
      assertEquals("Rodney", words[2].getLabel());
      assertEquals("inter-word space", Double.valueOf(20), words[2].getEnd().getOffset());
      assertEquals("next word linked to last", words[2].getEnd(), words[3].getStart());
      assertEquals("you", words[3].getLabel());
      assertEquals("inter-word space", Double.valueOf(24), words[3].getEnd().getOffset());
      assertEquals("next word linked to last", words[3].getEnd(), words[4].getStart());
      assertEquals("ready?", words[4].getLabel());
      assertEquals("inter-word space", Double.valueOf(31), words[4].getEnd().getOffset());
      assertEquals("next word linked to last", words[4].getEnd(), words[5].getStart());
      assertEquals("Cause", words[5].getLabel());
      assertEquals("inter-word space", Double.valueOf(37), words[5].getEnd().getOffset());
      assertEquals("next word linked to last", words[5].getEnd(), words[6].getStart());
      assertEquals("I’m", words[6].getLabel());
      assertEquals("inter-word space", Double.valueOf(41), words[6].getEnd().getOffset());
      assertEquals("next word linked to last", words[6].getEnd(), words[7].getStart());
      assertEquals("ready", words[7].getLabel());
      assertEquals(Double.valueOf(47), words[7].getEnd().getOffset());

      // lg
      Annotation[] verses = g.all("lg");
      assertEquals(16, verses.length);

      assertEquals(Double.valueOf(0), verses[0].getStart().getOffset());
      assertEquals(Double.valueOf(59), verses[0].getEnd().getOffset());
      assertEquals("lg with no type is labelled 'lg'", "lg", verses[0].getLabel());
      assertEquals(turns[0], verses[0].getParent());

      assertEquals(Double.valueOf(59), verses[1].getStart().getOffset());
      assertEquals(Double.valueOf(214), verses[1].getEnd().getOffset());
      assertEquals("lg", verses[1].getLabel());
      assertEquals(turns[0], verses[1].getParent());

      assertEquals(Double.valueOf(214), verses[2].getStart().getOffset());
      assertEquals(Double.valueOf(214), verses[2].getEnd().getOffset());
      assertEquals("lg", verses[2].getLabel());
      assertEquals(turns[0], verses[2].getParent());

      assertEquals(Double.valueOf(214), verses[3].getStart().getOffset());
      assertEquals(Double.valueOf(332), verses[3].getEnd().getOffset());
      assertEquals("lg with type is labelled with type", "chorus", verses[3].getLabel());
      assertEquals(turns[0], verses[3].getParent());

      assertEquals(0, g.all("entities").length);
      assertEquals(0, g.all("language").length);
      assertEquals(0, g.all("lexical").length);
      
      // check all annotations have 'manual' confidence
      for (Annotation a : g.getAnnotationsById().values()) {
         assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

   }

   @Test public void cmc() 
      throws Exception
   {
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("scribe", "Transcriber", 0, true, true, true),
	 new Layer("transcript_language", "Graph language", 0, false, false, true),
	 new Layer("transcript_version_date", "Version Date", 0, false, false, true),
	 new Layer("publication_date", "Publication Date", 0, false, false, true),
	 new Layer("transcript_program", "Program", 0, false, false, true),
	 new Layer("title", "Title", 0, false, false, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("sex", "Sex", 0, false, false, true, "who", true),
	 new Layer("age", "Age", 0, false, false, true, "who", true),
	 new Layer("dob", "Birth Date", 0, false, false, true, "who", true),
	 new Layer("topic", "Topic", 2, true, false, false),
	 new Layer("comment", "Comment", 2, true, false, true),
	 new Layer("noise", "Noise", 2, true, false, true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("entities", "Entities", 2, true, false, false, "turn", true),
	 new Layer("language", "Language", 2, true, false, false, "turn", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true),
	 new Layer("lexical", "Lexical", 0, true, false, false, "word", true));

      // access file
      NamedStream[] streams = {
	 new NamedStream(new File(getDir(), "test-cmc.xml"))
      };
      
      // create deserializer
      TEIDeserializer deserializer = new TEIDeserializer();

      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals("Configuration parameters" + configuration, 12, deserializer.configure(configuration, schema).size());      
      assertEquals("comment", "comment", 
		   ((Layer)configuration.get("commentLayer").getValue()).getId());
      assertEquals("language", "language", 
		   ((Layer)configuration.get("languageLayer").getValue()).getId());
      assertEquals("lexical", "lexical", 
		   ((Layer)configuration.get("lexicalLayer").getValue()).getId());
      assertEquals("entities", "entities", 
		   ((Layer)configuration.get("entityLayer").getValue()).getId());
      assertEquals("scribe", "scribe", 
		   ((Layer)configuration.get("scribeLayer").getValue()).getId());
      assertEquals("transcript_version_date", "transcript_version_date", 
		   ((Layer)configuration.get("versionDateLayer").getValue()).getId());
      assertEquals("publication_date", "publication_date", 
		   ((Layer)configuration.get("publicationDateLayer").getValue()).getId());
      assertEquals("transcript_language", "transcript_language", 
		   ((Layer)configuration.get("transcriptLanguageLayer").getValue()).getId());
      assertEquals("sex", "sex", 
		   ((Layer)configuration.get("sexLayer").getValue()).getId());
      assertEquals("age", "age", 
		   ((Layer)configuration.get("ageLayer").getValue()).getId());
      assertEquals("birthdate", "dob", 
		   ((Layer)configuration.get("birthLayer").getValue()).getId());

      // load the stream
      ParameterSet defaultParameters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(5, defaultParameters.size());
      assertNull("no url layer",
		   defaultParameters.get("idnoUrl").getValue());
      assertEquals("addressingTerm", "entities", 
		   ((Layer)defaultParameters.get("addressingTerm").getValue()).getId());
      assertEquals("addressee", "entities", 
		   ((Layer)defaultParameters.get("addressee").getValue()).getId());
      assertEquals("addressMarker", "entities", 
		   ((Layer)defaultParameters.get("addressMarker").getValue()).getId());
      assertEquals("emoticon", "entities", 
		   ((Layer)defaultParameters.get("emoticon").getValue()).getId());
      
      // configure the deserialization
      deserializer.setParameters(defaultParameters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("test-cmc.xml", g.getId());
      String[] title = g.labels("title"); 
      assertEquals(1, title.length);
      assertEquals("Computer-Mediated Communication Example", title[0]);

      // units
      assertEquals(Constants.UNIT_CHARACTERS, g.getOffsetUnits());

      // participants     
      Annotation[] author = g.all("who"); 
      assertEquals(3, author.length);
      assertEquals("Rachael Tatman", author[0].getLabel());
      assertEquals("rctatman", author[0].getId());
      assertEquals("Lauren Ackerman", author[1].getLabel());
      assertEquals("VerbingNouns", author[1].getId());
      assertEquals("Allison", author[2].getLabel());
      assertEquals("allisons", author[2].getId());

      // participant attributes - sex
      assertEquals("F", author[0].first("sex").getLabel());
      assertEquals("F", author[1].first("sex").getLabel());
      assertNull("Missing attribute", author[2].first("sex"));

      // participant attributes - age
      assertEquals("1", author[0].first("age").getLabel());
      assertEquals("10", author[1].first("age").getLabel());
      assertEquals("100", author[2].first("age").getLabel());

      // participant attributes - birth
      assertEquals("2016-02-20", author[0].first("dob").getLabel());
      assertEquals("2007-02-20", author[1].first("dob").getLabel());
      assertEquals("1917-02-20", author[2].first("dob").getLabel());

      // turns
      Annotation[] turns = g.all("turn");
      assertEquals(9, turns.length);
      assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
      //assertEquals(Double.valueOf(23.563), turns[0].getEnd().getOffset()); // TODO
      assertEquals("Rachael Tatman", turns[0].getLabel());
      assertEquals(g.getAnnotation("rctatman"), turns[0].getParent());
      assertEquals("Rachael Tatman", turns[1].getLabel());
      assertEquals("Rachael Tatman", turns[2].getLabel());
      assertEquals("Lauren Ackerman", turns[3].getLabel());
      assertEquals("Rachael Tatman", turns[4].getLabel());
      assertEquals("Lauren Ackerman", turns[5].getLabel());
      assertEquals("Allison", turns[6].getLabel());
      assertEquals("Rachael Tatman", turns[7].getLabel());
      assertEquals("Allison", turns[8].getLabel());

      // utterances
      Annotation[] utterances = g.all("utterance");
      assertEquals(11, utterances.length);
      assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
      assertEquals("inter-line space", Double.valueOf(40.0), utterances[0].getEnd().getOffset());
      assertEquals("Rachael Tatman", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals("inter-line space", Double.valueOf(40.0), utterances[1].getStart().getOffset());
      assertEquals("inter-line space", Double.valueOf(82.0), utterances[1].getEnd().getOffset());
      assertEquals("Rachael Tatman", utterances[1].getParent().getLabel());
      assertEquals(turns[0], utterances[1].getParent());

      assertEquals("inter-line space", Double.valueOf(82.0), utterances[2].getStart().getOffset());
      assertEquals("inter-line space", Double.valueOf(136.0), utterances[2].getEnd().getOffset());
      assertEquals("Rachael Tatman", utterances[2].getParent().getLabel());
      assertEquals(turns[0], utterances[2].getParent());

      assertEquals("inter-line space", Double.valueOf(136.0), utterances[3].getStart().getOffset());
      assertEquals("inter-line space", Double.valueOf(259.0), utterances[3].getEnd().getOffset());
      assertEquals("Rachael Tatman", utterances[3].getParent().getLabel());
      assertEquals("Turn change", turns[1], utterances[3].getParent());

      assertEquals("inter-line space", Double.valueOf(259.0), utterances[4].getStart().getOffset());
      assertEquals("inter-line space", Double.valueOf(366.0), utterances[4].getEnd().getOffset());
      assertEquals("Rachael Tatman", utterances[4].getParent().getLabel());
      assertEquals("Turn change", turns[2], utterances[4].getParent());

      assertEquals("inter-line space", Double.valueOf(366.0), utterances[5].getStart().getOffset());
      assertEquals("inter-line space", Double.valueOf(381.0), utterances[5].getEnd().getOffset());
      assertEquals("Lauren Ackerman", utterances[5].getParent().getLabel());
      assertEquals("Turn change", turns[3], utterances[5].getParent());

      Annotation[] words = g.all("word");
      assertEquals(Double.valueOf(0), words[0].getStart().getOffset());
      // System.out.println("" + Arrays.asList(Arrays.copyOfRange(words, 0, 10)));
      assertEquals("Two", words[0].getLabel());
      assertEquals("inter-word space", Double.valueOf(4), words[0].getEnd().getOffset());
      assertEquals("next word start where last ends",
		   Double.valueOf(4), words[1].getStart().getOffset());
      assertEquals("next word linked to last", words[0].getEnd(), words[1].getStart());
      assertEquals("things", words[1].getLabel());
      assertEquals("inter-word space", Double.valueOf(11), words[1].getEnd().getOffset());
      assertEquals("next word linked to last", words[1].getEnd(), words[2].getStart());
      assertEquals("that", words[2].getLabel());
      assertEquals("inter-word space", Double.valueOf(16), words[2].getEnd().getOffset());
      assertEquals("next word linked to last", words[2].getEnd(), words[3].getStart());
      assertEquals("drive", words[3].getLabel());
      
      assertEquals("me", words[4].getLabel());
      assertEquals("up", words[5].getLabel());
      assertEquals("the", words[6].getLabel());
      assertEquals("wall: -", words[7].getLabel());
      assertEquals("being", words[8].getLabel());
      assertEquals("told", words[9].getLabel());
      assertEquals("my", words[10].getLabel());
      assertEquals("voice", words[11].getLabel());
      assertEquals("is", words[12].getLabel());
      assertEquals("too", words[13].getLabel());
      assertEquals("high", words[14].getLabel());
      assertEquals("pitched -", words[15].getLabel());
      assertEquals("people", words[16].getLabel());
      assertEquals("*familiar", words[17].getLabel());
      assertEquals("w/", words[18].getLabel());
      assertEquals("my", words[19].getLabel());
      assertEquals("work*", words[20].getLabel());
      assertEquals("assuming", words[21].getLabel());
      assertEquals("I", words[22].getLabel());
      assertEquals("can't", words[23].getLabel());
      assertEquals("program", words[24].getLabel());
      assertEquals("Here", words[25].getLabel());
      assertEquals("is", words[26].getLabel());
      assertEquals("a", words[27].getLabel());
      assertEquals("fun", words[28].getLabel());
      assertEquals("fact", words[29].getLabel());
      assertEquals("from", words[30].getLabel());
      assertEquals("me,", words[31].getLabel());


      // addressee tags
      Annotation[] entities = g.all("entities");
      assertEquals(19, entities.length);
      assertEquals("emoticon", "face with tears of joy", entities[18].getLabel());
      assertTrue("emoticon tags last word", entities[18].tags(words[words.length-1]));
      
      Annotation at = words[67];
      Annotation addressee = words[68];
      assertEquals("addressing words", "@", at.getLabel());
      assertEquals("addressing words", "rctatman", addressee.getLabel());      
      assertEquals("addressMarker", entities[0].getLabel());
      assertTrue("addressMarker tags @", entities[0].tags(at));
      assertEquals("addressee", entities[1].getLabel());
      assertTrue("addressee tags person", entities[1].tags(addressee));
      assertEquals("addressingTerm", entities[2].getLabel());
      assertEquals("addressingTerm starts at addressMarker",
		   at.getStart(), entities[2].getStart());
      assertEquals("addressingTerm ends at addressee",
		   addressee.getEnd(), entities[2].getEnd());

      at = words[70];
      addressee = words[71];
      assertEquals("addressing words", "@", at.getLabel());
      assertEquals("addressing words", "VerbingNouns", addressee.getLabel());      
      assertEquals("addressMarker", entities[3].getLabel());
      assertTrue("addressMarker tags @", entities[3].tags(at));
      assertEquals("addressee", entities[4].getLabel());
      assertTrue("addressee tags person", entities[4].tags(addressee));
      assertEquals("addressingTerm", entities[5].getLabel());
      assertEquals("addressingTerm starts at addressMarker",
		   at.getStart(), entities[5].getStart());
      assertEquals("addressingTerm ends at addressee",
		   addressee.getEnd(), entities[5].getEnd());

      at = words[98];
      addressee = words[99];
      assertEquals("addressing words", "@", at.getLabel());
      assertEquals("addressing words", "rctatman", addressee.getLabel());      
      assertEquals("addressMarker", entities[6].getLabel());
      assertTrue("addressMarker tags @", entities[6].tags(at));
      assertEquals("addressee", entities[7].getLabel());
      assertTrue("addressee tags person", entities[7].tags(addressee));
      assertEquals("addressingTerm", entities[8].getLabel());
      assertEquals("addressingTerm starts at addressMarker",
		   at.getStart(), entities[8].getStart());
      assertEquals("addressingTerm ends at addressee",
		   addressee.getEnd(), entities[8].getEnd());

      assertEquals(0, g.all("language").length);
      assertEquals(0, g.all("lexical").length);
      
      // check all annotations have 'manual' confidence
      for (Annotation a : g.getAnnotationsById().values()) {
         assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

   }

   @Test public void cmcWithAttributes() 
      throws Exception
   {
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("scribe", "Transcriber", 0, true, true, true),
	 new Layer("subreddit", "Subreddit", 0, false, false, true),
	 new Layer("parent_id", "Parent", 0, false, false, true),
	 new Layer("air_date", "Publication Date", 0, false, false, true),
	 new Layer("url", "URL", 0, false, false, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("participant_language", "Language", 0, false, false, true, "who", true),
	 new Layer("comment", "Comment", 2, true, false, true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true));

      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "test-cmc-redditcomment.xml")) };
      
      // create deserializer
      TEIDeserializer deserializer = new TEIDeserializer();

      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals("Configuration parameters" + configuration, 12, deserializer.configure(configuration, schema).size());      
      assertEquals("comment", "comment", 
		   ((Layer)configuration.get("commentLayer").getValue()).getId());
      assertNull("language not mapped",
		 configuration.get("languageLayer").getValue());
      assertNull("lexical not mapped", 
		 configuration.get("lexicalLayer").getValue());
      assertNull("entities not mapped",
		 configuration.get("entityLayer").getValue());
      assertEquals("scribe", "scribe", 
		   ((Layer)configuration.get("scribeLayer").getValue()).getId());
      assertNull("transcript_version_date not mapped",
		 configuration.get("versionDateLayer").getValue());
      assertEquals("publication_date", "air_date", 
		   ((Layer)configuration.get("publicationDateLayer").getValue()).getId());
      assertNull("transcript_language not mapped",
		   configuration.get("transcriptLanguageLayer").getValue());
      assertNull("sex not mapped",
		 configuration.get("sexLayer").getValue());
      assertNull("age not mapped",
		 configuration.get("ageLayer").getValue());
      assertNull("birthdate not mapped",
		 configuration.get("birthLayer").getValue());

      // load the stream
      ParameterSet defaultParameters = deserializer.load(streams, schema);
//      for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(4, defaultParameters.size());
      assertEquals("url", "url", 
		   ((Layer)defaultParameters.get("idnoUrl").getValue()).getId());
      assertEquals("subreddit", "subreddit", 
		   ((Layer)defaultParameters.get("header_note_type_subreddit").getValue()).getId());
      assertEquals("parent_id", "parent_id", 
		   ((Layer)defaultParameters.get("header_note_type_parent_id").getValue()).getId());
      assertEquals("language", "participant_language", 
      		   ((Layer)defaultParameters.get("person_note_type_language").getValue()).getId());

      // configure the deserialization
      deserializer.setParameters(defaultParameters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("test-cmc-redditcomment.xml", g.getId());
      String[] title = g.labels("title"); 
      assertEquals(0, title.length);

      // units
      assertEquals("Associated WAV makes units seconds rather than characters",
		   Constants.UNIT_CHARACTERS, g.getOffsetUnits());

      // participants     
      Annotation[] author = g.all("who"); 
      assertEquals(1, author.length);
      assertEquals("Genkaichan", author[0].getLabel());
      assertEquals("Genkaichan", author[0].getId());

      // meta data
      assertEquals("english", author[0].first("participant_language").getLabel());

      assertEquals("2017-02-01T03:00:59.000Z", g.first("air_date").getLabel());
      assertEquals("StrangerThings", g.first("subreddit").getLabel());
      assertEquals("t1_dd5f8en", g.first("parent_id").getLabel());
      assertEquals("https://www.reddit.com/r/StrangerThings/comments/t1_dd5f8en#dd6aabs",
		   g.first("url").getLabel());

      // turns
      Annotation[] turns = g.all("turn");
      assertEquals("one turn " + Arrays.asList(turns), 1, turns.length);
      assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
      assertEquals(Double.valueOf(104.0), turns[0].getEnd().getOffset());
      assertEquals("Genkaichan", turns[0].getLabel());
      assertEquals(g.getAnnotation("Genkaichan"), turns[0].getParent());

      // utterances
      Annotation[] utterances = g.all("utterance");
      assertEquals(1, utterances.length);
      assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
      assertEquals("Genkaichan", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      Annotation[] words = g.all("word");
      assertEquals(Double.valueOf(0), words[0].getStart().getOffset());
      // System.out.println("" + Arrays.asList(Arrays.copyOfRange(words, 0, 10)));
      assertEquals("Such", words[0].getLabel());
      assertEquals("a", words[1].getLabel());
      assertEquals("lovable", words[2].getLabel());
      assertEquals("teddy", words[3].getLabel());      
      assertEquals("bear.", words[4].getLabel());
      assertEquals("As", words[5].getLabel());
      assertEquals("soon", words[6].getLabel());
      assertEquals("as", words[7].getLabel());
      assertEquals("I", words[8].getLabel());
      assertEquals("thought", words[9].getLabel());
      assertEquals("he'd", words[10].getLabel());
      assertEquals("help", words[11].getLabel());
      assertEquals("Eleven,", words[12].getLabel());
      assertEquals("those", words[13].getLabel());
      assertEquals("ideas", words[14].getLabel());
      assertEquals("were", words[15].getLabel());
      assertEquals("quickly", words[16].getLabel());
      assertEquals("dismissed...", words[17].getLabel());
      
      // check all annotations have 'manual' confidence
      for (Annotation a : g.getAnnotationsById().values()) {
         assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

   }

   @Test public void writing() 
      throws Exception
   {
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("scribe", "Transcriber", 0, true, true, true),
	 new Layer("transcript_language", "Graph language", 0, false, false, true),
	 new Layer("transcript_version_date", "Version Date", 0, false, false, true),
	 new Layer("transcript_type", "Transcript Type", 0, false, false, true),
	 new Layer("publication_date", "Publication Date", 0, false, false, true),
	 new Layer("transcript_program", "Program", 0, false, false, true),
	 new Layer("title", "Title", 0, false, false, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("sex", "Sex", 0, false, false, true, "who", true),
	 new Layer("age", "Age", 0, false, false, true, "who", true),
	 new Layer("dob", "Birth Date", 0, false, false, true, "who", true),
	 new Layer("education", "Level of Education", 0, false, false, true, "who", true),
	 new Layer("topic", "Topic", 2, true, false, false),
	 new Layer("comment", "Comment", 2, true, false, true),
	 new Layer("noise", "Noise", 2, true, false, true),
	 new Layer("figure", "Picture", 2, true, false, false),
	 new Layer("pb", "Page Break", 1, true, false, false),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("entities", "Entities", 2, true, false, false, "turn", true),
	 new Layer("type", "Writing type", 2, true, false, false, "turn", true),
	 new Layer("q", "Speech Bubble Text", 2, true, false, false, "turn", true),
	 new Layer("pc", "Punctuation", 2, true, false, false, "turn", true),
	 new Layer("emoticon", "Emoticon", 2, true, false, false, "turn", true),
	 new Layer("unclear", "Unclear", 2, true, false, false, "turn", true),
	 new Layer("sic", "Error", 2, true, false, false, "turn", true),
	 new Layer("orig", "Pre-correction Form", 2, true, false, false, "turn", true),
	 new Layer("quote", "Quote", 2, true, false, false, "turn", true),
	 new Layer("language", "Language", 2, true, false, false, "turn", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true),
	 new Layer("lexical", "Lexical", 0, true, false, false, "word", true));

      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "test-writing.xml")) };
      
      // create deserializer
      TEIDeserializer deserializer = new TEIDeserializer();

      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals("Configuration parameters" + configuration, 12, deserializer.configure(configuration, schema).size());      
      assertEquals("comment", "comment", 
		   ((Layer)configuration.get("commentLayer").getValue()).getId());
      assertEquals("language", "language", 
		   ((Layer)configuration.get("languageLayer").getValue()).getId());
      assertEquals("lexical", "lexical", 
		   ((Layer)configuration.get("lexicalLayer").getValue()).getId());
      assertEquals("entities", "entities", 
		   ((Layer)configuration.get("entityLayer").getValue()).getId());
      assertEquals("scribe", "scribe", 
		   ((Layer)configuration.get("scribeLayer").getValue()).getId());
      assertEquals("transcript_version_date", "transcript_version_date", 
		   ((Layer)configuration.get("versionDateLayer").getValue()).getId());
      assertEquals("publication_date", "publication_date", 
		   ((Layer)configuration.get("publicationDateLayer").getValue()).getId());
      assertEquals("transcript_language", "transcript_language", 
		   ((Layer)configuration.get("transcriptLanguageLayer").getValue()).getId());
      assertEquals("sex", "sex", 
		   ((Layer)configuration.get("sexLayer").getValue()).getId());
      assertEquals("age", "age", 
		   ((Layer)configuration.get("ageLayer").getValue()).getId());
      assertEquals("birthdate", "dob", 
		   ((Layer)configuration.get("birthLayer").getValue()).getId());

      // load the stream
      ParameterSet defaultParameters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(15, defaultParameters.size());
      assertNull("no idno layer",
		 defaultParameters.get("idno").getValue());
      assertEquals("q", "q", 
		   ((Layer)defaultParameters.get("q").getValue()).getId());
      assertEquals("figure", "figure", 
		   ((Layer)defaultParameters.get("figure").getValue()).getId());
      assertEquals("pb", "pb", 
		   ((Layer)defaultParameters.get("pb").getValue()).getId());
      assertEquals("pc", "pc", 
		   ((Layer)defaultParameters.get("pc").getValue()).getId());
      assertEquals("quote", "quote", 
		   ((Layer)defaultParameters.get("quote").getValue()).getId());
      assertEquals("orig", "orig", 
		   ((Layer)defaultParameters.get("orig").getValue()).getId());
      assertEquals("unclear", "unclear", 
		   ((Layer)defaultParameters.get("unclear").getValue()).getId());
      assertEquals("name", "entities", 
		   ((Layer)defaultParameters.get("name").getValue()).getId());
      assertEquals("sic", "sic", 
		   ((Layer)defaultParameters.get("sic").getValue()).getId());
      assertEquals("foreign", "language", 
		   ((Layer)defaultParameters.get("foreign").getValue()).getId());
      assertEquals("text type", "type", 
		   ((Layer)defaultParameters.get("ab_type").getValue()).getId());
      assertEquals("emoticon", "emoticon", 
		   ((Layer)defaultParameters.get("pc_type_emoticon").getValue()).getId());
      assertEquals("education", "education", 
		   ((Layer)defaultParameters.get("person_education").getValue()).getId());
      assertEquals("note", "comment", 
		   ((Layer)defaultParameters.get("note").getValue()).getId());
      
      // configure the deserialization
      deserializer.setParameters(defaultParameters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("test-writing.xml", g.getId());
      String[] title = g.labels("title"); 
      assertEquals(1, title.length);
      assertEquals("Writing Transcription Example", title[0]);
      assertEquals("mi", g.first("transcript_language").getLabel());
      assertEquals("2017-02-20", g.first("publication_date").getLabel());
      //assertEquals("writing", g.first("transcript_type").getLabel());

      // participants     
      Annotation[] author = g.all("who"); 
      assertEquals(1, author.length);
      assertEquals("ABCD", author[0].getId());
      assertEquals("Participant name defaults to ID", "ABCD", author[0].getLabel());

      // participant attributes - age
      assertEquals("44", author[0].first("age").getLabel());

      // participant attributes - education
      assertEquals("year 3", author[0].first("education").getLabel());

      // turns
      Annotation[] turns = g.all("turn");
      assertEquals(1, turns.length);
      assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
      //assertEquals(Double.valueOf(23.563), turns[0].getEnd().getOffset()); // TODO
      assertEquals("ABCD", turns[0].getLabel());
      assertEquals(g.getAnnotation("ABCD"), turns[0].getParent());

      // utterances
      Annotation[] utterances = g.all("utterance");
      assertEquals(5, utterances.length);
      assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
      assertEquals("inter-line space", Double.valueOf(41.0), utterances[0].getEnd().getOffset());
      assertEquals("ABCD", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals("inter-line space", Double.valueOf(41.0), utterances[1].getStart().getOffset());
      assertEquals("inter-line space", Double.valueOf(60.0), utterances[1].getEnd().getOffset());
      assertEquals("ABCD", utterances[1].getParent().getLabel());
      assertEquals(turns[0], utterances[1].getParent());


      Annotation[] words = g.all("word");
      assertEquals(28, words.length);
      assertEquals(Double.valueOf(0), words[0].getStart().getOffset());
      // System.out.println("" + Arrays.asList(Arrays.copyOfRange(words, 0, 10)));
      assertEquals("I", words[0].getLabel());
      assertEquals("inter-word space", Double.valueOf(2), words[0].getEnd().getOffset());
      assertEquals("next word start where last ends",
		   Double.valueOf(2), words[1].getStart().getOffset());
      assertEquals("next word linked to last", words[0].getEnd(), words[1].getStart());
      assertEquals("xi", words[1].getLabel());
      assertEquals("inter-word space", Double.valueOf(5), words[1].getEnd().getOffset());
      assertEquals("next word linked to last", words[1].getEnd(), words[2].getStart());
      assertEquals("xaixi", words[2].getLabel());
      assertEquals("inter-word space", Double.valueOf(11), words[2].getEnd().getOffset());
      assertEquals("next word linked to last", words[2].getEnd(), words[3].getStart());
      assertEquals("a", words[3].getLabel());
      
      assertEquals("FRIEND", words[4].getLabel()); // name
      assertEquals("xāua", words[5].getLabel());
      assertEquals("xo", words[6].getLabel());
      assertEquals("FRIEND", words[7].getLabel()); // name
      assertEquals("xi", words[8].getLabel());
      assertEquals("xā", words[9].getLabel());
      // pc: xxx
      assertEquals("wxāxaxi", words[10].getLabel());
      // pc: :-)
      assertEquals("xaxaxixi .", words[11].getLabel()); // regularized

      // figure / free writing / q
      assertEquals("Text", words[12].getLabel());
      assertEquals("in", words[13].getLabel());
      assertEquals("a", words[14].getLabel());
      assertEquals("balloon", words[15].getLabel());

      // free writing
      assertEquals("I", words[16].getLabel());
      assertEquals("xi", words[17].getLabel());
      assertEquals("xaixi", words[18].getLabel());
      assertEquals("a", words[19].getLabel());
      assertEquals("something", words[20].getLabel()); // unclear
      assertEquals("es", words[21].getLabel());    // foreign
      assertEquals("medio", words[22].getLabel()); // foreign
      assertEquals("rara", words[23].getLabel()); // sic
      assertEquals("xi", words[24].getLabel());
      assertEquals("xā", words[25].getLabel());
      // quote
      assertEquals("wxāxaxi", words[26].getLabel());
      assertEquals("xaxaxixi .", words[27].getLabel());

      // entities
      Annotation[] entities = g.all("entities");
      assertEquals(2, entities.length);
      
      assertEquals("name", entities[0].getLabel());
      assertTrue(entities[0].tags(words[4]));

      assertEquals("name", entities[1].getLabel());
      assertTrue(entities[1].tags(words[7]));

      Annotation[] pc = g.all("pc");
      assertEquals(1, pc.length);
      assertEquals("xxx", pc[0].getLabel());
      assertEquals(words[9].getEnd(), pc[0].getStart());

      Annotation[] emoticon = g.all("emoticon");
      assertEquals(1, emoticon.length);
      assertEquals(":-)", emoticon[0].getLabel());
      assertEquals(words[10].getEnd(), emoticon[0].getStart());

      Annotation[] comment = g.all("comment");
      assertEquals(1, comment.length);
      assertEquals("This is a comment", comment[0].getLabel());
      assertEquals(words[11].getEnd(), comment[0].getStart());

      Annotation[] figure = g.all("figure");
      assertEquals(1, figure.length);
      assertEquals("photo", figure[0].getLabel());
      assertEquals(words[12].getStart(), figure[0].getStart());

      Annotation[] unclear = g.all("unclear");
      assertEquals(1, unclear.length);
      assertEquals("unclear (medium)", unclear[0].getLabel());
      assertEquals(words[20].getStart(), unclear[0].getStart());

      Annotation[] language = g.all("language");
      assertEquals(1, language.length);
      assertEquals("es", language[0].getLabel());
      Annotation[] languageWords = language[0].includedAnnotationsOn("word");
      assertEquals(2, languageWords.length);
      assertEquals("es", languageWords[0].getLabel());
      assertEquals("medio", languageWords[1].getLabel());

      Annotation[] sic = g.all("sic");
      assertEquals(1, sic.length);
      assertEquals("sic", sic[0].getLabel());
      assertEquals(words[23].getStart(), sic[0].getStart());

      Annotation[] orig = g.all("orig");
      assertEquals(1, orig.length);
      assertEquals("xuxaxixi", orig[0].getLabel());
      assertTrue("orig choice tags corrected word", orig[0].tags(words[11]));
      
      // check all annotations have 'manual' confidence
      for (Annotation a : g.getAnnotationsById().values()) {
         assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

   }

   @Test public void wav() 
      throws Exception
   {
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("scribe", "Transcriber", 0, true, true, true),
	 new Layer("transcript_language", "Graph language", 0, false, false, true),
	 new Layer("transcript_version_date", "Version Date", 0, false, false, true),
	 new Layer("publication_date", "Publication Date", 0, false, false, true),
	 new Layer("transcript_program", "Program", 0, false, false, true),
	 new Layer("title", "Title", 0, false, false, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("sex", "Sex", 0, false, false, true, "who", true),
	 new Layer("age", "Age", 0, false, false, true, "who", true),
	 new Layer("dob", "Birth Date", 0, false, false, true, "who", true),
	 new Layer("topic", "Topic", 2, true, false, false),
	 new Layer("comment", "Comment", 2, true, false, true),
	 new Layer("noise", "Noise", 2, true, false, true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("entities", "Entities", 2, true, false, false, "turn", true),
	 new Layer("language", "Language", 2, true, false, false, "turn", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true),
	 new Layer("lexical", "Lexical", 0, true, false, false, "word", true));

      // access file
      NamedStream[] streams = {
	 new NamedStream(new File(getDir(), "test-cmc.xml")),
	 new NamedStream(new File(getDir(), "test-cmc.wav"))
      };
      
      // create deserializer
      TEIDeserializer deserializer = new TEIDeserializer();

      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals("Configuration parameters" + configuration, 12, deserializer.configure(configuration, schema).size());      
      assertEquals("comment", "comment", 
		   ((Layer)configuration.get("commentLayer").getValue()).getId());
      assertEquals("language", "language", 
		   ((Layer)configuration.get("languageLayer").getValue()).getId());
      assertEquals("lexical", "lexical", 
		   ((Layer)configuration.get("lexicalLayer").getValue()).getId());
      assertEquals("entities", "entities", 
		   ((Layer)configuration.get("entityLayer").getValue()).getId());
      assertEquals("scribe", "scribe", 
		   ((Layer)configuration.get("scribeLayer").getValue()).getId());
      assertEquals("transcript_version_date", "transcript_version_date", 
		   ((Layer)configuration.get("versionDateLayer").getValue()).getId());
      assertEquals("publication_date", "publication_date", 
		   ((Layer)configuration.get("publicationDateLayer").getValue()).getId());
      assertEquals("transcript_language", "transcript_language", 
		   ((Layer)configuration.get("transcriptLanguageLayer").getValue()).getId());
      assertEquals("sex", "sex", 
		   ((Layer)configuration.get("sexLayer").getValue()).getId());
      assertEquals("age", "age", 
		   ((Layer)configuration.get("ageLayer").getValue()).getId());
      assertEquals("birthdate", "dob", 
		   ((Layer)configuration.get("birthLayer").getValue()).getId());

      // load the stream
      ParameterSet defaultParameters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(5, defaultParameters.size());
      assertNull("no url layer",
		   defaultParameters.get("idnoUrl").getValue());
      assertEquals("addressingTerm", "entities", 
		   ((Layer)defaultParameters.get("addressingTerm").getValue()).getId());
      assertEquals("addressee", "entities", 
		   ((Layer)defaultParameters.get("addressee").getValue()).getId());
      assertEquals("addressMarker", "entities", 
		   ((Layer)defaultParameters.get("addressMarker").getValue()).getId());
      assertEquals("emoticon", "entities", 
		   ((Layer)defaultParameters.get("emoticon").getValue()).getId());
      
      // configure the deserialization
      deserializer.setParameters(defaultParameters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("test-cmc.xml", g.getId());
      String[] title = g.labels("title"); 
      assertEquals(1, title.length);
      assertEquals("Computer-Mediated Communication Example", title[0]);

      // units
      assertEquals("Associated WAV makes units seconds rather than characters",
		   Constants.UNIT_SECONDS, g.getOffsetUnits());
      assertEquals("Length is based on media",
       		   Double.valueOf(5.2941875), g.getEnd().getOffset());
      
      // check all annotations have 'manual' confidence
      for (Annotation a : g.getAnnotationsById().values()) {
         assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }
   }

   @Test public void mp3() 
      throws Exception
   {
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("scribe", "Transcriber", 0, true, true, true),
	 new Layer("transcript_language", "Graph language", 0, false, false, true),
	 new Layer("transcript_version_date", "Version Date", 0, false, false, true),
	 new Layer("publication_date", "Publication Date", 0, false, false, true),
	 new Layer("transcript_program", "Program", 0, false, false, true),
	 new Layer("title", "Title", 0, false, false, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("sex", "Sex", 0, false, false, true, "who", true),
	 new Layer("age", "Age", 0, false, false, true, "who", true),
	 new Layer("dob", "Birth Date", 0, false, false, true, "who", true),
	 new Layer("topic", "Topic", 2, true, false, false),
	 new Layer("comment", "Comment", 2, true, false, true),
	 new Layer("noise", "Noise", 2, true, false, true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("entities", "Entities", 2, true, false, false, "turn", true),
	 new Layer("language", "Language", 2, true, false, false, "turn", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true),
	 new Layer("lexical", "Lexical", 0, true, false, false, "word", true));

      // access file
      NamedStream[] streams = {
	 new NamedStream(new File(getDir(), "test-cmc.xml")),
	 new NamedStream(new File(getDir(), "test-cmc.mp3"))
      };
      
      // create deserializer
      TEIDeserializer deserializer = new TEIDeserializer();

      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals("Configuration parameters" + configuration, 12, deserializer.configure(configuration, schema).size());      
      assertEquals("comment", "comment", 
		   ((Layer)configuration.get("commentLayer").getValue()).getId());
      assertEquals("language", "language", 
		   ((Layer)configuration.get("languageLayer").getValue()).getId());
      assertEquals("lexical", "lexical", 
		   ((Layer)configuration.get("lexicalLayer").getValue()).getId());
      assertEquals("entities", "entities", 
		   ((Layer)configuration.get("entityLayer").getValue()).getId());
      assertEquals("scribe", "scribe", 
		   ((Layer)configuration.get("scribeLayer").getValue()).getId());
      assertEquals("transcript_version_date", "transcript_version_date", 
		   ((Layer)configuration.get("versionDateLayer").getValue()).getId());
      assertEquals("publication_date", "publication_date", 
		   ((Layer)configuration.get("publicationDateLayer").getValue()).getId());
      assertEquals("transcript_language", "transcript_language", 
		   ((Layer)configuration.get("transcriptLanguageLayer").getValue()).getId());
      assertEquals("sex", "sex", 
		   ((Layer)configuration.get("sexLayer").getValue()).getId());
      assertEquals("age", "age", 
		   ((Layer)configuration.get("ageLayer").getValue()).getId());
      assertEquals("birthdate", "dob", 
		   ((Layer)configuration.get("birthLayer").getValue()).getId());

      // load the stream
      ParameterSet defaultParameters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(5, defaultParameters.size());
      assertNull("no url layer",
		   defaultParameters.get("idnoUrl").getValue());
      assertEquals("addressingTerm", "entities", 
		   ((Layer)defaultParameters.get("addressingTerm").getValue()).getId());
      assertEquals("addressee", "entities", 
		   ((Layer)defaultParameters.get("addressee").getValue()).getId());
      assertEquals("addressMarker", "entities", 
		   ((Layer)defaultParameters.get("addressMarker").getValue()).getId());
      assertEquals("emoticon", "entities", 
		   ((Layer)defaultParameters.get("emoticon").getValue()).getId());
      
      // configure the deserialization
      deserializer.setParameters(defaultParameters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("Associated MP3 makes units seconds rather than characters",
		   Constants.UNIT_SECONDS, g.getOffsetUnits());
      // TODO assertEquals("Length is based on media",
      // 		   Double.valueOf(5.32), g.getEnd().getOffset());
      
      // check all annotations have 'manual' confidence
      for (Annotation a : g.getAnnotationsById().values()) {
         assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }
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
      org.junit.runner.JUnitCore.main("nzilbb.ag.test.TestTEIDeserializer");
   }
}
