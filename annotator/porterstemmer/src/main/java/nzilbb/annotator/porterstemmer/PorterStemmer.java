//
// Copyright 2020-2026 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import javax.script.ScriptException;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.DictionaryException;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.LabelBasedTagger;

/**
 * Annotator that tags words with their stem according to the Porter stemming algorithm.
 * <p> For more information about the algorithm, see 
 * <em>Porter, 1980, An algorithm for suffix stripping, Program, Vol. 14, no. 3, pp 130-137,</em>
 * or <a href="http://www.tartarus.org/~martin/PorterStemmer">
 * http://www.tartarus.org/~martin/PorterStemmer</a>.
 */
public class PorterStemmer extends LabelBasedTagger {
  /** Get the minimum version of the nzilbb.ag API supported by the serializer.*/
  public String getMinimumApiVersion() { return "2.0.0"; }
   
  /** The porter stemmer */
  Stemmer stemmer = new Stemmer();
   
  /**
   * ID of the stem layer that the annotator outputs its
   * annotations to. 
   * @return ID of the stem layer that the annotator outputs its annotations to.
   */
  public String getStemLayerId() { return tagLayerId; }
  /**
   * ID of the stem layer that the annotator outputs its
   * annotations to. 
   * @param newStemLayerId ID of the stem layer that the annotator outputs its annotations to.
   */
  public PorterStemmer setStemLayerId(String newStemLayerId) {
    tagLayerId = newStemLayerId; return this; }
   
  /**
   * Sets the configuration for a given annotation task.
   * @param parameters The configuration of the annotator; a value of <tt> null </tt>
   * will apply the default task parameters, with {@link #tokenLayerId} set to the
   * {@link Schema#wordLayerId} and stemLayerId set to <q>stem</q>.
   * @throws InvalidConfigurationException
   */
  public void setTaskParameters(String parameters) throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");

    // target English
    targetLanguagePattern = "[Ee][Nn].*";
         
    if (parameters == null) { // apply default configuration
         
      if (schema.getLayer("orthography") != null) {
        tokenLayerId = "orthography";
      } else {
        tokenLayerId = schema.getWordLayerId();
      }
         
      Layer[] candidates = schema.getMatchingLayers(
        layer -> schema.getRoot().getId().equals(layer.getParentId())
        && layer.getAlignment() == 0 // transcript attribute
        && layer.getId().matches(".*lang.*")); // with 'lang' in the name
      if (candidates.length > 0) transcriptLanguageLayerId = candidates[0].getId();
         
      // default phrase language layer
      candidates = schema.getMatchingLayers(
        layer -> schema.getTurnLayerId() != null
        && schema.getTurnLayerId().equals(layer.getParentId()) // child of turn
        && layer.getId().matches(".*lang.*")); // with 'lang' in the name
      if (candidates.length > 0) phraseLanguageLayerId = candidates[0].getId();
      
      tagLayerId = "stem";
         
    } else {
      beanPropertiesFromQueryString(parameters);
    }
      
    // does the outputLayer need to be added to the schema?
    if (tagLayerId != null) {
      Layer layer = schema.getLayer(tagLayerId);
      if (layer == null) {
        schema.addLayer(
          new Layer(tagLayerId)
          .setAlignment(Constants.ALIGNMENT_NONE)
          .setPeers(false).setSaturated(true)
          .setParentId(schema.getWordLayerId()));
      } else {
        if (layer.getAlignment() != Constants.ALIGNMENT_NONE) {
          layer.setAlignment(Constants.ALIGNMENT_NONE);
        }
        if (layer.getPeers()) layer.setPeers(false);
        if (layer.getPeersOverlap()) layer.setPeersOverlap(false);
        if (!layer.getSaturated()) layer.setSaturated(true);
      }
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
    Vector<String> requiredLayers = new Vector<String>();
    requiredLayers.add(tokenLayerId);
    if (transcriptLanguageLayerId != null) requiredLayers.add(transcriptLanguageLayerId);
    if (phraseLanguageLayerId != null) requiredLayers.add(phraseLanguageLayerId);
    return requiredLayers.toArray(new String[0]);
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
    if (tagLayerId == null)
      throw new InvalidConfigurationException(this, "Stem layer not set.");
    return new String[] { tagLayerId };
  }
  
  /**
   * Determines what tag labels should apply on the tag layer for
   * tokens with the given label on the token layer.
   * This is the method that must be implemented by derived classes.
   * @param tokenLabel The label of the token(s) that must be tagged.
   * @return A list of tags, which may be empty.
   */
  public Collection<String> tagsFor(String tokenLabel) throws DictionaryException {
    Vector<String> tags = new Vector<String>();
    String s = tokenLabel.toLowerCase();
    // get stem of each part
    StringTokenizer tokens = new StringTokenizer(s, "'-", true);
    String stem = "";
    while (tokens.hasMoreTokens()) {
      String sPart = tokens.nextToken();
      for (int c = 0; c < sPart.length(); c++) stemmer.add(sPart.charAt(c));
      stemmer.stem();
      stem += stemmer.toString();
    } // next part
    tags.add(stem);
    return tags;
  } // end of tagsFor()
   
}
