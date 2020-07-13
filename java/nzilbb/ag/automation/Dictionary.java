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
package nzilbb.ag.automation;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A dictionary mapping labels of one type to labels of another, e.g. a pronunciation
 * dictionary with word orthography as the key.
 * <p> Each key can map to multiple entries. 
 * <p> In order for implementations to be as minimal as possible, this interface provides
 * various default implementations that assume minimal functionality, and a read-only
 * dictionary.
 *
 * @author Robert Fromont robert@fromont.net.nz
 */
public interface Dictionary extends Function<String,List<String>> {

   /**
    * Provides the annotator that implements the dictionary.
    * @return The dictionary's annotator.
    */
   public Annotator getAnnotator();

   /**
    * Name of the dictionary, which must be unique among the dictionaries implemented by
    * the same annotator.
    * @return The dictionary's ID.
    */
   public String getDictionaryId();

   /** 
    * Look up all entries for the given key.
    */ 
   public List<String> lookup(String key) throws DictionaryException;

   /**
    * Returns a count of all words in the dictionary.
    * @return the number of words that would be returned by a call to listAllWords
    */
   public int countAllWords() throws DictionaryException;

   /**
    * Returns a sub-list of all words in the dictionary - i.e. all
    * words between the two specified indexes.  This primarily allows LaBB-CAT to
    * paginate the list on the Edit Dictionary page.
    * @param start the(zero-based) index of the first word to list
    * @param length the number of entries to return, or 0, meaning to the end of the list
    * @return a map of keys to their definitions.
    * @throws DictionaryReadOnlyException
    * @throws Exception
    */
   public Map<String, List<String>> listAllWords(int start, int length)
      throws DictionaryReadOnlyException, DictionaryException;
      
   /**
    * Determines whether the dictionary is entirely read-only, or has
    * facility for adding/editing entries.
    * @return true if there is no facility to add/edit entries in the
    * dictionary, false otherwise
    */
   default public boolean isReadOnly() { return true; }

   /**
    * Returns a count of all editable words in the dictionary.
    * @return the number of words that would be returned by a call to listEditableWords
    * @throws DictionaryReadOnlyException
    */
   default public int countEditableWords()
      throws DictionaryReadOnlyException, DictionaryException {
      return 0;
   }

   /**
    * Returns an aggregrate of all values of all words in the dictionary. This default
    * implementation always returns null. 
    * @param operation The aggregation operation - e.g. MAX, MIN, COUNT, or SUM (e.g. to
    * get the sum of all word frequencies in a frequency word-list. The supported
    * operations depend on the implementor. 
    * @return The result of the aggregation, or null if the operation is not supported.
    */
   default public String aggregateEntries(String operation) throws DictionaryException {
      return null;
   }

   /**
    * Returns an aggregrate of all words (i.e. keys) in the dictionary. This default
    * implementation always returns null. 
    * @param operation The aggregation operation - e.g. MAX, MIN, COUNT, or SUM (e.g. to
    * get the sum of all word frequencies in a frequency word-list. The supported
    * operations depend on the implementor. 
    * @return The result of the aggregation, or null if the operation is not supported.
    * @throws Exception
    */
   default public String aggregateKeys(String operation) throws DictionaryException {
      return null;
   }

   /**
    * Adds a word to the dictionary.
    * @param key The key to add an entry for - e.g. the word orthorgraphy.
    * @param entry The entry for the key - e.g. its pronunciation.
    * @return the ID for the word in the dictionary, if appropriate
    */
   default public String add(String key, String entry)
      throws DictionaryReadOnlyException, DictionaryException {
      throw new DictionaryReadOnlyException(this);
   }

