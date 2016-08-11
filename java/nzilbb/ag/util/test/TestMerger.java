//
// Copyright 2015-2016 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.ag.util.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.Vector;
import java.util.Iterator;
import nzilbb.ag.util.*;
import nzilbb.ag.*;

public class TestMerger
{      
   @Test public void identityMerge() 
   {
      Graph g = new Graph();
      Merger m = new Merger();
      m.setDebug(true);
/*
      try
      {
	 Vector<Change> changes = m.transform(g);
	 if (m.getLog() != null) for (String message : m.getLog()) System.out.println(message);
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }
*/
   }

   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.util.test.TestMerger");
   }
}
