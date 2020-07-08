// Some useful functions:

// make a GET request of the annotator
function get(path, onload, contentType) {
    var request = new XMLHttpRequest();
    request.open("GET", path);
    if (contentType) request.setRequestHeader("Accept", contentType);
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
