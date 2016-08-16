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

package nzilbb.ag.util.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Vector;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.List;
import nzilbb.configure.ParameterSet;
import nzilbb.configure.Parameter;
import nzilbb.ag.util.*;
import nzilbb.ag.serialize.SerializationException;
import nzilbb.ag.serialize.SerializerNotConfiguredException;
import nzilbb.ag.serialize.SerializationParametersMissingException;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.serialize.json.*;
import nzilbb.ag.*;
import nzilbb.editpath.MinimumEditPath;
import nzilbb.editpath.EditStep;

public class TestMerger
{      
   @Test public void identityMerge() 
      throws Exception
   {
      Schema schema = defaultSchema();

      File f = new File(getDir(), "identity.json");
      
      Graph originalGraph = loadGraphFromJSON(f, schema);

      Merger m = new Merger();

      try
      {
	 Vector<Change> changes = m.transform(originalGraph);
	 fail("Doesn't throw exception when editedGraph is unset: " + changes);
      }
      catch(TransformationException exception)
      {  // TransformationException should be thrown
      }
      
      m.setEditedGraph(loadGraphFromJSON(f, schema));
      //m.setDebug(true);
      m.setValidator(null);

      try
      {
	 Vector<Change> changes = m.transform(originalGraph);
	 if (m.getLog() != null) for (String message : m.getLog()) System.out.println(message);
	 assertEquals("No changes - " + changes, 0, changes.size());
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }

   }

   @Test public void identityMergeFragment() 
      throws Exception
   {
      Schema schema = defaultSchema();

      File f = new File(getDir(), "identity__10.000-70.000.json");
      
      Merger m = new Merger(loadGraphFromJSON(f, schema));
      //m.setDebug(true);
      m.setValidator(null);

      Graph originalGraph = loadGraphFromJSON(f, schema);

      try
      {
	 Vector<Change> changes = m.transform(originalGraph);
	 if (m.getLog() != null) for (String message : m.getLog()) System.out.println(message);
	 assertEquals("No changes - " + changes, 0, changes.size());
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }

   }

   /**
    * Basic edit operations that (mostly) affect a single layer.
    * <ol>
    *  <li>Label changes, both below and above original status</li>
    *  <li>Annotation insertion</li>
    *  <li>Annotation deletion</li>
    *  <li>Anchor offset changes, both below and above original status</li>
    *  <li>Connected graphs that become disconnected, on the same layer</li>
    *  <li>Disconnected graphs that become connected, on the same layer</li>
    *  <li>Annotation transposition</li>
    *  <li>Merge graphs with no defining annotation</li>
    *  <li>Merge graphs with partial hierarchy - e.g. an utterance - and check for correct ordinals</li>
    * </ol>
    */
   @Test public void fragBasic()
   {
      fragmentTests("frag", "Basic", false);
   }

   /**
    * Standardised method for running graph fragment tests based on files in the test directory.
    * @param sPrefix The filename 'prefix' to use - not literally a prefix, as the test number appears
    *                before it in the file name.
    */ 
   public void fragmentTests(String sDir, final String sPrefix, boolean bVerbosity)
   {
      // get a sorted list of tests
      File dir = getDir();
      File subdir = new File(dir, sDir);
      File[] afTests = subdir.listFiles(new FilenameFilter()
	 {
	    public boolean accept(File dir, String name)
	    {
	       return name.matches("^\\d+-"+sPrefix+".*\\.json$");
	    }
	 });
      TreeSet<String> fragments = new TreeSet<String>();
      for (File fTest : afTests) fragments.add(fTest.getName().replace(".json",""));
      // run the tests
      for (String fragmentName : fragments)
      {
	 System.out.println("Test: " + fragmentName);
	 Schema schema = defaultSchema();
	 File fOriginal = new File(subdir, fragmentName + ".json");
	 try
	 {
	    Graph originalGraph = loadGraphFromJSON(fOriginal, schema);
	    File fEdited = new File(subdir, "edited_" + fragmentName + ".json");
	    Graph editedGraph = loadGraphFromJSON(fEdited, schema);
	    Merger m = new Merger(editedGraph);
	    m.setDebug(bVerbosity);
	    try
	    {
	       Vector<Change> changes = m.transform(originalGraph);
	    }
	    catch(TransformationException exception)
	    {
	       StringWriter sw = new StringWriter();
	       PrintWriter pw = new PrintWriter(sw);
	       exception.printStackTrace(pw);
	       try { sw.close(); }
	       catch(IOException x) {}
	       pw.close();	
	       fail(fragmentName + ": merge() failed" + exception.toString() + "\n" + sw);
	    }
	    if (m.getLog() != null) for (String message : m.getLog()) System.out.println(message);

	    // save the actual result
	    File fActual = new File(subdir, "actual_" + fragmentName + ".json");
	    saveGraphToJSON(fActual, originalGraph);
	    
	    // compare with what we expected
	    Vector<String> actualLines = new Vector<String>();
	    BufferedReader reader = new BufferedReader(new FileReader(fActual));
	    String line = reader.readLine();
	    while (line != null)
	    {
	       actualLines.add(line);
	       line = reader.readLine();
	    }
	    File fExpected = new File(subdir, "expected_" + fragmentName + ".json");
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
	 }
	 catch(Exception exception)
	 {
	    StringWriter sw = new StringWriter();
	    PrintWriter pw = new PrintWriter(sw);
	    exception.printStackTrace(pw);
	    try { sw.close(); }
	    catch(IOException x) {}
	    pw.close();	
	    fail(fragmentName + ": failed" + exception.toString() + "\n" + sw);
	 }
      } // next test
   }
   
