//
// Copyright 2016-2020 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag.serialize.json;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.Vector;
import java.util.function.Consumer;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import nzilbb.ag.*;
import nzilbb.ag.serialize.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.util.TempFileInputStream;

/**
 * Annotation Graph serializer/deserializer for JSON.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class JSONSerialization
   implements GraphSerializer
{
   // Attributes:

   /**
    * String used for indenting.
    * @see #getIndenter()
    * @see #setIndenter(String)
    */
   protected String indenter = "  ";
   /**
    * Getter for {@link #indenter}: String used for indenting.
    * @return String used for indenting.
    */
   public String getIndenter() { return indenter; }
   /**
    * Setter for {@link #indenter}: String used for indenting.
    * @param newIndenter String used for indenting.
    */
   public JSONSerialization setIndenter(String newIndenter) { indenter = newIndenter; return this; }

   private DecimalFormat fmtOffset = new DecimalFormat(
      // force the locale to something with . as the decimal separator
      "0.0#########", new DecimalFormatSymbols(Locale.UK));
   
   protected Vector<String> warnings;
   private Schema schema;
   private LinkedHashMap<String, JsonObject> jsons;

   
   /**
    * Whether to sort anchors by offset and graph structure (which takes time for larger graphs). Default is false.
    * @see #getSortAnchors()
    * @see #setSortAnchors(boolean)
    */
   protected boolean sortAnchors = false;
   /**
    * Getter for {@link #sortAnchors}: Whether to sort anchors by offset and graph structure (which takes time for larger graphs). Default is false.
    * @return Whether to sort anchors by offset and graph structure (which takes time for larger graphs).
    */
   public boolean getSortAnchors() { return sortAnchors; }
   /**
    * Setter for {@link #sortAnchors}: Whether to sort anchors by offset and graph structure (which takes time for larger graphs).
    * @param newSortAnchors Whether to sort anchors by offset and graph structure (which takes time for larger graphs).
    */
   public JSONSerialization setSortAnchors(boolean newSortAnchors) { sortAnchors = newSortAnchors; return this; }

   private long graphCount = 0;
   private long consumedGraphCount = 0;
   /**
    * Determines how far through the serialization is.
    * @return An integer between 0 and 100 (inclusive), or null if progress can not be calculated.
    */
   public Integer getPercentComplete()
   {
      if (graphCount < 0) return null;
      return (int)((consumedGraphCount * 100) / graphCount);
   }
   
   /**
    * Serialization marked for cancelling.
    * @see #getCancelling()
    * @see #setCancelling(boolean)
    */
   protected boolean cancelling;
   /**
    * Getter for {@link #cancelling}: Serialization marked for cancelling.
    * @return Serialization marked for cancelling.
    */
   public boolean getCancelling() { return cancelling; }
   /**
    * Setter for {@link #cancelling}: Serialization marked for cancelling.
    * @param newCancelling Serialization marked for cancelling.
    */
   public JSONSerialization setCancelling(boolean newCancelling) { cancelling = newCancelling; return this; }

   // Methods:
   
   /**
    * Default constructor.
    */
   public JSONSerialization()
   {
   } // end of constructor

   /**
    * Returns the serialization descriptor.
    * <p>{@link GraphSerializer} and {@link IDeserializer} method.
    * @return The deserializer's descriptor
    */
   public SerializationDescriptor getDescriptor()
   {
      return new SerializationDescriptor(
	 "JSON (JavaScript Object Notation)", "0.2", "application/json", ".json", "20200710.1904", 
	 getClass().getResource(getClass().getSimpleName() + ".png"));
   }

   /**
    * Cancel the serialization in course (if any).
    */
   public void cancel()
   {
      setCancelling(true);
   }

   /**
    * Returns any warnings that may have arisen during the last execution of {@link #deserialize()}.
    * <p>{@link GraphSerializer} and {@link IDeserializer} method.
    * @return A possibly empty list of warnings.
    */
   public String[] getWarnings()
   {
      return warnings.toArray(new String[0]);
   }

   /**
    * Sets parameters for serializer as a whole.  This might include database connection
    * parameters, locations of supporting files, standard layer mappings, etc.
    * <p>When the serializer is installed, this method should be invoked with an empty parameter
    *  set, to discover what (if any) general configuration is required. If parameters are
    *  returned, and user interaction is possible, then the user may be presented with an
    *  interface for setting/confirming these parameters. 
    * <p>{@link GraphSerializer} and {@link IDeserializer} method.
    * @param configuration The general configuration for the serializer. 
    * @param schema The layer schema, definining layers and the way they interrelate.
    * @return A list of configuration parameters (still) must be set before
    *  {@link GraphSerializer#getRequiredLayers()} can be called. If this is an empty list,
    *  {@link GraphSerializer#getRequiredLayers()} can be called. If it's not an empty list,
    *  this method must be invoked again with the returned parameters' values set.
    */
   public ParameterSet configure(ParameterSet configuration, Schema schema)
   {
      return new ParameterSet();
   }

   /**
    * Loads the serialized form of the graph, using the given set of named streams.
    * <p>{@link IDeserializer} method.
    * @param streams A list of named streams that contain all the
    *  transcription/annotation data required.
    * @param schema The layer schema, definining layers and the way they interrelate.
    * @return A list of parameters that require setting before {@link IDeserializer#deserialize()}
    *  can be invoked. This may be an empty list, and may include parameters with the value
    *  already set to a workable default. If there are parameters, and user interaction is
    *  possible, then the user may be presented with an interface for setting/confirming these
    *  parameters, before they are then passed to {@link IDeserializer#setParameters(ParameterSet)}.
    * @throws SerializationException If the graph could not be loaded.
    * @throws IOException On IO error.
    * @throws SerializerNotConfiguredException If the configuration is not sufficient for deserialization.
    */
   public ParameterSet load(NamedStream[] streams, Schema schema) throws IOException, SerializationException, SerializerNotConfiguredException
   {
      warnings = new Vector<String>();
      this.schema = schema;

      jsons = new LinkedHashMap<String, JsonObject>();
      for (NamedStream stream : streams)
      {	 
	 if (stream.getName().endsWith(".json") || "application/json".equals(stream.getMimeType()))
	 {
	    jsons.put(stream.getName(), Json.createReader(new InputStreamReader(stream.getStream())).readObject());
	 }
      } // next stream

      return new ParameterSet();
   }

   /**
    * Sets parameters for a given deserialization operation, after loading the serialized form
    * of the graph. This might include mappings from format-specific objects like tiers to graph
    * layers, etc.
    * <p>{@link IDeserializer} method.
    * @param parameters The configuration for a given deserialization operation.
    * @throws SerializationParametersMissingException If not all required parameters have a value.
    */
   public void setParameters(ParameterSet parameters) throws SerializationParametersMissingException
   {
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
      // if there are errors, accumlate as many as we can before throwing SerializationException
      SerializationException errors = null;

      Vector<Graph> graphs = new Vector<Graph>();

      for (JsonObject o : jsons.values())
      {
	 try
	 {
	    graphs.add(jsonToGraph(o));
	 }
	 catch(SerializationException exception)
	 {
	    if (errors == null)
	       errors = exception;
	    else
	       errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
	 }
      } // next JSON stream

      if (errors != null) throw errors;
      return graphs.toArray(new Graph[0]);
   }
   
   /**
    * Converts a JSON object to a Graph.
    * @param json JSON object
    * @return The graph
    * @throws SerializationException On error.
    */
   protected Graph jsonToGraph(JsonObject json)
      throws SerializationException
   {
      try
      {
	 Graph graph = new Graph();
	 graph.setId(json.getString("id"));
	 if (json.containsKey("offsetGranularity"))
	 {
	    graph.setOffsetGranularity(json.getJsonNumber("offsetGranularity").doubleValue());
	 }

	 // schema
	 graph.setSchema(jsonToSchema(json.getJsonObject("schema")));

	 // anchors
	 JsonObject anchors = json.getJsonObject("anchors");
	 for (String anchorId : anchors.keySet())
	 {
	    graph.addAnchor(jsonToAnchor(anchorId, anchors.getJsonObject(anchorId)));
	 } // next child
	 
	 // annotations
	 for (String topLevelId : graph.getSchema().getRoot().getChildren().keySet())
	 {
	    if (json.containsKey(topLevelId))
	    {
	       jsonToAnnotations(graph, topLevelId, graph.getId(), json.getJsonArray(topLevelId));
	    }
	 } // next top level layer
	 return graph;
      }
      catch (JsonException x)
      {
	 throw new SerializationException(x);
      }
   } // end of jsonToGraph()

   /**
    * Converts a JSON object to a Schema.
    * @param json JSON object
    * @return The schema
    * @throws SerializationException On error.
    */
   protected Schema jsonToSchema(JsonObject json)
      throws SerializationException
   {
      try
      {
	 Schema s = new Schema();
	 if (json.containsKey("participantLayerId")) 
	    s.setParticipantLayerId(json.getString("participantLayerId"));
	 if (json.containsKey("turnLayerId")) 
	    s.setTurnLayerId(json.getString("turnLayerId"));
	 if (json.containsKey("utteranceLayerId")) 
	    s.setUtteranceLayerId(json.getString("utteranceLayerId"));
	 if (json.containsKey("wordLayerId")) 
	    s.setWordLayerId(json.getString("wordLayerId"));
	 if (json.containsKey("episodeLayerId")) 
	    s.setEpisodeLayerId(json.getString("episodeLayerId"));
	 if (json.containsKey("corpusLayerId")) 
	    s.setCorpusLayerId(json.getString("corpusLayerId"));

	 JsonObject root = json.getJsonObject("transcript");
	 JsonObject topLevel = root.getJsonObject("children");
	 for (String childId : topLevel.keySet())
	 {
	    jsonToLayer(s, "transcript", childId, topLevel.getJsonObject(childId));
	 } // next child
	 return s;
      }
      catch (JsonException x)
      {
	 throw new SerializationException(x);
      }
   } // end of jsonToSchema()

   /**
    * Converts a JSON object to a Layer definition.
    * @param s The schema to add te layer to.
    * @param parentId The layer's parent ID.
    * @param layerId The layer's ID.
    * @param json JSON object
    * @return The layer
    * @throws SerializationException On error.
    */
   protected Layer jsonToLayer(Schema s, String parentId, String layerId, JsonObject json)
      throws SerializationException
   {
      try
      {
	 Layer l = new Layer();
	 l.setId(layerId);
	 l.setParentId(parentId);
	 l.setDescription(json.getString("description"));
	 l.setAlignment(json.getInt("alignment"));
	 l.setPeers(json.getBoolean("peers"));
	 l.setPeersOverlap(json.getBoolean("peersOverlap"));
	 l.setParentIncludes(json.getBoolean("parentIncludes"));
	 l.setSaturated(json.getBoolean("saturated"));
	 s.addLayer(l);
	 if (json.containsKey("children"))
	 {
	    JsonObject children = json.getJsonObject("children");
	    for (String childId : children.keySet())
	    {
	       jsonToLayer(s, layerId, childId, children.getJsonObject(childId));
	    } // next child
	 }
	 return l;
      }
      catch (JsonException x)
      {
	 throw new SerializationException(x);
      }
   } // end of jsonToGraph()

   /**
    * Converts a JSON object to an Anchor.
    * @param anchorId The ID of the anchor.
    * @param json JSON object
    * @return The anchor
    * @throws SerializationException On error.
    */
   protected Anchor jsonToAnchor(String anchorId, JsonObject json)
      throws SerializationException
   {
      try
      {
	 Anchor a = new Anchor();
	 a.setId(anchorId);
	 if (json.containsKey("offset") && !json.isNull("offset")) 
	    a.setOffset(json.getJsonNumber("offset").doubleValue());
	 if (json.containsKey("confidence")) 
	    a.setConfidence(json.getInt("confidence"));
	 if (json.containsKey(Constants.COMMENT)) 
	    a.put(Constants.COMMENT, json.getString(Constants.COMMENT));
	 return a;
      }
      catch (JsonException x)
      {
	 throw new SerializationException(x);
      }
   } // end of jsonToGraph()
   
   /**
    * Recursively converts a JSON array of annotation definitions to Annotations, which are added to the graph.
    * @param graph The graph to add the annotations to.
    * @param layerId The ID of the layer to which the annotations should be added.
    * @param parentId The ID of the parent of the annotations.
    * @param json JSON array
    * @throws SerializationException On error.
    */
   protected void jsonToAnnotations(Graph graph, String layerId, String parentId, JsonArray json)
      throws SerializationException
   {
      try
      {
	 for (int i = 0; i < json.size(); i++)
	 {
	    jsonToAnnotation(graph, layerId, parentId, i+1, json.getJsonObject(i));
	 } // next annotation
      }
      catch (JsonException x)
      {
	 throw new SerializationException(x);
      }
   } // end of jsonToGraph()

   /**
    * Converts a JSON object to an Annotation, and recursively creates child annotations.
    * @param graph The graph to add the annotation to.
    * @param layerId The ID of the layer to which the annotation belongs.
    * @param parentId The ID of the parent of the annotations.
    * @param json JSON object
    * @return The anchor
    * @throws SerializationException On error.
    */
   protected Annotation jsonToAnnotation(Graph graph, String layerId, String parentId, int ordinal, JsonObject json)
      throws SerializationException
   {
      try
      {
	 Annotation a = new Annotation();
	 a.setLayerId(layerId);
	 a.setParentId(parentId);
	 a.setOrdinal(ordinal);
	 if (json.containsKey("id")) 
	    a.setId(json.getString("id"));
	 if (json.containsKey("label")) 
	    a.setLabel(json.getString("label"));
	 if (json.containsKey("startId")) 
	    a.setStartId(json.getString("startId"));
	 if (json.containsKey("endId")) 
	    a.setEndId(json.getString("endId"));
	 if (json.containsKey("confidence")) 
	    a.setConfidence(json.getInt("confidence"));
	 if (json.containsKey(Constants.COMMENT)) 
	    a.put(Constants.COMMENT, json.getString(Constants.COMMENT));
	 graph.addAnnotation(a);

	 // children
	 for (String childLayerId : graph.getLayer(layerId).getChildren().keySet())
	 {
	    if (json.containsKey(childLayerId))
	    {
	       jsonToAnnotations(graph, childLayerId, a.getId(), json.getJsonArray(childLayerId));
	    }
	 } // next top level layer
	 return a;
      }
      catch (JsonException x)
      {
	 throw new SerializationException(x);
      }
   } // end of jsonToGraph()

   /**
    * Determines which layers, if any, must be present in the graph that will be serialized.
    * <p>{@link GraphSerializer} method.
    * @return An empty array, as no particular layers are required.
    * @throws SerializationParametersMissingException If not all required parameters have a value.
    */
   public String[] getRequiredLayers() throws SerializationParametersMissingException
   {
      return new String[0];
   }

   /**
    * Determines the cardinality between graphs and serialized streams.
    * @return {@link nzilbb.ag.serialize.GraphSerializer.Cardinality}.NtoN as there is one
    * stream produced per graph. 
    */
   public Cardinality getCardinality()
   {
      return Cardinality.NToN;
   }

   /**
    * Serializes the given graph, generating one or more {@link NamedStream}s.
    * <p>Many data formats will only yield one stream (e.g. Transcriber transcript or Praat
    *  textgrid), however there are formats that use multiple files for the same transcript
    *  (e.g. XWaves, EmuR), which is why this method returns a list. There are formats that
    *  are capable of storing multiple transcripts in the same file (e.g. AGTK, Transana XML
    *  export), which is why this method accepts a list.
    * @param graphs The graphs to serialize.
    * @param layerIds The IDs of the layers to include, or null for all layers.
    * @param consumer The object receiving the streams.
    * @param warnings The object receiving warning messages.
    * @throws SerializerNotConfiguredException if the object has not been configured.
    */
   public void serialize(Spliterator<Graph> graphs, String[] layerIds, Consumer<NamedStream> consumer, Consumer<String> warnings, Consumer<SerializationException> errors) 
      throws SerializerNotConfiguredException
   {
      graphCount = graphs.getExactSizeIfKnown();
      graphs.forEachRemaining(graph -> {
            if (!getCancelling())
            {
               try
               {
                  File f = File.createTempFile(graph.getId(), ".json");
                  FileOutputStream out = new FileOutputStream(f);	 
                  PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "utf-8"));
                  
                  writer.println("{");
                  writer.println(keyValue(1, "id", graph.getId()) + ",");
                  
                  // offsetGranularity
                  if (graph.getOffsetGranularity() != null)
                  {
                     writer.println(keyValue(1, "offsetGranularity", graph.getOffsetGranularity()) + ",");
                  }
                  
                  // layers
                  Schema schema = graph.getSchema();
                  writer.println(indent(1) + "\"schema\":{");
                  if (schema.getCorpusLayerId() != null)
                     writer.println(keyValue(2, "corpusLayerId", schema.getCorpusLayerId()) + ",");
                  if (schema.getEpisodeLayerId() != null)
                     writer.println(keyValue(2, "episodeLayerId", schema.getEpisodeLayerId()) + ",");
                  if (schema.getParticipantLayerId() != null)
                     writer.println(keyValue(2, "participantLayerId", schema.getParticipantLayerId()) + ",");
                  if (schema.getTurnLayerId() != null)
                     writer.println(keyValue(2, "turnLayerId", schema.getTurnLayerId()) + ",");
                  if (schema.getUtteranceLayerId() != null)
                     writer.println(keyValue(2, "utteranceLayerId", schema.getUtteranceLayerId()) + ",");
                  if (schema.getWordLayerId() != null)
                     writer.println(keyValue(2, "wordLayerId", schema.getWordLayerId()) + ",");
                  serializeLayer(writer, 2, schema.getRoot());
                  writer.println();
                  writer.println(indent(1) + "},");
                  
                  // anchors
                  writer.println(indent(1) + "\"anchors\":{");
                  boolean firstAnchor = true;
                  Collection<Anchor> anchors = graph.getAnchors().values();
                  if (sortAnchors) anchors = graph.getAnchorsOrderedByStructure();
                  for (Anchor anchor : anchors)
                  {
                     if (firstAnchor)
                        firstAnchor = false;
                     else
                        writer.println(",");
                     serializeAnchor(writer, 2, anchor);
                  } // next anchor
                  writer.println();
                  writer.println(indent(1) + "},");
                  
                  // layers in predictable (alphabetical) order
                  for (String layerId : new TreeSet<String>(schema.getRoot().getChildren().keySet()))
                  {
                     serializeAnnotations(writer, 1, layerId, graph.getAnnotations(layerId));
                  }
                  
                  writer.println();
                  writer.println("}");
                  
                  // provide a stream from the buffer
                  writer.close();
                  TempFileInputStream in = new TempFileInputStream(f);
                  consumer.accept(new NamedStream(in, graph.getId() + ".json"));
               }
               catch(Exception exception)
               {
                  errors.accept(new SerializationException(exception));
               }
               consumedGraphCount++;
            }
         });// next graph
   }			
   
   /**
    * Recursively serializes the given layer's definition.
    * @param writer The writer to write to.
    * @param indent The current indent level.
    * @param layer The layer to serialize.
    */
   protected void serializeLayer(PrintWriter writer, int indent, Layer layer)
   {
      writer.println();
      writer.print(indent(indent) + q(layer.getId()) + ":{");
      writer.print(keyValue(0, "description", layer.getDescription()));
      if (layer.getParentId() != null)
      {
	 writer.print(", ");
	 writer.println(keyValue(0, "alignment", layer.getAlignment()) + ",");
	 writer.print(keyValue(indent+1, "peers", layer.getPeers()));
	 writer.print(", ");
	 writer.print(keyValue(0, "peersOverlap", layer.getPeersOverlap()));
	 writer.print(", ");
	 writer.print(keyValue(0, "parentIncludes", layer.getParentIncludes()));
	 writer.print(", ");
	 writer.print(keyValue(0, "saturated", layer.getSaturated()));
      }
      if (layer.getChildren().size() > 0)
      {
	 writer.println(",");
	 writer.print(indent(indent+1) + "\"children\":{");
	 // layers in predictable (alphabetical) order
	 boolean firstChild = true;
	 for (String childId : new TreeSet<String>(layer.getChildren().keySet()))
	 {
	    if (firstChild)
	       firstChild = false;
	    else
	       writer.print(",");
	    serializeLayer(writer, indent+2, layer.getChildren().get(childId));
	 } // next child
	 writer.println();
	 writer.print(indent(indent+1) + "}");
      }
      writer.print("}");
   } // end of serializeLayer()

   /**
    * Serializes the given anchor.
    * @param writer The writer to write to.
    * @param indent The current indent level.
    * @param anchor The anchor to serialize.
    */
   protected void serializeAnchor(PrintWriter writer, int indent, Anchor anchor)
   {
      writer.print(indent(indent) + q(anchor.getId()) + ":\t{");
      writer.print(keyValue(0, "offset", anchor.getOffset()));
      if (anchor.getConfidence() != null)
      {
	 writer.print(",\t");
	 writer.print(keyValue(0, "confidence", anchor.getConfidence()));
      }
      if (anchor.containsKey("comment"))
      {
	 writer.print(",\t");
	 writer.print(keyValue(0, "comment", anchor.get("comment").toString()));
      }
      writer.print("}");
   } // end of serializeAnchor()

   /**
    * Serializes the given annotation.
    * @param writer The writer to write to.
    * @param indent The current indent level.
    * @param layerId The ID of the annotations.
    * @param annotations A list of annotations on the same layer to serialize.
    */
   protected void serializeAnnotations(PrintWriter writer, int indent, String layerId, Collection<Annotation> annotations)
   {
      if (annotations.size() > 0)
      {
	 writer.print(indent(indent) + q(layerId) + ":[");
	 boolean firstLayer = true;
	 for (Annotation child : annotations) 
	 {
	    if (firstLayer)
	       firstLayer = false;
	    else
	       writer.print(",");
	    writer.println();
	    writer.print(indent(indent+1) + "{");
	    serializeAnnotation(writer, indent+2, child);
	    writer.print("}");
	 } // next child
	 writer.print("]");
      } // there really are children
   }

   /**
    * Serializes the given annotation.
    * @param writer The writer to write to.
    * @param indent The current indent level.
    * @param annotation The annotation to serialize.
    */
   protected void serializeAnnotation(PrintWriter writer, int indent, Annotation annotation)
   {
      writer.print(keyValue(0, "id", annotation.getId()));
      writer.print(",\t");
      writer.print(keyValue(0, "label", annotation.getLabel()));
      writer.print(",\t");
      writer.print(keyValue(0, "startId", annotation.getStartId()));
      writer.print(",\t");
      writer.print(keyValue(0, "endId", annotation.getEndId()));
      if (annotation.getConfidence() != null)
      {
	 writer.print(",\t");
	 writer.print(keyValue(0, "confidence", annotation.getConfidence()));
      }
      if (annotation.containsKey("comment"))
      {
	 writer.print(",\t");
	 writer.print(keyValue(0, "comment", annotation.get("comment").toString()));
      }
      LinkedHashMap<String,SortedSet<Annotation>> childLayers = annotation.getAnnotations();
      // layers in predictable (alphabetical) order
      for (String layerId : new TreeSet<String>(childLayers.keySet()))
      {
	 SortedSet<Annotation> children = childLayers.get(layerId);
	 if (children.size() > 0)
	 {
	    writer.println(",");
	    serializeAnnotations(writer, indent, layerId, children);
	 }
      } // next layer
   } // end of serializeAnnotation()
   
   /**
    * Expresses a key and simple string value as JSON.
    * @param indent The current indent level.
    * @param key The key to serialize.
    * @param value The value to serialize.
    * @return A string of the form {indent}"{key}":"{value}",
    */
   protected String keyValue(int indent, String key, String value)
   {
      StringBuffer s = new StringBuffer();
      for (int i = 0; i < indent; i++) s.append(indenter);
      s.append(q(key));
      s.append(":");
      s.append(q(value));
      return s.toString();
   } // end of keyValue()

   /**
    * Expresses a key and simple boolean value as JSON.
    * @param indent The current indent level.
    * @param key The key to serialize.
    * @param value The value to serialize.
    * @return A string of the form {indent}{key} : {value},
    */
   protected String keyValue(int indent, String key, boolean value)
   {
      StringBuffer s = new StringBuffer();
      for (int i = 0; i < indent; i++) s.append(indenter);
      s.append(q(key));
      s.append(":");
      s.append(value);
      return s.toString();
   } // end of keyValue()

   /**
    * Expresses a key and simple integer value as JSON.
    * @param indent The current indent level.
    * @param key The key to serialize.
    * @param value The value to serialize.
    * @return A string of the form {indent}{key} : {value},
    */
   protected String keyValue(int indent, String key, int value)
   {
      StringBuffer s = new StringBuffer();
      for (int i = 0; i < indent; i++) s.append(indenter);
      s.append(q(key));
      s.append(":");
      s.append(value);
      return s.toString();
   } // end of keyValue()

   /**
    * Expresses a key and Double object value as JSON.
    * @param indent The current indent level.
    * @param key The key to serialize.
    * @param value The value to serialize.
    * @return A string of the form {indent}{key} : {value},
    */
   protected String keyValue(int indent, String key, Double value)
   {
      StringBuffer s = new StringBuffer();
      for (int i = 0; i < indent; i++) s.append(indenter);
      s.append(q(key));
      s.append(":");
      if (value != null) 
	 s.append(fmtOffset.format(value));
      else
	 s.append("null");
      return s.toString();
   } // end of keyValue()
   
   /**
    * Write indent characters to the given level.
    * @param indent The level to indent to.
    * @return A string with the indent character repeated the indent level number of times.
    */
   protected String indent(int indent)
   {
      StringBuffer s = new StringBuffer();
      for (int i = 0; i < indent; i++) s.append(indenter);
      return s.toString();
   } // end of indent()
   
   /**
    * Escapes the given string for expression as a JSON string, and wraps it in quotes.
    * @param s The string to quote.
    * @return The given string, in quotes and escaped for JSON.
    */
   protected String q(String s)
   {
      if (s == null) return "null";
      return "\"" 
	 + s.replaceAll("\\\\", "\\\\")
	 .replaceAll("\\\"", "\\\\\"")
	 .replaceAll("\t", "\\t")
	 .replaceAll("\b", "\\b")
	 .replaceAll("\r", "\\r")
	 .replaceAll("\n", "\\n")
	 .replaceAll("\f", "\\f")
	 + "\"";
   } // end of q()

} // end of class JSONSerialization
