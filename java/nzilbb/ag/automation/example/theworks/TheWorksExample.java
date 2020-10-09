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
package nzilbb.ag.automation.example.theworks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.Dictionary;
import nzilbb.ag.automation.DictionaryException;
import nzilbb.ag.automation.ImplementsDictionaries;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.UsesRelationalDatabase;
import nzilbb.sql.ConnectionFactory;
import nzilbb.sql.mysql.MySQLTranslator;
import nzilbb.util.IO;
import nzilbb.util.MonitorableTask; // for javadoc

/**
 * This is an example annotator which implements all possible features of an annotator.
 * <p> The annotator:
 * <ul>
 *  <li>Includes a <i> conf </i> web-app</li>
 *  <li>Includes a <i> task </i> web-app</li>
 * </ul>
 */
@UsesFileSystem
@UsesRelationalDatabase
public class TheWorksExample extends Annotator
   implements ImplementsDictionaries {
   /** Get the minimum version of the nzilbb.ag API supported by the serializer.*/
   @Override public String getMinimumApiVersion() { return "20200708.2018"; }

   /**
    * This class implements its own version of {@link Annotator#getAnnotatorId()} which is
    * not necessary unless there's a class-name clash with some other annotator.
    * <p> If such a class exists, you would need this method to return something other
    * than <code>getClass().getSimpleName()</code>.
    * @return The annotator's ID.
    */
   @Override public String getAnnotatorId() {
      return getClass().getSimpleName();
   }

   /**
    * Sets the information required for connecting to the relational database.
    * <p> This is automatically called if the annotator is annotated with
    * {@link UsesRelationalDatabase}, providing the implementation with access to a
    * relational database.
    * @param db Factory for making new connections to the database.
    * @throws SQLException If the annotator can't connect to the given database.
    */
   @Override public void setRdbConnectionFactory(ConnectionFactory db) throws SQLException {

      // call the base class version first
      super.setRdbConnectionFactory(db);
      
      // check we can connect
      Connection rdb = newConnection();      
      try {
         
         // check the schema has been created
         try {
            PreparedStatement sql = rdb.prepareStatement(
               sqlx.apply("SELECT COUNT(*) AS theCount FROM "+getAnnotatorId()+"_table"));

            try {
               ResultSet rsCheck = sql.executeQuery();
               rsCheck.close();
            } finally {
               sql.close();
            }
         } catch(SQLException exception) {
            
            PreparedStatement sql = rdb.prepareStatement(
               sqlx.apply(
                  "CREATE TABLE "+getAnnotatorId()+"_table ("
                  +" id varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL"
                  +" COMMENT 'This is a comment on the ID field',"
                  +" a_number smallint NOT NULL"
                  +" COMMENT 'Does smallint work?',"
                  +" PRIMARY KEY (id)"
                  +") ENGINE=MyISAM"));
	    sql.executeUpdate();
	    sql.close();
         }
      } finally {
         try { rdb.close(); } catch(SQLException x) {}
      }      
   }

   /**
    * Runs any processing required to uninstall the annotator.
    * <p> In this case, the table created in rdbConnectionFactory() is DROPped.
    */
   @Override public void uninstall() {
      try {
         Connection rdb = newConnection();      
         try {
            
            // check the schema has been created
            PreparedStatement sql = rdb.prepareStatement(
               sqlx.apply("DROP TABLE "+getAnnotatorId()+"_table"));
            sql.executeUpdate();
	    sql.close();
            
         } finally {
            try { rdb.close(); } catch(SQLException x) {}
         }      
      } catch (SQLException x) {
      }
   }

   /**
    * Provides the overall configuration of the annotator. 
    * <p> This implementation uses {@link Annotator#beanPropertiesToQueryString()} to
    * generate the string from the current bean properties.
    * @return The overall configuration of the annotator, which will be passed to the
    * <i> config/index.html </i> configuration webapp, if any. This configuration may be null, or a
    * string that serializes the annotators configuration state in any encoding the
    * implementor prefers. The resulting string must be interpretable by the
    * <i> config/index.html </i> webapp. 
    * @see #setConfig(String)
    * @see #beanPropertiesToQueryString()
    */
   @Override public String getConfig() { return beanPropertiesToQueryString(); }
   
   /**
    * Specifies the overall configuration of the annotator, and runs any processing
    * required to install the annotator.
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
    * <p> This implementation uses {@link Annotator#beanPropertiesFromQueryString(String)} to
    * configure the bean properties from the query string.
    * @throws InvalidConfigurationException
    * @see #getConfig()
    * @see #beanPropertiesFromQueryString(String)
    */ 
   @Override public void setConfig(String config) throws InvalidConfigurationException {
      beanPropertiesFromQueryString(config);
      if (simulatedInstallationDuration != null && simulatedInstallationDuration >= 0) {
         for (int p = 0; p < 100; p += 10) {
            setPercentComplete(p);
            try {
               Thread.sleep(simulatedInstallationDuration * 100);
            } catch(Exception exception) {}
         }
      } // simulate a long installation

      // persist the configuration to a file in the working directory
      if (getWorkingDirectory() != null) {
         try {
            File f = new File(getWorkingDirectory(), getAnnotatorId() + ".cfg");
            FileWriter out = new FileWriter(f);
            out.write(config);
            out.close();
         } catch(IOException exception) {
            System.err.println(""+exception);
         }
      }
      
      setPercentComplete(100);
   }

   /**
    * Sets the configuration for a given annotation task.
    * @param parameters The configuration of the annotator; This annotator has no task
    * parmeter web-app, so <var> parameters </var> will always be null.
    * @throws InvalidConfigurationException
    */
   @Override
   public void setTaskParameters(String parameters) throws InvalidConfigurationException {
      beanPropertiesFromQueryString(parameters);
      
      // does the outputLayer need to be added to the schema?
      if (schema.getLayer(outputLayer) == null) {
         schema.addLayer(
            new Layer(outputLayer)
            .setAlignment(Constants.ALIGNMENT_NONE)
            .setPeers(false)
            .setParentId(schema.getWordLayerId()));
      }
   }
   
   /**
    * ID of input layer.
    * @see #getInputLayer()
    * @see #setInputLayer(String)
    */
   protected String inputLayer;
   /**
    * Getter for {@link #inputLayer}: ID of input layer.
    * @return ID of input layer.
    */
   public String getInputLayer() { return inputLayer; }
   /**
    * Setter for {@link #inputLayer}: ID of input layer.
    * @param newInputLayer ID of input layer.
    */
   public TheWorksExample setInputLayer(String newInputLayer) { inputLayer = newInputLayer; return this; }

   /**
    * ID of output layer.
    * @see #getOutputLayer()
    * @see #setOutputLayer(String)
    */
   protected String outputLayer;
   /**
    * Getter for {@link #outputLayer}: ID of output layer.
    * @return ID of output layer.
    */
   public String getOutputLayer() { return outputLayer; }
   /**
    * Setter for {@link #outputLayer}: ID of output layer.
    * @param newOutputLayer ID of output layer.
    */
   public TheWorksExample setOutputLayer(String newOutputLayer) { outputLayer = newOutputLayer; return this; }

   /**
    * Determines which layers the annotator requires in order to annotate a graph.
    * @return A list of layer IDs. In this case, the annotator only requires the schema's
    * word layer.
    * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
    * {@link #setSchema(Schema)} have not yet been called.
    */
   @Override 
   public String[] getRequiredLayers() throws InvalidConfigurationException {
      if (schema == null)
         throw new InvalidConfigurationException(this, "Schema is not set.");
      if (inputLayer == null)
         throw new InvalidConfigurationException(this, "No input layer set.");
      String[] layers = { inputLayer };
      return layers;
   }
   
   /**
    * Determines which layers the annotator will create/update/delete annotations on.
    * @return A list of layer IDs. In this case, the annotator has no task web-app for
    * specifying an output layer, and doesn't update any layers, so this method returns an
    * empty array.
    * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
    * {@link #setSchema(Schema)} have not yet been called.
    */
   @Override 
   public String[] getOutputLayers() throws InvalidConfigurationException {
      if (outputLayer == null)
         throw new InvalidConfigurationException(this, "No output layer set.");
      String[] layers = { outputLayer };
      return layers;
   }

   protected TheWorksExampleDictionary frequencies;
   
   /**
    * Transforms the graph. In this case, the graph is simply summarized, by counting all
    * tokens of each word type, and printing out the result to stdout.
    * @param graph The graph to transform.
    * @return The changes introduced by the tranformation.
    * @throws TransformationException If the transformation cannot be completed.
    */
   @Override 
   public Graph transform(Graph graph) throws TransformationException {
      if (schema == null)
         throw new InvalidConfigurationException(this, "Schema is not set.");
      if (schema.getWordLayerId() == null)
         throw new InvalidConfigurationException(this, "Schema has no word layer.");

      running = true;

      try { 
         // maintain a list of counts
         frequencies = new TheWorksExampleDictionary(this);
         
         // count tokens of each type
         for (Annotation token : graph.all(schema.getWordLayerId())) {
            if (frequencies.containsKey(token.getLabel())) {
               frequencies.put(token.getLabel(), 1);
            } else {
               frequencies.put(token.getLabel(), frequencies.get(token.getLabel()) + 1);
            }
         } // next token
         
         // print out the results
         System.out.println(graph.getId());
         for (String type : frequencies.keySet()) {
            System.out.println(type + "\t" + frequencies.get(type) );
         } // next type
      
         return graph;
      } finally {
         running = false;
      }
   }
   
   /**
    * Reverse annotation labels setting.
    * @see #getReverse()
    * @see #setReverse(Boolean)
    */
   protected Boolean reverse;
   /**
    * Getter for {@link #reverse}: Reverse annotation labels setting.
    * @return Reverse annotation labels setting.
    */
   public Boolean getReverse() { return reverse; }
   /**
    * Setter for {@link #reverse}: Reverse annotation labels setting.
    * @param newReverse Reverse annotation labels setting.
    */
   public void setReverse(Boolean newReverse) { reverse = newReverse; }

   /**
    * How many spaces to add on the left.
    * @see #getLeftPadding()
    * @see #setLeftPadding(int)
    */
   protected int leftPadding = 1;
   /**
    * Getter for {@link #leftPadding}: How many spaces to add on the left.
    * @return How many spaces to add on the left.
    */
   public int getLeftPadding() { return leftPadding; }
   /**
    * Setter for {@link #leftPadding}: How many spaces to add on the left.
    * @param newLeftPadding How many spaces to add on the left.
    */
   public TheWorksExample setLeftPadding(int newLeftPadding) { leftPadding = newLeftPadding; return this; }

   /**
    * How many spaces to add on the right.
    * @see #getRightPadding()
    * @see #setRightPadding(Integer)
    */
   protected Integer rightPadding = Integer.valueOf(2);
   /**
    * Getter for {@link #rightPadding}: How many spaces to add on the right.
    * @return How many spaces to add on the right.
    */
   public Integer getRightPadding() { return rightPadding; }
   /**
    * Setter for {@link #rightPadding}: How many spaces to add on the right.
    * @param newRightPadding How many spaces to add on the right.
    */
   public TheWorksExample setRightPadding(Integer newRightPadding) { rightPadding = newRightPadding; return this; }
   
   /**
    * Sets the padding.
    * @param leftPadding
    * @param rightPadding
    */
   public void setPadding(int leftPadding, int rightPadding)
   {
      setLeftPadding(leftPadding);
      setRightPadding(rightPadding);
   } // end of setPadding()

   /**
    * Prefix to add.
    * @see #getPrefix()
    * @see #setPrefix(String)
    */
   protected String prefix;
   /**
    * Getter for {@link #prefix}: Prefix to add.
    * @return Prefix to add.
    */
   public String getPrefix() { return prefix; }
   /**
    * Setter for {@link #prefix}: Prefix to add.
    * @param newPrefix Prefix to add.
    */
   public TheWorksExample setPrefix(String newPrefix) { prefix = newPrefix; return this; }
   
   /**
    * Value for annotator confidence.
    * @see #getLabelConfidence()
    * @see #setLabelConfidence(Double)
    */
   protected Double labelConfidence;
   /**
    * Getter for {@link #labelConfidence}: Value for annotator confidence.
    * @return Value for annotator confidence.
    */
   public Double getLabelConfidence() { return labelConfidence; }
   /**
    * Setter for {@link #labelConfidence}: Value for annotator confidence.
    * @param newLabelConfidence Value for annotator confidence.
    */
   public TheWorksExample setLabelConfidence(Double newLabelConfidence) { labelConfidence = newLabelConfidence; return this; }

   /**
    * Accepts an uploaded file.
    * @param file The file uploaded by the webapp.
    * @return null if upload was successful, an error message otherwise.
    */
   public String uploadFile(File file) {
      // file is a temporary file that will be deleted after this method finishes,
      // so we must copy it elsewhere or process the contents now
      return null;
   } // end of uploadLexicon()
   
   /**
    * How long, in seconds, the #setConfig(String) method should take to return.
    * @see #getSimulatedInstallationDuration()
    * @see #setSimulatedInstallationDuration(Integer)
    */
   protected Integer simulatedInstallationDuration;
   /**
    * Getter for {@link #simulatedInstallationDuration}: How long, in seconds, the
    * #setConfig(String) method should take to return. 
    * @return How long, in seconds, the #setConfig(String) method should take to return.
    */
   public Integer getSimulatedInstallationDuration() { return simulatedInstallationDuration; }
   /**
    * Setter for {@link #simulatedInstallationDuration}: How long, in seconds, the
    * #setConfig(String) method should take to return. 
    * @param newSimulatedInstallationDuration How long, in seconds, the #setConfig(String)
    * method should take to return. 
    */
   public TheWorksExample setSimulatedInstallationDuration(Integer newSimulatedInstallationDuration) { simulatedInstallationDuration = newSimulatedInstallationDuration; return this; }

   /**
    * Lists the dictionaries implemented by this Annotator.
    * <p> This method can assume that the following methods have been previously called:
    * <ul>
    *  <li> {@link Annotator#setSchema(Schema)} </li>
    *  <li> {@link Annotator#setTaskParameters(String)} </li>
    *  <li> {@link Annotator#setWorkingDirectory(File)} (if applicable) </li>
    *  <li> {@link Annotator#setRdbConnectionFactory(ConnectionFactory)}
    *       (if applicable) </li>
    * </ul>
    * @return A (possibly empty) list of IDs of dictionaries.
    */
   public List<String> getDictionaryIds() {
      return new Vector<String>() {{ add("frequencies"); }};
   }

   /**
    * Gets the identified dictionary.
    * <p> This method can assume that the following methods have been previously called:
    * <ul>
    *  <li> {@link Annotator#setSchema(Schema)} </li>
    *  <li> {@link Annotator#setTaskParameters(String)} </li>
    *  <li> {@link Annotator#setWorkingDirectory(File)} (if applicable) </li>
    *  <li> {@link Annotator#setRdbConnectionFactory(ConnectionFactory)}
    *       (if applicable) </li>
    * </ul>
    * @return The identified dictionary.
    * @throws DictionaryException If the given dictionary doesn't exist.
    */
   public Dictionary getDictionary(String id) throws DictionaryException {
      if (!"frequencies".equals(id)) {
         throw new DictionaryException(null, "Invalid dictionary: " + id);
      }
      return frequencies;
   }
}
