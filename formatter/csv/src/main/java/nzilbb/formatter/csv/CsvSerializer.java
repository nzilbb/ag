//
// Copyright 2019-2024 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.formatter.csv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nzilbb.ag.*;
import nzilbb.ag.serialize.GraphSerializer;
import nzilbb.ag.serialize.SerializationDescriptor;
import nzilbb.ag.serialize.SerializationException;
import nzilbb.ag.serialize.SerializationParametersMissingException;
import nzilbb.ag.serialize.SerializerNotConfiguredException;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.util.IO;
import org.apache.commons.csv.*;

/**
 * Converter that generates CSV files from annotation graphs.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class CsvSerializer implements GraphSerializer {
   
  // Attributes:
   
  /** Static format so we don't keep creating and destroying one */
  private static DecimalFormat fmt = new DecimalFormat(
    // force the locale to something with . as the decimal separator
    "0.000", new DecimalFormatSymbols(Locale.UK));
   
  protected Vector<String> warnings;
  /**
   * Returns any warnings that may have arisen during the last execution of
   * {@link #serialize(Spliterator,String[],Consumer,Consumer,Consumer)}.
   * <p>{@link GraphSerializer} method.
   * @return A possibly empty list of warnings.
   */
  public String[] getWarnings() {
    return warnings.toArray(new String[0]);
  }
   
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
  public CsvSerializer setSchema(Schema newSchema) { schema = newSchema; return this; }

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
  public CsvSerializer setParticipantLayer(Layer newParticipantLayer) { participantLayer = newParticipantLayer; return this; }
   
  /**
   * Minimum confidence for anchor offsets. Offsets with lower confidence will not be serialized.
   * @see #getMinimumAnchorConfidence()
   * @see #setMinimumAnchorConfidence(Integer)
   */
  protected Integer minimumAnchorConfidence = Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC);
  /**
   * Getter for {@link #minimumAnchorConfidence}: Minimum confidence for anchor offsets. Offsets with lower confidence will not be serialized.
   * @return Minimum confidence for anchor offsets. Offsets with lower confidence will not be serialized.
   */
  public Integer getMinimumAnchorConfidence() { return minimumAnchorConfidence; }
  /**
   * Setter for {@link #minimumAnchorConfidence}: Minimum confidence for anchor offsets. Offsets with lower confidence will not be serialized.
   * @param newMinimumAnchorConfidence Minimum confidence for anchor offsets. Offsets with lower confidence will not be serialized.
   */
  public CsvSerializer setMinimumAnchorConfidence(Integer newMinimumAnchorConfidence) {
    minimumAnchorConfidence = newMinimumAnchorConfidence; return this;
  }

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
  public CsvSerializer setCancelling(boolean newCancelling) { cancelling = newCancelling; return this; }
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
  public CsvSerializer() {
  } // end of constructor

  /**
   * Returns the serializer's descriptor.
   * <p>{@link GraphSerializer} method.
   * @return The serializer's descriptor
   */
  public SerializationDescriptor getDescriptor() {
    return new SerializationDescriptor(
      "Comma Separated Values", getClass().getPackage().getImplementationVersion(),
      "text/csv", ".csv", "1.2.2", getClass().getResource("icon.png"));
  }

  /**
   * Sets parameters for deserializer as a whole.  This might include database connection
   * parameters, locations of supporting files, etc. 
   * <p>When the deserializer is installed, this method should be invoked with an empty parameter
   *  set, to discover what (if any) general configuration is required. If parameters are
   *  returned, and user interaction is possible, then the user may be presented with an
   *  interface for setting/confirming these parameters.
   * <p>{@link GraphSerializer} method.
   * @param configuration The configuration for the deserializer. 
   * @param schema The layer schema, definining layers and the way they interrelate.
   * @return A list of configuration parameters (still) must be set before {@link GraphSerializer#getRequiredLayers()} can be invoked. If this is an empty list, {@link GraphSerializer#getRequiredLayers()} can be invoked. If it's not an empty list, this method must be invoked again with the returned parameters' values set.
   */
  public ParameterSet configure(ParameterSet configuration, Schema schema) {
    setSchema(schema);
    setParticipantLayer(schema.getParticipantLayer());
    
    // set any values that have been passed in
    for (Parameter p : configuration.values()) try { p.apply(this); } catch(Exception x) {}

    if (!configuration.containsKey("minimumAnchorConfidence")) {
      configuration.addParameter(
        new Parameter(
          "minimumAnchorConfidence", Integer.class, 
          "Min. Anchor Confidence",
          "Minimum confidence for an annotation's offset before it's included"
          +" in the CSV file. Generally, if set to 100, only manually set offsets "
          +" will be output, if set to 50, manually set and automatically aligned"
          +" offsets with be output. Specify 0 for all offsets regardless of reliability,"
          +" and nothing/null for no offsets.", true));
    }
    if (configuration.get("minimumAnchorConfidence").getValue() == null) {
      configuration.get("minimumAnchorConfidence").setValue(getMinimumAnchorConfidence());
    }

    return configuration;
  }   

  /**
   * Determines which layers, if any, must be present in the graph that will be serialized.
   * <p>{@link GraphSerializer} method.
   * @return A list of IDs of layers that must be present in the graph that will be serialized.
   * @throws SerializationParametersMissingException If not all required parameters have a value.
   */
  public String[] getRequiredLayers() throws SerializationParametersMissingException {
    return new String[0];
  }

  /**
   * Determines the cardinality between graphs and serialized streams.
   * @return {@link GraphSerializer.Cardinality}.NtoOne as there is one stream produced
   * regardless of  how many graphs are serialized.
   */
  public Cardinality getCardinality() {
    return Cardinality.NToOne;
  }

  // GraphSerializer method
   
  /**
   * Serializes the given graph, generating one or more {@link NamedStream}s.
   * <p>Many data formats will only yield one stream (e.g. Transcriber transcript or Praat
   *  textgrid), however there are formats that use multiple files for the same transcript
   *  (e.g. XWaves, EmuR), which is why this method returns a list. There are formats that
   *  are capable of storing multiple transcripts in the same file (e.g. AGTK, Transana XML
   *  export), which is why this method accepts a list.
   * @param graphs The graphs to serialize.
   * @param layerIds The IDs of the layers to include, or null for all layers.
   * @param consumer The object receiving the streams.
   * @param warnings The object receiving warning messages.
   * @throws SerializerNotConfiguredException if the object has not been configured.
   */
  public void serialize(
    Spliterator<Graph> graphs, String[] layerIds, Consumer<NamedStream> consumer,
    Consumer<String> warnings, Consumer<SerializationException> errors) 
    throws SerializerNotConfiguredException {
      
    graphCount = graphs.getExactSizeIfKnown();
    
    // create a pipe for CSV data
    try {
      final PipedInputStream in = new PipedInputStream();
      final PipedOutputStream out = new PipedOutputStream(in);
      final StringBuffer fileName = new StringBuffer();
      
      final CSVPrinter csv = new CSVPrinter(
        new OutputStreamWriter(out, "UTF-8"), CSVFormat.EXCEL);
      
      csv.print("graph");
      Vector<Layer> attributeLayers = new Vector<Layer>();
      Vector<Layer> temporalLayers = new Vector<Layer>();
      for (String layerId : layerIds) {
        Layer layer = schema.getLayer(layerId);
        if (layer != null) {
          if ((layer.getParentId().equals(schema.getRoot().getId())
               || layer.getParentId().equals(schema.getParticipantLayerId()))
              && layer.getAlignment() == Constants.ALIGNMENT_NONE) {
            attributeLayers.add(layer);
          } else {
            temporalLayers.add(layer);
          }
        }
      } // next layerId

      // there will be an attribute layer value, but no offsets, for every CSV row
      for (Layer layer : attributeLayers) {
        csv.print(layer.getId());
      }

      // there will be one temporal layer/value per CSV row
      for (Layer layer : temporalLayers) {
        csv.print(layer.getId());
        if (minimumAnchorConfidence != null) {
          switch (layer.getAlignment()) {
            case Constants.ALIGNMENT_INSTANT:
              csv.print(layer.getId() + " offset");
              break;
            default: // INTERVAL and NONE
              csv.print(layer.getId() + " start");
              csv.print(layer.getId() + " end");
              break;
          }
        }
      } // next temporal layer
      csv.println();
      graphs.forEachRemaining(graph -> {
          if (getCancelling()) return;
          if (fileName.length() == 0) {
            fileName.append(IO.WithoutExtension(graph.getId()));
            if (graphCount != 1) { // only one graph, so we know the final name of the stream
              fileName.append("-etc");
            }
            new Thread(() -> {
                consumer.accept(
                  new NamedStream(in, fileName+".csv", "text/csv"));
            }).start();
          }

          try {
            // for each anchor in offset/structure order
            for (Anchor anchor : graph.getAnchorsOrderedByStructure()) {
              // are there any interesting annotations starting here?
              Optional<LinkedHashSet<Annotation>> annotationsStartingHere
                = temporalLayers.stream()
                .map(layer->anchor.startOf​(layer.getId()))
                .filter(annotations->annotations.size() > 0)
                .findAny();
              if (annotationsStartingHere.isPresent()) {
                Annotation annotationStartingHere = annotationsStartingHere.get()
                  .iterator().next();
                // start the row with graph ID and any attributes
                
                // graph ID
                csv.print(graph.getId());
                
                // all the attribute layers
                for (Layer attributeLayer : attributeLayers) {
                  try {
                    csv.print(
                      // multiple values are represented with multiple lines in the cell
                      annotationStartingHere.every(attributeLayer.getId())
                      // convert to a stream of labels
                      .map(a -> a.getLabel())
                      // and concatenate them all together into a multi-line string
                      .collect(Collectors.joining("\n")));
                  } catch(NullPointerException exception) {
                    csv.print("");
                  }
                } // next attribute layer
                
                // now output annotations that start here, or blank columns
                for (Layer columnLayer : temporalLayers) {
                  LinkedHashSet<Annotation> startsHere = anchor.startOf​(
                    columnLayer.getId());
                  if (startsHere.size() > 0) {
                    // if there's more than one, concatenate their labels
                    String label = startsHere.stream()
                      .map(annotation->annotation.getLabel())
                      .collect(Collectors.joining("\n"));
                    csv.print(label);
                    if (minimumAnchorConfidence != null) {
                      // use offsets of the first one
                      Annotation annotation = startsHere.iterator().next();
                      switch (columnLayer.getAlignment()) {
                        case Constants.ALIGNMENT_INSTANT:
                          csv.print(offset(annotation.getStart()));
                          break;
                        default: // INTERVAL and NONE
                          csv.print(offset(annotation.getStart()));
                          csv.print(offset(annotation.getEnd()));
                          break;
                      }
                    }
                  } else { // no annotation, output empty cells
                    csv.print("");
                    if (minimumAnchorConfidence != null) {
                      switch (columnLayer.getAlignment()) {
                        case Constants.ALIGNMENT_INSTANT:
                          csv.print("");
                          break;
                        default: // INTERVAL and NONE
                          csv.print("");
                          csv.print("");
                          break;
                      }
                    }
                  }
                } // next temporal layer
                csv.println();
              } // annotations start here
            } // next anchor
            consumedGraphCount++;
          } catch(Exception exception) {
            errors.accept(new SerializationException(exception));
          }
        }); // next graph
      if (fileName.length() == 0) { // there were no graphs
        errors.accept(new SerializationException("There was nothing to serialize."));
      }
      csv.flush();
      out.close();
    } catch(Exception exception) {
      errors.accept(new SerializationException(exception));
      System.out.println(""+exception);
    }
  }
   
  /**
   * Returns the anchor's offset, or an empty string.
   * @param anchor The anchor to represent.
   * @return A string representing the offset, or an empty string if the offset is not
   * set, or it's below {@link #minimumAnchorConfidence}. 
   */
  public String offset(Anchor anchor) {
    if (anchor == null) return "";
    if (anchor.getOffset() == null) return "";
    if (minimumAnchorConfidence != null && anchor.getConfidence() > 0) {
      if (anchor.getConfidence() == null) return "";
      if (anchor.getConfidence() < minimumAnchorConfidence) return "";
    }
    return anchor.getOffset().toString();
  } // end of offset()

} // end of class CsvSerializer
