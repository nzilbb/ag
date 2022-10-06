//
// Copyright 2019 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.formatter.webvtt;
	      
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
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.util.Normalizer;
import nzilbb.ag.util.Validator;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.editpath.EditStep;
import nzilbb.editpath.MinimumEditPath;
import nzilbb.util.Timers;
import nzilbb.formatter.webvtt.*;

public class TestVttSerialization {

  /** Test a YouTube transcription. */
  @Test public void youtube()  throws Exception {
    Schema schema = new Schema("who", "turn", "utterance", "word",
                               new Layer("transcript_language", "Language", 0, true, true, true),
                               new Layer("kind", "Kind", 0, false, false, true),
                               new Layer("who", "Participants", 0, true, true, true),
                               new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
                               new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
                               new Layer("word", "Words", 2, true, false, false, "turn", true));

    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "youtube.vtt")) };
      
    // create deserializer
    VttSerialization deserializer = new VttSerialization();

    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals("Configuration parameters" + configuration, 0,
                 deserializer.configure(configuration, schema).size());      

    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals(2, defaultParameters.size());
    assertEquals("language", "transcript_language", 
                 ((Layer)defaultParameters.get("Language").getValue()).getId());
    assertEquals("kind", "kind", 
                 ((Layer)defaultParameters.get("Kind").getValue()).getId());
      
    // configure the deserialization
    deserializer.setParameters(defaultParameters);

    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
      
    assertEquals("youtube.vtt", g.getId());
    assertEquals("time units", Constants.UNIT_SECONDS, g.getOffsetUnits());

    // meta data
    assertEquals("graph meta data", 
                 "en-GB", g.first("transcript_language").getLabel());
    assertEquals("graph meta data", 
                 "captions", g.first("kind").getLabel());

    // participants     
    Annotation[] authors = g.all("who"); 
    assertEquals(1, authors.length);
    assertEquals("speaker", authors[0].getLabel());

    // turns
    Annotation[] turns = g.all("turn");
    assertEquals(1, turns.length);
    assertEquals("Turn label is speaker name",
                 "speaker", turns[0].getLabel());
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    assertEquals(Double.valueOf(2782.55), turns[0].getEnd().getOffset());

    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals("Utterance label is speaker name",
                 "speaker", utterances[0].getLabel());
    assertEquals(Double.valueOf(1.849), utterances[0].getStart().getOffset());
    assertEquals(Double.valueOf(6.43), utterances[0].getEnd().getOffset());

    assertEquals(Double.valueOf(6.43), utterances[1].getStart().getOffset());
    assertEquals(Double.valueOf(9.879), utterances[1].getEnd().getOffset());

    assertEquals(Double.valueOf(2777.96), utterances[utterances.length-1].getStart().getOffset());
    assertEquals(Double.valueOf(2782.55), utterances[utterances.length-1].getEnd().getOffset());

    // words
    Annotation[] words = g.all("word");
    assertEquals(7728, words.length);
    String[] checkWords = {
      "Before","we","move","to","the","first","question","to","the",
      "First","Minister,","I","invite","the","First","Minister",
      "to","make","a","few","remarks","following","the","tragic",
      "events","in","Christchurch","in","New","Zealand.",
      "The","First","Minister","(Nicola","Sturgeon):"};
    for (int w = 0; w < checkWords.length; w++) {
      assertEquals("check word " + w + ": " + checkWords[w], checkWords[w], words[w].getLabel());
    } // next word
      
      // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }
  }

  /** Test parsing with no metadata. */
  @Test public void noMetaData()  throws Exception {
    Schema schema = new Schema("who", "turn", "utterance", "word",
                               new Layer("who", "Participants", 0, true, true, true),
                               new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
                               new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
                               new Layer("word", "Words", 2, true, false, false, "turn", true));

    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "youtube.vtt")) };
      
    // create deserializer
    VttSerialization deserializer = new VttSerialization();

    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals("Configuration parameters" + configuration, 0,
                 deserializer.configure(configuration, schema).size());

    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals(2, defaultParameters.size());
    assertNull("language", (Layer)defaultParameters.get("Language").getValue());
    assertNull("kind", (Layer)defaultParameters.get("Kind").getValue());
      
    // configure the deserialization
    deserializer.setParameters(defaultParameters);

    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
      
    assertEquals("youtube.vtt", g.getId());
    assertEquals("time units", Constants.UNIT_SECONDS, g.getOffsetUnits());

    // meta data
    assertNull("graph meta data", g.first("transcript_language"));
    assertNull("graph meta data", g.first("kind"));

    // participants     
    Annotation[] authors = g.all("who"); 
    assertEquals(1, authors.length);
    assertEquals("speaker", authors[0].getLabel());

    // turns
    Annotation[] turns = g.all("turn");
    assertEquals(1, turns.length);
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    assertEquals(Double.valueOf(2782.55), turns[0].getEnd().getOffset());

    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals(Double.valueOf(1.849), utterances[0].getStart().getOffset());
    assertEquals(Double.valueOf(6.43), utterances[0].getEnd().getOffset());

    assertEquals(Double.valueOf(6.43), utterances[1].getStart().getOffset());
    assertEquals(Double.valueOf(9.879), utterances[1].getEnd().getOffset());

    assertEquals(Double.valueOf(2777.96), utterances[utterances.length-1].getStart().getOffset());
    assertEquals(Double.valueOf(2782.55), utterances[utterances.length-1].getEnd().getOffset());

    // utterances
    Annotation[] words = g.all("word");
    assertEquals(7728, words.length);
    String[] checkWords = {
      "Before","we","move","to","the","first","question","to","the",
      "First","Minister,","I","invite","the","First","Minister",
      "to","make","a","few","remarks","following","the","tragic",
      "events","in","Christchurch","in","New","Zealand.",
      "The","First","Minister","(Nicola","Sturgeon):"};
    for (int w = 0; w < checkWords.length; w++)
    {
      assertEquals("check word " + w + ": " + checkWords[w], checkWords[w], words[w].getLabel());
    } // next word
      
      // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }
  }

  /** Test parsing without tokensization. */
  @Test public void noWordLayer() throws Exception {
    Schema schema = new Schema("who", "turn", "utterance", null,
                               new Layer("transcript_language", "Language", 0, true, true, true),
                               new Layer("kind", "Kind", 0, false, false, true),
                               new Layer("who", "Participants", 0, true, true, true),
                               new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
                               new Layer("utterance", "Utterances", 2, true, false, true, "turn", true));

    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "youtube.vtt")) };
      
    // create deserializer
    VttSerialization deserializer = new VttSerialization();

    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals("Configuration parameters" + configuration, 1,
                 deserializer.configure(configuration, schema).size());      

    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals(2, defaultParameters.size());
    assertEquals("language", "transcript_language", 
                 ((Layer)defaultParameters.get("Language").getValue()).getId());
    assertEquals("kind", "kind", 
                 ((Layer)defaultParameters.get("Kind").getValue()).getId());
      
    // configure the deserialization
    deserializer.setParameters(defaultParameters);

    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    for (String warning : deserializer.getWarnings())
    {
      System.out.println(warning);
    }
      
    assertEquals("youtube.vtt", g.getId());
    assertEquals("time units", Constants.UNIT_SECONDS, g.getOffsetUnits());

    // meta data
    assertEquals("graph meta data", 
                 "en-GB", g.first("transcript_language").getLabel());
    assertEquals("graph meta data", 
                 "captions", g.first("kind").getLabel());

    // participants     
    Annotation[] authors = g.all("who"); 
    assertEquals(1, authors.length);
    assertEquals("speaker", authors[0].getLabel());

    // turns
    Annotation[] turns = g.all("turn");
    assertEquals(1, turns.length);
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    assertEquals(Double.valueOf(2782.55), turns[0].getEnd().getOffset());

    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals("Utterance label is transcript",
                 "Before we move to the first question to the First Minister, I invite the First Minister",
                 utterances[0].getLabel());
    assertEquals(571, utterances.length);
    assertEquals(Double.valueOf(1.849), utterances[0].getStart().getOffset());
    assertEquals(Double.valueOf(6.43), utterances[0].getEnd().getOffset());

    assertEquals(Double.valueOf(6.43), utterances[1].getStart().getOffset());
    assertEquals(Double.valueOf(9.879), utterances[1].getEnd().getOffset());

    assertEquals(Double.valueOf(2777.96), utterances[utterances.length-1].getStart().getOffset());
    assertEquals(Double.valueOf(2782.55), utterances[utterances.length-1].getEnd().getOffset());

      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }
  }


  /** Serialization of similtaneous speech. */
  @Test public void serializeSimultaneousSpeech() throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("language", "Language")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("gender", "Gender")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterances")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true));
    File dir = getDir();

    // create a graph with simultaneous speech turns

    Graph graph = new Graph()
      .setId("simultaneous_speech")
      .setSchema(schema);
    graph.addAnchor(new Anchor("a0", 0.0));
    graph.addAnchor(new Anchor("a15", 15.0));
    graph.addAnnotation(new Annotation("en", "en", "language", "a0", "a15"));
    // participants
    graph.addAnnotation(new Annotation("p1", "p1", "who", "a0", "a15"));
    graph.addAnnotation(new Annotation("p2", "p2", "who", "a0", "a15"));
    graph.addAnnotation(new Annotation("nb", "nb", "gender", "a0", "a15", "p2"));
    // turns
    graph.addAnnotation(new Annotation("t1", "p1", "turn", "a0", "a15", "p1"));
    graph.addAnchor(new Anchor("a5", 5.0));
    graph.addAnchor(new Anchor("a10", 10.0));
    graph.addAnnotation(new Annotation("t2", "p2", "turn", "a5", "a10", "p2"));
    // utterances
    graph.addAnnotation(new Annotation("u1-1", "p1", "utterance", "a0", "a5", "t1"));
    graph.addAnnotation(new Annotation("u1-2", "p1", "utterance", "a5", "a10", "t1"));
    graph.addAnnotation(new Annotation("u2-1", "p2", "utterance", "a5", "a10", "t2"));
    graph.addAnnotation(new Annotation("u1-3", "p1", "utterance", "a10", "a15", "t1"));

    // words
    graph.addAnnotation(new Annotation("w1-1", "w1-1", "word", 
                                       graph.addAnchor(new Anchor("a1", 1.0)).getId(),
                                       graph.addAnchor(new Anchor("a2", 2.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("w1-2", "w1-2", "word", 
                                       "a2",
                                       graph.addAnchor(new Anchor("a3", 3.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("w1-3", "w1-3", "word", 
                                       "a3",
                                       graph.addAnchor(new Anchor("a4", 4.0)).getId(),
                                       "t1"));
      
    graph.addAnnotation(new Annotation("w1-6", "w1-6", "word", 
                                       graph.addAnchor(new Anchor("a6", 6.0)).getId(),
                                       graph.addAnchor(new Anchor("a7", 7.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("w1-7", "w1-7", "word", 
                                       "a7", 
                                       graph.addAnchor(new Anchor("a8", 8.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("w1-8", "w1-8", "word", 
                                       "a8",
                                       graph.addAnchor(new Anchor("a9", 9.0)).getId(),
                                       "t1"));
      

    graph.addAnnotation(new Annotation("w1-11", "w1-11", "word", 
                                       graph.addAnchor(new Anchor("a11", 11.0)).getId(),
                                       graph.addAnchor(new Anchor("a12", 12.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("w1-12", "w1-12", "word", 
                                       "a12",
                                       graph.addAnchor(new Anchor("a13", 13.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("w1-13", "w1-13", "word", 
                                       "a13",
                                       graph.addAnchor(new Anchor("a14", 14.0)).getId(),
                                       "t1"));
      
    graph.addAnnotation(new Annotation("w2-6.5", "w2-6.5", "word", 
                                       graph.addAnchor(new Anchor("a6.5", 6.5)).getId(),
                                       graph.addAnchor(new Anchor("a7.5", 7.5)).getId(),
                                       "t2"));
    graph.addAnnotation(new Annotation("w2-7.5", "w2-7.5", "word", 
                                       "a7.5",
                                       graph.addAnchor(new Anchor("a8.5", 8.5)).getId(),
                                       "t2"));

    // create serializer
    VttSerialization serializer = new VttSerialization();
      
    // general configuration
    ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals(0, serializer.configure(configuration, schema).size());

    String[] needLayers = serializer.getRequiredLayers();
    assertEquals(4, needLayers.length);
    assertEquals("who", needLayers[0]);
    assertEquals("turn", needLayers[1]);
    assertEquals("utterance", needLayers[2]);
    assertEquals("word", needLayers[3]);
	 
    // serialize
    final Vector<SerializationException> exceptions = new Vector<SerializationException>();
    final Vector<NamedStream> streams = new Vector<NamedStream>();
    String[] layers = {"language", "gender"};
    serializer.serialize(Utility.OneGraphSpliterator(graph),
                         layers,
                         stream -> streams.add(stream),
                         warning -> System.out.println(warning),
                         exception -> exceptions.add(exception));
    if (exceptions.size() > 0) fail(""+exceptions);
      
    streams.elementAt(0).save(dir);
      
    // test using diff
    File result = new File(dir, graph.getId() + ".vtt");
    String differences = diff(new File(dir, "expected_" + graph.getId() + ".vtt"), result);
    if (differences != null) {
      fail(differences);
    } else {
      result.delete();
    }
  }

  /** Test round-trip works. */
  @Test public void canReadOwnOutput()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("transcript_language", "Language", 0, true, true, true),
      new Layer("kind", "Kind", 0, false, false, true),
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true));
      
    // access file
    NamedStream[] streams = {
      new NamedStream(new File(getDir(), "expected_simultaneous_speech.vtt")) };
      
    // create deserializer
    VttSerialization deserializer = new VttSerialization();

    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals("Configuration parameters" + configuration, 0,
                 deserializer.configure(configuration, schema).size());      

    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    //for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals("Kind, and meta-data NOTEs, become parameters", 4, defaultParameters.size());
      
    // configure the deserialization
    deserializer.setParameters(defaultParameters);

    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
      
    assertEquals("expected_simultaneous_speech.vtt", g.getId());
    assertEquals("time units", Constants.UNIT_SECONDS, g.getOffsetUnits());

    // participants     
    Annotation[] authors = g.all("who"); 
    assertEquals(1, authors.length);
    assertEquals("speaker", authors[0].getLabel());

    // turns
    Annotation[] turns = g.all("turn");
    assertEquals(1, turns.length);
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    assertEquals(Double.valueOf(15.0), turns[0].getEnd().getOffset());

    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals(4, utterances.length);
    assertEquals(Double.valueOf(0.0), utterances[0].getStart().getOffset());
    assertEquals(Double.valueOf(5.0), utterances[0].getEnd().getOffset());

    assertEquals(Double.valueOf(5.0), utterances[1].getStart().getOffset());
    assertEquals(Double.valueOf(10.0), utterances[1].getEnd().getOffset());

    assertEquals(Double.valueOf(10.0), utterances[utterances.length-1].getStart().getOffset());
    assertEquals(Double.valueOf(15.0), utterances[utterances.length-1].getEnd().getOffset());
      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }

  }

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
    org.junit.runner.JUnitCore.main("nzilbb.formatter.webvtt.TestVttSerialization");
  }
}
