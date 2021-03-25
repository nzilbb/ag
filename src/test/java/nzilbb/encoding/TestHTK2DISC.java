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
	      
import org.junit.*;
import static org.junit.Assert.*;

import nzilbb.encoding.HTK2DISC;
import nzilbb.encoding.CMU2DISC;

public class TestHTK2DISC {
   
   @Test public void HTK2DISC() throws Exception {
      // CMU is a little one-to-one to DISC      
      String[] aCodebook = {
	 "#", /* <-> */"_#", // BATH        - odd/father
	 "{", /* <-> */"_{", // TRAP        - at/fast
	 "V", /* <-> */"V", // STRUT       - hut/but
	 "$", /* <-> */"_$", // THOUGHT     - ought/fall
	 "6", /* <-> */"_6", // MOUTH       - cow/how
	 "2", /* <-> */"_2", // PRICE       - hide/my
	 "b", /* <-> */"b",
	 "J", /* <-> */"J",
	 "d", /* <-> */"d",
	 "D", /* <-> */"D",
	 "E", /* <-> */"E", // DRESS       - Ed/red
	 "3", /* <-> */"_3", // NURSE       - hurt/her
	 "1", /* <-> */"_1", // FACE        - ate/say
	 "f", /* <-> */"f", 
	 "g", /* <-> */"g",
	 "h", /* <-> */"h",
	 "I", /* <-> */"I", // KIT         - it/big
	 "i", /* <-> */"i", // FLEECE      - eat/bee
	 "_", /* <-> */"__",
	 "k", /* <-> */"k",
	 "l", /* <-> */"l",
	 "m", /* <-> */"m",
	 "n", /* <-> */"n",
	 "N", /* <-> */"N",
	 "5", /* <-> */"_5", // GOAT        - oat/show
	 "4", /* <-> */"_4", // CHOICE      - toy/boy
	 "p", /* <-> */"p",
	 "r", /* <-> */"r",
	 "s", /* <-> */"s",
	 "S", /* <-> */"S",
	 "t", /* <-> */"t",
	 "T", /* <-> */"T",
	 "U", /* <-> */"U", // FOOT        - hood/should
	 "u", /* <-> */"u", // GOOSE       - two/you
	 "v", /* <-> */"v",
	 "w", /* <-> */"w",
	 "j", /* <-> */"j",
	 "z", /* <-> */"z",
	 "Z", /* <-> */"Z"
      };

      HTK2DISC translator = new HTK2DISC();
      assertEquals("Source encoding name", "HTK", translator.getSourceEncoding());
      assertEquals("Destination encoding name", "DISC", translator.getDestinationEncoding());

      StringBuffer sDISC = new StringBuffer(aCodebook.length/2);
      StringBuffer sHTK = new StringBuffer(3*aCodebook.length/2);
      // for each pair of elements into the array
      for (int i = 0; i < aCodebook.length; i += 2) {
	 // check  mapping
	 assertEquals("DISC " + aCodebook[i+1], translator.apply(aCodebook[i]), aCodebook[i]);
         
	 // accumulate the whole set into strings
	 sDISC.append(aCodebook[i]);
	 if (sHTK.length() > 0) sHTK.append(" ");
	 sHTK.append(aCodebook[i+1]);
      } // next pair
      
      assertEquals(translator.apply(sHTK.toString()), sDISC.toString());
   }
   
