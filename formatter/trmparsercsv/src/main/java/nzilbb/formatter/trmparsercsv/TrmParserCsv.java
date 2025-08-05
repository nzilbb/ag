//
// Copyright 2025 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.formatter.trmparsercsv;

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

// TODO graph parsing, maybe with: https://graphstream-project.org/doc/Install/

/**
 * Generates CSV files specificially for export/import of data for the
 * <a href="https://github.com/connor-taylorbrown/trm-parser">trm-parser</a>
 * implemented by <a href="https://github.com/connor-taylorbrown">Connor Talyor-Brown</a>
 * for Māori data.
 * <p> When serializing utterances, the following transformations are made:
 * <ul>
 *  <li>Vowels with umlauts or followed by a colon are macronized.</li>
 *  <li>English words are enclosed in square brackets. </li>
 *  <li>Utterances are split on full stops, creating two utterances. </li>
 * </ul>
 * <p> A CSV file is generated with the folling columns:
 * <ul>
 *  <li><q>Document</q> - the transcript ID</li>
 *  <li><q>Speaker</q> - the participant ID</li>
 *  <li><q>ID</q> - the unique identifier for the utterance</li>
 *  <li><q>Utterance</q> - transcript text ending in a punctuation mark or newline</li>
 * </ul>
 * @author Robert Fromont robert@fromont.net.nz
 */
public class TrmParserCsv implements GraphSerializer {
   
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
  public TrmParserCsv setSchema(Schema newSchema) {
    schema = newSchema;
    return this;
  }
  
  /**
   * Layer that partitions tokens into utterances.
   * @see #getChunkLayer()
   * @see #setChunkLayer(Layer)
   */
  protected Layer chunkLayer;
  /**
   * Getter for {@link #chunkLayer}: Layer that partitions tokens into utterances.
   * @return Layer that partitions tokens into utterances.
   */
  public Layer getChunkLayer() { return chunkLayer; }
  /**
   * Setter for {@link #chunkLayer}: Layer that partitions tokens into utterances.
   * @param newChunkLayer Layer that partitions tokens into utterances.
   */
  public TrmParserCsv setChunkLayer(Layer newChunkLayer) { chunkLayer = newChunkLayer; return this; }
  
  /**
   * Layer that tokens come from.
   * @see #getTokenLayer()
   * @see #setTokenLayer(Layer)
   */
  protected Layer tokenLayer;
  /**
   * Getter for {@link #tokenLayer}: Layer that tokens come from.
   * @return Layer that tokens come from.
   */
  public Layer getTokenLayer() { return tokenLayer; }
  /**
   * Setter for {@link #tokenLayer}: Layer that tokens come from.
   * @param newTokenLayer Layer that tokens come from.
   */
  public TrmParserCsv setTokenLayer(Layer newTokenLayer) { tokenLayer = newTokenLayer; return this; }

  /**
   * Layer that tags tokens in a different language.
   * @see #getLanguageLayer()
   * @see #setLanguageLayer(Layer)
   */
  protected Layer languageLayer;
  /**
   * Getter for {@link #languageLayer}: Layer that tags tokens in a different language.
   * @return Layer that tags tokens in a different language.
   */
  public Layer getLanguageLayer() { return languageLayer; }
  /**
   * Setter for {@link #languageLayer}: Layer that tags tokens in a different language.
   * @param newLanguageLayer Layer that tags tokens in a different language.
   */
  public TrmParserCsv setLanguageLayer(Layer newLanguageLayer) { languageLayer = newLanguageLayer; return this; }
  
  /**
   * Regular expression identifying tokens with a pause marker.
   * @see #getPauseMarkerPattern()
   * @see #setPauseMarkerPattern(String)
   */
  protected String pauseMarkerPattern;
  /**
   * Getter for {@link #pauseMarkerPattern}: Regular expression
   * identifying tokens with a pause marker. 
   * @return Regular expression identifying tokens with a pause marker.
   */
  public String getPauseMarkerPattern() { return pauseMarkerPattern; }
  /**
   * Setter for {@link #pauseMarkerPattern}: Regular expression
   * identifying tokens with a pause marker. 
   * @param newPauseMarkerPattern Regular expression identifying tokens with a pause marker.
   */
  public TrmParserCsv setPauseMarkerPattern(String newPauseMarkerPattern) { pauseMarkerPattern = newPauseMarkerPattern; return this; }  
  
