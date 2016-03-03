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
package nzilbb.ag.util;

import java.util.Vector;
import java.util.HashMap;
import java.util.regex.*;
import nzilbb.ag.*;

/**
 * Transforms a text convention on a source layer into annotations on destination layers.
 * <p>Annotations on {@link #sourceLayerId} are scanned, and where a label matches the {@link #sourcePattern} regular expression, annotations are added (or modified, in the case of the source layer) on the layers specified by the keys of {@link #destinationResults}. The values of this collection are used as the labels for annotations added on the corresponding layers. These values can contain groups captured in {@link #sourcePattern}, in which case the corresponding group content is substituted into the label.
 * <p>Some examples:
 * <p>To convert words in the format <var>orthography</var>_<var>pos</var> into words with POS tags:
 * <ul>
 *  <li><b>sourceLayerId</b>: <samp>word</samp></li>
 *  <li><b>sourcePattern</b>: <samp>(.+)_(.+)</samp></li>
 *  <li><b>destinationResults</b>:
 *    <ul>
 *      <li>"word" = "$1"</li>
 *      <li>"pos" = "$2"</li>
 *    </ul>
 *  </li>
 * </ul>
 * So a word labelled "the_DT" will end up being labelled "the" and tagged with "DT" on the "pos" layer.
 * <p>To convert words prepended with a disfluency marker of <samp>&amp;</samp> to words with the marker stripped, and tagged with <samp>DIS</samp> on the "disfluency" layer:
 * <ul>
 *  <li><b>sourceLayerId</b>: <samp>word</samp></li>
 *  <li><b>sourcePattern</b>: <samp>&amp;(.+)</samp></li>
 *  <li><b>destinationResults</b>:
 *    <ul>
 *      <li>"word" = "$1"</li>
 *      <li>"disfluency" = "DIS"</li>
 *    </ul>
 *  </li>
 * </ul>
 * So a word labelled "&amp;th" will end up being labelled "th" and tagged with "DIS" on the "disfluency" layer.
 * <p>To convert words in square brackets into noise annotations:
 * <ul>
 *  <li><b>sourceLayerId</b>: <samp>word</samp></li>
 *  <li><b>sourcePattern</b>: <samp>&amp;\[(.+)\]</samp></li>
 *  <li><b>destinationResults</b>:
 *    <ul>
 *      <li>"noise" = "$1"</li>
 *    </ul>
 *  </li>
 * </ul>
 * So a word labelled "[coughs]" will end up being deleted, replaced by an annotation labelled "coughs" on the "noise" layer.
 * Note that in this case, the {@link #destinationResults} contains no key for the source layer, so the source annotation is deleted.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class ConventionTransformer
  implements IGraphTransformer
{
   // Attributes:
   
   /**
    * Layer ID of the annotations to transform.
    * @see #getSourceLayerId()
    * @see #setSourceLayerId(String)
    */
   protected String sourceLayerId;
   /**
    * Getter for {@link #sourceLayerId}: Layer ID of the annotations to transform.
    * @return Layer ID of the annotations to transform.
    */
   public String getSourceLayerId() { return sourceLayerId; }
   /**
    * Setter for {@link #sourceLayerId}: Layer ID of the annotations to transform.
    * @param newSourceLayerId Layer ID of the annotations to transform.
    */
   public void setSourceLayerId(String newSourceLayerId) { sourceLayerId = newSourceLayerId; }

   /**
    * Regular expression in the source layer which triggers transformation of the annotation. This may capture groups, which can be copied into the destination or source layers.
    * @see #getSourcePattern()
    * @see #setSourcePattern(String)
    */
   protected String sourcePattern;
   /**
    * Getter for {@link #sourcePattern}: Regular expression in the source layer which triggers transformation of the annotation. This may capture groups, which can be copied into the destination or source layers.
    * @return Regular expression in the source layer which triggers transformation of the annotation. This may capture groups, which can be copied into the destination or source layers.
    */
   public String getSourcePattern() { return sourcePattern; }
   /**
    * Setter for {@link #sourcePattern}: Regular expression in the source layer which triggers transformation of the annotation. This may capture groups, which can be copied into the destination or source layers.
    * @param newSourcePattern Regular expression in the source layer which triggers transformation of the annotation. This may capture groups, which can be copied into the destination or source layers.
    */
   public void setSourcePattern(String newSourcePattern) { sourcePattern = newSourcePattern; }


   /**
    * A map of layer IDs to label values which may include references to groups captured in the {@link #sourcePattern}.  If there is no key for {@link #sourceLayerId} then the matching annotation will be deleted.  If there is, then the label of the matching will be changed (unless the value is "\0", i.e. the whole source label, in which case the matching annotation is left unchanged).
    * @see #getDestinationResults()
    * @see #setDestinationResults(HashMap)
    */
   protected HashMap<String,String> destinationResults;
   /**
    * Getter for {@link #DestinationResults}: A map of layer IDs to label values which may include references to groups captured in the {@link #sourcePattern}.
    * @return A map of layer IDs to label values which may include references to groups captured in the {@link #sourcePattern}.
    */
   public HashMap<String,String> getDestinationResults() { return destinationResults; }
   /**
    * Setter for {@link #DestinationResults}: A map of layer IDs to label values which may include references to groups captured in the {@link #sourcePattern}.
    * @param newDestinationResults A map of layer IDs to label values which may include references to groups captured in the {@link #sourcePattern}.
    */
   public void setDestinationResults(HashMap<String,String> newDestinationResults) { destinationResults = newDestinationResults; }
   

   // Methods:
   
   /**
    * Default constructor.
    */
   public ConventionTransformer()
   {
      setDestinationResults(new HashMap<String,String>());
   } // end of constructor

   /**
    * Constructor from attribute values.
    * @param sourceLayerId Layer ID of the annotations to transform.
    * @param sourcePattern Regular expression in the source layer which triggers transformation of the annotation. This may capture groups, which can be copied into the destination or source layers.
    * @param destinationResults A map of layer IDs to label values which may include references to groups captured in the {@link #sourcePattern}.
    */
   public ConventionTransformer(String sourceLayerId, String sourcePattern, HashMap<String,String> destinationResults)
   {
      setSourceLayerId(sourceLayerId);
      setSourcePattern(sourcePattern);
      setDestinationResults(destinationResults);
   } // end of constructor

   /**
    * Constructor from attribute values. Destination results must be subsequently added using {@link #setDestinationResults(HashMap)} or {@link #addResult(String,String)}.
    * @param sourceLayerId Layer ID of the annotations to transform.
    * @param sourcePattern Regular expression in the source layer which triggers transformation of the annotation. This may capture groups, which can be copied into the destination or source layers.
    */
   public ConventionTransformer(String sourceLayerId, String sourcePattern)
   {
      setSourceLayerId(sourceLayerId);
      setSourcePattern(sourcePattern);
      setDestinationResults(new HashMap<String,String>());
   } // end of constructor

   /**
    * Utility constructor for the common scenario of identifying a pattern on one layer and, where it occurs, changing the label on the source label and adding an annotation on a second layer.
    * <p>For example, to tag disfluencies marked with a leading <samp>&amp;</samp> with a label <samp>DIS</samp>: 
    * <code>new ConventionTransformer("word", "&amp;(.+)", "\\1", "disfluency", "DIS")</code>
    * <br>...which strips the word annotation of the leading &amp;, and tags the word on the "disfluency" layer.
    * @param sourceLayerId Layer ID of the annotations to transform.
    * @param sourcePattern Regular expression in the source layer which triggers transformation of the annotation. This may capture groups, which can be copied into the destination or source layers.
    * @param sourceResult
    * @param destinationLayerId
    * @param destinationResult
    */
   public ConventionTransformer(String sourceLayerId, String sourcePattern, String sourceResult, String destinationLayerId, String destinationResult)
   {
      setSourceLayerId(sourceLayerId);
      setSourcePattern(sourcePattern);
      HashMap<String,String> destinationResults = new HashMap<String,String>();
      destinationResults.put(sourceLayerId, sourceResult);
      destinationResults.put(destinationLayerId, destinationResult);
      setDestinationResults(destinationResults);
   } // end of constructor

   
   /**
    * Add a destination result to {@link #destinationResults}.
    * @param layerId The layer on which the annotation will be added.
    * @param label The label for the destination annotation, which may include groups captured in {@link #sourcePattern}.
    */
   public void addDestinationResult(String layerId, String label)
   {
      getDestinationResults().put(layerId, label);
   } // end of addResult()


   /**
    * Transforms the graph.
    * @param graph The graph to transform.
    * @return The changes introduced by the tranformation.
    * @throws TransformationException If the transformation cannot be completed.
    */
   public Vector<Change> transform(Graph graph) throws TransformationException
   {
      if (graph.getLayer(getSourceLayerId()) == null) 
	 throw new TransformationException(this, "No source layer: " + getSourceLayerId());
      for (String destinationLayerId : getDestinationResults().keySet())
      {
	 if (graph.getLayer(destinationLayerId) == null) 
	    throw new TransformationException(this, "No destination layer: " + destinationLayerId);
      }
      try
      {
	 Pattern sourceRegexp = Pattern.compile(getSourcePattern());
	 Vector<Change> changes = new Vector<Change>();
	 for (Annotation source : graph.getAnnotations(getSourceLayerId()))
	 {
	    Matcher matcher = sourceRegexp.matcher(source.getLabel());
	    if (matcher.matches())
	    {
	       for (String destinationLayerId : getDestinationResults().keySet())
	       {
		  String result = getDestinationResults().get(destinationLayerId);
		  if (destinationLayerId.equals(getSourceLayerId()))
		  {
		     if (!result.equals("$0"))
		     {
			String label = matcher.replaceAll(result);
			// check it's really a change
			if (!label.equals(source.getLabel()))
			{
			   source.setLabel(label);
			   changes.add(source.getLastChange());
			}
		     }
		  }
		  else
		  {
		     Annotation tag = graph.createTag(source, destinationLayerId, matcher.replaceAll(result));
		     changes.addAll(tag.getChanges());
		  }
	       } // next destination result
	       // if the source layer isn't a destination layer
	       if (!getDestinationResults().containsKey(getSourceLayerId()))
	       { // delete the source annotation
		  source.destroy();
	       }
	    } // label matches
	 } // next source annotation
	 return changes;
      }
      catch(PatternSyntaxException exception)
      {
	 throw new TransformationException(this, exception);
      }
   }
} // end of class ConventionTransformer
