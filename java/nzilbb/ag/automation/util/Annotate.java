//
// Copyright 2020 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag.automation.util;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;
import java.util.Arrays;
import java.io.File;
import java.io.IOException;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.Schema;
import nzilbb.ag.Layer;
import nzilbb.ag.TransformationException;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.serialize.GraphDeserializer;
import nzilbb.ag.serialize.GraphSerializer;
import nzilbb.ag.serialize.SerializationException;
import nzilbb.ag.serialize.SerializerNotConfiguredException;
import nzilbb.ag.serialize.SerializationParametersMissingException;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.util.CommandLineProgram;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;
import nzilbb.util.IO;

/**
 * Command-line utility for running a given annotator on a given set of transcript files.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(
   value="Utility for running a given annotator over transcript files",
   arguments="transcript-file ...")
public class Annotate extends CommandLineProgram {
   
   /** Command-line entrypoint */
   public static void main(String argv[]) {
      Annotate application = new Annotate();
      if (application.processArguments(argv)) {
         try {
            application.init();
            application.start();
         } catch(IOException x) {
            System.err.println("Could not open jar: "+x.getMessage());
         } catch(ClassNotFoundException x) {
            System.err.println("No implementation found: "+x.getMessage());
         } catch(TransformationException x) {
            System.err.println("Could not annotate: "+x.getMessage());
         } catch(SerializationException x) {
            System.err.println("Could not convert: "+x.getMessage());
         } catch(SerializerNotConfiguredException x) {
            System.err.println("Serialization not configured: "+x.getMessage());
         } catch(SerializationParametersMissingException x) {
            System.err.println("Serialization needs more information: "+x.getMessage());
         } catch(Throwable t) {
            System.err.println("Could not process: "+t.getMessage());
            t.printStackTrace(System.err);
         }
      }
   }
   
   // Attributes:

   /**
    * Annotator .jar file.
    * @see #getAnnotator()
    * @see #setAnnotator(File)
    */
   protected File annotator;
   /**
    * Getter for {@link #annotator}: Annotator .jar file.
    * @return Annotator .jar file.
    */
   public File getAnnotator() { return annotator; }
   /**
    * Setter for {@link #annotator}: Annotator .jar file.
    * @param newAnnotator Annotator .jar file.
    */
   @Switch(value="Annotator .jar for the annotation task",compulsory=true)
   public Annotate setAnnotator(File newAnnotator) { annotator = newAnnotator; return this; }

   /**
    * Serialization .jar file that implements both GraphSerializer and GraphDeserializer.
    * @see #getSerialization()
    * @see #setSerialization(File)
    */
   protected File serialization;
   /**
    * Getter for {@link #serialization}: Serialization .jar file that implements
    * both GraphSerializer and GraphDeserializer. 
    * @return Serialization .jar file that implements both GraphSerializer and GraphDeserializer.
    */
   public File getSerialization() { return serialization; }
   /**
    * Setter for {@link #serialization}: Serialization .jar file that implements
    * both GraphSerializer and GraphDeserializer. 
    * @param newSerialization Serialization .jar file that implements both
    * GraphSerializer and GraphDeserializer. 
    */
   @Switch(value="Serialization .jar for the transcript format conversion",compulsory=true)
   public Annotate setSerialization(File newSerialization) { serialization = newSerialization; return this; }

   /**
    * Suffix for adding to the file name of input transcripts in order to determine the
    * annotated transcript file name. 
    * <p> Default is <q>-annotated</q>.
    * @see #getOutputSuffix()
    * @see #setOutputSuffix(String)
    */
   protected String outputSuffix;
   /**
    * Getter for {@link #outputSuffix}: Suffix for adding to the file name of input
    * transcripts in order to determine the annotated transcript file name.
    * <p> Default is <q>-annotated</q>.
    * @return Suffix for adding to the file name of input transcripts in order to
    * determine the annotated transcript file name. 
    */
   public String getOutputSuffix() { return outputSuffix; }
   /**
    * Setter for {@link #outputSuffix}: Suffix for adding to the file name of input
    * transcripts in order to determine the annotated transcript file name. 
    * @param newOutputSuffix Suffix for adding to the file name of input transcripts in
    * order to determine the annotated transcript file name. 
    */
   @Switch("Suffix for output transcript files")
   public Annotate setOutputSuffix(String newOutputSuffix) { outputSuffix = newOutputSuffix; return this; }

   /**
    * Write annotated output to stdout instead of to a file. Default is false.
    * @see #getStdout()
    * @see #setStdout(Boolean)
    */
   protected Boolean stdout = Boolean.FALSE;
   /**
    * Getter for {@link #stdout}: Write annotated output to stdout instead of to a
    * file. Default is false. 
    * @return Write annotated output to stdout instead of to a file. Default is false.
    */
   public Boolean getStdout() { return stdout; }
   /**
    * Setter for {@link #stdout}: Write annotated output to stdout instead of to a
    * file. Default is false. 
    * @param newStdout Write annotated output to stdout instead of to a file.
    */
   @Switch("Write annotated output to stdout instead of to a file")
   public Annotate setStdout(Boolean newStdout) { stdout = newStdout; return this; }
   
   /**
    * Whether to print debug tracing. 
    * @see #getDebug()
    * @see #setDebug(Boolean)
    */
   protected Boolean debug = Boolean.FALSE;
   /**
    * Getter for {@link #debug}: Whether to print debug tracing. 
    * @return Whether to print debug tracing. 
    */
   public Boolean getDebug() { return debug; }
   /**
    * Setter for {@link #debug}: Whether to print debug tracing. 
    * @param newDebug Whether to print debug tracing. 
    */
   @Switch("Print debug tracing")
   public Annotate setDebug(Boolean newDebug) { debug = newDebug; return this; }

   protected Schema schema;
   protected AnnotatorDescriptor descriptor;
   protected GraphSerializer serializer;
   protected GraphDeserializer deserializer;
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public Annotate() {
   } // end of constructor
   
   /**
    * Initialize the deserializer, annotator, and serializer.
    * @throws IOException If a jar file could not be opened.
    * @throws ClassNotFoundException If an implementing class is not found.
    * @throws InvalidConfigurationException If the annator has no default configuration.
    */
   public void init() throws IOException, ClassNotFoundException, InvalidConfigurationException {
      initSchema();
      initAnnotator(); // first because it might alter the schema
      initDeserializer();
      initSerializer();
   } // end of init()
   
   /** Start processing the transcripts */
   public void start()
      throws TransformationException, SerializationException, IOException,
      SerializerNotConfiguredException, SerializationParametersMissingException {
      
      if (arguments.size() == 0) System.err.println("No transcripts specified.");
      // for each transcript
      for (String argument: arguments) {
         if (debug) System.out.println(argument);
         File inputTranscript = new File(argument);
         if (!inputTranscript.exists()) {
            System.err.println("Transcript not found: " + argument);
            continue;
         }
         Graph[] graphs = deserialize(inputTranscript);
         graphs = annotate(graphs);
         File outputTranscript = new File(
            inputTranscript.getParentFile(),
            IO.WithoutExtension(inputTranscript) + outputSuffix
            + "." + IO.Extension(inputTranscript));
         serialize(graphs, outputTranscript);
      } // next transcript
   }

   /**
    * Initializes the schema.
    */
   public void initSchema() {
      if (schema == null) {
         // use a generic schema
         schema = new Schema(
            "who", "turn", "utterance", "word",
            new Layer("who", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
            .setPeers(true).setPeersOverlap(true).setSaturated(true),
            new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
            .setPeers(true).setPeersOverlap(false).setSaturated(false)
            .setParentId("who").setParentIncludes(true),
            new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
            .setPeers(true).setPeersOverlap(false).setSaturated(true)
            .setParentId("turn").setParentIncludes(true),
            new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
            .setPeers(true).setPeersOverlap(false).setSaturated(false)
            .setParentId("turn").setParentIncludes(true));
      }
   } // end of initSchema()

   /**
    * Initializes the annotator.
    * @throws IOException If the jar file could not be opened.
    * @throws ClassNotFoundException If an implementing class is not found.
    * @throws InvalidConfigurationException If the annator has no default configuration.
    */
   public void initAnnotator()
      throws ClassNotFoundException, IOException, InvalidConfigurationException {
      
      // load annotator/descriptor
      descriptor = new AnnotatorDescriptor(annotator);
      Annotator annotator = descriptor.getInstance();
      if (debug) System.out.println("Annotator: " + descriptor);
      // check API version
      if (Constants.VERSION.compareTo(annotator.getMinimumApiVersion()) < 0) {
         System.err.println(
            "Annotator " + descriptor
            + " requires nzilbb.ag version " + annotator.getMinimumApiVersion()
            + " but this is version " + Constants.VERSION);
      }
      
      // configure it first, as it might change the schema
      annotator.setSchema(schema);
      String annotatorConfig = annotator.getConfig();
      if (debug) System.out.println("Annotator default config: " + annotatorConfig);
      // try for default configuration
      annotator.setConfig(annotatorConfig);
      // try for default task parameters
      annotator.setTaskParameters(null);

      // if we haven't been given a suffix to use, base the suffix on the output layers
      if (outputSuffix == null) {
         StringBuilder suffix = new StringBuilder();
         for (String layerId : annotator.getOutputLayers()) {
            suffix.append("-");
            suffix.append(IO.SafeFileNameUrl(layerId));            
         } // next output layer
         if (suffix.length() == 0) { // no output layers
            // so base the suffix on the annotator ID
            suffix.append("-");
            suffix.append(annotator.getAnnotatorId());
         }
         outputSuffix = suffix.toString();
      }
   } // end of initAnnotator()

   /**
    * Initializes the deserializer.
    * @throws IOException If the jar file could not be opened.
    */
   public void initDeserializer() throws IOException {
      
      // load deserializer
      deserializer = (GraphDeserializer)IO.FindImplementorInJar(
         serialization, getClass().getClassLoader(), GraphDeserializer.class);
      if (debug) System.out.println("Deserializer: " + deserializer.getDescriptor());
      // check API version
      if (Constants.VERSION.compareTo(deserializer.getDescriptor().getMinimumApiVersion()) < 0) {
         System.err.println(
            "Deserializer " + deserializer.getDescriptor()
            + " requires nzilbb.ag version " + deserializer.getDescriptor().getMinimumApiVersion()
            + " but this is version " + Constants.VERSION);
      }

      // configure deserializer
      ParameterSet deserializerConfig = deserializer.configure(new ParameterSet(), schema);
      if (debug) {
         System.out.println("Deserializer default config:");
         for (Parameter p : deserializerConfig.values()) System.out.println(" "+p);
      }
      if (deserializerConfig.size() > 0) { // some configuration is required
         // try with default values suggested by the deserializer
         deserializerConfig = deserializer.configure(deserializerConfig, schema);
         if (deserializerConfig.size() > 0) { // the deserializer still wants some configuration
            // display a warning
            for (Parameter parameter : deserializerConfig.unsetRequiredParameters().values()) {
               System.err.println(
                  "Required deserializer parameter not set: " + parameter.getName());
            } // next parameter
         }
      }
      
   } // end of initDeserializer()

   /**
    * Initializes the serializer.
    * @throws IOException If the jar file could not be opened.
    */
   public void initSerializer() throws IOException {
      // load serializer
      serializer = (GraphSerializer)IO.FindImplementorInJar(
         serialization, getClass().getClassLoader(), GraphSerializer.class);
      if (debug) System.out.println("Serializer: " + serializer.getDescriptor());
      // check API version
      if (Constants.VERSION.compareTo(serializer.getDescriptor().getMinimumApiVersion()) < 0) {
         System.err.println(
            "Serializer " + serializer.getDescriptor()
            + " requires nzilbb.ag version " + serializer.getDescriptor().getMinimumApiVersion()
            + " but this is version " + Constants.VERSION);
      }

      // configure serializer
      ParameterSet serializerConfig = deserializer.configure(new ParameterSet(), schema);
      if (debug) {
         System.out.println("Serializer default config:");
         for (Parameter p : serializerConfig.values()) System.out.println(" "+p);
      }
      if (serializerConfig.size() > 0) { // some configuration is required
         // try with default values suggested by the serializer
         serializerConfig = serializer.configure(serializerConfig, schema);
         if (serializerConfig.size() > 0) { // the serializer still wants some configuration
            // display a warning
            for (Parameter parameter : serializerConfig.unsetRequiredParameters().values()) {
               System.err.println(
                  "Required serializer parameter not set: " + parameter.getName());
            } // next parameter
         }
      }      
   } // end of initSerializer()

   /**
    * Deserializes the given transcript.
    * @param transcript The transcript file.
    * @return The graph represented by the transcript file.
    * @throws SerializationException 
    * @throws IOException 
    * @throw SerializerNotConfiguredException
    */
   public Graph[] deserialize(File transcript)
      throws SerializationException, IOException, SerializerNotConfiguredException,
      SerializationParametersMissingException {
      
      NamedStream[] stream = Utility.OneNamedStreamArray(transcript);
      if (debug) System.out.println("deserializing " + transcript.getName());
      
      // Load serialized form
      ParameterSet parameters = deserializer.load(Utility.OneNamedStreamArray(transcript), schema);
      if (debug) {
         System.out.println("Deserializer default parameters:");
         for (Parameter p : parameters.values()) System.out.println(" "+p);
      }
      
      // Set deserialization parameters using
      deserializer.setParameters(parameters);
      
      // Generation graph(s)
      Graph[] graphs = deserializer.deserialize();
      
      // Display or log warnings
      for (String warning : deserializer.getWarnings()) {
         System.err.println(deserializer.getDescriptor().getName() + ": " + warning );
      }

      return graphs;
   } // end of deserialize()
   
   /**
    * Annotate the given graph.
    * @param graphs The graphs to annotate.
    * @return The annotated graphs.
    */
   public Graph[] annotate(Graph[] graphs) throws TransformationException {
      
      Annotator annotator = descriptor.getInstance();
      if (debug) System.out.println("Annotator: " + annotator.getAnnotatorId());
      
      // getRequiredLayers() is invoked to determine which layers are needed.
      String[] requiredLayerIds = annotator.getRequiredLayers();
      if (debug) for (String l : requiredLayerIds) System.out.println("Required: " + l);
      // check we have them
      for (String layerId : requiredLayerIds) {
         if (schema.getLayer(layerId) == null) {
            System.err.println(
               "Annotator " + annotator.getAnnotatorId() + " needs missing layer " + layerId);
         }
      }
      
      // getOutputLayers() is invoked to determine which layer are annotated.
      String[] outputLayerIds = annotator.getOutputLayers();
      if (debug) for (String l : outputLayerIds) System.out.println("Output: " + l);
      // check they exist
      for (String layerId : outputLayerIds) {
         if (schema.getLayer(layerId) == null) {
            System.err.println(
               "Annotator " + annotator.getAnnotatorId() + " outputs missing layer " + layerId);
         }
      }

      for (Graph graph : graphs) {
         // ensure the graph schema includes the output layers
         for (String layerId : outputLayerIds) {
            if (graph.getLayer(layerId) == null) {
               if (debug) System.out.println("Adding " + layerId + " to " + graph.getId());
               graph.addLayer((Layer)schema.getLayer(layerId).clone());
            }
         } // next output layer
         
         // transform(Graph) transform(graph) is invoked
         if (debug) System.out.println("transforming: " + graph.getId());
         annotator.transform(graph);
      } // next graph

      return graphs;
   } // end of annotate()

   /**
    * Serialize the given graph to a given file name.
    * @param graphs The graphs to save.
    * @param destination The destination file name to use.
    * @throws IOException If the file(s) can't be written.
    * @throws SerializationParametersMissingException If the serializer needs more information.
    * @throws SerializerNotConfiguredException If the serializer has not been configured.
    */
   public void serialize(Graph[] graphs, File destination)
      throws IOException, SerializationParametersMissingException,
      SerializerNotConfiguredException {
      if (debug) System.out.println("Serializer: " + serializer.getDescriptor());
      
      // Determine which layers are required for the serialization by calling getRequiredLayers()
      String[] requiredLayerIds = serializer.getRequiredLayers();
      if (debug) for (String l : requiredLayerIds) System.out.println("Required: " + l);      
      // check we have them
      for (String layerId : requiredLayerIds) {
         if (schema.getLayer(layerId) == null) {
            System.err.println(
               "Serializer " + serializer.getDescriptor() + " needs missing layer " + layerId);
         }
      }

      // create a consumer for processing the output streams
      Consumer<NamedStream> consumer = stream -> {
         stream.setName(destination.getName());
         try {
            stream.save(destination.getParentFile());
            System.out.println(destination.getPath());
         } catch(IOException x) {
            System.err.println("ERROR saving: " + destination.getPath() + ": " + x);
         }
      };
      if (serializer.getCardinality() == GraphSerializer.Cardinality.NToM) { // multiple output files
         // destination will be a directory for output files
         if (!destination.exists()) {
            if (!destination.mkdir()) {
               System.err.println("Cannot create output directory: " + destination.getPath());
            }
         }
         consumer = stream -> {
            try {
               stream.save(destination);
               System.out.println(destination.getPath() + "/" + stream.getName());
            } catch(IOException x) {
               System.err.println(
                  "ERROR saving: " + stream.getName() + " to " + destination.getPath() + ": " + x);
            }
         };
      } // multiple output files
      if (stdout) {
         consumer = stream -> {
            try {
               IO.Pump(stream.getStream(), System.out);
            } catch(IOException x) {
               System.err.println("ERROR writing: " + stream.getName() + ": " + x);
            }
         };
      }
      // Serialize the graphs
      serializer.serialize(
         Arrays.spliterator(graphs),
         schema.getLayers().keySet().toArray(new String[0]),
         consumer,
         warning -> System.err.println(warning),
         exception -> System.err.println("ERROR serializing: " + exception));
   } // end of serialize()
   
} // end of class Annotate
