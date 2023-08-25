//
// Copyright 2020-2022 New Zealand Institute of Language, Brain and Behaviour, 
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Vector;
import java.util.stream.Collectors;
import javax.script.ScriptException;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.Dictionary;
import nzilbb.ag.automation.DictionaryException;
import nzilbb.ag.automation.ImplementsDictionaries;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.UsesRelationalDatabase;
import nzilbb.sql.ConnectionFactory;
import nzilbb.util.IO;

/**
 * Annotator that tags words with their pronunciations according to the 
 * <a href="http://www.speech.cs.cmu.edu/cgi-bin/cmudict"> CMU Pronouncing Dictionary </a>.
 *
 * <p> In addition to looking up the CMU dictionary, the annotator can store labels using
 * the CELEX DISC encoding system as well as the native ARPAbet encoding.
 * <p> If the DISC encoding is used, the annotator also supports automatically tagging
 * short hesitations with pronunciations. Orthographies with trailing '~' are recognized
 * as short hesitations  e.g.
 * <ul>
 *  <li> <q> s~ </q> → <tt> s@ </tt></li>
 *  <li> <q> se~ </q> → <tt> s@ </tt></li>
 *  <li> <q> a~ </q> → <tt> { </tt></li>
 *  <li> <q> ph~ </q> → <tt> f@ </tt></li>
 * </ul>
 */
