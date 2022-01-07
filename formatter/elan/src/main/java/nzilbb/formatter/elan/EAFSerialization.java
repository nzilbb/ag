//
// Copyright 2017-2021 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.formatter.elan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import nzilbb.ag.*;
import nzilbb.ag.cli.Deserialize;
import nzilbb.ag.serialize.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.util.AnnotationComparatorByAnchor;
import nzilbb.ag.util.Coalescer;
import nzilbb.ag.util.ConventionTransformer;
import nzilbb.ag.util.OrthographyClumper;
import nzilbb.ag.util.SimpleTokenizer;
import nzilbb.ag.util.SpanningConventionTransformer;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.util.IO;
import nzilbb.util.ISO639;
import nzilbb.util.Switch;
import nzilbb.util.TempFileInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

// TODO serialize using token-lt (Symbolic subdivision) for layers that are alignment=2,peers, and no non-parent-linked anchors are aligned or have confidence above CONFIDENCE_DEFAULT

/**
 * Converter that converts ELAN EAF v2.7 files to Annotation Graphs
 * <p>The original XSD is here:
 * <a href="http://www.mpi.nl/tools/elan/EAFv2.7.xsd">http://www.mpi.nl/tools/elan/EAFv2.7.xsd</a>
 * @author Robert Fromont robert@fromont.net.nz
 */
public class EAFSerialization extends Deserialize implements GraphDeserializer, GraphSerializer {
   
  // Attributes:
  private File eafFile;
  private String timeUnits = "milliseconds";
  private ISO639 iso639 = new ISO639(); // for standard ISO 639 language code processing
  protected Vector<String> warnings;
  /**
   * Returns any warnings that may have arisen during the last execution of 
   * {@link #deserialize()}.
   * @return A possibly empty list of warnings.
   */
  public String[] getWarnings() {
    return warnings.toArray(new String[0]);
  }

  /** Time value multiplier, to yield time in seconds */
  protected double timeFactor = (1.0/1000.0);

  /**
   * A message for each tier, filled in during conversion.
   * @see #getTierMessages()
   * @see #setTierMessages(HashMap)
   */
  protected HashMap<String,String> mTierMessages = new HashMap<String,String>();
  /**
   * Getter for {@link #mTierMessages}: A message for each tier, filled in during conversion.
   * @return A message for each tier, filled in during conversion.
   */
  public HashMap<String,String> getTierMessages() { return mTierMessages; }
  /**
   * Setter for {@link #mTierMessages}: A message for each tier, filled in during conversion.
   * @param mNewTierMessages A message for each tier, filled in during conversion.
   */
  public EAFSerialization setTierMessages(HashMap<String,String> mNewTierMessages) { mTierMessages = mNewTierMessages; return this; }

  /**
   * Whether 'utterance' intervals (those which map to either {@link Labbcat#LAYER_TURN}
   * or {@link Labbcat#LAYER_UTTERANCE}) contain the name of the speaker (true) or the
   * words spoken (false - in which case the tier name is assumed to be the speaker name)
   * @see #getUtterancesAreSpeakerNames()
   * @see #setUtterancesAreSpeakerNames(boolean)
   */
  protected boolean utterancesAreSpeakerNames = false;
  /**
   * Getter for {@link #utterancesAreSpeakerNames}: Whether 'utterance' intervals (those
   * which map to either {@link Labbcat#LAYER_TURN} or {@link Labbcat#LAYER_UTTERANCE})
   * contain the name of the speaker (true) or the words spoken (false - in which case
   * the tier name is assumed to be the speaker name)
   * @return Whether 'utterance' intervals (those which map to either {@link
   * Labbcat#LAYER_TURN} or {@link Labbcat#LAYER_UTTERANCE}) contain the name of the
   * speaker (true) or the words spoken (false - in which case the tier name is assumed
   * to be the speaker name)
   */
  public boolean getUtterancesAreSpeakerNames() { return utterancesAreSpeakerNames; }
  /**
   * Setter for {@link #utterancesAreSpeakerNames}: Whether 'utterance' intervals (those
   * which map to either {@link Labbcat#LAYER_TURN} or {@link Labbcat#LAYER_UTTERANCE})
   * contain the name of the speaker (true) or the words spoken (false - in which case
   * the tier name is assumed to be the speaker name)
   * @param bNewUtterancesAreSpeakerNames Whether 'utterance' intervals (those which map
   * to either {@link Labbcat#LAYER_TURN} or {@link Labbcat#LAYER_UTTERANCE}) contain the
   * name of the speaker (true) or the words spoken (false - in which case the tier name
   * is assumed to be the speaker name)
   */
  public EAFSerialization setUtterancesAreSpeakerNames(boolean newUtterancesAreSpeakerNames) { utterancesAreSpeakerNames = newUtterancesAreSpeakerNames; return this; }
   
  /**
   * Layer schema.
   * @see #getSchema()
   * @see #setSchema(Schema)
   */
  protected Schema schema;
  /**
   * Getter for {@link #schema}: Layer schema.
   * @return Layer schema.
   */
  public Schema getSchema() { return schema; }
  /**
   * Setter for {@link #schema}: Layer schema.
   * @param newSchema Layer schema.
   */
  public EAFSerialization setSchema(Schema newSchema) { schema = newSchema; return this; }

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
  public EAFSerialization setParticipantLayer(Layer newParticipantLayer) { participantLayer = newParticipantLayer; return this; }

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
  public EAFSerialization setTurnLayer(Layer newTurnLayer) { turnLayer = newTurnLayer; return this; }

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
  public EAFSerialization setUtteranceLayer(Layer newUtteranceLayer) { utteranceLayer = newUtteranceLayer; return this; }

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
  public EAFSerialization setWordLayer(Layer newWordLayer) { wordLayer = newWordLayer; return this; }

  /**
   * Layer for lexical word tags.
   * @see #getLexicalLayer()
   * @see #setLexicalLayer(Layer)
   */
  protected Layer lexicalLayer;
  /**
   * Getter for {@link #lexicalLayer}: Layer for lexical word tags.
   * @return Layer for lexical word tags.
   */
  public Layer getLexicalLayer() { return lexicalLayer; }
  /**
   * Setter for {@link #lexicalLayer}: Layer for lexical word tags.
   * @param newLexicalLayer Layer for lexical word tags.
   */
  public EAFSerialization setLexicalLayer(Layer newLexicalLayer) { lexicalLayer = newLexicalLayer; return this; }

  /**
   * Layer for pronounce events.
   * @see #getPronounceLayer()
   * @see #setPronounceLayer(Layer)
   */
  protected Layer pronounceLayer;
  /**
   * Getter for {@link #pronounceLayer}: Layer for pronounce events.
   * @return Layer for pronounce events.
   */
  public Layer getPronounceLayer() { return pronounceLayer; }
  /**
   * Setter for {@link #pronounceLayer}: Layer for pronounce events.
   * @param newPronounceLayer Layer for pronounce events.
   */
  public EAFSerialization setPronounceLayer(Layer newPronounceLayer) { pronounceLayer = newPronounceLayer; return this; }

  /**
   * Layer for commentary.
   * @see #getCommentLayer()
   * @see #setCommentLayer(Layer)
   */
  protected Layer commentLayer;
  /**
   * Getter for {@link #commentLayer}: Layer for commentary.
   * @return Layer for commentary.
   */
  public Layer getCommentLayer() { return commentLayer; }
  /**
   * Setter for {@link #commentLayer}: Layer for commentary.
   * @param newCommentLayer Layer for commentary.
   */
  public EAFSerialization setCommentLayer(Layer newCommentLayer) { commentLayer = newCommentLayer; return this; }

  /**
   * Layer for noise annotations.
   * @see #getNoiseLayer()
   * @see #setNoiseLayer(Layer)
   */
  protected Layer noiseLayer;
  /**
   * Getter for {@link #noiseLayer}: Layer for noise annotations.
   * @return Layer for noise annotations.
   */
  public Layer getNoiseLayer() { return noiseLayer; }
  /**
   * Setter for {@link #noiseLayer}: Layer for noise annotations.
   * @param newNoiseLayer Layer for noise annotations.
   */
  public EAFSerialization setNoiseLayer(Layer newNoiseLayer) { noiseLayer = newNoiseLayer; return this; }

  /**
   * Layer for the author/transcriber.
   * @see #getAuthorLayer()
   * @see #setAuthorLayer(Layer)
   */
  protected Layer authorLayer;
  /**
   * Getter for {@link #AuthorLayer}: Layer for the author/transcriber.
   * @return Layer for the author/transcriber.
   */
  public Layer getAuthorLayer() { return authorLayer; }
  /**
   * Setter for {@link #AuthorLayer}: Layer for the author/transcriber.
   * @param newAuthorLayer Layer for the author/transcriber.
   */
  public EAFSerialization setAuthorLayer(Layer newAuthorLayer) { authorLayer = newAuthorLayer; return this; }

  /**
   * Layer for the document date.
   * @see #getDateLayer()
   * @see #setDateLayer(Layer)
   */
  protected Layer dateLayer;
  /**
   * Getter for {@link #DateLayer}: Layer for the document date.
   * @return Layer for the document date.
   */
  public Layer getDateLayer() { return dateLayer; }
  /**
   * Setter for {@link #DateLayer}: Layer for the document date.
   * @param newDateLayer Layer for the document date.
   */
  public EAFSerialization setDateLayer(Layer newDateLayer) { dateLayer = newDateLayer; return this; }

  /**
   * Layer for the document language.
   * @see #getLanguageLayer()
   * @see #setLanguageLayer(Layer)
   */
  protected Layer languageLayer;
  /**
   * Getter for {@link #LanguageLayer}: Layer for the document language.
   * @return Layer for the document language.
   */
  public Layer getLanguageLayer() { return languageLayer; }
  /**
   * Setter for {@link #LanguageLayer}: Layer for the document language.
   * @param newLanguageLayer Layer for the document language.
   */
  public EAFSerialization setLanguageLayer(Layer newLanguageLayer) { languageLayer = newLanguageLayer; return this; }

  /**
   * Layer for possible phrase language tags.
   * @see #getPhraseLanguageLayer()
   * @see #setPhraseLanguageLayer(Layer)
   */
  protected Layer phraseLanguageLayer;
  /**
   * Getter for {@link #phraseLanguageLayer}: Layer for possible phrase language tags.
   * @return Layer for possible phrase language tags.
   */
  public Layer getPhraseLanguageLayer() { return phraseLanguageLayer; }
  /**
   * Setter for {@link #phraseLanguageLayer}: Layer for possible phrase language tags.
   * @param newPhraseLanguageLayer Layer for possible phrase language tags.
   */
  public EAFSerialization setPhraseLanguageLayer(Layer newPhraseLanguageLayer) { phraseLanguageLayer = newPhraseLanguageLayer; return this; }
  
  /**
   * Whether to use text conventions for comment, noise, lexical, and pronounce annotations.
   * @see #getUseConventions()
   * @see #setUseConventions(Boolean)
   */
  protected Boolean bUseConventions = Boolean.TRUE;
  /**
   * Getter for {@link #bUseConventions}: Whether to use text conventions for comment, noise, lexical, and pronounce annotations.
   * @return Whether to use text conventions for comment, noise, lexical, and pronounce annotations.
   */
  public Boolean getUseConventions() { return bUseConventions; }
  /**
   * Setter for {@link #bUseConventions}: Whether to use text conventions for comment, noise, lexical, and pronounce annotations.
   * @param bNewTranscriptOnly Whether to use text conventions for comment, noise, lexical, and pronounce annotations.
   */
  @Switch("Use text conventions for comment, noise, lexical, and pronounce annotations")
  public EAFSerialization setUseConventions(Boolean bNewUseConventions) { bUseConventions = bNewUseConventions; return this; }

