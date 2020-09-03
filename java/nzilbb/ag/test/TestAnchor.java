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
import java.util.LinkedHashSet;
import javax.json.Json;
import javax.json.JsonObject;
import nzilbb.ag.*;

public class TestAnchor 
{
   @Test public void basicAttributes() 
   {
      // Default constructor
      Anchor a = new Anchor();
      a.setId("123");
      a.setOffset(456.789);
      assertEquals("123", a.getId());
      assertEquals(Double.valueOf(456.789), a.getOffset());
      assertEquals("456.789", a.toString());

      // Basic constructor
      a = new Anchor("123", 456.789);
      assertEquals("123", a.getId());
      assertEquals(Double.valueOf(456.789), a.getOffset());
      assertEquals("456.789", a.toString());
   }

   @Test public void extendedAttributes() 
   {
      Anchor a = new Anchor();
      a.setId("123");
      a.setOffset(456.789);
      a.put("alignmentStatus", 50);
      assertEquals(Integer.valueOf(50), a.get("alignmentStatus"));      
   }

   @Test public void copyConstructor() 
   {
      Anchor a = new Anchor("123", 456.789);
      // change offset to generate tracked original
      a.setOffset(123.456);
      // set copiable attribute
      a.put("copy", Boolean.TRUE);
      // set transient attribute
      a.put("@dontCopy", Boolean.TRUE);

      Anchor newA = new Anchor(a);
      assertNull("id not copied", newA.getId());
      assertEquals("offset copied", Double.valueOf(123.456), newA.getOffset());
      assertEquals("originalOffset not copied", Double.valueOf(123.456), newA.getOriginalOffset());
      assertEquals("other attribute copied", Boolean.TRUE, newA.get("copy"));
      assertNull("transient attribute not copied", newA.get("@dontCopy"));      
   }


   @Test public void objectAttributes() 
   {
      Anchor a = new Anchor();
      // TODO define before-id hashcode behaviour
      a.setId("123");
      int iStartHashCode = a.hashCode();
      a.setOffset(456.789);
      a.put("foo", "bar");
      int iEndHashCode = a.hashCode();
      assertEquals("Immutable hashcode:", iStartHashCode, iEndHashCode);
      assertEquals("456.789", a.toString());
      
      assertTrue("Equality reflexive", a.equals(a));
      Anchor a2 = new Anchor();
      assertFalse("equals before id set:", a.equals(a2));
      a2.setId("123");
      a2.setOffset(456.789);
      // no "foo" attribute, to ensure it doesn't contribute to equality
      assertTrue("id defines equality:", a.equals(a2));
      assertTrue("Equality is symmetric:", a2.equals(a));
      assertEquals("Comparable:", 0, a.compareTo(a2));
      
      a2.setOffset(123.456);
      assertTrue("offset doesn't affect equality:", a.equals(a2));
		  
      a2.setId("different id");
      assertFalse("Different id:", a.equals(a2));
      assertTrue("Compare by offset - 456.123 > 123.456", a.compareTo(a2) > 0);
      a2.setOffset(a.getOffset());
      assertTrue("Comparable: if offsets are equal, comparison is by ID - \"123\" < \"different id\"", a.compareTo(a2) < 0);
      a2.setOffset(null);
      assertTrue("Comparable: if left offset is null, comparison is by ID - \"123\" < \"different id\"", a.compareTo(a2) < 0);
      assertTrue("Comparable: if right offset is null, comparison is by ID - \"different id\" > \"123\"", a2.compareTo(a) > 0);
      a2.setId(a.getId());
      assertTrue("Resetting attribute resets equality:", a.equals(a2));

      LinkedHashMap<String,Object> aMap = new LinkedHashMap<String,Object>();
      aMap.putAll(a);
      assertFalse("A map with the same attributes isn't equal:", a.equals(aMap));
   }

