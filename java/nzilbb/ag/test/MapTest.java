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

package nzilbb.ag.test;

import java.util.Set;
import java.util.LinkedHashSet;
import nzilbb.ag.*;
import nzilbb.ag.TrackedProperty;
import nzilbb.util.ClonedProperty;

// subclass that defines some tracked attributes
@SuppressWarnings("serial")
public class MapTest extends TrackedMap
{
   static String[] aTrackedAttributes = {"tracked2", "tracked1", "tracked3"};
   // Using LinkedHashSet ensures that the attributes will be iterated in the order above
   static final LinkedHashSet<String> trackedAttributes = new LinkedHashSet<String>(java.util.Arrays.asList(aTrackedAttributes));

   public Set<String> getTrackedAttributes() { return trackedAttributes; }
   
   protected String tracked1;
   @TrackedProperty @ClonedProperty
   public String getTracked1() { return tracked1; }
   public void setTracked1(String newTracked1)
   {
      registerChange("tracked1", newTracked1);
      tracked1 = newTracked1;
   }
   
   protected String tracked2;
   @TrackedProperty @ClonedProperty
   public String getTracked2() { return tracked2; }
   public void setTracked2(String newTracked2)
   {
      registerChange("tracked2", newTracked2);
      tracked2 = newTracked2;
   }
   
   protected String tracked3;
   @TrackedProperty @ClonedProperty
   public String getTracked3() { return tracked3; }
   public void setTracked3(String newTracked3)
   {
      registerChange("tracked3", newTracked3);
      tracked3 = newTracked3;
   }
   
   protected String notTracked;
   public String getNotTracked() { return notTracked; }
   public void setNotTracked(String newNotTracked) { notTracked = newNotTracked; }
}
