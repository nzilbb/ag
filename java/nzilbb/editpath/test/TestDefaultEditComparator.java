//
// Copyright 2016 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of LaBB-CAT.
//
//    LaBB-CAT is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 2 of the License, or
//    (at your option) any later version.
//
//    LaBB-CAT is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with LaBB-CAT; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package nzilbb.editpath.test;
	      
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;

import nzilbb.editpath.*;

public class TestDefaultEditComparator
{
   @Test 
   public void basicCompare()
   {
      // default constructor
      DefaultEditComparator<Integer> c = new DefaultEditComparator<Integer>();

      // comparator's default equals comparator
      assertEquals(0, c.getComparator().compare(Integer.valueOf(1), Integer.valueOf(1)));
      assertNotEquals(0, c.getComparator().compare(Integer.valueOf(1), Integer.valueOf(2)));

      // no change
      EditStep<Integer> step = c.compare(1,1);
      assertEquals(Integer.valueOf(1), step.getFrom());
      assertEquals(Integer.valueOf(1), step.getTo());
      assertEquals(0, step.getStepDistance());
      assertEquals(EditStep.StepOperation.NONE, step.getOperation());
      
      // change
      step = c.compare(1,2);
      assertEquals(Integer.valueOf(1), step.getFrom());
      assertEquals(Integer.valueOf(2), step.getTo());
      assertEquals(1, step.getStepDistance());
      assertEquals(EditStep.StepOperation.CHANGE, step.getOperation());

      // delete
      step = c.delete(1);
      assertEquals(1, step.getStepDistance());
      assertEquals(Integer.valueOf(1), step.getFrom());
      assertNull(step.getTo());
      assertEquals(EditStep.StepOperation.DELETE, step.getOperation());

      // insert
      step = c.insert(1);
      assertEquals(1, step.getStepDistance());
      assertEquals(Integer.valueOf(1), step.getTo());
      assertNull(step.getFrom());
      assertEquals(EditStep.StepOperation.INSERT, step.getOperation());

      // different distance
      c = new DefaultEditComparator<Integer>(10);
      step = c.compare(1,2);
      assertEquals(Integer.valueOf(1), step.getFrom());
      assertEquals(Integer.valueOf(2), step.getTo());
      assertEquals(10, step.getStepDistance());
      assertEquals(EditStep.StepOperation.CHANGE, step.getOperation());

      // delete
      assertEquals(10, c.delete(1).getStepDistance());

      // insert
      assertEquals(10, c.insert(1).getStepDistance());

      // different distances
      c = new DefaultEditComparator<Integer>(10, 20, 30);
      step = c.compare(1,2);
      assertEquals(30, step.getStepDistance());

      // delete
      assertEquals(20, c.delete(1).getStepDistance());

      // insert
      assertEquals(10, c.insert(1).getStepDistance());

      
   }

   /**
    * Test compare where one or both of the arguments is null. 
    * Not actually intended to be possible, as used by MinimumEditPath
    * but it could conceivably happen if the given sequences include nulls.  
    * If this is the case, then the result operation isn't INSERT or DELETE, but rather CHANGE
    * i.e. you CHANGE an element from something to null, or from null to something.
    */ 
   @Test 
   public void compareIncludingNull()
   {
      // default constructor
      DefaultEditComparator<Integer> c = new DefaultEditComparator<Integer>();

      // no to
      EditStep<Integer> step = c.compare(1,null);
      assertEquals(Integer.valueOf(1), step.getFrom());
      assertNull(step.getTo());
      assertEquals(1, step.getStepDistance());
      assertEquals(EditStep.StepOperation.CHANGE, step.getOperation());
      
      // no from
      step = c.compare(null,2);
      assertNull(step.getFrom());
      assertEquals(Integer.valueOf(2), step.getTo());
      assertEquals(1, step.getStepDistance());
      assertEquals(EditStep.StepOperation.CHANGE, step.getOperation());

      // neither from nor to
      c = new DefaultEditComparator<Integer>(10);
      step = c.compare(null,null);
      assertNull(step.getFrom());
      assertNull(step.getTo());
      assertEquals(0, step.getStepDistance());
      assertEquals(EditStep.StepOperation.NONE, step.getOperation());

      // delete
      step = c.delete(null);
      assertEquals(10, step.getStepDistance());
      assertNull(step.getFrom());
      assertNull(step.getTo());
      assertEquals(EditStep.StepOperation.DELETE, step.getOperation());

      // insert
      step = c.insert(null);
      assertEquals(10, step.getStepDistance());
      assertNull(step.getFrom());
      assertNull(step.getTo());
      assertEquals(EditStep.StepOperation.INSERT, step.getOperation());
   }

   @Test 
   public void customisedEquals()
   {
      // default constructor
      DefaultEditComparator<String> c = new DefaultEditComparator<String>(
	 new EqualsComparator<String>()
	 {
	    // case-insensitive comparison
	    public int compare(String o1, String o2)
	    {
	       return o1.toLowerCase().compareTo(o2.toLowerCase());


	    }
	 }
	 );

      // comparator's equals comparator
      assertEquals("really equal", 0, c.getComparator().compare("this", "this"));
      assertEquals("equal but different case", 0, c.getComparator().compare("this", "This"));
      assertNotEquals("not equal", 0, c.getComparator().compare("this", "that"));

      // no change
      EditStep<String> step = c.compare("this","This");
      assertEquals("this", step.getFrom());
      assertEquals("This", step.getTo());
      assertEquals(0, step.getStepDistance());
      assertEquals(EditStep.StepOperation.NONE, step.getOperation());
      
      // change
      step = c.compare("this","that");
      assertEquals("this", step.getFrom());
      assertEquals("that", step.getTo());
      assertEquals(1, step.getStepDistance());
      assertEquals(EditStep.StepOperation.CHANGE, step.getOperation());
      
   }

   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nz.ac.canterbury.ling.util.test.TestDefaultEditComparator");
   }
}
