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

import nzilbb.encoding.DISC2Unisyn;
import nzilbb.encoding.Unisyn2DISC;

public class TestDISC2Unisyn2DISC {
   
   @Test public void codebook() throws Exception {
      // Unisyn is a little one-to-one to DISC      
      String[] aCodebook = {
	 "E", /* <-> */ "e", // DRESS
	 "{", /* <-> */ "a", // TRAP
	 "#", /* <-> */ "ah", // BATH
	 "5", /* <-> */ "ou", // GOAT - but a monophthong for edi
	 "Q", /* <-> */ "o", // LOT
	 "$", /* <-> */ "oo", // THOUGHT (but a diphthong in some en-US)
	 "i", /* <-> */ "ii", // FLEECE
	 "I", /* <-> */ "i", // KIT
	 "@", /* <-> */ "@", // schwa
	 "V", /* <-> */ "uh", // STRUT
	 "U", /* <-> */ "u", // FOOT
	 "u", /* <-> */ "uu", // GOOSE
	 "1", /* <-> */ "ei", // FACE
	 "2", /* <-> */ "ai", // PRICE
	 "4", /* <-> */ "oi", // CHOICE
	 "6", /* <-> */ "ow", // MOUTH
	 "7", /* <-> */ "i@", // NEAR
	 "3", /* <-> */ "@@r", // NURSE
	 "8", /* <-> */ "eir", // SQUARING (actually a monophthong in many)
	 "9", /* <-> */ "ur", // JURY

	 "'", /* <-> */ "*", // primary stress

	 "p", /* <-> */ "p",
	 "t", /* <-> */ "t",
	 "?", /* <-> */ "?", // glottal stop
	 "k", /* <-> */ "k",
	 "b", /* <-> */ "b",
	 "d", /* <-> */ "d",
	 "g", /* <-> */ "g",
	 "s", /* <-> */ "s",
	 "z", /* <-> */ "z",
	 "f", /* <-> */ "f",
	 "v", /* <-> */ "v",
	 "h", /* <-> */ "h",
	 "m", /* <-> */ "m",
	 "n", /* <-> */ "n",
	 "l", /* <-> */ "l",
	 "r", /* <-> */ "r",
	 "j", /* <-> */ "y",
	 "w", /* <-> */ "w",
	 "x", /* <-> */"x", // loch

	 "\"", /* <-> */ "~", // secondary stress
	 "-", /* <-> */ ".", // syllable boundary

	 "J", /* <-> */ "ch",
	 "_", /* <-> */ "jh",
	 "S", /* <-> */ "sh",
	 "Z", /* <-> */ "zh",
	 "T", /* <-> */ "th",
	 "D", /* <-> */ "dh",
	 "L", /* <-> */ "t^", // butter/merry flap
	 "F", /* <-> */ "m!", // chasm
	 "H", /* <-> */ "n!", // mission
	 "N", /* <-> */ "ng", 
	 "P", /* <-> */ "l!", // cattle

	 ",", /* <-> */ "-", // tertiary stress

	 // unknown phones
	 "X", /* <-> */"X", // ASCII letter
	 "/", /* <-> */"/", // ASCII non-letter
	 "ö", /* <-> */"ö" // UNICODE letter
      };

      DISC2Unisyn disc2unisyn = new DISC2Unisyn();
      assertEquals("Encoding name", "DISC", disc2unisyn.getSourceEncoding());
      assertEquals("Encoding name", "Unisyn", disc2unisyn.getDestinationEncoding());
      Unisyn2DISC unisyn2disc = new Unisyn2DISC();
      assertEquals("Encoding name", "Unisyn", unisyn2disc.getSourceEncoding());
      assertEquals("Encoding name", "DISC", unisyn2disc.getDestinationEncoding());

      StringBuffer sDISC = new StringBuffer(aCodebook.length/2);
      StringBuffer sUnisyn = new StringBuffer(3*aCodebook.length/2);
      // for each pair of elements into the array
      for (int i = 0; i < aCodebook.length; i += 2)
      {
	 // check  mapping
	 assertEquals("DISC phoneme " + aCodebook[i],
                      aCodebook[i+1], disc2unisyn.apply(aCodebook[i]));
	 assertEquals("Unisyn phoneme " + aCodebook[i+1],
                      aCodebook[i], unisyn2disc.apply(aCodebook[i+1]));
         
	 // accumulate the whole set into strings
	 sDISC.append(aCodebook[i]);
	 if (sUnisyn.length() > 0) sUnisyn.append(" ");
	 sUnisyn.append(aCodebook[i+1]);
      } // next pair
      
      assertEquals("DISC 'word'",
                   sUnisyn.toString(), disc2unisyn.apply(sDISC.toString()));
      assertEquals("Unisyn 'word'",
                   sDISC.toString(), unisyn2disc.apply(sUnisyn.toString()));
   }
   
