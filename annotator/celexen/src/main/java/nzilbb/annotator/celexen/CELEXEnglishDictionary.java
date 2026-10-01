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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Pattern;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.Dictionary;
import nzilbb.ag.automation.DictionaryException;
import nzilbb.ag.automation.DictionaryReadOnlyException;
import nzilbb.encoding.CMU2DISC;
import nzilbb.encoding.DISC2CMU;
import nzilbb.sql.mysql.MySQLTranslator;

/**
 * Dictionary for lexical entries (pronunciations, frequency, etc.) according to the 
 * <a href="https://catalog.ldc.upenn.edu/LDC96L14"> CELEX English lexicon </a>.
 */
@SuppressWarnings("serial")
public class CELEXEnglishDictionary implements Dictionary {

  public static final long IDNUM_PARTITION_START = 200000;
  private Connection rdb;
  private MySQLTranslator sqlx = new MySQLTranslator();
  private PreparedStatement sql; 
  private PreparedStatement sqlRaw; 
  private String field;

  /**
   * The annotator that created this dictionary.
   * @see #getAnnotator()
   */
  protected Annotator annotator;
  /**
   * {@link Dictionary} method - Provides the annotator that implements the dictionary.
   * @return The dictionary's annotator.
   */
  public Annotator getAnnotator() {
    return annotator;
  }
  
  /**
   * Name of the dictionary, which is the {@link #query} by default.
   * @see #getName()
   * @see #setName(String)
   */
  protected String name;
  /**
   * Getter for {@link #name}: Name of the dictionary, or {@link #query} if no name has
   * been set.
   * @return Name of the dictionary.
   */
  public String getName() { return Optional.ofNullable(name).orElse(query); }
  /**
   * Setter for {@link #name}: Name of the dictionary.
   * @param newName Name of the dictionary.
   */
  public CELEXEnglishDictionary setName(String newName) { name = newName; return this; }
  
  /**
   * SQL query that returns entries.
   * @see #getQuery()
   * @see #setQuery(String)
   */
  protected String query;
  /**
   * Getter for {@link #query}: SQL query that returns entries.
   * @return SQL query that returns entries.
   */
  public String getQuery() { return query; }
  /**
   * Setter for {@link #query}: SQL query that returns entries.
   * @param newQuery SQL query that returns entries.
   */
  public CELEXEnglishDictionary setQuery(String newQuery) { query = newQuery; return this; }
  
  /**
   * {@link Dictionary} method - Name of the dictionary, which must be unique among the
   * dictionaries implemented by the same annotator.
   * @return The dictionary's ID.
   */
  public String getDictionaryId() {
    return query;
  }

