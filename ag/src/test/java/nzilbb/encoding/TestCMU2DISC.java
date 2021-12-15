//
// Copyright 2020-2021 New Zealand Institute of Language, Brain and Behaviour, 
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

import nzilbb.encoding.CMU2DISC;

public class TestCMU2DISC {

  /** Test that general code-book translations work. */
  @Test public void translation() throws Exception {
    // CMU is a little one-to-one to DISC      
    String[] aCodebook = {
      "#", /* <-> */"AA", // BATH        - odd/father
      "{", /* <-> */"AE", // TRAP        - at/fast
      "V", /* <-> */"AH", // STRUT       - hut/but
      "$", /* <-> */"AO", // THOUGHT     - ought/fall
      "6", /* <-> */"AW", // MOUTH       - cow/how
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
      "Z", /* <-> */"ZH"
    };

    CMU2DISC translator = new CMU2DISC();
    assertEquals("Encoding name", "CMU", translator.getSourceEncoding());
    assertEquals("Encoding name", "DISC", translator.getDestinationEncoding());

    StringBuffer sDISC = new StringBuffer(aCodebook.length/2);
    StringBuffer sCMU = new StringBuffer(3*aCodebook.length/2);
    // for each pair of elements into the array
    for (int i = 0; i < aCodebook.length; i += 2) {
      // check  mapping
      assertEquals("CMU " + aCodebook[i+1], translator.apply(aCodebook[i+1]), aCodebook[i]);
         
      // accumulate the whole set into strings
      sDISC.append(aCodebook[i]);
      if (sCMU.length() > 0) sCMU.append(" ");
      sCMU.append(aCodebook[i+1]);
    } // next pair
      
    assertEquals(sDISC.toString(), translator.apply(sCMU.toString()));
  }

  /** Check ARPAbet stress=0 vowels are convert to schwa in DISC */
  @Test public void schwa() throws Exception {
    CMU2DISC translator = new CMU2DISC();
    assertEquals("Default zeroStressToSchwa=true",
                 "m3d@", translator.apply("M ER1 D ER0"));
    translator.setZeroStressToSchwa(false);
    assertEquals("zeroStressToSchwa=false",
                 "m3d3", translator.apply("M ER1 D ER0"));
  }  
   
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.encoding.TestCMU2DISC");
  }
}
