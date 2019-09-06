//
// Copyright 2019 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.emusdms;

import java.util.UUID;
import java.util.List;
import java.util.Vector;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.IOException;

import nzilbb.ag.*;
import nzilbb.ag.serialize.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.util.AnnotationComparatorByDistance;
import nzilbb.ag.util.AnnotationComparatorByAnchor;
import nzilbb.ag.util.LayerTraversal;
import nzilbb.ag.util.LayerHierarchyTraversal;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.configure.ParameterField;
import nzilbb.util.IO;
import nzilbb.util.TempFileInputStream;
import org.json.*;

/**
 * Serializer that produces JSON-encoded 'bundles' for consumption by the EMU-webapp.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class BundleSerialization
  implements ISerializer, ISchemaSerializer, IDeserializer
{
  // Attributes:
  
  /**
   * Name of the corpus.
   * @see #getCorpusName()
   * @see #setCorpusName(String)
   */
  @ParameterField("Name of the corpus")
  protected String corpusName = "corpus";
  /**
   * Getter for {@link #corpusName}.
   * @return Name of the corpus.
   */
  public String getCorpusName() { return corpusName; }
  /**
   * Setter for {@link #corpusName}.
   * @param corpusName Name of the corpus.
   * @return <var>this</var>.
   */
  public BundleSerialization setCorpusName(String corpusName)
  { this.corpusName = corpusName; return this; }

  /**
   * UUID of the schema.
   * @see #getUuid()
   * @see #setUuid(String)
   */
  @ParameterField("UUID of the schema")
  protected String uuid = UUID.randomUUID().toString();
  /**
   * Getter for {@link #uuid}.
   * @return UUID of the schema.
   */
  public String getUuid() { return uuid; }
  /**
   * Setter for {@link #uuid}.
   * @param uuid UUID of the schema.
   * @return <var>this</var>.
   */
  public BundleSerialization setUuid(String uuid) { this.uuid = uuid; return this; }

  /**
   * Whether to show perspectives sidebar.
   * @see #getShowPerspectivesSidebar()
   * @see #setShowPerspectivesSidebar(Boolean)
   */
  @ParameterField("Whether to show perspectives sidebar")
  protected Boolean showPerspectivesSidebar = Boolean.FALSE;
  /**
   * Getter for {@link #showPerspectivesSidebar}.
   * @return Whether to show perspectives sidebar.
   */
  public Boolean getShowPerspectivesSidebar() { return showPerspectivesSidebar; }
  /**
   * Setter for {@link #showPerspectivesSidebar}.
   * @param showPerspectivesSidebar Whether to show perspectives sidebar.
   * @return <var>this</var>.
   */
  public BundleSerialization setShowPerspectivesSidebar(Boolean showPerspectivesSidebar)
  { this.showPerspectivesSidebar = showPerspectivesSidebar; return this; }

  /**
   * Whether to allow playback.
   * @see #getPlayback()
   * @see #setPlayback(Boolean)
   */
  @ParameterField("Whether to allow playback")
  protected Boolean playback = Boolean.TRUE;
  /**
   * Getter for {@link #playback}.
   * @return Whether to allow playback.
   */
  public Boolean getPlayback() { return playback; }
  /**
   * Setter for {@link #playback}.
   * @param playback Whether to allow playback.
   * @return <var>this</var>.
   */
  public BundleSerialization setPlayback(Boolean playback)
  { this.playback = playback; return this; }

  /**
   * Whether to show the correction tool.
   * @see #getCorrectionTool()
   * @see #setCorrectionTool(Boolean)
   */
  @ParameterField("Whether to show the correction tool")
  protected Boolean correctionTool = Boolean.TRUE;
  /**
   * Getter for {@link #correctionTool}.
   * @return Whether to show the correction tool.
   */
  public Boolean getCorrectionTool() { return correctionTool; }
  /**
   * Setter for {@link #correctionTool}.
   * @param correctionTool Whether to show the correction tool.
   * @return <var>this</var>.
   */
  public BundleSerialization setCorrectionTool(Boolean correctionTool)
  { this.correctionTool = correctionTool; return this; }

  /**
   * Whether to allow editing of the item size.
   * @see #getEditItemSize()
   * @see #setEditItemSize(Boolean)
   */
  @ParameterField("Whether to allow editing of the item size")
  protected Boolean editItemSize = Boolean.TRUE;
  /**
   * Getter for {@link #editItemSize}.
   * @return Whether to allow editing of the item size.
   */
  public Boolean getEditItemSize() { return editItemSize; }
  /**
   * Setter for {@link #editItemSize}.
   * @param editItemSize Whether to allow editing of the item size.
   * @return <var>this</var>.
   */
  public BundleSerialization setEditItemSize(Boolean editItemSize)
  { this.editItemSize = editItemSize; return this; }

  /**
   * Whether to use large text input field.
   * @see #getUseLargeTextInputField()
   * @see #setUseLargeTextInputField(Boolean)
   */
  @ParameterField("Whether to use large text input field")
  protected Boolean useLargeTextInputField = Boolean.TRUE;
  /**
   * Getter for {@link #useLargeTextInputField}.
   * @return Whether to use large text input field.
   */
  public Boolean getUseLargeTextInputField() { return useLargeTextInputField; }
  /**
   * Setter for {@link #useLargeTextInputField}.
   * @param useLargeTextInputField Whether to use large text input field.
   * @return <var>this</var>.
   */
  public BundleSerialization setUseLargeTextInputField(Boolean useLargeTextInputField)
  { this.useLargeTextInputField = useLargeTextInputField; return this; }

  /**
   * Whether to allow saving of bundles.
   * @see #getSaveBundle()
   * @see #setSaveBundle(Boolean)
   */
  @ParameterField("Whether to allow saving of bundles")
  protected Boolean saveBundle = Boolean.TRUE;
  /**
   * Getter for {@link #saveBundle}.
   * @return Whether to allow saving of bundles.
   */
  public Boolean getSaveBundle() { return saveBundle; }
  /**
   * Setter for {@link #saveBundle}.
   * @param saveBundle Whether to allow saving of bundles.
   * @return <var>this</var>.
   */
  public BundleSerialization setSaveBundle(Boolean saveBundle)
  { this.saveBundle = saveBundle; return this; }

  /**
   * Whether to show hierarchy.
   * @see #getShowHierarchy()
   * @see #setShowHierarchy(Boolean)
   */
  @ParameterField("Whether to show hierarchy")
  protected Boolean showHierarchy = Boolean.TRUE;
  /**
   * Getter for {@link #showHierarchy}.
   * @return Whether to show hierarchy.
   */
  public Boolean getShowHierarchy() { return showHierarchy; }
  /**
   * Setter for {@link #showHierarchy}.
   * @param showHierarchy Whether to show hierarchy.
   * @return <var>this</var>.
   */
  public BundleSerialization setShowHierarchy(Boolean showHierarchy)
  { this.showHierarchy = showHierarchy; return this; }

  /**
   * How much to indent JSON-encoded lines, for each level, or 0 for JSON all on one line.
   * @see #getJsonIndentFactor()
   * @see #setJsonIndentFactor(int)
   */
  protected int jsonIndentFactor = 0;
  /**
   * Getter for {@link #jsonIndentFactor}: How much to indent JSON-encoded lines, for each level,
   * or 0 for JSON all on one line. 
   * @return How much to indent JSON-encoded lines, for each level, or 0 for JSON all on one
   * line. 
   */
  public int getJsonIndentFactor() { return jsonIndentFactor; }
  /**
   * Setter for {@link #jsonIndentFactor}: How much to indent JSON-encoded lines, for each level,
   * or 0 for JSON all on one line. 
   * @param newJsonIndentFactor How much to indent JSON-encoded lines, for each level, or 0 for
   * JSON all on one line. 
   */
  public BundleSerialization setJsonIndentFactor(int newJsonIndentFactor)
  { jsonIndentFactor = newJsonIndentFactor; return this; }
  
  /**
   * Sample rate for audio in Hz. Default is 16000.
   * @see #getSampleRate()
   * @see #setSampleRate(Integer)
   */
  @ParameterField("Sample rate for audio in Hz")
  protected Integer sampleRate = 16000;
  /**
   * Getter for {@link #sampleRate}.
   * @return Sample rate for audio in Hz.
   */
  public Integer getSampleRate() { return sampleRate; }
  /**
   * Setter for {@link #sampleRate}.
   * @param sampleRate Sample rate for audio in Hz.
   * @return <var>this</var>.
   */
  public BundleSerialization setSampleRate(Integer sampleRate)
  { this.sampleRate = sampleRate; return this; }

  /**
   * Whether to implement ONE_TO_MANY relationships between aligned parents and their aligned,
   * saturated children. Setting this to false makes the UI appear more like Praat, where both
   * word and phone are visible on the canvas, but allows users to misalign word and phone
   * boundaries. 
   * @see #getOneToManyRelationships()
   * @see #setOneToManyRelationships(Boolean)
   */
  @ParameterField("Use ONE_TO_MANY relationships where possible (hides word labels when phones are visible)")
  protected Boolean oneToManyRelationships = Boolean.TRUE;
  /**
   * Getter for {@link #oneToManyRelationships}.
   * @return Whether to implement ONE_TO_MANY relationships between aligned parents and their
   * aligned, saturated children. Setting this to false makes the UI appear more like Praat,
   * where both word and phone are visible on the canvas, but allows users to misalign word and
   * phone boundaries. 
   */
  public Boolean getOneToManyRelationships()
  {
    return Optional.of(oneToManyRelationships).orElse(Boolean.TRUE);
  }
  /**
   * Setter for {@link #oneToManyRelationships}.
   * @param oneToManyRelationships Whether to implement ONE_TO_MANY relationships between aligned
   * parents and their aligned, saturated children. Setting this to false makes the UI appear
   * more like Praat, where both word and phone are visible on the canvas, but allows users to
   * misalign word and phone boundaries. 
   * @return <var>this</var>.
   */
  public BundleSerialization setOneToManyRelationships(Boolean oneToManyRelationships) { this.oneToManyRelationships = oneToManyRelationships; return this; }
  /**
   * The last schema passed in.
   * @see #getSchema()
   * @see #setSchema(Schema)
   */
  protected Schema schema;
  /**
   * Getter for {@link #schema}.
   * @return The last schema passed in.
   */
  public Schema getSchema() { return schema; }
  /**
   * Setter for {@link #schema}.
   * @param schema The last schema passed in.
   * @return <var>this</var>.
   */
  public BundleSerialization setSchema(Schema schema) { this.schema = schema; return this; }

  /**
   * A list of JSON bundles to deserialize.
   * @see #getJsonBundles()
   * @see #setJsonBundles(LinkedHashMap)
   */
  protected LinkedHashMap<String,JSONObject> jsonBundles;
  /**
   * Getter for {@link #jsonBundles}.
   * @return A list of JSON bundles to deserialize.
   */
  public LinkedHashMap<String,JSONObject> getJsonBundles() { return jsonBundles; }
  /**
   * Setter for {@link #jsonBundles}.
   * @param jsonBundles A list of JSON bundles to deserialize.
   * @return <var>this</var>.
   */
  public BundleSerialization setJsonBundles(LinkedHashMap<String,JSONObject> jsonBundles)
  { this.jsonBundles = jsonBundles; return this; }
  
  /**
   * Mappings from levels or labels to Layers.
   * @see #getMappings()
   * @see #setMappings(ParameterSet)
   */
  protected ParameterSet mappings;
  /**
   * Getter for {@link #mappings}.
   * @return Mappings from levels or labels to Layers.
   */
  public ParameterSet getMappings() { return mappings; }
  /**
   * Setter for {@link #mappings}.
   * @param mappings Mappings from levels or labels to Layers.
   * @return <var>this</var>.
   */
  public BundleSerialization setMappings(ParameterSet mappings) { this.mappings = mappings; return this; }
  
  /** Required mappings; key is level parameter ID, values are label parameter IDs */
  protected TreeMap<String,TreeSet<String>> requiredMappings;
  
  // Methods:
  
  /**
   * Default constructor.
   */
  public BundleSerialization()
  {
  } // end of constructor

  /**
   * Returns the database configuration (e.g. a possible response to the EMU-webApp's 
   * <tt>GETGLOBALDBCONFIG</tt> request) for the given schema, and the given layers.
   * <p><em>NB</em> currently, all aligned layers are defined to be independent SEGMENT 
   * <i>levels</i> (with an associated <i>attribute</i>), and all tag layers are taken to be 
   * <i>attribute</i>s of the next aligned layer upward in the schema hierarchy.
   * @param schema The Annotation Graph schema.
   * @param layerIds The desired layers to include. The result may include others if necessary.
   * @param name The name of the configuration.
   * @param uuid The UUID of the configuration.
   * @param showPerspectivesSidebar EMU-webApp option.
   * @param playback EMU-webApp option.
   * @param correctionTool EMU-webApp option.
   * @param editItemSize EMU-webApp option.
   * @param useLargeTextInputField EMU-webApp option.
   * @param saveBundle EMU-webApp option.
   * @param showHierarchy EMU-webApp option.
   * @return A JSON-encoded representation of the database configuration.
   */
  public String getDbConfig(Schema schema, List<String> layerIds,
                            String name, String uuid,
                            boolean showPerspectivesSidebar, boolean playback,
                            boolean correctionTool, boolean editItemSize,
                            boolean useLargeTextInputField, boolean saveBundle,
                            boolean showHierarchy)
  {
    HashMap<String,JSONObject> levelsToAdd = new HashMap<String,JSONObject>();
    HashMap<String,JSONArray> attributeDefinitions = new HashMap<String,JSONArray>();
    Comparator<Layer> shallowToDeep = new Comparator<Layer>() {
        // comparator orders layers by 'depth', shallowest first
        // e.g. utterance at the top, then transcript, then segment
        public int compare(Layer l1, Layer l2)
        {
          int l1AncestorCount = l1.getAncestors().size(); 
          int l2AncestorCount = l2.getAncestors().size();
          
          // if they have a different number of ancestors,
          // the one with fewer is lower
          if (l1AncestorCount != l2AncestorCount) return l1AncestorCount - l2AncestorCount;
          
          // if they have the same number of ancestors...
          int l1DescendantDepth = l1.getDescendentDepth();
          int l2DescendantDepth = l2.getDescendentDepth();
          // if they have a different descendant depth,
          // the one with shallower descendants is lower
          if (l1DescendantDepth != l2DescendantDepth) return l1DescendantDepth - l2DescendantDepth;
          // same depth in both directions, so order by ID
          return l1.getId().compareTo(l2.getId());
        }
      };
    TreeSet<Layer> layersShallowToDeep = new TreeSet<Layer>(shallowToDeep);
    // process selected layers, shallowest first
    for (String l : layerIds)
    {
      Layer layer = schema.getLayer(l);
      if (layer != null)
      {
        layersShallowToDeep.add(layer);
        if (layer.getAlignment() == Constants.ALIGNMENT_NONE)
        { // ensure first aligned ancestor is included too
          for (Layer ancestor : layer.getAncestors())
          {
            if (ancestor.getAlignment() != Constants.ALIGNMENT_NONE)
            {
              layersShallowToDeep.add(ancestor);
              break;
            }
          } // next ancestor
        } // not aligned
      } // layer ID is valid
    } // next selected layer
    
    TreeSet<Layer> canvasOrder = new TreeSet<Layer>(shallowToDeep);
    JSONArray linkDefinitions = new JSONArray();
    for (Layer layer : layersShallowToDeep)
    {
      Layer alignedLayer = null; // corresponds to a SEGMENT layer
      Layer tagLayer = null; // corresponds to an attribute of a SEGMENT layer
      if (layer.getAlignment() != Constants.ALIGNMENT_NONE)
      { // this is a level, there's no extra attribute definition
        alignedLayer = layer;
      }
      else
      { // this is an attribute definition, we have to find its parent SEGMENT level
        tagLayer = layer;
        for (Layer ancestor : layer.getAncestors())
        {
          if (ancestor.getAlignment() != Constants.ALIGNMENT_NONE)
          {
            alignedLayer = ancestor;
            break;
          }
        } // next ancestor
          // if we didn't get an aligned layer, we can't continue with this layer
        if (alignedLayer == null) continue;
      }
      if (!levelsToAdd.containsKey(alignedLayer.getId()))
      { // define segment level
        // TODO include 'legalLabels' if any - array of strings
        attributeDefinitions.put(alignedLayer.getId(), new JSONArray()
                                 .put(new JSONObject()
                                      .put("name", alignedLayer.getId())
                                      // as at 2019-06-10, the only supported type:
                                      .put("type", "STRING")));
        levelsToAdd.put(alignedLayer.getId(), new JSONObject()
                        .put("name", alignedLayer.getId())
                        .put("type", "SEGMENT"));
        canvasOrder.add(alignedLayer);

        if (getOneToManyRelationships())
        {
          // Is there already an aligned peer layer?
          final Layer l = alignedLayer;
          Optional<Layer> alignedPeer = alignedLayer.getParent().getChildren().values().stream()
            .filter(peer -> peer != l)
            .filter(peer -> peer.getAlignment() == Constants.ALIGNMENT_INTERVAL)
            .filter(peer -> levelsToAdd.containsKey(peer.getId()))
            .findFirst();
          if (alignedPeer.isPresent())
          { // there's an aligned peer layer
            
            // make the peer an item - e.g. utterance is ITEM of word
            levelsToAdd.get(alignedPeer.get().getId()).put("type", "ITEM");
            canvasOrder.remove(alignedPeer.get());
            // and we add a link between them
            linkDefinitions.put(new JSONObject()
                                .put("type", "ONE_TO_MANY")
                                .put("superlevelName", alignedPeer.get().getId())
                                .put("sublevelName", alignedLayer.getId()));
          }
          // if we're also exporting the parent
          else if (levelsToAdd.containsKey(alignedLayer.getParentId())
              // and the parent is also aligned
              && alignedLayer.getParent().getAlignment() != Constants.ALIGNMENT_NONE
              // and the relationship is saturated
              && alignedLayer.getSaturated()
            )
          { // the parent is actually an "ITEM" layer, not a "SEGMENT" layer
            levelsToAdd.get(alignedLayer.getParentId()).put("type", "ITEM");
            canvasOrder.remove(alignedLayer.getParent());
            // and we add a link between them
            linkDefinitions.put(new JSONObject()
                                .put("type", "ONE_TO_MANY")
                                .put("superlevelName", alignedLayer.getParentId())
                                .put("sublevelName", alignedLayer.getId()));
          }
        } // oneToManyRelationships
      } // define segment level
      if (tagLayer != null)
      { // need to define an attribute
        // TODO include 'legal labels' if any
        attributeDefinitions.get(alignedLayer.getId())
          .put(new JSONObject()
               .put("name", tagLayer.getId())
               // as at 2019-06-10, the only supported type:
               .put("type", "STRING"));
      }
    } // next layer

    // add level definitions into the data
    JSONArray levelDefinitions = new JSONArray();
    for (String id : levelsToAdd.keySet())
    {
      JSONObject level = levelsToAdd.get(id);
      level.put("attributeDefinitions", attributeDefinitions.get(id));
      levelDefinitions.put(level);
    } // next level to add

    // now we've got all the levels, order them shallowest to deepest
    JSONArray levelCanvasesOrder = new JSONArray();
    for (Layer layer : canvasOrder)
    {
      levelCanvasesOrder.put(layer.getId());
    }
    
    JSONObject data = new JSONObject()
      .put("name", name)
      .put("UUID", uuid)
      .put("mediafileExtension", "wav")
      .put("ssffTrackDefinitions", new JSONArray()
           .put(new JSONObject()
                .put("name", "FORMANTS")
                .put("columnName", "fm")
                .put("fileExtension", "fms"))) 
      .put("levelDefinitions", levelDefinitions)
      .put("linkDefinitions", linkDefinitions)
      .put("EMUwebAppConfig", new JSONObject()
           .put("perspectives", new JSONArray()
                .put(new JSONObject()
                     .put("name", "default")
                     .put("signalCanvases", new JSONObject()
                          .put("order", new JSONArray()
                               .put("OSCI")
                               .put("SPEC"))
                          .put("assign", new JSONArray()
                               .put(new JSONObject()
                                    .put("signalCanvasName", "SPEC")
                                    .put("ssffTrackName", "FORMANTS")))
                          .put("contourLims", new JSONArray()
                               .put(new JSONObject()
                                    .put("ssffTrackName", "FORMANTS")
                                    .put("minContourIdx", 0)
                                    .put("maxContourIdx", 1))))
                     .put("levelCanvases", new JSONObject()
                          .put("order", levelCanvasesOrder))
                     .put("twoDimCanvases", new JSONObject()
                          .put("order", new JSONArray()))))
           .put("restrictions", new JSONObject()            
                .put("showPerspectivesSidebar", showPerspectivesSidebar)
                .put("playback", playback)
                .put("correctionTool", correctionTool)
                .put("editItemSize", editItemSize)
                .put("useLargeTextInputField", useLargeTextInputField))
           .put("activeButtons", new JSONObject()
                .put("saveBundle", saveBundle)
                .put("showHierarchy", showHierarchy)));
    return data.toString(jsonIndentFactor);
  } // end of getDbConfig()
  
  /**
   * Serializes the given graph, generating a {@link NamedStream}.
   * @param graph The graph to serialize.
   * @return A named stream that contains the TextGrid. 
   * @throws SerializationException if errors occur during deserialization.
   */
  protected NamedStream serializeGraph(Graph graph) 
    throws SerializationException
  {
    SerializationException errors = null;

    // offset times by how far through the graph is
    final double graphOffset = graph.getStart().getOffset();
    final TreeSet<String> tagLayersWithMultipleValues = new TreeSet<String>();
    final JSONArray links = new JSONArray();

    final Schema schema = graph.getSchema();

    if (getOneToManyRelationships())
    {
      // first detect ONE_TO_MANY relationhips - i.e. aligned saturated child layer is included
      for (final Layer layer : graph.getLayersTopDown())
      {
        // only things below turn
        if (layer.isAncestor(schema.getTurnLayerId())
            && layer.getAlignment() != Constants.ALIGNMENT_NONE)
        {
          Optional<Layer> alignedPeer = layer.getParent().getChildren().values().stream()
            .filter(peer -> peer != layer)
            .filter(peer -> peer.getAlignment() == Constants.ALIGNMENT_INTERVAL)
            .filter(peer -> peer.get("@hasItems") == null)
            .findFirst();
          if (alignedPeer.isPresent())
          { // there's an aligned peer layer
            // mark this layer as being an ITEM level
            layer.put("@hasItems", Boolean.TRUE);
            
            // for each parent layer annotation
            for (Annotation parent : graph.list(layer.getId()))
            {
              // if it has no children
              if (parent.list(alignedPeer.get().getId()).length == 0)
              {
                // create a dummy child so that there are no aligned items
                graph.addTag(parent, layer.getId(), "?")
                  .setConfidence(Constants.CONFIDENCE_NONE);
              }
              // create links
              for (Annotation child : parent.list(alignedPeer.get().getId()))
              {
                child.put("@link", parent);
              }
            } // next parent annotation
          }
          // if saturated
          else if (layer.getSaturated() && !layer.getParent().containsKey("@hasItems"))
          {
            // mark the parent layer as being an ITEM level
            layer.getParent().put("@hasItems", Boolean.TRUE);
            
            // for each parent layer annotation
            for (Annotation parent : graph.list(layer.getParentId()))
            {
              // if it has no childrent
              if (parent.getAnnotations(layer.getId()).size() == 0)
              {
                // create a dummy child so that there are no aligned items
                graph.addTag(parent, layer.getId(), "?")
                  .setConfidence(Constants.CONFIDENCE_NONE);
              }
              // create links
              for (Annotation child : parent.getAnnotations(layer.getId()))
              {
                child.put("@link", parent);
              }
            } // next parent annotation
          }
        } // // aligned and sub-turn
      } // next layer
    }
    
    // traverse the graph depth first
    LayerTraversal<HashMap<String,JSONObject>> traversal
      = new LayerTraversal<HashMap<String,JSONObject>>(new HashMap<String,JSONObject>(), graph) {
          int itemId = 0;
          protected void pre(Annotation annotation)
          {
            Layer layer = annotation.getLayer();
            // only things below turn
            if (layer.isAncestor(schema.getTurnLayerId()))
            {
              switch (layer.getAlignment())
              {
                case Constants.ALIGNMENT_INTERVAL:
                case Constants.ALIGNMENT_INSTANT: // TODO
                { // aligned layers are SEGMENT levels
                  boolean isItem = layer.containsKey("@hasItems");
                  // is the level already defined?
                  if (!result.containsKey(layer.getId()))
                  { // define the level
                    result.put(layer.getId(), new JSONObject()
                               .put("name", layer.getId())
                               // TODO if ONE_TO_MANY, then the parent will be type:ITEM
                               .put("type", isItem?"ITEM":"SEGMENT")
                               .put("items", new JSONArray()));
                  }
                  JSONArray items = result.get(layer.getId()).getJSONArray("items");
                  JSONArray labels = new JSONArray()
                    .put(new JSONObject()
                         .put("name", layer.getId())
                         .put("value", annotation.getLabel()));
                  JSONObject item = new JSONObject()
                    .put("id", itemId++)
                    .put("labels", labels);
                  if (!isItem)
                  { // SEGMENT
                    assert annotation.getStart() != null
                      : "annotation.getStart() != null - " + annotation.getId();
                    assert annotation.getStart().getOffset() != null
                      : "annotation.getStart().getOffset() != null - " + annotation.getId();
                    assert annotation.getEnd() != null
                      : "annotation.getEnd() != null - " + annotation.getId();
                    assert annotation.getEnd().getOffset() != null
                      : "annotation.getEnd().getOffset() != null - " + annotation.getId();
                    assert annotation.getDuration() != null
                      : "annotation.getDuration() != null - " + annotation.getId();
                    assert sampleRate != null : "sampleRate != null";
                    
                    // SEGMENT levels cannot have pauses between intervals,
                    // so insert blank labels before pauses
                    double startOffset = annotation.getStart().getOffset() - graphOffset;
                    long startOffsetSamples = Math.round(startOffset * sampleRate);
                    if (items.length() > 0)
                    {
                      // get last item
                      JSONObject previous = (JSONObject)items.get(items.length()-1);
                      // and get the end time in samples
                      long endSamples = previous.getLong("sampleStart")
                        + previous.getLong("sampleDur");
                      if (startOffsetSamples > endSamples)
                      { // there is a gap
                        // insert a blank item
                        JSONObject pauseItem = new JSONObject()
                          .put("id", itemId++)
                          .put("sampleStart", endSamples)
                          .put("sampleDur", startOffsetSamples - endSamples)
                          // no labels in pauses
                          .put("labels", new JSONArray());
                        items.put(pauseItem);
                      } // there is a gap
                    } // there is a previous annotation
                    
                    double endOffset = annotation.getEnd().getOffset() - graphOffset;
                    long endOffsetSamples = Math.round(endOffset * sampleRate);
                    item
                      .put("sampleStart", startOffsetSamples)
                      .put("sampleDur", endOffsetSamples - startOffsetSamples);

                  } // SEGMENT
                  // if the annotation has a link to another...
                  if (annotation.containsKey("@link"))
                  { // add link to parent
                    Annotation linkedAnnotation = (Annotation)annotation.get("@link");
                    JSONObject parentItem = (JSONObject)linkedAnnotation.get("@item");
                    if (parentItem != null)
                    {
                      links.put(new JSONObject()
                                .put("fromID", parentItem.getInt("id"))
                                .put("toID", item.getInt("id")));
                    }
                  }
                  // make sure child tags can find the labels and item
                  annotation.put("@labels", labels);
                  annotation.put("@item", item);
                  // add the item to the serialization
                  items.put(item);
                  break;
                } // aligned
                default: // Constants.ALIGNMENT_NONE:
                { // unaligned layers are attributes on a parent SEGMENT level
                  // we only add the first tag
                  if (annotation.getOrdinal() == 1)
                  { 
                    // find an aligned ancestor
                    for (Annotation ancestor : annotation.getAncestors())
                    {
                      if (ancestor.containsKey("@labels"))
                      { // found the nearest aligned ancestor
                        JSONArray labels = (JSONArray)ancestor.get("@labels");
                        labels.put(new JSONObject()
                                   .put("name", layer.getId())
                                   .put("value", annotation.getLabel()));
                        break;
                      } // found the nearest aligned ancestor
                    } // next ancestor
                  } // ordinal == 1
                  else
                  {
                    tagLayersWithMultipleValues.add(annotation.getLayerId());
                  }
                } // not aligned                
              } // switch (layer.getAlignment())
            } // a layer below turn
          } // end of pre()
        };

    if (tagLayersWithMultipleValues.size() > 0)
    {
      warnings.add("Tag layer"
                   +(tagLayersWithMultipleValues.size()==1?"":"s")
                   +" with multiple values; only first value used: "
                   + tagLayersWithMultipleValues);
    }
    
    JSONArray levels = new JSONArray();
    for (JSONObject level : traversal.getResult().values()) levels.put(level);
    
    JSONObject data = new JSONObject()
      .put("annotation", new JSONObject()
           .put("name", graph.getId())
           .put("annotates", graph.sourceGraph().getLabel()
                .replaceAll("\\.[^.]+$","")+".wav?t="+graphOffset)
           .put("sampleRate", getSampleRate())
           .put("levels", levels)
           // TODO links define parent ITEM / child SEGMENT relationships
           // [{"fromID": 2, "toID": 102}, from is parent, to is child
           .put("links", links))
      .put("ssffFiles", new JSONArray());
    
    if (graph.getMediaProvider() != null)
    {
      try
      {
        // get media
        String mediaUrl = graph.getMediaProvider().getMedia(
          null, "audio/wav; channels=1; samplerate=" + sampleRate);
        if (mediaUrl != null)
        {
          // encode it
          String base64EncodedContent = IO.Base64Encode(mediaUrl);

          // TODO delete the file if the mediaUrl is a file:// URL
          
          // add it to the bundle
          data.put("mediaFile", new JSONObject()
                   .put("encoding", "BASE64")
                   .put("data", base64EncodedContent));
        }
      }
      catch(Exception exception)
      {
        if (errors == null) errors = new SerializationException();
        if (errors.getCause() == null) errors.initCause(exception);
        errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
      }
    }
  
    if (errors != null) throw errors;
    
    try
    {
      // write the bundle to a temporary file
      File f = File.createTempFile(graph.getId(), ".json");
      FileOutputStream out = new FileOutputStream(f);	 
      PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "utf-8"));
      writer.print(data.toString(jsonIndentFactor));
      writer.close();
      TempFileInputStream in = new TempFileInputStream(f);

      // return a named stream from the file
      return new NamedStream(in, graph.getId().replaceAll("\\.[a-zA-Z]*$", "") + ".json");
    }
    catch(Exception exception)
    {
      errors = new SerializationException();
      errors.initCause(exception);
      errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
      throw errors;
    }      
  }

  // ISerializer and IDeserializer methods
  
  /**
   * Returns the deserializer's descriptor
   * @return The deserializer's descriptor
   */
  public SerializationDescriptor getDescriptor()
  {
    return new SerializationDescriptor(
      "EMU-SDMS Bundle", "0.1", "application/emusdms+json", ".json", "20190906.1040",
      getClass().getResource("icon.png"));
  }
  
  /**
   * Sets parameters for serializer as a whole.  This might include database connection
   * parameters, locations of supporting files, standard layer mappings, etc.
   * <p>When the serializer is installed, this method should be invoked with an empty parameter
   *  set, to discover what (if any) general configuration is required. If parameters are
   *  returned, and user interaction is possible, then the user may be presented with an
   *  interface for setting/confirming these parameters. Unlike the
   *  {@link #load(NamedStream[],Schema)} method, this always returns th}e required parameters, 
   *  whether or not they are fulfilled.
   * @param configuration The general configuration for the serializer. 
   * @param schema The layer schema, definining layers and the way they interrelate.
   * @return A list of configuration parameters (still) must be set before
   *  {@link ISerializer#getRequiredLayers()} can be called. If this is an empty list,
   *  {@link ISerializer#getRequiredLayers()} can be called. If it's not an empty list,
   *  this method must be invoked again with the returned parameters' values set.
   */
  public ParameterSet configure(ParameterSet configuration, Schema schema)
  {
    setSchema(schema);
    // add any parameters that are missing
    configuration.addParameters(this);    
    // set any values that have been passed in
    configuration.apply(this);
    return configuration;
  }

  /** Warnings */
  protected Vector<String> warnings = new Vector<String>();
  /**
   * Returns any warnings that may have arisen during the last execution of 
   * {@link #serialize(Graph[])}.
   * @return A possibly empty list of warnings.
   */
  public String[] getWarnings()
  {
    return warnings.toArray(new String[0]);
  }


  // ISerializer methods
  
  /**
   * Determines which layers, if any, must be present in the graph that will be serialized.
   * @return A list of IDs of layers that must be present in the graph that will be serialized.
   * @throws SerializationParametersMissingException If not all required parameters have a value.
   */
  public String[] getRequiredLayers()
    throws SerializationParametersMissingException
  {
    return new String[0];
  }
  
  /**
   * Serializes the given graph, generating one or more {@link NamedStream}s.
   * <p>Many data formats will only yield one stream (e.g. Transcriber transcript or Praat
   *  textgrid), however there are formats that use multiple files for the same transcript
   *  (e.g. XWaves, EmuR), which is why this method returns a list. There are formats that
   *  are capable of storing multiple transcripts in the same file (e.g. AGTK, Transana XML
   *  export), which is why this method accepts a list.
   * @param graphs The graphs to serialize.
   * @return A list of named streams that contain the serialization in the given format. 
   * @throws SerializerNotConfiguredException if the object has not been configured.
   * @throws SerializationException if errors occur during deserialization.
   */
  public NamedStream[] serialize(Graph[] graphs) 
      throws SerializerNotConfiguredException, SerializationException
  {
    warnings = new Vector<String>();
    Vector<NamedStream> streams = new Vector<NamedStream>();
    for (Graph graph : graphs)
    {
      streams.add(serializeGraph(graph));
    } // next graph
    return streams.toArray(new NamedStream[0]);     
  }
  
  // ISchemaSerializer methods

  /**
   * Serializes the given schema, generating one or more {@link NamedStream}s.
   * <p>Many data formats will only yield one stream (e.g. EMU-SDMS requires one JSON stream),
   * however there may be formats that use multiple files for the same schema, which is why this
   * method returns a list.
   * @param schema The schema to serialize.
   * @param layerIds A list of IDs of layers to include in the serialization, or null for all
   * layers. 
   * @return A list of named streams that contain the serialization in the given format. 
   * @throws SerializerNotConfiguredException if the object has not been configured.
   * @throws SerializationException if errors occur during deserialization.
   */
  public NamedStream[] serializeSchema(Schema schema, List<String> layerIds) 
    throws SerializerNotConfiguredException, SerializationException
  {
    setSchema(schema);
    SerializationException errors = null;

    if (errors != null) throw errors;

    String config = getDbConfig(
      schema, layerIds, corpusName, uuid, showPerspectivesSidebar, playback, correctionTool,
      editItemSize, useLargeTextInputField, saveBundle, showHierarchy);
    
    try
    {
      ByteArrayInputStream in = new ByteArrayInputStream(config.getBytes("UTF-8"));
      return Utility.OneNamedStreamArray(
        new NamedStream(in, corpusName.replaceAll("\\.[a-zA-Z]*$", "") + ".json"));
    }
    catch(Exception exception)
    {
      errors = new SerializationException();
      errors.initCause(exception);
      errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
      throw errors;
    }
  }

  // IDeserializer methods
  
  /**
   * Loads the serialized form of the graph, using the given set of named streams.
   * <p>{@link IDeserializer} method.
   * @param streams A list of named streams that contain all the
   *  transcription/annotation data required, and possibly (a) stream(s) for the media annotated.
   * @param schema The layer schema, definining layers and the way they interrelate.
   * @return A list of parameters that require setting before {@link IDeserializer#deserialize()}
   * can be invoked. This may be an empty list, and may include parameters with the value already
   * set to a workable default. If there are parameters, and user interaction is possible, then
   * the user may be presented with an interface for setting/confirming these parameters, before
   * they are then passed to {@link IDeserializer#setParameters(ParameterSet)}.
   * @throws SerializationException If the graph could not be loaded.
   * @throws IOException On IO error.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public ParameterSet load(NamedStream[] streams, Schema schema)
    throws SerializationException, IOException
  {
    setSchema(schema);
    warnings = new Vector<String>();
    NamedStream[] jsonStreams = Utility.FindStreams(streams, ".json", "application/emusdms+json");
    if (jsonStreams.length == 0)
    {
      throw new SerializationException("No EMU-SDMS bundle streams found");
    }

    // parse the JSON of all streams
    jsonBundles = new LinkedHashMap<String,JSONObject>();
    try
    {
      for (NamedStream stream : jsonStreams)
      {
        String json = IO.InputStreamToString(stream.getStream());
        jsonBundles.put(stream.getName(), new JSONObject(json));
      } // next stream
    }
    catch(JSONException exception)
    {
      throw new SerializationException(
        SerializationException.ErrorType.InvalidDocument, exception.getMessage());
    }

    LayerHierarchyTraversal<TreeMap<String,Layer>> intervalLayers
      = new LayerHierarchyTraversal<TreeMap<String,Layer>>(
        new TreeMap<String,Layer>(), schema) {
          protected void pre(Layer layer)
          {
            if (layer.getAlignment() == Constants.ALIGNMENT_INTERVAL)
            {
              result.put(layer.getId(), layer);
            }
          }
        };
    LayerHierarchyTraversal<TreeMap<String,Layer>> tagLayers
      = new LayerHierarchyTraversal<TreeMap<String,Layer>>(
        new TreeMap<String,Layer>(), schema) {
          protected void pre(Layer layer)
          {
            if (layer.getAlignment() == Constants.ALIGNMENT_NONE
                && layer.getParentId() != schema.getRoot().getId()
                && layer.getParent().getAlignment() == Constants.ALIGNMENT_INTERVAL)
            {
              result.put(layer.getId(), layer);
            }
          }
        };
    
    mappings = new ParameterSet();
    requiredMappings = new TreeMap<String,TreeSet<String>>();
    try
    {
      // Assume first stream is representative, and discover what levels/labels it has,
      // for mapping to layers
      JSONObject firstBundle = jsonBundles.values().iterator().next();
      JSONArray levels = firstBundle.getJSONObject("annotation").getJSONArray("levels");
      for (int l = 0; l < levels.length(); l++)
      {
        JSONObject level = levels.getJSONObject(l);
        if (!level.getString("type").equals("SEGMENT")
            && !level.getString("type").equals("ITEM"))
        {
          throw new SerializationException(
            SerializationException.ErrorType.InvalidDocument,
            "Unrecognized level type: " + level.getString("type"));
        }
        String levelName = level.getString("name");
        final Layer levelLayer = intervalLayers.getResult().get(levelName); // (or null)
        Parameter levelParameter = new Parameter(
          "level_"+levelName, Layer.class, levelName,
          "Layer for level called: " + levelName, true);
        requiredMappings.put(levelParameter.getName(), new TreeSet<String>());
        if (levelLayer != null)
        { // force mapping of same-named layers
          levelParameter.setPossibleValues(new Vector<Layer>() {{ add(levelLayer); }});
          levelParameter.setValue(levelLayer);
        }
        else
        {
          levelParameter.setPossibleValues(intervalLayers.getResult().values());
        }
        mappings.addParameter(levelParameter);

        // read all item labels
        JSONArray items = level.getJSONArray("items");
        for (int i = 0; i < items.length(); i++)
        {
          JSONObject item = items.getJSONObject(i);
          // read all labels
          JSONArray labels = item.getJSONArray("labels");
          for (int lb = 0; lb < labels.length(); lb++)
          {
            JSONObject label = labels.getJSONObject(lb);
            String labelName = label.getString("name");
            if (labelName.equals(levelName)) continue;
            if (!mappings.containsKey(labelName))
            { // no layer mapping yet
              Parameter labelParameter = new Parameter(
                "label_"+labelName, Layer.class, labelName,
                "Layer for label called: " + labelName, false);
              if (levelLayer != null)
              { // there is a level layer, so labels can only be tag unaligned children
                final Layer labelLayer = tagLayers.getResult().get(labelName); // (or null)
                if (labelLayer != null && labelLayer.getParentId().equals(levelLayer.getId()))
                { // a matching levelLayer child tag
                  labelParameter.setPossibleValues(new Vector<Layer>() {{ add(labelLayer); }});
                  labelParameter.setValue(labelLayer);
                } // a matching levelLayer child tag
                else
                { // no matching levelLayer child tag
                  Vector<Layer> tagChildren = new Vector<Layer>();
                  for (Layer child : levelLayer.getChildren().values())
                  {
                    if (child.getAlignment() == Constants.ALIGNMENT_NONE)
                    {
                      tagChildren.add(child);
                    }
                  }
                  levelParameter.setPossibleValues(tagChildren);
                } // no matching levelLayer child tag                
              } // there is a level layer
              else
              { // we don't know the level layer yet, so the label could be any aligned layer
                labelParameter.setPossibleValues(tagLayers.getResult().values());
              }
              
              mappings.addParameter(labelParameter);
              requiredMappings.get(levelParameter.getName()).add(labelParameter.getName());
            } // no layer mapping yet
          } // next item        
        } // next item        
      } // next level
      
    }
    catch(JSONException exception)
    {
      throw new SerializationException(
        SerializationException.ErrorType.InvalidDocument,
        "Invalid JSON structure: " + exception.getMessage());
    }
    return mappings;
  }

  /**
   * Sets parameters for a given deserialization operation, after loading the serialized form of
   * the graph. This might include mappings from format-specific objects like tiers to graph
   * layers, etc. 
   * <p>{@link IDeserializer} method.
   * @param parameters The configuration for a given deserialization operation.
   * @throws SerializationParametersMissingException If not all required parameters have a value.
   */
  public void setParameters(ParameterSet parameters) throws SerializationParametersMissingException
  {
    TreeSet<String> leftovers = new TreeSet<String>(requiredMappings.keySet());
    leftovers.removeAll(parameters.keySet());
    if (leftovers.size() > 0)
    {
      throw new SerializationParametersMissingException("Missing parameters: " + leftovers);
    }
    ParameterSet unset = parameters.unsetRequiredParameters();
    if (unset.size() > 0)
    {
      throw new SerializationParametersMissingException("Unset required parameters: " + unset);
    }
    ParameterSet invalid = parameters.invalidValueParameters();
    if (invalid.size() > 0)
    {
      throw new SerializationParametersMissingException(
        "Parameters with invalid values: " + invalid);
    }
    // check label layers are tag children of corresponding level layers
    for (String levelParameterId : requiredMappings.keySet())
    {
      Parameter levelParameter = parameters.get(levelParameterId);
      Layer levelLayer = (Layer)levelParameter.getValue();
      for (String labelParameterId : requiredMappings.get(levelParameterId))
      {
        Parameter labelParameter = parameters.get(labelParameterId);
        Layer labelLayer = (Layer)labelParameter.getValue();
        if (labelLayer != null)
        { // label layer must be tag child of level layer
          if (!labelLayer.getParentId().equals(levelLayer.getId()))
          {
            throw new SerializationParametersMissingException(
              "Layer for "
              + labelParameter.getLabel() + " ("+labelLayer.getId()
              +") is not a child of layer for "
              + levelParameter.getLabel() + " ("+levelLayer.getId()+")");
          }
          if (labelLayer.getAlignment() != Constants.ALIGNMENT_NONE)
          {
            throw new SerializationParametersMissingException(
              "Layer for "
              + labelParameter.getLabel() + " ("+labelLayer.getId()
              +") is not an unaligned layer ("+labelLayer.getAlignment()+")");
          }
        }
      }
    } // next level parameter
    setMappings(parameters);
  }

  /**
   * Deserializes the serialized data, generating one or more {@link Graph}s.
   * <p>Many data formats will only yield one graph (e.g. Transcriber
   * transcript or Praat textgrid), however there are formats that
   * are capable of storing multiple transcripts in the same file
   * (e.g. AGTK, Transana XML export), which is why this method
   * returns a list.
   * <p>{@link IDeserializer} method.
   * @return A list of valid (if incomplete) {@link Graph}s. 
   * @throws SerializerNotConfiguredException if the object has not been configured.
   * @throws SerializationParametersMissingException if the parameters for this particular graph have not been set.
   * @throws SerializationException if errors occur during deserialization.
   */
  public Graph[] deserialize() 
    throws SerializerNotConfiguredException, SerializationParametersMissingException, SerializationException
  {
    Vector<Graph> graphs = new Vector<Graph>();
    for (String name : jsonBundles.keySet())
    {
      graphs.add(deserializeJson(name, jsonBundles.get(name)));
    } // next bundle
    return graphs.toArray(new Graph[0]);
  }

  /**
   * Deserializes a given JSON bundle.
   * @param name The name of the original stream.
   * @param bundle JSON representation of the bundle.
   * @return The graph represented by the JSON bundle.
   * @throws SerializerNotConfiguredException
   * @throws SerializationParametersMissingException
   * @throws SerializationException
   */
  private Graph deserializeJson(String name, JSONObject bundle)
    throws SerializerNotConfiguredException, SerializationParametersMissingException, SerializationException
  {
    try
    {
      JSONObject top = bundle.getJSONObject("annotation");
      double sampleRate = top.getDouble("sampleRate");
      Graph graph = new Graph();
      graph.setId(top.getString("name"));
      graph.setOffsetGranularity(1/sampleRate);

      // copy schema special layer IDs
      graph.getSchema().copyLayerIdsFrom(schema);

      String rootId = graph.getSchema().getRoot().getId();
      for (Parameter p : mappings.values())
      {
        Layer layer = (Layer)p.getValue();
        if (layer != null)
        {
          // the schema structure must match the one we've been given
          // so unmapped ancestors must nevertheless be added
          for (Layer ancestor : layer.getAncestors())
          {
            if (graph.getLayer(ancestor.getId()) == null)
            {
              graph.addLayer((Layer)ancestor.clone());
            }
          } // next ancestor
          // now add the layer itself
          if (graph.getLayer(layer.getId()) == null)
          {
            graph.addLayer((Layer)layer.clone());
          }
        } // there's a mapped layer
      } // next parameter
      
      graph.setOffsetUnits(Constants.UNIT_SECONDS);
      
      JSONArray levels = top.getJSONArray("levels");

      HashMap<Integer,Annotation> idToAnnotation = new HashMap<Integer,Annotation>();
      
      for (int l = 0; l < levels.length(); l++)
      {
        JSONObject level = levels.getJSONObject(l);
        if (!level.getString("type").equals("SEGMENT")
            && !level.getString("type").equals("ITEM"))
        {
          throw new SerializationException(
            SerializationException.ErrorType.InvalidDocument,
            name + ": Unrecognized level type: " + level.getString("type"));
        }
        String levelName = level.getString("name");
        Layer levelLayer = (Layer)mappings.get("level_"+levelName).getValue();

        // read all item labels
        JSONArray items = level.getJSONArray("items");
        for (int i = 0; i < items.length(); i++)
        {
          JSONObject item = items.getJSONObject(i);
          String startId = null;
          String endId = null;
          if (level.getString("type").equals("SEGMENT"))
          {
            // get anchors
            Anchor start = graph.getOrCreateAnchorAt(
              item.getDouble("sampleStart")/sampleRate, Constants.CONFIDENCE_MANUAL);
            startId = start.getId();
            
            Anchor end = graph.getOrCreateAnchorAt(
              // to avoid rounding errors, get end time in samples, then divide by sampleRate:
              (item.getDouble("sampleStart") + item.getDouble("sampleDur"))/sampleRate,
              Constants.CONFIDENCE_MANUAL);
            endId = end.getId();
          }
          Annotation levelAnnotation = new Annotation().setLayerId(levelLayer.getId());
          idToAnnotation.put(item.getInt("id"), levelAnnotation);
          if (startId != null)
          {
            levelAnnotation.setStartId(startId);
            levelAnnotation.setEndId(endId);
          }
          // read all labels
          JSONArray labels = item.getJSONArray("labels");
          for (int lb = 0; lb < labels.length(); lb++)
          {
            JSONObject label = labels.getJSONObject(lb);
            String labelName = label.getString("name");
            String labelValue = label.getString("value");
            if (labelValue.length() == 0) continue;
            if (labelName.equals(levelName))
            {
              levelAnnotation.setLabel(labelValue);
              // now we know it's not an empty interval, we can add it to the graph
              graph.addAnnotation(levelAnnotation);
            }
            else
            { // a tag layer
              Layer labelLayer = (Layer)mappings.get("label_"+labelName).getValue();
              if (labelLayer != null)
              {
                // ensure the level annotation is in the graph, as it's the parent
                if (levelAnnotation.getId() == null) graph.addAnnotation(levelAnnotation);
                // create a tag annotation
                graph.addTag(levelAnnotation, labelLayer.getId(), labelValue);
              } // label is mapped to a layer
            } // a tag layer
          } // next label
        } // next item

      } // next level

      // now link parents to children
      JSONArray links = top.getJSONArray("links");
      TreeSet<Integer> unalignedItems = new TreeSet<Integer>();
      unalignedItems.add(-1);
      int loopCount = 0;
      do
      {
        unalignedItems = new TreeSet<Integer>();
        for (int l = 0; l < links.length(); l++)
        {
          JSONObject link = links.getJSONObject(l);
          Annotation parent = idToAnnotation.get(link.getInt("fromID"));
          if (parent == null)
          {
            throw new SerializationException(
              SerializationException.ErrorType.InvalidDocument,
              "Link: fromID " + link.getInt("fromID") + " not found");
          }
          Annotation child = idToAnnotation.get(link.getInt("toID"));
          if (child == null)
          {
            throw new SerializationException(
              SerializationException.ErrorType.InvalidDocument,
              "Link: toID " + link.getInt("toID") + " not found");
          }
          if (child.getLayer().getParentId().equals(parent.getLayerId())
              && child.getParent() == null)
          {
            parent.addAnnotation(child);
          }
          
          // check/set parent anchors
          Anchor parentStart = parent.getStart();
          Anchor childStart = child.getStart();
          if (childStart != null)
          {
            if (parentStart == null || parentStart.getOffset() == null)
            {
              parentStart = childStart;
            }
            if (childStart.getOffset() != null
                && childStart.getOffset() < parentStart.getOffset())
            {
              parentStart = childStart;
            }
          }
          parent.setStart(parentStart);
          Anchor parentEnd = parent.getEnd();
          Anchor childEnd = child.getEnd();
          if (childEnd != null)
          {
            if (parentEnd == null || parentEnd.getOffset() == null)
            {
              parentEnd = childEnd;
            }
            if (childEnd.getOffset() != null
                && childEnd.getOffset() > parentEnd.getOffset())
            {
              parentEnd = childEnd;
            }
          }
          parent.setEnd(parentEnd);
          if (!parent.getAnchored()) unalignedItems.add(link.getInt("fromID"));
        } // next link

        // this while loop is potentially infinite
      } // next pass to align everything
      while (unalignedItems.size() > 0 && loopCount < 100);
      
      if (unalignedItems.size() > 0)
      {
        throw new SerializationException(
          SerializationException.ErrorType.InvalidDocument,
          "Link: could not anchor items: " + unalignedItems);
      }

      // now we've got all the annotations, ensure they all have parents
      // traverse bottom-up in the hierarchy, to ensure that parents have been created before
      // we get to their layer, and also becayse overlappingAnnotations() uses list(), which
      // will only find annotations that are either orphans, or have a complete hierarchy
      // (if a parent candidate's parent is set but not its grandparent, list() won't list it)
      Vector<Layer> layersBottomUp = new LayerHierarchyTraversal<Vector<Layer>>(
        new Vector<Layer>(), 
        new Comparator<Layer>() { // reverse default child order
          public int compare(Layer l1, Layer l2) { 
            return -LayerHierarchyTraversal.defaultComparator.compare(l1,l2); 
          } },
        graph.getSchema()) {
          protected void post(Layer layer) { result.add(layer); } // post = child before parent
        }.getResult();
      for (Layer layer : layersBottomUp)
      {
        for (Annotation annotation : graph.list(layer.getId()))
        {
          if (!annotation.getAnchored()) continue;
              
          Layer parentLayer = annotation.getLayer().getParent();
          if (annotation.getParent() == null)
          {
            Annotation[] candidates = graph.overlappingAnnotations(annotation, parentLayer.getId());
            if (candidates.length == 1)
            { // parent found
              annotation.setParent(candidates[0]);
            } // parent found
            else if (candidates.length > 1)
            { // multiple possible parents found
              // pick the one with the most overlap - i.e. the lowest distance
              TreeSet<Annotation> byDistance = new TreeSet<Annotation>(
                new AnnotationComparatorByDistance(annotation));
              for (Annotation c : candidates) byDistance.add(c);
              annotation.setParent(byDistance.first());
            } // multiple possible parents found
            else
            { // parent not found
              // create a dummy parent
              Anchor start = graph.getStart();
              Anchor end = graph.getEnd();
              Annotation dummy = graph.addAnnotation(
                new Annotation(null, "", annotation.getLayer().getParentId(),
                               start.getId(), end.getId()));
              // later we need to unhook the anchors, so tag this as a dummy
              dummy.put("@dummy", Boolean.TRUE);
              
              annotation.setParent(dummy);
            } // parent not found
          } // annotation needs parent

          // check turns take label of utterance, and participant, from turn
          if (annotation.getParent().getLabel().length() == 0
              && ( // utterance -> turn
                (annotation.getLayer().getId().equals(graph.getSchema().getUtteranceLayerId())
                 && parentLayer.getId().equals(graph.getSchema().getTurnLayerId()))
                || // turn -> who
                (annotation.getLayer().getId().equals(graph.getSchema().getTurnLayerId())
                 && parentLayer.getId().equals(graph.getSchema().getParticipantLayerId())))
            )
          { // copy label to parent, which is the participant label
            annotation.getParent().setLabel(annotation.getLabel());
          }
        } // next annotation
      } // next layer

      // now de-anchor dummy annotations
      HashSet<Anchor> referencedAnchors = new HashSet<Anchor>();
      for (Annotation annotation : graph.getAnnotationsById().values())
      {
        referencedAnchors.add(annotation.getStart());
        referencedAnchors.add(annotation.getEnd());
        if (annotation.get("@dummy") != null)
        {
          annotation.remove("@dummy");
          annotation.setStartId("dummy-start");
          annotation.setEndId("dummy-end");
        } // dummy annotation
      } // next annotation

      // and remove any un-referenced anchors
      graph.getAnchors().values().retainAll(referencedAnchors);

      graph.commit();
      
      return graph;
    }
    catch(JSONException exception)
    {
      throw new SerializationException(
        SerializationException.ErrorType.InvalidDocument,
        name + ": Invalid JSON structure: " + exception.getMessage());
    }
  } // end of deserializeJson()
 
} // end of class BundleSerialization