  /**
   * Whether to ignore annotations with no label (true), or to include them as
   * blank-labelled annotations (false).
   * @see #getIgnoreBlankAnnotations()
   * @see #setIgnoreBlankAnnotations(Boolean)
   */
  protected Boolean ignoreBlankAnnotations = Boolean.TRUE;
  /**
   * Getter for {@link #ignoreBlankAnnotations}: Whether to ignore annotations with no
   * label (true), or to include them as blank-labelled annotations (false).
   * @return Whether to ignore annotations with no label (true), or to include them as
   * blank-labelled annotations (false).
   */
  public Boolean getIgnoreBlankAnnotations() { return ignoreBlankAnnotations; }
  /**
   * Setter for {@link #ignoreBlankAnnotations}: Whether to ignore annotations with no
   * label (true), or to include them as blank-labelled annotations (false).
   * @param newIgnoreBlankAnnotations Whether to ignore annotations with no label (true),
   * or to include them as blank-labelled annotations (false).
   */
  @Switch("Ignore annotations with no label")
  public EAFSerialization setIgnoreBlankAnnotations(Boolean newIgnoreBlankAnnotations) { ignoreBlankAnnotations = newIgnoreBlankAnnotations; return this; }
   
  /**
   * Minimum amount of time between two turns by the same speaker, with no intervening
   * speaker, for which the inter-turn pause counts as a turn change boundary. If the
   * pause is shorter than this, the turns are merged into one. Default is 0.0;
   * @see #getMinimumTurnPauseLength()
   * @see #setMinimumTurnPauseLength(Double)
   */
  protected Double minimumTurnPauseLength = Double.valueOf(0.0);
  /**
   * Getter for {@link #minimumTurnPauseLength}: Minimum amount of time between two turns
   * by the same speaker, with no intervening speaker, for which the inter-turn pause
   * counts as a turn change boundary. If the pause is shorter than this, the turns are
   * merged into one.
   * @return Minimum amount of time between two turns by the same speaker, with no
   * intervening speaker, for which the inter-turn pause counts as a turn change
   * boundary. If the pause is shorter than this, the turns are merged into one.
   */
  @Switch("Inter-turn pauses shorter than this are merged into one turn")
  public Double getMinimumTurnPauseLength() {
    if (minimumTurnPauseLength == null) minimumTurnPauseLength = Double.valueOf(0.0);
    return minimumTurnPauseLength;
  }
  /**
   * Setter for {@link #minimumTurnPauseLength}: Minimum amount of time between two turns
   * by the same speaker, with no intervening speaker, for which the inter-turn pause
   * counts as a turn change boundary. If the pause is shorter than this, the turns are
   * merged into one.
   * @param newMinimumTurnPauseLength Minimum amount of time between two turns by the
   * same speaker, with no intervening speaker, for which the inter-turn pause counts as
   * a turn change boundary. If the pause is shorter than this, the turns are merged into
   * one.
   */
  public EAFSerialization setMinimumTurnPauseLength(Double newMinimumTurnPauseLength) { minimumTurnPauseLength = newMinimumTurnPauseLength; return this; }

   
  // IStreamDeserializer methods:
   
  /**
   * Returns the deserializer's descriptor
   * @return The deserializer's descriptor
   */
  public SerializationDescriptor getDescriptor() {
    return new SerializationDescriptor(
      "ELAN EAF Transcript", getClass().getPackage().getImplementationVersion(),
      "text/x-eaf+xml", ".eaf", "1.0.5",
      getClass().getResource("icon.png"));
  }
   
  /**
   * Graph ID.
   * @see #getId()
   * @see #setId(String)
   */
  protected String id;
  /**
   * Getter for {@link #id}: Graph ID.
   * @return Graph ID.
   */
  public String getId() { return id; }
  /**
   * Setter for {@link #id}: Graph ID.
   * @param newId Graph ID.
   */
  public EAFSerialization setId(String newId) { id = newId; return this; }

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
  public EAFSerialization setTokenizer(GraphTransformer newTokenizer) { tokenizer = newTokenizer; return this; }

  private boolean mappingsDependOnTurn = false;
   
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
  public EAFSerialization setCancelling(boolean newCancelling) { cancelling = newCancelling; return this; }
  /**
   * Cancel the serialization in course (if any).
   */
  public void cancel() {
    setCancelling(true);
  }
   
  // Methods:
   
  /**
   * Constructor
   */
  public EAFSerialization() {
  } // end of constructor
   
  /**
   * Resets the state of the converter, ready to convert again.
   */
  public void reset() {
    warnings = new Vector<String>();
    timeFactor = (1.0/1000.0);
    utterancesAreSpeakerNames = false;
    id = null;
    mTierMessages.clear();
  } // end of reset()

   
  // GraphDeserializer methods

