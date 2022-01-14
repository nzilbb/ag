//
// Copyright 2022 New Zealand Institute of Language, Brain and Behaviour, 
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
import nzilbb.ag.Annotation;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.TransformationException;
import nzilbb.ag.util.DefaultOffsetGenerator;
import nzilbb.ag.serialize.GraphDeserializer;
import nzilbb.ag.serialize.GraphSerializer;
import nzilbb.configure.ParameterSet;
import nzilbb.formatter.elan.EAFSerialization;
import nzilbb.formatter.text.PlainTextSerialization;
import nzilbb.util.ProgramDescription;

/**
 * Converts time-aligned plain text .txt files to ELAN .eaf files.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Converts time-aligned plain text .txt transcripts to ELAN .eaf files",arguments="file1.txt file2.txt ...")
public class TextToEaf extends Converter {
  
  /**
   * Formatn for makring a chnage of turn within the transcript body, e.g. "{0}: "
   * @see #getParticipantFormat()
   * @see #setParticipantFormat(String)
   */
  protected String participantFormat = "{0}: ";
  /**
   * Getter for {@link #participantFormat}: Formatn for makring a chnage of turn within
   * the transcript body, e.g. "{0}: " 
   * @return Formatn for makring a chnage of turn within the transcript body, e.g. "{0}: "
   */
  public String getParticipantFormat() { return participantFormat; }
  /**
   * Setter for {@link #participantFormat}: Formatn for makring a chnage of turn within
   * the transcript body, e.g. "{0}: " 
   * @param newParticipantFormat Formatn for makring a chnage of turn within the
   * transcript body, e.g. "{0}: " 
   */
  public TextToEaf setParticipantFormat(String newParticipantFormat) { participantFormat = newParticipantFormat; return this; }
  
  /**
   * Format for time synchronizations withint the transcript body, e.g. HH:mm:ss.SSS
   * @see #getTimestampFormat()
   * @see #setTimestampFormat(String)
   */
  protected String timestampFormat = "HH:mm:ss.SSS";
  /**
   * Getter for {@link #timestampFormat}: Format for time synchronizations withint the
   * transcript body, e.g. HH:mm:ss.SSS 
   * @return Format for time synchronizations withint the transcript body, e.g. HH:mm:ss.SSS
   */
  public String getTimestampFormat() { return timestampFormat; }
  /**
   * Setter for {@link #timestampFormat}: Format for time synchronizations withint the
   * transcript body, e.g. HH:mm:ss.SSS 
   * @param newTimestampFormat Format for time synchronizations withint the transcript
   * body, e.g. HH:mm:ss.SSS 
   */
  public TextToEaf setTimestampFormat(String newTimestampFormat) { timestampFormat = newTimestampFormat; return this; }
  
  /**
   * Default constructor.
   */
  public TextToEaf() {
    info = "The plain text transcript must include synchronisation information - i.e. time codes"
      +"\n- and must end in a timecode, indicating the end time of the last utterance.";
  } // end of constructor
  
  public static void main(String argv[]) {
    new TextToEaf().mainRun(argv);
  }
  
  /** File filter for identifying files of the correct type */
  protected FileNameExtensionFilter getFileFilter() {
    return new FileNameExtensionFilter("Plain text transcripts", "txt");
  }

  /**
   * Gets the deserializer that #convert(File) uses.
   * @return The deserializer to use.
   */
  public GraphDeserializer getDeserializer() {
    return new PlainTextSerialization();
  }
  
  /**
   * Adjust the configuration of the deserializer. Implementors can adjust the
   * default configuration before it's applied. This method is invoked once for each
   * input file.
   * @param config The default configuration.
   * @return The new configuration.
   */
  @Override
  public ParameterSet deserializerConfiguration(ParameterSet config) {
    config.get("participantFormat").setValue(getParticipantFormat());
    config.get("timestampFormat").setValue(getTimestampFormat());
    return config;
  } // end of serializerConfiguration()
  
  /**
   * Gets the serializer that #convert(File) uses.
   * @return The serializer to use.
   */
  public GraphSerializer getSerializer() {
    return new EAFSerialization();
  }
  
  /**
   * Process the transcripts after they were deserialized, but before they're
   * serialized. Implementors can rename speakers, adjust meta-data, or change the graph
   * in any other way required before serialization. 
   * @param transcripts
   */
  public void processTranscripts(Graph[] transcripts) {
    for (Graph transcript : transcripts) {
      // need default word offsets for ELAN to not ignore them
      try {
        new DefaultOffsetGenerator().transform(transcript);
      } catch(TransformationException exception) {
        System.err.println(exception.getMessage());
      }
    }
  } // end of processTranscripts()
  
  /**
   * Specify the schema to used by  {@link #convert(File)}.
   * @return The schema.
   */
  public Schema getSchema() { // TODO need this?
    Schema schema = super.getSchema();
    return schema;
  } // end of getSchema()

  /**
   * Specifies which layers should be given to the serializer. The default implementaion
   * returns only the "utterance" layer.
   * @return An array of layer IDs.
   */
  public String[] getLayersToSerialize() {
    String[] layers = { "utterance" };
    return layers;
  } // end of getLayersToSerialize()
      
  private static final long serialVersionUID = -1;
} // end of class TextToEaf
