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

package nzilbb.ag.automation.util.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import nzilbb.ag.automation.*;
import nzilbb.ag.automation.util.*;
import nzilbb.ag.automation.example.minimal.MinimalExample;
import nzilbb.ag.automation.example.theworks.TheWorksExample;

public class TestAnnotatorDescriptor {
   
   @Test public void minimalExample() throws Exception {
      
      AnnotatorDescriptor d = new AnnotatorDescriptor(
         "nzilbb.ag.automation.example.minimal.MinimalExample", getClass().getClassLoader());
      assertEquals("annotatorClassName correct",
                   "nzilbb.ag.automation.example.minimal.MinimalExample",
                   d.getAnnotatorClassName());
      assertEquals("Annotator class name correct",
                   "nzilbb.ag.automation.example.minimal.MinimalExample",
                   d.getAnnotatorClass().getName());
      assertTrue("Annotator instance type correct",
                 d.getInstance() instanceof nzilbb.ag.automation.example.minimal.MinimalExample);
      assertFalse("No config webapp",
                   d.hasConfigWebapp());
      assertFalse("No task webapp",
                   d.hasTaskWebapp());
      assertTrue("Info is accessible",
                 d.getInfo().startsWith("<html><head><title>MinimalExample"));
   }

   @Test public void theWorksExample() throws Exception {
      
      AnnotatorDescriptor d = new AnnotatorDescriptor(
         "nzilbb.ag.automation.example.theworks.TheWorksExample", getClass().getClassLoader());
      assertEquals("annotatorClassName correct",
                   "nzilbb.ag.automation.example.theworks.TheWorksExample",
                   d.getAnnotatorClassName());
      assertEquals("Annotator class name correct",
                   "nzilbb.ag.automation.example.theworks.TheWorksExample",
                   d.getAnnotatorClass().getName());
      assertTrue("Annotator instace type correct",
                 d.getInstance() instanceof nzilbb.ag.automation.example.theworks.TheWorksExample);
      assertTrue("config webapp detected",
                  d.hasConfigWebapp());
      assertTrue("task webapp detected",
                  d.hasTaskWebapp());
      assertTrue("Info is accessible",
                 d.getInfo().startsWith("<html><head><title>TheWorksExample"));
   }

   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.automation.util.test.TestAnnotatorDescriptor");
   }
}
