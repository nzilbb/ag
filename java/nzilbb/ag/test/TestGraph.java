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

package nzilbb.ag.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.LinkedHashSet;
import java.util.Iterator;
import nzilbb.ag.*;

public class TestGraph
{
   @Test public void basicAttributes() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      assertEquals("my graph", g.getId());
      assertEquals("my graph", g.getLabel());
      assertEquals("graph's graph is itself", g, g.getGraph());
      assertEquals("source graph is itself", g, g.sourceGraph());
   }

   @Test public void extendedAttributes() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      g.put("foo", "bar");
      assertEquals("bar", g.get("foo"));
   }

   @SuppressWarnings("unchecked")
   @Test public void basicObjectInterrelation() 
   {
      Graph g = new Graph();
      g.setId("my graph");

      Layer who = new Layer("who", "Speaker", 0, true, true, false);
      g.addLayer(who);

      Layer turn = new Layer("turn", "Speaker turns", 2, true, true, false, "who", true);
      g.addLayer(turn);

      Layer word = new Layer("word", "Words", 2, true, false, false, "turn", true);
      g.addLayer(word);

      assertEquals(turn, g.getLayer("turn"));
      assertEquals(word, g.getLayer("word"));

      Anchor turnStart = new Anchor("turnStart", 1.0);
      Anchor a1 = new Anchor("a1", 1.0);
      Anchor a2 = new Anchor("a2", 2.0);
      Anchor a3 = new Anchor("a3", 3.0);
      Anchor a4 = new Anchor("a4", 4.0);
      Anchor a5 = new Anchor("a5", 5.0);
      Anchor turnEnd = new Anchor("turnEnd", 5.0);

      g.addAnchor(a1);
      g.addAnchor(turnStart);
      g.addAnchor(a2);
      g.addAnchor(a3);
      g.addAnchor(a4);
      g.addAnchor(turnEnd);
      g.addAnchor(a5);

      assertEquals(turnStart, g.getAnchor("turnStart"));
      assertEquals(a1, g.getAnchor("a1"));
      assertEquals(a2, g.getAnchor("a2"));
      assertEquals(a3, g.getAnchor("a3"));
      assertEquals(a4, g.getAnchor("a4"));
      assertEquals(a5, g.getAnchor("a5"));
      assertEquals(turnEnd, g.getAnchor("turnEnd"));

      assertEquals(g, turnStart.getGraph());
      assertEquals(g, a1.getGraph());
      assertEquals(g, a2.getGraph());
      assertEquals(g, a3.getGraph());
      assertEquals(g, a4.getGraph());
      assertEquals(g, a5.getGraph());
      assertEquals(g, turnEnd.getGraph());

      Annotation who1 = new Annotation("who1", "john smith", "who", null, null, "my graph");
      Annotation turn1 = new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "who1");
      Annotation the = new Annotation("word1", "the", "word", "a1", "a2", "turn1");
      Annotation quick = new Annotation("word2", "quick", "word", "a2", "a3", "turn1");
      Annotation brown = new Annotation("word3", "brown", "word", "a3", "a4", "turn1");
      Annotation fox = new Annotation("word4", "fox", "word", "a4", "a5", "turn1");
      Annotation orphan = new Annotation("orphan", "orphan", "word", "a3", "a4");

      g.addAnnotation(who1);
      assertNotNull("graph tags have start ID set ", who1.getStartId());
      assertNotNull("graph tags have end ID set ", who1.getEndId());
      g.addAnnotation(turn1);
      g.addAnnotation(the);
      g.addAnnotation(quick);
      g.addAnnotation(brown);
      g.addAnnotation(fox);
      g.addAnnotation(orphan);

      assertEquals(g, who1.getGraph());
      assertEquals(g, turn1.getGraph());
      assertEquals(g, the.getGraph());
      assertEquals(g, quick.getGraph());
      assertEquals(g, brown.getGraph());
      assertEquals(g, fox.getGraph());

      assertEquals(who1, g.getAnnotation("who1"));
      assertEquals(turn1, g.getAnnotation("turn1"));
      assertEquals(the, g.getAnnotation("word1"));
      assertEquals(quick, g.getAnnotation("word2"));
      assertEquals(brown, g.getAnnotation("word3"));
      assertEquals(fox, g.getAnnotation("word4"));
      
      // anchor objects are accessible
      assertEquals(turnStart, turn1.getStart());
      assertEquals(turnEnd, turn1.getEnd());
      assertEquals(a1, the.getStart());
      assertEquals(a2, the.getEnd());
      assertEquals(a2, quick.getStart());
      assertEquals(a3, quick.getEnd());
      assertEquals(a3, brown.getStart());
      assertEquals(a4, brown.getEnd());
      assertEquals(a4, fox.getStart());
      assertEquals(a5, fox.getEnd());
      assertEquals(turnStart, who1.getStart());
      assertEquals(turnEnd, who1.getEnd());

      // startOf set
      assertTrue(turnStart.startOf("turn").contains(turn1));
      assertTrue(a1.startOf("word").contains(the));
      assertTrue(a2.startOf("word").contains(quick));
      assertTrue(a3.startOf("word").contains(brown));
      assertTrue(a4.startOf("word").contains(fox));

      // endOf set
      assertTrue(a2.endOf("word").contains(the));
      assertTrue(a3.endOf("word").contains(quick));
      assertTrue(a4.endOf("word").contains(brown));
      assertTrue(a5.endOf("word").contains(fox));
      assertTrue(turnEnd.endOf("turn").contains(turn1));

      // layer objects are set
      assertEquals(turn, turn1.getLayer());
      assertEquals(word, the.getLayer());
      assertEquals(word, quick.getLayer());
      assertEquals(word, brown.getLayer());
      assertEquals(word, fox.getLayer());

      // parents are set
      assertEquals(turn1, the.getParent());
      assertEquals(turn1, quick.getParent());
      assertEquals(turn1, brown.getParent());
      assertEquals(turn1, fox.getParent());
      
      // ordinals are set - 1-based
      assertEquals(1, the.getOrdinal());
      assertEquals(2, quick.getOrdinal());
      assertEquals(3, brown.getOrdinal());
      assertEquals(4, fox.getOrdinal());

      // getPrevious works
      assertNull(the.getPrevious());
      assertEquals(the, quick.getPrevious());
      assertEquals(quick, brown.getPrevious());
      assertEquals(brown, fox.getPrevious());
      assertNull(turn1.getPrevious());

      // getNext works
      assertEquals(quick, the.getNext());
      assertEquals(brown, quick.getNext());
      assertEquals(fox, brown.getNext());
      assertNull(fox.getNext());
      assertNull(turn1.getNext());

      // graph's layer contains annotation
      List<Annotation> words = Arrays.asList(g.list("word"));
      assertTrue(words.contains(the));
      assertTrue(words.contains(quick));
      assertTrue(words.contains(brown));
      assertTrue(words.contains(fox));
      assertTrue("graph lists orphans", words.contains(orphan));
      assertTrue(words.size() == 5);
      // TODO graph 'contains' itself
      // assertEquals(g.list("graph")[0], g);
      // assertEquals(1, g.list("graph").length);
      
      List<Annotation> turns = Arrays.asList(g.list("turn"));
      assertTrue(turns.contains(turn1));

      // array version for top level only
      assertTrue(g.list("who")[0] == g.getAnnotations("who").first());


      // graph inherits from annotation, but some annotation behavrious are special
      assertEquals(g.getId(), g.getLabel());
      assertEquals("graph", g.getLayerId());
      assertNotNull("graph", g.getLayer());
      assertEquals("who", turn1.getLayer().getParentId());
      assertEquals(who1.getId(), turn1.getParentId());
      assertEquals(who1, turn1.getParent());
      assertEquals(g.getId(), who1.getParentId());
      assertEquals(g, who1.getParent());
      assertNull(g.getParentId());
      assertNull(g.getParent());
      assertEquals("turnStart", g.getStartId());
      assertEquals("turnEnd", g.getEndId());
      
      g.addAnchor(new Anchor("a51", null));
      g.addAnchor(new Anchor("a52", null));
      g.addAnchor(new Anchor("a53", null));
      g.addAnchor(new Anchor("a54", null));
      g.addAnchor(new Anchor("a6", null));

      // ID creation
      Annotation jumps = new Annotation(null, "jumps", "word", "a5", "a6", "turn1");
      Annotation j = new Annotation(null, "j", "phone", "a5", "a51");
      Annotation u = new Annotation(null, "u", "phone", "a51", "a52");
      Annotation m = new Annotation(null, "m", "phone", "a52", "a53");
      Annotation p = new Annotation(null, "p", "phone", "a53", "a54");
      Annotation s = new Annotation(null, "s", "phone", "a54", "a6");

      // add annotations before adding layer
      jumps.addAnnotation(j);
      jumps.addAnnotation(u);
      jumps.addAnnotation(m);
      jumps.addAnnotation(p);
      jumps.addAnnotation(s);

      // graph start/end anchor identification handles annotations with no layer
      assertEquals("Can get graph startId with annotations on unknown layer", 
		   "turnStart", g.getStartId());
      assertEquals("Can get graph endId with annotations on unknown layer", 
		   "turnEnd", g.getEndId());

      // now add the layer
      Layer phone = new Layer("phone", "Phones", 2, true, false, true, "word", true);
      g.addLayer(phone);

      assertNull(jumps.getId());
      assertNull(j.getParentId());
      assertNull(u.getParentId());
      assertNull(m.getParentId());
      assertNull(p.getParentId());
      assertNull(s.getParentId());
      g.addAnnotation(jumps);
      assertNotNull("Graph.addAnnotation sets ID", jumps.getId());
      assertEquals("Graph.addAnnotation sets ID", "1", jumps.getId());
      assertEquals("Graph.addAnnotation sets parent ID of children", "1", j.getParentId());
      assertEquals("Graph.addAnnotation sets parent ID of children", "1", u.getParentId());
      assertEquals("Graph.addAnnotation sets parent ID of children", "1", m.getParentId());
      assertEquals("Graph.addAnnotation sets parent ID of children", "1", p.getParentId());
      assertEquals("Graph.addAnnotation sets parent ID of children", "1", s.getParentId());

      assertNull("Graph.addAnnotation doesn't add children", j.getId());
      assertNull("Graph.addAnnotation doesn't add children", u.getId());
      assertNull("Graph.addAnnotation doesn't add children", m.getId());
      assertNull("Graph.addAnnotation doesn't add children", p.getId());
      assertNull("Graph.addAnnotation doesn't add children", s.getId());

   }

   @Test public void basicTagging() 
   {
      Graph g = new Graph();
      g.setId("my graph");

      Layer turn = new Layer("turn", "Speaker turns", 2, true, true, false);
      g.addLayer(turn);

      Layer word = new Layer("word", "Words", 2, true, false, false, "turn", true);
      g.addLayer(word);

      Layer pos = new Layer("pos", "Part of speech", 0, false, false, true, "word", true);
      g.addLayer(pos);

      Layer pron = new Layer("pron", "Phonemic transcription", 0, false, false, true, "word", true);
      g.addLayer(pron);

      g.addAnchor(new Anchor("turnStart", 0.0));
      g.addAnchor(new Anchor("a1", 1.0));
      g.addAnchor(new Anchor("a2", 2.0));
      g.addAnchor(new Anchor("a3", 3.0));
      g.addAnchor(new Anchor("a4", 4.0));
      g.addAnchor(new Anchor("a5", 5.0));
      g.addAnchor(new Anchor("turnEnd", 6.0));

      Annotation turn1 = new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "my graph");
      Annotation the = new Annotation("word1", "the", "word", "a1", "a2", "turn1");
      Annotation quick = new Annotation("word2", "quick", "word", "a2", "a3", "turn1");
      Annotation brown = new Annotation("word3", "brown", "word", "a3", "a4", "turn1");
      Annotation fox = new Annotation("word4", "fox", "word", "a4", "a5", "turn1");

      g.addAnnotation(turn1);
      g.addAnnotation(the);
      g.addAnnotation(quick);
      g.addAnnotation(brown);
      g.addAnnotation(fox);

      // tagging parent works
      Annotation posThe = the.createTag("pos", "DT");
      assertEquals("pos", posThe.getLayerId());
      assertEquals("DT", posThe.getLabel());
      assertEquals(posThe, g.getAnnotation(posThe.getId()));      
      assertEquals(pos, posThe.getLayer());
      assertEquals(the.getStartId(), posThe.getStartId());
      assertEquals(the.getEndId(), posThe.getEndId());
      assertEquals(the.getStart(), posThe.getStart());
      assertEquals(the.getEnd(), posThe.getEnd());
      assertEquals(the.getId(), posThe.getParentId());
      assertEquals(the, posThe.getParent());
      assertTrue(the.getAnnotations("pos").contains(posThe));
      assertEquals(posThe, g.list("pos")[0]);

      // ID creation
      assertNotNull(posThe.getId());
      assertEquals("1", posThe.getId());

      // tagging peer works
      Annotation pronThe = posThe.createTag("pron", "D@");
      assertEquals("pron", pronThe.getLayerId());
      assertEquals("D@", pronThe.getLabel());
      assertEquals(pronThe, g.getAnnotation(pronThe.getId()));      
      assertEquals(pron, pronThe.getLayer());

      // ID creation
      assertNotNull(pronThe.getId());
      assertEquals("2", pronThe.getId());

      // parent is "the", not the "DT"
      assertEquals(the.getStartId(), pronThe.getStartId());
      assertEquals(the.getEndId(), pronThe.getEndId());
      assertEquals(the.getStart(), pronThe.getStart());
      assertEquals(the.getEnd(), pronThe.getEnd());
      assertEquals(the.getId(), pronThe.getParentId());
      assertEquals(the, pronThe.getParent());
      assertTrue(the.getAnnotations("pron").contains(pronThe));

   }

   @Test public void basicSpans() 
   {
      Graph g = new Graph();
      g.setId("my graph");

      g.addLayer(new Layer("turn", "Speaker turns", 2, true, true, false, "graph", true));
      g.addLayer(new Layer("word", "Words", 2, true, false, false, "turn", true));
      g.addLayer(new Layer("phone", "Phones", 2, true, false, true, "word", true));
      g.addLayer(new Layer("pos", "Part of speech", 0, false, false, true, "word", true));
      g.addLayer(new Layer("phrase", "Phrase structure", 2, true, true, false, "turn", true));

      g.addAnchor(new Anchor("turnStart", 0.0));
      g.addAnchor(new Anchor("a1", 1.0));
      g.addAnchor(new Anchor("a1.5", 1.5));
      g.addAnchor(new Anchor("a2", 2.0));
      g.addAnchor(new Anchor("a2.25", 2.25));
      g.addAnchor(new Anchor("a2.5", 2.5));
      g.addAnchor(new Anchor("a2.75", 2.75));
      g.addAnchor(new Anchor("a3", 3.0));
      g.addAnchor(new Anchor("a4", 4.0));
      g.addAnchor(new Anchor("a5", 5.0));
      g.addAnchor(new Anchor("turnEnd", 6.0));

      Annotation turn1 = new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "my graph");

      Annotation the = new Annotation("word1", "the", "word", "a1", "a2", "turn1");
      Annotation DT = new Annotation("pos1", "DT", "pos", "a1", "a2", "word1");
      Annotation th = new Annotation("phone1", "D", "phone", "a1", "a1.5", "word1");
      Annotation e = new Annotation("phone2", "@", "phone", "a1.5", "a2", "word1");
      Annotation quick = new Annotation("word2", "quick", "word", "a2", "a3", "turn1");
      Annotation A = new Annotation("pos2", "A", "pos", "a2", "a3", "word2");
      Annotation k = new Annotation("phone3", "k", "phone", "a2", "a2.25", "word2");
      Annotation w = new Annotation("phone4", "w", "phone", "a2.25", "a2.5", "word2");
      Annotation I = new Annotation("phone5", "I", "phone", "a2.5", "a2.75", "word2");
      Annotation ck = new Annotation("phone6", "k", "phone", "a2.75", "a3", "word2");
      Annotation brown = new Annotation("word3", "brown", "word", "a3", "a4", "turn1");
      Annotation A2 = new Annotation("pos3", "A", "pos", "a3", "a4", "word3");
      Annotation fox = new Annotation("word4", "fox", "word", "a4", "a5", "turn1");
      Annotation N = new Annotation("pos4", "N", "pos", "a4", "a5", "word4");

      g.addAnnotation(turn1);

      g.addAnnotation(the);
      g.addAnnotation(quick);
      g.addAnnotation(brown);
      g.addAnnotation(fox);

      g.addAnnotation(th);
      g.addAnnotation(e);
      g.addAnnotation(k);
      g.addAnnotation(w);
      g.addAnnotation(I);
      g.addAnnotation(ck);

      g.addAnnotation(DT);
      g.addAnnotation(A);
      g.addAnnotation(A2);
      g.addAnnotation(N);

      // parent deduction - common parent
      Annotation NP = g.createSpan(the, fox, "phrase", "NP"); 
      assertEquals("phrase", NP.getLayerId());
      assertEquals("NP", NP.getLabel());
      assertEquals("a1", NP.getStartId());
      assertEquals("a5", NP.getEndId());
      assertEquals("turn1", NP.getParentId());
      // parent deduction - common ancestor
      Annotation AP = g.createSpan(k, A2, "phrase", "AP");
      assertEquals("phrase", AP.getLayerId());
      assertEquals("AP", AP.getLabel());
      assertEquals("a2", AP.getStartId());
      assertEquals("a4", AP.getEndId());
      assertEquals("turn1", AP.getParentId());


   }

   @Test public void basicChangeTracking() 
   {
      Graph g = new Graph();
      g.setId("my graph");

      g.addLayer(new Layer("turn", "Speaker turns", 2, true, true, false));
      g.addLayer(new Layer("word", "Words", 2, true, false, false, "turn", true));

      g.addAnchor(new Anchor("turnStart", 0.0));
      g.addAnchor(new Anchor("a1", 1.0));
      g.addAnchor(new Anchor("a2", 2.0));
      g.addAnchor(new Anchor("a3", 3.0));
      g.addAnchor(new Anchor("a4", 4.0));
      g.addAnchor(new Anchor("a5", 5.0));
      g.addAnchor(new Anchor("turnEnd", 6.0));

      Annotation turn1 = new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "my graph");
      Annotation the = new Annotation("word1", "the", "word", "a1", "a2", "turn1");
      Annotation quick = new Annotation("word2", "quick", "word", "a2", "a3", "turn1");
      Annotation brown = new Annotation("word3", "brown", "word", "a3", "a4", "turn1");
      Annotation fox = new Annotation("word4", "fox", "word", "a4", "a5", "turn1");

      g.addAnnotation(turn1);
      g.addAnnotation(the);
      g.addAnnotation(quick);
      g.addAnnotation(brown);
      g.addAnnotation(fox);
      assertNull(turn1.get("oldParentId"));

      assertEquals(Change.Operation.NoChange, g.getChange());
      assertEquals(0, g.getChanges().size());

      // make some changes
      the.setLabel("The");
      the.getStart().setOffset(0.5);
      assertEquals(Change.Operation.Update, g.getChange());
      Vector<Change> changes = g.getChanges();
      assertEquals(2, changes.size());
      assertEquals("Update a1: offset = 0.5", changes.elementAt(0).toString());
      assertEquals("Update word1: label = The", changes.elementAt(1).toString());

      Annotation jumps = new Annotation("word5", "jumps", "word", "a5", "a5", "turn1");
      jumps.create();
      g.addAnnotation(jumps);

      g.rollback();
      assertEquals(Change.Operation.NoChange, g.getChange());
      assertEquals(""+g.getChanges(), 0, g.getChanges().size());

      assertNull("ensure created annotations are removed by rollback", g.getAnnotation("word5"));

      g.create();
      assertEquals(Change.Operation.Create, g.getChange());
      changes = g.getChanges();
      int i = 0;
      assertEquals("Create my graph", changes.elementAt(i++).toString());
      assertEquals("Create turnStart", changes.elementAt(i++).toString());
      assertEquals("Update turnStart: offset = 0.0", changes.elementAt(i++).toString());
      assertEquals("Create a1", changes.elementAt(i++).toString());
      assertEquals("Update a1: offset = 1.0", changes.elementAt(i++).toString());
      assertEquals("Create a2", changes.elementAt(i++).toString());
      assertEquals("Update a2: offset = 2.0", changes.elementAt(i++).toString());
      assertEquals("Create a3", changes.elementAt(i++).toString());
      assertEquals("Update a3: offset = 3.0", changes.elementAt(i++).toString());
      assertEquals("Create a4", changes.elementAt(i++).toString());
      assertEquals("Update a4: offset = 4.0", changes.elementAt(i++).toString());
      assertEquals("Create a5", changes.elementAt(i++).toString());
      assertEquals("Update a5: offset = 5.0", changes.elementAt(i++).toString());
      assertEquals("Create turnEnd", changes.elementAt(i++).toString());
      assertEquals("Update turnEnd: offset = 6.0", changes.elementAt(i++).toString());
      assertEquals("Create turn1", changes.elementAt(i++).toString());
      assertEquals("Update turn1: label = john smith", changes.elementAt(i++).toString());
      assertEquals("Update turn1: startId = turnStart", changes.elementAt(i++).toString());
      assertEquals("Update turn1: endId = turnEnd", changes.elementAt(i++).toString());
      assertEquals("Update turn1: parentId = my graph", changes.elementAt(i++).toString());
      assertEquals("Update turn1: ordinal = 1", changes.elementAt(i++).toString());
      assertEquals("Create word1", changes.elementAt(i++).toString());
      assertEquals("Update word1: label = the", changes.elementAt(i++).toString());
      assertEquals("Update word1: startId = a1", changes.elementAt(i++).toString());
      assertEquals("Update word1: endId = a2", changes.elementAt(i++).toString());
      assertEquals("Update word1: parentId = turn1", changes.elementAt(i++).toString());
      assertEquals("Update word1: ordinal = 1", changes.elementAt(i++).toString());
      assertEquals("Create word2", changes.elementAt(i++).toString());
      assertEquals("Update word2: label = quick", changes.elementAt(i++).toString());
      assertEquals("Update word2: startId = a2", changes.elementAt(i++).toString());
      assertEquals("Update word2: endId = a3", changes.elementAt(i++).toString());
      assertEquals("Update word2: parentId = turn1", changes.elementAt(i++).toString());
      assertEquals("Update word2: ordinal = 2", changes.elementAt(i++).toString());
      assertEquals("Create word3", changes.elementAt(i++).toString());
      assertEquals("Update word3: label = brown", changes.elementAt(i++).toString());
      assertEquals("Update word3: startId = a3", changes.elementAt(i++).toString());
      assertEquals("Update word3: endId = a4", changes.elementAt(i++).toString());
      assertEquals("Update word3: parentId = turn1", changes.elementAt(i++).toString());
      assertEquals("Update word3: ordinal = 3", changes.elementAt(i++).toString());
      assertEquals("Create word4", changes.elementAt(i++).toString());
      assertEquals("Update word4: label = fox", changes.elementAt(i++).toString());
      assertEquals("Update word4: startId = a4", changes.elementAt(i++).toString());
      assertEquals("Update word4: endId = a5", changes.elementAt(i++).toString());
      assertEquals("Update word4: parentId = turn1", changes.elementAt(i++).toString());
      assertEquals("Update word4: ordinal = 4", changes.elementAt(i++).toString());
      assertEquals(i, changes.size());

      g.commit();
      assertEquals(Change.Operation.NoChange, g.getChange());

      g.destroy();
      assertEquals(Change.Operation.Destroy, g.getChange());
      changes = g.getChanges();
      i = 0;
      // children deleted before parents
      assertEquals("Destroy word1", changes.elementAt(i++).toString());
      assertEquals("Destroy word2", changes.elementAt(i++).toString());
      assertEquals("Destroy word3", changes.elementAt(i++).toString());
      assertEquals("Destroy word4", changes.elementAt(i++).toString());
      // parents deleted after children
      assertEquals("Destroy turn1", changes.elementAt(i++).toString());
      assertEquals("Destroy turnStart", changes.elementAt(i++).toString());
      assertEquals("Destroy a1", changes.elementAt(i++).toString());
      assertEquals("Destroy a2", changes.elementAt(i++).toString());
      assertEquals("Destroy a3", changes.elementAt(i++).toString());
      assertEquals("Destroy a4", changes.elementAt(i++).toString());
      assertEquals("Destroy a5", changes.elementAt(i++).toString());
      assertEquals("Destroy turnEnd", changes.elementAt(i++).toString());
      assertEquals("Destroy my graph", changes.elementAt(i++).toString());
      assertEquals(i, changes.size());

   }

   @Test public void changeOrder() 
   {
      Graph g = new Graph();
      g.setId("my graph");

      g.addLayer(new Layer("turn", "Speaker turns", 2, true, true, false));
      g.addLayer(new Layer("word", "Words", 2, true, false, false, "turn", true));

      g.addAnchor(new Anchor("turnStart", 0.0));
      g.addAnchor(new Anchor("a1", 1.0));
      g.addAnchor(new Anchor("a2", 2.0));
      g.addAnchor(new Anchor("a3", 3.0));
      g.addAnchor(new Anchor("a4", 4.0));
      g.addAnchor(new Anchor("a5", 5.0));
      g.addAnchor(new Anchor("a7", 7.0));
      g.addAnchor(new Anchor("turnEnd", 6.0));

      Annotation turn1 = new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "my graph");
      Annotation the = new Annotation("word1", "the", "word", "a1", "a2", "turn1");
      Annotation quick = new Annotation("word2", "quick", "word", "a2", "a3", "turn1");
      Annotation brown = new Annotation("word3", "brown", "word", "a3", "a4", "turn1");
      Annotation fox = new Annotation("word4", "fox", "word", "a4", "a5", "turn1");

      g.addAnnotation(turn1);
      g.addAnnotation(the);
      g.addAnnotation(quick);
      g.addAnnotation(brown);
      g.addAnnotation(fox);
      assertNull(turn1.get("oldParentId"));

      assertEquals(Change.Operation.NoChange, g.getChange());
      assertEquals(0, g.getChanges().size());

      // new word
      g.addAnchor(new Anchor("a6", 5.9));
      g.getAnchor("a6").create();
      Annotation jumps = new Annotation("word5", "jumps", "word", "a5", "a6", "turn1");
      jumps.create();
      g.addAnnotation(jumps);
      assertEquals("New child ordinal set", 5, jumps.getOrdinal());

      // and delete a word
      quick.destroy();
      turn1.getAnnotations("word");
      assertEquals("Deletions affect following ordinals", 2, brown.getOrdinal());
      assertEquals("Deletions affect following ordinals", 3, fox.getOrdinal());
      assertEquals("Deletions affect following ordinals", 4, jumps.getOrdinal());

      Vector<Change> changes = g.getChanges();
      int i = 0;
      assertEquals("Create a6", changes.elementAt(i++).toString());
      assertEquals("Update a6: offset = 5.9", changes.elementAt(i++).toString());
      assertEquals("Create word5", changes.elementAt(i++).toString());
      assertEquals("Update word5: label = jumps", changes.elementAt(i++).toString());
      assertEquals("Update word5: startId = a5", changes.elementAt(i++).toString());
      assertEquals("Update word5: endId = a6", changes.elementAt(i++).toString());
      assertEquals("Update word5: parentId = turn1", changes.elementAt(i++).toString());
      assertEquals("Update word5: ordinal = 4", changes.elementAt(i++).toString());
      assertEquals("Update word3: ordinal = 2", changes.elementAt(i++).toString());
      assertEquals("Update word4: ordinal = 3", changes.elementAt(i++).toString());
      // delete after create
      assertEquals("Destroy word2", changes.elementAt(i++).toString());
      assertEquals("" + changes, i, changes.size());

      assertEquals(1, turn1.getOrdinal());

      // replace a parent
      Annotation newTurn = new Annotation("newTurn", "john smith", "turn", "turnStart", "turnEnd", "my graph");
      assertEquals(0, newTurn.getOrdinal());
      newTurn.create();
      g.addAnnotation(newTurn);
      turn1.destroy();
      the.setParentId("newTurn");
      quick.setParentId("newTurn");
      brown.setParentId("newTurn");
      fox.setParentId("newTurn");
      jumps.setParentId("newTurn");

      changes = g.getChanges();
      i = 0;
      // create anchors first
      assertEquals("Create a6", changes.elementAt(i++).toString());
      assertEquals("Update a6: offset = 5.9", changes.elementAt(i++).toString());
      // new parent is created before any children are created or changed
      assertEquals("Create newTurn", changes.elementAt(i++).toString());
      assertEquals("Update newTurn: label = john smith", changes.elementAt(i++).toString());
      assertEquals("Update newTurn: startId = turnStart", changes.elementAt(i++).toString());
      assertEquals("Update newTurn: endId = turnEnd", changes.elementAt(i++).toString());
      assertEquals("Update newTurn: parentId = my graph", changes.elementAt(i++).toString());
      assertEquals("Update newTurn: ordinal = 1", changes.elementAt(i++).toString());
      // new child is created after its parent, and before its peers are changed
      assertEquals("Create word5", changes.elementAt(i++).toString());
      assertEquals("Update word5: label = jumps", changes.elementAt(i++).toString());
      assertEquals("Update word5: startId = a5", changes.elementAt(i++).toString());
      assertEquals("Update word5: endId = a6", changes.elementAt(i++).toString());
      assertEquals("Update word5: parentId = newTurn", changes.elementAt(i++).toString());
      assertEquals("Update word5: ordinal = 4", changes.elementAt(i++).toString());
      // then the children are updated
      assertEquals("Update word1: parentId = newTurn", changes.elementAt(i++).toString());
      assertEquals("Update word3: parentId = newTurn", changes.elementAt(i++).toString());
      assertEquals("Update word3: ordinal = 2", changes.elementAt(i++).toString());
      assertEquals("Update word4: parentId = newTurn", changes.elementAt(i++).toString());
      assertEquals("Update word4: ordinal = 3", changes.elementAt(i++).toString());
      // children delete before parents
      assertEquals("Destroy word2", changes.elementAt(i++).toString());
      // finally the old parent is deleted
      assertEquals("Destroy turn1", changes.elementAt(i++).toString());
      assertEquals(i, changes.size());

      // delete an anchor
      g.getAnchor("a7").destroy();
      Annotation word2 = g.getAnnotation("word2");
      g.commit();
      assertEquals(Change.Operation.NoChange, g.getChange());
      assertNull("commit removes deleted annotations", g.getAnnotation("word2"));
      assertEquals("commit removes anchor references to deleted annotations", 0, g.getAnchor("a2").startOf("word").size());
      assertEquals("commit removes anchor references to deleted annotations", 0, g.getAnchor("a3").endOf("word").size());
      assertNull("commit removes deleted annotations", g.getAnnotation("turn1"));
      assertNull("commit removes deleted anchors", g.getAnchor("a7"));
   }

   @Test public void constructionOrder() 
   {
      Graph g = new Graph();
      g.setId("my graph");

      // add word layer before its parent turn layer
      Layer word = new Layer("word", "Words", 2, true, false, false, "turn", true);
      g.addLayer(word);
      Layer turn = new Layer("turn", "Speaker turns", 2, true, true, false, "graph", true);
      g.addLayer(turn);

      assertEquals(turn, g.getLayer("turn"));
      assertEquals(word, g.getLayer("word"));
      assertEquals(turn, word.getParent());

      Anchor turnStart = new Anchor("turnStart", 0.0);
      Anchor a1 = new Anchor("a1", 1.0);
      Anchor a2 = new Anchor("a2", 2.0);
      Anchor a3 = new Anchor("a3", 3.0);
      Anchor a4 = new Anchor("a4", 4.0);
      Anchor a5 = new Anchor("a5", 5.0);
      Anchor turnEnd = new Anchor("turnEnd", 6.0);
      Anchor aX = new Anchor("aX", null);

      // don't add the annotations in offset order
      g.addAnchor(a4);
      g.addAnchor(a1);
      g.addAnchor(a3);
      g.addAnchor(a5);
      g.addAnchor(a2);
      g.addAnchor(turnStart);
      g.addAnchor(turnEnd);
      // add one with no offset
      g.addAnchor(aX);

      assertEquals(turnStart, g.getAnchor("turnStart"));
      assertEquals(a1, g.getAnchor("a1"));
      assertEquals(a2, g.getAnchor("a2"));
      assertEquals(a3, g.getAnchor("a3"));
      assertEquals(a4, g.getAnchor("a4"));
      assertEquals(a5, g.getAnchor("a5"));
      assertEquals(turnEnd, g.getAnchor("turnEnd"));

      assertEquals(turnStart, g.getSortedAnchors().first());
      assertEquals(turnEnd, g.getSortedAnchors().last());

      Annotation turn1 = new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "my graph");
      Annotation the = new Annotation("word1", "the", "word", "a1", "a2", "turn1", 1);
      Annotation quick = new Annotation("word2", "quick", "word", "a2", "a3", "turn1", 2);
      Annotation brown = new Annotation("word3", "brown", "word", "a3", "a4", "turn1", 3);
      Annotation fox = new Annotation("word4", "fox", "word", "a4", "a5", "turn1", 4);

      // add words out of ordinal order
      g.addAnnotation(quick);
      g.addAnnotation(the);
      g.addAnnotation(fox);
      g.addAnnotation(brown);

      // ordinals are set as above, despite insertion order
      assertEquals("Ordinals ignore insertion order:", 1, the.getOrdinal());
      assertEquals("Ordinals ignore insertion order:", 2, quick.getOrdinal());
      assertEquals("Ordinals ignore insertion order:", 3, brown.getOrdinal());
      assertEquals("Ordinals ignore insertion order:", 4, fox.getOrdinal());

      // add turn after child annotations
      g.addAnnotation(turn1);

      // ordinals are still set as above, despite insertion order
      assertEquals("Ordinals ignore insertion order:", 1, the.getOrdinal());
      assertEquals("Ordinals ignore insertion order:", 2, quick.getOrdinal());
      assertEquals("Ordinals ignore insertion order:", 3, brown.getOrdinal());
      assertEquals("Ordinals ignore insertion order:", 4, fox.getOrdinal());

      // layer contains annotation
      List<Annotation> words = Arrays.asList(g.list("word"));
      assertTrue(words.contains(the));
      assertTrue(words.contains(quick));
      assertTrue(words.contains(brown));
      assertTrue(words.contains(fox));
      List<Annotation> turns = Arrays.asList(g.list("turn"));
      assertTrue(turns.contains(turn1));

      // parents are set
      assertEquals(g, turn1.getParent());
      assertEquals(turn1, the.getParent());
      assertEquals(turn1, quick.getParent());
      assertEquals(turn1, brown.getParent());
      assertEquals(turn1, fox.getParent());

      // children are set
      assertTrue(g.getAnnotations("turn").contains(turn1));
      assertTrue(turn1.getAnnotations("word").contains(the));
      assertTrue(turn1.getAnnotations("word").contains(quick));
      assertTrue(turn1.getAnnotations("word").contains(brown));
      assertTrue(turn1.getAnnotations("word").contains(fox));

      // ordinals are set
      assertEquals(1, turn1.getOrdinal());
      assertEquals(1, the.getOrdinal());
      assertEquals(2, quick.getOrdinal());
      assertEquals(3, brown.getOrdinal());
      assertEquals(4, fox.getOrdinal());
      
      // getPrevious works
      assertNull(the.getPrevious());
      assertEquals(the, quick.getPrevious());
      assertEquals(quick, brown.getPrevious());
      assertEquals(brown, fox.getPrevious());
      assertNull(turn1.getPrevious());

      // getNext works
      assertEquals(quick, the.getNext());
      assertEquals(brown, quick.getNext());
      assertEquals(fox, brown.getNext());
      assertNull(fox.getNext());
      assertNull(turn1.getNext());
   }

   @Test public void annotationHierarchy() 
   {
      Graph g = new Graph();
      g.setId("my graph");

      g.addLayer(new Layer("turn", "Speaker turns", 2, true, true, false));
      g.addLayer(new Layer("word", "Words", 2, true, false, false, "turn", true));
      g.addLayer(new Layer("phone", "Phones", 2, true, false, true, "word", true));
      g.addLayer(new Layer("pos", "Part of speech", 0, false, false, true, "word", true));
      g.addLayer(new Layer("phrase", "Phrase structure", 2, true, true, false, "turn", true));

      g.addAnchor(new Anchor("turnStart", 0.0));
      g.addAnchor(new Anchor("a1", 1.0));
      g.addAnchor(new Anchor("a1.5", 1.5));
      g.addAnchor(new Anchor("a2", 2.0));
      g.addAnchor(new Anchor("a2.25", 2.25));
      g.addAnchor(new Anchor("a2.5", 2.5));
      g.addAnchor(new Anchor("a2.75", 2.75));
      g.addAnchor(new Anchor("a3", 3.0));
      g.addAnchor(new Anchor("a4", 4.0));
      g.addAnchor(new Anchor("a5", 5.0));
      g.addAnchor(new Anchor("turnEnd", 6.0));

      Annotation turn1 = new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "my graph");

      Annotation the = new Annotation("word1", "the", "word", "a1", "a2", "turn1");
      Annotation DT = new Annotation("pos1", "DT", "pos", "a1", "a2", "word1");
      Annotation th = new Annotation("phone1", "D", "phone", "a1", "a1.5", "word1");
      Annotation e = new Annotation("phone2", "@", "phone", "a1.5", "a2", "word1");
      Annotation quick = new Annotation("word2", "quick", "word", "a2", "a3", "turn1");
      Annotation A = new Annotation("pos2", "A", "pos", "a2", "a3", "word2");
      Annotation k = new Annotation("phone3", "k", "phone", "a2", "a2.25", "word2");
      Annotation w = new Annotation("phone4", "w", "phone", "a2.25", "a2.5", "word2");
      Annotation I = new Annotation("phone5", "I", "phone", "a2.5", "a2.75", "word2");
      Annotation ck = new Annotation("phone6", "k", "phone", "a2.75", "a3", "word2");
      Annotation brown = new Annotation("word3", "brown", "word", "a3", "a4", "turn1");
      Annotation AP = new Annotation("phrase1", "AP", "phrase", "a2", "a4", "turn1");
      Annotation fox = new Annotation("word4", "fox", "word", "a4", "a5", "turn1");
      Annotation N = new Annotation("pos3", "N", "pos", "a4", "a5", "word4");
      Annotation NP = new Annotation("phrase2", "NP", "phrase", "a1", "a5", "turn1");

      g.addAnnotation(turn1);

      g.addAnnotation(the);
      g.addAnnotation(quick);
      g.addAnnotation(brown);
      g.addAnnotation(fox);

      g.addAnnotation(th);
      g.addAnnotation(e);
      g.addAnnotation(k);
      g.addAnnotation(w);
      g.addAnnotation(I);
      g.addAnnotation(ck);

      g.addAnnotation(DT);
      g.addAnnotation(A);
      g.addAnnotation(N);

      g.addAnnotation(AP);
      g.addAnnotation(NP);

      // includingAnnotationsOn
      Annotation[] including = the.includingAnnotationsOn("turn");
      assertEquals(turn1, including[0]);
      assertEquals(1, including.length);
      including = the.includingAnnotationsOn("pos");
      assertEquals(DT, including[0]);
      assertEquals(1, including.length);
      including = the.includingAnnotationsOn("phone");
      assertEquals(0, including.length);
      including = quick.includingAnnotationsOn("phrase");
      assertEquals("earlier first", AP, including[0]);
      assertEquals("later last", NP, including[1]);
      assertEquals(2, including.length);
      // own layer
      including = the.includingAnnotationsOn("word");
      assertEquals("includingAnnotationsOn - exclude self", 0, including.length);
      including = AP.includingAnnotationsOn("phrase");
      assertEquals(NP, including[0]);
      assertEquals("includingAnnotationsOn - exclude self", 1, including.length);

      // includedAnnotationsOn
      Annotation[] included = AP.includedAnnotationsOn("word");
      assertEquals(quick, included[0]);
      assertEquals(brown, included[1]);
      assertEquals(2, included.length);
      included = DT.includedAnnotationsOn("word");
      assertEquals(the, included[0]);
      assertEquals(1, included.length);
      included = the.includedAnnotationsOn("pos");
      assertEquals(DT, included[0]);
      assertEquals(1, included.length);
      included = quick.includedAnnotationsOn("phrase");
      assertEquals(0, included.length);
      // own layer
      included = the.includedAnnotationsOn("word");
      assertEquals("includedAnnotationsOn - exclude self", 0, included.length);
      included = NP.includedAnnotationsOn("phrase");
      assertEquals(AP, included[0]);
      assertEquals("includingAnnotationsOn - exclude self", 1, included.length);

      // midpointIncludingAnnotationsOn
      including = the.midpointIncludingAnnotationsOn("turn");
      assertEquals(turn1, including[0]);
      assertEquals(1, including.length);
      including = the.midpointIncludingAnnotationsOn("pos");
      assertEquals(DT, including[0]);
      assertEquals(1, including.length);
      including = the.midpointIncludingAnnotationsOn("phone");
      assertEquals(e, including[0]);
      assertEquals(1, including.length);
      including = quick.midpointIncludingAnnotationsOn("phrase");
      assertEquals("earlier first", AP, including[0]);
      assertEquals("later last", NP, including[1]);
      assertEquals(2, including.length);
      // own layer
      including = the.midpointIncludingAnnotationsOn("word");
      assertEquals("midpointIncludingAnnotationsOn - exclude self", 0, including.length);
      including = AP.midpointIncludingAnnotationsOn("phrase");
      assertEquals(NP, including[0]);
      assertEquals("midpointIncludingAnnotationsOn - exclude self", 1, including.length);

      // tagsOn
      Annotation[] tags = the.tagsOn("turn");
      assertEquals(0, tags.length);
      tags = the.tagsOn("pos");
      assertEquals(DT, tags[0]);
      assertEquals(1, tags.length);
      tags = DT.tagsOn("word");
      assertEquals(the, tags[0]);
      assertEquals(1, tags.length);
      tags = the.tagsOn("phone");
      assertEquals(0, tags.length);
      tags = quick.tagsOn("phrase");
      assertEquals(0, tags.length);
      // own layer
      tags = the.tagsOn("word");
      assertEquals("tagsOn - exclude self", 0, tags.length);

      // ancestors
      LinkedHashSet<Annotation> ancestors = th.getAncestors();
      Iterator<Annotation> order = ancestors.iterator();
      assertEquals("ancestors - parent", the, order.next());
      assertEquals("ancestors - grandparent", turn1, order.next());
      assertEquals("ancestors - graph", g, order.next());
      assertFalse("ancestors", order.hasNext());
      assertEquals("ancestors - parent", the, th.getAncestor("word"));
      assertEquals("ancestors - grandparent", turn1, th.getAncestor("turn"));
      assertEquals("ancestors - graph", g, th.getAncestor("graph"));
      assertNull("ancestors - none", th.getAncestor("pos"));

      // getFirstCommonAncestor
      assertEquals("getFirstCommonAncestor - common parent", turn1, the.getFirstCommonAncestor(quick));
      assertEquals("getFirstCommonAncestor - parent/child", the, the.getFirstCommonAncestor(e));
      assertEquals("getFirstCommonAncestor - child/parent", the, e.getFirstCommonAncestor(the));

      assertEquals("getFirstCommonAncestor - across layers", turn1, the.getFirstCommonAncestor(NP));
      
      assertEquals("getFirstCommonAncestor - common grandparent", turn1, th.getFirstCommonAncestor(k));
      assertEquals("getFirstCommonAncestor - reflexive", the, the.getFirstCommonAncestor(the));

      // getEarliestDescendant
      assertEquals("getEarliestDescendant - parent/child", k, quick.getEarliestDescendant());
      assertEquals("getEarliestDescendant - parent/child with grandchildren", the, turn1.getEarliestDescendant());
      assertNull("getEarliestDescendant - no descendants", NP.getEarliestDescendant());

      // getLatestDescendant
      assertEquals("getLatestDescendant - parent/child", e, the.getLatestDescendant());
      assertEquals("getLatestDescendant - parent/child with grandchildren", fox, turn1.getLatestDescendant());
      assertNull("getLatestDescendant - no descendants", k.getLatestDescendant());
   }

   @Test public void fragmentByOffset() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      g.setMediaProvider(new IGraphMediaProvider() {
          public MediaFile[] getAvailableMedia() throws StoreException, PermissionException
          { return null; }
          public String getMedia(String trackSuffix, String mimeType)
            throws StoreException, PermissionException
          { return "GraphMediaProvider"; }
          public IGraphMediaProvider providerForGraph(Graph graph)
          {
            return new IGraphMediaProvider() {
              public MediaFile[] getAvailableMedia() throws StoreException, PermissionException
              { return null; }
              public String getMedia(String trackSuffix, String mimeType)
                throws StoreException, PermissionException
              { return "FragmentMediaProvider"; }
              public IGraphMediaProvider providerForGraph(Graph graph)
              { return this; }
            };
          }
        });

      g.addLayer(new Layer("topic", "Topics", 2, true, false, false));
      g.addLayer(new Layer("who", "Participants", 0, true, true, true));
      g.addLayer(new Layer("turn", "Speaker turns", 2, true, false, false, "who", true));
      g.addLayer(new Layer("utterance", "Utterances", 2, true, false, true, "turn", true));
      g.addLayer(new Layer("word", "Words", 2, true, false, false, "turn",true));
      g.addLayer(new Layer("phone", "Phones", 2, true, false, true, "word",  true));
      g.addLayer(new Layer("pos", "Part of speech", 0, false, false, true, "word", true));
      g.addLayer(new Layer("phrase", "Phrase structure", 0, true, true, false, "turn", true));

      g.getSchema().setParticipantLayerId("who");
      g.getSchema().setTurnLayerId("turn");
      g.getSchema().setUtteranceLayerId("utterance");
      g.getSchema().setWordLayerId("word");

      g.addAnchor(new Anchor("turnStart", 0.0));
      g.addAnchor(new Anchor("a1", 1.0));
      g.addAnchor(new Anchor("a1.5", 1.5));
       // include null offset anchor, it should still be added to the fragment
      g.addAnchor(new Anchor("a2", null));
      g.addAnchor(new Anchor("a2.25", 2.25));
      g.addAnchor(new Anchor("a2.5", 2.5));
      g.addAnchor(new Anchor("a2.75", 2.75));
      g.addAnchor(new Anchor("a3", 3.0));
      g.addAnchor(new Anchor("a4", 4.0));
      g.addAnchor(new Anchor("a5", 5.0));
      g.addAnchor(new Anchor("turnEnd", 6.0));

      Annotation who1 = new Annotation("who1", "john smith", "who", "turnStart", "turnEnd", "my graph");
      Annotation who2 = new Annotation("who2", "jane doe", "who", "turnStart", "turnEnd", "my graph");

      Annotation turn1 = new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "who1");

      Annotation utterance1 = new Annotation("utterance1", "john smith", "utterance", "turnStart", "a3", "turn1");
      Annotation utterance2 = new Annotation("utterance2", "john smith", "utterance", "a3", "turnEnd", "turn1");

      Annotation the = new Annotation("word1", "the", "word", "a1", "a2", "turn1");
      Annotation DT = new Annotation("pos1", "DT", "pos", "a1", "a2", "word1");
      Annotation th = new Annotation("phone1", "D", "phone", "a1", "a1.5", "word1");
      Annotation e = new Annotation("phone2", "@", "phone", "a1.5", "a2", "word1");
      Annotation quick = new Annotation("word2", "quick", "word", "a2", "a3", "turn1");
      Annotation A = new Annotation("pos2", "A", "pos", "a2", "a3", "word2");
      Annotation k = new Annotation("phone3", "k", "phone", "a2", "a2.25", "word2");
      Annotation w = new Annotation("phone4", "w", "phone", "a2.25", "a2.5", "word2");
      Annotation I = new Annotation("phone5", "I", "phone", "a2.5", "a2.75", "word2");
      Annotation ck = new Annotation("phone6", "k", "phone", "a2.75", "a3", "word2");
      Annotation brown = new Annotation("word3", "brown", "word", "a3", "a4", "turn1");
      Annotation AP = new Annotation("phrase1", "AP", "phrase", "a2", "a4", "turn1");
      Annotation fox = new Annotation("word4", "fox", "word", "a4", "a5", "turn1");
      Annotation N = new Annotation("pos3", "N", "pos", "a4", "a5", "word4");
      Annotation NP = new Annotation("phrase2", "NP", "phrase", "a1", "a5", "turn1");

      g.addAnnotation(who1);
      g.addAnnotation(turn1);
      g.addAnnotation(utterance1);
      g.addAnnotation(utterance2);

      g.addAnnotation(the);
      g.addAnnotation(quick);
      g.addAnnotation(brown);
      g.addAnnotation(fox);

      g.addAnnotation(th);
      g.addAnnotation(e);
      g.addAnnotation(k);
      g.addAnnotation(w);
      g.addAnnotation(I);
      g.addAnnotation(ck);

      g.addAnnotation(DT);
      g.addAnnotation(A);
      g.addAnnotation(N);

      g.addAnnotation(AP);
      g.addAnnotation(NP);

      assertFalse(g.isFragment());

      // create fragment
      Vector<String> layers = new Vector<String>();
      layers.add("turn"); // include ancestor layer - annotations should have no anchors
      layers.add("utterance");
      layers.add("word");
      Graph f = g.getFragment(0.0, 3.0, layers.toArray(new String[0]));
      assertEquals("my graph__0.000-3.000", f.getId());
      assertEquals("fragment's graph is itself", f, f.getGraph());
      assertTrue(f.isFragment());
      assertEquals("graph schema copied to fragment",
                  g.getSchema().getParticipantLayerId(), f.getSchema().getParticipantLayerId());
      assertEquals("graph schema copied to fragment",
                  g.getSchema().getTurnLayerId(), f.getSchema().getTurnLayerId());
      assertEquals("graph schema copied to fragment",
                  g.getSchema().getUtteranceLayerId(), f.getSchema().getUtteranceLayerId());
      assertEquals("graph schema copied to fragment",
                  g.getSchema().getWordLayerId(), f.getSchema().getWordLayerId());

      // check anchors
      assertTrue(f.getAnchors().containsKey("turnStart")); 
      assertTrue(f.getAnchors().containsKey("a1"));
      assertFalse(f.getAnchors().containsKey("a1.5"));
      assertTrue(f.getAnchors().containsKey("a2"));
      assertFalse(f.getAnchors().containsKey("a2.25"));
      assertFalse(f.getAnchors().containsKey("a2.5"));
      assertFalse(f.getAnchors().containsKey("a2.75"));
      assertTrue(f.getAnchors().containsKey("a3"));
      assertFalse(f.getAnchors().containsKey("a4"));
      assertFalse(f.getAnchors().containsKey("a5"));
      assertFalse(f.getAnchors().containsKey("turnEnd"));

      // check annotations
      assertTrue("speaker", f.getAnnotationsById().containsKey("who1"));
      assertTrue("turn", f.getAnnotationsById().containsKey("turn1"));
      assertTrue("utterance", f.getAnnotationsById().containsKey("utterance1"));
      assertFalse("utterance afterwards", f.getAnnotationsById().containsKey("utterance2"));

      assertTrue("included word", f.getAnnotationsById().containsKey("word1"));
      assertTrue("included word", f.getAnnotationsById().containsKey("word2"));
      assertFalse("word after", f.getAnnotationsById().containsKey("word3"));
      assertFalse("word after", f.getAnnotationsById().containsKey("word4"));

      assertFalse("included but excluded layer", f.getAnnotationsById().containsKey("phone1"));
      assertFalse("included but excluded layer", f.getAnnotationsById().containsKey("phone2"));
      assertFalse("included but excluded layer", f.getAnnotationsById().containsKey("phone3"));
      assertFalse("included but excluded layer", f.getAnnotationsById().containsKey("phone4"));
      assertFalse("included but excluded layer", f.getAnnotationsById().containsKey("phone5"));
      assertFalse("included but excluded layer", f.getAnnotationsById().containsKey("phone6"));

      assertFalse("included but excluded layer", f.getAnnotationsById().containsKey("pos1"));
      assertFalse("included but excluded layer", f.getAnnotationsById().containsKey("pos2"));
      assertFalse("excluded layer", f.getAnnotationsById().containsKey("pos3"));

      assertFalse("excluded layer", f.getAnnotationsById().containsKey("phrase1"));
      assertFalse("excluded layer", f.getAnnotationsById().containsKey("phrase2"));

      // check layers
      assertFalse("excluded layer", f.getSchema().getLayers().containsKey("topic"));
      assertTrue("ancestor layer", f.getSchema().getLayers().containsKey("who"));
      assertTrue("ancestor layer", f.getSchema().getLayers().containsKey("turn"));
      assertTrue("included layer", f.getSchema().getLayers().containsKey("utterance"));
      assertTrue("included layer", f.getSchema().getLayers().containsKey("word"));
      assertFalse("excluded layer", f.getSchema().getLayers().containsKey("phone"));
      assertFalse("excluded layer", f.getSchema().getLayers().containsKey("pos"));
      assertFalse("excluded layer", f.getSchema().getLayers().containsKey("phrase"));

      // check hierarchy traversal
      Annotation[] words = f.list("word");
      assertEquals("list words", "word1", words[0].getId());
      assertEquals("list words", "word2", words[1].getId());
      assertEquals("list all and only words", 2, words.length);

      // check layer identification
      assertTrue("fragment layer correct", f.getLayer() == f.getSchema().getRoot());

      // offset anchors
      assertEquals("before offset change", 
		   Double.valueOf(0.0), f.getAnchors().get("turnStart").getOffset()); 
      assertEquals("before offset change", 
		   Double.valueOf(1.0), f.getAnchors().get("a1").getOffset());
      assertNull("before offset change", 
		 f.getAnchors().get("a2").getOffset());
      assertEquals("before offset change", 
		   Double.valueOf(3.0), f.getAnchors().get("a3").getOffset());
      
      assertEquals("shiftAnchors", 3, f.shiftAnchors(100.0).size());

      assertEquals("after offset change", 
		   Double.valueOf(100.0), f.getAnchors().get("turnStart").getOffset()); 
      assertEquals("after offset change", 
		   Double.valueOf(101.0), f.getAnchors().get("a1").getOffset());
      assertNull("after offset change", 
                 f.getAnchors().get("a2").getOffset());
      assertEquals("after offset change", 
                   Double.valueOf(103.0), f.getAnchors().get("a3").getOffset());

      // check media provider handling
      try
      {
        assertEquals("graph has original media proviers",
                     "GraphMediaProvider", g.getMediaProvider().getMedia(null,null));
        assertEquals("fragment has derived media proviers",
                     "FragmentMediaProvider", f.getMediaProvider().getMedia(null,null));
      }
      catch(Exception exception)
      {
        fail("Media provider: " + exception);
      }
   }
  
   @Test public void fragmentByAnnotation() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      g.setMediaProvider(new IGraphMediaProvider() {
          public MediaFile[] getAvailableMedia() throws StoreException, PermissionException
          { return null; }
          public String getMedia(String trackSuffix, String mimeType)
            throws StoreException, PermissionException
          { return "GraphMediaProvider"; }
          public IGraphMediaProvider providerForGraph(Graph graph)
          {
            return new IGraphMediaProvider() {
              public MediaFile[] getAvailableMedia() throws StoreException, PermissionException
              { return null; }
              public String getMedia(String trackSuffix, String mimeType)
                throws StoreException, PermissionException
              { return "FragmentMediaProvider"; }
              public IGraphMediaProvider providerForGraph(Graph graph)
              { return this; }
            };
          }
        });

      g.setSchema(new Schema(
                  "who", "turn", "utterance", "word",
                  new Layer("topic", "Topics", 2, true, false, false),
                  new Layer("who", "Participants", 0, true, true, true),
                  new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
                  new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
                  new Layer("word", "Words", 2, true, false, false, "turn",true),
                  new Layer("phone", "Phones", 2, true, false, true, "word",  true),
                  new Layer("pos", "Part of speech", 0, false, false, true, "word", true),
                  new Layer("phrase", "Phrase structure", 0, true, true, false, "turn", true)));

      g.addAnchor(new Anchor("turnStart", 0.0));
      g.addAnchor(new Anchor("a1", 1.0));
      g.addAnchor(new Anchor("a1.5", 1.5));
      g.addAnchor(new Anchor("a2", 2.0));
       // include null offset anchor, it should still be added to the fragment
      g.addAnchor(new Anchor("a2.25", null));
      g.addAnchor(new Anchor("a2.5", 2.5));
      g.addAnchor(new Anchor("a2.75", 2.75));
      g.addAnchor(new Anchor("a3", 3.0));
      g.addAnchor(new Anchor("a4", 4.0));
      g.addAnchor(new Anchor("a5", 5.0));
      g.addAnchor(new Anchor("turnEnd", 6.0));

      Annotation who1 = new Annotation("who1", "john smith", "who", "turnStart", "turnEnd", "my graph");
      Annotation who2 = new Annotation("who2", "jane doe", "who", "turnStart", "turnEnd", "my graph");

      Annotation turn1 = new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "who1");

      Annotation utterance1 = new Annotation("utterance1", "john smith", "utterance", "turnStart", "a3", "turn1");
      Annotation utterance2 = new Annotation("utterance2", "john smith", "utterance", "a3", "turnEnd", "turn1");

      Annotation the = new Annotation("word1", "the", "word", "a1", "a2", "turn1");
      Annotation DT = new Annotation("pos1", "DT", "pos", "a1", "a2", "word1");
      Annotation th = new Annotation("phone1", "D", "phone", "a1", "a1.5", "word1");
      Annotation e = new Annotation("phone2", "@", "phone", "a1.5", "a2", "word1");
      Annotation quick = new Annotation("word2", "quick", "word", "a2", "a3", "turn1");
      Annotation A = new Annotation("pos2", "A", "pos", "a2", "a3", "word2");
      Annotation k = new Annotation("phone3", "k", "phone", "a2", "a2.25", "word2");
      Annotation w = new Annotation("phone4", "w", "phone", "a2.25", "a2.5", "word2");
      Annotation I = new Annotation("phone5", "I", "phone", "a2.5", "a2.75", "word2");
      Annotation ck = new Annotation("phone6", "k", "phone", "a2.75", "a3", "word2");
      Annotation brown = new Annotation("word3", "brown", "word", "a3", "a4", "turn1");
      Annotation AP = new Annotation("phrase1", "AP", "phrase", "a2", "a4", "turn1");
      Annotation fox = new Annotation("word4", "fox", "word", "a4", "a5", "turn1");
      Annotation N = new Annotation("pos3", "N", "pos", "a4", "a5", "word4");
      Annotation NP = new Annotation("phrase2", "NP", "phrase", "a1", "a5", "turn1");

      g.addAnnotation(who1);
      g.addAnnotation(turn1);
      g.addAnnotation(utterance1);
      g.addAnnotation(utterance2);

      g.addAnnotation(the);
      g.addAnnotation(quick);
      g.addAnnotation(brown);
      g.addAnnotation(fox);

      g.addAnnotation(th);
      g.addAnnotation(e);
      g.addAnnotation(k);
      g.addAnnotation(w);
      g.addAnnotation(I);
      g.addAnnotation(ck);

      g.addAnnotation(DT);
      g.addAnnotation(A);
      g.addAnnotation(N);

      g.addAnnotation(AP);
      g.addAnnotation(NP);

      assertFalse(g.isFragment());

      // create fragment
      Vector<String> layers = new Vector<String>();
      layers.add("turn"); // include ancestor layer - annotations should have no anchors
      layers.add("phone");
      layers.add("pos");
      assertEquals(2, quick.getOrdinal());
      Graph f = g.getFragment(quick, layers.toArray(new String[0]));
      assertEquals("my graph__2.000-3.000", f.getId());
      assertEquals("fragment's graph is itself", f, f.getGraph());
      assertTrue(f.isFragment());
      assertEquals("graph schema copied to fragment",
                  g.getSchema().getParticipantLayerId(), f.getSchema().getParticipantLayerId());
      assertEquals("graph schema copied to fragment",
                  g.getSchema().getTurnLayerId(), f.getSchema().getTurnLayerId());
      assertEquals("graph schema copied to fragment",
                  g.getSchema().getUtteranceLayerId(), f.getSchema().getUtteranceLayerId());
      assertEquals("graph schema copied to fragment",
                  g.getSchema().getWordLayerId(), f.getSchema().getWordLayerId());

      // check anchors
      assertFalse("check anchors", f.getAnchors().containsKey("turnStart")); 
      assertFalse("check anchors", f.getAnchors().containsKey("a1"));
      assertFalse("check anchors", f.getAnchors().containsKey("a1.5"));
      assertTrue("check anchors", f.getAnchors().containsKey("a2"));
      assertTrue("check anchors", f.getAnchors().containsKey("a2.25"));
      assertTrue("check anchors", f.getAnchors().containsKey("a2.5"));
      assertTrue("check anchors", f.getAnchors().containsKey("a2.75"));
      assertTrue("check anchors", f.getAnchors().containsKey("a3"));
      assertFalse("check anchors", f.getAnchors().containsKey("a4"));
      assertFalse("check anchors", f.getAnchors().containsKey("a5"));
      assertFalse("check anchors", f.getAnchors().containsKey("turnEnd"));

      // check annotations
      assertTrue("speaker", f.getAnnotationsById().containsKey("who1"));
      assertTrue("turn", f.getAnnotationsById().containsKey("turn1"));
      assertFalse("utterance not ancestor(!)", f.getAnnotationsById().containsKey("utterance1"));
      assertFalse("utterance afterwards", f.getAnnotationsById().containsKey("utterance2"));

      assertFalse("word before", f.getAnnotationsById().containsKey("word1"));
      assertTrue("annotation word", f.getAnnotationsById().containsKey("word2"));
      assertEquals("annotation word ordinal", 2, f.getAnnotation("word2").getOrdinal());
      assertFalse("word after", f.getAnnotationsById().containsKey("word3"));
      assertFalse("word after", f.getAnnotationsById().containsKey("word4"));

      assertFalse("other parent", f.getAnnotationsById().containsKey("phone1"));
      assertFalse("other parent", f.getAnnotationsById().containsKey("phone2"));
      assertTrue("included", f.getAnnotationsById().containsKey("phone3"));
      assertTrue("included", f.getAnnotationsById().containsKey("phone4"));
      assertTrue("included", f.getAnnotationsById().containsKey("phone5"));
      assertTrue("included", f.getAnnotationsById().containsKey("phone6"));

      assertFalse("other parent", f.getAnnotationsById().containsKey("pos1"));
      assertTrue("included", f.getAnnotationsById().containsKey("pos2"));
      assertFalse("other parent", f.getAnnotationsById().containsKey("pos3"));

      assertFalse("excluded layer", f.getAnnotationsById().containsKey("phrase1"));
      assertFalse("excluded layer", f.getAnnotationsById().containsKey("phrase2"));

      // check layers
      assertFalse("excluded layer", f.getSchema().getLayers().containsKey("topic"));
      assertTrue("ancestor layer", f.getSchema().getLayers().containsKey("who"));
      assertTrue("ancestor layer", f.getSchema().getLayers().containsKey("turn"));
      assertFalse("excluded", f.getSchema().getLayers().containsKey("utterance"));
      assertTrue("annotation layer", f.getSchema().getLayers().containsKey("word"));
      assertTrue("included layer", f.getSchema().getLayers().containsKey("phone"));
      assertTrue("included layer", f.getSchema().getLayers().containsKey("pos"));
      assertFalse("excluded layer", f.getSchema().getLayers().containsKey("phrase"));

      // check media provider handling
      try
      {
        assertEquals("graph has original media proviers",
                     "GraphMediaProvider", g.getMediaProvider().getMedia(null,null));
        assertEquals("fragment has derived media proviers",
                     "FragmentMediaProvider", f.getMediaProvider().getMedia(null,null));
      }
      catch(Exception exception)
      {
        fail("Media provider: " + exception);
      }
   }

   @Test public void fragmentByUtterance() 
   {
      Graph g = new Graph();
      g.setId("XXX");
      g.setMediaProvider(new IGraphMediaProvider() {
          public MediaFile[] getAvailableMedia() throws StoreException, PermissionException
          { return null; }
          public String getMedia(String trackSuffix, String mimeType)
            throws StoreException, PermissionException
          { return "GraphMediaProvider"; }
          public IGraphMediaProvider providerForGraph(Graph graph)
          {
            return new IGraphMediaProvider() {
              public MediaFile[] getAvailableMedia() throws StoreException, PermissionException
              { return null; }
              public String getMedia(String trackSuffix, String mimeType)
                throws StoreException, PermissionException
              { return "FragmentMediaProvider"; }
              public IGraphMediaProvider providerForGraph(Graph graph)
              { return this; }
            };
          }
        });

      g.addLayer(new Layer("topic", "Topics", 2, true, false, false));
      g.addLayer(new Layer("who", "Participants", 0, true, true, true));
      g.addLayer(new Layer("turn", "Speaker turns", 2, true, false, false, "who", true));
      g.addLayer(new Layer("utterance", "Utterances", 2, true, false, true, "turn", true));
      g.addLayer(new Layer("word", "Words", 2, true, false, false, "turn", true));
      g.addLayer(new Layer("phone", "Phones", 2, true, false, true, "word",  true));
      g.addLayer(new Layer("pos", "Part of speech", 0, false, false, true, "word", true));
      g.addLayer(new Layer("phrase", "Phrase structure", 0, true, true, false, "turn", true));

      g.getSchema().setParticipantLayerId("who");
      g.getSchema().setTurnLayerId("turn");
      g.getSchema().setUtteranceLayerId("utterance");
      g.getSchema().setWordLayerId("word");

      g.addAnchor(new Anchor("turnStart", 0.0));
      g.addAnchor(new Anchor("a1", 1.0));
      g.addAnchor(new Anchor("a1.5", 1.5));
      g.addAnchor(new Anchor("a2", 2.0));
       // include null offset anchor, it should still be added to the fragment
      g.addAnchor(new Anchor("a2.25", null));
      g.addAnchor(new Anchor("a2.5", 2.5));
      g.addAnchor(new Anchor("a2.75", 2.75));
      g.addAnchor(new Anchor("a3", 3.0));
      g.addAnchor(new Anchor("a4", 4.0));
      g.addAnchor(new Anchor("a5", 5.0));
      g.addAnchor(new Anchor("turnEnd", 6.0));

      Annotation who1 = new Annotation("who1", "john smith", "who", "turnStart", "turnEnd", "XXX");
      Annotation who2 = new Annotation("who2", "jane doe", "who", "turnStart", "turnEnd", "XXX");

      Annotation turn1 = new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "who1");

      Annotation utterance1 = new Annotation("utterance1", "john smith", "utterance", "turnStart", "a3", "turn1");
      Annotation utterance2 = new Annotation("utterance2", "john smith", "utterance", "a3", "turnEnd", "turn1");

      Annotation the = new Annotation("word1", "the", "word", "a1", "a2", "turn1");
      Annotation DT = new Annotation("pos1", "DT", "pos", "a1", "a2", "word1");
      Annotation th = new Annotation("phone1", "D", "phone", "a1", "a1.5", "word1");
      Annotation e = new Annotation("phone2", "@", "phone", "a1.5", "a2", "word1");
      Annotation quick = new Annotation("word2", "quick", "word", "a2", "a3", "turn1");
      Annotation A = new Annotation("pos2", "A", "pos", "a2", "a3", "word2");
      Annotation k = new Annotation("phone3", "k", "phone", "a2", "a2.25", "word2");
      Annotation w = new Annotation("phone4", "w", "phone", "a2.25", "a2.5", "word2");
      Annotation I = new Annotation("phone5", "I", "phone", "a2.5", "a2.75", "word2");
      Annotation ck = new Annotation("phone6", "k", "phone", "a2.75", "a3", "word2");
      Annotation brown = new Annotation("word3", "brown", "word", "a3", "a4", "turn1");
      Annotation AP = new Annotation("phrase1", "AP", "phrase", "a2", "a4", "turn1");
      Annotation fox = new Annotation("word4", "fox", "word", "a4", "a5", "turn1");
      Annotation N = new Annotation("pos3", "N", "pos", "a4", "a5", "word4");
      Annotation NP = new Annotation("phrase2", "NP", "phrase", "a1", "a5", "turn1");

      g.addAnnotation(who1);
      g.addAnnotation(turn1);
      g.addAnnotation(utterance1);
      g.addAnnotation(utterance2);

      g.addAnnotation(the);
      g.addAnnotation(quick);
      g.addAnnotation(brown);
      g.addAnnotation(fox);

      g.addAnnotation(th);
      g.addAnnotation(e);
      g.addAnnotation(k);
      g.addAnnotation(w);
      g.addAnnotation(I);
      g.addAnnotation(ck);

      g.addAnnotation(DT);
      g.addAnnotation(A);
      g.addAnnotation(N);

      g.addAnnotation(AP);
      g.addAnnotation(NP);

      assertFalse(g.isFragment());

      // create fragment
      Vector<String> layers = new Vector<String>();
      layers.add("turn"); // include ancestor layer - annotations should have no anchors
      layers.add("word");
      layers.add("phone");
      layers.add("pos");
      Graph f = g.getFragment(utterance1, layers.toArray(new String[0]));
      assertEquals("XXX__0.000-3.000", f.getId());
      assertEquals("fragment's graph is itself", f, f.getGraph());
      assertEquals("original graph is accessible", g, f.sourceGraph());
      assertTrue(f.isFragment());
      assertEquals("graph schema copied to fragment",
                  g.getSchema().getParticipantLayerId(), f.getSchema().getParticipantLayerId());
      assertEquals("graph schema copied to fragment",
                  g.getSchema().getTurnLayerId(), f.getSchema().getTurnLayerId());
      assertEquals("graph schema copied to fragment",
                  g.getSchema().getUtteranceLayerId(), f.getSchema().getUtteranceLayerId());
      assertEquals("graph schema copied to fragment",
                  g.getSchema().getWordLayerId(), f.getSchema().getWordLayerId());

      // check anchors
      assertTrue(f.getAnchors().containsKey("turnStart")); 
      assertTrue(f.getAnchors().containsKey("a1"));
      assertTrue(f.getAnchors().containsKey("a1.5"));
      assertTrue(f.getAnchors().containsKey("a2"));
      assertTrue(f.getAnchors().containsKey("a2.25"));
      assertTrue(f.getAnchors().containsKey("a2.5"));
      assertTrue(f.getAnchors().containsKey("a2.75"));
      assertTrue(f.getAnchors().containsKey("a3"));
      assertFalse(f.getAnchors().containsKey("a4"));
      assertFalse(f.getAnchors().containsKey("a5"));
      assertFalse(f.getAnchors().containsKey("turnEnd"));

      // check annotations
      assertTrue("speaker", f.getAnnotationsById().containsKey("who1"));
      assertTrue("turn", f.getAnnotationsById().containsKey("turn1"));
      assertTrue("defining utterance", f.getAnnotationsById().containsKey("utterance1"));
      assertFalse("utterance afterwards", f.getAnnotationsById().containsKey("utterance2"));

      assertTrue("words", f.getAnnotationsById().containsKey("word1"));
      assertTrue("words", f.getAnnotationsById().containsKey("word2"));
      assertFalse("word after", f.getAnnotationsById().containsKey("word3"));
      assertFalse("word after", f.getAnnotationsById().containsKey("word4"));

      assertTrue("phones", f.getAnnotationsById().containsKey("phone1"));
      assertTrue("phones", f.getAnnotationsById().containsKey("phone2"));
      assertTrue("phones", f.getAnnotationsById().containsKey("phone3"));
      assertTrue("phones", f.getAnnotationsById().containsKey("phone4"));
      assertTrue("phones", f.getAnnotationsById().containsKey("phone5"));
      assertTrue("phones", f.getAnnotationsById().containsKey("phone6"));

      assertTrue("pos", f.getAnnotationsById().containsKey("pos1"));
      assertTrue("pos", f.getAnnotationsById().containsKey("pos2"));
      assertFalse("other parent", f.getAnnotationsById().containsKey("pos3"));

      assertFalse("excluded layer", f.getAnnotationsById().containsKey("phrase1"));
      assertFalse("excluded layer", f.getAnnotationsById().containsKey("phrase2"));

      // check layers
      assertFalse("excluded layer", f.getSchema().getLayers().containsKey("topic"));
      assertTrue("ancestor layer", f.getSchema().getLayers().containsKey("who"));
      assertTrue("ancestor layer", f.getSchema().getLayers().containsKey("turn"));
      assertTrue("annotation layer", f.getSchema().getLayers().containsKey("utterance"));
      assertTrue("included layer", f.getSchema().getLayers().containsKey("word"));
      assertTrue("included layer", f.getSchema().getLayers().containsKey("phone"));
      assertTrue("included layer", f.getSchema().getLayers().containsKey("pos"));
      assertFalse("excluded layer", f.getSchema().getLayers().containsKey("phrase"));
      
      // check list
      Annotation[] words = f.list("word");
      assertEquals(2, words.length);

      // check media provider handling
      try
      {
        assertEquals("graph has original media proviers",
                     "GraphMediaProvider", g.getMediaProvider().getMedia(null,null));
        assertEquals("fragment has derived media proviers",
                     "FragmentMediaProvider", f.getMediaProvider().getMedia(null,null));
      }
      catch(Exception exception)
      {
        fail("Media provider: " + exception);
      }
   }

   @Test public void fragmentByBoundingAndAncestorAnnotations() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      g.setMediaProvider(new IGraphMediaProvider() {
          public MediaFile[] getAvailableMedia() throws StoreException, PermissionException
          { return null; }
          public String getMedia(String trackSuffix, String mimeType)
            throws StoreException, PermissionException
          { return "GraphMediaProvider"; }
          public IGraphMediaProvider providerForGraph(Graph graph)
          {
            return new IGraphMediaProvider() {
              public MediaFile[] getAvailableMedia() throws StoreException, PermissionException
              { return null; }
              public String getMedia(String trackSuffix, String mimeType)
                throws StoreException, PermissionException
              { return "FragmentMediaProvider"; }
              public IGraphMediaProvider providerForGraph(Graph graph)
              { return this; }
            };
          }
        });

      g.addLayer(new Layer("topic", "Topics", 2, true, false, false));
      g.addLayer(new Layer("who", "Participants", 0, true, true, true));
      g.addLayer(new Layer("turn", "Speaker turns", 2, true, false, false, "who", true));
      g.addLayer(new Layer("utterance", "Utterances", 2, true, false, true, "turn", true));
      g.addLayer(new Layer("word", "Words", 2, true, false, false, "turn",true));
      g.addLayer(new Layer("phone", "Phones", 2, true, false, true, "word",  true));
      g.addLayer(new Layer("pos", "Part of speech", 0, false, false, true, "word", true));
      g.addLayer(new Layer("phrase", "Phrase structure", 0, true, true, false, "turn", true));

      g.getSchema().setParticipantLayerId("who");
      g.getSchema().setTurnLayerId("turn");
      g.getSchema().setUtteranceLayerId("utterance");
      g.getSchema().setWordLayerId("word");

      g.addAnchor(new Anchor("turnStart", 0.0));
      g.addAnchor(new Anchor("a1", 1.0));
      g.addAnchor(new Anchor("a1.5", 1.5));
      g.addAnchor(new Anchor("a2", 2.0));
       // include null offset anchor, it should still be added to the fragment
      g.addAnchor(new Anchor("a2.25", null));
      g.addAnchor(new Anchor("a2.5", 2.5));
      g.addAnchor(new Anchor("a2.75", 2.75));
      g.addAnchor(new Anchor("a3", 3.0));
      g.addAnchor(new Anchor("a4", 4.0));
      g.addAnchor(new Anchor("a5", 5.0));
      g.addAnchor(new Anchor("turnEnd", 6.0));

      Annotation who1 = new Annotation("who1", "john smith", "who", "turnStart", "turnEnd", "my graph");
      Annotation who2 = new Annotation("who1", "jane doe", "who", "turnStart", "turnEnd", "my graph");

      Annotation turn1 = new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "who1");

      Annotation utterance1 = new Annotation("utterance1", "john smith", "utterance", "turnStart", "a3", "turn1");
      Annotation utterance2 = new Annotation("utterance2", "john smith", "utterance", "a3", "turnEnd", "turn1");

      Annotation the = new Annotation("word1", "the", "word", "a1", "a2", "turn1");
      Annotation DT = new Annotation("pos1", "DT", "pos", "a1", "a2", "word1");
      Annotation th = new Annotation("phone1", "D", "phone", "a1", "a1.5", "word1");
      Annotation e = new Annotation("phone2", "@", "phone", "a1.5", "a2", "word1");
      Annotation quick = new Annotation("word2", "quick", "word", "a2", "a3", "turn1");
      Annotation A = new Annotation("pos2", "A", "pos", "a2", "a3", "word2");
      Annotation k = new Annotation("phone3", "k", "phone", "a2", "a2.25", "word2");
      Annotation w = new Annotation("phone4", "w", "phone", "a2.25", "a2.5", "word2");
      Annotation I = new Annotation("phone5", "I", "phone", "a2.5", "a2.75", "word2");
      Annotation ck = new Annotation("phone6", "k", "phone", "a2.75", "a3", "word2");
      Annotation brown = new Annotation("word3", "brown", "word", "a3", "a4", "turn1");
      Annotation AP = new Annotation("phrase1", "AP", "phrase", "a2", "a4", "turn1");
      Annotation fox = new Annotation("word4", "fox", "word", "a4", "a5", "turn1");
      Annotation N = new Annotation("pos3", "N", "pos", "a4", "a5", "word4");
      Annotation NP = new Annotation("phrase2", "NP", "phrase", "a1", "a5", "turn1");

      g.addAnnotation(who1);
      g.addAnnotation(turn1);
      g.addAnnotation(utterance1);
      g.addAnnotation(utterance2);

      g.addAnnotation(the);
      g.addAnnotation(quick);
      g.addAnnotation(brown);
      g.addAnnotation(fox);

      g.addAnnotation(th);
      g.addAnnotation(e);
      g.addAnnotation(k);
      g.addAnnotation(w);
      g.addAnnotation(I);
      g.addAnnotation(ck);

      g.addAnnotation(DT);
      g.addAnnotation(A);
      g.addAnnotation(N);

      g.addAnnotation(AP);
      g.addAnnotation(NP);

      assertFalse(g.isFragment());

      // create fragment
      Vector<String> layers = new Vector<String>();
      layers.add("utterance");
      layers.add("phone");
      layers.add("pos");
      Graph f = g.getFragment(utterance1, turn1, layers.toArray(new String[0]));
      assertEquals("my graph__0.000-3.000", f.getId());
      assertEquals("fragment's graph is itself", f, f.getGraph());
      assertEquals("original graph is accessible", g, f.sourceGraph());
      assertTrue(f.isFragment());
      assertEquals("graph schema copied to fragment",
                  g.getSchema().getParticipantLayerId(), f.getSchema().getParticipantLayerId());
      assertEquals("graph schema copied to fragment",
                  g.getSchema().getTurnLayerId(), f.getSchema().getTurnLayerId());
      assertEquals("graph schema copied to fragment",
                  g.getSchema().getUtteranceLayerId(), f.getSchema().getUtteranceLayerId());
      assertEquals("graph schema copied to fragment",
                  g.getSchema().getWordLayerId(), f.getSchema().getWordLayerId());

      // check anchors
      assertTrue(f.getAnchors().containsKey("turnStart")); 
      assertTrue(f.getAnchors().containsKey("a1"));
      assertTrue(f.getAnchors().containsKey("a1.5"));
      assertTrue(f.getAnchors().containsKey("a2"));
      assertTrue(f.getAnchors().containsKey("a2.25"));
      assertTrue(f.getAnchors().containsKey("a2.5"));
      assertTrue(f.getAnchors().containsKey("a2.75"));
      assertTrue(f.getAnchors().containsKey("a3"));
      assertFalse(f.getAnchors().containsKey("a4"));
      assertFalse(f.getAnchors().containsKey("a5"));
      assertFalse(f.getAnchors().containsKey("turnEnd"));

      // check annotations
      assertTrue("speaker", f.getAnnotationsById().containsKey("who1"));
      assertTrue("turn", f.getAnnotationsById().containsKey("turn1"));
      assertTrue("utterance", f.getAnnotationsById().containsKey("utterance1"));
      assertFalse("utterance afterwards", f.getAnnotationsById().containsKey("utterance2"));

      assertTrue("ancestor word", f.getAnnotationsById().containsKey("word1"));
      assertTrue("ancestor word", f.getAnnotationsById().containsKey("word2"));
      assertFalse("word after", f.getAnnotationsById().containsKey("word3"));
      assertFalse("word after", f.getAnnotationsById().containsKey("word4"));

      assertTrue("included", f.getAnnotationsById().containsKey("phone1"));
      assertTrue("included", f.getAnnotationsById().containsKey("phone2"));
      assertTrue("included", f.getAnnotationsById().containsKey("phone3"));
      assertTrue("included", f.getAnnotationsById().containsKey("phone4"));
      assertTrue("included", f.getAnnotationsById().containsKey("phone5"));
      assertTrue("included", f.getAnnotationsById().containsKey("phone6"));

      assertTrue("included", f.getAnnotationsById().containsKey("pos1"));
      assertTrue("included", f.getAnnotationsById().containsKey("pos2"));
      assertFalse("after bounds", f.getAnnotationsById().containsKey("pos3"));

      assertFalse("excluded layer", f.getAnnotationsById().containsKey("phrase1"));
      assertFalse("excluded layer", f.getAnnotationsById().containsKey("phrase2"));

      // check layers
      assertFalse("excluded layer", f.getSchema().getLayers().containsKey("topic"));
      assertTrue("ancestor layer", f.getSchema().getLayers().containsKey("who"));
      assertTrue("ancestor layer", f.getSchema().getLayers().containsKey("turn"));
      assertTrue("included", f.getSchema().getLayers().containsKey("utterance"));
      assertTrue("ancestor layer", f.getSchema().getLayers().containsKey("word"));
      assertTrue("included layer", f.getSchema().getLayers().containsKey("phone"));
      assertTrue("included layer", f.getSchema().getLayers().containsKey("pos"));
      assertFalse("excluded layer", f.getSchema().getLayers().containsKey("phrase"));

      // check media provider handling
      try
      {
        assertEquals("graph has original media proviers",
                     "GraphMediaProvider", g.getMediaProvider().getMedia(null,null));
        assertEquals("fragment has derived media proviers",
                     "FragmentMediaProvider", f.getMediaProvider().getMedia(null,null));
      }
      catch(Exception exception)
      {
        fail("Media provider: " + exception);
      }
   }

   @Test public void orderOfAnnotationAnchorLinking() 
   {
      Graph g = new Graph();
      g.setId("my graph");

      g.addLayer(new Layer("test", "test", 2, true, false, false));

      g.addAnchor(new Anchor("start1", 0.0));
      g.addAnchor(new Anchor("end1", 1.0));

      g.addAnchor(new Anchor("start2", 100.0));
      g.addAnchor(new Anchor("end2", 101.0));

      Annotation test = new Annotation("test1", "test", "test", "start1", "end1", "my graph");

      assertFalse("unlinked", g.getAnchor("start1").startOf("test").contains(test));
      assertFalse("unlinked", g.getAnchor("end1").endOf("test").contains(test));
      assertFalse("unlinked", g.getAnchor("start2").startOf("test").contains(test));
      assertFalse("unlinked", g.getAnchor("end2").endOf("test").contains(test));

      g.addAnnotation(test);

      assertTrue("linked by Graph.addAnnotation()", g.getAnchor("start1").startOf("test").contains(test));
      assertTrue("linked by Graph.addAnnotation()", g.getAnchor("end1").endOf("test").contains(test));
      assertFalse("not linked yet", g.getAnchor("start2").startOf("test").contains(test));
      assertFalse("not linked yet", g.getAnchor("end2").endOf("test").contains(test));

      test.setStartId("start2");
      test.setEndId("end2");

      assertFalse("unlinked by Annotation.setStartId()", g.getAnchor("start1").startOf("test").contains(test));
      assertFalse("unlinked by Annotation.setEndId()", g.getAnchor("end1").endOf("test").contains(test));
      assertTrue("linked by Annotation.setStartId()", g.getAnchor("start2").startOf("test").contains(test));
      assertTrue("linked by Annotation.setEndId()", g.getAnchor("end2").endOf("test").contains(test));

      test.setStart(g.getAnchor("start1"));
      test.setEnd(g.getAnchor("end1"));

      assertTrue("linked by Annotation.setStart()", g.getAnchor("start1").startOf("test").contains(test));
      assertTrue("linked by Annotation.setEnd()", g.getAnchor("end1").endOf("test").contains(test));
      assertFalse("unlinked by Annotation.setStart()", g.getAnchor("start2").startOf("test").contains(test));
      assertFalse("unlinked  by Annotation.setEnd()", g.getAnchor("end2").endOf("test").contains(test));

      test.setStartId(null);
      test.setEndId(null);

      assertFalse("unlinked - null anchors", g.getAnchor("start1").startOf("test").contains(test));
      assertFalse("unlinked - null anchors", g.getAnchor("end1").endOf("test").contains(test));
      assertFalse("unlinked - null anchors", g.getAnchor("start2").startOf("test").contains(test));
      assertFalse("unlinked - null anchors", g.getAnchor("end2").endOf("test").contains(test));

   }

   @Test public void myAndList() 
   {
      Graph g = new Graph();
      g.setId("my graph");

      g.addLayer(new Layer("topic", "Topics", 2, true, false, false));
      g.addLayer(new Layer("corpus", "Corpus", 0, false, false, true));
      g.addLayer(new Layer("who", "Participants", 0, true, true, true));
      g.addLayer(new Layer("turn", "Speaker turns", 2, true, false, false, "who", true));
      g.addLayer(new Layer("utterance", "Utterances", 2, true, false, true, "turn", true));
      g.addLayer(new Layer("word", "Words", 2, true, false, false, "turn",true));
      g.addLayer(new Layer("phone", "Phones", 2, true, false, true, "word",  true));
      g.addLayer(new Layer("pos", "Part of speech", 0, false, false, true, "word", true));
      g.addLayer(new Layer("phrase", "Phrase structure", 0, true, true, false, "turn", true));

      g.addAnchor(new Anchor("turnStart", 0.0));
      g.addAnchor(new Anchor("a1", 1.0));
      g.addAnchor(new Anchor("a1.5", 1.5));
       // include null offset anchor, it should still be added to the fragment
      g.addAnchor(new Anchor("a2", null));
      g.addAnchor(new Anchor("a2.2", 2.2));
      g.addAnchor(new Anchor("a2.22", 2.22));
      g.addAnchor(new Anchor("a2.23", 2.23));
      g.addAnchor(new Anchor("a2.24", 2.24));
      g.addAnchor(new Anchor("a2.25", 2.25));
      g.addAnchor(new Anchor("a2.5", 2.5));
      g.addAnchor(new Anchor("a2.75", 2.75));
      g.addAnchor(new Anchor("a3", 3.0));
      g.addAnchor(new Anchor("a3.2", 3.2));
      g.addAnchor(new Anchor("a4", 4.0));
      g.addAnchor(new Anchor("a5", 5.0));
      g.addAnchor(new Anchor("turnEnd", 6.0));

      Annotation corpus = new Annotation("corpus1", "CC", "corpus", "turnStart", "turnEnd", "my graph");
      Annotation who1 = new Annotation("who1", "john smith", "who", "turnStart", "turnEnd", "my graph");
      Annotation who2 = new Annotation("who2", "jane doe", "who", "turnStart", "turnEnd", "my graph");

      Annotation turn1 = new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "who1");
      Annotation turn2 = new Annotation("turn2", "jane doe", "turn", "turnStart", "turnEnd", "who2");

      Annotation utterance1 = new Annotation("utterance1", "john smith", "utterance", "turnStart", "a3", "turn1");
      Annotation utterance2 = new Annotation("utterance2", "john smith", "utterance", "a3", "turnEnd", "turn1");

      Annotation utterance3 = new Annotation("utterance3", "jane doe", "utterance", "startStart", "turnEnd", "turn2");

      Annotation the = new Annotation("word1", "the", "word", "a1", "a2", "turn1");
      Annotation DT = new Annotation("pos1", "DT", "pos", "a1", "a2", "word1");
      Annotation th = new Annotation("phone1", "D", "phone", "a1", "a1.5", "word1");
      Annotation e = new Annotation("phone2", "@", "phone", "a1.5", "a2", "word1");
      Annotation quick = new Annotation("word2", "quick", "word", "a2", "a3", "turn1");
      Annotation A = new Annotation("pos2", "A", "pos", "a2", "a3", "word2");
      Annotation k = new Annotation("phone3", "k", "phone", "a2", "a2.25", "word2");
      Annotation w = new Annotation("phone4", "w", "phone", "a2.25", "a2.5", "word2");
      Annotation I = new Annotation("phone5", "I", "phone", "a2.5", "a2.75", "word2");
      Annotation ck = new Annotation("phone6", "k", "phone", "a2.75", "a3", "word2");
      Annotation brown = new Annotation("word3", "brown", "word", "a3", "a4", "turn1");
      Annotation AP = new Annotation("phrase1", "AP", "phrase", "a2", "a4", "turn1");
      Annotation fox = new Annotation("word4", "fox", "word", "a4", "a5", "turn1");
      Annotation N = new Annotation("pos3", "N", "pos", "a4", "a5", "word4");
      Annotation NP = new Annotation("phrase2", "NP", "phrase", "a1", "a5", "turn1");
      // other speaker
      Annotation yes = new Annotation("word5", "yes", "word", "a2.2", "a3.2", "turn2");
      Annotation y = new Annotation("phone7", "y", "phone", "a2.2", "a2.22", "word5");
      Annotation e2 = new Annotation("phone8", "e", "phone", "a2.22", "a2.23", "word5");
      Annotation s = new Annotation("phone9", "s", "phone", "a2.24", "a3.2", "word5");

      g.addAnnotation(corpus);
      g.addAnnotation(who1);
      g.addAnnotation(turn1);
      g.addAnnotation(utterance1);
      g.addAnnotation(utterance2);

      g.addAnnotation(the);
      g.addAnnotation(quick);
      g.addAnnotation(brown);
      g.addAnnotation(fox);

      g.addAnnotation(th);
      g.addAnnotation(e);
      g.addAnnotation(k);
      g.addAnnotation(w);
      g.addAnnotation(I);
      g.addAnnotation(ck);
      g.addAnnotation(y);
      g.addAnnotation(e2);
      g.addAnnotation(s);

      g.addAnnotation(DT);
      g.addAnnotation(A);
      g.addAnnotation(N);

      g.addAnnotation(AP);
      g.addAnnotation(NP);

      g.addAnnotation(who2);
      g.addAnnotation(turn2);
      g.addAnnotation(utterance3);
      g.addAnnotation(yes);

      assertEquals("layerId: graph", "graph", g.getLayerId());
      assertEquals("my: graph", g, g.my("graph"));

      assertEquals("my: parent", turn1, the.my("turn"));
      assertEquals("my: ancestor", who1, the.my("who"));
      assertEquals("my: graph", g, the.my("graph"));
      assertEquals("my: child", th, the.my("phone"));
      assertNull("my: none", fox.my("phone"));

      assertEquals("my: parent - other speaker", turn2, yes.my("turn"));
      assertEquals("my: ancestor - other speaker", who2, yes.my("who"));
      assertEquals("my: graph - other speaker", g, yes.my("graph"));
      assertEquals("my: child - other speaker", yes, turn2.my("word"));
      assertNull("my: none - other speaker", yes.my("pos"));

      assertEquals("my: ancestor child (peer layers)", utterance1, the.my("utterance"));
      assertEquals("my: ancestor child (non-peers)", utterance1, th.my("utterance"));
      assertEquals("my: ancestor child (child of graph)", corpus, the.my("corpus"));

      Annotation[] list = the.list("turn");
      assertEquals("list: parent", turn1, list[0]);
      assertEquals("list: parent", 1, list.length);
      list = the.list("who");
      assertEquals("list: ancestor", who1, list[0]);
      assertEquals("list: ancestor", 1, list.length);
      list = the.list("graph");
      assertEquals("list: graph", g, list[0]);
      assertEquals("list: graph", 1, list.length);
      assertEquals("list: child", the.annotations("phone"), the.list("phone"));
      assertEquals("list: none", 0, fox.list("phone").length);

      list = the.list("utterance");
      assertEquals("list: ancestor child (peer layers)", utterance1, list[0]);
      assertEquals("list: ancestor child (peer layers)", 1, list.length);
      list = utterance1.list("word");
      assertEquals("list: ancestor child (peer layers) - reflexive", 2, list.length);
      assertEquals("list: ancestor child (peer layers) - reflexive", the, list[0]);
      assertEquals("list: ancestor child (peer layers) - reflexive", quick, list[1]);
      list = th.list("utterance");
      assertEquals("list: ancestor child (non-peers)", utterance1, list[0]);
      assertEquals("list: ancestor child (non-peers)", 1, list.length);
      list = utterance1.list("phone");
      assertEquals("list: ancestor child (non-peers) - reflexive", 6, list.length);
      assertEquals("list: ancestor child (non-peers) - reflexive", th, list[0]);
      assertEquals("list: ancestor child (non-peers) - reflexive", e, list[1]);
      assertEquals("list: ancestor child (non-peers) - reflexive", k, list[2]);
      assertEquals("list: ancestor child (non-peers) - reflexive", w, list[3]);
      assertEquals("list: ancestor child (non-peers) - reflexive", I, list[4]);
      assertEquals("list: ancestor child (non-peers) - reflexive", ck, list[5]);
      
      list = yes.list("turn");
      assertEquals("list: parent - other speaker", turn2, list[0]);
      assertEquals("list: parent - other speaker", 1, list.length);
      list = yes.list("who");
      assertEquals("list: ancestor - other speaker", who2, list[0]);
      assertEquals("list: ancestor - other speaker", 1, list.length);
      list = yes.list("graph");
      assertEquals("list: graph - other speaker", g, list[0]);
      assertEquals("list: graph - other speaker", 1, list.length);
      assertEquals("list: child - other speaker", turn2.annotations("word"), turn2.list("word"));
      assertEquals("list: none - other speaker", 0, yes.list("pos").length);

      list = turn1.list("word");

      list = g.list("word");
      assertEquals("list: graph", the, list[0]);
      assertEquals("list: graph", quick, list[1]);
      assertEquals("list: graph", brown, list[2]);
      assertEquals("list: graph", fox, list[3]);
      assertEquals("list: graph", yes, list[4]);
      assertEquals("list: graph", 5, list.length);

      list = turn1.list("pos");
      assertEquals("list: grandchild", DT, list[0]);
      assertEquals("list: grandchild", A, list[1]);
      assertEquals("list: grandchild", N, list[2]);
      assertEquals("list: grandchild", 3, list.length);

      list = g.list("pos");
      assertEquals("list: distant descenant", DT, list[0]);
      assertEquals("list: distant descenant", A, list[1]);
      assertEquals("list: distant descenant", N, list[2]);
      assertEquals("list: distant descenant", 3, list.length);

      list = g.list("phone");
      assertEquals("list: distant descenant", th, list[0]);
      assertEquals("list: distant descenant", e, list[1]);
      assertEquals("list: distant descenant", k, list[2]);
      assertEquals("list: distant descenant", w, list[3]);
      assertEquals("list: distant descenant", I, list[4]);
      assertEquals("list: distant descenant", ck, list[5]);
      assertEquals("list: distant descenant", y, list[6]);
      assertEquals("list: distant descenant", e2, list[7]);
      assertEquals("list: distant descenant", s, list[8]);
      assertEquals("list: distant descenant", 9, list.length);

      list = the.list("graph");
      assertEquals("annotation.list('graph') contains one element", 1, list.length);
      assertEquals("annotation.list('graph') contains the graph", g, list[0]);

      list = g.list("graph");
      assertEquals("graph.list('graph') contains one element", 1, list.length);
      assertEquals("graph.list('graph') contains the graph", g, list[0]);
   }

   @Test public void easyAnchorChaining() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      
      g.addLayer(new Layer("test", "test", 2, true, false, false));
      
      g.addAnchor(new Anchor("start1", 0.0));
      g.addAnnotation(new Annotation("the", "the", "test"));
      g.addAnnotation(new Annotation("quick", "quick", "test"));
      g.addAnnotation(new Annotation("brown", "brown", "test"));
      g.addAnnotation(new Annotation("fox", "fox", "test"));
      g.addAnchor(new Anchor("end2", 101.0));
      
      assertEquals("previous addAnchor becomes start anchor", 
		   "start1", g.getAnnotation("the").getStartId());
      assertEquals("chaining anchors automatically created", 
		   g.getAnnotation("the").getEndId(), g.getAnnotation("quick").getStartId());
      assertEquals("chaining anchors automatically created", 
		   g.getAnnotation("quick").getEndId(), g.getAnnotation("brown").getStartId());
      assertEquals("chaining anchors automatically created", 
		   g.getAnnotation("brown").getEndId(), g.getAnnotation("fox").getStartId());

      assertNull("chaining anchors with no offset", g.getAnnotation("the").getEnd().getOffset());
      assertNull("chaining anchors with no offset", g.getAnnotation("quick").getEnd().getOffset());
      assertNull("chaining anchors with no offset", g.getAnnotation("brown").getEnd().getOffset());
      assertNull("chaining anchors with no offset", g.getAnnotation("fox").getEnd().getOffset());
      
   }
  
   @Test public void compareOffsets1000() 
   {
      Graph g = new Graph();
      g.setOffsetGranularity(0.001);
      assertEquals("big difference +ve",
                   1, g.compareOffsets(0.5, 0.4));
      assertEquals("big difference -ve",
                   -1, g.compareOffsets(0.4, 0.5));
      assertEquals("equal",
                   0, g.compareOffsets(0.5, 0.5));
      assertEquals("small difference +ve",
                   1, g.compareOffsets(0.005, 0.004));
      assertEquals("small difference -ve",
                   -1, g.compareOffsets(0.004, 0.005));
      assertEquals("under threshold +ve",
                   0, g.compareOffsets(0.0049, 0.004));
      assertEquals("under threshold -ve",
                   -0, g.compareOffsets(0.004, 0.0049));
   }

   @Test public void compareOffsets16000() 
   {
     Graph g = new Graph();
     g.setOffsetGranularity(1.0/16000.0); // 0.0000625
     assertEquals("big difference +ve",
                  1, g.compareOffsets(0.5, 0.4));
     assertEquals("big difference -ve",
                  -1, g.compareOffsets(0.4, 0.5));
     assertEquals("equal",
                  0, g.compareOffsets(0.5, 0.5));
     assertEquals("small difference +ve",
                  1, g.compareOffsets(0.0050625, 0.005));
     assertEquals("small difference -ve",
                  -1, g.compareOffsets(0.005, 0.0050625));
     assertEquals("under threshold +ve",
                  0, g.compareOffsets(0.0050624, 0.005));
     assertEquals("under threshold -ve",
                  -0, g.compareOffsets(0.005, 0.0050624));
   }

   @Test public void getAnchorAt() 
   {
     Graph g = new Graph();
     Anchor a = g.addAnchor(new Anchor(null, 0.005));
     assertEquals("equal",
                  a, g.getAnchorAt(0.005));
     assertNull("big difference",
                g.getAnchorAt(0.004));
     assertNull("small difference",
                g.getAnchorAt(0.0050625));
     assertNull("smaller difference",
                g.getAnchorAt(0.0050624));
     
     g.setOffsetGranularity(1.0/16000.0); // 0.0000625
     
     assertEquals("16kHz granularity: equal",
                  a, g.getAnchorAt(0.005));
     assertNull("16kHz granularity: big difference",
                g.getAnchorAt(0.004));
     assertNull("16kHz granularity: small difference",
                g.getAnchorAt(0.0050625));
     assertEquals("16kHz granularity: negligible difference",
                  a, g.getAnchorAt(0.0050624));
     
   }

   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.test.TestGraph");
   }
}
