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
package nzilbb.annotator.cmudict;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import javax.script.ScriptException;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.MySQLTranslator;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.UsesRelationalDatabase;

/**
 * Annotator that tags words with their pronunciations according to the 
 * <a href="http://www.speech.cs.cmu.edu/cgi-bin/cmudict"> CMU Pronouncing Dictionary </a>.
 */
public class CMUDict extends Annotator
   implements UsesRelationalDatabase, UsesFileSystem {
   /** Get the minimum version of the nzilbb.ag API supported by the serializer.*/
   public String getMinimumApiVersion() { return "20200708.2018"; }

   /**
    * The working directory for the annotator.
    * @see #getWorkingDirectory()
    * @see #setWorkingDirectory(File)
    */
   protected File workingDirectory;
   /**
    * Getter for {@link #workingDirectory}: The working directory for the annotator.
    * @return The working directory for the annotator.
    */
   public File getWorkingDirectory() { return workingDirectory; }
   /**
    * {@link UsesFileSystem} method that provides a persisent directory in which files can
    * be saved and accessed. 
    * @param directory
    */
   public void setWorkingDirectory(File directory) {
      workingDirectory = directory;
   }

   private MySQLTranslator sqlx;
   private String rdbUrl;
   private String rdbUser;
   private String rdbPassword;
   /**
    * {@link UsesRelationalDatabase} method that sets the information required for
    * connecting to the relational database. 
    * @param sqlTranslator SQL statement translator.
    * @param url URL for relational database, e.g. <q>jdbc:mysql://localhost/labbcat</q>
    * @param user Username for connecting to the database, if any.
    * @param password Password for connecting to the database, if any.
    * @throws SQLException If the annotator can't connect to the given database.
    */
   public void rdbConnectionDetails(
      MySQLTranslator sqlTranslator, String url, String user, String password)
      throws SQLException {

      sqlx = sqlTranslator;
      rdbUrl = url;
      rdbUser = user;
      rdbPassword = password;

      // check we can connect
      Connection rdb = newConnection();

      try {
         
         // check the schema has been created
         try { // either of prepareStatement or executeQuery may fail if the table doesn't exist
            PreparedStatement sql = rdb.prepareStatement(
               sqlx.apply("SELECT COUNT(*) AS theCount FROM "+getAnnotatorId()+"_wordform"));
            try {
               ResultSet rsCheck = sql.executeQuery();
               rsCheck.close();
            } finally {
               sql.close();
            }
         } catch(SQLException exception) {
            sql.close();
            
            sql = rdb.prepareStatement(
               sqlx.apply(
                  "CREATE TABLE "+getAnnotatorId()+"_wordform ("
                  +" wordform varchar(100)"
                  +" CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL"
                  +" COMMENT 'Orthographic spelling of the word, in lowercase letters',"
                  +" variant smallint NOT NULL"
                  +" COMMENT 'Pronunciation number, zero-based',"
                  +" pron_cmudict varchar(200)"
                  +" CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL"
                  +" COMMENT 'Original pronunciation in ARPAbet, from the cmudict file',"
                  +" pron_disc varchar(100)"
                  +" CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL"
                  +" COMMENT 'Pronunciation in the CELEX DISC character set',"
                  +" supplemental bit NOT NULL DEFAULT 0"
                  +" COMMENT 'Whether the word/variant has been manually added since uploading from the cmudict file',"		  
                  +" PRIMARY KEY (wordform,variant)"
                  +") ENGINE=MyISAM;"));
	    sql.executeUpdate();
	    sql.close();
         }
      } finally {
         try { rdb.close(); } catch(SQLException x) {}
      }      
   }
   
   /**
    * Get a new connection to the database.
    * @return A connection to the RDBMS.
    * @throws SQLException If a database access error occurs
    */
   public Connection newConnection() throws SQLException {
      return DriverManager.getConnection(rdbUrl, rdbUser, rdbPassword);
   } // end of newConnection()
   
   /**
    * Provides the overall configuration of the annotator. 
    * @return The overall configuration of the annotator, which will be passed to the
    * <i> config/index.html </i> configuration web-app, if any. This configuration may be
    * null, or a string that serializes the annotators configuration state in any encoding
    * the implementor prefers. The resulting string must be interpretable by the
    * <i> config/index.html </i> web-app. 
    * @see #setConfig(String)
    * @see #beanPropertiesToQueryString()
    */
   //TODO public String getConfig() { return null; }
   
   /**
    * Installs or updates the database schema and the contents of the <q>cmudict.txt file</q>.
    * @throws InvalidConfigurationException
    * @see #getConfig()
    * @see #beanPropertiesFromQueryString(String)
    */ 
   public void setConfig(String config) throws InvalidConfigurationException {
      // TODO update dictionary if a file was provided
      setPercentComplete(100);
   }

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
   public CMUDict setTokenLayerId(String newTokenLayerId) {
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
   public CMUDict setTranscriptLanguageLayerId(String newTranscriptLanguageLayerId) {
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
   public CMUDict setPhraseLanguageLayerId(String newPhraseLanguageLayerId) {
      if (newPhraseLanguageLayerId != null // empty string means null
          && newPhraseLanguageLayerId.trim().length() == 0) {
         newPhraseLanguageLayerId = null;
      }
      phraseLanguageLayerId = newPhraseLanguageLayerId;
      return this;
   }

   /**
    * ID of the output layer.
    * @see #getPronunciationLayerId()
    * @see #setPronunciationLayerId(String)
    */
   protected String pronunciationLayerId;
   /**
    * Getter for {@link #pronunciationLayerId}: ID of the output layer.
    * @return ID of the output layer.
    */
   public String getPronunciationLayerId() { return pronunciationLayerId; }
   /**
    * Setter for {@link #pronunciationLayerId}: ID of the output layer.
    * @param newPronunciationLayerId ID of the output layer.
    */
   public CMUDict setPronunciationLayerId(String newPronunciationLayerId) {
      pronunciationLayerId = newPronunciationLayerId; return this; }

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
         
         tokenLayerId = schema.getWordLayerId();
         
         try {
            // default transcript language layer
            Layer[] candidates = schema.getMatchingLayers(
               "layer.parentId == schema.root.id && layer.alignment == 0" // transcript attribute
               +" && /.*lang.*/.test(layer.id)"); // with 'lang' in the name
            if (candidates.length > 0) transcriptLanguageLayerId = candidates[0].getId();
            
            // default phrase language layer
            candidates = schema.getMatchingLayers(
               "layer.parentId == schema.turnLayerId" // child of turn
               +" && /.*lang.*/.test(layer.id)"); // with 'lang' in the name
            if (candidates.length > 0) phraseLanguageLayerId = candidates[0].getId();

            // default output layer
            candidates = schema.getMatchingLayers(
               "layer.parentId == schema.wordLayerId && layer.alignment == 0" // word tag
               +" && (/.*phoneme.*/.test(layer.id) || /.*pronunciation.*/.test(layer.id))");
            if (candidates.length > 0) {
               pronunciationLayerId = candidates[0].getId();
            } else { // suggest adding a new one
               pronunciationLayerId = "phonemes";
            }
         } catch(ScriptException impossible) {}
         
      } else {
         beanPropertiesFromQueryString(parameters);
      }
      
      // does the outputLayer need to be added to the schema?
      if (schema.getLayer(pronunciationLayerId) == null) {
         schema.addLayer(
            new Layer(pronunciationLayerId)
            .setAlignment(Constants.ALIGNMENT_NONE)
            .setPeers(false)
            .setParentId(schema.getWordLayerId()));
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
      if (pronunciationLayerId == null)
         throw new InvalidConfigurationException(this, "Stem layer not set.");
      return new String[] { pronunciationLayerId };
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
      Layer stemLayer = graph.getSchema().getLayer(pronunciationLayerId);
      if (stemLayer == null) {
         throw new InvalidConfigurationException(
            this, "Invalid output stem layer: " + pronunciationLayerId);
      }

      // what languages are in the transcript?
      boolean transcriptIsMainlyEnglish = true;
      if (transcriptLanguageLayerId != null) {
         Annotation transcriptLanguage = graph.first(transcriptLanguageLayerId);
         if (transcriptLanguage != null) {
            if (!transcriptLanguage.getLabel().toLowerCase().startsWith("en")) { // not English
               transcriptIsMainlyEnglish = false;
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
      if (transcriptIsMainlyEnglish && !thereArePhraseTags) {
         // process all tokens
         for (Annotation token : graph.all(tokenLayerId)) {
            // tag only tokens that are not already tagged
            if (token.first(pronunciationLayerId) == null) { // not tagged yet
//TODO               token.createTag(pronunciationLayerId, stem(token.getLabel()))
//                  .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
            } // not tagged yet
         } // next token
      } else if (transcriptIsMainlyEnglish) {
         // process all but the phrase-tagged tokens

         // tag the exceptions
         for (Annotation phrase : graph.all(phraseLanguageLayerId)) {
            if (!phrase.getLabel().toLowerCase().startsWith("en")) { // not English
               for (Annotation token : phrase.all(tokenLayerId)) {
                  // mark the token as an exception
                  token.put("@notEnglish", Boolean.TRUE);
               } // next token in the phrase
            } // non-English phrase
         } // next phrase
         
         for (Annotation token : graph.all(tokenLayerId)) {
            if (token.containsKey("@notEnglish")) {
               // while we're here, we remove the @notEnglish mark
               token.remove("@notEnglish");
            } else { // English, so tag it
               // tag only tokens that are not already tagged
               if (token.first(pronunciationLayerId) == null) { // not tagged yet
//TODO                  token.createTag(pronunciationLayerId, stem(token.getLabel()))
//                     .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
               } // not tagged yet
            } // English, so tag it
         } // next token
      } else if (thereArePhraseTags) {
         // process only the tokens phrase-tagged as English
         for (Annotation phrase : graph.all(phraseLanguageLayerId)) {
            if (phrase.getLabel().toLowerCase().startsWith("en")) {
               for (Annotation token : phrase.all(tokenLayerId)) {
                  // tag only tokens that are not already tagged
                  if (token.first(pronunciationLayerId) == null) { // not tagged yet
//TODO                     token.createTag(pronunciationLayerId, stem(token.getLabel()))
//                        .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
                  } // not tagged yet
               } // next token in the phrase
            } // English phrase
         } // next phrase
      } // thereArePhraseTags
      
      return graph;
   }
   
}
