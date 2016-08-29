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

package nzilbb.ag.util.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.Vector;
import java.util.Iterator;
import nzilbb.ag.util.*;
import nzilbb.ag.*;

public class TestDefaultOffsetGenerator
{
      
   @Test public void basicInterpolation() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      g.setCorpus("cc");

      g.addLayer(new Layer("who", "Participants", Constants.ALIGNMENT_NONE, 
			   true, // peers
			   true, // peersOverlap
			   true)); // saturated
      g.addLayer(new Layer("turn", "Speaker turns", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   false, // saturated
			   "who", // parentId
			   true)); // parentIncludes
      g.addLayer(new Layer("word", "Words", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   false, // saturated
			   "turn", // parentId
			   true)); // parentIncludes

      g.addAnchor(new Anchor("turnStart", 0.0)); // turn start
      g.addAnchor(new Anchor("a0", null)); // the
      g.addAnchor(new Anchor("a1", null)); // quick
      g.addAnchor(new Anchor("a2", null)); // brown
      g.addAnchor(new Anchor("a3", null)); // fox
      g.addAnchor(new Anchor("a4", null)); // jumps
      g.addAnchor(new Anchor("a5", null)); // over
      g.addAnchor(new Anchor("a6", null)); // a
      g.addAnchor(new Anchor("a7", null)); // lazy
      g.addAnchor(new Anchor("a8", null)); // dog
      g.addAnchor(new Anchor("a9", null)); // end of dog
      g.addAnchor(new Anchor("turnEnd", 9.0)); // turn end

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "participant1"));
      
      g.addAnnotation(new Annotation("the", "the", "word", "a0", "a1", "turn1"));
      g.addAnnotation(new Annotation("quick", "quick", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("brown", "brown", "word", "a2", "a3", "turn1"));
      g.addAnnotation(new Annotation("fox", "fox", "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("jumps", "jumps", "word", "a4", "a5", "turn1"));
      g.addAnnotation(new Annotation("over", "over", "word", "a5", "a6", "turn1"));
      g.addAnnotation(new Annotation("a", "a", "word", "a6", "a7", "turn1"));
      g.addAnnotation(new Annotation("lazy", "lazy", "word", "a7", "a8", "turn1"));
      g.addAnnotation(new Annotation("dog", "dog", "word", "a8", "a9", "turn1"));

      DefaultOffsetGenerator generator = new DefaultOffsetGenerator();
      // generator.setDebug(true);
      try
      {
	 Vector<Change> changes = generator.transform(g);
	 if (generator.getLog() != null) for (String m : generator.getLog()) System.out.println(m);

	 // test the values are what we expected

	 // test the changes are recorded
	 Iterator<Change> order = changes.iterator();
	 assertEquals(new Double(0.0), g.getAnchor("a0").getOffset());
	 assertEquals(new Double(1.0), g.getAnchor("a1").getOffset());
	 assertEquals(new Double(2.0), g.getAnchor("a2").getOffset());
	 assertEquals(new Double(3.0), g.getAnchor("a3").getOffset());
	 assertEquals(new Double(4.0), g.getAnchor("a4").getOffset());
	 assertEquals(new Double(5.0), g.getAnchor("a5").getOffset());
	 assertEquals(new Double(6.0), g.getAnchor("a6").getOffset());
	 assertEquals(new Double(7.0), g.getAnchor("a7").getOffset());
	 assertEquals(new Double(8.0), g.getAnchor("a8").getOffset());
	 assertEquals(new Double(9.0), g.getAnchor("a9").getOffset());

	 // collapsed back to start of turn
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a0"), "offset", new Double(0.0)), 
		      order.next());
	 // collapsed forward to end of turn
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a9"), "offset", new Double(9.0)), 
		      order.next());
	 // then the rest interpolated between
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a1"), "offset", new Double(1.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a2"), "offset", new Double(2.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a3"), "offset", new Double(3.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a4"), "offset", new Double(4.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a5"), "offset", new Double(5.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a6"), "offset", new Double(6.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a7"), "offset", new Double(7.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a8"), "offset", new Double(8.0)), 
		      order.next());
	 assertEquals("no extra changes to graph", changes.size(), g.getChanges().size());

      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void basicInterpolationWithConfidence() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      g.setCorpus("cc");

      g.addLayer(new Layer("who", "Participants", Constants.ALIGNMENT_NONE, 
			   true, // peers
			   true, // peersOverlap
			   true)); // saturated
      g.addLayer(new Layer("turn", "Speaker turns", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   false, // saturated
			   "who", // parentId
			   true)); // parentIncludes
      g.addLayer(new Layer("word", "Words", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   false, // saturated
			   "turn", // parentId
			   true)); // parentIncludes

      g.addAnchor(new Anchor("turnStart", 0.0)); // turn start
      g.getAnchor("turnStart").put(Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL);
      g.addAnchor(new Anchor("a0", 0.1)); // the
      g.getAnchor("a0").put(Constants.CONFIDENCE, Constants.CONFIDENCE_NONE);
      g.addAnchor(new Anchor("a05", 0.2)); // quick
      g.getAnchor("a05").put(Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT);
      g.addAnchor(new Anchor("a1", 1.3)); // brown
      g.getAnchor("a1").put(Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT);
      g.addAnchor(new Anchor("a15", 1.4)); // fox
      g.getAnchor("a15").put(Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT);
      g.addAnchor(new Anchor("a2", 2.0)); // jumps
      g.getAnchor("a2").put(Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL);
      g.addAnchor(new Anchor("a3", 3.3)); // over
      g.getAnchor("a3").put(Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC);
      g.addAnchor(new Anchor("a4", 4.4)); // a
      g.getAnchor("a4").put(Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC);
      g.addAnchor(new Anchor("a5", 5.5)); // lazy
      g.getAnchor("a5").put(Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC);
      g.addAnchor(new Anchor("a6", 6.6)); // dog
      // no confidence set for a6
      g.addAnchor(new Anchor("a7", null)); // end of dog
      // no confidence set for a7
      g.addAnchor(new Anchor("turnEnd", 7.0)); // turn end
      g.getAnchor("turnEnd").put(Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL);

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "participant1"));
      
      g.addAnnotation(new Annotation("the",   "the",   "word", "a0",  "a05", "turn1"));
      g.addAnnotation(new Annotation("quick", "quick", "word", "a05", "a1", "turn1"));
      g.addAnnotation(new Annotation("brown", "brown", "word", "a1",  "a15", "turn1"));
      g.addAnnotation(new Annotation("fox",   "fox",   "word", "a15", "a2", "turn1"));
      g.addAnnotation(new Annotation("jumps", "jumps", "word", "a2",  "a3", "turn1"));
      g.addAnnotation(new Annotation("over",  "over",  "word", "a3",  "a4", "turn1"));
      g.addAnnotation(new Annotation("a",     "a",     "word", "a4",  "a5", "turn1"));
      g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a5",  "a6", "turn1"));
      g.addAnnotation(new Annotation("dog",  "dog",    "word", "a6",  "a7", "turn1"));

      DefaultOffsetGenerator generator = new DefaultOffsetGenerator();
      generator.setDefaultAnchorConfidence(Constants.CONFIDENCE_NONE);
      generator.setDefaultOffsetThreshold(Constants.CONFIDENCE_AUTOMATIC);
      // generator.setDebug(true);
      try
      {
	 Vector<Change> changes = generator.transform(g);
	 if (generator.getLog() != null) for (String m : generator.getLog()) System.out.println(m);

	 // test the values are what we expected

	 // test the changes are recorded
	 Iterator<Change> order = changes.iterator();
	 assertEquals(new Double(0.0), g.getAnchor("a0").getOffset());
	 assertEquals(new Double(0.5), g.getAnchor("a05").getOffset());
	 assertEquals(new Double(1.0), g.getAnchor("a1").getOffset());
	 assertEquals(new Double(1.5), g.getAnchor("a15").getOffset());
	 assertEquals(new Double(2.0), g.getAnchor("a2").getOffset());
	 assertEquals(new Double(3.0), g.getAnchor("a3").getOffset());
	 assertEquals(new Double(4.0), g.getAnchor("a4").getOffset());
	 assertEquals(new Double(5.0), g.getAnchor("a5").getOffset());
	 assertEquals(new Double(6.0), g.getAnchor("a6").getOffset());
	 assertEquals(new Double(7.0), g.getAnchor("a7").getOffset());

	 // collapsed back to start of turn
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a0"), "offset", new Double(0.0)), 
		      order.next());
	 // then the rest interpolated between
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a05"), "offset", new Double(0.5)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a1"), "offset", new Double(1.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a15"), "offset", new Double(1.5)), 
		      order.next());
	 // a2 not changed
	 // collapsed forward to end of span
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a7"), "offset", new Double(7.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a3"), "offset", new Double(3.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a4"), "offset", new Double(4.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a5"), "offset", new Double(5.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a6"), "offset", new Double(6.0)), 
		      order.next());
	 assertEquals("no extra changes to graph", changes.size(), g.getChanges().size());

      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void utterancesPartitionWordsInTurn() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      g.setCorpus("cc");

      g.addLayer(new Layer("who", "Participants", Constants.ALIGNMENT_NONE, 
			   true, // peers
			   true, // peersOverlap
			   true)); // saturated
      g.addLayer(new Layer("turn", "Speaker turns", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   false, // saturated
			   "who", // parentId
			   true)); // parentIncludes
      g.addLayer(new Layer("utterance", "Utterance", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   true, // saturated
			   "turn", // parentId
			   true)); // parentIncludes
      g.addLayer(new Layer("word", "Words", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   false, // saturated
			   "turn", // parentId
			   true)); // parentIncludes

      g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL)); // turn start

      g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE, Constants.CONFIDENCE_NONE)); // the
      g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // quick
      g.addAnchor(new Anchor("a02", 0.03, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // brown
      g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // fox
      g.addAnchor(new Anchor("a04a", 0.04, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // fox end

      g.addAnchor(new Anchor("utteranceChange", 0.4, Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL)); // utterance boundary

      g.addAnchor(new Anchor("a04b", 2.0, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // jumps
      g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // over
      g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // a
      g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // lazy
      // no confidence set for a44 and a54
      g.addAnchor(new Anchor("a44", 5.1)); // dog
      g.addAnchor(new Anchor("a54", null)); // end of dog

      // no confidence set for a7
      g.addAnchor(new Anchor("turnEnd", 5.4, Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL)); // turn end

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "participant1"));

      g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utteranceChange", "turn1"));
      g.addAnnotation(new Annotation("utterance2", "john smith", "utterance", "utteranceChange", "turnEnd", "turn1"));
      
      g.addAnnotation(new Annotation("the",   "the",   "word", "a0",  "a01", "turn1"));
      g.addAnnotation(new Annotation("quick", "quick", "word", "a01", "a02", "turn1"));
      g.addAnnotation(new Annotation("brown", "brown", "word", "a02",  "a03", "turn1"));
      g.addAnnotation(new Annotation("fox",   "fox",   "word", "a03", "a04a", "turn1"));

      g.addAnnotation(new Annotation("jumps", "jumps", "word", "a04b",  "a14", "turn1"));
      g.addAnnotation(new Annotation("over",  "over",  "word", "a14",  "a24", "turn1"));
      g.addAnnotation(new Annotation("a",     "a",     "word", "a24",  "a34", "turn1"));
      g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a34",  "a44", "turn1"));
      g.addAnnotation(new Annotation("dog",  "dog",    "word", "a44",  "a54", "turn1"));

      DefaultOffsetGenerator generator = new DefaultOffsetGenerator();
      generator.setDefaultAnchorConfidence(Constants.CONFIDENCE_NONE);
      generator.setDefaultOffsetThreshold(Constants.CONFIDENCE_AUTOMATIC);
      // generator.setDebug(true);
      try
      {
	 Vector<Change> changes = generator.transform(g);
	 if (generator.getLog() != null) for (String m : generator.getLog()) System.out.println(m);

	 // test the values are what we expected

	 // test the changes are recorded
	 Iterator<Change> order = changes.iterator();
	 assertEquals(new Double(0.0), g.getAnchor("a0").getOffset());
	 assertEquals(new Double(0.1), g.getAnchor("a01").getOffset());
	 assertEquals(new Double(0.2), g.getAnchor("a02").getOffset());
	 // yay for inexact floating point representations!
	 assertEquals(new Double(0.30000000000000004), g.getAnchor("a03").getOffset());
	 assertEquals(new Double(0.4), g.getAnchor("a04a").getOffset());
	 assertEquals(new Double(0.4), g.getAnchor("a04b").getOffset());
	 assertEquals(new Double(1.4), g.getAnchor("a14").getOffset());
	 assertEquals(new Double(2.4), g.getAnchor("a24").getOffset());
	 assertEquals(new Double(3.4), g.getAnchor("a34").getOffset());
	 assertEquals(new Double(4.4), g.getAnchor("a44").getOffset());
	 assertEquals(new Double(5.4), g.getAnchor("a54").getOffset());

	 // collapsed back to start of turn
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a0"), "offset", new Double(0.0)), 
		      order.next());
	 // collapsed forward to end of utterance
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a04a"), "offset", new Double(0.4)), 
		      order.next());
	 // then the rest interpolated between
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a01"), "offset", new Double(0.1)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a02"), "offset", new Double(0.2)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a03"), "offset", new Double(0.30000000000000004)), // yay for inexact floating point representations
		      order.next());
	 // collapsed back to start of utterance
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a04b"), "offset", new Double(0.4)), 
		      order.next());
	 // collapsed forward to end of utterance
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a54"), "offset", new Double(5.4)), 
		      order.next());
	 // then the rest interpolated between
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a14"), "offset", new Double(1.4)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a24"), "offset", new Double(2.4)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a34"), "offset", new Double(3.4)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a44"), "offset", new Double(4.4)), 
		      order.next());
	 assertEquals("no extra changes to graph", changes.size(), g.getChanges().size());

      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void overlappingInterpolation() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      g.setCorpus("cc");

      g.addLayer(new Layer("who", "Participants", Constants.ALIGNMENT_NONE, 
			   true, // peers
			   true, // peersOverlap
			   true)); // saturated
      g.addLayer(new Layer("turn", "Speaker turns", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   false, // saturated
			   "who", // parentId
			   true)); // parentIncludes
      g.addLayer(new Layer("word", "Words", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   false, // saturated
			   "turn", // parentId
			   true)); // parentIncludes

      // john smith
      g.addAnchor(new Anchor("turn1Start", 0.0)); // turn start
      g.addAnchor(new Anchor("a0", null)); // the
      g.addAnchor(new Anchor("a1", null)); // quick
      g.addAnchor(new Anchor("a2", null)); // brown
      g.addAnchor(new Anchor("a3", null)); // fox
      g.addAnchor(new Anchor("a4", null)); // jumps
      g.addAnchor(new Anchor("a5", null)); // over
      g.addAnchor(new Anchor("a6", null)); // a
      g.addAnchor(new Anchor("a7", null)); // lazy
      g.addAnchor(new Anchor("a8", null)); // dog
      g.addAnchor(new Anchor("a9", null)); // end of dog
      g.addAnchor(new Anchor("turn1End", 9.0)); // turn end

      // jane doe
      g.addAnchor(new Anchor("turn2Start", 6.5)); // turn start
      g.addAnchor(new Anchor("b6", null)); // roses
      g.addAnchor(new Anchor("b7", null)); // are
      g.addAnchor(new Anchor("b8", null)); // red
      g.addAnchor(new Anchor("b9", null)); // violets
      g.addAnchor(new Anchor("b10", null)); // are
      g.addAnchor(new Anchor("b11", null)); // blue
      g.addAnchor(new Anchor("b12", null)); // end of blue
      g.addAnchor(new Anchor("turn2End", 12.5)); // turn end

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "turn1Start", "turn2End", "my graph"));
      g.addAnnotation(new Annotation("participant2", "jane doe", "who", "turn1Start", "turn2End", "my graph"));
      
      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turn1Start", "turn1End", "participant1"));
      g.addAnnotation(new Annotation("turn2", "jane doe", "turn", "turn2Start", "turn2End", "participant2"));
      
      g.addAnnotation(new Annotation("the",   "the",   "word", "a0", "a1", "turn1"));
      g.addAnnotation(new Annotation("quick", "quick", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("brown", "brown", "word", "a2", "a3", "turn1"));
      g.addAnnotation(new Annotation("fox",   "fox",   "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("jumps", "jumps", "word", "a4", "a5", "turn1"));
      g.addAnnotation(new Annotation("over",  "over",  "word", "a5", "a6", "turn1"));
      g.addAnnotation(new Annotation("a",     "a",     "word", "a6", "a7", "turn1"));
      g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a7", "a8", "turn1"));
      g.addAnnotation(new Annotation("dog",  "dog",    "word", "a8", "a9", "turn1"));

      g.addAnnotation(new Annotation("roses",   "roses",   "word", "b6",  "b7", "turn2"));
      g.addAnnotation(new Annotation("are",     "are",     "word", "b7",  "b8", "turn2"));
      g.addAnnotation(new Annotation("red",     "red",     "word", "b8",  "b9", "turn2"));
      g.addAnnotation(new Annotation("violets", "violets", "word", "b9",  "b10", "turn2"));
      g.addAnnotation(new Annotation("are2",    "are",     "word", "b10", "b11", "turn2"));
      g.addAnnotation(new Annotation("blue",    "roses",   "word", "b11", "b12", "turn2"));

      DefaultOffsetGenerator generator = new DefaultOffsetGenerator();
      // generator.setDebug(true);
      try
      {
	 Vector<Change> changes = generator.transform(g);
	 if (generator.getLog() != null) for (String m : generator.getLog()) System.out.println(m);

	 // test the values are what we expected
	 assertEquals(new Double(0.0), g.getAnchor("a0").getOffset());
	 assertEquals(new Double(1.0), g.getAnchor("a1").getOffset());
	 assertEquals(new Double(2.0), g.getAnchor("a2").getOffset());
	 assertEquals(new Double(3.0), g.getAnchor("a3").getOffset());
	 assertEquals(new Double(4.0), g.getAnchor("a4").getOffset());
	 assertEquals(new Double(5.0), g.getAnchor("a5").getOffset());
	 assertEquals(new Double(6.0), g.getAnchor("a6").getOffset());
	 assertEquals(new Double(7.0), g.getAnchor("a7").getOffset());
	 assertEquals(new Double(8.0), g.getAnchor("a8").getOffset());
	 assertEquals(new Double(9.0), g.getAnchor("a9").getOffset());

	 assertEquals(new Double(6.5),  g.getAnchor("b6").getOffset());
	 assertEquals(new Double(7.5),  g.getAnchor("b7").getOffset());
	 assertEquals(new Double(8.5),  g.getAnchor("b8").getOffset());
	 assertEquals(new Double(9.5),  g.getAnchor("b9").getOffset());
	 assertEquals(new Double(10.5), g.getAnchor("b10").getOffset());
	 assertEquals(new Double(11.5), g.getAnchor("b11").getOffset());
	 assertEquals(new Double(12.5), g.getAnchor("b12").getOffset());

	 // test the changes are recorded
	 Iterator<Change> order = changes.iterator();

	 // collapsed back to start of turn
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a0"), "offset", new Double(0.0)), 
		      order.next());
	 // collapsed forward to end of turn
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a9"), "offset", new Double(9.0)), 
		      order.next());
	 // then the rest interpolated between
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a1"), "offset", new Double(1.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a2"), "offset", new Double(2.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a3"), "offset", new Double(3.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a4"), "offset", new Double(4.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a5"), "offset", new Double(5.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a6"), "offset", new Double(6.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a7"), "offset", new Double(7.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a8"), "offset", new Double(8.0)), 
		      order.next());

	 // collapsed back to start of turn
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("b6"), "offset", new Double(6.5)), 
		      order.next());
	 // collapsed forward to end of turn
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("b12"), "offset", new Double(12.5)), 
		      order.next());
	 // then the rest interpolated between
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("b7"), "offset", new Double(7.5)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("b8"), "offset", new Double(8.5)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("b9"), "offset", new Double(9.5)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("b10"), "offset", new Double(10.5)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("b11"), "offset", new Double(11.5)), 
		      order.next());

	 assertEquals("no extra changes to graph", changes.size(), g.getChanges().size());

      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void padForAnnotationsOnOtherLayers() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      g.setCorpus("cc");

      g.addLayer(new Layer("who", "Participants", Constants.ALIGNMENT_NONE, 
			   true, // peers
			   true, // peersOverlap
			   true)); // saturated
      g.addLayer(new Layer("noise", "Noise", Constants.ALIGNMENT_INTERVAL, 
			   true, // peers
			   true, // peersOverlap
			   false)); // saturated
      g.addLayer(new Layer("turn", "Speaker turns", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   false, // saturated
			   "who", // parentId
			   true)); // parentIncludes
      g.addLayer(new Layer("word", "Words", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   false, // saturated
			   "turn", // parentId
			   true)); // parentIncludes

      g.addAnchor(new Anchor("turnStart", 0.0)); // turn start & noise1
      g.addAnchor(new Anchor("a1", null)); // noise2
      g.addAnchor(new Anchor("a2", null)); // the
      g.addAnchor(new Anchor("a3", null)); // quick
      g.addAnchor(new Anchor("a4", null)); // noise3
      g.addAnchor(new Anchor("a5", null)); // noise4
      g.addAnchor(new Anchor("a6", null)); // brown
      g.addAnchor(new Anchor("a7", null)); // fox
      g.addAnchor(new Anchor("a8", null)); // noise5
      g.addAnchor(new Anchor("a9", null)); // noise6
      g.addAnchor(new Anchor("turnEnd", 10.0)); // turn & noise6 end

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "participant1"));
      
      g.addAnnotation(new Annotation("noise1", "click",   "noise", "turnStart", "a1", "my graph"));
      g.addAnnotation(new Annotation("noise2", "ahem",    "noise", "a1",        "a2", "my graph"));
      g.addAnnotation(new Annotation("the",    "the",     "word",  "a2",        "a3", "turn1"));
      g.addAnnotation(new Annotation("quick",  "quick",   "word",  "a3",        "a4", "turn1"));
      g.addAnnotation(new Annotation("noise3", "shuffle", "noise", "a4",        "a5", "my graph"));
      g.addAnnotation(new Annotation("noise4", "cough",   "noise", "a5",        "a6", "my graph"));
      g.addAnnotation(new Annotation("brown",  "brown",   "word",  "a6",        "a7", "turn1"));
      g.addAnnotation(new Annotation("fox",    "fox",     "word",  "a7",        "a8", "turn1"));
      g.addAnnotation(new Annotation("noise5", "breath",  "noise", "a8",        "a9", "my graph"));
      g.addAnnotation(new Annotation("noise6", "sneeze",  "noise", "a9",        "turnEnd", "my graph"));

      DefaultOffsetGenerator generator = new DefaultOffsetGenerator();
      // generator.setDebug(true);
      try
      {
	 Vector<Change> changes = generator.transform(g);
	 if (generator.getLog() != null) for (String m : generator.getLog()) System.out.println(m);

	 // test the values are what we expected

	 // test the changes are recorded
	 Iterator<Change> order = changes.iterator();
	 assertEquals(new Double(1.0), g.getAnchor("a1").getOffset());
	 assertEquals(new Double(2.0), g.getAnchor("a2").getOffset());
	 assertEquals(new Double(3.0), g.getAnchor("a3").getOffset());
	 assertEquals(new Double(4.0), g.getAnchor("a4").getOffset());
	 assertEquals(new Double(5.0), g.getAnchor("a5").getOffset());
	 assertEquals(new Double(6.0), g.getAnchor("a6").getOffset());
	 assertEquals(new Double(7.0), g.getAnchor("a7").getOffset());
	 assertEquals(new Double(8.0), g.getAnchor("a8").getOffset());
	 assertEquals(new Double(9.0), g.getAnchor("a9").getOffset());

	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a1"), "offset", new Double(1.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a2"), "offset", new Double(2.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a3"), "offset", new Double(3.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a4"), "offset", new Double(4.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a5"), "offset", new Double(5.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a6"), "offset", new Double(6.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a7"), "offset", new Double(7.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a8"), "offset", new Double(8.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a9"), "offset", new Double(9.0)), 
		      order.next());
	 assertEquals("no extra changes to graph", changes.size(), g.getChanges().size());

      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void extraneousLayers() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      g.setCorpus("cc");

      g.addLayer(new Layer("who", "Participants", Constants.ALIGNMENT_NONE, 
			   true, // peers
			   true, // peersOverlap
			   true)); // saturated
      g.addLayer(new Layer("topic", "Topics", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   false)); // saturated
      g.addLayer(new Layer("turn", "Speaker turns", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   false, // saturated
			   "who", // parentId
			   true)); // parentIncludes
      g.addLayer(new Layer("word", "Words", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   false, // saturated
			   "turn", // parentId
			   true)); // parentIncludes
      g.addLayer(new Layer("orthography", "Orthography", Constants.ALIGNMENT_NONE,
			   false, // peers
			   false, // peersOverlap
			   true, // saturated
			   "word", // parentId
			   true)); // parentIncludes
      g.addLayer(new Layer("dependency", "Dependency", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   false, // saturated
			   "word", // parentId
			   false)); // parentIncludes
      g.addLayer(new Layer("phone", "Phone", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   true, // saturated
			   "word", // parentId
			   true)); // parentIncludes

      // topic
      g.addAnchor(new Anchor("topic1Start", null));
      g.addAnchor(new Anchor("topic1End", null));

      // john smith
      g.addAnchor(new Anchor("turn1Start", 0.0)); // turn start
      g.addAnchor(new Anchor("a0", null)); // the
      g.addAnchor(new Anchor("a1", null)); // quick
      g.addAnchor(new Anchor("a2", null)); // brown
      g.addAnchor(new Anchor("a3", null)); // fox
      g.addAnchor(new Anchor("a4", null)); // jumps
      g.addAnchor(new Anchor("a5", null)); // over
      g.addAnchor(new Anchor("a6", null)); // a
      g.addAnchor(new Anchor("a7", null)); // lazy
      g.addAnchor(new Anchor("a8", null)); // dog
      g.addAnchor(new Anchor("a9", null)); // end of dog
      g.addAnchor(new Anchor("turn1End", 9.0)); // turn end

      // jane doe
      g.addAnchor(new Anchor("turn2Start", 6.5)); // turn start
      g.addAnchor(new Anchor("b6", null)); // roses
      g.addAnchor(new Anchor("b7", null)); // are
      g.addAnchor(new Anchor("b8", null)); // red
      g.addAnchor(new Anchor("b9", null)); // violets
      g.addAnchor(new Anchor("b10", null)); // are
      g.addAnchor(new Anchor("b11", null)); // blue & b
      g.addAnchor(new Anchor("b12", null)); // l
      g.addAnchor(new Anchor("b13", null)); // ue
      g.addAnchor(new Anchor("b14", null)); // end of blue & ue
      g.addAnchor(new Anchor("turn2End", 14.5)); // turn end

      // topics
      // one independent
      g.addAnnotation(new Annotation("topic1", "foxes", "topic", "topicStart", "topicEnd", "my graph"));
      // and one that shares anchors with words
      g.addAnnotation(new Annotation("topic2", "dogs", "topic", "a6", "a9", "my graph"));

      // participants
      g.addAnnotation(new Annotation("participant1", "john smith", "who", "turn1Start", "turn2End", "my graph"));
      g.addAnnotation(new Annotation("participant2", "jane doe", "who", "turn1Start", "turn2End", "my graph"));
      
      // turns
      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turn1Start", "turn1End", "participant1"));
      g.addAnnotation(new Annotation("turn2", "jane doe", "turn", "turn2Start", "turn2End", "participant2"));
      
      // words
      g.addAnnotation(new Annotation("the",   "The",   "word", "a0", "a1", "turn1"));
      g.addAnnotation(new Annotation("quick", "quick", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("brown", "brown", "word", "a2", "a3", "turn1"));
      g.addAnnotation(new Annotation("fox",   "fox",   "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("jumps", "jumps", "word", "a4", "a5", "turn1"));
      g.addAnnotation(new Annotation("over",  "over",  "word", "a5", "a6", "turn1"));
      g.addAnnotation(new Annotation("a",     "a",     "word", "a6", "a7", "turn1"));
      g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a7", "a8", "turn1"));
      g.addAnnotation(new Annotation("dog",  "dog.",   "word", "a8", "a9", "turn1"));

      g.addAnnotation(new Annotation("roses",   "Roses",   "word", "b6",  "b7", "turn2"));
      g.addAnnotation(new Annotation("are",     "are",     "word", "b7",  "b8", "turn2"));
      g.addAnnotation(new Annotation("red",     "red,",    "word", "b8",  "b9", "turn2"));
      g.addAnnotation(new Annotation("violets", "violets", "word", "b9",  "b10", "turn2"));
      g.addAnnotation(new Annotation("are2",    "are",     "word", "b10", "b11", "turn2"));
      g.addAnnotation(new Annotation("blue",    "blue.",   "word", "b11", "b14", "turn2"));

      // phones
      g.addAnnotation(new Annotation("b",    "b",   "phone", "b11", "b12", "blue"));
      g.addAnnotation(new Annotation("l",    "l",   "phone", "b12", "b13", "blue"));
      g.addAnnotation(new Annotation("ue",    "u:",   "phone", "b13", "b14", "blue"));

      // orthography
      g.addAnnotation(new Annotation("theOrth",   "the",   "orthography", "a0", "a1", "the"));
      g.addAnnotation(new Annotation("quickOrth", "quick", "orthography", "a1", "a2", "quick"));
      g.addAnnotation(new Annotation("brownOrth", "brown", "orthography", "a2", "a3", "brown"));
      g.addAnnotation(new Annotation("foxOrth",   "fox",   "orthography", "a3", "a4", "fox"));
      g.addAnnotation(new Annotation("jumpsOrth", "jumps", "orthography", "a4", "a5", "jumps"));
      g.addAnnotation(new Annotation("overOrth",  "over",  "orthography", "a5", "a6", "over"));
      g.addAnnotation(new Annotation("aOrth",     "a",     "orthography", "a6", "a7", "a"));
      g.addAnnotation(new Annotation("lazyOrth",  "lazy",  "orthography", "a7", "a8", "lazy"));
      g.addAnnotation(new Annotation("dogOrth",  "dog",    "orthography", "a8", "a9", "dog"));

      g.addAnnotation(new Annotation("rosesOrth",   "Roses",   "orthography", "b6",  "b7", "roses"));
      g.addAnnotation(new Annotation("areOrth",     "are",     "orthography", "b7",  "b8", "are"));
      g.addAnnotation(new Annotation("redOrth",     "red",     "orthography", "b8",  "b9", "red"));
      g.addAnnotation(new Annotation("violetsOrth", "violets", "orthography", "b9",  "b10", "violets"));
      g.addAnnotation(new Annotation("are2Orth",    "are",     "orthography", "b10", "b11", "are2"));
      g.addAnnotation(new Annotation("blueOrth",    "blue",    "orthography", "b11", "b14", "blue"));

      // dependency
      g.addAnnotation(new Annotation("jumpsObj", "OBJ", "dependency", "a8", "a9", "jumps"));
      g.addAnnotation(new Annotation("jumpsSubj",   "SUBJ",   "dependency", "a3", "a4", "jumps"));

      assertEquals("no initial changes to graph: " + g.getChanges(), 0, g.getChanges().size());

      DefaultOffsetGenerator generator = new DefaultOffsetGenerator();
      // generator.setDebug(true);
      try
      {
	 Vector<Change> changes = generator.transform(g);
	 if (generator.getLog() != null) for (String m : generator.getLog()) System.out.println(m);

	 // test the values are what we expected
	 assertEquals(new Double(0.0), g.getAnchor("a0").getOffset());
	 assertEquals(new Double(1.0), g.getAnchor("a1").getOffset());
	 assertEquals(new Double(2.0), g.getAnchor("a2").getOffset());
	 assertEquals(new Double(3.0), g.getAnchor("a3").getOffset());
	 assertEquals(new Double(4.0), g.getAnchor("a4").getOffset());
	 assertEquals(new Double(5.0), g.getAnchor("a5").getOffset());
	 assertEquals(new Double(6.0), g.getAnchor("a6").getOffset());
	 assertEquals(new Double(7.0), g.getAnchor("a7").getOffset());
	 assertEquals(new Double(8.0), g.getAnchor("a8").getOffset());
	 assertEquals(new Double(9.0), g.getAnchor("a9").getOffset());

	 assertEquals(new Double(6.5),  g.getAnchor("b6").getOffset());
	 assertEquals(new Double(7.5),  g.getAnchor("b7").getOffset());
	 assertEquals(new Double(8.5),  g.getAnchor("b8").getOffset());
	 assertEquals(new Double(9.5),  g.getAnchor("b9").getOffset());
	 assertEquals(new Double(10.5), g.getAnchor("b10").getOffset());
	 // word and phone anchors evenly spread amongst each other
	 assertEquals(new Double(11.5), g.getAnchor("b11").getOffset());
	 assertEquals(new Double(12.5), g.getAnchor("b12").getOffset());
	 assertEquals(new Double(13.5), g.getAnchor("b13").getOffset());
	 assertEquals(new Double(14.5), g.getAnchor("b14").getOffset());

	 // topic left unchanged (it's a top-level layer)
	 assertNull(g.getAnchor("topic1Start").getOffset());
	 assertNull(g.getAnchor("topic1End").getOffset());

	 // test the changes are recorded
	 Iterator<Change> order = changes.iterator();

	 // collapsed back to start of turn
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a0"), "offset", new Double(0.0)), 
		      order.next());
	 // collapsed forward to end of turn
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a9"), "offset", new Double(9.0)), 
		      order.next());
	 // then the rest interpolated between
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a1"), "offset", new Double(1.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a2"), "offset", new Double(2.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a3"), "offset", new Double(3.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a4"), "offset", new Double(4.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a5"), "offset", new Double(5.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a6"), "offset", new Double(6.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a7"), "offset", new Double(7.0)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("a8"), "offset", new Double(8.0)), 
		      order.next());

	 // collapsed back to start of turn
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("b6"), "offset", new Double(6.5)), 
		      order.next());
	 // collapsed forward to end of turn
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("b14"), "offset", new Double(14.5)), 
		      order.next());
	 // then the rest interpolated between
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("b7"), "offset", new Double(7.5)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("b8"), "offset", new Double(8.5)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("b9"), "offset", new Double(9.5)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("b10"), "offset", new Double(10.5)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("b11"), "offset", new Double(11.5)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("b12"), "offset", new Double(12.5)), 
		      order.next());
	 assertEquals(new Change(Change.Operation.Update, g.getAnchor("b13"), "offset", new Double(13.5)), 
		      order.next());

	 assertEquals("no extra changes to graph - " + changes + " vs. " +g.getChanges(), 
		      changes.size(), g.getChanges().size());

      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void fragmentWithMissingAnchors() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      g.setCorpus("cc");

      g.addLayer(new Layer("who", "Participants", Constants.ALIGNMENT_NONE, 
			   true, // peers
			   true, // peersOverlap
			   true)); // saturated
      g.addLayer(new Layer("turn", "Speaker turns", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   false, // saturated
			   "who", // parentId
			   true)); // parentIncludes
      g.addLayer(new Layer("utterance", "Utterance", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   true, // saturated
			   "turn", // parentId
			   true)); // parentIncludes
      g.addLayer(new Layer("word", "Words", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   false, // saturated
			   "turn", // parentId
			   true)); // parentIncludes

      g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL)); // turn start

      g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE, Constants.CONFIDENCE_NONE)); // the
      g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // quick
      g.addAnchor(new Anchor("a02", 0.03, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // brown
      g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // fox
      g.addAnchor(new Anchor("a04a", 0.04, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // fox end

      g.addAnchor(new Anchor("utteranceChange", 0.4, Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL)); // utterance boundary

      g.addAnchor(new Anchor("a04b", 2.0, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // jumps
      g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // over
      g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // a
      g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // lazy
      // no confidence set for a44 and a54
      g.addAnchor(new Anchor("a44", 5.1)); // dog
      g.addAnchor(new Anchor("a54", 5.2)); // end of dog

      // no confidence set for a7
      g.addAnchor(new Anchor("turnEnd", 5.4, Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL)); // turn end

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "participant1"));

      g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utteranceChange", "turn1"));
      g.addAnnotation(new Annotation("utterance2", "john smith", "utterance", "utteranceChange", "turnEnd", "turn1"));
      
      g.addAnnotation(new Annotation("the",   "the",   "word", "a0",  "a01", "turn1"));
      g.addAnnotation(new Annotation("quick", "quick", "word", "a01", "a02", "turn1"));
      g.addAnnotation(new Annotation("brown", "brown", "word", "a02",  "a03", "turn1"));
      g.addAnnotation(new Annotation("fox",   "fox",   "word", "a03", "a04a", "turn1"));

      g.addAnnotation(new Annotation("jumps", "jumps", "word", "a04b",  "a14", "turn1"));
      g.addAnnotation(new Annotation("over",  "over",  "word", "a14",  "a24", "turn1"));
      g.addAnnotation(new Annotation("a",     "a",     "word", "a24",  "a34", "turn1"));
      g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a34",  "a44", "turn1"));
      g.addAnnotation(new Annotation("dog",  "dog",    "word", "a44",  "a54", "turn1"));

      Vector<String> fragmentLayers = new Vector<String>();
      fragmentLayers.add("word");
      Graph f = g.getFragment(g.getAnnotation("utterance1"), fragmentLayers);

      DefaultOffsetGenerator generator = new DefaultOffsetGenerator();
      generator.setDefaultAnchorConfidence(Constants.CONFIDENCE_NONE);
      generator.setDefaultOffsetThreshold(Constants.CONFIDENCE_AUTOMATIC);
      // generator.setDebug(true);
      try
      {
	 Vector<Change> changes = generator.transform(f);
	 if (generator.getLog() != null) for (String m : generator.getLog()) System.out.println(m);
	 for (String m : generator.getErrors()) System.out.println("ERROR: " + m);

	 // test the values are what we expected

	 // test the changes are recorded
	 Iterator<Change> order = changes.iterator();
	 assertEquals(new Double(0.0), f.getAnchor("a0").getOffset());
	 assertEquals(new Double(0.1), f.getAnchor("a01").getOffset());
	 assertEquals(new Double(0.2), f.getAnchor("a02").getOffset());
	 // yay for inexact floating point representations!
	 assertEquals(new Double(0.30000000000000004), f.getAnchor("a03").getOffset());
	 assertEquals(new Double(0.4), f.getAnchor("a04a").getOffset());

      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }

      f = g.getFragment(g.getAnnotation("utterance2"), fragmentLayers);

      // generator.setDebug(true);
      try
      {
	 Vector<Change> changes = generator.transform(f);
	 if (generator.getLog() != null) for (String m : generator.getLog()) System.out.println(m);
	 for (String m : generator.getErrors()) System.out.println("ERROR: " + m);

	 // test the values are what we expected
	 assertEquals(new Double(0.4), f.getAnchor("a04b").getOffset());
	 assertEquals(new Double(1.4), f.getAnchor("a14").getOffset());
	 assertEquals(new Double(2.4), f.getAnchor("a24").getOffset());
	 assertEquals(new Double(3.4), f.getAnchor("a34").getOffset());
	 assertEquals(new Double(4.4), f.getAnchor("a44").getOffset());
	 assertEquals(new Double(5.4), f.getAnchor("a54").getOffset());
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.util.test.TestDefaultOffsetGenerator");
   }
}
