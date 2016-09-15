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

/**
 * These tests ensure that basic string paths can easily be worked with, 
 * but also test the general beahviour of MinimumEditPath
 */
public class TestMinimumEditPathString
{
   @Test 
   public void stringEqual()
   {
      MinimumEditPathString mp = new MinimumEditPathString();

      List<EditStep<Character>> path = mp.minimumEditPath("this", "this");
      assertNotNull(path);

      // right number of steps
      assertEquals(4, path.size());

      // all no-change
      assertEquals(EditStep.StepOperation.NONE, path.get(0).getOperation());
      assertEquals(EditStep.StepOperation.NONE, path.get(1).getOperation());
      assertEquals(EditStep.StepOperation.NONE, path.get(2).getOperation());
      assertEquals(EditStep.StepOperation.NONE, path.get(3).getOperation());

      // 'from' set
      assertEquals(new Character('t'), path.get(0).getFrom());
      assertEquals(new Character('h'), path.get(1).getFrom());
      assertEquals(new Character('i'), path.get(2).getFrom());
      assertEquals(new Character('s'), path.get(3).getFrom());

      // 'to' set
      assertEquals(new Character('t'), path.get(0).getTo());
      assertEquals(new Character('h'), path.get(1).getTo());
      assertEquals(new Character('i'), path.get(2).getTo());
      assertEquals(new Character('s'), path.get(3).getTo());

      // distances set
      assertEquals(0, path.get(0).getStepDistance());
      assertEquals(0, path.get(1).getStepDistance());
      assertEquals(0, path.get(2).getStepDistance());
      assertEquals(0, path.get(3).getStepDistance());

      // total distances set
      assertEquals(0, path.get(0).totalDistance());
      assertEquals(0, path.get(1).totalDistance());
      assertEquals(0, path.get(2).totalDistance());
      assertEquals(0, path.get(3).totalDistance());

      // backtraces set
      assertNull(path.get(0).getBackTrace());
      assertEquals(path.get(0), path.get(1).getBackTrace());
      assertEquals(path.get(1), path.get(2).getBackTrace());
      assertEquals(path.get(2), path.get(3).getBackTrace());

      // fromIndices set
      assertEquals(0, path.get(0).getFromIndex());
      assertEquals(1, path.get(1).getFromIndex());
      assertEquals(2, path.get(2).getFromIndex());
      assertEquals(3, path.get(3).getFromIndex());

      // toIndices set
      assertEquals(0, path.get(0).getToIndex());
      assertEquals(1, path.get(1).getToIndex());
      assertEquals(2, path.get(2).getToIndex());
      assertEquals(3, path.get(3).getToIndex());

      // edit distance
      assertEquals(0, mp.minimumEditDistance("this", "this"));
   }

   @Test 
   public void thisToThat()
   {
      MinimumEditPathString mp = new MinimumEditPathString();

      List<EditStep<Character>> path = mp.minimumEditPath("this", "that");
      assertNotNull(path);

      // right number of steps
      assertEquals(4, path.size());

      // changes
      assertEquals(EditStep.StepOperation.NONE, path.get(0).getOperation());
      assertEquals(EditStep.StepOperation.NONE, path.get(1).getOperation());
      assertEquals(EditStep.StepOperation.CHANGE, path.get(2).getOperation());
      assertEquals(EditStep.StepOperation.CHANGE, path.get(3).getOperation());

      // 'from' set
      assertEquals(new Character('t'), path.get(0).getFrom());
      assertEquals(new Character('h'), path.get(1).getFrom());
      assertEquals(new Character('i'), path.get(2).getFrom());
      assertEquals(new Character('s'), path.get(3).getFrom());

      // 'to' set
      assertEquals(new Character('t'), path.get(0).getTo());
      assertEquals(new Character('h'), path.get(1).getTo());
      assertEquals(new Character('a'), path.get(2).getTo());
      assertEquals(new Character('t'), path.get(3).getTo());

      // distances set
      assertEquals(0, path.get(0).getStepDistance());
      assertEquals(0, path.get(1).getStepDistance());
      assertEquals(1, path.get(2).getStepDistance());
      assertEquals(1, path.get(3).getStepDistance());

      // total distances set
      assertEquals(0, path.get(0).totalDistance());
      assertEquals(0, path.get(1).totalDistance());
      assertEquals(1, path.get(2).totalDistance());
      assertEquals(2, path.get(3).totalDistance());

      // backtraces set
      assertNull(path.get(0).getBackTrace());
      assertEquals(path.get(0), path.get(1).getBackTrace());
      assertEquals(path.get(1), path.get(2).getBackTrace());
      assertEquals(path.get(2), path.get(3).getBackTrace());

      // fromIndices set
      assertEquals(0, path.get(0).getFromIndex());
      assertEquals(1, path.get(1).getFromIndex());
      assertEquals(2, path.get(2).getFromIndex());
      assertEquals(3, path.get(3).getFromIndex());

      // toIndices set
      assertEquals(0, path.get(0).getToIndex());
      assertEquals(1, path.get(1).getToIndex());
      assertEquals(2, path.get(2).getToIndex());
      assertEquals(3, path.get(3).getToIndex());

      // edit distance
      assertEquals(2, mp.minimumEditDistance("this", "that"));
   }

