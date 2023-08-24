//
// Copyright 2023 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.annotator.unisyn;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
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
import nzilbb.editpath.EditStep;
import nzilbb.editpath.MinimumEditPathString;
import nzilbb.encoding.comparator.DISC2DISCComparator;
import nzilbb.util.IO;

/**
 * Annotates words with their pronunciations from Unisyn:
 * <a href="https://www.cstr.ed.ac.uk/projects/unisyn/">
 *  https://www.cstr.ed.ac.uk/projects/unisyn/</a>
 */
@UsesRelationalDatabase
public class UnisynTagger extends Annotator implements ImplementsDictionaries {
  /** Get the minimum version of the nzilbb.ag API supported by the annotator.*/
  public String getMinimumApiVersion() { return "1.1.3"; }
  
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

      // check the phoneme map table has been created
      try { // either of prepareStatement or executeQuery may fail if the table doesn't exist
        PreparedStatement sql = rdb.prepareStatement(
          sqlx.apply("SELECT * FROM "+getAnnotatorId()+"_phoneme_map LIMIT 1"));
        try {
          ResultSet rsCheck = sql.executeQuery();
          rsCheck.close();
        } finally {
          sql.close();
        }
      } catch(SQLException exception) {
        
        PreparedStatement sql = rdb.prepareStatement(
          sqlx.apply(
            "CREATE TABLE "+getAnnotatorId()+"_phoneme_map ("
            +" lexicon_id INTEGER NOT NULL"
            +" COMMENT 'ID of the lexicon',"
            +" phoneme_orig varchar(10) CHARACTER SET utf8mb4 COLLATE utf8_bin NOT NULL DEFAULT ''"
            +" COMMENT 'The phoneme as represented in the original file',"
            +" phoneme_disc varchar(10) CHARACTER SET utf8mb4 COLLATE utf8_bin NULL"
            +" COMMENT 'The phoneme as represented in CELEX DISC encoding',"
            +" note varchar(100) CHARACTER SET utf8mb4 COLLATE utf8_general_ci NULL"
            +" COMMENT 'Notes',"
            +" PRIMARY KEY (lexicon_id,phoneme_orig)"
            +") ENGINE=MyISAM;"));
        sql.executeUpdate();
        sql.close();
      }

      // Upgrade from versions prior to 0.314?
      setStatus("Checking for mappings for existing lexicons...");
      PreparedStatement sql = rdb.prepareStatement(
        sqlx.apply("SELECT * FROM "+getAnnotatorId()+"_lexicon"));
      PreparedStatement sqlPhonemeCount = rdb.prepareStatement(
        sqlx.apply("SELECT COUNT(*) FROM "+getAnnotatorId()+"_phoneme_map WHERE lexicon_id = ?"));
      PreparedStatement sqlMap = rdb.prepareStatement(
        sqlx.apply(
          "INSERT INTO "+getAnnotatorId()
          +"_phoneme_map (lexicon_id, phoneme_orig, phoneme_disc, note)"
          +" VALUES (?,?,?,?)"));
      
