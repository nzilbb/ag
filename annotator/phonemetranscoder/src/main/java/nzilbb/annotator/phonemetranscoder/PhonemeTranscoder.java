//
// Copyright 2021-2024 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.annotator.phonemetranscoder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Vector;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.encoding.*;
import nzilbb.util.IO;

/**
 * Annotator that translates word pronunciaions from one phoneme encoding system to another.
 */
public class PhonemeTranscoder extends Annotator {
  /** Get the minimum version of the nzilbb.ag API supported by the annotator.*/
  public String getMinimumApiVersion() { return "1.0.5"; }
  
  /**
   * ID of the input layer.
   * @see #getSourceLayerId()
   * @see #setSourceLayerId(String)
   */
  protected String sourceLayerId;
  /**
   * Getter for {@link #sourceLayerId}: ID of the input layer.
   * @return ID of the input layer.
   */
   public String getSourceLayerId() { return sourceLayerId; }
  /**
   * Setter for {@link #sourceLayerId}: ID of the input layer.
   * @param newSourceLayerId ID of the input layer.
   */
  public PhonemeTranscoder setSourceLayerId(String newSourceLayerId) { sourceLayerId = newSourceLayerId; return this; }

  /**
   * ID of the layer that determines the language of the whole transcript.
   * @see #getTranscriptLanguageLayerId()
   * @see #setTranscriptLanguageLayerId(String)
   */
  protected String transcriptLanguageLayerId;
  /**
   * Getter for {@link #transcriptLanguageLayerId}: ID of the layer that determines the
   * language of the whole transcript. 
   * @return ID of the layer that determines the language of the whole transcript.
   */
  public String getTranscriptLanguageLayerId() { return transcriptLanguageLayerId; }
  /**
   * Setter for {@link #transcriptLanguageLayerId}: ID of the layer that determines the
   * language of the whole transcript. 
   * @param newTranscriptLanguageLayerId ID of the layer that determines the language of
   * the whole transcript. 
   */
  public PhonemeTranscoder setTranscriptLanguageLayerId(String newTranscriptLanguageLayerId) {
    if (newTranscriptLanguageLayerId != null // empty string means null
        && newTranscriptLanguageLayerId.trim().length() == 0) {
      newTranscriptLanguageLayerId = null;
    }
    transcriptLanguageLayerId = newTranscriptLanguageLayerId;
    return this;
  }

  /**
   * ID of the layer that determines the language of individual phrases.
   * @see #getPhraseLanguageLayerId()
   * @see #setPhraseLanguageLayerId(String)
   */
  protected String phraseLanguageLayerId;
  /**
   * Getter for {@link #phraseLanguageLayerId}: ID of the layer that determines the
   * language of individual phrases. 
   * @return ID of the layer that determines the language of individual phrases.
   */
  public String getPhraseLanguageLayerId() { return phraseLanguageLayerId; }
  /**
   * Setter for {@link #phraseLanguageLayerId}: ID of the layer that determines the
   * language of individual phrases. 
   * @param newPhraseLanguageLayerId ID of the layer that determines the language of
   * individual phrases. 
   */
  public PhonemeTranscoder setPhraseLanguageLayerId(String newPhraseLanguageLayerId) {
    if (newPhraseLanguageLayerId != null // empty string means null
        && newPhraseLanguageLayerId.trim().length() == 0) {
      newPhraseLanguageLayerId = null;
    }
    phraseLanguageLayerId = newPhraseLanguageLayerId;
    return this;
  }
  
  /**
   * Regular expression identifying the language of tokens that should be updated.
   * @see #getLanguage()
   * @see #setLanguage(String)
   */
  protected String language;
  /**
   * Getter for {@link #language}: Regular expression identifying the language of
   * tokens that should be updated. 
   * @return Regular expression identifying the language of tokens that should be updated.
   */
  public String getLanguage() { return language; }
  /**
   * Setter for {@link #language}: Regular expression identifying the language of
   * tokens that should be updated. 
   * @param newLanguage Regular expression identifying the language of tokens that
   * should be updated. 
   */
  public PhonemeTranscoder setLanguage(String newLanguage) { language = newLanguage; return this; }
  
