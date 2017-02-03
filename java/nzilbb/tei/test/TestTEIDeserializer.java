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
      assertEquals("Configuration parameters" + configuration, 9, deserializer.configure(configuration, schema).size());      
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

      // load the stream
      ParameterSet defaultParameters = deserializer.load(streams, schema);
      for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(1, defaultParameters.size());
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
      String[] author = g.labels("who"); 
      assertEquals(1, author.length);
      assertEquals("Aaliyah", author[0]);

      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(1, turns.length);
      assertEquals(new Double(0.0), turns[0].getStart().getOffset());
      //assertEquals(new Double(23.563), turns[0].getEnd().getOffset()); // TODO
      assertEquals("Aaliyah", turns[0].getLabel());
      assertEquals(g.my("who"), turns[0].getParent());

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(45, utterances.length);
      assertEquals(new Double(0.0), utterances[0].getStart().getOffset());
      assertEquals("inter-line space", new Double(10.0), utterances[0].getEnd().getOffset());
      assertEquals("Aaliyah", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals("inter-line space", new Double(10.0), utterances[1].getStart().getOffset());
      assertEquals("inter-line space", new Double(31.0), utterances[1].getEnd().getOffset());
      assertEquals("Aaliyah", utterances[1].getParent().getLabel());
      
      Annotation[] words = g.list("word");
      assertEquals(new Double(0), words[0].getStart().getOffset());
      // System.out.println("" + Arrays.asList(Arrays.copyOfRange(words, 0, 10)));
      assertEquals("[Aaliyah]", words[0].getLabel());
      assertEquals("inter-word space", new Double(10), words[0].getEnd().getOffset());
      assertEquals("next word start where last ends",
		   new Double(10), words[1].getStart().getOffset());
      assertEquals("next word linked to last", words[0].getEnd(), words[1].getStart());
      assertEquals("Yo", words[1].getLabel());
      assertEquals("inter-word space", new Double(13), words[1].getEnd().getOffset());
      assertEquals("next word linked to last", words[1].getEnd(), words[2].getStart());
      assertEquals("Rodney", words[2].getLabel());
      assertEquals("inter-word space", new Double(20), words[2].getEnd().getOffset());
      assertEquals("next word linked to last", words[2].getEnd(), words[3].getStart());
      assertEquals("you", words[3].getLabel());
      assertEquals("inter-word space", new Double(24), words[3].getEnd().getOffset());
      assertEquals("next word linked to last", words[3].getEnd(), words[4].getStart());
      assertEquals("ready?", words[4].getLabel());
      assertEquals("inter-word space", new Double(31), words[4].getEnd().getOffset());
      assertEquals("next word linked to last", words[4].getEnd(), words[5].getStart());
      assertEquals("Cause", words[5].getLabel());
      assertEquals("inter-word space", new Double(37), words[5].getEnd().getOffset());
      assertEquals("next word linked to last", words[5].getEnd(), words[6].getStart());
      assertEquals("I’m", words[6].getLabel());
      assertEquals("inter-word space", new Double(41), words[6].getEnd().getOffset());
      assertEquals("next word linked to last", words[6].getEnd(), words[7].getStart());
      assertEquals("ready", words[7].getLabel());
      assertEquals(new Double(47), words[7].getEnd().getOffset());

      // lg
      Annotation[] verses = g.list("lg");
      assertEquals(16, verses.length);

      assertEquals(new Double(0), verses[0].getStart().getOffset());
      assertEquals(new Double(59), verses[0].getEnd().getOffset());
      assertEquals("lg with no type is labelled 'lg'", "lg", verses[0].getLabel());
      assertEquals(turns[0], verses[0].getParent());

      assertEquals(new Double(59), verses[1].getStart().getOffset());
      assertEquals(new Double(214), verses[1].getEnd().getOffset());
      assertEquals("lg", verses[1].getLabel());
      assertEquals(turns[0], verses[1].getParent());

      assertEquals(new Double(214), verses[2].getStart().getOffset());
      assertEquals(new Double(214), verses[2].getEnd().getOffset());
      assertEquals("lg", verses[2].getLabel());
      assertEquals(turns[0], verses[2].getParent());

      assertEquals(new Double(214), verses[3].getStart().getOffset());
      assertEquals(new Double(332), verses[3].getEnd().getOffset());
      assertEquals("lg with type is labelled with type", "chorus", verses[3].getLabel());
      assertEquals(turns[0], verses[3].getParent());

      assertEquals(0, g.list("entities").length);
      assertEquals(0, g.list("language").length);
      assertEquals(0, g.list("lexical").length);

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
