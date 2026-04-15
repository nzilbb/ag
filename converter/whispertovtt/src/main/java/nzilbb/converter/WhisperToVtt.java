//
// Copyright 2026 New Zealand Institute of Language, Brain and Behaviour, 
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
import nzilbb.formatter.webvtt.VttSerialization;
import nzilbb.formatter.whisper.WhisperDeserializer;
import nzilbb.util.ProgramDescription;

/**
 * Converts Whisper ASR's JSON-formatted output to WebVTT subtitles.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Converts JSON-formatted Whisper ASR files to WebVTT subtitles",arguments="file1.json file2.json ...")
public class WhisperToVtt extends Converter {
  
  /**
   * Default constructor.
   */
  public WhisperToVtt() {
  } // end of constructor
  
  public static void main(String argv[]) {
    new WhisperToVtt().mainRun(argv);
  }
  
  /** File filter for identifying files of the correct type */
  protected FileNameExtensionFilter getFileFilter() {
    return new FileNameExtensionFilter("Whisper transcripts", "json");
  }
  
  /**
   * Gets the deserializer that #convert(File) uses.
   * @return The deserializer to use.
   */
  public GraphDeserializer getDeserializer() {
    return new WhisperDeserializer();
  }
  
  /**
   * Gets the serializer that #convert(File) uses.
   * @return The serializer to use.
   */
  public GraphSerializer getSerializer() {
    return new VttSerialization();
  }
  
  private static final long serialVersionUID = -1;
} // end of class WhisperToVtt
