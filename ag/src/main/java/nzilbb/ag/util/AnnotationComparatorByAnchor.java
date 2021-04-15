//
// Copyright 2015-2021 New Zealand Institute of Language, Brain and Behaviour, 
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
 * Comparator that compares annotations by anchor offset, ordering start earliest, then
 * end latest, then lowest ordinal first, then highest ID first. 
 * <p>Ordering is by start ascending then end descending so that tree structures come out
 * with a top-down traversal - i.e. earlier ancestors are first, and where ancestors start
 * at the same time, widest ancestors are first. 
 * @author Robert Fromont robert@fromont.net.nz
 */
public class AnnotationComparatorByAnchor implements Comparator<Annotation> {
  
  /**
   * Default constructor
   */
  public AnnotationComparatorByAnchor() {
  } // end of constructor
   
  public int compare(Annotation o1, Annotation o2) {
    try {
      Anchor o1Start = o1.getStart();
      Anchor o1End = o1.getEnd();
      Anchor o2Start = o2.getStart();
      Anchor o2End = o2.getEnd();
      double o1StartOffset = o1Start.getOffset().doubleValue();
      double o2StartOffset = o2Start.getOffset().doubleValue();
	 
      // starts earlier, is lower
      if (o1StartOffset < o2StartOffset) {
        // System.out.println("start: " + o1 + " < " + o2 + " (" + o1Start + " < " + o2Start + ")");
        return -6;
      }
      // starts later, is higher
      if (o1StartOffset > o2StartOffset) {
        // System.out.println("start: " + o1 + " > " + o2 + " (" + o1Start + " > " + o2Start + ")");
        return 6;
      }

      // same start anchors...
	 
      // special case - end anchors same as start
      double o1EndOffset = o1End.getOffset().doubleValue();
      double o2EndOffset = o2End.getOffset().doubleValue();
      if (o1StartOffset == o1EndOffset) {
        if (o2StartOffset != o2EndOffset) {
          // System.out.println("end same as start: " + o1 + " < " + o2 + " (" + o1Start + " < " + o2Start + ")");
          return -9;
        }
      } else if (o2StartOffset == o2EndOffset) {
        // System.out.println("start same as end: " + o1 + " > " + o2 + " (" + o1Start + " < " + o2Start + ")");
        return 9;
      }

      // ends earlier, is higher
      if (o1EndOffset < o2EndOffset) {
        // System.out.println("end: " + o1 + " > " + o2 + " (" + o1End + " < " + o2End + ")");
        return 8;
      }
      // ends later, is lower
      if (o1EndOffset > o2EndOffset) {
        // System.out.println("end: " + o1 + " < " + o2 + " (" + o1End + " > " + o2End + ")");
        return -8;
      }
    } catch(Throwable t) {
    }

    // special case: if the start anchor of one is the end anchor of another, it's earlier
    if (o1.getEndId() != null 
        && o1.getEndId().equals(o2.getStartId())) {
      // System.out.println("linked: " + o1 + " < " + o2 + " (" + o1.getEndId() + ")");
      return -7;
    }
    if (o1.getStartId() != null
        && o1.getStartId().equals(o2.getEndId())) {
      // System.out.println("linked: " + o1 + " > " + o2 + " (" + o1.getStartId() + ")");
      return 7;
    }

    // start anchors the same, compare layer id
    int o1LayerComparedToo2Layer = o1.getLayerId().compareTo(o2.getLayerId());
    if (o1LayerComparedToo2Layer > 0) {
      // System.out.println("layer: " + o1 + " > " + o2 + " (" + o1.getLayerId() + " > " + o2.getLayerId() + ")");
      return 4;
    }
    if (o1LayerComparedToo2Layer < 0) {
      // System.out.println("layer: " + o1 + " < " + o2 + " (" + o1.getLayerId() + " < " + o2.getLayerId() + ")");
      return -4;
    }

    // anchors/layers the same, compare ordinal
    if (o1.getParentId() != null && o1.getParentId().equals(o2.getParentId())
        && o1.getAssignedOrdinal() != 0 && o2.getAssignedOrdinal() != 0) {
      int o1Ordinal = o1.getAssignedOrdinal();
      int o2Ordinal = o2.getAssignedOrdinal();
      if (o1Ordinal < o2Ordinal) {
        // System.out.println("ordinal: " + o1 + " < " + o2 + " (" + o1.getOrdinal() + " < " + o2.getOrdinal() + ")");
        return -3;
      }
      if (o1Ordinal > o2Ordinal) {
        // System.out.println("ordinal: " + o1 + " > " + o2 + " (" + o1.getOrdinal() + " > " + o2.getOrdinal() + ")");
        return 3;
      }
    }

    // anchors/layers/ordinals the same or null, compare id
    if (o1.getId() != null && o2.getId() != null) {
      int o1IdComparedToo2Id = o1.getId().compareTo(o2.getId());
      if (o1IdComparedToo2Id < 0) {
        // System.out.println("id: " + o1 + " < " + o2 + " (" + o1.getId() + " < " + o2.getId() + ")");
        return -1;
      }
      if (o1IdComparedToo2Id > 0) {
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
