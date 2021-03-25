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
 * <tt>tr{nskrIpS@n</tt>
 * to IPA-encoded phonemic transcriptions using Unicode characters like
 * <tt>tɹænskɹɪpʃən</tt>.
 *
 * <style type="text/css"> tt:before { content: '/'; } tt:after { content: '/'; } </style>
 * @author Robert Fromont robert@fromont.net.nz
 */
public class DISC2IPA extends PhonemeTranslator {

   private static HashMap<Character,String> map;
   
   /**
    * Delimiter between phonemes, if any.
    * @see #getDelimiter()
    * @see #setDelimiter(String)
    */
   protected String delimiter;
   /**
    * Getter for {@link #delimiter}: Delimiter between phonemes, if any.
    * @return Delimiter between phonemes, if any.
    */
   public String getDelimiter() { return delimiter; }
   /**
    * Setter for {@link #delimiter}: Delimiter between phonemes, if any.
    * @param newDelimiter Delimiter between phonemes, if any.
    */
   public DISC2IPA setDelimiter(String newDelimiter) { delimiter = newDelimiter; return this; }
   
   /**
    * Default constructor.
    */
   public DISC2IPA() {
      sourceEncoding = "DISC";
      destinationEncoding = "IPA";
      
      // populate the static map of individual phones, if it's not already initialized...
      if (map == null) {
         map = new HashMap<Character,String>();
         
         map.put('&', "a");
         map.put('#', "ɑː");
         map.put('A', "ɑ");
         
         map.put('c', "æ̃");
         map.put('q', "ɑ̃ː");
         map.put('0', "æ̃ː");
         map.put('~', "ɒ̃ː");
         
         // linking R? etc.
         map.put('R', "ɹ");
         // x is now /x/ and C is /ç/
         
         // syllabics
         map.put('C', "ŋ̩");
         map.put('F', "m̩");
         map.put('H', "n̩");
         map.put('P', "l̩");
         
         //to be strictly correct
         map.put('r', "ɹ");
         
         map.put('u', "uː");
         map.put('i', "iː");
         map.put('y', "yː");
         map.put('o', "oː");
         map.put(')', "ɛː");
         map.put('|', "øː");
         map.put('^', "œ̃ː");
         map.put('/', "œ");      
         
         // diphthongs
         map.put('1', "eɪ");
         map.put('2', "aɪ");
         map.put('5', "əʊ");
         map.put('4', "ɔɪ");
         map.put('6', "aʊ");
         map.put('7', "ɪə");
         map.put('8', "ɛə");
         map.put('9', "ʊə");
         map.put('W', "ai");
         map.put('B', "au");
         map.put('X', "ɔy");
         
         // affricates
         map.put('_', "d͜ʒ");
         map.put('J', "t͜ʃ");
         
         map.put('E', "ɛ");
         
         map.put('=', "t͜s");
         
         // glottal stop
         map.put('?', "ʔ");
         // flap
         map.put('L', "œy");
         
         // consonants
         map.put('N', "ŋ");
         map.put('T', "θ");
         map.put('D', "ð");
         map.put('S', "ʃ");
         map.put('Z', "ʒ");
         map.put('+', "pf");
         
         // vowels
         map.put('3', "ɜː");
         map.put('{', "æ");
         map.put('}', "ʉ");
         map.put('$', "ɔː");
         map.put('I', "ɪ");
         map.put('Y', "ʏ");
         map.put('V', "ʌ");
         map.put('Q', "ɒ");
         map.put('O', "ɔ");
         map.put('U', "ʊ");
         map.put('@', "ə");
      }
   } // end of constructor
   
   /**
    * Translates a phonemic transcription from the source encoding to the destination encoding.
    * @param source Phonemic transcription in the source encoding.
    * @return Phonemic transcription in the destination encoding.
    */ 
   public String apply(String source) {
      StringBuffer IPA = new StringBuffer(source.length() * 3);
      // for each phone
      for (int c = 0; c < source.length(); c++) {
         if (delimiter != null && IPA.length() > 0) IPA.append(delimiter);
         if (map.containsKey(source.charAt(c))) {
            IPA.append(map.get(source.charAt(c)));
         } else { // unknown/unmapped phones are passed through
            IPA.append(source.charAt(c));
         }
      } // next phoneme
      return IPA.toString();
   }
   
} // end of class DISC2IPA