   @Test public void missingFromUnisyn() throws Exception {
      // Unisyn is a little one-to-one to DISC      
      String[] aCodebook = {
	 "c", /* -> */"o", /* -> */ "Q", // timbre
	 "q", /* -> */"o", /* -> */ "Q", // detente
	 "0", /* -> */"o", /* -> */ "Q", // lingerie
	 "~", /* -> */"o", /* -> */ "Q", // bouillon
	 "C", /* -> */"ng", /* -> */ "N" // bacon
      };

      DISC2Unisyn disc2unisyn = new DISC2Unisyn();
      Unisyn2DISC unisyn2disc = new Unisyn2DISC();

      StringBuffer sDISCBefore = new StringBuffer(aCodebook.length/3);
      StringBuffer sUnisyn = new StringBuffer(3*aCodebook.length/2);
      StringBuffer sDISCAfter = new StringBuffer(aCodebook.length/3);
      // for each pair of elements into the array
      for (int i = 0; i < aCodebook.length; i += 3)
      {
	 // check  mapping
	 assertEquals("DISC ("+i+") " + aCodebook[i],
                      aCodebook[i+1], disc2unisyn.apply(aCodebook[i]));
	 assertEquals("Unisyn ("+i+") " + aCodebook[i+1],
                      aCodebook[i+2], unisyn2disc.apply(aCodebook[i+1]));
         
	 // accumulate the whole set into strings
	 sDISCBefore.append(aCodebook[i]);
	 if (sUnisyn.length() > 0 && aCodebook[i+1].length() > 0) sUnisyn.append(" ");
	 sUnisyn.append(aCodebook[i+1]);
	 sDISCAfter.append(aCodebook[i+2]);
      } // next pair

      assertNotEquals("Before and after are expected to be different", sDISCBefore, sDISCAfter);
      assertEquals("before",
                   sUnisyn.toString(), disc2unisyn.apply(sDISCBefore.toString()));
      assertEquals("after",
                   sDISCAfter.toString(), unisyn2disc.apply(sUnisyn.toString()));
   }
   
