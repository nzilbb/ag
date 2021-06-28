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
package nzilbb.annotator.jython;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.script.*;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.util.IO;

/**
 * Annotator that executes a Python script on each transcript.
 * @author Robert Fromont robert@fromont.net.nz
 */
// Migration notes:
//  - thisLayer references -> sourceLayerIds[0]
//  - labbcat references -> ??
//  - cancelling() references -> isCancelling()
@UsesFileSystem
public class JythonAnnotator extends Annotator {
   /** Get the minimum version of the nzilbb.ag API supported by the serializer.*/
   public String getMinimumApiVersion() { return "1.0.0"; }

   /**
    * The URL for downloading Jython.
    * @see #getJythonUrl()
    * @see #setJythonUrl(String)
    */
   protected String jythonUrl = "https://repo1.maven.org/maven2/org/python/jython-standalone/2.7.2/jython-standalone-2.7.2.jar";
   /**
    * Getter for {@link #jythonUrl}: The URL for downloading Jython.
    * @return The URL for downloading Jython.
    */
   public String getJythonUrl() { return jythonUrl; }
   /**
    * Setter for {@link #jythonUrl}: The URL for downloading Jython.
    * @param newJythonUrl The URL for downloading Jython.
    */
   public JythonAnnotator setJythonUrl(String newJythonUrl) { jythonUrl = newJythonUrl; return this; }

   /**
    * Determines whether Jython is available (true) or needs to be downloaded (false).
    * @return true if Jython is available, false if needs to be downloaded.
    */
   public boolean getJythonAvailable() {
      File jythonJar = new File(getWorkingDirectory(), "jython.jar");
      return jythonJar.exists();
   } // end of isJythonAvailable()

   /**
    * Download Jython when setConfig is invoked, whether or not Jython is already available.
    * @see #getDownloadJython()
    * @see #setDownloadJython(boolean)
    */
   protected boolean downloadJython = false;
   /**
    * Getter for {@link #downloadJython}: Download Jython when setConfig is invoked,
    * whether or not Jython is already available.
    * @return Download Jython when setConfig is invoked, whether or not Jython is already
    * available. 
    */
   public boolean getDownloadJython() { return downloadJython; }
   /**
    * Setter for {@link #downloadJython}: Download Jython when setConfig is invoked, whether or 
    * not Jython is already available.  
    * @param newDownloadJython Download Jython when setConfig is invoked, whether or not Jython 
    * is already available.
    */
   public JythonAnnotator setDownloadJython(boolean newDownloadJython) { downloadJython = newDownloadJython; return this; }
   
   /**
    * The Python script engine.
    */
   protected ScriptEngine engine;
   
   /**
    * Python script.
    * @see #getScript()
    * @see #setScript(String)
    */
   protected String script;
   /**
    * Getter for {@link #script}: Python script.
    * @return Python script.
    */
   public String getScript() { return script; }
   /**
    * Setter for {@link #script}: Python script.
    * @param newScript Python script.
    */
   public JythonAnnotator setScript(String newScript) { script = newScript; return this; }

   /**
    * Default constructor.
    */
   public JythonAnnotator() {
   } // end of constructor
   
   /**
    * Creates a layer for output.
    * @param layerId
    * @param parentId
    * @param alignment
    * @return The new schema including the layer.
    */
   public Schema newLayer(String layerId, String parentId, int alignment)
      throws InvalidConfigurationException {
      if (schema.getLayer(layerId) != null) {
         throw new InvalidConfigurationException(this, "Layer already exists: " + layerId);
      }
      schema.addLayer(
         new Layer(layerId)
         .setAlignment(alignment)
         .setParentId(parentId));
      return schema;
   } // end of newLayer()

