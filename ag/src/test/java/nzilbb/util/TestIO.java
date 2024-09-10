//
// Copyright 2017-2019 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.util;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;
import nzilbb.util.IO;

public class TestIO {
   
  @Test public void Extension() throws Exception {
    assertEquals("Simple case",
                 "ext", IO.Extension("name.ext"));
    assertEquals("Multiple dots",
                 "TextGrid", IO.Extension("something__1.234-5.678.TextGrid"));
    assertEquals("Consecutive dots",
                 "trs", IO.Extension("something..trs"));
    assertEquals("No dot = no extension",
                 "", IO.Extension("something."));
    assertEquals("Ending dot = no extension",
                 "", IO.Extension("something."));
    assertEquals("Graph fragment suffix",
                 "", IO.Extension("something__1.234-5.678"));
  }
  @Test public void WithoutExtension() throws Exception {
    assertEquals("Simple case",
                 "name", IO.WithoutExtension("name.ext"));
    assertEquals("Including digit",
                 "video", IO.WithoutExtension("video.mp4"));
    assertEquals("Multiple dots",
                 "something__1.234-5.678",
                 IO.WithoutExtension("something__1.234-5.678.TextGrid"));
    assertEquals("Consecutive dots",
                 "something.", IO.WithoutExtension("something..trs"));
    assertEquals("No dot = no extension",
                 "something", IO.WithoutExtension("something"));
    assertEquals("Ending dot = no extension",
                 "something.", IO.WithoutExtension("something."));
    assertEquals("Graph fragment suffix",
                 "something__1.234-5.678", IO.WithoutExtension("something__1.234-5.678"));
  }
   
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.util.TestIO");
  }
}
