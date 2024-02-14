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

import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

/**
 * A node in a DependencyGraph.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class DependencyNode<C> {
  
  /**
   * The object that provides what's required for other nodes.
   * @see #getProvider()
   */
  protected C provider;
  /**
   * Getter for {@link #provider}: The object that provides what's required for other nodes.
   * @return The object that provides what's required for other nodes.
   */
  public C getProvider() { return provider; }
  
  /**
   * Set of graph edges; providers that this node depends on.
   * @see #getDependsOn()
   */
  protected Set<DependencyNode<C>> dependsOn = new LinkedHashSet<DependencyNode<C>>();
  /**
   * Getter for {@link #dependsOn}: Set of graph edges; providers that this node depends on.
   * @return Set of graph edges; providers that this node depends on.
   */
  public Set<DependencyNode<C>> getDependsOn() { return dependsOn; }
  
  /**
   * Constructor.
   * @param provider The object that provides what's required for other nodes.
   */
  public DependencyNode(C provider) {
    this.provider = provider;
  } // end of constructor
  
  /**
   * Adds an edge to the graph.
   * @param other The provider that this node depends on.
   * @return This object.
   */
  public DependencyNode dependsOn(DependencyNode<C> other) {
    dependsOn.add(other);
    return this;
  } // end of dependsOn()

  /**
   * String representation of the object.
   * @return String representation of the object.
   */
  @Override
  public String toString() {
    return ""+provider+ " (depends on "
      + dependsOn.stream().map(node -> ""+node.getProvider()).collect(Collectors.joining(","))
      + ")";
  } // end of toString()

} // end of class DependencyNode
