//
// Copyright 2022 New Zealand Institute of Language, Brain and Behaviour, 
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

import nzilbb.encoding.IPA2DISC;

public class TestIPA2DISC {

  /** Ensure that two-way DISC2IPA transcriptions are correctly converted.  */
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
      "g", /* <-> */"ɡ",
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

    IPA2DISC translatorDelimiter = new IPA2DISC().setDelimiter(" ");
    IPA2DISC translatorNoDelimiter = new IPA2DISC();
    assertEquals("Encoding name", "IPA", translatorDelimiter.getSourceEncoding());
    assertEquals("Encoding name", "DISC", translatorDelimiter.getDestinationEncoding());

    StringBuffer sDISC = new StringBuffer(aCodebook.length/2);
    StringBuffer sIPADelimited = new StringBuffer(3*aCodebook.length/2);
    // for each pair of elements into the array
    for (int i = 0; i < aCodebook.length; i += 2) {
      // check  mapping
      assertEquals("IPA (delim) " + aCodebook[i+1],
                   aCodebook[i], translatorDelimiter.apply(aCodebook[i+1]));
      assertEquals("IPA (no delim) " + aCodebook[i+1],
                   aCodebook[i], translatorNoDelimiter.apply(aCodebook[i+1]));
      
      // accumulate the whole set into strings
      sDISC.append(aCodebook[i]);
      if (sIPADelimited.length() > 0) sIPADelimited.append(" ");
      sIPADelimited.append(aCodebook[i+1]);
    } // next pair
    String sIPANoDelimiter = sIPADelimited.toString().replace(" ","");
    
    assertEquals("No delimiter",
                 sDISC.toString(), translatorNoDelimiter.apply(sIPANoDelimiter));
    assertEquals("Space delimiter",
                 sDISC.toString(), translatorDelimiter.apply(sIPADelimited.toString()));
  }

  
  @Test public void oneway() { 
    IPA2DISC translator = new IPA2DISC().setDelimiter(" ");
    assertEquals("LATIN SMALL LETTER TURNED E instead of schwa",
                 "@5789",
                 translator.apply("ǝ ǝʊ ɪǝ ɛǝ ʊǝ"));

    assertEquals("MFA labels",
                 "bjdj_fjkmjppjttjJvj@AQ$j@wE3jglP}j",
                 translator.apply("bʲ dʲ dʒ fʲ kʰ mʲ pʰ pʲ tʰ tʲ tʃ vʲ ɐ ɑ ɒː ɔj əw ɛ ɜ ɟ ɡ ɫ ɫ̩ ʉː ʎ"));

  }
   
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.encoding.TestIPA2DISC");
  }
}
