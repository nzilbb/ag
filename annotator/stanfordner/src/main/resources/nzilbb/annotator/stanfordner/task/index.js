startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

var classifier = "english.all.3class.distsim.crf.ser.gz"; // a good default
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
    var entityLayerId = document.getElementById("entityLayerId");
    addLayerOptions(
        entityLayerId, schema,
      layer => layer.parentId == schema.wordLayerId
        && (layer.alignment == 0 // entity layers are aligned
            || layer.id == taskId)); // ...but they might have set up the alignment wrong
    entityLayerId.selectedIndex = 0;

    // what classifiers are available
    getJSON("availableClassifiers", classifierOptions => {
        
        if (classifierOptions.length > 0) {
            // if there's only one option, select it
            if (classifierOptions.length == 1) classifier = classifierOptions[0];
            
            // list options...
            selectClassifier = document.getElementById("classifier");
            // remove all current options
            selectClassifier.textContent = "";
            // populate the span with options
            for (m in classifierOptions) {
                var classifierOption = classifierOptions[m];
                var option = document.createElement("option");
                option.value=classifierOption
                if (classifierOption == classifier) {
                    option.selected = true;
                }
                option.appendChild(document.createTextNode(classifierOption));
                selectClassifier.appendChild(option);
            } // next option
        }

        // GET request to getTaskParameters retrieves the current task parameters, if any
        getText("getTaskParameters", text => {
            try {
                var parameters = new URLSearchParams("?"+text);
                
                // set initial values of properties in the form above
                // (this assumes bean property names match input id's in the form above)
                for (const [key, value] of parameters) {
                    document.getElementById(key).value = value;
                    
                    if (key == "entityLayerId") {
                        // if there's a entity layer defined
                        if (value
                            // but it's not in the schema
                            && !schema.layers[value]) {
                            
                            // add it to the list
                            var select = document.getElementById("entityLayerId");
                            var layerOption = document.createElement("option");
                            layerOption.appendChild(document.createTextNode(value));
                            select.appendChild(layerOption);
                            // select it
                            select.selectedIndex = select.children.length - 1;
                        }
                    } // entityLayerId
                } // next parameter
                
                // if there's no entity layer defined
                if (entityLayerId.selectedIndex == 0
                    // but there's a layer named after the task
                    && schema.layers[taskId]) {
                    
                    // select that layer by default
                    entityLayerId.value = taskId;
                }
            } finally {
                finishedLoading();
            }
        });        
    })    
});

// this function detects when the user selects [add new layer]:
function changedLayer(select) {
    if (select.value == "[add new layer]") {
        var newLayer = prompt("Please enter the new layer ID", "namedEntity");
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

function testPatterns(alertOnError) {
    var tokenExclusionPattern = document.getElementById("tokenExclusionPattern");
    try {
        
        // test regular expression is valid
        new RegExp(tokenExclusionPattern.value, "g");
        // pattern is valid, so don't mark it as an error
        tokenExclusionPattern.className = "";
        tokenExclusionPattern.removeAttribute("title");
        
    } catch(error) {
        // pattern is invalid, so don't mark it as an error
        tokenExclusionPattern.className = "error";
        tokenExclusionPattern.title = error;
        if (alertOnError) {
            alert("Invalid Token Exclusion Pattern - " + error);
            tokenExclusionPattern.focus();
        }
    }
    var targetLanguagePattern = document.getElementById("targetLanguagePattern");
    try {
        
        // test regular expression is valid
        new RegExp(targetLanguagePattern.value, "g");
        // pattern is valid, so don't mark it as an error
        targetLanguagePattern.className = "";
        targetLanguagePattern.removeAttribute("title");
        
    } catch(error) {
        // pattern is invalid, so don't mark it as an error
        targetLanguagePattern.className = "error";
        targetLanguagePattern.title = error;
        if (alertOnError) {
            alert("Invalid Target Language Pattern - " + error);
            targetLanguagePattern.focus();
        }
    }
  return tokenExclusionPattern.className == ""
    && targetLanguagePattern.className == "";
}
