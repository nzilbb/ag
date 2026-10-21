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
package nzilbb.annotator.celexde;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLEncoder;
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
import java.util.Properties;
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
import nzilbb.editpath.EditStep;
import nzilbb.editpath.MinimumEditPathString;
import nzilbb.encoding.ValidLabelsDefinitions;
import nzilbb.encoding.comparator.DISC2DISCComparator;
import nzilbb.sql.ConnectionFactory;
import nzilbb.sql.derby.DerbyConnectionFactory;
import nzilbb.sql.mysql.MySQLConnectionFactory;
import nzilbb.util.IO;

/**
 * Annotator that tags words with their lexical entries (pronunciations, frequency, etc.)
 * according to the 
 * <a href="https://catalog.ldc.upenn.edu/LDC96L14"> CELEX German lexicon </a>.
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
public class CELEXGermanTagger extends LabelBasedTagger
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
        System.err.println("CELEXGermanTagger.openLog: " + t);
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
        System.err.println("CELEXGermanTagger.closeLog: " + t);
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

        // TODO if it's not a shared connection, delete all the tables
            
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
  public CELEXGermanTagger setDbConnectString(String newDbConnectString) { dbConnectString = newDbConnectString; return this; }

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
  public CELEXGermanTagger setDbUser(String newDbUser) { dbUser = newDbUser; return this; }
  
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
  public CELEXGermanTagger setDbPassword(String newDbPassword) { dbPassword = newDbPassword; return this; }
  
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
   * Takes a ZIP file containing the CELEX German lexicon files.
   * @param file The lexicon file.
   * @return null if upload was successful, an error message otherwise.
   */
  @ApiEndpoint("admin") public String uploadLexicon(File file) {
    if (!file.getName().endsWith(".zip") && !file.getName().endsWith(".ZIP")) {
      return "Must be a zip file containing all CELEX German lexicon files: "
        + file.getName();
    }
    File zip = new File(getWorkingDirectory(), "CELEX-DE.zip");
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
    return new File(getWorkingDirectory(), "CELEX-DE.zip").exists();
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
   * Determines how many records there are in the cxde_wordformmorphology table,
   * i.e. the last table created and populated during installation.
   * @param rdb A connected database connection,
   * @return The number of records in the cxde_wordformmorphology table,
   * or -1 if the table table doesn't exist.
   */
  public int countWordformMorphologyRecords(Connection rdb) {
    try { // check existence of schema
      try (PreparedStatement sql = rdb.prepareStatement(
             sqlx.apply("SELECT COUNT(*) AS theCount FROM cxde_wordformmorphology"))) {
        try (ResultSet rs = sql.executeQuery()) {
          rs.next();
          return rs.getInt(1);
        } // rs.close
      } // sql.close
    } catch (SQLException x) {
    }
    return -1;
  } // end of countWordformMorphologyRecords()
  
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
    // load configuration, if any
    File f = new File(getWorkingDirectory(), getAnnotatorId() + ".cfg");
    if (f.exists()) {
      try {
        beanPropertiesFromQueryString(IO.InputStreamToString(new FileInputStream(f)));
      } catch(IOException exception) {}
    }
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
    int schemaLine = 0;
    try {
      openLog();
      setStatus(""); // clear any residual status from the last run...
      
      beanPropertiesFromQueryString(config);
      
      // persist configuration
      if (dbConnectString != null && dbConnectString.length() > 0) {
        try (PrintWriter writer = new PrintWriter(
               new File(getWorkingDirectory(), getAnnotatorId() + ".cfg"), "UTF-8")) {
          writer.print(
            "dbConnectString="+URLEncoder.encode(dbConnectString, "UTF-8")
            +"&dbUser="+URLEncoder.encode(Optional.ofNullable(dbUser).orElse(""), "UTF-8")
            +"&dbPassword="+URLEncoder.encode(
              Optional.ofNullable(dbPassword).orElse(""), "UTF-8"));
        } // close writer
      }

      // has the dictionary data been added?
      try (Connection rdb = newLexiconConnection()) {
        setStatus("Connected to database.");

        int recordCount = countWordformMorphologyRecords(rdb);
        if (recordCount < 0 ) { // schema isn't created yet
          setStatus("Creating CELEX GERMAN database schema...");
          
          // open schema file
          URL urlSchema = getClass().getResource("schema.sql");
          InputStream is = urlSchema.openStream();
          InputStreamReader isReader = new InputStreamReader(is);
          BufferedReader reader = new BufferedReader(isReader);
          
          // read file line by line
          String statement = "";
          String line = reader.readLine();
          while (line != null) {
            schemaLine++;
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
          setStatus("CELEX GERMAN database schema created.");
        }
        setPercentComplete(10);

        if (recordCount <= 0) { // lexicon isn't loaded yet
          install(rdb, new File(getWorkingDirectory(), "CELEX-DE.zip"));
        } 
       
        setPercentComplete(100);
        setStatus("Finished.");
      } // rdb.close()
    } catch (SQLException sqlX) {
      setStatus("ERROR: line "+schemaLine+" : " + sqlX);
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
        processGOL(rdb, zip);
      }
      if (!isCancelling()) {
        setPercentComplete(20);
        processGPL(rdb, zip);
      }
      if (!isCancelling()) {
        setPercentComplete(30);
        processGML(rdb, zip);
      }
      if (!isCancelling()) {
        setPercentComplete(40);
        processGFL(rdb, zip);
      }
      if (!isCancelling()) {
        setPercentComplete(50);
        processGSL(rdb, zip);
      }
      if (!isCancelling()) {
        setPercentComplete(60);
        processGOW(rdb, zip);
      }
      if (!isCancelling()) {
        setPercentComplete(70);
        processGPW(rdb, zip);
      }
      if (!isCancelling()) {
        setPercentComplete(80);
        processGFW(rdb, zip);
      }
      if (!isCancelling()) {
        setPercentComplete(90);
        processGMW(rdb, zip);
      }
      if (!isCancelling()) {
        setPercentComplete(95);
        processWordReforms(rdb, zip);
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
      .filter(e -> e.getName().toLowerCase().endsWith("/"+name.toLowerCase()))
      .findAny()
      .get();
  } // end of findEntry()
  
  /**
   * Process GOL file.
   */
  public void processGOL(Connection rdb, ZipFile zip)
    throws SQLException, IOException, NoSuchElementException {
    setStatus("Importing and indexing GOL.CD ...");
    ZipEntry eolFile = findEntry(zip, "GOL.CD");
    
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxde_lemma"))) {
      sqlDelete.execute();
    }

    try (PreparedStatement sqlInsertLemma = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxde_lemma VALUES (?,?,?,?,?,?,?,?)"))) {
      try (BufferedReader reader = new BufferedReader(
             new InputStreamReader(zip.getInputStream(eolFile)))) {
        String sLine = reader.readLine();
        // process each line of the file
        while(sLine != null && !isCancelling()) {
          if (sLine.length() == 0) continue;
          CELEXLine line = new CELEXLine(sLine);
          
          // main record
          //setStatus("GOL: " + line.getInt(1));
          sqlInsertLemma
            .setLong(1, line.getInt(1)); // IdNum
          sqlInsertLemma
            .setString(2, convertDiacritics(line.getString(2))); // HeadDia
          sqlInsertLemma
            .setLong(3, line.getInt(3)); // Mann
          sqlInsertLemma
            .setString(4, convertDiacritics(line.getString(4))); // HeadSylDia
          sqlInsertLemma
            .setString(5, line.getString(5)); // HeadSylChg
          sqlInsertLemma
            .setString(6, convertDiacritics(line.getString(6))); // StemDia
          sqlInsertLemma
            .setString(7, convertDiacritics(line.getString(7))); // StemSylDia
          sqlInsertLemma
            .setString(8, line.getString(8)); // StemSylChg
          sqlInsertLemma.executeUpdate();
          
          sLine = reader.readLine();
        } // next line
      } // close reader
    } // close sqlInsertLemma and sqlInsertLemmaOrtho
    if (!isCancelling()) setStatus("GOL complete.");
  } // end of processEOL()

  /**
   * Process GPL file.
   */
  public void processGPL(Connection rdb, ZipFile zip)
    throws SQLException, IOException, NoSuchElementException
  {
    setStatus("Importing and indexing GPL.CD ...");
    ZipEntry eplFile = findEntry(zip, "GPL.CD");
    
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxde_lemmaphonology"))) {
      sqlDelete.execute();
    }

    try (PreparedStatement sqlInsertLemmaPhonology = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxde_lemmaphonology VALUES (?,?,?,?,?,?,?,?,?,?)"));
         BufferedReader reader = new BufferedReader(
           new InputStreamReader(zip.getInputStream(eplFile)))) {
      String sLine = reader.readLine();
      // process each line of the file
      while(sLine != null && !isCancelling()) {
        if (sLine.length() == 0) continue;
        CELEXLine line = new CELEXLine(sLine);
        
        // main record
        //setStatus("GPL: " + line.getInt(1));
        sqlInsertLemmaPhonology.setLong(1, line.getInt(1)); // IdNumLemma
        sqlInsertLemmaPhonology.setLong(2, line.getInt(3)); // Mann
        sqlInsertLemmaPhonology.setString(
          3, line.getString(4)
          // /x/ -> [C] / V[front]_
          .replaceAll("([iyIYe\\{124WX\\)][^x\\\\]{0,2})x","$1C")
          // /x/ -> [C] / _...
          .replaceAll("^('??)x","$1C")); // PhonStrsDISC
        sqlInsertLemmaPhonology.setString (
          4, line.getString(5)); // PhonSylBCLX
        sqlInsertLemmaPhonology.setString(
          5, line.getString(6)); // PhonStrsStDISC
        sqlInsertLemmaPhonology.setString(
          6, line.getString(7)); // PhonSylStBCLX
        sqlInsertLemmaPhonology.setString(
          7, line.getString(8)); // PhonCVBr
        sqlInsertLemmaPhonology.setString(
          8, line.getString(9)); // PhonStCVBr
        sqlInsertLemmaPhonology.setString(
          9, line.getString(10)); // PhonolSAM
        sqlInsertLemmaPhonology.setString(
          10, line.getString(11)); // PhonolCLX
        sqlInsertLemmaPhonology.executeUpdate();
        
        sLine = reader.readLine();
      } // next line
    } // close sqlInsertLemmaPhonology, sqlInsertLemmaPhonologyPron, reader
    if (!isCancelling()) setStatus("GPL complete.");
  } // end of processEPL()

  /**
   * Process GML file.
   */
  public void processGML(Connection rdb, ZipFile zip)
    throws SQLException, IOException, NoSuchElementException {
    setStatus("Importing and indexing GML.CD ...");
    ZipEntry emlFile = findEntry(zip, "GML.CD");
    
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxde_lemmamorphology"))) {
      sqlDelete.execute();
    }
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxde_lemmamorphologyparse"))) {
      sqlDelete.execute();
    }

    try (PreparedStatement sqlInsertLemmaMorphology = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxde_lemmamorphology VALUES (?,?,?)"));
         PreparedStatement sqlInsertLemmaMorphologyParse = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxde_lemmamorphologyparse"
                      +" VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"));	 
         BufferedReader reader = new BufferedReader(
           new InputStreamReader(zip.getInputStream(emlFile)))) {
      String sLine = reader.readLine();
      // process each line of the file
      while(sLine != null && !isCancelling()) {
        if (sLine.length() == 0) continue;
        CELEXLine line = new CELEXLine(sLine);
        
        // main record
        //setStatus("GML: " + line.getInt(1));
        sqlInsertLemmaMorphology.setLong(1, line.getInt(1)); // IdNumLemma
        sqlInsertLemmaMorphology.setLong(2, line.getInt(3)); // Mann
        sqlInsertLemmaMorphology.setString(3, line.getString(4)); // MorphStatus
        sqlInsertLemmaMorphology.executeUpdate();

        // first instance
        int iVariant = 1;
        sqlInsertLemmaMorphologyParse.setLong(1, line.getInt(1)); // IdNumLemma
        sqlInsertLemmaMorphologyParse.setInt(2, iVariant); // Variant
        sqlInsertLemmaMorphologyParse.setString(3, line.getString(6)); // DerComp
        sqlInsertLemmaMorphologyParse.setString(4, line.getString(7)); // Comp
        sqlInsertLemmaMorphologyParse.setString(5, line.getString(8)); // Def
        sqlInsertLemmaMorphologyParse.setString(6, line.getString(9)); // Imm
        sqlInsertLemmaMorphologyParse.setString(7, line.getString(10)); // ImmClass
        sqlInsertLemmaMorphologyParse.setString(8, line.getString(11)); // ImmAllo
        sqlInsertLemmaMorphologyParse.setString(9, line.getString(12)); // ImmOpac
        sqlInsertLemmaMorphologyParse.setString(10, line.getString(13)); // ImmUml
        sqlInsertLemmaMorphologyParse.setString(11, line.getString(14)); // StrucLab
        sqlInsertLemmaMorphologyParse.setString(12, line.getString(15)); // StrucAllo
        sqlInsertLemmaMorphologyParse.setString(13, line.getString(16)); // StrucOpac
        sqlInsertLemmaMorphologyParse.setString(14, line.getString(17)); // StrucUml
        sqlInsertLemmaMorphologyParse.setString(15, line.getString(18)); // Sepa
        sqlInsertLemmaMorphologyParse.setString(16, line.getString(19)); // InflPar
        sqlInsertLemmaMorphologyParse.setString(17, line.getString(20)); // InflVar
        sqlInsertLemmaMorphologyParse.executeUpdate();
        
        // if there's more than one
        int iVariantCount = (int)line.getInt(5);
        int iVariantOffset = 21;
        while (iVariantCount > iVariant) {
          iVariant++;
	  
          sqlInsertLemmaMorphologyParse.setInt (2, iVariant); // Variant
          sqlInsertLemmaMorphologyParse.setString(
            3, line.getString(iVariantOffset)); // DerComp
          sqlInsertLemmaMorphologyParse.setString(
            4, line.getString(iVariantOffset + 1)); // Comp
          sqlInsertLemmaMorphologyParse.setString(
            5, line.getString(iVariantOffset + 2)); // Def
          sqlInsertLemmaMorphologyParse.setString(
            6, line.getString(iVariantOffset + 3)); // Imm
          sqlInsertLemmaMorphologyParse.setString(
            7, line.getString(iVariantOffset + 4)); // ImmClass
          sqlInsertLemmaMorphologyParse.setString(
            8, line.getString(iVariantOffset + 5)); // ImmAllo
          sqlInsertLemmaMorphologyParse.setString(
            9, line.getString(iVariantOffset + 6)); // ImmOpac
          sqlInsertLemmaMorphologyParse.setString(
            10, line.getString(iVariantOffset + 7)); // ImmUml
          sqlInsertLemmaMorphologyParse.setString(
            11, line.getString(iVariantOffset + 8)); // StrucLab
          sqlInsertLemmaMorphologyParse.setString(
            12, line.getString(iVariantOffset + 9)); // StrucAllo
          sqlInsertLemmaMorphologyParse.setString(
            13, line.getString(iVariantOffset + 10)); // StrucOpac
          sqlInsertLemmaMorphologyParse.setString(
            14, line.getString(iVariantOffset + 11)); // StrucUml
          sqlInsertLemmaMorphologyParse.setString(
            15, line.getString(iVariantOffset + 12)); // Sepa
          sqlInsertLemmaMorphologyParse.setString(
            16, line.getString(iVariantOffset + 13)); // InflPar
          sqlInsertLemmaMorphologyParse.setString(
            17, line.getString(iVariantOffset + 14)); // InflVar
          sqlInsertLemmaMorphologyParse.executeUpdate();
          
          iVariantOffset += 15;
        } // next variant
        
        sLine = reader.readLine();
      } // next line
    } // close sqlInsertLemmaMorphology, sqlInsertLemmaMorphologyParse, reader
    if (!isCancelling()) setStatus("GML complete.");
  } // end of processEML()

  /**
   * Process GFL file.
   */
  public void processGFL(Connection rdb, ZipFile zip)
    throws SQLException, IOException, NoSuchElementException {
    setStatus("Importing and indexing GFL.CD ...");
    ZipEntry eflFile = findEntry(zip, "GFL.CD");
    
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxde_lemmafrequency"))) {
      sqlDelete.execute();
    }
    try (PreparedStatement sqlInsertLemmaFrequency = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxde_lemmafrequency VALUES (?,?,?,?,?,?,?,?,?,?,?)")); 
         BufferedReader reader = new BufferedReader(
           new InputStreamReader(zip.getInputStream(eflFile)))) {
      String sLine = reader.readLine();
      // process each line of the file
      while(sLine != null && !isCancelling()) {
        if (sLine.length() == 0) continue;
        CELEXLine line = new CELEXLine(sLine);
        
        // main record
        //setStatus("GFL: " + line.getInt(1));
        sqlInsertLemmaFrequency.setLong(1, line.getInt(1)); // IdNumLemma
        sqlInsertLemmaFrequency.setLong(2, line.getInt(3)); // Mann
        sqlInsertLemmaFrequency.setString(3, line.getString(4)); // MannDev
        sqlInsertLemmaFrequency.setString(4, line.getString(5)); // MannMln
        sqlInsertLemmaFrequency.setString(5, line.getString(6)); // MannLog
        sqlInsertLemmaFrequency.setString(6, line.getString(7)); // MannW
        sqlInsertLemmaFrequency.setString(7, line.getString(8)); // MannWMln
        sqlInsertLemmaFrequency.setString(8, line.getString(9)); // MannWLog
        sqlInsertLemmaFrequency.setString(9, line.getString(10)); // MannS
        sqlInsertLemmaFrequency.setString(10, line.getString(11)); // MannSMln
        sqlInsertLemmaFrequency.setString(11, line.getString(12)); // MannSLog
        sqlInsertLemmaFrequency.executeUpdate();
        
        sLine = reader.readLine();
      } // next line
    } // close sqlInsertLemmaFrequency, reader
    if (!isCancelling()) setStatus("GPL complete.");
  } // end of processEPL()
  
  /**
   * Process GSL file.
   */
  public void processGSL(Connection rdb, ZipFile zip)
    throws SQLException, IOException, NoSuchElementException {
    setStatus("Importing and indexing GSL.CD ...");
    ZipEntry eslFile = findEntry(zip, "GSL.CD");
    
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxde_lemmasyntax"))) {
      sqlDelete.execute();
    }
    
    try (PreparedStatement sqlInsertLemmaSyntax = rdb.prepareStatement(
           sqlx.apply(
             "INSERT INTO cxde_lemmasyntax VALUES"
             +" (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"));
         BufferedReader reader = new BufferedReader(
           new InputStreamReader(zip.getInputStream(eslFile)))) {
      String sLine = reader.readLine();
      // process each line of the file
      while(sLine != null && !isCancelling()) {
        if (sLine.length() == 0) continue;
        CELEXLine line = new CELEXLine(sLine);
        
        // main record
        //setStatus("GSL: " + line.getInt(1));
        sqlInsertLemmaSyntax.setLong(1, line.getInt(1)); // IdNumLemma
        sqlInsertLemmaSyntax.setLong(2, line.getInt(3)); // Mann
        sqlInsertLemmaSyntax.setString(3, line.getString(4)); // ClassNum
        sqlInsertLemmaSyntax.setString(4, line.getString(5)); // GendNum
        sqlInsertLemmaSyntax.setString(5, line.getString(6)); // PropNum
        sqlInsertLemmaSyntax.setString(6, line.getString(7)); // SingTant
        sqlInsertLemmaSyntax.setString(7, line.getString(8)); // PlurTant
        sqlInsertLemmaSyntax.setString(8, line.getString(9)); // AuxNum
        sqlInsertLemmaSyntax.setString(9, line.getString(10)); // SubClassVNum
        sqlInsertLemmaSyntax.setString(10, line.getString(11)); // CompComp
        sqlInsertLemmaSyntax.setString(11, line.getString(12)); // CompEsSubj
        sqlInsertLemmaSyntax.setString(12, line.getString(13)); // CompSubj
        sqlInsertLemmaSyntax.setString(13, line.getString(14)); // CompAcc
        sqlInsertLemmaSyntax.setString(14, line.getString(15)); // CompSecAcc
        sqlInsertLemmaSyntax.setString(15, line.getString(16)); // CompDat
        sqlInsertLemmaSyntax.setString(16, line.getString(17)); // CompGen
        sqlInsertLemmaSyntax.setString(17, line.getString(18)); // CompPrep
        sqlInsertLemmaSyntax.setString(18, line.getString(19)); // CompSecPrep
        sqlInsertLemmaSyntax.setString(19, line.getString(20)); // CompAdv
        sqlInsertLemmaSyntax.setString(20, line.getString(21)); // Grad
        sqlInsertLemmaSyntax.setString(21, line.getString(22)); // CardOrdNum
        sqlInsertLemmaSyntax.setString(22, line.getString(23)); // SubClassPNum
        sqlInsertLemmaSyntax.setString(23, line.getString(24)); // Case
        sqlInsertLemmaSyntax.executeUpdate();
        
        sLine = reader.readLine();
      } // next line
    } // close sqlInsertLemmaSyntax, reader
    if (!isCancelling()) setStatus("GSL complete.");
  } // end of processESL()
  
  /**
   * Process GOW file.
   */
  public void processGOW(Connection rdb, ZipFile zip)
    throws SQLException, IOException, NoSuchElementException {
    setStatus("Importing and indexing GOW.CD ...");
    ZipEntry eowFile = findEntry(zip, "GOW.CD");
    
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxde_wordform"))) {
      sqlDelete.execute();
    }
 
    try (PreparedStatement sqlInsertWordForm = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxde_wordform VALUES (?,?,?,?,?,?)"));
         BufferedReader reader = new BufferedReader(
           new InputStreamReader(zip.getInputStream(eowFile)))) {
      String sLine = reader.readLine();
      // process each line of the file
      while(sLine != null && !isCancelling()) {
        if (sLine.length() == 0) continue;
        CELEXLine line = new CELEXLine(sLine);
        
        // main record
        //setStatus("GOW: " + line.getInt(1));
        sqlInsertWordForm.setLong(1, line.getInt(1)); // IdNum
        sqlInsertWordForm.setLong(2, line.getInt(4)); // IdNumLemma
        sqlInsertWordForm.setLong(3, line.getInt(3)); // Mann
        sqlInsertWordForm.setString(4, convertDiacritics(line.getString(2))); // WordDia
        sqlInsertWordForm.setString(5, convertDiacritics(line.getString(5))); // WordSylDia
        sqlInsertWordForm.setString(6, line.getString(6)); // WordSylChg
        sqlInsertWordForm.executeUpdate();
        
        sLine = reader.readLine();
      } // next line
    } // close sqlInsertWordForm, sqlInsertWordFormOrtho, reader
    if (!isCancelling()) setStatus("GOW complete.");
  } // end of processEOW()

  /**
   * Converts 2-character diacritic markers into their corresponding diacritics.
   * From the CELEX German documentation:
   * <pre>
   * The fields marked with "Dia" may contain diacritics of various kinds.
   * These diacritics have been coded as a sequence of two ASCII symbols, a
   * diacritic marker followed by the hosting letter. The Eszett, however,
   * is represented by a single dollar sign. The following conventions have
   * been used:
   * 
   *   diacritic symbol           diacritic sign
   *         #                           '       (acute accent)
   *         "                           "       (diaeresis (Umlaut))
   *         $                           ss      (Eszett)
   * </pre>
   * @param sWord A string with 2-character diacritic markers
   * @return The string with diacritics
   */
  public String convertDiacritics(String word) {
    if (word == null) return "";
    return word
      .replaceAll("\"a","ä")
      .replaceAll("\"e","ë")
      .replaceAll("\"i","ï")
      .replaceAll("\"o","ö")
      .replaceAll("\"u","ü")
      .replaceAll("\"A","Ä")
      .replaceAll("\"E","Ë")
      .replaceAll("\"I","Ï")
      .replaceAll("\"O","Ö")
      .replaceAll("\"U","Ü")
      .replaceAll("#e","é")
      .replaceAll("\\$","ß");    
  } // end of convertDiacritics()
  
  /**
   * Process GPW file.
   */
  public void processGPW(Connection rdb, ZipFile zip)
    throws SQLException, IOException, NoSuchElementException {
    setStatus("Importing and indexing GPW.CD ...");
    ZipEntry epwFile = findEntry(zip, "GPW.CD");
    
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxde_wordformphonology"))) {
      sqlDelete.execute();
    }

    try (PreparedStatement sqlInsertWordFormPhonology = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxde_wordformphonology VALUES (?,?,?,?,?)"));
         BufferedReader reader = new BufferedReader(
           new InputStreamReader(zip.getInputStream(epwFile)))) {
      String sLine = reader.readLine();
      // process each line of the file
      while(sLine != null && !isCancelling()) {
        if (sLine.length() == 0) continue;
        CELEXLine line = new CELEXLine(sLine);
        
        // main record
        //setStatus("GPW: " + line.getInt(1));
        sqlInsertWordFormPhonology.setLong(1, line.getInt(1)); // IdNum
        sqlInsertWordFormPhonology.setLong(2, line.getInt(3)); // Mann
        sqlInsertWordFormPhonology.setString(
          3, line.getString(5)
          // /x/ -> [C] / V[front]_
          .replaceAll("([iyIYe\\{124WX\\)][^x\\\\]{0,2})x","$1C")
          // /x/ -> [C] / _...
          .replaceAll("^('??)x","$1C")
          ); // PhonStrsDISC
        sqlInsertWordFormPhonology.setString(4, line.getString(6)); // PhonSylBCLX
        sqlInsertWordFormPhonology.setString(5, line.getString(7)); // PhonCVBr
        sqlInsertWordFormPhonology.executeUpdate();
        
        sLine = reader.readLine();
      } // next line
    } // close sqlInsertWordFormPhonology, sqlInsertWordFormPhonologyPron, reader
    if (!isCancelling()) setStatus("GPW complete.");
  } // end of processEPW()

  /**
   * Process GFW file.
   */
  public void processGFW(Connection rdb, ZipFile zip)
    throws SQLException, IOException, NoSuchElementException {
    setStatus("Importing and indexing GFW.CD ...");
    ZipEntry efwFile = findEntry(zip, "GFW.CD");
    
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxde_wordformfrequency"))) {
      sqlDelete.execute();
    }

    try (PreparedStatement sqlInsertWordFormFrequency = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxde_wordformfrequency VALUES (?,?,?,?,?,?,?,?,?,?,?)"));
         BufferedReader reader = new BufferedReader(
           new InputStreamReader(zip.getInputStream(efwFile)))) {
      String sLine = reader.readLine();
      // process each line of the file
      while(sLine != null && !isCancelling()) {
        if (sLine.length() == 0) continue;
        CELEXLine line = new CELEXLine(sLine);
        
        // main record
        //setStatus("GFW: " + line.getInt(1));
        sqlInsertWordFormFrequency.setLong(1, line.getInt(1)); // IdNum
        sqlInsertWordFormFrequency.setLong(2, line.getInt(4)); // Mann
        sqlInsertWordFormFrequency.setString(3, line.getString(5)); // MannDev
        sqlInsertWordFormFrequency.setString(4, line.getString(6)); // MannMln
        sqlInsertWordFormFrequency.setString(5, line.getString(7)); // MannLog
        sqlInsertWordFormFrequency.setString(6, line.getString(8)); // MannW
        sqlInsertWordFormFrequency.setString(7, line.getString(9)); // MannWMln
        sqlInsertWordFormFrequency.setString(8, line.getString(10)); // MannCWLog
        sqlInsertWordFormFrequency.setString(9, line.getString(11)); // MannS
        sqlInsertWordFormFrequency.setString(10, line.getString(12)); // MannSMln
        sqlInsertWordFormFrequency.setString(11, line.getString(13)); // MannSLog
        sqlInsertWordFormFrequency.executeUpdate();
        
        sLine = reader.readLine();
      } // next line
    } // close sqlInsertWordFormFrequency, reader
    if (!isCancelling()) setStatus("GFW complete.");
  } // end of processEFW()

  /**
   * Process GMW file.
   */
  public void processGMW(Connection rdb, ZipFile zip)
    throws SQLException, IOException, NoSuchElementException {
    setStatus("Importing and indexing GMW.CD ...");
    ZipEntry emwFile = findEntry(zip, "GMW.CD");
    
    try (PreparedStatement sqlDelete = rdb.prepareStatement(
           sqlx.apply("DELETE FROM cxde_wordformmorphology"))) {
      sqlDelete.execute();
    }

    try (PreparedStatement sqlInsertWordFormMorphology = rdb.prepareStatement(
           sqlx.apply("INSERT INTO cxde_wordformmorphology VALUES (?,?,?,?)"));
         BufferedReader reader = new BufferedReader(
           new InputStreamReader(zip.getInputStream(emwFile)))) {
      String sLine = reader.readLine();
      // process each line of the file
      while(sLine != null && !isCancelling()) {
        if (sLine.length() == 0) continue;
        CELEXLine line = new CELEXLine(sLine);
        
        // main record
        //setStatus("GMW: " + line.getInt(1));
        sqlInsertWordFormMorphology.setLong(1, line.getInt(1)); // IdNum
        sqlInsertWordFormMorphology.setLong(2, line.getInt(3)); // Mann
        sqlInsertWordFormMorphology.setString(3, line.getString(5)); // FlectType
        sqlInsertWordFormMorphology.setString(
          4, flectTypeDescription(line.getString(5))); // FlectTypeDescription
        sqlInsertWordFormMorphology.executeUpdate();
        
        sLine = reader.readLine();
      } // next line
    } // close sqlInsertWordFormMorphology, reader
    if (!isCancelling()) setStatus("GMW complete.");
  } // end of processGMW()
  
  /**
   * Converts the FlectType codes in a string to descriptive labels,
   * as documented in GUGA4 Table 10.
   * @param codes A coded string, something like nP,gP,dP,aP,nS,gS,dS,aS
   * @return A string containing a descriptive labels instead of codes, something
   * like NomPlu,GenPlu,DatPlu,AccPlu,NomSing,GenSing,DatSing,AccSing
   */
  public String flectTypeDescription(String codes) {
    String descriptive = "";
    // for each character
    char[] cCodes = codes.toCharArray();
    for (char c : cCodes) {
      switch (c) {
        case '/': descriptive += "Sepa"; break;
        case 'S': descriptive += "Sing"; break;
        case 'P': descriptive += "Plu"; break;
        case 'n': descriptive += "Nom"; break;
        case 'g': descriptive += "Gen"; break;
        case 'd': descriptive += "Dat"; break;
        case 'a': descriptive += "Acc"; break;
        case 'o': descriptive += "Pos"; break;
        case 'c': descriptive += "Comp"; break;
        case 'u': descriptive += "Sup"; break;
        case 'i': descriptive += "Inf"; break;
        case 'z': descriptive += "ZuInf"; break;
        case 'p': descriptive += "Part"; break;
        case 'E': descriptive += "Pres"; break;
        case 'A': descriptive += "Past"; break;
        case '1': descriptive += "1"; break; //"Sin1";
        case '2': descriptive += "2"; break; //"Sin2";
        case '3': descriptive += "3"; break; //"Sin3";
        case 'I': descriptive += "Ind"; break;
        case 'K': descriptive += "Sub"; break;
        case 'r': descriptive += "Imp"; break;
        case '4': descriptive += "-e"; break; //"Suff_e";
        case '5': descriptive += "-en"; break; //"Suff_en";
        case '6': descriptive += "-er"; break; //"Suff_er";
        case '7': descriptive += "-em"; break; //"Suff_em";
        case '8': descriptive += "-es"; break; //"Suff_es";
        case '9': descriptive += "-s"; break; //"Suff_s";
        case 'X':
          //descriptive += "Head";
          break;
        case 'm': descriptive += "Mas"; break;
        case 'w': descriptive += "Fem"; break;
        case 's': descriptive += "Neu"; break;
        case ' ': descriptive += "Uninfl"; break;
        case '0':
          //descriptive += "Adjdec";
          break;
        default: descriptive += c; break;
      }
    } // next code
    return descriptive;
  } // end of flectTypeDescription()

  /**
   * Uses the word-reform file (if any) to create wordforms with reformed orthography.
   * A copy of the old-orthography wordform is made (i.e. linked to the same lemma)
   * but the spelling is updated.
   * <p> The file used is <i>CELEX_ROOT</i>/GOW/SPELLINGREFORM.properties,
   * which contains mappings from old spellings to new ones, e.g.
   * <pre>
   * Aberhunderte=aberhunderte
   * Abertausende=abertausende
   * Abfluß=Abfluss
   * Abguß=Abguss
   * Ablaß=Ablass
   * Abriß=Abriss
   * Abschluß=Abschluss
   * </pre>
   * ...
   * @param 
   */
  public void processWordReforms(Connection rdb, ZipFile zip) throws SQLException, IOException {
    try {
      ZipEntry spellingReformFile = findEntry(zip, "SPELLINGREFORM.properties");
      
      setStatus("Creating words with reformed spellings");
      Properties mappings = new Properties();
      try (InputStream is = zip.getInputStream(spellingReformFile)) {
        mappings.load(is);
      } // is.close()
      
      try (PreparedStatement sqlStartIdNum = rdb.prepareStatement(
             sqlx.apply("SELECT MAX(IdNum) AS IdNum FROM cxde_wordform"))) {
        try (ResultSet rsStartIdNum = sqlStartIdNum.executeQuery()) {
          rsStartIdNum.next();
          int iNewIdNum = rsStartIdNum.getInt("IdNum") + 100;
	  
          try (PreparedStatement sqlOldIdNum = rdb.prepareStatement(
                 sqlx.apply(
                   "SELECT MAX(IdNum) AS IdNum FROM cxde_wordform WHERE WordDia = ?"));
               PreparedStatement sqlCopyWordForm = rdb.prepareStatement(
                 sqlx.apply(
                   "INSERT INTO cxde_wordform"
                   +" SELECT ?, IdNumLemma, Mann, ?, WordSylDia, WordSylChg"
                   +" FROM cxde_wordform WHERE IdNum = ?"));
               PreparedStatement sqlCopyWordFormFrequency = rdb.prepareStatement(
                 sqlx.apply(
                   "INSERT INTO cxde_wordformfrequency"
                   +" SELECT ?,"
                   +" Mann, MannDev, MannMln, MannLog, MannW, MannWMln,"
                   +" MannWLog, MannS, MannSMln, MannS"
                   +" FROM cxde_wordformfrequency WHERE IdNum = ?"));
               PreparedStatement sqlCopyWordFormMorphology = rdb.prepareStatement(
                 sqlx.apply(
                   "INSERT INTO cxde_wordformmorphology"
                   +" SELECT ?,"
                   +" Mann, FlectType, FlectTypeDescription"
                   +" FROM cxde_wordformmorphology WHERE IdNum = ?"));
               PreparedStatement sqlCopyWordFormPhonology = rdb.prepareStatement(
                 sqlx.apply(
                   "INSERT INTO cxde_wordformphonology"
                   +" SELECT ?,"
                   +" Mann, PhonStrsDISC, PhonSylBCLX, PhonCVBr"
                   +" FROM cxde_wordformphonology WHERE IdNum = ?"))) {
            
            for (String sOld : mappings.stringPropertyNames()) {
              String sNew = mappings.getProperty(sOld);
              
              // only add words that aren't already there
              sqlOldIdNum.setString(1, sNew);
              try (ResultSet rsNew = sqlOldIdNum.executeQuery()) { 
                if (!rsNew.next()) {	
                  sqlOldIdNum.setString(1, sOld);
                  try (ResultSet rs = sqlOldIdNum.executeQuery()) {
                    if (rs.next()) {
                      iNewIdNum++;
                      int iOldIdNum = rs.getInt("IdNum");
                      
                      // copy WordForm row
                      sqlCopyWordForm.setInt(1, iNewIdNum);
                      sqlCopyWordForm.setString(2, sNew);
                      sqlCopyWordForm.setInt(3, iOldIdNum);
                      sqlCopyWordForm.executeUpdate();
                      
                      // copy WordFormFrequency row
                      sqlCopyWordFormFrequency.setInt(1, iNewIdNum);
                      sqlCopyWordFormFrequency.setInt(2, iOldIdNum);
                      sqlCopyWordFormFrequency.executeUpdate();
                      
                      // copy WordFormMorphology row
                      sqlCopyWordFormMorphology.setInt(1, iNewIdNum);
                      sqlCopyWordFormMorphology.setInt(2, iOldIdNum);
                      sqlCopyWordFormMorphology.executeUpdate();
                      
                      // copy WordFormPhonology row
                      sqlCopyWordFormPhonology.setInt(1, iNewIdNum);
                      sqlCopyWordFormPhonology.setInt(2, iOldIdNum);
                      sqlCopyWordFormPhonology.executeUpdate();
                    } // the old word is there
                  } // close rs
                } // !rsNew.next()
              } // close rsNew
            } // next word
          } // close statements
        } // close rsStartIdNum
      } // close rsStartIdNum
    } catch (NoSuchElementException absent) {
      setStatus(
        "SPELLINGREFORM.properties does not exist. Reformed spellings will not be created.");
    }
  } // end of processWordReforms()
  
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
  public CELEXGermanTagger setFirstVariantOnly(Boolean newFirstVariantOnly) {
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
  public CELEXGermanTagger setSql(String newSql) { sql = newSql; return this; }
  
  /**
   * Delimiter(s) for syllable recovery.
   * @see #getDelimiters()
   * @see #setDelimiters(String)
   */
  protected String delimiters;
  /**
   * Getter for {@link #delimiters}: Delimiter(s) for syllable recovery.
   * @return Delimiter(s) for syllable recovery.
   */
  public String getDelimiters() { return delimiters; }
  /**
   * Setter for {@link #delimiters}: Delimiter(s) for syllable recovery.
   * @param newDelimiters Delimiter(s) for syllable recovery.
   */
  public CELEXGermanTagger setDelimiters(String newDelimiters) { delimiters = newDelimiters; return this; }

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
    delimiters = null;
        
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
    tagLayer = schema.getLayer(tagLayerId);
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
      if (delimiters == null) { // not recovering syllables
        if (!tagLayer.getPeers() && !firstVariantOnly) {
          setStatus(
            "Pronunciation layer " + tagLayerId
            + " doesn't allow peer annotations; using first variant only.");
          firstVariantOnly = true;
        }
        if (tagLayer.getAlignment() != Constants.ALIGNMENT_NONE
            && schema.getWordLayerId().equals(tagLayer.getParentId())) {
          tagLayer.setAlignment(Constants.ALIGNMENT_NONE);
        }
      } else { // recoving syllables
        if (tagLayer.getAlignment() != Constants.ALIGNMENT_INTERVAL) {
          tagLayer.setAlignment(Constants.ALIGNMENT_INTERVAL);
        }
      }
    }
    // set valid labels (they're actually valid label parts!)
    List<Map<String,Object>> validLabelsDefinition = new Vector<Map<String,Object>>();
    if (sql.indexOf("PhonStrsDISC") >= 0 // could be phonological
        && sql.indexOf("CHAR_LENGTH") < 0) { // but not syllable count
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
    // if we're recovering syllables, we need the word layer
    if (delimiters != null) requiredLayers.add(schema.getWordLayerId());
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
   * Transforms the graph. In this case, the graph is simply summarized, by counting all
   * tokens of each word type, and printing out the result to stdout.
   * @param graph The graph to transform.
   * @return The changes introduced by the tranformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  @Override public Graph transform(Graph graph) throws TransformationException {
    if (delimiters == null) { // we're not splitting labels to recover syllables
      // the LabelBasedTagger method is more efficient
      return super.transform(graph);
    } else { // we're splitting labels to recover syllables...
    setRunning(true);
    try {
      setStatus("Recovering syllables in " + graph.getId());
      
      Layer tokenLayer = graph.getSchema().getLayer(tokenLayerId);
      if (tokenLayer == null) {
        throw new InvalidConfigurationException(
          this, "Invalid input token layer: " + tokenLayerId);
      }
      String wordLayerId = graph.getSchema().getWordLayerId();
      tagLayer = graph.getSchema().getLayer(tagLayerId);
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
      if (phraseLanguageLayerId != null && targetLanguagePattern != null) {
        if (graph.first(phraseLanguageLayerId) != null) {
          thereArePhraseTags = true;
        }
      }
      
      TreeMap<String,Vector<Annotation>> toAnnotate = new TreeMap<String,Vector<Annotation>>();
      // should we just tag everything?
      if (transcriptIsMainlyTargetLang && !thereArePhraseTags) {
        // process all tokens
        for (Annotation token : graph.all(wordLayerId)) {
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
            for (Annotation token : phrase.all(wordLayerId)) {
              // mark the token as an exception
              token.put("@notTargetLang", Boolean.TRUE);
            } // next token in the phrase
          } // non-TargetLang phrase
        } // next phrase
        
        for (Annotation token : graph.all(wordLayerId)) {
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
            for (Annotation token : phrase.all(wordLayerId)) {
              // tag only tokens that are not already tagged
              if (token.first(tagLayerId) == null) { // not tagged yet
                registorForAnnotation(token, toAnnotate);
              } // not tagged yet
            } // next token in the phrase
          } // TargetLang phrase
        } // next phrase
      } // thereArePhraseTags
      
      try {
        Dictionary dictionary = getTaggingDictionary();
        try {
          int t = 0;
          int typeCount = toAnnotate.size();
          setStatus("Distinct words: " + typeCount);
          setPercentComplete(0);
          for (String pronunciation : toAnnotate.keySet()) { // for each type
            if (isCancelling()) break;
            // setStatus("Pron: " + pronunciation + " = " + dictionary.lookup(pronunciation));
            recoverSyllables(
              toAnnotate.get(pronunciation), dictionary.lookup(pronunciation));
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
  }
  
  /**
   * Registers a token for annotation, by adding it to the list in the given map of labels
   * to annotations with that label.
   * @param wordToken The word token to annotate.
   * @param toAnnotate A map of label→Annotations that may or may not contain an entry for
   * this token's label
   */
  protected void registorForAnnotation(
    Annotation token, TreeMap<String,Vector<Annotation>> toAnnotate) {
    // we need to lookup the pronunciation, i.e. concantenation of the phone labels
    String lookup = Arrays.stream(token.all(tokenLayerId))
      .map(annotation->annotation.getLabel())
      .collect(Collectors.joining());
    if (lookup.length() > 0) {
      if (!toAnnotate.containsKey(lookup)) {
        toAnnotate.put(lookup, new Vector<Annotation>());
      }
      toAnnotate.get(lookup).add(token);
    }
  } // end of registorForAnnotation()
  
  /**
   * Recover syllables for all the given tokens of the given type.
   * @param words Word tokens to recover the syllables of.
   * @param pronunciations Syllable/stress marked possible pronunciations from dictionary.
   */
  protected void recoverSyllables(Vector<Annotation> words, List<String> pronunciations) {
    if (pronunciations.size() == 0) return; // no pronunciation entries
    if (words.size() == 0) return; // no tokens
    Graph graph = words.get(0).getGraph();    
    
    for (Annotation word : words) {
      
      if (word.first(tagLayerId) != null) {
        setStatus(
          "Skipping word with existing " + tagLayerId + " annotations: "
          + word.getLabel() + " ("+word.getId()+")");
        continue;
      }
      Annotation phones[] = word.all(tokenLayerId);
      if (phones.length > 0) { // there are phones to recover syllabification from
        
        // find the best entry for these segments
        final String concatenatedSegments = Arrays.stream(phones)
          .map(segment -> segment.getLabel())
          .collect(Collectors.joining());
        final String stressMarkers = "['\",]";
        final String syllableStressMarkers = "['\",-]";
        final Character syllableBoundary = delimiters.charAt(0); // TODO remove delimiters
        String firstPron = pronunciations.get(0);
        Optional<String> firstMatchingPron = pronunciations.stream()
          .filter(pron -> concatenatedSegments.equals(pron.replaceAll(syllableStressMarkers,"")))
          .findAny();
        Optional<String> firstMatchingStressedPron = pronunciations.stream()
          .filter(pron -> concatenatedSegments.equals(pron.replaceAll(syllableStressMarkers,"")))
          .filter(pron -> pron.matches(".*"+stressMarkers+".*"))
          .findAny();
        String bestPron = firstMatchingStressedPron
          .orElse(firstMatchingPron
                  .orElse(firstPron));
        setStatus("Token " + word + " -> " + bestPron);
        
        // find an edit path between bestPron and the phone labels
        MinimumEditPathString editPath  = new MinimumEditPathString(new DISC2DISCComparator());
        List<EditStep<Character>> path = editPath.minimumEditPath(bestPron, concatenatedSegments);
        
        // traverse the edit path looking for syllable boundaries, annotating as we go
        int p = -1; // phones index
        int o = 1; // syllable ordinal
        Annotation firstPhone = phones[0];
        Annotation lastPhone = phones[0];
        StringBuilder label = new StringBuilder();
        for (EditStep<Character> step : path) {
          Character syllableCharacter = step.getFrom();
          Character phoneLabel = step.getTo();
          // increment phone index if this step has one
          if (phoneLabel != null) {
            if (p < phones.length-1) {
              p++;
            }
            lastPhone = phones[p];
            if (firstPhone == null) firstPhone = phones[p];
          }
          // have we hit a syllable boundary?
          if (syllableBoundary.equals(syllableCharacter)) {
            if (firstPhone == null) firstPhone = lastPhone;
            // annotate the syllable
            graph.createSpan​(firstPhone, lastPhone, tagLayerId, label.toString(), word)
              .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
            // next syllable should start on the next phone            
            firstPhone = null;
            label.setLength(0);
          } else if (syllableCharacter != null) {
            // accumulate label
            label.append(syllableCharacter);
          }
        } // next edit step
        
        // finish last syllable
        if (firstPhone == null) firstPhone = lastPhone;
        graph.createSpan​(firstPhone, lastPhone, tagLayerId, label.toString(), word)
          .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
        
      } // there are phones
    } // token doesn't already have syllables
  } // end of recoverSyllables()

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
    if (delimiters == null) { // we're not splitting labels to recover syllables
      // the LabelBasedTagger method is more efficient
      super.transformTranscripts(store, expression);
    } else { // we're splitting labels to recover syllables...
      // annotate one graph at a time...

      setRunning(true);    
      setStatus(
        "Annotating"+(expression==null?" all":"")+" transcripts"
        +(expression==null?"":" matching"+expression)+"...");
      try {    
        final LinkedHashSet<String> layerIds = new LinkedHashSet<String>();
        for (String layerId : getRequiredLayers()) {
          // we want this layer
          layerIds.add(layerId);
          // and all it's ancestors, in case they're affected by the transformation
          Layer layer = schema.getLayer(layerId);
          if (layer != null) {
            layer.getAncestors().stream()
              .map(l->l.getId())
              .forEach(ancestorId -> layerIds.add(ancestorId));
          }
        } // next required layer
        for (String layerId : getOutputLayers()) {
          // we want this layer
          layerIds.add(layerId);
          Layer layer = schema.getLayer(layerId);
          if (layer != null) {
            // and all it's ancestors, in case they're affected by the transformation
            layer.getAncestors().stream()
              .map(l->l.getId())
              .forEach(ancestorId -> layerIds.add(ancestorId));
            // and all it's descendants, in case they're affected by the transformation
            layer.getDescendants().stream()
              .map(l->l.getId())
              .forEach(descendantId -> layerIds.add(descendantId));
          }
          
        } // next output layer
        String[] ids = expression == null
          ?store.getTranscriptIds()
          :store.getMatchingTranscriptIds(expression);
        percentComplete = 0; // TODO multithread this so it's faster
        StoreException transcriptException = null;
        int soFar = 0;
        for (String id : ids) {
          try {
            if (isCancelling()) break;
            setStatus("Annotating " + id);
            Graph graph = store.getTranscript(id, layerIds.toArray(new String[0]));
            if (isCancelling()) break;
            graph.trackChanges();
            try {
              globalProgress = true; // don't set transform set progress or running
              transform(graph);
            } finally {
              globalProgress = false;
            }
            if (isCancelling()) break;
            store.saveTranscript(graph);
            setPercentComplete((++soFar * 100) / ids.length);
          } catch (StoreException storeX) {
            // we don't let a single transcript's problem stop all the others from
            // being annotated, but we save the exception for throwing later               
            if (transcriptException == null) transcriptException = storeX;
          } catch (GraphNotFoundException extremelyUnlikely) {
            // we just got the ID from the store, so this is pretty unlikely, and ignorable
            System.err.println("Annotator.transformTranscripts: " + extremelyUnlikely);
          }
        } // next transcript
        setStatus("Finished.");
        setPercentComplete(100);
        if (transcriptException != null) throw transcriptException;
      } finally {
        setRunning(false);
      }
    }    
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

  private Layer tagLayer; // use this to track whether hesitationToDISC is needed
  
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
    if (!found && tagLayer.getType() == Constants.TYPE_IPA) { // might be a hesitation?
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
    return new Vector<String>();
    // {{ // TODO named dictionaries
    //   add("Cobuild Frequency (wordform)");
    //   add("Cobuild Frequency (lemma)");
    //   add("Phonology (wordform)");
    //   add("Morphology (wordform)");
    //   add("Syntax (wordform)");
    // }};
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
    try {
      return new CELEXGermanDictionary(
        this, newLexiconConnection(), sqlx, Optional.ofNullable(id).orElse(sql));
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
   * @param taskId This parameter is ignored, but is required for task app requests to work.
   * @return An HTML document describing the CELEX database schema.
   */
  @ApiEndpoint("admin") public String schemaHtml(String taskId) {
    return schemaHtml();
  }
  
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
      .append("\n  <title> CELEX German Relational Database Schema </title>")
      .append("\n  <link rel=\"stylesheet\" href=\"index.css\" type=\"text/css\">")
      .append("\n  <link rel=\"stylesheet\" href=\"schema.css\" type=\"text/css\">")
      .append("\n  </head><body><h1>CELEX German Relational Database Schema</h1>");
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