   /**
    * Provides the overall configuration of the annotator. 
    * @return The overall configuration of the annotator, which will be passed to the
    * <i> config/index.html </i> configuration web-app, if any. This configuration may be
    * null, or a string that serializes the annotators configuration state in any encoding
    * the implementor prefers. The resulting string must be interpretable by the
    * <i> config/index.html </i> web-app. 
    * @see #setConfig(String)
    * @see #beanPropertiesToQueryString()
    */
   public String getConfig() {
      return beanPropertiesToQueryString();
   }
   
   /**
    * Downloads jython.jar if necessary.
    * @throws InvalidConfigurationException
    * @see #getConfig()
    * @see #beanPropertiesFromQueryString(String)
    */ 
   public void setConfig(String config) throws InvalidConfigurationException {
      setRunning(true);
      setPercentComplete(0);
      try {
         setStatus(""); // clear any residual status from the last run...

         beanPropertiesFromQueryString(config);

         File jythonJar = new File(getWorkingDirectory(), "jython.jar");
         if (jythonJar.exists() && !downloadJython) {
            setStatus("Jython is already available.");
         } else {
            setStatus("Downloading Jython from: " + jythonUrl);
            System.out.println("Downloading Jython from: " + jythonUrl);
            IO.SaveUrlToFile(new URL(jythonUrl), jythonJar, new IntConsumer() {
                  public void accept(int percentComplete) {
                     setPercentComplete(percentComplete);
                  }
               });
         }
         setPercentComplete(100);
      } catch (IOException x) {
         throw new InvalidConfigurationException(this, "Could not get Jython from: " + jythonUrl);
      } finally {
         setRunning(false);
      }
   }

   /**
    * Sets the configuration for a given annotation task.
    * @param parameters The configuration of the annotator; a value of <tt> null </tt>
    * will apply the default task parameters, with {@link #tokenLayerId} set to the
    * {@link Schema#wordLayerId} and {@link #stemLayerId} set to <q>stem</q>.
    * @throws InvalidConfigurationException
    */
   public void setTaskParameters(String parameters) throws InvalidConfigurationException {
      if (schema == null)
         throw new InvalidConfigurationException(this, "Schema is not set.");
      
      if (parameters == null) { // there is no possible default parameter
         throw new InvalidConfigurationException(this, "Parameters not set.");         
      }

      // parse JSON parameters...

      // set basic attributes
      JsonObject json = beanPropertiesFromJSON(parameters);

      // validation...
      
      // there must be at least one source layer
      String[] requiredLayers = getRequiredLayers();
      if (requiredLayers.length == 0) {
         throw new InvalidConfigurationException(this, "There are no source layers specified.");
      }

      String[] outputLayers = getOutputLayers();
      // ensure all input layers exist
      for (String layerId : requiredLayers) {
         if (schema.getLayer(layerId) == null) {
            throw new InvalidConfigurationException(
               this, "Invalid required layer: " + layerId);
         }
      }
      // ensure all output layers exist
      for (String layerId : outputLayers) {
         if (schema.getLayer(layerId) == null) {
            schema.addLayer(
               new Layer(layerId)
               .setAlignment(Constants.ALIGNMENT_INTERVAL)
               .setPeers(true)
               .setParentId(schema.getRoot().getId())
               .setType(Constants.TYPE_STRING));
         }
      }
   }

