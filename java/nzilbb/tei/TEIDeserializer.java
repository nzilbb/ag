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
package nzilbb.tei;

import java.util.Vector;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import nzilbb.ag.*;
import nzilbb.ag.util.OrthographyClumper;
import nzilbb.ag.util.DefaultOffsetGenerator;
import nzilbb.ag.serialize.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;

/**
 * Deserializer for TEI P5 XML files.
 * <p>TEI tag support is basic at this stage, and many tags are not explicitly interpreted, but most can be mapped to arbitrary annotation layers.
 * <p>Arbitrary transcript/document attributes are implemented by including a &lt;notesStmt&gt;
 * within the &lt;fileDesc&gt; header, containing one &lt;note&gt; tag per attribute, and using the
 * <tt>type</tt> attribute as the attribute key and the tag content as the value - e.g.<br>
 * <pre>&lt;notesStmt&gt;
 *   &lt;note type="subreddit"&gt;StrangerThings&lt;/note&gt;
 *   &lt;note type="parent_id"&gt;t1_dd5f8en&lt;/note&gt;
 * &lt;/notesStmt&gt;
 * </pre>
 * <p>Participant/speaker attributes can be included in the &lt;person&gt; tag, as per the TEI
 *  specification.
 * <p>Arbitrary participant/speaker attributes (i.e. custom attributes or others not foreseen by
 *  the TEI specification) can be processed by including one &lt;note&gt; tag per attribute within
 *  each participant's &lt;person&gt; tag, and using the
 * <tt>type</tt> attribute as the attribute key and the tag content as the value - e.g.<br>
 * <pre>&lt;person&gt;
 *   &lt;idno&gt;ABCD&lt;/idno&gt;
 *   &lt;age&gt;46&lt;/age&gt;
 *   &lt;education&gt;Secondary&lt;/education&gt;
 *   &lt;note type="first language"&gt;English&lt;/note&gt;
 *   &lt;note type="origin"&gt;Liverpool&lt;/note&gt;
 * &lt;/person&gt;
 * </pre>
 * <p>Special support for regularization is used; for a construction like this: <br>
 * <code>&lt;choice&gt;&lt;orig&gt;color&lt;/orig&gt;&lt;reg&gt;colour&lt;/reg&gt;&lt;/choice&gt;</code> <br>
 * The contents of &lt;reg&gt;...&lt;/reg&gt; is used on the words layer, and it is annotated
 * on a layer mapped to "orig" with the contents of the 
 * <p>This deserializer supports part of the &lt;orig&gt;...&lt;/orig&gt; tag.
 * <a href="http://jtei.revues.org/476">schema for Representation of Computer-mediated Communication</a> 
 * proposed by Michael Beißwenger, Maria Ermakova, Alexander Geyken, Lothar Lemnitzer, 
 * and Angelika Storrer (2012), with the exception of the following:
 * <ul>
 *  <li>When &lt;posting&gt; tags are sychronised to a &lt;when&gt; tag inside a &lt;timeline&gt;, the time synchronisation is ignored.</li>
 *  <li>The &lt;addressingTerm&gt;, &lt;addressMarker&gt; and &lt;addressee&gt; tags supported, and mapped to "entities" layer by default, but the <tt>who</tt> attribute of &lt;addressee&gt; is ignored.</li>
 *  <li>The <tt>type</tt> attribute of the &lt;div&gt; tag is ignored.</li>
 *  <li>The <tt>revisedWhen</tt>, <tt>revisedBy</tt>, and <tt>indentLevel</tt> attributes of the &lt;posting&gt; tag are ignored</li>
 *  <li>The &lt;interactionTerm&gt; tag is ignored.</li>
 *  <li>The &lt;interactionTemplate&gt;, &lt;interactionWord&gt;, and &lt;emoticon&gt; tags are not explicitly supported.</li>
 *  <li>The &lt;autoSignature&gt; and &lt;signatureContent&gt; tags are not explicitly supported.</li>
 * </ul>
 * @author Robert Fromont robert@fromont.net.nz
 */

