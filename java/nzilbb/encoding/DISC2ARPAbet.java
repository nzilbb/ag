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
 * <tt>tr{nskrIpSVn</tt>
 * to <a href="https://en.wikipedia.org/wiki/ARPABET">ARPAbet</a>-encoded 
 * phonemic transcriptions like
 * <tt>T&nbsp;R&nbsp;AE&nbsp;N&nbsp;S&nbsp;K&nbsp;R&nbsp;IH&nbsp;P&nbsp;SH&nbsp;AX&nbsp;N</tt>.
 *
 * <p> There are differences between the {@link CMU2DISC} translation and this one:
 * <p> ARPAbet includes phonemes not in the CMU set:
 * <dl>
 * <dt> 'L' </dt><dd> "DX" - flap - this is an extension to DISC </dd>
 * <dt> '^' </dt><dd> "NX" - nasal flap - doesn't exist in DISC, we make it /n/ </dd>
 * <dt> '?' </dt><dd> "TQ" - glottal stop - this is an extension to DISC </dd>
 * </dl>
 *
 * <p> Also {@link CMU2DISC} strictly uses only phonemes in the CMU dictionary set, where
 * the "ARPAbet" translation may also contain phonemes corresponding to those that exist
 * in DISC but not in ARPAbet: 
 * <dl>
 * <dt> 'F' </dt><dd> "EM" - e.g. idealism </dd>
 * <dt> 'H' </dt><dd> "EN" - e.g. burden </dd>
 * <dt> 'P' </dt><dd> "EL" - e.g. dangle </dd>
 * <dt> 'C' </dt><dd> "UN" - e.g. bacon </dd>
 * <dt> '0' </dt><dd> "VN" - e.g. lingerie </dd>
 * <dt> '~' </dt><dd> "ON" - e.g. bouillon </dd>
 * <dt> 'c' </dt><dd> "IM" - e.g. timbre </dd>
 * <dt> 'q' </dt><dd> "IN" - e.g. detente </dd>
 * </dl> 
 * <p> ... and any other phones encountered that are in neither set are passed through
 * unchanged.
 *
 * <style type="text/css"> tt:before { content: '/'; } tt:after { content: '/'; } </style>
 * @see ARPAbet2DISC 
 * @see CMU2DISC
 * @author Robert Fromont robert@fromont.net.nz
 */
public class DISC2ARPAbet extends PhonemeTranslator {
   
   private static HashMap<Character,String> map;
   
   /**
    * Default constructor.
    */
   public DISC2ARPAbet() {
      sourceEncoding = "DISC";
      destinationEncoding = "ARPAbet";
      
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
         map.put('@',"AX"); // schwa       - discuss
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
         map.put('L',"DX"); // flap - this is an extension to DISC
         map.put('^',"NX"); // nasal flap - doesn't exist in DISC, we make it /n/
         map.put('?',"TQ"); // glottal stop - this is an extension to DISC
	 
         // Not in CMU set but exist in DISC
         map.put('7',"IY R"); // NEAR
         map.put('8',"EH R"); // SQUARE
         map.put('9',"UH R"); // CURE
         map.put('F',"EM"); // idealism
         map.put('H',"EN"); // burden
         map.put('P',"EL"); // dangle
         map.put('C',"UN"); // bacon
         map.put('0',"VN"); // lingerie
         map.put('~',"ON"); // bouillon
         map.put('c',"IM"); // timbre
         map.put('q',"IN"); // detente
      }
   } // end of constructor
   
   /**
    * Translates a phonemic transcription from the source encoding to the destination encoding.
    * @param source Phonemic transcription in the source encoding.
    * @return Phonemic transcription in the destination encoding.
    */ 
   public String apply(String source) {
      StringBuffer ARPAbet = new StringBuffer(source.length() * 3);
      // for each phone
      for (int c = 0; c < source.length(); c++) {
         if (ARPAbet.length() > 0) ARPAbet.append(" ");
         if (map.containsKey(source.charAt(c))) {
            ARPAbet.append(map.get(source.charAt(c)));
         } else { // unknown phones are passed through
            ARPAbet.append(source.charAt(c));
         }
      } // next phoneme
      return ARPAbet.toString()
         // get rid of doubled R's caused by r-coloured vowels followed by possible linking r
         .replaceAll("R R", "R");
   }
   
} // end of class DISC2ARPAbet
