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
package nzilbb.ag;

import java.util.Set;
import java.util.SortedSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Comparator;
import java.util.NoSuchElementException;

/**
 * A set of annotations chained together by sharing end/start annotations.
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class AnnotationChain
   extends LinkedHashSet<Annotation>
   implements SortedSet<Annotation>
{
   // Methods:
   
   /**
    * Default constructor.
    */
   public AnnotationChain()
   {
   } // end of constructor

   /**
    * Constructor.
    * @param other Another set of annotations
    */
   public AnnotationChain(Set<Annotation> other)
   {
      if (other.size() == 0) return; // empty set
      // find the start...

      // copy the set
      LinkedHashSet<Annotation> startCandidates = new LinkedHashSet<Annotation>(other);

      // keep a list of instants
      LinkedHashSet<Annotation> instants = new LinkedHashSet<Annotation>();

      // eliminate from the copy all the annotations whose start is the end of another one in the list
      for (Annotation a : other)
      {
	 // instant?
	 if (a.getInstantaneous())
	 {
	    startCandidates.remove(a);
	    instants.add(a);
	    break; // ignore instants when looking for the start
	 }
	 for (Annotation next : a.getEnd().getStartingAnnotations())
	 {
	    startCandidates.remove(next);
	 } // next following annotation
      } // next annotation
      if (startCandidates.size() == 0 && other.size() == 1)
      {
	 // this can only happen if other contains a single instantaneous annotation
	 add(other.iterator().next());
	 return;
      }

      // the remaining annotation(s) is the start of the chain
      assert startCandidates.size() > 0 : "startCandidates.size() > 0 - " + other;

      // get all possible chains, and pick the longest
      LinkedHashSet<Annotation> longest = new LinkedHashSet<Annotation>();
      Iterator<Annotation> itStartCandidates = startCandidates.iterator();
      while (itStartCandidates.hasNext())
      {
	 LinkedHashSet<Annotation> chain = new LinkedHashSet<Annotation>();

	 Annotation start = itStartCandidates.next();

	 // start from the beginning of the chain, and traverse the graph through nodes that are in other
	 Annotation last = start;
	 
	 boolean bFound = true;
	 while(bFound)
	 {	
	    // prefix any instants that start here
	    for (Annotation instant : instants) if (instant.getEnd().equals(last.getStart())) chain.add(instant);
	    
	    chain.add(last);
	    
	    bFound = false;
	    for (Annotation maybeNext : last.getEnd().getStartingAnnotations())
	    {
	       if (maybeNext.getInstantaneous()) continue; // ignore instants for now
	       if (other.contains(maybeNext))
	       {
		  last = maybeNext;
		  bFound = true;
		  break;
	       }
	    } // next possible next annotation
	 } // bFound
	 
	 // suffix any instants that end here
	 for (Annotation instant : instants)
	 {
	    if (instant.getStart().equals(last.getEnd()))
	    {
	       chain.add(instant);
	    }
	 } // next instant
	 
	 // is it the longest so far?
	 if (chain.size() > longest.size()) longest = chain;
      } // next candidate chain
      // now fill ourselves with the contents of the longest chain found
      addAll(longest);
   } // end of constructor

   /**
    * Constructor.
    * @param start Start anchor
    * @param end End anchor, different from aStart
    */
   public AnnotationChain(Anchor start, Anchor end)
   {
      findChain(start,  end, new HashSet<String>(), new HashSet<Anchor>());
   }

   /**
    * Constructor.
    * @param start Start anchor
    * @param end End anchor, different from aStart
    * @param excludeLayers Layers to exclude
    */
   public AnnotationChain(Anchor start, Anchor end, Set<String> excludeLayers)
   {
      findChain(start,  end, excludeLayers, new HashSet<Anchor>());
   }

   /**
    * Constructor.
    * @param start Start anchor
    * @param end End anchor, different from aStart
    * @param excludeLayers Layers to exclude
    * @param excludeAnchors Anchor to not follow
    */
   protected AnnotationChain(Anchor start, Anchor end, Set<String> excludeLayers, Set<Anchor> excludeAnchors)
   {
      findChain(start,  end, excludeLayers, excludeAnchors);
   }

   /**
    * Finds a chain between two anchors.
    * @param start Start anchor
    * @param end End anchor, different from aStart
    * @param excludeLayers Layers to exclude
    * @param excludeAnchors Anchor to not follow
    */
   protected void findChain(Anchor start, Anchor end, Set<String> excludeLayers, Set<Anchor> excludeAnchors)
   {
      if (start.getId().equals(end.getId())) return;
      for (Annotation startsHere : start.getStartingAnnotations())
      {
	 if (excludeLayers.contains(startsHere.getLayerId())) continue;
	 if (startsHere.getInstantaneous()) continue;
	 if (startsHere.getEnd().equals(end))
	 {
	    // prefix any instants that are at the start
	    for (Annotation a : start.getStartingAnnotations()) 
	    {
	       if (a.getInstantaneous())
	       {
		  add(a);
	       }
	    } // next starting annotation
	    add(startsHere);
	    // suffix any instants that are at the end
	    for (Annotation a : end.getStartingAnnotations()) 
	    {
	       if (a.getInstantaneous())
	       {
		  add(a);
	       }
	    } // next starting annotation
	 
	    return; // done!
	 }
      } // next annotation that starts here
      
      // there's no single-link chain, so recursively look for a multi-link chain
      for (Annotation startsHere : start.getStartingAnnotations())
      {
	 if (excludeLayers.contains(startsHere.getLayerId())) continue;
	 if (startsHere.getInstantaneous()) continue;
	 // don't follow tags (we'll already be following their parents)
	 if (startsHere.getLayer() == null) continue;
	 if (startsHere.getLayer().getAlignment() == Constants.ALIGNMENT_NONE) continue;
	 // guard against cycles, not all graphs are valid
	 if (excludeAnchors.contains(startsHere.getEnd())) continue; 
	 excludeAnchors.add(startsHere.getEnd());
	 AnnotationChain nextPart = new AnnotationChain(startsHere.getEnd(), end, excludeLayers, excludeAnchors);
	 if (nextPart.size() > 0)
	 {
	    // prefix any instants that are at the start
	    for (Annotation a : start.getStartingAnnotations()) 
	    {
	       if (a.getInstantaneous())
	       {
		  add(a);
	       }
	    } // next starting annotation
	    add(startsHere);
	    addAll(nextPart);
	    return; // done!
	 }
      } // next annotation that starts here
   }

   /**
    * Constructor.
    * @param start Start anchor
    * @param endOffset End offset
    * @param excludeLayers Layers to exclude
    */
   public AnnotationChain(Anchor start, double endOffset, Set<String> excludeLayers)
   {
      findChain(start, endOffset, excludeLayers, new HashSet<Anchor>());
   }
   /**
    * Constructor.
    * @param start Start anchor
    * @param endOffset End offset
    * @param excludeLayers Layers to exclude
    * @param excludeAnchors Anchor to not follow
    */
   protected AnnotationChain(Anchor start, double endOffset, Set<String> excludeLayers, Set<Anchor> excludeAnchors)
   {
      findChain(start, endOffset, excludeLayers, excludeAnchors);
   }
   /**
    * Finds a chain between a start anchor up to an offset.
    * @param start Start anchor
    * @param endOffset End offset
    * @param excludeLayers Layers to exclude
    * @param excludeAnchors Anchor to not follow
    */
   protected void findChain(Anchor start, double endOffset, Set<String> excludeLayers, Set<Anchor> excludeAnchors)
   {
      // too far - empty if start is after end
      if (start.getOffset() != null && start.getOffset() >= endOffset) return;

      // recursively look for a multi-link chain until we hit the end offset
      for (Annotation startsHere : start.getStartingAnnotations())
      {
	 if (excludeLayers.contains(startsHere.getLayerId())) continue;
	 if (startsHere.getInstantaneous()) continue;
	 // don't follow tags (we'll already be following their parents)
	 if (startsHere.getLayer() == null) continue;
	 if (startsHere.getLayer().getAlignment() == Constants.ALIGNMENT_NONE) continue;
	 // guard against cycles, not all graphs are valid
	 if (excludeAnchors.contains(startsHere.getEnd())) continue; 
	 excludeAnchors.add(startsHere.getEnd());
	 AnnotationChain nextPart = new AnnotationChain(startsHere.getEnd(), endOffset, excludeLayers, excludeAnchors);
	 if (nextPart.size() > 0)
	 {
	    // prefix any instants that are at the start
	    for (Annotation a : start.getStartingAnnotations()) 
	    {
	       if (a.getInstantaneous()) 
	       {
		  add(a);
	       }
	    } // next starting annotation
	    add(startsHere);
	    addAll(nextPart);
	    return; // done!
	 }

	 // if we get to here, then there are no annotations after startsHere that
	 // finish before endOffset
	 if (startsHere.getEnd().getOffset() != null
	     && startsHere.getEnd().getOffset() <= endOffset)
	 { // this is the last in the chain
	    // prefix any instants that are at the start
	    for (Annotation a : start.getStartingAnnotations()) 
	    {
	       if (a.getInstantaneous())
	       {
		  add(a);
	       }
	    } // next annotation
	    add(startsHere);
	    // suffix any instants that are at the end
	    for (Annotation a : startsHere.getEnd().getStartingAnnotations()) 
	    {
	       if (a.getInstantaneous())
	       {
		  add(a);
	       }
	    } // next annotation
	 }
      } // next annotation that starts here      
   }

   /**
    * Constructor.
    * @param startOffset Start offset
    * @param end End anchor
    * @param excludeLayers Layers to exclude
    */
   public AnnotationChain(double startOffset, Anchor end, Set<String> excludeLayers)
   {
      findChain(startOffset, end, excludeLayers, new HashSet<Anchor>());
   }
   /**
    * Constructor.
    * @param startOffset Start offset
    * @param end End anchor
    * @param excludeLayers Layers to exclude
    * @param excludeAnchors Anchor to not follow
    */
   protected AnnotationChain(double startOffset, Anchor end, Set<String> excludeLayers, Set<Anchor> excludeAnchors)
   {
      findChain(startOffset, end, excludeLayers, excludeAnchors);
   }
   /**
    * Finds a chain between a start offset and an anchor
    * @param startOffset Start offset
    * @param end End anchor
    * @param excludeLayers Layers to exclude
    * @param excludeAnchors Anchor to not follow
    */
   protected void findChain(double startOffset, Anchor end, Set<String> excludeLayers, Set<Anchor> excludeAnchors)
   {
      // too far - empty if start is after end
      if (end.getOffset() != null && end.getOffset() <= startOffset) return;

      // recursively look for a multi-link chain until we hit the start offset
      for (Annotation endsHere : end.getEndingAnnotations())
      {
	 if (excludeLayers.contains(endsHere.getLayerId())) continue;
	 if (endsHere.getInstantaneous()) continue;
	 // guard against cycles, not all graphs are valid
	 if (excludeAnchors.contains(endsHere.getStart())) continue; 
	 excludeAnchors.add(endsHere.getStart());
	 AnnotationChain previousPart = new AnnotationChain(startOffset, endsHere.getStart(), excludeLayers, excludeAnchors);
	 if (previousPart.size() > 0)
	 {
	    addAll(previousPart);
	    add(endsHere);
	    // suffix any instants that are at the end
	    for (Annotation a : end.getEndingAnnotations()) 
	    {
	       if (a.getInstantaneous())
	       {
		  add(a);
	       }
	    } // next annotation
	    return; // done!
	 }
	 
	 // if we get to here, then there are no annotations before endsHere that
	 // start after dStartOffset
	 if (endsHere.getStart().getOffset() != null
	     && endsHere.getStart().getOffset() >= startOffset)
	 { // this is the first in the chain
	    // prefix any instants that are at the start
	    for (Annotation a : endsHere.getStart().getStartingAnnotations()) 
	    {
	       if (a.getInstantaneous())
	       {
		  add(a);
	       }
	    } // next snnotation
	    add(endsHere);
	    // suffix any instants that are at the end
	    for (Annotation a : end.getStartingAnnotations()) 
	    {
	       if (a.getInstantaneous())
	       {
		  add(a);
	       }
	    } // next annotation
	 }

      } // next annotation that starts here
      
   }

   public Comparator<Annotation> comparator()
   {
      return null;
   }

   public SortedSet<Annotation> subSet(Annotation fromElement, Annotation toElement)
      throws IllegalArgumentException
   {
      if (!contains(fromElement)) throw new IllegalArgumentException();
      if (!contains(toElement)) throw new IllegalArgumentException();
      AnnotationChain other = new AnnotationChain();
      Annotation last = fromElement;
      while(last != toElement)
      {
	 other.add(last);
	 boolean bFound = false;
	 for (Annotation maybeNext : last.getEnd().getStartingAnnotations())
	 {
	    if (contains(maybeNext))
	    {
	       last = maybeNext;
	       bFound = true;
	       break;
	    }
	 }
	 if (!bFound) throw new IllegalArgumentException();
      }
      return other;
   }

   public SortedSet<Annotation> headSet(Annotation toElement)
   {
      return subSet(first(), toElement);
   }

   public SortedSet<Annotation> tailSet(Annotation fromElement)
   {
      if (!contains(fromElement)) throw new IllegalArgumentException();
      AnnotationChain other = new AnnotationChain();
      Annotation last = fromElement;
      boolean bFound = true;
      while(bFound)
      {
	 other.add(last);
	 bFound = false;
	 for (Annotation maybeNext : last.getEnd().getStartingAnnotations())
	 {
	    if (contains(maybeNext))
	    {
	       last = maybeNext;
	       bFound = true;
	       break;
	    }
	 }
      }
      return other;
   }
   
   public Annotation first()
      throws NoSuchElementException
   {
      return iterator().next();
   }

   public Annotation last()
      throws NoSuchElementException
   {
      Iterator<Annotation> i = iterator();
      Annotation last = i.next();
      while (i.hasNext()) last = i.next();
      return last;
   }
} // end of class AnnotationChain
