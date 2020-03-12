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

package nzilbb.converter.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;
import nzilbb.converter.EafToTrs;

public class TestEafToTrs extends UnitTestBase {
   
   @Test public void utterancesOnly() throws Exception {
      File dir = getDir();
      File input = new File(dir, "elan.eaf");
      EafToTrs converter = new EafToTrs();
      converter.convert(input);
      File actual = new File(dir, "elan.trs");
      File expected = new File(dir, "expected_elan.trs");
      String differences = diff(expected, actual);
      if (differences != null) {
         fail(differences);
      } else {
         actual.delete();
      }
   }
   
   @Test public void utterance_word_phone() throws Exception {
      File dir = getDir();
      File input = new File(dir, "elan_word_phone.eaf");
      EafToTrs converter = new EafToTrs();
      converter.convert(input);
      File actual = new File(dir, "elan_word_phone.trs");
      File expected = new File(dir, "expected_elan_word_phone.trs");
      String differences = diff(expected, actual);
      if (differences != null) {
         fail(differences);
      } else {
         actual.delete();
      }
   }

   @Test public void topic() throws Exception {
      File dir = getDir();
      File input = new File(dir, "elan_topic.eaf");
      EafToTrs converter = new EafToTrs();
      converter.convert(input);
      File actual = new File(dir, "elan_topic.trs");
      File expected = new File(dir, "expected_elan_topic.trs");
      String differences = diff(expected, actual);
      if (differences != null) {
         fail(differences);
      } else {
         actual.delete();
      }
   }

   @Test public void withAnnotations() throws Exception {
      File dir = getDir();
      File input = new File(dir, "elan_annotations.eaf");
      EafToTrs converter = new EafToTrs();
      converter.getTiersToIgnore().add("annotations");
      converter.convert(input);
      File actual = new File(dir, "elan_annotations.trs");
      File expected = new File(dir, "expected_elan_annotations.trs");
      String differences = diff(expected, actual);
      if (differences != null) {
         fail(differences);
      } else {
         actual.delete();
      }
   }

   public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nzilbb.converter.test.TestEafToTrs");
   }
}
