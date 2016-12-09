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

import java.util.List;
import java.util.Iterator;
import java.util.Vector;

/**
 * Implementation of the
 * <a href="https://en.wikipedia.org/wiki/Wagner%E2%80%93Fischer_algorithm">Wagner-Fischer algorithm</a> 
 * to determine the minimum edit path (or distance) between two sequences. While traditionally this is between two strings (sequences of characters), the implementation uses templates to support sequences of any type, and interfaces for comparators, to support different settings and methods for determining edit distance.
 * <p>A traditional Levenstein distance calculation can be achieved with MinimumEditPath&lt;Character&gt;.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class MinimumEditPath<T>
{
   // Attributes:   
   
   /**
    * The comparator used when determining the distance between two sequence elements.
    * @see #getComparator()
    * @see #setComparator(IEditComparator)
    */
   protected IEditComparator<T> comparator;
   /**
    * Getter for {@link #comparator}: The comparator used when determining the distance between two sequence elements.
    * @return The comparator used when determining the distance between two sequence elements.
    */
   public IEditComparator<T> getComparator() { return comparator; }
   /**
    * Setter for {@link #comparator}: The comparator used when determining the distance between two sequence elements.
    * @param Newcomparator The comparator used when determining the distance between two sequence elements.
    */
   public void setComparator(IEditComparator<T> Newcomparator) { comparator = Newcomparator; }
   
   
   // Methods:
   
   /**
    * Default constructor
    */
   public MinimumEditPath()
   {
      setComparator(new DefaultEditComparator<T>());
   } // end of constructor

   /**
    * Constructor
    * @param comparator Element comparator to use.
    */
   public MinimumEditPath(IEditComparator<T> comparator)
   {
      setComparator(comparator);
   } // end of constructor

   
   /**
    * Computes the minimum path from one sequence to another.
    * @param from The source (original) sequence.
    * @param to The destination (final) sequence.
    * @return The edit path between the two sequences that has the minimum edit distance.
    */
   public List<EditStep<T>> minimumEditPath(List<T> from, List<T> to)
   {
      // treat null sequences as empty sequences
      if (from == null) from = new Vector<T>();
      if (to == null) to = new Vector<T>();

      // we only need 2 lines - the previous one [0] and this one [1]
      // This optimization doesn't quite reduce the space requirement from O(mn) to O(n)
      // because the backtraces keep the referenced steps from being garbage-collected.
      // However, *some* of the steps can be garbage-collected - i.e. those that are 
      // inaccessible to any backtrace in path[2][] - and any saving is a good saving.
      @SuppressWarnings(value={"unchecked","rawtypes"})
      EditStep<T>[][] path = new EditStep[2][to.size() + 1];
	    
      // setup array borders with distance to empty list...
      path[0][0] = null; // the starting point

      // all inserts on first line
      EditStep<T> prev = path[0][0];
      int j = 0;
      for (T t : to)
      {
	 path[0][j+1] = comparator.insert(t).setBackTrace(prev); // all inserts
	 // prepare for next iteration:
	 prev = path[0][j+1];
	 j++;
      }

      for (T f : from)
      {
	 // all deletes column entry
	 path[1][0] = comparator.delete(f).setBackTrace(path[0][0]);

	 j = 1;
	 T lastT = null;
	 for (T t : to)
	 {
	    path[1][j] = minimumEdit(path[0][j], path[1][j - 1], path[0][j - 1], f, t);
	    
	    j++;
	 } // next j

	 // move the current row (1) into the previous row (0) position
	 for (j = 0; j <= to.size(); j++)
	 {
	    path[0][j] = path[1][j];
	    path[1][j] = null; // just in case
	 }	    
      } // next i
	    
      // traverse minimum path
      Vector<EditStep<T>> vPath = new Vector<EditStep<T>>();
      // starts at the end of the 0th row
      EditStep<T> e = path[0][to.size()];
      int f = from.size() - 1;
      int t = to.size() - 1;
      if (t < 0) t = 0;
      if (f < 0) f = 0;
      if (e != null)
      {
	 do
	 {
	    vPath.add(0,e);
	    // set from/to indices
	    switch(e.getOperation())
	    {
	       case DELETE:		  
		  e.setFromToIndices(f--, t);
		  break;
	       case INSERT:
		  e.setFromToIndices(f, t--);
		  break;
	       default:
		  e.setFromToIndices(f--, t--);
		  break;
	    }
	    if (t < 0) t = 0;
	    if (f < 0) f = 0;
	    e = e.getBackTrace();
	 }
	 while (e != null);
      }
      
      return vPath;
   } // end of computePath()


   /**
    * Returns the minimum edit, given the three neighboring cells. 
    * <p>This implementation, when faced with a change with the same distance as a delete or insert,
    * will favor the delete or insert over change. 
    * This is so you can favor delete/insert if desired, by simply upping the change distance to 2, i.e. with:
    * <br><code>new MinimumEditPath&lt;T&gt;(new DefaultEditComparator&lt;T&gt;(2));</code>
    * <p>It will also insert before deleting, given the same cost.
    * @param left The step to the 'left' in the edit matrix.
    * @param diagonal The step to the 'left' and 'below' in the edit matrix.
    * @param below The step 'below' in the edit matrix.
    * @param from Object in the source (original) sequence.
    * @param to Object in the destination (final) sequence.
    * @return The minimum edit
    */
   protected EditStep<T> minimumEdit(EditStep<T> left, EditStep<T> below, EditStep<T> diagonal, T from, T to)
   {
      EditStep<T> delete = comparator.delete(from);
      delete.setBackTrace(left);
      EditStep<T> insert = comparator.insert(to);
      insert.setBackTrace(below);
      EditStep<T> edit = comparator.compare(from, to);
      edit.setBackTrace(diagonal);
      EditStep<T> winner = edit;
      if (winner.totalDistance() >= insert.totalDistance()) winner = insert;
      if (winner.totalDistance() >= delete.totalDistance()) winner = delete;
      return winner;
   } // end of minimumEdit()

   
   /**
    * Computes the minimum edit distance between two sequences.
    * @param from The source (original) sequence.
    * @param to The destination (final) sequence.
    * @return The minimum edit distance between the two sequences.
    */
   public int minimumEditDistance(List<T> from, List<T> to)
   {
      List<EditStep<T>> path = minimumEditPath(from, to);
      return path.get(path.size() - 1).totalDistance();
   } // end of minimumDistance()

   
   /**
    * Collapses edit path so that subsequent delete/create steps are collapsed into a single change step.
    * @param path The path to collapse
    * @return The given path, with possibly fewer steps.
    */
   public List<EditStep<T>> collapse(List<EditStep<T>> path)
   {
      Iterator<EditStep<T>> steps = path.iterator();
      EditStep<T> lastStep = null;
      while (steps.hasNext())
      {
	 EditStep<T> thisStep = steps.next();
	 if (lastStep != null)
	 {
	    if (lastStep.getOperation() == EditStep.StepOperation.DELETE
		&& thisStep.getOperation() == EditStep.StepOperation.INSERT)
	    {
	       EditStep<T> change = comparator.compare(lastStep.getFrom(), thisStep.getTo());
	       if (change.getStepDistance() <= 3 * (lastStep.getStepDistance() + thisStep.getStepDistance()))
	       {
		  lastStep.setOperation(EditStep.StepOperation.CHANGE);
		  lastStep.setTo(thisStep.getTo());
		  lastStep.setToIndex(thisStep.getToIndex());
		  lastStep.setStepDistance(lastStep.getStepDistance() + thisStep.getStepDistance());
		  steps.remove();
		  thisStep = lastStep;
	       }
	       else
	       {
		  System.out.println("change " + change.getStepDistance() + " delete/insert " + (lastStep.getStepDistance() + thisStep.getStepDistance()));
	       }
	    }
	    else if (lastStep.getOperation() == EditStep.StepOperation.INSERT
		&& thisStep.getOperation() == EditStep.StepOperation.DELETE)
	    {
	       EditStep<T> change = comparator.compare(thisStep.getFrom(), lastStep.getTo());
	       if (change.getStepDistance() <= 3 * (lastStep.getStepDistance() + thisStep.getStepDistance()))
	       {
		  lastStep.setOperation(EditStep.StepOperation.CHANGE);
		  lastStep.setFrom(thisStep.getFrom());
		  lastStep.setFromIndex(thisStep.getFromIndex());
		  lastStep.setStepDistance(lastStep.getStepDistance() + thisStep.getStepDistance());
		  steps.remove();
		  thisStep = lastStep;
	       }
	       else
	       {
		  System.out.println("change " + change.getStepDistance() + " insert/delete " + (lastStep.getStepDistance() + thisStep.getStepDistance()));
	       }
	    }
	 }
	 lastStep = thisStep;
      }
      return path;
   } // end of Collapse()

   
} // end of class MinimumEditPath