   /**
    * Determines which layers the annotator requires in order to annotate a graph.
    * @return A list of layer IDs. In this case, the annotator only requires the schema's
    * word layer.
    * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
    * {@link #setSchema(Schema)} have not yet been called.
    */
   public String[] getRequiredLayers() throws InvalidConfigurationException {
      if (schema == null)
         throw new InvalidConfigurationException(this, "Schema is not set.");
      HashSet<String> requiredLayers = new HashSet<String>();
      // look for patterns that access layers
      String[] patterns = { // https://xkcd.com/1421/
         "\\.list\\(\"([^\"]+)\"\\)",       "\\.list\\('([^']+)'\\)",
         "\\.all\\(\"([^\"]+)\"\\)",        "\\.all\\('([^']+)'\\)",
         "\\.my\\(\"([^\"]+)\"\\)",         "\\.my\\('([^']+)'\\)",
         "\\.first\\(\"([^\"]+)\"\\)",      "\\.first\\('([^']+)'\\)",
         "\\.last\\(\"([^\"]+)\"\\)",       "\\.last\\('([^']+)'\\)",
         "\\.getAnnotations\\(\"([^\"]+)\"\\)"
         ,                                  "\\.getAnnotations\\('([^']+)'\\)",
         "\\.annotations\\(\"([^\"]+)\"\\)","\\.annotations\\('([^']+)'\\)",
         "\\.includingAnnotationsOn\\(\"([^\"]+)\"\\)"
         ,                                  "\\.includingAnnotationsOn\\('([^']+)'\\)",
         "\\.includedAnnotationsOn\\(\"([^\"]+)\"\\)"
         ,                                  "\\.includedAnnotationsOn\\('([^']+)'\\)",
         "\\.midpointIncludingAnnotationsOn\\(\"([^\"]+)\"\\)"
         ,                                  "\\.midpointIncludingAnnotationsOn\\('([^']+)'\\)",
         "\\.tagsOn\\(\"([^\"]+)\"\\)",     "\\.tagsOn\\('([^']+)'\\)",
         "\\.getAncestor\\(\"([^\"]+)\"\\)","\\.getAncestor\\('([^']+)'\\)",
         "\\.overlappingAnnotations\\([^)]+\"([^\"]+)\"\\)"
         ,                                  "\\.overlappingAnnotations\\([^)]+'([^']+)'\\)"
      };
      for (String pattern : patterns) {
         Matcher matcher = Pattern.compile(pattern).matcher(script);
         while (matcher.find()) {
            requiredLayers.add(matcher.group(1));
         } // next match
      } // next pattern
      return requiredLayers.toArray(new String[0]);
   }

   /**
    * Determines which layers the annotator will create/update/delete annotations on.
    * @return A list of layer IDs. In this case, the annotator has no task web-app for
    * specifying an output layer, and doesn't update any layers, so this method returns an
    * empty array.
    * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
    * {@link #setSchema(Schema)} have not yet been called.
    */
   public String[] getOutputLayers() throws InvalidConfigurationException {
      HashSet<String> outputLayers = new HashSet<String>();
      // look for patterns that annotate layers
      String[] patterns = { // https://xkcd.com/1421/ 
         // Annotation.createTag(layerId, label)
         "\\.createTag\\(\"([^\"]+)\",[^)]+\\)", 
         "\\.createTag\\('([^']+)',[^)]+\\)",
         // Graph.createTag(annotation, layerId, label)
         "transcript\\.createTag\\([^,]+,\\s*\"([^\"]+)\",[^)]+\\)", 
         "transcript\\.createTag\\([^,]+,\\s*'([^']+)',[^)]+\\)",
         // Graph.addTag(annotation, layerId, label)
         "\\.addTag\\([^,]+,\\s*\"([^\"]+)\",[^)]+\\)",
         "\\.addTag\\([^,]+,\\s*'([^']+)',[^)]+\\)",
         // Graph.createSpan(from, to, layerId, label[, parent])
         "\\.createSpan\\([^)\"]+,\\s*\"([^\"]+)\"[^)]*\\)", 
         "\\.createSpan\\([^)']+,\\s*'([^']+)'[^)]*\\)", 
         // Graph.addSpan(from, to, layerId, label[, parent])
         "\\.addSpan\\([^)\"]+,\\s*\"([^\"]+)\"[^)]*\\)", 
         "\\.addSpan\\([^)']+,\\s*'([^']+)'[^)]*\\)", 
         // Graph.createAnnotation(from, to, layerId, label[, parent])
         "\\.createAnnotation\\([^\"]+,\\s*\"([^\"]+)\"[^)]*\\)", 
         "\\.createAnnotation\\([^']+,\\s*'([^']+)'[^)]*\\)", 
         // Graph.addAnnotation(from, to, layerId, label[, parent])
         "\\.addAnnotation\\([^\"]+,\\s*\"([^\"]+)\"[^)]*\\)", 
         "\\.addAnnotation\\([^']+,\\s*'([^']+)'[^)]*\\)", 
      };
      for (String pattern : patterns) {
         Matcher matcher = Pattern.compile(pattern).matcher(script);
         while (matcher.find()) {
            outputLayers.add(matcher.group(1));
         } // next match
      } // next pattern
      return outputLayers.toArray(new String[0]);
   }
   
