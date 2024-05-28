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

<style type="text/css">
 #mapping td:first-child, #mapping th:first-child { text-align: right; } 
 #mapping td:nth-child(2) { text-align: center; } 
 #mapping td:nth-child(3) { font-family: monospace; } 
 #mapping td:first-child { font-family: monospace; } 
</style>
<table id="mapping"><caption>Mapping</caption>
 <thead><tr>
  <th>Unisyn</th><th></th><th>DISC</th><th>Example</th>
 </tr></thead>
 <tbody>

 <tr><td colspan="3"></td><th colspan="2">Stress/syllabification</th></tr>
 <tr><td>*</td>  <td>→</td>  <td>'</td>   <td></td> <td>primary stress</td></tr>
 <tr><td>~</td>  <td>→</td>  <td>"</td>   <td></td> <td>secondary stress</td></tr>
 <tr><td>-</td>  <td>→</td>  <td>,</td>   <td></td> <td>tertiary stress</td></tr>
 <tr><td>.</td>  <td>→</td>  <td>-</td>   <td></td> <td>syllable boundary</td></tr>

 <tr><td colspan="3"></td><th colspan="2">Vowels</th></tr>
 <tr><td>e</td>  <td>→</td>  <td>E</td>   <td>DRESS</td></tr>
 <tr><td>a</td>  <td>→</td>  <td>{</td>   <td>TRAP</td></tr>
 <tr><td>ou</td>  <td>→</td>  <td>5</td>   <td>GOAT</td> <td>(but a monophthong for edi)</td></tr>
 <tr><td>o</td>  <td>→</td>  <td>Q</td>   <td>LOT</td></tr>
 <tr><td>ah</td>  <td>→</td>  <td>#</td>   <td>BATH</td></tr>
 <tr><td>oo</td>  <td>→</td>  <td>$</td>   <td>THOUGHT</td> <td>(but a diphthong in some en-US)</td></tr>
 <tr><td>ii</td>  <td>→</td>  <td>i</td>   <td>FLEECE</td></tr>
 <tr><td>i</td>  <td>→</td>  <td>I</td>   <td>KIT</td></tr>
 <tr><td>@</td>  <td>→</td>  <td>@</td>   <td>schwa</td></tr>
 <tr><td>uh</td>  <td>→</td>  <td>V</td>   <td>STRUT</td></tr>
 <tr><td>u</td>  <td>→</td>  <td>U</td>   <td>FOOT</td></tr>
 <tr><td>uu</td>  <td>→</td>  <td>u</td>   <td>GOOSE</td></tr>
 <tr><td>ei</td>  <td>→</td>  <td>1</td>   <td>FACE</td></tr>
 <tr><td>ai</td>  <td>→</td>  <td>2</td>   <td>PRICE</td></tr>
 <tr><td>oi</td>  <td>→</td>  <td>4</td>   <td>CHOICE</td></tr>
 <tr><td>ow</td>  <td>→</td>  <td>6</td>   <td>MOUTH</td></tr>
 <tr><td>i@</td>  <td>→</td>  <td>7</td>   <td>NEAR</td></tr>
 <tr><td>@@r</td>  <td>→</td>  <td>3</td>   <td>NURSE</td></tr>
 <tr><td>eir</td>  <td>→</td>  <td>8</td>   <td>SQUARING</td> <td>(actually a monophthong in many)</td></tr>
 <tr><td>ur</td>  <td>→</td>  <td>9</td>   <td>JURY</td></tr>

 <tr><td colspan="3"></td><th colspan="2">(troublesome because they map in accent-specific ways)</th></tr>
 <tr><td>ar</td>  <td>→</td>  <td>Q</td>   <td>start → PALM → LOT (US)</td> <td>(but BATH (RP))</td></tr>
 <tr><td>aa</td>  <td>→</td>  <td>Q</td>   <td>PALM → LOT (US)</td> <td>(but BATH (RP))</td></tr>
 <tr><td>oa</td>  <td>→</td>  <td>{</td>   <td>BANANA → TRAP (US)</td> <td>(but BATH (RP))</td></tr>
 <tr><td>ao</td>  <td>→</td>  <td>#</td>   <td>MAZDA → BATH</td> <td>(but TRAP (NZ))</td></tr>

 <tr><td colspan="3"></td><th colspan="2">2-to-1</th></tr>
 <tr><td>our</td>  <td>→</td>  <td>$</td>   <td>FORCE → THOUGHT</td></tr>
 <tr><td>eh</td>  <td>→</td>  <td>{</td>   <td>ann use TRAP</td></tr>
 <tr><td>oul</td>  <td>→</td>  <td>5</td>   <td>goal</td> <td>post vocalic GOAT</td></tr>
 <tr><td>ouw</td>  <td>→</td>  <td>5</td>   <td>KNOW → GOAT</td> <td>(except for Abergave)</td></tr>
 <tr><td>oou</td>  <td>→</td>  <td>Q</td>   <td>adios → LOT</td></tr>
 <tr><td>au</td>  <td>→</td>  <td>Q</td>   <td>CLOTH → LOT</td> <td>(but a diphthong in some en-US)</td></tr>
 <tr><td>or</td>  <td>→</td>  <td>$</td>   <td></td> <td>r-coloured THOUGHT</td></tr>
 <tr><td>iy</td>  <td>→</td>  <td>i</td>   <td>HAPPY</td> <td>I for some varieties</td></tr>
 <tr><td>ie</td>  <td>→</td>  <td>i</td>   <td>HARRIET</td> <td>Leeds only</td></tr>
 <tr><td>ii;</td>  <td>→</td>  <td>i</td>   <td>AGREED → FLEECE</td></tr>
 <tr><td>@r</td>  <td>→</td>  <td>@</td>   <td></td> <td>r-coloured schwa</td></tr>
 <tr><td>iu</td>  <td>→</td>  <td>u</td>   <td>BLEW → GOOSE</td></tr>
 <tr><td>uu;</td>  <td>→</td>  <td>u</td>   <td>brewed → GOOSE</td></tr>
 <tr><td>uw</td>  <td>→</td>  <td>u</td>   <td>louise → GOOSE</td></tr>
 <tr><td>uul</td>  <td>→</td>  <td>u</td>   <td>goul</td> <td>post-vocalic GOOSE</td></tr>
 <tr><td>ee</td>  <td>→</td>  <td>1</td>   <td>WASTE → FACE</td> <td>(except for abercrave)</td></tr>
 <tr><td>ae</td>  <td>→</td>  <td>2</td>   <td>TIED → PRICE</td> <td>(except Edi and Aberdeen)</td></tr>
 <tr><td>ae</td>  <td>→</td>  <td>2</td>   <td>TIED → PRICE</td> <td>(except Edi and Aberdeen)</td></tr>
 <tr><td>aer</td>  <td>→</td>  <td>2</td>   <td>FIRE</td> <td>r-coloured PRICE</td></tr>
 <tr><td>aai</td>  <td>→</td>  <td>2</td>   <td>TIME → PRICE</td> <td>(except S. Carolina)</td></tr>
 <tr><td>oir</td>  <td>→</td>  <td>2</td>   <td>COIR</td> <td>r-coloured PRICE</td></tr>
 <tr><td>owr</td>  <td>→</td>  <td>6</td>   <td>HOUR</td> <td>r-coloured MOUTH</td></tr>
 <tr><td>oow</td>  <td>→</td>  <td>6</td>   <td>HOUR → MOUTH</td> <td>(exception S. Carolina)</td></tr>
 <tr><td>ir</td>  <td>→</td>  <td>i</td>   <td>NEARING</td> <td>r-coloured NEAR → FLEECE</td></tr>
 <tr><td>ir;</td>  <td>→</td>  <td>i</td>   <td>near</td> <td>scots-long NEAR → FLEECE</td></tr>
 <tr><td>iir</td>  <td>→</td>  <td>7</td>   <td>beard → NEAR</td> <td>(except en-AU)</td></tr>
 <tr><td>er</td>  <td>→</td>  <td>E</td>   <td></td> <td>r-coloured DRESS in scots en</td></tr>
 <tr><td>ur;</td>  <td>→</td>  <td>9</td>   <td>CURE</td> <td>scots-long JURY</td></tr>
 <tr><td>iur</td>  <td>→</td>  <td>9</td>   <td>curious</td> <td>JURY exception in Cardiff &amp; Abercrave</td></tr>

 <tr><td colspan="3"></td><th colspan="2">Consonants</th></tr>
 <tr><td>y</td>  <td>→</td>  <td>j</td></tr>
 <tr><td>ch</td>  <td>→</td>  <td>J</td></tr>
 <tr><td>jh</td>  <td>→</td>  <td>_</td></tr>
 <tr><td>sh</td>  <td>→</td>  <td>S</td></tr>
 <tr><td>zh</td>  <td>→</td>  <td>Z</td></tr>
 <tr><td>th</td>  <td>→</td>  <td>T</td></tr>
 <tr><td>dh</td>  <td>→</td>  <td>D</td></tr>
 <tr><td>t^</td>  <td>→</td>  <td>L</td>   <td>butter/merry flap</td></tr>
 <tr><td>m!</td>  <td>→</td>  <td>F</td>   <td>chasm</td></tr>
 <tr><td>n!</td>  <td>→</td>  <td>H</td>   <td>mission</td></tr>
 <tr><td>ng</td>  <td>→</td>  <td>N </td></tr>
 <tr><td>l!</td>  <td>→</td>  <td>P</td>   <td>cattle</td></tr>

 <tr><td colspan="3"></td><th colspan="2">(2-to-1)</th></tr>

 <tr><td>ll</td>  <td>→</td>  <td>l</td>   <td>llandudno</td> <td>(for Cardiff and Abercrave, this is different)</td></tr>
 <tr><td>lw</td>  <td>→</td>  <td>l</td>   <td>feel - dark l</td></tr>
 <tr><td>hw</td>  <td>→</td>  <td>w</td>   <td>which</td></tr>

 </tbody>
</table>

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
