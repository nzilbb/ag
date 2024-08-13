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

<style type="text/css">
 #mapping td:first-child, #mapping th:first-child { text-align: right; } 
 #mapping td:nth-child(2) { text-align: center; } 
 #mapping td:nth-child(3) { font-family: monospace; } 
 #mapping td:first-child { font-family: monospace; } 
</style>
<table id="mapping"><caption>Mapping</caption>
 <thead><tr>
  <th>ARPAbet</th><th></th><th>DISC</th><th>Example</th>
 </tr></thead>
 <tbody>

 <tr><td colspan="3"></td><th colspan="2">Vowels</th></tr>
 <tr><td>AA</td>  <td>→</td>  <td>#</td>   <td>START      </td> <td>odd/father</td></tr>
 <tr><td>AE</td>  <td>→</td>  <td>{</td>   <td>TRAP       </td> <td>at/fast</td></tr>
 <tr><td>AH</td>  <td>→</td>  <td>V</td>   <td>STRUT      </td> <td>hut/but</td></tr>
 <tr><td>AO</td>  <td>→</td>  <td>$</td>   <td>THOUGHT    </td> <td>ought/fall - one-to-two this could also be Q!</td></tr>
 <tr><td>AO</td>  <td>→</td>  <td>Q</td>   <td>LOT     </td> <td>ought/off</td></tr>
 <tr><td>AW</td>  <td>→</td>  <td>6</td>   <td>MOUTH      </td> <td>cow/how</td></tr>
 <tr><td>AY</td>  <td>→</td>  <td>2</td>   <td>PRICE      </td> <td>hide/my</td></tr>
 <tr><td>EH</td>  <td>→</td>  <td>E</td>   <td>DRESS      </td> <td>Ed/red</td></tr>
 <tr><td>ER</td>  <td>→</td>  <td>3</td>   <td>NURSE      </td> <td>hurt/her</td></tr>
 <tr><td>EY</td>  <td>→</td>  <td>1</td>   <td>FACE       </td> <td>ate/say</td></tr>
 <tr><td>IH</td>  <td>→</td>  <td>I</td>   <td>KIT        </td> <td>it/big</td></tr>
 <tr><td>IY</td>  <td>→</td>  <td>i</td>   <td>FLEECE     </td> <td>eat/bee</td></tr>
 <tr><td>OW</td>  <td>→</td>  <td>5</td>   <td>GOAT       </td> <td>oat/show</td></tr>
 <tr><td>OY</td>  <td>→</td>  <td>4</td>   <td>CHOICE     </td> <td>toy/boy</td></tr>
 <tr><td>UH</td>  <td>→</td>  <td>U</td>   <td>FOOT       </td> <td>hood/should</td></tr>
 <tr><td>UW</td>  <td>→</td>  <td>u</td>   <td>GOOSE      </td> <td>two/you</td></tr>

 <tr><td colspan="3"></td><th colspan="2">Consonants</th></tr>
 <tr><td>B</td>  <td>→</td>  <td>b</td></tr>
 <tr><td>CH</td>  <td>→</td>  <td>J</td></tr>
 <tr><td>D</td>  <td>→</td>  <td>d</td></tr>
 <tr><td>DH</td>  <td>→</td>  <td>D</td></tr>
 <tr><td>F</td>  <td>→</td>  <td>f</td></tr>
 <tr><td>G</td>  <td>→</td>  <td>g</td></tr>
 <tr><td>HH</td>  <td>→</td>  <td>h</td></tr>
 <tr><td>JH</td>  <td>→</td>  <td>_</td></tr>
 <tr><td>K</td>  <td>→</td>  <td>k</td></tr>
 <tr><td>L</td>  <td>→</td>  <td>l</td></tr>
 <tr><td>M</td>  <td>→</td>  <td>m</td></tr>
 <tr><td>N</td>  <td>→</td>  <td>n</td></tr>
 <tr><td>NG</td>  <td>→</td>  <td>N</td></tr>
 <tr><td>P</td>  <td>→</td>  <td>p</td></tr>
 <tr><td>R</td>  <td>→</td>  <td>r</td></tr>
 <tr><td>S</td>  <td>→</td>  <td>s</td></tr>
 <tr><td>SH</td>  <td>→</td>  <td>S</td></tr>
 <tr><td>T</td>  <td>→</td>  <td>t</td></tr>
 <tr><td>TH</td>  <td>→</td>  <td>T</td></tr>
 <tr><td>V</td>  <td>→</td>  <td>v</td></tr>
 <tr><td>W</td>  <td>→</td>  <td>w</td></tr>
 <tr><td>Y</td>  <td>→</td>  <td>j</td></tr>
 <tr><td>Z</td>  <td>→</td>  <td>z</td></tr>
 <tr><td>ZH</td>  <td>→</td>  <td>Z</td></tr>

 </tbody>
</table>

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
      map.put("AA",'#'); // START       - odd/father
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
