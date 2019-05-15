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
package nzilbb.ag;

/**
 * A graph media provider that uses an {@link IGraphStoreQuery} to access media.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class StoreGraphMediaProvider
  implements IGraphMediaProvider
{
  // Attributes:
  
  /**
   * The graph store for accessing media.
   * @see #getStore()
   * @see #setStore(IGraphStoreQuery)
   */
  protected IGraphStoreQuery store;
  /**
   * Getter for {@link #store}.
   * @return The graph store for accessing media.
   */
  public IGraphStoreQuery getStore() { return store; }
  /**
   * Setter for {@link #store}.
   * @param store The graph store for accessing media.
   * @return <var>this</var>.
   */
  public StoreGraphMediaProvider setStore(IGraphStoreQuery store) { this.store = store; return this; }
  
  /**
   * The graph.
   * @see #getGraph()
   * @see #setGraph(Graph)
   */
  protected Graph graph;
  /**
   * Getter for {@link #graph}.
   * @return The graph.
   */
  public Graph getGraph() { return graph; }
  /**
   * Setter for {@link #graph}. A side-effect of this method is that {@link Graph#mediaProvider}
   * is set. 
   * @param graph The graph.
   * @return <var>this</var>.
   */
  public StoreGraphMediaProvider setGraph(Graph graph)
  {
    this.graph = graph;
    this.graph.setMediaProvider(this);
    return this;
  }
  
  // Methods:
  
  /**
   * Default constructor.
   */
  public StoreGraphMediaProvider()
  {
  } // end of constructor

  /**
   * Attribute constructor. A side-effect of this constructor is that {@link Graph#mediaProvider}
   * is set. 
   * @param graph The graph.
   * @param store The graph store for accessing media.
   */
  public StoreGraphMediaProvider(Graph graph, IGraphStoreQuery store)
  {
    setGraph(graph);
    setStore(store);
  } // end of constructor

  // IGraphMediaProvider methods.

  /**
   * List the media available for the graph.
   * @return List of media files available for the given graph.
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public MediaFile[] getAvailableMedia() 
    throws StoreException, PermissionException
  {
    try
    {
      return store.getAvailableMedia(graph.getId());
    }
    catch(GraphNotFoundException exception)
    { // should be impossible, but...
      throw new StoreException(exception);
    }
  }
  
  /**
   * Gets a given media track for the graph.
   * @param trackSuffix The track suffix of the media - see {@link MediaTrackDefinition#suffix}.
   * @param mimeType The MIME type of the media.
   * @return A URL to the given media for the given graph, or null if the given media doesn't
   * exist. 
   * @throws StoreException If an error occurs.
   * @throws PermissionException If the operation is not permitted.
   */
  public String getMedia(String trackSuffix, String mimeType) 
    throws StoreException, PermissionException
  {
    try
    {
      if (graph.isFragment())
      {
        return store.getMedia(graph.sourceGraph().getId(), trackSuffix, mimeType,
                              graph.getStart().getOffset(), graph.getEnd().getOffset());
      }
      else
      {
        return store.getMedia(graph.getId(), trackSuffix, mimeType);
      }
    }
    catch(GraphNotFoundException exception)
    { // should be impossible, but...
      throw new StoreException(exception);
    }
  }

  /**
   * Provides another instance of the implementing class, for the given graph. The main use for
   * this method is to allow graphs to generate fragments that have their own media providers.
   * @param graph
   * @return A new object that can provide media for the given graph, or null if none can be
   * provided. 
   */
  public IGraphMediaProvider providerForGraph(Graph graph)
  {
    return new StoreGraphMediaProvider(graph, store);
  } // end of providerForGraph()
  
} // end of class StoreGraphMediaProvider
