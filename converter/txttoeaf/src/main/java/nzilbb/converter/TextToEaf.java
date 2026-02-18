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

import java.util.Arrays;
import java.util.Optional;
import java.util.Vector;
import javax.swing.filechooser.FileNameExtensionFilter;
import nzilbb.ag.Anchor;
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
   * Format for marking a change of turn within the transcript body, e.g. "{0}: "
   * @see #getParticipantFormat()
   * @see #setParticipantFormat(String)
   */
  protected String participantFormat = "{0}: ";
  /**
   * Getter for {@link #participantFormat}: Format for marking a change of turn within
   * the transcript body, e.g. "{0}: " 
   * @return Format for marking a change of turn within the transcript body, e.g. "{0}: "
   */
  public String getParticipantFormat() { return participantFormat; }
  /**
   * Setter for {@link #participantFormat}: Format for marking a change of turn within
   * the transcript body, e.g. "{0}: " 
   * @param newParticipantFormat Format for marking a change of turn within the
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
    info = "The plain text transcript must include synchronisation information"
      +"\n- i.e. time codes - and must end in a timecode, indicating the end time"
      +"\nof the last utterance."
      +"\n"
      +"\nConsecutive lines without intervening time codes will be merged into one"
      +"\nELAN annotation."
      +"\n"
      +"\nCheck the --timestampFormat setting matches your time codes."
      +"\nThis setting uses Java SimpleDateFormat format:"
      +"\nhttps://docs.oracle.com/javase/8/docs/api/index.html?java/text/SimpleDateFormat.html";
    // need to default utterance/word offsets before normalization
    setDefaultOffsetGenerator(new DefaultOffsetGenerator());
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
    return new EAFSerialization()
      .setIgnoreBlankAnnotations(true);
  }
  
  /**
   * Process the transcripts after they were deserialized, but before they're
   * serialized. Implementors can rename speakers, adjust meta-data, or change the graph
   * in any other way required before serialization. 
   * @param transcripts
   */
  public void processTranscripts(Graph[] transcripts) {
    for (Graph transcript : transcripts) {

      // texts can have consecutive lines with no intervening offset
      // ELAN files can't, so join these together
      transcript.trackChanges(); // in case we delete utterances
      for (Annotation utterance : transcript.all(getSchema().getUtteranceLayerId())) {
        if (utterance.getStart().getOffset() == null // not sure of the end
            || utterance.getStart().getConfidence() == null
            || utterance.getStart().getConfidence() < Constants.CONFIDENCE_MANUAL) {
          final Annotation participant = utterance.first(getSchema().getParticipantLayerId());
          
          Optional<Annotation> previousUtterance = utterance.getStart()
            // immediately preceding utterance...
            .endingAnnotations(getSchema().getUtteranceLayerId())
            // ...with the same speaker
            .filter(u -> u.first(getSchema().getParticipantLayerId()) == participant)
            .findAny();
          if (previousUtterance.isPresent()) {
            mergeAnnotations(previousUtterance.get(), utterance);
          }
        }
      } // next utterance
      transcript.commit();
    }
  } // end of processTranscripts()
  
  /**
   * Moves all of the children of the following peer into the preceding peer, sets the
   * end of the preceding to the end of the following, and marks the following for
   * deletion. 
   * @param preceding The preceding, surviving annotation.
   * @param following The following annotation, which will be deleted.
   */
  public void mergeAnnotations(Annotation preceding, Annotation following) {

    Anchor originalPrecedingEnd = preceding.getEnd();
    // set anchor
    if (preceding.getEnd().getOffset() == null
        || following.getEnd().getOffset() == null
        || preceding.getEnd().getOffset() < following.getEnd().getOffset()) {
      preceding.setEnd(following.getEnd());
    }
    
    // for each child layer
    for (String childLayerId : following.getAnnotations().keySet()) {
      
      // move everything from following to preceding
      int ordinal = 1;
      if (preceding.getAnnotations().containsKey(childLayerId)) {
        ordinal = preceding.getAnnotations().get(childLayerId).size() + 1;
      }
      for (Annotation child : following.annotations(childLayerId)) {
        // in order to prevent the annotation from checking/correcting all peer ordinals
        // which is time-consuming and unnecessary, we first unset the parent
        child.setParent(null);
        // then we set the ordinal
        child.setOrdinal(ordinal++);
        // and finally, we set the new parent, without appending (to skip the peer-checking step)
        child.setParent(preceding, false);
      } // next child annotation

      // saturated child layers can't have gaps
      if (preceding.getGraph().getLayer().getSaturated()) {
        // children linked to the original preceding end need re-linking to the following start
        Vector<Annotation> endingPrecedingChildren = // avoid ConcurrentModificationException
          new Vector<Annotation>(originalPrecedingEnd.endOf(childLayerId));
        for (Annotation endingPrecedingChild : endingPrecedingChildren) {
          // but only our own children
          if (endingPrecedingChild.getParentId().equals(preceding.getId())) {
            endingPrecedingChild.setEnd(following.getStart());
            
            // and any annotations on other layers that are also linked
            // (create a new collection to avoid ConcurrentModificationException)
            new Vector<Annotation>(
              originalPrecedingEnd.getEndingAnnotations()).stream()            
              .filter(ending -> !ending.getLayerId().equals(preceding.getLayerId()))
              .filter(ending -> !ending.getLayerId().equals(childLayerId))
              .forEach(endingOtherLayer -> {
                  endingOtherLayer.setEnd(following.getStart());
              });
          } // own child
        } // next child ending here
      } // saturated child layer
    } // next child layer


    following.destroy();
  } // end of mergeAnnotations()
  
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
