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
 * Translates <a href="http://en.wikipedia.org/wiki/Kirshenbaum">Kirshenbaum</a>-encoded 
 * phonemic transcriptions like 
 * <tt>tr&amp;nskrIpS@n</tt> 
 * to CELEX-DISC-encoded transcriptions like
 * <tt>tr{nskrIpS@n</tt>. 
 * @see DISC2SAMPA
 * @author Robert Fromont robert@fromont.net.nz
 */
public class Kirshenbaum2DISC extends PhonemeTranslator {

   private static HashMap<String,Character> map;
   
   /**
    * Default constructor.
    */
   public Kirshenbaum2DISC() {
      sourceEncoding = "Kirshenbaum";
      destinationEncoding = "DISC";
      
      // populate the static map of individual phones, if it's not already initialized...
      if (map == null) {
         map = new HashMap<String,Character>();
         
         // vowels
         map.put("&",'{');
         map.put("i:",'i');
         map.put("A:",'#');
         map.put("A.",'Q');
         map.put("O:",'$');
         map.put("u:",'u');
         map.put("V\"",'3');
         
         map.put("eI",'1');
         map.put("aI",'2');
         map.put("OI",'4');
         map.put("@U",'5');
         map.put("aU",'6');
         map.put("I@",'7');
         map.put("E@",'8');
         map.put("U@",'9');
         map.put("&~",'c');
         map.put("A~:",'q');
         map.put("&~:",'0');
         map.put("A.~:",'~');
         
         // consonants
         map.put("tS",'J');
         map.put("dZ",'_');
         map.put("r*",'R');
         map.put("N-",'C');
         map.put("m-",'F');
         map.put("n-",'H');
         map.put("l-",'P');
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
         if (c < source.length() - 3) {
             candidates.add(
                ""+source.charAt(c)+source.charAt(c+1)+source.charAt(c+2)+source.charAt(c+3));
         }
         if (c < source.length() - 2) {
             candidates.add(""+source.charAt(c)+source.charAt(c+1)+source.charAt(c+2));
         }
         if (c < source.length() - 1) {
            candidates.add(""+source.charAt(c)+source.charAt(c+1));
         }
         candidates.add(""+source.charAt(c));
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
            // pass it through unchanged
            DISC.append(source.charAt(c));
         }
      }
      return DISC.toString();
   }
   
} // end of class Kirshenbaum2DISC
