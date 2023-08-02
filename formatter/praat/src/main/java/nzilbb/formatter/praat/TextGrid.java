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

import nzilbb.ag.serialize.SerializationException;
import java.util.Vector;
import java.util.Locale;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.Writer;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Praat text-grid.
 * @author Robert Fromont
 */
public class TextGrid implements ITextEntity {
  static final DecimalFormat OffsetFormat = new DecimalFormat(
    "0.#################",
    // force the decimal separator to be '.':
    new DecimalFormatSymbols(Locale.UK));
  // Attributes:
   
  /** The start time. */
  protected double dXmin = 0.0;
  /** Sets start time
   * @param xmin Start tim in seconds.
   */
  public void setXmin(double xmin) { dXmin = xmin; }
  /** Gets start time
   * @return Start time in seconds.
   */
  public double getXmin() { return dXmin; }

  /** The end time. */
  protected double dXmax = 0.0;
  /** Sets end time
   * @param xmax End time in seconds.
   */
  public void setXmax(double xmax) { dXmax = xmax; }
  /** Gets start time
   * @return End time in seconds.
   */
  public double getXmax() { return dXmax; }

  /** The list of tiers. */
  protected Vector<Tier> vTiers = new Vector<Tier>();
  /** Gets list of {@link Tier} objects
   * @return A list of tiers.
   */
  public Vector<Tier> getTiers() { return vTiers; }
   
  /**
   * Constructor
   */
  public TextGrid() {
  } // end of constructor

  /**
   * Constructor
   * @param f Textgrid file to read from
   * @throws IOException If an IO error occurs
   * @throws ClassNotFoundException If an unknown object is encountered.
   * @throws InstantiationException If an object can't be instantiated.
   * @throws Exception If any other error occurs.
   */
  public TextGrid(File f) throws IOException, ClassNotFoundException, InstantiationException, Exception {
    readText(f);
  } // end of constructor
   
  /**
   * Constructor
   * @param xmin Start time
   * @param xmax End time
   */
  public TextGrid(double xmin, double xmax) {
    setXmin(xmin);
    setXmax(xmax);
  } // end of constructor
   
   
  /**
   * Adds a tier object.
   * @param tier The tier to add.
   */
  public void addTier(Tier tier) {
    // (specify index to make sure they come out in the order added)
    vTiers.add(vTiers.size(), tier);
    if (tier.getXmin() < getXmin()) setXmin(tier.getXmin());
    if (tier.getXmax() > getXmax()) setXmax(tier.getXmax());
  } // end of addTier()
   
  /**
   * Retrieves a tier by name.
   * @param sName The name of the tier to find.
   * @return The first tier found with the given name, or null if no tier with the given name is found.
   */
  public Tier findTier(String sName) {
    if (sName == null) return null;
    for (Tier tier : vTiers) {
      if (tier.getName() != null) {
        if (tier.getName().equals(sName)) return tier;
      } else { // null name      
        // both names are null
        if (sName == null) return tier;
      }
    } // next tier
      
      // tier not found
    return null;
  } // end of findTier()
   
  /**
   * Pads all {@link IntervalTier}s to the time represented by {@link #getXmax()}.
   */
  public void padIntervalTiersToXmax() {
    for (Tier tier : vTiers) {
      if (tier instanceof IntervalTier) {
        ((IntervalTier)tier).padTier(getXmax());
      }
    } // next tier
  } // end of padIntervalTiersToXmax()
   
  /**
   * Read text-file representation of the object.
   * @param f The file to read.
   * @throws IOException If an IO error occurs
   * @throws ClassNotFoundException If an unknown object is encountered.
   * @throws InstantiationException If an object can't be instantiated.
   * @throws IllegalAccessException If an object has a private constructor.
   * @throws SerializationException If any other error occurs.
   */
  public void readText(File f)
    throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, SerializationException {
    BufferedReader reader = new BufferedReader(
      new InputStreamReader(new FileInputStream(f), "ISO-8859-1")); // TODO handle UTF-8 and UTF-16
    try {
      readText(reader);
    } finally {
      reader.close();
    }
  }
   
  /**
   * Write text-file representation of the object.
   * @param f The file to serialize to.
   * @throws IOException If an IO error occurs.
   */
  public void writeText(File f) throws IOException {
    FileWriter writer = new FileWriter(f);
    writeText(writer);
    writer.close();
  }
   