   @Test 
   public void emptyToThat()
   {
      MinimumEditPathString mp = new MinimumEditPathString();

      List<EditStep<Character>> path = mp.minimumEditPath("", "that");
      assertNotNull(path);

      // right number of steps
      assertEquals(4, path.size());

      // all insert
      assertEquals(EditStep.StepOperation.INSERT, path.get(0).getOperation());
      assertEquals(EditStep.StepOperation.INSERT, path.get(1).getOperation());
      assertEquals(EditStep.StepOperation.INSERT, path.get(2).getOperation());
      assertEquals(EditStep.StepOperation.INSERT, path.get(3).getOperation());

      // 'from' set
      assertNull(path.get(0).getFrom());
      assertNull(path.get(1).getFrom());
      assertNull(path.get(2).getFrom());
      assertNull(path.get(3).getFrom());

      // 'to' set
      assertEquals(new Character('t'), path.get(0).getTo());
      assertEquals(new Character('h'), path.get(1).getTo());
      assertEquals(new Character('a'), path.get(2).getTo());
      assertEquals(new Character('t'), path.get(3).getTo());

      // distances set
      assertEquals(1, path.get(0).getStepDistance());
      assertEquals(1, path.get(1).getStepDistance());
      assertEquals(1, path.get(2).getStepDistance());
      assertEquals(1, path.get(3).getStepDistance());

      // total distances set
      assertEquals(1, path.get(0).totalDistance());
      assertEquals(2, path.get(1).totalDistance());
      assertEquals(3, path.get(2).totalDistance());
      assertEquals(4, path.get(3).totalDistance());

      // backtraces set
      assertNull(path.get(0).getBackTrace());
      assertEquals(path.get(0), path.get(1).getBackTrace());
      assertEquals(path.get(1), path.get(2).getBackTrace());
      assertEquals(path.get(2), path.get(3).getBackTrace());

      // fromIndices set
      assertEquals(0, path.get(0).getFromIndex());
      assertEquals(0, path.get(1).getFromIndex());
      assertEquals(0, path.get(2).getFromIndex());
      assertEquals(0, path.get(3).getFromIndex());

      // toIndices set
      assertEquals(0, path.get(0).getToIndex());
      assertEquals(1, path.get(1).getToIndex());
      assertEquals(2, path.get(2).getToIndex());
      assertEquals(3, path.get(3).getToIndex());

      // edit distance
      assertEquals(4, mp.minimumEditDistance("", "that"));
   }