  /**
   * Constructor.
   */
  public CELEXEnglishDictionary(
    CELEXEnglishTagger annotator, Connection rdb, MySQLTranslator translator,
    String definition) throws SQLException {
    this.annotator = annotator;
    this.rdb = rdb;
    this.sqlx = translator;

    if (definition.equals("Cobuild Frequency (wordform)")) {
      setName(definition);
      setQuery("SELECT cxen_wordform.Cob"
               +"\n FROM cxen_wordform"
               +"\n INNER JOIN cxen_wordformortho"
               +"\n ON cxen_wordformortho.IdNum = cxen_wordform.IdNum"
               +"\n WHERE cxen_wordformortho.WordDia = ?"
               +"\n ORDER BY cxen_wordform.Cob DESC");
      field = "cxen_wordform.Cob";
    } else if (definition.equals("Cobuild Frequency (lemma)")) {
      setName(definition);
      setQuery("SELECT cxen_lemma.Cob"
               +"\n FROM cxen_wordform"
               +"\n INNER JOIN cxen_wordformortho"
               +"\n ON cxen_wordformortho.IdNum = cxen_wordform.IdNum"
               +"\n INNER JOIN cxen_lemma"
               +"\n ON cxen_wordform.IdNumLemma = cxen_lemma.IdNumLemma"
               +"\n WHERE cxen_wordformortho.WordDia = ?"
               +"\n ORDER BY cxen_lemma.Cob DESC");
      field = "cxen_lemma.Cob";
    } else if (definition.equals("Phonology (wordform)")) {
      setName(definition);
      setQuery(
        "SELECT DISTINCT BINARY cxen_wordformphonologypron.PhonStrsDISC, cxen_wordformphonologypron.Variant, cxen_wordform.IdNum"
        +"\n FROM cxen_wordformphonologypron"
        +"\n INNER JOIN cxen_wordformortho"
        +"\n ON cxen_wordformphonologypron.IdNum = cxen_wordformortho.IdNum"
        +"\n INNER JOIN cxen_wordform"
        +"\n ON cxen_wordformphonologypron.IdNum = cxen_wordform.IdNum"
        +"\n WHERE cxen_wordformortho.WordDia = ?"
        +"\n ORDER BY cxen_wordformphonologypron.Variant, cxen_wordform.IdNum");
      field = "cxen_wordformphonologypron.PhonStrsDISC";
    } else if (definition.equals("Morphology (wordform)")) {
      setName(definition);
      setQuery(
        "SELECT"
        +"\n COALESCE(CONCAT(COALESCE(cxen_lemmamorphologyparse.Imm,cxen_lemma.HeadDia,''),"
        +"\n REPLACE(COALESCE(cxen_wordformmorphology.TransInfl,''), '@','')),'')"
        +"\n FROM cxen_wordformmorphology"
        +"\n INNER JOIN cxen_wordform ON cxen_wordformmorphology.IdNum = cxen_wordform.IdNum"
        +"\n INNER JOIN cxen_wordformortho"
        +" ON cxen_wordformmorphology.IdNum = cxen_wordformortho.IdNum"
        +"\n INNER JOIN cxen_lemma ON cxen_wordform.IdNumLemma = cxen_lemma.IdNumLemma"
        +"\n INNER JOIN cxen_lemmamorphologyparse"
        +" ON cxen_wordform.IdNumLemma = cxen_lemmamorphologyparse.IdNumLemma"
        +"\n WHERE cxen_wordformortho.WordDia = ?"
        +"\n ORDER BY cxen_lemma.Cob DESC");
      field = "cxen_wordformmorphology.TransInfl";
    } else if (definition.equals("Syntax (wordform)")) {
      setName(definition);
      setQuery(
        "SELECT cxen_wordclass.Label"
        +"\n FROM cxen_wordform"
        +"\n INNER JOIN cxen_wordformortho"
        +" ON cxen_wordform.IdNum = cxen_wordformortho.IdNum"
        +"\n INNER JOIN cxen_lemma ON cxen_wordform.IdNumLemma = cxen_lemma.IdNumLemma"
        +"\n INNER JOIN cxen_lemmasyntax"
        +" ON cxen_wordform.IdNumLemma = cxen_lemmasyntax.IdNumLemma"
        +"\n LEFT OUTER JOIN cxen_wordclass"
        +" ON cxen_lemmasyntax.ClassNum = cxen_wordclass.ClassNum"
        +"\n WHERE cxen_wordformortho.WordDia = ?"
        +"\n ORDER BY cxen_lemma.Cob DESC");
      field = "cxen_wordclass.Label";
    } else {
      setQuery(definition);
      setQuery(definition);
    }
    sql = rdb.prepareStatement(sqlx.apply(this.query));

    if (field == null) {
      String[] possibleFields = { "PhonStrsDISC", "PhonCVBr", "PhonSylBCLX" };
      for (String possibleField : possibleFields) {
        if (this.query.indexOf(possibleField) > 0) {
          field = possibleField;
          break;
        }
      } // next possibility
    }
    
    String rawQuery = Pattern.compile(
      "SELECT\\s.+\\sAS\\slabel", Pattern.MULTILINE + Pattern.DOTALL)
      .matcher(this.query).replaceAll("SELECT DISTINCT BINARY " + field);
    sqlRaw = rdb.prepareStatement(sqlx.apply(rawQuery));
  }

