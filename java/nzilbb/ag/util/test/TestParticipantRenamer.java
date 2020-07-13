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
import nzilbb.ag.*;
import nzilbb.ag.util.*;

public class TestParticipantRenamer
{
   @Test public void noChanges() 
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
      g.addAnnotation(new Annotation("participant2", "jane doe", "who", "turnStart", "turnEnd", "my graph"));
      
      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "utteranceChange", "participant1"));
      g.addAnnotation(new Annotation("turn2", "jane doe", "turn", "utteranceChange", "turnEnd", "participant2"));

      g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utteranceChange", "turn1"));
      g.addAnnotation(new Annotation("utterance2", "jane doe", "utterance", "utteranceChange", "turnEnd", "turn2"));
      
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

      // renaming a participant to the same name generates no changes
      ParticipantRenamer renamer = new ParticipantRenamer("jane doe", "jane doe");
      try
      {
         g.trackChanges();
	 renamer.transform(g);
         Set<Change> changes = g.getTracker().getChanges();
	 assertEquals("no changes: " + changes, 0, changes.size());
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void singleRename() 
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
      g.addAnnotation(new Annotation("participant2", "jane doe", "who", "turnStart", "turnEnd", "my graph"));
      
      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "utteranceChange", "participant1"));
      g.addAnnotation(new Annotation("turn2", "jane doe", "turn", "utteranceChange", "turnEnd", "participant2"));

      g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utteranceChange", "turn1"));
      g.addAnnotation(new Annotation("utterance2", "jane doe", "utterance", "utteranceChange", "turnEnd", "turn2"));
      
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

      // renaming a participant to the same name generates no changes
      ParticipantRenamer renamer = new ParticipantRenamer("john smith", "john doe");
      try
      {
         g.trackChanges();
	 renamer.transform(g);
         Set<Change> changes = g.getTracker().getChanges();
	 assertNotEquals("changes are returned", 0, changes.size());
	 assertNotEquals("name is changed", "john smith", g.getAnnotation("participant1").getLabel());
	 assertNotEquals("name is changed", "john smith", g.getAnnotation("turn1").getLabel());
	 assertNotEquals("name is changed", "john smith", g.getAnnotation("utterance1").getLabel());
	 assertEquals("new name correct", "john doe", g.getAnnotation("participant1").getLabel());
	 assertEquals("new name correct", "john doe", g.getAnnotation("turn1").getLabel());
	 assertEquals("new name correct", "john doe", g.getAnnotation("utterance1").getLabel());
	 assertEquals("others not changed", "jane doe", g.getAnnotation("participant2").getLabel());
	 assertEquals("others not changed", "jane doe", g.getAnnotation("turn2").getLabel());
	 assertEquals("others not changed", "jane doe", g.getAnnotation("utterance2").getLabel());
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void multiRename() 
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
      g.addAnnotation(new Annotation("participant2", "jane doe", "who", "turnStart", "turnEnd", "my graph"));
      
      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "utteranceChange", "participant1"));
      g.addAnnotation(new Annotation("turn2", "jane doe", "turn", "utteranceChange", "turnEnd", "participant2"));

      g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utteranceChange", "turn1"));
      g.addAnnotation(new Annotation("utterance2", "jane doe", "utterance", "utteranceChange", "turnEnd", "turn2"));
      
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

      // renaming a participant to the same name generates no changes
      ParticipantRenamer renamer = new ParticipantRenamer();
      renamer.rename("john smith", "john doe");
      renamer.rename("jane doe", "jane smith");
      try
      {
         g.trackChanges();
	 renamer.transform(g);
         Set<Change> changes = g.getTracker().getChanges();
	 assertNotEquals("changes are returned", 0, changes.size());
	 assertNotEquals("name is changed", "john smith", g.getAnnotation("participant1").getLabel());
	 assertNotEquals("name is changed", "john smith", g.getAnnotation("turn1").getLabel());
	 assertNotEquals("name is changed", "john smith", g.getAnnotation("utterance1").getLabel());
	 assertNotEquals("name is changed", "jane doe", g.getAnnotation("participant2").getLabel());
	 assertNotEquals("name is changed", "jane doe", g.getAnnotation("turn2").getLabel());
	 assertNotEquals("name is changed", "jane doe", g.getAnnotation("utterance2").getLabel());
	 assertEquals("new name correct", "john doe", g.getAnnotation("participant1").getLabel());
	 assertEquals("new name correct", "john doe", g.getAnnotation("turn1").getLabel());
	 assertEquals("new name correct", "john doe", g.getAnnotation("utterance1").getLabel());
	 assertEquals("new name correct", "jane smith", g.getAnnotation("participant2").getLabel());
	 assertEquals("new name correct", "jane smith", g.getAnnotation("turn2").getLabel());
	 assertEquals("new name correct", "jane smith", g.getAnnotation("utterance2").getLabel());
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.util.test.TestParticipantRenamer");
   }
}
