//
// Copyright 2018 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.csv;

import java.util.Vector;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
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
import org.apache.commons.csv.*;
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

/**
 * Deserializer for CSV files.
 * <p>These are assumed to have multiple transcripts, one transcript per row, with columns for transcript, author, and meta data. 
 * <p>e.g.
 * <table>
 * <tr><th>Name</th><th>Participant</th></th><th>Text</th><th>Time</th><th>URL</th><th>Corpus</th></tr>
 * <tr><td>Claire Timmins</td><td>@clairemtimmins</td><td>Shame our next meeting wasn't before this deadline @KSavage_Strath @SFaulknerPandO @g_efthimiou @JMcKerrecher</td><td>4:49 AM - 16 Jan 2018</td><td>https://twitter.com/clairemtimmins/status/953247839433510912</td><td>Twitter</td></tr>
 * <tr><td>Mark Levine</td><td>@ProfMarkLevine</td><td>“differences between lesbian or gay and straight faces in selfies relate to grooming, presentation, and lifestyle — that is, differences in culture, not in facial structure.” Algorithms and the junk science of physiognomy @blaiseaguera</td><td>4:27 AM - 13 Jan 2018</td><td>https://twitter.com/ProfMarkLevine/status/952155167188836352</td><td>Twitter</td></tr>
 * <tr><td>Rob Drummond</td><td>@robdrummond</td><td>'Awash with contemporary teenspeak' 😀👍</td><td>2:23 AM - 16 Jan 2018</td><td>https://twitter.com/robdrummond/status/953211091416420352</td><td>Twitter</td></tr>
 * <tr><td>The Irish For 😚<br>🐊</td><td>@theirishfor</td><td>Duolingo has brought the joys of Irish to people all over the world. 💚</td><td>12:09 AM - 16 Jan 2018</td><td>https://twitter.com/theirishfor/status/953177367505264640</td><td>Twitter</td></tr>
 * </table>
 * @author Robert Fromont robert@fromont.net.nz
 */

