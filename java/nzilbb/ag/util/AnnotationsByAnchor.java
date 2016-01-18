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

import java.util.TreeSet;
import java.util.Collection;
import nzilbb.ag.Annotation;
import nzilbb.ag.Change;

/**
 * Convenience class implemeting a set of annotations ordered by anchor (using {@link AnnotationComparatorByAnchor}). The constructors from other Annotation collections also filter out Annotations marked for deletion.
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("serial")
public class AnnotationsByAnchor
  extends TreeSet<Annotation>
{
   /**
    * Default constructor.
    */
   public AnnotationsByAnchor()
   {
      super(new AnnotationComparatorByAnchor());
   } // end of constructor

   /**
    * Constructor from a collection of Annotations.
    * <p>Annotations for which {@link Annotation#getChange()} is {@link nzilbb.ag.Change.Operation}.Delete are excluded from the set. If you want to include them, use addAll(Collection) instead.
    * @param annotations A collection of Annotations.
    */
   public AnnotationsByAnchor(Collection<Annotation> annotations)
   {
      super(new AnnotationComparatorByAnchor());
      for (Annotation annotation : annotations)
      { 
	 if (annotation.getChange() == Change.Operation.Destroy) continue; // ignore deleted annotations
	 
	 add(annotation);
      } // next child
   } // end of constructor

   /**
    * Constructor from an array of Annotations.
    * <p>Annotations for which {@link Annotation#getChange()} is {@link nzilbb.ag.Change.Operation}.Delete are excluded from the set. If you want to include them, use addAll(Collection) instead.
    * @param annotations An array of Annotations.
    */
   public AnnotationsByAnchor(Annotation[] annotations)
   {
      super(new AnnotationComparatorByAnchor());
      for (Annotation annotation : annotations)
      { 
	 if (annotation.getChange() == Change.Operation.Destroy) continue; // ignore deleted annotations
	 
	 add(annotation);
      } // next child
   } // end of constructor
} // end of class AnnotationsByAnchor
