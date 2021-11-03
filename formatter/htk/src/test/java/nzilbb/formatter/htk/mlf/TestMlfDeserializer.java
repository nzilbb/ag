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

package nzilbb.formatter.htk.mlf;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.io.File;
import java.net.URL;
import nzilbb.configure.ParameterSet;
import nzilbb.configure.Parameter;
import nzilbb.ag.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.formatter.htk.mlf.*;

public class TestMlfDeserializer {
   
   @Test public void basicAlignment()  throws Exception {
      Schema schema = new Schema(
         "participant", "turn", "utterance", "word",
	 new Layer("noise", "Noise")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false),
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
      //for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(6, deserializer.configure(configuration, schema).size());
      assertEquals("words mapping", "word", 
		   ((Layer)configuration.get("wordLayer").getValue()).getId());
      assertEquals("noise mapping", "noise", 
		   ((Layer)configuration.get("noiseLayer").getValue()).getId());
      assertEquals("phones mapping", "phone", 
		   ((Layer)configuration.get("phoneLayer").getValue()).getId());
      assertEquals("score mapping", "score", 
		   ((Layer)configuration.get("scoreLayer").getValue()).getId());
      assertEquals("default useP2FACorrection setting", Boolean.FALSE, 
		   configuration.get("useP2FACorrection").getValue());
      assertEquals("no noise identifiers", "", 
		   configuration.get("noiseIdentifiersString").getValue());

      // for this test, we don't want scores nor noises
      configuration.get("scoreLayer").setValue(null);
      configuration.get("noiseLayer").setValue(null);

      deserializer.configure(configuration, schema);

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(0, defaultParamaters.size());
      
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
      assertNull("first fragment no noise layer", g.getLayer("noise"));

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
      assertEquals("second fragment name",
                   "AP511_MikeThorpe__7.131-13.887", g.getId());
      assertEquals("second fragment source graph",
                   "AP511_MikeThorpe.eaf", g.sourceGraph().getId());
      assertEquals("second fragment startTime tag",
                   Double.valueOf(7.131), (Double)g.get("@startTime"));
   }

   @Test public void wordOnly()  throws Exception {
      Schema schema = new Schema(
         "participant", "turn", "utterance", "word",
	 new Layer("noise", "Noise")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false),
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
      //for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(6, deserializer.configure(configuration, schema).size());
      assertEquals("words mapping", "word", 
		   ((Layer)configuration.get("wordLayer").getValue()).getId());
      assertEquals("noise mapping", "noise", 
		   ((Layer)configuration.get("noiseLayer").getValue()).getId());
      assertEquals("phones mapping", "phone", 
		   ((Layer)configuration.get("phoneLayer").getValue()).getId());
      assertEquals("score mapping", "score", 
		   ((Layer)configuration.get("scoreLayer").getValue()).getId());
      assertEquals("default useP2FACorrection setting", Boolean.FALSE, 
		   configuration.get("useP2FACorrection").getValue());
      assertEquals("no noise identifiers", "", 
		   configuration.get("noiseIdentifiersString").getValue());

      // for this test, we don't want scores nor noises, nor phones
      configuration.get("scoreLayer").setValue(null);
      configuration.get("noiseLayer").setValue(null);
      configuration.get("phoneLayer").setValue(null);

      deserializer.configure(configuration, schema);

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(0, defaultParamaters.size());
      
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
      assertNull("first fragment no phone layer", g.getLayer("phone"));
      assertNull("first fragment no score layer", g.getLayer("score"));
      assertNull("first fragment no noise layer", g.getLayer("noise"));

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
      assertEquals("No phones", 0, phones.length);
      
      g = graphs[1];
      assertEquals("second fragment name",
                   "AP511_MikeThorpe__7.131-13.887", g.getId());
      assertEquals("second fragment source graph",
                   "AP511_MikeThorpe.eaf", g.sourceGraph().getId());
      assertEquals("second fragment startTime tag",
                   Double.valueOf(7.131), (Double)g.get("@startTime"));

      phones = g.all("phone");
      assertEquals("second fragment: No phones", 0, phones.length);
   }

