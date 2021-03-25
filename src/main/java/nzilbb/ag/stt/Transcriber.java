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
package nzilbb.ag.stt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.jar.JarFile;
import javax.json.Json;
import javax.json.JsonObject;
import nzilbb.ag.*;
import nzilbb.util.IO;
import nzilbb.util.MonitorableTask;

/**
 * Base class for an automated transcriber module.
 * <p> When {@link #transcribe(File, Graph)} is invoked, it should transcribe the given audio
 * file and insert the corresponding transcription data into the given annotation graph.
 * <p> The typical lifecycle of an transcriber is:
 * <ol>
 *  <li> The module is installed:
 *       <ol>
 *         <li> {@link #setSchema(Schema)} is invoked. </li>
 *         <li> {@link #setWorkingDirectory(File)} is invoked. </li>
 *         <li> {@link #getConfig()} is invoked, in case the transcriber has a default
 *              configuration. </li>
 *         <li> the user is presentated with the <a href="package-summary.html#config">
 *              config web-app</a>, if any, and </li> 
 *         <li> {@link #setConfig(String)} is invoked. (if there's no config web-app,
 *              then the config string passed will be the result of the earlier 
 *              <tt>getConfig()</tt> invocation) </li>
 *       </ol>
 *  </li>
 *  <li> Transcriber may be then run one or more times:
 *       <ol>
 *         <li> {@link #setSchema(Schema)} is invoked to provide the current schema. </li>
 *         <li> {@link #setWorkingDirectory(File)} is invoked. </li>
 *         <li> {@link #getDiarizationRequired()} is called to determine if the audio
 *              need chunking before calling {@link #transcribe(File, Graph)}. </li>
 *         <li> {@link #transcribe(File, Graph) transcribe(audio, transcript)} is invoked
 *              with the speech file and a graph that should contain the transcript. </li>
 *       </ol>
 *  </li>
 *  <li> The module is uninstalled, in which case {@link #uninstall()} is invoked, which
 *       should remove all persistent data on the system. </li>
 * </ol>
 * <p> The methods below marked in <b> bold </b> are those that an Transcriber subclass
 * should implement, in addition to {@link IGraphTransformer#transform(Graph) transform(graph)}.
 * @author Robert Fromont robert@fromont.net.nz
 */
public abstract class Transcriber implements MonitorableTask {
   
   /**
    * Unique name for the transcriber module, which is immutable across versions of the
    * implemetantation. 
    * <p> The default implementation returns the (simple) name of the transcriber
    * implementation class. 
    * @return The transcriber's ID.
    */
   public String getTranscriberId() {
      return getClass().getSimpleName();
   }
   
   /**
    * Version of this implementation; versions will typically be numeric, but this is not
    * a requirement.
    * <p> The default implementation assumes that the .jar file the transcriber is deployed
    * in has a 'comment' defined which contains the version (e.g. <q>20200708.1646</q>).
    * <p> If this is not the case, or some other versioning scheme is desired, the derived
    * class should override this method.
    * @return Transcriber version.
    */
   public String getVersion() {
      nzilbb.util.IO.ProjectProperties(getClass()).getProperty("version");
      return null;
   }
   
   /**
    * <b> Get the minimum version of the nzilbb.ag API supported by the serializer. </b>
    * @return Minimum version of the nzilbb.ag API supported by the serializer.
    * @see Constants#VERSION
    */
   public abstract String getMinimumApiVersion();

   /**
    * A persistent directory in which files can be saved and accessed.
    * @see #getWorkingDirectory()
    * @see #setWorkingDirectory(File)
    */
   private File workingDirectory;
   /**
    * A persistent directory in which files can be saved and accessed.
    * @return A persistent directory in which files can be saved and accessed.
    */
   public File getWorkingDirectory() { return workingDirectory; }
   /**
    * Setter for {@link #workingDirectory}: A persistent directory in which files can be saved
    * and accessed. 
    * <p> This is automatically called providing the implementation with persistent access 
    * to the file system.
    * @param directory A persistent directory in which files can be saved and accessed.
    */
   public Transcriber setWorkingDirectory(File directory) { 
      workingDirectory = directory;
      
      // load configuration, if any
      File f = new File(workingDirectory, getTranscriberId() + ".cfg");
      if (f.exists()) {
         try {
            beanPropertiesFromQueryString(IO.InputStreamToString(new FileInputStream(f)));
         } catch(IOException exception) {
            System.err.println("Could not read configuration file "+f.getPath()+": "+exception);
            exception.printStackTrace(System.err);
         }
      }
      return this;
   }

