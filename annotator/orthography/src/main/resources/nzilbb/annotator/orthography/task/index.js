// show spinner
startLoading();

// show annotator version
getText("getVersion", version => {
    document.getElementById("version").innerHTML = version;
});

var taskId = window.location.search.substring(1);

// first, get the layer schema
var schema = null;
getSchema(s => {
    schema = s;
    
    // populate layer input select options...          
    var tokenLayerId = document.getElementById("tokenLayerId");
    addLayerOptions(
        tokenLayerId, schema,
        // this is a function that takes a layer and returns true for the ones we want
        layer => layer.id == schema.wordLayerId
            || (layer.parentId == schema.wordLayerId && layer.alignment == 0));
    // default value:
    tokenLayerId.value = schema.wordLayerId;

    // populate layer output select options...          
    var orthographyLayerId = document.getElementById("orthographyLayerId");
    addLayerOptions(
        orthographyLayerId, schema,
        layer => layer.parentId == schema.wordLayerId && layer.alignment == 0);
    if (schema.layers["orthography"]) {
        orthographyLayerId.value = "orthography";
    } else {
        orthographyLayerId.selectedIndex = 0;
    }

    var lowerCase = document.getElementById("lowerCase");
    var exactMatch = document.getElementById("exactMatch");
    
    // GET request to getTaskParameters retrieves the current task parameters, if any
    getJSON("getTaskParameters", parameters => {
        try {
            if (parameters == null) { // no parameters
                // create default replacements
                newReplacement("\\s",""); // collapse all space (there could be space because of appended non-words)
                newReplacement("’","'"); // 'smart' apostrophes to normal ones
                newReplacement("[“”]","\""); // 'smart' quotes to normal ones
                newReplacement("—","-"); // 'em-dash' to hyphen
                newReplacement("[\\p{Punct}&&[^-~']]",""); // remove all punctuation except ~, -, and '
                newReplacement("^[-']+",""); // remove leading hyphens/apostrophes
                newReplacement("[-']+$",""); // remove trailing hyphens/apostrophes
                
                lowerCase.checked = true;
                exactMatch.checked = true;
                
                if (schema.layers[taskId]) { // there's a layer named after the task
                    // select it
                    orthographyLayerId.value = taskId;
                } else if (/.+:.+/.test(taskId)) { // might be an 'auxiliary'?
                    var layerId = taskId.replace(/:.+/,"");
                    if (schema.layers[layerId]) { // there's a layer named after the task
                        // select it
                        orthographyLayerId.value = layerId;
                    }
                }
                // if there's no option for the output layer, add one
                if (orthographyLayerId.value != taskId) {
                    var layerOption = document.createElement("option");
                    layerOption.appendChild(document.createTextNode(taskId));
                    orthographyLayerId.appendChild(layerOption);
                    orthographyLayerId.value = taskId;
                }
            } else {

                // set initial values of properties in the form
                tokenLayerId.value = parameters.tokenLayerId;
                lowerCase.checked = parameters.lowerCase;
                exactMatch.checked = parameters.exactMatch;
                orthographyLayerId.value = parameters.orthographyLayerId;
                for (var replacement in parameters.replacements) {
                    newReplacement(replacement, parameters.replacements[replacement]);
                } // next mapping
                // if there's no option for the output layer, add one
                if (orthographyLayerId.value != parameters.orthographyLayerId) {
                    var layerOption = document.createElement("option");
                    layerOption.appendChild(document.createTextNode(parameters.orthographyLayerId));
                    orthographyLayerId.appendChild(layerOption);
                    orthographyLayerId.value = parameters.orthographyLayerId;
                }
            }            
        } finally {
            // hide spinner
            finishedLoading();
        }        
    });
});

// this function detects when the user selects [add new layer]:
function changedLayer(select) {
    if (select.value == "[add new layer]") {
        var newLayer = prompt("Please enter the new layer ID", taskId);
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

var lastReplacement = null;

// Manage replacements

function newReplacement(pattern, label) {
    
    var divReplacement = document.createElement("div");
    
    var patternInput = document.createElement("input");
    patternInput.type = "text";
    patternInput.dataset.role = "pattern";
    patternInput.value = pattern;
    patternInput.title = "Regular-expression pattern to replace";
    patternInput.placeholder = "Pattern";
    patternInput.style.width = "25%";
    patternInput.style.textAlign = "center";
    patternInput.onfocus = function() { lastReplacement = this.parentNode; };
    patternInput.onkeyup = function() { validateRegularExpression(patternInput); };
    
    var labelInput = document.createElement("input");
    labelInput.type = "text";
    labelInput.dataset.role = "label";
    labelInput.title = "What to replace the pattern with."
	+"\nLeaving this blank removes the pattern from the label.";
    labelInput.placeholder = "Replace with nothing";
    labelInput.value = label;
    labelInput.style.width = "25%";
    labelInput.style.textAlign = "center";
    labelInput.onfocus = function() { lastReplacement = this.parentNode; };
    
    var arrow = document.createElement("span");
    arrow.innerHTML = " → ";
    
    divReplacement.appendChild(patternInput);
    divReplacement.patternInput = patternInput;
    divReplacement.appendChild(arrow);
    divReplacement.appendChild(labelInput);
    divReplacement.labelInput = labelInput;

    document.getElementById("replacements").appendChild(divReplacement);
    patternInput.focus();
    
    enableRemoveButton();
    
    return false; // so form doesn't submit
}

function enableRemoveButton() {
    document.getElementById("removeButton").disabled = 
        document.getElementById("replacements").childElementCount <= 1;
}

function removeReplacement() {
    if (lastReplacement) { 
        document.getElementById("replacements").removeChild(lastReplacement);
        lastReplacement = null;
        enableRemoveButton();
    }
    return false; // so form doesn't submit
}

function moveReplacementUp() {
    if (lastReplacement) { 
        var replacements = document.getElementById("replacements");
        var previousReplacement = lastReplacement.previousSibling;
        if (previousReplacement) {
            replacements.removeChild(lastReplacement);
            replacements.insertBefore(lastReplacement, previousReplacement);
        }
    }
    return false; // so form doesn't submit
}

function moveReplacementDown() {
    if (lastReplacement) { 
        var replacements = document.getElementById("replacements");
        var nextReplacement = lastReplacement.nextSibling;
        if (nextReplacement) {
            var nextNextReplacement = nextReplacement.nextSibling;
            replacements.removeChild(lastReplacement);
            if (nextNextReplacement) {
                replacements.insertBefore(lastReplacement, nextNextReplacement);
            } else {
                replacements.appendChild(lastReplacement);
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
    // to add the replacements as an array of objects, so we add them to the parameters 
    // (convertFormBodyToJSON will take care of the rest of the form inputs)
    var parameters = {
        replacements: {}
    };
    var replacementDivs = document.getElementById("replacements").children;
    for (var m = 0; m < replacementDivs.length; m++) {
        var div = replacementDivs[m];
        parameters.replacements[div.patternInput.value] = div.labelInput.value;
    }
    
    return convertFormBodyToJSON(form, parameters);
}

// add event handlers
document.getElementById("addButton").onclick = e=>newReplacement('','');
document.getElementById("upButton").onclick = e=>moveReplacementUp();
document.getElementById("downButton").onclick = e=>moveReplacementDown();
document.getElementById("removeButton").onclick = e=>removeReplacement();
document.getElementById("orthographyLayerId").onchange = function(e) { changedLayer(this); };
document.getElementById("form").onsubmit = function(e) { setTaskParameters(this); };


