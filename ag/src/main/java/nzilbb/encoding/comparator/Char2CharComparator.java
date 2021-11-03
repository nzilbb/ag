//
// Copyright 2018-2021 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.encoding.comparator;
	      
import nzilbb.editpath.EditComparator;
import nzilbb.editpath.EditStep;

/**
 * Comparator that maps one plain-text series of characters to another.
 */
public class Char2CharComparator<E> implements EditComparator<E> {
  /**
   * Compares two sequence elements, and evaluates the distance between them.
   * @param from The element from the source sequence, which may be null,
   * @param to The element from the destination sequence, which may be null.
   * @return An edit step between the two elements. {@link EditStep#getFrom()} is set to
   * <var>from</var>, {@link EditStep#getTo()} is set to <var>to</var>, 
   * {@link EditStep#getStepDistance()} is set to the computed edit distance between these two
   * elements, and {@link EditStep#getOperation()} is set to either
   * <var>EditStep.StepOperation.NONE</var> or <var>EditStep.StepOperation.CHANGE</var>.
   */
  public EditStep<E> compare(E from, E to) {
    EditStep<E> step = new EditStep<E>(from, to, 0, EditStep.StepOperation.NONE);
    if (from == null) {
      if (to != null) {
        step.setStepDistance(20);
        step.setOperation(EditStep.StepOperation.CHANGE);
      }
      // if both are null, we fall through to the return, which amounts to no change
    } else if (to == null) {
      step.setStepDistance(20);
      step.setOperation(EditStep.StepOperation.CHANGE);
    } else if (!from.equals(to)) {
      step.setOperation(EditStep.StepOperation.CHANGE);
      int iDistance = 20;
      char cFrom = from.toString().charAt(0);
      char cTo = to.toString().charAt(0);
      if (
        (Character.isDigit(cFrom) && Character.isDigit(cTo))
        || (Character.isSpaceChar(cFrom) && Character.isSpaceChar(cTo))
        ) {
	    iDistance = 4;
      } else if (Character.isLetter(cFrom) && Character.isLetter(cTo)) {
        iDistance = 8; // default between letters
        char cFromLower = Character.toLowerCase(cFrom);
        char cToLower = Character.toLowerCase(cTo);
        if (cFromLower == cToLower) { // same letter different case
          iDistance = 1;
        } else if (
          ("aeiou".indexOf(cFromLower) >= 0 && "aeiou".indexOf(cToLower) >= 0)
          || ("pbtdkg".indexOf(cFromLower) >= 0 && "pbtdkg".indexOf(cToLower) >= 0)
          || ("fvszh".indexOf(cFromLower) >= 0 && "fvszh".indexOf(cToLower) >= 0)
          || ("lr".indexOf(cFromLower) >= 0 && "lr".indexOf(cToLower) >= 0)
          || ("mn".indexOf(cFromLower) >= 0 && "mn".indexOf(cToLower) >= 0)
          ) {
          iDistance = 4;
        }
      }
      step.setStepDistance(iDistance);
    }
    return step;
  }
  
  /**
   * The distance for deleting the given element.
   * @param from The element that would be deleted, which may be null.
   * @return An edit step with {@link EditStep#getStepDistance()} set to the distance for
   * deleting the given element. {@link EditStep#getFrom()} is set to <var>from</var>, and
   * {@link EditStep#getOperation()} is set to <var>EditStep.StepOperation.DELETE</var> 
   */
  public EditStep<E> delete(E from) {
      return new EditStep<E>(from, null, 10, EditStep.StepOperation.DELETE);
  }
  
  /**
   * The distance for inserting the given element.
   * @param to The element that would be inserted, which may be null.
   * @return An edit step with {@link EditStep#getStepDistance()} set to the distance for
   * inserting the given element. {@link EditStep#getTo()} is set to <var>to</var>, and
   * {@link EditStep#getOperation()} is set to <var>EditStep.StepOperation.INSERT</var>
   */
  public EditStep<E> insert(E to) {
    return new EditStep<E>(null, to, 10, EditStep.StepOperation.INSERT);
  }

} // Char2CharComparator
