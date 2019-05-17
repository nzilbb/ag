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
import java.util.TreeSet;
import java.util.Comparator;
import java.util.Base64;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import nzilbb.ag.*;
import nzilbb.ag.serialize.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.util.LayerTraversal;
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
public class BundleSerializer
  implements ISerializer, ISchemaSerializer
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
  public BundleSerializer setCorpusName(String corpusName) { this.corpusName = corpusName; return this; }

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
  public BundleSerializer setUuid(String uuid) { this.uuid = uuid; return this; }

  /**
   * Whether to show perspectives sidebar.
   * @see #getShowPerspectivesSidebar()
   * @see #setShowPerspectivesSidebar(Boolean)
   */
  @ParameterField("Whether to show perspectives sidebar")
  protected Boolean showPerspectivesSidebar = Boolean.TRUE;
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
  public BundleSerializer setShowPerspectivesSidebar(Boolean showPerspectivesSidebar) { this.showPerspectivesSidebar = showPerspectivesSidebar; return this; }

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
  public BundleSerializer setPlayback(Boolean playback) { this.playback = playback; return this; }

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
  public BundleSerializer setCorrectionTool(Boolean correctionTool) { this.correctionTool = correctionTool; return this; }

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
  public BundleSerializer setEditItemSize(Boolean editItemSize) { this.editItemSize = editItemSize; return this; }

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
  public BundleSerializer setUseLargeTextInputField(Boolean useLargeTextInputField) { this.useLargeTextInputField = useLargeTextInputField; return this; }

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
  public BundleSerializer setSaveBundle(Boolean saveBundle) { this.saveBundle = saveBundle; return this; }

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
  public BundleSerializer setShowHierarchy(Boolean showHierarchy) { this.showHierarchy = showHierarchy; return this; }

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
  public BundleSerializer setJsonIndentFactor(int newJsonIndentFactor) { jsonIndentFactor = newJsonIndentFactor; return this; }
  
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
  public BundleSerializer setSampleRate(Integer sampleRate) { this.sampleRate = sampleRate; return this; }

  // Methods:
  
 /**
   * Default constructor.
   */
  public BundleSerializer()
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
    TreeSet<Layer> layerOrder = new TreeSet<Layer>(new Comparator<Layer>() {
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
      });
    
    TreeSet<String> layersToExport = new TreeSet<String>();
    layersToExport.addAll(layerIds);
    // include utterance layer, whether its mentioned or not
    layersToExport.add(schema.getUtteranceLayerId());
    for (String l : layersToExport)
    {
      Layer layer = schema.getLayer(l);
      Layer alignedLayer = null; // corresponds to a SEGMENT layer
      Layer tagLayer = null; // corresponds to an attribute of a SEGMENT layer
      if (layer.getAlignment() != Constants.ALIGNMENT_NONE)
      { // this is a SEGMENT level, there's no extra attribute definition
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
        attributeDefinitions.put(alignedLayer.getId(), new JSONArray()
                                 .put(new JSONObject()
                                      .put("name", alignedLayer.getId())
                                      .put("type", "STRING"))); // TODO other types
        levelsToAdd.put(alignedLayer.getId(), new JSONObject()
                        .put("name", alignedLayer.getId())
                        .put("type", "SEGMENT"));
        layerOrder.add(alignedLayer);
      } // define segment level
      if (tagLayer != null)
      { // need to define an attribute
        attributeDefinitions.get(alignedLayer.getId())
          .put(new JSONObject()
               .put("name", tagLayer.getId())
               .put("type", "STRING")); // TODO other types
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

    // now we've got all the SEGMENT levels, order them shallowest to deepest
    JSONArray levelCanvasesOrder = new JSONArray();
    for (Layer layer : layerOrder)
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
      .put("linkDefinitions", new JSONArray())
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

    final Schema schema = graph.getSchema();
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
                  // is the level already defined?
                  if (!result.containsKey(layer.getId()))
                  { // define the level
                    result.put(layer.getId(), new JSONObject()
                               .put("name", layer.getId())
                               .put("type", "SEGMENT")
                               .put("items", new JSONArray()));
                  }
                  JSONArray items = result.get(layer.getId()).getJSONArray("items");
                  JSONArray labels = new JSONArray()
                    .put(new JSONObject()
                         .put("name", layer.getId())
                         .put("value", annotation.getLabel()));
                  assert annotation.getStart() != null : "annotation.getStart() != null - " + annotation.getId();
                  assert annotation.getStart().getOffset() != null : "annotation.getStart().getOffset() != null - " + annotation.getId();
                  assert annotation.getEnd() != null : "annotation.getEnd() != null - " + annotation.getId();
                  assert annotation.getEnd().getOffset() != null : "annotation.getEnd().getOffset() != null - " + annotation.getId();
                  assert annotation.getDuration() != null : "annotation.getDuration() != null - " + annotation.getId();
                  assert sampleRate != null : "sampleRate != null";

                  // SEGMENT levels cannot have pauses between intervals, so insert blank labels before pauses
                  double offset = annotation.getStart().getOffset() - graphOffset;
                  long offsetSamples = (long)(offset * sampleRate);
                  if (items.length() > 0)
                  {
                    // get last item
                    JSONObject previous = (JSONObject)items.get(items.length()-1);
                    // and get the end time in samples
                    long endSamples = previous.getLong("sampleStart")
                      + previous.getLong("sampleDur");
                    if (offsetSamples > endSamples)
                    { // there is a gap
                      // insert a blank item
                      JSONObject item = new JSONObject()
                        .put("id", itemId++)
                        .put("sampleStart", endSamples)
                        .put("sampleDur", offsetSamples - endSamples)
                        .put("labels", new JSONArray()
                             .put(new JSONObject().put("name", layer.getId()).put("value", "")));
                      items.put(item);
                    } // there is a gap
                  } // there is a previous annotation
                  
                  JSONObject item = new JSONObject()
                    .put("id", itemId++)
                    .put("sampleStart", offsetSamples)
                    .put("sampleDur", (long)(annotation.getDuration() * sampleRate))
                    .put("labels", labels);
                  // make sure child tags can find the labels, as they'll need to add some
                  annotation.put("@labels", labels);
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
                } // not aligned                
              } // switch (layer.getAlignment())
            } // a layer below turn
          } // end of pre()
          protected void post(Annotation annotation)
          {
            // tidy up item links
            annotation.remove("@labels");
          } // end of post()
        };
    
    JSONArray levels = new JSONArray();
    for (JSONObject level : traversal.getResult().values()) levels.put(level);
    
    JSONObject data = new JSONObject()
      .put("annotation", new JSONObject()
           .put("name", graph.getId())
           .put("annotates", graph.getLabel())
           .put("sampleRate", getSampleRate())
           .put("levels", levels)
           .put("links", new JSONArray()))
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

  // ISerializer methods
  
  /**
   * Returns the deserializer's descriptor
   * @return The deserializer's descriptor
   */
  public SerializationDescriptor getDescriptor()
  {
    return new SerializationDescriptor(
      "EMU-SDMS Bundle", "0.01", "application/emusdms+json", ".json", "20190514.1428", getClass().getResource("icon.png"));
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
    // add any parameters that are missing
    configuration.addParameters(this);    
    // set any values that have been passed in
    configuration.apply(this);
    return configuration;
  }
  
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
    Vector<NamedStream> streams = new Vector<NamedStream>();
    for (Graph graph : graphs)
    {
      streams.add(serializeGraph(graph));
    } // next graph
    return streams.toArray(new NamedStream[0]);     
  }
  
  /**
   * Returns any warnings that may have arisen during the last execution of {@link #serialize(Graph[])}.
   * @return A possibly empty list of warnings.
   */
  public String[] getWarnings()
  {
    return new String[0];
  }

  // ISchemaSerializer methods

  /**
   * Serializes the given schema, generating one or more {@link NamedStream}s.
   * <p>Many data formats will only yield one stream (e.g. EMU-SDMS requires one JSON stream),
   * however there may be formats that use multiple files for the same schema, which is why this
   * method returns a list.
   * @param schema The schema to serialize.
   * @param layerIds A list of IDs of layers to include in the serialization, or null for all layers.
   * @return A list of named streams that contain the serialization in the given format. 
   * @throws SerializerNotConfiguredException if the object has not been configured.
   * @throws SerializationException if errors occur during deserialization.
   */
  public NamedStream[] serializeSchema(Schema schema, List<String> layerIds) 
    throws SerializerNotConfiguredException, SerializationException
  {
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

} // end of class BundleSerializer