  /**
   * Looks up a word and provides possible matches.
   * @param key The key to look up.
   * @param supplementalOnly Whether to return only supplemental entries (true) or all
   * entries (false) 
   * @return a Vector of Strings, one for each entry for the given word
   * @throws SQLException
   */
  protected Vector<String> lookupEntries(String key, boolean supplementalOnly)
    throws SQLException {      
    Vector<String> queryResults = new Vector<String>();
    if (key != null) {
      key = key.toLowerCase();
      if (supplementalOnly) { // only return entries for newly-added words
        try (PreparedStatement sqlSupplemental = rdb.prepareStatement(
               sqlx.apply(
                 "SELECT WordDia FROM cxen_wordformortho WHERE IdNum >= ? AND WordDia = ?"))) {
          sqlSupplemental.setLong(1, IDNUM_PARTITION_START);
          sqlSupplemental.setString(2, key);
          try (ResultSet rsSupplemental = sql.executeQuery()) {
            if (!rsSupplemental.next()) { // not editable
              return queryResults; // empty
            }
          } // close rsSupplemental
        } // close sqlSupplemental
      }
      sql.setString(1, key);
      try (ResultSet rs = sql.executeQuery()) {
        while (rs.next()) {
          queryResults.add(rs.getString(1));
        } // next result
      } catch(SQLException exception) {
        // ignore collation errors - it means we're looking up something that's not Latin
        // which isn't in CELEX anyway
        if (exception.getMessage().indexOf("Illegal mix of collations") < 0) {
          // not the exception we're looking for, so throw it
          throw exception;
        }
      } // rs.close()
    }
    return queryResults;
  }

  /**
   * Looks up a key and provides all possible matches, in their <q>raw</q>
   * representation for dictionary editing.   
   */
  public List<String> lookupRaw(String key) throws DictionaryException {
    Vector<String> queryResults = new Vector<String>();
    if (key != null) {
      key = key.toLowerCase();
      try {
        sql.setString(1, key);
        try (ResultSet rs = sqlRaw.executeQuery()) {
          while (rs.next()) {
            queryResults.add(rs.getString(1));
          } // next result
        } // close rs
      } catch(SQLException exception) {
        // ignore collation errors - it means we're looking up something that's not Latin
        // which isn't in CELEX anyway
        if (exception.getMessage().indexOf("Illegal mix of collations") < 0) {
          // not the exception we're looking for, so throw it
          throw new DictionaryException(this, exception);
        }
      } // rs.close()
    }
    return queryResults;
  }
  
  /** 
   * {@link Dictionary} method - Look up all entries for the given key.
   */ 
  public List<String> lookup(String key) throws DictionaryException {
    try {
      return lookupEntries(key, false);
    } catch (SQLException sqlX) {
      throw new DictionaryException(this, sqlX);
    }
  }

  /**
   * {@link Dictionary} method - Returns a count of all entries in the dictionary.
   * @return the number of keys that would be returned by a call to listAllEntries
   */
  public int countAllKeys() throws DictionaryException {
    // TODO ?? if (isReadOnly()) throw new DictionaryReadOnlyException(iLayerId);
    try {
      try (PreparedStatement sql = rdb.prepareStatement(
             sqlx.apply("SELECT COUNT(*) FROM cxen_wordformortho"))) {
        try (ResultSet rs = sql.executeQuery()) {
          if (!rs.next()) return 0;
          return rs.getInt(1);
        } // rs.close()
      } // sql.close()
    } catch(Throwable x) {
      System.err.println("CELEXEnglishDictionary.countAllKeys: " + x);
      return 0;
    }
  }

