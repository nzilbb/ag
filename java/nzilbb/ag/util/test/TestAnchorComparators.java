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

import java.util.TreeSet;
import java.util.Iterator;
import nzilbb.ag.*;
import nzilbb.ag.util.*;

public class TestAnchorComparators
{
      
   @Test public void includingStructure() 
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
      g.addLayer(new Layer("utterance", "Speaker utterances", 2, true, false, true, "turn", true));
      g.addLayer(new Layer("word", "Words", 2, true, false, false, "turn", true));
      g.addLayer(new Layer("phone", "Phones", 2, true, false, true, "word", true));
      g.addLayer(new Layer("pos", "Part of speech", 0, false, false, true, "word", true));
      g.addLayer(new Layer("phrase", "Phrase structure", 2, true, true, false, "turn", true));

      // add anchors out of offset and ID order...

      g.addAnchor(new Anchor("startTurn1", 0.0)); // turn start
      g.addAnchor(new Anchor("utteranceChange", 3.0)); // turn end/start
      g.addAnchor(new Anchor("endTurn1StartTurn2", 6.0)); // turn end/start
      g.addAnchor(new Anchor("endTurn2", 9.0)); // turn end

      // turn1 utterance1
      g.addAnchor(new Anchor("startThe", 1.0)); // the & DT & D & NP
      g.addAnchor(new Anchor("middleThe", 1.5)); // @
      g.addAnchor(new Anchor("startQuick", 2.0)); // quick & A & k & AP
      g.addAnchor(new Anchor("a2.25", 2.25)); // w
      g.addAnchor(new Anchor("a2.5", 2.5)); // I
      g.addAnchor(new Anchor("a2.75", 2.75)); // k
      g.addAnchor(new Anchor("endQuick", 3.0)); // quickEnd
      // utterance2
      g.addAnchor(new Anchor("startBrown", 3.0)); // brown
      g.addAnchor(new Anchor("endBrown", 3.5)); // brown end (pause)
      g.addAnchor(new Anchor("startFox", 4.0)); // fox & N
      // unset offsets
      g.addAnchor(new Anchor("startJumpsNoOffset", null)); // jumps & _
      g.addAnchor(new Anchor("startU", 5.3)); // V
      g.addAnchor(new Anchor("startMNoOffset", null)); // m
      g.addAnchor(new Anchor("startP", 5.5)); // p
      g.addAnchor(new Anchor("startSNoOffset", null)); // s
      g.addAnchor(new Anchor("startOverNoOffset", null)); // over
      g.addAnchor(new Anchor("endOver", 6.0)); // end of over

      // turn 2
      g.addAnchor(new Anchor("startSecondThe", 6.0)); // the
      // last words are 'instantaneous' (not shared anchors, but same offsets) with end of turn
      // - not t-included, nor mindpoint-included, but it still has to get the order right
      g.addAnchor(new Anchor("startLazy", 9.0)); // lazy
      g.addAnchor(new Anchor("startDog", 9.0)); // dog
      g.addAnchor(new Anchor("endDog", 9.0)); // dog end

