//
// Copyright 2017 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.text;

import java.util.Vector;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileOutputStream;
import java.text.MessageFormat;
import java.text.ParseException;
import java.net.URL;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import nzilbb.ag.*;
import nzilbb.ag.util.OrthographyClumper;
import nzilbb.ag.util.SimpleTokenizer;
import nzilbb.ag.util.ConventionTransformer;
import nzilbb.ag.util.SpanningConventionTransformer;
import nzilbb.ag.serialize.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.util.Timers;

/**
 * Deserializer for plain text files.
 * <p>These can be written documents, or transcripts of speech. Interlocutors, if any, may be identified with a configurable line-start pattern.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class PlainTextDeserializer
   implements IDeserializer
{
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
   public void setName(String newName) { name = newName; }

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
   public void setHeaderLines(Vector<String> newHeaderLines) { headerLines = newHeaderLines; }

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
   public void setLines(Vector<String> newLines) { lines = newLines; }

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
   public void setHasSpeakers(boolean newHasSpeakers) { hasSpeakers = newHasSpeakers; }

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
   public void setHasTimestamps(boolean newHasTimestamps) { hasTimestamps = newHasTimestamps; }

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
   public void setLexicalLayer(Layer newLexicalLayer) { lexicalLayer = newLexicalLayer; }

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
   public void setPronounceLayer(Layer newPronounceLayer) { pronounceLayer = newPronounceLayer; }

   

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
   public void setCommentLayer(Layer newCommentLayer) { commentLayer = newCommentLayer; }


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
   public void setNoiseLayer(Layer newNoiseLayer) { noiseLayer = newNoiseLayer; }

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
       throws SerializationParametersMissingException
   {
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
   public Boolean getUseConventions() { return useConventions; }
   /**
    * Setter for {@link #useConventions}: Whether to use text conventions for comment, noise, lexical, and pronunciation annotations.
    * @param newUseConventions Whether to use text conventions for comment, noise, lexical, and pronunciation annotations.
    */
   public void setUseConventions(Boolean newUseConventions) { useConventions = newUseConventions; }

   /**
    * Delimiters used for comments, a string whose first characters is the open-comment marker, and the last character is the close-comment marker. The default is "{}"
    * @see #getCommentDelimiters()
    * @see #setCommentDelimiters(String)
    */
   protected String commentDelimiters;
   /**
    * Getter for {@link #commentDelimiters}: Delimiters used for comments, a string whose first characters is the open-comment marker, and the last character is the close-comment marker. The default is "{}"
    * @return Delimiters used for comments, a string whose first characters is the open-comment marker, and the last character is the close-comment marker. The default is "{}"
    */
   public String getCommentDelimiters() { return commentDelimiters; }
   /**
    * Setter for {@link #commentDelimiters}: Delimiters used for comments, a string whose first characters is the open-comment marker, and the last character is the close-comment marker. The default is "{}"
    * @param newCommentDelimiters Delimiters used for comments, a string whose first characters is the open-comment marker, and the last character is the close-comment marker. The default is "{}"
    */
   public void setCommentDelimiters(String newCommentDelimiters) { commentDelimiters = newCommentDelimiters; }

   /**
    * Delimiters used for noises, a string whose first characters is the open-comment marker, and the last character is the close-comment marker. The default is "[]"
    * @see #getNoiseDelimiters()
    * @see #setNoiseDelimiters(String)
    */
   protected String noiseDelimiters;
   /**
    * Getter for {@link #noiseDelimiters}: Delimiters used for noises, a string whose first characters is the open-comment marker, and the last character is the close-comment marker. The default is "[]"
    * @return Delimiters used for noises, a string whose first characters is the open-comment marker, and the last character is the close-comment marker. The default is "[]"
    */
   public String getNoiseDelimiters() { return noiseDelimiters; }
   /**
    * Setter for {@link #noiseDelimiters}: Delimiters used for noises, a string whose first characters is the open-comment marker, and the last character is the close-comment marker. The default is "[]"
    * @param newNoiseDelimiters Delimiters used for noises, a string whose first characters is the open-comment marker, and the last character is the close-comment marker. The default is "[]"
    */
   public void setNoiseDelimiters(String newNoiseDelimiters) { noiseDelimiters = newNoiseDelimiters; }

   /**
    * Format for marking a change of turn within the transcript body. Default pattern is "{0}: ", where <tt>{0}</tt> is a place-holder for the participant ID/name.
    * @see #getParticipantFormat()
    * @see #setParticipantFormat(String)
    */
   protected String participantFormat = "{0}: ";
   /**
    * Getter for {@link #participantFormat}: Format for marking a change of turn within the transcript body.
    * @return Format for marking a change of turn within the transcript body.
    */
   public String getParticipantFormat() { return participantFormat; }
   /**
    * Setter for {@link #participantFormat}: Format for marking a change of turn within the transcript body.
    * @param newParticipantFormat Format for marking a change of turn within the transcript body.
    */
   public void setParticipantFormat(String newParticipantFormat) { participantFormat = newParticipantFormat; }
   
   /**
    * Format for a meta-data line. Default pattern is "{0}={1}".  <tt>{0}</tt> is a place-holder for the attribute name or key, and <tt>{1}</tt> is a place-holder for the attribute value.
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
   public void setMetaDataFormat(String newMetaDataFormat) { metaDataFormat = newMetaDataFormat; }

   /**
    * The maximum length of a parsed participant ID/name.  Default is 20.
    * @see #getMaxParticipantLength()
    * @see #setMaxParticipantLength(Integer)
    */
   protected Integer maxParticipantLength;
   /**
    * Getter for {@link #maxParticipantLength}: The maximum length of a parsed participant ID/name.  Default is 20.
    * @return The maximum length of a parsed participant ID/name.  Default is 20.
    */
   public Integer getMaxParticipantLength() { return maxParticipantLength; }
   /**
    * Setter for {@link #maxParticipantLength}: The maximum length of a parsed participant ID/name.  
    * @param newMaxParticipantLength The maximum length of a parsed participant ID/name. 
    */
   public void setMaxParticipantLength(Integer newMaxParticipantLength) { maxParticipantLength = newMaxParticipantLength; }

   /**
    * Format for time synchronizations within the transcript body. {0} = hours, {1} = minutes, {2} = seconds, {3} = milliseconds
    * @see #getTimestampFormat()
    * @see #setTimestampFormat(String)
    */
   protected String timestampFormat;
   /**
    * Getter for {@link #timestampFormat}: Format for time synchronizations within the transcript body. {0} = hours, {1} = minutes, {2} = seconds, {3} = milliseconds
    * @return Format for time synchronizations within the transcript body. {0} = hours, {1} = minutes, {2} = seconds, {3} = milliseconds
    */
   public String getTimestampFormat() { return timestampFormat; }
   /**
    * Setter for {@link #timestampFormat}: Format for time synchronizations within the transcript body. {0} = hours, {1} = minutes, {2} = seconds, {3} = milliseconds
    * @param newTimestampFormat Format for time synchronizations within the transcript body. {0} = hours, {1} = minutes, {2} = seconds, {3} = milliseconds
    */
   public void setTimestampFormat(String newTimestampFormat) { timestampFormat = newTimestampFormat; }


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
   public void setMediaDurationSeconds(Double newMediaDurationSeconds) { mediaDurationSeconds = newMediaDurationSeconds; }

   /**
    * Utterance tokenizer.  The default is {@link SimpleTokenizer}.
    * @see #getTokenizer()
    * @see #setTokenizer(IGraphTransformer)
    */
   protected IGraphTransformer tokenizer;
   /**
    * Getter for {@link #tokenizer}: Utterance tokenizer.
    * @return Utterance tokenizer.
    */
   public IGraphTransformer getTokenizer() { return tokenizer; }
   /**
    * Setter for {@link #tokenizer}: Utterance tokenizer.
    * @param newTokenizer Utterance tokenizer.
    */
   public void setTokenizer(IGraphTransformer newTokenizer) { tokenizer = newTokenizer; }

   /**
    * Maximum lines in a header
    * @see #getMaxHeaderLines()
    * @see #setMaxHeaderLines(Integer)
    */
   protected Integer maxHeaderLines = new Integer(50);
   /**
    * Getter for {@link #maxHeaderLines}: Maximum lines in a header
    * @return Maximum lines in a header
    */
   public Integer getMaxHeaderLines() { return maxHeaderLines; }
   /**
    * Setter for {@link #maxHeaderLines}: Maximum lines in a header
    * @param newMaxHeaderLines Maximum lines in a header
    */
   public void setMaxHeaderLines(Integer newMaxHeaderLines) { maxHeaderLines = newMaxHeaderLines; }
   
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
   public void setMediaError(String newMediaError) { mediaError = newMediaError; }

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

   // Methods:
   
   /**
    * Default constructor.
    */
   public PlainTextDeserializer()
   {
   } // end of constructor
   
   // IStreamDeserializer methods:
   
   /**
    * Returns the deserializer's descriptor
    * @return The deserializer's descriptor
    */
   public SerializationDescriptor getDescriptor()
   {
      return new SerializationDescriptor(
	 "Plain Text Document", "0.11", "text/plain", ".txt", "20170228.1353", getClass().getResource("icon.png"));
   }

   /**
    * Sets parameters for deserializer as a whole.  This might include database connection parameters, locations of supporting files, etc.
    * <p>When the deserializer is installed, this method should be invoked with an empty parameter
    *  set, to discover what (if any) general configuration is required. If parameters are
    *  returned, and user interaction is possible, then the user may be presented with an
    *  interface for setting/confirming these parameters.  
    * @param configuration The configuration for the deserializer. 
    * @param schema The layer schema, definining layers and the way they interrelate.
    * @return A list of configuration parameters (still) must be set before {@link IDeserializer#setParameters(ParameterSet)} can be invoked. If this is an empty list, {@link IDeserializer#setParameters(ParameterSet)} can be invoked. If it's not an empty list, this method must be invoked again with the returned parameters' values set.
    */
   public ParameterSet configure(ParameterSet configuration, Schema schema)
   {
      setSchema(schema);
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
      if (getParticipantLayer() == null || getTurnLayer() == null 
	  || getUtteranceLayer() == null || getWordLayer() == null)
      {
	 for (Layer top : schema.getRoot().getChildren().values())
	 {
	    if (top.getAlignment() == Constants.ALIGNMENT_NONE)
	    {
	       if (top.getChildren().size() == 0)
	       { // unaligned childless children of graph
		  participantTagLayers.put(top.getId(), top);
	       }
	       else
	       { // unaligned children of graph, with children of their own
		  possibleParticipantLayers.put(top.getId(), top);
		  for (Layer turn : top.getChildren().values())
		  {
		     if (turn.getAlignment() == Constants.ALIGNMENT_INTERVAL
			 && turn.getChildren().size() > 0)
		     { // aligned children of who with their own children
			possibleTurnLayers.put(turn.getId(), turn);
			for (Layer turnChild : turn.getChildren().values())
			{
			   if (turnChild.getAlignment() == Constants.ALIGNMENT_INTERVAL)
			   { // aligned children of turn
			      possibleTurnChildLayers.put(turnChild.getId(), turnChild);
			      for (Layer tag : turnChild.getChildren().values())
			      {
				 if (tag.getAlignment() == Constants.ALIGNMENT_NONE)
				 { // unaligned children of word
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
      } // missing special layers
      else
      {
	 for (Layer turnChild : getTurnLayer().getChildren().values())
	 {
	    if (turnChild.getAlignment() == Constants.ALIGNMENT_INTERVAL)
	    {
	       possibleTurnChildLayers.put(turnChild.getId(), turnChild);
	    }
	 } // next possible word tag layer
	 for (Layer tag : getWordLayer().getChildren().values())
	 {
	    if (tag.getAlignment() == Constants.ALIGNMENT_NONE
		&& tag.getChildren().size() == 0)
	    {
	       wordTagLayers.put(tag.getId(), tag);
	    }
	 } // next possible word tag layer
	 for (Layer tag : getParticipantLayer().getChildren().values())
	 {
	    if (tag.getAlignment() == Constants.ALIGNMENT_NONE
		&& tag.getChildren().size() == 0)
	    {
	       participantTagLayers.put(tag.getId(), tag);
	    }
	 } // next possible word tag layer
      }
      participantTagLayers.remove("main_participant");
      if (getParticipantLayer() == null)
      {
	 layerToPossibilities.put(
	    new Parameter("participantLayer", Layer.class, "Participant layer", 
			  "Layer for speaker/participant identification", true), 
	    Arrays.asList("participant","participants","who","speaker","speakers"));
	 layerToCandidates.put("participantLayer", possibleParticipantLayers);
      }
      if (getTurnLayer() == null)
      {
	 layerToPossibilities.put(
	    new Parameter("turnLayer", Layer.class, "Turn layer", "Layer for speaker turns", true),
	    Arrays.asList("turn","turns"));
	 layerToCandidates.put("turnLayer", possibleTurnLayers);
      }
      if (getUtteranceLayer() == null)
      {
	 layerToPossibilities.put(
	    new Parameter("utteranceLayer", Layer.class, "Utterance layer", 
			  "Layer for speaker utterances", true), 
	    Arrays.asList("utterance","utterances","line","lines"));
	 layerToCandidates.put("utteranceLayer", possibleTurnChildLayers);
      }
      if (getWordLayer() == null)
      {
	 layerToPossibilities.put(
	    new Parameter("wordLayer", Layer.class, "Word layer", 
			  "Layer for individual word tokens", true), 
	    Arrays.asList("transcript","word","words","w"));
	 layerToCandidates.put("wordLayer", possibleTurnChildLayers);
      }

      LinkedHashMap<String,Layer> topLevelLayers = new LinkedHashMap<String,Layer>();
      for (Layer top : schema.getRoot().getChildren().values())
      {
	 if (top.getAlignment() == Constants.ALIGNMENT_INTERVAL)
	 { // aligned children of graph
	    topLevelLayers.put(top.getId(), top);
	 }
      } // next top level layer

      LinkedHashMap<String,Layer> graphTagLayers = new LinkedHashMap<String,Layer>();
      for (Layer top : schema.getRoot().getChildren().values())
      {
	 if (top.getAlignment() == Constants.ALIGNMENT_NONE
	     && top.getChildren().size() == 0)
	 { // unaligned childless children of graph
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

      // add parameters that aren't in the configuration yet, and set possibile/default values
      for (Parameter p : layerToPossibilities.keySet())
      {
	 List<String> possibleNames = layerToPossibilities.get(p);
	 LinkedHashMap<String,Layer> candidateLayers = layerToCandidates.get(p.getName());
	 if (configuration.containsKey(p.getName()))
	 {
	    p = configuration.get(p.getName());
	 }
	 else
	 {
	    configuration.addParameter(p);
	 }
	 if (p.getValue() == null)
	 {
	    p.setValue(Utility.FindLayerById(candidateLayers, possibleNames));
	 }
	 p.setPossibleValues(candidateLayers.values());
      }

      if (!configuration.containsKey("useConventions"))
      {
	 configuration.addParameter(
	    new Parameter("useConventions", Boolean.class, 
			  "Use Annotation Conventions",
			  "Whether to use text conventions for comment, noise, lexical, and pronounce annotations", true));
      }
      if (configuration.get("useConventions").getValue() == null)
      {
	 configuration.get("useConventions").setValue(Boolean.TRUE);
      }
      if (!configuration.containsKey("maxParticipantLength"))
      {
	 configuration.addParameter(
	    new Parameter("maxParticipantLength", Integer.class, 
			  "Max Participant Length",
			  "The maximum length of a participant name", true));
      }
      if (configuration.get("maxParticipantLength").getValue() == null)
      {
	 configuration.get("maxParticipantLength").setValue(new Integer(20));
      }
      
      if (!configuration.containsKey("maxHeaderLines"))
      {
	 configuration.addParameter(
	    new Parameter("maxHeaderLines", Integer.class, 
			  "Max Header Lines",
			  "The maximum number of lines in a meta-data header", true));
      }
      if (configuration.get("maxHeaderLines").getValue() == null)
      {
	 configuration.get("maxHeaderLines").setValue(new Integer(50));
      }
      
      if (!configuration.containsKey("participantFormat"))
      {
	 configuration.addParameter(
	    new Parameter("participantFormat", String.class, 
			  "Participant Format",
			  "Format for marking a change of turn within the transcript body - e.g. {0}:, where {0} is a place-holder for the participant ID/name", true));
      }
      if (configuration.get("participantFormat").getValue() == null)
      {
	 configuration.get("participantFormat").setValue(participantFormat);
      }
      
      if (!configuration.containsKey("metaDataFormat"))
      {
	 configuration.addParameter(
	    new Parameter("metaDataFormat", String.class, 
			  "Meta-data Format",
			  "Format for a meta-data line in the header - e.g. {0}={1}, where {0} is a place-holder for the attribute name or key, and {1} is a place-holder for the attribute value", true));
      }
      if (configuration.get("metaDataFormat").getValue() == null)
      {
	 configuration.get("metaDataFormat").setValue(metaDataFormat);
      }

      return configuration;
   }

   /**
    * Loads the serialized form of the graph, using the given set of named streams.
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
   public ParameterSet load(NamedStream[] streams, Schema schema) throws SerializationException, IOException
   {
      // take the first stream, ignore all others.
      NamedStream txt = Utility.FindSingleStream(streams, ".txt", "text/plain");
      if (txt == null) throw new SerializationException("No text document stream found");
      setName(txt.getName());
      
      setSchema(schema);

      // create a list of layers we need and possible matching layer names
      LinkedHashMap<Parameter,List<String>> layerToPossibilities = new LinkedHashMap<Parameter,List<String>>();
      HashMap<String,LinkedHashMap<String,Layer>> layerToCandidates = new HashMap<String,LinkedHashMap<String,Layer>>();	 
      
      LinkedHashMap<String,Layer> metadataLayers = new LinkedHashMap<String,Layer>();
      for (Layer layer : schema.getRoot().getChildren().values())
      {
	 if (layer.getAlignment() == Constants.ALIGNMENT_NONE)
	 {
	    metadataLayers.put(layer.getId(), layer);
	 }
      } // next turn child layer

      // look for person attributes
      for (Layer layer : schema.getParticipantLayer().getChildren().values())
      {
	 if (layer.getAlignment() == Constants.ALIGNMENT_NONE)
	 {
	    metadataLayers.put(layer.getId(), layer);
	 }
      } // next turn child layer

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
      MessageFormat fmtTimestampFormat = null;
      if (timestampFormat != null && timestampFormat.length() > 0) fmtTimestampFormat = new MessageFormat(timestampFormat);
      while (sLine != null)
      {
	 if (iLine == 0)
	 { // first line
	    // strip of the UTF-8 BOM if there is one.
	    if (sLine.startsWith("\uFEFF"))
	    {
	       sLine = sLine.replaceAll("\uFEFF","");
	    }
	 }
	 iLine++;

	 if (!getHasTimestamps() && fmtTimestampFormat != null)
	 {
	    // is this line a speaker utterance line?
	    try 
	    { 
	       fmtTimestampFormat.parse(sLine); 
	       // parsed, so there are timestamps
	       setHasTimestamps(true);

	       // if it's a timestamp, it's not a name, etc...
	       getLines().add(sLine);
	       sLine = reader.readLine();
	       continue;
	    }
	    catch(ParseException exception) {} // not parseable
	    catch(NullPointerException exception) {} // null ID
	 } // don't know whether hasTimestamps yet

	 // if we encounter a speaker utterance within the first 150 lines, 
	 // it contains speakers (after that, we assume it's a coincidence)
	 if (iLine <= 150 && !getHasSpeakers())
	 {
	    // is this line a speaker utterance line?
	    try
	    {
	       String id = (String)fmtSpeakerFormat.parse(sLine)[0];
	       if (getMaxParticipantLength() == null
		   || id.length() <= getMaxParticipantLength())
	       {
		  setHasSpeakers(true);
		  if (iLine > 1)
		  {
		     // this is the first speaker we've seen, so everything before this is header
		     setHeaderLines(getLines());
		     // and the transcript content starts here
		     setLines(new Vector<String>());
		  }
	       }
	    }
	    catch(ParseException exception) {} // not parseable
	    catch(NullPointerException exception) {} // null ID
	 } // early enough and we don't know whether has speakers or not yet

	 getLines().add(sLine);
	 sLine = reader.readLine();
      } // next line
      reader.close();      

      // media duration
      NamedStream wav = Utility.FindSingleStream(streams, ".wav", "audio/wav");
      if (wav != null)
      {
	 // save the media file
	 File fMedia = File.createTempFile("PlainTextDeserializer-", wav.getName());
	 fMedia.deleteOnExit();
	 try
	 {
	    // we cannot just use the stream directly, because AudioSystem.getAudioInputStream()
	    // requires a mark/reset-able stream, which we can't guarantee that we have
	    // so we save the stream to a file, and give AudioSystem.getAudioInputStream() that file
	    FileOutputStream outStream = new FileOutputStream(fMedia);
	    byte[] buffer = new byte[1024];
	    int bytesRead = wav.getStream().read(buffer);
	    while(bytesRead >= 0)
	    {
	       outStream.write(buffer, 0, bytesRead);
	       bytesRead = wav.getStream().read(buffer);
	    } // next chunk of data
	    wav.getStream().close();
	    outStream.close();

	    // determine the duration of the media file
	    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(fMedia);
	    AudioFormat format = audioInputStream.getFormat();
	    long frames = audioInputStream.getFrameLength();
	    if (frames > 0)
	    {
	       double durationInSeconds = ((double)frames) / format.getFrameRate(); 
	       setMediaDurationSeconds(durationInSeconds);
	    }
	    else
	    {
	       setMediaError("Ignoring media: " + wav.getName() + " is valid but contains no frames.");
	    }
	 }
	 catch(Exception exception)
	 {
	    setMediaError("Ignoring media: " + wav.getName() + " ERROR: " + exception);
	 }	 
	 finally
	 {
	    fMedia.delete();
	 }
      } // there is a WAV file

      // if there are headers, we need to map them to layers
      MessageFormat fmtMetaDataFormat = new MessageFormat(metaDataFormat);
      for (String header : getHeaderLines())
      {
	 if (header.trim().length() == 0) continue;
	 try
	 {
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
	 }
	 catch(ParseException exception) {} // not parseable
      } // next header
	 
      ParameterSet parameters = new ParameterSet();
      // add parameters that aren't in the configuration yet, and set possibile/default values
      for (Parameter p : layerToPossibilities.keySet())
      {
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
    * @throws SerializationParametersMissingException if the parameters for this particular graph have not been set.
    * @throws SerializationException if errors occur during deserialization.
    */
   public Graph[] deserialize() 
      throws SerializerNotConfiguredException, SerializationParametersMissingException, SerializationException
   {
      if (timers != null) timers.start("initialization");
      
      if (participantLayer == null) throw new SerializerNotConfiguredException("Participant layer not set");
      if (turnLayer == null) throw new SerializerNotConfiguredException("Turn layer not set");
      if (utteranceLayer == null) throw new SerializerNotConfiguredException("Utterance layer not set");
      if (wordLayer == null) throw new SerializerNotConfiguredException("Word layer not set");
      if (schema == null) throw new SerializerNotConfiguredException("Layer schema not set");

      validate();

      // if there are errors, accumlate as many as we can before throwing SerializationException
      SerializationException errors = null;

      Graph graph = new Graph();
      graph.setTimers(timers);
      graph.setId(getName());
      double dOffsetFactor = 1.0;
      if (!getHasTimestamps() && getMediaDurationSeconds() == null)
      {
	 graph.setOffsetUnits(Constants.UNIT_CHARACTERS);
      }
      else
      {
	 graph.setOffsetUnits(Constants.UNIT_SECONDS);
	 if (!getHasTimestamps())
	 { // no timestamps
	    // use the length of the media file to scale the offsets
	    if (getMediaDurationSeconds() != null)
	    {
	       // get the length in characters
	       long charCount = 0;
	       for (String sLine : getLines())
	       {
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
      if (parameters != null)
      {
	 for (Parameter p : parameters.values())
	 {
	    Layer layer = (Layer)p.getValue();
	    if (layer != null && graph.getLayer(layer.getId()) == null)
	    { // haven't added this layer yet
	       graph.addLayer((Layer)layer.clone());
	    }
	 }
      }

      HashMap<String,Annotation> participants = new HashMap<String,Annotation>();
      Annotation participant = new Annotation(null, "author", schema.getParticipantLayerId());
      Vector<Annotation> participantTags = new Vector<Annotation>();
      MessageFormat fmtMetaDataFormat = new MessageFormat(metaDataFormat);
      for (String header : getHeaderLines())
      {
	 if (header.trim().length() == 0) continue;
	 try
	 { // parse meta data header
	    Object[] oMetaData = fmtMetaDataFormat.parse(header);
	    String key = (String)oMetaData[0];
	    Layer layer = (Layer)parameters.get("header_" + key).getValue();
	    if (layer != null)
	    {
	       String value = (String)oMetaData[1];
	       if (layer.getParentId().equals(schema.getRoot().getId())) // graph tag
	       {
		  graph.createTag(graph, layer.getId(), value);
	       }
	       else // participant tag
	       {
		  participantTags.add(new Annotation(null, value, layer.getId()));
	       }
	    }
	 } // parse meta data header
	 catch(ParseException exception) // not parseable
	 { // add the header line as a comment
	    if (getCommentLayer() != null)
	    {
	       int iNumChars = header.length();
	       Annotation anComment = new Annotation(
		  null, header, getCommentLayer().getId(),
		  graph.getOrCreateAnchorAt(0.0, Constants.CONFIDENCE_MANUAL).getId(),
		  graph.getOrCreateAnchorAt(0.0, Constants.CONFIDENCE_MANUAL).getId());
	       graph.addAnnotation(anComment);
	    } // there is a comment layer
	 } // comment header
      } // next header line
      setHeaderLines(null); // allow lines to be garbage collected
	 
      if (timers != null) timers.end("initialization");
      
      // graph
      Annotation turn = new Annotation(
	 null, participant.getLabel(), getTurnLayer().getId());
      graph.addAnnotation(turn);
      turn.setParent(participant);
      turn.setStart(
	 graph.getOrCreateAnchorAt(0.0, Constants.CONFIDENCE_MANUAL));
      Annotation line = new Annotation(null, turn.getLabel(), getUtteranceLayer().getId());
      line.setParentId(turn.getId());
      line.setStart(turn.getStart());
      int iLastPosition = 0;	 

      MessageFormat fmtSpeakerFormat = new MessageFormat(participantFormat);
      MessageFormat fmtTimestampFormat = null;
      if (timestampFormat != null && timestampFormat.length() > 0) fmtTimestampFormat = new MessageFormat(timestampFormat);

      if (timers != null) timers.start("process lines");

      Annotation lastLine = null;
      int lineOrdinal = 1;
      for (String sLine : getLines())
      {
	 sLine = sLine.trim();
	 // if (sLine.length() == 0 && getHasSpeakers())
	 // {
	 //    // a blank line in a transcript is taken to be a break in speaker
	 //    // subsequent lines with text but no speaker are taken to be notes/comments
	 //    participant = null;
	 //    continue;
	 // }
	 
	 int iNumChars = sLine.length();
	 line = new Annotation(null, turn.getLabel(), getUtteranceLayer().getId());
	 line.setParentId(turn.getId());
	 line.setStart(turn.getStart());
	 line.setOrdinal(lineOrdinal++);
	 
	 if (graph.getOffsetUnits() != Constants.UNIT_CHARACTERS)
	 {
	    // does the list start with a time stamp?
	    if (getHasTimestamps())
	    {
	       if (timers != null) timers.start("check for timestamp");
	       try
	       {
		  double dSeconds = 0.0;
		  Object[] timestamp = fmtTimestampFormat.parse(sLine);
		  try {dSeconds += Integer.parseInt(timestamp[0].toString()) * 3600;} // hours
		  catch(Throwable exception) {}
		  try {dSeconds += Integer.parseInt(timestamp[1].toString()) * 60;} // minutes
		  catch(Throwable exception) {}
		  try {dSeconds += Integer.parseInt(timestamp[2].toString());} // seconds
		  catch(Throwable exception) {}
		  try {dSeconds += Double.parseDouble(timestamp[3].toString()) / 1000;} // ms
		  catch(Throwable exception) {}
		  lastAnchor.setOffset(dSeconds);
		  lastAnchor.setConfidence(Constants.CONFIDENCE_MANUAL);
		  
		  // consume the timestamp
		  sLine = sLine.substring(fmtTimestampFormat.format(timestamp).length());
	       } // timestamp found
	       catch(ParseException exception) {} // not parseable
	       if (timers != null) timers.end("check for timestamp");
	    } // HasTimestamps
	 }
	 line.setStart(lastAnchor);
	 if (lastLine != null)
	 {
	    if (lastAnchor.getId() == null)
	    {
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
	    try
	    {
	       if (timers != null) timers.start("check for speaker change");
	       Object[] oSpeaker = fmtSpeakerFormat.parse(sLine);
	       if (timers != null) timers.end("check for speaker change");
	       String sSpeakerId = (String)oSpeaker[0];
	       if (getMaxParticipantLength() == null
		   || sSpeakerId.length() <= getMaxParticipantLength())
	       {
		  // speaker
		  participant = participants.get(sSpeakerId);
		  if (participant == null)
		  {
		     participant = new Annotation(null, sSpeakerId, schema.getParticipantLayerId());
		     graph.addAnnotation(participant);
		     participants.put(sSpeakerId, participant);
		  }
		  
		  if (lastAnchor.getOffset().equals(0.0))
		  { // just started, so recycle the turn and utterance we're already in
		     turn.setLabel(participant.getLabel());
		     turn.setParentId(participant.getId());
		     line.setLabel(participant.getLabel());
		     line.setParentId(turn.getId());
		  }
		  else
		  {
		     // finish old turn
		     if (turn != null && turn.getStart() != lastAnchor)
		     {
			turn.setEnd(lastAnchor);
			if (timers != null) timers.start("add old turn annotation");
			graph.addAnnotation(turn);
			if (timers != null) timers.end("add old turn annotation");
		     }

		     lineOrdinal = 1;
		     
		     // new turn		  
		     turn = new Annotation(null, participant.getLabel(), getTurnLayer().getId());
		     turn.setParentId(participant.getId());
		     if (timers != null) timers.start("add new turn annotation");
		     graph.addAnnotation(turn);
		     if (timers != null) timers.end("add new turn annotation");
		     turn.setStart(lastAnchor);
		     // start a new line too
		     line.setEnd(lastAnchor);
		     line.setOrdinal(lineOrdinal++);
		     if (!line.getStartId().equals(line.getEndId()))
		     { // if we have <div><p>... don't create an instantaneous, empty line
			graph.addAnnotation(line);
		     }
		     line = new Annotation(null, turn.getLabel(), getUtteranceLayer().getId());
		     line.setParentId(turn.getId());
		     line.setStart(lastAnchor);

		  }
		  // consume the speaker ID
		  sLine = sLine.substring(fmtSpeakerFormat.format(oSpeaker).length());
	       } // speaker found
	    }
	    catch(ParseException exception) {} // not parseable
	    catch(NullPointerException exception) {} // null ID
	 } // HasSpeakers
	 
	 // process text
	 if (sLine.length() > 0)
	 {
	    if (participant != null)
	    { // speech or text

	       // temporary set label to transcription
	       line.setLabel(sLine
			     // ensure ellipsis (sometimes appearing in MS Word transcripts)
			     // ends up as a token boundary
			     .replaceAll("…", "… "));
	       lastLine = line;
	    } // speech/text
	    else
	    { // no speaker, so taken as comments
	       Annotation anComment = new Annotation(null, sLine, getCommentLayer().getId());
	       anComment.setStart(lastAnchor);	       
	       lastLine = anComment;
	    } // comments
	 } // there is text on this line
	 
	 // update current position
	 if (!getHasTimestamps())
	 {
	    if (timers != null) timers.start("getOrCreateAnchorAt");
	    lastAnchor = graph.getOrCreateAnchorAt(
	       lastAnchor.getOffset() + (((double)iNumChars + 1) * dOffsetFactor), iWordAlignmentConfidence);
	    if (timers != null) timers.start("getOrCreateAnchorAt");
	 }
	 else
	 {
	    lastAnchor = new Anchor();
	 }
      } // next line
      if (timers != null) timers.end("process lines");
      setLines(null); // allow lines to be garbage collected

      if (getMediaDurationSeconds() != null)
      {
	 lastAnchor.setOffset(getMediaDurationSeconds());
	 lastAnchor.setConfidence(Constants.CONFIDENCE_MANUAL);
      }
      if (lastLine != null)
      {
	 if (lastAnchor.getId() == null)
	 {
	    graph.addAnchor(lastAnchor);
	 }
	 lastLine.setEndId(lastAnchor.getId());
	 graph.addAnnotation(lastLine);
      }
      if (turn != null)
      {
	 turn.setEnd(lastAnchor);
	 if (!turn.getStartId().equals(turn.getEndId()))
	 { // don't create an instantaneous, empty turn
	    graph.addAnnotation(turn);
	 }
	 if (!line.getStartId().equals(line.getEndId()))
	 { // don't create an instantaneous, empty line
	    graph.addAnnotation(line);
	 }
      }

      // ensure we have an utterance tokenizer
      if (getTokenizer() == null)
      {
	 setTokenizer(new SimpleTokenizer(getUtteranceLayer().getId(), getWordLayer().getId()));
      }
      try
      {
	 if (timers != null) timers.start("tokenization");
	 tokenizer.transform(graph);
	 if (timers != null) timers.end("tokenization");
      }
      catch(TransformationException exception)
      {
	 if (errors == null) errors = new SerializationException();
	 if (errors.getCause() == null) errors.initCause(exception);
	 errors.addError(SerializationException.ErrorType.Tokenization, exception.getMessage());
      }
      if (timers != null) timers.start("commit tokenization");
      graph.commit();
      if (timers != null) timers.end("commit tokenization");

      if (getUseConventions())
      {
	 if (timers != null) timers.start("apply conventions");
	 try
	 {
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
	       getWordLayer().getId(), "(.*)\\[(.*)\\]", "$1", 
	       pronounceLayer==null?null:pronounceLayer.getId(), "$2");
	    pronounceTransformer.transform(graph);
	    graph.commit();
	    
	    // word(lexical)
	    ConventionTransformer lexicalTransformer = new ConventionTransformer(
	       getWordLayer().getId(), "(.*)\\((.*)\\)", "$1", 
	       lexicalLayer==null?null:lexicalLayer.getId(), "$2");
	    lexicalTransformer.transform(graph);
	    graph.commit();
	 }
	 catch(TransformationException exception)
	 {
	    if (errors == null) errors = new SerializationException();
	    if (errors.getCause() == null) errors.initCause(exception);
	    errors.addError(SerializationException.ErrorType.Tokenization, exception.getMessage());
	 }
	 if (timers != null) timers.end("apply conventions");
      } // apply transcription conventions

      OrthographyClumper clumper = new OrthographyClumper(wordLayer.getId(), utteranceLayer.getId());
      try
      {
	 // clump non-orthographic 'words' with real words
	 if (timers != null) timers.start("orthography clumping");
	 clumper.transform(graph);
	 graph.commit();
	 if (timers != null) timers.end("orthography clumping");
      }
      catch(TransformationException exception)
      {
	 if (errors == null) errors = new SerializationException();
	 if (errors.getCause() == null) errors.initCause(exception);
	 errors.addError(SerializationException.ErrorType.Tokenization, exception.getMessage());
      }

      if (timers != null) timers.start("finalization");

      // now that we've got the whole text, we know who the participant(s) is for tagging
      participant = graph.my(getParticipantLayer().getId());
      for (Annotation tag : participantTags)
      {
	 tag.setParentId(participant.getId());
	 graph.addAnnotation(tag);
      } // next participant tag
      
      if (errors != null) throw errors;

      // set end anchors of graph tags
      for (Annotation a : graph.list(getParticipantLayer().getId()))
      {
	 a.setStartId(firstAnchor.getId());
	 a.setEndId(lastAnchor.getId());
      }

      graph.commit();
      
      if (timers != null) timers.end("finalization");

      if (graph.getOffsetUnits() == Constants.UNIT_CHARACTERS)
      {
	 // hack to bypass validation during upload
	 // this ensures that 'texts' (as opposed to 'transcripts') aren't put through validation
	 // when saved to the graph store - this is because:
	 //  1. they can be huge, so validation takes too long, and
	 //  2. the main purpose of validation here is to set unset offsets,
	 //     but these offsets are all set, so there's no need.
	 graph.put("@valid", Boolean.TRUE);
      }

      Graph[] graphs = { graph };
      return graphs;
   }

   /**
    * Returns any warnings that may have arisen during the last execution of {@link #deserialize()}.
    * @return A possibly empty list of warnings.
    */
   public String[] getWarnings()
   {
      return warnings.toArray(new String[0]);
   }

   /**
    * Validates the input and returns a list of errors that would
    * prevent the input from being converted into a {@link Graph}
    * when {@link #deserialize()} is called.
    * <p>This implementation checks for simultaneous speaker turns that have the same speaker mentioned more than once, speakers that have the same name, and mismatched start/end events.
    * @return A list of errors, which will be empty if there were no validation errors.
    */
   public Vector<String> validate()
   {     
      warnings = new Vector<String>();
      if (mediaError != null) warnings.add(mediaError);
      mediaError = null;
      return warnings;
   }

} // end of class PlainTextDeserializer
