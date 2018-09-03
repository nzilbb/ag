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
package nzilbb.elan;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import nzilbb.ag.*;
import nzilbb.ag.util.SimpleTokenizer;
import nzilbb.ag.util.ConventionTransformer;
import nzilbb.ag.util.SpanningConventionTransformer;
import nzilbb.ag.util.OrthographyClumper;
import nzilbb.ag.util.AnnotationComparatorByAnchor;
import nzilbb.ag.serialize.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;

/**
 * Converter that converts ELAN EAF v2.7 files to Annotation Graphs
 * <p>The original XSD is here:
 * <a href="http://www.mpi.nl/tools/elan/EAFv2.7.xsd">http://www.mpi.nl/tools/elan/EAFv2.7.xsd</a>
 * @author Robert Fromont robert@fromont.net.nz
 */
public class EAFDeserializer
   implements IDeserializer
{
   // Attributes:     
   protected Vector<String> warnings;
   /**
    * Returns any warnings that may have arisen during the last execution of {@link #deserialize()}.
    * @return A possibly empty list of warnings.
    */
   public String[] getWarnings()
   {
      return warnings.toArray(new String[0]);
   }

   /** ANNOTATION_DOCUMENT root node */
   protected Node root;
   
   /** HEADER node */
   protected Node header;
   
   /** TIME_ORDER node */
   protected Node timeOrder;
   
   /** tiers */
   protected NodeList tiers;
   
   /** XPATH processor */
   protected XPath xpath;
   
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
   public void setTierMessages(HashMap<String,String> mNewTierMessages) { mTierMessages = mNewTierMessages; }


   /**
    * Whether 'utterance' intervals (those which map to either {@link Labbcat#LAYER_TURN} or {@link Labbcat#LAYER_UTTERANCE}) contain the name of the speaker (true) or the words spoken (false - in which case the tier name is assumed to be the speaker name)
    * @see #getUtterancesAreSpeakerNames()
    * @see #setUtterancesAreSpeakerNames(boolean)
    */
   protected boolean utterancesAreSpeakerNames = false;
   /**
    * Getter for {@link #utterancesAreSpeakerNames}: Whether 'utterance' intervals (those which map to either {@link Labbcat#LAYER_TURN} or {@link Labbcat#LAYER_UTTERANCE}) contain the name of the speaker (true) or the words spoken (false - in which case the tier name is assumed to be the speaker name)
    * @return Whether 'utterance' intervals (those which map to either {@link Labbcat#LAYER_TURN} or {@link Labbcat#LAYER_UTTERANCE}) contain the name of the speaker (true) or the words spoken (false - in which case the tier name is assumed to be the speaker name)
    */
   public boolean getUtterancesAreSpeakerNames() { return utterancesAreSpeakerNames; }
   /**
    * Setter for {@link #utterancesAreSpeakerNames}: Whether 'utterance' intervals (those which map to either {@link Labbcat#LAYER_TURN} or {@link Labbcat#LAYER_UTTERANCE}) contain the name of the speaker (true) or the words spoken (false - in which case the tier name is assumed to be the speaker name)
    * @param bNewUtterancesAreSpeakerNames Whether 'utterance' intervals (those which map to either {@link Labbcat#LAYER_TURN} or {@link Labbcat#LAYER_UTTERANCE}) contain the name of the speaker (true) or the words spoken (false - in which case the tier name is assumed to be the speaker name)
    */
   public void setUtterancesAreSpeakerNames(boolean newUtterancesAreSpeakerNames) { utterancesAreSpeakerNames = newUtterancesAreSpeakerNames; }
   
   
   /**
    * Map of tier names to tiers.
    * @see #getTiers()
    * @see #setTiers(LinkedHashMap)
    */
   protected LinkedHashMap<String,Node> mTiers = new LinkedHashMap<String,Node>();
   /**
    * Getter for {@link #mTiers}: Map of tier names to tiers.
    * @return Map of tier names to tiers.
    */
   public LinkedHashMap<String,Node> getTiers() { return mTiers; }
   /**
    * Setter for {@link #mTiers}: Map of tier names to tiers.
    * @param mNewTiers Map of tier names to tiers.
    */
   public void setTiers(LinkedHashMap<String,Node> mNewTiers) { mTiers = mNewTiers; }
   

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
   public void setNoiseLayer(Layer newNoiseLayer) { noiseLayer = newNoiseLayer; }

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
   public void setAuthorLayer(Layer newAuthorLayer) { authorLayer = newAuthorLayer; }

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
   public void setDateLayer(Layer newDateLayer) { dateLayer = newDateLayer; }

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
   public void setLanguageLayer(Layer newLanguageLayer) { languageLayer = newLanguageLayer; }

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
   public void setUseConventions(Boolean bNewUseConventions) { bUseConventions = bNewUseConventions; }

   
   /**
    * Minimum amount of time between two turns by the same speaker, with no intervening speaker, for which the inter-turn pause counts as a turn change boundary. If the pause is shorter than this, the turns are merged into one. Default is 0.0;
    * @see #getMinimumTurnPauseLength()
    * @see #setMinimumTurnPauseLength(Double)
    */
   protected Double minimumTurnPauseLength;
   /**
    * Getter for {@link #minimumTurnPauseLength}: Minimum amount of time between two turns by the same speaker, with no intervening speaker, for which the inter-turn pause counts as a turn change boundary. If the pause is shorter than this, the turns are merged into one.
    * @return Minimum amount of time between two turns by the same speaker, with no intervening speaker, for which the inter-turn pause counts as a turn change boundary. If the pause is shorter than this, the turns are merged into one.
    */
   public Double getMinimumTurnPauseLength()
   {
      if (minimumTurnPauseLength == null) minimumTurnPauseLength = new Double(0.0);
      return minimumTurnPauseLength;
   }
   /**
    * Setter for {@link #minimumTurnPauseLength}: Minimum amount of time between two turns by the same speaker, with no intervening speaker, for which the inter-turn pause counts as a turn change boundary. If the pause is shorter than this, the turns are merged into one.
    * @param newMinimumTurnPauseLength Minimum amount of time between two turns by the same speaker, with no intervening speaker, for which the inter-turn pause counts as a turn change boundary. If the pause is shorter than this, the turns are merged into one.
    */
   public void setMinimumTurnPauseLength(Double newMinimumTurnPauseLength) { minimumTurnPauseLength = newMinimumTurnPauseLength; }

   
   // IStreamDeserializer methods:
   
   /**
    * Returns the deserializer's descriptor
    * @return The deserializer's descriptor
    */
   public SerializationDescriptor getDescriptor()
   {
      return new SerializationDescriptor(
	 "ELAN EAF Transcript", "0.4", "text/x-eaf+xml", ".eaf", "20170314.1631", getClass().getResource("icon.png"));
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
   public void setId(String newId) { id = newId; }

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
    * Constructor
    */
   public EAFDeserializer()
   {
   } // end of constructor
   
   /**
    * Resets the state of the converter, ready to convert again.
    */
   public void reset()
   {
      root = null;
      header = null;
      timeOrder = null;
      tiers = null;
      xpath = null;
      warnings = new Vector<String>();
      timeFactor = (1.0/1000.0);
      utterancesAreSpeakerNames = false;
      mTiers.clear();
      id = null;
      mTierMessages.clear();
   } // end of reset()

   
   // IDeserializer methods

   protected ParameterSet mappings;

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
      if (getParticipantLayer() == null || getTurnLayer() == null 
	  || getUtteranceLayer() == null || getWordLayer() == null)
      {
	 for (Layer top : schema.getRoot().getChildren().values())
	 {
	    if (top.getAlignment() == Constants.ALIGNMENT_NONE)
	    {
	       if (top.getChildren().size() != 0)
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
      }
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

      layerToPossibilities.put(
	 new Parameter("languageLayer", Layer.class, "Transcript Language layer", 
		       "The language of the whole transcript"), 
	 Arrays.asList("transcriptlanguage","language","transcriptlang","lang"));
      layerToCandidates.put("languageLayer", graphTagLayers);

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

      if (!configuration.containsKey("minimumTurnPauseLength"))
      {
	 configuration.addParameter(
	    new Parameter("minimumTurnPauseLength", Double.class, 
			  "Min. Turn Pause Length",
			  "Minimum amount of time between two turns by the same speaker, with no intervening speaker, for which the inter-turn pause counts as a turn change boundary. If the pause is shorter than this, the turns are merged into one.", true));
      }
      if (configuration.get("minimumTurnPauseLength").getValue() == null)
      {
	 configuration.get("minimumTurnPauseLength").setValue(new Double(0.0));
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
      setSchema(schema);

      // take the first stream, ignore all others.
      NamedStream eaf = Utility.FindSingleStream(streams, ".eaf", "text/x-eaf+xml");
      if (eaf == null) throw new SerializationException("No ELAN EAF stream found");
      setId(eaf.getName());
      
      // convert from UTF-16 to UTF-8 if necessary
      InputStream in = eaf.getStream();

      // Document factory
      DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
      try
      {
	  DocumentBuilder builder = builderFactory.newDocumentBuilder();   
	  builder.setEntityResolver(new EntityResolver() {
		public InputSource resolveEntity(String publicId, String systemId)
		   throws SAXException, IOException {
		   // Get DTDs locally, to prevent not found errors
		   String name = systemId.substring(systemId.lastIndexOf('/') + 1);
		   URL url = getClass().getResource(name);
		   if (url != null) return new InputSource(url.openStream());
		   return null;
		}});
	  
	  Document document = builder.parse(new InputSource(in));
	  
	  root = document.getFirstChild();
	  if (!root.getNodeName().equalsIgnoreCase("ANNOTATION_DOCUMENT"))
	  {
	     throw new SerializationException(SerializationException.ErrorType.InvalidDocument,
					      "XML top node is not ANNOTATION_DOCUMENT");
	  }
	  XPathFactory xpathFactory = XPathFactory.newInstance();
	  xpath = xpathFactory.newXPath();
	  header = (Node) xpath.evaluate("//HEADER", document, XPathConstants.NODE);
	  timeOrder = (Node) xpath.evaluate("//TIME_ORDER", document, XPathConstants.NODE);
	  tiers = (NodeList) xpath.evaluate("//TIER", root, XPathConstants.NODESET);
	  
	  if (timeOrder == null)
	  {
	     throw new SerializationException(SerializationException.ErrorType.InvalidDocument,
					      "Document has no TIME_ORDER node");
	  }
	  if (tiers == null || tiers.getLength() == 0)
	  {
	     throw new SerializationException(SerializationException.ErrorType.InvalidDocument,
					      "Document has no TIER nodes");
	  }
	  
	  ParameterSet mappings = new ParameterSet();
	  Vector<Layer> vIntervalLayers = new Vector<Layer>();
	  for (Layer layer : getSchema().getLayers().values())
	  {
	     if (!layer.getId().equals(getSchema().getRoot().getId())
		 && !layer.getId().equals(getParticipantLayer().getId()))
	     {
		switch (layer.getAlignment())
		{
		   case 0: 
		      if (!layer.getParentId().equals(getSchema().getRoot().getId())
			  && !layer.getParentId().equals(getParticipantLayer().getId()))
		      { // not graph or participant tags
			 vIntervalLayers.add(layer); 
		      }
		      break; 
		   case 2: vIntervalLayers.add(layer); break; 
		   case 1: break; // point tiers are not supported in ELAN
		}
	     }
	  } // next layer
	  
	  // map tiers to layers by name
	  Layer ignore = new Layer();
	  ignore.setId("[ignore tier]");      
	  for (int t = 0; t < tiers.getLength(); t++)
	  {
	     Node tier = tiers.item(t);
	     Attr tierId = (Attr)tier.getAttributes().getNamedItem("TIER_ID");
	     String tierName = tierId.getValue();
	     Attr childParticipant = (Attr)tier.getAttributes().getNamedItem("PARTICIPANT");
	     
	     Parameter p = new Parameter(
		"tier"+t, Layer.class, tierName,
		"Layer for tier called: " + tierName, true);
	     Vector<Layer> vPossiblLayers = new Vector<Layer>();
	     vPossiblLayers.add(ignore);
	     vPossiblLayers.addAll(vIntervalLayers);
	     
	     // look for a layer with the same name
	     if (tierName.equalsIgnoreCase("lines")
		 || tierName.equalsIgnoreCase("utterances"))
	     {
		tierName = getUtteranceLayer().getId();
	     }
	     else if (tierName.equalsIgnoreCase("speakers")
		      || tierName.equalsIgnoreCase("speaker")
		      || tierName.equalsIgnoreCase("turns")
		      || tierName.equalsIgnoreCase("turn"))
	     {
		tierName = getTurnLayer().getId();
	     }
	     else if (tierName.toLowerCase().startsWith("word")
		      || tierName.toLowerCase().endsWith("word")
		      || tierName.toLowerCase().startsWith("words")
		      || tierName.toLowerCase().endsWith("words"))
	     {
		tierName = getWordLayer().getId();
	     }
	     Layer layer = getSchema().getLayer(tierName);
	     if (layer == null)
	     { // no exact match
		// try a case-insensitive match
		// ignore spaces too
		String tierNameNoWhitespace = tierName.toLowerCase().replaceAll("\\s","");
		for (Layer mappableLayer : vPossiblLayers)
		{
		   if (tierNameNoWhitespace.equals(mappableLayer.getId().toLowerCase().replaceAll("\\s","")))
		   {
		      layer = mappableLayer;
		      break;
		   }
		} // next layer
	     }
	     if (layer == null)
	     { // no exact match
		// try a prefix-match - i.e. "transcript - John Smith" should map to the "transcript" layer
		// ignore spaces too
		String tierNameNoWhitespace = tierName.replaceAll("\\s","");
		for (Layer mappableLayer : vPossiblLayers)
		{
		   if (tierNameNoWhitespace.startsWith(mappableLayer.getId().replaceAll("\\s","")))
		   {
		      layer = mappableLayer;
		      break;
		   }
		} // next layer
	     }
	     if (layer != null)
	     { // there is a matching layer
		if (layer.getAlignment() != 1) p.setValue(layer);
	     }
	     else
	     { // no name match, assume it's a tier named after a speaker
		// make the utteranceLayer the default
		p.setValue(getUtteranceLayer());
	     }
	     p.setPossibleValues(vPossiblLayers);
	     mappings.addParameter(p);
	  } // next tier
	  
	  return mappings;
	  
      }      
      catch(ParserConfigurationException x) { throw new SerializationException(x); }
      catch(SAXException x) { throw new SerializationException(x); }
      catch(XPathExpressionException x) { throw new SerializationException(x); }
   }

   /**
    * Sets parameters for a given deserialization operation, after loading the serialized form of the graph. This might include mappings from format-specific objects like tiers to graph layers, etc.
    * @param parameters The configuration for a given deserialization operation.
    * @throws SerializationParametersMissingException If not all required parameters have a value.
    */
   public void setParameters(ParameterSet parameters) throws SerializationParametersMissingException
   {
      mappings = parameters;
      
      // check we've got enough mappings for turns and words
      // i.e. at least two of turn/utterance/transcription
      int iTurnLayerMapped = 0;
      int iUtteranceLayerMapped = 0;
      int iWordLayerMapped = 0;
      for (int t = 0; t < tiers.getLength(); t++)
      {
	 Node tier = tiers.item(t);
	 // is there a mapping for this tier?
	 Layer layer = (Layer)mappings.get("tier"+t).getValue();
	 if (layer != null)
	 {
	    if (layer.equals(getTurnLayer()))
	    {
	       iTurnLayerMapped++;
	    }
	    else if (layer.equals(getUtteranceLayer()))
	    {
	       iUtteranceLayerMapped++;
	    }
	    else if (layer.equals(getWordLayer()))
	    {
	       iWordLayerMapped++;
	    }
	 }
      }
      if (iTurnLayerMapped + iUtteranceLayerMapped + iWordLayerMapped == 0)
      {
	 throw new SerializationParametersMissingException(
	    "There are no turn, utterance, or word mappings");
      }
      String sTimeUnits = "milliseconds";
      try
      {
	 sTimeUnits = ((Attr)header.getAttributes().getNamedItem("TIME_UNITS")).getValue();
      }
      catch(Throwable exception)
      {}
      
      if (sTimeUnits.equalsIgnoreCase("milliseconds"))
      {
	 timeFactor = (1.0/1000.0);
      }
      else if (sTimeUnits.equalsIgnoreCase("NTSC-frames"))
      {
	 timeFactor = (1.0/30.0);
      }
      else if (sTimeUnits.equalsIgnoreCase("PAL-frames"))
      {
	 timeFactor = (1.0/25.0);
      }
      else
      {
	 warnings.add("Unkown TIME_UNITS: " + sTimeUnits);
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
      if (participantLayer == null) throw new SerializerNotConfiguredException("Participant layer not set");
      if (turnLayer == null) throw new SerializerNotConfiguredException("Turn layer not set");
      if (utteranceLayer == null) throw new SerializerNotConfiguredException("Utterance layer not set");
      if (wordLayer == null) throw new SerializerNotConfiguredException("Word layer not set");
      if (schema == null) throw new SerializerNotConfiguredException("Layer schema not set");

      // if there are errors, accumulate as many as we can before throwing SerializationException
      SerializationException errors = null;

      warnings = new Vector<String>();

      Graph graph = new Graph();
      graph.setId(getId());
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
      graph.addLayer((Layer)wordLayer.clone());
      graph.getSchema().setWordLayerId(wordLayer.getId());
      if (authorLayer != null) graph.addLayer((Layer)authorLayer.clone());
      if (dateLayer != null) graph.addLayer((Layer)dateLayer.clone());
      if (languageLayer != null) graph.addLayer((Layer)languageLayer.clone());

      graph.setOffsetUnits(Constants.UNIT_SECONDS);
      graph.setOffsetGranularity(timeFactor);

      // attributes
      try
      {
	 String sResult = xpath.evaluate("/ANNOTATION_DOCUMENT/@AUTHOR", root);
	 if (sResult != null && sResult.length() > 0 && authorLayer != null)
	 {
	    graph.createTag(graph, authorLayer.getId(), sResult);
	 }
	 sResult = xpath.evaluate("/ANNOTATION_DOCUMENT/@DATE", root);
	 if (sResult != null && sResult.length() > 0 && dateLayer != null)
	 {
	    graph.createTag(graph, dateLayer.getId(), sResult);
	 }
	 sResult = xpath.evaluate("/ANNOTATION_DOCUMENT/TIER/@LANG_REF", root);
	 if (sResult == null || sResult.length() == 0)
	 { // for backward compatibility
	    sResult = xpath.evaluate("/ANNOTATION_DOCUMENT/TIER/@DEFAULT_LOCALE", root);
	 }
	 if (sResult != null && sResult.length() > 0 && languageLayer != null)
	 {
	    graph.createTag(graph, languageLayer.getId(), sResult);
	 }	 
      }
      catch(XPathExpressionException x)
      {
	 warnings.add("Error determining document attributes: " + x);
      }
      
      // first of all, create Anchors from TIME_SLOTs
      HashMap<String,Anchor> mTimeslotIdToAnchor = new HashMap<String,Anchor>();
      try
      {
	 NodeList timeslots = (NodeList) xpath.evaluate("./TIME_SLOT", timeOrder, XPathConstants.NODESET);
	 for (int t = 0; t < timeslots.getLength(); t++)
	 {
	    Node timeslot = timeslots.item(t);
	    String sTimeSlotId = ((Attr)timeslot.getAttributes().getNamedItem("TIME_SLOT_ID")).getValue();
	    
	    try
	    {
	       String sTimeValue = ((Attr)timeslot.getAttributes().getNamedItem("TIME_VALUE")).getValue();
	       mTimeslotIdToAnchor.put(sTimeSlotId, new Anchor(
					  sTimeSlotId,
					  new Double(timeFactor * Double.parseDouble(sTimeValue)),
					  Constants.CONFIDENCE_MANUAL));
	    }
	    catch(NullPointerException exception)
	    {
	       mTimeslotIdToAnchor.put(sTimeSlotId, new Anchor(sTimeSlotId, null));
	    }
	    // don't add them to the graph yet - we'll do that as we go...
	 } // next timeslot
      }
      catch(XPathExpressionException x)
      {
	 throw new SerializationException("Error finding TIME_SLOT tags: " + x);
      }
	 

      // this map keys by the ELAN ANNOTATION_ID
      HashMap<String,Annotation> mAnnotationIdToAnnotation = new HashMap<String,Annotation>();

      boolean turnLayerMapped = false;
      boolean utteranceLayerMapped = false;
      boolean wordLayerMapped = false;
      for (int t = 0; t < tiers.getLength(); t++)
      {
	 Node tier = tiers.item(t);
	 // is there a mapping for this tier?
	 Layer layer = (Layer)mappings.get("tier"+t).getValue();
	 if (layer != null && !layer.getId().equals("[ignore tier]"))
	 {
	    if (layer.getId().equals(getTurnLayer().getId()))
	    {
	       turnLayerMapped = true;
	    }
	    else if (layer.getId().equals(getUtteranceLayer().getId()))
	    {
	       utteranceLayerMapped = true;
	    }
	    else if (layer.getId().equals(getWordLayer().getId()))
	    {
	       wordLayerMapped = true;
	    }
	    
	    if (graph.getLayer(layer.getId()) == null)
	    {
	       graph.addLayer((Layer)layer.clone());
	    }
	    
	 } // tier is mapped to a layer
      } // next tier

      if (!wordLayerMapped) 
      {
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
	 )
      {
	 setUtterancesAreSpeakerNames(true);
      }

      int iLastWordOrdinal = 0;
      
      // turn tiers of annotations into layers of annotations
      for (int t = 0; t < tiers.getLength(); t++)
      {
	 Node tier = tiers.item(t);
	 // is there a mapping for this tier?
	 Layer layer = (Layer)mappings.get("tier"+t).getValue();
	 if (layer != null && !layer.getId().equals("[ignore tier]"))
	 {
	    String sTierId = ((Attr)tier.getAttributes().getNamedItem("TIER_ID")).getValue();
	    String sSpeakerName = sTierId;
	    Attr participant = (Attr)tier.getAttributes().getNamedItem("PARTICIPANT");
	    if (participant != null) sSpeakerName = participant.getValue();

	    // process annotations
	    try
	    {
	       NodeList annotations = (NodeList) xpath.evaluate("ANNOTATION/REF_ANNOTATION", tier, XPathConstants.NODESET);
	       if (annotations != null && annotations.getLength() > 0) 
	       { // reference annotations TODO
	       } // reference annotations
	       else // alignable annotations
	       {
		  annotations = (NodeList) xpath.evaluate("ANNOTATION/ALIGNABLE_ANNOTATION", tier, XPathConstants.NODESET);
		  for (int a = 0; a < annotations.getLength(); a++)
		  {
		     Node annotationNode = annotations.item(a);
		     String sAnnotationId = ((Attr)annotationNode.getAttributes().getNamedItem("ANNOTATION_ID")).getValue();
		     String sTimeSlotRef1 = ((Attr)annotationNode.getAttributes().getNamedItem("TIME_SLOT_REF1")).getValue();
		     String sTimeSlotRef2 = ((Attr)annotationNode.getAttributes().getNamedItem("TIME_SLOT_REF2")).getValue();
		     String sAnnotationValue = (String)xpath.evaluate("ANNOTATION_VALUE/text()", annotationNode, XPathConstants.STRING);
		     // ignore empty intervals...
		     if (sAnnotationValue.trim().length() == 0) continue; 

		     Anchor start = mTimeslotIdToAnchor.get(sTimeSlotRef1);
		     Anchor end = mTimeslotIdToAnchor.get(sTimeSlotRef2);
		     Annotation annotation = new Annotation(sAnnotationId, sAnnotationValue, layer.getId(), start.getId(), end.getId());
		     annotation.put("@tierId", sTierId); // this might come in handy later
		     annotation.put("@participant", sSpeakerName); // this might come in handy later
		     mAnnotationIdToAnnotation.put(sAnnotationId, annotation);
		     annotation.setConfidence(Constants.CONFIDENCE_MANUAL);
		     // TODO annotation.setAnnotator(...), from the tier's settings.
		     // add anchors if they're not in the graph
		     if (!graph.getAnchors().containsKey(start.getId())) graph.addAnchor(start);
		     if (!graph.getAnchors().containsKey(end.getId())) graph.addAnchor(end);
		     // add annotation
		     graph.addAnnotation(annotation);
		  } // next annotation
	       } // alignable annotations
	       mTierMessages.put(
		  sTierId, "" + annotations.getLength() + " annotation" 
		  + (annotations.getLength()==1?"":"s") + " added to graph.");
	    }
	    catch (Throwable t2)
	    {
	       mTierMessages.put(sTierId, t2.getMessage());
	       if (errors == null) errors = new SerializationException(t2);
	       if (errors.getCause() == null) errors.initCause(t2);
	       errors.addError(SerializationException.ErrorType.Other, t2.getMessage());
	    }
	 } // layer is mapped
      } // next tier

      Anchor graphEnd = graph.getEnd();

      // ensure both turns and utterances exist, and parents are set
      if (wordLayerMapped && !turnLayerMapped && !utteranceLayerMapped)
      { // create utterances and turns from words	 
	 // given there are no utterance/turn intervals, 
	 // we assume that the tier name for words is the speaker name	 
	 HashMap<String,Annotation> turnsByName = new HashMap<String,Annotation>();
	 for (Annotation word : graph.list(wordLayer.getId()))
	 {
	    String participant = (String)word.get("@participant");
	    Annotation turn = turnsByName.get(participant);
	    if (turn == null)
	    {
	       // create turn 
	       turn = new Annotation(
		  null, participant, turnLayer.getId(), graphStart.getId(), graphEnd.getId());
	       graph.addAnnotation(turn);
	       turnsByName.put(participant, turn);

	       // create utterance
	       Annotation utterance = new Annotation(turn);
	       utterance.setLayerId(utteranceLayer.getId());
	       utterance.setParentId(turn.getId());
	       graph.addAnnotation(utterance);
	    }
	    // set parent of word
	    word.setParent(turn);
	 } // next turn
      } 
      else if (turnLayerMapped && !utteranceLayerMapped)
      { // create utterances from turns
	 for (Annotation turn : graph.list(turnLayer.getId()))
	 {
	    Annotation utterance = new Annotation(turn);
	    utterance.setLayerId(utteranceLayer.getId());
	    utterance.setParentId(turn.getId());
	    if (!wordLayerMapped) // no word layer
	    { // ...which means the label must be the untokenized words
	       // and so the turn's @participant must be the speaker name
	       turn.setLabel((String)turn.get("@participant"));
	    }
	    graph.addAnnotation(utterance);
	 } // next turn
      }
      else if (utteranceLayerMapped && !turnLayerMapped)
      { // create turns from utterances
	 for (Annotation utterance : graph.list(utteranceLayer.getId()))
	 {
	    Annotation turn = new Annotation(utterance);
	    turn.setLayerId(turnLayer.getId());
	    // the utterance's @participant is taken to be the speaker name
	    turn.setLabel((String)utterance.get("@participant"));
	    graph.addAnnotation(turn);
	    // now turn will have an ID, and we can set it to be the parent of utterance
	    utterance.setParent(turn);
	 } // next utterance
      }
      else if (utteranceLayerMapped && turnLayerMapped)
      {
	 // ensure utterance parent turns are set
	 for (Annotation utterance : graph.list(utteranceLayer.getId()))
	 {
	    Annotation[] possibleTurns = utterance.includingAnnotationsOn(turnLayer.getId());
	    if (possibleTurns.length == 1)
	    { // must be this one
	       utterance.setParent(possibleTurns[0]);
	    }
	    else if (possibleTurns.length > 1)
	    { // multiple possible turns
	       // use the turn whose label is included in the utterance's tier name
	       // e.g. the turn might be "John Smith" and the utterance tier might be "utterance - John Smith"
	       String utteranceTier = (String)utterance.get("@tierId");
	       String utteranceParticipant = (String)utterance.get("@participant");
	       Annotation turn = null;
	       for (Annotation possibleTurn : possibleTurns)
	       {
		  // is the label (the speaker) a part of the utterance's tier name?
		  if (utteranceTier.indexOf(possibleTurn.getLabel()) >= 0
		      || utteranceParticipant.equals(possibleTurn.getLabel()))
		  {
		     // multiple parents could match 
		     // e.g. "sp1" and "Interviewer sp1" both are suffixes of "utterance - Interviewer sp1"
		     // so we go with the longest one
		     if (turn == null
			 || possibleTurn.getLabel().length() > turn.getLabel().length())
		     {
			turn = possibleTurn;
		     } // longest match so far
		  } // label is a part of the tier name
	       } // next possible turn
	       if (turn != null) utterance.setParent(turn);
	    } // multiple possible turns
	 } // next utterance
      } // both utterance and turn layers mapped

      // now we have turns with participant name labels, 
      // and utterances with parents, that maybe need tokenizing

      // ensure participants are set
      HashMap<String,Annotation> participantsByName = new HashMap<String,Annotation>();
      int ordinal = 1;
      for (Annotation turn : graph.list(turnLayer.getId()))
      {
	 if (!participantsByName.containsKey(turn.getLabel()))
	 { // create participant
	    Annotation who = new Annotation(null, turn.getLabel(), participantLayer.getId());
	    graph.addAnnotation(who);
	    participantsByName.put(turn.getLabel(), who);
	 }
	 // set ordinal first, then don't allow appending in setParent, to save a performance hit
	 turn.setOrdinal(participantsByName.get(turn.getLabel()).getAnnotations(turnLayer.getId()).size() + 1);
	 turn.setParent(participantsByName.get(turn.getLabel()), false);
     } // next turn

      // join subsequent turns by the same speaker...
      // for each participant (assumed to be parent of turn)
      for (Annotation participant : graph.list(participantLayer.getId()))
      {
	 TreeSet<Annotation> annotations = new TreeSet<Annotation>(new AnnotationComparatorByAnchor());
	 annotations.addAll(participant.getAnnotations(turnLayer.getId()));
	 Annotation[] turns = annotations.toArray(new Annotation[0]);
	 // go back through all the turns, looking for a turn for the same speaker that is
	 // joined to, or overlaps, this one
	 for (int i = turns.length - 2; i >= 0; i--)
	 {
	    Annotation preceding = turns[i];
	    Annotation following = turns[i + 1];
	    boolean mergeTurns = false;
	    if (preceding.getEnd().getOffset() != null
		&& following.getStart().getOffset() != null)
	    {
	       if (preceding.getEnd().getOffset() >= following.getStart().getOffset())
	       {
		  mergeTurns = true;
	       }
	       else if (getMinimumTurnPauseLength() > 0
			&& preceding.getEnd().getOffset() + getMinimumTurnPauseLength() >= following.getStart().getOffset())
	       { // there is a short enough pause between two turns of the same participant
		  // but there also must be no intervening speakers
		  if (graph.overlappingAnnotations(
			 preceding.getEnd(), following.getStart(), turnLayer.getId())
		      .length == 0)
		  {
		     mergeTurns = true;
		  }
	       }
	    }
	    if (mergeTurns)
	    {
	       mergeTurns(preceding, following);
	       following.destroy();
	    }
	 } // next preceding turn
      } // next turn parent
      graph.commit();
      
      // now we have participants,
      // and rationalized turns with participant name labels and parents, 
      // and utterances with parents, that maybe need tokenizing

      if (!wordLayerMapped)
      { // tokenize utterances and apply conventions
	 // ensure we have an utterance tokenizer
	 if (getTokenizer() == null)
	 {
	    setTokenizer(new SimpleTokenizer(getUtteranceLayer().getId(), getWordLayer().getId()));
	 }
	 try
	 {
	    tokenizer.transform(graph);
	    // TODO annotation.setAnnotator(...) for all tokens, from the tier's settings.

	 }
	 catch(TransformationException exception)
	 {
	    if (errors == null) errors = new SerializationException();
	    if (errors.getCause() == null) errors.initCause(exception);
	    errors.addError(SerializationException.ErrorType.Tokenization, exception.getMessage());
	 }
	 graph.commit();
	 if (getUseConventions())
	 {
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

	       // run word[pronounce] again, in case some were masked by lexical tags:
	       // word[pronounce](lexical)
	       pronounceTransformer.transform(graph);
	       graph.commit();

	       // clump non-orthographic 'words' with real words
	       OrthographyClumper clumper = new OrthographyClumper(wordLayer.getId());	  
	       clumper.transform(graph);
	       graph.commit();
	    }
	    catch(TransformationException exception)
	    {
	       if (errors == null) errors = new SerializationException();
	       if (errors.getCause() == null) errors.initCause(exception);
	       errors.addError(SerializationException.ErrorType.Tokenization, exception.getMessage());
	    }
	 } // apply transcription conventions
      } // tokenize utterances
      else
      { // word layer mapped
	 // ensure word parent turns are set
	 for (Annotation word : graph.list(wordLayer.getId()))
	 {
	    if (word.getParent() == null)
	    {
	       Annotation[] possibleTurns = word.includingAnnotationsOn(turnLayer.getId());
	       if (possibleTurns.length == 1)
	       { // must be this one
		  word.setParent(possibleTurns[0]);
	       }
	       else if (possibleTurns.length > 1)
	       { // multiple possible turns
		  // use the turn whose label is included in the utterance's tier name
		  // e.g. the turn might be "John Smith" and the utterance tier might be "utterance - John Smith"
		  String wordTier = (String)word.get("@tierId");
		  String wordParticipant = (String)word.get("@participant");
		  Annotation turn = null;
		  for (Annotation possibleTurn : possibleTurns)
		  {
		     // is the label (the speaker) a part of the utterance's tier name?
		     if (wordTier.indexOf(possibleTurn.getLabel()) >= 0
			 || wordParticipant.equals(possibleTurn.getLabel()))
		     {
			// multiple parents could match 
			// e.g. "sp1" and "Interviewer sp1" both are suffixes of "word - Interviewer sp1"
			// so we go with the longest one
			if (turn == null
			    || possibleTurn.getLabel().length() > turn.getLabel().length())
			{
			   turn = possibleTurn;
			} // longest match so far
		     } // label is a part of the tier name
		  } // next possible turn
		  if (turn != null) word.setParent(turn);
	       } // multiple possible turns
	    } // parent not set
	    if (word.getParent() == null)
	    { // parent still not set
	       // maybe children don't quite line up with parents, so use midpoint-including instead
	       Annotation[] possibleTurns = word.midpointIncludingAnnotationsOn(turnLayer.getId());
	       if (possibleTurns.length == 1)
	       { // must be this one
		  word.setParent(possibleTurns[0]);
	       }
	       else if (possibleTurns.length > 1)
	       { // multiple possible turns
		  // use the turn whose label is included in the utterance's tier name
		  // e.g. the turn might be "John Smith" and the utterance tier might be "utterance - John Smith"
		  String wordTier = (String)word.get("@tierId");
		  String wordParticipant = (String)word.get("@participant");
		  Annotation turn = null;
		  for (Annotation possibleTurn : possibleTurns)
		  {
		     // is the label (the speaker) a part of the utterance's tier name?
		     if (wordTier.indexOf(possibleTurn.getLabel()) >= 0
			 || wordParticipant.equals(possibleTurn.getLabel()))
		     {
			// multiple parents could match 
			// e.g. "sp1" and "Interviewer sp1" both are suffixes of "word - Interviewer sp1"
			// so we go with the longest one
			if (turn == null
			    || possibleTurn.getLabel().length() > turn.getLabel().length())
			{
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

      // need to ensure that other required parents are set
      for (Annotation a : graph.getAnnotationsById().values())
      {
	 if (a.getParentId() == null)
	 {
	    Annotation[] possibleParents = a.includingAnnotationsOn(a.getLayer().getParentId());
	    if (possibleParents.length == 1)
	    { // must be this one
	       a.setParent(possibleParents[0]);
	    }
	    else if (possibleParents.length > 1)
	    { // multiple possible parents
	       // use the turn whose label is included in the utterance's tier name
	       // e.g. the turn might be "John Smith" and the utterance tier might be "utterance - John Smith"
	       String tier = (String)a.get("@tierId");
	       String participant = (String)a.get("@participant");
	       Annotation parent = null;
	       Annotation parentWho = null;
	       for (Annotation possibleParent : possibleParents)
	       {
		  // is the label (the speaker) a part of the utterance's tier name?
		  Annotation who = possibleParent.my("who");
		  if (who != null)
		  {
		     if (tier.indexOf(who.getLabel()) >= 0
			 || participant.equals(who.getLabel()))
		     {
			// multiple parents could match 
			// e.g. "sp1" and "Interviewer sp1" both are suffixes of "segment - Interviewer sp1"
			// so we go with the longest one
			if (parent == null
			    || who.getLabel().length() > parentWho.getLabel().length())
			{
			   parent = possibleParent;
			   parentWho = who;
			} // longest match so far
		     } // label is a part of the tier name
		  } // there is a speaker
	       } // next possible parent
	       if (parent != null) a.setParent(parent);
	    } // multiple possible parents
	 } // parent not set

	 if (a.getParentId() == null)
	 { // still not set
	    // maybe children don't quite line up with parents, so use midpoint-including instead
	    Annotation[] possibleParents = a.midpointIncludingAnnotationsOn(a.getLayer().getParentId());
	    if (possibleParents.length == 1)
	    { // must be this one
	       a.setParent(possibleParents[0]);
	    }
	    else if (possibleParents.length > 1)
	    { // multiple possible parents
	       // use the turn whose label is included in the utterance's tier name
	       // e.g. the turn might be "John Smith" and the utterance tier might be "utterance - John Smith"
	       String tier = (String)a.get("@tierId");
	       String participant = (String)a.get("@participant");
	       Annotation parent = null;
	       for (Annotation possibleParent : possibleParents)
	       {
		  // is the label (the speaker) a part of the utterance's tier name?
		  Annotation who = possibleParent.my("who");
		  if (who != null)
		  {
		     if (tier.indexOf(who.getLabel()) >= 0
			 || participant.equals(who.getLabel()))
		     {
			// multiple parents could match 
			// e.g. "sp1" and "Interviewer sp1" both are suffixes of "segment - Interviewer sp1"
			// so we go with the longest one
			if (parent == null
			    || possibleParent.getLabel().length() > parent.getLabel().length())
			{
			   parent = possibleParent;
			} // longest match so far
		     } // label is a part of the tier name
		  } // there is a speaker
	       } // next possible parent
	       if (parent != null) a.setParent(parent);
	    } // multiple possible parents
	 } // parent still not set

      } // next annotation

      // ensure anchors are shared between children and parents where required
      Vector<Layer> layers = graph.getLayersTopDown();
      // (bottom up to propagate changes from below)
      Collections.reverse(layers);
      for (Layer l : layers)
      {
	 if (l.getSaturated() && l.getAlignment() == Constants.ALIGNMENT_INTERVAL)
	 {
	    for (Annotation parent : graph.list(l.getParentId()))
	    {
	       SortedSet<Annotation> children = parent.getAnnotations(l.getId());
	       if (children.size() > 0)
	       {
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
      for (Annotation a : graph.getAnnotationsById().values())
      {
	 a.remove("@tierId");
	 a.remove("@participant");
      }

      // set end anchors of graph tags
      for (Annotation a : graph.list(getParticipantLayer().getId()))
      {
	 a.setStartId(graphStart.getId());
	 a.setEndId(graphEnd.getId());
      }

      graph.commit();

      if (errors != null) throw errors;

      Graph[] graphs = { graph };
      return graphs;
   }

   /**
    * Moves all of the children of the following turn into the preceding turn, set the the end of the preceding to the end of the following, and marks the following for deletion.
    * @param preceding The preceding, surviving turn.
    * @param following The following turn, which will be deleted.
    * @return The changes for this merge.
    */
   public void mergeTurns(Annotation preceding, Annotation following)
   {
      // set anchor
      if (preceding.getEnd().getOffset() == null
	  || following.getEnd().getOffset() == null
	  || preceding.getEnd().getOffset() < following.getEnd().getOffset()) 
      {
	 preceding.setEnd(following.getEnd());
      }

      // for each child layer
      for (String childLayerId : following.getAnnotations().keySet())
      {
	 // move everything from following to preceding
	 int ordinal = 1;
	 if (preceding.getAnnotations().containsKey(childLayerId))
	 {
	    ordinal = preceding.getAnnotations().get(childLayerId).size() + 1;
	 }
	 for (Annotation child : following.annotations(childLayerId))
	 {
	    child.setParent(preceding);
	    child.setOrdinal(ordinal++);
	 } // next child annotation
      } // next child layer
   } // end of joinTurns()
   
} // end of class EAFDeserializer
