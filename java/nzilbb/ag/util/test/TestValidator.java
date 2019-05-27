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

public class TestValidator
{
      
  @Test public void valid() 
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
    g.addLayer(new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("pos", "Part of speech", Constants.ALIGNMENT_NONE,
                         false, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("phrase", "Phrase structure", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         true, // peersOverlap
                         false, // saturated
                         "turn", // parentId
                         true)); // parentIncludes

    g.addAnchor(new Anchor("a0", 0.0)); // turn start
    g.addAnchor(new Anchor("a1", 1.0)); // the & DT & D & NP
    g.addAnchor(new Anchor("a1.5", 1.5)); // @
    g.addAnchor(new Anchor("a2", 2.0)); // quick & A & k & AP
    g.addAnchor(new Anchor("a2.25", 2.25)); // w
    g.addAnchor(new Anchor("a2.5", 2.5)); // I
    g.addAnchor(new Anchor("a2.75", 2.75)); // k
    g.addAnchor(new Anchor("a3", 3.0)); // brown
    g.addAnchor(new Anchor("a4", 4.0)); // fox & N
    // unset offsets
    g.addAnchor(new Anchor("a?1", null)); // jumps
    g.addAnchor(new Anchor("a?2", null)); // over
    g.addAnchor(new Anchor("a5", 5.0)); // end of over
    g.addAnchor(new Anchor("a6", 6.0)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a6", "my graph"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a6", "participant1"));

    g.addAnnotation(new Annotation("phrase1", "NP", "phrase", "a1", "a4", "turn1"));
    g.addAnnotation(new Annotation("phrase2", "AP", "phrase", "a2", "a4", "turn1"));

    g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("word2", "quick", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("word3", "brown", "word", "a3", "a4", "turn1"));
    g.addAnnotation(new Annotation("word4", "fox", "word", "a4", "a?1", "turn1"));
    g.addAnnotation(new Annotation("word5", "jumps", "word", "a?1", "a?2", "turn1"));
    g.addAnnotation(new Annotation("word6", "over", "word", "a?2", "a5", "turn1"));

    g.addAnnotation(new Annotation("pos1", "DT", "pos", "a1", "a2", "word1"));
    g.addAnnotation(new Annotation("pos2", "A", "pos", "a2", "a3", "word2"));
    g.addAnnotation(new Annotation("pos3", "N", "pos", "a4", "a?1", "word4"));

    g.addAnnotation(new Annotation("phone1", "D", "phone", "a1", "a1.5", "word1"));
    g.addAnnotation(new Annotation("phone2", "@", "phone", "a1.5", "a2", "word1"));
    g.addAnnotation(new Annotation("phone3", "k", "phone", "a2", "a2.25", "word2"));
    g.addAnnotation(new Annotation("phone4", "w", "phone", "a2.25", "a2.5", "word2"));
    g.addAnnotation(new Annotation("phone5", "I", "phone", "a2.5", "a2.75", "word2"));
    g.addAnnotation(new Annotation("phone6", "k", "phone", "a2.75", "a3", "word2"));

    Validator v = new Validator();
    v.setFullValidation(true);
    // v.setDebug(true);
    v.setDefaultOffsetThreshold(null);
    try
    {
      Vector<Change> changes = v.transform(g);
      if (v.getLog() != null) for (String m : v.getLog()) System.out.println(m);
      assertEquals("no changes to valid graph", 0, changes.size());
      assertEquals("no extra changes to graph", changes.size(), g.getChanges().size());
    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }

