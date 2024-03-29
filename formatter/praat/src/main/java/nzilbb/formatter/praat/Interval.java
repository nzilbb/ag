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
 * {@link IntervalTier} time interval.
 * @author Robert Fromont
 */

public class Interval implements ITextEntity {
  // Attributes:
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
   
  private String sText = "";
  /** Sets label
   * @param text The label.
   */
  public void setText(String text) { sText = text; }
  /** Gets label
   * @return The label.
   */
  public String getText() { if (sText == null) return ""; else return sText; }
   
  /**
   * Constructor
   */
  public Interval() {
  } // end of constructor
   
  /**
   * Constructor
   * @param text The label for the interval.
   * @param xMin The start time in seconds.
   * @param xMax The end time in seconds.
   */
  public Interval(String text, double xMin, double xMax) {
    setText(text);
    setXmin(xMin);
    setXmax(xMax);
  } // end of constructor
   
  /**
   * Copy constructor
   * @param other The interval to copy.
   */
  public Interval(Interval other) {
    setText(other.getText());
    setXmin(other.getXmin());
    setXmax(other.getXmax());
  } // end of constructor
   
  /**
   * Returns the halway point between Xmin and Xmax
   * @return Xmin + ( (Xmax-Xmin) / 2 )
   */
  public double getMidPoint() {
    return getXmin() + ( (getXmax()-getXmin()) / 2.0 );
  } // end of getMidPoint()
   
  // ITextEntity methods
   
  /**
   * Text-file representation of the object.
   */
  public void writeText(Writer writer) throws java.io.IOException {
    writer.write( 
      "\n            xmin = " + TextGrid.OffsetFormat.format(Math.min(getXmin(),getXmax())) + " " +
      "\n            xmax = " + TextGrid.OffsetFormat.format(Math.max(getXmin(),getXmax())) + " " +
      "\n            text = \"" + getText().replace("\"", "\"\"") + "\" ");
  }
   
  /**
   * Deserializes the interval.
   * @param reader The reader to read from.
   * @throws IOException If an IO error occurs.
   */
  public void readText(BufferedReader reader) throws IOException {
    setXmin(Double.parseDouble(TextGrid.readValue("xmin", reader)));
    setXmax(Double.parseDouble(TextGrid.readValue("xmax", reader)));
    setText(TextGrid.readValue("text", reader));
  }
   
  /**
   * String representation of the object
   */
  public String toString() {
    return "xmin = " + getXmin() +
      ", xmax = " + getXmax() +
      ", text = \"" + getText().replace("\"", "\"\"")
      + "\"";
  }
} // end of class Interval
