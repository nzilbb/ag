//
// Copyright 2020-2021 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.formatter.htk.mlf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import nzilbb.ag.*;
import nzilbb.ag.serialize.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;

/**
 * Deserializer for Master Label File (MLF) files produced by HTK.
 * <p> <em>.mlf</em> files are ASCII encoded and can contain multiple graphs and multiple layers:
 * <pre>
 * #!MLF!#
 * "&ast;/AP511_MikeThorpe.eaf_1.373_7.131.lab"
 * 0 700000 sil SILENCE
 * 700000 1900000 _# ah
 * 1900000 1900000 sp
 * 1900000 3300000 _w w~
 * 3300000 4000000 _@
 * 4000000 4000000 sp
 * 4000000 5700000 sil SILENCE
 * 5700000 6700000 _w well
 * 6700000 7000000 _E
 * 7000000 7300000 _l
 * 7300000 7300000 sp
 * 7300000 8300000 _2 i
 * 8300000 8300000 sp
 * 12400000 20100000 sil SILENCE
 * .
 * "&ast;/AP511_MikeThorpe.eaf_7.131_13.887.lab"
 * 0 500000 sil SILENCE
 * 500000 800000 _D the
 * 800000 1100000 _i
 * 1100000 1100000 sp
 * 1100000 2200000 _f first
 * 2200000 3100000 _3
 * 3100000 3500000 _s
 * 3500000 3800000 _t
 * 3800000 3800000 sp
 * 3800000 4100000 _w one
 * 4100000 4500000 _V
 * 4500000 5100000 _n
 * 5100000 5100000 sp
 * 5100000 5500000 _b being
 * 5500000 5900000 _i
 * 5900000 6200000 _I
 * 6200000 6900000 _N
 * 6900000 6900000 sp
 * ...</pre>
 * <p>The space-separated columns are:
 * <ol>
 *  <li>Start time (in 100ns)</li>
 *  <li>End time (in 100ns)</li>
 *  <li>The label for the phone </li>
 *  <li>The label for the word (optional) </li>
 * </ol>
 * @author Robert Fromont robert@fromont.net.nz
 */
public class MlfDeserializer implements GraphDeserializer {
   
  // Attributes:
  protected Vector<String> warnings;

  /**
   * The .mlf file stream.
   * @see #getMlf()
   * @see #setMlf(NamedStream)
   */
  protected NamedStream mlf;
  /**
   * Getter for {@link #mlf}: The .mlf file stream.
   * @return The .mlf file stream.
   */
  public NamedStream getMlf() { return mlf; }
  /**
   * Setter for {@link #mlf}: The .mlf file stream.
   * @param newMlf The .mlf file stream.
   */
  public MlfDeserializer setMlf(NamedStream newMlf) { mlf = newMlf; return this; }

  /**
   * Whether the P2FA 11,025Hz alignment correction should be applied (see P2FA's readme.txt).
   * @see #getUseP2FACorrection()
   * @see #setUseP2FACorrection(boolean)
   */
  protected boolean useP2FACorrection;
  /**
   * Getter for {@link #useP2FACorrection}: Whether the P2FA 11,025Hz alignment
   * correction should be applied (see P2FA's readme.txt). 
   * @return Whether the P2FA 11,025Hz alignment correction should be applied (see P2FA's
   * readme.txt). 
   */
  public boolean getUseP2FACorrection() { return useP2FACorrection; }
  /**
   * Setter for {@link #useP2FACorrection}: Whether the P2FA 11,025Hz alignment correction 
   * should be applied (see P2FA's readme.txt).  
   * @param newUseP2FACorrection Whether the P2FA 11,025Hz alignment correction should be
   * applied (see P2FA's readme.txt). 
   */
  public MlfDeserializer setUseP2FACorrection(boolean newUseP2FACorrection) { useP2FACorrection = newUseP2FACorrection; return this; }
   
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
  public MlfDeserializer setSchema(Schema newSchema) { schema = newSchema; return this;}
  
