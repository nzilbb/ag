//
// Copyright 2026 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.annotator.celexen;

import java.util.Vector;

/**
 * Convenience class for retrieving fields from a line in a CELEX CD
 * file.  This is used when the CELEX layer manager is being
 * installed, to help import data into the database.
 * @author Robert Fromont
 */
@SuppressWarnings("serial")
public class CELEXLine extends Vector<String> {
  
  /**
   * Constructor.
   * @param sLine The parameter line from the file - something like:<br>
   * <pre>54659\miner\41\28481\1\P\'m2-n@R\[CVV][CVC]\[maI][n@r*]</pre>
   */
  public CELEXLine(String line) {
    String[] tokens = line.split("\\\\");
    
    for (int i = 0; i < java.lang.reflect.Array.getLength(tokens); i++) {
      String sField = tokens[i];
      add(sField);
    } // next field
  } // end of constructor
  
  /**
   * Get string stored in a given field location.
   * @param f Field number to retrieve - this is 1-based to
   *  match the CELEX README file documentation 
   * @return The value of the given field, or null if there's no
   *  such field
   */
  public String getString(int f) {
    try {
      return get(f-1).toString(); // index from 1 to match readme files
    } catch (Exception e) {
      return null;
    }
  } // end of getString()
      
  /**
   * Get an integer stored in a given field location.
   * @param f Field number to retrieve - this is 1-based to
   *  match the CELEX README file documentation 
   * @return The value of the given field, or -1 if there's no
   *  such field
   */
  public long getInt(int f) {
    try {
	    return Long.parseLong(getString(f));
    } catch (Exception e) {
      return -1;
    }
  } // end of getInt()
  
} // end of class CELEXLine
