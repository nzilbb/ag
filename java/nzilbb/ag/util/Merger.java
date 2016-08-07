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
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.List;

import nzilbb.editpath.*;
import nzilbb.ag.*;

/**
 * Merges an editer version of a graph into the original verison of that graph.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class Merger
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
   public void setErrors(Vector<String> newErrors) { errors = newErrors; }

   /**
    * Whether a log of messages should be kept for reporting.
    * @see #getDebug()
    * @see #setDebug(boolean)
    * @see #getLog()
    * @see #log(String)
    */
   protected boolean debug = false;
   /**
    * Getter for {@link #debug}: Whether a log of messages should be kept for reporting.
    * @return Whether a log of messages should be kept for reporting.
    * @see #getLog()
    * @see #log(String)
    */
   public boolean getDebug() { return debug; }
   /**
    * Setter for {@link #debug}: Whether a log of messages should be kept for reporting.
    * @param newDebug Whether a log of messages should be kept for reporting.
    * @see #getLog()
    * @see #log(String)
    */
   public void setDebug(boolean newDebug) { debug = newDebug; }

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
   protected void setLog(Vector<String> newLog) { log = newLog; }
   
   /**
    * The edited version of the graph.
    * @see #getEditedGraph()
    * @see #setEditedGraph(Graph)
    */
   protected Graph editedGraph;
   /**
    * Getter for {@link #editedGraph}: The edited version of the graph.
    * @return The edited version of the graph.
    */
   public Graph getEditedGraph() { return editedGraph; }
   /**
    * Setter for {@link #editedGraph}: The edited version of the graph.
    * @param newEditedGraph The edited version of the graph.
    */
   public void setEditedGraph(Graph newEditedGraph) { editedGraph = newEditedGraph; }

   
   /**
    * The maximum length of a list used for mapping annotations of the same layer but in different graphs to each other.
    * <p>For word and segment layers, the arrays used for shortest-edit-path computation
    * can get very very large. In order to avoid running out of memory, we split the lists into
    * overlapping chunks and map overlapping chunks. This doesn't strictly give us a guaranteed 
    * shortest edit path, but it works for realistic cases and terminates in a realistic time 
    * with a realistic amount of memory available.
    * <p>Default is 500. A value of 0 means don't used overlapping chunks.
    * @see #getArraySizeLimit()
    * @see #setArraySizeLimit(int)
    */
   protected int arraySizeLimit = 500;
   /**
    * Getter for {@link #arraySizeLimit}: The maximum length of a list used for mapping annotations of the same layer but in different graphs to each other.
    * Default is 500. A value of 0 means don't used overlapping chunks.
    * @return The maximum length of a list used for mapping annotations of the same layer but in different graphs to each other.
    */
   public int getArraySizeLimit() { return arraySizeLimit; }
   /**
    * Setter for {@link #arraySizeLimit}: The maximum length of a list used for mapping annotations of the same layer but in different graphs to each other.
    * @param newArraySizeLimit The maximum length of a list used for mapping annotations of the same layer but in different graphs to each other. A value of 0 means don't used overlapping chunks.
    */
   public void setArraySizeLimit(int newArraySizeLimit) { arraySizeLimit = newArraySizeLimit; }


   /**
    * Initial duration of early/layer children, out of order annotations, etc. Default value is 0.00001.
    * @see #getSmidgin()
    * @see #setSmidgin(double)
    */
   protected double smidgin = 0.00001;
   /**
    * Getter for {@link #smidgin}: Initial duration of early/layer children, out of order annotations, etc.
    * @return Initial duration of early/layer children, out of order annotations, etc.
    */
   public double getSmidgin() { return smidgin; }
   /**
    * Setter for {@link #smidgin}: Initial duration of early/layer children, out of order annotations, etc.
    * @param newSmidgin Initial duration of early/layer children, out of order annotations, etc.
    */
   public void setSmidgin(double newSmidgin) { smidgin = newSmidgin; }


   /** Layer shema being used */
   private Schema schema = null;

   /** Default edit-path comparator for annotations */
   private IEditComparator<Annotation> defaultComparator = new IEditComparator<Annotation>()
   {
      int NO_WAY = 200; // weight for ensuring they don't map to each other
      MinimumEditPathString stringComparator = new MinimumEditPathString();
      MinimumEditPathString stringComparatorAvoidSubstitution = new MinimumEditPathString(
	 new DefaultEditComparator<Character>(1, 1, 2)); // change is more costly

      /**
       * Compares two sequence elements, and evaluates the distance between them.
       * @param from The element from the source sequence, which may be null,
       * @param to The element from the destination sequence, which may be null.
       * @return An edit step between the two elements. {@link EditStep#getFrom()} is set to <var>from</var>, {@link EditStep#getTo()} is set to <var>to</var>, {@link EditStep#getDistance()} is set to the computed edit distance between these two elements, and {@link EditStep#getOperation()} is set to either <var>EditStep.StepOperation.NONE</var> or <var>EditStep.StepOperation.CHANGE</var>.
       */
      public EditStep<Annotation> compare(Annotation a1, Annotation a2)
      {
	 EditStep<Annotation> step = new EditStep<Annotation>(
	    a1, a2, 0, EditStep.StepOperation.NONE);
	 if (a1 == null)
	 {
	    if (a2 != null)
	    {
	       step.setStepDistance(1);
	       step.setOperation(EditStep.StepOperation.CHANGE);
	    }
	    // if both are null, we fall through to the return, which amounts to no change
	 }
	 else if (a2 == null)
	 {
	    step.setStepDistance(1);
	    step.setOperation(EditStep.StepOperation.CHANGE);
	 }
	 else 
	 { // two annotations to compare
	    int iWeight = 0;

	    if (a1.containsKey("@other") || a2.containsKey("@other"))
	    { // already mapped
	       if (a1.get("@other") == null || a2.get("@other") == null || a1.get("@other") != a2)
	       { // not mapped to each other
		  iWeight += NO_WAY; // definitely don't want to map them
	       }
	       // else they're already mapped together, so iWeight = 0 is good.
	    }
	    else
	    { // not already mapped
	       // check labels (ignoring punctuation etc.)
	       if (!a1.getLabel().equals(a2.getLabel()))
	       {
		  // ignore punctuation and case by default
		  String s1 = a1.getLabel().replaceAll("[^\\p{javaLetter}\\p{javaDigit}]","").toLowerCase();
		  String s2 = a2.getLabel().replaceAll("[^\\p{javaLetter}\\p{javaDigit}]","").toLowerCase();
		  if (s1.length() <= 0 || s2.length() <= 0 
		      || a1.getLayer().get("@type").equals("D")) // TODO formalise layer types and compare phonological comparison with more sophistication
		  {
		     s1 = a1.getLabel();
		     s2 = a2.getLabel(); // for all-punctuation annotations
		  }
		  int iDistance = s1.length() <= 2 || s2.length() <= 2?
		     // really short strings don't as easily allow subsitutions
		     stringComparatorAvoidSubstitution.levenshteinDistance(s1, s2)
		     // but longer strings use standard edit costs
		     :stringComparator.levenshteinDistance(s1, s2);
		  int iMagnifier = 1; // magnify this because anchor offsets also contribute
		  // really short words have to be really similar
		  if (s1.length() <= 2 || s2.length() <= 2) iMagnifier = 3; 
		  iWeight += (iDistance * iMagnifier);
	       } // check labels

	       // an instant cannot map to a non-instant
	       if (a1.getInstantaneous() != a2.getInstantaneous())
	       {
		  iWeight += NO_WAY;
	       }
	       else
	       { // neither an instant, or both an instant
		  // distance is as important as the reliability of the least reliable annotations anchors
		  // i.e. if a1 & a2 have matching alignments (both default, both user-aligned, etc.)
		  // then the weight of the alignment is as heavy as the alignment
		  // but if a1 has default alignments and a2 has user-alignments, then importance is low
		  // because this is probably an alignment update or an unaligned update of aligned annotations
		  // alternatively, if the annotation has a mixture of trustworthyness, weight will be higher
		  double dImportance = Math.min(
		     (double)(Utility.getConfidence(a1.getStart(), Constants.CONFIDENCE_MANUAL)
			      + Utility.getConfidence(a1.getEnd(), Constants.CONFIDENCE_MANUAL)),
		     (double)(Utility.getConfidence(a2.getStart(), Constants.CONFIDENCE_MANUAL)
			      + Utility.getConfidence(a2.getEnd(), Constants.CONFIDENCE_MANUAL)))
		     // divided by CONFIDENCE_MANUAL, to make it near 1
		     / (double)(Constants.CONFIDENCE_MANUAL * 2);
		  // however, for "word" and "phone", which are frequently merged between aligned
		  // and unaligned versions, and which should be merged by label only 
		  // we ignore anchors
		  if (a1.getLayerId().equals(schema.getWordLayerId()) // word layer
		      // or (probably) phone layer
		      || (a1.getLayer().getParentId().equals(schema.getWordLayerId())
			  && a1.getLayer().getAlignment() == Constants.ALIGNMENT_INTERVAL))
		  {
		     dImportance = 0.01;
		  }			
		  // instantaneous annotations need to have more similar offsets than intervals
		  if (a1.getInstantaneous()) // && a2.getInstantaneous(), but we know it must be
		  {
		     dImportance *= 2.0;
		  }
		  Double dDistance = a1.maximumPairedOffsetDifference(a2);
		  if (dDistance != null && dDistance != 0)
		  {	
		     // we want to ensure that overlapping annotations are selected over non-overlapping ones
		     if (dImportance > 0)
		     {
			if (dDistance > 0)
			{ // no overlap
			   // prefer overlap over none
			   iWeight += (int)(dDistance * dImportance * 2);
			}
			else
			{  // overlap
			   // when choosing between fragments of a split-up annotation, choose the 
			   // fragment that overlaps the most
			   double dOverlapMagnitude = Math.abs(a1.minimumOffsetDifference(a2))
			      // but if the length difference is great, make it high cost anyway
			      / ((a1.getDuration() + a2.getDuration())/2);
			   dOverlapMagnitude *= 3; // soften the impact of this magically
			   iWeight += (int)
			      (Math.min(
				 (double)NO_WAY, // make sure this tops out at NO_WAY, to avoid overflow
				 Math.abs((-dDistance * dImportance / dOverlapMagnitude))));
			}
		     }
		     else 
		     { 
			// while distance doesn't contribute to the weight, REALLY BIG distances shouldn't map
			if (dDistance > 0)
			{ // no overlap
			   if (dDistance > 30)
			   {
			      iWeight += NO_WAY;
			   }
			   else if (Math.abs(a1.getDuration() - a2.getDuration()) > 10) // words differ in length by this much
			   {
			      iWeight += NO_WAY;
			   }
			}
			else
			{ // overlap - should be too different at all
			   if (-dDistance > 10)  iWeight += NO_WAY;
			}
		     }
		  } // distant annotation
	       } // neither an instant, or both an instant
	    } // not already mapped

	    step.setStepDistance(iWeight);
	    if (!a1.getLabel().equals(a2.getLabel())) 
	    { // label would actually change
	       step.setOperation(EditStep.StepOperation.CHANGE);
	    }
	 }
	 return step;
      }
      
      /**
       * The distance for deleting the given element.
       * @param from The element that would be deleted, which may be null.
       * @return An edit step with {@link EditStep#getDistance()} set to the distance for deleting the given element. {@link EditStep#getFrom()} is set to <var>from</var>, and {@link EditStep#getOperation()} is set to <var>EditStep.StepOperation.DELETE</var>
       */
      public EditStep<Annotation> delete(Annotation a1)
      {
	 return new EditStep<Annotation>(a1, null, 1, EditStep.StepOperation.DELETE);
      }

      /**
       * The distance for inserting the given element.
       * @param from The element that would be inserted, which may be null.
       * @return An edit step with {@link EditStep#getDistance()} set to the distance for inserting the given element. {@link EditStep#getTo()} is set to <var>to</var>, and {@link EditStep#getOperation()} is set to <var>EditStep.StepOperation.INSERT</var>
       */
      public EditStep<Annotation> insert(Annotation a2)
      {
	 return new EditStep<Annotation>(null, a2, 1, EditStep.StepOperation.INSERT);
      }
   };
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public Merger()
   {
   } // end of constructor

   /**
    * Constructor with edited graph.
    * @param editedGraph The edited version of the graph.
    */
   public Merger(Graph editedGraph)
   {
      setEditedGraph(editedGraph);
   } // end of constructor

   // IGraphTransformer method

   /**
    * Merges {@link #editedGraph} into the given graph.  
    * The changes are detected and applied to the <var>graph</var>, and returned in a vector of 
    * {@link Change} objects.
    * <p>Assumptions:
    * <ul>
    *  <li><var>editedGraph</var> represents a possibly partial 
    *   (i.e. with a subset of layers) version <var>graph</var> with some changes applied to it.</li>
    *  <li>{@link #editedGraph} is valid
    *   - e.g. that {@link Validator} has been applied to it before merging.</li>
    *  <li>{@link #editedGraph} has no proposed changes (i.e. {@link Graph#commit()} has been called)</li>
    *  <li>Participants are identified in both graphs using turn annotation labels,
    *   and this is done using the same annotation labels.</li>
    * </ul>
    * The IDs of Anchors and Annotations in <var>editedGraph</var> are not assumed to correspond
    * to IDs in <var>graph</var>. Only changes of equal or higher {@link Constants#CONFIDENCE} 
    * will be applied, so that automatic changes do not override prior manual ones.
    * Once merging is finished, <var>graph</var> may be in an invalid state, and should be made 
    * valid using {@link Validator}.
    * @param graph The graph to merge changes into.
    * @return The changes introduced by the tranformation.
    * @throws TransformationException If the transformation cannot be completed.
    */
   public Vector<Change> transform(Graph graph) 
      throws TransformationException
   {
      if (debug) setLog(new Vector<String>());
      setErrors(new Vector<String>());
      schema = graph.getSchema();

      // TODO maybe generated dummy turns in editedGraph
      // TODO maybe generated dummy participants in editedGraph

      // phase 1. - map annotations in graph to annotations in editedGraph horizontally
      graph.put("@other", editedGraph); // map graphs together manually, to help top-level
      editedGraph.put("@other", graph); // parent determination
      for (Layer layer : graph.getLayersTopDown())
      {
	 // if editedGraph has this layer
	 if (!layer.getId().equals("graph"))
	 {
	    if (editedGraph.getLayer(layer.getId()) != null)
	    {
	       TreeSet<Annotation> uneditedAnnotations 
		  = new TreeSet<Annotation>(new AnnotationComparatorByAnchor()); // TODO should these prioritise ordinal over anchor?
	       uneditedAnnotations.addAll(graph.getAnnotations(layer.getId()));
	       
	       TreeSet<Annotation> editedAnnotations 
		  = new TreeSet<Annotation>(new AnnotationComparatorByAnchor());
	       editedAnnotations.addAll(editedGraph.getAnnotations(layer.getId()));
	       
	       mapAnnotationsForMerge(layer, uneditedAnnotations, editedAnnotations);
	    }
	 }
      } // next layer

      // phase 2. - reconcile unmapped annotations
      for (Layer layer : graph.getLayersTopDown())
      {
	 // if editedGraph has this layer
	 if (editedGraph.getLayer(layer.getId()) != null)
	 {
	    createDestroyAnnotationsForMerge(layer, graph);
	 }
      } // next layer

      Vector<Change> changes = new Vector<Change>();
      return changes;
   }
   
   /**
    * Maps annotations from another fragment to annotations in this fragment, in order to then compute change deltas.
    * <p>Corresponding annotations in each graph are linked by having the "@other" attribute set.
    * <p>Only annotations with the same participant (if any) can be linked as counterparts.
    * @param layer Layer definition to use.
    * @param these Annotations from one layer in the graph to be merged into.
    * @param those Annotations from the same layer in {@link #editedGraph}.
    * @throws TransformationException
    */
   public void mapAnnotationsForMerge(Layer layer, SortedSet<Annotation> these, SortedSet<Annotation> those)
    throws TransformationException
   {
      HashMap<String,Vector<Annotation>> theseByParticipant = new HashMap<String,Vector<Annotation>>();
      HashMap<String,Vector<Annotation>> thoseByParticipant = new HashMap<String,Vector<Annotation>>();
      // check if this layer has participant assigned
      boolean splitByParticipant = false;
      // if the schema specifies a turn layer
      if (layer.getId().equals(schema.getTurnLayerId()))
      {
	 splitByParticipant = true;
      }
      else 
      {
	 if (schema.getTurnLayerId() != null
	     // and that layer is present in editedGraph
	     && editedGraph.getLayer(schema.getTurnLayerId()) != null)
	 {
	    for (Layer ancestor : layer.getAncestors())
	    {
	       if (ancestor.getId().equals(schema.getTurnLayerId()))
	       {
		  splitByParticipant = true;
		  break;
	       }
	    } // next ancestor
	 } // turn layer is defined for this schema
      }
      if (!splitByParticipant)
      { // just two straight lists
	 theseByParticipant.put("", new Vector<Annotation>(these));
	 thoseByParticipant.put("", new Vector<Annotation>(those));
      }
      else
      { // split annotations out by participant
	 // annotations from different participants are never paired, so split the lists by 
	 // participant and then map them 
	 // - this way, simultaneous speech doesn't turn into a whole bunch of unnecessary adds and deletes
	 // - this should improve memory usage too, because, with luck, the two lists to get minimum edit
	 //   path from are shorter in each case
	 
	 for (Annotation an : these)
	 {
	    String who = "";
	    Annotation turn = layer.getId().equals(schema.getTurnLayerId())?
	       an:an.my(schema.getTurnLayerId());
	    if (turn != null) who = turn.getLabel();
	    if (!theseByParticipant.containsKey(who))
	    {
	       theseByParticipant.put(who, new Vector<Annotation>());
	    }
	    theseByParticipant.get(who).add(an);
	 } // next annotation
	 for (Annotation an : those)
	 {
	    String who = "";
	    Annotation turn = layer.getId().equals(schema.getTurnLayerId())?
	       an:an.my(schema.getTurnLayerId());
	    if (turn != null) who = turn.getLabel();
	    if (!thoseByParticipant.containsKey(who))
	    {
	       thoseByParticipant.put(who, new Vector<Annotation>());
	    }
	    thoseByParticipant.get(who).add(an);
	 } // next annotation
	 // ensure both collections have the same keys
	 for (String who : theseByParticipant.keySet()) 
	    if (!thoseByParticipant.containsKey(who)) 
	       thoseByParticipant.put(who, new Vector<Annotation>());
	 for (String who : thoseByParticipant.keySet()) 
	    if (!theseByParticipant.containsKey(who)) 
	       theseByParticipant.put(who, new Vector<Annotation>());
      } // split out annotations by participant

      for (String who : theseByParticipant.keySet()) 
      {
	 Vector<Annotation> theseForWho = theseByParticipant.get(who);
	 Vector<Annotation> thoseForWho = thoseByParticipant.get(who);

	 // break collections into overlapping chunks to conserve memory

	 // two lists of lists of annotations
	 Vector<Vector<Annotation>> theses = new Vector<Vector<Annotation>>();
	 Vector<Vector<Annotation>> thoses = new Vector<Vector<Annotation>>();
	 
	 // if we're under size, or we're not doing chunking, just process the two given lists
	 boolean overlappingChunking = getArraySizeLimit() > 0 
	    && theseForWho.size() > arraySizeLimit && thoseForWho.size() > arraySizeLimit;
	 if (!overlappingChunking)
	 {
	    theses.add(theseForWho);
	    thoses.add(thoseForWho);
	 }
	 else
	 { // oversize
	    // break the lists into proportionate chunks that overlap
	    int iNumberOfDiscreteChunks = (theseForWho.size() + (arraySizeLimit-1)) / arraySizeLimit;
	    iNumberOfDiscreteChunks = Math.max(
	       iNumberOfDiscreteChunks, (thoseForWho.size() + (arraySizeLimit-1)) / arraySizeLimit);
	    theses = breakIntoOverlappingChunks(theseForWho, iNumberOfDiscreteChunks);
	    thoses = breakIntoOverlappingChunks(thoseForWho, iNumberOfDiscreteChunks);	    
	    assert theses.size() == thoses.size() 
	       : "theses.size() == thoses.size(): " 
	       + theseForWho.size() + " (" + theses.size() + ") " 
	       + thoseForWho.size() + " (" + thoses.size() + ") - " + iNumberOfDiscreteChunks;	 
	 } // oversize list

	 // for each pair of lists
	 Enumeration<Vector<Annotation>> enThese = theses.elements();
	 Enumeration<Vector<Annotation>> enThose = thoses.elements();
	 while (enThese.hasMoreElements())
	 {
	    // map the list elements
	    Vector<Annotation> theseAnnotations = enThese.nextElement();
	    Vector<Annotation> thoseAnnotations = enThose.nextElement();

	    if (overlappingChunking)
	    {
	       // the purpose of using overlapping chunks is to enable mapping trailing elements
	       // from one chunk to leading elements in the next chunk. in order to prevent
	       // internal unmapped elements from mapping on a second pass, the already-mapped
	       // section at the beginning of each list is removed
	       // - i.e. all leading annotations up to the last mapped one
	       removeLeadingMappedAnnotations(theseAnnotations);
	       removeLeadingMappedAnnotations(thoseAnnotations);
	    }

	    // find minimum edit distance
	    MinimumEditPath<Annotation> mp = new MinimumEditPath<Annotation>(defaultComparator);
	    List<EditStep<Annotation>> path = mp.minimumEditPath(theseAnnotations, thoseAnnotations);
	    // introduce mapped annotations to each other
	    for (EditStep<Annotation> step : path)
	    {
	       if (step.getFrom() != null && step.getTo() != null)
	       {
		  step.getFrom().put("@other", step.getTo());
		  step.getTo().put("@other", step.getFrom());
	       }
	    }
	 } // next chunk pair
      } // next who
   } // end of mapAnnotationsForMerge()

   /**
    * Create new annotations we don't have that exist in the other graph, and mark annotations that
    * don't exist in the other graph for deletion.
    * <p>This method assumes that {@link #mapAnnotationsForMerge(Layer,SortedSet,SortedSet)} has already
    * been called and thus annotations have had their "@other" attributes set appropriately.
    * <p>New annotations are also given new anchors, and if surrounding annotations (on the same layer)
    * share anchors in the edited version, then corresponding surrounding annotations in the original
    * will be linked to the new anchors.
    * @param layer The layer to traverse.
    * @param graph The graph to add changes to.
    * @return The resulting Create/Destroy changes.
    * @throws TransformationException
    */
   protected Vector<Change> createDestroyAnnotationsForMerge(Layer layer, Graph graph)
      throws TransformationException
   {
      Vector<Change> changes = new Vector<Change>();
      // unmapped annotations in graph are for deletion
      for (Annotation an : graph.getAnnotations(layer.getId()))
      {
	 if (!an.containsKey("@other"))
	 {
	    changes.add( // track changes of:
	       an.destroy());
	 }
      } // next annotation

       // might need these later:
      String saturatedParentLayerId = layer.getSaturated()?layer.getParentId():null;
      String layerId = layer.getId();
      
      // unmapped annotations of theirs are for addition
      Annotation anLastOriginal = null;
      for (Annotation anEdited : editedGraph.getAnnotations(layerId))
      {
	 if (!anEdited.containsKey("@other"))
	 {
	    // create a new annotation
	    Annotation newAnnotation = new Annotation(null, anEdited.getLabel(), layerId);
	    newAnnotation.put(Constants.CONFIDENCE, anEdited.get(Constants.CONFIDENCE));
	    newAnnotation.put("@other", anEdited);
	    newAnnotation.create();

	    // anchor it...
	    Anchor start = new Anchor(anEdited.getStart());

	    start.create();
	    if ((Integer)anEdited.getStart().get(Constants.CONFIDENCE) < Constants.CONFIDENCE_AUTOMATIC)
	    { // mark for realignment
	       start.put(Constants.CONFIDENCE, Constants.CONFIDENCE_NONE);
	    }
	    Anchor end = new Anchor(anEdited.getEnd());
	    end.create();
	    if ((Integer)anEdited.getEnd().get(Constants.CONFIDENCE) < Constants.CONFIDENCE_AUTOMATIC)
	    { // mark for realignment
	       end.put(Constants.CONFIDENCE, Constants.CONFIDENCE_NONE);
	    }
	    if (anEdited.getInstantaneous())
	    { // instantaneous annotation
	       end = start;
	    }
	    else
	    { // annotation with duration
	       // don't look for links for instants, because unlinking them from unlinked annotations later is tricky

	       // is the start anchor shared in the edited structure
	       for (Annotation anParallel : anEdited.getStart().getStartingAnnotations())
	       {
		  if (anParallel == anEdited) continue;
		  if (anParallel.getStartId() != anParallel.getOriginalStartId()) continue; // isn't the same anchor any more
		  if (anParallel.containsKey("@other"))
		  {
		     Annotation otherParallel = ((Annotation)anParallel.get("@other"));
		     start = otherParallel.getStart();
		     log(layerId+": "+ newAnnotation.getLabel() 
			 + " sharing start with linked " + logAnnotation(otherParallel));
		     break;
		  }
	       }

	       // is the end anchor shared in the edited structure
	       for (Annotation anParallel : anEdited.getEnd().getEndingAnnotations())
	       {
		  if (anParallel == anEdited) continue;	       
		  if (anParallel.getEndId() != anParallel.getOriginalEndId()) continue; // isn't the same anchor any more
		  if (anParallel.containsKey("@other"))
		  {
		     Annotation otherParallel = ((Annotation)anParallel.get("@other"));
		     end = otherParallel.getEnd();
		     log(layerId+":" + newAnnotation.getLabel() 
			 + " sharing end with linked " + logAnnotation(otherParallel));
		     break;
		  }
	       }
	    }
	    if (start.getId() == null)
	    {
	       graph.addAnchor(start);
	       changes.addAll( // track changes for:
		  start.getChanges());
	    }
	    newAnnotation.setStartId(start.getId());
	    if (end.getId() == null)
	    {
	       graph.addAnchor(end);
	       changes.addAll( // track changes for:
		  end.getChanges());
	    }
	    newAnnotation.setEndId(end.getId());

	    // set parent/peer annotations
	    Annotation editedParent = anEdited.getParent();
	    if (editedParent != null && editedParent.containsKey("@other"))
	    { // counterpart parent set
	       Annotation otherParent = (Annotation)editedParent.get("@other");
	       newAnnotation.setParentId(otherParent.getId());
	    } // counterpart parent set
	    else
	    { // counterpart parent not set
	       // for turns, look for the participant with the same label
	       if (layerId.equals(schema.getTurnLayerId()) 
		   && schema.getParticipantLayerId() != null)
	       {
		  for (Annotation participant : graph.getAnnotations(schema.getParticipantLayerId()))
		  {
		     if (participant.getLabel().equals(newAnnotation.getLabel()))
		     {
			newAnnotation.setParentId(participant.getId());
			break;
		     }
		  } // next participant
	       }
	    } // counterpart parent not set
	    
	    // relink previous annotation to the new anchors? (aligned layers only)
	    // in the edited version of the graph, 
	    // does this annotation share an anchor with some prior annotation?	    
	    if (layer.getAlignment() == Constants.ALIGNMENT_INTERVAL)
	    {
	       // look for one that's not necessarily the very last one
	       for (Annotation anPreviousEdited : anEdited.getStart().endOf(layerId))
	       {
		  if (anPreviousEdited == anEdited) continue;
		  if (anPreviousEdited.containsKey("@other"))
		  {
		     Annotation anPreviousEditedOther = (Annotation)anPreviousEdited.get("@other");
		     if (Utility.getConfidence(anPreviousEditedOther.getEnd())
			 > Utility.getConfidence(newAnnotation.getStart())
			 && anPreviousEditedOther.getEnd().getOffset().doubleValue()
			 != newAnnotation.getStart().getOffset().doubleValue())
		     {
			log(layerId
			    + ": Use offset of end of prior: " + logAnnotation(anLastOriginal) 
			    + " (end "+logAnchor(anPreviousEditedOther.getEnd())+")"
			    + " for start of new: " + logAnnotation(newAnnotation)
			    + " (start "+logAnchor(newAnnotation.getStart())+")");
			int newStatus = Math.min(
			   Utility.getConfidence(graph.getAnchor(anPreviousEditedOther.getOriginalEndId())),
			   Utility.getConfidence(start));
			if (newStatus <= Constants.CONFIDENCE_DEFAULT) 
			{
			   newStatus = Math.max(
			      Utility.getConfidence(graph.getAnchor(anPreviousEditedOther.getOriginalEndId())),
			      Utility.getConfidence(start));
			}
			if (Utility.getConfidence(end) > Constants.CONFIDENCE_DEFAULT 
			    && end.getOffset() <= anPreviousEditedOther.getEnd().getOffset())
			{
			   newStatus = Constants.CONFIDENCE_NONE;
			}
			if (start.getChange() == Change.Operation.Create
			    && newStatus > Constants.CONFIDENCE_AUTOMATIC)
			{ // using a new anchor, and replacing a manual anchor
			   // set the original attributes instead of a delta
			   // this is so it can't later be reverted to its 'new' values
			   start.commit();
			   start.create();
			}
			changes.addAll( // track changes of:
			   start.setOffset(
			      anPreviousEditedOther.getEnd().getOffset()));
			start.put(Constants.CONFIDENCE, newStatus);
		     } // previous more reliable
		     if (anPreviousEditedOther.getStart() != start)
		     {			
			log(
			   layerId
			   + ": Share anchor with prior: " + logAnnotation(anLastOriginal) 
			   + " and new: " + logAnnotation(newAnnotation)
			   + " using " + logAnchor(start));
			changeEndWithRelatedAnnotations(
			   anPreviousEditedOther, newAnnotation.getStart(),
			   // we don't re-link a related parent annotation - we might be inserting a new child
			   saturatedParentLayerId);
			break;
		     }
		  } // has counterpart
	       } // next
	    } // interval layer

	    graph.addAnnotation(newAnnotation);
	    log(layerId + ": Adding " + logAnnotation(newAnnotation));
	    changes.addAll( // track changes for
	       newAnnotation.getChanges());
	 } // new annotation
	 else
	 { // existing annotation
	    // check whether this anchor should be unlinked from prior ones that are not linked
	    // in the edited graph
	    Annotation anOriginal = (Annotation)anEdited.get("@other");
	    // copy the vector so that we don't get concurrent modification problems
	    Vector<Annotation> vEndAnnotations = new Vector<Annotation>(anOriginal.getStart().endOf(layerId)); 
	    for (Annotation anOriginalLinkedPrior : vEndAnnotations)
	    {
	       // no edited counterpart?
	       if (!anOriginalLinkedPrior.containsKey("@other")) continue;
	       // linked in the edited graph?
	       if (((Annotation)anOriginalLinkedPrior.get("@other")).getEnd() == anEdited.getStart()) continue;
	       // hasn't already been unlinked
	       if (anOriginalLinkedPrior.getEnd() != anOriginal.getStart()) continue;
	       // unlink the prior annotation from this one
	       Anchor newPriorEndAnchor = new Anchor(anOriginal.getStart());
	       newPriorEndAnchor.create();
	       changes.addAll(newPriorEndAnchor.getChanges());
	       log(
		  layerId+": Unsharing end of prior: " 
		  + logAnnotation(anOriginalLinkedPrior) + " and start of " 
		  + logAnnotation(anOriginal));
	       changeEndWithRelatedAnnotations(anOriginalLinkedPrior, newPriorEndAnchor);
	    } // next prior linked annotation
	    
	    if (anLastOriginal != null && anLastOriginal.getChange() == Change.Operation.Create)
	    { // mapped to an original annotation, and the last one was new
	       // relink this annotation to new anchor?
	       // in the edited version of the graph,
	       // does this annotation share an anchor with the last annotation?	    
	       if (((Annotation)anLastOriginal.get("@other")).getEnd() == anEdited.getStart()
		   && anLastOriginal.getEnd() != anOriginal.getStart())
	       {
		  // we prefer to use an original anchor (i.e. anOriginal.start)

		  // however we avoid creating an inappropriate parallel annotation
		  if (anLastOriginal.getStart() != anOriginal.getStart())
		  {
		     log(
			layerId+": Share anchor with next: " 
			+ logAnnotation(anLastOriginal) + " and " 
			+ logAnnotation(anOriginal));

		     // TODO or maybe instead of the above avoidance we should use the old anchor but change its offset
		     boolean bLastIsAnInstant = anLastOriginal.getInstantaneous(); // TODO or maybe just by offset?
		     // check we're not creating an instant or backward annotation
		     if (anLastOriginal.getStart().getOriginalOffset()
		     	 >= anOriginal.getStart().getOriginalOffset()
		     	 && !bLastIsAnInstant)
		     {
			if (Utility.getConfidence(anLastOriginal.getStart()) // TODO should be original confidence, but we don't track that!
			    < Utility.getConfidence(anOriginal.getStart()))
			{
			   double dNewOffset = anOriginal.getStart().getOffset() - smidgin;
			   log(
			      layerId+": Moving start of : " 
			      + logAnnotation(anLastOriginal) + " to " 
			      + dNewOffset + " to avoid non-positive length for " + logAnnotation(anLastOriginal));
			   anLastOriginal.getStart().setOffset(dNewOffset);
			   anLastOriginal.getStart().put(Constants.CONFIDENCE, Constants.CONFIDENCE_NONE);
			}
			else
			{
			   double dNewOffset = anOriginal.getStart().getOffset() + smidgin;
			   log(
			      layerId + ": Moving start of : " 
			      + logAnnotation(anOriginal) + " to " 
			      + dNewOffset + " to avoid non-positive length for " + logAnnotation(anLastOriginal));
			   anOriginal.getStart().setOffset(dNewOffset);
			   anOriginal.getStart().put(Constants.CONFIDENCE, Constants.CONFIDENCE_NONE);
			}
		     }

		     // ensure that the end anchor for the last annotation is updated
		     // we don't re-link a related parent annotation - we might be inserting a new child
		     changeEndWithRelatedAnnotations(
			anLastOriginal, anOriginal.getStart(), saturatedParentLayerId);
		     if (bLastIsAnInstant)
		     {
			changeStartWithRelatedAnnotations(anLastOriginal, anOriginal.getStart());
		     }
		  } // anLastOriginal.getStart() != anOriginal.getStart()
		  else
		  { // anLastOriginal.getStart() == anOriginal.getStart()
		     // we don't generally do the following because it screws up annotation
		     // insertion when the new, low-status anchors are far from the
		     // old, high-status anchors
		     log(
			layerId+ ": Share anchor with previous: " 
			+ logAnnotation(anLastOriginal) + " and " 
			+ logAnnotation(anOriginal));
		     // we don't re-link a related parent annotation - we might be inserting a new child
		     changeStartWithRelatedAnnotations(
			anOriginal, anLastOriginal.getEnd(), saturatedParentLayerId);
		  } // anLastOriginal.getStart() == anOriginal.getStart()		  
	       } // last annotation and this one are (now) linked	    
	    } // mapped, and the last one was a change
	 } // not a new annotation
	 
	 // copy ordinals - these will be updated later, but this ensures that annotations
	 // come out in order for following operations, despite crazy anchor values
	 // TODO test case for this - insert Anew before Aold, they have same ordinals but Anew.start > original Aold.start
	 Annotation anOriginal = (Annotation)anEdited.get("@other");
	 if (anOriginal.getOrdinal() != anEdited.getOrdinal())
	 {
	    log(
	       layerId+": changing ordinal of: " + logAnnotation(anOriginal) 
	       + " from " + anOriginal.getOrdinal() + " to " + anEdited.getOrdinal());
	    anOriginal.setOrdinal(anEdited.getOrdinal());
	 }
	 
	 anLastOriginal = anOriginal;
      } // next edited annotation
      return changes;
   } // end of createDeleteAnnotationsForMerge()
      
   /**
    * Break the given list into overlapping chunks using the given chunk size.
    * @param list The list to break up.
    * @param iNumberOfDiscreteChunks Number of chunks the results list of chunks should have
    * @return A list of chunks
    */
   protected Vector<Vector<Annotation>> breakIntoOverlappingChunks(Vector<Annotation> list, int iNumberOfDiscreteChunks)
   {
      Vector<Vector<Annotation>> vv = new Vector<Vector<Annotation>>();
      Vector<Annotation> va = new Vector<Annotation>();
      vv.add(va);
      Vector<Annotation> vb = null;
      double iChunkSize = ((double)list.size()) / iNumberOfDiscreteChunks;
      double iChangeSize = iChunkSize / 2;
      int i = 0;
      Iterator<Annotation> it = list.iterator();
      while (it.hasNext())
      {
	 Annotation an = it.next();
	 if (i > vv.size() * iChangeSize)
	 {
	    if (vb == null)
	    {
	       vb = new Vector<Annotation>();
	       vv.add(vb);
	    }
	    else
	    {
	       va = vb;
	       vb = new Vector<Annotation>();
	       vv.add(vb);
	    }
	 }
	 i++;
	 
	 va.add(an);
	 if (vb != null) vb.add(an);
      } // next annotation
      vv.remove(vv.lastElement());  // remove the last half-chunk
      return vv;
   } // end of breakIntoOverlappingChunks()

   /**
    * Removes leading annotations from the list, up to and including the last mapped annotation
    * (i.e. annotation with a non-null counterpart)
    * @param annotations A list of annotations
    */
   public void removeLeadingMappedAnnotations(Vector<Annotation> annotations)
   {
      if (annotations.size() == 0) return;

      // Start from the end
      ListIterator<Annotation> iAnnotations = annotations.listIterator(annotations.size()-1);
      iAnnotations.next();

      // go backwards until a mapped annotation is found
      while (iAnnotations.hasPrevious())
      {
	 if (iAnnotations.previous().containsKey("@other"))
	 {
	    iAnnotations.remove();
	    break;
	 }
      }

      // remove all annotations from here until the beginning
      while (iAnnotations.hasPrevious())
      {
	 iAnnotations.previous();
	 iAnnotations.remove();
      }
   } // end of removeLeadingMappedAnnotations()

   /**
    * Sets the Start Anchor of the given annotation, and also the start anchors of related annotations that start in the same place.
    * @param annotation The annotation to change the start anchor of.
    * @param newStartAnchor The new start anchor.
    * @return The changes made during this operation.
    */
   public Vector<Change> changeStartWithRelatedAnnotations(Annotation annotation, Anchor newStartAnchor)
   {
      return changeStartWithRelatedAnnotations(annotation, newStartAnchor, new HashSet<String>());
   }
   /**
    * Sets the Start Anchor of the given annotation, and also the start anchors of related annotations that start in the same place.
    * @param annotation The annotation whose start anchor will be changed.
    * @param newStartAnchor The new start anchor.
    * @param layerIdToExclude A layer to exclude when updating related annotations.
    * @return The changes made during this operation.
    */
   public Vector<Change> changeStartWithRelatedAnnotations(Annotation annotation, Anchor newStartAnchor, String layerIdToExclude)
   {
      HashSet<String> exclude = new HashSet<String>();
      if (layerIdToExclude != null) exclude.add(layerIdToExclude);
      return changeStartWithRelatedAnnotations(annotation, newStartAnchor, exclude);
   }
   /**
    * Sets the StartAnchor of the given annotation, and also the start anchors of related annotations that start in the same place.
    * @param annotation The annotation whose start anchor will be changed.
    * @param newStartAnchor The new start anchor.
    * @param layerIdsToExclude Layers to exclude when updating related annotations.
    * @return The changes made during this operation.
    */
   public Vector<Change> changeStartWithRelatedAnnotations(Annotation annotation, Anchor newStartAnchor, Set<String> layerIdsToExclude)
   {
      Vector<Change> changes = new Vector<Change>();
      log("changeStartWithRelatedAnnotations " 
	  + logAnnotation(annotation) + " to " + logAnchor(newStartAnchor) 
	  + (layerIdsToExclude.size() > 0?" excluding layers " + layerIdsToExclude:""));

      Anchor aOriginalStart = annotation.getStart();
      Anchor aOriginalEnd = annotation.getEnd();      
      changes.addAll( // record changes generated by:
	 annotation.setStart(newStartAnchor));
      if (aOriginalStart == aOriginalEnd)
      {
	 log(logAnnotation(annotation) + " is instantaneous, changing both anchors.");
	 changes.addAll( // record changes generated by:
	    annotation.setEnd(newStartAnchor));
      }
      
      Layer layer = annotation.getGraph().getLayer(annotation.getLayerId());
      
      // change parallel annotations
      for (Annotation anOther : aOriginalStart.getStartingAnnotations())
      {
	 if (anOther == annotation) continue;
	 if (anOther.getLayerId() == annotation.getLayerId()) continue;
	 if (layerIdsToExclude.contains(anOther.getLayerId())) continue;
	 // has it already been changed?
	 if (!anOther.getStartId().equals(aOriginalStart.getId())) continue;
	 // do they have a relationship that would actually preclude sharing?
	 Layer otherLayer = annotation.getGraph().getLayer(anOther.getLayerId());
	 if (layer != null && otherLayer != null)
	 {
	    if (layer.getParentId().equals(otherLayer.getId()))
	    { // other is parent layer to this
	       if (!layer.getSaturated()) continue; // sparse

	       // this belongs to another parent
	       if (!anOther.getId().equals(annotation.getParentId())) continue;
	    }
	    else if (otherLayer.getParentId().equals(layer.getId()))
	    { // this is parent layer to other
	       if (!otherLayer.getSaturated()) continue; // sparse
	       
	       // this belongs to another parent
	       if (!annotation.getId().equals(anOther.getParentId())) continue;
	    }
	 }
	 boolean bInstant = anOther.getInstantaneous();
	 log("Changing start" +(bInstant?" and end":"")+ " of related annotation " + logAnnotation(anOther) + " to " + logAnchor(newStartAnchor));
	 changes.addAll( // record changes generated by:
	    anOther.setStart(newStartAnchor));
	 if (bInstant)
	 {
	    changes.addAll( // record changes generated by:
	       anOther.setEnd(newStartAnchor));
	 }
      } // next parallel anchor starting here

      if (!layerIdsToExclude.contains(annotation.getLayerId()))
      {
	 // also change end anchor of annotations on the same layer
	 layerIdsToExclude.add(annotation.getLayerId()); // prevents infinite recursion
	 Vector<Annotation> vRelatedAnnotations = new Vector<Annotation>();
	 vRelatedAnnotations.addAll(aOriginalStart.endOf(annotation.getLayerId()));
	 for (Annotation anPrevious : vRelatedAnnotations)
	 {
	    if (anPrevious.getChange() == Change.Operation.Destroy) continue;
	    // only if it really still follows
	    if (!anPrevious.getEndId().equals(aOriginalStart.getId())) continue;

	    if (!anPrevious.getStartId().equals(anPrevious.getEndId())
		&& newStartAnchor.getId().equals(anPrevious.getStartId()))
	    {
	       log("Not changing end of related annotation " + logAnnotation(anPrevious) 
		   + " to avoid creating new instant");
	       continue;
	    }
	    if (!anPrevious.getParentId().equals(annotation.getParentId()))
	    {
	       log("Not changing end of related annotation " + logAnnotation(anPrevious) 
		   + " - different parents");
	       continue;
	    }
	    log("Changing end of previous linked annotation " + logAnnotation(anPrevious) 
		+ " to " + logAnchor(newStartAnchor));
	    changes.addAll( // record changes generated by:
	       changeEndWithRelatedAnnotations(anPrevious, newStartAnchor, layerIdsToExclude));
	 } // next ending annotation
      } // not excluding annotation's own layer
      return changes;
   } // end of changeStartWithRelatedAnnotations()

   /**
    * Sets the End Anchor of the given annotation, and also the end anchors of related annotations that end in the same place.
    * @param annotation The annotation whose end anchor should be changed.
    * @param newEndAnchor The new end anchor.
    * @return The changes made during this operation.
    */
   public Vector<Change> changeEndWithRelatedAnnotations(Annotation annotation, Anchor newEndAnchor)
   {
      return changeEndWithRelatedAnnotations(annotation, newEndAnchor, new HashSet<String>());
   }
   /**
    * Sets the End Anchor of the given annotation, and also the end anchors of related annotations that end in the same place.
    * @param annotation The annotation whose end anchor should be changed.
    * @param newEndAnchor The new end anchor.
    * @param layerIdToExclude A layer to exclude when updating related annotations.
    * @return The changes made during this operation.
    */
   public Vector<Change> changeEndWithRelatedAnnotations(Annotation annotation, Anchor newEndAnchor, String layerIdToExclude)
   {
      HashSet<String> exclude = new HashSet<String>();
      if (layerIdToExclude != null) exclude.add(layerIdToExclude);
      return changeEndWithRelatedAnnotations(annotation, newEndAnchor, exclude);
   }
   /**
    * Sets the End Anchor of the given annotation, and also the end anchors of related annotations that end in the same place.
    * @param annotation The annotation whose end anchor should be changed.
    * @param newEndAnchor The new end anchor.
    * @param layerIdsToExclude Layers to exclude when updating related annotations.
    * @return The changes made during this operation.
    */
   public Vector<Change> changeEndWithRelatedAnnotations(Annotation annotation, Anchor newEndAnchor, Set<String> layerIdsToExclude)
   {
      Vector<Change> changes = new Vector<Change>();
      log("changeEndWithRelatedAnnotations " + logAnnotation(annotation) + " to " + logAnchor(newEndAnchor) + (layerIdsToExclude.size() > 0?" excluding layers " + layerIdsToExclude:""));
      Anchor aOriginalEnd = annotation.getEnd();
      Anchor aOriginalStart = annotation.getStart();
      changes.addAll( // record changes generated by:
	 annotation.setEnd(newEndAnchor));
      if (aOriginalStart == aOriginalEnd)
      {
	 log(logAnnotation(annotation) + " is instantaneous, changing both anchors.");
	 changes.addAll( // record changes generated by:
	    annotation.setStart(newEndAnchor));
      }

      Layer layer = annotation.getGraph().getLayer(annotation.getLayerId());

      for (Annotation anOther : aOriginalEnd.getEndingAnnotations())
      {
	 if (anOther == annotation) continue;
	 if (anOther.getLayerId().equals(annotation.getLayerId())) continue;
	 if (layerIdsToExclude.contains(anOther.getLayerId())) continue;
	 // has it already been changed?
	 if (!anOther.getEndId().equals(aOriginalEnd.getId())) continue;

	 // do they have a relationship that would actually preclude sharing?
	 Layer otherLayer = annotation.getGraph().getLayer(anOther.getLayerId());
	 if (layer != null && otherLayer != null)
	 {
	    if (layer.getParentId().equals(otherLayer.getId()))
	    { // other is parent layer to this
	       if (!layer.getSaturated()) continue; // sparse

	       // this belongs to another parent
	       if (!anOther.getId().equals(annotation.getParentId())) continue;
	    }
	    else if (otherLayer.getParentId().equals(layer.getId()))
	    { // this is parent layer to other
	       if (!otherLayer.getSaturated()) continue; // sparse
	       
	       // this belongs to another parent
	       if (!annotation.getId().equals(anOther.getParentId())) continue;
	    }
	 }
	 boolean bInstant = anOther.getInstantaneous();
	 log("Changing end" +(bInstant?" and start":"")+ " of related annotation " + logAnnotation(anOther) + " to " + logAnchor(newEndAnchor));
	 changes.addAll( // record changes generated by:
	    anOther.setEnd(newEndAnchor));
	 if (bInstant)
	 {
	    changes.addAll( // record changes generated by:
	       anOther.setStart(newEndAnchor));
	 }
      } // next parallel anchor starting here
      if (!layerIdsToExclude.contains(annotation.getLayerId()))
      {
	 // also change start anchor of annotations on the same layer
	 layerIdsToExclude.add(annotation.getLayerId()); // prevents infinite recursion
	 Vector<Annotation> vRelatedAnnotations = new Vector<Annotation>();
	 vRelatedAnnotations.addAll(aOriginalEnd.startOf(annotation.getLayerId()));
	 // vRelatedAnnotations.addAll(aOriginalEnd.getDeltaStartAnnotationsLayer(annotation.getLayerId()));
	 if (vRelatedAnnotations.size() > 0)
	 {
	    int iNonDeletedCount = 0;
	    for (Annotation anNext : vRelatedAnnotations)
	    {
	       if (anNext.getChange() == Change.Operation.Destroy) continue; // ignore deleted ones
	       iNonDeletedCount++;
	       // only if it really still follows
	       if (!anNext.getStartId().equals(aOriginalEnd.getId())) continue;
	       if (!anNext.getStartId().equals(anNext.getEndId())
		   && newEndAnchor.getId().equals(anNext.getEndId()))
	       {
		  log("Not changing end of related annotation " + logAnnotation(anNext) 
		      + " to avoid creating new instant");
		  continue; // if not shared in the other graph, not shared here
	       }
	       log("Changing start of next linked annotation " + logAnnotation(anNext) + " to " + logAnchor(newEndAnchor));
	       changes.addAll( // log the following change:
		  changeStartWithRelatedAnnotations(anNext, newEndAnchor, layerIdsToExclude));
	    } // next starting annotation
	    
	    if (iNonDeletedCount == 0)
	    { // all the 'next' annotations on the same layer are deleted
	       // ensure that annotations that start here on *other* layers come with us
	       // find one related annotation on another layer
	       vRelatedAnnotations.clear();
	       vRelatedAnnotations.addAll(aOriginalEnd.getStartingAnnotations());
	       for (Annotation anNext : vRelatedAnnotations)
	       {
		  if (layerIdsToExclude.contains(anNext.getLayerId())) continue;
		  log("Next has been deleted, using " + logAnnotation(anNext) + " to bring starting annotations too");
		  changes.addAll( // log the following change:
		     changeStartWithRelatedAnnotations(anNext, newEndAnchor, layerIdsToExclude));
		  break; // one should be sufficient to bring the rest along
	       } // next starting annotation	       
	    }  // all the 'next' annotations on the same layer are deleted
	 } // there are 'next' starting annotations
      }
      return changes;
   } // end of changeStartWithRelatedAnnotations()

   /**
    * A representation of the given annotation for logging purposes.
    * @param annotation The annotation to log.
    * @return A representation of the given annotation for loggin purposes.
    */
   protected String logAnnotation(Annotation annotation)
   {
      if (annotation == null) return "[null]";
      return "[" + annotation.getId() + "]" + annotation.get("ordinal") + "#" + annotation.getLabel() + "("+annotation.getStart()+"-"+annotation.getEnd()+")";
   } // end of logAnnotation()

   /**
    * A representation of the given anchor for logging purposes.
    * @param anchor The anchor to log.
    * @return A representation of the given anchor for logging purposes.
    */
   protected String logAnchor(Anchor anchor)
   {
      if (anchor == null) return "[null]";
      return "[" + anchor.getId() + "]" + anchor.getOffset();
   } // end of logAnnotation()
   
   /**
    * Logs a debugging message.
    * @param message The debug message.
    */
   protected void log(String message)
   {
      if (debug)
      {
	 log.add(message);
	 System.out.println(message);
      }
   } // end of log()

} // end of class Merger