  /**
   * The layer for words. This is the schema word layer by default.
   * @see #getWordLayer()
   * @see #setWordLayer(Layer)
   */
  protected Layer wordLayer;
  /**
   * Getter for {@link #wordLayer}: The layer for words.
   * @return The layer for words.
   */
  public Layer getWordLayer() { return wordLayer; }
  /**
   * Setter for {@link #wordLayer}: The layer for words.
   * @param newWordLayer The layer for words.
   */
  public MlfDeserializer setWordLayer(Layer newWordLayer) { wordLayer = newWordLayer; return this; }

  /**
   * The layer for phones.
   * @see #getPhoneLayer()
   * @see #setPhoneLayer(Layer)
   */
  protected Layer phoneLayer;
  /**
   * Getter for {@link #phoneLayer}: The layer for phones.
   * @return The layer for phones.
   */
  public Layer getPhoneLayer() { return phoneLayer; }
  /**
   * Setter for {@link #phoneLayer}: The layer for phones.
   * @param newPhoneLayer The layer for phones.
   */
  public MlfDeserializer setPhoneLayer(Layer newPhoneLayer) { phoneLayer = newPhoneLayer; return this; }
   
  /**
   * The layer for confidence scores. 
   * <p> This must be set iff the MLF includes scores.
   * @see #getScoreLayer()
   * @see #setScoreLayer(Layer)
   */
  protected Layer scoreLayer;
  /**
   * Getter for {@link #scoreLayer}: The layer for confidence scores.
   * @return The layer for confidence scores.
   */
  public Layer getScoreLayer() { return scoreLayer; }
  /**
   * Setter for {@link #scoreLayer}: The layer for confidence scores.
   * <p> This must be set iff the MLF includes scores.
   * @param newScoreLayer The layer for confidence scores.
   */
  public MlfDeserializer setScoreLayer(Layer newScoreLayer) { scoreLayer = newScoreLayer; return this; }

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
  public MlfDeserializer setNoiseLayer(Layer newNoiseLayer) { noiseLayer = newNoiseLayer; return this; }

  /**
   * A set of labels that identify noise tokens.
   * <p> If there are noise intervals in the MLF, this must be set even if 
   * {@link #noiseLayer} is <em>not</em> set. Otherwise, the noise intervals will be
   * taken as words.
   * @see #getNoiseIdentifiers()
   * @see #setNoiseIdentifiers(Set)
   */
  protected Set<String> noiseIdentifiers = new HashSet<String>();
  /**
   * Getter for {@link #noiseIdentifiers}: A set of labels that identify noise tokens.
   * @return A set of labels that identify noise tokens.
   */
  public Set<String> getNoiseIdentifiers() { return noiseIdentifiers; }
  /**
   * Setter for {@link #noiseIdentifiers}: A set of labels that identify noise tokens.
   * @param newNoiseIdentifiers A set of labels that identify noise tokens.
   */
  public MlfDeserializer setNoiseIdentifiers(Set<String> newNoiseIdentifiers) { noiseIdentifiers = newNoiseIdentifiers; return this; }

  /**
   * Condfidence to assign to annotations.
   * @see #getAnnotationConfidence()
   * @see #setAnnotationConfidence(Integer)
   */
  protected Integer annotationConfidence = Constants.CONFIDENCE_AUTOMATIC;
  /**
   * Getter for {@link #annotationConfidence}: Condfidence to assign to annotations.
   * @return Condfidence to assign to annotations.
   */
  public Integer getAnnotationConfidence() { return annotationConfidence; }
  /**
   * Setter for {@link #annotationConfidence}: Condfidence to assign to annotations.
   * @param newAnnotationConfidence Condfidence to assign to annotations.
   */
  public MlfDeserializer setAnnotationConfidence(Integer newAnnotationConfidence) { annotationConfidence = newAnnotationConfidence; return this; }

