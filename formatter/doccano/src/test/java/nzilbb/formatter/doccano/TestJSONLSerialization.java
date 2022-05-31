//
// Copyright 2022 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.formatter.doccano;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Vector;
import nzilbb.ag.*;
import nzilbb.ag.serialize.SerializationException;
import nzilbb.ag.serialize.json.JSONSerialization;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.util.Normalizer;
import nzilbb.ag.util.Validator;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.editpath.EditStep;
import nzilbb.editpath.MinimumEditPath;
import nzilbb.util.Timers;

public class TestJSONLSerialization {

  /** Test basic serialization works */
  @Test public void serialize() throws Exception {

    // create serializer
    JSONLSerialization serializer = new JSONLSerialization();
      
    // general configuration
    ParameterSet configuration = serializer.configure(new ParameterSet(), schema());
    //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals(0, serializer.configure(configuration, schema()).size());

    String[] needLayers = serializer.getRequiredLayers();
    assertEquals(4, needLayers.length);
    assertEquals("who", needLayers[0]);
    assertEquals("turn", needLayers[1]);
    assertEquals("utterance", needLayers[2]);
    assertEquals("word", needLayers[3]);
	 
    // serialize
    final Vector<SerializationException> exceptions = new Vector<SerializationException>();
    final Vector<NamedStream> streams = new Vector<NamedStream>();
    String[] layers = {"word", "topic"};
    Vector<Graph> graphs = new Vector<Graph>();
    graphs.add(graph1());
    graphs.add(graph2());
    serializer.serialize(graphs.spliterator(), layers,
                         stream -> streams.add(stream),
                         warning -> System.out.println(warning),
                         exception -> exceptions.add(exception));
    if (exceptions.size() > 0) {
      for (Exception x : exceptions) x.printStackTrace(System.out);
      fail(""+exceptions);
    }

    File dir = getDir();
    for (NamedStream stream : streams) {
      stream.save(dir);
    }

    // test using diff
    File result = new File(dir, "doccano-test-1-etc.jsonl");
    String differences = diff(new File(dir, "expected_doccano-test-1-etc.jsonl"), result);
    if (differences != null) {
      fail(differences);
    } else {
      result.delete();
    }
  }
  
