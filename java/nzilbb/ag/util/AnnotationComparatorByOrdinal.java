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
package nzilbb.ag.util;

import java.util.Comparator;
import nzilbb.ag.Annotation;
import nzilbb.ag.Anchor;

/**
 * Comparator that compares annotations by ordinal if they share a parent.  Otherwise ordering matches {@link AnnotationComparatorByAnchor}.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class AnnotationComparatorByOrdinal // TODO unit test!
   extends AnnotationComparatorByAnchor
{
   // Methods:
   
   /**
    * Default constructor
    */
   public AnnotationComparatorByOrdinal()
   {
   } // end of constructor
   
   public int compare(Annotation o1, Annotation o2)
   {
      if (o1 == o2 || (o1.getId() != null && o1.getId().equals(o2.getId()))) return 0;
      // anchors/layers the same, compare ordinal
      if (o1.getParentId() != null && o1.getParentId().equals(o2.getParentId())
	  && o1.getAssignedOrdinal() != 0 && o2.getAssignedOrdinal() != 0)
      {
	 int o1Ordinal = o1.getAssignedOrdinal();
	 int o2Ordinal = o2.getAssignedOrdinal();
	 if (o1Ordinal < o2Ordinal)
	 {
	    return -3;
	 }
	 if (o1Ordinal > o2Ordinal)
	 {
	    return 3;
	 }
      }
      // if the parents are set, compare their start times
      Annotation p1 = o1.getParent();
      Annotation p2 = o2.getParent();
      if (p1 != null && p2 != null)
      {
	 try
	 {
	    Anchor p1Start = p1.getStart();
	    Anchor p2Start = p2.getStart();
	    double p1StartOffset = p1Start.getOffset().doubleValue();
	    double p2StartOffset = p2Start.getOffset().doubleValue();
	    
	    // starts earlier, is lower
	    if (p1StartOffset < p2StartOffset)
	    {
	       return -6;
	    }
	    // starts later, is higher
	    if (p1StartOffset > p2StartOffset)
	    {
	       return 6;
	    }
	 }
	 catch(Throwable t) {}
      }
      return super.compare(o1, o2);
   }
   
} // end of class AnnotationAnnotationComparatorByOrdinal
