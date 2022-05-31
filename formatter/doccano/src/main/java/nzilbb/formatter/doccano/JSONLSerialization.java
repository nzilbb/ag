//
// Copyright 2022 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.formatter.doccano;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import nzilbb.ag.*;
import nzilbb.ag.serialize.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.util.AnnotationComparatorByAnchor;
import nzilbb.ag.util.ConventionTransformer;
import nzilbb.ag.util.OrthographyClumper;
import nzilbb.ag.util.SimpleTokenizer;
import nzilbb.ag.util.SpanningConventionTransformer;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.util.IO;
import nzilbb.util.TempFileInputStream;
import nzilbb.util.Timers;

/**
 * Serializer/deserializer for JSONL files compatible with
 * <a href="https://doccano.github.io/doccano/">doccano</a>.
 * Doccano supports text-only (i.e. character offset) annotation, but also supports
 * importing arbitrary meta-data which is passed through from import to export, so when
 * the graph to serialize has temporal offsets
 * (i.e. {@link Graph#getOffsetUnits()} == {@link Constants#UNIT_SECONDS})
 * this meta-data is used to retain a mapping of character offsets to seconds.
 * <p> The structure used is:
 * <ul>
 *  <li> each text is a transcript </li>
 *  <li> each line is an utterance </li>
 *  <li> the first utterance of each turn starts with the participant label, formatted:
 *       <q>${participant}:\t</q></li>
 *  <li> span/phrase layers are tagged </li>
 *  <li> meta-data: <dl>
 *    <dt> transcript </dt><dd> graph ID </dd>
 *    <dt> anchors </dt><dd> object keyed on layer ID, each value being array of couples,
 *         one element for each annotation, each couple being the start time and end
 *         time of the annotation. <q>anchors</q> will contain at least the
 *         <q>utterance</q> offsets, one annotation for each line in the <q>text</q> </dd>  
 *   </dl></li>
 * </ul>
 * <p> <em>NB</em> If a graph is serialized with annotations, edited with Doccano, and
 * then exported for deserialization, the annotations included in the serialization will
 * be ignored during deserialization.
 * <p> This is because serialized annotations have their original anchor offsets included,
 * but we can't guarantee that these annotations haven't been edited in Doccano, and so
 * the saved offsets may be no longer valid.
 * <p> For this reason, Doccano can currently only be used to <em>add</em> annotations on
 * new layers, not for editing existing layers.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class JSONLSerialization implements GraphDeserializer, GraphSerializer {

  protected Vector<String> warnings;
   
  /**
   * Participant information layer.
   * @see #getParticipantLayer()
   * @see #setParticipantLayer(Layer)
   */
  protected Layer participantLayer;
  /**
   * Getter for {@link #participantLayer}: Participant information layer.
   * @return Participant information layer.
   */
  public Layer getParticipantLayer() { return participantLayer; }
  /**
   * Setter for {@link #participantLayer}: Participant information layer.
   * @param newParticipantLayer Participant information layer.
   */
  public JSONLSerialization setParticipantLayer(Layer newParticipantLayer) { participantLayer = newParticipantLayer; return this; }

  /**
   * Turn layer.
   * @see #getTurnLayer()
   * @see #setTurnLayer(Layer)
   */
  protected Layer turnLayer;
  /**
   * Getter for {@link #turnLayer}: Turn layer.
   * @return Turn layer.
   */
  public Layer getTurnLayer() { return turnLayer; }
  /**
   * Setter for {@link #turnLayer}: Turn layer.
   * @param newTurnLayer Turn layer.
   */
  public JSONLSerialization setTurnLayer(Layer newTurnLayer) { turnLayer = newTurnLayer; return this; }

  /**
   * Utterance layer.
   * @see #getUtteranceLayer()
   * @see #setUtteranceLayer(Layer)
   */
  protected Layer utteranceLayer;
  /**
   * Getter for {@link #utteranceLayer}: Utterance layer.
   * @return Utterance layer.
   */
  public Layer getUtteranceLayer() { return utteranceLayer; }
  /**
   * Setter for {@link #utteranceLayer}: Utterance layer.
   * @param newUtteranceLayer Utterance layer.
   */
  public JSONLSerialization setUtteranceLayer(Layer newUtteranceLayer) { utteranceLayer = newUtteranceLayer; return this; }

  /**
   * Word token layer.
   * @see #getWordLayer()
   * @see #setWordLayer(Layer)
   */
  protected Layer wordLayer;
  /**
   * Getter for {@link #wordLayer}: Word token layer.
   * @return Word token layer.
   */
  public Layer getWordLayer() { return wordLayer; }
  /**
   * Setter for {@link #wordLayer}: Word token layer.
   * @param newWordLayer Word token layer.
   */
  public JSONLSerialization setWordLayer(Layer newWordLayer) { wordLayer = newWordLayer; return this; }

  /**
   * Parameters and mappings for the next deserialization.
   * @see #getParameters()
   * @see #setParameters(ParameterSet)
   */
  protected ParameterSet parameters;
  /**
   * Getter for {@link #parameters}: Parameters and mappings for the next deserialization.
   * @return Parameters and mappings for the next deserialization.
   */
  public ParameterSet getParameters() { return parameters; }
  /**
   * Setter for {@link #parameters}: Sets parameters for a given deserialization
   * operation, after loading the serialized form of the graph. This might include
   * mappings from format-specific objects like tiers to graph layers, etc. 
   * @param newParameters The configuration for a given deserialization operation.
   * @throws SerializationParametersMissingException If not all required parameters have a value.
   */
  public void setParameters(ParameterSet newParameters)
    throws SerializationParametersMissingException {
    parameters = newParameters;
  }
  
  /**
   * The schema for import.
   * @see #getSchema()
   * @see #setSchema(Schema)
   */
  protected Schema schema;
  /**
   * Getter for {@link #schema}: The schema for import.
   * @return The schema for import.
   */
  public Schema getSchema() { return schema; }
  /**
   * Setter for {@link #schema}: The schema for import.
   * @param newSchema The schema for import.
   */
  public JSONLSerialization setSchema(Schema newSchema) { schema = newSchema; return this; }

  /**
   * Utterance tokenizer.  The default is {@link SimpleTokenizer}.
   * @see #getTokenizer()
   * @see #setTokenizer(GraphTransformer)
   */
  protected GraphTransformer tokenizer;
  /**
   * Getter for {@link #tokenizer}: Utterance tokenizer.
   * @return Utterance tokenizer.
   */
  public GraphTransformer getTokenizer() { return tokenizer; }
  /**
   * Setter for {@link #tokenizer}: Utterance tokenizer.
   * @param newTokenizer Utterance tokenizer.
   */
  public JSONLSerialization setTokenizer(GraphTransformer newTokenizer) { tokenizer = newTokenizer; return this; }

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
  public JSONLSerialization setCancelling(boolean newCancelling) { cancelling = newCancelling; return this; }
  /**
   * Cancel the serialization in course (if any).
   */
  public void cancel() {
    setCancelling(true);
  }

  // Methods:
   
  /**
   * Default constructor.
   */
  public JSONLSerialization() {
  } // end of constructor
   
  // IStreamDeserializer methods:
   
  /**
   * Returns the deserializer's descriptor
   * @return The deserializer's descriptor
   */
  public SerializationDescriptor getDescriptor() {
    return new SerializationDescriptor(
      "Doccano JSONL Dataset", getClass().getPackage().getImplementationVersion(),
      "application/x-jsonlines", ".jsonl", "1.0.0",
      getClass().getResource("icon.png"));
  }
  
  /**
   * Sets parameters for deserializer as a whole.  This might include database connection
   * parameters, locations of supporting files, etc. 
   * <p>When the deserializer is installed, this method should be invoked with an empty parameter
   *  set, to discover what (if any) general configuration is required. If parameters are
   *  returned, and user interaction is possible, then the user may be presented with an
   *  interface for setting/confirming these parameters.  
   * @param configuration The configuration for the deserializer. 
   * @param schema The layer schema, definining layers and the way they interrelate.
   * @return A list of configuration parameters (still) must be set before 
   * {@link GraphDeserializer#setParameters(ParameterSet)} can be invoked. If this is an empty 
   * list,
   * {@link GraphDeserializer#setParameters(ParameterSet)} can be invoked. If it's not an
   * empty list, this method must be invoked again with the returned parameters' values
   * set. 
   */
  public ParameterSet configure(ParameterSet configuration, Schema schema) {
    setTurnLayer(schema.getTurnLayer());
    setUtteranceLayer(schema.getUtteranceLayer());
    setWordLayer(schema.getWordLayer());
    setParticipantLayer(schema.getParticipantLayer());

    boolean firstTime = configuration.size() == 0;

    // set any values that have been passed in
    for (Parameter p : configuration.values()) try { p.apply(this); } catch(Exception x) {}

    // create a list of layers we need and possible matching layer names
    LinkedHashMap<Parameter,List<String>> layerToPossibilities = new LinkedHashMap<Parameter,List<String>>();
    HashMap<String,LinkedHashMap<String,Layer>> layerToCandidates = new HashMap<String,LinkedHashMap<String,Layer>>();

    // do we need to ask for participant/turn/utterance/word layers?
    LinkedHashMap<String,Layer> possibleParticipantLayers = new LinkedHashMap<String,Layer>();
    LinkedHashMap<String,Layer> possibleTurnLayers = new LinkedHashMap<String,Layer>();
    LinkedHashMap<String,Layer> possibleTurnChildLayers = new LinkedHashMap<String,Layer>();
    LinkedHashMap<String,Layer> wordTagLayers = new LinkedHashMap<String,Layer>();
    LinkedHashMap<String,Layer> participantTagLayers = new LinkedHashMap<String,Layer>();
    if (getParticipantLayer() == null || getTurnLayer() == null 
        || getUtteranceLayer() == null || getWordLayer() == null) {
      for (Layer top : schema.getRoot().getChildren().values()) {
        if (top.getAlignment() == Constants.ALIGNMENT_NONE) {
          if (top.getChildren().size() == 0) { // unaligned childless children of graph
            participantTagLayers.put(top.getId(), top);
          } else { // unaligned children of graph, with children of their own
            possibleParticipantLayers.put(top.getId(), top);
            for (Layer turn : top.getChildren().values()) {
              if (turn.getAlignment() == Constants.ALIGNMENT_INTERVAL
                  && turn.getChildren().size() > 0) {
                // aligned children of who with their own children
                possibleTurnLayers.put(turn.getId(), turn);
                for (Layer turnChild : turn.getChildren().values()) {
                  if (turnChild.getAlignment() == Constants.ALIGNMENT_INTERVAL) {
                    // aligned children of turn
                    possibleTurnChildLayers.put(turnChild.getId(), turnChild);
                    for (Layer tag : turnChild.getChildren().values()) {
                      if (tag.getAlignment() == Constants.ALIGNMENT_NONE) {
                        // unaligned children of word
                        wordTagLayers.put(tag.getId(), tag);
                      }
                    } // next possible word tag layer
                  }
                } // next possible turn child layer
              }
            } // next possible turn layer
          } // with children
        } // unaligned
      } // next possible participant layer
    } else {
      for (Layer turnChild : getTurnLayer().getChildren().values()) {
        if (turnChild.getAlignment() == Constants.ALIGNMENT_INTERVAL) {
          possibleTurnChildLayers.put(turnChild.getId(), turnChild);
        }
      } // next possible word tag layer
      for (Layer tag : getParticipantLayer().getChildren().values()) {
        if (tag.getAlignment() == Constants.ALIGNMENT_NONE
            && tag.getChildren().size() == 0) {
          participantTagLayers.put(tag.getId(), tag);
        }
      } // next possible participant tag layer
      for (Layer tag : getWordLayer().getChildren().values()) {
        if (tag.getAlignment() == Constants.ALIGNMENT_NONE
            && tag.getChildren().size() == 0) {
          wordTagLayers.put(tag.getId(), tag);
        }
      } // next possible word tag layer
    }
    if (getParticipantLayer() == null) {
      layerToPossibilities.put(
        new Parameter("participantLayer", Layer.class, "Participant layer", 
                      "Layer for speaker/participant identification", true), 
        Arrays.asList("participant","participants","who","speaker","speakers"));
      layerToCandidates.put("participantLayer", possibleParticipantLayers);
    }
    if (getTurnLayer() == null) {
      layerToPossibilities.put(
        new Parameter("turnLayer", Layer.class, "Turn layer", "Layer for speaker turns", true),
        Arrays.asList("turn","turns"));
      layerToCandidates.put("turnLayer", possibleTurnLayers);
    }
    if (getUtteranceLayer() == null) {
      layerToPossibilities.put(
        new Parameter("utteranceLayer", Layer.class, "Utterance layer", 
                      "Layer for speaker utterances", true), 
        Arrays.asList("utterance","utterances","line","lines"));
      layerToCandidates.put("utteranceLayer", possibleTurnChildLayers);
    }
    if (getWordLayer() == null) {
      layerToPossibilities.put(
        new Parameter("wordLayer", Layer.class, "Word layer", 
                      "Layer for individual word tokens", true), 
        Arrays.asList("transcript","word","words","w"));
      layerToCandidates.put("wordLayer", possibleTurnChildLayers);
    }

    LinkedHashMap<String,Layer> topLevelLayers = new LinkedHashMap<String,Layer>();
    for (Layer top : schema.getRoot().getChildren().values()) {
      if (top.getAlignment() == Constants.ALIGNMENT_INTERVAL) { // aligned children of graph
        topLevelLayers.put(top.getId(), top);
      }
    } // next top level layer

    LinkedHashMap<String,Layer> graphTagLayers = new LinkedHashMap<String,Layer>();
    for (Layer top : schema.getRoot().getChildren().values()) {
      if (top.getAlignment() == Constants.ALIGNMENT_NONE
          && top.getChildren().size() == 0) { // unaligned childless children of graph
        graphTagLayers.put(top.getId(), top);
      }
    } // next top level layer
    graphTagLayers.remove("corpus");
    graphTagLayers.remove("transcript_type");

    // add parameters that aren't in the configuration yet, and set possibile/default values
    for (Parameter p : layerToPossibilities.keySet()) {
      List<String> possibleNames = layerToPossibilities.get(p);
      LinkedHashMap<String,Layer> candidateLayers = layerToCandidates.get(p.getName());
      if (configuration.containsKey(p.getName())) {
        p = configuration.get(p.getName());
      } else {
        configuration.addParameter(p);
      }
      if (p.getValue() == null && firstTime) {
        p.setValue(Utility.FindLayerById(candidateLayers, possibleNames));
      }
      p.setPossibleValues(candidateLayers.values());
    }

    return configuration;
  }

  // GraphSerializer methods

  /**
   * Determines which layers, if any, must be present in the graph that will be serialized.
   * @return A list of IDs of layers that must be present in the graph that will be serialized.
   * @throws SerializationParametersMissingException If not all required parameters have a value.
   */
  public String[] getRequiredLayers() throws SerializationParametersMissingException {
    if (getTurnLayer() == null)
      throw new SerializationParametersMissingException("No turn layer specified");
    if (getUtteranceLayer() == null)
      throw new SerializationParametersMissingException("No utterance layer specified");
    if (getWordLayer() == null)
      throw new SerializationParametersMissingException("No word layer specified");
    
    Vector<String> requiredLayers = new Vector<String>();
    if (getParticipantLayer() != null) requiredLayers.add(getParticipantLayer().getId());
    requiredLayers.add(getTurnLayer().getId());
    requiredLayers.add(getUtteranceLayer().getId());
    requiredLayers.add(getWordLayer().getId());
    return requiredLayers.toArray(new String[0]);
  } // getRequiredLayers()

  /**
   * Determines the cardinality between graphs and serialized streams.
   * <p>The cardinality of this deserializer is NToOne as there is one stream produced
   * regardless of how many graphs are serialized.
   * @return {@link nzilbb.ag.serialize.GraphSerializer#Cardinality}.NToOne.
   */
  public Cardinality getCardinality() {
    return Cardinality.NToOne;
  }

  /**
   * Serializes the given series of graphs, generating one or more {@link NamedStream}s.
   * <p>Many data formats will only yield one stream per graph (e.g. Transcriber
   * transcript or Praat textgrid), however there are formats that use multiple files for
   * the same transcript (e.g. XWaves, EmuR), and others still that will produce one
   * stream from many Graphs (e.g. CSV).
   * <p>The method is synchronous in the sense that it should not return until all graphs
   * have been serialized.
   * @param graphs The graphs to serialize.
   * @param layerIds The IDs of the layers to include, or null for all layers.
   * @param consumer The consumer receiving the streams.
   * @param warnings A consumer for (non-fatal) warning messages.
   * @param errors A consumer for (fatal) error messages.
   * @throws SerializerNotConfiguredException if the object has not been configured.
   */
  public void serialize(
    Spliterator<Graph> graphs, String[] layerIds, Consumer<NamedStream> consumer,
    Consumer<String> warnings, Consumer<SerializationException> errors) 
    throws SerializerNotConfiguredException {

    graphCount = graphs.getExactSizeIfKnown();
    try {
      final StringBuffer fileName = new StringBuffer();
      final File jsonlFile = File.createTempFile("doccano.",".jsonl");

      graphs.forEachRemaining(graph -> {
          if (getCancelling()) return;
          if (fileName.length() == 0) {
            fileName.append(graph.getId().replaceAll("\\.[^.]+$",""));
          } else if (!fileName.toString().endsWith("-etc")) {
            fileName.append("-etc");
            
            try {
              // new line between each graph
              FileOutputStream out = new FileOutputStream(jsonlFile, true);
              out.write('\n');
              out.close();
            } catch(IOException exception) {}
          }
          try { // serialize graph
            serializeGraph(graph, layerIds, jsonlFile);
            consumedGraphCount++;
          } catch(Exception exception) {
            errors.accept(new SerializationException(exception));
          }
        }); // next graph

      // pass on streams
      consumer.accept(
       new NamedStream(new TempFileInputStream(jsonlFile),
                       fileName+".jsonl", "application/x-jsonlines"));     
    } catch(Exception exception) {
      errors.accept(new SerializationException(exception));
    }
  }

  /**
   * Serializes the given graph, generating a {@link NamedStream}.
   * @param graph The graph to serialize.
   * @param layerIds The IDs of the layers to include in the serializaton.
   * @param jsonlFile The file to serialize to. This method will append one JSON-encoded
   * line to this file, followed by a new-line.
   * @return A named stream that contains the PDF. 
   * @throws SerializationException if errors occur during deserialization.
   */
  protected void serializeGraph(Graph graph, String[] layerIds, File jsonlFile)
    throws SerializationException {
    SerializationException errors = null;
    Schema schema = graph.getSchema();
    try {      
      FileOutputStream out = new FileOutputStream(jsonlFile, true);
      JsonGenerator json = Json.createGenerator(out);
      json.writeStartObject();
      json.write("transcript", graph.getId());
      
      // order turns by anchor
      TreeSet<Annotation> turnsByAnchor
        = new TreeSet<Annotation>(new AnnotationComparatorByAnchor());
      for (Annotation t : graph.all(getTurnLayer().getId())) turnsByAnchor.add(t);
      // keep a list of utterances in the order they appear in the text
      Vector<Annotation> utterances = new Vector<Annotation>();
      // extract other annotations
      LinkedHashMap<String, List<Annotation>> annotations
        = new LinkedHashMap<String, List<Annotation>>();
      for (String layerId : layerIds) {
        if (layerId.equals(schema.getParticipantLayerId())) continue;
        if (layerId.equals(schema.getTurnLayerId())) continue;
        if (layerId.equals(schema.getUtteranceLayerId())) continue;
        if (layerId.equals(schema.getWordLayerId())) continue;
        annotations.put(layerId, Arrays.stream(graph.all(layerId))
                        .filter(a->a.getAnchored())
                        .collect(Collectors.toList()));
      } // next layer 
      
      // write the text
      StringBuilder text = new StringBuilder();
      String delimiter = "";
      for (Annotation turn : turnsByAnchor) {
        delimiter += turn.getLabel() + ":\t"; // first delimiter is participant ID
        for (Annotation utterance : turn.all("utterance")) {
          utterances.add(utterance);
          for (Annotation token : utterance.all(getWordLayer().getId())) {
            
            text.append(delimiter);
            
            // check for annotations that start before/during this token
            for (String layerId : annotations.keySet()) {
              for (Annotation a : annotations.get(layerId)) {
                if (!a.containsKey("@startChar")
                    && a.getStart().getOffset() < token.getEnd().getOffset()) {
                  a.put("@startChar", text.length());
                }
              } // next annotation
            } // next layer
            
            // add the token
            delimiter = " "; // subsequent delimiters are spaces
            text.append(token);
            
            // check for annotations that end after/during this token
            for (String layerId : annotations.keySet()) {
              for (Annotation a : annotations.get(layerId)) {
                if (!a.containsKey("@endChar")
                    && a.getEnd().getOffset() <= token.getEnd().getOffset()) {
                  a.put("@endChar", text.length());
                }
              } // next annotation
            } // next layer            
            
          } // next token
          delimiter = "\n"; // next delimiter is new line
        } // next utterance
      } // next turn
      json.write("text", text.toString());
      // finish off any unfinished annotations
      for (String layerId : annotations.keySet()) {
        for (Annotation a : annotations.get(layerId)) {
          if (!a.containsKey("@endChar")) {
            a.put("@endChar", text.length());
          }
        } // next annotation
      } // next layer
      
      // write the labels
      json.writeStartArray("label");
      for (String layerId : annotations.keySet()) {
        for (Annotation a : annotations.get(layerId)) {
          if (a.containsKey("@startChar") && a.containsKey("@endChar")) {
            json.writeStartArray();
            json.write((Integer)a.get("@startChar"));
            json.write((Integer)a.get("@endChar"));
            String label = layerId + ":" + a.getLabel();
            json.write(label);
            json.writeEnd(); // annotation triple
          }
        } // next annotation
      } // next layer
      json.writeEnd(); // label array
      
      // write anchors
      json.writeStartObject("anchors");
      json.write("offsetUnits", graph.getOffsetUnits());

      // utterance anchors
      json.writeStartArray(schema.getUtteranceLayerId());
      for (Annotation utterance : utterances) {        
        json.writeStartArray(); // start offset couple
        json.write(utterance.getStart().getOffset());
        json.write(utterance.getEnd().getOffset());
        json.writeEnd();  // end offset couple
      } // next utterance
      json.writeEnd(); // utterance array

      // annotation anchors
      for (String layerId : annotations.keySet()) {
        json.writeStartArray(layerId);
        for (Annotation a : annotations.get(layerId)) {
          json.writeStartArray(); // start offset couple
          json.write(a.getStart().getOffset());
          json.write(a.getEnd().getOffset());
          json.writeEnd();  // end offset couple
        } // next annotation
        json.writeEnd(); // layer array
      } // next layer
      json.writeEnd(); // anchors
      
      json.writeEnd();
      json.close();      
    } catch(Exception exception) {
      errors = new SerializationException();
      errors.initCause(exception);
      errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
      throw errors;
    }      
  }

  // GraphDeserializer methods:

  private File jsonlFile;

  /**
   * Loads the serialized form of the graphs, using the given set of named streams.
   * @param streams A list of named streams that contain all the transcription/annotation
   * data required, and possibly (a) stream(s) for the media annotated. 
   * @param schema The layer schema, definining layers and the way they interrelate.
   * @return A list of parameters that require setting before
   * {@link GraphDeserializer#deserialize()} can be invoked. This may be an empty list,
   * and may include parameters with the value already set to a workable default. If there
   * are parameters, and user interaction is possible, then the user may be presented with
   * an interface for setting/confirming these parameters, before they are then passed to
   * {@link GraphDeserializer#setParameters(ParameterSet)}.
   * @throws SerializationException If the graphs could not be loaded.
   * @throws IOException On IO error.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public ParameterSet load(NamedStream[] streams, Schema schema)
    throws SerializationException, IOException {
    warnings = new Vector<String>();
    // if there are errors, accumulate as many as we can before throwing SerializationException
    SerializationException errors = null;

    // take the first stream, ignore all others.
    NamedStream jsonl = Utility.FindSingleStream(streams, ".jsonl", "application/x-jsonlines");
    if (jsonl == null) throw new SerializationException("No JSONL document stream found");
      
    setSchema(schema);

    // create a list of layers we need and possible matching layer names
    LinkedHashMap<Parameter,List<String>> layerToPossibilities
      = new LinkedHashMap<Parameter,List<String>>();
    HashMap<String,LinkedHashMap<String,Layer>> layerToCandidates
      = new HashMap<String,LinkedHashMap<String,Layer>>();	 

    // the stream can contain multiple graphs and may be quite large, so save it to a
    // temporary file
    jsonlFile = File.createTempFile(
      IO.WithoutExtension(jsonl.getName()), "." + IO.Extension(jsonl.getName()));
    IO.SaveInputStreamToFile(jsonl.getStream(), jsonlFile);    

    // now go looking for tags to match to annotation layers...

    LinkedHashMap<String,Layer> tagLayers = new LinkedHashMap<String,Layer>();
    // span layers
    for (Layer layer : schema.getRoot().getChildren().values()) {
      if (layer.getAlignment() == Constants.ALIGNMENT_INTERVAL) {
        tagLayers.put(layer.getId(), layer);
      }
    }
    // phrase layers
    for (Layer layer : schema.getTurnLayer().getChildren().values()) {
      if (layer.getAlignment() == Constants.ALIGNMENT_INTERVAL) {
        tagLayers.put(layer.getId(), layer);
      }
    }
    // word tag layers
    for (Layer layer : schema.getWordLayer().getChildren().values()) {
      if (layer.getAlignment() == Constants.ALIGNMENT_NONE) {
        tagLayers.put(layer.getId(), layer);
      }
    }
    
    // labels are formatted "${layerId}:${label}"
    MessageFormat labelFormat = new MessageFormat("{0}:{1}");

    // each line is a transcript document
    BufferedReader reader = new BufferedReader(
      new InputStreamReader(new FileInputStream(jsonlFile), "UTF-8"));
    String document = reader.readLine();
    int l = 1;
    while (document != null) { // for each document
      // parse JSON
      JsonObject json = Json.createReader(new StringReader(document)).readObject();

      // we must have a "transcript" attribute to determine the ID of the resulting graph
      if (!json.containsKey("transcript")) {
        if (errors == null) errors = new SerializationException();
        errors.addError(SerializationException.ErrorType.InvalidDocument,
                        "Line "+l+": no transcript attribute to define transcript ID");
        continue;
      }

      // the anchors object is keyed on layerId, and contains arrays of couples with the
      // original offsets of the annotions when they were serialized
      JsonObject anchors = json.containsKey("anchors")?json.getJsonObject("anchors"):null;

      // look for labels to map to layers
      if (json.containsKey("label")) {
        JsonArray labelArray = json.getJsonArray("label");
        Set<String> anchoredLayers = anchors != null?anchors.keySet():new HashSet<String>();
        
        // each element is a triple
        for (JsonValue element : labelArray) {
          JsonArray triple = (JsonArray)element;
          // third element is the label
          String label = triple.getString(2);
          try {
            Object[] parts = labelFormat.parse(label);
            String layerId = ((String)parts[0]).trim();
            if (layerId.length() > 0 // there is a layerId
                // exclude layers that have anchors because they were serialized and we
                // can't guarantee they haven't changed
                && !anchoredLayers.contains(layerId)
                // we haven't seen it before
                && !layerToCandidates.containsKey(layerId)) { 
              layerToPossibilities.put(
                new Parameter(layerId, Layer.class, layerId), 
                new Vector<String>() {{ add(layerId); }});
              layerToCandidates.put(layerId, tagLayers);
            }
          } catch (ParseException x) {
            warnings.add("Line "+l+": Label cannot be interpreted as layer mapping: " + label);
          }
        }
      } // has "label" array

      document = reader.readLine();
      l++;
    } // next line
    reader.close();
    if (errors != null) throw errors;
	 
    ParameterSet parameters = new ParameterSet();
    // add parameters that aren't in the configuration yet, and set possibile/default values
    for (Parameter p : layerToPossibilities.keySet()) {
      List<String> possibleNames = layerToPossibilities.get(p);
      LinkedHashMap<String,Layer> candidateLayers = layerToCandidates.get(p.getName());
      parameters.addParameter(p);
      p.setValue(Utility.FindLayerById(candidateLayers, possibleNames));
      p.setPossibleValues(candidateLayers.values());
    }
    return parameters;
  }

  /**
   * Deserializes the serialized data, generating one or more {@link Graph}s.
   * <p>Many data formats will only yield one graph (e.g. Transcriber
   * transcript or Praat textgrid), however there are formats that
   * are capable of storing multiple transcripts in the same file
   * (e.g. AGTK, Transana XML export), which is why this method
   * returns a list.
   * @return A list of valid (if incomplete) {@link Graph}s. 
   * @throws SerializerNotConfiguredException if the object has not been configured.
   * @throws SerializationParametersMissingException if the parameters for this
   * particular graph have not been set. 
   * @throws SerializationException if errors occur during deserialization.
   */
  public Graph[] deserialize()
    throws SerializerNotConfiguredException, SerializationParametersMissingException,
    SerializationException {
    Vector<Graph> graphs = new Vector<Graph>();
      
    if (participantLayer == null) {
      throw new SerializerNotConfiguredException("Participant layer not set");
    }
    if (turnLayer == null) {
      throw new SerializerNotConfiguredException("Turn layer not set");
    }
    if (utteranceLayer == null) {
      throw new SerializerNotConfiguredException("Utterance layer not set");
    }
    if (wordLayer == null) {
      throw new SerializerNotConfiguredException("Word layer not set");
    }
    if (schema == null) {
      throw new SerializerNotConfiguredException("Layer schema not set");
    }
    if (jsonlFile == null) {
      throw new SerializerNotConfiguredException("No stream to deserialize");
    }
    if (!jsonlFile.exists()) {
      throw new SerializerNotConfiguredException(
        "JSONL stream has been removed: " + jsonlFile.getPath());
    }

    // if there are errors, accumulate as many as we can before throwing SerializationException
    SerializationException errors = null;
      
    try {
      // labels are formatted "${layerId}:${label}"
      MessageFormat labelFormat = new MessageFormat("{0}:{1}");
      
      // each line is a transcript document
      BufferedReader reader = new BufferedReader(
        new InputStreamReader(new FileInputStream(jsonlFile), "UTF-8"));
      String document = reader.readLine();
      int d = 1;
      while (document != null) { // for each document
        // parse JSON
        JsonObject json = Json.createReader(new StringReader(document)).readObject();
        
        Graph graph = new Graph();
        graphs.add(graph);
        graph.setId(json.getString("transcript"));
        // anchor offsets are characters for now
        graph.setOffsetUnits(Constants.UNIT_CHARACTERS);
        
        // creat the 0 anchor to prevent graph tagging from creating one with no confidence
        Anchor firstAnchor = graph.getOrCreateAnchorAt(0.0, Constants.CONFIDENCE_MANUAL);
        Anchor lastAnchor = firstAnchor;
        
        // add layers to the graph
        graph.addLayer((Layer)participantLayer.clone());
        graph.getSchema().setParticipantLayerId(participantLayer.getId());
        graph.addLayer((Layer)turnLayer.clone());
        graph.getSchema().setTurnLayerId(turnLayer.getId());
        graph.addLayer((Layer)utteranceLayer.clone());
        graph.getSchema().setUtteranceLayerId(utteranceLayer.getId());
        graph.addLayer((Layer)wordLayer.clone());
        graph.getSchema().setWordLayerId(wordLayer.getId());
        if (parameters != null) {
          for (Parameter p : parameters.values()) {
            Layer layer = (Layer)p.getValue();
            if (layer != null && graph.getLayer(layer.getId()) == null) {
              // haven't added this layer yet
              graph.addLayer((Layer)layer.clone());
            }
          }
        }
        
        // split text into lines
        String[] lines = json.getString("text").split("\n");
        
        HashMap<String,Annotation> participants = new HashMap<String,Annotation>();
        Annotation participant = new Annotation(
          null,
          IO.WithoutExtension(graph.getId()),
          schema.getParticipantLayerId());
        participant.setConfidence(Constants.CONFIDENCE_AUTOMATIC);      
        Annotation turn = new Annotation(
          null, participant.getLabel(), getTurnLayer().getId());
        turn.setConfidence(Constants.CONFIDENCE_MANUAL);
        graph.addAnnotation(turn);
        turn.setParent(participant);
        turn.setStart(
          graph.getOrCreateAnchorAt(0.0, Constants.CONFIDENCE_MANUAL));
        
        MessageFormat fmtParticipantFormat = new MessageFormat("{0}:\t");
        int lineOrdinal = 1;
        Vector<Annotation> utterances = new Vector<Annotation>();
        
        // one utterance per line, anchor offsets are character offsets for now
        for (String line : lines) {
          Annotation utterance = new Annotation(null, line, getUtteranceLayer().getId());
          utterance.setParentId(turn.getId())
            .setConfidence(Constants.CONFIDENCE_MANUAL);
          
          // does the line start with a participant ?
          try {
            Object[] oParticipant = fmtParticipantFormat.parse(line);
            String participantId = (String)oParticipant[0];
            participant = participants.get(participantId);
            if (participant == null) {
              participant = new Annotation(null, participantId, schema.getParticipantLayerId());
              participant.setConfidence(Constants.CONFIDENCE_MANUAL);
              graph.addAnnotation(participant);
              participants.put(participantId, participant);
            }
            
            if (lastAnchor.getOffset().equals(0.0)) {
              // just started, so recycle the turn and utterance we're already in
              turn.setLabel(participant.getLabel());
              turn.setParentId(participant.getId());
            } else {
              // finish old turn
              graph.addAnnotation(turn);
              
              // new turn		  
              turn = new Annotation(null, participant.getLabel(), getTurnLayer().getId());
              turn.setParentId(participant.getId())
                .setConfidence(Constants.CONFIDENCE_MANUAL);
              graph.addAnnotation(turn);
              turn.setStart(lastAnchor);
            } // not the first turn
            lineOrdinal = 1;
          } catch(ParseException exception) {
          } catch(NullPointerException exception) {
          } // null ID
          
          utterance.setParentId(turn.getId())
            .setOrdinal(lineOrdinal++)
            .setStart(lastAnchor)
            .setConfidence(Constants.CONFIDENCE_MANUAL);
          // update current position
          lastAnchor = graph.getOrCreateAnchorAt(
            lastAnchor.getOffset() + ((double)line.length() + 1), Constants.CONFIDENCE_MANUAL);
          utterance.setEnd(lastAnchor);
          turn.setEnd(lastAnchor);
          graph.addAnnotation(utterance);
          utterances.add(utterance);
          
        } // next line      
        graph.addAnnotation(turn);
        
        if (graph.first(getParticipantLayer().getId()) == null) {
          // we haven't added a participant yet, so add the default one
          graph.addAnnotation(participant);
        }
        
        // ensure we have an utterance tokenizer
        if (getTokenizer() == null) {
          setTokenizer(new SimpleTokenizer(
                         getUtteranceLayer().getId(), getWordLayer().getId())
                       .setCharacterAnchorConfidence(Constants.CONFIDENCE_DEFAULT));
        }
        // tokenize utterances
        try {
          tokenizer.transform(graph);
        } catch(TransformationException exception) {
          if (errors == null) errors = new SerializationException();
          if (errors.getCause() == null) errors.initCause(exception);
          errors.addError(
            SerializationException.ErrorType.Tokenization,
            "Transcript " + d + ": " + exception.getMessage());
        }
        
        graph.trackChanges();
        
        // delete tokens that are turn-start markers
        for (Annotation t : graph.all(turnLayer.getId())) {
          Annotation firstWord = t.first(wordLayer.getId());
          if (firstWord != null && firstWord.getLabel().equals(t.getLabel() + ":")) {
            Annotation second = firstWord.getNext();
            if (second != null // there is a second word
                // and it's not the start of a second utterance
                && second.getStart().startOf(schema.getUtteranceLayer().getId()).size() == 0) {
              // join the second word (and any annotations) back to the start of the first word
              for (Annotation startsHere : second.getStart().getStartingAnnotations()) {
                startsHere.setStart(firstWord.getStart());
              }
            }
            firstWord.destroy();
          }
        } // next turn
        graph.commit();
        
        OrthographyClumper clumper = new OrthographyClumper(
          wordLayer.getId(), utteranceLayer.getId());
        try {
          // clump non-orthographic 'words' with real words
          clumper.transform(graph);
          graph.commit();
        } catch(TransformationException exception) {
          if (errors == null) errors = new SerializationException();
          if (errors.getCause() == null) errors.initCause(exception);
          errors.addError(SerializationException.ErrorType.Tokenization,
                          "Transcript " + d + ": " + exception.getMessage());
        }
        
        if (json.containsKey("label")) {
          // add label annotations
          JsonArray labelArray = json.getJsonArray("label");
          // each element is a triple: startChar, endChar, "layerId:label"
          int e = 0;
          for (JsonValue element : labelArray) {
            JsonArray triple = element.asJsonArray();
            // character offsets as doubles because that's what we need later
            double startChar = triple.getJsonNumber(0).doubleValue();
            double endChar = triple.getJsonNumber(1).doubleValue();
            String annotation = triple.getString(2);
            try {
              Object[] parts = labelFormat.parse(annotation);
              String layerId = ((String)parts[0]).trim();
              String label = ((String)parts[1]).trim();

              // has this layer been mapped?
              if (parameters.containsKey(layerId)){
                Parameter p = parameters.get(layerId);
                Layer l = (Layer)p.getValue();
                if (l != null && l.getId() != null) {

                  // find the correct anchors
                  Annotation[] overlapping = graph.overlappingAnnotations(startChar, endChar, wordLayer.getId());
                  if (overlapping.length == 0) {
                    errors.addError(
                      SerializationException.ErrorType.Tokenization,
                      "Transcript " + d + " label "+l+" \""+annotation+"\""
                      +": Interval "+startChar+"-"+endChar+" includes no words");
                  } else {
                    Annotation firstWord = overlapping[0];
                    Annotation lastWord = overlapping[overlapping.length-1];
                    Annotation parent = graph;
                    if (l.getParentId().equals(turnLayer.getId())) {
                      parent = firstWord.getParent();
                    } else if (l.getParentId().equals(wordLayer.getId())) {
                      parent = firstWord;
                    } 
                    graph.createSpan(firstWord, lastWord, l.getId(), label, parent);
                  } // found annotated words
                } // layer is mapped
              } // parameter for the layer
              
            } catch (ParseException exception) {
              errors.addError(
                SerializationException.ErrorType.Tokenization,
                "Transcript " + d + " label "+e
                +": Could not parse 'layerId:label' from \""+annotation+"\" : "
                + exception.getMessage());
            }
            e++;
          }
        }
        
        // the anchors object is keyed on layerId, and contains arrays of couples with the
        // original offsets of the annotions when they were serialized
        if (json.containsKey("anchors")) {
          JsonObject anchors = json.getJsonObject("anchors");
          if (anchors.containsKey(schema.getUtteranceLayer().getId())) {
            // set utterance/turn anchors to their original values
            JsonArray utteranceAnchors = anchors.getJsonArray(schema.getUtteranceLayer().getId());
            if (utterances.size() != utteranceAnchors.size()) {
              if (errors == null) errors = new SerializationException();
              errors.addError(
                SerializationException.ErrorType.InvalidDocument,
                "Transcript " + d + ": Expected " + utterances.size()
                + " utterance anchor elements, found " + utteranceAnchors.size());
            } else {
              // set offset units
              if (anchors.containsKey("offsetUnits")) {
                graph.setOffsetUnits(anchors.getString("offsetUnits"));
              } else {
                graph.setOffsetUnits(Constants.UNIT_SECONDS);
              }
              
              // update offsets
              double lastEndOffset = 0.0;
              for (int u = 0; u < utterances.size(); u++) {
                Annotation utt = utterances.elementAt(u);
                JsonArray uttStartEnd = utteranceAnchors.getJsonArray(u);
                if (uttStartEnd.size() < 2) {
                  errors.addError(SerializationException.ErrorType.InvalidDocument,
                                  "Too few anchor offsets for utterance " + u);
                } else {
                  
                  // start
                  double startOffset = uttStartEnd.getJsonNumber(0).doubleValue();
                  if (startOffset == lastEndOffset) {
                    // update existing anchor
                    utt.getStart().setOffset(startOffset);
                  } else { // this start different from last end
                    // create a new anchor
                    Anchor newStart = graph.getOrCreateAnchorAt(startOffset);
                    // bring all other annotations starting here too
                    for (Annotation startingHere : utt.getStart().getStartingAnnotations()) {
                      startingHere.setStart(newStart);
                    } // next starting annotation
                  }
                  utt.getStart().setConfidence(Constants.CONFIDENCE_MANUAL);
                  
                  // end
                  double endOffset = uttStartEnd.getJsonNumber(1).doubleValue();
                  utt.getEnd().setOffset(endOffset)
                    .setConfidence(Constants.CONFIDENCE_MANUAL);
                  
                  lastEndOffset = endOffset;
                } // there are two anchors
              } // next utterance
              
              // unset word anchor offsets
              for (Annotation word : graph.all(wordLayer.getId())) {
                if (word.getStart().startOf(utteranceLayer.getId()).size() == 0) {
                  word.getStart().setOffset(null);
                }
                if (word.getEnd().endOf(utteranceLayer.getId()).size() == 0) {
                  word.getEnd().setOffset(null);
                }
              } // next word
              
            } // number of anchors ok
          } // utterance anchors exist
        } // anchors exist
        
        graph.commit();
        
        // reset all change tracking
        graph.getTracker().reset();
        graph.setTracker(null);

        document = reader.readLine();
        d++;
      } // next document
    } catch (Exception exception) {
      if (errors == null) errors = new SerializationException();
      if (errors.getCause() == null) errors.initCause(exception);
      errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
    }
    if (errors != null) throw errors;        
    return graphs.toArray(new Graph[0]);
  }

  /**
   * Returns any warnings that may have arisen during the last execution of 
   * {@link #deserialize()}.
   * @return A possibly empty list of warnings.
   */
  public String[] getWarnings() {
    return warnings.toArray(new String[0]);
  }

} // end of class JSONLSerialization