   /**
    * Adds a word to the dictionary with a given reference.  The default implementation of
    * this method simply calls {@link #add(String,String)} - i.e. the reference is ignored. 
    * @param key The key to add an entry for - e.g. the word orthorgraphy.
    * @param entry The entry for the key - e.g. its pronunciation.
    * @param reference A reference for the word. This is implementation-specific, but is
    * intended as a mechanism for allowing a new wordform to be associated with an
    * existing lemma. 
    * @return the ID for the word in the dictionary, if appropriate
    * @throws DictionaryReadOnlyException
    */
   default public String add(String key, String entry, String reference)
      throws DictionaryReadOnlyException, DictionaryException {
      return add(key, entry);
   }

   /**
    * Removes a key from the dictionary.
    * @param key
    * @return the ID for the word in the dictionary, if appropriate
    * @throws DictionaryReadOnlyException
    */
   default public String remove(String key)
      throws DictionaryReadOnlyException, DictionaryException {
      throw new DictionaryReadOnlyException(this);
   }

   /**
    * Removes a word entry from the dictionary.
    * @param key
    * @param entry
    * @return the ID for the word in the dictionary, if appropriate
    * @throws DictionaryReadOnlyException
    */
   default public String remove(String key, String entry)
      throws DictionaryReadOnlyException, DictionaryException {
      throw new DictionaryReadOnlyException(this);
   }

   /**
    * Returns a list of all editable words in the dictionary.
    * @return a map of keys to lists of definitions.
    * @throws DictionaryReadOnlyException
    */
   default public Map<String, List<String>> listEditableWords()
      throws DictionaryReadOnlyException, DictionaryException {
      return listEditableWords(0,0);
   }

   /**
    * Returns a list of all words in the dictionary.
    * @return a map of keys to their definitions. 
    * @throws DictionaryReadOnlyException
    */
   default public Map<String, List<String>> listAllWords()
      throws DictionaryReadOnlyException, DictionaryException {
      return listAllWords(0,0);
   }
   
   /**
    * Returns a sub-list of editable words in the dictionary - i.e. all
    * words between the two specified indexes.  This primarily allows LaBB-CAT to
    * paginate the list on the Edit Dictionary page.
    * @param start the(zero-based) index of the first word to list
    * @param length the number of entries to return, or 0, meaning to the end of the list
    * @return a map of keys to their definitions. 
    * @throws DictionaryReadOnlyException
    */
   default public Map<String, List<String>> listEditableWords(int start, int length)
      throws DictionaryReadOnlyException, DictionaryException {
      throw new DictionaryReadOnlyException(this);
   }

   /**
    * Returns a specific editable key from the dictionary.
    * @param key the key to look up
    * @return List of entries.
    * @throws DictionaryReadOnlyException
    */
   default public List<String> lookupEditableWord(String key)
      throws DictionaryReadOnlyException, DictionaryException {
      throw new DictionaryReadOnlyException(this);
   }

   /**
    * Suggests a possible entry for the given word.  This is intended for
    * words that have not yet been entered, to make it easier to add new
    * entries based on existing entries.  If the word is already in the
    * dictionary, then <i>OperationNotSupportedException</i> should be thrown.
    * @param key
    * @return a suggested entry for the given word, or null if no suggestion is possible
    * @throws OperationNotSupportedException When sWord is already in the dictionary
    */
   default public String suggest(String key) throws DictionaryException, DictionaryException {
      throw new DictionaryException(this, "Suggest not supported.");
   }
   
   /**
    * Looks up a key and provides all possible matches, in their <q>raw</q>
    * representation for dictionary editing.   
    * <p>The default implementation returns the result of {@link #lookup(String)},
    * but if an implementor wants to return a marked-up version of word definitions for
    * the purposes  of allowing the user to accurately edit the dictionary, this method
    * can be overridden. 
    */
   default public List<String> lookupRaw(String key) throws DictionaryException {
      return lookup(key); }

   /** 
    * Function method that looks up all entries for the given key.
    * <p> The default implementation calls {@link #lookup(String)}.
    */ 
   default public List<String> apply(String key) {
      try {
         return lookup(key);
      } catch(DictionaryException exception) {
         throw new RuntimeException(exception);
      }
   }
   
}


