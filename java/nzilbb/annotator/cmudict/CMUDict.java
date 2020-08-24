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
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Vector;
import javax.script.ScriptException;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.Dictionary;
import nzilbb.ag.automation.DictionaryException;
import nzilbb.ag.automation.ImplementsDictionaries;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.UsesRelationalDatabase;
import nzilbb.sql.mysql.MySQLTranslator;
import nzilbb.util.IO;

/**
 * Annotator that tags words with their pronunciations according to the 
 * <a href="http://www.speech.cs.cmu.edu/cgi-bin/cmudict"> CMU Pronouncing Dictionary </a>.
 */
@UsesRelationalDatabase
@UsesFileSystem
public class CMUDict extends Annotator
   implements ImplementsDictionaries {
   /** Get the minimum version of the nzilbb.ag API supported by the serializer.*/
   public String getMinimumApiVersion() { return "20200708.2018"; }
   
   private PrintWriter log;
   private static SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
   static {
      time.setTimeZone(TimeZone.getTimeZone("UTC"));
   }

   /**
    * Open the log for logging statuses.
    */
   public void openLog() {
      if (log == null) {
         try {
            log = new PrintWriter(new FileWriter(new File(getWorkingDirectory(), "status.log")));
         } catch(Throwable t) {
            System.err.println("CMUDict.openLog: " + t);
            t.printStackTrace(System.err);
            setStatus("Could not open log: " + t);
         }
      }
   } // end of openLog()

   /**
    * Closes the log for logging statuses.
    */
   public void closeLog() {
      if (log != null) {
         try {
            log.close();
         } catch(Throwable t) {
            System.err.println("CMUDict.closeLog: " + t);
            t.printStackTrace(System.err);
            setStatus("Could not close log: " + t);
         }
         log = null;
      }
   } // end of closeLog()

   
   /**
    * Setter for {@link #status}: The current status of the task.
    * @param status The current status of the task.
    */
   @Override
   public Annotator setStatus(String status) {
      super.setStatus(status);
      if (log != null) log.println(time.format(new Date()) + ": " +status);
      return this;
   }

   /**
    * {@link UsesRelationalDatabase} method that sets the information required for
    * connecting to the relational database. 
    * @param sqlTranslator SQL statement translator.
    * @param url URL for relational database, e.g. <q>jdbc:mysql://localhost/labbcat</q>
    * @param user Username for connecting to the database, if any.
    * @param password Password for connecting to the database, if any.
    * @throws SQLException If the annotator can't connect to the given database.
    */
   @Override
   public void rdbConnectionDetails(
      MySQLTranslator sqlTranslator, String url, String user, String password)
      throws SQLException {
      super.rdbConnectionDetails(sqlTranslator, url, user, password);

      // get DB connection
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
            
            PreparedStatement sql = rdb.prepareStatement(
               sqlx.apply(
                  "CREATE TABLE "+getAnnotatorId()+"_wordform ("
                  +" wordform VARCHAR(100)"
                  +" CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL"
                  +" COMMENT 'Orthographic spelling of the word, in lowercase letters',"
                  +" variant SMALLINT NOT NULL"
                  +" COMMENT 'Pronunciation number, zero-based',"
                  +" pron_cmudict VARCHAR(200)"
                  +" CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL"
                  +" COMMENT 'Original pronunciation in ARPAbet, from the cmudict file',"
                  +" supplemental BIT NOT NULL DEFAULT 0"
                  +" COMMENT 'Whether the word/variant has been manually added"
                  +" since uploading from the cmudict file',"		  
                  +" PRIMARY KEY (wordform,variant)"
                  +") ENGINE=MyISAM"));
	    sql.executeUpdate();
	    sql.close();
         }
      } finally {
         try { rdb.close(); } catch(SQLException x) {}
      }      
   }
   
   /**
    * Runs any processing required to uninstall the annotator.
    * <p> In this case, the table created in rdbConnectionDetails() is DROPped.
    */
   @Override
   public void uninstall() {
      try {
         Connection rdb = newConnection();      
         try {
            
            // check the schema has been created
            PreparedStatement sql = rdb.prepareStatement(
               sqlx.apply("DROP TABLE "+getAnnotatorId()+"_wordform"));
            sql.executeUpdate();
	    sql.close();
            
         } finally {
            try { rdb.close(); } catch(SQLException x) {}
         }      
      } catch (SQLException x) {
      }
   }
   
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
   public String getConfig() {
      return null;
   }
   
   /**
    * Installs or updates the database schema and the contents of the <q>cmudict.txt file</q>.
    * @throws InvalidConfigurationException
    * @see #getConfig()
    * @see #beanPropertiesFromQueryString(String)
    */ 
   public void setConfig(String config) throws InvalidConfigurationException {
      running = true;
      try {
         openLog();
         setStatus(""); // clear any residual status from the last run...
         
         // has the dictionary data been added?
         Connection rdb = newConnection();
         
         try { // (but finally close rdb...)
            
            PreparedStatement sql = rdb.prepareStatement(
               sqlx.apply("SELECT COUNT(*) AS theCount FROM "+getAnnotatorId()+"_wordform"));
            ResultSet rs = sql.executeQuery();
            rs.next();
            int wordCount = wordCount = rs.getInt(1);
            rs.close();
            sql.close();
            
            File cmuDictFile = new File(getWorkingDirectory(), "cmudict.txt");
            if (wordCount == 0 || cmuDictFile.exists()) {
               // load data into the table
               URL urlCmudictTxt = getClass().getResource("cmudict.txt");
               if (urlCmudictTxt == null) {
                  setStatus("ERROR: Could not find cmudict.txt");
                  throw new InvalidConfigurationException(this, "Could not find cmudict.txt");
               }
               // use the file they uploaded, if any
               if (cmuDictFile.exists()) {
                  setStatus("Loading pronunciations into wordform table from uploaded file...");
                  urlCmudictTxt = cmuDictFile.toURI().toURL();
                  // delete any existing entries
                  sql = rdb.prepareStatement(
                     sqlx.apply("DELETE FROM "+getAnnotatorId()+"_wordform"));
                  sql.executeUpdate();
                  sql.close();
               } else {
                  setStatus("Loading pronunciations into wordform table from built-in file...");
               }
               wordCount = loadDictionary(urlCmudictTxt.openStream(), rdb);
               setStatus("Number of pronunciations processed: " + wordCount);
               if (cmuDictFile.exists()) cmuDictFile.delete();
            } else {
               setStatus("Dictionary already loaded.");
            }
            
            setPercentComplete(100);
         } finally {
            rdb.close();
         }
      } catch (SQLException sqlX) {
         setStatus("ERROR: " + sqlX);
         throw new InvalidConfigurationException(
            this, "Error configuring database: " + sqlX.getMessage(), sqlX);
      } catch (IOException ioX) {
         setStatus("ERROR: " + ioX);
         throw new InvalidConfigurationException(
            this, "Error reading dictionary file: " + ioX.getMessage(), ioX);
      } finally {
         closeLog();
         running = false;
      }
   }
   
   /**
    * Takes a file to be used instead of the built-in copy of cmudict.txt
    * @param file The lexicon file.
    * @return null if upload was successful, an error message otherwise.
    */
   public String uploadLexicon(File file) {
      System.out.println("File: " + file.getPath());
      File cmuDictFile = new File(getWorkingDirectory(), "cmudict.txt");
      if (file.renameTo(file)) {
         try {
            IO.Copy(file, cmuDictFile);
         } catch(IOException exception) {
            return "Could not copy " + file.getName() + ": " + exception.getMessage();
         }
      }
      return null;
   } // end of uploadLexicon()

   /**
    * Reads the given stream and loads the corresponding dictionary entries into the
    * wordform table. 
    * @param is
    * @return The number of pronunciations found.
    * @throws IOException
    * @throws SQLException
    */
   public int loadDictionary(InputStream is, Connection connection)
      throws IOException, SQLException
   {
      PreparedStatement sql = connection.prepareStatement(
	 sqlx.apply("REPLACE INTO "+getAnnotatorId()+"_wordform"
                    +" (wordform, variant, pron_cmudict) VALUES (?,?,?)"));
      BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
      int pronunciationCount = 0;
      int lineCount = 0;
      try {
	 String line = reader.readLine();
	 while (line != null) {
	    if (isCancelling()) break;
	    lineCount++;
	    if (line.startsWith(";;;")) { // comment
	       if (line.startsWith(";;; # CMUdict")) {
                  // note the header, which contains the version
                  FileWriter version = new FileWriter(new File(getWorkingDirectory(), "version.txt"));
                  version.write(line);
                  version.close();
	       }
	    } else if (line.trim().length() > 0) { // not a comment
	       try {
		  String delimiter = "  ";
		  int delimiterPos = line.indexOf(delimiter);
		  String wordForm = line.substring(0, delimiterPos);
		  int variant = 0;
		  if (wordForm.endsWith(")")) { // variant something like "ABKHAZIAN(3)"
		     int openParenthesisPos = wordForm.lastIndexOf('(');
		     if (openParenthesisPos > 0) {
			variant = Integer.parseInt(
			   wordForm.substring(openParenthesisPos+1, wordForm.length() - 1));
		     }
		     wordForm = wordForm.substring(0, openParenthesisPos);
		  } // variant
		  wordForm = wordForm.toLowerCase();
		  String pronunciation = line.substring(delimiterPos + delimiter.length());
		  sql.setString(1, wordForm);
		  sql.setInt(2, variant);
		  sql.setString(3, pronunciation);
		  sql.executeUpdate();
		  pronunciationCount++;
		  
		  setPercentComplete((wordForm.charAt(0) - 'a') * 100/26);
	       } catch (Throwable t) {
		  setStatus("ERROR line "+lineCount+" \""+line+"\": " + t);
	       }
	    } // not a comment
            
	    line = reader.readLine();
	 } // next line
      } finally {
	 reader.close();
	 sql.close();
      }
      return pronunciationCount;
   } // end of loadDictionary()

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
    * Whether to use only the first pronunciation if there are multiple pronunciations.
    * @see #getFirstVariantOnly()
    * @see #setFirstVariantOnly(Boolean)
    */
   protected Boolean firstVariantOnly;
   /**
    * Getter for {@link #firstVariantOnly}: Whether to use only the first pronunciation if
    * there are multiple pronunciations. 
    * @return Whether to use only the first pronunciation if there are multiple pronunciations.
    */
   public Boolean getFirstVariantOnly() { return firstVariantOnly; }
   /**
    * Setter for {@link #firstVariantOnly}: Whether to use only the first pronunciation if
    * there are multiple pronunciations. 
    * @param newFirstVariantOnly Whether to use only the first pronunciation if there are
    * multiple pronunciations. 
    */
   public CMUDict setFirstVariantOnly(Boolean newFirstVariantOnly) {
      firstVariantOnly = newFirstVariantOnly; return this; }

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
         firstVariantOnly = Boolean.FALSE;
         
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
      if (firstVariantOnly == null) firstVariantOnly = Boolean.FALSE;
      
      if (schema.getLayer(tokenLayerId) == null)
         throw new InvalidConfigurationException(this, "Token layer not found: " + tokenLayerId);
      if (transcriptLanguageLayerId != null && schema.getLayer(transcriptLanguageLayerId) == null) 
         throw new InvalidConfigurationException(
            this, "Transcript language layer not found: " + transcriptLanguageLayerId);
      if (phraseLanguageLayerId != null && schema.getLayer(phraseLanguageLayerId) == null) 
         throw new InvalidConfigurationException(
            this, "Phrase language layer not found: " + phraseLanguageLayerId);
      
      // does the outputLayer need to be added to the schema?
      Layer pronunciationLayer = schema.getLayer(pronunciationLayerId);
      if (pronunciationLayer == null) {
         schema.addLayer(
            new Layer(pronunciationLayerId)
            .setAlignment(Constants.ALIGNMENT_NONE)
            .setPeers(!firstVariantOnly)
            .setParentId(schema.getWordLayerId()));
      } else {
         if (pronunciationLayerId.equals(tokenLayerId)
             || pronunciationLayerId.equals(transcriptLanguageLayerId)
             || pronunciationLayerId.equals(phraseLanguageLayerId)) {
                throw new InvalidConfigurationException(
                   this, "Invalid pronunciation layer: " + pronunciationLayerId);
             }
         if (!pronunciationLayer.getPeers() && !firstVariantOnly) {
            setStatus(
               "Pronunciation layer " + pronunciationLayerId
               + " doesn't allow peer annotations; using first variant only.");
            firstVariantOnly = true;
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
      running = true;
      try {
         openLog();
         setStatus(""); // clear any residual status from the last run...
         
         Layer tokenLayer = graph.getSchema().getLayer(tokenLayerId);
         if (tokenLayer == null) {
            throw new InvalidConfigurationException(
               this, "Invalid input token layer: " + tokenLayerId);
         }
         Layer pronLayer = graph.getSchema().getLayer(pronunciationLayerId);
         if (pronLayer == null) {
            throw new InvalidConfigurationException(
               this, "Invalid output pronunciation layer: " + pronunciationLayerId);
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

         Vector<Annotation> toAnnotate = new Vector<Annotation>();
         // should we just tag everything?
         if (transcriptIsMainlyEnglish && !thereArePhraseTags) {
            // process all tokens
            for (Annotation token : graph.all(tokenLayerId)) {
               // tag only tokens that are not already tagged
               if (token.first(pronunciationLayerId) == null) { // not tagged yet
                  toAnnotate.add(token);                        
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
                     toAnnotate.add(token);
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
                        toAnnotate.add(token);                  
                     } // not tagged yet
                  } // next token in the phrase
               } // English phrase
            } // next phrase
         } // thereArePhraseTags
         
         try {
            Dictionary dictionary = getDictionary("cmudict");
            try {
               for (Annotation token : toAnnotate) {                              
                  for (String pronunciation : dictionary.lookup(token.getLabel())) {
                     
                     token.createTag(pronunciationLayerId, pronunciation)
                        .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
                     
                     // do we want the first entry only?
                     if (firstVariantOnly) break;
                     
                  } // next entry
               } // next token
            } finally {
               dictionary.close();
            }
         } catch (DictionaryException x) {
            throw new TransformationException(this, x);
         }
         return graph;
      } finally {
         closeLog();
         running = false;
      }
   }
   
   /**
    * Lists the dictionaries implemented by this Annotator.
    * <p> This method can assume that the following methods have been previously called:
    * <ul>
    *  <li> {@link Annotator#setSchema(Schema)} </li>
    *  <li> {@link Annotator#setTaskParameters(String)} </li>
    *  <li> {@link Annotator#setWorkingDirectory(File)} (if applicable) </li>
    *  <li> {@link Annotator#rdbConnectionDetails(String,String,String)}
    *       (if applicable) </li>
    * </ul>
    * @return A (possibly empty) list of IDs of dictionaries.
    */
   public List<String> getDictionaryIds() {
      return new Vector<String>() {{ add("cmudict"); }};
   }
   
   /**
    * Gets the identified dictionary.
    * <p> This method can assume that the following methods have been previously called:
    * <ul>
    *  <li> {@link Annotator#setSchema(Schema)} </li>
    *  <li> {@link Annotator#setTaskParameters(String)} </li>
    *  <li> {@link Annotator#setWorkingDirectory(File)} (if applicable) </li>
    *  <li> {@link Annotator#rdbConnectionDetails(String,String,String)}
    *       (if applicable) </li>
    * </ul>
    * @return The identified dictionary.
    * @throws DictionaryException If the given dictionary doesn't exist.
    */
   public Dictionary getDictionary(String id) throws DictionaryException {
      if (!"cmudict".equals(id)) {
         throw new DictionaryException(null, "Invalid dictionary: " + id);
      }
      try {
         return new CMUDictionary(this, newConnection(), sqlx);
      } catch (SQLException sqlX) {
         throw new DictionaryException(null, sqlX);
      }
   }
}
