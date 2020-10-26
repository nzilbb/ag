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
package nzilbb.est;

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
 * Deserializer for <a href="http://festvox.org/docs/speech_tools-2.4.0/index.html">Edinburgh Speech Tools (EST)</a> Track files produced by EST, or other tools like <a href="https://github.com/google/REAPER">Reaper</a>.
 * <p><em>Track</em> files are ASCII encoded and start with a small number of header lines like this:
 * <pre>
 * EST_File Track
 * DataType ascii
 * NumFrames 60117
 * NumChannels 1
 * FrameShift 0.00000
 * VoicingEnabled true
 * EST_Header_End
 * </pre>
 * <p>After this, the rest of the file's lines consist of three columns like this:
 * <pre>
 * 0.000000 0 -1.000000
 * 0.005000 0 -1.000000
 * 0.010000 0 -1.000000
 * 0.015000 0 -1.000000
 * 0.020000 0 -1.000000
 * 0.025000 0 -1.000000
 * 0.030000 0 -1.000000
 * ...</pre>
 * <p>The columns are:
 * <ol>
 *  <li>The time in seconds</li>
 *  <li><samp>1</samp> (data present in third column) or <samp>0</samp> (ignore data in third columns)</li>
 *  <li>The label for the time point (e.g. in a Reaper .f0 file, it's the estimated value for F0 in Hz)</li>
 * </ol>
 * @author Robert Fromont robert@fromont.net.nz
 */
public class TrackDeserializer implements GraphDeserializer {
   
   // Attributes:
   protected Vector<String> warnings;

   /**
    * Name of the .cha file.
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
    * Setter for {@link #name}: Name of the .cha file.
    * @param newName Name of the .cha file.
    */
   public void setName(String newName) { name = newName; }
   
   /**
    * Data lines.
    * @see #getLines()
    * @see #setLines(Vector)
    */
   protected Vector<String> lines;
   /**
    * Getter for {@link #lines}: Data lines.
    * @return Data lines.
    */
   public Vector<String> getLines() { return lines; }
   /**
    * Setter for {@link #lines}: Data lines.
    * @param newLines Data lines.
    */
   public void setLines(Vector<String> newLines) { lines = newLines; }
      
   /**
    * Key/value pairs extracted from the header
    * @see #getHeader()
    * @see #setHeader(HashMap)
    */
   protected HashMap<String,String> header;
   /**
    * Getter for {@link #header}: Key/value pairs extracted from the header
    * @return Key/value pairs extracted from the header
    */
   public HashMap<String,String> getHeader() { return header; }
   /**
    * Setter for {@link #header}: Key/value pairs extracted from the header
    * @param newHeader Key/value pairs extracted from the header
    */
   public void setHeader(HashMap<String,String> newHeader) { header = newHeader; }

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
    * Layer for the annotations defined by the data lines.
    * @see #getAnnotationLayer()
    * @see #setAnnotationLayer(Layer)
    */
   protected Layer annotationLayer;
   /**
    * Getter for {@link #annotationLayer}: Layer for the annotations defined by the data lines.
    * @return Layer for the annotations defined by the data lines.
    */
   public Layer getAnnotationLayer() { return annotationLayer; }
   /**
    * Setter for {@link #annotationLayer}: Layer for the annotations defined by the data lines.
    * @param newAnnotationLayer Layer for the annotations defined by the data lines.
    */
   public void setAnnotationLayer(Layer newAnnotationLayer) { annotationLayer = newAnnotationLayer; }
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public TrackDeserializer() {
   } // end of constructor
   
   /**
    * Resete state.
    */
   public void reset() {
      warnings = new Vector<String>();
      lines = new Vector<String>();
      header = new HashMap<String,String>();
   } // end of reset()
   
   // IStreamDeserializer methods:

   /**
    * Returns the deserializer's descriptor
    * @return The deserializer's descriptor
    */
   public SerializationDescriptor getDescriptor() {
      return new SerializationDescriptor(
	 "EST Track", "0.5", "text/x-est-track", ".f0", "20200909.1954", getClass().getResource("icon.png"));
   }

   /**
    * Sets parameters for deserializer as a whole.  This might include database connection
    * parameters, locations of supporting files, etc.
    * <p>When the deserializer is installed, this method should be invoked with an empty parameter
    * set, to discover what (if any) general configuration is required. If parameters are returned,
    * and user interaction is possible, then the user may be presented with an interface for
    * setting/confirming these parameters. Once the parameters are set, this method can be
    * invoked again with the required values. 
    * @param schema The layer schema, definining layers and the way they interrelate.
    * @return A list of configuration parameters that must be set before
    * {@link GraphDeserializer#setParameters(ParameterSet)} can be invoked. If this is an empty list,
    * {@link GraphDeserializer#setParameters(ParameterSet)} can be invoked. If it's not an empty list,
    * this method must be invoked again with the returned parameters' values set.
    */
   public ParameterSet configure(ParameterSet configuration, Schema schema) {
      setSchema(schema);
      annotationLayer = null;
      return configuration;
   }

   /**
    * Loads the serialized form of the graph, using the given set of named streams.
    * @param streams A list of named streams that contain all the transcription/annotation data required.
    * @param schema The layer schema, definining layers and the way they interrelate.
    * @return A list of parameters that require setting before {@link GraphDeserializer#deserialize()} can be invoked. This may be an empty list, and may include parameters with the value already set to a workable default. If there are parameters, and user interaction is possible, then the user may be presented with an interface for setting/confirming these parameters, before they are then passed to {@link GraphDeserializer#setParameters(ParameterSet)}.
    * @throws SerializationException If the graph could not be loaded.
    * @throws IOException On IO error.
    * @throws SerializerNotConfiguredException If the configuration is not sufficient for deserialization.
    */
   @SuppressWarnings({"rawtypes", "unchecked"})
   public ParameterSet load(NamedStream[] streams, Schema schema)
      throws IOException, SerializationException, SerializerNotConfiguredException {
      reset();

      // take the first cha stream, ignore all others.
      NamedStream track = null;
      for (NamedStream stream : streams) {	 
	 if (stream.getName().toLowerCase().endsWith(".f0") 
	     || "text/x-est-track".equals(stream.getMimeType())) {
	    track = stream;
	    break;
	 }
      } // next stream
      if (track == null) throw new SerializationException("No EST Track stream found");
      setName(track.getName());      
      
      // read stream line by line
      boolean inHeader = true;
      BufferedReader reader = new BufferedReader(new InputStreamReader(track.getStream(), "UTF-8"));
      String line = reader.readLine();
      int headerLines = 0;
      while (line != null) {
	 if (inHeader) {
	    headerLines++;
	    if (line.equalsIgnoreCase("EST_Header_End")) {
	       inHeader = false;
	       // save the number of header lines
	       header.put("EST_Header_End", ""+headerLines);
	    } else {
	       // a key/value separated by whitespace something like:
	       // NumFrames 60117
	       String[] kv = line.split(" ");
	       if (kv.length < 2) kv = line.split("\t");
	       if (kv.length >= 2) {
		  header.put(kv[0], kv[1]);
	       }
	    }
	 } else { // data line
	    lines.add(line);
	 }
	 
	 line = reader.readLine();
      } // next line

      if (inHeader) {
	 throw new SerializationException("No EST_Header_End sentinel found");
      }
      if (!header.containsKey("EST_File")) {
	 throw new SerializationException("No EST_File header found");
      }
      if (!header.get("EST_File").equalsIgnoreCase("track")) {
	 throw new SerializationException("Expected file type \"Track\" but found \""+header.get("EST_File")+"\"");
      }
      ParameterSet mappings = new ParameterSet();

      HashMap<String,Layer> instantLayers = new HashMap<String,Layer>();
      for (Layer layer : getSchema().getLayers().values()) { // TODO got to be top-level layers
	 if (layer.getAlignment() == Constants.ALIGNMENT_INSTANT) {
	    // key by lowercase ID, so that matching is case-insensitive
	    instantLayers.put(layer.getId().toLowerCase(), layer);
	 }
      } // next layer
      Parameter p = new Parameter(
	 "labels", Layer.class, "Labels",
	 "Layer for annotation labels", true);
      for (String filePart : getName().split("\\.")) {
	 if (instantLayers.containsKey(filePart.toLowerCase())) {
	    p.setValue(instantLayers.get(filePart.toLowerCase()));
	    break;
	 }
      } // next part of the file/stream name
      p.setPossibleValues(instantLayers.values());
      mappings.addParameter(p);
      return mappings;
   }

   /**
    * Sets parameters for a given deserialization operation, after loading the serialized form of the graph. This might include mappings from format-specific objects like tiers to graph layers, etc.
    * @param parameters The configuration for a given deserialization operation.
    * @throws SerializationParametersMissingException If not all required parameters have a value.
    */
   public void setParameters(ParameterSet parameters)
      throws SerializationParametersMissingException {
      if (!parameters.containsKey("labels")) {
	 throw new SerializationParametersMissingException();
      }
      setAnnotationLayer((Layer)parameters.get("labels").getValue());
      if (getAnnotationLayer() == null) {
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
      throws SerializerNotConfiguredException, SerializationParametersMissingException,
      SerializationException {
      // if there are errors, accumlate as many as we can before throwing SerializationException
      SerializationException errors = null;

      Graph graph = new Graph();
      graph.setId(getName());
      // add layers to the graph
      // we don't just copy the whole schema, because that would imply that all the extra layers
      // contained no annotations, which is not necessarily true
      graph.addLayer((Layer)getAnnotationLayer().clone());
      String layerId = getAnnotationLayer().getId();
      
      // process lines...
      int l = Integer.parseInt(header.get("EST_Header_End"));
      int ordinal = 1;
      double lastOffset = Double.MIN_VALUE;
      for (String line : getLines()) {
	 l++;
	 String[] fields = line.split(" ");
	 if (fields.length < 3) fields = line.split("\t");
	 if (fields.length < 3) {
	    warnings.add("Invalid line " + l + ": only " + fields.length + " field"+(fields.length==1?"":"s"));
	 } else {
	    String time = fields[0];
	    String flag = fields[1];
	    String label = fields[2];
	    if (!flag.equals("0")) { // ignore lines flagged with 0
	       if (!flag.equals("1")) {
		  warnings.add("Line " + l + " unexpected flag: " + flag);
	       }
	       
	       try {
		  double offset = Double.parseDouble(time);
		  if (lastOffset >= offset) {
		     warnings.add("Line " + l + " out of order: time " + offset + " is not greater than previous time " + lastOffset);
		  }
		  lastOffset = offset;
	 	  Anchor anchor = graph.getOrCreateAnchorAt(offset, Constants.CONFIDENCE_MANUAL);
	 	  graph.addAnchor(anchor);
	 	  Annotation annotation = new Annotation(null, label, layerId, anchor.getId(), anchor.getId(), graph.getId(), ordinal++);
                  annotation.setConfidence(Constants.CONFIDENCE_MANUAL);
	 	  graph.addAnnotation(annotation);
	       } catch (NumberFormatException x) {
	 	  warnings.add("Invalid line " + l + ": invalid time " + time + " - " + x.getMessage());
	       }
	    } // flagged
	 } // there are enough fields
      } // next line
      if (errors != null) throw errors;
      Graph[] graphs = { graph };
      return graphs;
   }

   /**
    * Returns any warnings that may have arisen during the last execution of {@link #deserialize()}.
    * @return A possibly empty lilayersst of warnings.
    */
   public String[] getWarnings() {
      return warnings.toArray(new String[0]);
   }

} // end of class TrackDeserializer
