//
// Copyright 2022-2026 New Zealand Institute of Language, Brain and Behaviour, 
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.stream.JsonParser;
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
import nzilbb.util.Timers;
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
  public String getMinimumApiVersion() { return "1.2.2"; }
     
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
      
      try {        
        // check the schema has been created
        // either of prepareStatement or executeQuery may fail if the table doesn't exist
        try (PreparedStatement sql = rdb.prepareStatement(
               sqlx.apply("SELECT lexicon_id, name FROM "+getAnnotatorId()
                          +"_lexicon ORDER BY name"))) {
          try (ResultSet rsCheck = sql.executeQuery()) {
          } // rsCheck.close()
        } // sql.close()
      } catch(SQLException exception) {
        
        try(PreparedStatement sql = rdb.prepareStatement(
              sqlx.apply(
                "CREATE TABLE "+getAnnotatorId()+"_lexicon ("
                +" lexicon_id INTEGER NOT NULL AUTO_INCREMENT"
                +" COMMENT 'ID which is appended to "
                +getAnnotatorId()+" to compute the lexicon table name',"
                +" name varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL"
                +" COMMENT 'Identifying name for the lexicon',"
                +" PRIMARY KEY (lexicon_id)"
                +") ENGINE=MyISAM;"))) {
          sql.executeUpdate();
        } // sql.close();
      }
      
      // create field definitions table if it doesn't exist
      // either of prepareStatement or executeQuery may fail if the table doesn't exist
      try {
        try (PreparedStatement sql = rdb.prepareStatement(
               sqlx.apply("SELECT lexicon_id FROM "+getAnnotatorId()+"_field"))) {
          try (ResultSet rsCheck = sql.executeQuery()) {
          } // rsCheck.close();
        } // sql.close();
      } catch(SQLException exception) { // no _field table
        
        try (PreparedStatement sql = rdb.prepareStatement(
               sqlx.apply(
                 "CREATE TABLE "+getAnnotatorId()+"_field ("
                 +" lexicon_id INTEGER NOT NULL"
                 +" COMMENT 'Lexicon ID',"
                 +" field varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL"
                 +" COMMENT 'Name of the field',"
                 +" type varchar(30) NOT NULL DEFAULT 'string'"
                 +" COMMENT 'Type of the field, e.g. string, text, richtext, number, etc.',"
                 +" validation varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT ''"
                 +" COMMENT 'Constraints on values',"
                 +" ordinal INTEGER NOT NULL DEFAULT 0"
                 +" COMMENT 'Determins order between fields',"
                 +" PRIMARY KEY (lexicon_id, field)"
                 +") ENGINE=MyISAM;"))) {
          sql.executeUpdate();
        } // sql.close();
      }
      
      // populate field definitions table where necessary
      try (PreparedStatement sqlLexiconFields = rdb.prepareStatement(
             sqlx.apply("SELECT * FROM "+getAnnotatorId()+"_field WHERE lexicon_id = ?"));
           PreparedStatement sqlInsertField = rdb.prepareStatement(
             sqlx.apply(
               "INSERT INTO "+getAnnotatorId()+"_field"
               +" (lexicon_id, field, type, validation, ordinal)"
               +" VALUES (?,?,'string','',?)"))) {
        try (PreparedStatement sqlLexicons = rdb.prepareStatement(
               sqlx.apply(
                 "SELECT lexicon_id, name FROM "+getAnnotatorId()+"_lexicon ORDER BY name"))) {
          try (ResultSet rsLexicons = sqlLexicons.executeQuery()) {
            while (rsLexicons.next()) {
              int lexiconId = rsLexicons.getInt("lexicon_id");
              sqlLexiconFields.setInt(1, lexiconId);
              sqlInsertField.setInt(1, lexiconId);
              try (ResultSet rsFields = sqlLexiconFields.executeQuery()) {
                if (!rsFields.next()) { // no fields defined, so define them
                  setStatus(
                    "Default field definitions for " + rsLexicons.getString("name"));
                  String sLexiconTable = getAnnotatorId()+"_lexicon_"+rsLexicons.getInt("lexicon_id");
                  try (PreparedStatement selectEntry = rdb.prepareStatement(
                         sqlx.apply("SELECT * FROM "+sLexiconTable+" LIMIT 1"))) {
                    try (ResultSet row = selectEntry.executeQuery()) {
                      if (row.next()) { // only the first entry
                        ResultSetMetaData meta = row.getMetaData();
                        for (int c = 1; c <= meta.getColumnCount(); c++) {
                          String f = meta.getColumnName(c);
                          if (!f.equalsIgnoreCase("serial")
                              && !f.equalsIgnoreCase("supplemental")) {
                            sqlInsertField.setString(2, meta.getColumnName(c));
                            sqlInsertField.setInt(3, c);
                            sqlInsertField.executeUpdate();
                          } // real field
                        } // next column
                      } // there is a row
                    } // row.close()
                  } // selectEntry.close()
                } // no fields defined
              } // rsFields.close()
            } // next lexicon
          } // rsLexicons.close()
        } // sqlLexicons.close()
      } // sqlLexiconFields.close()
      
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

        try (PreparedStatement sql = rdb.prepareStatement(
               sqlx.apply("SELECT lexicon_id, name FROM "
                          +getAnnotatorId()+"_lexicon ORDER BY name"))) {
          try (ResultSet rs = sql.executeQuery()) {
            while (rs.next()) {
              String sName = rs.getString("name");
              String sLexiconTable = getAnnotatorId()+"_lexicon_"+rs.getInt("lexicon_id");
              try (PreparedStatement deleteLexicon = rdb.prepareStatement(
                     sqlx.apply("DROP TABLE " + sLexiconTable))) {
                deleteLexicon.executeUpdate();
              } // deleteLexicon.close();
            } // next lexicon
          } // rs.close();
        } // sql.close();
        
        // drop fields table
        try (PreparedStatement sql = rdb.prepareStatement(
               sqlx.apply("DROP TABLE "+getAnnotatorId()+"_field"))) {
          sql.executeUpdate();
        } // sql.close();
        
        // drop lexicon table
        try (PreparedStatement sql = rdb.prepareStatement(
               sqlx.apply("DROP TABLE "+getAnnotatorId()+"_lexicon"))) {
          sql.executeUpdate();
        } // sql.close();
        
      } finally {
        try { rdb.close(); } catch(SQLException x) {}
      }      
    } catch (SQLException x) {
    }
  }
   
  /**
   * Takes a file to be used as a lexicon.
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
      lexicon, fieldDelimiter, quote, comment, fieldNames, "true".equalsIgnoreCase(skipFirstLine), file);
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

        // delete the field records if any
        try (PreparedStatement deleteLexicon = rdb.prepareStatement(
               sqlx.apply(
                 "DELETE FROM "+getAnnotatorId()+"_field WHERE lexicon_id = ?"))) {
          deleteLexicon.setInt(1, lexiconId);
          deleteLexicon.executeUpdate();
        } // deleteLexicon.close();
        
        // create lexicon table
        String sqlQuote = rdb.getMetaData().getIdentifierQuoteString();
        StringBuilder columnList = new StringBuilder();
        StringBuilder argumentList = new StringBuilder();
        StringBuilder columnDefinitions = new StringBuilder();
        Vector<String> columnIndexDefinitions = new Vector<String>();
        int columnCount = 0;
        String splitPattern = fieldDelimiter;
        if (splitPattern.equals("\\")) splitPattern = "\\\\";
        
        try(PreparedStatement sqlInsertField = rdb.prepareStatement(
             sqlx.apply(
               "INSERT INTO "+getAnnotatorId()+"_field"
               +" (lexicon_id, field, type, validation, ordinal)"
               +" VALUES (?,?,'string','',?)"))) {
          sqlInsertField.setInt(1, lexiconId);
          int c = 1;
          for (String column : fieldNames.split(splitPattern)) {
            if (column.length() == 0) column = "Field" + columnCount;
            columnList.append(", ").append(sqlQuote).append(column).append(sqlQuote);
            argumentList.append(",?");
            columnDefinitions.append(", ").append(sqlQuote).append(column).append(sqlQuote)
              .append(" varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL");
            columnIndexDefinitions.add(
              "CREATE INDEX IDX_"+getAnnotatorId()+"_"+lexiconId+"_"+(++columnCount)+" ON "
              +getAnnotatorId()+"_lexicon_"+lexiconId+" ("+sqlQuote+column+sqlQuote+")");
            
            // create/update field definition
            sqlInsertField.setString(2, column);
            sqlInsertField.setInt(3, c++);
            sqlInsertField.executeUpdate();
          
          } // next column
        } // sqlInsertField.close()
        
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
	BufferedReader reader = new BufferedReader(
	   new InputStreamReader(new FileInputStream(file), "UTF-8"));
	BufferedWriter writer = new BufferedWriter(
	   new OutputStreamWriter(new FileOutputStream(tempFile), "UTF-8"));
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
   * Determines the lexicon ID given the name.
   * @param rdb Database connection to use.
   * @param lexicon The name of the lexicon.
   * @return The lexicon ID, or -1 if there's no such lexicon.
   * @throws SQLException x
   */
  protected int lexiconId(Connection rdb, String lexicon) throws SQLException {
    try (PreparedStatement sql = rdb.prepareStatement(
           sqlx.apply(
             "SELECT lexicon_id FROM "+getAnnotatorId()+"_lexicon WHERE name = ?"))) {
      sql.setString(1, lexicon);
      try (ResultSet rs = sql.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("lexicon_id");
        }
      } // rs.close()
    } // sql.close()
    return -1;
  } // end of lexiconId()
  
  /**
   * Deletes the given lexicon.
   * @param lexicon
   * @return An error message, if any, or an empty string if not.
   */
  public String deleteLexicon(String lexicon) throws SQLException {
    try (Connection rdb = newConnection()) {
      // find the lexicon
      int lexiconId = lexiconId(rdb, lexicon);
      if (lexiconId >= 0) {            
        // drop the lexicon table
        String sLexiconTable = getAnnotatorId()+"_lexicon_"+lexiconId;
        String dropError = "";
        try (PreparedStatement deleteLexicon = rdb.prepareStatement(
               sqlx.apply("DROP TABLE " + sLexiconTable))) {
          deleteLexicon.executeUpdate();
        } catch(SQLException exception) {
          dropError = exception.getMessage();
        } // deleteLexicon.close();
        
        // delete the field records
        try (PreparedStatement deleteLexicon = rdb.prepareStatement(
               sqlx.apply(
                 "DELETE FROM "+getAnnotatorId()+"_field WHERE lexicon_id = ?"))) {
          deleteLexicon.setInt(1, lexiconId);
          deleteLexicon.executeUpdate();
        } // deleteLexicon.close();
        
        // delete the lexicon record
        try (PreparedStatement deleteLexicon = rdb.prepareStatement(
               sqlx.apply(
                 "DELETE FROM "+getAnnotatorId()+"_lexicon WHERE lexicon_id = ?"))) {
          deleteLexicon.setInt(1, lexiconId);
          deleteLexicon.executeUpdate();
        } // deleteLexicon.close();
        return dropError;
      } else {
        return "Nonexistent lexicon: " + lexicon;
      }
    }
  } // end of deleteLexicon()
  
  /**
   * Create a new field in the given lexicon, of type 'string'.
   * @param lexicon The lexicon ID.
   * @param field The name of the field.
   * @return An error message if creation failed. Otherwise, an empty string.
   */
  public String createField(String lexicon, String field) throws SQLException {
    return createField(lexicon, field, "string", "");
  }
  
  /**
   * Create a new field in the given lexicon.
   * @param lexicon The lexicon ID.
   * @param field The name of the field.
   * @param type The field type.
   * @param validation The constraints on the field values.
   * @return An error message if creation failed. Otherwise, an empty string.
   */
  public String createField(
    String lexicon, String field, String type, String validation)
    throws SQLException {
    try (Connection rdb = newConnection()) {
      // find the lexicon
      int lexiconId = lexiconId(rdb, lexicon);
      if (lexiconId >= 0) {
        if (field == null || field.trim().length() == 0) {
          return "No field name specified.";
        }
        try (PreparedStatement sqlUnique = rdb.prepareStatement(
               sqlx.apply(
                 "SELECT * FROM "+getAnnotatorId()+"_field"
                 +" WHERE lexicon_id = ? AND field = ?"))) {
          sqlUnique.setInt(1, lexiconId);
          sqlUnique.setString(2, field);
          try (ResultSet rsUnique = sqlUnique.executeQuery()) {
            if (rsUnique.next()) {
              return "There is already a field called " + field;
            }
          }
        }
        int ordinal = 1;
        try (PreparedStatement sqlOrdinal = rdb.prepareStatement(
               sqlx.apply(
                 "SELECT MAX(ordinal) FROM "+getAnnotatorId()+"_field WHERE lexicon_id = ?"))) {
          sqlOrdinal.setInt(1, lexiconId);
          try (ResultSet rsOrdinal = sqlOrdinal.executeQuery()) {
            if (rsOrdinal.next()) {
              ordinal = rsOrdinal.getInt(1) + 1;
            }
          }
        }
        try (PreparedStatement sqlInsertFieldRow = rdb.prepareStatement(
               sqlx.apply(
                 "INSERT INTO "+getAnnotatorId()+"_field" // TODO ordinal
                 +" (lexicon_id, field, type, validation, ordinal) VALUES (?,?,?,?,?)"))) {
          sqlInsertFieldRow.setInt(1, lexiconId);
          sqlInsertFieldRow.setString(2, field);
          sqlInsertFieldRow.setString(3, type);
          sqlInsertFieldRow.setString(4, validation);
          sqlInsertFieldRow.setInt(5, ordinal);
          sqlInsertFieldRow.executeUpdate();

          // insert field row succeeded, so add the field to the table
          try (PreparedStatement sqlAlterTable = rdb.prepareStatement(
                 sqlx.apply(
                   "ALTER TABLE "+getAnnotatorId() +"_lexicon_"+lexiconId
                   +" ADD COLUMN `"+field+"` varchar(200)"
                   +" CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL"))) {
            sqlAlterTable.executeUpdate();
          } catch (SQLException alterX) { // ALTER TABLE failed
            // delete the field from the table
            try (PreparedStatement sqlDeleteFieldRow = rdb.prepareStatement(
                   sqlx.apply(
                     "DELETE FROM "+getAnnotatorId()+"_field"
                     +" WHERE lexicon_id = ? AND field = ?"))) {
              sqlDeleteFieldRow.setInt(1, lexiconId);
              sqlDeleteFieldRow.setString(2, field);
              sqlDeleteFieldRow.executeUpdate();
            } // sqlDeleteFieldRow.close();            
            return alterX.toString();
          } // sqlAlterTable.close()          
        } catch (SQLException x) { // field is already there
          return x.toString();
        } // sqlInsertFieldRow.close()
      } else {
        return "Nonexistent lexicon: " + lexicon;
      }
    } catch (Exception x) {
      System.err.println("FlatLexiconTagger.createField("+lexicon+", "+field+"): " + x);
      return x.toString();
    } // rdb.close()
    return "";
  } // end of createField()

  /**
   * Get all field definitions for the given lexicon.
   * @param lexicon The lexicon ID.
   * @return A list of field definitions for the lexicon.
   */
  public List<Map<String,String>> readFields(String lexicon)
   throws SQLException {
    List<Map<String,String>> fields = new Vector<Map<String,String>>();
    try (Connection rdb = newConnection()) {
      // find the lexicon
      int lexiconId = lexiconId(rdb, lexicon);
      if (lexiconId >= 0) {            
        // list the lexicon fields
        try (PreparedStatement sqlFields = rdb.prepareStatement(
               sqlx.apply(
                 "SELECT * FROM "+getAnnotatorId()+"_field"
                 +" WHERE lexicon_id = ? ORDER BY ordinal"))) {
          sqlFields.setInt(1, lexiconId);
          try (ResultSet rsFields = sqlFields.executeQuery()) {
            while (rsFields.next()) {
              LinkedHashMap<String,String> field = new LinkedHashMap<String,String>();
              field.put("field", rsFields.getString("field"));
              field.put("type", rsFields.getString("type"));
              field.put("validation", rsFields.getString("validation"));
              fields.add(field);
            } // next field
          } // rsFields.close()
        } // sqlFields.close()
      } else {
        System.err.println(
          "FlatLexiconTagger.readFields("+lexicon+"): Nonexistent lexicon.");
        fields.add(new LinkedHashMap<String,String>(){{
          put("FlatLexiconTagger_error", "Nonexistent lexicon: " + lexicon);
        }});
      }
    } catch (Exception x) {
      System.err.println("FlatLexiconTagger.readFields("+lexicon+"): " + x);
      fields.add(new LinkedHashMap<String,String>(){{
        put("FlatLexiconTagger_error", x.toString());
      }});      
    } // rdb.close()
    return fields;
  } // end of readFields()

  /**
   * Update an existing field in the given lexicon.
   * @param lexicon The lexicon ID.
   * @param field The name of the field.
   * @param type The field type.
   * @return An error message if update failed. Otherwise, an empty string.
   */
  public String updateField(String lexicon, String field, String type) throws SQLException {
    return updateField(lexicon, field, type, "");
  }
  /**
   * Update an existing field in the given lexicon.
   * @param lexicon The lexicon ID.
   * @param field The name of the field.
   * @param type The field type.
   * @param validation The constraints on the field values.
   * @return An error message if update failed. Otherwise, an empty string.
   */
  public String updateField(
    String lexicon, String field, String type, String validation)
    throws SQLException {
    try (Connection rdb = newConnection()) {
      // find the lexicon
      int lexiconId = lexiconId(rdb, lexicon);
      if (lexiconId >= 0) {
        try (PreparedStatement sqlUpdateFieldRow = rdb.prepareStatement(
               sqlx.apply(
                 "UPDATE "+getAnnotatorId()+"_field" // TODO ordinal
                 +" SET type = ?, validation = ? WHERE lexicon_id = ? AND field = ?"))) {
          sqlUpdateFieldRow.setString(1, type);
          sqlUpdateFieldRow.setString(2, validation);
          sqlUpdateFieldRow.setInt(3, lexiconId);
          sqlUpdateFieldRow.setString(4, field);
          if (sqlUpdateFieldRow.executeUpdate() == 0) {
            return "No such field \""+field+"\" in " + lexicon;
          }
        } catch (SQLException x) { // field is already there
          return x.toString();
        } // sqlInsertFieldRow.close()
      } else {
        return "Nonexistent lexicon: " + lexicon;
      }
    } catch (Exception x) {
      System.err.println("FlatLexiconTagger.createField("+lexicon+", "+field+"): " + x);
      return x.toString();
    } // rdb.close()
    return "";
  } // end of updateField()

  /**
   * Delete an existing field in the given lexicon.
   * @param lexicon The lexicon ID.
   * @param field The name of the field.
   * @return An error message if deletion failed. Otherwise, an empty string.
   */
  public String deleteField(String lexicon, String field) throws SQLException {
    try (Connection rdb = newConnection()) {
      // find the lexicon
      int lexiconId = lexiconId(rdb, lexicon);
      if (lexiconId >= 0) {
        try (PreparedStatement sqlDeleteFieldRow = rdb.prepareStatement(
               sqlx.apply(
                 "DELETE FROM "+getAnnotatorId()+"_field"
                 +" WHERE lexicon_id = ? AND field = ?"))) {
          sqlDeleteFieldRow.setInt(1, lexiconId);
          sqlDeleteFieldRow.setString(2, field);
          if (sqlDeleteFieldRow.executeUpdate() == 0) {
            return "No such field \""+field+"\" in " + lexicon;
          } else {
            // delete field row succeeded, so drop the field from the table
            try (PreparedStatement sqlAlterTable = rdb.prepareStatement(
                   sqlx.apply(
                     "ALTER TABLE "+getAnnotatorId() +"_lexicon_"+lexiconId
                     +" DROP COLUMN `"+field+"`"))) {
              sqlAlterTable.executeUpdate();
            } catch (SQLException alterX) { // ALTER TABLE failed
              return alterX.toString();
            } // sqlAlterTable.close()
          } // field existed
        } catch (SQLException x) { // field is already there
          return x.toString();
        } // sqlInsertFieldRow.close()
      } else {
        return "Nonexistent lexicon: " + lexicon;
      }
    } catch (Exception x) {
      System.err.println("FlatLexiconTagger.createField("+lexicon+", "+field+"): " + x);
      return x.toString();
    } // rdb.close()
    return "";
  } // end of deleteField()

  /**
   * Get all values for the given entry.
   * @param lexicon The lexicon ID.
   * @param field The field used to identify the entry.
   * @param entry The value for <var>field</var>, which identifies the
   * entry to return.
   * @return The keys/values for the entry.
   */
  public Map<String,String> readEntry(String lexicon, String field, String entry)
   throws SQLException {
    try (Connection rdb = newConnection()) {
      // find the lexicon
      int lexiconId = lexiconId(rdb, lexicon);
      if (lexiconId >= 0) {            
        String sLexiconTable = getAnnotatorId()+"_lexicon_"+lexiconId;
        try {
          LinkedHashMap<String,String> values = new LinkedHashMap<String,String>();
          String s = sqlx.apply(
            "SELECT * FROM "+sLexiconTable+" WHERE `"+field.replace(';',' ')+"` = ?");
          try (PreparedStatement selectEntry = rdb.prepareStatement(s)) {
            selectEntry.setString(1, entry);
            try (ResultSet fields = selectEntry.executeQuery()) {
              if (fields.next()) { // only the first entry, sorry
                ResultSetMetaData meta = fields.getMetaData();
                for (int c = 1; c <= meta.getColumnCount(); c++) {
                  String f = meta.getColumnName(c);
                  if (!f.equalsIgnoreCase("serial")
                      && !f.equalsIgnoreCase("supplemental")) {
                    values.put(f, fields.getString(f));
                  }
                } // next column
              } else {
                return new LinkedHashMap<String,String>(){{
                  put("FlatLexiconTagger_error", "No entry: " + entry);
                }};
              }
            } // fields.close()
          } // selectEntry.close()
          return values;
        } catch(SQLException exception) {
          System.err.println(
            "FlatLexiconTagger.getEntry("+lexicon+", "+field+", "+entry+"): "+exception);
          return new LinkedHashMap<String,String>(){{
            put("FlatLexiconTagger_error", exception.getMessage());
          }};
        }
      } else {
        System.err.println(
          "FlatLexiconTagger.getEntry("+lexicon+", "+field+", "+entry
          +"): Nonexistent lexicon.");
        return new LinkedHashMap<String,String>(){{
          put("FlatLexiconTagger_error", "Nonexistent lexicon: " + lexicon);
        }};
      }
    } // rdb.close()
  } // end of getEntry()
  
  /**
   * Get all values for the given entry.
   * @param lexicon The lexicon ID.
   * @param field The field used to identify the entry.
   * @param entry The value for <var>field</var>, which identifies the
   * entry to update.
   * @param data JSON representation of the entry's new attribute values.
   * @return The keys/values for the entry.
   */
  public Map<String,String> updateEntry(
    String lexicon, String field, String entry, String data) throws SQLException {
    JsonObject json = Json.createReader(new StringReader(data)).readObject();
    try (Connection rdb = newConnection()) {
      // find the lexicon
      int lexiconId = lexiconId(rdb, lexicon);
      if (lexiconId >= 0) {            
        // drop the lexicon table
        String sLexiconTable = getAnnotatorId()+"_lexicon_"+lexiconId;
        try {
          LinkedHashMap<String,String> values = new LinkedHashMap<String,String>();
          String s = sqlx.apply(
            "SELECT * FROM "+sLexiconTable+" WHERE `"+field.replace(';',' ')+"` = ?");
          try (PreparedStatement selectEntry = rdb.prepareStatement(s)) {
            selectEntry.setString(1, entry);
            String update = "";
            try (ResultSet fields = selectEntry.executeQuery()) {
              boolean changed = false;
              Vector<String> parameters = new Vector<String>();
              if (fields.next()) { // only the first entry, sorry
                ResultSetMetaData meta = fields.getMetaData();
                for (int c = 1; c <= meta.getColumnCount(); c++) {
                  String f = meta.getColumnName(c);
                  if (!f.equalsIgnoreCase("serial")
                      && !f.equalsIgnoreCase("supplemental")) {
                    // can we update this field?
                    if (!f.equals(field) && json.containsKey(f)
                        // the value has actually changed
                        && !json.getString(f).equals(fields.getString(f))) {
                      if (update.length() == 0) {
                        update = "UPDATE "+sLexiconTable+" SET `"+f+"` = ?";
                      } else {
                        update += ", `"+f+"` = ?";
                      }
                      parameters.add(json.getString(f));
                      values.put(f, json.getString(f));
                      changed = true;
                    } else {
                      values.put(f, fields.getString(f));
                    }
                  }
                } // next column
                if (changed) { // some fields have new values
                  // save changes
                  update += " WHERE `"+field.replace(';',' ')+"` = ?";
                  parameters.add(entry);
                  try (PreparedStatement updateEntry = rdb.prepareStatement(
                         sqlx.apply(update))) {
                    for (int p = 0; p < parameters.size(); p++) {
                      updateEntry.setString(p+1, parameters.get(p));
                    } // next parameter
                    updateEntry.executeUpdate();
                  } // updateEntry                    
                } // changed
              } else {
                return new LinkedHashMap<String,String>(){{
                  put("FlatLexiconTagger_error", "No entry: " + entry);
                }};
              }
            } // fields.close()
          } // selectEntry.close()
          return values;
        } catch(SQLException exception) {
          System.err.println(
            "FlatLexiconTagger.getEntry("+lexicon+", "+field+", "+entry+"): "+exception);
          return new LinkedHashMap<String,String>(){{
            put("FlatLexiconTagger_error", exception.getMessage());
          }};
        }
      } else {
        System.err.println(
          "FlatLexiconTagger.getEntry("+lexicon+", "+field+", "+entry
          +"): Nonexistent lexicon.");
        return new LinkedHashMap<String,String>(){{
          put("FlatLexiconTagger_error", "Nonexistent lexicon: " + lexicon);
        }};
      }
    } // rdb.close()
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
  public FlatLexiconTagger setTargetLanguagePattern(String newTargetLanguagePattern) {
    if (newTargetLanguagePattern != null // empty string means null
        && newTargetLanguagePattern.trim().length() == 0) {
      newTargetLanguagePattern = null;
    }
    targetLanguagePattern = newTargetLanguagePattern;
    return this;
  }
  
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
   * Whether dictionary lookups are case/accent sensitive or not. By default, lookups are
   * case/accent insensitive. 
   * @see #getExactMatch()
   * @see #setExactMatch(Boolean)
   */
  protected Boolean exactMatch = Boolean.FALSE;
  /**
   * Getter for {@link #exactMatch}: Whether dictionary lookups are case/accent sensitive
   * or not. By default, lookups are case/accent insensitive. 
   * @return Whether dictionary lookups are case/accent sensitive or not. By default,
   * lookups are case/accent insensitive. 
   */
  public Boolean getExactMatch() { return exactMatch; }
  /**
   * Setter for {@link #exactMatch}: Whether dictionary lookups are case/accent sensitive
   * or not. 
   * @param newExactMatch Whether dictionary lookups are case/accent sensitive or not. 
   */
  public FlatLexiconTagger setExactMatch(Boolean newExactMatch) { exactMatch = newExactMatch; return this; }
  
  /**
   * Deprecated synonym for {@link #setExactMatch(Boolean)} for backwards compatibility.
   * @param newExactMatch Whether dictionary lookups are case/accent sensitive or not. 
   */
  @Deprecated
  public FlatLexiconTagger setCaseSensitive(Boolean newExactMatch) { return setExactMatch(newExactMatch); }
  
  /**
   * If annotations should include a link to the lexicon entry for
   * each token, this is the URL to use, where <q>{0}</q> will be
   * replaced by the lexicon, <q>{1}</q> will be replaced by the key
   * field name, <q>{2}</q> will be replaced by the key, <q>{3}</q>
   * will be replaced by the value field name, and <q>{4}</q> will be
   * replaced by the value. 
   * @see #getLexiconLink()
   * @see #setLexiconLink(String)
   */
  protected String lexiconLink;
  /**
   * Getter for {@link #lexiconLink}: If annotations should include a
   * link to the lexicon entry for each token, this is the URL to use,
   * where <q>{0}</q> will be replaced by the lexicon, <q>{1}</q> will
   * be replaced by the key field name, <q>{2}</q> will be replaced by
   * the key, <q>{3}</q> will be replaced by the value field name, and
   * <q>{4}</q> will be replaced by the value.
   * @return If annotations should include a link to the lexicon entry
   * for each token, this is the URL to use, where <q>{0}</q> will be
   * replaced by the lexicon, <q>{1}</q> will be replaced by the key
   * field name, <q>{2}</q> will be replaced by the key, <q>{3}</q>
   * will be replaced by the value field name, and <q>{4}</q> will be
   * replaced by the value. 
   */
  public String getLexiconLink() { return lexiconLink; }
  /**
   * Setter for {@link #lexiconLink}: If annotations should include a
   * link to the lexicon entry for each token, this is the URL to use,
   * where <q>{0}</q> will be replaced by the lexicon, <q>{1}</q> will
   * be replaced by the key field name, <q>{2}</q> will be replaced by
   * the key, <q>{3}</q> will be replaced by the value field name, and
   * <q>{4}</q> will be replaced by the value. 
   * @param newLexiconLink If annotations should include a link to the
   * lexicon entry for each token, this is the URL to use, where
   * <q>{0}</q> will be replaced by the lexicon, <q>{1}</q> will be
   * replaced by the key field name, <q>{2}</q> will be replaced by
   * the key, <q>{3}</q> will be replaced by the value field name, and
   * <q>{4}</q> will be replaced by the value.
   */
   public FlatLexiconTagger setLexiconLink(String newLexiconLink) { lexiconLink = newLexiconLink; return this; }
  
  /**
   * Sets the configuration for a given annotation task.
   * @param parameters The configuration of the annotator; a value of <tt> null </tt>
   * will apply the default task parameters, with {@link #tokenLayerId} set to the
   * {@link Schema#wordLayerId} and {@link #tokenLayerId} set to <q>phonemes</q>.
   * @throws InvalidConfigurationException
   */
  public void setTaskParameters(String parameters) throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");

    // default values:
    strip = "";
    targetLanguagePattern = null;
    firstVariantOnly = Boolean.FALSE;
    exactMatch = Boolean.FALSE;
    lexiconLink = null;

    if (parameters == null) { // apply default configuration
         
      if (schema.getLayer("orthography") != null) {
        tokenLayerId = "orthography";
      } else {
        tokenLayerId = schema.getWordLayerId();
      }
         
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
          "layer.parentId == schema.wordLayerId" // word tag
          +" && (/.*phoneme.*/.test(layer.id) || /.*pronunciation.*/.test(layer.id))");
        if (candidates.length > 0) {
          tagLayerId = candidates[0].getId();
        } else { // suggest adding a new one
          tagLayerId = "phonemes";
        }
        // default lexicon/keyField/valueField if possible
        List<String> dictionaries = getDictionaryIds();
        if (dictionaries.size() > 0) { // there are dictionaries
          // default to the first, which will be field1->field2
          dictionary = dictionaries.get(0);
        } // there are dictionaries
      } catch(ScriptException impossible) {}
         
    } else {
      beanPropertiesFromQueryString(parameters);
    }
    if (firstVariantOnly == null) firstVariantOnly = Boolean.FALSE;
    if (exactMatch == null) exactMatch = Boolean.FALSE;
    if (lexiconLink != null && lexiconLink.length() == 0) lexiconLink = null;

    Layer tokenLayer = schema.getLayer(tokenLayerId);
    if (tokenLayer == null)
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
    int tagLayerAlignment = Constants.ALIGNMENT_NONE;
    if (!tokenLayerId.equals(schema.getWordLayerId())) {
      // the tokens might be morphemes or sum sub-word element
      tagLayerAlignment = tokenLayer.getAlignment();
    }
    if (tagLayer == null) {
      tagLayer = schema.addLayer(
        new Layer(tagLayerId)
        .setAlignment(tagLayerAlignment)
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
      if (tagLayer.getAlignment() != tagLayerAlignment) {
        tagLayer.setAlignment(tagLayerAlignment);
      }
    }
    if (lexiconLink != null
        && !"text/url".equals(tagLayer.getType())) {
      tagLayer.setType("text/url");
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
      if (transcriptLanguageLayerId != null && targetLanguagePattern != null) {
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
          if (targetLanguagePattern != null
              && !phrase.getLabel().matches(targetLanguagePattern)) { // not TargetLang
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
          MessageFormat url = lexiconLink==null?null : new MessageFormat("\n"+lexiconLink);
          Matcher idParser = Pattern.compile(
            "^(?<lexicon>[^:]+):(?<keyField>.+)->(?<valueField>.+)$")
            .matcher(this.dictionary);
          String lexicon = null;
          String keyField = null;
          String valueField = null;
          if (idParser.matches()) {
            lexicon = idParser.group("lexicon");
            keyField = idParser.group("keyField");
            valueField = idParser.group("valueField");
          }
          for (String type : toAnnotate.keySet()) { // for each type
            if (isCancelling()) break;
            boolean found = false;
            for (String entry : dictionary.lookup(type)) {
              if (strip.length() > 0) {
                entry = entry.replaceAll(
                  // replace characters in this class
                  "["+strip
                  // ...escape ']' so it doesn't accidentally close the class
                  .replace("]","\\]") 
                  // also any trailing space in case phonemes are space-delimited,
                  // extra spaces aren't left behind
                  +"] *","");
              }
              if (entry.length() == 0) continue; // no blank labels

              String label = entry;
              if (url != null) {
                try {
                  label += url.format(new Object[] {
                      URLEncoder.encode(lexicon, "UTF-8"),
                      URLEncoder.encode(keyField, "UTF-8"),
                      URLEncoder.encode(type, "UTF-8"),
                      URLEncoder.encode(valueField, "UTF-8"),
                      URLEncoder.encode(entry, "UTF-8")
                    });
                } catch(java.io.UnsupportedEncodingException impossible) {
                }
              }
              
              if (!found) setStatus("Tagging: " + type); // (log this only once)
              found = true;
              for (Annotation token : toAnnotate.get(type)) {
                token.createTag(tagLayerId, label)
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
   * @param token
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
    Dictionary dictionary = getDictionary(this.dictionary);
    try {
      
      StringBuilder languageExpression = new StringBuilder();
      if (targetLanguagePattern != null
          && (phraseLanguageLayerId != null || transcriptLanguageLayerId != null)) {
        languageExpression.append(" && /").append(targetLanguagePattern).append("/.test(");
        if (phraseLanguageLayerId != null) {
          languageExpression.append("first('").append(esc(phraseLanguageLayerId))
            .append("').label");
          if (transcriptLanguageLayerId != null) {
            languageExpression.append(" ?? "); // add coalescing operator
          }
        }
        if (transcriptLanguageLayerId != null) {
          languageExpression.append("first('").append(esc(transcriptLanguageLayerId))
            .append("').label");
        }
        languageExpression.append(")");
      } // add language condition
      
      store.deleteMatchingAnnotations(
        "layerId = '"+esc(tagLayerId)+"'"
        +languageExpression
        +" && first('"+esc(tokenLayerId)+"').label "
        +(exactMatch?"===":"==") // === is slower, so we don't use it unless necessary
        +" '"+esc(sourceLabel)+"'");

      String tokenExpression = "layerId = '"+esc(tokenLayerId)+"'"
        +languageExpression
        +" && label "
        +(exactMatch?"===":"==") // === is slower, so we don't use it unless necessary
        +" '"+esc(sourceLabel)+"'";
      int count = 0;
      HashSet<String> soFar = new HashSet<String>(); // only unique entries
      MessageFormat url = lexiconLink==null?null : new MessageFormat("\n"+lexiconLink);
      Matcher idParser = Pattern.compile(
        "^(?<lexicon>[^:]+):(?<keyField>.+)->(?<valueField>.+)$").matcher(this.dictionary);
      String lexicon = null;
      String keyField = null;
      String valueField = null;
      if (idParser.matches()) {
        lexicon = idParser.group("lexicon");
        keyField = idParser.group("keyField");
        valueField = idParser.group("valueField");
      }
      for (String tag : dictionary.lookup(sourceLabel)) {
        if (strip.length() > 0) tag = tag.replaceAll("["+strip+"]","");
        if (tag.length() == 0) continue; // no blank labels
        if (!soFar.contains(tag)) { // duplicates are possible if stripSyllStress
          String label = tag;
          if (url != null) {
            try {
              label += url.format(new Object[]{
                  URLEncoder.encode(lexicon, "UTF-8"),
                  URLEncoder.encode(keyField, "UTF-8"),
                  URLEncoder.encode(sourceLabel, "UTF-8"),
                  URLEncoder.encode(valueField, "UTF-8"),
                  URLEncoder.encode(tag, "UTF-8")
                });
            } catch(java.io.UnsupportedEncodingException impossible) {
            }
          }
          store.tagMatchingAnnotations(
            tokenExpression, tagLayerId, label, Constants.CONFIDENCE_AUTOMATIC);
          soFar.add(tag);
          count++;
        }
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
    Timers timers = new Timers();
    try {
      setPercentComplete(0);
      Layer tokenLayer = schema.getLayer(tokenLayerId);
      if (tokenLayer == null) {
        throw new InvalidConfigurationException(
          this, "Invalid input token layer: " + tokenLayerId);
      }
      Layer tagLayer = schema.getLayer(tagLayerId);
      if (tagLayer == null) {
        throw new InvalidConfigurationException(
          this, "Invalid output tag layer: " + tagLayerId);
      }    
      
      StringBuilder labelExpression = new StringBuilder();
      labelExpression.append("layer.id == '").append(esc(tokenLayer.getId())).append("'");
      if (targetLanguagePattern != null
          && (phraseLanguageLayerId != null || transcriptLanguageLayerId != null)) {
        labelExpression.append(" && /").append(targetLanguagePattern).append("/.test(");
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
      // timers.start("store.aggregateMatchingAnnotations");
      String[] distinctWords = store.aggregateMatchingAnnotations(
        exactMatch?"DISTINCT BINARY":"DISTINCT", labelExpression.toString());
      // timers.end("store.aggregateMatchingAnnotations");
      setStatus("There are "+distinctWords.length+" distinct token labels");
      int w = 0;
      Dictionary dictionary = getDictionary(this.dictionary);
      // for each label
      MessageFormat url = lexiconLink==null?null : new MessageFormat("\n"+lexiconLink);
      Matcher idParser = Pattern.compile(
        "^(?<lexicon>[^:]+):(?<keyField>.+)->(?<valueField>.+)$").matcher(this.dictionary);
      String lexicon = null;
      String keyField = null;
      String valueField = null;
      if (idParser.matches()) {
        lexicon = idParser.group("lexicon");
        keyField = idParser.group("keyField");
        valueField = idParser.group("valueField");
      }
      for (String word : distinctWords) {
        setStatus(word+"...");
        if (isCancelling()) break;
        HashSet<String> soFar = new HashSet<String>(); // only unique entries
        // timers.start("dictionary.lookup");
        for (String tag : dictionary.lookup(word)) {
          // timers.end("dictionary.lookup");
          if (isCancelling()) break;
          if (strip.length() > 0) tag = tag.replaceAll("["+strip+"]","");
          if (tag.length() == 0) continue; // no blank labels
          if (!soFar.contains(tag)) { // duplicates are possible if stripSyllStress
            StringBuilder tokenExpression = new StringBuilder(labelExpression);
            tokenExpression.append(" && label ")
              .append(exactMatch?"===":"==") // === is slower, so we don't use it unless necessary
              .append(" '").append(esc(word)).append("'");
            setStatus(word+" → "+tag);
            // timers.start("store.tagMatchingAnnotations");
            String label = tag;
            if (url != null) {
              try {
                label += url.format(new Object[] {
                    URLEncoder.encode(lexicon, "UTF-8"),
                    URLEncoder.encode(keyField, "UTF-8"),
                    URLEncoder.encode(word, "UTF-8"),
                    URLEncoder.encode(valueField, "UTF-8"),
                    URLEncoder.encode(tag, "UTF-8")
                  });
              } catch(java.io.UnsupportedEncodingException impossible) {
              }
            }

            store.tagMatchingAnnotations(
              tokenExpression.toString(), tagLayerId, label, Constants.CONFIDENCE_AUTOMATIC);
            // timers.end("store.tagMatchingAnnotations");
            soFar.add(tag);
          }
          // timers.end("dictionary.lookup");
          setStatus(timers.toString());
          // do we want the first entry only?
          if (firstVariantOnly) break;        
        } // next entry
        setPercentComplete((++w * 100) / distinctWords.length);
      } // next word
      if (isCancelling()) {
        setStatus("Cancelled.");
      } else {
        setPercentComplete(100);
        setStatus("Finished.");
      }
    } catch(DictionaryException x) {
      setStatus(x.getMessage());
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
                // dictionary IDs are formatted "${lexicon}:${keyField}->${valueField}"
                ids.add(name+":"+keyField+"->"+valueField);
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
   *  <li> {@link Annotator#setRdbConnectionFactory(ConnectionFactory)} </li>
   * </ul>
   * @return The identified dictionary.
   * @throws DictionaryException If the given dictionary doesn't exist.
   */
  public Dictionary getDictionary(String id) throws DictionaryException {
    try {
      if (id == null) {
        id = this.dictionary;  
      }
      // dictionary IDs are formatted "${lexicon}:${keyField}->${valueField}"
      Matcher idParser = Pattern.compile(
        "^(?<lexicon>[^:]+):(?<keyField>.+)->(?<valueField>.+)$").matcher(id);
      if (idParser.matches()) {
        return getDictionary(
          idParser.group("lexicon"),
          idParser.group("keyField"), idParser.group("valueField"));
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
       return new FlatLexicon(this, newConnection(), sqlx, lexicon, keyField, valueField)
         .setExactMatch(exactMatch == null?Boolean.FALSE:exactMatch);
     } catch(SQLException sqlX) {
       throw new DictionaryException(null, sqlX);
     }
   } // end of getDictionary()


}
