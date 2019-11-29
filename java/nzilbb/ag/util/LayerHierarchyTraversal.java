//
// Copyright 2015-2019 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.util.Collection;
import java.util.stream.Stream;
import java.util.Comparator;
import java.util.TreeSet;

import nzilbb.ag.*;

/**
 * Traverses {@link Layer}s a {@link Schema}'s Layer hierarchy.
 * <p>This base class handles the traversal. The pre-order and post-order operations (the
 * actual work being done) can be implemented by subclassing to implement 
 * {@link #pre(Layer)} and {@link #post(Layer)}.
 * <p>For example, to print a list of layers in hierarchy order:
 * <pre>
 * LayerHierarchyTraversal&lt;StringBuffer&gt; t = new LayerHierarchyTraversal&lt;StringBuffer&gt;(new StringBuffer(), schema)
 * {
 *   protected void pre(Layer layer)
 *   {
 *     result.append(layer.getId() + "\n");
 *   }
 * };
 * System.out.println(t.getResult().toString());
 * </pre>
 * @author Robert Fromont robert@fromont.net.nz
 */

public class LayerHierarchyTraversal<R>
{
   // Attributes:
   
   /**
    * Default comparator for ordering peer layers.
    * <p>The default ordering is
    * <ol>
    *  <li>non-included layers before included layers (dependency before pos)</li>
    *  <li>less alignment before more alignment (orthography before phone)</li>
    *  <li>fewer peers before more peers (orthography before pos)</li>
    *  <li>childless peers before childful peers (utterance before word)</li>
    *  <li>saturated before sparse (utterance before named entity)</li>
    *  <li>overlapping peers before sequential peers (parse tree before named entity)</li>
    * </ol>
    * @see #peerComparator
    */
   public static final Comparator<Layer> defaultComparator = new Comparator<Layer>() {
         public int compare(Layer l1, Layer l2) {
            if (l1 == l2) return 0;
            // non-included layers before included layers (dependency before pos)
            if (l1.getParentIncludes() != l2.getParentIncludes())
            {
               if (l1.getParentIncludes()) return 1;
               else return -1;
            }
            // less alignment before more alignment (orthography before phone)
            if (l1.getAlignment() != l2.getAlignment())
            {
               if (l1.getAlignment() > l2.getAlignment()) return 2;
               else return -2;
            }
            // fewer peers before more peers, (orthography before pos)
            if (l1.getPeers() != l2.getPeers())
            {
               if (l1.getPeers()) return 3;
               else return -3;
            }
            // childless peers before childful peers (utterance before word)
            if (l1.getChildren().size() != l2.getChildren().size())
            {
               if (l1.getChildren().size() > l2.getChildren().size()) return 4;
               else return -4;
            }
            // saturated before sparse
            if (l1.getSaturated() != l2.getSaturated())
            {
               if (l1.getSaturated()) return -5;
               else return 5;
            }
            // overlapping peers before sequential peers (parse tree before named entity)
            if (l1.getPeersOverlap() != l2.getPeersOverlap())
            {
               if (l1.getPeersOverlap()) return -6;
               else return 6;
            }
            // by display_order
            if (l1.containsKey("@display_order") && l2.containsKey("@display_order")
                && !l1.get("@display_order").equals(l2.get("@display_order")))
            {
               return ((Integer)l1.get("@display_order"))
                  .compareTo((Integer)l2.get("@display_order"));
            }
            // by layerId
            return l1.getId().compareTo(l2.getId());
         }
      };
   
   /**
    * Comparator for ordering peer layers. Set to <tt>null</tt> for order the children are
    * encountered in.
    * @see #defaultComparator
    * @see #getPeerComparator()
    * @see #setPeerComparator(Comparator)
    */
   protected Comparator<Layer> peerComparator = defaultComparator;
   /**
    * Getter for {@link #peerComparator}: Comparator for ordering peer layers.
    * @return Comparator for ordering peer layers, or <tt>null</tt> for order the children
    * are encountered in.
    */
   public Comparator<Layer> getPeerComparator() { return peerComparator; }
   /**
    * Setter for {@link #peerComparator}: Comparator for ordering peer layers.
    * @param newPeerComparator Comparator for ordering peer layers, or <tt>null</tt>
    * for order the children are encountered in.
    */
   public LayerHierarchyTraversal<R> setPeerComparator(Comparator<Layer> newPeerComparator)
   { peerComparator = newPeerComparator; return this; }
   
   /**
    * The schema to be traversed.
    * @see #getSchema()
    * @see #setSchema(Schema)
    */
   protected Schema schema;
   /**
    * Getter for {@link #schema}: The schema to be traversed.
    * @return The schema to be traversed.
    */
   public Schema getSchema() { return schema; }
   /**
    * Setter for {@link #schema}: The schema to be traversed.
    * @param newSchema The schema to be traversed.
    */
   public LayerHierarchyTraversal<R> setSchema(Schema newSchema)
   { schema = newSchema; return this; }
   
   /**
    * The result of the traversal, if required.
    * @see #getResult()
    * @see #setResult(Object)
    */
   protected R result;
   /**
    * Getter for {@link #result}: The result of the traversal, if required.
    * @return The result of the traversal, if required.
    */
   public R getResult() { return result; }
   /**
    * Setter for {@link #result}: The result of the traversal, if required.
    * @param newResult The result of the traversal, if required.
    */
   public LayerHierarchyTraversal<R> setResult(R newResult)
   { result = newResult; return this; }
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public LayerHierarchyTraversal()
   {
   } // end of constructor

   /**
    * Constructor with starting result.
    * @param result The result of the traversal.
    */
   public LayerHierarchyTraversal(R result)
   {
      setResult(result);
   } // end of constructor
 