  /**
   * Condfidence to assign to anchors.
   * @see #getAnchorConfidence()
   * @see #setAnchorConfidence(Integer)
   */
  protected Integer anchorConfidence = Constants.CONFIDENCE_AUTOMATIC;
  /**
   * Getter for {@link #anchorConfidence}: Condfidence to assign to anchors.
   * @return Condfidence to assign to anchors.
   */
  public Integer getAnchorConfidence() { return anchorConfidence; }
  /**
   * Setter for {@link #anchorConfidence}: Condfidence to assign to anchors.
   * @param newAnchorConfidence Condfidence to assign to anchors.
   */
  public MlfDeserializer setAnchorConfidence(Integer newAnchorConfidence) { anchorConfidence = newAnchorConfidence; return this; }

  // Methods:
   
  /**
   * Default constructor.
   */
  public MlfDeserializer() {
  } // end of constructor
   
  /**
   * Resete state.
   */
  public void reset() {
    warnings = new Vector<String>();
  } // end of reset()
   
  // IStreamDeserializer methods:

  /**
   * Returns the deserializer's descriptor
   * @return The deserializer's descriptor
   */
  public SerializationDescriptor getDescriptor() {
    return new SerializationDescriptor(
      "HTK Labels", getClass().getPackage().getImplementationVersion(),
      "text/x-htk+plain", ".mlf", "1.0.0",
      getClass().getResource("icon.png"));
  }