      ResultSet rsLexicon = sql.executeQuery();
      while (rsLexicon.next()) {
        String sName = rsLexicon.getString("name");
        int iId = rsLexicon.getInt("lexicon_id");
        setStatus("Checking for mappings for lexicon: " + sName);
        sqlPhonemeCount.setInt(1, iId);
        ResultSet rsPhonemeCount = sqlPhonemeCount.executeQuery();
        rsPhonemeCount.next();
        if (rsPhonemeCount.getInt(1) == 0) { // no mappings defined, so create default mappings
          sqlMap.setInt(1, iId);
          
          // SAMPA or Unisyn transcriptions? Unisyn ones include * as primary stress
          String sLexiconTable = getAnnotatorId()+"_lexicon_"+iId;
          PreparedStatement sqlUnisynOrSampa = rdb.prepareStatement(
            sqlx.apply("SELECT COUNT(*) FROM "+ sLexiconTable +" WHERE pron_orig LIKE '%*%'"));
          ResultSet rsUnisynOrSampa = sqlUnisynOrSampa.executeQuery();
          rsUnisynOrSampa.next();
          boolean bSampa = rsUnisynOrSampa.getInt(1) == 0;
          rsUnisynOrSampa.close();
          sqlUnisynOrSampa.close();
          
          setStatus("Installing default "+(bSampa?"SAMPA":"Unisyn")+" mappings for lexicon: " + sName);
          if (!bSampa) {
            // stress and syllabification
            saveMapping("*", "'", "primary stress", sqlMap);
            saveMapping("~", "\"", "secondary stress", sqlMap);
            saveMapping("-", ",", "tertiary stress", sqlMap);
            saveMapping(".", "-", "syllable boundary", sqlMap);
            
            // vowels
            saveMapping("e", "E", "DRESS", sqlMap);
            saveMapping("a", "{", "TRAP", sqlMap);
            saveMapping("ou", "5", "GOAT - but a monophthong for edi", sqlMap);
            saveMapping("o", "Q", "LOT", sqlMap);
            saveMapping("ah", "#", "BATH", sqlMap);
            saveMapping("oo", "$", "THOUGHT (but a diphthong in some en-US)", sqlMap);
            saveMapping("ii", "i", "FLEECE", sqlMap);
            saveMapping("i", "I", "KIT", sqlMap);
            saveMapping("@", "@", "schwa", sqlMap);
            saveMapping("uh", "V", "STRUT", sqlMap);
            saveMapping("u", "U", "FOOT", sqlMap);
            saveMapping("uu", "u", "GOOSE", sqlMap);
            saveMapping("ei", "1", "FACE", sqlMap);
            saveMapping("ai", "2", "PRICE", sqlMap);
            saveMapping("oi", "4", "CHOICE", sqlMap);
            saveMapping("ow", "6", "MOUTH", sqlMap);
            saveMapping("i@", "7", "NEAR", sqlMap);
            saveMapping("@@r", "3", "NURSE", sqlMap);
            saveMapping("eir", "8", "SQUARING (actually a monophthong in many)", sqlMap);
            saveMapping("ur", "9", "JURY", sqlMap);
              
            // troublesome because they map in accent-specific ways
            saveMapping("ar", "Q", "start -> PALM -> LOT (US) (but BATH (RP))", sqlMap);
            saveMapping("aa", "Q", "PALM -> LOT (US)  (but BATH (RP))", sqlMap);
            saveMapping("oa", "{", "BANANA -> TRAP (US) (but BATH (RP))", sqlMap);
            saveMapping("ao", "#", "MAZDA -> BATH (but TRAP (NZ))", sqlMap);
              
            // 2-to-1
            saveMapping("our", "$", "FORCE -> THOUGHT", sqlMap);
            saveMapping("eh", "{", "ann use TRAP", sqlMap);
            saveMapping("oul", "5", "goal - post vocalic GOAT", sqlMap);
            saveMapping("ouw", "5", "KNOW -> GOAT (except for Abergave)", sqlMap);
            saveMapping("oou", "Q", "adios -> LOT", sqlMap);
            saveMapping("au", "Q", "CLOTH -> LOT (but a diphthong in some en-US)", sqlMap);
            saveMapping("or", "$", "r-coloured THOUGHT", sqlMap);
            saveMapping("iy", "i", "HAPPY - I for some varieties", sqlMap);
            saveMapping("ie", "i", "HARRIET - Leeds only", sqlMap);
            saveMapping("ii;", "i", "AGREED -> FLEECE", sqlMap);
            saveMapping("@r", "@", "r-coloured schwa", sqlMap);
            saveMapping("iu", "u", "BLEW -> GOOSE", sqlMap);
            saveMapping("uu;", "u", "brewed -> GOOSE", sqlMap);
            saveMapping("uw", "u", "louise -> GOOSE", sqlMap);
            saveMapping("uul", "u", "goul - post-vocalic GOOSE", sqlMap);
            saveMapping("ee", "1", "WASTE -> FACE (except for abercrave)", sqlMap);
            saveMapping("ae", "2", "TIED -> PRICE (except Edi and Aberdeen)", sqlMap);
            saveMapping("aer", "2", "FIRE - r-coloured PRICE", sqlMap);
            saveMapping("aai", "2", "TIME -> PRICE (except S. Carolina)", sqlMap);
            saveMapping("oir", "2", "COIR - r-coloured PRICE", sqlMap);
            saveMapping("owr", "6", "HOUR - r-coloured MOUTH", sqlMap);
            saveMapping("oow", "6", "HOUR -> MOUTH (exception S. Carolina)", sqlMap);
            saveMapping("ir", "i", "NEARING - r-coloured NEAR -> FLEECE", sqlMap);
            saveMapping("ir;", "i", "near - scots-long NEAR -> FLEECE", sqlMap);
            saveMapping("iir", "7", "beard -> NEAR (except en-AU)", sqlMap);
            saveMapping("er", "E", "r-coloured DRESS in scots en", sqlMap);
            saveMapping("ur;", "9", "CURE - scots-long JURY", sqlMap);
            saveMapping("iur", "9", "curious - JURY exception in Cardiff & Abercrave", sqlMap);
              
            // consonants
            saveMapping("y", "j", "", sqlMap);
            saveMapping("ch", "J", "", sqlMap);
            saveMapping("jh", "_", "", sqlMap);
            saveMapping("sh", "S", "", sqlMap);
            saveMapping("zh", "Z", "", sqlMap);
            saveMapping("th", "T", "", sqlMap);
            saveMapping("dh", "D", "", sqlMap);
            saveMapping("t^", "L", "butter/merry flap", sqlMap);
            saveMapping("m!", "F", "chasm", sqlMap);
            saveMapping("n!", "H", "mission", sqlMap);
            saveMapping("ng", "N", "", sqlMap);
            saveMapping("l!", "P", "cattle", sqlMap);
              
            // 2-to-1
            saveMapping("ll", "l", "llandudno (for Cardiff and Abercrave, this is different)", sqlMap);
            saveMapping("lw", "l", "feel - dark l", sqlMap);
            saveMapping("hw", "w", "which", sqlMap);
          } else { // XSAMPA default mapping
            // vowels
            saveMapping("i:", "i", "", sqlMap);
            saveMapping("A:", "#", "", sqlMap);
            saveMapping("O:", "$", "", sqlMap);
            saveMapping("u:", "u", "", sqlMap);
            saveMapping("3:", "3", "", sqlMap);
            saveMapping("eI", "1", "", sqlMap);
            saveMapping("aI", "2", "", sqlMap);
            saveMapping("OI", "4", "", sqlMap);
            saveMapping("@U", "5", "", sqlMap);
            saveMapping("aU", "6", "", sqlMap);
            saveMapping("I@", "7", "", sqlMap);
            saveMapping("E@", "8", "", sqlMap);
            saveMapping("U@", "9", "", sqlMap);
            saveMapping("{~", "c", "", sqlMap);
            saveMapping("A~:", "q", "", sqlMap);
            saveMapping("{~:", "0", "", sqlMap);
            saveMapping("O~:", "~", "", sqlMap);
              
            // different from SAMPA
            saveMapping("}:", "}", "", sqlMap);
              
            // 2-to-1 vowel mappings to encompass en-AU and other variants
            // according to http://en.wikipedia.org/wiki/SAMPA_chart_for_English
            saveMapping("{I", "1", "en-AU", sqlMap);
            saveMapping("Ae", "2", "en-AU", sqlMap);
            saveMapping("AI", "2", "en-NZ", sqlMap);
            saveMapping("oI", "4", "en-AU", sqlMap);
            saveMapping("oU", "5", "en-US", sqlMap);
            saveMapping("@}", "5", "en-AU", sqlMap);
            saveMapping("VU", "5", "en-NZ", sqlMap);
            saveMapping("{O", "6", "en-AU", sqlMap);
            saveMapping("{U", "6", "en-NZ", sqlMap); 
            saveMapping("e:", "8", "en-AU", sqlMap);
              
            saveMapping("a:", "#", "", sqlMap);
            saveMapping("o:", "$", "", sqlMap);
              
            // consonants
            saveMapping("tS", "J", "", sqlMap);
            saveMapping("dZ", "_", "", sqlMap);
            saveMapping("r*", "R", "", sqlMap);
              
            // different from SAMPA
            saveMapping("r\\", "r", "", sqlMap);
            saveMapping("5", "l", "", sqlMap);
            saveMapping("N=", "C", "", sqlMap);
            saveMapping("m=", "F", "", sqlMap);
            saveMapping("n=", "H", "", sqlMap);
            saveMapping("l=", "P", "", sqlMap);
          } // SAMPA
        } // define mappings
        rsPhonemeCount.close();
      } // next lexicon
      rsLexicon.close();
      sql.close();
      sqlPhonemeCount.close();
      sqlMap.close();
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
        
        // drop the mappings table
        sql = rdb.prepareStatement(
          sqlx.apply("DROP TABLE "+getAnnotatorId()+"_phoneme_map"));
        sql.executeUpdate();
        sql.close();
        
