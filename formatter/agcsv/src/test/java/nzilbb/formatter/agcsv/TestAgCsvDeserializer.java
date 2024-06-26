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
package nzilbb.formatter.agcsv;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.Vector;
import java.util.Iterator;
import java.io.File;
import java.net.URL;
import nzilbb.configure.ParameterSet;
import nzilbb.configure.Parameter;
import nzilbb.ag.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.formatter.agcsv.*;

public class TestAgCsvDeserializer
{      
   @Test public void deserializeGraph() 
      throws Exception
   {
      Schema schema = new Schema(
	 "who", "turns", "utterances", "transcript",
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("turns", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("utterances", "Utterances", 2, true, false, true, "turns", true),
	 new Layer("language", "Language", 2, true, false, true, "turns", true),
	 new Layer("transcript", "Words", 2, true, false, false, "turns", true),
	 new Layer("orthography", "Orthography", 0, false, false, false, "transcript", true),
	 new Layer("lexical", "Lexical", 0, false, false, false, "transcript", true), // not present
	 new Layer("segments", "Phones", 2, true, false, true, "transcript", true)
	 );
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "test.csv")) };

      // create deserializer
      AgCsvDeserializer deserializer = new AgCsvDeserializer();

      // configure it
      ParameterSet defaultConfiguration = deserializer.configure(new ParameterSet(), schema);
      deserializer.configure(defaultConfiguration, schema);

      // load the stream
      ParameterSet defaultParameters = deserializer.load(streams, schema);
