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
package nzilbb.annotator.flatlexicon;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Annotator that tags words with their entries according to lexicons specified by 
 * 'flat', plain text files - e.g. CSV files.
 */
@UsesRelationalDatabase
public class FlatLexiconTagger extends Annotator implements ImplementsDictionaries {
  /** Get the minimum version of the nzilbb.ag API supported by the annotator.*/
  public String getMinimumApiVersion() { return "1.0.6"; }
     
  /**
   * Setter for {@link #status}: The current status of the task.
   * @param status The current status of the task.
   */
  @Override
  public Annotator setStatus(String status) {
    super.setStatus(status);
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
          sqlx.apply("SELECT lexicon_id, name FROM "+getAnnotatorId()+"_lexicon ORDER BY name"));
        try {
          ResultSet rsCheck = sql.executeQuery();
          rsCheck.close();
        } finally {
          sql.close();
        }
      } catch(SQLException exception) {
        
        PreparedStatement sql = rdb.prepareStatement(
          sqlx.apply(
            "CREATE TABLE "+getAnnotatorId()+"_lexicon ("
            +" lexicon_id INTEGER NOT NULL AUTO_INCREMENT"
            +" COMMENT 'ID which is appended to "
            +getAnnotatorId()+" to compute the lexicon table name',"
            +" name varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL"
            +" COMMENT 'Identifying name for the lexicon',"
            +" PRIMARY KEY (lexicon_id)"
            +") ENGINE=MyISAM;"));
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

        PreparedStatement sql = rdb.prepareStatement(
          sqlx.apply("SELECT lexicon_id, name FROM "+getAnnotatorId()+"_lexicon ORDER BY name"));
        ResultSet rs = sql.executeQuery();
        while (rs.next()) {
          String sName = rs.getString("name");
          String sLexiconTable = getAnnotatorId()+"_lexicon_"+rs.getInt("lexicon_id");
          PreparedStatement deleteLexicon = rdb.prepareStatement(
	    sqlx.apply("DROP TABLE " + sLexiconTable));
          deleteLexicon.executeUpdate();
          deleteLexicon.close();
        } // next lexicon
        rs.close();
        sql.close();
        
        // check the schema has been created
        sql = rdb.prepareStatement(
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
   * Takes a file to be used instead of the built-in copy of cmudict.txt.
   * <p> This is the same as {@link #loadLexicon(String,String,String,String,String,boolean,File)}
   * Except that <var>skipFirstLine</var> is defined as a string, so it can be martialled
   * from a HTTP post request.
   * @param lexicon The name for the resulting lexicon. If the named lexicon already
   * exists, it will be completely replaced with the contents of the file (i.e. all
   * existing entries will be deleted befor adding new entries from the file).
   * @param fieldDelimiter The character used to delimit fields in the file. 
   * If this is " - ", rows are split on only the <em>first</em> space, in line with common
   * dictionary formats.
   * @param quote The character used to quote field values (if any)
   * @param comment The character used to indicate a line is a comment (not an entry) (if any)
   * @param fieldNames A list of field names, delimited by <var>fieldDelimiter</var>.
   * @param skipFirstLine Whether to ignore the first line (because it contains field names).
   * @param file The lexicon file.
   * @return null if upload was successful, an error message otherwise.
   */
  public String loadLexicon(
    String lexicon, String fieldDelimiter, String quote, String comment, String fieldNames,
    String skipFirstLine, File file) {
    return loadLexicon(
      lexicon, fieldDelimiter, quote, comment, fieldNames, "true".equals(skipFirstLine), file);
  }
  
  /**
   * Loads a lexicon from a given file.
   * <p> This method starts a thread to load the records, and then returns. Callers must
   * track {@link Annotator#getPercentComplete()} and {@link Annotator#getRunning()} for
   * progress updates.
   * @param lexicon The name for the resulting lexicon (e.g. "," for CSV). If the named
   * lexicon already exists, it will be completely replaced with the contents of the file
   * (i.e. all existing entries will be deleted befor adding new entries from the file).
   * @param fieldDelimiter The character used to delimit fields in the file. 
   * If this is " - ", rows are split on only the <em>first</em> space, in line with common
   * dictionary formats.
   * @param quote The character used to quote field values (if any), e.g. "\"".
   * @param comment The character used to indicate a line is a comment (not an entry) (if
   * any), e.g. "#".
   * @param fieldNames A list of field names, delimited by <var>fieldDelimiter</var>.
   * @param skipFirstLine Whether to ignore the first line (because it contains field names).
   * @param file The lexicon file.
   * @return An empty string if upload was successful, an error message otherwise.
   */
  public String loadLexicon(
    String lexicon, String fieldDelimiter, String quote, String comment, String fieldNames,
    boolean skipFirstLine, File file) {
    try {
      Connection rdb = newConnection();      
      try {
        // determine lexiconId
        
        int lexiconId = -1;
        // new or updated lexicon
        PreparedStatement sql = rdb.prepareStatement(
          sqlx.apply("SELECT lexicon_id FROM "+getAnnotatorId()+"_lexicon WHERE name = ?"));
        sql.setString(1, lexicon);
        ResultSet rs = sql.executeQuery();
        try {
          if (rs.next()) {
            lexiconId = rs.getInt(1);
            setStatus("Replacing existing lexicon: " + lexicon);
          }
        } finally {
          rs.close();
          sql.close();
        }
        
        if (lexiconId < 0) {
          sql = rdb.prepareStatement(
            sqlx.apply("INSERT INTO "+getAnnotatorId() +"_lexicon (name) VALUES (?)"));
          sql.setString(1, lexicon);
          sql.executeUpdate();
          sql.close();
          sql = rdb.prepareStatement(
            sqlx.apply("SELECT LAST_INSERT_ID() FROM "+getAnnotatorId()+"_lexicon"));
          rs = sql.executeQuery();
          rs.next();
          lexiconId = rs.getInt(1);
          setStatus("Adding new lexicon: " + lexicon);
        }
        
        // create lexicon table
        String sqlQuote = rdb.getMetaData().getIdentifierQuoteString();
        StringBuilder columnList = new StringBuilder();
        StringBuilder argumentList = new StringBuilder();
        StringBuilder columnDefinitions = new StringBuilder();
        Vector<String> columnIndexDefinitions = new Vector<String>();
        int columnCount = 0;
        for (String column : fieldNames.split(fieldDelimiter)) {
          if (column.length() == 0) column = "Field" + columnCount;
          columnList.append(", ").append(sqlQuote).append(column).append(sqlQuote);
          argumentList.append(",?");
          columnDefinitions.append(", ").append(sqlQuote).append(column).append(sqlQuote)
            .append(" varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL");
          columnIndexDefinitions.add(
            "CREATE INDEX IDX_"+getAnnotatorId()+"_"+lexiconId+"_"+(++columnCount)+" ON "
            +getAnnotatorId()+"_lexicon_"+lexiconId+" ("+sqlQuote+column+sqlQuote+")");
        } // next column
        
        // drop the table first (just in case it's already there)
        String sSql = "DROP TABLE "+getAnnotatorId()+"_lexicon_"+lexiconId;
        try {
          sql = rdb.prepareStatement(sqlx.apply(sSql));
          sql.executeUpdate();
        } 
        catch(Exception exception) {} // might not already exist
        finally {
          try { sql.close(); } catch(Exception exception) {}
        }   
        
        // create the table
        sSql = "CREATE TABLE "+getAnnotatorId() +"_lexicon_"+lexiconId+" ("
          +" serial INTEGER NOT NULL AUTO_INCREMENT"
          +" COMMENT 'Primary key and variant ordering field',"
          +" supplemental SMALLINT NOT NULL DEFAULT 0"
          +" COMMENT 'Whether the word/variant has been manually added since uploading from the original file'"
          + columnDefinitions
          +", PRIMARY KEY (serial)) ENGINE=MyISAM;";
        sql = rdb.prepareStatement(sqlx.apply(sSql));
        sql.executeUpdate();
        sql.close();
        for (String indexDecolumnIndexDefinition : columnIndexDefinitions) {
          sql = rdb.prepareStatement(sqlx.apply(indexDecolumnIndexDefinition));
          sql.executeUpdate();
          sql.close();
        }

        // load the lexicon in another thread - the caller must track isRunning...
        
        // the thread needs it's own copy of the file
        final File tempFile = File.createTempFile("FlatLexiconTagger-", file.getName());
        tempFile.deleteOnExit();
        
        // we also count records so we can accurates track progress
        int entryCount = 0;
        // and maybe convert the first space to a tab
        boolean firstSpace = fieldDelimiter.equals(" - ");
        BufferedReader reader = new BufferedReader(new FileReader(file));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
        String line = reader.readLine(); 
        while (line != null) {
          if (line.trim().length() > 0) { // ignore blank lines
            if (entryCount > 0) writer.newLine();
            if (firstSpace) line = line.replaceFirst(" ", "\t");
            writer.write(line);
            entryCount++;
          }
          line = reader.readLine();
        } // next line
        reader.close();
        writer.close();
        if (skipFirstLine) entryCount--;
        setStatus("Loading " + entryCount + (entryCount==1?" entry...":" entries..."));
        
        final int finalEntryCount = entryCount;
        final int finalLexiconId = lexiconId;
        final int finalColumnCount = columnCount;
        final String finalFieldDelimiter = firstSpace?"\t":fieldDelimiter;
        setRunning(true);
        Runnable loadFile = () -> {
          try {
            
            // now insert all entries into the lexicon
            Connection rdbLoad = newConnection();
            try {
              PreparedStatement sqlLoad = rdbLoad.prepareStatement(
                sqlx.apply("INSERT INTO "+getAnnotatorId() +"_lexicon_"+finalLexiconId
                           +" (supplemental"+columnList+")"
                           +" VALUES (0"+argumentList+")"));
              CSVFormat format = CSVFormat.DEFAULT
                .withIgnoreEmptyLines(true).withDelimiter(finalFieldDelimiter.charAt(0));
              if (comment != null && comment.length() > 0) {
                format = format.withCommentMarker(comment.charAt(0));
              }
              if (quote != null && quote.length() > 0) {
                format = format.withQuote(quote.charAt(0)).withEscape('\\');
              } else {
                format = format.withQuote(null);
              }
              CSVParser csv = new CSVParser(
                new InputStreamReader(new FileInputStream(tempFile), "UTF-8"), format);
              int e = 0;
              setPercentComplete(0);
              try {
                Iterator<CSVRecord> records = csv.iterator();
                if (skipFirstLine && records.hasNext()) records.next();
                while (records.hasNext() && !isCancelling()) {
                  CSVRecord record = records.next();
                  for (int c = 0; c < finalColumnCount; c++) {
                    try {
                      String value = record.get(c);
                      if (value.indexOf('(') > 0 && value.endsWith(")")) { // indexed e.g. a(1)
                        // strip the index
                        value = value.replaceAll("^(.*)(\\([0-9]+\\))$", "$1");
                      }
                      // setStatus("Row " +e+ " Column " + c + ": " + value);
                      sqlLoad.setString(c + 1, value.trim());
                    } catch(Exception exception) {
                      sqlLoad.setString(c + 1, "");
                    }
                  } // next column
                  sqlLoad.executeUpdate();	 
                  e++;
                  setPercentComplete(e * 100 / finalEntryCount);
                } // next line
                if (isCancelling()) {
                  setStatus("Load lexicon cancelled: " + lexicon);
                } else {
                  setStatus(
                    "Loaded " + finalEntryCount + (finalEntryCount==1?" entry.":" entries."));
                  setPercentComplete(100);
                }
                // setStatus("Adding "+e+(e==1?" entry":" entries"));
              } finally {
                csv.close();
                sqlLoad.close();
              }
            } finally {
              try { rdbLoad.close(); } catch(SQLException x) {}
            }
          } catch (Exception x) {
            setStatus("ERROR: " + x);
          } finally {
            tempFile.delete();
            setRunning(false);
          }
        };
        new Thread(loadFile).start();
        
        return "";
      } finally {
        try { rdb.close(); } catch(SQLException x) {}
      }
    } catch (Exception x) {
      return x.toString();
    }
  } // end of loadLexicon()
  
  /**
   * Lists lexicon names.
   * @return A list of lexicon names.
   * @throws SQLException
   */
  public List<String> listLexicons() throws SQLException {
    Vector<String> names = new Vector<String>();
    Connection rdb = newConnection();
    try {
      PreparedStatement sql = rdb.prepareStatement(
        sqlx.apply("SELECT name FROM "+getAnnotatorId()+"_lexicon ORDER BY name"));
      ResultSet rs = sql.executeQuery();
      while (rs.next()) {
        names.add(rs.getString("name"));
      } // next lexicon
      rs.close();
      sql.close();
    } finally {
      rdb.close();
    }
    return names;
  } // end of listLexicons()
  
  /**
   * Deletes the given lexicon.
   * @param lexicon
   * @return An error message, if any, or an empty string if not.
   */
  public String deleteLexicon(String lexicon) throws SQLException {
    Connection rdb = newConnection();      
    try {
      // find the lexicon
      PreparedStatement sql = rdb.prepareStatement(
        sqlx.apply("SELECT lexicon_id FROM "+getAnnotatorId()+"_lexicon WHERE name = ?"));
      sql.setString(1, lexicon);
      ResultSet rs = sql.executeQuery();
      try {
        if (rs.next()) {

          // drop the lexicon table
          String sLexiconTable = getAnnotatorId()+"_lexicon_"+rs.getInt("lexicon_id");
          String dropError = "";
          try {
            PreparedStatement deleteLexicon = rdb.prepareStatement(
              sqlx.apply("DROP TABLE " + sLexiconTable));
            try {
              deleteLexicon.executeUpdate();
            } finally {
              deleteLexicon.close();
            }
          } catch(SQLException exception) {
            dropError = exception.getMessage();
          }

          // delete the lexicon record
          try {
            PreparedStatement deleteLexicon = rdb.prepareStatement(
              sqlx.apply("DELETE FROM "+getAnnotatorId()+"_lexicon WHERE name = ?"));
            deleteLexicon.setString(1, lexicon);
            try {
              deleteLexicon.executeUpdate();
            } finally {
              deleteLexicon.close();
            }
          } catch(SQLException exception) {}
          return dropError;          
        } else {
          return "Nonexistent lexicon: " + lexicon;
        }
      } finally {
        rs.close();
        sql.close();
      }
    } finally {
      rdb.close();
    }
  } // end of deleteLexicon()  

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
  public FlatLexiconTagger setTokenLayerId(String newTokenLayerId) {
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
  public FlatLexiconTagger setTranscriptLanguageLayerId(String newTranscriptLanguageLayerId) {
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
  public FlatLexiconTagger setPhraseLanguageLayerId(String newPhraseLanguageLayerId) {
    if (newPhraseLanguageLayerId != null // empty string means null
        && newPhraseLanguageLayerId.trim().length() == 0) {
      newPhraseLanguageLayerId = null;
    }
    phraseLanguageLayerId = newPhraseLanguageLayerId;
    return this;
  }
  
  /**
   * Regular expression for specifying which language to tag the tokens of.
   * @see #getTargetLanguagePattern()
   * @see #setTargetLanguagePattern(String)
   */
  protected String targetLanguagePattern;
  /**
   * Getter for {@link #targetLanguagePattern}: Regular expression for specifying which
   * language to tag the tokens of. 
   * @return Regular expression for specifying which language to tag the tokens of.
   */
  public String getTargetLanguagePattern() { return targetLanguagePattern; }
  /**
   * Setter for {@link #targetLanguagePattern}: Regular expression for specifying which
   * language to tag the tokens of. 
   * @param newTargetLanguagePattern Regular expression for specifying which language to
   * tag the tokens of. 
   */
  public FlatLexiconTagger setTargetLanguagePattern(String newTargetLanguagePattern) { targetLanguagePattern = newTargetLanguagePattern; return this; }
  
  /**
   * The ID of the dictionary to use.
   * @see #getDictionary()
   * @see #setDictionary(String)
   */
  protected String dictionary;
  /**
   * Getter for {@link #dictionary}: The ID of the dictionary to use.
   * @return The ID of the dictionary to use.
   */
  public String getDictionary() { return dictionary; }
  /**
   * Setter for {@link #dictionary}: The ID of the dictionary to use.
   * @param newDictionary The ID of the dictionary to use.
   */
  public FlatLexiconTagger setDictionary(String newDictionary) { dictionary = newDictionary; return this; }

  /**
   * ID of the output layer.
   * @see #getTagLayerId()
   * @see #setTagLayerId(String)
   */
  protected String tagLayerId;
  /**
   * Getter for {@link #tagLayerId}: ID of the output layer.
   * @return ID of the output layer.
   */
  public String getTagLayerId() { return tagLayerId; }
  /**
   * Setter for {@link #tagLayerId}: ID of the output layer.
   * @param newTagLayerId ID of the output layer.
   */
  public FlatLexiconTagger setTagLayerId(String newTagLayerId) {
    tagLayerId = newTagLayerId; return this; }

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
  public FlatLexiconTagger setFirstVariantOnly(Boolean newFirstVariantOnly) {
    firstVariantOnly = newFirstVariantOnly; return this; }
  
  /**
   * Characters to remove from the entry tag labels, if any.
   * @see #getStrip()
   * @see #setStrip(String)
   */
  protected String strip;
  /**
   * Getter for {@link #strip}: Characters to remove from the entry tag labels, if any.
   * @return Characters to remove from the entry tag labels, if any.
   */
  public String getStrip() { return strip; }
  /**
   * Setter for {@link #strip}: Characters to remove from the entry tag labels, if any.
   * @param newStrip Characters to remove from the entry tag labels, if any.
   */
  public FlatLexiconTagger setStrip(String newStrip) { strip = newStrip; return this; }

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
      strip = "";
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
          tagLayerId = candidates[0].getId();
        } else { // suggest adding a new one
          tagLayerId = "phonemes";
        }
        // default lexicon/keyField/valueField if possible
        List<String> dictionaries = getDictionaryIds();
        if (dictionaries.size() > 0) { // there are dictionaries
          // default to the first, which will be field1→field2
          dictionary = dictionaries.get(0);
        } // there are dictionaries
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
    if (dictionary == null || dictionary.length() == 0)
      throw new InvalidConfigurationException(this, "Dictionary not specified");
    try { // check dictionary configuration is valid
      Dictionary dict = getDictionary(dictionary);
    } catch(DictionaryException exception) {
      throw new InvalidConfigurationException(this, exception);
    }
    if (targetLanguagePattern != null && targetLanguagePattern.length() > 0) {
      try {
       Pattern.compile(targetLanguagePattern);
      } catch(PatternSyntaxException x) {
        throw new InvalidConfigurationException(
          this, "Invalid Target Language \""+targetLanguagePattern+"\": " + x.getMessage());
      }
    }
    if (strip == null) strip = "";
      
    // does the outputLayer need to be added to the schema?
    Layer tagLayer = schema.getLayer(tagLayerId);
    if (tagLayer == null) {
      schema.addLayer(
        new Layer(tagLayerId)
        .setAlignment(Constants.ALIGNMENT_NONE)
        .setPeers(!firstVariantOnly)
        .setParentId(schema.getWordLayerId()));
    } else {
      if (tagLayerId.equals(tokenLayerId)
          || tagLayerId.equals(transcriptLanguageLayerId)
          || tagLayerId.equals(phraseLanguageLayerId)) {
        throw new InvalidConfigurationException(
          this, "Invalid tag layer: " + tagLayerId);
      }
      if (!tagLayer.getPeers() && !firstVariantOnly) {
        setStatus(
          "Output tag layer " + tagLayerId
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
    if (tagLayerId == null)
      throw new InvalidConfigurationException(this, "Output tag layer not set.");
    return new String[] { tagLayerId };
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
      setStatus("Tagging " + graph.getId());
         
      Layer tokenLayer = graph.getSchema().getLayer(tokenLayerId);
      if (tokenLayer == null) {
        throw new InvalidConfigurationException(
          this, "Invalid input token layer: " + tokenLayerId);
      }
      Layer tagLayer = graph.getSchema().getLayer(tagLayerId);
      if (tagLayer == null) {
        throw new InvalidConfigurationException(
          this, "Invalid output tag layer: " + tagLayerId);
      }
         
      // what languages are in the transcript?
      boolean transcriptIsMainlyTargetLang = true;
      if (transcriptLanguageLayerId != null) {
        Annotation transcriptLanguage = graph.first(transcriptLanguageLayerId);
        if (transcriptLanguage != null) {
          if (!transcriptLanguage.getLabel().matches(targetLanguagePattern)) { // not TargetLang
            transcriptIsMainlyTargetLang = false;
          }
        }
      }
      boolean thereArePhraseTags = false;
      if (phraseLanguageLayerId != null) {
        if (graph.first(phraseLanguageLayerId) != null) {
          thereArePhraseTags = true;
        }
      }

      TreeMap<String,Vector<Annotation>> toAnnotate = new TreeMap<String,Vector<Annotation>>();
      // should we just tag everything?
      if (transcriptIsMainlyTargetLang && !thereArePhraseTags) {
        // process all tokens
        for (Annotation token : graph.all(tokenLayerId)) {
          // tag only tokens that are not already tagged
          if (token.first(tagLayerId) == null) { // not tagged yet
            registorForAnnotation(token, toAnnotate);
          } // not tagged yet
        } // next token
      } else if (transcriptIsMainlyTargetLang) {
        // process all but the phrase-tagged tokens
            
        // tag the exceptions
        for (Annotation phrase : graph.all(phraseLanguageLayerId)) {
          if (!phrase.getLabel().matches(targetLanguagePattern)) { // not TargetLang
            for (Annotation token : phrase.all(tokenLayerId)) {
              // mark the token as an exception
              token.put("@notTargetLang", Boolean.TRUE);
            } // next token in the phrase
          } // non-TargetLang phrase
        } // next phrase
            
        for (Annotation token : graph.all(tokenLayerId)) {
          if (token.containsKey("@notTargetLang")) {
            // while we're here, we remove the @notTargetLang mark
            token.remove("@notTargetLang");
          } else { // TargetLang, so tag it
            // tag only tokens that are not already tagged
            if (token.first(tagLayerId) == null) { // not tagged yet
            registorForAnnotation(token, toAnnotate);
            } // not tagged yet
          } // TargetLang, so tag it
        } // next token
      } else if (thereArePhraseTags) {
        // process only the tokens phrase-tagged as TargetLang
        for (Annotation phrase : graph.all(phraseLanguageLayerId)) {
          if (phrase.getLabel().matches(targetLanguagePattern)) {
            for (Annotation token : phrase.all(tokenLayerId)) {
              // tag only tokens that are not already tagged
              if (token.first(tagLayerId) == null) { // not tagged yet
                registorForAnnotation(token, toAnnotate);
              } // not tagged yet
            } // next token in the phrase
          } // TargetLang phrase
        } // next phrase
      } // thereArePhraseTags
         
      try {
        Dictionary dictionary = getDictionary(this.dictionary);
        try {
          int t = 0;
          int typeCount = toAnnotate.size();
          setPercentComplete(0);
          for (String type : toAnnotate.keySet()) { // for each type
            if (isCancelling()) break;
            boolean found = false;
            for (String entry : dictionary.lookup(type)) {
              
              if (!found) setStatus("Tagging: " + type); // (log this only once)
              found = true;
              if (strip.length() > 0) entry = entry.replaceAll("["+strip+"]","");              
              for (Annotation token : toAnnotate.get(type)) {
                token.createTag(tagLayerId, entry)
                  .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
              }
              
              // do we want the first entry only?
              if (firstVariantOnly) break;
              
            } // next entry
            setPercentComplete(++t * 100 / typeCount);
            
          } // next type
          if (!isCancelling()) setPercentComplete(100);
        } finally {
          dictionary.close();
        }
      } catch (DictionaryException x) {
        throw new TransformationException(this, x);
      }
      return graph;
    } finally {
      setRunning(false);
    }
  }
  
  /**
   * Registers a token for annotation.
   * @param annotation
   * @param 
   * @param toAnnotate
   */
  protected void registorForAnnotation(
    Annotation token, TreeMap<String,Vector<Annotation>> toAnnotate) {
    if (!toAnnotate.containsKey(token.getLabel())) {
      toAnnotate.put(token.getLabel(), new Vector<Annotation>());
    }
    toAnnotate.get(token.getLabel()).add(token);
  } // end of registorForAnnotation()
   
  /**
   * Lists the dictionaries implemented by this Annotator.
   * <p> This method can assume that the following methods have been previously called:
   * <ul>
   *  <li> {@link Annotator#setSchema(Schema)} </li>
   *  <li> {@link Annotator#setTaskParameters(String)} </li>
   *  <li> {@link Annotator#setWorkingDirectory(File)} (if applicable) </li>
   *  <li> {@link Annotator#rdbConnectionFactory(String,String,String)}
   *       (if applicable) </li>
   * </ul>
   * @return A (possibly empty) list of IDs of dictionaries.
   */
  public List<String> getDictionaryIds() {
    List<String> ids = new Vector<String>();
    try {
      Connection rdb = newConnection();      
      try {

        // for each lexicon
        PreparedStatement sql = rdb.prepareStatement(
          sqlx.apply("SELECT lexicon_id, name FROM "+getAnnotatorId()+"_lexicon ORDER BY name"));
        ResultSet rs = sql.executeQuery();
        while (rs.next()) {

          // get a list of all the fields
          String name = rs.getString("name");
          String lexiconTable = getAnnotatorId()+"_lexicon_"+rs.getInt("lexicon_id");
          PreparedStatement sqlLexicon = rdb.prepareStatement(
            sqlx.apply("SELECT * FROM "+lexiconTable+" LIMIT 1"));
          ResultSetMetaData rsmd = sqlLexicon.getMetaData();
          int numberOfColumns = rsmd.getColumnCount();
          Vector<String> fields = new Vector<String>();
          for (int c = 1; c <= numberOfColumns; c++) {
            String columnName = rsmd.getColumnName(c);
            if (!columnName.equalsIgnoreCase("serial")
                && !columnName.equalsIgnoreCase("supplemental")) {
              fields.add(columnName);
            }            
          } // next column
          sqlLexicon.close();

          // possible dictionaries are all pairs of fields in this lexicon
          for (String keyField : fields) {
            for (String valueField : fields) {
              if (!keyField.equals(valueField)) {
                // dictionary IDs are formatted "${lexicon}:${keyField}→${valueField}"
                ids.add(name+":"+keyField+"→"+valueField);
              }
            } // next valueField candidate
          } // next keyField candidate
          
        } // next lexicon
        
      } finally {
        try { rdb.close(); } catch(SQLException x) {}
      }      
    } catch (SQLException x) {
      setStatus("getDictionaryIds: "+x);
    }
    return ids;
  }
   
  /**
   * Gets the identified dictionary.
   * <p> This method can assume that the following methods have been previously called:
   * <ul>
   *  <li> {@link Annotator#setSchema(Schema)} </li>
   *  <li> {@link Annotator#setTaskParameters(String)} </li>
   *  <li> {@link Annotator#rdbConnectionFactory(ConnectionFactory)} </li>
   * </ul>
   * @return The identified dictionary.
   * @throws DictionaryException If the given dictionary doesn't exist.
   */
  public Dictionary getDictionary(String id) throws DictionaryException {
    try {
      // dictionary IDs are formatted "${lexicon}:${keyField}→${valueField}"
      Matcher idParser = Pattern.compile("^(?<lexicon>[^:]+):(?<keyField>.+)→(?<valueField>.+)$")
        .matcher(id);
      if (idParser.matches()) {
        return getDictionary(
          idParser.group("lexicon"), idParser.group("keyField"), idParser.group("valueField"));
      } else {
        throw new DictionaryException(null, "Malformed dictionary ID: " + id);
      }
    } catch (PatternSyntaxException sqlP) {
      throw new DictionaryException(null, sqlP);
    }
  }
  
   /**
    * Creates a dictionary that maps the given key field to the given value field in the
    * given lexicon. 
    * @param lexicon
    * @param keyField
    * @param valueField
    * @return A dictionary that maps the given key field to the given value field in the
    * given lexicon. 
    * @throws DictionaryException
    */
   public Dictionary getDictionary(String lexicon, String keyField, String valueField)
     throws DictionaryException {
     try {
       return new FlatLexicon(this, newConnection(), sqlx, lexicon, keyField, valueField);
     } catch(SQLException sqlX) {
       throw new DictionaryException(null, sqlX);
     }
   } // end of getDictionary()


}
