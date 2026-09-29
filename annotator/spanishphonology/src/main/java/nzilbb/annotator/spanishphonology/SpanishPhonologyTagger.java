//
// Copyright 2020-2026 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.annotator.spanishphonology;

import java.util.Collection;
import java.util.Vector;
import nz.ac.canterbury.ling.spanishphonology.SpanishPronunciation;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.DictionaryException;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.LabelBasedTagger;

/**
 * Annotator that tags Spanish words with their phonemic transcription, based orthography.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class SpanishPhonologyTagger extends LabelBasedTagger {
   /** Get the minimum version of the nzilbb.ag API supported by the serializer.*/
   public String getMinimumApiVersion() { return "2.0.0"; }

   private SpanishPronunciation transcriber = new SpanishPronunciation();

   /**
    * Which variety of Spanish to use.
    * @see #getLocale()
    * @see #setLocale(String)
    */
  protected String locale;
  /**
   * Getter for {@link #locale}: Which variety of Spanish to use.
   * @return Which variety of Spanish to use.
   */
  public String getLocale() { return locale; }
  /**
   * Setter for {@link #locale}: Which variety of Spanish to use.
   * @param newLocale Which variety of Spanish to use.
   */
  public SpanishPhonologyTagger setLocale(String newLocale) { locale = newLocale; return this; }

  /**
   * ID of the layer that the annotator outputs its annotations to. 
   * @return ID of the layer that the annotator outputs its annotations to.
   */
  public String getPhonemeLayerId() { return tagLayerId; }
  /**
   * ID of the layer that the annotator outputs its annotations to.
   * @param newPhonemeLayerId ID of the layer that the annotator outputs its annotations to.
   */
  public SpanishPhonologyTagger setPhonemeLayerId(String newPhonemeLayerId) { tagLayerId = newPhonemeLayerId; return this; }

  /**
   * Sets the configuration for a given annotation task.
   * @param parameters The configuration of the annotator; a value of <tt> null </tt>
   * will apply the default task parameters, with {@link #tokenLayerId} set to the
   * {@link Schema#wordLayerId} and {@link #phonemeLayerId} set to <q>phonemes</q>.
   * @throws InvalidConfigurationException
   */
  public void setTaskParameters(String parameters) throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");
      
    // target Spanish
    targetLanguagePattern = "[Ee][Ss].*";
      
    if (parameters == null) { // apply default configuration
         
      if (schema.getLayer("orthography") != null) {
        tokenLayerId = "orthography";
      } else {
        tokenLayerId = schema.getWordLayerId();
      }
         
      Layer[] candidates = schema.getMatchingLayers(
        layer -> schema.getRoot().getId().equals(layer.getParentId())
        && layer.getAlignment() == 0 // transcript attribute
        && layer.getId().matches(".*lang.*")); // with 'lang' in the name
      if (candidates.length > 0) transcriptLanguageLayerId = candidates[0].getId();
         
      // default phrase language layer
      candidates = schema.getMatchingLayers(
        layer -> schema.getTurnLayerId() != null
        && schema.getTurnLayerId().equals(layer.getParentId()) // child of turn
        && layer.getId().matches(".*lang.*")); // with 'lang' in the name
      if (candidates.length > 0) phraseLanguageLayerId = candidates[0].getId();
         
      // default output layer
      candidates = schema.getMatchingLayers(
        layer -> schema.getWordLayerId() != null
        && schema.getWordLayerId().equals(layer.getParentId())
        && (layer.getId().matches(".*phoneme.*")
            || layer.getId().matches(".*pronunciation.*")));
         
      tagLayerId = "phonemes";
         
    } else {
      beanPropertiesFromQueryString(parameters);
    }

    if (locale == null) locale = "es_ES";
    if (!transcriber.getSupportedLocales().contains(locale)) {
      throw new InvalidConfigurationException(this, "Invalid locale: " + locale);
    }
      
    // does the outputLayer need to be added to the schema?
    Layer layer = schema.getLayer(tagLayerId);
    if (layer == null) {
      schema.addLayer(
        new Layer(tagLayerId)
        .setAlignment(Constants.ALIGNMENT_NONE)
        .setPeers(false).setSaturated(true)
        .setParentId(schema.getWordLayerId())
        .setType(Constants.TYPE_IPA));
    } else {
      if (layer.getAlignment() != Constants.ALIGNMENT_NONE) {
        layer.setAlignment(Constants.ALIGNMENT_NONE);
      }
      if (layer.getPeers()) layer.setPeers(false);
      if (layer.getPeersOverlap()) layer.setPeersOverlap(false);
      if (!layer.getSaturated()) layer.setSaturated(true);
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
    if (tokenLayerId == null)
      throw new InvalidConfigurationException(this, "No input token layer set.");
    Vector<String> requiredLayers = new Vector<String>();
    requiredLayers.add(tokenLayerId);
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
    if (tagLayerId == null)
      throw new InvalidConfigurationException(this, "Phoneme layer not set.");
    return new String[] { tagLayerId };
  }
   
  /**
   * Determines what tag labels should apply on the tag layer for
   * tokens with the given label on the token layer.
   * This is the method that must be implemented by derived classes.
   * @param tokenLabel The label of the token(s) that must be tagged.
   * @return A list of tags, which may be empty.
   */
  public Collection<String> tagsFor(String tokenLabel) throws DictionaryException {
    Vector<String> tags = new Vector<String>();
    tags.add(transcriber.convert_spanish_word_to_phonetic_transcription(
               tokenLabel, locale));
    return tags;
  } // end of transcribe()

} // end of class SpanishPhonologyTagger
