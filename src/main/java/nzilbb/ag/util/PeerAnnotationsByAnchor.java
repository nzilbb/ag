//
// Copyright 2016 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.util.Vector;
import nzilbb.ag.Annotation;
import nzilbb.ag.Change;
import nzilbb.ag.Anchor;

/**
 * Convenience class implementing a list of peer annotations (i.e. with the same parent), 
 * ordered by offset then ordinal. The constructors from other Annotation collections also
 * filter out Annotations marked for deletion.
 * <p>This collection requires that all annotations are on the same layer and have the same parent,
 * but has much better performance an {@link AnnotationsByAnchor}.
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class PeerAnnotationsByAnchor
  extends Vector<Annotation>
{
   
   // Methods:
   
   /**
    * Constructor from parent and layer.
    * @param parent The parent annotaiton of the peers.
    * @param layerId The ID of the layer of the peers.
    */
   public PeerAnnotationsByAnchor(Annotation parent, String layerId)
   {
      // first add all in peer (ordinal) order
      for (Annotation annotation : parent.getAnnotations(layerId))
      { 
	 if (annotation.getChange() == Change.Operation.Destroy) continue; // ignore deleted annotations
	 
	 add(annotation);
      } // next child

      // now move children with anchors that are out of order by start or end anchor
      int lastSetIndex = 0;
      for (int c = 0; c < size(); c++)
      {
	 Annotation lastAnchoredAnnotation = elementAt(lastSetIndex);	 
	 Annotation annotation = elementAt(c);

	 if (before(annotation, lastAnchoredAnnotation))
	 { // out of order, move it back
	    boolean moved = false;
	    // for each preceding annotation
	    for (int p = lastSetIndex; p >= 0; p--)
	    {
	       Annotation preceding = elementAt(p);
	       if (preceding == annotation) continue;
	       // is the offset less than this one?
	       if (before(preceding, annotation))
	       {
		  // insert the annotation after the preceding one
		  remove(annotation);
		  insertElementAt(annotation, p+1);
		  moved = true;
		  break;
	       }
	    } // previous annotation
	    if (!moved)
	    { // we didn't find a preceding annotation
	       // so move it to the start
	       remove(annotation);
	       insertElementAt(annotation, 0);
	    }
	    lastSetIndex++;
	 } // out of order
	 else if ((annotation.getStart() != null && annotation.getStart().getOffset() != null)
		  || (annotation.getEnd() != null && annotation.getEnd().getOffset() != null))
	 {
	    lastSetIndex = c;
	 } // in order
      } // next child

      // now check chains of children with no offsets
      for (int c = 0; c < size(); c++)
      {
	 Annotation annotation = elementAt(c);
	 Anchor end = annotation.getEnd();
	 if (end != null)
	 { // no offset
	    // is this end the start of a peer?
	    int i = c;
	    for (Annotation startsHere : end.startOf(layerId))
	    {
	       if (startsHere == annotation) continue;
	       int f = indexOf(startsHere);
	       if (f < 0) continue; // not a child of this parent
	       if (f < i)
	       { // out of order, move it back
		  remove(annotation);
		  insertElementAt(annotation, f);
		  i = f;
	       }
	    } // next peer that starts here
	 } // offset null
      } // next child
      
   } // end of constructor

   
   /**
    * Determines whether the first annotation is before the second or not
    * @param a1 First annotation
    * @param a2 Second annotation
    * @return true if a1 is clearly before a2, false otherwise.
    */
   private boolean before(Annotation a1, Annotation a2)
   {
      Anchor a1Start = a1.getStart();
      Anchor a1End = a1.getEnd();
      Anchor a2Start = a2.getStart();
      Anchor a2End = a2.getEnd();
      if (a1End != null
	  && a1End.getOffset() != null)
      {	 
	 if (a2Start != null
	     && a2Start.getOffset() != null && a1End.getOffset() <= a2Start.getOffset())
	 {
	    return true;
	 }
      }
      if (a1Start != null
	  && a1Start.getOffset() != null)
      {
	 if (a2Start != null 
	     && a2Start.getOffset() != null && a1Start.getOffset() < a2Start.getOffset())
	 {
	    return true;
	 }
	 if (a2End != null
	     && a2End.getOffset() != null && a1Start.getOffset() < a2End.getOffset())
	 {
	    return true;
	 }
      }
      return false;
   } // end of bestOffset()


} // end of class PeerAnnotationsByAnchor
