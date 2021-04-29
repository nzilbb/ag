//
// Copyright 2018-2021 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.formatter.kaldi;
	      
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
import java.util.SortedSet;
import java.util.Vector;
import nzilbb.ag.*;
import nzilbb.ag.serialize.SerializationException;
import nzilbb.ag.serialize.json.JSONSerialization;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.editpath.EditStep;
import nzilbb.editpath.MinimumEditPath;
import nzilbb.formatter.kaldi.*;

public class TestKaldiSerializer {
  
  /** Serialization of a collection of graph fragments */
  @Test public void basicFragmentSerialization() throws Exception {
    Graph g = new Graph();
    g.setId("test.trs");
      
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("episode", "Episode").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("who", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("gender", "Speaker gender").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("who").setParentIncludes(true),
      new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("orthography", "Orthography").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true),
      new Layer("phonemes", "Pronunciation").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true)
      .setType(Constants.TYPE_IPA)
      );
    schema.setEpisodeLayerId("episode");
    g.setSchema(schema);

    g.addAnchor(new Anchor("turnStart", 0.0));
    g.addAnchor(new Anchor("a1", 0.0));
    g.addAnchor(new Anchor("a2", 1.0));
    g.addAnchor(new Anchor("a3a", 3.0));
    g.addAnchor(new Anchor("utterance", 3.0));
    g.addAnchor(new Anchor("a3b", 4.0));
    g.addAnchor(new Anchor("a4", 5.0));
    g.addAnchor(new Anchor("a5", 6.0));
    g.addAnchor(new Anchor("turnEnd", 6.0));

    g.addAnnotation(new Annotation("ep1", "episode", "episode", "turnStart", "turnEnd", "episode"));
      
    g.addAnnotation(new Annotation("who1", "john smith", "who", "turnStart", "turnEnd", "test.trs"));
    g.addAnnotation(new Annotation("gender1", "male", "gender", "turnStart", "turnEnd", "who1"));
    g.addAnnotation(new Annotation("who2", "jane doe", "who", "turnStart", "turnEnd", "test.trs"));

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "who1"));

    g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utterance", "turn1"));
    g.addAnnotation(new Annotation("utterance2", "john smith", "utterance", "utterance", "turnEnd", "turn1"));

    g.addAnnotation(new Annotation("word1", "The", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("orth1", "the", "orthography", "a1", "a2", "word1"));
    g.addAnnotation(new Annotation("pron1a", "D@", "phonemes", "a1", "a2", "word1"));
    g.addAnnotation(new Annotation("pron1b", "Di", "phonemes", "a1", "a2", "word1"));
    g.addAnnotation(new Annotation("word2", "quick", "word", "a2", "a3a", "turn1"));
    g.addAnnotation(new Annotation("orth2", "quick", "orthography", "a2", "a3a", "word2"));
    g.addAnnotation(new Annotation("pron2", "kwIk", "phonemes", "a2", "a3a", "word2"));
    g.addAnnotation(new Annotation("word3", "BROWN", "word", "a3b", "a4", "turn1"));
    g.addAnnotation(new Annotation("orth3", "brown", "orthography", "a2", "a4", "word3"));
    g.addAnnotation(new Annotation("pron3", "br6n", "phonemes", "a2", "a4", "word3"));
    g.addAnnotation(new Annotation("word4", "fox", "word", "a4", "a5", "turn1"));
    g.addAnnotation(new Annotation("orth4", "fox", "orthography", "a4", "a5", "word4"));
    g.addAnnotation(new Annotation("pron4", "f$ks", "phonemes", "a4", "a5", "word4"));

    // create deserializer
    KaldiSerializer serializer = new KaldiSerializer();

    ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
    //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    // configuration.get("episodeLayer").setValue(schema.getLayer("episode"));
    // configuration.get("orthographyLayer").setValue(schema.getLayer("orthography"));
    // configuration.get("prefixUtteranceId").setValue(Boolean.FALSE);
    assertEquals(5, serializer.configure(configuration, schema).size());

    // some layers required
    String[] requiredLayers = serializer.getRequiredLayers();
    assertEquals(6, requiredLayers.length);
    assertEquals("episode", requiredLayers[0]);
    assertEquals("who", requiredLayers[1]);
    assertEquals("utterance", requiredLayers[2]);
    assertEquals("orthography", requiredLayers[3]);
    assertEquals("phonemes", requiredLayers[4]);
    assertEquals("gender", requiredLayers[5]);

    // split out fragments
    String[] allLayers = {
      "episode", "who", "gender", "turn", "utterance", "word", "orthography", "phonemes"};
    Graph[] fragments = {
      g.getFragment(g.getAnnotation("utterance1"), allLayers),
      g.getFragment(g.getAnnotation("utterance2"), allLayers)
    };

    // offset anchors of fragments
    for (Graph fragment : fragments) {
      fragment.shiftAnchors(-fragment.getStart().getOffset());
    }

    // serialize
    File dir = getDir();
    final Vector<SerializationException> exceptions = new Vector<SerializationException>();
    final Vector<NamedStream> streams = new Vector<NamedStream>();
    serializer.serialize(Arrays.spliterator(fragments), allLayers,
                         stream -> streams.add(stream),
                         warning -> System.out.println(warning),
                         exception -> exceptions.add(exception));
    assertEquals(8, streams.size());
    for (NamedStream stream: streams) {
      stream.save(dir);
    }

    // make sure the streams are right
    String differences = "";
    for (NamedStream stream: streams) {      
      // compare with what we expected
      File fActual = new File(dir, stream.getName());
      Vector<String> actualLines = new Vector<String>();
      BufferedReader reader = new BufferedReader(new FileReader(fActual));
      String line = reader.readLine();
      while (line != null) {
        actualLines.add(line);
        line = reader.readLine();
      }
      File fExpected = new File(dir, "expected_" + stream.getName());
      Vector<String> expectedLines = new Vector<String>();
      reader = new BufferedReader(new FileReader(fExpected));
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
            differences += "\n"+fExpected.getPath()+":"+(step.getFromIndex()+1)+": Expected:\n" 
              + step.getFrom() 
              + "\n"+fActual.getPath()+":"+(step.getToIndex()+1)+": Found:\n" + step.getTo();
            break;
          case DELETE:
            differences += "\n"+fExpected.getPath()+":"+(step.getFromIndex()+1)+": Deleted:\n" 
              + step.getFrom();
            break;
          case INSERT:
            differences += "\n"+fActual.getPath()+":"+(step.getToIndex()+1)+": Inserted:\n" 
              + step.getTo();
            break;
        }
      } // next step
      if (differences.length() == 0) fActual.delete();
    } // next file
    if (differences.length() > 0) {
      fail(differences);
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
    org.junit.runner.JUnitCore.main("nzilbb.formatter.kaldi.TestKaldiSerializer");
  }
}