   @Test public void changeTracking() 
   {
      Anchor a = new Anchor();
      a.setId("changeTracking");
      a.setOffset(456.789);
      a.put("foo", "bar");
      a.setTracker(new ChangeTracker()); // after initialization 
      assertEquals(Double.valueOf(456.789), a.getOffset());
      assertEquals("Offset and original are the same", a.getOriginalOffset(), a.getOffset());
      assertEquals(Change.Operation.NoChange, a.getChange());

      a.put("foo", "foo");
      assertEquals("Only offset affects change:", Change.Operation.NoChange, a.getChange());

      a.setOffset(123.456);
      assertEquals(Double.valueOf(123.456), a.getOffset());
      assertEquals("Original offset remembers first offset:",
                   Double.valueOf(456.789), a.getOriginalOffset());
      assertEquals(Change.Operation.Update, a.getChange());

      a.setOffset(456.123);
      assertEquals(Double.valueOf(456.123), a.getOffset());
      assertEquals("Original offset only set once:", Double.valueOf(456.789), a.getOriginalOffset());
      assertEquals(Change.Operation.Update, a.getChange());

      a.rollback();
      assertEquals(Double.valueOf(456.789), a.getOffset());
      assertEquals("Offset and original are the same", a.getOriginalOffset(), a.getOffset());
      assertEquals(Change.Operation.NoChange, a.getChange());

      a.destroy();
      assertEquals(Change.Operation.Destroy, a.getChange());
      a.rollback();
      assertEquals(Change.Operation.NoChange, a.getChange());

      a.create();
      assertEquals(Change.Operation.Create, a.getChange());
      a.rollback();
      assertEquals("Create cannot be rolled back:", Change.Operation.Create, a.getChange());

   }

