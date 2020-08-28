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
 * Translates <a href="https://en.wikipedia.org/wiki/ARPABET">ARPAbet</a>-encoded phonemic
 * transcriptions like  
 * <tt>T&nbsp;R&nbsp;AE2&nbsp;N&nbsp;S&nbsp;K&nbsp;R&nbsp;IH1&nbsp;P&nbsp;SH&nbsp;AX0&nbsp;N</tt> 
 * to CELEX-DISC-encoded transcriptions like
 * <tt>tr{nskrIpS@n</tt>. 
 *
 * <p> There are differences between the {@link DISC2CMU} translation and this one:
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
 * @see DISC2ARPAbet
 * @author Robert Fromont robert@fromont.net.nz
 */
public class ARPAbet2DISC extends PhonemeTranslator {

   private static HashMap<String,Character> map;
   
   /**
    * Default constructor.
    */
   public ARPAbet2DISC() {
      sourceEncoding = "ARPAbet";
      destinationEncoding = "DISC";
      
      // populate the static map of individual phones, if it's not already initialized...
      if (map == null) {
         map = new HashMap<String,Character>();
         
         // vowel e.g.s marked with  // LEXICAL SET - cmu/arpabet
         map.put("AA",'#'); // BATH        - odd/father
         map.put("AE",'{'); // TRAP        - at/fast
         
         // this is also used in words where it should be schwa!
         map.put("AH",'V'); // STRUT       - hut/but
         
         // one-to-two - this could also be Q!
         map.put("AO",'$'); // THOUGHT     - ought/fall
         // map.put("AO",'Q'); // LOT      - ought/off
         
         map.put("AW",'6'); // MOUTH       - cow/how
         map.put("AX",'@'); // schwa       - discuss
         map.put("AY",'2'); // PRICE       - hide/my
         map.put("B",'b');
         map.put("CH",'J');
         map.put("D",'d');
         map.put("DH",'D');
         map.put("EH",'E'); // DRESS       - Ed/red
         map.put("ER",'3'); // NURSE       - hurt/her
         map.put("EY",'1'); // FACE        - ate/say
         map.put("F",'f');
         map.put("G",'g');
         map.put("HH",'h');
         map.put("IH",'I'); // KIT         - it/big
         map.put("IY",'i'); // FLEECE      - eat/bee
         map.put("JH",'_');
         map.put("K",'k');
         map.put("L",'l');
         map.put("M",'m');
         map.put("N",'n');
         map.put("NG",'N');
         map.put("OW",'5'); // GOAT        - oat/show
         map.put("OY",'4'); // CHOICE      - toy/boy
         map.put("P",'p');
         map.put("R",'r');
         // no mapping R -> possible linking R
         map.put("S",'s');
         map.put("SH",'S');
         map.put("T",'t');
         map.put("TH",'T');
         map.put("UH",'U'); // FOOT        - hood/should
         map.put("UW",'u'); // GOOSE       - two/you
         map.put("V",'v');
         map.put("W",'w');
         map.put("Y",'j');
         map.put("Z",'z');
         map.put("ZH",'Z');
	 
         // Not in the CMU set but exist in Buckeye corpus
         map.put("DX",'L'); // flap - this is an extension to DISC
         map.put("NX",'^'); // nasal flap - doesn't exist in DISC, we make it /n/
         map.put("TQ",'?'); // glottal stop - this is an extension to DISC
	 
         // Syllabics not in CMU set but exist in DISC
         map.put("EM",'F'); // idealism
         map.put("EN",'H'); // burden
         map.put("EL",'P'); // dangle
         map.put("UN",'C'); // bacon 
         map.put("VN",'0'); // lingerie
         map.put("ON",'~'); // bouillon
         map.put("IM",'c'); // timbre
         map.put("IN",'q'); // detente
      }
   } // end of constructor

   /**
    * Translates a phonemic transcription from the source encoding to the destination encoding.
    * @param source Phonemic transcription in the source encoding.
    * @return Phonemic transcription in the destination encoding.
    */ 
   public String apply(String source) {
      StringBuffer DISC = new StringBuffer(source.length() / 2);
      // for each phone
      String[] phonemes = source         
         .replaceAll("[012]","") // ignore stress marking
         .split(" ");
      for (String phoneme : phonemes) {
         if (map.containsKey(phoneme.toUpperCase())) {
            DISC.append(map.get(phoneme.toUpperCase()));
         } else {// unknown phones are passed through
            DISC.append(phoneme);
         }
      } // next phoneme
      return DISC.toString();
   }
   
} // end of class ARPAbet2DISC
