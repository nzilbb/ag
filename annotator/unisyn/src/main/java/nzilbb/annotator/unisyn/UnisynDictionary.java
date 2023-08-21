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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Vector;
import java.util.StringTokenizer;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.Dictionary;
import nzilbb.ag.automation.DictionaryException;
import nzilbb.ag.automation.DictionaryReadOnlyException;
import nzilbb.sql.mysql.MySQLTranslator;

/**
 * Dictionary for lexicons specified by 'flat', plain text files - e.g. CSV files.
 */
@SuppressWarnings("serial")
public class UnisynDictionary implements Dictionary {
  // TODO handle multiple pos entries delimited by '/'
  
  private Connection rdb;
  private MySQLTranslator sqlx = new MySQLTranslator();
  private PreparedStatement sql;
  private HashMap<String,String> map;
  private HashMap<String,String> reverseMap;
  
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
  public UnisynDictionary setLexicon(String newLexicon) { lexicon = newLexicon; return this; }
  
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
  public UnisynDictionary setLexiconId(int newLexiconId) { lexiconId = newLexiconId; return this; }
  
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
  public UnisynDictionary setKeyField(String newKeyField) { keyField = newKeyField; return this; }
  
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
  public UnisynDictionary setValueField(String newValueField) { valueField = newValueField; return this; }
  
