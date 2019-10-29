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
import java.util.Spliterator;
import java.util.function.Consumer;
import nzilbb.ag.*;
import nzilbb.util.MonitorableSeries;

/**
 * An implementation of Spliterator&lt;Graph&gt; that enumerates fragments corresponding to a search result set.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class ResultSeries
  implements MonitorableSeries<Graph>
{
   // Attributes:

   private PreparedStatement sql;
   private ResultSet rs;
   private long nextRow = 0;
   private long rowCount = -1;
   private boolean cancelling = false;   

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
   public ResultSeries setStore(SqlGraphStore newStore) { store = newStore; return this; }

   /**
    * <tt>result.search_id</tt> key value.
    * @see #getSearchId()
    * @see #setSearchId(long)
    */
   protected long searchId;
   /**
    * Getter for {@link #searchId}: <tt>result.search_id</tt> key value.
    * @return <tt>result.search_id</tt> key value.
    */
   public long getSearchId() { return searchId; }
   /**
    * Setter for {@link #searchId}: <tt>result.search_id</tt> key value.
    * @param newSearchId <tt>result.search_id</tt> key value.
    */
   public ResultSeries setSearchId(long newSearchId) { searchId = newSearchId; return this; }

   
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
   public ResultSeries setLayers(String[] newLayers) { layers = newLayers; return this; }
   
   // Methods:
   
   /**
    * Constructor.
    * @throws SQLException If an error occurs retrieving results.
    */
   public ResultSeries(long search_id, SqlGraphStore store, String[] layers)
      throws SQLException
   {
      setSearchId(search_id);
      setStore(store);
      setLayers(layers);

      sql = store.getConnection().prepareStatement(
	 "SELECT COUNT(*) FROM result WHERE search_id = ?");
      sql.setLong(1, getSearchId());
      rs = sql.executeQuery();
      rs.next();
      rowCount = rs.getLong(1);
      rs.close();
      sql.close();
      
      sql = store.getConnection().prepareStatement(
	 "SELECT match_id, result.ag_id, defining_annotation_id,"
         +" COALESCE(anchor.offset,0) AS start_offset"
	 +" FROM result"
         +" LEFT OUTER JOIN anchor ON anchor.anchor_id = result.start_anchor_id"
	 +" WHERE search_id = ?"
	 +" ORDER BY match_id");
      sql.setLong(1, getSearchId());
      rs = sql.executeQuery();
      
   } // end of constructor

   
   /**
    * Finalize method called by the garbage collector.
    * <p> This implementation ensures SQL resources are disposed of.
    */
   @SuppressWarnings("deprecation")
   public void finalize()
   {
      if (rs != null) try { rs.close(); } catch(Throwable t) {}
      if (sql != null) try { sql.close(); } catch(Throwable t) {}
   } // end of finalize()

   // Spliterator implementations

   public int characteristics()
   {
      return ORDERED | DISTINCT | IMMUTABLE | NONNULL | SUBSIZED | SIZED;
   }

   /**
    * Tests if this enumeration contains more elements.
    */
   public boolean hasMoreElements()
   {
      if (rs == null) return false;
      if (nextRow >= rowCount)
      {
	 if (rs != null) try { rs.close(); } catch(Throwable t) {}
	 rs = null;
	 if (sql != null) try { sql.close(); } catch(Throwable t) {}
	 sql = null;
	 return false;
      }
      return true;
   }

   /**
    * Returns the next element of this enumeration if this enumeration object has at least one more element to provide.
    */
   public boolean tryAdvance(Consumer<? super Graph> action)
   {
      if (cancelling) return false;
      if (!hasMoreElements()) return false;
      try
      {
	 rs.next();
	 nextRow++;
         Graph fragment = store.getFragment(
            rs.getString("ag_id"), "em_12_"+rs.getLong("defining_annotation_id"), layers);
         fragment.shiftAnchors(-rs.getDouble("start_offset"));
	 action.accept(fragment);
         return true;
      }
      catch(Exception exception)
      {
	 return false;
      }
   }

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
   
   // GraphSeries methods

   /**
    * Determines how far through the serialization is.
    * @return An integer between 0 and 100 (inclusive), or null if progress can not be calculated.
    */
   public Integer getPercentComplete()
   {
      if (rowCount > 0)
      {
	 return (int)((nextRow * 100) / rowCount);
      }
      return null;
   }   

   /**
    * Cancels spliteration; the next call to tryAdvance will return false.
    */
   public void cancel()
   {
      cancelling = true;
   }
} // end of class ResultSeries
