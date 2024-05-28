//
// Copyright 2020 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of nzilbb.ag.
//
//    nzilbb.ag is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 3 of the License, or
//    (at your option) any later version.
//
//    nzilbb.ag is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with nzilbb.ag; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package nzilbb.encoding;

import java.util.HashSet;

/**
 * Translates CELEX-DISC-encoded transcriptions like
 * <tt>tr{nskrIpS@n</tt>
 * to a form that would word for a Hidden Markov Model Toolkit (HTK) .dict file, like:
 * <tt>t r _{ n s k r I p S _@ n</tt>.
 * 
 * <p> The phonemes are space-delimited, and labels that don't start with an alphabetic
 * character are prefixed with '_' to avoid HTK processing errors.
 *
 * <p> This translator will also handle input that is IPA-encoded, e.g.
 * <tt>ˈmʌt.n̩.ˌt͡ʃɔps</tt>
 * to a form that would word for a Hidden Markov Model Toolkit (HTK) .dict file, like:
 * <tt>m _ʌ t n̩ t͡ʃ _ɔ p s</tt>.
 *

<style type="text/css">
 #mapping td:first-child, #mapping th:first-child { text-align: right; } 
 #mapping td:nth-child(2) { text-align: center; } 
 #mapping td:nth-child(3) { font-family: monospace; } 
 #mapping td:first-child { font-family: monospace; } 
</style>
<table id="mapping"><caption>Mapping</caption>
 <thead><tr>
  <th>Source</th><th></th><th>Destination</th><th>Example</th>
 </tr></thead>
 <tbody>

 </tbody>
</table>

 * <style type="text/css"> tt:before { content: '/'; } tt:after { content: '/'; } </style>
 * @see HTK2DISC
 * @author Robert Fromont robert@fromont.net.nz
 */
public class DISC2HTK extends PhonemeTranslator {

   /**
    * Default constructor.
    */
   public DISC2HTK() {
      sourceEncoding = "DISC";
      destinationEncoding = "HTK";      
   } // end of constructor
   
