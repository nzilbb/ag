//
// Copyright 2026 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.annotator.celexen;

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
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.script.ScriptException;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.ApiEndpoint;
import nzilbb.ag.automation.Dictionary;
import nzilbb.ag.automation.DictionaryException;
import nzilbb.ag.automation.ImplementsDictionaries;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.LabelBasedTagger;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.UsesRelationalDatabase;
import nzilbb.encoding.ValidLabelsDefinitions;
import nzilbb.sql.ConnectionFactory;
import nzilbb.sql.mysql.MySQLConnectionFactory;
import nzilbb.sql.derby.DerbyConnectionFactory;
import nzilbb.util.IO;

/**
 * Annotator that tags words with their lexical entries (pronunciations, frequency, etc.)
 * according to the 
 * <a href="https://catalog.ldc.upenn.edu/LDC96L14"> CELEX English lexicon </a>.
 *
 * <p> The annotator also supports automatically tagging
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
public class CELEXEnglishTagger extends LabelBasedTagger
  implements ImplementsDictionaries {
  /** Get the minimum version of the nzilbb.ag API supported by the annotator.*/
  public String getMinimumApiVersion() { return "2.0.0"; }
  
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
        System.err.println("CELEXEnglishTagger.openLog: " + t);
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
        System.err.println("CELEXEnglishTagger.closeLog: " + t);
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
   * The JDBC connect string for the lexicon relational database.
   * @see #getDbConnectString()
   * @see #setDbConnectString(String)
   */
  protected String dbConnectString;
  /**
   * Getter for {@link #dbConnectString}: The JDBC connect string for the lexicon
   * relational database.
   * @return The JDBC connect string for the lexicon relational database.
   */
  @ApiEndpoint("admin") public String getDbConnectString() { return dbConnectString; }
  /**
   * Setter for {@link #dbConnectString}: The JDBC connect string for the lexicon
   * relational database.
   * @param newDbConnectString The JDBC connect string for the lexicon relational database.
   */
  public CELEXEnglishTagger setDbConnectString(String newDbConnectString) { dbConnectString = newDbConnectString; return this; }

  /**
   * The username for connecting to the lexicon relational database.
   * @see #getDbUser()
   * @see #setDbUser(String)
   */
  protected String dbUser;
  /**
   * Getter for {@link #dbUser}: The username for connecting to the lexicon relational
   * database.
   * @return The username for connecting to the lexicon relational database.
   */
  @ApiEndpoint("admin") public String getDbUser() { return dbUser; }
  /**
   * Setter for {@link #dbUser}: The username for connecting to the lexicon relational
   * database.
   * @param newDbUser The username for connecting to the lexicon relational database.
   */
  public CELEXEnglishTagger setDbUser(String newDbUser) { dbUser = newDbUser; return this; }
  
  /**
   * The passwod for connecting to the lexicon relational database.
   * @see #getDbPassword()
   * @see #setDbPassword(String)
   */
  protected String dbPassword;
  /**
   * Getter for {@link #dbPassword}: The passwod for connecting to the lexicon relational
   * database.
   * @return The passwod for connecting to the lexicon relational database.
   */
  @ApiEndpoint("admin") public String getDbPassword() { return dbPassword; }
  /**
   * Setter for {@link #dbPassword}: The passwod for connecting to the lexicon relational
   * database.
   * @param newDbPassword The passwod for connecting to the lexicon relational database.
   */
  public CELEXEnglishTagger setDbPassword(String newDbPassword) { dbPassword = newDbPassword; return this; }
  
  /**
   * Connects to the lexicon relational database.
   * @return A connnected database connection.
   * @throws InvalidConfigurationException
   */
  public Connection newLexiconConnection() throws SQLException {
    // is the lexicon in a different database?
    if (dbConnectString != null && dbConnectString.length() > 0) {
      if (dbConnectString.indexOf("mysql") >= 0) {
        return new MySQLConnectionFactory(
          dbConnectString, dbUser, dbPassword).newConnection();
      } else {
        return new DerbyConnectionFactory(
          new File(dbConnectString)).newConnection();
      }
    } else {         
      return newConnection();
    }
  } // end of newLexiconConnection()
  
  /**
   * Takes a ZIP file containing the CELEX English lexicon files.
   * @param file The lexicon file.
   * @return null if upload was successful, an error message otherwise.
   */
  @ApiEndpoint("admin") public String uploadLexicon(File file) {
    if (!file.getName().endsWith(".zip") && !file.getName().endsWith(".ZIP")) {
      return "Must be a zip file containing all CELEX English lexicon files: "
        + file.getName();
    }
    File zip = new File(getWorkingDirectory(), "CELEX-EN.zip");
    try {
      IO.Rename(file, zip);
    } catch(IOException exception) {
      return "Could not copy " + file.getName() + ": " + exception.getMessage();
    }
    return null;
  } // end of uploadLexicon()
  
  /**
   * Determines whether the lexicon zip file has already been uploaded or not.
   * @return TRUE if there is a lexicon zip file present, FALSE otherwise.
   */
  @ApiEndpoint("admin") public Boolean lexiconFileExists() {
    return new File(getWorkingDirectory(), "CELEX-EN.zip").exists();
  } // end of lexiconFileExists()

  /**
   * Determines whether the lexicon database schema has already been created or not.
   * @return TRUE if the lexicon database tables are present, FALSE otherwise.
   */
  @ApiEndpoint("admin") public Boolean lexiconSchemaExists() {
    try (Connection rdb = newLexiconConnection()) {
      return countWordformMorphologyRecords(rdb) >= 0;
    } catch (Exception x) {
      return Boolean.FALSE;
    }
  } // end of lexiconFileExists()
  
  /**
   * Determines whether the lexicon data has already been imported or not.
   * @return TRUE if the lexicon data is present, FALSE otherwise.
   */
  @ApiEndpoint("admin") public Boolean lexiconDataExists() {
    try (Connection rdb = newLexiconConnection()) {
      return countWordformMorphologyRecords(rdb) > 0;
    } catch (Exception x) {
      return Boolean.FALSE;
    }
  } // end of lexiconFileExists()
  
  /**
   * Determines how many records there are in the cxen_wordformmorphology table,
   * i.e. the last table created and populated during installation.
   * @param rdb A connected database connection,
   * @return The number of records in the cxen_wordformmorphology table,
   * or -1 if the table table doesn't exist.
   */
  public int countWordformMorphologyRecords(Connection rdb) {
    try { // check existence of schema
      try (PreparedStatement sql = rdb.prepareStatement(
             sqlx.apply("SELECT COUNT(*) AS theCount FROM cxen_wordclass"))) {
        try (ResultSet rs = sql.executeQuery()) {
          rs.next();
          return rs.getInt(1);
        } // rs.close
      } // sql.close
    } catch (SQLException x) {
    }
    return -1;
  } // end of lexiconFileExists()
  
  /**
   * Runs a given SQL query for a given word, and returns the resulting matches.
   * @param word The word to use for the first SQL parameter.
   * @param sql The query to run.
   * @return The results of the query. Entries prefixed with "ERROR:" are error messages.
   */
  @ApiEndpoint("admin") public Collection<String> testSql(String word, String sql) {
    LinkedHashSet<String> results = new LinkedHashSet<String>();
    try {
      try (Connection rdb = newLexiconConnection()) {
        try (PreparedStatement sqlQuery = rdb.prepareStatement(
               sqlx.apply(sql))) {
          sqlQuery.setString(1, word);
          try (ResultSet rs = sqlQuery.executeQuery()) {
            while (rs.next()) {
              results.add(rs.getString(1));
            }            
          } // rs.close
        } // close sqlQuery
      } // close connection
    } catch (Throwable t) {
      results.add("ERROR: " + t);
    }
    return results;
  } // end of testSql()
   
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
      
      beanPropertiesFromQueryString(config);
      
      // has the dictionary data been added?
      try (Connection rdb = newLexiconConnection()) {

        int recordCount = countWordformMorphologyRecords(rdb);
        if (recordCount < 0 ) { // schema isn't created yet
          setStatus("Creating CELEX ENGLISH database schema...");
          
          // open schema file
          URL urlSchema = getClass().getResource("schema.sql");
          InputStream is = urlSchema.openStream();
          InputStreamReader isReader = new InputStreamReader(is);
          BufferedReader reader = new BufferedReader(isReader);
          
          // read file line by line
          String statement = "";
          String line = reader.readLine();
          while (line != null) {
            if (!(line.startsWith("/*") && line.endsWith("*/")) // not a comment line
                && line.trim().length() > 0) { // not a blank line
              statement += ("\n" + line.trim());
              if (statement.endsWith(";")) { // finished a statement, so execute it
                // setStatus("\""+statement+"\""); // TODO remove
                // setStatus(sqlx.apply(statement)); // TODO remove
                try (PreparedStatement sqlStatement = rdb.prepareStatement(
                       sqlx.apply(statement))) {
                  sqlStatement.executeUpdate();
                }
                statement = ""; // next statement
              }
            } // not skipping the line
            line = reader.readLine();
          } // next line
          if (statement.trim().length() > 0) { // execute last statement
            try (PreparedStatement sqlStatement = rdb.prepareStatement(
                   sqlx.apply(statement))) {
              sqlStatement.executeUpdate();
            }
          }          
          setStatus("CELEX ENGLISH database schema created.");
        }
        setPercentComplete(10);

        if (recordCount <= 0 ) { // lexicon isn't loaded yet
          install(rdb, new File(getWorkingDirectory(), "CELEX-EN.zip"));
        }
        
      } // rdb.close()
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
   * Installs the CELEX lexicon from the given zip file.
   * @param rdb An open connection to the lexicon database.
   * @param zip The file containing the lexicon files.
   * @throws SQLException If a database error occurs.
   * @throws IOException If the zip file could not be processed.
   */
  public void install(Connection rdb, File file) throws SQLException, IOException {
    if (!file.exists()) {
      throw new IOException("There is no lexicon zip file: " + file.getName());
    }
    setStatus("Starting data import from " + file.getName());

    try (ZipFile zip = new ZipFile(file)) {
      if (!isCancelling()) {
        setPercentComplete(10);
        processEOL(rdb, zip);
      }
      if (!isCancelling()) {
        setPercentComplete(20);
        processEPL(rdb, zip);
      }
      if (!isCancelling()) {
        setPercentComplete(30);
        processEML(rdb, zip);
      }
      if (!isCancelling()) {
        setPercentComplete(40);
        processEFL(rdb, zip);
      }
      if (!isCancelling()) {
        setPercentComplete(50);
        processESL(rdb, zip);
      }
      if (!isCancelling()) {
        setPercentComplete(60);
        processEOW(rdb, zip);
      }
      if (!isCancelling()) {
        setPercentComplete(70);
        processEPW(rdb, zip);
      }
      if (!isCancelling()) {
        setPercentComplete(80);
        processEFW(rdb, zip);
      }
      if (!isCancelling()) {
        setPercentComplete(90);
        processEMW(rdb, zip);
      }
      if (isCancelling()) {
        setStatus("Data import cancelled by user");
      } else {
        setPercentComplete(100);
        setStatus("Data import finished.");
      }
    } catch (NoSuchElementException x) {
      throw new IOException(x);
    }
    
  } // end of install()
  
  /**
   * Finds the ZipEntry with the given name in the given ZipFile.
   * @param zip The zip file
   * @param name The file name of the desired entry.
   * @return The entry.
   * @throws NoSuchElementException If the entry was not found.
   */
  public ZipEntry findEntry(ZipFile zip, String name) throws NoSuchElementException {
    return zip.stream()
      .filter(e -> e.getName().toLowerCase().endsWith(name.toLowerCase()))
      .findAny()
      .get();
  } // end of findEntry()
  
  /**
   * Process EOL file.
   */
  public void processEOL(Connection rdb, ZipFile zip)
    throws SQLException, IOException, NoSuchElementException {
    setStatus("Processing EOL...");
    ZipEntry eolFile = findEntry(zip, "EOL.CD");
    
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxen_lemma"))) {
      sqlDelete.execute();
    }
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxen_lemmaortho"))) {
      sqlDelete.execute();
    }

    try (PreparedStatement sqlInsertLemma = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxen_lemma VALUES (?,?,?)"));
         PreparedStatement sqlInsertLemmaOrtho = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxen_lemmaortho VALUES (?,?,?,?,?,?)"))) {
      try (BufferedReader reader = new BufferedReader(
             new InputStreamReader(zip.getInputStream(eolFile)))) {
        String sLine = reader.readLine();
        // process each line of the file
        while(sLine != null && !isCancelling()) {
          CELEXLine line = new CELEXLine(sLine);
          
          // main record
          //setStatus("EOL: " + line.getInt(1));
          sqlInsertLemma.setLong(1, line.getInt(1));
          sqlInsertLemma.setString(2, line.getString(2));
          sqlInsertLemma.setLong(3, line.getInt(3));
          sqlInsertLemma.executeUpdate();
          
          // first instance
          int iVariant = 1;
          sqlInsertLemmaOrtho.setLong(1, line.getInt(1)); // IdNumLemma
          sqlInsertLemmaOrtho.setInt(2, iVariant); // Variant
          sqlInsertLemmaOrtho.setString(3, line.getString(5)); // OrthoStatus
          sqlInsertLemmaOrtho.setLong(4, line.getInt(6)); // CobSpellFreq
          sqlInsertLemmaOrtho.setLong(5, line.getInt(7)); // CobSpellDev
          sqlInsertLemmaOrtho.setString(6, line.getString(8)); // HeadSylDia
          sqlInsertLemmaOrtho.executeUpdate();
          
          // if there's more than one
          int iVariantCount = (int)line.getInt(4);
          int iVariantOffset = 9;
          while (iVariantCount > iVariant) {
            iVariant++;
            
            sqlInsertLemmaOrtho.setInt(2, iVariant); // Variant
            sqlInsertLemmaOrtho.setString(3, line.getString(iVariantOffset)); // OrthoStatus
            sqlInsertLemmaOrtho.setLong(4, line.getInt(iVariantOffset + 1)); // CobSpellFreq
            sqlInsertLemmaOrtho.setLong(5, line.getInt(iVariantOffset + 2)); // CobSpellDev
            sqlInsertLemmaOrtho.setString(6, line.getString(iVariantOffset + 3)); // HeadSylDia
            sqlInsertLemmaOrtho.executeUpdate();
            
            iVariantOffset += 4;
          } // next variant
          
          sLine = reader.readLine();
        } // next line
      } // close reader
    } // close sqlInsertLemma and sqlInsertLemmaOrtho
    if (!isCancelling()) setStatus("EOL complete.");
  } // end of processEOL()

  /**
   * Process EPL file.
   */
  public void processEPL(Connection rdb, ZipFile zip)
    throws SQLException, IOException, NoSuchElementException
  {
    setStatus("Processing EPL...");
    ZipEntry eplFile = findEntry(zip, "EPL.CD");
    
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxen_lemmaphonology"))) {
      sqlDelete.execute();
    }
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxen_lemmaphonologypron"))) {
      sqlDelete.execute();
    }

    try (PreparedStatement sqlInsertLemmaPhonology = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxen_lemmaphonology VALUES (?,?)"));
         PreparedStatement sqlInsertLemmaPhonologyPron = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxen_lemmaphonologypron VALUES (?,?,?,?,?,?)"));
         BufferedReader reader = new BufferedReader(
           new InputStreamReader(zip.getInputStream(eplFile)))) {
      String sLine = reader.readLine();
      // process each line of the file
      while(sLine != null && !isCancelling()) {
        CELEXLine line = new CELEXLine(sLine);
        
        // main record
        //setStatus("EPL: " + line.getInt(1));
        sqlInsertLemmaPhonology.setLong(1, line.getInt(1)); // IdNumLemma
        sqlInsertLemmaPhonology.setLong(2, line.getInt(3)); // Cob
        sqlInsertLemmaPhonology.executeUpdate();
        
        // for possible-linking-r, where PhonStrsDISC ends in 'R', we split the prounciation
        // into two - the first without the trailing r, the second, with.
        // the implementation favours non-linking r by giving the with-r pronunciations
        // higher Variant numbers than the without-r pronunciations
        
        // first instance
        int iVariant = 1;
        sqlInsertLemmaPhonologyPron.setLong(1, line.getInt(1)); // IdNumLemma
        sqlInsertLemmaPhonologyPron.setInt(2, iVariant); // Variant
        sqlInsertLemmaPhonologyPron.setString(3, line.getString(5)); // PronStatus
        sqlInsertLemmaPhonologyPron.setString(4, line.getString(6) // PhonStrsDISC
                                              .replaceAll("R$","")); // strip off trailing R
        sqlInsertLemmaPhonologyPron.setString(5, line.getString(7)); // PhonCVBr
        sqlInsertLemmaPhonologyPron.setString(6, line.getString(8)); // PhonSylBCLX
        sqlInsertLemmaPhonologyPron.executeUpdate();
        
        if (line.getString(6).endsWith("R")) // possible linking r
        {
          sqlInsertLemmaPhonologyPron.setInt(2, iVariant + 1000); // higher Variant
          sqlInsertLemmaPhonologyPron.setString(4, line.getString(6) // PhonStrsDISC
                                                .replaceAll("R$","r")); // R->r
          sqlInsertLemmaPhonologyPron.executeUpdate();
        }
        
        // if there's more than one
        int iVariantCount = (int)line.getInt(4);
        int iVariantOffset = 9;
        while (iVariantCount > iVariant) {
          iVariant++;
          
          // some of the variant counts are wrong
          // e.g. IdNumLemma 2891 "Bahasa Indonesia" says 48 pronunciations, but there are 24
          if (line.getString(iVariantOffset) == null) break;
          
          sqlInsertLemmaPhonologyPron.setInt(2, iVariant); // Variant
          sqlInsertLemmaPhonologyPron.setString(
            3, line.getString(iVariantOffset)); // PronStatus
          sqlInsertLemmaPhonologyPron.setString(
            4, line.getString(iVariantOffset + 1) // PhonStrsDISC
            .replaceAll("R$","")); // strip off trailing R
          sqlInsertLemmaPhonologyPron.setString(
            5, line.getString(iVariantOffset + 2)); // PhonCVBr
          sqlInsertLemmaPhonologyPron.setString(
            6, line.getString(iVariantOffset + 3)); // PhonSylBCLX
          sqlInsertLemmaPhonologyPron.executeUpdate();
	       
          if (line.getString(iVariantOffset + 1).endsWith("R")) { // possible linking r
            sqlInsertLemmaPhonologyPron.setInt(2, iVariant + 1000); // higher Variant
            sqlInsertLemmaPhonologyPron.setString(
              4, line.getString(iVariantOffset + 1) // PhonStrsDISC
              .replaceAll("R$","r")); // R->r
            sqlInsertLemmaPhonologyPron.executeUpdate();
          }
          
          iVariantOffset += 4;
        } // next variant

        sLine = reader.readLine();
      } // next line
    } // close sqlInsertLemmaPhonology, sqlInsertLemmaPhonologyPron, reader
    if (!isCancelling()) setStatus("EPL complete.");
  } // end of processEPL()

  /**
   * Process EML file.
   */
  public void processEML(Connection rdb, ZipFile zip)
    throws SQLException, IOException, NoSuchElementException {
    setStatus("Processing EML...");
    ZipEntry emlFile = findEntry(zip, "EML.CD");
    
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxen_lemmamorphology"))) {
      sqlDelete.execute();
    }
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxen_lemmamorphologyparse"))) {
      sqlDelete.execute();
    }

    try (PreparedStatement sqlInsertLemmaMorphology = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxen_lemmamorphology VALUES (?,?,?,?)"));
         PreparedStatement sqlInsertLemmaMorphologyParse = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxen_lemmamorphologyparse"
                      +" VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"));	 
         BufferedReader reader = new BufferedReader(
           new InputStreamReader(zip.getInputStream(emlFile)))) {
      String sLine = reader.readLine();
      // process each line of the file
      while(sLine != null && !isCancelling()) {
        CELEXLine line = new CELEXLine(sLine);
        
        // main record
        //setStatus("EML: " + line.getInt(1));
        sqlInsertLemmaMorphology.setLong(1, line.getInt(1)); // IdNumLemma
        sqlInsertLemmaMorphology.setLong(2, line.getInt(3)); // Cob
        sqlInsertLemmaMorphology.setString(3, line.getString(4)); // MorphStatus
        sqlInsertLemmaMorphology.setString(4, line.getString(5)); // Lang
        sqlInsertLemmaMorphology.executeUpdate();
        
        // first instance
        int iVariant = 1;
        sqlInsertLemmaMorphologyParse.setLong(1, line.getInt(1)); // IdNumLemma
        sqlInsertLemmaMorphologyParse.setInt(2, iVariant); // Variant
        sqlInsertLemmaMorphologyParse.setString(3, line.getString(7)); // NVAffComp
        sqlInsertLemmaMorphologyParse.setString(4, line.getString(8)); // Der
        sqlInsertLemmaMorphologyParse.setString(5, line.getString(9)); // Comp
        sqlInsertLemmaMorphologyParse.setString(6, line.getString(10)); // DerComp
        sqlInsertLemmaMorphologyParse.setString(7, line.getString(11)); // Def
        sqlInsertLemmaMorphologyParse.setString(8, line.getString(12)); // Imm
        sqlInsertLemmaMorphologyParse.setString(9, line.getString(13)); // ImmSubCat
        sqlInsertLemmaMorphologyParse.setString(10, line.getString(14)); // ImmSA
        sqlInsertLemmaMorphologyParse.setString(11, line.getString(15)); // ImmAllo
        sqlInsertLemmaMorphologyParse.setString(12, line.getString(16)); // ImmSubst
        sqlInsertLemmaMorphologyParse.setString(13, line.getString(17)); // ImmOpac
        sqlInsertLemmaMorphologyParse.setString(14, line.getString(18)); // TransDer
        sqlInsertLemmaMorphologyParse.setString(15, line.getString(19)); // ImmInfix
        sqlInsertLemmaMorphologyParse.setString(16, line.getString(20)); // ImmRevers
        sqlInsertLemmaMorphologyParse.setString(17, line.getString(21)); // FlatSA
        sqlInsertLemmaMorphologyParse.setString(18, line.getString(22)); // StrucLab
        sqlInsertLemmaMorphologyParse.setString(19, line.getString(23)); // StrucAllo
        sqlInsertLemmaMorphologyParse.setString(20, line.getString(24)); // StrucSubst
        sqlInsertLemmaMorphologyParse.setString(21, line.getString(25)); // StrucOpac
        sqlInsertLemmaMorphologyParse.executeUpdate();
        
        // if there's more than one
        int iVariantCount = (int)line.getInt(6);
        int iVariantOffset = 26;
        while (iVariantCount > iVariant) {
          iVariant++;
          
          sqlInsertLemmaMorphologyParse.setInt(2, iVariant); // Variant
          sqlInsertLemmaMorphologyParse.setString(
            3, line.getString(iVariantOffset)); // NVAffComp
          sqlInsertLemmaMorphologyParse.setString(
            4, line.getString(iVariantOffset + 1)); // Der
          sqlInsertLemmaMorphologyParse.setString(
            5, line.getString(iVariantOffset + 2)); // Comp
          sqlInsertLemmaMorphologyParse.setString(
            6, line.getString(iVariantOffset + 3)); // DerComp
          sqlInsertLemmaMorphologyParse.setString(
            7, line.getString(iVariantOffset + 4)); // Def
          sqlInsertLemmaMorphologyParse.setString(
            8, line.getString(iVariantOffset + 5)); // Imm
          sqlInsertLemmaMorphologyParse.setString(
            9, line.getString(iVariantOffset + 6)); // ImmSubCat
          sqlInsertLemmaMorphologyParse.setString(
            10, line.getString(iVariantOffset + 7)); // ImmSA
          sqlInsertLemmaMorphologyParse.setString(
            11, line.getString(iVariantOffset + 8)); // ImmAllo
          sqlInsertLemmaMorphologyParse.setString(
            12, line.getString(iVariantOffset + 9)); // ImmSubst
          sqlInsertLemmaMorphologyParse.setString(
            13, line.getString(iVariantOffset + 10)); // ImmOpac
          sqlInsertLemmaMorphologyParse.setString(
            14, line.getString(iVariantOffset + 11)); // TransDer
          sqlInsertLemmaMorphologyParse.setString(
            15, line.getString(iVariantOffset + 12)); // ImmInfix
          sqlInsertLemmaMorphologyParse.setString(
            16, line.getString(iVariantOffset + 13)); // ImmRevers
          sqlInsertLemmaMorphologyParse.setString(
            17, line.getString(iVariantOffset + 14)); // FlatSA
          sqlInsertLemmaMorphologyParse.setString(
            18, line.getString(iVariantOffset + 15)); // StrucLab
          sqlInsertLemmaMorphologyParse.setString(
            19, line.getString(iVariantOffset + 16)); // StrucAllo
          sqlInsertLemmaMorphologyParse.setString(
            20, line.getString(iVariantOffset + 17)); // StrucSubst
          sqlInsertLemmaMorphologyParse.setString(
            21, line.getString(iVariantOffset + 18)); // StrucOpac
          sqlInsertLemmaMorphologyParse.executeUpdate();

          iVariantOffset += 19;
        } // next variant
        
        sLine = reader.readLine();
      } // next line
    } // close sqlInsertLemmaMorphology, sqlInsertLemmaMorphologyParse, reader
    if (!isCancelling()) setStatus("EML complete.");
  } // end of processEML()

  /**
   * Process EFL file.
   */
  public void processEFL(Connection rdb, ZipFile zip)
    throws SQLException, IOException, NoSuchElementException {
    setStatus("Processing EFL...");
    ZipEntry eflFile = findEntry(zip, "EFL.CD");
    
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxen_lemmafrequency"))) {
      sqlDelete.execute();
    }
    try (PreparedStatement sqlInsertLemmaFrequency = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxen_lemmafrequency VALUES (?,?,?,?,?,?,?,?,?,?,?)")); 
         BufferedReader reader = new BufferedReader(
           new InputStreamReader(zip.getInputStream(eflFile)))) {
      String sLine = reader.readLine();
      // process each line of the file
      while(sLine != null && !isCancelling()) {
        CELEXLine line = new CELEXLine(sLine);
        
        // main record
        //setStatus("EFL: " + line.getInt(1));
        sqlInsertLemmaFrequency.setLong(1, line.getInt(1)); // IdNumLemma
        sqlInsertLemmaFrequency.setLong(2, line.getInt(3)); // Cob
        sqlInsertLemmaFrequency.setString(3, line.getString(4)); // CobDev
        sqlInsertLemmaFrequency.setString(4, line.getString(5)); // CobMln
        sqlInsertLemmaFrequency.setString(5, line.getString(6)); // CobLog
        sqlInsertLemmaFrequency.setString(6, line.getString(7)); // CobW
        sqlInsertLemmaFrequency.setString(7, line.getString(8)); // CobWMln
        sqlInsertLemmaFrequency.setString(8, line.getString(9)); // CobWLog
        sqlInsertLemmaFrequency.setString(9, line.getString(10)); // CobS
        sqlInsertLemmaFrequency.setString(10, line.getString(11)); // CobSMln
        sqlInsertLemmaFrequency.setString(11, line.getString(12)); // CobSLog
        sqlInsertLemmaFrequency.executeUpdate();
        
        sLine = reader.readLine();
      } // next line
    } // close sqlInsertLemmaFrequency, reader
    if (!isCancelling()) setStatus("EPL complete.");
  } // end of processEPL()
  
  /**
   * Process ESL file.
   */
  public void processESL(Connection rdb, ZipFile zip)
    throws SQLException, IOException, NoSuchElementException {
    setStatus("Processing ESL...");
    ZipEntry eslFile = findEntry(zip, "ESL.CD");
    
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxen_lemmasyntax"))) {
      sqlDelete.execute();
    }

    try (PreparedStatement sqlInsertLemmaSyntax = rdb.prepareStatement(
           sqlx.apply(
             "INSERT INTO cxen_lemmasyntax VALUES"
             +" (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,"
             +"?,?,?,?,?,?,?,?,?,?,?,?,?,?)"));
         BufferedReader reader = new BufferedReader(
           new InputStreamReader(zip.getInputStream(eslFile)))) {
      String sLine = reader.readLine();
      // process each line of the file
      while(sLine != null && !isCancelling()) {
        CELEXLine line = new CELEXLine(sLine);
        
        // main record
        //setStatus("ESL: " + line.getInt(1));
        sqlInsertLemmaSyntax.setLong(1, line.getInt(1)); // IdNumLemma
        sqlInsertLemmaSyntax.setLong(2, line.getInt(3)); // Cob
        sqlInsertLemmaSyntax.setString(3, line.getString(4)); // ClassNum
        sqlInsertLemmaSyntax.setString(4, line.getString(5)); // C_N
        sqlInsertLemmaSyntax.setString(5, line.getString(6)); // Unc_N
        sqlInsertLemmaSyntax.setString(6, line.getString(7)); // Sing_N
        sqlInsertLemmaSyntax.setString(7, line.getString(8)); // Plu_N
        sqlInsertLemmaSyntax.setString(8, line.getString(9)); // GrC_N
        sqlInsertLemmaSyntax.setString(9, line.getString(10)); // GrUnc_N
        sqlInsertLemmaSyntax.setString(10, line.getString(11)); // Attr_N
        sqlInsertLemmaSyntax.setString(11, line.getString(12)); // PostPos_N
        sqlInsertLemmaSyntax.setString(12, line.getString(13)); // Voc_N
        sqlInsertLemmaSyntax.setString(13, line.getString(14)); // Proper_N
        sqlInsertLemmaSyntax.setString(14, line.getString(15)); // Exp_N
        sqlInsertLemmaSyntax.setString(15, line.getString(16)); // Trans_V
        sqlInsertLemmaSyntax.setString(16, line.getString(17)); // TransComp_V
        sqlInsertLemmaSyntax.setString(17, line.getString(18)); // Intrans_V
        sqlInsertLemmaSyntax.setString(18, line.getString(19)); // Ditrans_V
        sqlInsertLemmaSyntax.setString(19, line.getString(20)); // Link_V
        sqlInsertLemmaSyntax.setString(20, line.getString(21)); // Phr_V
        sqlInsertLemmaSyntax.setString(21, line.getString(22)); // Prep_V
        sqlInsertLemmaSyntax.setString(22, line.getString(23)); // PhrPrep_V
        sqlInsertLemmaSyntax.setString(23, line.getString(24)); // Exp_V
        sqlInsertLemmaSyntax.setString(24, line.getString(25)); // Ord_A
        sqlInsertLemmaSyntax.setString(25, line.getString(26)); // Attr_A
        sqlInsertLemmaSyntax.setString(26, line.getString(27)); // Pred_A
        sqlInsertLemmaSyntax.setString(27, line.getString(28)); // PostPos_A
        sqlInsertLemmaSyntax.setString(28, line.getString(29)); // Exp_A
        sqlInsertLemmaSyntax.setString(29, line.getString(30)); // Ord_ADV
        sqlInsertLemmaSyntax.setString(30, line.getString(31)); // Pred_ADV
        sqlInsertLemmaSyntax.setString(31, line.getString(32)); // PostPos_ADV
        sqlInsertLemmaSyntax.setString(32, line.getString(33)); // Comb_ADV
        sqlInsertLemmaSyntax.setString(33, line.getString(34)); // Exp_ADV
        sqlInsertLemmaSyntax.setString(34, line.getString(35)); // Card_NUM
        sqlInsertLemmaSyntax.setString(35, line.getString(36)); // Ord_NUM
        sqlInsertLemmaSyntax.setString(36, line.getString(37)); // Exp_NUM
        sqlInsertLemmaSyntax.setString(37, line.getString(38)); // Pers_PRON
        sqlInsertLemmaSyntax.setString(38, line.getString(39)); // Dem_PRON
        sqlInsertLemmaSyntax.setString(39, line.getString(40)); // Poss_PRON
        sqlInsertLemmaSyntax.setString(40, line.getString(41)); // Refl_PRON
        sqlInsertLemmaSyntax.setString(41, line.getString(42)); // Wh_PRON
        sqlInsertLemmaSyntax.setString(42, line.getString(43)); // Det_PRON
        sqlInsertLemmaSyntax.setString(43, line.getString(44)); // Pron_PRON
        sqlInsertLemmaSyntax.setString(44, line.getString(45)); // Exp_PRON
        sqlInsertLemmaSyntax.setString(45, line.getString(46)); // Cor_C
        sqlInsertLemmaSyntax.setString(46, line.getString(47)); // Sub_C
        sqlInsertLemmaSyntax.executeUpdate();
        
        sLine = reader.readLine();
      } // next line
    } // close sqlInsertLemmaSyntax, reader
    if (!isCancelling()) setStatus("ESL complete.");
  } // end of processESL()
  
  /**
   * Process EOW file.
   */
  public void processEOW(Connection rdb, ZipFile zip)
    throws SQLException, IOException, NoSuchElementException {
    setStatus("Processing EOW...");
    ZipEntry eowFile = findEntry(zip, "EOW.CD");
    
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxen_wordform"))) {
      sqlDelete.execute();
    }
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxen_wordformortho"))) {
      sqlDelete.execute();
    }
 
    try (PreparedStatement sqlInsertWordForm = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxen_wordform VALUES (?,?,?)"));
         PreparedStatement sqlInsertWordFormOrtho = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxen_wordformortho VALUES (?,?,?,?,?,?,?)"));
         BufferedReader reader = new BufferedReader(
           new InputStreamReader(zip.getInputStream(eowFile)))) {
      String sLine = reader.readLine();
      // process each line of the file
      while(sLine != null && !isCancelling()) {
        CELEXLine line = new CELEXLine(sLine);
        
        // main record
        //setStatus("EOW: " + line.getInt(1));
        sqlInsertWordForm.setLong(1, line.getInt(1)); // IdNum
        sqlInsertWordForm.setLong(2, line.getInt(4)); // IdNumLemma
        sqlInsertWordForm.setLong(3, line.getInt(3)); // Cob
        sqlInsertWordForm.executeUpdate();
        
        // first instance
        int iVariant = 1;
        sqlInsertWordFormOrtho.setLong(1, line.getInt(1)); // IdNum
        sqlInsertWordFormOrtho.setInt(2, iVariant); // Variant
        sqlInsertWordFormOrtho.setString(3, line.getString(2)); // WordDia
        sqlInsertWordFormOrtho.setString(4, line.getString(9)); // WordSylDia
        sqlInsertWordFormOrtho.setString(5, line.getString(6)); // OrthoStatus
        sqlInsertWordFormOrtho.setLong(6, line.getInt(7)); // CobSpellFreq
        sqlInsertWordFormOrtho.setLong(7, line.getInt(8)); // CobSpellDev
        sqlInsertWordFormOrtho.executeUpdate();
        
        // if there's more than one
        int iVariantCount = (int)line.getInt(5);
        int iVariantOffset = 10;
        while (iVariantCount > iVariant) {
          iVariant++;
          
          sqlInsertWordFormOrtho.setInt(2, iVariant); // Variant
          sqlInsertWordFormOrtho.setString(3, line.getString(iVariantOffset)); // WordDia
          sqlInsertWordFormOrtho.setString(
            4, line.getString(iVariantOffset + 4)); // WordSylDia
          sqlInsertWordFormOrtho.setString(
            5, line.getString(iVariantOffset + 1)); // OrthoStatus
          sqlInsertWordFormOrtho.setLong(
            6, line.getInt(iVariantOffset + 2)); // CobSpellFreq
          sqlInsertWordFormOrtho.setLong(
            7, line.getInt(iVariantOffset + 3)); // CobSpellDev
          sqlInsertWordFormOrtho.executeUpdate();
          
          iVariantOffset += 5;
        } // next variant
        
        sLine = reader.readLine();
      } // next line
    } // close sqlInsertWordForm, sqlInsertWordFormOrtho, reader
    if (!isCancelling()) setStatus("EOW complete.");
  } // end of processEOW()

  /**
   * Process EPW file.
   */
  public void processEPW(Connection rdb, ZipFile zip)
    throws SQLException, IOException, NoSuchElementException {
    setStatus("Processing EPW...");
    ZipEntry epwFile = findEntry(zip, "EPW.CD");
    
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxen_wordformphonology"))) {
      sqlDelete.execute();
    }
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxen_wordformphonologypron"))) {
      sqlDelete.execute();
    }

    try (PreparedStatement sqlInsertWordFormPhonology = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxen_wordformphonology VALUES (?,?)"));
         PreparedStatement sqlInsertWordFormPhonologyPron = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxen_wordformphonologypron VALUES (?,?,?,?,?,?)"));
         BufferedReader reader = new BufferedReader(
           new InputStreamReader(zip.getInputStream(epwFile)))) {
      String sLine = reader.readLine();
      // process each line of the file
      while(sLine != null && !isCancelling()) {
        CELEXLine line = new CELEXLine(sLine);
        
        // main record
        //setStatus("EPW: " + line.getInt(1));
        sqlInsertWordFormPhonology.setLong(1, line.getInt(1)); // IdNum
        sqlInsertWordFormPhonology.setLong(2, line.getInt(3)); // Cob
        sqlInsertWordFormPhonology.executeUpdate();
        
        // for possible-linking-r, where PhonStrsDISC ends in 'R', we split the prounciation
        // into two - the first without the trailing r, the second, with.
        // the implementation favours non-linking r by giving the with-r pronunciations
        // higher Variant numbers than the without-r pronunciations
        
        // first instance
        int iVariant = 1;
        sqlInsertWordFormPhonologyPron.setLong(1, line.getInt(1)); // IdNum
        sqlInsertWordFormPhonologyPron.setInt(2, iVariant); // Variant
        sqlInsertWordFormPhonologyPron.setString(3, line.getString(6)); // PronStatus
        sqlInsertWordFormPhonologyPron.setString(4, line.getString(7) // PhonStrsDISC
                                                 .replaceAll("R$","")); // strip off trailing R
        sqlInsertWordFormPhonologyPron.setString(5, line.getString(8)); // PhonCVBr
        sqlInsertWordFormPhonologyPron.setString(6, line.getString(9)); // PhonSylBCLX
        sqlInsertWordFormPhonologyPron.executeUpdate();
        
        if (line.getString(7).endsWith("R")) { // possible linking r
          sqlInsertWordFormPhonologyPron.setInt(2, iVariant + 1000); // higher Variant
          sqlInsertWordFormPhonologyPron.setString(4, line.getString(7) // PhonStrsDISC
                                                   .replaceAll("R$","r")); // R->r
          sqlInsertWordFormPhonologyPron.executeUpdate();
        }
        
        // if there's more than one
        int iVariantCount = (int)line.getInt(5);
        int iVariantOffset = 10;
        while (iVariantCount > iVariant) {
          iVariant++;
          
          // some of the variant counts are wrong
          // e.g. IdNum 5045 "Bahasa Indonesia" says 48 pronunciations, but there are 23
          if (line.getString(iVariantOffset) == null) break;
          
          sqlInsertWordFormPhonologyPron.setInt(2, iVariant); // Variant
          sqlInsertWordFormPhonologyPron.setString(
            3, line.getString(iVariantOffset)); // PronStatus
          sqlInsertWordFormPhonologyPron.setString(
            4, line.getString(iVariantOffset + 1) // PhonStrsDISC
            .replaceAll("R$","")); // strip off trailing R
          sqlInsertWordFormPhonologyPron.setString(
            5, line.getString(iVariantOffset + 2)); // PhonCVBr
          sqlInsertWordFormPhonologyPron.setString(
            6, line.getString(iVariantOffset + 3)); // PhonSylBCLX
          sqlInsertWordFormPhonologyPron.executeUpdate();
          
          if (line.getString(iVariantOffset + 1).endsWith("R")) { // possible linking r
            sqlInsertWordFormPhonologyPron.setInt(2, iVariant + 1000); // higher Variant
            sqlInsertWordFormPhonologyPron.setString(
              4, line.getString(iVariantOffset + 1) // PhonStrsDISC
                                                   .replaceAll("R$","r")); // R->r
            sqlInsertWordFormPhonologyPron.executeUpdate();
          }
          
          iVariantOffset += 4;
        } // next variant
        
        sLine = reader.readLine();
      } // next line
    } // close sqlInsertWordFormPhonology, sqlInsertWordFormPhonologyPron, reader
    if (!isCancelling()) setStatus("EPW complete.");
  } // end of processEPW()

  /**
   * Process EFW file.
   */
  public void processEFW(Connection rdb, ZipFile zip)
    throws SQLException, IOException, NoSuchElementException {
    setStatus("Processing EFW...");
    ZipEntry efwFile = findEntry(zip, "EFW.CD");
    
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxen_wordformfrequency"))) {
      sqlDelete.execute();
    }

    try (PreparedStatement sqlInsertWordFormFrequency = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxen_wordformfrequency VALUES (?,?,?,?,?,?,?,?,?,?,?)"));
         BufferedReader reader = new BufferedReader(
           new InputStreamReader(zip.getInputStream(efwFile)))) {
      String sLine = reader.readLine();
      // process each line of the file
      while(sLine != null && !isCancelling()) {
        CELEXLine line = new CELEXLine(sLine);
        
        // main record
        //setStatus("EFW: " + line.getInt(1));
        sqlInsertWordFormFrequency.setLong(1, line.getInt(1)); // IdNum
        sqlInsertWordFormFrequency.setLong(2, line.getInt(4)); // Cob
        sqlInsertWordFormFrequency.setString(3, line.getString(5)); // CobDev
        sqlInsertWordFormFrequency.setString(4, line.getString(6)); // CobMln
        sqlInsertWordFormFrequency.setString(5, line.getString(7)); // CobLog
        sqlInsertWordFormFrequency.setString(6, line.getString(8)); // CobW
        sqlInsertWordFormFrequency.setString(7, line.getString(9)); // CobWMln
        sqlInsertWordFormFrequency.setString(8, line.getString(10)); // CobWLog
        sqlInsertWordFormFrequency.setString(9, line.getString(11)); // CobS
        sqlInsertWordFormFrequency.setString(10, line.getString(12)); // CobSMln
        sqlInsertWordFormFrequency.setString(11, line.getString(13)); // CobSLog
        sqlInsertWordFormFrequency.executeUpdate();
        
        sLine = reader.readLine();
      } // next line
    } // close sqlInsertWordFormFrequency, reader
    if (!isCancelling()) setStatus("EFW complete.");
  } // end of processEFW()

  /**
   * Process EMW file.
   */
  public void processEMW(Connection rdb, ZipFile zip)
    throws SQLException, IOException, NoSuchElementException {
    setStatus("Processing EMW...");
    ZipEntry emwFile = findEntry(zip, "EMW.CD");
    
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxen_wordformmorphology"))) {
      sqlDelete.execute();
    }

    try (PreparedStatement sqlInsertWordFormMorphology = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxen_wordformmorphology VALUES (?,?,?,?)"));
         BufferedReader reader = new BufferedReader(
           new InputStreamReader(zip.getInputStream(emwFile)))) {
      String sLine = reader.readLine();
      // process each line of the file
      while(sLine != null && !isCancelling()) {
        CELEXLine line = new CELEXLine(sLine);
        
        // main record
        //setStatus("EMW: " + line.getInt(1));
        sqlInsertWordFormMorphology.setLong(1, line.getInt(1)); // IdNum
        sqlInsertWordFormMorphology.setLong(2, line.getInt(3)); // Cob
        sqlInsertWordFormMorphology.setString(3, line.getString(5)); // FlectType
        sqlInsertWordFormMorphology.setString(4, line.getString(6)); // TransInfl
        sqlInsertWordFormMorphology.executeUpdate();
        
        sLine = reader.readLine();
      } // next line
    } // close sqlInsertWordFormMorphology, reader
    if (!isCancelling()) setStatus("EFW complete.");
  } // end of processEFW()      

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
  public CELEXEnglishTagger setFirstVariantOnly(Boolean newFirstVariantOnly) {
    firstVariantOnly = newFirstVariantOnly; return this; }
  
  /**
   * SQL query for looking up the CELEX lexicon.
   * @see #getSql()
   * @see #setSql(String)
   */
  protected String sql;
  /**
   * Getter for {@link #sql}: SQL query for looking up the CELEX lexicon.
   * @return SQL query for looking up the CELEX lexicon.
   */
  public String getSql() { return sql; }
  /**
   * Setter for {@link #sql}: SQL query for looking up the CELEX lexicon.
   * @param newSql SQL query for looking up the CELEX lexicon.
   */
  public CELEXEnglishTagger setSql(String newSql) { sql = newSql; return this; }
  
  /**
   * Delimiter(s) for syllable recovery.
   * @see #getDelimiters()
   * @see #setDelimiters(String)
   */
  protected String delimiters; // TODO
  /**
   * Getter for {@link #delimiters}: Delimiter(s) for syllable recovery.
   * @return Delimiter(s) for syllable recovery.
   */
  public String getDelimiters() { return delimiters; }
  /**
   * Setter for {@link #delimiters}: Delimiter(s) for syllable recovery.
   * @param newDelimiters Delimiter(s) for syllable recovery.
   */
  public CELEXEnglishTagger setDelimiters(String newDelimiters) { delimiters = newDelimiters; return this; }

  /**
   * Sets the configuration for a given annotation task.
   * @param parameters The configuration of the annotator; a value of <tt> null </tt>
   * will apply the default task parameters, with {@link #tokenLayerId} set to the
   * {@link Schema#wordLayerId} and pronunciationLayerId set to <q>phonemes</q>.
   * @throws InvalidConfigurationException
   */
  public void setTaskParameters(String parameters) throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");
             
    if (parameters == null) { // there is no possible default parameter
      throw new InvalidConfigurationException(this, "Parameters not set.");         
    }

    // start with clean slate
    tokenLayerId = null;
    transcriptLanguageLayerId = null;
    phraseLanguageLayerId = null;
    targetLanguagePattern = null;
    tagLayerId = null;
    firstVariantOnly = Boolean.FALSE;
    sql = null;
        
    beanPropertiesFromQueryString(parameters);

    if (firstVariantOnly == null) firstVariantOnly = Boolean.FALSE;
    if ("".equals(delimiters)) delimiters = null;
    if (sql == null || sql.equals("")) 
      throw new InvalidConfigurationException(this, "No lexical query specified");
    
    if (schema.getLayer(tokenLayerId) == null)
      throw new InvalidConfigurationException(this, "Token layer not found: " + tokenLayerId);
    if (transcriptLanguageLayerId != null && schema.getLayer(transcriptLanguageLayerId) == null) 
      throw new InvalidConfigurationException(
        this, "Transcript language layer not found: " + transcriptLanguageLayerId);
    if (phraseLanguageLayerId != null && schema.getLayer(phraseLanguageLayerId) == null) 
      throw new InvalidConfigurationException(
        this, "Phrase language layer not found: " + phraseLanguageLayerId);
    if (targetLanguagePattern != null && targetLanguagePattern.length() > 0) {
      try {
        Pattern.compile(targetLanguagePattern);
      } catch(PatternSyntaxException x) {
        throw new InvalidConfigurationException(
          this, "Invalid Target Language \""+targetLanguagePattern+"\": " + x.getMessage());
      }
    }
      
    // does the outputLayer need to be added to the schema?
    Layer tagLayer = schema.getLayer(tagLayerId);
    if (tagLayer == null) {
      schema.addLayer(
        new Layer(tagLayerId)
        .setAlignment(delimiters == null?Constants.ALIGNMENT_NONE // lexical tagging
                      :Constants.ALIGNMENT_INTERVAL) // syllable recovery
        .setPeers(!firstVariantOnly || delimiters != null)
        .setParentId(schema.getWordLayerId()));
      tagLayer = schema.getLayer(tagLayerId);
    } else {
      if (tagLayerId.equals(tokenLayerId)
          || tagLayerId.equals(transcriptLanguageLayerId)
          || tagLayerId.equals(phraseLanguageLayerId)) {
        throw new InvalidConfigurationException(
          this, "Invalid pronunciation layer: " + tagLayerId);
      }
      if (!tagLayer.getPeers() && !firstVariantOnly && delimiters == null) {
        setStatus(
          "Pronunciation layer " + tagLayerId
          + " doesn't allow peer annotations; using first variant only.");
        firstVariantOnly = true;
      }
      if (tagLayer.getAlignment() != Constants.ALIGNMENT_NONE
          && delimiters != null) {
        tagLayer.setAlignment(Constants.ALIGNMENT_NONE);
      }
    }
    // set valid labels (they're actually valid label parts!)
    List<Map<String,Object>> validLabelsDefinition = new Vector<Map<String,Object>>();
    if (sql.indexOf("PhonStrsDISC") >= 0) {
      tagLayer.setType(Constants.TYPE_IPA);
      ValidLabelsDefinitions.AddDISCDefinitions(validLabelsDefinition);
    } 
    // for LaBB-CAT:
    tagLayer.put("validLabelsDefinition", validLabelsDefinition);
    // for general use
    tagLayer.setValidLabels(
      ValidLabelsDefinitions.ValidLabelsFromDefinition(validLabelsDefinition));
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
      throw new InvalidConfigurationException(this, "Pronunciation layer not set.");
    return new String[] { tagLayerId };
  }
  
  /**
   * Getter for {@link #taggingDictionary}: A dictionary that might be
   * used during calls to {@link #tagsFor(String)}, which will be
   * closed after tagging. 
   * @return A dictionary that might be used during calls to {@link #tagsFor(String)},
   * which will be closed after tagging.
   * @throws DictionaryException If the dictonary could not be instantiated.
   */
  @Override public Dictionary getTaggingDictionary() throws DictionaryException {
    if (taggingDictionary == null) {
      taggingDictionary = getDictionary(sql);
    }
    return taggingDictionary;
  }
  
  /**
   * Determines what tag labels should apply on the tag layer for
   * tokens with the given label on the token layer.
   * @param tokenLabel The label of the token(s) that must be tagged.
   * @return A list of tags, which may be empty.
   */
  public Collection<String> tagsFor(String tokenLabel) throws DictionaryException {
    LinkedHashSet<String> tags = new LinkedHashSet<String>();
    boolean found = false;
    for (String pronunciation : getTaggingDictionary().lookup(tokenLabel)) {
      
      found = true;
      tags.add(pronunciation);
      
      // do we want the first entry only?
      if (firstVariantOnly) break;
      
    } // next entry
    if (!found && sql.indexOf("PhonStrsDISC") >= 0) { // might be a hesitation?
      String pronunciation = hesitationToDISC(tokenLabel);
      if (pronunciation != null) {
        tags.add(pronunciation);
      }
    }
    return tags;
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
    return new Vector<String>() {{
      add("Cobuild Frequency (wordform)");
      add("Cobuild Frequency (lemma)");
      add("Phonology (wordform)");
      add("Morphology (wordform)");
      add("Syntax (wordform)");
    }};
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
    if (id == null || id.trim().length() == 0) { // null is not allowed
      throw new DictionaryException(null, "Invalid dictionary: " + id);
    }
    try {
      return new CELEXEnglishDictionary(
        this, newLexiconConnection(), sqlx, id);
    } catch (SQLException sqlX) {
      throw new DictionaryException(null, sqlX);
    }
  }

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
  
  /**
   * Returns the CELEX relational database schema, rendered as an HTML document.
   * @return An HTML document describing the CELEX database schema.
   */
  @ApiEndpoint("admin") public String schemaHtml() {
    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html>")
      .append("\n<html><head>")
      .append("\n  <meta content=\"text/html;charset=utf-8\" http-equiv=\"Content-Type\">")
      .append("\n  <meta content=\"utf-8\" http-equiv=\"encoding\">")
      .append("\n  <title> CELEX English Relational Database Schema </title>")
      .append("\n  <link rel=\"stylesheet\" href=\"index.css\" type=\"text/css\">")
      .append("\n  <link rel=\"stylesheet\" href=\"schema.css\" type=\"text/css\">")
      .append("\n  </head><body><h1>CELEX English Relational Database Schema</h1>");
    try {
      URL urlSchema = getClass().getResource("schema.sql");
      try (BufferedReader reader = new BufferedReader(
             new InputStreamReader(urlSchema.openStream()))) {
        html.append("<div class=\"schema\">");
        Pattern fieldPattern = Pattern.compile("^\\s*(\\w+) (.+)*,\\s*$");
        String line = reader.readLine();
        while (line != null) {
          //html.append(line + "\n");
          // build HTML
          if (line.matches("^CREATE TABLE.*")) {
            String sTableName = line.substring(13).replaceAll("\\(", "").trim();
            html.append("\n<table class=\"schema\" title=\""+sTableName+"\">"
                        +"<caption>"+sTableName+"</caption>"
                        +"<tbody>");
          } else if (line.matches("^\\) ENGINE=MyISAM;.*")) {
            html.append("\n</tbody></table>");
          } else if (line.matches("^.*KEY.*$")) {	    
          } else if (line.matches("^\\s*(\\w+) .*,\\s*$")) {
            Matcher m = fieldPattern.matcher(line);
            if (m.find()) {
              String fieldName = m.group(1);
              String fieldType = m.group(2);
              if (fieldType.indexOf("default") >= 0) {
                fieldType = fieldType.substring(0, fieldType.indexOf("default"));
              }
              html.append(
                "\n<tr"
                +(fieldType.contains("NOT NULL")?" class=\"primarykey\"":"")
                +"><td class=\"fieldname\">"+fieldName+"</td>"
                +"<td class=\"fieldtype\">"
                +fieldType.replaceAll("NOT NULL","")+"</td></tr>");
            }
          }
          line = reader.readLine();
        } // next line
      } // close reader
    } catch (Exception x) {
      html.append("<p class=\"error\">"+x.getMessage()+"</p>");
    }
    html.append("</body></html>");
    return html.toString();
  }
}
