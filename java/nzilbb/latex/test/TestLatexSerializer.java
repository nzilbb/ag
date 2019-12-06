//
// Copyright 2017-2019 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.latex.test;
	      
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
import nzilbb.latex.*;
import nzilbb.util.Timers;

public class TestLatexSerializer
{

   @Test public void serialize()
      throws Exception
   {
      Schema schema = new Schema(
         "who", "turn", "utterance", "word",
	 new Layer("scribe", "Transcriber")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(true)
         .setPeersOverlap(true)
         .setSaturated(true),
         new Layer("who", "Participants")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(true)
         .setPeersOverlap(true)
         .setSaturated(true),
         new Layer("comment", "Comment", 2, true, false, true),
         new Layer("noise", "Noise", 2, true, false, true),
         new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
         new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
         new Layer("word", "Words", 2, true, false, false, "turn", true),
         new Layer("phone", "Phones", 2, true, true, true, "word", true),
         new Layer("lexical", "Lexical", 0, true, false, false, "word", true),
         new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
      File dir = getDir();
      // access file
      NamedStream[] jsonStreams = { new NamedStream(new File(dir, "serialize_utterance_word.json")) };
      
      // deserialize graph from JSON
      JSONSerialization json = new JSONSerialization();
      json.configure(json.configure(new ParameterSet(), schema), schema);
      json.setParameters(json.load(jsonStreams, schema));
      Graph[] graphs = json.deserialize();
      
      // create serializer
      LatexSerializer serializer = new LatexSerializer();
      
      // general configuration
      ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
      //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(6, serializer.configure(configuration, schema).size());
      assertNull("no orthography", 
                 configuration.get("orthographyLayer").getValue());
      assertEquals("noise", "noise", 
		   ((Layer)configuration.get("noiseLayer").getValue()).getId());
      assertEquals("texPreamble",
                   "", configuration.get("texPreamble").getValue());
      assertEquals("texBeginTranscript",
                   "\\begin{description}", configuration.get("texBeginTranscript").getValue());
      assertEquals("texTurnCommand",
                   "\\item[#1:] #2", configuration.get("texTurnCommand").getValue());
      assertEquals("texEndTranscript",
                   "\\end{description}", configuration.get("texEndTranscript").getValue());

      String[] needLayers = serializer.getRequiredLayers();
      assertEquals(4, needLayers.length);
      assertEquals("who", needLayers[0]);
      assertEquals("turn", needLayers[1]);
      assertEquals("utterance", needLayers[2]);
      assertEquals("word", needLayers[3]);
	 
      // serialize
      final Vector<SerializationException> exceptions = new Vector<SerializationException>();
      final Vector<NamedStream> streams = new Vector<NamedStream>();
      String[] layers = {"word","scribe"}; 
      serializer.serialize(Arrays.spliterator(graphs), layers,
                           stream -> streams.add(stream),
                           warning -> System.out.println(warning),
                           exception -> exceptions.add(exception));
      if (exceptions.size() > 0) fail(""+exceptions);

      streams.elementAt(0).save(dir);

      // test using diff
      File result = new File(dir, "serialize_utterance_word.tex");
      String differences = diff(new File(dir, "expected_serialize_utterance_word.tex"), result);
      if (differences != null)
      {
         fail(differences);
      }
      else
      {
         result.delete();
      }
   }

   @Test public void serializeSimultaneousSpeech()
      throws Exception
   {
      Schema schema = new Schema(
         "who", "turn", "utterance", "word",
         new Layer("noise", "Noise") 
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true)
         .setPeersOverlap(false)
         .setSaturated(false),
         new Layer("who", "Participants")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(true)
         .setPeersOverlap(true)
         .setSaturated(true),
         new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
         new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
         new Layer("word", "Words", 2, true, false, false, "turn", true),
         new Layer("orthography", "Orthography")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true)
         .setParentId("word"));
      File dir = getDir();

      // create a graph with simultaneous speech turns

      Graph graph = new Graph()
         .setId("simultaneous_speech")
         .setSchema(schema);
      graph.addAnchor(new Anchor("a0", 0.0));
      graph.addAnchor(new Anchor("a15", 15.0));
      // participants
      graph.addAnnotation(new Annotation("p1", "p1", "who", "a0", "a15"));
      graph.addAnnotation(new Annotation("p2", "p2", "who", "a0", "a15"));
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

