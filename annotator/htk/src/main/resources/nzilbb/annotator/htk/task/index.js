startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

const taskId = window.location.search.substring(1);

// first, get the layer schema
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
        for (let layer in schema.layers) {
            if (layer.parentId == schema.wordLayerId
                && layer.alignment == 0
                && layer.type == "ipa"
                && layer.id != "pronounce") { // not the manual pronunciation tag layer
                pronunciationLayerId.value = layer.id;
                break;
            }
        } // next layer
    }
    
    const noiseLayerId = document.getElementById("noiseLayerId");
    addLayerOptions(
        noiseLayerId, schema,
        // top-level aligned layers
        layer => layer.parentId == "transcript" && layer.alignment != 0);
    // default value:
    if (schema.layers["noise"]) {
        noiseLayerId.value = "noise";
    }
              
    // populate output layer select options

    //TODO const wordAlignmentLayerId = document.getElementById("wordAlignmentLayerId");
    // addLayerOptions(
    //     wordAlignmentLayerId, schema,
    //     // word layer, or aligned turn children (TODO)
    //     layer => layer.id == schema.wordLayerId/*
    //         || (layer.parentId == schema.turnLayerId && layer.alignment == 2)*/);
    // // default value:
    // wordAlignmentLayerId.value = schema.wordLayerId;
    
    const phoneAlignmentLayerId = document.getElementById("phoneAlignmentLayerId");
    addLayerOptions(
        phoneAlignmentLayerId, schema,
        // segment layer or aligned turn children (TODO)
        layer => layer.id == "segment" || layer.id == "phone"/*
            || (layer.parentId == schema.turnLayerId && layer.alignment == 2)*/);
    // default value:
    if (schema.layers["segment"]) {
        phoneAlignmentLayerId.value = "segment";
    } else if (schema.layers["phone"]) {
        phoneAlignmentLayerId.value = "phone";
    }
    
    const scoreLayerId = document.getElementById("scoreLayerId");
    addLayerOptions(
        phoneAlignmentLayerId, schema,
        // segment children, or aligned turn children (TODO)
        layer => layer.parentId == "segment"
            || layer.parentId == "phone"/*
            || (layer.parentId == schema.turnLayerId && layer.alignment == 2)*/);
    
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
                getText("getPauseMarkers", value => {
                    document.getElementById("pauseMarkers").value = value;
                });
                getText("getNoisePatterns", value => {
                    document.getElementById("noisePatterns").value = value;
                });
                getText("getOverlapThreshold", value => {
                    document.getElementById("overlapThreshold").value = value;
                });
                getText("getCleanupOption", value => {
                    document.getElementById("cleanupOption").value = value;
                });
            } else {
                const parameters = new URLSearchParams(parameters);
                
                // set initial values of properties in the form above
                // (this assumes bean property names match input id's in the form above)
                for (const [key, value] of parameters) {
                    document.getElementById(key).value = value;
                }
                // set the checkboxes
                document.getElementById("ignoreAlignmentStatuses").checked
                    = parameters.get("ignoreAlignmentStatuses");
                document.getElementById("useP2FA").checked
                    = parameters.get("useP2FA");
                document.getElementById("sampleRate").checked
                    = parameters.get("sampleRate");
                // if P2FA models are used, 11025Hz must be used
                if (document.getElementById("useP2FA").checked) {
                    document.getElementById("sampleRate").disabled
                        = document.getElementById("sampleRate").checked
                    // and can't split participants by channel
                        = document.getElementById("leftPattern").disabled
                        = document.getElementById("rightPattern").disabled
                    // and no need for grouping of utterances
                        = document.getElementById("mainUtteranceGrouping").disabled
                        = document.getElementById("otherUtteranceGrouping").disabled
                        = true;
                }
            }
            // if there's no utterance tag layer defined
            if (utteranceTagLayerId.selectedIndex == 0
                // but there's a layer named after the task
                && schema.layers[taskId]) {
                
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
//TODO document.getElementById("wordAlignmentLayerId").onchange = function(e) { changedLayer(this, taskId + "_word"); };
//TODO document.getElementById("phoneAlignmentLayerId").onchange = function(e) { changedLayer(this, taskId + "_phone"); };
//TODO document.getElementById("utteranceTagLayerId").onchange = function(e) { changedLayer(this, taskId + "_time"); };
//TODO document.getElementById("participantTagLayerId").onchange = function(e) { changedLayer(this, "participant_" + taskId + "_time"); };
document.getElementById("scoreLayerId").onchange = function(e) { changedLayer(this, "score"); };

document.getElementById("useP2FA").onchange = function(e) {
    // if P2FA models are used, 11025Hz must be used
    document.getElementById("sampleRate").disabled
    // and can't split participants by channel
        = document.getElementById("leftPattern").disabled
        = document.getElementById("rightPattern").disabled
        = document.getElementById("useP2FA").checked;
    document.getElementById("sampleRate").checked
        = document.getElementById("sampleRate").disabled;
    
    // also with P2FA, there's no training, so no grouping by participant/tramscript
    document.getElementById("mainUtteranceGrouping").disabled
        = document.getElementById("otherUtteranceGrouping").disabled
        = document.getElementById("useP2FA").checked;
}
