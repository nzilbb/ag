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
package nzilbb.editpath;
	      
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;

import nzilbb.editpath.*;

/**
 * Tests for non-character edit path computations.
 */
public class TestMinimumEditPath
{
   @Test 
   public void nullsInPaths()
   {
      MinimumEditPath<Integer> mp = new MinimumEditPath<Integer>();

      Vector<Integer> vFrom = new Vector<Integer>();
      Vector<Integer> vTo = new Vector<Integer>();

      vFrom.add(1);
      vFrom.add(2);
      vFrom.add(3);
      vFrom.add(null);
      vFrom.add(5);
      vFrom.add(8);

      vTo.add(2);
      vTo.add(4);
      vTo.add(null);
      vTo.add(8);
      vTo.add(null);

      List<EditStep<Integer>> path = mp.minimumEditPath(vFrom, vTo);
      assertNotNull(path);

      System.out.println("Path:");
      for (EditStep<Integer> step: path) System.out.println(step.toString());

      // right number of steps
      assertEquals(7, path.size());

      // all no-change
      assertEquals(EditStep.StepOperation.DELETE, path.get(0).getOperation()); // 1/
      assertEquals(EditStep.StepOperation.NONE, path.get(1).getOperation());   // 2/2
      assertEquals(EditStep.StepOperation.CHANGE, path.get(2).getOperation()); // 3/4
      assertEquals(EditStep.StepOperation.NONE, path.get(3).getOperation());   // n/n
      assertEquals(EditStep.StepOperation.DELETE, path.get(4).getOperation()); // 5/
      assertEquals(EditStep.StepOperation.NONE, path.get(5).getOperation());   // 8/8
      assertEquals(EditStep.StepOperation.INSERT, path.get(6).getOperation()); //  /n

      // 'from' set
      assertEquals(Integer.valueOf(1), path.get(0).getFrom());
      assertEquals(Integer.valueOf(2), path.get(1).getFrom());
      assertEquals(Integer.valueOf(3), path.get(2).getFrom());
      assertNull(path.get(3).getFrom());
      assertEquals(Integer.valueOf(5), path.get(4).getFrom());
      assertEquals(Integer.valueOf(8), path.get(5).getFrom());
      assertNull(path.get(6).getFrom());

      // 'to' set
      assertNull(path.get(0).getTo());
      assertEquals(Integer.valueOf(2), path.get(1).getTo());
      assertEquals(Integer.valueOf(4), path.get(2).getTo());
      assertNull(path.get(3).getTo());
      assertNull(path.get(4).getTo());
      assertEquals(Integer.valueOf(8), path.get(5).getTo());
      assertNull(path.get(6).getTo());

      // distances set
      assertEquals(1, path.get(0).getStepDistance(), 0.0);
      assertEquals(0, path.get(1).getStepDistance(), 0.0);
      assertEquals(1, path.get(2).getStepDistance(), 0.0);
      assertEquals(0, path.get(3).getStepDistance(), 0.0);
      assertEquals(1, path.get(4).getStepDistance(), 0.0);
      assertEquals(0, path.get(5).getStepDistance(), 0.0);
      assertEquals(1, path.get(6).getStepDistance(), 0.0);

      // total distances set
      assertEquals(1, path.get(0).totalDistance(), 0.0);
      assertEquals(1, path.get(1).totalDistance(), 0.0);
      assertEquals(2, path.get(2).totalDistance(), 0.0);
      assertEquals(2, path.get(3).totalDistance(), 0.0);
      assertEquals(3, path.get(4).totalDistance(), 0.0);
      assertEquals(3, path.get(5).totalDistance(), 0.0);
      assertEquals(4, path.get(6).totalDistance(), 0.0);

      // backtraces set
      assertNull(path.get(0).getBackTrace());
      assertEquals(path.get(0), path.get(1).getBackTrace());
      assertEquals(path.get(1), path.get(2).getBackTrace());
      assertEquals(path.get(2), path.get(3).getBackTrace());
      assertEquals(path.get(3), path.get(4).getBackTrace());
      assertEquals(path.get(4), path.get(5).getBackTrace());
      assertEquals(path.get(5), path.get(6).getBackTrace());

      // fromIndices set
      assertEquals(0, path.get(0).getFromIndex());
      assertEquals(1, path.get(1).getFromIndex());
      assertEquals(2, path.get(2).getFromIndex());
      assertEquals(3, path.get(3).getFromIndex());
      assertEquals(4, path.get(4).getFromIndex());
      assertEquals(5, path.get(5).getFromIndex());
      assertEquals(5, path.get(6).getFromIndex());

      // toIndices set
      assertEquals(0, path.get(0).getToIndex());
      assertEquals(0, path.get(1).getToIndex());
      assertEquals(1, path.get(2).getToIndex());
      assertEquals(2, path.get(3).getToIndex());
      assertEquals(2, path.get(4).getToIndex());
      assertEquals(3, path.get(5).getToIndex());
      assertEquals(4, path.get(6).getToIndex());

      // edit distance
      assertEquals(4, mp.minimumEditDistance(vFrom, vTo), 0.0);
   }

   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nz.ac.canterbury.ling.util.TestMinimumEditPath");
   }
}
