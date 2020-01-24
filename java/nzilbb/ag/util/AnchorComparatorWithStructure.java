//
// Copyright 2015-2020 New Zealand Institute of Language, Brain and Behaviour, 
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
import java.util.LinkedHashSet;
import java.util.OptionalDouble;
import java.util.SortedSet;
import java.util.Vector;
import nzilbb.ag.Anchor;
import nzilbb.ag.Annotation;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.Schema;

/**
 * Anchor comparator that uses primarily offset, but when offset is null, uses the structure of the graph to compare anchors.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class AnchorComparatorWithStructure
  implements Comparator<Anchor>
{
   // Attributes:
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public AnchorComparatorWithStructure()
   {
   } // end of constructor

   public int compare(Anchor a1, Anchor a2)
   {
      // if they're the same anchor, return 0
      if (a1.equals(a2)) return 0;

      // does one follow the other in the graph?
      if (a1.follows(a2))
      { // there are directed links from a2 to a1
	 return 2;
      }
      if (a1.precedes(a2))
      { // there are directed links from a1 to a2
	 return -2;
      }

      // if offsets are non-null and different
      if (a1.getOffset() != null && a2.getOffset() != null 
	  && !a1.getOffset().equals(a2.getOffset()))
      { // compare by offset
	 return a1.getOffset().compareTo(a2.getOffset());
      }

      // if either offset is null
      // try to use graph connections 
      if (a1.getOffset() == null || a2.getOffset() == null)
      {
	 Double a1Min = a1.getOffsetMin();
	 if (a1Min != null)
	 {
	    Double a2Max = a2.getOffsetMax();
	    if (a2Max != null)
	    {
	       if (a1Min > a2Max)
	       { // a1 must be after a2
		  return a1Min.compareTo(a2Max);
	       }
	       else if (a1Min.equals(a2Max))
	       { // a1 must be after a2 (but compareTo will return 0 so...)
		  return 1;
	       }
	    } // a2Max not null
	 } // a1Min not null
	 Double a1Max = a1.getOffsetMax();
	 if (a1Max != null)
	 {
	    Double a2Min = a2.getOffsetMin();
	    if (a2Min != null)
	    {
	       if (a1Max < a2Min)
	       { // a1 must be before a2
		  return a1Max.compareTo(a2Min);
	       }
	       else if (a1Max.equals(a2Min))
	       { // a1 must be before a2 (but compareTo will return 0 so...)
		  return -1;
	       }
	    } // a2Min not null
	 } // a1Max not null
      } // one of the offsets is null

      // these are going to be useful:
      LinkedHashSet<Annotation> endingAtA1 = a1.getEndingAnnotations();
      LinkedHashSet<Annotation> endingAtA2 = a2.getEndingAnnotations();
      LinkedHashSet<Annotation> startingAtA1 = a1.getStartingAnnotations();
      LinkedHashSet<Annotation> startingAtA2 = a2.getStartingAnnotations();

      // if the offsets are equal
      if (a1.getOffset() != null && a1.getOffset().equals(a2.getOffset()))
      {         
         // compare anchors at the other end of annotations
         // i.e. if one they're both end anchors, and one has a start that's earlier than
         // the other, then it's after the other.
         // (utterance start will be ealier than word start)

         // find nearest start anchors
         OptionalDouble nearestStartOffset1 = endingAtA1.stream()
            .filter(a->a.getStart() != null)
            .filter(a->a.getStart().getOffset() != null)
            .mapToDouble(a->a.getStart().getOffset())
            .max();
         OptionalDouble nearestStartOffset2 = endingAtA2.stream()
            .filter(a->a.getStart() != null)
            .filter(a->a.getStart().getOffset() != null)
            .mapToDouble(a->a.getStart().getOffset())
            .max();
         if (nearestStartOffset1.isPresent() && nearestStartOffset2.isPresent())
         { // there are start offsets to compare
            if (nearestStartOffset1.getAsDouble() < nearestStartOffset2.getAsDouble()) return 9;
            if (nearestStartOffset1.getAsDouble() > nearestStartOffset2.getAsDouble()) return -9;
         } // there are start offsets to compare

         // find nearest end anchors
         OptionalDouble nearestEndOffset1 = startingAtA1.stream()
            .filter(a->a.getEnd() != null)
            .filter(a->a.getEnd().getOffset() != null)
            .mapToDouble(a->a.getEnd().getOffset())
            .min();
         OptionalDouble nearestEndOffset2 = startingAtA2.stream()
            .filter(a->a.getEnd() != null)
            .filter(a->a.getEnd().getOffset() != null)
            .mapToDouble(a->a.getEnd().getOffset())
            .min();
         if (nearestEndOffset1.isPresent() && nearestEndOffset2.isPresent())
         { // there are end offsets to compare
            if (nearestEndOffset1.getAsDouble() < nearestEndOffset2.getAsDouble()) return 10;
            if (nearestEndOffset1.getAsDouble() > nearestEndOffset2.getAsDouble()) return -10;
         } // there are end offsets to compare

      } // offsets are equal
      
      // if there is a common parent annotation, we can compare by ordinal
      // or if one is an ancestor of the other, we can compare by start/end
      LinkedHashSet<Annotation> a1Annotations = new LinkedHashSet<Annotation>(startingAtA1);
      a1Annotations.addAll(endingAtA1);
      LinkedHashSet<Annotation> a2Annotations = new LinkedHashSet<Annotation>(startingAtA2);
      a2Annotations.addAll(endingAtA2);
      for (Annotation a1Annotation : a1Annotations)
      {
	 Annotation a1Parent = a1Annotation.getParent();
	 if (a1Parent != null)
	 {
	    for (Annotation a2Annotation : a2Annotations)
	    {
	       if (a1Annotation.equals(a2Annotation)) continue;

	       Annotation a2Parent = a2Annotation.getParent();
	       if (a2Parent != null)
	       {
		  if (a1Parent.equals(a2Parent) // same parent on same layer
		      && a1Annotation.getLayerId().equals(a2Annotation.getLayerId()))
		  {
		     if (a1Annotation.getOrdinal() < a2Annotation.getOrdinal()) 
		     { // a1Annotation is prior to a2Annotation, so a1 < a2
			return -5;
		     }
		     if (a1Annotation.getOrdinal() > a2Annotation.getOrdinal()) 
		     { // a1Annotation is subsequent to a2Annotation, so a1 > a2
			return 5;
		     }
		  }
		  
		  if (a1Annotation.getAncestors().contains(a2Annotation))
		  { // a2 is an ancestor
		     if (a2Annotation.getStart().equals(a2))
		     { // a2 is ancestor's start anchor, so a1 is after a2
			return 6;
		     }
		     else // a2 is ancestor's end anchor, so a1 is before a2
		     {
			return -6;
		     }
		  }

		  if (a2Annotation.getAncestors().contains(a1Annotation))
		  { // a1 is an ancestor
		     if (a1Annotation.getStart().equals(a1))
		     { // a1 is ancestor's start anchor, so a1 is before a2
			return -7;
		     }
		     else // a1 is ancestor's end anchor, so a1 is after a2
		     {
			return 7;
		     }
		  }
	       } // a2 parent set
	    } // next a2 annotation
	 } // a1 parent set
      } // next a1 annotation

      // use first common ancestor to find ancestors comparable by ordinal
      Annotation deepestCommonAncestor = null;
      Annotation a1AncestorChild = null;
      Annotation a2AncestorChild = null;
      for (Annotation a1Annotation : a1Annotations)
      {
	 LinkedHashSet<Annotation> a1AnnotationAncestors = a1Annotation.getAncestors();
	 for (Annotation a2Annotation : a2Annotations)
	 {
	    Annotation thisCommonAncestor = a1Annotation.getFirstCommonAncestor(a2Annotation);
	    // if the graph is complete, thisCommonAncestor cannot be null
	    // but if it's a partial graph, with layers missing, it may be
	    if (thisCommonAncestor == null) continue;

	    // we potentially compare lots of annotations on different layers
	    // we want the deepest common ancestor amongst all of them 
	    // i.e. the nearest two annotations
	    if (deepestCommonAncestor == null
		|| thisCommonAncestor.getAncestors().size() > deepestCommonAncestor.getAncestors().size())
	    {
	       LinkedHashSet<Annotation> a2AnnotationAncestors = a2Annotation.getAncestors();

	       // there's a child that leads to a1 and a child that leads to a2
	       // but each child has to be on the same layer, or the ordinals aren't comparable
	       for (String layerId : thisCommonAncestor.getAnnotations().keySet())
	       {
		  if (a1Annotation.getGraph() == null
		      || a1Annotation.getGraph().getLayer(layerId) == null
		      || a1Annotation.getGraph().getLayer(layerId).getAlignment() == Constants.ALIGNMENT_NONE)
		  { // ignore unaligned child layers - e.g. the participant layer
		     continue;
		  }
		  SortedSet<Annotation> children = thisCommonAncestor.getAnnotations(layerId);
		  Annotation a1LayerChild = null;
		  Annotation a2LayerChild = null;
		  for (Annotation child : children)
		  {
		     if (a1Annotation == child || a1AnnotationAncestors.contains(child))
		     {
			a1LayerChild = child;
		     }
		     else if (a2Annotation == child || a2AnnotationAncestors.contains(child))
		     {
			a2LayerChild = child;
		     }
		     if (a1LayerChild != null && a2LayerChild != null
			 // double check they have a common parent
			 && a1LayerChild.getParentId().equals(a2LayerChild.getParentId()))
		     { // stop searching, we have what we're looking for
			break;
		     }
		  } // next child
		  if (a1LayerChild != null && a2LayerChild != null
		      // double check they have a common parent
		      && a1LayerChild.getParentId().equals(a2LayerChild.getParentId()))
		  { // stop searching, we have what we're looking for
		     deepestCommonAncestor = thisCommonAncestor;
		     a1AncestorChild = a1LayerChild;
		     a2AncestorChild = a2LayerChild;
		     break;
		  }
	       } // next layer
	    } // found a new deepest common ancestor
	 } // next a2 annotation
      } // next a1 annotation
      if (a1AncestorChild != null && a2AncestorChild != null)
      { // we have a common ancestor and the two children that lead to a1 and a2
	 if (a1AncestorChild.getOrdinal() > a2AncestorChild.getOrdinal())
	 {
	    return 8;
	 }
	 else if (a1AncestorChild.getOrdinal() < a2AncestorChild.getOrdinal())
	 {
	    return -8;
	 }	 
      }

      // if the offsets are equal
      if (a1.getOffset() != null && a1.getOffset().equals(a2.getOffset()))
      {         
	 if (endingAtA1.size() == 0 && endingAtA2.size() > 0)
	 { // a1 is not an end anchor and a2 is, so a1 is structurally after a2
	    return 3;
	 }
	 if (endingAtA2.size() == 0 && endingAtA1.size() > 0)
	 { // a2 is not an end anchor and a1 is, so a1 is structurally before a2
	    return -3;
	 }
	 if (startingAtA1.size() == 0 && startingAtA2.size() > 0)
	 { // a1 is not a start anchor and a2 is, so a1 is structurally before a2
	    return -4;
	 }
	 if (startingAtA2.size() == 0 && startingAtA1.size() > 0)
	 { // a2 is not a start anchor and a1 is, so a1 is structurally after a2
	    return 4;
	 }
	 // if both are ends only, use inclusion to decide which is earlier TODO these actually necessary?
	 // if both are starts only, use inclusion to decide which is earlier TODO

      } // offsets are equal

      return a1.getId().compareTo(a2.getId()); //TODO
   }

} // end of class AnchorComparatorWithStructure
