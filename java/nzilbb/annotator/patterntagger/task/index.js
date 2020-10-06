// TODO handle phrase/spanning destination layer

// show annotator version
getText("getVersion", function(e) {
    document.getElementById("version").innerHTML = this.responseText;
});

// first, get the layer schema
var schema = null;
getJSON("getSchema", function(e) {
    schema = JSON.parse(this.responseText);
    
    // populate layer input select options...          
    var sourceLayerId = document.getElementById("sourceLayerId");
    addLayerOptions(
        sourceLayerId, schema,
        // this is a function that takes a layer and returns true for the ones we want
        layer => layer.id == schema.wordLayerId
            || (layer.parentId == schema.wordLayerId && layer.alignment == 0));
    // default value:
    if (schema.layers["orthography"]) {
        sourceLayerId.value = "orthography";
    } else {
        sourceLayerId.value = schema.wordLayerId;
    }
    
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

    // populate layer destination select options...          
    var destinationLayerId = document.getElementById("destinationLayerId");
    addLayerOptions(
        destinationLayerId, schema,
        layer => layer.parentId == schema.wordLayerId && layer.alignment == 0);
    destinationLayerId.selectedIndex = 0;
    
    // GET request to getTaskParameters retrieves the current task parameters, if any
    getJSON("getTaskParameters"+window.location.search, function(e) {
        if (!this.responseText) {
            newMapping("", ""); // no parameters, start off with a blank one
        } else {
            var parameters = JSON.parse(this.responseText);
            
            // set initial values of properties in the form
            document.getElementById("sourceLayerId").value = parameters.sourceLayerId;
            document.getElementById("transcriptLanguageLayerId").value
                = parameters.transcriptLanguageLayerId;
            document.getElementById("phraseLanguageLayerId").value
                = parameters.phraseLanguageLayerId;
            document.getElementById("language").value = parameters.language;
            document.getElementById("nonMatchAction").value = parameters.nonMatchAction;
            document.getElementById("destinationLayerId").value = parameters.destinationLayerId;
            
            // insert current mappings
            if (!parameters.mappings) {
                newMapping("", ""); // no mappings, start off with a blank one
            } else {
                for (var mapping of parameters.mappings) {
                    newMapping(mapping.pattern, mapping.representation);
                } // next mapping
            }
        }
    });
});

// this function detects when the user selects [add new layer]:
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
            // select it
            select.selectedIndex = select.children.length - 1;
        }
    }
}

// ensure language regular expression is validated
var language = document.getElementById("language");
language.onkeyup = function() { validateRegularExpression(language); };

var lastMapping = null;

// Manage mappings

function newMapping(pattern, representation) {
    
    var divMapping = document.createElement("div");
    
    var inputSource = document.createElement("input");
    inputSource.type = "text";
    inputSource.name = "pattern";
    inputSource.value = pattern;
    inputSource.title = "regular-expression pattern to match source annotations";
    inputSource.placeholder = "Source Pattern";
    inputSource.style.width = "25%";
    inputSource.style.textAlign = "center";
    inputSource.onfocus = function() { lastMapping = this.parentNode; };
    inputSource.onkeyup = function() { validateRegularExpression(inputSource); };
    
    var inputDestination = document.createElement("input");
    inputDestination.type = "text";
    inputDestination.name = "representation";
    inputDestination.title = "You can type text or select a layer to copy from."
	+"\nLeaving this blank saves no representation, but"
	+" prevents the patterns below from matching.";
    inputDestination.placeholder = "Destination Label";
    inputDestination.value = representation;
    inputDestination.style.width = "25%";
    inputDestination.style.textAlign = "center";
    inputDestination.onfocus = function() { lastMapping = this.parentNode; };

    // TODO <c:if test="${!spanningLayer}">
    var COPY_FROM_LAYER_TEXT = "Copy from layer: ";
    var copyFromLayer = document.createElement("select");
    copyFromLayer.name = "copy";
    copyFromLayer.title = "Label for annotation can be copied from another layer, or some specified text.";
    copyFromLayer.onfocus = function() { lastMapping = this.parentNode; };
    var option = document.createElement("option");
    option.appendChild(document.createTextNode("Specific text"));
    option.value = "";
    copyFromLayer.appendChild(option);
    var copying = false;
    
    for (var layerId in schema.layers) {
        console.log("layer " + layerId + " - " + JSON.stringify(schema.layers[layerId]));
        var layer = schema.layers[layerId];
        if (layer.parentId == schema.wordLayerId && layer.alignment == 0) { // word tag
            option = document.createElement("option");
            option.value = COPY_FROM_LAYER_TEXT + layer.id;
            option.appendChild(document.createTextNode(COPY_FROM_LAYER_TEXT + layer.description));
            if (representation == COPY_FROM_LAYER_TEXT + layer.id) {
                option.selected = true;
                copying = true;
            }
            copyFromLayer.appendChild(option);
        } // permitted layer
    } // next layer

    copyFromLayer.inputDestination = inputDestination;
    copyFromLayer.onchange = function() {
        this.inputDestination.style.display = this.selectedIndex > 0?"none":"";
    };
    if (copying) {
        inputDestination.style.display = "none";
    }
    
    var arrow = document.createElement("span");
    arrow.innerHTML = " → ";
    
    divMapping.appendChild(inputSource);
    divMapping.appendChild(arrow);
    // TODO <c:if test="${!spanningLayer}">:
    divMapping.appendChild(copyFromLayer);    
    divMapping.appendChild(inputDestination);

    document.getElementById("mappings").appendChild(divMapping);
    inputSource.focus();
    
    enableRemoveButton();
}

function enableRemoveButton() {
    console.log("enableRemoveButton " + document.getElementById("mappings").childElementCount);
    document.getElementById("removeButton").disabled = 
        document.getElementById("mappings").childElementCount <= 1;
}

function removeMapping() {
    if (lastMapping) { 
        document.getElementById("mappings").removeChild(lastMapping);
        lastMapping = null;
        enableRemoveButton();
    }
}

function moveMappingUp() {
    if (lastMapping) { 
        var mappings = document.getElementById("mappings");
        var previousMapping = lastMapping.previousSibling;
        if (previousMapping) {
            mappings.removeChild(lastMapping);
            mappings.insertBefore(lastMapping, previousMapping);
        }
    }
}

function moveMappingDown() {
    if (lastMapping) { 
        var mappings = document.getElementById("mappings");
        var nextMapping = lastMapping.nextSibling;
        if (nextMapping) {
            var nextNextMapping = nextMapping.nextSibling;
            mappings.removeChild(lastMapping);
            if (nextNextMapping) {
                mappings.insertBefore(lastMapping, nextNextMapping);
            } else {
                mappings.appendChild(lastMapping);
            }
        }
    }
}

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
}
