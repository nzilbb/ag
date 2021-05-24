// TODO handle phrase/spanning destination layer

// show annotator version
getVersion(version=>{
    document.getElementById("version").innerHTML = version;
});

var taskId = window.location.search.substring(1);

// first, get the layer schema
var schema = null;
getSchema(s => {
    schema = s;
    
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
    getJSON("getTaskParameters", parameters => {
        if (parameters == null) {
            newMapping("", ""); // no parameters, start off with a blank one
        } else {
            
            // set initial values of properties in the form
            document.getElementById("sourceLayerId").value = parameters.sourceLayerId;
            document.getElementById("transcriptLanguageLayerId").value
                = parameters.transcriptLanguageLayerId;
            document.getElementById("phraseLanguageLayerId").value
                = parameters.phraseLanguageLayerId;
            document.getElementById("language").value = parameters.language;
            document.getElementById("deleteOnNoMatch").value = parameters.deleteOnNoMatch;
            destinationLayerId = document.getElementById("destinationLayerId");
            // if there's no destination layer defined
            if (!parameters.destinationLayerId
                // but there's a layer named after the task
                && schema.layers[taskId]) {
                destinationLayerId.value = taskId;
            } else { // there is a destination layer defined
                destinationLayerId.value = parameters.destinationLayerId;
                // if there's no option for that layer, add one
                if (destinationLayerId.value != parameters.destinationLayerId) {
                    var layerOption = document.createElement("option");
                    layerOption.appendChild(document.createTextNode(parameters.destinationLayerId));
                    destinationLayerId.appendChild(layerOption);
                    destinationLayerId.value = parameters.destinationLayerId;
                }
            }
            // insert current mappings
            if (!parameters.mappings) {
                newMapping("", ""); // no mappings, start off with a blank one
            } else {
                for (var mapping of parameters.mappings) {
                    newMapping(mapping.pattern, mapping.label);
                } // next mapping
            }
            
            // set destination parent layer
            var destinationLayerParentId = parameters.destinationLayerParentId;
            if (!destinationLayerParentId) { // infer it
                var destinationLayer = schema.layers[parameters.destinationLayerId];
                destinationLayerParentId = schema.wordLayerId;
                if (destinationLayer) {
                    destinationLayerParentId = destinationLayer.parentId;
                }
            }
            document.getElementById("destinationLayerParentId").value = destinationLayerParentId;
            enableLanguageParameters();
            
        }
    });
});

// this function detects when the user selects [add new layer]
function changedLayer(select) {
    if (select.value == "[add new word layer]"
        || select.value == "[add new phrase layer]"
        || select.value == "[add new span layer]") {
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
            document.getElementById("destinationLayerParentId").value = parentId;
            // select it
            select.selectedIndex = select.children.length - 1;
        }
    } else {
        var selectedLayer = schema.layers[select.value];
        if (selectedLayer) {
            document.getElementById("destinationLayerParentId").value = selectedLayer.parentId;
        }
    }
    enableLanguageParameters();
}

// check whether languguage-related fields should be visible
function enableLanguageParameters() {
    var destinationLayerParentId = document.getElementById("destinationLayerParentId").value;
    var displayLanguageFields = destinationLayerParentId == schema.wordLayerId?"":"none";
    var languageFields = document.getElementsByClassName("language-field");
    for (var f = 0; f < languageFields.length; f++) {
        languageFields[f].style.display = displayLanguageFields;
    }
    var mappingOptions = document.getElementsByClassName("label-processing");
    for (var o = 0; o < mappingOptions.length; o++) {
        mappingOptions[o].style.display = displayLanguageFields;
    }
}

// ensure language regular expression is validated
var language = document.getElementById("language");
language.onkeyup = function() { validateRegularExpression(language); };

var lastMapping = null;

// Manage mappings

