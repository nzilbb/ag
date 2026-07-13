"use strict";
/**
 * @file nzilbb.ag module processing Annotation Graphs.
 *
 * @example
 * ag.Graph.fromURL(jsonUrl, function(ag) {
 *   var words = ag.labels("word");
 *   for (var w in words) {
 *     console.log(words[w]);
 *   } // next word
 * });
 *
 * @author Robert Fromont robert.fromont@canterbury.ac.nz
 * @license magnet:?xt=urn:btih:1f739d935676111cfff4b4693e3816e664797050&dn=gpl-3.0.txt GPL v3.0
 * @copyright 2016 New Zealand Institute of Language, Brain and Behaviour, University of Canterbury
 *
 *    This file is part of nzilbb.ag
 *
 *    nzilbb.ag is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    nzilbb.ag is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with nzilbb.ag; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * @lic-end
 */

(function(exports){

  // namespace
  var nzilbb = nzilbb || {};
  nzilbb.ag = nzilbb.ag || {};
  nzilbb.ag._lastId = 0; // used to generate new IDs

  // AG class
  nzilbb.ag.Graph = function(id) {
    // only set attribute values if they're not already set
    if (!this.id) this.id = id;

    // top level layer
    if (!this.schema) this.schema = {
      transcript: {
        id: "transcript",
        description: "The graph as a whole",
        children: {},
        annotations : []
      }
    };
    if (!this.schema.layers) this.schema.layers = {};
    this.layers = this.schema.layers;

    // annotations collection
    if (!this.annotations) this.annotations = {};
    // anchors collection
    if (!this.anchors) this.anchors = {};
  }
  nzilbb.ag.Graph.prototype = {

    /**
     * The anchors sorted by offset. This includes only anchors for which the offset is
     * actually set.  
     */
    getSortedAnchors : function() { 
      var sortedAnchors = [];
      for (var a in this.anchors) {
	if (this.anchors[a].offset) {
	  sortedAnchors.push(this.anchors[a]);
	}
      }
      sortedAnchors.sort(function(a, b) { return a.offset-b.offset; });
      return sortedAnchors; 
    },

    labels : function(layerId) {
      var labels = [];
      var annotations = this.layers[layerId].annotations;
      for (var annotationid in annotations) {
	var annotation = annotations[annotationid]
	labels.push(annotation.label);
      }
      return labels;
    },

    annotationsAt : function(offset, layerId) {
      var annotations = [];
      var layers = layerId?{layerId:this.layers[layerId]}:this.layers;
      for (var l in layers) {
	var layer = layers[l];
	for (var a in layer.annotations) {
	  var annotation = layer.annotations[a];
	  if (annotation.includesOffset(offset)) annotations.push(annotation);
	  if (annotation.start && annotation.start.offset > offset) break; // assuming the list is sorted, we can stop now
	} // next annotation
      } // next layer
      return (annotations.length > 0)?annotations:null;
    },

    addAnnotation : function(annotation) {
      annotation.graph = this;
      if (!annotation.id) annotation.id = "+" + (++nzilbb.ag._lastId);
      this.annotations[annotation.id] = annotation;
      if (!this[annotation.layerId]) this[annotation.layerId] = [];
      this[annotation.layerId].push(annotation);
      if (!annotation.parent && annotation.parentId) {
        annotation.parent = this.annotations[annotation.parentId];
      }
      if (this.anchors[annotation.startId]) {
        if (!this.anchors[annotation.startId].startOf[annotation.layerId]) {
          this.anchors[annotation.startId].startOf[annotation.layerId] = []
        }
        this.anchors[annotation.startId].startOf[annotation.layerId].push(annotation);
      }
      if (this.anchors[annotation.endId]) {
        if (!this.anchors[annotation.endId].endOf[annotation.layerId]) {
          this.anchors[annotation.endId].endOf[annotation.layerId] = []
        }
        this.anchors[annotation.endId].endOf[annotation.layerId].push(annotation);
      }
      if (annotation.parent) {
	if (!annotation.parent[annotation.layerId]) annotation.parent[annotation.layerId] = [];
        var existing = annotation.parent[annotation.layerId].findIndex(
          a => a.id == annotation.id);
        if (existing >= 0) { // replace existing
          annotation.parent[annotation.layerId][existing] = annotation;
        } else {
	  annotation.parent[annotation.layerId].push(annotation);
        }
      }
      if (!annotation.layer.annotations) annotation.layer.annotations = []
      var existing = annotation.layer.annotations.findIndex(
        a => a.id == annotation.id);
      if (existing >= 0) { // replace existing
        annotation.layer.annotations[existing] = annotation;
      } else {
	annotation.layer.annotations.push(annotation);
      }
    },    
    removeAnnotation : function(id) {
      var annotation = this.annotations[id];
      this[annotation.layerId] = this[annotation.layerId].filter(a=>a.id != id);
      if (annotation.parent) {
	annotation.parent[annotation.layerId]
          = annotation.parent[annotation.layerId].filter(a=>a.id != id);
      }
      annotation.layer.annotations = annotation.layer.annotations.filter(a=>a.id != id);
    },    
    addAnchor : function(anchor) {
      anchor.graph = this;
      if (!anchor.id) anchor.id = "+" + (++nzilbb.ag._lastId);
      this.anchors[anchor.id] = this.anchors[anchor.id] || anchor;
    },
    
    first : function(layerId) {
      if (this.layers[layerId] && this.layers[layerId].annotations.length > 0) {
        return this.layers[layerId].annotations[0];
      }
      return null;
    },
    last : function(layerId) {
      if (this.layers[layerId] && this.layers[layerId].annotations.length > 0) {
        return this.layers[layerId].annotations[this.layers[layerId].annotations.length-1];
      }
      return null;
    },
    all : function(layerId) {
      if (layerId == "transcript") {
        return [ this ];
      }
      if (this.layers[layerId]) {
        return this.layers[layerId].annotations;
      }
      return [];
    }
  } // Graph methods
  nzilbb.ag.Graph.indexLayers = function(top, parent, layers) {
    for (var layerId in layers) {
      var layer = layers[layerId];
      layer.id = layerId;
      if (parent) {
        layer.parentId = parent.id;
        layer.parent = parent;
      } else {
        layer.parentId = "transcript";
      }
      layer.annotations = []; // allow enumeration by layer
      top[layerId] = layer;
      if (top[layerId].children)
      {
	nzilbb.ag.Graph.indexLayers(top, layer, layer.children);
      }
    } // next layer
  }
  nzilbb.ag.Graph.activateAnchors = function(anchors) {
    for (var a in anchors) {
      // set the id
      var anchor = new nzilbb.ag.Anchor(anchors[a].offset, this);
      Object.assign(anchor, anchors[a]);
      anchors[a] = anchor;
      anchors[a].id = a;
    }    
  }
  nzilbb.ag.Graph.activateLayer = function(ag, parent, layerId, annotations) {

    // annotations are JSON objects, we will create an array of child Annotation objects: 
    var children = [];
    for (var i in annotations) {
      var annotation = annotations[i];

      // data
      annotation.graphId = ag.id;
      annotation.graph = ag;
      annotation.layerId = layerId;
      if (parent) {
	annotation.parentId = parent.id;
	annotation.parent = parent;
      }

      // make it into an Annotation object, with all the corresponding methods        
      var a = new nzilbb.ag.Annotation(
        annotation.layerId, annotation.label, ag, annotation.startId, annotation.endId);
      Object.assign(a, annotation);
      annotation = a;
      children.push(annotation);

      // indexing for easy lookup and iteration
      ag.annotations[annotation.id] = annotation; // index annotations by id
      annotation.layer.annotations.push(annotation); // allow enumeration by layer
      var startAnchor = annotation.start;
      if (startAnchor) {
	if (!startAnchor.startOf[layerId]) startAnchor.startOf[layerId] = [];
	startAnchor.startOf[layerId].push(annotation);
      }
      var endAnchor = annotation.end;
      if (endAnchor) {
	if (!endAnchor.endOf[layerId]) endAnchor.endOf[layerId] = [];
	endAnchor.endOf[layerId].push(annotation);
      }

      // detect layers
      for (var key in annotation) {
	// is it an array?
	if (annotation[key] instanceof Array) {// TODO we actually know what layers to look for, so should use that instead
	  nzilbb.ag.Graph.activateLayer(ag, annotation, key, annotation[key]);
	}
      } // next key

    } // next annotation

    // sort layer annotations by offset
    ag.schema.layers[layerId].annotations.sort((a,b) => a.start.offset - b.start.offset);

    // ensure children are the Annotation objects
    if (parent) parent[layerId] = children;
  }

  /**
   * Callback invoked when a graph has been loaded.
   * @callback graphCallback
   * @param graph The loaded Graph
   */
  /**
   * Callback invoked when a graph could not be loaded.
   * @callback errorCallback
   * @param xhr The XMLHttpRequest object for the request that failed.
   */

  /**
   * Loads an annotation Graph from a given URL, representing a JSON serialization of a graph.
   * @param url URL to a JSON serialization of an annotation graph.
   * @callback {graphCallback} success Invoked when the request has returned a result.
   * @callback {errorCallback} error Invoked if the request fails.
   */
  nzilbb.ag.Graph.fromURL = function(url, success, error) {
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function() {
      if (xhr.readyState === XMLHttpRequest.DONE) {
        if (xhr.status === 200) {
          if (success) {
	    var jsonObject = JSON.parse(xhr.responseText);
            var graph = nzilbb.ag.Graph.activateObject(jsonObject);
	    success(graph);
	  } else {
	    if (error) error(xhr);
	  }		
        } else {
          if (error) error(xhr);
        }
      }
    };
    xhr.open("GET", url, true);
    xhr.send();
  }

  /**
   * Converts a JSON serialization of an Annotation Graph into a Graph object.
   * @param graph The JSON-deserialized object representing the Annotation Rgaph.
   */
  nzilbb.ag.Graph.activateObject = function(graph) {
    // make it into an AG
    var g = new nzilbb.ag.Graph(graph.id);
    Object.assign(g, graph);
    graph = g;

    graph.schema.layers = {};
    nzilbb.ag.Graph.indexLayers(
      graph.schema.layers, graph.schema.transcript, { transcript: graph.schema.transcript } );
    graph.layers = graph.schema.layers;
    nzilbb.ag.Graph.activateAnchors(graph.anchors);

    graph.annotations = {};
    for (var key in graph) {
      if (graph[key] instanceof Array // TODO we actually know what layers to look for, so should use that instead
	  && key != "layers" && key != "anchors" && key != "participants")
      {
	nzilbb.ag.Graph.activateLayer(graph, graph, key, graph[key]);
      }
    }    
    return graph;
  }

  /**
   * Converts an Annotation Graph (back) into a plain JSON-serializable object, removing any
   * possible cyclic references, and methods. 
   * @param graph The Graph to deactivate.
   */
  nzilbb.ag.Graph.deactivateObject = function(graph) {
    // de-cyclicify annotations
    for (var annotationId in graph.annotations) {
      var annotation = graph.annotations[annotationId];
      delete annotation.graph;
      delete annotation.graphId;
      delete annotation.parentId;
      delete annotation.parent;
      delete annotation.layerId;
      /*
	annotation.layer = null;
	annotation.ordinal = null;
	annotation.previous = null;
	annotation.next = null;
	annotation.start = null;
	annotation.end = null;
      */
    } // next annotation
    delete graph.annotations;

    // de-cyclicify anchors
    for (var anchorId in graph.anchors) {
      var anchor = graph.anchors[anchorId];
      delete anchor.id;
      delete anchor.graph;
      delete anchor.graphId;
      delete anchor.startOf;
      delete anchor.endOf;
    } // next annotation

    // de-cyclicify layers
    for (var layerId in graph.layers) {
      var layer = graph.layers[layerId];
      if (layer.parent) { // not a top-level layer
	delete layer.id;
	delete layer.parent;
	delete graph.layers[layerId];
      }
      delete layer.annotations;
    } // next annotation
    var layerChildren = graph.layers.children;
    graph.layers = {};
    graph.layers.children = layerChildren;
    return graph;
  }

  // Annotation class
  nzilbb.ag.Annotation = function(layerId, label, graph, startId, endId, id, parentId) {
    // only set attribute values if they're not already set
    if (!this.id) this.id = id;
    if (!this.label) this.label = label;
    if (!this.startId) this.startId = startId;
    if (!this.endId) this.endId = endId;
    if (!this.graph) this.graph = graph;
    if (!this.parentId) this.parentId = parentId;
    if (!this.parent) this.parent = null;
    if (!this.layerId) this.layerId = layerId;
    return this;
  }
  nzilbb.ag.Annotation.prototype = {

    // attributes
    get layer() { return this.graph.schema.layers[this.layerId]; },
    get ordinal() { return this.parent && this.parent[this.layerId].indexOf(this) + 1; },
    get previous() { return this.parent && this.parent[this.layerId][this.ordinal - 2]; },
    get next() { return this.parent && this.parent[this.layerId][this.ordinal]; },
    get start() { return this.graph.anchors[this.startId]; },
    get end() { return this.graph.anchors[this.endId]; },

    // query methods
    includesOffset : function(offset) {
      try {
        return this.start.offset <= offset && this.end.offset > offset;
      } catch(x) { return false; }
    },
    includes : function(annotation) {
      try {
        return this.includesOffset(annotation.start.offset)
          && (this.includesOffset(annotation.end.offset)
              || this.end.offset == annotation.end.offset);
      } catch(x) { return false; }
    },
    includesMidpoint : function(annotation) {
      return this.includesOffset(annotation.midpoint());
    },
    overlaps : function(annotation) {
      try {
        return this.start.offset < annotation.end.offset
          && this.end.offset > annotation.start.offset;
      } catch(x) { return false; }
    },
    duration : function() {
      try {
        return this.end.offset - this.start.offset;
      } catch(x) { return null; }
    },
    midpoint : function() {
      try {
        return this.start.offset + (this.duration() / 2);
      } catch(x) {return null; }
    },
    instantaneous : function() { return this.startId == this.endId; },
    anchored : function() {
      return this.start && this.start.offset >= 0 && this.end && this.end.offset >= 0;
    },
    toString : function Annotation_toString() { return this.label; },

    sharesStart : function(layerId) {
      try {
        return this.start.startOf[layerId];
      } catch(x) { return {}; }
    },
    sharesEnd : function(layerId) {
      try {
        return this.end.endOf[layerId];
      } catch(x) { return {}; }
    },
    startsWith : function(annotation) { return this.startId == annotation.startId; },
    endsWith : function(annotation) { return this.endId == annotation.endId; },
    tags : function(annotation) {
      return this.startsWith(annotation) && this.endsWith(annotation);
    },
    predecessorOf : function(annotation) { return this.endId == annotation.startId ; },
    successorOf : function(annotation) { return annotation.endId == this.startId ; },

    tagOn : function(layerId) {
      var tags = [];
      for (var i in this.start.startOf[layerId]) {
	var other = this.start.startOf[layerId][i];
	if (this.endsWith(other)) tags.push(other);
      } // next annotation that starts here
      return tags;
    },

    first : function(layerId) {
      var layer = this.graph.layers[layerId];
      // is it me?
      if (this.layerId == layerId) {
        return this;
      }
      // is it a child layer?
      if (this[layerId]) {
        if (this[layerId].length > 0) {
          return this[layerId][0];
        }
        return null;
      } else  if (layer.parentId == this.layerId) { // it's *meant* to be a child
        return null;
      }
      // go up through ancestors
      var ancestor = this.parent;
      while (ancestor != null && ancestor.layerId != layerId) {
        ancestor = ancestor.parent;
      } // next
      if (ancestor != null && ancestor.layerId == layerId) {
        return ancestor;
      }
      // are their annotations on the other layer that contain or are contained by this annotation?
      var annotations = []
      for (var a in this.graph.layers[layerId].annotations) {
        var annotation = this.graph.layers[layerId].annotations[a];
        if (annotation.includes(this) || this.includes(annotation)) {
          // TODO weed out the ones that aren't connected by a shared parent
          return annotation;
        }
      } // next annotation
      return null;
    },
    last : function(layerId) {
      // is it me?
      if (this.layerId == layerId) {
        return this;
      }
      // is it a child layer?
      if (this[layerId]) {
        if (this[layerId].length > 0) {
          return this[layerId][this[layerId].length - 1];
        }
      } else  if (layer.parentId == this.layerId) { // it's *meant* to be a child
        return null;
      }
      // go up through ancestors
      var ancestor = this.parent;
      while (ancestor != null && ancestor.layerId != layerId) {
        ancestor = ancestor.parent;
      } // next
      if (ancestor != null && ancestor.layerId == layerId) {
        return ancestor;
      }
      // traverse schema
      var all = this.all(layerId);
      if (all.length > 0) return all[all.length - 1];
      
      return null;
    },
    
    all : function(layerId) {
      var layer = this.graph.layers[layerId];
      if (!layer) return [];
      
      // is it me?
      if (this.layerId == layerId) {
        return [this];
      }
      // is it a child layer?
      if (this[layerId]) { // there's a 'child' layer (which may or may not be official)
        return this[layerId];
      } else  if (layer.parentId == this.layerId) { // it's *meant* to be a child
        return [];
      }
      // go up through ancestors
      var ancestor = this.parent;
      while (ancestor != null && ancestor.layerId != layerId) {
        ancestor = ancestor.parent;
      } // next
      if (ancestor != null && ancestor.layerId == layerId) {
        return [ancestor];
      }
      // are their annotations on the other layer that contain or are contained by this annotation?
      var firstCommonAncestorLayerId = firstCommonAncestorLayer(this.graph.schema, this.layerId, layerId);
      var annotations = [];
      if (firstCommonAncestorLayerId && firstCommonAncestorLayerId != "transcript") {
        var ancestor = this.first(firstCommonAncestorLayerId);
        if (ancestor) { // there is a possibly common ancestor annotation
          for (var annotation of ancestor.all(layerId)) {
            if (annotation.includes(this) || this.includes(annotation)) {
              annotations.push(annotation);
            }
          } // next annotation
        }
      } else { // no common ancestor layer
        // so scan the whole graph and use 'includes'
        for (var a in this.graph.layers[layerId].annotations) {
          var annotation = this.graph.layers[layerId].annotations[a];
          if (annotation.includes(this) || this.includes(annotation)) {
            annotations.push(annotation);
          }
        } // next annotation
      }
      return annotations
      // ordered by offset
        .toSorted((a,b)=> a.start.offset - b.start.offset);
    },

    labels : function(layerId) {
      return this.all(layerId).map(a => a.label);
    },

    // annotation methods

    createTag : function(layerId, label) {
      var tag = new nzilbb.ag.Annotation(layerId, label, this.graph, this.startId, this.endId);
      if (tag.layer.parent == this.layer) {
        // tag is child of this
	tag.parentId = this.id;
	tag.parent = this;
      } else if (tag.layer.parent == this.layer.parent) {
        // this layer and tag layer share a parent
	tag.parentId = this.parent.id;
	tag.parent = this.parent;
      }
      this.graph.addAnnotation(tag);
      return tag;
    }
    
  } // Annotation methods

  function layerAndAncestors(schema, layerId) {
    var ancestors = [ schema.layers[layerId] ];
    while (ancestors[ancestors.length-1].parent
           && !ancestors.includes(ancestors[ancestors.length-1].parent)) {
      ancestors.push(ancestors[ancestors.length-1].parent);
    }
    return ancestors.map(l=>l.id);
  }

  function firstCommonAncestorLayer(schema, layer1, layer2) {
    if (layer1 == layer2) return layer1;
    var ancestors1 = layerAndAncestors(schema, layer1).filter(l=>l!=layer1);
    var ancestors2 = layerAndAncestors(schema, layer2).filter(l=>l!=layer2);
    
    var intersection = ancestors1.filter(l => ancestors2.includes(l));
    if (intersection.length) return intersection[0];
    return null;
  }

  // Anchor class
  nzilbb.ag.Anchor = function(offset, graph) {
    // only set attribute values if they're not already set
    if (!this.id) this.id = null;
    if (!this.offset && offset) this.offset = offset;
    if (!this.graph) this.graph = graph;    
    this.startOf = {};
    this.endOf = {};
  }
  nzilbb.ag.Anchor.prototype = {

    toString : function Anchor_toString() { return this.offset; }

  } // Anchor methods

  exports.Graph = nzilbb.ag.Graph;
  exports.Annotation = nzilbb.ag.Annotation;
  exports.Anchor = nzilbb.ag.Anchor;

}(typeof exports === 'undefined' ? this.ag = {} : exports));