   /**
    * Whether the transcriber is currently annotating.
    * @see #getRunning()
    */
   protected boolean running = false;
   /**
    * Getter for {@link #running}: Whether the transcriber is currently annotating.
    * @return Whether the transcriber is currently annotating.
    */
   public boolean getRunning() { return running; }

   /**
    * The current status of the task.
    * @see #getStatus()
    * @see #setStatus(String)
    */
   protected String status;
   /**
    * Getter for {@link #status}: The current status of the task.
    * @return The current status of the task.
    */
   public String getStatus() { return status; }
   /**
    * Setter for {@link #status}: The current status of the task.
    * @param newStatus The current status of the task.
    */
   public Transcriber setStatus(String newStatus) { status = newStatus; return this; }
   
   /**
    * The layer schema.
    * @see #getSchema()
    * @see #setSchema(Schema)
    */
   protected Schema schema;
   /**
    * Getter for {@link #schema}: The layer schema.
    * @return The layer schema.
    */
   public Schema getSchema() { return schema; }
   /**
    * Setter for {@link #schema}: The layer schema.
    * @param newSchema The layer schema.
    */
   public Transcriber setSchema(Schema newSchema) { schema = newSchema; return this; }
   
   /**
    * Provides the overall configuration of the transcriber. 
    * @return The overall configuration of the transcriber, which will be passed to the
    * <i> config/index.html </i> configuration web-app, if any. This configuration may be
    * null, or a string that serializes the transcribers configuration state in any encoding
    * the implementor prefers. The resulting string must be interpretable by the
    * <i> config/index.html </i> web-app. 
    * @see #setConfig(String)
    * @see #beanPropertiesToQueryString()
    */
   public String getConfig() { return null; }
   
   /**
    * <b> Specifies the overall configuration of the transcriber, and runs any processing
    * required to install the transcriber. </b>
    * <p> This processing is assumed to be synchronous (this method doesn't return until
    * it's complete) and long-running, so the {@link MonitorableTask} methods should
    * provide a way for the caller to monitor/cancel processing - i.e. the Transcriber class
    * should provide an indication of progress by calling
    * {@link Transcriber#setPercentComplete(Integer)} and should regularly check 
    * {@link Transcriber#isCancelling()} to determine if installation should be stopped.
    * <p> If the user should provide information before this method is called, a 
    * <tt> config </tt> web-app must be provided to implement the user interface, which sets
    * any required configuration by invoking methods of the transcriber as required, and
    * invoking <tt> setConfig </tt> when configuration is ready.
    * <p> If the configuration needs to be persistent between installing the transcriber the
    * first time and subsequently upgrading it, then it is the transcriber's responsibility
    * to serialize it in a form which can be retrieved for a later call to {@link #getConfig()}.
    * @throws InvalidConfigurationException
    * @see #getConfig()
    * @see #beanPropertiesFromQueryString(String)
    */ 
   public void setConfig(String config) throws InvalidConfigurationException { setPercentComplete(100); }
   
   /**
    * Converts bean properties to a query string.
    * <p> This utility method uses introspection to discover bean property getters
    * (i.e. methods of the form <code> getAbc() </code> where <tt>abc</tt> is taken to be
    * the name of an object property) that are declared by the transcriber class. Getters
    * are called and the property names and their values concatenated into an HTTP query
    * string. 
    * <p> e.g. if the transcriber object has:
    * <ul>
    * <li> a method called <code>getFoo()</code> which returns the value <q>bar</q>, and</li> 
    * <li> a method called <code>getBar()</code> which returns the value <q>fubar</q>,</li> 
    * </ul>
    * &hellip; then this method will return <q>foo=bar&amp;bar=fubar</q>
    * @return An HTTP query string representing the state of the transcriber object.
    */
   protected String beanPropertiesToQueryString() {
      StringBuilder query = new StringBuilder();
      for (Method method : getClass().getDeclaredMethods()) {
         if (method.getName().equals("getTranscriberId")
             || method.getName().equals("getVersion")
             || method.getName().equals("getSchema")
             || method.getName().equals("getConfig")
             || method.getName().equals("getRequiredLayers")
             || method.getName().equals("getOutputLayers")
             || method.getName().equals("getPercentComplete")) continue;
         
         try {
            if (method.getName().startsWith("get")) { // found a getter
               String property =
                  method.getName().substring(3,4).toLowerCase()
                  + method.getName().substring(4);
               Object value = method.invoke(this);
               if (value != null) {
                  if (query.length() > 0) query.append("&");
                  query.append(property);
                  query.append("=");
                  query.append(URLEncoder.encode(value.toString(), "UTF-8"));
               } // there is a value
            } // found a getter
         } catch(Throwable t) {
            System.out.println(""+t);
         }
      } // next declared method
      return query.toString();
   } // end of beanPropertiesToQueryString()
   
