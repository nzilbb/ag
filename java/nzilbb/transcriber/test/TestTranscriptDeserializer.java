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

public class TestTranscriptDeserializer
{
   @Test public void basicConversion() 
      throws Exception
   {
      Layer[] layers = {
	 new Layer("transcriber", "Transcribers", 0, true, true, true),
	 new Layer("transcript_language", "Graph language", 0, true, true, true),
	 new Layer("transcript_scribe", "Transcriber", 0, true, true, true),
	 new Layer("topic", "Topic", 2, true, false, false),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("entities", "Entities", 2, true, false, true, "turn", true),
	 new Layer("comment", "Comment", 2, true, false, true, "turn", true),
	 new Layer("noise", "Noise", 2, true, false, true, "turn", true),
	 new Layer("language", "Language", 2, true, false, true, "turn", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true),
	 new Layer("lexical", "Lexical", 0, true, false, false, "word", true),
	 new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true)
      };
      Schema schema = new Schema(layers, "who", "turn", "utterance", "word");
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "mop03-2b-05.trs")) };
      
      // create deserializer
      TranscriptDeserializer deserializer = new TranscriptDeserializer();

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      //for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      assertEquals("mop03-2b-05.trs", g.getId());
      String[] transcribers = g.labels("transcript_scribe"); 
      assertEquals(1, transcribers.length);
      assertEquals("Robert Fromont", transcribers[0]);
      String[] languages = g.labels("transcript_language"); 
      assertEquals(1, languages.length);
      assertEquals("en", languages[0]);

      // participants     
      assertEquals(2, g.list("who").length);
      assertEquals("Interviewer", g.getAnnotation("spk1").getLabel());
      assertEquals("who", g.getAnnotation("spk1").getLayerId());
      assertEquals("mop03-2b", g.getAnnotation("spk2").getLabel());
      assertEquals("who", g.getAnnotation("spk2").getLayerId());

      // participant meta data
      // assertEquals("male", g.getAnnotation("spk1").my("type").getLabel());
      // assertEquals("male", g.getAnnotation("spk2").my("type").getLabel());

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

      // assertEquals("Pre expansion ordinal", "gonna", words[321].getLabel());
      // assertEquals("Pre expansion ordinal", 322, words[321].getOrdinal());
      // assertEquals("Post expansion ordinal", "lie", words[322].getLabel());
      // assertEquals("Post expansion ordinal", 323, words[322].getOrdinal());

      // for (int i = 0; i < words.length; i++)
      // {
      // 	 assertEquals("Correct ordinal: " + words[i].getLabel(), i+1, words[i].getOrdinal());
      // }

      // // disfluency
      // assertEquals("i", words[48].getLabel());
      // assertEquals("&", words[48].my("disfluency").getLabel());
      // assertEquals("word is parent", words[48], words[48].my("disfluency").getParent());
      
      // // completion
      // assertEquals("leading completion", "em", words[111].getLabel());
      // assertEquals("leading completion", "them", words[111].my("completion").getLabel());
      // assertEquals("completion - word is parent", words[111], words[111].my("completion").getParent());
      // assertEquals("trailing completion", "havin", words[133].getLabel());
      // assertEquals("trailing completion", "having", words[133].my("completion").getLabel());

      // // expansions
      // Annotation[] expansions = g.annotations("expansion");
      // assertEquals(11, expansions.length);
      // assertEquals("going to", expansions[0].getLabel());
      // assertEquals("gonna", expansions[0].my("word").getLabel());
      // assertEquals(expansions[0].my("word"), expansions[0].getParent());
      // assertEquals("kind of", expansions[1].getLabel());
      // assertEquals("kinda", expansions[1].my("word").getLabel());
      // assertEquals("going to", expansions[2].getLabel());
      // assertEquals("gonna", expansions[2].getParent().getLabel());
      // assertEquals("going to", expansions[3].getLabel());
      // assertEquals("gonna", expansions[3].my("word").getLabel());
      // assertEquals("going to", expansions[4].getLabel());
      // assertEquals("gonna", expansions[4].my("word").getLabel());
      // assertEquals("going to", expansions[5].getLabel());
      // assertEquals("gonna", expansions[5].my("word").getLabel());
      // assertEquals("going to", expansions[6].getLabel());
      // assertEquals("gonna", expansions[6].my("word").getLabel());
      // assertEquals("got to", expansions[7].getLabel());
      // assertEquals("gotta", expansions[7].my("word").getLabel());
      // assertEquals("going to", expansions[8].getLabel());
      // assertEquals("gonna", expansions[8].my("word").getLabel());
      // assertEquals("kind of", expansions[9].getLabel());
      // assertEquals("kinda", expansions[9].my("word").getLabel());
      // assertEquals("want to", expansions[10].getLabel());
      // assertEquals("wanna", expansions[10].my("word").getLabel());

      // // gems
      // Annotation[] gems = g.annotations("gem");
      // assertEquals(11, gems.length);
      // assertEquals(new Double(0.0), gems[0].getStart().getOffset());
      // assertEquals("Picnic", gems[0].getLabel());
      // assertEquals(new Double(197.802), gems[0].getEnd().getOffset());
      // assertEquals(new Double(197.802), gems[1].getStart().getOffset());
      // assertEquals("gdc", gems[1].getLabel());
      // assertEquals(new Double(409.735), gems[1].getEnd().getOffset());
      // assertEquals(new Double(409.735), gems[2].getStart().getOffset());
      // assertEquals("birthday", gems[2].getLabel());
      // assertEquals(new Double(461.218), gems[2].getEnd().getOffset());
      // assertEquals(new Double(461.218), gems[3].getStart().getOffset());
      // assertEquals("cat", gems[3].getLabel());
      // assertEquals(new Double(509.776), gems[3].getEnd().getOffset());
      // assertEquals(new Double(509.776), gems[4].getStart().getOffset());
      // assertEquals("argument", gems[4].getLabel());
      // assertEquals(new Double(576.043), gems[4].getEnd().getOffset());
      // assertEquals(new Double(576.043), gems[5].getStart().getOffset());
      // assertEquals("directions", gems[5].getLabel());
      // assertEquals(new Double(631.759), gems[5].getEnd().getOffset());
      // assertEquals(new Double(631.759), gems[6].getStart().getOffset());
      // assertEquals("peanut", gems[6].getLabel());
      // assertEquals(new Double(652.770), gems[6].getEnd().getOffset());
      // assertEquals(new Double(652.770), gems[7].getStart().getOffset());
      // assertEquals("flower", gems[7].getLabel());
      // assertEquals(new Double(679.712), gems[7].getEnd().getOffset());
      // assertEquals(new Double(679.712), gems[8].getStart().getOffset());
      // assertEquals("vacation", gems[8].getLabel());
      // assertEquals(new Double(736.213), gems[8].getEnd().getOffset());
      // assertEquals(new Double(736.213), gems[9].getStart().getOffset());
      // assertEquals("weekend", gems[9].getLabel());
      // assertEquals(new Double(880.456), gems[9].getEnd().getOffset());
      // assertEquals(new Double(880.456), gems[10].getStart().getOffset());
      // assertEquals("Christmas", gems[10].getLabel());
      // assertEquals(new Double(950.711), gems[10].getEnd().getOffset());

      // // we know about the overlapping boundary, but it will be repaired by Normalizer
      // assertNotEquals("overlapping utterance boundary not corrected", 
      // 		   utterances.elementAt(188).getEnd(), utterances.elementAt(189).getStart());
      // assertEquals("overlapping utterance boundary not corrected", 
      // 		   new Double(877.501), utterances.elementAt(188).getEnd().getOffset());
      // assertEquals("overlapping utterance boundary not corrected", 
      // 		   new Double(877.420), utterances.elementAt(189).getStart().getOffset());

      // assertEquals("wrapped line", new Double(28.452), utterances.elementAt(4).getStart().getOffset());
      // assertEquals("wrapped line", new Double(40.360), utterances.elementAt(4).getEnd().getOffset());

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
