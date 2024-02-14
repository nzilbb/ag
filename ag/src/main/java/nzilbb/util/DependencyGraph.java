//
// Copyright 2024 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.util;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Graph of dependencies.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class DependencyGraph<C> {
  
  /**
   * All the nodes in the graph.
   * @see #getNodes()
   */
  protected Set<DependencyNode<C>> nodes = new LinkedHashSet<DependencyNode<C>>();
  /**
   * Getter for {@link #nodes}: All the nodes in the graph.
   * @return All the nodes in the graph.
   */
  public Set<DependencyNode<C>> getNodes() { return nodes; }
  
  /**
   * Default constructor.
   */
  public DependencyGraph() {
  } // end of constructor
  
  /**
   * Adds the given node to the graph.
   * @param node
   * @return The node added.
   */
  public DependencyNode<C> add(DependencyNode<C> node) {
    if (node != null) nodes.add(node);
    return node;
  } // end of add()
  
  /**
   * Resolves dependencies, and returns an ordered list of nodes reflecting the order in
   * which nodes must be processed to ensure each node's depencies are met before
   * processing. 
   * @return An ordered list of nodes reflecting the order in which nodes must be
   * processed to ensure each node's depencies are met before processing. 
   * @throws CircularDependencyException If a circular dependency is detected.
   */
  public Collection<DependencyNode<C>> resolve() throws CircularDependencyException {
    LinkedHashSet<DependencyNode<C>> resolved = new LinkedHashSet<DependencyNode<C>>();
    LinkedHashSet<DependencyNode<C>> seen = new LinkedHashSet<DependencyNode<C>>();
    // loop through all nodes, in case the graph is not fully connected
    for (DependencyNode<C> node : nodes) {
      if (!seen.contains(node)) {
        resolve(node, resolved, seen);
      } // not already in the resolved set
    } // next node
    return resolved;
  } // end of resolve()

  /**
   * Internal recursive function for resolving dependencies. 
   * @param node The node to visit.
   * @param resolved The current state of the resolution.
   * @param seen List of nodes that have been visited.
   * @throws CircularDependencyException If a circular dependency is detected.
   */
  private void resolve(
    DependencyNode<C> node,
    LinkedHashSet<DependencyNode<C>> resolved,
    LinkedHashSet<DependencyNode<C>> seen) throws CircularDependencyException {
    seen.add(node);
    for (DependencyNode<C> edge: node.getDependsOn()) {
      if (!resolved.contains(edge)) {
        if (seen.contains(edge)) {
          throw new CircularDependencyException(node);
        }
        resolve(edge, resolved, seen);
      } // have not resolved the other node yet
    } // next edge
    resolved.add(node);
  } // end of resolve()

} // end of class DependencyGraph
