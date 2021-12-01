//
// Copyright 2021 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import nzilbb.editpath.EditComparator;
import nzilbb.editpath.EditStep;

/**
 * Comparator that maps one DISC-encoded series to another.
 */
public class IPA2IPAComparator<E> implements EditComparator<E> {
  // phone classes
  Set<String> vowels = new HashSet<String>(Arrays.asList("c","ŋ̩","ɛ","m̩","n̩","i","iː","ɪ","l̩","ɑ̃","ɑ̃ː","ɒ","u","ʊ","ʌ","æ̃","æ̃ː","eɪ","ɜː","ɜ","ɔɪ","ǝʊ","aʊ","ɪǝ","ɛǝ","ʊǝ","ɒ̃ː","ɒ̃","ɑː","ɑ","æ","ɔ","ɔː","ǝ","ə","ai","au","ɔy","yː","y"));
  Set<String> diphthongs = new HashSet<String>(Arrays.asList("ɔɪ","ǝʊ","aʊ","ɪǝ","ɛǝ","ʊǝ","ai","au","ɔy"));
  Set<String> monophthongs = new HashSet<String>(Arrays.asList("c","ŋ̩","ɛ","m̩","n̩","iː","ɪ","l̩","ɑ̃ː","ɒ","u","ʊ","ʌ","æ̃ː","ɒ̃ː","ɑː","æ","ɔː","ǝ","ə","yː"));
  Set<String> consonants = new HashSet<String>(Arrays.asList("b","ʙ","ŋ̩","d","ð","f","m̩","g","h","n̩","j","ʧ","t͜ʃ","k","l","ɾ","m","n","ŋ","p","pf","l̩","ɹ","ɾ","r","ʀ","s","ʃ","t","t͜s","θ","v","w","x","z","ʒ","ʤ","d͜ʒ","ʔ", "ɸ", "β", "ɣ", "ʂ", "ʐ", "ç", "ʝ", "ɣ", "ʁ", "ħ", "ʕ", "ɦ","ʈ","ɖ","c","ɟ","q","ɢ","ɳ","ɲ","ɴ","ⱱ","ɽ","ɬ","ɮ","ʋ","ɻ","ɰ","ɭ","ʎ","ʟ"));
  Set<String> plosives = new HashSet<String>(Arrays.asList("b","d","g","k","p","t","ʔ","ʈ","ɖ","c","ɟ","q","ɢ"));
  Set<String> fricatives = new HashSet<String>(Arrays.asList("ð","f","h","s","ʃ","θ","v","x","z","ʒ","ɸ", "β", "ɣ", "ʂ", "ʐ", "ç", "ʝ", "ɣ", "ʁ", "ħ", "ʕ", "ɦ","ɬ","ɮ"));
  Set<String> affricates = new HashSet<String>(Arrays.asList("ʧ","t͜ʃ","ʤ","d͜ʒ","pf","t͜s"));
  Set<String> nasals = new HashSet<String>(Arrays.asList("m","n","ŋ","ɳ","ɲ","ɴ"));
  Set<String> liquids = new HashSet<String>(Arrays.asList("l","Y","ɹ","r","w","x","ɽ","ɻ","ɰ","ɭ"));
  Set<String> typesOfR = new HashSet<String>(Arrays.asList("ɹ","r","ɾ","ɽ"));
  Set<String> flapTD = new HashSet<String>(Arrays.asList("t","d","ɾ"));
  Set<String> vocalisedNG = new HashSet<String>(Arrays.asList("ŋ","ŋ̩"));
  Set<String> vocalisedM = new HashSet<String>(Arrays.asList("m","m̩"));
  Set<String> vocalisedN = new HashSet<String>(Arrays.asList("n","n̩"));
  Set<String> vocalisedL = new HashSet<String>(Arrays.asList("l","l̩"));
  
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
      int iDistance = 40;
      String sFrom = from.toString();
      String sTo = to.toString();
      if (sFrom.replaceAll("ː","").equals(sTo.replaceAll("ː",""))) { // the same except for length
        iDistance = 2;
      } else if ( // allophones
        (flapTD.contains(sFrom) && flapTD.contains(sTo))
        || (typesOfR.contains(sFrom) && typesOfR.contains(sTo))
        || (vocalisedNG.contains(sFrom) && vocalisedNG.contains(sTo))
        || (vocalisedM.contains(sFrom) && vocalisedM.contains(sTo))
        || (vocalisedN.contains(sFrom) && vocalisedN.contains(sTo))
        || (vocalisedL.contains(sFrom) && vocalisedL.contains(sTo))
        ) {
        iDistance = 4;
      } else if ( // same narrow type
        (diphthongs.contains(sFrom) && diphthongs.contains(sTo))
        || (monophthongs.contains(sFrom) && monophthongs.contains(sTo))
        || (liquids.contains(sFrom) && liquids.contains(sTo))
        || (nasals.contains(sFrom) && nasals.contains(sTo))
        || (affricates.contains(sFrom) && affricates.contains(sTo))
        || (fricatives.contains(sFrom) && fricatives.contains(sTo))
        || (plosives.contains(sFrom) && plosives.contains(sTo))
        ) {
        iDistance = 8;
      } else if ( // same broad type
        (vowels.contains(sFrom) && vowels.contains(sTo))
//	    || (consonants.contains(sFrom) && consonants.contains(sTo))
        ) {
        iDistance = 16;
      }		  
      step.setStepDistance(iDistance);
    } else { // equal
      // but we want consonant matching to be more easier than vowel mapping
      // because vowels are more likely to vary than consonants
      // so weight vowels slightly
      if (vowels.contains(from.toString())) {
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
    return new EditStep<E>(from, null, 20, EditStep.StepOperation.DELETE);
  }
  
  /**
   * The distance for inserting the given element.
   * @param to The element that would be inserted, which may be null.
   * @return An edit step with {@link EditStep#getStepDistance()} set to the distance for
   * inserting the given element. {@link EditStep#getTo()} is set to <var>to</var>, and
   * {@link EditStep#getOperation()} is set to <var>EditStep.StepOperation.INSERT</var> 
   */
  public EditStep<E> insert(E to) {
    return new EditStep<E>(null, to, 20, EditStep.StepOperation.INSERT);
  }

} // IPA2IPAComparator
