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
import nzilbb.formatter.webvtt.VttSerialization;
import nzilbb.formatter.transcriber.TranscriptSerialization;
import nzilbb.util.ProgramDescription;

/**
 * Converts Transcriber .trs files to Web VTT subtitle format.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Converts Transcriber .trs files to Web VTT subtitles",arguments="file1.trs file2.trs ...")
public class TrsToVtt extends Converter {
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public TrsToVtt() {
     info = "As WebVTT is a subtitle format, not related to a linguistic annotation tool,"
       +" all annotations that Transcriber supports are lost during this conversion, including:"
       +"\n- topics"
       +"\n- comments"
       +"\n- noises"
       +"\n- phrase language annotations"
       +"\n- named entity annotations"
       +"\n- lexical tags"
       +"\n- pronounce tags"
       +"\n "
       +"\nAlthough WebVTT does not support meta-data, the meta-data that is present in the"
       +" Transcriber transcript is included in the .vtt file as NOTEs, including:"
       +"\n- version"
       +"\n- version date"
       +"\n- air date"
       +"\n- scribe"
       +"\n- language"
       +"\n- participant gender"
       +"\n- participant dialect"
       +"\n- participant accent"
       +"\n- participant scope";
   } // end of constructor
   
   public static void main(String argv[]) {
      new TrsToVtt().mainRun(argv);
   }

  /**
   * Specify the schema to used by  {@link #convert(File)}.
   * @return The schema.
   */
  public Schema getSchema() {
    Schema schema = super.getSchema();
    // transcriber layers...

    // meta data
    schema.addLayer(
      new Layer("transcript_language", "Main Language").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_scribe", "Scribe").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_version", "Version").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_version_date", "Version Date").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_air_date", "Air Date").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));

    // participant meta data
    schema.addLayer(
      new Layer("participant_gender", "Gender").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("participant_dialect", "Dialect").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("participant_accent", "Accent").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("participant_scope", "Scope").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));

    // span layers
    schema.addLayer(
      new Layer("topic", "Topics").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false));
    schema.addLayer(
      new Layer("noise", "Noises").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false));
    schema.addLayer(
      new Layer("comment", "Comments").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(true).setSaturated(false));

    // phrase layers
    schema.addLayer(
      new Layer("language", "Phrase Language").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("entity", "Named Entity").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));

    // word tag layers
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
      return new VttSerialization();
   }   
   
  /**
   * Specifies which layers should be given to the serializer. The default implementaion
   * returns only the "utterance" layer.
   * @return An array of layer IDs.
   */
  @Override
  public String[] getLayersToSerialize() {
    String[] layers = {
      "utterance", "word",
      // add meta-data as NOTEs
      "transcript_language", "transcript_scribe", "transcript_version", "transcript_version_date",
      "transcript_air_date",
      "participant_gender", "participant_dialect", "participant_accent", "participant_scope"};
    return layers;
  } // end of getLayersToSerialize()
   private static final long serialVersionUID = -1;
} // end of class TrsToVtt
