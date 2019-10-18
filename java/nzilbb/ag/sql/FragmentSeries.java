//
// Copyright 2019 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag.sql;

import java.sql.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import nzilbb.ag.*;

/**
 * An implementation of Spliterator&lt;Graph&gt; that enumerates fragments corresponding to a list of selected fragment Ids.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class FragmentSeries
  implements Spliterator<Graph>
{
   // Attributes:

   private long nextRow = 0;
   private long rowCount = -1;
   private Iterator<String> iterator;

   /**
    * The graph store object.
    * @see #getStore()
    * @see #setStore(SqlGraphStore)
    */
   protected SqlGraphStore store;
   /**
    * Getter for {@link #store}: The graph store object.
    * @return The graph store object.
    */
   public SqlGraphStore getStore() { return store; }
   /**
    * Setter for {@link #store}: The graph store object.
    * @param newStore The graph store object.
    */
   public FragmentSeries setStore(SqlGraphStore newStore) { store = newStore; return this; }

   /**
    * A collection of strings that identify a graph fragment.
    * @see #getFragmentIds()
    * @see #setFragmentIds(Collection)
    */
   protected Collection<String> fragmentIds;
   /**
    * Getter for {@link #fragmentIds}: A collection of strings that identify a graph fragment.
    * @return A collection of strings that identify a graph fragment.
    */
   public Collection<String> getFragmentIds() { return fragmentIds; }
   /**
    * Setter for {@link #fragmentIds}: A collection of strings that identify a graph fragment.
    * @param newFragmentIds A collection of strings that identify a graph fragment.
    */
   public FragmentSeries setFragmentIds(Collection<String> newFragmentIds) { fragmentIds = newFragmentIds; return this; }
   
   /**
    * Layers to load into the fragments.
    * @see #getLayers()
    * @see #setLayers(String[])
    */
   protected String[] layers;
   /**
    * Getter for {@link #layers}: Layers to load into the fragments.
    * @return Layers to load into the fragments.
    */
   public String[] getLayers() { return layers; }
   /**
    * Setter for {@link #layers}: Layers to load into the fragments.
    * @param newLayers Layers to load into the fragments.
    */
   public FragmentSeries setLayers(String[] newLayers) { layers = newLayers; return this; }
   
   // Methods:
   
   /**
    * Constructor.
    * @param fragmentIds A collection of strings that identify a graph fragment.
    * @throws SQLException If an error occurs retrieving results.
    */
   public FragmentSeries(Collection<String> fragmentIds, SqlGraphStore store, String[] layers)
      throws SQLException
   {
      setFragmentIds(fragmentIds);
      setStore(store);
      setLayers(layers);
      rowCount = fragmentIds.size();
      iterator = fragmentIds.iterator();
   } // end of constructor

   // Spliterator implementations
   
   public int characteristics()
   {
      return ORDERED | DISTINCT | IMMUTABLE | NONNULL | SUBSIZED | SIZED;
   }
   
   /**
    * Returns the next element of this enumeration if this enumeration object has at least one more element to provide.
    */
   public boolean tryAdvance(Consumer<? super Graph> action)
   {
      if (!iterator.hasNext()) return false;
      try
      {
         String spec = iterator.next();
	 nextRow++;
         String[] parts = spec.split(";");
	 String graphId = parts[0];
	 String[] interval = parts[1].split("-");
	 double start = Double.parseDouble(interval[0]);
	 double end = Double.parseDouble(interval[1]);
	 String prefix = parts.length > 2 && parts[2].startsWith("prefix=")?
            parts[2].substring("prefix=".length()):"";

         Graph fragment = store.getFragment(graphId, start, end, layers);
         if (prefix.length() > 0) fragment.setId(prefix + fragment.getId());         
	 action.accept(fragment);
         return true;
      }
      catch(Exception exception)
      {
	 return false;
      }
   }

   // Series methods

   /**
    * Counts the elements in the series, if possible.
    * @return The number of elements in the series, or null if the number is unknown.
    */
   public long estimateSize()
   {
      if (rowCount >= 0) return rowCount;
      return Long.MAX_VALUE;
   }

   public Spliterator<Graph> trySplit()
   {
      return null;
   }
   
   /**
    * Determines how far through the serialization is.
    * @return An integer between 0 and 100 (inclusive), or null if progress can not be calculated.
    */
   public Integer percentComplete()
   {
      if (rowCount > 0)
      {
	 return (int)((nextRow * 100) / rowCount);
      }
      return null;
   }   
} // end of class FragmentSeries
