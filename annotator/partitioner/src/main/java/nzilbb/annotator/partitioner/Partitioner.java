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
package nzilbb.annotator.partitioner;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.util.AnnotationsByAnchor;

/**
 * Divides the given tokens up into blocks of the given size.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class Partitioner extends Annotator {
  
  /** Get the minimum version of the nzilbb.ag API supported by the serializer.*/
  public String getMinimumApiVersion() { return "1.2.0"; }
  
  /**
   * ID of the layer whose annotations are to be partitioned.
   * @see #getBoundaryLayer()
   * @see #setBoundaryLayer(String)
   */
  protected String boundaryLayerId;
  /**
   * Getter for {@link #boundaryLayerId}: ID of the layer whose
   * annotations are to be partitioned. 
   * @return ID of the layer whose annotations are to be partitioned.
   */
  public String getBoundaryLayerId() { return boundaryLayerId; }
  /**
   * Setter for {@link #boundaryLayerId}: ID of the layer whose
   * annotations are to be partitioned. 
   * @param newBoundaryLayer ID of the layer whose annotations are to be partitioned.
   */
  public Partitioner setBoundaryLayerId(String newBoundaryLayerId) {
    boundaryLayerId = newBoundaryLayerId;
    if (boundaryLayerId != null && boundaryLayerId.length() == 0) {
      boundaryLayerId = null;
    }
    return this;
  }  
  
  /**
   * Whether partitions are aligned with the "start", "middle", or "end" of
   * the boundary annotations. 
   * @see #getAlignment()
   * @see #setAlignment(String)
   */
  protected String alignment;
  /**
   * Getter for {@link #alignment}: Whether partitions are aligned
   * with the "start", "middle", or "end" of the boundary annotations. 
   * @return Whether partitions are aligned with the "start", "middle", or
   * "end" of the boundary annotations. 
   */
  public String getAlignment() { return alignment; }
  /**
   * Setter for {@link #alignment}: Whether partitions are aligned
   * with the "start", "middle", or "end" of the boundary annotations. 
   * @param newAlignment Whether partitions are aligned with the
   * "start", "middle", or "end" of the boundary annotations. 
   */
  public Partitioner setAlignment(String newAlignment) { alignment = newAlignment; return this; }
  
  /**
   * ID of layer on which to count annotations to determine partition
   * boundaries, or null to partition by offset. 
   * @see #getTokenLayerId()
   * @see #setTokenLayerId(String)
   */
  protected String tokenLayerId;
  /**
   * Getter for {@link #tokenLayerId}: ID of layer on which to count
   * annotations to determine partition boundaries, or null to
   * partition by offset. 
   * @return ID of layer on which to count annotations to determine
   * partition boundaries, or null to partition by offset. 
   */
  public String getTokenLayerId() { return tokenLayerId; }
  /**
   * Setter for {@link #tokenLayerId}: ID of layer on which to count
   * annotations to determine partition boundaries, or null to
   * partition by offset. 
   * @param newTokenLayerId ID of layer on which to count annotations to
   * determine partition boundaries, or null to partition by offset. 
   */
  public Partitioner setTokenLayerId(String newTokenLayerId) {
    tokenLayerId = newTokenLayerId;
    if (tokenLayerId != null && tokenLayerId.length() == 0) tokenLayerId = null;
    return this;
  }

  /**
   * Number of tokens that make up one partition (or number of
   * seconds, if {@link #tokenLayer} is null). 
   * @see #getPartitionSize()
   * @see #setPartitionSize(Double)
   */
  protected Double partitionSize;
  /**
   * Getter for {@link #partitionSize}: Number of tokens that make up
   * one partition (or number of seconds, if {@link #getTokenLayer()}
   * is null). 
   * @return Number of tokens that make up one partition (or number of
   * seconds, if {@link #getTokenLayer()} is null). 
   */
  public Double getPartitionSize() { return partitionSize; }
  /**
   * Setter for {@link #partitionSize}: Number of tokens that make up
   * one partition (or number of seconds, if {@link #getTokenLayer()}
   * is null). 
   * @param newPartitionSize Number of tokens that make up one
   * partition (or number of seconds, if {@link #getTokenLayer()} is null). 
   */
  public Partitioner setPartitionSize(Double newPartitionSize) { partitionSize = newPartitionSize; return this; }
  
  /**
   * The maximum number of partitions, or null for no maximum.
   * @see #getMaxPartitions()
   * @see #setMaxPartitions(Integer)
   */
  protected Integer maxPartitions;
  /**
   * Getter for {@link #maxPartitions}: The maximum number of
   * partitions, or null for no maximum. 
   * @return The maximum number of partitions, or null for no maximum.
   */
  public Integer getMaxPartitions() { return maxPartitions; }
  /**
   * Setter for {@link #maxPartitions}: The maximum number of
   * partitions, or null for no maximum. 
   * @param newMaxPartitions The maximum number of partitions, or null for no maximum.
   */
  public Partitioner setMaxPartitions(Integer newMaxPartitions) { maxPartitions = newMaxPartitions; return this; }
  
  /**
   * Whether to annotate whatever is left over after partitions of the
   * correct size have been created. 
   * @see #getLeftOvers()
   * @see #setLeftOvers(boolean)
   */
  protected boolean leftOvers;
  /**
   * Getter for {@link #leftOvers}: Whether to annotate whatever is
   * left over after partitions of the correct size have been
   * created. 
   * @return Whether to annotate whatever is left over after
   * partitions of the correct size have been created. 
   */
  public boolean getLeftOvers() { return leftOvers; }
  /**
   * Setter for {@link #leftOvers}: Whether to annotate whatever is
   * left over after partitions of the correct size have been created. 
   * @param newLeftOvers Whether to annotate whatever is left over
   * after partitions of the correct size have been created. 
   */
  public Partitioner setLeftOvers(boolean newLeftOvers) { leftOvers = newLeftOvers; return this; }
  
  /**
   * ID of transcript attribute layer identifying transcripts to
   * exclude, e.g. "transcript_type".
   * @see #getExcludeOnAttribute()
   * @see #setExcludeOnAttribute(String)
   */
  protected String excludeOnAttribute;
  /**
   * Getter for {@link #excludeOnAttribute}: ID of transcript
   * attribute layer identifying transcripts to exclude, e.g. "transcript_type". 
   * @return ID of transcript attribute layer identifying transcripts to exclude.
   */
  public String getExcludeOnAttribute() { return excludeOnAttribute; }
  /**
   * Setter for {@link #excludeOnAttribute}: ID of transcript
   * attribute layer identifying transcripts to exclude. 
   * @param newExcludeOnAttribute ID of transcript attribute layer
   * identifying transcripts to exclude, e.g. "transcript_type". 
   */
  public Partitioner setExcludeOnAttribute(String newExcludeOnAttribute) {
    excludeOnAttribute = newExcludeOnAttribute;
    if (excludeOnAttribute != null && excludeOnAttribute.length() == 0) {
      excludeOnAttribute = null;
    }
    return this;
  }
  
  /**
   * What the annotation labels should be; one of "size" (the size of
   * the partition) or "serial" (a number unique within the transcript). 
   * @see #getLabel()
   * @see #setLabel(String)
   */
  protected String label = "size";
  /**
   * Getter for {@link #label}: What the annotation labels
   * should be; one of "size" (the size of the partition) or "serial"
   * (a number unique within the transcript). 
   * @return What the annotation labels should be; one of "size" (the
   * size of the partition) or "serial" (a number unique within the
   * transcript). 
   */
  public String getLabel() { return label; }
  /**
   * Setter for {@link #label}: What the annotation labels
   * should be; one of "size" (the size of the partition) or "serial"
   * (a number unique within the transcript). 
   * @param newLabel What the annotation labels should be;
   * one of "size" (the size of the partition) or "serial" (a number
   * unique within the transcript).c 
   */
  public Partitioner setLabel(String newLabel) { label = newLabel; return this; }
  
  /**
   * Comma-separated list of values of {@link #excludeOnAttribute}
   * layer annotations to exclude from partitioning,
   * e.g. "wordlist,reading". 
   * @see #getExcludeOnAttributeValues()
   * @see #setExcludeOnAttributeValues(String)
   */
  protected String excludeOnAttributeValues;
  /**
   * Getter for {@link #excludeOnAttributeValues}: Comma-separated
   * list of values of {@link #getExcludeOnAttribute()} layer
   * annotations to exclude from partitioning,
   * e.g. "wordlist,reading". 
   * @return Comma-separated list of values of {@link
   * #getExcludeOnAttribute()} layer annotations to exclude from
   * partitioning, e.g. "wordlist,reading". 
   */
  public String getExcludeOnAttributeValues() { return excludeOnAttributeValues; }
  /**
   * Setter for {@link #excludeOnAttributeValues}: Comma-separated
   * list of values of {@link #getExcludeOnAttribute()} layer
   * annotations to exclude from partitioning,
   * e.g. "wordlist,reading". 
   * @param newExcludeOnAttributeValues Comma-separated list of values
   * of {@link #getExcludeOnAttribute()} layer annotations to exclude
   * from partitioning, e.g. "wordlist,reading". 
   */
  public Partitioner setExcludeOnAttributeValues(String newExcludeOnAttributeValues) {
    excludeOnAttributeValues = newExcludeOnAttributeValues;
    if (excludeOnAttributeValues != null && excludeOnAttributeValues.length() == 0) {
      excludeOnAttributeValues = null;
    }
    return this;
  }
  
  /**
   * ID of the output layer, on which partition annotations will be created.
   * @see #getDestinationLayerId()
   * @see #setDestinationLayerId(String)
   */
  protected String destinationLayerId;
  /**
   * Getter for {@link #destinationLayerId}: ID of the output layer,
   * on which partition annotations will be created. 
   * @return ID of the output layer.
   */
  public String getDestinationLayerId() { return destinationLayerId; }
  /**
   * Setter for {@link #destinationLayerId}: ID of the output layer.
   * @param newDestinationLayerId ID of the output layer, on which
   * partition annotations will be created. 
   */
  public Partitioner setDestinationLayerId(String newDestinationLayerId) {
    destinationLayerId = newDestinationLayerId;
    if (destinationLayerId != null && destinationLayerId.length() == 0) {
      destinationLayerId = null;
    }
    return this;
  }

  /** Constructor */
  public Partitioner() {
    // set a default schema for testing purposes
    Layer transcriptTypeLayer = new Layer("transcript_type", "Transcript Type")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true);
    transcriptTypeLayer.getValidLabels().put("wordlist", "Word list");
    transcriptTypeLayer.getValidLabels().put("reading", "Reading Passage");
    transcriptTypeLayer.getValidLabels().put("interview", "Interview");
    schema = new Schema(
      "participant", "turn", "utterance", "word",
      transcriptTypeLayer,
      new Layer("participant", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("participant").setParentIncludes(true),
      new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("language", "Phrase Language").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true));
  }

  /**
   * Sets the configuration for a given annotation task.
   * @param parameters The configuration of the annotator; a value of <tt> null </tt>
   * is invalid.
   * @throws InvalidConfigurationException
   */
  public void setTaskParameters(String parameters) throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");
    
    if (parameters == null) { // there are no possible default parameters
      throw new InvalidConfigurationException(this, "Parameters not set.");         
    }

    // reset these so if they're not explicitly set in the parameters, they're defaults
    boundaryLayerId = null;
    alignment = "start";
    tokenLayerId = null;
    partitionSize = null;
    maxPartitions = null;
    leftOvers = false;
    excludeOnAttribute = null;
    excludeOnAttributeValues = null;
    destinationLayerId = null;
    label = null;
    
    // parse parameters...
    beanPropertiesFromQueryString(parameters);
      
    // validate parameters...

    if (!"size".equals(label) && !"serial".equals(label)) label = "size";
    
    if (boundaryLayerId == null)
      throw new InvalidConfigurationException(this, "No boundary layer set.");
    Layer boundaryLayer = schema.getLayer(boundaryLayerId);
    if (boundaryLayer == null) {
      throw new InvalidConfigurationException(
        this, "Invalid boundary layer: " + boundaryLayerId);
    }
    
    if (!"middle".equals(alignment) && !"end".equals(alignment)) alignment = "start";
    
    // tokenLayerId has to be null or valid
    Layer tokenLayer = null;
    if (tokenLayerId != null) {
      tokenLayer = schema.getLayer(tokenLayerId);
      if (tokenLayer == null) {
        throw new InvalidConfigurationException(
          this, "Invalid token layer: " + tokenLayerId);
      }
      // boundary and token layers can't be the same
      if (boundaryLayerId.equals(tokenLayerId)) {
        throw new InvalidConfigurationException(
          this, "Boundary and token layers cannot be the same: " + boundaryLayerId);
      }
    }
    
    if (partitionSize == null)
      throw new InvalidConfigurationException(this, "No partition size set.");

    if (maxPartitions != null && maxPartitions <= 0) {
      throw new InvalidConfigurationException(
        this, "Maximum number of partitions must be a positive number: " + maxPartitions);
    }
    
    // excludeOnAttribute has to be null or excludeOnAttribute
    if (excludeOnAttribute != null) {
      if (schema.getLayer(excludeOnAttribute) == null) {
        throw new InvalidConfigurationException(
          this, "Invalid exclusion layer: " + excludeOnAttribute);
      }
    }
    
    if (destinationLayerId == null)
      throw new InvalidConfigurationException(this, "Destination layer not set.");
    // source and destination layers can't be the same
    if (destinationLayerId.equals(boundaryLayerId)) {
      throw new InvalidConfigurationException(
        this, "Boundary and partition layers cannot be the same: " + destinationLayerId);
    }
    if (destinationLayerId.equals(tokenLayerId)) {
      throw new InvalidConfigurationException(
        this, "Token and partition layers cannot be the same: " + destinationLayerId);
    }
    if (destinationLayerId.equals(schema.getWordLayerId())) {
      throw new InvalidConfigurationException(
        this, "Token and partition layers cannot be " + schema.getWordLayerId());
    }
    if (destinationLayerId.equals(schema.getUtteranceLayerId())) {
      throw new InvalidConfigurationException(
        this, "Token and partition layers cannot be " + schema.getUtteranceLayerId());
    }
    if (destinationLayerId.equals(schema.getTurnLayerId())) {
      throw new InvalidConfigurationException(
        this, "Token and partition layers cannot be " + schema.getTurnLayerId());
    }
    
    // does the outputLayer need to be added to the schema?
    Layer destinationLayer = schema.getLayer(destinationLayerId);
    String destinationParentId = schema.getRoot().getId().equals(boundaryLayerId)
      || schema.getTurnLayerId().equals(boundaryLayerId)?boundaryLayerId
      :boundaryLayer.getParentId();
    if (destinationLayer == null) {
      schema.addLayer(
        new Layer(destinationLayerId)
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(false)
        .setParentId(destinationParentId)
        .setType(Constants.TYPE_NUMBER));
    } else { // destination layer exists
      // check the scope
      if (destinationLayer.getParentId().equals(schema.getRoot().getId())
          && destinationLayer.getAlignment() == Constants.ALIGNMENT_NONE) {
        throw new InvalidConfigurationException(
          this, "Partition layer ("+destinationLayerId+") cannot be transcript attribute");
      }
      if (destinationLayer.getParentId().equals(schema.getParticipantLayerId())
          && destinationLayer.getAlignment() == Constants.ALIGNMENT_NONE) {
        throw new InvalidConfigurationException(
          this, "Partition layer ("+destinationLayerId+") cannot be participant attribute");
      }
      if (!boundaryLayer.getAncestors().contains(schema.getTurnLayer())
          && !boundaryLayerId.equals(schema.getRoot().getId())
          && !boundaryLayerId.equals(schema.getTurnLayerId())
          && destinationLayer.getAncestors().contains(schema.getTurnLayer())) {
        throw new InvalidConfigurationException(
          this, "Partition layer ("+destinationLayerId+") parent is "
          + destinationLayer.getParentId()
          + " but should be " + destinationParentId
          + " or " + schema.getTurnLayerId()
          + " or " + schema.getWordLayerId());
      }
      if (destinationLayer.getAncestors().contains(schema.getTurnLayer())
          && boundaryLayerId.equals(schema.getRoot().getId())) {
        // partitions are turn-constrained but may cross turn boundaries
        // e.g. first n words by any participant
        if (maxPartitions == null) maxPartitions = 1;
        if (maxPartitions > 1) {
          throw new InvalidConfigurationException(
            this, "Maximum partitions ("+maxPartitions+") can't be greater than 1"
            +" when partitioning all " + tokenLayerId
            +" annotations in the whole transcript with "+destinationLayer+" annotations");
        } 
        if (!"start".equals(alignment)) {
          throw new InvalidConfigurationException(
            this, "Can only use 'start' alignment when partitioning all " + tokenLayerId
            +" annotations in the whole transcript with "+destinationLayer+" annotations");
        } 
        if (leftOvers) {
          throw new InvalidConfigurationException(
            this, "Cannot tag leftovers when partitioning all " + tokenLayerId
            +" annotations in the whole transcript with "+destinationLayer+" annotations");
        } 
      }
      // make sure alignment is correct
      if (destinationLayer.getAlignment() != Constants.ALIGNMENT_INTERVAL
          && !destinationLayer.getParentId().equals(schema.getWordLayerId())) {
        destinationLayer.setAlignment(Constants.ALIGNMENT_INTERVAL);
      }
    }
  }
   
  /**
   * Determines which layers the annotator requires in order to annotate a graph.
   * @return A list of layer IDs. In this case, the annotator only requires the schema's
   * word layer.
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public String[] getRequiredLayers() throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");
    if (boundaryLayerId == null)
      throw new InvalidConfigurationException(this, "No boundary layer set.");
    HashSet<String> requiredLayers = new HashSet<String>();
    requiredLayers.add(boundaryLayerId);
    // token layer
    if (tokenLayerId != null) requiredLayers.add(tokenLayerId);
    // exclusion layer
    if (excludeOnAttribute != null) requiredLayers.add(excludeOnAttribute);
    return requiredLayers.toArray(new String[0]);
  }
  
  /**
   * Determines which layers the annotator will create/update/delete annotations on.
   * @return A list of layer IDs. In this case, the annotator has no task web-app for
   * specifying an output layer, and doesn't update any layers, so this method returns an
   * empty array.
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public String[] getOutputLayers() throws InvalidConfigurationException {
    if (destinationLayerId == null)
      throw new InvalidConfigurationException(this, "Destination layer not set.");
    return new String[] { destinationLayerId };
  }
  
  /**
   * Partition the excludeOnAttribute. 
   * @param transcript The annotation graph to transform.
   * @return The changes introduced by the tranformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph transform(Graph transcript) throws TransformationException {
    setRunning(true);
    try {
      Layer boundaryLayer = schema.getLayer(boundaryLayerId);
      if (boundaryLayer == null) {
        throw new InvalidConfigurationException(
          this, "Invalid boundary layer: " + boundaryLayerId);
      }
      Layer destinationLayer = transcript.getSchema().getLayer(destinationLayerId);
      if (destinationLayer == null) {
        throw new InvalidConfigurationException(
          this, "Invalid output destination layer: " + destinationLayerId);
      }
      if (excludeOnAttribute != null) {
        if (schema.getLayer(excludeOnAttribute) == null) {
          throw new InvalidConfigurationException(
            this, "Invalid exclusion layer: " + excludeOnAttribute);
        } else if (excludeOnAttributeValues != null) { // exclude this transcript?
          String[] excludedValues = excludeOnAttributeValues.split(",");
          for (Annotation attribute : transcript.all(excludeOnAttribute)) {
            for (String excluded : excludedValues) {
              if (attribute.getLabel().equals(excluded)) {
                setStatus("Skipping: " + excludeOnAttribute + " = " + excluded);
                return transcript;
              }
            } // next excluded value
          } // next annotation on the exclusion layer
        } // exclude transcript?
      }
    
      if (tokenLayerId == null) {
        return partitionByOffset(transcript);
      } else { // tokenLayerId set...
        Layer tokenLayer = schema.getLayer(tokenLayerId);
        if (tokenLayer == null) {
          throw new InvalidConfigurationException(
            this, "Invalid token layer: " + tokenLayerId);
        } else if (boundaryLayer.getId().equals(schema.getRoot().getId())
                   // partition layer is restricted to turns
                   && destinationLayer.getAncestors().contains(schema.getTurnLayer())) {
          // all annotations within the graph - only supports "first n" partitioning
          return partitionTokensWithinGraph(transcript);
        } else if (boundaryLayer.getId().equals(schema.getParticipantLayerId())) {
          return partitionParticipants(transcript);
        } else {
          return partition(transcript);
        }
      }
    } finally {    
      setRunning(false);
    }
  }
  
  /**
   * Partition bounded by normal temporal layers
   * @param transcript The annotation graph to transform.
   * @return The changes introduced by the tranformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph partition(Graph transcript) throws TransformationException {
    // all annotations for the speakers in the graph - only supports "first n" partitioning
    int numTokens = partitionSize.intValue();
    setStatus(
      transcript.getId()+":"
      +(maxPartitions != null?" up to " + maxPartitions:"")
      +" partitions of " + boundaryLayerId + " annotations by every "
      + numTokens + " " + tokenLayerId + " annotations"
      + (leftOvers?" (+leftovers)":"")
      + " alignment: " + alignment);
    Layer tokenLayer = schema.getLayer(tokenLayerId);
    Layer destinationLayer = schema.getLayer(destinationLayerId);
    // if it's a word layer, we'll be copying the source layer instead of creating spans
    boolean tagWords = schema.getWordLayerId().equals(destinationLayer.getParentId());
    
    // annotate the graph
    Annotation[] boundingAnnotations = transcript.list(boundaryLayerId);
    int doneCount = 0;
    for (Annotation bounding : boundingAnnotations) {
      if (isCancelling()) break;
      Annotation[] includedTokens = bounding.all(tokenLayerId);
      String label = ""+numTokens;
      setStatus("Bounding: " + bounding + " includes " + includedTokens.length + " tokens");
      
      // in principle we will do two passes - going forward and going backward
      // if alignment = start, then we go forward from the beginning, and don't go backward
      // if alignment = end, then we don't go forward, and we go back from the end
      // if alignment = middle, then we go forward from the middle,
      //  and go back from the middle

      // we'll add them out of order, but need ordinals to be in order
      AnnotationsByAnchor partititions = new AnnotationsByAnchor();
	    
      int forwardStartIndex = 0; // default for alignment = "start"
      Integer forwardMaxPartitions = maxPartitions;
      if (alignment.equals("end")) {
        forwardStartIndex = includedTokens.length;
      } else if (alignment.equals("middle")) {
        forwardStartIndex = (includedTokens.length /* round up: */ + 1) / 2;
        // if there's no max, or the max is odd
        if (maxPartitions == null || maxPartitions % 2 != 0) {
          // the first partition should cover the middle (not be aligned with the middle)
          forwardStartIndex -= numTokens / 2;
        }
        
        if (maxPartitions != null) {
          forwardMaxPartitions = (maxPartitions /* round up: */ + 1) / 2;
        }
      }
      
      // partition forwards
      int iEnd = forwardStartIndex;
      int iPartitionCount = 0;
      for (int i = forwardStartIndex;
           i <= includedTokens.length - numTokens;
           i += numTokens) {
        if (isCancelling()) break;
        if (forwardMaxPartitions != null && forwardMaxPartitions <= iPartitionCount) break;
        Annotation startToken = includedTokens[i];
        iEnd = i + numTokens - 1;
        Annotation endToken = null;
        if (iEnd < includedTokens.length) {
          endToken = includedTokens[iEnd];
        }
        
        if (endToken != null) {
          if (tagWords) {
            for (int t = i; t <= iEnd; t++) { // copy the label for each token
              partititions.add(
                transcript.createTag(
                  includedTokens[t], destinationLayerId, includedTokens[t].getLabel()));
            } // next token
          } else { // tag the span
            partititions.add(
              transcript.createSpan(startToken, endToken, destinationLayerId, label));
            // setStatus("forward " + startToken.getStart() + "-" + endToken.getEnd());
          }
          iPartitionCount++;
        }
      } // next partition (forward)
      Annotation possibleForwardOverflow = null;
      if (!tagWords && leftOvers && iEnd < includedTokens.length - 1 && iEnd > 0) {
        // possibly annotate leftovers to the end of the array
        // (defer until we're sure this won't take us over maxPartitions)
        possibleForwardOverflow = new Annotation()
          .setStartId(includedTokens[iEnd + 1].getStartId())
          .setEndId(includedTokens[includedTokens.length - 1].getEndId())
          .setLabel(""+(includedTokens.length - iEnd - 1));
      }
      
      // partition backwards
      int iStart = forwardStartIndex;
      for (int i = forwardStartIndex - 1; i+1 >= numTokens; i -= numTokens) {
        if (isCancelling()) break;
        if (maxPartitions != null && maxPartitions <= iPartitionCount) break;
        Annotation endToken = includedTokens[i];
        iStart = i - numTokens + 1;
        Annotation startToken = null;
        if (iStart >= 0) {
          startToken = includedTokens[iStart];
        }
        
        if (startToken != null) {
          if (tagWords) {
            for (int t = iStart; t <= i; t++) { // copy the label for each token
              partititions.add(
                transcript.createTag(
                  includedTokens[t], destinationLayerId, includedTokens[t].getLabel()));
            } // next token
          } else { // tag the span
            partititions.add(
              transcript.createSpan(startToken, endToken, destinationLayerId, label));
            // setStatus("backward " + startToken.getStart() + "-" + endToken.getEnd());
          }
          iPartitionCount++;
        }
      } // next partition
      
      // add forward overflow?
      if (possibleForwardOverflow != null) {
        if (maxPartitions == null || iPartitionCount < maxPartitions) { // not beyond max
          possibleForwardOverflow = transcript.createAnnotation(
            transcript.getAnchor(possibleForwardOverflow.getStartId()),
            transcript.getAnchor(possibleForwardOverflow.getEndId()),
            destinationLayerId, possibleForwardOverflow.getLabel(),
            bounding);
          partititions.add(possibleForwardOverflow);
          // setStatus(
          //   "forward overflow " + possibleForwardOverflow.getStart()
          //   + "-" + possibleForwardOverflow.getEnd());
          iPartitionCount++;
        }
      }

      // add backward overflow?
      if (!tagWords
          && leftOvers && iStart > 0) { // annotate leftovers to the end of the array
        if (maxPartitions == null || iPartitionCount < maxPartitions) { // not beyond max
          Annotation startToken = includedTokens[0];
          Annotation endToken = includedTokens[iStart - 1];
          partititions.add(
            transcript.createSpan(startToken, endToken, destinationLayerId, ""+iStart));
          // setStatus("backward leftover " + startToken.getStart() + "-" + endToken.getEnd());
          iPartitionCount++;
        }
      }

      // ensure ordinals are in offset order
      int ordinal = 1;
      for (Annotation partition : partititions) partition.setOrdinal(ordinal++);
      
      doneCount++;
      setPercentComplete(10 + ((doneCount * 65) / boundingAnnotations.length));      
    } // next bounding annotation to partition
    if ("serial".equals(label)) {
      int serial = 0;
      for (Annotation partition : transcript.all(destinationLayerId)) {
        if (partition.getChange() != Change.Operation.Destroy) {
          partition.setLabel(""+(++serial));
        }
      }
    }
    return transcript;
  } // partition
  
  /** Static format so we don't keep creating and destroying one */
  private static DecimalFormat offsetFormat = new DecimalFormat(
    // force the locale to something with . as the decimal separator
    "0.000", new DecimalFormatSymbols(Locale.UK));
  
  /**
   * Partition the excludeOnAttribute. 
   * @param transcript The annotation graph to transform.
   * @return The changes introduced by the tranformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph partitionByOffset(Graph transcript) throws TransformationException {
    setStatus(
      transcript.getId()+":"
      +(maxPartitions != null?" up to " + maxPartitions:"")
      +" partitions of " + boundaryLayerId + " annotations by every "
      + partitionSize + "s"
      + (leftOvers?" (+leftovers)":"")
      + " alignment: " + alignment);
    Layer boundaryLayer = schema.getLayer(boundaryLayerId);
    Layer destinationLayer = schema.getLayer(destinationLayerId);
    Annotation[] boundingAnnotations = transcript.list(boundaryLayerId);
    int doneCount = 0;
    for (Annotation bound : boundingAnnotations) {
      if (isCancelling()) break;
      setStatus("Bounding: " + bound + " ("+bound.getStart()+"-"+bound.getEnd()+")");
      Annotation parent = boundaryLayer.getParentId() != null && boundaryLayer.getParentId()
        .equals(destinationLayer.getParentId())?bound.getParent():bound;
      int partitionCount = 0;
      if (alignment.equals("start")) {
        Anchor start = bound.getStart();
        Integer offsetConfidence = start.getConfidence();
        double boundEndOffset = bound.getEnd().getOffset();
        String label = ""+partitionSize;
        for (double endOffset = start.getOffset() + partitionSize;
             endOffset <= boundEndOffset;
             endOffset = start.getOffset() + partitionSize) {		  
          Anchor end = endOffset == boundEndOffset?
            bound.getEnd() // use bound end anchor if it matches exactly
            : transcript.createAnchorAt​(endOffset, offsetConfidence);
          transcript.createAnnotation(start, end, destinationLayerId, label, parent);
          
          partitionCount++;
          if (maxPartitions != null && partitionCount >= maxPartitions) { // reached maximum
            break;
          }
          
          start = end;
        } // next partition
        // leftover?
        if (start.getOffset() < boundEndOffset
            && leftOvers && (maxPartitions == null || partitionCount < maxPartitions)) {
          transcript.createAnnotation(
            start, bound.getEnd(), destinationLayerId,
            offsetFormat.format(bound.getEnd().getOffset() - start.getOffset()), parent);
        }
      } else if (alignment.equals("end")) {
        // we'll add them out of order, but need ordinals to be in order
        AnnotationsByAnchor partititions = new AnnotationsByAnchor();
	    
        Anchor end = bound.getEnd();
        Integer offsetConfidence = end.getConfidence();
        double boundStartOffset = bound.getStart().getOffset();
        String label = ""+partitionSize;
        for (double startOffset = end.getOffset() - partitionSize;
             startOffset >= boundStartOffset;
             startOffset = end.getOffset() - partitionSize) {		  
          Anchor start = startOffset == boundStartOffset?
            start = bound.getStart() // use bound end anchor if it matches up exactly
            :transcript.createAnchorAt(startOffset, offsetConfidence);
          partititions.add(
            transcript.createAnnotation(start, end, destinationLayerId, label, parent));
          // setStatus("partition: ("+start+"-"+end+")");
          
          partitionCount++;
          if (maxPartitions != null && partitionCount >= maxPartitions) { // reached maximum
            break;
          }
          
          end = start;
        } // next partition
        // leftover?
        if (end.getOffset() > boundStartOffset
            && leftOvers && (maxPartitions == null || partitionCount < maxPartitions))
        {
          transcript.createAnnotation(
            bound.getStart(), end, destinationLayerId,
            offsetFormat.format(end.getOffset() - bound.getStart().getOffset()), parent);
        }
        // ensure ordinals are in offset order
        int ordinal = 1;
        for (Annotation partitition : partititions) partitition.setOrdinal(ordinal++);
      
      } else { // alignment == middle
        int annotationCount = (int)(bound.getDuration() / partitionSize);
        if (maxPartitions != null && annotationCount > maxPartitions) {
          annotationCount = maxPartitions;
        }
        double leftoverSeconds = bound.getDuration() - (partitionSize*annotationCount);
        Anchor start = bound.getStart();
        Integer offsetConfidence = start.getConfidence();
        double boundEndOffset = bound.getEnd().getOffset();
        
        if (leftoverSeconds > 0) {
          Anchor multipleStart = transcript.createAnchorAt(
            start.getOffset() + (leftoverSeconds/2), offsetConfidence);
          if (leftOvers) {
            if (bound.getDuration() < partitionSize) {
              // the bounding annotation isn't long enough for one full length partition
              // so just add one partitition taking up the whole bounding annotation
              transcript.createAnnotation(
                bound.getStart(), bound.getEnd(), destinationLayerId,
                ""+bound.getDuration(), parent);
              partitionCount++;
              multipleStart.destroy(); // (didn't use it)
              continue; // next bounding annotation
            } else {
              // no starting leftover if we are excluding any full-length partitions
              if (maxPartitions == null || annotationCount < maxPartitions) { 
                transcript.createAnnotation(
                  start, multipleStart, destinationLayerId,
                  offsetFormat.format(leftoverSeconds/2), parent);
                partitionCount++;
              }
            }
          }
          start = multipleStart;
        }
        
        String label = ""+partitionSize;	       
        int skipCount = 0;
        if (maxPartitions != null) {
          // if more partitions fit than are allowed by maxPartitions, skip the first few
          skipCount = Math.max(0, (annotationCount - maxPartitions) / 2);
          // if an odd number would fit, but we'll be inserting an
          // even number, or vice versa
          if (annotationCount > maxPartitions && annotationCount % 2 != maxPartitions % 2) {
            // shift forward by half
            start = transcript.createAnchorAt(
              start.getOffset() + (partitionSize/2), offsetConfidence);
          }
        } // maxPartitions is set
	       
        for (double endOffset = start.getOffset() + partitionSize;
             endOffset <= boundEndOffset; 
             endOffset = start.getOffset() + partitionSize) {		  
          if (isCancelling()) break;
          Anchor end = endOffset == boundEndOffset?
            bound.getEnd() // use bound end anchor if it matches up exactly
            :transcript.createAnchorAt(endOffset, offsetConfidence);
          if (--skipCount < 0) {
            // setStatus("skipcount now " + skipCount);
            transcript.createAnnotation(start, end, destinationLayerId, label, parent);

            partitionCount++;
            if (maxPartitions != null && partitionCount >= maxPartitions) { // reached max
              break;
            }
          } else {
            setStatus("Skipped");
          }
          
          start = end;
        } // next partition
        // leftover?
        if (start.getOffset() < boundEndOffset
            && leftOvers && (maxPartitions == null || partitionCount < maxPartitions)) {
          transcript.createAnnotation(
            start, bound.getEnd(), destinationLayerId,
            offsetFormat.format(bound.getEnd().getOffset() - start.getOffset()), parent);
        }
      } // alignment = middle
      doneCount++;
      setPercentComplete(10 + ((doneCount * 65) / boundingAnnotations.length));
    } // next bounding annotation
    return transcript;
  } // partitionByOffset
  
  /**
   * Partition bounded by the whole transcript, to a partition layer
   * that's a phrase or word layer. 
   * @param transcript The annotation graph to transform.
   * @return The changes introduced by the tranformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph partitionTokensWithinGraph(Graph transcript) throws TransformationException {
    // all annotations within the graph - only supports "first n" partitioning
    int numTokens = partitionSize.intValue();
    int tokensLeft = numTokens;
    setStatus("partitionTokensWithinGraph - only supports 'first n' partitioning...");
    Layer tokenLayer = schema.getLayer(tokenLayerId);
    setStatus("Scanning graph for : " + numTokens + " " + tokenLayer.getId() + "s");
    Layer destinationLayer = schema.getLayer(destinationLayerId);
    // if it's a word layer, we'll be copying the source layer instead of creating spans
    boolean tagWords = schema.getWordLayerId().equals(destinationLayer.getParentId());
    
    // for each turn
    Vector<Annotation> wholeTurns = new Vector<Annotation>();
    Annotation parent = transcript;
    Annotation startToken = null;
    Annotation endToken = null;
    for (Annotation turn : transcript.all(schema.getTurnLayerId())) {
      setStatus("turn: " + turn.getStart() + " tokensLeft = " + tokensLeft);
      if (isCancelling()) break;
      Annotation[] includedTokens = turn.all(tokenLayer.getId());
      setStatus(tokenLayer.getId() + ": " + includedTokens.length);
      if (includedTokens.length == 0) continue;
      if (includedTokens.length <= tokensLeft) {
        wholeTurns.add(turn);
        setStatus(tokenLayer.getId() + ": whole turn");
        tokensLeft -= includedTokens.length;
      } else {
        startToken = includedTokens[0];
        endToken = includedTokens[tokensLeft-1];
        setStatus(tokenLayer.getId() + ": part turn - up to " + tokensLeft);
        if (destinationLayer.getParentId().equals(schema.getTurnLayerId())) {
          parent = turn;
        }
        tokensLeft = 0; // drop out, we've come far enough
      }
      if (tokensLeft <= 0) break;
    } // next turn
    if (tokensLeft <= 0) { // found the requisite number of tokens, so annotate the lot
      for (Annotation turn : wholeTurns) {
        if (tagWords) {
          for (Annotation token : turn.list(tokenLayer.getId())) {
            // copy the label for each token
            transcript.createTag(token, destinationLayerId, token.getLabel());
          } // next token
        } else { // tag the whole turn
          transcript.createTag(turn, destinationLayerId, ""+numTokens);
          // setStatus("create tag " + turn.getId());
        }
      }
      if (startToken != null && endToken != null) {
        if (tagWords) {
          for (Annotation token : startToken.first(schema.getTurnLayerId())
                 .all(tokenLayer.getId())) { // copy the label for each token
            transcript.createTag(token, destinationLayerId, token.getLabel());
            if (token == endToken) break;
          } // next token
        } else { // tag the whole span
          transcript.createSpan(
            startToken, endToken, destinationLayerId, ""+numTokens, parent);
          // setStatus(
          //   "create tag from " + startToken.getLabel() + " to " + endToken.getLabel());
        }
      }
    }
    return transcript;
  } // partitionTokensWithinGraph

  /**
   * Partition bounded by the participant within the transcript. 
   * @param transcript The annotation graph to transform.
   * @return The changes introduced by the tranformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph partitionParticipants(Graph transcript) throws TransformationException {
    // all annotations for the speakers in the graph - only supports "first n" partitioning
    setStatus("partitionParticipants - not supported!");
    throw new TransformationException(
      this, "Partitioning by participant across the whole graph store is currently not supported.");
      /* previous code:

      SqlGraphStore store = new SqlGraphStore(getConnection());

      // get configuration
      Properties settings = loadSettings(iLayerId, iAuxiliary);
      Layer boundaryLayer = store.getLayer(settings.getProperty("boundaryLayer"));
      assert boundaryLayer.getId().equals("participant") : "boundaryLayer.getId().equals(\"participant\") - " + boundaryLayer.getId();
      String alignment = settings.getProperty("alignment");
      Layer tokenLayer = store.getLayer(settings.getProperty("tokenLayer"));
      int numTokens = Integer.parseInt(settings.getProperty("numTokens"));
      Integer maxPartitions = null;      
      try {maxPartitions = new Integer(settings.getProperty("maxPartitions"));} 
      catch(Throwable exception) {}
      boolean leftOvers = Boolean.parseBoolean(settings.getProperty("leftOvers"));
      String sExcludedTranscriptTypes = settings.getProperty("ExcludedTranscriptTypes", "");
      HashSet<String> excludedTranscriptTypes = new HashSet<String>();
      if (sExcludedTranscriptTypes != null 
	  && sExcludedTranscriptTypes.length() > 0)
      {
	 StringTokenizer tokIds = new StringTokenizer(sExcludedTranscriptTypes, ",");
	 while (tokIds.hasMoreTokens())
	 {
	    excludedTranscriptTypes.add(tokIds.nextToken());
	 }
      }

      setStatus("Partitioning participant: " + participant);

      // get our layer name
      Layer layer = null;
      for (Layer l : store.getLayers())
      {
	 if (((Integer)l.get("layer_id")).intValue() == iLayerId)
	 {
	    layer = l;
	    break;
	 }
      }
      assert layer != null : "layer != null - " + iLayerId;
      // if it's a word layer, we'll be copying the source layer instead of creating spans
      boolean tagWords = "W".equals(layer.get("scope"));

      if (!getGeneratingAllTranscripts()) iPercentComplete = 1;

      if (!getGeneratingAllTranscripts()) iPercentComplete = 10;

      // delete annotations for this participant
      if (iAuxiliary == null)
      {
	 setStatus("Deleting annotations for " + participant);
	 PreparedStatement sqlDelete = getConnection().prepareStatement(
	    "DELETE l.* FROM annotation_layer_" + iLayerId + " l"
	    +" INNER JOIN annotation_layer_11 t ON t.annotation_id = l.parent_id"
	    +" INNER JOIN speaker s ON s.speaker_number = t.label"
	    +" WHERE s.name = ?");
	 sqlDelete.setString(1, participant);
	 sqlDelete.executeUpdate();
	 sqlDelete.close();
      }      

      // load graph from database, ordered by series/series position
      TreeSet<Graph> graphs = new TreeSet<Graph>(
	 new Comparator<Graph>()
	 {
	    public int compare(Graph g1, Graph g2)
	    {
	       String series1 = (String)g1.get("@series");
	       String series2 = (String)g2.get("@series");
	       int seriesCompare = series1.compareTo(series2);
	       if (seriesCompare != 0) return seriesCompare;
	       // same series, compare by ordinal
	       if (g1.getOrdinal() < g2.getOrdinal()) return -1;
	       if (g1.getOrdinal() > g2.getOrdinal()) return 1;
	       // fallback to ID comparison
	       return g1.getId().compareTo(g2.getId());
	    }
	 });
      String[] bareLayers = {"participant", "main_participant"};
      for (String graphId : store.getGraphIdsWithParticipant(participant))
      {
	 Graph graph = store.getGraph(graphId, bareLayers); // no layers for now
	 if (excludedTranscriptTypes.contains(graph.get("@transcript_type")))
	 {
	    setStatus("Ignoring "+graph.getId() + " - transcript type excluded: " + graph.get("@transcript_type"));
	    continue;
	 }
	 // only include this graph if the speaker is a main participant
	 for (Annotation main : graph.list("main_participant"))
	 {
	    if (main.getParent().getLabel().equals(participant))
	    {
	       graphs.add(graph);
	       break;
	    }
	 } // next main participant
      } // next graph

      // partition across all graphs      
      setStatus("Partitioning "+graphs.size() + " graphs - " + tokenLayer.getId() + " within " + boundaryLayer.getId() + "...");
      int tokensLeft = numTokens;
      Vector<Graph> graphsToSave = new Vector<Graph>();
      for (Graph bareGraph : graphs)
      { 
	 if (isCancelling()) break;
	 setStatus("scanning "+bareGraph.getId()+" for : " + tokensLeft + " more " + tokenLayer.getId() + "s");
	 
	 // load full graph
	 Graph fullGraph = store.getGraph(bareGraph.getId());
	 
	 // for each turn
	 Vector<Annotation> wholeTurns = new Vector<Annotation>();
	 Annotation parent = fullGraph;
	 Annotation startToken = null;
	 Annotation endToken = null;
	 for (Annotation turn : fullGraph.list("turns"))
	 {
	    if (isCancelling()) break;
	    if (!turn.getLabel().equals(participant)) continue;

	    // setStatus("turn: " + turn.getStart() + " tokensLeft = " + tokensLeft);
	    Annotation[] includedTokens = turn.list(tokenLayer.getId());
	    // setStatus(tokenLayer.getId() + ": " + includedTokens.length);
	    if (includedTokens.length == 0) continue;
	    if (includedTokens.length <= tokensLeft)
	    {
	       wholeTurns.add(turn);
	       tokensLeft -= includedTokens.length;
	    }
	    else
	    {
	       startToken = includedTokens[0];
	       endToken = includedTokens[tokensLeft-1];
	       if (layer.getParentId().equals("turns"))
	       {
		  parent = turn;
	       }
	       tokensLeft = 0; // drop out, we've come far enough
	    }
	    if (tokensLeft <= 0) break;
	 } // next turn
	 graphsToSave.add(fullGraph);
	 for (Annotation turn : wholeTurns)
	 {
	    if (tagWords)
	    {
	       for (Annotation token : turn.list(tokenLayer.getId()))
	       { // copy the label for each token
		  fullGraph.createTag(token, layer.getId(), token.getLabel())
		     .put(Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC);
	       } // next token
	    }
	    else
	    { // tag the whole turn
	       fullGraph.createTag(turn, layer.getId(), ""+numTokens)
		  .put(Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC);
	    }
	 }
	 if (startToken != null && endToken != null)
	 {
	       if (tagWords)
	       {
		  for (Annotation token : startToken.my("turns").list(tokenLayer.getId()))
		  { // copy the label for each token
		     fullGraph.createTag(token, layer.getId(), token.getLabel())
			.put(Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC);
		     if (token == endToken) break;
		  } // next token
	       }
	       else
	       { // tag the whole span
		  fullGraph.createSpan(startToken, endToken, layer.getId(), ""+numTokens, parent)
		     .put(Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC);
	       }
	 }
	 if (tokensLeft <= 0) break;
      } // next graph
      if (tokensLeft <= 0)
      { // found the requisite number of tokens, so save all annotations
	 for (Graph graph : graphsToSave)
	 {
	    if (isCancelling()) break;
	    // save graph back to store
	    if (graph.getChange() != Change.Operation.NoChange)
	    {
	       setStatus("Saving " + graph.getId());
	       store.saveGraph(graph);
	    }
	    else
	    {
	       setStatus("No changes to save in " + graph.getId());
	    }
	 } // next graph to save
      }
      */
  } // partitionParticipants
} // end of class Partitioner
