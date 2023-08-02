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

/**
 * Interface for objects that are saved to / loaded from text files.
 * @author Robert Fromont
 */
public interface ITextEntity {
  /**
   * Write the text-file representation of the object.
   * @param writer The writer to write to.
   * @throws java.io.IOException If an IO error occurs.
   */
  public void writeText(Writer writer) throws java.io.IOException;
   
  /**
   * Read the text-file representation of the object.
   * @param reader The reader to read from.
   * @throws Exception If an error occurs.
   */
  public void readText(BufferedReader reader) throws Exception;
   
} // end of interface ITextEntity
