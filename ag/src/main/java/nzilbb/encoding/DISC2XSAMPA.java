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
 * <tt>str1n_</tt>
 * to X-SAMPA-encoded phonemic transcriptions like
 * <tt>str\eIndZ</tt>.
 *

<style type="text/css">
 #mapping td:first-child, #mapping th:first-child { text-align: right; } 
 #mapping td:nth-child(2) { text-align: center; } 
 #mapping td:nth-child(3) { font-family: monospace; } 
 #mapping td:first-child { font-family: monospace; } 
</style>
<table id="mapping"><caption>Mapping</caption>
 <thead><tr>
  <th>DISC</th><th></th><th>X-SAMPA</th><th>Example</th>
 </tr></thead>
 <tbody>

 <tr><td colspan="3"></td><th colspan="2">Vowels</th></tr>
 <tr><td>i</td>  <td>→</td>  <td>i:</td></tr>
 <tr><td>#</td>  <td>→</td>  <td>A:</td></tr>
 <tr><td>$</td>  <td>→</td>  <td>O:</td></tr>
 <tr><td>u</td>  <td>→</td>  <td>u:</td></tr>
 <tr><td>3</td>  <td>→</td>  <td>3:</td></tr>
 <tr><td>1</td>  <td>→</td>  <td>eI</td></tr>
 <tr><td>2</td>  <td>→</td>  <td>aI</td></tr>
 <tr><td>4</td>  <td>→</td>  <td>OI</td></tr>
 <tr><td>5</td>  <td>→</td>  <td>@U</td></tr>
 <tr><td>6</td>  <td>→</td>  <td>aU</td></tr>
 <tr><td>7</td>  <td>→</td>  <td>I@</td></tr>
 <tr><td>8</td>  <td>→</td>  <td>E@</td></tr>
 <tr><td>9</td>  <td>→</td>  <td>U@</td></tr>
 <tr><td>c</td>  <td>→</td>  <td>{~</td></tr>
 <tr><td>q</td>  <td>→</td>  <td>A~:</td></tr>
 <tr><td>0</td>  <td>→</td>  <td>{~:</td></tr>
 <tr><td>~</td>  <td>→</td>  <td>O~:</td></tr>
         
 <tr><td colspan="3"></td><th colspan="2">Consonants</th></tr>
 <tr><td>J</td>  <td>→</td>  <td>tS</td></tr>
 <tr><td>_</td>  <td>→</td>  <td>dZ</td></tr>
 <tr><td>R</td>  <td>→</td>  <td>r*</td></tr>
         
 <tr><td>r</td>  <td>→</td>  <td>r\</td></tr>
 <tr><td>}</td>  <td>→</td>  <td>}:</td></tr>
 <tr><td>l</td>  <td>→</td>  <td>5</td></tr>
         
 <tr><td>C</td>  <td>→</td>  <td>N=</td></tr>
 <tr><td>F</td>  <td>→</td>  <td>m=</td></tr>
 <tr><td>H</td>  <td>→</td>  <td>n=</td></tr>
 <tr><td>P</td>  <td>→</td>  <td>l=</td></tr>

 </tbody>
</table>

 * <style type="text/css"> tt:before { content: '/'; } tt:after { content: '/'; } </style>
 * @see XSAMPA2DISC
 * @author Robert Fromont robert@fromont.net.nz
 */
public class DISC2XSAMPA extends PhonemeTranslator {

   private static HashMap<Character,String> map;
   
   /**
    * Default constructor.
    */
   public DISC2XSAMPA() {
      sourceEncoding = "DISC";
      destinationEncoding = "XSAMPA";
      
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
         map.put('R',"r*");
         
         // X-XSAMPA not in CELEX table
         map.put('r', "r\\");
         map.put('}', "}:");
         map.put('l', "5");
         
         map.put('C',"N=");
         map.put('F',"m=");
         map.put('H',"n=");
         map.put('P',"l=");
      }
   } // end of constructor
   
   /**
    * Translates a phonemic transcription from the source encoding to the destination encoding.
    * @param source Phonemic transcription in the source encoding.
    * @return Phonemic transcription in the destination encoding.
    */ 
   public String apply(String source) {
      StringBuffer XSAMPA = new StringBuffer(source.length() * 3);
      // for each phone
      for (int c = 0; c < source.length(); c++) {
         if (map.containsKey(source.charAt(c))) {
            XSAMPA.append(map.get(source.charAt(c)));
         } else { // unknown or unexceptional phones are passed through
            XSAMPA.append(source.charAt(c));
         }
      } // next phoneme
      return XSAMPA.toString();
   }
   
} // end of class DISC2XSAMPA