   @Test 
   public void thisToEmpty()
   {
      MinimumEditPathString mp = new MinimumEditPathString();

      List<EditStep<Character>> path = mp.minimumEditPath("this", "");
      assertNotNull(path);

      // right number of steps
      assertEquals(4, path.size());

      // all delete
      assertEquals(EditStep.StepOperation.DELETE, path.get(0).getOperation());
      assertEquals(EditStep.StepOperation.DELETE, path.get(1).getOperation());
      assertEquals(EditStep.StepOperation.DELETE, path.get(2).getOperation());
      assertEquals(EditStep.StepOperation.DELETE, path.get(3).getOperation());

      // 'from' set
      assertEquals(new Character('t'), path.get(0).getFrom());
      assertEquals(new Character('h'), path.get(1).getFrom());
      assertEquals(new Character('i'), path.get(2).getFrom());
      assertEquals(new Character('s'), path.get(3).getFrom());

      // 'to' set
      assertNull(path.get(0).getTo());
      assertNull(path.get(1).getTo());
      assertNull(path.get(2).getTo());
      assertNull(path.get(3).getTo());

      // distances set
      assertEquals(1, path.get(0).getStepDistance());
      assertEquals(1, path.get(1).getStepDistance());
      assertEquals(1, path.get(2).getStepDistance());
      assertEquals(1, path.get(3).getStepDistance());

      // total distances set
      assertEquals(1, path.get(0).totalDistance());
      assertEquals(2, path.get(1).totalDistance());
      assertEquals(3, path.get(2).totalDistance());
      assertEquals(4, path.get(3).totalDistance());

      // backtraces set
      assertNull(path.get(0).getBackTrace());
      assertEquals(path.get(0), path.get(1).getBackTrace());
      assertEquals(path.get(1), path.get(2).getBackTrace());
      assertEquals(path.get(2), path.get(3).getBackTrace());

      // fromIndices set
      assertEquals(0, path.get(0).getFromIndex());
      assertEquals(1, path.get(1).getFromIndex());
      assertEquals(2, path.get(2).getFromIndex());
      assertEquals(3, path.get(3).getFromIndex());

      // toIndices set
      assertEquals(0, path.get(0).getToIndex());
      assertEquals(0, path.get(1).getToIndex());
      assertEquals(0, path.get(2).getToIndex());
      assertEquals(0, path.get(3).getToIndex());

      // edit distance
      assertEquals(4, mp.minimumEditDistance("this", ""));
   }

   @Test 
   public void emptyToEmpty()
   {
      MinimumEditPathString mp = new MinimumEditPathString();

      List<EditStep<Character>> path = mp.minimumEditPath("", "");
      assertNotNull(path);

      // right number of steps
      assertEquals(0, path.size());

      // edit distance
      assertEquals(0, mp.minimumEditDistance("", ""));
   }

   @Test 
   public void defaultCosts()
   {
      MinimumEditPathString mp = new MinimumEditPathString();

      List<EditStep<Character>> path = mp.minimumEditPath("plomo", "oro");
      assertNotNull(path);

      // right number of steps
      assertEquals(5, path.size());

      // changes
      assertEquals(EditStep.StepOperation.DELETE, path.get(0).getOperation());
      assertEquals(EditStep.StepOperation.DELETE, path.get(1).getOperation());
      assertEquals(EditStep.StepOperation.NONE, path.get(2).getOperation());
      assertEquals(EditStep.StepOperation.CHANGE, path.get(3).getOperation());
      assertEquals(EditStep.StepOperation.NONE, path.get(4).getOperation());

      // 'from' set
      assertEquals(new Character('p'), path.get(0).getFrom());
      assertEquals(new Character('l'), path.get(1).getFrom());
      assertEquals(new Character('o'), path.get(2).getFrom());
      assertEquals(new Character('m'), path.get(3).getFrom());
      assertEquals(new Character('o'), path.get(4).getFrom());

      // 'to' set
      assertNull(path.get(0).getTo());
      assertNull(path.get(1).getTo());
      assertEquals(new Character('o'), path.get(2).getTo());
      assertEquals(new Character('r'), path.get(3).getTo());
      assertEquals(new Character('o'), path.get(4).getTo());

      // distances set
      assertEquals(1, path.get(0).getStepDistance());
      assertEquals(1, path.get(1).getStepDistance());
      assertEquals(0, path.get(2).getStepDistance());
      assertEquals(1, path.get(3).getStepDistance());
      assertEquals(0, path.get(4).getStepDistance());

      // total distances set
      assertEquals(1, path.get(0).totalDistance());
      assertEquals(2, path.get(1).totalDistance());
      assertEquals(2, path.get(2).totalDistance());
      assertEquals(3, path.get(3).totalDistance());
      assertEquals(3, path.get(4).totalDistance());

      // backtraces set
      assertNull(path.get(0).getBackTrace());
      assertEquals(path.get(0), path.get(1).getBackTrace());
      assertEquals(path.get(1), path.get(2).getBackTrace());
      assertEquals(path.get(2), path.get(3).getBackTrace()); 
      assertEquals(path.get(3), path.get(4).getBackTrace());

      // fromIndices set
      assertEquals(0, path.get(0).getFromIndex());
      assertEquals(1, path.get(1).getFromIndex());
      assertEquals(2, path.get(2).getFromIndex());
      assertEquals(3, path.get(3).getFromIndex());
      assertEquals(4, path.get(4).getFromIndex());

      // toIndices set
      assertEquals(0, path.get(0).getToIndex());
      assertEquals(0, path.get(1).getToIndex());
      assertEquals(0, path.get(2).getToIndex());
      assertEquals(1, path.get(3).getToIndex());
      assertEquals(2, path.get(4).getToIndex());

   }

