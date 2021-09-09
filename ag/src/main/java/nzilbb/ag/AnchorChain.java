//
// Copyright 2021 New Zealand Institute of Language, Brain and Behaviour, 
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
import java.util.Vector;
import java.util.function.Predicate;
import nzilbb.ag.Anchor;
import nzilbb.ag.Annotation;

/**
 * Chain of anchors joined by annotations.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class AnchorChain extends Vector<Anchor> {
  
  /**
   * Creates a chain of anchors from the given anchor forwards through the graph, until
   * the given condition is met, preferring paths through the given layer IDs (in order).
   * @param start Start from this anchor and traverse forward through the graph.
   * @param preferLayerIds Layer IDs of layers to traverse through preferentially. These
   * will be tried in order first, looking for a path. If not path is found, other layers
   * will be traversed.
   * @param follow Traverse annotations that meet this condition 
   * - i.e. ignore linking annotations where <code>follow.test(annotation)</code> returns false.
   * @param boundary Stop when an anchor meets this condition 
   * - i.e. when <code>boundary.test(anchor)</code> returns true. This anchor will be
   * returned as part of the chain. Can be null.
   * @return A chain of anchors from the given anchor (which is excluded) until the anchor that
   * met the given boundary condition (which is included).
   */
  public static AnchorChain ChainForwardUntil(
    Anchor start, List<String> preferLayerIds, Predicate<Annotation> follow, Predicate<Anchor> boundary) {
    if (boundary == null) boundary = a -> false; // default condition is never met
    if (follow == null) follow = a -> true; // default condition is to always follow
    AnchorChain chain = new AnchorChain();
    
    Anchor currentAnchor = start;
    Anchor nextAnchor = null;

    do { // loop until we meet the condition or run out of paths
      if (preferLayerIds != null) {
        // try preferred layers first
        for (String layerId : preferLayerIds) {
          if (currentAnchor.getStartOf().containsKey(layerId)) {
            for (Annotation startsHere : currentAnchor.getStartOf().get(layerId)) {
              if (startsHere.getChange() == Change.Operation.Destroy) continue;
              if (!follow.test(startsHere)) continue;
              nextAnchor = startsHere.getEnd();
              if (nextAnchor == currentAnchor) { // skip instants
                nextAnchor = null; 
              }
              if (nextAnchor != null) { // found a path, drop out
                break;
              }
            } // next annotation that starts here
          } // a known layer
          if (nextAnchor != null) {
            break;
          }
        } // next preferred layer
      }
      
      if (nextAnchor == null) { // no next anchor yet
        // try any annotations that start here
        for (String layerId : currentAnchor.getStartOf().keySet()) {
          for (Annotation startsHere : currentAnchor.getStartOf().get(layerId)) {
            if (startsHere.getChange() == Change.Operation.Destroy) continue;
            if (!follow.test(startsHere)) continue;
            nextAnchor = startsHere.getEnd();
            if (nextAnchor == currentAnchor) { // skip instants
              nextAnchor = null; 
            }
            if (nextAnchor != null) { // found a path, drop out
              break;
            }
          } // next annotation that starts here
        } // next starting layer      
      }
      if (nextAnchor != null) { // found a path to another anchor
        chain.add(nextAnchor);
        currentAnchor = nextAnchor;
        nextAnchor = null;      
      } else { // no path found
        // can't go any further
        break;
      }
    } while (!boundary.test(currentAnchor));
    
    return chain;
  } // end of ChainForwardUntil()

  /**
   * Creates a chain of anchors from the given anchor backwards through the graph, until
   * the given condition is met, preferring paths through the given layer IDs (in order).
   * @param start Start from this anchor and traverse backward through the graph.
   * @param preferLayerIds Layer IDs of layers to traverse through preferentially. These
   * will be tried in order first, looking for a path. If not path is found, other layers
   * will be traversed.
   * @param follow Follow annotations that meet this condition 
   * - i.e. ignore linking annotations where <code>follow.test(annotation)</code> returns false.
   * @param boundary Stop when an anchor meets this condition 
   * - i.e. when <code>boundary.test(anchor)</code> returns true. This anchor will be
   * returned as part of the chain.
   * @return A chain of anchors from the given anchor (which is excluded) until the anchor that
   * met the given boundary condition (which is included).
   */
  public static AnchorChain ChainBackwardUntil(
    Anchor start, List<String> preferLayerIds, Predicate<Annotation> follow, Predicate<Anchor> boundary) {
    if (boundary == null) boundary = a -> false; // default condition is never met
    if (follow == null) follow = a -> true; // default condition is to always follow
    AnchorChain chain = new AnchorChain();
    
    Anchor currentAnchor = start;
    Anchor nextAnchor = null;

    do { // loop until we meet the condition or run out of paths
      if (preferLayerIds != null) {
        // try preferred layers first
        for (String layerId : preferLayerIds) {
          if (currentAnchor.getEndOf().containsKey(layerId)) {
            for (Annotation endsHere : currentAnchor.getEndOf().get(layerId)) {
              if (endsHere.getChange() == Change.Operation.Destroy) continue;
              if (!follow.test(endsHere)) continue;
              nextAnchor = endsHere.getStart();
              if (nextAnchor == currentAnchor) { // skip instants
                nextAnchor = null; 
              }
              if (nextAnchor != null) { // found a path, drop out
                break;
              }
            } // next annotation that starts here
          } // a known layer
          if (nextAnchor != null) {
            break;
          }
        } // next preferred layer
      }
      
      if (nextAnchor == null) { // no next anchor yet
        // try any annotations that start here
        for (String layerId : currentAnchor.getEndOf().keySet()) {
          for (Annotation endsHere : currentAnchor.getEndOf().get(layerId)) {
            if (endsHere.getChange() == Change.Operation.Destroy) continue;
            if (!follow.test(endsHere)) continue;
            nextAnchor = endsHere.getStart();
            if (nextAnchor == currentAnchor) { // skip instants
              nextAnchor = null; 
            }
            if (nextAnchor != null) { // found a path, drop out
              break;
            }
          } // next annotation that starts here
        } // next starting layer      
      }
      if (nextAnchor != null) { // found a path to another anchor
        chain.insertElementAt(nextAnchor, 0);
        currentAnchor = nextAnchor;
        nextAnchor = null;      
      } else { // no path found
        // can't go any further
        break;
      }
    } while (!boundary.test(currentAnchor));
    
    return chain;
  } // end of ChainBackwardUntil()

  /**
   * Default constructor.
   */
  public AnchorChain() {
  } // end of constructor

  
  
} // end of class AnchorChain
