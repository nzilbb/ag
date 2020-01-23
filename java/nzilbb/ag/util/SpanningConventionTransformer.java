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
import java.util.LinkedHashSet;
import java.util.regex.*;
import nzilbb.ag.*;

/**
 * Transforms a text convention that spans possibly multiple annotations on a source layer
 * into annotations on a destination layer. 
 * <p>Annotations on {@link #sourceLayerId} are scanned, and where a label matches the
 * {@link #startPattern} regular expression, it and subsequent annotation labels, until a
 * label that matches the {@link #endPattern} regular expression, are concatenated to form
 * the label of an annotation on {@link #destinationLayerId}. During concatenation, the
 * start and end annotations are relabelled using {@link #destinationStartResult} and
 * {@link #destinationEndResult}, respectively. The spanned annotation may or may not be
 * deleted, depending on the value of {@link #deleteInSource}. If they are kept, the start
 * and end annotations are relabelled using {@link #sourceStartResult} and {@link
 * #sourceEndResult}, respectively. 
 * <p>Some examples:
 * <p>To convert a sequence of words of the form <tt>{word1 word2 ... wordn}</tt> into a
 * comment annotation: 
 * <ul>
 *  <li><b>sourceLayerId</b>: <tt>word</tt></li>
 *  <li><b>startPattern</b>: <tt>{(.*)</tt></li>
 *  <li><b>endPattern</b>: <tt>(.*)}</tt></li>
 *  <li><b>deleteInSource</b>: <tt>true</tt></li>
 *  <li><b>destinationLayerId</b>: <tt>comment</tt></li>
 *  <li><b>destinationStartResult</b>: <tt>$1</tt></li>
 *  <li><b>destinationEndResult</b>: <tt>$1</tt></li>
 * </ul>
 * So a sequence of words "here {pointing to head} and there" will end up being the
 * sequence "here and there" with a comment annotation labelled "pointing to head" between
 * "here" and "and". 
 * <p>To extract the VP phrase from a sequence of words of the form <tt>[XP word1 word2
 * ... wordn]</tt>: 
 * <ul>
 *  <li><b>sourceLayerId</b>: <tt>word</tt></li>
 *  <li><b>startPattern</b>: <tt>[(.*)</tt></li>
 *  <li><b>endPattern</b>: <tt>(.*)]</tt></li>
 *  <li><b>deleteInSource</b>: <tt>false</tt></li>
 *  <li><b>sourceStartResult</b>: <tt>null</tt></li>
 *  <li><b>sourceEndResult</b>: <tt>$1</tt></li>
 *  <li><b>destinationLayerId</b>: <tt>phrase</tt></li>
 *  <li><b>destinationStartResult</b>: <tt>$1</tt></li>
 *  <li><b>destinationEndResult</b>: <tt>null</tt></li>
 * </ul>
 * So a sequence of words "the fox [VP jumps over the dog]" will end up being the sequence
 * "the fox jumps over the dog]" with a phrase annotation labelled "NP" spanning "jumps
 * over the dog". 
 * <br>Note that in this case, having {@link #sourceStartResult} set to null indicates
 * that the start source annotation should be deleted, and having {@link
 * #destinationEndResult} set to null inicates that the last annotation should not be
 * concatenated to the destination label. 
 * @author Robert Fromont robert@fromont.net.nz
 */