public class CsvDeserializer
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
    * CSV parser.
    * @see #getParser()
    * @see #setParser(CSVParser)
    */
   protected CSVParser parser;
   /**
    * Getter for {@link #parser}: CSV parser.
    * @return CSV parser.
    */
   public CSVParser getParser() { return parser; }
   /**
    * Setter for {@link #parser}: CSV parser.
    * @param newParser CSV parser.
    */
   public void setParser(CSVParser newParser) { parser = newParser; }

   /**
    * Map of header names to column indices.
    * @see #getHeaderMap()
    * @see #setHeaderMap(Map<String,Integer>)
    */
   protected Map<String,Integer> headerMap;
   /**
    * Getter for {@link #headerMap}: Map of header names to column indices.
    * @return Map of header names to column indices.
    */
   public Map<String,Integer> getHeaderMap() { return headerMap; }
   /**
    * Setter for {@link #headerMap}: Map of header names to column indices.
    * @param newHeaderMap Map of header names to column indices.
    */
   public void setHeaderMap(Map<String,Integer> newHeaderMap) { headerMap = newHeaderMap; }

   // Methods:
   
   /**
    * Default constructor.
    */
   public CsvDeserializer()
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
	 "CSV text collection", "0.1", "text/csv", ".csv", "20190906.1040", getClass().getResource("icon.png"));
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
      NamedStream csv = Utility.FindSingleStream(streams, ".csv", "text/csv");
      if (csv == null) throw new SerializationException("No CSV stream found");
      setName(csv.getName());
      
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
      LinkedHashMap<String,Layer> utteranceAndMetadataLayers = new LinkedHashMap<String,Layer>(metadataLayers);
      utteranceAndMetadataLayers.put(getUtteranceLayer().getId(), getUtteranceLayer());
      LinkedHashMap<String,Layer> whoAndMetadataLayers = new LinkedHashMap<String,Layer>(metadataLayers);
      whoAndMetadataLayers.put(getParticipantLayer().getId(), getParticipantLayer());

      // read the header line

      setParser(CSVParser.parse(
		   csv.getStream(),
		   java.nio.charset.Charset.forName("UTF-8"),
		   CSVFormat.EXCEL.withHeader()));
      setHeaderMap(parser.getHeaderMap());      
      Vector<String> possibleIDHeaders = new Vector<String>();
      Vector<String> possibleUtteranceHeaders = new Vector<String>();
      Vector<String> possibleParticipantHeaders = new Vector<String>();
      for (String header : getHeaderMap().keySet())
      {
	 if (header.trim().length() == 0) continue;
	 Vector<String> possibleMatches = new Vector<String>();
	 possibleMatches.add("transcript" + header);
	 possibleMatches.add("participant" + header);
	 possibleMatches.add("speaker" + header);	 
	 possibleMatches.add(header);

	 // special cases
	 if (header.equalsIgnoreCase("id")
	     || header.equalsIgnoreCase("transcript"))
	 {
	    possibleIDHeaders.add(header);
	 }
	 else if (header.equalsIgnoreCase("text")
	     || header.equalsIgnoreCase("document"))
	 {
	    possibleUtteranceHeaders.add(header);
	 }
	 else if (header.equalsIgnoreCase("name")
	     || header.equalsIgnoreCase("participant")
	     || header.equalsIgnoreCase("participantid"))
	 {
	    possibleParticipantHeaders.add(header);
	 }
	 
	 layerToPossibilities.put(
	    new Parameter("header_"+getHeaderMap().get(header), Layer.class, header), 
	    possibleMatches);
	 layerToCandidates.put("header_"+getHeaderMap().get(header), metadataLayers);
      } // next header
	 
      ParameterSet parameters = new ParameterSet();
      
      // add utterance/participant parameters
      int defaultUtterancePossibilityIndex = 0;
	 
      // if there are no obvious participant column possibilities...      
      Parameter idColumn = new Parameter(
	 "id", String.class, "ID Column", "Column containing the ID of the text.", false);
      if (possibleIDHeaders.size() == 0)
      { // ...include all columns
	 possibleIDHeaders.addAll(getHeaderMap().keySet());
      }
      else
      {
	 idColumn.setValue(possibleIDHeaders.firstElement());
      }
      idColumn.setPossibleValues(possibleIDHeaders);
      parameters.addParameter(idColumn);

      // if there are no obvious participant column possibilities...      
      if (possibleParticipantHeaders.size() == 0)
      { // ...include all columns
	 possibleParticipantHeaders.addAll(getHeaderMap().keySet());
	 // default participant column will be the first column,
	 // so default utterance should be the second (if we didn't find obvious possible text column)
	 if (possibleParticipantHeaders.size() > 1) // but only if there's more than one column
	 {
	    defaultUtterancePossibilityIndex = 1;
	 }
      }
      Parameter participantColumn = new Parameter(
	 "who", "Participant Column", "Column containing the ID of the author of the text.", true,
	 possibleParticipantHeaders.firstElement());
      participantColumn.setPossibleValues(possibleParticipantHeaders);
      parameters.addParameter(participantColumn);
      
      // if there are no obvious text column possibilities...
      if (possibleUtteranceHeaders.size() == 0)
      { // ...include all columns
	 possibleUtteranceHeaders.addAll(getHeaderMap().keySet());
      }
      else
      {
	 // we found a possible text column, so run with it regardless of whether we also found
	 // a possible participant column
	 defaultUtterancePossibilityIndex = 0;
      }
      Parameter utteranceColumn = new Parameter(
	 "text", "Text Column", "Column containing the transcript text.", true,
	 possibleUtteranceHeaders.elementAt(defaultUtterancePossibilityIndex));
      utteranceColumn.setPossibleValues(possibleUtteranceHeaders);
      parameters.addParameter(utteranceColumn);
      
      // add column-mapping parameters, and set possibile/default values
      for (Parameter p : layerToPossibilities.keySet())
      {
	 List<String> possibleNames = layerToPossibilities.get(p);
	 LinkedHashMap<String,Layer> candidateLayers = layerToCandidates.get(p.getName());
	 parameters.addParameter(p);
	 if (p.getValue() == null && candidateLayers != null && possibleNames != null)
	 {
	    p.setValue(Utility.FindLayerById(candidateLayers, possibleNames));
	 }
	 if (p.getPossibleValues() == null && candidateLayers != null)
	 {
	    p.setPossibleValues(candidateLayers.values());
	 }
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
    * <p>This deserializer generates one graph per data row in the CSV file.
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

      String participantColumn = (String)parameters.get("who").getValue();
      String textColumn = (String)parameters.get("text").getValue();

      // if there are errors, accumlate as many as we can before throwing SerializationException
      SerializationException errors = null;

      Vector<Graph> graphs = new Vector<Graph>();
      Iterator<CSVRecord> records = getParser().iterator();
      while (records.hasNext())
      {
	 CSVRecord record = records.next();
	 Graph graph = new Graph();
	 if (parameters == null || parameters.get("id") == null || parameters.get("id").getValue() == null)
	 {
	    graph.setId(getName() + "-" + record.getRecordNumber());
	 }
	 else
	 {
	    graph.setId(record.get((String)parameters.get("id").getValue()));
	 }
	 graph.setOffsetUnits(Constants.UNIT_CHARACTERS);
      
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
	 if (parameters != null)
	 {
	    for (Parameter p : parameters.values())
	    {
	       if (p.getValue() instanceof Layer)
	       {
		  Layer layer = (Layer)p.getValue();
		  if (layer != null && graph.getLayer(layer.getId()) == null)
		  { // haven't added this layer yet
		     graph.addLayer((Layer)layer.clone());
		  }
	       }
	    }
	 }

	 // participant/author
	 Annotation participant = graph.createTag(
	    graph, schema.getParticipantLayerId(), record.get(participantColumn));

	 // meta-data
	 for (String header : getHeaderMap().keySet())
	 {
	    if (header.trim().length() == 0) continue;
	    Parameter p = parameters.get("header_"+getHeaderMap().get(header));
	    if (p != null && p.getValue() != null)
	    {
	       Layer layer = (Layer)p.getValue();
	       String value = record.get(header);
	       if (layer.getParentId().equals(schema.getRoot().getId())) // graph tag
	       {
		  graph.createTag(graph, layer.getId(), value);
	       }
	       else // participant tag
	       {
		  graph.createTag(participant, layer.getId(), value);
	       }
	    } // parameter set
	 } // next header
	 
	 // text
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

	 String sLine = record.get(textColumn).trim();	 
	 int iNumChars = sLine.length();
	 line = new Annotation(null, sLine, getUtteranceLayer().getId());
	 line.setParentId(turn.getId());
	 line.setStart(turn.getStart());	 
	 Anchor end = graph.getOrCreateAnchorAt(
	    ((double)iNumChars + 1), Constants.CONFIDENCE_MANUAL);
	 line.setEnd(end);
	 graph.addAnnotation(line);

	 // ensure we have an utterance tokenizer
	 if (getTokenizer() == null)
	 {
	    setTokenizer(new SimpleTokenizer(getUtteranceLayer().getId(), getWordLayer().getId()));
	 }
	 try
	 {
	    tokenizer.transform(graph);
	 }
	 catch(TransformationException exception)
	 {
	    if (errors == null) errors = new SerializationException();
	    if (errors.getCause() == null) errors.initCause(exception);
	    errors.addError(SerializationException.ErrorType.Tokenization, exception.getMessage());
	 }
	 graph.commit();

	 OrthographyClumper clumper = new OrthographyClumper(wordLayer.getId(), utteranceLayer.getId());
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
      
	 if (errors != null) throw errors;

	 // set end anchors of graph tags
	 for (Annotation a : graph.list(getParticipantLayer().getId()))
	 {
	    a.setStartId(firstAnchor.getId());
	    a.setEndId(lastAnchor.getId());
	 }

	 graph.commit();

	 graphs.add(graph);
      } // next record      
      
      return graphs.toArray(new Graph[0]);
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
      return warnings;
   }

} // end of class CsvDeserializer
