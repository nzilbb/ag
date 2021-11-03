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
 * Comparator that maps one DISC-encoded series to another.
 */
public class DISC2DISCComparator<E> implements EditComparator<E> {
  // phone classes
  String vowels = "cCEFHiIPqQuUV0123456789~#{$@WBXy";
  String diphthongs = "012456789WBX";
  String monophthongs = "cCEFHiIPqQuUV3y";
  String consonants = "bCdDfFghHjJklLmnNpPrRsStTvwxzZ_?";
  String plosives = "bdgkpt?";
  String fricatives = "CDfhsSTvxzZ";
  String affricates = "J_+=";
  String nasals = "mnN";
  String liquids = "lLrRwx";
  String flapTD = "tdL";
  String linkingR = "rR";
  String vocalisedNG = "NC";
  String vocalisedM = "mF";
  String vocalisedN = "nH";
  String vocalisedL = "lP";
  
  /**
   * Compares two sequence elements, and evaluates the distance between them.
   * @param from The element from the source sequence, which may be null,
   * @param to The element from the destination sequence, which may be null.
   * @return An edit step between the two elements. {@link EditStep#getFrom()} is set to
   * <var>from</var>, {@link EditStep#getTo()} is set to <var>to</var>, 
   * {@link EditStep#getStepDistance()} is set to the computed edit distance between
   * these two elements, and {@link EditStep#getOperation()} is set to either
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
    } else if (!from.toString().equals(to.toString())) {
      step.setOperation(EditStep.StepOperation.CHANGE);
      int iDistance = 20;
      // This is DISC, so the label should be one character long
      char cFrom = from.toString().charAt(0);
      char cTo = to.toString().charAt(0);
      if ( // allophones
        (flapTD.indexOf(cFrom) >= 0 && flapTD.indexOf(cTo) >= 0)
        || (vocalisedNG.indexOf(cFrom) >= 0 && vocalisedNG.indexOf(cTo) >= 0)
        || (vocalisedM.indexOf(cFrom) >= 0 && vocalisedM.indexOf(cTo) >= 0)
        || (vocalisedN.indexOf(cFrom) >= 0 && vocalisedN.indexOf(cTo) >= 0)
        || (vocalisedL.indexOf(cFrom) >= 0 && vocalisedL.indexOf(cTo) >= 0)
        ) {
	    iDistance = 2;
      } else if ( // same narrow type
        (diphthongs.indexOf(cFrom) >= 0 && diphthongs.indexOf(cTo) >= 0)
        || (monophthongs.indexOf(cFrom) >= 0 && monophthongs.indexOf(cTo) >= 0)
        || (liquids.indexOf(cFrom) >= 0 && liquids.indexOf(cTo) >= 0)
        || (nasals.indexOf(cFrom) >= 0 && nasals.indexOf(cTo) >= 0)
        || (affricates.indexOf(cFrom) >= 0 && affricates.indexOf(cTo) >= 0)
        || (fricatives.indexOf(cFrom) >= 0 && fricatives.indexOf(cTo) >= 0)
        || (plosives.indexOf(cFrom) >= 0 && plosives.indexOf(cTo) >= 0)
        ) {
        iDistance = 4;
      } else if ( // same broad type
        (vowels.indexOf(cFrom) >= 0 && vowels.indexOf(cTo) >= 0)
//	    || (consonants.indexOf(cFrom) >= 0 && consonants.indexOf(cTo) >= 0)
        ) {
        iDistance = 8;
      }		  
      step.setStepDistance(iDistance);
    } else { // equal
      // but we want consonant matching to be more easier than vowel mapping
      // because vowels are more likely to vary than consonants
      // so weight vowels slightly
      if (vowels.indexOf(from.toString().charAt(0)) >= 0) {
        step.setStepDistance(1);
      }
    } // equal
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

} // DISC2DISCComparator
