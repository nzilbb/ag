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

/**
 * Useful annotation constants.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class Constants
{
   /** Standardized attribute name (key) for anchor offset or annotation label confidence. Value is "confidence". */
   public static final String CONFIDENCE = "confidence";

   /** Standardized possible value for anchor offset or annotation label confidence - no confidence. Value is 0. */
   public static final int CONFIDENCE_NONE = 0;

   /** Standardized possible value for anchor offset or annotation label confidence - low confidence computed - e.g. anchor offsets set by linear interpolation. Value is 10. */
   public static final int CONFIDENCE_DEFAULT = 10;

   /** Standardized possible value for anchor offset or annotation label confidence - confidence unknown - e.g. a graph that was saved with no confidence information. Value is 25. */
   public static final int CONFIDENCE_UNKNOWN = 25;

   /** Standardized possible value for anchor offset or annotation label confidence - high confidence computed - e.g. anchor offset from automated forced alignment, or annotaton label computed by an automated classifier. Value is 50. */
   public static final int CONFIDENCE_AUTOMATIC = 50;

   /** Standardized possible value for anchor offset or annotation label confidence - highest confidence, as was set by a human annotator. Value is 100. */
   public static final int CONFIDENCE_MANUAL = 100;

   /** {@link Layer#getAlignment()} value - no alignment (0). */
   public static final int ALIGNMENT_NONE = 0;

   /** {@link Layer#getAlignment()} value - instant alignment (1). */
   public static final int ALIGNMENT_INSTANT = 3;

   /** {@link Layer#getAlignment()} value - interval alignment (2). */
   public static final int ALIGNMENT_INTERVAL = 2;


} // end of class Constants
