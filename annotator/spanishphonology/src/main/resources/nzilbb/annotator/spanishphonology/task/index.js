// show spinner
startLoading();

// show annotator version
getVersion(version => {
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
    if (schema.layers["orthography"]) {
        tokenLayerId.value = "orthography";
    } else {
        tokenLayerId.value = schema.wordLayerId;
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
    
    // populate layer output select options...          
    var phonemeLayerId = document.getElementById("phonemeLayerId");
    addLayerOptions(
        phonemeLayerId, schema,
        layer => layer.parentId == schema.wordLayerId && layer.alignment == 0);
    phonemeLayerId.selectedIndex = 0;
    
    // GET request to getTaskParameters retrieves the current task parameters, if any
    getText("getTaskParameters", text => {
        try {
            var parameters = new URLSearchParams(text);
            
            // set initial values of properties in the form above
            // (this assumes bean property names match input id's in the form above)
            for (const [key, value] of parameters) {
                document.getElementById(key).value = value;
                
                if (key == "phonemeLayerId") {
                    // if there's a pos layer defined
                    if (value
                        // but it's not in the schema
                        && !schema.layers[value]) {
                        
                        // add it to the list
                        var select = document.getElementById("phonemeLayerId");
                        var layerOption = document.createElement("option");
                        layerOption.appendChild(document.createTextNode(value));
                        select.appendChild(layerOption);
                        // select it
                        select.selectedIndex = select.children.length - 1;
                    }
                } // phonemeLayerId
            } // next parameter
            
            // if there's no phoneme layer defined
            if (phonemeLayerId.selectedIndex == 0) {
                // but there's a layer named after the task
                if (schema.layers[taskId]) {            
                    // select that layer by default
                    phonemeLayerId.value = taskId;
                } else if (schema.layers["phonemes"]) {
                    phonemeLayerId.value = "phonemes";
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
        var newLayer = prompt("Please enter the new layer ID", "phonemes");
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
