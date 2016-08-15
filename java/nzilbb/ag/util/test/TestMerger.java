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
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.Iterator;
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
      m.setDebug(true);
      m.setValidator(null);

      try
      {
	 Vector<Change> changes = m.transform(originalGraph);
	 assertEquals("No changes - " + changes, 0, changes.size());
	 if (m.getLog() != null) for (String message : m.getLog()) System.out.println(message);
      }
      catch(TransformationException exception)
      {
	 fail(exception.toString());
      }

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
   


   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.util.test.TestMerger");
   }
}
