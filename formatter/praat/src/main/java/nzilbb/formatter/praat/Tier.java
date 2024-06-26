//
// Copyright 2004-2023 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.formatter.praat;

import java.io.Writer;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * {@link TextGrid} tier.
 * @author Robert Fromont
 */
public class Tier implements ITextEntity {
  // Attributes
  private String sName = "";
  /** Sets the tier name
   * @param name The name of the tier.
   */
  public void setName(String name) { sName = name; if (sName == null) sName = ""; }
  /** Gets the tier name
   * @return The name of the tier.
   */
  public String getName() { return sName; }
   
  private double dXmin = 0.0;
  /** Sets start time
   * @param xmin The start time in seconds.
   */
  public void setXmin(double xmin) { dXmin = xmin; }
  /** Gets start time
   * @return The start time in seconds.
   */
  public double getXmin() { return dXmin; }
   
  private double dXmax = 0.0;
  /** Sets end time
   * @param xmax The end time in seconds.
   */
  public void setXmax(double xmax) { dXmax = xmax; }
  /** Gets end time
   * @return The end time in seconds.
   */
  public double getXmax() { return dXmax; }
   
  // Methods
   
  /**
   * Constructor
   */
  public Tier() {
  }
   
  /**
   * Constructor.
   * @param name Tier name
   * @param xmin Start time
   * @param xmax End time
   */
  public Tier(String name, double xmin, double xmax) {
    setName(name);
    setXmin(xmin);
    setXmax(xmax);
  }
   
  // ITextEntity methods
   
  /**
   * Text-file representation of the object
   * @param writer The writer to write to.
   * @throws java.io.IOException If an IO error occurs.
   */
  public void writeText(Writer writer) throws java.io.IOException {
    writer.write(
      "\n        class = \"" + TextGrid.className(this) + "\" " +
      "\n        name = \"" + getName() + "\" " + 
      "\n        xmin = " + TextGrid.OffsetFormat.format(getXmin()) + " " +
      "\n        xmax = " + TextGrid.OffsetFormat.format(getXmax()) + " "); 
  }
   
  /**
   * Reads the tier text
   * @param reader The reader to read from.
   * @throws IOException If an IO error occurs.
   */
  public void readText(BufferedReader reader) throws IOException {
    // class has already been read
    setName(TextGrid.readValue("name", reader));
    setXmin(Double.parseDouble(TextGrid.readValue("xmin", reader)));
    setXmax(Double.parseDouble(TextGrid.readValue("xmax", reader)));
  }
   
  /**
   * String representation of the Tier
   * @return The name of the Tier
   */
  public String toString() {
    if (getName().length() == 0) {
      return "<unnamed tier>";
    } else {
      return getName();
    }
  } // end of toString()
   
}
