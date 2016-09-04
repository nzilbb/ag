//
// Copyright 2016 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.clan;

import java.util.Vector;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import nzilbb.ag.*;
import nzilbb.ag.util.SimpleTokenizer;
import nzilbb.ag.util.ConventionTransformer;
import nzilbb.ag.util.SpanningConventionTransformer;
import nzilbb.ag.serialize.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;

/**
 * Deserializer for CHAT files produced by CLAN.
 * <p><em>NB</em> the current implementation is <em>not exhaustive</em>; it only covers:
 * <ul>
 *  <li>Time synchronization codes, including mid-line synchronization.
 *    <br>Overlapping utterances in the same speaker turn are handled as follows:
 *    <ul>
 *      <li>If overlap is partial, the start of the second utterance is set to the end of the first</li>
 *      <li>If overlap is total, the two utterances are chained together with a non-aligned anchor between them.</li>
 *    </ul>
 *  </li>
 *  <li>Disfluency marking with &amp; - e.g. <tt>so &amp;sund Sunday</tt></li>
 *  <li>Non-standard form expansion - e.g. <tt>gonna [: going to]</tt></li>
 *  <li>Incomplete word completion - e.g. <tt>dinner doin(g) all</tt></li>
 *  <li>Acronym/proper name joining with _ - e.g. <tt>no T_V in my room</tt></li>
 *  <li>Retracing - e.g. <tt>&lt;some friends and I&gt; [//] uh</tt> or <tt>and sit [//] sets him</tt></li>
 *  <li>Repetition/stuttered false starts - e.g. <tt>the &lt;picnic&gt; [/] picnic</tt> or <tt>the Saturday [/] in the morning</tt></li>
 *  <li>Errors - e.g. <tt>they've &lt;work up a hunger&gt; [* s:r]</tt> or <tt>they got [* m] to</tt></li>
 * </ul>
 * @author Robert Fromont robert@fromont.net.nz
 */