   /**
    * Constructor with starting result and schema to immediately traverse.
    * @param result The result of the traversal.
    * @param schema The schema to be traversed.
    */
   public LayerHierarchyTraversal(R result, Schema schema)
   {
      setResult(result);
      setResult(traverseSchema(schema));
   } // end of constructor

   /**
    * Constructor with starting result and schema to immediately traverse.
    * The comparator supplied determines the ordering among peer layers.
    * e.g. to achieve the reverse of the default ordering (see {@link #defaultComparator}),
    * set <var>comparator</var> to:
    * <pre>
    * new Comparator&lt;Layer&gt;() { 
    *    public int compare(Layer l1, Layer l2) { 
    *       return -LayerHierarchyTraversal.defaultComparator.compare(l1,l2); 
    *    } }
    * </pre>
    * @param result The result of the traversal.
    * @param comparator Peer layer comparator.
    * @param schema The schema to be traversed.
    */
   public LayerHierarchyTraversal(R result, Comparator<Layer> comparator, Schema schema)
   {
      setResult(result);
      setPeerComparator(comparator);
      setResult(traverseSchema(schema));
   } // end of constructor

   /**
    * Constructor with starting result and a collection of layers to immediately traverse.
    * @param result The result of the traversal.
    * @param layers A collection of layers to traverse.
    */
   public LayerHierarchyTraversal(R result, Collection<Layer> layers)
   {
      setResult(result);
      setResult(traverseLayers(layers));
   } // end of constructor

   /**
    * Constructor with starting result and a collection of layers to immediately traverse.
    * The comparator supplied determines the ordering among peer layers.
    * e.g. to achieve the reverse of the default ordering (see {@link #defaultComparator}),
    * set <var>comparator</var> to:
    * <pre>
    * new Comparator&lt;Layer&gt;() { 
    *    public int compare(Layer l1, Layer l2) { 
    *       return -LayerHierarchyTraversal.defaultComparator.compare(l1,l2); 
    *    } }
    * </pre>
    * @param result The result of the traversal.
    * @param comparator Peer layer comparator.
    * @param layers A collection of layers to traverse.
    */
   public LayerHierarchyTraversal(R result, Comparator<Layer> comparator, Collection<Layer> layers)
   {
      setResult(result);
      setPeerComparator(comparator);
      setResult(traverseLayers(layers));
   } // end of constructor

   /**
    * Traverses the given collection of layers.
    * @param layers The layers to traverse.
    * @return Some result of the traversal, if required.
    */
   public R traverseLayers(Collection<Layer> layers)
   {
      // a top level layer is any layer with "graph" as a parent, 
      // or any layer whose parent is not in the graph 
      // (this ensures all layers are included, even for partial graphs)
      // (this ensures all layers are included, even for partial graphs)
      Stream<Layer> topLevelLayersStream = layers.stream()
         .filter(layer -> !layer.getId().equals("graph"))
         .filter(layer -> layer.getParentId() != null)
         .filter(layer -> layer.getParentId().equals("graph")
                 || !schema.getLayers().containsKey(layer.getParentId()));
      if (peerComparator != null)
      {
         topLevelLayersStream = topLevelLayersStream.sorted(peerComparator);            
      }
      
      // for each top level layer
      topLevelLayersStream.forEach(this::traverseLayer);
      return getResult();
   } // end of traverseLayers()
  
   /**
    * Traverses the given schema.
    * @param schema The schema to traverse.
    * @return Some result of the traversal, if required.
    */
   public R traverseSchema(Schema schema)
   {
      setSchema(schema);
      
      // a top level layer is any layer with "graph" as a parent, 
      // or any layer whose parent is not in the graph 
      // (this ensures all layers are included, even for partial graphs)
      Stream<Layer> topLevelLayersStream = schema.getLayers().values().stream()
         .filter(layer -> !layer.getId().equals("graph"))
         .filter(layer -> layer.getParentId() != null)
         .filter(layer -> layer.getParentId().equals("graph")
                 || !schema.getLayers().containsKey(layer.getParentId()));
      if (peerComparator != null)
      {
         topLevelLayersStream = topLevelLayersStream.sorted(peerComparator);            
      }
      
      // for each top level layer
      topLevelLayersStream.forEach(this::traverseLayer);
      return getResult();
   } // end of traverseSchema()
   
   /**
    * Depth-first recursive method that traverses the layer annotations using the layer
    * hierarchy, calling {@link #pre(Layer)}, then calling itself for all children, then
    * called {@link #post(Layer)}, before returning. 
    * @param layer The layer to traverse.
    */
   protected void traverseLayer(Layer layer)
   {
      pre(layer);
      Collection<Layer> orderedChildren = layer.getChildren().values();
      if (peerComparator != null)
      {
         orderedChildren = new TreeSet<Layer>(peerComparator);
         orderedChildren.addAll(layer.getChildren().values());
      }
      for (Layer childLayer : orderedChildren)
      {
         traverseLayer(childLayer);
      } // next child layer
      post(layer);
   } // end of traverse()

   /**
    * Operation to perform before processing children. Default implementation does
    * nothing, subclasses should implement this to provide pre-order functionality. 
    * <p>This method may call change {@link #result} in order to build up a result for the traversal.
    * @param layer The layer to process.
    */
   protected void pre(Layer layer)
   {
   } // end of pre()

   /**
    * Operation to perform after processing children. Default implementation does nothing,
    * subclasses should implement this to provide post-order functionality. 
    * <p>This method may call change {@link #result} in order to build up a result for the traversal.
    * @param layer The layer to process.
    */
   protected void post(Layer layer)
   {
   } // end of post()

} // end of class LayerHierarchyTraversal
