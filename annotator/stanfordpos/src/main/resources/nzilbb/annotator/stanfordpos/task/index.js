
// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

var model = "english-caseless-left3words-distsim.tagger"; // a good default

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
    
    var chunkLayerId = document.getElementById("chunkLayerId");
    addLayerOptions(
        chunkLayerId, schema,
        // this is a function that takes a layer and returns true for the ones we want
        layer => (layer.id == schema.turnLayerId || (layer.parentId == schema.turnLayerId))
            && layer.id != schema.wordLayerId);
    chunkLayerId.value = schema.turnLayerId;
    
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
    var posLayerId = document.getElementById("posLayerId");
    addLayerOptions(
        posLayerId, schema,
        layer => layer.parentId == schema.wordLayerId && layer.alignment == 0);
    posLayerId.selectedIndex = 0;

    // what models are available
    getJSON("availableModels", modelsOptions => {
        
        // if there are no models available, do nothing
        if (modelsOptions.length == 0) return;
        
        // if there's only one option, select it
        if (modelsOptions.length == 1) models = modelsOptions[0];
        
        // list options...
        selectModels = document.getElementById("models");
        // remove all current options
        selectModels.textContent = "";
        // populate the span with options
        for (m in modelsOptions) {
            var modelsOption = modelsOptions[m];
            var option = document.createElement("option");
            option.value=modelsOption
            if (modelsOption == models) {
                option.selected = true;
            }
            option.appendChild(document.createTextNode(modelsOption));
            selectModels.appendChild(option);
        } // next option

        // GET request to getTaskParameters retrieves the current task parameters, if any
        getText("getTaskParameters", text => {
            var parameters = new URLSearchParams("?"+text);
            
            // set initial values of properties in the form above
            // (this assumes bean property names match input id's in the form above)
            for (const [key, value] of parameters) {
                document.getElementById(key).value = value;
                
                if (key == "posLayerId") {
                    // if there's a pos layer defined
                    if (value
                        // but it's not in the schema
                        && !schema.layers[value]) {
                
                        // add it to the list
                        var select = document.getElementById("posLayerId");
                        var layerOption = document.createElement("option");
                        layerOption.appendChild(document.createTextNode(value));
                        select.appendChild(layerOption);
                        // select it
                        select.selectedIndex = select.children.length - 1;
                    }
                } // posLayerId
            } // next parameter

        });        
    })    
});

// this function detects when the user selects [add new layer]:
function changedLayer(select) {
    if (select.value == "[add new layer]") {
        var newLayer = prompt("Please enter the new layer ID", "pos");
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

