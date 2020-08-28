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

package nzilbb.encoding.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

import nzilbb.encoding.DISC2ARPAbet;
import nzilbb.encoding.ARPAbet2DISC;

public class TestDISC2ARPAbet2DISC {
   
   @Test public void codebook() throws Exception {
      // ARPAbet is a little one-to-one to DISC      
      String[] aCodebook = {
	 "#", /* <-> */"AA", // BATH        - odd/father
	 "{", /* <-> */"AE", // TRAP        - at/fast
	 "V", /* <-> */"AH", // STRUT       - hut/but
	 "$", /* <-> */"AO", // THOUGHT     - ought/fall
	 "6", /* <-> */"AW", // MOUTH       - cow/how
	 "@", /* <-> */"AX", // schwa       - discuss
	 "2", /* <-> */"AY", // PRICE       - hide/my
	 "b", /* <-> */"B",
	 "J", /* <-> */"CH",
	 "d", /* <-> */"D",
	 "D", /* <-> */"DH",
	 "E", /* <-> */"EH", // DRESS       - Ed/red
	 "3", /* <-> */"ER", // NURSE       - hurt/her
	 "1", /* <-> */"EY", // FACE        - ate/say
	 "f", /* <-> */"F", 
	 "g", /* <-> */"G",
	 "h", /* <-> */"HH",
	 "I", /* <-> */"IH", // KIT         - it/big
	 "i", /* <-> */"IY", // FLEECE      - eat/bee
	 "_", /* <-> */"JH",
	 "k", /* <-> */"K",
	 "l", /* <-> */"L",
	 "m", /* <-> */"M",
	 "n", /* <-> */"N",
	 "N", /* <-> */"NG",
	 "5", /* <-> */"OW", // GOAT        - oat/show
	 "4", /* <-> */"OY", // CHOICE      - toy/boy
	 "p", /* <-> */"P",
	 "r", /* <-> */"R",
	 "s", /* <-> */"S",
	 "S", /* <-> */"SH",
	 "t", /* <-> */"T",
	 "T", /* <-> */"TH",
	 "U", /* <-> */"UH", // FOOT        - hood/should
	 "u", /* <-> */"UW", // GOOSE       - two/you
	 "v", /* <-> */"V",
	 "w", /* <-> */"W",
	 "j", /* <-> */"Y",
	 "z", /* <-> */"Z",
	 "Z", /* <-> */"ZH",
	 
	 // Not in the CMU set but exist in Buckeye corpus
	 "L", /* <-> */"DX", // flap - this is an extension to DISC
	 "^", /* <-> */"NX", // nasal flap - doesn't exist in DISC, we make it /n/
	 "?", /* <-> */"TQ", // glottal stop - this is an extension to DISC
	 
	 // Syllabics not in ARPAbet set but exist in DISC
	 "F", /* <-> */"EM", // idealism
	 "H", /* <-> */"EN", // burden
	 "P", /* <-> */"EL", // dangle
	 "C", /* <-> */"UN", // bacon
	 "0", /* <-> */"VN", // lingerie
	 "~", /* <-> */"ON", // bouillon
	 "c", /* <-> */"IM", // timbre
	 "q", /* <-> */"IN", // detente

	 // unknown phones
	 "x", /* <-> */"x", // ASCII letter
	 "X", /* <-> */"X", // ASCII letter
	 "/", /* <-> */"/", // ASCII non-letter
	 "ö", /* <-> */"ö" // UNICODE letter
      };

      DISC2ARPAbet disc2arpabet = new DISC2ARPAbet();
      ARPAbet2DISC arpabet2disc = new ARPAbet2DISC();

      StringBuffer sDISC = new StringBuffer(aCodebook.length/2);
      StringBuffer sARPAbet = new StringBuffer(3*aCodebook.length/2);
      // for each pair of elements into the array
      for (int i = 0; i < aCodebook.length; i += 2)
      {
	 // check  mapping
	 assertEquals("DISC phoneme " + aCodebook[i],
                      aCodebook[i+1], disc2arpabet.apply(aCodebook[i]));
	 assertEquals("ARPAbet phoneme " + aCodebook[i+1],
                      aCodebook[i], arpabet2disc.apply(aCodebook[i+1]));
         
	 // accumulate the whole set into strings
	 sDISC.append(aCodebook[i]);
	 if (sARPAbet.length() > 0) sARPAbet.append(" ");
	 sARPAbet.append(aCodebook[i+1]);
      } // next pair
      
      assertEquals("DISC 'word'",
                   sARPAbet.toString(), disc2arpabet.apply(sDISC.toString()));
      assertEquals("ARPAbet 'word'",
                   sDISC.toString(), arpabet2disc.apply(sARPAbet.toString()));
   }
   
   @Test public void roundTripExceptions() throws Exception {
      // ARPAbet is a little one-to-one to DISC      
      String[] aCodebook = {
	 "R", /* -> */"R",    /* -> */ "r",  // possible linking R is pretty definitely R
	 "Q", /* -> */ "AO",  /* -> */ "$",  // LOT         - ought/off
	 "7", /* -> */"IY R", /* -> */ "ir", // NEAR
	 "8", /* -> */"EH R", /* -> */ "Er", // SQUARE
	 "9", /* -> */"UH R", /* -> */ "Ur",  // CURE

	 "kj9R", /* -> */  "K Y UH R", /* not K Y UH R R */ /* -> */ "kjUr",  // linking/doubled r
	 "kj9rIN", /* -> */  "K Y UH R IH NG",              /* -> */ "kjUrIN",// linking/doubled r
      };

      DISC2ARPAbet disc2arpabet = new DISC2ARPAbet();
      ARPAbet2DISC arpabet2disc = new ARPAbet2DISC();

      StringBuffer sDISCBefore = new StringBuffer(aCodebook.length/3);
      StringBuffer sARPAbet = new StringBuffer(3*aCodebook.length/2);
      StringBuffer sDISCAfter = new StringBuffer(aCodebook.length/3);
      // for each pair of elements into the array
      for (int i = 0; i < aCodebook.length; i += 3)
      {
	 // check  mapping
	 assertEquals("DISC ("+i+") " + aCodebook[i],
                      aCodebook[i+1], disc2arpabet.apply(aCodebook[i]));
	 assertEquals("ARPAbet ("+i+")" + aCodebook[i+1],
                      aCodebook[i+2], arpabet2disc.apply(aCodebook[i+1]));
         
	 // accumulate the whole set into strings
	 sDISCBefore.append(aCodebook[i]);
	 if (sARPAbet.length() > 0 && aCodebook[i+1].length() > 0) sARPAbet.append(" ");
	 sARPAbet.append(aCodebook[i+1]);
	 sDISCAfter.append(aCodebook[i+2]);
      } // next pair

      assertNotEquals("Before and after are expected to be different", sDISCBefore, sDISCAfter);
      assertEquals("before",
                   disc2arpabet.apply(sARPAbet.toString()), sDISCBefore.toString());
      assertEquals("after",
                   arpabet2disc.apply(sDISCAfter.toString()), sARPAbet.toString());
   }
   
   public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nzilbb.encoding.test.TestDISC2ARPAbet2DISC");
   }
}
