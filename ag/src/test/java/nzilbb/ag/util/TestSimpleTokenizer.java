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

package nzilbb.ag.util;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;
import nzilbb.ag.*;
import nzilbb.ag.util.*;

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

      g.addAnchor(new Anchor("a0", 0.0, Constants.CONFIDENCE_MANUAL));
      g.addAnchor(new Anchor("a1", 1.0, Constants.CONFIDENCE_MANUAL));
      g.addAnchor(new Anchor("a2", 2.0, Constants.CONFIDENCE_MANUAL));
      g.addAnchor(new Anchor("a3", 3.0, Constants.CONFIDENCE_MANUAL));

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "my graph"));
      g.addAnnotation(new Annotation("participant2", "jane doe", "who", "my graph"));

      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a3", "participant1"));
      g.addAnnotation(new Annotation("turn2", "jane doe", "turn", "a2", "a3", "participant2"));

      Annotation utterance = new Annotation("utterance1", "the quick brown fox", "utterance",
                                            "a0", "a1", "turn1", 1, Constants.CONFIDENCE_MANUAL);
      utterance.setAnnotator("annotator");
      g.addAnnotation(utterance);
      utterance = new Annotation("utterance2", "jumps over", "utterance", "a1", "a3", "turn1",
                                 2, Constants.CONFIDENCE_MANUAL);
      utterance.setAnnotator("annotator");
      g.addAnnotation(utterance);
      utterance = new Annotation("utterance3", "the lazy dog", "utterance",
                                 "a2", "a3", "turn2", 1, Constants.CONFIDENCE_MANUAL);
      utterance.setAnnotator("annotator");
      g.addAnnotation(utterance);

      try
      {
         g.setTracker(new ChangeTracker());
	 SimpleTokenizer tokenizer = new SimpleTokenizer("utterance", "word");
	 tokenizer.transform(g);
	 Annotation[] words = g.getAnnotation("turn1").all("word");
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

	 words = g.getAnnotation("turn2").all("word");
	 assertEquals(3, words.length);
	 assertEquals("first word shares start with utterance", "a2", words[0].getStartId());
	 assertEquals("the", words[0].getLabel());
	 assertEquals("lazy", words[1].getLabel());
	 assertEquals("dog", words[2].getLabel());
	 assertEquals("first word shares end with utterance", "a3", words[2].getEndId());

	 for (Annotation word : g.all("word"))
	 {
            assertEquals("Tokens have same confidence as original annotation: " + word,
                         Integer.valueOf(Constants.CONFIDENCE_MANUAL), word.getConfidence());
            assertEquals("Tokens have same annotator as original annotation: " + word,
                         "annotator", word.getAnnotator());
	    if (!word.getStart().isStartOn("utterance"))
	    {
	       assertNull("Word start anchors have no confidence: " + word + " - "
                          + word.getStart() + ":" + word.getStart().getConfidence(),
			  word.getStart().getConfidence());
	    }
	    if (!word.getEnd().isEndOn("utterance"))
	    {
	       assertNull("Word end anchors have no confidence: " + word + " - "
                          + word.getEnd() + ":" + word.getEnd().getConfidence(),
			  word.getEnd().getConfidence());
	    }
	 } // next word
	 
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void characterAnchors() 
   {
      Graph g = new Graph();
      g.setId("my graph");

      // if anchor units are characters, then new anchors have offsets set.
      g.setOffsetUnits(Constants.UNIT_CHARACTERS);

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

      g.addAnchor(new Anchor("a0", 0.0, Constants.CONFIDENCE_MANUAL));
      g.addAnchor(new Anchor("a1", 20.0, Constants.CONFIDENCE_MANUAL));
      g.addAnchor(new Anchor("a2", 31.0, Constants.CONFIDENCE_MANUAL));
      g.addAnchor(new Anchor("a3", 44.0, Constants.CONFIDENCE_MANUAL));

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "my graph"));
      g.addAnnotation(new Annotation("participant2", "jane doe", "who", "my graph"));

      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a3", "participant1"));
      g.addAnnotation(new Annotation("turn2", "jane doe", "turn", "a2", "a3", "participant2"));

      g.addAnnotation(new Annotation("utterance1", "the quick brown fox", "utterance", "a0", "a1", "turn1"));
      g.addAnnotation(new Annotation("utterance2", "jumps over", "utterance", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("utterance3", "the lazy dog", "utterance", "a2", "a3", "turn2"));

      try
      {
         g.setTracker(new ChangeTracker());
	 SimpleTokenizer tokenizer = new SimpleTokenizer("utterance", "word");
	 tokenizer.transform(g);
	 Annotation[] words = g.getAnnotation("turn1").all("word");
	 assertEquals(6, words.length);

	 assertEquals("first word shares start with utterance", "a0", words[0].getStartId());

	 assertEquals("the", words[0].getLabel());

	 assertEquals("first anchors don't have null offsets", 
		      Double.valueOf(0.0), words[0].getStart().getOffset());
	 assertEquals("intermediate anchors don't have null offsets", 
		      Double.valueOf(4.0), words[0].getEnd().getOffset());
	 assertEquals("tokens chained together", words[0].getEndId(), words[1].getStartId());

	 assertEquals("quick", words[1].getLabel());
	 assertEquals("offsets set", Double.valueOf(10), words[1].getEnd().getOffset());
	 assertEquals("brown", words[2].getLabel());
	 assertEquals("offsets set", Double.valueOf(16), words[2].getEnd().getOffset());
	 assertEquals("fox", words[3].getLabel());
	 assertEquals("offsets set", Double.valueOf(20), words[3].getEnd().getOffset());
	 assertEquals("last word shares end with utterance", "a1", words[3].getEndId());
	 assertEquals("first word shares start with utterance", "a1", words[4].getStartId());
	 assertEquals("jumps", words[4].getLabel());
	 assertEquals("offsets set", Double.valueOf(26), words[4].getEnd().getOffset());
	 assertEquals("over", words[5].getLabel());
	 assertEquals("offsets set", Double.valueOf(31), words[5].getEnd().getOffset());
	 assertEquals("last word shares end with utterance", "a2", words[5].getEndId());

	 words = g.getAnnotation("turn2").all("word");
	 assertEquals(3, words.length);
	 assertEquals("first word shares start with utterance", "a2", words[0].getStartId());
	 assertEquals("the", words[0].getLabel());
	 assertEquals("offsets set", Double.valueOf(35), words[0].getEnd().getOffset());
	 assertEquals("lazy", words[1].getLabel());
	 assertEquals("offsets set", Double.valueOf(40), words[1].getEnd().getOffset());
	 assertEquals("dog", words[2].getLabel());
	 assertEquals("offsets set", Double.valueOf(44), words[2].getEnd().getOffset());
	 assertEquals("first word shares end with utterance", "a3", words[2].getEndId());
	 
	 for (Anchor a : g.getAnchors().values())
	 {
	    assertNotNull("ensure all anchors have confidence: " + a + ": " + a.getEndingAnnotations() + "." + a.getStartingAnnotations(),
			  a.getConfidence());
	    assertEquals("ensure all anchors have high confidence: " + a + " - " + a.getConfidence(),
			 Constants.CONFIDENCE_MANUAL, a.getConfidence().longValue());
	 }
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
         g.setTracker(new ChangeTracker());
	 SimpleTokenizer tokenizer = new SimpleTokenizer("utterance", "word");
	 tokenizer.transform(g);
	 Annotation[] words = g.getAnnotation("turn1").all("word");
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

	 words = g.getAnnotation("turn2").all("word");
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
         g.setTracker(new ChangeTracker());
	 SimpleTokenizer tokenizer = new SimpleTokenizer("utterance", "word", "\\|");
	 tokenizer.transform(g);
	 Annotation[] words = g.getAnnotation("turn1").all("word");
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

	 words = g.getAnnotation("turn2").all("word");
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

   @Test public void tokensInSource()
   {
      Graph g = new Graph();
      g.setId("my graph");

      g.addLayer(new Layer("who", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
                 .setPeers(true).setPeersOverlap(true).setSaturated(true));
      g.addLayer(new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
                 .setPeers(true).setPeersOverlap(false).setSaturated(false)
                 .setParentId("who").setParentIncludes(true));
      g.addLayer(new Layer("linkage", "Linkages").setAlignment(Constants.ALIGNMENT_INTERVAL)
                 .setPeers(true).setPeersOverlap(false).setSaturated(false)
                 .setParentId("turn").setParentIncludes(true)); 
      g.addLayer(new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
                 .setPeers(true).setPeersOverlap(false).setSaturated(false)
                 .setParentId("turn").setParentIncludes(true));

      // john smith
      g.addAnchor(new Anchor("turn1Start", 0.0)); // turn start
      g.addAnchor(new Anchor("a0", null)); // the
      g.addAnchor(new Anchor("a1", null)); // B_B_C
      g.addAnchor(new Anchor("a2", null)); // jumps
      g.addAnchor(new Anchor("a5", null)); // over
      g.addAnchor(new Anchor("a6", null)); // a
      g.addAnchor(new Anchor("a7", null)); // lazy
      g.addAnchor(new Anchor("a8", null)); // dog
      g.addAnchor(new Anchor("a9", null)); // end of dog
      g.addAnchor(new Anchor("turn1End", 9.0)); // turn end

      // jane doe
      g.addAnchor(new Anchor("turn2Start", 6.5)); // turn start
      g.addAnchor(new Anchor("b6", null)); // roses
      g.addAnchor(new Anchor("b7", null)); // are
      g.addAnchor(new Anchor("b8", null)); // red
      g.addAnchor(new Anchor("b9", null)); // violets
      g.addAnchor(new Anchor("b10", null)); // are
      g.addAnchor(new Anchor("b11", null)); // blue
      g.addAnchor(new Anchor("b12", null)); // end of blue
      g.addAnchor(new Anchor("turn2End", 12.5)); // turn end

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "turn1Start", "turn2End", "my graph"));
      g.addAnnotation(new Annotation("participant2", "jane doe", "who", "turn1Start", "turn2End", "my graph"));
      
      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turn1Start", "turn1End", "participant1"));
      g.addAnnotation(new Annotation("turn2", "jane doe", "turn", "turn2Start", "turn2End", "participant2"));
      
      g.addAnnotation(new Annotation("the",   "the",   "word", "a0", "a1", "turn1"));
      g.addAnnotation(new Annotation("B_B_C", "B_B_C", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("jumps", "jumps", "word", "a2", "a5", "turn1"));
      g.addAnnotation(new Annotation("over",  "over",  "word", "a5", "a6", "turn1"));
      g.addAnnotation(new Annotation("a",     "a",     "word", "a6", "a7", "turn1"));
      g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a7", "a8", "turn1"));
      g.addAnnotation(new Annotation("dog",  "dog",    "word", "a8", "a9", "turn1"));

      g.addAnnotation(new Annotation("roses",   "roses",   "word", "b6",  "b7", "turn2"));
      g.addAnnotation(new Annotation("are",     "are",     "word", "b7",  "b8", "turn2"));
      g.addAnnotation(new Annotation("red",     "red",     "word", "b8",  "b9", "turn2"));
      g.addAnnotation(new Annotation("violets", "violets", "word", "b9",  "b10", "turn2"));
      g.addAnnotation(new Annotation("are2",    "are",     "word", "b10", "b11", "turn2"));
      g.addAnnotation(new Annotation("blue",    "roses",   "word", "b11", "b12", "turn2"));

      try
      {
         g.setTracker(new ChangeTracker());
	 SimpleTokenizer tokenizer = new SimpleTokenizer("word", "linkage", "_", true);
	 tokenizer.transform(g);
	 Annotation[] words = g.getAnnotation("turn1").all("word");
	 assertEquals(10, words.length);

	 assertEquals("the", words[0].getLabel());
	 assertEquals("B", words[1].getLabel());
	 assertEquals("B_B_C", words[2].getLabel());
	 assertEquals(Change.Operation.Destroy, words[2].getChange());
	 assertEquals("B", words[3].getLabel());
	 assertEquals("C", words[4].getLabel());
	 assertEquals("jumps", words[5].getLabel());
	 assertEquals("over", words[6].getLabel());
	 assertEquals("a", words[7].getLabel());
	 assertEquals("lazy", words[8].getLabel());
	 assertEquals("dog", words[9].getLabel());

	 // ordinals correct
	 assertEquals("ordinal check", 1, words[0].getOrdinal());
	 assertEquals("ordinal check", 2, words[1].getOrdinal());
	 assertEquals("ordinal check", 3, words[3].getOrdinal());
	 assertEquals("ordinal check", 4, words[4].getOrdinal());
	 assertEquals("ordinal check", 5, words[5].getOrdinal());
	 assertEquals("ordinal check", 6, words[6].getOrdinal());
	 assertEquals("ordinal check", 7, words[7].getOrdinal());
	 assertEquals("ordinal check", 8, words[8].getOrdinal());
	 assertEquals("ordinal check", 9, words[9].getOrdinal());

	 // anchors
	 assertEquals("anchor link", words[0].getEnd(), words[1].getStart());
	 assertEquals("anchor link", words[1].getEnd(), words[3].getStart());
	 assertEquals("anchor link", words[3].getEnd(), words[4].getStart());
	 assertEquals("anchor link", words[4].getEnd(), words[5].getStart());
	 assertEquals("anchor link", words[5].getEnd(), words[6].getStart());
	 assertEquals("anchor link", words[6].getEnd(), words[7].getStart());
	 assertEquals("anchor link", words[7].getEnd(), words[8].getStart());
	 assertEquals("anchor link", words[8].getEnd(), words[9].getStart());

	 // destination
	 Annotation[] linkage = g.getAnnotation("turn1").all("linkage");
	 assertEquals("" + linkage, 1, linkage.length);
	 assertEquals(words[2].getLabel(), linkage[0].getLabel());
	 assertEquals(words[1].getStart(), linkage[0].getStart());
	 assertEquals(words[4].getEnd(), linkage[0].getEnd());

	 // parents
	 assertEquals("turn1", linkage[0].getParentId());
	 for (Annotation word : words)
	 {
	    assertEquals("turn1", word.getParentId());
	 }
	 
	 words = g.getAnnotation("turn2").all("word");
	 assertEquals(6, words.length);
	 // ordinals correct
	 for (int i = 0; i < words.length; i++)
	 {
	    assertEquals("ordinal check: " + words[i], i+1, words[i].getOrdinal());
	 }
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void tokensInSourceNoDestination()
   {
      Graph g = new Graph();
      g.setId("my graph");

      g.addLayer(new Layer("who", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
                 .setPeers(true).setPeersOverlap(true).setSaturated(true));
      g.addLayer(new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
                 .setPeers(true).setPeersOverlap(false).setSaturated(false)
                 .setParentId("who").setParentIncludes(true));
      g.addLayer(new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
                 .setPeers(true).setPeersOverlap(false).setSaturated(false)
                 .setParentId("turn").setParentIncludes(true));

      // john smith
      g.addAnchor(new Anchor("turn1Start", 0.0)); // turn start
      g.addAnchor(new Anchor("a0", null)); // the
      g.addAnchor(new Anchor("a1", null)); // B_B_C
      g.addAnchor(new Anchor("a2", null)); // jumps
      g.addAnchor(new Anchor("a5", null)); // over
      g.addAnchor(new Anchor("a6", null)); // a
      g.addAnchor(new Anchor("a7", null)); // lazy
      g.addAnchor(new Anchor("a8", null)); // dog
      g.addAnchor(new Anchor("a9", null)); // end of dog
      g.addAnchor(new Anchor("turn1End", 9.0)); // turn end

      // jane doe
      g.addAnchor(new Anchor("turn2Start", 6.5)); // turn start
      g.addAnchor(new Anchor("b6", null)); // roses
      g.addAnchor(new Anchor("b7", null)); // are
      g.addAnchor(new Anchor("b8", null)); // red
      g.addAnchor(new Anchor("b9", null)); // violets
      g.addAnchor(new Anchor("b10", null)); // are
      g.addAnchor(new Anchor("b11", null)); // blue
      g.addAnchor(new Anchor("b12", null)); // end of blue
      g.addAnchor(new Anchor("turn2End", 12.5)); // turn end

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "turn1Start", "turn2End", "my graph"));
      g.addAnnotation(new Annotation("participant2", "jane doe", "who", "turn1Start", "turn2End", "my graph"));
      
      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turn1Start", "turn1End", "participant1"));
      g.addAnnotation(new Annotation("turn2", "jane doe", "turn", "turn2Start", "turn2End", "participant2"));
      
      g.addAnnotation(new Annotation("the",   "the",   "word", "a0", "a1", "turn1"));
      g.addAnnotation(new Annotation("B_B_C", "B_B_C", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("jumps", "jumps", "word", "a2", "a5", "turn1"));
      g.addAnnotation(new Annotation("over",  "over",  "word", "a5", "a6", "turn1"));
      g.addAnnotation(new Annotation("a",     "a",     "word", "a6", "a7", "turn1"));
      g.addAnnotation(new Annotation("lazy",  "lazy",  "word", "a7", "a8", "turn1"));
      g.addAnnotation(new Annotation("dog",  "dog",    "word", "a8", "a9", "turn1"));

      g.addAnnotation(new Annotation("roses",   "roses",   "word", "b6",  "b7", "turn2"));
      g.addAnnotation(new Annotation("are",     "are",     "word", "b7",  "b8", "turn2"));
      g.addAnnotation(new Annotation("red",     "red",     "word", "b8",  "b9", "turn2"));
      g.addAnnotation(new Annotation("violets", "violets", "word", "b9",  "b10", "turn2"));
      g.addAnnotation(new Annotation("are2",    "are",     "word", "b10", "b11", "turn2"));
      g.addAnnotation(new Annotation("blue",    "roses",   "word", "b11", "b12", "turn2"));

      try
      {
         g.setTracker(new ChangeTracker());
	 SimpleTokenizer tokenizer = new SimpleTokenizer("word", null, "_", true);
	 tokenizer.transform(g);
	 Annotation[] words = g.getAnnotation("turn1").all("word");
	 assertEquals(10, words.length);

	 assertEquals("the", words[0].getLabel());
	 assertEquals("B", words[1].getLabel());
	 assertEquals("B_B_C", words[2].getLabel());
	 assertEquals(Change.Operation.Destroy, words[2].getChange());
	 assertEquals("B", words[3].getLabel());
	 assertEquals("C", words[4].getLabel());
	 assertEquals("jumps", words[5].getLabel());
	 assertEquals("over", words[6].getLabel());
	 assertEquals("a", words[7].getLabel());
	 assertEquals("lazy", words[8].getLabel());
	 assertEquals("dog", words[9].getLabel());

	 // ordinals correct
	 assertEquals("ordinal check", 1, words[0].getOrdinal());
	 assertEquals("ordinal check", 2, words[1].getOrdinal());
	 assertEquals("ordinal check", 3, words[3].getOrdinal());
	 assertEquals("ordinal check", 4, words[4].getOrdinal());
	 assertEquals("ordinal check", 5, words[5].getOrdinal());
	 assertEquals("ordinal check", 6, words[6].getOrdinal());
	 assertEquals("ordinal check", 7, words[7].getOrdinal());
	 assertEquals("ordinal check", 8, words[8].getOrdinal());
	 assertEquals("ordinal check", 9, words[9].getOrdinal());

	 // anchors
	 assertEquals("anchor link", words[0].getEnd(), words[1].getStart());
	 assertEquals("anchor link", words[1].getEnd(), words[3].getStart());
	 assertEquals("anchor link", words[3].getEnd(), words[4].getStart());
	 assertEquals("anchor link", words[4].getEnd(), words[5].getStart());
	 assertEquals("anchor link", words[5].getEnd(), words[6].getStart());
	 assertEquals("anchor link", words[6].getEnd(), words[7].getStart());
	 assertEquals("anchor link", words[7].getEnd(), words[8].getStart());
	 assertEquals("anchor link", words[8].getEnd(), words[9].getStart());
	 
	 words = g.getAnnotation("turn2").all("word");
	 assertEquals(6, words.length);
	 // ordinals correct
	 for (int i = 0; i < words.length; i++)
	 {
	    assertEquals("ordinal check: " + words[i], i+1, words[i].getOrdinal());
	 }
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void nonLatin() 
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

      g.addAnchor(new Anchor("a0", 0.0, Constants.CONFIDENCE_MANUAL));
      g.addAnchor(new Anchor("a1", 1.0, Constants.CONFIDENCE_MANUAL));
      g.addAnchor(new Anchor("a2", 2.0, Constants.CONFIDENCE_MANUAL));
      g.addAnchor(new Anchor("a3", 3.0, Constants.CONFIDENCE_MANUAL));

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "my graph"));
      g.addAnnotation(new Annotation("participant2", "jane doe", "who", "my graph"));

      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a3", "participant1"));
      g.addAnnotation(new Annotation("turn2", "jane doe", "turn", "a2", "a3", "participant2"));

      g.addAnnotation(new Annotation("utterance1", "그게 뭐 영원히 남아서", "utterance", "a0", "a1", "turn1"));
      g.addAnnotation(new Annotation("utterance2", "뭐 이제", "utterance", "a1", "a3", "turn1"));
      g.addAnnotation(new Annotation("utterance3", "선 보는 데도", "utterance", "a2", "a3", "turn2"));

      try
      {
         g.setTracker(new ChangeTracker());
	 SimpleTokenizer tokenizer = new SimpleTokenizer("utterance", "word");
	 tokenizer.transform(g);
	 Annotation[] words = g.getAnnotation("turn1").all("word");
	 assertEquals(6, words.length);

	 assertEquals("first word shares start with utterance", "a0", words[0].getStartId());

	 assertEquals("그게", words[0].getLabel());

	 assertNull("intermediate anchors have null offsets", words[0].getEnd().getOffset());
	 assertEquals("tokens chained together", words[0].getEndId(), words[1].getStartId());

	 assertEquals("뭐", words[1].getLabel());
	 assertEquals("영원히", words[2].getLabel());
	 assertEquals("남아서", words[3].getLabel());
	 assertEquals("last word shares end with utterance", "a1", words[3].getEndId());
	 assertEquals("first word shares start with utterance", "a1", words[4].getStartId());
	 assertEquals("뭐", words[4].getLabel());
	 assertEquals("이제", words[5].getLabel());
	 assertEquals("last word shares end with utterance", "a3", words[5].getEndId());

	 words = g.getAnnotation("turn2").all("word");
	 assertEquals(3, words.length);
	 assertEquals("first word shares start with utterance", "a2", words[0].getStartId());
	 assertEquals("선", words[0].getLabel());
	 assertEquals("보는", words[1].getLabel());
	 assertEquals("데도", words[2].getLabel());
	 assertEquals("first word shares end with utterance", "a3", words[2].getEndId());

	 for (Annotation word : g.all("word"))
	 {
	    if (!word.getStart().isStartOn("utterance"))
	    {
	       assertNull("Ensure word start anchors have no confidence: " + word + " - " + word.getStart() + ":" + word.getStart().getConfidence(),
			  word.getStart().getConfidence());
	    }
	    if (!word.getEnd().isEndOn("utterance"))
	    {
	       assertNull("Ensure word end anchors have no confidence: " + word + " - " + word.getEnd() + ":" + word.getEnd().getConfidence(),
			  word.getEnd().getConfidence());
	    }
	 } // next word
	 
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   @Test public void nonBMPUnicode() 
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

      g.addAnchor(new Anchor("a0", 0.0, Constants.CONFIDENCE_MANUAL));
      g.addAnchor(new Anchor("a1", 1.0, Constants.CONFIDENCE_MANUAL));
      g.addAnchor(new Anchor("a2", 2.0, Constants.CONFIDENCE_MANUAL));
      g.addAnchor(new Anchor("a3", 3.0, Constants.CONFIDENCE_MANUAL));

      g.addAnnotation(new Annotation("participant1", "john smith", "who", "my graph"));
      g.addAnnotation(new Annotation("participant2", "jane doe", "who", "my graph"));

      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a3", "participant1"));
      g.addAnnotation(new Annotation("turn2", "jane doe", "turn", "a2", "a3", "participant2"));

      g.addAnnotation(new Annotation("utterance1", "🍁 𝓜𝓪𝓷𝓰𝓸 𝓑𝓪𝔂 😍", "utterance", "a0", "a1", "turn1"));
      g.addAnnotation(new Annotation("utterance2", "🅳🅰🅽🅺 🅼🅴🅼🅴🆂", "utterance", "a1", "a3", "turn1"));
      g.addAnnotation(new Annotation("utterance3", "༼つ ◕_◕ ༽つ", "utterance", "a2", "a3", "turn2"));

      try
      {
         g.setTracker(new ChangeTracker());
	 SimpleTokenizer tokenizer = new SimpleTokenizer("utterance", "word");
	 tokenizer.transform(g);
	 Annotation[] words = g.getAnnotation("turn1").all("word");
	 assertEquals(6, words.length);

	 assertEquals("first word shares start with utterance", "a0", words[0].getStartId());

	 assertEquals("🍁", words[0].getLabel());

	 assertNull("intermediate anchors have null offsets", words[0].getEnd().getOffset());
	 assertEquals("tokens chained together", words[0].getEndId(), words[1].getStartId());

	 assertEquals("𝓜𝓪𝓷𝓰𝓸", words[1].getLabel());
	 assertEquals("𝓑𝓪𝔂", words[2].getLabel());
	 assertEquals("😍", words[3].getLabel());
	 assertEquals("last word shares end with utterance", "a1", words[3].getEndId());
	 assertEquals("first word shares start with utterance", "a1", words[4].getStartId());
	 assertEquals("🅳🅰🅽🅺", words[4].getLabel());
	 assertEquals("🅼🅴🅼🅴🆂", words[5].getLabel());
	 assertEquals("last word shares end with utterance", "a3", words[5].getEndId());

	 words = g.getAnnotation("turn2").all("word");
	 assertEquals(3, words.length);
	 assertEquals("first word shares start with utterance", "a2", words[0].getStartId());
	 assertEquals("༼つ", words[0].getLabel());
	 assertEquals("◕_◕", words[1].getLabel());
	 assertEquals("༽つ", words[2].getLabel());
	 assertEquals("first word shares end with utterance", "a3", words[2].getEndId());

	 for (Annotation word : g.all("word"))
	 {
	    if (!word.getStart().isStartOn("utterance"))
	    {
	       assertNull("Ensure word start anchors have no confidence: " + word + " - " + word.getStart() + ":" + word.getStart().getConfidence(),
			  word.getStart().getConfidence());
	    }
	    if (!word.getEnd().isEndOn("utterance"))
	    {
	       assertNull("Ensure word end anchors have no confidence: " + word + " - " + word.getEnd() + ":" + word.getEnd().getConfidence(),
			  word.getEnd().getConfidence());
	    }
	 } // next word
	 
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
   }

   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.util.TestSimpleTokenizer");
   }
}
