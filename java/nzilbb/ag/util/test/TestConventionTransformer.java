//
// Copyright 2016-2019 New Zealand Institute of Language, Brain and Behaviour, 
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
import java.util.List;
import java.util.SortedSet;
import java.util.Vector;
import nzilbb.ag.*;
import nzilbb.ag.util.*;

public class TestConventionTransformer
{
  @Test public void disfluency() 
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
    g.addLayer(new Layer("disfluency", "Disfluency", Constants.ALIGNMENT_NONE,
                         false, // peers
                         false, // peersOverlap
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

    g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("word2", "&qui", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("word3", "quick", "word", "a3", "a4", "turn1"));
    g.addAnnotation(new Annotation("word4", "fox&hound", "word", "a4", "a?1", "turn1"));
    g.addAnnotation(new Annotation("word5", "jumps", "word", "a?1", "a?2", "turn1"));
    g.addAnnotation(new Annotation("word6", "over", "word", "a?2", "a5", "turn1"));

    try
    {
      ConventionTransformer transformer = new ConventionTransformer("word", "&(.+)", "$1", "disfluency", "DIS");
      List<Change> changes = transformer.transform(g);
      assertEquals("the", g.getAnnotation("word1").getLabel());
      assertEquals("full label matches", "qui", g.getAnnotation("word2").getLabel());
      assertEquals("quick", g.getAnnotation("word3").getLabel());
      assertEquals("only part of the label matches", "fox&hound", g.getAnnotation("word4").getLabel());
      assertEquals("jumps", g.getAnnotation("word5").getLabel());
      assertEquals("over", g.getAnnotation("word6").getLabel());

      assertNull(g.getAnnotation("word1").my("disfluency"));
      assertEquals("DIS", g.getAnnotation("word2").my("disfluency").getLabel());
      assertNull(g.getAnnotation("word3").my("disfluency"));
      assertNull(g.getAnnotation("word4").my("disfluency"));
      assertNull(g.getAnnotation("word5").my("disfluency"));
      assertNull(g.getAnnotation("word6").my("disfluency"));

    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }

  @Test public void pos() 
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

    g.addAnnotation(new Annotation("word1", "the_DT", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("word2", "quick_A", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("word3", "brown_A", "word", "a3", "a4", "turn1"));
    g.addAnnotation(new Annotation("word4", "fox_N", "word", "a4", "a?1", "turn1"));
    g.addAnnotation(new Annotation("word5", "jumps_V", "word", "a?1", "a?2", "turn1"));
    g.addAnnotation(new Annotation("word6", "over", "word", "a?2", "a5", "turn1"));

    try
    {
      ConventionTransformer transformer = new ConventionTransformer("word", "(.+)_(.+)", "$1", "pos", "$2");
      List<Change> changes = transformer.transform(g);
      assertEquals("the", g.getAnnotation("word1").getLabel());
      assertEquals("quick", g.getAnnotation("word2").getLabel());
      assertEquals("brown", g.getAnnotation("word3").getLabel());
      assertEquals("fox", g.getAnnotation("word4").getLabel());
      assertEquals("jumps", g.getAnnotation("word5").getLabel());
      assertEquals("over", g.getAnnotation("word6").getLabel());

      assertEquals("DT", g.getAnnotation("word1").my("pos").getLabel());
      assertEquals("A", g.getAnnotation("word2").my("pos").getLabel());
      assertEquals("A", g.getAnnotation("word3").my("pos").getLabel());
      assertEquals("N", g.getAnnotation("word4").my("pos").getLabel());
      assertEquals("V", g.getAnnotation("word5").my("pos").getLabel());
      assertNull(g.getAnnotation("word6").my("pos"));

    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }

  @Test public void noise() 
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
    g.addLayer(new Layer("noise", "Noise", Constants.ALIGNMENT_INTERVAL,
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
    g.addAnnotation(new Annotation("word2", "[coughs]", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("word3", "quick", "word", "a3", "a4", "turn1"));
    g.addAnnotation(new Annotation("word4", "fox", "word", "a4", "a?1", "turn1"));
    g.addAnnotation(new Annotation("word5", "jumps", "word", "a?1", "a?2", "turn1"));
    g.addAnnotation(new Annotation("word6", "over", "word", "a?2", "a5", "turn1"));

    g.setTracker(new ChangeTracker());
    try
    {
      ConventionTransformer transformer = new ConventionTransformer("word", "\\[(.+)\\]");
      transformer.addDestinationResult("noise", "$1");
      List<Change> changes = transformer.transform(g);
      assertEquals("annotation is deleted", Change.Operation.Destroy, g.getAnnotation("word2").getChange());

      SortedSet<Annotation> noises = g.getAnnotations("noise");
      assertEquals(1, noises.size());
      Annotation cough = noises.first();
      assertEquals("a2", cough.getStartId());
      assertEquals("a3", cough.getEndId());

    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }

  @Test public void ELANConventions() 
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
    g.addLayer(new Layer("pron", "Pronounce", Constants.ALIGNMENT_NONE,
                         false, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("lex", "Lexical", Constants.ALIGNMENT_NONE,
                         false, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes

    g.addAnchor(new Anchor("a0", 0.0)); // turn start
    g.addAnchor(new Anchor("a1", 1.0)); // the
    g.addAnchor(new Anchor("a2", 2.0)); // qweek
    g.addAnchor(new Anchor("a3", 3.0)); // qweek
    g.addAnchor(new Anchor("a4", 4.0)); // qui~
    // unset offsets
    g.addAnchor(new Anchor("a?1", null)); // qu~
    g.addAnchor(new Anchor("a?2", null)); // ah
    g.addAnchor(new Anchor("a5", 5.0)); // end of ah
    g.addAnchor(new Anchor("a6", 6.0)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a6", "my graph"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a6", "participant1"));

    g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("word2", "qweek[kwik](quick)", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("word3", "qweek(quick)[kwik]", "word", "a3", "a4", "turn1"));
    g.addAnnotation(new Annotation("word4", "qu~[kw@](quick)", "word", "a4", "a?1", "turn1"));
    g.addAnnotation(new Annotation("word5", "qu~(quick)[kw@]", "word", "a?1", "a?2", "turn1"));
    g.addAnnotation(new Annotation("word6", "ah", "word", "a?2", "a5", "turn1"));

    try
    {
      // word[pronounce]
      ConventionTransformer pronounceTransformer = new ConventionTransformer(
        "word", "(.+)\\[(.*)\\](\\p{Punct}*)", "$1$3", "pron", "$2");
      pronounceTransformer.transform(g);
      g.commit();
	 
      // word(lexical)
      ConventionTransformer lexicalTransformer = new ConventionTransformer(
        "word", "(.+)\\((.*)\\)(\\p{Punct}*)", "$1$3", "lex", "$2");
      lexicalTransformer.transform(g);
      g.commit();

      // run word[pronounce] again, in case some were masked by lexical tags:
      // word[pronounce](lexical)
      pronounceTransformer.transform(g);
      g.commit();

      // ensure the order of the conventions doesn't matter

      Annotation token = g.getAnnotation("word2");
      assertNotNull("pron-before-lex: pron", token.my("pron"));
      assertEquals("pron-before-lex: pron label", "kwik", token.my("pron").getLabel());
      assertNotNull("pron-before-lex: lex", token.my("lex"));
      assertEquals("pron-before-lex: lex label", "quick", token.my("lex").getLabel());
      assertEquals("pron-before-lex: token label", "qweek", token.getLabel());

      token = g.getAnnotation("word3");
      assertNotNull("lex-before-pron: pron", token.my("pron"));
      assertEquals("lex-before-pron: pron label", "kwik", token.my("pron").getLabel());
      assertNotNull("lex-before-pron: lex", token.my("lex"));
      assertEquals("lex-before-pron: lex label", "quick", token.my("lex").getLabel());
      assertEquals("pron-before-lex: token label", "qweek", token.getLabel());

      token = g.getAnnotation("word4");
      assertNotNull("~ pron-before-lex: pron", token.my("pron"));
      assertEquals("~ pron-before-lex: pron label", "kw@", token.my("pron").getLabel());
      assertNotNull("~ pron-before-lex: lex", token.my("lex"));
      assertEquals("~ pron-before-lex: lex label", "quick", token.my("lex").getLabel());
      assertEquals("pron-before-lex: token label", "qu~", token.getLabel());

      token = g.getAnnotation("word5");
      assertNotNull("~ lex-before-pron: pron", token.my("pron"));
      assertEquals("~ lex-before-pron: pron label", "kw@", token.my("pron").getLabel());
      assertNotNull("~ lex-before-pron: lex", token.my("lex"));
      assertEquals("~ lex-before-pron: lex label", "quick", token.my("lex").getLabel());
      assertEquals("pron-before-lex: token label", "qu~", token.getLabel());

    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }
  
  @Test public void MalformedELANConventions() 
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
    g.addLayer(new Layer("pron", "Pronounce", Constants.ALIGNMENT_NONE,
                         false, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("lex", "Lexical", Constants.ALIGNMENT_NONE,
                         false, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes

    g.addAnchor(new Anchor("a0", 0.0)); // turn start
    g.addAnchor(new Anchor("a1", 1.0)); // the
    g.addAnchor(new Anchor("a2", 2.0)); // qweek
    g.addAnchor(new Anchor("a3", 3.0)); // qweek
    g.addAnchor(new Anchor("a4", 4.0)); // qui~
    // unset offsets
    g.addAnchor(new Anchor("a?1", null)); // qu~
    g.addAnchor(new Anchor("a?2", null)); // ah
    g.addAnchor(new Anchor("a5", 5.0)); // end of ah
    g.addAnchor(new Anchor("a6", 6.0)); // turn end

    g.addAnnotation(new Annotation("participant1", "john smith", "who", "a0", "a6", "my graph"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a6", "participant1"));

    g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("word2", "qweek[kwik](quick)", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("word3", "qweek(quick)[kwik]", "word", "a3", "a4", "turn1"));
    g.addAnnotation(new Annotation("word4", "qu[kw@](quick)~", "word", "a4", "a?1", "turn1"));
    g.addAnnotation(new Annotation("word5", "qu(quick)[kw@]~", "word", "a?1", "a?2", "turn1"));
    g.addAnnotation(new Annotation("word6", "ah", "word", "a?2", "a5", "turn1"));

    try
    {
      // word[pronounce]
      ConventionTransformer pronounceTransformer = new ConventionTransformer(
        "word", "(.+)\\[(.*)\\](\\p{Punct}*)", "$1$3", "pron", "$2");
      pronounceTransformer.transform(g);
      g.commit();
	 
      // word(lexical)
      ConventionTransformer lexicalTransformer = new ConventionTransformer(
        "word", "(.+)\\((.*)\\)(\\p{Punct}*)", "$1$3", "lex", "$2");
      lexicalTransformer.transform(g);
      g.commit();

      // run word[pronounce] again, in case some were masked by lexical tags:
      // word[pronounce](lexical)
      pronounceTransformer.transform(g);
      g.commit();

      // ensure the order of the conventions doesn't matter

      Annotation token = g.getAnnotation("word2");
      assertNotNull("pron-before-lex: pron", token.my("pron"));
      assertEquals("pron-before-lex: pron label", "kwik", token.my("pron").getLabel());
      assertNotNull("pron-before-lex: lex", token.my("lex"));
      assertEquals("pron-before-lex: lex label", "quick", token.my("lex").getLabel());
      assertEquals("pron-before-lex: token label", "qweek", token.getLabel());

      token = g.getAnnotation("word3");
      assertNotNull("lex-before-pron: pron", token.my("pron"));
      assertEquals("lex-before-pron: pron label", "kwik", token.my("pron").getLabel());
      assertNotNull("lex-before-pron: lex", token.my("lex"));
      assertEquals("lex-before-pron: lex label", "quick", token.my("lex").getLabel());
      assertEquals("pron-before-lex: token label", "qweek", token.getLabel());

      token = g.getAnnotation("word4");
      assertNotNull("~ pron-before-lex: pron", token.my("pron"));
      assertEquals("~ pron-before-lex: pron label", "kw@", token.my("pron").getLabel());
      assertNotNull("~ pron-before-lex: lex", token.my("lex"));
      assertEquals("~ pron-before-lex: lex label", "quick", token.my("lex").getLabel());
      assertEquals("pron-before-lex: token label", "qu~", token.getLabel());

      token = g.getAnnotation("word5");
      assertNotNull("~ lex-before-pron: pron", token.my("pron"));
      assertEquals("~ lex-before-pron: pron label", "kw@", token.my("pron").getLabel());
      assertNotNull("~ lex-before-pron: lex", token.my("lex"));
      assertEquals("~ lex-before-pron: lex label", "quick", token.my("lex").getLabel());
      assertEquals("pron-before-lex: token label", "qu~", token.getLabel());

    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }

  @Test public void nullDestinationLayer() 
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
    g.addAnnotation(new Annotation("word2", "quick", "word", "a2", "a3", "turn1"));
    // pronunciation annotation, which will be stripped out
    g.addAnnotation(new Annotation("word3", "brown[brAHn]", "word", "a3", "a4", "turn1"));
    g.addAnnotation(new Annotation("word4", "fox", "word", "a4", "a?1", "turn1"));
    g.addAnnotation(new Annotation("word5", "jumps", "word", "a?1", "a?2", "turn1"));
    g.addAnnotation(new Annotation("word6", "over", "word", "a?2", "a5", "turn1"));

    try
    {
      ConventionTransformer transformer = new ConventionTransformer(
        "word", "(.*)\\[(.*)\\]", "$1", null, "$2"); // null destination layer
      List<Change> changes = transformer.transform(g);
      assertEquals("annotation is deleted", "brown", g.getAnnotation("word3").getLabel());	 
    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }

  @Test public void clanAcronyms() // TODO break the acronyms apart properly
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
    g.addLayer(new Layer("acronym", "CLAN acronym", Constants.ALIGNMENT_NONE,
                         false, // peers
                         false, // peersOverlap
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

    g.addAnnotation(new Annotation("word1", "Scarface_Claw", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("word2", "jumps", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("word3", "over", "word", "a3", "a4", "turn1"));
    g.addAnnotation(new Annotation("word4", "the", "word", "a4", "a?1", "turn1"));
    g.addAnnotation(new Annotation("word5", "lazy", "word", "a?1", "a?2", "turn1"));
    g.addAnnotation(new Annotation("word6", "B_B_C", "word", "a?2", "a5", "turn1"));

    try
    {
      ConventionTransformer transformer = new ConventionTransformer("word", "(.+)_(.+)", "$0", "acronym", "$1 $2");
      List<Change> changes = transformer.transform(g);
      assertEquals("one underscore", "Scarface_Claw", g.getAnnotation("word1").getLabel());
      assertEquals("two underscores", "B_B_C", g.getAnnotation("word6").getLabel());

      assertEquals("Scarface Claw", g.getAnnotation("word1").my("acronym").getLabel());
      assertEquals("two underscores TODO make this B B C", "B_B C", g.getAnnotation("word6").my("acronym").getLabel());

    }
    catch(TransformationException exception)
    {
      fail(exception.toString());
    }
  }


  public static void main(String args[]) 
  {
    org.junit.runner.JUnitCore.main("nzilbb.ag.util.test.TestConventionTransformer");
  }
}
