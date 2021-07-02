//
// Copyright 2015-2021 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.ag.util;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;
import nzilbb.ag.*;
import nzilbb.ag.util.*;
import nzilbb.util.Timers;

public class TestDefaultOffsetGenerator {

  /** Test all-null offsets are evenly spread through the duration. */
  @Test public void basicInterpolation() {
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
      
    g.addAnnotation(new Annotation("the", "the", "word", "a0", "a1", "turn1", 1));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a1", "a2", "turn1", 2));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a2", "a3", "turn1", 3));
    g.addAnnotation(new Annotation("fox", "fox", "word", "a3", "a4", "turn1", 4));
    g.addAnnotation(new Annotation("jumps", "jumps", "word", "a4", "a5", "turn1", 5));
    g.addAnnotation(new Annotation("over", "over", "word", "a5", "a6", "turn1", 6));
    g.addAnnotation(new Annotation("a", "a", "word", "a6", "a7", "turn1", 7));
    g.addAnnotation(new Annotation("lazy", "lazy", "word", "a7", "a8", "turn1", 8));
    g.addAnnotation(new Annotation("dog", "dog", "word", "a8", "a9", "turn1", 9));

    g.trackChanges();

    DefaultOffsetGenerator generator = new DefaultOffsetGenerator();
    // generator.setDebug(true);
    try {
      generator.transform(g);
      Set<Change> changes = g.getTracker().getChanges();
      if (generator.getLog() != null) for (String m : generator.getLog()) System.out.println(m);

      // test the values are what we expected

      // test the changes are recorded
      assertEquals(Double.valueOf(0.0), g.getAnchor("a0").getOffset());
      assertEquals(Double.valueOf(1.0), g.getAnchor("a1").getOffset());
      assertEquals(Double.valueOf(2.0), g.getAnchor("a2").getOffset());
      assertEquals(Double.valueOf(3.0), g.getAnchor("a3").getOffset());
      assertEquals(Double.valueOf(4.0), g.getAnchor("a4").getOffset());
      assertEquals(Double.valueOf(5.0), g.getAnchor("a5").getOffset());
      assertEquals(Double.valueOf(6.0), g.getAnchor("a6").getOffset());
      assertEquals(Double.valueOf(7.0), g.getAnchor("a7").getOffset());
      assertEquals(Double.valueOf(8.0), g.getAnchor("a8").getOffset());
      assertEquals(Double.valueOf(9.0), g.getAnchor("a9").getOffset());

      Set<String> changeStrings = changes.stream()
        .map(Change::toString).collect(Collectors.toSet());
      // collapsed back to start of turn
      assertTrue(changeStrings.contains("Update a0: offset = 0.0 (was null)"));
      // collapsed forward to end of turn
      assertTrue(changeStrings.contains("Update a9: offset = 9.0 (was null)"));
      // then the rest interpolated between
      assertTrue(changeStrings.contains("Update a1: offset = 1.0 (was null)"));
      assertTrue(changeStrings.contains("Update a2: offset = 2.0 (was null)"));
      assertTrue(changeStrings.contains("Update a3: offset = 3.0 (was null)"));
      assertTrue(changeStrings.contains("Update a4: offset = 4.0 (was null)"));
      assertTrue(changeStrings.contains("Update a5: offset = 5.0 (was null)"));
      assertTrue(changeStrings.contains("Update a6: offset = 6.0 (was null)"));
      assertTrue(changeStrings.contains("Update a7: offset = 7.0 (was null)"));
      assertTrue(changeStrings.contains("Update a8: offset = 8.0 (was null)"));
      assertEquals("no extra changes to graph: " + g.getChanges(),
                   changes.size(), g.getChanges().size());
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Test anchors with offsets of low confidence are evenly spread through the duration. */
  @Test public void basicInterpolationWithConfidence() {
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
    g.getAnchor("turnStart").setConfidence(Constants.CONFIDENCE_MANUAL);
    g.addAnchor(new Anchor("a0", 0.1)); // the
    g.getAnchor("a0").setConfidence(Constants.CONFIDENCE_NONE);
    g.addAnchor(new Anchor("a05", 0.2)); // quick
    g.getAnchor("a05").setConfidence(Constants.CONFIDENCE_DEFAULT);
    g.addAnchor(new Anchor("a1", 1.3)); // brown
    g.getAnchor("a1").setConfidence(Constants.CONFIDENCE_DEFAULT);
    g.addAnchor(new Anchor("a15", 1.4)); // fox
    g.getAnchor("a15").setConfidence(Constants.CONFIDENCE_DEFAULT);
    g.addAnchor(new Anchor("a2", 2.0)); // jumps
    g.getAnchor("a2").setConfidence(Constants.CONFIDENCE_MANUAL);
    g.addAnchor(new Anchor("a3", 3.3)); // over
    g.getAnchor("a3").setConfidence(Constants.CONFIDENCE_AUTOMATIC);
    g.addAnchor(new Anchor("a4", 4.4)); // a
    g.getAnchor("a4").setConfidence(Constants.CONFIDENCE_AUTOMATIC);
    g.addAnchor(new Anchor("a5", 5.5)); // lazy
    g.getAnchor("a5").setConfidence(Constants.CONFIDENCE_AUTOMATIC);
    g.addAnchor(new Anchor("a6", 6.6)); // dog
    // no confidence set for a6
    g.addAnchor(new Anchor("a7", null)); // end of dog
    // no confidence set for a7
    g.addAnchor(new Anchor("turnEnd", 7.0)); // turn end
    g.getAnchor("turnEnd").setConfidence(Constants.CONFIDENCE_MANUAL);

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

    g.trackChanges();
    
    DefaultOffsetGenerator generator = new DefaultOffsetGenerator();
    generator.setDefaultAnchorConfidence(Constants.CONFIDENCE_NONE);
    generator.setDefaultOffsetThreshold(Constants.CONFIDENCE_AUTOMATIC);
    // generator.setDebug(true);
    try {
      generator.transform(g);
      Set<Change> changes = g.getTracker().getChanges();
      if (generator.getLog() != null) for (String m : generator.getLog()) System.out.println(m);

      // test the values are what we expected

      // test the changes are recorded
      Set<String> changeStrings = changes.stream()
        .map(Change::toString).collect(Collectors.toSet());
      assertEquals(Double.valueOf(0.0), g.getAnchor("a0").getOffset());
      assertEquals(Double.valueOf(0.5), g.getAnchor("a05").getOffset());
      assertEquals(Double.valueOf(1.0), g.getAnchor("a1").getOffset());
      assertEquals(Double.valueOf(1.5), g.getAnchor("a15").getOffset());
      assertEquals(Double.valueOf(2.0), g.getAnchor("a2").getOffset());
      assertEquals(Double.valueOf(3.0), g.getAnchor("a3").getOffset());
      assertEquals(Double.valueOf(4.0), g.getAnchor("a4").getOffset());
      assertEquals(Double.valueOf(5.0), g.getAnchor("a5").getOffset());
      assertEquals(Double.valueOf(6.0), g.getAnchor("a6").getOffset());
      assertEquals(Double.valueOf(7.0), g.getAnchor("a7").getOffset());

      // collapsed back to start of turn
      assertTrue(changeStrings.contains("Update a0: offset = 0.0 (was 0.1)"));
      // then the rest interpolated between
      assertTrue(changeStrings.contains("Update a05: offset = 0.5 (was 0.2)"));
      assertTrue(changeStrings.contains("Update a1: offset = 1.0 (was 1.3)"));
      assertTrue(changeStrings.contains("Update a15: offset = 1.5 (was 1.4)"));
      // a2 not changed
      // collapsed forward to end of span
      assertTrue(changeStrings.contains("Update a7: offset = 7.0 (was null)"));
      assertTrue(changeStrings.contains("Update a3: offset = 3.0 (was 3.3)"));
      assertTrue(changeStrings.contains("Update a4: offset = 4.0 (was 4.4)"));
      assertTrue(changeStrings.contains("Update a5: offset = 5.0 (was 5.5)"));
      assertTrue(changeStrings.contains("Update a6: offset = 6.0 (was 6.6)"));
      assertEquals("no extra changes to graph", changes.size(), g.getChanges().size());

    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Utterances partition boundaries of interpolation. */
  @Test public void utterancesPartitionWordsInTurn() {
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

    g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start

    g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE_NONE)); // the
    g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE_DEFAULT)); // quick
    g.addAnchor(new Anchor("a02", 0.03, Constants.CONFIDENCE_DEFAULT)); // brown
    g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox
    g.addAnchor(new Anchor("a04a", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox end

    g.addAnchor(new Anchor("utteranceChange", 0.4, Constants.CONFIDENCE_MANUAL)); // utterance boundary

    g.addAnchor(new Anchor("a04b", 2.0, Constants.CONFIDENCE_AUTOMATIC)); // jumps
    g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE_AUTOMATIC)); // over
    g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE_AUTOMATIC)); // a
    g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE_AUTOMATIC)); // lazy
    // no confidence set for a44 and a54
    g.addAnchor(new Anchor("a44", 5.1)); // dog
    g.addAnchor(new Anchor("a54", null)); // end of dog

    // no confidence set for a7
    g.addAnchor(new Anchor("turnEnd", 5.4, Constants.CONFIDENCE_MANUAL)); // turn end

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

    g.trackChanges();
    
    DefaultOffsetGenerator generator = new DefaultOffsetGenerator();
    generator.setDefaultAnchorConfidence(Constants.CONFIDENCE_NONE);
    generator.setDefaultOffsetThreshold(Constants.CONFIDENCE_AUTOMATIC);
    // generator.setDebug(true);
    try {
      generator.transform(g);
      if (generator.getLog() != null) for (String m : generator.getLog()) System.out.println(m);
      Set<Change> changes = g.getTracker().getChanges();

      // test the values are what we expected

      // test the changes are recorded
      Set<String> changeStrings = changes.stream()
        .map(Change::toString).collect(Collectors.toSet());
      assertEquals(Double.valueOf(0.0), g.getAnchor("a0").getOffset());
      assertEquals(Double.valueOf(0.1), g.getAnchor("a01").getOffset());
      assertEquals(Double.valueOf(0.2), g.getAnchor("a02").getOffset());
      // yay for inexact floating point representations!
      assertEquals(Double.valueOf(0.30000000000000004), g.getAnchor("a03").getOffset());
      assertEquals(Double.valueOf(0.4), g.getAnchor("a04a").getOffset());
      assertEquals(Double.valueOf(0.4), g.getAnchor("a04b").getOffset());
      assertEquals(Double.valueOf(1.4), g.getAnchor("a14").getOffset());
      assertEquals(Double.valueOf(2.4), g.getAnchor("a24").getOffset());
      assertEquals(Double.valueOf(3.4), g.getAnchor("a34").getOffset());
      assertEquals(Double.valueOf(4.4), g.getAnchor("a44").getOffset());
      assertEquals(Double.valueOf(5.4), g.getAnchor("a54").getOffset());

      // collapsed back to start of turn
      assertTrue(changeStrings.contains("Update a0: offset = 0.0 (was 0.01)"));
      // collapsed forward to end of utterancew 
      assertTrue(changeStrings.contains("Update a04a: offset = 0.4 (was 0.04)"));
      // then the rest interpolated between
      assertTrue(changeStrings.contains("Update a01: offset = 0.1 (was 0.02)"));
      assertTrue(changeStrings.contains("Update a02: offset = 0.2 (was 0.03)"));
      assertTrue(changeStrings.contains("Update a03: offset = 0.30000000000000004 (was 0.04)"));
      // collapsed back to start of utterance
      assertTrue(changeStrings.contains("Update a04b: offset = 0.4 (was 2.0)"));
      // collapsed forward to end of utterance
      assertTrue(changeStrings.contains("Update a54: offset = 5.4 (was null)"));
      // then the rest interpolated between
      assertTrue(changeStrings.contains("Update a14: offset = 1.4 (was 3.3)"));
      assertTrue(changeStrings.contains("Update a24: offset = 2.4 (was 4.4)"));
      assertTrue(changeStrings.contains("Update a34: offset = 3.4 (was 5.0)"));
      assertTrue(changeStrings.contains("Update a44: offset = 4.4 (was 5.1)"));
      assertEquals("no extra changes to graph", changes.size(), g.getChanges().size());

    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Test words uttered by different speakers are interpolated independently. */
  @Test public void overlappingInterpolation() {
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

    g.trackChanges();

    DefaultOffsetGenerator generator = new DefaultOffsetGenerator();
    // generator.setDebug(true);
    try {
      generator.transform(g);
      if (generator.getLog() != null) for (String m : generator.getLog()) System.out.println(m);
      Set<Change> changes = g.getTracker().getChanges();

      // test the values are what we expected
      assertEquals(Double.valueOf(0.0), g.getAnchor("a0").getOffset());
      assertEquals(Double.valueOf(1.0), g.getAnchor("a1").getOffset());
      assertEquals(Double.valueOf(2.0), g.getAnchor("a2").getOffset());
      assertEquals(Double.valueOf(3.0), g.getAnchor("a3").getOffset());
      assertEquals(Double.valueOf(4.0), g.getAnchor("a4").getOffset());
      assertEquals(Double.valueOf(5.0), g.getAnchor("a5").getOffset());
      assertEquals(Double.valueOf(6.0), g.getAnchor("a6").getOffset());
      assertEquals(Double.valueOf(7.0), g.getAnchor("a7").getOffset());
      assertEquals(Double.valueOf(8.0), g.getAnchor("a8").getOffset());
      assertEquals(Double.valueOf(9.0), g.getAnchor("a9").getOffset());

      assertEquals(Double.valueOf(6.5),  g.getAnchor("b6").getOffset());
      assertEquals(Double.valueOf(7.5),  g.getAnchor("b7").getOffset());
      assertEquals(Double.valueOf(8.5),  g.getAnchor("b8").getOffset());
      assertEquals(Double.valueOf(9.5),  g.getAnchor("b9").getOffset());
      assertEquals(Double.valueOf(10.5), g.getAnchor("b10").getOffset());
      assertEquals(Double.valueOf(11.5), g.getAnchor("b11").getOffset());
      assertEquals(Double.valueOf(12.5), g.getAnchor("b12").getOffset());

      // test the changes are recorded
      Set<String> changeStrings = changes.stream()
        .map(Change::toString).collect(Collectors.toSet());

      // collapsed back to start of turn
      assertTrue(changeStrings.contains("Update a0: offset = 0.0 (was null)"));
      // collapsed forward to end of turn
      assertTrue(changeStrings.contains("Update a9: offset = 9.0 (was null)"));
      // then the rest interpolated between
      assertTrue(changeStrings.contains("Update a1: offset = 1.0 (was null)"));
      assertTrue(changeStrings.contains("Update a2: offset = 2.0 (was null)"));
      assertTrue(changeStrings.contains("Update a3: offset = 3.0 (was null)"));
      assertTrue(changeStrings.contains("Update a4: offset = 4.0 (was null)"));
      assertTrue(changeStrings.contains("Update a5: offset = 5.0 (was null)"));
      assertTrue(changeStrings.contains("Update a6: offset = 6.0 (was null)"));
      assertTrue(changeStrings.contains("Update a7: offset = 7.0 (was null)"));
      assertTrue(changeStrings.contains("Update a8: offset = 8.0 (was null)"));

      // collapsed back to start of turn
      assertTrue(changeStrings.contains("Update b6: offset = 6.5 (was null)"));
      // collapsed forward to end of turn
      assertTrue(changeStrings.contains("Update b12: offset = 12.5 (was null)"));
      // then the rest interpolated between
      assertTrue(changeStrings.contains("Update b7: offset = 7.5 (was null)"));
      assertTrue(changeStrings.contains("Update b8: offset = 8.5 (was null)"));
      assertTrue(changeStrings.contains("Update b9: offset = 9.5 (was null)"));
      assertTrue(changeStrings.contains("Update b10: offset = 10.5 (was null)"));
      assertTrue(changeStrings.contains("Update b11: offset = 11.5 (was null)"));

      assertEquals("no extra changes to graph", changes.size(), g.getChanges().size());
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Interpolation works across linked annotations from other layers. */
  @Test public void padForAnnotationsOnOtherLayers() {
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

    g.trackChanges();

    DefaultOffsetGenerator generator = new DefaultOffsetGenerator();
    // generator.setDebug(true);
    try {
      generator.transform(g);
      if (generator.getLog() != null) for (String m : generator.getLog()) System.out.println(m);
      Set<Change> changes = g.getTracker().getChanges();

      // test the values are what we expected

      // test the changes are recorded
      Set<String> changeStrings = changes.stream()
        .map(Change::toString).collect(Collectors.toSet());
      assertEquals(Double.valueOf(1.0), g.getAnchor("a1").getOffset());
      assertEquals(Double.valueOf(2.0), g.getAnchor("a2").getOffset());
      assertEquals(Double.valueOf(3.0), g.getAnchor("a3").getOffset());
      assertEquals(Double.valueOf(4.0), g.getAnchor("a4").getOffset());
      assertEquals(Double.valueOf(5.0), g.getAnchor("a5").getOffset());
      assertEquals(Double.valueOf(6.0), g.getAnchor("a6").getOffset());
      assertEquals(Double.valueOf(7.0), g.getAnchor("a7").getOffset());
      assertEquals(Double.valueOf(8.0), g.getAnchor("a8").getOffset());
      assertEquals(Double.valueOf(9.0), g.getAnchor("a9").getOffset());

      assertTrue(changeStrings.contains("Update a1: offset = 1.0 (was null)"));
      assertTrue(changeStrings.contains("Update a2: offset = 2.0 (was null)"));
      assertTrue(changeStrings.contains("Update a3: offset = 3.0 (was null)"));
      assertTrue(changeStrings.contains("Update a4: offset = 4.0 (was null)"));
      assertTrue(changeStrings.contains("Update a5: offset = 5.0 (was null)"));
      assertTrue(changeStrings.contains("Update a6: offset = 6.0 (was null)"));
      assertTrue(changeStrings.contains("Update a7: offset = 7.0 (was null)"));
      assertTrue(changeStrings.contains("Update a8: offset = 8.0 (was null)"));
      assertTrue(changeStrings.contains("Update a9: offset = 9.0 (was null)"));
      assertEquals("no extra changes to graph", changes.size(), g.getChanges().size());

    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Test independent annotations from other layers don't interfere with interpolation. */
  @Test public void extraneousLayers() {
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

    g.trackChanges();

    DefaultOffsetGenerator generator = new DefaultOffsetGenerator();
    // generator.setDebug(true);
    try {
      generator.transform(g);
      if (generator.getLog() != null) for (String m : generator.getLog()) System.out.println(m);
      Set<Change> changes = g.getTracker().getChanges();

      // test the values are what we expected
      assertEquals(Double.valueOf(0.0), g.getAnchor("a0").getOffset());
      assertEquals(Double.valueOf(1.0), g.getAnchor("a1").getOffset());
      assertEquals(Double.valueOf(2.0), g.getAnchor("a2").getOffset());
      assertEquals(Double.valueOf(3.0), g.getAnchor("a3").getOffset());
      assertEquals(Double.valueOf(4.0), g.getAnchor("a4").getOffset());
      assertEquals(Double.valueOf(5.0), g.getAnchor("a5").getOffset());
      assertEquals(Double.valueOf(6.0), g.getAnchor("a6").getOffset());
      assertEquals(Double.valueOf(7.0), g.getAnchor("a7").getOffset());
      assertEquals(Double.valueOf(8.0), g.getAnchor("a8").getOffset());
      assertEquals(Double.valueOf(9.0), g.getAnchor("a9").getOffset());

      assertEquals(Double.valueOf(6.5),  g.getAnchor("b6").getOffset());
      assertEquals(Double.valueOf(7.5),  g.getAnchor("b7").getOffset());
      assertEquals(Double.valueOf(8.5),  g.getAnchor("b8").getOffset());
      assertEquals(Double.valueOf(9.5),  g.getAnchor("b9").getOffset());
      assertEquals(Double.valueOf(10.5), g.getAnchor("b10").getOffset());
      // word and phone anchors evenly spread amongst each other
      assertEquals(Double.valueOf(11.5), g.getAnchor("b11").getOffset());
      assertEquals(Double.valueOf(12.5), g.getAnchor("b12").getOffset());
      assertEquals(Double.valueOf(13.5), g.getAnchor("b13").getOffset());
      assertEquals(Double.valueOf(14.5), g.getAnchor("b14").getOffset());

      // topic left unchanged (it's a top-level layer)
      assertNull(g.getAnchor("topic1Start").getOffset());
      assertNull(g.getAnchor("topic1End").getOffset());

      // test the changes are recorded
      Set<String> changeStrings = changes.stream()
        .map(Change::toString).collect(Collectors.toSet());

      // collapsed back to start of turn
      assertTrue(changeStrings.contains("Update a0: offset = 0.0 (was null)"));
      // collapsed forward to end of turn
      assertTrue(changeStrings.contains("Update a9: offset = 9.0 (was null)"));
      // then the rest interpolated between
      assertTrue(changeStrings.contains("Update a1: offset = 1.0 (was null)"));
      assertTrue(changeStrings.contains("Update a2: offset = 2.0 (was null)"));
      assertTrue(changeStrings.contains("Update a3: offset = 3.0 (was null)"));
      assertTrue(changeStrings.contains("Update a4: offset = 4.0 (was null)"));
      assertTrue(changeStrings.contains("Update a5: offset = 5.0 (was null)"));
      assertTrue(changeStrings.contains("Update a6: offset = 6.0 (was null)"));
      assertTrue(changeStrings.contains("Update a7: offset = 7.0 (was null)"));
      assertTrue(changeStrings.contains("Update a8: offset = 8.0 (was null)"));

      // collapsed back to start of turn
      assertTrue(changeStrings.contains("Update b6: offset = 6.5 (was null)"));
      // collapsed forward to end of turn
      assertTrue(changeStrings.contains("Update b14: offset = 14.5 (was null)"));
      // then the rest interpolated between
      assertTrue(changeStrings.contains("Update b7: offset = 7.5 (was null)"));
      assertTrue(changeStrings.contains("Update b8: offset = 8.5 (was null)"));
      assertTrue(changeStrings.contains("Update b9: offset = 9.5 (was null)"));
      assertTrue(changeStrings.contains("Update b10: offset = 10.5 (was null)"));
      assertTrue(changeStrings.contains("Update b11: offset = 11.5 (was null)"));
      assertTrue(changeStrings.contains("Update b12: offset = 12.5 (was null)"));
      assertTrue(changeStrings.contains("Update b13: offset = 13.5 (was null)"));

      assertEquals("no extra changes to graph - " + changes + " vs. " +g.getChanges(), 
                   changes.size(), g.getChanges().size());

    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Fragments extracted from larger graphs can have annotations with anchors that are
   * not in the fragment; test that they're ignored. */
  @Test public void fragmentWithMissingAnchors() {
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

    g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start

    g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE_NONE)); // the
    g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE_DEFAULT)); // quick
    g.addAnchor(new Anchor("a02", 0.03, Constants.CONFIDENCE_DEFAULT)); // brown
    g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox
    g.addAnchor(new Anchor("a04a", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox end

    g.addAnchor(new Anchor("utteranceChange", 0.4, Constants.CONFIDENCE_MANUAL)); // utterance boundary

    g.addAnchor(new Anchor("a04b", 2.0, Constants.CONFIDENCE_AUTOMATIC)); // jumps
    g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE_AUTOMATIC)); // over
    g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE_AUTOMATIC)); // a
    g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE_AUTOMATIC)); // lazy
    // no confidence set for a44 and a54
    g.addAnchor(new Anchor("a44", 5.1)); // dog
    g.addAnchor(new Anchor("a54", 5.2)); // end of dog

    // no confidence set for a7
    g.addAnchor(new Anchor("turnEnd", 5.4, Constants.CONFIDENCE_MANUAL)); // turn end

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
    fragmentLayers.add("utterance");
    Graph f = g.getFragment(g.getAnnotation("utterance1"), fragmentLayers.toArray(new String[0]));

    f.trackChanges();

    DefaultOffsetGenerator generator = new DefaultOffsetGenerator();
    generator.setDefaultAnchorConfidence(Constants.CONFIDENCE_NONE);
    generator.setDefaultOffsetThreshold(Constants.CONFIDENCE_AUTOMATIC);
    // generator.setDebug(true);
    try {
      generator.transform(f);
      Set<Change> changes = f.getTracker().getChanges();
      if (generator.getLog() != null) for (String m : generator.getLog()) System.out.println(m);
      for (String m : generator.getErrors()) System.out.println("ERROR: " + m);

      // test the values are what we expected

      // test the changes are recorded
      Set<String> changeStrings = changes.stream()
        .map(Change::toString).collect(Collectors.toSet());
      assertEquals(Double.valueOf(0.0), f.getAnchor("a0").getOffset());
      assertEquals(Double.valueOf(0.1), f.getAnchor("a01").getOffset());
      assertEquals(Double.valueOf(0.2), f.getAnchor("a02").getOffset());
      // yay for inexact floating point representations!
      assertEquals(Double.valueOf(0.30000000000000004), f.getAnchor("a03").getOffset());
      assertEquals(Double.valueOf(0.4), f.getAnchor("a04a").getOffset());

    } catch(TransformationException exception) {
      fail(exception.toString());
    }

    f = g.getFragment(g.getAnnotation("utterance2"), fragmentLayers.toArray(new String[0]));

    // generator.setDebug(true);
    try {
      generator.transform(f);
      if (generator.getLog() != null) for (String m : generator.getLog()) System.out.println(m);
      for (String m : generator.getErrors()) System.out.println("ERROR: " + m);

      // test the values are what we expected
      assertEquals(Double.valueOf(0.4), f.getAnchor("a04b").getOffset());
      assertEquals(Double.valueOf(1.4), f.getAnchor("a14").getOffset());
      assertEquals(Double.valueOf(2.4), f.getAnchor("a24").getOffset());
      assertEquals(Double.valueOf(3.4), f.getAnchor("a34").getOffset());
      assertEquals(Double.valueOf(4.4), f.getAnchor("a44").getOffset());
      assertEquals(Double.valueOf(5.4), f.getAnchor("a54").getOffset());
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Test partial alignment correction scenario, where only one anchor among
   * default-aligned anchors is manually aligned.  */
  @Test public void partialAlignmentOfUnalignedFragment() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");

    g.addLayer(new Layer("who", "Participants")
               .setAlignment(Constants.ALIGNMENT_NONE) 
               .setPeers(true)
               .setPeersOverlap(true)
               .setSaturated(true));
    g.addLayer(new Layer("turn", "Speaker turns")
               .setAlignment(Constants.ALIGNMENT_INTERVAL)
               .setPeers(true)
               .setPeersOverlap(false)
               .setSaturated(false)
               .setParentId("who")
               .setParentIncludes(true));
    g.addLayer(new Layer("utterance", "Utterance")
               .setAlignment(Constants.ALIGNMENT_INTERVAL)
               .setPeers(true)
               .setPeersOverlap(false)
               .setSaturated(true)
               .setParentId("turn")
               .setParentIncludes(true));
    g.addLayer(new Layer("word", "Words")
               .setAlignment(Constants.ALIGNMENT_INTERVAL)
               .setPeers(true)
               .setPeersOverlap(false)
               .setSaturated(false)
               .setParentId("turn")
               .setParentIncludes(true));

    g.addAnchor(new Anchor("utteranceStart", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start
    g.addAnchor(new Anchor("a0", 0.0, Constants.CONFIDENCE_DEFAULT)); // the
    g.addAnchor(new Anchor("a1", 1.0, Constants.CONFIDENCE_MANUAL)); // quick
    g.addAnchor(new Anchor("a2", 2.5, Constants.CONFIDENCE_DEFAULT)); // brown
    g.addAnchor(new Anchor("a3", 3.5, Constants.CONFIDENCE_DEFAULT)); // fox
    g.addAnchor(new Anchor("a4", 4.5, Constants.CONFIDENCE_DEFAULT)); // jumps
    g.addAnchor(new Anchor("a5", 5.5, Constants.CONFIDENCE_DEFAULT)); // over
    g.addAnchor(new Anchor("a6", 6.5, Constants.CONFIDENCE_DEFAULT)); // a
    g.addAnchor(new Anchor("a7", 7.5, Constants.CONFIDENCE_DEFAULT)); // lazy
    g.addAnchor(new Anchor("a8", 8.5, Constants.CONFIDENCE_DEFAULT)); // dog
    g.addAnchor(new Anchor("a9", 9.0, Constants.CONFIDENCE_DEFAULT)); // end of dog
    g.addAnchor(new Anchor("utteranceEnd", 9.0, Constants.CONFIDENCE_MANUAL)); // turn end

    g.addAnnotation(
      new Annotation("participant1", "john smith", "who", "utteranceStart", "utteranceEnd",
                     "my graph"));
      
    g.addAnnotation( // null anchors like a fragment
      new Annotation("turn1", "john smith", "turn", "none1", "none2", "participant1"));
    
    g.addAnnotation(
      new Annotation("utterance1", "john smith", "utterance", "utteranceStart", "utteranceEnd",
                     "turn1"));
      
    g.addAnnotation(new Annotation("the", "the", "word", "a0", "a1", "turn1", 1));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a1", "a2", "turn1", 2));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a2", "a3", "turn1", 3));
    g.addAnnotation(new Annotation("fox", "fox", "word", "a3", "a4", "turn1", 4));
    g.addAnnotation(new Annotation("jumps", "jumps", "word", "a4", "a5", "turn1", 5));
    g.addAnnotation(new Annotation("over", "over", "word", "a5", "a6", "turn1", 6));
    g.addAnnotation(new Annotation("a", "a", "word", "a6", "a7", "turn1", 7));
    g.addAnnotation(new Annotation("lazy", "lazy", "word", "a7", "a8", "turn1", 8));
    g.addAnnotation(new Annotation("dog", "dog", "word", "a8", "a9", "turn1", 9));

    g.trackChanges();

    DefaultOffsetGenerator generator = new DefaultOffsetGenerator();
    // generator.setDebug(true);
    try {
      generator.transform(g);
      if (generator.getLog() != null) for (String m : generator.getLog()) System.out.println(m);
      Set<Change> changes = g.getTracker().getChanges();

      // test the values are what we expected

      Set<String> changeStrings = changes.stream()
        .map(Change::toString).collect(Collectors.toSet());

      assertEquals("Anchor offset: " + changeStrings,
                   Double.valueOf(0.0), g.getAnchor("a0").getOffset());
      assertEquals("Anchor offset: " + changeStrings,
                   Double.valueOf(1.0), g.getAnchor("a1").getOffset());
      assertEquals("Anchor offset: " + changeStrings,
                   Double.valueOf(2.0), g.getAnchor("a2").getOffset());
      assertEquals("Anchor offset: " + changeStrings,
                   Double.valueOf(3.0), g.getAnchor("a3").getOffset());
      assertEquals("Anchor offset: " + changeStrings,
                   Double.valueOf(4.0), g.getAnchor("a4").getOffset());
      assertEquals("Anchor offset: " + changeStrings,
                   Double.valueOf(5.0), g.getAnchor("a5").getOffset());
      assertEquals("Anchor offset: " + changeStrings,
                   Double.valueOf(6.0), g.getAnchor("a6").getOffset());
      assertEquals("Anchor offset: " + changeStrings,
                   Double.valueOf(7.0), g.getAnchor("a7").getOffset());
      assertEquals("Anchor offset: " + changeStrings,
                   Double.valueOf(8.0), g.getAnchor("a8").getOffset());
      assertEquals("Anchor offset: " + changeStrings,
                   Double.valueOf(9.0), g.getAnchor("a9").getOffset());

      // test the changes are recorded
      assertFalse("First default anchor unchanged",
                  changeStrings.contains("Update a0: offset = 0.0 (was null)"));
      assertFalse("Last default anchor unchanged",
                  changeStrings.contains("Update a9: offset = 9.0 (was null)"));
      assertFalse("Manual alignment unchanged",
                  changeStrings.contains("Update a1: offset = 1.0 (was null)"));
      
      // then the rest interpolated between
      assertTrue("Default anchor shifted: " + changeStrings,
                 changeStrings.contains("Update a2: offset = 2.0 (was 2.5)"));
      assertTrue("Default anchor shifted: " + changeStrings,
                 changeStrings.contains("Update a3: offset = 3.0 (was 3.5)"));
      assertTrue("Default anchor shifted: " + changeStrings,
                 changeStrings.contains("Update a4: offset = 4.0 (was 4.5)"));
      assertTrue("Default anchor shifted: " + changeStrings,
                 changeStrings.contains("Update a5: offset = 5.0 (was 5.5)"));
      assertTrue("Default anchor shifted: " + changeStrings,
                 changeStrings.contains("Update a6: offset = 6.0 (was 6.5)"));
      assertTrue("Default anchor shifted: " + changeStrings,
                 changeStrings.contains("Update a7: offset = 7.0 (was 7.5)"));
      assertTrue("Default anchor shifted: " + changeStrings,
                 changeStrings.contains("Update a8: offset = 8.0 (was 8.5)"));
      
      assertEquals("no extra changes to graph: " + g.getChanges(),
                   changes.size(), g.getChanges().size());
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Ensure chained sub-word annotations (e.g. grammatical words within orthographic
   * words) get offsets. */
  @Test public void chainedSubWordAnnotations() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");
    
    g.addLayer(new Layer("who", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
               .setPeers(true).setPeersOverlap(true).setSaturated(true));
    g.addLayer(new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
               .setPeers(true).setPeersOverlap(false).setSaturated(false)
               .setParentId("who").setParentIncludes(true));
    g.addLayer(new Layer("word", "Orthographic Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
               .setPeers(true).setPeersOverlap(false).setSaturated(false)
               .setParentId("turn").setParentIncludes(true));
    g.addLayer(new Layer("gram", "Grammatical Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
               .setPeers(true).setPeersOverlap(true).setSaturated(true)
               .setParentId("word").setParentIncludes(true));

    // john smith
    g.addAnchor(new Anchor("turn1Start", 0.0)); // turn start
    g.addAnchor(new Anchor("a0", 0.0)); // I
    g.addAnchor(new Anchor("a05", null)); // 'm
    g.addAnchor(new Anchor("a1", 1.0)); // wanna - alternatively "want to" or "want a"
    g.addAnchor(new Anchor("a13", null)); // to
    g.addAnchor(new Anchor("a16", null)); // a
    g.addAnchor(new Anchor("a2", 2.0)); // jolly
    g.addAnchor(new Anchor("a3", 3.0)); // well
    g.addAnchor(new Anchor("a4", 4.0)); // jump
    g.addAnchor(new Anchor("a5", 5.0)); // over
    g.addAnchor(new Anchor("a6", 6.0)); // a
    g.addAnchor(new Anchor("a7", 7.0)); // lazy
    g.addAnchor(new Anchor("a8", 8.0)); // dog
    g.addAnchor(new Anchor("a9", 9.0)); // end of dog
    g.addAnchor(new Anchor("turn1End", 9.0)); // turn end

    // participants
    g.addAnnotation(
      new Annotation("participant1", "john smith", "who", "turn1Start", "turn1End", "my graph"));
      
    // turns
    g.addAnnotation(
      new Annotation("turn1", "john smith", "turn", "turn1Start", "turn1End", "participant1"));
      
    // words
    // "I'm" splits into "I am"
    g.addAnnotation(new Annotation("I'm",   "I'm",   "word", "a0", "a1", "turn1"));
    // "wanna" splits two possible ways "want to" and "want a"
    g.addAnnotation(new Annotation("wanna", "wanna", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("jolly", "jolly", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("well",  "well",   "word", "a3", "a4", "turn1"));
    g.addAnnotation(new Annotation("jump",  "jump", "word", "a4", "a5", "turn1"));
    g.addAnnotation(new Annotation("over",  "over",  "word", "a5", "a6", "turn1"));
    g.addAnnotation(new Annotation("a",     "a",     "word", "a6", "a7", "turn1"));
    g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a7", "a8", "turn1"));
    g.addAnnotation(new Annotation("dog",   "dog.",   "word", "a8", "a9", "turn1"));

    // grammatical words
    g.addAnnotation(new Annotation("gram_I",   "I",   "gram", "a0", "a05", "I'm"));
    g.addAnnotation(new Annotation("gram_'m",   "am",   "gram", "a05", "a1", "I'm"));

    // analysis 1 "want to"
    g.addAnnotation(new Annotation("gram_want1", "want", "gram", "a1", "a13", "wanna"));
    g.addAnnotation(new Annotation("gram_to1", "to", "gram", "a13", "a2", "wanna"));
    
    // analysis 2 "want a"
    g.addAnnotation(new Annotation("gram_want2", "want", "gram", "a1", "a16", "wanna"));
    g.addAnnotation(new Annotation("gram_a2", "a", "gram", "a16", "a2", "wanna"));
    
    g.addAnnotation(new Annotation("gram_jolly", "jolly", "gram", "a2", "a3", "jolly"));
    g.addAnnotation(new Annotation("gram_well",  "well",   "gram", "a3", "a4", "well"));
    g.addAnnotation(new Annotation("gram_jump",  "jump", "gram", "a4", "a5", "jump"));
    g.addAnnotation(new Annotation("gram_over",  "over",  "gram", "a5", "a6", "over"));
    g.addAnnotation(new Annotation("gram_a",     "a",     "gram", "a6", "a7", "a"));
    g.addAnnotation(new Annotation("gram_lazy",  "lazy",  "gram", "a7", "a8", "lazy"));
    g.addAnnotation(new Annotation("gram_dog",   "dog.",   "gram", "a8", "a9", "dog"));

    assertEquals("no initial changes to graph: " + g.getChanges(), 0, g.getChanges().size());

    g.trackChanges();

    // grammatical words are not anchored yet
    assertNull("Grammatical word offset unset - I'm",
               g.getAnchor("a05").getOffset());
    assertNull("Grammatical word offset unset - wanna 1",
               g.getAnchor("a13").getOffset());
    assertNull("Grammatical word offset unset - wanna 2",
               g.getAnchor("a16").getOffset());
    DefaultOffsetGenerator generator = new DefaultOffsetGenerator();
    // generator.setDebug(true);
    try {
      generator.transform(g);
      if (generator.getLog() != null) for (String m : generator.getLog()) System.out.println(m);
      Set<Change> changes = g.getTracker().getChanges();

      // orthographic words are unchanged
      assertEquals("Word anchors unchanged", Double.valueOf(0.0), g.getAnchor("a0").getOffset());
      assertEquals("Word anchors unchanged", Double.valueOf(1.0), g.getAnchor("a1").getOffset());
      assertEquals("Word anchors unchanged", Double.valueOf(2.0), g.getAnchor("a2").getOffset());
      assertEquals("Word anchors unchanged", Double.valueOf(3.0), g.getAnchor("a3").getOffset());
      assertEquals("Word anchors unchanged", Double.valueOf(4.0), g.getAnchor("a4").getOffset());
      assertEquals("Word anchors unchanged", Double.valueOf(5.0), g.getAnchor("a5").getOffset());
      assertEquals("Word anchors unchanged", Double.valueOf(6.0), g.getAnchor("a6").getOffset());
      assertEquals("Word anchors unchanged", Double.valueOf(7.0), g.getAnchor("a7").getOffset());
      assertEquals("Word anchors unchanged", Double.valueOf(8.0), g.getAnchor("a8").getOffset());
      assertEquals("Word anchors unchanged", Double.valueOf(9.0), g.getAnchor("a9").getOffset());

      // grammatical words are anchored

      // I + 'm anchors at the mid point
      assertEquals("Grammatical word offset set - I'm",
                   Double.valueOf(0.5), g.getAnchor("a05").getOffset());

      // wann + a - two analyses, anchor points are spread evenly across the word
      assertEquals("Grammatical word offset set - wanna 1",
                   Double.valueOf(1.0 + 1.0/3.0), g.getAnchor("a13").getOffset());
      assertEquals("Grammatical word offset set - wanna 2",
                   Double.valueOf(1.0 + 2.0/3.0), g.getAnchor("a16").getOffset());

    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }
  
  /** Ensure aligned chained sub-word annotations (e.g. grammatical words within orthographic
   * words) get re-aligned after forces/manual alignment. */
  @Test public void chainedSubWordAnnotationsAfterRealignment() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");
    
    g.addLayer(new Layer("who", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
               .setPeers(true).setPeersOverlap(true).setSaturated(true));
    g.addLayer(new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
               .setPeers(true).setPeersOverlap(false).setSaturated(false)
               .setParentId("who").setParentIncludes(true));
    g.addLayer(new Layer("word", "Orthographic Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
               .setPeers(true).setPeersOverlap(false).setSaturated(false)
               .setParentId("turn").setParentIncludes(true));
    g.addLayer(new Layer("gram", "Grammatical Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
               .setPeers(true).setPeersOverlap(true).setSaturated(true)
               .setParentId("word").setParentIncludes(true));

    // john smith
    g.addAnchor(new Anchor("turn1Start", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start
    // I'm -> I + am - was 0.0-1.0, but now 1.9-2.0 - middle anchor moves forward
    g.addAnchor(new Anchor("a0", 1.9, Constants.CONFIDENCE_AUTOMATIC)); // I
    g.addAnchor(new Anchor("a05", 0.5, Constants.CONFIDENCE_DEFAULT)); // 'm
    // wanna -> want + to or want a, was 2.0-3.0, but now 2.0-2.3 - middle anchors move back
    g.addAnchor(new Anchor("a1", 2.0, Constants.CONFIDENCE_AUTOMATIC)); // wanna
    g.addAnchor(new Anchor("a13", 2.33, Constants.CONFIDENCE_DEFAULT)); // to
    g.addAnchor(new Anchor("a16", 2.66, Constants.CONFIDENCE_DEFAULT)); // a
    g.addAnchor(new Anchor("a2", 2.3, Constants.CONFIDENCE_AUTOMATIC)); // jolly
    g.addAnchor(new Anchor("a3", 3.0, Constants.CONFIDENCE_AUTOMATIC)); // well
    g.addAnchor(new Anchor("a4", 4.0, Constants.CONFIDENCE_AUTOMATIC)); // jump
    g.addAnchor(new Anchor("a5", 5.0, Constants.CONFIDENCE_AUTOMATIC)); // over
    g.addAnchor(new Anchor("a6", 6.0, Constants.CONFIDENCE_AUTOMATIC)); // a
    g.addAnchor(new Anchor("a7", 7.0, Constants.CONFIDENCE_AUTOMATIC)); // lazy
    g.addAnchor(new Anchor("a8", 8.0, Constants.CONFIDENCE_AUTOMATIC)); // dog
    g.addAnchor(new Anchor("a9", 9.0, Constants.CONFIDENCE_AUTOMATIC)); // end of dog
    g.addAnchor(new Anchor("turn1End", 9.0, Constants.CONFIDENCE_MANUAL)); // turn end

    // participants
    g.addAnnotation(
      new Annotation("participant1", "john smith", "who", "turn1Start", "turn1End", "my graph"));
      
    // turns
    g.addAnnotation(
      new Annotation("turn1", "john smith", "turn", "turn1Start", "turn1End", "participant1"));
      
    // words
    // "I'm" splits into "I am"
    g.addAnnotation(new Annotation("I'm",   "I'm",   "word", "a0", "a1", "turn1"));
    // "wanna" splits two possible ways "want to" and "want a"
    g.addAnnotation(new Annotation("wanna", "wanna", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("jolly", "jolly", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("well",  "well",   "word", "a3", "a4", "turn1"));
    g.addAnnotation(new Annotation("jump",  "jump", "word", "a4", "a5", "turn1"));
    g.addAnnotation(new Annotation("over",  "over",  "word", "a5", "a6", "turn1"));
    g.addAnnotation(new Annotation("a",     "a",     "word", "a6", "a7", "turn1"));
    g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a7", "a8", "turn1"));
    g.addAnnotation(new Annotation("dog",   "dog.",   "word", "a8", "a9", "turn1"));

    // grammatical words
    g.addAnnotation(new Annotation("gram_I",   "I",   "gram", "a0", "a05", "I'm"));
    g.addAnnotation(new Annotation("gram_'m",   "am",   "gram", "a05", "a1", "I'm"));

    // analysis 1 "want to"
    g.addAnnotation(new Annotation("gram_want1", "want", "gram", "a1", "a13", "wanna"));
    g.addAnnotation(new Annotation("gram_to1", "to", "gram", "a13", "a2", "wanna"));
    
    // analysis 2 "want a"
    g.addAnnotation(new Annotation("gram_want2", "want", "gram", "a1", "a16", "wanna"));
    g.addAnnotation(new Annotation("gram_a2", "a", "gram", "a16", "a2", "wanna"));
    
    g.addAnnotation(new Annotation("gram_jolly", "jolly", "gram", "a2", "a3", "jolly"));
    g.addAnnotation(new Annotation("gram_well",  "well",   "gram", "a3", "a4", "well"));
    g.addAnnotation(new Annotation("gram_jump",  "jump", "gram", "a4", "a5", "jump"));
    g.addAnnotation(new Annotation("gram_over",  "over",  "gram", "a5", "a6", "over"));
    g.addAnnotation(new Annotation("gram_a",     "a",     "gram", "a6", "a7", "a"));
    g.addAnnotation(new Annotation("gram_lazy",  "lazy",  "gram", "a7", "a8", "lazy"));
    g.addAnnotation(new Annotation("gram_dog",   "dog.",   "gram", "a8", "a9", "dog"));

    assertEquals("no initial changes to graph: " + g.getChanges(), 0, g.getChanges().size());

    g.trackChanges();

    DefaultOffsetGenerator generator = new DefaultOffsetGenerator();
    // generator.setDebug(true);
    try {
      generator.transform(g);
      if (generator.getLog() != null) for (String m : generator.getLog()) System.out.println(m);
      Set<Change> changes = g.getTracker().getChanges();

      // orthographic words are unchanged
      assertEquals("Word anchors unchanged", Double.valueOf(1.9), g.getAnchor("a0").getOffset());
      assertEquals("Word anchors unchanged", Double.valueOf(2.0), g.getAnchor("a1").getOffset());
      assertEquals("Word anchors unchanged", Double.valueOf(2.3), g.getAnchor("a2").getOffset());
      assertEquals("Word anchors unchanged", Double.valueOf(3.0), g.getAnchor("a3").getOffset());
      assertEquals("Word anchors unchanged", Double.valueOf(4.0), g.getAnchor("a4").getOffset());
      assertEquals("Word anchors unchanged", Double.valueOf(5.0), g.getAnchor("a5").getOffset());
      assertEquals("Word anchors unchanged", Double.valueOf(6.0), g.getAnchor("a6").getOffset());
      assertEquals("Word anchors unchanged", Double.valueOf(7.0), g.getAnchor("a7").getOffset());
      assertEquals("Word anchors unchanged", Double.valueOf(8.0), g.getAnchor("a8").getOffset());
      assertEquals("Word anchors unchanged", Double.valueOf(9.0), g.getAnchor("a9").getOffset());

      // grammatical words are anchored

      // I + 'm anchors at the mid point
      assertEquals("Grammatical word offset set - I'm",
                   Double.valueOf(1.95), g.getAnchor("a05").getOffset());

      // wann + a - two analyses, anchor points are spread evenly across the word
      assertEquals("Grammatical word offset set - wanna 1",
                   Double.valueOf(2.1), g.getAnchor("a13").getOffset());
      assertEquals("Grammatical word offset set - wanna 2",
                   // yay for rounding errors
                   Double.valueOf(2.1999999999999997), g.getAnchor("a16").getOffset());

    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Test offset generation completes in a timely manner */
  @Test public void performance() {
    Graph g = new Graph();
    g.setId("g");
    
    g.addLayer(new Layer("who", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
               .setPeers(true).setPeersOverlap(true).setSaturated(true));
    g.addLayer(new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
               .setPeers(true).setPeersOverlap(false).setSaturated(false)
               .setParentId("who").setParentIncludes(true));
    g.addLayer(new Layer("utterance", "Lines").setAlignment(Constants.ALIGNMENT_INTERVAL)
               .setPeers(true).setPeersOverlap(false).setSaturated(true)
               .setParentId("turn").setParentIncludes(true));
    g.addLayer(new Layer("word", "Orthographic Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
               .setPeers(true).setPeersOverlap(false).setSaturated(false)
               .setParentId("turn").setParentIncludes(true));

    // create a graph with a huge number of words
    int wordCount = 5000; // got bored waiting for 10000 to finish
    int wordsPerUtterance = 5;
    int utterancesPerTurn = 2;
    int numUtterances = wordCount/wordsPerUtterance;
    int numTurns = numUtterances/utterancesPerTurn;
    Anchor lastAnchor = g.getOrCreateAnchorAt(0.0);

    // participant
    Annotation participant = g.addAnnotation(
      new Annotation("p", "participant", "who", lastAnchor.getId(), lastAnchor.getId(), "g"));

    // create turns
    for (int t = 0; t < numTurns; t++) {
      Anchor turnEnd = g.getOrCreateAnchorAt(
        (t+1)*utterancesPerTurn *wordsPerUtterance);
      Annotation turn = g.addAnnotation(
        new Annotation("turn"+t, participant.getLabel(), "turn",
                       lastAnchor.getId(), turnEnd.getId(), "p"));
      for (int u = 0; u < utterancesPerTurn; u++) {
        Anchor utteranceEnd = g.getOrCreateAnchorAt(
          lastAnchor.getOffset() + wordsPerUtterance);
        Annotation utterance = g.addAnnotation(
          new Annotation("turn"+t+"-utt"+u, participant.getLabel(), "utterance",
                         lastAnchor.getId(), utteranceEnd.getId(), turn.getId()));
        // create words
        for (int w = 0; w < wordsPerUtterance; w++) {
          Anchor start = new Anchor();
          if (w == 0) {
            start.setOffset(lastAnchor.getOffset());
          }
          g.addAnchor(start);
          Anchor end = new Anchor();
          if (w == wordsPerUtterance-1) {
            end.setOffset(utteranceEnd.getOffset());
          }
          g.addAnchor(end);
          g.addAnnotation(
            new Annotation(null, utterance.getId() + "-word" + w, "word",
                           start.getId(), end.getId(), turn.getId()));
        } // next word
        lastAnchor = utteranceEnd;
      } // next utterance
      
      lastAnchor = turnEnd;
    } // next turn
    participant.setEndId(lastAnchor.getId());

    Timers timers = new Timers();
    DefaultOffsetGenerator generator = new DefaultOffsetGenerator();
    try {
      timers.start("DefaultOffsetGenerator.transform");
      generator.transform(g);
      timers.end("DefaultOffsetGenerator.transform");
      if (generator.getLog() != null) for (String m : generator.getLog()) System.out.println(m);

      System.out.println(timers.toString());
      assertTrue(
        "DefaultOffsetGenerator too slow:\n" + timers.toString(),
        30000 > timers.getTotals().get("DefaultOffsetGenerator.transform"));      
      
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.ag.util.TestDefaultOffsetGenerator");
  }
}
