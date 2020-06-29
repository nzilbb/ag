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
package nzilbb.ag;

import nzilbb.util.MonitorableTask;

/**
 * Interface an automated annotation module must implement.
 * @author Robert Fromont robert@fromont.net.nz
 */

public interface IAnnotator extends IGraphTransformer, MonitorableTask {

   /**
    * Unique name for the annotator.
    * @return The annotator's ID.
    */
   public String getId();

   /**
    * Version of this implementation.
    * @return Annotator version.
    */
   public String version();
   
   /**
    * Sets the layer schema.
    * @param schema The layer schema.
    * @return A reference to this object.
    */
   public IAnnotator setSchema(Schema schema);
   
   /**
    * Sets the configuration for a given annotation task.
    * @param configuration The configuration of the annotator.
    * @return A reference to this object.
    * @throws InvalidConfigurationException
    */
   public IAnnotator setTaskConfiguration(String configuration) throws InvalidConfigurationException;
   
   /**
    * Determines which layers the annotator requires in order to annotate a graph.
    * @return A list of layer IDs.
    * @throws InvalidConfigurationException If {@link #setConfiguration(String)} has not
    * yet been called.
    */
   public String[] getRequiredLayers() throws InvalidConfigurationException;

   /**
    * Determines which layers the annotator will create/update/delete annotations on.
    * @return A list of layer IDs.
    * @throws InvalidConfigurationException If {@link #setConfiguration(String)} has not
    * yet been called.
    */
   public String[] getOutputLayers() throws InvalidConfigurationException;
} // end of class IAnnotator
