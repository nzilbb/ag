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
import nzilbb.ag.*;

public class TestAnnotation 
{
   @Test public void basicAttributes() 
   {
      // default constructor
      Annotation a = new Annotation();
      a.setId("123");
      a.setLayerId("word");
      a.setLabel("LABEL");
      a.setStartId("start");
      a.setEndId("end");
      a.setParentId("parent");
      a.setOrdinal(99);
      assertEquals("123", a.getId());
      assertEquals("word", a.getLayerId());
      assertEquals("LABEL", a.toString());
      assertEquals("start", a.getStartId());
      assertEquals("end", a.getEndId());
      assertEquals("parent", a.getParentId());
      assertEquals(99, a.getOrdinal());

      // basic constructor
      a = new Annotation("123", "LABEL", "word");
      assertEquals("123", a.getId());
      assertEquals("word", a.getLayerId());
      assertEquals("LABEL", a.toString());

      // ...with start/end
      a = new Annotation("123", "LABEL", "word", "start", "end");
      assertEquals("123", a.getId());
      assertEquals("LABEL", a.toString());
      assertEquals("start", a.getStartId());
      assertEquals("end", a.getEndId());

      // ...and parent
      a = new Annotation("123", "LABEL", "word", "start", "end", "parent");
      assertEquals("123", a.getId());
      assertEquals("LABEL", a.toString());
      assertEquals("start", a.getStartId());
      assertEquals("end", a.getEndId());
      assertEquals("parent", a.getParentId());

      // ...and ordinal
      a = new Annotation("123", "LABEL", "word", "start", "end", "parent", 99);
      assertEquals("123", a.getId());
      assertEquals("LABEL", a.toString());
      assertEquals("start", a.getStartId());
      assertEquals("end", a.getEndId());
      assertEquals("parent", a.getParentId());
      assertEquals(99, a.getOrdinal());
   }

   @Test public void extendedAttributes() 
   {
      Annotation a = new Annotation("123", "LABEL", "word");
      a.put("labelStatus", 50);
      assertEquals(Integer.valueOf(50), a.get("labelStatus"));      
   }

   @Test public void objectAttributes() 
   {
      Annotation a = new Annotation();
      // TODO define before-id hashcode behaviour
      a.setId("123");
      int iStartHashCode = a.hashCode();
      a.setLayerId("word");
      a.setLabel("LABEL");
      a.put("foo", "bar");
      int iEndHashCode = a.hashCode();
      assertEquals("Immutable hashcode:", iStartHashCode, iEndHashCode);
      assertEquals("LABEL", a.toString());
      
      assertTrue("Equality reflexive", a.equals(a));
      Annotation a2 = new Annotation();
      assertFalse("equals before id set:", a.equals(a2));
      a2.setId("123");
      a2.setLayerId("word");
      a2.setLabel("LABEL");
      // no "foo" attribute, to ensure it doesn't contribute to equality
      assertTrue("id defines equality:", a.equals(a2));
      assertTrue("Equality is symmetric:", a2.equals(a));
      
      a2.setLabel("DIFFERENT LABEL");
      assertTrue("label doesn't affect equality:", a.equals(a2));
      a2.setLabel(a.getLabel());
		  
      a2.setLayerId("different layer");
      assertTrue("layer doesn't affect equality:", a.equals(a2));
      a2.setLayerId(a.getLayerId());

      a2.setId("different id");
      assertFalse("Different id:", a.equals(a2));
      a2.setId(a.getId());
      assertTrue("Resetting attribute resets equality:", a.equals(a2));

      LinkedHashMap<String,Object> aMap = new LinkedHashMap<String,Object>();
      aMap.putAll(a);
      assertFalse("A map with the same attributes isn't equal:", a.equals(aMap));
   }

