//
// Copyright 2020-2022 New Zealand Institute of Language, Brain and Behaviour, 
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
import nzilbb.ag.GraphStore;
import nzilbb.ag.GraphTransformer;
import nzilbb.ag.StoreException;
import nzilbb.ag.TransformationException;

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
   * @param id The indentifier of the desired dictionary. This can be null if the
   * Annotator supports only one dictionary, or the Annotator has already been configured
   * (e.g. via {@link Annotator#setTaskParameters(String)} sufficiently to know which
   * dictionary to return.
   * @return The identified dictionary.
   * @throws DictionaryException If the given dictionary doesn't exist.
   */
  public Dictionary getDictionary(String id) throws DictionaryException;
  
  /**
   * Tags all instances of the given word in the given graph store, using the dictionary
   * specified by current task configuration (i.e. the dictionary returned by
   * <code>getDictionary(null)</code>).
   * <p> The default implementation throws TransformationException
   * @param store
   * @param sourceLabel
   * @return The number of tags created.
   * @throws DictionaryException, TransformationException, InvalidConfigurationException,
   * StoreException 
   */
  default public int tagAllInstances(GraphStore store, String sourceLabel)
    throws DictionaryException, TransformationException, InvalidConfigurationException,
    StoreException {
    throw new TransformationException((GraphTransformer)this, "Not implemented");
  }
   
} // end of class ImplementsDictionaries