//      for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());

      // configure the deserialization
      deserializer.setParameters(defaultParameters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }

      assertEquals("test", g.getId());

      // layers
      for (Layer gLayer : g.getSchema().getLayers().values())
      {
	 Layer sLayer = schema.getLayer(gLayer.getId());
	 assertNotNull(gLayer.getId(), sLayer);
	 assertEquals(sLayer.getId(), gLayer.getId());
	 assertEquals(sLayer.getParentId(), gLayer.getParentId());
	 assertEquals(sLayer.getDescription(), gLayer.getDescription());
	 assertEquals(sLayer.getAlignment(), gLayer.getAlignment());
	 assertEquals(sLayer.getPeers(), gLayer.getPeers());
	 assertEquals(sLayer.getPeersOverlap(), gLayer.getPeersOverlap());
	 assertEquals(sLayer.getParentIncludes(), gLayer.getParentIncludes());
	 assertEquals(sLayer.getSaturated(), gLayer.getSaturated());
      }

      // participants     
      Annotation[] who = g.list("who");
      assertEquals(2, who.length);
      assertEquals("Maxwell Smart", who[0].getLabel());
      assertEquals("Agent 99", who[1].getLabel());

      // turns
      Annotation[] turns = g.list("turns");
      assertEquals(13, turns.length);
      assertEquals("em_11_0", turns[0].getId());
      assertEquals("n_10", turns[0].getStartId());
      assertEquals("n_70", turns[0].getEndId());
      assertEquals(new Double(10.0), turns[0].getStart().getOffset());
      assertEquals(new Double(70.0), turns[0].getEnd().getOffset());
      assertEquals("Maxwell Smart", turns[0].getLabel());
      assertEquals("Maxwell Smart", turns[0].getParent().getLabel());
      assertEquals("em_11_1", turns[1].getId());
      assertEquals("n_70", turns[1].getStartId());
      assertEquals("n_100", turns[1].getEndId());
      assertEquals(new Double(70.0), turns[1].getStart().getOffset());
      assertEquals(new Double(100.0), turns[1].getEnd().getOffset());
      assertEquals("Agent 99", turns[1].getLabel());
      assertEquals("Agent 99", turns[1].getParent().getLabel());

      assertEquals("em_11_24", turns[12].getId());
      assertEquals("n_2260", turns[12].getStartId());
      assertEquals("n_2340", turns[12].getEndId());
      assertEquals(new Double(2260.0), turns[12].getStart().getOffset());
      assertEquals(new Double(2340.0), turns[12].getEnd().getOffset());
      assertEquals("Agent 99", turns[12].getLabel());
      assertEquals("Agent 99", turns[12].getParent().getLabel());

      // utterances
      Annotation[] utterances = g.list("utterances");
      assertEquals(23, utterances.length);
      assertEquals("em_12_1", utterances[0].getId());
      assertEquals("n_10", utterances[0].getStartId());
      assertEquals("n_40", utterances[0].getEndId());
      assertEquals(new Double(10.0), utterances[0].getStart().getOffset());
      assertEquals(new Double(40.0), utterances[0].getEnd().getOffset());
      assertEquals("Maxwell Smart", utterances[0].getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals("em_12_2", utterances[1].getId());
      assertEquals("n_40", utterances[1].getStartId());
      assertEquals("n_70", utterances[1].getEndId());
      assertEquals(new Double(40.0), utterances[1].getStart().getOffset());
      assertEquals(new Double(70.0), utterances[1].getEnd().getOffset());
      assertEquals("Maxwell Smart", utterances[1].getLabel());
      assertEquals(turns[0], utterances[1].getParent());

      assertEquals("em_12_29", utterances[22].getId());
      assertEquals("n_2300", utterances[22].getStartId());
      assertEquals("n_2340", utterances[22].getEndId());
      assertEquals(new Double(2300.0), utterances[22].getStart().getOffset());
      assertEquals(new Double(2340.0), utterances[22].getEnd().getOffset());
      assertEquals("Agent 99", utterances[22].getLabel());
      assertEquals(turns[12], utterances[22].getParent());

      Annotation[] words = g.list("transcript");
      assertEquals(82, words.length);
      String[] wordLabels = {
	 "MID-LINE-CHANGES", "86-1-2", "86-1-3", 
	 "86-2-1", "86-2-2-to-change", "86-2-3", 
	 "99-3-1", "99-3-2", "99-3-3", 
	 "86-4-1", "86-4-2-insert-word-after-this", "86-4-3", "86-4-4", 
	 "86-5-1", "86-5-2-delete", "86-5-3", "86-5-4", 
	 "99-6-1", "99-6-2-transpose", "99-6-3-transpose", "99-6-4"};
      for (int i = 0; i < wordLabels.length; i++)
      {
	 assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
      }

      // comments
      assertEquals("comment on first word", words[0].get("comment"));
      assertNull("no comment on second word",words[1].get("comment"));
      assertEquals("comment on anchor", words[0].getStart().get("comment"));
      assertNull("no comment on first turn anchor",turns[0].get("comment"));

      Annotation[] language = g.list("language");
      assertEquals(4, language.length);
      assertEquals("spans 86-1-2 to 86-1-3", language[0].getLabel());


      words = language[0].list("transcript");
      assertEquals(2, words.length);
      assertEquals("86-1-2", words[0].getLabel());
      assertEquals("86-1-3", words[1].getLabel());

      assertEquals("instantaneous", language[1].getLabel());
      words = language[1].list("transcript");
      // technically, list returns 1 word for this instantaneous span
      assertEquals(1, words.length);
      // because the word t-includes the instant
      assertTrue(words[0].includes(language[1]));
      // but the language doesn't t-include the word
      assertFalse(language[1].includes(words[0]));

      assertEquals("spans 99-6-2 and 99-6-3", language[2].getLabel());
      words = language[2].list("transcript");
      assertEquals(2, words.length);
      assertEquals("99-6-2-transpose", words[0].getLabel());
      assertEquals("99-6-3-transpose", words[1].getLabel());

      assertEquals("spans 86-1-3 to 86-2-2", language[3].getLabel());
      words = language[3].list("transcript");
      assertEquals(3, words.length);
      assertEquals("86-1-3", words[0].getLabel());
      assertEquals("86-2-1", words[1].getLabel());
      assertEquals("86-2-2", words[2].getLabel());

   }

   @Test public void deserializeFragment() 
      throws Exception
   {
      Schema schema = new Schema(
	 "who", "turns", "utterances", "transcript",
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("turns", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("utterances", "Utterances", 2, true, false, true, "turns", true),
	 new Layer("transcript", "Words", 2, true, false, false, "turns", true),
	 new Layer("segments", "Phones", 2, true, false, true, "transcript", true)
	 );
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "test__10.000-70.000.csv")) };

      // create deserializer
      AgCsvDeserializer deserializer = new AgCsvDeserializer();

      // configure it
      ParameterSet defaultConfiguration = deserializer.configure(new ParameterSet(), schema);
      deserializer.configure(defaultConfiguration, schema);

      // load the stream
      ParameterSet defaultParameters = deserializer.load(streams, schema);
