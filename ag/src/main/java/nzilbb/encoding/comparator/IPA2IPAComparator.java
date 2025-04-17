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
 * Comparator that maps one IPA-encoded series to another.
 */
public class IPA2IPAComparator<E> implements EditComparator<E> {
  // phone classes
  Set<String> vowels;
  Set<String> diphthongs = new HashSet<String>(
    Arrays.asList(
      "ɔɪ","ǝʊ","əʊ","eɪ","oʊ","aʊ","ɪǝ","ɪə","ɛǝ","ɛə","ʊǝ","ʊə","ai","aɪ","au","ɔy","œy",
      // MFA diphtongs
      "aj","aw","ej","ɔj","əw"
      ));
  Set<String> monophthongs = new HashSet<String>(
    Arrays.asList(
      "c","ŋ̩","ɛ","m̩","n̩","i","iː","ɪ","l̩","ɑ̃ː","ɒ","ɒː","ɒ̃ː","u","uː","ʉː","ʊ","ʌ","æ̃ː",
      "ɑ","ɑː","æ","ɔː","ɔ","ǝ","ə","y","yː","œ","ɜː","ɜ","ɛ","œː","ɛː","ɨ","ʉ","ɯ","ʏ",
      "ø","ɵ","ɘ","ɞ","ɶ","ɐ", "e", "eː"));
  Set<String> consonants;
  Set<String> plosives = new HashSet<String>(
    Arrays.asList(
      "p","b","t","d","ʈ","ɖ","c","ɟ","k","g","ɡ","q","ɢ","ʔ",
      // MFA
      "bʲ","dʲ","kʰ","pʰ","pʲ","tʰ","tʲ"));
  Set<String> nasals = new HashSet<String>(
    Arrays.asList(
      "m","ɱ","n","ɳ","ɲ","ŋ","ɴ",
      // MFA
      "mʲ"));
  Set<String> trills = new HashSet<String>(
    Arrays.asList("ʙ","r","ʀ"));
  Set<String> tapFlap = new HashSet<String>(
    Arrays.asList("ⱱ","ɾ","ɽ"));
  Set<String> fricatives = new HashSet<String>(
    Arrays.asList(
      "ɸ","β","f","v","θ","ð","s","z","ʃ","ʒ","ʂ","ʐ","ç","ʝ","x","ɣ","χ","ʁ",
      "ħ","ʕ","h","ɦ","ɬ","ɮ",
      // MFA
      "fʲ","vʲ"));
  Set<String> approximants = new HashSet<String>(
    Arrays.asList("ʋ","	ɹ","ɻ","j","ɰ","l","ɭ","ʎ","ʟ",
                  // MFA
                  "ɟ","ɫ","ɫ̩"));
  Set<String> clicks = new HashSet<String>(
    Arrays.asList("ʘ","ǀ","ǃ","ǂ","ǁ"));
  Set<String> voicedImplosives = new HashSet<String>(
    Arrays.asList("ɓ","ɗ","ʄ","ɠ","ʛ"));
  Set<String> ejectives = new HashSet<String>(
    Arrays.asList("pʼ","tʼ","kʼ", "sʼ"));
  Set<String> affricates = new HashSet<String>(
    Arrays.asList(
      "ʧ","t͜ʃ","ʤ","d͜ʒ","pf","t͜s",
      "t͡s","t͡ʃ","t͡ɕ","ʈ͡ʂ","d͡z","d͡ʒ","d͡ʑ","ɖ͡ʐ ",
      // MFA
      "dʒ","tʃ"));
  Set<String> typesOfR = new HashSet<String>(Arrays.asList("ɹ","r","ɾ","ɽ","ʁ","ʀ"));
  Set<String> flapTD = new HashSet<String>(Arrays.asList("t","d","ɾ"));
  Set<String> stopFricativeD = new HashSet<String>(Arrays.asList("ð","d"));
  Set<String> vocalisedNG = new HashSet<String>(Arrays.asList("ŋ","ŋ̩"));
  Set<String> vocalisedM = new HashSet<String>(Arrays.asList("m","m̩"));
  Set<String> vocalisedN = new HashSet<String>(Arrays.asList("n","n̩"));
  Set<String> vocalisedL = new HashSet<String>(Arrays.asList("l","l̩","ɫ̩"));
  
  /** Constructor */
  public IPA2IPAComparator() {
    vowels = new HashSet<String>();
    vowels.addAll(monophthongs);
    vowels.addAll(diphthongs);
    consonants = new HashSet<String>();
    consonants.addAll(plosives);
    consonants.addAll(nasals);
    consonants.addAll(trills);
    consonants.addAll(tapFlap);
    consonants.addAll(fricatives);
    consonants.addAll(approximants);
    consonants.addAll(clicks);
    consonants.addAll(voicedImplosives);
    consonants.addAll(ejectives);
    consonants.addAll(affricates);
    consonants.addAll(Arrays.asList("ŋ̩","m̩","n̩","l̩"));
  }
  
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
      if (sFrom.replaceAll("ː","").equals(sTo.replaceAll("ː","")) // the same except for length
          || sFrom.replaceAll("[͜͡]","").equals(sTo.replaceAll("[͜͡]",""))) { // same without tie
        iDistance = 2;
      } else if ( // allophones
        (flapTD.contains(sFrom) && flapTD.contains(sTo))
        || (typesOfR.contains(sFrom) && typesOfR.contains(sTo))
        || (vocalisedNG.contains(sFrom) && vocalisedNG.contains(sTo))
        || (vocalisedM.contains(sFrom) && vocalisedM.contains(sTo))
        || (vocalisedN.contains(sFrom) && vocalisedN.contains(sTo))
        || (vocalisedL.contains(sFrom) && vocalisedL.contains(sTo))
        || (stopFricativeD.contains(sFrom) && stopFricativeD.contains(sTo))
        ) {
        iDistance = 4;
      } else if ( // same narrow type
        (diphthongs.contains(sFrom) && diphthongs.contains(sTo))
        || (monophthongs.contains(sFrom) && monophthongs.contains(sTo))
        || (plosives.contains(sFrom) && plosives.contains(sTo))
        || (nasals.contains(sFrom) && nasals.contains(sTo))
        || (trills.contains(sFrom) && trills.contains(sTo))
        || (tapFlap.contains(sFrom) && tapFlap.contains(sTo))
        || (fricatives.contains(sFrom) && fricatives.contains(sTo))
        || (approximants.contains(sFrom) && approximants.contains(sTo))
        || (clicks.contains(sFrom) && clicks.contains(sTo))
        || (voicedImplosives.contains(sFrom) && voicedImplosives.contains(sTo))
        || (ejectives.contains(sFrom) && ejectives.contains(sTo))
        || (affricates.contains(sFrom) && affricates.contains(sTo))
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
