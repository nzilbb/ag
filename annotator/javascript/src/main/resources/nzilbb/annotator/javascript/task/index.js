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
    
    var sourceLayerId = document.getElementById("sourceLayerId");
    addLayerOptions(sourceLayerId, schema, layer => true);
    // select the word layer by default
    sourceLayerId.value = schema.wordLayerId;

    var destinationLayerId = document.getElementById("destinationLayerId");
    destinationLayerId.selectedIndex = 0;

    // populate the language layers...
    
    var transcriptLanguageLayerId = document.getElementById("transcriptLanguageLayerId");
    addLayerOptions(
        transcriptLanguageLayerId, schema,
        layer => layer.parentId == schema.root.id && layer.alignment == 0
            && /.*lang.*/.test(layer.id));
    // select the first one by default
    transcriptLanguageLayerId.selectedIndex = 1;
    
    var phraseLanguageLayerId = document.getElementById("phraseLanguageLayerId");
    addLayerOptions(
        phraseLanguageLayerId, schema,
        layer => layer.parentId == schema.turnLayerId && layer.alignment == 2
            && /.*lang.*/.test(layer.id));
    // select the first one by default
    phraseLanguageLayerId.selectedIndex = 1;

    // set layer types for adding new layers
    document.getElementById("rootParent").value = schema.root.id;
    document.getElementById("turnParent").value = schema.turnLayerId;
    document.getElementById("wordParent").value = schema.wordLayerId;
    
    // GET request to getTaskParameters retrieves the current task parameters, if any
    getJSON("getTaskParameters", parameters => {
        if (parameters == null || parameters == "") {
            document.getElementById("labelMappingFalse").checked = true;
            setScript("// for each turn in the transcript\n"
		      +"for each (turn in transcript.all(\""+schema.turnLayerId+"\")) {\n"
		      +"  if (annotator.cancelling) break; // cancelled by the user\n"
		      +"  // for each word\n"
		      +"  for each (word in turn.all(\""+schema.wordLayerId+"\")) {\n"
		      +"    // ** change the following line to tag the word as desired **\n"
		      +"    tag = word.createTag(\""+window.location.search.substring(1)+"\", \"length: \" + word.label.length());\n"
		      +"    log(\"Tagged word \" + word.label + \" with \" + tag.label);\n"
		      +"  } // next word\n"
		      +"} // next turn");
        } else {
            
            // set initial values of properties in the form
            setScript(parameters.script);
            document.getElementById("transcriptLanguageLayerId").value
                = parameters.transcriptLanguageLayerId;
            document.getElementById("phraseLanguageLayerId").value
                = parameters.phraseLanguageLayerId;
            document.getElementById("language").value = parameters.language;
            if (parameters.labelMapping == "true") {
                document.getElementById("labelMappingTrue").checked = true;
                document.getElementById("labelScript").value = parameters.script;
            } else {
                document.getElementById("labelMappingFalse").checked = true;
            }
            if (parameters.labelMapping == "true") {
                // source/dest layers are the first element in the corresponding arrays
                document.getElementById("sourceLayerId").value = parameters.sourceLayerId;
                changeSourceLayer();
                destinationLayerId.value = parameters.destinationLayerId;
                // if there's no option for that layer, add one
                if (parameters.destinationLayerId
                    && destinationLayerId.value != parameters.destinationLayerId) {
                    var layerOption = document.createElement("option");
                    layerOption.appendChild(
                        document.createTextNode(parameters.destinationLayerId));
                    destinationLayerId.appendChild(layerOption);
                    destinationLayerId.value = parameters.destinationLayerId;
                }
            } // labelMapping
        }
        checkMode();
        updateLabelScript();
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

function checkMode() {
    if (document.getElementById("labelMappingTrue").checked) {
        document.getElementById("scriptConfiguration").style.display = "none";
        document.getElementById("labelMappingConfiguration").style.display = "";
        document.getElementById("labelScript").focus();
    } else {
        document.getElementById("scriptConfiguration").style.display = "";
        document.getElementById("labelMappingConfiguration").style.display = "none";
        editor.focus();
    }
}

function setScript(js) {
    var script = document.getElementById("script");
    script.value = js;
    editor.getDoc().setValue(js);
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
        var scriptAsBlob = new Blob([editor.getDoc().getValue()], { type:'text/plain' }); // TODO text/javascript
        var downloadLink = document.createElement("a");
        // name the file after the task by default
        var fileName = window.location.search.substring(1) + ".js";
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

// ensure language regular expression is validated
var language = document.getElementById("language");
language.onkeyup = function() { validateRegularExpression(language); };

function validateRegularExpression(input) {
    if (input.value.length == 0) {
        input.className = "";
        input.title = "";
    } else {
        try {        
            // test regular expression is valid
            new RegExp(input.value);
            // pattern is valid, so don't mark it as an error
            input.className = "";
            input.title = "";
        } catch(error) {
            // pattern is invalid, so don't mark it as an error
            input.className = "error";
            input.title = error;
        }
    }
    return input.className == "";
}

function setTaskParameters(form) {

    var labelMapping = document.getElementById("labelMappingTrue").checked;
    
    // validate script
    if (labelMapping) {
        var jsSource = document.getElementById("labelScript");
        if (jsSource.value.length == 0) {
            alert("You have not entered a script.");
            jsSource.focus();
            return false;
        }
        // save the script as "script"
        document.getElementById("script").value = jsSource.value;
    } else { // full script
        var jsSource = document.getElementById("script");
        if (jsSource.value.length == 0) {
            alert("You have not entered a script.");
            jsSource.focus();
            return false;
        }
    }

    if (labelMapping) {
        
        // validate output layer
        if (document.getElementById("destinationLayerId").selectedIndex < 2) {
            alert("Please select a destination layer.");
            document.getElementById("destinationLayerId").focus();
            return false;
        }

        // validate language
        var language = document.getElementById("language");
        if (!validateRegularExpression(language)) {
            alert("Language is not a valid regular expression:\n" + language.title);
            language.focus();
            return false;
        }
        
    }
        
    return convertFormBodyToJSON(form);
}

function changeSourceLayer() {
    updateLabelScript();
    // force destination layers to be the same scope
    var sourceLayerId = document.getElementById("sourceLayerId");
    var sourceLayer = schema.layers[sourceLayerId.value];
    if (sourceLayer) {
        var destinationLayerId = document.getElementById("destinationLayerId");
        // remove existing options
        var options = destinationLayerId.options;
        for (o = 2; o < options.length; o++) {
            options[o].remove();
        }
        // add options only in the same scope as the sourceLayerId
        addLayerOptions(
            destinationLayerId, schema,
            // layers in the same scope
            layer => layer.parentId == sourceLayer.parentId
            // but not the layer itself
                && layer.id != sourceLayer.id
            // nor any formal schema layers
                && layer.id != schema.root.id
                && layer.id != schema.turnLayerId
                && layer.id != schema.utteranceLayerId
                && layer.id != schema.participantLayerId
                && layer.id != schema.wordLayerId
                && layer.id != schema.corpusLayerId);
    }

}

function changeDestinationLayer() {
    if (!changedLayer(document.getElementById("destinationLayerId"))) {
        return false;
    }
    updateLabelScript();
}

function updateLabelScript() {
    // update sourceName sourceVar destinationName
    var sourceName = document.getElementById("sourceLayerId").value;
    var elements = document.getElementsByClassName("sourceName");
    for (e = 0; e < elements.length; e++) {
        elements[e].innerHTML = sourceName;
    }
    elements = document.getElementsByClassName("sourceVar");
    for (e = 0; e < elements.length; e++) {
        elements[e].innerHTML = sourceName.replace(/[^a-zA-Z_]/,"");
    }
    destinationLayerId = document.getElementById("destinationLayerId")
    if (destinationLayerId.selectedIndex > 1) { // destination has actually been selected
        var destinationName = destinationLayerId.value;
        elements = document.getElementsByClassName("destinationName");
        for (e = 0; e < elements.length; e++) {
            elements[e].innerHTML = destinationName;
        }
    }
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
document.getElementById("sourceLayerId").onchange = function(e) { changeSourceLayer(); };
document.getElementById("destinationLayerId").onchange = function(e) { return changeDestinationLayer(); };
document.getElementById("labelMappingFalse").onchange = function(e) { checkMode(); };
document.getElementById("labelMappingTrue").onchange = function(e) { checkMode(); };
document.getElementById("loadLocalScript").onchange = function(e) { loadScript(); };
document.getElementById("saveLocalScript").onclick = function(e) { saveScript(); return false; };
document.getElementById("newLayerButton").onclick = function(e) { newLayer(); return false; };
document.getElementById("addLayerButton").onclick = function(e) { addLayer(); return false; };
document.getElementById("newLayerParentId").onchange = function(e) { enableLayerAlignment(); };
document.getElementById("form").onsubmit = function(e) { return setTaskParameters(this); };
