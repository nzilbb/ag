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
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import nzilbb.ag.Anchor;
import nzilbb.ag.Annotation;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.automation.InvalidConfigurationException;

public class TestPartitioner {
  
  static Partitioner annotator = new Partitioner();
  
  /** Ensure parameter validation is working. */
  @Test public void parameterValidation() throws Exception {
      
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    try {
      annotator.setTaskParameters(
        "boundaryLayerId=" // no boundary layer
        +"&tokenLayerId=word"
        +"&partitionSize=5"
        +"&maxPartitions="
        +"&alignment=left"
        +"&excludeOnAttribute=transcript_type"
        +"&excludeOnAttributeValues=wordlist,test%20graph"
        +"&destinationLayerId=partition"
        +"&leftOvers=on");
      fail("Should fail with no boundary layer");
    } catch (InvalidConfigurationException x) {
      // System.out.println(x.toString());
    }
    
    try {
      annotator.setTaskParameters(
        "boundaryLayerId=nonexistent" // nonexistent layer
        +"&tokenLayerId=word"
        +"&partitionSize=5"
        +"&maxPartitions="
        +"&alignment=left"
        +"&excludeOnAttribute=transcript_type"
        +"&excludeOnAttributeValues=wordlist,test%20graph"
        +"&destinationLayerId=partition"
        +"&leftOvers=on");
      fail("Should fail with nonexistent boundary layer");
    } catch (InvalidConfigurationException x) {
      // System.out.println(x.toString());
    }
    
    try {
      annotator.setTaskParameters(
        "boundaryLayerId=turn"
        +"&tokenLayerId=nonexistent" // nonexistent layer
        +"&partitionSize=5"
        +"&maxPartitions="
        +"&alignment=left"
        +"&excludeOnAttribute=transcript_type"
        +"&excludeOnAttributeValues=wordlist,test%20graph"
        +"&destinationLayerId=partition"
        +"&leftOvers=on");
      fail("Should fail with nonexistent token layer");
    } catch (InvalidConfigurationException x) {
      // System.out.println(x.toString());
    }
    
    try {
      annotator.setTaskParameters(
        "boundaryLayerId=turn"
        +"&tokenLayerId=word"
        +"&partitionSize=" // no size
        +"&maxPartitions="
        +"&alignment=left"
        +"&excludeOnAttribute=transcript_type"
        +"&excludeOnAttributeValues=wordlist,test%20graph"
        +"&destinationLayerId=partition"
        +"&leftOvers=on");
      fail("Should fail with no partition size");
    } catch (InvalidConfigurationException x) {
      // System.out.println(x.toString());
    }
    
    try {
      annotator.setTaskParameters(
        "boundaryLayerId=turn"
        +"&tokenLayerId=word"
        +"&partitionSize=non-number" // invalid size
        +"&maxPartitions="
        +"&alignment=left"
        +"&excludeOnAttribute=transcript_type"
        +"&excludeOnAttributeValues=wordlist,test%20graph"
        +"&destinationLayerId=partition"
        +"&leftOvers=on");
      fail("Should fail with invalid partition size");
    } catch (InvalidConfigurationException x) {
      // System.out.println(x.toString());
    }
    
    annotator.setTaskParameters(
      "boundaryLayerId=turn"
      +"&tokenLayerId=word"
      +"&partitionSize=5"
      +"&maxPartitions=non-number" // invalid maxPartitions
      +"&alignment=left"
      +"&excludeOnAttribute=transcript_type"
      +"&excludeOnAttributeValues=wordlist,test%20graph"
      +"&destinationLayerId=partition"
      +"&leftOvers=on");
    assertNull("Invalid maxPartitions defaults to null", annotator.getMaxPartitions());
    
    try {
      annotator.setTaskParameters(
        "boundaryLayerId=turn"
        +"&tokenLayerId=word"
        +"&partitionSize=5"
        +"&maxPartitions=0" // invalid maxPartitions
        +"&alignment=left"
        +"&excludeOnAttribute=transcript_type"
        +"&excludeOnAttributeValues=wordlist,test%20graph"
        +"&destinationLayerId=partition"
        +"&leftOvers=on");
      fail("Should fail with zero maxPartitions");
    } catch (InvalidConfigurationException x) {
      // System.out.println(x.toString());
    }
    
    try {
      annotator.setTaskParameters(
        "boundaryLayerId=turn"
        +"&tokenLayerId=word"
        +"&partitionSize=5"
        +"&maxPartitions="
        +"&alignment=left"
        +"&excludeOnAttribute=nonexistent" // nonexistent layer
        +"&excludeOnAttributeValues=wordlist,test%20graph"
        +"&destinationLayerId=partition"
        +"&leftOvers=on");
      fail("Should fail with nonexistent exclusion layer");
    } catch (InvalidConfigurationException x) {
      // System.out.println(x.toString());
    }
    
    try {
      annotator.setTaskParameters(
        "boundaryLayerId=turn"
        +"&tokenLayerId=word"
        +"&partitionSize=5"
        +"&maxPartitions="
        +"&alignment=left"
        +"&excludeOnAttribute=transcript_type"
        +"&excludeOnAttributeValues=wordlist,test%20graph"
        +"&destinationLayerId=" // no layer
        +"&leftOvers=on");
      fail("Should fail with no destinationLayerId");
    } catch (InvalidConfigurationException x) {
      // System.out.println(x.toString());
    }
    
    try {
      annotator.setTaskParameters(
        "boundaryLayerId=turn"
        +"&tokenLayerId=word"
        +"&partitionSize=5"
        +"&maxPartitions="
        +"&alignment=left"
        +"&excludeOnAttribute=transcript_type"
        +"&excludeOnAttributeValues=wordlist,test%20graph"
        +"&destinationLayerId=turn" // same as boundaryLayerId
        +"&leftOvers=on");
      fail("Should fail with invalid boundaryLayerId==destinationLayerId");
    } catch (InvalidConfigurationException x) {
      // System.out.println(x.toString());
    }
    
    try {
      annotator.setTaskParameters(
        "boundaryLayerId=turn"
        +"&tokenLayerId=word"
        +"&partitionSize=5"
        +"&maxPartitions="
        +"&alignment=left"
        +"&excludeOnAttribute=transcript_type"
        +"&excludeOnAttributeValues=wordlist,test%20graph"
        +"&destinationLayerId=word" // same as tokenLayerId
        +"&leftOvers=on");
      fail("Should fail with tokenLayerId==destinationLayerId");
    } catch (InvalidConfigurationException x) {
      // System.out.println(x.toString());
    }
    
    try {
      annotator.setTaskParameters(
        "boundaryLayerId=turn"
        +"&tokenLayerId=word"
        +"&partitionSize=5"
        +"&maxPartitions="
        +"&alignment=left"
        +"&excludeOnAttribute=transcript_type"
        +"&excludeOnAttributeValues=wordlist,test%20graph"
        +"&destinationLayerId=utterance" // system layer
        +"&leftOvers=on");
      fail("Should fail with system layer as destinationLayerId");
    } catch (InvalidConfigurationException x) {
      // System.out.println(x.toString());
    }
    
    try {
      annotator.setTaskParameters(
        "boundaryLayerId=turn"
        +"&tokenLayerId=word"
        +"&partitionSize=5"
        +"&maxPartitions="
        +"&alignment=left"
        +"&excludeOnAttribute=" // (unset)
        +"&excludeOnAttributeValues=wordlist,test%20graph"
        +"&destinationLayerId=transcript_type" // transcript attribute
        +"&leftOvers=on");
      fail("Should fail with transcript attribute destinationLayerId");
    } catch (InvalidConfigurationException x) {
      // System.out.println(x.toString());
    }
    
    try {
      annotator.setTaskParameters(
        "boundaryLayerId=topic"
        +"&tokenLayerId=word"
        +"&partitionSize=5"
        +"&maxPartitions="
        +"&alignment=left"
        +"&excludeOnAttribute=transcript_type"
        +"&excludeOnAttributeValues=wordlist,test%20graph"
        +"&destinationLayerId=phrase" // same as boundaryLayerId
        +"&leftOvers=on");
      fail("Should fail with boundaryLayerId and destinationLayerId in different scopes");
    } catch (InvalidConfigurationException x) {
      // System.out.println(x.toString());
    }
    
    try {
      annotator.setTaskParameters(
        "boundaryLayerId=transcript"
        +"&tokenLayerId=word"
        +"&partitionSize=5"
        +"&maxPartitions=2" // partitioning tokens in whole graph: this must be unset or 1
        +"&alignment=left"
        +"&excludeOnAttribute=transcript_type"
        +"&excludeOnAttributeValues=wordlist,test%20graph"
        +"&destinationLayerId=phrase");
      fail("Should fail with maxPartitions > 1");
    } catch (InvalidConfigurationException x) {
      // System.out.println(x.toString());
    }
    
    try {
      annotator.setTaskParameters(
        "boundaryLayerId=transcript"
        +"&tokenLayerId=word"
        +"&partitionSize=5"
        +"&maxPartitions="
        +"&alignment=middle"  // partitioning tokens in whole graph: this must be 'start'
        +"&excludeOnAttribute=transcript_type"
        +"&excludeOnAttributeValues=wordlist,test%20graph"
        +"&destinationLayerId=phrase");
      fail("Should fail with invalid alignment");
    } catch (InvalidConfigurationException x) {
      // System.out.println(x.toString());
    }
    
    try {
      annotator.setTaskParameters(
        "boundaryLayerId=transcript"
        +"&tokenLayerId=word"
        +"&partitionSize=5"
        +"&maxPartitions="
        +"&alignment=start"
        +"&excludeOnAttribute=transcript_type"
        +"&excludeOnAttributeValues=wordlist,test%20graph"
        +"&destinationLayerId=phrase"
        +"&leftOvers=on");  // partitioning tokens in whole graph: this must be unset
      fail("Should fail with invalid leftOvers");
    } catch (InvalidConfigurationException x) {
      // System.out.println(x.toString());
    }    
    
    annotator.setTaskParameters(
      "boundaryLayerId=turn"
      +"&tokenLayerId=word"
      +"&partitionSize=5"
      +"&maxPartitions="
      +"&alignment=invalid"
      +"&excludeOnAttribute=transcript_type"
      +"&excludeOnAttributeValues=wordlist,test%20graph"
      +"&destinationLayerId=partition"
      +"&leftOvers=on");
    assertEquals("Invalid alignment defaults to 'start'",
                 "start", annotator.getAlignment());
    
  }   
   
