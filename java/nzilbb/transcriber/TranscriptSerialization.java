//
// Copyright 2016-2019 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.transcriber;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Spliterator;
import java.util.Vector;
import java.util.function.Consumer;
import javax.xml.parsers.ParserConfigurationException;
import nzilbb.ag.*;
import nzilbb.ag.serialize.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.util.UtteranceParallelizer;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.util.IO;
import nzilbb.util.TempFileInputStream;
import org.xml.sax.SAXException;

/**
 * Deserializer for trs files produced with Transcriber.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class TranscriptSerialization
   extends Transcript
   implements IDeserializer, ISerializer
{
   // Attributes:
   protected Vector<String> warnings;

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
   public TranscriptSerialization setSchema(Schema newSchema) { schema = newSchema; return this; }

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
   public TranscriptSerialization setParticipantLayer(Layer newParticipantLayer) { participantLayer = newParticipantLayer; return this; }

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
   public TranscriptSerialization setTurnLayer(Layer newTurnLayer) { turnLayer = newTurnLayer; return this; }

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
   public TranscriptSerialization setUtteranceLayer(Layer newUtteranceLayer) { utteranceLayer = newUtteranceLayer; return this; }

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
   public TranscriptSerialization setWordLayer(Layer newWordLayer) { wordLayer = newWordLayer; return this; }

   /**
    * Layer for topic tags.
    * @see #getTopicLayer()
    * @see #setTopicLayer(Layer)
    */
   protected Layer topicLayer;
   /**
    * Getter for {@link #topicLayer}: Layer for topic tags.
    * @return Layer for topic tags.
    */
   public Layer getTopicLayer() { return topicLayer; }
   /**
    * Setter for {@link #topicLayer}: Layer for topic tags.
    * @param newTopicLayer Layer for topic tags.
    */
   public TranscriptSerialization setTopicLayer(Layer newTopicLayer) { topicLayer = newTopicLayer; return this; }

   /**
    * Layer for language tags.
    * @see #getLanguageLayer()
    * @see #setLanguageLayer(Layer)
    */
   protected Layer languageLayer;
   /**
    * Getter for {@link #languageLayer}: Layer for language tags.
    * @return Layer for language tags.
    */
   public Layer getLanguageLayer() { return languageLayer; }
   /**
    * Setter for {@link #languageLayer}: Layer for language tags.
    * @param newLanguageLayer Layer for language tags.
    */
   public TranscriptSerialization setLanguageLayer(Layer newLanguageLayer) { languageLayer = newLanguageLayer; return this; }

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
   public TranscriptSerialization setLexicalLayer(Layer newLexicalLayer) { lexicalLayer = newLexicalLayer; return this; }

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
   public TranscriptSerialization setPronounceLayer(Layer newPronounceLayer) { pronounceLayer = newPronounceLayer; return this; }

   /**
    * Layer for named entities.
    * @see #getEntityLayer()
    * @see #setEntityLayer(Layer)
    */
   protected Layer entityLayer;
   /**
    * Getter for {@link #entityLayer}: Layer for named entities.
    * @return Layer for named entities.
    */
   public Layer getEntityLayer() { return entityLayer; }
   /**
    * Setter for {@link #entityLayer}: Layer for named entities.
    * @param newEntityLayer Layer for named entities.
    */
   public TranscriptSerialization setEntityLayer(Layer newEntityLayer) { entityLayer = newEntityLayer; return this; }

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
   public TranscriptSerialization setCommentLayer(Layer newCommentLayer) { commentLayer = newCommentLayer; return this; }

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
   public TranscriptSerialization setNoiseLayer(Layer newNoiseLayer) { noiseLayer = newNoiseLayer; return this; }

   /**
    * Transcript tag layer for name of trascriber.
    * @see #getScribeLayer()
    * @see #setScribeLayer(Layer)
    */
   protected Layer scribeLayer;
   /**
    * Getter for {@link #scribeLayer}: Transcript tag layer for name of trascriber.
    * @return Transcript tag layer for name of trascriber.
    */
   public Layer getScribeLayer() { return scribeLayer; }
   /**
    * Setter for {@link #scribeLayer}: Transcript tag layer for name of trascriber.
    * @param newScribeLayer Transcript tag layer for name of trascriber.
    */
   public TranscriptSerialization setScribeLayer(Layer newScribeLayer) { scribeLayer = newScribeLayer; return this; }

   /**
    * Transcript tag layer for version.
    * @see #getVersionLayer()
    * @see #setVersionLayer(Layer)
    */
   protected Layer versionLayer;
   /**
    * Getter for {@link #versionLayer}: Transcript tag layer for version.
    * @return Transcript tag layer for version.
    */
   public Layer getVersionLayer() { return versionLayer; }
   /**
    * Setter for {@link #versionLayer}: Transcript tag layer for version.
    * @param newVersionLayer Transcript tag layer for version.
    */
   public TranscriptSerialization setVersionLayer(Layer newVersionLayer) { versionLayer = newVersionLayer; return this; }

   /**
    * Transcript tag layer for version date.
    * @see #getVersionDateLayer()
    * @see #setVersionDateLayer(Layer)
    */
   protected Layer versionDateLayer;
   /**
    * Getter for {@link #versionDateLayer}: Transcript tag layer for version date.
    * @return Transcript tag layer for version date.
    */
   public Layer getVersionDateLayer() { return versionDateLayer; }
   /**
    * Setter for {@link #versionDateLayer}: Transcript tag layer for version date.
    * @param newVersionDateLayer Transcript tag layer for version date.
    */
   public TranscriptSerialization setVersionDateLayer(Layer newVersionDateLayer) { versionDateLayer = newVersionDateLayer; return this; }

   /**
    * Transcript tag layer for program name.
    * @see #getProgramLayer()
    * @see #setProgramLayer(Layer)
    */
   protected Layer programLayer;
   /**
    * Getter for {@link #programLayer}: Transcript tag layer for program name.
    * @return Transcript tag layer for program name.
    */
   public Layer getProgramLayer() { return programLayer; }
   /**
    * Setter for {@link #programLayer}: Transcript tag layer for program name.
    * @param newProgramLayer Transcript tag layer for program name.
    */
   public TranscriptSerialization setProgramLayer(Layer newProgramLayer) { programLayer = newProgramLayer; return this; }

   /**
    * Transcript tag layer for air date.
    * @see #getAirDateLayer()
    * @see #setAirDateLayer(Layer)
    */
   protected Layer airDateLayer;
   /**
    * Getter for {@link #airDateLayer}: Transcript tag layer for air date.
    * @return Transcript tag layer for air date.
    */
   public Layer getAirDateLayer() { return airDateLayer; }
   /**
    * Setter for {@link #airDateLayer}: Transcript tag layer for air date.
    * @param newAirDateLayer Transcript tag layer for air date.
    */
   public TranscriptSerialization setAirDateLayer(Layer newAirDateLayer) { airDateLayer = newAirDateLayer; return this; }

   /**
    * Transcript tag layer for trascript language.
    * @see #getTranscriptLanguageLayer()
    * @see #setTranscriptLanguageLayer(Layer)
    */
   protected Layer transcriptLanguageLayer;
   /**
    * Getter for {@link #transcriptLanguageLayer}: Transcript tag layer for trascript language.
    * @return Transcript tag layer for trascript language.
    */
   public Layer getTranscriptLanguageLayer() { return transcriptLanguageLayer; }
   /**
    * Setter for {@link #transcriptLanguageLayer}: Transcript tag layer for trascript language.
    * @param newTranscriptLanguageLayer Transcript tag layer for trascript language.
    */
   public TranscriptSerialization setTranscriptLanguageLayer(Layer newTranscriptLanguageLayer) { transcriptLanguageLayer = newTranscriptLanguageLayer; return this; }

   /**
    * Participant tag layer for check attribute.
    * @see #getParticipantCheckLayer()
    * @see #setParticipantCheckLayer(Layer)
    */
   protected Layer participantCheckLayer;
   /**
    * Getter for {@link #participantCheckLayer}: Participant tag layer for check attribute.
    * @return Participant tag layer for check attribute.
    */
   public Layer getParticipantCheckLayer() { return participantCheckLayer; }
   /**
    * Setter for {@link #participantCheckLayer}: Participant tag layer for check attribute.
    * @param newParticipantCheckLayer Participant tag layer for check attribute.
    */
   public TranscriptSerialization setParticipantCheckLayer(Layer newParticipantCheckLayer) { participantCheckLayer = newParticipantCheckLayer; return this; }

   /**
    * Participant tag layer for gender ('type') attribute.
    * @see #getGenderLayer()
    * @see #setGenderLayer(Layer)
    */
   protected Layer genderLayer;
   /**
    * Getter for {@link #genderLayer}: Participant tag layer for gender ('type') attribute.
    * @return Participant tag layer for gender ('type') attribute.
    */
   public Layer getGenderLayer() { return genderLayer; }
   /**
    * Setter for {@link #genderLayer}: Participant tag layer for gender ('type') attribute.
    * @param newGenderLayer Participant tag layer for gender ('type') attribute.
    */
   public TranscriptSerialization setGenderLayer(Layer newGenderLayer) { genderLayer = newGenderLayer; return this; }

   /**
    * Participant tag layer for dialect attribute.
    * @see #getDialectLayer()
    * @see #setDialectLayer(Layer)
    */
   protected Layer dialectLayer;
   /**
    * Getter for {@link #dialectLayer}: Participant tag layer for dialect attribute.
    * @return Participant tag layer for dialect attribute.
    */
   public Layer getDialectLayer() { return dialectLayer; }
   /**
    * Setter for {@link #dialectLayer}: Participant tag layer for dialect attribute.
    * @param newDialectLayer Participant tag layer for dialect attribute.
    */
   public TranscriptSerialization setDialectLayer(Layer newDialectLayer) { dialectLayer = newDialectLayer; return this; }

   /**
    * Participant tag layer for accent attribute.
    * @see #getAccentLayer()
    * @see #setAccentLayer(Layer)
    */
   protected Layer accentLayer;
   /**
    * Getter for {@link #accentLayer}: Participant tag layer for accent attribute.
    * @return Participant tag layer for accent attribute.
    */
   public Layer getAccentLayer() { return accentLayer; }
   /**
    * Setter for {@link #accentLayer}: Participant tag layer for accent attribute.
    * @param newAccentLayer Participant tag layer for accent attribute.
    */
   public TranscriptSerialization setAccentLayer(Layer newAccentLayer) { accentLayer = newAccentLayer; return this; }

   /**
    * Participant tag layer for scope attribute.
    * @see #getScopeLayer()
    * @see #setScopeLayer(Layer)
    */
   protected Layer scopeLayer;
   /**
    * Getter for {@link #scopeLayer}: Participant tag layer for scope attribute.
    * @return Participant tag layer for scope attribute.
    */
   public Layer getScopeLayer() { return scopeLayer; }
   /**
    * Setter for {@link #scopeLayer}: Participant tag layer for scope attribute.
    * @param newScopeLayer Participant tag layer for scope attribute.
    */
   public TranscriptSerialization setScopeLayer(Layer newScopeLayer) { scopeLayer = newScopeLayer; return this; }

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
   public TranscriptSerialization setCancelling(boolean newCancelling) { cancelling = newCancelling; return this; }
   /**
    * Cancel the serialization in course (if any).
    */
   public void cancel()
   {
      setCancelling(true);
   }
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public TranscriptSerialization()
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
	 "Transcriber transcript", "1.51", "text/xml-transcriber", ".trs", "20191211.1902",
         getClass().getResource("icon.png"));
   }

   /**
    * Sets parameters for deserializer as a whole.  This might include database connection parameters, locations of supporting files, etc.
    * <p>When the deserializer is installed, this method should be invoked with an empty parameter
    *  set, to discover what (if any) general configuration is required. If parameters are
    *  returned, and user interaction is possible, then the user may be presented with an
    *  interface for setting/confirming these parameters.  
    * @param configuration The configuration for the deserializer. 
    * @param schema The layer schema, definining layers and the way they interrelate.
    * @return A list of configuration parameters (still) must be set before {@link IDeserializer#setParameters()} can be invoked. If this is an empty list, {@link IDeserializer#setParameters()} can be invoked. If it's not an empty list, this method must be invoked again with the returned parameters' values set.
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

      layerToPossibilities.put(new Parameter("topicLayer", Layer.class, "Topic layer", "Topic tags"), 
				  Arrays.asList("topic","topics","gem","gems"));
      layerToCandidates.put("topicLayer", topLevelLayers);

      layerToPossibilities.put(
	 new Parameter("commentLayer", Layer.class, "Comment layer", "Commentary"), 
	 Arrays.asList("comment","commentary","note","notes"));
      layerToCandidates.put("commentLayer", topLevelLayers);

      layerToPossibilities.put(
	 new Parameter("noiseLayer", Layer.class, "Noise layer", "Noise annotations"), 
	 Arrays.asList("noise","noises","backgroundnoise"));
      layerToCandidates.put("noiseLayer", topLevelLayers);

      layerToPossibilities.put(
	 new Parameter("languageLayer", Layer.class, "Language layer", "Inline language tags"), 
	 Arrays.asList("language","lang"));
      layerToCandidates.put("languageLayer", possibleTurnChildLayers);

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
	 new Parameter("entityLayer", Layer.class, "Entity layer", "Named entities"), 
	 Arrays.asList("entity","namedentity","entities","namedentities"));
      layerToCandidates.put("entityLayer", possibleTurnChildLayers);

      layerToPossibilities.put(
	 new Parameter("scribeLayer", Layer.class, "Scribe layer", "Name of transcriber"), 
	 Arrays.asList("transcripttranscriber","transcriptscribe", "scribe","transcriber"));
      layerToCandidates.put("scribeLayer", graphTagLayers);

      layerToPossibilities.put(
	 new Parameter("versionLayer", Layer.class, "Version layer", "Version of transcriber"), 
	 Arrays.asList("transcriptversion","version"));
      layerToCandidates.put("versionLayer", graphTagLayers);

      layerToPossibilities.put(
	 new Parameter("versionDateLayer", Layer.class, "Version Date layer", "Version date of transcriber"), 
	 Arrays.asList("transcriptversiondate","versiondate"));
      layerToCandidates.put("versionDateLayer", graphTagLayers);

      layerToPossibilities.put(
	 new Parameter("programLayer", Layer.class, "Program layer", "Name of the program recorded"), 
	 Arrays.asList("transcriptprogram","program","episode","series","family"));
      layerToCandidates.put("programLayer", graphTagLayers);

      layerToPossibilities.put(
	 new Parameter("airDateLayer", Layer.class, "Air-date layer", "Date the program aired"), 
	 Arrays.asList("transcriptairdate","airedate", "transcriptrecordingdate","recordingdate"));
      layerToCandidates.put("airDateLayer", graphTagLayers);

      layerToPossibilities.put(
	 new Parameter("transcriptLanguageLayer", Layer.class, "Transcript Language layer", 
		       "The language of the whole transcript"), 
	 Arrays.asList("transcriptlanguage","language"));
      layerToCandidates.put("transcriptLanguageLayer", graphTagLayers);

      layerToPossibilities.put(
	 new Parameter("participantCheckLayer", Layer.class, "Participant Check layer", 
		       "Participant checked"), 
	 Arrays.asList("participantcheck"));
      layerToCandidates.put("participantCheckLayer", participantTagLayers);

      layerToPossibilities.put(
	 new Parameter("genderLayer", Layer.class, "Gender layer", 
		       "Gender - participant 'type'"), 
	 Arrays.asList("participantgender","participantsex","gender","sex"));
      layerToCandidates.put("genderLayer", participantTagLayers);

      layerToPossibilities.put(
	 new Parameter("dialectLayer", Layer.class, "Dialect layer", 
		       "Participant's dialect"), 
	 Arrays.asList("participantdialect","dialect"));
      layerToCandidates.put("dialectLayer", participantTagLayers);

      layerToPossibilities.put(
	 new Parameter("accentLayer", Layer.class, "Accent layer", 
		       "Participant's accent"), 
	 Arrays.asList("participantaccent","accent"));
      layerToCandidates.put("accentLayer", participantTagLayers);

      layerToPossibilities.put(
	 new Parameter("scopeLayer", Layer.class, "Scope layer", 
		       "Participant's 'scope'"), 
	 Arrays.asList("participantscope","scope"));
      layerToCandidates.put("scopeLayer", participantTagLayers);

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
      reset();

      // take the first stream, ignore all others.
      NamedStream trs = Utility.FindSingleStream(streams, ".trs", "text/xml-transcriber");
      if (trs == null) throw new SerializationException("No Transciber transcript stream found");
      setId(trs.getName());
      try
      {
	 load(trs.getStream());
      }
      catch(ParserConfigurationException exception)
      {
	 throw new SerializationException(exception);
      }
      catch(SAXException exception)
      {
	 throw new SerializationException(exception);
      }

      setSchema(schema);

      return new ParameterSet();
   }

   /**
    * Sets parameters for a given deserialization operation, after loading the serialized
    * form of the graph. This might include mappings from format-specific objects like
    * tiers to graph layers, etc. 
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
    * @return A list of valid (if incomplete) {@link Graph}s. 
    * @throws SerializerNotConfiguredException if the object has not been configured.
    * @throws SerializationParametersMissingException if the parameters for this particular graph have not been set.
    * @throws SerializationException if errors occur during deserialization.
    */
   public Graph[] deserialize() 
      throws SerializerNotConfiguredException, SerializationParametersMissingException, SerializationException
   {
      if (participantLayer == null) throw new SerializerNotConfiguredException("Participant layer not set");
      if (turnLayer == null) throw new SerializerNotConfiguredException("Turn layer not set");
      if (utteranceLayer == null) throw new SerializerNotConfiguredException("Utterance layer not set");
      if (wordLayer == null) throw new SerializerNotConfiguredException("Word layer not set");
      if (schema == null) throw new SerializerNotConfiguredException("Layer schema not set");

      validate();

      Graph graph = new Graph();
      graph.setId(getId());
      // creat the 0 anchor to prevent graph tagging from creating one with no confidence
      graph.getOrCreateAnchorAt(0.0, Constants.CONFIDENCE_AUTOMATIC);

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
      if (topicLayer != null) graph.addLayer((Layer)topicLayer.clone());
      if (languageLayer != null) graph.addLayer((Layer)languageLayer.clone());
      if (lexicalLayer != null) graph.addLayer((Layer)lexicalLayer.clone());
      if (pronounceLayer != null) graph.addLayer((Layer)pronounceLayer.clone());
      if (entityLayer != null) graph.addLayer((Layer)entityLayer.clone());
      if (commentLayer != null) graph.addLayer((Layer)commentLayer.clone());
      if (noiseLayer != null) graph.addLayer((Layer)noiseLayer.clone());
      if (scribeLayer != null) graph.addLayer((Layer)scribeLayer.clone());
      if (versionLayer != null) graph.addLayer((Layer)versionLayer.clone());
      if (versionDateLayer != null) graph.addLayer((Layer)versionDateLayer.clone());
      if (programLayer != null) graph.addLayer((Layer)programLayer.clone());
      if (airDateLayer != null) graph.addLayer((Layer)airDateLayer.clone());
      if (transcriptLanguageLayer != null) graph.addLayer((Layer)transcriptLanguageLayer.clone());
      if (participantCheckLayer != null) graph.addLayer((Layer)participantCheckLayer.clone());
      if (genderLayer != null) graph.addLayer((Layer)genderLayer.clone());
      if (dialectLayer != null) graph.addLayer((Layer)dialectLayer.clone());
      if (accentLayer != null) graph.addLayer((Layer)accentLayer.clone());
      if (scopeLayer != null) graph.addLayer((Layer)scopeLayer.clone());

      graph.setOffsetUnits(Constants.UNIT_SECONDS);

      // attributes
      if (getScribe() != null && getScribe().length() > 0 && scribeLayer != null)
      {
	 graph.createTag(graph, scribeLayer.getId(), getScribe());
      }
      if (getVersion() != null && getVersion().length() > 0 && versionLayer != null)
      {
	 graph.createTag(graph, versionLayer.getId(), getVersion());
      }
      if (getVersionDate() != null && getVersionDate().length() > 0 && versionDateLayer != null)
      {
	 graph.createTag(graph, versionDateLayer.getId(), getVersionDate());
      }
      if (getProgram() != null && getProgram().length() > 0 && programLayer != null)
      {
	 graph.createTag(graph, programLayer.getId(), getProgram());
      }
      if (getAirDate() != null && getAirDate().length() > 0 && airDateLayer != null)
      {
	 graph.createTag(graph, airDateLayer.getId(), getAirDate());
      }
      if (getLanguage() != null && getLanguage().length() > 0 && transcriptLanguageLayer != null)
      {
	 graph.createTag(graph, transcriptLanguageLayer.getId(), getLanguage());
      }

      // participants
      for (Speaker speaker : getSpeakers())
      {
	 Annotation participant = new Annotation(
	    speaker.getId(), 
	    speaker.getName(), 
	    schema.getParticipantLayerId());
	 participant.setParentId(graph.getId());
	 graph.addAnnotation(participant);

	 if (speaker.getCheck().length() > 0 && participantCheckLayer != null)
	 {
	    graph.createTag(participant, participantCheckLayer.getId(), speaker.getCheck());
	 }
	 if (speaker.getType().length() > 0 && genderLayer != null)
	 {
	    String genderLabel = speaker.getType();
	    LinkedHashMap<String,String> validLabels = genderLayer.getValidLabels();
	    if (validLabels.size() > 0 // there are valid labels
		// and the "type" isn't one of them
		&& !validLabels.containsKey(genderLabel)) 
	    { // normalize label if possible
	       for (String key : validLabels.keySet())
	       {
		  if (key.equalsIgnoreCase(genderLabel)
		      || validLabels.get(key).equalsIgnoreCase(genderLabel))
		  {
		     genderLabel = key;
		     break;
		  }		     
	       } // next label
	    } // normalize label if possible
	    graph.createTag(participant, genderLayer.getId(), genderLabel);
	 }
	 if (speaker.getDialect().length() > 0 && dialectLayer != null)
	 {
	    graph.createTag(participant, dialectLayer.getId(), speaker.getDialect());
	 }
	 if (speaker.getAccent().length() > 0 && accentLayer != null)
	 {
	    graph.createTag(participant, accentLayer.getId(), speaker.getAccent());
	 }
	 if (speaker.getScope().length() > 0 && scopeLayer != null)
	 {
	    graph.createTag(participant, scopeLayer.getId(), speaker.getScope());
	 }
      }

      // graph
      HashMap<String,Annotation> htStartedEvents = new HashMap<String,Annotation>();
      
      // for each section
      for (Section section : getSections())
      {
	 Annotation anTopic = null;
	 String sTopic = section.getTopicName();
	 if (sTopic != null && sTopic.length() > 0 && topicLayer != null)
	 {
	    anTopic = new Annotation(null, sTopic, topicLayer.getId());
	    anTopic.setStart(
	       graph.getOrCreateAnchorAt(
		  section.getStartTimeAsDouble(), Constants.CONFIDENCE_AUTOMATIC));
	    anTopic.setEnd(
	       graph.getOrCreateAnchorAt(
		  section.getEndTimeAsDouble(), Constants.CONFIDENCE_AUTOMATIC));
	    graph.addAnnotation(anTopic);
	 }
	 
	 // for each turn
	 for (Turn turn : section.getTurns())
	 {
	    // could be simultaneous speech so we need a speakerId lookup
	    HashMap<String, Annotation> htTurnAnnotations = new HashMap<String, Annotation>();
	    
	    // turn.getSpeakerId() is something like "spk1" or "spk2 spk1"
	    for (String sSpeakerId : turn.getSpeakerId().split(" "))
	    {
	       String label = sSpeakerId;
	       if (getSpeaker(sSpeakerId) != null) label = getSpeaker(sSpeakerId).getName();
	       Annotation anTurn = new Annotation(null, label, schema.getTurnLayerId());
	       if (graph.getAnnotation(sSpeakerId) != null)
	       {
		  anTurn.setParentId(graph.getAnnotation(sSpeakerId).getId());
	       }
	       anTurn.setStart(
		  graph.getOrCreateAnchorAt(
		     turn.getStartTimeAsDouble(), Constants.CONFIDENCE_AUTOMATIC));
	       anTurn.setEnd(
		  graph.getOrCreateAnchorAt(
		     turn.getEndTimeAsDouble(), Constants.CONFIDENCE_AUTOMATIC));
	       htTurnAnnotations.put(sSpeakerId, anTurn);
	       graph.addAnnotation(anTurn);
	    }
	    
	    // for each sync
	    for(Sync sync : turn.getSyncs())
	    {
	       Vector<Sync> vSubSyncs = new Vector<Sync>();
	       if (sync.isSimultaneousSpeech())
	       {
		  // whole bunch of syncs at once
		  vSubSyncs.addAll(sync.getSimultaneousSyncs());
	       }
	       else
	       {
		  // just one sync - this one
		  vSubSyncs.addElement(sync);
	       }
	       
	       for (Sync thisSync : vSubSyncs)
	       {
		  // lookup the turn annotation
		  Annotation anTurn = htTurnAnnotations.get(thisSync.getWho());
		  Annotation anLine = new Annotation(
		     null, anTurn.getLabel(), schema.getUtteranceLayerId());
		  anLine.setParentId(anTurn.getId());
		  anLine.setStart(
		     graph.getOrCreateAnchorAt(
			thisSync.getTimeAsDouble(), Constants.CONFIDENCE_AUTOMATIC));
		  graph.addAnnotation(anLine);
		  
		  // force Events to assign to their given Words where poss.
		  thisSync.getWords();
		  
		  Vector<Annotation> vWordlessEvents = new Vector<Annotation>();
		  
		  // events that have no word
		  for (Event event : thisSync.getEvents())
		  {
		     if (event.getWord() == null)
		     {
			Annotation anEvent = new Annotation();
			if (event.getType().equals("noise"))
			{
			   if (noiseLayer != null) anEvent.setLayerId(noiseLayer.getId());
			}
			else if (event.getType().equals("lexical"))
			{
			   if (lexicalLayer != null)
			   {
			      vWordlessEvents.add(anEvent);
			      anEvent.setLayerId(lexicalLayer.getId());
			   }
			}
			else if (event.getType().equals("pronounce"))
			{
			   if (pronounceLayer != null)
			   {
			      vWordlessEvents.add(anEvent);
			      anEvent.setLayerId(pronounceLayer.getId());
			   }
			}
			else if (event.getType().equals("language"))
			{
			   if (languageLayer != null) anEvent.setLayerId(languageLayer.getId());
			}
			else if (event.getType().equals("entities"))
			{
			   if (entityLayer != null) anEvent.setLayerId(entityLayer.getId());
			   anEvent.setParentId(anTurn.getId());
			}
			else if (event.getType().equals("comment"))
			{
			   if (commentLayer != null) anEvent.setLayerId(commentLayer.getId());
			   anEvent.setParentId(graph.getId());			   
			}
			anEvent.setLabel(event.getDescription());
			
			if (anEvent.getLayerId() != null)
			{ // we have a layer for this type of event
			   if (event.getExtent().equals("begin"))
			   {
			      // set the start anchor
			      anEvent.setStartId(anLine.getStartId());
			      // and save it until we find the end...
			      htStartedEvents.put(
				 event.getType() + ":" + event.getDescription(), anEvent);
			   }
			   else if (event.getExtent().equals("end"))
			   {
			      // ditch this anchor, use the one created at the
			      // beginning instead:
			      anEvent = htStartedEvents.remove(
				 event.getType() + ":" + event.getDescription());
			      if (anEvent != null)
			      {
				 anEvent.setEnd(anLine.getStart());
				 graph.addAnnotation(anEvent);
			      }
			   }
			   else // put it at the beginning of the line
			   {
			      anEvent.setStart(anLine.getStart());
			      anEvent.setEnd(anLine.getStart());
			      graph.addAnnotation(anEvent);
			   }
			}
			
		     }  // event has no word
		  } // next sync event
		  
		  // words
		  Annotation lastWord = null;
		  for (Word word : thisSync.getWords())
		  {
		     Annotation anWord = new Annotation(null, word.getRawOrthography(), schema.getWordLayerId());
		     anWord.setParentId(anTurn.getId());
		     graph.addAnnotation(anWord);
		     if (lastWord == null) anWord.setStartId(anLine.getStartId());
		     lastWord = anWord;
		     
		     // line-start events that need a word set?
		     for (Annotation anEvent : vWordlessEvents)
		     {
			anEvent.setParentId(anWord.getId());
		     } // next wordless event
		     vWordlessEvents.clear();
		     
		     // events assigned to this word
		     for (Event event : word.getEvents())
		     {
			Annotation anEvent = new Annotation();
			if (event.getType().equals("noise"))
			{
			   if (noiseLayer != null) anEvent.setLayerId(noiseLayer.getId());
			   anEvent.setParentId(graph.getId());
			}
			else if (event.getType().equals("lexical"))
			{
			   if (lexicalLayer != null) anEvent.setLayerId(lexicalLayer.getId());
			   anEvent.setParentId(anWord.getId());
			}
			else if (event.getType().equals("pronounce"))
			{
			   if (pronounceLayer != null) anEvent.setLayerId(pronounceLayer.getId());
			   anEvent.setParentId(anWord.getId());
			}
			else if (event.getType().equals("language"))
			{
			   if (languageLayer != null) anEvent.setLayerId(noiseLayer.getId());
			   anEvent.setParentId(anTurn.getId());
			}
			else if (event.getType().equals("entities"))
			{
			   if (entityLayer != null) anEvent.setLayerId(entityLayer.getId());
			   anEvent.setParentId(anTurn.getId());
			}
			else if (event.getType().equals("comment"))
			{
			   if (commentLayer != null) anEvent.setLayerId(commentLayer.getId());
			   anEvent.setParentId(anTurn.getId());
			}
			anEvent.setLabel(event.getDescription());
			if (anEvent.getLayerId() != null)
			{ // we have a layer for this type of event
			   if (event.getExtent().equals("begin"))
			   {
			      // set the start anchor
			      anEvent.setStart(anWord.getEnd());
			      // and save it until we find the end...
			      htStartedEvents.put(
				 event.getType() + ":" + event.getDescription(), anEvent);
			   }
			   else if (event.getExtent().equals("end"))
			   {
			      // ditch this anchor, use the one created at the
			      // beginning instead:
			      anEvent = htStartedEvents.remove(
				 event.getType() + ":" + event.getDescription());
			      if (anEvent != null)
			      {
				 anEvent.setEnd(anWord.getEnd());
				 graph.addAnnotation(anEvent);
			      }
			   }
			   else if (event.getExtent().equals("instantaneous"))
			   {
			      anEvent.setStart(anWord.getEnd());
			      anEvent.setEnd(anWord.getEnd());
			      graph.addAnnotation(anEvent);
			   }
			   else // wrap around the current word
			   {
			      anEvent.setStart(anWord.getStart());
			      anEvent.setEnd(anWord.getEnd());
			      graph.addAnnotation(anEvent);
			   }
			}
		     }
		  } // next word
		  
		  anLine.setEnd(
		     graph.getOrCreateAnchorAt(
			thisSync.getEndTimeAsDouble(), Constants.CONFIDENCE_AUTOMATIC));
		  
		  if (lastWord != null)
		  {
		     lastWord.setEnd(anLine.getEnd());
		  }
	       } // next sub-sync
	    } // next sync
	 } // next turn	 
      } // next section
      
      // check for leftover events
      if (htStartedEvents.size() != 0)
      {
	 Anchor lastAnchor = graph.getEnd();
	 for (Annotation anEvent : htStartedEvents.values())
	 {
	    anEvent.setEnd(lastAnchor);
	    graph.addAnnotation(anEvent);
	 } // next event
      }

      // TODO annotation.setAnnotator(...) for all annotations, from the tier's 'transcriber' setting.

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
    * prevent the input from being converted into an {@link AnnotationGraph}
    * when {@link #toAnnotationGraphs(LinkedHashMap)} is called.
    * <p>This implementation checks for simultaneous speaker turns that have the same speaker mentioned more than once, speakers that have the same name, and mismatched start/end events.
    * @return A list of errors, which will be empty if there were no validation errors.
    */
   public Vector<String> validate()
   {     
      warnings = new Vector<String>();
      // check there are speakers
      if (getSpeakers().size() == 0)
      {
	 warnings.addElement("Transcript contains  no speakers.");
      }
      
      // check for two speakers with the same name
      Vector<String> vSpeakers = new Vector<String>();
      for (Speaker speaker : getSpeakers())
      {
	 if (vSpeakers.contains(speaker.getName()))
	 {
	    warnings.addElement("Transcript contains more than one speaker called \"" + speaker.getName() +"\".");
	 }
	 else
	 {
	    vSpeakers.addElement(speaker.getName());
	 }
      } // next speaker
      
      // check for simultaneous speech turns where the speakers are the same
      // and for mismatched start/end events
      
      // for each turn
      HashMap<String,Event> htStartedEvents = new HashMap<String,Event>();
      for (Turn turn : getTurns())
      {
	 // for each sync
	 for (Sync sync : turn.getSyncs())
	 {
	    if (sync.isSimultaneousSpeech())
	    {
	       // check for speaker speaking at the same time as themselves
	       Vector<String> vSimSpeakers = new Vector<String>();
	       for (SimultaneousSync sim : sync.getSimultaneousSyncs())
	       {
		  if (vSimSpeakers.contains(sim.getWho()))
		  {
		     warnings.addElement(
			"Simultaneous speech at " + sync.getTime()
			+"s contains the same speaker more than once.");
		  }
		  else
		  {
		     vSimSpeakers.addElement(sim.getWho());
		  }
	       } // next sync
	       if (vSimSpeakers.size() < 2)
	       {
		  warnings.addElement(
		     "Simultaneous speech at " + sync.getTime()
		     +"s doesn't contain multiple speakers.");
	       }
	    } // simultaneous speech
	    else if (sync.getWho().indexOf(' ') >= 0)
	    {
	       warnings.addElement(
		  "Simultaneous speech at " + sync.getTime()
		  +"s doesn't contain multiple speakers.");
	    }
	    
	    // check events
	    for (Event event : sync.getEvents())
	    {		  
	       if (event.getExtent().equals("begin"))
	       {
		  // and save it until we find the end...
		  htStartedEvents.put(
		     event.getType() + ":" + event.getDescription(),
		     event);
	       }
	       else if (event.getExtent().equals("end"))
	       {
		  // does this have a beginning?
		  if (htStartedEvents.containsKey(
			 event.getType()
			 + ":" + event.getDescription()))
		  {
		     htStartedEvents.remove(
			event.getType()
			+ ":" + event.getDescription());
		  }
		  else
		  {
		     warnings.add(
			"End event after " + sync.getTime()
			+"s has no start tag: " + event);
		  }
	       }
	    }
	 } // next sync
      } // next turn
      
      // leftover start events?
      for(String sKey : htStartedEvents.keySet())
      {
	 Event event = htStartedEvents.get(sKey);
	 String sMessage = "Start event has no end tag: " + event;
	 if (event.getWord() != null)
	 {
	    sMessage = "Start event after " 
	       + event.getWord().getSync().getTime()
	       +"s has no end tag: " + event;
	 }
	 warnings.add(sMessage);
      } // next leftover start event

      // TODO check for turns with no speaker assigned.
      return warnings;
   }
   // ISerializer methods
   
   /**
    * Determines which layers, if any, must be present in the graph that will be serialized.
    * <p>{@link ISerializer} method.
    * @return A list of IDs of layers that must be present in the graph that will be serialized.
    * @throws SerializationParametersMissingException If not all required parameters have a value.
    */
   public String[] getRequiredLayers() throws SerializationParametersMissingException
   {
      Vector<String> requiredLayers = new Vector<String>();
      if (participantLayer != null)        requiredLayers.add(participantLayer.getId());
      if (turnLayer != null)               requiredLayers.add(turnLayer.getId());
      if (utteranceLayer != null)          requiredLayers.add(utteranceLayer.getId());
      if (wordLayer != null)               requiredLayers.add(wordLayer.getId());
      if (lexicalLayer != null)            requiredLayers.add(lexicalLayer.getId());
      if (pronounceLayer != null)          requiredLayers.add(pronounceLayer.getId());
      if (commentLayer != null)            requiredLayers.add(commentLayer.getId());
      if (noiseLayer != null)              requiredLayers.add(noiseLayer.getId());
      if (topicLayer != null)              requiredLayers.add(topicLayer.getId());
      if (languageLayer != null)           requiredLayers.add(languageLayer.getId());
      if (transcriptLanguageLayer != null) requiredLayers.add(transcriptLanguageLayer.getId());
      if (entityLayer != null)             requiredLayers.add(entityLayer.getId());
      if (scribeLayer != null)             requiredLayers.add(scribeLayer.getId());
      if (versionLayer != null)            requiredLayers.add(versionLayer.getId());
      if (versionDateLayer != null)        requiredLayers.add(versionDateLayer.getId());
      if (programLayer != null)            requiredLayers.add(programLayer.getId());
      if (airDateLayer != null)            requiredLayers.add(airDateLayer.getId());
      if (participantCheckLayer != null)   requiredLayers.add(participantCheckLayer.getId());
      if (genderLayer != null)             requiredLayers.add(genderLayer.getId());
      if (dialectLayer != null)            requiredLayers.add(dialectLayer.getId());
      if (accentLayer != null)             requiredLayers.add(accentLayer.getId());
      if (scopeLayer != null)              requiredLayers.add(scopeLayer.getId());
      return requiredLayers.toArray(new String[0]);
   }

   /**
    * Determines the cardinality between graphs and serialized streams.
    * @return {@link nzilbb.ag.serialize.ISerializer#Cardinality}.NtoN as there is one
    * stream produced for each graph to serialize.
    */
   public Cardinality getCardinality()
   {
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
   public void serialize(Spliterator<Graph> graphs, String[] layerIds, Consumer<NamedStream> consumer, Consumer<String> warnings, Consumer<SerializationException> errors) 
      throws SerializerNotConfiguredException
   {
      graphCount = graphs.getExactSizeIfKnown();
      graphs.forEachRemaining(graph -> {
            if (getCancelling()) return;
            try
            {
               consumer.accept(serializeGraph(graph, layerIds, warnings));
            }
            catch(SerializationException exception)
            {
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
   protected NamedStream serializeGraph(Graph graph, String[] layerIds, Consumer<String> warnings) 
      throws SerializationException
   {
      SerializationException errors = null;      

      HashSet<String> selectedLayers = new HashSet<String>();
      if (layerIds != null)
      {
         for (String l : layerIds) selectedLayers.add(l);
      }
      else
      {
         for (Layer l : graph.getSchema().getLayers().values()) selectedLayers.add(l.getId());
      }

      graph.setOffsetGranularity(Constants.GRANULARITY_MILLISECONDS);

      Transcript transcript = new Transcript();
      transcript.setId(graph.getId());
      transcript.setVersion("0");
      if (versionLayer != null)
      {
         Annotation tag = graph.my(versionLayer.getId());
         if (tag != null)
         {
            transcript.setVersion(tag.getLabel());
         }
      }
      SimpleDateFormat fmtTranscriberDate =new SimpleDateFormat("yymmdd");
      String sVersionDate = fmtTranscriberDate.format(new java.util.Date());
      transcript.setVersionDate(sVersionDate);
      if (versionDateLayer != null)
      {
         Annotation tag = graph.my(versionDateLayer.getId());
         if (tag != null)
         {
            transcript.setVersionDate(tag.getLabel());
         }
      }
      if (scribeLayer != null)
      {
         Annotation tag = graph.my(scribeLayer.getId());
         if (tag != null)
         {
            transcript.setScribe(tag.getLabel());
         }
      }
      if (transcript.getScribe() == null) transcript.setScribe(getDescriptor().toString());
      if (graph.getMediaProvider() != null)
      {
         try
         {
            MediaFile[] files = graph.getMediaProvider().getAvailableMedia();
            if (files.length > 0)
            {
               MediaFile firstFile = files[0];
               transcript.setAudioFilename(firstFile.getName());
            }
         }
         catch(StoreException exception) {}
         catch(PermissionException exception) {}
      }
      if (programLayer != null)
      {
         Annotation tag = graph.my(programLayer.getId());
         if (tag != null)
         {
            transcript.setProgram(tag.getLabel());
         }
      }
      if (transcriptLanguageLayer != null)
      {
         Annotation tag = graph.my(transcriptLanguageLayer.getId());
         if (tag != null)
         {
            transcript.setLanguage(tag.getLabel());
         }
      }

      // speakers
      int iSpk = 0;
      HashMap<String,String> mSpeakerToSpk = new HashMap<String,String>();
      for (Annotation participant : graph.list(participantLayer.getId()))
      {
	 Speaker speaker = new Speaker(transcript);
	 speaker.setId("spk" + (++iSpk));
	 speaker.setName(participant.getLabel());
	 transcript.addSpeaker(speaker);
	 mSpeakerToSpk.put(participant.getLabel(), speaker.getId());

         // participant attributes?
         if (genderLayer != null)
         {
            Annotation tag = participant.my(genderLayer.getId());
            if (tag != null)
            {
               speaker.setType(tag.getLabel());
            }
         }
         if (participantCheckLayer != null)
         {
            Annotation tag = participant.my(participantCheckLayer.getId());
            if (tag != null)
            {
               speaker.setCheck(tag.getLabel());
            }
         }
         if (scopeLayer != null)
         {
            Annotation tag = participant.my(scopeLayer.getId());
            if (tag != null)
            {
               speaker.setScope(tag.getLabel());
            }
         }
         if (accentLayer != null)
         {
            Annotation tag = participant.my(accentLayer.getId());
            if (tag != null)
            {
               speaker.setAccent(tag.getLabel());
            }
         }
         if (dialectLayer != null)
         {
            Annotation tag = participant.my(dialectLayer.getId());
            if (tag != null)
            {
               speaker.setDialect(tag.getLabel());
            }
         }
      }

      // ensure simultaneous speech is isolated into discrete turns
      try
      {
         new UtteranceParallelizer(schema).transform(graph);
      }
      catch(TransformationException exception)
      {
         errors = new SerializationException(exception);
      }

      // transcript...

      DecimalFormat fmt = new DecimalFormat(
	 // force the locale to something with . as the decimal separator
	 "0.000", new DecimalFormatSymbols(Locale.UK));

      HashMap<String, String> topicId = new HashMap<String, String>();

      Section section = null;

      Annotation currentTopic = null;
      HashSet<String> consumed = new HashSet<String>();
      for (Annotation anTurn : graph.list(turnLayer.getId()))
      {
	 // ignore it if we've already added it (because it's simultaneous)
	 if (consumed.contains(anTurn.getId())) continue;

	 // check for new topic
         if (topicLayer != null)
         {
            Annotation topic = anTurn.my(topicLayer.getId());
            if (currentTopic != topic)
            {
               currentTopic = topic;
               section = new Section(transcript);
               if (currentTopic != null)
               {
                  if (!topicId.containsKey(currentTopic.getLabel()))
                  {
                     topicId.put(currentTopic.getLabel(), "to" + (topicId.size() + 1));
                     transcript.addTopic(topicId.get(currentTopic.getLabel()), currentTopic.getLabel());
                  }
                  section.setTopic(topicId.get(currentTopic.getLabel()));
               } // there is a topic
               transcript.addSection(section);
            } // topic changing
         } // topic layer specified

	 if (section == null)
	 {
	    section = new Section(transcript);
	    transcript.addSection(section);
	 }
	 
	 Turn turn = new Turn(section);
	 turn.setStartTime(fmt.format(anTurn.getStart().getOffset()));
	 turn.setEndTime(fmt.format(anTurn.getEnd().getOffset()));
	 
	 LinkedHashSet<Annotation> turnsStartingHere = anTurn.getStart().startOf(anTurn.getLayerId());
	 int i = 1;
	 for (Annotation anThisTurn : turnsStartingHere)
	 {
	    if (turn.getSpeakerId().length() == 0)
	    {
	       turn.setSpeakerId(mSpeakerToSpk.get(anThisTurn.getLabel()));
	    }
	    else
	    { // more than one speaker
	       turn.setSpeakerId(turn.getSpeakerId() 
				 + " " + mSpeakerToSpk.get(anThisTurn.getLabel()));
	    }
	    consumed.add(anThisTurn.getId());
	 } // next turn

	 // utterances
	 for (Annotation anLine : anTurn.list(utteranceLayer.getId()))
	 {
	    // ignore it if we've already added it (because it's simultaneous)
	    if (consumed.contains(anLine.getId())) continue;
            
	    Sync sync = new Sync(turn);
	    sync.setTime(fmt.format(anLine.getStart().getOffset()));
	    sync.setEndTime(fmt.format(anLine.getEnd().getOffset()));

	    checkForNonWordEvents(anLine.getStart(), sync);

	    LinkedHashSet<Annotation> linesStartingHere = anLine.getStart().startOf(anLine.getLayerId());
	    for (Annotation anThisLine : linesStartingHere)
	    {
	       Sync thisSync = sync;
	       if (linesStartingHere.size() > 1)
	       { // simultaneous speech
		  thisSync = new SimultaneousSync(mSpeakerToSpk.get(anThisLine.getLabel()), sync);
		  sync.getSimultaneousSyncs().add((SimultaneousSync)thisSync);
	       }
	       // words 
	       thisSync.setText(""); // TODO handle events
	       for (Annotation anWord : anThisLine.list(wordLayer.getId()))
	       {
		  checkForNonWordEvents(anWord.getStart(), thisSync);
		  if (thisSync.getText().length() > 0)
		  {
		     thisSync.setText(thisSync.getText() + " " + anWord.getLabel());
		  }
		  else
		  {
		     thisSync.setText(anWord.getLabel());
		  }
		  if (anWord.my(lexicalLayer.getId()) != null)
		  { // lexical tag
		     thisSync.addEvent(new Event(
					  anWord.my(lexicalLayer.getId()).getLabel(),
					  "lexical", "previous"));
		  }
		  if (anWord.my(pronounceLayer.getId()) != null)
		  { // lexical tag
		     thisSync.addEvent(new Event(
					  anWord.my(pronounceLayer.getId()).getLabel(),
					  "pronounce", "previous"));
		  }
		  checkForNonWordEvents(anWord.getEnd(), thisSync);
	       } // next word
	       
	       consumed.add(anThisLine.getId());
	    } // next line

	    checkForNonWordEvents(anLine.getEnd(), sync);
	    
	    turn.addSync(sync);
	 } // next line
	 
	 section.addTurn(turn);
      } // next turn

      // remove tags used during serialization
      for (Annotation a : graph.getAnnotationsById().values())
      {
         a.remove("@begun");
         a.remove("@ended");
      } // next annotation

      if (errors != null) throw errors;
      
      try
      {
         File f = File.createTempFile(IO.SafeFileNameUrl(graph.getId()), ".trs");
         FileOutputStream out = new FileOutputStream(f);	 
         OutputStreamWriter writer = new OutputStreamWriter(out, "utf-8");
         transcript.writeText(writer);
         writer.flush();
         TempFileInputStream in = new TempFileInputStream(f);

         // return a named stream from the file
         String name = IO.SafeFileNameUrl(graph.getId());
         if (!name.toLowerCase().endsWith(".trs")) name += ".trs";
         return new NamedStream(in, name);
      }
      catch(Exception exception)
      {
         errors = new SerializationException();
         errors.initCause(exception);
         errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
         throw errors;
      }
   }
   
   /**
    * Checks the givn anchor for non-word events, e.g. language start or end tags.
    * @param anchor
    * @param sync
    */
   public void checkForNonWordEvents(Anchor anchor, Sync sync)
   {
      // ending language tags
      if (languageLayer != null)
      {
         for (Annotation an : anchor.endOf(languageLayer.getId()))
         {
            if (an.containsKey("@ended")) continue;
            sync.addEvent(new Event(an.getLabel(), "language", "end"));
            an.put("@ended", Boolean.TRUE);
         }
      }
      // ending entity tags
      if (entityLayer != null)
      {
         for (Annotation an : anchor.endOf(entityLayer.getId()))
         {
            if (an.containsKey("@ended")) continue;
            sync.addEvent(new Event(an.getLabel(), "entities", "end"));
            an.put("@ended", Boolean.TRUE);
         }
      }
      
      // noise tags
      if (noiseLayer != null)
      {
         for (Annotation an : anchor.endOf(noiseLayer.getId()))
         {
            if (an.containsKey("@ended")) continue;
            sync.addEvent(new Event(an.getLabel(), "noise", "instantaneous"));
            an.put("@ended", Boolean.TRUE);
         }
      }
      
      // comments tags
      if (commentLayer != null)
      {
         for (Annotation an : anchor.endOf(commentLayer.getId()))
         {
            if (an.containsKey("@ended")) continue;
            sync.addEvent(new Comment(an.getLabel()));
            an.put("@ended", Boolean.TRUE);
         }
      }
      
      // beginning entity tags
      if (entityLayer != null)
      {
         for (Annotation an : anchor.startOf(entityLayer.getId()))
         {
            if (an.containsKey("@begun")) continue;
            sync.addEvent(new Event(an.getLabel(), "entities", "begin"));
            an.put("@begun", Boolean.TRUE);
         }
      }
      // beginning language tags
      if (languageLayer != null)
      {
         for (Annotation an : anchor.startOf(languageLayer.getId()))
         {
            if (an.containsKey("@begun")) continue;
            sync.addEvent(new Event(an.getLabel(), "language", "begin"));
            an.put("@begun", Boolean.TRUE);
         }
      }
   } // end of checkForNonWordEvents()
   
} // end of class TranscriptSerialization
