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

import java.util.Vector;
import java.io.Writer;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * {@link TextGrid} tier subclass representing a tier of labelled time points.
 * @author Robert Fromont
 */

public class TextTier
   extends Tier
   implements ITextEntity
{
   // Attributes:
   private Vector<Point> vPoints = new Vector<Point>();
   /** Gets a list of time points - {@link Point} objects
    * @return A list of points.
    */
   public Vector<Point> getPoints() { return vPoints; }
   
   /**
    * Constructor
    */
   public TextTier()
   {
   } // end of constructor
   
   /**
    * Constructor
    * @param name Tier name
    * @param xmin Start time
    * @param xmax End time
    */
   public TextTier(String name, double xmin, double xmax)
   {
      super(name, xmin, xmax);
   }
   
   /**
    * Copy constructor (deep copy)
    * @param other the tier to copy
    */
   public TextTier(TextTier other)
   {
      super(other.getName(), other.getXmin(), other.getXmax());
      for (Object o: other.getPoints())
      {
	 addPoint(new Point((Point) o));
      } // next point
   } // end of copy()
   
   /**
    * Adds a Point object
    * @param point The point to add.
    */
   public void addPoint(Point point)
   {
      vPoints.add(point);
   } // end of addTier()
   
   /**
    * Returns the first non-blank point in the tier
    * @return The first point where {@link Point#getMark()} is not blank, or null if no such point exists
    */
   public Point firstNonBlankPoint()
   {
      for (Object o: vPoints)
      {
	 Point point = (Point) o;
	 if (point.getMark() != null 
	     && point.getMark().trim().length() > 0)
	 {
	    return point;
	 }
      } // next point
      return null;
   } // end of firstNonBlankPoint()
   
   /**
    * Returns the first non-blank point in the tier after the given time
    * @param dTime The minimum time.
    * @return The first point where {@link Point#getMark()} is not blank and {@link Point#getTime()} greater than <em>dTime</em>, or null if no such point exists
    */
   public Point firstPointAfter(double dTime)
   {
      for (Object o: vPoints)
      {
	 Point point = (Point) o;
	 if (point.getMark() != null 
	     && point.getMark().trim().length() > 0
	     && point.getTime() > dTime)
	 {
	    return point;
	 }
      } // next point
      return null;
   } // end of firstNonBlankPoint()
   
   // ITextEntity methods
   
   /**
    * Text-file representation of the object
    * @param writer The writer to serialize to.
    * @throws java.io.IOException If an IO error occurs.
    */
   public void writeText(Writer writer)
      throws java.io.IOException
   {
      super.writeText(writer);
      writer.write("\n        points: size = " + vPoints.size());
      for (int i = 0; i < vPoints.size(); i++)
      {
	 Point point = vPoints.elementAt(i);
	 writer.write("\n        points [" + (i+1) + "]:"); // 1-based
	 point.writeText(writer);
      } // next tier
   }
   
   /**
    * Reads the tier text
    * @param reader The reader to deserialize from.
    * @throws IOException If an IO error occurs.
    */
   public void readText(BufferedReader reader)
      throws IOException
   {
      super.readText(reader);
      // find tiers
      int iPointCount 
	 = Integer.parseInt(TextGrid.readValue("points: size", reader));
      for (int i = 0; i < iPointCount; i++)
      {
	 Point point = new Point();
	 point.readText(reader);
	 addPoint(point);
      }
      
   } // readText
} // end of class TextTier
