//
// Copyright 2020-2024 New Zealand Institute of Language, Brain and Behaviour, 
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
 * <tt>tr{nskrIpS@n</tt>
 * to IPA-encoded phonemic transcriptions using Unicode characters like
 * <tt>tɹænskɹɪpʃən</tt>.
 * <p> This supports straight round-trip conversion with IPA symbols used by
 * {@link IPA2DISC}. The 1-to-1 mappings are:

<h3 id="mapping">Mapping</h3>

<style type="text/css">
 #equiv td:first-child, #equiv th:first-child { text-align: right; } 
 #equiv td:nth-child(2) { text-align: center; } 
 #equiv td:nth-child(3) { font-family: monospace; } 
 #equiv td:first-child { font-family: monospace; } 
</style>
<table id="equiv">
 <thead><tr>
  <th>DISC</th><th></th><th>IPA</th><th>Example</th>
 </tr></thead>
 <tbody>
 
 <tr><td>&amp;</td>  <td>↔</td>  <td>a</td> <td> h<b>a</b>t</td><td>(German)</td></tr>
 
 <tr><td colspan="3"></td><th colspan="2">Nasals</th></tr>        
 <tr><td>c</td>  <td>↔</td>  <td>æ̃</td>     <td>t<b>i</b>mbre</td></tr>
 <tr><td>q</td>  <td>↔</td>  <td>ɑ̃ː</td>    <td>dét<b>en</b>te</td></tr>
 <tr><td>0</td>  <td>↔</td>  <td>æ̃ː</td>    <td>l<b>in</b>gerie</td></tr>
 <tr><td>~</td>  <td>↔</td>  <td>ɒ̃ː</td>     <td>bouill<b>on</b></td></tr>
  
 <tr><td colspan="3"></td><th colspan="2">Possible linking r</th></tr>
 <tr><td>R</td>  <td>↔</td>  <td>ɹ</td>      <td>fathe<b>r</b></td></tr>
 
 <tr><td colspan="3"></td><th colspan="2">Syllabics</th></tr>
 <tr><td>C</td>  <td>↔</td>  <td>ŋ̩</td>     <td>baco<b>n</b></td></tr>
 <tr><td>F</td>  <td>↔</td>  <td>m̩</td>     <td>idealis<b>m</b></td></tr>
 <tr><td>H</td>  <td>↔</td>  <td>n̩</td>     <td>burde<b>n</b></td></tr>
 <tr><td>P</td>  <td>↔</td>  <td>l̩</td>     <td>dang<b>l</b>e</td></tr>
         
 <tr><td>r</td>  <td>↔</td>  <td>ɹ</td>     <td><b>r</b>at</td></tr>
 <tr><td>g</td>  <td>↔</td>  <td>ɡ</td>     <td><b>g</b>ame</td><td>(this is LATIN SMALL LETTER SCRIPT G which is always straight-backed)</td></tr>
         
 <tr><td colspan="3"></td><th colspan="2">Monophthongs</th></tr>
 <tr><td>3</td>  <td>↔</td>  <td>ɜː</td>     <td>N<b>UR</b>SE</td></tr>
 <tr><td>{</td>  <td>↔</td>  <td>æ</td>     <td>TR<b>A</b>P</td></tr>
 <tr><td>}</td>  <td>↔</td>  <td>ʉ</td>     <td>put</td><td>(Dutch)</td></tr>
 <tr><td>$</td>  <td>↔</td>  <td>ɔː</td>     <td>TH<b>OU</b>GHT</td></tr>
 <tr><td>I</td>  <td>↔</td>  <td>ɪ</td>     <td>K<b>I</b>T</td></tr>
 <tr><td>Y</td>  <td>↔</td>  <td>ʏ</td>     <td>Pf<b>ü</b>tze</td></tr>
 <tr><td>V</td>  <td>↔</td>  <td>ʌ</td>     <td>STR<b>U</b>T</td></tr>
 <tr><td>Q</td>  <td>↔</td>  <td>ɒ</td>     <td>L<b>O</b>T</td></tr>
 <tr><td>O</td>  <td>↔</td>  <td>ɔ</td>     <td>Gl<b>o</b>cke</td><td>(German)</td></tr>
 <tr><td>U</td>  <td>↔</td>  <td>ʊ</td>     <td>F<b>OO</b>T</td></tr>
 <tr><td>@</td>  <td>↔</td>  <td>ə</td>     <td><b>a</b>nother</td> <td>Schwa</td></tr>
 <tr><td>u</td>  <td>↔</td>  <td>uː</td>     <td>G<b>OO</b>SE</td></tr>
 <tr><td>i</td>  <td>↔</td>  <td>iː</td>     <td>FL<b>EE</b>CE</td></tr>
 <tr><td>E</td>  <td>↔</td>  <td>ɛ</td>     <td>DR<b>E</b>SS</td></tr>
 <tr><td>#</td>  <td>↔</td>  <td>ɑː</td>     <td>B<b>A</b>TH</td></tr>
 <tr><td>A</td>  <td>↔</td>  <td>ɑ</td>     <td>k<b>a</b>levala</td><td>(German)</td></tr>
 <tr><td>y</td>  <td>↔</td>  <td>yː</td>     <td>f<b>ü</b>r</td><td>(German)</td></tr>
 <tr><td>o</td>  <td>↔</td>  <td>oː</td>     <td>b<b>oo</b>t</td><td>(German)</td></tr>
 <tr><td>)</td>  <td>↔</td>  <td>ɛː</td>     <td>K<b>ä</b>se</td><td>(German)</td></tr>
 <tr><td>|</td>  <td>↔</td>  <td>øː</td>     <td>M<b>ö</b>bel</td><td>(German)</td></tr>
 <tr><td>^</td>  <td>↔</td>  <td>œ̃ː</td>     <td>Parf<b>u</b>m</td><td>(German)</td></tr>
 <tr><td>/</td>  <td>↔</td>  <td>œ</td>     <td>G<b>ö</b>tter</td><td>(German)</td></tr>      
 
 <tr><td colspan="3"></td><th colspan="2">Diphthongs</th></tr>
 <tr><td>1</td>  <td>↔</td>  <td>eɪ</td>     <td>F<b>A</b>CE</td></tr>
 <tr><td>2</td>  <td>↔</td>  <td>aɪ</td>     <td>PR<b>I</b>CE</td></tr>
 <tr><td>4</td>  <td>↔</td>  <td>ɔɪ</td>     <td>CH<b>OI</b>CE</td></tr>
 <tr><td>5</td>  <td>↔</td>  <td>əʊ</td>     <td>G<b>OA</b>T</td></tr>
 <tr><td>6</td>  <td>↔</td>  <td>aʊ</td>     <td>M<b>OU</b>TH</td></tr>
 <tr><td>7</td>  <td>↔</td>  <td>ɪə</td>     <td>N<b>EAR</b></td></tr>
 <tr><td>8</td>  <td>↔</td>  <td>ɛə</td>     <td>SQU<b>AR</b>E</td></tr>
 <tr><td>9</td>  <td>↔</td>  <td>ʊə</td>     <td>C<b>UR</b>E</td></tr>
 <tr><td>W</td>  <td>↔</td>  <td>ai</td>     <td>w<b>ei</b>t</td><td>(German)</td></tr>
 <tr><td>B</td>  <td>↔</td>  <td>au</td>     <td>H<b>au</b>t</td><td>(German)</td></tr>
 <tr><td>X</td>  <td>↔</td>  <td>ɔy</td>     <td>fr<b>eu</b>t</td><td>(German)</td></tr>
 <tr><td>L</td>  <td>↔</td>  <td>œy</td>     <td>h<b>ui</b>s</td><td>(Dutch)</td></tr>
         
 <tr><td colspan="3"></td><th colspan="2">Consonants</th></tr>
 <tr><td>N</td>  <td>↔</td>  <td>ŋ</td>     <td>ba<b>ng</b></td></tr>
 <tr><td>T</td>  <td>↔</td>  <td>θ</td>     <td><b>th</b>in</td></tr>
 <tr><td>D</td>  <td>↔</td>  <td>ð</td>     <td><b>th</b>en</td></tr>
 <tr><td>S</td>  <td>↔</td>  <td>ʃ</td>     <td><b>sh</b>eep</td></tr>
 <tr><td>Z</td>  <td>↔</td>  <td>ʒ</td>     <td>mea<b>s</b>ure</td></tr>
 <tr><td>+</td>  <td>↔</td>  <td>pf</td>    <td><b>Pf</b>erd</td><td>(German)</td></tr>
 
 <tr><td colspan="3"></td><th colspan="2">Affricates</th></tr>
 <tr><td>_</td>  <td>↔</td>  <td>d͜ʒ</td>   <td><b>j</b>eep</td></tr>
 <tr><td>J</td>  <td>↔</td>  <td>t͜ʃ</td>    <td><b>ch</b>eap</td></tr>
 <tr><td>=</td>  <td>↔</td>  <td>t͜s</td>   <td><b>Z</b>ahl</td><td>(German)</td></tr>
         
 <tr><td colspan="3"></td><th colspan="2">Extensions to DISC, not in CELEX</th></tr>
 <tr><td>?</td>  <td>↔</td>  <td>ʔ</td>     <td>uh<b>-</b>oh</td><td>Glottal Stop</td></tr>

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

 *
 * <style type="text/css"> tt:before { content: '/'; } tt:after { content: '/'; } </style>
 * @author Robert Fromont robert@fromont.net.nz
 */
