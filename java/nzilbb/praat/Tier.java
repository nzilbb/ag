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
 * {@link TextGrid} tier.
 * @author Robert Fromont
 */

public class Tier
   implements ITextEntity
{
   // Attributes
   private String sName = "";
   /** Sets the tier name */
   public void setName(String name) { sName = name; if (sName == null) sName = ""; }
   /** Gets the tier name */
   public String getName() { return sName; }
   
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
   
   // Methods
   
   /**
    * Constructor
    */
   public Tier()
   {
   }
   
   /**
    * Constructor.
    * @param name Tier name
    * @param xmin Start time
    * @param xmax End time
    */
   public Tier(String name, double xmin, double xmax)
   {
      setName(name);
      setXmin(xmin);
      setXmax(xmax);
   }
   
   // ITextEntity methods
   
   /**
    * Text-file representation of the object
    * @param writer
    * @throws java.io.IOException
    */
   public void writeText(Writer writer)
      throws java.io.IOException
   {
      writer.write(
	 "\n        class = \"" + TextGrid.className(this) + "\"" +
	 "\n        name = \"" + getName() + "\"" + 
	 "\n        xmin = " + getXmin() +
	 "\n        xmax = " + getXmax()); 
   }
   
   /**
    * Reads the tier text
    * @param reader
    * @throws Exception
    */
   public void readText(BufferedReader reader)
      throws IOException
   {
      // class has already been read
      setName(TextGrid.readValue("name", reader));
      setXmin(Double.parseDouble(TextGrid.readValue("xmin", reader)));
      setXmax(Double.parseDouble(TextGrid.readValue("xmax", reader)));
   }
   
   /**
    * String representation of the Tier
    * @return The name of the Tier
    */
   public String toString()
   {
      if (getName().length() == 0)
      {
	 return "<unnamed tier>";
      }
      else
      {
	 return getName();
      }
   } // end of toString()
   
   /**
    * Compares for equality - i.e. two tiers are equal if they are of the same type, have the same name, and the same Xmin and Xmax.
    * @param o
    * @return true if the given object is a Tier, the two tiers are of the same type, have the same name, and the same Xmin and Xmax, false otherwise
    */
   public boolean equals(Object o)
   {
      if (getClass().equals(o.getClass()))
      {
	 Tier other = (Tier)o;
	 return getName().equals(other.getName())
	    && getXmin() == other.getXmin()
	    && getXmax() == other.getXmax();
      }
      return false;
   } // end of equals()   
}
