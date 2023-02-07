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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.Dictionary;
import nzilbb.ag.automation.DictionaryException;
import nzilbb.ag.automation.DictionaryReadOnlyException;
import nzilbb.sql.mysql.MySQLTranslator;

/**
 * Dictionary for lexicons specified by 'flat', plain text files - e.g. CSV files.
 */
@SuppressWarnings("serial")
public class FlatLexicon implements Dictionary { 
  
  private Connection rdb;
  private MySQLTranslator sqlx = new MySQLTranslator();
  private PreparedStatement sql;
  private String quote;
  
  /**
   * Lexicon name
   * @see #getLexicon()
   * @see #setLexicon(String)
   */
  protected String lexicon;
  /**
   * Getter for {@link #lexicon}: Lexicon name
   * @return Lexicon name
   */
  public String getLexicon() { return lexicon; }
  /**
   * Setter for {@link #lexicon}: Lexicon name
   * @param newLexicon Lexicon name
   */
  public FlatLexicon setLexicon(String newLexicon) { lexicon = newLexicon; return this; }
  
  /**
   * Key ID of lexicon in lexicon table.
   * @see #getLexiconId()
   * @see #setLexiconId(int)
   */
  protected int lexiconId;
  /**
   * Getter for {@link #lexiconId}: Key ID of lexicon in lexicon table.
   * @return Key ID of lexicon in lexicon table.
   */
  public int getLexiconId() { return lexiconId; }
  /**
   * Setter for {@link #lexiconId}: Key ID of lexicon in lexicon table.
   * @param newLexiconId Key ID of lexicon in lexicon table.
   */
  public FlatLexicon setLexiconId(int newLexiconId) { lexiconId = newLexiconId; return this; }
  
  /**
   * Name of the field to look up, in which values will match labels on the token layer.
   * @see #getKeyField()
   * @see #setKeyField(String)
   */
  protected String keyField;
  /**
   * Getter for {@link #keyField}: Name of the field to look up, in which values will
   * match labels on the token layer. 
   * @return Name of the field to look up, in which values will match labels on the token layer.
   */
  public String getKeyField() { return keyField; }
  /**
   * Setter for {@link #keyField}: Name of the field to look up, in which values will
   * match labels on the token layer. 
   * @param newKeyField Name of the field to look up, in which values will match labels on
   * the token layer. 
   */
  public FlatLexicon setKeyField(String newKeyField) { keyField = newKeyField; return this; }
  
  /**
   * Name of the field that provides labels for the tag layer.
   * @see #getValueField()
   * @see #setValueField(String)
   */
  protected String valueField;
  /**
   * Getter for {@link #valueField}: Name of the field that provides labels for the tag layer.
   * @return Name of the field that provides labels for the tag layer.
   */
  public String getValueField() { return valueField; }
  /**
   * Setter for {@link #valueField}: Name of the field that provides labels for the tag layer.
   * @param newValueField Name of the field that provides labels for the tag layer.
   */
  public FlatLexicon setValueField(String newValueField) { valueField = newValueField; return this; }
  
  /**
   * The FlatLexiconTagger annotator that created this dictionary.
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
   * {@link Dictionary} method - Name of the dictionary, which must be unique among the
   * dictionaries implemented by the same annotator.
   * @return The dictionary's ID.
   */
  public String getDictionaryId() { return lexicon+":"+keyField+"->"+valueField; }
  
