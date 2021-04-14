//
// Copyright 2017-2020 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.formatter.text;

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
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
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
 * Deserializer for plain text files.
 * <p>These can be written documents, or transcripts of speech. Interlocutors, if any, may be identified with a configurable line-start pattern.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class PlainTextSerialization implements GraphDeserializer, GraphSerializer {
   
   // Attributes:
   protected Vector<String> warnings;
   
   /**
    * Name of the file.
    * @see #getName()
    * @see #setName(String)
    */
   protected String name;
   /**
    * Getter for {@link #name}: Name of the file.
    * @return Name of the file.
    */
   public String getName() { return name; }
   /**
    * Setter for {@link #name}: Name of the file.
    * @param newName Name of the file.
    */
   public PlainTextSerialization setName(String newName) { name = newName; return this; }

   /**
    * Lines of the header, if any.
    * @see #getHeaderLines()
    * @see #setHeaderLines(Vector<String>)
    */
   protected Vector<String> headerLines;
   /**
    * Getter for {@link #headerLines}: Lines of the header, if any.
    * @return Lines of the header, if any.
    */
   public Vector<String> getHeaderLines() { return headerLines; }
   /**
    * Setter for {@link #headerLines}: Lines of the header, if any.
    * @param newHeaderLines Lines of the header, if any.
    */
   public PlainTextSerialization setHeaderLines(Vector<String> newHeaderLines) { headerLines = newHeaderLines; return this; }

   /**
    * Lines of the main text.
    * @see #getLines()
    * @see #setLines(Vector<String>)
    */
   protected Vector<String> lines;
   /**
    * Getter for {@link #lines}: Lines of the main text.
    * @return Lines of the main text.
    */
   public Vector<String> getLines() { return lines; }
   /**
    * Setter for {@link #lines}: Lines of the main text.
    * @param newLines Lines of the main text.
    */
   public PlainTextSerialization setLines(Vector<String> newLines) { lines = newLines; return this; }

   /**
    * Whether the text includes marked speaker turns.
    * @see #getHasSpeakers()
    * @see #setHasSpeakers(boolean)
    */
   protected boolean hasSpeakers = false;
   /**
    * Getter for {@link #hasSpeakers}: Whether the text includes marked speaker turns.
    * @return Whether the text includes marked speaker turns.
    */
   public boolean getHasSpeakers() { return hasSpeakers; }
   /**
    * Setter for {@link #hasSpeakers}: Whether the text includes marked speaker turns.
    * @param newHasSpeakers Whether the text includes marked speaker turns.
    */
   public PlainTextSerialization setHasSpeakers(boolean newHasSpeakers) { hasSpeakers = newHasSpeakers; return this; }

   /**
    * Whether the text includes timestamps or not.
    * @see #getHasTimestamps()
    * @see #setHasTimestamps(booelan)
    */
   protected boolean hasTimestamps;
   /**
    * Getter for {@link #hasTimestamps}: Whether the text includes timestamps or not.
    * @return Whether the text includes timestamps or not.
    */
   public boolean getHasTimestamps() { return hasTimestamps; }
   /**
    * Setter for {@link #hasTimestamps}: Whether the text includes timestamps or not.
    * @param newHasTimestamps Whether the text includes timestamps or not.
    */
   public PlainTextSerialization setHasTimestamps(boolean newHasTimestamps) { hasTimestamps = newHasTimestamps; return this; }

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
   public PlainTextSerialization setSchema(Schema newSchema) { schema = newSchema; return this; }

   /**
    * Episode information layer.
    * @see #getEpisodeLayer()
    * @see #setEpisodeLayer(Layer)
    */
   protected Layer episodeLayer;
   /**
    * Getter for {@link #episodeayer}: Episode information layer.
    * @return Episode information layer.
    */
   public Layer getEpisodeLayer() { return episodeLayer; }
   /**
    * Setter for {@link #episodeLayer}: Episode information layer.
    * @param newEpisodeLayer Episode information layer.
    */
   public PlainTextSerialization setEpisodeLayer(Layer newEpisodeLayer) { episodeLayer = newEpisodeLayer; return this; }

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
   public PlainTextSerialization setParticipantLayer(Layer newParticipantLayer) { participantLayer = newParticipantLayer; return this; }

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
   public PlainTextSerialization setTurnLayer(Layer newTurnLayer) { turnLayer = newTurnLayer; return this; }

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
   public PlainTextSerialization setUtteranceLayer(Layer newUtteranceLayer) { utteranceLayer = newUtteranceLayer; return this; }

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
   public PlainTextSerialization setWordLayer(Layer newWordLayer) { wordLayer = newWordLayer; return this; }

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
   public PlainTextSerialization setLexicalLayer(Layer newLexicalLayer) { lexicalLayer = newLexicalLayer; return this; }

   /**
    * Layer for exceptional pronunciation tags.
    * @see #getPronounceLayer()
    * @see #setPronounceLayer(Layer)
    */
   protected Layer pronounceLayer;
   /**
    * Getter for {@link #pronounceLayer}: Layer for exceptional pronunciation tags.
    * @return Layer for exceptional pronunciation tags.
    */
   public Layer getPronounceLayer() { return pronounceLayer; }
   /**
    * Setter for {@link #pronounceLayer}: Layer for exceptional pronunciation tags.
    * @param newPronounceLayer Layer for exceptional pronunciation tags.
    */
   public PlainTextSerialization setPronounceLayer(Layer newPronounceLayer) { pronounceLayer = newPronounceLayer; return this; }

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
   public PlainTextSerialization setCommentLayer(Layer newCommentLayer) { commentLayer = newCommentLayer; return this; }

   /**
    * Layer for background noise.
    * @see #getNoiseLayer()
    * @see #setNoiseLayer(Layer)
    */
   protected Layer noiseLayer;
   /**
    * Getter for {@link #noiseLayer}: Layer for background noise.
    * @return Layer for background noise.
    */
   public Layer getNoiseLayer() { return noiseLayer; }
   /**
    * Setter for {@link #noiseLayer}: Layer for background noise.
    * @param newNoiseLayer Layer for background noise.
    */
   public PlainTextSerialization setNoiseLayer(Layer newNoiseLayer) { noiseLayer = newNoiseLayer; return this; }

   /**
    * Layer for orthography.
    * @see #getOrthographyLayer()
    * @see #setOrthographyLayer(Layer)
    */
   protected Layer orthographyLayer;
   /**
    * Getter for {@link #orthographyLayer}: Layer for orthography.
    * @return Layer for orthography.
    */
   public Layer getOrthographyLayer() { return orthographyLayer; }
   /**
    * Setter for {@link #orthographyLayer}: Layer for orthography.
    * @param newOrthographyLayer Layer for orthography.
    */
   public PlainTextSerialization setOrthographyLayer(Layer newOrthographyLayer) { orthographyLayer = newOrthographyLayer; return this; }

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
    * Setter for {@link #parameters}: Sets parameters for a given deserialization operation, after loading the serialized form of the graph. This might include mappings from format-specific objects like tiers to graph layers, etc.
    * @param newParameters The configuration for a given deserialization operation.
    * @throws SerializationParametersMissingException If not all required parameters have a value.
    */
   public void setParameters(ParameterSet newParameters)
      throws SerializationParametersMissingException {
      parameters = newParameters;
   }

   /**
    * Whether to use text conventions for comment, noise, lexical, and pronunciation annotations.
    * @see #getUseConventions()
    * @see #setUseConventions(Boolean)
    */
   protected Boolean useConventions = Boolean.TRUE;
   /**
    * Getter for {@link #useConventions}: Whether to use text conventions for comment, noise, lexical, and pronunciation annotations.
    * @return Whether to use text conventions for comment, noise, lexical, and pronunciation annotations.
    */
   public Boolean getUseConventions() { return useConventions == null?Boolean.FALSE:useConventions; }
   /**
    * Setter for {@link #useConventions}: Whether to use text conventions for comment, noise, lexical, and pronunciation annotations.
    * @param newUseConventions Whether to use text conventions for comment, noise, lexical, and pronunciation annotations.
    */
   public PlainTextSerialization setUseConventions(Boolean newUseConventions) { useConventions = newUseConventions; return this; }

   /**
    * Delimiters used for comments, a string whose first characters is the open-comment
    * marker, and the last character is the close-comment marker. The default is "{}" 
    * @see #getCommentDelimiters()
    * @see #setCommentDelimiters(String)
    */
   protected String commentDelimiters; // TODO
   /**
    * Getter for {@link #commentDelimiters}: Delimiters used for comments, a string whose
    * first characters is the open-comment marker, and the last character is the
    * close-comment marker. The default is "{}" 
    * @return Delimiters used for comments, a string whose first characters is the
    * open-comment marker, and the last character is the close-comment marker. The default
    * is "{}" 
    */
   public String getCommentDelimiters() { return commentDelimiters; }
   /**
    * Setter for {@link #commentDelimiters}: Delimiters used for comments, a string whose
    * first characters is the open-comment marker, and the last character is the
    * close-comment marker. The default is "{}" 
    * @param newCommentDelimiters Delimiters used for comments, a string whose first
    * characters is the open-comment marker, and the last character is the close-comment
    * marker. The default is "{}" 
    */
   public PlainTextSerialization setCommentDelimiters(String newCommentDelimiters) { commentDelimiters = newCommentDelimiters; return this; }

   /**
    * Delimiters used for noises, a string whose first characters is the open-comment marker, and the last character is the close-comment marker. The default is "[]"
    * @see #getNoiseDelimiters()
    * @see #setNoiseDelimiters(String)
    */
   protected String noiseDelimiters; // TODO
   /**
    * Getter for {@link #noiseDelimiters}: Delimiters used for noises, a string whose
    * first characters is the open-comment marker, and the last character is the
    * close-comment marker. The default is "[]" 
    * @return Delimiters used for noises, a string whose first characters is the
    * open-comment marker, and the last character is the close-comment marker. The default
    * is "[]" 
    */
   public String getNoiseDelimiters() { return noiseDelimiters; }
   /**
    * Setter for {@link #noiseDelimiters}: Delimiters used for noises, a string whose
    * first characters is the open-comment marker, and the last character is the
    * close-comment marker. The default is "[]" 
    * @param newNoiseDelimiters Delimiters used for noises, a string whose first
    * characters is the open-comment marker, and the last character is the close-comment
    * marker. The default is "[]" 
    */
   public PlainTextSerialization setNoiseDelimiters(String newNoiseDelimiters) { noiseDelimiters = newNoiseDelimiters; return this; }

   /**
    * Format for marking a change of turn within the transcript body. Default pattern is
    * "{0}: ", where <tt>{0}</tt> is a place-holder for the participant ID/name. 
    * @see #getParticipantFormat()
    * @see #setParticipantFormat(String)
    */
   protected String participantFormat = "{0}: ";
   /**
    * Getter for {@link #participantFormat}: Format for marking a change of turn within
    * the transcript body. 
    * @return Format for marking a change of turn within the transcript body.
    */
   public String getParticipantFormat() { return participantFormat; }
   /**
    * Setter for {@link #participantFormat}: Format for marking a change of turn within
    * the transcript body. 
    * @param newParticipantFormat Format for marking a change of turn within the transcript body.
    */
   public PlainTextSerialization setParticipantFormat(String newParticipantFormat) { participantFormat = newParticipantFormat; return this; }
   
   /**
    * Format for a meta-data line. Default pattern is "{0}={1}".  <tt>{0}</tt> is a
    * place-holder for the attribute name or key, and <tt>{1}</tt> is a place-holder for
    * the attribute value. 
    * @see #getMetaDataFormat()
    * @see #setMetaDataFormat(String)
    */
   protected String metaDataFormat = "{0}={1}";
   /**
    * Getter for {@link #metaDataFormat}: Format for a meta-data line.
    * @return Format for a meta-data line.
    */
   public String getMetaDataFormat() { return metaDataFormat; }
   /**
    * Setter for {@link #metaDataFormat}: Format for a meta-data line.
    * @param newMetaDataFormat Format for a meta-data line.
    */
   public PlainTextSerialization setMetaDataFormat(String newMetaDataFormat) { metaDataFormat = newMetaDataFormat; return this; }

   /**
    * The maximum length of a parsed participant ID/name.  Default is 20.
    * @see #getMaxParticipantLength()
    * @see #setMaxParticipantLength(Integer)
    */
   protected Integer maxParticipantLength;
   /**
    * Getter for {@link #maxParticipantLength}: The maximum length of a parsed participant
    * ID/name.  Default is 20. 
    * @return The maximum length of a parsed participant ID/name.  Default is 20.
    */
   public Integer getMaxParticipantLength() { return maxParticipantLength; }
   /**
    * Setter for {@link #maxParticipantLength}: The maximum length of a parsed participant ID/name.
    * @param newMaxParticipantLength The maximum length of a parsed participant ID/name. 
    */
   public PlainTextSerialization setMaxParticipantLength(Integer newMaxParticipantLength) { maxParticipantLength = newMaxParticipantLength; return this; }

   /**
    * Format for time synchronizations within the transcript body. e.g. HH:mm:ss.SSS
    * @see #getTimestampFormat()
    * @see #setTimestampFormat(String)
    */
   protected String timestampFormat = "HH:mm:ss.SSS";
   /**
    * Getter for {@link #timestampFormat}: Format for time synchronizations within the transcript body.
    * @return Format for time synchronizations within the transcript body. 
    */
   public String getTimestampFormat() { return timestampFormat; }
   /**
    * Setter for {@link #timestampFormat}: Format for time synchronizations within the transcript body. 
    * @param newTimestampFormat Format for time synchronizations within the transcript body. e.g. "HH:mm:ss.SSS"
    */
   public PlainTextSerialization setTimestampFormat(String newTimestampFormat) { timestampFormat = newTimestampFormat; return this; }

   /**
    * Duration of the media file in seconds, if known.
    * @see #getMediaDurationSeconds()
    * @see #setMediaDurationSeconds(Double)
    */
   protected Double mediaDurationSeconds;
   /**
    * Getter for {@link #mediaDurationSeconds}: Duration of the media file in seconds, if known.
    * @return Duration of the media file in seconds, if known.
    */
   public Double getMediaDurationSeconds() { return mediaDurationSeconds; }
   /**
    * Setter for {@link #mediaDurationSeconds}: Duration of the media file in seconds, if known.
    * @param newMediaDurationSeconds Duration of the media file in seconds, if known.
    */
   public PlainTextSerialization setMediaDurationSeconds(Double newMediaDurationSeconds) { mediaDurationSeconds = newMediaDurationSeconds; return this; }

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
   public PlainTextSerialization setTokenizer(GraphTransformer newTokenizer) { tokenizer = newTokenizer; return this; }

   /**
    * Maximum lines in a header
    * @see #getMaxHeaderLines()
    * @see #setMaxHeaderLines(Integer)
    */
   protected Integer maxHeaderLines = Integer.valueOf(50);
   /**
    * Getter for {@link #maxHeaderLines}: Maximum lines in a header
    * @return Maximum lines in a header
    */
   public Integer getMaxHeaderLines() { return maxHeaderLines; }
   /**
    * Setter for {@link #maxHeaderLines}: Maximum lines in a header
    * @param newMaxHeaderLines Maximum lines in a header
    */
   public PlainTextSerialization setMaxHeaderLines(Integer newMaxHeaderLines) { maxHeaderLines = newMaxHeaderLines; return this; }
   
   /**
    * Error encountered when trying to get length of media, if any.
    * @see #getMediaError()
    * @see #setMediaError(String)
    */
   protected String mediaError;
   /**
    * Getter for {@link #mediaError}: Error encountered when trying to get length of media, if any.
    * @return Error encountered when trying to get length of media, if any.
    */
   public String getMediaError() { return mediaError; }
   /**
    * Setter for {@link #mediaError}: Error encountered when trying to get length of media, if any.
    * @param newMediaError Error encountered when trying to get length of media, if any.
    */
   public PlainTextSerialization setMediaError(String newMediaError) { mediaError = newMediaError; return this; }

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
   public PlainTextSerialization setTimers(Timers newTimers) { timers = newTimers; return this; }

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
   public PlainTextSerialization setCancelling(boolean newCancelling) { cancelling = newCancelling; return this; }
   
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
   public PlainTextSerialization() {
   } // end of constructor
   
   // IStreamDeserializer methods:
   
   /**
    * Returns the deserializer's descriptor
    * @return The deserializer's descriptor
    */
   public SerializationDescriptor getDescriptor() {
      return new SerializationDescriptor(
         "Plain Text Document", getClass().getPackage().getImplementationVersion(),
         "text/plain", ".txt", "1.0.0",
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
    * @return A list of configuration parameters (still) must be set before {@link
    * GraphDeserializer#setParameters(ParameterSet)} can be invoked. If this is an empty list,
    * {@link GraphDeserializer#setParameters(ParameterSet)} can be invoked. If it's not an
    * empty list, this method must be invoked again with the returned parameters' values
    * set. 
    */
   public ParameterSet configure(ParameterSet configuration, Schema schema) {
      setSchema(schema);
      setEpisodeLayer(schema.getEpisodeLayer());
      setParticipantLayer(schema.getParticipantLayer());
      setTurnLayer(schema.getTurnLayer());
      setUtteranceLayer(schema.getUtteranceLayer());
      setWordLayer(schema.getWordLayer());

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
      LinkedHashMap<String,Layer> episodeTagLayers = new LinkedHashMap<String,Layer>();
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
         for (Layer tag : getWordLayer().getChildren().values()) {
            if (tag.getAlignment() == Constants.ALIGNMENT_NONE
                && tag.getChildren().size() == 0) {
               wordTagLayers.put(tag.getId(), tag);
            }
         } // next possible word tag layer
         for (Layer tag : getParticipantLayer().getChildren().values()) {
            if (tag.getAlignment() == Constants.ALIGNMENT_NONE
                && tag.getChildren().size() == 0) {
               participantTagLayers.put(tag.getId(), tag);
            }
         } // next possible participant tag layer
         if (getEpisodeLayer() != null) {
            for (Layer tag : getEpisodeLayer().getChildren().values()) {
               if (tag.getAlignment() == Constants.ALIGNMENT_NONE
                   && tag.getChildren().size() == 0) {
                  episodeTagLayers.put(tag.getId(), tag);
               }
            } // next possible word tag layer
         } // there is an episode layer
      }
      participantTagLayers.remove("main_participant");
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

      // other layers...

      layerToPossibilities.put(
         new Parameter("commentLayer", Layer.class, "Comment layer", "Commentary"), 
         Arrays.asList("comment","commentary","note","notes"));
      layerToCandidates.put("commentLayer", topLevelLayers);

      layerToPossibilities.put(
         new Parameter("noiseLayer", Layer.class, "Noise layer", "Background noises"), 
         Arrays.asList("noise","noises","background","backgroundnoise"));
      layerToCandidates.put("noiseLayer", topLevelLayers);

      layerToPossibilities.put(
         new Parameter("lexicalLayer", Layer.class, "Lexical layer", "Lexical tags"), 
         Arrays.asList("lexical"));
      layerToCandidates.put("lexicalLayer", wordTagLayers);

      layerToPossibilities.put(
         new Parameter("pronounceLayer", Layer.class, "Pronounce layer", "Non-standard pronunciation tags"), 
         Arrays.asList("pronounce", "pronounced"));
      layerToCandidates.put("pronounceLayer", wordTagLayers);

      layerToPossibilities.put(
         new Parameter("orthographyLayer", Layer.class, "Orthography layer", "Orthography"), 
         Arrays.asList("orthography"));
      layerToCandidates.put("orthographyLayer", wordTagLayers);

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
         configuration.get("useConventions").setValue(Boolean.TRUE);
      }
      if (!configuration.containsKey("maxParticipantLength")) {
         configuration.addParameter(
            new Parameter("maxParticipantLength", Integer.class, 
                          "Max Participant Length",
                          "The maximum length of a participant name", true));
      }
      if (configuration.get("maxParticipantLength").getValue() == null) {
         configuration.get("maxParticipantLength").setValue(Integer.valueOf(20));
      }
      
      if (!configuration.containsKey("maxHeaderLines")) {
         configuration.addParameter(
            new Parameter("maxHeaderLines", Integer.class, 
                          "Max Header Lines",
                          "The maximum number of lines in a meta-data header", true));
      }
      if (configuration.get("maxHeaderLines").getValue() == null) {
         configuration.get("maxHeaderLines").setValue(Integer.valueOf(50));
      }
      
      if (!configuration.containsKey("participantFormat")) {
         configuration.addParameter(
            new Parameter("participantFormat", String.class, 
                          "Participant Format",
                          "Format for marking a change of turn within the transcript body - e.g. {0}:, where {0} is a place-holder for the participant ID/name", true));
      }
      if (configuration.get("participantFormat").getValue() == null) {
         configuration.get("participantFormat").setValue(participantFormat);
      }
      
      if (!configuration.containsKey("metaDataFormat")) {
         configuration.addParameter(
            new Parameter("metaDataFormat", String.class, 
                          "Meta-data Format",
                          "Format for a meta-data line in the header - e.g. {0}={1}, where {0} is a place-holder for the attribute name or key, and {1} is a place-holder for the attribute value", true));
      }
      if (configuration.get("metaDataFormat").getValue() == null) {
         configuration.get("metaDataFormat").setValue(metaDataFormat);
      }

      if (!configuration.containsKey("timestampFormat")) {
         configuration.addParameter(
            new Parameter("timestampFormat", String.class, 
                          "Time-stamp Format",
                          "Format for a time stamp - e.g. HH:mm:ss.SSS", true));
      }
      if (configuration.get("timestampFormat").getValue() == null) {
         configuration.get("timestampFormat").setValue(timestampFormat);
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
      // take the first stream, ignore all others.
      NamedStream txt = Utility.FindSingleStream(streams, ".txt", "text/plain");
      if (txt == null) throw new SerializationException("No text document stream found");
      setName(txt.getName());
      
      setSchema(schema);

      // create a list of layers we need and possible matching layer names
      LinkedHashMap<Parameter,List<String>> layerToPossibilities
         = new LinkedHashMap<Parameter,List<String>>();
      HashMap<String,LinkedHashMap<String,Layer>> layerToCandidates
         = new HashMap<String,LinkedHashMap<String,Layer>>();	 
      
      LinkedHashMap<String,Layer> metadataLayers = new LinkedHashMap<String,Layer>();
      for (Layer layer : schema.getRoot().getChildren().values()) {
         if (layer.getAlignment() == Constants.ALIGNMENT_NONE) {
            metadataLayers.put(layer.getId(), layer);
         }
      } // next turn child layer

      // look for person attributes
      for (Layer layer : schema.getParticipantLayer().getChildren().values()) {
         if (layer.getAlignment() == Constants.ALIGNMENT_NONE) {
            metadataLayers.put(layer.getId(), layer);
         }
      } // next turn child layer

      // look for episode attributes
      if (schema.getEpisodeLayer() != null) {
         for (Layer layer : schema.getEpisodeLayer().getChildren().values()) {
            if (layer.getAlignment() == Constants.ALIGNMENT_NONE) {
               metadataLayers.put(layer.getId(), layer);
            }
         } // next turn child layer
      }

      // read through the text looking for clues about whether:
      //  * it's time aligned
      //  * it contains speaker names
      //  * it has layers, e.g. tagged words limited with _ TODO
      //  * it has a header and a body, or is all body

      setHeaderLines(new Vector<String>());
      setLines(new Vector<String>());

      BufferedReader reader = new BufferedReader(new InputStreamReader(txt.getStream(), "UTF-8"));
      String sLine = reader.readLine();
      int iLine = 0;
      MessageFormat fmtSpeakerFormat = new MessageFormat(participantFormat);
      SimpleDateFormat fmtTimestampFormat = null;
      if (timestampFormat != null && timestampFormat.length() > 0) {
         // prefix with {5} and {6} to include the possibility that the timestamp
         // isn't at the beginning or end of the line
         fmtTimestampFormat = new SimpleDateFormat(timestampFormat);
         fmtTimestampFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
      }
      while (sLine != null) {
         if (iLine == 0) { // first line
            // strip of the UTF-8 BOM if there is one.
            if (sLine.startsWith("\uFEFF")) {
               sLine = sLine.replaceAll("\uFEFF","");
            }
         }
         iLine++;

         String leftover = sLine;

         // if we encounter a speaker utterance within the first 150 lines, 
         // it contains speakers (after that, we assume it's a coincidence)
         if (iLine <= 150 && !getHasSpeakers()) {
            // is this line a speaker utterance line?
            try {
               Object[] oSpeaker = fmtSpeakerFormat.parse(sLine);
               String id = (String)oSpeaker[0];
               if (getMaxParticipantLength() == null
                   || id.length() <= getMaxParticipantLength()) {
                  setHasSpeakers(true);
                  if (iLine > 1) {
                     // this is the first speaker we've seen, so everything before this is header
                     setHeaderLines(getLines());
                     // and the transcript content starts here
                     setLines(new Vector<String>());
                  }
                  leftover = leftover.substring(fmtSpeakerFormat.format(oSpeaker).length()).trim();
               }
            }
            catch(ParseException exception) {} // not parseable
            catch(NullPointerException exception) {} // null ID
         } // early enough and we don't know whether has speakers or not yet

         if (!getHasTimestamps() && fmtTimestampFormat != null) {
            // is this line a speaker utterance line?
            try {
               fmtTimestampFormat.parse(leftover); 
               // parsed, so there are timestamps
               setHasTimestamps(true);
            }
            catch(ParseException exception) {} // not parseable
            catch(NullPointerException exception) {} // null ID
         } // don't know whether hasTimestamps yet

         getLines().add(sLine);
         sLine = reader.readLine();
      } // next line
      reader.close();

      // media duration
      NamedStream wav = Utility.FindSingleStream(streams, ".wav", "audio/wav");
      if (wav != null) {
         // save the media file
         File fMedia = File.createTempFile("PlainTextSerialization-", wav.getName());
         fMedia.deleteOnExit();
         try {
            // we cannot just use the stream directly, because AudioSystem.getAudioInputStream()
            // requires a mark/reset-able stream, which we can't guarantee that we have
            // so we save the stream to a file, and give AudioSystem.getAudioInputStream() that file
            IO.SaveInputStreamToFile​(wav.getStream(), fMedia);

            // determine the duration of the media file
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(fMedia);
            AudioFormat format = audioInputStream.getFormat();
            long frames = audioInputStream.getFrameLength();
            if (frames > 0) {
               double durationInSeconds = ((double)frames) / format.getFrameRate(); 
               setMediaDurationSeconds(durationInSeconds);
            } else {
               setMediaError("Ignoring media: " + wav.getName() + " is valid but contains no frames.");
            }
         } catch(Exception exception) {
            setMediaError("Ignoring media: " + wav.getName() + " ERROR: " + exception);
         } finally {
            fMedia.delete();
         }
      } else {
         NamedStream video = Utility.FindSingleStream(streams, ".webm", "video/webm");
         if (video == null) video = Utility.FindSingleStream(streams, ".mp4", "video/mp4");
         if (video == null) video = Utility.FindSingleStream(streams, ".mov", "video/quicktime");
         if (video != null) {
            setMediaDurationSeconds(100.0); // TODO find the actual length of the video
         }
      }

      // if there are headers, we need to map them to layers
      MessageFormat fmtMetaDataFormat = new MessageFormat(metaDataFormat);
      for (String header : getHeaderLines()) {
         if (header.trim().length() == 0) continue;
         try {
            Object[] oMetaData = fmtMetaDataFormat.parse(header);
            String key = (String)oMetaData[0];
            Vector<String> possibleMatches = new Vector<String>();
            possibleMatches.add("transcript" + key);
            possibleMatches.add("participant" + key);
            possibleMatches.add("speaker" + key);
            possibleMatches.add(key);
	    
            layerToPossibilities.put(
               new Parameter("header_"+key, Layer.class, "Header: " + key), 
               possibleMatches);
            layerToCandidates.put("header_"+key, metadataLayers);
         } catch(ParseException exception) {} // not parseable
      } // next header
	 
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
      if (timers != null) timers.start("initialization");
      
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

      validate();

      // if there are errors, accumlate as many as we can before throwing SerializationException
      SerializationException errors = null;

      Graph graph = new Graph();
      graph.setTimers(timers);
      graph.setId(getName());
      double dOffsetFactor = 1.0;
      if (!getHasTimestamps() && getMediaDurationSeconds() == null) {
         graph.setOffsetUnits(Constants.UNIT_CHARACTERS);
      } else {
         graph.setOffsetUnits(Constants.UNIT_SECONDS);
         if (!getHasTimestamps()) { // no timestamps
            // use the length of the media file to scale the offsets
            if (getMediaDurationSeconds() != null) {
               // get the length in characters
               long charCount = 0;
               for (String sLine : getLines()) {
                  charCount += sLine.length() + 2; // +2 for line terminators
               } // next line
               // set the offset factor to divide characters evenly through the media duration
               dOffsetFactor = getMediaDurationSeconds().doubleValue() / charCount;
            }
         } // no timestamps
      }

      // alignment status to give words, if there are no timestamps
      int iWordAlignmentConfidence = graph.getOffsetUnits() == Constants.UNIT_CHARACTERS?
         // if there are no speakers, then this is not a spoken text,
         // so (character) offsets are correct
         Constants.CONFIDENCE_MANUAL
         // if there are speakers, then this is an unaligned transcript, 
         // so ensure anchors will be updated if it's layer aligned and reuploaded, 
         // by using an 'untrusted' status
         :Constants.CONFIDENCE_DEFAULT;
      
      // creat the 0 anchor to prevent graph tagging from creating one with no confidence
      Anchor firstAnchor = graph.getOrCreateAnchorAt(0.0, Constants.CONFIDENCE_MANUAL);
      Anchor lastAnchor = firstAnchor;

      // add layers to the graph
      // we don't just copy the whole schema, because that would imply that all the extra layers
      // contained no annotations, which is not necessarily true
      if (episodeLayer != null) {
         graph.addLayer((Layer)episodeLayer.clone());
         graph.getSchema().setEpisodeLayerId(episodeLayer.getId());
      }
      graph.addLayer((Layer)participantLayer.clone());
      graph.getSchema().setParticipantLayerId(participantLayer.getId());
      graph.addLayer((Layer)turnLayer.clone());
      graph.getSchema().setTurnLayerId(turnLayer.getId());
      graph.addLayer((Layer)utteranceLayer.clone());
      graph.getSchema().setUtteranceLayerId(utteranceLayer.getId());
      graph.addLayer((Layer)wordLayer.clone());
      graph.getSchema().setWordLayerId(wordLayer.getId());
      if (pronounceLayer != null) graph.addLayer((Layer)pronounceLayer.clone());
      if (lexicalLayer != null) graph.addLayer((Layer)lexicalLayer.clone());
      if (noiseLayer != null) graph.addLayer((Layer)noiseLayer.clone());
      if (commentLayer != null) graph.addLayer((Layer)commentLayer.clone());
      if (parameters != null) {
         for (Parameter p : parameters.values()) {
            Layer layer = (Layer)p.getValue();
            if (layer != null && graph.getLayer(layer.getId()) == null) {
               // haven't added this layer yet
               graph.addLayer((Layer)layer.clone());
            }
         }
      }

      HashMap<String,Annotation> participants = new HashMap<String,Annotation>();
      Annotation participant = new Annotation(
         null,
         // default participant name is "author" for texts...
         graph.getOffsetUnits() == Constants.UNIT_CHARACTERS?"author"
         // ...but is the name of the file for recordings
         :IO.WithoutExtension(graph.getId()),
         schema.getParticipantLayerId());
      participant.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
      Vector<Annotation> participantTags = new Vector<Annotation>();
      MessageFormat fmtMetaDataFormat = new MessageFormat(metaDataFormat);
      for (String header : getHeaderLines()) {
         if (header.trim().length() == 0) continue;
         try { // parse meta data header
            Object[] oMetaData = fmtMetaDataFormat.parse(header);
            String key = (String)oMetaData[0];
            Layer layer = (Layer)parameters.get("header_" + key).getValue();
            if (layer != null) {
               String value = (String)oMetaData[1];
               if (layer.getParentId().equals(schema.getRoot().getId())) { // graph tag
                  graph.createTag(graph, layer.getId(), value)
                     .setConfidence(Constants.CONFIDENCE_MANUAL);
               } else if (getEpisodeLayer() != null
                        && layer.getParentId().equals(getEpisodeLayer().getId())) { // episode tag
                  Annotation episode = graph.first(getEpisodeLayer().getId());
                  if (episode == null) {
                     episode = graph.createTag(graph, getEpisodeLayer().getId(), graph.getLabel());
                     episode.setConfidence(Constants.CONFIDENCE_MANUAL);
                  }
                  graph.createTag(episode, layer.getId(), value)
                     .setConfidence(Constants.CONFIDENCE_MANUAL);
               } else { // participant tag
                  Annotation tag = new Annotation(null, value, layer.getId());
                  tag.setConfidence(Constants.CONFIDENCE_MANUAL);
                  participantTags.add(tag);
               }
            }
         } catch(ParseException exception) { // not parseable
            // add the header line as a comment
            if (getCommentLayer() != null) {
               int iNumChars = header.length();
               Annotation anComment = new Annotation(
                  null, header, getCommentLayer().getId(),
                  graph.getOrCreateAnchorAt(0.0, Constants.CONFIDENCE_MANUAL).getId(),
                  graph.getOrCreateAnchorAt(0.0, Constants.CONFIDENCE_MANUAL).getId());
               anComment.setConfidence(Constants.CONFIDENCE_MANUAL);
               graph.addAnnotation(anComment);
            } // there is a comment layer
         } // comment header
      } // next header line
      setHeaderLines(null); // allow lines to be garbage collected
	 
      if (timers != null) timers.end("initialization");
      
      // graph
      Annotation turn = new Annotation(
         null, participant.getLabel(), getTurnLayer().getId());
      turn.setConfidence(Constants.CONFIDENCE_MANUAL);
      graph.addAnnotation(turn);
      turn.setParent(participant);
      turn.setStart(
         graph.getOrCreateAnchorAt(0.0, Constants.CONFIDENCE_MANUAL));
      Annotation line = new Annotation(null, "", getUtteranceLayer().getId());
      line.setParentId(turn.getId())
         .setStart(turn.getStart())
         .setConfidence(Constants.CONFIDENCE_MANUAL);;
      int iLastPosition = 0;	 

      MessageFormat fmtSpeakerFormat = new MessageFormat(participantFormat);
      SimpleDateFormat fmtTimestampFormat = null;
      if (timestampFormat != null && timestampFormat.length() > 0) {
         fmtTimestampFormat = new SimpleDateFormat(timestampFormat);
         fmtTimestampFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
      }

      if (timers != null) timers.start("process lines");

      Annotation lastLine = null;
      int lineOrdinal = 1;
      for (String sLine : getLines()) {
         sLine = sLine.trim();
         // if (sLine.length() == 0 && getHasSpeakers())
         // {
         //    // a blank line in a transcript is taken to be a break in speaker
         //    // subsequent lines with text but no speaker are taken to be notes/comments
         //    participant = null;
         //    continue;
         // }
	 
         int iNumChars = sLine.length();
         line = new Annotation(null, "", getUtteranceLayer().getId());
         line.setParentId(turn.getId())
            .setStart(turn.getStart())
            .setOrdinal(lineOrdinal++)
            .setConfidence(Constants.CONFIDENCE_MANUAL);;
	 
         line.setStart(lastAnchor);
         if (lastLine != null) {
            if (lastAnchor.getId() == null) {
               graph.addAnchor(lastAnchor);
            }
            lastLine.setEndId(lastAnchor.getId());
            if (timers != null) timers.start("add line annotation");
            graph.addAnnotation(lastLine);
            if (timers != null) timers.end("add line annotation");
         }
	 
         if (getHasSpeakers())
         {
            // does the line start with a speaker ID?
            try {
               if (timers != null) timers.start("check for speaker change");
               Object[] oSpeaker = fmtSpeakerFormat.parse(sLine);
               if (timers != null) timers.end("check for speaker change");
               String sSpeakerId = (String)oSpeaker[0];
               if (getMaxParticipantLength() == null
                   || sSpeakerId.length() <= getMaxParticipantLength()) {
                  // speaker
                  participant = participants.get(sSpeakerId);
                  if (participant == null) {
                     participant = new Annotation(null, sSpeakerId, schema.getParticipantLayerId());
                     participant.setConfidence(Constants.CONFIDENCE_MANUAL);
                     graph.addAnnotation(participant);
                     participants.put(sSpeakerId, participant);
                  }
		  
                  if (lastAnchor.getOffset() != null && lastAnchor.getOffset().equals(0.0)) {
                     // just started, so recycle the turn and utterance we're already in
                     turn.setLabel(participant.getLabel());
                     turn.setParentId(participant.getId());
                     line.setLabel(participant.getLabel());
                     line.setParentId(turn.getId());
                  } else {
                     // finish old turn
                     if (turn != null && turn.getStart() != lastAnchor) {
                        turn.setEnd(lastAnchor);
                        if (timers != null) timers.start("add old turn annotation");
                        graph.addAnnotation(turn);
                        if (timers != null) timers.end("add old turn annotation");
                     }

                     lineOrdinal = 1;
		     
                     // new turn		  
                     turn = new Annotation(null, participant.getLabel(), getTurnLayer().getId());
                     turn.setParentId(participant.getId())
                        .setConfidence(Constants.CONFIDENCE_MANUAL);
                     if (timers != null) timers.start("add new turn annotation");
                     graph.addAnnotation(turn);
                     if (timers != null) timers.end("add new turn annotation");
                     turn.setStart(lastAnchor);
                     // start a new line too
                     line.setEnd(lastAnchor);
                     line.setOrdinal(lineOrdinal++);
                     if (line.getStartId() != null
                         && !line.getStartId().equals(line.getEndId())) {
                        // if we have <div><p>... don't create an instantaneous, empty line
                        graph.addAnnotation(line);
                     }
                     line = new Annotation(null, "", getUtteranceLayer().getId());
                     line.setParentId(turn.getId())
                        .setStart(lastAnchor)
                        .setConfidence(Constants.CONFIDENCE_MANUAL);

                  }
                  // consume the speaker ID
                  sLine = sLine.substring(fmtSpeakerFormat.format(oSpeaker).length()).trim();
               } // speaker found
            } catch(ParseException exception) {
               // System.out.println("SPEAKER: " + exception);
            } catch(NullPointerException exception) {
               // System.out.println("SPEAKER: " + exception);
            } // null ID
         } // HasSpeakers
	 
         if (graph.getOffsetUnits() != Constants.UNIT_CHARACTERS) {
            // does the line start with a time stamp?
            if (getHasTimestamps()) {
               if (timers != null) timers.start("check for timestamp");
               try {
                  double dSeconds = 0.0;
                  Date timestamp = fmtTimestampFormat.parse(sLine);
                  lastAnchor.setOffset(((double)(timestamp.getTime()))/1000);
                  lastAnchor.setConfidence(Constants.CONFIDENCE_MANUAL);
		  
                  // consume the timestamp
                  sLine = sLine.substring(fmtTimestampFormat.format(timestamp).length()).trim();
               } // timestamp found
               catch(ParseException exception) {} // not parseable
               if (timers != null) timers.end("check for timestamp");
            } // HasTimestamps
         }

         // process text
         if (sLine.length() > 0) {
            if (participant != null) { // speech or text

               // temporary set label to transcription
               line.setLabel(sLine
                             // ensure ellipsis (sometimes appearing in MS Word transcripts)
                             // ends up as a token boundary
                             .replaceAll("…", "… "));
               lastLine = line;
            } else { // no speaker, so taken as comments
               Annotation anComment = new Annotation(null, sLine, getCommentLayer().getId());
               anComment.setStart(lastAnchor)
                  .setConfidence(Constants.CONFIDENCE_MANUAL);	       
               lastLine = anComment;
            } // comments
         } // there is text on this line
	 
         // update current position
         if (!getHasTimestamps()) {
            if (timers != null) timers.start("getOrCreateAnchorAt");
            lastAnchor = graph.getOrCreateAnchorAt(
               lastAnchor.getOffset() + (((double)iNumChars + 1) * dOffsetFactor), iWordAlignmentConfidence);
            if (timers != null) timers.start("getOrCreateAnchorAt");
         } else {
            lastAnchor = new Anchor();
         }
      } // next line
      if (timers != null) timers.end("process lines");
      setLines(null); // allow lines to be garbage collected

      if (getMediaDurationSeconds() != null) {
         if (firstAnchor == lastAnchor) {
            // no utterances - create a new lastAnchor
            lastAnchor = graph.getOrCreateAnchorAt(
               getMediaDurationSeconds(), Constants.CONFIDENCE_MANUAL);
            graph.addAnchor(lastAnchor);
         } else {
            lastAnchor.setOffset(getMediaDurationSeconds());
            lastAnchor.setConfidence(Constants.CONFIDENCE_MANUAL);
         }
      }
      if (lastLine != null) {
         if (lastAnchor.getId() == null) {
            graph.addAnchor(lastAnchor);
         }
         lastLine.setEndId(lastAnchor.getId());
         graph.addAnnotation(lastLine);
      }
      if (turn != null) {
         turn.setEnd(lastAnchor);
         if (!turn.getStartId().equals(turn.getEndId())) {
            // don't create an instantaneous, empty turn
            graph.addAnnotation(turn);
         }
         if (!line.getStartId().equals(line.getEndId())) {
            // don't create an instantaneous, empty line
            graph.addAnnotation(line);
         }
      }

      // ensure we have an utterance tokenizer
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

      graph.trackChanges();
      
      if (getUseConventions()) {         
         if (timers != null) timers.start("apply conventions");
         try {
            // word {comment comment} word
            SpanningConventionTransformer commentTransformer = new SpanningConventionTransformer(
               getWordLayer().getId(), "\\{(.*)", "(.*)\\}", true, null, null, 
               commentLayer==null?null:commentLayer.getId(), "$1", "$1", false, false);
            commentTransformer.transform(graph);
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
         } catch(TransformationException exception) {
            if (errors == null) errors = new SerializationException();
            if (errors.getCause() == null) errors.initCause(exception);
            errors.addError(SerializationException.ErrorType.Tokenization, exception.getMessage());
         }
         if (timers != null) timers.end("apply conventions");
      } // apply transcription conventions

      OrthographyClumper clumper = new OrthographyClumper(wordLayer.getId(), utteranceLayer.getId());
      try {
         // clump non-orthographic 'words' with real words
         if (timers != null) timers.start("orthography clumping");
         clumper.transform(graph);
         graph.commit();
         if (timers != null) timers.end("orthography clumping");
      } catch(TransformationException exception) {
         if (errors == null) errors = new SerializationException();
         if (errors.getCause() == null) errors.initCause(exception);
         errors.addError(SerializationException.ErrorType.Tokenization, exception.getMessage());
      }

      if (timers != null) timers.start("finalization");

      if (graph.first(getParticipantLayer().getId()) == null) {
         // we haven't added a participant yet, so add the default one
         graph.addAnnotation(participant);
      }

      // now that we've got the whole text, we know who the participant(s) is for tagging
      participant = graph.first(getParticipantLayer().getId());
      for (Annotation tag : participantTags) {
         tag.setParentId(participant.getId());
         graph.addAnnotation(tag);
      } // next participant tag
      
      if (errors != null) throw errors;

      // set end anchors of graph tags
      for (Annotation a : graph.all(getParticipantLayer().getId())) {
         a.setStartId(firstAnchor.getId());
         a.setEndId(lastAnchor.getId());
      }
      if (getEpisodeLayer() != null) {
         for (Annotation a : graph.all(getEpisodeLayer().getId())) {
            a.setStartId(firstAnchor.getId());
            a.setEndId(lastAnchor.getId());
         }
      }

      graph.commit();
      
      if (timers != null) timers.end("finalization");

      if (graph.getOffsetUnits() == Constants.UNIT_CHARACTERS) {
         // hack to bypass validation during upload
         // this ensures that 'texts' (as opposed to 'transcripts') aren't put through validation
         // when saved to the graph store - this is because:
         //  1. they can be huge, so validation takes too long, and
         //  2. the main purpose of validation here is to set unset offsets,
         //     but these offsets are all set, so there's no need.
         graph.put("@valid", Boolean.TRUE);
      }

      // reset all change tracking
      graph.getTracker().reset();
      graph.setTracker(null);

      Graph[] graphs = { graph };
      return graphs;
   }

   /**
    * Returns any warnings that may have arisen during the last execution of 
    * {@link #deserialize()}.
    * @return A possibly empty list of warnings.
    */
   public String[] getWarnings() {
      return warnings.toArray(new String[0]);
   }

   /**
    * Validates the input and returns a list of errors that would
    * prevent the input from being converted into a {@link Graph}
    * when {@link #deserialize()} is called.
    * <p>This implementation checks for simultaneous speaker turns that have the same
    * speaker mentioned more than once, speakers that have the same name, and mismatched
    * start/end events. 
    * @return A list of errors, which will be empty if there were no validation errors.
    */
   public Vector<String> validate() {     
      warnings = new Vector<String>();
      if (mediaError != null) warnings.add(mediaError);
      mediaError = null;
      return warnings;
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
      if (getUseConventions()) {
         if (getNoiseLayer() != null) requiredLayers.add(getNoiseLayer().getId());
         if (getCommentLayer() != null) requiredLayers.add(getCommentLayer().getId());
         if (getLexicalLayer() != null) requiredLayers.add(getLexicalLayer().getId());
         if (getPronounceLayer() != null) requiredLayers.add(getPronounceLayer().getId());
      }
      return requiredLayers.toArray(new String[0]);
   } // getRequiredLayers()

   /**
    * Determines the cardinality between graphs and serialized streams.
    * <p>The cardinality of this deseerializer is NToN.
    * @return {@link nzilbb.ag.serialize.GraphSerializer#Cardinality}.NToN.
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
      // TODO maybe serialize a list of graph fragments into a single file?
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
      
      LinkedHashSet<String> selectedLayers = new LinkedHashSet<String>();
      LinkedHashSet<String> tagLayers = new LinkedHashSet<String>();
      if (layerIds != null) {
         for (String l : layerIds) {
            Layer layer = graph.getSchema().getLayer(l);
            if (layer != null) {
               selectedLayers.add(l);
               if (getWordLayer().getId().equals(layer.getParentId())
                   && (lexicalLayer == null || !layer.getId().equals(lexicalLayer.getId()))
                   && (pronounceLayer == null || !layer.getId().equals(pronounceLayer.getId()))) {
                  tagLayers.add(l); // TODO maybe allow other layers
               }
            }
         } // next layeyId
      } else {
         for (Layer l : graph.getSchema().getLayers().values()) selectedLayers.add(l.getId());
      }

      try {
         // write the text to a temporary file
         File f = File.createTempFile(graph.getId(), ".txt");
         PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), "utf-8"));

         Schema schema = graph.getSchema();
         
         // meta-data first
         MessageFormat fmtMetaDataFormat = new MessageFormat(metaDataFormat);
         boolean thereWereAttributes = false;
         for (String id : selectedLayers) {
            Layer layer = schema.getLayer(id);
            // is it a graph tag layer
            if (layer.getParent() != null
                && layer.getParent().equals(schema.getRoot())
                && layer.getAlignment() == Constants.ALIGNMENT_NONE
                && !layer.equals(getParticipantLayer())) { // it's a graph tag
               for (Annotation a : graph.all(id)) {
                  thereWereAttributes = true;
                  Object[] metadata = { id, a.getLabel() }; 
                  writer.println(fmtMetaDataFormat.format(metadata));
               } // next attribute               
            } // it's a graph tag
         } // next selected layer

         // get noises if needed
         TreeSet<Annotation> noisesByAnchor
            = new TreeSet<Annotation>(new AnnotationComparatorByAnchor());
         if (noiseLayer != null && getUseConventions()) {
            // list all anchored noises
            for (Annotation n : graph.all(noiseLayer.getId())) if (n.getAnchored()) noisesByAnchor.add(n);
         }
         Iterator<Annotation> noises = noisesByAnchor.iterator();
         Annotation nextNoise = noises.hasNext()?noises.next():null;

         // get comments if needed
         TreeSet<Annotation> commentsByAnchor
            = new TreeSet<Annotation>(new AnnotationComparatorByAnchor());
         if (commentLayer != null && getUseConventions()) {
            // list all anchored comments
            for (Annotation n : graph.all(commentLayer.getId())) if (n.getAnchored()) commentsByAnchor.add(n);
         }
         Iterator<Annotation> comments = commentsByAnchor.iterator();
         Annotation nextComment = comments.hasNext()?comments.next():null;

         // for each utterance...
         Annotation currentParticipant = null;
         MessageFormat fmtParticipant = new MessageFormat(participantFormat);
         
         // order utterances by anchor so that simultaneous speech comes out in utterance order
         TreeSet<Annotation> utterancesByAnchor
            = new TreeSet<Annotation>(new AnnotationComparatorByAnchor());
         for (Annotation u : graph.all(getUtteranceLayer().getId())) utterancesByAnchor.add(u);

         for (Annotation utterance : utterancesByAnchor) {
            if (cancelling) break;
            // is the participant changing?
            Annotation participant = utterance.first(getParticipantLayer().getId());
            if (participant != currentParticipant) { // participant change
               currentParticipant = participant;
               Object[] participantLabel = { currentParticipant.getLabel() }; 
               writer.println();
               writer.print(fmtParticipant.format(participantLabel));
            } // participant change

            for (Annotation token : utterance.all(getWordLayer().getId())) {
               writer.print(" ");
               
               if (getUseConventions()) {
                  // is there a noise to insert?
                  while (nextNoise != null
                         && token.getStart() != null
                         && token.getStart().getOffset() != null
                         && token.getStart().getOffset() >= nextNoise.getStart().getOffset()) {
                     writer.print("[" + nextNoise.getLabel() + "] ");
                     nextNoise = noises.hasNext()?noises.next():null;
                  } // next noise
                  
                  // is there a comment to insert?
                  while (nextComment != null
                         && token.getStart() != null
                         && token.getStart().getOffset() != null
                         && token.getStart().getOffset() >= nextComment.getStart().getOffset()) {
                     writer.print("{" + nextComment.getLabel() + "} ");
                     nextComment = comments.hasNext()?comments.next():null;
                  } // next comment
               } // useConventions
               
               Annotation orthography = token;
               if (orthographyLayer != null
                   && selectedLayers.contains(orthographyLayer.getId())) {
                  orthography = token.first(orthographyLayer.getId());
                  if (orthography == null) orthography = token;
               }
               writer.print(orthography.getLabel()); // TODO transcript convention support
               // add tags
               for (String layerId : tagLayers) {
                  if (!layerId.equals(token.getLayerId())
                      && (orthographyLayer == null || !layerId.equals(orthographyLayer.getId()))) {
                     writer.print("_");
                     Annotation tag = token.first(layerId);
                     if (tag != null) {
                        writer.print(tag.getLabel());
                     }
                  }
               } // next selected layer 

               if (getUseConventions())
               {
                  if (lexicalLayer != null) {
                     Annotation tag = token.first(lexicalLayer.getId());
                     if (tag != null) {
                        writer.print("(");
                        writer.print(tag.getLabel());
                        writer.print(")");
                     }
                  }
                  if (pronounceLayer != null) {
                     Annotation tag = token.first(pronounceLayer.getId());
                     if (tag != null) {
                        writer.print("[");
                        writer.print(tag.getLabel());
                        writer.print("]");
                     }
                  }
               } // useConventions
            } // next token
            
            // is there a noise to append to the end of the line?
            while (nextNoise != null
                   && utterance.getEnd() != null
                   && utterance.getEnd().getOffset() != null
                   && utterance.getEnd().getOffset() >= nextNoise.getStart().getOffset()) {
               writer.print(" [" + nextNoise.getLabel() + "]");
               nextNoise = noises.hasNext()?noises.next():null;
            } // next noise

            // is there a comment to append to the end of the line?
            while (nextComment != null
                   && utterance.getEnd() != null
                   && utterance.getEnd().getOffset() != null
                   && utterance.getEnd().getOffset() >= nextComment.getStart().getOffset()) {
               writer.print(" [" + nextComment.getLabel() + "]");
               nextComment = comments.hasNext()?comments.next():null;
            } // next comment
            
            writer.println();
         } // next utterance
         writer.close();

         TempFileInputStream in = new TempFileInputStream(f);
         
         // return a named stream from the file
         String streamName = graph.getId();
         if (!IO.Extension(streamName).equals("txt")) {
            streamName = IO.WithoutExtension(streamName) + ".txt";
         }
         return new NamedStream(in, IO.SafeFileNameUrl(streamName));
      } catch(Exception exception) {
         errors = new SerializationException();
         errors.initCause(exception);
         errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
         throw errors;
      }      
   }

} // end of class PlainTextSerialization