  /**
   * {@link Dictionary} method - Returns a sub-list of all entries in the dictionary
   * - i.e. allentries between the two specified indexes.  This primarily allows LaBB-CAT to
   * paginate the list on the Edit Dictionary page.
   * @param start the(zero-based) index of the first entry to list
   * @param length the number of entries to return, or 0, meaning to the end of the list
   * @return a map of keys to their definitions.
   * @throws DictionaryReadOnlyException
   * @throws Exception
   */
  public Map<String, List<String>> listAllEntries(int start, int length)
    throws DictionaryReadOnlyException, DictionaryException {
    try {
      Map<String,List<String>> words = new LinkedHashMap<String,List<String>>();
      try (PreparedStatement sql = rdb.prepareStatement(
             sqlx.apply(
               "SELECT WordDia FROM cxen_wordformortho"
               + " ORDER BY WordDia "
               + (length > 0? " LIMIT " + start + ", " + length:"")))) {
        try (ResultSet rs = sql.executeQuery()) {
          while (rs.next()) {
            String word = rs.getString("wordform");
            Vector<String> entries = lookupEntries(word, false);
            if (entries.size() == 0) continue;
            words.put(word, entries);
          } // next word
        } // rs.close()
      } // sql.close()
      return words;
    } catch (SQLException sqlX) {
      throw new DictionaryException(this, sqlX);
    }
  }
      
  /**
   * {@link Dictionary} method - Determines whether the dictionary is entirely read-only, or
   * has facility for adding/editing entries.
   * @return true if there is no facility to add/edit entries in the
   * dictionary, false otherwise
   */
  public boolean isReadOnly() {
    return field == null;
  }

  /**
   * Returns a count of all editable keys in the dictionary.
   * @return the number of keys that would be returned by a call to listEditableKeys
   * @throws DictionaryReadOnlyException
   */
  public int countEditableKeys() throws DictionaryReadOnlyException, DictionaryException {
    try (PreparedStatement sql = rdb.prepareStatement(
           sqlx.apply("SELECT COUNT(*) FROM cxen_wordformortho WHERE IdNum >= ?"))) {
      sql.setLong(1, IDNUM_PARTITION_START);      
      try (ResultSet rs = sql.executeQuery()) {
        if (!rs.next()) return 0;
        return rs.getInt(1);
      } // rs.close()
    } catch (SQLException sqlX) {
      throw new DictionaryException(this, sqlX);
    } // sql.close()
  }
  /**
   * {@link Dictionary} method - Returns a count of all editable entries in the dictionary.
   * @return the number of entries that would be returned by a call to listEditableEntries
   * @throws DictionaryReadOnlyException
   */
  public int countEditableEntries()
    throws DictionaryReadOnlyException, DictionaryException {
    return countEditableKeys(); // TODO check this
  }

  /**
   * {@link Dictionary} method - Returns an aggregrate of all values of all entries in the
   * dictionary.
   * @param operation Any aggregate operation supported by SQL for VARCHAR fields.. 
   * @return The result of the aggregation, or null if the operation is not supported.
   */
  public String aggregateEntries(String operation) throws DictionaryException {
    String query = null;
    if (field.equals("cxen_wordform.Cob")) {
      query = "SELECT "+operation+"(cxen_wordform.Cob)"
        +"\n FROM cxen_wordform"
        +"\n INNER JOIN cxen_wordformortho"
        +"\n ON cxen_wordformortho.IdNum = cxen_wordform.IdNum";
    } else if (field.equals("cxen_lemma.Cob")) {
      query = "SELECT "+operation+"(cxen_lemma.Cob)"
        +"\n FROM cxen_wordform"
        +"\n INNER JOIN cxen_wordformortho"
        +"\n ON cxen_wordformortho.IdNum = cxen_wordform.IdNum"
        +"\n INNER JOIN cxen_lemma"
        +"\n ON cxen_wordform.IdNumLemma = cxen_lemma.IdNumLemma";
    } else {
      return null;
    }
    
    try (PreparedStatement sqlAggregate = rdb.prepareStatement(sqlx.apply(query))) {
      try (ResultSet rsAggregate = sqlAggregate.executeQuery()) {
        rsAggregate.next();
        return rsAggregate.getString(1);
      } // rsAggregate.close()
    } catch (SQLException x) {
      throw new DictionaryException(this, x);
    } // sqlAggregate.close()
  }