  /**
   * Whether dictionary lookups are case sensitive or not.
   * @see #getCaseSensitive()
   * @see #setCaseSensitive(boolean)
   */
  protected boolean caseSensitive = false;
  /**
   * Getter for {@link #caseSensitive}: Whether dictionary lookups are case sensitive or not. 
   * @return Whether dictionary lookups are case sensitive or not.
   */
  public boolean getCaseSensitive() { return caseSensitive; }
  /**
   * Setter for {@link #caseSensitive}: Whether dictionary lookups are case sensitive or
   * not.
   * @param newCaseSensitive Whether dictionary lookups are case sensitive or not. 
   */
  public FlatLexicon setCaseSensitive(boolean newCaseSensitive) throws SQLException {
    if (sql != null) {
      try { sql.close(); } catch(Exception exception) {}
    }
    caseSensitive = newCaseSensitive;
    sql = rdb.prepareStatement(
      sqlx.apply(
        "SELECT DISTINCT "+quote+valueField+quote+", supplemental"
        +" FROM "+annotator.getAnnotatorId()+"_lexicon_"+lexiconId
        // use BINARY for accent sensitivity
        // use LOWER to remove case sensitivity of BINARY
        +" WHERE "+(caseSensitive?"":"LOWER")+"("+quote+keyField+quote+")"
        +" = BINARY "+(caseSensitive?"":"LOWER")+"(?)"
        +" ORDER BY "+quote+valueField+quote+""));
    return this;
  }
  
  /**
   * Constructor.
   */
  public FlatLexicon(
    FlatLexiconTagger annotator, Connection rdb, MySQLTranslator translator,
    String lexicon, String keyField, String valueField)
    throws SQLException, DictionaryException {
    this.annotator = annotator;
    this.rdb = rdb;
    this.quote = rdb.getMetaData().getIdentifierQuoteString();
    this.sqlx = translator;
    this.lexicon = lexicon;
    this.keyField = keyField;
    this.valueField = valueField;
    this.caseSensitive = caseSensitive;
    sql = rdb.prepareStatement(
      sqlx.apply(
        "SELECT lexicon_id FROM "+annotator.getAnnotatorId()+"_lexicon WHERE name = ?"));
    sql.setString(1, lexicon);
    try {
      ResultSet rs = sql.executeQuery();
      try {
        if (!rs.next()) {
          throw new DictionaryException(null, "Not a valid lexicon name: " + lexicon);
        }
        lexiconId = rs.getInt(1);
      } finally {
        rs.close();
      }
    } finally {
      sql.close();
    }
    setCaseSensitive(false); // this creates the sql query
    // ensure keyField/valueField are valid
    try {
      lookupEntries("test", false);
    } catch(SQLException exception) {
      throw new DictionaryException(null, "Invalid key/value field: " + exception.getMessage());
    }
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
      sql.setString(1, key);
      ResultSet rs = sql.executeQuery();
      try {
        while (rs.next()) {
          if (supplementalOnly && !rs.getBoolean("supplemental")) continue;
          String entry = rs.getString(1);
          if (entry != null) {
            queryResults.add(entry);
          }
        } // next result
      } finally {
        rs.close();
      }
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
    try {
      PreparedStatement sql = rdb.prepareStatement(
        sqlx.apply(
          "SELECT COUNT(DISTINCT "+quote+keyField+quote+")"
          +" FROM "+annotator.getAnnotatorId()+"_lexicon_"+lexiconId));
      try {
        ResultSet rs = sql.executeQuery();
        try {
          rs.next();
          return rs.getInt(1);
        } finally {
          rs.close();
        }
      } finally {
        sql.close();
      }
    } catch(Throwable x) {
      return 0;
    }
  }

  /**
   * {@link Dictionary} method - Returns a sub-list of all entries in the dictionary - i.e. all
   * entries between the two specified indexes.  This primarily allows LaBB-CAT to
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
      PreparedStatement sql = rdb.prepareStatement(
        sqlx.apply(
          "SELECT DISTINCT "+quote+keyField+quote+""
          +" FROM "+annotator.getAnnotatorId()+"_lexicon_"+lexiconId
          +" ORDER BY "+quote+keyField+quote+""
          + (length > 0? " LIMIT " + start + ", " + length:"")));
      try {
        ResultSet rs = sql.executeQuery();
        try {
          while (rs.next()) {
            String word = rs.getString(keyField);
            Vector<String> entries = lookupEntries(word, false);
            if (entries.size() == 0) continue;
            words.put(word, entries);
          } // next word
        } finally {
          rs.close();
        }
      } finally {
        sql.close();
      }
      return words;
    } catch (SQLException sqlX) {
      throw new DictionaryException(this, sqlX);
    }
  }
      
  /**
   * {@link Dictionary} method - Determines whether the dictionary is entirely read-only, or has
   * facility for adding/editing entries.
   * @return true if there is no facility to add/edit entries in the
   * dictionary, false otherwise
   */
  public boolean isReadOnly() { return false; }