  /**
   * Sets parameters for deserializer as a whole.  This might include database connection
   * parameters, locations of supporting files, etc.
   * <p> When the deserializer is installed, this method should be invoked with an empty
   * parameter set, to discover what (if any) general configuration is required. If
   * parameters are returned, and user interaction is possible, then the user may be
   * presented with an interface for setting/confirming these parameters. Once the
   * parameters are set, this method can be invoked again with the required values. 
   * @param schema The layer schema, definining layers and the way they interrelate.
   * @return A list of configuration parameters that must be set before
   * {@link GraphDeserializer#setParameters(ParameterSet)} can be invoked. 
   * If this is an empty list,
   * {@link GraphDeserializer#setParameters(ParameterSet)} can be invoked. 
   * If it's not an empty list,
   * this method must be invoked again with the returned parameters' values set.
   */
  public ParameterSet configure(ParameterSet configuration, Schema schema) {
    setSchema(schema);

    // set any values that have been passed in
    for (Parameter p : configuration.values()) {
      if (p.getName().equals("noiseIdentifiersString")) {
        String[] noiseIdentifiersArray = p.getValue().toString().split(" ");
        noiseIdentifiers.clear();
        noiseIdentifiers.addAll(Arrays.asList(noiseIdentifiersArray));
      } else {
        try { p.apply(this); } catch(Exception x) {}
      }
    } // next parameters

    // validate
    if (phoneLayer != null && scoreLayer != null) {
      if (!scoreLayer.getParentId().equals(phoneLayer.getId())
          // but if the phone layer is a phrase layer, it's ok
          && !phoneLayer.getParentId().equals(schema.getTurnLayerId())) {
        // score layer must be a phone layer child
        scoreLayer = null;
        if (configuration.containsKey("scoreLayer")) {
          configuration.get("scoreLayer").setValue(null);
        }
      }
    }

    LinkedHashMap<String,Layer> topLevelLayers = new LinkedHashMap<String,Layer>();
    for (Layer top : schema.getRoot().getChildren().values()) {
         
      if (top.getAlignment() == Constants.ALIGNMENT_INTERVAL) {
        // aligned children of graph
        topLevelLayers.put(top.getId(), top);
      }
    } // next top level layer 
    LinkedHashMap<String,Layer> wordPartitionLayers = new LinkedHashMap<String,Layer>();
    LinkedHashMap<String,Layer> phoneTagLayers = new LinkedHashMap<String,Layer>();
    for (Layer layer : getSchema().getWordLayer().getChildren().values()) {
      if (layer.getAlignment() == Constants.ALIGNMENT_INTERVAL && layer.getPeers()) {
        // key by lowercase ID, so that matching is case-insensitive
        wordPartitionLayers.put(layer.getId().toLowerCase(), layer);

        // look for possible tags of this child layer
        for (Layer grandchild : layer.getChildren().values()) {
          if (grandchild.getAlignment() == Constants.ALIGNMENT_NONE) {
            // key by lowercase ID, so that matching is case-insensitive
            phoneTagLayers.put(grandchild.getId().toLowerCase(), grandchild);
          }
        }
      }
    } // next layer
    if (phoneLayer != null) { // if we already know the phone layer
      // ensure only child tags are available as scoreLayer options
      phoneTagLayers.clear();
      for (Layer grandchild : phoneLayer.getChildren().values()) {
        if (grandchild.getAlignment() == Constants.ALIGNMENT_NONE) {
          // key by lowercase ID, so that matching is case-insensitive
          phoneTagLayers.put(grandchild.getId().toLowerCase(), grandchild);
        }
      }
    }
    LinkedHashMap<String,Layer> phraseLayers = new LinkedHashMap<String,Layer>();
    for (Layer layer : getSchema().getTurnLayer().getChildren().values()) {
      if (layer.getAlignment() == Constants.ALIGNMENT_INTERVAL && layer.getPeers()
          && !layer.getId().equals(schema.getUtteranceLayerId())) {
        // key by lowercase ID, so that matching is case-insensitive
        phraseLayers.put(layer.getId().toLowerCase(), layer);
        // can be a phone layer too!
        wordPartitionLayers.put(layer.getId().toLowerCase(), layer);
        // and phone tag layer
        phoneTagLayers.put(layer.getId().toLowerCase(), layer);
      }
    } // next layer
     
    // create a list of layers we need and possible matching layer names
    LinkedHashMap<Parameter,List<String>> layerToPossibilities = new LinkedHashMap<Parameter,List<String>>();
    HashMap<String,LinkedHashMap<String,Layer>> layerToCandidates = new HashMap<String,LinkedHashMap<String,Layer>>();
      
    layerToPossibilities.put(
      new Parameter("noiseLayer", Layer.class, "Noise layer", "Noise annotations"), 
      Arrays.asList("noise","noises","backgroundnoise"));
    layerToCandidates.put("noiseLayer", topLevelLayers);
      
    layerToPossibilities.put(
      new Parameter("phoneLayer", Layer.class, "Phones layer", "Layer for aligned phones"), 
      Arrays.asList("phone","phones","segment", "segments"));
    layerToCandidates.put("phoneLayer", wordPartitionLayers);
      
    layerToPossibilities.put(
      new Parameter("wordLayer", Layer.class, "Words layer", "Layer for aligned words"), 
      Arrays.asList(schema.getWordLayerId(),"word","words"));
    layerToCandidates.put("wordLayer", phraseLayers);
      
    layerToPossibilities.put(
      new Parameter("scoreLayer", Layer.class, "Score layer", "Layer for HTK confidence score"), 
      Arrays.asList("score","scores","confidence"));
    layerToCandidates.put("scoreLayer", phoneTagLayers);
      
    // add parameters that aren't in the configuration yet, and set possibile/default values
    for (Parameter p : layerToPossibilities.keySet()) {
         
      List<String> possibleNames = layerToPossibilities.get(p);
      LinkedHashMap<String,Layer> candidateLayers = layerToCandidates.get(p.getName());
      if (configuration.containsKey(p.getName())) {
            
        p = configuration.get(p.getName());
      } else {            
        configuration.addParameter(p);
      }
      if (p.getValue() == null) {
        p.setValue(Utility.FindLayerById(candidateLayers, possibleNames));
      }
      p.setPossibleValues(candidateLayers.values());
    }
      
    // useP2FACorrection
    if (!configuration.containsKey("useP2FACorrection")) {
      configuration.addParameter(
        new Parameter("useP2FACorrection", Boolean.class, 
                      "P2FA 11,025Hz Correction",
                      "Whether the P2FA 11,025Hz alignment correction should be applied",
                      true));
    }
    if (configuration.get("useP2FACorrection").getValue() == null) {
      configuration.get("useP2FACorrection").setValue(Boolean.FALSE);
    }
      
    // noiseIdentifiers
    if (!configuration.containsKey("noiseIdentifiersString")) {
      configuration.addParameter(
        new Parameter("noiseIdentifiersString", String.class, 
                      "Noise identifiers",
                      "Space-separated list of noise labels",
                      true));
    }
    if (configuration.get("noiseIdentifiersString").getValue() == null) {
      configuration.get("noiseIdentifiersString").setValue("");
    }

    return configuration;
  }

