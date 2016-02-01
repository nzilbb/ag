//
// (c) 2015, Robert Fromont - robert@fromont.net.nz
//
//
//    This module is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 3 of the License, or
//    (at your option) any later version.
//
//    This module is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this module; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
      Object oConfidence = o.get("confidence");
      if (oConfidence == null) return defaultConfidence;
      if (!(oConfidence instanceof Integer)) return defaultConfidence;
      return ((Integer)oConfidence).intValue();
   } // end of getConfidence()

} // end of class Utility