  // ITextEntity methods
   
  /**
   * Text-file representation of the object.
   * @param writer The write to serialize to.
   */
  public void writeText(Writer writer) throws IOException {
    writer.write( 
      "File type = \"ooTextFile\"" +
      "\nObject class = \"TextGrid\"" +
      "\n" +
      "\nxmin = " + OffsetFormat.format(getXmin()) + " " +
      "\nxmax = " + OffsetFormat.format(getXmax()) + " "); 
      
    if (vTiers.size() > 0) {
      writer.write("\ntiers? <exists> ");
      writer.write("\nsize = " + vTiers.size() + " ");
      writer.write("\nitem []: ");
      for (int i = 0; i < vTiers.size(); i++) {
        Tier tier = vTiers.elementAt(i);
        writer.write("\n    item [" + (i+1) + "]:"); // array is 1-based
        tier.writeText(writer);
      } // next tier
    }
  } // writeText

  /**
   * Read text-file representation of the object.
   * @param reader The reader to deserialize from.
   * @throws IOException If an IO error occurs
   * @throws ClassNotFoundException If an unknown object is encountered.
   * @throws InstantiationException If an object can't be instantiated.
   * @throws IllegalAccessException If an object has a private constructor.
   * @throws SerializationException If any other error occurs.
   */
  public void readText(BufferedReader reader)
    throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, SerializationException {
    String sFileType = readValue("File type", reader);
    if (sFileType == null)  throw new SerializationException("TextGrid File type not specified.");
    if (!sFileType.equalsIgnoreCase("ooTextFile")) throw new SerializationException("File type not supported: " + sFileType);
    setXmin(Double.parseDouble(readValue("xmin", reader)));
    setXmax(Double.parseDouble(readValue("xmax", reader)));
      
    // find tiers
    int iTierCount = Integer.parseInt(readValue("size", reader));
    for (int i = 0; i < iTierCount; i++) {
      String sClass = readValue("class", reader);
      // assume it's a class in our package
      try {
        Tier tier = (Tier) Class.forName("nzilbb.formatter.praat."+ sClass).getConstructor().newInstance();
	    
        tier.readText(reader);
        addTier(tier);
      }
      catch(NoSuchMethodException exception) {
      }
      catch(java.lang.reflect.InvocationTargetException exception) {
      }
    }
      
  } // readText
   
  /**
   * Utility method for reading ahead until the key is encountered, 
   * and then returning the value.
   * @param sKey Key to search for - can be a regulare expression like "((time)|(number))"
   * @param reader Objectto read from
   * @return the value of the target key
   * @throws IOException If an IO error occurs
   */
  public static String readValue(String sKey, BufferedReader reader) throws IOException {
    String sKeyPattern = "^\\s*" + sKey + " *= *.*$";
    // read until the key is encountered
    // (deleting nulls due to Praat encoding its files funny)
    String sLine = reader.readLine();
    while (sLine != null && !sLine.matches(sKeyPattern)) {
      sLine = reader.readLine();
    }
    if (sLine == null) return null;
      
    // extract the value
    int iEquals = sLine.indexOf('=');
    if (iEquals < 0) return null;
    String sValue = sLine.substring(iEquals + 1).trim();
      
    if (sValue.startsWith("\"") && 
        (sValue.length() == 1 || !sValue.endsWith("\""))) { // quoted string - read until whole string is captured
      sValue += reader.readLine();
      while (!sValue.trim().endsWith("\"")) {
        sValue += "\n" + reader.readLine();
      }// next line of text
    }
    sValue = sValue.trim(); // drop any trailing space
    if (sValue.startsWith("\"") && sValue.endsWith("\"") 
        && sValue.length() > 1) {
      // strip off the quotes
      sValue = sValue.substring(1, sValue.length() - 1);
    }
    return sValue;
  } // end of readValue()
   
  /**
   * Utility method for extracting the local class name of a given object (i.e. the last word of the fully qualified class name).
   * @param obj The object to name the class of.
   * @return all text after the last '.' in the classname of the object
   */
  public static String className(Object obj) {
    return obj.getClass().getSimpleName();
  } // end of className()
   
  /**
   * Resets the object to a clean slate
   */
  public void reset() {
    dXmin = 0.0;
    dXmax = 0.0;
    vTiers = new Vector<Tier>();
  } // end of reset()
   
} // end of class TextGrid
