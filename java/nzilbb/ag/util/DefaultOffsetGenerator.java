//
// Copyright 2015-2016 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import nzilbb.ag.*;

/**
 * Generates default anchor offsets. These are computed using linear interpolation between certain offsets. What counts as <var>certain</var> depends on how {@link #defaultOffsetThreshold} is set.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class DefaultOffsetGenerator
  implements IGraphTransformer
{
   // Attributes:
   
   /**
    * Fatal errors raised during the last {@link #transform(Graph)}.
    * @see #getErrors()
    * @see #setErrors(Vector)
    */
   protected Vector<String> errors;
   /**
    * Getter for {@link #errors}: Fatal errors raised during the last {@link #transform(Graph)}.
    * @return Fatal errors raised during the last {@link #transform(Graph)}.
    */
   public Vector<String> getErrors() { return errors; }
   /**
    * Setter for {@link #errors}: Fatal errors raised during the last {@link #transform(Graph)}.
    * @param newErrors Fatal errors raised during the last {@link #transform(Graph)}.
    */
   public DefaultOffsetGenerator setErrors(Vector<String> newErrors) { errors = newErrors; return this; }
   
   /**
    * Whether a log of messages should be kept for reporting.
    * @see #getDebug()
    * @see #setDebug(boolean)
    * @see #getLog()
    * @see #log(Object...)
    */
   protected boolean debug = false;
   /**
    * Getter for {@link #debug}: Whether a log of messages should be kept for reporting.
    * @return Whether a log of messages should be kept for reporting.
    * @see #getLog()
    * @see #log(Object...)
    */
   public boolean getDebug() { return debug; }
   /**
    * Setter for {@link #debug}: Whether a log of messages should be kept for reporting.
    * @param newDebug Whether a log of messages should be kept for reporting.
    * @see #getLog()
    * @see #log(Object...)
    */
   public DefaultOffsetGenerator setDebug(boolean newDebug) { debug = newDebug; return this; }

   /**
    * Messages for debugging.
    * @see #getLog()
    * @see #setLog(Vector)
    */
   protected Vector<String> log;
   /**
    * Getter for {@link #log}: Messages for debugging.
    * @return Messages for debugging.
    */
   public Vector<String> getLog() { return log; }
   /**
    * Setter for {@link #log}: Messages for debugging.
    * @param newLog Messages for debugging.
    */
   protected DefaultOffsetGenerator setLog(Vector<String> newLog) { log = newLog; return this; }

   /**
    * Value to assume in the case of Anchors that have no value for "confidence".
    * <p>The default value is {@link Constants#CONFIDENCE_MANUAL} - i.e. anchors with no "confidence" attribute are assumed to be manually aligned (high confidence). This means that the default behaviour on a graph that does not include "confidence" values on anchors is to only assign offsets to anchors that have none.
    * @see #getDefaultAnchorConfidence()
    * @see #setDefaultAnchorConfidence(int)
    * @see Constants#CONFIDENCE_MANUAL
    */
   protected int defaultAnchorConfidence = Constants.CONFIDENCE_MANUAL;
   /**
    * Getter for {@link #defaultAnchorConfidence}: Value to assume in the case of Anchors that have no value for "confidence".
    * @return Value to assume in the case of Anchors that have no value for "confidence".
    */
   public int getDefaultAnchorConfidence() { return defaultAnchorConfidence; }
   /**
    * Setter for {@link #defaultAnchorConfidence}: Value to assume in the case of Anchors that have no value for "confidence".
    * @param newDefaultAnchorConfidence Value to assume in the case of Anchors that have no value for "confidence".
    */
   public DefaultOffsetGenerator setDefaultAnchorConfidence(int newDefaultAnchorConfidence) { defaultAnchorConfidence = newDefaultAnchorConfidence; return this; }
   
   /**
    * The confidence threshold for default anchor offset computation.
    * <p>Anchors with null offset, with no "confidence" attribute, or with the "confidence" attribute set to equal or below this value, may have their offset set to a default, computed value. Generally, the default value is determined by linear interpolation between parent start/end anchors, or between anchors with higher confidence.
    * <p>The default value is {@link Constants#CONFIDENCE_DEFAULT}.
    * @see #getDefaultOffsetThreshold()
    * @see #setDefaultOffsetThreshold(int)
    */
   protected int defaultOffsetThreshold = Constants.CONFIDENCE_DEFAULT;
   /**
    * Getter for {@link #defaultOffsetThreshold}: The confidence threshold for default anchor offset computation, or null to skip default offset computation.
    * @return The confidence threshold for default anchor offset computation, or null to skip default offset computation.
    */
   public int getDefaultOffsetThreshold() { return defaultOffsetThreshold; }
   /**
    * Setter for {@link #defaultOffsetThreshold}: The confidence threshold for default anchor offset computation, or null to skip default offset computation.
    * @param newDefaultOffsetThreshold The confidence threshold for default anchor offset computation, or null to skip default offset computation.
    */
   public DefaultOffsetGenerator setDefaultOffsetThreshold(int newDefaultOffsetThreshold) { defaultOffsetThreshold = newDefaultOffsetThreshold; return this; }

   
   /**
    * Value to set for <var>confidence</var> for anchors that have their offsets changed by this transformer.
    * <p>The default value is {@link Constants#CONFIDENCE_DEFAULT}.
    * @see #getConfidence()
    * @see #setConfidence(int)
    */
   protected int confidence = Constants.CONFIDENCE_DEFAULT;
   /**
    * Getter for {@link #confidence}: Value to set for <var>confidence</var> for anchors that have their offsets changed by this transformer.
    * @return Value to set for <var>confidence</var> for anchors that have their offsets changed by this transformer.
    */
   public int getConfidence() { return confidence; }
   /**
    * Setter for {@link #confidence}: Value to set for <var>confidence</var> for anchors that have their offsets changed by this transformer.
    * @param newConfidence Value to set for <var>confidence</var> for anchors that have their offsets changed by this transformer.
    */
   public DefaultOffsetGenerator setConfidence(int newConfidence) { confidence = newConfidence; return this; }


   // Methods:
   
   /**
    * Default constructor.
    */
   public DefaultOffsetGenerator()
   {
   } // end of constructor

   /**
    * Constructor with attributes.
    * @param defaultOffsetThreshold The confidence threshold for default anchor offset computation, or null to skip default offset computation.
    * @param defaultAnchorConfidence Value to assume in the case of Anchors that have no value for "confidence".
    * @throws TransformationException If the transformation cannot be completed.
    */
   public DefaultOffsetGenerator(int defaultOffsetThreshold, int defaultAnchorConfidence)
      throws TransformationException
   {
      setDefaultAnchorConfidence(defaultAnchorConfidence);
      setDefaultOffsetThreshold(defaultOffsetThreshold);
   } // end of constructor

   // IGraphTransformer method

   /**
    * Generates default anchor offsets.
    * <p>Anchors with null offset, with no "confidence" attribute, or with the "confidence" attribute set to equal or below {@link #defaultOffsetThreshold}, may have their offset set to a default, computed value. 
    * <p>Strings of candidate anchors are have their offsets set by linear interpolation between bounding anchors.
    * <p>Strings are determined by:
    * <ul>
    *  <li>chaining annotations together by common {@link Annotation#getStart()}/{@link Annotation#getEnd()} anchors (on any layer) - e.g. words chained together with interspersed or bounding noise annotations, or</li>
    *  <li>chaining annotations together by successive values of {@link Annotation#getOrdinal()} on a common layer and within a common parent - e.g. words within a turn, interspersed with discontinuties in the graph (i.e. pauses).</li>
    * </ul>
    * <p>Bounding anchors are determined by:
    * <ul>
    *  <li>having higher "confidence" than {@link #defaultOffsetThreshold} on the same layer and within a common parent - e.g. manually aligned words within a turn,</li>
    *  <li>having higher "confidence" than {@link #defaultOffsetThreshold} on a different layer and within a common parent - e.g. manually aligned utterances within a turn partition the words in that turn, and</li>
    *  <li>being a start or end anchor of a parent {@link #defaultOffsetThreshold} - e.g. a turn's start/end anchors bound the anchors of it's words.</li>
    *  <li>or otherwise using the existing offsets of bounding candidate anchors.</li>
    * </ul>
    * @param graph The graph to transform.
    * @return The changes introduced by the tranformation.
    * @throws TransformationException If the transformation cannot be completed.
    */
   public List<Change> transform(Graph graph)
      throws TransformationException
   {
      if (debug) setLog(new Vector<String>());
      setErrors(new Vector<String>());

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

      // before going to great effort, check there are any anchors at all that might be affected
      boolean anchorsUnderThreshold = false;
      for (Anchor a : graph.getAnchors().values())	 
      {
	 if (a.getOffset() == null || getConfidence(a) <= defaultOffsetThreshold)
	 {
	    anchorsUnderThreshold = true;
	    break;
	 }
      } // next anchor
      if (anchorsUnderThreshold)
      {
	 // we can't just sort the anchors of the graph all together before interpolation
	 // because there may be independant spans that should be interpolated separately
	 // e.g. the unaligned words of overlapping speech of two speakers
	 // what we need to do is chunk the graph by parent annotations whose children
	 // don't overlap (e.g. turns of words) and then interpolate their non-overlapping descendants
	 // however, we're not going to do this for 'top level' layers - e.g. topic tags,
	 // because their annotations may share anchors into other layers, and so changing those
	 // offsets will disrupt the order of those other layers, being out of their context.
	 
	 // traverse the layer hiercharchy to get a list of the uppermost layers that
	 // are not top-level, and are peersOverlap == false      
	 LayerHierarchyTraversal<HashSet<Layer>> layerTraversal 
	    = new LayerHierarchyTraversal<HashSet<Layer>>(new HashSet<Layer>(), graph.getSchema())
	    {
	       protected void pre(Layer child)
	       {
		  Layer parent = child.getParent();
		  // if the parent is not a top-level layer
		  if (parent != null // may be incomplete graph
		      && parent.getParentId() != null 
		      && !parent.getParentId().equals("graph")
		      // and children can have peers
		      && child.getPeers()
		      // and child peers cannot overlap
		      && !child.getPeersOverlap()
		      // and it's not a tag layer
		      && child.getAlignment() != Constants.ALIGNMENT_NONE
		      // and the parent temporally includes the children
		      && child.getParentIncludes()
		      // and we haven't already added this parent
		      && !getResult().contains(parent))
		  {
		     // and we haven't already added an ancestor
		     boolean includesAncestor = false;
		     for (Layer ancestor : parent.getAncestors())
		     {
			if (getResult().contains(ancestor))
			{
			   includesAncestor = true;
			   break;
			}
		     }
		     if (!includesAncestor)
		     {
			getResult().add(parent); // add the *parent* layer
		     }
		  } // not top level and peers don't overlap
	       }
	    };
	 
	 // for each non-top-level children-don't-overlap parent layer
	 for (Layer layer : layerTraversal.getResult())
	 {
	    // log("Layer ", layer);
	    // for each parent annotation
	    for (Annotation parent : graph.list(layer.getId()))
	    {
	       if (parent.getChange() == Change.Operation.Destroy) continue;
	       try
	       {
                  // set the offsets of the descendants
                  setOffsetsForDescendantsOf(parent);
	       }
	       catch (TransformationException x)
	       {
		  errors.add("Could not set descendant offsets for " + logAnnotation(parent) 
			     + ": " + x.getMessage());
	       }
	    } // next parent annotation
	    // log("Layer complete ", layer);
	 } // next layer
	 // log("Layers complete");
      } // anchorsUnderThreshold
      else
      {
	 log("There are no anchors with confidence <= ", defaultOffsetThreshold);
      }
      
      // set the tracker back how it was
      if (originalTracker == null)
      {
         graph.setTracker(null);
      }
      else
      {
         originalTracker.removeListener(ourTracker);
      }
      return new Vector<Change>(ourTracker.getChanges());
   }
   
   /**
    * Sets the default offsets for anchors of all descendants of the given annotation.
    * @param top The top of the annotation hierarchy to set anchor offsets of.
    * @return The changes made during this operation.
    * @throws TransformationException If the transformation cannot be completed.
    */
   public void setOffsetsForDescendantsOf(Annotation top)
      throws TransformationException
   {
      //log("Top: ", top);
      if (!top.getInstantaneous())
      {	 
	 // get a list of all anchors for relevant descendant annotations, 
	 // ordered by offset and also by using graph structure
	 // to order anchors with equal offsets and anchors with no offsets
	 TreeSet<Anchor> sortedAnchors = new TreeSet<Anchor>(new AnchorComparatorWithStructure());
	 // recursively descend through children, gathering anchors for non-overlapping child layers
	 descendantAnchors(top, sortedAnchors);

	 // avoid unbounded anchor chain problems by starting/ending the collection with
	 // immovable start/end anchors - these come from graph.getSortedAnchors()
	 // - which includes only anchors with offsets - instead of sortedAnchors
	 // ensure that, no matter what, the bounding sentinels have offsets set
	 Vector<Anchor> boundedAnchors = new Vector<Anchor>();
	 Anchor startSentinel = null;
	 if (top.getStart() != null && top.getStart().getOffset() != null)
	 {
	    startSentinel = new Anchor(top.getStart());
	 }
	 else
	 {
	    // use the lowest offset we find
	    startSentinel = new Anchor();
	    for (Anchor a : sortedAnchors)
	    {
	       if (a.getOffset() != null
		   && (startSentinel.getOffset() == null
		       || a.getOffset() < startSentinel.getOffset()))
	       {
		  startSentinel.setOffset(a.getOffset());
		  break;
	       }
	    } // next anchor
	 }
	 startSentinel.setConfidence(getDefaultOffsetThreshold() + 1);
	 boundedAnchors.add(startSentinel);

	 // then add them to our bounded anchor list
	 boundedAnchors.addAll(sortedAnchors);
	 
	 Anchor endSentinel = null;
	 if (top.getEnd() != null && top.getEnd().getOffset() != null)
	 {
	    endSentinel = new Anchor(top.getEnd());
	 }
	 else
	 { // use the highest offset we find
	    endSentinel = new Anchor();
	    for (Anchor a : sortedAnchors)
	    {
	       if (a.getOffset() != null
		   && (endSentinel.getOffset() == null
		       || a.getOffset() > endSentinel.getOffset()))
	       {
		  endSentinel.setOffset(a.getOffset());
	       }
	    } // next anchor
	 }
	 endSentinel.setConfidence(getDefaultOffsetThreshold() + 1);
	 boundedAnchors.add(endSentinel);
	 
	 // crawl through the anchors looking for unset offsets
	 Iterator<Anchor> anchors = boundedAnchors.iterator();
	 Anchor lastSetAnchor = null;
	 while (anchors.hasNext())
	 {
	    Anchor anchor = anchors.next();
	    if (anchor.getOffset() != null && getConfidence(anchor) > defaultOffsetThreshold)
	    {
	       lastSetAnchor = anchor;
	       log("last set: ", lastSetAnchor);
	    }
	    else
	    {
	       if (lastSetAnchor == null)
	       {
		  String message = "Could not determine bounds, starting from " + logAnchor(top.getStart());
		  log("ERROR: ", message);
		  throw new TransformationException(this, message);
	       }
		  
	       Vector<Anchor> unsetAnchors = new Vector<Anchor>();
	       unsetAnchors.add(anchor);
	       log("first unset: ", anchor);
	       
	       // scan forward from here to find the next set Anchor
	       Anchor nextSetAnchor = null;
	       while (nextSetAnchor == null)
	       {
		  // check we haven't hit the end
		  if (!anchors.hasNext())
		  {
		     if (anchor.getOffset() != null && getConfidence(anchor) > defaultOffsetThreshold)
		     {
			// the last one we saw actually has an offset,
			// so we use that one as the last anchor
			unsetAnchors.remove(anchor);
			nextSetAnchor = anchor;
			break;
		     }
		     else
		     {
			String message = "Could not determine bounds, starting from " + logAnchor(lastSetAnchor) 
			   + " after " + logAnchor(unsetAnchors.lastElement());
			log("ERROR: ", message);
			throw new TransformationException(this, message);
		     }
		  }
		  anchor = anchors.next();
		  if (anchor.getOffset() == null 
		      || getConfidence(anchor) <= defaultOffsetThreshold)
		  {
		     // add the unset anchor to the collection
		     unsetAnchors.add(anchor);
		     log("unset: ", anchor);
		  }
		  else // offset is set
		  {
		     // stop
		     nextSetAnchor = anchor;
		     log("next set: ", nextSetAnchor);
		  }
	       } // next anchor
		  
	       if (unsetAnchors.size() > 0)
	       {
		  // if there are no annotations between the last set anchor
		  // and the first unset anchor, then give the unset anchor
		  // the same offset as the last set anchor
		  Anchor firstUnset = unsetAnchors.firstElement();
		  assert lastSetAnchor != null : "lastSetAnchor != null";
		  assert firstUnset != null : "firstUnset != null";
		  if (lastSetAnchor.annotationTo(firstUnset) == null)
		  {
                     firstUnset
                        .setOffset(lastSetAnchor.getOffset())
                        .setConfidence(getConfidence());
		     unsetAnchors.remove(firstUnset);
		     lastSetAnchor = firstUnset;
		     log("revised last: ", lastSetAnchor);
		  }
	       }
		  
	       if (unsetAnchors.size() > 0)
	       {
		  // if there are no annotations between the next set anchor
		  // and the last unset anchor, then give the unset anchor
		  // the same offset as the next set anchor
		  Anchor lastUnset = unsetAnchors.lastElement();
		  if (lastUnset.annotationTo(nextSetAnchor) == null)
		  {
                     lastUnset
                        .setOffset(nextSetAnchor.getOffset())
                        .setConfidence(getConfidence());
		     unsetAnchors.remove(lastUnset);
		     nextSetAnchor = lastUnset;
		     log("revised next: ", nextSetAnchor);
		  }
		     
		  if (unsetAnchors.size() > 0)
		  {
		     // spread out unset anchors evenly between the bounds
		     double dStart = lastSetAnchor.getOffset();
		     double dEnd = nextSetAnchor.getOffset();
		     double dDuration = dEnd - dStart;
		     double dIncrement = dDuration / (unsetAnchors.size() + 1);
		     int i = 0;
		     for (Anchor unset : unsetAnchors)
		     {
			i++;
			double newOffset = dStart + i * dIncrement;
			if (unset.getOffset() == null 
			    || unset.getOffset().doubleValue() != newOffset
			    // upgrade confidence even if unset.offset == newOffset
			    || getConfidence(unset) < getConfidence())
			{
			   log("setting: ", unset, " offset to ", newOffset);
                           unset
                              .setOffset(newOffset)
                              .setConfidence(getConfidence());
			}
		     } // next unset anchor
		  } // unsetAnchors.size() > 0
	       } // unsetAnchors.size() > 0
		  
	       // update the last set anchor 
	       lastSetAnchor = nextSetAnchor;
		  
	    } // offset is not set
	 } // next anchor
      } // not an instant
   } // end of setOffsetsForDescendantsOf()
   
   
   /**
    * Recursively passes traverses child layers, adding anchors of children on non-peer-overlapping layers to the given set. Does not add the anchors of the parent (unless they're also anchors of some child).
    * @param parent The parent of the children to process.
    * @param anchors The collection to add the anchors to.
    */
   protected void descendantAnchors(Annotation parent, TreeSet<Anchor> anchors)
   {
      // log("Descendant anchors for ", parent);
      for (String layerId : parent.getAnnotations().keySet())
      {	 
	 Layer layer = parent.getGraph().getLayer(layerId);
	 if (layer == null) continue; // unknown layer
	 // log("child layer: ", layer.getId());
	 boolean addAnchors = layer.getPeers() 
	    && !layer.getPeersOverlap() 
	    && layer.getAlignment() != Constants.ALIGNMENT_NONE
	    && layer.getParentIncludes();
	 Anchor previousAnchor = parent.getStart(); 
	 for (Annotation child : parent.getAnnotations(layerId))
	 {
	    if (child.getChange() == Change.Operation.Destroy) continue;
	    if (addAnchors)
	    {
	       // add anchors from any leading chain between the last anchor and the start of the wchild
	       AnnotationChain chain = new AnnotationChain(previousAnchor, child.getStart());
	       for (Annotation link : chain)
	       {
		  // log("linked between: ", link);
		  if (link.getStart() != null) anchors.add(link.getStart());
		  if (link.getEnd() != null) anchors.add(link.getEnd());
	       } // next link
	       log(" child: ", child, " ", child.getStart(), "-", child.getEnd());
	       if (child.getStart() != null) anchors.add(child.getStart());
	       //log("added start ", child.getStart());
	       if (child.getEnd() != null) anchors.add(child.getEnd());
	       //log("added end ", child.getEnd());
	       previousAnchor = child.getEnd();
	    } // add anchors
	    // recurse into all layers regardless of layer definition, to catch interesting grandchildren
	    // log("descendants for: ", child);
	    descendantAnchors(child, anchors);
	 } // next child
	 
	 if (parent.getStart() != null
	     && addAnchors && !previousAnchor.equals(parent.getStart()))
	 { // add anchors from any trailing chain to the end of the parent
	    // log("looking for trailing links");
	    AnnotationChain chain = new AnnotationChain(previousAnchor, parent.getEnd());
	    // log("chain of ", chain.size(), " annotations");
	    for (Annotation link : chain)
	    {
	       // log("linked after: ", link);
	       if (link.getStart() != null) anchors.add(link.getStart());
	       if (link.getEnd() != null) anchors.add(link.getEnd());
	    } // next link
	 } // add anchors from any trailing chain to the end
      } // next child layer
   } // end of descendantAnchors()

   /**
    * Gets the confidence rating of a given anchor.  If no Integer confidence attribute is present, the #defaultAnchorConfidence is returned.
    * @param anchor The anchor to get the rating of.
    * @return The confidence rating of a given annotation, or defaultAnchorConfidence if it could not be determined.
    */
   protected int getConfidence(Anchor anchor)
   {
      Integer c = anchor.getConfidence();
      if (c == null) return getDefaultAnchorConfidence();
      return c.intValue();
   } // end of getConfidence()
   
   /**
    * A representation of the given anchor for logging purposes.
    * @param anchor The anchor to log.
    * @return A representation of the given anchor for logging purposes.
    */
   protected String logAnchor(Anchor anchor)
   {
      if (anchor == null) return "[null]";
      return "[" + anchor.getId() + "]" + anchor.getOffset() + "(" + getConfidence(anchor) + ")";
   } // end of logAnnotation()

   /**
    * A representation of the given annotation for logging purposes.
    * @param annotation The annotation to log.
    * @return A representation of the given annotation for loggin purposes.
    */
   protected String logAnnotation(Annotation annotation)
   {
      if (annotation == null) return "[null]";
      return "[" + annotation.getId() + "]" + annotation.getOrdinal() + "#" + annotation.getLabel();
   } // end of logAnnotation()

   /**
    * Logs a debugging message.
    * @param messages The objects making up the log message.
    */
   protected void log(Object ... messages)
   {
      if (debug)
      { // we only interpret arguments to log() if we're actually debugging...
	 StringBuilder s = new StringBuilder();
	 for (Object m : messages)
	 {
	    if (m instanceof Annotation)
	    {
	       if (m == null)
	       {
		  s.append("[null]");
	       }
	       else
	       {
		  Annotation annotation = (Annotation)m;
		  s.append("[").append(annotation.getId()).append("]")
		     .append(annotation.getOrdinal()).append("#")
		     .append(annotation.getLabel())
		     .append("(").append(annotation.getStart())
		     .append("-").append(annotation.getEnd()).append(")");
	       }
	    }
	    else if (m instanceof Anchor)
	    {
	       if (m == null)
	       {
		  s.append("[null]");
	       }
	       else
	       {
		  Anchor anchor = (Anchor)m;
		  s.append("[").append(anchor.getId()).append("]").append(anchor.getOffset());
	       }
	    }
	    else
	    {
	       s.append(m.toString());
	    }
	 }	 
	 log.add(s.toString());
	 // System.out.println(message);
      }
   } // end of log()

} // end of class Validator