  /** Ensure basic partitioning works. */
  @Test public void basicPartitioning() throws Exception {
      
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
      
    // use specified configuration
    annotator.setTaskParameters(
      "boundaryLayerId=turn"
      +"&tokenLayerId=word"
      +"&partitionSize=5"
      +"&maxPartitions="
      +"&excludeOnAttribute=transcript_type"
      +"&excludeOnAttributeValues=wordlist"
      +"&destinationLayerId=partition"
      +"&leftOvers=on");
      
    assertEquals("boundary layer", "turn", annotator.getBoundaryLayerId());
    assertEquals("token layer", "word", annotator.getTokenLayerId());
    assertEquals("partition size", Double.valueOf(5), annotator.getPartitionSize());
    assertNull("maximum partitions", annotator.getMaxPartitions());
    assertEquals("alignment defaults to 'start'", "start", annotator.getAlignment());
    assertEquals("exclusion attribute",
                 "transcript_type", annotator.getExcludeOnAttribute());
    assertEquals("exclusion values", "wordlist", annotator.getExcludeOnAttributeValues());
    assertTrue("leftovers", annotator.getLeftOvers());
    assertEquals("partition layer", "partition", annotator.getDestinationLayerId());
    Layer partitionLayer = schema.getLayer("partition");
    assertNotNull("partition layer created", partitionLayer);
    assertEquals("partition layer child of turn",
                 "turn", partitionLayer.getParentId());
    assertEquals("partition layer aligned",
                 Constants.ALIGNMENT_INTERVAL, partitionLayer.getAlignment());
    assertEquals("partition layer type correct",
                 Constants.TYPE_NUMBER, partitionLayer.getType());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("3 required layers: "+requiredLayers,
                 3, requiredLayers.size());
    assertTrue("boundary layer required "+requiredLayers,
               requiredLayers.contains("turn"));
    assertTrue("token layer required "+requiredLayers, requiredLayers.contains("word"));
    assertTrue("exclusion attribute required "+requiredLayers,
               requiredLayers.contains("transcript_type"));
    String outputLayers[] = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                 1, outputLayers.length);
    assertEquals("output layer correct "+Arrays.asList(outputLayers),
                 "partition", outputLayers[0]);

    // // some sanity checks on the graph
    // assertEquals(2, g.all("participant").length);
    // assertEquals(2, g.all("turn").length);
    // assertEquals(1, g.first("participant").all("turn").length);
    // assertEquals(6, g.first("turn").all("utterance").length);
    // assertEquals(60, g.first("turn").all("word").length);
    // assertEquals(Double.valueOf(0.0), g.first("word").getStart().getOffset());
    // assertEquals(Double.valueOf(1.0), g.first("word").getEnd().getOffset());
    // assertEquals(Double.valueOf(0.5), g.first("word").first("morpheme").getEnd().getOffset());