  /**
   * The UnisynDictionaryTagger annotator that created this dictionary.
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
   * Constructor.
   */
  public UnisynDictionary(
    UnisynTagger annotator, Connection rdb, MySQLTranslator translator,
    String lexicon, String keyField, String valueField)
    throws SQLException, DictionaryException {
    this.annotator = annotator;
    this.rdb = rdb;
    this.sqlx = translator;
    this.lexicon = lexicon;
    this.keyField = keyField;
    this.valueField = valueField;
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
    if (valueField.equals("pron_disc")) {
      valueField = "pron_orig";
      map = new HashMap<String,String>();
      reverseMap = new HashMap<String,String>();
      sql = rdb.prepareStatement(
        sqlx.apply(
          "SELECT phoneme_orig, phoneme_disc FROM "
          +annotator.getAnnotatorId()+"_phoneme_map WHERE lexicon_id = ?"));
      sql.setInt(1, lexiconId);
      ResultSet rs = sql.executeQuery();
      while (rs.next()) {
        map.put(rs.getString("phoneme_orig"), rs.getString("phoneme_disc"));
        if (rs.getString("phoneme_disc").length() > 0) {
          reverseMap.put(rs.getString("phoneme_disc"), rs.getString("phoneme_orig"));
        }
      }
      rs.close();
      sql.close();
    }
    String sSql = "SELECT DISTINCT "+valueField+", supplemental, variant"
      +" FROM "+annotator.getAnnotatorId()+"_lexicon_"+lexiconId
      +" WHERE LOWER("+keyField+") = LOWER(?)"
      +" ORDER BY variant";
    sql = rdb.prepareStatement(sqlx.apply(sSql));
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
          if (supplementalOnly && rs.getInt("supplemental") == 0) continue;
          String entry = rs.getString(1);
          if (entry != null) {
            if ("pos".equals(valueField)) {
              // POS can include multiple options in the same result
              // e.g. "VB/NN/VBP"
              // so we split these out
              StringTokenizer stOptions = new StringTokenizer(entry, "/");
              for(String p : entry.split("/")) {
                queryResults.add(p);
              } // next option
            } else {
              if (map != null) { // map the pronunciation to DISC
                StringBuffer sDISC = new StringBuffer(entry.length() / 2);
                StringTokenizer stPhones = new StringTokenizer(
                  entry
                  .replaceAll("[><{}\\$=#]","") // ignore morphological stuff
                  , " ");
                while (stPhones.hasMoreTokens()) {
                  String sPhone = stPhones.nextToken();
                  if (map.containsKey(sPhone)) {
                    sDISC.append(map.get(sPhone));
                  } else { // pass through unknown phones
                    sDISC.append(sPhone);
                  }
                } // next phone
                entry = sDISC.toString();
              } // map to DISC
              
              queryResults.add(entry);
            }
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
          "SELECT COUNT(DISTINCT "+keyField+")"
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
          "SELECT DISTINCT "+keyField+", variant"
          +" FROM "+annotator.getAnnotatorId()+"_lexicon_"+lexiconId
          +" ORDER BY variant"
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
          "SELECT COUNT(DISTINCT "+keyField+")"
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
          "SELECT COUNT(DISTINCT "+valueField+")"
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
          "SELECT "+operation+"(DISTINCT "+valueField+")"
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
    String select = operation + "("+keyField+")";
    if ("COUNT".equals(operation)) select = "COUNT(DISTINCT "+keyField+")";
    try {
      PreparedStatement sql = rdb.prepareStatement(
        sqlx.apply(
          "SELECT "+select+" FROM "+annotator.getAnnotatorId()+"_lexicon_"+lexiconId));
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
      // get the next variant of the word
      PreparedStatement sql = rdb.prepareStatement(
        sqlx.apply(
          "SELECT COALESCE(MAX(variant) + 1, 0) AS variant"
          +" FROM "+annotator.getAnnotatorId()+"_lexicon_"+lexiconId
          +" WHERE wordform = ?"));
      sql.setString(1, key);
      ResultSet rs = sql.executeQuery();
      rs.next();
      int variant = rs.getInt("variant");
      rs.close();
      sql.close();
      String variantDescription = "";
      String pos = "";
      String pronOrig = "";
      String pronDisc = "";
      String enrichedOrthography = "";
      int frequency = 0; 
      int syllableCount = 1;
      
      if (valueField.equals("pron_orig")) {
        pronOrig = entry;
        if (map != null) {
          // we've been given a DISC pronunciation, so have to convert it back to 'original' Unisyn
          StringBuffer unisyn = new StringBuffer(pronOrig.length() * 2);
          unisyn.append("{ ");
          for (char disc : pronOrig.toCharArray()) {
            String phone = ""+disc;
            if (reverseMap.containsKey(phone)) {
              unisyn.append(reverseMap.get(phone));
            } else { // pass through unknown phones
              unisyn.append(phone);
            }
            unisyn.append(" ");
          } // next phone
          unisyn.append("}");
          pronOrig = unisyn.toString().trim();
        }
        syllableCount = pronOrig.replaceAll("[^.]", "").length() + 1;
      } else if (valueField.equals("pos")) {
        pos = entry;
      } else if (valueField.equals("enriched_orthography")) {
        enrichedOrthography = entry;
      } else if (valueField.equals("frequency")) {
        frequency = Integer.parseInt(entry);
      } else {
        throw new DictionaryException(this, "Unknown field for update: " + valueField);
      }

      sql = rdb.prepareStatement(
        sqlx.apply(
          "INSERT INTO "+annotator.getAnnotatorId()+"_lexicon_"+lexiconId
          +" (wordform, variant, pos, pron_orig, enriched_orthography, frequency, syllable_count,"
          +" supplemental) VALUES (?,?,?,?,?,?,?,1)"));
      try {
        sql.setString(1, key.toLowerCase());
        sql.setInt(2, variant);
        sql.setString(3, pos);
        sql.setString(4, pronOrig);
        sql.setString(5, enrichedOrthography);
        sql.setInt(6, frequency);
        sql.setInt(7, syllableCount);
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
        sqlx.apply(
          "DELETE FROM "+annotator.getAnnotatorId()+"_lexicon_"+lexiconId
          // use LOWER to remove case sensitivity
          +" WHERE LOWER("+keyField+") = LOWER(?)"));
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
        sqlx.apply(
          "DELETE FROM "+annotator.getAnnotatorId()+"_lexicon_"+lexiconId
          // use LOWER to remove case sensitivity of BINARY
          +" WHERE LOWER("+keyField+") = LOWER(?)"
          // force case-sensitivity for value, because it might be DISC, in which 'i' != 'I' etc.
          +" AND "+valueField+" = BINARY ?"));
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
        sqlx.apply(
          "SELECT DISTINCT "+keyField+", variant"
          +" FROM "+annotator.getAnnotatorId()+"_lexicon_"+lexiconId
          +" ORDER BY variant"
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

  /**
   * {@link Dictionary} method - Suggests a possible entry for the given entry.  
   * <p> In this case, English-based phonological heuristics are applied for common
   * suffixes like <q>~'s</q>, <q>~es</q>, <q>~ed</q>, etc. to suggest pronunciations for
   * unknown words. 
   * @param key The unknown word.
   * @return A suggested pronunciation for the given word, or null if no suggestion is possible.
   * @throws OperationNotSupportedException When sEntry is already in the dictionary
   */
  public String suggest(String key) throws DictionaryException {
    if (lookup(key).size() > 0) {
      throw new DictionaryException(this, "Suggestion for existing word: " + key);
    }
    if (map == null) {
      throw new DictionaryException(this, "Can only suggest DISC pronunciations");
    }
    String suggestedPhonology = null;
    String syllableMarker = "-";
    if (map == null) syllableMarker = ".";
      
    // suggest pronunciation based on heuristics, if possible
    if (key.endsWith("'s")) {
      // strip off the 's
      String candidate = key.substring(0, key.length() - 2);
      // look up the dictionary
      try {
        candidate = lookup(candidate).stream().findAny().orElse(null);
        if (candidate != null) {
          if (candidate.endsWith("s")
              || candidate.endsWith("S")
              || candidate.endsWith("z")
              || candidate.endsWith("Z")
              || candidate.endsWith("J")
              || candidate.endsWith("_")
            ) { // [sSzZJ_]_ -> Iz
            suggestedPhonology = candidate + syllableMarker + "Iz";
          } else if (candidate.endsWith("k")
                   || candidate.endsWith("f")
                   || candidate.endsWith("h")
                   || candidate.endsWith("p")
                   || candidate.endsWith("t")
                   || candidate.endsWith("T")
            ) { // [-voice]_ -> s
            suggestedPhonology = candidate + "s";
          } else { // otherwise -> z
            suggestedPhonology = candidate + "z";
          }
        }
      } catch (Exception x) {}
    } else if(key.endsWith("'ve")) {
      // strip off the 've
      String candidate = key.substring(0, key.length() - 3);
      // look up the dictionary
      try {
        candidate = lookup(candidate).stream().findAny().orElse(null);
        if (candidate != null) {
          suggestedPhonology = candidate + syllableMarker + "@v";
        }
      } catch (Exception x) {
      }
    } else if(key.endsWith("'d")) {
      // strip off the 'd
      String candidate = key.substring(0, key.length() - 2);
      // look up the dictionary
      try {
        candidate = lookup(candidate).stream().findAny().orElse(null);
        if (candidate != null) {
          suggestedPhonology = candidate + syllableMarker + "@d";
        }
      } catch (Exception x) {
      }
    } else if(key.endsWith("s")) {
      // strip off the s
      String candidate = key.substring(0, key.length() - 1);
      // look up the dictionary
      try {
        candidate = lookup(candidate).stream().findAny().orElse(null);
        if (candidate != null) {
          if (candidate.endsWith("s")
              || candidate.endsWith("S")
              || candidate.endsWith("z")
              || candidate.endsWith("Z")
              || candidate.endsWith("J")
              || candidate.endsWith("_")
            ) { // [sSzZJ_]_ -> Iz
            suggestedPhonology = candidate + syllableMarker + "Iz";
          } else if (candidate.endsWith("k") 
                   || candidate.endsWith("f")
                   || candidate.endsWith("h")
                   || candidate.endsWith("p")
                   || candidate.endsWith("t")
                   || candidate.endsWith("T")
            ) { // [-voice]_ -> s
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
}