  /**
   * {@link Dictionary} method - Returns an aggregrate of all entries (i.e. keys) in the
   * dictionary.
   * @param operation Any aggregate operation supported by SQL for VARCHAR fields.. 
   * @return The result of the aggregation, or null if the operation is not supported.
   * @throws DictionaryException
   */
  public String aggregateKeys(String operation) throws DictionaryException {    
    String query = null;
    if (field.equals("cxen_wordform.Cob")) {
      query = "SELECT "+operation+"(cxen_wordformortho.WordDia)"
        +"\n FROM cxen_wordform"
        +"\n INNER JOIN cxen_wordformortho"
        +"\n ON cxen_wordformortho.IdNum = cxen_wordform.IdNum";
    } else if (field.equals("cxen_lemma.Cob")) {
      query = "SELECT "+operation+"(cxen_wordformortho.WordDia)"
        +"\n FROM cxen_wordform"
        +"\n INNER JOIN cxen_wordformortho"
        +"\n ON cxen_wordformortho.IdNum = cxen_wordform.IdNum"
        +"\n INNER JOIN cxen_lemma"
        +"\n ON cxen_wordform.IdNumLemma = cxen_lemma.IdNumLemma";
    } else {
      return null;
    }
    try {
      try (PreparedStatement sqlAggregate = rdb.prepareStatement(sqlx.apply(query))) {
        try (ResultSet rsAggregate = sqlAggregate.executeQuery()) {
          rsAggregate.next();
          return rsAggregate.getString(1);
        } // rsAggregate.close()
      } // sqlAggregate.close()
      
    } catch (SQLException x) {
      throw new DictionaryException(this, x);
    }
  }

  /**
   * Looks up the IdNum of the given word
   * @param sWordform
   * @return IdNum of the given wordform, or -1 if the wordform is not present
   */
  private long getIdNumFromWordForm(String sWordform) {
    try (PreparedStatement sql = rdb.prepareStatement(
           sqlx.apply(
             "SELECT IdNum FROM cxen_wordformortho WHERE WordDia = ?"))) {
      sql.setString(1, sWordform);
      try (ResultSet rs = sql.executeQuery()) {
        if (rs.next()) {
          return rs.getLong("IdNum");
        } else {
          return -1;
        }
      } // rs.close()
    } catch(SQLException exception) {
      System.err.println("CELEXEnglishDictionary.getIdNumFromWordForm: " + exception);
      return -2;
    } // sql.close()
  } // end of getIdNumFromWordForm()
  
  /**
   * Determines the next free IdNum.
   * @return the next unused IdNum
   * @throws SQLException
   */
  public long nextIdNum() throws SQLException {
    try (PreparedStatement sql = rdb.prepareStatement(
           sqlx.apply("SELECT MAX(IdNum) + 1 FROM cxen_wordform"))) {
      try (ResultSet rs = sql.executeQuery()) {
        rs.next();
        long lNextId = rs.getLong(1);
        if (lNextId < IDNUM_PARTITION_START) {
          return IDNUM_PARTITION_START;
        } else {
          return lNextId;
        }
      } // rs.close()
    } catch (Exception ex) {
      return IDNUM_PARTITION_START;
    } // sql.close()
  } // end of nextIdNum()
  
