//
// Copyright 2015-2025 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.function.Predicate;
import javax.script.*;
import nzilbb.ag.util.LayerHierarchyTraversal;
import nzilbb.util.CloneableBean;
import nzilbb.util.ClonedProperty;

/**
 * Definition of layers and their interrelations.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class Schema implements Cloneable, CloneableBean {

  // Attributes:

  /**
   * The root of the layer hierarchy, representing the graph as a whole.
   * @see #getRoot()
   */
  protected final Layer root = new Layer("transcript", "The annotation graph as a whole", 2, false, false, true);
  /**
   * Getter for {@link #root}: The root of the layer hierarchy, representing the graph as a whole.
   * @return The root of the layer hierarchy, representing the graph as a whole.
   */
  @ClonedProperty
  public Layer getRoot() { return root; }
   
  /**
   * Layers, indexed by ID.
   * @see #getLayers()
   * @see #setLayers(LinkedHashMap)
   */
  protected LinkedHashMap<String,Layer> layers = new LinkedHashMap<String,Layer>();
  /**
   * Getter for {@link #layers}: Layers, indexed by ID.
   * @return Layers, indexed by ID.
   */
  @ClonedProperty
  public LinkedHashMap<String,Layer> getLayers() { return layers; }
  /**
   * Setter for {@link #layers}: Layers, indexed by ID.
   * @param newLayers Layers, indexed by ID.
   */
  public Schema setLayers(LinkedHashMap<String,Layer> newLayers) {
    layers = newLayers; return this; }

  /**
   * An ordered map of category names to descriptions.
   * @see #getCategories()
   * @see #setCategories(LinkedHashMap<String,String>)
   */
  protected LinkedHashMap<String,String> categories = new LinkedHashMap<String,String>();
  /**
   * Getter for {@link #categories}: An ordered map of category names to descriptions.
   * @return An ordered map of category names to descriptions.
   */
  @ClonedProperty
  public LinkedHashMap<String,String> getCategories() { return categories; }
  /**
   * Setter for {@link #categories}: An ordered map of category names to descriptions.
   * @param newCategories An ordered map of category names to descriptions.
   */
  public Schema setCategories(LinkedHashMap<String,String> newCategories) { categories = newCategories; return this; }
   
  /**
   * ID of the layer that contains participants.
   * @see #getParticipantLayerId()
   * @see #setParticipantLayerId(String)
   */
  protected String participantLayerId;
  /**
   * Getter for {@link #participantLayerId}: ID of the layer that contains participants.
   * @return ID of the layer that contains participants.
   */
  @ClonedProperty
  public String getParticipantLayerId() { return participantLayerId; }
  /**
   * Setter for {@link #participantLayerId}: ID of the layer that contains participants.
   * @param newParticipantLayerId ID of the layer that contains participants.
   */
  public Schema setParticipantLayerId(String newParticipantLayerId) {
    participantLayerId = newParticipantLayerId; return this; }

  /**
   * ID of the layer that contains speaker turns.
   * @see #getTurnLayerId()
   * @see #setTurnLayerId(String)
   */
  protected String turnLayerId;
  /**
   * Getter for {@link #turnLayerId}: ID of the layer that contains speaker turns.
   * @return ID of the layer that contains speaker turns.
   */
  @ClonedProperty
  public String getTurnLayerId() { return turnLayerId; }
  /**
   * Setter for {@link #turnLayerId}: ID of the layer that contains speaker turns.
   * @param newTurnLayerId ID of the layer that contains speaker turns.
   */
  public Schema setTurnLayerId(String newTurnLayerId) {
    turnLayerId = newTurnLayerId; return this; }

  /**
   * ID of the layer that contains speaker utterances.
   * @see #getUtteranceLayerId()
   * @see #setUtteranceLayerId(String)
   */
  protected String utteranceLayerId;
  /**
   * Getter for {@link #utteranceLayerId}: ID of the layer that contains speaker utterances.
   * @return ID of the layer that contains speaker utterances.
   */
  @ClonedProperty
  public String getUtteranceLayerId() { return utteranceLayerId; }
  /**
   * Setter for {@link #utteranceLayerId}: ID of the layer that contains speaker utterances.
   * @param newUtteranceLayerId ID of the layer that contains speaker utterances.
   */
  public Schema setUtteranceLayerId(String newUtteranceLayerId) {
    utteranceLayerId = newUtteranceLayerId; return this; }

  /**
   * ID of the layer that contains individual word tokens.
   * @see #getWordLayerId()
   * @see #setWordLayerId(String)
   */
  protected String wordLayerId;
  /**
   * Getter for {@link #wordLayerId}: ID of the layer that contains individual word tokens.
   * @return ID of the layer that contains individual word tokens.
   */
  @ClonedProperty
  public String getWordLayerId() { return wordLayerId; }
  /**
   * Setter for {@link #wordLayerId}: ID of the layer that contains individual word tokens.
   * @param newWordLayerId ID of the layer that contains individual word tokens.
   */
  public Schema setWordLayerId(String newWordLayerId) {
    wordLayerId = newWordLayerId; return this; }

  /**
   * ID of the layer that tags the graph with its episode name, if any.
   * @see #getEpisodeLayerId()
   * @see #setEpisodeLayerId(String)
   */
  protected String episodeLayerId;
  /**
   * Getter for {@link #episodeLayerId}: ID of the layer that tags the graph with its
   * episode name, if any. 
   * @return ID of the layer that tags the graph with its episode name, if any.
   */
  @ClonedProperty
  public String getEpisodeLayerId() { return episodeLayerId; }
  /**
   * Setter for {@link #episodeLayerId}: ID of the layer that tags the graph with its
   * episode name, if any. 
   * @param newEpisodeLayerId ID of the layer that tags the graph with its episode name, if any.
   */
  public Schema setEpisodeLayerId(String newEpisodeLayerId) {
    episodeLayerId = newEpisodeLayerId; return this; }

  /**
   * ID of the layer that tags the graph with its corpus name, if any.
   * @see #getCorpusLayerId()
   * @see #setCorpusLayerId(String)
   */
  protected String corpusLayerId;
  /**
   * Getter for {@link #corpusLayerId}: ID of the layer that tags the graph with its
   * corpus name, if any. 
   * @return ID of the layer that tags the graph with its corpus name, if any.
   */
  @ClonedProperty
  public String getCorpusLayerId() { return corpusLayerId; }
  /**
   * Setter for {@link #corpusLayerId}: ID of the layer that tags the graph with its
   * corpus name, if any. 
   * @param newCorpusLayerId ID of the layer that tags the graph with its corpus name, if any.
   */
  public Schema setCorpusLayerId(String newCorpusLayerId) {
    corpusLayerId = newCorpusLayerId; return this; }
   
  private LinkedHashMap<String,Vector<Layer>> pendingParents
  = new LinkedHashMap<String,Vector<Layer>>();

  // Methods:
   
  /**
   * Default constructor.
   */
  public Schema() {
    addLayer(root);
  } // end of constructor

  /**
   * Constructor from array.
   * @param layers Array of layers.
   */
  public Schema(Layer[] layers) {
    addLayer(root);
    for (Layer layer : layers) {
      addLayer(layer);
    } // next layer
  } // end of constructor

  /**
   * Constructor from collection.
   * @param layers Collection of layers.
   */
  public Schema(Collection<Layer> layers) {
    addLayer(root);
    for (Layer layer : layers) {
      addLayer(layer);
    } // next layer
  } // end of constructor

  /**
   * Constructor from array and attributes.
   * @param layers Array of layers.
   * @param participantLayerId ID of the layer that contains participants.
   * @param turnLayerId ID of the layer that contains speaker turns.
   * @param utteranceLayerId ID of the layer that contains speaker utterances.
   * @param wordLayerId ID of the layer that contains individual word tokens.
   */
  public Schema(Layer[] layers, String participantLayerId, String turnLayerId,
                String utteranceLayerId, String wordLayerId) {
    addLayer(root);
    for (Layer layer : layers) {
      addLayer(layer);
    } // next layer
    setParticipantLayerId(participantLayerId);
    setTurnLayerId(turnLayerId);
    setUtteranceLayerId(utteranceLayerId);
    setWordLayerId(wordLayerId);
  } // end of constructor

  /**
   * Constructor from array and attributes.
   * @param layers Array of layers.
   * @param participantLayerId ID of the layer that contains participants.
   * @param turnLayerId ID of the layer that contains speaker turns.
   * @param utteranceLayerId ID of the layer that contains speaker utterances.
   * @param wordLayerId ID of the layer that contains individual word tokens.
   */
  public Schema(String participantLayerId, String turnLayerId, String utteranceLayerId,
                String wordLayerId, Layer... layers) {
    addLayer(root);
    for (Layer layer : layers) {
      addLayer(layer);
    } // next layer
    setParticipantLayerId(participantLayerId);
    setTurnLayerId(turnLayerId);
    setUtteranceLayerId(utteranceLayerId);
    setWordLayerId(wordLayerId);
  } // end of constructor

  /**
   * Constructor from collection and attributes.
   * @param layers Collection of layers.
   * @param participantLayerId ID of the layer that contains participants.
   * @param turnLayerId ID of the layer that contains speaker turns.
   * @param utteranceLayerId ID of the layer that contains speaker utterances.
   * @param wordLayerId ID of the layer that contains individual word tokens.
   */
  public Schema(Collection<Layer> layers, String participantLayerId, String turnLayerId,
                String utteranceLayerId, String wordLayerId) {
    addLayer(root);
    for (Layer layer : layers) {
      addLayer(layer);
    } // next layer
    setParticipantLayerId(participantLayerId);
    setTurnLayerId(turnLayerId);
    setUtteranceLayerId(utteranceLayerId);
    setWordLayerId(wordLayerId);
  } // end of constructor

  /**
   * Constructor from array and attributes.
   * @param layers Array of layers.
   * @param participantLayerId ID of the layer that contains participants.
   * @param turnLayerId ID of the layer that contains speaker turns.
   * @param utteranceLayerId ID of the layer that contains speaker utterances.
   * @param wordLayerId ID of the layer that contains individual word tokens.
   * @param episodeLayerId ID of the layer that tags the graph with its episode name, if any.
   * @param corpusLayerId ID of the layer that tags the graph with its corpus name, if any.
   */
  public Schema(Layer[] layers, String participantLayerId, String turnLayerId,
                String utteranceLayerId, String wordLayerId, String episodeLayerId,
                String corpusLayerId) {
    addLayer(root);
    for (Layer layer : layers) {
      addLayer(layer);
    } // next layer
    setParticipantLayerId(participantLayerId);
    setTurnLayerId(turnLayerId);
    setUtteranceLayerId(utteranceLayerId);
    setWordLayerId(wordLayerId);
    setEpisodeLayerId(episodeLayerId);
    setCorpusLayerId(corpusLayerId);
  } // end of constructor

  /**
   * Constructor from collection and attributes.
   * @param layers Collection of layers.
   * @param participantLayerId ID of the layer that contains participants.
   * @param turnLayerId ID of the layer that contains speaker turns.
   * @param utteranceLayerId ID of the layer that contains speaker utterances.
   * @param wordLayerId ID of the layer that contains individual word tokens.
   * @param episodeLayerId ID of the layer that tags the graph with its episode name, if any.
   * @param corpusLayerId ID of the layer that tags the graph with its corpus name, if any.
   */
  public Schema(Collection<Layer> layers, String participantLayerId, String turnLayerId,
                String utteranceLayerId, String wordLayerId, String episodeLayerId,
                String corpusLayerId) {
    addLayer(root);
    for (Layer layer : layers) {
      addLayer(layer);
    } // next layer
    setParticipantLayerId(participantLayerId);
    setTurnLayerId(turnLayerId);
    setUtteranceLayerId(utteranceLayerId);
    setWordLayerId(wordLayerId);
    setEpisodeLayerId(episodeLayerId);
    setCorpusLayerId(corpusLayerId);
  } // end of constructor
   
  /**
   * Adds a layer. 
   * <p>If the given layer was already in the schema, the original definition is not replaced.
   * @param layer The layer to add.
   * @return The layer in the schema - the given layer if it was not already in the
   * schema, or the original layer object, if it was already in the schema. 
   */
  public Layer addLayer(Layer layer) {
    if (layers.containsKey(layer.getId())) return getLayer(layer.getId());

    layers.put(layer.getId(), layer);

    if (layer.getParentId() == null && !layer.getId().equals(root.getId())) {
      layer.setParentId(root.getId());
    }
    // set their parent
    if (layer.getParentId() != null) {
      if (layers.containsKey(layer.getParentId())) {
        layer.setParent(layers.get(layer.getParentId()));
      } else {
        if (!pendingParents.containsKey(layer.getParentId())) {
          pendingParents.put(layer.getParentId(), new Vector<Layer>());
        }
        pendingParents.get(layer.getParentId()).add(layer);
      }
    }

    // check whether any child layers have already been added
    if (pendingParents.containsKey(layer.getId())) {
      for (Layer otherLayer : pendingParents.get(layer.getId())) {
        otherLayer.setParent(layer);
      }
      pendingParents.remove(layer.getId());
    }

    // add category if required
    if (layer.getCategory() != null && layer.getCategory().length() > 0) {
      if (!categories.containsKey(layer.getCategory())) {
        categories.put(layer.getCategory(), layer.getCategory());
      }
    }
    return layer;
  } // end of addLayer()
   
  /**
   * Gets the named layer.
   * @param id The ID of the desired layer.
   * @return The named layer, or null if it's not in the schema.
   */
  public Layer getLayer(String id) {
    return layers.get(id);
  } // end of getLayer()
   
  /**
   * Get the layer specified by {@link #episodeLayerId}
   * @return The layer specified by {@link #episodeLayerId}, or null if there is none.
   */
  public Layer getEpisodeLayer() {
    return getLayer(episodeLayerId);
  } // end of getEpisodeLayer()

  /**
   * Get the layer specified by {@link #participantLayerId}
   * @return The layer specified by {@link #participantLayerId}, or null if there is none.
   */
  public Layer getParticipantLayer() {
    return getLayer(participantLayerId);
  } // end of getParticipantLayer()

  /**
   * Get the layer specified by {@link #turnLayerId}
   * @return The layer specified by {@link #turnLayerId}, or null if there is none.
   */
  public Layer getTurnLayer() {
    return getLayer(turnLayerId);
  } // end of getTurnLayer()

  /**
   * Get the layer specified by {@link #utteranceLayerId}
   * @return The layer specified by {@link #utteranceLayerId}, or null if there is none.
   */
  public Layer getUtteranceLayer() {
    return getLayer(utteranceLayerId);
  } // end of getUtteranceLayer()

  /**
   * Get the layer specified by {@link #wordLayerId}
   * @return The layer specified by {@link #wordLayerId}, or null if there is none.
   */
  public Layer getWordLayer() {
    return getLayer(wordLayerId);
  } // end of getWordLayer()
   
  /**
   * Return the layers as an array.
   * @return The layers as an array.
   */
  public Layer[] layers() {
    return layers.values().toArray(new Layer[0]);
  } // end of layers()
   
  /**
   * Returns layers that match the given expression.
   * <p> The expression is evaluated as a JavaScript boolean expressin and can assume
   * that <var> layer </var> is a reference to a candidate layer and <var> schema </var>
   * is a reference to this schema.
   * <p> e.g. <q>layer.parentId == schema.participantLayerId &amp;&amp; layer.alignment == 0</q>
   * should match all un-aligned children of the participant layer; i.e. participant attributes.
   * @param expression A Javascript boolean expression to use to filter layers.
   * @return A list of matching layers, which may be empty.
   */
  public Layer[] getMatchingLayers(String expression) throws ScriptException {
    Vector<Layer> layers = new Vector<Layer>();
    ScriptEngineManager manager = new ScriptEngineManager();
    ScriptEngine engine = manager.getEngineByMimeType("application/javascript");
    //ScriptEngineFactory factory = engine.getFactory();
    ScriptContext context = engine.getContext();
    context.setAttribute("schema", this, ScriptContext.ENGINE_SCOPE);
    for (Layer layer : layers()) {
      context.setAttribute("layer", layer, ScriptContext.ENGINE_SCOPE);
      if ((Boolean)engine.eval(expression)) {
        layers.add(layer);
      }
    } // next layer
    return layers.toArray(new Layer[0]);
  } // end of getMatchingLayers()

  /**
   * Returns layers that match the given expression.
   * <p> The expression is evaluated as a JavaScript boolean expressin and can assume
   * that <var> layer </var> is a reference to a candidate layer and <var> schema </var>
   * is a reference to this schema.
   * <p> e.g. <q>layer.parentId == schema.participantLayerId &amp;&amp; layer.alignment == 0</q>
   * should match all un-aligned children of the participant layer; i.e. participant attributes.
   * @param predicate A predicate that returns true for the desired layers.
   * @return A list of matching layers, which may be empty.
   */
  public Layer[] getMatchingLayers(Predicate<Layer> predicate) {
    return layers.values().stream()
      .filter(predicate)
      .collect(Collectors.toList())
      .toArray(new Layer[0]);
  } // end of getMatchingLayers()

  /**
   * Copies the IDs of the special layers identified by the given schema.
   * @param source The source schema.
   * @return this
   * @see #getParticipantLayerId()
   * @see #getTurnLayerId()
   * @see #getUtteranceLayerId()
   * @see #getWordLayerId()
   * @see #getEpisodeLayerId()
   * @see #getCorpusLayerId()
   */
  public Schema copyLayerIdsFrom(Schema source) {
    participantLayerId = source.participantLayerId;
    turnLayerId = source.turnLayerId;
    utteranceLayerId = source.utteranceLayerId;
    wordLayerId = source.wordLayerId;
    episodeLayerId = source.episodeLayerId;
    corpusLayerId = source.corpusLayerId;
    return this;
  } // end of copyLayerIdsFrom()

  /**
   * Override of Object's clone method.
   * @return A copy of the object.
   */
  public Object clone() {
    Schema copy = new Schema().copyLayerIdsFrom(this);

    // null comparator, so that children will be in the same order in the copy as they are
    // in the original
    LayerHierarchyTraversal<Schema> t = new LayerHierarchyTraversal<Schema>(copy, null, this) {
        // add parents before children (so we know we don't have to check for orphans)
        protected void pre(Layer layer) {
          if (layer.getParentId() != null) { // not root 
            Layer layerCopy = (Layer)layer.clone();
            result.layers.put(layer.getId(), layerCopy);
            layerCopy.setParent(result.layers.get(layerCopy.getParentId()));
          }
        }
      };

    // copy categories
    copy.categories = (LinkedHashMap)this.categories.clone();
    
    return copy;
  } // end of clone()
  
} // end of class Schema
