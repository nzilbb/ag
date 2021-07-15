// Some useful functions for annotator web-apps:

// make a GET request of the annotator
function get(path, onload, contentType) {
    var request = new XMLHttpRequest();
    request.open("GET", path);
    request.setRequestHeader("Accept", contentType || "text/plain");
    request.addEventListener("load", onload, false);
    request.send();
}

// make a GET a JSON object from the annotator
function getJSON(path, gotJSON) {
    get(path, function(e) {
        var json = null;
        if (this.responseText) {
            try {
                json = JSON.parse(this.responseText);
            } catch (x) {
                console.log("getJSON could not parse: " + this.responseText);
                console.log("getJSON: " + x);
            }
        }
        gotJSON(json);
    }, "application/json");
}

// make a GET a text string from the annotator
function getText(path, gotText) {
    get(path, function(e) { gotText(this.responseText); }, "text/plain");
}

// encode a function parameter
function encode(parameter) {
    return encodeURI(parameter).replace(",","%2C");
}

// encode a function parameter
function resourceForFunction(functionName, param1, param2, param3, param4, param5) {
    var resource = functionName;
    if (param1 != null) {
        resource += "?" + encode(param1);
        if (param2 != null) {
            resource += "," + encode(param2);
            if (param3 != null) {
                resource += "," + encode(param3);
                if (param4 != null) {
                    resource += "," + encode(param4);
                    if (param5 != null) {
                        resource += "," + encode(param5);
                    }
                }
            }
        }
    }
    return resource;
}

// get the layer schema being used
function getSchema(gotSchema) {
    getJSON("getSchema", gotSchema);
}

// get the version of the annotator implementation
function getVersion(gotVersion) {
    getText("getVersion", gotVersion);
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

// Normally an HTML form sends its body with url-encoded parameters like this:
// ?foo=value1&bar=value2
// If you want an HTML form to instead post a JSON-encoded object, like this:
// {"foo":"value1","bar","value2"}
// ...then call this function in the "onsubmit" event of the form, i.e.:
// <form onsubmit="convertFormBodyToJSON(this)">
// If you want to send some extra, more complex attributes in the body,
// (beyond the named input controls) use the second parameter for this, e.g.
// <form onsubmit="convertFormBodyToJSON(this, { anArrayOfObjects:[{foo:1,bar:2},{foo:3,bar4}] })">
function convertFormBodyToJSON(form, body) {

    body = body||{};

    // for each form input
    var inputs = form.elements;
    for (i = 0; i < inputs.length; i++) {
        if (inputs[i].name) {
            // skip unchecked radio buttons / checkboxes
            if ((inputs[i].type != "radio" && inputs[i].type != "checkbox")
                || inputs[i].checked) {
                // add the parameter to the body
                body[inputs[i].name] = inputs[i].value;
            }
        }
        // disable the input
        inputs[i].setAttribute("disabled", "");
    } // next input

    // add a new input for the JSON
    // although the browser will send the body in the form name=value
    // we trick the browser into sending valid JSON by ensuring that
    // the end of 'name' looks like the beginning of a new JSON attribute: ,"":"
    // and the value looks like the end of the JSON object: "}
    // so name=value is send as ,"":"="}
    // this cunning idea comes from:
    // https://systemoverlord.com/2016/08/24/posting-json-with-an-html-form.html
    var jsonInput = document.createElement("input");
    jsonInput.style.display = "none";
    jsonInput.type = "text";
    jsonInput.name = JSON.stringify(body).replace(/}$/,',"":"');
    jsonInput.value = '"}';
    form.appendChild(jsonInput);
    
    // set enctype to plain text to prevent URL-encoding
    form.enctype = "text/plain";

    return true;
}

// Cover the page with a <div> element with a spinner, to prevent the user
// from interacting with the page while data and settings are loaded.
function startLoading() {
    if (!document.getElementById("loadingStyle")) {
        // add style for loading panel
        var style = document.head.appendChild(document.createElement("style"));
        style.id = "loadingStyle";
        // CSS-only spinner thanks to https://stephanwagner.me/only-css-loading-spinner
        style.innerHTML
            ="@keyframes spinner {"
            +"\n  to {transform: rotate(360deg);}"
            +"\n}"
            +"\n#loading {"
            +"\n    position: absolute;"
            +"\n    width: 100%;"
            +"\n    height: 100%;"
            +"\n    left: 0px;"
            +"\n    top: 0px;"
            +"\n    z-index: 2;"
            +"\n    cursor: wait;"
            +"\n    background: white;"
            +"\n    opacity: 0.5;"
            +"\n}"
            +"\n#loading:before {"
            +"\n  content: '';"
            +"\n  box-sizing: border-box;"
            +"\n  position: absolute;"
            +"\n  top: 50%;"
            +"\n  left: 50%;"
            +"\n  width: 20px;"
            +"\n  height: 20px;"
            +"\n  margin-top: -10px;"
            +"\n  margin-left: -10px;"
            +"\n  border-radius: 50%;"
            +"\n  border: 2px solid #ccc;"
            +"\n  border-top-color: #859044;"
            +"\n  animation: spinner .6s linear infinite;"
            +"\n}";
    }        
    if (!document.getElementById("loading")) {
        var loading = document.createElement("div");
        loading.id = "loading";
        document.body.appendChild(loading);
    }
}

// Remove the <div> element spinner element previously create by startLoading(),
// to allow the user to interact with the page.
function finishedLoading() {
    var loading = document.getElementById("loading");
    if (loading) loading.remove();
}