function newMapping(pattern, label) {
    
    var divMapping = document.createElement("div");
    
    var patternInput = document.createElement("input");
    patternInput.type = "text";
    patternInput.dataset.role = "pattern";
    patternInput.value = pattern;
    patternInput.title = "Regular-expression pattern to match source annotations";
    patternInput.placeholder = "Source Pattern";
    patternInput.style.width = "25%";
    patternInput.style.textAlign = "center";
    patternInput.onfocus = function() { lastMapping = this.parentNode; };
    patternInput.onkeyup = function() { validateRegularExpression(patternInput); };
    
    var labelInput = document.createElement("input");
    labelInput.type = "text";
    labelInput.dataset.role = "label";
    labelInput.title = "You can type text or select a layer to copy from."
	+"\nLeaving this blank saves no representation, but"
	+" prevents the patterns below from matching.";
    labelInput.placeholder = "Destination Label";
    labelInput.value = label;
    labelInput.style.width = "25%";
    labelInput.style.textAlign = "center";
    labelInput.onfocus = function() { lastMapping = this.parentNode; };

    // TODO <c:if test="${!spanningLayer}">
    var COPY_FROM_LAYER_TEXT = "Copy from layer: ";
    var copyFromLayer = document.createElement("select");
    copyFromLayer.className = "label-processing";
    copyFromLayer.title = "Label for annotation can be copied from another layer, or some specified text.";
    copyFromLayer.onfocus = function() { lastMapping = this.parentNode; };
    var option = document.createElement("option");
    option.appendChild(document.createTextNode("Specific text"));
    option.value = "";
    copyFromLayer.appendChild(option);
    var copying = false;
    
    for (var layerId in schema.layers) {
        var layer = schema.layers[layerId];
        if (layer.parentId == schema.wordLayerId && layer.alignment == 0) { // word tag
            option = document.createElement("option");
            option.value = COPY_FROM_LAYER_TEXT + layer.id;
            option.appendChild(document.createTextNode(COPY_FROM_LAYER_TEXT + layer.description));
            if (label == COPY_FROM_LAYER_TEXT + layer.id) {
                option.selected = true;
                copying = true;
            }
            copyFromLayer.appendChild(option);
        } // permitted layer
    } // next layer

    copyFromLayer.labelInput = labelInput;
    copyFromLayer.onchange = function() {
        this.labelInput.style.display = this.selectedIndex > 0?"none":"";
    };
    if (copying) {
        labelInput.style.display = "none";
    }
    
    var arrow = document.createElement("span");
    arrow.innerHTML = " → ";
    
    divMapping.appendChild(patternInput);
    divMapping.patternInput = patternInput;
    divMapping.appendChild(arrow);
    // TODO <c:if test="${!spanningLayer}">:
    divMapping.appendChild(copyFromLayer);    
    divMapping.appendChild(labelInput);
    divMapping.labelInput = labelInput;

    document.getElementById("mappings").appendChild(divMapping);
    patternInput.focus();
    
    enableRemoveButton();
    
    return false; // so form doesn't submit
}

function enableRemoveButton() {
    document.getElementById("removeButton").disabled = 
        document.getElementById("mappings").childElementCount <= 1;
}

function removeMapping() {
    if (lastMapping) { 
        document.getElementById("mappings").removeChild(lastMapping);
        lastMapping = null;
        enableRemoveButton();
    }
    return false; // so form doesn't submit
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
    return false; // so form doesn't submit
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
    return false; // so form doesn't submit
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

function setTaskParameters(form) {

    // we use the convertFormBodyToJSON from util.js to send the form as JSON, but we want to
    // to add the mappings as an array of objects, so we add them to the parameters 
    // (convertFormBodyToJSON will take care of the rest of the form inputs)
    var parameters = {
        mappings: []
    };
    var mappingDivs = document.getElementById("mappings").children;
    var mappings = [];
    for (var m = 0; m < mappingDivs.length; m++) {
        var div = mappingDivs[m];
        parameters.mappings.push({
            pattern: div.patternInput.value,
            label: div.labelInput.value
        });
    }
    
    return convertFormBodyToJSON(form, parameters);
}

// add event handlers
document.getElementById("addButton").onclick = e=>newMapping('','');
document.getElementById("upButton").onclick = e=>moveMappingUp();
document.getElementById("downButton").onclick = e=>moveMappingDown();
document.getElementById("removeButton").onclick = e=>removeMapping();
document.getElementById("destinationLayerId").onchange = function(e) { changedLayer(this); };
document.getElementById("form").onsubmit = function(e) { setTaskParameters(this); };

