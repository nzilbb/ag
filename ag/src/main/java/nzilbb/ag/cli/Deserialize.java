//
// Copyright 2021 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag.cli;

import java.io.File;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.serialize.GraphDeserializer;
import nzilbb.ag.serialize.json.JSONSerialization;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.util.DefaultOffsetGenerator;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.util.CommandLineProgram;
import nzilbb.util.IO;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;

/**
 * Deserializes given files.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Converts a transcript to a JSON-encoded annotation graph",arguments="formatter.jar trascript-1 [transcript-2 ...]")
public class Deserialize extends CommandLineProgram {

  /**
   * Whether to set null anchor offsets or not.
   * @see #getDefaultOffsets()
   * @see #setDefaultOffsets(Boolean)
   */
  protected Boolean defaultOffsets = Boolean.TRUE;
  /**
   * Getter for {@link #defaultOffsets}: Whether to set null anchor offsets or not.
   * @return Whether to set null anchor offsets or not.
   */
  public Boolean getDefaultOffsets() { return defaultOffsets; }
  /**
   * Setter for {@link #defaultOffsets}: Whether to set null anchor offsets or not.
   * @param newDefaultOffsets Whether to set null anchor offsets or not.
   */
  @Switch("Set null anchor offsets")
  public Deserialize setDefaultOffsets(Boolean newDefaultOffsets) { defaultOffsets = newDefaultOffsets; return this; }

  /**
   * Print verbose output.
   * @see #getVerbose()
   * @see #setVerbose(Boolean)
   */
  protected Boolean verbose = Boolean.FALSE;
  /**
   * Getter for {@link #verbose}: Print verbose output.
   * @return Print verbose output.
   */
  public Boolean getVerbose() { return verbose; }
  /**
   * Setter for {@link #verbose}: Print verbose output.
   * @param newVerbose Print verbose output.
   */
  @Switch("Print verbose output")
  public Deserialize setVerbose(Boolean newVerbose) { verbose = newVerbose; return this; }
  
  public static void main(String argv[]) {
    Deserialize application = new Deserialize();
    if (application.processArguments(argv)) {
      application.start();
    }
  }
  
  public void start() {
    // start JSON array (because there may be more than one graph output)
    System.out.println("[");
    boolean firstGraph = true;
    
    GraphDeserializer deserializer = null;

    // if a deserializer uses this as a base class, it's the deserializer to use
    if (this instanceof GraphDeserializer) deserializer = (GraphDeserializer)this;
    
    for (String argument: arguments) {
      // argument is a file
      File file = new File(argument);
      if (verbose) System.err.println(file.getPath() + "...");

      if (deserializer == null) { // if the deserializer isn't known yet
        // first argument is the formatter jar
        try {
          deserializer = (GraphDeserializer)IO.FindImplementorInJar(
            file, this.getClass().getClassLoader(), GraphDeserializer.class);
          continue; // next file
        } catch(Exception exception) {
          System.err.println(
            "Could not get deserializer from " + argument + ": " + exception.toString());
          break; // fatal
        }
      }
      
      if (!file.exists()) {
        System.err.println("File doesn't exist: " + argument);
      } else {

        // get schema
        Schema schema = getDefaultSchema();
        // get default configuration
        ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
        if (verbose) {
          System.err.println("Default configuration:");
          for (Parameter p : configuration.values()) {
            System.err.println(" " + p.getName() + " = " + p.getValue());
          }
        }
        // confirm configuration
        deserializer.configure(configuration, schema);
        try {
          // get default parameters
          ParameterSet defaultParameters = deserializer.load(
            Utility.OneNamedStreamArray(file), schema);
          if (verbose) {
            System.err.println("Parameters:");
            for (Parameter p : defaultParameters.values()) {
              System.err.println(" " + p.getName() + " = " + p.getValue());
            }
          }
          // confirm default parameters
          deserializer.setParameters(defaultParameters);
          
          // deserialize
          Graph[] graphs = deserializer.deserialize();
          for (Graph graph : graphs) {
            if (verbose) System.err.println("Graph: " + graph.getId());
            if (firstGraph) {
              firstGraph = false;
            } else { // print array element separator
              System.out.println(",");
            }
            
            if (defaultOffsets) {
              if (verbose) System.err.println(
                graph.getId() + ": Generating default anchor offsets...");
              new DefaultOffsetGenerator().transform(graph);
            }
            
            // serialize as JSON
            JSONSerialization s = new JSONSerialization();
            s.configure(s.configure(new ParameterSet(), graph.getSchema()), graph.getSchema());
              if (verbose) System.err.println(
                graph.getId() + ": Serializing to JSON...");
            s.serialize(Utility.OneGraphSpliterator(graph), null,
                        // send stream to stdout (there will be only one):
                        stream -> {
                          try {
                            IO.Pump(stream.getStream(), System.out, false);
                          } catch(Exception exception) {
                            System.err.println(
                            "Could not write stream " + stream.getName()
                            + ": " + exception.toString());
                          }
                        },
                        // warnings/errors to stderr:
                        warning -> System.err.println(warning),
                        exception -> {
                          System.err.println(exception.getMessage());
                          exception.printStackTrace(System.err);
                        });
          } // next graph
          
        } catch(Exception exception) {
          System.err.println(
            "Could not process file " + file + ": " + exception.getMessage());
          exception.printStackTrace(System.err);
        }  
      } // file exists
    } // next argument
    // finish JSON array
    System.out.println("]");
    if (verbose) System.err.println("Finished.");
  }

  /**
   * Specify the schema to used by  {@link #convert(File)}.
   * @return The schema.
   */
  protected Schema getDefaultSchema() {
    return new Schema(
      "participant", "turn", "utterance", "word",
      new Layer("participant", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("participant").setParentIncludes(true),
      new Layer("utterance", "Utterances")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true));
  } // end of getDefaultSchema()

} // end of class Deserialize