   @Test public void changeTracking() 
   {
      Annotation a = new Annotation("123", "LABEL", "word", "start", "end", "parent", 99);
      assertEquals("LABEL", a.getLabel());
      assertEquals("Offset and original are the same", a.getLabel(), a.getOriginalLabel());
      assertEquals(Change.Operation.NoChange, a.getChange());
      assertFalse(a.containsKey("originalLabel"));

      a.put("foo", "foo");
      assertEquals("Non-tracked keys don't affect change:", Change.Operation.NoChange, a.getChange());

      a.setId("differentId");
      assertEquals("differentId", a.getId());
      assertEquals("Non-tracked attributes don't affect change - id:", Change.Operation.NoChange, a.getChange());
      a.setLayerId("differentLayer");
      assertEquals("differentLayer", a.getLayerId());
      assertEquals("Non-tracked attributes don't affect change - layer:", Change.Operation.NoChange, a.getChange());

      a.setLabel("NEW LABEL");
      assertEquals("NEW LABEL", a.getLabel());
      assertEquals("Original value attribute:", "LABEL", a.getOriginalLabel());
      assertEquals("Original value in map:", "LABEL", a.get("originalLabel"));
      assertEquals(Change.Operation.Update, a.getChange());

      a.rollback();
      assertEquals("Rollback:", "LABEL", a.getLabel());
      assertEquals("Original value attribute:", "LABEL", a.getOriginalLabel());
      assertFalse("Original value no longer in map:", a.containsKey("originalLabel"));
      assertEquals(Change.Operation.NoChange, a.getChange());

      a.setStartId("new start");
      assertEquals("new start", a.getStartId());
      assertEquals("Original value attribute:", "start", a.getOriginalStartId());
      assertEquals("Original value in map:", "start", a.get("originalStartId"));
      assertEquals(Change.Operation.Update, a.getChange());

      a.commit();
      assertEquals("new start", a.getStartId());
      assertEquals("Committed original attribute:", "new start", a.getOriginalStartId());
      assertFalse("Original value no longer in map:", a.containsKey("originalStartId"));
      assertEquals(Change.Operation.NoChange, a.getChange());

      a.setEndId("new end");
      assertEquals("new end", a.getEndId());
      assertEquals("Original value attribute:", "end", a.getOriginalEndId());
      assertEquals("Original value in map:", "end", a.get("originalEndId"));
      assertEquals(Change.Operation.Update, a.getChange());

      a.rollback();
      assertEquals(Change.Operation.NoChange, a.getChange());

      a.setParentId("new parent");
      assertEquals("new parent", a.getParentId());
      assertEquals("Original value attribute:", "parent", a.getOriginalParentId());
      assertEquals("Original value in map:", "parent", a.get("originalParentId"));
      assertEquals(Change.Operation.Update, a.getChange());

      a.rollback();
      assertEquals(Change.Operation.NoChange, a.getChange());

      a.setOrdinal(1000);
      assertEquals(1000, a.getOrdinal());
      assertEquals("Original value attribute:", 99, a.getOriginalOrdinal());
      assertEquals("Original value in map:", Integer.valueOf(99), a.get("originalOrdinal"));
      assertEquals(Change.Operation.Update, a.getChange());

      a.create();
      assertEquals("Create trumps Update as a change", Change.Operation.Create, a.getChange());

      a.destroy();
      assertEquals("Destroy trumps Create as a change", Change.Operation.Destroy, a.getChange());

      a.rollback();
      assertEquals("Create cannot be rolled back:", Change.Operation.Create, a.getChange());

   }

