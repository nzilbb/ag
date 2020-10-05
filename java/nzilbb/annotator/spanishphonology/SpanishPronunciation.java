//
// Spanish transcription code based on:
// https://github.com/easypronunciation/spanish-pronunciation-rules-php
// a PHP function that converts a Spanish word (UTF-8 encoded) into IPA phonetic transcription symbols.
// Written by Timur Baytukalov, http://easypronunciation.com/en/
// Contact me at: http://easypronunciation.com/en/contacts
// License: http://www.gnu.org/licenses/gpl.html
// @version 0.1
// 
// Ported to Java by robert.fromont@canterbury.ac.nz 2016-01-26
//
// Copyright 2016 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
//
//    This file is part of spanishphonology.
//
//    spanishphonology is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 2 of the License, or
//    (at your option) any later version.
//
//    spanishphonology is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with spanishphonology; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package nz.ac.canterbury.ling.spanishphonology;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.SortedSet;

/**
 * Converts a Spanish word into IPA phonemic transcription symbols.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class SpanishPronunciation {
   // Attributes:
   private TreeSet<String> locales = new TreeSet<String>();
   private HashMap<String,String> word_conversions = new HashMap<String,String>();
   private HashMap<Character,Character> spanish_letters = new HashMap<Character,Character>();
   private HashMap<Character, HashMap<String,Character>> spanish_letters_localized = new HashMap<Character, HashMap<String,Character>>();
   private HashMap<String, HashMap<String,Character>> spanish_letters_localized_variables = new HashMap<String, HashMap<String,Character>>();
   private HashSet<Character> s_becomes_z = new HashSet<Character>();
   private HashSet<Character> v_becomes_b_transcription_signs = new HashSet<Character>();
   private HashSet<Character> a_e_i_o_u = new HashSet<Character>();
   private HashSet<Character> i_e_for_c_and_g = new HashSet<Character>();
   private HashSet<Character> a_e_o_u_for_i = new HashSet<Character>();
   private HashSet<Character> a_e_o_i_for_u = new HashSet<Character>();
   private HashSet<Character> a_o_u_for_nc = new HashSet<Character>();
   private HashSet<Character> n_becomes_m = new HashSet<Character>();

   
   /**
    * Whether vowels that carry an orthographic accent (á, é, í, ó, ú) are represented with a capital letted in the phonemic transcription.
    * @see #getCapitalAccentedVowels()
    * @see #setCapitalAccentedVowels(boolean)
    */
   protected boolean capitalAccentedVowels = false;
   /**
    * Getter for {@link #capitalAccentedVowels}: Whether vowels that carry an orthographic accent (á, é, í, ó, ú) are represented with a capital letted in the phonemic transcription.
    * @return Whether vowels that carry an orthographic accent (á, é, í, ó, ú) are represented with a capital letted in the phonemic transcription.
    */
   public boolean getCapitalAccentedVowels() { return capitalAccentedVowels; }
   /**
    * Setter for {@link #capitalAccentedVowels}: Whether vowels that carry an orthographic accent (á, é, í, ó, ú) are represented with a capital letted in the phonemic transcription.
    * @param newCapitalAccentedVowels Whether vowels that carry an orthographic accent (á, é, í, ó, ú) are represented with a capital letted in the phonemic transcription.
    */
   public void setCapitalAccentedVowels(boolean newCapitalAccentedVowels) { capitalAccentedVowels = newCapitalAccentedVowels; }

   
   // Methods:
   
   /**
    * Default constructor.
    */
   public SpanishPronunciation() {

      // add conversions for common 'non words'
      word_conversions.put("1", "uno");
      word_conversions.put("2", "dós");
      word_conversions.put("3", "trés");
      word_conversions.put("4", "cuatro");
      word_conversions.put("5", "cinco");
      word_conversions.put("6", "seis");
      word_conversions.put("7", "siete");
      word_conversions.put("8", "ocho");
      word_conversions.put("9", "nueve");
      word_conversions.put("10", "diez");
      word_conversions.put("11", "once");
      word_conversions.put("12", "doce");
      word_conversions.put("13", "trece");
      word_conversions.put("14", "catorce");
      word_conversions.put("15", "quince");
      word_conversions.put("16", "dieciséis");
      word_conversions.put("17", "diecisiete");
      word_conversions.put("18", "dieciocho");
      word_conversions.put("19", "diecinueve");
      word_conversions.put("20", "veinte");
      word_conversions.put("21", "veintiuno");
      word_conversions.put("22", "veintidós");
      word_conversions.put("23", "veintitrés");
      word_conversions.put("24", "veinticuatro");
      word_conversions.put("25", "veinticinco");
      word_conversions.put("26", "veintiseis");
      word_conversions.put("27", "veintisiete");
      word_conversions.put("28", "veintiocho");
      word_conversions.put("29", "veintinueve");
      word_conversions.put("30", "treinta");
      word_conversions.put("31", "treinta y uno");
      word_conversions.put("32", "treinta y dós");
      word_conversions.put("33", "treinta y trés");
      word_conversions.put("34", "treinta y cuatro");
      word_conversions.put("35", "treinta y cinco");
      word_conversions.put("36", "treinta y seis");
      word_conversions.put("37", "treinta y siete");
      word_conversions.put("38", "treinta y ocho");
      word_conversions.put("39", "treinta y nueve");
      word_conversions.put("40", "cuarenta");
      word_conversions.put("41", "cuarenta y uno");
      word_conversions.put("42", "cuarenta y dós");
      word_conversions.put("43", "cuarenta y trés");
      word_conversions.put("44", "cuarenta y cuatro");
      word_conversions.put("45", "cuarenta y cinco");
      word_conversions.put("46", "cuarenta y seis");
      word_conversions.put("47", "cuarenta y siete");
      word_conversions.put("48", "cuarenta y ocho");
      word_conversions.put("49", "cuarenta y nueve");
      word_conversions.put("50", "cinquenta");
      word_conversions.put("60", "sesenta");
      word_conversions.put("70", "setenta");
      word_conversions.put("80", "ochenta");
      word_conversions.put("90", "noventa");
      word_conversions.put("100", "cien");

      // all the letters from the following array are pronounced always the same way by all Spanish speakers:
      // (capital letter means that the vowel letter is stressed).
      
      spanish_letters.put('a', 'a');
      spanish_letters.put('á', 'A');
      spanish_letters.put('e', 'e');
      spanish_letters.put('é', 'E');
      spanish_letters.put('f', 'f');
      spanish_letters.put('í', 'I');
      spanish_letters.put('j', 'x');
      spanish_letters.put('k', 'k');
      spanish_letters.put('m', 'm');
      spanish_letters.put('ñ', 'ɲ');
      spanish_letters.put('o', 'o');
      spanish_letters.put('ó', 'O');
      spanish_letters.put('p', 'p');
      spanish_letters.put('q', 'k');
      spanish_letters.put('t', 't');
      spanish_letters.put('ú', 'U');
      spanish_letters.put('w', 'w');

      // localized pronunciation variants:
      HashMap<String,Character> z = new HashMap<String,Character>();
      z.put("es_ES", 'θ');
      z.put("es_MX", 's');
      spanish_letters_localized.put('z', z);

      HashMap<String,Character> c_before_i_or_e = new HashMap<String,Character>();
      c_before_i_or_e.put("es_ES", 'θ');
      c_before_i_or_e.put("es_MX", 's');
      spanish_letters_localized_variables.put("c_before_i_or_e", c_before_i_or_e);

      locales.add("es_ES");
      locales.add("es_MX");

      s_becomes_z.add('l');
      s_becomes_z.add('m');
      s_becomes_z.add('n');
      s_becomes_z.add('b');
      s_becomes_z.add('d');
      s_becomes_z.add('g');

      v_becomes_b_transcription_signs.add('m');
      v_becomes_b_transcription_signs.add('n');
      v_becomes_b_transcription_signs.add('ɲ');

      a_e_i_o_u.add('a');
      a_e_i_o_u.add('e');
      a_e_i_o_u.add('i');
      a_e_i_o_u.add('o');
      a_e_i_o_u.add('u');
      a_e_i_o_u.add('á');
      a_e_i_o_u.add('é');
      a_e_i_o_u.add('í');
      a_e_i_o_u.add('ó');
      a_e_i_o_u.add('ú');

      i_e_for_c_and_g.add('e');
      i_e_for_c_and_g.add('i');
      i_e_for_c_and_g.add('é');
      i_e_for_c_and_g.add('í');

      a_e_o_u_for_i.add('a');
      a_e_o_u_for_i.add('e');
      a_e_o_u_for_i.add('o');
      a_e_o_u_for_i.add('u');
      a_e_o_u_for_i.add('á');
      a_e_o_u_for_i.add('é');
      a_e_o_u_for_i.add('ó');
      a_e_o_u_for_i.add('ú');

      a_e_o_i_for_u.add('a');
      a_e_o_i_for_u.add('e');
      a_e_o_i_for_u.add('o');
      a_e_o_i_for_u.add('i');
      a_e_o_i_for_u.add('á');
      a_e_o_i_for_u.add('é');
      a_e_o_i_for_u.add('ó');
      a_e_o_i_for_u.add('í');

      a_o_u_for_nc.add('a');
      a_o_u_for_nc.add('o');
      a_o_u_for_nc.add('u');
      a_o_u_for_nc.add('á');
      a_o_u_for_nc.add('ó');
      a_o_u_for_nc.add('ú');

      n_becomes_m.add('b');
      n_becomes_m.add('f');
      n_becomes_m.add('m');
      n_becomes_m.add('p');
      n_becomes_m.add('v');

   } // end of constructor

   /**
    * Returns a list of supported locales, as ISO Identifiers.
    * @return A list of supported locales, as ISO Identifiers, sorted alphabetically.
    */
   public SortedSet<String> getSupportedLocales() {
      return locales;
   } // end of getSupportedLocales()

   /**
    * Synonym for {@link #convert_spanish_word_to_phonetic_transcription(String,String)}
    * @param word The orthographic spelling of the word.
    * @param locale The locale to use - one of {@link #getSupportedLocales()}
    * @return The phonemic transcription of the word.
    */
   public String transcribe(String word, String locale) {
      return convert_spanish_word_to_phonetic_transcription(word, locale);
   } // end of transcribe()

   /**
    * Phonemically transcribe a given word.
    * @param word The orthographic spelling of the word.
    * @param locale The locale to use - one of {@link #getSupportedLocales()}
    * @return The phonemic transcription of the word.
    */
   public String convert_spanish_word_to_phonetic_transcription(String word, String locale) {    
      // we return an error, if the locale is not supported:
      if (!locales.contains(locale)) return null;

      if (word_conversions.containsKey(word)) {
	 word = word_conversions.get(word);
      }
      
      // we convert the word to lowercase:
      word = word.toLowerCase();

      // we set the future phonetic transcription
      StringBuffer phonetic_transcription = new StringBuffer();

      // we set the variable that will allow us to skip some letters, if we want to:
      int skip_next_letter = 0;
	
      for (int current_position = 1; current_position < word.length() + 1; current_position++) {
	 // we skip the current letter (repeat the cycle the desired number of times):
	 if (skip_next_letter > 0) {
	    skip_next_letter--;
	    continue;
	 }

	 // we set the previous and the following letters:
	 char current_letter = word.charAt(current_position-1);
	 char previous_letter = '\0';
	 char next_letter = '\0';
	 char after_next_letter = '\0';
	 if (current_position>1) { previous_letter = word.charAt(current_position-2); }
	 if (current_position<word.length()) { next_letter = word.charAt(current_position); }
	 if (current_position<word.length()-1) { after_next_letter = word.charAt(current_position+1); }

	 // we set the last transcription sign
	 char last_transcription_sign = '\0';
	 if (phonetic_transcription.length() > 0) { 
	    last_transcription_sign = phonetic_transcription.charAt(phonetic_transcription.length()-1);
	 }

	 // the letter is pronounced the same way by all Spanish speakers:
	 if (spanish_letters.containsKey(current_letter)) {
	    phonetic_transcription.append(spanish_letters.get(current_letter));
	    continue;
	 }

	 // the letter can be pronounced differently by Spanish speakers from different countries:
	 if (spanish_letters_localized.containsKey(current_letter)) {
	    phonetic_transcription.append(spanish_letters_localized.get(current_letter).get(locale));
	    continue;
	 }

	 // letters "b" and "v" are equivalent:
	 if ((current_letter == 'b') || (current_letter == 'v')) {
	    // at the beginning of a word
	    if ((current_position == 1) ||
		// [mb], [nb], [ɲb]
		(v_becomes_b_transcription_signs.contains(last_transcription_sign))) 
	    {
	       phonetic_transcription.append('b');
	    } 
	    else 
	    {
	       phonetic_transcription.append('β');
	    }
	    continue;
	 }
	
	 if (current_letter == 'c') {
	    if (i_e_for_c_and_g.contains(next_letter)) {
	       phonetic_transcription.append(spanish_letters_localized_variables.get("c_before_i_or_e").get(locale));
	       continue;
	    }
	    if (next_letter == 'h') {
	       phonetic_transcription.append('ʧ');
	       skip_next_letter = 1;
	       continue;
	    }
	    phonetic_transcription.append('k');
	    continue;
	 }
	 
	 if (current_letter == 'd') {
	    // at the beginning of a word
	    if ((current_position == 1) ||
		// [nd]
		(last_transcription_sign == 'n') ||
		// [ld]
		(last_transcription_sign == 'l')) {
	       phonetic_transcription.append('d');
	    } else {
	       phonetic_transcription.append('ð');
	    }
	    continue;
	 }
		
	 if (current_letter == 'g') {
	    if (i_e_for_c_and_g.contains(next_letter)) {
	       phonetic_transcription.append('x');
	       continue;
	    }
	    // at the beginning of a word
	    if ((current_position == 1) ||
		// "ng"
		(previous_letter == 'n') ||
		// "lg"
		(previous_letter == 'l')) {
	       phonetic_transcription.append('g');
	       continue;
	    }
	    phonetic_transcription.append('ɣ');
	    continue;
	 }

	 if (current_letter == 'i') {
	    if (a_e_o_u_for_i.contains(next_letter)) {
	       phonetic_transcription.append('j');
	    } else {
	       phonetic_transcription.append('i');
	    }
	    continue;
	 }
	 
	 if (current_letter == 'l') {
	    // "ll"
	    if (next_letter == 'l') {
	       phonetic_transcription.append('ʎ');
	       skip_next_letter = 1;
	       continue;
	    }
	    phonetic_transcription.append('l');
	    continue;
	 }

	 if (current_letter == 'n') {
	    if (n_becomes_m.contains(next_letter)) {
	       phonetic_transcription.append('m');
	       continue;
	    }
	    // "nca", "nco", "ncu"
	    if (((next_letter == 'c') && (a_o_u_for_nc.contains(after_next_letter))) ||
		// "nqu"
		((next_letter == 'q') && ((after_next_letter == 'u') || (after_next_letter == 'ú'))) ||
		// "nk"
		(next_letter == 'k') ||
		// "ng"
		(next_letter == 'g') ||
		// "nj"
		(next_letter == 'j')) {
	       phonetic_transcription.append('ŋ');
	       continue;
	    }
	    // "nll"
	    if (((next_letter == 'l') && (after_next_letter == 'l')) ||
		// "nch"
		((next_letter == 'c') && (after_next_letter == 'h')) ||
		// "nhi"
		((next_letter == 'h') && ((after_next_letter == 'i') || (after_next_letter == 'í'))) ||
		// "ny"
		(next_letter == 'y')) {
	       phonetic_transcription.append('ɲ');
	       continue;
	    }
	    phonetic_transcription.append('n');
	    continue;
	 }
	 
	 if (current_letter == 'r') {
	    // at the beginning of a word
	    if ((current_position == 1) ||
		// "nr"
		(last_transcription_sign == 'n') ||
		// "lr"
		(last_transcription_sign == 'l') ||
		// "sr"
		(last_transcription_sign == 's') ||
		// "rr"
		(next_letter == 'r')) {
	       phonetic_transcription.append('r');
	       if (next_letter == 'r') {
		  skip_next_letter = 1;
	       }
	       continue;
	    }
	    phonetic_transcription.append('ɾ');
	    continue;
	 }
	
	 if (current_letter == 's') {
	    if (s_becomes_z.contains(next_letter)) {
	       phonetic_transcription.append('z');
	       continue;
	    }
	    phonetic_transcription.append('s');
	    continue;
	 }

	 if (current_letter == 'u') {
	    // "gui", "gue" - not pronounced
	    if (((previous_letter == 'g') && (i_e_for_c_and_g.contains(next_letter))) ||
		// "qu" - not pronounced
		(previous_letter == 'q')) {
	       continue;
	    }
	    // "ua", "ue", "ui", "uo"
	    if (a_e_o_i_for_u.contains(next_letter)) {
	       phonetic_transcription.append('w');
	       continue;
	    }
	    phonetic_transcription.append('u');
	    continue;
	 }
	 
	 if (current_letter == 'ü') {
	    // "üa", "üe", "üo", "üi"
	    if (a_e_o_i_for_u.contains(next_letter)) {
	       phonetic_transcription.append('w');
	       continue;
	    }
	    phonetic_transcription.append('u');
	    continue;
	 }
	 
	 if (current_letter == 'x') {
	    // words starting with "méxic", "mexic" are exceptions:
	    if ((current_position == 3) && ((word.startsWith("méxic") || (word.startsWith("mexic"))))) {
	       phonetic_transcription.append('x');
	       continue;
	    }
	    phonetic_transcription.append("ks");
	    continue;
	 }
	 
	 if (current_letter == 'y') {
	    // the next letter is vowel
	    if (a_e_i_o_u.contains(next_letter)) {
	       phonetic_transcription.append('ʝ');
	       continue;
	    }
	    // the following is for proper handling of the accent position later:
	    if ((word.length() > 1) && (current_position == word.length())) {
	       phonetic_transcription.append('Y');
	    } else {
	       phonetic_transcription.append('i');
	    }
	    continue;
	 }
      }

      // the following is to normalize the phonetic transcription to the IPA standard
      // we don't use this when processing the Spanish text
      String transcription = phonetic_transcription.toString();
      if (!capitalAccentedVowels) {
	 HashMap<Character,Character> stressedToStandard = new HashMap<Character,Character>();
	 stressedToStandard.put('A', 'a');
	 stressedToStandard.put('E', 'e');
	 stressedToStandard.put('I', 'i');
	 stressedToStandard.put('O', 'o');
	 stressedToStandard.put('U', 'u');
	 stressedToStandard.put('Y', 'i');
	 for (Character original : stressedToStandard.keySet()) {
	    transcription = transcription.replace(original, stressedToStandard.get(original));
	 }
      }
      
      return transcription;

   }

} // end of class SpanishPronunciation
