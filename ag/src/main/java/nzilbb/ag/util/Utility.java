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

import nzilbb.ag.*;

/**
 * Some handy static functions.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class Utility
{
   // Methods:

   /**
    * Gets the confidence rating of a given object.  If no Integer confidence attribute is present, the given default value is returned.
    * @param o The object to get the confidence rating for (most likely an {@link Annotation} or {@link Anchor})
    * @param defaultConfidence The default rating if no explicit value is set.
    * @return The confidence rating of a given object, or <var>defaultConfidence</var> if it could not be determined.
    */
   public static int getConfidence(TrackedMap o, int defaultConfidence)
   {
      Integer c = o.getConfidence();
      if (c == null) return defaultConfidence;
      return c;
   } // end of getConfidence()

   /**
    * Gets the confidence rating of a given object.  If no Integer confidence attribute is present, {@link Constants#CONFIDENCE_MANUAL} is returned.
    * @param o The object to get the confidence rating for (most likely an {@link Annotation} or {@link Anchor})
    * @return The confidence rating of a given object, or {@link Constants#CONFIDENCE_MANUAL} if it could not be determined.
    */
   public static int getConfidence(TrackedMap o)
   {      
      return getConfidence(o, Constants.CONFIDENCE_MANUAL);
   } // end of getConfidence()

   /**
    * Sets the confidence rating of a given object.
    * @param o The object to get the confidence rating for (most likely an {@link Annotation} or {@link Anchor})
    * @param confidence The confidence rating for a given object.
    */
   public static void setConfidence(TrackedMap o, int confidence)
   {      
      o.setConfidence(confidence);
   } // end of setConfidence()

} // end of class Utility