        // drop the lexicon table
        sql = rdb.prepareStatement(
          sqlx.apply("DROP TABLE "+getAnnotatorId()+"_lexicon"));
        sql.executeUpdate();
        sql.close();
        
      } finally {
        try { rdb.close(); } catch(SQLException x) {}
      }      
    } catch (SQLException x) {
    }
  }
  
  /**
   * Loads a lexicon from a given file.
   * <p> This method starts a thread to load the records, and then returns. Callers must
   * track {@link Annotator#getPercentComplete()} and {@link Annotator#getRunning()} for
   * progress updates.
   * @param lexicon The name for the resulting lexicon. If the named
   * lexicon already exists, it will be completely replaced with the contents of the file
   * (i.e. all existing entries will be deleted befor adding new entries from the file).
   * @param file The lexicon file.
   * @return An empty string if upload was successful, an error message otherwise.
   */
  public String loadLexicon(String lexicon, File file) {
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

        // create lexicon table...

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
          +" wordform VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL"
          +" COMMENT 'Orthographic spelling of the word, in lowercase letters',"
          +" variant SMALLINT NOT NULL"
          +" COMMENT 'Identifier number, one-based if there is more than one, otherwise zero',"
          +" variant_description VARCHAR(50) NULL"
          +" COMMENT 'Description of variant, if any',"
          +" pos VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL"
          +" COMMENT 'Part of Speech, possibilities delimited by / ',"
          +" pron_orig VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL"
          +" COMMENT 'Original pronunciation from the Unisyn file',"
          +" enriched_orthography VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL"
          +" COMMENT 'Enriched orthography including morphological info, etc.',"
          +" frequency INTEGER NULL"
          +" COMMENT 'frequency in uni_freqs',"
          +" syllable_count INTEGER NOT NULL DEFAULT 0"
          +" COMMENT 'Syllable count based on the number of . in pron_orig',"
          +" supplemental BIT NOT NULL DEFAULT 0"
          +" COMMENT 'Whether the word/variant has been manually added since uploading from the Unisyn file',"
          +" PRIMARY KEY (wordform,variant)"
          +") ENGINE=MyISAM";
        sql = rdb.prepareStatement(sqlx.apply(sSql));
        sql.executeUpdate();
        sql.close();

        // load the lexicon in another thread - the caller must track isRunning...
        
        // the thread needs it's own copy of the file
        final File tempFile = File.createTempFile("UnisynTagger-", file.getName());
        tempFile.deleteOnExit();
        
        // we also count records so we can accurately track progress
        int entryCount = 0;
        // and maybe convert the first space to a tab
        BufferedReader firstReader = new BufferedReader(new FileReader(file));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));
        String line = firstReader.readLine(); 
        while (line != null) {
          if (line.trim().length() > 0) { // ignore blank lines
            if (entryCount > 0) writer.newLine();
            writer.write(line);
            entryCount++;
          }
          line = firstReader.readLine();
        } // next line
        firstReader.close();
        writer.close();
        setStatus("Loading " + entryCount + (entryCount==1?" entry...":" entries..."));
        
        final int finalEntryCount = entryCount;
        final int finalLexiconId = lexiconId;
        setRunning(true);
        Runnable loadFile = () -> {
          try {
            
            // now insert all entries into the lexicon
            Connection rdbLoad = newConnection();
            try {
              PreparedStatement sqlLoad = rdbLoad.prepareStatement(
                sqlx.apply(
                  "REPLACE INTO "+getAnnotatorId()+"_lexicon_"+finalLexiconId
                  +" (wordform, variant, variant_description, pos, pron_orig,"
                  +" enriched_orthography, frequency, syllable_count) VALUES (?,?,?,?,?,?,?,?)"));
              BufferedReader reader = new BufferedReader(new FileReader(tempFile));
              int iLine = 0;
              Boolean bSampa = null; // true for SAMPA transcriptions, false for Unisyn transcriptions
              try
              {
                String sLine = reader.readLine();
                while (sLine != null) {
                  if (isCancelling()) break;
                  iLine++;
                  if (sLine.trim().length() > 0) {
                    // something like. zealand::NNP: "zi:$5@nd :{ze==aland}:2682
                    // : can is a field delimiter, but also might appear in the pronunciation
                    try {
                      String sPronStart = ": ";
                      int iPronStart = sLine.indexOf(sPronStart);
                      String sPronEnd = " :";
                      int iPronEnd = sLine.indexOf(sPronEnd);
                      String sDelimiter = ":";
                      
                      StringTokenizer stBefore 
                        = new StringTokenizer(sLine.substring(0, iPronStart), sDelimiter);
                      String sWordForm = stBefore.nextToken();
                      int iVariant = 0;
                      String sVariantDescription = "";
                      if (sLine.indexOf("::") < 0) {
                        String sVariant = stBefore.nextToken();
                        try {
                          iVariant = Integer.parseInt(sVariant);
                        } catch(NumberFormatException x1) {
                          // variant is something like "1,scotland"
                          if (sVariant.indexOf(',') >= 0) {
                            sVariantDescription = sVariant.substring(sVariant.indexOf(',') + 1);
                            sVariant = sVariant.substring(0, sVariant.indexOf(','));
                          }
                          try {
                            iVariant = Integer.parseInt(sVariant);
                          } catch(NumberFormatException x2) {
                            // variant is something like "sound-of-hitting"
                            sVariantDescription = sVariant;
                          }
                        }
                      }
                      String sPos = stBefore.nextToken();
                      
                      String sPron = sLine.substring(iPronStart + sPronStart.length(), iPronEnd);

                      // SAMPA or Unisyn transcription? Unisyn ones include * as primary stress
                      if (bSampa == null) bSampa = new Boolean(sPron.indexOf('*') < 0);
                      
                      StringTokenizer stAfter = new StringTokenizer(
                        sLine.substring(iPronEnd + sPronEnd.length()), sDelimiter);
                      String sEnrichedOrthography = stAfter.nextToken();
                      String sFrequency = stAfter.nextToken();
                      
                      sWordForm = sWordForm.toLowerCase();
                      
                      int iSyllableCount = sPron.replaceAll("[^.]", "").length() + 1;
                      
                      sqlLoad.setString(1, sWordForm);
                      sqlLoad.setInt(2, iVariant);
                      sqlLoad.setString(3, sVariantDescription);
                      sqlLoad.setString(4, sPos);
                      sqlLoad.setString(5, sPron);
                      sqlLoad.setString(6, sEnrichedOrthography);
                      sqlLoad.setInt(7, Integer.parseInt(sFrequency));
                      sqlLoad.setInt(8, iSyllableCount);
                      sqlLoad.executeUpdate();
                      
                      setPercentComplete((sWordForm.charAt(0) - 'a') * 100/26);
                      
                    } catch (Throwable t) {
                      System.out.println(
                        "UnisynTagger.loadLexicon "+tempFile.getName()+": ERROR line "+iLine+" \""+sLine+"\": "
                        + Optional.of(t.getMessage()).orElse(t.toString()));
                      setStatus("ERROR line "+iLine+" \""+sLine+"\": " + Optional.of(t.getMessage()).orElse(t.toString()));
                    }
                  } // not a comment
                  
                  sLine = reader.readLine();
                } // next line
              } finally {
                reader.close();
                sqlLoad.close();
              }

              // create phoneme map if it's not already there
              sqlLoad = rdbLoad.prepareStatement(
                sqlx.apply("SELECT COUNT(*) AS theCount FROM "
                           +getAnnotatorId()+"_phoneme_map WHERE lexicon_id = ?"));
              sqlLoad.setInt(1, finalLexiconId);
              ResultSet rsCheck = sqlLoad.executeQuery();
              rsCheck.next();
              boolean bCreate = rsCheck.getInt(1) == 0;
              rsCheck.close();
              sqlLoad.close();
              if (bCreate) {
                sqlLoad = rdbLoad.prepareStatement(
                  sqlx.apply(
                    "INSERT INTO "+getAnnotatorId()+"_phoneme_map"
                    +" (lexicon_id, phoneme_orig, phoneme_disc, note) VALUES (?,?,?,?)"));
                sqlLoad.setInt(1, finalLexiconId);
                
                if (bSampa == null || !bSampa) {
                  // stress and syllabification
                  saveMapping("*", "'", "primary stress", sqlLoad);
                  saveMapping("~", "\"", "secondary stress", sqlLoad);
                  saveMapping("-", ",", "tertiary stress", sqlLoad);
                  saveMapping(".", "-", "syllable boundary", sqlLoad);
                  
                  // vowels
                  saveMapping("e", "E", "DRESS", sqlLoad);
                  saveMapping("a", "{", "TRAP", sqlLoad);
                  saveMapping("ou", "5", "GOAT - but a monophthong for edi", sqlLoad);
                  saveMapping("o", "Q", "LOT", sqlLoad);
                  saveMapping("ah", "#", "BATH", sqlLoad);
                  saveMapping("oo", "$", "THOUGHT (but a diphthong in some en-US)", sqlLoad);
                  saveMapping("ii", "i", "FLEECE", sqlLoad);
                  saveMapping("i", "I", "KIT", sqlLoad);
                  saveMapping("@", "@", "schwa", sqlLoad);
                  saveMapping("uh", "V", "STRUT", sqlLoad);
                  saveMapping("u", "U", "FOOT", sqlLoad);
                  saveMapping("uu", "u", "GOOSE", sqlLoad);
                  saveMapping("ei", "1", "FACE", sqlLoad);
                  saveMapping("ai", "2", "PRICE", sqlLoad);
                  saveMapping("oi", "4", "CHOICE", sqlLoad);
                  saveMapping("ow", "6", "MOUTH", sqlLoad);
                  saveMapping("i@", "7", "NEAR", sqlLoad);
                  saveMapping("@@r", "3", "NURSE", sqlLoad);
                  saveMapping("eir", "8", "SQUARING (actually a monophthong in many)", sqlLoad);
                  saveMapping("ur", "9", "JURY", sqlLoad);
                  
                  // troublesome because they map in accent-specific ways
                  saveMapping("ar", "Q", "start -> PALM -> LOT (US) (but BATH (RP))", sqlLoad);
                  saveMapping("aa", "Q", "PALM -> LOT (US)  (but BATH (RP))", sqlLoad);
                  saveMapping("oa", "{", "BANANA -> TRAP (US) (but BATH (RP))", sqlLoad);
                  saveMapping("ao", "#", "MAZDA -> BATH (but TRAP (NZ))", sqlLoad);
                  
                  // 2-to-1
                  saveMapping("our", "$", "FORCE -> THOUGHT", sqlLoad);
                  saveMapping("eh", "{", "ann use TRAP", sqlLoad);
                  saveMapping("oul", "5", "goal - post vocalic GOAT", sqlLoad);
                  saveMapping("ouw", "5", "KNOW -> GOAT (except for Abergave)", sqlLoad);
                  saveMapping("oou", "Q", "adios -> LOT", sqlLoad);
                  saveMapping("au", "Q", "CLOTH -> LOT (but a diphthong in some en-US)", sqlLoad);
                  saveMapping("or", "$", "r-coloured THOUGHT", sqlLoad);
                  saveMapping("iy", "i", "HAPPY - I for some varieties", sqlLoad);
                  saveMapping("ie", "i", "HARRIET - Leeds only", sqlLoad);
                  saveMapping("ii;", "i", "AGREED -> FLEECE", sqlLoad);
                  saveMapping("@r", "@", "r-coloured schwa", sqlLoad);
                  saveMapping("iu", "u", "BLEW -> GOOSE", sqlLoad);
                  saveMapping("uu;", "u", "brewed -> GOOSE", sqlLoad);
                  saveMapping("uw", "u", "louise -> GOOSE", sqlLoad);
                  saveMapping("uul", "u", "goul - post-vocalic GOOSE", sqlLoad);
                  saveMapping("ee", "1", "WASTE -> FACE (except for abercrave)", sqlLoad);
                  saveMapping("ae", "2", "TIED -> PRICE (except Edi and Aberdeen)", sqlLoad);
                  saveMapping("aer", "2", "FIRE - r-coloured PRICE", sqlLoad);
                  saveMapping("aai", "2", "TIME -> PRICE (except S. Carolina)", sqlLoad);
                  saveMapping("oir", "2", "COIR - r-coloured PRICE", sqlLoad);
                  saveMapping("owr", "6", "HOUR - r-coloured MOUTH", sqlLoad);
                  saveMapping("oow", "6", "HOUR -> MOUTH (exception S. Carolina)", sqlLoad);
                  saveMapping("ir", "i", "NEARING - r-coloured NEAR -> FLEECE", sqlLoad);
                  saveMapping("ir;", "i", "near - scots-long NEAR -> FLEECE", sqlLoad);
                  saveMapping("iir", "7", "beard -> NEAR (except en-AU)", sqlLoad);
                  saveMapping("er", "E", "r-coloured DRESS in scots en", sqlLoad);
                  saveMapping("ur;", "9", "CURE - scots-long JURY", sqlLoad);
                  saveMapping("iur", "9", "curious - JURY exception in Cardiff & Abercrave", sqlLoad);
                  
                  // consonants
                  saveMapping("y", "j", "", sqlLoad);
                  saveMapping("ch", "J", "", sqlLoad);
                  saveMapping("jh", "_", "", sqlLoad);
                  saveMapping("sh", "S", "", sqlLoad);
                  saveMapping("zh", "Z", "", sqlLoad);
                  saveMapping("th", "T", "", sqlLoad);
                  saveMapping("dh", "D", "", sqlLoad);
                  saveMapping("t^", "L", "butter/merry flap", sqlLoad);
                  saveMapping("m!", "F", "chasm", sqlLoad);
                  saveMapping("n!", "H", "mission", sqlLoad);
                  saveMapping("ng", "N", "", sqlLoad);
                  saveMapping("l!", "P", "cattle", sqlLoad);
                  
                  // 2-to-1
                  saveMapping("ll", "l",
                              "llandudno (for Cardiff and Abercrave, this is different)", sqlLoad);
                  saveMapping("lw", "l", "feel - dark l", sqlLoad);
                  saveMapping("hw", "w", "which", sqlLoad);
                } else { // XSAMPA default mapping
                  // vowels
                  saveMapping("i:", "i", "", sqlLoad);
                  saveMapping("A:", "#", "", sqlLoad);
                  saveMapping("O:", "$", "", sqlLoad);
                  saveMapping("u:", "u", "", sqlLoad);
                  saveMapping("3:", "3", "", sqlLoad);
                  saveMapping("eI", "1", "", sqlLoad);
                  saveMapping("aI", "2", "", sqlLoad);
                  saveMapping("OI", "4", "", sqlLoad);
                  saveMapping("@U", "5", "", sqlLoad);
                  saveMapping("aU", "6", "", sqlLoad);
                  saveMapping("I@", "7", "", sqlLoad);
                  saveMapping("E@", "8", "", sqlLoad);
                  saveMapping("U@", "9", "", sqlLoad);
                  saveMapping("{~", "c", "", sqlLoad);
                  saveMapping("A~:", "q", "", sqlLoad);
                  saveMapping("{~:", "0", "", sqlLoad);
                  saveMapping("O~:", "~", "", sqlLoad);
                  
                  // different from SAMPA
                  saveMapping("}:", "}", "", sqlLoad);
                  
                  // 2-to-1 vowel mappings to encompass en-AU and other variants
                  // according to http://en.wikipedia.org/wiki/SAMPA_chart_for_English
                  saveMapping("{I", "1", "en-AU", sqlLoad);
                  saveMapping("Ae", "2", "en-AU", sqlLoad);
                  saveMapping("AI", "2", "en-NZ", sqlLoad);
                  saveMapping("oI", "4", "en-AU", sqlLoad);
                  saveMapping("oU", "5", "en-US", sqlLoad);
                  saveMapping("@}", "5", "en-AU", sqlLoad);
                  saveMapping("VU", "5", "en-NZ", sqlLoad);
                  saveMapping("{O", "6", "en-AU", sqlLoad);
                  saveMapping("{U", "6", "en-NZ", sqlLoad); 
                  saveMapping("e:", "8", "en-AU", sqlLoad);
                  
                  saveMapping("a:", "#", "", sqlLoad);
                  saveMapping("o:", "$", "", sqlLoad);
                  
                  // consonants
                  saveMapping("tS", "J", "", sqlLoad);
                  saveMapping("dZ", "_", "", sqlLoad);
                  saveMapping("r*", "R", "", sqlLoad);
                  
                  // different from SAMPA
                  saveMapping("r\\", "r", "", sqlLoad);
                  saveMapping("5", "l", "", sqlLoad);
                  saveMapping("N=", "C", "", sqlLoad);
                  saveMapping("m=", "F", "", sqlLoad);
                  saveMapping("n=", "H", "", sqlLoad);
                  saveMapping("l=", "P", "", sqlLoad);
                }
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
      System.err.println("UnisynTagger.loadLexicon: " + x);
      x.printStackTrace(System.err);
      return x.toString();
    }
  } // end of loadLexicon()
  
  /**
   * Saves a single mapping row in the mapping table.
   * @param from Value for parameter 2.
   * @param to Value for parameter 3
   * @param comment Value for parameter 4
   * @param sql The SQL INSERT/REPLACE statement to execute. 
   * @throws SQLException On SQL error.
   */
  private void saveMapping(String from, String to, String comment, PreparedStatement sql)
    throws SQLException {
    sql.setString(2, from);
    sql.setString(3, to);
    sql.setString(4, comment);
    sql.executeUpdate();
  } // end of saveMapping()
  
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

          // delete the mappings
          try {
            PreparedStatement sqlDeleteMappings = rdb.prepareStatement(
              sqlx.apply(
                "DELETE FROM "+getAnnotatorId()+"_phoneme_map WHERE lexicon_id = ?"));
            sqlDeleteMappings.setInt(1, rs.getInt("lexicon_id"));
            try {
              sqlDeleteMappings.executeUpdate();
            } finally {
              sqlDeleteMappings.close();
            }
          } catch(SQLException exception) {}

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
   * Create a new phone mapping in a given lexicon.
   * @param lexicon Lexicon name.
   * @param phoneme_orig The new phoneme label to create.
   * @param phoneme_disc The new DISC label for the phoneme.
   * @param note The new note for the label.
   * @return An error if any, or null if not.
   */
  public String createDiscMapping(String lexicon, String phoneme_orig, String phoneme_disc, String note) {
    Vector<LinkedHashMap> mappings = new Vector<LinkedHashMap>();
    try {
      Connection rdb = newConnection();      
      PreparedStatement sql = rdb.prepareStatement(
        sqlx.apply("INSERT INTO "+getAnnotatorId()+"_phoneme_map"
                   +" (lexicon_id, phoneme_orig, phoneme_disc, note) VALUES (?,?,?,?)"));
      try {
        sql.setInt(1, lexiconIdFromName(lexicon, rdb));
        sql.setString(2, phoneme_orig);
        sql.setString(3, phoneme_disc);
        sql.setString(4, note);
        sql.executeUpdate();
        return null; // everything OK
      } finally {
        sql.close();
        rdb.close();
      }
    } catch (Exception x) {
      return Optional.of(x.getMessage()).orElse(x.toString());
    }
  }

  /**
   * Lists all phoneme mappings for the given lexicon.
   * @param lexicon Lexicon name.
   * @return List of Map object, the map keys are:
   *  <dl>
   *   <dt>phoneme_orig</dt> <dd> The phoneme label in the original encoding from the uploaded file. </dd>
   *   <dt>phoneme_disc</dt> <dd> The DISC-encoded version of the phoneme label. </dd>
   *   <dt>note</dt> <dd> A descriptive note about the phoneme label(s). </dd>
   *  </dl>
   */
  public Collection<Map<String,String>> readDiscMappings(String lexicon) {
    Vector<Map<String,String>> mappings = new Vector<Map<String,String>>();
    try {
      Connection rdb = newConnection();      
      PreparedStatement sql = rdb.prepareStatement(
        sqlx.apply("SELECT * FROM "+getAnnotatorId()+"_phoneme_map WHERE lexicon_id = ? ORDER BY phoneme_orig"));
      sql.setInt(1, lexiconIdFromName(lexicon, rdb));
      try {
        ResultSet rs = sql.executeQuery();
        try {
          while (rs.next()) {
            LinkedHashMap<String,String> m = new LinkedHashMap<String,String>();
            m.put("phoneme_orig", rs.getString("phoneme_orig"));
            m.put("phoneme_disc", rs.getString("phoneme_disc"));
            m.put("note", rs.getString("note"));
            mappings.add(m);
          }
        } finally {
          rs.close();
        }
      } finally {
        sql.close();
        rdb.close();
      }
    } catch (Exception x) {
      System.err.println("UnisynTagger.readDiscMappings(\""+lexicon+"\"): " + x);
      LinkedHashMap<String,String> m = new LinkedHashMap<String,String>();
      m.put("error", x.toString());
      mappings.add(m);
    }
    return mappings;
  }

  /**
   * Updates a given phone mapping in a given lexicon.
   * @param lexicon Lexicon name.
   * @param phoneme_orig The phoneme label to update.
   * @param phoneme_disc The new DISC label for the phoneme.
   * @param note The new note for the label.
   * @return An error if any, or null if not.
   */
  public String updateDiscMapping(String lexicon, String phoneme_orig, String phoneme_disc, String note) {
    Vector<LinkedHashMap> mappings = new Vector<LinkedHashMap>();
    try {
      Connection rdb = newConnection();      
      PreparedStatement sql = rdb.prepareStatement(
        sqlx.apply("UPDATE "+getAnnotatorId()+"_phoneme_map"
                   +" SET phoneme_disc = ?, note = ? WHERE lexicon_id = ? AND phoneme_orig = ?"));
      try {
        sql.setString(1, phoneme_disc);
        sql.setString(2, note);
        sql.setInt(3, lexiconIdFromName(lexicon, rdb));
        sql.setString(4, phoneme_orig);
        int rowCount = sql.executeUpdate();
        if (rowCount == 1) {
          return null; // everything OK
        } else {
          return "" + rowCount + " mappings updated";
        }
      } finally {
        sql.close();
        rdb.close();
      }
    } catch (Exception x) {
      System.err.println("UnisynTagger.updateDiscMappings(\""+lexicon+"\", \""+phoneme_orig+"\"): " + x);
      return Optional.of(x.getMessage()).orElse(x.toString());
    }
  }

  /**
   * Deletes a given phone mapping from the given lexicon.
   * @param lexicon Lexicon name.
   * @param phoneme_orig The phoneme label to update.
   * @return An error if any, or null if not.
   */
  public String deleteDiscMapping(String lexicon, String phoneme_orig) {
    Vector<LinkedHashMap> mappings = new Vector<LinkedHashMap>();
    try {
      Connection rdb = newConnection();      
      PreparedStatement sql = rdb.prepareStatement(
        sqlx.apply("DELETE FROM "+getAnnotatorId()+"_phoneme_map WHERE lexicon_id = ? AND phoneme_orig = ?"));
      try {
        sql.setInt(1, lexiconIdFromName(lexicon, rdb));
        sql.setString(2, phoneme_orig);
        int rowCount = sql.executeUpdate();
        if (rowCount == 1) {
          return null; // everything OK
        } else {
          return "" + rowCount + " mappings deleted";
        }
      } finally {
        sql.close();
        rdb.close();
      }
    } catch (Exception x) {
      System.err.println("UnisynTagger.deleteDiscMappings(\""+lexicon+"\", \""+phoneme_orig+"\"): " + x);
      return Optional.of(x.getMessage()).orElse(x.toString());
    }
  }

  /**
   * Provides the lexicon_id private key of the given lexicon name
   * @param lexicon Lexicon name.
   * @param rdb An open database connection.
   * @return lexicon_id database key, or -1 if there's no such lexicon, or -2 if an error occurs.
   */
  private int lexiconIdFromName(String lexicon, Connection rdb) {
    try {
      PreparedStatement sql = rdb.prepareStatement(
        sqlx.apply("SELECT lexicon_id FROM "+getAnnotatorId()+"_lexicon WHERE name = ?"));
      sql.setString(1, lexicon);
      try {
        ResultSet rs = sql.executeQuery();
        try {
          if (rs.next()) {
            return rs.getInt(1);
          } else {
            return -1;
          }
        } finally {
          rs.close();
        }
      } finally {
        sql.close();
      }
    } catch (Exception x) {
      System.err.println("UnisynTagger.lexiconId(\""+lexicon+"\"): " + x);
      return -2;
    }
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
  public UnisynTagger setTokenLayerId(String newTokenLayerId) {
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
  public UnisynTagger setTranscriptLanguageLayerId(String newTranscriptLanguageLayerId) {
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
  public UnisynTagger setPhraseLanguageLayerId(String newPhraseLanguageLayerId) {
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
  public UnisynTagger setTargetLanguagePattern(String newTargetLanguagePattern) {
    if (newTargetLanguagePattern != null // empty string means null
        && newTargetLanguagePattern.trim().length() == 0) {
      newTargetLanguagePattern = null;
    }
    targetLanguagePattern = newTargetLanguagePattern;
    return this;
  }
  
  /**
   * The ID of the lexicon to use.
   * @see #getLexicon()
   * @see #setLexicon(String)
   */
  protected String lexicon;
  /**
   * Getter for {@link #lexicon}: The ID of the lexicon to use.
   * @return The ID of the lexicon to use.
   */
  public String getLexicon() { return lexicon; }
  /**
   * Setter for {@link #lexicon}: The ID of the lexicon to use.
   * @param newLexicon The ID of the lexicon to use.
   */
  public UnisynTagger setLexicon(String newLexicon) { lexicon = newLexicon; return this; }

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
  public UnisynTagger setTagLayerId(String newTagLayerId) {
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
  public UnisynTagger setFirstVariantOnly(Boolean newFirstVariantOnly) {
    firstVariantOnly = newFirstVariantOnly; return this; }
  
  /**
   * Whether to strip syllable/stress markings or not.
   * @see #getStripSyllStress()
   * @see #setStripSyllStress(Boolean)
   */
  protected Boolean stripSyllStress;
  /**
   * Getter for {@link #stripSyllStress}: Whether to strip syllable/stress markings or not.
   * @return Whether to strip syllable/stress markings or not.
   */
  public Boolean getStripSyllStress() { return stripSyllStress; }
  /**
   * Setter for {@link #stripSyllStress}: Whether to strip syllable/stress markings or not.
   * @param newStripSyllStress Whether to strip syllable/stress markings or not.
   */
  public UnisynTagger setStripSyllStress(Boolean newStripSyllStress) {
    stripSyllStress = newStripSyllStress; return this; }
  
  /**
   * Which field to annotate the token with.
   * <p> Valid values are: <dl>
   * <dt>pron_disc</dt> <dd> The DISC-encoded pronunciation. </dd>
   * <dt>pron_orig</dt> <dd> The original pronunciation from the uploaded lexicon file. </dd>
   * <dt>pos</dt> <dd> The possible parts of speech. </dd>
   * <dt>frequency</dt> <dd> The word frequency. </dd>
   * <dt>enriched_orthography</dt> <dd> Wordform enriched with morphological markup. </dd>
   * <dt>syllable_count</dt> <dd> The number of syllables. </dd>
   * <dt>wordform</dt> <dd> The plain orthography of the word. </dd></dl> 
   * @see #getField()
   * @see #setField(String)
   */
  protected String field;
  /**
   * Getter for {@link #field}: Which field to annotate the token with.
   * @return Which field to annotate the token with.
   */
  public String getField() { return field; }
  /**
   * Setter for {@link #field}: Which field to annotate the token with.
   * @param newField Which field to annotate the token with.
   * Valid values are: <dl>
   * <dt>pron_disc</dt> <dd> The DISC-encoded pronunciation. </dd>
   * <dt>pron_orig</dt> <dd> The original pronunciation from the uploaded lexicon file. </dd>
   * <dt>pos</dt> <dd> The possible parts of speech. </dd>
   * <dt>frequency</dt> <dd> The word frequency. </dd>
   * <dt>enriched_orthography</dt> <dd> Wordform enriched with morphological markup. </dd>
   * <dt>syllable_count</dt> <dd> The number of syllables. </dd>
   * <dt>wordform</dt> <dd> The plain orthography of the word. </dd></dl> 
   */
  public UnisynTagger setField(String newField) { field = newField; return this; }
  
  /**
   * LayerId containing phones for which syllable structure should be recovered.
   * <p> If set, the annotator looks up the stress/syllable-tagged pronunciation for each
   * token, and uses it to partition the phones on this layer into stress-marked syllables
   * - i.e. an annotation will be created for each syllable, with start/end anchors
   * coinciding with the first/last phones on <var>phoneLayerId</var>.
   * @see #getPhoneLayerId()
   * @see #setPhoneLayerId(String)
   */
  protected String phoneLayerId;
  /**
   * Getter for {@link #phoneLayerId}: LayerId containing phones for which syllable
   * structure should be recovered. 
   * <p> If set, the annotator looks up the stress/syllable-tagged pronunciation for each
   * token, and uses it to partition the phones on this layer into stress-marked syllables
   * - i.e. an annotation will be created for each syllable, with start/end anchors
   * coinciding with the first/last phones on <var>syllableComponentLayerId</var>.
   * @return LayerId containing phones for which syllable structure should be recovered.
   */
  public String getPhoneLayerId() { return phoneLayerId; }
  /**
   * Setter for {@link #phoneLayerId}: LayerId containing phones for which syllable
   * structure should be recovered. 
   * <p> If set, the annotator looks up the stress/syllable-tagged pronunciation for each
   * token, and uses it to partition the phones on this layer into stress-marked syllables
   * - i.e. an annotation will be created for each syllable, with start/end anchors
   * coinciding with the first/last phones on <var>syllableComponentLayerId</var>.
   * @param newPhoneLayerId LayerId containing phones for which syllable structure should
   * be recovered. 
   */
  public UnisynTagger setPhoneLayerId(String newPhoneLayerId) {
    if (newPhoneLayerId != null // empty string means null
        && newPhoneLayerId.trim().length() == 0) {
      newPhoneLayerId = null;
    }
    phoneLayerId = newPhoneLayerId;
    return this;
  }
  
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

    if (parameters == null) { // apply default configuration
         
      if (schema.getLayer("orthography") != null) {
        tokenLayerId = "orthography";
      } else {
        tokenLayerId = schema.getWordLayerId();
      }
      stripSyllStress = Boolean.TRUE;
      firstVariantOnly = Boolean.FALSE;
      phoneLayerId = null;
      
      // default transcript language layer
      Layer[] candidates = schema.getMatchingLayers(
        layer -> schema.getRoot().getId().equals(layer.getParentId())
        && layer.getAlignment() == Constants.ALIGNMENT_NONE // transcript attribute
        && layer.getId().matches(".*lang.*")); // with 'lang' in the name
      if (candidates.length > 0) transcriptLanguageLayerId = candidates[0].getId();
            
      // default phrase language layer
      candidates = schema.getMatchingLayers(
        layer -> schema.getTurnLayerId().equals(layer.getParentId()) // child of turn
        && layer.getId().matches(".*lang.*")); // with 'lang' in the name
      if (candidates.length > 0) phraseLanguageLayerId = candidates[0].getId();

      // default output layer
      candidates = schema.getMatchingLayers(
        layer -> schema.getWordLayerId().equals(layer.getParentId())
        && layer.getAlignment() == Constants.ALIGNMENT_NONE // word tag
        && (layer.getId().matches(".*phoneme.*") || layer.getId().matches(".*pronunciation.*")));
      if (candidates.length > 0) {
        tagLayerId = candidates[0].getId();
      } else { // suggest adding a new one
        tagLayerId = "phonemes";
      }
      // default lexicon if possible
      List<String> dictionaries = getDictionaryIds();
      if (dictionaries.size() > 0) { // there are dictionaries
        // default to the first, which will be lexicon:wordform->pron_orig
        lexicon = dictionaries.get(0).replaceAll(":.*","");
      } // there are dictionaries
        // default field - DISC
      field = "pron_disc";         
    } else {
      // start from unset state
      tokenLayerId = null;
      tagLayerId = null;
      transcriptLanguageLayerId = null;
      phraseLanguageLayerId = null;
      targetLanguagePattern = null;
      lexicon = null;
      stripSyllStress = null;
      firstVariantOnly = Boolean.FALSE;
      phoneLayerId = null;

      // set state from parameters
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
    if (phoneLayerId != null) {
      if (schema.getLayer(phoneLayerId) == null) 
        throw new InvalidConfigurationException(
          this, "Phone layer not found: " + phoneLayerId);
      // stripping syllable boundaries makes no sense if we're recovering syllable structure
      stripSyllStress = Boolean.FALSE;
    }
    if (lexicon == null || lexicon.length() == 0)
      throw new InvalidConfigurationException(this, "Lexicon not specified");
    try { // check dictionary configuration is valid
      Dictionary dict = getDictionary();
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
    if (stripSyllStress == null) stripSyllStress = Boolean.FALSE;
    if (field == null) field = "pron_disc";
    // ensure field is valid
    if (!field.equals("wordform")
        && !field.equals("pos")
        && !field.equals("pron_orig")
        && !field.equals("pron_disc")
        && !field.equals("enriched_orthography")
        && !field.equals("frequency")
        && !field.equals("syllable_count")) {
      throw new InvalidConfigurationException(this, "Invalid field: " + field);
    }
      
    // does the outputLayer need to be added to the schema?
    Layer tagLayer = schema.getLayer(tagLayerId);
    if (tagLayer == null) {
      schema.addLayer(
        new Layer(tagLayerId)
        .setAlignment(phoneLayerId == null?Constants.ALIGNMENT_NONE:Constants.ALIGNMENT_INTERVAL)
        .setPeers(!firstVariantOnly)
        .setParentId(schema.getWordLayerId())
        .setType(
          "pron_disc".equals(field)?Constants.TYPE_IPA
          :"frequency".equals(field) || "syllable_count".equals(field)?Constants.TYPE_NUMBER:
          Constants.TYPE_STRING));
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
    if (phoneLayerId !=null) requiredLayers.add(phoneLayerId);
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
      String wordLayerId = tokenLayerId;
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
        Dictionary dictionary = getDictionary();
        try {
          int t = 0;
          int typeCount = toAnnotate.size();
          setStatus("Distinct words: " + typeCount);
          setPercentComplete(0);
          for (String type : toAnnotate.keySet()) { // for each type
            if (isCancelling()) break;
            if (phoneLayerId != null) { // syllable recovery
              
              recoverSyllables(toAnnotate.get(type), dictionary.lookup(type));
              
            } else { // regular lookup/tag

              boolean found = false;
              HashSet<String> soFar = new HashSet<String>(); // only unique entries
              for (String entry : dictionary.lookup(type)) {
                
                if (!found) setStatus("Tagging: " + type); // (log this only once)
                found = true;
                
                if (stripSyllStress) {
                  entry = entry.replaceAll(
                    // replace characters in this class
                    "['\",-]" // TODO don't assume DISC
                    // also any trailing space in case phonemes are space-delimited,
                    // extra spaces aren't left behind
                    +" *","");
                }
                if (!soFar.contains(entry)) { // duplicates are possible if stripSyllStress
                  for (Annotation token : toAnnotate.get(type)) {
                    token.createTag(tagLayerId, entry)
                      .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
                  } // for each token
                  soFar.add(entry);
                }
              
                // do we want the first entry only?
                if (firstVariantOnly) break;
              } // next entry
            } // regular lookup/tag
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
   * Recover syllables for all the given tokens of the given type.
   * @param tokens Word tokens to recover the syllables of.
   * @param pronunciations Syllable/stress marked possible pronunciations from dictionary.
   */
  protected void recoverSyllables(Vector<Annotation> tokens, List<String> pronunciations) {
    if (pronunciations.size() == 0) return; // no pronunciation entries
    if (tokens.size() == 0) return; // no tokens
    Graph graph = tokens.get(0).getGraph();    
    
    for (Annotation token : tokens) {

      if (token.first(tagLayerId) != null) {
        setStatus(
          "Skipping token with existing " + tagLayerId + " annotations: "
          + token.getLabel() + " ("+token.getId()+")");
        continue;
      }
      Annotation phones[] = token.all(phoneLayerId);
      if (phones.length > 0) { // there are phones to recover syllabification from
        
        // find the best entry for these segments
        final String concatenatedSegments = Arrays.stream(phones)
          .map(segment -> segment.getLabel())
          .collect(Collectors.joining());
        final String stressMarkers = field.equals("pron_disc")?"['\",]":"[*~-]";
        final String syllableStressMarkers = field.equals("pron_disc")?"['\",-]":"[.*~-]";
        final Character syllableBoundary = field.equals("pron_disc")?'-':'.';
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
        setStatus("Token " + token + " -> " + bestPron);

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
            graph.createSpan​(firstPhone, lastPhone, tagLayerId, label.toString(), token)
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
        graph.createSpan​(firstPhone, lastPhone, tagLayerId, label.toString(), token)
          .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
                
      } // there are phones
    } // token doesn't already have syllables
  } // end of recoverSyllables()
  
  /**
   * Registers a token for annotation, by adding it to the list in the given map of labels
   * to annotations with that label.
   * @param token The token to annotate.
   * @param toAnnotate A map of label→Annotations that may or may not contain an entry for
   * this token's label
   */
  protected void registorForAnnotation(
    Annotation token, TreeMap<String,Vector<Annotation>> toAnnotate) {
    String lookup = token.getLabel();
    if (lookup.length() > 0) {
      if (!toAnnotate.containsKey(lookup)) {
        toAnnotate.put(lookup, new Vector<Annotation>());
      }
      toAnnotate.get(lookup).add(token);
    }
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
    Dictionary dictionary = getDictionary();
    try {
      
      store.deleteMatchingAnnotations(
        "layerId = '"+esc(tagLayerId)+"'"
        +" && first('"+esc(tokenLayerId)+"').label == '"+esc(sourceLabel)+"'");

      String tokenExpression = "layerId = '"+esc(tokenLayerId)+"'"
        +" && label = '"+esc(sourceLabel)+"'";
      if (transcriptLanguageLayerId != null && targetLanguagePattern != null) {
        tokenExpression += " && /"+targetLanguagePattern+"/.test(first('"
          +esc(transcriptLanguageLayerId)+"').label)";
        // TODO take phraseLanguageLayerId into account
      }
      int count = 0;
      HashSet<String> soFar = new HashSet<String>(); // only unique entries
      for (String tag : dictionary.lookup(sourceLabel)) {        
        if (stripSyllStress) {
          tag = tag.replaceAll(
            // replace characters in this class
            "['\",-]" // TODO don't assume DISC
            // also any trailing space in case phonemes are space-delimited,
            // extra spaces aren't left behind
            +" *","");
        }
        if (!soFar.contains(tag)) { // duplicates are possible if stripSyllStress
          store.tagMatchingAnnotations(
            tokenExpression, tagLayerId, tag, Constants.CONFIDENCE_AUTOMATIC);
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
    if (phoneLayerId != null) { // syllable recovery should be done one transcript at a time
      super.transformTranscripts​(store, expression);
      return;
    }
    
    setRunning(true);
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
      String[] distinctWords = store.aggregateMatchingAnnotations(
        "DISTINCT", labelExpression.toString());
      setStatus("There are "+distinctWords.length+" distinct token labels");
      int w = 0;
      Dictionary dictionary = getDictionary();
      // for each label
      for (String word : distinctWords) {
        if (isCancelling()) break;
        HashSet<String> soFar = new HashSet<String>(); // only unique entries
        for (String tag : dictionary.lookup(word)) {
          if (isCancelling()) break;
          if (stripSyllStress) {
            tag = tag.replaceAll(
              // replace characters in this class
              "['\",-]" // TODO don't assume DISC
              // also any trailing space in case phonemes are space-delimited,
              // extra spaces aren't left behind
              +" *","");
          }
          if (!soFar.contains(tag)) { // duplicates are possible if stripSyllStress
            StringBuilder tokenExpression = new StringBuilder(labelExpression);
            tokenExpression.append(" && label == '").append(esc(word)).append("'");
            setStatus(word+" → "+tag);
            store.tagMatchingAnnotations(
              tokenExpression.toString(), tagLayerId, tag, Constants.CONFIDENCE_AUTOMATIC);
            soFar.add(tag);
          }
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
   * Lists the lexicons implemented by this Annotator.
   * <p> Each lexicon corresponds to one uploaded lexicon file. Each lexicon has multiple
   * dictionaries; one for each mapping from wordform to the other lexicon fields.
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
  public List<String> getLexiconIds() {
    List<String> ids = new Vector<String>();
    try {
      Connection rdb = newConnection();      
      try {
        // for each lexicon
        PreparedStatement sql = rdb.prepareStatement(
          sqlx.apply("SELECT lexicon_id, name FROM "+getAnnotatorId()+"_lexicon ORDER BY name"));
        ResultSet rs = sql.executeQuery();
        while (rs.next()) {
          ids.add(rs.getString("name"));
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
   * Lists the dictionaries implemented by this Annotator.
   * <p> Each lexicon corresponds to one uploaded lexicon file. Each lexicon has multiple
   * dictionaries; one for each mapping from wordform to the other lexicon fields.
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
          ids.add(name+":wordform->pron_orig");
          ids.add(name+":wordform->pron_disc");
          ids.add(name+":wordform->enriched_orthography");
          ids.add(name+":wordform->pos");
          ids.add(name+":wordform->frequency");
          ids.add(name+":wordform->syllable_count");
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
   * Gets the dictionary corresponding to the current configuration.
   * <p> This method can assume that the following methods have been previously called:
   * <ul>
   *  <li> {@link Annotator#setSchema(Schema)} </li>
   *  <li> {@link Annotator#setTaskParameters(String)} </li>
   *  <li> {@link Annotator#setRdbConnectionFactory(ConnectionFactory)} </li>
   * </ul>
   * @return The current dictionary.
   * @throws DictionaryException If the given dictionary doesn't exist.
   */
  public Dictionary getDictionary() throws DictionaryException {
    try {
      String keyField = "wordform";
      String valueField = field;
      return getDictionary(lexicon, keyField, valueField);
    } catch (PatternSyntaxException sqlP) {
      throw new DictionaryException(null, sqlP);
    }
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
      // dictionary IDs are formatted "${lexicon}:${keyField}->${valueField}"
      Matcher idParser = Pattern.compile("^(?<lexicon>[^:]+):(?<keyField>.+)->(?<valueField>.+)$")
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
      return new UnisynDictionary(this, newConnection(), sqlx, lexicon, keyField, valueField);
    } catch(SQLException sqlX) {
      throw new DictionaryException(null, sqlX);
    }
  } // end of getDictionary()

}
