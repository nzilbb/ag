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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import nzilbb.util.MonitorableTask;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.*;

/**
 * This is an example annotator which implements all possible features of an annotator.
 * <p> The annotator:
 * <ul>
 *  <li>Includes a <i> conf </i> web-app</li>
 *  <li>Includes a <i> task </i> web-app</li>
 * </ul>
 */
public class TheWorksExample extends Annotator {

   /**
    * This class implements its own version of {@link Annotator#getAnnotatorId()} which is
    * not necessary unless there's a class-name clash with some other annotator.
    * <p> If such a class exists, you would need this method to return something other
    * than <code>getClass().getSimpleName()</code>.
    * @return The annotator's ID.
    */
   public String getAnnotatorId() {
      return getClass().getSimpleName();
   }

   /**
    * Version of this implementation. 
    * @return Annotator version.
    */
   public String getVersion() { return "0.1"; }

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
   public String getConfig() { return beanPropertiesToQueryString(); }
   
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
   public void setConfig(String config) throws InvalidConfigurationException {
      beanPropertiesFromQueryString(config);
      if (simulatedInstallationDuration != null && simulatedInstallationDuration >= 0) {
         for (int p = 0; p < 100; p += 10) {
            setPercentComplete(p);
            try {
               Thread.sleep(simulatedInstallationDuration * 100);
            } catch(Exception exception) {}
         }
      } // simulate a long installation
      setPercentComplete(100);
   }
   
   /**
    * Sets the configuration for a given annotation task.
    * @param parameters The configuration of the annotator; This annotator has no task
    * parmeter web-app, so <var> parameters </var> will always be null.
    * @throws InvalidConfigurationException
    */
   public void setTaskParameters(String parameters) throws InvalidConfigurationException { }

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
      if (schema.getWordLayerId() == null)
         throw new InvalidConfigurationException(this, "Schema has no word layer.");
      String[] layers = { schema.getWordLayerId() };
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
   public String[] getOutputLayers() throws InvalidConfigurationException {
      return new String[0];
   }
   
   /**
    * Transforms the graph. In this case, the graph is simply summarized, by counting all
    * tokens of each word type, and printing out the result to stdout.
    * @param graph The graph to transform.
    * @return The changes introduced by the tranformation.
    * @throws TransformationException If the transformation cannot be completed.
    */
   public List<Change> transform(Graph graph) throws TransformationException {
      if (schema == null)
         throw new InvalidConfigurationException(this, "Schema is not set.");
      if (schema.getWordLayerId() == null)
         throw new InvalidConfigurationException(this, "Schema has no word layer.");

      // maintain a list of counts
      TreeMap<String,Integer> typesCounts = new TreeMap<String,Integer>();

      // count tokens of each type
      for (Annotation token : graph.list(schema.getWordLayerId())) {
         if (typesCounts.containsKey(token.getLabel())) {
            typesCounts.put(token.getLabel(), 1);
         } else {
            typesCounts.put(token.getLabel(), typesCounts.get(token.getLabel()) + 1);
         }
      } // next token

      // print out the results
      System.out.println(graph.getId());
      for (String type : typesCounts.keySet()) {
         System.out.println(type + "\t" + typesCounts.get(type) );
      } // next type
      
      return new Vector<Change>();
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
}