  /**
   * Schema for test graphs.
   * @return Schema for test graphs.
   */
  public Schema schema() {
    return new Schema(
      "who", "turn", "utterance", "word",
      new Layer("topic", "Topic")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false)
      .setSaturated(false),
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true)
      .setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false)
      .setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterances")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false)
      .setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false)
      .setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("orthography", "Orthography")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false)
      .setSaturated(true)
      .setParentId("word"),
      new Layer("pos", "Part of Speech")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false)
      .setSaturated(true)
      .setParentId("word"));
  } // end of schema()
  
  /**
   * Graph for testing.
   * @return Test graph.
   */
  public Graph graph1() {
    Graph graph = new Graph()
      .setId("doccano-test-1")
      .setSchema(schema());
    graph.addAnchor(new Anchor("a0", 0.0));
    graph.addAnchor(new Anchor("a5", 5.5));
    graph.addAnchor(new Anchor("a10", 10.0));
    // participants
    graph.addAnnotation(new Annotation("author", "author", "who", "a0", "a10"));
    // turns
    graph.addAnnotation(new Annotation("t1", "author", "turn", "a0", "a10", "author"));
    // utterances
    graph.addAnnotation(new Annotation("u1", "author", "utterance", "a0", "a5", "t1"));
    graph.addAnnotation(new Annotation("u2", "author", "utterance", "a5", "a10", "t1"));

    // words
    graph.addAnnotation(new Annotation("the", "The", "word",
                                       "a0",
                                       graph.addAnchor(new Anchor("a1", 1.1)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("quick", "'quick", "word", 
                                       "a1",
                                       graph.addAnchor(new Anchor("a2", 2.2)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("brown", "brown'", "word", 
                                       "a2",
                                       graph.addAnchor(new Anchor("a3", 3.3)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("fox", "fox", "word", 
                                       "a3",
                                       graph.addAnchor(new Anchor("a4", 4.4)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("jumps", "jumps -", "word", 
                                       "a4",
                                       "a5",
                                       "t1"));
      
    graph.addAnnotation(new Annotation("over", "over", "word",
                                       "a5",
                                       graph.addAnchor(new Anchor("a6", 6.6)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("the2", "the", "word", 
                                       "a6",
                                       graph.addAnchor(new Anchor("a7", 7.7)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("lazy", "lazy", "word", 
                                       "a7",
                                       graph.addAnchor(new Anchor("a8", 8.8)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("dog", "\"dog\"", "word", 
                                       "a8",
                                       graph.addAnchor(new Anchor("a9", 9.9)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation(".", ".", "word", 
                                       "a9",
                                       "a10",
                                       "t1"));

    // orthography
    graph.addTag(graph.getAnnotation("the"), "orthography", "the");
    graph.addTag(graph.getAnnotation("quick"), "orthography", "quick");
    graph.addTag(graph.getAnnotation("brown"), "orthography", "brown");
    graph.addTag(graph.getAnnotation("fox"), "orthography", "fox");
    graph.addTag(graph.getAnnotation("jumps"), "orthography", "jumps");
    graph.addTag(graph.getAnnotation("over"), "orthography", "over");
    graph.addTag(graph.getAnnotation("the2"), "orthography", "the");
    graph.addTag(graph.getAnnotation("lazy"), "orthography", "lazy");
    graph.addTag(graph.getAnnotation("dog"), "orthography", "dog");

    // POS
    graph.addTag(graph.getAnnotation("the"), "pos", "DET");
    graph.addTag(graph.getAnnotation("quick"), "pos", "ADJ");
    graph.addTag(graph.getAnnotation("brown"), "pos", "ADJ");
    graph.addTag(graph.getAnnotation("fox"), "pos", "N");
    graph.addTag(graph.getAnnotation("jumps"), "pos", "V");
    graph.addTag(graph.getAnnotation("over"), "pos", "PREP");
    graph.addTag(graph.getAnnotation("the2"), "pos", "DET");
    graph.addTag(graph.getAnnotation("lazy"), "pos", "ADJ");
    graph.addTag(graph.getAnnotation("dog"), "pos", "N");
    graph.addTag(graph.getAnnotation("."), "pos", "PUNC");

    // topic
    graph.addAnnotation(new Annotation("topic-fox", "fox", "topic", "a0", "a6"));
    graph.addAnnotation(new Annotation("topic-dog", "dog", "topic", "a6", "a10"));

    return graph;
  } // end of graph1()
  
  /**
   * Graph for testing.
   * @return Test graph.
   */
  public Graph graph2() {
    Graph graph = new Graph()
      .setId("doccano-test-2")
      .setSchema(schema());
    graph.addAnchor(new Anchor("a0", 0.0));
    graph.addAnchor(new Anchor("a5", 5.5));
    graph.addAnchor(new Anchor("a10", 10.0));
    // participants
    graph.addAnnotation(new Annotation("speaker1", "speaker1", "who", "a0", "a10"));
    graph.addAnnotation(new Annotation("speaker2", "speaker2", "who", "a0", "a10"));
    // turns
    graph.addAnnotation(new Annotation("t1", "speaker1", "turn", "a0", "a10", "speaker1"));
    graph.addAnnotation(new Annotation("t2", "speaker2", "turn", "a0", "a10", "speaker2"));
    // utterances
    graph.addAnnotation(new Annotation("u1", "author", "utterance", "a0", "a5", "t1"));
    graph.addAnnotation(new Annotation("u2", "author", "utterance", "a5", "a10", "t2"));

    // words
    graph.addAnnotation(new Annotation("the", "The", "word",
                                       "a0",
                                       graph.addAnchor(new Anchor("a1", 1.1)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("quick", "'quick", "word", 
                                       "a1",
                                       graph.addAnchor(new Anchor("a2", 2.2)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("brown", "brown'", "word", 
                                       "a2",
                                       graph.addAnchor(new Anchor("a3", 3.3)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("fox", "fox", "word", 
                                       "a3",
                                       graph.addAnchor(new Anchor("a4", 4.4)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("jumps", "jumps -", "word", 
                                       "a4",
                                       "a5",
                                       "t1"));
      
    graph.addAnnotation(new Annotation("over", "over", "word",
                                       "a5",
                                       graph.addAnchor(new Anchor("a6", 6.6)).getId(),
                                       "t2"));
    graph.addAnnotation(new Annotation("the2", "the", "word", 
                                       "a6",
                                       graph.addAnchor(new Anchor("a7", 7.7)).getId(),
                                       "t2"));
    graph.addAnnotation(new Annotation("lazy", "lazy", "word", 
                                       "a7",
                                       graph.addAnchor(new Anchor("a8", 8.8)).getId(),
                                       "t2"));
    graph.addAnnotation(new Annotation("dog", "\"dog\"", "word", 
                                       "a8",
                                       graph.addAnchor(new Anchor("a9", 9.9)).getId(),
                                       "t2"));
    graph.addAnnotation(new Annotation(".", ".", "word", 
                                       "a9",
                                       "a10",
                                       "t2"));

    // orthography
    graph.addTag(graph.getAnnotation("the"), "orthography", "the");
    graph.addTag(graph.getAnnotation("quick"), "orthography", "quick");
    graph.addTag(graph.getAnnotation("brown"), "orthography", "brown");
    graph.addTag(graph.getAnnotation("fox"), "orthography", "fox");
    graph.addTag(graph.getAnnotation("jumps"), "orthography", "jumps");
    graph.addTag(graph.getAnnotation("over"), "orthography", "over");
    graph.addTag(graph.getAnnotation("the2"), "orthography", "the");
    graph.addTag(graph.getAnnotation("lazy"), "orthography", "lazy");
    graph.addTag(graph.getAnnotation("dog"), "orthography", "dog");

    // pos
    graph.addTag(graph.getAnnotation("the"), "pos", "DET");
    graph.addTag(graph.getAnnotation("quick"), "pos", "ADJ");
    graph.addTag(graph.getAnnotation("brown"), "pos", "ADJ");
    graph.addTag(graph.getAnnotation("fox"), "pos", "N");
    graph.addTag(graph.getAnnotation("jumps"), "pos", "V");
    graph.addTag(graph.getAnnotation("over"), "pos", "PREP");
    graph.addTag(graph.getAnnotation("the2"), "pos", "DET");
    graph.addTag(graph.getAnnotation("lazy"), "pos", "ADJ");
    graph.addTag(graph.getAnnotation("dog"), "pos", "N");
    graph.addTag(graph.getAnnotation("."), "pos", "PUNC");

    // topic
    graph.addAnnotation(new Annotation("topic-fox", "fox", "topic", "a0", "a4"));
    graph.addAnnotation(new Annotation("topic-dog", "dog", "topic", "a6", "a10"));

    return graph;
  } // end of graph2()

  /**
   * Diffs two files.
   * @param expected
   * @param actual
   * @return null if the files are the same, and a String describing differences if not.
   */
  public String diff(File expected, File actual) {
    StringBuffer d = new StringBuffer();
      
    try {
      // compare with what we expected
      Vector<String> actualLines = new Vector<String>();
      BufferedReader reader = new BufferedReader(new FileReader(actual));
      String line = reader.readLine();
      while (line != null) {
        actualLines.add(line);
        line = reader.readLine();
      }
      Vector<String> expectedLines = new Vector<String>();
      reader = new BufferedReader(new FileReader(expected));
      line = reader.readLine();
      while (line != null) {
        expectedLines.add(line);
        line = reader.readLine();
      }
      MinimumEditPath<String> comparator = new MinimumEditPath<String>();
      List<EditStep<String>> path = comparator.minimumEditPath(expectedLines, actualLines);
      for (EditStep<String> step : path) {
        switch (step.getOperation()) {
          case CHANGE:
            d.append("\n"+expected.getPath()+":"+(step.getFromIndex()+1)+": Expected:\n" 
                     + step.getFrom() 
                     + "\n"+actual.getPath()+":"+(step.getToIndex()+1)+": Found:\n" + step.getTo());
            break;
          case DELETE:
            d.append("\n"+expected.getPath()+":"+(step.getFromIndex()+1)+": Deleted:\n" 
                     + step.getFrom()
                     + "\n"+actual.getPath()+":"+(step.getToIndex()+1)+": Missing");
            break;
          case INSERT:
            d.append("\n"+expected.getPath()+":"+(step.getFromIndex()+1)+": Missing" 
                     + "\n"+actual.getPath()+":"+(step.getToIndex()+1)+": Inserted:\n" 
                     + step.getTo());
            break;
        }
      } // next step
    } catch(Exception exception) {
      d.append("\n" + exception);
    }
    if (d.length() > 0) return d.toString();
    return null;
  } // end of diff()

  /**
   * Directory for text files.
   * @see #getDir()
   * @see #setDir(File)
   */
  protected File fDir;
  /**
   * Getter for {@link #fDir}: Directory for text files.
   * @return Directory for text files.
   */
  public File getDir() { 
    if (fDir == null) {
      try {
        URL urlThisClass = getClass().getResource(getClass().getSimpleName() + ".class");
        File fThisClass = new File(urlThisClass.toURI());
        fDir = fThisClass.getParentFile();
      } catch(Throwable t) {
        System.out.println("" + t);
      }
    }
    return fDir; 
  }
  /**
   * Setter for {@link #fDir}: Directory for text files.
   * @param fNewDir Directory for text files.
   */
  public void setDir(File fNewDir) { fDir = fNewDir; }

  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.formatter.doccano.TestJSONLSerialization");
  }
}
