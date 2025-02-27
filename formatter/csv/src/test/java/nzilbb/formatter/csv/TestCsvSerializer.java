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
package nzilbb.formatter.csv;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import nzilbb.ag.*;
import nzilbb.ag.serialize.SerializationException;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.formatter.csv.*;
import nzilbb.editpath.EditStep;
import nzilbb.editpath.MinimumEditPath;

public class TestCsvSerializer
{      
   @Test public void basicSerialization()  throws Exception {
      Graph g = createGraph("basicSerialization.trs");
      
      // create deserializer
      CsvSerializer serializer = new CsvSerializer();
      
      ParameterSet configuration = serializer.configure(new ParameterSet(), g.getSchema());
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(1, serializer.configure(configuration, g.getSchema()).size());
      assertEquals("minimumAnchorConfidence", Integer.valueOf(50), 
                   (Integer)configuration.get("minimumAnchorConfidence").getValue());
      
      // no layers required
      String[] requiredLayers = serializer.getRequiredLayers();
      assertEquals(0, requiredLayers.length);
      
      // serialize
      Graph[] graphs = { g };
      File dir = getDir();
      String[] layerIds = {"who", "word"};
      final Vector<SerializationException> exceptions = new Vector<SerializationException>();
      final Vector<NamedStream> streams = new Vector<NamedStream>();
      serializer.serialize(Arrays.spliterator(graphs), layerIds,
                           stream -> streams.add(stream),
                           warning -> System.out.println(warning),
                           exception -> exceptions.add(exception));
      assertEquals(1, streams.size());
      streams.elementAt(0).save(dir);
      
      File actual = new File(dir, streams.elementAt(0).getName());
      String differences = diff(
         new File(dir, "expected_" + streams.elementAt(0).getName()),
         actual);
      if (differences != null) {
         fail(differences);
      } else {
         actual.delete();
      }
   }

   @Test public void ignoreConfidence()  throws Exception {
      Graph g = createGraph("ignoreConfidence.trs");
      
      // create deserializer
      CsvSerializer serializer = new CsvSerializer();
      
      ParameterSet configuration = serializer.configure(new ParameterSet(), g.getSchema());
      assertEquals(1, configuration.size());
      assertEquals("minimumAnchorConfidence", Integer.valueOf(50), 
                   (Integer)configuration.get("minimumAnchorConfidence").getValue());
      configuration.get("minimumAnchorConfidence").setValue(Integer.valueOf(0));
      
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(1, serializer.configure(configuration, g.getSchema()).size());
      assertEquals("minimumAnchorConfidence", Integer.valueOf(0), 
                   (Integer)configuration.get("minimumAnchorConfidence").getValue());
      
      // no layers required
      String[] requiredLayers = serializer.getRequiredLayers();
      assertEquals(0, requiredLayers.length);
      
      // serialize
      Graph[] graphs = { g };
      File dir = getDir();
      String[] layerIds = {"who", "word"};
      final Vector<SerializationException> exceptions = new Vector<SerializationException>();
      final Vector<NamedStream> streams = new Vector<NamedStream>();
      serializer.serialize(Arrays.spliterator(graphs), layerIds,
                           stream -> streams.add(stream),
                           warning -> System.out.println(warning),
                           exception -> exceptions.add(exception));
      assertEquals(1, streams.size());
      streams.elementAt(0).save(dir);
      
      File actual = new File(dir, streams.elementAt(0).getName());
      String differences = diff(
         new File(dir, "expected_" + streams.elementAt(0).getName()),
         actual);
      if (differences != null) {
         fail(differences);
      } else {
         actual.delete();
      }
   }

   @Test public void withAttributes()  throws Exception {
      Graph g = createGraph("withAttributes.trs");
      
      // create deserializer
      CsvSerializer serializer = new CsvSerializer();
      
      ParameterSet configuration = serializer.configure(new ParameterSet(), g.getSchema());
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(1, serializer.configure(configuration, g.getSchema()).size());
      assertEquals("minimumAnchorConfidence", Integer.valueOf(50), 
                   (Integer)configuration.get("minimumAnchorConfidence").getValue());
      
      // no layers required
      String[] requiredLayers = serializer.getRequiredLayers();
      assertEquals(0, requiredLayers.length);
      
      // serialize
      Graph[] graphs = { g };
      File dir = getDir();
      String[] layerIds = {"transcript_transcriber", "who", "word"};
      final Vector<SerializationException> exceptions = new Vector<SerializationException>();
      final Vector<NamedStream> streams = new Vector<NamedStream>();
      serializer.serialize(Arrays.spliterator(graphs), layerIds,
                           stream -> streams.add(stream),
                           warning -> System.out.println(warning),
                           exception -> exceptions.add(exception));
      assertEquals(1, streams.size());
      streams.elementAt(0).save(dir);

      File actual = new File(dir, streams.elementAt(0).getName());
      String differences = diff(
         new File(dir, "expected_" + streams.elementAt(0).getName()),
         actual);
      if (differences != null) {
         fail(differences);
      } else {
         actual.delete();
      }
   }

