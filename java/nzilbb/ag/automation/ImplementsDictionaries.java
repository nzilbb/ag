//
// Copyright 2020 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag.automation;

import java.util.List;

/**
 * Extend this interface if your {@link Annotator} publishes {@link Dictionary} implementations.
 * @author Robert Fromont robert@fromont.net.nz
 */
public interface ImplementsDictionaries {
   
   /**
    * Lists the dictionaries implemented by this Annotator.
    * <p> This method can assume that the following methods have been previously called:
    * <ul>
    *  <li> {@link Annotator#setSchema(Schema)} </li>
    *  <li> {@link Annotator#setTaskParameters(String)} </li>
    *  <li> {@link Annotator#setWorkingDirectory(File)} (if applicable) </li>
    *  <li> {@link Annotator#setRdbConnectionFactory(ConnectionFactory)}
    *       (if applicable) </li>
    * </ul>
    * @return A (possibly empty) list of IDs of dictionaries.
    */
   public List<String> getDictionaryIds();

   /**
    * Gets the identified dictionary.
    * <p> This method can assume that the following methods have been previously called:
    * <ul>
    *  <li> {@link Annotator#setSchema(Schema)} </li>
    *  <li> {@link Annotator#setTaskParameters(String)} </li>
    *  <li> {@link Annotator#setWorkingDirectory(File)} (if applicable) </li>
    *  <li> {@link Annotator#setRdbConnectionFactory(ConnectionFactory)}
    *       (if applicable) </li>
    * </ul>
    * @return The identified dictionary.
    * @throws DictionaryException If the given dictionary doesn't exist.
    */
   public Dictionary getDictionary(String id) throws DictionaryException;
   
} // end of class UsesFileSystem