   @Test public void cloning() 
   {
      Anchor a = new Anchor("123", 99.0, Constants.CONFIDENCE_AUTOMATIC);
      a.put("foo", "foo");
      Anchor c = (Anchor)a.clone();
      assertNotNull(c);
      assertEquals("123", c.getId());
      assertEquals(Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC), c.getConfidence());
      assertEquals(Double.valueOf(99.0), c.getOffset());
      assertFalse(c.containsKey("foo"));     
   }

   @Test public void fromJson() 
   {
      JsonObject json = Json.createObjectBuilder()
         .add("id", "123")
         .add("offset", 99.0)
         .add("confidence", Constants.CONFIDENCE_AUTOMATIC)
         .add("foo", "foo") // copied
         .add("@bar", "bar") // not copied
         .build();
      Anchor c = (Anchor)new Anchor().fromJson(json.toString());
      assertEquals("123", c.getId());
      assertEquals(Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC), c.getConfidence());
      assertEquals(Double.valueOf(99.0), c.getOffset());
      assertTrue("arbitrary values starting with alphanumeric are copied",
                 c.containsKey("foo"));     
      assertFalse("arbitrary values starting with non alphanumeric are copied",
                  c.containsKey("@bar"));     
   }

   @Test public void nullInitialOffset() 
   {
      Anchor a = new Anchor("nullInitialOffset", null);
      a.setTracker(new ChangeTracker());
      
      assertEquals("nullInitialOffset", a.getId());
      assertNull(a.getOffset());
      assertEquals("no change registered when offset is initialised as null",
		   0, a.getChanges().size());

      a.setOffset(Double.valueOf(99.0)).size();
      
      assertEquals(Double.valueOf(99.0), a.getOffset());

      assertEquals("a change registered when offset is initialised as null and then is changed",
		   1, a.getChanges().size());
   }

   @Test public void offsetMinMaxAndPrecedingFollowing() 
   {
      Graph g = new Graph();

      Anchor a = new Anchor("test", null);
      g.addAnchor(a);

      assertNull("offsetMin - null offset, no preceding", a.getOffsetMin());
      
      g.addAnchor(new Anchor("early", null));
      g.addAnnotation(new Annotation("toEarly", "direct to early", "layer1", "early", "test"));
      assertNull("offsetMin - null offsets, one preceding", a.getOffsetMin());

      g.addAnchor(new Anchor("earlier", null));
      g.addAnnotation(new Annotation("toEarlier", "chained to earlier", "layer1", "earlier", "early"));
      assertNull("offsetMin - null offsets, chain preceding", a.getOffsetMin());

      g.addAnchor(new Anchor("earliest", null));
      g.addAnnotation(new Annotation("toEarliest", "direct to earliest", "layer2", "earliest", "test"));
      assertNull("offsetMin - null offsets, multiple paths preceding", a.getOffsetMin());

      g.getAnchor("earliest").setOffset(1.0);
      assertEquals("offsetMin - earliest", Double.valueOf(1.0), a.getOffsetMin());

      g.getAnchor("earlier").setOffset(2.0);
      assertEquals("offsetMin - chain overrides direct connection", Double.valueOf(2.0), a.getOffsetMin());

      g.getAnchor("early").setOffset(3.0);
      assertEquals("offsetMin - direct connection overrides chain", Double.valueOf(3.0), a.getOffsetMin());

      assertNull("offsetMax - null offset, no following", a.getOffsetMax());

      g.addAnchor(new Anchor("late", null));
      g.addAnnotation(new Annotation("toLate", "direct to late", "layer1", "test", "late"));
      assertNull("offsetMax - null offsets, one following", a.getOffsetMax());

      g.addAnchor(new Anchor("later", null));
      g.addAnnotation(new Annotation("toLater", "chained to later", "layer1", "late", "later"));
      assertNull("offsetMax - null offsets, chain following", a.getOffsetMax());

      g.addAnchor(new Anchor("latest", null));
      g.addAnnotation(new Annotation("toLatest", "direct to latest", "layer2", "test", "latest"));
      assertNull("offsetMax - null offsets, multiple paths following", a.getOffsetMax());

      g.getAnchor("latest").setOffset(7.0);
      assertEquals("offsetMax - latest", Double.valueOf(7.0), a.getOffsetMax());

      g.getAnchor("later").setOffset(6.0);
      assertEquals("offsetMax - chain overrides direct connection", Double.valueOf(6.0), a.getOffsetMax());

      g.getAnchor("late").setOffset(5.0);
      assertEquals("offsetMax - direct connection overrides chain", Double.valueOf(5.0), a.getOffsetMax());
      
      a.setOffset(5.0);
      assertEquals("offsetMin - offset set", Double.valueOf(5.0), a.getOffsetMin());
      assertEquals("offsetMax - offset set", Double.valueOf(5.0), a.getOffsetMax());

      // preceding
      LinkedHashSet<Anchor> preceding = a.getPreceding();
      assertTrue("preceding", preceding.contains(g.getAnchor("earliest")));
      assertTrue("preceding", preceding.contains(g.getAnchor("earlier")));
      assertTrue("preceding", preceding.contains(g.getAnchor("early")));
      assertEquals("preceding size", 3, preceding.size());

      // following
      LinkedHashSet<Anchor> following = a.getFollowing();
      assertTrue("following", following.contains(g.getAnchor("late")));
      assertTrue("following", following.contains(g.getAnchor("later")));
      assertTrue("following", following.contains(g.getAnchor("latest")));
      assertEquals("following size", 3, following.size());

      // follows
      assertTrue("follows", a.follows(g.getAnchor("early")));
      assertTrue("follows", a.follows(g.getAnchor("earlier")));
      assertTrue("follows", a.follows(g.getAnchor("early")));
      assertFalse("follows - self", a.follows(a));
      assertFalse("follows", a.follows(g.getAnchor("late")));
      assertFalse("follows", a.follows(g.getAnchor("later")));
      assertFalse("follows", a.follows(g.getAnchor("latest")));

      // precedes
      assertFalse("precedes", a.precedes(g.getAnchor("early")));
      assertFalse("precedes", a.precedes(g.getAnchor("earlier")));
      assertFalse("precedes", a.precedes(g.getAnchor("early")));
      assertFalse("precedes - self", a.precedes(a));
      assertTrue("precedes", a.precedes(g.getAnchor("late")));
      assertTrue("precedes", a.precedes(g.getAnchor("later")));
      assertTrue("precedes", a.precedes(g.getAnchor("latest")));

      // add some instants
      g.addAnnotation(new Annotation("middleInstant", "instant at test anchor", "layer1", "test", "test"));
      g.addAnnotation(new Annotation("firstInstant", "instant at earliest", "layer2", "earliest", "earliest"));
      g.addAnnotation(new Annotation("lastInstant", "instant at latest", "layer2", "latest", "latest"));

      // ending
      LinkedHashSet<Annotation> ending = a.getEndingAnnotations();
      assertTrue("ending", ending.contains(g.getAnnotation("toEarly")));
      assertTrue("ending", ending.contains(g.getAnnotation("toEarliest")));
      assertTrue("ending", ending.contains(g.getAnnotation("middleInstant")));
      assertEquals("ending size", 3, ending.size());

      // starting
      LinkedHashSet<Annotation> starting = a.getStartingAnnotations();
      assertTrue("starting", starting.contains(g.getAnnotation("toLate")));
      assertTrue("starting", starting.contains(g.getAnnotation("toLatest")));
      assertTrue("starting", starting.contains(g.getAnnotation("middleInstant")));
      assertEquals("starting size", 3, starting.size());

      // linking
      assertNull("linking - none", a.annotationTo(g.getAnchor("later")));
      assertEquals("linking", g.getAnnotation("toLate"), a.annotationTo(g.getAnchor("late")));
      assertNull("linking - not symmetric", g.getAnchor("late").annotationTo(a));

   }   
   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.test.TestAnchor");
   }
}
