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

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.LinkedHashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.swing.filechooser.FileNameExtensionFilter;
import nzilbb.ag.Annotation;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.TransformationException;
import nzilbb.ag.serialize.GraphDeserializer;
import nzilbb.ag.serialize.GraphSerializer;
import nzilbb.ag.util.DefaultOffsetGenerator;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.formatter.salt.SltSerialization;
import nzilbb.formatter.transcriber.TranscriptSerialization;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;

/**
 * Converts Transcriber .trs transcripts to SALT .slt transcripts.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Converts Transcriber .trs files to SALT .slt transcripts",arguments="file1.trs file2.trs ...")
public class TrsToSlt extends Converter {
  
  /**
   * Pattern for 'pronounce' word codes.
   * @see #getPronounceCodePattern()
   * @see #setPronounceCodePattern(String)
   */
  protected String pronounceCodePattern = "PRON:{0}";
  /**
   * Getter for {@link #pronounceCodePattern}: Pattern for 'pronounce' word codes.
   * @return Pattern for 'pronounce' word codes.
   */
  public String getPronounceCodePattern() { return pronounceCodePattern; }
  /**
   * Setter for {@link #pronounceCodePattern}: Pattern for 'pronounce' word codes.
   * @param newPronounceCodePattern Pattern for 'pronounce' word codes.
   */
  @Switch("Pattern to match for converting word codes into pronounce events. Default is PRON:{0}")
  public TrsToSlt setPronounceCodePattern(String newPronounceCodePattern) { pronounceCodePattern = newPronounceCodePattern; return this; }
  
  /**
   * Pattern for 'lexical' word codes.
   * @see #getLexicalCodePattern()
   * @see #setLexicalCodePattern(String)
   */
  protected String lexicalCodePattern = "LEX:{0}";
  /**
   * Getter for {@link #lexicalCodePattern}: Pattern for 'lexical' word codes.
   * @return Pattern for 'lexical' word codes.
   */
  public String getLexicalCodePattern() { return lexicalCodePattern; }
  /**
   * Setter for {@link #lexicalCodePattern}: Pattern for 'lexical' word codes.
   * @param newLexicalCodePattern Pattern for 'lexical' word codes.
   */
  @Switch("Pattern to match for converting word codes into lexical events. Default is LEX:{0}")
  public TrsToSlt setLexicalCodePattern(String newLexicalCodePattern) { lexicalCodePattern = newLexicalCodePattern; return this; }
  
  /**
   * Default constructor.
   */
  public TrsToSlt() {
    info = "The Transcriber 'Program' becomes the SALT 'Context' header."
      +"\nThe first Transcriber 'Topic' becomes the SALT 'Subgroup' header."
      +"\nComments at the beginning of transcript that start with + become SALT meta-data headers."
      +"\nBy default  Transcriber 'pronounce' and 'lexical' events are converted to be "
      +" certain SALT word codes, of the form \"[PRON:...]\" and \"[LEX:...]\" respectively."
      +" Use the command-line switches --pronounceCodePattern and --lexicalCodePattern"
      +" to control this behaviour."
      +"\ne.g. if you specify --pronounceCodePattern=WP:{0} then all pronounce events will"
      +" become word codes like [WP:...]"
      +"\nSimilarly if you specify --pronounceCodePattern=WL:{0} then all lexical events will"
      +" become word codes like [WL:...]."
      +"\nTo disable these conversions, use \"--pronounceCodePattern= --lexCodePattern=\""
      +" on the command line."
      +"\nThe format for dates is taken from your system settings;"
      +" to override this, use the --dateFormat command line setting.";
  } // end of constructor
  
  public static void main(String argv[]) {
    new TrsToSlt().mainRun(argv);
  }
  
  /** File filter for identifying files of the correct type */
  protected FileNameExtensionFilter getFileFilter() {
    return new FileNameExtensionFilter("Transcriber transcripts", "trs");
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
    return new SltSerialization();
  }
  
  /**
   * Specify the schema to used by  {@link #convert(File)}.
   * @return The schema.
   */
  @Override
  public Schema getSchema() {
    Schema schema = super.getSchema();
    // include SALT layers
    schema.addLayer(
      new Layer("transcript_language", "Language").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    // transcript_recording_date is a name that both SltSerialization and TranscriptSerialization
    // will recognize by default
    schema.addLayer(
      new Layer("transcript_recording_date", "Recording date")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_ca", "Current Age").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_context", "Context").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_subgroup", "Subgroup/Story").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_collect", "Collection Point").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_location", "Location").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("comment", "Comments").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(true).setSaturated(false));
    schema.addLayer(
      new Layer("main_participant", "Target Speaker").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("participant_id", "Participant ID").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("participant_gender", "Gender").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("participant_dob", "Birth Date").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("participant_ethnicity", "Ethnicity").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    schema.addLayer( // TODO comment pauses
      new Layer("pause", "Pauses").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("error", "Errors").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("code", "Non-error Codes").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("noise", "Verbal sound effects etc.").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("entity", "Proper Names").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    // no partial_word layer, so that the ~ -> * transformation occurs in
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
   * Map SALT Context to Transcriber Program, and SALT Subgroup to Transcriber Topic.
   * @param config The default configuration.
   * @return The new configuration.
   */
  public ParameterSet deserializerConfiguration(ParameterSet config) {
    Schema schema = getSchema();
    config.get("programLayer").setValue(schema.getLayer("transcript_context"));
    config.get("topicLayer").setValue(schema.getLayer("transcript_subgroup"));
    return config;
  } // end of serializerConfiguration()

  /**
   * Parse comments for dob, ca, ethnicity, collection, location, errors and codes, and
   * create lexical/pron word codes if configured.
   * @param transcripts
   */
  @Override
  public void processTranscripts(Graph[] transcripts) {
    Schema schema = getSchema();
    
    MessageFormat pronounceFormat = pronounceCodePattern.length()==0?null
      : new MessageFormat(pronounceCodePattern);
    MessageFormat lexicalFormat = lexicalCodePattern.length()==0?null
      : new MessageFormat(lexicalCodePattern);
    Pattern codeComment = Pattern.compile("\\[([^\\]]+)\\]");
    DefaultOffsetGenerator defaultOffsetGenerator = new DefaultOffsetGenerator();
    for (Graph transcript : transcripts) { // for each transcript (probably only one)
      transcript.setSchema(getSchema());
      transcript.trackChanges();
      try {
        transcript = defaultOffsetGenerator.transform(transcript);
      } catch(TransformationException exception) {
        System.err.println(transcript.getId() + ": " + exception.getMessage());
      }

      // ensure entities share starts/ends with words, so that proper names are correctly delimited
      for (Annotation e : transcript.all("entity")) {

        // start shares with word?
        if (e.getStart().startOf(schema.getWordLayerId()).size() == 0) { // doesn't start at a word
          LinkedHashSet<Annotation> startingUtterances
            = e.getStart().startOf(schema.getUtteranceLayerId());
          if (startingUtterances.size() > 0) {
            Annotation utterance = startingUtterances.iterator().next();
            // relink the entity to the start of the first word
            Annotation[] words = utterance.all(schema.getWordLayerId());
            if (words.length > 0) {
              e.setStart(words[0].getStart());
            }
          }
        }

        // end shares with word?
        if (e.getEnd().endOf(schema.getWordLayerId()).size() == 0) { // doesn't end at a word
          LinkedHashSet<Annotation> endingUtterances
            = e.getEnd().endOf(schema.getUtteranceLayerId());
          if (endingUtterances.size() > 0) {
            Annotation utterance = endingUtterances.iterator().next();
            // relink the entity to the end of the last word
            Annotation[] words = utterance.all(schema.getWordLayerId());
            if (words.length > 0) {
              e.setEnd(words[words.length-1].getEnd());
            }
          }
        }
        
      } // next entity

      // parse header comments and codes
      Annotation participant = transcript.first(schema.getParticipantLayerId());
      String[] headerToLayer = {
        "+ Dob: ", "participant_dob",
        "+ Ethnicity: ", "participant_ethnicity",
        "+ ParticipantId: ", "participant_id",
        "+ Ca: ", "transcript_ca",
        "+ Collect: ", "transcript_collect",
        "+ Location: ", "participant_location" };
      for (Annotation comment : transcript.all("comment")) {

        // header comment?
        for (int i = 0; i < headerToLayer.length; i += 2) {
          String headerPrefix = headerToLayer[i];
          String layerId = headerToLayer[i+1];
          if (comment.getLabel().startsWith(headerPrefix)) {
            Annotation parent = layerId.startsWith("participant")?participant:transcript;
            transcript.createTag(
              parent, layerId, comment.getLabel().substring(headerPrefix.length()));
            comment.destroy();            
          }
        } // next possible header comment

        // code?
        Matcher codeMatcher = codeComment.matcher(comment.getLabel());
        if (codeMatcher.matches()) {
          String codeLabel = codeMatcher.group(1);
          Annotation parent = null;
          LinkedHashSet<Annotation> word
            = comment.getStart().endOf(transcript.getSchema().getWordLayerId());
          if (word.size() > 0) {
            parent = word.iterator().next();
          } else {
            LinkedHashSet<Annotation> utterance
              = comment.getStart().endOf(transcript.getSchema().getUtteranceLayerId());
            if (utterance.size() > 0) {
              parent = utterance.iterator().next();
            }
          }
          if (parent != null) {
            Annotation code = transcript.createTag(parent, "code", codeLabel);
            comment.destroy();
          }
        }
      } // next comment
      
      if (pronounceFormat != null) { // some codes are pronounce events
        for (Annotation code : transcript.all("pronounce")) {
          // does the code tag a word?
          Annotation[] word = code.tagsOn(transcript.getSchema().getWordLayerId());
          if (word.length > 0) {
            // create a code tag
            Object[] codeLabel = { code.getLabel() }; 
            transcript.createTag(word[0], "code", pronounceFormat.format(codeLabel));
          }
        } // next code
      }  // some codes are pronounce events
        
      if (lexicalFormat != null) { // some codes are lexical events
        for (Annotation code : transcript.all("lexical")) {        
          // does the code tag a word?
          Annotation[] word = code.tagsOn(transcript.getSchema().getWordLayerId());
          if (word.length > 0) {
            // create a code tag
            Object[] codeLabel = { code.getLabel() }; 
            transcript.createTag(word[0], "code", lexicalFormat.format(codeLabel));
          }
        } // next code
      }  // some codes are lexical events
      
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
    String[] layers = { "utterance", "word" };
    return layers;
  } // end of getLayersToSerialize()

  private static final long serialVersionUID = -1;
} // end of class TrsToSlt