  /**
   * An inter-word pause longer than this counts as the end of an utterance.
   * @see #getPauseSeconds()
   * @see #setPauseSeconds(Double)
   */
  protected Double pauseSeconds;
  /**
   * Getter for {@link #pauseSeconds}: An inter-word pause longer than
   * this counts as the end of an utterance. 
   * @return An inter-word pause longer than this counts as the end of an utterance.
   */
  public Double getPauseSeconds() { return pauseSeconds; }
  /**
   * Setter for {@link #pauseSeconds}: An inter-word pause longer than
   * this counts as the end of an utterance. 
   * @param newPauseSeconds An inter-word pause longer than this
   * counts as the end of an utterance. 
    */
   public TrmParserCsv setPauseSeconds(Double newPauseSeconds) { pauseSeconds = newPauseSeconds; return this; }

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
  public TrmParserCsv setCancelling(boolean newCancelling) { cancelling = newCancelling; return this; }
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
  public TrmParserCsv() {
  } // end of constructor

  /**
   * Returns the serializer's descriptor.
   * <p>{@link GraphSerializer} method.
   * @return The serializer's descriptor
   */
  public SerializationDescriptor getDescriptor() {
    return new SerializationDescriptor(
      "trm-parser CSV", getClass().getPackage().getImplementationVersion(),
      "text/trm-parser+csv", ".csv", "1.2.2", getClass().getResource("icon.svg"));
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

    chunkLayer = null;
    tokenLayer = null;
    languageLayer = null;
    pauseSeconds = null;
    pauseMarkerPattern = null;
    if (configuration.size() > 0) {
      configuration.apply(this);
    }

    LinkedHashMap<String,Layer> possibleChunkLayers = new LinkedHashMap<String,Layer>();
    possibleChunkLayers.put(schema.getTurnLayerId(), schema.getTurnLayer());
    for (Layer turnChild : schema.getTurnLayer().getChildren().values()) {
      if (turnChild.getAlignment() == Constants.ALIGNMENT_INTERVAL
          && !turnChild.getId().equals(schema.getWordLayerId())) {
        possibleChunkLayers.put(turnChild.getId(), turnChild);
      }
    } // next turn child layer
    Parameter pChunkLayer = configuration.containsKey("chunkLayer")?
      configuration.get("chunkLayer")
      :configuration.addParameter(
        new Parameter("chunkLayer", Layer.class, "Chunk layer",
                      "Layer that partitions tokens into utterances."));
    String[] possibilitiesC = {"sentence","clause","utterance","turn"};
    pChunkLayer.setValue(
      Utility.FindLayerById(possibleChunkLayers, Arrays.asList(possibilitiesC)));
    pChunkLayer.setPossibleValues(possibleChunkLayers.values());

    LinkedHashMap<String,Layer> possibleTokenLayers = new LinkedHashMap<String,Layer>();
    possibleTokenLayers.put(schema.getWordLayerId(), schema.getWordLayer());
    for (Layer wordChild : schema.getWordLayer().getChildren().values()) {
      if (wordChild.getAlignment() != Constants.ALIGNMENT_INSTANT
          && !wordChild.getId().equals("segment")) {
        possibleTokenLayers.put(wordChild.getId(), wordChild);
      }
    } // next turn child layer
    Parameter pTokenLayer = configuration.containsKey("tokenLayer")?
      configuration.get("tokenLayer")
      :configuration.addParameter(
        new Parameter("tokenLayer", Layer.class, "Token layer",
                      "Layer that tokens are taken from."));
    String[] possibilitiesT = {"token","word","orthography"};
    pTokenLayer.setValue(
      Utility.FindLayerById(possibleTokenLayers, Arrays.asList(possibilitiesT)));
    pTokenLayer.setPossibleValues(possibleTokenLayers.values());
    
    LinkedHashMap<String,Layer> possibleLanguageLayers = new LinkedHashMap<String,Layer>();
    for (Layer turnChild : schema.getTurnLayer().getChildren().values()) {
      if (turnChild.getAlignment() == Constants.ALIGNMENT_INTERVAL
          && !turnChild.getId().equals(schema.getWordLayerId())
          && !turnChild.getId().equals(schema.getUtteranceLayerId())) {
        possibleLanguageLayers.put(turnChild.getId(), turnChild);
      }
    } // next turn child layer
    Parameter pLanguageLayer = configuration.containsKey("languageLayer")?
      configuration.get("languageLayer")
      :configuration.addParameter(
        new Parameter("languageLayer", Layer.class, "Language layer",
                      "Layer that tags tokens in a different language."));
    String[] possibilitiesL = {"lang","language","codeswitch","cs"};
    pLanguageLayer.setValue(
      Utility.FindLayerById(possibleLanguageLayers, Arrays.asList(possibilitiesL)));
    pLanguageLayer.setPossibleValues(possibleLanguageLayers.values());

    Parameter pPauseSeconds = configuration.containsKey("pauseSeconds")?
      configuration.get("pauseSeconds")
      :configuration.addParameter(
        new Parameter(
          "pauseSeconds", Double.class, "Pause Threshold (seconds)",
          "An inter-word pause longer than this counts as the end of an utterance (0 to not break on pauses).",
          Double.valueOf(1.0)));

    Parameter pPauseMarkerPattern = configuration.containsKey("pauseMarkerPattern")?
      configuration.get("pauseMarkerPattern")
      :configuration.addParameter(
        new Parameter(
          "pauseMarkerPattern", String.class, "Pause Marker Pattern",
          "Regular expression identifying tokens with a pause marker, e.g. \".+[.-]\".",
          ".+[.-]"));

    return configuration;
  }   

