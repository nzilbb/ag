//
// Copyright 2021 New Zealand Institute of Language, Brain and Behaviour, 
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
import nzilbb.formatter.latex.LatexSerializer;
import nzilbb.formatter.webvtt.VttSerialization;
import nzilbb.util.ProgramDescription;

/**
 * Converts WebVTT subtitle files to LaTeX documents.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Converts WebVTT subtitle files to LaTeX documents",arguments="file1.vtt file2.vtt ...")
public class VttToTex extends Converter {
   
  /**
   * Default constructor.
   */
  public VttToTex() {
    setDefaultWindowTitle("WebVTT to LaTeX converter");
    info = "The resulting .tex files each include a definition for a new '\turn' command which"
      +" is used throughout the trancript to format speaker turns; this can be customized"
      +" directly in the .tex files after conversion, or with the --texTurnCommand command line"
      +" switch. e.g. \"--texTurnCommand=\\item[#1:] #2\""
      +"\n (#1 = Speaker ID, #2 = Turn Text)";
  } // end of constructor
  
  public static void main(String argv[]) {
    new VttToTex().mainRun(argv);
  }
  
  /** File filter for identifying files of the correct type */
  protected FileNameExtensionFilter getFileFilter() {
    return new FileNameExtensionFilter("WebVTT files", "vtt");
  }
  
  /**
   * Gets the deserializer that #convert(File) uses.
   * @return The deserializer to use.
    */
  public GraphDeserializer getDeserializer() {
    return new VttSerialization();
  }
  
  /**
   * Gets the serializer that #convert(File) uses.
   * @return The serializer to use.
   */
  public GraphSerializer getSerializer() {
    return new LatexSerializer();
  }
  
  private static final long serialVersionUID = -1;
} // end of class VttToTex