  /**
   * Returns a count of all editable keys in the dictionary.
   * @return the number of keys that would be returned by a call to listEditableKeys
   * @throws DictionaryReadOnlyException
   */
  public int countEditableKeys() throws DictionaryReadOnlyException, DictionaryException {
    try {
      PreparedStatement sql = rdb.prepareStatement(
        sqlx.apply(
          "SELECT COUNT(DISTINCT "+quote+keyField+quote+")"
          +" FROM "+annotator.getAnnotatorId()+"_lexicon_"+lexiconId));
      try {
        ResultSet rs = sql.executeQuery();
        try {
          rs.next();
          return rs.getInt(1);
        } finally {
          rs.close();
        }
      } finally {
        sql.close();
      }
    } catch (SQLException sqlX) {
      throw new DictionaryException(this, sqlX);
    }
  }
  /**
   * {@link Dictionary} method - Returns a count of all editable entries in the dictionary.
   * @return the number of entries that would be returned by a call to listEditableEntries
   * @throws DictionaryReadOnlyException
   */
  public int countEditableEntries()
    throws DictionaryReadOnlyException, DictionaryException {
    try {
      PreparedStatement sql = rdb.prepareStatement(
        sqlx.apply(
          "SELECT COUNT(DISTINCT "+quote+valueField+quote+")"
          +" FROM "+annotator.getAnnotatorId()+"_lexicon_"+lexiconId));
      try {
        ResultSet rs = sql.executeQuery();
        try {
          rs.next();
          return rs.getInt(1);
        } finally {
          rs.close();
        }
      } finally {
        sql.close();
      }
    } catch (SQLException sqlX) {
      throw new DictionaryException(this, sqlX);
    }
  }

  /**
   * {@link Dictionary} method - Returns an aggregrate of all values of all entries in the
   * dictionary.
   * @param operation Any aggregate operation supported by SQL for VARCHAR fields.. 
   * @return The result of the aggregation, or null if the operation is not supported.
   */
  public String aggregateEntries(String operation) throws DictionaryException {
    try {
      PreparedStatement sql = rdb.prepareStatement(
        sqlx.apply(
        "SELECT "+operation+"(DISTINCT "+quote+valueField+quote+")"
        +" FROM "+annotator.getAnnotatorId()+"_lexicon_"+lexiconId));
      try {
        ResultSet rs = sql.executeQuery();
        try {
          rs.next();
          return rs.getString(1);
        } finally {
          rs.close();
        }
      } finally {
        sql.close();
      }
    } catch (SQLException sqlX) {
      return null;
    }
  }

  /**
   * {@link Dictionary} method - Returns an aggregrate of all entries (i.e. keys) in the
   * dictionary.
   * @param operation Any aggregate operation supported by SQL for VARCHAR fields.. 
   * @return The result of the aggregation, or null if the operation is not supported.
   * @throws DictionaryException
   */
  public String aggregateKeys(String operation) throws DictionaryException {
    String select = operation + "("+quote+keyField+quote+")";
    if ("COUNT".equals(operation)) select = "COUNT(DISTINCT "+quote+keyField+quote+")";
    try {
      PreparedStatement sql = rdb.prepareStatement(
        "SELECT "+select+" FROM "+annotator.getAnnotatorId()+"_lexicon_"+lexiconId);
      try {
        ResultSet rs = sql.executeQuery();
        try {
          rs.next();
          return rs.getString(1);
        } finally {
          rs.close();
        }
      } finally {
        sql.close();
      }
    } catch (SQLException sqlX) {
      return null;
    }
  }

