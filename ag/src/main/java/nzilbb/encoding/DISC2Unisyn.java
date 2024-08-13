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
 * <tt>"tr{n-'skrIp-S@n</tt>
 * to <a href="http://www.cstr.ed.ac.uk/projects/unisyn/">Unisyn</a>-encoded 
 * phonemic transcriptions like
 * <tt>~&nbsp;t&nbsp;r&nbsp;a&nbsp;n&nbsp;.&nbsp;*&nbsp;s&nbsp;k&nbsp;r&nbsp;i&nbsp;p&nbsp;.&nbsp;sh&nbsp;@&nbsp;n</tt>.
 *
 * <p>This converts not only the phonemes, but also syllabification and stress markers:
 * <ul>
 *  <li><code>'</code> &rarr; <code>*</code> - primary stress</li>
 *  <li><code>"</code> &rarr; <code>~</code> - secondary stress</li>
 *  <li><code>,</code> &rarr; <code>-</code> - tertiary stress</li>
 *  <li><code>-</code> &rarr; <code>.</code> - syllable boundary</li>
 * </ul> 

<style type="text/css">
 #mapping td:first-child, #mapping th:first-child { text-align: right; } 
 #mapping td:nth-child(2) { text-align: center; } 
 #mapping td:nth-child(3) { font-family: monospace; } 
 #mapping td:first-child { font-family: monospace; } 