public class DISC2IPA extends PhonemeTranslator {

   private static HashMap<Character,String> map;
   
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
   public DISC2IPA setDelimiter(String newDelimiter) { delimiter = newDelimiter; return this; }
   
   /**
    * Default constructor.
    */
   public DISC2IPA() {
      sourceEncoding = "DISC";
      destinationEncoding = "IPA";
      
      // populate the static map of individual phones, if it's not already initialized...
      // *NB* Any changes should also be reflected in the JavaDoc comments above!
      if (map == null) {
         map = new HashMap<Character,String>();
         
         map.put('&', "a");
         map.put('#', "ɑː");
         map.put('A', "ɑ");
         
         map.put('c', "æ̃");
         map.put('q', "ɑ̃ː");
         map.put('0', "æ̃ː");
         map.put('~', "ɒ̃ː");
         
         // linking R? etc.
         map.put('R', "ɹ");
         // x is now /x/ and C is /ç/
         
         // syllabics
         map.put('C', "ŋ̩");
         map.put('F', "m̩");
         map.put('H', "n̩");
         map.put('P', "l̩");
         
         //to be strictly correct
         map.put('r', "ɹ");
         map.put('g', "ɡ"); // this is LATIN SMALL LETTER SCRIPT G which is always straight-backed
         
         map.put('u', "uː");
         map.put('i', "iː");
         map.put('y', "yː");
         map.put('o', "oː");
         map.put(')', "ɛː");
         map.put('|', "øː");
         map.put('^', "œ̃ː");
         map.put('/', "œ");      
         
         // diphthongs
         map.put('1', "eɪ");
         map.put('2', "aɪ");
         map.put('5', "əʊ");
         map.put('4', "ɔɪ");
         map.put('6', "aʊ");
         map.put('7', "ɪə");
         map.put('8', "ɛə");
         map.put('9', "ʊə");
         map.put('W', "ai");
         map.put('B', "au");
         map.put('X', "ɔy");
         
         // affricates
         map.put('_', "d͜ʒ");
         map.put('J', "t͜ʃ");
         
         map.put('E', "ɛ");
         
         map.put('=', "t͜s");
         
         // glottal stop
         map.put('?', "ʔ");
         // flap
         map.put('L', "œy");
         
         // consonants
         map.put('N', "ŋ");
         map.put('T', "θ");
         map.put('D', "ð");
         map.put('S', "ʃ");
         map.put('Z', "ʒ");
         map.put('+', "pf");
         
         // vowels
         map.put('3', "ɜː");
         map.put('{', "æ");
         map.put('}', "ʉ");
         map.put('$', "ɔː");
         map.put('I', "ɪ");
         map.put('Y', "ʏ");
         map.put('V', "ʌ");
         map.put('Q', "ɒ");
         map.put('O', "ɔ");
         map.put('U', "ʊ");
         map.put('@', "ə");
      }
   } // end of constructor
   
   /**
    * Translates a phonemic transcription from the source encoding to the destination encoding.
    * @param source Phonemic transcription in the source encoding.
    * @return Phonemic transcription in the destination encoding.
    */ 
   public String apply(String source) {
      StringBuffer IPA = new StringBuffer(source.length() * 3);
      // for each phone
      for (int c = 0; c < source.length(); c++) {
         if (delimiter != null && IPA.length() > 0) IPA.append(delimiter);
         if (map.containsKey(source.charAt(c))) {
            IPA.append(map.get(source.charAt(c)));
         } else { // unknown/unmapped phones are passed through
            IPA.append(source.charAt(c));
         }
      } // next phoneme
      return IPA.toString();
   }
   
} // end of class DISC2IPA