  /**
   * Adds a wordform/lemma to the CELEX DB if it's not already there.
   * @param sWordform
   * @return the new IdNum of the word, or th existing IdNum, if it was already there.
   */
  public long ensureWordExists(String sWordform) throws SQLException {
    // check whether the word's already there...
    long lIdNum = getIdNumFromWordForm(sWordform);
    if (lIdNum >= 0) {
      // word already exists, just return the IdNum
      return lIdNum;
    } else {
      // get IdNum
      lIdNum = nextIdNum();
      
      // create lemma record
      try (PreparedStatement sql = rdb.prepareStatement(
             sqlx.apply(
               "INSERT INTO cxen_lemma (IdNumLemma, HeadDia, Cob) VALUES (?,?,?)"))) {
        sql.setLong(1, lIdNum);
        sql.setString(2, sWordform);
        sql.setInt(3,0);
        sql.executeUpdate();
      } // sql.close()
	    
      // create wordform records
      try (PreparedStatement sql = rdb.prepareStatement(
             sqlx.apply(
               "INSERT INTO cxen_wordform (IdNum, IdNumLemma, Cob) VALUES (?,?,?)"))) {
        sql.setLong(1, lIdNum);
        sql.setLong(2, lIdNum);
        sql.setInt(3,0);
        sql.executeUpdate();
      } // sql.close()

      try (PreparedStatement sql = rdb.prepareStatement(
             sqlx.apply(
               "INSERT INTO cxen_wordformortho (IdNum, Variant, WordDia) VALUES (?,?,?)"))){
        sql.setLong(1, lIdNum);
        sql.setLong(2, 0);
        sql.setString(3, sWordform);
        sql.executeUpdate();
      } // sql.close()
      return lIdNum;
    }
  } // end of ensureWordExists()
  
  /**
   * {@link Dictionary} method - Adds a entry to the dictionary.
   * @param key The key to add an entry for - e.g. the entry orthorgraphy.
   * @param entry The entry for the key - e.g. its pronunciation.
   * @return the ID for the entry in the dictionary, if appropriate
   */
  public String add(String key, String entry)
    throws DictionaryReadOnlyException, DictionaryException {

    try {
      long lIdNum = ensureWordExists(key);
      if (lIdNum < IDNUM_PARTITION_START) {
        throw new DictionaryReadOnlyException(this, key + " is read only.");
      }
      // get the next variant of the word
      int iVariant = 1;
      // get the next available pronunciation variant
      try (PreparedStatement sql = rdb.prepareStatement(
             sqlx.apply("SELECT MAX(Variant) + 1 FROM cxen_wordformphonologypron"
                        +" WHERE IdNum = ?"))) {
        sql.setLong(1, lIdNum);
        try (ResultSet rs = sql.executeQuery()) {
          rs.next();
          iVariant = rs.getInt(1);
        } // close rs
      } catch (Exception ex) {
        // doesn't exist - create the wordformphonology record
        try (PreparedStatement sqlInsert = rdb.prepareStatement(
               sqlx.apply("INSERT INTO cxen_wordformphonology (IdNum, Cob) VALUES (?,?)"))) {
          sqlInsert.setLong(1, lIdNum);
          sqlInsert.setInt(2,0);
          sqlInsert.executeUpdate();
          iVariant = 1;
        } // sqlInsert.close()
      } // sql.close()
      
      // insert the pronunciation
      try (PreparedStatement sql = rdb.prepareStatement(
             sqlx.apply("INSERT INTO cxen_wordformphonologypron (IdNum, Variant, " + field 
                        + ") VALUES (?,?,?)"))) {
        sql.setLong(1, lIdNum);
        sql.setInt(2, iVariant);
        sql.setString(3, entry);
        sql.executeUpdate();
        return ""+lIdNum;
      } // close sql
    } catch (SQLException sqlX) {
      throw new DictionaryException(this, sqlX);
    } // sql.close()
  }

