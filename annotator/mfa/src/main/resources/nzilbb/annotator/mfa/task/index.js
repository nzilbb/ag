startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

const taskId = window.location.search.substring(1);
let existingPhoneLayerId = null;

// populate list of dictionary names
getJSON("validDictionaryNames", names => {
    const dictionaryName = document.getElementById("dictionaryName");
    for (name of names) {
        var layerOption = document.createElement("option");
        layerOption.appendChild(document.createTextNode(name));
        dictionaryName.appendChild(layerOption);
    } // next name
});

// populate list of pretrained models names
getJSON("validAcousticModels", names => {
    const modelsName = document.getElementById("modelsName");
    for (name of names) {
        var layerOption = document.createElement("option");
        layerOption.appendChild(document.createTextNode(name));
        modelsName.appendChild(layerOption);
    } // next name
});

// get the layer schema
let schema = null;
getSchema(s => {
    schema = s;
    
    // populate layer input select options...
    
    const orthographyLayerId = document.getElementById("orthographyLayerId");
    addLayerOptions(
        orthographyLayerId, schema,
        // the word layer, or word tag layers
        layer => layer.id == schema.wordLayerId
            || (layer.parentId == schema.wordLayerId && layer.alignment == 0));
    // default value:
    if (schema.layers["orthography"]) {
        orthographyLayerId.value = "orthography";
    } else {
        orthographyLayerId.value = schema.wordLayerId;
    }
    
    const pronunciationLayerId = document.getElementById("pronunciationLayerId");
    addLayerOptions(
        pronunciationLayerId, schema,
        // the word layer, or word tag layers
        layer => layer.parentId == schema.wordLayerId && layer.alignment == 0);
    // default value:
    if (schema.layers["phonemes"]) {
        pronunciationLayerId.value = "phonemes";
    } else if (schema.layers["phonology"]) {
        pronunciationLayerId.value = "phonology";
    } else {
        // pick the first phonology-type layer
        for (let l in schema.layers) {
            const layer = schema.layers[l];
            if (layer.parentId == schema.wordLayerId
                && layer.alignment == 0
                && layer.type == "ipa"
                && layer.id != "pronounce") { // not the manual pronunciation tag layer
                pronunciationLayerId.value = layer.id;
                break;
            }
        } // next layer
    }
    
    // populate output layer select options

    const wordAlignmentLayerId = document.getElementById("wordAlignmentLayerId");
    addLayerOptions(
        wordAlignmentLayerId, schema,
        // word layer, or aligned turn children
        layer => layer.id == schema.wordLayerId
            || (layer.parentId == schema.turnLayerId && layer.alignment == 2
                && layer.id != schema.utteranceLayerId));
    // default value:
    wordAlignmentLayerId.value = schema.wordLayerId;
    
    const phoneAlignmentLayerId = document.getElementById("phoneAlignmentLayerId");
    addLayerOptions(
        phoneAlignmentLayerId, schema,
        // segment layer or aligned turn children
        layer => layer.id == "segment" || layer.id == "phone"
            || (layer.parentId == schema.turnLayerId && layer.alignment == 2
                && layer.id != schema.utteranceLayerId
                && layer.id != schema.wordLayerId));
    // default value:
    if (schema.layers["segment"]) {
        phoneAlignmentLayerId.value = "segment";
        existingPhoneLayerId = "segment";
    } else if (schema.layers["phone"]) {
        phoneAlignmentLayerId.value = "phone";
        existingPhoneLayerId = "phone";
    }
    
    const utteranceTagLayerId = document.getElementById("utteranceTagLayerId");
    addLayerOptions(
        utteranceTagLayerId, schema,
        // aligned turn children
        layer => layer.parentId == schema.turnLayerId && layer.alignment == 2
        // but not word nor utterance
            && layer.id != schema.wordLayerId && layer.id != schema.utteranceLayerId);
    
    // TODO const participantTagLayerId = document.getElementById("participantTagLayerId");
    // addLayerOptions(
    //     participantTagLayerId, schema,
    //     // participant attributes
    //     layer => layer.parentId == schema.participantLayerId && layer.alignment == 0);
    
    // GET request to getTaskParameters retrieves the current task parameters, if any
    getText("getTaskParameters", parameters => {
        try {
            if (!parameters) { // new task
                // set some sensible defaults
            } else {
                parameters = new URLSearchParams(parameters);
                
                // set initial values of properties in the form above
                // (this assumes bean property names match input id's in the form above)
                for (const [key, value] of parameters) {
                    const element = document.getElementById(key)
                    try {
                        element.value = value;
                    } catch (x) {}
                    if (element.value != value) { // layer that hasn't been created yet
                        try {
                            // add the layer to the list
                            var layerOption = document.createElement("option");
                            layerOption.appendChild(document.createTextNode(value));
                            element.appendChild(layerOption);
                            // select it
                            element.selectedIndex = element.children.length - 1;
                        } catch (x) {}
                    }
                }
                // set the checkboxes
                document.getElementById("ignoreAlignmentStatuses").checked
                    = parameters.get("ignoreAlignmentStatuses");
                // either pronunciationLayerId or dictionaryName
                document.getElementById("pronunciationLayerId").disabled
                    = document.getElementById("dictionaryName").value != ""; 
            }
            // if there's no utterance tag layer defined
            if (utteranceTagLayerId.selectedIndex == 0
                // but there's a layer named after the task
                && schema.layers[taskId]
                // and it's not being otherwise used
                && wordAlignmentLayerId.value != taskId && phoneAlignmentLayerId != null) {
                
                // select that layer by default
                utteranceTagLayerId.value = taskId;
            }
        } finally {
            finishedLoading();
        }
    });
});

