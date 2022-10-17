//
// Copyright 2016-2022 New Zealand Institute of Language, Brain and Behaviour, 
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

public class TestEditStep {
   @Test public void constructor() {
      // default constructor
      EditStep<Integer> step0 = new EditStep<Integer>();
      assertNull(step0.getFrom());
      assertNull(step0.getTo());
      assertEquals(0.0, step0.getStepDistance(), 0.0);
      assertEquals(EditStep.StepOperation.NONE, step0.getOperation());

      // constructor with no backtrace
      EditStep<Integer> step1 = new EditStep<Integer>(1,1,0,EditStep.StepOperation.NONE);
      assertEquals(Integer.valueOf(1), step1.getFrom());
      assertEquals(Integer.valueOf(1), step1.getTo());
      assertEquals(0.0, step1.getStepDistance(), 0.0);
      assertEquals(EditStep.StepOperation.NONE, step1.getOperation());

      // constructor with backtrace
      EditStep<Integer> step2 = new EditStep<Integer>(2,-2,1,EditStep.StepOperation.CHANGE, step1);
      assertEquals(Integer.valueOf(2), step2.getFrom());
      assertEquals(Integer.valueOf(-2), step2.getTo());
      assertEquals(1.0, step2.getStepDistance(), 0.0);
      assertEquals(EditStep.StepOperation.CHANGE, step2.getOperation());
      assertEquals(step1, step2.getBackTrace());

      // no-distance change is possible
      EditStep<Integer> step3 = new EditStep<Integer>(3,33,0,EditStep.StepOperation.CHANGE, step2);
      assertEquals(Integer.valueOf(3), step3.getFrom());
      assertEquals(Integer.valueOf(33), step3.getTo());
      assertEquals(0.0, step3.getStepDistance(), 0.0);
      assertEquals(EditStep.StepOperation.CHANGE, step3.getOperation());
      assertEquals(step2, step3.getBackTrace());

      // delete
      EditStep<Integer> step4 = new EditStep<Integer>(4,null,50,EditStep.StepOperation.DELETE, step3);
      assertEquals(Integer.valueOf(4), step4.getFrom());
      assertNull(step4.getTo());
      assertEquals(50.0, step4.getStepDistance(), 0.0);
      assertEquals(EditStep.StepOperation.DELETE, step4.getOperation());
      assertEquals(step3, step4.getBackTrace());

      // insert
      EditStep<Integer> step5 = new EditStep<Integer>(null,5,25,EditStep.StepOperation.INSERT, step4);
      assertNull(step5.getFrom());
      assertEquals(Integer.valueOf(5), step5.getTo());
      assertEquals(25.0, step5.getStepDistance(), 0.0);
      assertEquals(EditStep.StepOperation.INSERT, step5.getOperation());
      assertEquals(step4, step5.getBackTrace());
   }

   @Test public void stepVsTotalDistance() {
      EditStep<Integer> step1 = new EditStep<Integer>(1,1,0,EditStep.StepOperation.NONE);
      EditStep<Integer> step2 = new EditStep<Integer>(2,-2,0,EditStep.StepOperation.CHANGE, step1);
      EditStep<Integer> step3 = new EditStep<Integer>(3,33,100,EditStep.StepOperation.CHANGE, step2);
      EditStep<Integer> step4 = new EditStep<Integer>(4,null,50,EditStep.StepOperation.DELETE, step3);
      EditStep<Integer> step5 = new EditStep<Integer>(null,5,25,EditStep.StepOperation.INSERT, step4);
      EditStep<Integer> step6 = new EditStep<Integer>(6,6,0,EditStep.StepOperation.NONE, step5);
      assertEquals(0.0, step1.getStepDistance(), 0.0);
      assertEquals(0.0, step1.totalDistance(), 0.0);

      assertEquals(0.0, step2.getStepDistance(), 0.0);
      assertEquals(0.0, step2.totalDistance(), 0.0);

      assertEquals(100.0, step3.getStepDistance(), 0.0);
      assertEquals(100.0, step3.totalDistance(), 0.0);

      assertEquals(50.0, step4.getStepDistance(), 0.0);
      assertEquals(150.0, step4.totalDistance(), 0.0);

      assertEquals(25.0, step5.getStepDistance(), 0.0);
      assertEquals(175.0, step5.totalDistance(), 0.0);

      assertEquals(0.0, step6.getStepDistance(), 0.0);
      assertEquals(175.0, step6.totalDistance(), 0.0);

   }
   
   public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nz.ac.canterbury.ling.util.TestEditStep");
   }
}