   @Test 
   public void defaultExpensiveChange()
   {
      MinimumEditPathString mp = new MinimumEditPathString(new DefaultEditComparator<Character>(1,1,2));

      List<EditStep<Character>> path = mp.minimumEditPath("plomo", "oro");
      assertNotNull(path);

      // right number of steps
      assertEquals(6, path.size());

      // changes
      assertEquals(EditStep.StepOperation.DELETE, path.get(0).getOperation());
      assertEquals(EditStep.StepOperation.DELETE, path.get(1).getOperation());
      assertEquals(EditStep.StepOperation.NONE, path.get(2).getOperation());
      assertEquals(EditStep.StepOperation.INSERT, path.get(3).getOperation());
      assertEquals(EditStep.StepOperation.DELETE, path.get(4).getOperation());
      assertEquals(EditStep.StepOperation.NONE, path.get(5).getOperation());

      // 'from' set
      assertEquals(new Character('p'), path.get(0).getFrom()); // delete p
      assertEquals(new Character('l'), path.get(1).getFrom()); // delete l
      assertEquals(new Character('o'), path.get(2).getFrom()); 
      assertNull(path.get(3).getFrom());                       // insert r
      assertEquals(new Character('m'), path.get(4).getFrom()); // delete m
      assertEquals(new Character('o'), path.get(5).getFrom());

      // 'to' set
      assertNull(path.get(0).getTo());                       // delete p
      assertNull(path.get(1).getTo());                       // delete l
      assertEquals(new Character('o'), path.get(2).getTo());
      assertEquals(new Character('r'), path.get(3).getTo()); // insert r
      assertNull(path.get(4).getTo());                       // delete m
      assertEquals(new Character('o'), path.get(5).getTo());

      // distances set
      assertEquals(1, path.get(0).getStepDistance()); // delete p
      assertEquals(1, path.get(1).getStepDistance()); // delete l
      assertEquals(0, path.get(2).getStepDistance());
      assertEquals(1, path.get(3).getStepDistance()); // insert r
      assertEquals(1, path.get(4).getStepDistance()); // delete m
      assertEquals(0, path.get(5).getStepDistance());

      // total distances set
      assertEquals(1, path.get(0).totalDistance()); // delete p
      assertEquals(2, path.get(1).totalDistance()); // delete l
      assertEquals(2, path.get(2).totalDistance());
      assertEquals(3, path.get(3).totalDistance()); // insert r
      assertEquals(4, path.get(4).totalDistance()); // delete m
      assertEquals(4, path.get(5).totalDistance());

      // backtraces set
      assertNull(path.get(0).getBackTrace());
      assertEquals(path.get(0), path.get(1).getBackTrace());
      assertEquals(path.get(1), path.get(2).getBackTrace());
      assertEquals(path.get(2), path.get(3).getBackTrace()); 
      assertEquals(path.get(3), path.get(4).getBackTrace());
      assertEquals(path.get(4), path.get(5).getBackTrace());

      // fromIndices set
      assertEquals(0, path.get(0).getFromIndex());
      assertEquals(1, path.get(1).getFromIndex());
      assertEquals(2, path.get(2).getFromIndex());
      assertEquals(2, path.get(3).getFromIndex());
      assertEquals(3, path.get(4).getFromIndex());
      assertEquals(4, path.get(5).getFromIndex());

      // toIndices set
      assertEquals(0, path.get(0).getToIndex());
      assertEquals(0, path.get(1).getToIndex());
      assertEquals(0, path.get(2).getToIndex());
      assertEquals(1, path.get(3).getToIndex());
      assertEquals(1, path.get(4).getToIndex());
      assertEquals(2, path.get(5).getToIndex());

      // collapse
      mp.collapse(path);

      // right number of steps
      assertEquals("PATH " + path, 5, path.size());

      // changes
      assertEquals(EditStep.StepOperation.DELETE, path.get(0).getOperation());
      assertEquals(EditStep.StepOperation.DELETE, path.get(1).getOperation());
      assertEquals(EditStep.StepOperation.NONE, path.get(2).getOperation());
      assertEquals(EditStep.StepOperation.CHANGE, path.get(3).getOperation());
      assertEquals(EditStep.StepOperation.NONE, path.get(4).getOperation());

      // 'from' set
      assertEquals(new Character('p'), path.get(0).getFrom()); // delete p
      assertEquals(new Character('l'), path.get(1).getFrom()); // delete l
      assertEquals(new Character('o'), path.get(2).getFrom()); 
      assertEquals(new Character('m'), path.get(3).getFrom()); // change m to r
      assertEquals(new Character('o'), path.get(4).getFrom());

      // 'to' set
      assertNull(path.get(0).getTo());                       // delete p
      assertNull(path.get(1).getTo());                       // delete l
      assertEquals(new Character('o'), path.get(2).getTo());
      assertEquals(new Character('r'), path.get(3).getTo()); // change m to r
      assertEquals(new Character('o'), path.get(4).getTo());

      // distances set
      assertEquals(1, path.get(0).getStepDistance()); // delete p
      assertEquals(1, path.get(1).getStepDistance()); // delete l
      assertEquals(0, path.get(2).getStepDistance());
      assertEquals(2, path.get(3).getStepDistance()); // change m to r
      assertEquals(0, path.get(4).getStepDistance());

      // total distances set
      assertEquals(1, path.get(0).totalDistance()); // delete p
      assertEquals(2, path.get(1).totalDistance()); // delete l
      assertEquals(2, path.get(2).totalDistance());
      assertEquals(4, path.get(3).totalDistance()); // chaneg m to r
      assertEquals(4, path.get(4).totalDistance());

   }

