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
import java.util.Set;
import java.util.LinkedHashSet;
import nzilbb.ag.*;

public class TestTrackedMap
{
   static String[] aTrackedAttributes = {"tracked2", "tracked1", "tracked3"};
   // Using LinkedHashSet ensures that the attributes will be iterated in the order above
   static final LinkedHashSet<String> trackedAttributes = new LinkedHashSet<String>(java.util.Arrays.asList(aTrackedAttributes));
   
   // subclass that defines some tracked attributes
   @SuppressWarnings("serial")
   class TestMap extends TrackedMap
   {
      public Set<String> getTrackedAttributes() { return trackedAttributes; }
   }
   
   @Test public void changeTracking() 
   {
      TestMap m = new TestMap();
      m.setId("123");
      m.put("tracked1", "value1");
      m.put("tracked2", "value2");
      m.put("tracked3", "value3");
      m.put("notTracked", "value4");
      
      // values set
      assertEquals("123", m.getId());
      assertEquals("123", m.get("id"));
      assertEquals("value1", m.get("tracked1"));
      assertEquals("value2", m.get("tracked2"));
      assertEquals("value3", m.get("tracked3"));
      assertEquals("value4", m.get("notTracked"));

      // initial change tracking state
      assertFalse(m.containsKey("originalTracked1"));
      assertFalse(m.containsKey("originalTracked2"));
      assertFalse(m.containsKey("originalTracked3"));
      assertFalse(m.containsKey("originalTracked4"));
      assertEquals(Change.Operation.NoChange, m.getChange());
      assertEquals(0, m.getChanges().size());

      m.put("notTracked", "newValue4");
      assertEquals("newValue4", m.get("notTracked"));
      assertEquals("Non-tracked keys don't affect change:", Change.Operation.NoChange, m.getChange());
      assertEquals(0, m.getChanges().size());
      
      m.setId("ID");
      assertEquals("ID", m.getId());
      assertEquals("Non-tracked attributes don't affect change - id:", Change.Operation.NoChange, m.getChange());

      m.put("tracked1", "newValue1");
      assertEquals("newValue1", m.get("tracked1"));
      assertEquals("Original value in map:", "value1", m.get("originalTracked1"));
      assertEquals(Change.Operation.Update, m.getChange());
      assertEquals(1, m.getChanges().size());
      assertEquals("Update ID: tracked1 = newValue1", m.getChanges().elementAt(0).toString());

      m.rollback();
      assertEquals("value1", m.get("tracked1"));
      assertFalse("Original value no longer in map:", m.containsKey("originalTracked1"));
      assertEquals(Change.Operation.NoChange, m.getChange());
      assertEquals(0, m.getChanges().size());

      m.put("tracked2", "newValue2");
      assertEquals("newValue2", m.get("tracked2"));
      assertEquals("Original value in map:", "value2", m.get("originalTracked2"));
      assertEquals(Change.Operation.Update, m.getChange());
      assertEquals(1, m.getChanges().size());
      assertEquals("Update ID: tracked2 = newValue2", m.getChanges().elementAt(0).toString());

      m.commit();
      assertEquals("newValue2", m.get("tracked2"));
      assertFalse("Original value no longer in map:", m.containsKey("originalTracked2"));
      assertEquals(Change.Operation.NoChange, m.getChange());
      assertEquals(0, m.getChanges().size());

      m.put("tracked3", "newerValue3");
      assertEquals("newerValue3", m.get("tracked3"));
      assertEquals("Original value in map:", "value3", m.get("originalTracked3"));
      assertEquals(Change.Operation.Update, m.getChange());
      assertEquals(1, m.getChanges().size());
      assertEquals("Update ID: tracked3 = newerValue3", m.getChanges().elementAt(0).toString());

      m.put("tracked3", "newestValue3");
      assertEquals("newestValue3", m.get("tracked3"));
      assertEquals("First original value in map:", "value3", m.get("originalTracked3"));
      assertEquals(Change.Operation.Update, m.getChange());
      assertEquals("Only latest change is reported", 1, m.getChanges().size());
      assertEquals("Only latest change is reported", "Update ID: tracked3 = newestValue3", m.getChanges().elementAt(0).toString());

      m.put("tracked2", "newestValue2");
      assertEquals("newestValue2", m.get("tracked2"));
      assertEquals("Original value in map:", "newValue2", m.get("originalTracked2"));
      assertEquals(Change.Operation.Update, m.getChange());
      assertEquals("Multiple attribute changes", 2, m.getChanges().size());
      assertEquals("Multiple attribute changes", "Update ID: tracked2 = newestValue2", m.getChanges().elementAt(0).toString());
      assertEquals("Multiple attribute changes", "Update ID: tracked3 = newestValue3", m.getChanges().elementAt(1).toString());

      m.rollback("tracked3");
      assertEquals("Rollback single key", "value3", m.get("tracked3"));
      assertFalse("Original value no longer in map:", m.containsKey("originalTracked3"));
      assertEquals(Change.Operation.Update, m.getChange());
      assertEquals("Rollback single key", 1, m.getChanges().size());
      assertEquals("Rollback single key", "Update ID: tracked2 = newestValue2", m.getChanges().elementAt(0).toString());

      m.destroy();
      assertEquals("Destroy trumps Update as a change", Change.Operation.Destroy, m.getChange());
      assertEquals(1, m.getChanges().size());
      assertEquals("Destroy ID", m.getChanges().elementAt(0).toString());

      m.rollback();
      assertEquals(Change.Operation.NoChange, m.getChange());
      assertEquals(0, m.getChanges().size());

      m.create();
      m.put("tracked2", "value2"); // just to be tidily in line with other values
      assertEquals(Change.Operation.Create, m.getChange());
      assertEquals("Create reports all attributes", 4, m.getChanges().size());
      assertEquals("Multiple attribute changes", "Create ID", m.getChanges().elementAt(0).toString());
      // these should be ordered according to the aTrackedAttributes above:
      assertEquals("Multiple attribute changes", "Update ID: tracked2 = value2", m.getChanges().elementAt(1).toString());
      assertEquals("Multiple attribute changes", "Update ID: tracked1 = value1", m.getChanges().elementAt(2).toString());
      assertEquals("Multiple attribute changes", "Update ID: tracked3 = value3", m.getChanges().elementAt(3).toString());

      m.destroy();
      assertEquals("Destroy trumps Create as a change", Change.Operation.Destroy, m.getChange());
      // an insert followed by a delete cancels out - there is no operation
      assertEquals("Insert followed by delete", 0, m.getChanges().size());

      m.rollback();
      assertEquals("Create cannot be rolled back", Change.Operation.Create, m.getChange());
      assertEquals("Create reports all attributes", 4, m.getChanges().size());

   }

   @Test public void cloning() 
   {
      // can't seem to make TestMap cloneable, so use Annotation instead
      Annotation m = new Annotation();
      m.setId("123");
      m.put("startId", "value1");
      m.put("endId", "value2");
      // replace that value with a new value
      m.put("endId", "newValue2");
      m.put("notTracked", "value4");

      Annotation c = (Annotation)m.clone();
      assertEquals("123", c.get("id"));
      assertEquals("copy tracked values", "value1", c.get("startId"));
      assertEquals("copy current, not old, traced values", "newValue2", c.get("endId"));
      assertFalse("don't copy nonexistent tracked values", c.containsKey("label"));
      assertFalse("don't copy non-tracked values", c.containsKey("notTracked"));      
   }

   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.test.TestTrackedMap");
   }
}
