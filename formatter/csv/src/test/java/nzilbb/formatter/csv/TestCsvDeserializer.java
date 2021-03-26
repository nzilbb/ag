//
// Copyright 2018 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.formatter.csv;
	      
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
import nzilbb.formatter.csv.*;

public class TestCsvDeserializer {
   
   @Test public void basicDeserialization()  throws Exception {
      Schema schema = new Schema(
	 "who", "turns", "utterances", "word",
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("turns", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("utterances", "Utterances", 2, true, false, true, "turns", true),
	 new Layer("word", "Words", 2, true, false, false, "turns", true)
	 );
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "tweets.csv")) };

      // create deserializer
      CsvDeserializer deserializer = new CsvDeserializer();

      // configure it
      ParameterSet defaultConfiguration = deserializer.configure(new ParameterSet(), schema);
      for (Parameter p : defaultConfiguration.values()) System.out.println("condifgure: " + p.getName() + " = " + p.getValue());
      assertEquals("Configuration parameters" + defaultConfiguration, 0, defaultConfiguration.size());
      deserializer.configure(defaultConfiguration, schema);
      
      // load the stream
      ParameterSet defaultParameters = deserializer.load(streams, schema);
      //for (Parameter p : defaultParameters.values()) System.out.println(p.getName() + " = " + p.getValue());
      assertEquals(10, defaultParameters.size());
      assertEquals("id", "ID", 
		   defaultParameters.get("id").getValue());
      assertEquals("who", "Participant", 
		   defaultParameters.get("who").getValue());
      assertEquals("text", "Text", 
		   defaultParameters.get("text").getValue());
      assertNull("column 0 default mapping",
		 defaultParameters.get("header_0").getValue());
      assertNull("column 1 default mapping",
		 defaultParameters.get("header_1").getValue());
      assertNull("column 2 default mapping",
		 defaultParameters.get("header_2").getValue());
      assertNull("column 3 default mapping",
		 defaultParameters.get("header_3").getValue());
      assertNull("column 4 default mapping",
		 defaultParameters.get("header_4").getValue());
      assertNull("column 5 default mapping",
		 defaultParameters.get("header_5").getValue());
      assertNull("column 6 default mapping",
		 defaultParameters.get("header_6").getValue());

      // configure the deserialization
      deserializer.setParameters(defaultParameters);

      // build the graphs
      Graph[] graphs = deserializer.deserialize();
      assertEquals("Right number of graphs returned", 4, graphs.length);      
      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }

      { // row 1
	 Graph g = graphs[0];
	 assertEquals("name correct", "clairemtimmins_953247839433510912", g.getId());
	 
	 // layers
	 for (Layer gLayer : g.getSchema().getLayers().values()) {
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
	 Annotation[] who = g.all("who");
	 assertEquals(1, who.length);
	 assertEquals("@clairemtimmins", who[0].getLabel());
	 
	 // turns
	 Annotation[] turns = g.all("turns");
	 assertEquals(1, turns.length);
	 assertEquals("@clairemtimmins", turns[0].getLabel());
	 
	 // utterances
	 Annotation[] utterances = g.all("utterances");
	 assertEquals(1, utterances.length);
	 assertEquals(turns[0], utterances[0].getParent());
	 
	 // words
	 Annotation[] words = g.all("word");
	 assertEquals(12, words.length);
	 String[] wordLabels = {
	    "Shame", "our", "next", "meeting", "wasn't", "before", "this", "deadline",
	    "@KSavage_Strath", "@SFaulknerPandO", "@g_efthimiou", "@JMcKerrecher"};
	 for (int i = 0; i < wordLabels.length; i++) {
	    assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
	 }
      
         // check all annotations have 'manual' confidence
         for (Annotation a : g.getAnnotationsById().values()) {
            assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                         Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
         }
      } // row 1
      
      { // row 2
	 Graph g = graphs[1];
	 assertEquals("name correct", "ProfMarkLevine_952155167188836352", g.getId());
	 
	 // layers
	 for (Layer gLayer : g.getSchema().getLayers().values()) {
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
	 Annotation[] who = g.all("who");
	 assertEquals(1, who.length);
	 assertEquals("@ProfMarkLevine", who[0].getLabel());
	 
	 // turns
	 Annotation[] turns = g.all("turns");
	 assertEquals(1, turns.length);
	 assertEquals("@ProfMarkLevine", turns[0].getLabel());
	 
	 // utterances
	 Annotation[] utterances = g.all("utterances");
	 assertEquals(1, utterances.length);
	 assertEquals(turns[0], utterances[0].getParent());
	 
	 // words
	 Annotation[] words = g.all("word");
	 assertEquals(32, words.length);
	 String[] wordLabels = {
	    "“differences", "between", "lesbian", "or", "gay", "and", "straight", "faces",
	    "in", "selfies", "relate", "to", "grooming,", "presentation,", "and", "lifestyle — that",
	    "is,", "differences", "in", "culture,", "not", "in", "facial", "structure.”",
	    "Algorithms", "and", "the", "junk", "science", "of", "physiognomy", "@blaiseaguera"};
	 for (int i = 0; i < wordLabels.length; i++) {
	    assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
	 }
      
         // check all annotations have 'manual' confidence
         for (Annotation a : g.getAnnotationsById().values()) {
            assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                         Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
         }
      } // row 2

      { // row 3
	 Graph g = graphs[2];
	 assertEquals("name correct", "robdrummond_953211091416420352", g.getId());
	 
	 // layers
	 for (Layer gLayer : g.getSchema().getLayers().values()) {
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
	 Annotation[] who = g.all("who");
	 assertEquals(1, who.length);
	 assertEquals("@robdrummond", who[0].getLabel());
	 
	 // turns
	 Annotation[] turns = g.all("turns");
	 assertEquals(1, turns.length);
	 assertEquals("@robdrummond", turns[0].getLabel());
	 
	 // utterances
	 Annotation[] utterances = g.all("utterances");
	 assertEquals(1, utterances.length);
	 assertEquals(turns[0], utterances[0].getParent());
	 
	 // words
	 Annotation[] words = g.all("word");
	 assertEquals(5, words.length);
	 String[] wordLabels = {
	    "'Awash", "with", "contemporary", "teenspeak'", "😀👍"};
	 for (int i = 0; i < wordLabels.length; i++) {
	    assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
	 }
      
         // check all annotations have 'manual' confidence
         for (Annotation a : g.getAnnotationsById().values()) {
            assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                         Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
         }
      } // row 3
      
      { // row 4
	 Graph g = graphs[3];
	 assertEquals("name correct", "theirishfor_953177367505264640", g.getId());
	 
	 // layers
	 for (Layer gLayer : g.getSchema().getLayers().values()) {
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
	 Annotation[] who = g.all("who");
	 assertEquals(1, who.length);
	 assertEquals("@theirishfor", who[0].getLabel());
	 
	 // turns
	 Annotation[] turns = g.all("turns");
	 assertEquals(1, turns.length);
	 assertEquals("@theirishfor", turns[0].getLabel());
	 
	 // utterances
	 Annotation[] utterances = g.all("utterances");
	 assertEquals(1, utterances.length);
	 assertEquals(turns[0], utterances[0].getParent());
	 
	 // words
	 Annotation[] words = g.all("word");
	 assertEquals(14, words.length);
	 String[] wordLabels = {
	    "Duolingo", "has", "brought", "the", "joys", "of", "Irish", "to", "people",
	    "all", "over", "the", "world.", "💚"};
	 for (int i = 0; i < wordLabels.length; i++) {
	    assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
	 }
      
         // check all annotations have 'manual' confidence
         for (Annotation a : g.getAnnotationsById().values()) {
            assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                         Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
         }
      } // row 4
   }

   @Test public void metaData()  throws Exception {
      Schema schema = new Schema(
	 "who", "turns", "utterances", "word",
	 new Layer("transcript_time", "Tweet time", 0, false, false, true),
	 new Layer("transcript_url", "URL", 0, false, false, true),
	 new Layer("corpus", "Corpus", 0, false, false, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("participant_name", "Participant Name", 0, false, false, true, "who", true),
	 new Layer("turns", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("utterances", "Utterances", 2, true, false, true, "turns", true),
	 new Layer("word", "Words", 2, true, false, false, "turns", true)
	 );
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "tweets.csv")) };

      // create deserializer
      CsvDeserializer deserializer = new CsvDeserializer();

      // configure it
      ParameterSet defaultConfiguration = deserializer.configure(new ParameterSet(), schema);
      for (Parameter p : defaultConfiguration.values()) System.out.println("configure: " + p.getName() + " = " + p.getValue());
      assertEquals("Configuration parameters" + defaultConfiguration, 0, defaultConfiguration.size());
      deserializer.configure(defaultConfiguration, schema);
      
      // load the stream
      ParameterSet defaultParameters = deserializer.load(streams, schema);
      //for (Parameter p : defaultParameters.values()) System.out.println(p.getName() + " = " + p.getValue());
      assertEquals(10, defaultParameters.size());
      assertEquals("id", "ID", 
		   defaultParameters.get("id").getValue());
      assertEquals("who", "Participant", 
		   defaultParameters.get("who").getValue());
      assertEquals("text", "Text", 
		   defaultParameters.get("text").getValue());
      assertNull("ID column default mapping", // ID = transcript ID
		 defaultParameters.get("header_0").getValue());
      assertNull("Participant column default mapping", // Participant = who
		 defaultParameters.get("header_1").getValue());
      assertEquals("Name column default mapping", "participant_name", 
		   ((Layer)defaultParameters.get("header_2").getValue()).getId());
      assertNull("Text column default mapping", // Text = utterance
		 defaultParameters.get("header_3").getValue());
      assertEquals("Time column default mapping", "transcript_time",
		   ((Layer)defaultParameters.get("header_4").getValue()).getId());
      assertEquals("URL column default mapping", "transcript_url",
		   ((Layer)defaultParameters.get("header_5").getValue()).getId());
      assertEquals("Corpus column default mapping", "corpus",
		   ((Layer)defaultParameters.get("header_6").getValue()).getId());
      
      // configure the deserialization
      deserializer.setParameters(defaultParameters);

      // build the graphs
      Graph[] graphs = deserializer.deserialize();
      assertEquals("Right number of graphs returned", 4, graphs.length);      
      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }

      { // row 1
	 Graph g = graphs[0];
	 assertEquals("name correct", "clairemtimmins_953247839433510912", g.getId());
	 
	 // layers
	 for (Layer gLayer : g.getSchema().getLayers().values()) {
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
	 Annotation[] who = g.all("who");
	 assertEquals(1, who.length);
	 assertEquals("@clairemtimmins", who[0].getLabel());
	 
	 // turns
	 Annotation[] turns = g.all("turns");
	 assertEquals(1, turns.length);
	 assertEquals("@clairemtimmins", turns[0].getLabel());

	 // utterances
	 Annotation[] utterances = g.all("utterances");
	 assertEquals(1, utterances.length);
	 assertEquals(turns[0], utterances[0].getParent());
	 
	 // words
	 Annotation[] words = g.all("word");
	 assertEquals(12, words.length);
	 String[] wordLabels = {
	    "Shame", "our", "next", "meeting", "wasn't", "before", "this", "deadline",
	    "@KSavage_Strath", "@SFaulknerPandO", "@g_efthimiou", "@JMcKerrecher"};
	 for (int i = 0; i < wordLabels.length; i++) {
	    assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
	 }
	 
	 // meta data
	 
	 assertEquals("row 1: One time tag", 1, g.all("transcript_time").length);
	 Annotation tag = g.first("transcript_time");
	 assertEquals("row 1: time tag - correct parent: " + tag.getParent(), g, tag.getParent());
	 assertEquals("row 1: time tag - correct label: " + tag.getLabel(),
		      "4:49 AM - 16 Jan 2018", tag.getLabel());
	 
	 assertEquals("row 1: One url tag", 1, g.all("transcript_url").length);
	 tag = g.first("transcript_url");
	 assertEquals("row 1: url tag - correct parent: " + tag.getParent(), g, tag.getParent());
	 assertEquals("row 1: url tag - correct label: " + tag.getLabel(),
		      "https://twitter.com/clairemtimmins/status/953247839433510912", tag.getLabel());
	 
	 assertEquals("row 1: One corpus tag", 1, g.all("corpus").length);
	 tag = g.first("corpus");
	 assertEquals("row 1: corpus tag - correct parent: " + tag.getParent(), g, tag.getParent());
	 assertEquals("row 1: corpus tag - correct label: " + tag.getLabel(),
		      "Twitter", tag.getLabel());
	 
	 assertEquals("row 1: One name tag", 1, g.all("participant_name").length);
	 tag = g.first("participant_name");
	 assertEquals("row 1: name tag - correct parent: " + tag.getParent(), who[0], tag.getParent());
	 assertEquals("row 1: name tag - correct label: " + tag.getLabel(),
		      "Claire Timmins", tag.getLabel());
      
         // check all annotations have 'manual' confidence
         for (Annotation a : g.getAnnotationsById().values()) {
            assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                         Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
         }
      } // row 1      
      
      { // row 2
	 Graph g = graphs[1];
	 assertEquals("name correct", "ProfMarkLevine_952155167188836352", g.getId());
	 
	 // layers
	 for (Layer gLayer : g.getSchema().getLayers().values()) {
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
	 Annotation[] who = g.all("who");
	 assertEquals(1, who.length);
	 assertEquals("@ProfMarkLevine", who[0].getLabel());
	 
	 // turns
	 Annotation[] turns = g.all("turns");
	 assertEquals(1, turns.length);
	 assertEquals("@ProfMarkLevine", turns[0].getLabel());
	 
	 // utterances
	 Annotation[] utterances = g.all("utterances");
	 assertEquals(1, utterances.length);
	 assertEquals(turns[0], utterances[0].getParent());
	 
	 // words
	 Annotation[] words = g.all("word");
	 assertEquals(32, words.length);
	 String[] wordLabels = {
	    "“differences", "between", "lesbian", "or", "gay", "and", "straight", "faces",
	    "in", "selfies", "relate", "to", "grooming,", "presentation,", "and", "lifestyle — that",
	    "is,", "differences", "in", "culture,", "not", "in", "facial", "structure.”",
	    "Algorithms", "and", "the", "junk", "science", "of", "physiognomy", "@blaiseaguera"};
	 for (int i = 0; i < wordLabels.length; i++) {
	    assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
	 }
	 
	 // meta data
	 
	 assertEquals("row 2: One time tag", 1, g.all("transcript_time").length);
	 Annotation tag = g.first("transcript_time");
	 assertEquals("row 2: time tag - correct parent: " + tag.getParent(), g, tag.getParent());
	 assertEquals("row 2: time tag - correct label: " + tag.getLabel(),
		      "4:27 AM - 13 Jan 2018", tag.getLabel());
	 
	 assertEquals("row 2: One url tag", 1, g.all("transcript_url").length);
	 tag = g.first("transcript_url");
	 assertEquals("row 2: url tag - correct parent: " + tag.getParent(), g, tag.getParent());
	 assertEquals("row 2: url tag - correct label: " + tag.getLabel(),
		      "https://twitter.com/ProfMarkLevine/status/952155167188836352", tag.getLabel());
	 
	 assertEquals("row 2: One corpus tag", 1, g.all("corpus").length);
	 tag = g.first("corpus");
	 assertEquals("row 2: corpus tag - correct parent: " + tag.getParent(), g, tag.getParent());
	 assertEquals("row 2: corpus tag - correct label: " + tag.getLabel(),
		      "Twitter", tag.getLabel());
	 
	 assertEquals("row 2: One name tag", 1, g.all("participant_name").length);
	 tag = g.first("participant_name");
	 assertEquals("row 2: name tag - correct parent: " + tag.getParent(), who[0], tag.getParent());
	 assertEquals("row 2: name tag - correct label: " + tag.getLabel(),
		      "Mark Levine", tag.getLabel());
         // check all annotations have 'manual' confidence
         for (Annotation a : g.getAnnotationsById().values()) {
            assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                         Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
         }
      } // row 2

      { // row 3
	 Graph g = graphs[2];
	 assertEquals("name correct", "robdrummond_953211091416420352", g.getId());
	 
	 // layers
	 for (Layer gLayer : g.getSchema().getLayers().values()) {
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
	 Annotation[] who = g.all("who");
	 assertEquals(1, who.length);
	 assertEquals("@robdrummond", who[0].getLabel());
	 
	 // turns
	 Annotation[] turns = g.all("turns");
	 assertEquals(1, turns.length);
	 assertEquals("@robdrummond", turns[0].getLabel());
	 
	 // utterances
	 Annotation[] utterances = g.all("utterances");
	 assertEquals(1, utterances.length);
	 assertEquals(turns[0], utterances[0].getParent());
	 
	 // words
	 Annotation[] words = g.all("word");
	 assertEquals(5, words.length);
	 String[] wordLabels = {
	    "'Awash", "with", "contemporary", "teenspeak'", "😀👍"};
	 for (int i = 0; i < wordLabels.length; i++) {
	    assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
	 }
	 
	 // meta data
	 
	 assertEquals("row 3: One time tag", 1, g.all("transcript_time").length);
	 Annotation tag = g.first("transcript_time");
	 assertEquals("row 3: time tag - correct parent: " + tag.getParent(), g, tag.getParent());
	 assertEquals("row 3: time tag - correct label: " + tag.getLabel(),
		      "2:23 AM - 16 Jan 2018", tag.getLabel());
	 
	 assertEquals("row 3: One url tag", 1, g.all("transcript_url").length);
	 tag = g.first("transcript_url");
	 assertEquals("row 3: url tag - correct parent: " + tag.getParent(), g, tag.getParent());
	 assertEquals("row 3: url tag - correct label: " + tag.getLabel(),
		      "https://twitter.com/robdrummond/status/953211091416420352", tag.getLabel());
	 
	 assertEquals("row 3: One corpus tag", 1, g.all("corpus").length);
	 tag = g.first("corpus");
	 assertEquals("row 3: corpus tag - correct parent: " + tag.getParent(), g, tag.getParent());
	 assertEquals("row 3: corpus tag - correct label: " + tag.getLabel(),
		      "Twitter", tag.getLabel());
	 
	 assertEquals("row 3: One name tag", 1, g.all("participant_name").length);
	 tag = g.first("participant_name");
	 assertEquals("row 3: name tag - correct parent: " + tag.getParent(), who[0], tag.getParent());
	 assertEquals("row 3: name tag - correct label: " + tag.getLabel(),
		      " Rob Drummond", tag.getLabel());
         // check all annotations have 'manual' confidence
         for (Annotation a : g.getAnnotationsById().values()) {
            assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                         Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
         }
      } // row 3
      
      { // row 4
	 Graph g = graphs[3];
	 assertEquals("name correct", "theirishfor_953177367505264640", g.getId());
	 
	 // layers
	 for (Layer gLayer : g.getSchema().getLayers().values()) {
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
	 Annotation[] who = g.all("who");
	 assertEquals(1, who.length);
	 assertEquals("@theirishfor", who[0].getLabel());
	 
	 // turns
	 Annotation[] turns = g.all("turns");
	 assertEquals(1, turns.length);
	 assertEquals("@theirishfor", turns[0].getLabel());
	 
	 // utterances
	 Annotation[] utterances = g.all("utterances");
	 assertEquals(1, utterances.length);
	 assertEquals(turns[0], utterances[0].getParent());
	 
	 // words
	 Annotation[] words = g.all("word");
	 assertEquals(14, words.length);
	 String[] wordLabels = {
	    "Duolingo", "has", "brought", "the", "joys", "of", "Irish", "to", "people",
	    "all", "over", "the", "world.", "💚"};
	 for (int i = 0; i < wordLabels.length; i++) {
	    assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
	 }
	 
	 // meta data
	 
	 assertEquals("row 4: One time tag", 1, g.all("transcript_time").length);
	 Annotation tag = g.first("transcript_time");
	 assertEquals("row 4: time tag - correct parent: " + tag.getParent(), g, tag.getParent());
	 assertEquals("row 4: time tag - correct label: " + tag.getLabel(),
		      "12:09 AM - 16 Jan 2018", tag.getLabel());
	 
	 assertEquals("row 4: One url tag", 1, g.all("transcript_url").length);
	 tag = g.first("transcript_url");
	 assertEquals("row 4: url tag - correct parent: " + tag.getParent(), g, tag.getParent());
	 assertEquals("row 4: url tag - correct label: " + tag.getLabel(),
		      "https://twitter.com/theirishfor/status/953177367505264640", tag.getLabel());
	 
	 assertEquals("row 4: One corpus tag", 1, g.all("corpus").length);
	 tag = g.first("corpus");
	 assertEquals("row 4: corpus tag - correct parent: " + tag.getParent(), g, tag.getParent());
	 assertEquals("row 4: corpus tag - correct label: " + tag.getLabel(),
		      "Twitter", tag.getLabel());
	 
	 assertEquals("row 4: One name tag", 1, g.all("participant_name").length);
	 tag = g.first("participant_name");
	 assertEquals("row 4: name tag - correct parent: " + tag.getParent(), who[0], tag.getParent());
	 assertEquals("row 4: name tag - correct label: " + tag.getLabel(),
		      "The Irish For 😚\n🐊", tag.getLabel());
         // check all annotations have 'manual' confidence
         for (Annotation a : g.getAnnotationsById().values()) {
            assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                         Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
         }
      } // row 4      
   }
   
   @Test public void badHeaders()  throws Exception {
      Schema schema = new Schema(
	 "who", "turns", "utterances", "word",
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("turns", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("utterances", "Utterances", 2, true, false, true, "turns", true),
	 new Layer("word", "Words", 2, true, false, false, "turns", true)
	 );
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "badheaders.csv")) };

      // create deserializer
      CsvDeserializer deserializer = new CsvDeserializer();

      // configure it
      ParameterSet defaultConfiguration = deserializer.configure(new ParameterSet(), schema);
      for (Parameter p : defaultConfiguration.values()) System.out.println("configure: " + p.getName() + " = " + p.getValue());
      assertEquals("Configuration parameters" + defaultConfiguration, 0, defaultConfiguration.size());
      deserializer.configure(defaultConfiguration, schema);
      
      // load the stream
      ParameterSet defaultParameters = deserializer.load(streams, schema);
      //for (Parameter p : defaultParameters.values()) System.out.println(p.getName() + " = " + p.getValue());
      assertEquals(5, defaultParameters.size());
      assertNull("id",
		 defaultParameters.get("id").getValue());
      assertEquals("who", "handle", 
		   defaultParameters.get("who").getValue());
      assertEquals("text", "tweet", 
		   defaultParameters.get("text").getValue());
      assertNull("column 0 default mapping",
		 defaultParameters.get("header_0").getValue());
      assertNull("column 1 default mapping",
		 defaultParameters.get("header_1").getValue());
      
      // configure the deserialization
      deserializer.setParameters(defaultParameters);

      // build the graphs
      Graph[] graphs = deserializer.deserialize();
      assertEquals("Right number of graphs returned", 4, graphs.length);      
      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }

      { // row 1
	 Graph g = graphs[0];
	 assertEquals("name correct", "badheaders.csv-1", g.getId());
	 
	 // layers
	 for (Layer gLayer : g.getSchema().getLayers().values()) {
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
	 Annotation[] who = g.all("who");
	 assertEquals(1, who.length);
	 assertEquals("@clairemtimmins", who[0].getLabel());
	 
	 // turns
	 Annotation[] turns = g.all("turns");
	 assertEquals(1, turns.length);
	 assertEquals("@clairemtimmins", turns[0].getLabel());
	 
	 // utterances
	 Annotation[] utterances = g.all("utterances");
	 assertEquals(1, utterances.length);
	 assertEquals(turns[0], utterances[0].getParent());
	 
	 // words
	 Annotation[] words = g.all("word");
	 assertEquals(12, words.length);
	 String[] wordLabels = {
	    "Shame", "our", "next", "meeting", "wasn't", "before", "this", "deadline",
	    "@KSavage_Strath", "@SFaulknerPandO", "@g_efthimiou", "@JMcKerrecher"};
	 for (int i = 0; i < wordLabels.length; i++) {
	    assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
	 }
         
         // check all annotations have 'manual' confidence
         for (Annotation a : g.getAnnotationsById().values()) {
            assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                         Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
         }
      } // row 1
      
      { // row 2
	 Graph g = graphs[1];
	 assertEquals("name correct", "badheaders.csv-2", g.getId());
	 
	 // layers
	 for (Layer gLayer : g.getSchema().getLayers().values()) {
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
	 Annotation[] who = g.all("who");
	 assertEquals(1, who.length);
	 assertEquals("@ProfMarkLevine", who[0].getLabel());
	 
	 // turns
	 Annotation[] turns = g.all("turns");
	 assertEquals(1, turns.length);
	 assertEquals("@ProfMarkLevine", turns[0].getLabel());
	 
	 // utterances
	 Annotation[] utterances = g.all("utterances");
	 assertEquals(1, utterances.length);
	 assertEquals(turns[0], utterances[0].getParent());
	 
	 // words
	 Annotation[] words = g.all("word");
	 assertEquals(32, words.length);
	 String[] wordLabels = {
	    "“differences", "between", "lesbian", "or", "gay", "and", "straight", "faces",
	    "in", "selfies", "relate", "to", "grooming,", "presentation,", "and", "lifestyle — that",
	    "is,", "differences", "in", "culture,", "not", "in", "facial", "structure.”",
	    "Algorithms", "and", "the", "junk", "science", "of", "physiognomy", "@blaiseaguera"};
	 for (int i = 0; i < wordLabels.length; i++) {
	    assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
	 }
         
         // check all annotations have 'manual' confidence
         for (Annotation a : g.getAnnotationsById().values()) {
            assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                         Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
         }
      } // row 2

      { // row 3
	 Graph g = graphs[2];
	 assertEquals("name correct", "badheaders.csv-3", g.getId());
	 
	 // layers
	 for (Layer gLayer : g.getSchema().getLayers().values()) {
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
	 Annotation[] who = g.all("who");
	 assertEquals(1, who.length);
	 assertEquals("@robdrummond", who[0].getLabel());
	 
	 // turns
	 Annotation[] turns = g.all("turns");
	 assertEquals(1, turns.length);
	 assertEquals("@robdrummond", turns[0].getLabel());
	 
	 // utterances
	 Annotation[] utterances = g.all("utterances");
	 assertEquals(1, utterances.length);
	 assertEquals(turns[0], utterances[0].getParent());
	 
	 // words
	 Annotation[] words = g.all("word");
	 assertEquals(5, words.length);
	 String[] wordLabels = {
	    "'Awash", "with", "contemporary", "teenspeak'", "😀👍"};
	 for (int i = 0; i < wordLabels.length; i++) {
	    assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
	 }

         // check all annotations have 'manual' confidence
         for (Annotation a : g.getAnnotationsById().values()) {
            assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                         Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
         }
      } // row 3
      
      { // row 4
	 Graph g = graphs[3];
	 assertEquals("name correct", "badheaders.csv-4", g.getId());
	 
	 // layers
	 for (Layer gLayer : g.getSchema().getLayers().values()) {
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
	 Annotation[] who = g.all("who");
	 assertEquals(1, who.length);
	 assertEquals("@theirishfor", who[0].getLabel());
	 
	 // turns
	 Annotation[] turns = g.all("turns");
	 assertEquals(1, turns.length);
	 assertEquals("@theirishfor", turns[0].getLabel());
	 
	 // utterances
	 Annotation[] utterances = g.all("utterances");
	 assertEquals(1, utterances.length);
	 assertEquals(turns[0], utterances[0].getParent());
	 
	 // words
	 Annotation[] words = g.all("word");
	 assertEquals(14, words.length);
	 String[] wordLabels = {
	    "Duolingo", "has", "brought", "the", "joys", "of", "Irish", "to", "people",
	    "all", "over", "the", "world.", "💚"};
	 for (int i = 0; i < wordLabels.length; i++) {
	    assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
	 }
         
         // check all annotations have 'manual' confidence
         for (Annotation a : g.getAnnotationsById().values()) {
            assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                         Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
         }
      } // row 4
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
      org.junit.runner.JUnitCore.main("nzilbb.formatter.csv.TestCsvDeserializer");
   }
}
