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
package nzilbb.ag.serialize.json;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Vector;
import java.util.LinkedHashMap;
import nzilbb.util.TempFileInputStream;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.ag.*;
import nzilbb.ag.serialize.*;
import nzilbb.ag.serialize.util.NamedStream;

/**
 * Annotation Graph serializer/deserializer for JSON.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class JSONSerialization
  implements ISerializer
{
   // Attributes:

   private DecimalFormat fmtOffset = new DecimalFormat(
      // force the locale to something with . as the decimal separator
      "0.0", new DecimalFormatSymbols(Locale.UK));

   
   /**
    * String used for indenting.
    * @see #getIndenter()
    * @see #setIndenter(String)
    */
   protected String indenter = "  ";
   /**
    * Getter for {@link #indenter}: String used for indenting.
    * @return String used for indenting.
    */
   public String getIndenter() { return indenter; }
   /**
    * Setter for {@link #indenter}: String used for indenting.
    * @param newIndenter String used for indenting.
    */
   public void setIndenter(String newIndenter) { indenter = newIndenter; }

   
   // Methods:
   
   /**
    * Default constructor.
    */
   public JSONSerialization()
   {
   } // end of constructor

   /**
    * Returns the deserializer's descriptor
    * @return The deserializer's descriptor
    */
   public SerializationDescriptor getDescriptor()
   {
      return new SerializationDescriptor(
	 "JSON (JavaScript Object Notation)", "0.1", "application/json", ".json");
   }

   /**
    * Sets parameters for serializer as a whole.  This might include database connection
    * parameters, locations of supporting files, standard layer mappings, etc.
    * <p>When the serializer is installed, this method should be invoked with an empty parameter
    *  set, to discover what (if any) general configuration is required. If parameters are
    *  returned, and user interaction is possible, then the user may be presented with an
    *  interface for setting/confirming these parameters. Once the parameters are set, this
    *  method can be invoked again with the required values, resulting in an empty parameter
    *  set being returned to confirm that nothing further is required.
    * @param configuration The general configuration for the serializer. 
    * @param schema The layer schema, definining layers and the way they interrelate.
    * @return A list of configuration parameters (still) must be set before
    *  {@link ISerializer#getRequiredLayers()} can be called. If this is an empty list,
    *  {@link ISerializer#getRequiredLayers()} can be called. If it's not an empty list,
    *  this method must be invoked again with the returned parameters' values set.
    */
   public ParameterSet configure(ParameterSet configuration, Schema schema)
   {
      return new ParameterSet();
   }

   /**
    * Determines which layers, if any, must be present in the graph that will be serialized.
    * @return A list of IDs of layers that must be present in the graph that will be serialized.
    * @throws SerializationParametersMissingException If not all required parameters have a value.
    */
   public String[] getRequiredLayers() throws SerializationParametersMissingException
   {
      return new String[0];
   }

   /**
    * Serializes the given graph, generating one or more {@link NamedStream}s.
    * <p>Many data formats will only yield one stream (e.g. Transcriber transcript or Praat
    *  textgrid), however there are formats that use multiple files for the same transcript
    *  (e.g. XWaves, EmuR), which is why this method returns a list. There are formats that
    *  are capable of storing multiple transcripts in the same file (e.g. AGTK, Transana XML
    *  export), which is why this method accepts a list.
    * @param graph The graph to serialize.
    * @return A list of named streams that contain the serialization in the given format. 
    * @throws SerializerNotConfiguredException if the object has not been configured.
    * @throws SerializationException if errors occur during deserialization.
    */
   public NamedStream[] serialize(Graph[] graphs) 
      throws SerializerNotConfiguredException, SerializationException
   {
      // if there are errors, accumlate as many as we can before throwing DeserializationException
      SerializationException errors = null;
      Vector<NamedStream> streams = new Vector<NamedStream>();
      for (Graph graph : graphs)
      {
	 try
	 {
	    File f = File.createTempFile(graph.getId(), ".json");
	    FileOutputStream out = new FileOutputStream(f);	 
	    PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "utf-8"));
	    
	    writer.println("{");
	    writer.println(keyValue(1, "id", graph.getId()));
	    
	    // layers
	    Schema schema = graph.getSchema();
	    writer.println(indent(1) + "\"schema\":{");
	    if (schema.getCorpusLayerId() != null)
	       writer.println(keyValue(2, "corpusLayerId", schema.getCorpusLayerId()));
	    if (schema.getEpisodeLayerId() != null)
	       writer.println(keyValue(2, "episodeLayerId", schema.getEpisodeLayerId()));
	    if (schema.getParticipantLayerId() != null)
	       writer.println(keyValue(2, "participantLayerId", schema.getParticipantLayerId()));
	    if (schema.getTurnLayerId() != null)
	       writer.println(keyValue(2, "turnLayerId", schema.getTurnLayerId()));
	    if (schema.getUtteranceLayerId() != null)
	       writer.println(keyValue(2, "utteranceLayerId", schema.getUtteranceLayerId()));
	    if (schema.getWordLayerId() != null)
	       writer.println(keyValue(2, "wordutteranceLayerId", schema.getWordLayerId()));
	    serializeLayer(writer, 2, schema.getRoot());
	    writer.println();
	    writer.println(indent(1) + "}");
	    
	    // anchors
	    LinkedHashMap<String,Anchor> anchors = graph.getAnchors();
	    writer.println(indent(1) + "\"anchors\":{");
	    for (Anchor anchor : anchors.values())
	    {
	       serializeAnchor(writer, 2, anchor);
	    } // next anchor
	    writer.println(indent(1) + "}");

	    for (String layerId : schema.getRoot().getChildren().keySet())
	    {
	       System.out.println(layerId + " " + graph.getAnnotations(layerId).size());
	       serializeAnnotations(writer, 1, layerId, graph.getAnnotations(layerId));
	    }
	    
	    writer.println();
	    writer.println("}");
	    
	    // provide a stream from the buffer
	    writer.close();
	    TempFileInputStream in = new TempFileInputStream(f);
	    streams.add(new NamedStream(in, graph.getId() + ".json"));
	 }
	 catch(Exception exception)
	 {
	    if (errors == null) errors = new SerializationException();
	    if (errors.getCause() == null) errors.initCause(exception);
	    errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
	 }
      } // next graph
      if (errors != null) throw errors;
      return streams.toArray(new NamedStream[0]);     
   }			

   
   /**
    * Recursively serializes the given layer's definition.
    * @param writer
    * @param indent
    * @param layer
    */
   public void serializeLayer(PrintWriter writer, int indent, Layer layer)
   {
      writer.println();
      writer.print(indent(indent) + q(layer.getId()) + ":{");
      writer.print(keyValue(0, "description", layer.getDescription()));
      if (layer.getParentId() != null)
      {
	 writer.print(" ");
	 writer.println(keyValue(0, "alignment", layer.getAlignment()));
	 writer.print(keyValue(indent+1, "peers", layer.getPeers()));
	 writer.print(" ");
	 writer.print(keyValue(0, "peersOverlap", layer.getPeersOverlap()));
	 writer.print(" ");
	 writer.print(keyValue(0, "parentIncludes", layer.getParentIncludes()));
	 writer.print(" ");
	 writer.print(keyValue(0, "saturated", layer.getSaturated()));
      }
      if (layer.getChildren().size() > 0)
      {
	 writer.println();
	 writer.print(indent(indent+1) + "\"children\":{");
	 for (Layer child : layer.getChildren().values())
	 {
	    serializeLayer(writer, indent+2, child);
	 } // next child
	 writer.println();
	 writer.print(indent(indent+1) + "}");
      }
      writer.print("},");
   } // end of serializeLayer()

   /**
    * Serializes the given anchor.
    * @param writer
    * @param indent
    * @param anchor
    */
   public void serializeAnchor(PrintWriter writer, int indent, Anchor anchor)
   {
      writer.print(indent(indent) + q(anchor.getId()) + ":\t{");
      writer.print(keyValue(0, "offset", anchor.getOffset()));
      if (anchor.containsKey(Constants.CONFIDENCE))
      {
	 writer.print("\t");
	 writer.print(keyValue(0, Constants.CONFIDENCE, (Integer)anchor.get(Constants.CONFIDENCE)));
      }
      writer.println("},");
   } // end of serializeLayer()

   /**
    * Serializes the given annotation.
    * @param writer
    * @param indent
    * @param anchor
    */
   public void serializeAnnotations(PrintWriter writer, int indent, String layerId, Vector<Annotation> annotations)
   {
      if (annotations.size() > 0)
      {
	 writer.print(indent(indent) + q(layerId) + ":[");
	 for (Annotation child : annotations) 
	 {
	    writer.println();
	    writer.print(indent(indent+1) + "{");
	    serializeAnnotation(writer, indent+2, child);
	    writer.print("},");
	 } // next child
	 writer.print("],");
      } // there really are children
   }

   /**
    * Serializes the given annotation.
    * @param writer
    * @param indent
    * @param anchor
    */
   public void serializeAnnotation(PrintWriter writer, int indent, Annotation annotation)
   {
      writer.print(keyValue(0, "id", annotation.getId()));
      writer.print("\t");
      writer.print(keyValue(0, "label", annotation.getLabel()));
      writer.print("\t");
      writer.print(keyValue(0, "startId", annotation.getStartId()));
      writer.print("\t");
      writer.print(keyValue(0, "endId", annotation.getEndId()));
      if (annotation.containsKey(Constants.CONFIDENCE))
      {
	 writer.print("\t");
	 writer.print(keyValue(0, Constants.CONFIDENCE, (Integer)annotation.get(Constants.CONFIDENCE)));
      }
      LinkedHashMap<String,Vector<Annotation>> childLayers = annotation.getAnnotations();
      for (String layerId : childLayers.keySet())
      {
	 Vector<Annotation> children = childLayers.get(layerId);
	 writer.println();
	 serializeAnnotations(writer, indent, layerId, children);
      } // next layer
   } // end of serializeLayer()

   
   /**
    * Expresses a key and simple string value as JSON.
    * @param indent
    * @param key
    * @param value
    * @return A string of the form {indent}"{key}":"{value}",
    */
   public String keyValue(int indent, String key, String value)
   {
      StringBuffer s = new StringBuffer();
      for (int i = 0; i < indent; i++) s.append(indenter);
      s.append(q(key));
      s.append(":");
      s.append(q(value));
      s.append(",");
      return s.toString();
   } // end of keyValue()

   /**
    * Expresses a key and simple boolean value as JSON.
    * @param indent
    * @param key
    * @param value
    * @return A string of the form {indent}{key} : {value},
    */
   public String keyValue(int indent, String key, boolean value)
   {
      StringBuffer s = new StringBuffer();
      for (int i = 0; i < indent; i++) s.append(indenter);
      s.append(q(key));
      s.append(":");
      s.append(value);
      s.append(",");
      return s.toString();
   } // end of keyValue()

   /**
    * Expresses a key and simple integer value as JSON.
    * @param indent
    * @param key
    * @param value
    * @return A string of the form {indent}{key} : {value},
    */
   public String keyValue(int indent, String key, int value)
   {
      StringBuffer s = new StringBuffer();
      for (int i = 0; i < indent; i++) s.append(indenter);
      s.append(q(key));
      s.append(":");
      s.append(value);
      s.append(",");
      return s.toString();
   } // end of keyValue()

   /**
    * Expresses a key and Double object value as JSON.
    * @param indent
    * @param key
    * @param value
    * @return A string of the form {indent}{key} : {value},
    */
   public String keyValue(int indent, String key, Double value)
   {
      StringBuffer s = new StringBuffer();
      for (int i = 0; i < indent; i++) s.append(indenter);
      s.append(q(key));
      s.append(":");
      if (value != null) 
	 s.append(fmtOffset.format(value));
      else
	 s.append("null");
      s.append(",");
      return s.toString();
   } // end of keyValue()

   
   /**
    * Write indent characters to the given level.
    * @param indent
    * @return A string with the indent character repeated the indent level number of times.
    */
   public String indent(int indent)
   {
      StringBuffer s = new StringBuffer();
      for (int i = 0; i < indent; i++) s.append(indenter);
      return s.toString();
   } // end of indent()

   
   /**
    * Escapes the given string for expression as a JSON string, and wraps it in quotes.
    * @param s
    * @return The given string, in quotes and escaped for JSON.
    */
   public String q(String s)
   {
      if (s == null) return "null";
      return "\"" 
	 + s.replaceAll("\\\\", "\\\\")
	 .replaceAll("\\\"", "\\\\\"")
	 .replaceAll("\t", "\\t")
	 .replaceAll("\b", "\\b")
	 .replaceAll("\r", "\\r")
	 .replaceAll("\n", "\\n")
	 .replaceAll("\f", "\\f")
	 + "\"";
   } // end of q()


   /**
    * Returns any warnings that may have arisen during the last execution of {@link #deserialize()}.
    * @return A possibly empty list of warnings.
    */
   public String[] getWarnings()
   {
      return new String[0];
   }


} // end of class JSONSerialization
