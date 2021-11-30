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

import nzilbb.encoding.DISC2XSAMPA;
import nzilbb.encoding.XSAMPA2DISC;

public class TestDISC2XSAMPA2DISC {
   
   @Test public void codebook() throws Exception {
      // XSAMPA is a little one-to-one to DISC      
      String[] aCodebook = {
	 "#", /* <-> */"A:", // BATH        - odd/father
	 "{", /* <-> */"{", // TRAP        - at/fast
	 "}", /* <-> */"}:",// close central rounded vowel - NZE boot
	 "V", /* <-> */"V", // STRUT       - hut/but
	 "$", /* <-> */"O:", // THOUGHT     - ought/fall
	 "6", /* <-> */"aU", // MOUTH       - cow/how
	 "@", /* <-> */"@", // schwa       - discuss
	 "2", /* <-> */"aI", // PRICE       - hide/my
	 "b", /* <-> */"b",
	 "J", /* <-> */"tS",
	 "d", /* <-> */"d",
	 "D", /* <-> */"D",
	 "E", /* <-> */"E", // DRESS       - Ed/red
	 "3", /* <-> */"3:", // NURSE       - hurt/her
	 "1", /* <-> */"eI", // FACE        - ate/say
	 "f", /* <-> */"f", 
	 "g", /* <-> */"g",
	 "h", /* <-> */"h",
	 "I", /* <-> */"I", // KIT         - it/big
	 "i", /* <-> */"i:", // FLEECE      - eat/bee
	 "_", /* <-> */"dZ",
	 "k", /* <-> */"k",
	 "l", /* <-> */"5",
	 "m", /* <-> */"m",
	 "n", /* <-> */"n",
	 "N", /* <-> */"N",
	 "5", /* <-> */"@U", // GOAT        - oat/show
	 "4", /* <-> */"OI", // CHOICE      - toy/boy
	 "p", /* <-> */"p",
	 "r", /* <-> */"r\\",
	 "R", /* <-> */"r*",  // possible linking R is pretty definitely R
	 "s", /* <-> */"s",
	 "S", /* <-> */"S",
	 "t", /* <-> */"t",
	 "T", /* <-> */"T",
	 "U", /* <-> */"U", // FOOT        - hood/should
	 "u", /* <-> */"u:", // GOOSE       - two/you
	 "v", /* <-> */"v",
	 "w", /* <-> */"w",
	 "j", /* <-> */"j",
	 "z", /* <-> */"z",
	 "Z", /* <-> */"Z",

	 "Q", /* <-> */"Q", // LOT         - ought/off
	 "7", /* <-> */"I@", // NEAR
	 "8", /* <-> */"E@", // SQUARE
	 "9", /* <-> */"U@", // CURE
	 "?", /* <-> */"?", // glottal stop
	 
	 // Not in the SAMPA set but exist in Buckeye corpus
	 "L", /* <-> */"L", // flap - this is an extension to DISC
	 "^", /* <-> */"^", // nasal flap - doesn't exist in DISC
	 
	 // Syllabics not in ARPAbet set but exist in DISC
	 "F", /* <-> */"m=", // idealism
	 "H", /* <-> */"n=", // burden
	 "P", /* <-> */"l=", // dangle
	 "C", /* <-> */"N=", // bacon
	 "0", /* <-> */"{~:", // lingerie
	 "~", /* <-> */"O~:", // bouillon
	 "c", /* <-> */"{~", // timbre
	 "q", /* <-> */"A~:", // detente

	 // unknown phones
	 "x", /* <-> */"x", // ASCII letter
	 "X", /* <-> */"X", // ASCII letter
	 "/", /* <-> */"/", // ASCII non-letter
	 "ö", /* <-> */"ö" // UNICODE letter
      };

      DISC2XSAMPA disc2xsampa = new DISC2XSAMPA();
      assertEquals("Encoding name", "DISC", disc2xsampa.getSourceEncoding());
      assertEquals("Encoding name", "XSAMPA", disc2xsampa.getDestinationEncoding());
      XSAMPA2DISC xsampa2disc = new XSAMPA2DISC();
      assertEquals("Encoding name", "XSAMPA", xsampa2disc.getSourceEncoding());
      assertEquals("Encoding name", "DISC", xsampa2disc.getDestinationEncoding());

      StringBuffer sDISC = new StringBuffer(aCodebook.length/2);
      StringBuffer sXSAMPA = new StringBuffer(3*aCodebook.length/2);
      // for each pair of elements into the array
      for (int i = 0; i < aCodebook.length; i += 2)
      {
	 // check  mapping
	 assertEquals("DISC phoneme " + aCodebook[i],
                      aCodebook[i+1], disc2xsampa.apply(aCodebook[i]));
	 assertEquals("XSAMPA phoneme " + aCodebook[i+1],
                      aCodebook[i], xsampa2disc.apply(aCodebook[i+1]));
         
	 // accumulate the whole set into strings
	 sDISC.append(aCodebook[i]);
	 sXSAMPA.append(aCodebook[i+1]);
      } // next pair
      
      assertEquals("DISC 'word'",
                   sXSAMPA.toString(), disc2xsampa.apply(sDISC.toString()));
      assertEquals("XSAMPA 'word'",
                   sDISC.toString(), xsampa2disc.apply(sXSAMPA.toString()));

      // check dangerous cases
      assertEquals("Sweatshops with syllable marking",
                   "swEt-SQps", xsampa2disc.apply("swEt-SQps"));
      // without syllable marking, it goes wrong
      assertEquals("Sweatshops without syllable marking - known failure",
                   "swEJQps", xsampa2disc.apply("swEtSQps"));
      assertEquals("strange",
                   "str\\eIndZ", disc2xsampa.apply("str1n_"));
      assertEquals("transcription",
                   "str1n_", xsampa2disc.apply("str\\eIndZ"));

      
   }
   
   public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nzilbb.encoding.TestDISC2XSAMPA2DISC");
   }
}
