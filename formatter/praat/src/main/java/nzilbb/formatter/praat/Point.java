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
package nzilbb.formatter.praat;

import java.io.Writer;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * {@link TextTier} time-point.
 * @author Robert Fromont
 */

public class Point
   implements ITextEntity
{
   // Attributes:
   private double dTime = 0.0;
   /** Sets time point */
   public void setTime(double time) { dTime = time; }
   /** Gets time point */
   public double getTime() { return dTime; }
   
   private String sMark = "";
   /** Sets label */
   public void setMark(String mark) { sMark = mark; }
   /** Gets label */
   public String getMark() { return sMark; }
   
   /**
    * Constructor
    */
   public Point()
   {
   } // end of constructor
   
   /**
    * Constructor
    * @param mark
    * @param time
    */
   public Point(String mark, double time)
   {
      setMark(mark);
      setTime(time);
   } // end of constructor
   
   /**
    * Copy constructor
    */
   public Point(Point otherPoint)
   {
      setMark(otherPoint.getMark());
      setTime(otherPoint.getTime());
   } // end of constructor
   
   // ITextEntity methods
   
   /**
    * Text-file representation of the object.
    */
   public void writeText(Writer writer)
      throws java.io.IOException
   {
      writer.write(
	 "\n            time = " + getTime() +
	 "\n            mark = \"" + getMark().replace("\"", "\"\"") 
	 + "\"");
   }
   
   /**
    * Reads the point text
    * @param reader
    * @throws Exception
    */
   public void readText(BufferedReader reader)
      throws IOException
   {
      setTime(Double.parseDouble(TextGrid.readValue("((time)|(number))", reader)));
      setMark(TextGrid.readValue("mark", reader));
   }
   
   /**
    * Represents the point as a string.
    */
   public String toString()
   {
      return "" + getMark() + " (" + getTime() + ")";
   } // end of toString()
   
} // end of class Point
