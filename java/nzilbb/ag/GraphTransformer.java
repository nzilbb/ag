//
// Copyright 2015-2020 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag;

import java.util.List;
import java.util.function.UnaryOperator;
/**
 * Interface for transformer that transforms a Graph in some way.  This might include
 * tokenizers, valitors, and taggers. 
 * <p> Note that it is valid (indeed, normal) for the Graph returned by
 * <code>apply(Graph)</code> to be the same object as the one passed in.
 * @author Robert Fromont robert@fromont.net.nz
 */
public interface GraphTransformer extends UnaryOperator<Graph> {
   
   /**
    * Transforms the graph.
    * @param graph The graph to transform.
    * @return The changes introduced by the tranformation.
    * @throws TransformationException If the transformation cannot be completed.
    */
   public List<Change> transform(Graph graph) throws TransformationException;

   default public Graph apply(Graph graph) {
      try {
         transform(graph);
         return graph;
      } catch(TransformationException exception) {
         throw new RuntimeException(exception);
      }
   }
} // end of interface GraphTransformer
