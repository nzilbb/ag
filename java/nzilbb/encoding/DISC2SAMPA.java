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
 * Translates CELEX-DISC-encodedtranscriptions like
 * <tt>tr{nskrIpSVn</tt>
 * to SAMPA-encoded phonemic transcriptions like
 * <tt>T&nbsp;R&nbsp;AE2&nbsp;N&nbsp;S&nbsp;K&nbsp;R&nbsp;IH1&nbsp;P&nbsp;SH&nbsp;AH0&nbsp;N</tt>.
 * <p> The SAMPA encoding is assumed to use only the phonemes used by the SAMPA Pronouncing
 * Dictionary: 
 * <a href="http://www.speech.cs.cmu.edu/cgi-bin/cmudict">
 *  http://www.speech.cs.cmu.edu/cgi-bin/cmudict</a>.
 *
 * <p> Thanks to Stefanie Jannedy for this mapping.
 *
 * <p> There are differences between the {@link ARPAbet2DISC} translation and this one,
 * primarily that this translation is strict; phonemes that are not explicitly present in
 * the phone set are dropped, where {@link ARPAbet2DISC} includes extra phonemes, includes
 * some extensions to ARPAbet and DISC, and passes through unknown phonemes unchanged.
 * @see SAMPA2DISC
 * @see DISC2ARPAbet
 * @author Robert Fromont robert@fromont.net.nz
 */
public class DISC2SAMPA extends PhonemeTranslator {

   private static HashMap<Character,String> map;
   
   /**
    * Default constructor.
    */
   public DISC2SAMPA() {
      sourceEncoding = "DISC";
      destinationEncoding = "SAMPA";
      
      // populate the static map of individual phones, if it's not already initialized...
      if (map == null) {
         map = new HashMap<Character,String>();
         
         // vowels
         map.put('i',"i:");
         map.put('#',"A:");
         map.put('$',"O:");
         map.put('u',"u:");
         map.put('3',"3:");
         map.put('1',"eI");
         map.put('2',"aI");
         map.put('4',"OI");
         map.put('5',"@U");
         map.put('6',"aU");
         map.put('7',"I@");
         map.put('8',"E@");
         map.put('9',"U@");
         map.put('c',"{~");
         map.put('q',"A~:");
         map.put('0',"{~:");
         map.put('~',"O~:");
         
         // consonants
         map.put('J',"tS");
         map.put('_',"dZ");
         map.put('C',"N,");
         map.put('F',"m,");
         map.put('H',"n,");
         map.put('P',"l,");
         map.put('R',"r*");
      }
   } // end of constructor
   
   /**
    * Translates a phonemic transcription from the source encoding to the destination encoding.
    * @param source Phonemic transcription in the source encoding.
    * @return Phonemic transcription in the destination encoding.
    */ 
   public String apply(String source) {
      StringBuffer SAMPA = new StringBuffer(source.length() * 3);
      // for each phone
      for (int c = 0; c < source.length(); c++) {
         if (map.containsKey(source.charAt(c))) {
            SAMPA.append(map.get(source.charAt(c)));
         } else { // unknown or unexceptional phones are passed through
            SAMPA.append(source.charAt(c));
         }
      } // next phoneme
      return SAMPA.toString();
   }
   
} // end of class DISC2SAMPA
