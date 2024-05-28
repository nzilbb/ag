//
// Copyright 2022-2024 New Zealand Institute of Language, Brain and Behaviour, 
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
 * {@link DISC2IPA}, but also recognise other IPA symbols like /ʤ/, and 
 * Montreal Forced Aligner symbols like /ej/, for conversion to DISC.

<h3 id="mapping">Mapping</h3>

<div style="display: flex; flex-direction: row; align-items: baseline;">
<style type="text/css">
 #equiv td:first-child, #equiv th:first-child { text-align: right; } 
 #equiv td:nth-child(2) { text-align: center; } 
 #equiv td:nth-child(3) { font-family: monospace; } 
 #equiv td:first-child { font-family: monospace; } 
</style>
<table id="equiv"><caption>1-to-1 mappings</caption>
 <thead><tr>
  <th>IPA</th><th></th><th>DISC</th><th>Example</th>
 </tr></thead>
 <tbody>
 
 <tr><td>a</td>  <td>↔</td>  <td>&amp;</td> <td> h<b>a</b>t</td><td>(German)</td></tr>
 
 <tr><td colspan="3"></td><th colspan="2">Nasals</th></tr>        
 <tr><td>æ̃</td>  <td>↔</td>  <td>c</td>     <td>t<b>i</b>mbre</td></tr>
 <tr><td>ɑ̃ː</td>  <td>↔</td>  <td>q</td>    <td>dét<b>en</b>te</td></tr>
 <tr><td>æ̃ː</td>  <td>↔</td>  <td>0</td>    <td>l<b>in</b>gerie</td></tr>
 <tr><td>ɒ̃ː</td>  <td>↔</td>  <td>~</td>     <td>bouill<b>on</b></td></tr>
  
 <tr><td colspan="3"></td><th colspan="2">Possible linking r</th></tr>
 <tr><td>ɹ</td>  <td>↔</td>  <td>R</td>      <td>fathe<b>r</b></td></tr>
 
 <tr><td colspan="3"></td><th colspan="2">Syllabics</th></tr>
 <tr><td>ŋ̩</td>  <td>↔</td>  <td>C</td>     <td>baco<b>n</b></td></tr>
 <tr><td>m̩</td>  <td>↔</td>  <td>F</td>     <td>idealis<b>m</b></td></tr>
 <tr><td>n̩</td>  <td>↔</td>  <td>H</td>     <td>burde<b>n</b></td></tr>
 <tr><td>l̩</td>  <td>↔</td>  <td>P</td>     <td>dang<b>l</b>e</td></tr>
         
 <tr><td>ɹ</td>  <td>↔</td>  <td>r</td>     <td><b>r</b>at</td></tr>
 <tr><td>ɡ</td>  <td>↔</td>  <td>g</td>     <td><b>g</b>ame</td><td>(this is LATIN SMALL LETTER SCRIPT G which is always straight-backed)</td></tr>
         
 <tr><td colspan="3"></td><th colspan="2">Monophthongs</th></tr>
 <tr><td>ɜː</td>  <td>↔</td>  <td>3</td>     <td>N<b>UR</b>SE</td></tr>
 <tr><td>æ</td>  <td>↔</td>  <td>{</td>     <td>TR<b>A</b>P</td></tr>
 <tr><td>ʉ</td>  <td>↔</td>  <td>}</td>     <td>put</td><td>(Dutch)</td></tr>
 <tr><td>ɔː</td>  <td>↔</td>  <td>$</td>     <td>TH<b>OU</b>GHT</td></tr>
 <tr><td>ɪ</td>  <td>↔</td>  <td>I</td>     <td>K<b>I</b>T</td></tr>
 <tr><td>ʏ</td>  <td>↔</td>  <td>Y</td>     <td>Pf<b>ü</b>tze</td></tr>
 <tr><td>ʌ</td>  <td>↔</td>  <td>V</td>     <td>STR<b>U</b>T</td></tr>
 <tr><td>ɒ</td>  <td>↔</td>  <td>Q</td>     <td>L<b>O</b>T</td></tr>
 <tr><td>ɔ</td>  <td>↔</td>  <td>O</td>     <td>Gl<b>o</b>cke</td><td>(German)</td></tr>
 <tr><td>ʊ</td>  <td>↔</td>  <td>U</td>     <td>F<b>OO</b>T</td></tr>
 <tr><td>ə</td>  <td>↔</td>  <td>@</td>     <td><b>a</b>nother</td> <td>Schwa</td></tr>
 <tr><td>uː</td>  <td>↔</td>  <td>u</td>     <td>G<b>OO</b>SE</td></tr>
 <tr><td>iː</td>  <td>↔</td>  <td>i</td>     <td>FL<b>EE</b>CE</td></tr>
 <tr><td>ɛ</td>  <td>↔</td>  <td>E</td>     <td>DR<b>E</b>SS</td></tr>
 <tr><td>ɑː</td>  <td>↔</td>  <td>#</td>     <td>B<b>A</b>TH</td></tr>
 <tr><td>ɑ</td>  <td>↔</td>  <td>A</td>     <td>k<b>a</b>levala</td><td>(German)</td></tr>
 <tr><td>yː</td>  <td>↔</td>  <td>y</td>     <td>f<b>ü</b>r</td><td>(German)</td></tr>
 <tr><td>oː</td>  <td>↔</td>  <td>o</td>     <td>b<b>oo</b>t</td><td>(German)</td></tr>
 <tr><td>ɛː</td>  <td>↔</td>  <td>)</td>     <td>K<b>ä</b>se</td><td>(German)</td></tr>
 <tr><td>øː</td>  <td>↔</td>  <td>|</td>     <td>M<b>ö</b>bel</td><td>(German)</td></tr>
 <tr><td>œ̃ː</td>  <td>↔</td>  <td>^</td>     <td>Parf<b>u</b>m</td><td>(German)</td></tr>
 <tr><td>œ</td>  <td>↔</td>  <td>/</td>     <td>G<b>ö</b>tter</td><td>(German)</td></tr>      
 
 <tr><td colspan="3"></td><th colspan="2">Diphthongs</th></tr>
 <tr><td>eɪ</td>  <td>↔</td>  <td>1</td>     <td>F<b>A</b>CE</td></tr>
 <tr><td>aɪ</td>  <td>↔</td>  <td>2</td>     <td>PR<b>I</b>CE</td></tr>
 <tr><td>ɔɪ</td>  <td>↔</td>  <td>4</td>     <td>CH<b>OI</b>CE</td></tr>
 <tr><td>əʊ</td>  <td>↔</td>  <td>5</td>     <td>G<b>OA</b>T</td></tr>
 <tr><td>aʊ</td>  <td>↔</td>  <td>6</td>     <td>M<b>OU</b>TH</td></tr>
 <tr><td>ɪə</td>  <td>↔</td>  <td>7</td>     <td>N<b>EAR</b></td></tr>
 <tr><td>ɛə</td>  <td>↔</td>  <td>8</td>     <td>SQU<b>AR</b>E</td></tr>
 <tr><td>ʊə</td>  <td>↔</td>  <td>9</td>     <td>C<b>UR</b>E</td></tr>
 <tr><td>ai</td>  <td>↔</td>  <td>W</td>     <td>w<b>ei</b>t</td><td>(German)</td></tr>
 <tr><td>au</td>  <td>↔</td>  <td>B</td>     <td>H<b>au</b>t</td><td>(German)</td></tr>
 <tr><td>ɔy</td>  <td>↔</td>  <td>X</td>     <td>fr<b>eu</b>t</td><td>(German)</td></tr>
 <tr><td>œy</td>  <td>↔</td>  <td>L</td>     <td>h<b>ui</b>s</td><td>(Dutch)</td></tr>
         
 <tr><td colspan="3"></td><th colspan="2">Consonants</th></tr>
 <tr><td>ŋ</td>  <td>↔</td>  <td>N</td>     <td>ba<b>ng</b></td></tr>
 <tr><td>θ</td>  <td>↔</td>  <td>T</td>     <td><b>th</b>in</td></tr>
 <tr><td>ð</td>  <td>↔</td>  <td>D</td>     <td><b>th</b>en</td></tr>
 <tr><td>ʃ</td>  <td>↔</td>  <td>S</td>     <td><b>sh</b>eep</td></tr>
 <tr><td>ʒ</td>  <td>↔</td>  <td>Z</td>     <td>mea<b>s</b>ure</td></tr>
 <tr><td>pf</td>  <td>↔</td>  <td>+</td>    <td><b>Pf</b>erd</td><td>(German)</td></tr>
 
 <tr><td colspan="3"></td><th colspan="2">Affricates</th></tr>
 <tr><td>d͜ʒ</td>  <td>↔</td>  <td>_</td>   <td><b>j</b>eep</td></tr>
 <tr><td>t͜ʃ</td>  <td>↔</td>  <td>J</td>    <td><b>ch</b>eap</td></tr>
 <tr><td>t͜s</td>  <td>↔</td>  <td>=</td>   <td><b>Z</b>ahl</td><td>(German)</td></tr>
         
 <tr><td colspan="3"></td><th colspan="2">Extensions to DISC, not in CELEX</th></tr>
 <tr><td>ʔ</td>  <td>↔</td>  <td>?</td>     <td>uh<b>-</b>oh</td><td>Glottal Stop</td></tr>

 <tr><td colspan="3"></td><th colspan="2">Any other symbol is passed through unchanged, including:</th></tr>

 <tr><td>p</td>  <td>↔</td>  <td>p</td>     <td><b>p</b>at</td></tr>
 <tr><td>b</td>  <td>↔</td>  <td>b</td>     <td><b>b</b>ad</td></tr>
 <tr><td>t</td>  <td>↔</td>  <td>t</td>     <td><b>t</b>ack</td></tr>
 <tr><td>d</td>  <td>↔</td>  <td>d</td>     <td><b>d</b>ad</td></tr>
 <tr><td>k</td>  <td>↔</td>  <td>k</td>     <td><b>c</b>ad</td></tr>
 <tr><td>g</td>  <td>↔</td>  <td>g</td>     <td><b>g</b>ame</td></tr>
 <tr><td>l</td>  <td>↔</td>  <td>l</td>     <td><b>l</b>ad</td></tr>
 <tr><td>m</td>  <td>↔</td>  <td>m</td>     <td><b>m</b>ad</td></tr>
 <tr><td>n</td>  <td>↔</td>  <td>n</td>     <td><b>n</b>at</td></tr>
 <tr><td>f</td>  <td>↔</td>  <td>f</td>     <td><b>f</b>at</td></tr>
 <tr><td>v</td>  <td>↔</td>  <td>v</td>     <td><b>v</b>at</td></tr>
 <tr><td>s</td>  <td>↔</td>  <td>s</td>     <td><b>s</b>ap</td></tr>
 <tr><td>z</td>  <td>↔</td>  <td>z</td>     <td><b>z</b>ap</td></tr>
 <tr><td>j</td>  <td>↔</td>  <td>j</td>     <td><b>y</b>ank</td></tr>
 <tr><td>h</td>  <td>↔</td>  <td>h</td>     <td><b>h</b>ad</td></tr>
 <tr><td>w</td>  <td>↔</td>  <td>w</td>     <td><b>w</b>hy</td></tr>
 <tr><td>x</td>  <td>↔</td>  <td>x</td>     <td>lo<b>ch</b></td></tr>
 </tbody>
