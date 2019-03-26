//
// Copyright 2019 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.webvtt.test;
	      
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
import nzilbb.webvtt.*;

public class TestVttDeserializer
{
   @Test public void youtube() 
      throws Exception
   {
      Schema schema = new Schema("who", "turn", "utterance", "word",
	 new Layer("transcript_language", "Language", 0, true, true, true),
	 new Layer("kind", "Kind", 0, false, false, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true));

      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "youtube.vtt")) };
      
      // create deserializer
      VttDeserializer deserializer = new VttDeserializer();

      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals("Configuration parameters" + configuration, 0,
		   deserializer.configure(configuration, schema).size());      

      // load the stream
      ParameterSet defaultParameters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(2, defaultParameters.size());
      assertEquals("language", "transcript_language", 
		   ((Layer)defaultParameters.get("Language").getValue()).getId());
      assertEquals("kind", "kind", 
		   ((Layer)defaultParameters.get("Kind").getValue()).getId());
      
      // configure the deserialization
      deserializer.setParameters(defaultParameters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("youtube.vtt", g.getId());
      assertEquals("time units", Constants.UNIT_SECONDS, g.getOffsetUnits());

      // meta data
      assertEquals("graph meta data", 
		   "en-GB", g.my("transcript_language").getLabel());
      assertEquals("graph meta data", 
		   "captions", g.my("kind").getLabel());

      // participants     
      Annotation[] authors = g.list("who"); 
      assertEquals(1, authors.length);
      assertEquals("speaker", authors[0].getLabel());

      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(1, turns.length);
      assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
      assertEquals(Double.valueOf(2782.55), turns[0].getEnd().getOffset());

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(Double.valueOf(1.849), utterances[0].getStart().getOffset());
      assertEquals(Double.valueOf(6.43), utterances[0].getEnd().getOffset());

      assertEquals(Double.valueOf(6.43), utterances[1].getStart().getOffset());
      assertEquals(Double.valueOf(9.879), utterances[1].getEnd().getOffset());

      assertEquals(Double.valueOf(2777.96), utterances[utterances.length-1].getStart().getOffset());
      assertEquals(Double.valueOf(2782.55), utterances[utterances.length-1].getEnd().getOffset());

      // words
      Annotation[] words = g.list("word");
      assertEquals(7728, words.length);
      String[] checkWords = {
	 "Before","we","move","to","the","first","question","to","the",
	 "First","Minister,","I","invite","the","First","Minister",
	 "to","make","a","few","remarks","following","the","tragic",
	 "events","in","Christchurch","in","New","Zealand.",
	 "The","First","Minister","(Nicola","Sturgeon):"};
      for (int w = 0; w < checkWords.length; w++)
      {
	 assertEquals("check word " + w + ": " + checkWords[w], checkWords[w], words[w].getLabel());
      } // next word
   }

   @Test public void noMetaData() 
      throws Exception
   {
      Schema schema = new Schema("who", "turn", "utterance", "word",
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true));

      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "youtube.vtt")) };
      
      // create deserializer
      VttDeserializer deserializer = new VttDeserializer();

      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals("Configuration parameters" + configuration, 0,
		   deserializer.configure(configuration, schema).size());

      // load the stream
      ParameterSet defaultParameters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(2, defaultParameters.size());
      assertNull("language", (Layer)defaultParameters.get("Language").getValue());
      assertNull("kind", (Layer)defaultParameters.get("Kind").getValue());
      
      // configure the deserialization
      deserializer.setParameters(defaultParameters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("youtube.vtt", g.getId());
      assertEquals("time units", Constants.UNIT_SECONDS, g.getOffsetUnits());

      // meta data
      assertNull("graph meta data", g.my("transcript_language"));
      assertNull("graph meta data", g.my("kind"));

      // participants     
      Annotation[] authors = g.list("who"); 
      assertEquals(1, authors.length);
      assertEquals("speaker", authors[0].getLabel());

      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(1, turns.length);
      assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
      assertEquals(Double.valueOf(2782.55), turns[0].getEnd().getOffset());

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(Double.valueOf(1.849), utterances[0].getStart().getOffset());
      assertEquals(Double.valueOf(6.43), utterances[0].getEnd().getOffset());

      assertEquals(Double.valueOf(6.43), utterances[1].getStart().getOffset());
      assertEquals(Double.valueOf(9.879), utterances[1].getEnd().getOffset());

      assertEquals(Double.valueOf(2777.96), utterances[utterances.length-1].getStart().getOffset());
      assertEquals(Double.valueOf(2782.55), utterances[utterances.length-1].getEnd().getOffset());

      // utterances
      Annotation[] words = g.list("word");
      assertEquals(7728, words.length);
      String[] checkWords = {
	 "Before","we","move","to","the","first","question","to","the",
	 "First","Minister,","I","invite","the","First","Minister",
	 "to","make","a","few","remarks","following","the","tragic",
	 "events","in","Christchurch","in","New","Zealand.",
	 "The","First","Minister","(Nicola","Sturgeon):"};
      for (int w = 0; w < checkWords.length; w++)
      {
	 assertEquals("check word " + w + ": " + checkWords[w], checkWords[w], words[w].getLabel());
      } // next word
   }

   @Test public void noWordLayer()
      throws Exception
   {
      Schema schema = new Schema("who", "turn", "utterance", null,
	 new Layer("transcript_language", "Language", 0, true, true, true),
	 new Layer("kind", "Kind", 0, false, false, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true));

      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "youtube.vtt")) };
      
      // create deserializer
      VttDeserializer deserializer = new VttDeserializer();

      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals("Configuration parameters" + configuration, 1,
		   deserializer.configure(configuration, schema).size());      

      // load the stream
      ParameterSet defaultParameters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(2, defaultParameters.size());
      assertEquals("language", "transcript_language", 
		   ((Layer)defaultParameters.get("Language").getValue()).getId());
      assertEquals("kind", "kind", 
		   ((Layer)defaultParameters.get("Kind").getValue()).getId());
      
      // configure the deserialization
      deserializer.setParameters(defaultParameters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("youtube.vtt", g.getId());
      assertEquals("time units", Constants.UNIT_SECONDS, g.getOffsetUnits());

      // meta data
      assertEquals("graph meta data", 
		   "en-GB", g.my("transcript_language").getLabel());
      assertEquals("graph meta data", 
		   "captions", g.my("kind").getLabel());

      // participants     
      Annotation[] authors = g.list("who"); 
      assertEquals(1, authors.length);
      assertEquals("speaker", authors[0].getLabel());

      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(1, turns.length);
      assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
      assertEquals(Double.valueOf(2782.55), turns[0].getEnd().getOffset());

      // utterances
      Annotation[] utterances = g.list("utterance");
      assertEquals(571, utterances.length);
      assertEquals(Double.valueOf(1.849), utterances[0].getStart().getOffset());
      assertEquals(Double.valueOf(6.43), utterances[0].getEnd().getOffset());

      assertEquals(Double.valueOf(6.43), utterances[1].getStart().getOffset());
      assertEquals(Double.valueOf(9.879), utterances[1].getEnd().getOffset());

      assertEquals(Double.valueOf(2777.96), utterances[utterances.length-1].getStart().getOffset());
      assertEquals(Double.valueOf(2782.55), utterances[utterances.length-1].getEnd().getOffset());

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
      org.junit.runner.JUnitCore.main("nzilbb.text.test.TestVttDeserializer");
   }
}
