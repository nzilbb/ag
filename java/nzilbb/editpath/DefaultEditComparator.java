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

import java.util.Comparator;

/**
 * Default implementation of IEditComparator, for which any from/to pair for which equals() is not true is given an edit distance of {@link #getChangeDistance()}.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class DefaultEditComparator<T>
   implements IEditComparator<T>
{
   // Attributes:
   
   /**
    * The distance represented by a change. The default value is 1.
    * @see #getChangeDistance()
    * @see #setChangeDistance(int)
    */
   protected int iChangeDistance = 1; // TODO make distances Double
   /**
    * Getter for {@link #iChangeDistance}: The distance represented by a change.
    * @return The distance represented by a change.
    */
   public int getChangeDistance() { return iChangeDistance; }
   /**
    * Setter for {@link #iChangeDistance}: The distance represented by a change.
    * @param iNewChangeDistance The distance represented by a change.
    */
   public void setChangeDistance(int iNewChangeDistance) { iChangeDistance = iNewChangeDistance; }

   
   /**
    * The distance represented by a deletion. The default is 1.
    * @see #getDeleteDistance()
    * @see #setDeleteDistance(int)
    */
   protected int iDeleteDistance = 1;
   /**
    * Getter for {@link #iDeleteDistance}: The distance represented by a deletion.
    * @return The distance represented by a deletion.
    */
   public int getDeleteDistance() { return iDeleteDistance; }
   /**
    * Setter for {@link #iDeleteDistance}: The distance represented by a deletion.
    * @param iNewDeleteDistance The distance represented by a deletion.
    */
   public void setDeleteDistance(int iNewDeleteDistance) { iDeleteDistance = iNewDeleteDistance; }


   /**
    * The distance represented by an insertion. The default is 1.
    * @see #getInsertDistance()
    * @see #setInsertDistance(int)
    */
   protected int iInsertDistance = 1;
   /**
    * Getter for {@link #iInsertDistance}: The distance represented by an insertion.
    * @return The distance represented by an insertion.
    */
   public int getInsertDistance() { return iInsertDistance; }
   /**
    * Setter for {@link #iInsertDistance}: The distance represented by an insertion.
    * @param iNewInsertDistance The distance represented by an insertion.
    */
   public void setInsertDistance(int iNewInsertDistance) { iInsertDistance = iNewInsertDistance; }


   private final Comparator<T> objectEqualsComparator = new EqualsComparator<T>();
   /**
    * A comparator to use to determin equality, or null to use java.util.Object.equals(Object).
    * @see #getComparator()
    * @see #setComparator(Comparator)
    */
   protected Comparator<T> comparator = objectEqualsComparator;
   /**
    * Getter for {@link #comparator}: A comparator to use to determin equality, or null to use java.util.Object.equals(Object).
    * @return A comparator to use to determin equality, or null to use java.util.Object.equals(Object).
    */
   public Comparator<T> getComparator() { return comparator; }
   /**
    * Setter for {@link #comparator}: A comparator to use to determin equality, or null to use java.util.Object.equals(Object).
    * @param newComparator A comparator to use to determin equality, or null to use java.util.Object.equals(Object).
    */
   public void setComparator(Comparator<T> newComparator) { comparator = newComparator; if (comparator == null) comparator = objectEqualsComparator; }

   
   // Methods:
   
   /**
    * Default constructor
    */
   public DefaultEditComparator()
   {
   } // end of constructor

   /**
    * Constructor with custom comparator.
    * @param comparator The custom comparator.
    */
   public DefaultEditComparator(Comparator<T> comparator)
   {
      setComparator(comparator);
   } // end of constructor

   /**
    * Constructor
    * @param insertDeleteChangeDistance The distance applied to an insert, delete, or change.
    */
   public DefaultEditComparator(int insertDeleteChangeDistance)
   {
      setChangeDistance(insertDeleteChangeDistance);
      setInsertDistance(insertDeleteChangeDistance);
      setDeleteDistance(insertDeleteChangeDistance);
   } // end of constructor

   /**
    * Constructor
    * @param insertDistance The distance applied to an insertion.
    * @param deleteDistance The distance applied to a deletion.
    * @param changeDistance The distance applied to a change.
    */
   public DefaultEditComparator(int insertDistance, int deleteDistance, int changeDistance)
   {
      setChangeDistance(changeDistance);
      setInsertDistance(insertDistance);
      setDeleteDistance(deleteDistance);
   } // end of constructor

   /**
    * {@link IEditComparator} method: Compares two sequence elements, and evaluates the distance between them.
    * @param from The element from the source sequence, which may be null.
    * @param to The element from the destination sequence, which may be null.
    * @return An edit step between the two elements. {@link EditStep#getFrom()} is set to <var>from</var>, {@link EditStep#getTo()} is set to <var>to</var>, {@link EditStep#getStepDistance()} is set to the computed edit distance between these two elements, and {@link EditStep#getOperation()} is set to either <var>EditStep.StepOperation.NONE</var> or <var>EditStep.StepOperation.CHANGE</var>.
    */
   public EditStep<T> compare(T from, T to)
   {
      EditStep<T> step = new EditStep<T>(from, to, 0, EditStep.StepOperation.NONE);
      if (from == null)
      {
	 if (to != null)
	 {
	    step.setStepDistance(iChangeDistance);
	    step.setOperation(EditStep.StepOperation.CHANGE);
	 }
	 // if both are null, we fall through to the return, which amounts to no change
      }
      else if (to == null)
      {
	 step.setStepDistance(iChangeDistance);
	 step.setOperation(EditStep.StepOperation.CHANGE);
      }
      else if (comparator.compare(from, to) != 0)
      {
	 step.setStepDistance(iChangeDistance);
	 step.setOperation(EditStep.StepOperation.CHANGE);
      }
      return step;
   }

   /**
    * The distance for deleting the given element.
    * @param from The element that would be deleted, which may be null.
    * @return An edit step with {@link EditStep#getStepDistance()} set to the distance for deleting the given element. {@link EditStep#getFrom()} is set to <var>from</var>, and {@link EditStep#getOperation()} is set to <var>EditStep.StepOperation.DELETE</var>
    */
   public EditStep<T> delete(T from) 
   {
      return new EditStep<T>(from, null, iDeleteDistance, EditStep.StepOperation.DELETE);
   }

   /**
    * The distance for inserting the given element.
    * @param to The element that would be inserted, which may be null.
    * @return An edit step with {@link EditStep#getStepDistance()} set to the distance for inserting the given element. {@link EditStep#getTo()} is set to <var>to</var>, and {@link EditStep#getOperation()} is set to <var>EditStep.StepOperation.INSERT</var>
    */
   public EditStep<T> insert(T to)
   {
      return new EditStep<T>(null, to, iInsertDistance, EditStep.StepOperation.INSERT);
   }


} // end of class DefaultEditComparator
