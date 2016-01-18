//
// Copyright 2015 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of LaBB-CAT.
//
//    LaBB-CAT is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 2 of the License, or
//    (at your option) any later version.
//
//    LaBB-CAT is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with LaBB-CAT; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//

package nzilbb.ag.util.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.Vector;
import java.util.Iterator;
import java.util.HashSet;
import nzilbb.ag.util.*;
import nzilbb.ag.*;

public class TestAnnotationChain
{
      
   @Test public void noChain() 
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

      g.addAnchor(new Anchor("a0", 0.0)); // turn start
      g.addAnchor(new Anchor("a1", 1.0)); // the
      g.addAnchor(new Anchor("a2", 2.0)); // quick
      g.addAnchor(new Anchor("a3", 3.0)); // brown
      g.addAnchor(new Anchor("a4", 4.0)); // fox
      // unset offsets
      g.addAnchor(new Anchor("a?1", null)); // jumps
      g.addAnchor(new Anchor("a?2", null)); // over
      g.addAnchor(new Anchor("a5", 5.0)); // end of over
      g.addAnchor(new Anchor("a6", 6.0)); // turn end

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a6", "my graph"));

      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a6", "participant1"));

      g.addAnnotation(new Annotation("the", "the", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("quick", "quick", "word", "a2", "a3", "turn1"));
      g.addAnnotation(new Annotation("brown", "brown", "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("fox", "fox", "word", "a4", "a?1", "turn1"));
      g.addAnnotation(new Annotation("jumps", "jumps", "word", "a?1", "a?2", "turn1"));
      g.addAnnotation(new Annotation("over", "over", "word", "a?2", "a5", "turn1"));

      AnnotationChain chain = new AnnotationChain(g.getAnchor("a1"), g.getAnchor("a6"));
      assertEquals("no chain between the start of the first word and the end of the turn", 0, chain.size());
   }

   @Test public void oneLinkChain() 
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

      g.addAnchor(new Anchor("a0", 0.0)); // turn start
      g.addAnchor(new Anchor("a1", 1.0)); // the
      g.addAnchor(new Anchor("a2", 2.0)); // quick
      g.addAnchor(new Anchor("a3", 3.0)); // brown
      g.addAnchor(new Anchor("a4", 4.0)); // fox
      // unset offsets
      g.addAnchor(new Anchor("a?1", null)); // jumps
      g.addAnchor(new Anchor("a?2", null)); // over
      g.addAnchor(new Anchor("a5", 5.0)); // end of over
      g.addAnchor(new Anchor("a6", 6.0)); // turn end

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a6", "my graph"));

      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a6", "participant1"));

      g.addAnnotation(new Annotation("the", "the", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("quick", "quick", "word", "a2", "a3", "turn1"));
      g.addAnnotation(new Annotation("brown", "brown", "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("fox", "fox", "word", "a4", "a?1", "turn1"));
      g.addAnnotation(new Annotation("jumps", "jumps", "word", "a?1", "a?2", "turn1"));
      g.addAnnotation(new Annotation("over", "over", "word", "a?2", "a5", "turn1"));

      AnnotationChain chain = new AnnotationChain(g.getAnchor("a1"), g.getAnchor("a2"));
      assertEquals("one link between start and end of word", 1, chain.size());
      assertTrue("word is link between start and end", chain.contains(g.getAnnotation("the")));

      chain = new AnnotationChain(g.getAnchor("a4"), g.getAnchor("a?1"));
      assertEquals("end offset optional", 1, chain.size());
      assertTrue("end offset optional", chain.contains(g.getAnnotation("fox")));

      chain = new AnnotationChain(g.getAnchor("a?2"), g.getAnchor("a5"));
      assertEquals("start offset optional", 1, chain.size());
      assertTrue("start offset optional", chain.contains(g.getAnnotation("over")));

      chain = new AnnotationChain(g.getAnchor("a?1"), g.getAnchor("a?2"));
      assertEquals("offsets optional", 1, chain.size());
      assertTrue("offsets optional", chain.contains(g.getAnnotation("jumps")));
   }

   @Test public void multiLinkChain() 
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
      g.addLayer(new Layer("pause", "Pauses", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   false, // saturated
			   "turn", // parentId
			   true)); // parentIncludes

      g.addAnchor(new Anchor("a0", 0.0)); // turn start
      g.addAnchor(new Anchor("a1", 1.0)); // the
      g.addAnchor(new Anchor("a2", 2.0)); // quick
      g.addAnchor(new Anchor("a2.5", 2.5)); // pause
      g.addAnchor(new Anchor("a3", 3.0)); // brown
      g.addAnchor(new Anchor("a4", 4.0)); // fox
      // unset offsets
      g.addAnchor(new Anchor("a?1", null)); // jumps
      g.addAnchor(new Anchor("a?2", null)); // over
      g.addAnchor(new Anchor("a5", 5.0)); // end of over
      g.addAnchor(new Anchor("a6", 6.0)); // turn end

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a6", "my graph"));

      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a6", "participant1"));

      g.addAnnotation(new Annotation("the", "the", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("quick", "quick", "word", "a2", "a2.5", "turn1"));
      g.addAnnotation(new Annotation("pause", "pause", "pause", "a2.5", "a3", "turn1"));
      g.addAnnotation(new Annotation("brown", "brown", "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("fox", "fox", "word", "a4", "a?1", "turn1"));
      g.addAnnotation(new Annotation("jumps", "jumps", "word", "a?1", "a?2", "turn1"));
      g.addAnnotation(new Annotation("over", "over", "word", "a?2", "a5", "turn1"));

      AnnotationChain chain = new AnnotationChain(g.getAnchor("a3"), g.getAnchor("a5"));
      assertEquals("chain on same layer", 4, chain.size());
      assertTrue("chain on same layer", chain.contains(g.getAnnotation("brown")));
      assertTrue("chain on same layer", chain.contains(g.getAnnotation("fox")));
      assertTrue("chain on same layer", chain.contains(g.getAnnotation("jumps")));
      assertTrue("chain on same layer", chain.contains(g.getAnnotation("over")));

      chain = new AnnotationChain(g.getAnchor("a1"), g.getAnchor("a3"));
      assertEquals("chain across layers", 3, chain.size());
      assertTrue("chain across layers", chain.contains(g.getAnnotation("the")));
      assertTrue("chain across layers", chain.contains(g.getAnnotation("quick")));
      assertTrue("chain across layers", chain.contains(g.getAnnotation("pause")));

      chain = new AnnotationChain(g.getAnchor("a1"), g.getAnchor("a5"));
      assertEquals("detour", 7, chain.size());
      assertTrue("detour", chain.contains(g.getAnnotation("the")));
      assertTrue("detour", chain.contains(g.getAnnotation("quick")));
      assertTrue("detour", chain.contains(g.getAnnotation("pause")));
      assertTrue("detour", chain.contains(g.getAnnotation("brown")));
      assertTrue("detour", chain.contains(g.getAnnotation("fox")));
      assertTrue("detour", chain.contains(g.getAnnotation("jumps")));
      assertTrue("detour", chain.contains(g.getAnnotation("over")));

      HashSet<String> excludeLayer = new HashSet<String>();
      excludeLayer.add("pause");
      chain = new AnnotationChain(g.getAnchor("a1"), g.getAnchor("a5"), excludeLayer);
      assertEquals("exclude layer", 0, chain.size());

   }

   @Test public void cycleDetection() 
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

      g.addAnchor(new Anchor("a0", 0.0)); // turn start
      g.addAnchor(new Anchor("a1", 1.0)); // round
      g.addAnchor(new Anchor("a2", 2.0)); // and
      g.addAnchor(new Anchor("a3", 3.0)); // around with
      g.addAnchor(new Anchor("a4", 4.0)); // its
      // unset offsets
      g.addAnchor(new Anchor("a?1", null)); // own
      g.addAnchor(new Anchor("a?2", null)); // sound
      g.addAnchor(new Anchor("a5", 5.0)); // sound end
      g.addAnchor(new Anchor("a6", 6.0)); // turn end

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a6", "my graph"));

      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a6", "participant1"));

      g.addAnnotation(new Annotation("round", "round", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("and", "and", "word", "a2", "a3", "turn1"));
      // "around" loops back, which is invalid, but AnnotationChain shouldn't infinitely loop anyway
      g.addAnnotation(new Annotation("around", "around", "word", "a3", "a2", "turn1"));

      try
      {
	 AnnotationChain chain = new AnnotationChain(g.getAnchor("a1"), g.getAnchor("a5"));
	 assertEquals("cycle with no chain", 0, chain.size());
      }
      catch(java.lang.StackOverflowError exception)
      {
	 fail("cycle with no chain not skipped");
      }

      try
      {
	 g.addAnnotation(new Annotation("with", "with", "word", "a3", "a4", "turn1"));
	 g.addAnnotation(new Annotation("its", "its", "word", "a4", "a?1", "turn1"));
	 g.addAnnotation(new Annotation("own", "own", "word", "a?1", "a?2", "turn1"));
	 g.addAnnotation(new Annotation("sound", "sound", "word", "a?2", "a5", "turn1"));
	 
	 AnnotationChain chain = new AnnotationChain(g.getAnchor("a1"), g.getAnchor("a5"));
	 assertEquals("cycle with chain", 6, chain.size());
	 assertTrue("cycle with chain", chain.contains(g.getAnnotation("round")));
	 assertTrue("cycle with chain", chain.contains(g.getAnnotation("and")));
	 assertTrue("cycle with chain", chain.contains(g.getAnnotation("with")));
	 assertTrue("cycle with chain", chain.contains(g.getAnnotation("its")));
	 assertTrue("cycle with chain", chain.contains(g.getAnnotation("own")));
	 assertTrue("cycle with chain", chain.contains(g.getAnnotation("sound")));
      }
      catch(java.lang.StackOverflowError exception)
      {
	 fail("cycle with chain not skipped");
      }

      try
      {
	 AnnotationChain chain = new AnnotationChain(g.getAnchor("a1"), 5.0, new HashSet<String>());
	 assertEquals("from offset", 6, chain.size());
	 assertTrue("from offset", chain.contains(g.getAnnotation("round")));
	 assertTrue("from offset", chain.contains(g.getAnnotation("and")));
	 assertTrue("from offset", chain.contains(g.getAnnotation("with")));
	 assertTrue("from offset", chain.contains(g.getAnnotation("its")));
	 assertTrue("from offset", chain.contains(g.getAnnotation("own")));
	 assertTrue("from offset", chain.contains(g.getAnnotation("sound")));
      }
      catch(java.lang.StackOverflowError exception)
      {
	 fail("cycle with chain not skipped");
      }

      try
      {
	 AnnotationChain chain = new AnnotationChain(1.0, g.getAnchor("a5"), new HashSet<String>());
	 assertEquals("to offset", 6, chain.size());
	 assertTrue("to offset", chain.contains(g.getAnnotation("round")));
	 assertTrue("to offset", chain.contains(g.getAnnotation("and")));
	 assertTrue("to offset", chain.contains(g.getAnnotation("with")));
	 assertTrue("to offset", chain.contains(g.getAnnotation("its")));
	 assertTrue("to offset", chain.contains(g.getAnnotation("own")));
	 assertTrue("to offset", chain.contains(g.getAnnotation("sound")));
      }
      catch(java.lang.StackOverflowError exception)
      {
	 fail("cycle with chain not skipped");
      }

   }

   @Test public void chainFromOffset() 
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
      g.addLayer(new Layer("pause", "Pauses", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   false, // saturated
			   "turn", // parentId
			   true)); // parentIncludes

      g.addAnchor(new Anchor("a0", 0.0)); // turn start
      g.addAnchor(new Anchor("a1", 1.0)); // the
      g.addAnchor(new Anchor("a2", 2.0)); // quick
      g.addAnchor(new Anchor("a2.5", 2.5)); // pause
      g.addAnchor(new Anchor("a3", 3.0)); // brown
      g.addAnchor(new Anchor("a4", 4.0)); // fox
      // unset offsets
      g.addAnchor(new Anchor("a?1", null)); // jumps
      g.addAnchor(new Anchor("a?2", null)); // over
      g.addAnchor(new Anchor("a5", 5.0)); // end of over
      g.addAnchor(new Anchor("a6", 6.0)); // turn end

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a6", "my graph"));

      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a6", "participant1"));

      g.addAnnotation(new Annotation("the", "the", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("quick", "quick", "word", "a2", "a2.5", "turn1"));
      g.addAnnotation(new Annotation("pause", "pause", "pause", "a2.5", "a3", "turn1"));
      g.addAnnotation(new Annotation("brown", "brown", "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("fox", "fox", "word", "a4", "a?1", "turn1"));
      g.addAnnotation(new Annotation("jumps", "jumps", "word", "a?1", "a?2", "turn1"));
      g.addAnnotation(new Annotation("over", "over", "word", "a?2", "a5", "turn1"));


      HashSet<String> excludeLayer = new HashSet<String>();

      AnnotationChain chain = new AnnotationChain(3.0, g.getAnchor("a5"), excludeLayer);
      assertEquals("chain on same layer", 4, chain.size());
      assertTrue("chain on same layer", chain.contains(g.getAnnotation("brown")));
      assertTrue("chain on same layer", chain.contains(g.getAnnotation("fox")));
      assertTrue("chain on same layer", chain.contains(g.getAnnotation("jumps")));
      assertTrue("chain on same layer", chain.contains(g.getAnnotation("over")));

      chain = new AnnotationChain(1.0, g.getAnchor("a3"), excludeLayer);
      assertEquals("chain across layers", 3, chain.size());
      assertTrue("chain across layers", chain.contains(g.getAnnotation("the")));
      assertTrue("chain across layers", chain.contains(g.getAnnotation("quick")));
      assertTrue("chain across layers", chain.contains(g.getAnnotation("pause")));

      chain = new AnnotationChain(1.0, g.getAnchor("a5"), excludeLayer);
      assertEquals("detour", 7, chain.size());
      assertTrue("detour", chain.contains(g.getAnnotation("the")));
      assertTrue("detour", chain.contains(g.getAnnotation("quick")));
      assertTrue("detour", chain.contains(g.getAnnotation("pause")));
      assertTrue("detour", chain.contains(g.getAnnotation("brown")));
      assertTrue("detour", chain.contains(g.getAnnotation("fox")));
      assertTrue("detour", chain.contains(g.getAnnotation("jumps")));
      assertTrue("detour", chain.contains(g.getAnnotation("over")));

      excludeLayer.add("pause");
      chain = new AnnotationChain(1.0, g.getAnchor("a5"), excludeLayer);
      // from after pause
      assertEquals("exclude layer", 4, chain.size());
      assertTrue("detour", chain.contains(g.getAnnotation("brown")));
      assertTrue("detour", chain.contains(g.getAnnotation("fox")));
      assertTrue("detour", chain.contains(g.getAnnotation("jumps")));
      assertTrue("detour", chain.contains(g.getAnnotation("over")));

      chain = new AnnotationChain(5.0, g.getAnchor("a3"), excludeLayer);
      assertEquals("no chain", 0, chain.size());

   }

   @Test public void chainToOffset() 
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
      g.addLayer(new Layer("pause", "Pauses", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   false, // saturated
			   "turn", // parentId
			   true)); // parentIncludes

      g.addAnchor(new Anchor("a0", 0.0)); // turn start
      g.addAnchor(new Anchor("a1", 1.0)); // the
      g.addAnchor(new Anchor("a2", 2.0)); // quick
      g.addAnchor(new Anchor("a2.5", 2.5)); // pause
      g.addAnchor(new Anchor("a3", 3.0)); // brown
      g.addAnchor(new Anchor("a4", 4.0)); // fox
      // unset offsets
      g.addAnchor(new Anchor("a?1", null)); // jumps
      g.addAnchor(new Anchor("a?2", null)); // over
      g.addAnchor(new Anchor("a5", 5.0)); // end of over
      g.addAnchor(new Anchor("a6", 6.0)); // turn end

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a6", "my graph"));

      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a6", "participant1"));

      g.addAnnotation(new Annotation("the", "the", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("quick", "quick", "word", "a2", "a2.5", "turn1"));
      g.addAnnotation(new Annotation("pause", "pause", "pause", "a2.5", "a3", "turn1"));
      g.addAnnotation(new Annotation("brown", "brown", "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("fox", "fox", "word", "a4", "a?1", "turn1"));
      g.addAnnotation(new Annotation("jumps", "jumps", "word", "a?1", "a?2", "turn1"));
      g.addAnnotation(new Annotation("over", "over", "word", "a?2", "a5", "turn1"));

      HashSet<String> excludeLayer = new HashSet<String>();

      AnnotationChain chain = new AnnotationChain(g.getAnchor("a3"), 5.0, excludeLayer);
      assertEquals("chain on same layer", 4, chain.size());
      assertTrue("chain on same layer", chain.contains(g.getAnnotation("brown")));
      assertTrue("chain on same layer", chain.contains(g.getAnnotation("fox")));
      assertTrue("chain on same layer", chain.contains(g.getAnnotation("jumps")));
      assertTrue("chain on same layer", chain.contains(g.getAnnotation("over")));

      chain = new AnnotationChain(g.getAnchor("a1"), 3.0, excludeLayer);
      assertEquals("chain across layers", 3, chain.size());
      assertTrue("chain across layers", chain.contains(g.getAnnotation("the")));
      assertTrue("chain across layers", chain.contains(g.getAnnotation("quick")));
      assertTrue("chain across layers", chain.contains(g.getAnnotation("pause")));

      chain = new AnnotationChain(g.getAnchor("a1"), 5.0, excludeLayer);
      assertEquals("detour", 7, chain.size());
      assertTrue("detour", chain.contains(g.getAnnotation("the")));
      assertTrue("detour", chain.contains(g.getAnnotation("quick")));
      assertTrue("detour", chain.contains(g.getAnnotation("pause")));
      assertTrue("detour", chain.contains(g.getAnnotation("brown")));
      assertTrue("detour", chain.contains(g.getAnnotation("fox")));
      assertTrue("detour", chain.contains(g.getAnnotation("jumps")));
      assertTrue("detour", chain.contains(g.getAnnotation("over")));

      excludeLayer.add("pause");
      chain = new AnnotationChain(g.getAnchor("a1"), 5.0, excludeLayer);
      // up to pause
      assertEquals("exclude layer", 2, chain.size());
      assertTrue("detour", chain.contains(g.getAnnotation("the")));
      assertTrue("detour", chain.contains(g.getAnnotation("quick")));

   }

   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.util.test.TestAnnotationChain");
   }
}
