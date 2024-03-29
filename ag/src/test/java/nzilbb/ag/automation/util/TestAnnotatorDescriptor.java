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

package nzilbb.ag.automation.util;
	      
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
      assertFalse("No ext webapp",
                   d.hasExtWebapp());
      assertTrue("Info is accessible",
                 d.getInfo().startsWith("<html><head><title>MinimalExample"));
      assertNull("No config parameter info is accessible",
                 d.getConfigParameterInfo());
      assertNull("No task parameter info is accessible",
                 d.getTaskParameterInfo());
      assertNull("No ext API info is accessible",
                 d.getExtApiInfo());
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
      assertTrue("ext webapp detected",
                  d.hasExtWebapp());
      assertTrue("Info is accessible",
                 d.getInfo().startsWith("<!DOCTYPE html>\n<html>\n  <head>\n    <title>TheWorksExample"));
      assertTrue("Config parameter info is accessible",
                 d.getConfigParameterInfo().startsWith("<!DOCTYPE html>\n<html>\n  <head>\n    <title>Configuration Parameters"));
      assertTrue("Task parameter info is accessible",
                 d.getTaskParameterInfo().startsWith("<!DOCTYPE html>\n<html>\n  <head>\n    <title>Task Parameters"));
      assertTrue("Ext API info is accessible",
                 d.getExtApiInfo().startsWith("<!DOCTYPE html>\n<html>\n  <head>\n    <title>Extended API"));
   }

   public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nzilbb.ag.automation.util.TestAnnotatorDescriptor");
   }
}
