//
// Copyright 2021 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.formatter.salt;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.Vector;
import java.util.stream.Collectors;
import nzilbb.ag.*;
import nzilbb.ag.serialize.SerializationException;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.editpath.EditStep;
import nzilbb.editpath.MinimumEditPath;

public class TestSltSerialization {
  
  @Test public void deserialize()  throws Exception {
    Schema schema = new Schema(
      "participant", "turn", "utterance", "word",
      new Layer("transcript_language", "Language").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("transcript_doe", "Recording date").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("transcript_ca", "Current Age").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("transcript_context", "Context").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("transcript_subgroup", "Subgroup/Story").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("transcript_collect", "Collection Point").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("transcript_location", "Location").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      
      new Layer("comment", "Comments").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(true).setSaturated(false),
      
      new Layer("participant", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("main_participant", "Target Speaker").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("participant").setParentIncludes(true),
      new Layer("participant_id", "Participant ID").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("participant").setParentIncludes(true),
      new Layer("participant_gender", "Gender").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("participant").setParentIncludes(true),
      new Layer("participant_dob", "Birth Date").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("participant").setParentIncludes(true),
      new Layer("participant_ethnicity", "Ethnicity").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("participant").setParentIncludes(true),
      
      new Layer("turn", "Speaker Turn").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("participant").setParentIncludes(true),
      new Layer("utterance", "Lines").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),

      new Layer("pause", "Pauses").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("cunit", "C-Unit").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("parenthetical", "Parentheticals").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("repetition", "Repetitions").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("error", "Errors").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("code", "Non-error Codes").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("maze", "Mazes").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("noise", "Verbal sound effects etc.").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("entity", "Proper Names").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),

      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      
      new Layer("root", "Root form").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true),
      new Layer("bound_morpheme", "Bound Morphemes").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true),
      new Layer("partial_word", "Partial Words").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true),
      new Layer("omission", "Omissions").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true)
      );
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "test.slt")) };
    
    // create deserializer
    SltSerialization deserializer = new SltSerialization();
    
    // general configuration
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) {
    //   System.out.println("" + p.getName() + " = " + p.getValue());
    // }
    assertEquals("Correct number of configuration parameters", 26, configuration.size());
    assertEquals("cunit",
                 ((Layer)configuration.get("cUnitLayer").getValue()).getId());
    assertEquals("main_participant",
                 ((Layer)configuration.get("targetParticipantLayer").getValue()).getId());
    assertEquals("comment",
                 ((Layer)configuration.get("commentLayer").getValue()).getId());
    assertEquals("parenthetical",
                 ((Layer)configuration.get("parentheticalLayer").getValue()).getId());
    assertEquals("entity",
                 ((Layer)configuration.get("properNameLayer").getValue()).getId());
    assertEquals("repetition",
                 ((Layer)configuration.get("repetitionsLayer").getValue()).getId());
    assertEquals("root",
                 ((Layer)configuration.get("rootLayer").getValue()).getId());
    assertEquals("error",
                 ((Layer)configuration.get("errorLayer").getValue()).getId());
    assertEquals("noise",
                 ((Layer)configuration.get("soundEffectLayer").getValue()).getId());
    assertEquals("pause",
                 ((Layer)configuration.get("pauseLayer").getValue()).getId());
    assertEquals("bound_morpheme",
                 ((Layer)configuration.get("boundMorphemeLayer").getValue()).getId());
    assertEquals("maze",
                 ((Layer)configuration.get("mazeLayer").getValue()).getId());
    assertEquals("partial_word",
                 ((Layer)configuration.get("partialWordLayer").getValue()).getId());
    assertEquals("omission",
                 ((Layer)configuration.get("omissionLayer").getValue()).getId());
    assertEquals("code",
                 ((Layer)configuration.get("codeLayer").getValue()).getId());
    assertEquals("transcript_language",
                 ((Layer)configuration.get("languageLayer").getValue()).getId());
    assertEquals("participant_id",
                 ((Layer)configuration.get("participantIdLayer").getValue()).getId());
    assertEquals("participant_gender",
                 ((Layer)configuration.get("genderLayer").getValue()).getId());
    assertEquals("participant_dob",
                 ((Layer)configuration.get("dobLayer").getValue()).getId());
    assertEquals("transcript_doe",
                 ((Layer)configuration.get("doeLayer").getValue()).getId());
    assertEquals("transcript_ca",
                 ((Layer)configuration.get("caLayer").getValue()).getId());
    assertEquals("participant_ethnicity",
                 ((Layer)configuration.get("ethnicityLayer").getValue()).getId());
    assertEquals("transcript_context",
                 ((Layer)configuration.get("contextLayer").getValue()).getId());
    assertEquals("transcript_subgroup",
                 ((Layer)configuration.get("subgroupLayer").getValue()).getId());
    assertEquals("transcript_collect",
                 ((Layer)configuration.get("collectLayer").getValue()).getId());
    assertEquals("transcript_location",
                 ((Layer)configuration.get("locationLayer").getValue()).getId());

    // final configuration
    deserializer.configure(configuration, schema);
    
    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    for (Parameter p : defaultParameters.values()) {
      System.out.println("" + p.getName() + " = " + p.getValue());
    }
    assertEquals("No stream-specific parameters", 0, defaultParameters.size());

    // configure the deserialization
    deserializer.setParameters(defaultParameters);
    
    // build the graph
    Graph[] graphs = deserializer.deserialize();
    assertEquals("conversion is 1-1", 1, graphs.length);
    Graph g = graphs[0];
    
    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
    
    assertEquals("test.slt", g.getId());

    // meta-data
    assertEquals("Language set", 1, g.all("transcript_language").length);
    assertEquals("Language correct", "English", g.first("transcript_language").getLabel()); // TODO ISO code
    assertEquals("Doe set", 1, g.all("transcript_doe").length);
    assertEquals("Doe correct", "30/3/2021", g.first("transcript_doe").getLabel()); // TODO ISO format
    assertEquals("Ca set", 1, g.all("transcript_ca").length);
    assertEquals("Ca correct", "4;4", g.first("transcript_ca").getLabel());
    assertEquals("Context set", 1, g.all("transcript_context").length);
    assertEquals("Context correct", "Nar", g.first("transcript_context").getLabel());
    assertEquals("Subgroup set", 1, g.all("transcript_subgroup").length);
    assertEquals("Subgroup correct", "TEST", g.first("transcript_subgroup").getLabel());
    assertEquals("Collect set", 1, g.all("transcript_collect").length);
    assertEquals("Collect correct", "1", g.first("transcript_collect").getLabel());
    assertEquals("Location not set", 0, g.all("transcript_location").length);
    
    // participants     
    assertEquals("Two participants", 2, g.all("participant").length);
    assertEquals("One target", 1, g.all("main_participant").length);
    Annotation child = g.first("main_participant").getParent();
    assertEquals("Target partcipant correct", "Child", child.getLabel()); // TODO name after the graph
    Annotation examiner = null;
    for (Annotation p : g.all("participant")) {
      if (!p.getId().equals(child.getId())) {
        examiner = p;
        break;
      }
    }
    assertNotNull("Examiner found", examiner);
    
    // participant meta data
    assertEquals("ParticipantId correct",
                 "Ada-Lovelace", child.first("participant_id").getLabel());
    assertEquals("Gender correct",
                 "F", child.first("participant_gender").getLabel());
    assertEquals("Dob correct",
                 "26/9/1972", child.first("participant_dob").getLabel());
    assertEquals("Ethnicity correct",
                 "NZ European", child.first("participant_ethnicity").getLabel());
    assertNull("Examiner ParticipantId not set", examiner.first("participant_id"));
    assertNull("Examiner Gender not set", examiner.first("participant_gender"));
    assertNull("Examiner Dob not set", examiner.first("participant_dob"));
    assertNull("Examiner Ethnicity not set", examiner.first("participant_ethnicity"));

     // turns
    Annotation[] turns = g.all("turn");
    assertEquals(12, turns.length);

    // check turn timestamps
    double[] turnTimeStamps = {
      0, 29, 34, 39, 47, 52, 62, 63, 69, 71, 86, 93 };
    Annotation lastTurn = null;
    assertEquals("Examiner is first", "Examiner", turns[0].getLabel());
    for (int t = 0; t < turns.length; t++) {
      assertEquals("start of turn " + t,
                   Double.valueOf(turnTimeStamps[t]), turns[t].getStart().getOffset());
      assertEquals("confidence of start of turn " + t + " " + turns[t].getStart(),
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL),
                   turns[t].getStart().getConfidence());
      if (lastTurn != null) {
        assertEquals("End of turn " + (t-1),
                     turns[t].getStart(), lastTurn.getEnd());
        assertNotEquals("Speaker has changed",
                        lastTurn.getLabel(), turns[t].getLabel());
      }
      lastTurn = turns[t];
    } // next turn
    // transcript has no end timestamp, so it should be 1s after the last known timestamp
    assertEquals("Dummy last timestamp (" + lastTurn.getStart() + "-" + lastTurn.getEnd() + ")",
                 Double.valueOf(94.0), lastTurn.getEnd().getOffset());
    assertEquals("Dummy last timestamp has low confidence",
                 Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC),
                 lastTurn.getEnd().getConfidence());
      
    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals(21, utterances.length);
    
    // check utterance timestamps and speakers
    double[] utteranceTimeStamps = {
      0, 5, 12, 16, 19, 23, 29, 34, 37, 39, 41, 46, 47, 52, 62, 63, 69, 71, 83, 86, 93 };
    String[] utteranceSpeakers = {
      "Examiner", "Examiner", "Examiner", "Examiner", "Examiner", "Examiner",
      "Child",
      "Examiner", "Examiner",
      "Child", "Child", "Child",
      "Examiner",
      "Child",
      "Examiner",
      "Child",
      "Examiner",
      "Child", "Child",
      "Examiner",
      "Child"
    };
    Annotation lastUtterance = null;
    for (int u = 0; u < utterances.length; u++) {
      assertEquals("start of utterance " + u,
                   Double.valueOf(utteranceTimeStamps[u]), utterances[u].getStart().getOffset());
      assertEquals("confidence of start of utterance " + u,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL),
                   utterances[u].getStart().getConfidence());
      assertEquals("speaker of utterance " + u + " ("+utterances[u].getStart()+")",
                   utteranceSpeakers[u], utterances[u].getParent().getLabel());
      if (lastUtterance != null) {
        assertEquals("End of utterance " + (u-1) + " ("+utterances[u-1].getStart()+")",
                     utterances[u].getStart(), lastUtterance.getEnd());
      }
      lastUtterance = utterances[u];
    } // next utterance
    // transcript has no end timestamp, so it should be 1s after the last known timestamp
    assertEquals("Dummy last timestamp",
                 Double.valueOf(94.0), lastUtterance.getEnd().getOffset());
    assertEquals("Dummy last timestamp has low confidence",
                 Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC),
                 lastUtterance.getEnd().getConfidence());

    // check utterance transcriptions
    String[] lines = { // TODO strip out annotations
      "I'm at Byron kindergarten on the 30th of March 2021.",
      "My name is X[REDACTED], and I'm here with X[REDACTED], doing the oral language assessment [CENSOR].",
      "Okay, so now Ada it's your turn to tell the story.",
      "You can look at the pictures when you're telling the story.",
      "So let's start at the beginning.",
      "What was the story about?",
      "(Um the kids) the kid/s, they quickly put their gumboot/s on.",
      "Ah Mm hmm.",
      "Anything else?",
      "And please go for a walk [EU]?",
      "You need to put your gumboot/s on.",
      "It/'s too dark.",
      "What happened in this one?",
      "And then it/'s too dark.",
      "What happened next?",
      "Schnitzel_von_Krumm s* fell out *of the nest.",
      "What happened next?",
      "They (put them) put it back in the nest.",
      "Bye bye little bird.",
      "Anything else that happened?",
      "x."
    };
    for (int u = 0; u < utterances.length; u++) {
      String transcription = Arrays.stream(utterances[u].all("word"))
        .map(word -> word.getLabel())
        .collect(Collectors.joining(" "));
      assertEquals("Transcription of utterance " + u + " ("+utterances[u].getStart()+")",
                   lines[u], transcription);
    }        
      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }
    
  }

   // @Test public void serialize() throws Exception {
   //    Schema schema = new Schema(
   //       "who", "turn", "utterance", "word",
   //       new Layer("scribe", "Transcriber")
   //       .setAlignment(Constants.ALIGNMENT_NONE)
   //       .setPeers(true).setPeersOverlap(true).setSaturated(true),
   //       new Layer("transcript_language", "Language")
   //       .setAlignment(Constants.ALIGNMENT_NONE)
   //       .setPeers(false).setPeersOverlap(false).setSaturated(true),
   //       new Layer("noise", "Non-speech noises")
   //       .setAlignment(Constants.ALIGNMENT_INTERVAL)
   //       .setPeers(true).setPeersOverlap(false).setSaturated(false),
   //       new Layer("who", "Participants")
   //       .setAlignment(Constants.ALIGNMENT_NONE)
   //       .setPeers(true).setPeersOverlap(true).setSaturated(true),
   //       new Layer("main_participant", "Main speaker")
   //       .setAlignment(Constants.ALIGNMENT_NONE)
   //       .setPeers(false).setPeersOverlap(false).setSaturated(true)
   //       .setParentId("who").setParentIncludes(true),
   //       new Layer("participant_gender", "Gender")
   //       .setAlignment(Constants.ALIGNMENT_NONE)
   //       .setPeers(false).setPeersOverlap(false).setSaturated(true)
   //       .setParentId("who").setParentIncludes(true),
   //       new Layer("participant_age", "Age")
   //       .setAlignment(Constants.ALIGNMENT_NONE)
   //       .setPeers(false).setPeersOverlap(false).setSaturated(true)
   //       .setParentId("who").setParentIncludes(true),
   //       new Layer("participant_language", "Language")
   //       .setAlignment(Constants.ALIGNMENT_NONE)
   //       .setPeers(false).setPeersOverlap(false).setSaturated(true)
   //       .setParentId("who").setParentIncludes(true),
   //       new Layer("turn", "Speaker turns")
   //       .setAlignment(Constants.ALIGNMENT_INTERVAL)
   //       .setPeers(true).setPeersOverlap(false).setSaturated(false)
   //       .setParentId("who").setParentIncludes(true),
   //       new Layer("utterance", "Utterances")
   //       .setAlignment(Constants.ALIGNMENT_INTERVAL)
   //       .setPeers(true).setPeersOverlap(false).setSaturated(true)
   //       .setParentId("turn").setParentIncludes(true),
   //       new Layer("word", "Words")
   //       .setAlignment(Constants.ALIGNMENT_INTERVAL)
   //       .setPeers(true).setPeersOverlap(false).setSaturated(false)
   //       .setParentId("turn").setParentIncludes(true));
      
   //    File dir = getDir();
   //    Graph graph = new Graph()
   //       .setId("serialize-test.txt")
   //       .setSchema(schema);
   //    graph.addAnchor(new Anchor("a0", 0.0));
   //    graph.addAnchor(new Anchor("a5", 5.4321)); // will be rendered 5432
   //    graph.addAnchor(new Anchor("a10", 10.0));
   //    graph.addAnchor(new Anchor("a15", 15.0));
   //    // language
   //    graph.addAnnotation(new Annotation("lang", "en", "transcript_language", "a0", "a15"));
   //    // participants
   //    graph.addAnnotation(new Annotation("child", "John Smith", "who", "a0", "a15"));
   //    graph.addAnnotation(new Annotation("mother", "Mrs. Smith", "who", "a0", "a15"));
   //    graph.addAnnotation(new Annotation("child-main", "John Smith", "main_participant", "a0", "a15",
   //                                       "child"));
   //    graph.addAnnotation(new Annotation("child-age", "2;10.10", "participant_age", "a0", "a15",
   //                                       "child"));
   //    graph.addAnnotation(new Annotation("child-gender", "M", "participant_gender", "a0", "a15",
   //                                       "child"));
   //    graph.addAnnotation(new Annotation("child-language", "en", "participant_language", "a0", "a15",
   //                                       "child"));
   //    graph.addAnnotation(new Annotation("mother-language", "Spanish", "participant_language", "a0", "a15",
   //                                       "mother"));
   //    // turns
   //    graph.addAnnotation(new Annotation("t1", "John Smith", "turn", "a0", "a10", "child"));
   //    graph.addAnnotation(new Annotation("t2", "Mrs. Smith", "turn", "a10", "a15", "mother"));
   //    // utterances
   //    graph.addAnnotation(new Annotation("u1", "John Smith", "utterance", "a0", "a5", "t1"));
   //    graph.addAnnotation(new Annotation("u2", "John Smith", "utterance", "a5", "a10", "t1"));
   //    graph.addAnnotation(new Annotation("u3", "Mrs. Smith", "utterance", "a10", "a15", "t2"));
      
   //    // words
   //    graph.addAnnotation(new Annotation("the", "The", "word",
   //                                       "a0",
   //                                       // 1.2345 will become ..._1234
   //                                       graph.addAnchor(new Anchor("a1", 1.0)).getId(),
   //                                       "t1"));
   //    graph.addAnnotation(new Annotation("quick", "'quick", "word", 
   //                                       "a1",
   //                                       graph.addAnchor(new Anchor("a2", 2.0)).getId(),
   //                                       "t1"));
   //    graph.addAnnotation(new Annotation("brown", "brown'", "word", 
   //                                       "a2",
   //                                       graph.addAnchor(new Anchor("a3", 3.0)).getId(),
   //                                       "t1"));
   //    graph.addAnnotation(new Annotation("fox", "fox", "word", 
   //                                       "a3",
   //                                       "a5",
   //                                       "t1"));
      
   //    graph.addAnnotation(new Annotation("jumps", "jumps -", "word", 
   //                                       "a5",
   //                                       graph.addAnchor(new Anchor("a6", 6.0)).getId(),
   //                                       "t1"));      
   //    graph.addAnnotation(new Annotation("over", "over", "word",
   //                                       "a6",
   //                                       graph.addAnchor(new Anchor("a7", 8.0)).getId(),
   //                                       "t1"));
   //    // noise
   //    graph.addAnnotation(new Annotation("cough", "coughs", "noise",
   //                                       "a7",
   //                                       graph.addAnchor(new Anchor("a8", 8.0)).getId(),
   //                                       "t1"));
      
   //    graph.addAnnotation(new Annotation("th~", "th~", "word", // th~ becomes &th
   //                                       "a8",
   //                                       "a10",
   //                                       "t1"));
      
   //    graph.addAnnotation(new Annotation("the2", "the", "word", 
   //                                       "a10",
   //                                       graph.addAnchor(new Anchor("a12", 12.0)).getId(),
   //                                       "t2"));
   //    graph.addAnnotation(new Annotation("lazy", "lazy", "word", 
   //                                       "a12",
   //                                       graph.addAnchor(new Anchor("a13", 13.0)).getId(),
   //                                       "t2"));
   //    graph.addAnnotation(new Annotation("dog", "\"dog\"", "word", 
   //                                       "a13",
   //                                       graph.addAnchor(new Anchor("a14", 14.0)).getId(),
   //                                       "t2"));
   //    graph.addAnnotation(new Annotation(".", ".", "word", 
   //                                       "a14",
   //                                       "a15",
   //                                       "t2"));
      
   //    // create serializer
   //    ChatSerialization serializer = new ChatSerialization();
      
   //    // general configuration
   //    ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
   //    //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
   //    configuration = serializer.configure(configuration, schema);
   //    assertEquals(22, configuration.size());
   //    assertEquals("scribe attribute", "scribe", 
   //      	   ((Layer)configuration.get("transcriberLayer").getValue()).getId());
   //    assertEquals("languages attribute", "transcript_language", 
   //      	   ((Layer)configuration.get("languagesLayer").getValue()).getId());
   //    assertEquals("target participant attribute", "main_participant", 
   //      	   ((Layer)configuration.get("targetParticipantLayer").getValue()).getId());
   //    assertEquals("non-word layer", "noise", 
   //      	   ((Layer)configuration.get("nonWordLayer").getValue()).getId());
   //    assertEquals("sex attribute mapped to gender", "participant_gender",
   //                 ((Layer)configuration.get("sexLayer").getValue()).getId());
   //    assertEquals("age attribute", "participant_age",
   //                 ((Layer)configuration.get("ageLayer").getValue()).getId());

   //    LinkedHashSet<String> needLayers = new LinkedHashSet<String>(
   //       Arrays.asList(serializer.getRequiredLayers()));
   //    assertEquals("Needed layers: " + needLayers,
   //                 11, needLayers.size());
   //    assertTrue(needLayers.contains("who"));
   //    assertTrue(needLayers.contains("main_participant"));
   //    assertTrue(needLayers.contains("scribe"));
   //    assertTrue(needLayers.contains("transcript_language"));
   //    assertTrue(needLayers.contains("turn"));
   //    assertTrue(needLayers.contains("utterance"));
   //    assertTrue(needLayers.contains("word"));
   //    assertTrue(needLayers.contains("noise"));
   //    assertTrue(needLayers.contains("participant_age"));
   //    assertTrue(needLayers.contains("participant_gender"));
   //    assertTrue(needLayers.contains("participant_language"));
      
   //    // serialize
   //    final Vector<SerializationException> exceptions = new Vector<SerializationException>();
   //    final Vector<NamedStream> streams = new Vector<NamedStream>();
   //    String[] layers = {"word","transcript_language"};
   //    Graph[] graphs = { graph };
   //    serializer.serialize(Arrays.spliterator(graphs), layers,
   //                         stream -> streams.add(stream),
   //                         warning -> System.out.println(warning),
   //                         exception -> exceptions.add(exception));
   //    if (exceptions.size() > 0) {
   //       fail(exceptions.stream()
   //            .map(x -> {
   //                  StringWriter sw = new StringWriter();
   //                  PrintWriter pw = new PrintWriter(sw);
   //                  x.printStackTrace(pw);
   //                  return x.toString() + ": " + sw;
   //               })
   //            .collect(Collectors.joining("\n","","")));
   //    }
      
   //    streams.elementAt(0).save(dir);
      
   //    // test using diff
   //    File result = new File(dir, "serialize-test.cha");
   //    String differences = diff(new File(dir, "expected_serialize-test.cha"), result);
   //    if (differences != null) {
   //       fail(differences);
   //    } else {
   //       result.delete();
   //    }
   // }

   /**
    * Diffs two files.
    * @param expected
    * @param actual
    * @return null if the files are the same, and a String describing differences if not.
    */
   public String diff(File expected, File actual) {
      StringBuffer d = new StringBuffer();
      
      try {
         // compare with what we expected
         Vector<String> actualLines = new Vector<String>();
         BufferedReader reader = new BufferedReader(new FileReader(actual));
         String line = reader.readLine();
         while (line != null) {
            actualLines.add(line);
            line = reader.readLine();
         }
         Vector<String> expectedLines = new Vector<String>();
         reader = new BufferedReader(new FileReader(expected));
         line = reader.readLine();
         while (line != null) {
            expectedLines.add(line);
            line = reader.readLine();
         }
         MinimumEditPath<String> comparator = new MinimumEditPath<String>();
         List<EditStep<String>> path = comparator.minimumEditPath(expectedLines, actualLines);
         for (EditStep<String> step : path) {
            switch (step.getOperation()) {
               case CHANGE:
                  d.append("\n"+expected.getPath()+":"+(step.getFromIndex()+1)+": Expected:\n" 
                           + step.getFrom() 
                           + "\n"+actual.getPath()+":"+(step.getToIndex()+1)+": Found:\n" + step.getTo());
                  break;
               case DELETE:
                  d.append("\n"+expected.getPath()+":"+(step.getFromIndex()+1)+": Deleted:\n" 
                           + step.getFrom()
                           + "\n"+actual.getPath()+":"+(step.getToIndex()+1)+": Missing");
                  break;
               case INSERT:
                  d.append("\n"+expected.getPath()+":"+(step.getFromIndex()+1)+": Missing" 
                           + "\n"+actual.getPath()+":"+(step.getToIndex()+1)+": Inserted:\n" 
                           + step.getTo());
                  break;
            }
         } // next step
      } catch(Exception exception) {
         d.append("\n" + exception);
      }
      if (d.length() > 0) return d.toString();
      return null;
   } // end of diff()

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
      org.junit.runner.JUnitCore.main("nzilbb.formatter.salt.TestSltSerialization");
   }
}
