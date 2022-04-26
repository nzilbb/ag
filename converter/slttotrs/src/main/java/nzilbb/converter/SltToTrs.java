//
// Copyright 2021-2022 New Zealand Institute of Language, Brain and Behaviour, 
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
import javax.swing.filechooser.FileNameExtensionFilter;
import nzilbb.ag.Annotation;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.serialize.GraphDeserializer;
import nzilbb.ag.serialize.GraphSerializer;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.formatter.salt.SltSerialization;
import nzilbb.formatter.transcriber.TranscriptSerialization;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;

/**
 * Converts SALT .slt transcripts to Transcriber .trs transcripts.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Converts SALT .slt transcripts to Transcriber .trs files",arguments="file1.slt file2.slt ...")
public class SltToTrs extends Converter {
  
  /**
   * Pattern for 'pronounce' word codes.
   * @see #getPronounceCodePattern()
   * @see #setPronounceCodePattern(String)
   */
  protected String pronounceCodePattern = "PRONOUNCE:{0}";
  /**
   * Getter for {@link #pronounceCodePattern}: Pattern for 'pronounce' word codes.
   * @return Pattern for 'pronounce' word codes.
   */
  public String getPronounceCodePattern() { return pronounceCodePattern; }
  /**
   * Setter for {@link #pronounceCodePattern}: Pattern for 'pronounce' word codes.
   * @param newPronounceCodePattern Pattern for 'pronounce' word codes.
   */
  @Switch("Pattern to match for converting word codes into pronounce events. Default is PRONOUNCE:{0}")
  public SltToTrs setPronounceCodePattern(String newPronounceCodePattern) { pronounceCodePattern = newPronounceCodePattern; return this; }
  
  /**
   * Pattern for 'lexical' word codes.
   * @see #getLexicalCodePattern()
   * @see #setLexicalCodePattern(String)
   */
  protected String lexicalCodePattern = "LEXICAL:{0}";
  /**
   * Getter for {@link #lexicalCodePattern}: Pattern for 'lexical' word codes.
   * @return Pattern for 'lexical' word codes.
   */
  public String getLexicalCodePattern() { return lexicalCodePattern; }
  /**
   * Setter for {@link #lexicalCodePattern}: Pattern for 'lexical' word codes.
   * @param newLexicalCodePattern Pattern for 'lexical' word codes.
   */
  @Switch("Pattern to match for converting word codes into lexical events. Default is LEXICAL:{0}")
  public SltToTrs setLexicalCodePattern(String newLexicalCodePattern) { lexicalCodePattern = newLexicalCodePattern; return this; }
  
  
  /**
   * Pattern for noise-event comments.
   * @see #getNoiseCommentPattern()
   * @see #setNoiseCommentPattern(String)
   */
  protected String noiseCommentPattern = "NOISE:{0}";
  /**
   * Getter for {@link #noiseCommentPattern}: Pattern for noise-event comments.
   * @return Pattern for noise-event comments.
   */
  public String getNoiseCommentPattern() { return noiseCommentPattern; }
  /**
   * Setter for {@link #noiseCommentPattern}: Pattern for noise-event comments.
   * @param newNoiseCommentPattern Pattern for noise-event comments.
   */
  @Switch("Pattern to use for converting comments into noise events. Default is NOISE:{0}")
  public SltToTrs setNoiseCommentPattern(String newNoiseCommentPattern) { noiseCommentPattern = newNoiseCommentPattern; return this; }

  /**
   * Default constructor.
   */
  public SltToTrs() {
    // default to false, as it's what users of this converter most likely expect,
    // and it means that if the idea is a round-trip conversion, inline annotations are not lost
    setSwitch("parseInlineConventions", "false");
    
    info = "The SALT 'Context' header becomes the Transcriber 'Program'."
      +"\nThe SALT 'Subgroup' header becomes the Transcriber 'Topic'."
      +"\nSALT meta-data that is not supported by Transcriber is added in comments"
      +" at the beginning of the transcript."
      +"\nBy default, inline annotations (mazes, codes, bound morphemes, etc.)"
      +" are not iterpreted. If you want them to be processed, use --parseInlineConventions"
      +"\nBy default certain SALT word codes are converted into Transcriber 'pronounce'"
      +" or 'lexical' events - i.e. those of the form \"[PRONOUNCE:...]\" and \"[LEXICAL:...]\""
      +" respectively."
      +"\nAlso certain SALT comments are converted into Transcriber 'noise' events"
      +" - i.e. those of the form \"{NOISE:...}\"."
      +" Use the command-line switches --pronounceCodePattern, --lexicalCodePattern,"
      +" and --noiseCommentPattern to control this behaviour."
      +"\ne.g. if you specify --pronounceCodePattern=WP:{0} then all word codes like"
      +" [WP:...] will be pronounce events in the Transcriber transcript."
      +"\nSimilarly if you specify --pronounceCodePattern=WL:{0} then all word codes like"
      +" [WL:...] will be lexical events in the Transcriber transcript."
      +"\nSimilarly if you specify --noiseCommentPattern=BG:{0} then all comments like"
      +" [BG:...] will be noise events in the Transcriber transcript."
      +"\nTo disable these conversions, use"
      +" \"--pronounceCodePattern= --lexicalCodePattern= --noiseCommentPattern=\""
      +" on the command line."
      +"\nThe format for dates is taken from your system settings;"
      +" to override this, use the --dateFormat command line setting.";
  } // end of constructor
  
  public static void main(String argv[]) {
    new SltToTrs().mainRun(argv);
  }
  
  /** File filter for identifying files of the correct type */
  protected FileNameExtensionFilter getFileFilter() {
    return new FileNameExtensionFilter("SALT transcripts", "slt");
  }

  /**
   * Gets the deserializer that #convert(File) uses.
   * @return The deserializer to use.
   */
  public GraphDeserializer getDeserializer() {
    return new SltSerialization();
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
    // noise layer will map automatically for Transcriber because it's top-level,
    // but not for SALT (as the sound-effect layer), because it's not a phrase layer,
    // (SALT sound effects are verbal noises,
    //  but Transcriber noises are non-verbal background noise)
    schema.addLayer(
      new Layer("noise", "Background noises").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
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
    schema.addLayer(
      new Layer("pause", "Pauses").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("cunit", "C-Unit").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("parenthetical", "Parentheticals").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("repetition", "Repetitions").setAlignment(Constants.ALIGNMENT_INTERVAL)
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
      new Layer("maze", "Mazes").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("sound_effects", "Verbal sound effects etc.")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("entity", "Proper Names").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("omission", "Omissions").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId(schema.getTurnLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("root", "Root form").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getWordLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("bound_morpheme", "Bound Morphemes").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getWordLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("partial_word", "Partial Words").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getWordLayerId()).setParentIncludes(true));
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
  public ParameterSet serializerConfiguration(ParameterSet config) {
    Schema schema = getSchema();
    config.get("programLayer").setValue(schema.getLayer("transcript_context"));
    config.get("topicLayer").setValue(schema.getLayer("transcript_subgroup"));
    return config;
  } // end of serializerConfiguration()

  /**
   * Make dob, ca, ethnicity, collection, location, errors and codes into comments, and
   * create lexical/pron word tags if configured.
   * @param transcripts
   */
  @Override
  public void processTranscripts(Graph[] transcripts) {
    Schema schema = getSchema();
    
    MessageFormat pronounceFormat = pronounceCodePattern.length()==0?null
      : new MessageFormat(pronounceCodePattern);
    MessageFormat lexicalFormat = lexicalCodePattern.length()==0?null
      : new MessageFormat(lexicalCodePattern);
    MessageFormat noiseFormat = noiseCommentPattern.length()==0?null
      : new MessageFormat(noiseCommentPattern);
    for (Graph transcript : transcripts) {
      transcript.trackChanges();
      
      if (pronounceFormat != null) {
        transcript.addLayer(schema.getLayer("pronounce"));
      }
      if (lexicalFormat != null) {
        transcript.addLayer(schema.getLayer("lexical"));
      }
      
      if (noiseFormat != null) { // some comments are noise events
        for (Annotation comment : transcript.all("comment")) {
          try { // does it match the pattern?
            Object[] label = noiseFormat.parse(comment.getLabel());
            if (label.length > 0) { // the comment matches the pattern
              // create a noise event
              transcript.createAnnotation(
                comment.getStart(), comment.getEnd(), "noise", (String)label[0], transcript);
              // remove the comment
              comment.destroy();
            }
          } catch(ParseException exception) {}
        } // next comment
      } // noiseFormat set
      // remove destroyed annotations
      transcript.commit();

      for (Annotation code : transcript.all("code")) {
        
        if (pronounceFormat != null) { // some codes are pronounce events
          try { // does it match the pattern?
            Object[] label = pronounceFormat.parse(code.getLabel());
            if (label.length > 0) {
              // the code matches the pattern, does the code tag a word?
              Annotation[] word = code.tagsOn(transcript.getSchema().getWordLayerId());
              if (word.length > 0) {
                // create a pronounce tag
                transcript.createTag(word[0], "pronounce", (String)label[0]);
                continue;
              }
            }
          } catch(ParseException exception) {}
        } // pronounceFormat set
        
        if (lexicalFormat != null) { // some codes are lexical events
          try { // does it match the pattern?
            Object[] label = lexicalFormat.parse(code.getLabel());
            if (label.length > 0) {
              // the code matches the pattern, does the code tag a word?
              Annotation[] word = code.tagsOn(transcript.getSchema().getWordLayerId());
              if (word.length > 0) {
                // create a lexical tag
                transcript.createTag(word[0], "lexical", (String)label[0]);
                continue;
              }
            }
          } catch(ParseException exception) {}
        } // lexicalFormat set
        
        transcript.createTag(code, "comment", "["+code.getLabel()+"]");
      } // next code
      for (Annotation code : transcript.all("error")) {
        transcript.createTag(code, "comment", "["+code.getLabel()+"]");
      } // next code

      // add meta-data as comments at the beginning
      // so that can be parsed back out to slt if there's a round-trip
      String startId = transcript.first(schema.getTurnLayerId()).getStart().getId();
      // dob
      Annotation metadata = transcript.first("participant_dob");
      if (metadata != null) {
        transcript.addAnnotation(new Annotation().setLayerId("comment")
                                 .setLabel("+ Dob: " + metadata.getLabel())
                                 .setStartId(startId).setEndId(startId));
      }
      // ethnicity
      metadata = transcript.first("participant_ethnicity");
      if (metadata != null) {
        transcript.addAnnotation(new Annotation().setLayerId("comment")
                                 .setLabel("+ Ethnicity: " + metadata.getLabel())
                                 .setStartId(startId).setEndId(startId));
      }
      // participantId
      metadata = transcript.first("participant_id");
      if (metadata != null) {
        transcript.addAnnotation(new Annotation().setLayerId("comment")
                                 .setLabel("+ ParticipantId: " + metadata.getLabel())
                                 .setStartId(startId).setEndId(startId));
      }
      // ca
      metadata = transcript.first("transcript_ca");
      if (metadata != null) {
        transcript.addAnnotation(new Annotation().setLayerId("comment")
                                 .setLabel("+ Ca: " + metadata.getLabel())
                                 .setStartId(startId).setEndId(startId));
      }
      // collection
      metadata = transcript.first("transcript_collect");
      if (metadata != null) {
        transcript.addAnnotation(new Annotation().setLayerId("comment")
                                 .setLabel("+ Collect: " + metadata.getLabel())
                                 .setStartId(startId).setEndId(startId));
      }
      // location
      metadata = transcript.first("transcript_location");
      if (metadata != null) {
        transcript.addAnnotation(new Annotation().setLayerId("comment")
                                 .setLabel("+ Location: " + metadata.getLabel())
                                 .setStartId(startId).setEndId(startId));
      }

      
      // also ensure that the subgroup parent is set TODO find out why it's not
      Annotation subgroup = transcript.first("transcript_subgroup");
      if (subgroup != null) {
        subgroup.setParent(transcript);
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
    String[] layers = { "utterance", "word" };
    return layers;
  } // end of getLayersToSerialize()

  private static final long serialVersionUID = -1;
} // end of class SltToTrs