      // noises
      graph.addAnnotation(new Annotation("n1", "n1", "noise", 
                                         "a2",
                                         "a2"));
      graph.addAnnotation(new Annotation("n2", "n2", "noise", 
                                         "a4",
                                         "a5"));      

      // add orthography tags that should not be used because orthography is not selected
      for (Annotation word : graph.list("word"))
      {
         graph.addTag(word, "orthography", word.getLabel()+"-orthography");
      }
      
      // create serializer
      LatexSerializer serializer = new LatexSerializer();
      
      // general configuration
      ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(6, serializer.configure(configuration, schema).size());
      assertEquals("orthography", "orthography", 
		   ((Layer)configuration.get("orthographyLayer").getValue()).getId());
      assertEquals("noise", "noise", 
		   ((Layer)configuration.get("noiseLayer").getValue()).getId());
      assertEquals("texPreamble",
                   "", configuration.get("texPreamble").getValue());
      assertEquals("texBeginTranscript",
                   "\\begin{description}", configuration.get("texBeginTranscript").getValue());
      assertEquals("texTurnCommand",
                   "\\item[#1:] #2", configuration.get("texTurnCommand").getValue());
      assertEquals("texEndTranscript",
                   "\\end{description}", configuration.get("texEndTranscript").getValue());

      String[] needLayers = serializer.getRequiredLayers();
      assertEquals(4, needLayers.length);
      assertEquals("who", needLayers[0]);
      assertEquals("turn", needLayers[1]);
      assertEquals("utterance", needLayers[2]);
      assertEquals("word", needLayers[3]);
	 
      // serialize
      final Vector<SerializationException> exceptions = new Vector<SerializationException>();
      final Vector<NamedStream> streams = new Vector<NamedStream>();
      String[] layers = {"word"}; 
      serializer.serialize(Utility.OneGraphSpliterator(graph), layers,
                           stream -> streams.add(stream),
                           warning -> System.out.println(warning),
                           exception -> exceptions.add(exception));
      if (exceptions.size() > 0) fail(""+exceptions);

      streams.elementAt(0).save(dir);

