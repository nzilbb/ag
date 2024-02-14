//
// (c) 2015, Robert Fromont - robert@fromont.net.nz
//
//
//    This module is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 2 of the License, or
//    (at your option) any later version.
//
//    This module is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this module; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
package nzilbb.util;

/**
 * Exception thrown when DependencyGraph detects a circular dependency.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class CircularDependencyException extends Exception {
  
  /**
   * The node with the circular dependency.
   * @see #getNode()
   */
  protected DependencyNode node;
  /**
   * Getter for {@link #node}: The node with the circular dependency.
   * @return The node with the circular dependency.
   */
  public DependencyNode getNode() { return node; }
  
  /**
   * Default constructor.
   */
  public CircularDependencyException(DependencyNode node) {
    this.node = node;
  } // end of constructor
  
  /**
   * String representation of the object.
   * @return String representation of the object.
   */
  @Override
  public String toString() {
    return ""+node;
  } // end of toString()

} // end of class CircularDependencyException
