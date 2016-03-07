//
// Copyright 2016 New Zealand Institute of Language, Brain and Behaviour, 
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

public class TestNormalizer
{
   @Test public void alreadyNormalized() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      g.setCorpus("cc");      
      Layer[] layers = {
	 new Layer("who", "Participants", Constants.ALIGNMENT_NONE, 
		   true, // peers
		   true, // peersOverlap
		   true), // saturated
	 new Layer("turn", "Speaker turns", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   false, // saturated
		   "who", // parentId
		   true), // parentIncludes
	 new Layer("utterance", "Utterance", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   true, // saturated
		   "turn", // parentId
		   true), // parentIncludes
	 new Layer("word", "Words", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   false, // saturated
		   "turn", // parentId
		   true), // parentIncludes
	 new Layer("pos", "POS", Constants.ALIGNMENT_NONE,
		   true, // peers
		   true, // peersOverlap
		   true, // saturated
		   "word", // parentId
		   true)}; // parentIncludes
      g.setSchema(new Schema(layers, "who", "turn", "utterance", "word"));

      g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL)); // turn start

      g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // the
      g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // quick
      g.addAnchor(new Anchor("a02", 0.03, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // brown
      g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // fox
      g.addAnchor(new Anchor("a04a", 0.04, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // fox end

      g.addAnchor(new Anchor("utteranceChange", 0.4, Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL)); // utterance boundary

      g.addAnchor(new Anchor("a04b", 2.0, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // jumps
      g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // over
      g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // a
      g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // lazy
      g.addAnchor(new Anchor("a44", 5.1, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // dog
      g.addAnchor(new Anchor("a54", null)); // end of dog

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

      g.getAnnotation("jumps").setLabel("test");
      g.createTag(g.getAnnotation("jumps"), "pos", "V");

      Normalizer n = new Normalizer();
      try
      {
	 Vector<Change> changes = n.transform(g);

	 assertEquals("no changes: " + changes, 0, changes.size());
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void joinTurns() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      g.setCorpus("cc");      
      Layer[] layers = {
	 new Layer("who", "Participants", Constants.ALIGNMENT_NONE, 
		   true, // peers
		   true, // peersOverlap
		   true), // saturated
	 new Layer("turn", "Speaker turns", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   false, // saturated
		   "who", // parentId
		   true), // parentIncludes
	 new Layer("utterance", "Utterance", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   true, // saturated
		   "turn", // parentId
		   true), // parentIncludes
	 new Layer("word", "Words", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   false, // saturated
		   "turn", // parentId
		   true), // parentIncludes
	 new Layer("pos", "POS", Constants.ALIGNMENT_NONE,
		   true, // peers
		   true, // peersOverlap
		   true, // saturated
		   "word", // parentId
		   true)}; // parentIncludes
      g.setSchema(new Schema(layers, "who", "turn", "utterance", "word"));

      g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL)); // turn start

      g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // the
      g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // quick
      g.addAnchor(new Anchor("a02", 0.03, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // brown
      g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // fox
      g.addAnchor(new Anchor("a04a", 0.04, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // fox end

      g.addAnchor(new Anchor("utteranceChange", 0.4, Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL)); // utterance boundary

      g.addAnchor(new Anchor("a04b", 2.0, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // jumps
      g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // over
      g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // a
      g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // lazy
      g.addAnchor(new Anchor("a44", 5.1, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // dog
      g.addAnchor(new Anchor("a54", null)); // end of dog

      g.addAnchor(new Anchor("turnEnd", 5.4, Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL)); // turn end

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

      Normalizer n = new Normalizer();
      try
      {
	 Vector<Change> changes = n.transform(g);

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
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void disconnectFromTurnsUtterances() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      g.setCorpus("cc");      
      Layer[] layers = {
	 new Layer("who", "Participants", Constants.ALIGNMENT_NONE, 
		   true, // peers
		   true, // peersOverlap
		   true), // saturated
	 new Layer("turn", "Speaker turns", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   false, // saturated
		   "who", // parentId
		   true), // parentIncludes
	 new Layer("utterance", "Utterance", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   true, // saturated
		   "turn", // parentId
		   true), // parentIncludes
	 new Layer("word", "Words", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   false, // saturated
		   "turn", // parentId
		   true), // parentIncludes
	 new Layer("pos", "POS", Constants.ALIGNMENT_NONE,
		   true, // peers
		   true, // peersOverlap
		   true, // saturated
		   "word", // parentId
		   true)}; // parentIncludes
      g.setSchema(new Schema(layers, "who", "turn", "utterance", "word"));

      g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL)); // turn start

      g.addAnchor(new Anchor("a0", 0.01, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // the
      g.addAnchor(new Anchor("a01", 0.02, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // quick
      g.addAnchor(new Anchor("a02", 0.03, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // brown
      g.addAnchor(new Anchor("a03", 0.04, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // fox
      g.addAnchor(new Anchor("a04a", 0.04, Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT)); // fox end

      g.addAnchor(new Anchor("utteranceChange", 0.4, Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL)); // utterance boundary

      g.addAnchor(new Anchor("a04b", 2.0, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // jumps
      g.addAnchor(new Anchor("a14", 3.3, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // over
      g.addAnchor(new Anchor("a24", 4.4, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // a
      g.addAnchor(new Anchor("a34", 5.0, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // lazy
      g.addAnchor(new Anchor("a44", 5.1, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC)); // dog
      g.addAnchor(new Anchor("a54", null)); // end of dog

      g.addAnchor(new Anchor("turnEnd", 5.4, Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL)); // turn end

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
      
      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "participant1"));

      g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utteranceChange", "turn1"));
      g.addAnnotation(new Annotation("utterance2", "john smith", "utterance", "utteranceChange", "turnEnd", "turn2"));
      
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

      Normalizer n = new Normalizer();
      try
      {
	 Vector<Change> changes = n.transform(g);

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

	 assertEquals("same offset", new Double(0.0), 
		      g.getAnnotation("the").getStart().getOffset());
	 assertEquals("same offset", new Double(0.4), 
		      g.getAnnotation("fox").getEnd().getOffset());
	 assertEquals("same offset", new Double(0.4), 
		      g.getAnnotation("jumps").getStart().getOffset());
	 assertEquals("same offset", new Double(5.4), 
		      g.getAnnotation("dog").getEnd().getOffset());

	 assertEquals("low confidence", Constants.CONFIDENCE_DEFAULT, 
		      g.getAnnotation("the").getStart().get(Constants.CONFIDENCE));
	 assertEquals("same offset", Constants.CONFIDENCE_DEFAULT, 
		      g.getAnnotation("fox").getEnd().get(Constants.CONFIDENCE));
	 assertEquals("same offset", Constants.CONFIDENCE_DEFAULT, 
		      g.getAnnotation("jumps").getStart().get(Constants.CONFIDENCE));
	 assertEquals("same offset", Constants.CONFIDENCE_DEFAULT, 
		      g.getAnnotation("dog").getEnd().get(Constants.CONFIDENCE));

      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.util.test.TestNormalizer");
   }
}
