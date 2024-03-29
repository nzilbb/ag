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
package nzilbb.converter;

import javax.swing.filechooser.FileNameExtensionFilter;
import nzilbb.ag.Constants;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.serialize.GraphDeserializer;
import nzilbb.ag.serialize.GraphSerializer;
import nzilbb.formatter.praat.TextGridSerialization;
import nzilbb.formatter.transcriber.TranscriptSerialization;
import nzilbb.util.ProgramDescription;

/**
 * Converts Transcriber .trs files to Praat TextGrids.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Converts Transcriber .trs files to Praat TextGrids",arguments="file1.trs file2.trs ...")
public class TrsToTextGrid extends Converter {
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public TrsToTextGrid() {
     info = "Praat does not support meta-data as Transcriber does,"
       +" so the following meta-data is lost during conversion:"
       +"\n- version"
       +"\n- version date"
       +"\n- air date"
       +"\n- scribe"
       +"\n- language"
       +"\n- participant gender"
       +"\n- participant dialect"
       +"\n- participant accent"
       +"\n- participant scope"
       +"\nThe following Transcriber annotations are not supported by Praat, and are lost:"
       +"\n- phrase language annotations"
       +"\n- named entity annotations"
       +"\n- topic tags"
       +"\nThe following Transcriber annotations are not directly supported by Praat, and"
       +" are converted using bracketed, inline conventions within annotation labels:"
       +"\n- comments"
       +"\n- noises"
       +"\n- lexical tags"
       +"\n- pronounce tags"
       +"\nTo disable these conventions (and thus lose these annotations during conversion)"
       +" use the --useConventions=false command line switch.";
   } // end of constructor
   
   public static void main(String argv[]) {
      new TrsToTextGrid().mainRun(argv);
   }

   /** File filter for identifying files of the correct type */
   protected FileNameExtensionFilter getFileFilter() {
      return new FileNameExtensionFilter("Transcriber files", "trs");
   }

   /**
    * Gets the deserializer that #convert(File) uses.
    * @return The deserializer to use.
    */
   public GraphDeserializer getDeserializer() {
      return new TranscriptSerialization();
   }

   /**
    * Gets the serializer that #convert(File) uses.
    * @return The serializer to use.
    */
   public GraphSerializer getSerializer() {
      return new TextGridSerialization();
   }   
   
   private static final long serialVersionUID = -1;
} // end of class TrsToTextGrid
