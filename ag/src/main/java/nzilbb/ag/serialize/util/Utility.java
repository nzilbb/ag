//
// Copyright 2016-2020 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Spliterator;
import java.util.Vector;
import nzilbb.ag.Graph;
import nzilbb.ag.Layer;
import nzilbb.ag.serialize.*;

/**
 * Useful functions for serialization.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class Utility {
  /**
   * Creates an array of {@link Graph}s from one graph. Handy for calling
   * {@link nzilbb.ag.serialize.ISerializer#serialize(Spliterator,String[],Consumer,Consumer,Consumer)}
   * when you have
   * only one graph. 
   * @param graph The graph.
   * @return A graph with one element.
   */
  public static Spliterator<Graph> OneGraphSpliterator(Graph graph) {
    Graph[] graphs = { graph };
    return Arrays.spliterator(graphs);
  } // end of OneGraphArray()

  /**
   * Creates an array of {@link NamedStream}s from one file. Handy for calling
   * {@link IDeserializer#load(NamedStream[],Schema)} when you have only one graph.
   * @param file The file.
   * @return A graph with one element.
   * @throws FileNotFoundException If the given file does not exist.
   */
  public static NamedStream[] OneNamedStreamArray(File file) throws FileNotFoundException {
     return OneNamedStreamArray(new NamedStream(file));
  } // end of OneNamedStreamArray()
   
  /**
   * Creates an array of {@link NamedStream}s from one stream. Handy for calling
   * {@link IDeserializer#load(NamedStream[],Schema)} when you have only one graph.
   * @param stream The named stream.
   * @return A graph with one element.
   */
  public static NamedStream[] OneNamedStreamArray(NamedStream stream) {
    NamedStream[] streams = new NamedStream[1];
    streams[0] = stream;
    return streams;
  } // end of OneNamedStreamArray()
   
  /**
   * Finds a single stream of the given type in the given list of streams.
   * @param streams The streams to search.
   * @param nameSuffix The file extension to check the name for.
   * @param mimeType The MIME type to match.
   * @return The first stream whose {@link NamedStream#name} ends with
   * <var>nameSuffix</var> or whose {@link NamedStream#mimeType} is the same as
   * <var>mimeType</var>, or null if no such stream was found. 
   */
  public static NamedStream FindSingleStream(
     NamedStream[] streams, String nameSuffix, String mimeType) {
    for (NamedStream stream : streams) {
      if (stream.getName().toLowerCase().endsWith(nameSuffix) 
          || (mimeType != null && mimeType.equals(stream.getMimeType()))) {
        return stream;
      }
    } // next stream
    return null;
  } // end of findSingleStream()

  /**
   * Finds all streams of the given type in the given list of streams.
   * @param streams The streams to search.
   * @param nameSuffix The file extension to check the name for.
   * @param mimeType The MIME type to match.
   * @return A possibly empty array of streams whose {@link NamedStream#name} ends with
   * <var>nameSuffix</var> or whose {@link NamedStream#mimeType} is the same as
   * <var>mimeType</var>. 
   */
  public static NamedStream[] FindStreams(
     NamedStream[] streams, String nameSuffix, String mimeType) {
    Vector<NamedStream> matching = new Vector<NamedStream>();
    for (NamedStream stream : streams) {
      if (stream.getName().toLowerCase().endsWith(nameSuffix) 
          || (mimeType != null && mimeType.equals(stream.getMimeType()))) {
        matching.add(stream);
      }
    } // next stream
    return matching.toArray(new NamedStream[0]);
  } // end of findStreams()

  /**
   * Tries to find a layer in the given map, using an ordered list of possible IDs. Spaces, 
   * underscores, and case are ignored when looking for matching layer IDs.
   * @param possibleLayers Collection of layers from which a possibility can be selected.
   * @param possibleIds Guesses at possible layer IDs.
   * @return The first matching layer, or null if none matched.
   */
  public static Layer FindLayerById(
     LinkedHashMap<String,Layer> possibleLayers, List<String> possibleIds) {
    LinkedHashMap<String,Layer> possibleLayersSimplifiedIds = new LinkedHashMap<String,Layer>();
    for (String id : possibleLayers.keySet()) {
      possibleLayersSimplifiedIds.put(
         id.toLowerCase().replaceAll("[^a-zA-Z0-9]",""), possibleLayers.get(id));
    }
    for (String id : possibleIds) {
      if (possibleLayersSimplifiedIds.containsKey(
             id.toLowerCase().replaceAll("[^a-zA-Z0-9]",""))) {
        return possibleLayersSimplifiedIds.get(id.toLowerCase().replaceAll("[^a-zA-Z0-9]",""));
      }
    }
    return null;
  } // end of findLayerById()

} // end of class Utility
