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
package nzilbb.ag.automation.example.minimal;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.*;

/**
 * This is an example annotator which implements the minimal possible for a complete
 * annotator.
 */
public class MinimalExample extends Annotator {
   /** Get the minimum version of the nzilbb.ag API supported by the serializer.*/
   public String getMinimumApiVersion() { return "20200708.2018"; }

   /**
    * Sets the configuration for a given annotation task.
    * @param parameters The configuration of the annotator; This annotator has no task
    * parmeter web-app, so <var> parameters </var> will always be null.
    * @throws InvalidConfigurationException
    */
   public void setTaskParameters(String parameters) throws InvalidConfigurationException { }

   /**
    * Determines which layers the annotator requires in order to annotate a graph.
    * @return A list of layer IDs. In this case, the annotator only requires the schema's
    * word layer.
    * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
    * {@link #setSchema(Schema)} have not yet been called.
    */
   public String[] getRequiredLayers() throws InvalidConfigurationException {
      if (schema == null)
         throw new InvalidConfigurationException(this, "Schema is not set.");
      if (schema.getWordLayerId() == null)
         throw new InvalidConfigurationException(this, "Schema has no word layer.");
      String[] layers = { schema.getWordLayerId() };
      return layers;
   }

   /**
    * Determines which layers the annotator will create/update/delete annotations on.
    * @return A list of layer IDs. In this case, the annotator has no task web-app for
    * specifying an output layer, and doesn't update any layers, so this method returns an
    * empty array.
    * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
    * {@link #setSchema(Schema)} have not yet been called.
    */
   public String[] getOutputLayers() throws InvalidConfigurationException {
      return new String[0];
   }
   
   /**
    * Transforms the graph. In this case, the graph is simply summarized, by counting all
    * tokens of each word type, and printing out the result to stdout.
    * @param graph The graph to transform.
    * @return The changes introduced by the tranformation.
    * @throws TransformationException If the transformation cannot be completed.
    */
   public Graph transform(Graph graph) throws TransformationException {
      if (schema == null)
         throw new InvalidConfigurationException(this, "Schema is not set.");
      if (schema.getWordLayerId() == null)
         throw new InvalidConfigurationException(this, "Schema has no word layer.");

      // maintain a list of counts
      TreeMap<String,Integer> typesCounts = new TreeMap<String,Integer>();

      // count tokens of each type
      for (Annotation token : graph.list(schema.getWordLayerId())) {
         if (typesCounts.containsKey(token.getLabel())) {
            typesCounts.put(token.getLabel(), 1);
         } else {
            typesCounts.put(token.getLabel(), typesCounts.get(token.getLabel()) + 1);
         }
      } // next token

      // print out the results
      System.out.println(graph.getId());
      for (String type : typesCounts.keySet()) {
         System.out.println(type + "\t" + typesCounts.get(type) );
      } // next type
      
      return graph;
   }

}
