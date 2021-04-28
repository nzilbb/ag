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
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.serialize.GraphDeserializer;
import nzilbb.ag.serialize.GraphSerializer;
import nzilbb.formatter.kaldi.KaldiSerializer;
import nzilbb.formatter.transcriber.TranscriptSerialization;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;

/**
 * Converts Transcriber .trs files to corpus input files for Kaldi.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Converts Transcriber .trs files to corpus input files for Kaldi",arguments="file1.trs file2.trs ...")
public class TrsToKaldi extends Converter {
  
  /**
   * Whether to clean orthographies (downcase and remove extraneous punctuation) or not.
   * @see #getCleanOrthography()
   * @see #setCleanOrthography(Boolean)
   */
  protected Boolean cleanOrthography = Boolean.TRUE;
  /**
   * Getter for {@link #cleanOrthography}: Whether to clean orthographies (downcase and
   * remove extraneous punctuation) or not. 
   * @return Whether to clean orthographies (downcase and remove extraneous punctuation) or not.
   */
  public Boolean getCleanOrthography() { return cleanOrthography; }
  /**
   * Setter for {@link #cleanOrthography}: Whether to clean orthographies (downcase and
   * remove extraneous punctuation) or not. 
   * @param newCleanOrthography Whether to clean orthographies (downcase and remove
   * extraneous punctuation) or not. 
   */
  @Switch("Clean orthography by converting to lower case and removin extraneous punctuation")
  public TrsToKaldi setCleanOrthography(Boolean newCleanOrthography) { cleanOrthography = newCleanOrthography; return this; }
  
  /**
   * Regular expression for identifying characters to remove. 
   * Default value is <q>[\\p{Punct}&amp;&amp;[^~\\-:']]</q>
   * - all punctuation except <q>~</q>, <q>-</q>, <q>'</q>, and <q>:</q>
   * @see #getRemovalPattern()
   * @see #setRemovalPattern(String)
   */
  protected String removalPattern = "[\\p{Punct}&&[^~\\-:']]";
  /**
   * Getter for {@link #removalPattern}: Regular expression for identifying characters to
   * remove. Default value is <q>[\\p{Punct}&amp;&amp;[^~\\-:']]</q>
   * - all punctuation except <q>~</q>, <q>-</q>, <q>'</q>, and <q>:</q>
   * @return Regular expression for identifying characters to remove.
   */
  public String getRemovalPattern() { return removalPattern; }
  /**
   * Setter for {@link #removalPattern}: Regular expression for identifying characters to remove.
   * @param newRemovalPattern Regular expression for identifying characters to remove.
   */
  @Switch("Regular expression for identifying characters to remove; default is [\\p{Punct}&&[^~\\-:']]")
  public TrsToKaldi setRemovalPattern(String newRemovalPattern) { removalPattern = newRemovalPattern; return this; }
   
  /**
   * Default constructor.
   */
  public TrsToKaldi() {
    info = "The participant genders from the Transcriber transcripts are used, if present,"
      +" to generate the spk2gender file."
      +"\n "
      +"The following participant meta-data is lost during conversion:"
      +"\n- dialect"
      +"\n- accent"
      +"\n- scope"
      +"\n- version"
      +"\n- version date"
      +"\n- air date"
      +"\n- scribe"
      +"\n- language"
      +"\n "
      +"\nThe following Transcriber annotations are lost during conversion:"
      +"\n- phrase language annotations"
      +"\n- named entity annotations"
      +"\n- comments"
      +"\n- noises"
      +"\n- lexical tags"
      +"\n- pronounce tags"
      +"\n "
      +"\nBy default, all words are converted to lowercase, and extraneous punctuation is removed."
      +"\n To disable this behaviour, use the --cleanOrthography=false command line switch.";
  } // end of constructor
   
  public static void main(String argv[]) {
    new TrsToKaldi().mainRun(argv);
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
    return new KaldiSerializer();
  }   
   
  /**
   * Specify the schema to used by  {@link #convert(File)}.
   * @return The schema.
   */
  @Override
  public Schema getSchema() {
    Schema schema = super.getSchema();
    schema.addLayer(
      new Layer("participant_gender", "Gender")         
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getParticipantLayerId()).setParentIncludes(true));
    if (cleanOrthography) {
      schema.addLayer(
        new Layer("orthography", "Cleaned orthography")         
        .setAlignment(Constants.ALIGNMENT_NONE)
        .setPeers(false).setPeersOverlap(false).setSaturated(true)
        .setParentId(schema.getWordLayerId()).setParentIncludes(true));
    }
    return schema;
  } // end of getSchema()
   
  /**
   * Clean up orthographies.
   * @param transcripts
   */
  @Override
  public void processTranscripts(Graph[] transcripts) {
    if (cleanOrthography) {
      Schema schema = getSchema();
      for (Graph transcript : transcripts) {
        transcript.addLayer((Layer)schema.getLayer("orthography").clone());
        for (Annotation word : transcript.all(schema.getWordLayerId())) {
          String orthography = orthography(word.getLabel(), removalPattern);
          // only add an annotation if there's actually a label
          if (orthography.length() > 0) {
            word.createTag("orthography", orthography)
              .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
          }
        } // next word token
      } // next transcript
    } // cleanOrthography
  } // end of processTranscripts()
  
  /**
   * Specifies which layers should be given to the serializer. 
   * @return An array of layer IDs.
   */
  @Override
  public String[] getLayersToSerialize() {
    String[] layers = { "utterance", "participant_gender" };
    return layers;
  } // end of getLayersToSerialize()


  /**
   * Returns the orthography of the given string, using the given removal pattern.
   * @param word
   * @param removalPattern
   * @return The orthography of the given string.
   */
  public String orthography(String word, String removalPattern) {
    String orth =  word
      .toLowerCase()
      // collapse all space (there could be space because of appended non-words)
      .replaceAll("\\s","")
      // might be spaces left after stripping
      .trim()
      // TODO would be nice to have a configurable lise of conversions like ’->'
      // 'smart' apostrophes to normal ones
      .replace('’','\'')
      // 'smart' quotes to normal ones
      .replace('“','"').replace('”','"')
      // 'em-dash' to hyphen
      .replace('—','-')
      // remove leading hyphens/apostrophes
      .replaceAll("^[\\-']*", "");
    if (removalPattern.length() > 0) {
      // remove characters identified by removalPattern
      orth = orth.replaceAll(removalPattern,"");
    }
    orth = orth
      // might be spaces left after stripping
      .trim()
      // remove trailing hyphens/apostrophes
      .replaceAll("[\\-']*$", "")
      // remove leading hyphens/apostrophes
      .replaceAll("^[\\-']*", "")
      // might be spaces left after stripping
      .trim();
    return orth;
  } // end of orthography()

  private static final long serialVersionUID = -1;
} // end of class TrsToKaldi
