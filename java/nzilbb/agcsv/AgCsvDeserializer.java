//
// Copyright 2016-2017 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.agcsv;

import java.util.Vector;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import org.apache.commons.csv.*; 
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import nzilbb.ag.*;
import nzilbb.ag.serialize.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;

/**
 * Deserializer for legacy LaBB-CAT CSV annotation graph files, for unit tests and debugging.
 * <p>Each graph is encoded using a single csv file, using the following conventions:
 * <dl>
 *  <dt>Anchors</dt>
 *  <dd>Anchors are listed at the beginning of the file, with fields:
 *   <ol>
 *    <li>id (string) - UID</li>
 *    <li>offset (double)</li>
 *    <li>alignmentStatus (byte)</li>
 *    <li>[comment (string)]</li>
 *   </ol>
 *  </dd>
 *  <dt>Layers</dt>
 *  <dd>Each layer starts with a header with the following (unheadered) fields:
 *   <ol>
 *    <li>"layer"</li>
 *    <li>name (string)</li>
 *    <li>description (string)</li>
 *    <li>type - T = text N = number D = DISC phonological</li>
 *    <li>scope - S = segmnet W = word M = meta F = freeform </li>
 *    <li>alignment</li>
 *    <li>numeric layer ID</li>
 *    <li>[comment (string)]</li>
 *   </ol>
 *   This header is followed by the annotations, with the following fields:
 *   <ol>
 *    <li>id (string) - UID</li>
 *    <li>startAnchor.id (string)</li>
 *    <li>endAnchor.id (string)</li>
 *    <li>label (string)</li>
 *    <li>labelStatus (byte)</li>
 *    <li>turnAnnotationId (string) - optional</li>
 *    <li>ordinalInTurn (int) - optional</li>
 *    <li>wordAnnotationId (string) - optional</li>
 *    <li>ordinalInWord (int) - optional</li>
 *    <li>segmentAnnotationId (string) - optional</li>
 *   </ol>
 *  </dd>
 * </dl>
 * @author Robert Fromont robert@fromont.net.nz
 */

