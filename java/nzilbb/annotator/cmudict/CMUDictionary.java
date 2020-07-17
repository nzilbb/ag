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
 * This is an example annotator dictionary.
 */
@SuppressWarnings("serial")
public class CMUDictionary implements Dictionary {


   private Connection rdb;
   private MySQLTranslator sqlx = new MySQLTranslator();
   private PreparedStatement sql; 

   /**
    * The TheWorksExample annotator that created this dictionary.
    * @see #getAnnotator()
    * @see #setAnnotator(Annotator)
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
   public String getDictionaryId() {
      return "frequencies";
   }

   /**
    * Constructor.
    */
   public CMUDictionary(CMUDict annotator, Connection rdb, MySQLTranslator translator)
      throws SQLException {
      this.annotator = annotator;
      this.rdb = rdb;
      this.sqlx = translator;
      sql = rdb.prepareStatement(
         sqlx.apply(
            "SELECT pron_cmudict, supplemental FROM "+annotator.getAnnotatorId()+"_wordform"
            +" WHERE wordform = ? ORDER BY variant"));
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
         sql.setString(1, key);
         ResultSet rs = sql.executeQuery();
         try {
            while (rs.next()) {
               if (supplementalOnly && !rs.getBoolean("supplemental")) continue;
               String entry = rs.getString(1);
               if (entry != null) queryResults.add(entry);
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
               "SELECT COUNT(DISTINCT wordform) FROM "+annotator.getAnnotatorId()+"_wordform"));
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
               "SELECT DISTINCT wordform FROM "+annotator.getAnnotatorId()+"_wordform"
               +" ORDER BY wordform"
               + (length > 0? " LIMIT " + start + ", " + length:"")));
         try {
            ResultSet rs = sql.executeQuery();
            try {
               while (rs.next()) {
                  String word = rs.getString("wordform");
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
               "SELECT COUNT(DISTINCT wordform) FROM "+annotator.getAnnotatorId()+"_wordform"
               +" WHERE supplemental = TRUE"));
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
               "SELECT COUNT(*) FROM "+annotator.getAnnotatorId()+"_wordform"
               +" WHERE supplemental = TRUE"));
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
               "SELECT "+operation+"(pron_cmudict)"
               +" FROM "+annotator.getAnnotatorId()+"_wordform"));
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
      String select = operation + "(wordform)";
      if ("COUNT".equals(operation)) select = "COUNT(DISTINCT wordform)";
      try {
         PreparedStatement sql = rdb.prepareStatement(
            "SELECT "+select+" FROM "+annotator.getAnnotatorId()+"_wordform");
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
      
      // get the next variant of the word
      int iVariant = 0;
      try {
         PreparedStatement sql = rdb.prepareStatement(
            "SELECT COALESCE(MAX(variant) + 1, 0) AS variant"
            +" FROM "+annotator.getAnnotatorId()+"_wordform"
            +" WHERE wordform = ?");
         sql.setString(1, key);
         try {
            ResultSet rs = sql.executeQuery();
            try {
               rs.next();
               iVariant = rs.getInt("variant");
            } finally {
               rs.close();
            }
         } finally {
            sql.close();
         }

         // add it
         sql = rdb.prepareStatement(
            "INSERT INTO "+annotator.getAnnotatorId()+"_wordform"
            +" (wordform, variant, pron_cmudict, supplemental) VALUES (?,?,?,1)");
         try {
            sql.setString(1, key.toLowerCase());
            sql.setInt(2, iVariant);
            sql.setString(3, entry);
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
            "DELETE FROM "+annotator.getAnnotatorId()+"_wordform"
            +" WHERE wordform = ? AND supplemental = 1");
         try {
            sql.setString(1, key);
            if (sql.executeUpdate() == 0) {
               throw new DictionaryReadOnlyException(
                  this, "No supplemental entries found for " + key);
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
            "DELETE FROM "+annotator.getAnnotatorId()+"_wordform"
            +" WHERE wordform = ? AND pron_cmudict = ? AND supplemental = 1");
         try {
            sql.setString(1, key);
            sql.setString(2, entry);
            if (sql.executeUpdate() == 0) {
               throw new DictionaryReadOnlyException(
                  this, "No supplemental entry found for " + key + " = " + entry);
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
            "SELECT DISTINCT wordform FROM "+annotator.getAnnotatorId()+"_wordform"
            +" WHERE supplemental = 1 ORDER BY wordform"
            + (length > 0? " LIMIT " + start + ", " + length:""));
         try {
            ResultSet rs = sql.executeQuery();
            try {
               while (rs.next()) {
                  String word = rs.getString("wordform");
                  Vector<String> entries = lookupEntries(word, true);
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
         return lookupEntries(key, true);
      } catch (SQLException sqlX) {
         throw new DictionaryException(this, sqlX);
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
      if (lookup(key) != null) {
	 throw new DictionaryException(this, "Suggestion for existing word: " + key);
      }
      String suggestedPhonology = null;
      
      // suggest pronunciation based on heuristics, if possible
      if (key.endsWith("'s")) {
	 // strip off the 's
	 String candidate = key.substring(0, key.length() - 2);
	 // look up the dictionary
	 try {
	    candidate = lookupEntries(candidate, false).firstElement();
	    if (candidate != null) {
	       // [sSzZJ_]_ -> Iz
	       if (candidate.endsWith("S")
		   || candidate.endsWith("SH")
		   || candidate.endsWith("Z")
		   || candidate.endsWith("ZH")
		   || candidate.endsWith("CH")
		   || candidate.endsWith("JH")) {
		  suggestedPhonology = candidate + " I Z";
	       } else if (candidate.endsWith("K") // [-voice]_ -> s
			|| candidate.endsWith("F")
			|| candidate.endsWith("H")
			|| candidate.endsWith("P")
			|| candidate.endsWith("T")
			|| candidate.endsWith("TH")) {
		  suggestedPhonology = candidate + " S";
	       } else { // otherwise -> z
		  suggestedPhonology = candidate + " Z";
	       }
	    }
	 } catch (Exception x) {
         }
      } else if(key.endsWith("'ve")) {
	 // strip off the 've
	 String candidate = key.substring(0, key.length() - 3);
	 // look up the dictionary
	 try {
	    candidate = lookupEntries(candidate, false).firstElement();
	    if (candidate != null) {
	       suggestedPhonology = candidate + " IH V";
	    }
	 } catch (Exception x) {
         }
      } else if(key.endsWith("'d")) {
	 // strip off the 'd
	 String candidate = key.substring(0, key.length() - 2);
	 // look up the dictionary
	 try {
	    candidate = lookupEntries(candidate, false).firstElement();
	    if (candidate != null) {
	       suggestedPhonology = candidate + " IH D";
	    }
	 } catch (Exception x) {
         }
      } else if(key.endsWith("s")) {
	 // strip off the s
	 String candidate = key.substring(0, key.length() - 1);
	 // look up the dictionary
	 try {
	    candidate = lookupEntries(candidate, false).firstElement();
	    if (candidate != null) { // [sSzZJ_]_ -> Iz
	       if (candidate.endsWith("S")
		   || candidate.endsWith("SH")
		   || candidate.endsWith("Z")
		   || candidate.endsWith("ZH")
		   || candidate.endsWith("CH")
		   || candidate.endsWith("JH")) {
		  suggestedPhonology = candidate + " I Z";
	       } else if (candidate.endsWith("K") // [-voice]_ -> s
			|| candidate.endsWith("F")
			|| candidate.endsWith("H")
			|| candidate.endsWith("P")
			|| candidate.endsWith("T")
			|| candidate.endsWith("TH")) {
		  suggestedPhonology = candidate + " S";
	       } else { // otherwise -> z
		  suggestedPhonology = candidate + " Z";
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