@UsesRelationalDatabase
@UsesFileSystem
public class CMUDictionaryTagger extends Annotator
  implements ImplementsDictionaries {
  /** Get the minimum version of the nzilbb.ag API supported by the annotator.*/
  public String getMinimumApiVersion() { return "1.0.6"; }
   
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
        System.err.println("CMUDictionaryTagger.openLog: " + t);
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
        System.err.println("CMUDictionaryTagger.closeLog: " + t);
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
   * @param db Connection factory for getting new database connections.
   * @throws SQLException If the annotator can't connect to the given database.
   */
  @Override
  public void setRdbConnectionFactory(ConnectionFactory db)
    throws SQLException {
    super.setRdbConnectionFactory(db);

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
   * <p> In this case, the table created in rdbConnectionFactory() is DROPped.
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
    setRunning(true);
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
      setRunning(false);
    }
  }
   
  /**
   * Takes a file to be used instead of the built-in copy of cmudict.txt
   * @param file The lexicon file.
   * @return null if upload was successful, an error message otherwise.
   */
  public String uploadLexicon(File file) {
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
  public CMUDictionaryTagger setTokenLayerId(String newTokenLayerId) {
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
  public CMUDictionaryTagger setTranscriptLanguageLayerId(String newTranscriptLanguageLayerId) {
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
  public CMUDictionaryTagger setPhraseLanguageLayerId(String newPhraseLanguageLayerId) {
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
  public CMUDictionaryTagger setPronunciationLayerId(String newPronunciationLayerId) {
    pronunciationLayerId = newPronunciationLayerId; return this; }

  /**
   * Phoneme encoding - "CMU" or "DISC".
   * @see #getEncoding()
   * @see #setEncoding(String)
   */
  protected String encoding;
  /**
   * Getter for {@link #encoding}: Phoneme encoding - "CMU" or "DISC".
   * @return Phoneme encoding - "CMU" or "DISC".
   */
  public String getEncoding() { return encoding; }
  /**
   * Setter for {@link #encoding}: Phoneme encoding - "CMU" or "DISC".
   * @param newEncoding Phoneme encoding - "CMU" or "DISC".
   */
  public CMUDictionaryTagger setEncoding(String newEncoding) { encoding = newEncoding; return this; }

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
  public CMUDictionaryTagger setFirstVariantOnly(Boolean newFirstVariantOnly) {
    firstVariantOnly = newFirstVariantOnly; return this; }

  /**
   * Sets the configuration for a given annotation task.
   * @param parameters The configuration of the annotator; a value of <tt> null </tt>
   * will apply the default task parameters, with {@link #tokenLayerId} set to the
   * {@link Schema#wordLayerId} and {@link #pronunciationLayerId} set to <q>phonemes</q>.
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
      firstVariantOnly = Boolean.FALSE;
         
      // default transcript language layer
      Layer[] candidates = schema.getMatchingLayers(
        layer -> schema.getRoot().getId().equals(layer.getParentId())
        && layer.getAlignment() == Constants.ALIGNMENT_NONE // transcript attribute
        && layer.getId().toLowerCase().matches(".*lang.*")); // with 'lang' in the name
      if (candidates.length > 0) transcriptLanguageLayerId = candidates[0].getId();
      
      // default phrase language layer
      candidates = schema.getMatchingLayers(
        layer -> schema.getTurnLayerId() != null
        && schema.getTurnLayerId().equals(layer.getParentId()) // child of turn
        && layer.getId().toLowerCase().matches(".*lang.*")); // with 'lang' in the name
      if (candidates.length > 0) phraseLanguageLayerId = candidates[0].getId();
      
      // default output layer
      candidates = schema.getMatchingLayers(
        layer -> schema.getWordLayerId() != null
        && schema.getWordLayerId().equals(layer.getParentId())
        && layer.getAlignment() == Constants.ALIGNMENT_NONE // word tag
        && layer.getId().toLowerCase().matches(".*lang.*") // with 'lang' in the name
        && (layer.getId().toLowerCase().matches(".*phoneme.*")
            || layer.getId().toLowerCase().matches(".*pronunciation.*")));
      if (candidates.length > 0) {
        pronunciationLayerId = candidates[0].getId();
      } else { // suggest adding a new one
        pronunciationLayerId = "phonemes";
      }
      
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
      throw new InvalidConfigurationException(this, "Pronunciation layer not set.");
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
    setRunning(true);
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
          int t = 0;
          int tokenCount = toAnnotate.size();
          setPercentComplete(0);
          for (Annotation token : toAnnotate) {
            if (isCancelling()) break;
            boolean found = false;
            for (String pronunciation : dictionary.lookup(token.getLabel())) {

              found = true;
              token.createTag(pronunciationLayerId, pronunciation)
                .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
                     
              // do we want the first entry only?
              if (firstVariantOnly) break;
                     
            } // next entry
            if (!found) { // might be a hesitation?
              String pronunciation = hesitationToDISC(token.getLabel());
              if (pronunciation != null) {
                token.createTag(pronunciationLayerId, pronunciation)
                  .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
              }
            }
            setPercentComplete(++t * 100 / tokenCount);
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
      setRunning(false);
    }
  }
   
  /**
   * Lists the dictionaries implemented by this Annotator.
   * <p> This method can assume that the following methods have been previously called:
   * <ul>
   *  <li> {@link Annotator#setSchema(Schema)} </li>
   *  <li> {@link Annotator#setTaskParameters(String)} </li>
   *  <li> {@link Annotator#setWorkingDirectory(File)} (if applicable) </li>
   *  <li> {@link Annotator#setRdbConnectionFactory(ConnectionFactory)}
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
   *  <li> {@link Annotator#setRdbConnectionFactory(ConnectionFactory)}
   *       (if applicable) </li>
   * </ul>
   * @return The identified dictionary.
   * @throws DictionaryException If the given dictionary doesn't exist.
   */
  public Dictionary getDictionary(String id) throws DictionaryException {
    if (id != null && !id.equals("cmudict")) { // null is allowed
      throw new DictionaryException(null, "Invalid dictionary: " + id);
    }
    try {
      return new CMUDictionary(this, newConnection(), sqlx)
        .setEncoding(
          "DISC".equals(encoding)?CMUDictionary.Encoding.DISC:CMUDictionary.Encoding.CMU);
    } catch (SQLException sqlX) {
      throw new DictionaryException(null, sqlX);
    }
  }

  /**
   * Tags all instances of the given word in the given graph store, using the dictionary
   * specified by current task configuration (i.e. the dictionary returned by
   * <code>getDictionary(null)</code>).
   * <p> The default implementation throws TransformationException
   * @param store
   * @param sourceLabel
   * @return The number of tags created.
   * @throws DictionaryException, TransformationException, InvalidConfigurationException,
   * StoreException 
   */
  @Override public int tagAllInstances(GraphStore store, String sourceLabel)
    throws DictionaryException, TransformationException, InvalidConfigurationException,
    StoreException {
    Dictionary dictionary = getDictionary("cmudict");
    try {
      
      store.deleteMatchingAnnotations(
        "layerId = '"+esc(pronunciationLayerId)+"'"
        +" && first('"+esc(tokenLayerId)+"').label == '"+esc(sourceLabel)+"'");

      String tokenExpression = "layerId = '"+esc(tokenLayerId)+"'"
        +" && label = '"+esc(sourceLabel)+"'";
      if (transcriptLanguageLayerId != null) {
        tokenExpression += " && /en.*/.test(first('"+esc(transcriptLanguageLayerId)+"').label)";
      }
      int count = 0;
      for (String pronunciation : dictionary.lookup(sourceLabel)) {
        
        store.tagMatchingAnnotations(
          tokenExpression, pronunciationLayerId, pronunciation, Constants.CONFIDENCE_AUTOMATIC);
        count++;
        // do we want the first entry only?
        if (firstVariantOnly) break;        
      } // next entry
      return count;
    } catch(PermissionException x) {
      throw new TransformationException(this, x);
    } finally {
      dictionary.close();
    }
  }
  
  /**
   * Transforms all graphs from the given graph store that match the given graph expression.
   * <p> This implementation uses
   * {@link GraphStoreQuery#aggregateMatchingAnnotations(String,String)}
   * and {@link GraphStore#tagMatchingAnnotations​(String,String,String,Integer)}
   * to optimize tagging transcripts en-masse.
   * @param store The graph to store.
   * @param expression An expression for identifying transcripts to update, or null to transform
   * all transcripts in the store.
   * @return The changes introduced by the tranformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public void transformTranscripts​(GraphStore store, String expression)
    throws TransformationException, InvalidConfigurationException, StoreException,
    PermissionException {
    setRunning(true);
    try {
      setPercentComplete(0);
      Layer tokenLayer = schema.getLayer(tokenLayerId);
      if (tokenLayer == null) {
        throw new InvalidConfigurationException(
          this, "Invalid input token layer: " + tokenLayerId);
      }
      Layer pronunciationLayer = schema.getLayer(pronunciationLayerId);
      if (pronunciationLayer == null) {
        throw new InvalidConfigurationException(
          this, "Invalid output pronunciation layer: " + pronunciationLayerId);
      }    
      
      StringBuilder labelExpression = new StringBuilder();
      labelExpression.append("layer.id == '").append(esc(tokenLayer.getId())).append("'");
      if (phraseLanguageLayerId != null || transcriptLanguageLayerId != null) {
        labelExpression.append(" && /").append("en.*").append("/.test(");
        if (phraseLanguageLayerId != null) {
          labelExpression.append("first('").append(esc(phraseLanguageLayerId))
            .append("').label");
          if (transcriptLanguageLayerId != null) {
            labelExpression.append(" ?? "); // add coalescing operator
          }
        }
        if (transcriptLanguageLayerId != null) {
          labelExpression.append("first('").append(esc(transcriptLanguageLayerId))
            .append("').label");
        }
        labelExpression.append(")");
      } // add language condition
      
      if (expression != null && expression.trim().length() > 0) {
        labelExpression.append(" && [");
        String[] ids = store.getMatchingTranscriptIds(expression);
        if (ids.length == 0) {
          setStatus("No matching transcripts");
          setPercentComplete(100);
          return;
        } else {
          labelExpression.append(
            Arrays.stream(ids)
            // quote and escape each ID
            .map(id->"'"+id.replace("'", "\\'")+"'")
            // make a comma-delimited list
            .collect(Collectors.joining(",")));
          labelExpression.append("].includes(graphId)");
        }
      }
      setStatus("Getting distinct token labels...");
      String[] distinctWords = store.aggregateMatchingAnnotations(
        "DISTINCT", labelExpression.toString());
      setStatus("There are "+distinctWords.length+" distinct token labels");
      int w = 0;
      Dictionary dictionary = getDictionary("cmudict");
      // for each label
      for (String word : distinctWords) {
        if (isCancelling()) break;
        boolean found = false;
        StringBuilder tokenExpression = new StringBuilder(labelExpression);
        tokenExpression.append(" && label == '").append(esc(word)).append("'");
        for (String pronunciation : dictionary.lookup(word)) {
          if (isCancelling()) break;
          setStatus(word+" → "+pronunciation);
          found = true;
          store.tagMatchingAnnotations(
            tokenExpression.toString(), pronunciationLayerId, pronunciation,
            Constants.CONFIDENCE_AUTOMATIC);
          // do we want the first entry only?
          if (firstVariantOnly) break;        
        } // next entry
        if (!found) { // might be a hesitation?
          String pronunciation = hesitationToDISC(word);
          if (pronunciation != null) {
            store.tagMatchingAnnotations(
              tokenExpression.toString(), pronunciationLayerId, pronunciation,
              Constants.CONFIDENCE_AUTOMATIC);
          }
        }
        setPercentComplete((++w * 100) / distinctWords.length);
      } // next word
      if (isCancelling()) {
        setStatus("Cancelled.");
      } else {
        setPercentComplete(100);
        setStatus("Finished.");
      }
    } catch(DictionaryException x) {
      throw new TransformationException(this, x);
    } finally {
      setRunning(false);
    }
  }
  
  /**
   * Escapes quotes in the given string for inclusion in QL or SQL queries.
   * @param s The string to escape.
   * @return The given string, with quotes escapeed.
   */
  private String esc(String s) {
    if (s == null) return "";
    return s.replace("\\","\\\\").replace("'","\\'");
  } // end of esc()  

  /**
   * Converts a possible single-phoneme hesitation into it's DISC phonology
   * representation.  Orthographies with trailing '~' are recognized as short hesitations
   * - e.g. 's~' is converted to 's@', 'a~' to 'a', 'ph~' to 'f@'.  
   * <p>This also recognizes consonant-followed-by-vowel hesitiations - e.g. 'se~' is
   * also converted to 's@'. 
   * @param orthography Source orthography. If this does not have a trailing '~' or
   * contains too many letters, the method will return null. 
   * @return The DISC phonological representation of the given source orthography, or
   * null if {@link #encoding} != "DISC" or no orthography is appropriate. Consonants
   * have schwa appended.  
   */
  public String hesitationToDISC(String orthography) {
    if (orthography == null) return null;
    if (!"DISC".equals(encoding)) return null;
    String disc = null;
    if (orthography.endsWith("~")) {
      // strip of trailing ~
      orthography = orthography.substring(0, orthography.length() - 1);
         
      // if it's a consonant followed by a vowel
      if (orthography.matches("^[^aieou][aieou]$")) {	       
        // then strip off the vowel, so that cases like
        // 'fi~' are treated like 'f~'
        orthography = orthography.substring(0,1);
      }
      // if it's two consonants followed by a vowel
      if (orthography.matches("^[^aieou][^aieou][aieou]$")) {	       
        // then strip off the vowel, so that cases like
        // 'shi~' are treated like 'sh~'
        orthography = orthography.substring(0,2);
      }
         
      // is it a single character?
      if (orthography.length() == 1) {
        // the phoneme is the character
        // ... but deal with exceptional cases
        switch (orthography.charAt(0)) {
          // these whouldn't be used, but just in case...
          case 'c': { disc = "k"; break; }
          case 'q': { disc = "k"; break; }
                  
            // consonants
          case 'j': { disc = "_"; break; }
          case 'y': { disc = "j"; break; }
                  
            // vowels
          case 'a': { disc = "{"; break; } // trap
          case 'e': { disc = "E"; break; } // dress
          case 'i': { disc = "I"; break; } // kit
          case 'o': { disc = "Q"; break; } // lot
          case 'u': { disc = "V"; break; } // strut
                  
            // otherwise, just pass it through
          default: { disc = orthography; }
        }
      } else if (orthography.length() == 2) {
        // deal with multi-letter possibilities
        if (orthography.equals("ng")) { // ngati
          disc = "N"; 
        } else if (orthography.equals("th")) { // think, thought
          disc = "T";
        } else if (orthography.equals("dh")) { // then, they
          disc = "D";
        } else if (orthography.equals("sh")) { // sheet shine
          disc = "S";
        } else if (orthography.equals("ch")) { // cheat, china
          disc = "J";
        } else if (orthography.equals("wh")) { // what, which
          disc = "hw"; // taken to be aspirated
        } else if (orthography.equals("ph")) { // phonology, phew
          disc = "f";
        } else if (orthography.equals("gn")) { // gnome, gnash
          disc = "n";
        } else if (orthography.equals("kn")) { // know, knife
          disc = "n";
        } else if (orthography.equals("pn")) { // pneumatic, pneumonia
          disc = "nj";
        } else if (orthography.equals("ps")) { // psychology, psalm
          disc = "s";
        } else if (orthography.equals("pt")) { // ptomaine, pterodactyl
          disc = "t";
        } else if (orthography.equals("wr")) { // wrack, write
          disc = "r";
        }
      }
         
      if (disc != null) {
        // if it's not a vowel, append schwa
        switch (disc.charAt(0)) {
          // vowels
          case 'I': case 'E': case '{': case 'V': case 'Q': case '@':
          case 'i': case '#': case '$': case 'u': case '3': case '1':
          case '2': case '4': case '5': case '6': case '7': case '8':
          case '9': case 'c': case 'q': case '0': case '"': 
            break; // do nothing
          default: 
            disc += "@";
        }
      }
    }
    return disc;
  } // end of hestitationToDISC()

}
