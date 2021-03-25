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

import nzilbb.ag.*;
import java.util.HashSet;

/**
 * Traverses {@link Annotation}s in a {@link Graph} using the Layer hierarchy (i.e. Annotations are nodes, parent/child linkes are edges).
 * <p>This base class handles the traversal. The pre-order and post-order operations (the actual work being done) can be implemented by subclassing to implement {@link #pre(Annotation)} and {@link #post(Annotation)}.
 * <p>For example, to print a list of annotations in hierarchy order:
 * <pre>
 * LayerTraversal&lt;StringBuffer&gt; t = new LayerTraversal&lt;StringBuffer&gt;(new StringBuffer(), graph)
 * {
 *   protected void pre(Annotation annotation)
 *   {
 *     result.append(annotation.getLabel() + "\n");
 *   }
 * };
 * System.out.println(t.getResult().toString());
 * </pre>
 * <p>The main traversal uses only the layer hierarchy defined in the graph. This means that if the graph contains annotations for which no layer is defined, those annotations will not be visited by this traversal.  After traversing the layer hierarchy, the annotations that have not been visited are processed by {@link #except(Annotation)} - provide an implementation for this method if these annotations must also be processed.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class LayerTraversal<R>
{
   // Attributes:
   
   /**
    * The graph to be traversed.
    * @see #getGraph()
    * @see #setGraph(Graph)
    */
   protected Graph graph;
   /**
    * Getter for {@link #graph}: The graph to be traversed.
    * @return The graph to be traversed.
    */
   public Graph getGraph() { return graph; }
   /**
    * Setter for {@link #graph}: The graph to be traversed.
    * @param newGraph The graph to be traversed.
    */
   public LayerTraversal<R> setGraph(Graph newGraph) { graph = newGraph; return this; }

   
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
   public LayerTraversal<R> setResult(R newResult) { result = newResult; return this; }
   
   /**
    * Whether the traversal is breadth-first (true) or depth-first (false - the default).
    * @see #getBreadthFirst()
    * @see #setBreadthFirst(boolean)
    */
   protected boolean breadthFirst = false;
   /**
    * Getter for {@link #breadthFirst}: Whether the traversal is breadth-first (true) or depth-first (false - the default).
    * @return Whether the traversal is breadth-first (true) or depth-first (false - the default).
    */
   public boolean getBreadthFirst() { return breadthFirst; }
   /**
    * Setter for {@link #breadthFirst}: Whether the traversal is breadth-first (true) or depth-first (false - the default).
    * @param newBreadthFirst Whether the traversal is breadth-first (true) or depth-first (false - the default).
    */
   public LayerTraversal<R> setBreadthFirst(boolean newBreadthFirst) { breadthFirst = newBreadthFirst; return this; }

   protected HashSet<Annotation> visited = new HashSet<Annotation>();

   // Methods:
   
   /**
    * Default constructor.
    */
   public LayerTraversal()
   {
   } // end of constructor

   /**
    * Constructor with starting result.
    * @param result The result of the traversal.
    */
   public LayerTraversal(R result)
   {
      setResult(result);
   } // end of constructor
 
   /**
    * Constructor with starting result and graph to immediately traverse. Traversal will be depth-first.
    * @param result The result of the traversal.
    * @param graph The graph to be traversed.
    */
   public LayerTraversal(R result, Graph graph)
   {
      setResult(result);
      setResult(traverseGraph(graph));
   } // end of constructor

   /**
    * Constructor with starting result and annotation to immediately traverse. Traversal will be depth-first.
    * @param result The result of the traversal.
    * @param annotation The annotation to be traversed.
    */
   public LayerTraversal(R result, Annotation annotation)
   {
      setResult(result);
      if (annotation instanceof Graph)
      {
	 setResult(traverseGraph((Graph)annotation));
      }
      else
      {
	 setResult(traverseAnnotation(annotation));
      }
   } // end of constructor
  
   /**
    * Constructor with starting result and graph to immediately traverse.
    * @param result The result of the traversal.
    * @param graph The graph to be traversed.
    * @param breadthFirst Whether the traversal is breadth-first (true) or depth-first (false).
    */
   public LayerTraversal(R result, Graph graph, boolean breadthFirst)
   {
      setResult(result);
      setBreadthFirst(breadthFirst);
      setResult(traverseGraph(graph));
   } // end of constructor
  
   /**
    * Traverses the given graph.
    * @param graph The graph to traverse.
    * @return Some result of the traversal, if required.
    */
   public R traverseGraph(Graph graph)
   {
      visited.clear();
      setGraph(graph);
      
      // for each top-level layer
      for (Layer layer : graph.getLayer("transcript").getChildren().values())
      {
	 if (breadthFirst)
	 { // process all annotations on one layer before moving to chlid layers
	    traverseLayer(layer);
	 }
	 else
	 { // depth-first - go down through annotations	    
	    for (Annotation annotation : graph.getAnnotations(layer.getId()))
	    {
	       traverseAnnotation(annotation);	    
	    } // next annotation
	 }
      } // next top level layer

      // now process annotations missed by the traversal, 
      // and untag the annotations we did visit
      for (Annotation annotation : graph.getAnnotationsById().values())
      {
	 if (!visited.contains(annotation))
	 { // outside the hierarchy
	    except(annotation);
	 }
      }
      visited.clear();
      return getResult();
   } // end of traverseGraph()

   
   /**
    * Depth-first recursive method that traverses the layer annotations using the layer hierarchy, calling {@link #pre(Annotation)}, then calling itself for all children, then called {@link #post(Annotation)}, before returning.
    * @param annotation The annotation to traverse through.
    * @see #breadthFirst
    * @return The results - i.e. {@link #getResult()}
    */
   public R traverseAnnotation(Annotation annotation)
   {
      // only visit a node once (even if the parent/child hierarchy is corrupt)
      if (!visited.contains(annotation))
      {
	 visited.add(annotation);
	 pre(annotation);
	 for (Layer childLayer : annotation.getLayer().getChildren().values())
	 {
	    // if the annotation has children on that layer
	    if (annotation.getAnnotations().containsKey(childLayer.getId()))
	    {
	       // traverse them
	       for (Annotation child : annotation.getAnnotations(childLayer.getId()))
	       {
		  traverseAnnotation(child);
	       } // next child
	    } // there are children on this layer
	 } // next child layer
	 post(annotation);
      }
      return getResult();
   } // end of traverse()

   /**
    * Breadth-first recursive method that traverses the layer annotations using the layer hierarchy, calling {@link #pre(Annotation)}, then calling itself for all children, then called {@link #post(Annotation)}, before returning.
    * @param layer The layer to traverse.
    * @see #breadthFirst
    */
   protected void traverseLayer(Layer layer)
   {
      for (Annotation annotation : graph.all(layer.getId()))
      {
	 // only visit a node once (even if the parent/child hierarchy is corrupt)
	 if (!visited.contains(annotation))
	 {
	    // (don't tag it yet, we have to know whether to call post() below)
	    pre(annotation);
	 }
      }
      for (Layer childLayer : layer.getChildren().values())
      {
	 traverseLayer(childLayer);
      } // next child layer
      for (Annotation annotation : graph.all(layer.getId()))
      {
	 // only visit a node once (even if the parent/child hierarchy is corrupt)
	 if (!visited.contains(annotation))
	 {
	    visited.add(annotation);
	    post(annotation);
	 }
      }
   } // end of traverse()

   
   /**
    * Operation to perform before processing children. Default implementation does nothing, subclasses should implement this to provide pre-order functionality.
    * <p>This method may call change {@link #result} in order to build up a result for the traversal.
    * @param annotation The annotation that is about to be traversed.
    */
   protected void pre(Annotation annotation)
   {
   } // end of pre()

   /**
    * Operation to perform after processing children. Default implementation does nothing, subclasses should implement this to provide post-order functionality.
    * <p>This method may call change {@link #result} in order to build up a result for the traversal.
    * @param annotation The annotation that has just been traversed.
    */
   protected void post(Annotation annotation)
   {
   } // end of post()

   /**
    * Operation to perform for annotations that were not visited by the main traversal. These will be annotations that are in the graph, but their layer is not in the graph's layer definition hierarchy, perhaps because a {@link Layer} was not added to the graph.
    * <p>This method may call change {@link #result} in order to build up a result for the traversal.
    * @param annotation An annotation that was not visited during the main traversal.
    */
   protected void except(Annotation annotation)
   {
   } // end of except()
   

} // end of class LayerTraversal
