//
// Copyright 2016 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.csv;

import java.io.File;
import nzilbb.configure.ParameterSet;
import nzilbb.configure.Parameter;
import nzilbb.ag.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.serialize.json.*;
import nzilbb.util.CommandLineProgram;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;

/**
 * Utility for converting legacy CSV annotation graph testing files to JSON.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Utility for converting legacy CSV annotation graph testing files to JSON",arguments="graph1.csv [graph2.csv [...]]")
public class CsvToJson extends CommandLineProgram
{
   public static void main(String argv[])
   {
      CsvToJson application = new CsvToJson();
      if (argv.length == 0) application.setUsage(true);
      if (application.processArguments(argv)) application.start();
   }

   public void start()
   {
      Schema schema = new Schema(
	 "who", "turns", "utterances", "transcript",
	 new Layer("who", "participants", 0, true, true, true),
	 new Layer("turns", "turns", 2, true, false, false, "who", true),
	 new Layer("utterances", "utterances", 2, true, false, true, "turns", true),
	 new Layer("language", "Language", 2, true, false, true, "turns", true),
	 new Layer("transcript", "transcript", 2, true, false, false, "turns", true),
	 new Layer("lexical", "lexical", 0, false, false, false, "transcript", true),
	 new Layer("pronounce", "pronounce", 0, false, false, false, "transcript", true),
	 new Layer("segments", "segments", 2, true, false, false, "transcript", true)
	 );      
      
      try
      {
	 // create deserializer
	 AgCsvDeserializer fromCsv = new AgCsvDeserializer();
	 
	 // configure it
	 ParameterSet defaultConfiguration = fromCsv.configure(new ParameterSet(), schema);
	 fromCsv.configure(defaultConfiguration, schema);
	    
	 // create serializer
	 JSONSerialization toJson = new JSONSerialization();
	 // configure it
	 toJson.configure(toJson.configure(new ParameterSet(), schema), schema);	    
	 
	 for (String argument: arguments)
	 {
	    // access file
	    File f = new File(argument).getAbsoluteFile();
	    if (!f.exists())
	    {
	       System.err.println(argument + " not found.");
	       continue;
	    }
	    System.out.println("Convert: " + argument);
	    NamedStream[] streams = { new NamedStream(f) };
	    
	    try
	    {
	       // load the stream
	       ParameterSet defaultParameters = fromCsv.load(streams, null, schema);
	       // configure the deserialization
	       fromCsv.setParameters(defaultParameters);
	       
	       // build the graph
	       Graph[] graphs = fromCsv.deserialize();
	       Graph g = graphs[0];	    
	       for (String warning : fromCsv.getWarnings())
	       {
		  System.out.println(warning);
	       }
	       
	       // convert to JSON
	       streams = toJson.serialize(Utility.OneGraphArray(g));
	       streams[0].save(f.getParentFile());	    
	    }
	    catch(Exception exception)
	    {
	       System.err.println("Error processing " + argument + ": " + exception.toString());
	       exception.printStackTrace(System.err);
	    }

	 } // next argument
      }
      catch(Exception exception)
      {
	 System.err.println(exception.toString());
	 exception.printStackTrace(System.err);
      }
   }
} // end of class CsvToJson