   /**
    * Returns the default schema for testing.
    * @return A test schema that matches the test files.
    */
   public Schema defaultSchema()
   {
      return new Schema(
	 "who", "turn", "utterance", "word",
	 new Layer("topic", "topic", Constants.ALIGNMENT_INTERVAL, 
		   true, // peers
		   false, // peersOverlap
		   false), // saturated
	 new Layer("who", "participants", Constants.ALIGNMENT_NONE, 
		   true, // peers
		   true, // peersOverlap
		   true), // saturated
	 new Layer("turns", "turns", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   false, // saturated
		   "who", // parentId
		   true), // parentIncludes
	 new Layer("utterances", "utterances", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   true, // saturated
		   "turn", // parentId
		   true), // parentIncludes
	 new Layer("language", "language", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   false, // saturated
		   "turn", // parentId
		   true), // parentIncludes
	 new Layer("transcript", "Words", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   false, // saturated
		   "turn", // parentId
		   true), // parentIncludes
	 new Layer("pos", "Part of speech", Constants.ALIGNMENT_NONE,
		   false, // peers
		   false, // peersOverlap
		   true, // saturated
		   "word", // parentId
		   true), // parentIncludes
	 new Layer("segments", "segments", Constants.ALIGNMENT_INTERVAL,
		   true, // peers
		   false, // peersOverlap
		   true, // saturated
		   "word", // parentId
		   true) // parentIncludes
	 );      
   } // end of defaultSchema()


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

   
   /**
    * Loads an annotation graph from a JSON file.
    * @param file The JSON file.
    * @param schema The schema to use.
    * @return The annotation graph represented by the file.
    * @throws SerializationException If the graph could not be loaded.
    * @throws SerializationParametersMissingException
    * @throws SerializerNotConfiguredException
    * @throws IOException On IO error.
    * @throws FileNotFoundException If <var>file</var> doesn't exist.
    */
   public Graph loadGraphFromJSON(File file, Schema schema)
      throws FileNotFoundException, IOException, SerializationException, SerializationParametersMissingException, SerializerNotConfiguredException
   {
      // create deserializer
      JSONSerialization s = new JSONSerialization();
      // configure it with its default options
      s.configure(s.configure(new ParameterSet(), schema), schema);      
      // load file
      ParameterSet parameters = s.load(Utility.OneNamedStreamArray(new NamedStream(file)), schema);
      // use default deserialization parameters
      s.setParameters(parameters); // run with default values
      // deserialize      
      return s.deserialize()[0];
   } // end of loadGraphFromJSON()

   /**
    * Loads an annotation graph from a JSON file.
    * @param file The JSON file.
    * @param graph The graph to save.
    * @return The annotation graph represented by the file.
    * @throws SerializationException If the graph could not be loaded.
    * @throws SerializerNotConfiguredException
    * @throws IOException On IO error.
    */
   public void saveGraphToJSON(File file, Graph graph)
      throws IOException, SerializationException, SerializerNotConfiguredException
   {
      // create deserializer
      JSONSerialization s = new JSONSerialization();
      // configure it with its default options
      s.configure(s.configure(new ParameterSet(), graph.getSchema()), graph.getSchema());
      // serialize      
      NamedStream[] streams = s.serialize(Utility.OneGraphArray(graph));
      streams[0].setName(file.getName());
      streams[0].save(file.getParentFile());
   } // end of loadGraphFromJSON()
   


   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.util.test.TestMerger");
   }
}
