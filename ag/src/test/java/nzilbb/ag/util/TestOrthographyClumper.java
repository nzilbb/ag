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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import nzilbb.ag.*;
import nzilbb.ag.util.*;

public class TestOrthographyClumper {
  
  /** Basic clumping: punctuation except underscore is attached to adjacent words. */
  @Test public void basicClumping() {
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

    g.addAnchor(new Anchor("a0", 0.0));
    g.addAnchor(new Anchor("a1", 1.0));
    g.addAnchor(new Anchor("a2", 2.0));
    g.addAnchor(new Anchor("a3", 3.0));
    g.addAnchor(new Anchor("a4", 4.0));
    g.addAnchor(new Anchor("a5", 5.0));
    g.addAnchor(new Anchor("a6", 6.0));
    g.addAnchor(new Anchor("a7", 7.0));
    g.addAnchor(new Anchor("a8", 8.0));
    g.addAnchor(new Anchor("a9", 9.0));
    g.addAnchor(new Anchor("a10", 10.0));
    g.addAnchor(new Anchor("a11", 11.0));
    g.addAnchor(new Anchor("a12", 12.0));
    g.addAnchor(new Anchor("a13", 13.0));
    g.addAnchor(new Anchor("a14", 14.0));
    g.addAnchor(new Anchor("a15", 15.0));
    g.addAnchor(new Anchor("a16", 16.0));

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a15", "my graph"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a15", "participant1"));

    g.addAnnotation(new Annotation("hyphen", "-", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("openquote", "\"", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("the", "the", "word", "a3", "a4", "turn1"));
    g.addAnnotation(new Annotation("openscarequote", "'", "word", "a4", "a5", "turn1"));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a5", "a6", "turn1"));
    g.addAnnotation(new Annotation("closescarequote", "'", "word", "a6", "a7", "turn1"));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a7", "a8", "turn1"));
    g.addAnnotation(new Annotation("fox", "fox", "word", "a8", "a9", "turn1"));
    g.addAnnotation(new Annotation("longpause", "--", "word", "a9", "a10", "turn1"));
    g.addAnnotation(new Annotation("jumps", "jumps", "word", "a10", "a11", "turn1"));
    g.addAnnotation(new Annotation("underscore", "_", "word", "a11", "a12", "turn1"));
    g.addAnnotation(new Annotation("over", "over", "word", "a12", "a13", "turn1"));
    g.addAnnotation(new Annotation("fullstop", ".", "word", "a13", "a14", "turn1"));
    g.addAnnotation(new Annotation("closequote", "\"", "word", "a14", "a15", "turn1"));

    try {
      g.setTracker(new ChangeTracker());
      OrthographyClumper transformer = new OrthographyClumper("word");
      transformer.transform(g);
      g.commit();
      Annotation words[] = g.list("word");
      assertEquals("- \" the '", words[0].getLabel());
      assertEquals("quick '", words[1].getLabel());
      assertEquals("brown", words[2].getLabel());
      assertEquals("fox --", words[3].getLabel());
      assertEquals("jumps", words[4].getLabel());
      assertEquals("_", words[5].getLabel());
      assertEquals("over . \"", words[6].getLabel());
      assertEquals(7, words.length);

      for (int o = 0; o < words.length; o++) {
        assertEquals("orthography corrected", o+1, words[o].getOrdinal());
        if (o > 0) {
          assertEquals("anchor linking", words[o-1].getEnd(), words[o].getStart());
        }
      }
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Clumping works only within utterances. */
  @Test public void dontClumpAcrossLines() {
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
    g.addLayer(new Layer("line", "Lines", Constants.ALIGNMENT_INTERVAL,
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

    g.addAnchor(new Anchor("a0", 0.0));
    g.addAnchor(new Anchor("a1", 1.0));
    g.addAnchor(new Anchor("a2", 2.0));
    g.addAnchor(new Anchor("a3", 3.0));
    g.addAnchor(new Anchor("a4", 4.0));
    g.addAnchor(new Anchor("a5", 5.0));
    g.addAnchor(new Anchor("a6", 6.0));
    g.addAnchor(new Anchor("a7", 7.0));
    g.addAnchor(new Anchor("a8", 8.0));
    g.addAnchor(new Anchor("a9", 9.0));
    g.addAnchor(new Anchor("a10", 10.0));
    g.addAnchor(new Anchor("a11", 11.0));
    g.addAnchor(new Anchor("a12", 12.0));
    g.addAnchor(new Anchor("a13", 13.0));
    g.addAnchor(new Anchor("a14", 14.0));
    g.addAnchor(new Anchor("a15", 15.0));

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a15", "my graph"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a15", "participant1"));

    // - " the ' quick ' brown fox 
    g.addAnnotation(new Annotation("line1", "john smith", "line", "a0", "a9", "turn1"));
    // -- jumps over . "
    g.addAnnotation(new Annotation("line2", "john smith", "line", "a9", "a15", "turn1"));
    // (the leading "--" on the second line should not be joined to "fox" on the previous

    g.addAnnotation(new Annotation("hyphen", "-", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("openquote", "\"", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("the", "the", "word", "a3", "a4", "turn1"));
    g.addAnnotation(new Annotation("openscarequote", "'", "word", "a4", "a5", "turn1"));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a5", "a6", "turn1"));
    g.addAnnotation(new Annotation("closescarequote", "'", "word", "a6", "a7", "turn1"));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a7", "a8", "turn1"));
    g.addAnnotation(new Annotation("fox", "fox", "word", "a8", "a9", "turn1"));
    g.addAnnotation(new Annotation("longpause", "--", "word", "a9", "a10", "turn1"));
    g.addAnnotation(new Annotation("jumps", "jumps", "word", "a10", "a11", "turn1"));
    g.addAnnotation(new Annotation("over", "over", "word", "a11", "a12", "turn1"));
    g.addAnnotation(new Annotation("fullstop", ".", "word", "a12", "a13", "turn1"));
    g.addAnnotation(new Annotation("closequote", "\"", "word", "a13", "a14", "turn1"));

    try {
      g.setTracker(new ChangeTracker());
      OrthographyClumper transformer = new OrthographyClumper("word", "line");
      transformer.transform(g);
      g.commit();
      Annotation words[] = g.list("word");
      assertEquals("- \" the '", words[0].getLabel());
      assertEquals("quick '", words[1].getLabel());
      assertEquals("brown", words[2].getLabel());
      assertEquals("previous line doesn't steal pause",
                   "fox", words[3].getLabel());
      assertEquals("pause is on second line",
                   "-- jumps", words[4].getLabel());
      assertEquals("over . \"", words[5].getLabel());
      assertEquals(6, words.length);

      Annotation lines[] = g.list("line");
      assertEquals("line anchors correct", "a0", lines[0].getStartId());
      assertEquals("line anchors correct", "a9", lines[0].getEndId());

      assertEquals("line anchors correct", "a9", lines[1].getStartId());
      assertEquals("line anchors correct", "a15", lines[1].getEndId());
      
      for (int o = 0; o < words.length; o++) {
        assertEquals("orhtography corrected", o+1, words[o].getOrdinal());
        if (o > 0) {
          assertEquals("anchor linking", words[o-1].getEnd(), words[o].getStart());
        }
      }
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Clumping across annotations at the end of a line. */
  @Test public void clumpAcrossAnnotations() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");

    g.addLayer(new Layer("noise", "Noise", Constants.ALIGNMENT_INTERVAL, 
                         true, // peers
                         false, // peersOverlap
                         false)); // saturated
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
    g.addLayer(new Layer("line", "Lines", Constants.ALIGNMENT_INTERVAL,
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

    g.addAnchor(new Anchor("a0", 0.0));
    g.addAnchor(new Anchor("a1", 1.0));
    g.addAnchor(new Anchor("a2", 2.0));
    g.addAnchor(new Anchor("a3", 3.0));
    g.addAnchor(new Anchor("a4", 4.0));
    g.addAnchor(new Anchor("a5", 5.0));
    g.addAnchor(new Anchor("a6", 6.0));
    g.addAnchor(new Anchor("a7", 7.0));
    g.addAnchor(new Anchor("a8", 8.0));
    g.addAnchor(new Anchor("a9", 9.0));
    g.addAnchor(new Anchor("a10", 10.0));
    g.addAnchor(new Anchor("a11", 11.0));
    g.addAnchor(new Anchor("a12", 12.0));
    g.addAnchor(new Anchor("a13", 13.0));
    g.addAnchor(new Anchor("a14", 14.0));
    g.addAnchor(new Anchor("a15", 15.0));

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a15", "my graph"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a15", "participant1"));

    // - " the ' quick ' brown fox 
    g.addAnnotation(new Annotation("line1", "john smith", "line", "a0", "a10", "turn1"));
    // -- jumps over . "
    g.addAnnotation(new Annotation("line2", "john smith", "line", "a10", "a15", "turn1"));
    // (the leading "--" on the second line should not be joined to "fox" on the previous

    g.addAnnotation(new Annotation("hyphen", "-", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("openquote", "\"", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("the", "the", "word", "a3", "a4", "turn1"));
    g.addAnnotation(new Annotation("openscarequote", "'", "word", "a4", "a5", "turn1"));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a5", "a6", "turn1"));
    g.addAnnotation(new Annotation("closescarequote", "'", "word", "a6", "a7", "turn1"));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a7", "a8", "turn1"));
    g.addAnnotation(new Annotation("noise", "cough", "noise", "a8", "a9", "turn1"));
    g.addAnnotation(new Annotation("longpause", "--", "word", "a9", "a10", "turn1"));
    // line break
    g.addAnnotation(new Annotation("jumps", "jumps", "word", "a10", "a11", "turn1"));
    g.addAnnotation(new Annotation("over", "over", "word", "a11", "a12", "turn1"));
    g.addAnnotation(new Annotation("fullstop", ".", "word", "a12", "a13", "turn1"));
    g.addAnnotation(new Annotation("closequote", "\"", "word", "a13", "a14", "turn1"));

    try {
      g.setTracker(new ChangeTracker());
      OrthographyClumper transformer = new OrthographyClumper("word", "line");
      transformer.transform(g);
      g.commit();
      Annotation words[] = g.list("word");
      assertEquals("- \" the '", words[0].getLabel());
      assertEquals("quick '", words[1].getLabel());
      assertEquals("brown", words[2].getLabel());
      assertEquals("pause is not clumped",
                   "--", words[3].getLabel());
      assertEquals("first word on second line unchanged",
                   "jumps", words[4].getLabel());
      assertEquals("over . \"", words[5].getLabel());
      assertEquals(6, words.length);

      Annotation lines[] = g.list("line");
      assertEquals("line anchors correct", "a0", lines[0].getStartId());
      assertEquals("line 1 end still the same", "a10", lines[0].getEndId());

      assertEquals("line 2 start still the same", "a10", lines[1].getStartId());
      assertEquals("line anchors correct", "a15", lines[1].getEndId());
      
      for (int o = 0; o < words.length; o++) {
        assertEquals("orthography corrected", o+1, words[o].getOrdinal());
        if (o == 3) {
          assertNotEquals("anchor not linking", words[o-1].getEnd(), words[o].getStart());
        } else if (o > 0) {
          assertEquals("anchor linking", words[o-1].getEnd(), words[o].getStart());
        }
      }
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Child annotations are moved to new parents when clumping occurs. */
  @Test public void childrenMove() {
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
    g.addLayer(new Layer("pos", "Part of speech", Constants.ALIGNMENT_NONE,
                         false, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes

    g.addAnchor(new Anchor("a0", 0.0));
    g.addAnchor(new Anchor("a1", 1.0));
    g.addAnchor(new Anchor("a2", 2.0));
    g.addAnchor(new Anchor("a3", 3.0));
    g.addAnchor(new Anchor("a4", 4.0));
    g.addAnchor(new Anchor("a5", 5.0));
    g.addAnchor(new Anchor("a6", 6.0));
    g.addAnchor(new Anchor("a7", 7.0));
    g.addAnchor(new Anchor("a8", 8.0));
    g.addAnchor(new Anchor("a9", 9.0));
    g.addAnchor(new Anchor("a10", 10.0));
    g.addAnchor(new Anchor("a11", 11.0));
    g.addAnchor(new Anchor("a12", 12.0));
    g.addAnchor(new Anchor("a13", 13.0));
    g.addAnchor(new Anchor("a14", 14.0));
    g.addAnchor(new Anchor("a15", 15.0));

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a15", "my graph"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a15", "participant1"));

    g.addAnnotation(new Annotation("hyphen", "-", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("openquote", "\"", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("the", "the", "word", "a3", "a4", "turn1"));
    g.addAnnotation(new Annotation("openscarequote", "'", "word", "a4", "a5", "turn1"));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a5", "a6", "turn1"));
    g.addAnnotation(new Annotation("closescarequote", "'", "word", "a6", "a7", "turn1"));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a7", "a8", "turn1"));
    g.addAnnotation(new Annotation("fox", "fox", "word", "a8", "a9", "turn1"));
    g.addAnnotation(new Annotation("longpause", "--", "word", "a9", "a10", "turn1"));
    g.addAnnotation(new Annotation("jumps", "jumps", "word", "a10", "a11", "turn1"));
    g.addAnnotation(new Annotation("over", "over", "word", "a11", "a12", "turn1"));
    g.addAnnotation(new Annotation("fullstop", ".", "word", "a12", "a13", "turn1"));
    g.addAnnotation(new Annotation("closequote", "\"", "word", "a13", "a14", "turn1"));

    g.addAnnotation(new Annotation("poshyphen", "pause", "pos", "a1", "a2", "hyphen"));
    g.addAnnotation(new Annotation("posopenquote", "quote", "pos", "a2", "a3", "openquote"));
    g.addAnnotation(new Annotation("posthe", "det", "pos", "a3", "a4", "the"));
    g.addAnnotation(new Annotation("posopenscarequote", "quote", "pos", "a4", "a5", "openscarequote"));
    g.addAnnotation(new Annotation("posquick", "adj", "pos", "a5", "a6", "quick"));
    g.addAnnotation(new Annotation("posclosescarequote", "quote", "pos", "a6", "a7", "closescarequote"));
    g.addAnnotation(new Annotation("posbrown", "adj", "pos", "a7", "a8", "brown"));
    g.addAnnotation(new Annotation("posfox", "n", "pos", "a8", "a9", "fox"));
    g.addAnnotation(new Annotation("poslongpause", "pause", "pos", "a9", "a10", "longpause"));
    g.addAnnotation(new Annotation("posjumps", "v", "pos", "a10", "a11", "jumps"));
    g.addAnnotation(new Annotation("posover", "adv", "pos", "a11", "a12", "over"));
    g.addAnnotation(new Annotation("posfullstop", "punct", "pos", "a12", "a13", "fullstop"));
    g.addAnnotation(new Annotation("posclosequote", "quote", "pos", "a13", "a14", "closequote"));

    try {
      g.setTracker(new ChangeTracker());
      OrthographyClumper transformer = new OrthographyClumper("word");
      transformer.transform(g);
      g.commit();
      Annotation words[] = g.list("word");
      assertEquals("- \" the '", words[0].getLabel());
      assertEquals("quick '", words[1].getLabel());
      assertEquals("brown", words[2].getLabel());
      assertEquals("fox --", words[3].getLabel());
      assertEquals("jumps", words[4].getLabel());
      assertEquals("over . \"", words[5].getLabel());
      assertEquals(6, words.length);

      HashSet<Annotation> pos = new HashSet<Annotation>(Arrays.asList(words[0].list("pos")));
      assertTrue(words[0] + " pos: " + pos, pos.contains(g.getAnnotation("poshyphen")));
      assertTrue(words[0] + " pos: "+ g.getAnnotation("posopenquote") + " " + pos, pos.contains(g.getAnnotation("posopenquote")));
      assertTrue(words[0] + " pos: " + pos, pos.contains(g.getAnnotation("posthe")));
      assertTrue(words[0] + " pos: " + pos, pos.contains(g.getAnnotation("posopenscarequote")));
      assertEquals(4, pos.size());

      pos = new HashSet<Annotation>(Arrays.asList(words[1].list("pos")));
      assertTrue(words[1] + " pos: " + pos, pos.contains(g.getAnnotation("posquick")));
      assertTrue(words[1] + " pos: " + pos, pos.contains(g.getAnnotation("posclosescarequote")));
      assertEquals(2, pos.size());

      pos = new HashSet<Annotation>(Arrays.asList(words[2].list("pos")));
      assertTrue(words[2] + " pos: " + pos, pos.contains(g.getAnnotation("posbrown")));
      assertEquals(1, pos.size());

      pos = new HashSet<Annotation>(Arrays.asList(words[3].list("pos")));
      assertTrue(words[3] + " pos: " + pos, pos.contains(g.getAnnotation("posfox")));
      assertTrue(words[3] + " pos: " + pos, pos.contains(g.getAnnotation("poslongpause")));
      assertEquals(2, pos.size());

      pos = new HashSet<Annotation>(Arrays.asList(words[4].list("pos")));
      assertTrue(words[4] + " pos: " + pos, pos.contains(g.getAnnotation("posjumps")));
      assertEquals(1, pos.size());

      pos = new HashSet<Annotation>(Arrays.asList(words[5].list("pos")));
      assertTrue(words[5] + " pos: " + pos, pos.contains(g.getAnnotation("posover")));
      assertTrue(words[5] + " pos: " + pos, pos.contains(g.getAnnotation("posfullstop")));
      assertTrue(words[5] + " pos: " + pos, pos.contains(g.getAnnotation("posclosequote")));
      assertEquals(3, pos.size());

    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Linked annotations are re-anchored when words are clumped. */
  @Test public void annotationsMove() {
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
    g.addLayer(new Layer("entity", "Named Entity", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         true, // peersOverlap
                         false, // saturated
                         "turn", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("word", "Words", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         false, // peersOverlap
                         false, // saturated
                         "turn", // parentId
                         true)); // parentIncludes

    g.addAnchor(new Anchor("a0", 0.0));
    g.addAnchor(new Anchor("a1", 1.0));
    g.addAnchor(new Anchor("a2", 2.0));
    g.addAnchor(new Anchor("a3", 3.0));
    g.addAnchor(new Anchor("a4", 4.0));
    g.addAnchor(new Anchor("a5", 5.0));
    g.addAnchor(new Anchor("a6", 6.0));
    g.addAnchor(new Anchor("a7", 7.0));
    g.addAnchor(new Anchor("a8", 8.0));
    g.addAnchor(new Anchor("a9", 9.0));
    g.addAnchor(new Anchor("a10", 10.0));
    g.addAnchor(new Anchor("a11", 11.0));
    g.addAnchor(new Anchor("a12", 12.0));
    g.addAnchor(new Anchor("a13", 13.0));
    g.addAnchor(new Anchor("a14", 14.0));
    g.addAnchor(new Anchor("a15", 15.0));

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a15", "my graph"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a15", "participant1"));

    g.addAnnotation(new Annotation("hyphen", "-", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("openquote", "\"", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("the", "the", "word", "a3", "a4", "turn1"));
    g.addAnnotation(new Annotation("openscarequote", "'", "word", "a4", "a5", "turn1"));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a5", "a6", "turn1"));
    g.addAnnotation(new Annotation("closescarequote", "'", "word", "a6", "a7", "turn1"));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a7", "a8", "turn1"));
    g.addAnnotation(new Annotation("fox", "fox", "word", "a8", "a9", "turn1"));
    g.addAnnotation(new Annotation("longpause", "--", "word", "a9", "a10", "turn1"));
    g.addAnnotation(new Annotation("jumps", "jumps", "word", "a10", "a11", "turn1"));
    g.addAnnotation(new Annotation("over", "over", "word", "a11", "a12", "turn1"));
    g.addAnnotation(new Annotation("fullstop", ".", "word", "a12", "a13", "turn1"));
    g.addAnnotation(new Annotation("closequote", "\"", "word", "a13", "a14", "turn1"));

    g.addAnnotation(new Annotation("tqbf", "THE FOX", "entity", "a3", "a9", "turn1"));
    g.addAnnotation(new Annotation("qbf", "FOX", "entity", "a5", "a9", "turn1"));
    g.addAnnotation(new Annotation("jo", "JUMPING", "entity", "a9", "a12", "turn1"));

    try {
      g.setTracker(new ChangeTracker());
      OrthographyClumper transformer = new OrthographyClumper("word");
      transformer.transform(g);
      g.commit();
      Annotation words[] = g.list("word");
      assertEquals("a1", words[0].getStartId());
      assertEquals("- \" the '", words[0].getLabel());
      assertEquals("a5", words[0].getEndId());
      assertEquals("a5", words[1].getStartId());
      assertEquals("quick '", words[1].getLabel());
      assertEquals("a7", words[1].getEndId());
      assertEquals("a7", words[2].getStartId());
      assertEquals("brown", words[2].getLabel());
      assertEquals("a8", words[2].getEndId());
      assertEquals("a8", words[3].getStartId());
      assertEquals("fox --", words[3].getLabel());
      assertEquals("a10", words[3].getEndId());
      assertEquals("a10", words[4].getStartId());
      assertEquals("jumps", words[4].getLabel());
      assertEquals("a11", words[4].getEndId());
      assertEquals("a11", words[5].getStartId());
      assertEquals("over . \"", words[5].getLabel());
      assertEquals("a14", words[5].getEndId());
      assertEquals(6, words.length);

      Annotation[] entity = g.list("entity");
      assertEquals("THE FOX", entity[0].getLabel());
      assertEquals("entity start - prepended", words[0].getStart(), entity[0].getStart());
      assertEquals("entity end - appended", words[3].getEnd(), entity[0].getEnd());
	 
      assertEquals("FOX", entity[1].getLabel());
      assertEquals("entity start - unchanged", words[1].getStart(), entity[1].getStart());
      assertEquals("entity end - appended", words[3].getEnd(), entity[1].getEnd());

      assertEquals("JUMPING", entity[2].getLabel());
      assertEquals("entity start - moved by previous move", words[4].getStart(), entity[2].getStart());
      assertEquals("entity end - appended", words[5].getEnd(), entity[2].getEnd());

    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  @Test public void avoidGaps() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");

    g.addLayer(new Layer("who", "Participants", Constants.ALIGNMENT_NONE, 
                         true, // peers
                         true, // peersOverlap
                         true)); // saturated
    g.addLayer(new Layer("noise", "Noise", Constants.ALIGNMENT_INTERVAL,
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
      
    g.addAnchor(new Anchor("a0", 0.0));
    g.addAnchor(new Anchor("a1", 1.0));
    g.addAnchor(new Anchor("a2", 2.0));
    g.addAnchor(new Anchor("a3", 3.0));
    g.addAnchor(new Anchor("a4", 4.0));
    g.addAnchor(new Anchor("a5", 5.0));
    g.addAnchor(new Anchor("a55", 5.5));
    g.addAnchor(new Anchor("a6", 6.0));
    g.addAnchor(new Anchor("a7", 7.0));
    g.addAnchor(new Anchor("a8", 8.0));
    g.addAnchor(new Anchor("a9", 9.0));
    g.addAnchor(new Anchor("a95", 9.5));
    g.addAnchor(new Anchor("a10", 10.0));
    g.addAnchor(new Anchor("a11", 11.0));
    g.addAnchor(new Anchor("a12", 12.0));
    g.addAnchor(new Anchor("a13", 13.0));
    g.addAnchor(new Anchor("a14", 14.0));
    g.addAnchor(new Anchor("a15", 15.0));

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a15", "my graph"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a15", "participant1"));

    g.addAnnotation(new Annotation("hyphen", "-", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("openquote", "\"", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("the", "the", "word", "a3", "a4", "turn1"));

    // intervening annotation
    g.addAnnotation(new Annotation("noise", "[ahem]", "noise", "a4", "a5"));

    g.addAnnotation(new Annotation("openscarequote", "'", "word", "a5", "a55", "turn1"));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a55", "a6", "turn1"));
    g.addAnnotation(new Annotation("closescarequote", "'", "word", "a6", "a7", "turn1"));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a7", "a8", "turn1"));
    g.addAnnotation(new Annotation("fox", "fox", "word", "a8", "a9", "turn1"));

    // gap here

    g.addAnnotation(new Annotation("longpause", "--", "word", "a95", "a10", "turn1"));
    g.addAnnotation(new Annotation("jumps", "jumps", "word", "a10", "a11", "turn1"));
    g.addAnnotation(new Annotation("over", "over", "word", "a11", "a12", "turn1"));
    g.addAnnotation(new Annotation("fullstop", ".", "word", "a12", "a13", "turn1"));
    g.addAnnotation(new Annotation("closequote", "\"", "word", "a13", "a14", "turn1"));

    try {
      g.setTracker(new ChangeTracker());
      OrthographyClumper transformer = new OrthographyClumper("word");
      transformer.transform(g);
      g.commit();
      Annotation words[] = g.list("word");
      assertEquals("- \" the", words[0].getLabel());

      assertEquals("' quick '", words[1].getLabel());
      assertEquals("brown", words[2].getLabel());
      assertEquals("fox", words[3].getLabel());

      assertEquals("-- jumps", words[4].getLabel());
      assertEquals("over . \"", words[5].getLabel());
      assertEquals(6, words.length);

      for (int o = 0; o < words.length; o++) {
        assertEquals("orhtography corrected", o+1, words[o].getOrdinal());
      }
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** Non-latin characters are counted as orthographic characters. */
  @Test public void nonLatin() {
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

    g.addAnchor(new Anchor("a0", 0.0));
    g.addAnchor(new Anchor("a1", 1.0));
    g.addAnchor(new Anchor("a2", 2.0));
    g.addAnchor(new Anchor("a3", 3.0));
    g.addAnchor(new Anchor("a4", 4.0));
    g.addAnchor(new Anchor("a5", 5.0));
    g.addAnchor(new Anchor("a6", 6.0));
    g.addAnchor(new Anchor("a7", 7.0));
    g.addAnchor(new Anchor("a8", 8.0));
    g.addAnchor(new Anchor("a9", 9.0));
    g.addAnchor(new Anchor("a10", 10.0));
    g.addAnchor(new Anchor("a15", 15.0));

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a15", "my graph"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a15", "participant1"));

    g.addAnnotation(new Annotation("emoji1", "그게", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("mango", "뭐", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("bay", "영원히", "word", "a3", "a4", "turn1"));
    g.addAnnotation(new Annotation("emoji2", "남아서", "word", "a4", "a5", "turn1"));
    g.addAnnotation(new Annotation("dank", "뭐", "word", "a5", "a6", "turn1"));
    g.addAnnotation(new Annotation("memes", "이제", "word", "a6", "a7", "turn1"));
    g.addAnnotation(new Annotation("leftear", "선", "word", "a7", "a8", "turn1"));
    g.addAnnotation(new Annotation("face", "보는", "word", "a8", "a9", "turn1"));
    g.addAnnotation(new Annotation("rightear", "데도", "word", "a9", "a10", "turn1"));

    try {
      g.setTracker(new ChangeTracker());
      OrthographyClumper transformer = new OrthographyClumper("word");
      transformer.transform(g);
      g.commit();
      Annotation words[] = g.list("word");
      assertEquals("그게", words[0].getLabel());
      assertEquals("뭐", words[1].getLabel());
      assertEquals("영원히", words[2].getLabel());
      assertEquals("남아서", words[3].getLabel());
      assertEquals("뭐", words[4].getLabel());
      assertEquals("이제", words[5].getLabel());
      assertEquals("선", words[6].getLabel());
      assertEquals("보는", words[7].getLabel());
      assertEquals("데도", words[8].getLabel());
      assertEquals(9, words.length);
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  /** 
   * Non-letter characters that are not punctuation are counted as orthographic characters,
   * including emoji, kaomoji, stylized letters, etc.
   */
  @Test public void nonBMPUnicode() {
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

    g.addAnchor(new Anchor("a0", 0.0));
    g.addAnchor(new Anchor("a1", 1.0));
    g.addAnchor(new Anchor("a2", 2.0));
    g.addAnchor(new Anchor("a3", 3.0));
    g.addAnchor(new Anchor("a4", 4.0));
    g.addAnchor(new Anchor("a5", 5.0));
    g.addAnchor(new Anchor("a6", 6.0));
    g.addAnchor(new Anchor("a7", 7.0));
    g.addAnchor(new Anchor("a8", 8.0));
    g.addAnchor(new Anchor("a9", 9.0));
    g.addAnchor(new Anchor("a10", 10.0));
    g.addAnchor(new Anchor("a15", 15.0));

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a15", "my graph"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a15", "participant1"));

    g.addAnnotation(new Annotation("emoji1", "🍁", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("mango", "𝓜𝓪𝓷𝓰𝓸", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("bay", "𝓑𝓪𝔂", "word", "a3", "a4", "turn1"));
    g.addAnnotation(new Annotation("emoji2", "😍", "word", "a4", "a5", "turn1"));
    g.addAnnotation(new Annotation("dank", "🅳🅰🅽🅺", "word", "a5", "a6", "turn1"));
    g.addAnnotation(new Annotation("memes", "🅼🅴🅼🅴🆂", "word", "a6", "a7", "turn1"));
    g.addAnnotation(new Annotation("leftear", "༼つ", "word", "a7", "a8", "turn1"));
    g.addAnnotation(new Annotation("face", "◕_◕", "word", "a8", "a9", "turn1"));
    g.addAnnotation(new Annotation("rightear", "༽つ", "word", "a9", "a10", "turn1"));

    try {
      g.setTracker(new ChangeTracker());
      OrthographyClumper transformer = new OrthographyClumper("word");
      transformer.transform(g);
      g.commit();
      Annotation words[] = g.list("word");
      assertEquals("emoji counts as word",
                   "🍁", words[0].getLabel());
      assertEquals("cursive word not clumped",
                   "𝓜𝓪𝓷𝓰𝓸", words[1].getLabel());
      assertEquals("cursive word not clumped",
                   "𝓑𝓪𝔂", words[2].getLabel());
      assertEquals("emoji counts as word",
                   "😍", words[3].getLabel());
      assertEquals("block word not clumped",
                   "🅳🅰🅽🅺", words[4].getLabel());
      assertEquals("block word not clumped",
                   "🅼🅴🅼🅴🆂", words[5].getLabel());
      assertEquals("extended characters not clumped",
                   "༼つ", words[6].getLabel());
      assertEquals("extended characters not clumped",
                   "◕_◕", words[7].getLabel());
      assertEquals("extended characters not clumped",
                   "༽つ", words[8].getLabel());
      assertEquals(9, words.length);
    } catch(TransformationException exception) {
      fail(exception.toString());
    }
  }

  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.ag.util.TestOrthographyClumper");
  }
}
