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

package nzilbb.formatter.whisper;
	      
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
import nzilbb.formatter.whisper.*;

/** Test WhisperDeserializer */
public class TestWhisperDeserializer {
  /** Ensure a multi-utterance transcript is correctly deserialized and tokenized */
  @Test public void tokenized()  throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Speakers")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterances")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Word tokens")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true));
    
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "test.wt")) };
    
    // create deserializer
    WhisperDeserializer deserializer = new WhisperDeserializer();

    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals("Configuration parameters" + configuration, 0,
                 deserializer.configure(configuration, schema).size());      
    
    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals(0, defaultParameters.size());
    
    // configure the deserialization
    deserializer.setParameters(defaultParameters);

    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];

    // lines with no timestamp are ignored but warned about
    for (String warning : deserializer.getWarnings()) {
      assertEquals("Warning for comment line", "Invalid line: \"# New turn here:\"", warning);
    }
    
    assertEquals("test.wt", g.getId());
    assertEquals("time units", Constants.UNIT_SECONDS, g.getOffsetUnits());
    
    
    // participants     
    Annotation[] speakers = g.all("who"); 
    assertEquals(1, speakers.length);
    assertEquals("test.wt", speakers[0].getLabel());
    
    // turns
    Annotation[] turns = g.all("turn");
    // a turn break was added by having an utterance start after the previous utterance end
    assertEquals("Two turns", 2, turns.length);
    assertEquals("Turn 1 label is speaker name",
                 "test.wt", turns[0].getLabel());
    assertEquals("Turn 2 label is speaker name",
                 "test.wt", turns[1].getLabel());
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    assertEquals(Double.valueOf(14.240), turns[0].getEnd().getOffset());
    assertEquals(Double.valueOf(15.0), turns[1].getStart().getOffset());
    assertEquals(Double.valueOf(331.8), turns[1].getEnd().getOffset());
    
    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals("Correct number of utterances", 53, utterances.length);
    assertEquals("Utterance label is speaker name",
                 "test.wt", utterances[0].getLabel());
    assertEquals("Start time of first utterance",
                 Double.valueOf(0.0), utterances[0].getStart().getOffset());
    assertEquals("End time of first utterance",
                 Double.valueOf(10.32), utterances[0].getEnd().getOffset());
    
    assertEquals("Start time of second utterance",
                 Double.valueOf(10.32), utterances[1].getStart().getOffset());
    assertEquals("End time of second utterance",
                 Double.valueOf(14.24), utterances[1].getEnd().getOffset());

    assertEquals("Start time of last utterance",
                 Double.valueOf(311.180), utterances[utterances.length-1].getStart().getOffset());
    assertEquals("End time of last utterance",
                 Double.valueOf(331.800), utterances[utterances.length-1].getEnd().getOffset());
    
    // words
    Annotation[] words = g.all("word");
    assertEquals("Correct number of words", 696, words.length);
    String[] checkWords = {
      "The", "rest", "of", "that", "side", "of", "the", "family,", "so", "he",
      "generously", "agreed", "that", "she", "could", "go", "with", "them,"};
    for (int w = 0; w < checkWords.length; w++) {
      assertEquals("check word " + w + ": " + checkWords[w], checkWords[w], words[w].getLabel());
    } // next word
    
      // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      if (a.getLayerId().equals("who")) {
        assertEquals("Speaker has 'automatic' confidence: " + a.getLayer() + ": " + a,
                     Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC), a.getConfidence());
      } else {
        assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                     Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }
    }
  }
  
  @Test public void noWordLayer() throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", null, // no word layer
      new Layer("who", "Speakers")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterances")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true));
    
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "test.wt")) };
    
    // create deserializer
    WhisperDeserializer deserializer = new WhisperDeserializer();

    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals("Configuration wants missing word layer" + configuration, 1,
                 deserializer.configure(configuration, schema).size());      
    
    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParameters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
    assertEquals(0, defaultParameters.size());
    
    // configure the deserialization
    deserializer.setParameters(defaultParameters);

    // build the graph
    Graph[] graphs = deserializer.deserialize();
    Graph g = graphs[0];
    
    // lines with no timestamp are ignored but warned about
    for (String warning : deserializer.getWarnings()) {
      assertEquals("Warning for comment line", "Invalid line: \"# New turn here:\"", warning);
    }
    
    assertEquals("test.wt", g.getId());
    assertEquals("time units", Constants.UNIT_SECONDS, g.getOffsetUnits());    
    
    // participants     
    Annotation[] speakers = g.all("who"); 
    assertEquals(1, speakers.length);
    assertEquals("test.wt", speakers[0].getLabel());
    
    // turns
    Annotation[] turns = g.all("turn");
    // a turn break was added by having an utterance start after the previous utterance end
    assertEquals("Two turns", 2, turns.length);
    assertEquals("Turn 1 label is speaker name",
                 "test.wt", turns[0].getLabel());
    assertEquals("Turn 2 label is speaker name",
                 "test.wt", turns[1].getLabel());
    assertEquals(Double.valueOf(0.0), turns[0].getStart().getOffset());
    assertEquals(Double.valueOf(14.240), turns[0].getEnd().getOffset());
    assertEquals(Double.valueOf(15.0), turns[1].getStart().getOffset());
    assertEquals(Double.valueOf(331.8), turns[1].getEnd().getOffset());
    
    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals("Correct number of utterances", 53, utterances.length);
    assertEquals("Start time of first utterance",
                 Double.valueOf(0.0), utterances[0].getStart().getOffset());
    assertEquals("End time of first utterance",
                 Double.valueOf(10.32), utterances[0].getEnd().getOffset());
    
    assertEquals("Start time of second utterance",
                 Double.valueOf(10.32), utterances[1].getStart().getOffset());
    assertEquals("End time of second utterance",
                 Double.valueOf(14.24), utterances[1].getEnd().getOffset());

    assertEquals("Start time of last utterance",
                 Double.valueOf(311.180), utterances[utterances.length-1].getStart().getOffset());
    assertEquals("End time of last utterance",
                 Double.valueOf(331.800), utterances[utterances.length-1].getEnd().getOffset());

    assertEquals(
      "First utterance label is transcript",
      "The rest of that side of the family, so he generously agreed that she could go with them,",
      utterances[0].getLabel());
    assertEquals(
      "Second utterance label is transcript",
      "but there were so many people there that nothing was done constructively.",
      utterances[1].getLabel());
    assertEquals(
      "Last utterance label is transcript",
      "We were the oldest ones, of course I was seventeen, Barry was nineteen, and uh, he, she, Dad.",
      utterances[utterances.length-1].getLabel());
    
    // check all annotations have correct confidence    
    for (Annotation a : g.getAnnotationsById().values()) {
      if (a.getLayerId().equals("who")) {
        assertEquals("Speaker has 'automatic' confidence: " + a.getLayer() + ": " + a,
                     Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC), a.getConfidence());
      } else {
        assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                     Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }
    }
  }

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
    org.junit.runner.JUnitCore.main("nzilbb.formatter.whisper.TestWhisperDeserializer");
  }
}
