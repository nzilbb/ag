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
    * Unique name for the annotator, which is immutable across versions of the implemetantation.
    * @return The annotator's ID.
    */
   public String getAnnotatorId() { return getClass().getSimpleName(); }

   /**
    * Version of this implementation. 
    * @return Annotator version.
    */
   public String getVersion() { return "0.1"; }
   
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
    * @see #setRightPadding(int)
    */
   protected int rightPadding = 2;
   /**
    * Getter for {@link #rightPadding}: How many spaces to add on the right.
    * @return How many spaces to add on the right.
    */
   public int getRightPadding() { return rightPadding; }
   /**
    * Setter for {@link #rightPadding}: How many spaces to add on the right.
    * @param newRightPadding How many spaces to add on the right.
    */
   public TheWorksExample setRightPadding(int newRightPadding) { rightPadding = newRightPadding; return this; }
   
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
}
