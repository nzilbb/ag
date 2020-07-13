//
// Copyright 2016-2020 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import nzilbb.ag.*;
import nzilbb.ag.util.*;

public class TestSpanningConventionTransformer
{
   @Test public void basicConvert() 
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
      g.addLayer(new Layer("span", "Spans", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
			   false, // saturated
			   "turn", // parentId
			   true)); // parentIncludes
      g.addLayer(new Layer("comment", "Comment", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   true, // peersOverlap
			   false)); // saturated

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

      g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("word2", "{quick", "word", "a2", "a3", "turn1"));
      g.addAnnotation(new Annotation("word3", "brown", "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("word4", "fox}", "word", "a4", "a?1", "turn1"));
      g.addAnnotation(new Annotation("word5", "jumps", "word", "a?1", "a?2", "turn1"));
      g.addAnnotation(new Annotation("word6", "over", "word", "a?2", "a5", "turn1"));

      // span for word1, to test its anchor moves with word1's
      g.addAnnotation(new Annotation("span", "span", "span", "a1", "a2", "turn1"));

      try
      {
         g.setTracker(new ChangeTracker());
	 SpanningConventionTransformer transformer = new SpanningConventionTransformer(
	    "word", "\\{(.*)", "(.*)\\}", true, null, null, "comment", "$1", "$1", false, true);
	 transformer.transform(g);
	 assertEquals("the", g.getAnnotation("word1").getLabel());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("word2").getChange());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("word3").getChange());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("word4").getChange());
	 assertEquals("jumps", g.getAnnotation("word5").getLabel());
	 assertEquals("ordinal corrected", 2, g.getAnnotation("word5").getOrdinal());
	 assertEquals("over", g.getAnnotation("word6").getLabel());
	 assertEquals("ordinal corrected", 3, g.getAnnotation("word6").getOrdinal());
	 assertEquals("chained across gap", g.getAnnotation("word1").getEnd(), g.getAnnotation("word5").getStart());

	 Annotation span = g.list("comment")[0];
	 assertEquals("quick brown fox", span.getLabel());
	 assertEquals("a2", span.getStartId());
	 assertEquals("a?1", span.getEndId());
	 assertEquals("parent set", "my graph", span.getParentId());

	 span = g.list("span")[0];
	 assertEquals("a1", span.getStartId());
	 assertEquals("chained across gap with word", g.getAnnotation("word1").getEnd(), g.getAnnotation("span").getEnd());
	 assertEquals("a?1", span.getEndId());
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void basicConvertLeaveGaps() 
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
      g.addLayer(new Layer("comment", "Comment", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   true, // peersOverlap
			   false)); // saturated

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

      g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("word2", "{quick", "word", "a2", "a3", "turn1"));
      g.addAnnotation(new Annotation("word3", "brown", "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("word4", "fox}", "word", "a4", "a?1", "turn1"));
      g.addAnnotation(new Annotation("word5", "jumps", "word", "a?1", "a?2", "turn1"));
      g.addAnnotation(new Annotation("word6", "over", "word", "a?2", "a5", "turn1"));

      try
      {
         g.setTracker(new ChangeTracker());
	 SpanningConventionTransformer transformer = new SpanningConventionTransformer(
	    "word", "\\{(.*)", "(.*)\\}", true, null, null, "comment", "$1", "$1", false, 
	    // this time don't close gaps:
	    false);
	 transformer.transform(g);
	 assertEquals("the", g.getAnnotation("word1").getLabel());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("word2").getChange());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("word3").getChange());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("word4").getChange());
	 assertEquals("jumps", g.getAnnotation("word5").getLabel());
	 assertEquals("ordinal corrected", 2, g.getAnnotation("word5").getOrdinal());
	 assertEquals("over", g.getAnnotation("word6").getLabel());
	 assertEquals("ordinal corrected", 3, g.getAnnotation("word6").getOrdinal());
	 assertEquals("not chained across gap", "a2", g.getAnnotation("word1").getEndId());
	 assertEquals("not chained across gap", "a?1", g.getAnnotation("word5").getStartId());

	 Annotation span = g.list("comment")[0];
	 assertEquals("quick brown fox", span.getLabel());
	 assertEquals("a2", span.getStartId());
	 assertEquals("a?1", span.getEndId());
	 assertEquals("parent set", "my graph", span.getParentId());
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void basicConvertShortCases() 
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
      g.addLayer(new Layer("comment", "Comment", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   true, // peersOverlap
			   false)); // saturated

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

      g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("word2", "{quick}", "word", "a2", "a3", "turn1"));
      g.addAnnotation(new Annotation("word3", "brown", "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("word4", "{fox", "word", "a4", "a?1", "turn1"));
      g.addAnnotation(new Annotation("word5", "jumps}", "word", "a?1", "a?2", "turn1"));
      g.addAnnotation(new Annotation("word6", "over", "word", "a?2", "a5", "turn1"));

      try
      {
         g.setTracker(new ChangeTracker());
	 SpanningConventionTransformer transformer = new SpanningConventionTransformer(
	    "word", "\\{(.*)", "(.*)\\}", true, null, null, "comment", "$1", "$1", false, true);
	 transformer.transform(g);
	 assertEquals("the", g.getAnnotation("word1").getLabel());
	 assertEquals("start = end", Change.Operation.Destroy, g.getAnnotation("word2").getChange());
	 assertEquals("chained across gap", g.getAnnotation("word1").getEnd(), g.getAnnotation("word3").getStart());
	 assertEquals("brown", g.getAnnotation("word3").getLabel());
	 assertEquals("ordinal corrected", 2, g.getAnnotation("word3").getOrdinal());
	 assertEquals("none between", Change.Operation.Destroy, g.getAnnotation("word4").getChange());
	 assertEquals("none between", Change.Operation.Destroy, g.getAnnotation("word5").getChange());
	 assertEquals("over", g.getAnnotation("word6").getLabel());
	 assertEquals("ordinal corrected", 3, g.getAnnotation("word6").getOrdinal());
	 assertEquals("chained across gap", g.getAnnotation("word3").getEnd(), g.getAnnotation("word6").getStart());

	 Annotation[] comments = g.list("comment");
	 Annotation span = comments[0];
	 assertEquals("quick", span.getLabel());
	 assertEquals("a2", span.getStartId());
	 assertEquals("a3", span.getEndId());
	 assertEquals("parent set", "my graph", span.getParentId());

	 span = comments[1];
	 assertEquals("fox jumps", span.getLabel());
	 assertEquals("a4", span.getStartId());
	 assertEquals("a?2", span.getEndId());
	 assertEquals("parent set", "my graph", span.getParentId());

      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void emptyResults() 
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
      g.addLayer(new Layer("comment", "Comment", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   true, // peersOverlap
			   false)); // saturated

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

      g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("word2", "{", "word", "a2", "a3", "turn1"));
      g.addAnnotation(new Annotation("word3", "brown", "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("word4", "", "word", "a4", "a?1", "turn1"));
      g.addAnnotation(new Annotation("word5", "}", "word", "a?1", "a?2", "turn1"));
      g.addAnnotation(new Annotation("word6", "over", "word", "a?2", "a5", "turn1"));

      try
      {
         g.setTracker(new ChangeTracker());
	 SpanningConventionTransformer transformer = new SpanningConventionTransformer(
	    "word", "\\{(.*)", "(.*)\\}", true, null, null, "comment", "$1", "$1");
	 transformer.transform(g);
	 assertEquals("the", g.getAnnotation("word1").getLabel());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("word2").getChange());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("word3").getChange());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("word4").getChange());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("word5").getChange());
	 assertEquals("over", g.getAnnotation("word6").getLabel());
	 assertEquals("chained across gap", g.getAnnotation("word1").getEnd(), g.getAnnotation("word6").getStart());

	 Annotation span = g.list("comment")[0];
	 assertEquals("empty results don't result in delimiter being added", "brown", span.getLabel());
	 assertEquals("a2", span.getStartId());
	 assertEquals("a?2", span.getEndId());
	 assertEquals("parent set", "my graph", span.getParentId());
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void basicConvertPartitioned() 
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
      g.addLayer(new Layer("comment", "Comment", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   true, // peersOverlap
			   false)); // saturated

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
      g.addAnnotation(new Annotation("participant2", "jane doe", "who", "a0", "a6", "my graph"));

      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a4", "participant1"));
      g.addAnnotation(new Annotation("turn2", "jane doe", "turn", "a4", "a6", "participant1"));

      g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("word2", "{quick", "word", "a2", "a3", "turn1"));
      g.addAnnotation(new Annotation("word3", "brown", "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("word4", "fox}", "word", "a4", "a?1", "turn2"));
      g.addAnnotation(new Annotation("word5", "jumps", "word", "a?1", "a?2", "turn2"));
      g.addAnnotation(new Annotation("word6", "over", "word", "a?2", "a5", "turn2"));

      try
      {
         g.trackChanges();
	 SpanningConventionTransformer transformer = new SpanningConventionTransformer(
	    "word", "\\{(.*)", "(.*)\\}", true, null, null, "comment", "$1", "$1");
	 transformer.transform(g);
         Set<Change> changes = g.getTracker().getChanges();
	 // there should be no changes
	 for (Change change : changes) System.out.println(change.toString());
	 assertEquals("the", g.getAnnotation("word1").getLabel());
	 assertEquals("Different turns", "{quick", g.getAnnotation("word2").getLabel());
	 assertEquals("brown", g.getAnnotation("word3").getLabel());
	 assertEquals("Different turns", "fox}", g.getAnnotation("word4").getLabel());
	 assertEquals("jumps", g.getAnnotation("word5").getLabel());
	 assertEquals("over", g.getAnnotation("word6").getLabel());

	 assertEquals("Different turns", 0, changes.size());
	 assertEquals("Different turns", 0, g.list("comment").length);

      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void basicExtract() 
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
      g.addLayer(new Layer("phrase", "Phrases", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   true, // peersOverlap
			   false, // saturated
			   "turn", // parentId
			   true)); // parentIncludes

      g.addAnchor(new Anchor("a0", 0.0)); // turn start
      g.addAnchor(new Anchor("a1", 1.0)); // the
      g.addAnchor(new Anchor("a2", 2.0)); // quick
      g.addAnchor(new Anchor("a3", 3.0)); // brown
      g.addAnchor(new Anchor("a4", 4.0)); // fox
      // unset offsets
      g.addAnchor(new Anchor("a?1", null)); // VP
      g.addAnchor(new Anchor("a?", null)); // jumps
      g.addAnchor(new Anchor("a?2", null)); // over
      g.addAnchor(new Anchor("a5", 5.0)); // end of over
      g.addAnchor(new Anchor("a6", 6.0)); // turn end

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a6", "my graph"));

      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a6", "participant1"));

      g.addAnnotation(new Annotation("NP", "[NP", "word", "a0", "a1", "turn1"));
      g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("word2", "quick", "word", "a2", "a3", "turn1"));
      g.addAnnotation(new Annotation("word3", "brown", "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("word4", "fox]", "word", "a4", "a?1", "turn1"));
      g.addAnnotation(new Annotation("VP", "[VP", "word", "a?1", "a?", "turn1"));
      g.addAnnotation(new Annotation("word5", "jumps", "word", "a?", "a?2", "turn1"));
      g.addAnnotation(new Annotation("word6", "over]", "word", "a?2", "a5", "turn1"));

      try
      {
         g.trackChanges();
	 SpanningConventionTransformer transformer = new SpanningConventionTransformer(
	    "word", "\\[(.*)", "(.*)\\]", false, null, "$1", "phrase", "$1", null);
	 transformer.transform(g);
	 assertEquals("the", g.getAnnotation("word1").getLabel());
	 assertEquals("ordinal corrected", 1, g.getAnnotation("word1").getOrdinal());
	 assertEquals("quick", g.getAnnotation("word2").getLabel());
	 assertEquals("ordinal corrected", 2, g.getAnnotation("word2").getOrdinal());
	 assertEquals("brown", g.getAnnotation("word3").getLabel());
	 assertEquals("ordinal corrected", 3, g.getAnnotation("word3").getOrdinal());
	 assertEquals("fox", g.getAnnotation("word4").getLabel());
	 assertEquals("ordinal corrected", 4, g.getAnnotation("word4").getOrdinal());
	 assertEquals("jumps", g.getAnnotation("word5").getLabel());
	 assertEquals("ordinal corrected", 5, g.getAnnotation("word5").getOrdinal());
	 assertEquals("over", g.getAnnotation("word6").getLabel());
	 assertEquals("ordinal corrected", 6, g.getAnnotation("word6").getOrdinal());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("NP").getChange());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("VP").getChange());

	 Annotation[] spans = g.list("phrase");
	 Annotation span = spans[0];
	 assertEquals("NP", span.getLabel());
	 assertEquals("a0", span.getStartId());
	 assertEquals("a?1", span.getEndId());
	 assertEquals("parent set", "turn1", span.getParentId());

	 span = spans[1];
	 assertEquals("VP", span.getLabel());
	 assertEquals("a?1", span.getStartId());
	 assertEquals("a5", span.getEndId());
	 assertEquals("parent set", "turn1", span.getParentId());

      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void annotatePrevious() 
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
      g.addLayer(new Layer("expansion", "CLAN Expansion", Constants.ALIGNMENT_NONE,
			   false, // peers
			   true, // peersOverlap
			   true, // saturated
			   "word", // parentId
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

      g.addAnnotation(new Annotation("word1", "gonna", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("word2", "[:", "word", "a2", "a3", "turn1"));
      g.addAnnotation(new Annotation("word3", "going", "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("word4", "to]", "word", "a4", "a?1", "turn1"));
      g.addAnnotation(new Annotation("word5", "jump", "word", "a?1", "a?2", "turn1"));
      g.addAnnotation(new Annotation("word6", "over", "word", "a?2", "a5", "turn1"));

      try
      {
         g.trackChanges();
	 SpanningConventionTransformer transformer = new SpanningConventionTransformer(
	    "word", "\\[:", "(.*)\\]", true, null, null, "expansion", null, "$1", true, true);
	 transformer.transform(g);
	 assertEquals("gonna", g.getAnnotation("word1").getLabel());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("word2").getChange());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("word3").getChange());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("word4").getChange());
	 assertEquals("jump", g.getAnnotation("word5").getLabel());
	 assertEquals("over", g.getAnnotation("word6").getLabel());
	 assertEquals("corrected ordinal", 2, g.getAnnotation("word5").getOrdinal());
	 assertEquals("corrected ordinal", 3, g.getAnnotation("word6").getOrdinal());
	 assertEquals("chained across gap", g.getAnnotation("word1").getEnd(), g.getAnnotation("word5").getStart());

	 Annotation span = g.list("expansion")[0];
	 assertEquals("going to", span.getLabel());
	 assertEquals("shares start with previous", g.getAnnotation("word1").getStart(), span.getStart());
	 assertEquals("shares start with previous", "a1", span.getStartId());
	 assertEquals("shares end with previous", g.getAnnotation("word1").getEnd(), span.getEnd());
	 assertEquals("shares end with previous", "a?1", span.getEndId());
	 assertEquals("parent set", "word1", span.getParentId());

	 Annotation word2 = g.getAnnotation("word2");
	 Annotation word3 = g.getAnnotation("word3");
	 Annotation word4 = g.getAnnotation("word4");
	 assertNotNull("commit will remove annotations", g.getAnnotation("word2"));
	 assertNotNull("commit will remove annotations", g.getAnnotation("word3"));
	 assertNotNull("commit will remove annotations", g.getAnnotation("word4"));
	 g.commit();
	 assertEquals("gonna", g.getAnnotation("word1").getLabel());
	 assertNull("commit removes annotation", g.getAnnotation("word2"));
	 assertNull("commit removes annotation", g.getAnnotation("word3"));
	 assertNull("commit removes annotation", g.getAnnotation("word4"));
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void annotatePreviousEvenWithTrailingSuffix() 
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
      g.addLayer(new Layer("expansion", "CLAN Expansion", Constants.ALIGNMENT_NONE,
			   false, // peers
			   true, // peersOverlap
			   true, // saturated
			   "word", // parentId
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

      g.addAnnotation(new Annotation("word1", "gonna", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("word2", "[:", "word", "a2", "a3", "turn1"));
      g.addAnnotation(new Annotation("word3", "going", "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("word4", "to]...", "word", "a4", "a?1", "turn1"));
      g.addAnnotation(new Annotation("word5", "jump", "word", "a?1", "a?2", "turn1"));
      g.addAnnotation(new Annotation("word6", "over", "word", "a?2", "a5", "turn1"));

      try
      {
	 // transformer allows suffix after closing ]
         g.trackChanges();
	 SpanningConventionTransformer transformer = new SpanningConventionTransformer(
	    "word", "\\[:", "(.*)\\](.*)", true, null, "$2", "expansion", null, "$1", true, true);
	 transformer.transform(g);
	 assertEquals("gonna", g.getAnnotation("word1").getLabel());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("word2").getChange());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("word3").getChange());
	 assertEquals("...", g.getAnnotation("word4").getLabel());
	 assertEquals("jump", g.getAnnotation("word5").getLabel());
	 assertEquals("over", g.getAnnotation("word6").getLabel());
	 assertEquals("corrected ordinal", 3, g.getAnnotation("word5").getOrdinal());
	 assertEquals("corrected ordinal", 4, g.getAnnotation("word6").getOrdinal());
	 assertEquals("chained across gap", g.getAnnotation("word1").getEnd(), g.getAnnotation("word4").getStart());

	 Annotation span = g.list("expansion")[0];
	 assertEquals("going to", span.getLabel());
	 assertEquals("shares start with previous", g.getAnnotation("word1").getStart(), span.getStart());
	 assertEquals("shares start with previous", "a1", span.getStartId());
	 assertEquals("shares end with previous", g.getAnnotation("word1").getEnd(), span.getEnd());
	 assertEquals("shares end with previous", "a4", span.getEndId());
	 assertEquals("parent set", "word1", span.getParentId());

	 Annotation word2 = g.getAnnotation("word2");
	 Annotation word3 = g.getAnnotation("word3");
	 Annotation word4 = g.getAnnotation("word4");
	 assertNotNull("commit will remove annotations", word2);
	 assertNotNull("commit will remove annotations", word3);
	 assertNotNull("commit will not remove all annotations", g.getAnnotation("word4"));
	 g.commit();
	 assertEquals("gonna", g.getAnnotation("word1").getLabel());
	 assertNull("commit removes annotation", g.getAnnotation("word2"));
	 assertNull("commit removes annotation", g.getAnnotation("word3"));
	 assertNotNull("commit doesn't remove suffixed annotation", g.getAnnotation("word4"));
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void annotatePreviousWithNoTrailingSuffix() 
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
      g.addLayer(new Layer("expansion", "CLAN Expansion", Constants.ALIGNMENT_NONE,
			   false, // peers
			   true, // peersOverlap
			   true, // saturated
			   "word", // parentId
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

      g.addAnnotation(new Annotation("word1", "gonna", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("word2", "[:", "word", "a2", "a3", "turn1"));
      g.addAnnotation(new Annotation("word3", "going", "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("word4", "to]", "word", "a4", "a?1", "turn1"));
      g.addAnnotation(new Annotation("word5", "jump", "word", "a?1", "a?2", "turn1"));
      g.addAnnotation(new Annotation("word6", "over", "word", "a?2", "a5", "turn1"));

      try
      {
	 // transformer allows suffix after closing ]
         g.trackChanges();
	 SpanningConventionTransformer transformer = new SpanningConventionTransformer(
	    "word", "\\[:", "(.*)\\](.*)", true, null, "$2", "expansion", null, "$1", true, true);
	 transformer.transform(g);
	 assertEquals("gonna", g.getAnnotation("word1").getLabel());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("word2").getChange());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("word3").getChange());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("word4").getChange());
	 assertEquals("jump", g.getAnnotation("word5").getLabel());
	 assertEquals("over", g.getAnnotation("word6").getLabel());
	 assertEquals("corrected ordinal", 2, g.getAnnotation("word5").getOrdinal());
	 assertEquals("corrected ordinal", 3, g.getAnnotation("word6").getOrdinal());
	 assertEquals("chained across gap", g.getAnnotation("word1").getEnd(), g.getAnnotation("word5").getStart());

	 Annotation span = g.list("expansion")[0];
	 assertEquals("going to", span.getLabel());
	 assertEquals("shares start with previous", g.getAnnotation("word1").getStart(), span.getStart());
	 assertEquals("shares start with previous", "a1", span.getStartId());
	 assertEquals("shares end with previous", g.getAnnotation("word1").getEnd(), span.getEnd());
	 assertEquals("shares end with previous", "a?1", span.getEndId());
	 assertEquals("parent set", "word1", span.getParentId());

	 Annotation word2 = g.getAnnotation("word2");
	 Annotation word3 = g.getAnnotation("word3");
	 Annotation word4 = g.getAnnotation("word4");
	 assertNotNull("commit will remove annotations", g.getAnnotation("word2"));
	 assertNotNull("commit will remove annotations", g.getAnnotation("word3"));
	 assertNotNull("commit will remove annotations", g.getAnnotation("word4"));
	 g.commit();
	 assertEquals("gonna", g.getAnnotation("word1").getLabel());
	 assertNull("commit removes annotation", g.getAnnotation("word2"));
	 assertNull("commit removes annotation", g.getAnnotation("word3"));
	 assertNull("commit removes annotation", g.getAnnotation("word4"));
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void dontAnnotatePrevious() 
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
      g.addLayer(new Layer("span", "CLAN Span", Constants.ALIGNMENT_INTERVAL,
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

      g.addAnnotation(new Annotation("word1", "we", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("word2", "<are", "word", "a2", "a3", "turn1"));
      g.addAnnotation(new Annotation("word3", "going", "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("word4", "to>", "word", "a4", "a?1", "turn1"));
      g.addAnnotation(new Annotation("word5", "jump", "word", "a?1", "a?2", "turn1"));
      g.addAnnotation(new Annotation("word6", "over", "word", "a?2", "a5", "turn1"));

      try
      {
         g.trackChanges();
	 SpanningConventionTransformer transformer = new SpanningConventionTransformer(
	    "word", "<(.*)", "(.*)>(.*)", false, "$1", "$1$2", "span", "-", "$1", "$1", false);
	 transformer.transform(g);
	 assertEquals("we", g.getAnnotation("word1").getLabel());
	 assertEquals("are", g.getAnnotation("word2").getLabel());
	 assertEquals("going", g.getAnnotation("word3").getLabel());
	 assertEquals("to", g.getAnnotation("word4").getLabel());
	 assertEquals("jump", g.getAnnotation("word5").getLabel());
	 assertEquals("over", g.getAnnotation("word6").getLabel());

	 assertEquals("correct ordinal", 1, g.getAnnotation("word1").getOrdinal());
	 assertEquals("correct ordinal", 2, g.getAnnotation("word2").getOrdinal());
	 assertEquals("correct ordinal", 3, g.getAnnotation("word3").getOrdinal());
	 assertEquals("correct ordinal", 4, g.getAnnotation("word4").getOrdinal());
	 assertEquals("correct ordinal", 5, g.getAnnotation("word5").getOrdinal());
	 assertEquals("correct ordinal", 6, g.getAnnotation("word6").getOrdinal());

	 Annotation span = g.list("span")[0];
	 assertEquals("are-to", span.getLabel());
	 assertEquals("shares start with first", g.getAnnotation("word2").getStart(), span.getStart());
	 assertEquals("shares start with first", "a2", span.getStartId());
	 assertEquals("shares end with last", g.getAnnotation("word4").getEnd(), span.getEnd());
	 assertEquals("shares end with last", "a?1", span.getEndId());
	 assertEquals("parent set", "turn1", span.getParentId());

      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void noDestination() 
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

      g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("word2", "{quick", "word", "a2", "a3", "turn1"));
      g.addAnnotation(new Annotation("word3", "brown", "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("word4", "fox}", "word", "a4", "a?1", "turn1"));
      g.addAnnotation(new Annotation("word5", "jumps", "word", "a?1", "a?2", "turn1"));
      g.addAnnotation(new Annotation("word6", "over", "word", "a?2", "a5", "turn1"));

      try
      {
	 // delete span by setting destination layer to null
         g.trackChanges();
	 SpanningConventionTransformer transformer = new SpanningConventionTransformer(
	    "word", "\\{(.*)", "(.*)\\}", true, null, null, null, null, null);
	 transformer.transform(g);
	 assertEquals("the", g.getAnnotation("word1").getLabel());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("word2").getChange());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("word3").getChange());
	 assertEquals(Change.Operation.Destroy, g.getAnnotation("word4").getChange());
	 assertEquals("jumps", g.getAnnotation("word5").getLabel());
	 assertEquals("over", g.getAnnotation("word6").getLabel());
	 assertEquals("chained across gap", g.getAnnotation("word1").getEnd(), g.getAnnotation("word5").getStart());

      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }


   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.util.test.TestSpanningConventionTransformer");
   }
}
