//
// Copyright 2020-2021 New Zealand Institute of Language, Brain and Behaviour, 
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
 * Translates CMU-encoded phonemic transcriptions like 
 * <tt>T&nbsp;R&nbsp;AE2&nbsp;N&nbsp;S&nbsp;K&nbsp;R&nbsp;IH1&nbsp;P&nbsp;SH&nbsp;AH0&nbsp;N</tt> 
 * to CELEX-DISC-encoded transcriptions like
 * <tt>tr{nskrIpSVn</tt>. 
 * <p> The CMU encoding is assumed to use only the phonemes used by the CMU Pronouncing
 * Dictionary: 
 * <a href="http://www.speech.cs.cmu.edu/cgi-bin/cmudict">
 *  http://www.speech.cs.cmu.edu/cgi-bin/cmudict</a>.
 * <p> Thanks to Stefanie Jannedy for this mapping.
 *
 * <p> There are differences between the {@link DISC2ARPAbet} translation and this one,
 * primarily that this translation is strict; phonemes that are not explicitly present in
 * the phone set are dropped, where {@link DISC2ARPAbet} includes extra phonemes, includes
 * some extensions to ARPAbet and DISC, and passes through unknown phonemes unchanged.
 *
 * <style type="text/css"> tt:before { content: '/'; } tt:after { content: '/'; } </style>
 * @see DISC2CMU
 * @see DISC2ARPAbet
 * @author Robert Fromont robert@fromont.net.nz
 */
public class CMU2DISC extends PhonemeTranslator {
  
  private static HashMap<String,Character> map;
  
  /**
   * Translate zero-stress vowels as schwa. Default is true.
   * @see #getZeroStressToSchwa()
   * @see #setZeroStressToSchwa(boolean)
   */
  protected boolean zeroStressToSchwa = true;
  /**
   * Getter for {@link #zeroStressToSchwa}: Translate zero-stress vowels as schwa. Default is true.
   * @return Translate zero-stress vowels as schwa. Default is true.
   */
  public boolean getZeroStressToSchwa() { return zeroStressToSchwa; }
  /**
   * Setter for {@link #zeroStressToSchwa}: Translate zero-stress vowels as schwa. Default is true.
   * @param newZeroStressToSchwa Translate zero-stress vowels as schwa. Default is true.
   */
  public CMU2DISC setZeroStressToSchwa(boolean newZeroStressToSchwa) { zeroStressToSchwa = newZeroStressToSchwa; return this; }  
  
  /**
   * Default constructor.
   */
  public CMU2DISC() {
    sourceEncoding = "CMU";
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
      .toUpperCase() // all uppercase
      .replaceAll("["+(zeroStressToSchwa?"":"0")+"12]","") // ignore stress marking
      .split(" ");
    for (String phoneme : phonemes) {
      if (map.containsKey(phoneme)) {
        DISC.append(map.get(phoneme));
      } else if (phoneme.endsWith("0")) {
        DISC.append('@');
      }
      // unknown phones are dropped
    } // next phoneme
    return DISC.toString();
  }
   
} // end of class CMU2DISC
