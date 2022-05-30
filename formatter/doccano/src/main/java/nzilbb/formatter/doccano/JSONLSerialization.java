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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Spliterator;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.Vector;
import java.util.function.Consumer;
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
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

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
 * @author Robert Fromont robert@fromont.net.nz
 */
public class JSONLSerialization implements /*TODO GraphDeserializer,*/ GraphSerializer {
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
      "application/pdf", ".jsonl", "1.0.0",
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
    Vector<String> requiredLayers = new Vector<String>();
    if (getParticipantLayer() != null) requiredLayers.add(getParticipantLayer().getId());
    if (getTurnLayer() != null) requiredLayers.add(getTurnLayer().getId());
    if (getUtteranceLayer() != null) requiredLayers.add(getUtteranceLayer().getId());
    if (getWordLayer() != null) requiredLayers.add(getWordLayer().getId());
    return requiredLayers.toArray(new String[0]);
  } // getRequiredLayers()

  /**
   * Determines the cardinality between graphs and serialized streams.
   * <p>The cardinality of this deserializer is NToM as there are two streams produced
   * regardless of how many graphs are serialized; the dataset, and the labels definition.
   * @return {@link nzilbb.ag.serialize.GraphSerializer#Cardinality}.NToOne.
   */
  public Cardinality getCardinality() {
    return Cardinality.NToM;
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
      final TreeSet<String> labels = new TreeSet<String>();

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
            serializeGraph(graph, layerIds, jsonlFile, labels);
            consumedGraphCount++;
          } catch(Exception exception) {
            errors.accept(new SerializationException(exception));
          }
        }); // next graph

      // write labels file
      File labelsJsonFile = File.createTempFile("doccano-labels.",".json");
      FileOutputStream out = new FileOutputStream(labelsJsonFile, true);
      JsonGenerator json = Json.createGenerator(out);
      json.writeStartArray();
      for (String label : labels) {
        json.writeStartObject();
        json.write("text", label);
        json.write("suffix_key", "");
        json.write("background_color", "#96A339");
        json.write("text_color", "#AAAAAA");
        json.writeEnd(); // label
      } // next label
      json.writeEnd(); // array
      json.close();

      // pass on streams
      consumer.accept(
       new NamedStream(new TempFileInputStream(jsonlFile),
                       fileName+".jsonl", "application/x-jsonlines"));     
      consumer.accept(
        new NamedStream(new TempFileInputStream(labelsJsonFile),
                        "labels.json", "application/json"));
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
   * @param labels Set of annotation labels encountered so far, which the method should add to.
   * @return A named stream that contains the PDF. 
   * @throws SerializationException if errors occur during deserialization.
   */
  protected void serializeGraph(Graph graph, String[] layerIds, File jsonlFile, TreeSet<String> labels)
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
      LinkedHashMap<String, Annotation[]> annotations = new LinkedHashMap<String, Annotation[]>();
      for (String layerId : layerIds) {
        if (layerId.equals(schema.getParticipantLayerId())) continue;
        if (layerId.equals(schema.getTurnLayerId())) continue;
        if (layerId.equals(schema.getUtteranceLayerId())) continue;
        if (layerId.equals(schema.getWordLayerId())) continue;
        annotations.put(layerId, graph.all(layerId));
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
            labels.add(label); // accumulate labels for labels definition stream
          }
        } // next annotation
      } // next layer
      json.writeEnd(); // label array

      // write anchors
      json.writeStartObject("anchors");
      String layerId = schema.getUtteranceLayerId();
      json.writeStartArray(layerId);
      for (Annotation utterance : utterances) {        
        json.writeStartArray(); // start offset couple
        json.write(utterance.getStart().getOffset());
        json.write(utterance.getEnd().getOffset());
        json.writeEnd();  // end offset couple
      } // next utterance
      json.writeEnd(); // utterance array
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
   
} // end of class JSONLSerialization
