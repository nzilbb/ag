//
// Copyright 2020-2021 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag.automation;

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
import java.sql.*;
import java.util.List;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonObject;
import nzilbb.ag.*;
import nzilbb.sql.ConnectionFactory;
import nzilbb.sql.mysql.MySQLTranslator;
import nzilbb.util.IO;
import nzilbb.util.MonitorableTask;

/**
 * Base class for an automated annotation module.
 * <p> When {@link GraphTransformer#transform(Graph)} is invoked, it should perform
 * whatever annotation task has been configured by the last call to
 * {@link #setTaskParameters(String)}.
 * <p> The typical lifecycle of an annotator is:
 * <ol>
 *  <li> The module is installed:
 *       <ol>
 *         <li> {@link #setSchema(Schema)} is invoked. </li>
 *         <li> {@link #getConfig()} is invoked, in case the annotator has a default
 *              configuration. </li>
 *         <li> the user is presentated with the <a href="package-summary.html#config">
 *              config web-app</a>, if any, and </li> 
 *         <li> {@link #setConfig(String)} is invoked. (if there's no config web-app,
 *              then the config string passed will be the result of the earlier 
 *              <tt>getConfig()</tt> invocation) </li>
 *       </ol>
 *  </li>
 *  <li> One or more annotation tasks are created; for each one: 
 *       <ol>
 *         <li> {@link #setSchema(Schema)} is invoked. </li>
 *         <li> the user is presented with the <a href="package-summary.html#task">
 *              task web-app</a>, if any, and </li> 
 *         <li> {@link #setTaskParameters(String)} is invoked to allow any installation
 *              processing to occur; e.g. persisting configuration, loading lexicons, etc. </li>
 *       </ol>
 *  </li>
 *  <li> Annotation tasks may be then run one or more times:
 *       <ol>
 *         <li> {@link #setSchema(Schema)} is invoked to provide the current schema. </li>
 *         <li> {@link #setTaskParameters(String)} is invoked to provide the parameters. 
 *              (it's possible that this will be invoked with null parameters to determine
 *              whether the annotator has a default task configuration) </li>
 *         <li> {@link #getRequiredLayers()} is invoked to determine which layers are needed. </li>
 *         <li> {@link #getOutputLayers()} is invoked to determine which layer are annotated. </li> 
 *         <li> {@link IGraphTransformer#transform(Graph) transform(graph)} is invoked
 *              with a graph that should be annotated. It can be assumed to have
 *              annotations on its input layers, and possible annotations on its output
 *              layers as well (produced by previous calls to
 *              {@link IGraphTransformer#transform(Graph) transform(graph)} or by other
 *              annotators
 *              - i.e. <em>the annotator should not assume the output layer is empty</em>) </li>
 *       </ol>
 *  </li>
 *  <li> If there is an  <a href="package-summary.html#ext"> extensions web-app</a> the
 *       user may visit it to access data, visualizations, etc.</li>
 *  <li> The module may be upgraded, in which case:
 *       <ol>
 *         <li> {@link #setSchema(Schema)} is invoked. </li>
 *         <li> {@link #getConfig()} is invoked, to determine the pre-upgrade configuration. </li>
 *         <li> the user is presented with the <a href="package-summary.html#config">
 *              config web-app</a>, if any, and </li> 
 *         <li> {@link #setConfig(String)} is invoked to allow any upgrade processing to
 *              occur, e.g. updating configuration, changes in data schema, etc. </li>
 *       </ol>
 *  </li>
 *  <li> The module is uninstalled, in which case {@link #uninstall()} is invoked, which
 *       should remove all persistent data on the system - e.g. DROP database tables,
 *       delete data files, etc. </li>
 * </ol>
 * <p> The methods below marked in <b> bold </b> are those that an Annotator subclass
 * should implement, in addition to {@link IGraphTransformer#transform(Graph) transform(graph)}.
 * @author Robert Fromont robert@fromont.net.nz
 */
public abstract class Annotator implements GraphTransformer, MonitorableTask {
  
  /**
   * Unique name for the annotator, which is immutable across versions of the implemetantation.
   * <p> The default implementation returns the (simple) name of the annotator
   * implementation class. 
   * @return The annotator's ID.
   */
  public String getAnnotatorId() {
    return getClass().getSimpleName();
  }
  
