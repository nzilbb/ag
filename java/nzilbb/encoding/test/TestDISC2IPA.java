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

import nzilbb.encoding.DISC2IPA;

public class TestDISC2IPA {
   
   @Test public void codebook() throws Exception {
      String[] aCodebook = {
	 "&", /* <-> */"a",
	 "#", /* <-> */"ɑː", // BATH        - odd/father
	 "A", /* <-> */"ɑ",
	 ")", /* <-> */"ɛː",
	 "|", /* <-> */"øː",
	 "^", /* <-> */"œ̃ː",
	 "/", /* <-> */"œ",
	 "=", /* <-> */"t͜s",
	 "+", /* <-> */"pf",
	 "{", /* <-> */"æ", // TRAP        - at/fast
	 "}", /* <-> */"ʉ",
	 "V", /* <-> */"ʌ", // STRUT       - hut/but
	 "$", /* <-> */"ɔː", // THOUGHT     - ought/fall
	 "6", /* <-> */"aʊ", // MOUTH       - cow/how
	 "@", /* <-> */"ə", // schwa       - discuss
	 "2", /* <-> */"aɪ", // PRICE       - hide/my
	 "b", /* <-> */"b",
	 "B", /* <-> */"au",
	 "J", /* <-> */"t͜ʃ",
	 "d", /* <-> */"d",
	 "D", /* <-> */"ð",
	 "E", /* <-> */"ɛ", // DRESS       - Ed/red
	 "3", /* <-> */"ɜː", // NURSE       - hurt/her
	 "1", /* <-> */"eɪ", // FACE        - ate/say
	 "f", /* <-> */"f", 
	 "g", /* <-> */"g",
	 "h", /* <-> */"h",
	 "I", /* <-> */"ɪ", // KIT         - it/big
	 "i", /* <-> */"iː", // FLEECE      - eat/bee
	 "_", /* <-> */"d͜ʒ",
	 "k", /* <-> */"k",
	 "l", /* <-> */"l",
	 "L", /* <-> */"œy",
	 "m", /* <-> */"m",
	 "n", /* <-> */"n",
	 "N", /* <-> */"ŋ",
	 "o", /* <-> */"oː",
	 "O", /* <-> */"ɔ",
	 "5", /* <-> */"əʊ", // GOAT        - oat/show
	 "4", /* <-> */"ɔɪ", // CHOICE      - toy/boy
	 "p", /* <-> */"p",
	 "r", /* <-> */"ɹ",
	 "R", /* <-> */"ɹ", // possible linking R
	 "s", /* <-> */"s",
	 "S", /* <-> */"ʃ",
	 "t", /* <-> */"t",
	 "T", /* <-> */"θ",
	 "U", /* <-> */"ʊ", // FOOT        - hood/should
	 "u", /* <-> */"uː", // GOOSE       - two/you
	 "v", /* <-> */"v",
	 "w", /* <-> */"w",
	 "W", /* <-> */"ai",
	 "X", /* <-> */"ɔy",
	 "j", /* <-> */"j",
	 "y", /* <-> */"yː",
	 "Y", /* <-> */"ʏ",
	 "z", /* <-> */"z",
	 "Z", /* <-> */"ʒ",

	 "Q", /* <-> */"ɒ", // LOT         - ought/off
	 "7", /* <-> */"ɪə", // NEAR
	 "8", /* <-> */"ɛə", // SQUARE
	 "9", /* <-> */"ʊə", // CURE
	 "?", /* <-> */"ʔ", // glottal stop
	 
	 "F", /* <-> */"m̩", // idealism
	 "H", /* <-> */"n̩", // burden
	 "P", /* <-> */"l̩", // dangle
	 "C", /* <-> */"ŋ̩", // bacon
	 "0", /* <-> */"æ̃ː", // lingerie
	 "~", /* <-> */"ɒ̃ː", // bouillon
	 "c", /* <-> */"æ̃", // timbre
	 "q", /* <-> */"ɑ̃ː", // detente

	 // unknown phones
	 "x", /* <-> */"x", // ASCII letter
	 "-", /* <-> */"-", // ASCII non-letter
	 "ö", /* <-> */"ö" // UNICODE letter
      };

      DISC2IPA translator = new DISC2IPA();
      assertEquals("Encoding name", "DISC", translator.getSourceEncoding());
      assertEquals("Encoding name", "IPA", translator.getDestinationEncoding());

      StringBuffer sDISC = new StringBuffer(aCodebook.length/2);
      StringBuffer sIPADelimited = new StringBuffer(3*aCodebook.length/2);
      // for each pair of elements into the array
      for (int i = 0; i < aCodebook.length; i += 2)
      {
	 // check  mapping
	 assertEquals("DISC " + aCodebook[i], translator.apply(aCodebook[i]), aCodebook[i+1]);
         
	 // accumulate the whole set into strings
	 sDISC.append(aCodebook[i]);
	 if (sIPADelimited.length() > 0) sIPADelimited.append(" ");
	 sIPADelimited.append(aCodebook[i+1]);
      } // next pair
      String sIPANoDelimiter = sIPADelimited.toString().replace(" ","");
      
      assertEquals("No delimiter",
                   sIPANoDelimiter, translator.apply(sDISC.toString()));
      translator.setDelimiter(" ");
      assertEquals("Space delimiter",
                   sIPADelimited.toString(), translator.apply(sDISC.toString()));
   }   
   
   public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nzilbb.encoding.test.TestDISC2IPA");
   }
}