  /**
   * {@link Dictionary} method - Removes a key from the dictionary.
   * @param key
   * @return the ID for the entry in the dictionary, if appropriate
   * @throws DictionaryReadOnlyException
   */
  public String remove(String key)
    throws DictionaryReadOnlyException, DictionaryException {

    if (isReadOnly()) throw new DictionaryReadOnlyException(this);
    
    long lIdNum = getIdNumFromWordForm(key);	 
    if (lIdNum < IDNUM_PARTITION_START) {
      throw new DictionaryReadOnlyException(this, key + " is read only.");
    }
    try (PreparedStatement sql = rdb.prepareStatement(
           // TODO should delete all entries in all tables
           sqlx.apply("DELETE FROM cxen_wordformphonologypron WHERE IdNum = ?"))) {
      sql.setLong(1, lIdNum);
      sql.executeUpdate();
    } catch (SQLException x) {
      throw new DictionaryException(this, x);
    } // sql.close()
    
    return "" +lIdNum;
  }

  /**
   * {@link Dictionary} method - Removes a entry entry from the dictionary.
   * @param key
   * @param entry
   * @return the ID for the entry in the dictionary, if appropriate
   * @throws DictionaryReadOnlyException
   */
  public String remove(String key, String entry)
    throws DictionaryReadOnlyException, DictionaryException {
    if (isReadOnly()) {
      throw new DictionaryReadOnlyException(this);
    }
    try {
      
      long lIdNum = getIdNumFromWordForm(key);
      if (lIdNum < IDNUM_PARTITION_START) {
        throw new DictionaryReadOnlyException(this, key + " is read only");
      }
      
      try (PreparedStatement sql = rdb.prepareStatement(
             sqlx.apply(
               "DELETE FROM cxen_wordformphonologypron WHERE IdNum = ?"
               +" AND " + field + " = ?"))) {
        sql.setLong(1, lIdNum);
        sql.setString(2, entry);
        sql.executeUpdate();
      } // sql.close()
      
      return ""+lIdNum;
    } catch (SQLException sqlX) {
      throw new DictionaryException(this, sqlX);
    }
  }

  /**
   * {@link Dictionary} method - Returns a sub-list of editable entries in the dictionary
   * - i.e. all entries between the two specified indexes.  This primarily allows
   * pagination of the list on an Edit Dictionary page.
   * @param start the(zero-based) index of the first entry to list
   * @param length the number of entries to return, or 0, meaning to the end of the list
   * @return a map of keys to their definitions. 
   * @throws DictionaryReadOnlyException
   */
  public Map<String, List<String>> listEditableEntries(int start, int length)
    throws DictionaryReadOnlyException, DictionaryException {
    try {
      Map<String,List<String>> words = new LinkedHashMap<String,List<String>>();
      try (PreparedStatement sql = rdb.prepareStatement(
             sqlx.apply(
               "SELECT WordDia FROM cxen_wordformortho WHERE IdNum >= ?"
               + " ORDER BY WordDia "
               + (length > 0? " LIMIT " + start + ", " + length:"")))) {
        sql.setLong(1, IDNUM_PARTITION_START);
        try (ResultSet rs = sql.executeQuery()) {
          while (rs.next()) {
            String word = rs.getString("wordform");
            List<String> entries = lookupRaw(word);
            if (entries.size() == 0) continue;
            words.put(word, entries);
          } // next word
        } // rs.close()
      } // sql.close()
      return words;
    } catch (SQLException sqlX) {
      throw new DictionaryException(this, sqlX);
    }    
  }

  /**
   * {@link Dictionary} method - Returns a specific editable key from the dictionary.
   * @param key the key to look up
   * @return List of entries.
   * @throws DictionaryReadOnlyException
   */
  public List<String> lookupEditableEntry(String key)
    throws DictionaryReadOnlyException, DictionaryException {
    return lookupRaw(key);
  }

