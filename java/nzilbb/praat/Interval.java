//
// Copyright 2004-2016 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.praat;

import java.io.Writer;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * {@link IntervalTier} time interval.
 * @author Robert Fromont
 */

public class Interval
   implements ITextEntity
{
   // Attributes:
   private double dXmin = 0.0;
   /** Sets start time */
   public void setXmin(double xmin) { dXmin = xmin; }
   /** Gets start time */
   public double getXmin() { return dXmin; }
   
   private double dXmax = 0.0;
   /** Sets end time */
   public void setXmax(double xmax) { dXmax = xmax; }
   /** Gets end time */
   public double getXmax() { return dXmax; }
   
   private String sText = "";
   /** Sets label */
   public void setText(String text) { sText = text; }
   /** Gets label */
   public String getText() { if (sText == null) return ""; else return sText; }
   
   /**
    * Constructor
    */
   public Interval()
   {
   } // end of constructor
   
   /**
    * Constructor
    * @param text
    * @param xMin
    * @param xMax
    */
   public Interval(String text, double xMin, double xMax)
   {
      setText(text);
      setXmin(xMin);
      setXmax(xMax);
   } // end of constructor
   
   /**
    * Copy constructor
    */
   public Interval(Interval other)
   {
      setText(other.getText());
      setXmin(other.getXmin());
      setXmax(other.getXmax());
   } // end of constructor
   
   /**
    * Returns the halway point between Xmin and Xmax
    * @return Xmin + ( (Xmax-Xmin) / 2 )
    */
   public double getMidPoint()
   {
      return getXmin() + ( (getXmax()-getXmin()) / 2.0 );
   } // end of getMidPoint()
   
   // ITextEntity methods
   
   /**
    * Text-file representation of the object.
    */
   public void writeText(Writer writer)
      throws java.io.IOException
   {
      writer.write( 
	 "\n            xmin = " + Math.min(getXmin(),getXmax()) +
	 "\n            xmax = " + Math.max(getXmin(),getXmax()) +
	 "\n            text = \"" + getText().replaceAll("\\\"", "\'") 
	 + "\"");
   }
   
   /**
    * Reads the tier text
    * @param reader
    * @throws Exception
    */
   public void readText(BufferedReader reader)
      throws IOException
   {
      setXmin(Double.parseDouble(TextGrid.readValue("xmin", reader)));
      setXmax(Double.parseDouble(TextGrid.readValue("xmax", reader)));
      setText(TextGrid.readValue("text", reader));
   }
   
   /**
    * String representation of the object
    */
   public String toString()
   {
      return "xmin = " + getXmin() +
	 ", xmax = " + getXmax() +
	 ", text = \"" + getText().replaceAll("\\\"", "\'") 
	 + "\"";
   }
} // end of class Interval
