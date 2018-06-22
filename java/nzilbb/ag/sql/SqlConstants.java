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
package nzilbb.ag.sql;

/**
 * Constants relating to SQL annotation stores.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class SqlConstants
{
   /** Database Layer ID for raw transcription layer - i.e. how the transcript was typed in Transcriber */
   public static final int LAYER_TRANSCRIPTION = 0;
   
   /** Database Layer ID for segment layer - i.e. the components of each word (most likely phonemes) */
   public static final int LAYER_SEGMENT = 1;
   
   /** Database Layer ID for orthography layer - i.e. the original text cleaned up to make standard spellings (if possible) */
   public static final int LAYER_ORTHOGRAPHY = 2;
   
   /** Database Layer ID for turn layer - i.e. the speaker turns originally marked in the transcript */
   public static final int LAYER_TURN = 11;
   
   /** Database Layer ID for utterance layer - i.e. the lines/chunks of the transcript as originally time aligned before upload (in Transcriber, the words between Sync points) */
   public static final int LAYER_UTTERANCE = 12;
   
   /** Scope for Episode layer - value is "e" */
   public static final String SCOPE_EPISODE = "e"; 

   /** Scope for Participant layer - value is "p" */
   public static final String SCOPE_PARTICIPANT = "p"; 

   /** Scope for Freeform layers - value is "f" */
   public static final String SCOPE_FREEFORM = "f"; 

   /** Scope for Meta layers - value is "m" */
   public static final String SCOPE_META = "m"; 

   /** Scope for Word layers - value is "w" */
   public static final String SCOPE_WORD = "w";

   /** Scope for Segment layers - value is "s" */
   public static final String SCOPE_SEGMENT = "s";
      

} // end of class SqlConstants
