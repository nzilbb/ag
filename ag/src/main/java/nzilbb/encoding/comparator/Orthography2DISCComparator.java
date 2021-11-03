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
 * Comparator that maps an (English) orthography series to a DISC-encoded series.
 */
public class Orthography2DISCComparator<E> implements EditComparator<E> {
  // phone classes
  String DISCvowels = "cCEFHiIPqQuUV0123456789~#{$@WBXy";
  String DISCdiphthongs = "012456789WBX";
  String DISCmonophthongs = "cCEFHiIPqQuUV3y";
  String DISCconsonants = "bCdDfFghHjJklLmnNpPrRsStTvwxzZ_?";
  String DISCplosives = "bdgkpt?";
  String DISCfricatives = "CDfhsSTvxzZ";
  String DISCaffricates = "J_+=";
  String DISCnasals = "mnN";
  String DISCliquids = "lLrRwx";
  String DISCflapT = "tL";
  String DISClinkingR = "rR";
  String DISCvocalisedNG = "NC";
  String DISCvocalisedM = "mF";
  String DISCvocalisedN = "nH";
  String DISCvocalisedL = "lP";
  String sameLetters = "pbtdkgmnlrfvszhw";
  
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
      // This is DISC, so the label should be one character long
      char cTo = to.toString().charAt(0);
      if (!(sameLetters.indexOf(cFrom) >= 0 && cFrom == cTo)) {
        // no direct match between orthography and phonology
        step.setOperation(EditStep.StepOperation.CHANGE);
        int iDistance = 10;
        if ( // definite correspondences
          (cFrom == 'r' && cTo == 'R')) {
          iDistance = 0;
        } else if ( // probable consonant correspondences
          (cFrom == 's' && cTo == 'S')
          || (cFrom == 's' && cTo == 'Z')
          || (cFrom == 's' && cTo == 'z')
          || (cFrom == 't' && cTo == 'T')
          || (cFrom == 't' && cTo == 'D')
          || (cFrom == 'n' && cTo == 'N')
          || (cFrom == 'y' && cTo == 'j')
          || (cFrom == 'c' && cTo == 'x')
          || (cFrom == 'c' && cTo == 'J')
          || (cFrom == 'd' && cTo == '_')
          || (cFrom == 'j' && cTo == '_')
          || (cFrom == 'g' && cTo == '_')
          || (cFrom == 'n' && cTo == 'C')
          || (cFrom == 'm' && cTo == 'F')
          || (cFrom == 'n' && cTo == 'H')
          || (cFrom == 'l' && cTo == 'P')
          || (cFrom == 't' && cTo == 'L')
          ) {
          iDistance = 2;
        } else if ( // probable vowle correspondences
          (cFrom == 'a' && "{V#18".indexOf(cTo) >= 0)
          || (cFrom == 'e' && "Ei378q".indexOf(cTo) >= 0)
          || (cFrom == 'i' && "I2c0".indexOf(cTo) >= 0)
          || (cFrom == 'o' && "Q$u4569~".indexOf(cTo) >= 0)
          || (cFrom == 'u' && "VUu32".indexOf(cTo) >= 0)
          ) {
          iDistance = 2;
        } else if ( // vowels
          ("aeiou".indexOf(cFrom) >= 0 && DISCvowels.indexOf(cTo) >= 0)
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

} // Orthography2DISCComparator
