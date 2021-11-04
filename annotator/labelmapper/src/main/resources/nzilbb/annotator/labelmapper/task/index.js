// show spinner
startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

// first, get the layer schema
var schema = null;
getSchema(s => {
    schema = s;
    
    // populate layer input select options...          
    var labelLayerId = document.getElementById("labelLayerId");
    addLayerOptions(
        labelLayerId, schema,
        // this is a function that takes a layer and returns true for the ones we want
        layer => layer.id == schema.wordLayerId || layer.parentId == schema.wordLayerId);
    var mappingLayerId = document.getElementById("mappingLayerId");
    labelLayerId.selectedIndex = 0; // force selection
    addLayerOptions(
        mappingLayerId, schema,
        // word layers
        layer => layer.id == schema.wordLayerId || layer.parentId == schema.wordLayerId
        // or segment layers
            || (schema.layers[layer.parentId]
                && schema.layers[layer.parentId].parentId == schema.wordLayerId));
    mappingLayerId.selectedIndex = 0; // force selection
    var tokenLayerId = document.getElementById("tokenLayerId");
    addLayerOptions(
        tokenLayerId, schema,
        // this is a function that takes a layer and returns true for the ones we want
        layer => layer.id == schema.wordLayerId || layer.parentId == schema.wordLayerId);
    tokenLayerId.selectedIndex = 0; // force selection
    
    // GET request to getTaskParameters retrieves the current task parameters, if any
    getText("getTaskParameters", text => {
        try {
            var parameters = new URLSearchParams(text);
            
            // set initial values of properties in the form above
            // (this assumes bean property names match input id's in the form above)
            for (const [key, value] of parameters) {
                try {
                    document.getElementById(key).value = value;
                } catch(x) {
                }
            }

            // // set splitLabels value
            // try {
            //     document.getElementById("splitLabels-"+parameters.splitLabels).checked = true;
            // } catch( x) {
            //     console.log(`Invalid splitLabels value: "${parameters.splitLabels}"`);
            // }
            
            setComparatorExamples(document.getElementById("comparator"));
        } finally {
            // hide spinner
            finishedLoading();
        }
    });
});

function changedLabelLayer(select) {
    defaultComparator();
    // TODO filter possible token layers
}
function changedTokenLayer(select) {
    defaultComparator();
}
function defaultComparator() {
    var labelLayerId = document.getElementById("labelLayerId");
    var tokenLayerId = document.getElementById("tokenLayerId");
    var labelLayer = schema.layers[labelLayerId.value];
    var tokenLayer = schema.layers[tokenLayerId.value];
    var defaultComparator = "OrthographyToArpabet";
    if (labelLayer && tokenLayer) {
        if (labelLayer.type == "ipa" && tokenLayer.type == "ipa") {
            defaultComparator = "DISCToDISC";
        } else if (labelLayer.type == "string" && tokenLayer.type == "ipa") {
            defaultComparator = "OrthographyToDISC";
        } else if (labelLayer.type == "string" && tokenLayer.type == "ipa") {
            defaultComparator = "OrthographyToDISC";
        }
    }
    document.getElementById("comparator").value = defaultComparator;
}
// this function detects when the user selects [add new layer]:
function changedMappingLayer(select) {
    if (select.value == "[add new layer]") {
        var newLayer = prompt("Please enter the new layer ID", window.location.search.substring(1));
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


// this function detects when the user changes the comparator, and shows an example:
function setComparatorExamples(select) {
    // CharacterToCharacter by default
    var exampleLabel = "transcription"; // orthography
    var exampleMapping = "? ? ? ? ? ? ? ? ? ? ? ? ?"; // orthography
    var exampleToken   = "t r a n s c r i p t i o n"; // orthography
    switch (select.value) {
    case "OrthographyToDISC":
        exampleLabel = "transcription"; // orthography
        exampleMapping = "? ? ? ? ? ? ? ? ? ? ? ?"; // orthography
        exampleToken   = "t r { n s k r I p S V n"; // DISC
        break;
    case "OrthographyToArpabet":
        exampleLabel = "transcription"; // orthography
        exampleMapping = "? ?  ?  ? ? ? ?  ?  ? ?   ?  ?"; // orthography
        exampleToken   = "T R AE2 N S K R IH1 P SH AH0 N"; // ARPAbet
        break;
    case "DISCToDISC":
        exampleLabel = "tr{nskrIpSVn"; // DISC
        exampleMapping = "? ? ? ? ? ? ? ? ? ? ? ?"; // orthography
        exampleToken   = "t r { n s k r I p S V n"; // DISC
        break;
    }
    document.getElementById("exampleLabelLabel").innerHTML = exampleLabel;
    document.getElementById("exampleMappingLabel").innerHTML = exampleMapping;
    document.getElementById("exampleMapping").innerHTML = exampleMapping.replace(/\?/g,"↓");
    document.getElementById("exampleTokenLabel").innerHTML = exampleToken;
}

// add event handlers
document.getElementById("labelLayerId").onchange = function(e) {
    return changedLabelLayer(this); };
document.getElementById("mappingLayerId").onchange = function(e) {
    return changedMappingLayer(this); };
document.getElementById("tokenLayerId").onchange = function(e) {
    return changedTokenLayer(this); };
document.getElementById("comparator").onchange = function(e) {
    return setComparatorExamples(this); };