   @Test public void missingFromDISC() throws Exception {
      // Unisyn is a little one-to-one to DISC      
      String[] aCodebook = {
	 "ar",   /* -> */ "Q", /* -> */ "o", // start -> LOT
	 "aa",   /* -> */ "Q", /* -> */ "o", // PALM -> LOT
	 "oa",   /* -> */ "{", /* -> */ "a", // BANANA -> TRAP
	 "ao",   /* -> */ "#", /* -> */ "ah", // MAZDA -> BATH
	 "eh",   /* -> */ "{", /* -> */ "a", // ann use TRAP
	 "oul",  /* -> */ "5", /* -> */ "ou", // goal - post vocalic GOAT
	 "ouw",  /* -> */ "5", /* -> */ "ou", // KNOW -> GOAT (except for Abergave)
	 "oou",  /* -> */ "Q", /* -> */ "o", // adios -> LOT
	 "au",   /* -> */ "Q", /* -> */ "o", // CLOTH -> LOT (but a diphthong in some en-US)
	 "or",   /* -> */ "$", /* -> */ "oo", // r-coloured THOUGHT
	 "iy",   /* -> */ "i", /* -> */ "ii", // HAPPY - I for some varieties
	 "ie",   /* -> */ "i", /* -> */ "ii", // HARRIET - Leeds only
	 "ii;",  /* -> */ "i", /* -> */ "ii", // AGREED -> FLEECE
	 "@r",   /* -> */ "@", /* -> */ "@", // r-coloured schwa
	 "iu",   /* -> */ "u", /* -> */ "uu", // BLEW -> GOOSE
	 "uu;",  /* -> */ "u", /* -> */ "uu", // brewed -> GOOSE
	 "uw",   /* -> */ "u", /* -> */ "uu", // louise -> GOOSE
	 "uul",  /* -> */ "u", /* -> */ "uu", // goul - post-vocalic GOOSE
	 "ee",   /* -> */ "1", /* -> */ "ei", // WASTE -> FACE (except for abercrave)
	 "ae",   /* -> */ "2", /* -> */ "ai", // TIED -> PRICE (except Edi and Aberdeen)
	 "ae",   /* -> */ "2", /* -> */ "ai", // TIED -> PRICE (except Edi and Aberdeen)
	 "aer",  /* -> */ "2", /* -> */ "ai", // FIRE - r-coloured PRICE
	 "aai",  /* -> */ "2", /* -> */ "ai", // TIME -> PRICE (except S. Carolina)
	 "oir",  /* -> */ "2", /* -> */ "ai", // COIR - r-coloured PRICE
	 "owr",  /* -> */ "6", /* -> */ "ow", // HOUR - r-coloured MOUTH
	 "oow",  /* -> */ "6", /* -> */ "ow", // HOUR -> MOUTH (exception S. Carolina)
	 "ir",   /* -> */ "i", /* -> */ "ii", // NEARING - r-coloured NEAR -> FLEECE
	 "ir;",  /* -> */ "i", /* -> */ "ii", // near - scots-long NEAR -> FLEECE
	 "iir",  /* -> */ "7", /* -> */ "i@", // beard -> NEAR (except en-AU)
	 "er",   /* -> */ "E", /* -> */ "e", // r-coloured DRESS in scots en
	 "ur;",  /* -> */ "9", /* -> */ "ur", // CURE - scots-long JURY
	 "iur",  /* -> */ "9", /* -> */ "ur", // curious - JURY exception in Cardiff & Abercrave

	 "hw",   /* -> */ "w", /* -> */ "w", // which
	 "ll",   /* -> */ "l", /* -> */ "l", // llandudno (except Cardiff and Abercrave)
	 "lw",   /* -> */ "l", /* -> */ "l" // feel - dark l
      };

      DISC2Unisyn disc2unisyn = new DISC2Unisyn();
      Unisyn2DISC unisyn2disc = new Unisyn2DISC();

      StringBuffer sUnisynBefore = new StringBuffer(aCodebook.length/3);
      StringBuffer sDISC = new StringBuffer(3*aCodebook.length/2);
      StringBuffer sUnisynAfter = new StringBuffer(aCodebook.length/3);
      // for each pair of elements into the array
      for (int i = 0; i < aCodebook.length; i += 3)
      {
	 // check  mapping
	 assertEquals("DISC ("+i+") " + aCodebook[i],
                      aCodebook[i+1], unisyn2disc.apply(aCodebook[i]));
	 assertEquals("Unisyn ("+i+")" + aCodebook[i+1],
                      aCodebook[i+2], disc2unisyn.apply(aCodebook[i+1]));
         
	 // accumulate the whole set into strings
	 if (sUnisynBefore.length() > 0 && aCodebook[i].length() > 0) sUnisynBefore.append(" ");
	 sUnisynBefore.append(aCodebook[i]);
	 sDISC.append(aCodebook[i+1]);
	 if (sUnisynAfter.length() > 0 && aCodebook[i+1].length() > 0) sUnisynAfter.append(" ");
	 sUnisynAfter.append(aCodebook[i+2]);
      } // next pair

      assertNotEquals("Before and after are expected to be different",
                      sUnisynBefore, sUnisynAfter);
      assertEquals("before",
                   sDISC.toString(), unisyn2disc.apply(sUnisynBefore.toString()));
      assertEquals("after",
                   sUnisynAfter.toString(), disc2unisyn.apply(sDISC.toString()));
   }
   
   public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nzilbb.encoding.test.TestDISC2Unisyn2DISC");
   }
}
