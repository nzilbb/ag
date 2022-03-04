//
// Copyright 2017 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.media;

import nzilbb.util.Execution;

/**
 * Thread processing media.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class MediaThread extends Thread {
  // Attributes:
   
  /**
   * Percent complete.
   * @see #getPercentComplete()
   * @see #setPercentComplete(int)
   */
  protected int percentComplete = 0;
  /**
   * Getter for {@link #percentComplete}: Percent complete.
   * @return Percent complete.
   */
  public int getPercentComplete() {      
    if (percentComplete == 0 && execution != null && execution.getFinished()) return 100;
    return percentComplete;
  }
  /**
   * Setter for {@link #percentComplete}: Percent complete.
   * @param newPercentComplete Percent complete.
   */
  public MediaThread setPercentComplete(int newPercentComplete) { percentComplete = newPercentComplete; return this; }
   
  /**
   * The external process Execution object, if the thread was given one.
   * @see #getExecution()
   * @see #setExecution(Execution)
   */
  protected Execution execution;
  /**
   * Getter for {@link #execution}: The external process Execution object, if the thread
   * was given one. 
   * @return The external process Execution object, if the thread was given one.
   */
  public Execution getExecution() { return execution; }
  /**
   * Setter for {@link #execution}: The external process Execution object, if the thread
   * was given one. 
   * @param newExecution The external process Execution object, if the thread was given one.
   */
  public MediaThread setExecution(Execution newExecution) { execution = newExecution; return this; }
   
  /**
   * Last error, if any.
   * @see #getLastError()
   * @see #setLastError(Throwable)
   */
  protected Throwable lastError;
  /**
   * Getter for {@link #lastError}: Last error, if any.
   * @return Last error, if any.
   */
  public Throwable getLastError() { return lastError; }
  /**
   * Setter for {@link #lastError}: Last error, if any.
   * @param newLastError Last error, if any.
   */
  public MediaThread setLastError(Throwable newLastError) { lastError = newLastError; return this; }
   
  // Methods:
   
  /**
   * Default constructor.
   */
  public MediaThread() {
    super();
  } // end of constructor

  /**
   * Constructor with target.
   */
  public MediaThread(Runnable target) {
    super(target);
    if (target instanceof Execution) {
      setExecution((Execution)target);
    }
  }
} // end of class MediaThread