   /**
    * Transforms the graph. In this case, the graph is simply summarized, by counting all
    * tokens of each word type, and printing out the result to stdout.
    * @param graph The graph to transform.
    * @return The changes introduced by the tranformation.
    * @throws TransformationException If the transformation cannot be completed.
    */
   public Graph transform(Graph graph) throws TransformationException {
      setRunning(true);
     
      File jythonJar = new File(getWorkingDirectory(), "jython.jar");
      if (!jythonJar.exists()) {
         throw new TransformationException(
            this, "Jython is not available, please configured the annotator again.");
      } else {
         try {
            // load jython classes in jar
            ClassLoader loader = URLClassLoader.newInstance(
               new URL[] { jythonJar.toURI().toURL() },
               getClass().getClassLoader()
               );
            ScriptEngineManager manager = new ScriptEngineManager(loader);
            engine = manager.getEngineByExtension("py");
         } catch (MalformedURLException x) {
            throw new TransformationException(this, x);
         }
         if (engine == null) {
            throw new TransformationException(
               this, "Jython engine is not available, please configured the annotator again.");
         }
      }

      // create script engine
      ScriptEngineFactory factory = engine.getFactory();
      setStatus(
	 factory.getEngineName() + " ("+factory.getEngineVersion()+") for " 
	 + factory.getLanguageName() + " ("+factory.getLanguageVersion()+")");
      engine.put(
         ScriptEngine.FILENAME, IO.SafeFileNameUrl(Arrays.asList(getOutputLayers()).toString()) + ".py");
      ScriptContext context = engine.getContext();

      context.setAttribute("annotator", this, ScriptContext.ENGINE_SCOPE);
      context.setAttribute("transcript", graph, ScriptContext.ENGINE_SCOPE);

      try {
         // provide logging function
         engine.eval(
            "def log(message):"
            +"\n  annotator.setStatus(\""+graph.getId().replaceAll("\"", "\\\"")+": \" + message)"
            +"\n  return");
         
         setPercentComplete(25);
         
         // run script
         setStatus("Running script on " + graph.getId());
         // setStatus(script);
         try {
            engine.eval(script);
         } catch(ScriptException exception) {
            setStatus("Cancelling due to error: " + exception);
            cancel();
            throw new InvalidConfigurationException(this, exception);
         }
         
         setPercentComplete(75);
         
         // set confidences...
         
         if (!isCancelling()) {
            
            // for each annotation
            Set<String> destinationLayerIds = new HashSet<String>(Arrays.asList(getOutputLayers()));
            for (Annotation a : graph.getAnnotationsById().values()) {
               if (destinationLayerIds.contains(a.getLayerId())) {
                  // mark output layer changes as CONFIDENCE_AUTOMATIC
                  a.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
               } else {
                  // cancel changes to non-output layers
                  a.rollback();
               }
            } // next annotation
            
            // for each anchor
            for (Anchor a : graph.getAnchors().values()) {
               if (a.getChange() == Change.Operation.Create) {
                  // mark new anchors as CONFIDENCE_AUTOMATIC
                  a.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
               }
            } // next anchor
         } // not cancelling
      } catch (ScriptException exception) {
         throw new InvalidConfigurationException(this, exception);
      }
      setRunning(false);
      return graph;
   }
   
} // end of class JythonAnnotator