      // test using diff
      File result = new File(dir, graph.getId() + ".tex");
      String differences = diff(new File(dir, "expected_" + graph.getId() + ".tex"), result);
      if (differences != null)
      {
         fail(differences);
      }
      else
      {
         result.delete();
      }
   }

   @Test public void serializeWithTags()
      throws Exception
   {
      Schema schema = new Schema(
         "who", "turn", "utterance", "word",
         new Layer("who", "Participants")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(true)
         .setPeersOverlap(true)
         .setSaturated(true),
         new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
         new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
         new Layer("word", "Words", 2, true, false, false, "turn", true),
         new Layer("orthography", "Orthography")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true)
         .setParentId("word"),
         new Layer("pos", "Part of Speech")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false)
         .setPeersOverlap(false)
         .setSaturated(true)
         .setParentId("word"));
      File dir = getDir();
      
      Graph graph = new Graph()
         .setId("tagged_sentence")
         .setSchema(schema);
      graph.addAnchor(new Anchor("a0", 0.0));
      graph.addAnchor(new Anchor("a5", 5.0));
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
                                         graph.addAnchor(new Anchor("a1", 1.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("quick", "'quick", "word", 
                                         "a1",
                                         graph.addAnchor(new Anchor("a2", 2.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("brown", "brown'", "word", 
                                         "a2",
                                         graph.addAnchor(new Anchor("a3", 3.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("fox", "fox", "word", 
                                         "a3",
                                         graph.addAnchor(new Anchor("a4", 4.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("jumps", "jumps -", "word", 
                                         "a4",
                                         "a5",
                                         "t1"));
      
      graph.addAnnotation(new Annotation("over", "over", "word",
                                         "a5",
                                         graph.addAnchor(new Anchor("a6", 6.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("the2", "the", "word", 
                                         "a6",
                                         graph.addAnchor(new Anchor("a7", 7.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("lazy", "lazy", "word", 
                                         "a7",
                                         graph.addAnchor(new Anchor("a8", 8.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation("dog", "\"dog\"", "word", 
                                         "a8",
                                         graph.addAnchor(new Anchor("a9", 9.0)).getId(),
                                         "t1"));
      graph.addAnnotation(new Annotation(".", ".", "word", 
                                         "a9",
                                         "a10",
                                         "t1"));
      graph.addTag(graph.getAnnotation("the"), "orthography", "the");
      graph.addTag(graph.getAnnotation("quick"), "orthography", "quick");
      graph.addTag(graph.getAnnotation("brown"), "orthography", "brown");
      graph.addTag(graph.getAnnotation("fox"), "orthography", "fox");
      graph.addTag(graph.getAnnotation("jumps"), "orthography", "jumps");
      graph.addTag(graph.getAnnotation("over"), "orthography", "over");
      graph.addTag(graph.getAnnotation("the2"), "orthography", "the");
      graph.addTag(graph.getAnnotation("lazy"), "orthography", "lazy");
      graph.addTag(graph.getAnnotation("dog"), "orthography", "dog");

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

      // create serializer
      LatexSerializer serializer = new LatexSerializer();
      
      // general configuration
      ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
      assertEquals(6, serializer.configure(configuration, schema).size());
      assertEquals("orthography", "orthography", 
		   ((Layer)configuration.get("orthographyLayer").getValue()).getId());
      assertNull("no noise", 
                 configuration.get("noiseLayer").getValue());
      assertEquals("texPreamble",
                   "", configuration.get("texPreamble").getValue());
      assertEquals("texBeginTranscript",
                   "\\begin{description}", configuration.get("texBeginTranscript").getValue());
      assertEquals("texTurnCommand",
                   "\\item[#1:] #2", configuration.get("texTurnCommand").getValue());
      assertEquals("texEndTranscript",
                   "\\end{description}", configuration.get("texEndTranscript").getValue());

      String[] needLayers = serializer.getRequiredLayers();
      assertEquals(4, needLayers.length);
      assertEquals("who", needLayers[0]);
      assertEquals("turn", needLayers[1]);
      assertEquals("utterance", needLayers[2]);
      assertEquals("word", needLayers[3]);
	 
      // serialize
      final Vector<SerializationException> exceptions = new Vector<SerializationException>();
      final Vector<NamedStream> streams = new Vector<NamedStream>();
      String[] layers = {"word", "pos", "orthography"}; 
      serializer.serialize(Utility.OneGraphSpliterator(graph), layers,
                           stream -> streams.add(stream),
                           warning -> System.out.println(warning),
                           exception -> exceptions.add(exception));
      if (exceptions.size() > 0) fail(""+exceptions);

      streams.elementAt(0).save(dir);

      // test using diff
      File result = new File(dir, graph.getId() + ".tex");
      String differences = diff(new File(dir, "expected_" + graph.getId() + ".tex"), result);
      if (differences != null)
      {
         fail(differences);
      }
      else
      {
         result.delete();
      }
   }

   /**
    * Diffs two files.
    * @param expected
    * @param actual
    * @return null if the files are the same, and a String describing differences if not.
    */
   public String diff(File expected, File actual)
   {
      StringBuffer d = new StringBuffer();
      
      try
      {
         // compare with what we expected
         Vector<String> actualLines = new Vector<String>();
         BufferedReader reader = new BufferedReader(new FileReader(actual));
         String line = reader.readLine();
         while (line != null)
         {
            actualLines.add(line);
            line = reader.readLine();
         }
         Vector<String> expectedLines = new Vector<String>();
         reader = new BufferedReader(new FileReader(expected));
         line = reader.readLine();
         while (line != null)
         {
            expectedLines.add(line);
            line = reader.readLine();
         }
         MinimumEditPath<String> comparator = new MinimumEditPath<String>();
         List<EditStep<String>> path = comparator.minimumEditPath(expectedLines, actualLines);
         for (EditStep<String> step : path)
         {
            switch (step.getOperation())
            {
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
      }
      catch(Exception exception)
      {
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
   public File getDir() 
   { 
      if (fDir == null)
      {
	 try
	 {
	    URL urlThisClass = getClass().getResource(getClass().getSimpleName() + ".class");
	    File fThisClass = new File(urlThisClass.toURI());
	    fDir = fThisClass.getParentFile();
	 }
	 catch(Throwable t)
	 {
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

   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.text.test.TestLatexSerializer");
   }
}
