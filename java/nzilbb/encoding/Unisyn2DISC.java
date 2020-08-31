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
 * Translates <a href="http://www.cstr.ed.ac.uk/projects/unisyn/">Unisyn</a>-encoded phonemic
 * transcriptions like  
 * <tt>~&nbsp;t&nbsp;r&nbsp;a&nbsp;n&nbsp;.&nbsp;*&nbsp;s&nbsp;k&nbsp;r&nbsp;i&nbsp;p&nbsp;.&nbsp;sh&nbsp;@&nbsp;n</tt> 
 * to CELEX-DISC-encoded transcriptions like
 * <tt>"tr{n-'skrIp-S@n</tt>. 
 *
 * <p>This converts not only the phonemes, but also syllabification and stress markers:
 * <ul>
 *  <li><code>*</code> &rarr; <code>'</code> - primary stress</li>
 *  <li><code>~</code> &rarr; <code>"</code> - secondary stress</li>
 *  <li><code>-</code> &rarr; <code>,</code> - tertiary stress</li>
 *  <li><code>.</code> &rarr; <code>-</code> - syllable boundary</li>
 * </ul> 
 * @see DISC2Unisyn
 * @author Robert Fromont robert@fromont.net.nz
 */
public class Unisyn2DISC extends PhonemeTranslator {

   private static HashMap<String,Character> map;
   
   /**
    * Default constructor.
    */
   public Unisyn2DISC() {
      sourceEncoding = "Unisyn";
      destinationEncoding = "DISC";
      
      // populate the static map of individual phones, if it's not already initialized...
      if (map == null) {
         map = new HashMap<String,Character>();
         
         // stress and syllabification
         map.put("*",'\''); // primary stress
         map.put("~",'"'); // secondary stress
         map.put("-",','); // tertiary stress
         map.put(".",'-'); // syllable boundary
         
         // vowels
         map.put("e",'E'); // DRESS
         map.put("a",'{'); // TRAP
         map.put("ou",'5'); // GOAT - but a monophthong for edi
         map.put("o",'Q'); // LOT
         map.put("ah",'#'); // BATH
         map.put("oo",'$'); // THOUGHT (but a diphthong in some en-US)
         map.put("ii",'i'); // FLEECE
         map.put("i",'I'); // KIT
         map.put("@",'@'); // schwa
         map.put("uh",'V'); // STRUT
         map.put("u",'U'); // FOOT
         map.put("uu",'u'); // GOOSE
         map.put("ei",'1'); // FACE
         map.put("ai",'2'); // PRICE
         map.put("oi",'4'); // CHOICE
         map.put("ow",'6'); // MOUTH
         map.put("i@",'7'); // NEAR
         map.put("@@r",'3'); // NURSE
         map.put("eir",'8'); // SQUARING (actually a monophthong in many)
         map.put("ur",'9'); // JURY
         
         // troublesome because they map in accent-specific ways
         map.put("ar",'Q'); // start -> PALM -> LOT (US) (but BATH (RP))
         map.put("aa",'Q'); // PALM -> LOT (US)  (but BATH (RP))
         map.put("oa",'{'); // BANANA -> TRAP (US) (but BATH (RP))
         map.put("ao",'#'); // MAZDA -> BATH (but TRAP (NZ))
         
         // 2-to-1
         map.put("our",'$'); // FORCE -> THOUGHT
         map.put("eh",'{'); // ann use TRAP
         map.put("oul",'5'); // goal - post vocalic GOAT
         map.put("ouw",'5'); // KNOW -> GOAT (except for Abergave)
         map.put("oou",'Q'); // adios -> LOT
         map.put("au",'Q'); // CLOTH -> LOT (but a diphthong in some en-US)
         map.put("or",'$'); // r-coloured THOUGHT
         map.put("iy",'i'); // HAPPY - I for some varieties
         map.put("ie",'i'); // HARRIET - Leeds only
         map.put("ii;",'i'); // AGREED -> FLEECE
         map.put("@r",'@'); // r-coloured schwa
         map.put("iu",'u'); // BLEW -> GOOSE
         map.put("uu;",'u'); // brewed -> GOOSE
         map.put("uw",'u'); // louise -> GOOSE
         map.put("uul",'u'); // goul - post-vocalic GOOSE
         map.put("ee",'1'); // WASTE -> FACE (except for abercrave)
         map.put("ae",'2'); // TIED -> PRICE (except Edi and Aberdeen)
         map.put("ae",'2'); // TIED -> PRICE (except Edi and Aberdeen)
         map.put("aer",'2'); // FIRE - r-coloured PRICE
         map.put("aai",'2'); // TIME -> PRICE (except S. Carolina)
         map.put("oir",'2'); // COIR - r-coloured PRICE
         map.put("owr",'6'); // HOUR - r-coloured MOUTH
         map.put("oow",'6'); // HOUR -> MOUTH (exception S. Carolina)
         map.put("ir",'i'); // NEARING - r-coloured NEAR -> FLEECE
         map.put("ir;",'i'); // near - scots-long NEAR -> FLEECE
         map.put("iir",'7'); // beard -> NEAR (except en-AU)
         map.put("er",'E'); // r-coloured DRESS in scots en
         map.put("ur;",'9'); // CURE - scots-long JURY
         map.put("iur",'9'); // curious - JURY exception in Cardiff & Abercrave
         
         // missing
         //map.put("{~",'c');
         //map.put("A~:",'q');
         //map.put("{~:",'0');
         //map.put("O~:",'~');
         
         // consonants
         map.put("y",'j');
         map.put("ch",'J');
         map.put("jh",'_');
         map.put("sh",'S');
         map.put("zh",'Z');
         map.put("th",'T');
         map.put("dh",'D');
         map.put("t^",'L'); // butter/merry flap
         map.put("m!",'F'); // chasm
         map.put("n!",'H'); // mission
         map.put("ng",'N'); 
         map.put("l!",'P'); // cattle
         
         // 2-to-1
         map.put("ll",'l'); // llandudno (for Cardiff and Abercrave, this is different)
         map.put("lw",'l'); // feel - dark l
         map.put("hw",'w'); // which
         
         // missing:
         //map.put("N=",'C');
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
         .replaceAll("[><{}\\$=#]","") // ignore morphological stuff
         .split(" ");
      for (String phoneme : phonemes) {
         if (map.containsKey(phoneme)) {
            DISC.append(map.get(phoneme));
         } else { // unknown phones are passed through
            DISC.append(phoneme);
         }
      } // next phoneme
      return DISC.toString();
   }
   
} // end of class Unisyn2DISC
