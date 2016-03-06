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

package nzilbb.clan.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

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
import nzilbb.clan.*;

public class TestChatDeserializer
{
   @Test public void minimalConversion() 
      throws Exception
   {
      Layer[] layers = {
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true),
	 new Layer("completion", "Completion", 2, true, false, false, "word", true),
	 new Layer("expansion", "Expansion", 0, false, false, true, "word", true),
	 new Layer("disfluency", "Disfluency", 0, false, false, true, "word", true),
	 new Layer("gem", "Gems", 2, true, false, true)
      };
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "8064.cha")) }; // TODO test griffin.cha

      // create deserializer
      ChatDeserializer deserializer = new ChatDeserializer();

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, null, layers);
//      for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }

      assertEquals("8064.cha", g.getId());

      assertEquals(2, g.getAnnotations("who").size());
      assertEquals("8064", g.getAnnotation("SUB").getLabel());
      assertEquals("who", g.getAnnotation("SUB").getLayerId());
      assertEquals("Investigator", g.getAnnotation("EXA").getLabel());
      assertEquals("who", g.getAnnotation("EXA").getLayerId());

      assertEquals(1, g.getAnnotations("turn").size());

      Vector<Annotation> utterances = g.getAnnotations("utterance");
      assertEquals(new Double(0.0), utterances.elementAt(0).getStart().getOffset());
      assertEquals(new Double(10.988), utterances.elementAt(0).getEnd().getOffset());
      assertEquals("SUB", utterances.elementAt(0).getParent().getLabel());

      assertEquals(new Double(10.988), utterances.elementAt(1).getStart().getOffset());
      assertEquals(new Double(15.673), utterances.elementAt(1).getEnd().getOffset());
      assertEquals("SUB", utterances.elementAt(1).getParent().getLabel());

      assertEquals(new Double(15.673), utterances.elementAt(2).getStart().getOffset());
      assertEquals(new Double(22.676), utterances.elementAt(2).getEnd().getOffset());
      assertEquals("SUB", utterances.elementAt(2).getParent().getLabel());

      Annotation[] words = g.annotations("word");
      assertEquals("this", words[0].getLabel());
      assertEquals("family", words[1].getLabel());
      assertEquals("of", words[2].getLabel());
      assertEquals("mice", words[3].getLabel());
      assertEquals("lived", words[4].getLabel());
      assertEquals("in", words[5].getLabel());

      // disfluency
      assertEquals("i", words[48].getLabel());
      assertEquals("&", words[48].my("disfluency").getLabel());
      
      // completion
      assertEquals("leading completion", "em", words[111].getLabel());
      assertEquals("leading completion", "them", words[111].my("completion").getLabel());
      assertEquals("trailing completion", "havin", words[133].getLabel());
      assertEquals("trailing completion", "having", words[133].my("completion").getLabel());

      // expansions
      Annotation[] expansions = g.annotations("expansion");
      assertEquals(11, expansions.length);
      assertEquals("going to", expansions[0].getLabel());
      assertEquals("gonna", expansions[0].my("word").getLabel());
      assertEquals("kind of", expansions[1].getLabel());
      assertEquals("kinda", expansions[1].my("word").getLabel());
      assertEquals("going to", expansions[2].getLabel());
      assertEquals("gonna", expansions[2].my("word").getLabel());
      assertEquals("going to", expansions[3].getLabel());
      assertEquals("gonna", expansions[3].my("word").getLabel());
      assertEquals("going to", expansions[4].getLabel());
      assertEquals("gonna", expansions[4].my("word").getLabel());
      assertEquals("going to", expansions[5].getLabel());
      assertEquals("gonna", expansions[5].my("word").getLabel());
      assertEquals("going to", expansions[6].getLabel());
      assertEquals("gonna", expansions[6].my("word").getLabel());
      assertEquals("got to", expansions[7].getLabel());
      assertEquals("gotta", expansions[7].my("word").getLabel());
      assertEquals("going to", expansions[8].getLabel());
      assertEquals("gonna", expansions[8].my("word").getLabel());
      assertEquals("kind of", expansions[9].getLabel());
      assertEquals("kinda", expansions[9].my("word").getLabel());
      assertEquals("want to", expansions[10].getLabel());
      assertEquals("wanna", expansions[10].my("word").getLabel());

      // gems
      Annotation[] gems = g.annotations("gem");
      assertEquals(11, gems.length);
      assertEquals(new Double(0.0), gems[0].getStart().getOffset());
      assertEquals("Picnic", gems[0].getLabel());
      assertEquals(new Double(197.802), gems[0].getEnd().getOffset());
      assertEquals(new Double(197.802), gems[1].getStart().getOffset());
      assertEquals("gdc", gems[1].getLabel());
      assertEquals(new Double(409.735), gems[1].getEnd().getOffset());
      assertEquals(new Double(409.735), gems[2].getStart().getOffset());
      assertEquals("birthday", gems[2].getLabel());
      assertEquals(new Double(461.218), gems[2].getEnd().getOffset());
      assertEquals(new Double(461.218), gems[3].getStart().getOffset());
      assertEquals("cat", gems[3].getLabel());
      assertEquals(new Double(509.776), gems[3].getEnd().getOffset());
      assertEquals(new Double(509.776), gems[4].getStart().getOffset());
      assertEquals("argument", gems[4].getLabel());
      assertEquals(new Double(576.043), gems[4].getEnd().getOffset());
      assertEquals(new Double(576.043), gems[5].getStart().getOffset());
      assertEquals("directions", gems[5].getLabel());
      assertEquals(new Double(631.759), gems[5].getEnd().getOffset());
      assertEquals(new Double(631.759), gems[6].getStart().getOffset());
      assertEquals("peanut", gems[6].getLabel());
      assertEquals(new Double(652.770), gems[6].getEnd().getOffset());
      assertEquals(new Double(652.770), gems[7].getStart().getOffset());
      assertEquals("flower", gems[7].getLabel());
      assertEquals(new Double(679.712), gems[7].getEnd().getOffset());
      assertEquals(new Double(679.712), gems[8].getStart().getOffset());
      assertEquals("vacation", gems[8].getLabel());
      assertEquals(new Double(736.213), gems[8].getEnd().getOffset());
      assertEquals(new Double(736.213), gems[9].getStart().getOffset());
      assertEquals("weekend", gems[9].getLabel());
      assertEquals(new Double(880.456), gems[9].getEnd().getOffset());
      assertEquals(new Double(880.456), gems[10].getStart().getOffset());
      assertEquals("Christmas", gems[10].getLabel());
      assertEquals(new Double(950.711), gems[10].getEnd().getOffset());
      
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
      org.junit.runner.JUnitCore.main("nzilbb.ag.test.TestGraph");
   }
}
