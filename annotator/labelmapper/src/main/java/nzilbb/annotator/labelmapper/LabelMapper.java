//
// Copyright 2020-2021 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.annotator.labelmapper;

import java.util.List;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.encoding.comparator.*;
import nzilbb.editpath.*;

/**
 * This annotator creates a mapping between pairs of layers, by finding the minimum edit
 * path between them. 
 * @author Robert Fromont robert@fromont.net.nz
 */
public class LabelMapper extends Annotator {

  /** Get the minimum version of the nzilbb.ag API supported by the serializer.*/
  public String getMinimumApiVersion() { return "1.0.5"; }

  /**
   * Layer ID for the source of labels.
   * @see #getLabelLayerId()
   * @see #setLabelLayerId(String)
   */
  protected String labelLayerId;
  /**
   * Getter for {@link #labelLayerId}: Layer ID for the source of labels.
   * @return Layer ID for the source of labels.
   */
  public String getLabelLayerId() { return labelLayerId; }
  /**
   * Setter for {@link #labelLayerId}: Layer ID for the source of labels.
   * @param newLabelLayerId Layer ID for the source of labels.
   */
  public LabelMapper setLabelLayerId(String newLabelLayerId) { labelLayerId = newLabelLayerId; return this; }

  /**
   * Layer ID for the output layer.
   * @see #getMappingLayerId()
   * @see #setMappingLayerId(String)
   */
  protected String mappingLayerId;
  /**
   * Getter for {@link #mappingLayerId}: Layer ID for the output layer.
   * @return Layer ID for the output layer.
   */
  public String getMappingLayerId() { return mappingLayerId; }
  /**
   * Setter for {@link #mappingLayerId}: Layer ID for the output layer.
   * @param newMappingLayerId Layer ID for the output layer.
   */
  public LabelMapper setMappingLayerId(String newMappingLayerId) { mappingLayerId = newMappingLayerId; return this; }

  /**
   * Layer ID of tokens to tag.
   * @see #getTokenLayerId()
   * @see #setTokenLayerId(String)
   */
  protected String tokenLayerId;
  /**
   * Getter for {@link #tokenLayerId}: Layer ID of tokens to tag.
   * @return Layer ID of tokens to tag.
   */
  public String getTokenLayerId() { return tokenLayerId; }
  /**
   * Setter for {@link #tokenLayerId}: Layer ID of tokens to tag.
   * @param newTokenLayerId Layer ID of tokens to tag.
   */
  public LabelMapper setTokenLayerId(String newTokenLayerId) { tokenLayerId = newTokenLayerId; return this; }

  /**
   * How to compare the label and token layers. Valid options are "CharacterToCharacter",
   * "OrthographyToDISC", "OrthographyToArpabet", or "DISCToDISC". 
   * @see #getComparator()
   * @see #setComparator(String)
   */
  protected String comparator;
  /**
   * Getter for {@link #comparator}: How to compare the label and token layers. Valid
   * options are "CharacterToCharacter", "OrthographyToDISC", "OrthographyToArpabet", or
   * "DISCToDISC". 
   * @return How to compare the label and token layers. Valid options are
   * "CharacterToCharacter", "OrthographyToDISC", "OrthographyToArpabet", or "DISCToDISC".
   */
  public String getComparator() { return comparator; }
  /**
   * Setter for {@link #comparator}: How to compare the label and token layers. Valid options 
   * are "CharacterToCharacter", "OrthographyToDISC", "OrthographyToArpabet", or "DISCToDISC". 
   * @param newComparator How to compare the label and token layers. Valid options are 
   * "CharacterToCharacter", "OrthographyToDISC", "OrthographyToArpabet", or "DISCToDISC".
   */
  public LabelMapper setComparator(String newComparator) { comparator = newComparator; return this; }
   
  /**
   * Sets the configuration for a given annotation task.
   * @param parameters The configuration of the annotator; a value of <tt> null </tt>
   * will apply the default task parameters, with {@link #sourceLayerId} set to the
   * {@link Schema#wordLayerId} and {@link #stemLayerId} set to <q>stem</q>.
   * @throws InvalidConfigurationException
   */
  public void setTaskParameters(String parameters) throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");
      
    if (parameters == null) { // there is no possible default parameter
      throw new InvalidConfigurationException(this, "Parameters not set.");         
    }

    // set basic attributes
    beanPropertiesFromQueryString(parameters);

    // validate parameters
    if (labelLayerId == null || labelLayerId.length() == 0)
      throw new InvalidConfigurationException(this, "No label layer set.");
    Layer labelLayer = schema.getLayer(labelLayerId);
    if (labelLayer == null) 
      throw new InvalidConfigurationException(this, "Invalid label layer: " + labelLayerId);
    if (tokenLayerId == null || tokenLayerId.length() == 0)
      throw new InvalidConfigurationException(this, "No token layer set.");
    Layer tokenLayer = schema.getLayer(tokenLayerId);
    if (tokenLayer == null) 
      throw new InvalidConfigurationException(this, "Invalid token layer: " + tokenLayerId);
    if (comparator == null || comparator.length() == 0)
      throw new InvalidConfigurationException(this, "Comparator not set.");
    if (mappingLayerId == null || mappingLayerId.length() == 0)
      throw new InvalidConfigurationException(this, "No mapping layer set.");
    // layers must all be distinct
    HashSet<String> layerSet = new HashSet<String>();
    layerSet.add(labelLayerId);
    layerSet.add(tokenLayerId);
    layerSet.add(mappingLayerId);
    if (layerSet.size() != 3) {
      throw new InvalidConfigurationException(
        this, "Label ("+labelLayerId+"), mapping ("+mappingLayerId
        +"), and token ("+tokenLayerId+") layers must all be distinct.");
    }