   @Test public void cloning() 
   {
      Annotation a = new Annotation("123", "LABEL", "word", "start", "end", "parent", 99);
      a.put("foo", "foo");
      a.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
      Annotation c = (Annotation)a.clone();
      assertEquals("123", c.getId());
      assertEquals("LABEL", c.getLabel());
      assertEquals("word", c.getLayerId());
      assertEquals("start", c.getStartId());
      assertEquals("end", c.getEndId());
      assertEquals("parent", c.getParentId());
      assertEquals(99, c.getOrdinal());     
      assertEquals(Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC), c.getConfidence());
      assertFalse(c.containsKey("foo"));     
   }

   @Test public void copyConstructor() 
   {
      Annotation a = new Annotation("123", "LABEL", "word", "start", "end", "parent", 99);
      a.put("foo", "foo");
      a.put("@bar", "bar");
      a.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
      Annotation c = new Annotation(a);
      assertNull(c.getId());
      assertEquals("LABEL", c.getLabel());
      assertEquals("word", c.getLayerId());
      assertEquals("start", c.getStartId());
      assertEquals("end", c.getEndId());
      assertEquals("parent", c.getParentId());
      assertEquals(99, c.getOrdinal());     
      assertEquals(Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC), c.getConfidence());
      assertEquals("foo", c.get("foo"));     
      assertFalse(c.containsKey("@bar"));     
   }

   @Test public void instantaneous() 
   {
      Annotation a = new Annotation("123", "LABEL", "word", "start", "end", "parent", 99);
      assertFalse(a.getInstantaneous());
      a.setEndId("start");
      assertTrue(a.getInstantaneous());
   }
      
   @Test public void anchoringAndInclusion() 
   {
      Annotation test = new Annotation("test", "test", "word", "start", "end", "parent", 99);
      Anchor start = new Anchor("start", 1.0);
      Anchor end = new Anchor("end", 2.0);
      // anchors not available yet
      assertFalse("Anchors inaccessible", test.includesOffset(1.5));

      test.setStart(start);
      test.setEnd(end);
      // anchors still not available, because they have to be in a graph to be looked up
      assertFalse("Anchors inaccessible", test.includesOffset(1.5));

      Graph graph = new Graph();
      graph.addAnchor(start);
      graph.addAnchor(end);
      graph.addAnnotation(test);
      // now it should work
      assertTrue("Anchors accessible", test.includesOffset(1.5));
      assertTrue("Start offset included", test.includesOffset(1.0));
      assertFalse("End offset excluded", test.includesOffset(2.0));
      assertFalse("Lower offset excluded", test.includesOffset(0.5));
      assertFalse("Higher offset excluded", test.includesOffset(2.5));

      // null offsets
      start.setOffset(null);
      assertFalse("Null start offset", test.includesOffset(1.5));
      start.rollback();
      end.setOffset(null);
      assertFalse("Null end offset", test.includesOffset(1.5));
      end.rollback();

      // tags(Annotation) and includes(Annotation)
      Annotation parallel = new Annotation("parallel", "parallel", "word", "start", "end", "parent", 99);
      Annotation startInstant = new Annotation("startInstant", "startInstant", "word", "start", "start", "parent", 99);
      Annotation endInstant = new Annotation("endInstant", "endInstant", "word", "end", "end", "parent", 99);
      Annotation simultaneous = new Annotation("simultaneous", "simultaneous", "word", "start2", "end2", "parent", 99);
      Anchor start2 = new Anchor("start2", 1.0);
      Anchor end2 = new Anchor("end2", 2.0);
      Annotation previous = new Annotation("previous", "before", "word", "before", "start", "parent", 99);
      Anchor before = new Anchor("before", 0.5);
      Annotation next = new Annotation("next", "before", "word", "end", "after", "parent", 99);
      Anchor after = new Anchor("after", 2.5);

      Annotation inside = new Annotation("inside", "inside", "word", "afterStart", "beforeEnd", "parent", 99);
      Anchor afterStart = new Anchor("afterStart", 1.25);
      Anchor betweenNoOffset = new Anchor("betweenNoOffset", null);
      Anchor beforeEnd = new Anchor("beforeEnd", 1.75);

      Annotation insideUnanchoredEnd = new Annotation("insideUnanchoredEnd", "insideUnanchoredEnd", "word", "afterStart", "betweenNoOffset", "parent", 99);
      Annotation insideUnanchoredStart = new Annotation("insideUnanchoredStart", "insideUnanchoredStart", "word", "betweenNoOffset", "beforeEnd", "parent", 99);

      Annotation outside = new Annotation("outside", "outside", "word", "before", "after", "parent", 99);

      Annotation startEdge = new Annotation("startEdge", "startEdge", "word", "start", "afterStart", "parent", 99);
      Annotation startEdgeInstant = new Annotation("startEdgeInstant", "startEdgeInstant", "word", "start", "start", "parent", 99);
      Annotation endEdge = new Annotation("endEdge", "endEdge", "word", "beforeEnd", "end", "parent", 99);
      Annotation endEdgeUnlinked = new Annotation("endEdgeUnlinked", "endEdgeUnlinked", "word", "beforeEnd", "end2", "parent", 99);
      Annotation endEdgeInstant = new Annotation("endEdgeInstant", "endEdgeInstant", "word", "end", "end2", "parent", 99);

      Annotation overStart = new Annotation("overStart", "overStart", "word", "before", "afterStart", "parent", 99);
      Annotation overEnd = new Annotation("overEnd", "overEnd", "word", "beforeEnd", "after", "parent", 99);

      Annotation unknown = new Annotation("unknown", "unknown", "word", "sometime", "sometime2", "parent", 99);
      Anchor sometime = new Anchor("sometime", null);
      Anchor sometime2 = new Anchor("sometime2", null);

      Annotation unknownStart = new Annotation("unknownStart", "unknownStart", "word", "sometime", "after", "parent", 99);
      Annotation unknownEnd = new Annotation("unknownStart", "unknownStart", "word", "before", "sometime", "parent", 99);

      graph.addAnchor(before);
      graph.addAnchor(start2);
      graph.addAnchor(end2);
      graph.addAnchor(after);

      graph.addAnnotation(parallel);
      graph.addAnnotation(simultaneous);
      graph.addAnnotation(startInstant);
      graph.addAnnotation(endInstant);
      graph.addAnnotation(previous);
      graph.addAnnotation(next);
      graph.addAnnotation(inside);
      graph.addAnnotation(insideUnanchoredEnd);
      graph.addAnnotation(insideUnanchoredStart);
      graph.addAnnotation(outside);
      graph.addAnnotation(startEdge);
      graph.addAnnotation(startEdgeInstant);
      graph.addAnnotation(endEdge);
      graph.addAnnotation(endEdgeUnlinked);
      graph.addAnnotation(endEdgeInstant);
      graph.addAnnotation(overStart);
      graph.addAnnotation(overEnd);
      graph.addAnnotation(unknown);
      
      assertFalse("includes - inside but unknown anchors", test.includes(inside));
      graph.addAnchor(afterStart);
      graph.addAnchor(betweenNoOffset);
      graph.addAnchor(beforeEnd);
      assertFalse("tags - inside", test.tags(inside));
      assertTrue("includes - inside", test.includes(inside));
      assertTrue("includes - insideUnanchoredEnd", test.includes(insideUnanchoredEnd));
      assertTrue("includes - insideUnanchoredStart", test.includes(insideUnanchoredStart));
      assertFalse("tags - outside", test.tags(outside));
      assertFalse("includes - outside", test.includes(outside));
      assertFalse("tags - previous", test.tags(previous));
      assertFalse("includes - previous", test.includes(previous));
      assertFalse("tags - next", test.tags(next));
      assertFalse("includes - next", test.includes(next));
      assertTrue("tags - parallel (shared anchors)", test.tags(parallel));
      assertTrue("includes - parallel (shared anchors)", test.includes(parallel));
      assertFalse("tags - simultaneous (not shared anchors)", test.tags(simultaneous));
      assertTrue("includes - simultaneous (not shared anchors)", test.includes(simultaneous));
      assertFalse("tags - startEdge", test.tags(startEdge));
      assertTrue("includes - startEdge", test.includes(startEdge));
      assertTrue("includes - startEdgeInstant", test.includes(startEdgeInstant));
      assertFalse("tags - endEdge", test.tags(endEdge));
      assertTrue("includes - endEdge", test.includes(endEdge));
      assertTrue("includes - endEdgeUnlinked", test.includes(endEdgeUnlinked));
      assertFalse("includes - endEdgeInstant", test.includes(endEdgeInstant));
      assertFalse("tags - overStart", test.tags(overStart));
      assertFalse("includes - overStart", test.includes(overStart));
      assertFalse("tags - overEnd", test.tags(overEnd));
      assertFalse("includes - overEnd", test.includes(overEnd));
      assertFalse("tags - unknown anchors", test.tags(unknown));
      assertFalse("includes - unknown anchors", test.includes(unknown));
      graph.addAnchor(sometime);
      graph.addAnchor(sometime2);
      assertFalse("tags - unknown offsets", test.tags(unknown));
      assertFalse("includes - unknown offsets", test.includes(unknown));
      assertFalse("tags - own unknown offsets", unknown.tags(test));
      assertFalse("includes - own unknown offsets", unknown.includes(test));
      assertTrue("includes - reflexive", test.includes(test));
      assertFalse("tags - startInstant", test.tags(startInstant));
      assertTrue("includes - startInstant", test.includes(startInstant));
      assertFalse("tags - endInstant", test.tags(endInstant));
      assertFalse("includes - endInstant", test.includes(endInstant));

      // duration
      assertEquals("interval duration", Double.valueOf(1.0), test.getDuration());
      assertEquals("instant duration", Double.valueOf(0.0), startInstant.getDuration());
      assertNull("unknown duration", unknown.getDuration());

      // midpoint
      assertEquals("interval midpoint", Double.valueOf(1.5), test.getMidpoint());
      assertEquals("instant midpoint", Double.valueOf(1.0), startInstant.getMidpoint());
      assertNull("unknown midpoint", unknown.getMidpoint());

      // includesMidpointOf
      assertTrue("includesMidpointOf - reflexive", test.includesMidpointOf(test));
      assertTrue("includesMidpointOf - parallel", test.includesMidpointOf(parallel));
      assertTrue("includesMidpointOf - simultaneous", test.includesMidpointOf(simultaneous));
      assertTrue("includesMidpointOf - inside", test.includesMidpointOf(inside));
      assertTrue("includesMidpointOf - outside", test.includesMidpointOf(outside));
      assertTrue("includesMidpointOf - startEdge", test.includesMidpointOf(startEdge)); 
      assertTrue("includesMidpointOf - endEdge", test.includesMidpointOf(endEdge));
      assertTrue("includesMidpointOf - startInstant", test.includesMidpointOf(startInstant)); 
      assertFalse("includesMidpointOf - endInstant", test.includesMidpointOf(endInstant));
      assertFalse("includesMidpointOf - previous", test.includesMidpointOf(previous));
      assertFalse("includesMidpointOf - next", test.includesMidpointOf(next));
      assertFalse("includesMidpointOf - unknown offsets", test.includesMidpointOf(unknown));
      assertFalse("includesMidpointOf - own unknown offsets", unknown.includesMidpointOf(test));
      assertTrue("includesMidpointOf - reflexive when offsets are unknown", unknown.includesMidpointOf(unknown));

      // distance
      Annotation prior = new Annotation("prior", "prior", "word", "first", "before", "parent", 99);
      Anchor first = new Anchor("first", 0.0);
      Annotation following = new Annotation("following", "following", "word", "after", "last", "parent", 99);
      Anchor last = new Anchor("last", 3.0);
      graph.addAnnotation(prior);
      graph.addAnchor(first);
      graph.addAnnotation(following);
      graph.addAnchor(last);
      assertEquals("distance prior", Double.valueOf(0.5), test.distance(prior));
      assertEquals("distance previous", Double.valueOf(0.0), test.distance(previous));
      assertEquals("distance next", Double.valueOf(0.0), test.distance(next));
      assertEquals("distance following", Double.valueOf(0.5), test.distance(following));
      assertEquals("distance overStart", Double.valueOf(-0.25), test.distance(overStart));
      assertEquals("distance overEnd", Double.valueOf(-0.25), test.distance(overEnd));
      assertEquals("distance startEdge", Double.valueOf(-0.25), test.distance(startEdge));
      assertEquals("distance endEdge", Double.valueOf(-0.25), test.distance(endEdge));
      assertEquals("distance parallel", Double.valueOf(-1.0), test.distance(parallel));
      assertEquals("distance simultaneous", Double.valueOf(-1.0), test.distance(simultaneous));
      assertEquals("distance reflexive", Double.valueOf(-1.0), test.distance(test));
      // we tolerate negative-zero even though it's silly
      assertEquals("distance startInstant", Double.valueOf(-0.0), test.distance(startInstant));
      assertEquals("distance endInstant", Double.valueOf(0.0), test.distance(endInstant));
      assertNull("distance unknown", test.distance(unknown));

      assertEquals("maxPairedDistance prior", Double.valueOf(1.5), test.maxPairedDistance(prior));
      assertEquals("maxPairedDistance previous", Double.valueOf(1.0), test.maxPairedDistance(previous));
      assertEquals("maxPairedDistance next", Double.valueOf(1.0), test.maxPairedDistance(next));
      assertEquals("maxPairedDistance following", Double.valueOf(1.5), test.maxPairedDistance(following));
      assertEquals("maxPairedDistance overStart", Double.valueOf(-0.75), test.maxPairedDistance(overStart));
      assertEquals("maxPairedDistance overEnd", Double.valueOf(-0.75), test.maxPairedDistance(overEnd));
      assertEquals("maxPairedDistance startEdge", Double.valueOf(-0.75), test.maxPairedDistance(startEdge));
      assertEquals("maxPairedDistance endEdge", Double.valueOf(-0.75), test.maxPairedDistance(endEdge));
      assertEquals("maxPairedDistance startInstant", Double.valueOf(1.0), test.maxPairedDistance(startInstant));
      assertEquals("maxPairedDistance endInstant", Double.valueOf(1.0), test.maxPairedDistance(endInstant));
      assertNull("maxPairedDistance unknown", test.maxPairedDistance(unknown));
      // we tolerate negative-zero even though it's silly
      assertEquals("maxPairedDistance reflexive", Double.valueOf(-0.0), test.maxPairedDistance(test));
      assertEquals("maxPairedDistance parallel", Double.valueOf(-0.0), test.maxPairedDistance(parallel));
      assertEquals("maxPairedDistance simultaneous", Double.valueOf(-0.0), test.maxPairedDistance(simultaneous));

      // anchored
      assertTrue("anchored", test.getAnchored());
      assertFalse("not anchored", unknown.getAnchored());
      assertFalse("not anchored", unknownStart.getAnchored());
      assertFalse("not anchored", unknownEnd.getAnchored());
     
   }