</table>

<style type="text/css">
 #onewaymap td:first-child, #onewaymap th:first-child { text-align: right; } 
 #onewaymap td:nth-child(2) { text-align: center; } 
 #onewaymap td:nth-child(3) { font-family: monospace; } 
 #onewaymap td:first-child { font-family: monospace; } 
</style>
<table id="onewaymap"><caption>Other mappings</caption>
 <thead><tr>
  <th>IPA</th><th></th><th>DISC</th><th>Example</th>
 </tr></thead>
 <tbody>

 <tr><td colspan="3"></td><th colspan="2">Affricates</th></tr>
 <tr><td>ʤ</td>  <td>→</td>  <td>_</td>    <td><b>j</b>eep</td><td>(ligature)</td></tr>
 <tr><td>ʧ</td>   <td>→</td>  <td>J</td>    <td><b>ch</b>eap</td><td>(ligature)</td></tr>
 <tr><td>d͡ʒ</td> <td>→</td>  <td>_</td>   <td><b>j</b>eep</td><td>(over-joined)</td></tr>
 <tr><td>t͡ʃ</td> <td>→</td>  <td>J</td>   <td><b>ch</b>eap</td><td>(over-joined)</td></tr>

 <tr><td colspan="3"></td><th colspan="2">Unicode's LATIN SMALL LETTER TURNED E instead of schwa</th></tr>

 <tr><td>ǝ</td>  <td>→</td>  <td>@</td>     <td><b>a</b>nother</td></tr>
 <tr><td>ǝʊ</td>  <td>→</td>  <td>5</td>    <td>M<b>OU</b>TH</td></tr>
 <tr><td>ɪǝ</td>  <td>→</td>  <td>7</td>    <td>N<b>EAR</b></td></tr>
 <tr><td>ɛǝ</td>  <td>→</td>  <td>8</td>    <td>SQU<b>AR</b>E</td></tr>
 <tr><td>ʊǝ</td>  <td>→</td>  <td>9</td>    <td>C<b>UR</b>E</td></tr>

 <tr><td colspan="3"></td><th colspan="2">MFA phoneme labels</th></tr>

 <tr><td>aj</td>  <td>→</td>  <td>2</td>    <td>PR<b>I</b>CE</td></tr>
 <tr><td>aw</td>  <td>→</td>  <td>6</td>    <td>M<b>OU</b>TH</td></tr>
 <tr><td>bʲ</td>  <td>→</td>  <td>bj</td>    <td>attri<b>b</b>ute</td></tr>
 <tr><td>dʲ</td>  <td>→</td>  <td>dj</td>    <td><b>d</b>uel</td></tr>
 <tr><td>dʒ</td>  <td>→</td>  <td>_</td>    <td><b>j</b>eep</td></tr>
 <tr><td>ej</td>  <td>→</td>  <td>1</td>    <td>F<b>A</b>CE</td></tr>
 <tr><td>fʲ</td>  <td>→</td>  <td>fj</td>    <td><b>f</b>uel</td></tr>
 <tr><td>kʰ</td>  <td>→</td>  <td>k</td>    <td><b>k</b>angaroo</td></tr>
 <tr><td>mʲ</td>  <td>→</td>  <td>mj</td>    <td><b>M</b>unich</td></tr>
 <tr><td>pʰ</td>  <td>→</td>  <td>p</td>    <td><b>P</b>acific</td></tr>
 <tr><td>pʲ</td>  <td>→</td>  <td>pj</td>    <td><b>p</b>uberty</td></tr>
 <tr><td>tʰ</td>  <td>→</td>  <td>t</td>    <td><b>t</b>able</td></tr>
 <tr><td>tʲ</td>  <td>→</td>  <td>tj</td>    <td><b>t</b>uition</td></tr>
 <tr><td>tʃ</td>  <td>→</td>  <td>J</td>    <td><b>ch</b>eap</td></tr>
 <tr><td>vʲ</td>  <td>→</td>  <td>vj</td>    <td><b>v</b>iew</td></tr>
 <tr><td>ɐ</td>  <td>→</td>  <td>V</td>    <td><b>u</b>nderstand</td></tr>
 <tr><td>ɒː</td>  <td>→</td>  <td>Q</td>    <td><b>au</b>thor</td></tr>
 <tr><td>ɔj</td>  <td>→</td>  <td>4</td>    <td>CH<b>OI</b>CE</td></tr>
 <tr><td>əw</td>  <td>→</td>  <td>5</td>    <td>G<b>OA</b>T</td></tr>
 <tr><td>ɜ</td>  <td>→</td>  <td>3</td>    <td>leis<b>ur</b>ely</td></tr>
 <tr><td>ɟ</td>  <td>→</td>  <td>g</td>    <td>le<b>g</b>ume</td></tr>
 <tr><td>ɟʷ</td>  <td>→</td>  <td>g</td>    <td>lin<b>gu</b>istics</td></tr>
 <tr><td>ɡ</td>  <td>→</td>  <td>g</td>    <td><b>g</b>et</td></tr>
 <tr><td>ɫ</td>  <td>→</td>  <td>l</td>    <td>mi<b>l</b>k</td></tr>
 <tr><td>ɫ̩</td>  <td>→</td>  <td>P</td>    <td>mod<b>el</b></td></tr>
 <tr><td>ʉː</td>  <td>→</td>  <td>}</td>    <td><b>oo</b>ze</td></tr>
 <tr><td>ʎ</td>  <td>→</td>  <td>j</td>    <td><b>l</b>eak</td></tr>
 </tbody>
