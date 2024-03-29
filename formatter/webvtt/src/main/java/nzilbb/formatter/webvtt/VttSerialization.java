//
// Copyright 2019-2023 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.formatter.webvtt;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * (De)serializes VTT subtitle/caption files.
 * <p> Whole-line voice spans, if present are parsed to identify participants, i.e.
 * utterances formatted: <code>&lt;v.speaker1 John Smith&gt;The quick brown fox.</code>
 * will result in an utterance "The quick brown fox." uttered by a participant called
 * "John Smith".
 * <p> Output captions include such voice spans which include the participant label, and each
 * is identified with a class like "speaker1" to allow different speakers to be styled
 * differently.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class VttSerialization implements GraphDeserializer, GraphSerializer {
   
  // Attributes:
  protected Vector<String> warnings;
  /**
   * Returns any warnings that may have arisen during the last execution of {@link #deserialize()}.
   * <p>{@link GraphDeserializer} method.
   * @return A possibly empty list of warnings.
   */
  public String[] getWarnings()
  {
    return warnings.toArray(new String[0]);
  }

  /**
   * Returns the deserializer's descriptor.
   * <p>{@link GraphDeserializer} method.
   * @return The deserializer's descriptor
   */
  public SerializationDescriptor getDescriptor() {
    return new SerializationDescriptor(
      "WebVTT subtitles", getClass().getPackage().getImplementationVersion(),
      "text/vtt", ".vtt", "1.0.0", getClass().getResource("icon.png"));
  }

  /**
   * Name/ID of the file/graph.
   * @see #getName()
   * @see #setName(String)
   */
  protected String name;
  /**
   * Getter for {@link #name}: Name/ID of the file/graph.
   * @return Name/ID of the file/graph.
   */
  public String getName() { return name; }
  /**
   * Setter for {@link #name}: Name/ID of the file/graph.
   * @param newName Name/ID of the file/graph.
   */
  public void setName(String newName) { name = newName; }

  /**
   * Reader for VTT stread.
   * @see #getVtt()
   * @see #setVtt(BufferedReader)
   */
  protected BufferedReader vtt;
  /**
   * Getter for {@link #vtt}: Reader for VTT stread.
   * @return Reader for VTT stread.
   */
  public BufferedReader getVtt() { return vtt; }
  /**
   * Setter for {@link #vtt}: Reader for VTT stread.
   * @param newVtt Reader for VTT stread.
   */
  public void setVtt(BufferedReader newVtt) { vtt = newVtt; }

  /**
   * The annotation graph schema.
   * @see #getSchema()
   * @see #setSchema(Schema)
   */
  protected Schema schema;
  /**
   * Getter for {@link #schema}: The annotation graph schema.
   * @return The annotation graph schema.
   */
  public Schema getSchema() { return schema; }
  /**
   * Setter for {@link #schema}: The annotation graph schema.
   * @param newSchema The annotation graph schema.
   */
  public void setSchema(Schema newSchema) { schema = newSchema; }

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
  public void setParticipantLayer(Layer newParticipantLayer) { participantLayer = newParticipantLayer; }

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
  public void setTurnLayer(Layer newTurnLayer) { turnLayer = newTurnLayer; }

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
  public void setUtteranceLayer(Layer newUtteranceLayer) { utteranceLayer = newUtteranceLayer; }

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
  public void setWordLayer(Layer newWordLayer) { wordLayer = newWordLayer; }

  /**
   * Timers for debugging and optimization.
   * @see #getTimers()
   * @see #setTimers(Timers)
   */
  protected Timers timers;
  /**
   * Getter for {@link #timers}: Timers for debugging and optimization.
   * @return Timers for debugging and optimization.
   */
  public Timers getTimers() { return timers; }
  /**
   * Setter for {@link #timers}: Timers for debugging and optimization.
   * @param newTimers Timers for debugging and optimization.
   */
  public void setTimers(Timers newTimers) { timers = newTimers; }

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
  public void setTokenizer(GraphTransformer newTokenizer) { tokenizer = newTokenizer; }
   
  /**
   * Meta-data values.
   * @see #getMetaData()
   * @see #setMetaData(HashMap)
   */
  protected HashMap<String,String> metaData = new HashMap<String,String>();
  /**
   * Getter for {@link #metaData}: Meta-data values.
   * @return Meta-data values.
   */
  public HashMap<String,String> getMetaData() { return metaData; }
  /**
   * Setter for {@link #metaData}: Meta-data values.
   * @param newMetaData Meta-data values.
   */
  public void setMetaData(HashMap<String,String> newMetaData) { metaData = newMetaData; }

  private long graphCount = 0;
  private long consumedGraphCount = 0;
  /**
   * Determines how far through the serialization is.
   * @return An integer between 0 and 100 (inclusive), or null if progress can not be calculated.
   */
  public Integer getPercentComplete() {
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
  public VttSerialization setCancelling(boolean newCancelling) { cancelling = newCancelling; return this; }
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
  public VttSerialization() {
  } // end of constructor

  // GraphDeserializer methods

  /**
   * Sets parameters for deserializer as a whole.  This might include database connection
   * parameters, locations of supporting files, etc. 
   * <p>When the deserializer is installed, this method should be invoked with an empty parameter
   *  set, to discover what (if any) general configuration is required. If parameters are
   *  returned, and user interaction is possible, then the user may be presented with an
   *  interface for setting/confirming these parameters.
   * <p>{@link GraphDeserializer} method.
   * @param configuration The configuration for the deserializer. 
   * @param schema The layer schema, definining layers and the way they interrelate.
   * @return A list of configuration parameters (still) must be set before
   * {@link GraphDeserializer#setParameters(ParameterSet)} can be invoked. If this is an
   * empty list, {@link GraphDeserializer#setParameters(ParameterSet)} can be invoked. If
   * it's not an empty list, this method must be invoked again with the returned
   * parameters' values set. 
   */
  public ParameterSet configure(ParameterSet configuration, Schema schema) {
    setSchema(schema);
    setParticipantLayer(schema.getParticipantLayer());
    setTurnLayer(schema.getTurnLayer());
    setUtteranceLayer(schema.getUtteranceLayer());
    setWordLayer(schema.getWordLayer());

    // set any values that have been passed in
    for (Parameter p : configuration.values()) try { p.apply(this); } catch(Exception x) {}

    // create a list of layers we need and possible matching layer names
    LinkedHashMap<Parameter,List<String>> layerToPossibilities
      = new LinkedHashMap<Parameter,List<String>>();
    HashMap<String,LinkedHashMap<String,Layer>> layerToCandidates
      = new HashMap<String,LinkedHashMap<String,Layer>>();

    // do we need to ask for participant/turn/utterance/word layers?
    LinkedHashMap<String,Layer> possibleParticipantLayers = new LinkedHashMap<String,Layer>();
    LinkedHashMap<String,Layer> possibleTurnLayers = new LinkedHashMap<String,Layer>();
    LinkedHashMap<String,Layer> possibleTurnChildLayers = new LinkedHashMap<String,Layer>();
    LinkedHashMap<String,Layer> wordTagLayers = new LinkedHashMap<String,Layer>();
    if (getParticipantLayer() == null || getTurnLayer() == null 
        || getUtteranceLayer() == null || getWordLayer() == null) {
      for (Layer top : schema.getRoot().getChildren().values()) {
        if (top.getAlignment() == Constants.ALIGNMENT_NONE) {
          if (top.getChildren().size() != 0) {
            // unaligned children of graph, with children of their own
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
      if (getWordLayer() != null) {
        for (Layer tag : getWordLayer().getChildren().values()) {
          if (tag.getAlignment() == Constants.ALIGNMENT_NONE
              && tag.getChildren().size() == 0) {
            wordTagLayers.put(tag.getId(), tag);
          }
        } // next possible word tag layer
      }
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
                      "Layer for individual word tokens", false), 
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
      if (p.getValue() == null) {
        p.setValue(Utility.FindLayerById(candidateLayers, possibleNames));
      }
      p.setPossibleValues(candidateLayers.values());
    }

    return configuration;
  }   

  /**
   * Loads the serialized form of the graph, using the given set of named streams.
   * <p>{@link GraphDeserializer} method.
   * @param streams A list of named streams that contain all the
   *  transcription/annotation data required, and possibly (a) stream(s) for the media annotated.
   * @param schema The layer schema, definining layers and the way they interrelate.
   * @return A list of parameters that require setting before
   * {@link GraphDeserializer#deserialize()} can be invoked. This may be an empty list,
   * and may include parameters with the value already set to a workable default. If there
   * are parameters, and user interaction is possible, then the user may be presented with
   * an interface for setting/confirming these parameters, before they are then passed to
   * {@link GraphDeserializer#setParameters(ParameterSet)}.
   * @throws SerializationException If the graph could not be loaded.
   * @throws IOException On IO error.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public ParameterSet load(NamedStream[] streams, Schema schema)
    throws SerializationException, IOException {
    if (timers != null) timers.start("load");
    setSchema(schema);

    // take the first stream, ignore all others.
    NamedStream vttStream = Utility.FindSingleStream(streams, ".vtt", "text/vtt");
    if (vttStream == null) throw new SerializationException("No WebVTT stream found");
    setName(vttStream.getName());
    setVtt(new BufferedReader(new InputStreamReader(vttStream.getStream(), "UTF-8")));

    LinkedHashMap<Parameter,List<String>> layerToPossibilities
      = new LinkedHashMap<Parameter,List<String>>();
    HashMap<String,LinkedHashMap<String,Layer>> layerToCandidates
      = new HashMap<String,LinkedHashMap<String,Layer>>();	 

    LinkedHashMap<String,Layer> metadataLayers = new LinkedHashMap<String,Layer>();
    for (Layer layer : schema.getRoot().getChildren().values()) {
      if (layer.getAlignment() == Constants.ALIGNMENT_NONE) {
        metadataLayers.put(layer.getId(), layer);
      }
    } // next graph child layer
    for (Layer layer : schema.getParticipantLayer().getChildren().values()) {
      if (layer.getAlignment() == Constants.ALIGNMENT_NONE) {
        metadataLayers.put(layer.getId(), layer);
      }
    } // next participant child layer

    // read the header to discover attributes
    MessageFormat metaDataFormat = new MessageFormat("{0}:{1}");
    setMetaData(new HashMap<String,String>());
    String line = vtt.readLine();
    while (line != null && line.length() > 0) {
      try {
        Object[] oMetaData = metaDataFormat.parse(line);
        String key = ((String)oMetaData[0]).trim();
        String value = ((String)oMetaData[1]).trim();
        if (key.length() > 0 && value.length() > 0) {
          Vector<String> possibleMatches = new Vector<String>();
          possibleMatches.add("transcript" + key);
          possibleMatches.add("participant" + key);
          possibleMatches.add("speaker" + key);
          possibleMatches.add(key);
	       
          layerToPossibilities.put(
            new Parameter(key, Layer.class, "Header: " + key), 
            possibleMatches);
          layerToCandidates.put(key, metadataLayers);

          getMetaData().put(key, value);
        }
      } catch(ParseException exception) { // not parseable
      }
	 
      line = vtt.readLine();
    } // next header line

    if (timers != null) timers.end("load");
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
   * Sets parameters for a given deserialization operation, after loading the serialized
   * form of the graph. This might include mappings from format-specific objects like
   * tiers to graph layers, etc. 
   * <p>{@link GraphDeserializer} method.
   * @param parameters The configuration for a given deserialization operation.
   * @throws SerializationParametersMissingException If not all required parameters have a value.
   */
  public void setParameters(ParameterSet parameters)
    throws SerializationParametersMissingException {
    this.parameters = parameters;
  }

  /**
   * Deserializes the serialized data, generating one or more {@link Graph}s.
   * <p>Many data formats will only yield one graph (e.g. Transcriber
   * transcript or Praat textgrid), however there are formats that
   * are capable of storing multiple transcripts in the same file
   * (e.g. AGTK, Transana XML export), which is why this method
   * returns a list.
   * <p>{@link GraphDeserializer} method.
   * @return A list of valid (if incomplete) {@link Graph}s. 
   * @throws SerializerNotConfiguredException if the object has not been configured.
   * @throws SerializationParametersMissingException if the parameters for this particular
   * graph have not been set. 
   * @throws SerializationException if errors occur during deserialization.
   */
  public Graph[] deserialize() 
    throws SerializerNotConfiguredException, SerializationParametersMissingException,
    SerializationException {
    if (timers != null) timers.start("deserialize");
    if (schema == null) throw new SerializerNotConfiguredException("Layer schema not set");

    // if there are errors, accumlate as many as we can before throwing SerializationException
    SerializationException errors = null;

    warnings = new Vector<String>();

    Graph graph = new Graph();
    graph.setId(getName());
    graph.setOffsetGranularity(Constants.GRANULARITY_MILLISECONDS);
    // creat the 0 anchor to prevent graph tagging from creating one with no confidence
    Anchor graphStart = graph.getOrCreateAnchorAt(0.0, Constants.CONFIDENCE_MANUAL);

    // add layers to the graph
    // we don't just copy the whole schema, because that would imply that all the extra layers
    // contained no annotations, which is not necessarily true
    graph.addLayer((Layer)participantLayer.clone());
    graph.getSchema().setParticipantLayerId(participantLayer.getId());
    graph.addLayer((Layer)turnLayer.clone());
    graph.getSchema().setTurnLayerId(turnLayer.getId());
    graph.addLayer((Layer)utteranceLayer.clone());
    graph.getSchema().setUtteranceLayerId(utteranceLayer.getId());
    if (wordLayer != null) {
      graph.addLayer((Layer)wordLayer.clone());
      graph.getSchema().setWordLayerId(wordLayer.getId());
    }

    // meta-data
    for (String key : getMetaData().keySet()) {
      Parameter p = getParameters().get(key);
      Layer layer = (Layer)p.getValue();
      if (layer != null && !layer.getId().equals("[ignore]")) {
        graph.addLayer((Layer)schema.getLayer(layer.getId()).clone());
        graph.createTag(graph, layer.getId(), getMetaData().get(key))
          .setConfidence(Constants.CONFIDENCE_MANUAL);
      } // there's a layer mapping
    } // next meta-data item
     
    graph.setOffsetUnits(Constants.UNIT_SECONDS);
    graph.setOffsetGranularity(Constants.GRANULARITY_MILLISECONDS);

    // look for the time-interval pattern - something like
    // 00:00:03.389 --> 00:00:05.269 align:start position:0%
    // can also be 00:03.389 --> 00:05.269 align:start position:0%
    MessageFormat intervalFormatFull = new MessageFormat(
      "{0,number,integer}:{1,number,integer}:{2,number,integer}.{3,number,integer} --> {4,number,integer}:{5,number,integer}:{6,number,integer}.{7,number,integer}{8}");
    MessageFormat intervalFormatAbbr = new MessageFormat(
      "{0,number,integer}:{1,number,integer}.{2,number,integer} --> {3,number,integer}:{4,number,integer}.{5,number,integer}{6}");

    // speakers are specified by voice spans like <v.loud John Smith>Hello!
    Pattern voiceSpan = Pattern.compile(
      "^\\s*<v(?<class>\\.\\S+)? (?<voice>[^>]+)>(?<utterance>.*)$");

    // create a default speaker
    graph.addAnnotation(new Annotation(null, "", schema.getParticipantLayerId()));
    // but we might find some named voices to keep track of
    HashMap<String,Annotation> participantsByName = new HashMap<String,Annotation>();
    Annotation currentTurn = new Annotation(
      null, graph.first(schema.getParticipantLayerId()).getLabel(),
      schema.getTurnLayerId(),
      graphStart.getId(), graphStart.getId(),
      graph.first(schema.getParticipantLayerId()).getId());
    graph.addAnnotation(currentTurn);
    currentTurn.setConfidence(Constants.CONFIDENCE_MANUAL);
    Annotation currentUtterance = new Annotation(
      null, "",
      schema.getUtteranceLayerId(),
      graphStart.getId(), graphStart.getId(),
      currentTurn.getId());
    currentUtterance.setConfidence(Constants.CONFIDENCE_MANUAL);
    Annotation lastUtterance = null;
    try {
      // For each line...
      String line = vtt.readLine();
      while (line != null) {
        // skip comment lines
        if (!line.startsWith("NOTE ")
            // skip cue-number lines
            && !line.matches("^[0-9]+$")) {
          
          Long startHours = null;
          Long startMinutes = null;
          Long startSeconds = null;
          Long startMilliseconds = null;
          Long endHours = null;
          Long endMinutes = null;
          Long endSeconds = null;
          Long endMilliseconds = null;
          String suffix = null;

          try { // try full pattern
            Object[] intervalLine = intervalFormatFull.parse(line);            
            startHours = (Long)intervalLine[0];
            startMinutes = (Long)intervalLine[1];
            startSeconds = (Long)intervalLine[2];
            startMilliseconds = (Long)intervalLine[3];
            endHours = (Long)intervalLine[4];
            endMinutes = (Long)intervalLine[5];
            endSeconds = (Long)intervalLine[6];
            endMilliseconds = (Long)intervalLine[7];
            suffix = (String)intervalLine[8];
          } catch(ParseException exception) { 
            try { // try abbreviated pattern
              Object[] intervalLine = intervalFormatAbbr.parse(line);
              // this is an interval line...
              startHours = Long.valueOf(0);
              startMinutes = (Long)intervalLine[0];
              startSeconds = (Long)intervalLine[1];
              startMilliseconds = (Long)intervalLine[2];
              endHours = Long.valueOf(0);
              endMinutes = (Long)intervalLine[3];
              endSeconds = (Long)intervalLine[4];
              endMilliseconds = (Long)intervalLine[5];
              suffix = (String)intervalLine[6];
            } catch(ParseException exception2) {
            }
          }

          if (startMinutes != null) { // this is an interval line...
            // finish last utterance
            if (currentUtterance.getLabel().trim().length() > 0)
            { // the utterance actually contains something
              
              currentUtterance.setLabel(
                currentUtterance.getLabel()
                // remove <...> tags
                .replaceAll("<[^>]+>","")
                // strip out newlines and multiple spaces
                .replaceAll("[\r\n ]+", " ").replaceAll(" +", " ").trim());
              lastUtterance = graph.addAnnotation(currentUtterance);
            }
            
            // start new utterance
            Anchor start = graph.getOrCreateAnchorAt(
              startHours * 3600 + startMinutes * 60
              + startSeconds + startMilliseconds.doubleValue()/1000,
              Constants.CONFIDENCE_MANUAL);
            
            Anchor end = graph.getOrCreateAnchorAt(
              endHours * 3600 + endMinutes * 60
              + endSeconds + endMilliseconds.doubleValue()/1000,
              Constants.CONFIDENCE_MANUAL);
            
            currentUtterance = new Annotation(
              null, "",
              schema.getUtteranceLayerId(),
              start.getId(), end.getId(), currentTurn.getId());
            currentUtterance.setConfidence(Constants.CONFIDENCE_MANUAL);
            currentTurn.setEndId(end.getId());
            
            line = vtt.readLine();
            continue;
          } // this is an interval line
          
          // not an interval definition, so add the text to the utterance

          // look for voice span something like <v.load John Smith>Hello!
          Matcher matchVoiceSpan = voiceSpan.matcher(line);
          if (matchVoiceSpan.matches()) {
            String voice = matchVoiceSpan.group("voice");
            line = matchVoiceSpan.group("utterance")
              // strip of closing tag if any
              .trim().replaceAll("</v>$","");

            // if the current participant isn't named
            if (currentTurn.getLabel().length() == 0) { // no name
              // set the current participant's label to this voice
              currentTurn.setLabel(voice); // turn...
              currentTurn.getParent().setLabel(voice); // ...and participant
              participantsByName.put(voice, currentTurn.getParent());
            } else { // current participant has a name
              if (!currentTurn.getLabel().equals(voice)) { // different speaker?
                // start a new turn here...

                // is there an existing participant with this name?
                Annotation participant = participantsByName.get(voice);
                if (participant == null) { // haven't encounterd this voice before
                  // create a new participant
                  participant = graph.addAnnotation(
                    new Annotation(null, voice, schema.getParticipantLayerId()));
                }

                // the last turn ends at the start of this utterance
                if (lastUtterance != null) {
                  currentTurn.setEndId(lastUtterance.getEndId());
                }

                // new turn
                currentTurn = new Annotation(
                  null, voice,
                  schema.getTurnLayerId(),
                  currentUtterance.getStartId(), currentUtterance.getEndId(),
                  participant.getId());
                graph.addAnnotation(currentTurn);
                currentTurn.setConfidence(Constants.CONFIDENCE_MANUAL);
                // utterance's parent is this turn
                currentUtterance.setParent(currentTurn);
              } // change of voice
            } // current participant has a name
          } // voice span
          
          currentUtterance.setLabel(currentUtterance.getLabel() + " " + line);
        }
            
        line = vtt.readLine();
      } // next line
      // finish last utterance
      if (currentUtterance.getLabel().trim().length() > 0) {
        // the utterance actually contains something
        graph.addAnnotation(currentUtterance);
      }
	 
      // ensure we have an utterance tokenizer
      if (getWordLayer() != null) {
        graph.trackChanges();
        if (getTokenizer() == null) {
          setTokenizer(new SimpleTokenizer(getUtteranceLayer().getId(), getWordLayer().getId()));
        }
        try {
          if (timers != null) timers.start("tokenization");
          tokenizer.transform(graph);
          if (timers != null) timers.end("tokenization");
        } catch(TransformationException exception) {
          if (errors == null) errors = new SerializationException();
          if (errors.getCause() == null) errors.initCause(exception);
          errors.addError(SerializationException.ErrorType.Tokenization, exception.getMessage());
        }
	    
        OrthographyClumper clumper
          = new OrthographyClumper(wordLayer.getId(), utteranceLayer.getId());
        try {
          // clump non-orthographic 'words' with real words
          if (timers != null) timers.start("orthography clumping");
          clumper.transform(graph);
          if (timers != null) timers.end("orthography clumping");
        } catch(TransformationException exception) {
          if (errors == null) errors = new SerializationException();
          if (errors.getCause() == null) errors.initCause(exception);
          errors.addError(SerializationException.ErrorType.Tokenization, exception.getMessage());
        }

      } // there is a word layer
      graph.commit();
    } catch(IOException exception) {
      if (errors == null) errors = new SerializationException();
      if (errors.getCause() == null) errors.initCause(exception);
      errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
    }

    // set end anchors of graph tags
    int unnamedSpeakersSoFar = 0;
    for (Annotation a : graph.all(getParticipantLayer().getId())) {
      a.setStartId(graphStart.getId());
      if (currentUtterance != null && currentUtterance.getEnd() != null) {
        a.setEndId(currentUtterance.getEnd().getId());
      }
      // ensure all participants have a name
      if (a.getLabel().length() == 0) {
        String speakerLabel = "speaker"
          + (unnamedSpeakersSoFar == 0?"":"-"+(unnamedSpeakersSoFar+1));
        a.setLabel(speakerLabel);
        a.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
        // ensure turns are similarly relabelled
        for (Annotation turn : a.all(schema.getTurnLayerId())) turn.setLabel(speakerLabel);
        unnamedSpeakersSoFar++;
      } else { // had an explicit name
        a.setConfidence(Constants.CONFIDENCE_MANUAL);
      }
    }
    graph.commit();

    if (errors != null) throw errors;

    // reset all change tracking
    if (graph.getTracker() != null) {
      graph.getTracker().reset();
      graph.setTracker(null);
    }
      
    Graph[] graphs = { graph };
    if (timers != null) timers.end("deserialize");
    return graphs;
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
   * <p>The cardinatlity of this deseerializer is NToN.
   * @return {@link nzilbb.ag.serialize.GraphSerializer.Cardinality}.NToN.
   */
  public Cardinality getCardinality() {
    return Cardinality.NToN;
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
    graphs.forEachRemaining(graph -> {
        if (getCancelling()) return;
        try {
          consumer.accept(serializeGraph(graph, layerIds));
        } catch(SerializationException exception) {
          errors.accept(exception);
        }
        consumedGraphCount++;
      }); // next graph
  }
   
  /**
   * Serializes the given graph, generating a {@link NamedStream}.
   * @param graph The graph to serialize.
   * @return A named stream that contains the TextGrid. 
   * @throws SerializationException if errors occur during deserialization.
   */
  protected NamedStream serializeGraph(Graph graph, String[] layerIds)
    throws SerializationException {
      
    SerializationException errors = null;
    try {
      // write the text to a temporary file
      File f = File.createTempFile(graph.getId(), ".txt");
      PrintWriter writer
        = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), "utf-8"));

      writer.println("WEBVTT Kind: captions");
      writer.println("NOTE Generated by nzilbb.ag converter - " + getDescriptor());
      
      Schema schema = graph.getSchema();

      // meta data
      Vector<Layer> participantMetaData = new Vector<Layer>();
      if (layerIds != null) {
        for (String l : layerIds) {
          Layer layer = schema.getLayer(l);
          if (layer != null) {
            if ((layer.getParentId() == null
                 || layer.getParentId().equals(schema.getRoot().getId()))
                && layer.getAlignment() == Constants.ALIGNMENT_NONE
                && !layer.getId().equals(schema.getParticipantLayerId())) { // meta data
              // include NOTE with the label(s)
              for (Annotation tag : graph.all(l)) {
                if (tag.getLabel().length() > 0) {
                  writer.println("NOTE " + l + ":" + tag.getLabel());
                }
              } // next label on this layer
            } else if (schema.getParticipantLayerId().equals(layer.getParentId())
                       && layer.getAlignment() == Constants.ALIGNMENT_NONE) {
              // participant meta data
              participantMetaData.add(layer);
            }
          }
        } // next layeyId
      }
      
      // number each participant
      int s = 1;
      for (Annotation participant : graph.all(participantLayer.getId())) {
        participant.put("@serial", Integer.valueOf(s++));

        // also include participant IDs and meta data as NOTEs
        writer.println("NOTE Speaker: " + participant.getLabel());
        
        for (Layer layer : participantMetaData) {
          // include NOTE with the label(s)
          for (Annotation tag : participant.all(layer.getId())) {
            if (tag.getLabel().length() > 0) {
              writer.println("NOTE " + layer.getId() + ":" + tag.getLabel());
            }
          }
        } // next participant attribute
        
      } // next participant
      writer.println();
         
      // order utterances by anchor so that simultaneous speech comes out in utterance order
      TreeSet<Annotation> utterancesByAnchor
        = new TreeSet<Annotation>(new AnnotationComparatorByAnchor());
      for (Annotation u : graph.all(getUtteranceLayer().getId())) utterancesByAnchor.add(u);
         
      int iSubtitle = 0;
      for (Annotation utterance : utterancesByAnchor) {
        if (cancelling) break;
        writer.println("" + (++iSubtitle));
        writer.println(VttTime(utterance.getStart().getOffset())
                       + " --> "
                       + VttTime(utterance.getEnd().getOffset()));

        // is the participant changing?
        Annotation participant = utterance.first(participantLayer.getId());
        writer.print(
          "<v"
          + ".speaker"+participant.get("@serial")
          + " "+participant.getLabel().replace(">", "&gt;") // replace '>'
          +">");

        boolean firstWord = true;
        for (Annotation token : utterance.all(getWordLayer().getId())) {
          if (firstWord) {
            firstWord = false;
          } else {
            writer.print(" ");
          }
          writer.print(token.getLabel());
        } // next token
        writer.println("</v>"); // to end the line
        writer.println(); // to create a blank line
      } // next utterance
      writer.close();

      TempFileInputStream in = new TempFileInputStream(f);
         
      // return a named stream from the file
      return new NamedStream(in, IO.SafeFileNameUrl(graph.getId()) + ".vtt");
    } catch(Exception exception) {
      errors = new SerializationException();
      errors.initCause(exception);
      errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
      throw errors;
    }      
  }
   
  private static final MessageFormat timeFormatter = new MessageFormat(
    "{0,number,00}:{1,number,00}:{2,number,00.000}", new Locale("en"));
  /**
   * Formats a number of seconds as HH:MM:SS,mmm
   * @param dSeconds
   * @return The time in HH:MM:SS,mmm format.
   */
  public static String VttTime(double dSeconds) {
    Integer iHours = Integer.valueOf((int)(dSeconds / 3600));
    dSeconds -= (iHours * 3600);
    Integer iMinutes = Integer.valueOf((int)(dSeconds / 60));
    dSeconds -= (iMinutes * 60);
    Double dTheRest = Double.valueOf(dSeconds);
    Object[] args = {iHours, iMinutes, dTheRest};
    return timeFormatter.format(args);
  } // end of VttTime()

} // end of class VttSerialization
