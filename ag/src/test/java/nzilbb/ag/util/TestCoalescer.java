//
// Copyright 2025 New Zealand Institute of Language, Brain and Behaviour, 
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
import java.util.Set;
import java.util.SortedSet;
import nzilbb.ag.*;
import nzilbb.ag.util.*;

public class TestCoalescer {

  /** Basic joining of turns works */
  @Test public void basicCoalesce() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setSchema(
      new Schema(
        "who", "turn", "utterance", "word",
        new Layer("noise", "noises")
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false).setSaturated(false),
        new Layer("who", "participants")
        .setAlignment(Constants.ALIGNMENT_NONE) 
        .setPeers(true).setPeersOverlap(true).setSaturated(true),
        new Layer("turn", "turns")
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false).setSaturated(false)
        .setParentId("who").setParentIncludes(true),
        new Layer("utterance", "utterances")
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false).setSaturated(true)
        .setParentId("turn").setParentIncludes(true),
        new Layer("phrase", "phrase")
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(true).setSaturated(false)
        .setParentId("turn").setParentIncludes(true),
        new Layer("word", "Words")
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false).setSaturated(false)
        .setParentId("turn").setParentIncludes(true)
        ));
    
    g.addAnchor(new Anchor("turn1Start", 0.0));
    g.addAnchor(new Anchor("a1", 0.0));
    g.addAnchor(new Anchor("a0", 0.0));
    g.addAnchor(new Anchor("a2", null));
    g.addAnchor(new Anchor("a22", 2.2));
    g.addAnchor(new Anchor("a3", 2.5));
    g.addAnchor(new Anchor("turn1End", 3.0));
    // short gap
    g.addAnchor(new Anchor("turn2Start", 3.1));
    g.addAnchor(new Anchor("a4", 4.0));
    g.addAnchor(new Anchor("a5", 5.0));
    g.addAnchor(new Anchor("a6", 6.0));
    g.addAnchor(new Anchor("turn2End", 7.0));
    // long gap
    g.addAnchor(new Anchor("turn3Start", 8.0));
    g.addAnchor(new Anchor("a9", 9.0));
    g.addAnchor(new Anchor("a10", 10.0));
    g.addAnchor(new Anchor("a11", 11.0));
    g.addAnchor(new Anchor("turn3End", 12.0));
    
    g.addAnnotation(new Annotation("who1", "john", "who", "turnStart", "turnEnd", "my graph"));
    g.addAnnotation(new Annotation("turn1", "john", "turn", "turn1Start", "turn1End", "who1"));
    g.addAnnotation(
      new Annotation("utt1", "john", "utterance", "turn1Start", "turn1End", "turn1"));
    g.addAnnotation(new Annotation("turn2", "john", "turn", "turn2Start", "turn2End", "who1"));
    g.addAnnotation(
      new Annotation("utt2", "john", "utterance", "turn2Start", "turn2End", "turn2"));
    g.addAnnotation(new Annotation("turn3", "john", "turn", "turn3Start", "turn3End", "who1"));
    g.addAnnotation(
      new Annotation("utt3", "john", "utterance", "turn3Start", "turn3End", "turn3"));
    
    g.addAnnotation(new Annotation("t1-1", "t1-1", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("t1-2", "t1-2", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("t2-1", "t2-1", "word", "a4", "a5", "turn2"));
    g.addAnnotation(new Annotation("t2-2", "t2-2", "word", "a5", "a6", "turn2"));
    g.addAnnotation(new Annotation("t3-1", "t3-1", "word", "a9", "a10", "turn3"));
    g.addAnnotation(new Annotation("t3-2", "t3-2", "word", "a10", "a11", "turn3"));

    // other speaker
    g.addAnnotation(new Annotation("who2", "jane", "who", "turnStart", "turnEnd", "my graph"));
    g.addAnnotation(new Annotation("turn4", "jane", "turn", "turn1Start", "turn1End", "who2"));
    g.addAnnotation(new Annotation("utt4", "jane", "utterance", "turn1Start", "turn1End", "turn4"));
    g.addAnnotation(new Annotation("t4-1", "t4-1", "word", "a0", "a22", "turn4"));

    try
    {
      g.setTracker(new ChangeTracker());
      Coalescer transformer = new Coalescer()
        .setLayerId("turn")
        .setMinimumPauseLength(0.5);
      transformer.transform(g);

      // turn 2 merged with turn 1
      assertEquals("turn2 removed",
                   Change.Operation.Destroy, g.getAnnotation("turn2").getChange());
      // anchors
      assertEquals("turn1 start unchanged",
                   "turn1Start", g.getAnnotation("turn1").getStartId());
      assertEquals("turn1 end changed",
                   "turn2End", g.getAnnotation("turn1").getEndId());
      assertEquals("turn3 start unchanged",
                   "turn3Start", g.getAnnotation("turn3").getStartId());
      assertEquals("turn3 end unchanged",
                   "turn3End", g.getAnnotation("turn3").getEndId());
      assertEquals("turn4 start unchanged",
                   "turn1Start", g.getAnnotation("turn4").getStartId());
      assertEquals("turn4 end unchanged",
                   "turn1End", g.getAnnotation("turn4").getEndId());
      
      // utterance parents
      assertEquals("utt1 parent unchanged", "turn1", g.getAnnotation("utt1").getParentId());
      assertEquals("utt2 parent changed", "turn1", g.getAnnotation("utt2").getParentId());
      assertEquals("utt3 parent unchanged", "turn3", g.getAnnotation("utt3").getParentId());
      
      assertEquals("utt4 parent unchanged", "turn4", g.getAnnotation("utt4").getParentId());
      
      // utterance anchors
      assertEquals("utt1 start unchanged", "turn1Start", g.getAnnotation("utt1").getStartId());
      assertEquals("utt1 end changed", "turn2Start", g.getAnnotation("utt1").getEndId());
      assertEquals("utt2 start unchanged", "turn2Start", g.getAnnotation("utt2").getStartId());
      assertEquals("utt2 end unchanged", "turn2End", g.getAnnotation("utt2").getEndId());
      assertEquals("utt3 start unchanged", "turn3Start", g.getAnnotation("utt3").getStartId());
      assertEquals("utt3 end unchanged", "turn3End", g.getAnnotation("utt3").getEndId());

      assertEquals("utt4 start unchanged", "turn1Start", g.getAnnotation("utt4").getStartId());
      assertEquals("utt4 end unchanged", "turn1End", g.getAnnotation("utt4").getEndId());

      // word parents
      assertEquals("t1-1 parent unchanged", "turn1", g.getAnnotation("t1-1").getParentId());
      assertEquals("t1-2 parent unchanged", "turn1", g.getAnnotation("t1-2").getParentId());
      assertEquals("t2-1 parent changed", "turn1", g.getAnnotation("t2-1").getParentId());
      assertEquals("t2-2 parent changed", "turn1", g.getAnnotation("t2-2").getParentId());
      assertEquals("t3-1 parent unchanged", "turn3", g.getAnnotation("t3-1").getParentId());
      assertEquals("t3-2 parent unchanged", "turn3", g.getAnnotation("t3-2").getParentId());

      assertEquals("t4-1 parent unchanged", "turn4", g.getAnnotation("t4-1").getParentId());
    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }
  
  /** When merging, ensure linked annotations are relinked */
  @Test public void moveLinkedAnnotations() {
    Graph g = new Graph();
    g.setId("my graph");
    g.setSchema(
      new Schema(
        "who", "turn", "utterance", "word",
        new Layer("noise", "noises")
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false).setSaturated(false),
        new Layer("who", "participants")
        .setAlignment(Constants.ALIGNMENT_NONE) 
        .setPeers(true).setPeersOverlap(true).setSaturated(true),
        new Layer("turn", "turns")
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false).setSaturated(false)
        .setParentId("who").setParentIncludes(true),
        new Layer("utterance", "utterances")
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false).setSaturated(true)
        .setParentId("turn").setParentIncludes(true),
        new Layer("phrase", "phrase")
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(true).setSaturated(false)
        .setParentId("turn").setParentIncludes(true),
        new Layer("word", "Words")
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false).setSaturated(false)
        .setParentId("turn").setParentIncludes(true)
        ));
    
    g.addAnchor(new Anchor("turn1Start", 0.0));
    g.addAnchor(new Anchor("a1", 0.0));
    g.addAnchor(new Anchor("a0", 0.0));
    g.addAnchor(new Anchor("a2", null));
    g.addAnchor(new Anchor("a22", 2.2));
    g.addAnchor(new Anchor("a3", 2.5));
    // a noise will go here
    g.addAnchor(new Anchor("turn1End", 3.0));
    // short gap
    g.addAnchor(new Anchor("turn2Start", 3.1));
    g.addAnchor(new Anchor("a4", 4.0));
    g.addAnchor(new Anchor("a5", 5.0));
    g.addAnchor(new Anchor("a6", 6.0));
    g.addAnchor(new Anchor("turn2End", 7.0));
    // long gap
    g.addAnchor(new Anchor("turn3Start", 8.0));
    g.addAnchor(new Anchor("a9", 9.0));
    g.addAnchor(new Anchor("a10", 10.0));
    g.addAnchor(new Anchor("a11", 11.0));
    g.addAnchor(new Anchor("turn3End", 12.0));
    
    g.addAnnotation(new Annotation("who1", "john", "who", "turn1Start", "turn3End", "my graph"));
    g.addAnnotation(new Annotation("turn1", "john", "turn", "turn1Start", "turn1End", "who1"));
    g.addAnnotation(
      new Annotation("utt1", "john", "utterance", "turn1Start", "turn1End", "turn1"));
    g.addAnnotation(new Annotation("turn2", "john", "turn", "turn2Start", "turn2End", "who1"));
    g.addAnnotation(
      new Annotation("utt2", "john", "utterance", "turn2Start", "turn2End", "turn2"));
    g.addAnnotation(new Annotation("turn3", "john", "turn", "turn3Start", "turn3End", "who1"));
    g.addAnnotation(
      new Annotation("utt3", "john", "utterance", "turn3Start", "turn3End", "turn3"));
    
    g.addAnnotation(new Annotation("t1-1", "t1-1", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("t1-2", "t1-2", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("t2-1", "t2-1", "word", "a4", "a5", "turn2"));
    g.addAnnotation(new Annotation("t2-2", "t2-2", "word", "a5", "a6", "turn2"));
    g.addAnnotation(new Annotation("t3-1", "t3-1", "word", "a9", "a10", "turn3"));
    g.addAnnotation(new Annotation("t3-2", "t3-2", "word", "a10", "a11", "turn3"));

    // other speaker
    g.addAnnotation(new Annotation("who2", "jane", "who", "turn1Start", "turn3End", "my graph"));
    g.addAnnotation(new Annotation("turn4", "jane", "turn", "turn1Start", "turn1End", "who2"));
    g.addAnnotation(new Annotation("utt4", "jane", "utterance", "turn1Start", "turn1End", "turn4"));
    g.addAnnotation(new Annotation("t4-1", "t4-1", "word", "a0", "a22", "turn4"));

    // add noise between t1-2 end and turn1 end
    g.addAnnotation(new Annotation("noise", "noise", "noise", "a3", "turn1End", "turn1"));
    
    try
    {
      g.setTracker(new ChangeTracker());
      Coalescer transformer = new Coalescer()
        .setLayerId("turn")
        .setMinimumPauseLength(0.5);
      transformer.transform(g);

      // turn 2 merged with turn 1
      assertEquals("turn2 removed",
                   Change.Operation.Destroy, g.getAnnotation("turn2").getChange());
      // anchors
      assertEquals("turn1 start unchanged",
                   "turn1Start", g.getAnnotation("turn1").getStartId());
      assertEquals("turn1 end changed",
                   "turn2End", g.getAnnotation("turn1").getEndId());
      assertEquals("turn3 start unchanged",
                   "turn3Start", g.getAnnotation("turn3").getStartId());
      assertEquals("turn3 end unchanged",
                   "turn3End", g.getAnnotation("turn3").getEndId());
      assertEquals("turn4 start unchanged",
                   "turn1Start", g.getAnnotation("turn4").getStartId());
      assertEquals("turn4 end unchanged",
                   "turn1End", g.getAnnotation("turn4").getEndId());
      
      // utterance parents
      assertEquals("utt1 parent unchanged", "turn1", g.getAnnotation("utt1").getParentId());
      assertEquals("utt2 parent changed", "turn1", g.getAnnotation("utt2").getParentId());
      assertEquals("utt3 parent unchanged", "turn3", g.getAnnotation("utt3").getParentId());
      
      assertEquals("utt4 parent unchanged", "turn4", g.getAnnotation("utt4").getParentId());
      
      // utterance anchors
      assertEquals("utt1 start unchanged", "turn1Start", g.getAnnotation("utt1").getStartId());
      assertEquals("utt1 end changed", "turn2Start", g.getAnnotation("utt1").getEndId());
      assertEquals("utt2 start unchanged", "turn2Start", g.getAnnotation("utt2").getStartId());
      assertEquals("utt2 end unchanged", "turn2End", g.getAnnotation("utt2").getEndId());
      assertEquals("utt3 start unchanged", "turn3Start", g.getAnnotation("utt3").getStartId());
      assertEquals("utt3 end unchanged", "turn3End", g.getAnnotation("utt3").getEndId());

      assertEquals("utt4 start unchanged", "turn1Start", g.getAnnotation("utt4").getStartId());
      assertEquals("utt4 end unchanged", "turn1End", g.getAnnotation("utt4").getEndId());

      // word parents
      assertEquals("t1-1 parent unchanged", "turn1", g.getAnnotation("t1-1").getParentId());
      assertEquals("t1-2 parent unchanged", "turn1", g.getAnnotation("t1-2").getParentId());
      assertEquals("t2-1 parent changed", "turn1", g.getAnnotation("t2-1").getParentId());
      assertEquals("t2-2 parent changed", "turn1", g.getAnnotation("t2-2").getParentId());
      assertEquals("t3-1 parent unchanged", "turn3", g.getAnnotation("t3-1").getParentId());
      assertEquals("t3-2 parent unchanged", "turn3", g.getAnnotation("t3-2").getParentId());

      assertEquals("t4-1 parent unchanged", "turn4", g.getAnnotation("t4-1").getParentId());
      
      // noise relinked
      assertEquals("noise end also moved", "turn2Start", g.getAnnotation("noise").getEndId());
    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }
  
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.ag.util.TestCoalescer");
  }
}

