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

package nzilbb.ag.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import nzilbb.ag.*;

public class TestConstants {
   @Test public void versionExtraction() {
      assertNotNull("Constants.VERSION is set ",
                    Constants.VERSION);
      assertTrue("Constants.VERSION a date-version: " + Constants.VERSION,
                 Constants.VERSION.matches("\\d{8}\\.\\d{4}"));
   }

   public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nzilbb.ag.test.TestConstants");
   }
}
