//
// Copyright 2020 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.encoding;

import java.util.function.UnaryOperator;

/**
 * Function that converts phonemic transcriptions from one encoding to another.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class PhonemeTranslator implements UnaryOperator<String> {
   
   // Attributes:
   
   /**
    * The name of the source encoding.
    * @see #getSourceEncoding()
    */
   protected String sourceEncoding;
   /**
    * Getter for {@link #sourceEncoding}: The name of the source encoding.
    * @return The name of the source encoding.
    */
   public String getSourceEncoding() { return sourceEncoding; }

   /**
    * The name of the destination encoding.
    * @see #getDestinationEncoding()
    */
   protected String destinationEncoding;
   /**
    * Getter for {@link #destinationEncoding}: The name of the destination encoding.
    * @return The name of the destination encoding.
    */
   public String getDestinationEncoding() { return destinationEncoding; }
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public PhonemeTranslator() {
   } // end of constructor
   
   /**
    * Translates a phonemic transcription from the source encoding to the destination encoding.
    * <p> The default implementation simply returns the source transcription.
    * @param source Phonemic transcription in the source encoding.
    * @return Phonemic transcription in the destination encoding.
    */ 
   public String apply(String source) {
      return source;
   }
   
} // end of class PhonemeTranslator
