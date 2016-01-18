//
// Copyright 2015 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of LaBB-CAT.
//
//    LaBB-CAT is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 2 of the License, or
//    (at your option) any later version.
//
//    LaBB-CAT is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with LaBB-CAT; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package nzilbb.ag.util;

import java.util.Comparator;
import nzilbb.ag.Annotation;

/**
 * Comparator that compares annotations by anchor offset, ordering start earliest, then end latest, then highest ordinal first, then highest ID first.
 * <p>Ordering is by start ascending then end descending so that tree structures come out with a top-down traversal - i.e. earlier ancestors are first, and where ancestors start at the same time, widest ancestors are first.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class AnnotationComparatorByAnchor
   implements Comparator<Annotation>
{
   // Attributes:
   
   // Methods:
   
   /**
    * Default constructor
    */
   public AnnotationComparatorByAnchor()
   {
   } // end of constructor
   
   public int compare(Annotation o1, Annotation o2)
   {
      if (o1.equals(o2))
      {
	 // System.out.println("" + o1 + " === " + o2);
	 return 0;
      }
      try
      {
	 // starts earlier, is lower
	 if (o1.getStart().getOffset() < o2.getStart().getOffset())
	 {
	    // System.out.println("start: " + o1 + " < " + o2 + " (" + o1.getStart() + " < " + o2.getStart() + ")");
	    return -6;
	 }
	 // starts later, is higher
	 if (o1.getStart().getOffset() > o2.getStart().getOffset())
	 {
	    // System.out.println("start: " + o1 + " > " + o2 + " (" + o1.getStart() + " > " + o2.getStart() + ")");
	    return 6;
	 }

	 // same start anchors...
	 
	 // special case - end anchors same as start
	 if (o1.getStart().getOffset().doubleValue() == o1.getEnd().getOffset().doubleValue())
	 {
	    if (o2.getStart().getOffset().doubleValue() != o2.getEnd().getOffset().doubleValue())
	    {
	       return -9;
	    }
	 }
	 else if (o2.getStart().getOffset().doubleValue() == o2.getEnd().getOffset().doubleValue())
	 {
	    return 9;
	 }

	 // ends earlier, is higher
	 if (o1.getEnd().getOffset() < o2.getEnd().getOffset())
	 {
	    // System.out.println("end: " + o1 + " > " + o2 + " (" + o1.getEnd() + " < " + o2.getEnd() + ")");
	    return 8;
	 }
	 // ends later, is lower
	 if (o1.getEnd().getOffset() > o2.getEnd().getOffset())
	 {
	    // System.out.println("end: " + o1 + " < " + o2 + " (" + o1.getEnd() + " > " + o2.getEnd() + ")");
	    return -8;
	 }
      }
      catch(Throwable t) {}


      // special case: if the start anchor of one is the end anchor of another, it's earlier
      if (o1.getEndId().equals(o2.getStartId()))
      {
	 // System.out.println("linked: " + o1 + " < " + o2 + " (" + o1.getEndId() + ")");
	 return -7;
      }
      if (o1.getStartId().equals(o2.getEndId())) 
      {
	 // System.out.println("linked: " + o1 + " > " + o2 + " (" + o1.getStartId() + ")");
	 return 7;
      }

      // start anchors the same, compare layer id
      if (o1.getLayerId().compareTo(o2.getLayerId()) > 0)
      {
	 // System.out.println("layer: " + o1 + " > " + o2 + " (" + o1.getLayerId() + " > " + o2.getLayerId() + ")");
	 return 4;
      }
      if (o1.getLayerId().compareTo(o2.getLayerId()) < 0)
      {
	 // System.out.println("layer: " + o1 + " < " + o2 + " (" + o1.getLayerId() + " < " + o2.getLayerId() + ")");
	 return -4;
      }

      // anchors/layers the same, compare ordinal
      if (o1.getOrdinal() < o2.getOrdinal())
      {
	 // System.out.println("ordinal: " + o1 + " < " + o2 + " (" + o1.getOrdinal() + " < " + o2.getOrdinal() + ")");
	 return -3;
      }
      if (o1.getOrdinal() > o2.getOrdinal())
      {
	 // System.out.println("ordinal: " + o1 + " > " + o2 + " (" + o1.getOrdinal() + " > " + o2.getOrdinal() + ")");
	 return 3;
      }

      // anchors/layers/ordinals the same or null, compare id
      if (o1.getId() != null && o2.getId() != null)
      {
	 if (o1.getId().compareTo(o2.getId()) < 0)
	 {
	    // System.out.println("id: " + o1 + " < " + o2 + " (" + o1.getId() + " < " + o2.getId() + ")");
	    return -1;
	 }
	 if (o1.getId().compareTo(o2.getId()) > 0)
	 {
	    // System.out.println("id: " + o1 + " > " + o2 + " (" + o1.getId() + " > " + o2.getId() + ")");
	    return 1;
	 }
	 // System.out.println("id: " + o1 + " == " + o2 + " (" + o1.getId() + " == " + o2.getId() + ")");
	 return 0; // same offsets, label, and ID, same annotation
      }
      // anchors/layers/ordinals/labels/ID null, use hashcode
      // System.out.println("hashcode: " + o1 + " - " + o2 + " (" + o1.hashCode() + " - " + o2.hashCode() + ")");
      if (o1.hashCode() < o2.hashCode()) return -99;
      return 99;
   }
   
} // end of class AnnotationAnnotationComparatorByAnchor