public class SpanningConventionTransformer // TODO implementation that handles nested extraction, for phrae structure
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
   public SpanningConventionTransformer setSourceLayerId(String newSourceLayerId) { sourceLayerId = newSourceLayerId; return this; }

   /**
    * A regular expression matching the label of the first source annotation in the span.
    * @see #getStartPattern()
    * @see #setStartPattern(String)
    */
   protected String startPattern;
   /**
    * Getter for {@link #startPattern}: A regular expression matching the label of the
    * first source annotation in the span. 
    * @return A regular expression matching the label of the first source annotation in the span.
    */
   public String getStartPattern() { return startPattern; }
   /**
    * Setter for {@link #startPattern}: A regular expression matching the label of the
    * first source annotation in the span. 
    * @param newStartPattern A regular expression matching the label of the first source
    * annotation in the span. 
    */
   public SpanningConventionTransformer setStartPattern(String newStartPattern) { startPattern = newStartPattern; return this; }

   /**
    * A regular expression matching the label of the last source annotation in the span.
    * @see #getEndPattern()
    * @see #setEndPattern(String)
    */
   protected String endPattern;
   /**
    * Getter for {@link #endPattern}: A regular expression matching the label of the last
    * source annotation in the span. 
    * @return A regular expression matching the label of the last source annotation in the span.
    */
   public String getEndPattern() { return endPattern; }
   /**
    * Setter for {@link #endPattern}: A regular expression matching the label of the last
    * source annotation in the span. 
    * @param newEndPattern A regular expression matching the label of the last source annotation in the span.
    */
   public SpanningConventionTransformer setEndPattern(String newEndPattern) { endPattern = newEndPattern; return this; }

   /**
    * Whether to delete the source annotations between the start and end source
    * annotations (exclusive). 
    * @see #getDeleteInSource()
    * @see #setDeleteInSource(boolean)
    */
   protected boolean deleteInSource;
   /**
    * Getter for {@link #deleteInSource}: Whether to delete the source annotations between
    * the start and end source annotations (exclusive). 
    * @return Whether to delete the source annotations between the start and end source
    * annotations (exclusive). 
    */
   public boolean getDeleteInSource() { return deleteInSource; }
   /**
    * Setter for {@link #deleteInSource}: Whether to delete the source annotations between
    * the start and end source annotations (exclusive). 
    * @param newDeleteInSource Whether to delete the source annotations between the start
    * and end source annotations (exclusive). 
    */
   public SpanningConventionTransformer setDeleteInSource(boolean newDeleteInSource) { deleteInSource = newDeleteInSource; return this; }

   /**
    * The resulting label of the start source annotation, which may contain references to
    * captured groups in {@link #startPattern}, or be null to delete the start
    * annotation. 
    * @see #getSourceStartResult()
    * @see #setSourceStartResult(String)
    */
   protected String sourceStartResult;
   /**
    * Getter for {@link #sourceStartResult}: The resulting label of the start source
    * annotation, which may contain references to captured groups in {@link
    * #startPattern}, or be null to delete the start annotation. 
    * @return The resulting label of the start source annotation, which may contain
    * references to captured groups in {@link #startPattern}, or be null to delete the
    * start annotation. 
    */
   public String getSourceStartResult() { return sourceStartResult; }
   /**
    * Setter for {@link #sourceStartResult}: The resulting label of the start source
    * annotation, which may contain references to captured groups in {@link
    * #startPattern}, or be null to delete the start annotation. 
    * @param newSourceStartResult The resulting label of the start source annotation,
    * which may contain references to captured groups in {@link #startPattern}, or be null
    * to delete the start annotation. 
    */
   public SpanningConventionTransformer setSourceStartResult(String newSourceStartResult) { sourceStartResult = newSourceStartResult; return this; }

   /**
    * The resulting label of the end source annotation, which may contain references to
    * captured groups in {@link #endPattern}, or be null to delete the end annotation. 
    * @see #getSourceEndResult()
    * @see #setSourceEndResult(String)
    */
   protected String sourceEndResult;
   /**
    * Getter for {@link #sourceEndResult}: The resulting label of the end source
    * annotation, which may contain references to captured groups in {@link #endPattern},
    * or be null to delete the end annotation. 
    * @return The resulting label of the end source annotation, which may contain
    * references to captured groups in {@link #endPattern}, or be null to delete the end
    * annotation. 
    */
   public String getSourceEndResult() { return sourceEndResult; }
   /**
    * Setter for {@link #sourceEndResult}: The resulting label of the end source
    * annotation, which may contain references to captured groups in {@link #endPattern},
    * or be null to delete the end annotation. 
    * @param newSourceEndResult The resulting label of the end source annotation, which
    * may contain references to captured groups in {@link #endPattern}, or be null to
    * delete the end annotation. 
    */
   public SpanningConventionTransformer setSourceEndResult(String newSourceEndResult) { sourceEndResult = newSourceEndResult; return this; }

   /**
    * Layer ID of the annotation created for each span.
    * @see #getDestinationLayerId()
    * @see #setDestinationLayerId(String)
    */
   protected String destinationLayerId;
   /**
    * Getter for {@link #destinationLayerId}: Layer ID of the annotation created for each span.
    * @return Layer ID of the annotation created for each span.
    */
   public String getDestinationLayerId() { return destinationLayerId; }
   /**
    * Setter for {@link #destinationLayerId}: Layer ID of the annotation created for each span.
    * @param newDestinationLayerId Layer ID of the annotation created for each span.
    */
   public SpanningConventionTransformer setDestinationLayerId(String newDestinationLayerId) { destinationLayerId = newDestinationLayerId; return this; }

   /**
    * Delimiter to insert between source labels when concatenting them to for the
    * destination label.  The default is <code>" "</code> (space). 
    * @see #getDelimiter()
    * @see #setDelimiter(String)
    */
   protected String delimiter = " ";
   /**
    * Getter for {@link #delimiter}: Delimiter to insert between source labels when
    * concatenting them to for the destination label. 
    * @return Delimiter to insert between source labels when concatenting them to for the
    * destination label. 
    */
   public String getDelimiter() { return delimiter; }
   /**
    * Setter for {@link #delimiter}: Delimiter to insert between source labels when
    * concatenting them to for the destination label. 
    * @param newDelimiter Delimiter to insert between source labels when concatenting them
    * to for the destination label. 
    */
   public SpanningConventionTransformer setDelimiter(String newDelimiter) { delimiter = newDelimiter; return this; }

   /**
    * The resulting label appended to the destination annotation, which may contain
    * references to captured groups in {@link #startPattern}, or be null to not include
    * the start annotation. 
    * @see #getDestinationStartResult()
    * @see #setDestinationStartResult(String)
    */
   protected String destinationStartResult;
   /**
    * Getter for {@link #destinationStartResult}: The resulting label appended to the
    * destination annotation, which may contain references to captured groups in {@link
    * #startPattern}, or be null to not include the start annotation. 
    * @return The resulting label appended to the destination annotation, which may
    * contain references to captured groups in {@link #startPattern}, or be null to not
    * include the start annotation. 
    */
   public String getDestinationStartResult() { return destinationStartResult; }
   /**
    * Setter for {@link #destinationStartResult}: The resulting label appended to the
    * destination annotation, which may contain references to captured groups in {@link
    * #startPattern}, or be null to not include the start annotation. 
    * @param newDestinationStartResult The resulting label appended to the destination
    * annotation, which may contain references to captured groups in {@link
    * #startPattern}, or be null to not include the start annotation. 
    */
   public SpanningConventionTransformer setDestinationStartResult(String newDestinationStartResult) { destinationStartResult = newDestinationStartResult; return this; }

   /**
    * The resulting label appended to the destination annotation, which may contain
    * references to captured groups in {@link #endPattern}, or be null to not include the
    * end annotation. 
    * @see #getDestinationEndResult()
    * @see #setDestinationEndResult(String)
    */
   protected String destinationEndResult;
   /**
    * Getter for {@link #destinationEndResult}: The resulting label appended to the
    * destination annotation, which may contain references to captured groups in {@link
    * #endPattern}, or be null to not include the end annotation. 
    * @return The resulting label appended to the destination annotation, which may
    * contain references to captured groups in {@link #endPattern}, or be null to not
    * include the end annotation. 
    */
   public String getDestinationEndResult() { return destinationEndResult; }
   /**
    * Setter for {@link #destinationEndResult}: The resulting label appended to the
    * destination annotation, which may contain references to captured groups in {@link
    * #endPattern}, or be null to not include the end annotation. 
    * @param newDestinationEndResult The resulting label appended to the destination
    * annotation, which may contain references to captured groups in {@link #endPattern},
    * or be null to not include the end annotation. 
    */
   public SpanningConventionTransformer setDestinationEndResult(String newDestinationEndResult) { destinationEndResult = newDestinationEndResult; return this; }
   
   /**
    * Whether the destination annotation annotates the source token prior to the first
    * matching source token (i.e. whether they should share start anchors). 
    * @see #getAnnotatePrevious()
    * @see #setAnnotatePrevious(boolean)
    */
   protected boolean annotatePrevious = false;
   /**
    * Getter for {@link #annotatePrevious}: Whether the destination annotation annotates
    * the source token prior to the first matching source token (i.e. whether they should
    * share start anchors). 
    * @return Whether the destination annotation annotates the source token prior to the
    * first matching source token (i.e. whether they should share start anchors). 
    */
   public boolean getAnnotatePrevious() { return annotatePrevious; }
   /**
    * Setter for {@link #annotatePrevious}: Whether the destination annotation annotates
    * the source token prior to the first matching source token (i.e. whether they should
    * share start anchors). 
    * @param newAnnotatePrevious Whether the destination annotation annotates the source
    * token prior to the first matching source token (i.e. whether they should share start
    * anchors). 
    */
   public SpanningConventionTransformer setAnnotatePrevious(boolean newAnnotatePrevious) { annotatePrevious = newAnnotatePrevious; return this; }
   
   /**
    * Whether to close gaps in the source layer created by deleting span annotations, by
    * setting the end of the previous annotation to the start of the following
    * annotation. 
    * @see #getCloseGaps()
    * @see #setCloseGaps(boolean)
    */
   protected boolean closeGaps = true;
   /**
    * Getter for {@link #closeGaps}: Whether to close gaps in the source layer created by
    * deleting span annotations, by setting the end of the previous annotation to the
    * start of the following annotation. 
    * @return Whether to close gaps in the source layer created by deleting span
    * annotations, by setting the end of the previous annotation to the start of the
    * following annotation. 
    */
   public boolean getCloseGaps() { return closeGaps; }
   /**
    * Setter for {@link #closeGaps}: Whether to close gaps in the source layer created by
    * deleting span annotations, by setting the end of the previous annotation to the
    * start of the following annotation. 
    * @param newCloseGaps Whether to close gaps in the source layer created by deleting
    * span annotations, by setting the end of the previous annotation to the start of the
    * following annotation. 
    */
   public SpanningConventionTransformer setCloseGaps(boolean newCloseGaps) { closeGaps = newCloseGaps; return this; }

   // Methods:
   
   /**
    * Default constructor.
    */
   public SpanningConventionTransformer()
   {
   } // end of constructor

   /**
    * Constructor from attribute values.
    * @param sourceLayerId Layer ID of the annotations to transform.
    * @param startPattern A regular expression matching the label of the first source
    * annotation in the span. 
    * @param endPattern A regular expression matching the label of the last source
    * annotation in the span. 
    * @param deleteInSource Whether to delete the source annotations between the start and
    * end source annotations (exclusive). 
    * @param sourceStartResult The resulting label of the start source annotation, which
    * may contain references to captured groups in {@link #startPattern}, or be null to
    * delete the start annotation. 
    * @param sourceEndResult The resulting label of the end source annotation, which may
    * contain references to captured groups in {@link #endPattern}, or be null to delete
    * the end annotation. 
    * @param destinationLayerId Layer ID of the annotation created for each span.
    * @param delimiter Delimiter to insert between source labels when concatenting them to
    * for the destination label. 
    * @param destinationStartResult The resulting label appended to the destination
    * annotation, which may contain references to captured groups in {@link
    * #startPattern}, or be null to not include the start annotation. 
    * @param destinationEndResult The resulting label appended to the destination
    * annotation, which may contain references to captured groups in {@link #endPattern},
    * or be null to not include the end annotation. 
    */
   public SpanningConventionTransformer(String sourceLayerId, String startPattern, String endPattern, boolean deleteInSource, String sourceStartResult, String sourceEndResult, String destinationLayerId, String delimiter, String destinationStartResult, String destinationEndResult)
   {
      setSourceLayerId(sourceLayerId);
      setStartPattern(startPattern);
      setEndPattern(endPattern);
      setDeleteInSource(deleteInSource);
      setSourceStartResult(sourceStartResult);
      setSourceEndResult(sourceEndResult);
      setDestinationLayerId(destinationLayerId);
      setDelimiter(delimiter);
      setDestinationStartResult(destinationStartResult);
      setDestinationEndResult(destinationEndResult);
   } // end of constructor

   /**
    * Constructor from attribute values. The {@link #delimiter} used is the default.
    * @param sourceLayerId Layer ID of the annotations to transform.
    * @param startPattern A regular expression matching the label of the first source
    * annotation in the span. 
    * @param endPattern A regular expression matching the label of the last source
    * annotation in the span. 
    * @param deleteInSource Whether to delete the source annotations between the start and
    * end source annotations (exclusive). 
    * @param sourceStartResult The resulting label of the start source annotation, which
    * may contain references to captured groups in {@link #startPattern}, or be null to
    * delete the start annotation. 
    * @param sourceEndResult The resulting label of the end source annotation, which may
    * contain references to captured groups in {@link #endPattern}, or be null to delete
    * the end annotation. 
    * @param destinationLayerId Layer ID of the annotation created for each span. 
    * @param destinationStartResult The resulting label appended to the destination
    * annotation, which may contain references to captured groups in {@link #startPattern},
    * or be null to not include the start annotation. 
    * @param destinationEndResult The resulting label appended to the destination
    * annotation, which may contain references to captured groups in {@link #endPattern},
    * or be null to not include the end annotation. 
    */
   public SpanningConventionTransformer(String sourceLayerId, String startPattern, String endPattern, boolean deleteInSource, String sourceStartResult, String sourceEndResult, String destinationLayerId, String destinationStartResult, String destinationEndResult)
   {
      setSourceLayerId(sourceLayerId);
      setStartPattern(startPattern);
      setEndPattern(endPattern);
      setDeleteInSource(deleteInSource);
      setSourceStartResult(sourceStartResult);
      setSourceEndResult(sourceEndResult);
      setDestinationLayerId(destinationLayerId);
      setDestinationStartResult(destinationStartResult);
      setDestinationEndResult(destinationEndResult);
   } // end of constructor

   /**
    * Constructor from attribute values.
    * @param sourceLayerId Layer ID of the annotations to transform.
    * @param startPattern A regular expression matching the label of the first source
    * annotation in the span. 
    * @param endPattern A regular expression matching the label of the last source
    * annotation in the span. 
    * @param deleteInSource Whether to delete the source annotations between the start and
    * end source annotations (exclusive). 
    * @param sourceStartResult The resulting label of the start source annotation, which
    * may contain references to captured groups in {@link #startPattern}, or be null to
    * delete the start annotation. 
    * @param sourceEndResult The resulting label of the end source annotation, which may
    * contain references to captured groups in {@link #endPattern}, or be null to delete
    * the end annotation. 
    * @param destinationLayerId Layer ID of the annotation created for each span. 
    * @param delimiter Delimiter to insert between source labels when concatenting them to
    * for the destination label. 
    * @param destinationStartResult The resulting label appended to the destination
    * annotation, which may contain references to captured groups in {@link
    * #startPattern}, or be null to not include the start annotation. 
    * @param destinationEndResult The resulting label appended to the destination
    * annotation, which may contain references to captured groups in {@link #endPattern},
    * or be null to not include the end annotation. 
    * @param annotatePrevious Whether the destination annotation annotates the source
    * token prior to the first matching source token (i.e. whether they should share start
    * anchors). 
    */
   public SpanningConventionTransformer(String sourceLayerId, String startPattern, String endPattern, boolean deleteInSource, String sourceStartResult, String sourceEndResult, String destinationLayerId, String delimiter, String destinationStartResult, String destinationEndResult, boolean annotatePrevious)
   {
      setSourceLayerId(sourceLayerId);
      setStartPattern(startPattern);
      setEndPattern(endPattern);
      setDeleteInSource(deleteInSource);
      setSourceStartResult(sourceStartResult);
      setSourceEndResult(sourceEndResult);
      setDestinationLayerId(destinationLayerId);
      setDelimiter(delimiter);
      setDestinationStartResult(destinationStartResult);
      setDestinationEndResult(destinationEndResult);
      setAnnotatePrevious(annotatePrevious);
   } // end of constructor

   /**
    * Constructor from attribute values. The {@link #delimiter} used is the default.
    * @param sourceLayerId Layer ID of the annotations to transform.
    * @param startPattern A regular expression matching the label of the first source
    * annotation in the span. 
    * @param endPattern A regular expression matching the label of the last source
    * annotation in the span. 
    * @param deleteInSource Whether to delete the source annotations between the start and
    * end source annotations (exclusive). 
    * @param sourceStartResult The resulting label of the start source annotation, which
    * may contain references to captured groups in {@link #startPattern}, or be null to
    * delete the start annotation. 
    * @param sourceEndResult The resulting label of the end source annotation, which may
    * contain references to captured groups in {@link #endPattern}, or be null to delete
    * the end annotation. 
    * @param destinationLayerId Layer ID of the annotation created for each span. 
    * @param destinationStartResult The resulting label appended to the destination
    * annotation, which may contain references to captured groups in {@link
    * #startPattern}, or be null to not include the start annotation. 
    * @param destinationEndResult The resulting label appended to the destination
    * annotation, which may contain references to captured groups in {@link #endPattern},
    * or be null to not include the end annotation. 
    * @param annotatePrevious Whether the destination annotation annotates the source
    * token prior to the first matching source token (i.e. whether they should share start
    * anchors). 
    */
   public SpanningConventionTransformer(String sourceLayerId, String startPattern, String endPattern, boolean deleteInSource, String sourceStartResult, String sourceEndResult, String destinationLayerId, String destinationStartResult, String destinationEndResult, boolean annotatePrevious)
   {
      setSourceLayerId(sourceLayerId);
      setStartPattern(startPattern);
      setEndPattern(endPattern);
      setDeleteInSource(deleteInSource);
      setSourceStartResult(sourceStartResult);
      setSourceEndResult(sourceEndResult);
      setDestinationLayerId(destinationLayerId);
      setDestinationStartResult(destinationStartResult);
      setDestinationEndResult(destinationEndResult);
      setAnnotatePrevious(annotatePrevious);
  } // end of constructor

   /**
    * Constructor from attribute values. The {@link #delimiter} used is the default.
    * @param sourceLayerId Layer ID of the annotations to transform.
    * @param startPattern A regular expression matching the label of the first source
    * annotation in the span. 
    * @param endPattern A regular expression matching the label of the last source
    * annotation in the span. 
    * @param deleteInSource Whether to delete the source annotations between the start and
    * end source annotations (exclusive). 
    * @param sourceStartResult The resulting label of the start source annotation, which
    * may contain references to captured groups in {@link #startPattern}, or be null to
    * delete the start annotation. 
    * @param sourceEndResult The resulting label of the end source annotation, which may
    * contain references to captured groups in {@link #endPattern}, or be null to delete
    * the end annotation. 
    * @param destinationLayerId Layer ID of the annotation created for each span. 
    * @param destinationStartResult The resulting label appended to the destination
    * annotation, which may contain references to captured groups in {@link
    * #startPattern}, or be null to not include the start annotation. 
    * @param destinationEndResult The resulting label appended to the destination
    * annotation, which may contain references to captured groups in {@link #endPattern},
    * or be null to not include the end annotation. 
    * @param annotatePrevious Whether the destination annotation annotates the source
    * token prior to the first matching source token (i.e. whether they should share start
    * anchors). 
    * @param closeGaps Whether to close gaps in the source layer created by deleting span
    * annotations, by setting the end of the previous annotation to the start of the
    * following annotation. 
    */
   public SpanningConventionTransformer(String sourceLayerId, String startPattern, String endPattern, boolean deleteInSource, String sourceStartResult, String sourceEndResult, String destinationLayerId, String destinationStartResult, String destinationEndResult, boolean annotatePrevious, boolean closeGaps)
   {
      setSourceLayerId(sourceLayerId);
      setStartPattern(startPattern);
      setEndPattern(endPattern);
      setDeleteInSource(deleteInSource);
      setSourceStartResult(sourceStartResult);
      setSourceEndResult(sourceEndResult);
      setDestinationLayerId(destinationLayerId);
      setDestinationStartResult(destinationStartResult);
      setDestinationEndResult(destinationEndResult);
      setAnnotatePrevious(annotatePrevious);
      setCloseGaps(closeGaps);
  } // end of constructor

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
      Layer destinationLayer = graph.getLayer(getDestinationLayerId());
      if (getSourceLayerId().equals(getDestinationLayerId())) 
	 throw new TransformationException(this, "Source and destination layer are the same: " + getDestinationLayerId());

      // ensure we can track our changes
      ChangeTracker ourTracker = new ChangeTracker();
      ChangeTracker originalTracker = graph.getTracker();
      if (originalTracker == null)
      {
         graph.setTracker(ourTracker);
         ourTracker.reset(); // in case there were any lingering creates/destroys in the graph
      }
      else
      {
         originalTracker.addListener(ourTracker);
      }

      boolean sourceDestinationOfParent = destinationLayer != null 
	 && destinationLayer.getParentId().equals(getSourceLayerId());
      boolean graphDestinationOfParent = destinationLayer != null 
	 && destinationLayer.getParentId().equals("graph");
      try
      {
	 Pattern startRegexp = Pattern.compile(getStartPattern());
	 Pattern endRegexp = Pattern.compile(getEndPattern());
	 Vector<Change> changes = new Vector<Change>();
	 // group the source annotations by parent...
	 // for each parent
	 for (Annotation parent : graph.list(graph.getLayer(getSourceLayerId()).getParentId()))
	 {
	    Vector<Annotation> span = null;
	    Annotation previousSource = null;
	    Annotation newTarget = null;
	    for (Annotation source : parent.annotations(getSourceLayerId()))
	    {
	       // are we in a span?
	       if (span == null)
	       { // look for start
		  Matcher matcher = startRegexp.matcher(source.getLabel());
		  if (matcher.matches())
		  {
		     span = new Vector<Annotation>();
		  } // label matches
		  else
		  {
		     if (source.getChange() != Change.Operation.Destroy) previousSource = source;
		  }
	       } // not in a span

	       if (span != null)
	       { // in a span
		  span.add(source);
		  // look for end
		  Matcher endMatcher = endRegexp.matcher(source.getLabel());
		  if (endMatcher.matches())
		  { // found the end of the span
		     Matcher startMatcher = startRegexp.matcher(span.elementAt(0).getLabel());

		     if (getDestinationLayerId() != null)
		     {
			// destination annotation:
			
			StringBuffer label = new StringBuffer();
			if (getDestinationStartResult() != null)
			{ // add start annotation to label
			   label.append(startMatcher.replaceAll(getDestinationStartResult()));
			}
			if (span.size() == 1)
			{ // special case: span a single source annotation
			   if (getDestinationEndResult() != null)
			   {
			      endMatcher = endRegexp.matcher(label.toString());
			      label = new StringBuffer();
			      label.append(endMatcher.replaceAll(getDestinationEndResult()));
			   }
			}
			else
			{ // span longer than 1
			   if (getDeleteInSource())
			   {
			      // for each annotation between the start and the end
			      for (int i = 1; i < span.size() - 1; i++)
			      {
				 // only append anything if the source label is set
				 if (span.elementAt(i).getLabel().length() > 0)
				 {
				    // only append delimiter if destintation label is not empty
				    if (label.length() > 0) label.append(getDelimiter());
				    // append the source label to the destination
				    label.append(span.elementAt(i).getLabel());
				 }
			      }
			   }
			   
			   // end annotation
			   if (getDestinationEndResult() != null)
			   {
			      String endResult = endMatcher.replaceAll(getDestinationEndResult());
			      // only append anything if the source label is set
			      if (endResult.length() > 0)
			      {
				 // only append delimiter if destintation label is not empty
				 if (label.length() > 0) label.append(getDelimiter());
				 // append the transformed end source label to the destination
				 label.append(endResult);
			      }
			   }
			} // span longer than 1
			Annotation startSpan = span.firstElement();
			Annotation endSpan = span.lastElement();
			if (getAnnotatePrevious() && previousSource != null)
			{
			   startSpan = previousSource;
			   endSpan = previousSource;
			}
			Annotation spanParent = sourceDestinationOfParent?startSpan
			   :graphDestinationOfParent?graph
			   :startSpan.getParent();
			Annotation annotation = graph.createSpan(
			   startSpan, endSpan, 
			   getDestinationLayerId(), label.toString(), 
			   spanParent);
			changes.addAll( // record changes of:
			   annotation.getChanges());
		     } // non-null destination

		     // source annotations: 
		     Anchor endOfGap = null;

		     if (getSourceStartResult() == null)
		     { // delete start annotation
			endOfGap = span.firstElement().getEnd();
			changes.add( // record changes of:
			   span.firstElement().destroy());
		     }
		     else
		     { // change the label
			String l = startMatcher.replaceAll(getSourceStartResult());
			if (l.length() == 0) // treat empty label as a delete
			{
			   endOfGap = span.firstElement().getEnd();
			   changes.add( // record changes of:
			      span.firstElement().destroy());
			}
			else
			{
			   if (!l.equals(span.firstElement().getLabel()))
			   { // only change if it's different
                              span.firstElement().setLabel(l);
			   }
			}
		     }
		     
		     if (getDeleteInSource())
		     { // intervening annotations
			// for each annotation between the start and the end
			for (int i = 1; i < span.size() - 1; i++)
			{
			   endOfGap = span.elementAt(i).getEnd();
			   changes.add( // record changes of:
			      span.elementAt(i).destroy());
			}
		     } // intervening annotations

		     if (getSourceEndResult() == null)
		     { // delete end annotation
			endOfGap = span.lastElement().getEnd();
			changes.add( // record changes of:
			   span.lastElement().destroy());
		     }
		     else
		     { // change the label
			String l = endMatcher.replaceAll(getSourceEndResult());
			if (l.length() == 0) // treat empty label as a delete
			{
			   endOfGap = span.lastElement().getEnd();
			   changes.add( // record changes of:
			      span.lastElement().destroy());
			}
			else
			{
			   if (!l.equals(span.lastElement().getLabel()))
			   { // only change if it's different
                              span.lastElement().setLabel(l);
			   }
			}
		     }

		     if (getCloseGaps())
		     {
			if (getDeleteInSource())
			{
			   if (endOfGap != null && previousSource != null)
			   {
			      Anchor oldEnd = previousSource.getEnd();
			      if (endOfGap != oldEnd)
			      { // everything that ends here will now end at endOfGap
				 oldEnd.moveEndingAnnotations(endOfGap);
			      } // end of gap is elsewhere
			   } // endOfGap != null && previousSource != null
			}
			// else // !deleteInSource TODO
		     } // close gaps
		     
		     span = null;
		  } // found the end of the span
	       } // in a span	       
	    } // next source annotation

	    // now ensure ordinals are correct
	    int ordinal = 0;
	    for (Annotation source : parent.annotations(getSourceLayerId()))
	    {
	       // set initial value of ordinal to the first ordinal
	       if (ordinal == 0) ordinal = source.getOrdinal() - 1;

	       if (source.getChange() != Change.Operation.Destroy)
	       {
		  ordinal++;		  
		  // ensure ordinal is correct
		  if (source.getOrdinal() != ordinal)
		  {
		     source.setOrdinal(ordinal);
		  }
	       } // this annotation not deleted
	    } // next child

	 } // next parent

         return new Vector<Change>(ourTracker.getChanges());
      }
      catch(PatternSyntaxException exception)
      {
	 throw new TransformationException(this, exception);
      }
      finally
      {
         // set the tracker back how it was
         if (originalTracker == null)
         {
            graph.setTracker(null);
         }
         else
         {
            originalTracker.removeListener(ourTracker);
         }
      }
   }
} // end of class SpanningConventionTransformer
