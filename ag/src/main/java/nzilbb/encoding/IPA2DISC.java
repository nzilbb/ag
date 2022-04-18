//
// Copyright 2022 New Zealand Institute of Language, Brain and Behaviour, 
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
 * Translates IPA-encoded phonemic transcriptions using Unicode characters like
 * <tt>tɹænskɹɪpʃən</tt>
 * to CELEX-DISC-encoded transcriptions like
 * <tt>tr{nskrIpS@n</tt>
 *
 * <p> This should support straight round-trip conversion with IPA symbols used by
 * {@link DISC2IPA}, but also recognise other IPA symbols like /ʤ/ for conversion to DISC.
 *
 * <style type="text/css"> tt:before { content: '/'; } tt:after { content: '/'; } </style>
 * @see DISC2IPA
 * @author Robert Fromont robert@fromont.net.nz
 */
public class IPA2DISC extends PhonemeTranslator {

  private static HashMap<String,Character> map;
   
  /**
   * Delimiter between IPA phonemes, if any.
   * @see #getDelimiter()
   * @see #setDelimiter(String)
   */
  protected String delimiter;
  /**
   * Getter for {@link #delimiter}: Delimiter between IPA phonemes, if any.
   * @return Delimiter between IPA phonemes, if any.
   */
  public String getDelimiter() { return delimiter; }
  /**
   * Setter for {@link #delimiter}: Delimiter between IPA phonemes, if any.
   * @param newDelimiter Delimiter between IPA phonemes, if any.
   */
  public IPA2DISC setDelimiter(String newDelimiter) { delimiter = newDelimiter; return this; }
   
  /**
   * Default constructor.
   */
  public IPA2DISC() {
    sourceEncoding = "IPA";
    destinationEncoding = "DISC";
      
    // populate the static map of individual phones, if it's not already initialized...
    if (map == null) {
      map = new HashMap<String,Character>();

      // reverse mappings from DISC2IPA ...
         
      map.put("a", '&');
      map.put("ɑː", '#');
      map.put("ɑ", 'A');
      
      map.put("æ̃", 'c');
      map.put("ɑ̃ː", 'q');
      map.put("æ̃ː", '0');
      map.put("ɒ̃ː", '~');
         
      // x is now /x/ and C is /ç/
         
      // syllabics
      map.put("ŋ̩", 'C');
      map.put("m̩", 'F');
      map.put("n̩", 'H');
      map.put("l̩", 'P');
         
      //to be strictly correct
      map.put("ɹ", 'r');
      map.put("ɡ", 'g'); // this is LATIN SMALL LETTER SCRIPT G which is always straight-backed
         
      map.put("uː", 'u');
      map.put("iː", 'i');
      map.put("yː", 'y');
      map.put("oː", 'o');
      map.put("ɛː", ')');
      map.put("øː", '|');
      map.put("œ̃ː", '^');
      map.put("œ", '/');      
         
      // diphthongs
      map.put("eɪ", '1');
      map.put("aɪ", '2');
      map.put("əʊ", '5');
      map.put("ɔɪ", '4');
      map.put("aʊ", '6');
      map.put("ɪə", '7');
      map.put("ɛə", '8');
      map.put("ʊə", '9');
      map.put("ai", 'W');
      map.put("au", 'B');
      map.put("ɔy", 'X');
         
      // affricates
      map.put("d͜ʒ", '_');
      map.put("t͜ʃ", 'J');
         
      map.put("ɛ", 'E');
         
      map.put("t͜s", '=');
         
      // glottal stop
      map.put("ʔ", '?');
      // flap
      map.put("œy", 'L');
         
      // consonants
      map.put("ŋ", 'N');
      map.put("θ", 'T');
      map.put("ð", 'D');
      map.put("ʃ", 'S');
      map.put("ʒ", 'Z');
      map.put("pf", '+');
         
      // vowels
      map.put("ɜː", '3');
      map.put("æ", '{');
      map.put("ʉ", '}');
      map.put("ɔː", '$');
      map.put("ɪ", 'I');
      map.put("ʏ", 'Y');
      map.put("ʌ", 'V');
      map.put("ɒ", 'Q');
      map.put("ɔ", 'O');
      map.put("ʊ", 'U');
      map.put("ə", '@');

      // other mappings...

      // afficate ligatures
      map.put("ʤ", '_');
      map.put("ʧ", 'J');
      // and over-joined
      map.put("d͡ʒ", '_');
      map.put("t͡ʃ", 'J');
         
      // LATIN SMALL LETTER TURNED E instead of schwa
      map.put("ǝ", '@');
      map.put("ǝʊ", '5');
      map.put("ɪǝ", '7');
      map.put("ɛǝ", '8');
      map.put("ʊǝ", '9');

      // TODO MFA characters         
    }
  } // end of constructor
  
  /**
   * Translates a phonemic transcription from the source encoding to the destination encoding.
   * @param source Phonemic transcription in the source encoding.
   * @return Phonemic transcription in the destination encoding.
   */ 
  public String apply(String source) {
    StringBuilder DISC = new StringBuilder();
    if (delimiter != null) {
      String[] phonemes = source.split(delimiter);
      for (String phoneme : phonemes) {
        if (map.containsKey(phoneme)) {
          DISC.append(map.get(phoneme));
        } else { // unknown/unmapped phones are passed through
          DISC.append(phoneme);
        }
      } // next phoneme
    } else { // no delimiter
      StringBuilder whatsLeft = new StringBuilder(source);

      // nibble off characters from the start of the string until there are none left

      while (whatsLeft.length() > 0) {
        // look for known multi-character phonemes (longest multi-char phoneme is 3 characters)
        boolean foundMultiCharPhoneme = false;
        if (whatsLeft.length() > 1) {
          String prefix = whatsLeft.substring(0, Math.min(3, whatsLeft.length()));
          for (String key : map.keySet()) {
            if (key.length() > 1) {
              if (prefix.startsWith(key)) {
                DISC.append(map.get(whatsLeft.substring(0, key.length())));
                whatsLeft.delete(0, key.length());
                foundMultiCharPhoneme = true;
                break;
              } // found multi-character phoneme prefix
            } // multi-character phoneme
          } // next possible phoneme
        } // there could be multi-character phoneme at the beginning
        
        if (!foundMultiCharPhoneme) {
          // otherwise, just assume the first character is the phoneme
          
          String firstCharacter = ""+whatsLeft.charAt(0);
          if (map.containsKey(firstCharacter)) {
            DISC.append(map.get(firstCharacter));
          } else { // unknown/unmapped phones are passed through
            DISC.append(firstCharacter);
          }
          whatsLeft.deleteCharAt(0);
        }
      } // next phoneme
    } // no delimiter
    return DISC.toString();
  }
   
} // end of class IPA2DISC
