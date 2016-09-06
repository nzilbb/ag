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

import java.util.Vector;
import java.util.NoSuchElementException;
import java.io.Writer;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * {@link TextGrid} tier subclass, representing a collection of labelled time intervals.
 * @author Robert Fromont
 */

public class IntervalTier
   extends Tier
   implements ITextEntity
{
   // Attributes:
   private Vector<Interval> vIntervals = new Vector<Interval>();
   /** Gets a list of time intervals - {@link Interval} objects */
   public Vector<Interval> getIntervals() { return vIntervals; }
   
   private boolean bAutoPad = false;
   /** Gets the auto-pad setting - whether the intervals are automatically padded with blank intervals if the end time of the last interval doesn't match the start time of a newly added interval */
   public boolean getAutoPad() { return bAutoPad; }
   /** Sets the auto-pad setting - whether the intervals are automatically padded with blank intervals if the end time of the last interval doesn't match the start time of a newly added interval */
   public void setAutoPad(boolean autoPad) { bAutoPad = autoPad; }
   
   /**
    * Constructor
    */
   public IntervalTier()
   {
   } // end of constructor
   
   /**
    * Constructor
    * @param name Tier name
    * @param xmin Start time
    * @param xmax End time
    */
   public IntervalTier(String name, double xmin, double xmax)
   {
      super(name, xmin, xmax);
   }
   
   /**
    * Copy Constructor (deep copy)
    * @param other the tier to copy
    */
   public IntervalTier(IntervalTier other)
   {
      super(other.getName(), other.getXmin(), other.getXmax());
      for (Object o: other.getIntervals())
      {
	 addInterval(new Interval((Interval) o));
      } // next point
   }

   /**
    * Adds an interval object.  Automatically pads the tier out with an
    * intervening blank tier if this interval starts after the end of
    * the last one, and getAutoPad() is true.
    * @param interval
    */
   public void addInterval(Interval interval)
   {
      if (getAutoPad()) padTier(interval.getXmin());
      
      vIntervals.add(interval);
      if (getXmax() < interval.getXmax())
      {
	 setXmax(interval.getXmax());
      }
   } // end of addTier()
   
   /**
    * If necessary, adds a blank interval after the last interval, up to the
    * given time, to ensure a contiguous collection of intervals.
    * @param dEndTime
    */
   public void padTier(double dEndTime)
   {
      // add a blank interval from the last one up to now
      if (dEndTime > 0.0)
      {
	 Interval blankIntervening = new Interval();
	 try
	 {
	    Interval lastInterval = getIntervals().lastElement();
	    blankIntervening.setXmin(lastInterval.getXmax());
	 }
	 catch(NoSuchElementException ex)
	 {}
	 blankIntervening.setXmax(dEndTime);
	 if (blankIntervening.getXmin() 
	     < blankIntervening.getXmax())
	 { // only if it's really an interval
	    addInterval(blankIntervening);
	 }
      }
   } // end of padTier()
   
   // ITextEntity methods
   
   /**
    * Text-file representation of the object.
    * @param writer
    * @throws java.io.IOException
    */
   public void writeText(Writer writer)
      throws java.io.IOException
   {
      super.writeText(writer);
      writer.write("\n        intervals: size = " + vIntervals.size());
      for (int i = 0; i < vIntervals.size(); i++)
      {
	 Interval interval = vIntervals.elementAt(i);
	 writer.write("\n        intervals [" + (i+1) + "]:"); // 1-based
	 interval.writeText(writer);
      } // next tier
   }
   
   /**
    * Reads the tier text
    * @param reader
    * @throws Exception
    */
   public void readText(BufferedReader reader)
      throws IOException
   {
      super.readText(reader);
      // find tiers
      int iIntervalCount = Integer.parseInt(TextGrid.readValue("intervals: size", reader));
      for (int i = 0; i < iIntervalCount; i++)
      {
	 Interval interval = new Interval();
	 interval.readText(reader);
	 addInterval(interval);
      } // next interval
      
   } // readText
   
   /**
    * Returns the first non-blank interval in the tier
    * @return The first interval where {@link Interval#getText()} is not blank, or null if no such interval exists
    */
   public Interval firstNonBlankInterval()
   {
      for (Object o: vIntervals)
      {
	 Interval interval = (Interval) o;
	 if (interval.getText() != null 
	     && interval.getText().trim().length() > 0)
	 {
	    return interval;
	 }
      } // next tier
      return null;
   } // end of firstNonBlankInterval()
   
   /**
    * Returns the first non-blank interval in the tier that occurs after the given time
    * @param dTime
    * @return The first interval where {@link Interval#getText()} is not blank and {@link Interval#getXmin()} is greater than <em>dTime</em>, or null if no such interval exists
    */
   public Interval firstIntervalAfter(double dTime)
   {
      for (Object o: vIntervals)
      {
	 Interval interval = (Interval) o;
	 if (interval.getText() != null 
	     && interval.getText().trim().length() > 0
	     && interval.getXmin() > dTime)
	 {
	    return interval;
	 }
      } // next tier
      return null;
   } // end of firstIntervalAfter()

} // end of class IntervalTier