   /**
    * Converts a query string to bean properties.
    * <p> This utility method parses the given query string (most likely the body of a
    * POST request generated by an HTML form implemented by <i>config/index.html</i>), and
    * calls setters on this object that are named after the parameters found.
    * <p> e.g. if <var> query </var> is <q>foo=bar&amp;bar=fubar</q> then 
    * <ul>
    * <li> the annotation class's <code>setFoo</code> would be called with the value
    *      <q>bar</q>, and</li> 
    * <li> the annotation class's <code>setBar</code> would be called with the value
    *      <q>fubar</q>.</li> 
    * </ul>
    * @param query The URL query string to parse.
    */
   @SuppressWarnings({"rawtypes","unchecked"})
   protected void beanPropertiesFromQueryString(String query) {
      if (query == null) return;
      for (String parameter : query.split("&")) {
         int equals = parameter.indexOf('=');
         if (equals <= 0) continue;
         String property = parameter.substring(0, equals);
         String valueString = parameter.substring(equals + 1);
         Class transcriberClass = getClass();
         try {
            Method setter = transcriberClass.getMethod(
               "set" + property.substring(0,1).toUpperCase() + property.substring(1),
               String.class);
            setter.invoke(this, URLDecoder.decode(valueString, "UTF-8"));
         } catch(Throwable notString) {
            try {
               Method setter = transcriberClass.getMethod(
                  "set" + property.substring(0,1).toUpperCase() + property.substring(1),
                  Boolean.class);
               // accept truthy values
               if ("on".equalsIgnoreCase(valueString)
                   || "yes".equalsIgnoreCase(valueString)
                   || "y".equalsIgnoreCase(valueString)
                   || "t".equalsIgnoreCase(valueString)
                   || "1".equals(valueString)) valueString = "true";
               setter.invoke(this, Boolean.valueOf(valueString));            
            } catch(Throwable notBoolean) {
               try {
                  Method setter = transcriberClass.getMethod(
                     "set" + property.substring(0,1).toUpperCase() + property.substring(1),
                     boolean.class);
                  // accept truthy values
                  if ("on".equalsIgnoreCase(valueString)
                      || "yes".equalsIgnoreCase(valueString)
                      || "y".equalsIgnoreCase(valueString)
                      || "t".equalsIgnoreCase(valueString)
                      || "1".equals(valueString)) valueString = "true";
                  setter.invoke(this, Boolean.valueOf(valueString));            
               } catch(Throwable notBool) {
                  try {
                     Method setter = transcriberClass.getMethod(
                        "set" + property.substring(0,1).toUpperCase() + property.substring(1),
                        Integer.class);
                     setter.invoke(this, Integer.valueOf(valueString));            
                  } catch(Throwable notInteger) {
                     try {
                        Method setter = transcriberClass.getMethod(
                           "set" + property.substring(0,1).toUpperCase() + property.substring(1),
                           int.class);
                        setter.invoke(this, Integer.valueOf(valueString));            
                     } catch(Throwable notInt) {
                        try {
                           Method setter = transcriberClass.getMethod(
                              "set"
                              + property.substring(0,1).toUpperCase() + property.substring(1),
                              Double.class);
                           setter.invoke(this, Double.valueOf(valueString));            
                        } catch(Throwable notDouble) {
                           try {
                              Method setter = transcriberClass.getMethod(
                                 "set"
                                 + property.substring(0,1).toUpperCase() + property.substring(1),
                                 double.class);
                              setter.invoke(this, Double.valueOf(valueString));            
                           } catch(Throwable notD) {
                              try {
                                 Method setter = transcriberClass.getMethod(
                                    "set"
                                    + property.substring(0,1).toUpperCase()
                                    + property.substring(1),
                                    Float.class);
                                 setter.invoke(this, Float.valueOf(valueString));            
                              } catch(Throwable notFloat) {
                                 try {
                                    Method setter = transcriberClass.getMethod(
                                       "set"
                                       + property.substring(0,1).toUpperCase()
                                       + property.substring(1),
                                       float.class);
                                    setter.invoke(this, Float.valueOf(valueString));            
                                 } catch(Throwable notF) {
                                 } // not float
                              } // not Float
                           } // not double
                        } // not Double
                     } // not int
                  } // not Integer
               } // not boolean
            } // not Boolean
         } // not String
      }  // next parameter
   } // end of beanPropertiessFromQueryString()
   