  /**
   * Loads the serialized form of the graph, using the given set of named streams.
   * @param streams A list of named streams that contain all the transcription/annotation data
   * required.
   * @param schema The layer schema, definining layers and the way they interrelate.
   * @return A list of parameters that require setting before
   * {@link GraphDeserializer#deserialize()} can be invoked. This may be an empty list, and may
   * include parameters with the value already set to a workable default. If there are
   * parameters, and user interaction is possible, then the user may be presented with an
   * interface for setting/confirming these parameters, before they are then passed to
   * {@link GraphDeserializer#setParameters(ParameterSet)}.
   * @throws SerializationException If the graph could not be loaded.
   * @throws IOException On IO error.
   * @throws SerializerNotConfiguredException If the configuration is not sufficient for
   * deserialization. 
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public ParameterSet load(NamedStream[] streams, Schema schema)
    throws IOException, SerializationException, SerializerNotConfiguredException {
    reset();
    if (schema.getUtteranceLayer() == null) {
      throw new SerializationException("No utterance layer defined for schema");
    }

    // take the first cha stream, ignore all others.
    for (NamedStream stream : streams) {	 
      if (stream.getName().toLowerCase().endsWith(".mlf") 
          || "text/x-htk+plain".equals(stream.getMimeType())) {
        mlf = stream;
        break;
      }
    } // next stream
    if (mlf == null) throw new SerializationException("No Master Label File stream found");

    return new ParameterSet();
  }

  /**
   * Sets parameters for a given deserialization operation, after loading the serialized
   * form of the graph. This might include mappings from format-specific objects like
   * tiers to graph layers, etc. 
   * @param parameters The configuration for a given deserialization operation.
   * @throws SerializationParametersMissingException If not all required parameters have a value.
   */
  public void setParameters(ParameterSet parameters)
    throws SerializationParametersMissingException {
  }

  /**
   * Utility class for deducing the transcript filename and utterance
   * properties from the label 'file' name
   */
  private class LabFileNameParser {
    /** Resulting transcript file name */
    public String transcriptName = null;
      
    /** Resulting utterance start time */
    public Double startTime = null;
      
    /** Resulting utterance end time */
    public Double endTime = null;
      
    /** 
     * Constructor.
     * @param transcriptFileName
     * @param startTime
     * @param endTime
     */
    public LabFileNameParser(String transcriptFileName, Double startTime, Double endTime) {
      transcriptName = transcriptFileName;
      startTime = startTime;
      endTime = endTime;
    }
      
