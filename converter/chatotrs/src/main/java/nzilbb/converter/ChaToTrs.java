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
import nzilbb.ag.Annotation;
import nzilbb.ag.Anchor;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.serialize.GraphDeserializer;
import nzilbb.ag.serialize.GraphSerializer;
import nzilbb.formatter.clan.ChatSerialization;
import nzilbb.formatter.transcriber.TranscriptSerialization;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;

/**
 * Converts CLAN CHAT transcripts to Transcriber transcripts.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Converts CLAN CHAT transcripts to Transcriber files",arguments="file1.cha file2.cha ...")
public class ChaToTrs extends Converter {

  /**
   * Default constructor.
   */
  public ChaToTrs() {
    info = "Transcriber doesn't support meta-data like @Recording Quality, @Location, etc."
      +" so CHAT header meta-data is instead included as comments at the beginning of the"
      +" Transcriber transcript."
      +"\n "
      +"\nThis conversion will only work well for CHAT transcripts that are fully aligned;"
      +" i.e. all lines include time alignment bullets."
      +"\n "
      +"\nThe CLAN parser is *not exhaustive*; it one parses:"
      +"\n- Disfluency marking with &+ - e.g. \"so &+sund Sunday\""
      +"\n- Non-standard form expansion - e.g. \"gonna [: going to]\""
      +"\n- Incomplete word completion - e.g. \"dinner doin(g) all\""
      +"\n- Acronym/proper name joining with _ - e.g. \"no T_V in my room\""
      +"\n- Retracing - e.g. \"<some friends and I> [//] uh\" or \"and sit [//] sets him\""
      +"\n- Repetition/stuttered false starts - e.g. \"the <picnic> [/] picnic\" or \"the Saturday [/] in the morning\""
      +"\n- Errors - e.g. \"they've <work up a hunger> [* s:r]\" or \"they got [* m] to\"";
  } // end of constructor
  
  public static void main(String argv[]) {
    new ChaToTrs().mainRun(argv);
  }
  
  /** File filter for identifying files of the correct type */
  protected FileNameExtensionFilter getFileFilter() {
    return new FileNameExtensionFilter("CLAN CHAT transcripts", "cha");
  }

  /**
   * Gets the deserializer that #convert(File) uses.
   * @return The deserializer to use.
   */
  public GraphDeserializer getDeserializer() {
    return new ChatSerialization();
  }
  
  /**
   * Gets the serializer that #convert(File) uses.
   * @return The serializer to use.
   */
  public GraphSerializer getSerializer() {
    return new TranscriptSerialization();
  }
  
  /**
   * Specify the schema to used by  {@link #convert(File)}.
   * @return The schema.
   */
  public Schema getSchema() {
    Schema schema = super.getSchema();
    // include CLAN layers
    schema.addLayer(
      new Layer("transcript_language", "Language").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_scribe", "Transcriber").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_recording_date", "Date").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_recording_quality", "Recording Quality")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_room_layout", "Room Layout").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_tape_location", "Tape Location").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_location", "Location").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("main_participant", "Target Speaker").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("participant_gender", "Gender").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("participant_age", "Age").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("participant_language", "Participant Language")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("participant_corpus", "Corpus").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("participant_group", "Group").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("participant_ses", "SES").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("participant_role", "Role").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("participant_education", "Education").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("cunit", "C-Unit").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("noise", "Non-words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(false).setPeersOverlap(false).setSaturated(false));
    schema.addLayer(
      new Layer("expansion", "Expansions").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getWordLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("error", "Errors").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("linkage", "Linkages").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("repetition", "Repetitions").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("retracing", "Retracing").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("disfluency", "Disfluencies").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("completion", "Completions").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("topic", "Gems").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(false).setPeersOverlap(false).setSaturated(false));
    schema.addLayer(
      new Layer("comment", "Comments").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false));
    return schema;
  } // end of getSchema()

  /**
   * Make recording quality, room layout, tape location, and location into comments.
   * @param transcripts
   */
  @Override
  public void processTranscripts(Graph[] transcripts) {
    Schema schema = getSchema();
    
    for (Graph transcript : transcripts) {
      
      // add meta-data as comments at the beginning
      // so that can be parsed back out to slt if there's a round-trip
      String startId = transcript.getStart().getId();
      Annotation firstUtterance = transcript.first("utterance");
      if (firstUtterance != null) { // use first utterance, it might not start at 0
        startId = firstUtterance.getStart().getId();
      }
      // transcript_recording_quality
      Annotation metadata = transcript.first("transcript_recording_quality");
      if (metadata != null) {
        transcript.addAnnotation(new Annotation().setLayerId("comment")
                                 .setLabel("@Recording Quality: " + metadata.getLabel())
                                 .setStartId(startId).setEndId(startId));
      }
      // transcript_room_layout
      metadata = transcript.first("transcript_room_layout");
      if (metadata != null) {
        transcript.addAnnotation(new Annotation().setLayerId("comment")
                                 .setLabel("@Room Layout: " + metadata.getLabel())
                                 .setStartId(startId).setEndId(startId));
      }
      // transcript_tape_location
      metadata = transcript.first("transcript_tape_location");
      if (metadata != null) {
        transcript.addAnnotation(new Annotation().setLayerId("comment")
                                 .setLabel("@Tape Location: " + metadata.getLabel())
                                 .setStartId(startId).setEndId(startId));
      }
      // location
      metadata = transcript.first("transcript_location");
      if (metadata != null) {
        transcript.addAnnotation(new Annotation().setLayerId("comment")
                                 .setLabel("@Location: " + metadata.getLabel())
                                 .setStartId(startId).setEndId(startId));
      }
      transcript.commit();
    } // next transcript
  } // end of processGraphs()
   

  /**
   * Specifies which layers should be given to the serializer. The default implementaion
   * returns only the "utterance" layer.
   * @return An array of layer IDs.
   */
  @Override
  public String[] getLayersToSerialize() {
    String[] layers = { "utterance", "word", "noise", "topic", "comment",
      "transcript_language", "transcript_recording_date", "transcript_scribe",
      "transcript_room_layout", "transcript_recording_quality", "transcript_tape_location",
      "transcript_location", "participant_gender"};
    return layers;
  } // end of getLayersToSerialize()


  private static final long serialVersionUID = -1;
} // end of class ChaToTrs