   // scores and noises
   @Test public void scoresAndNoise()  throws Exception {
      Schema schema = new Schema(
         "participant", "turn", "utterance", "word",
	 new Layer("noise", "Noise")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false),
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
      NamedStream[] streams = { new NamedStream(new File(getDir(), "test-scores-noises.mlf")) };
      
      // create deserializer
      MlfDeserializer deserializer = new MlfDeserializer();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      //for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(6, deserializer.configure(configuration, schema).size());
      assertEquals("words mapping", "word", 
		   ((Layer)configuration.get("wordLayer").getValue()).getId());
      assertEquals("noise mapping", "noise", 
		   ((Layer)configuration.get("noiseLayer").getValue()).getId());
      assertEquals("phones mapping", "phone", 
		   ((Layer)configuration.get("phoneLayer").getValue()).getId());
      assertEquals("score mapping", "score", 
		   ((Layer)configuration.get("scoreLayer").getValue()).getId());
      assertEquals("default useP2FACorrection setting", Boolean.FALSE, 
		   configuration.get("useP2FACorrection").getValue());
      assertEquals("no noise identifiers", "", 
		   configuration.get("noiseIdentifiersString").getValue());
      
      // noises included 
      configuration.get("noiseIdentifiersString").setValue("laugh unclear noise");

      deserializer.configure(configuration, schema);
      
      // check noises were split out
      assertTrue("noiseIdentifiers includes laugh",
                 deserializer.getNoiseIdentifiers().contains("laugh"));
      assertTrue("noiseIdentifiers includes unclear",
                 deserializer.getNoiseIdentifiers().contains("unclear"));
      assertTrue("noiseIdentifiers includes noise",
                 deserializer.getNoiseIdentifiers().contains("noise"));
      
      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(0, defaultParamaters.size());
      
      // configure the deserialization
      deserializer.setParameters(defaultParamaters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      assertEquals("one graph: " + graphs.length, 1, graphs.length);

      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }

      Graph g = graphs[0];
      assertEquals("fragment name", "AP513_Steve__388.470-409.460", g.getId());
      assertEquals("fragment source graph", "AP513_Steve.eaf", g.sourceGraph().getId());
      assertEquals("fragment startTime tag",
                   Double.valueOf(388.47), (Double)g.get("@startTime"));
      assertNotNull("fragment utterance layer", g.getLayer("utterance"));
      assertNotNull("fragment word layer", g.getLayer("word"));
      assertNotNull("fragment phone layer", g.getLayer("phone"));
      assertNotNull("fragment noise layer", g.getLayer("noise"));
      assertNotNull("fragment score layer", g.getLayer("score"));

      // defining annotation
      Annotation utterance = g.first("utterance");
      assertNotNull("Utterance", utterance);
      assertEquals("utterance has predictable label",
                   "AP513_Steve__388.470-409.460", utterance.getLabel());
      assertEquals("utterance start",
                   Double.valueOf(0.0), utterance.getStart().getOffset());
      assertEquals("utterance end",
                   Double.valueOf(20.99000000000001), utterance.getEnd().getOffset());
      
      // labels and times
      Annotation[] words = g.all("word");
      String[] correctWords = { "our", "family", "came", "up", "to", "christchurch", "drove", 
         "through", "the", "central", "building", "whatever", "and", "i", "was", "devastated" }; 
      Double[] wordStarts = { 0.12, 0.26, 3.58, 3.87, 4.1, 4.67, 6.99,
         7.43, 7.7, 8.57, 9.84, 11.57, 13.88, 16.18, 16.28, 16.57 }; 
      Double[] wordEnds = { 0.26, 0.9, 3.87, 4.05, 4.5, 5.52, 7.43,
         7.7, 8.06, 9.18, 10.38, 12.64, 14.23, 16.28, 16.54, 17.41}; 
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
         "_6","_@",
         "_f","_{","_m","_@","_l","_I",
         "_k","_1","_m",
         "_V","_p",
         "_t","_u",
         "_k","_r","_2","_s","_t","_J","_3","_J",
         "_d","_r","_5","_v",
         "_T","_r","_u",
         "_D","_i",
         "_s","_E","_n","_t","_r","_@","_l",
         "_b","_I","_l","_d","_I","_N",
         "_w","_Q","_t","_E","_v","_@",
         "_{","_n","_d",
         "_2",
         "_w","_Q","_z",
         "_d","_E","_v","_@","_s","_t","_1","_t","_I","_d" }; 
      String[] correctScores = {
         "-74.501472","-88.456352",
         "-72.381149","-80.422935","-76.298866","-77.614586","-80.032188","-83.345787","-61.570362",
         "-82.980652","-79.888588","-76.021339",
         "-80.791237","-74.185844","-60.462578",
         "-82.366051","-73.640511",
         "-81.215950","-75.076324","-88.129120","-79.540848","-85.040924","-87.444504","-80.925194","-74.313751","-67.355667",
         "-88.261612","-79.722382","-79.100876","-70.732887",
         "-74.491776","-77.317619","-77.316399",
         "-78.758072","-81.341545",
         "-74.552338","-78.420929","-80.989769","-82.882103","-88.579872","-89.756905","-77.090782","-60.521915",
         "-86.141991","-89.725822","-77.033241","-86.492111","-80.314415","-70.944778","-72.876167",
         "-79.772171","-89.542480","-85.945908","-77.870911","-76.900627","-88.874947","-61.112000",
         "-69.476707","-69.313995","-83.793709",
         "-81.505508",
         "-72.885101","-81.001671","-75.671638",
         "-92.774216","-76.531364","-76.395500","-95.949585","-78.116875","-90.150795","-82.994461","-82.912346","-81.953323","-81.954918",
      }; 
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
         assertEquals("score for phone " + i,
                    correctScores[i], phones[i].first("score").getLabel());
      }

      Annotation[] noises = g.all("noise");
      assertEquals(1, noises.length);
      assertEquals("label for noise",
                   "LAUGH", noises[0].getLabel());
      assertEquals("confidence for noise ",
                   Constants.CONFIDENCE_AUTOMATIC, (int)noises[0].getConfidence());
      assertEquals("offset for noise start ",
                   Double.valueOf(13.85), noises[0].getStart().getOffset());
      assertEquals("offset for noise end ",
                   Double.valueOf(13.88), noises[0].getEnd().getOffset());
      assertEquals("confidence for noise start ",
                   Constants.CONFIDENCE_AUTOMATIC, (int)noises[0].getStart().getConfidence());
      assertEquals("confidence for noise end ",
                   Constants.CONFIDENCE_AUTOMATIC, (int)noises[0].getEnd().getConfidence());
   }

   // scores and noises
   @Test public void ignoreNoise()  throws Exception {
      Schema schema = new Schema(
         "participant", "turn", "utterance", "word",
	 new Layer("noise", "Noise")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false),
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
      NamedStream[] streams = { new NamedStream(new File(getDir(), "test-scores-noises.mlf")) };
      
      // create deserializer
      MlfDeserializer deserializer = new MlfDeserializer();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      //for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(6, deserializer.configure(configuration, schema).size());
      assertEquals("words mapping", "word", 
		   ((Layer)configuration.get("wordLayer").getValue()).getId());
      assertEquals("noise mapping", "noise", 
		   ((Layer)configuration.get("noiseLayer").getValue()).getId());
      assertEquals("phones mapping", "phone", 
		   ((Layer)configuration.get("phoneLayer").getValue()).getId());
      assertEquals("score mapping", "score", 
		   ((Layer)configuration.get("scoreLayer").getValue()).getId());
      assertEquals("default useP2FACorrection setting", Boolean.FALSE, 
		   configuration.get("useP2FACorrection").getValue());
      assertEquals("no noise identifiers", "", 
		   configuration.get("noiseIdentifiersString").getValue());
      
      // noises must be parsed out
      configuration.get("noiseIdentifiersString").setValue("laugh unclear noise");

      // but we don't want noises
      configuration.get("noiseLayer").setValue(null);
      
      deserializer.configure(configuration, schema);
      
      // check noises were split out
      assertTrue("noiseIdentifiers includes laugh",
                 deserializer.getNoiseIdentifiers().contains("laugh"));
      assertTrue("noiseIdentifiers includes unclear",
                 deserializer.getNoiseIdentifiers().contains("unclear"));
      assertTrue("noiseIdentifiers includes noise",
                 deserializer.getNoiseIdentifiers().contains("noise"));
      
      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(0, defaultParamaters.size());
      
      // configure the deserialization
      deserializer.setParameters(defaultParamaters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      assertEquals("one graph: " + graphs.length, 1, graphs.length);

      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }

      Graph g = graphs[0];
      assertEquals("fragment name", "AP513_Steve__388.470-409.460", g.getId());
      assertEquals("fragment source graph", "AP513_Steve.eaf", g.sourceGraph().getId());
      assertEquals("fragment startTime tag",
                   Double.valueOf(388.47), (Double)g.get("@startTime"));
      assertNotNull("fragment utterance layer", g.getLayer("utterance"));
      assertNotNull("fragment word layer", g.getLayer("word"));
      assertNotNull("fragment phone layer", g.getLayer("phone"));
      assertNull("fragment no noise layer", g.getLayer("noise"));
      assertNotNull("fragment score layer", g.getLayer("score"));

      // defining annotation
      Annotation utterance = g.first("utterance");
      assertNotNull("Utterance", utterance);
      assertEquals("utterance has predictable label",
                   "AP513_Steve__388.470-409.460", utterance.getLabel());
      assertEquals("utterance start",
                   Double.valueOf(0.0), utterance.getStart().getOffset());
      assertEquals("utterance end",
                   Double.valueOf(20.99000000000001), utterance.getEnd().getOffset());
      
      // labels and times
      Annotation[] words = g.all("word");
      String[] correctWords = { "our", "family", "came", "up", "to", "christchurch", "drove", 
         "through", "the", "central", "building", "whatever", "and", "i", "was", "devastated" }; 
      Double[] wordStarts = { 0.12, 0.26, 3.58, 3.87, 4.1, 4.67, 6.99,
         7.43, 7.7, 8.57, 9.84, 11.57, 13.88, 16.18, 16.28, 16.57 }; 
      Double[] wordEnds = { 0.26, 0.9, 3.87, 4.05, 4.5, 5.52, 7.43,
         7.7, 8.06, 9.18, 10.38, 12.64, 14.23, 16.28, 16.54, 17.41}; 
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
         "_6","_@",
         "_f","_{","_m","_@","_l","_I",
         "_k","_1","_m",
         "_V","_p",
         "_t","_u",
         "_k","_r","_2","_s","_t","_J","_3","_J",
         "_d","_r","_5","_v",
         "_T","_r","_u",
         "_D","_i",
         "_s","_E","_n","_t","_r","_@","_l",
         "_b","_I","_l","_d","_I","_N",
         "_w","_Q","_t","_E","_v","_@",
         "_{","_n","_d",
         "_2",
         "_w","_Q","_z",
         "_d","_E","_v","_@","_s","_t","_1","_t","_I","_d" }; 
      String[] correctScores = {
         "-74.501472","-88.456352",
         "-72.381149","-80.422935","-76.298866","-77.614586","-80.032188","-83.345787","-61.570362",
         "-82.980652","-79.888588","-76.021339",
         "-80.791237","-74.185844","-60.462578",
         "-82.366051","-73.640511",
         "-81.215950","-75.076324","-88.129120","-79.540848","-85.040924","-87.444504","-80.925194","-74.313751","-67.355667",
         "-88.261612","-79.722382","-79.100876","-70.732887",
         "-74.491776","-77.317619","-77.316399",
         "-78.758072","-81.341545",
         "-74.552338","-78.420929","-80.989769","-82.882103","-88.579872","-89.756905","-77.090782","-60.521915",
         "-86.141991","-89.725822","-77.033241","-86.492111","-80.314415","-70.944778","-72.876167",
         "-79.772171","-89.542480","-85.945908","-77.870911","-76.900627","-88.874947","-61.112000",
         "-69.476707","-69.313995","-83.793709",
         "-81.505508",
         "-72.885101","-81.001671","-75.671638",
         "-92.774216","-76.531364","-76.395500","-95.949585","-78.116875","-90.150795","-82.994461","-82.912346","-81.953323","-81.954918",
      }; 
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
         assertEquals("score for phone " + i,
                    correctScores[i], phones[i].first("score").getLabel());
      }


      Annotation[] noises = g.all("noise");
      assertEquals("no noises: " + Arrays.asList(noises), 0, noises.length);
   }

   @Test public void fragmentNamesWithHyphens()  throws Exception {
      Schema schema = new Schema(
         "participant", "turn", "utterance", "word",
	 new Layer("noise", "Noise")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false),
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
      NamedStream[] streams = { new NamedStream(new File(getDir(), "test-hyphen.mlf")) };
      
      // create deserializer
      MlfDeserializer deserializer = new MlfDeserializer();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      //for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(6, deserializer.configure(configuration, schema).size());
      assertEquals("words mapping", "word", 
		   ((Layer)configuration.get("wordLayer").getValue()).getId());
      assertEquals("noise mapping", "noise", 
		   ((Layer)configuration.get("noiseLayer").getValue()).getId());
      assertEquals("phones mapping", "phone", 
		   ((Layer)configuration.get("phoneLayer").getValue()).getId());
      assertEquals("score mapping", "score", 
		   ((Layer)configuration.get("scoreLayer").getValue()).getId());
      assertEquals("default useP2FACorrection setting", Boolean.FALSE, 
		   configuration.get("useP2FACorrection").getValue());
      assertEquals("no noise identifiers", "", 
		   configuration.get("noiseIdentifiersString").getValue());

      // for this test, we don't want scores nor noises
      configuration.get("scoreLayer").setValue(null);
      configuration.get("noiseLayer").setValue(null);

      deserializer.configure(configuration, schema);

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(0, defaultParamaters.size());
      
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
      assertNull("first fragment no noise layer", g.getLayer("noise"));

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
      assertEquals("second fragment name",
                   "AP511_MikeThorpe__7.131-13.887", g.getId());
      assertEquals("second fragment source graph",
                   "AP511_MikeThorpe.eaf", g.sourceGraph().getId());
      assertEquals("second fragment startTime tag",
                   Double.valueOf(7.131), (Double)g.get("@startTime"));
   }

   @Test public void phraseLayers()  throws Exception {
      Schema schema = new Schema(
         "participant", "turn", "utterance", "word",
	 new Layer("noise", "Noise")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false),
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
         new Layer("htk_word", "Word Alignment")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false)
         .setParentId("turn"),
         new Layer("htk_phone", "Phone Alignment")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false)
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
      //for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(6, deserializer.configure(configuration, schema).size());
      assertEquals("words mapping", "word", 
		   ((Layer)configuration.get("wordLayer").getValue()).getId());
      assertEquals("noise mapping", "noise", 
		   ((Layer)configuration.get("noiseLayer").getValue()).getId());
      assertEquals("phones mapping", "phone", 
		   ((Layer)configuration.get("phoneLayer").getValue()).getId());
      assertEquals("score mapping", "score", 
		   ((Layer)configuration.get("scoreLayer").getValue()).getId());
      assertEquals("default useP2FACorrection setting", Boolean.FALSE, 
		   configuration.get("useP2FACorrection").getValue());
      assertEquals("no noise identifiers", "", 
		   configuration.get("noiseIdentifiersString").getValue());

      // for this test, we don't want scores nor noises
      configuration.get("scoreLayer").setValue(null);
      configuration.get("noiseLayer").setValue(null);

      // change word/phone to phrase layers
      configuration.get("wordLayer").setValue(schema.getLayer("htk_word"));
      configuration.get("phoneLayer").setValue(schema.getLayer("htk_phone"));

      deserializer.configure(configuration, schema);

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(0, defaultParamaters.size());
      
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
      assertNotNull("first fragment word layer", g.getLayer("htk_word"));
      assertNotNull("first fragment phone layer", g.getLayer("htk_phone"));
      assertNull("first fragment no score layer", g.getLayer("score"));
      assertNull("first fragment no noise layer", g.getLayer("noise"));

      // defining annotation
      Annotation utterance = g.first("utterance");
      assertNotNull("Utterance", utterance);
      assertEquals("utterance has predictable label",
                   "AP511_MikeThorpe__1.373-7.131", utterance.getLabel());
      assertEquals("utterance start", Double.valueOf(0.0), utterance.getStart().getOffset());
      assertEquals("utterance end", Double.valueOf(5.758), utterance.getEnd().getOffset());
      
      // labels and times
      assertEquals("no words", 0, g.all("word").length);
      assertEquals("no phones", 0, g.all("phone").length);
      Annotation[] words = g.all("htk_word");
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

      Annotation[] phones = g.all("htk_phone");
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
         // in this case, the parent should be a turn, but there's no turn set in the fragment
         // so we don't test anything about the parent
      }

      g = graphs[1];
      assertEquals("second fragment name",
                   "AP511_MikeThorpe__7.131-13.887", g.getId());
      assertEquals("second fragment source graph",
                   "AP511_MikeThorpe.eaf", g.sourceGraph().getId());
      assertEquals("second fragment startTime tag",
                   Double.valueOf(7.131), (Double)g.get("@startTime"));
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
      org.junit.runner.JUnitCore.main("nzilbb.formatter.htk.mlf.TestMlfDeserializer");
   }
}