   /**
    * Translates a phonemic transcription from the source encoding to the destination encoding.
    * @param source Phonemic transcription in the source encoding.
    * @return Phonemic transcription in the destination encoding.
    */ 
   public String apply(String source) {
      StringBuffer s = new StringBuffer(source.length() * 2);
      // for each character
      for (int c = 0; c < source.length(); c++) {
         char ch = source.charAt(c);
         if (IsIPASuprasegmental(ch)) continue; // skip stress/syllable marking
         if (s.length() > 0) {
            if (IsIPADiacritic(ch)) { // it's a diacritic so add it to the last one
               s.append(source.charAt(c));
               continue;
            }
            
            // tie bar?
            if (ch == '͡' || (c > 0 && source.charAt(c-1) == '͡')) { // e.g. d͡ʒ is one phoneme
               s.append(source.charAt(c));
               continue;
            }
	       
            s.append(" ");
         }
         // if the phone doesn't start with an ASCII letter, prefix it  with an underscore,
         // so HTK doesn't panic about them starting with digits, etc.
         if (!((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z'))) {
            s.append("_");
         }
         s.append(source.charAt(c));         
      }
      
      return s.toString();
   }
   
   private static HashSet<Character> ipaDiacritics;
   /**
    * Determines whether the given character is a known diacritic in IPA.
    * <p>Diacritics include:
    * <dl>
    * <dt> ̩ </dt><dd>syllabic</dd>
    * <dt> ̍ </dt><dd>syllabic</dd>
    * <dt> ̯ </dt><dd>non-syllabic</dd>
    * <dt> ̑ </dt><dd>non-syllabic</dd>
    * <dt> ʰ </dt><dd>aspirated</dd>
    * <dt> ⁿ </dt><dd>nasal release</dd>
    * <dt> ̚ </dt><dd>no audible release</dd>
    * <dt> ˡ </dt><dd>lateral release</dd>
    * <dt> ᶿ </dt><dd>Voiceless dental fricative release</dd>
    * <dt> ˣ </dt><dd>Voiceless velar fricative release</dd>
    * <dt> ᵊ </dt><dd>Mid central vowel release</dd>
    * <dt> ̥ </dt><dd>voiceless</dd>
    * <dt> ̊ </dt><dd>voiceless</dd>
    * <dt> ̤ </dt><dd>breathy voiced</dd>
    * <dt> ̬ </dt><dd>voiced</dd>
    * <dt> ̰ </dt><dd>creaky-voiced</dd>
    * <dt> ̪ </dt><dd>dental</dd>
    * <dt> ̼ </dt><dd>linguolanbial</dd>
    * <dt> ̻ </dt><dd>laminal</dd>
    * <dt> ̺ </dt><dd>apical</dd>
    * <dt> ̟ </dt><dd>advanced</dd>
    * <dt> ˖ </dt><dd>advanced</dd>
    * <dt> ̠ </dt><dd>retracted</dd>
    * <dt> ˗ </dt><dd>retracted</dd>
    * <dt> ̽ </dt><dd>mid-centralized</dd>
    * <dt> ̝ </dt><dd>raised</dd>
    * <dt> ˔ </dt><dd>raised</dd>
    * <dt> ̞ </dt><dd>lowered</dd>
    * <dt> ˕ </dt><dd>lowered</dd>
    * <dt> ̹ </dt><dd>more rounded</dd>
    * <dt> ̜ </dt><dd>less rounded</dd>
    * <dt> ʷ </dt><dd>labialized</dd>
    * <dt> ʲ </dt><dd>palatalized</dd>
    * <dt> ᶣ </dt><dd>labio palatalized</dd>
    * <dt> ᶹ </dt><dd>Labialized without protrusion of the lips or velarization</dd>
    * <dt> ˠ </dt><dd>velarized</dd>
    * <dt> ˤ </dt><dd>Pharyngealized</dd>
    * <dt> ̘ </dt><dd>advanced tongue root</dd>
    * <dt> ̙ </dt><dd>retracted tongue root</dd>
    * <dt> ̃ </dt><dd>nasalized</dd>
    * <dt> ː </dt><dd>long</dd>
    * <dt> ˑ </dt><dd>half long</dd>
    * <dt> ̆ </dt><dd>extra short</dd>
    * </dl>
    * @param c The character.
    * @return true if <var>c</var> is a diacritic, false otherwise
    */
   public static boolean IsIPADiacritic(char c) {
      if (ipaDiacritics == null) {
         ipaDiacritics = new HashSet<Character>();
         ipaDiacritics.add('̩'); // syllabic
         ipaDiacritics.add('̍'); // syllabic
         ipaDiacritics.add('̯'); // non-syllabic
         ipaDiacritics.add('̑'); // non-syllabic
         ipaDiacritics.add('ʰ'); // aspirated
         ipaDiacritics.add('ⁿ'); // nasal release
         ipaDiacritics.add('̚'); // no audible release
         ipaDiacritics.add('ˡ'); // lateral release
         ipaDiacritics.add('ᶿ'); // Voiceless dental fricative release
         ipaDiacritics.add('ˣ'); // Voiceless velar fricative release
         ipaDiacritics.add('ᵊ'); // Mid central vowel release
         ipaDiacritics.add('̥'); // voiceless
         ipaDiacritics.add('̊'); // voiceless
         ipaDiacritics.add('̤'); // breathy voiced
         ipaDiacritics.add('̬'); // voiced
         ipaDiacritics.add('̰'); // creaky-voiced
         ipaDiacritics.add('̪'); // dental
         ipaDiacritics.add('̼'); // linguolanbial
         ipaDiacritics.add('̻'); // laminal
         ipaDiacritics.add('̺'); // apical
         ipaDiacritics.add('̟'); // advanced
         ipaDiacritics.add('˖'); // advanced
         ipaDiacritics.add('̠'); // retracted
         ipaDiacritics.add('˗'); // retracted
         ipaDiacritics.add('̽'); // mid-centralized
         ipaDiacritics.add('̝'); // raised
         ipaDiacritics.add('˔'); // raised
         ipaDiacritics.add('̞'); // lowered
         ipaDiacritics.add('˕'); // lowered
         ipaDiacritics.add('̹'); // more rounded
         ipaDiacritics.add('̜'); // less rounded
         ipaDiacritics.add('ʷ'); // labialized
         ipaDiacritics.add('ʲ'); // palatalized
         ipaDiacritics.add('ᶣ'); // labio palatalized
         ipaDiacritics.add('ᶹ'); // Labialized without protrusion of the lips or velarization
         ipaDiacritics.add('ˠ'); // velarized
         ipaDiacritics.add('ˤ'); // Pharyngealized
         ipaDiacritics.add('̘'); // advanced tongue root
         ipaDiacritics.add('̙'); // retracted tongue root
         ipaDiacritics.add('̃'); // nasalized
         ipaDiacritics.add('ː'); // long
         ipaDiacritics.add('ˑ'); // half long
         ipaDiacritics.add('̆'); // extra short
      }
      return ipaDiacritics.contains(c);
   }

   private static HashSet<Character> ipaSuprasegmentals;
   /**
    * Determines whether the given character is a known suprasegmental in IPA.
    * <p>Suprasegmentals include:
    * <dl>
    * <dt> ˈ </dt><dd>primary stress</dd>
    * <dt> ˌ </dt><dd>secondary</dd>
    * <dt> ‿ </dt><dd>linking</dd>
    * <dt> . </dt><dd>syllable boundary</dd>
    * </dl>
    * @param c The character.
    * @return true if <var>c</var> is a diacritic, false otherwise
    */
   public static boolean IsIPASuprasegmental(char c) {
      if (ipaSuprasegmentals == null) {
         ipaSuprasegmentals = new HashSet<Character>();
         ipaSuprasegmentals.add('ˈ'); // primary stress
         ipaSuprasegmentals.add('ˌ'); // secondary stress
         ipaSuprasegmentals.add('‿'); // linking
         ipaSuprasegmentals.add('.'); // syllable
      }
      return ipaSuprasegmentals.contains(c);
   }
   
} // end of class DISC2HTK
