//
// Copyright 2015-2017 New Zealand Institute of Language, Brain and Behaviour, 
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
   
   @Test public void changeTracking() 
   {
      MapTest m = new MapTest();
      m.setId("123");
      m.setTracked1("value1");
      m.setTracked2("value2");
      m.setTracked3("value3");
      m.setNotTracked("value4");
      m.setTracker(new ChangeTracker());
      
      // values set
      assertEquals("123", m.getId());
      assertEquals("value1", m.getTracked1());
      assertEquals("value2", m.getTracked2());
      assertEquals("value3", m.getTracked3());
      assertEquals("value4", m.getNotTracked());

      // initial change tracking state
      assertFalse(m.containsKey("originalTracked1"));
      assertFalse(m.containsKey("originalTracked2"));
      assertFalse(m.containsKey("originalTracked3"));
      assertFalse(m.containsKey("originalNotTracked"));
      assertEquals(Change.Operation.NoChange, m.getChange());
      assertEquals(0, m.getChanges().size());

      m.setNotTracked("newValue4");
      assertEquals("newValue4", m.getNotTracked());
      assertEquals("Non-tracked keys don't affect change:",
                   Change.Operation.NoChange, m.getChange());
      assertEquals(0, m.getChanges().size());
      
      m.setId("ID");
      assertEquals("ID", m.getId());
      assertEquals("Non-tracked attributes don't affect change - id:",
                   Change.Operation.NoChange, m.getChange());

      m.setTracked1("newValue1");
      assertEquals("newValue1", m.getTracked1());
      assertEquals(Change.Operation.Update, m.getChange());
      assertEquals(1, m.getChanges().size());
      assertEquals("Update ID: tracked1 = newValue1 (was value1)",
                   m.getChanges().elementAt(0).toString());

      m.rollback();
      assertEquals("value1", m.getTracked1());
      assertEquals(Change.Operation.NoChange, m.getChange());
      assertEquals(0, m.getChanges().size());

      m.setTracked2("newValue2");
      assertEquals("newValue2", m.getTracked2());
      assertEquals(Change.Operation.Update, m.getChange());
      assertEquals(1, m.getChanges().size());
      assertEquals("Update ID: tracked2 = newValue2 (was value2)",
                   m.getChanges().elementAt(0).toString());

      m.getTracker().reset();
      assertEquals("newValue2", m.getTracked2());
      assertEquals(Change.Operation.NoChange, m.getChange());
      assertEquals(0, m.getChanges().size());

      m.setTracked3("newerValue3");
      assertEquals("newerValue3", m.getTracked3());
      assertEquals(Change.Operation.Update, m.getChange());
      assertEquals(1, m.getChanges().size());
      assertEquals("Update ID: tracked3 = newerValue3 (was value3)",
                   m.getChanges().elementAt(0).toString());
      
      m.setTracked3("newestValue3");
      assertEquals("newestValue3", m.getTracked3());
      assertEquals(Change.Operation.Update, m.getChange());
      assertEquals("Only latest change is reported", 1, m.getChanges().size());
      assertEquals("Only latest change is reported",
                   "Update ID: tracked3 = newestValue3 (was value3)",
                   m.getChanges().elementAt(0).toString());
      
      m.setTracked2("newestValue2");
      assertEquals("newestValue2", m.getTracked2());
      assertEquals(Change.Operation.Update, m.getChange());
      assertEquals("Multiple attribute changes", 2, m.getChanges().size());
      assertEquals("Multiple attribute changes",
                   "Update ID: tracked2 = newestValue2 (was newValue2)",
                   m.getChanges().elementAt(0).toString());
      assertEquals("Multiple attribute changes",
                   "Update ID: tracked3 = newestValue3 (was value3)",
                   m.getChanges().elementAt(1).toString());

      m.rollback("tracked3");
      assertEquals("Rollback single key", "value3", m.getTracked3());
      assertEquals(Change.Operation.Update, m.getChange());
      assertEquals("Rollback single key", 1, m.getChanges().size());
      assertEquals("Rollback single key",
                   "Update ID: tracked2 = newestValue2 (was newValue2)",
                   m.getChanges().elementAt(0).toString());
      
      m.destroy();
      assertEquals("Destroy trumps Update as a change", Change.Operation.Destroy, m.getChange());
      assertEquals(1, m.getChanges().size());
      assertEquals("Destroy ID", m.getChanges().elementAt(0).toString());

      m.rollback();
      assertEquals(Change.Operation.NoChange, m.getChange());
      assertEquals(0, m.getChanges().size());
      
      m.create();
      m.setTracked2("value2"); // just to be tidily in line with other values
      assertEquals(Change.Operation.Create, m.getChange());
      assertEquals("Create reports all attributes", 4, m.getChanges().size());
      assertEquals("Multiple attribute changes", "Create ID", m.getChanges().elementAt(0).toString());
      // these should be ordered according to the aTrackedAttributes above:
      assertEquals("Multiple attribute changes",
                   "Update ID: tracked2 = value2 (was null)",
                   m.getChanges().elementAt(1).toString());
      assertEquals("Multiple attribute changes",
                   "Update ID: tracked1 = value1 (was null)",
                   m.getChanges().elementAt(2).toString());
      assertEquals("Multiple attribute changes",
                   "Update ID: tracked3 = value3 (was null)",
                   m.getChanges().elementAt(3).toString());
      
      m.destroy();
      assertEquals("Destroy trumps Create as a change", Change.Operation.Destroy, m.getChange());
      m.rollback();

      m.create();
      m.rollback();
      assertEquals("Create cannot be rolled back", Change.Operation.Create, m.getChange());
      assertEquals("Create reports all attributes", 4, m.getChanges().size());

   }

   @Test public void mapGetter() 
   {
      MapTest m = new MapTest();
      m.setId("123");
      m.setTracked1("value1");
      m.setTracked2("value2");
      m.setTracked3("value3");
      m.setNotTracked("value4");
      m.put("noGetter", "value5");
      
      // values set
      assertEquals("123", m.getId());
      assertEquals("value1", m.getTracked1());
      assertEquals("value2", m.getTracked2());
      assertEquals("value3", m.getTracked3());
      assertEquals("value4", m.getNotTracked());

      // containsKey is false
      assertFalse(m.containsKey("tracked1"));
      assertFalse(m.containsKey("tracked2"));
      assertFalse(m.containsKey("tracked3"));
      assertFalse(m.containsKey("notTracked"));

      // get()
      assertEquals("123", m.getId());
      assertNull(m.get("tracked1"));
      assertNull(m.get("tracked2"));
      assertNull(m.get("tracked3"));
      assertNull(m.get("notTracked"));
      assertEquals("value5", m.get("noGetter"));
   }

   @Test public void cloning() 
   {
      // can't seem to make MapTest cloneable, so use Annotation instead
      Annotation m = new Annotation("123", null, "layer", "value1", "value2");
      // replace endId value with a new value
      m.setEndId("newValue2");
      m.put("notTracked", "value4");

      Annotation c = (Annotation)m.clone();
      assertEquals("123", c.getId());
      assertEquals("copy tracked values", "value1", c.getStartId());
      assertEquals("copy current, not old, tracked values", "newValue2", c.getEndId());
      assertNull("don't copy nonexistent tracked values", c.getLabel());
      assertFalse("don't copy non-tracked values", c.containsKey("notTracked"));      
   }

   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.test.TestTrackedMap");
   }
}
