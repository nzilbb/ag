//
// Copyright 2015-2020 New Zealand Institute of Language, Brain and Behaviour, 
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
import org.junit.runners.MethodSorters;
import org.junit.FixMethodOrder;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;
import nzilbb.ag.*;
import nzilbb.ag.serialize.SerializationException;
import nzilbb.ag.serialize.SerializationParametersMissingException;
import nzilbb.ag.serialize.SerializerNotConfiguredException;
import nzilbb.ag.serialize.json.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.util.*;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.editpath.EditStep;
import nzilbb.editpath.MinimumEditPath;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestMerger
{      
  @Test public void identityMerge() 
    throws Exception
  {
    Schema schema = defaultSchema();

    File f = new File(getDir(), "identity.json");
      
    Graph originalGraph = loadGraphFromJSON(f, schema);

    originalGraph.setTracker(new ChangeTracker());
    Merger m = new Merger();

    try
    {
      Vector<Change> changes = m.transform(originalGraph);
      fail("Doesn't throw exception when editedGraph is unset: " + changes);
    }
    catch(TransformationException exception)
    {  // TransformationException should be thrown
    }
      
    m.setEditedGraph(loadGraphFromJSON(f, schema));
    //m.setDebug(true);

    try
    {
      Vector<Change> changes = m.transform(originalGraph);
      if (m.getLog() != null) for (String message : m.getLog()) System.out.println(message);
      assertEquals("No changes - " + changes, 0, changes.size());
    }
    catch(TransformationException exception)
    {
      fail(" " + exception.toString());
    }

  }

  @Test public void identityMergeFragment() 
    throws Exception
  {
    Schema schema = defaultSchema();

    File f = new File(getDir(), "identity__10.000-70.000.json");
      
    Merger m = new Merger(loadGraphFromJSON(f, schema));
    //m.setDebug(true);

    Graph originalGraph = loadGraphFromJSON(f, schema);
    originalGraph.setTracker(new ChangeTracker());

    try
    {
      Vector<Change> changes = m.transform(originalGraph);
      if (m.getLog() != null) for (String message : m.getLog()) System.out.println(message);
      assertEquals("No changes - " + changes, 0, changes.size());
    }
    catch(TransformationException exception)
    {
      fail(" " + exception.toString());
    }

  }

  @Test public void selectiveMerge()
  {
    Graph g = new Graph();
    g.setSchema(new Schema(
                  "who", "turn", "utterance", "word",
                  new Layer("who", "participants", Constants.ALIGNMENT_NONE, 
                            true, // peers
                            true, // peersOverlap
                            true), // saturated
                  new Layer("turn", "turns", Constants.ALIGNMENT_INTERVAL,
                            true, // peers
                            false, // peersOverlap
                            false, // saturated
                            "who", // parentId
                            true), // parentIncludes
                  new Layer("utterance", "utterances", Constants.ALIGNMENT_INTERVAL,
                            true, // peers
                            false, // peersOverlap
                            true, // saturated
                            "turn", // parentId
                            true), // parentIncludes
                  new Layer("phrase", "phrase", Constants.ALIGNMENT_INTERVAL,
                            true, // peers
                            true, // peersOverlap
                            false, // saturated
                            "turn", // parentId
                            true), // parentIncludes
                  new Layer("word", "Words", Constants.ALIGNMENT_INTERVAL,
                            true, // peers
                            false, // peersOverlap
                            false, // saturated
                            "turn", // parentId
                            true), // parentIncludes
                  new Layer("pos", "Part of speech", Constants.ALIGNMENT_NONE,
                            false, // peers
                            false, // peersOverlap
                            true, // saturated
                            "word", // parentId
                            true), // parentIncludes
                  new Layer("phone", "segments", Constants.ALIGNMENT_INTERVAL,
                            true, // peers
                            false, // peersOverlap
                            true, // saturated
                            "word", // parentId
                            true) // parentIncludes
                  ));
    g.setId("my graph");

    g.addAnchor(new Anchor("turnStart", 0.0));
    g.addAnchor(new Anchor("a1", 0.0));
    g.addAnchor(new Anchor("a2", null));
    g.addAnchor(new Anchor("a3a", 3.0));
    g.addAnchor(new Anchor("utterance", 3.0));
    g.addAnchor(new Anchor("a3b", 3.0));
    g.addAnchor(new Anchor("a4", null));
    g.addAnchor(new Anchor("a5", 6.0));
    g.addAnchor(new Anchor("turnEnd", 6.0));

    g.addAnnotation(new Annotation("who1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
    g.addAnnotation(new Annotation("who2", "jane doe", "who", "turnStart", "turnEnd", "my graph"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "who1"));

    g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utterance", "turn1"));
    g.addAnnotation(new Annotation("utterance2", "john smith", "utterance", "utterance", "turnEnd", "turn1"));

    g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("pos1", "DT", "pos", "a1", "a2", "word1"));
    g.addAnnotation(new Annotation("word2", "quick", "word", "a2", "a3a", "turn1"));
    g.addAnnotation(new Annotation("pos2", "A", "pos", "a2", "a3a", "word2"));
    g.addAnnotation(new Annotation("word3", "brown", "word", "a3b", "a4", "turn1"));
    g.addAnnotation(new Annotation("phrase1", "AP", "phrase", "a2", "a4", "turn1"));
    g.addAnnotation(new Annotation("word4", "fox", "word", "a4", "a5", "turn1"));
    g.addAnnotation(new Annotation("pos3", "N", "pos", "a4", "a5", "word4"));
    g.addAnnotation(new Annotation("phrase2", "NP", "phrase", "a1", "a5", "turn1"));
    // no phones

    Graph e = new Graph();
    e.setSchema(new Schema(
                  "who", "turn", "utterance", "word",
                  new Layer("who", "participants", Constants.ALIGNMENT_NONE, 
                            true, // peers
                            true, // peersOverlap
                            true), // saturated
                  new Layer("turn", "turns", Constants.ALIGNMENT_INTERVAL,
                            true, // peers
                            false, // peersOverlap
                            false, // saturated
                            "who", // parentId
                            true), // parentIncludes
                  new Layer("utterance", "utterances", Constants.ALIGNMENT_INTERVAL,
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
                  new Layer("phone", "segments", Constants.ALIGNMENT_INTERVAL,
                            true, // peers
                            false, // peersOverlap
                            true, // saturated
                            "word", // parentId
                            true) // parentIncludes
                  ));
    e.setId("my graph");
      
    e.addAnchor(new Anchor("turnStart", 0.0));
    e.addAnchor(new Anchor("a1", 1.0));
    e.addAnchor(new Anchor("a1.5", 1.5));
    e.addAnchor(new Anchor("a2", 2.0));
    e.addAnchor(new Anchor("a2.25", 2.25));
    e.addAnchor(new Anchor("a2.5", 2.5));
    e.addAnchor(new Anchor("a2.75", 2.75));
    e.addAnchor(new Anchor("a3a", 3.0));
    e.addAnchor(new Anchor("utterance", 3.0));
    e.addAnchor(new Anchor("a3b", 3.0));
    e.addAnchor(new Anchor("a4", 4.0));
    e.addAnchor(new Anchor("a5", 5.0));
    e.addAnchor(new Anchor("a6", 6.0));
    e.addAnchor(new Anchor("turnEnd", 6.0));

    e.addAnnotation(new Annotation("who1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
    // no who2

    e.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "who1"));

    e.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utterance", "turn1"));
    e.addAnnotation(new Annotation("utterance2", "john smith", "utterance", "utterance", "turnEnd", "turn1"));

    e.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
    e.addAnnotation(new Annotation("phone1", "D", "phone", "a1", "a1.5", "word1"));
    e.addAnnotation(new Annotation("phone2", "@", "phone", "a1.5", "a2", "word1"));
    // quick -> fast
    e.addAnnotation(new Annotation("word2", "fast", "word", "a2", "a3a", "turn1"));
    e.addAnnotation(new Annotation("phone3", "k", "phone", "a2", "a2.25", "word2"));
    e.addAnnotation(new Annotation("phone4", "w", "phone", "a2.25", "a2.5", "word2"));
    e.addAnnotation(new Annotation("phone5", "I", "phone", "a2.5", "a2.75", "word2"));
    e.addAnnotation(new Annotation("phone6", "k", "phone", "a2.75", "a3a", "word2"));
    // brown deleted
    e.addAnnotation(new Annotation("word4", "fox", "word", "a4", "a5", "turn1"));
    e.addAnnotation(new Annotation("word5", "ah", "word", "a5", "a6", "turn1"));
    e.addAnnotation(new Annotation("phrase2", "NP", "phrase", "a1", "a5", "turn1"));
    // no pos nor phrase
    
    g.setTracker(new ChangeTracker());

    Merger m = new Merger(e);
    // m.setDebug(true);
    m.getNoChangeLayers().add("who");
    m.getNoChangeLayers().add("word");
    try
    {
      Vector<Change> changes = m.transform(g);
      if (m.getLog() != null) for (String message : m.getLog()) System.out.println(message);
      g.commit();

      Annotation[] phones = g.list("phone");
      assertEquals("phones have been added - " + Arrays.asList(phones), 6, phones.length);
      assertEquals("D", phones[0].getLabel());
      assertEquals("@", phones[1].getLabel());
      assertEquals("k", phones[2].getLabel());
      assertEquals("w", phones[3].getLabel());
      assertEquals("I", phones[4].getLabel());
      assertEquals("k", phones[5].getLabel());

      Annotation[] words = g.list("word");
      assertEquals("words are unchanged - " + Arrays.asList(words), 4, words.length);
      assertEquals("the", words[0].getLabel());
      assertEquals("word not changed", "quick", words[1].getLabel());
      assertEquals("brown", words[2].getLabel());
      assertEquals("fox", words[3].getLabel());

      assertTrue("speakers are unchanged", g.getAnnotationsById().containsKey("who1"));
      assertTrue("speakers are unchanged", g.getAnnotationsById().containsKey("who2"));

      assertEquals("word alignments updated", Double.valueOf(1.0), words[0].getStart().getOffset());
      assertEquals("word alignments updated", Double.valueOf(2.0), words[0].getEnd().getOffset());
      assertEquals("word alignments updated", Double.valueOf(2.0), words[1].getStart().getOffset());
      assertEquals("word alignments updated", Double.valueOf(3.0), words[1].getEnd().getOffset());
      assertEquals("word alignments updated", Double.valueOf(3.0), words[2].getStart().getOffset());
      assertEquals("word alignments updated", Double.valueOf(4.0), words[2].getEnd().getOffset());
      assertEquals("word alignments updated", Double.valueOf(4.0), words[3].getStart().getOffset());
      assertEquals("word alignments updated", Double.valueOf(5.0), words[3].getEnd().getOffset());

    }
    catch(TransformationException exception)
    {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      exception.printStackTrace(pw);
      try { sw.close(); }
      catch(IOException x) {}
      pw.close();	
      fail("merge() failed" + exception.toString() + "\n" + sw);
    }
  }

  @Test public void extractedFragmentMerge()
  {
    Graph g = new Graph();
    g.setSchema(new Schema(
                  "who", "turn", "utterance", "word",
                  new Layer("who", "participants")
                  .setAlignment(Constants.ALIGNMENT_NONE) 
                  .setPeers(true).setPeersOverlap(true).setSaturated(true),
                  new Layer("turn", "turns")
                  .setAlignment(Constants.ALIGNMENT_INTERVAL)
                  .setPeers(true).setPeersOverlap(false).setSaturated(false)
                  .setParentId("who").setParentIncludes(true),
                  new Layer("utterance", "utterances")
                  .setAlignment(Constants.ALIGNMENT_INTERVAL)
                  .setPeers(true).setPeersOverlap(false).setSaturated(true)
                  .setParentId("turn").setParentIncludes(true),
                  new Layer("phrase", "phrase")
                  .setAlignment(Constants.ALIGNMENT_INTERVAL)
                  .setPeers(true).setPeersOverlap(true).setSaturated(false)
                  .setParentId("turn").setParentIncludes(true),
                  new Layer("word", "Words")
                  .setAlignment(Constants.ALIGNMENT_INTERVAL)
                  .setPeers(true).setPeersOverlap(false).setSaturated(false)
                  .setParentId("turn").setParentIncludes(true),
                  new Layer("pos", "Part of speech")
                  .setAlignment(Constants.ALIGNMENT_NONE)
                  .setPeers(false).setPeersOverlap(false).setSaturated(true)
                  .setParentId("word").setParentIncludes(true),
                  new Layer("phone", "segments")
                  .setAlignment(Constants.ALIGNMENT_INTERVAL)
                  .setPeers(true).setPeersOverlap(false).setSaturated(true)
                  .setParentId("word").setParentIncludes(true)
                  ));
    g.setId("my graph");

    g.addAnchor(new Anchor("turnStart", 0.0));
    g.addAnchor(new Anchor("a1", 0.0));
    g.addAnchor(new Anchor("a2", null));
    g.addAnchor(new Anchor("a3a", 3.0));
    g.addAnchor(new Anchor("utterance", 3.0));
    g.addAnchor(new Anchor("a3b", 3.0));
    g.addAnchor(new Anchor("a4", 3.1));
    g.addAnchor(new Anchor("a5", 6.0));
    g.addAnchor(new Anchor("turnEnd", 6.0));

    g.addAnnotation(new Annotation("who1", "john smith", "who", "turnStart", "turnEnd", "my graph"));
    g.addAnnotation(new Annotation("who2", "jane doe", "who", "turnStart", "turnEnd", "my graph"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "who1"));

    g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utterance", "turn1"));
    g.addAnnotation(new Annotation("utterance2", "john smith", "utterance", "utterance", "turnEnd", "turn1"));

    g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("pos1", "DT", "pos", "a1", "a2", "word1"));
    g.addAnnotation(new Annotation("word2", "quick", "word", "a2", "a3a", "turn1"));
    g.addAnnotation(new Annotation("pos2", "A", "pos", "a2", "a3a", "word2"));
    g.addAnnotation(new Annotation("word3", "brown", "word", "a3b", "a4", "turn1"));
    g.addAnnotation(new Annotation("phrase1", "AP", "phrase", "a2", "a4", "turn1"));
    g.addAnnotation(new Annotation("word4", "fox", "word", "a4", "a5", "turn1"));
    g.addAnnotation(new Annotation("pos3", "N", "pos", "a4", "a5", "word4"));
    g.addAnnotation(new Annotation("phrase2", "NP", "phrase", "a1", "a5", "turn1"));
    // no phones

    // now we have a whole graph, we're going to extract a fragment of it.
    // "utterance2" is used to ensure that ordinals don't start from 1
    String[] layers = g.getSchema().getLayers().keySet().toArray(new String[0]);
    Graph originalFragment = g.getFragment(g.getAnnotation("utterance2"), layers);

    // the resulting graphs includes annotations that reference anchors that aren't in the graph
    assertEquals("Turn start ID in fragment",
                 "turnStart", originalFragment.getAnnotation("turn1").getStartId());
    assertNull("Turn start anchor not in fragment",
               originalFragment.getAnchor("turnStart"));
    assertNull("Turn start unavailable",
               originalFragment.getAnnotation("turn1").getStart());
    assertEquals("Turn end ID in fragment",
                 "turnEnd", originalFragment.getAnnotation("turn1").getEndId());
    assertNotNull("Turn end is in fragment",
                  originalFragment.getAnnotation("turn1").getEnd());

    // and includes layers with ordinals that don't start from 1
    assertEquals("Utterance ordinal in fragmant",
                 2, originalFragment.getAnnotation("utterance2").getOrdinal());
    assertEquals("Word 3 ordinal in fragmant",
                 3, originalFragment.getAnnotation("word3").getOrdinal());
    assertEquals("Word 4 ordinal in fragmant",
                 4, originalFragment.getAnnotation("word4").getOrdinal());

    // test initial words state we'll test later...
    Annotation[] words = originalFragment.list("word");
    assertEquals("words - " + Arrays.asList(words), 2, words.length);
    assertEquals("brown", words[0].getLabel());
    assertEquals("fox", words[1].getLabel());

    assertEquals("word 3 start", Double.valueOf(3.0), words[0].getStart().getOffset());
    assertEquals("word 3 end", Double.valueOf(3.1), words[0].getEnd().getOffset());
    assertEquals("word 4 start", Double.valueOf(3.1), words[1].getStart().getOffset());
    assertEquals("word 4 end", Double.valueOf(6.0), words[1].getEnd().getOffset());
    
    // construct an edited version of the fragment, with flattened schema
    Graph editedFragment = new Graph();
    editedFragment.setSchema(new Schema(
                               "who", "turn", "utterance", "word",
                               new Layer("utterance", "utterances")
                               .setAlignment(Constants.ALIGNMENT_INTERVAL)
                               .setPeers(false).setPeersOverlap(false).setSaturated(true),
                               new Layer("word", "Words")
                               .setAlignment(Constants.ALIGNMENT_INTERVAL)
                               .setPeers(true).setPeersOverlap(false).setSaturated(false),
                               new Layer("phone", "segments")
                               .setAlignment(Constants.ALIGNMENT_INTERVAL)
                               .setPeers(true).setPeersOverlap(false).setSaturated(true)
                               .setParentId("word").setParentIncludes(true)
                               ));
    editedFragment.setId("my edited fragment");

    editedFragment.addAnchor(new Anchor("utterance", 3.0));
    editedFragment.addAnchor(new Anchor("a3b", 3.0));
    editedFragment.addAnchor(new Anchor("a4", 4.0)); // has offset
    editedFragment.addAnchor(new Anchor("a5", 5.0)); // different offset
    editedFragment.addAnchor(new Anchor("turnEnd", 6.0));

    editedFragment.addAnnotation(
      new Annotation(
        "utterance2", "john smith", "utterance", "utterance", "turnEnd"));
    // words have uppercase labels now
    editedFragment.addAnnotation(
      new Annotation("word3", "BROWN", "word", "a3b", "a4"));
    editedFragment.addAnnotation(
      new Annotation("word4", "FOX", "word", "a4", "a5"));

    originalFragment.setTracker(new ChangeTracker());
    
    Merger m = new Merger(editedFragment);
    // m.setDebug(true);
    try
    {
      Vector<Change> changes = m.transform(originalFragment);
      if (m.getLog() != null) for (String message : m.getLog()) System.out.println(message);
      g.commit();

      words = originalFragment.list("word");
      assertEquals("words are unchanged - " + Arrays.asList(words), 2, words.length);
      assertEquals("BROWN", words[0].getLabel());
      assertEquals("FOX", words[1].getLabel());

      assertEquals("word 3 start unchanged", Double.valueOf(3.0), words[0].getStart().getOffset());
      assertEquals("word 3 end changed", Double.valueOf(4.0), words[0].getEnd().getOffset());
      assertEquals("word 4 start changed", Double.valueOf(4.0), words[1].getStart().getOffset());
      assertEquals("word 4 end changed", Double.valueOf(5.0), words[1].getEnd().getOffset());

      // turn is unchanged
      assertEquals("Turn start ID in fragment",
                   "turnStart", originalFragment.getAnnotation("turn1").getStartId());
      assertNull("Turn start not in fragment",
                 originalFragment.getAnnotation("turn1").getStart());
      assertEquals("Turn end ID in fragment",
                 "turnEnd", originalFragment.getAnnotation("turn1").getEndId());
      assertNotNull("Turn end is in fragment",
                 originalFragment.getAnnotation("turn1").getEnd());

      // ordinals are unchanged
      assertEquals("Utterance ordinal in fragmant",
                   2, originalFragment.getAnnotation("utterance2").getOrdinal());
      assertEquals("Word 3 ordinal in fragmant",
                   3, originalFragment.getAnnotation("word3").getOrdinal());
      assertEquals("Word 4 ordinal in fragmant",
                   4, originalFragment.getAnnotation("word4").getOrdinal());
      
    }
    catch(TransformationException exception)
    {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      exception.printStackTrace(pw);
      try { sw.close(); }
      catch(IOException x) {}
      pw.close();	
      fail("merge() failed" + exception.toString() + "\n" + sw);
    }
  }

  /**
   * "Basic" edit operations that (mostly) affect a single layer.
   * <ol>
   *  <li>Label changes, both below and above original status</li>
   *  <li>Annotation insertion</li>
   *  <li>Annotation deletion</li>
   *  <li>Anchor offset changes, both below and above original status</li>
   *  <li>Connected graphs that become disconnected, on the same layer</li>
   *  <li>Disconnected graphs that become connected, on the same layer</li>
   *  <li>Annotation transposition</li>
   *  <li>Merge graphs with partial hierarchy - e.g. an utterance - and check for correct ordinals</li>
   * </ol>
   * Then "Rel" tests: edits that have inter-layer consequences.
   * <ol>
   *  <li>Parents - for saturated relationships, child anchors trump parent anchors</li>
   *  <li>Label changes, with unaligned children</li>
   *  <li>Label changes, with aligned children (to ensure they don't get given default alignments)</li>
   *  <li>New annotations that share anchors with old annotations on another layer
   *      - e.g. insert a word at the beginning of a language block </li>
   *  <li>Peers - anchor changes on annotations are reflected in their unaligned peer tags, when
   *      the peer layer <i>isn't</i> present in the edited fragment</li>
   *  <li>Peers - anchor changes on annotations are reflected in their unaligned peer tags, when
   *      the peer layer <i>is</i> present in the edited fragment</li>
   * </ol>
   * Then "Align" tests: edits that have more complicated possible consequences.
   * <ol>
   *  <li>New annotations that share anchors with old annotations on another layer,
   *      where new anchors are lower status than old ones
   *      - e.g. insert a word at the beginning of a language block </li>
   *  <li>Annotation insertion - works if the low-status new
   *  offsets for surrounding anchors are a long way away from their
   *  high-status originals 
   *  (i.e. if a manually aligned utterance
   *   has words clustered at the beginning,
   *   and a new word is inserted as the second-to-last word,
   *   and the edited annotations have default alignments,
   *  it comes out in the wash) </li>
   * </ol>
   * Then "Htk" tests: edits that simulate HTK
   * <ol>
   *  <li>simple alignment with no segments</li>
   *  <li>simple alignment with no segments, but will parallel annotations on other layers</li>
   *  <li>simple alignment with prior default segments</li>
   *  <li>simple re-alignment with prior aligned segments</li>
   *  <li>re-alignment with different segments - add/change/delete</li>
   *  <li>re-alignment avoiding manual alignments</li>
   *  <li>re-alignment avoiding manual alignments, where differences are big enough to create 
   *      an invalid graph.
   *      e.g. 
   *      <ul>
   *         <li><tt>original: .1.2.            !3!4!5.6.</tt> (manually aligned .4.5.6.)</li>
   *         <li><tt>edited:   . .1.2.3.4. .5.6.         </tt> (order would be changed</li>
   *         <li><tt>result:     .1.2.          !3!4!5.6.</tt> (keep order)</li>
   *      <li><tt>not:<s>        .1.2.     .5.6.!3!4!    </s></tt> (keep alignments)</li>
   *      </ul>
   *  </li>
   *  <li>also test conflicting alignments in the other direction.
   *      e.g.
   *      <ul>
   *         <li><tt>original: .1.2.!3!4!            .5.6.</tt> (manually aligned .4.5.6.)</li>
   *         <li><tt>edited:   .          .1.2.3.4. .5.6. </tt> (order would be changed</li>
   *         <li><tt>result:   .1.2.!3!4!           .5.6. </tt> (keep order)</li>
   *      <li><tt>not:<s>           !3!4! .1.2.     .5.6. </s></tt> (keep alignments)</li>
   *      </ul>
   * </li>
   * </ol>
   * Then "Reupload" tests: edits that simulate re-upload
   * <ol>
   *  <li>unaligned word edits to aligned words</li>
   *  <li>unaligned word edits to aligned words with aligned segments</li>
   *  <li>unaligned word/language edits to aligned words with aligned segments</li>
   *  <li>move unaligned word to another utterance, with previously aligned words</li>
   *  <li>change utterance alignments, with previously aligned words, to create mid-word utterance boundary</li>
   *  <li>merge utterances</li>
   *  <li>split utterances</li>
   * </ol>
   * Then test merge of graphs with mismatched granularities, to simulate exporting to TextGrid and then re-importing.
   */
  @Test public void fragmentTests()
  {
    tests("frag", null);
  }

  /**
   * Test edits that simulate re-upload
   * <ol>
   *  <li>Change turn alignments, with previously aligned words, to create mid-word turn boundary</li>
   *  <li>Changing parents - move word to neighbouring turn with different speaker</li>
   *  <li>Peers - parent changes on annotations are reflected in aligned and unaligned peers</li>
   *  <li>Changing parents - move unaligned word to another turn, with previously aligned words</li>
   *  <li>Changing parents - move unaligned word to a simultaneous turn</li>
   *  <li>Changing parents - merge two turns (delete intervening other-speaker turn)</li>
   *  <li>Changing parents - split a turn in two (insert intervening other-speaker turn)</li>
   * </ol>
   */
  @Test public void graphTests()
  {
    tests("graph", null);
  }

  /**
   * Standardised method for running graph fragment tests based on files in the test directory.
   * @param sDir Subdirectory name
   * @param log null, or a filename substring like "001" to identify a test for which to switch on debug logging.
   */ 
  public void tests(String sDir, String log)
  {
    // get a sorted list of tests
    File dir = getDir();
    File subdir = new File(dir, sDir);
    File[] afTests = subdir.listFiles(new FilenameFilter()
      {
        public boolean accept(File dir, String name)
        {
          return name.matches("^\\d+-.*\\.json$");
        }
      });
    TreeSet<String> fragments = new TreeSet<String>();
    for (File fTest : afTests) fragments.add(fTest.getName().replace(".json",""));
    // run the tests
    for (String fragmentName : fragments)
    {
      System.out.println("Test "+sDir+": " + fragmentName);
      Schema schema = defaultSchema();
      File fOriginal = new File(subdir, fragmentName + ".json");
      try
      {
        Graph originalGraph = loadGraphFromJSON(fOriginal, schema);
        originalGraph.trackChanges();
        File fEdited = new File(subdir, "edited_" + fragmentName + ".json");
        Graph editedGraph = loadGraphFromJSON(fEdited, schema);
        Merger m = new Merger(editedGraph);
        m.setDebug(log != null && fragmentName.indexOf(log) >= 0);
        try
        {
          Vector<Change> changes = m.transform(originalGraph);
        }
        catch(TransformationException exception)
        {
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          exception.printStackTrace(pw);
          try { sw.close(); }
          catch(IOException x) {}
          pw.close();	
          fail(fragmentName + ": merge() failed" + exception.toString() + "\n" + sw);
        }
        if (m.getLog() != null) for (String message : m.getLog()) System.out.println(message);
        for (String message : m.getErrors()) System.out.println("ERROR: " + message);

        // destroy any unreferenced anchors
        for (Anchor a : new Vector<Anchor>(originalGraph.getAnchors().values()))
        {
          // we should just be able to check that the size of the collections is zero
          // but there may be disconnect between tag layer anchor Id attributes and 
          // the parents' anchors
          boolean destroy = true;
          for (Annotation an : a.getStartingAnnotations())
          {
            if (an.getChange() != Change.Operation.Destroy)
            {
              destroy = false;
              break;
            }
          }
          for (Annotation an : a.getEndingAnnotations())
          {
            if (an.getChange() != Change.Operation.Destroy)
            {
              destroy = false;
              break;
            }
          }
          if (destroy) a.destroy();
        } // next anchor
        
        originalGraph.commit();

        // save the actual result
        File fActual = new File(subdir, "final_" + fragmentName + ".json");
        saveGraphToJSON(fActual, originalGraph);
	    
        // compare with what we expected
        Vector<String> actualLines = new Vector<String>();
        BufferedReader reader = new BufferedReader(new FileReader(fActual));
        String line = reader.readLine();
        while (line != null)
        {
          actualLines.add(line);
          line = reader.readLine();
        }
        File fExpected = new File(subdir, "expected_" + fragmentName + ".json");
        Vector<String> expectedLines = new Vector<String>();
        reader = new BufferedReader(new FileReader(fExpected));
        line = reader.readLine();
        while (line != null)
        {
          expectedLines.add(line);
          line = reader.readLine();
        }
        MinimumEditPath<String> comparator = new MinimumEditPath<String>();
        List<EditStep<String>> path = comparator.minimumEditPath(expectedLines, actualLines);
        String differences = "";
        for (EditStep<String> step : path)
        {
          switch (step.getOperation())
          {
            case CHANGE:
              differences += "\n"+fExpected.getPath()+":"+(step.getFromIndex()+1)+": Expected:\n" 
                + step.getFrom() 
                + "\n"+fActual.getPath()+":"+(step.getToIndex()+1)+": Found:\n" + step.getTo();
              break;
            case DELETE:
              differences += "\n"+fExpected.getPath()+":"+(step.getFromIndex()+1)+": Deleted:\n" 
                + step.getFrom()
                + "\n"+fActual.getPath()+":"+(step.getToIndex()+1)+": Missing";
              break;
            case INSERT:
              differences += "\n"+fExpected.getPath()+":"+(step.getFromIndex()+1)+": Missing" 
                + "\n"+fActual.getPath()+":"+(step.getToIndex()+1)+": Inserted:\n" 
                + step.getTo();
              break;
          }
        } // next step
        if (differences.length() > 0) fail(differences);	 
        if (m.getErrors().size() > 0) fail(m.getErrors().toString());

        fActual.delete();
      }
      catch(Exception exception)
      {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        try { sw.close(); }
        catch(IOException x) {}
        pw.close();	
        fail(fragmentName + ": failed" + exception.toString() + "\n" + sw);
      }
    } // next test
  }
   
  /**
   * Returns the default schema for testing.
   * @return A test schema that matches the test files.
   */
  public Schema defaultSchema()
  {
    return new Schema(
      "who", "turn", "utterance", "word",
      new Layer("topic", "topic", Constants.ALIGNMENT_INTERVAL, 
                true, // peers
                false, // peersOverlap
                false), // saturated
      new Layer("who", "participants", Constants.ALIGNMENT_NONE, 
                true, // peers
                true, // peersOverlap
                true), // saturated
      new Layer("turns", "turns", Constants.ALIGNMENT_INTERVAL,
                true, // peers
                false, // peersOverlap
                false, // saturated
                "who", // parentId
                true), // parentIncludes
      new Layer("utterances", "utterances", Constants.ALIGNMENT_INTERVAL,
                true, // peers
                false, // peersOverlap
                true, // saturated
                "turn", // parentId
                true), // parentIncludes
      new Layer("language", "language", Constants.ALIGNMENT_INTERVAL,
                true, // peers
                false, // peersOverlap
                false, // saturated
                "turn", // parentId
                true), // parentIncludes
      new Layer("transcript", "Words", Constants.ALIGNMENT_INTERVAL,
                true, // peers
                false, // peersOverlap
                false, // saturated
                "turn", // parentId
                true), // parentIncludes
      new Layer("pos", "Part of speech", Constants.ALIGNMENT_NONE,
                false, // peers
                false, // peersOverlap
                true, // saturated
                "word", // parentId
                true), // parentIncludes
      new Layer("segments", "segments", Constants.ALIGNMENT_INTERVAL,
                true, // peers
                false, // peersOverlap
                true, // saturated
                "word", // parentId
                true) // parentIncludes
      );
  } // end of defaultSchema()

  /**
   * Directory for text files.
   * @see #getDir()
   * @see #setDir(File)
   */
  protected File fDir;
  /**
   * Getter for {@link #fDir}: Directory for text files.
   * @return Directory for text files.
   */
  public File getDir() 
  { 
    if (fDir == null)
    {
      try
      {
        URL urlThisClass = getClass().getResource(getClass().getSimpleName() + ".class");
        File fThisClass = new File(urlThisClass.toURI());
        fDir = fThisClass.getParentFile();
      }
      catch(Throwable t)
      {
        System.out.println("" + t);
      }
    }
    return fDir; 
  }
  /**
   * Setter for {@link #fDir}: Directory for text files.
   * @param fNewDir Directory for text files.
   */
  public void setDir(File fNewDir) { fDir = fNewDir; }
   
  /**
   * Loads an annotation graph from a JSON file.
   * @param file The JSON file.
   * @param schema The schema to use.
   * @return The annotation graph represented by the file.
   * @throws SerializationException If the graph could not be loaded.
   * @throws SerializationParametersMissingException
   * @throws SerializerNotConfiguredException
   * @throws IOException On IO error.
   * @throws FileNotFoundException If <var>file</var> doesn't exist.
   */
  public Graph loadGraphFromJSON(File file, Schema schema)
    throws FileNotFoundException, IOException, SerializationException, SerializationParametersMissingException, SerializerNotConfiguredException
  {
    // create deserializer
    JSONSerialization s = new JSONSerialization();
    // configure it with its default options
    s.configure(s.configure(new ParameterSet(), schema), schema);      
    // load file
    ParameterSet parameters = s.load(Utility.OneNamedStreamArray(new NamedStream(file)), schema);
    // use default deserialization parameters
    s.setParameters(parameters); // run with default values
    // deserialize      
    return s.deserialize()[0];
  } // end of loadGraphFromJSON()

  /**
   * Loads an annotation graph from a JSON file.
   * @param file The JSON file.
   * @param graph The graph to save.
   * @return The annotation graph represented by the file.
   * @throws SerializationException If the graph could not be loaded.
   * @throws SerializerNotConfiguredException
   * @throws IOException On IO error.
   */
  public void saveGraphToJSON(File file, Graph graph)
    throws IOException, SerializationException, SerializerNotConfiguredException
  {
    // create deserializer
    JSONSerialization s = new JSONSerialization();
    s.setSortAnchors(true);
    // configure it with its default options
    s.configure(s.configure(new ParameterSet(), graph.getSchema()), graph.getSchema());
    // serialize      
    final Vector<SerializationException> exceptions = new Vector<SerializationException>();
    final Vector<NamedStream> streams = new Vector<NamedStream>();
    s.serialize(Utility.OneGraphSpliterator(graph), null,
                (stream) -> streams.add(stream),
                (warning) -> System.out.println(warning),
                (exception) -> exceptions.add(exception));
    streams.elementAt(0).setName(file.getName());
    streams.elementAt(0).save(file.getParentFile());

  } // end of loadGraphFromJSON()

  public static void main(String args[]) 
  {
    org.junit.runner.JUnitCore.main("nzilbb.ag.util.test.TestMerger");
  }
}
