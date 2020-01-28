/**
 * @file nzilbb.labbcat module for communicating with a LaBB-CAT web application.
 *
 * @example
 * var lc = new labbcat.Labbcat(baseUrl);
 * // load corpora
 * lc.getCorpusIds(function(result, errors, messages, call, id) {
 *     if (errors) {
 *        alert("Could not list corpora: " + errors[0]);
 *     } else {
 *       var corpora = document.getElementById("corpus");
 *       for (var i in result) {
 *         var option = document.createElement("option");
 *         option.appendChild(document.createTextNode(result[i]));
 *         if (result[i] == "${sessionScope['corpus']}") {
 *           option.selected = "selected";
 *         }
 *         corpora.appendChild(option);
 *       }
 *       
 *     }
 *   });
 *
 * @author Robert Fromont robert.fromont@canterbury.ac.nz
 * @license magnet:?xt=urn:btih:1f739d935676111cfff4b4693e3816e664797050&dn=gpl-3.0.txt GPL v3.0
 * @copyright 2016 New Zealand Institute of Language, Brain and Behaviour, University of Canterbury
 *
 *    This file is part of LaBB-CAT.
 *
 *    LaBB-CAT is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    LaBB-CAT is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with LaBB-CAT; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * @lic-end
 */

(function(exports){

// namespace
var nzilbb = nzilbb || {};
nzilbb.labbcat = nzilbb.labbcat || {};

var runningOnNode = false;

if (typeof(require) == "function") { // running on node.js
    XMLHttpRequest = require('xhr2');
    FormData = require('form-data');
    fs = require('fs');
    btoa = require('btoa');
    parseUrl = require('url').parse;
    runningOnNode = true;
}

/**
 * Callback invoked when the result of a request is available.
 *
 * @callback resultCallback
 * @param result The result of the method
 * @param {string[]} errors The error, if any
 * @param {string[]} messages The error, if any
 * @param call The method that was called
 * @param id The ID that was passed to the method, if any.
 * @param {object} taskIds A list of IDs of the resulting server tasks, if any.
 */

function callComplete(evt) {
    try {
	var response = JSON.parse(this.responseText);
	var result = null;
        if (response.model != null) {
            if (response.model.result) result = response.model.result;
	    if (!result && result != 0) result = response.model;
        }
	var errors = response.errors;
	if (!errors || errors.length == 0) errors = null;
	var messages = response.messages;
	if (!messages || messages.length == 0) messages = null;
	evt.target.onResult(result, errors, messages, evt.target.call, evt.target.id);
    } catch(exception) {
	evt.target.onResult(null, ["" +exception+ ": " + this.responseText], [], evt.target.call, evt.target.id);
    }
}
function callFailed(evt) {
    evt.target.onResult(null, ["failed: " + this.responseText], [], evt.target.call, evt.target.id);
}
function callCancelled(evt) {
    evt.target.onResult(null, ["cancelled"], [], evt.target.call, evt.target.id);
}

// GraphStoreQuery class - read-only "view" access

nzilbb.labbcat.GraphStoreQuery = function(baseUrl) {
    if (!/\/$/.test(baseUrl)) baseUrl += "/";
    this.url = baseUrl + "api/store/";
}

nzilbb.labbcat.GraphStoreQuery.prototype = {

    // methods

    /**
     * Creates an http request
     * @param {string} call The name of the API function to call
     * @param parameters The arguments of the function, if any
     * @callback {resultCallback} onResult Invoked when the request has returned a result.
     * @return {XMLHttpRequest} An open request.
     */
    createRequest : function(call, parameters, onResult, url) {
	var xhr = new XMLHttpRequest();
	xhr.call = call;
	if (parameters && parameters.id) xhr.id = parameters.id;
	xhr.onResult = onResult;
	xhr.addEventListener("load", callComplete, false);
	xhr.addEventListener("error", callFailed, false);
	xhr.addEventListener("abort", callCancelled, false);
	var queryString = "";
	if (parameters) {
	    for (var key in parameters) {
		if (parameters[key]) {
  		    if (parameters[key].constructor === Array) {
			for (var i in parameters[key]) {
			    queryString += "&"+key+"="+encodeURIComponent(parameters[key][i])
			}
		    } else {
			queryString += "&"+key+"="+encodeURIComponent(parameters[key])
		    }
		}
	    } // next parameter
	}
        queryString = queryString.replace(/^&/,"?");
	if (!url) url = this.url;
	xhr.open("GET", url + call + queryString, true, this.username, this.password);
	if (this.username) {
	    xhr.setRequestHeader("Authorization", "Basic " + btoa(this.username + ":" + this.password))
	}
	xhr.setRequestHeader("Accept", "application/json");
	return xhr;
    },
    
    /**
     * Gets the store's ID.
     * @callback {resultCallback} onResult Invoked when the request has returned a result.
     * @return {string} The annotation store's ID.
     */
    getId : function(onResult) {
	this.createRequest("getId", null, onResult).send();
    },
    
    /**
     * Gets a list of layer IDs (annotation 'types').
     * @callback {resultCallback} onResult Invoked when the request has returned a result.
     * @return {string[]} A list of layer IDs.
     */
    getLayerIds : function(onResult) {
	this.createRequest("getLayerIds", null, onResult).send();
    },
    
    /**
     * Gets a list of layer definitions.
     * @callback {resultCallback} onResult Invoked when the request has returned a result.
     * @return A list of layer definitions.
     */
    getLayers : function(onResult) {
	this.createRequest("getLayers", null, onResult).send();
    },
    
    /**
     * Gets the layer schema.
     * @callback {resultCallback} onResult Invoked when the request has returned a result.
     * @return A schema defining the layers and how they relate to each other.
     */
    getSchema : function(onResult) {
	this.createRequest("getSchema", null, onResult).send();
    },
    
    /**
     * Gets a layer definition.
     * @callback {resultCallback} onResult Invoked when the request has returned a result.
     * @param {string} id ID of the layer to get the definition for.
     * @return The definition of the given layer.
     */
    getLayer : function(id, onResult) {
	var xhr = this.createRequest("getLayer", { id : id }, onResult).send();
    },
    
    /**
     * Gets a list of corpus IDs.
     * @callback {resultCallback} onResult Invoked when the request has returned a result.
     * @return {string[]} A list of corpus IDs.
     */
    getCorpusIds : function(onResult) {
	this.createRequest("getCorpusIds", null, onResult).send();
    },
    
    /**
     * Gets a list of participant IDs.
     * @callback {resultCallback} onResult Invoked when the request has returned a result.
     * @return {string[]} A list of participant IDs.
     */
    getParticipantIds : function(onResult) {
	this.createRequest("getParticipantIds", null, onResult).send();
    },
    
    /**
     * Gets a list of graph IDs.
     * @callback {resultCallback} onResult Invoked when the request has returned a result.
     * @return {string[]} A list of graph IDs.
     */
    getGraphIds : function(onResult) {
	this.createRequest("getGraphIds", null, onResult).send();
    },
    
    /**
     * Gets a list of graph IDs in the given corpus.
     * @param {string} id A corpus ID.
     * @callback {resultCallback} onResult Invoked when the request has returned a result.
     * @return {string[]} A list of graph IDs.
     */
    getGraphIdsInCorpus : function(id, onResult) {
	this.createRequest("getGraphIdsInCorpus", { id : id }, onResult).send();
    },
    
    /**
     * Gets a list of IDs of graphs that include the given participant.
     * @param {string} id A participant ID.
     * @callback {resultCallback} onResult Invoked when the request has returned a result.
     * @return {string[]} A list of graph IDs.
     */
    getGraphIdsWithParticipant : function(id, onResult) {
	this.createRequest("getGraphIdsWithParticipant", { id : id }, onResult).send();
    },
    
    /**
     * Gets a graph given its ID, containing only the given layers.
     * @param {string} id The given graph ID.
     * @param {string[]} layerId The IDs of the layers to load, or null for all layers. If only graph data is required, set this to ["graph"].
     * @callback {resultCallback} onResult Invoked when the request has returned a result.
     * @return The identified graph.
     */
    getGraph : function (id, layerId, onResult) {
	this.createRequest("getGraph", { id : id, layerId : layerId }, onResult).send();
    },
    
    /**
     * Gets the number of annotations on the given layer of the given graph.
     * @param {string} id The given graph ID.
     * @param {string} layerId The ID of the layer to load.
     * @callback {resultCallback} onResult Invoked when the request has returned a result.
     * @return The identified graph.
     */
    countAnnotations : function (id, layerId, onResult) {
	this.createRequest("countAnnotations", { id : id, layerId : layerId }, onResult).send();
    },
    
    /**
     * Gets the annotations on the given layer of the given graph.
     * @param {string} id The given graph ID.
     * @param {string} layerId The ID of the layer to load.
     * @param {int} pageLength The number of annotations per page (or null for one big page with all annotations on it).
     * @param {int} pageNumber The page number to return (or null for the first page).
     * @callback {resultCallback} onResult Invoked when the request has returned a result.
     * @return The identified graph.
     */
    getAnnotations : function (id, layerId, pageLength, pageNumber, onResult) {
	this.createRequest("getAnnotations", { id : id, layerId : layerId, pageLength : pageLength, pageNumber : pageNumber }, onResult).send();
    },
        
    /**
     * Gets the given anchors in the given graph.
     * @param {string} id The given graph ID.
     * @param {string[]} anchorId The IDs of the anchors to load.
     * @callback {resultCallback} onResult Invoked when the request has returned a result.
     * @return The identified graph.
     */
    getAnchors : function (id, anchorId, onResult) {
	this.createRequest("getAnchors", { id : id, anchorId : anchorId }, onResult).send();
    },
        
    /**
     * List the predefined media tracks available for transcripts.
     * @callback {resultCallback} onResult Invoked when the request has returned a result.
     * @return An ordered list of media track definitions.
     */
    getMediaTracks : function(onResult) {
	this.createRequest("getMediaTracks", null, onResult).send();
    },
    
    /**
     * List the media available for the given graph.
     * @param {string} id The graph ID.
     * @callback {resultCallback} onResult Invoked when the request has returned a result.
     * @return List of media files available for the given graph.
     */
    getAvailableMedia : function(id, onResult) {
	this.createRequest("getAvailableMedia", { id : id }, onResult).send();
    },
    
    /**
     * Gets a given media track for a given graph.
     * @param {string} id The graph ID.
     * @param {string} trackSuffix The track suffix of the media - see {@link MediaTrackDefinition#suffix}.
     * @param {string} mimeType The MIME type of the media.
     * @callback {resultCallback} onResult Invoked when the request has returned a result.
     * @return {string} A URL to the given media for the given graph, or null if the given media doesn't exist.
     */
    getMedia : function(id, trackSuffix, mimeType, onResult) {
	this.createRequest("getMedia", { id : id, trackSuffix : trackSuffix, mimeType : mimeType}, onResult).send();
    }
    
}

// GraphStore class - read/write "edit" access

nzilbb.labbcat.GraphStore = function(baseUrl) {
    nzilbb.labbcat.GraphStoreQuery.call(this, baseUrl);
}

nzilbb.labbcat.GraphStore.prototype = Object.create(nzilbb.labbcat.GraphStoreQuery.prototype);

/**
 * Saves the given graph. The graph can be partial e.g. include only some of the layers that the stored version of the graph contains.
 * @param graph The graph to save.
 * @callback {resultCallback} onResult Invoked when the request has returned a result.
 * @return true if changes were saved, false if there were no changes to save.
 */
nzilbb.labbcat.GraphStore.prototype.saveGraph = function(graph, onResult) { // TODO
};
    
/**
 * Saves the given media for the given graph
 * @param {string} id The graph ID
 * @param {string} trackSuffix The track suffix of the media - see {@link MediaTrackDefinition#suffix}.
 * @param {string} mediaUrl A URL to the media content.
 * @callback {resultCallback} onResult Invoked when the request has returned a result.
 */
nzilbb.labbcat.GraphStore.prototype.saveMedia = function(id, trackSuffix, mediaUrl, onResult) { // TODO
};

/**
 * Saves the given source file (transcript) for the given graph.
 * @param {string} id The graph ID
 * @param {string} url A URL to the transcript.
 * @callback {resultCallback} onResult Invoked when the request has returned a result.
 */
nzilbb.labbcat.GraphStore.prototype.saveSource = function(id, url, onResult) {
};

/**
 * Saves the given document for the episode of the given graph.
 * @param {string} id The graph ID
 * @param {string} url A URL to the document.
 * @callback {resultCallback} onResult Invoked when the request has returned a result.
 */
nzilbb.labbcat.GraphStore.prototype.saveEpisodeDocument = function(id, url, onResult) {
};

nzilbb.labbcat.GraphStore.prototype.constructor = nzilbb.labbcat.GraphStore;

// Labbcat class - GraphStore plus some LaBB-CAT specific functions

nzilbb.labbcat.Labbcat = function(baseUrl, username, password) {
    nzilbb.labbcat.GraphStore.call(this, baseUrl); 
    if (!/\/$/.test(baseUrl)) baseUrl += "/";
    this.baseUrl = baseUrl;
    this.username = username;
    this.password = password;
}

nzilbb.labbcat.Labbcat.prototype = Object.create(nzilbb.labbcat.GraphStore.prototype);

/**
 * Uploads a new transcript.
 * @param {file} transcript The transcript to upload.
 * @param {file|file[]} media The media to upload, if any.
 * @param {string} mediaSuffix The media suffix for the media.
 * @param {string} transcriptType The transcript type.
 * @param {string} corpus The corpus for the transcript.
 * @param {string} episode The episode the transcript belongs to.
 * @callback {resultCallback} onResult Invoked when the request has returned a result.
 * @callback onProgress Invoked on XMLHttpRequest progress.
 */
nzilbb.labbcat.Labbcat.prototype.newTranscript = function(transcript, media, mediaSuffix, transcriptType, corpus, episode, onResult, onProgress) {
    // create form
    var fd = new FormData();
    fd.append("todo", "new");
    fd.append("auto", "true");
    if (transcriptType) fd.append("transcript_type", transcriptType);
    if (corpus) fd.append("corpus", corpus);
    if (episode) fd.append("episode", episode);

    if (!runningOnNode) {	

	fd.append("uploadfile1_0", transcript);
	if (media) {
	    if (!mediaSuffix) mediaSuffix = "";
	    if (media.constructor === Array) { // multiple files
		for (var f in media) {
		    fd.append("uploadmedia"+mediaSuffix+"1", media[f]);
		} // next file
            } else { // a single file
		fd.append("uploadmedia"+mediaSuffix+"1", media);
	    }
	}
    
	// create HTTP request
	var xhr = new XMLHttpRequest();
	xhr.call = "newTranscript";
	xhr.id = transcript.name;
	xhr.onResult = onResult;
	xhr.addEventListener("load", callComplete, false);
	xhr.addEventListener("error", callFailed, false);
	xhr.addEventListener("abort", callCancelled, false);
	xhr.upload.addEventListener("progress", onProgress, false);
	xhr.upload.id = transcript.name; // for knowing what status to update during events
	
	xhr.open("POST", this.baseUrl + "edit/transcript/new");
	if (this.username) {
	    xhr.setRequestHeader("Authorization", "Basic " + btoa(this.username + ":" + this.password))
	}
	xhr.setRequestHeader("Accept", "application/json");
	xhr.send(fd);
    } else { // runningOnNode
	
	// on node.js, files are actually paths
	var transcriptName = transcript.replace(/.*\//g, "");
	fd.append("uploadfile1_0", 
		  fs.createReadStream(transcript).on('error', function(){
		      onResult(null, ["Invalid transcript: " + transcriptName], [], "newTranscript", transcriptName);
		  }), transcriptName);

	if (media) {
	    if (!mediaSuffix) mediaSuffix = "";
	    if (media.constructor === Array) { // multiple files
		for (var f in media) {
		    var mediaName = media[f].replace(/.*\//g, "");
		    try {
			fd.append("uploadmedia"+mediaSuffix+"1", 
				  fs.createReadStream(media[f]).on('error', function(){
				      onResult(null, ["Invalid media: " + mediaName], [], "newTranscript", transcriptName);
				  }), mediaName);
		    } catch(error) {
			onResult(null, ["Invalid media: " + mediaName], [], "newTranscript", transcriptName);
			return;
		    }
		} // next file
            } else { // a single file
		var mediaName = media.replace(/.*\//g, "");
		fd.append("uploadmedia"+mediaSuffix+"1", 
			  fs.createReadStream(media).on('error', function(){
			      onResult(null, ["Invalid media: " + mediaName], [], "newTranscript", transcriptName);
			  }), mediaName);
	    }
	}
	
	var urlParts = parseUrl(this.baseUrl + "edit/transcript/new");
	// for tomcat 8, we need to explicitly send the content-type and content-length headers...
	var labbcat = this;
	fd.getLength(function(something, contentLength) {
	    var requestParameters = {
		port: urlParts.port,
		path: urlParts.pathname,
		host: urlParts.hostname,
		headers: {
		    "Accept" : "application/json",
		    "content-length" : contentLength,
		    "Content-Type" : "multipart/form-data; boundary=" + fd.getBoundary()
		}
	    };
	    if (labbcat.username && labbcat.password) {
		requestParameters.auth = labbcat.username+':'+labbcat.password;
	    }
	    if (/^https.*/.test(labbcat.baseUrl)) {
		requestParameters.protocol = "https:";
	    }
	    fd.submit(requestParameters, function(err, res) {
		var responseText = "";
		if (!err) {
		    res.on('data',function(buffer) {
			//console.log('data ' + buffer);
			responseText += buffer;
		    });
		    res.on('end',function(){
			try {
			    var response = JSON.parse(responseText);
			    var result = response.model.result || response.model;
			    var errors = response.errors;
			    if (errors.length == 0) errors = null
			    var messages = response.messages;
			    if (messages.length == 0) messages = null
			    onResult(result, errors, messages, "newTranscript", transcriptName);
			} catch(exception) {
			    onResult(null, ["" +exception+ ": " + labbcat.responseText], [], "newTranscript", transcript.name);
			}
		    });
		} else {
		    onResult(null, ["" +err+ ": " + labbcat.responseText], [], "newTranscript", transcriptName);
		}
		
		if (res) res.resume();
	    });
	}); // got length
    } // runningOnNode
};

/**
 * Uploads a new transcript.
 * @param {file} transcript The transcript to upload.
 * @callback {resultCallback} onResult Invoked when the request has returned a result.
 * @callback onProgress Invoked on XMLHttpRequest progress.
 */
nzilbb.labbcat.Labbcat.prototype.updateTranscript = function(transcript, onResult, onProgress) {
    // create form
    var fd = new FormData();
    fd.append("todo", "update");
    fd.append("auto", "true");

    if (!runningOnNode) {	

	fd.append("uploadfile1_0", transcript);
    
	// create HTTP request
	var xhr = new XMLHttpRequest();
	xhr.call = "newTranscript";
	xhr.id = transcript.name;
	xhr.onResult = onResult;
	xhr.addEventListener("load", callComplete, false);
	xhr.addEventListener("error", callFailed, false);
	xhr.addEventListener("abort", callCancelled, false);
	xhr.upload.addEventListener("progress", onProgress, false);
	xhr.upload.id = transcript.name; // for knowing what status to update during events
	
	xhr.open("POST", this.baseUrl + "edit/transcript/new");
	if (this.username) {
	    xhr.setRequestHeader("Authorization", "Basic " + btoa(this.username + ":" + this.password))
	}
	xhr.setRequestHeader("Accept", "application/json");
	xhr.send(fd);
    } else { // runningOnNode
	
	// on node.js, files are actually paths
	var transcriptName = transcript.replace(/.*\//g, "");
	fd.append("uploadfile1_0", 
		  fs.createReadStream(transcript).on('error', function(){
		      onResult(null, ["Invalid transcript: " + transcriptName], [], "newTranscript", transcriptName);
		  }), transcriptName);
	
	var urlParts = parseUrl(this.baseUrl + "edit/transcript/new");
	var requestParameters = {
	    port: urlParts.port,
	    path: urlParts.pathname,
	    host: urlParts.hostname,
	    headers: { "Accept" : "application/json" }
	};
	if (this.username && this.password) {
	    requestParameters.auth = this.username+':'+this.password;
	}
	if (/^https.*/.test(this.baseUrl)) {
	    requestParameters.protocol = "https:";
	}
	fd.submit(requestParameters, function(err, res) {
	    var responseText = "";
	    if (!err) {
		res.on('data',function(buffer) {
		    console.log('data ' + buffer);
		    responseText += buffer;
		});
		res.on('end',function(){
		    try {
			var response = JSON.parse(responseText);
			var result = response.model.result || response.model;
			var errors = response.errors;
			if (errors.length == 0) errors = null
			var messages = response.messages;
			if (messages.length == 0) messages = null
			onResult(result, errors, messages, "newTranscript", transcriptName);
		    } catch(exception) {
			onResult(null, ["" +exception+ ": " + this.responseText], [], "newTranscript", transcript.name);
		    }
		});
	    } else {
		onResult(null, ["" +err+ ": " + this.responseText], [], "newTranscript", transcriptName);
	    }

	    if (res) res.resume();
	});
    }
};

/**
 * Delete a transcript.
 * @param {string} id ID of the transcript.
 * @callback {resultCallback} onResult Invoked when the request has returned a result.
 */
nzilbb.labbcat.Labbcat.prototype.deleteTranscript = function(id, onResult) {
    this.createRequest("deleteTranscript", { id : id, transcript_id : id, btnConfirmDelete : true, chkDb : true }, onResult, this.baseUrl + "edit/transcript/delete").send();
};

/**
 * Gets list of tasks.
 * @callback {resultCallback} onResult Invoked when the request has returned a result.
 */
nzilbb.labbcat.Labbcat.prototype.getTasks = function(onResult) {
    this.createRequest("getTasks", null, onResult, this.baseUrl + "threads").send();
};

/**
 * Gets the status of a task.
 * @param {string} id ID of the task.
 * @callback {resultCallback} onResult Invoked when the request has returned a result.
 */
nzilbb.labbcat.Labbcat.prototype.taskStatus = function(id, onResult) {
    this.createRequest("taskStatus", { id : id, threadId : id }, onResult, this.baseUrl + "thread").send();
};

/**
 * Releases a finished a task so it no longer uses resources on the server.
 * @param {string} id ID of the task.
 * @callback {resultCallback} onResult Invoked when the request has returned a result.
 */
nzilbb.labbcat.Labbcat.prototype.releaseTask = function(id, onResult) {
    this.createRequest("releaseTask", { id : id, threadId : id, command : "release" }, onResult, this.baseUrl + "threads").send();
};

nzilbb.labbcat.Labbcat.prototype.constructor = nzilbb.labbcat.Labbcat;

exports.Labbcat = nzilbb.labbcat.Labbcat;

}(typeof exports === 'undefined' ? this.labbcat = {} : exports));
