//
// Copyright 2020 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.htk.mlf.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.io.File;
import java.net.URL;
import nzilbb.configure.ParameterSet;
import nzilbb.configure.Parameter;
import nzilbb.ag.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.htk.mlf.*;

public class TestMlfDeserializer {
   
   @Test public void basicAlignment()  throws Exception {
      Schema schema = new Schema(
         "participant", "turn", "utterance", "word",
	 new Layer("participant", "Participant")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(true).setPeersOverlap(true).setSaturated(true),
         new Layer("turn", "Turn")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false)
         .setParentId("participant"),
         new Layer("utterance", "Utterance")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(true)
         .setParentId("turn"),
         new Layer("word", "Word")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false)
         .setParentId("turn"),
         new Layer("phone", "Phone")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(true)
         .setParentId("word"),
         new Layer("score", "Score")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true)
         .setParentId("phone"));
      
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "test.mlf")) };
      
      // create deserializer
      MlfDeserializer deserializer = new MlfDeserializer();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(0, deserializer.configure(configuration, schema).size());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(1, defaultParamaters.size());
      assertEquals("phones mapping", "phone", 
		   ((Layer)defaultParamaters.get("phones").getValue()).getId());
      
      // configure the deserialization
      deserializer.setParameters(defaultParamaters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      assertEquals("multiple graphs: " + graphs.length, 2, graphs.length);

      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }

      Graph g = graphs[0];
      assertEquals("first fragment name", "AP511_MikeThorpe__1.373-7.131", g.getId());
      assertEquals("first fragment source graph", "AP511_MikeThorpe.eaf", g.sourceGraph().getId());
      assertEquals("first fragment startTime tag",
                   Double.valueOf(1.373), (Double)g.get("@startTime"));
      assertNotNull("first fragment utterance layer", g.getLayer("utterance"));
      assertNotNull("first fragment word layer", g.getLayer("word"));
      assertNotNull("first fragment phone layer", g.getLayer("phone"));
      assertNull("first fragment no score layer", g.getLayer("score"));

      // defining annotation
      Annotation utterance = g.first("utterance");
      assertNotNull("Utterance", utterance);
      assertEquals("utterance has predictable label",
                   "AP511_MikeThorpe__1.373-7.131", utterance.getLabel());
      assertEquals("utterance start", Double.valueOf(0.0), utterance.getStart().getOffset());
      assertEquals("utterance end", Double.valueOf(5.758), utterance.getEnd().getOffset());
      
      // labels and times
      Annotation[] words = g.all("word");
      String[] correctWords = { "ah", "w~", "well", "i", "have", "a", "m~", "fairly", "vivid",
         "recollection", "um", "of", "all", "of", "the", "major", "quakes", "and" }; 
      Double[] wordStarts = { 0.07, 0.19, 0.57, 0.73, 0.83, 1.00, 2.01, 2.22, 2.57,
         2.96, 3.80, 4.19, 4.35, 4.50, 4.58, 4.65, 4.95, 5.35}; 
      Double[] wordEnds = { 0.19, 0.4, 0.73, 0.83, 1.0, 1.24, 2.22, 2.57, 2.96,
         3.8, 4.19, 4.35, 4.5, 4.58, 4.65, 4.95, 5.35, 5.64,  }; 
      assertEquals(
         "correct number of words: " + Arrays.asList(words),
         correctWords.length, words.length);

      for (int i = 0; i < words.length; i++) {
         assertEquals("label for word " + i,
                      correctWords[i], words[i].getLabel());
         assertEquals("confidence for word " + i,
                      Constants.CONFIDENCE_AUTOMATIC, (int)words[i].getConfidence());
         assertEquals("offset for word start " + i,
                      wordStarts[i], words[i].getStart().getOffset());
         assertEquals("confidence for word start " + i,
                      Constants.CONFIDENCE_AUTOMATIC, (int)words[i].getStart().getConfidence());
         assertEquals("offset for word end " + i,
                      wordEnds[i], words[i].getEnd().getOffset());
         assertEquals("confidence for word end " + i,
                      Constants.CONFIDENCE_AUTOMATIC, (int)words[i].getEnd().getConfidence());
      }

      Annotation[] phones = g.all("phone");
      String[] correctPhones = {
         "_#",
         "_w", "_@",
         "_w", "_E", "_l",
         "_2",
         "_h", "_{", "_v",
         "_1",
         "_m", "_@",
         "_f", "_8", "_l", "_I",
         "_v", "_I", "_v", "_I", "_d",
         "_r", "_E", "_k", "_@", "_l", "_E", "_k", "_S", "_H",
         "_@", "_m",
         "_Q", "_v",
         "_$", "_l",
         "_Q", "_v",
         "_D", "_i",
         "_m", "_1", "__", "_@",
         "_k", "_w", "_1", "_k", "_s",
         "_{", "_n", "_d"}; 
      for (int i = 0; i < phones.length; i++) {
         assertEquals("label for phone " + i,
                      correctPhones[i], phones[i].getLabel());
         assertEquals("confidence for phone " + i,
                      Constants.CONFIDENCE_AUTOMATIC, (int)phones[i].getConfidence());
         assertEquals("confidence for phone start " + i,
                      Constants.CONFIDENCE_AUTOMATIC, (int)phones[i].getStart().getConfidence());
         assertEquals("confidence for phone end " + i,
                      Constants.CONFIDENCE_AUTOMATIC, (int)phones[i].getEnd().getConfidence());
         assertTrue("word includes phone " + i,
                    phones[i].getParent().includes(phones[i]));
      }

      g = graphs[1];
      assertEquals("seconds fragment name", "AP511_MikeThorpe__7.131-13.887", g.getId());
      assertEquals("first fragment source graph", "AP511_MikeThorpe.eaf", g.sourceGraph().getId());
      assertEquals("first fragment startTime tag",
                   Double.valueOf(7.131), (Double)g.get("@startTime"));
   }

   // TODO scores

   // TODO no phones layer (i.e. word alignment only)

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
      org.junit.runner.JUnitCore.main("nzilbb.htk.mlf.test.TestMlfDeserializer");
   }
}
