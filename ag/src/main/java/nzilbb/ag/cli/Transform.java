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
import nzilbb.ag.Annotation;
import nzilbb.ag.Change;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.GraphTransformer;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.TransformationException;
import nzilbb.ag.serialize.GraphDeserializer;
import nzilbb.ag.serialize.json.JSONSerialization;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.util.DefaultOffsetGenerator;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.util.CommandLineProgram;
import nzilbb.util.IO;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;

/**
 * Transforms given annotations graphs.
 * <p> Derive {@link GraphTransformer} implementations from this class in order to make
 * the transformer avaible on the command line.
 * e.g. if the following is implemented:
 * <pre>package nzilbb.ag.util;
 * import nzilbb.ag.cli.Deserialize;
 * ...
 * public class Normalizer extends Transform implements GraphTransformer {
 *   ...
 *   public static void main(String argv[]) {
 *     Normalizer cli = new Normalizer();
 *     if (cli.processArguments(argv)) {
 *       cli.start();
 *     }
 *   }
 * }</pre>
 * ... then the transformer can be invoked on the command line to normalize JSON-encoded
 * graphs: <br>
 * <code>java -cp nzilbb.ag.jar nzilbb.ag.util.Normalize &lt; graph.json &gt; normalized.json</code>
 * <p> <i>stdin</i> is assumed to contain one or more JSON-encoded annotation graphs,
 * which are deserialized, tranformed, and serialized to <i>stdout</i> as JSON.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class Transform extends CommandLineProgram {

  /** Transformer to use. */
  protected static GraphTransformer transformer = null;
  
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
  public Transform setVerbose(Boolean newVerbose) { verbose = newVerbose; return this; }
  
  public void start() {

    // if the user is just looking for usage info, don't wait for data on stdin
    if (getUsage()) return;

    // parse stdin
    JSONSerialization s = new JSONSerialization();
    s.configure(s.configure(new ParameterSet(), null), null);
    s.setSortAnchors(true);
    
    try {
      ParameterSet parameters = s.load(
        Utility.OneNamedStreamArray(
          new NamedStream(System.in, "<stdin>", "application/json")), null);
      s.setParameters(parameters); // run with default values
      Graph[] graphs = s.deserialize();
      
      // start JSON array (because there may be more than one graph output)
      System.out.print("[");
      boolean firstGraph = true;

      if (transformer == null && this instanceof GraphTransformer) {
        transformer = (GraphTransformer)this;
      }
      
      for (Graph graph : graphs) {
        if (verbose) System.err.println("Graph: " + graph.getId());
        if (firstGraph) {
          firstGraph = false;
        } else { // print array element separator
          System.out.println(",");
        }

        // track changes so that destroyed annotations are tracked
        graph.trackChanges();

        try {
          // transform
          graph = transformer.transform(graph);
          
          // ensure any annotations marked for destruction are not output
          graph.commit();
          
          // serialize as JSON
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
        } catch(TransformationException x) {
          System.err.println(graph.getId() + " : transformation failed: " + x.getMessage());
          x.printStackTrace(System.err);
        }
      } // next graph
      
      // finish JSON array
      System.out.println();
      System.out.print("]");
      if (verbose) System.err.println("Finished.");
    } catch(Exception exception) {
      System.err.println(exception.toString());
      exception.printStackTrace(System.err);
    }
  }

} // end of class Transform
