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
import nzilbb.converter.TrsToText;

public class TestTrsToText extends UnitTestBase {
   
   @Test public void transcriber() throws Exception {
      File dir = getDir();
      File input = new File(dir, "transcriber.trs");
      TrsToText converter = new TrsToText();
      converter.convert(input);
      File actual = new File(dir, "transcriber.txt");
      File expected = new File(dir, "expected_transcriber.txt");
      String differences = diff(expected, actual);
      if (differences != null) {
         fail(differences);
      } else {
         actual.delete();
      }
   }

   public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nzilbb.converter.test.TestTrsToText");
   }
}
