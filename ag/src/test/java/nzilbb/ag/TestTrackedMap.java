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

package nzilbb.ag;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import javax.json.Json;
import javax.json.JsonObject;
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
                   m.getChanges().get(0).toString());

      m.rollback();
      assertEquals("value1", m.getTracked1());
      assertEquals(Change.Operation.NoChange, m.getChange());
      assertEquals(0, m.getChanges().size());

      m.setTracked2("newValue2");
      assertEquals("newValue2", m.getTracked2());
      assertEquals(Change.Operation.Update, m.getChange());
      assertEquals(1, m.getChanges().size());
      assertEquals("Update ID: tracked2 = newValue2 (was value2)",
                   m.getChanges().get(0).toString());

      m.getTracker().reset();
      assertEquals("newValue2", m.getTracked2());
      assertEquals(Change.Operation.NoChange, m.getChange());
      assertEquals(0, m.getChanges().size());

      m.setTracked3("newerValue3");
      assertEquals("newerValue3", m.getTracked3());
      assertEquals(Change.Operation.Update, m.getChange());
      assertEquals(1, m.getChanges().size());
      assertEquals("Update ID: tracked3 = newerValue3 (was value3)",
                   m.getChanges().get(0).toString());
      
      m.setTracked3("newestValue3");
      assertEquals("newestValue3", m.getTracked3());
      assertEquals(Change.Operation.Update, m.getChange());
      assertEquals("Only latest change is reported", 1, m.getChanges().size());
      assertEquals("Only latest change is reported",
                   "Update ID: tracked3 = newestValue3 (was value3)",
                   m.getChanges().get(0).toString());
      
      m.setTracked2("newestValue2");
      assertEquals("newestValue2", m.getTracked2());
      assertEquals(Change.Operation.Update, m.getChange());
      assertEquals("Multiple attribute changes", 2, m.getChanges().size());
      assertEquals("Multiple attribute changes",
                   "Update ID: tracked2 = newestValue2 (was newValue2)",
                   m.getChanges().get(0).toString());
      assertEquals("Multiple attribute changes",
                   "Update ID: tracked3 = newestValue3 (was value3)",
                   m.getChanges().get(1).toString());

      m.rollback("tracked3");
      assertEquals("Rollback single key", "value3", m.getTracked3());
      assertEquals(Change.Operation.Update, m.getChange());
      assertEquals("Rollback single key", 1, m.getChanges().size());
      assertEquals("Rollback single key",
                   "Update ID: tracked2 = newestValue2 (was newValue2)",
                   m.getChanges().get(0).toString());
      
      m.destroy();
      assertEquals("Destroy trumps Update as a change", Change.Operation.Destroy, m.getChange());
      assertEquals(1, m.getChanges().size());
      assertEquals("Destroy ID", m.getChanges().get(0).toString());

      m.rollback();
      assertEquals(Change.Operation.NoChange, m.getChange());
      assertEquals(0, m.getChanges().size());
      
      m.create();
      m.setTracked2("value2"); // just to be tidily in line with other values
      assertEquals(Change.Operation.Create, m.getChange());
      assertEquals("Create reports all attributes", 4, m.getChanges().size());
      assertEquals("Multiple attribute changes", "Create ID", m.getChanges().get(0).toString());
      // these should be ordered according to the aTrackedAttributes above:
      assertEquals("Multiple attribute changes",
                   "Update ID: tracked2 = value2 (was null)",
                   m.getChanges().get(1).toString());
      assertEquals("Multiple attribute changes",
                   "Update ID: tracked1 = value1 (was null)",
                   m.getChanges().get(2).toString());
      assertEquals("Multiple attribute changes",
                   "Update ID: tracked3 = value3 (was null)",
                   m.getChanges().get(3).toString());
      
      m.destroy();
      assertEquals("Create then Destroy amounts to Destroy",
                   Change.Operation.Destroy, m.getChange());
      m.rollback();

      m.create();
      m.rollback();
      assertEquals("Create cannot be rolled back", Change.Operation.Create, m.getChange());
      assertEquals("Create reports all attributes", 4, m.getChanges().size());
   }

   @Test public void changeTrackingWithNoTracker() 
   {
      MapTest m = new MapTest();
      m.setId("123");
      m.setTracked1("value1");
      m.setTracked2("value2");
      m.setTracked3("value3");
      m.setNotTracked("value4");
      
      // values set
      assertEquals("123", m.getId());
      assertEquals("value1", m.getTracked1());
      assertEquals("value2", m.getTracked2());
      assertEquals("value3", m.getTracked3());
      assertEquals("value4", m.getNotTracked());

      // initial change tracking state
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
      assertEquals("Updates to tracked values aren't tracked if no tracker set",
                   Change.Operation.NoChange, m.getChange());
      assertEquals(0, m.getChanges().size());

      m.create();
      assertEquals("Creation tracked even if there's no tracker",
                   Change.Operation.Create, m.getChange());
      assertEquals("Create reports all attributes", 4, m.getChanges().size());
      assertEquals("Multiple attribute changes", "Create ID", m.getChanges().get(0).toString());
      // these should be ordered according to the aTrackedAttributes above:
      assertEquals("Multiple attribute changes",
                   "Update ID: tracked2 = value2 (was null)",
                   m.getChanges().get(1).toString());
      assertEquals("Multiple attribute changes",
                   "Update ID: tracked1 = newValue1 (was null)",
                   m.getChanges().get(2).toString());
      assertEquals("Multiple attribute changes",
                   "Update ID: tracked3 = value3 (was null)",
                   m.getChanges().get(3).toString());
      
      m.destroy();
      assertEquals("Destroy trumps Create as a change, even though there's no tracker",
                   Change.Operation.Destroy, m.getChange());
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
      assertEquals("value1", m.get("tracked1"));
      assertEquals("value2", m.get("tracked2"));
      assertEquals("value3", m.get("tracked3"));
      assertEquals("value4", m.get("notTracked"));
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

   @Test public void fromJsonObject() 
   {
      JsonObject json = Json.createObjectBuilder()
         .add("tracked1", "1")
         .add("tracked2", "2")
         .add("notTracked", "not")
         .add("extra", "extra")
         .build();
      
      MapTest m = (MapTest)new MapTest().fromJson(json.toString());
      
      assertEquals("copy cloned property",
                   "1", m.getTracked1());
      assertEquals("copy cloned property",
                   "2", m.getTracked2());
      assertNull("don't copy nonexistent cloned property",
                 m.getTracked3());
      assertNull("don't copy non-cloned property",
                 m.getNotTracked());
      assertTrue("copy non-tracked properties in map",
                 m.containsKey("notTracked"));      
      assertTrue("copy other values in map",
                 m.containsKey("extra"));      
   }

   @Test public void fromJson() 
   {
      Annotation a = (Annotation)(new Annotation().fromJson(
         Json.createObjectBuilder()
         .add("id", "123")
         .add("layer", "layer")
         .add("startId", "value1")
         .add("endId", "value2")
         .add("confidence", 100)
         .add("annotator", "TestTrackedMap")
         .add("when", "1972-09-26T12:00:00.000")
         .add("notTracked", "value4")
         .build()));
      
      assertEquals("123", a.getId());
      assertEquals("copy tracked values", "value1", a.getStartId());
      assertEquals("copy tracked values", "value2", a.getEndId());
      assertEquals("copy tracked values", Integer.valueOf(100), a.getConfidence());
      assertEquals("copy annotator", "TestTrackedMap", a.getAnnotator());
      assertEquals("copy timestamp", new java.util.Date(72, 8, 26, 12, 0, 0), a.getWhen());
      assertNull("don't copy nonexistent tracked values", a.getLabel());
      assertTrue("copy non-tracked values", a.containsKey("notTracked"));      
   }
   
   @Test public void toJson() 
   {
      Annotation a = new Annotation("123", null, "layer", "value1", "value2");
      a.put("a-tag", "yes");
      a.put("@transient", "no");
      a.setAnnotator("TestTrackedMap");
      a.setWhen(new java.util.Date(72, 8, 26, 12, 0, 0));
      assertEquals("{"
                   // alphabetical order for bean properties
                   +"\"annotator\":\"TestTrackedMap\""
                   +",\"endId\":\"value2\""
                   +",\"id\":\"123\""
                   +",\"layerId\":\"layer\""
                   +",\"ordinal\":0" // not explicitly set
                   +",\"startId\":\"value1\""
                   +",\"when\":\"1972-09-26T12:00:00.000\""
                   // map entries have unpredictable order
                   +",\"a-tag\":\"yes\"}", a.toJsonString());
   }

   @Test public void toJsonWithTrackedMapAttribute() 
   {
      Annotation a = new Annotation("123", null, "layer", "value1", "value2");
      a.put("a-tag", "yes");
      a.put("@transient", "no");
      a.setAnnotator("TestTrackedMap");
      a.setWhen(new java.util.Date(72, 8, 26, 12, 0, 0));
      a.put("start", new Anchor("anchorId", 123.456));
      assertEquals("{"
                   // alphabetical order for bean properties
                   +"\"annotator\":\"TestTrackedMap\""
                   +",\"endId\":\"value2\""
                   +",\"id\":\"123\""
                   +",\"layerId\":\"layer\""
                   +",\"ordinal\":0" // not explicitly set
                   +",\"startId\":\"value1\""
                   +",\"when\":\"1972-09-26T12:00:00.000\""
                   // map entries have unpredictable order
                   +",\"a-tag\":\"yes\""
                   +",\"start\":{"
                   +"\"id\":\"anchorId\",\"offset\":123.456,\"startOf\":{},\"endOf\":{}}}",
                   a.toJsonString());
   }

   @Test public void fromJsonWithMapAttribute() 
   {
      // Layer has an attribute that's a map, so we need to ensure it's interpreted
      Layer l = new Layer(
         Json.createObjectBuilder()
         .add("id", "layer")
         .add("parentId", "parent")
         .add("description", "Desc")
         .add("alignment", 2)
         .add("peers", true)
         .add("peersOverlap", false)
         .add("validLabels", Json.createObjectBuilder()
              .add("v1", "value1")
              .add("v2", "value2"))
         .build());
      
      assertEquals("layer", l.getId());
      assertEquals("parent", l.getParentId());
      assertEquals("Desc", l.getDescription());
      assertEquals(2, l.getAlignment());
      assertEquals(Boolean.TRUE, l.getPeers());
      assertEquals(Boolean.FALSE, l.getPeersOverlap());
      assertNotNull("validLabels map set",
                    l.getValidLabels());
      assertEquals("validLabels map right size",
                    2, l.getValidLabels().size());
      assertEquals("validLabels value 1",
                    "value1", l.getValidLabels().get("v1"));
      assertEquals("validLabels value 2",
                    "value2", l.getValidLabels().get("v2"));
   }

   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.TestTrackedMap");
   }
}
