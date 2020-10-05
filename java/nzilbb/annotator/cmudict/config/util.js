// Some useful functions:

// make a GET request of the annotator
function get(path, onload, contentType) {
    var request = new XMLHttpRequest();
    request.open("GET", path);
    request.setRequestHeader("Accept", contentType || "text/plain");
    request.addEventListener("load", onload, false);
    request.send();
}

// make a GET a JSON object from the annotator
function getJSON(path, onload) {
    get(path, onload, "application/json");
}

// make a GET a text string from the annotator
function getText(path, onload) {
    get(path, onload, "text/plain");
}

// encode a function parameter
function encode(parameter) {
    return encodeURI(parameter).replace(",","%2C");
}

// populate a <select> element with layers for which a predicate is true
function addLayerOptions(select, schema, layerPredicate) {
    for (var layerId in schema.layers) {
        if (!layerPredicate || layerPredicate(schema.layers[layerId])) {
            var option = document.createElement("option");
            option.appendChild(document.createTextNode(layerId));
            select.appendChild(option);
        } // permitted layer
    } // next layer
}

// upload a file, using PUT, which does not send the file name
// e.g. putFile("uploadLexicon", input.files[0], function(e) { console.log(this.responseText);});
function putFile(path, file, onUploaded, onProgress, onFailed) {
    var request = new XMLHttpRequest();
    request.open("PUT", path);
    request.setRequestHeader("Accept", "text/plain");
    if (onUploaded) {
        request.addEventListener("load", onUploaded, false);
        request.addEventListener("error", onFailed|onUploaded, false);
    }
    if (onProgress) request.addEventListener("progress", onProgress, false);
    request.send(file);
}

// make a multipart POST request, so multiple files and other parameters are supported
// e.g.
//  var fd = new FormData();
//  fd.append("file", input.files[0]);
//  postForm("uploadLexicon", fd, function(e) { console.log(this.responseText);});
function postForm(path, formData, onUploaded, onProgress, onFailed) {
    var request = new XMLHttpRequest();
    request.open("POST", path);
    request.setRequestHeader("Accept", "text/plain");
    console.log("onUploaded " + onUploaded);
    if (!onFailed) onFailed = onUploaded;
    if (onUploaded) request.addEventListener("load", onUploaded, false);
    if (onFailed) request.addEventListener("error", onFailed, false);
    if (onProgress) request.addEventListener("progress", onProgress, false);
    request.send(formData);
}
