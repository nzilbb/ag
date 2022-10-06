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
package nzilbb.formatter.whisper;

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
   * Reader for transcript stream.
   * @see #getTranscript()
   * @see #setTranscript(BufferedReader)
   */
  protected BufferedReader transcript;
  /**
   * Getter for {@link #transcript}: Reader for transcript stream.
   * @return Reader for transcript stream.
   */
  public BufferedReader getTranscript() { return transcript; }
  /**
   * Setter for {@link #transcript}: Reader for transcript stream.
   * @param newTranscript Reader for transcript stream.
   */
  public void setTranscript(BufferedReader newTranscript) { transcript = newTranscript; }

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
   * {@link GraphDeserializer#setParameters()} can be invoked. If this is an empty list,
   * {@link GraphDeserializer#setParameters()} can be invoked. If it's not an empty list, this
   * method must be invoked again with the returned parameters' values set. 
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
    NamedStream transcriptStream = Utility.FindSingleStream(streams, ".wt", "text/whisper+plain");
    if (transcriptStream == null)
      throw new SerializationException("No Whisper transcript stream found");
    setName(transcriptStream.getName());
    setTranscript(
      new BufferedReader(new InputStreamReader(transcriptStream.getStream(), "UTF-8")));

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
  }
   
  private static final MessageFormat timeFormatter = new MessageFormat(
    "{0,number,00}:{1,number,00}:{2,number,00.000}", new Locale("en"));

} // end of class WhisperDeserializer