</table>
</div>

 *
 * <style type="text/css"> tt:before { content: '/'; } tt:after { content: '/'; } </style>
 * @see DISC2IPA
 * @author Robert Fromont robert@fromont.net.nz
 */
public class IPA2DISC extends PhonemeTranslator {

  private static HashMap<String,String> map;
   
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
    // *NB* Any changes should also be reflected in the JavaDoc comments above!
    if (map == null) {
      map = new HashMap<String,String>();

      // reverse mappings from DISC2IPA ...
         
      map.put("a", "&");
      map.put("ɑː", "#");
      map.put("ɑ", "A");
      
      map.put("æ̃", "c");
      map.put("ɑ̃ː", "q");
      map.put("æ̃ː", "0");
      map.put("ɒ̃ː", "~");
         
      // x is now /x/ and C is /ç/
         
      // syllabics
      map.put("ŋ̩", "C");
      map.put("m̩", "F");
      map.put("n̩", "H");
      map.put("l̩", "P");
         
      //to be strictly correct
      map.put("ɹ", "r");
      map.put("ɡ", "g"); // this is LATIN SMALL LETTER SCRIPT G which is always straight-backed
         
      map.put("uː", "u");
      map.put("iː", "i");
      map.put("yː", "y");
      map.put("oː", "o");
      map.put("ɛː", ")");
      map.put("øː", "|");
      map.put("œ̃ː", "^");
      map.put("œ", "/");      
         
      // diphthongs
      map.put("eɪ", "1");
      map.put("aɪ", "2");
      map.put("əʊ", "5");
      map.put("ɔɪ", "4");
      map.put("aʊ", "6");
      map.put("ɪə", "7");
      map.put("ɛə", "8");
      map.put("ʊə", "9");
      map.put("ai", "W");
      map.put("au", "B");
      map.put("ɔy", "X");
         
      // affricates
      map.put("d͜ʒ", "_");
      map.put("t͜ʃ", "J");
         
      map.put("ɛ", "E");
         
      map.put("t͜s", "=");
         
      // glottal stop
      map.put("ʔ", "?");

      map.put("œy", "L");
         
      // consonants
      map.put("ŋ", "N");
      map.put("θ", "T");
      map.put("ð", "D");
      map.put("ʃ", "S");
      map.put("ʒ", "Z");
      map.put("pf", "+");
         
      // vowels
      map.put("ɜː", "3");
      map.put("æ", "{");
      map.put("ʉ", "}");
      map.put("ɔː", "$");
      map.put("ɪ", "I");
      map.put("ʏ", "Y");
      map.put("ʌ", "V");
      map.put("ɒ", "Q");
      map.put("ɔ", "O");
      map.put("ʊ", "U");
      map.put("ə", "@");

      // other mappings...

      // afficate ligatures
      map.put("ʤ", "_");
      map.put("ʧ", "J");
      // and over-joined
      map.put("d͡ʒ", "_");
      map.put("t͡ʃ", "J");
         
      // LATIN SMALL LETTER TURNED E instead of schwa
      map.put("ǝ", "@");
      map.put("ǝʊ", "5");
      map.put("ɪǝ", "7");
      map.put("ɛǝ", "8");
      map.put("ʊǝ", "9");

      // MFA phoneme labels
      // aj aw b bʲ c cʰ d dʒ dʲ ej f fʲ h i iː j k kʰ l m mʲ m̩ n n̩ p pʰ pʲ s t tʃ tʰ tʲ
      // v vʲ w z æ ç ð ŋ ɐ ɑ ɑː ɒ ɒː ɔj ə əw ɛ ɛː ɜ ɜː ɟ ɡ ɪ ɫ ɫ̩ ɱ ɲ ɹ ʃ ʉ ʉː ʊ ʎ ʒ ʔ θ
      map.put("aj", "2");
      map.put("aw", "6");
      map.put("bʲ", "bj");
      map.put("dʲ", "dj");
      map.put("dʒ", "_");
      map.put("ej", "1");
      map.put("fʲ", "fj");
      map.put("kʰ", "k");
      map.put("mʲ", "mj");
      map.put("pʰ", "p");
      map.put("pʲ", "pj");
      map.put("tʰ", "t");
      map.put("tʲ", "tj");
      map.put("tʃ", "J");
      map.put("vʲ", "vj");
      map.put("ɐ", "V");
      map.put("ɒː", "Q");
      map.put("ɔj", "4");
      map.put("əw", "5");
      map.put("ɜ", "3");
      map.put("ɟ", "g");
      map.put("ɟʷ", "g");
      map.put("ɡ", "g");
      map.put("ɫ", "l");
      map.put("ɫ̩", "P");
      map.put("ʉː", "}");
      map.put("ʎ", "j");
      
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
