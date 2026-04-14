//
// Copyright 2022-2026 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.formatter.whisper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
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
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.util.AnnotationsByAnchor;
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
 * Parser for transcriptions output by the
 * <a href="https://github.com/openai/whisper">Whisper</a>
 * ASR system.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class WhisperDeserializer implements GraphDeserializer {
   
  // Attributes:
  protected Vector<String> warnings;
  /**
   * Returns any warnings that may have arisen during the last execution of
   * {@link #deserialize()}.
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
      "Whisper ASR transcript", getClass().getPackage().getImplementationVersion(),
      "text/whisper+plain", ".wt", "1.0.0", getClass().getResource("icon.svg"));
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
   * Reader for transcript stdout stream.
   * @see #getTranscript()
   * @see #setTranscript(BufferedReader)
   */
  protected BufferedReader transcript;
  /**
   * Getter for {@link #transcript}: Reader for transcript stdout stream.
   * @return Reader for transcript stdout stream.
   */
  public BufferedReader getTranscript() { return transcript; }
  /**
   * Setter for {@link #transcript}: Reader for transcript stdout stream.
   * @param newTranscript Reader for transcript stdout stream.
   */
  public void setTranscript(BufferedReader newTranscript) { transcript = newTranscript; }
  
  /**
   * JSON object representing the JSON-formatted transcript.
   * @see #getJson()
   * @see #setJson(JsonObject)
   */
  protected JsonObject json;
  /**
   * Getter for {@link #json}: JSON object representing the JSON-formatted transcript.
   * @return JSON object representing the JSON-formatted transcript.
   */
  public JsonObject getJson() { return json; }
  /**
   * Setter for {@link #json}: JSON object representing the JSON-formatted transcript.
   * @param newJson JSON object representing the JSON-formatted transcript.
   */
  public WhisperDeserializer setJson(JsonObject newJson) { json = newJson; return this; }  

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
   * The minimum inter-word pause length, in seconds, before a pause
   * counts as a 'short pause'. The default value is 0.2.
   * @see #minMediumPauseLength
   * @see #minLongPauseLength
   * @see #shortPauseLabel
   * @see #getMinShortPauseLength()
   * @see #setMinShortPauseLength(Double)
   */
  protected Double minShortPauseLength = 0.2;
  /**
   * Getter for {@link #minShortPauseLength}: The minimum inter-word
   * pause length, in seconds, before a pause counts as a 'short
   * pause'. The default values is 0.2.
   * @return The minimum inter-word pause length, in seconds, before a
   * pause counts as a 'short pause'. 
   * @see #getShortPauseLabel()
   */
  public Double getMinShortPauseLength() { return minShortPauseLength; }
  /**
   * Setter for {@link #minShortPauseLength}: The minimum inter-word
   * pause length, in seconds, before a pause counts as a 'short
   * pause'.
   * @param newMinShortPauseLength The minimum inter-word pause
   * length, in seconds, before a pause counts as a 'short pause'. 
   */
  public WhisperDeserializer setMinShortPauseLength(Double newMinShortPauseLength) { minShortPauseLength = newMinShortPauseLength; return this; }
  
  /**
   * The minimum inter-word pause length, in seconds, before a pause
   * counts as a 'medium pause'. The default value is 0.7.
   * @see #minShortPauseLength
   * @see #minLongPauseLength
   * @see #mediumPauseLabel
   * @see #getMinMediumPauseLength()
   * @see #setMinMediumPauseLength(Double)
   */
  protected Double minMediumPauseLength = 0.7;
  /**
   * Getter for {@link #minMediumPauseLength}: The minimum inter-word
   * pause length, in seconds, before a pause counts as a 'medium pause'.
   * The default value is 0.7. 
   * @return The minimum inter-word pause length, in seconds, before
   * a pause counts as a 'medium pause'. 
   * @see #getMediumPauseLabel()
   */
  public Double getMinMediumPauseLength() { return minMediumPauseLength; }
  /**
   * Setter for {@link #minMediumPauseLength}: The minimum inter-word
   * pause length, in seconds, before a pause counts as a 'medium pause'. 
   * @param newMinMediumPauseLength The minimum inter-word pause
   * length, in seconds, before a pause counts as a 'medium pause'. 
   */
  public WhisperDeserializer setMinMediumPauseLength(Double newMinMediumPauseLength) { minMediumPauseLength = newMinMediumPauseLength; return this; }
  
  /**
   * The minimum inter-word pause length, in seconds, before a pause
   * counts as a 'long pause'. The default value is 1.4.
   * @see #minShortPauseLength
   * @see #minMediumPauseLength
   * @see #longPauseLabel
   * @see #getMinLongPauseLength()
   * @see #setMinLongPauseLength(Double)
   */
  protected Double minLongPauseLength = 1.4;
  /**
   * Getter for {@link #minLongPauseLength}: The minimum inter-word
   * pause length, in seconds, before a pause counts as a 'long pause'.
   * The default value is 1.4. 
   * @return The minimum inter-word pause length, in seconds, before a
   * pause counts as a 'long pause'. 
   * @see #getLongPauseLabel()
   */
  public Double getMinLongPauseLength() { return minLongPauseLength; }
  /**
   * Setter for {@link #minLongPauseLength}: The minimum inter-word
   * pause length, in seconds, before a pause counts as a 'long pause'. 
   * @param newMinLongPauseLength The minimum inter-word pause length,
   * in seconds, before a pause counts as a 'long pause'. 
   */
  public WhisperDeserializer setMinLongPauseLength(Double newMinLongPauseLength) { minLongPauseLength = newMinLongPauseLength; return this; }

  /**
   * The string to append to the word before a short pause.
   * If an inter-word pause has a duration between
   * {@link #getMinShortPauseLength()} and {@link #getMinMediumPauseLength()}, then
   * the word before the pause will have this string appended to its
   * label (after a space).
   * @see #getShortPauseLabel()
   * @see #setShortPauseLabel(String)
   */
  protected String shortPauseLabel = "(.)";
  /**
   * Getter for {@link #shortPauseLabel}: The string to append to the
   * word before a short pause.  
   * If an inter-word pause has a duration between
   * {@link #getMinShortPauseLength()} and {@link #getMinMediumPauseLength()}, then
   * the word before the pause will have this string appended to its
   * label (after a space).
   * If null or empty, no short pauses are labelled.
   * @return The string to append to the word before a short pause.
   */
  public String getShortPauseLabel() { return shortPauseLabel; }
  /**
   * Setter for {@link #shortPauseLabel}: The string to append to the
   * word before a short pause. 
   * If an inter-word pause has a duration between
   * {@link #getMinShortPauseLength()} and {@link #getMinMediumPauseLength()}, then
   * the word before the pause will have this string appended to its
   * label (after a space).
   * @param newShortPauseLabel The string to append to the word before a short pause.
   */
  public WhisperDeserializer setShortPauseLabel(String newShortPauseLabel) { shortPauseLabel = newShortPauseLabel; return this; }

  /**
   * The string to append to the word before a medium pause.
   * If an inter-word pause has a duration between
   * {@link #getMinMediumPauseLength()} and {@link #getMinLongPauseLength()}, then
   * the word before the pause will have this string appended to its
   * label (after a space).
   * @see #getMediumPauseLabel()
   * @see #setMediumPauseLabel(String)
   */
  protected String mediumPauseLabel = "(..)";
  /**
   * Getter for {@link #mediumPauseLabel}: The string to append to the
   * word before a medium pause. 
   * If an inter-word pause has a duration between
   * {@link #getMinMediumPauseLength()} and {@link #getMinLongPauseLength()}, then
   * the word before the pause will have this string appended to its
   * label (after a space).
   * If null or empty, no medium pauses are labelled.
   * @return The string to append to the word before a medium pause.
   */
  public String getMediumPauseLabel() { return mediumPauseLabel; }
  /**
   * Setter for {@link #mediumPauseLabel}: The string to append to the
   * word before a medium pause. 
   * If an inter-word pause has a duration between
   * {@link #getMinMediumPauseLength()} and {@link #getMinLongPauseLength()}, then
   * the word before the pause will have this string appended to its
   * label (after a space).
   * @param newMediumPauseLabel The string to append to the word before a medium pause.
   */
  public WhisperDeserializer setMediumPauseLabel(String newMediumPauseLabel) { mediumPauseLabel = newMediumPauseLabel; return this; }
  
  /**
   * The string to append to the word before a long pause.
   * If an inter-word pause has a duration longer than
   * {@link #getMinLongPauseLength()}, then the word before the pause
   * will have this string appended to its 
   * label (after a space).
   * @see #getLongPauseLabel()
   * @see #setLongPauseLabel(String)
   */
  protected String longPauseLabel = "(...)";
  /**
   * Getter for {@link #longPauseLabel}: The string to append to the
   * word before a long pause. 
   * If an inter-word pause has a duration longer than
   * {@link #getMinLongPauseLength()}, then the word before the pause
   * will have this string appended to its 
   * If null or empty, no long pauses are labelled.
   * @return The string to append to the word before a long pause.
   */
  public String getLongPauseLabel() { return longPauseLabel; }
  /**
   * Setter for {@link #longPauseLabel}: The string to append to the
   * word before a long pause. 
   * If an inter-word pause has a duration longer than
   * {@link #getMinLongPauseLength()}, then the word before the pause
   * will have this string appended to its 
   * @param newLongPauseLabel The string to append to the word before a long pause.
   */
  public WhisperDeserializer setLongPauseLabel(String newLongPauseLabel) { longPauseLabel = newLongPauseLabel; return this; }
  
  /**
   * Maximum utterance duration to target (seconds). Longer utterances will be
   * split on longer inter-word pauses. 15s by default. 
   * @see #getMaxUtteranceDuration()
   * @see #setMaxUtteranceDuration(Double)
   */
  protected Double maxUtteranceDuration = 15.0;
  /**
   * Getter for {@link #maxUtteranceDuration}: Maximum utterance
   * duration to target. Longer utterances will be split on longer
   * inter-word pauses. 15s by default.
   * @return Maximum utterance duration to target (seconds). Longer utterances
   * will be split on longer inter-word pauses. 
   */
  public Double getMaxUtteranceDuration() { return maxUtteranceDuration; }
  /**
   * Setter for {@link #maxUtteranceDuration}: Maximum utterance
   * duration to target. Longer utterances will be split on longer
   * inter-word pauses.
   * @param newMaxUtteranceDuration Maximum utterance duration to
   * target. Longer utterances will be split on longer inter-word
   * pauses. 
   */
  public WhisperDeserializer setMaxUtteranceDuration(Double newMaxUtteranceDuration) { maxUtteranceDuration = newMaxUtteranceDuration; return this; }

  
  /**
   * Maximum number of seconds to subtract from the start time and add
   * to the end time of each utterance, to allow for alignment errors of
   * first/last word in each segment. The default is 0.5s. 
   * @see #getUtterancePadding()
   * @see #setUtterancePadding(Double)
   */
  protected Double utterancePadding = 0.5;
  /**
   * Getter for {@link #utterancePadding}: Maximum number of seconds to
   * subtract from the start time and add to the end time of each utterance,
   * to allow for alignment errors of first/last word in each segment.
   * The default is 0.5s.
   * @return Maximum number of seconds to subtract from the start time and
   * add to the end time of each utterance, to allow for alignment errors
   * of first/last word in each segment.
   */
  public Double getUtterancePadding() { return utterancePadding; }
  /**
   * Setter for {@link #utterancePadding}: Maximum number of seconds to
   * subtract from the start time and add to the end time of each utterance,
   * to allow for alignment errors of first/last word in each segment.
   * @param newUtterancePadding Maximum number of seconds to subtract
   * from the start time and add to the end time of each utterance, to
   * allow for alignment errors of first/last word in each segment.
   */
  public WhisperDeserializer setUtterancePadding(Double newUtterancePadding) { utterancePadding = newUtterancePadding; return this; }
  
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
  public WhisperDeserializer setCancelling(boolean newCancelling) { cancelling = newCancelling; return this; }
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
  public WhisperDeserializer() {
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
   * {@link GraphDeserializer#setParameters(ParameterSet)} can be invoked. If this is an empty 
   * list, {@link GraphDeserializer#setParameters(ParameterSet)} can be invoked. If it's not an 
   * empty list, this method must be invoked again with the returned parameters' values set. 
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
    
    // add parameters that aren't in the configuration yet, and set possible/default values
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

    if (!configuration.containsKey("minShortPauseLength")) {
      configuration.addParameter(
        new Parameter(
          "minShortPauseLength", Double.class, 
          "Minimum Short Pause Length",
          "The minimum inter-word pause length, in seconds,"
          +" before a pause counts as a 'short pause'.", true));
    }
    if (configuration.get("minShortPauseLength").getValue() == null) {
      configuration.get("minShortPauseLength").setValue(getMinShortPauseLength());
    }
    if (!configuration.containsKey("shortPauseLabel")) {
      configuration.addParameter(
        new Parameter(
          "shortPauseLabel", String.class, 
          "Short Pause Label",
          "If an inter-word pause has a duration between minShortPauseLength"
          +" and minMediumPauseLength, then the word before the pause"
          +" will have this string appended to its"
          +" label (after a space).", false));
    }
    if (configuration.get("shortPauseLabel").getValue() == null) {
      configuration.get("shortPauseLabel").setValue(getShortPauseLabel());
    }
    if (!configuration.containsKey("minMediumPauseLength")) {
      configuration.addParameter(
        new Parameter(
          "minMediumPauseLength", Double.class, 
          "Minimum Medium Pause Length",
          "The minimum inter-word pause length, in seconds,"
          +" before a pause counts as a 'medium pause' e.g. (.)", true));
    }
    if (configuration.get("minMediumPauseLength").getValue() == null) {
      configuration.get("minMediumPauseLength").setValue(getMinMediumPauseLength());
    }
    if (!configuration.containsKey("mediumPauseLabel")) {
      configuration.addParameter(
        new Parameter(
          "mediumPauseLabel", String.class, 
          "Medium Pause Label",
          "If an inter-word pause has a duration between minMediumPauseLength"
          +" and minLongPauseLength, then the word before the pause"
          +" will have this string appended to its"
          +" label (after a space) e.g (..)", false));
    }
    if (configuration.get("mediumPauseLabel").getValue() == null) {
      configuration.get("mediumPauseLabel").setValue(getMediumPauseLabel());
    }
    if (!configuration.containsKey("minLongPauseLength")) {
      configuration.addParameter(
        new Parameter(
          "minLongPauseLength", Double.class, 
          "Minimum Long Pause Length",
          "The minimum inter-word pause length, in seconds,"
          +" before a pause counts as a 'long pause'.", true));
    }
    if (configuration.get("minLongPauseLength").getValue() == null) {
      configuration.get("minLongPauseLength").setValue(getMinLongPauseLength());
    }
    if (!configuration.containsKey("longPauseLabel")) {
      configuration.addParameter(
        new Parameter(
          "longPauseLabel", String.class, 
          "Long Pause Label",
          "If an inter-word pause has a duration more than minLongPauseLength,"
          +" then the word before the pause will have this string appended to its"
          +" label (after a space) e.g. for the the lengh of the pause in parentheses,"
          +" use: ({0.000})", false));
    }
    if (configuration.get("longPauseLabel").getValue() == null) {
      configuration.get("longPauseLabel").setValue(getLongPauseLabel());
    }

    if (!configuration.containsKey("maxUtteranceDuration")) {
      configuration.addParameter(
        new Parameter(
          "maxUtteranceDuration", Double.class, 
          "Maximum Utterance Duration (s)",
          "Utterances longer than this will be split on longer inter-word pauses,"
          +" where possible.", false));
    }
    if (configuration.get("maxUtteranceDuration").getValue() == null) {
      configuration.get("maxUtteranceDuration").setValue(getMaxUtteranceDuration());
    }
    
    if (!configuration.containsKey("utterancePadding")) {
      configuration.addParameter(
        new Parameter(
          "utterancePadding", Double.class, 
          "Utterance Padding (s)",
          "Maximum number of seconds to subtract from the start time and"
          +" add to the end time of each utterance, to allow for alignment"
          +" errors of first/last word in each segment.", false));
    }
    if (configuration.get("utterancePadding").getValue() == null) {
      configuration.get("utterancePadding").setValue(getUtterancePadding());
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

    // take the first stream JSON stream and ignore all others
    NamedStream transcriptStream = Utility.FindSingleStream(streams, ".json", "application/json");
    if (transcriptStream != null) { // JSON stream
      setJson(
        Json.createReader(new InputStreamReader(transcriptStream.getStream(), "UTF-8"))
        .readObject());
      if (!json.containsKey("segments")) {
        throw new SerializationException(
          "Stream " + transcriptStream.getName() + " has no \"segments\" attribute.");
      }
    } else { // no JSON stream
      // look for a stdout stream (i.e. the output of the "whisper" command)
      transcriptStream = Utility.FindSingleStream(streams, ".wt", "text/whisper+plain");
      setTranscript(
        new BufferedReader(new InputStreamReader(transcriptStream.getStream(), "UTF-8")));
      if (transcriptStream == null) { // no stdout stream
        throw new SerializationException("No Whisper transcript stream found");
      }
    } // no JSON stream
    
    setName(transcriptStream.getName());
    
    return new ParameterSet();
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

    // deserialize from the output of the command
    if (getName().endsWith(".json")) {
      return deserializeJSON();
    } else {
      return deserializeStdOut();
    }
  }
  
  /**
   * Deserializes the transcript from the JSON file written by Whisper.
   * <p> This format is preferable because it includes word alignments and
   * may include speaker IDs (if it's the output from WhisperX).
   * @return A list of valid (if incomplete) {@link Graph}s. 
   * @throws SerializerNotConfiguredException if the object has not been configured.
   * @throws SerializationParametersMissingException if the parameters for this particular
   * graph have not been set. 
   * @throws SerializationException if errors occur during deserialization.
   */
  protected Graph[] deserializeJSON() 
    throws SerializerNotConfiguredException, SerializationParametersMissingException,
    SerializationException {
    // if there are errors, accumlate as many as we can before throwing SerializationException
    if (timers != null) timers.start("deserialize");
    if (schema == null) throw new SerializerNotConfiguredException("Layer schema not set");
    
    if (shortPauseLabel == null) shortPauseLabel = ""; 
    if (mediumPauseLabel == null) mediumPauseLabel = "";
    if (longPauseLabel == null) longPauseLabel = "";
    if ((shortPauseLabel+mediumPauseLabel+longPauseLabel).length() > 0) { // pause labels
      if (minShortPauseLength == null) {
        throw new SerializerNotConfiguredException("minShortPauseLength not set");
      }
      if (minMediumPauseLength == null) {
        throw new SerializerNotConfiguredException("minMediumPauseLength not set");
      }
      if (minLongPauseLength == null) {
        throw new SerializerNotConfiguredException("minLongPauseLength not set");
      }
    }
    // create pause formatters if the label definitions include {...}
    DecimalFormat shortPauseFormat = !shortPauseLabel.matches("(.*)\\{(.+)\\}(.*)")?null
      :new DecimalFormat(shortPauseLabel.replaceAll("(.*)\\{(.+)\\}(.*)", "$2"));
    DecimalFormat mediumPauseFormat = !mediumPauseLabel.matches("(.*)\\{(.+)\\}(.*)")?null
      :new DecimalFormat(mediumPauseLabel.replaceAll("(.*)\\{(.+)\\}(.*)", "$2"));
    DecimalFormat longPauseFormat = !longPauseLabel.matches("(.*)\\{(.+)\\}(.*)")?null
      :new DecimalFormat(longPauseLabel.replaceAll("(.*)\\{(.+)\\}(.*)", "$2"));

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

    graph.setOffsetUnits(Constants.UNIT_SECONDS);
    graph.setOffsetGranularity(Constants.GRANULARITY_MILLISECONDS);

    // use a default speaker named after the file
    Annotation currentTurn = null;
    Annotation lastWord = null;
    int utteranceOrdinal = 0;
    Anchor lastUtteranceEnd = null;
    HashMap<String,Annotation> participants = new HashMap<String,Annotation>();
    int initialUtteranceBoundaryConfidence
      = utterancePadding == null?Constants.CONFIDENCE_MANUAL
      :Constants.CONFIDENCE_AUTOMATIC;
    
    JsonArray jsonSegments = json.getJsonArray("segments");
    // each segment is an utterance
    for (JsonObject segment : jsonSegments.getValuesAs(JsonObject.class)) {
      Anchor start = graph.getOrCreateAnchorAt(
        segment.getJsonNumber("start").doubleValue(), initialUtteranceBoundaryConfidence);
      Anchor end = graph.getOrCreateAnchorAt(
        segment.getJsonNumber("end").doubleValue(), initialUtteranceBoundaryConfidence);
      if (start.getOffset() > end.getOffset()) { // backwards segment
        warnings.add("Utterance " + start + "-" + end + " backwards.");
        Anchor originalStart = start;
        start = end;
        end = originalStart;
      }
      String speaker = graph.getId();
      if (segment.containsKey("speaker")) { // explicit speaker
        speaker = segment.getString("speaker");
      } else if (currentTurn != null) { // no explicit speaker but we're in a turn
        // just continue with the last speaker
        speaker = currentTurn.getLabel();
      }
      if (!participants.containsKey(speaker)) {
        participants.put(
          speaker, graph.addAnnotation(
            new Annotation(null, speaker, schema.getParticipantLayerId())));
        participants.get(speaker).setConfidence(Constants.CONFIDENCE_AUTOMATIC);
      }
      
      if (currentTurn == null || !currentTurn.getLabel().equals(speaker) // speaker change
          // or last utterance was a while ago
          || (utterancePadding != null && lastUtteranceEnd != null
              && start.getOffset() - lastUtteranceEnd.getOffset()
              > utterancePadding * 2)) {
        currentTurn = new Annotation(
          null, speaker, schema.getTurnLayerId(),
          start.getId(), end.getId(), participants.get(speaker).getId());
        currentTurn.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
        graph.addAnnotation(currentTurn);

        // don't add inter-word pause labels across turns
        lastWord = null;
        utteranceOrdinal = 0;
      }
      Annotation utterance = new Annotation(
        null, speaker, schema.getUtteranceLayerId(),
        start.getId(), end.getId(), currentTurn.getId(),
        ++utteranceOrdinal, Constants.CONFIDENCE_AUTOMATIC);
      currentTurn.setEndId(end.getId());
      graph.addAnnotation(utterance);
      
      if (!segment.containsKey("words")) { // no individual word tokens
        utterance.setLabel(segment.getString("text"));
      } else { // there are individual word tokens
        JsonArray words = segment.getJsonArray("words");
        Anchor lastEnd = start;
        // each word...
        for (JsonObject word : words.getValuesAs(JsonObject.class)) {
          Anchor wordStart = word.containsKey("start")?
            graph.createAnchorAt(
              word.getJsonNumber("start").doubleValue(), Constants.CONFIDENCE_AUTOMATIC)
            : lastEnd;
          Anchor wordEnd = word.containsKey("end")?
            graph.createAnchorAt(
              word.getJsonNumber("end").doubleValue(), Constants.CONFIDENCE_AUTOMATIC)
            : graph.addAnchor(new Anchor());
          Annotation thisWord = new Annotation(
            null, word.getString("word"), schema.getWordLayerId(),
            wordStart.getId(), wordEnd.getId(), currentTurn.getId());
          thisWord.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
          graph.addAnnotation(thisWord);

          if (lastWord != null
              && lastWord.getEnd().getOffset() != null
              && wordStart.getOffset() != null) {
            double interWordPause
              = wordStart.getOffset() - lastWord.getEnd().getOffset();
            if (interWordPause > minShortPauseLength) { // inter-word pause
              if (interWordPause <= minMediumPauseLength) { // short pause
                if (shortPauseLabel.length() > 0) {
                  lastWord.setLabel(lastWord.getLabel() + " " + shortPauseLabel);
                }
              } else if (interWordPause <= minLongPauseLength) { // medium pause
                if (mediumPauseLabel.length() > 0) {
                  lastWord.setLabel(lastWord.getLabel() + " " + mediumPauseLabel);
                }
              } else { // long pause
                lastWord.setLabel(
                  pauseLabel(
                    lastWord.getLabel(), interWordPause, longPauseLabel, longPauseFormat));
              }
            } // inter-word pause
          }
          
          lastWord = thisWord;
        } // next word
        if (lastWord.getEnd().getOffset() == null) { // last word had no "end"
          // set the end of the last word to be the end of the utterance
          lastWord.setEndId(end.getId());
        }        
      } // there are individual word tokens

      lastUtteranceEnd = utterance.getEnd();
    } // next segment

    if (maxUtteranceDuration != null) {
      // look for utterances that are too long, and split them on the longest pauses
      LinkedList<Annotation> utteranceQueue = new LinkedList<Annotation>(
        new AnnotationsByAnchor​((graph.all(schema.getUtteranceLayerId()))));
      while (!utteranceQueue.isEmpty()) {
        Annotation utterance = utteranceQueue.remove();
        if (utterance.getDuration() > maxUtteranceDuration) { // utterance too long
          // split the utterance into two at the longest inter-word pause
          Annotation splitAfter = null;
          Annotation splitBefore = null;
          double splitPauseDuration = 0.0;
          Annotation previousWord = null;
          for (Annotation word : utterance.all(schema.getWordLayerId())) {
            if (previousWord != null && previousWord.getEnd().getOffset() != null
                && word.getStart().getOffset() != null) {
              double interWordPauseDuration
                = word.getStart().getOffset() - previousWord.getEnd().getOffset();
              if (interWordPauseDuration > splitPauseDuration) {
                splitAfter = previousWord;
                splitBefore = word;
                splitPauseDuration = interWordPauseDuration;
              }
            }
            previousWord = word;
          } // next word
          
          if (splitBefore != null) { // found a split point
            
            // new utterance covers the last half
            Annotation newUtterance = new Annotation(
              null, utterance.getLabel(), schema.getUtteranceLayerId(),
              splitBefore.getStartId(), utterance.getEndId(),
              utterance.getParentId(), utterance.getOrdinal() + 1,
              Constants.CONFIDENCE_AUTOMATIC);
            graph.addAnnotation(newUtterance);
            utteranceQueue.offer(newUtterance); // might still be too long
            
            // existing utterance covers the first half
            utterance.setEnd(splitAfter.getEnd());
            utteranceQueue.offer(utterance); // might still be too long
            
          } // split point identified
          
        } // utterance is too long
      } // there are still utterances to check
    } // maxUtteranceDuration is set

    if (utterancePadding != null) { // pad utterances where possible
      // utterance boundaries are often the first/last word boundaries
      // but the word boundaries may not be precise, so we pad utterances
      // where possible to ensure they entirely cover the words they contain
      Annotation previousUtterance = null;
      for (Annotation utterance :new  AnnotationsByAnchor​(
             graph.all(schema.getUtteranceLayerId()))) {
        if (previousUtterance == null) { // first utterance
          Anchor newStart = graph.getOrCreateAnchorAt(
            Math.max(utterance.getStart().getOffset() - utterancePadding, 0.0),
            Constants.CONFIDENCE_MANUAL);
          if (utterance.getStartId().equals(utterance.getParent().getStartId())) {
            // shares start with parent
            utterance.getParent().setStart(newStart);
          }
          utterance.setStart(newStart);
        } else { // there is a previous utterance
          if (utterance.getStart().getOffset() - previousUtterance.getEnd().getOffset()
              > utterancePadding*2) { // the previous utterance was too long ago
            Anchor newStart = graph.createAnchorAt(
              utterance.getStart().getOffset() - utterancePadding,
              Constants.CONFIDENCE_MANUAL);
              if (utterance.getStartId().equals(utterance.getParent().getStartId())) {
                // shares start with parent
                utterance.getParent().setStart(newStart);
              }
            utterance.setStart(newStart);
            Anchor newEnd = graph.createAnchorAt(
              previousUtterance.getEnd().getOffset() + utterancePadding,
              Constants.CONFIDENCE_MANUAL);
            if (previousUtterance.getEndId().equals(
                  previousUtterance.getParent().getEndId())) {
              // shares end with parent
              previousUtterance.getParent().setEnd(newEnd);
            }
            previousUtterance.setEnd(newEnd);
          } else { // the previous utterance could share an anchor with this one
            // set the shared offset as halfway between them
            Anchor newShared = graph.createAnchorAt(
              previousUtterance.getEnd().getOffset()
              + ((utterance.getStart().getOffset()
                  - previousUtterance.getEnd().getOffset())/2),
              Constants.CONFIDENCE_MANUAL);
              if (utterance.getStartId().equals(utterance.getParent().getStartId())) {
                // shares start with parent
                utterance.getParent().setStart(newShared);
              }
            utterance.setStart(newShared);
            if (previousUtterance.getEndId().equals(
                  previousUtterance.getParent().getEndId())) {
              // shares end with parent
              previousUtterance.getParent().setEnd(newShared);
            }
            previousUtterance.setEnd(utterance.getStart());
          }
        } // there is a previous utterance
        previousUtterance = utterance;
      } // next utterance
      // we don't pad the last utterance, because we don't know if that would
      // go beyond the end of the media
      if (previousUtterance != null) {
        previousUtterance.getEnd().setConfidence(Constants.CONFIDENCE_MANUAL);
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
  } // deserializeJSON
  
  /**
   * Determines the pause label, given the inter-word pause, and the
   * format definitions for its label 
   * @param priorWordLabel The label of the word prior to the pause.
   * @param interWordPause The magnitude of the inter-word pause.
   * @param pauseLabel The pause label definition, which can be null.
   * @param pauseLabelFormat The format for rendering the pause
   * length, which may be null.
   * @return The new label for the prior word.
   */
  public String pauseLabel(String priorWordLabel, double interWordPause, String pauseLabel, DecimalFormat pauseLabelFormat) {
    if (pauseLabel == null || pauseLabel.length() == 0) return priorWordLabel;
    if (pauseLabelFormat == null) {
      return priorWordLabel + " " + pauseLabel;
    }
    return priorWordLabel + " " +
      pauseLabel.replaceAll(
        "(.*)\\{(.+)\\}(.*)",
        "$1"+pauseLabelFormat.format(interWordPause)+"$3");
  } // end of pauseLabel()
  
   
  /**
   * Deserializes the transcript from the output of the "whisper" command.
   * @return A list of valid (if incomplete) {@link Graph}s. 
   * @throws SerializerNotConfiguredException if the object has not been configured.
   * @throws SerializationParametersMissingException if the parameters for this particular
   * graph have not been set. 
   * @throws SerializationException if errors occur during deserialization.
   */
  protected Graph[] deserializeStdOut() 
    throws SerializerNotConfiguredException, SerializationParametersMissingException,
    SerializationException {
    // if there are errors, accumlate as many as we can before throwing SerializationException
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

    graph.setOffsetUnits(Constants.UNIT_SECONDS);
    graph.setOffsetGranularity(Constants.GRANULARITY_MILLISECONDS);

    // all lines should be like the following 
    // [00:00.000 --> 00:05.600]  Well I have a fairly vivid recollection.
    MessageFormat utteranceFormat = new MessageFormat(
      "[{0,number,integer}:{1,number,integer}.{2,number,integer}"
      +" --> "
      +"{3,number,integer}:{4,number,integer}.{5,number,integer}]{6}");

    // use a default speaker named after the file
    Annotation speaker = graph.addAnnotation(
      new Annotation(null, name, schema.getParticipantLayerId()));
    speaker.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
    Annotation currentTurn = null;
    Annotation currentUtterance = null;
    try {
      // For each line...
      String line = transcript.readLine();
      while (line != null) {
        if (line.trim().length() == 0) continue; // skip blank lines
        try {
          Object[] utteranceLine = utteranceFormat.parse(line);
          // this is an utterance line...
          
          Long minutes = (Long)utteranceLine[0];
          Long seconds = (Long)utteranceLine[1];
          Long milliseconds = (Long)utteranceLine[2];
          Anchor start = graph.getOrCreateAnchorAt(
            minutes * 60 + seconds + milliseconds.doubleValue()/1000,
            Constants.CONFIDENCE_MANUAL);
                  
          minutes = (Long)utteranceLine[3];
          seconds = (Long)utteranceLine[4];
          milliseconds = (Long)utteranceLine[5];
          Anchor end = graph.getOrCreateAnchorAt(
            minutes * 60 + seconds + milliseconds.doubleValue()/1000,
            Constants.CONFIDENCE_MANUAL);

          String transcript = ((String)utteranceLine[6]).trim();
          if (transcript.length() > 0) {

            // if this start time isn't equal to the last end time...
            if (currentUtterance == null || start != currentUtterance.getEnd()) {
              currentTurn = new Annotation(
                null, speaker.getLabel(), schema.getTurnLayerId(),
                start.getId(), end.getId(), speaker.getId());
              currentTurn.setConfidence(Constants.CONFIDENCE_MANUAL);
              graph.addAnnotation(currentTurn);
            }            
            
            currentUtterance = new Annotation(
              null, transcript, schema.getUtteranceLayerId(),
              start.getId(), end.getId(), currentTurn.getId());
            currentUtterance.setConfidence(Constants.CONFIDENCE_MANUAL);
            currentTurn.setEndId(end.getId());
            graph.addAnnotation(currentUtterance);
          }
        } catch(ParseException exception) {
          warnings.add("Invalid line: \""+line+"\"");
        }
        
        line = transcript.readLine();
      } // next line
	 
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
	    
        OrthographyClumper clumper = new OrthographyClumper(
          wordLayer.getId(), utteranceLayer.getId());
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
        
        // utterance annotations are speaker labels then
        for (Annotation utterance : graph.all(utteranceLayer.getId())) {
          utterance.setLabel(utterance.getParent().getLabel());
        } // next turn
      } // there is a word layer
      graph.commit();
    } catch(IOException exception) {
      if (errors == null) errors = new SerializationException();
      if (errors.getCause() == null) errors.initCause(exception);
      errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
    }
    
    if (errors != null) throw errors;
    
    // reset all change tracking
    if (graph.getTracker() != null) {
      graph.getTracker().reset();
      graph.setTracker(null);
    }
    
    Graph[] graphs = { graph };
    if (timers != null) timers.end("deserialize");
    return graphs;
  } // deserializeStdOut
   
  private static final MessageFormat timeFormatter = new MessageFormat(
    "{0,number,00}:{1,number,00}:{2,number,00.000}", new Locale("en"));

} // end of class WhisperDeserializer