    /**
     * Constructor.
     * @param sLine Line from MLF file that represents the
     * beginning of a label 'file'
     */
    public LabFileNameParser(String sLine) {
      // line is something like:
      // */001-94-baigentcarla-03.trs_0.0_5.281.lab
      String sTrimmedLine = sLine.replaceFirst("\\*/","").replaceFirst("\\.lab$","");
      // 001-94-baigentcarla-03.trs_0.0_5.281
	 
      // start from the end and work back - this allows the transcript
      // name to contain _s
        
      try {
        int lastDelimiter = sTrimmedLine.lastIndexOf('_');
        endTime = Double.valueOf(sTrimmedLine.substring(lastDelimiter + 1));
        sTrimmedLine = sTrimmedLine.substring(0, lastDelimiter);
        lastDelimiter = sTrimmedLine.lastIndexOf('_');
        startTime = Double.valueOf(sTrimmedLine.substring(lastDelimiter + 1));
        transcriptName = sTrimmedLine.substring(0, lastDelimiter);
      } catch (NumberFormatException x) {
        // might be with a hyphen between times - e.g.
        // */001-94-baigentcarla-03.trs__0.0-5.281.lab
        int lastDelimiter = sTrimmedLine.lastIndexOf('-');
        endTime = Double.valueOf(sTrimmedLine.substring(lastDelimiter + 1));
        sTrimmedLine = sTrimmedLine.substring(0, lastDelimiter);
        lastDelimiter = sTrimmedLine.lastIndexOf('_');
        startTime = Double.valueOf(sTrimmedLine.substring(lastDelimiter + 1));
        transcriptName = sTrimmedLine.substring(0, lastDelimiter);
        if (transcriptName.endsWith("_")) { // it was __ before the start time
          transcriptName = transcriptName.substring(0, transcriptName.length() - 1);
        }
      }
    }
      