   /**
    * Converts a query string to bean properties.
    * <p> This utility method parses the given JSON string (most likely the body of a
    * POST request generated by an HTML form implemented by <i>config/index.html</i>), and
    * calls setters on this object that are named after the top-level string attributes found.
    * <p> e.g. if <var> jsonString </var> is
    * <q>{"foo":"bar","bar":"1","complex": {"objects": "ignored"}}</q> then 
    * <ul>
    * <li> the annotation class's <code>setFoo</code> would be called with the value
    *      <q>bar</q>, and</li> 
    * <li> the annotation class's <code>setBar</code> would be called with the value
    *      <q>1</q>.</li> 
    * </ul>
    * @param jsonString The JSON string to parse.
    * @return A JsonObject parsed from the <var>jsonString</var>.
    */
   @SuppressWarnings({"rawtypes","unchecked"})
   protected JsonObject beanPropertiesFromJSON(String jsonString) {
      if (jsonString == null) return null;
      JsonObject json = Json.createReader(new StringReader(jsonString)).readObject();
      for (String property : json.keySet()) {
         if (property.length() == 0) continue;
         try {
            String valueString = json.getString(property);
            Class transcriberClass = getClass();
            try {
               Method setter = transcriberClass.getMethod(
                  "set" + property.substring(0,1).toUpperCase() + property.substring(1),
                  String.class);
               setter.invoke(this, URLDecoder.decode(valueString, "UTF-8"));
            } catch(Throwable notString) {
               try {
                  Method setter = transcriberClass.getMethod(
                     "set" + property.substring(0,1).toUpperCase() + property.substring(1),
                     Boolean.class);
                  // accept truthy values
                  if ("on".equalsIgnoreCase(valueString)
                      || "yes".equalsIgnoreCase(valueString)
                      || "y".equalsIgnoreCase(valueString)
                      || "t".equalsIgnoreCase(valueString)
                      || "1".equals(valueString)) valueString = "true";
                  setter.invoke(this, Boolean.valueOf(valueString));            
               } catch(Throwable notBoolean) {
                  try {
                     Method setter = transcriberClass.getMethod(
                        "set" + property.substring(0,1).toUpperCase() + property.substring(1),
                        boolean.class);
                     // accept truthy values
                     if ("on".equalsIgnoreCase(valueString)
                         || "yes".equalsIgnoreCase(valueString)
                         || "y".equalsIgnoreCase(valueString)
                         || "t".equalsIgnoreCase(valueString)
                         || "1".equals(valueString)) valueString = "true";
                     setter.invoke(this, Boolean.valueOf(valueString));            
                  } catch(Throwable notBool) {
                     try {
                        Method setter = transcriberClass.getMethod(
                           "set" + property.substring(0,1).toUpperCase() + property.substring(1),
                           Integer.class);
                        setter.invoke(this, Integer.valueOf(valueString));            
                     } catch(Throwable notInteger) {
                        try {
                           Method setter = transcriberClass.getMethod(
                              "set" + property.substring(0,1).toUpperCase() + property.substring(1),
                              int.class);
                           setter.invoke(this, Integer.valueOf(valueString));            
                        } catch(Throwable notInt) {
                           try {
                              Method setter = transcriberClass.getMethod(
                                 "set"
                                 + property.substring(0,1).toUpperCase() + property.substring(1),
                                 Double.class);
                              setter.invoke(this, Double.valueOf(valueString));            
                           } catch(Throwable notDouble) {
                              try {
                                 Method setter = transcriberClass.getMethod(
                                    "set"
                                    + property.substring(0,1).toUpperCase() + property.substring(1),
                                    double.class);
                                 setter.invoke(this, Double.valueOf(valueString));            
                              } catch(Throwable notD) {
                                 try {
                                    Method setter = transcriberClass.getMethod(
                                       "set"
                                       + property.substring(0,1).toUpperCase()
                                       + property.substring(1),
                                       Float.class);
                                    setter.invoke(this, Float.valueOf(valueString));            
                                 } catch(Throwable notFloat) {
                                    try {
                                       Method setter = transcriberClass.getMethod(
                                          "set"
                                          + property.substring(0,1).toUpperCase()
                                          + property.substring(1),
                                          float.class);
                                       setter.invoke(this, Float.valueOf(valueString));            
                                    } catch(Throwable notF) {
                                    } // not float
                                 } // not Float
                              } // not double
                           } // not Double
                        } // not int
                     } // not Integer
                  } // not boolean
               } // not Boolean
            } // not String
         } catch (ClassCastException x) {} // ignore non-string values
      }  // next parameter
      return json;
   } // end of beanPropertiessFromQueryString()
   
