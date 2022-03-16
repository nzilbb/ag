// show spinner
startLoading();

// show annotator version
getVersion(version=>{
    document.getElementById("version").innerHTML = version;
});

// Code editor
var editor = CodeMirror.fromTextArea(document.getElementById("script"), { lineNumbers: true });
editor.setSize(800, 600); // TODO can this be dynamic?

// default new layer name
document.getElementById("newLayerId").value = window.location.search.substring(1);

// first, get the layer schema
var schema = null;
getSchema(s => {
    schema = s;
    
    // set layer types for adding new layers
    document.getElementById("rootParent").value = schema.root.id;
    document.getElementById("turnParent").value = schema.turnLayerId;
    document.getElementById("wordParent").value = schema.wordLayerId;
    
    // GET request to getTaskParameters retrieves the current task parameters, if any
    getJSON("getTaskParameters", parameters => {
        try {
            if (parameters == null || parameters == "") {
                setScript("# for each turn in the transcript\n"
		          +"for turn in transcript.all(\""+schema.turnLayerId+"\"):\n"
		          +"  if annotator.cancelling: break; # cancelled by the user\n"
		          +"  # for each word\n"
		          +"  for word in turn.all(\""+schema.wordLayerId+"\"):\n"
		          +"    # change the following line to tag the word as desired \n"
		          +"    word.createTag(\""+window.location.search.substring(1)+"\", \"length: \" + str(len(word.label))\n"
		          +"    log(\"Tagged word \" + word.label)\n");
            } else {
                
                // set initial values of properties in the form
                setScript(parameters.script);
            }
        } finally {
            // hide spinner
            finishedLoading();
        }
    });
});

// this function detects when the user selects [add new layer]
function changedLayer(select) {
    if (select.value == "[add new layer]") {
        var newLayer = prompt( //  default is the task ID
            "Please enter the new layer ID", window.location.search.substring(1));
        if (newLayer) { // they didn't cancel
            // check there's not already a layer with that name
            for (var l in schema.layers) {
                var layer = schema.layers[l];
                if (layer.id == newLayer) {
                    alert("A layer called "+newLayer+" already exists");
                    select.selectedIndex = 0;
                    return;
                }
            } // next layer
            // add the layer to the list
            var layerOption = document.createElement("option");
            layerOption.appendChild(document.createTextNode(newLayer));
            select.appendChild(layerOption);
            var parentId = schema.wordLayerId;
            if (select.value == "[add new phrase layer]") {
                parentId = schema.turnLayerId;
            } else if (select.value == "[add new span layer]") {
                parentId = schema.root.id;
            }
            // select it
            select.selectedIndex = select.children.length - 1;
        } else {
            return false;
        }
    }
    return true;
}

function setScript(python) {
    var script = document.getElementById("script");
    script.value = python;
    editor.getDoc().setValue(python);
}

function loadScript() {
    var reader = new FileReader();
    var file = document.getElementById("loadLocalScript").files[0];
    reader.readAsText(file);
    reader.onload = function(event) {
        try {
	    var txt = event.target.result;
            setScript(txt);
        } catch(exception) {
	    alert("Unable to parse " + file.fileName + ": " + exception); 
        }
    };
    reader.onerror = function() {
        alert("Unable to read " + file.fileName);
    }; 
}

function saveScript() {
    try {
        var scriptAsBlob = new Blob([editor.getDoc().getValue()], { type:'text/plain' }); // TODO text/python
        var downloadLink = document.createElement("a");
        // name the file after the task by default
        var fileName = window.location.search.substring(1) + ".py";
        try {
            fileName = document.getElementById("loadLocalScript").files[0].name;
        } catch(x) {
        }
        downloadLink.download = fileName;
        downloadLink.style.display = "none";
        downloadLink.innerHTML = "Download File";
        downloadLink.href = (window.URL||window.webkitURL).createObjectURL(scriptAsBlob);
        document.body.appendChild(downloadLink);
        downloadLink.click();
    } catch(X) { alert(X); }
}

function setTaskParameters(form) {

    // validate script
    var pySource = document.getElementById("script");
    if (pySource.value.length == 0) {
        alert("You have not entered a script.");
        pySource.focus();
        return false;
    }
            
    return convertFormBodyToJSON(form);
}

function newLayer() {
    document.getElementById("addingLayer").style.display = "";
    document.getElementById("newLayerButton").style.display = "none";
    enableLayerAlignment();
}

function addLayer() {
    var newLayerId = document.getElementById("newLayerId").value;
    var newLayerParentId = document.getElementById("newLayerParentId").value;
    var newLayerAlignment = document.getElementById("newLayerAlignment").value;
    if (newLayerParentId != schema.wordLayerId && newLayerAlignment == "0") {
        // only word layers can be tags, so make it an interval by default
        newLayerAlignment = "2";
    }
    
    // check the new layer doesn't already exist
    if (schema.layers[newLayerId]) {
        alert("Layer already exists: " + newLayerId);
        return null;
    }

    // create the layer
    getJSON(resourceForFunction("newLayer", newLayerId, newLayerParentId, newLayerAlignment), s=>{
        // we get the new schema back
        schema = s;

        // finished adding, allow a new layer to be created
        document.getElementById("addingLayer").style.display = "none";
        document.getElementById("newLayerButton").style.display = "";
    });
}

function enableLayerAlignment() {
    var newLayerAlignment = document.getElementById("newLayerAlignment");
    var newLayerAlignment0 = document.getElementById("newLayerAlignment0");
    var newLayerParentId = document.getElementById("newLayerParentId")
    if (newLayerParentId.value == schema.wordLayerId) {
        newLayerAlignment0.disabled = false;
        newLayerAlignment.value = "0"; // tag by default
    } else {  // span/phrase layer
        newLayerAlignment0.disabled = true;
        newLayerAlignment.value = "2"; // interval by default
    }
}

// add event handlers
document.getElementById("loadLocalScript").onchange = function(e) { loadScript(); };
document.getElementById("saveLocalScript").onclick = function(e) { saveScript(); return false; };
document.getElementById("newLayerButton").onclick = function(e) { newLayer(); return false; };
document.getElementById("addLayerButton").onclick = function(e) { addLayer(); return false; };
document.getElementById("newLayerParentId").onchange = function(e) { enableLayerAlignment(); };
document.getElementById("form").onsubmit = function(e) { return setTaskParameters(this); };