  /**
   * ID of the output layer.
   * @see #getDestinationLayerId()
   * @see #setDestinationLayerId(String)
   */
  protected String destinationLayerId;
  /**
   * Getter for {@link #destinationLayerId}: ID of the output layer.
   * @return ID of the output layer.
   */
  public String getDestinationLayerId() { return destinationLayerId; }
  /**
   * Setter for {@link #destinationLayerId}: ID of the output layer.
   * @param newDestinationLayerId ID of the output layer.
   */
  public PhonemeTranscoder setDestinationLayerId(String newDestinationLayerId) { destinationLayerId = newDestinationLayerId; return this; }
  
  /**
   * The translation to perform - the name of a PhonemeTranslator, or "custom" for a
   * custom mapping. 
   * @see #getTranslation()
   * @see #setTranslation(String)
   */
  protected String translation;
  /**
   * Getter for {@link #translation}: The translation to perform - the name of a
   * PhonemeTranslator, or "custom" for a custom mapping. 
   * @return The translation to perform - the name of a PhonemeTranslator, or "custom" for
   * a custom mapping. 
   */
  public String getTranslation() { return translation; }
  /**
   * Setter for {@link #translation}: The translation to perform - the name of a
   * PhonemeTranslator, or "custom" for a custom mapping. 
   * @param newTranslation The translation to perform - the name of a PhonemeTranslator,
   * or "custom" for a custom mapping. 
   */
  public PhonemeTranscoder setTranslation(String newTranslation) { translation = newTranslation; return this; }

  /**
   * PhonemeTranslator identified by {@link #translation}.
   * @see #getTranslator()
   * @see #setTranslator(PhonemeTranslator)
   */
  protected PhonemeTranslator translator;
  /**
   * Getter for {@link #translator}: PhonemeTranslator identified by {@link #translation}.
   * @return PhonemeTranslator identified by {@link #translation}.
   */
  public PhonemeTranslator getTranslator() { return translator; }
  /**
   * Setter for {@link #translator}: PhonemeTranslator identified by {@link #translation}.
   * @param newTranslator PhonemeTranslator identified by {@link #translation}.
   */
  public PhonemeTranscoder setTranslator(PhonemeTranslator newTranslator) { translator = newTranslator; return this; }

  /**
   * List of mappings from source to destination labels for custom translation.
   */
  protected LinkedHashMap<String,String> customTranslation = new LinkedHashMap<String,String>();
  
  /**
   * Whether to copy source characters that match no mapping, for custom translations.
   * @see #getCopyCharacters()
   * @see #setCopyCharacters(boolean)
   */
  protected boolean copyCharacters = true;
  /**
   * Getter for {@link #copyCharacters}: Whether to copy source characters that match no
   * mapping, for custom translations. 
   * @return Whether to copy source characters that match no mapping, for custom translations.
   */
  public boolean getCopyCharacters() { return copyCharacters; }
  /**
   * Setter for {@link #copyCharacters}: Whether to copy source characters that match no
   * mapping, for custom translations. 
   * @param newCopyCharacters Whether to copy source characters that match no mapping, for
   * custom translations. 
   */
  public PhonemeTranscoder setCopyCharacters(boolean newCopyCharacters) { copyCharacters = newCopyCharacters; return this; }
  
  /**
   * Sets the configuration for a given annotation task.
   * @param parameters The configuration of the annotator; a value of <tt> null </tt>
   * is invalid.
   * @throws InvalidConfigurationException
   */
  public void setTaskParameters(String parameters) throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");

    if (parameters == null) { // apply default configuration
      throw new InvalidConfigurationException(this, "Parameters not set.");         
    }
    
    // set basic attributes
    JsonObject json = beanPropertiesFromJSON(parameters);

    Layer sourceLayer = schema.getLayer(sourceLayerId);
    if (sourceLayer == null)
      throw new InvalidConfigurationException(this, "Source layer not found: " + sourceLayerId);
    if (transcriptLanguageLayerId != null && schema.getLayer(transcriptLanguageLayerId) == null) 
      throw new InvalidConfigurationException(
        this, "Transcript language layer not found: " + transcriptLanguageLayerId);
    if (phraseLanguageLayerId != null && schema.getLayer(phraseLanguageLayerId) == null) 
      throw new InvalidConfigurationException(
        this, "Phrase language layer not found: " + phraseLanguageLayerId);
    if (translation == null || translation.length() == 0) 
      throw new InvalidConfigurationException(this, "No translation was specified.");
    if (language != null && language.length() == 0) language = null;
    