</style>
<table id="mapping"><caption>Mapping</caption>
 <thead><tr>
  <th>DISC</th><th></th><th>Unisyn</th><th>Example</th>
 </tr></thead>
 <tbody>

 <tr><td colspan="3"></td><th colspan="2">Stress and syllabification</th></tr>
 <tr><td>'</td>  <td>→</td>  <td>*</td>   <td></td> <td>primary stress</td></tr>
 <tr><td>"</td>  <td>→</td>  <td>~</td>   <td></td> <td>secondary stress</td></tr>
 <tr><td>,</td>  <td>→</td>  <td>-</td>   <td></td> <td>tertiary stress</td></tr>
 <tr><td>-</td>  <td>→</td>  <td>.</td>   <td></td> <td>syllable boundary</td></tr>

 <tr><td colspan="3"></td><th colspan="2">Vowels</th></tr>
 <tr><td>E</td>  <td>→</td>  <td>e</td>   <td>DRESS</td></tr>
 <tr><td>{</td>  <td>→</td>  <td>a</td>   <td>TRAP</td></tr>
 <tr><td>#</td>  <td>→</td>  <td>ah</td>   <td>START</td></tr>
 <tr><td>5</td>  <td>→</td>  <td>ou</td>   <td>GOAT</td><td>but a monophthong for edi</td></tr>
 <tr><td>Q</td>  <td>→</td>  <td>o</td>   <td>LOT</td></tr>
 <tr><td>$</td>  <td>→</td>  <td>oo</td>   <td>THOUGHT</td><td>(but a diphthong in some en-US)</td></tr>
 <tr><td>i</td>  <td>→</td>  <td>ii</td>   <td>FLEECE</td></tr>
 <tr><td>I</td>  <td>→</td>  <td>i</td>   <td>KIT</td></tr>
 <tr><td>@</td>  <td>→</td>  <td>@</td>   <td>schwa</td></tr>
 <tr><td>V</td>  <td>→</td>  <td>uh</td>   <td>STRUT</td></tr>
 <tr><td>U</td>  <td>→</td>  <td>u</td>   <td>FOOT</td></tr>
 <tr><td>u</td>  <td>→</td>  <td>uu</td>   <td>GOOSE</td></tr>
 <tr><td>1</td>  <td>→</td>  <td>ei</td>   <td>FACE</td></tr>
 <tr><td>2</td>  <td>→</td>  <td>ai</td>   <td>PRICE</td></tr>
 <tr><td>4</td>  <td>→</td>  <td>oi</td>   <td>CHOICE</td></tr>
 <tr><td>6</td>  <td>→</td>  <td>ow</td>   <td>MOUTH</td></tr>
 <tr><td>7</td>  <td>→</td>  <td>i@</td>   <td>NEAR</td></tr>
 <tr><td>3</td>  <td>→</td>  <td>@@r</td>   <td>NURSE</td></tr>
 <tr><td>8</td>  <td>→</td>  <td>eir</td>   <td>SQUARING</td><td>(actually a monophthong in many)</td></tr>
 <tr><td>9</td>  <td>→</td>  <td>ur</td>   <td>JURY</td></tr>

 <tr><td colspan="3"></td><th colspan="2">Missing</th></tr>
 <tr><td>c</td>  <td>→</td>  <td>o</td>   <td>LOT</td></tr>
 <tr><td>q</td>  <td>→</td>  <td>o</td>   <td>LOT</td></tr>
 <tr><td>0</td>  <td>→</td>  <td>o</td>   <td>LOT</td></tr>
 <tr><td>~</td>  <td>→</td>  <td>o</td>   <td>LOT</td></tr>

 <tr><td colspan="3"></td><th colspan="2">Consonants</th></tr>
 <tr><td>j</td>  <td>→</td>  <td>y</td></tr>
 <tr><td>J</td>  <td>→</td>  <td>ch</td></tr>
 <tr><td>_</td>  <td>→</td>  <td>jh</td></tr>
 <tr><td>S</td>  <td>→</td>  <td>sh</td></tr>
 <tr><td>Z</td>  <td>→</td>  <td>zh</td></tr>
 <tr><td>T</td>  <td>→</td>  <td>th</td></tr>
 <tr><td>D</td>  <td>→</td>  <td>dh</td></tr>
 <tr><td>L</td>  <td>→</td>  <td>t^</td>   <td>butter/merry flap</td></tr>
 <tr><td>F</td>  <td>→</td>  <td>m!</td>   <td>chasm</td></tr>
 <tr><td>H</td>  <td>→</td>  <td>n!</td>   <td>mission</td></tr>
 <tr><td>N</td>  <td>→</td>  <td>ng </td></tr>
 <tr><td>P</td>  <td>→</td>  <td>l!</td>   <td>cattle</td></tr>

 </tbody>
</table>

 * @see Unisyn2DISC 
 * @author Robert Fromont robert@fromont.net.nz
 */
public class DISC2Unisyn extends PhonemeTranslator {
   
   private static HashMap<Character,String> map;
   
   /**
    * Default constructor.
    */
   public DISC2Unisyn() {
      sourceEncoding = "DISC";
      destinationEncoding = "Unisyn";
      
      // populate the static map of individual phones, if it's not already initialized...
      if (map == null) {
         map = new HashMap<Character,String>();
         
         // stress and syllabification
         map.put('\'', "*"); // primary stress
         map.put('"', "~"); // secondary stress
         map.put(',', "-"); // tertiary stress
         map.put('-', "."); // syllable boundary
         
         // vowels
         map.put('E', "e"); // DRESS
         map.put('{', "a"); // TRAP
         map.put('#', "ah"); // START
         map.put('5', "ou"); // GOAT - but a monophthong for edi
         map.put('Q', "o"); // LOT
         map.put('$', "oo"); // THOUGHT (but a diphthong in some en-US)
         map.put('i', "ii"); // FLEECE
         map.put('I', "i"); // KIT
         map.put('@', "@"); // schwa
         map.put('V', "uh"); // STRUT
         map.put('U', "u"); // FOOT
         map.put('u', "uu"); // GOOSE
         map.put('1', "ei"); // FACE
         map.put('2', "ai"); // PRICE
         map.put('4', "oi"); // CHOICE
         map.put('6', "ow"); // MOUTH
         map.put('7', "i@"); // NEAR
         map.put('3', "@@r"); // NURSE
         map.put('8', "eir"); // SQUARING (actually a monophthong in many)
         map.put('9', "ur"); // JURY
         
         // missing
         map.put('c', "o");  // LOT
         map.put('q', "o");  // LOT
         map.put('0', "o");  // LOT
         map.put('~', "o");  // LOT
         
         // consonants
         map.put('j', "y");
         map.put('J', "ch");
         map.put('_', "jh");
         map.put('S', "sh");
         map.put('Z', "zh");
         map.put('T', "th");
         map.put('D', "dh");
         map.put('L', "t^"); // butter/merry flap
         map.put('F', "m!"); // chasm
         map.put('H', "n!"); // mission
         map.put('N', "ng"); 
         map.put('P', "l!"); // cattle
         
         // missing
         map.put('C', "ng");
      }
   } // end of constructor
   
   /**
    * Translates a phonemic transcription from the source encoding to the destination encoding.
    * @param source Phonemic transcription in the source encoding.
    * @return Phonemic transcription in the destination encoding.
    */ 
   public String apply(String source) {
      StringBuffer unisyn = new StringBuffer(source.length() * 3);
      // for each phone
      for (int c = 0; c < source.length(); c++) {
         if (unisyn.length() > 0) unisyn.append(" ");
         if (map.containsKey(source.charAt(c))) {
            unisyn.append(map.get(source.charAt(c)));
         } else { // unknown phones are passed through
            unisyn.append(source.charAt(c));
         }
      } // next phoneme
      return unisyn.toString();
   }
   
} // end of class DISC2Unisyn