public class AgCsvDeserializer
   implements IDeserializer
{
   // Attributes:
   protected Vector<String> warnings;
   
   /**
    * Name of the .csv file.
    * @see #getName()
    * @see #setName(String)
    */
   protected String name;
   /**
    * Getter for {@link #name}: Name of the .csv file.
    * @return Name of the .csv file.
    */
   public String getName() { return name; }
   /**
    * Setter for {@link #name}: Name of the .csv file.
    * @param newName Name of the .csv file.
    */
   public void setName(String newName) { name = newName; }
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public AgCsvDeserializer()
   {
   } // end of constructor
   
   /**
    * Resete state.
    */
   public void reset()
   {
      warnings = new Vector<String>();
      mCsvData.clear();
   } // end of reset()

   // IStreamDeserializer methods:

   /**
    * Returns the deserializer's descriptor
    * @return The deserializer's descriptor
    */
   public SerializationDescriptor getDescriptor()
   {
      return new SerializationDescriptor(
	 "LaBB-CAT legacy CSV files", "0.1", "text/csv", ".csv", "20200909.1954", getClass().getResource(getClass().getSimpleName() + ".png"));
   }

   /**
    * CSV field delimiter - the tab character by default, despite the C in CSV standing for 
    * 'comma', because this format is intended for testing and so has to be as human-readible 
    * as possible.
    * @see #getFieldDelimiter()
    * @see #setFieldDelimiter(String)
    */
   protected String fieldDelimiter = "\t";
   /**
    * Getter for {@link #fieldDelimiter}: CSV field delimiter - the tab character by default,
    * despite the C in CSV standing for 'comma', because this format is intended for testing and 
    * so has to be as human-readible as possible.
    * @return CSV field delimiter - the tab character by default, despite the C in CSV standing 
    * for 'comma', because this format is intended for testing and so has to be as human-readible
    * as possible.
    */
   public String getFieldDelimiter() { return fieldDelimiter; }
   /**
    * Setter for {@link #fieldDelimiter}: CSV field delimiter - the tab character by default, 
    * despite the C in CSV standing for 'comma', because this format is intended for testing and
    * so has to be as human-readible as possible.
    * @param sNewFieldDelimiter CSV field delimiter - the tab character by default, despite the 
    * C in CSV standing for 'comma', because this format is intended for testing and so has to be
    * as human-readible as possible.
    */
   public void setFieldDelimiter(String sNewFieldDelimiter) { fieldDelimiter = sNewFieldDelimiter; }

   /**
    * Collection of layer data discovered in the CSV files (keyed by layer name) if any.
    */
   protected HashMap<String,Layer> mDiscoveredLayers = new HashMap<String,Layer>();

   /**
    * Layer schema.
    */
   protected Schema s;

   /** Data from CSV files, keyed on layer name (or "anchor" in the case of anchors) */
   protected LinkedHashMap<String, Vector<CSVRecord>> mCsvData = new LinkedHashMap<String, Vector<CSVRecord>>();

   /**
    * Sets parameters for deserializer as a whole.  This might include database connection
    * parameters, locations of supporting files, etc.
    * <p>When the deserializer is installed, this method should be invoked with an empty parameter
    * set, to discover what (if any) general configuration is required. If parameters are
    * returned, and user interaction is possible, then the user may be presented with an interface
    * for setting/confirming these parameters.  
    * @param configuration The configuration for the deserializer. 
    * @param schema The layer schema, definining layers and the way they interrelate.
    * @return A list of configuration parameters (still) must be set before
    * {@link IDeserializer#setParameters(ParameterSet)} can be invoked. If this is an empty list,
    * {@link IDeserializer#setParameters(ParameterSet)} can be invoked. If it's not an empty list,
    * this method must be invoked again with the returned parameters' values set.
    */
   public ParameterSet configure(ParameterSet configuration, Schema schema)
   {
      s = schema;
      if (!configuration.containsKey("fieldDelimiter"))
      {
	 configuration.addParameter(
	    new Parameter("fieldDelimiter", String.class, "Field delimiter for CSV files", 
			  "Field delimiter for CSV files, e.g. \",\" or tab", true, "\t"));
	 return configuration;
      }
      else
      {
	 try {configuration.get("fieldDelimiter").apply(this);} 
	 catch(Exception exception) {}
      }
      return new ParameterSet(); // all done
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
      if (getFieldDelimiter() == null) throw new SerializerNotConfiguredException("fieldDelimiter must be set.");
      ParameterSet parameters = new ParameterSet();

      // take the first csv stream, ignore all others.
      NamedStream csv = Utility.FindSingleStream(streams, ".csv", "text/csv");
      if (csv == null) throw new SerializationException("No CSV stream found");
      setName(csv.getName());
      setName(getName().replaceFirst("\\.csv$","").replaceFirst("\\.ag$",""));
      
      reset();
      
      CSVParser parser = new CSVParser(
	 new InputStreamReader(
	    csv.getStream()), CSVFormat.EXCEL.withDelimiter(fieldDelimiter.charAt(0)));
      mDiscoveredLayers = new HashMap<String,Layer>();
      Vector<CSVRecord> vRecords = new Vector<CSVRecord>();
      mCsvData.put("anchor", vRecords); // start with anchors

      // read all the lines, and extract the layer names
      for (CSVRecord line : parser)
      {
	 // does it have only one field? - the layer name
	 if (line.get(0).equals("layer"))
	 {
	    Layer layer = new Layer(line.get(1), line.get(2), Integer.parseInt(line.get(5)), 
				    true, // peers
				    false, // peersOverlap
				    false, // saturated
				    line.get(4).equals("W")?schema.getWordLayerId()  // parentId
				    :line.get(4).equals("M")?schema.getTurnLayerId() // parentId
				    :line.get(4).equals("F")?"graph":"segments",     // parentId
				    true); // parentIncludes
	    int layerId = Integer.parseInt(line.get(6));
	    if (layerId == 11) // turn
	    {
	       layer.setParentId(schema.getParticipantLayerId());
	    }
	    else if (layerId == 12) // utterance
	    {
	       layer.setSaturated(true);
	    }
	    else if (layerId == 0) // transcription
	    {
	       layer.setParentId(schema.getTurnLayerId());
	    }
	    else if (layerId == 2) // orthography
	    {
	       layer.setPeers(false);
	       layer.setSaturated(true);
	    }
	    else if (layerId == 1) // segments
	    {
	       layer.setSaturated(true);
	    }
	    layer.put("@layer_id", layerId);
	    layer.put("@type", line.get(3));
	    layer.put("@scope", line.get(4));
	    mDiscoveredLayers.put(line.get(1), layer);
	    Parameter p = new Parameter(layer.getId(), Layer.class, layer.getId(), layer.getDescription(), true);
	    p.setValue(schema.getLayer(layer.getId()));
	    p.setPossibleValues(schema.getLayers().values());
	    parameters.addParameter(p);

	    // start a new set of records
	    vRecords = new Vector<CSVRecord>();
	    mCsvData.put(layer.getId(), vRecords);
	 }
	 vRecords.add(line);
      } // next line
      parser.close();

      return parameters;
   }

   /**
    * Sets parameters for a given deserialization operation, after loading the serialized form of the graph. This might include mappings from format-specific objects like tiers to graph layers, etc.
    * @param parameters The configuration for a given deserialization operation.
    * @throws SerializationParametersMissingException If not all required parameters have a value.
    */
   public void setParameters(ParameterSet parameters) throws SerializationParametersMissingException
   {
      for (String layerId : mDiscoveredLayers.keySet())
      {
	 mDiscoveredLayers.put(layerId, (Layer)parameters.get(layerId).getValue());
      } // next layer
   }

   /**
    * Deserializes the serialized data, generating one or more {@link Graph}s.
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
      graph.addLayer((Layer)s.getParticipantLayer().clone());
      graph.getSchema().setParticipantLayerId(s.getParticipantLayer().getId());
      graph.addLayer((Layer)s.getTurnLayer().clone());
      graph.getSchema().setTurnLayerId(s.getTurnLayer().getId());
      graph.addLayer((Layer)s.getUtteranceLayer().clone());
      graph.getSchema().setUtteranceLayerId(s.getUtteranceLayer().getId());
      graph.addLayer((Layer)s.getWordLayer().clone());
      graph.getSchema().setWordLayerId(s.getWordLayer().getId());
      for (String layerId : mDiscoveredLayers.keySet())
      {
	 if (mDiscoveredLayers.get(layerId) != null)
	 {
	    graph.addLayer((Layer)mDiscoveredLayers.get(layerId).clone());
	 }
      } // next layer
      
      // anchors
      for (CSVRecord line : mCsvData.get("anchor"))
      {
	 if (line.get(1).equals("offset")) continue; // skip header line
	 Anchor anchor = new Anchor(line.get(0), new Double(line.get(1)), 
				    new Integer(line.get(2)));
	 graph.addAnchor(anchor);
	 if (line.size() > 3)
	 {
	    String comment = line.get(3);
	    if (comment.length() > 0)
	    {
	       anchor.put("comment", comment);
	    }
	 }
      } // next anchor
      mCsvData.remove("anchor");

      // layers
      for (String originalId : mCsvData.keySet())
      {
	 if (mDiscoveredLayers.get(originalId) != null)
	 { // mapped to a schema layer
	    try
	    {
	       readAnnotations(mCsvData.get(originalId), mDiscoveredLayers.get(originalId), graph);
	    }
	    catch(SerializationException exception)
	    {
	       if (errors == null)
	       {
		  errors = exception;
	       }
	       else
	       {
		  errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
	       }
	    }
	 } // mapped to a schema layer
      } // next layer

      if (errors != null) throw errors;
      Graph[] graphs = { graph };
      return graphs;
   }
   
   /**
    * Create annotations from the given CSV rows.
    * @param lines CSV records.
    * @param layer Layer for the annotations.
    * @param graph Graph to add the annotations to.
    * @throws SerializationException On error.
    */
   public void readAnnotations(Vector<CSVRecord> lines, Layer layer, Graph graph)
    throws SerializationException
   {
      // map header columns
      HashMap<String,Integer> mHeadings = new HashMap<String,Integer>();
      for (int c = 0; c < lines.elementAt(1).size(); c++)
      {
	 String sHeader = lines.elementAt(1).get(c);
	 if (sHeader.equalsIgnoreCase("id")) mHeadings.put("id",c);
	 else if (sHeader.equalsIgnoreCase("startAnchor.id")) mHeadings.put("startAnchor.id", c);
	 else if (sHeader.equalsIgnoreCase("endAnchor.id"))  mHeadings.put("endAnchor.id", c);
	 else if (sHeader.equalsIgnoreCase("label"))  mHeadings.put("label", c);
	 else if (sHeader.equalsIgnoreCase("labelStatus"))  mHeadings.put("labelStatus", c);
	 else if (sHeader.equalsIgnoreCase("turnAnnotationId"))  mHeadings.put("turnAnnotationId", c);
	 else if (sHeader.equalsIgnoreCase("ordinalInTurn"))  mHeadings.put("ordinalInTurn", c);
	 else if (sHeader.equalsIgnoreCase("wordAnnotationId"))  mHeadings.put("wordAnnotationId", c);
	 else if (sHeader.equalsIgnoreCase("ordinalInWord"))  mHeadings.put("ordinalInWord", c);
	 else if (sHeader.equalsIgnoreCase("segmentAnnotationId"))  mHeadings.put("segmentAnnotationId", c);
      } // next header
      int highestHeaderIndex = 0;
      for (Integer i : mHeadings.values()) highestHeaderIndex = Math.max(highestHeaderIndex, i);
      mHeadings.put("comment", highestHeaderIndex + 1);

      for (int i = 2; i < lines.size(); i++)
      {
	 CSVRecord line = lines.elementAt(i);
	 Annotation annotation = new Annotation(
	    line.get(mHeadings.get("id")), 
	    line.get(mHeadings.get("label")), 
	    layer.getId(), 
	    line.get(mHeadings.get("startAnchor.id")), 
	    line.get(mHeadings.get("endAnchor.id")));
	 annotation.setConfidence(new Integer(line.get(mHeadings.get("labelStatus"))));
	 if (mHeadings.get("comment") < line.size())
	 {
	    String comment = line.get(mHeadings.get("comment"));
	    if (comment.length() > 0)
	    {
	       annotation.put("comment", comment);
	    }
	 }

	 // parent
	 if (layer.getParentId().equals("graph"))
	 {
	    annotation.setParentId(graph.getId());
	 }
	 else if (layer.getParentId().equals(graph.getSchema().getTurnLayerId()))
	 {
	    if (layer.getId().equals(graph.getSchema().getUtteranceLayerId()))
	    {
	       // make sure turn exists
	       Annotation turn = graph.getAnnotation(line.get(mHeadings.get("turnAnnotationId")));
	       if (turn == null)
	       {
		  
		  // make sure participant exists
		  Annotation participant = graph.getAnnotation(annotation.getLabel());
		  if (participant == null)
		  {
		     participant = new Annotation(
			annotation.getLabel(), annotation.getLabel(), 
			graph.getSchema().getParticipantLayerId());
		     graph.addAnnotation(participant);
		  }

		  turn = new Annotation(
		     line.get(mHeadings.get("turnAnnotationId")), annotation.getLabel(), 
		     graph.getSchema().getTurnLayerId(), 
		     // start/end IDs are set, but the anchor's themselves aren't added
		     line.get(mHeadings.get("turnAnnotationId")) + " start", 
		     line.get(mHeadings.get("turnAnnotationId")) + " end",
		     participant.getId());
		  graph.addAnnotation(turn);
	       } // turn isn't there
	    } // utterance layer
	    annotation.setParentId(line.get(mHeadings.get("turnAnnotationId")));
	 }
	 else if (layer.getParentId().equals(graph.getSchema().getWordLayerId()))
	 {
	    annotation.setParentId(line.get(mHeadings.get("wordAnnotationId")));
	 }
	 else if (layer.getParentId().equals("segments"))
	 {
	    annotation.setParentId(line.get(mHeadings.get("segmentAnnotationId")));
	 }
	 else if (layer.getId().equals(graph.getSchema().getTurnLayerId()))
	 { // turn layer
	    // make sure participant exists
	    Annotation participant = graph.getAnnotation(annotation.getLabel());
	    if (participant == null)
	    {
	       participant = new Annotation(
		  annotation.getLabel(), annotation.getLabel(), 
		  graph.getSchema().getParticipantLayerId());
	       graph.addAnnotation(participant);
	    }
	    annotation.setParentId(participant.getId());
	 }

	 // ordinal
	 if (layer.getId().equals(graph.getSchema().getWordLayerId()))
	 {
	    annotation.setOrdinal(Integer.parseInt(line.get(mHeadings.get("ordinalInTurn"))));
	 }
	 else if (layer.getId().equals("segments"))
	 {
	    annotation.setOrdinal(Integer.parseInt(line.get(mHeadings.get("ordinalInWord"))));
	 }
	 graph.addAnnotation(annotation);
      }
   } // end of readAnnotations()


   /**
    * Returns any warnings that may have arisen during the last execution of {@link #deserialize()}.
    * @return A possibly empty lilayersst of warnings.
    */
   public String[] getWarnings()
   {
      return warnings.toArray(new String[0]);
   }

} // end of class AgCsvDeserializer
