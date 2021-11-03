//
// Copyright 2018 New Zealand Institute of Language, Brain and Behaviour, 
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
 * Comparator that maps an (English) orthography series to a DISC-encoded series.
 */
public class Orthography2ARPAbetComparator<E> implements EditComparator<E> {
  // phone classes
  String sameLetters = "bdfgklmnprstvwyz";
  
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
    if (from == null || from.toString().length() == 0) {
      if (to != null && to.toString().length() > 0) {
        step.setStepDistance(20);
        step.setOperation(EditStep.StepOperation.CHANGE);
      }
      // if both are null, we fall through to the return, which amounts to no change
    } else if (to == null || to.toString().length() == 0) {
      step.setStepDistance(20);
      step.setOperation(EditStep.StepOperation.CHANGE);
    } else {
      // One (lowercase) letter at a time
      char cFrom = from.toString().toLowerCase().charAt(0);
      // This is ARPABET, so the label could be 1, 2, or 3 characters
      String sTo = to.toString().toUpperCase();
      if (!(sTo.length() == 1
            && sameLetters.indexOf(cFrom) >= 0
            && cFrom == sTo.toLowerCase().charAt(0))) {
        // no direct match between orthography and phonology
        step.setOperation(EditStep.StepOperation.CHANGE);
        int iDistance = 10;
        if ( // definite correspondences
          (cFrom == 'h' && sTo.equals("HH"))) {
          iDistance = 0;
        } else if ( // probable consonant correspondences
          (cFrom == 's' && sTo.equals("SH"))
          || (cFrom == 's' && sTo.equals("ZH"))
          || (cFrom == 's' && sTo.equals("Z"))
          || (cFrom == 't' && sTo.equals("TH"))
          || (cFrom == 't' && sTo.equals("DH"))
          || (cFrom == 'j' && sTo.equals("JH"))
          || (cFrom == 'g' && sTo.equals("JH"))
          || (cFrom == 'n' && sTo.equals("NG"))
          || (cFrom == 't' && sTo.equals("DX"))
          || (cFrom == 'n' && sTo.equals("NX"))
          || (cFrom == 't' && sTo.equals("TQ"))
          ) {
          iDistance = 2;
        } else if ( // probable vowel correspondences
          (cFrom == 'a' && (sTo.startsWith("AA")
                            || sTo.startsWith("AE")
                            || sTo.startsWith("AH")
                            || sTo.startsWith("AO")
                            || sTo.startsWith("EY")))
          || (cFrom == 'e' && (sTo.startsWith("EH")
                               || sTo.startsWith("ER")
                               || sTo.startsWith("IY")))
          || (cFrom == 'i' && (sTo.startsWith("AY")
                               || sTo.startsWith("ER")
                               || sTo.startsWith("IH")
                               || sTo.startsWith("IY")))
          || (cFrom == 'o' && (sTo.startsWith("AA")
                               || sTo.startsWith("AO")
                               || sTo.startsWith("AW")
                               || sTo.startsWith("OW")
                               || sTo.startsWith("OY")
                               || sTo.startsWith("UH")
                               || sTo.startsWith("UW")))
          || (cFrom == 'u' && (sTo.startsWith("AH")
                               || sTo.startsWith("ER")
                               || sTo.startsWith("UH")
                               || sTo.startsWith("UW")))
          ) {
          iDistance = 2;
        } else if ( // vowels
          ("aeiou".indexOf(cFrom) >= 0 && "aeiou".indexOf(sTo.toLowerCase().charAt(0)) >= 0)
          ) {
          iDistance = 8;
        }
        step.setStepDistance(iDistance);
      } // no direct match
    } // labels set
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
  
} // Orthography2ARPAbetComparator