   @Test 
   public void customCosts()
   {
      MinimumEditPathString mp = new MinimumEditPathString(new DefaultEditComparator<Character>(10));

      List<EditStep<Character>> path = mp.minimumEditPath("plomo", "oro");
      assertNotNull(path);

      // right number of steps
      assertEquals(5, path.size());

      // changes
      assertEquals(EditStep.StepOperation.DELETE, path.get(0).getOperation());
      assertEquals(EditStep.StepOperation.DELETE, path.get(1).getOperation());
      assertEquals(EditStep.StepOperation.NONE, path.get(2).getOperation());
      assertEquals(EditStep.StepOperation.CHANGE, path.get(3).getOperation());
      assertEquals(EditStep.StepOperation.NONE, path.get(4).getOperation());

      // 'from' set
      assertEquals(new Character('p'), path.get(0).getFrom());
      assertEquals(new Character('l'), path.get(1).getFrom());
      assertEquals(new Character('o'), path.get(2).getFrom());
      assertEquals(new Character('m'), path.get(3).getFrom());
      assertEquals(new Character('o'), path.get(4).getFrom());

      // 'to' set
      assertNull(path.get(0).getTo());
      assertNull(path.get(1).getTo());
      assertEquals(new Character('o'), path.get(2).getTo());
      assertEquals(new Character('r'), path.get(3).getTo());
      assertEquals(new Character('o'), path.get(4).getTo());

      // distances set
      assertEquals(10, path.get(0).getStepDistance());
      assertEquals(10, path.get(1).getStepDistance());
      assertEquals(0, path.get(2).getStepDistance());
      assertEquals(10, path.get(3).getStepDistance());
      assertEquals(0, path.get(4).getStepDistance());

      // total distances set
      assertEquals(10, path.get(0).totalDistance());
      assertEquals(20, path.get(1).totalDistance());
      assertEquals(20, path.get(2).totalDistance());
      assertEquals(30, path.get(3).totalDistance());
      assertEquals(30, path.get(4).totalDistance());

      // backtraces set
      assertNull(path.get(0).getBackTrace());
      assertEquals(path.get(0), path.get(1).getBackTrace());
      assertEquals(path.get(1), path.get(2).getBackTrace());
      assertEquals(path.get(2), path.get(3).getBackTrace()); 
      assertEquals(path.get(3), path.get(4).getBackTrace());

      // edit distance
      assertEquals(30, mp.minimumEditDistance("plomo", "oro"));
   }

