// show spinner
startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

var taskId = window.location.search.substring(1);
console.log("taskId " + taskId);

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
    if (schema.layers["orthography"]) {
        tokenLayerId.value = "orthography";
    } else {
        tokenLayerId.value = schema.wordLayerId;
    }

    // populate the language layer...    
    var languagesLayerId = document.getElementById("languagesLayerId");
    addLayerOptions(
        languagesLayerId, schema,
        layer => layer.parentId == schema.root.id && layer.alignment == 0
            && /.*lang.*/.test(layer.id));
    // select the first one by default
    languagesLayerId.selectedIndex = 1;
    
    // populate layer output select options...          
    var morLayerId = document.getElementById("morLayerId");
    addLayerOptions(
        morLayerId, schema,
        layer => layer.parentId == schema.wordLayerId && layer.alignment == 2);
    morLayerId.selectedIndex = 0;

    var layerId = document.getElementById("prefixLayerId");
    addLayerOptions(
        layerId, schema,
        layer => layer.parentId == schema.wordLayerId && layer.alignment == 2);
    layerId.selectedIndex = 0;

    layerId = document.getElementById("partOfSpeechLayerId");
    addLayerOptions(
        layerId, schema,
        layer => layer.parentId == schema.wordLayerId && layer.alignment == 2);
    layerId.selectedIndex = 0;

    layerId = document.getElementById("partOfSpeechSubcategoryLayerId");
    addLayerOptions(
        layerId, schema,
        layer => layer.parentId == schema.wordLayerId && layer.alignment == 2);
    layerId.selectedIndex = 0;

    layerId = document.getElementById("stemLayerId");
    addLayerOptions(
        layerId, schema,
        layer => layer.parentId == schema.wordLayerId && layer.alignment == 2);
    layerId.selectedIndex = 0;

    layerId = document.getElementById("fusionalSuffixLayerId");
    addLayerOptions(
        layerId, schema,
        layer => layer.parentId == schema.wordLayerId && layer.alignment == 2);
    layerId.selectedIndex = 0;

    layerId = document.getElementById("suffixLayerId");
    addLayerOptions(
        layerId, schema,
        layer => layer.parentId == schema.wordLayerId && layer.alignment == 2);
    layerId.selectedIndex = 0;

    layerId = document.getElementById("glossLayerId");
    addLayerOptions(
        layerId, schema,
        layer => layer.parentId == schema.wordLayerId && layer.alignment == 2);
    layerId.selectedIndex = 0;

    // GET request to getTaskParameters retrieves the current task parameters, if any
    getText("getTaskParameters", text => {
        try {
            if (!text) { // default config 
                text = "splitMorTagGroups=true&splitMorWordGroups=true&morLayerId=mor";
            }
            var parameters = new URLSearchParams("?"+text);
            
            // set initial values of properties in the form above
            // (this assumes bean property names match input id's in the form above)
            for (const [key, value] of parameters) {
                var control = document.getElementById(key);
                if (control) {
                    if (/.*LayerId$/.test(key)) {
                        control.value = value;
                        
                        // if there's a layer defined
                        if (value
                            // but it's not in the schema
                            && !schema.layers[value]) {
                            
                            // add it to the list
                            var select = document.getElementById(key);
                            var layerOption = document.createElement("option");
                            layerOption.appendChild(document.createTextNode(value));
                            select.appendChild(layerOption);
                            // select it
                            select.selectedIndex = select.children.length - 1;
                        }
                    } else if (key == "splitMorTagGroups" || key == "splitMorWordGroups") {
                        if (value == "true") {
                            control.checked = true;
                        }
                    }
                } // setting has a control
            } // next parameter
            
            
            // if there's no mor layer defined
            if (morLayerId.selectedIndex == 0
                // but there's a layer named after the task
                && schema.layers[taskId]) {
                
                // select that layer by default
                morLayerId.value = taskId;
            }
            
            enableLayers();
        } finally {
            // hide spinner
            finishedLoading();
        }
    });
});

// this function detects when the user selects [add new layer]:
function changedLayer(select) {
    if (select.value == "[add new layer]") {
        var newLayer = prompt("Please enter the new layer ID", select.dataset.defaultLayer);
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
        } else {
            select.selectedIndex = 0;
        }
    }
}

function enableLayers() {
    var splitMorTagGroups = document.getElementById("splitMorTagGroups");
    var splitMorWordGroups = document.getElementById("splitMorWordGroups");
    var prefixLayerId = document.getElementById("prefixLayerId");
    var partOfSpeechLayerId = document.getElementById("partOfSpeechLayerId");
    var partOfSpeechSubcategoryLayerId = document.getElementById("partOfSpeechSubcategoryLayerId");
    var stemLayerId = document.getElementById("stemLayerId");
    var fusionalSuffixLayerId = document.getElementById("fusionalSuffixLayerId");
    var suffixLayerId = document.getElementById("suffixLayerId");
    var glossLayerId = document.getElementById("glossLayerId");

    if (!splitMorTagGroups.checked) {
        splitMorWordGroups.checked = false;
        splitMorWordGroups.disabled = true;
    } else {
        splitMorWordGroups.disabled = false;
    }

    prefixLayerId.disabled
        = partOfSpeechLayerId.disabled
        = partOfSpeechSubcategoryLayerId.disabled
        = stemLayerId.disabled
        = fusionalSuffixLayerId.disabled
        = suffixLayerId.disabled
        = glossLayerId.disabled
        = !(splitMorTagGroups.checked && splitMorWordGroups.checked);
}