  /**
   * {@link Dictionary} method - Adds a entry to the dictionary.
   * @param key The key to add an entry for - e.g. the entry orthorgraphy.
   * @param entry The entry for the key - e.g. its pronunciation.
   * @return the ID for the entry in the dictionary, if appropriate
   */
  public String add(String key, String entry)
    throws DictionaryReadOnlyException, DictionaryException {

    try {
      sql = rdb.prepareStatement(
        "INSERT INTO "+annotator.getAnnotatorId()+"_lexicon_"+lexiconId
        +" ("+quote+keyField+quote+", "+quote+valueField+quote+", supplemental) VALUES (?,?,1)");
      try {
        sql.setString(1, key.toLowerCase());
        sql.setString(2, entry);
        sql.executeUpdate();
      } finally {
        sql.close();
      }
      
      return null;
    } catch (SQLException sqlX) {
      throw new DictionaryException(this, sqlX);
    }
  }

  /**
   * {@link Dictionary} method - Removes a key from the dictionary.
   * @param key
   * @return the ID for the entry in the dictionary, if appropriate
   * @throws DictionaryReadOnlyException
   */
  public String remove(String key)
    throws DictionaryReadOnlyException, DictionaryException {

    try {
      PreparedStatement sql = rdb.prepareStatement(
        "DELETE FROM "+annotator.getAnnotatorId()+"_lexicon_"+lexiconId
        // use BINARY for accent sensitivity
        // use LOWER to remove case sensitivity of BINARY
        +" WHERE "+(caseSensitive?"":"LOWER")+"("+quote+keyField+quote+")"
        +" = BINARY "+(caseSensitive?"":"LOWER")+"(?)");
      try {
        sql.setString(1, key);
        if (sql.executeUpdate() == 0) {
          throw new DictionaryReadOnlyException(
            this, "No entries found for " + key);
        }
      } finally {
        sql.close();
      }
      return null;
    } catch (SQLException sqlX) {
      throw new DictionaryException(this, sqlX);
    }
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
    try {
      PreparedStatement sql = rdb.prepareStatement(
        "DELETE FROM "+annotator.getAnnotatorId()+"_lexicon_"+lexiconId
        // use BINARY for accent sensitivity
        // use LOWER to remove case sensitivity of BINARY
        +" WHERE "+(caseSensitive?"":"LOWER")+"("+quote+keyField+quote+")"
        +" = BINARY "+(caseSensitive?"":"LOWER")+"(?)"
        +" AND "+(caseSensitive?"":"LOWER")+"("+quote+valueField+quote+")"
        +" = BINARY "+(caseSensitive?"":"LOWER")+"(?)");
      try {
        sql.setString(1, key);
        sql.setString(2, entry);
        if (sql.executeUpdate() == 0) {
          throw new DictionaryReadOnlyException(
            this, "No entry found for " + key + " = " + entry);
        }
      } finally {
        sql.close();
      }
      return null;
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
      PreparedStatement sql = rdb.prepareStatement(
        "SELECT DISTINCT "+quote+keyField+quote+" FROM "+annotator.getAnnotatorId()+"_lexicon_"+lexiconId
        +" ORDER BY "+quote+keyField+quote+""
        + (length > 0? " LIMIT " + start + ", " + length:""));
      try {
        ResultSet rs = sql.executeQuery();
        try {
          while (rs.next()) {
            String word = rs.getString(keyField);
            Vector<String> entries = lookupEntries(word, false);
            if (entries.size() == 0) continue;
            words.put(word, entries);
          } // next word
        } finally {
          rs.close();
        }
      } finally {
        sql.close();
      }
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
    try {
      return lookupEntries(key, false);
    } catch (SQLException sqlX) {
      throw new DictionaryException(this, sqlX);
    }
  }

  /** {@link Dictionary} method - Frees any resources reserved by the dictionary */
  public void close() {
    try {
      rdb.close();
    } catch (SQLException sqlX) {
    }
  }
}
