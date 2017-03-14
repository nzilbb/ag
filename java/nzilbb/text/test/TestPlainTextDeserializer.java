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
import nzilbb.ag.serialize.util.NamedStream;
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
      assertEquals("Configuration parameters" + configuration, 4, deserializer.configure(configuration, schema).size());      
      assertEquals("comment", "comment", 
		   ((Layer)configuration.get("commentLayer").getValue()).getId());
      assertEquals("noise", "noise", 
		   ((Layer)configuration.get("noiseLayer").getValue()).getId());
      assertEquals("lexical", "lexical", 
		   ((Layer)configuration.get("lexicalLayer").getValue()).getId());
      assertEquals("pronounce", "pronounce", 
		   ((Layer)configuration.get("pronounceLayer").getValue()).getId());

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
      assertEquals(new Double(0.0), turns[0].getStart().getOffset());
      //assertEquals(new Double(23.563), turns[0].getEnd().getOffset()); // TODO
      assertEquals("YoungModern", turns[0].getLabel());
      assertEquals(g.my("who"), turns[0].getParent());

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(1, utterances.length);
      assertEquals(new Double(0.0), utterances[0].getStart().getOffset());
      assertEquals("YoungModern", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());
      
      Annotation[] words = g.list("word");
      assertEquals(new Double(0), words[0].getStart().getOffset());
      // System.out.println("" + Arrays.asList(Arrays.copyOfRange(words, 0, 10)));
      assertEquals("Most", words[0].getLabel());
      assertEquals("inter-word space", new Double(5), words[0].getEnd().getOffset());
      assertEquals("next word start where last ends",
		   new Double(5), words[1].getStart().getOffset());
      assertEquals("next word linked to last", words[0].getEnd(), words[1].getStart());
      assertEquals("of", words[1].getLabel());
      assertEquals("inter-word space", new Double(8), words[1].getEnd().getOffset());
      assertEquals("next word linked to last", words[1].getEnd(), words[2].getStart());
      assertEquals("us", words[2].getLabel());
      assertEquals("inter-word space", new Double(11), words[2].getEnd().getOffset());
      assertEquals("next word linked to last", words[2].getEnd(), words[3].getStart());
      assertEquals("have", words[3].getLabel());
      assertEquals("inter-word space", new Double(16), words[3].getEnd().getOffset());
      assertEquals("next word linked to last", words[3].getEnd(), words[4].getStart());
      assertEquals("some", words[4].getLabel());
      assertEquals("inter-word space", new Double(21), words[4].getEnd().getOffset());
      assertEquals("next word linked to last", words[4].getEnd(), words[5].getStart());
      assertEquals("family", words[5].getLabel());
      assertEquals("inter-word space", new Double(28), words[5].getEnd().getOffset());
      assertEquals("next word linked to last", words[5].getEnd(), words[6].getStart());
      assertEquals("members", words[6].getLabel());
      assertEquals("inter-word space", new Double(36), words[6].getEnd().getOffset());
      assertEquals("next word linked to last", words[6].getEnd(), words[7].getStart());
      assertEquals("like", words[7].getLabel());
      assertEquals(new Double(41), words[7].getEnd().getOffset());

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
