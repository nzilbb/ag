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

import java.util.HashMap;

/**
 * Translates CELEX-DISC-encoded transcriptions like
 * <tt>"tr{n-'skrIp-S@n</tt>
 * to <a href="http://www.cstr.ed.ac.uk/projects/unisyn/">Unisyn</a>-encoded 
 * phonemic transcriptions like
 * <tt>~&nbsp;t&nbsp;r&nbsp;a&nbsp;n&nbsp;.&nbsp;*&nbsp;s&nbsp;k&nbsp;r&nbsp;i&nbsp;p&nbsp;.&nbsp;sh&nbsp;@&nbsp;n</tt>.
 *
 * <p>This converts not only the phonemes, but also syllabification and stress markers:
 * <ul>
 *  <li><code>'</code> &rarr; <code>*</code> - primary stress</li>
 *  <li><code>"</code> &rarr; <code>~</code> - secondary stress</li>
 *  <li><code>,</code> &rarr; <code>-</code> - tertiary stress</li>
 *  <li><code>-</code> &rarr; <code>.</code> - syllable boundary</li>
 * </ul> 
 * @see Unisyn2DISC 
 * @author Robert Fromont robert@fromont.net.nz
 */
public class DISC2Unisyn extends PhonemeTranslator {
   
   private static HashMap<Character,String> map;
   
   /**
    * Default constructor.
    */
   public DISC2Unisyn() {
      sourceEncoding = "DISC";
      destinationEncoding = "Unisyn";
      
      // populate the static map of individual phones, if it's not already initialized...
      if (map == null) {
         map = new HashMap<Character,String>();
         
         // stress and syllabification
         map.put('\'', "*"); // primary stress
         map.put('"', "~"); // secondary stress
         map.put(',', "-"); // tertiary stress
         map.put('-', "."); // syllable boundary
         
         // vowels
         map.put('E', "e"); // DRESS
         map.put('{', "a"); // TRAP
         map.put('#', "ah"); // BATH
         map.put('5', "ou"); // GOAT - but a monophthong for edi
         map.put('Q', "o"); // LOT
         map.put('$', "oo"); // THOUGHT (but a diphthong in some en-US)
         map.put('i', "ii"); // FLEECE
         map.put('I', "i"); // KIT
         map.put('@', "@"); // schwa
         map.put('V', "uh"); // STRUT
         map.put('U', "u"); // FOOT
         map.put('u', "uu"); // GOOSE
         map.put('1', "ei"); // FACE
         map.put('2', "ai"); // PRICE
         map.put('4', "oi"); // CHOICE
         map.put('6', "ow"); // MOUTH
         map.put('7', "i@"); // NEAR
         map.put('3', "@@r"); // NURSE
         map.put('8', "eir"); // SQUARING (actually a monophthong in many)
         map.put('9', "ur"); // JURY
         
         // missing
         map.put('c', "o");  // LOT
         map.put('q', "o");  // LOT
         map.put('0', "o");  // LOT
         map.put('~', "o");  // LOT
         
         // consonants
         map.put('j', "y");
         map.put('J', "ch");
         map.put('_', "jh");
         map.put('S', "sh");
         map.put('Z', "zh");
         map.put('T', "th");
         map.put('D', "dh");
         map.put('L', "t^"); // butter/merry flap
         map.put('F', "m!"); // chasm
         map.put('H', "n!"); // mission
         map.put('N', "ng"); 
         map.put('P', "l!"); // cattle
         
         // missing
         map.put('C', "ng");
      }
   } // end of constructor
   
   /**
    * Translates a phonemic transcription from the source encoding to the destination encoding.
    * @param source Phonemic transcription in the source encoding.
    * @return Phonemic transcription in the destination encoding.
    */ 
   public String apply(String source) {
      StringBuffer unisyn = new StringBuffer(source.length() * 3);
      // for each phone
      for (int c = 0; c < source.length(); c++) {
         if (unisyn.length() > 0) unisyn.append(" ");
         if (map.containsKey(source.charAt(c))) {
            unisyn.append(map.get(source.charAt(c)));
         } else { // unknown phones are passed through
            unisyn.append(source.charAt(c));
         }
      } // next phoneme
      return unisyn.toString();
   }
   
} // end of class DISC2Unisyn
