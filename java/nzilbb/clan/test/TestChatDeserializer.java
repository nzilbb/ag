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
import java.util.SortedSet;
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
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
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
      ChatDeserializer deserializer = new ChatDeserializer();

      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(20, deserializer.configure(configuration, schema).size());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());

      // configure the deserialization
      deserializer.setParameters(defaultParamaters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings())
      {
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
      assertEquals(2, g.list("who").length);
      assertEquals("Nony_mouse", g.getAnnotation("SUB").getLabel());
      assertEquals("who", g.getAnnotation("SUB").getLayerId());
      assertEquals("Investigator", g.getAnnotation("EXA").getLabel());
      assertEquals("who", g.getAnnotation("EXA").getLayerId());

      // participant meta data
      assertEquals("en", g.getAnnotation("SUB").my("language").getLabel());
      assertEquals("en", g.getAnnotation("EXA").my("language").getLabel());
      assertEquals("W", g.getAnnotation("SUB").my("corpus").getLabel());
      assertEquals("W", g.getAnnotation("EXA").my("corpus").getLabel());
      assertEquals("Participant", g.getAnnotation("SUB").my("role").getLabel());
      assertEquals("Investigator", g.getAnnotation("EXA").my("role").getLabel());

      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(1, turns.length);
      assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
      assertEquals("unaligned final utterance - turn has end time", 
		   Double.valueOf(681.935), turns[0].getEnd().getOffset());
      assertEquals(g.getAnnotation("SUB"), turns[0].getParent());

      // utterances
      Annotation[] utterances = g.list("utterance");
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

      assertEquals("linking utterance", "", utterances[4].getLabel());
      assertEquals("linking utterance", Double.valueOf(29.994), utterances[4].getStart().getOffset());
      assertEquals("linking utterance", Double.valueOf(34.723), utterances[4].getEnd().getOffset());

      assertEquals(Double.valueOf(34.723), utterances[5].getStart().getOffset());
      assertEquals(Double.valueOf(35.752), utterances[5].getEnd().getOffset());

      assertEquals("mid-line synchronisation - first utterance", 
		   Double.valueOf(414.937), utterances[142].getStart().getOffset());
      assertEquals("mid-line synchronisation - first utterance", 
		   Double.valueOf(418.673), utterances[142].getEnd().getOffset());
      assertEquals("mid-line synchronisation - linking utterance", 
		   Double.valueOf(418.673), utterances[143].getStart().getOffset());
      assertEquals("mid-line synchronisation - linking utterance", 
		   Double.valueOf(418.809), utterances[143].getEnd().getOffset());
      assertEquals("mid-line synchronisation - second utterance", 
		   Double.valueOf(418.809), utterances[144].getStart().getOffset());
      assertEquals("mid-line synchronisation - second utterance", 
		   Double.valueOf(420.631), utterances[144].getEnd().getOffset());

      assertEquals("overlapping utterances - first start unchanged", 
		   Double.valueOf(452.319), utterances[159].getStart().getOffset());
      assertEquals("overlapping utterances - first end unchanged", 
		   Double.valueOf(455.432), utterances[159].getEnd().getOffset());
      assertEquals("overlapping utterances - second start changed", 
		   Double.valueOf(455.432), utterances[160].getStart().getOffset());
      assertEquals("overlapping utterances - second end unchanged", 
		   Double.valueOf(460.584), utterances[160].getEnd().getOffset());

      assertEquals("unsynchronised utterance - before", 
		   Double.valueOf(489.467), utterances[170].getStart().getOffset());
      assertEquals("unsynchronised utterances - before - end moved", 
		   Double.valueOf(491.397), utterances[170].getEnd().getOffset());
      assertEquals("unsynchronised utterances - before - low confidence", 
		   Constants.CONFIDENCE_DEFAULT, utterances[170].getEnd().getConfidence().intValue());
      assertEquals("unsynchronised utterance - chained with utterance before", 
		   utterances[170].getEnd(), utterances[171].getStart());

      assertEquals("unsynchronised utterance - end original alignment", 
		   Double.valueOf(493.327), utterances[171].getEnd().getOffset());
      assertEquals("unsynchronised utterance - after", 
		   Double.valueOf(493.327), utterances[172].getStart().getOffset());
      assertEquals("unsynchronised utterances - after", 
		   Double.valueOf(496.813), utterances[172].getEnd().getOffset());

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
      
      Annotation[] words = g.list("turn")[0].list("word");
      String[] wordLabels = { // NB we have a c-unit layer, so terminators are stripped off 
	 "ab", "abc", "abcdef", "abcd", "gonna", "lie", "abcd", "abc", "pet", "abcdefghij", 
	 "abc", "abcde", "abc", "ab", "abcd", "ab", "abc", "worryin", "i", "abcd",
	 "she'll", "ab", "nd", "abcdefg", 
	 "abcd", "abcdefg", "ab", "abc", "abcdef", "abcde", "abc", "ab", "abc", "abcdefgh", "abc", "abcdef", "abc",
	 "ab", "abcde", "until", "she's", "abc", "ab", "abcde", "abcdef", "abcde", "ab", "abc", "baby's", "crib", 
	 "abc", "abcdefg", "abc", "abcd", "abcde", "abc", "abcd", "abc", "a", "b", "c", "d"};
      for (int i = 0; i < wordLabels.length; i++)
      {
	 assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
      }
      for (int i = 0; i < words.length; i++)
      {
	 assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
	 	      i+1, words[i].getOrdinal());
      }

      assertEquals("they've", words[270].getLabel());
      assertEquals("work", words[271].getLabel());
      assertEquals("ab", words[272].getLabel());
      assertEquals("a", words[273].getLabel());
      assertEquals("hunger", words[274].getLabel());
      assertEquals("ab", words[275].getLabel());
      assertEquals("Unaligned last utterance doesn't cause @End to be last word",
		   "test", words[words.length-1].getLabel());

      assertEquals(turns[0].getId(), words[271].getParentId());
      assertEquals(turns[0].getId(), words[272].getParentId());
      assertEquals(turns[0].getId(), words[273].getParentId());
      assertEquals(turns[0].getId(), words[274].getParentId());
      assertEquals(turns[0].getId(), words[275].getParentId());
      assertEquals(turns[0].getId(), words[276].getParentId());
      assertEquals(turns[0].getId(), words[277].getParentId());

      for (int i = 0; i < words.length; i++)
      {	 
	 assertEquals("ordinals correct " + words[i], i+1, words[i].getOrdinal());
	 assertEquals("tagged as manual: " + words[i], 
		      Integer.valueOf(Constants.CONFIDENCE_MANUAL), words[i].getConfidence());
      }

      // c-units
      Annotation[] cUnits = g.list("c-unit");
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
		   utterances[170].getStart(), cUnits[102].getStart());
      assertEquals("unsynchronised utterances c-unit", 
		   utterances[171].getEnd(), cUnits[102].getEnd());
      assertEquals("unsynchronised utterance - c-unit after", 
		   utterances[172].getStart(), cUnits[103].getStart());
      assertEquals("unsynchronised utterances - c-unit after", 
		   utterances[173].getEnd(), cUnits[103].getEnd());

      assertEquals("unsynchronised last utterance - last c-unit start", 
		   utterances[utterances.length-1].getStart(), cUnits[cUnits.length-1].getStart());
      assertEquals("unsynchronised last utterance - last c-unit end", 
		   utterances[utterances.length-1].getEnd(), cUnits[cUnits.length-1].getEnd());

      for (Annotation a : cUnits)
      {
	 assertEquals("tagged as manual: " + a + " " + a.getStart() + "-" + a.getEnd(), 
		      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
	 assertEquals("parent set: " + a + " " + a.getStart() + "-" + a.getEnd(), 
		      turns[0], a.getParent());
      }

      // disfluency
      assertEquals("i", words[18].getLabel());
      assertEquals("&", words[18].my("disfluency").getLabel());
      assertEquals("word is parent", words[18], words[18].my("disfluency").getParent());
      // ensure they're marked as manual annotations
      for (Annotation a : g.list("disfluency"))
      {
	 assertEquals("tagged as manual: " + a, 
		      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

      Annotation[] expansions = g.list("expansion");
      assertEquals(1, expansions.length);
      assertEquals("going to", expansions[0].getLabel());
      assertEquals("gonna", expansions[0].my("word").getLabel());
      assertEquals(expansions[0].my("word"), expansions[0].getParent());
      assertEquals(expansions[0].my("word"), words[4]);
      assertEquals("Pre expansion ordinal", "gonna", words[4].getLabel());
      assertEquals("Pre expansion ordinal", 5, words[4].getOrdinal());
      assertEquals("Post expansion ordinal", "lie", words[5].getLabel());
      assertEquals("Post expansion ordinal", 6, words[5].getOrdinal());
      for (Annotation a : expansions)
      {
	 assertEquals("tagged as manual: " + a, 
		      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

      // linkages
      Annotation[] linkages = g.list("linkage");
      assertEquals(1, linkages.length);
      assertEquals("a_b_c_d", linkages[0].getLabel());

      // errors
      Annotation[] errors = g.list("error");
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
      Annotation[] completions = g.list("completion");
      assertEquals(3, completions.length);
      for (Annotation a : completions)
      {
	 assertEquals("tagged as manual: " + a, 
		      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

      assertEquals("leading completion", "nd", words[22].getLabel());
      assertEquals("leading completion", "and", words[22].my("completion").getLabel());
      assertEquals("completion - word is parent", words[22], words[22].my("completion").getParent());
      assertEquals("trailing completion", "worryin", words[17].getLabel());
      assertEquals("trailing completion", "worrying", words[17].my("completion").getLabel());

      assertEquals("leading/trailing completion", "fridge", words[280].getLabel());
      assertEquals("leading/trailing completion", "refridgerator", words[280].my("completion").getLabel());

      // retracing
      Annotation[] retracing = g.list("retracing");
      assertEquals(6, retracing.length);
      for (Annotation a : retracing)
      {
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
      Annotation[] repetition = g.list("repetition");
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

      for (Annotation a : repetition)
      {
	 assertEquals("tagged as manual: " + a, 
		      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

      // gems
      Annotation[] gems = g.list("gem");
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
      assertEquals(turns[0].getEnd(), gems[10].getEnd());
      for (Annotation a : gems)
      {
	 assertEquals("tagged as manual: " + a, 
		      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

   }

   @Test public void minimalConversionWithoutAnnotations() 
      throws Exception
   {
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
      ChatDeserializer deserializer = new ChatDeserializer();

      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(20, deserializer.configure(configuration, schema).size());

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
      
      assertEquals("test.cha", g.getId());
      String[] transcribers = g.labels("transcriber"); 
      assertEquals(2, transcribers.length);
      assertEquals("Alan Turing", transcribers[0]);
      assertEquals("Oscar Wilde", transcribers[1]);
      String[] languages = g.labels("languages"); 
      assertEquals(1, languages.length);
      assertEquals("en", languages[0]);

      // participants     
      assertEquals(2, g.list("who").length);
      assertEquals("Nony_mouse", g.getAnnotation("SUB").getLabel());
      assertEquals("who", g.getAnnotation("SUB").getLayerId());
      assertEquals("Investigator", g.getAnnotation("EXA").getLabel());
      assertEquals("who", g.getAnnotation("EXA").getLayerId());

      // participant meta data
      assertEquals("en", g.getAnnotation("SUB").my("language").getLabel());
      assertEquals("en", g.getAnnotation("EXA").my("language").getLabel());
      assertEquals("W", g.getAnnotation("SUB").my("corpus").getLabel());
      assertEquals("W", g.getAnnotation("EXA").my("corpus").getLabel());
      assertEquals("Participant", g.getAnnotation("SUB").my("role").getLabel());
      assertEquals("Investigator", g.getAnnotation("EXA").my("role").getLabel());

      // turns
      Annotation[] turns = g.list("turn");
      assertEquals(1, turns.length);
      assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
      assertEquals("unaligned final utterance - turn has end time", 
		   Double.valueOf(681.935), turns[0].getEnd().getOffset());
      assertEquals(g.getAnnotation("SUB"), turns[0].getParent());

      // utterances
      Annotation[] utterances = g.list("utterance");
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

      assertEquals("linking utterance", "", utterances[4].getLabel());
      assertEquals("linking utterance", Double.valueOf(29.994), utterances[4].getStart().getOffset());
      assertEquals("linking utterance", Double.valueOf(34.723), utterances[4].getEnd().getOffset());

      assertEquals(Double.valueOf(34.723), utterances[5].getStart().getOffset());
      assertEquals(Double.valueOf(35.752), utterances[5].getEnd().getOffset());

      assertEquals("mid-line synchronisation - first utterance", 
		   Double.valueOf(414.937), utterances[142].getStart().getOffset());
      assertEquals("mid-line synchronisation - first utterance", 
		   Double.valueOf(418.673), utterances[142].getEnd().getOffset());
      assertEquals("mid-line synchronisation - linking utterance", 
		   Double.valueOf(418.673), utterances[143].getStart().getOffset());
      assertEquals("mid-line synchronisation - linking utterance", 
		   Double.valueOf(418.809), utterances[143].getEnd().getOffset());
      assertEquals("mid-line synchronisation - second utterance", 
		   Double.valueOf(418.809), utterances[144].getStart().getOffset());
      assertEquals("mid-line synchronisation - second utterance", 
		   Double.valueOf(420.631), utterances[144].getEnd().getOffset());

      assertEquals("overlapping utterances - first start unchanged", 
		   Double.valueOf(452.319), utterances[159].getStart().getOffset());
      assertEquals("overlapping utterances - first end unchanged", 
		   Double.valueOf(455.432), utterances[159].getEnd().getOffset());
      assertEquals("overlapping utterances - second start changed", 
		   Double.valueOf(455.432), utterances[160].getStart().getOffset());
      assertEquals("overlapping utterances - second end unchanged", 
		   Double.valueOf(460.584), utterances[160].getEnd().getOffset());

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
      
      Annotation[] words = g.list("word");
      String[] wordLabels = { 
	 // NB we have no c-unit layer, so terminators are still present
	 // NB we have no linkage layer, so linkages are not split
	 "ab", "abc", "abcdef", "abcd", "gonna", "lie", "abcd", "abc", "pet", "abcdefghij", 
	 "abc", "abcde", "abc", "ab", "abcd", "ab", "abc", "worryin", "i", "abcd.",
	 "she'll", "ab", "nd", "abcdefg.", 
	 "abcd", "abcdefg", "ab", "abc", "abcdef", "abcde", "abc", "ab", "abc", "abcdefgh", "abc", "abcdef", "abc", "+//?",
	 "ab", "abcde", "until", "she's", "abc", "ab", "abcde", "abcdef", "abcde", "ab", "abc", "baby's", "crib", 
	 "abc", "abcdefg", "abc", "abcd", "abcde", "abc", "abcd", "abc", "a_b_c_d."};
      for (int i = 0; i < wordLabels.length; i++)
      {	 
	 assertEquals("word labels " + i, wordLabels[i], words[i].getLabel());
      }
      for (int i = 0; i < words.length; i++)
      {
	 assertEquals("Correct ordinal: " + i + " " + words[i].getLabel(), 
	 	      i+1, words[i].getOrdinal());
      }

      // disfluency
      assertEquals("i", words[18].getLabel());
      assertNull("disfluency not tagged", words[18].my("disfluency"));
      assertEquals("no disfluencies", 0, g.list("disfluency").length);

      // expansions
      assertEquals("no expansions", 0, g.list("expansion").length);
      assertEquals("Pre expansion ordinal", "gonna", words[4].getLabel());
      assertEquals("Pre expansion ordinal", 5, words[4].getOrdinal());
      assertEquals("Post expansion ordinal", "lie", words[5].getLabel());
      assertEquals("Post expansion ordinal", 6, words[5].getOrdinal());

      // errors
      assertEquals("errors not tagged", 0, g.list("error").length);

      assertEquals("they've", words[268].getLabel());
      assertEquals("work", words[269].getLabel());
      assertEquals("ab", words[270].getLabel());
      assertEquals("a", words[271].getLabel());
      assertEquals("hunger", words[272].getLabel());
      assertEquals(".", words[273].getLabel());
      assertEquals("ab", words[274].getLabel());

      assertEquals(turns[0].getId(), words[267].getParentId());
      assertEquals(turns[0].getId(), words[268].getParentId());
      assertEquals(turns[0].getId(), words[269].getParentId());
      assertEquals(turns[0].getId(), words[270].getParentId());
      assertEquals(turns[0].getId(), words[271].getParentId());
      assertEquals(turns[0].getId(), words[272].getParentId());
      assertEquals(turns[0].getId(), words[273].getParentId());

      // completion
      assertEquals("no completions", 0, g.list("completion").length);

      // retracing
      assertEquals("no retracing", 0, g.list("retracing").length);

      // repetition
      assertEquals("no repetitions", 0, g.list("repetition").length);

      // gems
      assertEquals("no gems", 0, g.list("gem").length);
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
      org.junit.runner.JUnitCore.main("nzilbb.clan.test.TestChatDeserializer");
   }
}
