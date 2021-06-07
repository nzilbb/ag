//
// Copyright 2016-2021 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.formatter.clan;
	      
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
import nzilbb.formatter.clan.*;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.editpath.EditStep;
import nzilbb.editpath.MinimumEditPath;

public class TestChatSerialization {
   
   @Test public void minimalConversion()  throws Exception {
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("transcript_date", "Recording date")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_location", "Location")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_recording_quality", "Recording quality")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_room_layout", "Room layout")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_tape_location", "Tape location")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcriber", "Transcribers", 0, true, true, true),
	 new Layer("languages", "Graph language", 0, true, true, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("language", "Speaker language", 0, false, false, true, "who", true),
	 new Layer("corpus", "Speaker corpus", 0, false, false, true, "who", true),
	 new Layer("role", "Speaker role", 0, false, false, true, "who", true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("c-unit", "C-Units", 2, true, false, false, "turn", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("linkage", "Linkages", 2, true, false, false, "turn", true),
	 new Layer("error", "Errors", 2, true, false, false, "turn", true),
	 new Layer("retracing", "Retracing", 2, true, false, false, "turn", true),
	 new Layer("repetition", "Repetitions", 2, true, false, false, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true),
	 new Layer("completion", "Completion", 0, true, false, false, "word", true),
	 new Layer("expansion", "Expansion", 0, false, false, true, "word", true),
	 new Layer("disfluency", "Disfluency", 0, false, false, true, "word", true),
	 new Layer("gem", "Gems", 2, true, false, true));
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "test.cha")) };

      // create deserializer
      ChatSerialization deserializer = new ChatSerialization();

      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(39, deserializer.configure(configuration, schema).size());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      for (String warning : deserializer.getWarnings()) System.out.println(warning);
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }

      // meta data
      assertEquals("test.cha", g.getId());
      String[] transcribers = g.labels("transcriber"); 
      assertEquals(2, transcribers.length);
      assertEquals("Alan Turing", transcribers[0]);
      assertEquals("Oscar Wilde", transcribers[1]);
      
      String[] languages = g.labels("languages"); 
      assertEquals(1, languages.length);
      assertEquals("en", languages[0]);
      
      assertEquals("1965-07-01", g.my("transcript_date").getLabel());
      assertEquals("Boston, MA, USA", g.my("transcript_location").getLabel());
      assertEquals("3", g.my("transcript_recording_quality").getLabel());
      assertEquals(
        "Kitchen; Table in center of room with window on west wall, door to outside on north wall",
        g.my("transcript_room_layout").getLabel());
      assertEquals("tape74, side a, 104", g.my("transcript_tape_location").getLabel());

      // participants     
      assertEquals(2, g.all("who").length);
      assertEquals("Nony_mouse", g.getAnnotation("SUB").getLabel());
      assertEquals("who", g.getAnnotation("SUB").getLayerId());
      assertEquals("Investigator", g.getAnnotation("EXA").getLabel());
      assertEquals("who", g.getAnnotation("EXA").getLayerId());

      // participant meta data
      assertEquals("en", g.getAnnotation("SUB").first("language").getLabel());
      assertEquals("en", g.getAnnotation("EXA").first("language").getLabel());
      assertEquals("W", g.getAnnotation("SUB").first("corpus").getLabel());
      assertEquals("W", g.getAnnotation("EXA").first("corpus").getLabel());
      assertEquals("Participant", g.getAnnotation("SUB").first("role").getLabel());
      assertEquals("Investigator", g.getAnnotation("EXA").first("role").getLabel());

      // turns
      Annotation[] turns = g.all("turn");
      assertEquals("Number of turns correct", 3, turns.length);
      assertEquals("Turn 1 start time",
                   Double.valueOf(0.001), turns[0].getStart().getOffset());
      assertEquals("Turn 1 end time",
                   Double.valueOf(25.057), turns[0].getEnd().getOffset()); 
      assertEquals("Turn 1 speaker",
                   g.getAnnotation("SUB"), turns[0].getParent());
      assertEquals("Turn 2 start time",
                   Double.valueOf(25.057), turns[1].getStart().getOffset());
      assertEquals("Turn 2 end time",
                   Double.valueOf(29.994), turns[1].getEnd().getOffset());
      assertEquals("Turn 2 speaker",
                   g.getAnnotation("EXA"), turns[1].getParent());
      assertEquals("Turn 3 start time",
                   Double.valueOf(34.723), turns[2].getStart().getOffset());
      assertEquals("unaligned final utterance - turn has end time", 
		   Double.valueOf(681.935), turns[2].getEnd().getOffset());
      assertEquals("unaligned final utterance - turn has end time", 
		   Double.valueOf(681.935), turns[2].getEnd().getOffset());
      assertEquals("Turn 3 speaker",
                   g.getAnnotation("SUB"), turns[2].getParent());

      // utterances
      Annotation[] utterances = g.all("utterance");
      assertEquals(Double.valueOf(0.001), utterances[0].getStart().getOffset());
      assertEquals(Double.valueOf(21.510), utterances[0].getEnd().getOffset());
      assertEquals("SUB", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals("wrapped line", Double.valueOf(21.510), utterances[1].getStart().getOffset());
      assertEquals("simultaneos with next line", 
		   Double.valueOf(23.2835), utterances[1].getEnd().getOffset());
      assertEquals("simultaneos with next line", 
		   Integer.valueOf(Constants.CONFIDENCE_DEFAULT), 
		   utterances[1].getEnd().getConfidence());
      assertEquals("SUB", utterances[1].getParent().getLabel());

      assertEquals("simultaneous with previous line", 
		   Double.valueOf(23.2835), utterances[2].getStart().getOffset());
      assertEquals("simultaneous with previous line", 
		   Integer.valueOf(Constants.CONFIDENCE_DEFAULT), 
		   utterances[2].getStart().getConfidence());
      assertEquals("simultaneous line", Double.valueOf(25.057), utterances[2].getEnd().getOffset());
      assertEquals("SUB", utterances[2].getParent().getLabel());

      assertEquals(Double.valueOf(25.057), utterances[3].getStart().getOffset());
      assertEquals(Double.valueOf(29.994), utterances[3].getEnd().getOffset());

      assertEquals("linking utterance", "", utterances[5].getLabel());
      assertEquals("linking utterance",
                   Double.valueOf(35.752), utterances[5].getStart().getOffset());
      assertEquals("linking utterance",
                   Double.valueOf(40.481), utterances[5].getEnd().getOffset());

      assertEquals(Double.valueOf(40.481), utterances[6].getStart().getOffset());
      assertEquals(Double.valueOf(43.425), utterances[6].getEnd().getOffset());

      assertEquals("mid-line synchronisation - first utterance", 
		   Double.valueOf(414.937), utterances[141].getStart().getOffset());
      assertEquals("mid-line synchronisation - first utterance", 
		   Double.valueOf(418.673), utterances[141].getEnd().getOffset());
      assertEquals("mid-line synchronisation - linking utterance", 
		   Double.valueOf(418.673), utterances[142].getStart().getOffset());
      assertEquals("mid-line synchronisation - linking utterance", 
		   Double.valueOf(418.809), utterances[142].getEnd().getOffset());
      assertEquals("mid-line synchronisation - second utterance", 
		   Double.valueOf(418.809), utterances[143].getStart().getOffset());
      assertEquals("mid-line synchronisation - second utterance", 
		   Double.valueOf(420.631), utterances[143].getEnd().getOffset());

      assertEquals("overlapping utterances - first start unchanged", 
		   Double.valueOf(452.319), utterances[158].getStart().getOffset());
      assertEquals("overlapping utterances - first end unchanged", 
		   Double.valueOf(455.432), utterances[158].getEnd().getOffset());
      assertEquals("overlapping utterances - second start changed", 
		   Double.valueOf(455.432), utterances[159].getStart().getOffset());
      assertEquals("overlapping utterances - second end unchanged", 
		   Double.valueOf(460.584), utterances[159].getEnd().getOffset());

      assertEquals("unsynchronised utterance - before", 
		   Double.valueOf(489.467), utterances[169].getStart().getOffset());
      assertEquals("unsynchronised utterances - before - end moved", 
		   Double.valueOf(491.397), utterances[169].getEnd().getOffset());
      assertEquals("unsynchronised utterances - before - low confidence", 
		   Constants.CONFIDENCE_DEFAULT, utterances[169].getEnd().getConfidence().intValue());
      assertEquals("unsynchronised utterance - chained with utterance before", 
		   utterances[169].getEnd(), utterances[170].getStart());

      assertEquals("unsynchronised utterance - end original alignment", 
		   Double.valueOf(493.327), utterances[170].getEnd().getOffset());
      assertEquals("unsynchronised utterance - after", 
		   Double.valueOf(493.327), utterances[171].getStart().getOffset());
      assertEquals("unsynchronised utterances - after", 
		   Double.valueOf(496.813), utterances[171].getEnd().getOffset());

      assertEquals("unaligned final utterance has end time", 
		   Double.valueOf(681.935), utterances[utterances.length-1].getEnd().getOffset());
      assertEquals("aligned penultimate utterance has original start time", 
		   Double.valueOf(678.341), utterances[utterances.length-2].getStart().getOffset());
      assertEquals("aligned penultimate utterance links to unaligned ultimate utterance", 
		   utterances[utterances.length-2].getEnd(), 
		   utterances[utterances.length-1].getStart());
      assertEquals("aligned penultimate utterance has end time adjusted", 
		   Double.valueOf(681.935 + ((678.341-681.935)/2)), 
		   utterances[utterances.length-2].getEnd().getOffset());
      assertEquals("aligned penultimate utterance has end confidence adjusted", 
		   Constants.CONFIDENCE_DEFAULT, 
		   utterances[utterances.length-2].getEnd().getConfidence().intValue());
      Annotation[] words = g.all("turn")[0].all("word");
      String[] wordLabels = { // NB we have a c-unit layer, so terminators are stripped off 
	 "ab", "abc", "abcdef", "abcd", "gonna", "lie", "abcd", "abc", "pet", "abcdefghij", 
	 "abc", "abcde", "abc", "ab", "abcd", "ab", "abc", "worryin", "i", "abcd",
	 "she'll", "ab", "nd", "abcdefg"};
      for (int i = 0; i < wordLabels.length; i++) {
	 assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
      }
      for (int i = 0; i < words.length; i++) {
	 assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
	 	      i+1, words[i].getOrdinal());
      }

      words = g.all("turn")[2].all("word");
      assertEquals("they've", words[233].getLabel());
      assertEquals("work", words[234].getLabel());
      assertEquals("ab", words[235].getLabel());
      assertEquals("a", words[236].getLabel());
      assertEquals("hunger", words[237].getLabel());
      assertEquals("ab", words[238].getLabel());
      assertEquals("Unaligned last utterance doesn't cause @End to be last word",
		   "test", words[words.length-1].getLabel());

      assertEquals(turns[2].getId(), words[234].getParentId());
      assertEquals(turns[2].getId(), words[235].getParentId());
      assertEquals(turns[2].getId(), words[236].getParentId());
      assertEquals(turns[2].getId(), words[237].getParentId());
      assertEquals(turns[2].getId(), words[238].getParentId());
      assertEquals(turns[2].getId(), words[239].getParentId());
      assertEquals(turns[2].getId(), words[240].getParentId());

      for (int i = 0; i < words.length; i++) {	 
	 assertEquals("ordinals correct " + words[i], i+1, words[i].getOrdinal());
	 assertEquals("tagged as manual: " + words[i], 
		      Integer.valueOf(Constants.CONFIDENCE_MANUAL), words[i].getConfidence());
      }

      // c-units
      Annotation[] cUnits = g.all("c-unit");
      assertEquals(150, cUnits.length);
      assertEquals(".", cUnits[0].getLabel());
      assertEquals(utterances[0].getStart(), cUnits[0].getStart());
      assertEquals(utterances[1].getEnd(), cUnits[0].getEnd());
      assertEquals(utterances[2].getStart(), cUnits[1].getStart());
      assertEquals(utterances[2].getEnd(), cUnits[1].getEnd());
      assertEquals("+//?", cUnits[2].getLabel());
      assertEquals(utterances[3].getStart(), cUnits[2].getStart());
      assertEquals(utterances[3].getEnd(), cUnits[2].getEnd());

      assertEquals("unsynchronised utterance - c-unit", 
		   utterances[169].getStart(), cUnits[102].getStart());
      assertEquals("unsynchronised utterances c-unit", 
		   utterances[170].getEnd(), cUnits[102].getEnd());
      assertEquals("unsynchronised utterance - c-unit after", 
		   utterances[171].getStart(), cUnits[103].getStart());
      assertEquals("unsynchronised utterances - c-unit after", 
		   utterances[172].getEnd(), cUnits[103].getEnd());

      assertEquals("unsynchronised last utterance - last c-unit start", 
		   utterances[utterances.length-1].getStart(), cUnits[cUnits.length-1].getStart());
      assertEquals("unsynchronised last utterance - last c-unit end", 
		   utterances[utterances.length-1].getEnd(), cUnits[cUnits.length-1].getEnd());

      for (Annotation a : cUnits) {
	 assertEquals("tagged as manual: " + a + " " + a.getStart() + "-" + a.getEnd(), 
		      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
         if (a.getStart().getOffset() < 25.057) {
           assertEquals("parent set: " + a + " " + a.getStart() + "-" + a.getEnd(), 
                        turns[0], a.getParent());
         } else if (a.getStart().getOffset() < 29.994) {
           assertEquals("parent set: " + a + " " + a.getStart() + "-" + a.getEnd(), 
                        turns[1], a.getParent());
         } else {
           assertEquals("parent set: " + a + " " + a.getStart() + "-" + a.getEnd(), 
                        turns[2], a.getParent());
         }
      }

      // disfluency
      words = g.all("turn")[0].all("word");
      assertEquals("i", words[18].getLabel());
      assertEquals("&+", words[18].first("disfluency").getLabel());
      assertEquals("word is parent", words[18], words[18].first("disfluency").getParent());
      // ensure they're marked as manual annotations
      for (Annotation a : g.all("disfluency")) {
	 assertEquals("tagged as manual: " + a, 
		      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

      Annotation[] expansions = g.all("expansion");
      assertEquals(1, expansions.length);
      assertEquals("going to", expansions[0].getLabel());
      assertEquals("gonna", expansions[0].first("word").getLabel());
      assertEquals(expansions[0].first("word"), expansions[0].getParent());
      assertEquals(expansions[0].first("word"), words[4]);
      assertEquals("Pre expansion ordinal", "gonna", words[4].getLabel());
      assertEquals("Pre expansion ordinal", 5, words[4].getOrdinal());
      assertEquals("Post expansion ordinal", "lie", words[5].getLabel());
      assertEquals("Post expansion ordinal", 6, words[5].getOrdinal());
      for (Annotation a : expansions) {
	 assertEquals("tagged as manual: " + a, 
		      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

      // linkages
      Annotation[] linkages = g.all("linkage");
      assertEquals(1, linkages.length);
      assertEquals("a_b_c_d", linkages[0].getLabel());

      // errors
      Annotation[] errors = g.all("error");
      assertEquals(8, errors.length);

      // tag marked span
      assertEquals("s:r", errors[0].getLabel());
      assertEquals("starts with span", "work", 
		   errors[0].getStart().startOf("word").iterator().next().getLabel());
      assertEquals("ends with span", "hunger", 
		   errors[0].getEnd().endOf("word").iterator().next().getLabel());

      // tag word
      assertEquals("m", errors[1].getLabel());
      assertEquals("starts with word", "got", 
		   errors[1].getStart().startOf("word").iterator().next().getLabel());
      assertEquals("ends with word", "got", 
		   errors[1].getEnd().endOf("word").iterator().next().getLabel());

      // tag marked word
      assertEquals("f", errors[2].getLabel());
      assertEquals("starts with span", "a", 
		   errors[2].getStart().startOf("word").iterator().next().getLabel());
      assertEquals("ends with span", "a", 
		   errors[2].getEnd().endOf("word").iterator().next().getLabel());
      for (Annotation a : errors)
      {
	 assertEquals("tagged as manual: " + a, 
		      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

      // completion
      Annotation[] completions = g.all("completion");
      assertEquals(3, completions.length);
      for (Annotation a : completions) {
	 assertEquals("tagged as manual: " + a, 
		      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

      assertEquals("leading completion", "nd", words[22].getLabel());
      assertEquals("leading completion", "and", words[22].first("completion").getLabel());
      assertEquals("completion - word is parent", words[22], words[22].first("completion").getParent());
      assertEquals("trailing completion", "worryin", words[17].getLabel());
      assertEquals("trailing completion", "worrying", words[17].first("completion").getLabel());

      words = g.all("turn")[2].all("word");
      assertEquals("leading/trailing completion", "fridge", words[243].getLabel());
      assertEquals("leading/trailing completion", "refridgerator", words[243].first("completion").getLabel());

      // retracing
      Annotation[] retracing = g.all("retracing");
      assertEquals(6, retracing.length);
      for (Annotation a : retracing) {
	 assertEquals("tagged as manual: " + a, 
		      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

      // tag previous word
      assertEquals("//", retracing[0].getLabel());
      assertEquals("tag previous word", "sit", 
		   retracing[0].getStart().startOf("word").iterator().next().getLabel());
      assertEquals("tag previous word", "sit", 
		   retracing[0].getEnd().endOf("word").iterator().next().getLabel());

      // tag marked span
      assertEquals("//", retracing[1].getLabel());
      assertEquals("starts with span", "aten", 
		   retracing[1].getStart().startOf("word").iterator().next().getLabel());
      assertEquals("ends with span", "too", 
		   retracing[1].getEnd().endOf("word").iterator().next().getLabel());

      // tag marked word
      assertEquals("//", retracing[3].getLabel());
      assertEquals("starts with span", "Circus", 
		   retracing[3].getStart().startOf("word").iterator().next().getLabel());
      assertEquals("ends with span", "Circus", 
		   retracing[3].getEnd().endOf("word").iterator().next().getLabel());

      // repetition
      Annotation[] repetition = g.all("repetition");
      assertEquals(3, repetition.length);

      // tag marked word
      assertEquals("/", repetition[0].getLabel());
      assertEquals("starts with span", "picnic", 
		   repetition[0].getStart().startOf("word").iterator().next().getLabel());
      assertEquals("ends with span", "picnic", 
		   repetition[0].getEnd().endOf("word").iterator().next().getLabel());

      // tag marked span
      assertEquals("/", repetition[1].getLabel());
      assertEquals("starts with span", "spent", 
		   repetition[1].getStart().startOf("word").iterator().next().getLabel());
      assertEquals("ends with span", "morning", 
		   repetition[1].getEnd().endOf("word").iterator().next().getLabel());

      // tag previous word
      assertEquals("/", repetition[2].getLabel());
      assertEquals("tag previous word", "Saturday", 
		   repetition[2].getStart().startOf("word").iterator().next().getLabel());
      assertEquals("tag previous word", "Saturday", 
		   repetition[2].getEnd().endOf("word").iterator().next().getLabel());

      for (Annotation a : repetition) {
	 assertEquals("tagged as manual: " + a, 
		      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

      // gems
      Annotation[] gems = g.all("gem");
      assertEquals(11, gems.length);
      assertEquals(Double.valueOf(0.001), gems[0].getStart().getOffset());
      assertEquals("gdc", gems[0].getLabel());
      assertEquals(Double.valueOf(180.068), gems[0].getEnd().getOffset());
      assertEquals(Double.valueOf(180.068), gems[1].getStart().getOffset());
      assertEquals("picnic", gems[1].getLabel());
      assertEquals(Double.valueOf(347.077), gems[1].getEnd().getOffset());
      assertEquals(Double.valueOf(347.077), gems[2].getStart().getOffset());
      assertEquals("christmas", gems[2].getLabel());
      assertEquals(Double.valueOf(403.531), gems[2].getEnd().getOffset());
      assertEquals(Double.valueOf(403.531), gems[3].getStart().getOffset());
      assertEquals("weekend", gems[3].getLabel());
      assertEquals(Double.valueOf(455.432), gems[3].getEnd().getOffset());
      assertEquals("Shifted to end of previous utterance", 
		   Double.valueOf(455.432), gems[4].getStart().getOffset());
      assertEquals("vacation", gems[4].getLabel());
      assertEquals(Double.valueOf(502.431), gems[4].getEnd().getOffset());
      assertEquals(Double.valueOf(507.270), gems[5].getStart().getOffset());
      assertEquals("peanut", gems[5].getLabel());
      assertEquals(Double.valueOf(536.950), gems[5].getEnd().getOffset());
      assertEquals("Shifted to end of previous utterance", 
		   Double.valueOf(536.950), gems[6].getStart().getOffset());
      assertEquals("flower", gems[6].getLabel());
      assertEquals(Double.valueOf(562.364), gems[6].getEnd().getOffset());
      assertEquals(Double.valueOf(562.364), gems[7].getStart().getOffset());
      assertEquals("birthday", gems[7].getLabel());
      assertEquals(Double.valueOf(605.459), gems[7].getEnd().getOffset());
      assertEquals(Double.valueOf(605.552), gems[8].getStart().getOffset());
      assertEquals("directions", gems[8].getLabel());
      assertEquals(Double.valueOf(630.083), gems[8].getEnd().getOffset());
      assertEquals("Shifted to end of previous utterance", 
		   Double.valueOf(630.083), gems[9].getStart().getOffset());
      assertEquals("argument", gems[9].getLabel());
      assertEquals(Double.valueOf(657.253), gems[9].getEnd().getOffset());
      assertEquals(Double.valueOf(657.253), gems[10].getStart().getOffset());
      assertEquals("cat", gems[10].getLabel());
      assertEquals(turns[2].getEnd(), gems[10].getEnd());
      for (Annotation a : gems) {
	 assertEquals("tagged as manual: " + a, 
		      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }
      
      // check all annotations have 'manual' confidence
      for (Annotation a : g.getAnnotationsById().values()) {
         assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

   }

   @Test public void minimalConversionWithoutAnnotations()  throws Exception {
      Layer[] layers = {
	 new Layer("transcriber", "Transcribers", 0, true, true, true),
	 new Layer("languages", "Graph language", 0, true, true, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("language", "Speaker language", 0, false, false, true, "who", true),
	 new Layer("corpus", "Speaker corpus", 0, false, false, true, "who", true),
	 new Layer("role", "Speaker role", 0, false, false, true, "who", true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true),
      };
      Schema schema = new Schema(layers, "who", "turn", "utterance", "word");
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "test.cha")) };

      // create deserializer
      ChatSerialization deserializer = new ChatSerialization();

      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(39, deserializer.configure(configuration, schema).size());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      //for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }
      
      assertEquals("test.cha", g.getId());
      String[] transcribers = g.labels("transcriber"); 
      assertEquals(2, transcribers.length);
      assertEquals("Alan Turing", transcribers[0]);
      assertEquals("Oscar Wilde", transcribers[1]);
      String[] languages = g.labels("languages"); 
      assertEquals(1, languages.length);
      assertEquals("en", languages[0]);

      // participants     
      assertEquals(2, g.all("who").length);
      assertEquals("Nony_mouse", g.getAnnotation("SUB").getLabel());
      assertEquals("who", g.getAnnotation("SUB").getLayerId());
      assertEquals("Investigator", g.getAnnotation("EXA").getLabel());
      assertEquals("who", g.getAnnotation("EXA").getLayerId());

      // participant meta data
      assertEquals("en", g.getAnnotation("SUB").first("language").getLabel());
      assertEquals("en", g.getAnnotation("EXA").first("language").getLabel());
      assertEquals("W", g.getAnnotation("SUB").first("corpus").getLabel());
      assertEquals("W", g.getAnnotation("EXA").first("corpus").getLabel());
      assertEquals("Participant", g.getAnnotation("SUB").first("role").getLabel());
      assertEquals("Investigator", g.getAnnotation("EXA").first("role").getLabel());

      // turns
      Annotation[] turns = g.all("turn");
      assertEquals(3, turns.length);
      assertEquals(Double.valueOf(0.001), turns[0].getStart().getOffset());
      assertEquals("unaligned final utterance - turn has end time", 
		   Double.valueOf(681.935), turns[2].getEnd().getOffset());
      assertEquals(g.getAnnotation("SUB"), turns[0].getParent());
      assertEquals(g.getAnnotation("EXA"), turns[1].getParent());
      assertEquals(g.getAnnotation("SUB"), turns[2].getParent());

      // utterances
      Annotation[] utterances = g.all("utterance");
      assertEquals(Double.valueOf(0.001), utterances[0].getStart().getOffset());
      assertEquals(Double.valueOf(21.510), utterances[0].getEnd().getOffset());
      assertEquals("SUB", utterances[0].getParent().getLabel());
      assertEquals(turns[0], utterances[0].getParent());

      assertEquals("wrapped line", Double.valueOf(21.510), utterances[1].getStart().getOffset());
      assertEquals("simultaneos with next line", 
		   Double.valueOf(23.2835), utterances[1].getEnd().getOffset());
      assertEquals("simultaneos with next line", 
		   Integer.valueOf(Constants.CONFIDENCE_DEFAULT), 
		   utterances[1].getEnd().getConfidence());
      assertEquals("SUB", utterances[1].getParent().getLabel());

      assertEquals("simultaneous with previous line", 
		   Double.valueOf(23.2835), utterances[2].getStart().getOffset());
      assertEquals("simultaneous with previous line", 
		   Integer.valueOf(Constants.CONFIDENCE_DEFAULT), 
		   utterances[2].getStart().getConfidence());
      assertEquals("simultaneous line", Double.valueOf(25.057), utterances[2].getEnd().getOffset());
      assertEquals("SUB", utterances[2].getParent().getLabel());

      assertEquals(Double.valueOf(25.057), utterances[3].getStart().getOffset());
      assertEquals(Double.valueOf(29.994), utterances[3].getEnd().getOffset());

      assertEquals("linking utterance", "", utterances[5].getLabel());
      assertEquals("linking utterance",
                   Double.valueOf(35.752), utterances[5].getStart().getOffset());
      assertEquals("linking utterance",
                   Double.valueOf(40.481), utterances[5].getEnd().getOffset());

      assertEquals(Double.valueOf(40.481), utterances[6].getStart().getOffset());
      assertEquals(Double.valueOf(43.425), utterances[6].getEnd().getOffset());

      assertEquals("mid-line synchronisation - first utterance", 
		   Double.valueOf(414.937), utterances[141].getStart().getOffset());
      assertEquals("mid-line synchronisation - first utterance", 
		   Double.valueOf(418.673), utterances[141].getEnd().getOffset());
      assertEquals("mid-line synchronisation - linking utterance", 
		   Double.valueOf(418.673), utterances[142].getStart().getOffset());
      assertEquals("mid-line synchronisation - linking utterance", 
		   Double.valueOf(418.809), utterances[142].getEnd().getOffset());
      assertEquals("mid-line synchronisation - second utterance", 
		   Double.valueOf(418.809), utterances[143].getStart().getOffset());
      assertEquals("mid-line synchronisation - second utterance", 
		   Double.valueOf(420.631), utterances[143].getEnd().getOffset());

      assertEquals("overlapping utterances - first start unchanged", 
		   Double.valueOf(452.319), utterances[158].getStart().getOffset());
      assertEquals("overlapping utterances - first end unchanged", 
		   Double.valueOf(455.432), utterances[158].getEnd().getOffset());
      assertEquals("overlapping utterances - second start changed", 
		   Double.valueOf(455.432), utterances[159].getStart().getOffset());
      assertEquals("overlapping utterances - second end unchanged", 
		   Double.valueOf(460.584), utterances[159].getEnd().getOffset());

      assertEquals("unaligned final utterance has end time", 
		   Double.valueOf(681.935), utterances[utterances.length-1].getEnd().getOffset());
      assertEquals("aligned penultimate utterance has original start time", 
		   Double.valueOf(678.341), utterances[utterances.length-2].getStart().getOffset());
      assertEquals("aligned penultimate utterance links to unaligned ultimate utterance", 
		   utterances[utterances.length-2].getEnd(), 
		   utterances[utterances.length-1].getStart());
      assertEquals("aligned penultimate utterance has end time adjusted", 
		   Double.valueOf(681.935 + ((678.341-681.935)/2)), 
		   utterances[utterances.length-2].getEnd().getOffset());
      assertEquals("aligned penultimate utterance has end confidence adjusted", 
		   Constants.CONFIDENCE_DEFAULT, 
		   utterances[utterances.length-2].getEnd().getConfidence().intValue());
      
      Annotation[] words = g.all("word");
      String[] wordLabels = { 
	 // NB we have no c-unit layer, so terminators are still present
	 // NB we have no linkage layer, so linkages are not split
	 "ab", "abc", "abcdef", "abcd", "gonna", "lie", "abcd", "abc", "pet", "abcdefghij", 
	 "abc", "abcde", "abc", "ab", "abcd", "ab", "abc", "worryin", "i", "abcd.",
	 "she'll", "ab", "nd", "abcdefg.", 
	 "abcd", "abcdefg", "ab", "abc", "abcdef", "abcde", "abc", "ab", "abc", "abcdefgh", "abc", "abcdef", "abc", "+//?",
	 "ab", "abcde", "until", "she's", "abc", "ab", "abcde", "abcdef", "abcde", "ab", "abc", "baby's", "crib", 
	 "abc", "abcdefg", "abc", "abcd", "abcde", "abc", "abcd", "abc", "a_b_c_d."};
      for (int i = 0; i < wordLabels.length; i++) {
	 assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
      }

      // disfluency
      assertEquals("i", words[18].getLabel());
      assertNull("disfluency not tagged", words[18].first("disfluency"));
      assertEquals("no disfluencies", 0, g.all("disfluency").length);

      // expansions
      assertEquals("no expansions", 0, g.all("expansion").length);
      assertEquals("Pre expansion ordinal", "gonna", words[4].getLabel());
      assertEquals("Pre expansion ordinal", 5, words[4].getOrdinal());
      assertEquals("Post expansion ordinal", "lie", words[5].getLabel());
      assertEquals("Post expansion ordinal", 6, words[5].getOrdinal());

      // errors
      assertEquals("errors not tagged", 0, g.all("error").length);

      assertEquals("they've", words[268].getLabel());
      assertEquals("work", words[269].getLabel());
      assertEquals("ab", words[270].getLabel());
      assertEquals("a", words[271].getLabel());
      assertEquals("hunger", words[272].getLabel());
      assertEquals(".", words[273].getLabel());
      assertEquals("ab", words[274].getLabel());

      assertEquals(turns[2].getId(), words[267].getParentId());
      assertEquals(turns[2].getId(), words[268].getParentId());
      assertEquals(turns[2].getId(), words[269].getParentId());
      assertEquals(turns[2].getId(), words[270].getParentId());
      assertEquals(turns[2].getId(), words[271].getParentId());
      assertEquals(turns[2].getId(), words[272].getParentId());
      assertEquals(turns[2].getId(), words[273].getParentId());

      // completion
      assertEquals("no completions", 0, g.all("completion").length);

      // retracing
      assertEquals("no retracing", 0, g.all("retracing").length);

      // repetition
      assertEquals("no repetitions", 0, g.all("repetition").length);

      // gems
      assertEquals("no gems", 0, g.all("gem").length);
      
      // check all annotations have 'manual' confidence
      for (Annotation a : g.getAnnotationsById().values()) {
         assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }
   }

  /* Basic MOR tag parsing. <em>NB</em> the griffin-mor.cha test file is output from MOR that has
   * been manually edited to create test cases that will cover cases not naturally
   * present in this test data. */
   @Test public void deserializeMor()  throws Exception {
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("transcript_date", "Recording date")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_location", "Location")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_recording_quality", "Recording quality")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_room_layout", "Room layout")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_tape_location", "Tape location")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcriber", "Transcribers", 0, true, true, true),
	 new Layer("languages", "Graph language", 0, true, true, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("language", "Speaker language", 0, false, false, true, "who", true),
	 new Layer("corpus", "Speaker corpus", 0, false, false, true, "who", true),
	 new Layer("role", "Speaker role", 0, false, false, true, "who", true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("c-unit", "C-Units", 2, true, false, false, "turn", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("linkage", "Linkages", 2, true, false, false, "turn", true),
	 new Layer("error", "Errors", 2, true, false, false, "turn", true),
	 new Layer("retracing", "Retracing", 2, true, false, false, "turn", true),
	 new Layer("repetition", "Repetitions", 2, true, false, false, "turn", true),
	 new Layer("pause", "Unfilled pauses", 2, true, false, false, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true),
	 new Layer("completion", "Completion", 0, true, false, false, "word", true),
	 new Layer("expansion", "Expansion", 0, false, false, true, "word", true),
	 new Layer("disfluency", "Disfluency", 0, false, false, true, "word", true),
	 new Layer("mor", "%mor tags")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(true).setSaturated(true)
         .setParentId("word").setParentIncludes(true),
	 new Layer("gem", "Gems", 2, true, false, true));
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "griffin-mor.cha")) };
      
      // create deserializer
      ChatSerialization deserializer = new ChatSerialization();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals("MOR layer", "mor",
                   ((Layer)configuration.get("morLayer").getValue()).getId());
      assertEquals("Pause layer", "pause",
                   ((Layer)configuration.get("pauseLayer").getValue()).getId());
      assertEquals("Split MOR tag groups by default", Boolean.TRUE,
                   configuration.get("splitMorTagGroups").getValue());
      assertEquals("Split MOR word groups by default", Boolean.TRUE,
                   configuration.get("splitMorWordGroups").getValue());
      configuration.get("splitMorTagGroups").setValue(Boolean.FALSE);
      assertEquals(39, deserializer.configure(configuration, schema).size());
      assertEquals("Don't split MOR tag groups",
                   Boolean.FALSE, deserializer.getSplitMorTagGroups());
      
      
      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      for (String warning : deserializer.getWarnings()) System.out.println(warning);
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }

      // meta data
      assertEquals("griffin-mor.cha", g.getId());
      String[] transcribers = g.labels("transcriber"); 
      assertEquals(1, transcribers.length);
      assertEquals("SH", transcribers[0]);
      
      String[] languages = g.labels("languages"); 
      assertEquals(1, languages.length);
      assertEquals("ISO639 alpha3 is converted to alpha2", "en", languages[0]);
      
      // participants     
      assertEquals(5, g.all("who").length);
      assertEquals("Nick_Griffin", g.getAnnotation("GRI").getLabel());
      assertEquals("who", g.getAnnotation("GRI").getLayerId());
      assertEquals("Dimbleby", g.getAnnotation("DIM").getLabel());
      assertEquals("who", g.getAnnotation("DIM").getLayerId());
      assertEquals("Pause", g.getAnnotation("PPP").getLabel());
      assertEquals("who", g.getAnnotation("PPP").getLayerId());
      assertEquals("Applause", g.getAnnotation("APP").getLabel());
      assertEquals("who", g.getAnnotation("APP").getLayerId());
      assertEquals("Unknown", g.getAnnotation("UNK").getLabel());
      assertEquals("who", g.getAnnotation("UNK").getLayerId());

      // participant meta data
      assertEquals("ISO639 alpha3 is converted to alpha2",
                   "en", g.getAnnotation("GRI").first("language").getLabel());
      assertEquals("ISO639 alpha3 is converted to alpha2",
                   "en", g.getAnnotation("DIM").first("language").getLabel());
      assertEquals("ISO639 alpha3 is converted to alpha2",
                   "en", g.getAnnotation("PPP").first("language").getLabel());
      assertEquals("ISO639 alpha3 is converted to alpha2",
                   "en", g.getAnnotation("APP").first("language").getLabel());
      assertEquals("ISO639 alpha3 is converted to alpha2",
                   "en", g.getAnnotation("UNK").first("language").getLabel());
      assertEquals("change_corpus_later", g.getAnnotation("GRI").first("corpus").getLabel());
      assertEquals("change_corpus_later", g.getAnnotation("DIM").first("corpus").getLabel());
      assertEquals("change_corpus_later", g.getAnnotation("PPP").first("corpus").getLabel());
      assertEquals("change_corpus_later", g.getAnnotation("APP").first("corpus").getLabel());
      assertEquals("change_corpus_later", g.getAnnotation("UNK").first("corpus").getLabel());
      assertEquals("Participant", g.getAnnotation("GRI").first("role").getLabel());
      assertEquals("Participant", g.getAnnotation("DIM").first("role").getLabel());
      assertEquals("Unidentified", g.getAnnotation("PPP").first("role").getLabel());
      assertEquals("Unidentified", g.getAnnotation("APP").first("role").getLabel());
      assertEquals("Unidentified", g.getAnnotation("UNK").first("role").getLabel());

      Annotation[] gems = g.all("gem");
      assertEquals(1, gems.length);
      assertEquals("Gem label", "denial", gems[0].getLabel());
      assertEquals("Gen start", Double.valueOf(9.929), gems[0].getStart().getOffset());
      assertEquals("Gen end", Double.valueOf(18.053), gems[0].getEnd().getOffset());

      // turns
      Annotation[] turns = g.all("turn");
      assertEquals("Number of turns correct", 32, turns.length);

      // utterances
      Annotation[] utterances = g.all("utterance");
      assertEquals("Number of utterances correct", 44, utterances.length);
      Annotation[] words = g.all("turn")[1].all("word");
      String[] wordLabels = {
	"without", "a", "shadow", "of", "a", "doubt", "i", "appreciate",
	"that", "if", "you", "look", "at", "some", "of", "the", "things", "i'm", "quoted",
	"as", "having", "said↗",
	"in", "the", "daily", "mail", "n", "dai-", "an'", "so", "on",
	"i'd", "be", "a", "↑monster↘" };
      for (int i = 0; i < wordLabels.length; i++) {
	 assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
      }
      for (int i = 0; i < words.length; i++) {
	 assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
	 	      i+1, words[i].getOrdinal());
      }

      // morphosyntactic tags
      Annotation[] mor = g.all("turn")[1].all("mor");
      String[] morLabels = {
	"prep|+conj|with+prep|out=sin", "det:art|a=una", "n|shadow^v|shadow=sombra", "prep|of=de", "det:art|a",
        "n|doubt^v|doubt", "n:let|i", "v|appreciate",
        "comp|that^pro:rel|that^pro:dem|that^det:dem|that", "comp|if^conj|if", "pro:per|you",
        "cop|look^co|look^n|look^v|look", "prep|at", "qn|some^pro:indef|some", "prep|of",
        "det:art|the", "n|thing-PL", "n:let|i$aux|be&1S^n:let|i~cop|be&1S",
        "part|quote-PASTP^v|quote-PAST",
	"adv|as^conj|as^prep|as", "aux|have-PRESP^n:gerund|have-PRESP^part|have-PRESP",
        "part|say&PASTP^v|say&PAST",
	"prep|in^adv|in", "det:art|the", "adv:tem|day&dn-LY^adj|daily", "n|mail^v|mail",
        "n:let|n", "?|dai–", "?|an'", "co|so^adv|so^conj|so", "prep|on^adv|on",
	"n:let|i~mod|genmod", "cop|be^aux|be", "det:art|a", "n|monster" };
      for (int i = 0; i < wordLabels.length; i++) {
	 assertEquals("mor labels " + i, morLabels[i], mor[i].getLabel());
	 assertEquals("mor words " + i, wordLabels[i], mor[i].getParent().getLabel());
      }
      
      // pauses
      Annotation[] pauses = g.all("pause");
      String[] pauseLabels = {
	"3.1", ".", "0.3", "0.2", "0.2", "0.6", "0.3", "0.2", "0.7", "0.3", "0.3", "0.2", "0.5",
	"0.2", "0.2", "3.0", "6.0", "1.3", "0.4", "1.4", "0.8", "0.7", "0.4" };
      assertEquals("Number of pauses correct", pauseLabels.length, pauses.length);
      for (int i = 0; i < pauseLabels.length; i++) {
	 assertEquals("pause labels " + i, pauseLabels[i], pauses[i].getLabel());
      }

      // check all annotations have 'manual' confidence
      for (Annotation a : g.getAnnotationsById().values()) {
         assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

   }
  
  /* Ignoring MOR tags and pauses; they're still parsed out, but no annotations are
   * created. <em>NB</em> the griffin-mor.cha test file is output from MOR that has 
   * been manually edited to create test cases that will cover cases not naturally
   * present in this test data. */
   @Test public void canIgnorePausesAndMor()  throws Exception {
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("transcript_date", "Recording date")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_location", "Location")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_recording_quality", "Recording quality")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_room_layout", "Room layout")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_tape_location", "Tape location")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcriber", "Transcribers", 0, true, true, true),
	 new Layer("languages", "Graph language", 0, true, true, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("language", "Speaker language", 0, false, false, true, "who", true),
	 new Layer("corpus", "Speaker corpus", 0, false, false, true, "who", true),
	 new Layer("role", "Speaker role", 0, false, false, true, "who", true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("c-unit", "C-Units", 2, true, false, false, "turn", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("linkage", "Linkages", 2, true, false, false, "turn", true),
	 new Layer("error", "Errors", 2, true, false, false, "turn", true),
	 new Layer("retracing", "Retracing", 2, true, false, false, "turn", true),
	 new Layer("repetition", "Repetitions", 2, true, false, false, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true),
	 new Layer("completion", "Completion", 0, true, false, false, "word", true),
	 new Layer("expansion", "Expansion", 0, false, false, true, "word", true),
	 new Layer("disfluency", "Disfluency", 0, false, false, true, "word", true),
	 new Layer("gem", "Gems", 2, true, false, true));
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "griffin-mor.cha")) };
      
      // create deserializer
      ChatSerialization deserializer = new ChatSerialization();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(39, deserializer.configure(configuration, schema).size());
      assertNull("No MOR layer", 
                 configuration.get("morLayer").getValue());
      assertNull("No pause layer", 
                 configuration.get("pauseLayer").getValue());
      
      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      for (String warning : deserializer.getWarnings()) System.out.println(warning);
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }

      // meta data
      assertEquals("griffin-mor.cha", g.getId());
      String[] transcribers = g.labels("transcriber"); 
      assertEquals(1, transcribers.length);
      assertEquals("SH", transcribers[0]);
      
      String[] languages = g.labels("languages"); 
      assertEquals(1, languages.length);
      assertEquals("ISO639 alpha3 is converted to alpha2", "en", languages[0]);
      
      // participants     
      assertEquals(5, g.all("who").length);
      assertEquals("Nick_Griffin", g.getAnnotation("GRI").getLabel());
      assertEquals("who", g.getAnnotation("GRI").getLayerId());
      assertEquals("Dimbleby", g.getAnnotation("DIM").getLabel());
      assertEquals("who", g.getAnnotation("DIM").getLayerId());
      assertEquals("Pause", g.getAnnotation("PPP").getLabel());
      assertEquals("who", g.getAnnotation("PPP").getLayerId());
      assertEquals("Applause", g.getAnnotation("APP").getLabel());
      assertEquals("who", g.getAnnotation("APP").getLayerId());
      assertEquals("Unknown", g.getAnnotation("UNK").getLabel());
      assertEquals("who", g.getAnnotation("UNK").getLayerId());

      // participant meta data
      assertEquals("ISO639 alpha3 is converted to alpha2",
                   "en", g.getAnnotation("GRI").first("language").getLabel());
      assertEquals("ISO639 alpha3 is converted to alpha2",
                   "en", g.getAnnotation("DIM").first("language").getLabel());
      assertEquals("ISO639 alpha3 is converted to alpha2",
                   "en", g.getAnnotation("PPP").first("language").getLabel());
      assertEquals("ISO639 alpha3 is converted to alpha2",
                   "en", g.getAnnotation("APP").first("language").getLabel());
      assertEquals("ISO639 alpha3 is converted to alpha2",
                   "en", g.getAnnotation("UNK").first("language").getLabel());
      assertEquals("change_corpus_later", g.getAnnotation("GRI").first("corpus").getLabel());
      assertEquals("change_corpus_later", g.getAnnotation("DIM").first("corpus").getLabel());
      assertEquals("change_corpus_later", g.getAnnotation("PPP").first("corpus").getLabel());
      assertEquals("change_corpus_later", g.getAnnotation("APP").first("corpus").getLabel());
      assertEquals("change_corpus_later", g.getAnnotation("UNK").first("corpus").getLabel());
      assertEquals("Participant", g.getAnnotation("GRI").first("role").getLabel());
      assertEquals("Participant", g.getAnnotation("DIM").first("role").getLabel());
      assertEquals("Unidentified", g.getAnnotation("PPP").first("role").getLabel());
      assertEquals("Unidentified", g.getAnnotation("APP").first("role").getLabel());
      assertEquals("Unidentified", g.getAnnotation("UNK").first("role").getLabel());

      Annotation[] gems = g.all("gem");
      assertEquals(1, gems.length);
      assertEquals("Gem label", "denial", gems[0].getLabel());
      assertEquals("Gen start", Double.valueOf(9.929), gems[0].getStart().getOffset());
      assertEquals("Gen end", Double.valueOf(18.053), gems[0].getEnd().getOffset());

      // turns
      Annotation[] turns = g.all("turn");
      assertEquals("Number of turns correct", 32, turns.length);

      // utterances
      Annotation[] utterances = g.all("utterance");
      assertEquals("Number of utterances correct", 44, utterances.length);
      Annotation[] words = g.all("turn")[1].all("word");
      String[] wordLabels = {
	"without", "a", "shadow", "of", "a", "doubt", "i", "appreciate",
	"that", "if", "you", "look", "at", "some", "of", "the", "things", "i'm", "quoted",
	"as", "having", "said↗",
	"in", "the", "daily", "mail", "n", "dai-", "an'", "so", "on",
	"i'd", "be", "a", "↑monster↘" };
      for (int i = 0; i < wordLabels.length; i++) {
	 assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
      }
      for (int i = 0; i < words.length; i++) {
	 assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
	 	      i+1, words[i].getOrdinal());
      }

      // check all annotations have 'manual' confidence
      for (Annotation a : g.getAnnotationsById().values()) {
         assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }
   }

  /* MOR tag parsing with tag groups (alternative analyses) split, but not word groups
   * (sub-word components). <em>NB</em> the griffin-mor.cha test file is output from MOR
   * that has been manually edited to create test cases that will cover cases not
   * naturally present in this test data. */
   @Test public void splitMorTagGroupsNotWordGroups()  throws Exception {
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("transcript_date", "Recording date")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_location", "Location")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_recording_quality", "Recording quality")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_room_layout", "Room layout")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_tape_location", "Tape location")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcriber", "Transcribers", 0, true, true, true),
	 new Layer("languages", "Graph language", 0, true, true, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("language", "Speaker language", 0, false, false, true, "who", true),
	 new Layer("corpus", "Speaker corpus", 0, false, false, true, "who", true),
	 new Layer("role", "Speaker role", 0, false, false, true, "who", true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("c-unit", "C-Units", 2, true, false, false, "turn", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("linkage", "Linkages", 2, true, false, false, "turn", true),
	 new Layer("error", "Errors", 2, true, false, false, "turn", true),
	 new Layer("retracing", "Retracing", 2, true, false, false, "turn", true),
	 new Layer("repetition", "Repetitions", 2, true, false, false, "turn", true),
	 new Layer("pause", "Unfilled pauses", 2, true, false, false, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true),
	 new Layer("completion", "Completion", 0, true, false, false, "word", true),
	 new Layer("expansion", "Expansion", 0, false, false, true, "word", true),
	 new Layer("disfluency", "Disfluency", 0, false, false, true, "word", true),
	 new Layer("mor", "%mor tags")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(true).setPeersOverlap(true).setSaturated(true)
         .setParentId("word").setParentIncludes(true),
	 new Layer("gem", "Gems", 2, true, false, true));
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "griffin-mor.cha")) };
      
      // create deserializer
      ChatSerialization deserializer = new ChatSerialization();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals("MOR layer", "mor",
                   ((Layer)configuration.get("morLayer").getValue()).getId());
      assertEquals("Pause layer", "pause",
                   ((Layer)configuration.get("pauseLayer").getValue()).getId());
      assertEquals("Split MOR tag groups by default", Boolean.TRUE,
                   configuration.get("splitMorTagGroups").getValue());
      assertEquals("Split MOR word groups by default", Boolean.TRUE,
                   configuration.get("splitMorWordGroups").getValue());
      configuration.get("splitMorWordGroups").setValue(Boolean.FALSE);
      assertEquals(39, deserializer.configure(configuration, schema).size());
      assertEquals("Don't split MOR word groups",
                   Boolean.FALSE, deserializer.getSplitMorWordGroups());
      
      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      for (String warning : deserializer.getWarnings()) System.out.println(warning);
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }

      // meta data
      assertEquals("griffin-mor.cha", g.getId());
      String[] transcribers = g.labels("transcriber"); 
      assertEquals(1, transcribers.length);
      assertEquals("SH", transcribers[0]);
      
      String[] languages = g.labels("languages"); 
      assertEquals(1, languages.length);
      assertEquals("ISO639 alpha3 is converted to alpha2", "en", languages[0]);
      
      // participants     
      assertEquals(5, g.all("who").length);
      assertEquals("Nick_Griffin", g.getAnnotation("GRI").getLabel());
      assertEquals("who", g.getAnnotation("GRI").getLayerId());
      assertEquals("Dimbleby", g.getAnnotation("DIM").getLabel());
      assertEquals("who", g.getAnnotation("DIM").getLayerId());
      assertEquals("Pause", g.getAnnotation("PPP").getLabel());
      assertEquals("who", g.getAnnotation("PPP").getLayerId());
      assertEquals("Applause", g.getAnnotation("APP").getLabel());
      assertEquals("who", g.getAnnotation("APP").getLayerId());
      assertEquals("Unknown", g.getAnnotation("UNK").getLabel());
      assertEquals("who", g.getAnnotation("UNK").getLayerId());

      // participant meta data
      assertEquals("ISO639 alpha3 is converted to alpha2",
                   "en", g.getAnnotation("GRI").first("language").getLabel());
      assertEquals("ISO639 alpha3 is converted to alpha2",
                   "en", g.getAnnotation("DIM").first("language").getLabel());
      assertEquals("ISO639 alpha3 is converted to alpha2",
                   "en", g.getAnnotation("PPP").first("language").getLabel());
      assertEquals("ISO639 alpha3 is converted to alpha2",
                   "en", g.getAnnotation("APP").first("language").getLabel());
      assertEquals("ISO639 alpha3 is converted to alpha2",
                   "en", g.getAnnotation("UNK").first("language").getLabel());
      assertEquals("change_corpus_later", g.getAnnotation("GRI").first("corpus").getLabel());
      assertEquals("change_corpus_later", g.getAnnotation("DIM").first("corpus").getLabel());
      assertEquals("change_corpus_later", g.getAnnotation("PPP").first("corpus").getLabel());
      assertEquals("change_corpus_later", g.getAnnotation("APP").first("corpus").getLabel());
      assertEquals("change_corpus_later", g.getAnnotation("UNK").first("corpus").getLabel());
      assertEquals("Participant", g.getAnnotation("GRI").first("role").getLabel());
      assertEquals("Participant", g.getAnnotation("DIM").first("role").getLabel());
      assertEquals("Unidentified", g.getAnnotation("PPP").first("role").getLabel());
      assertEquals("Unidentified", g.getAnnotation("APP").first("role").getLabel());
      assertEquals("Unidentified", g.getAnnotation("UNK").first("role").getLabel());

      Annotation[] gems = g.all("gem");
      assertEquals(1, gems.length);
      assertEquals("Gem label", "denial", gems[0].getLabel());
      assertEquals("Gen start", Double.valueOf(9.929), gems[0].getStart().getOffset());
      assertEquals("Gen end", Double.valueOf(18.053), gems[0].getEnd().getOffset());

      // turns
      Annotation[] turns = g.all("turn");
      assertEquals("Number of turns correct", 32, turns.length);

      // utterances
      Annotation[] utterances = g.all("utterance");
      assertEquals("Number of utterances correct", 44, utterances.length);
      Annotation[] words = g.all("turn")[1].all("word");
      String[] wordLabels = {
	"without", "a", "shadow", "of", "a", "doubt", "i", "appreciate",
	"that", "if", "you", "look", "at", "some", "of", "the", "things", "i'm", "quoted",
	"as", "having", "said↗",
	"in", "the", "daily", "mail", "n", "dai-", "an'", "so", "on",
	"i'd", "be", "a", "↑monster↘" };
      for (int i = 0; i < wordLabels.length; i++) {
	 assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
      }
      for (int i = 0; i < words.length; i++) {
	 assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
	 	      i+1, words[i].getOrdinal());
      }

      // morphosyntactic tags
      Annotation[] mor = g.all("turn")[1].all("mor");
      String[] morLabels = {
	"prep|+conj|with+prep|out=sin", "det:art|a=una", "n|shadow","v|shadow=sombra", "prep|of=de", "det:art|a",
        "n|doubt","v|doubt", "n:let|i", "v|appreciate",
        "comp|that","pro:rel|that","pro:dem|that","det:dem|that",
        "comp|if","conj|if", "pro:per|you",
        "cop|look","co|look","n|look","v|look", "prep|at", "qn|some","pro:indef|some", "prep|of",
        "det:art|the", "n|thing-PL", "n:let|i$aux|be&1S","n:let|i~cop|be&1S",
        "part|quote-PASTP","v|quote-PAST",
	"adv|as","conj|as","prep|as", "aux|have-PRESP","n:gerund|have-PRESP","part|have-PRESP",
        "part|say&PASTP","v|say&PAST",
	"prep|in","adv|in", "det:art|the", "adv:tem|day&dn-LY","adj|daily", "n|mail","v|mail",
        "n:let|n", "?|dai–", "?|an'", "co|so","adv|so","conj|so", "prep|on","adv|on",
	"n:let|i~mod|genmod", "cop|be","aux|be", "det:art|a", "n|monster" };
      String[] morWords = {
	"without", "a", "shadow","shadow", "of", "a",
        "doubt","doubt", "i", "appreciate",
        "that","that","that","that",
        "if","if", "you",
        "look","look","look","look", "at", "some","some", "of",
        "the", "things", "i'm","i'm",
        "quoted","quoted",
	"as","as","as", "having","having","having",
        "said↗","said↗",
	"in","in", "the", "daily","daily", "mail","mail",
        "n", "dai-", "an'", "so","so","so", "on","on",
	"i'd", "be","be", "a", "↑monster↘" };
      for (int i = 0; i < morLabels.length; i++) {
	 assertEquals("mor labels " + i, morLabels[i], mor[i].getLabel());
	 assertEquals("mor word " + i, morWords[i], mor[i].getParent().getLabel());
      }
      
      // pauses
      Annotation[] pauses = g.all("pause");
      String[] pauseLabels = {
	"3.1", ".", "0.3", "0.2", "0.2", "0.6", "0.3", "0.2", "0.7", "0.3", "0.3", "0.2", "0.5",
	"0.2", "0.2", "3.0", "6.0", "1.3", "0.4", "1.4", "0.8", "0.7", "0.4" };
      assertEquals("Number of pauses correct", pauseLabels.length, pauses.length);
      for (int i = 0; i < pauseLabels.length; i++) {
	 assertEquals("pause labels " + i, pauseLabels[i], pauses[i].getLabel());
      }

      // check all annotations have 'manual' confidence
      for (Annotation a : g.getAnnotationsById().values()) {
         assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

   }
  
  /* MOR tag parsing where all components are parsed and generate separate annotations. 
   * <em>NB</em> the griffin-mor.cha test file is output from MOR that has been manually
   * edited to create test cases that will cover cases not naturally present in this test
   * data. */ 
   @Test public void splitMorWordGroups()  throws Exception {
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("transcript_date", "Recording date")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_location", "Location")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_recording_quality", "Recording quality")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_room_layout", "Room layout")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_tape_location", "Tape location")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcriber", "Transcribers", 0, true, true, true),
	 new Layer("languages", "Graph language", 0, true, true, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("language", "Speaker language", 0, false, false, true, "who", true),
	 new Layer("corpus", "Speaker corpus", 0, false, false, true, "who", true),
	 new Layer("role", "Speaker role", 0, false, false, true, "who", true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("c-unit", "C-Units", 2, true, false, false, "turn", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("linkage", "Linkages", 2, true, false, false, "turn", true),
	 new Layer("error", "Errors", 2, true, false, false, "turn", true),
	 new Layer("retracing", "Retracing", 2, true, false, false, "turn", true),
	 new Layer("repetition", "Repetitions", 2, true, false, false, "turn", true),
	 new Layer("pause", "Unfilled pauses", 2, true, false, false, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true),
	 new Layer("completion", "Completion", 0, true, false, false, "word", true),
	 new Layer("expansion", "Expansion", 0, false, false, true, "word", true),
	 new Layer("disfluency", "Disfluency", 0, false, false, true, "word", true),
	 new Layer("mor", "%mor tags")
         .setAlignment(Constants.ALIGNMENT_INTERVAL) // because sub-words are sequential
         .setPeers(true).setPeersOverlap(true).setSaturated(false)
         .setParentId("word").setParentIncludes(true),
	 new Layer("morPrefix", "MOR Prefixes")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(true).setSaturated(false)
         .setParentId("word").setParentIncludes(true),
	 new Layer("pos", "MOR Part of Speech labels")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(true).setSaturated(false)
         .setParentId("word").setParentIncludes(true),
	 new Layer("morPOSSubcategory", "MOR Part of Speech Subcategories")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(true).setSaturated(false)
         .setParentId("word").setParentIncludes(true),
	 new Layer("stem", "MOR Stem")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(true).setSaturated(false)
         .setParentId("word").setParentIncludes(true),
	 new Layer("morFusionalSuffix", "MOR Fusional Suffixes")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(true).setSaturated(false)
         .setParentId("word").setParentIncludes(true),
	 new Layer("morSuffix", "MOR Suffixes")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(true).setSaturated(false)
         .setParentId("word").setParentIncludes(true),
	 new Layer("gloss", "MOR English Glosses")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(true).setSaturated(false)
         .setParentId("word").setParentIncludes(true),
	 new Layer("gem", "Gems", 2, true, false, true));
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "griffin-mor.cha")) };
      
      // create deserializer
      ChatSerialization deserializer = new ChatSerialization();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals("MOR layer", "mor",
                   ((Layer)configuration.get("morLayer").getValue()).getId());
      assertEquals("Pause layer", "pause",
                   ((Layer)configuration.get("pauseLayer").getValue()).getId());
      assertEquals("Split MOR tag groups by default", Boolean.TRUE,
                   configuration.get("splitMorTagGroups").getValue());
      assertEquals("Split MOR word groups by default", Boolean.TRUE,
                   configuration.get("splitMorWordGroups").getValue());
      assertEquals("Prefix layer", "morPrefix",
                   ((Layer)configuration.get("morPrefixLayer").getValue()).getId());
      assertEquals("POS layer", "pos",
                   ((Layer)configuration.get("morPartOfSpeechLayer").getValue()).getId());
      assertEquals("Subcategory layer", "morPOSSubcategory",
                   ((Layer)configuration.get("morPartOfSpeechSubcategoryLayer").getValue()).getId());
      assertEquals("Stem layer", "stem",
                   ((Layer)configuration.get("morStemLayer").getValue()).getId());
      assertEquals("Fusional Suffix layer", "morFusionalSuffix",
                   ((Layer)configuration.get("morFusionalSuffixLayer").getValue()).getId());
      assertEquals("Suffix layer", "morSuffix",
                   ((Layer)configuration.get("morSuffixLayer").getValue()).getId());
      assertEquals("Gloss layer", "gloss",
                   ((Layer)configuration.get("morGlossLayer").getValue()).getId());
      assertEquals(39, deserializer.configure(configuration, schema).size());
      assertNotNull("Suffix layer set", deserializer.getMorSuffixLayer());
      
      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      for (String warning : deserializer.getWarnings()) System.out.println(warning);
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }

      // check morphosyntactic tag labels
      Annotation[] mor = g.all("turn")[1].all("mor");
      Annotation[] pos = g.all("turn")[1].all("pos");
      assertEquals("There's a POS for every MOR tag", mor.length, pos.length);
      String[] morLabels = {
	"prep|","conj|with","prep|out=sin", "det:art|a=una", "n|shadow","v|shadow=sombra", "prep|of=de", "det:art|a",
        "n|doubt","v|doubt", "n:let|i", "v|appreciate",
        "comp|that","pro:rel|that","pro:dem|that","det:dem|that",
        "comp|if","conj|if", "pro:per|you",
        "cop|look","co|look","n|look","v|look", "prep|at", "qn|some","pro:indef|some", "prep|of",
        "det:art|the", "n|thing-PL", "n:let|i", "aux|be&1S","n:let|i","cop|be&1S",
        "part|quote-PASTP","v|quote-PAST",
	"adv|as","conj|as","prep|as", "aux|have-PRESP","n:gerund|have-PRESP","part|have-PRESP",
        "part|say&PASTP","v|say&PAST",
	"prep|in","adv|in", "det:art|the", "adv:tem|day&dn-LY","adj|daily", "n|mail","v|mail",
        "n:let|n", "?|dai–", "?|an'", "co|so","adv|so","conj|so", "prep|on","adv|on",
	"n:let|i","mod|genmod", "cop|be","aux|be", "det:art|a", "n|monster" };
      String[] posLabels = {
	"prep","conj","prep", "det", "n","v", "prep", "det",
        "n","v", "n", "v",
        "comp","pro","pro","det",
        "comp","conj", "pro",
        "cop","co","n","v", "prep", "qn","pro", "prep",
        "det", "n", "n", "aux","n","cop",
        "part","v",
	"adv","conj","prep", "aux","n","part",
        "part","v",
	"prep","adv", "det", "adv","adj", "n","v",
        "n", "?", "?", "co","adv","conj", "prep","adv",
	"n","mod", "cop","aux", "det", "n" };
      String[] morWords = {
	"without","without","without", "a", "shadow","shadow", "of", "a",
        "doubt","doubt", "i", "appreciate",
        "that","that","that","that",
        "if","if", "you",
        "look","look","look","look", "at", "some","some", "of",
        "the", "things", "i'm","i'm","i'm","i'm",
        "quoted","quoted",
	"as","as","as", "having","having","having",
        "said↗","said↗",
	"in","in", "the", "daily","daily", "mail","mail",
        "n", "dai-", "an'", "so","so","so", "on","on",
	"i'd","i'd", "be","be", "a", "↑monster↘" };
      String[] alignedWithStart = {
	"without",null,null, "a", "shadow","shadow", "of", "a",
        "doubt","doubt", "i", "appreciate",
        "that","that","that","that",
        "if","if", "you",
        "look","look","look","look", "at", "some","some", "of",
        "the", "things", "i'm",null,"i'm",null,
        "quoted","quoted",
	"as","as","as", "having","having","having",
        "said↗","said↗",
	"in","in", "the", "daily","daily", "mail","mail",
        "n", "dai-", "an'", "so","so","so", "on","on",
	"i'd",null, "be","be", "a", "↑monster↘" };
      String[] alignedWithEnd = {
	null,null,"without", "a", "shadow","shadow", "of", "a",
        "doubt","doubt", "i", "appreciate",
        "that","that","that","that",
        "if","if", "you",
        "look","look","look","look", "at", "some","some", "of",
        "the", "things", null,"i'm",null,"i'm",
        "quoted","quoted",
	"as","as","as", "having","having","having",
        "said↗","said↗",
	"in","in", "the", "daily","daily", "mail","mail",
        "n", "dai-", "an'", "so","so","so", "on","on",
	null,"i'd", "be","be", "a", "↑monster↘" };
      for (int i = 0; i < morLabels.length; i++) {
	 assertEquals("mor labels " + i, morLabels[i], mor[i].getLabel());
	 assertEquals("mor word " + i, morWords[i], mor[i].getParent().getLabel());
         if (alignedWithStart[i] == null) {
           // check start doesn't align with a word
           assertEquals("start "+i+" not aligned with word: " + mor[i].getStart().startOf("word"),
                        0, mor[i].getStart().startOf("word").size());
         } else {
           // check start aligns with given word
           assertNotEquals("start "+i+" aligned with word: " + mor[i].getStart().startOf("word"),
                           0, mor[i].getStart().startOf("word").size());
           assertEquals("start "+i+" aligned with correct word: "+mor[i].getStart().startOf("word"),
                        alignedWithStart[i],
                        mor[i].getStart().startOf("word").iterator().next().getLabel());
         }
         if (alignedWithEnd[i] == null) {
           // check end doesn't align with a word
           assertEquals("end "+i+" not aligned with word: " + mor[i].getEnd().endOf("word"),
                        0, mor[i].getEnd().endOf("word").size());
         } else {
           // check word aligns with given word
           assertNotEquals("end "+i+" aligned with word: " + mor[i].getEnd().endOf("word"),
                           0, mor[i].getEnd().endOf("word").size());
           assertEquals("end "+i+" aligned with correct word: " + mor[i].getEnd().endOf("word"),
                       alignedWithEnd[i],
                       mor[i].getEnd().endOf("word").iterator().next().getLabel());
         }
	 assertEquals("pos labels " + i, posLabels[i], pos[i].getLabel());
	 assertEquals("pos word " + i, morWords[i], pos[i].getParent().getLabel());
      }

      // check stem labels
      Annotation[] stem = g.all("turn")[1].all("stem");      
      String[] stemLabels = {
	"with","out", "a", "shadow","shadow", "of", "a",
        "doubt","doubt", "i", "appreciate",
        "that","that","that","that",
        "if","if", "you",
        "look","look","look","look", "at", "some","some", "of",
        "the", "thing", "i", "be","i","be",
        "quote","quote",
	"as","as","as", "have","have","have",
        "say","say",
	"in","in", "the", "day","daily", "mail","mail",
        "n", "dai–", "an'", "so","so","so", "on","on",
	"i","genmod", "be","be", "a", "monster" };
      for (int i = 0; i < stemLabels.length; i++) {
	 assertEquals("stem label " + i, stemLabels[i], stem[i].getLabel());
      }
      
      // check POS subcategory labels
      Annotation[] subcategory = g.all("turn")[1].all("morPOSSubcategory");      
      String[] subcategoryLabels = {
	"art", "art",
        "let",
        "rel","dem","dem",
        "per",
        "indef",
        "art", "let", "let",
	"gerund",
	"art", "tem",
        "let", 
	"let","art" };
      for (int i = 0; i < subcategoryLabels.length; i++) {
	 assertEquals("POS subcategory label " + i,
                      subcategoryLabels[i], subcategory[i].getLabel());
      }
      
      // check fusional suffix labels
      Annotation[] fusionalSuffix = g.all("turn")[1].all("morFusionalSuffix");      
      String[] fusionalSuffixLabels = {
	"1S","1S",
        "PASTP","PAST",
	"dn" };
      for (int i = 0; i < fusionalSuffixLabels.length; i++) {
	 assertEquals("Fusional suffix label " + i,
                      fusionalSuffixLabels[i], fusionalSuffix[i].getLabel());
      }
      // check POS suffix labels
      Annotation[] suffix = g.all("turn")[1].all("morSuffix");      
      String[] suffixLabels = {
	"PL", "PASTP","PAST",
	"PRESP","PRESP","PRESP",
	"LY" };
      for (int i = 0; i < suffixLabels.length; i++) {
	 assertEquals("Suffix label " + i, suffixLabels[i], suffix[i].getLabel());
      }

      // check gloss labels
      Annotation[] gloss = g.all("turn")[1].all("gloss");      
      String[] glossLabels = {
	"sin", "una","sombra", "de" };
      for (int i = 0; i < glossLabels.length; i++) {
	 assertEquals("Gloss label " + i, glossLabels[i], gloss[i].getLabel());
      }
   }
  
  /* MOR tag parsing where all components are parsed and generate separate annotations,
   * but there's no layer for raw MOR labels. 
   * <em>NB</em> the griffin-mor.cha test file is output from MOR that has been manually
   * edited to create test cases that will cover cases not naturally present in this test
   * data. */ 
   @Test public void morePartsWithoutRawMorLayer()  throws Exception {
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("transcript_date", "Recording date")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_location", "Location")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_recording_quality", "Recording quality")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_room_layout", "Room layout")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_tape_location", "Tape location")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcriber", "Transcribers", 0, true, true, true),
	 new Layer("languages", "Graph language", 0, true, true, true),
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("language", "Speaker language", 0, false, false, true, "who", true),
	 new Layer("corpus", "Speaker corpus", 0, false, false, true, "who", true),
	 new Layer("role", "Speaker role", 0, false, false, true, "who", true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("c-unit", "C-Units", 2, true, false, false, "turn", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
	 new Layer("linkage", "Linkages", 2, true, false, false, "turn", true),
	 new Layer("error", "Errors", 2, true, false, false, "turn", true),
	 new Layer("retracing", "Retracing", 2, true, false, false, "turn", true),
	 new Layer("repetition", "Repetitions", 2, true, false, false, "turn", true),
	 new Layer("pause", "Unfilled pauses", 2, true, false, false, "turn", true),
	 new Layer("word", "Words", 2, true, false, false, "turn", true),
	 new Layer("completion", "Completion", 0, true, false, false, "word", true),
	 new Layer("expansion", "Expansion", 0, false, false, true, "word", true),
	 new Layer("disfluency", "Disfluency", 0, false, false, true, "word", true),
	 new Layer("morPrefix", "MOR Prefixes")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(true).setSaturated(false)
         .setParentId("word").setParentIncludes(true),
	 new Layer("pos", "MOR Part of Speech labels")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(true).setSaturated(false)
         .setParentId("word").setParentIncludes(true),
	 new Layer("morPOSSubcategory", "MOR Part of Speech Subcategories")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(true).setSaturated(false)
         .setParentId("word").setParentIncludes(true),
	 new Layer("stem", "MOR Stem")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(true).setSaturated(false)
         .setParentId("word").setParentIncludes(true),
	 new Layer("morFusionalSuffix", "MOR Fusional Suffixes")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(true).setSaturated(false)
         .setParentId("word").setParentIncludes(true),
	 new Layer("morSuffix", "MOR Suffixes")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(true).setSaturated(false)
         .setParentId("word").setParentIncludes(true),
	 new Layer("gloss", "MOR English Glosses")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(true).setSaturated(false)
         .setParentId("word").setParentIncludes(true),
	 new Layer("gem", "Gems", 2, true, false, true));
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "griffin-mor.cha")) };
      
      // create deserializer
      ChatSerialization deserializer = new ChatSerialization();
      
      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertNull("No MOR layer",
                 configuration.get("morLayer").getValue());
      assertEquals("Pause layer", "pause",
                   ((Layer)configuration.get("pauseLayer").getValue()).getId());
      assertEquals("Split MOR tag groups by default", Boolean.TRUE,
                   configuration.get("splitMorTagGroups").getValue());
      assertEquals("Split MOR word groups by default", Boolean.TRUE,
                   configuration.get("splitMorWordGroups").getValue());
      assertEquals("Prefix layer", "morPrefix",
                   ((Layer)configuration.get("morPrefixLayer").getValue()).getId());
      assertEquals("POS layer", "pos",
                   ((Layer)configuration.get("morPartOfSpeechLayer").getValue()).getId());
      assertEquals("Subcategory layer", "morPOSSubcategory",
                   ((Layer)configuration.get("morPartOfSpeechSubcategoryLayer").getValue()).getId());
      assertEquals("Stem layer", "stem",
                   ((Layer)configuration.get("morStemLayer").getValue()).getId());
      assertEquals("Fusional Suffix layer", "morFusionalSuffix",
                   ((Layer)configuration.get("morFusionalSuffixLayer").getValue()).getId());
      assertEquals("Suffix layer", "morSuffix",
                   ((Layer)configuration.get("morSuffixLayer").getValue()).getId());
      assertEquals("Gloss layer", "gloss",
                   ((Layer)configuration.get("morGlossLayer").getValue()).getId());
      assertEquals(39, deserializer.configure(configuration, schema).size());
      assertNotNull("Suffix layer set", deserializer.getMorSuffixLayer());
      
      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      for (String warning : deserializer.getWarnings()) System.out.println(warning);
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }

      // check pos labels
      Annotation[] pos = g.all("turn")[1].all("pos");
      String[] posLabels = {
	"prep","conj","prep", "det", "n","v", "prep", "det",
        "n","v", "n", "v",
        "comp","pro","pro","det",
        "comp","conj", "pro",
        "cop","co","n","v", "prep", "qn","pro", "prep",
        "det", "n", "n", "aux","n","cop",
        "part","v",
	"adv","conj","prep", "aux","n","part",
        "part","v",
	"prep","adv", "det", "adv","adj", "n","v",
        "n", "?", "?", "co","adv","conj", "prep","adv",
	"n","mod", "cop","aux", "det", "n" };
      String[] morWords = {
	"without","without","without", "a", "shadow","shadow", "of", "a",
        "doubt","doubt", "i", "appreciate",
        "that","that","that","that",
        "if","if", "you",
        "look","look","look","look", "at", "some","some", "of",
        "the", "things", "i'm","i'm","i'm","i'm",
        "quoted","quoted",
	"as","as","as", "having","having","having",
        "said↗","said↗",
	"in","in", "the", "daily","daily", "mail","mail",
        "n", "dai-", "an'", "so","so","so", "on","on",
	"i'd","i'd", "be","be", "a", "↑monster↘" };
      String[] alignedWithStart = {
	"without",null,null, "a", "shadow","shadow", "of", "a",
        "doubt","doubt", "i", "appreciate",
        "that","that","that","that",
        "if","if", "you",
        "look","look","look","look", "at", "some","some", "of",
        "the", "things", "i'm",null,"i'm",null,
        "quoted","quoted",
	"as","as","as", "having","having","having",
        "said↗","said↗",
	"in","in", "the", "daily","daily", "mail","mail",
        "n", "dai-", "an'", "so","so","so", "on","on",
	"i'd",null, "be","be", "a", "↑monster↘" };
      String[] alignedWithEnd = {
	null,null,"without", "a", "shadow","shadow", "of", "a",
        "doubt","doubt", "i", "appreciate",
        "that","that","that","that",
        "if","if", "you",
        "look","look","look","look", "at", "some","some", "of",
        "the", "things", null,"i'm",null,"i'm",
        "quoted","quoted",
	"as","as","as", "having","having","having",
        "said↗","said↗",
	"in","in", "the", "daily","daily", "mail","mail",
        "n", "dai-", "an'", "so","so","so", "on","on",
	null,"i'd", "be","be", "a", "↑monster↘" };
      for (int i = 0; i < posLabels.length; i++) {
	 assertEquals("pos labels " + i, posLabels[i], pos[i].getLabel());
	 assertEquals("mor word " + i, morWords[i], pos[i].getParent().getLabel());
         if (alignedWithStart[i] == null) {
           // check start doesn't align with a word
           assertEquals("start "+i+" not aligned with word: " + pos[i].getStart().startOf("word"),
                        0, pos[i].getStart().startOf("word").size());
         } else {
           // check start aligns with given word
           assertNotEquals("start "+i+" aligned with word: " + pos[i].getStart().startOf("word"),
                           0, pos[i].getStart().startOf("word").size());
           assertEquals("start "+i+" aligned with correct word: "+pos[i].getStart().startOf("word"),
                        alignedWithStart[i],
                        pos[i].getStart().startOf("word").iterator().next().getLabel());
         }
         if (alignedWithEnd[i] == null) {
           // check end doesn't align with a word
           assertEquals("end "+i+" not aligned with word: " + pos[i].getEnd().endOf("word"),
                        0, pos[i].getEnd().endOf("word").size());
         } else {
           // check word aligns with given word
           assertNotEquals("end "+i+" aligned with word: " + pos[i].getEnd().endOf("word"),
                           0, pos[i].getEnd().endOf("word").size());
           assertEquals("end "+i+" aligned with correct word: " + pos[i].getEnd().endOf("word"),
                       alignedWithEnd[i],
                       pos[i].getEnd().endOf("word").iterator().next().getLabel());
         }
      }

      // check stem labels
      Annotation[] stem = g.all("turn")[1].all("stem");      
      String[] stemLabels = {
	"with","out", "a", "shadow","shadow", "of", "a",
        "doubt","doubt", "i", "appreciate",
        "that","that","that","that",
        "if","if", "you",
        "look","look","look","look", "at", "some","some", "of",
        "the", "thing", "i", "be","i","be",
        "quote","quote",
	"as","as","as", "have","have","have",
        "say","say",
	"in","in", "the", "day","daily", "mail","mail",
        "n", "dai–", "an'", "so","so","so", "on","on",
	"i","genmod", "be","be", "a", "monster" };
      for (int i = 0; i < stemLabels.length; i++) {
	 assertEquals("stem label " + i, stemLabels[i], stem[i].getLabel());
      }
      
      // check POS subcategory labels
      Annotation[] subcategory = g.all("turn")[1].all("morPOSSubcategory");      
      String[] subcategoryLabels = {
	"art", "art",
        "let",
        "rel","dem","dem",
        "per",
        "indef",
        "art", "let", "let",
	"gerund",
	"art", "tem",
        "let", 
	"let","art" };
      for (int i = 0; i < subcategoryLabels.length; i++) {
	 assertEquals("POS subcategory label " + i,
                      subcategoryLabels[i], subcategory[i].getLabel());
      }
      
      // check fusional suffix labels
      Annotation[] fusionalSuffix = g.all("turn")[1].all("morFusionalSuffix");      
      String[] fusionalSuffixLabels = {
	"1S","1S",
        "PASTP","PAST",
	"dn" };
      for (int i = 0; i < fusionalSuffixLabels.length; i++) {
	 assertEquals("Fusional suffix label " + i,
                      fusionalSuffixLabels[i], fusionalSuffix[i].getLabel());
      }
      // check POS suffix labels
      Annotation[] suffix = g.all("turn")[1].all("morSuffix");      
      String[] suffixLabels = {
	"PL", "PASTP","PAST",
	"PRESP","PRESP","PRESP",
	"LY" };
      for (int i = 0; i < suffixLabels.length; i++) {
	 assertEquals("Suffix label " + i, suffixLabels[i], suffix[i].getLabel());
      }

      // check gloss labels
      Annotation[] gloss = g.all("turn")[1].all("gloss");      
      String[] glossLabels = {
	"sin", "una","sombra", "de" };
      for (int i = 0; i < glossLabels.length; i++) {
	 assertEquals("Gloss label " + i, glossLabels[i], gloss[i].getLabel());
      }
   }
  
   @Test public void serialize() throws Exception {
      Schema schema = new Schema(
         "who", "turn", "utterance", "word",
	 new Layer("scribe", "Transcriber")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(true).setPeersOverlap(true).setSaturated(true),
         new Layer("transcript_language", "Language")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
         new Layer("noise", "Non-speech noises")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false),
         new Layer("who", "Participants")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(true).setPeersOverlap(true).setSaturated(true),
         new Layer("main_participant", "Main speaker")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true)
         .setParentId("who").setParentIncludes(true),
         new Layer("participant_gender", "Gender")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true)
         .setParentId("who").setParentIncludes(true),
         new Layer("participant_age", "Age")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true)
         .setParentId("who").setParentIncludes(true),
         new Layer("participant_language", "Language")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true)
         .setParentId("who").setParentIncludes(true),
         new Layer("turn", "Speaker turns")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false)
         .setParentId("who").setParentIncludes(true),
         new Layer("utterance", "Utterances")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(true)
         .setParentId("turn").setParentIncludes(true),
         new Layer("word", "Words")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false)
         .setParentId("turn").setParentIncludes(true));
      
      File dir = getDir();
      Graph graph = new Graph()
         .setId("serialize-test.txt")
         .setSchema(schema);
      graph.addAnchor(new Anchor("a0", 0.0));
      graph.addAnchor(new Anchor("a5", 5.4321)); // will be rendered 5432
      graph.addAnchor(new Anchor("a10", 10.0));
      graph.addAnchor(new Anchor("a15", 15.0));
      // language
      graph.addAnnotation(new Annotation("lang", "en", "transcript_language", "a0", "a15"));
      // participants
      graph.addAnnotation(new Annotation("child", "John Smith", "who", "a0", "a15"));
      graph.addAnnotation(new Annotation("mother", "Mrs. Smith", "who", "a0", "a15"));
      graph.addAnnotation(new Annotation("child-main", "John Smith", "main_participant", "a0", "a15",
                                         "child"));
      graph.addAnnotation(new Annotation("child-age", "2;10.10", "participant_age", "a0", "a15",
                                         "child"));
      graph.addAnnotation(new Annotation("child-gender", "M", "participant_gender", "a0", "a15",
                                         "child"));
      graph.addAnnotation(new Annotation("child-language", "en", "participant_language", "a0", "a15",
                                         "child"));
      graph.addAnnotation(new Annotation("mother-language", "Spanish", "participant_language", "a0", "a15",
                                         "mother"));
      // turns
      graph.addAnnotation(new Annotation("t1", "John Smith", "turn", "a0", "a10", "child"));
      graph.addAnnotation(new Annotation("t2", "Mrs. Smith", "turn", "a10", "a15", "mother"));
      // utterances
      graph.addAnnotation(new Annotation("u1", "John Smith", "utterance", "a0", "a5", "t1"));
      graph.addAnnotation(new Annotation("u2", "John Smith", "utterance", "a5", "a10", "t1"));
      graph.addAnnotation(new Annotation("u3", "Mrs. Smith", "utterance", "a10", "a15", "t2"));
      
      // words
      graph.addAnnotation(new Annotation("the", "The", "word",
                                         "a0",
                                         // 1.2345 will become ..._1234
                                         graph.addAnchor(new Anchor("a1", 1.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("quick", "'quick", "word", 
                                         "a1",
                                         graph.addAnchor(new Anchor("a2", 2.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("brown", "brown'", "word", 
                                         "a2",
                                         graph.addAnchor(new Anchor("a3", 3.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("fox", "fox", "word", 
                                         "a3",
                                         "a5",
                                         "t1"));
      
      graph.addAnnotation(new Annotation("jumps", "jumps -", "word", 
                                         "a5",
                                         graph.addAnchor(new Anchor("a6", 6.0)).getId(),
                                         "t1"));      
      graph.addAnnotation(new Annotation("over", "over", "word",
                                         "a6",
                                         graph.addAnchor(new Anchor("a7", 8.0)).getId(),
                                         "t1"));
      // noise
      graph.addAnnotation(new Annotation("cough", "coughs", "noise",
                                         "a7",
                                         graph.addAnchor(new Anchor("a8", 8.0)).getId(),
                                         "t1"));
      
      graph.addAnnotation(new Annotation("th~", "th~", "word", // th~ becomes &th
                                         "a8",
                                         "a10",
                                         "t1"));
      
      graph.addAnnotation(new Annotation("the2", "the", "word", 
                                         "a10",
                                         graph.addAnchor(new Anchor("a12", 12.0)).getId(),
                                         "t2"));
      graph.addAnnotation(new Annotation("lazy", "lazy", "word", 
                                         "a12",
                                         graph.addAnchor(new Anchor("a13", 13.0)).getId(),
                                         "t2"));
      graph.addAnnotation(new Annotation("dog", "\"dog\"", "word", 
                                         "a13",
                                         graph.addAnchor(new Anchor("a14", 14.0)).getId(),
                                         "t2"));
      graph.addAnnotation(new Annotation(".", ".", "word", 
                                         "a14",
                                         "a15",
                                         "t2"));
      
      // create serializer
      ChatSerialization serializer = new ChatSerialization();
      
      // general configuration
      ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
      //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      configuration = serializer.configure(configuration, schema);
      assertEquals(39, configuration.size());
      assertEquals("scribe attribute", "scribe", 
		   ((Layer)configuration.get("transcriberLayer").getValue()).getId());
      assertEquals("languages attribute", "transcript_language", 
		   ((Layer)configuration.get("languagesLayer").getValue()).getId());
      assertEquals("target participant attribute", "main_participant", 
		   ((Layer)configuration.get("targetParticipantLayer").getValue()).getId());
      assertEquals("non-word layer", "noise", 
		   ((Layer)configuration.get("nonWordLayer").getValue()).getId());
      assertEquals("sex attribute mapped to gender", "participant_gender",
                   ((Layer)configuration.get("sexLayer").getValue()).getId());
      assertEquals("age attribute", "participant_age",
                   ((Layer)configuration.get("ageLayer").getValue()).getId());
      assertEquals("includeTimeCodes", Boolean.TRUE, 
		   configuration.get("includeTimeCodes").getValue());
      assertNull("date attribute", 
                 configuration.get("dateLayer").getValue());
      assertNull("recording quality attribute",
                 configuration.get("recordingQualityLayer").getValue());
      assertNull("room layout attribute", 
                 configuration.get("roomLayoutLayer").getValue());
      assertNull("tape location attribute",
                 configuration.get("tapeLocationLayer").getValue());
      assertNull("No mor layer", 
                 configuration.get("morLayer").getValue());
      assertNull("No pause layer", 
                 configuration.get("pauseLayer").getValue());
      assertEquals("Split MOR tag groups by default", Boolean.TRUE,
                   configuration.get("splitMorTagGroups").getValue());
      
      LinkedHashSet<String> needLayers = new LinkedHashSet<String>(
         Arrays.asList(serializer.getRequiredLayers()));
      assertEquals("Needed layers: " + needLayers,
                   11, needLayers.size());
      assertTrue(needLayers.contains("who"));
      assertTrue(needLayers.contains("main_participant"));
      assertTrue(needLayers.contains("scribe"));
      assertTrue(needLayers.contains("transcript_language"));
      assertTrue(needLayers.contains("turn"));
      assertTrue(needLayers.contains("utterance"));
      assertTrue(needLayers.contains("word"));
      assertTrue(needLayers.contains("noise"));
      assertTrue(needLayers.contains("participant_age"));
      assertTrue(needLayers.contains("participant_gender"));
      assertTrue(needLayers.contains("participant_language"));
      
      // serialize
      final Vector<SerializationException> exceptions = new Vector<SerializationException>();
      final Vector<NamedStream> streams = new Vector<NamedStream>();
      String[] layers = {"word","transcript_language"};
      Graph[] graphs = { graph };
      serializer.serialize(Arrays.spliterator(graphs), layers,
                           stream -> streams.add(stream),
                           warning -> System.out.println(warning),
                           exception -> exceptions.add(exception));
      if (exceptions.size() > 0) {
         fail(exceptions.stream()
              .map(x -> {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    x.printStackTrace(pw);
                    return x.toString() + ": " + sw;
                 })
              .collect(Collectors.joining("\n","","")));
      }
      
      streams.elementAt(0).save(dir);
      
      // test using diff
      File result = new File(dir, "serialize-test.cha");
      String differences = diff(new File(dir, "expected_serialize-test.cha"), result);
      if (differences != null) {
         fail(differences);
      } else {
         result.delete();
      }
   }

   @Test public void serializeWithoutSynchronization() throws Exception {
      Schema schema = new Schema(
         "who", "turn", "utterance", "word",
	 new Layer("scribe", "Transcriber")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(true).setPeersOverlap(true).setSaturated(true),
	 new Layer("transcript_date", "Recording date")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_location", "Location")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_recording_quality", "Recording quality")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_room_layout", "Room layout")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
	 new Layer("transcript_tape_location", "Tape location")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
         new Layer("transcript_language", "Language")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true),
         new Layer("noise", "Non-speech noises")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false),
         new Layer("who", "Participants")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(true).setPeersOverlap(true).setSaturated(true),
         new Layer("main_participant", "Main speaker")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true)
         .setParentId("who").setParentIncludes(true),
         new Layer("participant_gender", "Gender")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true)
         .setParentId("who").setParentIncludes(true),
         new Layer("participant_age", "Age")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true)
         .setParentId("who").setParentIncludes(true),
         new Layer("participant_language", "Language")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true)
         .setParentId("who").setParentIncludes(true),
         new Layer("turn", "Speaker turns")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false)
         .setParentId("who").setParentIncludes(true),
         new Layer("utterance", "Utterances")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(true)
         .setParentId("turn").setParentIncludes(true),
         new Layer("word", "Words")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false)
         .setParentId("turn").setParentIncludes(true));
      
      File dir = getDir();
      Graph graph = new Graph()
         .setId("serialize-test-nosync.txt")
         .setSchema(schema);
      graph.addAnchor(new Anchor("a0", 0.0));
      graph.addAnchor(new Anchor("a5", 5.4321)); // will be rendered 5432
      graph.addAnchor(new Anchor("a10", 10.0));
      graph.addAnchor(new Anchor("a15", 15.0));
      // meta-data
      graph.addAnnotation(new Annotation("lang", "en", "transcript_language", "a0", "a15"));
      graph.addAnnotation(new Annotation("date", "1972-09-26", "transcript_date", "a0", "a15"));
      graph.addAnnotation(
        new Annotation("loc", "Boston, MA, USA", "transcript_location", "a0", "a15"));
      graph.addAnnotation(new Annotation("rq", "5", "transcript_recording_quality", "a0", "a15"));
      graph.addAnnotation(new Annotation("layout", "Kitchen; Table in center of room with window on west wall, door to outside on north wall", "transcript_room_layout", "a0", "a15"));
      graph.addAnnotation(
        new Annotation("tape", "tape74, side a, 104", "transcript_tape_location", "a0", "a15"));
      // participants
      graph.addAnnotation(new Annotation("child", "John Smith", "who", "a0", "a15"));
      graph.addAnnotation(new Annotation("mother", "Mrs. Smith", "who", "a0", "a15"));
      graph.addAnnotation(new Annotation("child-main", "John Smith", "main_participant", "a0", "a15",
                                         "child"));
      graph.addAnnotation(new Annotation("child-age", "2;10.10", "participant_age", "a0", "a15",
                                         "child"));
      graph.addAnnotation(new Annotation("child-gender", "M", "participant_gender", "a0", "a15",
                                         "child"));
      graph.addAnnotation(new Annotation("child-language", "en", "participant_language", "a0", "a15",
                                         "child"));
      graph.addAnnotation(new Annotation("mother-language", "Spanish", "participant_language", "a0", "a15",
                                         "mother"));
      // turns
      graph.addAnnotation(new Annotation("t1", "John Smith", "turn", "a0", "a10", "child"));
      graph.addAnnotation(new Annotation("t2", "Mrs. Smith", "turn", "a10", "a15", "mother"));
      // utterances
      graph.addAnnotation(new Annotation("u1", "John Smith", "utterance", "a0", "a5", "t1"));
      graph.addAnnotation(new Annotation("u2", "John Smith", "utterance", "a5", "a10", "t1"));
      graph.addAnnotation(new Annotation("u3", "Mrs. Smith", "utterance", "a10", "a15", "t2"));
      
      // words
      graph.addAnnotation(new Annotation("the", "The", "word",
                                         "a0",
                                         // 1.2345 will become ..._1234
                                         graph.addAnchor(new Anchor("a1", 1.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("quick", "'quick", "word", 
                                         "a1",
                                         graph.addAnchor(new Anchor("a2", 2.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("brown", "brown'", "word", 
                                         "a2",
                                         graph.addAnchor(new Anchor("a3", 3.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("fox", "fox", "word", 
                                         "a3",
                                         "a5",
                                         "t1"));
      
      graph.addAnnotation(new Annotation("jumps", "jumps -", "word", 
                                         "a5",
                                         graph.addAnchor(new Anchor("a6", 6.0)).getId(),
                                         "t1"));      
      graph.addAnnotation(new Annotation("over", "over", "word",
                                         "a6",
                                         graph.addAnchor(new Anchor("a7", 8.0)).getId(),
                                         "t1"));
      // noise
      graph.addAnnotation(new Annotation("cough", "coughs", "noise",
                                         "a7",
                                         graph.addAnchor(new Anchor("a8", 8.0)).getId(),
                                         "t1"));
      
      graph.addAnnotation(new Annotation("th~", "th~", "word", // th~ becomes &th
                                         "a8",
                                         "a10",
                                         "t1"));
      
      graph.addAnnotation(new Annotation("the2", "the", "word", 
                                         "a10",
                                         graph.addAnchor(new Anchor("a12", 12.0)).getId(),
                                         "t2"));
      graph.addAnnotation(new Annotation("lazy", "lazy", "word", 
                                         "a12",
                                         graph.addAnchor(new Anchor("a13", 13.0)).getId(),
                                         "t2"));
      graph.addAnnotation(new Annotation("dog", "\"dog\"", "word", 
                                         "a13",
                                         graph.addAnchor(new Anchor("a14", 14.0)).getId(),
                                         "t2"));
      graph.addAnnotation(new Annotation(".", ".", "word", 
                                         "a14",
                                         "a15",
                                         "t2"));
      
      // create serializer
      ChatSerialization serializer = new ChatSerialization();
      
      // general configuration
      ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      configuration.get("includeTimeCodes").setValue(Boolean.FALSE);
      configuration = serializer.configure(configuration, schema);
      assertEquals(39, configuration.size());
      assertEquals("scribe attribute", "scribe", 
		   ((Layer)configuration.get("transcriberLayer").getValue()).getId());
      assertEquals("languages attribute", "transcript_language", 
		   ((Layer)configuration.get("languagesLayer").getValue()).getId());
      assertEquals("date attribute", "transcript_date", 
		   ((Layer)configuration.get("dateLayer").getValue()).getId());
      assertEquals("recording quality attribute", "transcript_recording_quality", 
		   ((Layer)configuration.get("recordingQualityLayer").getValue()).getId());
      assertEquals("room layout attribute", "transcript_room_layout", 
		   ((Layer)configuration.get("roomLayoutLayer").getValue()).getId());
      assertEquals("tape location attribute", "transcript_tape_location", 
		   ((Layer)configuration.get("tapeLocationLayer").getValue()).getId());
      assertEquals("target participant attribute", "main_participant", 
		   ((Layer)configuration.get("targetParticipantLayer").getValue()).getId());
      assertEquals("non-word layer", "noise", 
		   ((Layer)configuration.get("nonWordLayer").getValue()).getId());
      assertEquals("sex attribute mapped to gender", "participant_gender",
                   ((Layer)configuration.get("sexLayer").getValue()).getId());
      assertEquals("age attribute", "participant_age",
                   ((Layer)configuration.get("ageLayer").getValue()).getId());
      assertEquals("includeTimeCodes", Boolean.FALSE, 
		   configuration.get("includeTimeCodes").getValue());
      assertNull("No mor layer", 
                 configuration.get("morLayer").getValue());
      assertNull("No pause layer", 
                 configuration.get("pauseLayer").getValue());

      LinkedHashSet<String> needLayers = new LinkedHashSet<String>(
         Arrays.asList(serializer.getRequiredLayers()));
      assertEquals("Needed layers: " + needLayers,
                   11, needLayers.size());
      assertTrue(needLayers.contains("who"));
      assertTrue(needLayers.contains("main_participant"));
      assertTrue(needLayers.contains("scribe"));
      assertTrue(needLayers.contains("transcript_language"));
      assertTrue(needLayers.contains("turn"));
      assertTrue(needLayers.contains("utterance"));
      assertTrue(needLayers.contains("word"));
      assertTrue(needLayers.contains("noise"));
      assertTrue(needLayers.contains("participant_age"));
      assertTrue(needLayers.contains("participant_gender"));
      assertTrue(needLayers.contains("participant_language"));
      
      // serialize
      final Vector<SerializationException> exceptions = new Vector<SerializationException>();
      final Vector<NamedStream> streams = new Vector<NamedStream>();
      String[] layers = {"word","transcript_language"};
      Graph[] graphs = { graph };
      serializer.serialize(Arrays.spliterator(graphs), layers,
                           stream -> streams.add(stream),
                           warning -> System.out.println(warning),
                           exception -> exceptions.add(exception));
      if (exceptions.size() > 0) {
         fail(exceptions.stream()
              .map(x -> {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    x.printStackTrace(pw);
                    return x.toString() + ": " + sw;
                 })
              .collect(Collectors.joining("\n","","")));
      }
      
      streams.elementAt(0).save(dir);
      
      // test using diff
      File result = new File(dir, "serialize-test-nosync.cha");
      String differences = diff(new File(dir, "expected_serialize-test-nosync.cha"), result);
      if (differences != null) {
         fail(differences);
      } else {
         result.delete();
      }
   }

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
      org.junit.runner.JUnitCore.main("nzilbb.formatter.clan.TestChatSerialization");
   }
}