//      for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());

      // configure the deserialization
      deserializer.setParameters(defaultParameters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];
      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }

      assertEquals("test__10.000-70.000", g.getId());

      // participants     
      Annotation[] who = g.list("who");
      assertEquals(1, who.length);
      assertEquals("Maxwell Smart", who[0].getLabel());
      assertEquals(g, who[0].getParent());

      // turns
      Annotation[] turns = g.list("turns");
      assertEquals(1, turns.length);
      assertEquals("em_11_0", turns[0].getId());
      assertEquals("n_100", turns[0].getStartId());
      assertEquals("n_700", turns[0].getEndId());
      assertEquals(new Double(10.0), turns[0].getStart().getOffset());
      assertEquals(new Double(70.0), turns[0].getEnd().getOffset());
      assertEquals(who[0], turns[0].getParent());

      // utterances
      Annotation[] utterances = g.list("utterances");
      assertEquals(2, utterances.length);
      assertEquals("em_12_1", utterances[0].getId());
      assertEquals("n_100", utterances[0].getStartId());
      assertEquals("n_400", utterances[0].getEndId());
      assertEquals(new Double(10.0), utterances[0].getStart().getOffset());
      assertEquals(new Double(40.0), utterances[0].getEnd().getOffset());
      assertEquals("Maxwell Smart", utterances[0].getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals("em_12_2", utterances[1].getId());
      assertEquals("n_400", utterances[1].getStartId());
      assertEquals("n_700", utterances[1].getEndId());
      assertEquals(new Double(40.0), utterances[1].getStart().getOffset());
      assertEquals(new Double(70.0), utterances[1].getEnd().getOffset());
      assertEquals("Maxwell Smart", utterances[1].getLabel());
      assertEquals(turns[0], utterances[1].getParent());

      Annotation[] words = g.list("transcript");
      assertEquals(6, words.length);
      String[] wordLabels = {
	 "86-1-1", "86-1-2", "86-1-3", 
	 "86-2-1", "86-2-2", "86-2-3"};
      for (int i = 0; i < wordLabels.length; i++)
      {
	 assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
      }

      Annotation[] segments = g.list("segments");
      assertEquals(7, segments.length);
      String[] segmentLabels = {
	 "86-1-1-1", "86-1-1-2", "86-1-1-3", 
	 "86-1-3-1", "86-1-3-2", "86-1-3-3", "86-1-3-4"};
      for (int i = 0; i < segmentLabels.length; i++)
      {
	 assertEquals("segment labels " + i, segmentLabels[i], segments[i].getLabel());
      }

   }

   @Test public void deserializeFragmentMissingTurn() 
      throws Exception
   {
      Schema schema = new Schema(
	 "who", "turns", "utterances", "transcript",
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("turns", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("utterances", "Utterances", 2, true, false, true, "turns", true),
	 new Layer("transcript", "Words", 2, true, false, false, "turns", true),
	 new Layer("segments", "Phones", 2, true, false, true, "transcript", true)
	 );
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "testNoTurn__10.000-70.000.csv")) };

      // create deserializer
      AgCsvDeserializer deserializer = new AgCsvDeserializer();

      // configure it
      ParameterSet defaultConfiguration = deserializer.configure(new ParameterSet(), schema);
      deserializer.configure(defaultConfiguration, schema);

      // load the stream
      ParameterSet defaultParameters = deserializer.load(streams, schema);
//      for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());

      // configure the deserialization
      deserializer.setParameters(defaultParameters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];
      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }

      assertEquals("testNoTurn__10.000-70.000", g.getId());

      // participants     
      Annotation[] who = g.list("who");
      assertEquals("participant was created", 1, who.length);
      assertEquals("participant label", "Maxwell Smart", who[0].getLabel());
      assertEquals("participant parent is graph", g, who[0].getParent());

      // turns
      Annotation[] turns = g.list("turns");
      assertEquals(1, turns.length);
      assertEquals("turn was created", "em_11_0", turns[0].getId());
      assertEquals("turn start ID is set", "em_11_0 start", turns[0].getStartId());
      assertEquals("turn end ID is set", "em_11_0 end", turns[0].getEndId());
      assertNull("turn start is unkown", turns[0].getStart());
      assertNull("turn end is unknown", turns[0].getEnd());
      assertEquals("turn parent is participant", who[0], turns[0].getParent());

      // utterances
      Annotation[] utterances = g.list("utterances");
      assertEquals(2, utterances.length);
      assertEquals("em_12_1", utterances[0].getId());
      assertEquals("n_100", utterances[0].getStartId());
      assertEquals("n_400", utterances[0].getEndId());
      assertEquals(new Double(10.0), utterances[0].getStart().getOffset());
      assertEquals(new Double(40.0), utterances[0].getEnd().getOffset());
      assertEquals("Maxwell Smart", utterances[0].getLabel());
      assertEquals("utterance has new turn as parent", turns[0], utterances[0].getParent());

      assertEquals("em_12_2", utterances[1].getId());
      assertEquals("n_400", utterances[1].getStartId());
      assertEquals("n_700", utterances[1].getEndId());
      assertEquals(new Double(40.0), utterances[1].getStart().getOffset());
      assertEquals(new Double(70.0), utterances[1].getEnd().getOffset());
      assertEquals("Maxwell Smart", utterances[1].getLabel());
      assertEquals(turns[0], utterances[1].getParent());

      Annotation[] words = g.list("transcript");
      assertEquals(6, words.length);
      String[] wordLabels = {
	 "86-1-1", "86-1-2", "86-1-3", 
	 "86-2-1", "86-2-2", "86-2-3"};
      for (int i = 0; i < wordLabels.length; i++)
      {
	 assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
      }

      Annotation[] segments = g.list("segments");
      assertEquals(7, segments.length);
      String[] segmentLabels = {
	 "86-1-1-1", "86-1-1-2", "86-1-1-3", 
	 "86-1-3-1", "86-1-3-2", "86-1-3-3", "86-1-3-4"};
      for (int i = 0; i < segmentLabels.length; i++)
      {
	 assertEquals("segment labels " + i, segmentLabels[i], segments[i].getLabel());
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
      org.junit.runner.JUnitCore.main("nzilbb.formatter.agcsv.TestAgCsvDeserializer");
   }
}
