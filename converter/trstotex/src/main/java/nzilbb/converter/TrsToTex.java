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
import nzilbb.formatter.transcriber.TranscriptSerialization;
import nzilbb.util.ProgramDescription;

/**
 * Converts Transcriber .trs files to LaTeX documents.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Converts Transcriber .trs files to LaTeX documents",arguments="file1.trs file2.trs ...")
public class TrsToTex extends Converter {
   
  // Methods:
   
  /**
   * Default constructor.
   */
  public TrsToTex() {
    info = "The resulting .tex files each include a definition for a new '\turn' command which"
      +" is used throughout the trancript to format speaker turns; this can be customized"
      +" directly in the .tex files after conversion, or with the --texTurnCommand command line"
      +" switch. e.g. \"--texTurnCommand=\\item[#1:] #2\""
      +"\n (#1 = Speaker ID, #2 = Turn Text)";
  } // end of constructor
   
  public static void main(String argv[]) {
    new TrsToTex().mainRun(argv);
  }

  /**
   * Specify the schema to used by  {@link #convert(File)}.
   * @return The schema.
   */
  @Override
  public Schema getSchema() {
    Schema schema = super.getSchema();
    schema.addLayer(
      new Layer("transcript_recording_date", "Recording date")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_scribe", "Scribe")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_language", "Main language")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("topic", "Topic").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false));
    schema.addLayer(
      new Layer("noise", "Noises").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false));
    schema.addLayer(
      new Layer("comment", "Comment")         
      .setAlignment(Constants.ALIGNMENT_INSTANT)
      .setPeers(true).setPeersOverlap(false).setSaturated(false));
    schema.addLayer(
      new Layer("language", "Phrase Language").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("entity", "Proper Names").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("pronounce", "Pronunciation tags").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getWordLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("lexical", "Lexical tags").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getWordLayerId()).setParentIncludes(true));
    return schema;
  } // end of getSchema()

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
    return new LatexSerializer();
  }
  
  /**
   * Specifies which layers should be given to the serializer. The default implementaion
   * returns only the "utterance" layer.
   * @return An array of layer IDs.
   */
  public String[] getLayersToSerialize() {
    String[] layers = { "utterance", "comment", "noise" };
    return layers;
  } // end of getLayersToSerialize()
   
  private static final long serialVersionUID = -1;
} // end of class TrsToTex