   @Test public void IPAtoHTK() {
      
      // A nominally DISC layer can actually be pure IPA
      // the HTK encoding uses a space separator, prefixes each with an unserscore,
      // and recognises diacritics and suprasegmentals
      String[] aCodebook = {
	 "ɑː", /* <-> */"_ɑː", // BATH        - odd/father
	 "æ", /* <-> */"_æ", // TRAP        - at/fast
	 "ʌ", /* <-> */"_ʌ", // STRUT       - hut/but
	 "ɒː", /* <-> */"_ɒː", // THOUGHT     - ought/fall
//	 "6", /* <-> */"aU", // MOUTH       - cow/how
	 "ə", /* <-> */"_ə", // schwa       - discuss
//	 "2", /* <-> */"aI", // PRICE       - hide/my
	 "b", /* <-> */"b",
	 "ʧ", /* <-> */"_ʧ",
	 "t͡ʃ", /* <-> */"t͡ʃ", // with tie
	 "d", /* <-> */"d",
	 "ð", /* <-> */"_ð",
	 "ɛ", /* <-> */"_ɛ", // DRESS       - Ed/red
	 "ɜ", /* <-> */"_ɜ", // NURSE       - hurt/her
//	 "1", /* <-> */"eI", // FACE        - ate/say
	 "f", /* <-> */"f", 
	 "g", /* <-> */"g",
	 "h", /* <-> */"h",
	 "ɪ", /* <-> */"_ɪ", // KIT         - it/big
	 "iː", /* <-> */"iː", // FLEECE      - eat/bee
	 "_", /* <-> */"__", // classic DISC
	 "ʤ", /* <-> */"_ʤ", // IPA
	 "ʤ", /* <-> */"_ʤ",
	 "d͡ʒ", /* <-> */"d͡ʒ", // with tie
	 "k", /* <-> */"k",
	 "l", /* <-> */"l",
	 "m", /* <-> */"m",
	 "n", /* <-> */"n",
	 "ŋ", /* <-> */"_ŋ",
//	 "5", /* <-> */"@U", // GOAT        - oat/show
//	 "4", /* <-> */"OI", // CHOICE      - toy/boy
	 "p", /* <-> */"p",
	 "r", /* <-> */"r",
	 "s", /* <-> */"s",
	 "ʃ", /* <-> */"_ʃ",
	 "t", /* <-> */"t",
	 "θ", /* <-> */"_θ",
	 "ʊ", /* <-> */"_ʊ", // FOOT        - hood/should
	 "uː", /* <-> */"uː", // GOOSE       - two/you
	 "v", /* <-> */"v",
	 "w", /* <-> */"w",
	 "x", /* <-> */"x",
	 "j", /* <-> */"j",
	 "z", /* <-> */"z",
	 "ʒ", /* <-> */"_ʒ",

	 "ɔ", /* <-> */"_ɔ", // LOT         - ought/off
//	 "7", /* <-> */"I@", // NEAR
//	 "8", /* <-> */"E@", // SQUARE
//	 "9", /* <-> */"U@", // CURE
	 "ʔ", /* <-> */"_ʔ", // glottal stop

	 // diacritics
	 
	 "m̩", /* <-> */"m̩", // idealism
	 "n̩", /* <-> */"n̩", // burden
	 "l̩", /* <-> */"l̩", // dangle
	 "ŋ̍", /* <-> */"_ŋ̍", // bacon
	 "ñ", /* <-> */"ñ", // lingerie
	 "æ̃", /* <-> */"_æ̃", // timbre
	 "ɑ̃ː", /* <-> */"_ɑ̃ː", // detente

	 // unknown phones
	 "X", /* <-> */"X", // ASCII letter
	 "/", /* <-> */"_/", // ASCII non-letter
	 "ö", /* <-> */"_ö" // UNICODE letter
      };

      HTK2DISC translator = new HTK2DISC();
      assertEquals("Source encoding name", "HTK", translator.getSourceEncoding());
      assertEquals("Destination encoding name", "DISC", translator.getDestinationEncoding());

      StringBuffer sDISC = new StringBuffer(aCodebook.length/2);
      StringBuffer sHTK = new StringBuffer(3*aCodebook.length/2);
      // for each pair of elements into the array
      for (int i = 0; i < aCodebook.length; i += 2) {
	 // check  mapping
	 assertEquals("DISC " + aCodebook[i], aCodebook[i], translator.apply(aCodebook[i+1]));
         
	 // accumulate the whole set into strings
	 sDISC.append(aCodebook[i]);
	 if (sHTK.length() > 0) sHTK.append(" ");
	 sHTK.append(aCodebook[i+1]);
      } // next pair
      
      assertEquals(translator.apply(sHTK.toString()), sDISC.toString());      

      // DISC ʤ
      assertEquals("DISC ʤ",
		   "_V_", translator.apply("__ V __"));
   }
   
   public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nzilbb.encoding.TestHTK2DISC");
   }
}
