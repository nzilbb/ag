//
// Copyright 2015-2016 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag.serialize.json.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.net.URL;
import nzilbb.configure.ParameterSet;
import nzilbb.configure.Parameter;
import nzilbb.ag.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.serialize.json.*;
import nzilbb.editpath.*;

public class TestJSONSerialization
{
   @Test public void minimalSerializationDeserialization() 
      throws Exception
   {
      Graph g = new Graph();
      g.setId("test");

      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("topic", "Topics", Constants.ALIGNMENT_INTERVAL, 
		   true, // peers
		   false, // peersOverlap
		   false), // saturated
	 new Layer("who", "Participants", Constants.ALIGNMENT_NONE, 
		   true, // peers
		   true, // peersOverlap
		   true), // saturated
	 new Layer("turn", "Speaker turns", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   false, // saturated
		   "who", // parentId
		   true), // parentIncludes
	 new Layer("utterance", "Utterances", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   true, // saturated
		   "turn", // parentId
		   true), // parentIncludes
	 new Layer("phrase", "Phrase", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   false, // saturated
		   "turn", // parentId
		   true), // parentIncludes
	 new Layer("word", "Words", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   false, // saturated
		   "turn", // parentId
		   true), // parentIncludes
	 new Layer("pos", "Part of speec", Constants.ALIGNMENT_NONE,
		   false, // peers
		   false, // peersOverlap
		   true, // saturated
		   "word", // parentId
		   true), // parentIncludes
	 new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   true, // saturated
		   "word", // parentId
		   true) // parentIncludes
	 );      
      g.setSchema(schema);

      g.setOffsetGranularity(0.001);

      g.addAnchor(new Anchor("turnStart", 0.0, Constants.CONFIDENCE_MANUAL));
      g.addAnchor(new Anchor("a1", 1.0, Constants.CONFIDENCE_AUTOMATIC));
      g.addAnchor(new Anchor("a1.5", 1.5, Constants.CONFIDENCE_AUTOMATIC));
      g.addAnchor(new Anchor("a2", 2.0, Constants.CONFIDENCE_AUTOMATIC));
      g.addAnchor(new Anchor("a2.25", 2.25, Constants.CONFIDENCE_AUTOMATIC));
      g.addAnchor(new Anchor("a2.5", 2.5, Constants.CONFIDENCE_AUTOMATIC));
      g.addAnchor(new Anchor("a2.75", 2.75, Constants.CONFIDENCE_AUTOMATIC));
      g.addAnchor(new Anchor("a3", 3.0, Constants.CONFIDENCE_AUTOMATIC));
      g.addAnchor(new Anchor("a4", 4.0, Constants.CONFIDENCE_AUTOMATIC));
      // null offset handled
      g.addAnchor(new Anchor("a5", null));
      g.addAnchor(new Anchor("turnEnd", 6.0, Constants.CONFIDENCE_MANUAL));

      Annotation who1 = new Annotation("who1", "john smith", "who", "turnStart", "turnEnd", "test");
      who1.setConfidence(Constants.CONFIDENCE_MANUAL);
      Annotation turn1 = new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "who1");
      turn1.setConfidence(Constants.CONFIDENCE_MANUAL);
      Annotation line1 = new Annotation("line1", "john smith", "utterance", "turnStart", "turnEnd", "turn1");
      line1.setConfidence(Constants.CONFIDENCE_MANUAL);

      Annotation the = new Annotation("word1", "the", "word", "a1", "a2", "turn1");
      the.setConfidence(Constants.CONFIDENCE_MANUAL);
      Annotation DT = new Annotation("pos1", "DT", "pos", "a1", "a2", "word1");
      DT.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
      Annotation th = new Annotation("phone1", "D", "phone", "a1", "a1.5", "word1");
      th.setConfidence(Constants.CONFIDENCE_MANUAL);
      Annotation e = new Annotation("phone2", "@", "phone", "a1.5", "a2", "word1");
      e.setConfidence(Constants.CONFIDENCE_MANUAL);
      Annotation quick = new Annotation("word2", "quick", "word", "a2", "a3", "turn1");
      quick.setConfidence(Constants.CONFIDENCE_MANUAL);
      Annotation A = new Annotation("pos2", "A", "pos", "a2", "a3", "word2");
      A.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
      Annotation k = new Annotation("phone3", "k", "phone", "a2", "a2.25", "word2");
      k.setConfidence(Constants.CONFIDENCE_MANUAL);
      Annotation w = new Annotation("phone4", "w", "phone", "a2.25", "a2.5", "word2");
      w.setConfidence(Constants.CONFIDENCE_MANUAL);
      Annotation I = new Annotation("phone5", "I", "phone", "a2.5", "a2.75", "word2");
      I.setConfidence(Constants.CONFIDENCE_MANUAL);
      Annotation ck = new Annotation("phone6", "k", "phone", "a2.75", "a3", "word2");
      ck.setConfidence(Constants.CONFIDENCE_MANUAL);
      Annotation brown = new Annotation("word3", "brown", "word", "a3", "a4", "turn1");
      brown.setConfidence(Constants.CONFIDENCE_MANUAL);
      Annotation AP = new Annotation("phrase1", "AP", "phrase", "a2", "a4", "turn1");
      AP.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
      Annotation fox = new Annotation("word4", "fox", "word", "a4", "a5", "turn1");
      fox.setConfidence(Constants.CONFIDENCE_MANUAL);
      Annotation N = new Annotation("pos3", "N", "pos", "a4", "a5", "word4");
      N.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
      Annotation NP = new Annotation("phrase2", "NP", "phrase", "a1", "a5", "turn1");
      NP.setConfidence(Constants.CONFIDENCE_AUTOMATIC);

      g.addAnnotation(who1);
      g.addAnnotation(turn1);
      g.addAnnotation(line1);
      
      g.addAnnotation(the);
      g.addAnnotation(quick);
      g.addAnnotation(brown);
      g.addAnnotation(fox);
      
      g.addAnnotation(th);
      g.addAnnotation(e);
      g.addAnnotation(k);
      g.addAnnotation(w);
      g.addAnnotation(I);
      g.addAnnotation(ck);

      g.addAnnotation(DT);
      g.addAnnotation(A);
      g.addAnnotation(N);

      g.addAnnotation(NP);
      g.addAnnotation(AP);

      // add some comments
      the.put("comment", "word comment");
      the.getStart().put("comment", "anchor comment");
      
      // create deserializer
      JSONSerialization s = new JSONSerialization();

      // double-call to configure is sufficient
      assertEquals(0, s.configure(s.configure(new ParameterSet(), schema), schema).size());

      // no particular layers required
      assertEquals(0, s.getRequiredLayers().length);

      // serialize
      File dir = getDir();
      NamedStream[] streams = s.serialize(Utility.OneGraphArray(g));
      assertEquals(1, streams.length);
      streams[0].save(dir);

      // compare with what we expected
      File fActual = new File(dir, "test.json");
      Vector<String> actualLines = new Vector<String>();
      BufferedReader reader = new BufferedReader(new FileReader(fActual));
      String line = reader.readLine();
      while (line != null)
      {
	 actualLines.add(line);
	 line = reader.readLine();
      }
      File fExpected = new File(dir, "test_expected.json");
      Vector<String> expectedLines = new Vector<String>();
      reader = new BufferedReader(new FileReader(fExpected));
      line = reader.readLine();
      while (line != null)
      {
	 expectedLines.add(line);
	 line = reader.readLine();
      }
      MinimumEditPath<String> comparator = new MinimumEditPath<String>();
      List<EditStep<String>> path = comparator.minimumEditPath(expectedLines, actualLines);
      String differences = "";
      for (EditStep<String> step : path)
      {
	 switch (step.getOperation())
	 {
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
      if (differences.length() > 0) fail(differences);

      // now deserialization
      ParameterSet parameters = s.load(Utility.OneNamedStreamArray(new NamedStream(fActual)), schema);
      s.setParameters(parameters); // run with default values
      Graph[] graphs = s.deserialize();
      Graph d = graphs[0];

      assertEquals("Ensure deserialized graph has no changes", 
		   0, d.getChanges().size());

      // compare d with g
      
      // attributes
      assertEquals(g.getId(), d.getId());
      assertEquals(g.getOffsetGranularity(), d.getOffsetGranularity());

      // layers
      for (Layer gLayer : g.getSchema().getLayers().values())
      {
	 Layer dLayer = d.getLayer(gLayer.getId());
	 assertNotNull(gLayer.getId(), 
		       dLayer);
	 assertEquals(gLayer.getId(), 
		      gLayer.getId(), dLayer.getId());
	 assertEquals(gLayer.getId(), 
		      gLayer.getParentId(), dLayer.getParentId());
	 assertEquals(gLayer.getId(), 
		      gLayer.getDescription(), dLayer.getDescription());
	 assertEquals(gLayer.getId(), 
		      gLayer.getAlignment(), dLayer.getAlignment());
	 assertEquals(gLayer.getId(), 
		      gLayer.getPeers(), dLayer.getPeers());
	 assertEquals(gLayer.getId(), 
		      gLayer.getPeersOverlap(), dLayer.getPeersOverlap());
	 assertEquals(gLayer.getId(), 
		      gLayer.getParentIncludes(), dLayer.getParentIncludes());
	 assertEquals(gLayer.getId(), 
		      gLayer.getSaturated(), dLayer.getSaturated());
      } // next layer
      assertEquals("No extra layers: " + d.getSchema().getLayers().values(),
		   g.getSchema().getLayers().size(), d.getSchema().getLayers().size());

      // anchors
      for (Anchor gAnchor : g.getAnchors().values())
      {
	 Anchor dAnchor = d.getAnchor(gAnchor.getId());
	 assertNotNull(gAnchor.getId(), 
		       dAnchor);
	 assertEquals(gAnchor.getId(), 
		      gAnchor.getId(), dAnchor.getId());
	 assertEquals(gAnchor.getId(), 
		      gAnchor.getOffset(), dAnchor.getOffset());
	 assertEquals(gAnchor.getId(), 
		      gAnchor.getConfidence(), dAnchor.getConfidence());
	 assertEquals(gAnchor.getId(), 
		      gAnchor.get(Constants.COMMENT), dAnchor.get(Constants.COMMENT));
      } // next layer
      assertEquals("No extra anchors: " + d.getAnchors().values(), 
		   g.getAnchors().size(), d.getAnchors().size());

      // annotations
      for (Annotation gAnnotation : g.getAnnotationsById().values())
      {
	 Annotation dAnnotation = d.getAnnotation(gAnnotation.getId());
	 assertNotNull(gAnnotation.getId(), 
		       dAnnotation);
	 assertEquals(gAnnotation.getId(), 
		      gAnnotation.getId(), dAnnotation.getId());
	 assertEquals(gAnnotation.getId(), 
		      gAnnotation.getLabel(), dAnnotation.getLabel());
	 assertEquals(gAnnotation.getId(), 
		      gAnnotation.getStartId(), dAnnotation.getStartId());
	 assertEquals(gAnnotation.getId(), 
		      gAnnotation.getEndId(), dAnnotation.getEndId());
	 assertEquals(gAnnotation.getId(), 
		      gAnnotation.getConfidence(), dAnnotation.getConfidence());
	 assertEquals(gAnnotation.getId(), 
		      gAnnotation.get(Constants.COMMENT), dAnnotation.get(Constants.COMMENT));

	 assertEquals(gAnnotation.getId(), 
		      gAnnotation.getParentId(), dAnnotation.getParentId());
      } // next layer
      assertEquals("No extra annotations: " + d.getAnnotationsById().values(),
		   g.getAnnotationsById().size(), d.getAnnotationsById().size());

   }

   @Test public void childOrder() 
      throws Exception
   {
      Graph g = new Graph();
      g.setId("test");
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("topic", "Topics", Constants.ALIGNMENT_INTERVAL, 
		   true, // peers
		   false, // peersOverlap
		   false), // saturated
	 new Layer("who", "Participants", Constants.ALIGNMENT_NONE, 
		   true, // peers
		   true, // peersOverlap
		   true), // saturated
	 new Layer("turn", "Speaker turns", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   false, // saturated
		   "who", // parentId
		   true), // parentIncludes
	 new Layer("utterance", "Utterances", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   true, // saturated
		   "turn", // parentId
		   true), // parentIncludes
	 new Layer("phrase", "Phrase", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   false, // saturated
		   "turn", // parentId
		   true), // parentIncludes
	 new Layer("word", "Words", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   false, // saturated
		   "turn", // parentId
		   true), // parentIncludes
	 new Layer("pos", "Part of speec", Constants.ALIGNMENT_NONE,
		   false, // peers
		   false, // peersOverlap
		   true, // saturated
		   "word", // parentId
		   true), // parentIncludes
	 new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   true, // saturated
		   "word", // parentId
		   true) // parentIncludes
	 );      

      File dir = getDir();

      // graph with shuffled layers
      File fShuffled = new File(dir, "test_shuffled.json");

      // create deserializer
      JSONSerialization s = new JSONSerialization();
      s.setSortAnchors(true);

      // double-call to configure is sufficient
      assertEquals(0, s.configure(s.configure(new ParameterSet(), schema), schema).size());

      // deserialize
      ParameterSet parameters = s.load(Utility.OneNamedStreamArray(new NamedStream(fShuffled)), schema);
      s.setParameters(parameters); // run with default values
      Graph[] graphs = s.deserialize();
      Graph d = graphs[0];

      NamedStream[] streams = s.serialize(Utility.OneGraphArray(d));
      File fCorrected = new File(dir, "test_corrected.json");
      assertEquals(1, streams.length);
      streams[0].setName(fCorrected.getName());
      streams[0].save(dir);

      // compare corrected with expected
      File fExpected = new File(dir, "test_expected.json");

      Vector<String> actualLines = new Vector<String>();
      BufferedReader reader = new BufferedReader(new FileReader(fCorrected));
      String line = reader.readLine();
      while (line != null)
      {
	 actualLines.add(line);
	 line = reader.readLine();
      }
      Vector<String> expectedLines = new Vector<String>();
      reader = new BufferedReader(new FileReader(fExpected));
      line = reader.readLine();
      while (line != null)
      {
	 expectedLines.add(line);
	 line = reader.readLine();
      }
      MinimumEditPath<String> comparator = new MinimumEditPath<String>();
      List<EditStep<String>> path = comparator.minimumEditPath(expectedLines, actualLines);
      String differences = "";
      for (EditStep<String> step : path)
      {
	 switch (step.getOperation())
	 {
	    case CHANGE:
	       differences += "\n"+fExpected.getPath()+":"+(step.getFromIndex()+1)+": Expected:\n" 
		  + step.getFrom() 
		  + "\n"+fCorrected.getPath()+":"+(step.getToIndex()+1)+": Found:\n" + step.getTo();
	       break;
	    case DELETE:
	       differences += "\n"+fExpected.getPath()+":"+(step.getFromIndex()+1)+": Deleted:\n" 
		  + step.getFrom();
	       break;
	    case INSERT:
	       differences += "\n"+fCorrected.getPath()+":"+(step.getToIndex()+1)+": Inserted:\n" 
		  + step.getTo();
	       break;
	 }
      } // next step
      if (differences.length() > 0) fail(differences);
      
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
      org.junit.runner.JUnitCore.main("nzilbb.ag.serialize.json.test.TestJSONSerialization");
   }
}
