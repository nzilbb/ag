//
// Copyright 2016-2021 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.editpath;

import java.util.Comparator;

/**
 * Implementation of IComparator that uses java.lang.Object.equals(Object) to determine
 * equality or not.  This is used as a default comparison method for 
 * {@link DefaultEditComparator}, but can also be used as an adapter for customising the
 * equals comparison, e.g.: 
 * <pre>
 * // case-insensitive comparison
 * new EqualsComparator&lt;String&gt;()
 * {
 *   public int compare(String o1, String o2)
 *   {
 *      return o1.toLowerCase().compareTo(o2.toLowerCase());
 *   }
 *  }
 * </pre>
 * <p><em>NB</em> This comparator cannot be used for ordering elements, as it only returns
 * 0 (for "equal") or 1 (for "not equal"). 
 * @author Robert Fromont robert@fromont.net.nz
 */
@SuppressWarnings("overrides")
public class EqualsComparator<T> implements Comparator<T> {
  // Methods:
   
  /**
   * Default constructor.
   */
  public EqualsComparator() {
  } // end of constructor

  /**
   * Compares two objects for equality only. 
   * <p><em>NB</em> This cannot be used for ordering elements, as it only returns 0 or 1.
   * @return 0 if o1.equals(o2), 1 otherwise,
   */
  public int compare(T o1,T o2) {
    if (o1.equals(o2)) return 0;
    // we don't really care about inequality:
    return 1;
  }

  /** Returns true iff this == object */
  public boolean equals(Object obj) { 
    return obj == this; 
  }

} // end of class EqualsComparator
