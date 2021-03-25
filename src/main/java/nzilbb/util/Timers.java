//
// Copyright 2015 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of LaBB-CAT.
//
//    LaBB-CAT is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 2 of the License, or
//    (at your option) any later version.
//
//    LaBB-CAT is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with LaBB-CAT; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package nzilbb.util;

import java.util.LinkedHashMap;
import java.util.Date;

/**
 * Maintains a set of named timers, primarily for performance tests.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class Timers
{
   // Attributes:
   
   /**
    * Currently running timers - a map of IDs to start times.
    * @see #getRunningTimers()
    * @see #setRunningTimers(LinkedHashMap)
    */
   protected LinkedHashMap<String,Long> runningTimers = new LinkedHashMap<String,Long>();
   /**
    * Getter for {@link #runningTimers}: Currently running timers - a map of IDs to start times.
    * @return Currently running timers - a map of IDs to start times.
    */
   public LinkedHashMap<String,Long> getRunningTimers() { return runningTimers; }
   /**
    * Setter for {@link #runningTimers}: Currently running timers - a map of IDs to start times.
    * @param newRunningTimers Currently running timers - a map of IDs to start times.
    */
   public Timers setRunningTimers(LinkedHashMap<String,Long> newRunningTimers) { runningTimers = newRunningTimers; return this; }
   
   /**
    * Timer totals - a map of IDs to total run times in ms.
    * @see #getTotals()
    * @see #setTotals(LinkedHashMap)
    */
   protected LinkedHashMap<String,Long> totals = new LinkedHashMap<String,Long>();
   /**
    * Getter for {@link #totals}: Timer totals - a map of IDs to total run times.
    * @return Timer totals - a map of IDs to total run times.
    */
   public LinkedHashMap<String,Long> getTotals() { return totals; }
   /**
    * Setter for {@link #totals}: Timer totals - a map of IDs to total run times.
    * @param newTotals Timer totals - a map of IDs to total run times.
    */
   public Timers setTotals(LinkedHashMap<String,Long> newTotals) { totals = newTotals; return this; }
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public Timers()
   {
   } // end of constructor
   
   /**
    * Start a timer.
    * @param id The ID of the timer.
    * @return The current time.
    */
   public long start(String id)
   {
      long now = new Date().getTime();
      runningTimers.put(id, now);
      return now;
   } // end of start()

   /**
    * Ends a timer and adds the duration to the total for the given ID.
    * @param id The ID of the timer.
    * @return Duration for this timer.
    */
   public long end(String id)
   {
      long now = new Date().getTime();
      Long startTime = runningTimers.get(id);
      if (startTime == null) return 0;
      runningTimers.remove(id);

      long duration = now - startTime;
      if (totals.containsKey(id))
      {
	 totals.put(id, totals.get(id) + duration);
      }
      else
      {
	 totals.put(id, duration);
      }
      return duration;
   } // end of end()
   
   /**
    * String representation of the totals.
    * @return String representation of the totals.
    */
   public String toString()
   {
      StringBuffer s = new StringBuffer();
      for (String id : totals.keySet())
      {
	 s.append(id);
	 s.append(": ");
	 s.append(totals.get(id));
	 s.append("\n");
      } // next key
      return s.toString();
   } // end of toString()

} // end of class Timers