    /** Represents the object as a string */
    public String toString() {
      return Graph.FragmentId(transcriptName, startTime, endTime);
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
    Vector<Graph> graphs = new Vector<Graph>();

    int scoreOffset = scoreLayer == null?0:1;

    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(mlf.getStream(), "UTF-8"));
      String line = reader.readLine();
      Graph fragment = new Graph();
      Annotation word = new Annotation();
      while (line != null) {
        if (line.startsWith("\"")) { // new graph
               
          fragment = new Graph();
          // we don't just copy the whole schema, because that would imply that all the
          // extra layers contained no annotations, which is not necessarily true
          fragment.addLayer((Layer)schema.getParticipantLayer().clone());
          fragment.getSchema().setParticipantLayerId(schema.getParticipantLayerId());
          fragment.addLayer((Layer)schema.getTurnLayer().clone());
          fragment.getSchema().setTurnLayerId(schema.getTurnLayerId());
          fragment.addLayer((Layer)schema.getUtteranceLayer().clone());
          fragment.getSchema().setUtteranceLayerId(schema.getUtteranceLayerId());
          fragment.getSchema().setWordLayerId(schema.getWordLayerId());
          if (wordLayer != null) {
            fragment.addLayer((Layer)wordLayer.clone());
          }
          if (phoneLayer != null) {
            fragment.addLayer((Layer)phoneLayer.clone());
          }
          if (noiseLayer != null) {
            fragment.addLayer((Layer)noiseLayer.clone());
          }
          if (scoreLayer != null) {
            fragment.addLayer((Layer)scoreLayer.clone());
          }
          fragment.setOffsetUnits(Constants.UNIT_SECONDS);
               
          LabFileNameParser labFile = new LabFileNameParser(
            line.replaceFirst("^\"","").replaceFirst("\"$",""));
          fragment.setId(Graph.FragmentId(
                           labFile.transcriptName, labFile.startTime, labFile.endTime));
          Graph sourceGraph = new Graph();

          // record the inferred source graph name
          sourceGraph.setId(labFile.transcriptName);
          fragment.setGraph(sourceGraph);

          // also record the inferred fragment start time
          fragment.put("@startTime", labFile.startTime);
               
          fragment.addAnnotation(
            new Annotation()
            .setLabel(labFile.toString())
            .setLayerId(schema.getUtteranceLayerId())
            .setStart(fragment.getOrCreateAnchorAt(0.0, Constants.CONFIDENCE_MANUAL))
            .setEnd(fragment.getOrCreateAnchorAt(
                      labFile.endTime - labFile.startTime, Constants.CONFIDENCE_MANUAL)));
               
          graphs.add(fragment);
               
        } else { // annotation line
               
          StringTokenizer stTokens = new StringTokenizer(line, " ");

          if (stTokens.countTokens() >= 4 + scoreOffset) { // word line
            try {
              String sStartTime = stTokens.nextToken();
              String sEndTime = stTokens.nextToken();
              String sPhoneme = stTokens.nextToken();
              String sScore = scoreLayer==null?"":stTokens.nextToken();
              String sWord = decodeOctal(stTokens.nextToken());
              //System.out.println("StartTime: " + sStartTime + ", EndTime: " + sEndTime + ", Phoneme: " + sPhoneme + ", Word: " + sWord);
              double dStart = timestampToSeconds(sStartTime);
              double dEnd = timestampToSeconds(sEndTime);

              if (noiseIdentifiers.contains(sPhoneme)) { // a noise

                if (noiseLayer != null) {
                  Annotation noise = new Annotation()
                    .setLabel(sWord)
                    .setLayerId(noiseLayer.getId())
                    .setStart(fragment.getOrCreateAnchorAt(dStart, anchorConfidence))
                    .setEnd(fragment.getOrCreateAnchorAt(dEnd, anchorConfidence));
                  noise.setConfidence(annotationConfidence);
                  fragment.addAnnotation(noise);
                }
                        
              } else if (!sWord.equals("SILENCE") 
                         && !sWord.equals("PAUSE")
                         && !sWord.equals("SHORTPAUSE")) { // a word
                        
                word = new Annotation()
                  .setLabel(sWord)
                  .setLayerId(wordLayer.getId())
                  .setStart(fragment.getOrCreateAnchorAt(dStart, anchorConfidence))
                  .setEnd(fragment.getOrCreateAnchorAt(dEnd, anchorConfidence));
                word.setConfidence(annotationConfidence);
                fragment.addAnnotation(word);

                if (phoneLayer != null) {
                  // add the phoneme to the phoneme layer
                  sPhoneme = decodeOctal(sPhoneme);
                           
                  Annotation segment = new Annotation()
                    .setLabel(sPhoneme)
                    .setLayerId(phoneLayer.getId())
                    .setStart(word.getStart())
                    .setEnd(word.getEnd());
                  if (wordLayer != null && wordLayer.getId().equals(schema.getWordLayerId())) {
                    segment.setParent(word);
                  }
                  segment.setConfidence(annotationConfidence);
                  fragment.addAnnotation(segment);
                           
                  // save score?
                  if (sScore.length() > 0) {
                    if (scoreLayer != null) {
                      Annotation score = new Annotation()
                        .setLabel(sScore)
                        .setLayerId(scoreLayer.getId())
                        .setStart(segment.getStart())
                        .setEnd(segment.getEnd());
                      score.setConfidence(annotationConfidence);
                      if (scoreLayer.getParentId().equals(phoneLayer.getId())) {
                        score.setParent(segment);
                      }
                      fragment.addAnnotation(score);
                    } // score layer set
                  } // score
                } // phoneLayer set		     
              } // not silence
            } catch (Exception exWord) {
              throw exWord;
            }
          } else if (stTokens.countTokens() >= 3 + scoreOffset) { // phone line
            try {
              String sPhonemeStartTime = stTokens.nextToken();
              String sPhonemeEndTime = stTokens.nextToken();
              String sPhoneme = stTokens.nextToken();
              String sScore = scoreLayer==null?"":stTokens.nextToken();
                     
              // the end of a word
              if (word != null) {
                //message("Word: " + word);
                // each word ends with an 'sp'
                // even if it has no duration
                if (!sPhoneme.equals("sp")) {
                  // add the phoneme to the phoneme layer
                  sPhoneme = decodeOctal(sPhoneme);
                  double dStart = timestampToSeconds(sPhonemeStartTime);
                  double dEnd = timestampToSeconds(sPhonemeEndTime);
                  //System.out.println("Phoneme: " + sPhoneme + ", StartTime: " + dStart + ", EndTime: " + dEnd);

                  word.setEnd(fragment.getOrCreateAnchorAt(dEnd, anchorConfidence));
                           
                  if (phoneLayer != null) {
                    Annotation segment = new Annotation()
                      .setLabel(sPhoneme)
                      .setLayerId(phoneLayer.getId())
                      .setStart(fragment.getOrCreateAnchorAt(dStart, anchorConfidence))
                      .setEnd(fragment.getOrCreateAnchorAt(dEnd, anchorConfidence));
                    if (wordLayer != null && wordLayer.getId().equals(schema.getWordLayerId())) {
                      segment.setParent(word);
                    }
                    segment.setConfidence(annotationConfidence);
                              
                    // add it to the fragment
                    fragment.addAnnotation(segment);
                              
                    // save score?
                    if (sScore.length() > 0) {
                      if (scoreLayer != null) {
                        Annotation score = new Annotation()
                          .setLabel(sScore)
                          .setLayerId(scoreLayer.getId())
                          .setStart(segment.getStart())
                          .setEnd(segment.getEnd());
                        if (scoreLayer.getParentId().equals(phoneLayer.getId())) {
                          score.setParent(segment);
                        }
                        score.setConfidence(annotationConfidence);
                        fragment.addAnnotation(score);
                      } // scoreLayer set
                    } // score
                  } // phoneLayer set
                } // phoneme
              } // we are currently processing a word
            }  catch (Exception exWord) {
              throw exWord;
              //throw new Exception ("ERROR: Word line \'" + line + "\': " + exWord);
            }
          } // correct number of tokens for a phoneme
               
        } // annotation line
            
        line = reader.readLine();
      } // next line
    } catch (IOException io) {
      throw new SerializationException(io);
    }
    return graphs.toArray(new Graph[0]);
  }

  /**
   * Convert a 100ns mlf timestamp to seconds, applying the 11,025Hz/10ms rounding error 
   * correction if {@link #useP2FACorrection} is set.
   * @param timestamp MLF timestamp in 100ns
   * @return Time in seconds.
   */
  public double timestampToSeconds(String timestamp) {
    // from the last line of P2FA's readme.txt
    if (useP2FACorrection) {
      return (Double.parseDouble(timestamp) / 10000000 + 0.0125)*(11000.0/11025.0);
    } else {
      return (Double.parseDouble(timestamp) / 10000000);
    }
  } // end of timestampToSeconds()
   
  /**
   * Decodes sequences of non-7-bit-ASCII characters that have been encoded by HTK in
   * octal. e.g. ö is encoded as \303\266. 
   * It is assumed that the octal sequences represent UTF-8 characters
   * @param s
   * @return String with \888 sequences decoded into characters.
   */
  public String decodeOctal(String s) {
    // if there are no encoded strings, no need to do anything special
    if (s.indexOf('\\') < 0) return s;
      
    // we have a segment that includes octal bytes
    // so reconstruct the original string byte by byte
    Vector<Short> bytes = new Vector<Short>();
    byte[] originalBytes = s.getBytes();
    for (int i = 0; i < originalBytes.length; i++) {
      if (originalBytes[i] != (byte)'\\') {
        bytes.add((short)originalBytes[i]);
      } else { // \ indicating octal follows
        // e.g. \303
        // get next three bytes, which are digits
        String octal = "";
        octal += ""+((char)originalBytes[++i]);
        octal += ""+((char)originalBytes[++i]);
        octal += ""+((char)originalBytes[++i]);
        bytes.add(Short.parseShort(octal, 8));
      }
    } // next character
      
    byte[] ab = new byte[bytes.size()];
    for (int i = 0; i < bytes.size(); i++) {
      ab[i] = bytes.elementAt(i).byteValue();
    }
    try {
      return new String(ab, "UTF-8");
    } catch(UnsupportedEncodingException willNeverBeThrown) {
      return s;
    }
  } // end of decodeOctal()   

  /**
   * Returns any warnings that may have arisen during the last execution of
   * {@link #deserialize()}.
   * @return A possibly empty list of warnings.
   */
  public String[] getWarnings() {
    return warnings.toArray(new String[0]);
  }

} // end of class MlfDeserializer