    if ("custom".equals(translation)) {
      if (json.containsKey("custom")) {
        // set mappings
        JsonArray jsonMappings = json.getJsonArray("custom");
        customTranslation.clear();
        for (JsonValue element : jsonMappings) {
          if (element instanceof JsonObject) {
            JsonObject jsonMapping = (JsonObject)element;
            customTranslation.put(
              jsonMapping.getString("source"), jsonMapping.getString("destination"));
          } 
        } // next array element
        translator = new PhonemeTranslator() {
            public String apply(String label) {
              if (label == null) return null;
              StringBuilder destination = new StringBuilder();
              while (label.length() > 0) {
                boolean matchFound = false;
                // process mappings in order
                for (String source : customTranslation.keySet()) {
                  if (label.startsWith(source)) {
                    // the start of the string matches
                    matchFound = true;
                    
                    // nibble off the right amount from the source label
                    label = label.substring(source.length());
			
                    destination.append(customTranslation.get(source));
                    break;
                  } // match found
                } // next mapping
		  
                if (!matchFound) {
                  // copy as is?
                  if (copyCharacters) destination.append(label.substring(0,1));
                  // nibble off the first character
                  label = label.substring(1);
                }
              } // next chunk of the source label
              return destination.toString();
            }
          };
      }
    } else if ("DISC2CMU".equals(translation)) {
      translator = new DISC2CMU().setDefaultStress("2");
    } else if ("DISC2ARPAbet".equals(translation)) {
      translator = new DISC2ARPAbet();
    } else if ("CMU2DISC".equals(translation)) {
      translator = new CMU2DISC();
    } else if ("ARPAbet2DISC".equals(translation)) {
      translator = new ARPAbet2DISC();
    } else if ("DISC2Unisyn".equals(translation)) {
      translator = new DISC2Unisyn();
    } else if ("Unisyn2DISC".equals(translation)) {
      translator = new Unisyn2DISC();
    } else if ("DISC2Kirshenbaum".equals(translation)) {
      translator = new DISC2Kirshenbaum();
    } else if ("Kirshenbaum2DISC".equals(translation)) {
      translator = new Kirshenbaum2DISC();
    } else if ("DISC2SAMPA".equals(translation)) {
      translator = new DISC2SAMPA();
    } else if ("SAMPA2DISC".equals(translation)) {
      translator = new SAMPA2DISC();
    } else if ("DISC2XSAMPA".equals(translation)) {
      translator = new DISC2XSAMPA();
    } else if ("XSAMPA2DISC".equals(translation)) {
      translator = new XSAMPA2DISC();
    } else if ("DISC2IPA".equals(translation)) {
      translator = new DISC2IPA().setDelimiter(" ");
      // TODO delimiter
    } else {
      throw new InvalidConfigurationException(this, "Invalid translation: " + translation);
    }
    
