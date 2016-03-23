/**
 * @file nzilbb.labbcat module for communicating with a LaBB-CAT web application.
 *
 * @example
 * TODO
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

"use strict";

// namespace
var nzilbb = nzilbb || {};
nzilbb.labbcat = nzilbb.labbcat || {};

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
    var response = JSON.parse(this.responseText);
    var result = response.model.result;
    var errors = response.errors;
    if (errors.length == 0) errors = null
    var messages = response.messages;
    if (messages.length == 0) messages = null
    evt.target.onResult(result, errors, messages, evt.target.call, evt.target.id);
}
function callFailed(evt) {
    evt.target.onResult(null, ["failed: " + this.responseText], evt.target.call, evt.target.id);
}
function callCancelled(evt) {
    evt.target.onResult(null, ["cancelled"], evt.target.call, evt.target.id);
}

// GraphStoreQuery class - read-only "view" access

nzilbb.labbcat.GraphStoreQuery = function(baseUrl) {
    if (!baseUrl.endsWith("/")) baseUrl += "/";
    this.url = baseUrl + "store";
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
    createRequest : function(call, parameters, onResult) {
	var xhr = new XMLHttpRequest();
	xhr.call = call;
	if (parameters && parameters.id) xhr.id = parameters.id;
	xhr.onResult = onResult;
	xhr.addEventListener("load", callComplete, false);
	xhr.addEventListener("error", callFailed, false);
	xhr.addEventListener("abort", callCancelled, false);
	var queryString = "?call="+call;
	if (parameters) {
	    for (var key in parameters) {
		if (parameters[key].constructor === Array) {
		    for (var i in parameters[key]) {
			queryString += "&"+key+"="+encodeURIComponent(parameters[key][i])
		    }
		} else {
		    queryString += "&"+key+"="+encodeURIComponent(parameters[key])
		}
	    }
	}
	xhr.open("GET", this.url + queryString);
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
     * Gets a graph given its ID.
     * @param {string} id The given graph ID.
     * @callback {resultCallback} onResult Invoked when the request has returned a result.
     * @return The identified graph.
     */
    getGraph : function(resultCallback, id) {
	this.createRequest("getGraph", { id : id }, onResult);
    },
    
    /**
     * Gets a graph given its ID, containing only the given layers.
     * @param {string} id The given graph ID.
     * @param {string[]} layerId The IDs of the layers to load, or null if only graph data is required.
     * @callback {resultCallback} onResult Invoked when the request has returned a result.
     * @return The identified graph.
     */
    getGraph : function (id, layerId, onResult) {
	this.createRequest("getGraph", { id : id, layerId : layerId }, onResult).send();
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

nzilbb.labbcat.Labbcat = function(baseUrl) {
    nzilbb.labbcat.GraphStore.call(this, baseUrl); 
    if (!baseUrl.endsWith("/")) baseUrl += "/";
    this.baseUrl = baseUrl;
}

nzilbb.labbcat.Labbcat.prototype = Object.create(nzilbb.labbcat.GraphStore.prototype);

/**
 * Uploads a new transcript.
 * @param {file} transcript The transcript to upload.
 * @param {file} media The media to upload, if any.
 * @param {string} mediaSuffix The media suffix for the media.
 * @param {string} transcriptType The transcript type.
 * @param {string} corpus The corpus for the transcript.
 * @param {string} episode The expisode the transcript belongs to.
 * @callback {resultCallback} onResult Invoked when the request has returned a result.
 * @callback onProgress Invoked on XMLHttpRequest progress.
 */
nzilbb.labbcat.Labbcat.prototype.newTranscript = function(transcript, media, mediaSuffix, transcriptType, corpus, episode, onResult, onProgress) {
    // create form
    var fd = new FormData();
    fd.append("todo", "upload");
    fd.append("auto", "true");
    if (transcriptType) fd.append("transcript_type", transcriptType);
    if (corpus) fd.append("corpus", corpus);
    if (episode) fd.append("episode", episode);
    fd.append("uploadfile1_0", transcript);
    if (media) {
	if (!mediaSuffix) mediaSuffix = "";
	fd.append("uploadmedia"+mediaSuffix+"1", media);
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
    xhr.setRequestHeader("Accept", "application/json");
	xhr.send(fd);
};

nzilbb.labbcat.Labbcat.prototype.constructor = nzilbb.labbcat.Labbcat;