    // does the mapping layer need to be added to the schema?
    Layer mappingLayer = schema.getLayer(mappingLayerId);
    if (mappingLayer == null) {
      String mappingParentId = tokenLayerId;
      if (tokenLayer.getAlignment() == Constants.ALIGNMENT_NONE) {
        // token layer it itself a tag, so we will tag its parent too
        mappingParentId = tokenLayer.getParentId();
      }
      // tag layer
      schema.addLayer(
        new Layer(mappingLayerId)
        .setAlignment(Constants.ALIGNMENT_NONE)
        .setPeers(false)
        .setParentId(mappingParentId)
        .setType(labelLayer.getType()));
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
    if (labelLayerId == null)
      throw new InvalidConfigurationException(this, "No label layer set.");
    if (tokenLayerId == null)
      throw new InvalidConfigurationException(this, "No token layer set.");
    HashSet<String> requiredLayers = new HashSet<String>();
    requiredLayers.add(labelLayerId);
    requiredLayers.add(tokenLayerId);
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
    if (mappingLayerId == null)
      throw new InvalidConfigurationException(this, "Mapping layer not set.");
    return new String[] { mappingLayerId };
  }

  class LabelElement {
    Annotation source;
    String label;
    public LabelElement(Annotation s, String l) {
      source = s;
      label = l;
    }
    public LabelElement(Annotation s) {
      source = s;
      label = s.getLabel();
    }
    public LabelElement(String l) {
      label = l;
    }
    public LabelElement(char l) {
      label = ""+l;
    }
    public String toString() { // must return just the label, as this is used by comparators
      return label;
    }    
  }
  
  /**
   * Transforms the graph. In this case, the graph is simply summarized, by counting all
   * tokens of each word type, and printing out the result to stdout.
   * @param graph The graph to transform.
   * @return The changes introduced by the tranformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph transform(Graph graph) throws TransformationException {
    setRunning(true);      
    Layer labelLayer = graph.getSchema().getLayer(labelLayerId);
    if (labelLayer == null) {
      throw new InvalidConfigurationException(
        this, "Invalid label layer: " + labelLayer);
    }
    Layer tokenLayer = graph.getSchema().getLayer(tokenLayerId);
    if (tokenLayer == null) {
      throw new InvalidConfigurationException(
        this, "Invalid token layer: " + tokenLayer);
    }
    Layer mappingLayer = graph.getSchema().getLayer(mappingLayerId);
    if (mappingLayer == null) {
      throw new InvalidConfigurationException(
        this, "Invalid output mapping layer: " + mappingLayerId);
    }
    
    if (tokenLayer.getParentId().equals(labelLayer.getParentId())) { // map from same-scope layer TODO
      throw new TransformationException(this, "Same-scope mapping is not yet implemented.");
    } else { // map from parent-scope layer
      // create a comparator for the mapping
      EditComparator<LabelElement> comparator = null;
      if (getComparator().equals("DISCToDISC")) {
        comparator = new DISC2DISCComparator<LabelElement>();
      } else if (getComparator().equals("OrthographyToDISC")) {
        comparator = new Orthography2DISCComparator<LabelElement>();
      } else if (getComparator().equals("OrthographyToArpabet")) {
        comparator = new Orthography2ARPAbetComparator<LabelElement>();
      } else {
        comparator = new Char2CharComparator<LabelElement>();
      }
      MinimumEditPath<LabelElement> mp = new MinimumEditPath<LabelElement>(comparator);

      // for each word
      for (Annotation word : graph.list(schema.getWordLayerId())) { // TODO chunk layer
        // create source element list
        Annotation[] tokens = word.all(tokenLayerId);
        if (tokens.length == 0) continue;
        Vector<LabelElement> vTokens = new Vector<LabelElement>();
        for (Annotation s : tokens) vTokens.add(new LabelElement(s));
	    
        // create destination element list
        Annotation labels = word.first(labelLayerId);
        if (labels == null) continue;
        Vector<LabelElement> vLabels = new Vector<LabelElement>();
        for (char c : labels.getLabel().toCharArray()) vLabels.add(new LabelElement(c));
        
        // setStatus("word: " + word.getLabel() + " - " + labels.getLabel());
        
        // find the minimum path between them
        List<EditStep<LabelElement>> path = mp.minimumEditPath(vLabels, vTokens);
        
        // create tags on the source layer from the path
        Annotation lastTag = null;
        String initialInserts = "";
        for (EditStep<LabelElement> step : path) {
          switch(step.getOperation()) {
            case NONE:
              lastTag = step.getTo().source.createTag(
                mappingLayerId, step.getFrom().label);
              if (initialInserts.length() > 0) { // prepend inserts we had already found
                lastTag.setLabel(initialInserts + lastTag.getLabel());
                initialInserts = "";
              }
              break;
            case CHANGE:
              lastTag = step.getTo().source.createTag(
                mappingLayerId, step.getFrom().label);
              if (initialInserts.length() > 0) { // prepend inserts we had already found
                lastTag.setLabel(initialInserts + lastTag.getLabel());
                initialInserts = "";
              }
              break;
            case DELETE:
              // append to the previous one
              if (lastTag != null) {
                lastTag.setLabel(lastTag.getLabel() + step.getFrom().label);
              } else { // remember the label until we can prepend it to something
                initialInserts += step.getFrom().label;
              }
              break;
              // do nothing
            case INSERT:
              break;
          }
        } // next step
        // for (Annotation s : tokens)
        // {
        //    setStatus(s.getLabel() + ":" + s.my(DestinationLayerId));
        // }
      } // next word
      
    } // map from parent-scope layer      

    setRunning(false);
    return graph;
  }   
   
} // end of class LabelMapper
