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

package nzilbb.ag.automation.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.List;
import nzilbb.ag.Change;
import nzilbb.ag.Graph;
import nzilbb.ag.TransformationException;
import nzilbb.ag.automation.*;
import nzilbb.ag.automation.example.minimal.MinimalExample;
import nzilbb.ag.automation.example.theworks.TheWorksExample;
import nzilbb.ag.automation.util.*;

public class TestAnnotator {
   
   @Test public void beanPropertiesToQueryString() throws Exception {
      
      TheWorksExample annotator = new TheWorksExample()
         .setPrefix(" untrimmed prefix ")
         .setLeftPadding(50)
         .setRightPadding(100);
      String config = annotator.getConfig();
      assertTrue("query string is correct for String",
                 config.indexOf("prefix=+untrimmed+prefix+") >= 0);
      assertTrue("query string is correct for int",
                 config.indexOf("leftPadding=50") >= 0);
      assertTrue("query string is correct for Integer",
                 config.indexOf("rightPadding=100") >= 0);
      assertFalse("null values excluded - Boolean",
                  config.indexOf("reverse") >= 0);
      assertFalse("null values excluded - Double",
                  config.indexOf("labelConfidence") >= 0);
      assertTrue("& separator",
                 config.indexOf("&") >= 0);
      assertFalse("& not a prefix",
                 config.startsWith("&"));
      
      annotator.setReverse(true);
      assertTrue("query string is correct for Boolean",
                 annotator.getConfig().indexOf("reverse=true") >= 0);

      annotator.setLabelConfidence(0.75);
      assertTrue("query string is correct for Double",
                 annotator.getConfig().indexOf("labelConfidence=0.75") >= 0);

   }

   @Test public void beanPropertiesToQueryStringNoProperties() throws Exception {

      // subclass TheWorksExample with no getters/setters
      Annotator annotator = new TheWorksExample() {};
      assertEquals("no query string", "", annotator.getConfig());
   }

   @Test public void beanPropertiesFromQueryStringNull() throws Exception {

      // subclass TheWorksExample with no getters/setters
      TheWorksExample annotator = new TheWorksExample();
      annotator.setConfig(null);
      assertEquals("default values - leftPadding", 1, annotator.getLeftPadding());
      assertEquals("default values - rightPadding",
                   Integer.valueOf(2), annotator.getRightPadding());
   }

   @Test public void beanPropertiesFromQueryString() throws Exception {

      // subclass TheWorksExample with no getters/setters
      TheWorksExample annotator = new TheWorksExample();
      annotator.setConfig(
         "leftPadding=2&rightPadding=1&reverse=true&prefix=+untrimmed+&labelConfidence=0.25"
         +"&leftPadding&ignore encoded equals%3Dprefix%3Ddecoded equals"
         +"&ignore encoded ampsersand%26prefix=decoded ampersand"
         +"&trailing&");
      assertEquals("int", 2, annotator.getLeftPadding());
      assertEquals("Integer", Integer.valueOf(1), annotator.getRightPadding());
      assertEquals("Boolean", Boolean.TRUE, annotator.getReverse());
      assertEquals("String", " untrimmed ", annotator.getPrefix());
      assertEquals("Double", Double.valueOf(0.25), annotator.getLabelConfidence());
   }

   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.automation.test.TestAnnotator");
   }
}