    // does the outputLayer need to be added to the schema?
    Layer destinationLayer = schema.getLayer(destinationLayerId);
    if (destinationLayer == null) { // destination layer doesn't exist
      // create it
      destinationLayer = new Layer(destinationLayerId)
        .setAlignment(Constants.ALIGNMENT_NONE)
        .setPeers(sourceLayer.getPeers())
        .setParentId(schema.getWordLayerId());
      if ("DISC".equals(translator.getDestinationEncoding())
          || "IPA".equals(translator.getDestinationEncoding())) {
        destinationLayer.setType("ipa");
      }
      schema.addLayer(destinationLayer);
    } else {
      if (destinationLayerId.equals(sourceLayerId)
          || destinationLayerId.equals(transcriptLanguageLayerId)
          || destinationLayerId.equals(phraseLanguageLayerId)) {
        throw new InvalidConfigurationException(
          this, "Invalid destination layer: " + destinationLayerId);
      }
      if (("DISC".equals(translator.getDestinationEncoding())
           || "IPA".equals(translator.getDestinationEncoding()))
          && !destinationLayer.getType().equals("ipa")) {
        destinationLayer.setType("ipa");
      }
    }
  }

  /**
   * Determines which layers the annotator requires in order to annotate a graph.
   * @return A list of layer IDs. In this case, the annotator only requires the schema's
   * word layer.
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public String[] getRequiredLayers() throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");
    if (sourceLayerId == null)
      throw new InvalidConfigurationException(this, "No source layer set.");
    Vector<String> requiredLayers = new Vector<String>();
    requiredLayers.add(sourceLayerId);
    if (transcriptLanguageLayerId != null) requiredLayers.add(transcriptLanguageLayerId);
    if (phraseLanguageLayerId != null) requiredLayers.add(phraseLanguageLayerId);
    return requiredLayers.toArray(new String[0]);
  }

  /**
   * Determines which layers the annotator will create/update/delete annotations on.
   * @return A list of layer IDs. In this case, the annotator has no task web-app for
   * specifying an output layer, and doesn't update any layers, so this method returns an
   * empty array.
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public String[] getOutputLayers() throws InvalidConfigurationException {
    if (destinationLayerId == null)
      throw new InvalidConfigurationException(this, "Destination layer not set.");
    return new String[] { destinationLayerId };
  }
   
  /**
   * Transforms the graph. In this case, the graph is simply summarized, by counting all
   * tokens of each word type, and printing out the result to stdout.
   * @param graph The graph to transform.
   * @return The changes introduced by the tranformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph transform(Graph graph) throws TransformationException {
    setRunning(true);
    try {
      setStatus(""); // clear any residual status from the last run...
         
      Layer sourceLayer = graph.getSchema().getLayer(sourceLayerId);
      if (sourceLayer == null) {
        throw new InvalidConfigurationException(this, "Invalid source layer: " + sourceLayerId);
      }
      Layer destinationLayer = graph.getSchema().getLayer(destinationLayerId);
      if (destinationLayer == null) {
        throw new InvalidConfigurationException(this, "Invalid destination layer: " + destinationLayer);
      }
      if (translator == null) {
        throw new InvalidConfigurationException(this, "No translator specified.");
      }
         
      // what languages are in the transcript?
      boolean transcriptIsMainlyLanguage = true;
      if (transcriptLanguageLayerId != null && language != null) {
        Annotation transcriptLanguage = graph.first(transcriptLanguageLayerId);
        if (transcriptLanguage != null) {
          if (!transcriptLanguage.getLabel().matches(language)) { // not destination language
            transcriptIsMainlyLanguage = false;
          }
        }
      }
      boolean thereArePhraseTags = false;
      if (phraseLanguageLayerId != null && language != null) {
        if (graph.first(phraseLanguageLayerId) != null) {
          thereArePhraseTags = true;
        }
      }

      Vector<Annotation> toAnnotate = new Vector<Annotation>();
      // should we just tag everything?
      if (transcriptIsMainlyLanguage && !thereArePhraseTags) {
        // process all tokens
        for (Annotation token : graph.all(sourceLayerId)) {
          // tag only tokens that are not already tagged
          if (token.first(destinationLayerId) == null) { // not tagged yet
            toAnnotate.add(token);                        
          } // not tagged yet
        } // next token
      } else if (transcriptIsMainlyLanguage) {
        // process all but the phrase-tagged tokens
            
        // tag the exceptions
        for (Annotation phrase : graph.all(phraseLanguageLayerId)) {
          if (!phrase.getLabel().matches(language)) { // not destination language
            for (Annotation token : phrase.all(sourceLayerId)) {
              // mark the token as an exception
              token.put("@notLanguage", Boolean.TRUE);
            } // next token in the phrase
          } // non-language phrase
        } // next phrase
        
        for (Annotation token : graph.all(sourceLayerId)) {
          if (token.containsKey("@notLanguage")) {
            // while we're here, we remove the @notLanguage mark
            token.remove("@notLanguage");
          } else { // The correct language, so tag it
            // tag only tokens that are not already tagged
            if (token.first(destinationLayerId) == null) { // not tagged yet
              toAnnotate.add(token);
            } // not tagged yet
          } // tag it
        } // next token
      } else if (thereArePhraseTags) {
        // process only the tokens phrase-tagged as the target language
        for (Annotation phrase : graph.all(phraseLanguageLayerId)) {
          if (phrase.getLabel().matches(language)) {
            for (Annotation token : phrase.all(sourceLayerId)) {
              // tag only tokens that are not already tagged
              if (token.first(destinationLayerId) == null) { // not tagged yet
                toAnnotate.add(token);                  
              } // not tagged yet
            } // next token in the phrase
          } // matching language phrase
        } // next phrase
      } // thereArePhraseTags

      setStatus(sourceLayerId + " ("+translator.getSourceEncoding()+") → "
                + destinationLayerId + " ("+translator.getDestinationEncoding()+")");
      for (Annotation token : toAnnotate) {
        token.createTag(destinationLayerId, translator.apply(token.getLabel()))
          .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
      } // next token
      return graph;
    } finally {
      setRunning(false);
    }
  } // transform()  
}
