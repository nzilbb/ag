//
// Copyright 2019 New Zealand Institute of Language, Brain and Behaviour, 
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
import java.util.TreeSet;
import java.util.Vector;
import nzilbb.ag.*;

/**
 * Graph transformer that converts simultaneous speech into parallel turns.
 * <p> Changes the graph structure so that simultaneous speech is partititioned off into
 * discreet parallel turns. This undoes the work done by {@link Normalizer}. 
 * <p>Essentially, if two turns overlap, they're divided into four:
 * <ol>
 *  <li>A turn for the first speaker, up until the second speaker starts.</li>
 *  <li>A turn for the first speaker, for the duration of the second speaker's
 *      simultaneous speech.</li> 
 *  <li>A turn for the second speaker, for the same duration as above.</li>
 *  <li>A turn for the second speaker, starting from the time the first speaker stops.</li>
 * </ol>
 * The turns all share anchors, rather than having simultaneous but distinct anchors, so
 * that simultaneous speech can easily be detected (e.g. for converting to the Transcriber
 * format) 
 * <p>Turns are also split on topic changes (i.e. when a topic layer annotation
 * starts/ends), mainly because that suits Transcriber.
 * <p><em>NB</em> If there are anchors with unset offsets, the {@link DefaultOffsetGenerator} is
 * used to assign them.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class UtteranceParallelizer implements IGraphTransformer {
   
   // Attributes:
   
   /**
    * Layer IDs to parallelize, e.g. "turn", "utterance".
    * @see #getLayerIds()
    * @see #setLayerIds(HashSet)
    */
   protected HashSet<String> layerIds = new HashSet<String>();
   /**
    * Getter for {@link #layerIds}: Layer IDs to parallelize, e.g. "turn", "utterance".
    * @return Layer IDs to parallelize, e.g. "turn", "utterance".
    */
   public HashSet<String> getLayerIds() { return layerIds; }
   /**
    * Setter for {@link #layerIds}: Layer IDs to parallelize, e.g. "turn", "utterance".
    * @param newLayerIds Layer IDs to parallelize, e.g. "turn", "utterance".
    */
   public UtteranceParallelizer setLayerIds(HashSet<String> newLayerIds) { layerIds = newLayerIds; return this; }

   // Methods:
   
   /**
    * Default constructor.
    */
   public UtteranceParallelizer() {
   } // end of constructor
   
   /**
    * Constructor from Schema, which automatically adds the turn and utterance layers.
    */
   public UtteranceParallelizer(Schema schema) {
      if (schema.getTurnLayerId() != null)
         addLayerId(schema.getTurnLayerId());
      if (schema.getUtteranceLayerId() != null)
         addLayerId(schema.getUtteranceLayerId());
   } // end of constructor
   
   /**
    * Add a layerId to layerIds for parallelization.
    * @param layerId
    * @return this, so that method calls can be chained together.
    */
   public UtteranceParallelizer addLayerId(String layerId) {
      layerIds.add(layerId);
      return this;
   } // end of addLayerId()


   /**
    * Transforms the graph.
    * @param graph The graph to transform.
    * @return The changes introduced by the tranformation.
    * @throws TransformationException If the transformation cannot be completed.
    */
   public List<Change> transform(Graph graph) throws TransformationException {
      
      // ensure we can track our changes
      ChangeTracker ourTracker = new ChangeTracker();
      ChangeTracker originalTracker = graph.getTracker();
      if (originalTracker == null) {
         graph.setTracker(ourTracker);
         ourTracker.reset(); // in case there were any lingering creates/destroys in the graph
      } else {
         originalTracker.addListener(ourTracker);
      }

      // this only works if offsets are set
      boolean unsetOffsets = graph.getAnchors().values().stream()         
         .map(a->a.getOffset() == null) // stream of 'true if no offset'
         .reduce(Boolean::logicalOr)    // 'or' them together
         .orElse(false);                // false if there are no anchors
      if (unsetOffsets) {
         // use the DefaultOffsetGenerator to ensure offsets are set
         new DefaultOffsetGenerator()
            // only set unset offsets
            .setDefaultOffsetThreshold(Constants.CONFIDENCE_NONE - 1)
            .transform(graph);
      }

      LayerHierarchyTraversal<Vector<String>> bottomUpSelectedLayers
         = new LayerHierarchyTraversal<Vector<String>>(new Vector<String>(), graph.getSchema()) {
               protected void post(Layer layer) {
                  if (layerIds.contains(layer.getId())) result.add(layer.getId());
               }};

      // order utterances by anchor so that simultaneous speech comes out in offset order
      AnnotationComparatorByAnchor byAnchor = new AnnotationComparatorByAnchor();

      for (String layerId : bottomUpSelectedLayers.getResult()) {
         
         // order annotations by anchor so that simultaneous speech comes out in offset order
         TreeSet<Annotation> annotationsByAnchor = new TreeSet<Annotation>(byAnchor);
         for (Annotation a : graph.list(layerId)) annotationsByAnchor.add(a);

         // loop until there are no annotations left...
         while (annotationsByAnchor.size() > 0) {
            
            // find the next batch of annotations that start at the same time
            Double startOffset = null;
            Anchor nextStart = null;
            TreeSet<Annotation> startingHere = new TreeSet<Annotation>(byAnchor);
            Iterator<Annotation> iCurrentAnnotations = annotationsByAnchor.iterator();
            while (iCurrentAnnotations.hasNext()) {
               
               Annotation annotation = iCurrentAnnotations.next();
               if (startOffset == null) {
                  // starting the list
                  startOffset = annotation.getStart().getOffset();
                  startingHere.add(annotation);
                  iCurrentAnnotations.remove();
               } else if (annotation.getStart().getOffset().equals(startOffset)) {
                  // add this to our list
                  startingHere.add(annotation);
                  iCurrentAnnotations.remove();
               } else {
                  // start must be greater, so that becomes the next start offset
                  nextStart = annotation.getStart();
                  break;
               }
            } // next annotation
            
            // now we have a list of annotations that all start at the same time
            
            // find the shortest annotation in the list
            // it must be the last one because AnnotationComparatorByAnchor is longest-first
            Annotation shortestStartingHere = startingHere.last();
            
            // if the shortest one ends before nextStart
            if (nextStart == null
                || shortestStartingHere.getEnd().getOffset() < nextStart.getOffset()) {
               // use its end as the next start
               nextStart = shortestStartingHere.getEnd();
            }
            
            // split any that end after the next start time
            for (Annotation annotation : startingHere) {
               
               if (annotation.getEnd().getOffset() > nextStart.getOffset()) {
                  
                  // split annotation at nextStart 
                  Annotation following = new Annotation(annotation);
                  annotation.setEnd(nextStart);
                  following.create();
                  following.setStart(nextStart);
                  graph.addAnnotation(following);
                                    
                  // new annotation will be processed next time around the main loop
                  annotationsByAnchor.add(following);
                  
                  // move children appropriately
                  for (String childLayerId : annotation.getAnnotations().keySet()) {
                     
                     Vector<Annotation> children // use a copy to avoid concurrent modification
                        = new Vector<Annotation>(annotation.getAnnotations().get(childLayerId));
                     for (Annotation child : children) {
                        
                        // use includesMidpointOf so that t-inclusion doesn't have to be
                        // strict - i.e. children can overlap the parent at this point
                        if (following.includesMidpointOf(child)) {
                           child.setParent(following);
                        }
                     } // next child
                  } // next child layer
               } // ends after next start
            } // next annotation that starts here
         } // process annotationsByAnchor again
      } // next layer
      
      // set the tracker back how it was
      if (originalTracker == null) {
         graph.setTracker(null);
      } else {
         originalTracker.removeListener(ourTracker);
      }
      return new Vector<Change>(ourTracker.getChanges());
   }
} // end of class UtteranceParallelizer
