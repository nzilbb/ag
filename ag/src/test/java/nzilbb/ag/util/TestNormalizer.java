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

package nzilbb.ag.util;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import nzilbb.ag.*;
import nzilbb.ag.util.*;
import nzilbb.util.Timers;

public class TestNormalizer {

  /** Test that already-normal graphs are not changed. */
  @Test public void alreadyNormalized() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");
    Layer[] layers = {
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterance")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("pos", "POS")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true)};
    g.setSchema(new Schema(layers, "who", "turn", "utterance", "word"));

    g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start

    g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE_DEFAULT)); // the
    g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE_DEFAULT)); // quick
    g.addAnchor(new Anchor("a02", 0.03, Constants.CONFIDENCE_DEFAULT)); // brown
    g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox
    g.addAnchor(new Anchor("a04a", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox end

    g.addAnchor(new Anchor("utteranceChange", 0.4, Constants.CONFIDENCE_MANUAL)); // utterance boundary

    g.addAnchor(new Anchor("a04b", 2.0, Constants.CONFIDENCE_AUTOMATIC)); // jumps
    g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE_AUTOMATIC)); // over
    g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE_AUTOMATIC)); // a
    g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE_AUTOMATIC)); // lazy
    g.addAnchor(new Anchor("a44", 5.1, Constants.CONFIDENCE_AUTOMATIC)); // dog
    g.addAnchor(new Anchor("a54", null)); // end of dog

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

    g.getAnnotation("jumps").setLabel("test");
    g.createTag(g.getAnnotation("jumps"), "pos", "V");

    g.trackChanges();
    Normalizer n = new Normalizer();
    try {
      n.transform(g);
      Set<Change> changes = g.getTracker().getChanges();

      assertEquals("no changes: " + changes, 0, changes.size());
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Test that contiguous turns are joined. */
  @Test public void joinTurns() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");      
    Layer[] layers = {
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterance")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("pos", "POS")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true)};
    g.setSchema(new Schema(layers, "who", "turn", "utterance", "word"));

    g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start

    g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE_DEFAULT)); // the
    g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE_DEFAULT)); // quick
    g.addAnchor(new Anchor("a02", 0.03, Constants.CONFIDENCE_DEFAULT)); // brown
    g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox
    g.addAnchor(new Anchor("a04a", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox end

    g.addAnchor(new Anchor("utteranceChange", 0.4, Constants.CONFIDENCE_MANUAL)); // utterance boundary

    g.addAnchor(new Anchor("a04b", 2.0, Constants.CONFIDENCE_AUTOMATIC)); // jumps
    g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE_AUTOMATIC)); // over
    g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE_AUTOMATIC)); // a
    g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE_AUTOMATIC)); // lazy
    g.addAnchor(new Anchor("a44", 5.1, Constants.CONFIDENCE_AUTOMATIC)); // dog
    g.addAnchor(new Anchor("a54", null)); // end of dog

    g.addAnchor(new Anchor("turnEnd", 5.4, Constants.CONFIDENCE_MANUAL)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "utteranceChange", "participant1"));
    g.addAnnotation(new Annotation("turn2", "john smith", "turn", "utteranceChange", "turnEnd", "participant1"));

    g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utteranceChange", "turn1"));
    g.addAnnotation(new Annotation("utterance2", "john smith", "utterance", "utteranceChange", "turnEnd", "turn2"));
      
    g.addAnnotation(new Annotation("the",   "the",   "word", "a0",  "a01", "turn1"));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a01", "a02", "turn1"));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a02",  "a03", "turn1"));
    g.addAnnotation(new Annotation("fox",   "fox",   "word", "a03", "a04a", "turn1"));

    g.addAnnotation(new Annotation("jumps", "jumps", "word", "a04b",  "a14", "turn2"));
    g.addAnnotation(new Annotation("over",  "over",  "word", "a14",  "a24", "turn2"));
    g.addAnnotation(new Annotation("a",     "a",     "word", "a24",  "a34", "turn2"));
    g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a34",  "a44", "turn2"));
    g.addAnnotation(new Annotation("dog",  "dog",    "word", "a44",  "a54", "turn2"));

    g.getAnnotation("jumps").setLabel("test");
    g.createTag(g.getAnnotation("jumps"), "pos", "V");

    g.trackChanges();
    Normalizer n = new Normalizer();
    try {
      n.transform(g);
      Set<Change> changes = g.getTracker().getChanges();

      assertNotEquals("changes: " + changes, 0, changes.size());

      assertEquals("turn1 end later", "turnEnd", g.getAnnotation("turn1").getEndId());
      assertEquals("turn2 deleted", Change.Operation.Destroy, g.getAnnotation("turn2").getChange());
      assertEquals("turn changed", "turn1", g.getAnnotation("jumps").getParentId());
      assertEquals("turn changed", "turn1", g.getAnnotation("over").getParentId());
      assertEquals("turn changed", "turn1", g.getAnnotation("a").getParentId());
      assertEquals("turn changed", "turn1", g.getAnnotation("lazy").getParentId());
      assertEquals("turn changed", "turn1", g.getAnnotation("dog").getParentId());

      assertEquals("ordinal changed", 5, g.getAnnotation("jumps").getOrdinal());
      assertEquals("ordinal changed", 1, g.getAnnotation("jumps").getOriginalOrdinal());
      assertEquals("ordinal changed", 6, g.getAnnotation("over").getOrdinal());
      assertEquals("ordinal changed", 2, g.getAnnotation("over").getOriginalOrdinal());
      assertEquals("ordinal changed", 7, g.getAnnotation("a").getOrdinal());
      assertEquals("ordinal changed", 3, g.getAnnotation("a").getOriginalOrdinal());
      assertEquals("ordinal changed", 8, g.getAnnotation("lazy").getOrdinal());
      assertEquals("ordinal changed", 4, g.getAnnotation("lazy").getOriginalOrdinal());
      assertEquals("ordinal changed", 9, g.getAnnotation("dog").getOrdinal());
      assertEquals("ordinal changed", 5, g.getAnnotation("dog").getOriginalOrdinal());
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Test that contiguous turns are not joined when there's no minimum pause length. */
  @Test public void joinTurnsNoMinimumTurnPauseLength() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");      
    Layer[] layers = {
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterance")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("pos", "POS")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true)};
    g.setSchema(new Schema(layers, "who", "turn", "utterance", "word"));

    g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start

    g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE_DEFAULT)); // the
    g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE_DEFAULT)); // quick
    g.addAnchor(new Anchor("a02", 0.03, Constants.CONFIDENCE_DEFAULT)); // brown
    g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox
    g.addAnchor(new Anchor("a04a", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox end

    g.addAnchor(new Anchor("utteranceEnd", 0.4, Constants.CONFIDENCE_MANUAL)); // utterance boundary
    g.addAnchor(new Anchor("utteranceStart", 1.0, Constants.CONFIDENCE_MANUAL)); // utterance boundary

    g.addAnchor(new Anchor("a04b", 2.0, Constants.CONFIDENCE_AUTOMATIC)); // jumps
    g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE_AUTOMATIC)); // over
    g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE_AUTOMATIC)); // a
    g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE_AUTOMATIC)); // lazy
    g.addAnchor(new Anchor("a44", 5.1, Constants.CONFIDENCE_AUTOMATIC)); // dog
    g.addAnchor(new Anchor("a54", null)); // end of dog

    g.addAnchor(new Anchor("turnEnd", 5.4, Constants.CONFIDENCE_MANUAL)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "utteranceEnd", "participant1"));
    g.addAnnotation(new Annotation("turn2", "john smith", "turn", "utteranceStart", "turnEnd", "participant1"));

    g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utteranceEnd", "turn1"));
    g.addAnnotation(new Annotation("utterance2", "john smith", "utterance", "utteranceStart", "turnEnd", "turn2"));
      
    g.addAnnotation(new Annotation("the",   "the",   "word", "a0",  "a01", "turn1"));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a01", "a02", "turn1"));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a02",  "a03", "turn1"));
    g.addAnnotation(new Annotation("fox",   "fox",   "word", "a03", "a04a", "turn1"));

    g.addAnnotation(new Annotation("jumps", "jumps", "word", "a04b",  "a14", "turn2"));
    g.addAnnotation(new Annotation("over",  "over",  "word", "a14",  "a24", "turn2"));
    g.addAnnotation(new Annotation("a",     "a",     "word", "a24",  "a34", "turn2"));
    g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a34",  "a44", "turn2"));
    g.addAnnotation(new Annotation("dog",  "dog",    "word", "a44",  "a54", "turn2"));

    g.getAnnotation("jumps").setLabel("test");
    g.createTag(g.getAnnotation("jumps"), "pos", "V");

    g.trackChanges();
    Normalizer n = new Normalizer();
    // no minimumTurnPauseLength, so the gap between turns is not bridged
    try {
      n.transform(g);
      Set<Change> changes = g.getTracker().getChanges();

      assertEquals("changes: " + changes, 0, changes.size());

      assertEquals("turn1 not changed", "utteranceEnd", g.getAnnotation("turn1").getEndId());
      assertEquals("turn2 not changed", "utteranceStart", g.getAnnotation("turn2").getStartId());
      assertEquals("turn2 not deleted", Change.Operation.NoChange, g.getAnnotation("turn2").getChange());
      assertEquals("turn not changed", "turn2", g.getAnnotation("jumps").getParentId());
      assertEquals("turn not changed", "turn2", g.getAnnotation("over").getParentId());
      assertEquals("turn not changed", "turn2", g.getAnnotation("a").getParentId());
      assertEquals("turn not changed", "turn2", g.getAnnotation("lazy").getParentId());
      assertEquals("turn not changed", "turn2", g.getAnnotation("dog").getParentId());
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Test that minimum turn pause length is respected when joining contiguous turns. */
  @Test public void joinTurnsMinimumTurnPauseLength() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");      
    Layer[] layers = {
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterance")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("pos", "POS")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true)};
    g.setSchema(new Schema(layers, "who", "turn", "utterance", "word"));

    g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start

    g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE_DEFAULT)); // the
    g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE_DEFAULT)); // quick
    g.addAnchor(new Anchor("a02", 0.03, Constants.CONFIDENCE_DEFAULT)); // brown
    g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox
    g.addAnchor(new Anchor("a04a", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox end

    g.addAnchor(new Anchor("utteranceEnd", 0.4, Constants.CONFIDENCE_MANUAL)); // utterance boundary
    g.addAnchor(new Anchor("utteranceStart", 1.0, Constants.CONFIDENCE_MANUAL)); // utterance boundary

    g.addAnchor(new Anchor("a04b", 2.0, Constants.CONFIDENCE_AUTOMATIC)); // jumps
    g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE_AUTOMATIC)); // over
    g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE_AUTOMATIC)); // a
    g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE_AUTOMATIC)); // lazy
    g.addAnchor(new Anchor("a44", 5.1, Constants.CONFIDENCE_AUTOMATIC)); // dog
    g.addAnchor(new Anchor("a54", null)); // end of dog

    g.addAnchor(new Anchor("turnEnd", 5.4, Constants.CONFIDENCE_MANUAL)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "utteranceEnd", "participant1"));
    g.addAnnotation(new Annotation("turn2", "john smith", "turn", "utteranceStart", "turnEnd", "participant1"));

    g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utteranceEnd", "turn1"));
    g.addAnnotation(new Annotation("utterance2", "john smith", "utterance", "utteranceStart", "turnEnd", "turn2"));
      
    g.addAnnotation(new Annotation("the",   "the",   "word", "a0",  "a01", "turn1"));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a01", "a02", "turn1"));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a02",  "a03", "turn1"));
    g.addAnnotation(new Annotation("fox",   "fox",   "word", "a03", "a04a", "turn1"));

    g.addAnnotation(new Annotation("jumps", "jumps", "word", "a04b",  "a14", "turn2"));
    g.addAnnotation(new Annotation("over",  "over",  "word", "a14",  "a24", "turn2"));
    g.addAnnotation(new Annotation("a",     "a",     "word", "a24",  "a34", "turn2"));
    g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a34",  "a44", "turn2"));
    g.addAnnotation(new Annotation("dog",  "dog",    "word", "a44",  "a54", "turn2"));

    g.getAnnotation("jumps").setLabel("test");
    g.createTag(g.getAnnotation("jumps"), "pos", "V");

    g.trackChanges();
    Normalizer n = new Normalizer();
    // minimum long enough to bridge the gap
    n.setMinimumTurnPauseLength(1.0);
    try {
      n.transform(g);
      Set<Change> changes = g.getTracker().getChanges();
      
      assertNotEquals("changes: " + changes, 0, changes.size());

      assertEquals("turn1 end later", "turnEnd", g.getAnnotation("turn1").getEndId());
      assertEquals("turn2 deleted", Change.Operation.Destroy, g.getAnnotation("turn2").getChange());
      assertEquals("turn changed", "turn1", g.getAnnotation("jumps").getParentId());
      assertEquals("turn changed", "turn1", g.getAnnotation("over").getParentId());
      assertEquals("turn changed", "turn1", g.getAnnotation("a").getParentId());
      assertEquals("turn changed", "turn1", g.getAnnotation("lazy").getParentId());
      assertEquals("turn changed", "turn1", g.getAnnotation("dog").getParentId());

      assertEquals("ordinal changed", 5, g.getAnnotation("jumps").getOrdinal());
      assertEquals("ordinal changed", 1, g.getAnnotation("jumps").getOriginalOrdinal());
      assertEquals("ordinal changed", 6, g.getAnnotation("over").getOrdinal());
      assertEquals("ordinal changed", 2, g.getAnnotation("over").getOriginalOrdinal());
      assertEquals("ordinal changed", 7, g.getAnnotation("a").getOrdinal());
      assertEquals("ordinal changed", 3, g.getAnnotation("a").getOriginalOrdinal());
      assertEquals("ordinal changed", 8, g.getAnnotation("lazy").getOrdinal());
      assertEquals("ordinal changed", 4, g.getAnnotation("lazy").getOriginalOrdinal());
      assertEquals("ordinal changed", 9, g.getAnnotation("dog").getOrdinal());
      assertEquals("ordinal changed", 5, g.getAnnotation("dog").getOriginalOrdinal());

      assertEquals("utterance1 linked to utterance2",
                   g.getAnnotation("utterance2").getStartId(),
                   g.getAnnotation("utterance1").getEndId());
      assertEquals("utterance1 linked to utterance2 by utterance2.start",
                   "utteranceStart", g.getAnnotation("utterance1").getEndId());
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Test that minimum turn pause length is respected when joining contiguous turns, but
   * if there are shared anchors with the turn of another speaker, the other speaker turn
   * isn't changed. */
  @Test public void joinTurnsMinimumTurnPauseLengthSharedAnchors() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");      
    Layer[] layers = {
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterance")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("pos", "POS")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true)};
    g.setSchema(new Schema(layers, "who", "turn", "utterance", "word"));

    g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start

    g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE_DEFAULT)); // the
    g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE_DEFAULT)); // quick
    g.addAnchor(new Anchor("a02", 0.03, Constants.CONFIDENCE_DEFAULT)); // brown
    g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox
    g.addAnchor(new Anchor("a04a", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox end

    g.addAnchor(new Anchor("other-a01", 0.01, Constants.CONFIDENCE_DEFAULT)); // ah
    g.addAnchor(new Anchor("other-a02", 0.02, Constants.CONFIDENCE_DEFAULT)); // hah
    g.addAnchor(new Anchor("other-a03", 0.03, Constants.CONFIDENCE_DEFAULT)); // hah

    g.addAnchor(new Anchor("utteranceEnd", 0.4, Constants.CONFIDENCE_MANUAL)); // utterance boundary
    g.addAnchor(new Anchor("utteranceStart", 1.0, Constants.CONFIDENCE_MANUAL)); // utterance boundary

    g.addAnchor(new Anchor("a04b", 2.0, Constants.CONFIDENCE_AUTOMATIC)); // jumps
    g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE_AUTOMATIC)); // over
    g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE_AUTOMATIC)); // a
    g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE_AUTOMATIC)); // lazy
    g.addAnchor(new Anchor("a44", 5.1, Constants.CONFIDENCE_AUTOMATIC)); // dog
    g.addAnchor(new Anchor("a54", null)); // end of dog

    g.addAnchor(new Anchor("turnEnd", 5.4, Constants.CONFIDENCE_MANUAL)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
    g.addAnnotation(new Annotation("participant2", "jane doe", "who", "turnStart", "utteranceEnd", "my graph"));
      
    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "utteranceEnd", "participant1"));
    g.addAnnotation(new Annotation("other-turn1", "jane dow", "turn", "turnStart", "utteranceEnd", "participant2"));
    g.addAnnotation(new Annotation("turn2", "john smith", "turn", "utteranceStart", "turnEnd", "participant1"));
    

    g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utteranceEnd", "turn1"));
    g.addAnnotation(new Annotation("other-utterance1", "jane doe", "utterance", "turnStart", "utteranceEnd", "other-turn1"));
    g.addAnnotation(new Annotation("utterance2", "john smith", "utterance", "utteranceStart", "turnEnd", "turn2"));
      
    g.addAnnotation(new Annotation("the",   "the",   "word", "a0",  "a01", "turn1"));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a01", "a02", "turn1"));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a02",  "a03", "turn1"));
    g.addAnnotation(new Annotation("fox",   "fox",   "word", "a03", "a04a", "turn1"));

    g.addAnnotation(new Annotation("ah",  "ah",  "word", "other-a01", "other-a02", "other-turn1"));
    g.addAnnotation(new Annotation("huh", "huh", "word", "other-a02", "other-a03", "other-turn1"));

    g.addAnnotation(new Annotation("jumps", "jumps", "word", "a04b",  "a14", "turn2"));
    g.addAnnotation(new Annotation("over",  "over",  "word", "a14",  "a24", "turn2"));
    g.addAnnotation(new Annotation("a",     "a",     "word", "a24",  "a34", "turn2"));
    g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a34",  "a44", "turn2"));
    g.addAnnotation(new Annotation("dog",  "dog",    "word", "a44",  "a54", "turn2"));

    g.getAnnotation("jumps").setLabel("test");
    g.createTag(g.getAnnotation("jumps"), "pos", "V");

    g.trackChanges();
    Normalizer n = new Normalizer();
    // minimum long enough to bridge the gap
    n.setMinimumTurnPauseLength(1.0);
    try {
      n.transform(g);
      Set<Change> changes = g.getTracker().getChanges();
      
      assertNotEquals("changes: " + changes, 0, changes.size());

      assertEquals("turn1 end later", "turnEnd", g.getAnnotation("turn1").getEndId());
      assertEquals("turn2 deleted", Change.Operation.Destroy, g.getAnnotation("turn2").getChange());
      assertEquals("turn changed", "turn1", g.getAnnotation("jumps").getParentId());
      assertEquals("turn changed", "turn1", g.getAnnotation("over").getParentId());
      assertEquals("turn changed", "turn1", g.getAnnotation("a").getParentId());
      assertEquals("turn changed", "turn1", g.getAnnotation("lazy").getParentId());
      assertEquals("turn changed", "turn1", g.getAnnotation("dog").getParentId());

      assertEquals("other-turn1 end unchanged",
                   "utteranceEnd", g.getAnnotation("other-turn1").getEndId());
      assertEquals("other-utterance1 end unchanged",
                   "utteranceEnd", g.getAnnotation("other-utterance1").getEndId());

      assertEquals("ordinal changed", 5, g.getAnnotation("jumps").getOrdinal());
      assertEquals("ordinal changed", 1, g.getAnnotation("jumps").getOriginalOrdinal());
      assertEquals("ordinal changed", 6, g.getAnnotation("over").getOrdinal());
      assertEquals("ordinal changed", 2, g.getAnnotation("over").getOriginalOrdinal());
      assertEquals("ordinal changed", 7, g.getAnnotation("a").getOrdinal());
      assertEquals("ordinal changed", 3, g.getAnnotation("a").getOriginalOrdinal());
      assertEquals("ordinal changed", 8, g.getAnnotation("lazy").getOrdinal());
      assertEquals("ordinal changed", 4, g.getAnnotation("lazy").getOriginalOrdinal());
      assertEquals("ordinal changed", 9, g.getAnnotation("dog").getOrdinal());
      assertEquals("ordinal changed", 5, g.getAnnotation("dog").getOriginalOrdinal());

      assertEquals("utterance1 linked to utterance2",
                   g.getAnnotation("utterance2").getStartId(),
                   g.getAnnotation("utterance1").getEndId());
      assertEquals("utterance1 linked to utterance2 by utterance2.start",
                   "utteranceStart", g.getAnnotation("utterance1").getEndId());
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Ensure turns are not joined when minimum pause length is too short. */
  @Test public void joinTurnsMinimumTurnPauseLengthTooShort() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");      
    Layer[] layers = {
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterance")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("pos", "POS")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true)};
    g.setSchema(new Schema(layers, "who", "turn", "utterance", "word"));

    g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start

    g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE_DEFAULT)); // the
    g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE_DEFAULT)); // quick
    g.addAnchor(new Anchor("a02", 0.03, Constants.CONFIDENCE_DEFAULT)); // brown
    g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox
    g.addAnchor(new Anchor("a04a", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox end

    g.addAnchor(new Anchor("utteranceEnd", 0.4, Constants.CONFIDENCE_MANUAL)); // utterance boundary
    g.addAnchor(new Anchor("utteranceStart", 1.0, Constants.CONFIDENCE_MANUAL)); // utterance boundary

    g.addAnchor(new Anchor("a04b", 2.0, Constants.CONFIDENCE_AUTOMATIC)); // jumps
    g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE_AUTOMATIC)); // over
    g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE_AUTOMATIC)); // a
    g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE_AUTOMATIC)); // lazy
    g.addAnchor(new Anchor("a44", 5.1, Constants.CONFIDENCE_AUTOMATIC)); // dog
    g.addAnchor(new Anchor("a54", null)); // end of dog

    g.addAnchor(new Anchor("turnEnd", 5.4, Constants.CONFIDENCE_MANUAL)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "utteranceEnd", "participant1"));
    g.addAnnotation(new Annotation("turn2", "john smith", "turn", "utteranceStart", "turnEnd", "participant1"));

    g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utteranceEnd", "turn1"));
    g.addAnnotation(new Annotation("utterance2", "john smith", "utterance", "utteranceStart", "turnEnd", "turn2"));
      
    g.addAnnotation(new Annotation("the",   "the",   "word", "a0",  "a01", "turn1"));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a01", "a02", "turn1"));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a02",  "a03", "turn1"));
    g.addAnnotation(new Annotation("fox",   "fox",   "word", "a03", "a04a", "turn1"));

    g.addAnnotation(new Annotation("jumps", "jumps", "word", "a04b",  "a14", "turn2"));
    g.addAnnotation(new Annotation("over",  "over",  "word", "a14",  "a24", "turn2"));
    g.addAnnotation(new Annotation("a",     "a",     "word", "a24",  "a34", "turn2"));
    g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a34",  "a44", "turn2"));
    g.addAnnotation(new Annotation("dog",  "dog",    "word", "a44",  "a54", "turn2"));

    g.getAnnotation("jumps").setLabel("test");
    g.createTag(g.getAnnotation("jumps"), "pos", "V");

    g.trackChanges();
    Normalizer n = new Normalizer();
    // minimum is too short to bridge the gap
    n.setMinimumTurnPauseLength(0.5);
    try {
      n.transform(g);
      Set<Change> changes = g.getTracker().getChanges();

      assertEquals("changes: " + changes, 0, changes.size());

      assertEquals("turn1 not changed", "utteranceEnd", g.getAnnotation("turn1").getEndId());
      assertEquals("turn2 not changed", "utteranceStart", g.getAnnotation("turn2").getStartId());
      assertEquals("turn2 not deleted", Change.Operation.NoChange, g.getAnnotation("turn2").getChange());
      assertEquals("turn not changed", "turn2", g.getAnnotation("jumps").getParentId());
      assertEquals("turn not changed", "turn2", g.getAnnotation("over").getParentId());
      assertEquals("turn not changed", "turn2", g.getAnnotation("a").getParentId());
      assertEquals("turn not changed", "turn2", g.getAnnotation("lazy").getParentId());
      assertEquals("turn not changed", "turn2", g.getAnnotation("dog").getParentId());
      
      assertNotEquals("utterance1 not linked to utterance2",
                      g.getAnnotation("utterance2").getStartId(),
                      g.getAnnotation("utterance1").getEndId());
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Ensure turns are not joined when there's an intervening speaker. */
  @Test public void joinTurnsMinimumTurnPauseLengthInterveningSpeaker() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");      
    Layer[] layers = {
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterance")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("pos", "POS")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true)};
    g.setSchema(new Schema(layers, "who", "turn", "utterance", "word"));

    g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start

    g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE_DEFAULT)); // the
    g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE_DEFAULT)); // quick
    g.addAnchor(new Anchor("a02", 0.03, Constants.CONFIDENCE_DEFAULT)); // brown
    g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox
    g.addAnchor(new Anchor("a04a", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox end

    g.addAnchor(new Anchor("utteranceEnd", 0.4, Constants.CONFIDENCE_MANUAL)); // utterance boundary
    g.addAnchor(new Anchor("ao1", 0.5, Constants.CONFIDENCE_DEFAULT)); // yes start
    g.addAnchor(new Anchor("ao2", 0.9, Constants.CONFIDENCE_DEFAULT)); // yes end
    g.addAnchor(new Anchor("utteranceStart", 1.0, Constants.CONFIDENCE_MANUAL)); // utterance boundary

    g.addAnchor(new Anchor("a04b", 2.0, Constants.CONFIDENCE_AUTOMATIC)); // jumps
    g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE_AUTOMATIC)); // over
    g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE_AUTOMATIC)); // a
    g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE_AUTOMATIC)); // lazy
    g.addAnchor(new Anchor("a44", 5.1, Constants.CONFIDENCE_AUTOMATIC)); // dog
    g.addAnchor(new Anchor("a54", null)); // end of dog

    g.addAnchor(new Anchor("turnEnd", 5.4, Constants.CONFIDENCE_MANUAL)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
    g.addAnnotation(new Annotation("participant2", "jane doe", "who", "turnStart", "turnEnd", "my graph"));
      
    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "utteranceEnd", "participant1"));
    g.addAnnotation(new Annotation("turnOther", "jane doe", "turn", "utteranceEnd", "utteranceStart", "participant2"));
    g.addAnnotation(new Annotation("turn2", "john smith", "turn", "utteranceStart", "turnEnd", "participant1"));

    g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utteranceEnd", "turn1"));
    g.addAnnotation(new Annotation("utteranceOther", "jane doe", "utterance", "utteranceEnd", "utteranceStart", "turnOther"));
    g.addAnnotation(new Annotation("utterance2", "john smith", "utterance", "utteranceStart", "turnEnd", "turn2"));
      
    g.addAnnotation(new Annotation("the",   "the",   "word", "a0",  "a01", "turn1"));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a01", "a02", "turn1"));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a02",  "a03", "turn1"));
    g.addAnnotation(new Annotation("fox",   "fox",   "word", "a03", "a04a", "turn1"));

    g.addAnnotation(new Annotation("yes",   "yes",   "word", "ao1", "ao2", "turnOther"));

    g.addAnnotation(new Annotation("jumps", "jumps", "word", "a04b",  "a14", "turn2"));
    g.addAnnotation(new Annotation("over",  "over",  "word", "a14",  "a24", "turn2"));
    g.addAnnotation(new Annotation("a",     "a",     "word", "a24",  "a34", "turn2"));
    g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a34",  "a44", "turn2"));
    g.addAnnotation(new Annotation("dog",  "dog",    "word", "a44",  "a54", "turn2"));

    g.getAnnotation("jumps").setLabel("test");
    g.createTag(g.getAnnotation("jumps"), "pos", "V");

    g.trackChanges();
    Normalizer n = new Normalizer();
    // minimum long enough to bridge the gap, but there's an intervening speaker
    n.setMinimumTurnPauseLength(1.0);
    try {
      n.transform(g);
      Set<Change> changes = g.getTracker().getChanges();

      assertEquals("changes: " + changes, 0, changes.size());

      assertEquals("turn1 not changed", "utteranceEnd", g.getAnnotation("turn1").getEndId());
      assertEquals("turn2 not changed", "utteranceStart", g.getAnnotation("turn2").getStartId());
      assertEquals("turn2 not deleted", Change.Operation.NoChange, g.getAnnotation("turn2").getChange());
      assertEquals("turn not changed", "turn2", g.getAnnotation("jumps").getParentId());
      assertEquals("turn not changed", "turn2", g.getAnnotation("over").getParentId());
      assertEquals("turn not changed", "turn2", g.getAnnotation("a").getParentId());
      assertEquals("turn not changed", "turn2", g.getAnnotation("lazy").getParentId());
      assertEquals("turn not changed", "turn2", g.getAnnotation("dog").getParentId());
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Ensure words are disconnected from turns and utterances (so they can be
   * independently aligned). */
  @Test public void disconnectFromTurnsUtterances() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");      
    Layer[] layers = {
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterance")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("pos", "POS")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true)};
    g.setSchema(new Schema(layers, "who", "turn", "utterance", "word"));

    g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start

    g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE_DEFAULT+1)); // the
    g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE_DEFAULT+1)); // quick
    g.addAnchor(new Anchor("a02", 0.03, Constants.CONFIDENCE_DEFAULT+1)); // brown
    g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE_DEFAULT+1)); // fox
    g.addAnchor(new Anchor("a04a", 0.04, Constants.CONFIDENCE_DEFAULT+1)); // fox end

    g.addAnchor(new Anchor("utteranceChange", 0.4, Constants.CONFIDENCE_MANUAL)); // utterance boundary

    g.addAnchor(new Anchor("a04b", 2.0, Constants.CONFIDENCE_AUTOMATIC)); // jumps
    g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE_AUTOMATIC)); // over
    g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE_AUTOMATIC)); // a
    g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE_AUTOMATIC)); // lazy
    g.addAnchor(new Anchor("a44", 5.1, Constants.CONFIDENCE_AUTOMATIC)); // dog
    g.addAnchor(new Anchor("a54", null)); // end of dog

    g.addAnchor(new Anchor("turnEnd", 5.4, Constants.CONFIDENCE_MANUAL)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "participant1"));

    g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utteranceChange", "turn1"));
    g.addAnnotation(new Annotation("utterance2", "john smith", "utterance", "utteranceChange", "turnEnd", "turn1"));
      
    g.addAnnotation(new Annotation("the",   "the",   "word", "turnStart",  "a01", "turn1"));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a01", "a02", "turn1"));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a02",  "a03", "turn1"));
    g.addAnnotation(new Annotation("fox",   "fox",   "word", "a03", "utteranceChange", "turn1"));

    g.addAnnotation(new Annotation("jumps", "jumps", "word", "utteranceChange",  "a14", "turn1"));
    g.addAnnotation(new Annotation("over",  "over",  "word", "a14",  "a24", "turn1"));
    g.addAnnotation(new Annotation("a",     "a",     "word", "a24",  "a34", "turn1"));
    g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a34",  "a44", "turn1"));
    g.addAnnotation(new Annotation("dog",  "dog",    "word", "a44",  "turnEnd", "turn1"));

    g.getAnnotation("jumps").setLabel("test");
    g.createTag(g.getAnnotation("jumps"), "pos", "V");

    g.trackChanges();
    Normalizer n = new Normalizer();
    try {
      n.transform(g);
      Set<Change> changes = g.getTracker().getChanges();

      assertNotEquals("changes: " + changes, 0, changes.size());

      assertNotEquals("start changed", "turnStart", g.getAnnotation("the").getStartId());
      assertNotEquals("end changed", "utteranceChange", g.getAnnotation("fox").getEndId());
      assertNotEquals("start changed", "utteranceChange", g.getAnnotation("jumps").getStartId());
      assertNotEquals("end changed", "turnEnd", g.getAnnotation("dog").getEndId());

      assertEquals("new anchor", Change.Operation.Create, 
                   g.getAnnotation("the").getStart().getChange());
      assertEquals("new anchor", Change.Operation.Create, 
                   g.getAnnotation("fox").getEnd().getChange());
      assertEquals("new anchor", Change.Operation.Create, 
                   g.getAnnotation("jumps").getStart().getChange());
      assertEquals("new anchor", Change.Operation.Create, 
                   g.getAnnotation("dog").getEnd().getChange());
      assertNotEquals("words not linked", g.getAnnotation("jumps").getStart(), 
                      g.getAnnotation("fox").getEnd());

      assertEquals("same offset", Double.valueOf(0.0), 
                   g.getAnnotation("the").getStart().getOffset());
      assertEquals("same offset", Double.valueOf(0.4), 
                   g.getAnnotation("fox").getEnd().getOffset());
      assertEquals("same offset", Double.valueOf(0.4), 
                   g.getAnnotation("jumps").getStart().getOffset());
      assertEquals("same offset", Double.valueOf(5.4), 
                   g.getAnnotation("dog").getEnd().getOffset());

      assertEquals("confidence copied from end", Constants.CONFIDENCE_DEFAULT+1, 
                   g.getAnnotation("the").getStart().getConfidence().intValue());
      assertEquals("confidence copied from start", Constants.CONFIDENCE_DEFAULT+1, 
                   g.getAnnotation("fox").getEnd().getConfidence().intValue());
      assertEquals("confidence copied from end", Constants.CONFIDENCE_AUTOMATIC, 
                   g.getAnnotation("jumps").getStart().getConfidence().intValue());
      assertEquals("confidence copied from start", Constants.CONFIDENCE_AUTOMATIC, 
                   g.getAnnotation("dog").getEnd().getConfidence().intValue());

    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Ensure that anchor confidence is kept when disconnecting words from turns/utterances. */
  @Test public void disconnectFromTurnsUtterancesKeepConfidence() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");      
    Layer[] layers = {
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterance")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("pos", "POS")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true)};
    g.setSchema(new Schema(layers, "who", "turn", "utterance", "word"));

    // all word starts/ends have same confidence rating, so CONFIDENCE_DEFAULT isn't used
    g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start

    g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE_MANUAL)); // the
    g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE_MANUAL)); // quick
    g.addAnchor(new Anchor("a02", 0.03, Constants.CONFIDENCE_MANUAL)); // brown
    g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE_MANUAL)); // fox
    g.addAnchor(new Anchor("a04a", 0.04, Constants.CONFIDENCE_MANUAL)); // fox end

    g.addAnchor(new Anchor("utteranceChange", 0.4, Constants.CONFIDENCE_MANUAL)); // utterance boundary

    g.addAnchor(new Anchor("a04b", 2.0, Constants.CONFIDENCE_MANUAL)); // jumps
    g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE_MANUAL)); // over
    g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE_MANUAL)); // a
    g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE_MANUAL)); // lazy
    g.addAnchor(new Anchor("a44", 5.1, Constants.CONFIDENCE_MANUAL)); // dog
    g.addAnchor(new Anchor("a54", null)); // end of dog

    g.addAnchor(new Anchor("turnEnd", 5.4, Constants.CONFIDENCE_MANUAL)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "participant1"));

    g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utteranceChange", "turn1"));
    g.addAnnotation(new Annotation("utterance2", "john smith", "utterance", "utteranceChange", "turnEnd", "turn1"));
      
    g.addAnnotation(new Annotation("the",   "the",   "word", "turnStart",  "a01", "turn1"));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a01", "a02", "turn1"));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a02",  "a03", "turn1"));
    g.addAnnotation(new Annotation("fox",   "fox",   "word", "a03", "utteranceChange", "turn1"));

    g.addAnnotation(new Annotation("jumps", "jumps", "word", "utteranceChange",  "a14", "turn1"));
    g.addAnnotation(new Annotation("over",  "over",  "word", "a14",  "a24", "turn1"));
    g.addAnnotation(new Annotation("a",     "a",     "word", "a24",  "a34", "turn1"));
    g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a34",  "a44", "turn1"));
    g.addAnnotation(new Annotation("dog",  "dog",    "word", "a44",  "turnEnd", "turn1"));

    g.getAnnotation("jumps").setLabel("test");
    g.createTag(g.getAnnotation("jumps"), "pos", "V");

    g.trackChanges();
    Normalizer n = new Normalizer();
    try {
      n.transform(g);
      Set<Change> changes = g.getTracker().getChanges();

      assertNotEquals("changes: " + changes, 0, changes.size());

      assertNotEquals("start changed", "turnStart", g.getAnnotation("the").getStartId());
      assertNotEquals("end changed", "utteranceChange", g.getAnnotation("fox").getEndId());
      assertNotEquals("start changed", "utteranceChange", g.getAnnotation("jumps").getStartId());
      assertNotEquals("end changed", "turnEnd", g.getAnnotation("dog").getEndId());

      assertEquals("new anchor", Change.Operation.Create, 
                   g.getAnnotation("the").getStart().getChange());
      assertEquals("new anchor", Change.Operation.Create, 
                   g.getAnnotation("fox").getEnd().getChange());
      assertEquals("new anchor", Change.Operation.Create, 
                   g.getAnnotation("jumps").getStart().getChange());
      assertEquals("new anchor", Change.Operation.Create, 
                   g.getAnnotation("dog").getEnd().getChange());
      assertNotEquals("words not linked", g.getAnnotation("jumps").getStart(), 
                      g.getAnnotation("fox").getEnd());

      assertEquals("same offset", Double.valueOf(0.0), 
                   g.getAnnotation("the").getStart().getOffset());
      assertEquals("same offset", Double.valueOf(0.4), 
                   g.getAnnotation("fox").getEnd().getOffset());
      assertEquals("same offset", Double.valueOf(0.4), 
                   g.getAnnotation("jumps").getStart().getOffset());
      assertEquals("same offset", Double.valueOf(5.4), 
                   g.getAnnotation("dog").getEnd().getOffset());

      assertEquals("same confidence", Constants.CONFIDENCE_MANUAL, 
                   g.getAnnotation("the").getStart().getConfidence().intValue());
      assertEquals("same confidence", Constants.CONFIDENCE_MANUAL, 
                   g.getAnnotation("fox").getEnd().getConfidence().intValue());
      assertEquals("same confidence", Constants.CONFIDENCE_MANUAL, 
                   g.getAnnotation("jumps").getStart().getConfidence().intValue());
      assertEquals("same confidence", Constants.CONFIDENCE_MANUAL, 
                   g.getAnnotation("dog").getEnd().getConfidence().intValue());

    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Ensure that words don't link across utterance boundaries. */
  @Test public void noInterUtteranceLinks() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");
    Layer[] layers = {
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterance")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("pos", "POS")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true)};
    g.setSchema(new Schema(layers, "who", "turn", "utterance", "word"));

    g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start

    g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE_DEFAULT)); // the
    g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE_DEFAULT)); // quick
    g.addAnchor(new Anchor("a02", null)); // brown
    g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox
    
    g.addAnchor(new Anchor("a04", 0.4, Constants.CONFIDENCE_AUTOMATIC)); // fox end, over start
    g.addAnchor(new Anchor("utteranceChange", 0.4, Constants.CONFIDENCE_MANUAL)); // utterance boundary

    g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE_AUTOMATIC)); // over
    g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE_AUTOMATIC)); // a
    g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE_AUTOMATIC)); // lazy
    g.addAnchor(new Anchor("a44", 5.1, Constants.CONFIDENCE_AUTOMATIC)); // dog
    g.addAnchor(new Anchor("a54", null)); // end of dog

    g.addAnchor(new Anchor("turnEnd", 5.4, Constants.CONFIDENCE_MANUAL)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "participant1"));

    g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utteranceChange", "turn1"));
    g.addAnnotation(new Annotation("utterance2", "john smith", "utterance", "utteranceChange", "turnEnd", "turn1"));
      
    g.addAnnotation(new Annotation("the",   "the",   "word", "a0",  "a01", "turn1"));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a01", "a02", "turn1"));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a02",  "a03", "turn1"));
    g.addAnnotation(new Annotation("fox",   "fox",   "word", "a03", "a04", "turn1"));

    g.addAnnotation(new Annotation("jumps", "jumps", "word", "a04",  "a14", "turn1"));
    g.addAnnotation(new Annotation("over",  "over",  "word", "a14",  "a24", "turn1"));
    g.addAnnotation(new Annotation("a",     "a",     "word", "a24",  "a34", "turn1"));
    g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a34",  "a44", "turn1"));
    g.addAnnotation(new Annotation("dog",  "dog",    "word", "a44",  "a54", "turn1"));

    g.getAnnotation("jumps").setLabel("test");
    g.createTag(g.getAnnotation("jumps"), "pos", "V");

    g.trackChanges();
    Normalizer n = new Normalizer();
    try {
      n.transform(g);
      Set<Change> changes = g.getTracker().getChanges();

      assertNotEquals("fox and jumps are not linked",
                      g.getAnnotation("fox").getEndId(), g.getAnnotation("jumps").getStartId());
      assertEquals("fox end has the right offset",
                   Double.valueOf(0.4), g.getAnnotation("fox").getEnd().getOffset());
      assertEquals("jumps start has the right offset",
                   Double.valueOf(0.4), g.getAnnotation("jumps").getStart().getOffset());
      assertEquals("fox end has the right confidence",
                   Constants.CONFIDENCE_AUTOMATIC,
                   g.getAnnotation("fox").getEnd().getConfidence().intValue());
      assertEquals("jumps start has the right confidence",
                   Constants.CONFIDENCE_AUTOMATIC,
                   g.getAnnotation("jumps").getStart().getConfidence().intValue());
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Ensure that words don't overflow utterance boundaries. */
  @Test public void noOverflowingUtterances() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");
    Layer[] layers = {
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterance")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("pos", "POS")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true)};
    g.setSchema(new Schema(layers, "who", "turn", "utterance", "word"));

    g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start

    g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE_DEFAULT)); // the
    g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE_DEFAULT)); // quick
    g.addAnchor(new Anchor("a02", null)); // brown
    g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox    
    g.addAnchor(new Anchor("a04a", 0.41, Constants.CONFIDENCE_DEFAULT)); // fox end
    
    g.addAnchor(new Anchor("utteranceChange", 0.4, Constants.CONFIDENCE_MANUAL)); // utterance boundary

    g.addAnchor(new Anchor("a04b", 0.39, Constants.CONFIDENCE_DEFAULT)); // over start
    g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE_AUTOMATIC)); // over
    g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE_AUTOMATIC)); // a
    g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE_AUTOMATIC)); // lazy
    g.addAnchor(new Anchor("a44", 5.1, Constants.CONFIDENCE_AUTOMATIC)); // dog
    g.addAnchor(new Anchor("a54", null)); // end of dog

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

    g.getAnnotation("jumps").setLabel("test");
    g.createTag(g.getAnnotation("jumps"), "pos", "V");

    g.trackChanges();
    Normalizer n = new Normalizer();
    try {
      n.transform(g);
      Set<Change> changes = g.getTracker().getChanges();

      assertNotEquals("fox and jumps are not linked",
                      g.getAnnotation("fox").getEndId(), g.getAnnotation("jumps").getStartId());
      assertEquals("fox end has the right offset",
                   Double.valueOf(0.4), g.getAnnotation("fox").getEnd().getOffset());
      assertEquals("jumps start has the right offset",
                   Double.valueOf(0.4), g.getAnnotation("jumps").getStart().getOffset());
      assertEquals("fox end has the right confidence",
                   Constants.CONFIDENCE_NONE,
                   g.getAnnotation("fox").getEnd().getConfidence().intValue());
      assertEquals("jumps start has the right confidence",
                   Constants.CONFIDENCE_NONE,
                   g.getAnnotation("jumps").getStart().getConfidence().intValue());
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Ensure that words don't link <b>and</b> overflow across utterance boundaries. */
  @Test public void noOverflowingInterlinkedUtterances() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");
    Layer[] layers = {
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterance")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("pos", "POS")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true)};
    g.setSchema(new Schema(layers, "who", "turn", "utterance", "word"));

    g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start

    g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE_DEFAULT)); // the
    g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE_DEFAULT)); // quick
    g.addAnchor(new Anchor("a02", null)); // brown
    g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox    
    
    g.addAnchor(new Anchor("utteranceChange", 0.4, Constants.CONFIDENCE_MANUAL)); // utterance boundary

    g.addAnchor(new Anchor("a04", 0.41, Constants.CONFIDENCE_AUTOMATIC)); // fox end, over start
    g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE_AUTOMATIC)); // over
    g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE_AUTOMATIC)); // a
    g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE_AUTOMATIC)); // lazy
    g.addAnchor(new Anchor("a44", 5.1, Constants.CONFIDENCE_AUTOMATIC)); // dog
    g.addAnchor(new Anchor("a54", null)); // end of dog

    g.addAnchor(new Anchor("turnEnd", 5.4, Constants.CONFIDENCE_MANUAL)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "participant1"));

    g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utteranceChange", "turn1"));
    g.addAnnotation(new Annotation("utterance2", "john smith", "utterance", "utteranceChange", "turnEnd", "turn1"));
      
    g.addAnnotation(new Annotation("the",   "the",   "word", "a0",  "a01", "turn1"));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a01", "a02", "turn1"));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a02",  "a03", "turn1"));
    g.addAnnotation(new Annotation("fox",   "fox",   "word", "a03", "a04", "turn1"));

    g.addAnnotation(new Annotation("jumps", "jumps", "word", "a04",  "a14", "turn1"));
    g.addAnnotation(new Annotation("over",  "over",  "word", "a14",  "a24", "turn1"));
    g.addAnnotation(new Annotation("a",     "a",     "word", "a24",  "a34", "turn1"));
    g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a34",  "a44", "turn1"));
    g.addAnnotation(new Annotation("dog",  "dog",    "word", "a44",  "a54", "turn1"));

    g.getAnnotation("jumps").setLabel("test");
    g.createTag(g.getAnnotation("jumps"), "pos", "V");

    g.trackChanges();
    Normalizer n = new Normalizer();
    try {
      n.transform(g);
      Set<Change> changes = g.getTracker().getChanges();

      assertNotEquals("fox and jumps are not linked",
                      g.getAnnotation("fox").getEndId(), g.getAnnotation("jumps").getStartId());
      assertEquals("fox end has the right offset",
                   Double.valueOf(0.4), g.getAnnotation("fox").getEnd().getOffset());
      assertEquals("jumps start has the right offset",
                   Double.valueOf(0.41), g.getAnnotation("jumps").getStart().getOffset());
      assertEquals("fox end has the right confidence",
                   Constants.CONFIDENCE_NONE,
                   g.getAnnotation("fox").getEnd().getConfidence().intValue());
      assertEquals("jumps start has the right confidence",
                   Constants.CONFIDENCE_AUTOMATIC,
                   g.getAnnotation("jumps").getStart().getConfidence().intValue());
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }
  
  /** Ensure utterances are chained within their turns. */
  @Test public void chainUtterancesWithinTurns() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");      
    Layer[] layers = {
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterance")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("pos", "POS")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true)};
    g.setSchema(new Schema(layers, "who", "turn", "utterance", "word"));

    g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start

    g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE_DEFAULT)); // the
    g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE_DEFAULT)); // quick
    g.addAnchor(new Anchor("a02", 0.03, Constants.CONFIDENCE_DEFAULT)); // brown
    g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox
    g.addAnchor(new Anchor("a04a", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox end

    // first utterance ends slightly after second utterance
    g.addAnchor(new Anchor("utteranceChange1", 0.6, Constants.CONFIDENCE_MANUAL));
    // second utterance starts slightly before first utterance
    g.addAnchor(new Anchor("utteranceChange2", 0.4, Constants.CONFIDENCE_MANUAL));

    g.addAnchor(new Anchor("a04b", 2.0, Constants.CONFIDENCE_AUTOMATIC)); // jumps
    g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE_AUTOMATIC)); // over
    g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE_AUTOMATIC)); // a
    g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE_AUTOMATIC)); // lazy
    g.addAnchor(new Anchor("a44", 5.1, Constants.CONFIDENCE_AUTOMATIC)); // dog
    g.addAnchor(new Anchor("a54", null)); // end of dog

    g.addAnchor(new Anchor("turnEnd", 5.4, Constants.CONFIDENCE_MANUAL)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
    g.addAnnotation(new Annotation("turnX", "john smith", "turn", "turnStart", "turnEnd", "participant1"));

    g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utteranceChange1", "turnX"));
    g.addAnnotation(new Annotation("utterance2", "john smith", "utterance", "utteranceChange2", "turnEnd", "turnX"));
      
    g.addAnnotation(new Annotation("the",   "the",   "word", "turnStart",  "a01", "turnX"));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a01", "a02", "turnX"));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a02",  "a03", "turnX"));
    // last utterance1 word shares end with utterance1
    g.addAnnotation(new Annotation("fox",   "fox",   "word", "a03", "utteranceChange1", "turnX"));

    // first utterance2 word shares start with utterance2
    g.addAnnotation(new Annotation("jumps", "jumps", "word", "utteranceChange2",  "a14", "turnX"));
    g.addAnnotation(new Annotation("over",  "over",  "word", "a14",  "a24", "turnX"));
    g.addAnnotation(new Annotation("a",     "a",     "word", "a24",  "a34", "turnX"));
    g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a34",  "a44", "turnX"));
    g.addAnnotation(new Annotation("dog",  "dog",    "word", "a44",  "turnEnd", "turnX"));

    g.getAnnotation("jumps").setLabel("test");
    g.createTag(g.getAnnotation("jumps"), "pos", "V");

    g.trackChanges();
    Normalizer n = new Normalizer();
    try {
      n.transform(g);
      Set<Change> changes = g.getTracker().getChanges();

      assertNotEquals("changes: " + changes, 0, changes.size());

      assertEquals("utterances share anchor", 
                   g.getAnnotation("utterance1").getEnd(), g.getAnnotation("utterance2").getStart());
      assertEquals("utterances share utterance2 anchor", 
                   "utteranceChange2", g.getAnnotation("utterance1").getEndId());

      assertNotEquals("end changed", "utteranceChange1", g.getAnnotation("fox").getEndId());
      assertNotEquals("start changed", "utteranceChange2", g.getAnnotation("jumps").getStartId());

      assertEquals("new anchor", Change.Operation.Create, 
                   g.getAnnotation("fox").getEnd().getChange());
      assertEquals("new anchor", Change.Operation.Create, 
                   g.getAnnotation("jumps").getStart().getChange());
      assertEquals("new anchor", Change.Operation.Create, 
                   g.getAnnotation("dog").getEnd().getChange());
      assertNotEquals("words not linked", g.getAnnotation("jumps").getStart(), 
                      g.getAnnotation("fox").getEnd());

      assertEquals("same offset", Double.valueOf(0.4), 
                   g.getAnnotation("fox").getEnd().getOffset());
      assertEquals("same offset", Double.valueOf(0.4), 
                   g.getAnnotation("jumps").getStart().getOffset());

      assertEquals("confidence copied from start", Constants.CONFIDENCE_DEFAULT, 
                   g.getAnnotation("fox").getEnd().getConfidence().intValue());
      assertEquals("confidence copied from end", Constants.CONFIDENCE_AUTOMATIC, 
                   g.getAnnotation("jumps").getStart().getConfidence().intValue());

    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Ensure turn/utterance labels are made equal. */
  @Test public void turnUtteranceLabels() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");      
    Layer[] layers = {
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterance")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("pos", "POS")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true)};
    g.setSchema(new Schema(layers, "who", "turn", "utterance", "word"));

    g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start

    g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE_DEFAULT)); // the
    g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE_DEFAULT)); // quick
    g.addAnchor(new Anchor("a02", 0.03, Constants.CONFIDENCE_DEFAULT)); // brown
    g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox
    g.addAnchor(new Anchor("a04a", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox end

    g.addAnchor(new Anchor("utteranceChange", 0.4, Constants.CONFIDENCE_MANUAL)); // utterance boundary

    g.addAnchor(new Anchor("a04b", 2.0, Constants.CONFIDENCE_AUTOMATIC)); // jumps
    g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE_AUTOMATIC)); // over
    g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE_AUTOMATIC)); // a
    g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE_AUTOMATIC)); // lazy
    g.addAnchor(new Anchor("a44", 5.1, Constants.CONFIDENCE_AUTOMATIC)); // dog
    g.addAnchor(new Anchor("a54", null)); // end of dog

    g.addAnchor(new Anchor("turnEnd", 5.4, Constants.CONFIDENCE_MANUAL)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
    // turn label doesn't match partipant
    g.addAnnotation(new Annotation("turn1", "Speaker 1", "turn", "turnStart", "turnEnd", "participant1"));

    // utterance labels don't match partipant
    g.addAnnotation(new Annotation("utterance1", "the quick brown fox", "utterance", "turnStart", "utteranceChange", "turn1"));
    g.addAnnotation(new Annotation("utterance2", "jumps over the lazy dog", "utterance", "utteranceChange", "turnEnd", "turn1"));
      
    g.addAnnotation(new Annotation("the",   "the",   "word", "a0",  "a01", "turn1"));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a01", "a02", "turn1"));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a02",  "a03", "turn1"));
    g.addAnnotation(new Annotation("fox",   "fox",   "word", "a03", "a04a", "turn1"));

    g.addAnnotation(new Annotation("jumps", "jumps", "word", "a04b",  "a14", "turn1"));
    g.addAnnotation(new Annotation("over",  "over",  "word", "a14",  "a24", "turn1"));
    g.addAnnotation(new Annotation("a",     "a",     "word", "a24",  "a34", "turn1"));
    g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a34",  "a44", "turn1"));
    g.addAnnotation(new Annotation("dog",  "dog",    "word", "a44",  "a54", "turn1"));

    g.getAnnotation("jumps").setLabel("test");
    g.createTag(g.getAnnotation("jumps"), "pos", "V");

    g.trackChanges();
    Normalizer n = new Normalizer();
    try {
      n.transform(g);
      Set<Change> changes = g.getTracker().getChanges();
      assertNotEquals("there are changes", 0, changes.size());

      assertEquals("turn changed: " + g.getAnnotation("turn1").getLabel(), 
                   "john smith", g.getAnnotation("turn1").getLabel());
      assertEquals("utterance changed: " + g.getAnnotation("utterance1").getLabel(), 
                   "john smith", g.getAnnotation("utterance1").getLabel());
      assertEquals("utterance changed: " + g.getAnnotation("utterance2").getLabel(), 
                   "john smith", g.getAnnotation("utterance2").getLabel());      
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Ensure utterance labels are set to the turn label. */
  @Test public void turnLabelsOnly() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");      
    Layer[] layers = {
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterance")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true)};

    // no word layer
    g.setSchema(new Schema(layers, "who", "turn", "utterance", null));

    g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start
    g.addAnchor(new Anchor("utteranceChange", 0.4, Constants.CONFIDENCE_MANUAL)); // utterance boundary
    g.addAnchor(new Anchor("turnEnd", 5.4, Constants.CONFIDENCE_MANUAL)); // turn end
    g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
    // turn label doesn't match partipant
    g.addAnnotation(new Annotation("turn1", "Speaker 1", "turn", "turnStart", "turnEnd", "participant1"));

    // utterance labels don't match partipant
    g.addAnnotation(new Annotation("utterance1", "the quick brown fox", "utterance", "turnStart", "utteranceChange", "turn1"));
    g.addAnnotation(new Annotation("utterance2", "jumps over the lazy dog", "utterance", "utteranceChange", "turnEnd", "turn1"));
      
    g.trackChanges();
    Normalizer n = new Normalizer();
    try {
      n.transform(g);
      Set<Change> changes = g.getTracker().getChanges();
      assertNotEquals("there are changes", 0, changes.size());

      assertEquals("turn changed: " + g.getAnnotation("turn1").getLabel(), 
                   "john smith", g.getAnnotation("turn1").getLabel());
      assertEquals("utterance not changed: " + g.getAnnotation("utterance1").getLabel(), 
                   "the quick brown fox", g.getAnnotation("utterance1").getLabel());
      assertEquals("utterance not changed: " + g.getAnnotation("utterance2").getLabel(), 
                   "jumps over the lazy dog", g.getAnnotation("utterance2").getLabel());      
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Ensure unnamed speaker is named after episode. */
  @Test public void nameLoneSpeakerAfterEpisode() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");      
    Layer[] layers = {
      new Layer("episode", "Transcript series")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterance")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("pos", "POS")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true)};
    g.setSchema(new Schema(layers, "who", "turn", "utterance", "word", "episode", null));
      
    g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start

    g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE_DEFAULT)); // the
    g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE_DEFAULT)); // quick
    g.addAnchor(new Anchor("a02", 0.03, Constants.CONFIDENCE_DEFAULT)); // brown
    g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox
    g.addAnchor(new Anchor("a04a", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox end

    g.addAnchor(new Anchor("utteranceChange", 0.4, Constants.CONFIDENCE_MANUAL)); // utterance boundary

    g.addAnchor(new Anchor("a04b", 2.0, Constants.CONFIDENCE_AUTOMATIC)); // jumps
    g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE_AUTOMATIC)); // over
    g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE_AUTOMATIC)); // a
    g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE_AUTOMATIC)); // lazy
    g.addAnchor(new Anchor("a44", 5.1, Constants.CONFIDENCE_AUTOMATIC)); // dog
    g.addAnchor(new Anchor("a54", null)); // end of dog

    g.addAnchor(new Anchor("turnEnd", 5.4, Constants.CONFIDENCE_MANUAL)); // turn end

    g.addAnnotation(
      new Annotation("episode1", "text 1", "episode", "turnStart", "turnEnd", "my graph"));
    g.addAnnotation(new Annotation("participant1", "", "who", "turnStart", "turnEnd", "my graph"));
      
    // turn label doesn't match partipant
    g.addAnnotation(
      new Annotation("turn1", "Main text", "turn", "turnStart", "turnEnd", "participant1"));

    // utterance labels don't match partipant
    g.addAnnotation(
      new Annotation(
        "utterance1", "line 1", "utterance", "turnStart", "utteranceChange", "turn1"));
    g.addAnnotation(
      new Annotation("utterance2", "line 2", "utterance", "utteranceChange", "turnEnd", "turn1"));
      
    g.addAnnotation(new Annotation("the",   "the",   "word", "a0",  "a01", "turn1"));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a01", "a02", "turn1"));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a02",  "a03", "turn1"));
    g.addAnnotation(new Annotation("fox",   "fox",   "word", "a03", "a04a", "turn1"));

    g.addAnnotation(new Annotation("jumps", "jumps", "word", "a04b",  "a14", "turn1"));
    g.addAnnotation(new Annotation("over",  "over",  "word", "a14",  "a24", "turn1"));
    g.addAnnotation(new Annotation("a",     "a",     "word", "a24",  "a34", "turn1"));
    g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a34",  "a44", "turn1"));
    g.addAnnotation(new Annotation("dog",  "dog",    "word", "a44",  "a54", "turn1"));

    g.getAnnotation("jumps").setLabel("test");
    g.createTag(g.getAnnotation("jumps"), "pos", "V");

    g.trackChanges();
    Normalizer n = new Normalizer();
    try {
      n.transform(g);
      Set<Change> changes = g.getTracker().getChanges();
      assertNotEquals("there are changes", 0, changes.size());

      assertEquals("participant changed: " + g.getAnnotation("participant1").getLabel(), 
                   "text 1", g.getAnnotation("participant1").getLabel());
      assertEquals("turn changed: " + g.getAnnotation("turn1").getLabel(), 
                   "text 1", g.getAnnotation("turn1").getLabel());
      assertEquals("utterance changed: " + g.getAnnotation("utterance1").getLabel(), 
                   "text 1", g.getAnnotation("utterance1").getLabel());
      assertEquals("utterance changed: " + g.getAnnotation("utterance2").getLabel(), 
                   "text 1", g.getAnnotation("utterance2").getLabel());      
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Ensure label length is correctly limited. */
  @Test public void labelLength() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");      
    Layer[] layers = {
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterance")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("pos", "POS")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true)};
    g.setSchema(new Schema(layers, "who", "turn", "utterance", "word"));

    g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start

    g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE_DEFAULT)); // the
    g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE_DEFAULT)); // quick
    g.addAnchor(new Anchor("a02", 0.03, Constants.CONFIDENCE_DEFAULT)); // brown
    g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox
    g.addAnchor(new Anchor("a04a", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox end

    g.addAnchor(
      new Anchor("utteranceChange", 0.4, Constants.CONFIDENCE_MANUAL)); // utterance boundary

    g.addAnchor(new Anchor("a04b", 2.0, Constants.CONFIDENCE_AUTOMATIC)); // jumps
    g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE_AUTOMATIC)); // over
    g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE_AUTOMATIC)); // a
    g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE_AUTOMATIC)); // lazy
    g.addAnchor(new Anchor("a44", 5.1, Constants.CONFIDENCE_AUTOMATIC)); // dog
    g.addAnchor(new Anchor("a54", null)); // end of dog

    g.addAnchor(new Anchor("turnEnd", 5.4, Constants.CONFIDENCE_MANUAL)); // turn end

    g.addAnnotation(
      new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
    g.addAnnotation(
      new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "participant1"));

    g.addAnnotation(
      new Annotation(
        "utterance1", "john smith", "utterance", "turnStart", "utteranceChange", "turn1"));
    g.addAnnotation(
      new Annotation(
        "utterance2", "john smith", "utterance", "utteranceChange", "turnEnd", "turn1"));
      
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
    Normalizer n = new Normalizer();
    n.setMaxLabelLength(4);
    try {
      n.transform(g);
      Set<Change> changes = g.getTracker().getChanges();

      assertEquals("changes: " + changes, 7, changes.size());
      assertEquals("quic", g.getAnnotation("quick").getLabel());
      assertEquals("brow", g.getAnnotation("brown").getLabel());
      assertEquals("jump", g.getAnnotation("jumps").getLabel());
      assertEquals("john", g.getAnnotation("participant1").getLabel());
      assertEquals("john", g.getAnnotation("turn1").getLabel());
      assertEquals("john", g.getAnnotation("utterance1").getLabel());
      assertEquals("john", g.getAnnotation("utterance2").getLabel());
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Test nomalization completes in a timely manner */
  @Test public void performance() {
    Graph g = new Graph();
    g.setId("g");
    g.setSchema(
      new Schema(
        "who", "turn", "utterance", "word",
        new Layer("who", "Participants")
        .setAlignment(Constants.ALIGNMENT_NONE)
        .setPeers(true).setPeersOverlap(true).setSaturated(true),
        new Layer("turn", "Speaker turns")
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false).setSaturated(false)
        .setParentId("who").setParentIncludes(true),
        new Layer("utterance", "Lines")
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false).setSaturated(true)
        .setParentId("turn").setParentIncludes(true),
        new Layer("word", "Orthographic Words")
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false).setSaturated(false)
        .setParentId("turn").setParentIncludes(true)));
    
    // create a graph with a huge number of words
    int wordCount = 10000; // got bored waiting for 10000 to finish
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
    Normalizer normalizer = new Normalizer();
    try {
      timers.start("Normalizer.transform");
      normalizer.transform(g);
      timers.end("Normalizer.transform");      

      // System.out.println(timers.toString());
      assertTrue(
        "Normalizer too slow:\n" + timers.toString(),
        30000 > timers.getTotals().get("Normalizer.transform"));      
      
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.ag.util.TestNormalizer");
  }
}