  protected ParameterSet mappings;

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
   * {@link GraphDeserializer#setParameters()} can be invoked. If this is an empty list,
   * {@link GraphDeserializer#setParameters()} can be invoked. If it's not an empty list,
   * this method must be invoked again with the returned parameters' values set. 
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
    LinkedHashMap<String,Layer> possiblePhraseLayers = new LinkedHashMap<String,Layer>();
    for (String id : possibleTurnChildLayers.keySet()) {
      if (!id.equals(schema.getUtteranceLayerId())
          && !id.equals(schema.getWordLayerId())) {
        possiblePhraseLayers.put(id, possibleTurnChildLayers.get(id));
      }
    } // next aligned turn child layer

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

    // other layers...

    layerToPossibilities.put(
      new Parameter("commentLayer", Layer.class, "Comment layer", "Commentary"), 
      Arrays.asList("comment","commentary","note","notes"));
    layerToCandidates.put("commentLayer", topLevelLayers);

    layerToPossibilities.put(
      new Parameter("noiseLayer", Layer.class, "Noise layer", "Noise annotations"), 
      Arrays.asList("noise","noises","backgroundnoise"));
    layerToCandidates.put("noiseLayer", topLevelLayers);

    layerToPossibilities.put(
      new Parameter("lexicalLayer", Layer.class, "Lexical layer", "Lexical tags"), 
      Arrays.asList("lexical"));
    layerToCandidates.put("lexicalLayer", wordTagLayers);

    layerToPossibilities.put(
      new Parameter("pronounceLayer", Layer.class, "Pronounce layer", 
                    "Manual pronunciation tags"), 
      Arrays.asList("pronounce"));
    layerToCandidates.put("pronounceLayer", wordTagLayers);

    layerToPossibilities.put(
      new Parameter("authorLayer", Layer.class, "Author layer", "Name of transcriber"), 
      Arrays.asList("transcripttranscriber","transcriptscribe","scribe","transcriber", "author"));
    layerToCandidates.put("authorLayer", graphTagLayers);

    layerToPossibilities.put(
      new Parameter("dateLayer", Layer.class, "Date layer", "Document date"), 
      Arrays.asList("transcriptversiondate","versiondate","transcriptdate","date"));
    layerToCandidates.put("dateLayer", graphTagLayers);

    // transcript language
    layerToPossibilities.put(
      new Parameter("languageLayer", Layer.class, "Transcript Language layer", 
                    "The language of the whole transcript"), 
      Arrays.asList("transcriptlanguage","language","transcriptlang","lang"));
    layerToCandidates.put("languageLayer", graphTagLayers);

    // phrase language
    layerToPossibilities.put(
      new Parameter("phraseLanguageLayer", Layer.class, "Phrase Language layer", 
                    "For tagging individual phrases with a language"), 
      Arrays.asList("phraselanguage","language","phraselang","lang"));
    layerToCandidates.put("phraseLanguageLayer", possiblePhraseLayers);

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

    if (!configuration.containsKey("useConventions")) {
      configuration.addParameter(
        new Parameter("useConventions", Boolean.class, 
                      "Use Annotation Conventions",
                      "Whether to use text conventions for comment, noise, lexical, and pronounce annotations", true));
    }
    if (configuration.get("useConventions").getValue() == null) {
      configuration.get("useConventions").setValue(getUseConventions());
    }

    if (!configuration.containsKey("ignoreBlankAnnotations")) {
      configuration.addParameter(
        new Parameter("ignoreBlankAnnotations", Boolean.class, 
                      "Ignore Blank Annotations",
                      "Whether to skip annotations with no label, or process them", true));
    }
    if (configuration.get("ignoreBlankAnnotations").getValue() == null) {
      configuration.get("ignoreBlankAnnotations").setValue(getIgnoreBlankAnnotations());
    }

    if (!configuration.containsKey("minimumTurnPauseLength")) {
      configuration.addParameter(
        new Parameter("minimumTurnPauseLength", Double.class, 
                      "Min. Turn Pause Length",
                      "Minimum amount of time between two turns by the same speaker,"
                      +" with no intervening speaker, for which the inter-turn pause"
                      +" counts as a turn change boundary. If the pause is shorter than"
                      +" this, the turns are merged into one.", true));
    }
    if (configuration.get("minimumTurnPauseLength").getValue() == null) {
      configuration.get("minimumTurnPauseLength").setValue(getMinimumTurnPauseLength());
    }

    return configuration;
  }   

  /**
   * Loads the serialized form of the graph, using the given set of named streams.
   * @param streams A list of named streams that contain all the
   *  transcription/annotation data required, and possibly (a) stream(s) for the media annotated.
   * @param schema The layer schema, definining layers and the way they interrelate.
   * @return A list of parameters that require setting before {@link GraphDeserializer#deserialize()}
   * can be invoked. This may be an empty list, and may include parameters with the value already
   * set to a workable default. If there are parameters, and user interaction is possible, then
   * the user may be presented with an interface for setting/confirming these parameters, before
   * they are then passed to {@link GraphDeserializer#setParameters(ParameterSet)}.
   * @throws SerializationException If the graph could not be loaded.
   * @throws IOException On IO error.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public ParameterSet load(NamedStream[] streams, Schema schema)
    throws SerializationException, IOException {
      
    reset();
    setSchema(schema);

    // take the first stream, ignore all others.
    NamedStream eaf = Utility.FindSingleStream(streams, ".eaf", "text/x-eaf+xml");
    if (eaf == null) throw new SerializationException("No ELAN EAF stream found");
    setId(eaf.getName());

    // save to a temporary file
    eafFile = File.createTempFile(eaf.getName(), ".eaf");
    IO.SaveInputStreamToFile​(eaf.getStream(), eafFile);
    
    InputStream in = new FileInputStream(eafFile);
    try {
	  
      ParameterSet mappings = new ParameterSet();
      Vector<Layer> vIntervalLayers = new Vector<Layer>();
      for (Layer layer : getSchema().getLayers().values()) {
        if (!layer.getId().equals(getSchema().getRoot().getId())
            && !layer.getId().equals(getParticipantLayer().getId())) {
          switch (layer.getAlignment()) {
            case 0: 
              if (!layer.getParentId().equals(getSchema().getRoot().getId())
                  && !layer.getParentId().equals(getParticipantLayer().getId())) {
                // not graph or participant tags
                vIntervalLayers.add(layer); 
              }
              break; 
            case 2: vIntervalLayers.add(layer); break; 
            case 1: break; // point tiers are not supported in ELAN
          }
        }
      } // next layer

      boolean foundTimeOrder = false;
      boolean foundTiers = false;

      // map tiers to layers by name
      Layer ignore = new Layer();
      ignore.setId("[ignore tier]");
      in = new FileInputStream(eafFile);
      XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(in);
      int t = 0;
      while (parser.hasNext()) {
        int event = parser.next();
        if (event == XMLStreamConstants.START_ELEMENT) {
          if (parser.getLocalName().equals("TIME_ORDER")) {
            foundTimeOrder = true;            
          } else if (parser.getLocalName().equals("TIER")) {
            foundTiers = true;
            String tierName = parser.getAttributeValue(null, "TIER_ID");
            String parent = parser.getAttributeValue(null, "PARENT_REF");
            // Node tier = tiers.item(t);
            // Attr tierId = (Attr)tier.getAttributes().getNamedItem("TIER_ID");
            // String tierName = tierId.getValue();
            // Attr childParticipant = (Attr)tier.getAttributes().getNamedItem("PARTICIPANT");
	     
            Parameter p = new Parameter(
              "tier"+(t++), Layer.class, tierName,
              "Layer for tier called: " + tierName, true);
            Vector<Layer> vPossiblLayers = new Vector<Layer>();
            vPossiblLayers.add(ignore);
            vPossiblLayers.addAll(vIntervalLayers);
	     
            // look for a layer with the same name
            if (tierName.equalsIgnoreCase("lines")
                || tierName.equalsIgnoreCase("utterances")) {
              tierName = getUtteranceLayer().getId();
            } else if (tierName.equalsIgnoreCase("speakers")
                       || tierName.equalsIgnoreCase("speaker")
                       || tierName.equalsIgnoreCase("turns")
                       || tierName.equalsIgnoreCase("turn")) {
              tierName = getTurnLayer().getId();
            } else if (tierName.toLowerCase().indexOf("word") >= 0
                       || tierName.toLowerCase().indexOf("word") >= 0
                       || tierName.toLowerCase().endsWith("transcript")) { // backwards compat.
              tierName = getWordLayer().getId();
            }
            Layer layer = getSchema().getLayer(tierName);
            if (layer == null) { // no exact match
              // try a case-insensitive match
              // ignore spaces too
              String tierNameNoWhitespace = tierName.toLowerCase().replaceAll("\\s","");
              for (Layer mappableLayer : vPossiblLayers) {
                if (tierNameNoWhitespace.equals(
                      mappableLayer.getId().toLowerCase().replaceAll("\\s",""))) {
                  layer = mappableLayer;
                  break;
                }
              } // next layer
            }
            // TODO Noises->noise and COMMENTS->comment
            if (layer == null) { // no exact match
              // try a prefix-match - i.e. "word - John Smith" should map to the "word" layer
              // ignore spaces too
              String tierNameNoWhitespace = tierName.replaceAll("\\s","");
              for (Layer mappableLayer : vPossiblLayers) {
                if (tierNameNoWhitespace.startsWith(mappableLayer.getId().replaceAll("\\s",""))) {
                  layer = mappableLayer;
                  break;
                }
              } // next layer
            }
            if (layer == null) { // no match
              // try a suffix-match - i.e. "John Smith - word" should map to the "word" layer
              // ignore spaces too
              String tierNameNoWhitespace = tierName.replaceAll("\\s","");
              for (Layer mappableLayer : vPossiblLayers) {
                if (tierNameNoWhitespace.endsWith(mappableLayer.getId().replaceAll("\\s",""))) {
                  layer = mappableLayer;
                  break;
                }
              } // next layer
            }
            if (layer != null) { // there is a matching layer
              if (layer.getAlignment() != 1) p.setValue(layer);
            } else if (parent == null) {
              // no name match, and it's not a child tier
               
              // assume it's a tier named after a speaker - make the utteranceLayer the default
              p.setValue(getUtteranceLayer());
            }
            p.setPossibleValues(vPossiblLayers);
            mappings.addParameter(p);
          } else if (parser.getLocalName().equals("HEADER")) {
            timeUnits = parser.getAttributeValue(null, "TIME_UNITS");
            if (timeUnits == null) timeUnits = "milliseconds";
          }
        } // START_ELEMENT
      } // next event

      if (!foundTimeOrder) {
        throw new SerializationException(SerializationException.ErrorType.InvalidDocument,
                                         "Document has no TIME_ORDER node");
      }
      if (!foundTiers) {
        throw new SerializationException(SerializationException.ErrorType.InvalidDocument,
                                         "Document has no TIER nodes");
      }

      return mappings;

    } catch(XMLStreamException x) {
      throw new SerializationException(x);
    }
  }

  /**
   * Sets parameters for a given deserialization operation, after loading the serialized
   * form of the graph. This might include mappings from format-specific objects like
   * tiers to graph layers, etc.
   * @param parameters The configuration for a given deserialization operation.
   * @throws SerializationParametersMissingException If not all required parameters have
   * a value.
   */
  public void setParameters(ParameterSet parameters)
    throws SerializationParametersMissingException {
      
    mappings = parameters;
      
    // check we've got enough mappings for turns and words
    // i.e. at least two of turn/utterance/transcription
    int iTurnLayerMapped = 0;
    int iUtteranceLayerMapped = 0;
    int iWordLayerMapped = 0;
    mappingsDependOnTurn = false;
    for (Parameter p : mappings.values()) {
      if (p.getValue() != null && p.getValue() instanceof Layer) {
        Layer layer = (Layer)p.getValue();
        if (!layer.getId().equals("[ignore tier]")) {
          if (layer.equals(getTurnLayer())
              || layer.getAncestors().contains(getTurnLayer())) {
            mappingsDependOnTurn = true;
          }
	    
          if (layer.equals(getTurnLayer())) {
            iTurnLayerMapped++;
          } else if (layer.equals(getUtteranceLayer())) {
            iUtteranceLayerMapped++;
          } else if (layer.equals(getWordLayer())) {
            iWordLayerMapped++;
          }
        } // not ignoring this tier
      } // a layer mapping
    } // next parameter
    
    if (iTurnLayerMapped + iUtteranceLayerMapped + iWordLayerMapped == 0
        && mappingsDependOnTurn) {
      throw new SerializationParametersMissingException(
        "There are no turn, utterance, or word mappings, but at least one is required");
    }
      
    if (timeUnits.equalsIgnoreCase("milliseconds")) {
      timeFactor = (1.0/1000.0);
    } else if (timeUnits.equalsIgnoreCase("NTSC-frames")) {
      timeFactor = (1.0/30.0);
    } else if (timeUnits.equalsIgnoreCase("PAL-frames")) {
      timeFactor = (1.0/25.0);
    } else {
      warnings.add("Unkown TIME_UNITS: " + timeUnits);
    }
  }

  /**
   * Deserializes the serialized data, generating one or more {@link Graph}s.
   * <p>Many data formats will only yield one graph (e.g. Transcriber transcript or Praat
   * textgrid), however there are formats that are capable of storing multiple
   * transcripts in the same file (e.g. AGTK, Transana XML export), which is why this
   * method returns a list. 
   * @return A list of valid (if incomplete) {@link Graph}s. 
   * @throws SerializerNotConfiguredException if the object has not been configured.
   * @throws SerializationParametersMissingException if the parameters for this
   * particular graph have not been set.
   * @throws SerializationException if errors occur during deserialization.
   */
  public Graph[] deserialize() 
    throws SerializerNotConfiguredException, SerializationParametersMissingException,
    SerializationException {

    if (mappingsDependOnTurn) {
      if (participantLayer == null)
        throw new SerializerNotConfiguredException("Participant layer not set");
      if (turnLayer == null)
        throw new SerializerNotConfiguredException("Turn layer not set");
      if (utteranceLayer == null)
        throw new SerializerNotConfiguredException("Utterance layer not set");
      if (wordLayer == null)
        throw new SerializerNotConfiguredException("Word layer not set");
    }
    if (schema == null)
      throw new SerializerNotConfiguredException("Layer schema not set");

    // if there are errors, accumulate as many as we can before throwing SerializationException
    SerializationException errors = null;

    warnings = new Vector<String>();

    Graph graph = new Graph();
    graph.setId(getId());
    // creat the 0 anchor to prevent graph tagging from creating one with no confidence
    Anchor graphStart = graph.getOrCreateAnchorAt(0.0, Constants.CONFIDENCE_MANUAL);

    // add layers to the graph
    if (mappingsDependOnTurn) {
      // we don't just copy the whole schema, because that would imply that all the extra layers
      // contained no annotations, which is not necessarily true
      graph.addLayer((Layer)participantLayer.clone());
      graph.getSchema().setParticipantLayerId(participantLayer.getId());
      graph.addLayer((Layer)turnLayer.clone());
      graph.getSchema().setTurnLayerId(turnLayer.getId());
      graph.addLayer((Layer)utteranceLayer.clone());
      graph.getSchema().setUtteranceLayerId(utteranceLayer.getId());
      graph.addLayer((Layer)wordLayer.clone());
      graph.getSchema().setWordLayerId(wordLayer.getId());
    }
    if (authorLayer != null) graph.addLayer((Layer)authorLayer.clone());
    if (dateLayer != null) graph.addLayer((Layer)dateLayer.clone());
    if (languageLayer != null) graph.addLayer((Layer)languageLayer.clone());
    if (phraseLanguageLayer != null) graph.addLayer((Layer)phraseLanguageLayer.clone());

    graph.setOffsetUnits(Constants.UNIT_SECONDS);
    graph.setOffsetGranularity(timeFactor);

    boolean turnLayerMapped = false;
    boolean utteranceLayerMapped = false;
    boolean wordLayerMapped = false;

    for (Parameter p : mappings.values()) {
      if (p.getValue() != null && p.getValue() instanceof Layer) {
        Layer layer = (Layer)p.getValue();
        if (!layer.getId().equals("[ignore tier]")) {
          if (layer.getId().equals(getTurnLayer().getId())) {
            turnLayerMapped = true;
          } else if (layer.getId().equals(getUtteranceLayer().getId())) {
            utteranceLayerMapped = true;
          } else if (layer.getId().equals(getWordLayer().getId())) {
            wordLayerMapped = true;
          }
          // ensure the layer is added to the graph
          if (graph.getLayer(layer.getId()) == null) {
            graph.addLayer((Layer)layer.clone());
          }          
        } // tier is mapped to a layer
      } // Layer value
    } // next mapping
    
    if (mappingsDependOnTurn) {
      if (!wordLayerMapped) {
        // add convention layers, as we'll be breaking utterances into tokens
        if (lexicalLayer != null) graph.addLayer((Layer)lexicalLayer.clone());
        if (pronounceLayer != null) graph.addLayer((Layer)pronounceLayer.clone());
        if (commentLayer != null) graph.addLayer((Layer)commentLayer.clone());
        if (noiseLayer != null) graph.addLayer((Layer)noiseLayer.clone());	 
      }
      
      // are at least two of turn/utterance/word are mapped?
      // i.e. one for speaker name and other for transcription
      if((turnLayerMapped && utteranceLayerMapped)
         || (turnLayerMapped && wordLayerMapped)
         || (utteranceLayerMapped && wordLayerMapped)
        ) {
        setUtterancesAreSpeakerNames(true);
      }
    }

    // some variables to remember state while parsing...

    // create Anchors from TIME_SLOTs
    HashMap<String,Anchor> mTimeslotIdToAnchor = new HashMap<String,Anchor>();
    int tierIndex = -1;
    String lang = null;
    String speaker = null;
    String tierId = null;
    Layer layer = null;
    String annotationId = null;
    String annotationRef = null;
    String timeSlot1 = null;
    String timeSlot2 = null;

    Vector<Annotation> symbolicAnnotations = new Vector<Annotation>();
    
    // parse the XML file a tag at a time
    try {
      InputStream in = new FileInputStream(eafFile);
      XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(in);
      while (parser.hasNext()) {
        int event = parser.next();
        if (event == XMLStreamConstants.START_ELEMENT) {
          if (parser.getLocalName().equals("ANNOTATION_DOCUMENT")) {
            // attributes
            String sResult = parser.getAttributeValue(null, "AUTHOR");
            if (sResult != null && sResult.length() > 0 && authorLayer != null) {
              graph.createTag(graph, authorLayer.getId(), sResult)
                .setConfidence(Constants.CONFIDENCE_MANUAL);;
            }
            sResult = parser.getAttributeValue(null, "DATE");
            if (sResult != null && sResult.length() > 0 && dateLayer != null) {
              graph.createTag(graph, dateLayer.getId(), sResult)
                .setConfidence(Constants.CONFIDENCE_MANUAL);;
            }
            // TODO sResult = xpath.evaluate("/ANNOTATION_DOCUMENT/TIER/@LANG_REF", root);
            // if (sResult == null || sResult.length() == 0) { // for backward compatibility
            //   sResult = xpath.evaluate("/ANNOTATION_DOCUMENT/TIER/@DEFAULT_LOCALE", root);
            // }
            // if (sResult != null && sResult.length() > 0 && languageLayer != null) {
            //   graph.createTag(graph, languageLayer.getId(), sResult)
            //     .setConfidence(Constants.CONFIDENCE_MANUAL);;
            // }
            
          } else if (parser.getLocalName().equals("TIME_SLOT")) { // anchor
            
            String timeSlotId = parser.getAttributeValue(null, "TIME_SLOT_ID");
            String timeValue = parser.getAttributeValue(null, "TIME_VALUE");
            if (timeValue == null) {
              mTimeslotIdToAnchor.put(timeSlotId, new Anchor(timeSlotId, null));
            } else {
              mTimeslotIdToAnchor.put(
                timeSlotId, new Anchor(
                  timeSlotId,
                  Double.valueOf(timeFactor * Double.parseDouble(timeValue)),
                  Constants.CONFIDENCE_MANUAL));
            }
            // don't add them to the graph yet - we'll do that as we go...
            
          } else if (parser.getLocalName().equals("TIER")) { // layer
            // TODO?? mTierMessages.put(
            //   sTierId, "" + annotations.getLength() + " annotation" 
            //   + (annotations.getLength()==1?"":"s") + " added to graph.");
            
            String langRef = parser.getAttributeValue(null, "LANG_REF");
            // for backward compatibility:
            if (langRef == null) langRef = parser.getAttributeValue(null, "DEFAULT_LOCALE");
            if (langRef != null) lang = langRef;
            tierId = parser.getAttributeValue(null, "TIER_ID");
            tierIndex++;
            speaker = parser.getAttributeValue(null, "PARTICIPANT");
            if (speaker == null) speaker = tierId;
            layer = (Layer)mappings.get("tier"+tierIndex).getValue();
            
          } else if (parser.getLocalName().equals("ALIGNABLE_ANNOTATION")) { // annotation

            annotationId = parser.getAttributeValue(null, "ANNOTATION_ID");
            timeSlot1 = parser.getAttributeValue(null, "TIME_SLOT_REF1");
            timeSlot2 = parser.getAttributeValue(null, "TIME_SLOT_REF2");

          } else if (parser.getLocalName().equals("REF_ANNOTATION")) {
            
            annotationId = parser.getAttributeValue(null, "ANNOTATION_ID");
            annotationRef = parser.getAttributeValue(null, "ANNOTATION_REF");
            
          } else if (parser.getLocalName().equals("ANNOTATION_VALUE")) { // annotation label

            String value = parser.getElementText();
            if (layer != null
                && (!getIgnoreBlankAnnotations()
                    || value.trim().length() > 0)) { // ignore empty intervals...
              if (annotationRef == null) { // alignable annotation
                Anchor start = mTimeslotIdToAnchor.get(timeSlot1);
                Anchor end = mTimeslotIdToAnchor.get(timeSlot2);
                Annotation annotation = new Annotation(
                  annotationId, value, layer.getId(), start.getId(), end.getId());
                annotation.setConfidence(Constants.CONFIDENCE_MANUAL);
                annotation.put("@tierId", tierId); // this might come in handy later
                annotation.put("@participant", speaker); // this might come in handy later
                // TODO annotation.setAnnotator(...), from the tier's settings.
                // add anchors if they're not in the graph
                if (!graph.getAnchors().containsKey(start.getId())) graph.addAnchor(start);
                if (!graph.getAnchors().containsKey(end.getId())) graph.addAnchor(end);
                // add annotation
                graph.addAnnotation(annotation);
              } else { // symbolic reference annotation
                // create the annotation now, but save adding it until all annotations are
                // identified, so we know that the referent annotation is known
                Annotation annotation = new Annotation(annotationId, value, layer.getId());
                annotation.put("@tierId", tierId); // this might come in handy later
                annotation.put("@participant", speaker); // this might come in handy later
                annotation.put("@annotationRef", annotationRef); // this might come in handy later
                // save for later...
                symbolicAnnotations.add(annotation);
              }              
            } // not an ignored annotation
            
          } // ANNOTATION_VALUE
        } else if (event == XMLStreamConstants.END_ELEMENT) {
          if (parser.getLocalName().equals("ANNOTATION")) {
            // reset annotation attributes
            annotationId = null;
            annotationRef = null;
            timeSlot1 = null;
            timeSlot2 = null;              
          } else if (parser.getLocalName().equals("TIER")) {
            // reset annotation attributes
            tierId = null;
            layer = null;
          }
        } // END_ELEMENT
      } // next element
    } catch(IOException x) {
      if (errors == null) errors = new SerializationException(x);
      if (errors.getCause() == null) errors.initCause(x);
      errors.addError(SerializationException.ErrorType.Other, x.getMessage());
    } catch(XMLStreamException x) {
      mTierMessages.put(tierId, x.getMessage());
      if (errors == null) errors = new SerializationException(x);
      if (errors.getCause() == null) errors.initCause(x);
      errors.addError(SerializationException.ErrorType.InvalidDocument, x.getMessage());
    } finally {
      // delete input file
      eafFile.delete();
    }

    // now that we've got all annotations, add symbolic annotations    
    while (symbolicAnnotations.size() > 0) {
      boolean resolvedReferences = false;
      Iterator<Annotation> symbolic = symbolicAnnotations.iterator();
      while (symbolic.hasNext()) {
        Annotation annotation = symbolic.next();
        annotationRef = (String)annotation.get("@annotationRef");
        // the 'parent' annotation is identified by annotationRef
        Annotation ref = graph.getAnnotation(annotationRef);
        if (ref != null) {
          annotationId = annotation.getId();
          tierId = (String)annotation.get("@tierId");
          speaker = (String)annotation.get("@participant");
          annotation = graph.createSubdivision(
            ref, annotation.getLayerId(), annotation.getLabel());
          annotation.setId(annotationId); // use EAF ID 
          annotation.setConfidence(Constants.CONFIDENCE_MANUAL);
          annotation.put("@tierId", tierId); // this might come in handy later
          annotation.put("@participant", speaker); // this might come in handy later
          annotation.put("@ref", ref); // this might come in handy later
          // TODO annotation.setAnnotator(...), from the tier's settings.
          if (ref.getLayerId().equals(annotation.getLayer().getParentId())) {
            // ref is parent
            annotation.setParent(ref);
          }
          symbolic.remove();
          resolvedReferences = true;
        } // ref found
      } // next symbolic annotation
      if (!resolvedReferences) {
        if (errors == null) errors = new SerializationException();
        for (Annotation annotation : symbolicAnnotations) {
          errors.addError(
            SerializationException.ErrorType.InvalidDocument,
            "Annotation " + annotation.getId()
            + " \"" + annotation.getLabel() + "\" on tier " + annotation.get("@tierId")
            + ": Cannot find referenced annotation: " + annotation.get("@annotationRef")
            + (ignoreBlankAnnotations?
               " - the referenced annotation may be blank,"
               +" and blank annotations are configured to be ignored.":""));
        } // next unreferenceable annotation
        break;
      } // didn't resulve any references this time around
    } // there are still symbolic annotations to process
    
    // did we find a language?
    if (lang != null && lang.length() > 0 && languageLayer != null) {
      graph.createTag(graph, languageLayer.getId(), lang)
        .setConfidence(Constants.CONFIDENCE_MANUAL);
    }
    
    Anchor graphEnd = graph.getEnd();

    // ensure both turns and utterances exist, and parents are set
    if (wordLayerMapped && !turnLayerMapped && !utteranceLayerMapped) {
      // create utterances and turns from words
         
      // given there are no utterance/turn intervals, 
      // we assume that the tier name for words is the speaker name	 
      HashMap<String,Annotation> turnsByName = new HashMap<String,Annotation>();
      for (Annotation word : graph.all(wordLayer.getId())) {
        String participant = (String)word.get("@participant");
        Annotation turn = turnsByName.get(participant);
        if (turn == null) {
          // create turn 
          turn = new Annotation(
            null, participant, turnLayer.getId(), graphStart.getId(), graphEnd.getId());
          turn.setConfidence(Constants.CONFIDENCE_MANUAL);
          graph.addAnnotation(turn);
          turnsByName.put(participant, turn);

          // create utterance
          Annotation utterance = new Annotation(turn);
          utterance.setLayerId(utteranceLayer.getId())
            .setParentId(turn.getId())
            .setConfidence(Constants.CONFIDENCE_MANUAL);               
          graph.addAnnotation(utterance);
        }
        // set parent of word
        word.setParent(turn);
      } // next turn
    } else if (turnLayerMapped && !utteranceLayerMapped) { // create utterances from turns
      for (Annotation turn : graph.all(turnLayer.getId())) {
        Annotation utterance = new Annotation(turn);
        utterance.setLayerId(utteranceLayer.getId())
          .setParentId(turn.getId())
          .setConfidence(Constants.CONFIDENCE_MANUAL);;
        if (!wordLayerMapped) { // no word layer 
          // ...which means the label must be the untokenized words
          // and so the turn's @participant must be the speaker name
          turn.setLabel((String)turn.get("@participant"));
        }
        graph.addAnnotation(utterance);
      } // next turn
    } else if (utteranceLayerMapped && !turnLayerMapped) { // create turns from utterances
      for (Annotation utterance : graph.all(utteranceLayer.getId())) {
        Annotation turn = new Annotation(utterance);
        turn.setLayerId(turnLayer.getId())
          // the utterance's @participant is taken to be the speaker name
          .setLabel((String)utterance.get("@participant"))
          .setConfidence(Constants.CONFIDENCE_MANUAL);
        graph.addAnnotation(turn);
        // now turn will have an ID, and we can set it to be the parent of utterance
        utterance.setParent(turn);
      } // next utterance
    } else if (utteranceLayerMapped && turnLayerMapped) {
      // ensure utterance parent turns are set
      for (Annotation utterance : graph.all(utteranceLayer.getId())) {
        Annotation[] possibleTurns = utterance.includingAnnotationsOn(turnLayer.getId());
        if (possibleTurns.length == 1) { // must be this one
          utterance.setParent(possibleTurns[0]);
        } else if (possibleTurns.length > 1) { // multiple possible turns
          // use the turn whose label is included in the utterance's tier name
          // e.g. the turn might be "John Smith" and the utterance tier might be
          // "utterance - John Smith"
          String utteranceTier = (String)utterance.get("@tierId");
          String utteranceParticipant = (String)utterance.get("@participant");
          Annotation turn = null;
          for (Annotation possibleTurn : possibleTurns) {
            // is the label (the speaker) a part of the utterance's tier name?
            if (utteranceTier.indexOf(possibleTurn.getLabel()) >= 0
                || utteranceParticipant.equals(possibleTurn.getLabel())) {
              // multiple parents could match 
              // e.g. "sp1" and "Interviewer sp1" both are suffixes of
              // "utterance - Interviewer sp1"
              // so we go with the longest one
              if (turn == null
                  || possibleTurn.getLabel().length() > turn.getLabel().length()) {
                turn = possibleTurn;
              } // longest match so far
            } // label is a part of the tier name
          } // next possible turn
          if (turn != null) utterance.setParent(turn);
        } // multiple possible turns
      } // next utterance
    } // both utterance and turn layers mapped

    if (mappingsDependOnTurn) {
      // now we have turns with participant name labels, 
      // and utterances with parents, that maybe need tokenizing
	 
      // ensure participants are set
      HashMap<String,Annotation> participantsByName = new HashMap<String,Annotation>();
      int ordinal = 1;
      for (Annotation turn : graph.all(turnLayer.getId())) {
        if (!participantsByName.containsKey(turn.getLabel())) { // create participant
          Annotation who = new Annotation(null, turn.getLabel(), participantLayer.getId());
          who.setConfidence(Constants.CONFIDENCE_MANUAL);
          graph.addAnnotation(who);
          participantsByName.put(turn.getLabel(), who);
        }
        // set ordinal first, then don't allow appending in setParent, to save a performance hit
        turn.setOrdinal(
          participantsByName.get(turn.getLabel()).getAnnotations(turnLayer.getId()).size() + 1);
        turn.setParent(participantsByName.get(turn.getLabel()), false);
      } // next turn
         
      // have to track changes to be able to mark things for destrucion...
      graph.trackChanges();
         
      // join subsequent turns by the same speaker...
      // for each participant (assumed to be parent of turn)
      for (Annotation participant : graph.all(participantLayer.getId())) {
        TreeSet<Annotation> annotations
          = new TreeSet<Annotation>(new AnnotationComparatorByAnchor());
        annotations.addAll(participant.getAnnotations(turnLayer.getId()));
        Annotation[] turns = annotations.toArray(new Annotation[0]);
        // go back through all the turns, looking for a turn for the same speaker that is
        // joined to, or overlaps, this one
        for (int i = turns.length - 2; i >= 0; i--) {
          Annotation preceding = turns[i];
          Annotation following = turns[i + 1];
          boolean mergeTurns = false;
          if (preceding.getEnd().getOffset() != null
              && following.getStart().getOffset() != null) {
            if (preceding.getEnd().getOffset() >= following.getStart().getOffset()) {
              mergeTurns = true;
            } else if (getMinimumTurnPauseLength() > 0
                       && preceding.getEnd().getOffset() + getMinimumTurnPauseLength()
                       >= following.getStart().getOffset()) {
              // there is a short enough pause between two turns of the same participant
                     
              // but there also must be no intervening speakers
              if (graph.overlappingAnnotations(
                    preceding.getEnd(), following.getStart(), turnLayer.getId())
                  .length == 0) {
                mergeTurns = true;
              }
            }
          }
          if (mergeTurns) {
            mergeTurns(preceding, following);
            following.destroy();
          }
        } // next preceding turn
      } // next turn parent

      graph.commit();
	 
      // now we have participants,
      // and rationalized turns with participant name labels and parents, 
      // and utterances with parents, that maybe need tokenizing
	 
      if (!wordLayerMapped) { // tokenize utterances and apply conventions
        // ensure we have an utterance tokenizer
        if (getTokenizer() == null) {
          setTokenizer(
            new SimpleTokenizer(getUtteranceLayer().getId(), getWordLayer().getId()));
        }
        try {
          tokenizer.transform(graph);
          // TODO annotation.setAnnotator(...) for all tokens, from the tier's settings.
	       
        } catch(TransformationException exception) {
          if (errors == null) errors = new SerializationException();
          if (errors.getCause() == null) errors.initCause(exception);
          errors.addError(
            SerializationException.ErrorType.Tokenization, exception.getMessage());
        }
        graph.commit();
        if (getUseConventions()) {
          try {
            // word {comment comment} word
            SpanningConventionTransformer commentTransformer = new SpanningConventionTransformer(
              getWordLayer().getId(), "\\{(.*)", "(.*)\\}", true, null, null, 
              commentLayer==null?null:commentLayer.getId(), "$1", "$1", false, false);
            commentTransformer.transform(graph);
            graph.commit();
		  
            // [CS:lang]word word[CS:lang] - i.e. phrase tag
            SpanningConventionTransformer phraseLanguageTransformer
              = new SpanningConventionTransformer(
                getWordLayer().getId(), "\\[CS:([^\\]]+)\\](.*)", "(.+)\\[CS:([^\\]]+)\\](\\p{Punct}*)",
                false, "$2", "$1$3", 
                phraseLanguageLayer==null?null:phraseLanguageLayer.getId(), "$1", null,
                false, false);
            phraseLanguageTransformer.transform(graph);
		  
            // word[CS:lang] - i.e. word tag
            ConventionTransformer wordLanguageTransformer = new ConventionTransformer(
              getWordLayer().getId(), "(.+)\\[CS:([^\\]]+)\\](\\p{Punct}*)", "$1$3", 
              phraseLanguageLayer==null?null:phraseLanguageLayer.getId(), "$2");
            wordLanguageTransformer.transform(graph);

            if (phraseLanguageLayer != null) {
              // as there are two phases for language tagging (phrase, then single word)
              // we make sure that the language ordinals are in order according to the words
              // they annotate (offsets are mostly not set so can't be used for ordering)
              // (if annotations are out of order, this can cause later validation problems)
              
              // for each language tag parent (i.e. each turn)...
              final String phraseLanguageParentId = phraseLanguageLayer.getParentId();
              final String wordLayerId = getWordLayer().getId();
              for (Annotation parent : graph.all(phraseLanguageParentId)) {

                // sort language annotations by the ordinal of the linked word
                TreeSet<Annotation> langInWordOrdinalOrder = new TreeSet<Annotation>(
                  new Comparator<Annotation>() {
                    public int compare(Annotation lang1, Annotation lang2) {
                      Set<Annotation> words1 = lang1.getStart().startOf(wordLayerId);
                      Set<Annotation> words2 = lang2.getStart().startOf(wordLayerId);
                      if (words1.size() > 0 && words2.size() > 0) { // both connected to words
                        Annotation word1 = words1.iterator().next();
                        Annotation word2 = words2.iterator().next();
                        if (word1.getOrdinal() < word2.getOrdinal()) {
                          return -1;
                        } else if (word1.getOrdinal() > word2.getOrdinal()) {
                          return 1;
                        }
                      } else { // at least one is not connected to a word
                        if (lang1.getStart().startOf(phraseLanguageParentId).size() > 0) {
                          // lang1 at start of parent
                          return -2;
                        }
                        if (lang2.getStart().startOf(phraseLanguageParentId).size() > 0) {
                          // lang2 at start of parent
                          return 2;
                        }
                      }
                      return lang1.compareTo(lang2);
                    }
                  });
                
                // create a list sorted by word annotation
                for (Annotation l : parent.all(phraseLanguageLayer.getId())) {
                  langInWordOrdinalOrder.add(l);
                }

                // now set the ordinal, by re-adding all children
                int ord = 1;
                parent.getAnnotations(phraseLanguageLayer.getId()).clear();
                for (Annotation l : langInWordOrdinalOrder) {
                  l.setOrdinal(ord++);
                  parent.getAnnotations(phraseLanguageLayer.getId()).add(l);
                }
              } // next language parent
            } // phraseLanguageLayer set
            graph.commit();
            
            if (phraseLanguageLayer != null) {
              // join contiguous phrase language tags with the same label
              new Coalescer()
                .setLayerId(phraseLanguageLayer.getId())
                .transform(graph);
            }
            graph.commit();
		  
            // word [noise noise] word
            SpanningConventionTransformer noiseTransformer = new SpanningConventionTransformer(
              getWordLayer().getId(), "\\[(.*)", "(.*)\\]", true, null, null, 
              noiseLayer==null?null:noiseLayer.getId(), "$1", "$1", false, false);
            noiseTransformer.transform(graph);
            graph.commit();
		  
            // word[pronounce]
            ConventionTransformer pronounceTransformer = new ConventionTransformer(
              getWordLayer().getId(), "(.+)\\[(.*)\\](\\p{Punct}*)", "$1$3", 
              pronounceLayer==null?null:pronounceLayer.getId(), "$2");
            pronounceTransformer.transform(graph);
            graph.commit();
		  
            // word(lexical)
            ConventionTransformer lexicalTransformer = new ConventionTransformer(
              getWordLayer().getId(), "(.+)\\((.*)\\)(\\p{Punct}*)", "$1$3", 
              lexicalLayer==null?null:lexicalLayer.getId(), "$2");
            lexicalTransformer.transform(graph);
            graph.commit();
		  
            // run word[pronounce] again, in case some were masked by lexical tags:
            // word[pronounce](lexical)
            pronounceTransformer.transform(graph);
            graph.commit();
		  
            // clump non-orthographic 'words' with real words
            OrthographyClumper clumper = new OrthographyClumper(wordLayer.getId());	  
            clumper.transform(graph);
            graph.commit();
          } catch(TransformationException exception) {
            if (errors == null) errors = new SerializationException();
            if (errors.getCause() == null) errors.initCause(exception);
            errors.addError(
              SerializationException.ErrorType.Tokenization, exception.getMessage());
          }
        } // apply transcription conventions
      } else { // word layer mapped
        // ensure word parent turns are set
        for (Annotation word : graph.all(wordLayer.getId())) {
          if (word.getParent() == null
              && word.containsKey("@ref")) { // was it a referring annotation?
            Annotation ref = (Annotation)word.get("@ref");
            if (ref.getLayer().getParentId().equals(word.getLayer().getParentId())) {
                // ref shares parent with word
              word.setParent(ref.getParent());
            }
          }
          if (word.getParent() == null) {

            Annotation[] possibleTurns = word.includingAnnotationsOn(turnLayer.getId());
            if (possibleTurns.length == 1) { // must be this one
              word.setParent(possibleTurns[0]);
            } else if (possibleTurns.length > 1) { // multiple possible turns
              // use the turn whose label is included in the utterance's tier name
              // e.g. the turn might be "John Smith" and the utterance tier might
              // be "utterance - John Smith"
              String wordTier = (String)word.get("@tierId");
              String wordParticipant = (String)word.get("@participant");
              Annotation turn = null;
              for (Annotation possibleTurn : possibleTurns) {
                // is the label (the speaker) a part of the utterance's tier name?
                if (wordTier.indexOf(possibleTurn.getLabel()) >= 0
                    || wordParticipant.equals(possibleTurn.getLabel())) {
                  // multiple parents could match 
                  // e.g. "sp1" and "Interviewer sp1" both are suffixes of "word
                  // - Interviewer sp1" so we go with the longest one
                  if (turn == null
                      || possibleTurn.getLabel().length() > turn.getLabel().length()) {
                    turn = possibleTurn;
                  } // longest match so far
                } // label is a part of the tier name
              } // next possible turn
              if (turn != null) word.setParent(turn);
            } // multiple possible turns
          } // parent not set
          if (word.getParent() == null) { // parent still not set
            // maybe children don't quite line up with parents, so use
            // midpoint-including instead 
            Annotation[] possibleTurns
              = word.midpointIncludingAnnotationsOn(turnLayer.getId());
            if (possibleTurns.length == 1) { // must be this one
              word.setParent(possibleTurns[0]);
            } else if (possibleTurns.length > 1) { // multiple possible turns
              // use the turn whose label is included in the utterance's tier name
              // e.g. the turn might be "John Smith" and the utterance tier might
              // be "utterance - John Smith"
              String wordTier = (String)word.get("@tierId");
              String wordParticipant = (String)word.get("@participant");
              Annotation turn = null;
              for (Annotation possibleTurn : possibleTurns) {
                // is the label (the speaker) a part of the utterance's tier name?
                if (wordTier.indexOf(possibleTurn.getLabel()) >= 0
                    || wordParticipant.equals(possibleTurn.getLabel())) {
                  // multiple parents could match 
                  // e.g. "sp1" and "Interviewer sp1" both are suffixes of
                  // "word - Interviewer sp1" so we go with the longest one
                  if (turn == null
                      || possibleTurn.getLabel().length() > turn.getLabel().length()) {
                    turn = possibleTurn;
                  } // longest match so far
                } // label is a part of the tier name
              } // next possible turn
              if (turn != null) word.setParent(turn);
            } // multiple possible turns
          } // parent still not set
        } // next utterance
      } // word layer mapped
	 
      // now we have participants,
      // and turns with participant name labels and parents, 
      // and utterances with parents
      // and words with parents
	 
    } // mappingsDependOnTurn
      
    // need to ensure that other required parents are set
    for (Annotation a : graph.getAnnotationsById().values()) {
      if (a.getParentId() == null) {
        Annotation[] possibleParents = a.includingAnnotationsOn(a.getLayer().getParentId());
        if (possibleParents.length == 1) { // must be this one
          a.setParent(possibleParents[0]);
        } else if (possibleParents.length > 1) { // multiple possible parents
          // use the turn whose label is included in the utterance's tier name
          // e.g. the turn might be "John Smith" and the utterance tier might be
          // "utterance - John Smith"
          String tier = (String)a.get("@tierId");
          String participant = (String)a.get("@participant");
          Annotation parent = null;
          Annotation parentWho = null;
          for (Annotation possibleParent : possibleParents) {
            // is the label (the speaker) a part of the utterance's tier name?
            Annotation who = possibleParent.first("who");
            if (who != null) {
              if (tier.indexOf(who.getLabel()) >= 0
                  || participant.equals(who.getLabel())) {
                // multiple parents could match 
                // e.g. "sp1" and "Interviewer sp1" both are suffixes of
                // "segment - Interviewer sp1" so we go with the longest one
                if (parent == null
                    || who.getLabel().length() > parentWho.getLabel().length()) {
                  parent = possibleParent;
                  parentWho = who;
                } // longest match so far
              } // label is a part of the tier name
            } // there is a speaker
          } // next possible parent
          if (parent != null) a.setParent(parent);
        } // multiple possible parents
      } // parent not set

      if (a.getParentId() == null) { // still not set
        // maybe children don't quite line up with parents, so use midpoint-including instead
        Annotation[] possibleParents
          = a.midpointIncludingAnnotationsOn(a.getLayer().getParentId());
        if (possibleParents.length == 1) { // must be this one
          a.setParent(possibleParents[0]);
        } else if (possibleParents.length > 1) { // multiple possible parents
          // use the turn whose label is included in the utterance's tier name
          // e.g. the turn might be "John Smith" and the utterance tier might be
          // "utterance - John Smith"
          String tier = (String)a.get("@tierId");
          String participant = (String)a.get("@participant");
          Annotation parent = null;
          for (Annotation possibleParent : possibleParents) {
            // is the label (the speaker) a part of the utterance's tier name?
            Annotation who = possibleParent.first("who");
            if (who != null) {
              if (tier.indexOf(who.getLabel()) >= 0
                  || participant.equals(who.getLabel())) {
                // multiple parents could match 
                // e.g. "sp1" and "Interviewer sp1" both are suffixes of
                // "segment - Interviewer sp1" so we go with the longest one
                if (parent == null
                    || possibleParent.getLabel().length() > parent.getLabel().length()) {
                  parent = possibleParent;
                } // longest match so far
              } // label is a part of the tier name
            } // there is a speaker
          } // next possible parent
          if (parent != null) a.setParent(parent);
        } // multiple possible parents
      } // parent still not set

    } // next annotation

    // ensure all language labels are ISO alpha-2
    if (languageLayer != null) {
      for (Annotation l : graph.all(languageLayer.getId())) {
        // try to make it an alpha-2 code
        l.setLabel(
          iso639.alpha2(l.getLabel())
          .orElse(l.getLabel())); // fall back to the original label
      }
    }
    if (phraseLanguageLayer != null) {
      for (Annotation l : graph.all(phraseLanguageLayer.getId())) {
        // try to make it an alpha-2 code
        l.setLabel(
          iso639.alpha2(l.getLabel())
          .orElse(l.getLabel())); // fall back to the original label
      }
    }

    // ensure anchors are shared between children and parents where required
    Vector<Layer> layers = graph.getLayersTopDown();
    // (bottom up to propagate changes from below)
    Collections.reverse(layers);
    for (Layer l : layers) {
      if (l.getSaturated() && l.getAlignment() == Constants.ALIGNMENT_INTERVAL) {
        for (Annotation parent : graph.all(l.getParentId())) {
          SortedSet<Annotation> children = parent.getAnnotations(l.getId());
          if (children.size() > 0) {
            if (children.first().getStart() != null)
              parent.setStart(children.first().getStart());
            if (children.last().getEnd() != null)
              parent.setEnd(children.last().getEnd());
          }
        } // next parent
      }
    }
    graph.commit();

    // remove references to tier information
    for (Annotation a : graph.getAnnotationsById().values()) {
      a.remove("@tierId");
      a.remove("@participant");
    }

    // set end anchors of graph tags
    for (Annotation a : graph.all(getParticipantLayer().getId())) {
      a.setStartId(graphStart.getId());
      a.setEndId(graphEnd.getId());
    }

    graph.commit();

    if (errors != null) throw errors;

    // reset all change tracking
    if (graph.getTracker() != null) {
      graph.getTracker().reset();
      graph.setTracker(null);
    }
    
    Graph[] graphs = { graph };
    return graphs;
  }

  /**
   * Moves all of the children of the following turn into the preceding turn, set the
   * end of the preceding to the end of the following, and marks the following for
   * deletion. 
   * @param preceding The preceding, surviving turn.
   * @param following The following turn, which will be deleted.
   * @return The changes for this merge.
   */
  public void mergeTurns(Annotation preceding, Annotation following) {
    // set anchor
    if (preceding.getEnd().getOffset() == null
        || following.getEnd().getOffset() == null
        || preceding.getEnd().getOffset() < following.getEnd().getOffset()) {
      preceding.setEnd(following.getEnd());
    }

    // for each child layer
    for (String childLayerId : following.getAnnotations().keySet()) {
      // move everything from following to preceding
      int ordinal = 1;
      if (preceding.getAnnotations().containsKey(childLayerId)) {
        ordinal = preceding.getAnnotations().get(childLayerId).size() + 1;
      }
      for (Annotation child : following.annotations(childLayerId)) {
        child.setParent(preceding);
        child.setOrdinal(ordinal++);
      } // next child annotation
    } // next child layer
  } // end of joinTurns()

  // GraphSerializer methods
   
  /**
   * Determines which layers, if any, must be present in the graph that will be serialized.
   * <p>{@link GraphSerializer} method.
   * @return A list of IDs of layers that must be present in the graph that will be serialized.
   * @throws SerializationParametersMissingException If not all required parameters have a value.
   */
  public String[] getRequiredLayers() throws SerializationParametersMissingException {
    Vector<String> requiredLayers = new Vector<String>();
    if (getParticipantLayer() != null) requiredLayers.add(getParticipantLayer().getId());
    if (getTurnLayer() != null) requiredLayers.add(getTurnLayer().getId());
    if (getUtteranceLayer() != null) requiredLayers.add(getUtteranceLayer().getId());
    if (getWordLayer() != null) requiredLayers.add(getWordLayer().getId());
    if (getLanguageLayer() != null) requiredLayers.add(getLanguageLayer().getId());
    if (bUseConventions != null && bUseConventions) {
      if (getLexicalLayer() != null) requiredLayers.add(getLexicalLayer().getId());
      if (getPronounceLayer() != null) requiredLayers.add(getPronounceLayer().getId());
      if (getCommentLayer() != null) requiredLayers.add(getCommentLayer().getId());
      if (getNoiseLayer() != null) requiredLayers.add(getNoiseLayer().getId());
    }
    return requiredLayers.toArray(new String[0]);
  }

  /**
   * Determines the cardinality between graphs and serialized streams.
   * @return {@link nzilbb.ag.serialize.GraphSerializer#Cardinality}.NtoN as there is one
   * stream produced for each graph to serialize.
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
   * @param consumer The object receiving the streams.
   * @param warnings The object receiving warning messages.
   * @return A list of named streams that contain the serialization in the given format. 
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
          consumer.accept(serializeGraph(graph, layerIds, warnings));
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
   * @throws SerializationException if errors occur during serialization.
   */
  protected NamedStream serializeGraph(
    Graph graph, String[] layerIds, Consumer<String> warnings) throws SerializationException {
    SerializationException errors = null;
    Schema schema = graph.getSchema();

    HashSet<String> selectedLayers = new HashSet<String>();
    if (layerIds != null) {
      for (String l : layerIds) selectedLayers.add(l);
    } else {
      for (Layer l : schema.getLayers().values()) selectedLayers.add(l.getId());
    }

    graph.setOffsetGranularity(Constants.GRANULARITY_MILLISECONDS);
    String language = null;
    if (languageLayer != null && selectedLayers.contains(languageLayer.getId())) {
      Annotation lang = graph.first(languageLayer.getId());
      if (lang != null) {
        language = lang.getLabel();
        // try to make it an alpha-3 code
        language = iso639.alpha3(language).orElse(language);
      }
    }
    if (phraseLanguageLayer != null && selectedLayers.contains(phraseLanguageLayer.getId())) {
      for (Annotation lang : graph.all(languageLayer.getId())) {
        // try to make it an alpha-3 code
        lang.setLabel(
          iso639.alpha3(lang.getLabel())
          .orElse(lang.getLabel())); // fall back to the original label
      }
    }

    // create a new XML document
    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = null;   
    try {
      builder = builderFactory.newDocumentBuilder();   
    } catch(Exception exception) {
      errors = new SerializationException(exception);
    }
    builder.setEntityResolver(new EntityResolver() {
        public InputSource resolveEntity(String publicId, String systemId)
          throws SAXException, IOException
        {
          // Get DTDs locally, to prevent not found errors
          String sName = systemId.substring(systemId.lastIndexOf('/') + 1);
          URL url = getClass().getResource(sName);
          if (url != null)
            return new InputSource(url.openStream());
          else
            return null;
        }
      });
      
    Document document = builder.newDocument();
    document.setXmlStandalone(true);
    Element annotationDocument = document.createElement("ANNOTATION_DOCUMENT");
    document.appendChild(annotationDocument);
    Annotation author = authorLayer==null || !selectedLayers.contains(authorLayer.getId())?
      null:graph.first(authorLayer.getId());
    if (author != null) {
      annotationDocument.setAttribute("AUTHOR", author.getLabel());
    } else {
      annotationDocument.setAttribute("AUTHOR", "");
    }
    Annotation date = dateLayer==null || !selectedLayers.contains(dateLayer.getId())?
      null:graph.first(dateLayer.getId());
    if (date != null) {
      annotationDocument.setAttribute("DATE", date.getLabel());
    } else {
      annotationDocument.setAttribute("DATE", ""); // TODO invalid!!
    }
    annotationDocument.setAttribute("FORMAT", "2.7");
    annotationDocument.setAttribute("VERSION", "2.7");
    annotationDocument.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
    annotationDocument.setAttribute(
      "xsi:noNamespaceSchemaLocation", "http://www.mpi.nl/tools/elan/EAFv2.7.xsd");
      
    Element header = document.createElement("HEADER");
    annotationDocument.appendChild(header);
    header.setAttribute("MEDIA_FILE","");
    header.setAttribute("TIME_UNITS","milliseconds");
    long lLastUnusedAnnotationId = 0;
    if (graph.getMediaProvider() != null) {
      try {
        MediaFile[] files = graph.getMediaProvider().getAvailableMedia();
        if (files.length > 0) {
          Element mediaDescriptor = document.createElement("MEDIA_DESCRIPTOR");
          header.appendChild(mediaDescriptor);
          mediaDescriptor.setAttribute("MEDIA_URL", "");
          mediaDescriptor.setAttribute("MIME_TYPE", "");
          MediaFile firstFile = files[0];
          if (firstFile.getUrl() != null && firstFile.getUrl().startsWith("http")) {
            // http URLs are not supported by ELAN
                  
            // so we'll just mangle the file name so that it's probably right if they've
            // downloaded media too
            mediaDescriptor.setAttribute(
              "MEDIA_URL",
              IO.WithoutExtension(graph.getId()) + "." + firstFile.getExtension());
          } else {
            mediaDescriptor.setAttribute("MEDIA_URL", firstFile.getUrl());
          }
          mediaDescriptor.setAttribute("MIME_TYPE", firstFile.getMimeType());
          header.setAttribute("MEDIA_FILE",firstFile.getName());
        }
      }
      catch(StoreException exception) {}
      catch(PermissionException exception) {}
    }
    Element lastUnusedAnnotationId = document.createElement("PROPERTY");
    header.appendChild(lastUnusedAnnotationId);
    lastUnusedAnnotationId.setAttribute("NAME", "lastUnusedAnnotationId");
      
    // create time slots
    Element timeOrder = document.createElement("TIME_ORDER");
    annotationDocument.appendChild(timeOrder);
    HashMap<String,String> mapAnchorToTimeslotId = new HashMap<String,String>();
    // for (Anchor a : graph.getSortedAnchors())
    // {
    //    ensureAnchorHasTimeslot(a, document, timeOrder, mapAnchorToTimeslotId);
    // } // next anchor
      
    // a line tier for each speaker
    HashMap<String,Element> mSpeakerTiers = new HashMap<String,Element>();
    for (Annotation participant : graph.all(participantLayer.getId())) {
      Element utterances = document.createElement("TIER");
      annotationDocument.appendChild(utterances);
      utterances.setAttribute("LINGUISTIC_TYPE_REF", "default-lt");
      utterances.setAttribute("TIER_ID", participant.getLabel());
      utterances.setAttribute("PARTICIPANT", participant.getLabel());
      if (language != null) utterances.setAttribute("LANG_REF", language);
      mSpeakerTiers.put(participant.getLabel(), utterances);
    } // next participant
      
    // 'freeform' layers first - i.e. aligned children of graph
    for (Layer layer : schema.getLayers().values().stream()
           .filter(layer -> selectedLayers.contains(layer.getId()))
           .filter(layer -> !schema.getRoot().equals(layer))
           // parent is root
           .filter(layer -> layer.getParent() == null
                   || layer.getParent().equals(graph.getSchema().getRoot()))
           // is aligned
           .filter(layer -> layer.getAlignment() != Constants.ALIGNMENT_NONE)
           .collect(Collectors.toList())) {
         
      lLastUnusedAnnotationId = insertTier(
        layer, graph, document, annotationDocument, timeOrder, mapAnchorToTimeslotId,
        lLastUnusedAnnotationId, warnings, language);
    }
      
    // utterances
    for (Annotation utterance : graph.all(utteranceLayer.getId())) {
      // find the speaker's tier
      Element tier = mSpeakerTiers.get(utterance.getLabel());
      if (tier == null) continue;
         
      // add an annotation to it
      Element annotation = document.createElement("ANNOTATION");
      tier.appendChild(annotation);
      Element alignableAnnotation = document.createElement("ALIGNABLE_ANNOTATION");
      annotation.appendChild(alignableAnnotation);
      String sId = "a"+(++lLastUnusedAnnotationId);
      alignableAnnotation.setAttribute("ANNOTATION_ID", sId);
      utterance.put("@alignableAnnotation", alignableAnnotation);
      alignableAnnotation.setAttribute(
        "TIME_SLOT_REF1", ensureAnchorHasTimeslot(
          utterance.getStart(), document, timeOrder, mapAnchorToTimeslotId));
      alignableAnnotation.setAttribute(
        "TIME_SLOT_REF2", ensureAnchorHasTimeslot(
          utterance.getEnd(), document, timeOrder, mapAnchorToTimeslotId));
      Element annotationValue = document.createElement("ANNOTATION_VALUE");
      alignableAnnotation.appendChild(annotationValue);
      StringBuffer sUtteranceText = new StringBuffer();
         
      for (Annotation word : utterance.all(wordLayer.getId())) {
        if (sUtteranceText.length() > 0) sUtteranceText.append(" ");
        sUtteranceText.append(word.getLabel());
            
        // link word to its utterance, so that words can be dependent on utterances
        word.put("@utterance", utterance);
      }
      annotationValue.setTextContent(sUtteranceText.toString());
    } // next annotation
      
    // 'meta' layers next - i.e. children of turn
    for (Layer layer : schema.getLayers().values().stream()
           .filter(layer -> selectedLayers.contains(layer.getId()))
           // parent is turn
           .filter(layer -> layer.getParentId() != null
                   && layer.getParentId().equals(turnLayer.getId()))
           // not utterance
           .filter(layer -> layer.getId().equals(utteranceLayer.getId()))
           // nor word
           .filter(layer -> wordLayer != null
                   && layer.getId().equals(wordLayer.getId()))
           .collect(Collectors.toList())) {
         
      lLastUnusedAnnotationId = insertTier(
        layer, graph, document, annotationDocument, timeOrder, mapAnchorToTimeslotId,
        lLastUnusedAnnotationId, warnings, language);
    }

    // do we want word tokens? depends on selected layers...

    List<Layer> wordTagLayers = schema.getLayers().values().stream()
      .filter(layer -> selectedLayers.contains(layer.getId()))
      // parent is word
      .filter(layer -> layer.getParentId() != null && wordLayer != null
              && layer.getParentId().equals(wordLayer.getId()))
      // not aligned
      .filter(layer -> layer.getAlignment() == Constants.ALIGNMENT_NONE)
      .collect(Collectors.toList());

    List<Layer> segmentLayers = schema.getLayers().values().stream()
      .filter(layer -> selectedLayers.contains(layer.getId()))
      // parent is word
      .filter(layer -> layer.getParentId() != null && wordLayer != null
              && layer.getParentId().equals(wordLayer.getId()))
      // aligned
      .filter(layer -> layer.getAlignment() != Constants.ALIGNMENT_NONE)
      .collect(Collectors.toList());
         
    boolean wordTokens
      // if explicitly selected
      = selectedLayers.contains(schema.getWordLayerId())
      // or there are word tag layers selected
      || wordTagLayers.size() > 0
      // or there are segment layers selected
      || segmentLayers.size() > 0;

    if (wordTokens) {
      // now word layer
      lLastUnusedAnnotationId = insertTier(
        wordLayer, graph, document, annotationDocument, timeOrder, mapAnchorToTimeslotId,
        lLastUnusedAnnotationId, warnings, language);
    }
      
    // word tag layers
    for (Layer layer : wordTagLayers) {
      lLastUnusedAnnotationId = insertTier(
        layer, graph, document, annotationDocument, timeOrder, mapAnchorToTimeslotId,
        lLastUnusedAnnotationId, warnings, language);
    }
      
    // segment layers
    for (Layer layer : segmentLayers) {
      lLastUnusedAnnotationId = insertTier(
        layer, graph, document, annotationDocument, timeOrder, mapAnchorToTimeslotId,
        lLastUnusedAnnotationId, warnings, language);
    }
      
    // trailing structure definitions
    Element linguisticType = document.createElement("LINGUISTIC_TYPE");
    annotationDocument.appendChild(linguisticType);
    linguisticType.setAttribute("GRAPHIC_REFERENCES","false");
    linguisticType.setAttribute("LINGUISTIC_TYPE_ID","default-lt");
    linguisticType.setAttribute("TIME_ALIGNABLE","true");
      
    linguisticType = document.createElement("LINGUISTIC_TYPE");
    annotationDocument.appendChild(linguisticType);
    linguisticType.setAttribute("GRAPHIC_REFERENCES","false");
    linguisticType.setAttribute("LINGUISTIC_TYPE_ID","tag-lt");
    linguisticType.setAttribute("TIME_ALIGNABLE","false");
    linguisticType.setAttribute("CONSTRAINTS","Symbolic_Association");
      
    linguisticType = document.createElement("LINGUISTIC_TYPE");
    annotationDocument.appendChild(linguisticType);
    linguisticType.setAttribute("GRAPHIC_REFERENCES","false");
    linguisticType.setAttribute("LINGUISTIC_TYPE_ID","subinterval-lt");
    linguisticType.setAttribute("TIME_ALIGNABLE","true");
    linguisticType.setAttribute("CONSTRAINTS","Included_In");
      
    linguisticType = document.createElement("LINGUISTIC_TYPE");
    annotationDocument.appendChild(linguisticType);
    linguisticType.setAttribute("GRAPHIC_REFERENCES","false");
    linguisticType.setAttribute("LINGUISTIC_TYPE_ID","partition-lt");
    linguisticType.setAttribute("TIME_ALIGNABLE","true");
    linguisticType.setAttribute("CONSTRAINTS","Time_Subdivision");

    linguisticType = document.createElement("LINGUISTIC_TYPE");
    annotationDocument.appendChild(linguisticType);
    linguisticType.setAttribute("GRAPHIC_REFERENCES","false");
    linguisticType.setAttribute("LINGUISTIC_TYPE_ID","token-lt");
    linguisticType.setAttribute("TIME_ALIGNABLE","false");
    linguisticType.setAttribute("CONSTRAINTS","Symbolic_Subdivision");
      
    if (language != null) {
      Element languageElement = document.createElement("LANGUAGE");
      annotationDocument.appendChild(languageElement);
      languageElement.setAttribute("LANG_ID", language);
      languageElement.setAttribute("LANG_LABEL", iso639.name(language).orElse(language));
      languageElement.setAttribute("LANG_DEF", "http://cdb.iso.org/lg/CDB-00138502-001");
    }
    Element constraint = document.createElement("CONSTRAINT");
    annotationDocument.appendChild(constraint);
    constraint.setAttribute("DESCRIPTION","Time subdivision of parent annotation's time interval, no time gaps allowed within this interval");
    constraint.setAttribute("STEREOTYPE","Time_Subdivision");
      
    constraint = document.createElement("CONSTRAINT");
    annotationDocument.appendChild(constraint);
    constraint.setAttribute("DESCRIPTION","Symbolic subdivision of a parent annotation. Annotations refering to the same parent are ordered");
    constraint.setAttribute("STEREOTYPE","Symbolic_Subdivision");
      
    constraint = document.createElement("CONSTRAINT");
    annotationDocument.appendChild(constraint);
    constraint.setAttribute("DESCRIPTION","1-1 association with a parent annotation");
    constraint.setAttribute("STEREOTYPE","Symbolic_Association");
      
    constraint = document.createElement("CONSTRAINT");
    annotationDocument.appendChild(constraint);
    constraint.setAttribute("DESCRIPTION","Time alignable annotations within the parent annotation's time interval, gaps are allowed");
    constraint.setAttribute("STEREOTYPE","Included_In");
      
    lastUnusedAnnotationId.setTextContent(""+(++lLastUnusedAnnotationId));

    // unlink the graph objects frome the XML elements
    for (Annotation a : graph.getAnnotationsById().values()) {
      a.remove("@alignableAnnotation");
      a.remove("@utterance");
    } // next annotation
      
    if (errors != null) throw errors;
      
    try {
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
         
      // indented XML 
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
         
      DOMSource source = new DOMSource(document);
      File f = File.createTempFile(IO.SafeFileNameUrl(graph.getId()), ".eaf");
      FileWriter fw = new FileWriter(f);
      StreamResult result = new StreamResult(fw);
      transformer.transform(source, result);
      TempFileInputStream in = new TempFileInputStream(f);

      // return a named stream from the file
      String name = IO.SafeFileNameUrl(IO.WithoutExtension(graph.getId())) + ".eaf";
      return new NamedStream(in, name);
    } catch(Exception exception) {
      errors = new SerializationException();
      errors.initCause(exception);
      errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
      throw errors;
    }      
  } // end of serializeGraph()

  /**
   * Ensures that the given anchor has a a timeslot in the given time order.  If there
   * isn't one already there, one is created. 
   * @param anchor
   * @param timeOrder
   * @param mapAnchorToTimeslotId
   * @return The TIME_SLOT_ID for the anchor
   */
  protected String ensureAnchorHasTimeslot(
    Anchor anchor, Document document, Element
    timeOrder, HashMap<String,String> mapAnchorToTimeslotId) {
      
    if (mapAnchorToTimeslotId.containsKey(anchor.getId()))
    {
      return mapAnchorToTimeslotId.get(anchor.getId());
    } else {
      Element timeSlot = document.createElement("TIME_SLOT");
      timeOrder.appendChild(timeSlot);
      String sId = "ts"+(mapAnchorToTimeslotId.size() + 1);
      timeSlot.setAttribute("TIME_SLOT_ID", sId);
      mapAnchorToTimeslotId.put(anchor.getId(), sId);
      if (anchor.getOffset() == null) {
        System.err.println("Anchor " + anchor.getId() + " has no offset, and will be ignored.");
      } else {
        long lMs = (long)((anchor.getOffset() * 1000)
                          + 0.5); // rounding, not truncating
        timeSlot.setAttribute("TIME_VALUE", ""+lMs);
      }
      return sId;
    }
  } // end of ensureAnchorHasTimeslot()
   
  /**
   * Inserts tiers for the layers that have the given scope (ignoring system layers)
   * @param layer The layer to convert into tiers
   * @param graph The graph
   * @param document The XML document
   * @param annotationDocument The ANNOTATION_DOCUMENT element
   * @param timeOrder The TIME_ORDER element
   * @param mapAnchorToTimeslotId A map from Anchor IDs to Timeslot IDs
   * @param lLastUnusedAnnotationId The last unused annotation ID 
   * @return The new value for lLastUnusedAnnotationId
   */
  protected long insertTier(
    Layer layer, Graph graph, Document document, Element annotationDocument, Element timeOrder,
    HashMap<String,String> mapAnchorToTimeslotId, long lLastUnusedAnnotationId,
    Consumer<String> warnings, String language) {
      
    if (layer.getAlignment() == 1) { // point layer
      warnings.accept("Cannot serialize time-point layers: " + layer.getId());
      return lLastUnusedAnnotationId; // can't represent points is EAF
    }

    /// unaligned or interval layer - either way, start/end anchors are distinct
	 
    // tiers for words (one tier collection for each speaker)
    // (we build a collection of like-named tiers
    //  which allows us to represent overlapping annotations
    //  if necessary)
    HashMap<String,Vector<Element>> mSpeakerTiers = new HashMap<String,Vector<Element>>();
    // to keep track of the latest time in the current tier
    HashMap<String,Vector<Double>> mSpeakerTimes = new HashMap<String,Vector<Double>>();
	 
    // in order to get longer annotations above shorter ones
    // (e.g. for constituent 'trees') we need to order
    // by start time, and then by reverse end time
    // TreeSet<Annotation> annotations 
    //    = new TreeSet<Annotation>(new AnnotationComparatorByAnchor());
    // annotations.addAll(ag.getLayer(layer.getId()));
    Annotation[] annotations = graph.all(layer.getId());

    // what relationship are we using, if any
    Layer dominatingLayer = layer.getParent();
    String sLinguisticTypeRef = "default-lt";
    String sAnnotationTag = "ALIGNABLE_ANNOTATION";
    // if (layer.getParentId() != null
    //     && !layer.getParentId().equals(graph.getSchema().getRoot().getId())) { // not top level
    //   if (!layer.getPeers()
    //       && layer.getSaturated()
    //       && layer.getAlignment() == Constants.ALIGNMENT_NONE
    //       && dominatingLayer.getId().equals(wordLayer.getId())) { // word tag layer
    //     sLinguisticTypeRef = "tag-lt";
    //   } else if (layer.getPeers()) {
    //     if (layer.getSaturated()) {
    //       sLinguisticTypeRef = "partition-lt";
    //     } else {
    //       sLinguisticTypeRef = "subinterval-lt";
    //     }
    //   }
    //   sAnnotationTag = "REF_ANNOTATION";
    // } // not top level
    if (!layer.getPeers()
        && layer.getSaturated()
        && layer.getAlignment() == Constants.ALIGNMENT_NONE
        && dominatingLayer.getId().equals(wordLayer.getId())) { // word tag layer
      sLinguisticTypeRef = "tag-lt";
      sAnnotationTag = "REF_ANNOTATION";
    } else if (layer.getPeers()) {
      if (layer.getSaturated()) {
        sLinguisticTypeRef = "partition-lt";
      } else {
        sLinguisticTypeRef = "subinterval-lt";
      }
    }
      
    // should we look for the participant layer?
    boolean ancestorOfParticipant = layer.getAncestors().contains(participantLayer);
      
    // for each annotation
    HashMap<String,Double> mapTierIdToLastOffset = new HashMap<String,Double>();
    for (Annotation annotation : annotations) {
      if (annotation.getStart().getOffset() == null) {
        warnings.accept("Annotation " + annotation.getId() + " \"" + annotation.getLabel()
                        + "\" has no start offset, and will be ignored.");
        continue;
      }
      if (annotation.getEnd().getOffset() == null) {
        warnings.accept("Annotation " + annotation.getId() + " \"" + annotation.getLabel()
                        + "\" has no end offset, and will be ignored.");
        continue;
      }
      Annotation participant = null;
      if (ancestorOfParticipant) {
        participant = annotation.first(participantLayer.getId());
      }
      String participantId = participant != null?participant.getId():"";
      String participantName = participant != null?participant.getLabel():"";

      // reference to dominating annotation?
      Annotation anDominating = annotation.containsKey("@utterance")
        ?(Annotation)annotation.get("@utterance")
        :annotation.getParent();
         
      Vector<Element> vTiers = mSpeakerTiers.get(participantId);
      if (vTiers == null) {
        // create a new tier
        vTiers = new Vector<Element>();
        Element firstTier = document.createElement("TIER");
        annotationDocument.appendChild(firstTier);
        firstTier.setAttribute("LINGUISTIC_TYPE_REF", sLinguisticTypeRef);
        String sId = layer.getId() + " - " + participantName;
        firstTier.setAttribute("TIER_ID", sId);
        firstTier.setAttribute("PARTICIPANT", participantName);
        if (language != null) firstTier.setAttribute("LANG_REF", language);
        if (anDominating != null) {
          Element dominatingElement = (Element)anDominating.get("@alignableAnnotation");
          if (dominatingElement != null) {
            firstTier.setAttribute(
              "PARENT_REF", 
              ((Element)dominatingElement.getParentNode().getParentNode()).getAttribute("TIER_ID"));
          }
        }
        vTiers.add(firstTier);
        mSpeakerTiers.put(participantId, vTiers);
        mapTierIdToLastOffset.put(sId, 0.0);	       
      }
	    
      // find a tier to add the interval to - i.e. the first
      // one whose last interval is before this one
      Element tier = null;
      for (Element t : vTiers) {
        // can we add to this tier?			
        if (mapTierIdToLastOffset.get(t.getAttribute("TIER_ID")).doubleValue()
            <= annotation.getStart().getOffset()) {
          tier = t;
          break;
        }
      } // next possible tier
      if (tier == null) {
        if (layer.getAlignment() == Constants.ALIGNMENT_NONE 
            && layer.getParent().getId().equals(wordLayer.getId())) { // unaligned word layer
          // subsequent annotations are alternatives,
          // so after we have the first one, skip the rest
          // TODO check for conjuctive (rather than disjunctive) cases like clitics dividing the word in two
          continue;
        } else {
          // can't add to any current tier, so add a new one
          tier = document.createElement("TIER");
          annotationDocument.appendChild(tier);
          tier.setAttribute("LINGUISTIC_TYPE_REF", sLinguisticTypeRef);
          String sId = layer.getId() + " - " + (vTiers.size() + 1) + " - " + participantName;
          tier.setAttribute("TIER_ID", sId); // TODO uniquify TIER_ID
          tier.setAttribute("PARTICIPANT", participantName);
          if (language != null) tier.setAttribute("LANG_REF", language);
          if (anDominating != null) {
            Element dominatingElement = (Element)anDominating.get("@alignableAnnotation");
            if (dominatingElement != null) {
              tier.setAttribute(
                "PARENT_REF", 
                ((Element)dominatingElement.getParentNode().getParentNode()).getAttribute("TIER_ID"));
            }
          }
          vTiers.add(tier);
          mapTierIdToLastOffset.put(sId, 0.0);
        }
      } // if tier == null 
	    
      // add an annotation to it
      Element elAnnotation = document.createElement("ANNOTATION");
      tier.appendChild(elAnnotation);
      Element alignableAnnotation = document.createElement(sAnnotationTag);
      elAnnotation.appendChild(alignableAnnotation);
      String sId = "a"+(++lLastUnusedAnnotationId);
      alignableAnnotation.setAttribute("ANNOTATION_ID", sId);
      annotation.put("@alignableAnnotation", alignableAnnotation);
         
      // anchors?
      if (layer.getAlignment() != Constants.ALIGNMENT_NONE) {
        alignableAnnotation.setAttribute(
          "TIME_SLOT_REF1", ensureAnchorHasTimeslot(
            annotation.getStart(), document, timeOrder, mapAnchorToTimeslotId));
        alignableAnnotation.setAttribute(
          "TIME_SLOT_REF2", ensureAnchorHasTimeslot(
            annotation.getEnd(), document, timeOrder, mapAnchorToTimeslotId));
      } else {
        // hopefully it's in the map!
        if (!anDominating.containsKey("@alignableAnnotation")) {
          warnings.accept("Cannot link to dominating ANNOTATION_ID: "
                          + annotation.getLayerId() + ":" + annotation.getLabel()
                          + " (" + annotation.getStart() + "-" + annotation.getEnd() + ")"
                          +" - dominated by: "
                          + anDominating.getLayerId() + ":" + anDominating.getLabel()
                          + " (" + anDominating.getStart() + "-" + anDominating.getEnd() + ")");
        } else {
          alignableAnnotation.setAttribute(
            "ANNOTATION_REF",
            ((Element)anDominating.get("@alignableAnnotation")).getAttribute("ANNOTATION_ID"));
        }
      }

      Element annotationValue = document.createElement("ANNOTATION_VALUE");
      alignableAnnotation.appendChild(annotationValue);
      annotationValue.setTextContent(annotation.getLabel());
	    
      // note the end time
      mapTierIdToLastOffset.put(
        tier.getAttribute("TIER_ID"), annotation.getEnd().getOffset());
         
    } // next annotation
	 
      // add all the tiers we created
    if (mSpeakerTiers.size() > 0) {
      for (String s : mSpeakerTiers.keySet()) {
        for (Element t : mSpeakerTiers.get(s)) {
          if (mSpeakerTiers.size() == 1) {
            // no need to distinguish tiers using speaker name...
            t.setAttribute("TIER_ID", layer.getId());
          }
          annotationDocument.appendChild(t);
        } // next tier
      } // next speaker
    } // there were tiers created
    return lLastUnusedAnnotationId;
  } // end of insertTier

  /** Command line interface, which takes transcript file names, and outputs JSON-encoded
   * annotation graphs. */
  public static void main(String argv[]) {
    EAFSerialization application = new EAFSerialization();
    if (application.processArguments(argv)) {
      application.start();
    }
  }
  /**
   * Specify the schema to used by  {@link Deserialize#convert(File)}.
   * @return The schema.
   */
  @Override protected Schema getDefaultSchema() {
    Schema schema = super.getDefaultSchema();
    schema.addLayer(
      new Layer("transcript_author", "Transcript Author")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_date", "Date of recording")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("transcript_language", "Transcript Language")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true));
    schema.addLayer(
      new Layer("language", "Phrase Language").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false).setParentId("turn"));
    schema.addLayer(
      new Layer("topic", "Topic")         
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false));
    schema.addLayer(
      new Layer("noise", "Noises")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false));
    schema.addLayer(
      new Layer("comment", "Comments").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(true).setSaturated(false));
    schema.addLayer(
      new Layer("pronounce", "Pronunciation tags").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getWordLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("lexical", "Lexical tags").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getWordLayerId()).setParentIncludes(true));
    return schema;
  }

} // end of class EAFSerialization