   @Test public void multipleLayers()  throws Exception {
      Graph g = createGraph("multipleLayers.trs");
      
      // create deserializer
      CsvSerializer serializer = new CsvSerializer();
      
      ParameterSet configuration = serializer.configure(new ParameterSet(), g.getSchema());
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(1, serializer.configure(configuration, g.getSchema()).size());
      assertEquals("minimumAnchorConfidence", Integer.valueOf(50), 
                   (Integer)configuration.get("minimumAnchorConfidence").getValue());
      
      // no layers required
      String[] requiredLayers = serializer.getRequiredLayers();
      assertEquals(0, requiredLayers.length);
      
      // serialize
      Graph[] graphs = { g };
      File dir = getDir();
      String[] layerIds = {"who", "word", "orthography"};
      final Vector<SerializationException> exceptions = new Vector<SerializationException>();
      final Vector<NamedStream> streams = new Vector<NamedStream>();
      serializer.serialize(Arrays.spliterator(graphs), layerIds,
                           stream -> streams.add(stream),
                           warning -> System.out.println(warning),
                           exception -> exceptions.add(exception));
      assertEquals(1, streams.size());
      streams.elementAt(0).save(dir);

      File actual = new File(dir, streams.elementAt(0).getName());
      String differences = diff(
         new File(dir, "expected_" + streams.elementAt(0).getName()),
         actual);
      if (differences != null) {
         fail(differences);
      } else {
         actual.delete();
      }
   }
   
   /**
    * Creates a test graph.
    * @return The graph for testing.
    */
   public Graph createGraph(String id)
   {
      Graph g = new Graph();
      g.setId(id);
      
      Schema schema = new Schema(
         "who", "turn", "utterance", "word",
         new Layer("episode", "Episode")
         .setAlignment(Constants.ALIGNMENT_NONE) 
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true),
         new Layer("transcript_transcriber", "Transcriber")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true),
         new Layer("who", "Participants")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(true)
         .setPeersOverlap(true)
         .setSaturated(true),
         new Layer("turn", "Speaker turns")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true)
         .setPeersOverlap(false)
         .setSaturated(false)
         .setParentId("who")
         .setParentIncludes(true),  
         new Layer("utterance", "Utterances")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true)
         .setPeersOverlap(false)
         .setSaturated(true)
         .setParentId("turn")
         .setParentIncludes(true), 
         new Layer("word", "Words")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true)
         .setPeersOverlap(false)
         .setSaturated(false)
         .setParentId("turn")
         .setParentIncludes(true),
         new Layer("orthography", "Orthography")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true)
         .setParentId("word")
         .setParentIncludes(true) 
         );
      g.setSchema(schema);
      
      g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL));
      g.addAnchor(new Anchor("a1", 0.0, Constants.CONFIDENCE_AUTOMATIC));
      g.addAnchor(new Anchor("a2", 1.0, Constants.CONFIDENCE_AUTOMATIC));
      g.addAnchor(new Anchor("a3a", 3.0, Constants.CONFIDENCE_AUTOMATIC));
      g.addAnchor(new Anchor("utterance", 3.0, Constants.CONFIDENCE_MANUAL));
      g.addAnchor(new Anchor("a3b", 4.0, Constants.CONFIDENCE_DEFAULT));
      g.addAnchor(new Anchor("a4", 5.0, Constants.CONFIDENCE_DEFAULT));
      g.addAnchor(new Anchor("a5", 6.0, Constants.CONFIDENCE_DEFAULT));
      g.addAnchor(new Anchor("turnEnd", 6.0, Constants.CONFIDENCE_MANUAL));
      
      g.addAnnotation(new Annotation("ep1", "episode", "episode", "turnStart", "turnEnd", "episode"));
      
      g.addAnnotation(new Annotation("who1", "john smith", "who", "turnStart", "turnEnd", "test.trs"));
      g.addAnnotation(new Annotation("who2", "jane doe", "who", "turnStart", "turnEnd", "test.trs"));
      
      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "who1"));
      
      g.addAnnotation(new Annotation("utterance1", "john smith", "utterance", "turnStart", "utterance", "turn1"));
      g.addAnnotation(new Annotation("utterance2", "john smith", "utterance", "utterance", "turnEnd", "turn1"));
      
      g.addAnnotation(new Annotation("word1", "The", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("orth1", "the", "orthography", "a1", "a2", "word1"));
      g.addAnnotation(new Annotation("word2", "quick", "word", "a2", "a3a", "turn1"));
      g.addAnnotation(new Annotation("orth2", "quick", "orthography", "a2", "a3a", "word2"));
      g.addAnnotation(new Annotation("word3", "BROWN", "word", "a3b", "a4", "turn1"));
      g.addAnnotation(new Annotation("ord3", "brown", "orthography", "a2", "a4", "word3"));
      g.addAnnotation(new Annotation("word4", "fox", "word", "a4", "a5", "turn1"));
      g.addAnnotation(new Annotation("orth3", "fox", "orthography", "a4", "a5", "word4"));

      g.addAnnotation(new Annotation("trans", "Robert Fromont", "transcript_transcriber", g.getId()));

      return g;
   } // end of createGraph()
   
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
      org.junit.runner.JUnitCore.main("nzilbb.formatter.csv.TestCsvSerializer");
   }
}