      // speakers in reverse turn order
      g.addAnnotation(new Annotation("jane doe", "jane doe", "who", "startTurn1", "endTurn2", "my graph"));
      g.addAnnotation(new Annotation("john smith", "john smith", "who", "startTurn1", "endTurn2", "my graph"));

      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "startTurn1", "endTurn1StartTurn2", "john smith"));
      g.addAnnotation(new Annotation("turn2", "jane doe", "turn", "endTurn1StartTurn2", "endTurn2", "jane doe"));

      g.addAnnotation(new Annotation("utterance1", "jane doe", "utterance", "startTurn1", "utteranceChange", "turn1"));
      g.addAnnotation(new Annotation("utterance2", "jane doe", "utterance", "utteranceChange", "endTurn1StartTurn2", "turn1"));

      g.addAnnotation(new Annotation("word1", "the", "word", "startThe", "startQuick", "turn1"));
      g.addAnnotation(new Annotation("pos1", "DT", "pos", "startThe", "startQuick", "word1"));
      g.addAnnotation(new Annotation("phone1", "D", "phone", "startThe", "middleThe", "word1"));
      g.addAnnotation(new Annotation("phone2", "@", "phone", "middleThe", "startQuick", "word1"));
      g.addAnnotation(new Annotation("word2", "quick", "word", "startQuick", "endQuick", "turn1"));
      g.addAnnotation(new Annotation("pos2", "A", "pos", "startQuick", "endQuick", "word2"));
      g.addAnnotation(new Annotation("phone3", "k", "phone", "startQuick", "a2.25", "word2"));
      g.addAnnotation(new Annotation("phone4", "w", "phone", "a2.25", "a2.5", "word2"));
      g.addAnnotation(new Annotation("phone5", "I", "phone", "a2.5", "a2.75", "word2"));
      g.addAnnotation(new Annotation("phone6", "k", "phone", "a2.75", "endQuick", "word2"));
      g.addAnnotation(new Annotation("word3", "brown", "word", "startBrown", "endBrown", "turn1"));
      g.addAnnotation(new Annotation("phrase1", "AP", "phrase", "startQuick", "startFox", "turn1"));
      g.addAnnotation(new Annotation("word4", "fox", "word", "startFox", "startJumpsNoOffset", "turn1"));
      g.addAnnotation(new Annotation("pos3", "N", "pos", "startFox", "startJumpsNoOffset", "word4"));
      g.addAnnotation(new Annotation("phrase2", "NP", "phrase", "startThe", "startFox", "turn1"));
      g.addAnnotation(new Annotation("word5", "jumps", "word", "startJumpsNoOffset", "startOverNoOffset", "turn1"));
      g.addAnnotation(new Annotation("phone7", "_", "phone", "startJumpsNoOffset", "startU", "word5"));
      g.addAnnotation(new Annotation("phone7", "V", "phone", "startU", "startMNoOffset", "word5"));
      g.addAnnotation(new Annotation("phone7", "m", "phone", "startMNoOffset", "startP", "word5"));
      g.addAnnotation(new Annotation("phone7", "p", "phone", "startP", "startSNoOffset", "word5"));
      g.addAnnotation(new Annotation("phone7", "s", "phone", "startSNoOffset", "startOverNoOffset", "word5"));
      g.addAnnotation(new Annotation("word6", "over", "word", "startOverNoOffset", "endOver", "turn1"));

      g.addAnnotation(new Annotation("word7", "the", "word", "startSecondThe", "startLazy", "turn2"));
      g.addAnnotation(new Annotation("word8", "lazy", "word", "startLazy", "startDog", "turn2"));
      g.addAnnotation(new Annotation("word9", "dog", "word", "startDog", "endDog", "turn2"));

      AnchorComparatorWithStructure comparator = new AnchorComparatorWithStructure();

      // test individual comparisons

      assertTrue("equality", 
		 comparator.compare(g.getAnchor("startTurn1"), g.getAnchor("startTurn1")) == 0);
      assertTrue("by offset", 
		 comparator.compare(g.getAnchor("startTurn1"), g.getAnchor("startThe")) < 0);
      assertTrue("by offset", 
		 comparator.compare(g.getAnchor("startThe"), g.getAnchor("startTurn1")) > 0);

      assertTrue("one offset - by offsetMax/Min direct", 
		 comparator.compare(g.getAnchor("startJumpsNoOffset"), g.getAnchor("endOver")) < 0);
      assertTrue("one offset - by offsetMax/Min direct", 
		 comparator.compare(g.getAnchor("endOver"), g.getAnchor("startJumpsNoOffset")) > 0);
      assertTrue("one offset - by offsetMax/Min chained", 
		 comparator.compare(g.getAnchor("startU"), g.getAnchor("startOverNoOffset")) < 0);
      assertTrue("one offset - by offsetMax/Min chaind", 
		 comparator.compare(g.getAnchor("startOverNoOffset"), g.getAnchor("startU")) > 0);

      assertTrue("one offset - by offsetMax/Min indirect", 
		 comparator.compare(g.getAnchor("startJumpsNoOffset"), g.getAnchor("endOver")) < 0);
      assertTrue("one offset - by offsetMax/Min indirect", 
		 comparator.compare(g.getAnchor("endOver"), g.getAnchor("startJumpsNoOffset")) > 0);

      assertTrue("no offsets - by offsetMax/Min direct", 
		 comparator.compare(g.getAnchor("startJumpsNoOffset"), g.getAnchor("startSNoOffset")) < 0);
      assertTrue("no offsets - by offsetMax/Min direct", 
		 comparator.compare(g.getAnchor("startSNoOffset"), g.getAnchor("startJumpsNoOffset")) > 0);
      assertTrue("no offsets - by offsetMax/Min chained", 
		 comparator.compare(g.getAnchor("startJumpsNoOffset"), g.getAnchor("startOverNoOffset")) < 0);
      assertTrue("no offsets - by offsetMax/Min chained", 
		 comparator.compare(g.getAnchor("startOverNoOffset"), g.getAnchor("startJumpsNoOffset")) > 0);

      assertTrue("no offsets - one intervening anchored anchor  - by offsetMax/Min chained", 
		 comparator.compare(g.getAnchor("startJumpsNoOffset"), g.getAnchor("startMNoOffset")) < 0);
      assertTrue("no offsets - one intervening anchored anchor  - by offsetMax/Min chained", 
		 comparator.compare(g.getAnchor("startMNoOffset"), g.getAnchor("startJumpsNoOffset")) > 0);

      assertTrue("no offsets - chained", 
		 comparator.compare(g.getAnchor("startSNoOffset"), g.getAnchor("startOverNoOffset")) < 0);
      assertTrue("no offsets - chained", 
		 comparator.compare(g.getAnchor("startOverNoOffset"), g.getAnchor("startSNoOffset")) > 0);

      assertTrue("equals offsets - starting/ending only", 
		 comparator.compare(g.getAnchor("endOver"), g.getAnchor("startSecondThe")) < 0);
      assertTrue("equals offsets - starting/ending only", 
		 comparator.compare(g.getAnchor("startSecondThe"), g.getAnchor("endOver")) > 0);
      assertTrue("equals offsets - ending only", 
		 comparator.compare(g.getAnchor("endOver"), g.getAnchor("endTurn1StartTurn2")) < 0);
      assertTrue("equals offsets - ending only", 
		 comparator.compare(g.getAnchor("endTurn1StartTurn2"), g.getAnchor("endOver")) > 0);
      assertTrue("equals offsets - starting only", 
		 comparator.compare(g.getAnchor("endTurn1StartTurn2"), g.getAnchor("startSecondThe")) < 0);
      assertTrue("equals offsets - starting only", 
		 comparator.compare(g.getAnchor("startSecondThe"), g.getAnchor("endTurn1StartTurn2")) > 0);

      assertTrue("compare by ordinal", 
		 comparator.compare(g.getAnchor("startFox"), g.getAnchor("startJumpsNoOffset")) < 0);
      assertTrue("compare by ordinal", 
		 comparator.compare(g.getAnchor("startJumpsNoOffset"), g.getAnchor("startFox")) > 0);

      assertTrue("compare ancestry", 
		 comparator.compare(g.getAnchor("startTurn1"), g.getAnchor("startJumpsNoOffset")) < 0);
      assertTrue("compare ancestry", 
		 comparator.compare(g.getAnchor("startJumpsNoOffset"), g.getAnchor("startTurn1")) > 0);
      assertTrue("compare ancestry", 
		 comparator.compare(g.getAnchor("startJumpsNoOffset"), g.getAnchor("endTurn1StartTurn2")) < 0);
      assertTrue("compare ancestry", 
		 comparator.compare(g.getAnchor("endTurn1StartTurn2"), g.getAnchor("startJumpsNoOffset")) > 0);

      assertTrue("compare ancestry - non-t-included child", 
		 comparator.compare(g.getAnchor("startDog"), g.getAnchor("endTurn2")) < 0);
      assertTrue("compare ancestry - non-t-included child", 
		 comparator.compare(g.getAnchor("endTurn2"), g.getAnchor("startDog")) > 0);
      
      assertEquals("endTurn2", g.getAnnotation("turn2").getEndId());
      assertEquals("endDog", g.getAnnotation("word9").getEndId());
      assertEquals(new Double(9.0), g.getAnnotation("turn2").getEnd().getOffset());
      assertEquals(new Double(9.0), g.getAnnotation("word9").getEnd().getOffset());
      assertEquals("turn2", g.getAnnotation("word9").getParentId());
      assertTrue("compare ancestry - non-t-included child: " + comparator.compare(g.getAnchor("endDog"), g.getAnchor("endTurn2")), 
		 comparator.compare(g.getAnchor("endDog"), g.getAnchor("endTurn2")) < 0);
      
      assertTrue("compare ancestry - non-t-included child", 
		 comparator.compare(g.getAnchor("endTurn2"), g.getAnchor("endDog")) > 0);

      // test sorting
      TreeSet<Anchor> anchors = new TreeSet<Anchor>(comparator);
      anchors.addAll(g.getAnchors().values());
      // System.out.println(""+anchors);
      Iterator<Anchor> order = anchors.iterator();
      assertEquals("startTurn1", order.next().getId());
      assertEquals("startThe", order.next().getId());
      assertEquals("middleThe", order.next().getId());
      assertEquals("startQuick", order.next().getId());
      assertEquals("a2.25", order.next().getId());
      assertEquals("a2.5", order.next().getId());
      assertEquals("a2.75", order.next().getId());
      assertEquals("endQuick", order.next().getId());
      assertEquals("utteranceChange", order.next().getId());
      assertEquals("startBrown", order.next().getId());
      assertEquals("endBrown", order.next().getId());
      assertEquals("startFox", order.next().getId());

      // unset offsets
      assertEquals("startJumpsNoOffset", order.next().getId());
      assertEquals("startU", order.next().getId());
      assertEquals("startMNoOffset", order.next().getId());
      assertEquals("startP", order.next().getId());
      assertEquals("startSNoOffset", order.next().getId());
      assertEquals("startOverNoOffset", order.next().getId());

      assertEquals("endOver", order.next().getId());
      assertEquals("endTurn1StartTurn2", order.next().getId());
      assertEquals("startSecondThe", order.next().getId());
      assertEquals("startLazy", order.next().getId());
      assertEquals("startDog", order.next().getId());
      assertEquals("endDog", order.next().getId());
      assertEquals("endTurn2", order.next().getId());
      assertFalse(order.hasNext());
   }

   @Test public void includingStructureNoOffsets() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      g.setCorpus("cc");

      g.addLayer(new Layer("turn", "Speaker turns", 2, true, true, false));
      g.addLayer(new Layer("word", "Words", 2, true, false, false, "turn", true));
      g.addLayer(new Layer("phone", "Phones", 2, true, false, true, "word", true));
      g.addLayer(new Layer("pos", "Part of speech", 0, false, false, true, "word", true));
      g.addLayer(new Layer("phrase", "Phrase structure", 2, true, true, false, "turn", true));

      g.addAnchor(new Anchor("startTurn1", null)); // turn start
      g.addAnchor(new Anchor("endTurn1StartTurn2", null)); // turn end/start
      g.addAnchor(new Anchor("endTurn2", null)); // turn end

      g.addAnchor(new Anchor("startThe", null)); // the & DT & D & NP
      g.addAnchor(new Anchor("middleThe", null)); // @
      g.addAnchor(new Anchor("startQuick", null)); // quick & A & k & AP
      g.addAnchor(new Anchor("startW", null)); // w
      g.addAnchor(new Anchor("startI", null)); // I
      g.addAnchor(new Anchor("startK", null)); // k
      g.addAnchor(new Anchor("startBrown", null)); // brown
      g.addAnchor(new Anchor("endBrown", null)); // brown end (pause)
      g.addAnchor(new Anchor("startFox", null)); // fox & N
      g.addAnchor(new Anchor("startJumps", null)); // jumps & _
      g.addAnchor(new Anchor("startU", null)); // V
      g.addAnchor(new Anchor("startM", null)); // m
      g.addAnchor(new Anchor("startP", null)); // p
      g.addAnchor(new Anchor("startS", null)); // s
      g.addAnchor(new Anchor("startOver", null)); // over
      g.addAnchor(new Anchor("endOver", null)); // end of over
      g.addAnchor(new Anchor("startSecondThe", null)); // the
      g.addAnchor(new Anchor("startLazy", null)); // lazy
      g.addAnchor(new Anchor("startDog", null)); // dog
      g.addAnchor(new Anchor("endDog", null)); // dog end

      // add a couple of totally unconnnected anchors
      g.addAnchor(new Anchor("aaa-first", null));
      g.addAnchor(new Anchor("zzz-last", null));

      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "startTurn1", "endTurn1StartTurn2", "my graph"));
      g.addAnnotation(new Annotation("turn2", "jane doe", "turn", "endTurn1StartTurn2", "endTurn2", "my graph"));
      g.addAnnotation(new Annotation("word1", "the", "word", "startThe", "startQuick", "turn1"));
      g.addAnnotation(new Annotation("pos1", "DT", "pos", "startThe", "startQuick", "word1"));
      g.addAnnotation(new Annotation("phone1", "D", "phone", "startThe", "middleThe", "word1"));
      g.addAnnotation(new Annotation("phone2", "@", "phone", "middleThe", "startQuick", "word1"));
      g.addAnnotation(new Annotation("word2", "quick", "word", "startQuick", "startBrown", "turn1"));
      g.addAnnotation(new Annotation("pos2", "A", "pos", "startQuick", "startBrown", "word2"));
      g.addAnnotation(new Annotation("phone3", "k", "phone", "startQuick", "startW", "word2"));
      g.addAnnotation(new Annotation("phone4", "w", "phone", "startW", "startI", "word2"));
      g.addAnnotation(new Annotation("phone5", "I", "phone", "startI", "startK", "word2"));
      g.addAnnotation(new Annotation("phone6", "k", "phone", "startK", "startBrown", "word2"));
      g.addAnnotation(new Annotation("word3", "brown", "word", "startBrown", "endBrown", "turn1"));
      g.addAnnotation(new Annotation("phrase1", "AP", "phrase", "startQuick", "startFox", "turn1"));
      g.addAnnotation(new Annotation("word4", "fox", "word", "startFox", "startJumps", "turn1"));
      g.addAnnotation(new Annotation("pos3", "N", "pos", "startFox", "startJumps", "word4"));
      g.addAnnotation(new Annotation("phrase2", "NP", "phrase", "startThe", "startFox", "turn1"));
      g.addAnnotation(new Annotation("word5", "jumps", "word", "startJumps", "startOver", "turn1"));
      g.addAnnotation(new Annotation("phone7", "_", "phone", "startJumps", "startU", "word5"));
      g.addAnnotation(new Annotation("phone7", "V", "phone", "startU", "startM", "word5"));
      g.addAnnotation(new Annotation("phone7", "m", "phone", "startM", "startP", "word5"));
      g.addAnnotation(new Annotation("phone7", "p", "phone", "startP", "startS", "word5"));
      g.addAnnotation(new Annotation("phone7", "s", "phone", "startS", "startOver", "word5"));
      g.addAnnotation(new Annotation("word6", "over", "word", "startOver", "endOver", "turn1"));

      g.addAnnotation(new Annotation("word7", "the", "word", "startSecondThe", "startLazy", "turn2"));
      g.addAnnotation(new Annotation("word8", "lazy", "word", "startLazy", "startDog", "turn2"));
      g.addAnnotation(new Annotation("word9", "dog", "word", "startDog", "endDog", "turn2"));

      AnchorComparatorWithStructure comparator = new AnchorComparatorWithStructure();

      // test individual comparisons

      assertTrue("equality", 
		 comparator.compare(g.getAnchor("startTurn1"), g.getAnchor("startTurn1")) == 0);

      assertTrue("chained", 
		 comparator.compare(g.getAnchor("startS"), g.getAnchor("startOver")) < 0);
      assertTrue("chained", 
		 comparator.compare(g.getAnchor("startOver"), g.getAnchor("startS")) > 0);

      assertTrue("compare by ordinal", 
		 comparator.compare(g.getAnchor("startFox"), g.getAnchor("startJumps")) < 0);
      assertTrue("compare by ordinal", 
		 comparator.compare(g.getAnchor("startJumps"), g.getAnchor("startFox")) > 0);

      assertTrue("compare ancestry", 
		 comparator.compare(g.getAnchor("startTurn1"), g.getAnchor("startJumps")) < 0);
      assertTrue("compare ancestry", 
		 comparator.compare(g.getAnchor("startJumps"), g.getAnchor("startTurn1")) > 0);
      assertTrue("compare ancestry", 
		 comparator.compare(g.getAnchor("startJumps"), g.getAnchor("endTurn1StartTurn2")) < 0);
      assertTrue("compare ancestry", 
		 comparator.compare(g.getAnchor("endTurn1StartTurn2"), g.getAnchor("startJumps")) > 0);

      assertTrue("compare ancestor ordinals", 
		 comparator.compare(g.getAnchor("startW"), g.getAnchor("startM")) < 0);
      assertTrue("compare ancestry ordinals", 
		 comparator.compare(g.getAnchor("startM"), g.getAnchor("startW")) > 0);
      assertTrue("compare ancestor ordinals, ancestor parent is graph", 
		 comparator.compare(g.getAnchor("startTurn1"), g.getAnchor("endDog")) < 0);
      assertTrue("compare ancestor ordinals, ancestor parent is graph", 
		 comparator.compare(g.getAnchor("endDog"), g.getAnchor("startTurn1")) > 0);

      assertTrue("fallback to id comparison", 
		 comparator.compare(g.getAnchor("aaa-first"), g.getAnchor("startM")) < 0);
      assertTrue("fallback to id comparison", 
		 comparator.compare(g.getAnchor("startM"), g.getAnchor("aaa-first")) > 0);
      assertTrue("fallback to id comparison", 
		 comparator.compare(g.getAnchor("startM"), g.getAnchor("zzz-last")) < 0);
      assertTrue("fallback to id comparison", 
		 comparator.compare(g.getAnchor("zzz-last"), g.getAnchor("startM")) > 0);

      // test sorting
      TreeSet<Anchor> anchors = new TreeSet<Anchor>(comparator);
      anchors.addAll(g.getAnchors().values());
      // System.out.println(""+anchors);
      Iterator<Anchor> order = anchors.iterator();
      assertEquals("aaa-first", order.next().getId());
      assertEquals("startTurn1", order.next().getId());
      assertEquals("startThe", order.next().getId());
      assertEquals("middleThe", order.next().getId());
      assertEquals("startQuick", order.next().getId());
      assertEquals("startW", order.next().getId());
      assertEquals("startI", order.next().getId());
      assertEquals("startK", order.next().getId());
      assertEquals("startBrown", order.next().getId());
      assertEquals("endBrown", order.next().getId());
      assertEquals("startFox", order.next().getId());

      // unset offsets
      assertEquals("startJumps", order.next().getId());
      assertEquals("startU", order.next().getId());
      assertEquals("startM", order.next().getId());
      assertEquals("startP", order.next().getId());
      assertEquals("startS", order.next().getId());
      assertEquals("startOver", order.next().getId());

      assertEquals("endOver", order.next().getId());
      assertEquals("endTurn1StartTurn2", order.next().getId());
      assertEquals("startSecondThe", order.next().getId());
      assertEquals("startLazy", order.next().getId());
      assertEquals("startDog", order.next().getId());
      assertEquals("endDog", order.next().getId());
      assertEquals("endTurn2", order.next().getId());
      assertEquals("zzz-last", order.next().getId());
      assertFalse(order.hasNext());
   }

   @Test public void includingStructureBasicLinearInterpolationCase()
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

      AnchorComparatorWithStructure comparator = new AnchorComparatorWithStructure();

      assertTrue("parent start before child start", 
		 comparator.compare(g.getAnchor("turnStart"), g.getAnchor("a0")) < 0);
      assertTrue("parent end after child end", 
		 comparator.compare(g.getAnchor("a9"), g.getAnchor("turnEnd")) < 0);

      // test sorting
      TreeSet<Anchor> anchors = new TreeSet<Anchor>(comparator);
      anchors.addAll(g.getAnchors().values());
      // System.out.println(""+anchors);
      Iterator<Anchor> order = anchors.iterator();
      assertEquals("turnStart", order.next().getId());
      assertEquals("a0", order.next().getId());
      assertEquals("a1", order.next().getId());
      assertEquals("a2", order.next().getId());
      assertEquals("a3", order.next().getId());
      assertEquals("a4", order.next().getId());
      assertEquals("a5", order.next().getId());
      assertEquals("a6", order.next().getId());
      assertEquals("a7", order.next().getId());
      assertEquals("a8", order.next().getId());
      assertEquals("a9", order.next().getId());
      assertEquals("turnEnd", order.next().getId());
      assertFalse(order.hasNext());

   }

   @Test public void includingStructureInvalidStartCase()
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
      g.addAnchor(new Anchor("thatStart", 129.9, Constants.CONFIDENCE_DEFAULT));
      g.addAnchor(new Anchor("a2Start", null));
      g.addAnchor(new Anchor("tStart", null));
      g.addAnchor(new Anchor("yeahStart", 129.5, Constants.CONFIDENCE_DEFAULT));
      g.addAnchor(new Anchor("eahStart", 129.6, Constants.CONFIDENCE_DEFAULT));
      g.addAnchor(new Anchor("yeahEnd", 129.7, Constants.CONFIDENCE_DEFAULT));
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

      AnchorComparatorWithStructure comparator = new AnchorComparatorWithStructure();

      assertEquals("order without offsets", 
		   -2, comparator.compare(g.getAnchor("thatStart"), g.getAnchor("a2Start")));
      assertTrue("order without offsets", 
		 comparator.compare(g.getAnchor("a2Start"), g.getAnchor("tStart")) < 0);
      assertTrue("order without offsets", 
		 comparator.compare(g.getAnchor("tStart"), g.getAnchor("yeahStart")) < 0);

      // test sorting
      TreeSet<Anchor> anchors = new TreeSet<Anchor>(comparator);
      anchors.addAll(g.getAnchors().values());
      // System.out.println(""+anchors);
      Iterator<Anchor> order = anchors.iterator();
      assertEquals("turnStart", order.next().getId());
      assertEquals("thanStart", order.next().getId());
      assertEquals("a1Start", order.next().getId());
      assertEquals("nStart", order.next().getId());
      assertEquals("thatStart", order.next().getId());
      assertEquals("a2Start", order.next().getId());
      assertEquals("tStart", order.next().getId());
      assertEquals("yeahStart", order.next().getId());
      assertEquals("eahStart", order.next().getId());
      assertEquals("yeahEnd", order.next().getId());
      assertEquals("turnEnd", order.next().getId());
      assertFalse(order.hasNext());

   }

   @Test public void peerWordUtteranceCase()
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
      g.addLayer(new Layer("transcript", "Words", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   false, // saturated
			   "turn", // parentId
			   true)); // parentIncludes
      g.addLayer(new Layer("utterance", "Utterances", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   true, // saturated
			   "turn", // parentId
			   true)); // parentIncludes

      g.addAnchor(new Anchor("1", 10.0, // the
			     Constants.CONFIDENCE_DEFAULT));
      g.addAnchor(new Anchor("n_101", 10.0, // quick
			     Constants.CONFIDENCE_DEFAULT));
      g.addAnchor(new Anchor("n_100", 10.0, // turn start 
			     Constants.CONFIDENCE_MANUAL));
      g.addAnchor(new Anchor("n_300", 25.0, // brown
			     Constants.CONFIDENCE_DEFAULT));
      g.addAnchor(new Anchor("n_399", 40.0, // brown end
			     Constants.CONFIDENCE_DEFAULT));
      g.addAnchor(new Anchor("n_400", 40.0, // utterance break
			     Constants.CONFIDENCE_MANUAL));
      g.addAnchor(new Anchor("n_401", 40.0, // fox
			     Constants.CONFIDENCE_DEFAULT));
      g.addAnchor(new Anchor("4", 50.0, // jumps
			     Constants.CONFIDENCE_DEFAULT));
      g.addAnchor(new Anchor("n_500", 60.0, // over
			     Constants.CONFIDENCE_DEFAULT));
      g.addAnchor(new Anchor("n_600", 70.0, // over end
			     Constants.CONFIDENCE_DEFAULT));
      g.addAnchor(new Anchor("n_700", 70.0, // turn end
			     Constants.CONFIDENCE_MANUAL));

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "n_100", "n_700", "my graph"));
      
      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "n_100", "n_700", "participant1"));

      g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "n_100", "n_400", "turn1"));
      g.addAnnotation(new Annotation("utterance2", "john smith", "utterance", "n_400", "n_700", "turn1"));
      
      g.addAnnotation(new Annotation("the", "the", "transcript", "1", "n_101", "turn1"));
      g.addAnnotation(new Annotation("quick", "quick", "transcript", "n_101", "n_300", "turn1"));
      g.addAnnotation(new Annotation("brown", "brown", "transcript", "n_300", "n_399", "turn1"));
      g.addAnnotation(new Annotation("fox", "fox", "transcript", "n_401", "4", "turn1"));
      g.addAnnotation(new Annotation("jumps", "jumps", "transcript", "4", "n_500", "turn1"));
      g.addAnnotation(new Annotation("over", "over", "transcript", "n_500", "n_600", "turn1"));
  
      AnchorComparatorWithStructure comparator = new AnchorComparatorWithStructure();

      assertTrue("parent start before child start", 
		 comparator.compare(g.getAnchor("n_100"), g.getAnchor("1")) < 0);
      assertTrue("parent start before child end", 
		 comparator.compare(g.getAnchor("n_100"), g.getAnchor("n_101")) < 0);
      assertTrue("parent end after child end", 
		 comparator.compare(g.getAnchor("n_600"), g.getAnchor("n_700")) < 0);

      // test sorting
      TreeSet<Anchor> anchors = new TreeSet<Anchor>(comparator);
      anchors.addAll(g.getAnchors().values());
      // System.out.println(""+anchors);
      Iterator<Anchor> order = anchors.iterator();
      assertEquals("n_100", order.next().getId());
      assertEquals("1", order.next().getId());
      assertEquals("n_101", order.next().getId());
      assertEquals("n_300", order.next().getId());
      assertEquals("n_399", order.next().getId());
      assertEquals("n_400", order.next().getId());
      assertEquals("n_401", order.next().getId());
      assertEquals("4", order.next().getId());
      assertEquals("n_500", order.next().getId());
      assertEquals("n_600", order.next().getId());
      assertEquals("n_700", order.next().getId());
      assertFalse(order.hasNext());

   }


   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.util.test.TestAnchorComparators");
   }
}