   /**
    * <b> Runs any processing required to uninstall the transcriber. </b>
    */
   public void uninstall() { }
   
   private boolean ignoreSetPercentComplete = false;
   private Integer percentComplete;
   /**
    * Setter for {@link #percentComplete}: Progress indicator; set to 100 when processing
    * is complete. 
    * @param newPercentComplete Progress indicator; set to 100 when processing is complete.
    */
   protected Transcriber setPercentComplete(Integer newPercentComplete) {
      if (!ignoreSetPercentComplete) percentComplete = newPercentComplete;
      return this;
   }
   /**
    * Getter for {@link #percentComplete}: Progress indicator; set to 100 when processing
    * is complete. 
    * @return Progress indicator; set to 100 when processing is complete.
    */
   public Integer getPercentComplete() { return percentComplete; }

   private boolean cancelling = false;   
   /**
    * Determines whether the user has requested that processing be cancelled.
    * @return true if {@link #cancel()} has been called, false otherwise.
    */
   protected boolean isCancelling() {
      return cancelling;
   } // end of isCancelling()
   /**
    * Cancels the current operation, if any.
    */
   public void cancel() {
      cancelling = true;
   }

   /**
    * <b> Specify whether the transcriber needs the audio to be split into utterance chunks
    * before {@link #transcribe(File,Graph)} is called. </b>
    * <p> If the transcriber returns true when this method is called, it should assume
    * that the {@link Schema#getParticipantLayerId() participant},
    * {@link Schema#getTurnLayerId() turn} and {@link Schema#getUtteranceLayerId() utterance} 
    * layers are populated when
    * {@link #transcribe(File,Graph)} is called, and that the utterance annotations define
    * the start and end times of individual speaker utterances for transcription.
    * <p> If the transcriber returns false when this method is called, it should assume
    * that the {@link Schema#getTurnLayerId() turn} and
    * {@link Schema#getUtteranceLayerId() utterance} layers are empty when
    * {@link #transcribe(File,Graph)} is called. 
    * @re{@link Schema#getTurnLayerId() turn} true if diarization is required, false otherwise.
    */
   public abstract boolean getDiarizationRequired();

   /**
    * <b> Transcribes the given audio file, saving the resulting transcript in the given
    * graph. </b>
    * @param speech An audio file containing the speech to transcribe.
    * @param transcript The annotation graph that should contain the transcription. 
    * <p> If the transcriber's {@link #getDiarizationRequired()} returns false, the
    * annotation graph may or may not have any annotations on the
    * {@link Schema#getTurnLayerId() turn}, {@link Schema#getUtteranceLayerId() utterance}, and
    * {@link Schema#getWordLayerId() word} layers. If there are existing annotations, they
    * should be re-used if possible, or {@link Annotation#destroy()} should be called on
    * each to ensure they're removed from the graph.
    * <p> If the transcriber's {@link #getDiarizationRequired()} returns true, it should
    * be assumed that the annotation graph has annotations on the
    * {@link Schema#getParticipantLayerId() participant}, {@link Schema#getTurnLayerId() turn}, 
    * and {@link Schema#getUtteranceLayerId() utterance} layers, and that the utterance
    * annotations define the start and end times of individual speaker utterances for
    * transcription. In this case, the transcriber should fill in the labels of the given
    * utterance annotations.
    * @return The given graph. This should have annotations structured as follows:
    *  <ul>
    *   <li> Annotations on the {@link Schema#getParticipantLayerId() participant} layer,
    *        if the given transcript had no pre-existing participants. </li>
    *   <li> Annotations on the {@link Schema#getTurnLayerId() turn} layer (even if it's
    *        one big turn encompassing the whole transcript), with the parent(s) set to the
    *        corresponding participant annotations. The turn labels should match the participant
    *        labels </li> 
    *   <li> Annotations on the {@link Schema#getUtteranceLayerId() utterance} layer, with
    *        the parent(s) set to the corresponding turn annotations. The labels should be the
    *        transcript of the utterance. </li>
    *   <li> Optionally, new annotations on the {@link Schema#getWordLayerId() word}
    *        layer, representing individual word tokens with alignment information, if
    *        available.</li> 
    *  </ul>
    * @throws Exception
    */
   public abstract Graph transcribe(File speech, Graph transcript) throws Exception;
   
} // end of class Transcriber