  /**
   * {@link Dictionary} method - Suggests a possible entry for the given entry.  
   * <p> In this case, English-based phonological heuristics are applied for common
   * suffixes like <q>~'s</q>, <q>~es</q>, <q>~ed</q>, etc. to suggest pronunciations for
   * unknown words. 
   * @param key The unknown word.
   * @return A suggested pronunciation for the given word, or null if no suggestion is
   * possible.
   * @throws OperationNotSupportedException When sEntry is already in the dictionary
   */
  public String suggest(String key) throws DictionaryException {
    if (lookup(key).size() > 0) {
      throw new DictionaryException(this, "Suggestion for existing word: " + key);
    }
    String suggestedPhonology = null;
    
    // suggest pronunciation based on heuristics, if possible
    // Some of these suggestions include a syllable boundary, which may not be quite
    // right, in which case the user should correct it as appropriate.  Even if they
    // don't, the presence of the syllable boundary (even if it's a consonant or two
    // late) is better that its absence - at lease syllable counts and vowel stress
    // will come out right.

    if (key.endsWith("'s")) {
      // strip off the 's
      String candidate = key.substring(0, key.length() - 2);
      // look up the dictionary
      try {
        List<String> rawEntries = lookupRaw(candidate);
        if (rawEntries.size() > 0) {
          candidate = rawEntries.get(0);
          // [sSzZJ_]_ -> Iz
          if (candidate.endsWith("s")
              || candidate.endsWith("S")
              || candidate.endsWith("z")
              || candidate.endsWith("Z")
              || candidate.endsWith("J")
              || candidate.endsWith("_")
            ) {
            suggestedPhonology = candidate + "-Iz";
          }
          // [-voice]_ -> s
          else if (candidate.endsWith("k")
                   || candidate.endsWith("f")
                   || candidate.endsWith("h")
                   || candidate.endsWith("p")
                   || candidate.endsWith("t")
                   || candidate.endsWith("T")
            ) {
            suggestedPhonology = candidate + "s";
          } else { // otherwise -> z
            suggestedPhonology = candidate + "z";
          }
        }
      } catch (Exception x) {
      }
    } else if(key.endsWith("'ve")) {
      // strip off the 've
      String candidate = key.substring(0, key.length() - 3);
      // look up the dictionary
      try {
        List<String> rawEntries = lookupRaw(candidate);
        if (rawEntries.size() > 0) {
          candidate = rawEntries.get(0);
          suggestedPhonology = candidate + "-@v";
        }
      } catch (Exception x) {
      }
    } else if(key.endsWith("'d")) {
      // strip off the 'd
      String candidate = key.substring(0, key.length() - 2);
      // look up the dictionary
      try {
        List<String> rawEntries = lookupRaw(candidate);
        if (rawEntries.size() > 0) {
          candidate = rawEntries.get(0);
          suggestedPhonology = candidate + "-@d";
        }
      } catch (Exception x) {
      }
    } else if(key.endsWith("s")) {
      // strip off the s
      String candidate = key.substring(0, key.length() - 1);
      // look up the dictionary
      try {
        List<String> rawEntries = lookupRaw(candidate);
        if (rawEntries.size() > 0) {
          candidate = rawEntries.get(0);
          // [sSzZJ_]_ -> Iz
          if (candidate.endsWith("s")
              || candidate.endsWith("S")
              || candidate.endsWith("z")
              || candidate.endsWith("Z")
              || candidate.endsWith("J")
              || candidate.endsWith("_")
            ) {
            suggestedPhonology = candidate + "-Iz";
          }
          // [-voice]_ -> s
          else if (candidate.endsWith("k")
                   || candidate.endsWith("f")
                   || candidate.endsWith("h")
                   || candidate.endsWith("p")
                   || candidate.endsWith("t")
                   || candidate.endsWith("T")
            ) {
            suggestedPhonology = candidate + "s";
          } else { // otherwise -> z
            suggestedPhonology = candidate + "z";
          }
        }
      } catch (Exception x) {
      }
    }
    
    // return whatever suggestion we may have
    return suggestedPhonology;
  }
   
  /** {@link Dictionary} method - Frees any resources reserved by the dictionary */
  public void close() {
    try {
      rdb.close();
    } catch (SQLException sqlX) {
    }
  }
}
