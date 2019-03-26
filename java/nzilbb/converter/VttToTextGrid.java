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
package nzilbb.converter;

import java.io.File;
import nzilbb.util.IO;
import nzilbb.util.CommandLineProgram;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;
import nzilbb.configure.ParameterSet;

import nzilbb.ag.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.webvtt.VttDeserializer;
import nzilbb.praat.TextGridSerialization;

/**
 * Converts WebVTT subtitle files to Praat TextGrids.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Converts WebVTT subtitle files to Praat TextGrids",arguments="file1.vtt file2.vtt ...")
public class VttToTextGrid
   extends CommandLineProgram
{
   // Attributes:
   
   /**
    * Whether detailed verbose output is printed or not.
    * @see #getVerbose()
    * @see #setVerbose(Boolean)
    */
   protected Boolean verbose = Boolean.FALSE;
   /**
    * Getter for {@link #verbose}: Whether detailed verbose output is printed or not.
    * @return Whether detailed verbose output is printed or not.
    */
   public Boolean getVerbose() { return verbose; }
   /**
    * Setter for {@link #verbose}: Whether detailed verbose output is printed or not.
    * @param newVerbose Whether detailed verbose output is printed or not.
    */
   @Switch(value="Whether detailed verbose output is printed or not",compulsory=false)
   public void setVerbose(Boolean newVerbose) { verbose = newVerbose; }

   
   // Methods:
   
   /**
    * Default constructor.
    */
   public VttToTextGrid()
   {
   } // end of constructor
   
   public static void main(String argv[])
   {
      VttToTextGrid application = new VttToTextGrid();
      if (application.processArguments(argv))
      {
	 application.start();
      }
   }

   public void start()
   {
      if (arguments.size() == 0)
      {
	 System.err.println("No input files speecified, nothing to do. Try using --usage command line switch.");
      }
      for (String sArgument: arguments)
      {
	 try
	 {
	    File inputFile = new File(sArgument);
	    if (!inputFile.exists())
	    {
	       System.err.println("Input file doesn't exist: " + sArgument);
	    }
	    else
	    {
	       convert(inputFile);
	    }
	 }
	 catch(Exception exception)
	 {
	    System.err.println("Error processing: " + sArgument + " : " + exception.getMessage());
	    exception.printStackTrace(System.err);
	 }
      } // next argument
   }
   
   /**
    * Converts a file.
    * @param inputFile
    * @param outputFile
    * @throws Exception
    */
   public void convert(File inputFile)
      throws Exception
   {
      if (verbose) System.out.println("Converting " + inputFile.getPath());
      
      Schema schema = new Schema("who", "turn", "utterance", null,
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true));

      // deserialize...
      
      NamedStream[] streams = { new NamedStream(inputFile) };
      
      // create deserializer
      VttDeserializer deserializer = new VttDeserializer();
      if (verbose) System.out.println("Deserializing with " + deserializer.getDescriptor());

      // configure deserializer
      ParameterSet defaultConfig = deserializer.configure(new ParameterSet(), schema);
      deserializer.configure(defaultConfig, schema);

      // load the stream
      ParameterSet defaultParameters = deserializer.load(streams, schema);
      
      // configure the deserialization
      deserializer.setParameters(defaultParameters);
      
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];     
      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      // serialize...

      // create serializer
      TextGridSerialization serializer = new TextGridSerialization();
      if (verbose) System.out.println("Serializing with " + serializer.getDescriptor());
      
      // configure serializer
      ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
      serializer.configure(configuration, schema);

      // serialize
      NamedStream[] outputStreams = serializer.serialize(graphs);
      for (NamedStream stream : outputStreams)
      {
	 stream.save(inputFile.getParentFile());
      }
      
      if (verbose) System.out.println("Finished " + inputFile.getPath());
   } // end of convert()

   
} // end of class VttToTextGrid