  @Test public void reconcileOrphansNewParents() 
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
    g.addLayer(new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("pos", "Part of speech", Constants.ALIGNMENT_NONE,
                         false, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("phrase", "Phrase structure", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         true, // peersOverlap
                         false, // saturated
                         "turn", // parentId
                         true)); // parentIncludes

    g.addAnchor(new Anchor("a0", 0.0)); // turn1 start
    g.addAnchor(new Anchor("a1", 1.0)); // the & DT & D & NP
    g.addAnchor(new Anchor("a1.5", 1.5)); // @
    g.addAnchor(new Anchor("a2", 2.0)); // quick & A & k & AP
    g.addAnchor(new Anchor("a2.25", 2.25)); // w
    g.addAnchor(new Anchor("a2.5", 2.5)); // I
    g.addAnchor(new Anchor("a2.75", 2.75)); // k
    g.addAnchor(new Anchor("a3a", 3.0)); // end of quick

    g.addAnchor(new Anchor("a3b", 3.0)); // turn1 end & turn2 start

    g.addAnchor(new Anchor("a3c", 3.0)); // brown
    g.addAnchor(new Anchor("a4a", 4.0)); // end of brown
    g.addAnchor(new Anchor("a4b", 4.0)); // turn3 start
    g.addAnchor(new Anchor("a4c", 4.0)); // fox & N
    g.addAnchor(new Anchor("a4.125", 4.125)); // end of fox & N

    g.addAnchor(new Anchor("a4.25", 4.25)); // turn2 end

    g.addAnchor(new Anchor("a4.5", 4.5)); // jumps
    g.addAnchor(new Anchor("a4.75", 4.75)); // over
    g.addAnchor(new Anchor("a5", 5.0)); // end of over
    g.addAnchor(new Anchor("a6", 6.0)); // turn3 end

    // null participant anchors - will be corrected below
    g.addAnnotation(new Annotation("participant1", "john smith", "who", null, null, "my graph"));
    g.addAnnotation(new Annotation("participant2", "jane doe", "who", null, null, "my graph"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a3b", "participant1"));
    g.addAnnotation(new Annotation("turn2", "jane doe", "turn", "a3b", "a4.25", "participant2"));
    g.addAnnotation(new Annotation("turn3", "john smith", "turn", "a4b", "a6", "participant1"));

    g.addAnnotation(new Annotation("phrase1", "AP", "phrase", "a2", "a3a", "turn1"));
    g.addAnnotation(new Annotation("phrase2", "NP", "phrase", "a3c", "a4.125", "turn2"));

    g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("word2", "quick", "word", "a2", "a3a", "turn1"));

    // wrong turn and speaker
    g.addAnnotation(new Annotation("word3", "brown", "word", "a3b", "a4a", "turn1"));

    // wrong turn, two possible candidates for new turn 
    g.addAnnotation(new Annotation("word4", "fox", "word", "a4c", "a4.125", "turn1"));

    // wrong turn
    g.addAnnotation(new Annotation("word5", "jumps", "word", "a4.5", "a4.75", "turn1"));

    // turn on wrong layer
    g.addAnnotation(new Annotation("word6", "over", "word", "a4.75", "a5", "participant1"));
    assertEquals("participant1", g.getAnnotation("word6").getParentId());

    g.addAnnotation(new Annotation("pos1", "DT", "pos", "a1", "a2", "word1"));
    g.getAnnotation("pos1").setConfidence(Constants.CONFIDENCE_AUTOMATIC);
    g.addAnnotation(new Annotation("pos2", "A", "pos", "a2", "a3a", "word2"));
    g.getAnnotation("pos2").setConfidence(Constants.CONFIDENCE_AUTOMATIC);
    g.addAnnotation(new Annotation("pos3", "N", "pos", "a4c", "a4.125", "word4"));
    g.getAnnotation("pos3").setConfidence(Constants.CONFIDENCE_AUTOMATIC);

    g.addAnnotation(new Annotation("phone1", "D", "phone", "a1", "a1.5", "word1"));
    g.addAnnotation(new Annotation("phone2", "@", "phone", "a1.5", "a2", "word1"));
    g.addAnnotation(new Annotation("phone3", "k", "phone", "a2", "a2.25", "word2"));
    g.addAnnotation(new Annotation("phone4", "w", "phone", "a2.25", "a2.5", "word2"));
    g.addAnnotation(new Annotation("phone5", "I", "phone", "a2.5", "a2.75", "word2"));
    g.addAnnotation(new Annotation("phone6", "k", "phone", "a2.75", "a3a", "word2"));

    assertEquals("no initial changes to graph: " + g.getChanges(), 0, g.getChanges().size());

    assertEquals(4, g.getAnnotation("word4").getOrdinal());
    assertEquals("participant1", g.getAnnotation("word6").getParentId());
    assertEquals("parent on wrong layer - ordinal not set",
                 0, g.getAnnotation("word6").getAssignedOrdinal());

    Validator v = new Validator();
    // v.setDebug(true);
    v.setFullValidation(true);
    v.setDefaultOffsetThreshold(null);
    try
    {
      Vector<Change> changes = v.transform(g);
      if (v.getLog() != null) for (String m : v.getLog()) System.out.println(m);
      Iterator<Change> order = changes.iterator();
      assertEquals("moved to new turn - different speaker", 
                   new Change(Change.Operation.Update, g.getAnnotation("word3"), "parentId", "turn2"), 
                   order.next());
      assertEquals("moved to new turn - update ordinal", 
                   new Change(Change.Operation.Update, g.getAnnotation("word3"), "ordinal", new Integer(1)), 
                   order.next());
      assertEquals("moved to new turn - prefer same speaker", 
                   new Change(Change.Operation.Update, g.getAnnotation("word4"), "parentId", "turn3"), 
                   order.next());
      assertEquals(1, g.getAnnotation("word4").getOrdinal());
      assertEquals("moved to new turn - update ordinal", 
                   new Change(Change.Operation.Update, g.getAnnotation("word4"), "ordinal", new Integer(1)), 
                   order.next());
      assertEquals("moved to new turn - same speaker", 
                   new Change(Change.Operation.Update, g.getAnnotation("word5"), "parentId", "turn3"), 
                   order.next());
      assertEquals("moved to new turn - update ordinal", 
                   new Change(Change.Operation.Update, g.getAnnotation("word5"), "ordinal", new Integer(2)), 
                   order.next());
      assertEquals("" + g.getAnnotation("word6").getChange(), "turn3", g.getAnnotation("word6").getParentId());
      assertEquals("parent on wrong layer", 
                   new Change(Change.Operation.Update, g.getAnnotation("word6"), "parentId", "turn3"), 
                   order.next());
      assertEquals(3, g.getAnnotation("word6").getOrdinal());
      // setting the ordinal doesn't count as a change, because ordinal is not set in the first place
      // assertEquals("parent on wrong layer - update ordinal", 
      // 	      new Change(Change.Operation.Update, g.getAnnotation("word6"), "ordinal", new Integer(3)), 
      // 	      order.next());
      assertFalse(order.hasNext());
      assertEquals("no extra changes to graph - " + changes + " vs. " +g.getChanges(), changes.size(), g.getChanges().size());
    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }

  @Test public void reconcileOrphansDeleteChildren() 
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
    g.addLayer(new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("pos", "Part of speech", Constants.ALIGNMENT_NONE,
                         true, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("phrase", "Phrase structure", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         true, // peersOverlap
                         false, // saturated
                         "turn", // parentId
                         true)); // parentIncludes

    g.addAnchor(new Anchor("a0", 0.0)); // turn start
    g.addAnchor(new Anchor("a1", 1.0)); // the & DT & D & NP
    g.addAnchor(new Anchor("a1.5", 1.5)); // @
    g.addAnchor(new Anchor("a2", 2.0)); // quick & A & k & AP
    g.addAnchor(new Anchor("a2.25", 2.25)); // w
    g.addAnchor(new Anchor("a2.5", 2.5)); // I
    g.addAnchor(new Anchor("a2.75", 2.75)); // k
    g.addAnchor(new Anchor("a3", 3.0)); // brown
    g.addAnchor(new Anchor("a4", 4.0)); // fox & N
    // unset offsets
    g.addAnchor(new Anchor("a?1", null)); // jumps
    g.addAnchor(new Anchor("a?2", null)); // over
    g.addAnchor(new Anchor("a5", 5.0)); // end of over
    g.addAnchor(new Anchor("a6", 6.0)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a6", "my graph"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a6", "participant1"));

    g.addAnnotation(new Annotation("phrase1", "NP", "phrase", "a1", "a4", "turn1"));
    g.addAnnotation(new Annotation("phrase2", "AP", "phrase", "a2", "a4", "turn1"));

    g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("word2", "quick", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("word3", "brown", "word", "a3", "a4", "turn1"));
    g.addAnnotation(new Annotation("word4", "fox", "word", "a4", "a?1", "turn1"));
    g.addAnnotation(new Annotation("word5", "jumps", "word", "a?1", "a?2", "turn1"));
    g.addAnnotation(new Annotation("word6", "over", "word", "a?2", "a5", "turn1"));

    g.addAnnotation(new Annotation("pos1", "DT", "pos", "a1", "a2", "word1"));
    g.getAnnotation("pos1").setConfidence(Constants.CONFIDENCE_AUTOMATIC);
    g.addAnnotation(new Annotation("pos2", "A", "pos", "a2", "a3", "word2"));
    g.getAnnotation("pos2").setConfidence(Constants.CONFIDENCE_AUTOMATIC);
    g.addAnnotation(new Annotation("pos3", "N", "pos", "a4", "a?1", "word4"));
    g.getAnnotation("pos3").setConfidence(Constants.CONFIDENCE_AUTOMATIC);

    // manual child - not deleted
    g.addAnnotation(new Annotation("pos4", "ADV", "pos", "a?2", "a5", "word6"));
    g.getAnnotation("pos4").setConfidence(Constants.CONFIDENCE_MANUAL);
    assertEquals("a?2", g.getAnnotation("pos4").getStartId());
    assertNull(g.getAnnotation("pos4").get("originalStartId"));
    // automatic child - deleted
    g.addAnnotation(new Annotation("pos5", "deleteme", "pos", "a?2", "a5", "word6"));
    g.getAnnotation("pos5").setConfidence(Constants.CONFIDENCE_AUTOMATIC);

    g.addAnnotation(new Annotation("phone1", "D", "phone", "a1", "a1.5", "word1"));
    g.addAnnotation(new Annotation("phone2", "@", "phone", "a1.5", "a2", "word1"));
    g.addAnnotation(new Annotation("phone3", "k", "phone", "a2", "a2.25", "word2"));
    g.addAnnotation(new Annotation("phone4", "w", "phone", "a2.25", "a2.5", "word2"));
    g.addAnnotation(new Annotation("phone5", "I", "phone", "a2.5", "a2.75", "word2"));
    g.addAnnotation(new Annotation("phone6", "k", "phone", "a2.75", "a3", "word2"));

    // delete a word with children
    g.getAnnotation("word6").destroy();

    Validator v = new Validator();
    v.setFullValidation(true);
    // v.setDebug(true);
    v.setDefaultOffsetThreshold(null);
    try
    {
      Vector<Change> changes = v.transform(g);
      if (v.getLog() != null) for (String m : v.getLog()) System.out.println(m);
      assertEquals("a?1", g.getAnnotation("pos4").getStartId());
      assertEquals("a?2", g.getAnnotation("pos4").get("originalStartId"));
      assertEquals("a?2", g.getAnnotation("pos4").getOriginalStartId());

      Iterator<Change> order = changes.iterator();
      assertEquals("Delete word - manual child has new parent", 
                   new Change(Change.Operation.Update, g.getAnnotation("pos4"), "parentId", "word5"), 
                   order.next());
      assertEquals("Delete word - manual child shares start", 
                   new Change(Change.Operation.Update, g.getAnnotation("pos4"), "startId", "a?1"), 
                   order.next());
      assertEquals("Delete word - manual child shares end", 
                   new Change(Change.Operation.Update, g.getAnnotation("pos4"), "endId", "a?2"), 
                   order.next());
      assertEquals("Delete word - automatically generated child is deleted", 
                   new Change(Change.Operation.Destroy, g.getAnnotation("pos5")), 
                   order.next());
      assertFalse("Delete word - manual child is not deleted " + changes, order.hasNext());
      assertEquals("one extra change in graph - the word deletion - " + g.getChanges(), 
                   changes.size() + 1, g.getChanges().size());
    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }

  @Test public void validateHierarchyOrdinalsAndDeletion() 
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
    g.addLayer(new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("pos", "Part of speech", Constants.ALIGNMENT_NONE,
                         false, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("phrase", "Phrase structure", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         true, // peersOverlap
                         false, // saturated
                         "turn", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("role", "Role", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         true, // peersOverlap
                         false, // saturated
                         "word", // parentId
                         false)); // parentIncludes

    g.addAnchor(new Anchor("a0", 0.0)); // turn start
    g.addAnchor(new Anchor("a1", 1.0)); // the & DT & NP
    g.addAnchor(new Anchor("a2", 2.0)); // quick & A & AP
    g.addAnchor(new Anchor("a3", 3.0)); // brown
    g.addAnchor(new Anchor("a4", 4.0)); // fox & N
    // unset offsets
    g.addAnchor(new Anchor("a?1", null)); // jumps
    g.addAnchor(new Anchor("a?2", null)); // over
    g.addAnchor(new Anchor("a5", 5.0)); // end of over
    g.addAnchor(new Anchor("a6", 6.0)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a6", "my graph"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a6", "participant1"));

    g.addAnnotation(new Annotation("phrase1", "NP", "phrase", "a1", "a4", "turn1"));
    g.addAnnotation(new Annotation("phrase2", "AP", "phrase", "a2", "a4", "turn1"));

    g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1", 1));

    // add words out of order to ensure their ordinals are corrected
    g.addAnnotation(new Annotation("word3", "brown", "word", "a3", "a4", "turn1"));
    g.addAnnotation(new Annotation("word2", "quick", "word", "a2", "a3", "turn1"));

    g.addAnnotation(new Annotation("word4", "fox", "word", "a4", "a?1", "turn1", 4));
    g.addAnnotation(new Annotation("word5", "jumps", "word", "a?1", "a?2", "turn1", 5));
    g.addAnnotation(new Annotation("word6", "over", "word", "a?2", "a5", "turn1", 6));

    g.addAnnotation(new Annotation("pos1", "DT", "pos", "a1", "a2", "word1"));
    g.addAnnotation(new Annotation("pos2", "A", "pos", "a2", "a3", "word2"));

    // non-chronological dependencies do not have their ordinals corrected
    g.addAnnotation(new Annotation("role1", "object", "role", "a?2", "a5", "word5")); // over
    g.addAnnotation(new Annotation("role2", "subject", "role", "a4", "a?1", "word5")); // fox

    // delete a word to ensure subsequent ordinals are updated
    assertEquals(5, g.getAnnotation("word5").getAssignedOrdinal());
    assertEquals(5, g.getAnnotation("word5").getOrdinal());
    g.getAnnotation("word4").destroy();
    assertEquals(5, g.getAnnotation("word5").getAssignedOrdinal());

    Validator v = new Validator();
    v.setFullValidation(true);
    // v.setDebug(true);
    v.setDefaultOffsetThreshold(null);
    try
    {
      Vector<Change> changes = v.transform(g);
      if (v.getLog() != null) for (String m : v.getLog()) System.out.println(m);
      Iterator<Change> order = changes.iterator();
      assertEquals("children out of order - update ordinal", 
                   new Change(Change.Operation.Update, g.getAnnotation("word2"), "ordinal", new Integer(2)), 
                   order.next());
      // assertEquals("children out of order - update ordinal", 
      // 	      new Change(Change.Operation.Update, g.getAnnotation("word3"), "ordinal", new Integer(3)), 
      // 	      order.next());
      assertEquals(4, g.getAnnotation("word5").getOrdinal());
      // deletion should cause updated ordinals of subsequent annotations, but this is actually
      // handled by Annotation itself before it gets to the Validator
      assertFalse("non-included children are not ordinal-corrected", order.hasNext());
      assertEquals(4, g.getAnnotation("word5").getOrdinal());
      assertEquals(5, g.getAnnotation("word6").getOrdinal());
      assertTrue("update ordinal after deleted annotation", 
                 g.getChanges().contains(
                   new Change(Change.Operation.Update, g.getAnnotation("word5"), 
                              "ordinal", new Integer(4))));
      assertTrue("update ordinal after deleted annotation", 
                 g.getChanges().contains(
                   new Change(Change.Operation.Update, g.getAnnotation("word6"), 
                              "ordinal", new Integer(5))));
      // assertEquals("three extra changes to graph, the deletion and two ordinal updates" + g.getChanges(), changes.size() + 3, g.getChanges().size());
    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
      
  }

  @Test public void validateHierarchyOverlappingChildren()
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
    g.addLayer(new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("pos", "Part of speech", Constants.ALIGNMENT_NONE,
                         false, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("phrase", "Phrase structure", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         true, // peersOverlap
                         false, // saturated
                         "turn", // parentId
                         true)); // parentIncludes

    g.addAnchor(new Anchor("a0", 0.0)); // turn1 start
    g.addAnchor(new Anchor("a1", 1.0)); // the & DT & D & NP
    g.addAnchor(new Anchor("a1.5", 1.5)); // @
    g.addAnchor(new Anchor("a2", 2.0)); // quick & A & k & AP
    g.addAnchor(new Anchor("a2.25", 2.25)); // w
    g.addAnchor(new Anchor("a2.5", 2.5)); // I
    g.addAnchor(new Anchor("a2.75", 2.75)); // k
    g.addAnchor(new Anchor("a3a", 3.0)); // end of quick

    g.addAnchor(new Anchor("a3b", 3.0)); // turn1 end & turn2 start

    g.addAnchor(new Anchor("a3.5", 3.5)); // brown
    g.addAnchor(new Anchor("a3.625", 3.625)); // turn3 start
    g.addAnchor(new Anchor("a3.75", 3.75)); // jumps
    // end of brown, start of fox & N
    // also end of jumps, start of over, must be split from brown/fox
    g.addAnchor(new Anchor("a4", 4.0)); // ...will be split into two
    g.addAnchor(new Anchor("a4.125", 4.125)); // end of fox & N

    g.addAnchor(new Anchor("a4.25", 4.25)); // turn2 end

    g.addAnchor(new Anchor("a5", 5.0)); // end of over
    g.addAnchor(new Anchor("a6", 6.0)); // turn3 end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a6", "my graph"));
    g.addAnnotation(new Annotation("participant2", "jane doe", "who", "a0", "a6", "my graph"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a3b", "participant1"));
    g.addAnnotation(new Annotation("turn2", "jane doe", "turn", "a3b", "a4.25", "participant2"));
    g.addAnnotation(new Annotation("turn3", "john smith", "turn", "a3.625", "a6", "participant1"));

    // these two share ends, but aren't split
    g.addAnnotation(new Annotation("phrase1", "NP", "phrase", "a1", "a3a", "turn1"));
    g.addAnnotation(new Annotation("phrase2", "AP", "phrase", "a2", "a3a", "turn1"));

    g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("word2", "quick", "word", "a2", "a3a", "turn1"));
    // shares end with jumps:
    g.addAnnotation(new Annotation("word3", "brown", "word", "a3.5", "a4", "turn2")); 
    // shares start with over;
    g.addAnnotation(new Annotation("word4", "fox", "word", "a4", "a4.125", "turn2")); 
    // shares end with brown
    g.addAnnotation(new Annotation("word5", "jumps", "word", "a3.75", "a4", "turn3")); 
    // shares start with fox
    g.addAnnotation(new Annotation("word6", "over", "word", "a4", "a5", "turn3")); 

    g.addAnnotation(new Annotation("pos1", "DT", "pos", "a1", "a2", "word1"));
    g.addAnnotation(new Annotation("pos2", "A", "pos", "a2", "a3a", "word2"));
    g.addAnnotation(new Annotation("pos3", "N", "pos", "a4", "a4.125", "word4"));

    g.addAnnotation(new Annotation("phone1", "D", "phone", "a1", "a1.5", "word1"));
    g.addAnnotation(new Annotation("phone2", "@", "phone", "a1.5", "a2", "word1"));
    g.addAnnotation(new Annotation("phone3", "k", "phone", "a2", "a2.25", "word2"));
    g.addAnnotation(new Annotation("phone4", "w", "phone", "a2.25", "a2.5", "word2"));
    g.addAnnotation(new Annotation("phone5", "I", "phone", "a2.5", "a2.75", "word2"));
    g.addAnnotation(new Annotation("phone6", "k", "phone", "a2.75", "a3a", "word2"));

    assertEquals("no extra changes to graph", 0, g.getChanges().size());

    Validator v = new Validator();
    v.setFullValidation(true);
    // v.setDebug(true);
    v.setDefaultOffsetThreshold(null);
    try
    {
      Vector<Change> changes = v.transform(g);
      if (v.getLog() != null) for (String m : v.getLog()) System.out.println(m);
      Iterator<Change> order = changes.iterator();
	 
      assertEquals("word share start anchors - new anchor: " + changes, 
                   new Change(Change.Operation.Create, g.getAnchor("1")), 
                   order.next());
      assertEquals("word share start anchors - new anchor copies offset: " + changes, 
                   new Change(Change.Operation.Update, g.getAnchor("1"), "offset", new Double(4.0)), 
                   order.next());
      assertNotEquals("word share start anchors - not shared any more: " + changes, 
                      g.getAnnotation("word6").getStartId(), 
                      g.getAnnotation("word4").getStartId());
      assertEquals("word share start anchors - tag not shared any more either: " + changes, 
                   g.getAnnotation("word4").get("startId"), 
                   g.getAnnotation("pos3").get("startId"));
      assertEquals("word share start anchors - new start: " + changes, 
                   new Change(Change.Operation.Update, g.getAnnotation("word4"), "startId", "1"), 
                   order.next());
      assertNotEquals("word share end anchors - not shared any more: " + changes, 
                      g.getAnnotation("word5").getEndId(), 
                      g.getAnnotation("word4").getEndId());
      assertEquals("word tag share start anchors - new start: " + changes, 
                   new Change(Change.Operation.Update, g.getAnnotation("pos3"), "startId", "1"), 
                   order.next());
      assertEquals("word share start anchors - new end: " + changes, 
                   new Change(Change.Operation.Update, g.getAnnotation("word3"), "endId", "1"), 
                   order.next());

      assertFalse("no more changes " + changes, order.hasNext());
      assertEquals("no extra changes to graph", changes.size(), g.getChanges().size());
    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }

  @Test public void validateHierarchyAnchorsOutOfSequence()
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
    g.addLayer(new Layer("pos", "Part of speech", Constants.ALIGNMENT_NONE,
                         false, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes
      
    g.addAnchor(new Anchor("a0", 0.0)); // turn start
    g.addAnchor(new Anchor("a1", 1.0)); // the & D
    g.addAnchor(new Anchor("a1.5", 1.5)); // @
    g.addAnchor(new Anchor("a2", 2.0)); // quick & k
    g.addAnchor(new Anchor("a2.25", 2.25)); // w
    // start of brown is between the last and the next
    g.addAnchor(new Anchor("a2.5", 2.5)); // I
    g.addAnchor(new Anchor("a2.75", 2.75)); // k
    g.addAnchor(new Anchor("a3", 3.0)); // quick end
    // brown starts before quick ends, in the middle of I
    g.addAnchor(new Anchor("a2.6", 2.6)); // brown & b
    g.addAnchor(new Anchor("a2.8", 2.8)); // r
    // end of quick is between the last and the next
    g.addAnchor(new Anchor("a3.2", 3.2)); // ow
    g.addAnchor(new Anchor("a3.5", 3.5)); // n
    g.addAnchor(new Anchor("a4", 4.0)); // fox & N
    g.addAnchor(new Anchor("a4.25", 4.25)); // jumps
    g.addAnchor(new Anchor("a4.75", 4.75)); // over
    g.addAnchor(new Anchor("a5", 5.0)); // the
    g.addAnchor(new Anchor("a6a", 6.0)); // end of the
    g.addAnchor(new Anchor("a6b", 6.0)); // lazy
    g.addAnchor(new Anchor("a7a", 7.0)); // end of lazy
    g.addAnchor(new Anchor("a7a", 7.0)); // dog
    g.addAnchor(new Anchor("a8", 8.0)); // end of dog
    g.addAnchor(new Anchor("a9", 9.0)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a9", "my graph"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a9", "participant1"));

    g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("word2", "quick", "word", "a2", "a3", "turn1"));
    // brown starts before over ends
    g.addAnnotation(new Annotation("word3", "brown", "word", "a2.6", "a4", "turn1"));
    g.addAnnotation(new Annotation("word4", "fox", "word", "a4", "a4.25", "turn1"));
    g.addAnnotation(new Annotation("word5", "jumps", "word", "a4.25", "a4.75", "turn1"));
    g.addAnnotation(new Annotation("word6", "over", "word", "a4.75", "a5", "turn1"));
    g.addAnnotation(new Annotation("word7", "the", "word", "a5", "a6a", "turn1"));
    g.addAnnotation(new Annotation("word8", "lazy", "word", "a6b", "a7a", "turn1"));
    g.addAnnotation(new Annotation("word9", "dog", "word", "a7a", "a8", "turn1"));

    g.addAnnotation(new Annotation("phone1", "D",  "phone", "a1",    "a1.5",  "word1"));
    g.addAnnotation(new Annotation("phone2", "@",  "phone", "a1.5",  "a2",    "word1"));

    g.addAnnotation(new Annotation("phone3", "k",  "phone", "a2",    "a2.25", "word2"));
    g.addAnnotation(new Annotation("phone4", "w",  "phone", "a2.25", "a2.5",  "word2"));
    g.addAnnotation(new Annotation("phone5", "I",  "phone", "a2.5",  "a2.75", "word2"));
    g.addAnnotation(new Annotation("phone6", "k",  "phone", "a2.75", "a3",    "word2"));
    // out of order...
    g.addAnnotation(new Annotation("phone7", "b",  "phone", "a2.6",  "a2.8",  "word3"));
    g.addAnnotation(new Annotation("phone8", "r",  "phone", "a2.8",  "a3.2",  "word3"));
    g.addAnnotation(new Annotation("phone9", "au", "phone", "a3.2",  "a3.5",    "word3"));
    g.addAnnotation(new Annotation("phone10", "n", "phone", "a3.5",  "a4",    "word3"));

    g.addAnnotation(new Annotation("pos1", "DT", "pos", "a1", "a2", "word1")); // the
    g.addAnnotation(new Annotation("pos2", "A", "pos", "a2", "a3", "word2")); // quick
    g.addAnnotation(new Annotation("pos3", "A", "pos", "a2.6", "a4", "word3")); // brown

    Validator v = new Validator();
    v.setFullValidation(true);
    // v.setDebug(true);
    v.setDefaultOffsetThreshold(null);
    try
    {
      Vector<Change> changes = v.transform(g);
      if (v.getLog() != null) for (String m : v.getLog()) System.out.println(m);

      // check the anchoring is what we expect
	 
      // there's not teasing apart, because correctReversedAnchors() gets there first

      // the
      assertEquals("prior word end shared", g.getAnnotation("word2").getStartId(),
                   g.getAnnotation("word1").getEndId());

      // quick
      assertEquals("previous start kept", "a2", g.getAnnotation("word2").getStartId());
      assertEquals("previous start offset unchanged", 
                   new Double(2.0), g.getAnnotation("word2").getStart().getOffset());
      assertEquals("previous end kept", "a3", g.getAnnotation("word2").getEndId());
      assertNull("previous end offset reset", 
                 g.getAnnotation("word2").getEnd().getOffset());
      // brown
      assertEquals("next start kept", "a2.6", g.getAnnotation("word3").getStartId());
      assertEquals("next end offset kept", 
                   new Double(2.6), g.getAnnotation("word3").getStart().getOffset());
      assertEquals("next end kept", "a4", g.getAnnotation("word3").getEndId());
      assertEquals("next end offset unchanged", 
                   new Double(4.0), g.getAnnotation("word3").getEnd().getOffset());

      // fox
      assertEquals("trailing word start shared", g.getAnnotation("word3").getEndId(),
                   g.getAnnotation("word4").getStartId());

      // k
      assertEquals("child start unchanged", "a2", g.getAnnotation("phone3").getStartId());
      assertEquals("child end unchanged", "a2.25", g.getAnnotation("phone3").getEndId());
      // w
      assertEquals("child start unchanged", "a2.25", g.getAnnotation("phone4").getStartId());
      assertEquals("child end unchanged", "a2.5", g.getAnnotation("phone4").getEndId());
      // I
      assertEquals("child start unchanged", "a2.5", g.getAnnotation("phone5").getStartId());
      assertEquals("next start offset unchanged", 
                   new Double(2.5), g.getAnnotation("phone5").getStart().getOffset());
      assertEquals("child end unchanged", "a2.75", g.getAnnotation("phone5").getEndId());
      assertNull("child end reset offset", g.getAnnotation("phone5").getEnd().getOffset());
      // k
      assertEquals("child start unchanged", "a2.75", g.getAnnotation("phone6").getStartId());
      assertNull("child start reset offset", g.getAnnotation("phone6").getStart().getOffset());
      assertEquals("child end with parent", g.getAnnotation("word2").getEndId(), 
                   g.getAnnotation("phone6").getEndId());

      // b
      assertEquals("child start with parent", g.getAnnotation("word3").getStartId(),
                   g.getAnnotation("phone7").getStartId());
      assertEquals("child end kept", "a2.8", g.getAnnotation("phone7").getEndId());
      assertEquals("child end offset kept", 
                   new Double(2.8), g.getAnnotation("phone7").getEnd().getOffset());
      // r
      assertEquals("child start kept", "a2.8", g.getAnnotation("phone8").getStartId());
      assertEquals("child start offset kept", 
                   new Double(2.8), g.getAnnotation("phone8").getStart().getOffset());
      assertEquals("child end unchanged", "a3.2", g.getAnnotation("phone8").getEndId());
      assertEquals("next end offset unchanged", 
                   new Double(3.2), g.getAnnotation("phone8").getEnd().getOffset());
      // au
      assertEquals("child start unchanged", "a3.2", g.getAnnotation("phone9").getStartId());
      assertEquals("child end unchanged", "a3.5", g.getAnnotation("phone9").getEndId());
      // n
      assertEquals("child start unchanged", "a3.5", g.getAnnotation("phone10").getStartId());
      assertEquals("child end with parent", g.getAnnotation("word3").getEndId(),
                   g.getAnnotation("phone10").getEndId());

      // pos tags came along too
      assertEquals("tag anchors updated", g.getAnnotation("word1").getStartId(),
                   g.getAnnotation("pos1").getStartId());
      assertEquals("tag anchors updated", g.getAnnotation("word1").getEndId(),
                   g.getAnnotation("pos1").getEndId());
      assertEquals("tag anchors updated", g.getAnnotation("word2").getStartId(),
                   g.getAnnotation("pos2").getStartId());
      assertEquals("tag anchors updated", g.getAnnotation("word2").getEndId(),
                   g.getAnnotation("pos2").getEndId());
      assertEquals("tag anchors updated", g.getAnnotation("word3").getStartId(),
                   g.getAnnotation("pos3").getStartId());
      assertEquals("tag anchors updated", g.getAnnotation("word3").getEndId(),
                   g.getAnnotation("pos3").getEndId());

      assertEquals("no extra changes to graph", changes.size(), g.getChanges().size());
    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }

  @Test public void validateHierarchyReversedAnchorsSimpleCases()
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
    g.addLayer(new Layer("pos", "Part of speech", Constants.ALIGNMENT_NONE,
                         false, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("phrase", "Phrase structure", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         true, // peersOverlap
                         false, // saturated
                         "turn", // parentId
                         true)); // parentIncludes
      
    g.addAnchor(new Anchor("a0", 0.0,  // turn start
                           Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("a1", 1.0,  // the & DT & D & NP
                           Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("a1.5", 1.5,  // @
                           Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("a2a", 2.0,  // end of the
                           Constants.CONFIDENCE_AUTOMATIC));

    // quick has reversed anchors
    // phones are manually aligned, so should be kept as-is
    g.addAnchor(new Anchor("a2b", 3.0,  // quick & A & k & AP
                           Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("a2.25", 2.25,  // w
                           Constants.CONFIDENCE_MANUAL));
    g.addAnchor(new Anchor("a2.5", 2.5,  // I
                           Constants.CONFIDENCE_MANUAL));
    g.addAnchor(new Anchor("a2.75", 2.75,  // k
                           Constants.CONFIDENCE_MANUAL));
    g.addAnchor(new Anchor("a3a", 2.0,  // end of quick
                           Constants.CONFIDENCE_AUTOMATIC));

    g.addAnchor(new Anchor("a3b", 3.0,  // brown
                           Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("a4", 4.0,  // fox & N
                           Constants.CONFIDENCE_AUTOMATIC));
    // jumps is reversed, all same confidences (first anchor will be kept)
    g.addAnchor(new Anchor("a4.75", 4.75, // jumps
                           Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("a4.25", 4.25, // over
                           Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("a5", 5.0,  // end of over
                           Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("a6", 6.0,  // turn end
                           Constants.CONFIDENCE_AUTOMATIC));

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a6", "my graph"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a6", "participant1"));

    g.addAnnotation(new Annotation("phrase1", "NP", "phrase", "a1", "a4", "turn1"));
    g.addAnnotation(new Annotation("phrase2", "AP", "phrase", "a2b", "a4", "turn1"));

    g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2a", "turn1"));
    g.addAnnotation(new Annotation("word2", "quick", "word", "a2b", "a3a", "turn1"));
    g.addAnnotation(new Annotation("word3", "brown", "word", "a3b", "a4", "turn1"));
    g.addAnnotation(new Annotation("word4", "fox", "word", "a4", "a4.75", "turn1"));
    // reversed
    g.addAnnotation(new Annotation("word5", "jumps", "word", "a4.75", "a4.25", "turn1"));

    g.addAnnotation(new Annotation("word6", "over", "word", "a4.25", "a5", "turn1"));

    g.addAnnotation(new Annotation("pos1", "DT", "pos", "a1", "a2a", "word1"));
    g.addAnnotation(new Annotation("pos2", "A", "pos", "a2b", "a3a", "word2"));
    g.addAnnotation(new Annotation("pos3", "N", "pos", "a4", "a4.75", "word4"));

    g.addAnnotation(new Annotation("phone1", "D", "phone", "a1", "a1.5", "word1"));
    g.addAnnotation(new Annotation("phone2", "@", "phone", "a1.5", "a2a", "word1"));
    g.addAnnotation(new Annotation("phone3", "k", "phone", "a2b", "a2.25", "word2"));
    g.addAnnotation(new Annotation("phone4", "w", "phone", "a2.25", "a2.5", "word2"));
    g.addAnnotation(new Annotation("phone5", "I", "phone", "a2.5", "a2.75", "word2"));
    g.addAnnotation(new Annotation("phone6", "k", "phone", "a2.75", "a3a", "word2"));

    Validator v = new Validator();
    v.setFullValidation(true);
    // v.setDebug(true);
    v.setDefaultOffsetThreshold(null);
    try
    {
      Vector<Change> changes = v.transform(g);
      if (v.getLog() != null) for (String m : v.getLog()) System.out.println(m);

      // check the anchoring is what we expect

      // quick
	 
      // word start has been reset
      assertNull(g.getAnchor("a2b").getOffset());
      assertEquals(new Integer(Constants.CONFIDENCE_NONE), 
                   g.getAnchor("a2b").getConfidence());
      // phones are the same as before
      assertEquals(new Double(2.25), g.getAnchor("a2.25").getOffset());
      assertEquals(new Integer(Constants.CONFIDENCE_MANUAL), 
                   g.getAnchor("a2.25").getConfidence());
      assertEquals(new Double(2.5), g.getAnchor("a2.5").getOffset());
      assertEquals(new Integer(Constants.CONFIDENCE_MANUAL), 
                   g.getAnchor("a2.5").getConfidence());
      assertEquals(new Double(2.75), g.getAnchor("a2.75").getOffset());
      assertEquals(new Integer(Constants.CONFIDENCE_MANUAL), 
                   g.getAnchor("a2.75").getConfidence());
      // word end has been reset
      assertNull(g.getAnchor("a3a").getOffset());
      assertEquals(new Integer(Constants.CONFIDENCE_NONE), 
                   g.getAnchor("a3a").getConfidence());

      // annotations still linked as before
      assertEquals("a2b", g.getAnnotation("word2").getStartId());
      assertEquals("a3a", g.getAnnotation("word2").getEndId());
      assertEquals("a2b", g.getAnnotation("phone3").getStartId());
      assertEquals("a2.25", g.getAnnotation("phone3").getEndId());
      assertEquals("a2.25", g.getAnnotation("phone4").getStartId());
      assertEquals("a2.5", g.getAnnotation("phone4").getEndId());
      assertEquals("a2.5", g.getAnnotation("phone5").getStartId());
      assertEquals("a2.75", g.getAnnotation("phone5").getEndId());
      assertEquals("a2.75", g.getAnnotation("phone6").getStartId());
      assertEquals("a3a", g.getAnnotation("phone6").getEndId());

      // jumps
      assertNull("same confidence - start reset", 
                 g.getAnchor("a4.75").getOffset());
      assertEquals("same confidence - start reset", 
                   new Integer(Constants.CONFIDENCE_NONE), 
                   g.getAnchor("a4.75").getConfidence());
      assertEquals("same confidence - end kept", 
                   new Double(4.25), g.getAnchor("a4.25").getOffset());
      assertEquals("same confidence - end reset", 
                   new Integer(Constants.CONFIDENCE_AUTOMATIC), 
                   g.getAnchor("a4.25").getConfidence());

      assertEquals("a4.75", g.getAnnotation("word5").getStartId());
      assertEquals("a4.25", g.getAnnotation("word5").getEndId());

      // order of changes
      Iterator<Change> order = changes.iterator();
      assertEquals(new Change(Change.Operation.Update, g.getAnchor("a2b"), "offset", null), 
                   order.next());
      assertEquals(new Change(Change.Operation.Update, g.getAnchor("a3a"), "offset", null), 
                   order.next());
      assertEquals(new Change(Change.Operation.Update, g.getAnchor("a4.75"), "offset", null), 
                   order.next());
      assertFalse(order.hasNext());
      assertEquals("no extra changes to graph", changes.size(), g.getChanges().size());
    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }

  @Test public void validateHierarchyReversedAnchorsComplexCases()
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
    g.addLayer(new Layer("pos", "Part of speech", Constants.ALIGNMENT_NONE,
                         false, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("phrase", "Phrase structure", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         true, // peersOverlap
                         false, // saturated
                         "turn", // parentId
                         true)); // parentIncludes
      
    g.addAnchor(new Anchor("a0", 0.0,  // turn start
                           Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("a1", 1.0,  // the & DT & D & NP
                           Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("a1.5", 1.5,  // @
                           Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("a2a", 2.0,  // end of the
                           Constants.CONFIDENCE_AUTOMATIC));

    // quick has reversed anchors and reversed children, all anchors have the same confidence
    // two sequences are good, the 2.5/2.75/3.0 sequnce is kept
    g.addAnchor(new Anchor("a2.5", 2.5,  // quick & A & k & AP
                           Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("a2.75", 2.75,  // w
                           Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("a3.0", 3.0,  // I
                           Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("a2.0", 2.0,  // k
                           Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("a2.25", 2.25,  // end of quick

                           Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("a3b", 3.0,  // brown
                           Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("a4", 4.0,  // fox & N
                           Constants.CONFIDENCE_AUTOMATIC));
    // unset offsets
    g.addAnchor(new Anchor("a?1", null)); // jumps
    g.addAnchor(new Anchor("a?2", null)); // over
    g.addAnchor(new Anchor("a5", 5.0,  // end of over
                           Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("a6", 6.0,  // turn end
                           Constants.CONFIDENCE_AUTOMATIC));

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a6", "my graph"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a6", "participant1"));

    g.addAnnotation(new Annotation("phrase1", "NP", "phrase", "a1", "a4", "turn1"));
    g.addAnnotation(new Annotation("phrase2", "AP", "phrase", "a2.5", "a4", "turn1"));

    g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2a", "turn1"));

    // "quick" reversed
    g.addAnnotation(new Annotation("word2", "quick", "word", "a2.5", "a2.25", "turn1"));

    g.addAnnotation(new Annotation("word3", "brown", "word", "a3b", "a4", "turn1"));
    g.addAnnotation(new Annotation("word4", "fox", "word", "a4", "a?1", "turn1"));
    g.addAnnotation(new Annotation("word5", "jumps", "word", "a?1", "a?2", "turn1"));
    g.addAnnotation(new Annotation("word6", "over", "word", "a?2", "a5", "turn1"));

    g.addAnnotation(new Annotation("pos1", "DT", "pos", "a1", "a2a", "word1"));
    // reversed, but it's a tag on "quick", so will change with that
    g.addAnnotation(new Annotation("pos2", "A", "pos", "a2.5", "a2.25", "word2"));

    g.addAnnotation(new Annotation("pos3", "N", "pos", "a4", "a?1", "word4"));

    g.addAnnotation(new Annotation("phone1", "D", "phone", "a1", "a1.5", "word1"));
    g.addAnnotation(new Annotation("phone2", "@", "phone", "a1.5", "a2a", "word1"));

    // "quick" children - some reversed, some not
    g.addAnnotation(new Annotation("phone3", "k", "phone", "a2.5", "a2.75", "word2"));
    g.addAnnotation(new Annotation("phone4", "w", "phone", "a2.75", "a3.0", "word2"));
    g.addAnnotation(new Annotation("phone5", "I", "phone", "a3.0", "a2.0", "word2"));
    g.addAnnotation(new Annotation("phone6", "k", "phone", "a2.0", "a2.25", "word2"));

    Validator v = new Validator();
    v.setFullValidation(true);
    // v.setDebug(true);
    v.setDefaultOffsetThreshold(null);
    try
    {
      Vector<Change> changes = v.transform(g);
      if (v.getLog() != null) for (String m : v.getLog()) System.out.println(m);

      // check the anchoring is what we expect
	 
      // start the same as before
      assertEquals(new Double(2.5), g.getAnchor("a2.5").getOffset());
      assertEquals(new Integer(Constants.CONFIDENCE_AUTOMATIC), 
                   g.getAnchor("a2.5").getConfidence());
      assertEquals(new Double(2.75), g.getAnchor("a2.75").getOffset());
      assertEquals(new Integer(Constants.CONFIDENCE_AUTOMATIC), 
                   g.getAnchor("a2.75").getConfidence());
      assertEquals(new Double(3.0), g.getAnchor("a3.0").getOffset());
      assertEquals(new Integer(Constants.CONFIDENCE_AUTOMATIC), 
                   g.getAnchor("a3.0").getConfidence());

      // last two have been reset
      assertNull(g.getAnchor("a2.0").getOffset());
      assertEquals(new Integer(Constants.CONFIDENCE_NONE), 
                   g.getAnchor("a2.0").getConfidence());
      assertNull(g.getAnchor("a2.25").getOffset());
      assertEquals(new Integer(Constants.CONFIDENCE_NONE), 
                   g.getAnchor("a2.25").getConfidence());

      // annotations still linked as before
      assertEquals("a2.5", g.getAnnotation("word2").getStartId());
      assertEquals("a2.25", g.getAnnotation("word2").getEndId());
      assertEquals("a2.5", g.getAnnotation("phone3").getStartId());
      assertEquals("a2.75", g.getAnnotation("phone3").getEndId());
      assertEquals("a2.75", g.getAnnotation("phone4").getStartId());
      assertEquals("a3.0", g.getAnnotation("phone4").getEndId());
      assertEquals("a3.0", g.getAnnotation("phone5").getStartId());
      assertEquals("a2.0", g.getAnnotation("phone5").getEndId());
      assertEquals("a2.0", g.getAnnotation("phone6").getStartId());
      assertEquals("a2.25", g.getAnnotation("phone6").getEndId());
	 
      Iterator<Change> order = changes.iterator();
      assertEquals(new Change(Change.Operation.Update, g.getAnchor("a2.0"), "offset", null), 
                   order.next());
      assertEquals(new Change(Change.Operation.Update, g.getAnchor("a2.25"), "offset", null), 
                   order.next());
      assertEquals(new Change(Change.Operation.Update, g.getAnchor("a3b"), "offset", null), 
                   order.next());
      assertFalse("No extraneous changes", order.hasNext());

      assertEquals("no extra changes to graph", changes.size(), g.getChanges().size());
    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }

  @Test public void anchorsOutOfOrderHigherConfidenceFollowing()
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
    g.addLayer(new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL, 
                         true,  // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes

    g.addAnchor(new Anchor("turnStart", 120.0, Constants.CONFIDENCE_MANUAL));
    // than has out of sequence phones
    g.addAnchor(new Anchor("thanStart", 129.1, Constants.CONFIDENCE_MANUAL));
    g.addAnchor(new Anchor("a1Start", 130.6, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("nStart", 131.3, Constants.CONFIDENCE_DEFAULT));
    // that is backwards
    g.addAnchor(new Anchor("thatStart", 131.4, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("a2Start", 129.5, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("tStart", 129.6, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("yeahStart", 129.7, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("eahStart", 129.8, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("yeahEnd", 129.9, Constants.CONFIDENCE_MANUAL));
    g.addAnchor(new Anchor("turnEnd", 140.0, Constants.CONFIDENCE_MANUAL)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "participant1"));
      
    g.addAnnotation(new Annotation("than", "than", "word", "thanStart", "thatStart", "turn1"));
    g.addAnnotation(new Annotation("that", "that", "word", "thatStart", "yeahStart", "turn1"));
    g.addAnnotation(new Annotation("yeah", "yeah", "word", "yeahStart", "yeahEnd", "turn1"));

    g.addAnnotation(new Annotation("th1", "th", "phone", "thanStart", "a1Start", "than"));
    g.addAnnotation(new Annotation("a1", "a", "phone", "a1Start", "nStart", "than"));
    g.addAnnotation(new Annotation("n", "n", "phone", "nStart", "thatStart", "than"));
    g.addAnnotation(new Annotation("th2", "th", "phone", "thatStart", "a2Start", "that"));
    g.addAnnotation(new Annotation("a2", "a", "phone", "a2Start", "tStart", "that"));
    g.addAnnotation(new Annotation("t", "t", "phone", "tStart", "yeahStart", "that"));
    g.addAnnotation(new Annotation("y", "y", "phone", "yeahStart", "eahStart", "yeah"));
    g.addAnnotation(new Annotation("eah", "eah", "phone", "eahStart", "yeahEnd", "yeah"));

    Validator v = new Validator();
    v.setFullValidation(true);
    // v.setDebug(true);
    v.setDefaultOffsetThreshold(null);
    try
    {
      Vector<Change> changes = v.transform(g);
      if (v.getLog() != null) for (String m : v.getLog()) System.out.println(m);

      // check the anchoring is what we expect
	 
      // same as before...
      assertEquals("unchanged", new Double(120.0), g.getAnchor("turnStart").getOffset());
      assertEquals("unchanged", new Double(129.1), g.getAnchor("thanStart").getOffset());
      // changed...
      assertNull("reset", g.getAnchor("a1Start").getOffset());
      assertNull("reset", g.getAnchor("nStart").getOffset());
      assertNull("reset", g.getAnchor("thatStart").getOffset());
      // same as before...
      assertEquals("unchanged", new Double(129.5), g.getAnchor("a2Start").getOffset());
      assertEquals("unchanged", new Double(129.6), g.getAnchor("tStart").getOffset());
      assertEquals("unchanged", new Double(129.7), g.getAnchor("yeahStart").getOffset());
      assertEquals("unchanged", new Double(129.8), g.getAnchor("eahStart").getOffset());
      assertEquals("unchanged", new Double(129.9), g.getAnchor("yeahEnd").getOffset());
      assertEquals("unchanged", new Double(140.0), g.getAnchor("turnEnd").getOffset());

    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }

  @Test public void anchorsOutOfOrderHigherConfidencePrior()
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
    g.addLayer(new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL, 
                         true,  // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes

    g.addAnchor(new Anchor("turnStart", 120.0, Constants.CONFIDENCE_MANUAL));
    // than has out of sequence phones
    g.addAnchor(new Anchor("thanStart", 129.1, Constants.CONFIDENCE_MANUAL));
    g.addAnchor(new Anchor("a1Start", 130.6, Constants.CONFIDENCE_MANUAL));
    g.addAnchor(new Anchor("nStart", 131.3, Constants.CONFIDENCE_DEFAULT));
    // that is backwards
    g.addAnchor(new Anchor("thatStart", 131.4, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("a2Start", 129.5, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("tStart", 129.6, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("yeahStart", 129.7, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("eahStart", 129.8, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("yeahEnd", 129.9, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("turnEnd", 140.0, Constants.CONFIDENCE_MANUAL)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "participant1"));
      
    g.addAnnotation(new Annotation("than", "than", "word", "thanStart", "thatStart", "turn1"));
    g.addAnnotation(new Annotation("that", "that", "word", "thatStart", "yeahStart", "turn1"));
    g.addAnnotation(new Annotation("yeah", "yeah", "word", "yeahStart", "yeahEnd", "turn1"));

    g.addAnnotation(new Annotation("th1", "th", "phone", "thanStart", "a1Start", "than"));
    g.addAnnotation(new Annotation("a1", "a", "phone", "a1Start", "nStart", "than"));
    g.addAnnotation(new Annotation("n", "n", "phone", "nStart", "thatStart", "than"));
    g.addAnnotation(new Annotation("th2", "th", "phone", "thatStart", "a2Start", "that"));
    g.addAnnotation(new Annotation("a2", "a", "phone", "a2Start", "tStart", "that"));
    g.addAnnotation(new Annotation("t", "t", "phone", "tStart", "yeahStart", "that"));
    g.addAnnotation(new Annotation("y", "y", "phone", "yeahStart", "eahStart", "yeah"));
    g.addAnnotation(new Annotation("eah", "eah", "phone", "eahStart", "yeahEnd", "yeah"));

    Validator v = new Validator();
    v.setFullValidation(true);
    // v.setDebug(true);
    v.setDefaultOffsetThreshold(null);
    try
    {
      Vector<Change> changes = v.transform(g);
      if (v.getLog() != null) for (String m : v.getLog()) System.out.println(m);

      // check the anchoring is what we expect
	 
      // same as before...
      assertEquals("unchanged", new Double(120.0), g.getAnchor("turnStart").getOffset());
      assertEquals("unchanged", new Double(129.1), g.getAnchor("thanStart").getOffset());
      assertEquals("unchanged", new Double(130.6), g.getAnchor("a1Start").getOffset());
      assertEquals("unchanged", new Double(131.3), g.getAnchor("nStart").getOffset());
      assertEquals("unchanged", new Double(131.4), g.getAnchor("thatStart").getOffset());
      // changed...
      assertNull("reset", g.getAnchor("a2Start").getOffset());
      assertNull("reset", g.getAnchor("tStart").getOffset());
      assertNull("reset", g.getAnchor("yeahStart").getOffset());
      assertNull("reset", g.getAnchor("eahStart").getOffset());
      assertNull("reset", g.getAnchor("yeahEnd").getOffset());
      // same as before...
      assertEquals("unchanged", new Double(140.0), g.getAnchor("turnEnd").getOffset());

    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }

  @Test public void anchorsOutOfOrderFewerPrior()
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
    g.addLayer(new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL, 
                         true,  // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes

    g.addAnchor(new Anchor("turnStart", 120.0, Constants.CONFIDENCE_MANUAL));
    // than has out of sequence phones
    g.addAnchor(new Anchor("thanStart", 129.1, Constants.CONFIDENCE_MANUAL));
    g.addAnchor(new Anchor("a1Start", 130.6, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("nStart", 131.3, Constants.CONFIDENCE_DEFAULT));
    // that is backwards
    g.addAnchor(new Anchor("thatStart", 131.4, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("a2Start", 129.5, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("tStart", 129.6, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("yeahStart", 129.7, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("eahStart", 129.8, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("yeahEnd", 129.9, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("turnEnd", 140.0, Constants.CONFIDENCE_MANUAL)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "participant1"));
      
    g.addAnnotation(new Annotation("than", "than", "word", "thanStart", "thatStart", "turn1"));
    g.addAnnotation(new Annotation("that", "that", "word", "thatStart", "yeahStart", "turn1"));
    g.addAnnotation(new Annotation("yeah", "yeah", "word", "yeahStart", "yeahEnd", "turn1"));

    g.addAnnotation(new Annotation("th1", "th", "phone", "thanStart", "a1Start", "than"));
    g.addAnnotation(new Annotation("a1", "a", "phone", "a1Start", "nStart", "than"));
    g.addAnnotation(new Annotation("n", "n", "phone", "nStart", "thatStart", "than"));
    g.addAnnotation(new Annotation("th2", "th", "phone", "thatStart", "a2Start", "that"));
    g.addAnnotation(new Annotation("a2", "a", "phone", "a2Start", "tStart", "that"));
    g.addAnnotation(new Annotation("t", "t", "phone", "tStart", "yeahStart", "that"));
    g.addAnnotation(new Annotation("y", "y", "phone", "yeahStart", "eahStart", "yeah"));
    g.addAnnotation(new Annotation("eah", "eah", "phone", "eahStart", "yeahEnd", "yeah"));

    Validator v = new Validator();
    v.setFullValidation(true);
    // v.setDebug(true);
    v.setDefaultOffsetThreshold(null);
    try
    {
      Vector<Change> changes = v.transform(g);
      if (v.getLog() != null) for (String m : v.getLog()) System.out.println(m);

      // check the anchoring is what we expect
	 
      // same as before...
      assertEquals("unchanged", new Double(120.0), g.getAnchor("turnStart").getOffset());
      assertEquals("unchanged", new Double(129.1), g.getAnchor("thanStart").getOffset());
      // changed...
      assertNull("reset", g.getAnchor("a1Start").getOffset());
      assertNull("reset", g.getAnchor("nStart").getOffset());
      assertNull("reset", g.getAnchor("thatStart").getOffset());
      // same as before...
      assertEquals("unchanged", new Double(129.5), g.getAnchor("a2Start").getOffset());
      assertEquals("unchanged", new Double(129.6), g.getAnchor("tStart").getOffset());
      assertEquals("unchanged", new Double(129.7), g.getAnchor("yeahStart").getOffset());
      assertEquals("unchanged", new Double(129.8), g.getAnchor("eahStart").getOffset());
      assertEquals("unchanged", new Double(129.9), g.getAnchor("yeahEnd").getOffset());
      assertEquals("unchanged", new Double(140.0), g.getAnchor("turnEnd").getOffset());

    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }

  @Test public void anchorsOutOfOrderFewerFollowing()
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
    g.addLayer(new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL, 
                         true,  // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes

    g.addAnchor(new Anchor("turnStart", 120.0, Constants.CONFIDENCE_MANUAL));
    // than has out of sequence phones
    g.addAnchor(new Anchor("thanStart", 129.1, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("a1Start", 130.6, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("nStart", 131.3, Constants.CONFIDENCE_DEFAULT));
    // that is backwards
    g.addAnchor(new Anchor("thatStart", 131.4, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("thatEnd", 129.7, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("turnEnd", 140.0, Constants.CONFIDENCE_MANUAL)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "participant1"));
      
    g.addAnnotation(new Annotation("than", "than", "word", "thanStart", "thatStart", "turn1"));
    g.addAnnotation(new Annotation("that", "that", "word", "thatStart", "thatEnd", "turn1"));

    g.addAnnotation(new Annotation("th1", "th", "phone", "thanStart", "a1Start", "than"));
    g.addAnnotation(new Annotation("a1", "a", "phone", "a1Start", "nStart", "than"));
    g.addAnnotation(new Annotation("n", "n", "phone", "nStart", "thatStart", "than"));

    Validator v = new Validator();
    v.setFullValidation(true);
    // v.setDebug(true);
    v.setDefaultOffsetThreshold(null);
    try
    {
      Vector<Change> changes = v.transform(g);
      if (v.getLog() != null) for (String m : v.getLog()) System.out.println(m);

      // check the anchoring is what we expect
	 
      // same as before...
      assertEquals("unchanged", new Double(120.0), g.getAnchor("turnStart").getOffset());
      assertEquals("unchanged", new Double(129.1), g.getAnchor("thanStart").getOffset());
      assertEquals("unchanged", new Double(130.6), g.getAnchor("a1Start").getOffset());
      assertEquals("unchanged", new Double(131.3), g.getAnchor("nStart").getOffset());
      assertEquals("unchanged", new Double(131.4), g.getAnchor("thatStart").getOffset());
      // changed...
      assertNull("reset", g.getAnchor("thatEnd").getOffset());
      // same as before...
      assertEquals("unchanged", new Double(140.0), g.getAnchor("turnEnd").getOffset());

    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }

  @Test public void anchorsOutOfOrderHigherConfidencePriorAndFollowing()
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
    g.addLayer(new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL, 
                         true,  // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes

    g.addAnchor(new Anchor("turnStart", 120.0, Constants.CONFIDENCE_MANUAL));
    // than has out of sequence phones
    g.addAnchor(new Anchor("thanStart", 129.1, Constants.CONFIDENCE_MANUAL));
    g.addAnchor(new Anchor("a1Start", 129.6, Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("nStart", 131.3, Constants.CONFIDENCE_DEFAULT));
    // that is backwards
    g.addAnchor(new Anchor("thatStart", 131.4, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("a2Start", 129.5, Constants.CONFIDENCE_DEFAULT));
    g.addAnchor(new Anchor("tStart", 129.6, Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("yeahStart", 129.7, Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("eahStart", 129.8, Constants.CONFIDENCE_AUTOMATIC));
    g.addAnchor(new Anchor("yeahEnd", 129.9, Constants.CONFIDENCE_MANUAL));
    g.addAnchor(new Anchor("turnEnd", 140.0, Constants.CONFIDENCE_MANUAL)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "participant1"));
      
    g.addAnnotation(new Annotation("than", "than", "word", "thanStart", "thatStart", "turn1"));
    g.addAnnotation(new Annotation("that", "that", "word", "thatStart", "yeahStart", "turn1"));
    g.addAnnotation(new Annotation("yeah", "yeah", "word", "yeahStart", "yeahEnd", "turn1"));

    g.addAnnotation(new Annotation("th1", "th", "phone", "thanStart", "a1Start", "than"));
    g.addAnnotation(new Annotation("a1", "a", "phone", "a1Start", "nStart", "than"));
    g.addAnnotation(new Annotation("n", "n", "phone", "nStart", "thatStart", "than"));
    g.addAnnotation(new Annotation("th2", "th", "phone", "thatStart", "a2Start", "that"));
    g.addAnnotation(new Annotation("a2", "a", "phone", "a2Start", "tStart", "that"));
    g.addAnnotation(new Annotation("t", "t", "phone", "tStart", "yeahStart", "that"));
    g.addAnnotation(new Annotation("y", "y", "phone", "yeahStart", "eahStart", "yeah"));
    g.addAnnotation(new Annotation("eah", "eah", "phone", "eahStart", "yeahEnd", "yeah"));

    Validator v = new Validator();
    v.setFullValidation(true);
    // v.setDebug(true);
    v.setDefaultOffsetThreshold(null);
    try
    {
      Vector<Change> changes = v.transform(g);
      if (v.getLog() != null) for (String m : v.getLog()) System.out.println(m);

      // check the anchoring is what we expect
	 
      // same as before...
      assertEquals("unchanged", new Double(120.0), g.getAnchor("turnStart").getOffset());
      assertEquals("unchanged", new Double(129.1), g.getAnchor("thanStart").getOffset());
      // changed...
      assertEquals("unchanged", new Double(129.6), g.getAnchor("a1Start").getOffset());
      assertNull("reset", g.getAnchor("nStart").getOffset());
      assertNull("reset", g.getAnchor("thatStart").getOffset());
      assertNull("reset", g.getAnchor("a2Start").getOffset());
      assertNull("reset", g.getAnchor("tStart").getOffset());
      // same as before...
      assertEquals("unchanged", new Double(129.7), g.getAnchor("yeahStart").getOffset());
      assertEquals("unchanged", new Double(129.8), g.getAnchor("eahStart").getOffset());
      assertEquals("unchanged", new Double(129.9), g.getAnchor("yeahEnd").getOffset());
      assertEquals("unchanged", new Double(140.0), g.getAnchor("turnEnd").getOffset());

    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }

  @Test public void utterancesPartitionWordsInTurnNoChange() 
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
    g.addLayer(new Layer("pos", "POS", Constants.ALIGNMENT_NONE,
                         true, // peers
                         true, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes

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

    Validator v = new Validator();
    v.setFullValidation(true);
    // v.setDebug(true);
    v.setDefaultOffsetThreshold(null);
    try
    {
      Vector<Change> changes = v.transform(g);
      if (v.getLog() != null) for (String m : v.getLog()) System.out.println(m);

      assertEquals("no changes: " + changes, 0, changes.size());
    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }

  @Test public void utterancesPartitionWordsInTurnAnchorsOutOfOrder() 
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

    g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL)); // turn start

    // "the" reversed
    g.addAnchor(new Anchor("a0", 0.02, Constants.CONFIDENCE_DEFAULT)); // the
    g.addAnchor(new Anchor("a01", 0.01, Constants.CONFIDENCE_DEFAULT)); // quick
    g.addAnchor(new Anchor("a02", 0.03, Constants.CONFIDENCE_DEFAULT)); // brown
    g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox
    g.addAnchor(new Anchor("a04a", 0.04, Constants.CONFIDENCE_DEFAULT)); // fox end

    g.addAnchor(new Anchor("utteranceChange", 0.4, Constants.CONFIDENCE_MANUAL)); // utterance boundary

    g.addAnchor(new Anchor("a04b", 2.0, Constants.CONFIDENCE_AUTOMATIC)); // jumps
    g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE_AUTOMATIC)); // over
    g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE_AUTOMATIC)); // a
    // "lazy" reversed
    g.addAnchor(new Anchor("a34", 5.1, Constants.CONFIDENCE_AUTOMATIC)); // lazy
    g.addAnchor(new Anchor("a44", 5.0, Constants.CONFIDENCE_AUTOMATIC)); // dog
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

    Validator v = new Validator();
    v.setFullValidation(true);
    // v.setDebug(true);
    v.setDefaultOffsetThreshold(null);
    try
    {
      Vector<Change> changes = v.transform(g);
      if (v.getLog() != null) for (String m : v.getLog()) System.out.println(m);

      assertNull("reset", g.getAnchor("a0").getOffset());
      assertNull("reset", g.getAnchor("a34").getOffset());

      assertEquals("two changes: " + changes, 2, changes.size());
    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }

  @Test public void deleteExtraneousPeers() 
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
    g.addLayer(new Layer("orthography", "Orthography", Constants.ALIGNMENT_NONE,
                         false, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes

    g.addAnchor(new Anchor("a0", 0.0)); // turn start
    g.addAnchor(new Anchor("a1", 1.0)); // the & DT & D & NP
    g.addAnchor(new Anchor("a1.5", 1.5)); // @
    g.addAnchor(new Anchor("a2", 2.0)); // quick & A & k & AP
    g.addAnchor(new Anchor("a2.25", 2.25)); // w
    g.addAnchor(new Anchor("a2.5", 2.5)); // I
    g.addAnchor(new Anchor("a2.75", 2.75)); // k
    g.addAnchor(new Anchor("a3", 3.0)); // brown
    g.addAnchor(new Anchor("a4", 4.0)); // fox & N
    // unset offsets
    g.addAnchor(new Anchor("a?1", null)); // jumps
    g.addAnchor(new Anchor("a?2", null)); // over
    g.addAnchor(new Anchor("a5", 5.0)); // end of over
    g.addAnchor(new Anchor("a6", 6.0)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a6", "my graph"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a6", "participant1"));

    g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("word2", "quick", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("word3", "brown", "word", "a3", "a4", "turn1"));
    g.addAnnotation(new Annotation("word4", "fox", "word", "a4", "a?1", "turn1"));
    g.addAnnotation(new Annotation("word5", "jumps", "word", "a?1", "a?2", "turn1"));
    g.addAnnotation(new Annotation("word6", "over", "word", "a?2", "a5", "turn1"));

    g.addAnnotation(new Annotation("orth1", "the", "orthography", null, null, "word1"));
    g.addAnnotation(new Annotation("orth2", "quick", "orthography", null, null, "word2"));
    g.addAnnotation(new Annotation("orth2.5", "deleteme", "orthography", null, null, "word2"));
    g.addAnnotation(new Annotation("orth3", "willbedeleted", "orthography", null, null, "word3"));
    g.addAnnotation(new Annotation("orth3.5", "brown", "orthography", null, null, "word3"));
    g.addAnnotation(new Annotation("orth4", "fox", "orthography", null, null, "word4"));
    g.addAnnotation(new Annotation("orth5", "jumps", "orthography", null, null, "word5"));
    g.addAnnotation(new Annotation("orth6", "over", "orthography", null, null, "word6"));
      
    g.getAnnotation("orth3").destroy();

    Validator v = new Validator();
    v.setFullValidation(true);
    // v.setDebug(true);
    v.setDefaultOffsetThreshold(null);
    try
    {
      Vector<Change> changes = v.transform(g);
      if (v.getLog() != null) for (String m : v.getLog()) System.out.println(m);
      assertNotEquals("changes applied", 0, changes.size());
      assertEquals("extra peer deleted", Change.Operation.Destroy, g.getAnnotation("orth2.5").getChange());
      assertNotEquals("deleted peers skipped", Change.Operation.Destroy, g.getAnnotation("orth3.5").getChange());
    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }

  @Test public void canValidateFragment() 
  {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");

    g.addLayer(new Layer("topic", "Topics", 2, true, false, false));
    g.addLayer(new Layer("who", "Participants", 0, true, true, true));
    g.addLayer(new Layer("turn", "Speaker turns", 2, true, false, false, "who", true));
    g.addLayer(new Layer("utterance", "Utterances", 2, true, false, true, "turn", true));
    g.addLayer(new Layer("word", "Words", 2, true, false, false, "turn",true));
    g.addLayer(new Layer("phone", "Phones", 2, true, false, true, "word",  true));
    g.addLayer(new Layer("pos", "Part of speech", 0, false, false, true, "word", true));
    g.addLayer(new Layer("phrase", "Phrase structure", 0, true, true, false, "turn", true));

    g.addAnchor(new Anchor("turnStart", 0.0));
    g.addAnchor(new Anchor("a1", 1.0));
    g.addAnchor(new Anchor("a1.5", 1.5));
    g.addAnchor(new Anchor("a2", 2.0));
    // include null offset anchor, it should still be added to the fragment
    g.addAnchor(new Anchor("a2.25", null));
    g.addAnchor(new Anchor("a2.5", 2.5));
    g.addAnchor(new Anchor("a2.75", 2.75));
    g.addAnchor(new Anchor("a3", 3.0));
    g.addAnchor(new Anchor("a4", 4.0));
    g.addAnchor(new Anchor("a5", 5.0));
    g.addAnchor(new Anchor("turnEnd", 6.0));

    Annotation who1 = new Annotation("who1", "john smith", "who", "turnStart", "turnEnd", "my graph");
    Annotation who2 = new Annotation("who2", "jane doe", "who", "turnStart", "turnEnd", "my graph");

    Annotation turn1 = new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "who1");

    Annotation utterance1 = new Annotation("utterance1", "john smith", "utterance", "turnStart", "a3", "turn1");
    Annotation utterance2 = new Annotation("utterance2", "john smith", "utterance", "a3", "turnEnd", "turn1");

    Annotation the = new Annotation("word1", "the", "word", "a1", "a2", "turn1");
    Annotation DT = new Annotation("pos1", "DT", "pos", "a1", "a2", "word1");
    Annotation th = new Annotation("phone1", "D", "phone", "a1", "a1.5", "word1");
    Annotation e = new Annotation("phone2", "@", "phone", "a1.5", "a2", "word1");
    Annotation quick = new Annotation("word2", "quick", "word", "a2", "a3", "turn1");
    Annotation A = new Annotation("pos2", "A", "pos", "a2", "a3", "word2");
    Annotation k = new Annotation("phone3", "k", "phone", "a2", "a2.25", "word2");
    Annotation w = new Annotation("phone4", "w", "phone", "a2.25", "a2.5", "word2");
    Annotation I = new Annotation("phone5", "I", "phone", "a2.5", "a2.75", "word2");
    Annotation ck = new Annotation("phone6", "k", "phone", "a2.75", "a3", "word2");
    Annotation brown = new Annotation("word3", "brown", "word", "a3", "a4", "turn1");
    Annotation AP = new Annotation("phrase1", "AP", "phrase", "a2", "a4", "turn1");
    Annotation fox = new Annotation("word4", "fox", "word", "a4", "a5", "turn1");
    Annotation N = new Annotation("pos3", "N", "pos", "a4", "a5", "word4");
    Annotation NP = new Annotation("phrase2", "NP", "phrase", "a1", "a5", "turn1");

    g.addAnnotation(who1);
    g.addAnnotation(turn1);
    g.addAnnotation(utterance1);
    g.addAnnotation(utterance2);

    g.addAnnotation(the);
    g.addAnnotation(quick);
    g.addAnnotation(brown);
    g.addAnnotation(fox);

    g.addAnnotation(th);
    g.addAnnotation(e);
    g.addAnnotation(k);
    g.addAnnotation(w);
    g.addAnnotation(I);
    g.addAnnotation(ck);

    g.addAnnotation(DT);
    g.addAnnotation(A);
    g.addAnnotation(N);

    g.addAnnotation(AP);
    g.addAnnotation(NP);

    assertFalse(g.isFragment());

    // create fragment
    Vector<String> layers = new Vector<String>();
    layers.add("utterance");
    layers.add("word");
    layers.add("phone");
    assertEquals(2, g.getAnnotation("utterance2").getOrdinal());
    Graph f = g.getFragment(utterance2, layers.toArray(new String[0]));
    assertEquals(2, f.getAnnotation("utterance2").getOrdinal());
    f.getAnnotation("turn1").getAnnotations("word");
    assertEquals("word ordinal before validation",
                 3, f.getAnnotation("word3").getOrdinal());
    assertEquals("word ordinal before validation",
                 4, f.getAnnotation("word4").getOrdinal());
    assertNotEquals(f.getAnnotation("turn1").getStart(), 
                    f.getAnnotation("utterance2").getStart());
    assertTrue(f.isFragment());

    Validator v = new Validator();
    v.setFullValidation(true);
    // v.setDebug(true);
    try
    {
      Vector<Change> changes = v.transform(f);
      if (v.getLog() != null) for (String m : v.getLog()) System.out.println(m);
      assertEquals(2, f.getAnnotation("utterance2").getOrdinal());
      assertNotEquals(f.getAnnotation("turn1").getStart(), 
                      f.getAnnotation("utterance2").getStart());
      assertEquals("word ordinal after validation",
                   3, f.getAnnotation("word3").getOrdinal());
      assertEquals("word ordinal after validation",
                   4, f.getAnnotation("word4").getOrdinal());
      assertEquals("no changes applied", 0, changes.size());
    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }

  @Test public void validateHierarchyParentChildSynchronicity() // TODO saturated anchor sharing, and non-saturated by parent-including violations
  {
  }

  public static void main(String args[]) 
  {
    org.junit.runner.JUnitCore.main("nzilbb.ag.util.test.TestValidator");
  }
}
