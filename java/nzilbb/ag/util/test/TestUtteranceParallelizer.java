//
// Copyright 2019-2020 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import nzilbb.ag.*;
import nzilbb.ag.util.*;

public class TestUtteranceParallelizer
{
   @Test public void envelopedTurn() throws Exception
   {
      Schema schema = new Schema(
         "who", "turn", "utterance", "word",
         new Layer("who", "Participants")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(true).setPeersOverlap(true)
         .setSaturated(true),
	 new Layer("turn", "Speaker turns")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false)
         .setSaturated(false).setParentId("who"),
         new Layer("utterance", "Utterances")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false)
         .setSaturated(true).setParentId("turn"),
         new Layer("word", "Words")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false)
         .setSaturated(false).setParentId("turn"));
      
      // create a graph with simultaneous speech turns
      
      Graph graph = new Graph()
         .setId("simultaneous_speech")
         .setSchema(schema);
      graph.addAnchor(new Anchor("a0", 0.0));
      graph.addAnchor(new Anchor("a15", 15.0));
      // transcript attributes
      graph.addAnnotation(new Annotation("l", "en-NZ", "transcript_language", "a0", "a15"));
      graph.addAnnotation(new Annotation("by", "robert", "scribe", "a0", "a15"));
      graph.addAnnotation(new Annotation("v", "1", "version", "a0", "a15"));
      graph.addAnnotation(new Annotation("date", "20191211", "version_date", "a0", "a15"));
      // participants
      graph.addAnnotation(new Annotation("p1", "p1", "who", "a0", "a15"));
      graph.addAnnotation(new Annotation("p2", "p2", "who", "a0", "a15"));
      // participant attributes
      graph.addAnnotation(new Annotation("g1", "M", "gender", "a0", "a15", "p1"));
      graph.addAnnotation(new Annotation("g2", "F", "gender", "a0", "a15", "p2"));
      // turns
      graph.addAnnotation(new Annotation("t1", "p1", "turn", "a0", "a15", "p1"));
      graph.addAnchor(new Anchor("a5", 5.0));
      graph.addAnchor(new Anchor("a10", 10.0));
      graph.addAnnotation(new Annotation("t2", "p2", "turn", "a5", "a10", "p2"));
      // utterances
      graph.addAnnotation(new Annotation("u1-1", "p1", "utterance", "a0", "a5", "t1"));
      graph.addAnnotation(new Annotation("u1-2", "p1", "utterance", "a5", "a10", "t1"));
      graph.addAnnotation(new Annotation("u2-1", "p2", "utterance", "a5", "a10", "t2"));
      graph.addAnnotation(new Annotation("u1-3", "p1", "utterance", "a10", "a15", "t1"));

      // words
      graph.addAnnotation(new Annotation("w1-1", "w1-1", "word", 
                                         graph.addAnchor(new Anchor("a1", 1.0)).getId(),
                                         graph.addAnchor(new Anchor("a2", 2.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("w1-2", "w1-2", "word", 
                                         "a2",
                                         graph.addAnchor(new Anchor("a3", 3.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("w1-3", "w1-3", "word", 
                                         "a3",
                                         graph.addAnchor(new Anchor("a4", 4.0)).getId(),
                                         "t1"));
      
      graph.addAnnotation(new Annotation("w1-6", "w1-6", "word", 
                                         graph.addAnchor(new Anchor("a6", 6.0)).getId(),
                                         graph.addAnchor(new Anchor("a7", 7.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("w1-7", "w1-7", "word", 
                                         "a7", 
                                         graph.addAnchor(new Anchor("a8", 8.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("w1-8", "w1-8", "word", 
                                         "a8",
                                         graph.addAnchor(new Anchor("a9", 9.0)).getId(),
                                         "t1"));
      

      graph.addAnnotation(new Annotation("w1-11", "w1-11", "word", 
                                         graph.addAnchor(new Anchor("a11", 11.0)).getId(),
                                         graph.addAnchor(new Anchor("a12", 12.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("w1-12", "w1-12", "word", 
                                         "a12",
                                         graph.addAnchor(new Anchor("a13", 13.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("w1-13", "w1-13", "word", 
                                         "a13",
                                         graph.addAnchor(new Anchor("a14", 14.0)).getId(),
                                         "t1"));
      
      graph.addAnnotation(new Annotation("w2-6.5", "w2-6.5", "word", 
                                         graph.addAnchor(new Anchor("a6.5", 6.5)).getId(),
                                         graph.addAnchor(new Anchor("a7.5", 7.5)).getId(),
                                         "t2"));
      graph.addAnnotation(new Annotation("w2-7.5", "w2-7.5", "word", 
                                         "a7.5",
                                         graph.addAnchor(new Anchor("a8.5", 8.5)).getId(),
                                         "t2"));
      assertEquals("2 turns", 2, graph.list("turn").length);

      graph.trackChanges();
      
      UtteranceParallelizer parallelizer = new UtteranceParallelizer(schema);
      try
      {
	 parallelizer.transform(graph);
         Set<Change> changes = graph.getTracker().getChanges();

	 assertNotEquals("changes: " + changes, 0, changes.size());

         // there are now four turns
         Annotation[] turns = graph.list("turn");
         // for (Annotation t : turns) System.out.println(t.getLabel() + " ("+t.getStart()+"-"+t.getEnd()+")");
	 assertEquals("now 4 turns: " + Arrays.asList(turns),
                      4, turns.length);

         // each p1 turn has three words and one utterance
         for (Annotation t : turns)
         {
            assertEquals("1 utterance " + t.getLabel() + " ("+t.getStart()+"-"+t.getEnd()+")",
                         1, t.list("utterance").length);
            if (t.getParentId().equals("p1"))
            {
               assertEquals("3 words " + t.getLabel() + " ("+t.getStart()+"-"+t.getEnd()+")",
                            3, t.list("word").length);
            }
            else
            {
               assertEquals("2 words " + t.getLabel() + " ("+t.getStart()+"-"+t.getEnd()+")",
                            2, t.list("word").length);
            }
         } // next turn
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
                      
   }
   
   @Test public void overlappingTurn() throws Exception
   {
      Schema schema = new Schema(
         "who", "turn", "utterance", "word",
         new Layer("who", "Participants")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(true).setPeersOverlap(true)
         .setSaturated(true),
	 new Layer("turn", "Speaker turns")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false)
         .setSaturated(false).setParentId("who"),
         new Layer("utterance", "Utterances")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false)
         .setSaturated(true).setParentId("turn"),
         new Layer("word", "Words")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false)
         .setSaturated(false).setParentId("turn"));
      
      // create a graph with simultaneous speech turns
      
      Graph graph = new Graph()
         .setId("simultaneous_speech")
         .setSchema(schema);
      graph.addAnchor(new Anchor("a0", 0.0));
      graph.addAnchor(new Anchor("a15", 15.0));
      // transcript attributes
      graph.addAnnotation(new Annotation("l", "en-NZ", "transcript_language", "a0", "a15"));
      graph.addAnnotation(new Annotation("by", "robert", "scribe", "a0", "a15"));
      graph.addAnnotation(new Annotation("v", "1", "version", "a0", "a15"));
      graph.addAnnotation(new Annotation("date", "20191211", "version_date", "a0", "a15"));
      // participants
      graph.addAnnotation(new Annotation("p1", "p1", "who", "a0", "a15"));
      graph.addAnnotation(new Annotation("p2", "p2", "who", "a0", "a15"));
      // participant attributes
      graph.addAnnotation(new Annotation("g1", "M", "gender", "a0", "a15", "p1"));
      graph.addAnnotation(new Annotation("g2", "F", "gender", "a0", "a15", "p2"));
      // turns
      graph.addAnchor(new Anchor("a5", 5.0));
      graph.addAnchor(new Anchor("a10", 10.0));
      graph.addAnnotation(new Annotation("t1", "p1", "turn", "a0", "a10", "p1"));
      graph.addAnnotation(new Annotation("t2", "p2", "turn", "a5", "a15", "p2"));
      // utterances
      graph.addAnnotation(new Annotation("u1-1", "p1", "utterance", "a0", "a5", "t1"));
      graph.addAnnotation(new Annotation("u1-2", "p1", "utterance", "a5", "a10", "t1"));
      graph.addAnnotation(new Annotation("u2-1", "p2", "utterance", "a5", "a10", "t2"));
      graph.addAnnotation(new Annotation("u2-2", "p2", "utterance", "a10", "a15", "t2"));

      // words
      graph.addAnnotation(new Annotation("w1-1", "w1-1", "word", 
                                         graph.addAnchor(new Anchor("a1", 1.0)).getId(),
                                         graph.addAnchor(new Anchor("a2", 2.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("w1-2", "w1-2", "word", 
                                         "a2",
                                         graph.addAnchor(new Anchor("a3", 3.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("w1-3", "w1-3", "word", 
                                         "a3",
                                         graph.addAnchor(new Anchor("a4", 4.0)).getId(),
                                         "t1"));
      
      graph.addAnnotation(new Annotation("w1-6", "w1-6", "word", 
                                         graph.addAnchor(new Anchor("a6", 6.0)).getId(),
                                         graph.addAnchor(new Anchor("a7", 7.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("w1-7", "w1-7", "word", 
                                         "a7", 
                                         graph.addAnchor(new Anchor("a8", 8.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("w1-8", "w1-8", "word", 
                                         "a8",
                                         graph.addAnchor(new Anchor("a9", 9.0)).getId(),
                                         "t1"));
      

      graph.addAnnotation(new Annotation("w2-6.5", "w2-6.5", "word", 
                                         graph.addAnchor(new Anchor("a6.5", 6.5)).getId(),
                                         graph.addAnchor(new Anchor("a7.5", 7.5)).getId(),
                                         "t2"));
      graph.addAnnotation(new Annotation("w2-7.5", "w2-7.5", "word", 
                                         "a7.5",
                                         graph.addAnchor(new Anchor("a8.5", 8.5)).getId(),
                                         "t2"));
      
      graph.addAnnotation(new Annotation("w2-11", "w1-11", "word", 
                                         graph.addAnchor(new Anchor("a11", 11.0)).getId(),
                                         graph.addAnchor(new Anchor("a12", 12.0)).getId(),
                                         "t2"));
      graph.addAnnotation(new Annotation("w2-12", "w1-12", "word", 
                                         "a12",
                                         graph.addAnchor(new Anchor("a13", 13.0)).getId(),
                                         "t2"));
      
      assertEquals("2 turns", 2, graph.list("turn").length);

      graph.trackChanges();
      UtteranceParallelizer parallelizer = new UtteranceParallelizer()
         .addLayerId("utterance").addLayerId("turn");
      try
      {
	 parallelizer.transform(graph);
         Set<Change> changes = graph.getTracker().getChanges();

	 assertNotEquals("changes: " + changes, 0, changes.size());

         // there are now four turns
         Annotation[] turns = graph.list("turn");
         // for (Annotation t : turns) System.out.println(t.getLabel() + " ("+t.getStart()+"-"+t.getEnd()+")");
	 assertEquals("now 4 turns: " + Arrays.asList(turns),
                      4, turns.length);

         // each p1 turn has three words and one utterance
         for (Annotation t : turns)
         {
            assertEquals("1 utterance " + t.getLabel() + " ("+t.getStart()+"-"+t.getEnd()+")",
                         1, t.list("utterance").length);
            if (t.getParentId().equals("p1"))
            {
               assertEquals("3 words " + t.getLabel() + " ("+t.getStart()+"-"+t.getEnd()+")",
                            3, t.list("word").length);
            }
            else
            {
               assertEquals("2 words " + t.getLabel() + " ("+t.getStart()+"-"+t.getEnd()+")",
                            2, t.list("word").length);
            }
         } // next turn
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
                      
   }

   @Test public void unsetOffsets() throws Exception
   {
      Schema schema = new Schema(
         "who", "turn", "utterance", "word",
         new Layer("who", "Participants")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(true).setPeersOverlap(true)
         .setSaturated(true),
	 new Layer("turn", "Speaker turns")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false)
         .setSaturated(false).setParentId("who"),
         new Layer("utterance", "Utterances")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false)
         .setSaturated(true).setParentId("turn"),
         new Layer("word", "Words")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false)
         .setSaturated(false).setParentId("turn"));
      
      // create a graph with simultaneous speech turns
      
      Graph graph = new Graph()
         .setId("simultaneous_speech")
         .setSchema(schema);
      graph.addAnchor(new Anchor("a0", 0.0));
      graph.addAnchor(new Anchor("a15", 15.0));
      // transcript attributes
      graph.addAnnotation(new Annotation("l", "en-NZ", "transcript_language", "a0", "a15"));
      graph.addAnnotation(new Annotation("by", "robert", "scribe", "a0", "a15"));
      graph.addAnnotation(new Annotation("v", "1", "version", "a0", "a15"));
      graph.addAnnotation(new Annotation("date", "20191211", "version_date", "a0", "a15"));
      // participants
      graph.addAnnotation(new Annotation("p1", "p1", "who", "a0", "a15"));
      graph.addAnnotation(new Annotation("p2", "p2", "who", "a0", "a15"));
      // participant attributes
      graph.addAnnotation(new Annotation("g1", "M", "gender", "a0", "a15", "p1"));
      graph.addAnnotation(new Annotation("g2", "F", "gender", "a0", "a15", "p2"));
      // turns
      graph.addAnnotation(new Annotation("t1", "p1", "turn", "a0", "a15", "p1"));
      // simultaneous non-shared anchors will be shared
      graph.addAnchor(new Anchor("a5", 5.0));
      graph.addAnchor(new Anchor("a5b", 5.0));
      graph.addAnchor(new Anchor("a10", 10.0));
      graph.addAnchor(new Anchor("a10b", 10.0));
      graph.addAnnotation(new Annotation("t2", "p2", "turn", "a5", "a10", "p2"));
      // utterances
      graph.addAnnotation(new Annotation("u1-1", "p1", "utterance", "a0", "a5", "t1"));
      graph.addAnnotation(new Annotation("u1-2", "p1", "utterance", "a5", "a10", "t1"));
      graph.addAnnotation(new Annotation("u2-1", "p2", "utterance", "a5b", "a10b", "t2"));
      graph.addAnnotation(new Annotation("u1-3", "p1", "utterance", "a10", "a15", "t1"));

      // words - with null intervening anchor offsets
      graph.addAnnotation(new Annotation("w1-1", "w1-1", "word", 
                                         graph.addAnchor(new Anchor("a1", 1.0)).getId(),
                                         graph.addAnchor(new Anchor("a2", null)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("w1-2", "w1-2", "word", 
                                         "a2",
                                         graph.addAnchor(new Anchor("a3", null)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("w1-3", "w1-3", "word", 
                                         "a3",
                                         graph.addAnchor(new Anchor("a4", 4.0)).getId(),
                                         "t1"));
      
      graph.addAnnotation(new Annotation("w1-6", "w1-6", "word", 
                                         graph.addAnchor(new Anchor("a6", 6.0)).getId(),
                                         graph.addAnchor(new Anchor("a7", null)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("w1-7", "w1-7", "word", 
                                         "a7", 
                                         graph.addAnchor(new Anchor("a8", null)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("w1-8", "w1-8", "word", 
                                         "a8",
                                         graph.addAnchor(new Anchor("a9", 9.0)).getId(),
                                         "t1"));
      

      graph.addAnnotation(new Annotation("w1-11", "w1-11", "word", 
                                         graph.addAnchor(new Anchor("a11", 11.0)).getId(),
                                         graph.addAnchor(new Anchor("a12", null)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("w1-12", "w1-12", "word", 
                                         "a12",
                                         graph.addAnchor(new Anchor("a13", null)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("w1-13", "w1-13", "word", 
                                         "a13",
                                         graph.addAnchor(new Anchor("a14", 14.0)).getId(),
                                         "t1"));
      
      graph.addAnnotation(new Annotation("w2-6.5", "w2-6.5", "word", 
                                         graph.addAnchor(new Anchor("a6.5", 6.5)).getId(),
                                         graph.addAnchor(new Anchor("a7.5", null)).getId(),
                                         "t2"));
      graph.addAnnotation(new Annotation("w2-7.5", "w2-7.5", "word", 
                                         "a7.5",
                                         graph.addAnchor(new Anchor("a8.5", 8.5)).getId(),
                                         "t2"));
      assertEquals("2 turns", 2, graph.list("turn").length);

      graph.trackChanges();
      
      UtteranceParallelizer parallelizer = new UtteranceParallelizer(schema);
      try
      {
	 parallelizer.transform(graph);
         Set<Change> changes = graph.getTracker().getChanges();

	 assertNotEquals("changes: " + changes, 0, changes.size());

         // there are now four turns
         Annotation[] turns = graph.list("turn");
         // for (Annotation t : turns) System.out.println(t.getLabel() + " ("+t.getStart()+"-"+t.getEnd()+")");
	 assertEquals("now 4 turns: " + Arrays.asList(turns),
                      4, turns.length);

         // each p1 turn has three words and one utterance
         for (Annotation t : turns)
         {
            assertEquals("1 utterance " + t.getLabel() + " ("+t.getStart()+"-"+t.getEnd()+")",
                         1, t.list("utterance").length);
            if (t.getParentId().equals("p1"))
            {
               assertEquals("3 words " + t.getLabel() + " ("+t.getStart()+"-"+t.getEnd()+") "
                            + t.getAnnotations("word"),
                            3, t.list("word").length);
            }
            else
            {
               assertEquals("2 words " + t.getLabel() + " ("+t.getStart()+"-"+t.getEnd()+")",
                            2, t.list("word").length);
            }
         } // next turn

         // anchor sharing
         assertEquals("simultaneous non-shared start anchors are now shared",
                      graph.getAnnotation("u1-2").getStartId(),
                      graph.getAnnotation("u2-1").getStartId());
         assertEquals("simultaneous non-shared end anchors are now shared",
                      graph.getAnnotation("u1-2").getEndId(),
                      graph.getAnnotation("u2-1").getEndId());
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
                      
   }
   
   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.util.test.TestUtteranceParallelizer");
   }
}
