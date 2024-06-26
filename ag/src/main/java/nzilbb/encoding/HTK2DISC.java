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

/**
 * Translates Hidden Markov Model Toolkit (HTK) dictionary pronunciations like
 * <tt>t r _{ n s k r I p S _@ n</tt>.
 * to CELEX-DISC-encoded transcriptions, like:
 * <tt>tr{nskrIpS@n</tt>
 * 
 * <p> Essentially, spaces and underscores are removed.
 *
 * <p> This translator will also handle input that is IPA-encoded, e.g.
 * <tt>mʌtnt͡ʃɔps</tt>
 * ...becomes:
 * <tt>m _ʌ t n̩ t͡ʃ _ɔ p s</tt>.
 *

<style type="text/css">
 #mapping td:first-child, #mapping th:first-child { text-align: right; } 
 #mapping td:nth-child(2) { text-align: center; } 
 #mapping td:nth-child(3) { font-family: monospace; } 
 #mapping td:first-child { font-family: monospace; } 
</style>
<table id="mapping"><caption>Mapping</caption>
 <thead><tr>
  <th>Source</th><th></th><th>Destination</th><th>Example</th>
 </tr></thead>
 <tbody>

 </tbody>
</table>

 * <style type="text/css"> tt:before { content: '/'; } tt:after { content: '/'; } </style>
 * @see DISC2HTK
 * @author Robert Fromont robert@fromont.net.nz
 */
public class HTK2DISC extends PhonemeTranslator {

   /**
    * Default constructor.
    */
   public HTK2DISC() {
      sourceEncoding = "HTK";
      destinationEncoding = "DISC";      
   } // end of constructor

   /**
    * Translates a phonemic transcription from the source encoding to the destination encoding.
    * @param source Phonemic transcription in the source encoding.
    * @return Phonemic transcription in the destination encoding.
    */ 
   public String apply(String source) {
    return source.replaceAll(" ","").replaceAll("_([^ ])","$1");
   }
   
} // end of class HTK2DISC
