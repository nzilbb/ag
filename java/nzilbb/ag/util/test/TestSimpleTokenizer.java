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

public class TestSimpleTokenizer
{
   @Test public void basicTokenization() 
   {
      Graph g = new Graph();
      g.setId("my graph");

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
      g.addLayer(new Layer("utterance", "Utterances", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
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

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "my graph"));
      g.addAnnotation(new Annotation("participant2", "jane doe", "who", "my graph"));

      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a3", "participant1"));
      g.addAnnotation(new Annotation("turn2", "jane doe", "turn", "a2", "a3", "participant2"));

      g.addAnnotation(new Annotation("utterance1", "the quick brown fox", "utterance", "a0", "a1", "turn1"));
      g.addAnnotation(new Annotation("utterance2", "jumps over", "utterance", "a1", "a3", "turn1"));
      g.addAnnotation(new Annotation("utterance3", "the lazy dog", "utterance", "a2", "a3", "turn2"));

      try
      {
	 SimpleTokenizer tokenizer = new SimpleTokenizer("utterance", "word");
	 Vector<Change> changes = tokenizer.transform(g);
	 Annotation[] words = g.getAnnotation("turn1").list("word");
	 assertEquals(6, words.length);

	 assertEquals("first word shares start with utterance", "a0", words[0].getStartId());

	 assertEquals("the", words[0].getLabel());

	 assertNull("intermediate anchors have null offsets", words[0].getEnd().getOffset());
	 assertEquals("tokens chained together", words[0].getEndId(), words[1].getStartId());

	 assertEquals("quick", words[1].getLabel());
	 assertEquals("brown", words[2].getLabel());
	 assertEquals("fox", words[3].getLabel());
	 assertEquals("last word shares end with utterance", "a1", words[3].getEndId());
	 assertEquals("first word shares start with utterance", "a1", words[4].getStartId());
	 assertEquals("jumps", words[4].getLabel());
	 assertEquals("over", words[5].getLabel());
	 assertEquals("last word shares end with utterance", "a3", words[5].getEndId());

	 words = g.getAnnotation("turn2").list("word");
	 assertEquals(3, words.length);
	 assertEquals("first word shares start with utterance", "a2", words[0].getStartId());
	 assertEquals("the", words[0].getLabel());
	 assertEquals("lazy", words[1].getLabel());
	 assertEquals("dog", words[2].getLabel());
	 assertEquals("first word shares end with utterance", "a3", words[2].getEndId());
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void dirtyTranscription() 
   {
      Graph g = new Graph();
      g.setId("my graph");

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
      g.addLayer(new Layer("utterance", "Utterances", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
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

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "my graph"));
      g.addAnnotation(new Annotation("participant2", "jane doe", "who", "my graph"));

      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a3", "participant1"));
      g.addAnnotation(new Annotation("turn2", "jane doe", "turn", "a2", "a3", "participant2"));

      g.addAnnotation(new Annotation("utterance1", "\nthe\n quick brown fox\r\n", "utterance", "a0", "a1", "turn1"));
      g.addAnnotation(new Annotation("utterance2", "jumps  over   ", "utterance", "a1", "a3", "turn1"));
      g.addAnnotation(new Annotation("utterance3", "the\tlazy\tdog", "utterance", "a2", "a3", "turn2"));

      try
      {
	 SimpleTokenizer tokenizer = new SimpleTokenizer("utterance", "word");
	 Vector<Change> changes = tokenizer.transform(g);
	 Annotation[] words = g.getAnnotation("turn1").list("word");
	 assertEquals(6, words.length);

	 assertEquals("first word shares start with utterance", "a0", words[0].getStartId());

	 assertEquals("the", words[0].getLabel());

	 assertNull("intermediate anchors have null offsets", words[0].getEnd().getOffset());
	 assertEquals("tokens chained together", words[0].getEndId(), words[1].getStartId());

	 assertEquals("quick", words[1].getLabel());
	 assertEquals("brown", words[2].getLabel());
	 assertEquals("fox", words[3].getLabel());
	 assertEquals("last word shares end with utterance", "a1", words[3].getEndId());
	 assertEquals("first word shares start with utterance", "a1", words[4].getStartId());
	 assertEquals("jumps", words[4].getLabel());
	 assertEquals("over", words[5].getLabel());
	 assertEquals("last word shares end with utterance", "a3", words[5].getEndId());

	 words = g.getAnnotation("turn2").list("word");
	 assertEquals(3, words.length);
	 assertEquals("first word shares start with utterance", "a2", words[0].getStartId());
	 assertEquals("the", words[0].getLabel());
	 assertEquals("lazy", words[1].getLabel());
	 assertEquals("dog", words[2].getLabel());
	 assertEquals("first word shares end with utterance", "a3", words[2].getEndId());
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void alternativeDelimiters() 
   {
      Graph g = new Graph();
      g.setId("my graph");

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
      g.addLayer(new Layer("utterance", "Utterances", Constants.ALIGNMENT_INTERVAL,
			   true, // peers
			   false, // peersOverlap
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

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "my graph"));
      g.addAnnotation(new Annotation("participant2", "jane doe", "who", "my graph"));

      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a3", "participant1"));
      g.addAnnotation(new Annotation("turn2", "jane doe", "turn", "a2", "a3", "participant2"));

      g.addAnnotation(new Annotation("utterance1", "the|quick|brown|fox", "utterance", "a0", "a1", "turn1"));
      g.addAnnotation(new Annotation("utterance2", "jumps|over", "utterance", "a1", "a3", "turn1"));
      g.addAnnotation(new Annotation("utterance3", "the|lazy|dog", "utterance", "a2", "a3", "turn2"));

      try
      {
	 SimpleTokenizer tokenizer = new SimpleTokenizer("utterance", "word", "\\|");
	 Vector<Change> changes = tokenizer.transform(g);
	 Annotation[] words = g.getAnnotation("turn1").list("word");
	 assertEquals(6, words.length);

	 assertEquals("first word shares start with utterance", "a0", words[0].getStartId());

	 assertEquals("the", words[0].getLabel());

	 assertNull("intermediate anchors have null offsets", words[0].getEnd().getOffset());
	 assertEquals("tokens chained together", words[0].getEndId(), words[1].getStartId());

	 assertEquals("quick", words[1].getLabel());
	 assertEquals("brown", words[2].getLabel());
	 assertEquals("fox", words[3].getLabel());
	 assertEquals("last word shares end with utterance", "a1", words[3].getEndId());
	 assertEquals("first word shares start with utterance", "a1", words[4].getStartId());
	 assertEquals("jumps", words[4].getLabel());
	 assertEquals("over", words[5].getLabel());
	 assertEquals("last word shares end with utterance", "a3", words[5].getEndId());

	 words = g.getAnnotation("turn2").list("word");
	 assertEquals(3, words.length);
	 assertEquals("first word shares start with utterance", "a2", words[0].getStartId());
	 assertEquals("the", words[0].getLabel());
	 assertEquals("lazy", words[1].getLabel());
	 assertEquals("dog", words[2].getLabel());
	 assertEquals("first word shares end with utterance", "a3", words[2].getEndId());
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.util.test.TestSimpleTokenizer");
   }
}
