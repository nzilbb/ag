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

public class TestSemanticVersionComparator {
  
  /** Ensure basic comparisons like "1.2.3" &lt; "3.2.1" work. */
  @Test public void basicSemanticComparison() throws Exception {
    SemanticVersionComparator c = new SemanticVersionComparator();
    assertTrue("equal", c.compare("1.2.3", "1.2.3") == 0);
    assertTrue("less", c.compare("1.2.3", "3.2.1") < 0);
    assertTrue("more", c.compare("3.2.1", "1.2.3") > 0);
    assertTrue("less - non-ascii order", c.compare("2.3.4", "10.2.1") < 0);
    assertTrue("more - non-ascii order", c.compare("10.2.1", "2.3.4") > 0);
  }
   
  /** Ensure suffixed comparisons like "1.2.3-SNAPSHOT" &lt; "1.2.3" work. */
  @Test public void semanticWithSuffixes() throws Exception {
    SemanticVersionComparator c = new SemanticVersionComparator();
    assertTrue("equal", c.compare("1.2.3-SNAPSHOT", "1.2.3-SNAPSHOT") == 0);
    assertTrue("less", c.compare("1.2.3-SNAPSHOT", "1.2.3") < 0);
    assertTrue("more", c.compare("1.2.3", "1.2.3-SNAPSHOT") > 0);
    assertTrue("less - non-ascii order", c.compare("2.3.4-SNAPSHOT", "10.2.1") < 0);
    assertTrue("more - non-ascii order", c.compare("10.2.1-SNAPSHOT", "2.3.4") > 0);
  }
   
  /** Ensure R-style hyphened comparisons like "1.2-3" &lt; "3.2-1" work. */
  @Test public void basicSemanticComparisonRStyle() throws Exception {
    SemanticVersionComparator c = new SemanticVersionComparator();
    assertTrue("equal", c.compare("1.2-3", "1.2-3") == 0);
    assertTrue("less", c.compare("1.2-3", "3.2-1") < 0);
    assertTrue("more", c.compare("3.2-1", "1.2-3") > 0);
    assertTrue("less - non-ascii order", c.compare("2.3-4", "10.2-1") < 0);
    assertTrue("more - non-ascii order", c.compare("10.2-1", "2.3-4") > 0);
  }
   
  /** Ensure mixed-style comparisons like "20210329.1700" &lt; "1.2.3" work as expected. */
  @Test public void semanticWithNonSemantic() throws Exception {
    SemanticVersionComparator c = new SemanticVersionComparator();
    assertTrue("less", c.compare("20210329.1700", "1.2.3") < 0);
    assertTrue("more", c.compare("1.2.3", "20210329.1700") > 0);
  }
   
  /** Ensure non-semantic comparisons like "20210329.1700" &lt; "20210329.1701" work. */
  @Test public void nonSemantic() throws Exception {
    SemanticVersionComparator c = new SemanticVersionComparator();
    assertTrue("equal", c.compare("20210329.1700", "20210329.1700") == 0);
    assertTrue("less", c.compare("20210329.1700", "20210329.1701") < 0);
    assertTrue("more", c.compare("20210329.1701", "20210329.1700") > 0);

    assertTrue("less - amost semantic", c.compare("10.2", "2.1") < 0);
    assertTrue("more - amost semantic", c.compare("2.1", "10.2") > 0);
  }
   
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.util.TestSemanticVersionComparator");
  }
}