// this function detects when the user selects [add new layer]:
function changedLayer(select, defaultNewLayerName) {
    if (select.value == "[add new layer]") {
        const newLayer = prompt("Please enter the new layer ID", defaultNewLayerName);
        if (newLayer) { // they didn't cancel
            // check there's not already a layer with that name
            for (let l in schema.layers) {
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

// add event handlers
document.getElementById("wordAlignmentLayerId").onchange = function(e) {
    changedLayer(this, taskId + "Word"); };
document.getElementById("phoneAlignmentLayerId").onchange = function(e) {
    changedLayer(this, taskId + "Phone"); };
document.getElementById("utteranceTagLayerId").onchange = function(e) {
    changedLayer(this, taskId + "Time"); };
//TODO document.getElementById("participantTagLayerId").onchange = function(e) { changedLayer(this, "participant_" + taskId + "_time"); };

document.getElementById("form").onsubmit = function(e) {
    const wordAlignmentLayerId = document.getElementById("wordAlignmentLayerId");
    const phoneAlignmentLayerId = document.getElementById("phoneAlignmentLayerId");
    try {
        // wordAlignmentLayerId and phoneAlignmentLayerId must be both system layers, or neither
        if (existingPhoneLayerId
            && (wordAlignmentLayerId.value == schema.wordLayerId)
            != (phoneAlignmentLayerId.value == existingPhoneLayerId)) {
            alert(`For Word/Phone Alignment Layers, either both ${schema.wordLayerId} and ${existingPhoneLayerId} must be selected, or neither.`);
            wordAlignmentLayerId.focus();
            return false;
        }
        // either a pronunciationLayerId or a dictionaryName must be specified
        if (!document.getElementById("pronunciationLayerId").value
            && !document.getElementById("dictionaryName").value) {
            alert("You must select a Pronunciation Layer, or enter a Dictionary Name.");
            document.getElementById("pronunciationLayerId").focus();
            return false;
        }
        // if a dictionary is selected, models must also be selected
        if (document.getElementById("dictionaryName").value
           && !document.getElementById("modelsName").value) {
            alert("If you have selected a Dictionary Name, you must also select Pretrained Acoustic Models.");
            document.getElementById("modelsName").focus();
            return false;
        }
        if (!document.getElementById("dictionaryName").value
           && document.getElementById("modelsName").value) {
            alert("If you have selected Pretrained Acoustic Models, you must also select a Dictionary Name.");
            document.getElementById("dictionaryName").focus();
            return false;
        }
        return true;
    } catch (x) {
        alert(x);
        return false;
    }
}

document.getElementById("dictionaryName").onchange = function(e) {
    // either pronunciationLayerId or dictionaryName
    document.getElementById("pronunciationLayerId").disabled
        = document.getElementById("dictionaryName").value != ""; 
}