    // run the annotator
    // annotator.getStatusObservers().add(s->System.out.println(s));
    annotator.transform(g);
    Annotation[] turns = g.all("turn");

    // first turn
    Annotation[] partitions = turns[0].all("partition");
    assertEquals("Correct number of partitions "+Arrays.asList(partitions),
                 12, partitions.length);
    assertEquals("First partition start "+partitions[0].getStart(),
                 Double.valueOf(0), partitions[0].getStart().getOffset());
    assertEquals("First partition end "+partitions[0].getEnd(),
                 Double.valueOf(5), partitions[0].getEnd().getOffset());
    assertEquals("Last partition start "+partitions[11].getStart(),
                 Double.valueOf(55), partitions[11].getStart().getOffset());
    assertEquals("Last partition end "+partitions[11].getEnd(),
                 Double.valueOf(60), partitions[11].getEnd().getOffset());

    // second turn
    partitions = turns[1].all("partition");
    assertEquals("Correct number of partitions "+Arrays.asList(partitions),
                 12, partitions.length);
    assertEquals("First partition start "+partitions[0].getStart(),
                 Double.valueOf(40), partitions[0].getStart().getOffset());
    assertEquals("First partition end "+partitions[0].getEnd(),
                 Double.valueOf(45), partitions[0].getEnd().getOffset());
    assertEquals("Last partition start "+partitions[11].getStart(),
                 Double.valueOf(95), partitions[11].getStart().getOffset());
    assertEquals("Last partition end "+partitions[11].getEnd(),
                 Double.valueOf(100), partitions[11].getEnd().getOffset());

