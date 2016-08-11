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
package nzilbb.ag.serialize.util;

import nzilbb.ag.Graph;

/**
 * Useful functions for serialization.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class Utility
{
   
   /**
    * Creates an array of graphs from one graph. Handy for calling
    * {@link ISerializer#serialize(Graph[])} when you have only one graph.
    * @param graph
    * @return A graph with one element.
    */
   public static Graph[] OneGraphArray(Graph graph)
   {
      Graph[] graphs = new Graph[1];
      graphs[0] = graph;
      return graphs;
   } // end of OneGraphArray()

} // end of class Utility
