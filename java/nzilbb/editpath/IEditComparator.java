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

/**
 * Interface for comparators between elements.
 * <p>This interface uses {@link EditStep} both for convenience, and also to allow the possibility
 * of an edit operation having both {@link EditStep#getDistance()} = 0 and {@link EditStep#getOperation()} is set to either <var>EditStep.StepOperation.CHANGE</var>.  This allows for non-equal elements to have zero distance, but still preserve the fact of their inequality.
 * @author Robert Fromont robert@fromont.net.nz
 */

public interface IEditComparator<T>
{
   /**
    * Compares two sequence elements, and evaluates the distance between them.
    * @param from The element from the source sequence, which may be null,
    * @param to The element from the destination sequence, which may be null.
    * @return An edit step between the two elements. {@link EditStep#getFrom()} is set to <var>from</var>, {@link EditStep#getTo()} is set to <var>to</var>, {@link EditStep#getDistance()} is set to the computed edit distance between these two elements, and {@link EditStep#getOperation()} is set to either <var>EditStep.StepOperation.NONE</var> or <var>EditStep.StepOperation.CHANGE</var>.
    */
   public EditStep<T> compare(T from, T to);
   
   
   /**
    * The distance for deleting the given element.
    * @param from The element that would be deleted, which may be null.
    * @return An edit step with {@link EditStep#getDistance()} set to the distance for deleting the given element. {@link EditStep#getFrom()} is set to <var>from</var>, and {@link EditStep#getOperation()} is set to <var>EditStep.StepOperation.DELETE</var>
    */
   public EditStep<T> delete(T from);

   /**
    * The distance for inserting the given element.
    * @param from The element that would be inserted, which may be null.
    * @return An edit step with {@link EditStep#getDistance()} set to the distance for inserting the given element. {@link EditStep#getTo()} is set to <var>to</var>, and {@link EditStep#getOperation()} is set to <var>EditStep.StepOperation.INSERT</var>
    */
   public EditStep<T> insert(T to);

} // end of class IEditComparator