  /**
   * Determines which layers, if any, must be present in the graph that will be serialized.
   * <p>{@link GraphSerializer} method.
   * @return A list of IDs of layers that must be present in the graph that will be serialized.
   * @throws SerializationParametersMissingException If not all required parameters have a value.
   */
  public String[] getRequiredLayers() throws SerializationParametersMissingException {
    if (languageLayer != null) {
      return new String[] { chunkLayer.getId(), tokenLayer.getId(), languageLayer.getId() };
    } else {
      return new String[] { chunkLayer.getId(), tokenLayer.getId() };
    }
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
      final double pauseThreshold = Optional.ofNullable(pauseSeconds).orElse(0.0);
      pauseMarkerPattern = Optional.ofNullable(pauseMarkerPattern).orElse("");
      
      final CSVPrinter csv = new CSVPrinter(
        new OutputStreamWriter(out, "UTF-8"), CSVFormat.EXCEL);
      
      // print headers
      csv.print("Document");
      csv.print("Speaker");
      csv.print("ID");
      csv.print("Utterance");
      
      graphs.forEachRemaining(graph -> {
          if (getCancelling()) return;
          if (fileName.length() == 0) {
            fileName.append(IO.WithoutExtension(graph.sourceGraph().getId()));
            if (graphCount != 1) { // only one graph, so we know the final name of the stream
              fileName.append("-etc");
            }
            new Thread(() -> {
                consumer.accept(
                  new NamedStream(in, fileName+".csv", "text/csv"));
            }).start();
          }

          // if it'a fragment, we need to adjust chunk the anchor offsets
          double offsetAdjustment = 0;
          if (graph.isFragment()) {
            try {
              String[] nameParts = graph.ParseFragmentId(graph.getId());
              offsetAdjustment = Double.parseDouble(nameParts[1]);
            } catch(Throwable t) {
              System.err.println("Error parsing fragment name " + graph.getId() + ": " + t);
            }
          }

          // create a chunk layer that divides utterances int parts on fullstops as required
          Layer tempLayer = (Layer)schema.getLayer(chunkLayer.getId()).clone();
          tempLayer.setId("@trm-parser-chunk");
          graph.getSchema().addLayer(tempLayer);
          
          try {
            for (Annotation utterance : graph.all(chunkLayer.getId())) {
              Anchor start = utterance.getStart();
              // look for tokens with full-stops
              Annotation previousChunk = null;
              boolean previousTokenWasLast = false;
              for (Annotation token : utterance.all(tokenLayer.getId())) {
                Annotation nextToken = token.getNext();
                if (pauseMarkerPattern.length() > 0
                    && token.getLabel().matches(pauseMarkerPattern)) {
                  // there's a pause marker
                  previousChunk = graph.createAnnotation(
                    start, token.getEnd(), tempLayer.getId(), utterance.getLabel(),
                    utterance.getParent());
                  start = token.getEnd();
                  previousTokenWasLast = true;
                } else if (pauseThreshold > 0.0
                           && nextToken != null
                           && token.getAnchored() && nextToken.getAnchored()
                           && nextToken.getStart().getOffset() - token.getEnd().getOffset()
                           >= pauseThreshold) {
                  // there's a pause following the token
                  previousChunk = graph.createAnnotation(
                    start, token.getEnd(), tempLayer.getId(), utterance.getLabel(),
                    utterance.getParent());
                  start = nextToken.getStart();
                  previousTokenWasLast = true;
                } else {                  
                  previousTokenWasLast = false;
                }
              } // next token
              if (previousTokenWasLast && previousChunk != null) {
                // we've just added a chunk, so don't add another
                previousChunk.setEnd(utterance.getEnd());
              } else { // there are leftover tokens
                // finish off last chunk
                graph.createAnnotation(
                  start, utterance.getEnd(), tempLayer.getId(), utterance.getLabel(),
                  utterance.getParent());
              }
            } // next utterance
            
            // there's one CSV row per chunk
            for (Annotation chunk : graph.all(tempLayer.getId())) {
              String text = chunk.every(tokenLayer.getId())
                .map(token -> standardize(token))
                .collect(Collectors.joining(" "))
                // eliminate excess spaces
                .replaceAll(" +"," ").trim();
              if (text.length() > 0) {
                csv.println();
                csv.print(graph.sourceGraph().getId()); // Document
                csv.print(chunk.getLabel());            // Speaker              
                csv.print(Graph.FragmentId​(             // ID
                            graph.sourceGraph(),
                            offsetAdjustment + chunk.getStart().getOffset(),
                            offsetAdjustment + chunk.getEnd().getOffset()));
                csv.print(text);
              }
            } // next utterance
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
      System.err.println(""+exception);
    }
  }
  
  /**
   * Transforms the token ino a label with a standardized form:
   * <ul>
   *  <li>Vowels with umlauts or followed by a colon are macronized.</li>
   *  <li>English words are enclosed in square brackets. </li>
   *  <li>Full-stops are surrounded by whitespace. </li>
   * </ul>
   * @param token The token annotation.
   * @return A standardized version of the given token's label.
   */
  public String standardize(Annotation token) {
    if (languageLayer != null && token.first(languageLayer.getId()) != null) {
      return "["+token.getLabel().trim()+"]";
    } 
    return token.getLabel()
      .replace("ä","ā").replace("ë","ē").replace("ï","ī").replace("ö","ō").replace("ü","ū")
      .replace("Ä","Ā").replace("Ë","Ē").replace("Ï","Ī").replace("Ö","Ō").replace("Ü","Ū")
      .replace("a:","ā").replace("e:","ē").replace("i:","ī").replace("o:","ō").replace("u:","ū")
      .replace("A:","Ā").replace("E:","Ē").replace("I:","Ī").replace("O:","Ō").replace("U:","Ū")
      // ensure space is isolated
      .replace(".", " . ");
  } // end of standardize()

} // end of class TrmParserCsv