public class TEIDeserializer
   implements IDeserializer
{
   // Attributes:
   protected Vector<String> warnings;

   /** XPATH processor */
   protected XPath xpath;

   /** TEI root node */
   protected Node root;

   /** Header node */
   protected Node header;
   
   /** Main text node node */
   protected Node text;
   
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
   public void setLanguageLayer(Layer newLanguageLayer) { languageLayer = newLanguageLayer; }

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
   public void setEntityLayer(Layer newEntityLayer) { entityLayer = newEntityLayer; }

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
   public void setScribeLayer(Layer newScribeLayer) { scribeLayer = newScribeLayer; }
   
   /**
    * Transcript/document title layer.
    * @see #getTitleLayer()
    * @see #setTitleLayer(Layer)
    */
   protected Layer titleLayer;
   /**
    * Getter for {@link #titleLayer}: Transcript/document title layer.
    * @return Transcript/document title layer.
    */
   public Layer getTitleLayer() { return titleLayer; }
   /**
    * Setter for {@link #titleLayer}: Transcript/document title layer.
    * @param newTitleLayer Transcript/document title layer.
    */
   public void setTitleLayer(Layer newTitleLayer) { titleLayer = newTitleLayer; }


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
   public void setVersionDateLayer(Layer newVersionDateLayer) { versionDateLayer = newVersionDateLayer; }

   /**
    * Transcript tag layer for air date.
    * @see #getPublicationDateLayer()
    * @see #setPublicationDateLayer(Layer)
    */
   protected Layer publicationDateLayer;
   /**
    * Getter for {@link #publicationDateLayer}: Transcript tag layer for air date.
    * @return Transcript tag layer for air date.
    */
   public Layer getPublicationDateLayer() { return publicationDateLayer; }
   /**
    * Setter for {@link #publicationDateLayer}: Transcript tag layer for air date.
    * @param newPublicationDateLayer Transcript tag layer for air date.
    */
   public void setPublicationDateLayer(Layer newPublicationDateLayer) { publicationDateLayer = newPublicationDateLayer; }

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
   public void setTranscriptLanguageLayer(Layer newTranscriptLanguageLayer) { transcriptLanguageLayer = newTranscriptLanguageLayer; }

   /**
    * Participant tag layer for sex attribute.
    * @see #getSexLayer()
    * @see #setSexLayer(Layer)
    */
   protected Layer sexLayer;
   /**
    * Getter for {@link #sexLayer}: Participant tag layer for sex attribute.
    * @return Participant tag layer for sex attribute.
    */
   public Layer getSexLayer() { return sexLayer; }
   /**
    * Setter for {@link #sexLayer}: Participant tag layer for sex attribute.
    * @param newSexLayer Participant tag layer for sex attribute.
    */
   public void setSexLayer(Layer newSexLayer) { sexLayer = newSexLayer; }


   /**
    * Participant tag laye for age attribute.
    * @see #getAgeLayer()
    * @see #setAgeLayer(Layer)
    */
   protected Layer ageLayer;
   /**
    * Getter for {@link #ageLayer}: Participant tag laye for age attribute.
    * @return Participant tag laye for age attribute.
    */
   public Layer getAgeLayer() { return ageLayer; }
   /**
    * Setter for {@link #ageLayer}: Participant tag laye for age attribute.
    * @param newAgeLayer Participant tag laye for age attribute.
    */
   public void setAgeLayer(Layer newAgeLayer) { ageLayer = newAgeLayer; }

   /**
    * Participant tag laye for birth date attribute.
    * @see #getBirthLayer()
    * @see #setBirthLayer(Layer)
    */
   protected Layer birthLayer;
   /**
    * Getter for {@link #birthLayer}: Participant tag laye for birth date attribute.
    * @return Participant tag laye for birth date attribute.
    */
   public Layer getBirthLayer() { return birthLayer; }
   /**
    * Setter for {@link #birthLayer}: Participant tag laye for birth date attribute.
    * @param newBirthLayer Participant tag laye for birth date attribute.
    */
   public void setBirthLayer(Layer newBirthLayer) { birthLayer = newBirthLayer; }


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

   // Methods:
   
   /**
    * Default constructor.
    */
   public TEIDeserializer()
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
	 "TEI Document", "0.12", "application/tei+xml", ".xml", "20191031.1734", getClass().getResource("icon.png"));
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
	 new Parameter("languageLayer", Layer.class, "Language layer", "Inline language tags"), 
	 Arrays.asList("language","lang"));
      layerToCandidates.put("languageLayer", possibleTurnChildLayers);

      layerToPossibilities.put(
	 new Parameter("lexicalLayer", Layer.class, "Lexical layer", "Lexical tags"), 
	 Arrays.asList("lexical"));
      layerToCandidates.put("lexicalLayer", wordTagLayers);

      layerToPossibilities.put(
	 new Parameter("entityLayer", Layer.class, "Entity layer", "Named entities"), 
	 Arrays.asList("entity","namedentity","entities","namedentities"));
      layerToCandidates.put("entityLayer", possibleTurnChildLayers);

      layerToPossibilities.put(
	 new Parameter("titleLayer", Layer.class, "Title layer", "Title of transcript/document"), 
	 Arrays.asList("title","transcripttitle"));
      layerToCandidates.put("titleLayer", graphTagLayers);

      layerToPossibilities.put(
	 new Parameter("scribeLayer", Layer.class, "Scribe layer", "Name of transcriber"), 
	 Arrays.asList("transcripttranscriber","transcriptscribe", "scribe","transcriber"));
      layerToCandidates.put("scribeLayer", graphTagLayers);

      layerToPossibilities.put(
	 new Parameter("versionDateLayer", Layer.class, "Version Date layer", "Version date of transcriber"), 
	 Arrays.asList("transcriptversiondate","versiondate"));
      layerToCandidates.put("versionDateLayer", graphTagLayers);

      layerToPossibilities.put(
	 new Parameter("publicationDateLayer", Layer.class, "Publication date layer", "Date the source document was published"), 
	 Arrays.asList("transcriptpublicationdate", "publicationdate","transcriptairdate","airdate", "transcriptrecordingdate","recordingdate"));
      layerToCandidates.put("publicationDateLayer", graphTagLayers);

      layerToPossibilities.put(
	 new Parameter("transcriptLanguageLayer", Layer.class, "Transcript Language layer", 
		       "The language of the whole transcript"), 
	 Arrays.asList("transcriptlanguage","language"));
      layerToCandidates.put("transcriptLanguageLayer", graphTagLayers);

      // in TEI, there is sex but not gender, so we conflate them when looking for possible mappings
      layerToPossibilities.put(
	 new Parameter("sexLayer", Layer.class, "Sex layer", 
		       "Sex"), 
	 Arrays.asList("participantgender","participantsex","gender","sex"));
      layerToCandidates.put("sexLayer", participantTagLayers);

      layerToPossibilities.put(
	 new Parameter("ageLayer", Layer.class, "Age layer", 
		       "Age"), 
	 Arrays.asList("participantage","age"));
      layerToCandidates.put("ageLayer", participantTagLayers);

      layerToPossibilities.put(
	 new Parameter("birthLayer", Layer.class, "Birth layer", 
		       "Birth date"), 
	 Arrays.asList("participantbirth","participantborn","participantdob","participantdateofbirth","participantyearofbirth","participantbirthdate","participantbirthday","birth","born","dob","dateofbirth","yearofbirth","birthdate","birthday"));
      layerToCandidates.put("birthLayer", participantTagLayers);

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
      // take the first stream, ignore all others.
      NamedStream tei = Utility.FindSingleStream(streams, ".xml", "application/tei+xml");
      if (tei == null) throw new SerializationException("No TEI document stream found");
      setName(tei.getName());

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
      else
      {
	 NamedStream video = Utility.FindSingleStream(streams, ".webm", "video/webm");
	 if (video == null) video = Utility.FindSingleStream(streams, ".mp4", "video/mp4");
	 if (video == null) video = Utility.FindSingleStream(streams, ".mov", "video/quicktime");
	 if (video == null) video = Utility.FindSingleStream(streams, ".mp3", "audio/mp3");
	 if (video != null)
	 {
	    setMediaDurationSeconds(100.0); // TODO find the actual length of the video
	 }
      }

      // Document factory
      DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
      try
      {
	 DocumentBuilder builder = builderFactory.newDocumentBuilder();   
	 builder.setEntityResolver(new EntityResolver()
	    {
	       public InputSource resolveEntity(String publicId, String systemId)
		  throws SAXException, IOException
	       {
		  // Get DTDs locally, to prevent not found errors
		  String sName = systemId.substring(
		     systemId.lastIndexOf('/') + 1);
		  URL url = getClass().getResource(sName);
		  if (url != null)
		     return new InputSource(url.openStream());
		  else
		     return null;
	       }
	    }
	    );
	 
	 // parse
	 Document document = builder.parse(new InputSource(tei.getStream()));
	 XPathFactory xpathFactory = XPathFactory.newInstance();
	 xpath = xpathFactory.newXPath();
	 root = (Node) xpath.evaluate("/TEI", document, XPathConstants.NODE);
	 if (root == null)
	 {
	    throw new SerializationException("XML root is not TEI: " + root.getNodeName());
	 }
	 header = (Node) xpath.evaluate("//teiHeader", document, XPathConstants.NODE);
	 text = (Node) xpath.evaluate("//text", document, XPathConstants.NODE);
	 
	 if (text == null)
	 {
	    throw new SerializationException("Document has no text node");
	 }
	 
	 setSchema(schema);

	 // create a list of layers we need and possible matching layer names
	 LinkedHashMap<Parameter,List<String>> layerToPossibilities = new LinkedHashMap<Parameter,List<String>>();
	 HashMap<String,LinkedHashMap<String,Layer>> layerToCandidates = new HashMap<String,LinkedHashMap<String,Layer>>();	 

	 LinkedHashMap<String,Layer> graphTagLayers = new LinkedHashMap<String,Layer>();
	 for (Layer layer : schema.getRoot().getChildren().values())
	 {
	    if (layer.getAlignment() == Constants.ALIGNMENT_NONE)
	    {
	       graphTagLayers.put(layer.getId(), layer);
	    }
	 } // next turn child layer

	 // transcript attributes
	 NodeList items = (NodeList) xpath.evaluate("fileDesc/sourceDesc/msDesc/msIdentifier/idno[@type='URL']", header, XPathConstants.NODESET);
	 if (items != null && items.getLength() > 0)
	 {
	    layerToPossibilities.put(
	       new Parameter("idnoUrl", Layer.class, "ID URL"), 
	       Arrays.asList("url","transcripturl","documenturl","sourceurl","transcriptidno"));
	    layerToCandidates.put("idnoUrl", graphTagLayers);
	 }
	 else
	 {
	    items = (NodeList) xpath.evaluate("fileDesc/sourceDesc/msDesc/msIdentifier/idno", header, XPathConstants.NODESET);
	    if (items != null && items.getLength() > 0)
	    {
	       layerToPossibilities.put(
		  new Parameter("idno", Layer.class, "ID"), 
		  Arrays.asList("idno", "transcriptidno"));
	       layerToCandidates.put("idno", graphTagLayers);
	    }
	 }

	 items = (NodeList) xpath.evaluate("//notesStmt/note/@type", header, XPathConstants.NODESET);
	 if (items != null && items.getLength() > 0)
	 {
	    // typed <note>s can be mapped to graph tags
	    for (int p = 0; p < items.getLength(); p++)
	    {
	       Attr noteType = (Attr)items.item(p);
	       String keyName = "header_note_type_"+noteType.getValue();
	       if (!layerToCandidates.containsKey(keyName))
	       {
		  Vector<String> possibleMatches = new Vector<String>();
		  possibleMatches.add(noteType.getValue());
		  possibleMatches.add("transcript" + noteType.getValue());
		  layerToPossibilities.put(
		     new Parameter(keyName, Layer.class, "Document Note Type: " + noteType.getValue()), 
		     possibleMatches);
		  layerToCandidates.put(keyName, graphTagLayers);
	       } // not already specified
	    } // next pc type
	 } // there are pc types

	 LinkedHashMap<String,Layer> instantLayers = new LinkedHashMap<String,Layer>();
	 LinkedHashMap<String,Layer> intervalTurnPeerAndChildLayers = new LinkedHashMap<String,Layer>();
	 for (Layer layer : schema.getRoot().getChildren().values())
	 {
	    if (layer.getAlignment() == Constants.ALIGNMENT_INTERVAL)
	    { // aligned children of graph
	       intervalTurnPeerAndChildLayers.put(layer.getId(), layer);
	    }
	    else if (layer.getAlignment() == Constants.ALIGNMENT_INSTANT)
	    {
	       instantLayers.put(layer.getId(), layer);
	    }
	 } // next top level layer

	 LinkedHashMap<String,Layer> intervalTurnChildLayers = new LinkedHashMap<String,Layer>();
	 for (Layer layer : schema.getTurnLayer().getChildren().values())
	 {
	    if (layer.getAlignment() == Constants.ALIGNMENT_INTERVAL)
	    {
	       intervalTurnChildLayers.put(layer.getId(), layer);
	       intervalTurnPeerAndChildLayers.put(layer.getId(), layer);
	    }
	    else if (layer.getAlignment() == Constants.ALIGNMENT_INSTANT)
	    {
	       instantLayers.put(layer.getId(), layer);
	    }
	 } // next turn child layer

	 LinkedHashMap<String,Layer> wordTagLayers = new LinkedHashMap<String,Layer>();
	 for (Layer layer : schema.getWordLayer().getChildren().values())
	 {
	    if (layer.getAlignment() == Constants.ALIGNMENT_NONE)
	    {
	       wordTagLayers.put(layer.getId(), layer);
	    }
	 } // next turn child layer

	 // look for possible word tag layers - i.e. attributes on <w> tags
	 //  e.g.  in the case of <w lemma="Niitty" msd="Nom SG" type="Noun">Niittykin</w>
	 //  potential extra layers are lemma, msd, and type
	 items = (NodeList) xpath.evaluate("//w", text, XPathConstants.NODESET);
	 if (items != null)	    
	 {
	    for (int w = 0; w < items.getLength(); w++)
	    {
	       Node item = items.item(w);
	       NamedNodeMap attributes = item.getAttributes();
	       for (int a = 0; a < attributes.getLength(); a++)
	       {
		  Attr attribute = (Attr)attributes.item(a);
		  if (!layerToCandidates.containsKey(attribute.getName()))
		  {
		     Vector<String> possibleMatches = new Vector<String>();
		     possibleMatches.add(attribute.getName());
		     if (attribute.getName().equals("type"))
		     {
			possibleMatches.add("pos");
			possibleMatches.add("partofspeech");
		     }
		     layerToPossibilities.put(
			new Parameter("w_"+attribute.getName(), Layer.class, "Attribute: " + attribute.getName()), 
			possibleMatches);
		     layerToCandidates.put("w_"+attribute.getName(), wordTagLayers);
		  }
	       } // next attribute
	    } // next w
	 } // there are w's
	 
	 // default to namedEntitiesLayer for nodes
	 for (String sType : distinctNonPWNodeTypes(text))
	 {
	    // these are automatically mapped...
	    if (sType.equals("l") // utterance
		|| sType.equals("lb")) // utterance
	    {
	       continue;
	    }
	    else
	    {
	       Vector<String> possibleMatches = new Vector<String>();
	       if (sType.equals("foreign") && getLanguageLayer() != null)
	       {
		  possibleMatches.add(getLanguageLayer().getId());
	       }
	       else if (sType.equals("note") && getCommentLayer() != null)
	       {
		  possibleMatches.add(getCommentLayer().getId());
	       }
	       possibleMatches.add(sType);
	       if (getEntityLayer() != null) possibleMatches.add(getEntityLayer().getId());
	       layerToPossibilities.put(
		  new Parameter(sType, Layer.class, "Tag: " + sType), possibleMatches);
	       if (sType.equals("figure")
		   || sType.equals("note"))
	       { // could be turn peer or child
		  layerToCandidates.put(sType, intervalTurnPeerAndChildLayers);
	       }
	       else if (sType.equals("pb"))
	       { // instant
		  layerToCandidates.put(sType, instantLayers);
	       }
	       else
	       {
		  layerToCandidates.put(sType, intervalTurnChildLayers);
	       }
	    }
	 } // next tag type	    

	 items = (NodeList) xpath.evaluate("//ab/@type", text, XPathConstants.NODESET);
	 if (items != null && items.getLength() > 0)
	 {
	    Vector<String> possibleMatches = new Vector<String>();
	    if (intervalTurnChildLayers.containsKey("type"))
	    {
	       possibleMatches.add("type");
	    }
	    if (getEntityLayer() != null) possibleMatches.add(getEntityLayer().getId());
	    layerToPossibilities.put(
	       new Parameter("ab_type", Layer.class, "Block Type"), possibleMatches);
	    layerToCandidates.put("ab_type", intervalTurnChildLayers);
	 } // there are instances of <ab type="...">

	 items = (NodeList) xpath.evaluate("//pc/@type", text, XPathConstants.NODESET);
	 if (items != null && items.getLength() > 0)
	 {
	    // typed <pc>s (punctuation characters) can be mapped to their own layer
	    for (int p = 0; p < items.getLength(); p++)
	    {
	       Attr pcType = (Attr)items.item(p);
	       String keyName = "pc_type_"+pcType.getValue();
	       if (!layerToCandidates.containsKey(keyName))
	       {
		  Vector<String> possibleMatches = new Vector<String>();
		  possibleMatches.add(pcType.getValue());
		  if (getEntityLayer() != null) possibleMatches.add(getEntityLayer().getId());
		  layerToPossibilities.put(
		     new Parameter(keyName, Layer.class, "PunctuationType: " + pcType.getValue()), 
		     possibleMatches);
		  layerToCandidates.put(keyName, intervalTurnChildLayers);
	       } // not already specified
	    } // next pc type
	 } // there are pc types

	 // look for person attributes
	 LinkedHashMap<String,Layer> participantTagLayers = new LinkedHashMap<String,Layer>();
	 for (Layer layer : schema.getParticipantLayer().getChildren().values())
	 {
	    if (layer.getAlignment() == Constants.ALIGNMENT_NONE)
	    {
	       participantTagLayers.put(layer.getId(), layer);
	    }
	 } // next turn child layer
	 Node particDesc = (Node) xpath.evaluate("//particDesc/listPerson", header, XPathConstants.NODE);
	 if (particDesc == null)
	 { // person may be a child of listPerson or directly of particDesc
	    particDesc = (Node) xpath.evaluate("//particDesc", header, XPathConstants.NODE);
	 }
	 for (String sType : distinctPersonNodes(particDesc)) // TODO treat person/note separately
	 {
	    Vector<String> possibleMatches = new Vector<String>();
	    possibleMatches.add(sType);
	    if (sType.startsWith("note_type_"))
	    { // person/note
	       possibleMatches.add(sType.replaceAll("^note_type",""));
	       possibleMatches.add("participant"+sType.replaceAll("^note_type",""));
	       possibleMatches.add("speaker"+sType.replaceAll("^note_type",""));
	    }
	    layerToPossibilities.put(
	       new Parameter("person_" + sType, Layer.class, "Person: " + sType), possibleMatches);
	    layerToCandidates.put("person_" + sType, participantTagLayers);
	 } // next tag type	    
	 
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
      catch(ParserConfigurationException x) { throw new SerializationException(x); }
      catch(SAXException x) { throw new SerializationException(x); }
      catch(XPathExpressionException x) { throw new SerializationException(x); }
   }

   /**
    * Traverses the given node recursively to build a set of distinct node types that are not text, nor &lt;p&gt; (nor &lt;div&gt;, etc. )
    * @param n Node to traverse.
    * @return Set of node type names
    */
   protected HashSet<String> distinctNonPWNodeTypes(Node n)
   {
      HashSet<String> types = new HashSet<String>();
      String sType = n.getNodeName();
      if (!sType.equals("w") 
	  && !sType.equals("p") 
	  && !sType.equals("lb") 
	  && !sType.equals("ab") 
	  && !sType.equals("div") 
	  && !sType.equals("body") 
	  && !sType.equals("text") 
	  && !sType.equals("front") 
	  && !sType.equals("timeline") 
	  && !sType.equals("when") 
	  && !sType.equals("#comment")
	  && !sType.equals("posting") // http://jtei.revues.org/476
	  && !sType.equals("interactionTerm")
	  // choice/orig/reg constructions have special handling, mapping just "orig" is sufficient
	  && !sType.equals("choice") && !sType.equals("reg")) 
      {
	 types.add(sType);
      }
      NodeList children = n.getChildNodes();
      for (int c = 0; c < children.getLength(); c++)
      {
	 Node child = children.item(c);
	 if (!(child instanceof Text))
	 {	    
	    types.addAll(distinctNonPWNodeTypes(child));
	 }
      } // next child
      return types;
   } // end of distinctNonPWNodeTypes()

   /**
    * Traverses the given node recursively to build a set of distinct node types that are children of a &lt;person&gt; node, excluding idno, persName, sex, age, and birth, which are globally configured.
    * @param n Note to traverse.
    * @return Set of node type names
    */
   protected HashSet<String> distinctPersonNodes(Node n)
   {
      HashSet<String> types = new HashSet<String>();
      try
      {
	 NodeList items = (NodeList) xpath.evaluate("person", n, XPathConstants.NODESET);
	 if (items != null)	    
	 {
	    for (int w = 0; w < items.getLength(); w++)
	    {
	       Node person = items.item(w);
	       NodeList children = person.getChildNodes();
	       for (int c = 0; c < children.getLength(); c++)
	       {
		  Node child = children.item(c);	       
		  String sType = child.getNodeName();
		  if (!sType.equals("idno") 
		      && !sType.equals("age") 
		      && !sType.equals("birth")
		      && !sType.equals("persName")
		      && !sType.equals("#text") 
		      && !sType.equals("#comment")
		      && !sType.equals("note"))
		  {
		     types.add(sType);
		  }
		  else if (sType.equals("note"))
		  {
		     Attr type = (Attr)child.getAttributes().getNamedItem("type");
		     types.add("note_type_"+type.getValue());
		  }
	       } // next child
	    } // next person
	 } // there are persons
      }
      catch(XPathExpressionException exception)
      {}
      return types;
   } // end of distinctNonPWNodeTypes()

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

      // if there are errors, accumlate as many as we can before throwing SerializationException
      SerializationException errors = null;

      Graph graph = new Graph();
      graph.setId(getName());
      
      // creat the 0 anchor to prevent graph tagging from creating one with no confidence
      Anchor startAnchor = graph.getOrCreateAnchorAt(0.0, Constants.CONFIDENCE_MANUAL);
      Anchor endAnchor = null;
      boolean characterOffsets = true;

      if (getMediaDurationSeconds() != null)
      {
	 graph.setOffsetUnits(Constants.UNIT_SECONDS);
	 endAnchor = graph.getOrCreateAnchorAt(getMediaDurationSeconds(), Constants.CONFIDENCE_MANUAL);
	 characterOffsets = false;
      }
      else
      {
	 graph.setOffsetUnits(Constants.UNIT_CHARACTERS); // TODO look for timeline
      }

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
      if (languageLayer != null) graph.addLayer((Layer)languageLayer.clone());
      if (lexicalLayer != null) graph.addLayer((Layer)lexicalLayer.clone());
      if (entityLayer != null) graph.addLayer((Layer)entityLayer.clone());
      if (commentLayer != null) graph.addLayer((Layer)commentLayer.clone());
      if (titleLayer != null) graph.addLayer((Layer)titleLayer.clone());
      if (scribeLayer != null) graph.addLayer((Layer)scribeLayer.clone());
      if (versionDateLayer != null) graph.addLayer((Layer)versionDateLayer.clone());
      if (publicationDateLayer != null) graph.addLayer((Layer)publicationDateLayer.clone());
      if (transcriptLanguageLayer != null) graph.addLayer((Layer)transcriptLanguageLayer.clone());
      if (sexLayer != null) graph.addLayer((Layer)sexLayer.clone());
      if (ageLayer != null) graph.addLayer((Layer)ageLayer.clone());
      if (birthLayer != null) graph.addLayer((Layer)birthLayer.clone());
      if (parameters == null) setParameters(new ParameterSet());
      for (Parameter p : parameters.values())
      {
	 Layer layer = (Layer)p.getValue();
	 if (layer != null && graph.getLayer(layer.getId()) == null)
	 { // haven't added this layer yet
	    graph.addLayer((Layer)layer.clone());
	 }
      }
      
      try
      {
	 // attributes
	 String sResult = xpath.evaluate("fileDesc/titleStmt/respStmt/name/text()", header);
	 if (sResult != null && sResult.length() > 0 && scribeLayer != null)
	 {
	    graph.createTag(graph, scribeLayer.getId(), sResult);
	 }
	 sResult = xpath.evaluate("revisionDesc/change/respStmt/name/text()", header);
	 if (sResult != null && sResult.length() > 0 && scribeLayer != null)
	 {
	    graph.createTag(graph, scribeLayer.getId(), sResult);
	 }
	 sResult = xpath.evaluate("fileDesc/titleStmt/title/text()", header);
	 if (sResult != null && sResult.length() > 0 && titleLayer != null)
	 {
	    graph.createTag(graph, titleLayer.getId(), sResult);
	 }
	 sResult = xpath.evaluate("revisionDesc/change/date/text()", header);
	 if (sResult != null && sResult.length() > 0 && versionDateLayer != null)
	 {
	    graph.createTag(graph, versionDateLayer.getId(), sResult);
	 }
	 sResult = xpath.evaluate("profileDesc/creation/date/text()", header);
	 if (sResult != null && sResult.length() > 0 && publicationDateLayer != null)
	 {
	    graph.createTag(graph, publicationDateLayer.getId(), sResult);
	 }
	 else
	 { // might be CDC-style - i.e. on a timeline
	    sResult = xpath.evaluate("front/timeline/when/@absolute", text);
	    if (sResult != null && sResult.length() > 0 && publicationDateLayer != null)
	    {
	       graph.createTag(graph, publicationDateLayer.getId(), sResult);
	    }
	 }
	 sResult = xpath.evaluate("profileDesc/langUsage/language/@ident", header);
	 if (sResult != null && sResult.length() > 0 && transcriptLanguageLayer != null)
	 {
	    graph.createTag(graph, transcriptLanguageLayer.getId(), sResult);
	 }
	 Attr lang = (Attr)text.getAttributes().getNamedItem("lang");
	 if (lang != null)
	 {
	    graph.createTag(graph, transcriptLanguageLayer.getId(), lang.getValue().toLowerCase());
	 }
	 NodeList items = (NodeList) xpath.evaluate("fileDesc/sourceDesc/msDesc/msIdentifier/idno", header, XPathConstants.NODESET);
	 if (items != null && items.getLength() > 0)
	 {
	    Element idno = (Element)items.item(0);
	    String value = idno.getTextContent();
	    if (value != null) value = value.trim();
	    if (value != null && value.length() > 0)
	    { // there's value
	       Layer layer = null;	       
	       if (parameters.containsKey("idnoUrl"))
	       { // idno is URL
		  layer = (Layer) parameters.get("idnoUrl").getValue();
	       }
	       else if (parameters.containsKey("idno"))
	       { // idno isn't URL
		  layer = (Layer) parameters.get("idno").getValue();
	       }
	       if (layer != null)
	       { // it's mapped to a layer
		  graph.createTag(graph, layer.getId(), value);
	       }
	    } // there's a value
	 }

	 items = (NodeList) xpath.evaluate("//note[@type]", header, XPathConstants.NODESET);
	 if (items != null && items.getLength() > 0)
	 {
	    // typed <note>s can be mapped to graph tags
	    for (int p = 0; p < items.getLength(); p++)
	    {
	       Element note = (Element)items.item(p);
	       String value = note.getTextContent();
	       if (value != null) value = value.trim();
	       if (value != null && value.length() > 0)
	       { // there's value
		  String noteType = note.getAttribute("type");
		  String keyName = "header_note_type_"+noteType;
		  if (parameters.containsKey(keyName))
		  { // there is a parameter
		     Layer layer = (Layer) parameters.get(keyName).getValue();
		     if (layer != null)
		     { // mapped to a layer
			graph.createTag(graph, layer.getId(), value);
		     }
		  } // there is a parameter
	       } // there's value
	    } // next note type
	 } // there are note types

	 
	 // participants - teiHeader/profileDesc/particDesc[/listPerson]/person
	 Annotation participant = null;
	 items = (NodeList) xpath.evaluate(
	    // person may be a child of listPerson or directly of particDesc
	    "profileDesc/particDesc/listPerson/person|profileDesc/particDesc/person",
	    header, XPathConstants.NODESET);
	 if (items != null && items.getLength() > 0)
	 {
	    for (int p = 0; p < items.getLength(); p++)
	    {
	       participant = new Annotation(null, "author", schema.getParticipantLayerId(), startAnchor.getId(), startAnchor.getId());
	       Element person = (Element)items.item(p);
	       // set ID
	       if (person.getAttribute("xml:id").length() > 0)
	       { // @id attribute
		  participant.setId(person.getAttribute("xml:id"));
	       }
	       else
	       { // no @id, so look for idno element
		  sResult = xpath.evaluate("idno/text()", person);
		  if (sResult != null && sResult.length() > 0)
		  {
		     participant.setId(sResult);
		  }
	       }
	       // set name
	       sResult = xpath.evaluate("persName/text()", person);
	       if (sResult != null && sResult.length() > 0)
	       {
		  participant.setLabel(sResult);
	       }
	       else if (participant.getId() != null)
	       { // no name, so use the ID as the name
		  participant.setLabel(participant.getId());
	       }
	       participant.setParentId(graph.getId());
	       graph.addAnnotation(participant);
	       
	       // now that they're added, we can tag them with more info...
	       
	       // sex
	       if (person.getAttribute("sex").length() > 0
		   && getSexLayer() != null)
	       {
		  graph.createTag(participant, sexLayer.getId(), person.getAttribute("sex"));
	       }
	       // age
	       sResult = xpath.evaluate("age/text()", person);
	       if (sResult != null && sResult.length() > 0
		   && getAgeLayer() != null)
	       {
		  graph.createTag(participant, ageLayer.getId(), sResult);
	       }
	       // birth
	       sResult = xpath.evaluate("birth/@when", person);
	       if (sResult != null && sResult.length() > 0
		   && getBirthLayer() != null)
	       {
		  graph.createTag(participant, birthLayer.getId(), sResult);
	       }
	       
	       // other tags...
	       NodeList children = person.getChildNodes();
	       for (int c = 0; c < children.getLength(); c++)
	       {
		  Node child = children.item(c);
		  if (child instanceof Element && child.getChildNodes().getLength() > 0)
		  {
		     String name = child.getNodeName();
		     if (!name.equals("idno")
			 && !name.equals("birth")
			 && !name.equals("age")
			 && !name.equals("persName")
			 && !name.equals("note"))
		     {
			String value = child.getChildNodes().item(0).getNodeValue();
			Layer layer = (Layer)parameters.get("person_" + name).getValue();
			if (layer != null)
			{
			   graph.createTag(participant, layer.getId(), value);
			}
		     }
		     else if (name.equals("note"))
		     {
			Attr noteType = (Attr)child.getAttributes().getNamedItem("type");
			String keyName = "person_note_type_" + noteType.getValue();
			if (noteType != null && parameters.containsKey(keyName))
			{		     
			   Layer layer = (Layer)parameters.get(keyName).getValue();
			   if (layer != null)
			   {
			      String value = child.getTextContent();
			      graph.createTag(participant, layer.getId(), value);
			   }
			}
		     }
		  } // element child
	       } // next child
	       
	    } // next participant
	 }
	 else
	 { // no participants, so use an "author"
	    participant = new Annotation(null, "author", schema.getParticipantLayerId());
	    // maybe the author is named
	    sResult = xpath.evaluate("fileDesc/sourceDesc/biblStruct/monogr/author/text()", header);
	    if (sResult != null && sResult.length() > 0)
	    {
	       participant.setLabel(sResult);
	    }
	    else
	    {
	       sResult = xpath.evaluate("fileDesc/titleStmt/author/text()", header);
	       if (sResult != null && sResult.length() > 0) participant.setLabel(sResult);
	    }
	    participant.setParentId(graph.getId());
	    graph.addAnnotation(participant);
	 }
	 if (graph.list(getParticipantLayer().getId()).length > 1)
	 { // don't default to the single participant
	    participant = null;
	 }
	 
	 // graph
	 Annotation turn = new Annotation(
	    null, participant != null?participant.getLabel():"", getTurnLayer().getId());
	 graph.addAnnotation(turn);
	 if (participant != null) turn.setParent(participant);
	 turn.setStart(startAnchor);
	 Annotation line = new Annotation(null, turn.getLabel(), getUtteranceLayer().getId());
	 line.setParentId(turn.getId());
	 line.setStart(turn.getStart());
	 int iLastPosition = 0;	 
	 
	 Vector<Node> vNodes = flattenToWords(text);      
	 HashMap<Node,Annotation> mFoundEntities = new HashMap<Node,Annotation>();
	 // for each word or text chunk
	 String wordLayerId = getWordLayer().getId();
	 int w = 1;
	 Annotation anLastWord = null;
	 Anchor aChoiceStarted = null;
	 Annotation anCurrentOrig = null;
	 Anchor lastAnchor = startAnchor;
	 for (Node n : vNodes)
	 {
	    boolean finishHere = false;
	    if (n instanceof Text)
	    {
	       StringTokenizer words = new StringTokenizer(n.getNodeValue()); // TODO use configured tokenizer

	       while (words.hasMoreTokens())
	       {
		  Annotation anWord = new Annotation(null, words.nextToken(), wordLayerId, turn.getId());
		  anWord.setOrdinal(w++);
		  //TODO anWord.setLabelStatus(Labbcat.LABEL_STATUS_USER);
		  if (characterOffsets)
		  {
		     anWord.setStartId(graph.getOrCreateAnchorAt(
					  (double)iLastPosition, Constants.CONFIDENCE_MANUAL).getId());
		     iLastPosition += anWord.getLabel().length() + 1; // include inter-word space
		     anWord.setEndId(graph.getOrCreateAnchorAt(
					(double)iLastPosition, Constants.CONFIDENCE_MANUAL).getId());
		  }
		  else
		  {
		     anWord.setStartId(lastAnchor.getId());
		     lastAnchor = new Anchor();
		     graph.addAnchor(lastAnchor);
		     anWord.setEndId(lastAnchor.getId());
		  }
		  
		  graph.addAnnotation(anWord);
		  
		  anLastWord = anWord;
	       } // next word
	    }
	    else if (n.getNodeName().equals("p")
		     || n.getNodeName().equals("div")
		     || n.getNodeName().equals("ab")
		     || n.getNodeName().equals("l")
		     || n.getNodeName().equals("lb"))
	    {
	       // end the last line
	       if (characterOffsets)
	       {
		  line.setEnd(graph.getOrCreateAnchorAt(
				 (double)iLastPosition, Constants.CONFIDENCE_MANUAL));
	       }
	       else
	       {
		  lastAnchor = new Anchor();
		  graph.addAnchor(lastAnchor);
		  line.setEnd(lastAnchor);
	       }
	       if (!line.getStartId().equals(line.getEndId()))
	       { // if we have <div><p>... don't create an instantaneous, empty line
		  graph.addAnnotation(line);
	       }
	       
	       // start a new line
	       line = new Annotation(null, turn.getLabel(), getUtteranceLayer().getId());
	       line.setParentId(turn.getId());
	       //graph.addAnnotation(line);
	       if (characterOffsets)
	       {
		  line.setStart(graph.getOrCreateAnchorAt(
				   (double)iLastPosition, Constants.CONFIDENCE_MANUAL));
	       }
	       else
	       {
		  line.setStart(lastAnchor);
	       }
	    } // line
	    else if (n.getNodeName().equals("u")
		     || n.getNodeName().equals("posting"))
	    {
	       if (turn.getLabel().length() == 0)
	       { // default empty starting turn
		  // set participant
		  Element attributable = (Element)n;
		  if (attributable.getAttribute("who").length() > 0)
		  {		  
		     Annotation who = graph.getAnnotation(attributable.getAttribute("who")
							  .replaceAll("^#","")); // ignore leading #
		     if (who != null)
		     {
			participant = who;
			turn.setLabel(participant.getLabel());
			turn.setParentId(participant.getId());
		     }
		  }
	       }
	       else
	       { // new turn
		  // "attributable" spans, utterance, etc.
		  if (characterOffsets)
		  {
		     turn.setEnd(graph.getOrCreateAnchorAt(
				    (double)iLastPosition, Constants.CONFIDENCE_MANUAL));
		  }
		  else
		  {
		     lastAnchor = new Anchor();
		     graph.addAnchor(lastAnchor);
		     turn.setEnd(lastAnchor);
		  }
		  if (!turn.getStartId().equals(turn.getEndId()))
		  { // don't create an instantaneous, empty turn
		     graph.addAnnotation(turn);
		  }
		  
		  // set participant
		  Element attributable = (Element)n;
		  if (attributable.getAttribute("who").length() > 0)
		  {		  
		     Annotation who = graph.getAnnotation(attributable.getAttribute("who")
							  .replaceAll("^#","")); // ignore leading #
		     if (who != null)
		     {
			participant = who;
		     }
		  }
		  
		  // start a new turn
		  if (!turn.getStartId().equals(turn.getEndId()))
		  {
		     turn = new Annotation(null, participant.getLabel(), getTurnLayer().getId());
		     turn.setParentId(participant.getId());
		     graph.addAnnotation(turn);
		     if (characterOffsets)
		     {
			turn.setStart(graph.getOrCreateAnchorAt(
					 (double)iLastPosition, Constants.CONFIDENCE_MANUAL));
		     }
		     else
		     {
			turn.setStart(lastAnchor);
		     }
		  }
		  else
		  {
		     turn.setParentId(participant.getId());
		  }
		  // start a new line too
		  if (characterOffsets)
		  {
		     line.setEnd(graph.getOrCreateAnchorAt(
				    (double)iLastPosition, Constants.CONFIDENCE_MANUAL));
		  }
		  else
		  {
		     line.setEnd(lastAnchor);
		  }
		  if (!line.getStartId().equals(line.getEndId()))
		  { // if we have <div><p>... don't create an instantaneous, empty line
		     graph.addAnnotation(line);
		  }
		  line = new Annotation(null, turn.getLabel(), getUtteranceLayer().getId());
		  line.setParentId(turn.getId());
		  if (characterOffsets)
		  {
		     line.setStart(graph.getOrCreateAnchorAt(
				      (double)iLastPosition, Constants.CONFIDENCE_MANUAL));
		  }
		  else
		  {
		     line.setStart(lastAnchor);
		  }
	       }
	    } // turn
	    else if (n.getNodeName().equals("choice"))
	    {
	       if (aChoiceStarted == null)
	       { // opening a new choice tag
		  if (characterOffsets)
		  {
		     aChoiceStarted = graph.getOrCreateAnchorAt(
			(double)iLastPosition, Constants.CONFIDENCE_MANUAL);
		  }
		  else
		  {
		     aChoiceStarted = lastAnchor;
		  }
		  anCurrentOrig = null;
	       }
	       else
	       { // closing an opened choice tag
		  if (anCurrentOrig != null)
		  {
		     if (characterOffsets)
		     {
			anCurrentOrig.setEnd(
			   graph.getOrCreateAnchorAt(
			      (double)iLastPosition, Constants.CONFIDENCE_MANUAL));
		     }
		     else
		     {
			anCurrentOrig.setEnd(lastAnchor);
		     }
		     anCurrentOrig = null;
		  }
		  aChoiceStarted = null;
	       }
	    }
	    else
	    { // some other entity
	       if (!mFoundEntities.containsKey(n))
	       { // new named entity
		  Layer layer = (Layer)parameters.get(n.getNodeName()).getValue();
		  if (n.getNodeName().equals("pc"))
		  { // <pc> tags can be mapped by type attribute
		     Attr att = (Attr)n.getAttributes().getNamedItem("type");
		     if (att != null && parameters.containsKey("pc_type_" + att.getValue()))
		     {		     
			layer = (Layer)parameters.get("pc_type_" + att.getValue()).getValue();
		     }
		  }
		  if (layer != null)
		  {
		     String label = n.getNodeName();
		     if (n.getChildNodes().getLength() > 0)
		     {
			label = n.getChildNodes().item(0).getNodeValue();
		     }
		     Annotation anEntity = new Annotation(null, label, layer.getId());
		     anEntity.setStart(characterOffsets?
				       graph.getOrCreateAnchorAt(
					  (double)iLastPosition, Constants.CONFIDENCE_MANUAL)
				       :lastAnchor);
		     if (n.getNodeName().equals("orig"))
		     {
			if (aChoiceStarted != null)
			{
			   anEntity.setStart(aChoiceStarted);
			   anCurrentOrig = anEntity;
			}
		     }
		     else if (n.getNodeName().equals("note"))
		     {
			finishHere = true;			
		     }
		     else if (n.getNodeName().equals("pc"))
		     {
			finishHere = true;
		     }
		     else if (n.getNodeName().equals("pb"))
		     { // page break
			finishHere = true;
		     }
		     else if (n.getNodeName().equals("foreign"))
		     {
			Attr att = (Attr)n.getAttributes().getNamedItem("xml:lang");
			if (att != null)
			{
			   // the label is the language
			   anEntity.setLabel(att.getValue());
			}
		     }
		     else if (n.getNodeName().equals("unclear"))
		     {
			anEntity.setLabel(n.getNodeName());
			Attr att = (Attr)n.getAttributes().getNamedItem("reason");
			if (att != null)
			{
			   if (getEntityLayer() != null
			       || !layer.getId().equals(getEntityLayer().getId()))
			   { // tags have their own layer, so the label doesn't need the tag name
			      anEntity.setLabel(att.getValue());
			   }
			   else
			   {
			      anEntity.setLabel(anEntity.getLabel() + ": " + att.getValue());
			   }
			}
			att = (Attr)n.getAttributes().getNamedItem("cert");
			if (att != null)
			{
			   anEntity.setLabel(anEntity.getLabel() + " (" + att.getValue() + ")");
			}
		     }
		     else
		     { // everything that's not "orig" nor "note" nor "foreign" nor "unclear"
			anEntity.setLabel(n.getNodeName());
			Attr att = (Attr)n.getAttributes().getNamedItem("type");
			if (att != null)
			{
			   if (getEntityLayer() == null
			       || !layer.getId().equals(getEntityLayer()))
			   { // tags have their own layer, so the label doesn't need the tag name
			      anEntity.setLabel(att.getValue());
			   }
			   else
			   {
			      anEntity.setLabel(anEntity.getLabel() + ": " + att.getValue());
			   }
			}
		     } // not "orig" nor "note" nor "foreign" nor "unclear"
		     anEntity.setConfidence(Constants.CONFIDENCE_MANUAL);
		     if (layer.getParentId().equals(getTurnLayer().getId()))
		     {
			anEntity.setParentId(turn.getId());
		     }
		     else if (layer.getParentId().equals(schema.getRoot().getId()))
		     {
			anEntity.setParentId(graph.getId());
		     }
		     mFoundEntities.put(n, anEntity);
		  }
	       } // new entity
	       else // previously started entity
	       {
		  finishHere = true;
	       }
	       
	       if (finishHere)
	       { // close the entity
		  Annotation anEntity = mFoundEntities.get(n);
		  anEntity.setEnd(characterOffsets?
				  graph.getOrCreateAnchorAt(
				     (double)iLastPosition, Constants.CONFIDENCE_MANUAL)
				  :lastAnchor);
		  graph.addAnnotation(anEntity);
		  mFoundEntities.remove(n);
	       }
	    }  // some other entity
	 } // next node

	 if (characterOffsets)
	 {
	    turn.setEnd(graph.getOrCreateAnchorAt(
			   (double)iLastPosition, Constants.CONFIDENCE_MANUAL));
	 }
	 else
	 {
	    turn.setEnd(endAnchor);
	 }
	 if (!turn.getStartId().equals(turn.getEndId()))
	 { // don't create an instantaneous, empty turn
	    graph.addAnnotation(turn);
	 }
	 line.setEnd(turn.getEnd());
	 if (!line.getStartId().equals(line.getEndId()))
	 { // don't create an instantaneous, empty line
	    graph.addAnnotation(line);
	 }

         graph.trackChanges();
         
	 OrthographyClumper clumper = new OrthographyClumper(wordLayer.getId());
	 // orthographic characters include not only letters and numbers,
	 // but also @, so that tweet addressee annotations aren't clumped
	 clumper.setNonOrthoCharacterPattern("[^\\p{javaLetter}\\p{javaDigit}@]");
	 try
	 {
	    // clump non-orthographic 'words' with real words
	    clumper.transform(graph);
	    graph.commit();
	 }
	 catch(TransformationException exception)
	 {
	    if (errors == null) errors = new SerializationException();
	    if (errors.getCause() == null) errors.initCause(exception);
	    errors.addError(SerializationException.ErrorType.Tokenization, exception.getMessage());
	 }
	 
	 // set end anchors of graph tags
	 SortedSet<Anchor> anchors = graph.getSortedAnchors();
	 Anchor firstAnchor = anchors.first();
	 lastAnchor = anchors.last();
	 for (Annotation a : graph.list(getParticipantLayer().getId()))
	 {
	    a.setStartId(firstAnchor.getId());
	    a.setEndId(lastAnchor.getId());
	 }

	 if (!characterOffsets)
	 { // set default offsets
	    try
	    {
	       new DefaultOffsetGenerator().transform(graph);	    
	    }
	    catch(TransformationException exception)
	    {
	       if (errors == null) errors = new SerializationException();
	       if (errors.getCause() == null) errors.initCause(exception);
	       errors.addError(SerializationException.ErrorType.Tokenization, exception.getMessage());
	    }
	 }
	 
	 graph.commit();

	 if (errors != null) throw errors;

         // reset all change tracking
         graph.getTracker().reset();
         graph.setTracker(null);

	 Graph[] graphs = { graph };
	 return graphs;
      }
      catch(XPathExpressionException x) { throw new SerializationException(x); }
   }

   /**
    * Traverses the given node recursively to build a list of words and intervening structures.
    * @param n Node to traverse.
    * @return An ordered list of Node objects that are either &lt;w&gt; nodes, or character data nodes that correspond to words, or &lt;p&gt; or &lt;s&gt; nodes
    */
   protected Vector<Node> flattenToWords(Node n)
   {
      Vector<Node> vNodes = new Vector<Node>();
      NodeList children = n.getChildNodes();
      for (int c = 0; c < children.getLength(); c++)
      {
	 Node child = children.item(c);
	 if (child instanceof Text)
	 {	    
	    if (child.getNodeValue().trim().length() > 0) vNodes.add(child);
	 }
	 else if (child instanceof Element)
	 {
	    if (child.getNodeName().equals("w"))
	    {
	       vNodes.add(child);
	    }
	    else
	    {
	       if (child.getNodeName().equals("p")
		   || child.getNodeName().equals("div")
		   || child.getNodeName().equals("ab")
		   || child.getNodeName().equals("lg")
		   || child.getNodeName().equals("l")
		   || child.getNodeName().equals("lb")
		   || child.getNodeName().equals("note")
		   // attributable
		   || child.getNodeName().equals("u")
		   || child.getNodeName().equals("posting")
		   // we need the start (and end) of "choice" tags to correctly handle "orig" tags
		   || child.getNodeName().equals("choice")
		   // we need the node of "orig" tags
		   || child.getNodeName().equals("orig")
		   // and other spans
		   || parameters.containsKey(child.getNodeName()))
	       {
		  vNodes.add(child);
	       }
	       // we don't pass back the children of "orig" nor "note" nor "pc"
	       if (!child.getNodeName().equals("orig")
		   && !child.getNodeName().equals("note")
		   && !child.getNodeName().equals("pc"))
	       {
		  vNodes.addAll(flattenToWords(child));
	       }
	       // we need the (start and) end of "choice" tags to correctly handle "orig" tags
	       if (child.getNodeName().equals("choice")
		   // and other spans (but not note and pc, which are instantaneous)
		   || (parameters.containsKey(child.getNodeName())
		       && !child.getNodeName().equals("note")
		       && !child.getNodeName().equals("pc")
		       && !child.getNodeName().equals("pb"))) // page break
	       {
		  vNodes.add(child);
	       }
	    }
	 }
      } // next child
      return vNodes;
   } // end of getWords()

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

} // end of class TEIDeserializer
