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
package nzilbb.ag.automation.example.theworks;

import java.util.TreeMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.Dictionary;
import nzilbb.ag.automation.DictionaryException;
import nzilbb.ag.automation.DictionaryReadOnlyException;

/**
 * This is an example annotator dictionary.
 */
@SuppressWarnings("serial")
public class TheWorksExampleDictionary extends TreeMap<String,Integer> implements Dictionary {

   /**
    * Constructor.
    */
   public TheWorksExampleDictionary(Annotator annotator) {
      this.annotator = annotator;
   }

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
    * {@link Dictionary} method - Look up all entries for the given key.
    */ 
   public List<String> lookup(String key) throws DictionaryException {
      Vector<String> list = new Vector<String>();
      if (containsKey(key)) list.add(get(key).toString());
      return list;
   }

   /**
    * {@link Dictionary} method - Returns a count of all entries in the dictionary.
    * @return the number of keys that would be returned by a call to listAllEntries
    */
   public int countAllKeys() throws DictionaryException {
      return size();
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
      TreeMap<String, List<String>> entries = new TreeMap<String, List<String>>();
      int i = 0;
      for (String key : keySet()) {
         if (i++ < start) continue;
         Integer entry = get(key);
         entries.put(key, new Vector<String>() {{ add(entry.toString()); }});
         if (entries.size() >= length) break;
      } // next entry
      return entries;
   }
      
   /**
    * {@link Dictionary} method - Determines whether the dictionary is entirely read-only, or has
    * facility for adding/editing entries.
    * @return true if there is no facility to add/edit entries in the
    * dictionary, false otherwise
    */
   public boolean isReadOnly() {
      return true;
   }

   /**
    * {@link Dictionary} method - Returns a count of all editable entries in the dictionary.
    * @return the number of entries that would be returned by a call to listEditableEntries
    * @throws DictionaryReadOnlyException
    */
   public int countEditableEntries()
      throws DictionaryReadOnlyException, DictionaryException {
      return 0;
   }

   /**
    * {@link Dictionary} method - Returns an aggregrate of all values of all entries in the
    * dictionary.
    * @param operation The aggregation operation - e.g. MAX, MIN, COUNT, or SUM (e.g. to
    * get the sum of all entry frequencies in a frequency entry-list. The supported
    * operations depend on the implementor. 
    * @return The result of the aggregation, or null if the operation is not supported.
    */
   public String aggregateEntries(String operation) throws DictionaryException {
      if ("COUNT".equals(operation)) return ""+size();
      return null;
   }

   /**
    * {@link Dictionary} method - Returns an aggregrate of all entries (i.e. keys) in the dictionary. This default
    * implementation always returns null. 
    * @param operation The aggregation operation - e.g. MAX, MIN, COUNT, or SUM (e.g. to
    * get the sum of all entry frequencies in a frequency entry-list. The supported
    * operations depend on the implementor. 
    * @return The result of the aggregation, or null if the operation is not supported.
    * @throws DictionaryException
    */
   public String aggregateKeys(String operation) throws DictionaryException {
      if ("COUNT".equals(operation)) return ""+size();
      if ("MIN".equals(operation) && size() > 0) return ""+keySet().iterator().next();
      return null;
   }

   /**
    * {@link Dictionary} method - Adds a entry to the dictionary.
    * @param key The key to add an entry for - e.g. the entry orthorgraphy.
    * @param entry The entry for the key - e.g. its pronunciation.
    * @return the ID for the entry in the dictionary, if appropriate
    */
   public String add(String key, String entry)
      throws DictionaryReadOnlyException, DictionaryException {
      throw new DictionaryReadOnlyException(this);
   }

   /**
    * {@link Dictionary} method - Adds a entry to the dictionary with a given reference.
    * The implementation of this method simply calls {@link #add(String,String)} -
    * i.e. the reference is ignored.
    * @param key The key to add an entry for - e.g. the entry orthorgraphy.
    * @param entry The entry for the key - e.g. its pronunciation.
    * @param reference A reference for the entry. This is implementation-specific, but is
    * intended as a mechanism for allowing a new entryform to be associated with an
    * existing lemma. 
    * @return the ID for the entry in the dictionary, if appropriate
    * @throws DictionaryReadOnlyException
    */
   public String add(String key, String entry, String reference)
      throws DictionaryReadOnlyException, DictionaryException {
      return add(key, entry);
   }

   /**
    * {@link Dictionary} method - Removes a key from the dictionary.
    * @param key
    * @return the ID for the entry in the dictionary, if appropriate
    * @throws DictionaryReadOnlyException
    */
   public String remove(String key)
      throws DictionaryReadOnlyException, DictionaryException {
      throw new DictionaryReadOnlyException(this);
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
      throw new DictionaryReadOnlyException(this);
   }

   /**
    * {@link Dictionary} method - Returns a list of all editable entries in the dictionary.
    * @return a map of keys to lists of definitions.
    * @throws DictionaryReadOnlyException
    */
   public Map<String, List<String>> listEditableEntries()
      throws DictionaryReadOnlyException, DictionaryException {
      return listEditableEntries(0,0);
   }

   /**
    * {@link Dictionary} method - Returns a list of all entries in the dictionary.
    * @return a map of keys to their definitions. 
    * @throws DictionaryReadOnlyException
    */
   public Map<String, List<String>> listAllEntries()
      throws DictionaryReadOnlyException, DictionaryException {
      return listAllEntries(0,0);
   }
   
   /**
    * {@link Dictionary} method - Returns a sub-list of editable entries in the dictionary - i.e. all
    * entries between the two specified indexes.  This primarily allows LaBB-CAT to
    * paginate the list on the Edit Dictionary page.
    * @param start the(zero-based) index of the first entry to list
    * @param length the number of entries to return, or 0, meaning to the end of the list
    * @return a map of keys to their definitions. 
    * @throws DictionaryReadOnlyException
    */
   public Map<String, List<String>> listEditableEntries(int start, int length)
      throws DictionaryReadOnlyException, DictionaryException {
      throw new DictionaryReadOnlyException(this);
   }

   /**
    * {@link Dictionary} method - Returns a specific editable key from the dictionary.
    * @param key the key to look up
    * @return List of entries.
    * @throws DictionaryReadOnlyException
    */
   public List<String> lookupEditableEntry(String key)
      throws DictionaryReadOnlyException, DictionaryException {
      throw new DictionaryReadOnlyException(this);
   }

   /**
    * {@link Dictionary} method - Suggests a possible entry for the given entry.  This is
    * intended for entries that have not yet been entered, to make it easier to add new
    * entries based on existing entries.  If the entry is already in the
    * dictionary, then <i>OperationNotSupportedException</i> should be thrown.
    * @param key
    * @return a suggested entry for the given entry, or null if no suggestion is possible
    * @throws OperationNotSupportedException When sEntry is already in the dictionary
    */
   public String suggest(String key) throws DictionaryException, DictionaryException {
      throw new DictionaryException(this, "Suggest not supported.");
   }
   
   /**
    * {@link Dictionary} method - Looks up a key and provides all possible matches, in
    * their <q>raw</q> representation for dictionary editing.   
    * <p>The implementation returns the result of {@link #lookup(String)},
    * but if an implementor wants to return a marked-up version of entry definitions for
    * the purposes  of allowing the user to accurately edit the dictionary, this method
    * can be overridden. 
    */
   public List<String> lookupRaw(String key) throws DictionaryException {
      return lookup(key);
   }

   /** {@link Dictionary} method - Frees any resources reserved by the dictionary */
   public void close() {
      // if we had an open database connection, we'd close it here
   }
}
