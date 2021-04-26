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
import nzilbb.formatter.elan.EAFSerialization;
import nzilbb.formatter.transcriber.TranscriptSerialization;
import nzilbb.util.ProgramDescription;

/**
 * Converts Transcriber .trs files to ELAN .eaf files.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Converts Transcriber .trs transcripts to ELAN .eaf files",arguments="file1.trs file2.trs ...")
public class TrsToEaf extends Converter {
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public TrsToEaf() {
     info = "ELAN does not support the same meta-data that Transcriber does,"
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
       +"\nThe following Transcriber annotations are not supported by ELAN, and are lost:"
       +"\n- phrase language annotations"
       +"\n- named entity annotations"
       +"\nThe following Transcriber annotations are not directly supported by ELAN, and"
       +" are converted using bracketed, inline conventions within annotation labels:"
       +"\n- comments"
       +"\n- noises"
       +"\n- lexical tags"
       +"\n- pronounce tags"
       +"\nTo disable these conventions (and thus lose these annotations during conversion)"
       +" use the --useConventions=false command line switch."
       +"\nIf the Transcriber transcript includes topic tags, these are included in the ELAN"
       +" file on their own tier.";
   } // end of constructor
   
   public static void main(String argv[]) {
      new TrsToEaf().mainRun(argv);
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
      return new EAFSerialization();
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

   /**
    * Specifies which layers should be given to the serializer. The default implementaion
    * returns only the "utterance" layer.
    * @return An array of layer IDs.
    */
   public String[] getLayersToSerialize() {
     String[] layers = { "utterance", "topic", 
       "transcript_language", "transcript_scribe" };
      return layers;
   } // end of getLayersToSerialize()
      
   private static final long serialVersionUID = -1;
} // end of class TrsToEaf
