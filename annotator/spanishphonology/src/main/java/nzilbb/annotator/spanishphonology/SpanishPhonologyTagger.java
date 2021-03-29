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
package nzilbb.annotator.spanishphonology;

import java.util.Vector;
import javax.script.ScriptException;
import nz.ac.canterbury.ling.spanishphonology.SpanishPronunciation;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;

/**
 * Annotator that tags Spanish words with their phonemic transcription, based orthography.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class SpanishPhonologyTagger extends Annotator {
   /** Get the minimum version of the nzilbb.ag API supported by the serializer.*/
   public String getMinimumApiVersion() { return "1.0.0"; }

   private SpanishPronunciation transcriber = new SpanishPronunciation();

   /**
    * ID of the input layer containing word tokens.
    * @see #getTokenLayerId()
    * @see #setTokenLayerId(String)
    */
   protected String tokenLayerId;
   /**
    * Getter for {@link #tokenLayerId}: ID of the input layer containing word tokens.
    * @return ID of the input layer containing word tokens.
    */
   public String getTokenLayerId() { return tokenLayerId; }
   /**
    * Setter for {@link #tokenLayerId}: ID of the input layer containing word tokens.
    * @param newTokenLayerId ID of the input layer containing word tokens.
    */
   public SpanishPhonologyTagger setTokenLayerId(String newTokenLayerId) {
      tokenLayerId = newTokenLayerId; return this; }

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
   public SpanishPhonologyTagger setTranscriptLanguageLayerId(String newTranscriptLanguageLayerId) {
      if (newTranscriptLanguageLayerId != null // empty string means null
          && newTranscriptLanguageLayerId.trim().length() == 0) {
         newTranscriptLanguageLayerId = null;
      }
      transcriptLanguageLayerId = newTranscriptLanguageLayerId;
      return this;
   }

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
   public SpanishPhonologyTagger setPhraseLanguageLayerId(String newPhraseLanguageLayerId) {
      if (newPhraseLanguageLayerId != null // empty string means null
          && newPhraseLanguageLayerId.trim().length() == 0) {
         newPhraseLanguageLayerId = null;
      }
      phraseLanguageLayerId = newPhraseLanguageLayerId;
      return this;
   }

   /**
    * ID of the layer that the annotator outputs its annotations to.
    * @see #getPhonemeLayerId()
    * @see #setPhonemeLayerId(String)
    */
   protected String phonemeLayerId;
   /**
    * Getter for {@link #phonemeLayerId}: ID of the layer that the annotator outputs its
    * annotations to. 
    * @return ID of the layer that the annotator outputs its annotations to.
    */
   public String getPhonemeLayerId() { return phonemeLayerId; }
   /**
    * Setter for {@link #phonemeLayerId}: ID of the layer that the annotator outputs its 
    * annotations to.
    * @param newPhonemeLayerId ID of the layer that the annotator outputs its annotations to.
    */
   public SpanishPhonologyTagger setPhonemeLayerId(String newPhonemeLayerId) { phonemeLayerId = newPhonemeLayerId; return this; }

   /**
    * Sets the configuration for a given annotation task.
    * @param parameters The configuration of the annotator; a value of <tt> null </tt>
    * will apply the default task parameters, with {@link #tokenLayerId} set to the
    * {@link Schema#wordLayerId} and {@link #stemLayerId} set to <q>stem</q>.
    * @throws InvalidConfigurationException
    */
   public void setTaskParameters(String parameters) throws InvalidConfigurationException {
      if (schema == null)
         throw new InvalidConfigurationException(this, "Schema is not set.");
      
      if (parameters == null) { // apply default configuration
         
         if (schema.getLayer("orthography") != null) {
            tokenLayerId = "orthography";
         } else {
            tokenLayerId = schema.getWordLayerId();
         }
         
         try {
            Layer[] candidates = schema.getMatchingLayers(
               "layer.parentId == schema.root.id && layer.alignment == 0" // transcript attribute
               +" && /.*lang.*/.test(layer.id)"); // with 'lang' in the name
            if (candidates.length > 0) transcriptLanguageLayerId = candidates[0].getId();
            
            candidates = schema.getMatchingLayers(
               "layer.parentId == schema.turnLayerId" // child of turn
               +" && /.*lang.*/.test(layer.id)"); // with 'lang' in the name
            if (candidates.length > 0) phraseLanguageLayerId = candidates[0].getId();
         } catch(ScriptException impossible) {}

         phonemeLayerId = "phonemes";
         
      } else {
         beanPropertiesFromQueryString(parameters);
      }

      if (locale == null) locale = "es_ES";
      if (!transcriber.getSupportedLocales().contains(locale)) {
         throw new InvalidConfigurationException(this, "Invalid locale: " + locale);
      }
      
      // does the outputLayer need to be added to the schema?
      if (schema.getLayer(phonemeLayerId) == null) {
         schema.addLayer(
            new Layer(phonemeLayerId)
            .setAlignment(Constants.ALIGNMENT_NONE)
            .setPeers(false)
            .setParentId(schema.getWordLayerId())
            .setType(Constants.TYPE_IPA));
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
      if (phonemeLayerId == null)
         throw new InvalidConfigurationException(this, "Phoneme layer not set.");
      return new String[] { phonemeLayerId };
   }
   
   /**
    * Transforms the graph. In this case, the graph is simply summarized, by counting all
    * tokens of each word type, and printing out the result to stdout.
    * @param graph The graph to transform.
    * @return The changes introduced by the tranformation.
    * @throws TransformationException If the transformation cannot be completed.
    */
   public Graph transform(Graph graph) throws TransformationException {
      
      Layer tokenLayer = graph.getSchema().getLayer(tokenLayerId);
      if (tokenLayer == null) {
         throw new InvalidConfigurationException(
            this, "Invalid input token layer: " + tokenLayerId);
      }
      Layer phonemeLayer = graph.getSchema().getLayer(phonemeLayerId);
      if (phonemeLayer == null) {
         throw new InvalidConfigurationException(
            this, "Invalid output phoneme layer: " + phonemeLayerId);
      }

      // what languages are in the transcript?
      boolean transcriptIsMainlySpanish = true;
      if (transcriptLanguageLayerId != null) {
         Annotation transcriptLanguage = graph.first(transcriptLanguageLayerId);
         if (transcriptLanguage != null) {
            if (!transcriptLanguage.getLabel().startsWith("es")) { // not Spanish
               transcriptIsMainlySpanish = false;
            }
         }
      }
      boolean thereArePhraseTags = false;
      if (phraseLanguageLayerId != null) {
         if (graph.first(phraseLanguageLayerId) != null) {
            thereArePhraseTags = true;
         }
      }
      
      // should we just tag everything?
      if (transcriptIsMainlySpanish && !thereArePhraseTags) {
         // process all tokens
         for (Annotation token : graph.all(tokenLayerId)) {
            // tag only tokens that are not already tagged
            if (token.first(phonemeLayerId) == null) { // not tagged yet
               token.createTag(phonemeLayerId, transcribe(token.getLabel()))
                  .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
            } // not tagged yet
         } // next token
      } else if (transcriptIsMainlySpanish) {
         // process all but the phrase-tagged tokens

         // tag the exceptions
         for (Annotation phrase : graph.all(phraseLanguageLayerId)) {
            if (!phrase.getLabel().startsWith("es")) { // not Spanish
               for (Annotation token : phrase.all(tokenLayerId)) {
                  // mark the token as an exception
                  token.put("@notSpanish", Boolean.TRUE);
               } // next token in the phrase
            } // non-Spanish phrase
         } // next phrase
         
         for (Annotation token : graph.all(tokenLayerId)) {
            if (token.containsKey("@notSpanish")) {
               // while we're here, we remove the @notSpanish mark
               token.remove("@notSpanish");
            } else { // Spanish, so tag it
               // tag only tokens that are not already tagged
               if (token.first(phonemeLayerId) == null) { // not tagged yet
                  token.createTag(phonemeLayerId, transcribe(token.getLabel()))
                     .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
               } // not tagged yet
            } // Spanish, so tag it
         } // next token
      } else if (thereArePhraseTags) {
         // process only the tokens phrase-tagged as Spanish
         for (Annotation phrase : graph.all(phraseLanguageLayerId)) {
            if (phrase.getLabel().startsWith("es")) {
               for (Annotation token : phrase.all(tokenLayerId)) {
                  // tag only tokens that are not already tagged
                  if (token.first(phonemeLayerId) == null) { // not tagged yet
                     token.createTag(phonemeLayerId, transcribe(token.getLabel()))
                        .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
                  } // not tagged yet
               } // next token in the phrase
            } // Spanish phrase
         } // next phrase
      } // thereArePhraseTags
      
      return graph;
   }

   /**
    * Phonetically transcribes the given word.
    * @param word The word to transcribe.
    * @return The phonemic transcription of the given word.
    */
   public String transcribe(String word) {
      return transcriber.convert_spanish_word_to_phonetic_transcription(word, locale);
   } // end of transcribe()

} // end of class SpanishPhonologyTagger