/*
   @Test public void containment() 
   {
      Anchor a1 = new Anchor(1.0, Labbcat.ALIGNMENT_STATUS_AUTOMATIC);
      Anchor a2 = new Anchor(2.0, Labbcat.ALIGNMENT_STATUS_AUTOMATIC);
      Anchor a3 = new Anchor(3.0, Labbcat.ALIGNMENT_STATUS_AUTOMATIC);
      Anchor a4 = new Anchor(4.0, Labbcat.ALIGNMENT_STATUS_AUTOMATIC);
      
      Annotation w = new Annotation();
      w.setId(1L);
      w.setLayerId(0);
      w.setLabel("WORD");
      w.setStartAnchor(a1);
      w.setEndAnchor(a4);

      Annotation s1 = new Annotation();
      s1.setId(1L);
      s1.setLayerId(1);
      s1.setLabel("SEG1");
      s1.setStartAnchor(a1);
      s1.setEndAnchor(a2);
      Annotation s2 = new Annotation();
      s2.setId(2L);
      s2.setLayerId(1);
      s2.setLabel("SEG2");
      s2.setStartAnchor(a2);
      s2.setEndAnchor(a3);
      Annotation s3 = new Annotation();
      //s3.setId(3L); // doesn't matter whether ID is set
      s3.setLayerId(1);
      s3.setLabel("SEG3");
      s3.setStartAnchor(a3);
      s3.setEndAnchor(a4);

      // shouldn't matter what order we add the segments to the word...
      w.addContainedAnnotation(s1);
      w.addContainedAnnotation(s3);
      w.addContainedAnnotation(s2);
      
      // they should come out in order
      int iS = 1;
      for (Annotation s : w.getContainedAnnotationsLayer(1))
      {
	 assertEquals("SEG"+(iS++), s.getLabel());
      }
   }
   @Test public void distance() 
   {
      Anchor a1 = new Anchor(1.0, Labbcat.ALIGNMENT_STATUS_AUTOMATIC);
      Anchor a2 = new Anchor(2.0, Labbcat.ALIGNMENT_STATUS_AUTOMATIC);
      Anchor a3 = new Anchor(3.0, Labbcat.ALIGNMENT_STATUS_AUTOMATIC);
      Anchor a4 = new Anchor(4.0, Labbcat.ALIGNMENT_STATUS_AUTOMATIC);
      
      Annotation a12 = new Annotation();
      a12.setId(1L);
      a12.setLayerId(0);
      a12.setLabel("1-2");
      a12.setStartAnchor(a1);
      a12.setEndAnchor(a2);

      Annotation a13 = new Annotation();
      a13.setId(1L);
      a13.setLayerId(0);
      a13.setLabel("1-3");
      a13.setStartAnchor(a1);
      a13.setEndAnchor(a3);

      Annotation a14 = new Annotation();
      a14.setId(1L);
      a14.setLayerId(0);
      a14.setLabel("1-4");
      a14.setStartAnchor(a1);
      a14.setEndAnchor(a4);

      Annotation a23 = new Annotation();
      a23.setId(1L);
      a23.setLayerId(0);
      a23.setLabel("2-3");
      a23.setStartAnchor(a2);
      a23.setEndAnchor(a3);

      Annotation a24 = new Annotation();
      a24.setId(1L);
      a24.setLayerId(0);
      a24.setLabel("2-4");
      a24.setStartAnchor(a2);
      a24.setEndAnchor(a4);

      Annotation a34 = new Annotation();
      a34.setId(1L);
      a34.setLayerId(0);
      a34.setLabel("3-4");
      a34.setStartAnchor(a3);
      a34.setEndAnchor(a4);

      // distal
      assertEquals(Double.valueOf(1.0), a12.minimumOffsetDifference(a34, false));
      assertEquals(a34.minimumOffsetDifference(a12, false), a12.minimumOffsetDifference(a34, false));

      // joined
      assertEquals(Double.valueOf(0.0), a12.minimumOffsetDifference(a23, false));
      assertEquals(a23.minimumOffsetDifference(a12, false), a12.minimumOffsetDifference(a23, false));

      // overlapping
      assertEquals(Double.valueOf(-1.0), a13.minimumOffsetDifference(a24, false));
      assertEquals(a24.minimumOffsetDifference(a13, false), a13.minimumOffsetDifference(a24, false));

      // identical
      assertEquals(Double.valueOf(-2.0), a13.minimumOffsetDifference(a13, false));

      // t-included
      assertEquals(Double.valueOf(-1.0), a14.minimumOffsetDifference(a12, false));
      assertEquals(a14.minimumOffsetDifference(a12, false), a12.minimumOffsetDifference(a14, false));

      assertEquals(Double.valueOf(-1.0), a14.minimumOffsetDifference(a23, false));
      assertEquals(a14.minimumOffsetDifference(a23, false), a23.minimumOffsetDifference(a14, false));

      assertEquals(Double.valueOf(-1.0), a14.minimumOffsetDifference(a34, false));
      assertEquals(a14.minimumOffsetDifference(a34, false), a34.minimumOffsetDifference(a14, false));
   }
   @Test public void validate()
   {
      AnnotationGraph ag = new AnnotationGraph();
      Anchor falseStart = new Anchor(ag, 300.00, Labbcat.ALIGNMENT_STATUS_NONE);
      Anchor falseEnd = new Anchor(ag, 200.00, Labbcat.ALIGNMENT_STATUS_NONE);
      Anchor goodStart = falseEnd;
      Anchor goodEnd = falseStart;
      Annotation anParent = new Annotation(ag, 99, "PARENT", Labbcat.LABEL_STATUS_NONE);
      ag.addLayerInfo(new LayerInfo(99, "Parent Layer", "Text", "M", 2));
      anParent.setId(99L);
      anParent.setStartAnchor(falseStart);
      anParent.setEndAnchor(falseEnd);
      try
      {
	 anParent.validate(false);
	 fail("ReversedAnchorsError not thrown");
      }
      catch (ReversedAnchorsError r)
      {
	 try
	 {
	    r.repair();
	    fail("Repair should not reverse anchors");
	 }
	 catch (ValidationError x)
	 {
	    // anchors have not been swapped
	    assertEquals(anParent.getStartAnchor(), goodEnd);
	    assertEquals(anParent.getEndAnchor(), goodStart);
	 }
      }
      catch (ValidationError x)
      {
	 fail(x.toString());
      }
      anParent.setStartAnchor(goodStart);
      anParent.setEndAnchor(goodEnd);
      try
      {
	 // now valid
	 anParent.validate(false);
      }
      catch (ValidationError x)
      {
	 fail(x.toString());
      }

      // child correction
      Annotation anChild = new Annotation(ag, 98, "CHILD", Labbcat.LABEL_STATUS_NONE);
      anChild.setId(98L);
      anChild.setStartAnchor(new Anchor(ag, 100.00, Labbcat.ALIGNMENT_STATUS_NONE));
      anChild.setEndAnchor(new Anchor(ag, 400.00, Labbcat.ALIGNMENT_STATUS_NONE));
      ag.addLayerInfo(new LayerInfo(98, "Child Layer", "Text", "M", 2));
      anParent.addContainedAnnotation(anChild);
      Anchor narrowStart = anParent.getStartAnchor();
      Anchor narrowEnd = anParent.getEndAnchor();
      Anchor wideStart = anChild.getStartAnchor();
      Anchor wideEnd = anChild.getEndAnchor();

      // TODO these tests disabled because this validation should be done by layer relationship
      // try
      // {
      // 	 anParent.validate(false);
      // 	 fail("ParentDoesntTIncludeChild not thrown");
      // }
      // catch (ParentDoesntTIncludeChild t)
      // {
      // 	 try
      // 	 {
      // 	    t.repair();
      // 	    // parent anchors the same
      // 	    anParent.applyDelta();
      // 	    assertEquals(anParent.getStartAnchor(), narrowStart);
      // 	    assertEquals(anParent.getEndAnchor(), narrowEnd);
      // 	    // child anchors changed
      // 	    anChild.applyDelta();
      // 	    assertEquals(anChild.getStartAnchor(), narrowStart);
      // 	    assertEquals(anChild.getEndAnchor(), narrowEnd);
      // 	 }
      // 	 catch (ValidationError x)
      // 	 {
      // 	    fail(x.toString());
      // 	 }
      // }
      // catch (ValidationError x)
      // {
      // 	 fail(x.toString());
      // }

      // child entirely in the future
      anChild = new Annotation(ag, 97, "CHILD", Labbcat.LABEL_STATUS_NONE);
      anChild.setId(97L);
      anChild.setStartAnchor(new Anchor(ag, 500.00, Labbcat.ALIGNMENT_STATUS_NONE));
      anChild.setEndAnchor(new Anchor(ag, 600.00, Labbcat.ALIGNMENT_STATUS_NONE));
      anParent.addContainedAnnotation(anChild);
      ag.addLayerInfo(new LayerInfo(97, "Parent Layer 2", "Text", "M", 2));
      Anchor futureStart = anChild.getStartAnchor();
      Anchor futureEnd = anChild.getEndAnchor();

      // try
      // {
      // 	 anParent.validate(false);
      // 	 fail("ParentDoesntTIncludeChild not thrown");
      // }
      // catch (ParentDoesntTIncludeChild t)
      // {
      // 	 try
      // 	 {
      // 	    t.repair();
      // 	    // parent anchors the same
      // 	    anParent.applyDelta();
      // 	    assertEquals(narrowStart, anParent.getStartAnchor());
      // 	    assertEquals(narrowEnd, anParent.getEndAnchor());
      // 	    // child anchors changed
      // 	    anChild.applyDelta();
      // 	    assertEquals(narrowEnd, anChild.getStartAnchor());
      // 	    assertEquals(narrowEnd, anChild.getEndAnchor());
      // 	 }
      // 	 catch (ValidationError x)
      // 	 {
      // 	    fail(x.toString());
      // 	 }
      // }
      // catch (ValidationError x)
      // {
      // 	 fail(x.toString());
      // }

      // child entirely in the past
      anChild = new Annotation(ag, 97, "CHILD", Labbcat.LABEL_STATUS_NONE);
      anChild.setId(97L);
      anChild.setStartAnchor(new Anchor(ag, 50.00, Labbcat.ALIGNMENT_STATUS_NONE));
      anChild.setEndAnchor(new Anchor(ag, 60.00, Labbcat.ALIGNMENT_STATUS_NONE));
      anParent.addContainedAnnotation(anChild);
      Anchor pastStart = anChild.getStartAnchor();
      Anchor pastEnd = anChild.getEndAnchor();

      // try
      // {
      // 	 anParent.validate(false);
      // 	 fail("ParentDoesntTIncludeChild not thrown");
      // }
      // catch (ParentDoesntTIncludeChild t)
      // {
      // 	 try
      // 	 {
      // 	    t.repair();
      // 	    // parent anchors the same
      // 	    anParent.applyDelta();
      // 	    assertEquals(narrowStart, anParent.getStartAnchor());
      // 	    assertEquals(narrowEnd, anParent.getEndAnchor());
      // 	    // child anchors changed
      // 	    anChild.applyDelta();
      // 	    assertEquals(narrowStart, anChild.getStartAnchor());
      // 	    assertEquals(narrowStart, anChild.getEndAnchor());
      // 	 }
      // 	 catch (ValidationError x)
      // 	 {
      // 	    fail(x.toString());
      // 	 }
      // }
      // catch (ValidationError x)
      // {
      // 	 fail(x.toString());
      // }

      // child ok
      anChild = new Annotation(ag, 97, "CHILD", Labbcat.LABEL_STATUS_NONE);
      anChild.setId(97L);
      anChild.setStartAnchor(new Anchor(ag, 225.00, Labbcat.ALIGNMENT_STATUS_NONE));
      anChild.setEndAnchor(new Anchor(ag, 275.00, Labbcat.ALIGNMENT_STATUS_NONE));
      anParent.addContainedAnnotation(anChild);
      Anchor okStart = anChild.getStartAnchor();
      Anchor okEnd = anChild.getEndAnchor();

      try
      {
	 anParent.validate(false);
      }
      catch (ValidationError x)
      {
	 fail(x.toString());
      }

   }
*/   
   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.test.TestAnnotation");
   }
}
