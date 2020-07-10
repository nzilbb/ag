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
package nzilbb.annotator.porterstemmer;

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;

/**
 * Annotator that tags words with their stem according to the Porter stemming algorithm.
 * <p> For more information about the algorithm, see 
 * <em>Porter, 1980, An algorithm for suffix stripping, Program, Vol. 14, no. 3, pp 130-137,</em>
 * or <a href="http://www.tartarus.org/~martin/PorterStemmer">
 * http://www.tartarus.org/~martin/PorterStemmer</a>.
 */
public class PorterStemmer extends Annotator {
   /** Get the minimum version of the nzilbb.ag API supported by the serializer.*/
   public String getMinimumApiVersion() { return "20200708.2018"; }
   
   /** The porter stemmer */
   Stemmer stemmer = new Stemmer();
   
   /**
    * ID of the input layer containing word tokens.
    * @see #getTokenLayerId()
    * @see #setTokenLayerId(String)
    */
   protected String tokenLayerId;
   /**
    * Getter for {@link #tokenLayerId}: ID of the input layer containing word tokens.
    * @return ID of the input layer containing word tokens.
    */
   public String getTokenLayerId() { return tokenLayerId; }
   /**
    * Setter for {@link #tokenLayerId}: ID of the input layer containing word tokens.
    * @param newTokenLayerId ID of the input layer containing word tokens.
    */
   public PorterStemmer setTokenLayerId(String newTokenLayerId) { tokenLayerId = newTokenLayerId; return this; }

   /**
    * ID of the stem layer that the annotator outputs its annotations to.
    * @see #getStemLayerId()
    * @see #setStemLayerId(String)
    */
   protected String stemLayerId;
   /**
    * Getter for {@link #stemLayerId}: ID of the stem layer that the annotator outputs its annotations to.
    * @return ID of the stem layer that the annotator outputs its annotations to.
    */
   public String getStemLayerId() { return stemLayerId; }
   /**
    * Setter for {@link #stemLayerId}: ID of the stem layer that the annotator outputs its annotations to.
    * @param newStemLayerId ID of the stem layer that the annotator outputs its annotations to.
    */
   public PorterStemmer setStemLayerId(String newStemLayerId) { stemLayerId = newStemLayerId; return this; }
   
   /**
    * Sets the configuration for a given annotation task.
    * @param parameters The configuration of the annotator; a value of <tt> null </tt>
    * will apply the default task parameters, with {@link #tokenLayerId} set to the
    * {@link Schema#wordLayerId} and {@link #stemLayerId} set to <q>stem</q>.
    * @throws InvalidConfigurationException
    */
   public void setTaskParameters(String parameters) throws InvalidConfigurationException {
      if (schema == null)
         throw new InvalidConfigurationException(this, "Schema is not set.");

      if (parameters == null) { // apply default configuration
         tokenLayerId = schema.getWordLayerId();
         stemLayerId = "stem";
      } else {
         beanPropertiesFromQueryString(parameters);
      }
      
      // does the outputLayer need to be added to the schema?
      if (schema.getLayer(stemLayerId) == null) {
         schema.addLayer(
            new Layer(stemLayerId)
            .setAlignment(Constants.ALIGNMENT_NONE)
            .setPeers(false)
            .setParentId(schema.getWordLayerId()));
      }
   }

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
      if (tokenLayerId == null)
         throw new InvalidConfigurationException(this, "No input token layer set.");
      return new String[] { tokenLayerId };
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
      if (stemLayerId == null)
         throw new InvalidConfigurationException(this, "Stem layer not set.");
      return new String[] { stemLayerId };
   }
   
   /**
    * Transforms the graph. In this case, the graph is simply summarized, by counting all
    * tokens of each word type, and printing out the result to stdout.
    * @param graph The graph to transform.
    * @return The changes introduced by the tranformation.
    * @throws TransformationException If the transformation cannot be completed.
    */
   public List<Change> transform(Graph graph) throws TransformationException { // TODO avoid tagging non-english
      Layer tokenLayer = graph.getSchema().getLayer(tokenLayerId);
      if (tokenLayer == null) {
         throw new InvalidConfigurationException(
            this, "Invalid input token layer: " + tokenLayerId);
      }
      Layer stemLayer = graph.getSchema().getLayer(stemLayerId);
      if (stemLayer == null) {
         throw new InvalidConfigurationException(
            this, "Invalid output stem layer: " + stemLayerId);
      }

      // tag only tokens that are not already tagged
      for (Annotation token : graph.list(tokenLayerId)) {
         if (token.my(stemLayerId) == null) { // not tagged yet
            token.createTag(stemLayerId, stem(token.getLabel()))
               .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
         } // not tagged yet
      } // next token
      
      return new Vector<Change>();
   }

   /**
    * Stems the given word.
    * @param s The word to stem.
    * @return The stem of the given word.
    */
   public String stem(String s) {
      s = s.toLowerCase();
      // get stem of each part
      StringTokenizer tokens = new StringTokenizer(s, "'-", true);
      String stem = "";
      while (tokens.hasMoreTokens()) {
	 String sPart = tokens.nextToken();
	 for (int c = 0; c < sPart.length(); c++) stemmer.add(sPart.charAt(c));
	 stemmer.stem();
	 stem += stemmer.toString();
      } // next part
      return stem;
   } // end of stem()
   
}
