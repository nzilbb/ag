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

/**
 * Represents a single step in editing one sequency into another.
 * @see MinimumEditPath
 * @author Robert Fromont robert@fromont.net.nz
 */

public class EditStep<T> {

   /**
    * Enumeration for representing the operation represented by a step 
    * @see #getOperation()
    */
   public enum StepOperation { NONE, DELETE, CHANGE, INSERT }; // TODO Add the possiblity of merge? i.e. two subsequent symbols converting to one symbol

   // Attributes:
   
   /**
    * The operation represented by this step.
    * @see #getOperation()
    * @see #setOperation(StepOperation)
    */
   protected StepOperation enOperation = StepOperation.NONE;
   /**
    * Getter for {@link #enOperation}: The operation represented by this step.
    * @return The operation represented by this step.
    */
   public StepOperation getOperation() { return enOperation; }
   /**
    * Setter for {@link #enOperation}: The operation represented by this step.
    * @param enNewOperation The operation represented by this step.
    * @return this.
    */
   public EditStep<T> setOperation(StepOperation enNewOperation) { enOperation = enNewOperation; return this; }

   /**
    * The element in the start sequence.
    * @see #getFrom()
    * @see #setFrom(Object)
    */
   protected T oFrom;
   /**
    * Getter for {@link #oFrom}: The element in the start sequence.
    * @return The element in the start sequence.
    */
   public T getFrom() { return oFrom; }
   /**
    * Setter for {@link #oFrom}: The element in the start sequence.
    * @param oNewFrom The element in the start sequence.
    * @return this.
    */
   public EditStep<T> setFrom(T oNewFrom) { oFrom = oNewFrom; return this; }

   /**
    * The index of "from".
    * @see #getFromIndex()
    * @see #setFromIndex(int)
    */
   protected int fromIndex = -1;
   /**
    * Getter for {@link #fromIndex}: The index of "from".
    * @return The index of "from".
    */
   public int getFromIndex() { return fromIndex; }
   /**
    * Setter for {@link #fromIndex}: The index of "from".
    * @param newFromIndex The index of "from".
    */
   public void setFromIndex(int newFromIndex) { fromIndex = newFromIndex; }
   
   /**
    * The element in the end sequence.
    * @see #getTo()
    * @see #setTo(Object)
    */
   protected T oTo;
   /**
    * Getter for {@link #oTo}: The element in the end sequence.
    * @return The element in the end sequence.
    */
   public T getTo() { return oTo; }
   /**
    * Setter for {@link #oTo}: The element in the end sequence.
    * @param oNewTo The element in the end sequence.
    * @return this.
    */
   public EditStep<T> setTo(T oNewTo) { oTo = oNewTo; return this; }

   /**
    * The index of "to".
    * @see #getToIndex()
    * @see #setToIndex(int)
    */
   protected int toIndex = -1;
   /**
    * Getter for {@link #toIndex}: The index of "to".
    * @return The index of "to".
    */
   public int getToIndex() { return toIndex; }
   /**
    * Setter for {@link #toIndex}: The index of "to".
    * @param newToIndex The index of "to".
    */
   public void setToIndex(int newToIndex) { toIndex = newToIndex; }   
   
   /**
    * Backtrace to the previous (minimum) edit in the sequence.
    * @see #getBackTrace()
    * @see #setBackTrace(EditStep)
    */
   protected EditStep<T> eBackTrace;
   /**
    * Getter for {@link #eBackTrace}: Backtrace to the previous (minimum) edit in the sequence.
    * @return Backtrace to the previous (minimum) edit in the sequence.
    */
   public EditStep<T> getBackTrace() { return eBackTrace; }
   /**
    * Setter for {@link #eBackTrace}: Backtrace to the previous (minimum) edit in the sequence.
    * @param eNewBackTrace Backtrace to the previous (minimum) edit in the sequence.
    * @return this.
    */
   public EditStep<T> setBackTrace(EditStep<T> eNewBackTrace) { 
      eBackTrace = eNewBackTrace; 
      if (eBackTrace != null) {
	 iBackTraceTotalDistance = eBackTrace.totalDistance();
      } else {
	 iBackTraceTotalDistance = 0;
      }
      return this;
   }

   /**
    * The total distance to the previous step. This is cached to avoid repeatedly following the
    * edit path back every time {@link #totalDistance()} is invoked.
    */
   protected int iBackTraceTotalDistance = 0;
   
   /**
    * The distance represented by this single step.
    * @see #getStepDistance()
    * @see #setStepDistance(int)
    */
   protected int iStepDistance; // TODO make distances Double
   /**
    * Getter for {@link #iStepDistance}: The distance represented by this single step.
    * @return The distance represented by this single step.
    */
   public int getStepDistance() { return iStepDistance; }
   /**
    * Setter for {@link #iStepDistance}: The distance represented by this single step.
    * @param iNewStepDistance The distance represented by this single step.
    * @return this.
    */
   public EditStep<T> setStepDistance(int iNewStepDistance) { iStepDistance = iNewStepDistance; return this; }
   
   // Methods:
   
   /**
    * Default constructor
    */
   public EditStep()
   {
   } // end of constructor

   /**
    * Constructor
    * @param from Object in the source (original) list, or null.
    * @param to Object in the destination (final) list, or null.
    * @param stepDistance The distance represented by this single step.
    * @param operation The operation represented by this step.
    */
   public EditStep(T from, T to, int stepDistance, StepOperation operation) {
      setFrom(from);
      setTo(to);
      setOperation(operation);
      setStepDistance(stepDistance);
   } // end of constructor

   /**
    * Constructor
    * @param from Object in the source (original) list, or null.
    * @param to Object in the destination (final) list, or null.
    * @param stepDistance The distance represented by this single step.
    * @param operation The operation represented by this step.
    * @param backtrace The previous (minimum) edit in the sequence.
    */
   public EditStep(T from, T to, int stepDistance, StepOperation operation, EditStep<T> backtrace) {
      setFrom(from);
      setTo(to);
      setOperation(operation);
      setStepDistance(stepDistance);
      setBackTrace(backtrace);
   } // end of constructor
   
   /**
    * The total distance up to and including this edit.
    * @return The total distance up to and including this edit.
    */
   public int totalDistance() {
      return iBackTraceTotalDistance + iStepDistance;
   } // end of totalDistance()

   
   /**
    * Sets both fromIndex and toIndex.
    * @param fromIndex Value for {@link #fromIndex}
    * @param toIndex Value for {@link #toIndex}
    */
   public void setFromToIndices(int fromIndex, int toIndex) {
      setFromIndex(fromIndex);
      setToIndex(toIndex);
   } // end of setFromToIndices()
   
   /**
    * Representation of the step as a string.
    * @return Representation of the step as a string.
    */
   public String toString() {
      StringBuilder s = new StringBuilder();
      s.append(enOperation);
      s.append("\t");
      if (oFrom != null) {
	 s.append(oFrom.toString());
      } else {
	 if (enOperation == EditStep.StepOperation.INSERT) {
	    s.append("·");
	 } else {
	    s.append("<null>");
	 }
      }
      s.append("\t→\t");
      if (oTo != null) {
	 s.append(oTo.toString());
      } else {
	 if (enOperation == EditStep.StepOperation.DELETE)
	 {
	    s.append("·");
	 } else {
	    s.append("<null>");
	 }
      }
      return s.toString();
   } // end of toString

} // end of class EditStep
