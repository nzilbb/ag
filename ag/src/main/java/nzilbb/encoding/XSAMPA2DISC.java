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
import java.util.Vector;

/**
 * Translates X-SAMPA-encoded phonemic transcriptions like 
 * <tt>str\eIndZ</tt> 
 * to CELEX-DISC-encoded transcriptions like
 * <tt>str1n_</tt>. 
 *
 * <style type="text/css"> tt:before { content: '/'; } tt:after { content: '/'; } </style>
 * @see DISC2XSAMPA
 * @author Robert Fromont robert@fromont.net.nz
 */
public class XSAMPA2DISC extends PhonemeTranslator {

   private static HashMap<String,Character> map;
   
   /**
    * Default constructor.
    */
   public XSAMPA2DISC() {
      sourceEncoding = "XSAMPA";
      destinationEncoding = "DISC";
      
      // populate the static map of individual phones, if it's not already initialized...
      if (map == null) {
         map = new HashMap<String,Character>();
         
         // vowels
         map.put("i:",'i');
         map.put("A:",'#');
         map.put("O:",'$');
         map.put("u:",'u');
         map.put("3:",'3');
         map.put("eI",'1');
         map.put("aI",'2');
         map.put("OI",'4');
         map.put("@U",'5');
         map.put("aU",'6');
         map.put("I@",'7');
         map.put("E@",'8');
         map.put("U@",'9');
         map.put("{~",'c');
         map.put("A~:",'q');
         map.put("{~:",'0');
         map.put("O~:",'~');

         // vowels output by MAUSBasic
         map.put("@}",'5');
         map.put("Ae",'2');
         map.put("{Q",'6');
         map.put("{I",'1'); // duplicate of "eI"
         map.put("6",'@'); // 'open schwa' (ɐ)
         map.put("o:",'$');
         map.put("O", 'Q');
         map.put("4", 'L'); // flap - this is an extension to DISC
         
         // different from SAMPA
         map.put("}:",'}');
         
         // consonants
         map.put("tS",'J');
         map.put("dZ",'_');
         map.put("r*",'R');
	 
         // different from SAMPA
         map.put("r\\",'r');
         map.put("5",'l');
         map.put("N=",'C');
         map.put("m=",'F');
         map.put("n=",'H');
         map.put("l=",'P');

      }
   } // end of constructor
   
   /**
    * Translates a phonemic transcription from the source encoding to the destination encoding.
    * @param source Phonemic transcription in the source encoding.
    * @return Phonemic transcription in the destination encoding.
    */ 
   public String apply(String source) {
      StringBuffer DISC = new StringBuffer(source.length() / 2);
      Vector<String> candidates = new Vector<String>();
      // for each phone
      for (int c = 0; c < source.length(); c++)
      {
         candidates.clear();
         char nextChar = source.charAt(c);
         if (c < source.length() - 2) {
             candidates.add(""+nextChar+source.charAt(c+1)+source.charAt(c+2));
         }
         if (c < source.length() - 1) {
            candidates.add(""+nextChar+source.charAt(c+1));
         }
         candidates.add(""+nextChar);
         boolean bFound = false;
         for (String sTry : candidates) {
            if (map.containsKey(sTry)) {
               DISC.append(map.get(sTry));
               c += sTry.length() - 1; // consume the extra characters
               bFound = true;
               break;
            }
         }
         if (!bFound) { // unknown or unexceptional phone
           if (nextChar != ':' && nextChar != ' ') {
             // pass it through unchanged
             DISC.append(nextChar);
           }
         }
      }
      return DISC.toString();
   }
   
} // end of class XSAMPA2DISC