    // each partition has the right label and number of tokens
    partitions = g.all("partition");
    for (int p = 0; p < partitions.length; p++) {
      assertEquals("Partition "+p+" label", "5", partitions[p].getLabel());
      assertEquals("Partition "+p+" tokens", 5, partitions[p].all("word").length);
    }
  }
  
  /** Attribute exclusion skips correct transcripts. */
  @Test public void excludeOnAttribute() throws Exception {
      
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
      
    // use specified configuration
    annotator.setTaskParameters(
      "boundaryLayerId=turn"
      +"&tokenLayerId=word"
      +"&partitionSize=5"
      +"&maxPartitions="
      +"&excludeOnAttribute=transcript_type"
      +"&excludeOnAttributeValues=wordlist"
      +"&destinationLayerId=partition"
      +"&leftOvers=on");
      
    assertEquals("exclusion attribute",
                 "transcript_type", annotator.getExcludeOnAttribute());
    assertEquals("exclusion values", "wordlist", annotator.getExcludeOnAttributeValues());
    // tag the graph for exclusion
    g.createTag(g, "transcript_type", "wordlist");

    // run the annotator
    // annotator.getStatusObservers().add(s->System.out.println(s));
    annotator.transform(g);
    Annotation[] turns = g.all("turn");

    // first turn
    Annotation[] partitions = g.all("partition");
    assertEquals("No partitions "+Arrays.asList(partitions),
                 0, partitions.length);
  }
  
  /** Ensure maxPartitions and alignment settings are respected. */
  @Test public void maxPartitionsAndAlignment() throws Exception {
      
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
      
    // alignment=start
    annotator.setTaskParameters(
      "boundaryLayerId=turn"
      +"&tokenLayerId=word"
      +"&partitionSize=10"
      +"&maxPartitions=3"
      +"&alignment=start"
      +"&destinationLayerId=partition");
      
    assertEquals("partition size", Double.valueOf(10), annotator.getPartitionSize());
    assertEquals("maximum partitions", Integer.valueOf(3), annotator.getMaxPartitions());
    assertEquals("alignment", "start", annotator.getAlignment());
    assertNull("exclusion attribute", annotator.getExcludeOnAttribute());
    assertNull("exclusion values", annotator.getExcludeOnAttributeValues());
    assertFalse("leftovers", annotator.getLeftOvers());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("2 required layers: "+requiredLayers,
                 2, requiredLayers.size());
    assertTrue("boundary layer required "+requiredLayers,
               requiredLayers.contains("turn"));
    assertTrue("token layer required "+requiredLayers, requiredLayers.contains("word"));

    // run the annotator
    // annotator.getStatusObservers().add(s->System.out.println(s));
    annotator.transform(g);
    Annotation[] turns = g.all("turn");

    // first turn
    Annotation[] partitions = turns[0].all("partition");
    assertEquals("align=start: Correct number of partitions "+Arrays.asList(partitions),
                 3, partitions.length);
    assertEquals("align=start: First partition start "+partitions[0].getStart(),
                 Double.valueOf(0), partitions[0].getStart().getOffset());
    assertEquals("align=start: First partition end "+partitions[0].getEnd(),
                 Double.valueOf(10), partitions[0].getEnd().getOffset());
    assertEquals("align=start: Last partition start "+partitions[2].getStart(),
                 Double.valueOf(20), partitions[2].getStart().getOffset());
    assertEquals("align=start: Last partition end "+partitions[2].getEnd(),
                 Double.valueOf(30), partitions[2].getEnd().getOffset());

    // second turn
    partitions = turns[1].all("partition");
    assertEquals("align=start: Correct number of partitions "+Arrays.asList(partitions),
                 3, partitions.length);
    assertEquals("align=start: First partition start "+partitions[0].getStart(),
                 Double.valueOf(40), partitions[0].getStart().getOffset());
    assertEquals("align=start: First partition end "+partitions[0].getEnd(),
                 Double.valueOf(50), partitions[0].getEnd().getOffset());
    assertEquals("align=start: Last partition start "+partitions[2].getStart(),
                 Double.valueOf(60), partitions[2].getStart().getOffset());
    assertEquals("align=start: Last partition end "+partitions[2].getEnd(),
                 Double.valueOf(70), partitions[2].getEnd().getOffset());

    // each partition has the right label and number of tokens
    partitions = g.all("partition");
    for (int p = 0; p < partitions.length; p++) {
      assertEquals("align=start: Partition "+p+" label", "10", partitions[p].getLabel());
      assertEquals("align=start: Partition "+p+" tokens",
                   10, partitions[p].all("word").length);
    }

    // alignment=end
    g = graph();
    annotator.setSchema(g.getSchema());
    annotator.setTaskParameters(
      "boundaryLayerId=turn"
      +"&tokenLayerId=word"
      +"&partitionSize=10"
      +"&maxPartitions=3"
      +"&alignment=end"
      +"&destinationLayerId=partition");      
    assertEquals("alignment", "end", annotator.getAlignment());

    // run the annotator
    annotator.transform(g);
    turns = g.all("turn");

    // first turn
    partitions = turns[0].all("partition");
    assertEquals("align=end: Correct number of partitions "+Arrays.asList(partitions),
                 3, partitions.length);
    assertEquals("align=end: First partition start "+partitions[0].getStart(),
                 Double.valueOf(30), partitions[0].getStart().getOffset());
    assertEquals("align=end: First partition end "+partitions[0].getEnd(),
                 Double.valueOf(40), partitions[0].getEnd().getOffset());
    assertEquals("align=end: Last partition start "+partitions[2].getStart(),
                 Double.valueOf(50), partitions[2].getStart().getOffset());
    assertEquals("align=end: Last partition end "+partitions[2].getEnd(),
                 Double.valueOf(60), partitions[2].getEnd().getOffset());

    // second turn
    partitions = turns[1].all("partition");
    assertEquals("align=end: Correct number of partitions "+Arrays.asList(partitions),
                 3, partitions.length);
    assertEquals("align=end: First partition start "+partitions[0].getStart(),
                 Double.valueOf(70), partitions[0].getStart().getOffset());
    assertEquals("align=end: First partition end "+partitions[0].getEnd(),
                 Double.valueOf(80), partitions[0].getEnd().getOffset());
    assertEquals("align=end: Last partition start "+partitions[2].getStart(),
                 Double.valueOf(90), partitions[2].getStart().getOffset());
    assertEquals("align=end: Last partition end "+partitions[2].getEnd(),
                 Double.valueOf(100), partitions[2].getEnd().getOffset());

    // each partition has the right label and number of tokens
    partitions = g.all("partition");
    for (int p = 0; p < partitions.length; p++) {
      assertEquals("align=end: Partition "+p+" label", "10", partitions[p].getLabel());
      assertEquals("align=end: Partition "+p+" tokens",
                   10, partitions[p].all("word").length);
    }
    
    // alignment=middle (odd number)
    g = graph();
    annotator.setSchema(g.getSchema());
    annotator.setTaskParameters(
      "boundaryLayerId=turn"
      +"&tokenLayerId=word"
      +"&partitionSize=10"
      +"&maxPartitions=3"
      +"&alignment=middle"
      +"&destinationLayerId=partition");      
    assertEquals("alignment", "middle", annotator.getAlignment());
    assertFalse("leftOvers", annotator.getLeftOvers());

    // run the annotator
    annotator.transform(g);
    turns = g.all("turn");

    // first turn
    partitions = turns[0].all("partition");
    assertEquals("align=middle: Correct number of partitions "+Arrays.asList(partitions),
                 3, partitions.length);
    assertEquals("align=middle: First partition start "+partitions[0].getStart(),
                 Double.valueOf(15), partitions[0].getStart().getOffset());
    assertEquals("align=middle: First partition end "+partitions[0].getEnd(),
                 Double.valueOf(25), partitions[0].getEnd().getOffset());
    assertEquals("align=middle: Last partition start "+partitions[2].getStart(),
                 Double.valueOf(35), partitions[2].getStart().getOffset());
    assertEquals("align=middle: Last partition end "+partitions[2].getEnd(),
                 Double.valueOf(45), partitions[2].getEnd().getOffset());

    // second turn
    partitions = turns[1].all("partition");
    assertEquals("align=middle: Correct number of partitions "+Arrays.asList(partitions),
                 3, partitions.length);
    assertEquals("align=middle: First partition start "+partitions[0].getStart(),
                 Double.valueOf(55), partitions[0].getStart().getOffset());
    assertEquals("align=middle: First partition end "+partitions[0].getEnd(),
                 Double.valueOf(65), partitions[0].getEnd().getOffset());
    assertEquals("align=middle: Last partition start "+partitions[2].getStart(),
                 Double.valueOf(75), partitions[2].getStart().getOffset());
    assertEquals("align=middle: Last partition end "+partitions[2].getEnd(),
                 Double.valueOf(85), partitions[2].getEnd().getOffset());

    // each partition has the right label and number of tokens
    partitions = g.all("partition");
    for (int p = 0; p < partitions.length; p++) {
      assertEquals("align=middle: Partition "+p+" label", "10", partitions[p].getLabel());
      assertEquals("align=middle: Partition "+p+" tokens",
                   10, partitions[p].all("word").length);
    }

    // alignment=middle (even number)
    g = graph();
    annotator.setSchema(g.getSchema());
    annotator.setTaskParameters(
      "boundaryLayerId=turn"
      +"&tokenLayerId=word"
      +"&partitionSize=10"
      +"&maxPartitions=2"
      +"&alignment=middle"
      +"&destinationLayerId=partition");
    assertEquals("alignment", "middle", annotator.getAlignment());
    assertEquals("maxPartitions", Integer.valueOf(2), annotator.getMaxPartitions());
    assertFalse("leftOvers", annotator.getLeftOvers());

    // run the annotator
    annotator.transform(g);
    turns = g.all("turn");

    // first turn
    partitions = turns[0].all("partition");
    assertEquals("align=middle+leftovers: Correct number of partitions "
                 +Arrays.asList(partitions),
                 2, partitions.length);
    assertEquals("align=middle+leftovers: First partition start "+partitions[0].getStart(),
                 Double.valueOf(20), partitions[0].getStart().getOffset());
    assertEquals("align=middle+leftovers: First partition end "+partitions[0].getEnd(),
                 Double.valueOf(30), partitions[0].getEnd().getOffset());
    assertEquals("align=middle+leftovers: First partition label",
                 "10", partitions[0].getLabel());
    assertEquals("align=middle+leftovers: First partition tokens",
                 10, partitions[0].all("word").length);
    assertEquals("align=middle+leftovers: Last partition start "+partitions[1].getStart(),
                 Double.valueOf(30), partitions[1].getStart().getOffset());
    assertEquals("align=middle+leftovers: Last partition end "+partitions[1].getEnd(),
                 Double.valueOf(40), partitions[1].getEnd().getOffset());
    assertEquals("align=middle+leftovers: Last partition label",
                 "10", partitions[1].getLabel());
    assertEquals("align=middle+leftovers: Last partition tokens",
                 10, partitions[1].all("word").length);

    // second turn
    partitions = turns[1].all("partition");
    assertEquals("align=middle+leftovers: Correct number of partitions "
                 +Arrays.asList(partitions),
                 2, partitions.length);
    assertEquals("align=middle+leftovers: First partition start "+partitions[0].getStart(),
                 Double.valueOf(60), partitions[0].getStart().getOffset());
    assertEquals("align=middle+leftovers: First partition end "+partitions[0].getEnd(),
                 Double.valueOf(70), partitions[0].getEnd().getOffset());
    assertEquals("align=middle+leftovers: Last partition start "+partitions[1].getStart(),
                 Double.valueOf(70), partitions[1].getStart().getOffset());
    assertEquals("align=middle+leftovers: Last partition end "+partitions[1].getEnd(),
                 Double.valueOf(80), partitions[1].getEnd().getOffset());

  }

  /** Ensure leftovers never exceed maxPartitions, and are never longer than partitions. */
  @Test public void leftovers() throws Exception {
      
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // more leftover than maxPartitions
    annotator.setTaskParameters(
      "boundaryLayerId=turn"
      +"&tokenLayerId=word"
      +"&partitionSize=25"
      +"&maxPartitions=2" // two partitions only, no leftovers
      +"&alignment=middle"
      +"&destinationLayerId=partition"
      +"&leftOvers=true");
    assertEquals("alignment", "middle", annotator.getAlignment());
    assertEquals("maxPartitions", Integer.valueOf(2), annotator.getMaxPartitions());
    assertTrue("leftOvers", annotator.getLeftOvers());

    // run the annotator
    // annotator.getStatusObservers().add(s->System.out.println(s));
    annotator.transform(g);
    Annotation[] turns = g.all("turn");

    Annotation[] partitions = turns[0].all("partition");
    assertEquals("Correct number of partitions (no leftovers) "+Arrays.asList(partitions),
                 2, partitions.length);
    assertEquals("First partition start "+partitions[0].getStart(),
                 Double.valueOf(5), partitions[0].getStart().getOffset());
    assertEquals("First partition end "+partitions[0].getEnd(),
                 Double.valueOf(30), partitions[0].getEnd().getOffset());
    assertEquals("First partition label",
                 "25", partitions[0].getLabel());
    assertEquals("First partition tokens",
                 25, partitions[0].all("word").length);
    assertEquals("Last partition start "+partitions[1].getStart(),
                 Double.valueOf(30), partitions[1].getStart().getOffset());
    assertEquals("Last partition end "+partitions[1].getEnd(),
                 Double.valueOf(55), partitions[1].getEnd().getOffset());
    assertEquals("Last partition label",
                 "25", partitions[1].getLabel());
    assertEquals("Last partition tokens",
                 25, partitions[1].all("word").length);

    // leftover included in maxPartitions
    g = graph();
    annotator.setSchema(g.getSchema());
    annotator.setTaskParameters(
      "boundaryLayerId=turn"
      +"&tokenLayerId=word"
      +"&partitionSize=25"
      +"&maxPartitions=4" // two partitions, two leftovers
      +"&alignment=middle"
      +"&destinationLayerId=partition"
      +"&leftOvers=true");
    assertEquals("alignment", "middle", annotator.getAlignment());
    assertEquals("maxPartitions", Integer.valueOf(4), annotator.getMaxPartitions());
    assertTrue("leftOvers", annotator.getLeftOvers());

    // run the annotator
    annotator.transform(g);
    turns = g.all("turn");
    partitions = turns[0].all("partition");

    assertEquals("Correct number of partitions (incl leftovers) "+Arrays.asList(partitions),
                 4, partitions.length);
    assertEquals("Leftover partition start "
                 +partitions[0].getStart(),
                 Double.valueOf(0), partitions[0].getStart().getOffset());
    assertEquals("Leftover partition end "+partitions[0].getEnd(),
                 Double.valueOf(5), partitions[0].getEnd().getOffset());
    assertEquals("Leftover partition label",
                 "5", partitions[0].getLabel());
    assertEquals("Leftover partition tokens",
                 5, partitions[0].all("word").length);
    assertEquals("First partition start "+partitions[1].getStart(),
                 Double.valueOf(5), partitions[1].getStart().getOffset());
    assertEquals("First partition end "+partitions[1].getEnd(),
                 Double.valueOf(30), partitions[1].getEnd().getOffset());
    assertEquals("First partition label",
                 "25", partitions[1].getLabel());
    assertEquals("First partition tokens",
                 25, partitions[1].all("word").length);
    assertEquals("Last partition start "+partitions[2].getStart(),
                 Double.valueOf(30), partitions[2].getStart().getOffset());
    assertEquals("Last partition end "+partitions[2].getEnd(),
                 Double.valueOf(55), partitions[2].getEnd().getOffset());
    assertEquals("Last partition label",
                 "25", partitions[2].getLabel());
    assertEquals("Last partition tokens",
                 25, partitions[2].all("word").length);
    assertEquals("Leftover partition start "
                 +partitions[3].getStart(),
                 Double.valueOf(55), partitions[3].getStart().getOffset());
    assertEquals("Leftover partition end "+partitions[3].getEnd(),
                 Double.valueOf(60), partitions[3].getEnd().getOffset());
    assertEquals("Leftover partition label",
                 "5", partitions[3].getLabel());
    assertEquals("Leftover partition tokens",
                 5, partitions[3].all("word").length);

  }

  /** Partitioning to a token tag layer - i.e. 'copy middle 30 tokens' usage. */
  @Test public void tagTokens() throws Exception {
      
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
      
    // use specified configuration
    annotator.setTaskParameters(
      "boundaryLayerId=turn"
      +"&tokenLayerId=word"
      +"&partitionSize=30"
      +"&maxPartitions=1"
      +"&alignment=middle"
      +"&destinationLayerId=wordTag"); // copy 'word' to 'wordTag' labels
      
    assertEquals("boundary layer", "turn", annotator.getBoundaryLayerId());
    assertEquals("token layer", "word", annotator.getTokenLayerId());
    assertEquals("partition size", Double.valueOf(30), annotator.getPartitionSize());
    assertEquals("maximum partitions", Integer.valueOf(1), annotator.getMaxPartitions());
    assertEquals("alignment", "middle", annotator.getAlignment());
    assertFalse("leftovers", annotator.getLeftOvers());
    assertEquals("partition layer", "wordTag", annotator.getDestinationLayerId());
    Layer partitionLayer = schema.getLayer("wordTag");
    assertEquals("partition layer child of word",
                 "word", partitionLayer.getParentId());
    assertEquals("partition layer not aligned",
                 Constants.ALIGNMENT_NONE, partitionLayer.getAlignment());
    assertEquals("partition layer type correct",
                 Constants.TYPE_STRING, partitionLayer.getType());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("2 required layers: "+requiredLayers,
                 2, requiredLayers.size());
    assertTrue("boundary layer required "+requiredLayers,
               requiredLayers.contains("turn"));
    assertTrue("token layer required "+requiredLayers, requiredLayers.contains("word"));
    String outputLayers[] = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                 1, outputLayers.length);
    assertEquals("output layer correct "+Arrays.asList(outputLayers),
                 "wordTag", outputLayers[0]);
    
    // run the annotator
    // annotator.getStatusObservers().add(s->System.out.println(s));
    annotator.transform(g);
    Annotation[] turns = g.all("turn");

    // first turn
    Annotation[] partitions = turns[0].all("wordTag");
    assertEquals("Correct number of partition tokens "+Arrays.asList(partitions),
                 30, partitions.length);
    assertEquals("First token start "+partitions[0].getStart(),
                 Double.valueOf(15), partitions[0].getStart().getOffset());
    assertEquals("First token end "+partitions[0].getEnd(),
                 Double.valueOf(16), partitions[0].getEnd().getOffset());
    assertEquals("Last token start "+partitions[29].getStart(),
                 Double.valueOf(44), partitions[29].getStart().getOffset());
    assertEquals("Last token end "+partitions[29].getEnd(),
                 Double.valueOf(45), partitions[29].getEnd().getOffset());

    // second turn
    partitions = turns[1].all("wordTag");
    assertEquals("Correct number of partition tokens "+Arrays.asList(partitions),
                 30, partitions.length);
    assertEquals("First token start "+partitions[0].getStart(),
                 Double.valueOf(55), partitions[0].getStart().getOffset());
    assertEquals("First token end "+partitions[0].getEnd(),
                 Double.valueOf(56), partitions[0].getEnd().getOffset());
    assertEquals("Last token start "+partitions[29].getStart(),
                 Double.valueOf(84), partitions[29].getStart().getOffset());
    assertEquals("Last token end "+partitions[29].getEnd(),
                 Double.valueOf(85), partitions[29].getEnd().getOffset());

    // each partition has the right label and number of tokens
    partitions = g.all("wordTag");
    for (int p = 0; p < partitions.length; p++) {
      assertEquals("Partition "+p+" tokens", 1, partitions[p].all("word").length);
      assertEquals("Partition "+p+" label copy of word",
                   partitions[p].getParent().getLabel(), partitions[p].getLabel());
    }
  }
  
  /** Partition by offset works. */
  @Test public void partitionByOffset() throws Exception {
      
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
      
    // use specified configuration
    annotator.setTaskParameters(
      "boundaryLayerId=topic"
      +"&tokenLayerId=" // no token layer ID
      +"&partitionSize=9.5"
      +"&maxPartitions=6" // enough to include leftovers
      +"&alignment=middle"
      +"&destinationLayerId=partition"
      +"&leftOvers=on");
      
    assertEquals("boundary layer", "topic", annotator.getBoundaryLayerId());
    assertNull("no token layer", annotator.getTokenLayerId());
    assertEquals("partition size", Double.valueOf(9.5), annotator.getPartitionSize());
    assertEquals("maximum partitions", Integer.valueOf(6), annotator.getMaxPartitions());
    assertEquals("alignment", "middle", annotator.getAlignment());
    assertTrue("leftovers", annotator.getLeftOvers());
    assertEquals("partition layer", "partition", annotator.getDestinationLayerId());
    Layer partitionLayer = schema.getLayer("partition");
    assertNotNull("partition layer created", partitionLayer);
    assertEquals("partition layer top level like 'topic'",
                 "transcript", partitionLayer.getParentId());
    assertEquals("partition layer aligned",
                 Constants.ALIGNMENT_INTERVAL, partitionLayer.getAlignment());
    assertEquals("partition layer type correct",
                 Constants.TYPE_NUMBER, partitionLayer.getType());
    String[] requiredLayers = annotator.getRequiredLayers();
    assertEquals("1 required layer: "+Arrays.asList(requiredLayers),
                 1, requiredLayers.length);
    assertEquals("boundary layer required "+Arrays.asList(requiredLayers),
                 "topic", requiredLayers[0]);
    String[] outputLayers = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                 1, outputLayers.length);
    assertEquals("output layer correct "+Arrays.asList(outputLayers),
                 "partition", outputLayers[0]);

    // run the annotator
    // annotator.getStatusObservers().add(s->System.out.println(s));
    annotator.transform(g);
    Annotation[] turns = g.all("turn");

    // first turn
    Annotation[] partitions = g.all("partition");
    assertEquals("Correct number of partitions "+Arrays.asList(partitions),
                 8, partitions.length);
    Double[] starts = {
      25.0, 28.0, 37.5, 47.0,
      75.0, 78.0, 87.5, 97.0
    };
    Double[] ends = {
      28.0, 37.5, 47.0, 50.0,
      78.0, 87.5, 97.0, 100.0
    };
    String[] labels = {
      "3.000", "9.5", "9.5", "3.000",
      "3.000", "9.5", "9.5", "3.000"
    };
    for (int p = 0; p < partitions.length; p++) {
      assertEquals("partition "+p+" start",
                   starts[p], partitions[p].getStart().getOffset());
      assertEquals("partition "+p+" end",
                   ends[p], partitions[p].getEnd().getOffset());
      assertEquals("partition "+p+" label",
                   labels[p], partitions[p].getLabel());
      assertEquals("partition "+p+" parent", g.getId(), partitions[p].getParentId());
    } // next partition

    // turn boundary, left alignment, no leftovers
    g = graph();
    annotator.setSchema(g.getSchema());
    annotator.setTaskParameters(
      "boundaryLayerId=turn"
      +"&tokenLayerId=" // no token layer ID
      +"&partitionSize=5.5"
      +"&maxPartitions=2" // not enough to include leftovers
      +"&alignment=start"
      +"&destinationLayerId=phrase"
      +"&leftOvers=on");
      
    assertEquals("boundary layer", "turn", annotator.getBoundaryLayerId());
    assertNull("no token layer", annotator.getTokenLayerId());
    assertEquals("partition size", Double.valueOf(5.5), annotator.getPartitionSize());
    assertEquals("maximum partitions", Integer.valueOf(2), annotator.getMaxPartitions());
    assertEquals("alignment", "start", annotator.getAlignment());
    assertTrue("leftovers", annotator.getLeftOvers());
    assertEquals("partition layer", "phrase", annotator.getDestinationLayerId());
    partitionLayer = schema.getLayer("phrase");
    assertEquals("partition layer child of turn",
                 "turn", partitionLayer.getParentId());
    assertEquals("partition layer aligned",
                 Constants.ALIGNMENT_INTERVAL, partitionLayer.getAlignment());
    assertEquals("partition layer left as before",
                 Constants.TYPE_STRING, partitionLayer.getType());
    requiredLayers = annotator.getRequiredLayers();
    assertEquals("1 required layer: "+Arrays.asList(requiredLayers),
                 1, requiredLayers.length);
    assertEquals("boundary layer required "+Arrays.asList(requiredLayers),
                 "turn", requiredLayers[0]);
    outputLayers = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                 1, outputLayers.length);
    assertEquals("output layer correct "+Arrays.asList(outputLayers),
                 "phrase", outputLayers[0]);

    // run the annotator
    annotator.transform(g);

    for (Annotation turn : g.all("turn")) {
      partitions = turn.all("phrase");
      assertEquals("Correct number of partitions "+Arrays.asList(partitions),
                   2, partitions.length);
      for (int p = 0; p < partitions.length; p++) {
        assertEquals("partition "+p+" start",
                     Double.valueOf(turn.getStart().getOffset() + (p*5.5)),
                     partitions[p].getStart().getOffset());
        assertEquals("partition "+p+" end",
                     Double.valueOf(turn.getStart().getOffset() + ((p+1)*5.5)),
                     partitions[p].getEnd().getOffset());
        assertEquals("partition "+p+" label", "5.5", partitions[p].getLabel());
        assertEquals("partition "+p+" parent", turn.getId(), partitions[p].getParentId());
      } // next partition
    } // next turn

    // graph boundary, right alignment, no leftovers
    g = graph();
    annotator.setSchema(g.getSchema());
    annotator.setTaskParameters(
      "boundaryLayerId=transcript"
      +"&tokenLayerId=" // no token layer ID
      +"&partitionSize=5.5"
      +"&maxPartitions=2"
      +"&alignment=end"
      +"&destinationLayerId=partition");
    
    assertEquals("boundary layer", "transcript", annotator.getBoundaryLayerId());
    assertNull("no token layer", annotator.getTokenLayerId());
    assertEquals("partition size", Double.valueOf(5.5), annotator.getPartitionSize());
    assertEquals("maximum partitions", Integer.valueOf(2), annotator.getMaxPartitions());
    assertEquals("alignment", "end", annotator.getAlignment());
    assertFalse("leftovers", annotator.getLeftOvers());
    assertEquals("partition layer", "partition", annotator.getDestinationLayerId());
    partitionLayer = schema.getLayer("partition");
    assertEquals("partition layer is top level",
                 "transcript", partitionLayer.getParentId());
    assertEquals("partition layer aligned",
                 Constants.ALIGNMENT_INTERVAL, partitionLayer.getAlignment());
    assertEquals("partition layer type correct",
                 Constants.TYPE_NUMBER, partitionLayer.getType());
    requiredLayers = annotator.getRequiredLayers();
    assertEquals("1 required layer: "+Arrays.asList(requiredLayers),
                 1, requiredLayers.length);
    assertEquals("boundary layer required "+Arrays.asList(requiredLayers),
                 "transcript", requiredLayers[0]);
    outputLayers = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                 1, outputLayers.length);
    assertEquals("output layer correct "+Arrays.asList(outputLayers),
                 "partition", outputLayers[0]);

    // run the annotator
    annotator.transform(g);

    partitions = g.all("partition");
    assertEquals("Correct number of partitions "+Arrays.asList(partitions),
                 2, partitions.length);
    assertEquals("partition 0 start",
                 Double.valueOf(89),
                 partitions[0].getStart().getOffset());
    assertEquals("partition 0 end",
                 Double.valueOf(94.5),
                 partitions[0].getEnd().getOffset());
    assertEquals("partition 0 label", "5.5", partitions[0].getLabel());
    assertEquals("partition 1 parent", g.getId(), partitions[1].getParentId());
    assertEquals("partition 1 start",
                 Double.valueOf(94.5),
                 partitions[1].getStart().getOffset());
    assertEquals("partition 1 end",
                 Double.valueOf(100),
                 partitions[1].getEnd().getOffset());
    assertEquals("partition 1 label", "5.5", partitions[1].getLabel());
    assertEquals("partition 1 parent", g.getId(), partitions[1].getParentId());

  }
  
  /** Ensure partitions can cross turn boundaries if required (i.e. multiple partitions).
   * i.e. boundary is whole graph, token layer is phrase or word layer*/
  @Test public void crossTurnPartitioning() throws Exception {
      
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
      
    // use specified configuration
    annotator.setTaskParameters(
      "boundaryLayerId=transcript"
      +"&tokenLayerId=word"
      +"&partitionSize=70" // enough for all the first turn and some of the second
      +"&destinationLayerId=phrase");
      
    assertEquals("boundary layer", "transcript", annotator.getBoundaryLayerId());
    assertEquals("token layer", "word", annotator.getTokenLayerId());
    assertEquals("partition size", Double.valueOf(70), annotator.getPartitionSize());
    assertEquals("maximum partitions set to 1",
                 Integer.valueOf(1), annotator.getMaxPartitions());
    assertEquals("alignment defaults to 'start'", "start", annotator.getAlignment());
    assertFalse("leftovers", annotator.getLeftOvers());
    assertEquals("partition layer", "phrase", annotator.getDestinationLayerId());
    Layer partitionLayer = schema.getLayer("phrase");
    assertEquals("partition layer child of turn",
                 "turn", partitionLayer.getParentId());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("2 required layers: "+requiredLayers,
                 2, requiredLayers.size());
    assertTrue("boundary layer required "+requiredLayers,
               requiredLayers.contains("transcript"));
    assertTrue("token layer required "+requiredLayers, requiredLayers.contains("word"));
    String outputLayers[] = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                 1, outputLayers.length);
    assertEquals("output layer correct "+Arrays.asList(outputLayers),
                 "phrase", outputLayers[0]);

    // run the annotator
    // annotator.getStatusObservers().add(s->System.out.println(s));
    annotator.transform(g);
    Annotation[] turns = g.all("turn");

    // first turn
    Annotation[] partitions = turns[0].all("phrase");
    assertEquals("Correct number of partitions "+Arrays.asList(partitions),
                 1, partitions.length);
    assertTrue("First part covers whole turn "
               +partitions[0].getStart()+"-"+partitions[0].getEnd(),
               partitions[0].tags(turns[0]));
    assertEquals("First part has correct label", "70", partitions[0].getLabel());
    assertEquals("First part has correct number of tokens",
                 60, partitions[0].all("word").length);

    // second turn
    partitions = turns[1].all("phrase");
    assertEquals("Correct number of partitions "+Arrays.asList(partitions),
                 1, partitions.length);
    assertEquals("Second part starts with second turn "+partitions[0].getStart(),
                 turns[1].getStart(), partitions[0].getStart());
    assertEquals("Second part ends ten words in "+partitions[0].getEnd(),
                 Double.valueOf(turns[1].getStart().getOffset() + 10),
                 partitions[0].getEnd().getOffset());
    assertEquals("Second part has correct label", "70", partitions[0].getLabel());
    assertEquals("Second part has correct number of tokens",
                 10, partitions[0].all("word").length);

  }

  /** Ensure token layer can be a sub-word layer, e.g. morphemes, segments, etc. */
  @Test public void subWordPartitioning() throws Exception {
      
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
      
    // use specified configuration
    annotator.setTaskParameters(
      "boundaryLayerId=transcript" // top level
      +"&tokenLayerId=morpheme" // sub-word layer
      +"&partitionSize=11" // odd number will end/start mid-word
      +"&destinationLayerId=span" // top level
      +"&leftOvers=true");
      
    // // some sanity checks on the graph
    // assertEquals(120, g.all("word").length);
    // assertEquals(""+Arrays.asList(g.all("morpheme")), 240, g.all("morpheme").length);
    
    assertEquals("boundary layer", "transcript", annotator.getBoundaryLayerId());
    assertEquals("token layer", "morpheme", annotator.getTokenLayerId());
    assertEquals("partition size", Double.valueOf(11), annotator.getPartitionSize());
    assertNull("maximum partitions unset", annotator.getMaxPartitions());
    assertEquals("alignment defaults to 'start'", "start", annotator.getAlignment());
    assertTrue("leftovers", annotator.getLeftOvers());
    assertEquals("partition layer", "span", annotator.getDestinationLayerId());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("2 required layers: "+requiredLayers,
                 2, requiredLayers.size());
    assertTrue("boundary layer required "+requiredLayers,
               requiredLayers.contains("transcript"));
    assertTrue("token layer required "+requiredLayers, requiredLayers.contains("morpheme"));
    String outputLayers[] = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                 1, outputLayers.length);
    assertEquals("output layer correct "+Arrays.asList(outputLayers),
                 "span", outputLayers[0]);

    // run the annotator
    // annotator.getStatusObservers().add(s->System.out.println(s));
    annotator.transform(g);
    Annotation[] partitions = g.all("span");
    assertEquals("Correct number of partitions "+Arrays.asList(partitions),
                 // 60 words in the first turn, 2 morphemes per word = 120 tokens
                 11, // 120 / 11 = 10 + 1 remainder
                 partitions.length);
    assertEquals("First partitition starts at zero",
                 Double.valueOf(0), partitions[0].getStart().getOffset());
    assertEquals("First partitition ends after 11 half seconds",
                 Double.valueOf(11*0.5), partitions[0].getEnd().getOffset());
    assertEquals("Second partitition starts at 11 half seconds",
                 Double.valueOf(11*0.5), partitions[1].getStart().getOffset());
    assertEquals("First partitition ends after 22 half seconds",
                 Double.valueOf(11), partitions[1].getEnd().getOffset());

    for (int p = 0; p < partitions.length - 1; p++) {
      assertEquals("Partition "+p+" label", "11", partitions[p].getLabel());
      assertEquals("Partition "+p+" tokens",
                   11, partitions[p].all("morpheme").length);
    }
    assertEquals("Last partition label", "10", partitions[partitions.length-1].getLabel());
    assertEquals("Last partition tokens",
                 10, partitions[partitions.length-1].all("morpheme").length);
    assertEquals("Last partition ends with first turn",
                 g.first("turn").getEnd(), partitions[partitions.length-1].getEnd());

  }
  
  /**
   * Returns a graph for annotating.
   * @return The graph for testing with.
   */
  public static Graph graph() {
    Schema schema = new Schema(
      "participant", "turn", "utterance", "word",
      new Layer("transcript_type", "Type of transcript")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("topic", "Topic").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false),
      new Layer("span", "Spans").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false),
      new Layer("participant", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("participant").setParentIncludes(true),
      new Layer("phrase", "Phrase layer").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("wordTag", "Word tags").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true),
      new Layer("morpheme", "Morphemes").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true));
    // annotate a graph
    Graph g = new Graph()
      .setSchema(schema);
    g.setId("test-graph");
    g.getOrCreateAnchorAt(0);
    g.getOrCreateAnchorAt(100);

    // transcript type
    g.createTag(g, "transcript_type", "test graph");

    // a couple of non-contiguous topic tags
    g.createAnnotation​(
      g.getOrCreateAnchorAt(25), g.getOrCreateAnchorAt(50), "topic", "topic 1", g);
    g.createAnnotation​(
      g.getOrCreateAnchorAt(75), g.getOrCreateAnchorAt(100), "topic", "topic 1", g);

    // two speakers
    Annotation p1 = g.createTag(g, "participant", "participant 1");
    Annotation p2 = g.createTag(g, "participant", "participant 2");

    // two turns that overlap
    Annotation turn1 = g.createAnnotation​(
      g.getOrCreateAnchorAt(0), g.getOrCreateAnchorAt(60), "turn", "turn 1", p1);
    Annotation turn2 = g.createAnnotation​(
      g.getOrCreateAnchorAt(40), g.getOrCreateAnchorAt(100), "turn", "turn 2", p2);

    // an utterance every ten seconds in each turn
    for (int start = 0; start < 60; start += 10) {
      g.createAnnotation​(
        g.getOrCreateAnchorAt(turn1.getStart().getOffset() + start),
        g.getOrCreateAnchorAt(turn1.getStart().getOffset() + start + 10),
        "utterance", "utterance 1-" + ((start/10)+1), turn1);
      g.createAnnotation​(
        g.getOrCreateAnchorAt(turn2.getStart().getOffset() + start),
        g.getOrCreateAnchorAt(turn2.getStart().getOffset() + start + 10),
        "utterance", "utterance 2-" + ((start/10)+1), turn2);
    }
    
    // a word every second in each utterance
    for (int start = 0; start < 60; start++) {
      Annotation word = g.createAnnotation​(
        g.getOrCreateAnchorAt(turn1.getStart().getOffset() + start),
        g.getOrCreateAnchorAt(turn1.getStart().getOffset() + start + 1),
        "word", "word 1-" + (start+1), turn1);
      g.createAnnotation​(
        g.getOrCreateAnchorAt(turn2.getStart().getOffset() + start),
        g.getOrCreateAnchorAt(turn2.getStart().getOffset() + start + 1),
        "word", "word 2-" + (start+1), turn2);

      // each of participant 1's words divided into two morphemes
      Annotation m1 = g.createSubdivision​(word, "morpheme", word.getLabel() + " m1");
      Annotation m2 = g.createSubdivision​(word, "morpheme", word.getLabel() + " m2");
      // set offset of intervening anchor
      m1.getEnd().setOffset(start + 0.5);
    }
    return g;
  } // end of graph()

  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.annotator.partitioner.TestPartitioner");
  }
}
