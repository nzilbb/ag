//
// Copyright 2019 New Zealand Institute of Language, Brain and Behaviour, 
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
 * Comparator that compares annotations by distance from a given annotation.  Otherwise ordering
 * matches {@link AnnotationComparatorByAnchor}. 
 * @author Robert Fromont robert@fromont.net.nz
 */
public class AnnotationComparatorByDistance
   extends AnnotationComparatorByAnchor
{  
  /**
   * The annotation to compute the distance from.
   * @see #getReference()
   * @see #setReference(Annotation)
   */
  protected Annotation reference;
  /**
   * Getter for {@link #reference}.
   * @return The annotation to compute the distance from.
   */
  public Annotation getReference() { return reference; }
  /**
   * Setter for {@link #reference}.
   * @param reference The annotation to compute the distance from.
   * @return <var>this</var>.
   */
  public AnnotationComparatorByDistance setReference(Annotation reference) { this.reference = reference; return this; }

  // Methods:
  
  /**
   * Constructor
   */
  public AnnotationComparatorByDistance(Annotation reference)
  {
    setReference(reference);
  } // end of constructor
  
  public int compare(Annotation o1, Annotation o2)
  {
    if (!o1.getAnchored()) return -99;
    if (!o2.getAnchored()) return 99;
    int comparison = o1.distance(reference).compareTo(o2.distance(reference));
    if (comparison != 0) return comparison;
    return super.compare(o1, o2);
  }
   
} // end of class AnnotationComparatorByDistance
