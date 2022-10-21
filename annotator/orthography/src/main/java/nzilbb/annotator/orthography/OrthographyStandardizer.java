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
package nzilbb.annotator.orthography;

import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;

/**
 * Annotator that 'cleans up' word labels, ensuring they're all lowercase and with
 * extraneous punctuation is removed.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class OrthographyStandardizer extends Annotator {
   
  /** Get the minimum version of the nzilbb.ag API supported by the serializer.*/
  public String getMinimumApiVersion() { return "1.0.0"; }
   
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
  public OrthographyStandardizer setTokenLayerId(String newTokenLayerId) {
    tokenLayerId = newTokenLayerId; return this; }
  
  /**
   * Convert tokens to lower-case.
   * @see #getLowerCase()
   * @see #setLowerCase(boolean)
   */
  protected boolean lowerCase = true;
  /**
   * Getter for {@link #lowerCase}: Convert tokens to lower-case.
   * @return Convert tokens to lower-case.
   */
  public boolean getLowerCase() { return lowerCase; }
  /**
   * Setter for {@link #lowerCase}: Convert tokens to lower-case.
   * @param newLowerCase Convert tokens to lower-case.
   */
  public OrthographyStandardizer setLowerCase(boolean newLowerCase) { lowerCase = newLowerCase; return this; }

  /**
   * Regular expression for identifying characters to remove. 
   * Default value is <q>[\\p{Punct}&amp;&amp;[^~\\-:']]</q>
   * - all punctuation except <q>~</q>, <q>-</q>, <q>'</q>, and <q>:</q>
   * @see #getRemovalPattern()
   * @see #setRemovalPattern(String)
   */
  protected String removalPattern = "[\\p{Punct}&&[^~\\-:']]";
  /**
   * Getter for {@link #removalPattern}: Regular expression for identifying characters to
   * remove. Default value is <q>[\\p{Punct}&amp;&amp;[^~\\-:']]</q>
   * - all punctuation except <q>~</q>, <q>-</q>, <q>'</q>, and <q>:</q>
   * @return Regular expression for identifying characters to remove.
   */
  public String getRemovalPattern() { return removalPattern; }
  /**
   * Setter for {@link #removalPattern}: Regular expression for identifying characters to remove.
   * @param newRemovalPattern Regular expression for identifying characters to remove.
   */
  public OrthographyStandardizer setRemovalPattern(String newRemovalPattern) { removalPattern = newRemovalPattern; return this; }

  /**
   * ID of the output layer containing standardized orthography layers.
   * @see #getOrthographyLayerId()
   * @see #setOrthographyLayerId(String)
   */
  protected String orthographyLayerId;
  /**
   * Getter for {@link #orthographyLayerId}: ID of the output layer containing standardized orthography layers.
   * @return ID of the output layer containing standardized orthography layers.
   */
  public String getOrthographyLayerId() { return orthographyLayerId; }
  /**
   * Setter for {@link #orthographyLayerId}: ID of the output layer containing standardized orthography layers.
   * @param newOrthographyLayerId ID of the output layer containing standardized orthography layers.
   */
  public OrthographyStandardizer setOrthographyLayerId(String newOrthographyLayerId) { orthographyLayerId = newOrthographyLayerId; return this; }
   
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
      // remove non-word characters (but leave '~', '-', ''', and ':' in)
      removalPattern = "[\\p{Punct}&&[^~\\-:']]";
      orthographyLayerId = "orthography";
         
    } else {
      beanPropertiesFromQueryString(parameters);

      // validate removalPattern
      try {
        Pattern.compile(removalPattern);
      } catch(PatternSyntaxException exception) {
        throw new InvalidConfigurationException(
          this, "Removal pattern is not a valid regular expression: "
          + exception.getMessage());
      }
    }
      
    // does the outputLayer need to be added to the schema?
    if (schema.getLayer(orthographyLayerId) == null) {
      schema.addLayer(
        new Layer(orthographyLayerId)
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
    Vector<String> requiredLayers = new Vector<String>();
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
    if (orthographyLayerId == null)
      throw new InvalidConfigurationException(this, "Orthography layer not set.");
    return new String[] { orthographyLayerId };
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
      
    Layer tokenLayer = graph.getSchema().getLayer(tokenLayerId);
    if (tokenLayer == null) {
      throw new InvalidConfigurationException(
        this, "Invalid input token layer: " + tokenLayerId);
    }
    Layer orthographyLayer = graph.getSchema().getLayer(orthographyLayerId);
    if (orthographyLayer == null) {
      throw new InvalidConfigurationException(
        this, "Invalid output orthography layer: " + orthographyLayerId);
    }

    for (Annotation token : graph.all(tokenLayerId)) {
      // tag only tokens that are not already tagged
      if (token.first(orthographyLayerId) == null) { // not tagged yet
        String orthography = orthography(token.getLabel(), removalPattern);
        // only add an annotation if there's actually a label
        if (orthography.length() > 0) {
          token.createTag(orthographyLayerId, orthography)
            .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
        }
      } // not tagged yet
    } // next token

    setRunning(false);
    return graph;
  }
   
  /**
   * Returns the orthography of the given string, using the given removal pattern.
   * @param word
   * @param removalPattern
   * @return The orthography of the given string.
   */
  public String orthography(String word, String removalPattern) {
    String orth = word;
    if (lowerCase) orth = orth.toLowerCase();
    orth = orth
      // collapse all space (there could be space because of appended non-words)
      .replaceAll("\\s","")
      // might be spaces left after stripping
      .trim()
      // TODO would be nice to have a configurable lise of conversions like ’->'
      // 'smart' apostrophes to normal ones
      .replace('’','\'')
      // 'smart' quotes to normal ones
      .replace('“','"').replace('”','"')
      // 'em-dash' to hyphen
      .replace('—','-')
      // remove leading hyphens/apostrophes
      .replaceAll("^[\\-']*", "");
    if (removalPattern.length() > 0) {
      // remove characters identified by removalPattern
      orth = orth.replaceAll(removalPattern,"");
    }
    orth = orth
      // might be spaces left after stripping
      .trim()
      // remove trailing hyphens/apostrophes
      .replaceAll("[\\-']*$", "")
      // remove leading hyphens/apostrophes
      .replaceAll("^[\\-']*", "")
      // might be spaces left after stripping
      .trim();
    return orth;
  } // end of orthography()
   
} // end of class OrthographyStandardizer
