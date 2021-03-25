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
package nzilbb.util;

import java.sql.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * A task whose progress can be monitored, and which can be cancelled.
 * @author Robert Fromont robert@fromont.net.nz
 */
public interface MonitorableTask {
   /**
    * Determines how far through the task is is.
    * @return An integer between 0 and 100 (inclusive), or null if progress can not be calculated.
    */
   public Integer getPercentComplete();
   
   /** Cancels the task. */
   public void cancel();

   /**
    * Reveals whether the task is still running or not.
    * @return true if the task is currently running, false otherwise.
    */
   public boolean getRunning();
   
   /**
    * Returns a unique identifier for the task.
    * <p> The default implementation returns the ID of the current thread.
    * @return A unique identifier.
    */
   default public String getTaskId() { return ""+Thread.currentThread().getId(); }
   
   /**
    * Reveals the current status of the task.
    * <p> The default implementation always returns an empty string.
    * @return A description of the current status of the task for displaying to the user,
    * or an empty string.
    */
   default public String getStatus() { return ""; }
} // end of class MonitorableSeries