  /**
   * Version of this implementation; versions will typically be numeric, but this is not
   * a requirement.
   * <p> The default implementation assumes that the .jar file the annotator is deployed
   * in has a 'comment' defined which contains the version (e.g. <q>20200708.1646</q>).
   * <p> If this is not the case, or some other versioning scheme is desired, the derived
   * class should override this method.
   * @return Annotator version.
   */
  public String getVersion() {
    return getClass().getPackage().getImplementationVersion();
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
   * <p> This is automatically called if the annotator is annotated by {@link UsesFileSystem},
   * providing the implementation with persistent access to the file system.
   * @return A persistent directory in which files can be saved and accessed.
   */
  public File getWorkingDirectory() { return workingDirectory; }
  /**
   * Setter for {@link #workingDirectory}: A persistent directory in which files can be saved
   * and accessed. 
   * @param directory A persistent directory in which files can be saved and accessed.
   */
  public Annotator setWorkingDirectory(File directory) { 
    workingDirectory = directory;
    
    // load configuration, if any
    File f = new File(workingDirectory, getAnnotatorId() + ".cfg");
    if (f.exists()) {
      try {
        beanPropertiesFromQueryString(IO.InputStreamToString(new FileInputStream(f)));
      } catch(IOException exception) {}
    }
    return this;
  }
  
  /** 
   * Allows MySQL-specific constructions to be automatically translated to some other
   * dialect using <code>sqlx.apply(mySqlQuery)</code>
   */
  protected MySQLTranslator sqlx;
  private ConnectionFactory db;
  /**
   * Sets the information required for connecting to the relational database.
   * <p> This is automatically called if the annotator is annotated with
   * {@link UsesRelationalDatabase}, providing the implementation with access to a
   * relational database.
   * @param db Factory for making new connections to the database.
   * @throws SQLException If the annotator can't connect to the given database.
   */
  public void setRdbConnectionFactory(ConnectionFactory db) throws SQLException {      
    this.sqlx = db.newSQLTranslator();
    this.db = db;
  }
  
  /**
   * Get a new connection to the database.
   * @return A connection to the RDBMS.
   * @throws SQLException If a database access error occurs
   */
  public Connection newConnection() throws SQLException {
    return db.newConnection();
  } // end of newConnection()
  
  /**
   * The graph store, if the annotator is annotated with {@link UseseGraphStore}.
   * @see #getStore()
   * @see #setStore(GraphStore)
   */
  protected GraphStore store;
  /**
   * Getter for {@link #store}: The graph store, if the annotator is annotated
   * with {@link UsesGraphStore}. 
   * @return The graph store, if the annotator is annotated with {@link UsesGraphStore}.
   */
  public GraphStore getStore() { return store; }
  /**
   * Setter for {@link #store}: The graph store, if the annotator is annotated
   * with {@link UsesGraphStore}. 
   * @param newStore The graph store, if the annotator is annotated with {@link UsesGraphStore}.
   */
  public Annotator setStore(GraphStore newStore) { store = newStore; return this; }
  
  /**
   * Whether the annotator is currently annotating.
   * @see #getRunning()
   * @see #setRunning(boolen)
   */
  private boolean running = false;
  /**
   * Getter for {@link #running}: Whether the annotator is currently annotating.
   * @return Whether the annotator is currently annotating.
   */
  public boolean getRunning() { return running; }
  /**
   * Setter for {@link #running}: Whether the annotator is currently annotating.
   * @param running Whether the annotator is currently annotating.
   */
  protected Annotator setRunning(boolean running) {
    this.running = running;
    for (Consumer<Boolean> observer : runningObservers) observer.accept(running);
    return this;
  }
  
  /**
   * Listeners for run state.
   * @see #getRunningObservers()
   * @see #setRunningObservers(List<Consumer<Boolean>>)
   */
  protected List<Consumer<Boolean>> runningObservers = new Vector<Consumer<Boolean>>();
  /**
   * Getter for {@link #runningObservers}: Listeners for run state.
   * @return Listeners for run state.
   */
  public List<Consumer<Boolean>> getRunningObservers() { return runningObservers; }
  
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
  public Annotator setStatus(String newStatus) {
    status = newStatus;
    for (Consumer<String> observer : statusObservers) observer.accept(status);
    return this;
  }
  
  /**
   * Listeners for status updates.
   * @see #getStatusObservers()
   */
  protected List<Consumer<String>> statusObservers = new Vector<Consumer<String>>();
  /**
   * Getter for {@link #statusObservers}: Listeners for status updates.
   * @return Listeners for status updates.
   */
  public List<Consumer<String>> getStatusObservers() { return statusObservers; }
  
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
  public Annotator setSchema(Schema newSchema) { schema = newSchema; return this; }
  
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
  public String getConfig() { return null; }
  
  /**
   * <b> Specifies the overall configuration of the annotator, and runs any processing
   * required to install the annotator. </b>
   * <p> This processing is assumed to be synchronous (this method doesn't return until
   * it's complete) and long-running, so the {@link MonitorableTask} methods should
   * provide a way for the caller to monitor/cancel processing - i.e. the Annotator class
   * should provide an indication of progress by calling
   * {@link Annotator#setPercentComplete(Integer)} and should regularly check 
   * {@link Annotator#isCancelling()} to determine if installation should be stopped.
   * <p> If the user should provide information before this method is called, a 
   * <tt> config </tt> web-app must be provided to implement the user interface, which sets
   * any required configuration by invoking methods of the annotator as required, and
   * invoking <tt> setConfig </tt> when configuration is ready.
   * <p> If the configuration needs to be persistent between installing the annotator the
   * first time and subsequently upgrading it, then it is the annotator's responsibility
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
   * the name of an object property) that are declared by the annotator class. Getters
   * are called and the property names and their values concatenated into an HTTP query
   * string. 
   * <p> e.g. if the annotator object has:
   * <ul>
   *  <li> a method called <code>getFoo()</code> which returns the value <q>bar</q>, and</li> 
   *  <li> a method called <code>getBar()</code> which returns the value <q>fubar</q>,</li> 
   * </ul>
   * &hellip; then this method will return <q>foo=bar&amp;bar=fubar</q>
   * @return An HTTP query string representing the state of the annotator object.
   */
  protected String beanPropertiesToQueryString() {
    StringBuilder query = new StringBuilder();
    for (Method method : getClass().getDeclaredMethods()) {
      if (method.getName().equals("getAnnotatorId")
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
   *  <li> the annotation class's <code>setFoo</code> would be called with the value
   *       <q>bar</q>, and</li> 
   *  <li> the annotation class's <code>setBar</code> would be called with the value
   *       <q>fubar</q>.</li> 
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
      Class annotatorClass = getClass();
      try {
        Method setter = annotatorClass.getMethod(
          "set" + property.substring(0,1).toUpperCase() + property.substring(1),
          String.class);
        setter.invoke(this, URLDecoder.decode(valueString, "UTF-8"));
      } catch(Throwable notString) {
        try {
          Method setter = annotatorClass.getMethod(
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
            Method setter = annotatorClass.getMethod(
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
              Method setter = annotatorClass.getMethod(
                "set" + property.substring(0,1).toUpperCase() + property.substring(1),
                Integer.class);
              setter.invoke(this, Integer.valueOf(valueString));            
            } catch(Throwable notInteger) {
              try {
                Method setter = annotatorClass.getMethod(
                  "set" + property.substring(0,1).toUpperCase() + property.substring(1),
                  int.class);
                setter.invoke(this, Integer.valueOf(valueString));            
              } catch(Throwable notInt) {
                try {
                  Method setter = annotatorClass.getMethod(
                    "set"
                    + property.substring(0,1).toUpperCase() + property.substring(1),
                    Double.class);
                  setter.invoke(this, Double.valueOf(valueString));            
                } catch(Throwable notDouble) {
                  try {
                    Method setter = annotatorClass.getMethod(
                      "set"
                      + property.substring(0,1).toUpperCase() + property.substring(1),
                      double.class);
                    setter.invoke(this, Double.valueOf(valueString));            
                  } catch(Throwable notD) {
                    try {
                      Method setter = annotatorClass.getMethod(
                        "set"
                        + property.substring(0,1).toUpperCase()
                        + property.substring(1),
                        Float.class);
                      setter.invoke(this, Float.valueOf(valueString));            
                    } catch(Throwable notFloat) {
                      try {
                        Method setter = annotatorClass.getMethod(
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
   * Converts a JSON string to bean properties.
   * <p> This utility method parses the given JSON string (most likely the body of a
   * POST request generated by an HTML form implemented by <i>config/index.html</i>), and
   * calls setters on this object that are named after the top-level string attributes found.
   * <p> e.g. if <var> jsonString </var> is
   * <q>{"foo":"bar","bar":"1","complex": {"objects": "ignored"},"nonStringValue":false}</q> then 
   * <ul>
   *  <li> the annotation class's <code>setFoo</code> would be called with the value
   *       <q>bar</q>, and</li> 
   *  <li> the annotation class's <code>setBar</code> would be called with the value
   *       <q>1</q>.</li> 
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
        Class annotatorClass = getClass();
        try {
          Method setter = annotatorClass.getMethod(
            "set" + property.substring(0,1).toUpperCase() + property.substring(1),
            String.class);
          setter.invoke(this, valueString);
        } catch(Throwable notString) {
          try {
            Method setter = annotatorClass.getMethod(
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
              Method setter = annotatorClass.getMethod(
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
                Method setter = annotatorClass.getMethod(
                  "set" + property.substring(0,1).toUpperCase() + property.substring(1),
                  Integer.class);
                setter.invoke(this, Integer.valueOf(valueString));            
              } catch(Throwable notInteger) {
                try {
                  Method setter = annotatorClass.getMethod(
                    "set" + property.substring(0,1).toUpperCase() + property.substring(1),
                    int.class);
                  setter.invoke(this, Integer.valueOf(valueString));            
                } catch(Throwable notInt) {
                  try {
                    Method setter = annotatorClass.getMethod(
                      "set"
                      + property.substring(0,1).toUpperCase() + property.substring(1),
                      Double.class);
                    setter.invoke(this, Double.valueOf(valueString));            
                  } catch(Throwable notDouble) {
                    try {
                      Method setter = annotatorClass.getMethod(
                        "set"
                        + property.substring(0,1).toUpperCase() + property.substring(1),
                        double.class);
                      setter.invoke(this, Double.valueOf(valueString));            
                    } catch(Throwable notD) {
                      try {
                        Method setter = annotatorClass.getMethod(
                          "set"
                          + property.substring(0,1).toUpperCase()
                          + property.substring(1),
                          Float.class);
                        setter.invoke(this, Float.valueOf(valueString));            
                      } catch(Throwable notFloat) {
                        try {
                          Method setter = annotatorClass.getMethod(
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
   * <b> Runs any processing required to uninstall the annotator. </b>
   */
  public void uninstall() { }
  
  /**
   * <b> Sets the configuration for a given annotation task. </b>
   * <p> If the user should provide information before an annotation task is run for the
   * first time, a <tt> task </tt> web-app must be provided to implement the user
   * interface, which may be provided with an existing configurtaion, and invoking
   * <tt> setTaskParameters </tt> when ready.
   * <p> If the Annotator performs exclusively (or mainly) one annotation task only
   * (e.g. the PorterStemmer) then calling this method with 
   * <code> <var> parameters </var> = null </code> should apply any default parameters so
   * that the Annotator can be used without invoking the <tt> task </tt> web-app.
   * @param parameters The configuration of the annotator, encoded in a String using
   * whatever mechanism is preferred (serialization of Properties object, JSON, etc.)
   * @throws InvalidConfigurationException
   */
  public abstract void setTaskParameters(String parameters) throws InvalidConfigurationException;
  
  /**
   * <b> Determines which layers the annotator requires in order to annotate a graph. </b>
   * @return A list of layer IDs.
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public abstract String[] getRequiredLayers() throws InvalidConfigurationException;
  
  /**
   * <b> Determines which layers the annotator will create/update/delete annotations on. </b>
   * @return A list of layer IDs.
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public abstract String[] getOutputLayers() throws InvalidConfigurationException;
  
  private boolean ignoreSetPercentComplete = false;
  private Integer percentComplete;
  /**
   * Setter for {@link #percentComplete}: Progress indicator; set to 100 when processing
   * is complete. 
   * @param newPercentComplete Progress indicator; set to 100 when processing is complete.
   */
  protected Annotator setPercentComplete(Integer newPercentComplete) {
    if (!ignoreSetPercentComplete) {
      percentComplete = newPercentComplete;
      for (Consumer<Integer> observer : percentCompleteObservers) observer.accept(percentComplete);
    }
    return this;
  }
  /**
   * Getter for {@link #percentComplete}: Progress indicator; set to 100 when processing
   * is complete. 
   * @return Progress indicator; set to 100 when processing is complete.
   */
  public Integer getPercentComplete() { return percentComplete; }
  
  /**
   * Listeners for progress updates.
   * @see #getPercentCompleteObservers()
   */
  protected List<Consumer<Integer>> percentCompleteObservers = new Vector<Consumer<Integer>>();
  /**
   * Getter for {@link #percentCompleteObservers}: Listeners for progress updates.
   * @return Listeners for progress updates.
   */
  public List<Consumer<Integer>> getPercentCompleteObservers() { return percentCompleteObservers; }
  
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
    for (Consumer<Boolean> observer : cancellationObservers) observer.accept(cancelling);
  }

  /**
   * Listeners for cancellation.
   * @see #getCancellationObservers()
   * @see #setCancellationObservers(List<Consumer<Boolean>>)
   */
  protected List<Consumer<Boolean>> cancellationObservers = new Vector<Consumer<Boolean>>();
  /**
   * Getter for {@link #cancellationObservers}: Listeners for cancellation.
   * @return Listeners for cancellation.
   */
  public List<Consumer<Boolean>> getCancellationObservers() { return cancellationObservers; }
  /**
   * Setter for {@link #cancellationObservers}: Listeners for cancellation.
   * @param newCancellationObservers Listeners for cancellation.
   */
  public Annotator setCancellationObservers(List<Consumer<Boolean>> newCancellationObservers) { cancellationObservers = newCancellationObservers; return this; }
  
  /**
   * Transforms all graphs from the given graph store that match the given graph
   * expression.
   * <p> This can be overridden for optimized cross-graph updates. The default
   * implementation simply calls {@link IGraphStoreQuery#getMatchingTranscriptIds(String)}
   * and calls {@link IGraphStoreQuery#getGraph(String,String[])} and 
   * {@link GraphTransformer#transform(Graph)} serially for each returned ID.
   * @param store
   * @param expression
   * @throws TransformationException
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   * @throws StoreException If one of the <var> store </var> methods throws this exception.
   * @throws PermissionException If one of the <var> store </var> methods throws this exception.
   */
  public void transformTranscripts(GraphStore store, String expression)
    throws TransformationException, InvalidConfigurationException, StoreException,
    PermissionException {
    
    Vector<String> layerIds = new Vector<String>();
    for (String layerId : getRequiredLayers()) layerIds.add(layerId);
    for (String layerId : getOutputLayers()) layerIds.add(layerId);
    String[] ids = store.getMatchingTranscriptIds(expression);
    ignoreSetPercentComplete = true; // global progress
    percentComplete = 0;
    try {
      StoreException transcriptException = null;
      int soFar = 0;
      for (String id : ids) {
        try {
          if (cancelling) break;
          Graph graph = store.getTranscript(id, layerIds.toArray(new String[0]));
          if (cancelling) break;
          transform(graph);
          if (cancelling) break;
          store.saveTranscript(graph);
          percentComplete = (int)((double)(soFar * 100) / (double)ids.length);
        } catch (StoreException storeX) {
          // we don't let a single transcript's problem stop all the others from
          // being annotated, but we save the exception for throwing later               
          if (transcriptException == null) transcriptException = storeX;
        } catch (GraphNotFoundException extremelyUnlikely) {
          // we just got the ID from the store, so this is pretty unlikely, and ignorable
          System.err.println("Annotator.transformTranscripts: " + extremelyUnlikely);
        }
      } // next transcript
      percentComplete = 100;
      if (transcriptException != null) throw transcriptException;
    } finally {
      ignoreSetPercentComplete = false;
    }
  } // end of transformTranscripts()
  
  /**
   * Transforms all fragments from the given stream.
   * <p> This can be overridden for optimized cross-graph updates. The default
   * implementation simply calls {@link GraphTransformer#transform(Graph)} and then
   * consumer.accept(Graph) serially for each graph. 
   * @param fragments A stream of graphs which must be transcript fragments, e.g. utterances.
   * @param consumer A consumer for receiving the graphs once they're transformed.
   * @throws TransformationException
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public void transformFragments(Stream<Graph> fragments, Consumer<Graph> consumer)
    throws TransformationException, InvalidConfigurationException {
    
    List<Graph> transcripts = fragments.collect(Collectors.toList());
    ignoreSetPercentComplete = true; // global progress
    percentComplete = 0;
    try {
      int soFar = 0;
      for (Graph transcript : transcripts) {
        if (cancelling) break;
        transform(transcript);
        consumer.accept(transcript);
        if (cancelling) break;
        percentComplete = (int)((double)(soFar * 100) / (double)transcripts.size());
      } // next transcript
      percentComplete = 100;
    } finally {
      ignoreSetPercentComplete = false;
    }
  } // end of transformFragments()
  
} // end of class Annotator
