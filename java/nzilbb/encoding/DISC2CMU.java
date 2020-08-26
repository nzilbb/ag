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
 * to CMU-encoded phonemic transcriptions like
 * <tt>T&nbsp;R&nbsp;AE2&nbsp;N&nbsp;S&nbsp;K&nbsp;R&nbsp;IH1&nbsp;P&nbsp;SH&nbsp;AH0&nbsp;N</tt>.
 * <p> The CMU encoding is assumed to use only the phonemes used by the CMU Pronouncing
 * Dictionary: 
 * <a href="http://www.speech.cs.cmu.edu/cgi-bin/cmudict">
 *  http://www.speech.cs.cmu.edu/cgi-bin/cmudict</a>.
 * <p> Thanks to Stefanie Jannedy for this mapping.
 * @see CMU2DISC
 * @author Robert Fromont robert@fromont.net.nz
 */
public class DISC2CMU extends PhonemeTranslator {

   private static HashMap<Character,String> map;
   
   /**
    * Default constructor.
    */
   public DISC2CMU() {
      sourceEncoding = "DISC";
      destinationEncoding = "CMU";
      
      // populate the static map of individual phones, if it's not already initialized...
      if (map == null) {
         map = new HashMap<Character,String>();
         
         // vowel e.g.s marked with  // LEXICAL SET - cmu/arpabet
         map.put('#',"AA"); // BATH        - odd/father
         map.put('{',"AE"); // TRAP        - at/fast
         map.put('V',"AH"); // STRUT       - hut/but
         
         // two-to-one!
         map.put('$',"AO"); // THOUGHT     - ought/fall
         map.put('Q',"AO"); // LOT         - ought/off
         
         map.put('6',"AW"); // MOUTH       - cow/how
         map.put('@',"IH"); // schwa       - discuss - doesn't exist in CMU
         map.put('2',"AY"); // PRICE       - hide/my
         map.put('b',"B");
         map.put('J',"CH");
         map.put('d',"D");
         map.put('D',"DH");
         map.put('E',"EH"); // DRESS       - Ed/red
         map.put('3',"ER"); // NURSE       - hurt/her
         map.put('1',"EY"); // FACE        - ate/say
         map.put('f',"F"); 
         map.put('g',"G");
         map.put('h',"HH");
         map.put('I',"IH"); // KIT         - it/big
         map.put('i',"IY"); // FLEECE      - eat/bee
         map.put('_',"JH");
         map.put('k',"K");
         map.put('l',"L");
         map.put('m',"M");
         map.put('n',"N");
         map.put('N',"NG");
         map.put('5',"OW"); // GOAT        - oat/show
         map.put('4',"OY"); // CHOICE      - toy/boy
         map.put('p',"P");
         map.put('r',"R");
         map.put('R',"R");  // possible linking R is pretty definitely R
         map.put('s',"S");
         map.put('S',"SH");
         map.put('t',"T");
         map.put('T',"TH");
         map.put('U',"UH"); // FOOT        - hood/should
         map.put('u',"UW"); // GOOSE       - two/you
         map.put('v',"V");
         map.put('w',"W");
         map.put('j',"Y");
         map.put('z',"Z");
         map.put('Z',"ZH");
	 
         // Not in the CMU set but exist in Buckeye corpus
         map.put('L',"D"); // flap - this is an extension to DISC
         //map.put('n',"NX"); // nasal flap - doesn't exist in DISC, we make it /n/
         map.put('?',"K"); // glottal stop - this is an extension to DISC
	 
         // Not in CMU set but exist in DISC
         map.put('7',"IY R"); // NEAR
         map.put('8',"EH R"); // SQUARE
         map.put('9',"UH R"); // CURE
         map.put('F',"IH M"); // idealism
         map.put('H',"IH N"); // burden
         map.put('P',"IH L"); // dangle
         map.put('C',"IH NG"); // bacon
         map.put('0',"AO N"); // lingerie
         map.put('~',"AO N"); // bouillon
         map.put('c',"AO M"); // timbre
         map.put('q',"AO N"); // detente
      }
   } // end of constructor
   
   /**
    * Translates a phonemic transcription from the source encoding to the destination encoding.
    * @param source Phonemic transcription in the source encoding.
    * @return Phonemic transcription in the destination encoding.
    */ 
   public String apply(String source) {
      StringBuffer CMU = new StringBuffer(source.length() * 3);
      // for each phone
      for (int c = 0; c < source.length(); c++) {
         if (map.containsKey(source.charAt(c))) {
            if (CMU.length() > 0) CMU.append(" ");
            CMU.append(map.get(source.charAt(c)));
         }
         // unknown phones are dropped
      } // next phoneme
      return CMU.toString()
         // get rid of doubled R's caused by r-coloured vowels followed by possible linking r
         .replaceAll("R R", "R");
   }
   
} // end of class DISC2CMU
