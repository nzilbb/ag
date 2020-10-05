// show annotator version
getText("getVersion", function(e) {
    document.getElementById("version").innerHTML = this.responseText;
});

// first, get the layer schema
var schema = null;
getJSON("getSchema", function(e) {
    schema = JSON.parse(this.responseText);
    
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
    if (schema.layers["phonemes"]) {
        phonemeLayerId.value = "phonemes";
    } else {
        phonemeLayerId.selectedIndex = 0;
    }
    
    // GET request to getTaskParameters retrieves the current task parameters, if any
    getText( "getTaskParameters"+window.location.search, function(e) {
        var parameters = new URLSearchParams(this.responseText);
        
        // set initial values of properties in the form above
        // (this assumes bean property names match input id's in the form above)
        for (const [key, value] of parameters) {
            document.getElementById(key).value = value;
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