   /**
    * These tests use a custom comparator which assigns distances depending on the
    * characters involved:
    * <ul>
    *  <li>If it's the same character but different case, distance is 1</li>
    *  <li>If it's a space character to another space character, distance is 4</li>
    *  <li>If it's a digit to another digit, distance is 4</li>
    *  <li>If it's a vowel to another vowel, distance is 4</li>
    *  <li>If it's a plosive to another plosive, distance is 4</li>
    *  <li>If it's a fricative to another fricative, distance is 4</li>
    *  <li>If it's a stop to another stop, distance is 4</li>
    *  <li>If it's a letter to another leffer, distance is 8</li>
    *  <li>Otherwise it's 20</li>
    * </ul>
    * Also, deletes and inserts are both 10.
    */
   @Test 
   public void customComparator()
   {
      MinimumEditPathString mp = new MinimumEditPathString(
	 new IEditComparator<Character>()
	 {
	    public EditStep<Character> compare(Character from, Character to)
	    {
	       EditStep<Character> step = new EditStep<Character>(from, to, 0, EditStep.StepOperation.NONE);
	       if (from == null)
	       {
		  if (to != null)
		  {
		     step.setStepDistance(20);
		     step.setOperation(EditStep.StepOperation.CHANGE);
		  }
		  // if both are null, we fall through to the return, which amounts to no change
	       }
	       else if (to == null)
	       {
		  step.setStepDistance(20);
		  step.setOperation(EditStep.StepOperation.CHANGE);
	       }
	       else if (!from.equals(to))
	       {
		  step.setOperation(EditStep.StepOperation.CHANGE);
		  int iDistance = 20;
		  char cFrom = from.charValue();
		  char cTo = to.charValue();
		  if (
		     (Character.isDigit(cFrom) && Character.isDigit(cTo))
		     || (Character.isSpaceChar(cFrom) && Character.isSpaceChar(cTo))
		     )
		  {
		     iDistance = 4;
		  }
		  else if (Character.isLetter(cFrom) && Character.isLetter(cTo))
		  {
		     iDistance = 8; // default between letters
		     char cFromLower = Character.toLowerCase(cFrom);
		     char cToLower = Character.toLowerCase(cTo);
		     if (cFromLower == cToLower)
		     { // same letter different case
			iDistance = 1;
		     }
		     else if (
			("aeiou".indexOf(cFromLower) >= 0 && "aeiou".indexOf(cToLower) >= 0)
			|| ("pbtdkg".indexOf(cFromLower) >= 0 && "pbtdkg".indexOf(cToLower) >= 0)
			|| ("fvszh".indexOf(cFromLower) >= 0 && "fvszh".indexOf(cToLower) >= 0)
			|| ("lr".indexOf(cFromLower) >= 0 && "lr".indexOf(cToLower) >= 0)
			|| ("mn".indexOf(cFromLower) >= 0 && "mn".indexOf(cToLower) >= 0)
			)
		     {
			iDistance = 4;
		     }
		  }
		  step.setStepDistance(iDistance);
	       }
	       return step;
	    }

	    public EditStep<Character> delete(Character from) { return new EditStep<Character>(from, null, 10, EditStep.StepOperation.DELETE); }
	    public EditStep<Character> insert(Character to) { return new EditStep<Character>(null, to, 10, EditStep.StepOperation.INSERT); }

	 });

      List<EditStep<Character>> path = mp.minimumEditPath(" plomo 1", "Principe2");
      assertNotNull(path);

      // string representation of path
      System.out.println("Character-type-sensitive edit: "+MinimumEditPathString.printPath(path));
      assertEquals("\r\n plom·o·· 1\r\n·Principe·2\r\n", MinimumEditPathString.printPath(path));

      // right number of steps
      assertEquals(11, path.size());

      // changes
      assertEquals(EditStep.StepOperation.DELETE, path.get(0).getOperation()); // space
      assertEquals(EditStep.StepOperation.CHANGE, path.get(1).getOperation()); // p/P
      assertEquals(EditStep.StepOperation.CHANGE, path.get(2).getOperation()); // l/r
      assertEquals(EditStep.StepOperation.CHANGE, path.get(3).getOperation()); // o/i
      assertEquals(EditStep.StepOperation.CHANGE, path.get(4).getOperation()); // m/n
      assertEquals(EditStep.StepOperation.INSERT, path.get(5).getOperation()); //  /c
      assertEquals(EditStep.StepOperation.CHANGE, path.get(6).getOperation()); // o/i
      assertEquals(EditStep.StepOperation.INSERT, path.get(7).getOperation()); //  /p
      assertEquals(EditStep.StepOperation.INSERT, path.get(8).getOperation()); //  /e
      assertEquals(EditStep.StepOperation.DELETE, path.get(9).getOperation()); // space
      assertEquals(EditStep.StepOperation.CHANGE, path.get(10).getOperation()); // 1/2

      // 'from' set
      assertEquals(new Character(' '), path.get(0).getFrom());
      assertEquals(new Character('p'), path.get(1).getFrom());
      assertEquals(new Character('l'), path.get(2).getFrom());
      assertEquals(new Character('o'), path.get(3).getFrom());
      assertEquals(new Character('m'), path.get(4).getFrom());
      assertNull(path.get(5).getFrom());
      assertEquals(new Character('o'), path.get(6).getFrom());
      assertNull(path.get(7).getFrom());
      assertNull(path.get(8).getFrom());
      assertEquals(new Character(' '), path.get(9).getFrom());
      assertEquals(new Character('1'), path.get(10).getFrom());

      // 'to' set
      assertNull(path.get(0).getTo());
      assertEquals(new Character('P'), path.get(1).getTo());
      assertEquals(new Character('r'), path.get(2).getTo());
      assertEquals(new Character('i'), path.get(3).getTo());
      assertEquals(new Character('n'), path.get(4).getTo());
      assertEquals(new Character('c'), path.get(5).getTo());
      assertEquals(new Character('i'), path.get(6).getTo());
      assertEquals(new Character('p'), path.get(7).getTo());
      assertEquals(new Character('e'), path.get(8).getTo());
      assertNull(path.get(9).getTo());
      assertEquals(new Character('2'), path.get(10).getTo());

      // distances set
      assertEquals(10, path.get(0).getStepDistance()); // space
      assertEquals(1, path.get(1).getStepDistance());  // p/P
      assertEquals(4, path.get(2).getStepDistance());  // l/r
      assertEquals(4, path.get(3).getStepDistance());  // o/i
      assertEquals(4, path.get(4).getStepDistance());  // m/n
      assertEquals(10, path.get(5).getStepDistance()); //  /c
      assertEquals(4, path.get(6).getStepDistance());  // o/i
      assertEquals(10, path.get(7).getStepDistance()); //  /p
      assertEquals(10, path.get(8).getStepDistance()); //  /e
      assertEquals(10, path.get(9).getStepDistance()); // space
      assertEquals(4, path.get(10).getStepDistance()); // 1/2

      // total distances set
      assertEquals(10, path.get(0).totalDistance());
      assertEquals(11, path.get(1).totalDistance());
      assertEquals(15, path.get(2).totalDistance());
      assertEquals(19, path.get(3).totalDistance());
      assertEquals(23, path.get(4).totalDistance());
      assertEquals(33, path.get(5).totalDistance());
      assertEquals(37, path.get(6).totalDistance());
      assertEquals(47, path.get(7).totalDistance());
      assertEquals(57, path.get(8).totalDistance());
      assertEquals(67, path.get(9).totalDistance());
      assertEquals(71, path.get(10).totalDistance());

      // backtraces set
      assertNull(path.get(0).getBackTrace());
      assertEquals(path.get(0), path.get(1).getBackTrace());
      assertEquals(path.get(1), path.get(2).getBackTrace());
      assertEquals(path.get(2), path.get(3).getBackTrace()); 
      assertEquals(path.get(3), path.get(4).getBackTrace());
      assertEquals(path.get(4), path.get(5).getBackTrace());
      assertEquals(path.get(5), path.get(6).getBackTrace());
      assertEquals(path.get(6), path.get(7).getBackTrace());
      assertEquals(path.get(7), path.get(8).getBackTrace());
      assertEquals(path.get(8), path.get(9).getBackTrace());
      assertEquals(path.get(9), path.get(10).getBackTrace());

      // now try the same string with the default comparator
      mp.setComparator(new DefaultEditComparator<Character>(10));

      path = mp.minimumEditPath(" plomo 1", "Principe2");
      assertNotNull(path);

      // string representation of path
      System.out.println("Default edit: "+MinimumEditPathString.printPath(path));
      assertEquals("\r\n plomo 1·\r\nPrincipe2\r\n", MinimumEditPathString.printPath(path));

      // right number of steps
      assertEquals(9, path.size());

      // changes
      assertEquals(EditStep.StepOperation.CHANGE, path.get(0).getOperation()); //  /P
      assertEquals(EditStep.StepOperation.CHANGE, path.get(1).getOperation()); // p/r
      assertEquals(EditStep.StepOperation.CHANGE, path.get(2).getOperation()); // l/i
      assertEquals(EditStep.StepOperation.CHANGE, path.get(3).getOperation()); // o/n
      assertEquals(EditStep.StepOperation.CHANGE, path.get(4).getOperation()); // m/c
      assertEquals(EditStep.StepOperation.CHANGE, path.get(5).getOperation()); // o/i
      assertEquals(EditStep.StepOperation.CHANGE, path.get(6).getOperation()); //  /p
      assertEquals(EditStep.StepOperation.CHANGE, path.get(7).getOperation()); // 1/e
      assertEquals(EditStep.StepOperation.INSERT, path.get(8).getOperation()); //  /2

      // 'from' set
      assertEquals(new Character(' '), path.get(0).getFrom());
      assertEquals(new Character('p'), path.get(1).getFrom());
      assertEquals(new Character('l'), path.get(2).getFrom());
      assertEquals(new Character('o'), path.get(3).getFrom());
      assertEquals(new Character('m'), path.get(4).getFrom());
      assertEquals(new Character('o'), path.get(5).getFrom());
      assertEquals(new Character(' '), path.get(6).getFrom());
      assertEquals(new Character('1'), path.get(7).getFrom());
      assertNull(path.get(8).getFrom());

      // 'to' set
      assertEquals(new Character('P'), path.get(0).getTo());
      assertEquals(new Character('r'), path.get(1).getTo());
      assertEquals(new Character('i'), path.get(2).getTo());
      assertEquals(new Character('n'), path.get(3).getTo());
      assertEquals(new Character('c'), path.get(4).getTo());
      assertEquals(new Character('i'), path.get(5).getTo());
      assertEquals(new Character('p'), path.get(6).getTo());
      assertEquals(new Character('e'), path.get(7).getTo());
      assertEquals(new Character('2'), path.get(8).getTo());
      
      // distances
      assertEquals(10, path.get(0).getStepDistance()); //  /P
      assertEquals(10, path.get(1).getStepDistance()); // p/r
      assertEquals(10, path.get(2).getStepDistance()); // l/i
      assertEquals(10, path.get(3).getStepDistance()); // o/n
      assertEquals(10, path.get(4).getStepDistance()); // m/c
      assertEquals(10, path.get(5).getStepDistance()); // o/i
      assertEquals(10, path.get(6).getStepDistance()); //  /p
      assertEquals(10, path.get(7).getStepDistance()); // 1/e
      assertEquals(10, path.get(8).getStepDistance()); //  /2

      // total distance
      assertEquals(90, path.get(8).totalDistance());

   }
   
   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nz.ac.canterbury.ling.util.test.TestMinimumEditPath");
   }
}
