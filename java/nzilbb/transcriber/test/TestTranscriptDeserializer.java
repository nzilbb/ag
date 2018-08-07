//
// Copyright 2016 New Zealand Institute of Language, Brain and Behaviour, 
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
import nzilbb.transcriber.*;

// TODO add a test for turns with no speaker assigned.
public class TestTranscriptDeserializer
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
      TranscriptDeserializer deserializer = new TranscriptDeserializer();

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
      assertEquals(new Double(0.0), turns[0].getStart().getOffset());
      assertEquals(new Double(23.563), turns[0].getEnd().getOffset());
      assertEquals("mop03-2b", turns[0].getLabel());
      assertEquals(g.getAnnotation("spk2"), turns[0].getParent());
      assertEquals(new Double(302.834), turns[24].getStart().getOffset());
      assertEquals(new Double(304.334), turns[24].getEnd().getOffset());
      assertEquals("Interviewer", turns[24].getLabel());
      assertEquals(g.getAnnotation("spk1"), turns[24].getParent());

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(140, utterances.length);
      assertEquals(new Double(0.0), utterances[0].getStart().getOffset());
      assertEquals(new Double(5.75), utterances[0].getEnd().getOffset());
      assertEquals("mop03-2b", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals(new Double(5.75), utterances[1].getStart().getOffset());
      assertEquals(new Double(6.907), utterances[1].getEnd().getOffset());
      assertEquals("mop03-2b", utterances[1].getParent().getLabel());

      
      Annotation[] words = g.list("word");
      assertEquals(new Double(0), words[0].getStart().getOffset());
      // System.out.println("" + Arrays.asList(Arrays.copyOfRange(words, 0, 10)));
      assertEquals("and", words[0].getLabel());
      assertEquals("ah .", words[1].getLabel());
      assertEquals("Cyril", words[2].getLabel());
      assertEquals("would", words[3].getLabel());
      assertEquals("arrive", words[4].getLabel());
      assertEquals("at", words[5].getLabel());
      assertEquals("the", words[6].getLabel());
      assertEquals("door", words[7].getLabel());
      assertEquals(new Double(5.75), words[7].getEnd().getOffset());

      // topic
      Annotation[] topics = g.list("topic");
      assertEquals(9, topics.length);

      assertEquals(new Double(0.0), topics[0].getStart().getOffset());
      assertEquals(new Double(23.563), topics[0].getEnd().getOffset());
      assertEquals("teen-decadeplus-friend-general", topics[0].getLabel());
      assertEquals(g, topics[0].getParent());

      assertEquals(new Double(23.563), topics[1].getStart().getOffset());
      assertEquals(new Double(183.995), topics[1].getEnd().getOffset());
      assertEquals("teen-decadeplus-me-event", topics[1].getLabel());
      assertEquals(g, topics[1].getParent());

      assertEquals(new Double(295.115), topics[8].getStart().getOffset());
      assertEquals(new Double(306.920), topics[8].getEnd().getOffset());
      assertEquals("teen-decadeplus-friend-general", topics[8].getLabel());
      assertEquals(g, topics[8].getParent());

      // noise
      Annotation[] noises = g.list("noise");
      assertEquals(4, noises.length);

      assertEquals(new Double(174.168), noises[0].getStart().getOffsetMin());
      assertEquals(new Double(177.59), noises[0].getEnd().getOffsetMax());
      assertEquals("and -", noises[0].getStart().endOf("word").iterator().next().getLabel());
      assertEquals("interviewer: clears throat", noises[0].getLabel());
      assertEquals("--", noises[0].getEnd().startOf("word").iterator().next().getLabel());
      assertEquals(g, noises[0].getParent());

      assertEquals(new Double(185.339), noises[1].getStart().getOffset());
      assertEquals(new Double(185.339), noises[1].getEnd().getOffset());
      assertEquals("both laugh", noises[1].getLabel());
      assertEquals(g, noises[1].getParent());

      assertEquals(new Double(233.727), noises[3].getStart().getOffsetMin());
      assertEquals(new Double(236.946), noises[3].getEnd().getOffsetMax());
      assertEquals("microphone movement noise", noises[3].getLabel());
      assertEquals(g, noises[3].getParent());

      // comment
      Annotation[] comments = g.list("comment");
      assertEquals(6, comments.length);

      assertEquals(new Double(55.444), comments[0].getStart().getOffsetMin());
      assertEquals(new Double(60.101), comments[0].getEnd().getOffsetMax());
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
      TranscriptDeserializer deserializer = new TranscriptDeserializer();

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
      assertEquals(new Double(0.0), turns[0].getStart().getOffset());
      assertEquals(new Double(23.563), turns[0].getEnd().getOffset());
      assertEquals("mop03-2b", turns[0].getLabel());
      assertEquals(g.getAnnotation("spk2"), turns[0].getParent());
      assertEquals(new Double(302.834), turns[24].getStart().getOffset());
      assertEquals(new Double(304.334), turns[24].getEnd().getOffset());
      assertEquals("Interviewer", turns[24].getLabel());
      assertEquals(g.getAnnotation("spk1"), turns[24].getParent());

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(140, utterances.length);
      assertEquals(new Double(0.0), utterances[0].getStart().getOffset());
      assertEquals(new Double(5.75), utterances[0].getEnd().getOffset());
      assertEquals("mop03-2b", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals(new Double(5.75), utterances[1].getStart().getOffset());
      assertEquals(new Double(6.907), utterances[1].getEnd().getOffset());
      assertEquals("mop03-2b", utterances[1].getParent().getLabel());
      
      Annotation[] words = g.list("word");
      assertEquals(new Double(0), words[0].getStart().getOffset());
      // System.out.println("" + Arrays.asList(Arrays.copyOfRange(words, 0, 10)));
      assertEquals("and", words[0].getLabel());
      assertEquals("ah .", words[1].getLabel());
      assertEquals("Cyril", words[2].getLabel());
      assertEquals("would", words[3].getLabel());
      assertEquals("arrive", words[4].getLabel());
      assertEquals("at", words[5].getLabel());
      assertEquals("the", words[6].getLabel());
      assertEquals("door", words[7].getLabel());
      assertEquals(new Double(5.75), words[7].getEnd().getOffset());

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
      org.junit.runner.JUnitCore.main("nzilbb.ag.test.TestTranscriptDeserializer");
   }
}
