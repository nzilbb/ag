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
    * Creates an array of {@link Graph}s from one graph. Handy for calling
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

   /**
    * Creates an array of {@link NamedStream}s from one stream. Handy for calling
    * {@link IDeserializer#load(NamedStream[],NamedStream[],Schema)} when you have only one graph.
    * @param stream
    * @return A graph with one element.
    */
   public static NamedStream[] OneNamedStreamArray(NamedStream stream)
   {
      NamedStream[] streams = new NamedStream[1];
      streams[0] = stream;
      return streams;
   } // end of OneNamedStreamArray()

   
   /**
    * Finds a single stream of the given type in the given list of streams.
    * @param filenameSuffix The file extension to check the name for.
    * @param mimeType The MIME type to match.
    * @return The first stream whose {@link NamedStream#name} ends with <var>nameSuffix</var> or whose {@link NamedStream#mimeType} is the same as <var>mimeType</var>, or null if no such stream was found.
    */
   public static NamedStream findSingleStream(NamedStream[] streams, String nameSuffix, String mimeType)
   {      
      for (NamedStream stream : streams)
      {	 
	 if (stream.getName().toLowerCase().endsWith(nameSuffix) 
	     || (mimeType != null && mimeType.equals(stream.getMimeType())))
	 {
	    return stream;
	 }
      } // next stream
      return null;
   } // end of findSingleStream()


} // end of class Utility
