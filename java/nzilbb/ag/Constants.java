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
package nzilbb.ag;

/**
 * Useful annotation constants.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class Constants
{

   /** API version */
   public static final String VERSION = "20200226.1031";
   
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
   public static final int ALIGNMENT_INSTANT = 1;

   /** {@link Layer#getAlignment()} value - interval alignment (2). */
   public static final int ALIGNMENT_INTERVAL = 2;

   /** {@link Graph#getOffsetUnits()} value for seconds. Value is "s" */
   public static final String UNIT_SECONDS = "s";

   /** {@link Graph#getOffsetUnits()} value for characters. Value is "char" */
   public static final String UNIT_CHARACTERS = "char";

   /** {@link Graph#getOffsetUnits()} value for millisecond accuracy. Value is 0.001 */
   public static final Double GRANULARITY_MILLISECONDS = 0.001;

   /** Standardized attribute name (key) for a comment. Value is "comment". */
   public static final String COMMENT = "comment";

   /** Possible value for {@link Layer#getType()} representing a string of text - "string". */
   public static final String TYPE_STRING = "string";
   /** Possible value for {@link Layer#getType()} representing a string of phonemes using unicode IPA - "ipa". */
   public static final String TYPE_IPA = "ipa";
   /** Possible value for {@link Layer#getType()} representing a number - "number". */
   public static final String TYPE_NUMBER = "number";
   /** Possible value for {@link Layer#getType()} representing a number - "number". */
   public static final String TYPE_BOOLEAN = "boolean";
   /** Possible value for {@link Layer#getType()} representing a string value select from a closed set of possibilities - "select". 
    * @see Layer#getValidLabels()
    */
   public static final String TYPE_SELECT = "select";
   

} // end of class Constants