public class ChatDeserializer
  implements IDeserializer
{
   // Attributes:
   protected Vector<String> warnings;

   /**
    * Name of the .cha file.
    * @see #getName()
    * @see #setName(String)
    */
   protected String name;
   /**
    * Getter for {@link #name}: Name of the .cha file.
    * @return Name of the .cha file.
    */
   public String getName() { return name; }
   /**
    * Setter for {@link #name}: Name of the .cha file.
    * @param newName Name of the .cha file.
    */
   public void setName(String newName) { name = newName; }
   
   /**
    * List of languages.
    * @see #getLanguages()
    * @see #setLanguages(Vector)
    */
   protected Vector<String> languages;
   /**
    * Getter for {@link #languages}: List of languages.
    * @return List of languages.
    */
   public Vector<String> getLanguages() { return languages; }
   /**
    * Setter for {@link #languages}: List of languages.
    * @param newLanguages List of languages.
    */
   public void setLanguages(Vector<String> newLanguages) { languages = newLanguages; }
   
   /**
    * Map of participant IDs to properties.
    * @see #getParticipants()
    * @see #setParticipants(HashMap)
    */
   protected HashMap<String,HashMap<String,String>> participants;
   /**
    * Getter for {@link #participants}: Map of participant IDs to properties.
    * @return Map of participant IDs to properties.
    */
   public HashMap<String,HashMap<String,String>> getParticipants() { return participants; }
   /**
    * Setter for {@link #participants}: Map of participant IDs to properties.
    * @param newParticipants Map of participant IDs to properties.
    */
   public void setParticipants(HashMap<String,HashMap<String,String>> newParticipants) { participants = newParticipants; }
   
   /**
    * Transcript lines.
    * @see #getLines()
    * @see #setLines(Vector)
    */
   protected Vector<String> lines;
   /**
    * Getter for {@link #lines}: Transcript lines.
    * @return Transcript lines.
    */
   public Vector<String> getLines() { return lines; }
   /**
    * Setter for {@link #lines}: Transcript lines.
    * @param newLines Transcript lines.
    */
   public void setLines(Vector<String> newLines) { lines = newLines; }
   
   /**
    * Header lines
    * @see #getHeaders()
    * @see #setHeaders(Vector)
    */
   protected Vector<String> headers;
   /**
    * Getter for {@link #headers}: Header lines
    * @return Header lines
    */
   public Vector<String> getHeaders() { return headers; }
   /**
    * Setter for {@link #headers}: Header lines
    * @param newHeaders Header lines
    */
   public void setHeaders(Vector<String> newHeaders) { headers = newHeaders; }
   
   /**
    * Name of media file.
    * @see #getMediaName()
    * @see #setMediaName(String)
    */
   protected String mediaName;
   /**
    * Getter for {@link #mediaName}: Name of media file.
    * @return Name of media file.
    */
   public String getMediaName() { return mediaName; }
   /**
    * Setter for {@link #mediaName}: Name of media file.
    * @param newMediaName Name of media file.
    */
   public void setMediaName(String newMediaName) { mediaName = newMediaName; }

   /**
    * Type of media.
    * @see #getMediaType()
    * @see #setMediaType(String)
    */
   protected String mediaType;
   /**
    * Getter for {@link #mediaType}: Type of media.
    * @return Type of media.
    */
   public String getMediaType() { return mediaType; }
   /**
    * Setter for {@link #mediaType}: Type of media.
    * @param newMediaType Type of media.
    */
   public void setMediaType(String newMediaType) { mediaType = newMediaType; }

   /**
    * List of names of transcribers.
    * @see #getTranscribers()
    * @see #setTranscribers(Vector)
    */
   protected Vector<String> transcribers;
   /**
    * Getter for {@link #transcribers}: List of names of transcribers.
    * @return List of names of transcribers.
    */
   public Vector<String> getTranscribers() { return transcribers; }
   /**
    * Setter for {@link #transcribers}: List of names of transcribers.
    * @param newTranscribers List of names of transcribers.
    */
   public void setTranscribers(Vector<String> newTranscribers) { transcribers = newTranscribers; }

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
    * Participant layer.
    * @see #getParticipantLayer()
    * @see #setParticipantLayer(Layer)
    */
   protected Layer participantLayer;
   /**
    * Getter for {@link #participantLayer}: Participant layer.
    * @return Participant layer.
    */
   public Layer getParticipantLayer() { return participantLayer; }
   /**
    * Setter for {@link #participantLayer}: Participant layer.
    * @param newParticipantLayer Participant layer.
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
    * Word layer.
    * @see #getWordLayer()
    * @see #setWordLayer(Layer)
    */
   protected Layer wordLayer;
   /**
    * Getter for {@link #wordLayer}: Word layer.
    * @return Word layer.
    */
   public Layer getWordLayer() { return wordLayer; }
   /**
    * Setter for {@link #wordLayer}: Word layer.
    * @param newWordLayer Word layer.
    */
   public void setWordLayer(Layer newWordLayer) { wordLayer = newWordLayer; }

   /**
    * Disfluency layer.
    * @see #getDisfluencyLayer()
    * @see #setDisfluencyLayer(Layer)
    */
   protected Layer disfluencyLayer;
   /**
    * Getter for {@link #disfluencyLayer}: Disfluency layer.
    * @return Disfluency layer.
    */
   public Layer getDisfluencyLayer() { return disfluencyLayer; }
   /**
    * Setter for {@link #disfluencyLayer}: Disfluency layer.
    * @param newDisfluencyLayer Disfluency layer.
    */
   public void setDisfluencyLayer(Layer newDisfluencyLayer) { disfluencyLayer = newDisfluencyLayer; }

   /**
    * Expansion layer.
    * @see #getExpansionLayer()
    * @see #setExpansionLayer(Layer)
    */
   protected Layer expansionLayer;
   /**
    * Getter for {@link #expansionLayer}: Expansion layer.
    * @return Expansion layer.
    */
   public Layer getExpansionLayer() { return expansionLayer; }
   /**
    * Setter for {@link #expansionLayer}: Expansion layer.
    * @param newExpansionLayer Expansion layer.
    */
   public void setExpansionLayer(Layer newExpansionLayer) { expansionLayer = newExpansionLayer; }

   /**
    * Errors layer.
    * @see #getErrorsLayer()
    * @see #setErrorsLayer(Layer)
    */
   protected Layer errorsLayer;
   /**
    * Getter for {@link #errorsLayer}: Errors layer.
    * @return Errors layer.
    */
   public Layer getErrorsLayer() { return errorsLayer; }
   /**
    * Setter for {@link #errorsLayer}: Errors layer.
    * @param newErrorsLayer Errors layer.
    */
   public void setErrorsLayer(Layer newErrorsLayer) { errorsLayer = newErrorsLayer; }

   /**
    * Retracing layer.
    * @see #getRetracingLayer()
    * @see #setErrorsLayer(Layer)
    */
   protected Layer retracingLayer;
   /**
    * Getter for {@link #retracingLayer}: Retracing layer.
    * @return Retracing layer.
    */
   public Layer getRetracingLayer() { return retracingLayer; }
   /**
    * Setter for {@link #retracingLayer}: Retracing layer.
    * @param newRetracingLayer Retracing layer.
    */
   public void setRetracingLayer(Layer newRetracingLayer) { retracingLayer = newRetracingLayer; }

   /**
    * Repetitions layer.
    * @see #getRepetitionsLayer()
    * @see #setRepetitionsLayer(Layer)
    */
   protected Layer repetitionsLayer;
   /**
    * Getter for {@link #repetitionsLayer}: Repetitions layer.
    * @return Repetitions layer.
    */
   public Layer getRepetitionsLayer() { return repetitionsLayer; }
   /**
    * Setter for {@link #repetitionsLayer}: Repetitions layer.
    * @param newRepetitionsLayer Repetitions layer.
    */
   public void setRepetitionsLayer(Layer newRepetitionsLayer) { repetitionsLayer = newRepetitionsLayer; }


   /**
    * Completion layer.
    * @see #getCompletionLayer()
    * @see #setCompletionLayer(Layer)
    */
   protected Layer completionLayer;
   /**
    * Getter for {@link #completionLayer}: Completion layer.
    * @return Completion layer.
    */
   public Layer getCompletionLayer() { return completionLayer; }
   /**
    * Setter for {@link #completionLayer}: Completion layer.
    * @param newCompletionLayer Completion layer.
    */
   public void setCompletionLayer(Layer newCompletionLayer) { completionLayer = newCompletionLayer; }

   /**
    * Linkage layer.
    * @see #getLinkageLayer()
    * @see #setLinkageLayer(Layer)
    */
   protected Layer linkageLayer;
   /**
    * Getter for {@link #linkageLayer}: Linkage layer.
    * @return Linkage layer.
    */
   public Layer getLinkageLayer() { return linkageLayer; }
   /**
    * Setter for {@link #linkageLayer}: Linkage layer.
    * @param newLinkageLayer Linkage layer.
    */
   public void setLinkageLayer(Layer newLinkageLayer) { linkageLayer = newLinkageLayer; }

   /**
    * C-Unit layer.
    * @see #getCUnitLayer()
    * @see #setCUnitLayer(Layer)
    */
   protected Layer cUnitLayer;
   /**
    * Getter for {@link #cUnitLayer}: C-Unit layer.
    * @return C-Unit layer.
    */
   public Layer getCUnitLayer() { return cUnitLayer; }
   /**
    * Setter for {@link #cUnitLayer}: C-Unit layer.
    * @param newCUnitLayer C-Unit layer.
    */
   public void setCUnitLayer(Layer newCUnitLayer) { cUnitLayer = newCUnitLayer; }
   
   /**
    * Gems
    * @see #getGemLayer()
    * @see #setGemLayer(Layer)
    */
   protected Layer gemLayer;
   /**
    * Getter for {@link #gemLayer}: Gems
    * @return Gems
    */
   public Layer getGemLayer() { return gemLayer; }
   /**
    * Setter for {@link #gemLayer}: Gems
    * @param newGemLayer Gems
    */
   public void setGemLayer(Layer newGemLayer) { gemLayer = newGemLayer; }
   
   /**
    * Transcriber graph attributes.
    * @see #getTranscriberLayer()
    * @see #setTranscriberLayer(Layer)
    */
   protected Layer transcriberLayer;
   /**
    * Getter for {@link #transcriberLayer}: Transcriber graph attributes.
    * @return Transcriber graph attributes.
    */
   public Layer getTranscriberLayer() { return transcriberLayer; }
   /**
    * Setter for {@link #transcriberLayer}: Transcriber graph attributes.
    * @param newTranscriberLayer Transcriber graph attributes.
    */
   public void setTranscriberLayer(Layer newTranscriberLayer) { transcriberLayer = newTranscriberLayer; }

   /**
    * Graph language.
    * @see #getLanguagesLayer()
    * @see #setLanguagesLayer(Layer)
    */
   protected Layer languagesLayer;
   /**
    * Getter for {@link #languagesLayer}: Graph language.
    * @return Graph language.
    */
   public Layer getLanguagesLayer() { return languagesLayer; }
   /**
    * Setter for {@link #languagesLayer}: Graph language.
    * @param newLanguagesLayer Graph language.
    */
   public void setLanguagesLayer(Layer newLanguagesLayer) { languagesLayer = newLanguagesLayer; }

   /**
    * Required participant meta-data layers.
    * @see #getParticipantLayers()
    * @see #setParticipantLayers(HashMap)
    */
   protected HashMap<String,Layer> participantLayers;
   /**
    * Getter for {@link #participantLayers}: Required participant meta-data layers.
    * @return Required participant meta-data layers.
    */
   public HashMap<String,Layer> getParticipantLayers() { return participantLayers; }
   /**
    * Setter for {@link #participantLayers}: Required participant meta-data layers.
    * @param newParticipantLayers Required participant meta-data layers.
    */
   public void setParticipantLayers(HashMap<String,Layer> newParticipantLayers) { participantLayers = newParticipantLayers; }
   
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
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public ChatDeserializer()
   {
      participantLayers = new HashMap<String,Layer>();
      participantLayers.put("language", null);
      participantLayers.put("corpus", null);
      participantLayers.put("age", null);
      participantLayers.put("sex", null);
      participantLayers.put("group", null);
      participantLayers.put("SES", null);
      participantLayers.put("role", null);
      participantLayers.put("education", null);
      participantLayers.put("custom", null);
   } // end of constructor
   
   /**
    * Resete state.
    */
   public void reset()
   {
      warnings = new Vector<String>();
      languages = new Vector<String>();
      mediaName = null;
      mediaType = null;
      participants = new HashMap<String,HashMap<String,String>>();
      transcribers = new Vector<String>();
      lines = new Vector<String>();
      headers = new Vector<String>();
   } // end of reset()

   // IStreamDeserializer methods:

   /**
    * Returns the deserializer's descriptor
    * @return The deserializer's descriptor
    */
   public SerializationDescriptor getDescriptor()
   {
      return new SerializationDescriptor(
	 "CLAN CHAT transcript", "0.5", "text/x-chat", ".cha", getClass().getResource("icon.gif"));
   }

   /**
    * Sets parameters for deserializer as a whole.  This might include database connection
    * parameters, locations of supporting files, etc.
    * <p>When the deserializer is installed, this method should be invoked with an empty parameter
    * set, to discover what (if any) general configuration is required. If parameters are returned,
    * and user interaction is possible, then the user may be presented with an interface for
    * setting/confirming these parameters. Once the parameters are set, this method can be
    * invoked again with the required values. Unlike the {@link #load(NamedStream[],Schema)}
    * method, this always returns th}e required parameters, whether or not they are fulfilled.
    * @param configuration The configuration for the deserializer. 
    * @param schema The layer schema, definining layers and the way they interrelate.
    * @return A list of configuration parameters that must be set before
    * {@link IDeserializer#setParameters(ParameterSet)} can be invoked. If this is an empty list,
    * {@link IDeserializer#setParameters(ParameterSet)} can be invoked. If it's not an empty list,
    * this method must be invoked again with the returned parameters' values set.
    */
   public ParameterSet configure(ParameterSet configuration, Schema schema)
   {
      setSchema(schema);
      setParticipantLayer(schema.getParticipantLayer());
      setTurnLayer(schema.getTurnLayer());
      setUtteranceLayer(schema.getUtteranceLayer());
      setWordLayer(schema.getWordLayer());
      linkageLayer = null;
      cUnitLayer = null;
      gemLayer = null;
      transcriberLayer = null;
      languagesLayer = null;
      expansionLayer = null;
      errorsLayer = null;
      retracingLayer = null;
      repetitionsLayer = null;
      completionLayer = null;
      disfluencyLayer = null;

      if (configuration.size() > 0)
      {
	 if (configuration.containsKey("participantLayer"))
	 {
	    setParticipantLayer((Layer)configuration.get("participantLayer").getValue());
	 }
	 if (configuration.containsKey("turnLayer"))
	 {
	    setTurnLayer((Layer)configuration.get("turnLayer").getValue());
	 }
	 if (configuration.containsKey("utteranceLayer"))
	 {
	    setUtteranceLayer((Layer)configuration.get("utteranceLayer").getValue());
	 }
	 if (configuration.containsKey("wordLayer"))
	 {
	    setWordLayer((Layer)configuration.get("wordLayer").getValue());
	 }
	 if (configuration.containsKey("disfluencyLayer"))
	 {
	    setDisfluencyLayer((Layer)configuration.get("disfluencyLayer").getValue());
	 }
	 if (configuration.containsKey("expansionLayer"))
	 {
	    setExpansionLayer((Layer)configuration.get("expansionLayer").getValue());
	 }
	 if (configuration.containsKey("errorsLayer"))
	 {
	    setErrorsLayer((Layer)configuration.get("errorsLayer").getValue());
	 }
	 if (configuration.containsKey("repetitionsLayer"))
	 {
	    setRepetitionsLayer((Layer)configuration.get("repetitionsLayer").getValue());
	 }
	 if (configuration.containsKey("retracingLayer"))
	 {
	    setRetracingLayer((Layer)configuration.get("retracingLayer").getValue());
	 }
	 if (configuration.containsKey("completionLayer"))
	 {
	    setCompletionLayer((Layer)configuration.get("completionLayer").getValue());
	 }
	 if (configuration.containsKey("gemLayer"))
	 {
	    setGemLayer((Layer)configuration.get("gemLayer").getValue());
	 }
	 if (configuration.containsKey("linkageLayer"))
	 {
	    setLinkageLayer((Layer)configuration.get("linkageLayer").getValue());
	 }
	 if (configuration.containsKey("cUnitLayer"))
	 {
	    setCUnitLayer((Layer)configuration.get("cUnitLayer").getValue());
	 }
	 if (configuration.containsKey("transcriberLayer"))
	 {
	    setTranscriberLayer((Layer)configuration.get("transcriberLayer").getValue());
	 }
	 if (configuration.containsKey("languagesLayer"))
	 {
	    setLanguagesLayer((Layer)configuration.get("languagesLayer").getValue());
	 }
	 for (String attribute : participantLayers.keySet())
	 {
	    if (configuration.containsKey(attribute + "Layer"))
	    {
	       participantLayers.put(attribute, ((Layer)configuration.get(attribute + "Layer").getValue()));
	    }
	 }
      }

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
	 Parameter p = configuration.containsKey("participantLayer")?configuration.get("participantLayer")
	    :configuration.addParameter(
	       new Parameter("participantLayer", Layer.class, "Participant layer", "Layer for speaker/participant identification", true));
	 String[] possibilities = {"participant","participants","who","speaker","speakers"};
	 if (p.getValue() == null) p.setValue(findLayerById(possibleParticipantLayers, possibilities));
	 p.setPossibleValues(possibleParticipantLayers.values());
      }
      if (getTurnLayer() == null)
      {
	 Parameter p = configuration.containsKey("turnLayer")?configuration.get("turnLayer")
	    :configuration.addParameter(
	       new Parameter("turnLayer", Layer.class, "Turn layer", "Layer for speaker turns", true));
	 String[] possibilities = {"turn","turns"};
	 if (p.getValue() == null) p.setValue(findLayerById(possibleTurnLayers, possibilities));
	 p.setPossibleValues(possibleTurnLayers.values());
      }
      if (getUtteranceLayer() == null)
      {
	 Parameter p = configuration.containsKey("utteranceLayer")?configuration.get("utteranceLayer")
	    :configuration.addParameter(
	       new Parameter("utteranceLayer", Layer.class, "Utterance layer", "Layer for speaker utterances", true));
	 String[] possibilities = {"utterance","utterances","line","lines"};
	 if (p.getValue() == null) p.setValue(findLayerById(possibleTurnChildLayers, possibilities));
	 p.setPossibleValues(possibleTurnChildLayers.values());
      }
      if (getWordLayer() == null)
      {
	 Parameter p = configuration.containsKey("wordLayer")?configuration.get("wordLayer")
	    :configuration.addParameter(
	       new Parameter("wordLayer", Layer.class, "Word layer", "Layer for individual word tokens", true));
	 String[] possibilities = {"transcript","word","words","w"};
	 if (p.getValue() == null) p.setValue(findLayerById(possibleTurnChildLayers, possibilities));
	 p.setPossibleValues(possibleTurnChildLayers.values());
      }
      Parameter pC = configuration.containsKey("cUnitLayer")?configuration.get("cUnitLayer")
	 :configuration.addParameter(
	    new Parameter("cUnitLayer", Layer.class, "C-Unit layer", "Layer for marking c-units"));
      String[] possibilitiesC = {"c-unit","cunit","sentence"};
      pC.setValue(findLayerById(possibleTurnChildLayers, possibilitiesC));
      pC.setPossibleValues(possibleTurnChildLayers.values());

      Parameter p = configuration.containsKey("disfluencyLayer")?configuration.get("disfluencyLayer")
	 :configuration.addParameter(
	    new Parameter("disfluencyLayer", Layer.class, "Disfluency layer", "Layer for disfluency annotations"));
      String[] possibilities_disfluency = {"disfluency","disfluencies"};
      if (p.getValue() == null) p.setValue(findLayerById(wordTagLayers, possibilities_disfluency));
      p.setPossibleValues(wordTagLayers.values());

      p = configuration.containsKey("expansionLayer")?configuration.get("expansionLayer")
	 :configuration.addParameter(
	    new Parameter("expansionLayer", Layer.class, "Expansion layer", "Layer for expansion annotations"));
      String[] possibilities_expansion = {"expansion","expansions"};
      if (p.getValue() == null) p.setValue(findLayerById(wordTagLayers, possibilities_expansion));
      p.setPossibleValues(wordTagLayers.values());

      p = configuration.containsKey("errorsLayer")?configuration.get("errorsLayer")
	 :configuration.addParameter(
	    new Parameter("errorsLayer", Layer.class, "Errors layer", "Layer for error  annotations"));
      String[] possibilities_error = {"error","error"};
      if (p.getValue() == null) p.setValue(findLayerById(possibleTurnChildLayers, possibilities_error));
      p.setPossibleValues(possibleTurnChildLayers.values());

      p = configuration.containsKey("linkageLayer")?configuration.get("linkageLayer")
	 :configuration.addParameter(
	    new Parameter("linkageLayer", Layer.class, "Linkages layer", "Layer for linkage annotations"));
      String[] possibilities_linkage = {"linkage","linkages"};
      if (p.getValue() == null) p.setValue(findLayerById(possibleTurnChildLayers, possibilities_linkage));
      p.setPossibleValues(possibleTurnChildLayers.values());

      p = configuration.containsKey("repetitionsLayer")?configuration.get("repetitionsLayer")
	 :configuration.addParameter(
	    new Parameter("repetitionsLayer", Layer.class, "Repetitions layer", "Layer for repetition annotations"));
      String[] possibilities_repetition = {"repetition","repetitions"};
      if (p.getValue() == null) p.setValue(findLayerById(possibleTurnChildLayers, possibilities_repetition));
      p.setPossibleValues(possibleTurnChildLayers.values());

      p = configuration.containsKey("retracingLayer")?configuration.get("retracingLayer")
	 :configuration.addParameter(
	    new Parameter("retracingLayer", Layer.class, "Retracing layer", "Layer for retracing annotations"));
      String[] possibilities_retrace = {"retrace","retracing","correction"};
      if (p.getValue() == null) p.setValue(findLayerById(possibleTurnChildLayers, possibilities_retrace));
      p.setPossibleValues(possibleTurnChildLayers.values());

      p = configuration.containsKey("completionLayer")?configuration.get("completionLayer")
	 :configuration.addParameter(
	    new Parameter("completionLayer", Layer.class, "Completion layer", "Layer for completion annotations"));
      String[] possibilities_completion = {"completion","completions"};
      if (p.getValue() == null) p.setValue(findLayerById(wordTagLayers, possibilities_completion));
      p.setPossibleValues(wordTagLayers.values());

      LinkedHashMap<String,Layer> possibleLayers = new LinkedHashMap<String,Layer>();
      for (Layer top : schema.getRoot().getChildren().values())
      {
	 if (top.getAlignment() == Constants.ALIGNMENT_INTERVAL)
	 { // aligned children of graph
	    possibleLayers.put(top.getId(), top);
	 }
      } // next top level layer
      p = configuration.containsKey("gemLayer")?configuration.get("gemLayer")
	 :configuration.addParameter(
	    new Parameter("gemLayer", Layer.class, "Gem layer", "Layer for gems"));
      String[] possibilities_gem = {"gem","gems","topic","topics"};
      if (p.getValue() == null) p.setValue(findLayerById(possibleLayers, possibilities_gem));
      p.setPossibleValues(possibleLayers.values());

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
      p = configuration.containsKey("transcriberLayer")?configuration.get("transcriberLayer")
	 :configuration.addParameter(
	    new Parameter("transcriberLayer", Layer.class, "Transcriber layer", "Layer for transcriber name"));
      String[] possibilities_transcriber = {"transcriber","transcribers","transcript_transcriber","transcript_transcribers", "scribe","scribes", "transcript_scribe","transcript_scribes"};
      if (p.getValue() == null) p.setValue(findLayerById(graphTagLayers, possibilities_transcriber));
      p.setPossibleValues(graphTagLayers.values());

      p = configuration.containsKey("languagesLayer")?configuration.get("languagesLayer")
	 :configuration.addParameter(
	    new Parameter("languagesLayer", Layer.class, "Transcript language layer", "Layer for transcriber language"));
      String[] possibilities_transcript = {"transcript_language","transcript_languages","language","languages"};
      if (p.getValue() == null) p.setValue(findLayerById(graphTagLayers, possibilities_transcript));
      p.setPossibleValues(graphTagLayers.values());

      // participant meta data layers
      for (String attribute : participantLayers.keySet())
      {
	 p = configuration.containsKey(attribute + "Layer")?configuration.get(attribute + "Layer")
	    :configuration.addParameter(
	       new Parameter(attribute + "Layer", Layer.class, attribute + " layer", "Layer for " + attribute));
	 // if we have a layer called that
	 String[] possibilities_participant = {"participant_"+attribute, attribute};
	 if (p.getValue() == null) p.setValue(findLayerById(participantTagLayers, possibilities_participant));
	 p.setPossibleValues(participantTagLayers.values());
      }
      
      return configuration;
   }

   /**
    * Loads the serialized form of the graph, using the given set of named streams.
    * @param streams A list of named streams that contain all the transcription/annotation data required.
    * @param schema The layer schema, definining layers and the way they interrelate.
    * @return A list of parameters that require setting before {@link IDeserializer#deserialize()} can be invoked. This may be an empty list, and may include parameters with the value already set to a workable default. If there are parameters, and user interaction is possible, then the user may be presented with an interface for setting/confirming these parameters, before they are then passed to {@link IDeserializer#setParameters(ParameterSet)}.
    * @throws SerializationException If the graph could not be loaded.
    * @throws IOException On IO error.
    * @throws SerializerNotConfiguredException If the configuration is not sufficient for deserialization.
    */
   @SuppressWarnings({"rawtypes", "unchecked"})
   public ParameterSet load(NamedStream[] streams, Schema schema) throws IOException, SerializationException, SerializerNotConfiguredException
   {
      // take the first cha stream, ignore all others.
      NamedStream cha = null;
      for (NamedStream stream : streams)
      {	 
	 if (stream.getName().toLowerCase().endsWith(".cha") 
	     || "text/x-chat".equals(stream.getMimeType()))
	 {
	    cha = stream;
	    break;
	 }
      } // next stream
      if (cha == null) throw new SerializationException("No CHAT stream found");
      setName(cha.getName());

      reset();

      // read stream line by line
      boolean inHeader = true;
      BufferedReader reader = new BufferedReader(new InputStreamReader(cha.getStream(), "UTF-8"));
      String line = reader.readLine();
      while (line != null)
      {
	 if (line.startsWith("@G") || line.startsWith("@Bg") || line.startsWith("*"))
	 {
	    inHeader = false;
	 }

	 if (inHeader)
	 {
	    if (line.startsWith("@"))
	    { // new header
	       headers.add(line);
	    }
	    else
	    { // continuation of last header line
	       // remove the last header
	       String lastHeader = headers.remove(headers.size()-1);
	       // append this line to it
	       headers.add(lastHeader + " " + line.trim());
	    }
	 }
	 else if (!line.equals("@End"))
	 { // transcript line
	    // is it the first line?
	    if (lines.size() == 0 
		// or this line starts with a speaker ID
		|| line.startsWith("*")
		// or the last line was time synchronized?
		|| lines.lastElement().endsWith("")
		// or the last line was a 'gem'?
		|| lines.lastElement().startsWith("@")
	       )
	    { // this is a new utterance
	       lines.add(line);
	    }
	    else
	    { // last line was not time-synchronized, so append this line to it
	       // remove the last line
	       String lastLine = lines.remove(lines.size()-1);
	       // append this line to it
	       lines.add(lastLine + " " + line);
	    }
	 }

	 line = reader.readLine();
      } // next line

      for (String header : headers)
      {
	 if (header.startsWith("@"))
	 { // @ line
	    int iColon = header.indexOf(':');
	    if (iColon >= 0)
	    { // it's a key/value pair
	       String value = header.substring(iColon + 1).trim();
	       if (header.startsWith("@Languages:"))
	       {
		  if (value.trim().length() > 0)
		  {
		     StringTokenizer tokens = new StringTokenizer(value, ", ");
		     while (tokens.hasMoreTokens())
		     {
			languages.add(tokens.nextToken());
		     }
		  }
	       }
	       else if (header.startsWith("@Participants:"))
	       {
		  // something like:
		  // @Participants:	SUB 2001 Participant, EXA Investigator Investigator
		  StringTokenizer tokens = new StringTokenizer(value, ",");
		  while (tokens.hasMoreTokens())
		  {
		     String sParticipant = tokens.nextToken();
		     StringTokenizer participantTokens = new StringTokenizer(sParticipant.trim());
		     if (participantTokens.hasMoreTokens())
		     {
			String id = participantTokens.nextToken();
			// ensure they're in the participants map
			if (!participants.containsKey(id))
			{
			   participants.put(id, new HashMap<String,String>());
			}
			if (participantTokens.hasMoreTokens())
			{
			   String name = participantTokens.nextToken();
			   participants.get(id).put("name", name);
			   if (participantTokens.hasMoreTokens())
			   {
			      String role = participantTokens.nextToken();
			      participants.get(id).put("role", role);
			   }
			}
		     }
		  }
	       }
	       else if (header.startsWith("@ID:"))
	       {		  
		  // @ID: language|corpus|code|age|sex|group|SES|role|education|custom|
		  String[] tokens = value.split("\\|");
		  String language = tokens.length<=0?"":tokens[0];
		  String corpus = tokens.length<=1?"":tokens[1];
		  String code = tokens.length<=2?"":tokens[2];
		  String age = tokens.length<=3?"":tokens[3];
		  String sex = tokens.length<=4?"":tokens[4];
		  String group = tokens.length<=5?"":tokens[5];
		  String SES = tokens.length<=6?"":tokens[6];
		  String role = tokens.length<=7?"":tokens[7];
		  String education = tokens.length<=8?"":tokens[8];
		  String custom = tokens.length<=9?"":tokens[9];
		  // ensure they're in the participants map
		  if (!participants.containsKey(code))
		  {
		     participants.put(code, new HashMap<String,String>());
		  }
		  // set the attribute values and make sure we ask for layers if there are values
		  participants.get(code).put("language", language);
		  participants.get(code).put("corpus", corpus);
		  participants.get(code).put("age", age);
		  participants.get(code).put("sex", sex);
		  participants.get(code).put("group", group);
		  participants.get(code).put("SES", SES);
		  participants.get(code).put("role", role);
		  participants.get(code).put("education", education);
		  participants.get(code).put("custom", custom);
	       }
	       else if (header.startsWith("@Media:"))
	       {
		  StringTokenizer tokens = new StringTokenizer(value, ", ");
		  if (tokens.hasMoreTokens())
		  {
		     setMediaName(tokens.nextToken());
		     if (tokens.hasMoreTokens())
		     {
			setMediaType(tokens.nextToken());
		     }
		  }
	       }
	       else if (header.startsWith("@Transcriber:"))
	       {
		  transcribers.add(value);
	       }
	    } // it's a key/value pair
	 } // @ line
      } // next header

      return new ParameterSet(); // everything is in configure()
   }

   
   /**
    * Tries to find a layer in the given map, using an ordered list of possible IDs.
    * @param possibleLayers Collection of layers from which a possibility can be selected.
    * @param possibleIds Guesses at possible layer IDs.
    * @return The first matching layer, or null if none matched.
    */
   public Layer findLayerById(LinkedHashMap<String,Layer> possibleLayers, String[] possibleIds)
   {
      HashSet<String> possibleLayerIds = new HashSet<String>();
      for (String id : possibleLayers.keySet()) possibleLayerIds.add(id.toLowerCase());
      for (String id : possibleIds)
      {
	 if (possibleLayerIds.contains(id.toLowerCase()))
	 {
	    return possibleLayers.get(id);
	 }
      }
      return null;
   } // end of findLayerById()


   /**
    * Sets parameters for a given deserialization operation, after loading the serialized form of the graph. This might include mappings from format-specific objects like tiers to graph layers, etc.
    * @param parameters The configuration for a given deserialization operation.
    * @throws SerializationParametersMissingException If not all required parameters have a value.
    */
   public void setParameters(ParameterSet parameters) throws SerializationParametersMissingException
   {
      if (getParticipantLayer() == null || getTurnLayer() == null || getUtteranceLayer() == null || getWordLayer() == null)
      {
	 throw new SerializationParametersMissingException();
      }
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
      // if there are errors, accumlate as many as we can before throwing SerializationException
      SerializationException errors = null;

      Graph graph = new Graph();
      graph.setId(getName());
      // add layers to the graph
      // we don't just copy the whole schema, because that would imply that all the extra layers
      // contained no annotations, which is not necessarily true
      graph.addLayer((Layer)getParticipantLayer().clone());
      graph.getSchema().setParticipantLayerId(getParticipantLayer().getId());
      graph.addLayer((Layer)getTurnLayer().clone());
      graph.getSchema().setTurnLayerId(getTurnLayer().getId());
      graph.addLayer((Layer)getUtteranceLayer().clone());
      graph.getSchema().setUtteranceLayerId(getUtteranceLayer().getId());
      graph.addLayer((Layer)getWordLayer().clone());
      graph.getSchema().setWordLayerId(getWordLayer().getId());
      if (getDisfluencyLayer() != null) graph.addLayer((Layer)getDisfluencyLayer().clone());
      if (getExpansionLayer() != null) graph.addLayer((Layer)getExpansionLayer().clone());
      if (getErrorsLayer() != null) graph.addLayer((Layer)getErrorsLayer().clone());
      if (getRepetitionsLayer() != null) graph.addLayer((Layer)getRepetitionsLayer().clone());
      if (getRetracingLayer() != null) graph.addLayer((Layer)getRetracingLayer().clone());
      if (getCompletionLayer() != null) graph.addLayer((Layer)getCompletionLayer().clone());
      if (getGemLayer() != null) graph.addLayer((Layer)getGemLayer().clone());
      if (getLinkageLayer() != null) graph.addLayer((Layer)getLinkageLayer().clone());
      if (getCUnitLayer() != null) graph.addLayer((Layer)getCUnitLayer().clone());
      if (getTranscriberLayer() != null) graph.addLayer((Layer)getTranscriberLayer().clone());
      if (getLanguagesLayer() != null) graph.addLayer((Layer)getLanguagesLayer().clone());
      for (String attribute : participantLayers.keySet())
      {
	 Layer layer = participantLayers.get(attribute);
	 if (layer != null)
	 {
	    graph.addLayer((Layer)layer.clone());
	 }
      } // next participant layer 

      // graph meta data
      if (getTranscriberLayer() != null)
      {
	 for (String transcriber : transcribers)
	 {
	    graph.createTag(graph, getTranscriberLayer().getId(), transcriber);
	 }
      }
      if (getLanguagesLayer() != null)
      {
	 for (String language : languages)
	 {
	    graph.createTag(graph, getLanguagesLayer().getId(), language);
	 }
      }
      
      // participants
      for (String participantId : participants.keySet())
      {
	 HashMap<String,String> attributes = participants.get(participantId);
	 Annotation participant = new Annotation(
	    participantId, 
	    attributes.containsKey("name")?attributes.get("name"):participantId, 
	    getParticipantLayer().getId());
	 participant.setParentId(graph.getId());
	 graph.addAnnotation(participant);
	 // set the participant meta-data
	 for (String attribute : participantLayers.keySet())
	 {
	    Layer layer = participantLayers.get(attribute);
	    if (layer != null && attributes.containsKey(attribute))
	    {
	       graph.createTag(participant, layer.getId(), attributes.get(attribute));
	    }
	 }	 
      }

      // ensure we have an utterance tokenizer
      if (getTokenizer() == null)
      {
	 setTokenizer(new SimpleTokenizer(getUtteranceLayer().getId(), getWordLayer().getId()));
      }

      // regular expressions
      Pattern lineSplitPattern = Pattern.compile("([0-9]+)_([0-9]+)");
      Pattern synchronisedPattern = Pattern.compile("^(.*)([0-9]+)_([0-9]+)$");
      Pattern specialTerminatorsPattern = Pattern.compile(
	 "^.*((\\+\\.\\.\\.)|(\\+\\.\\.\\?)|(\\+!\\?)|(\\+/\\.)|(\\+/\\?)|(\\+//\\.)|(\\+//\\?)|(\\+\\.)|(\\+\"/\\.)|(\\+\"\\.))$");
      Pattern basicTerminatorsPattern = Pattern.compile("^.*([.?!,;:])$");
      Pattern speakerPattern = Pattern.compile("^\\*(\\p{Alnum}+):(.+)$");

      // any lines that contain mid-line synchronisation codes are split into two
      Vector<String> syncLines = new Vector<String>();
      for (String line : getLines())
      {
	 Matcher matcher = lineSplitPattern.matcher(line);
	 // nibble off matching parts
	 int begin = 0;
	 while (matcher.find())
	 {
	    syncLines.add(line.substring(begin, matcher.end()));
	    begin = matcher.end();
	 } // next match
	 if (begin < line.length() - 1)
	 {
	    syncLines.add(line.substring(begin));
	 }
      }

      // utterances
      Annotation currentTurn = new Annotation(null, "", getTurnLayer().getId());
      Annotation lastUtterance = new Annotation(null, "", getUtteranceLayer().getId());
      Annotation gem = null;
      Annotation cUnit = null;
      Anchor lastAnchor = new Anchor(null, 0.0, Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL);
      graph.addAnchor(lastAnchor);
      Anchor lastAlignedAnchor = null;
      for (String line : syncLines)
      {
	 if (line.startsWith("@"))
	 {
	    if (line.startsWith("@G") || line.startsWith("@Bg"))
	    {
	       if (gem != null)
	       {
		  gem.setEnd(lastAnchor);
		  graph.addAnnotation(gem);
	       }
	       if (getGemLayer() != null)
	       {
		  String label = "gem";
		  int iColon = line.indexOf(':');
		  if (iColon >= 0)
		  { // it's a key/value pair
		     label = line.substring(iColon + 1).trim();
		  }
		  gem = new Annotation(null, label, getGemLayer().getId(), graph.getId());
	       }
	    }
	    else if (line.startsWith("@Eg") && gem != null)
	    {
	       gem.setEnd(lastAnchor);
	       graph.addAnnotation(gem);
	       gem = null;
	    }
	 }
	 else
	 {
	    Matcher speakerMatcher = speakerPattern.matcher(line);
	    if (speakerMatcher.matches())
	    { // setting the speaker, ID is 3 characters
	       if (cUnit != null && getCUnitLayer() != null)
	       { // add last c-unit
		  cUnit.setEnd(lastAnchor);
		  // try to match first the multi-character terminators, then the single-character ones
		  Matcher terminator = specialTerminatorsPattern.matcher(lastUtterance.getLabel());
		  if (!terminator.matches())
		  {
		     terminator = basicTerminatorsPattern.matcher(lastUtterance.getLabel());
		  }
		  if (terminator.matches())
		  {
		     // set the c-unit label as the terminator
		     cUnit.setLabel(terminator.group(1));
		     // strip the terminator off the utterance
		     lastUtterance.setLabel(lastUtterance.getLabel().substring(0, terminator.start(1)));
		  }
		  graph.addAnnotation(cUnit);
		  cUnit = null;
	       } // c-unit to add
	       // something like:
	       // *SUB: ...
	       String participantId = speakerMatcher.group(1);
	       if (!participants.containsKey(participantId))
	       {
		  warnings.add("Undeclared participant: " + participantId);
		  
		  Annotation participant = new Annotation(participantId, participantId, getParticipantLayer().getId());
		  participant.setParentId(graph.getId());
		  graph.addAnnotation(participant);
	       } // undeclared participant
	       if (!participantId.equals(currentTurn.getLabel()))
	       { // new turn
		  if (currentTurn.getEndId() == null)
		  {
		     currentTurn.setEnd(lastAnchor);
		     currentTurn = new Annotation(null, participantId, getTurnLayer().getId());
		     currentTurn.setStartId(lastAnchor.getId());		  
		     currentTurn.setParentId(participantId);
		     graph.addAnnotation(currentTurn);
		  }
	       }
	       line = speakerMatcher.group(2);
	    } // participant
	    
	    Annotation utterance = new Annotation(null, line, getUtteranceLayer().getId());
	    utterance.setStart(lastUtterance.getEnd());
	    if (cUnit == null && getCUnitLayer() != null)
	    {
	       cUnit = new Annotation(null, "", getCUnitLayer().getId());
	       cUnit.setStart(lastUtterance.getEnd());
	       cUnit.setParent(currentTurn);
	    }
	    Matcher synchronisedMatcher = synchronisedPattern.matcher(line);
	    if (synchronisedMatcher.matches())
	    {
	       utterance.setLabel(synchronisedMatcher.group(1).trim());
	       Anchor start = graph.getOrCreateAnchorAt(
		  Double.parseDouble(synchronisedMatcher.group(2))
		  / 1000, Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL);
	       if (cUnit != null && (cUnit.getStartId() == null 
				     || cUnit.getStartId().equals(utterance.getStartId()))) 
	       {
		  cUnit.setStart(start);
	       }
	       // avoid creating instantaneous utterances
	       avoidInstantaneousUtterance(start, lastUtterance, graph);
	       utterance.setStart(start);
	       Anchor end = graph.getOrCreateAnchorAt(
		  Double.parseDouble(synchronisedMatcher.group(3))
		  / 1000, Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL);
	       utterance.setEnd(end);
	       lastAlignedAnchor = end;
	       
	       if (start.getOffset() > end.getOffset())
	       { // start and end in reverse order 
		  if (errors == null) errors = new SerializationException();
		  errors.addError(SerializationException.ErrorType.Alignment, 
				  "Utterance start is after end: " 
				  + synchronisedMatcher.group(2) + "_" + synchronisedMatcher.group(3));
	       }
	       else
	       {
		  checkAlignmentAgainstLastUtterance(utterance, start, end, cUnit, lastUtterance, currentTurn, utteranceLayer, graph);
	       }
	    } // synchronised utterance
	    graph.addAnnotation(utterance);
	    utterance.setParent(currentTurn);

	    // if a gem has just been started
	    if (gem != null && gem.getStartId() == null)
	    { // set the gem's start anchor ID
	       gem.setStartId(utterance.getStartId());
	    }
	    
	    lastUtterance = utterance;
	    lastAnchor = utterance.getEnd();
	 } // transcript line
      } // next line
      if (gem != null)
      {
	 gem.setEnd(lastAnchor);
	 graph.addAnnotation(gem);
      }
      if (cUnit != null && getCUnitLayer() != null)
      { // add last c-unit
	 cUnit.setEnd(lastAnchor);
	 // try to match first the multi-character terminators, then the single-character ones
	 Matcher terminator = specialTerminatorsPattern.matcher(lastUtterance.getLabel());
	 if (!terminator.matches())
	 {
	    terminator = basicTerminatorsPattern.matcher(lastUtterance.getLabel());
	 }
	 if (terminator.matches())
	 {
	    // set the c-unit label as the terminator
	    cUnit.setLabel(terminator.group(1));
	    // strip the terminator off the utterance
	    lastUtterance.setLabel(lastUtterance.getLabel().substring(0, terminator.start(1)));
	 }
	 graph.addAnnotation(cUnit);
	 cUnit = null;
      } // c-unit to add
      if (lastAnchor.getOffset() == null && lastAlignedAnchor != null)
      {
	 if (lastAlignedAnchor == lastUtterance.getStart())
	 {
	    avoidInstantaneousUtterance(lastAlignedAnchor, lastUtterance, graph);
	    lastUtterance.getEnd().moveEndingAnnotations(lastAlignedAnchor);
	    lastAnchor = lastUtterance.getEnd();
	 }
	 else
	 {
	    lastAnchor.setOffset(lastAlignedAnchor.getOffset());
	 }
      }
      currentTurn.setEndId(lastAnchor.getId());

      // tokenize utterances into words
      try
      {
	 getTokenizer().transform(graph);
      }
      catch(TransformationException exception)
      {
	 if (errors == null) errors = new SerializationException();
	 if (errors.getCause() == null) errors.initCause(exception);
	 errors.addError(SerializationException.ErrorType.Tokenization, exception.getMessage());
      }

      try
      {
	 // split linkages?
	 if (getLinkageLayer() != null)
	 { 
	    SimpleTokenizer linkageSplitter 
	       = new SimpleTokenizer(getWordLayer().getId(), getLinkageLayer().getId(), "_", true);
	    linkageSplitter.transform(graph);
	    graph.commit();
	 }

	 // annotated spans marked by <...>
	 graph.addLayer(new Layer("@span", "Annotation Spans", Constants.ALIGNMENT_INTERVAL, true, true, false, getTurnLayer().getId(), true));
	 SpanningConventionTransformer spanningTransformer = new SpanningConventionTransformer(
	    getWordLayer().getId(), "<(.*)", "(.*)>(.*)", false, "$1", "$1$2", 
	    "@span", "-", "$1", "$1", false);	  
	 spanningTransformer.transform(graph);	
	 graph.commit();

	 // disfluencies
	 ConventionTransformer transformer = new ConventionTransformer(getWordLayer().getId(), "&(\\w+)");
	 transformer.addDestinationResult(getWordLayer().getId(), "$1");
	 if (getDisfluencyLayer() != null)
	 {
	    transformer.addDestinationResult(getDisfluencyLayer().getId(), "&");
	 }
	 transformer.transform(graph);
	 graph.commit();
	 
	 // expansions
	 String expansionLayerId = getExpansionLayer()!=null?getExpansionLayer().getId():null;
	 spanningTransformer = new SpanningConventionTransformer(
	    getWordLayer().getId(), "\\[:", "(.*)\\]", true, null, null, 
	    expansionLayerId, null, "$1", true);	  
	 spanningTransformer.transform(graph);	
	 graph.commit();

	 // errors
	 String errorsLayerId = getErrorsLayer()!=null?getErrorsLayer().getId():null;
	 spanningTransformer = new SpanningConventionTransformer(
	    getWordLayer().getId(), "\\[\\*", "(.*)\\](.*)", true, null, "$2", 
	    errorsLayerId, "", "$1", true, true);	  
	 spanningTransformer.transform(graph);	
	 if (errorsLayerId != null)
	 {
	    // if they coincide with the ends of spans, then they annotate the span
	    for (Annotation error : graph.list(errorsLayerId))
	    {
	       LinkedHashSet<Annotation> endingSpans = error.getEnd().endOf("@span");
	       if (endingSpans.size() > 0)
	       {
		  Annotation span = endingSpans.iterator().next();
		  error.setStart(span.getStart());
	       }
	    } // next error
	 } // errorsLayerId set
	 graph.commit();

	 // repetitions
	 String repetitionsLayerId = getRepetitionsLayer()!=null?getRepetitionsLayer().getId():null;
	 spanningTransformer = new SpanningConventionTransformer(
	    getWordLayer().getId(), "\\[/\\].*", "\\[/\\]", true, null, null, 
	    repetitionsLayerId, "/", "", true, true);	  
	 spanningTransformer.transform(graph);	
	 if (repetitionsLayerId != null)
	 {
	    // if they coincide with the ends of spans, then they annotate the span
	    for (Annotation repetition : graph.list(repetitionsLayerId))
	    {
	       LinkedHashSet<Annotation> endingSpans = repetition.getEnd().endOf("@span");
	       if (endingSpans.size() > 0)
	       {
		  Annotation span = endingSpans.iterator().next();
		  repetition.setStart(span.getStart());
	       }
	    } // next error
	 } // repetitionsLayerId set
	 graph.commit();
	 
	 // retracing
	 String retracingLayerId = getRetracingLayer()!=null?getRetracingLayer().getId():null;
	 spanningTransformer = new SpanningConventionTransformer(
	    getWordLayer().getId(), "\\[//\\]", "\\[//\\]", true, null, null, 
	    retracingLayerId, "//", "", true, true);	  
	 spanningTransformer.transform(graph);	
	 if (retracingLayerId != null)
	 {
	    // if they coincide with the ends of spans, then they annotate the span
	    for (Annotation retrace : graph.list(retracingLayerId))
	    {
	       LinkedHashSet<Annotation> endingSpans = retrace.getEnd().endOf("@span");
	       if (endingSpans.size() > 0)
	       {
		  Annotation span = endingSpans.iterator().next();
		  retrace.setStart(span.getStart());
	       }
	    } // next error
	 } // retracingLayerId set
	 graph.commit();
	 
	 // completions at the start and at the end
	 transformer = new ConventionTransformer(getWordLayer().getId(), "\\((\\p{Alnum}+)\\)(.+)\\((\\p{Alnum}+)\\)");
	 transformer.addDestinationResult(getWordLayer().getId(), "$2");
	 if (getCompletionLayer() != null)
	 {
	    transformer.addDestinationResult(getCompletionLayer().getId(), "$1$2$3");
	 }
	 transformer.transform(graph);	
	 graph.commit();
	 
	 // completions at the start
	 transformer = new ConventionTransformer(getWordLayer().getId(), "\\((\\p{Alnum}+)\\)(.+)");
	 transformer.addDestinationResult(getWordLayer().getId(), "$2");
	 if (getCompletionLayer() != null)
	 {
	    transformer.addDestinationResult(getCompletionLayer().getId(), "$1$2");
	 }
	 transformer.transform(graph);	
	 graph.commit();
	 
	 // completions at the end
	 transformer = new ConventionTransformer(getWordLayer().getId(), "(.+)\\((\\p{Alnum}+)\\)");
	 transformer.addDestinationResult(getWordLayer().getId(), "$1");
	 if (getCompletionLayer() != null)
	 {
	    transformer.addDestinationResult(getCompletionLayer().getId(), "$1$2");
	 }
	 transformer.transform(graph);
	 graph.commit();

	 // remove temporary span annotations
	 for (Annotation span : graph.list("@span"))
	 {
	    span.destroy();
	 } // next span	
	 graph.commit();

	 // set all annotations to manual confidence
	 for (Annotation a : graph.getAnnotationsById().values())
	 {
	    a.put(Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL);
	 }

      }
      catch(TransformationException exception)
      {
	 if (errors == null) errors = new SerializationException();
	 if (errors.getCause() == null) errors.initCause(exception);
	 errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
      }
      if (errors != null) throw errors;
      Graph[] graphs = { graph };
      return graphs;
   }


   /**
    * Avoid the special case of a non-syncrhonised utterance sandwiched between two synchronised ones, where the third starts and the time the first ends.
    * <p> e.g. 511.784 ...penultimateUtterance... 514.337 ...lastUtterance... 514.337 ...utterance... 517.092
    * @param start The next start anchor.
    * @param lastUtterance The last utterance.
    * @param graph The annotation graph.
    */
   public void avoidInstantaneousUtterance(Anchor start, Annotation lastUtterance, Graph graph)
   {
      if (lastUtterance.getStart() == start 
	  && lastUtterance.getEnd().getOffset() == null) // only for unaligned middle utterances
      {
	 Anchor middleAnchor = graph.addAnchor(new Anchor());
	 // can we reach back a further utterance
	 LinkedHashSet<Annotation> endingAtStart = start.endOf(getUtteranceLayer().getId());
	 Annotation penultimateUtterance = null;
	 if (endingAtStart.size() > 0)
	 {
	    penultimateUtterance = endingAtStart.iterator().next();
	    if (penultimateUtterance.getStart().getOffset() != null)
	    {
	       middleAnchor.setOffset(
		  penultimateUtterance.getStart().getOffset() 
		  + ((start.getOffset() - penultimateUtterance.getStart().getOffset()) /2));
	       middleAnchor.put(Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT);
	    } // penultimateUtterance has a start offset
	 } // there is a penultimate utterance
	 warnings.add("Utterance at " + lastUtterance.getStart().getOffset() 
		      + "-" + lastUtterance.getEnd().getOffset() + " is instantaneous"
		      + ": using " + middleAnchor + " as start time.");
	 // move linking annotations to the middle anchor
	 start.moveEndingAnnotations(middleAnchor);
	 start.moveStartingAnnotations(middleAnchor);
      }
   } // end of avoidInstantaneousUtterance()


   /**
    * Check the alignment of the given utterance against the given last utterance, to ensure there are no gaps if there shouldn't be, and that the anchors are in the right order.
    * @param utterance Current utterance, which is not assumed to be in the graph yet.
    * @param start The utterance's start anchor, which is assumed to be in the graph.
    * @param end The utterance's end anchor, which is assumed to be in the graph.
    * @param cUnit The current c-unit annotation, which is not assumed to be in the graph.
    * @param lastUtterance Last utterance, which is assumed to be in the graph.
    * @param currentTurn Current turn, which is assumed to be in the graph, and the parent of <var></var>utterance
    * @param utteranceLayer The utterance layer definition.
    * @param graph The annotation graph.
    */
   public void checkAlignmentAgainstLastUtterance(Annotation utterance, Anchor start, Anchor end, Annotation cUnit, Annotation lastUtterance, Annotation currentTurn, Layer utteranceLayer, Graph graph)
   {
      // is this utterance in the same turn as the last one?
      Anchor lastEnd = lastUtterance.getEnd();
      if (currentTurn.getId().equals(lastUtterance.getParentId()) && lastEnd != null)
      {
	 if (lastEnd.getOffset() == null)
	 { // last utterance was not aligned
	    // set its end time to the last of this one (and bring related annotions along)
	    lastUtterance.getEnd().moveEndingAnnotations(start);
	 }
	 else
	 { // we can check the alignment against the last one
	    if (utteranceLayer.getSaturated())
	    { // check for a gap between this one and the last one
	       if (start.getOffset() - lastEnd.getOffset() > 0.01)
	       { // add an empty filler utterance
		  graph.addAnnotation(
		     new Annotation(null, "", getUtteranceLayer().getId(), 
				    lastEnd.getId(), start.getId(), currentTurn.getId()));
	       } // gap between last utterance and this one
	    } // utterance layer should be saturated
	    
	    if (!utteranceLayer.getPeersOverlap())
	    { // check there's no overlap
	       if (start.getOffset() < lastEnd.getOffset())
	       { // this utterance starts before the last one ends
		  if (end.getOffset() > lastEnd.getOffset())
		  { // partial overlap
		     warnings.add("Utterance at " + start + "-" + end 
				  + " overlaps previous at " + lastUtterance.getStart() + "-" + lastEnd
				  + ": using " + lastEnd + " as start time.");
		     // use the later time
		     if (cUnit != null && cUnit.getStartId().equals(start.getId()))
		     {
			cUnit.setStartId(lastEnd.getId());
		     }
		     utterance.setStartId(lastEnd.getId());
		  }
		  else
		  { // full overlap - this should probably be an error, but instead:
		     // chain this utterance to the last one with an unaligned anchor
		     Anchor middleAnchor = graph.addAnchor(
			new Anchor(null, lastEnd.getOffset() + ((start.getOffset() - lastEnd.getOffset())/2), 
				   Constants.CONFIDENCE, Constants.CONFIDENCE_DEFAULT));
		     warnings.add("Utterance at " + start + "-" + end 
				  + " completely overlaps previous at " + lastUtterance.getStart() + "-" + lastEnd
				  + ": using " + middleAnchor +" as end time of first and start time of second.");
		     // set end
		     // (and bring related annotions along)
		     lastUtterance.getEnd().moveEndingAnnotations(middleAnchor);
		     // set start
		     if (cUnit != null && cUnit.getStartId().equals(start.getId()))
		     { // bring the c-unit along 
			// (can't use moveStartingAnnotations() because utterance isn't in graph)
			cUnit.setStart(middleAnchor);
		     }
		     utterance.setStart(middleAnchor);
		  }
	       } // this utterance starts before the last one ends
	    } // check there's no overlap
	 } // lastEnd offset is set
      } // utterance the same turn as the last one
   } // end of checkAlignmentAgainstLastUtterance()

   /**
    * Returns any warnings that may have arisen during the last execution of {@link #deserialize()}.
    * @return A possibly empty lilayersst of warnings.
    */
   public String[] getWarnings()
   {
      return warnings.toArray(new String[0]);
   }

} // end of class ChatDeserializer
