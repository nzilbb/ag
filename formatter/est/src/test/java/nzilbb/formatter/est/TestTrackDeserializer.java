//
// Copyright 2017 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.formatter.est;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.SortedSet;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.io.File;
import java.net.URL;
import nzilbb.configure.ParameterSet;
import nzilbb.configure.Parameter;
import nzilbb.ag.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.formatter.est.*;

public class TestTrackDeserializer {
   
   @Test public void f0ReaperFileExtension()  throws Exception {
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("topic", "Topics", 2, true, true, true),
	 new Layer("test", "Intervals", 2, true, true, true),
	 new Layer("f0", "Points", 1, true, true, true),
	 new Layer("points", "Other points", 1, true, true, true));
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "test.f0")) };
      
      // create deserializer
      TrackDeserializer deserializer = new TrackDeserializer();

      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(0, deserializer.configure(configuration, schema).size());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(1, defaultParamaters.size());
      assertEquals("labels mapping", "f0", 
		   ((Layer)defaultParamaters.get("labels").getValue()).getId());
      
      // configure the deserialization
      deserializer.setParameters(defaultParamaters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }

      assertEquals("test.f0", g.getId());
      
      // labels and times
      Annotation[] annotations = g.all("f0");

      assertEquals(Double.valueOf(1.41), annotations[0].getStart().getOffset());
      assertTrue(annotations[0].getInstantaneous());
      assertEquals("183.908051", annotations[0].getLabel());
      assertEquals(g, annotations[0].getParent());

      assertEquals(Double.valueOf(1.415), annotations[1].getStart().getOffset());
      assertTrue(annotations[1].getInstantaneous());
      assertEquals("177.777771", annotations[1].getLabel());
      assertEquals(g, annotations[1].getParent());

      assertEquals(Double.valueOf(1.42), annotations[2].getStart().getOffset());
      assertTrue(annotations[2].getInstantaneous());
      assertEquals("179.775284", annotations[2].getLabel());
      assertEquals(g, annotations[2].getParent());

      assertEquals(Double.valueOf(1.425), annotations[3].getStart().getOffset());
      assertTrue(annotations[3].getInstantaneous());
      assertEquals("179.775284", annotations[3].getLabel());
      assertEquals(g, annotations[3].getParent());

      assertEquals(Double.valueOf(300.559998), annotations[annotations.length-1].getStart().getOffset());
      assertTrue(annotations[annotations.length-1].getInstantaneous());
      assertEquals("205.128204", annotations[annotations.length-1].getLabel());
      assertEquals(g, annotations[annotations.length-1].getParent());
      
      assertEquals(39515, annotations.length);
      assertEquals(39515, g.getAnchors().size());
      
      // check all annotations have 'manual' confidence
      for (Annotation a : g.getAnnotationsById().values()) {
         assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

   }

   @Test public void f0ReaperFileName()  throws Exception {
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("topic", "Topics", 2, true, true, true),
	 new Layer("test", "Intervals", 1, true, true, true),
	 new Layer("f0", "Points", 2, true, true, true),
	 new Layer("points", "Other points", 1, true, true, true));
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "test.f0")) };
      
      // create deserializer
      TrackDeserializer deserializer = new TrackDeserializer();

      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(0, deserializer.configure(configuration, schema).size());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(1, defaultParamaters.size());
      assertEquals("labels mapping", "test", 
		   ((Layer)defaultParamaters.get("labels").getValue()).getId());
      
      // configure the deserialization
      deserializer.setParameters(defaultParamaters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }

      assertEquals("test.f0", g.getId());
      
      // labels and times
      Annotation[] annotations = g.all("test");

      assertEquals(Double.valueOf(1.41), annotations[0].getStart().getOffset());
      assertTrue(annotations[0].getInstantaneous());
      assertEquals("183.908051", annotations[0].getLabel());
      assertEquals(g, annotations[0].getParent());

      assertEquals(Double.valueOf(1.415), annotations[1].getStart().getOffset());
      assertTrue(annotations[1].getInstantaneous());
      assertEquals("177.777771", annotations[1].getLabel());
      assertEquals(g, annotations[1].getParent());

      assertEquals(Double.valueOf(1.42), annotations[2].getStart().getOffset());
      assertTrue(annotations[2].getInstantaneous());
      assertEquals("179.775284", annotations[2].getLabel());
      assertEquals(g, annotations[2].getParent());

      assertEquals(Double.valueOf(1.425), annotations[3].getStart().getOffset());
      assertTrue(annotations[3].getInstantaneous());
      assertEquals("179.775284", annotations[3].getLabel());
      assertEquals(g, annotations[3].getParent());

      assertEquals(Double.valueOf(300.559998), annotations[annotations.length-1].getStart().getOffset());
      assertTrue(annotations[annotations.length-1].getInstantaneous());
      assertEquals("205.128204", annotations[annotations.length-1].getLabel());
      assertEquals(g, annotations[annotations.length-1].getParent());
      
      assertEquals(39515, annotations.length);
      assertEquals(39515, g.getAnchors().size());
      
      // check all annotations have 'manual' confidence
      for (Annotation a : g.getAnnotationsById().values()) {
         assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

   }

   @Test public void f0ReaperFilePreferNameoverExtension()  throws Exception {
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("topic", "Topics", 2, true, true, true),
	 new Layer("test", "Intervals", 1, true, true, true),
	 new Layer("f0", "Points", 1, true, true, true),
	 new Layer("points", "Other points", 1, true, true, true));
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "test.f0")) };
      
      // create deserializer
      TrackDeserializer deserializer = new TrackDeserializer();

      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(0, deserializer.configure(configuration, schema).size());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(1, defaultParamaters.size());
      assertEquals("labels mapping", "test", 
		   ((Layer)defaultParamaters.get("labels").getValue()).getId());
      
      // configure the deserialization
      deserializer.setParameters(defaultParamaters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }

      assertEquals("test.f0", g.getId());
      
      // labels and times
      Annotation[] annotations = g.all("test");

      assertEquals(Double.valueOf(1.41), annotations[0].getStart().getOffset());
      assertTrue(annotations[0].getInstantaneous());
      assertEquals("183.908051", annotations[0].getLabel());
      assertEquals(g, annotations[0].getParent());

      assertEquals(Double.valueOf(1.415), annotations[1].getStart().getOffset());
      assertTrue(annotations[1].getInstantaneous());
      assertEquals("177.777771", annotations[1].getLabel());
      assertEquals(g, annotations[1].getParent());

      assertEquals(Double.valueOf(1.42), annotations[2].getStart().getOffset());
      assertTrue(annotations[2].getInstantaneous());
      assertEquals("179.775284", annotations[2].getLabel());
      assertEquals(g, annotations[2].getParent());

      assertEquals(Double.valueOf(1.425), annotations[3].getStart().getOffset());
      assertTrue(annotations[3].getInstantaneous());
      assertEquals("179.775284", annotations[3].getLabel());
      assertEquals(g, annotations[3].getParent());

      assertEquals(Double.valueOf(300.559998), annotations[annotations.length-1].getStart().getOffset());
      assertTrue(annotations[annotations.length-1].getInstantaneous());
      assertEquals("205.128204", annotations[annotations.length-1].getLabel());
      assertEquals(g, annotations[annotations.length-1].getParent());
      
      assertEquals(39515, annotations.length);
      assertEquals(39515, g.getAnchors().size());
      
      // check all annotations have 'manual' confidence
      for (Annotation a : g.getAnnotationsById().values()) {
         assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
      }

   }

   @Test public void errorDetectionAndTolerance()  throws Exception {
      Schema schema = new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("f0", "Points", 1, true, true, true));
      // access file
      NamedStream[] streams = { new NamedStream(new File(getDir(), "errors.f0")) };
      
      // create deserializer
      TrackDeserializer deserializer = new TrackDeserializer();

      // general configuration
      ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
      // for (Parameter p : configuration.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(0, deserializer.configure(configuration, schema).size());

      // load the stream
      ParameterSet defaultParamaters = deserializer.load(streams, schema);
      // for (Parameter p : defaultParamaters.values()) System.out.println("" + p.getName() + " = " + p.getValue());
      assertEquals(1, defaultParamaters.size());
      assertEquals("labels mapping", "f0", 
		   ((Layer)defaultParamaters.get("labels").getValue()).getId());
      
      // configure the deserialization
      deserializer.setParameters(defaultParamaters);

      // build the graph
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];

      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }

      assertEquals("errors.f0", g.getId());
      
      // labels and times
      Annotation[] annotations = g.all("f0");

      assertEquals(Double.valueOf(1.41), annotations[0].getStart().getOffset());
      assertTrue(annotations[0].getInstantaneous());
      assertEquals("183.908051", annotations[0].getLabel());
      assertEquals(g, annotations[0].getParent());

      assertEquals(Double.valueOf(1.415), annotations[1].getStart().getOffset());
      assertTrue(annotations[1].getInstantaneous());
      assertEquals("177.777771", annotations[1].getLabel());
      assertEquals(g, annotations[1].getParent());

      assertEquals(Double.valueOf(1.42), annotations[2].getStart().getOffset());
      assertTrue(annotations[2].getInstantaneous());
      assertEquals("179.775284", annotations[2].getLabel());
      assertEquals(g, annotations[2].getParent());

      assertEquals(Double.valueOf(1.425), annotations[3].getStart().getOffset());
      assertTrue(annotations[3].getInstantaneous());
      assertEquals("179.775284", annotations[3].getLabel());
      assertEquals(g, annotations[3].getParent());

      assertEquals(Double.valueOf(300.559998), annotations[annotations.length-1].getStart().getOffset());
      assertTrue(annotations[annotations.length-1].getInstantaneous());
      assertEquals("205.128204", annotations[annotations.length-1].getLabel());
      assertEquals(g, annotations[annotations.length-1].getParent());
      
      assertEquals(39515, annotations.length);
      assertEquals(39515, g.getAnchors().size());
      
      // check all annotations have 'manual' confidence
      for (Annotation a : g.getAnnotationsById().values()) {
         assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                      Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
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
      org.junit.runner.JUnitCore.main("nzilbb.formatter.est.TestTrackDeserializer");
   }
}
