// show annotator version
getText("getVersion", function(e) {
    document.getElementById("version").innerHTML = this.responseText;
});

function testRemovalPattern() {
    var removalPattern = document.getElementById("removalPattern");

    var textOrthography = document.getElementById("textOrthography");
    try {
        
        // test regular expression is valid
        new RegExp(removalPattern.value, "g");
        // pattern is valid, so don't mark it as an error
        removalPattern.className = "";
        removalPattern.title = "";
        textOrthography.className = "";
        textOrthography.title = "";
        
        var textTranscript = document.getElementById("textTranscript");

        // show the test word transformation by using the annotator's orthography function        
        getText(
            resourceForFunction("orthography?", textTranscript.value, removalPattern.value),
            text => { textOrthography.value = text; });
        
    } catch(error) {
        // pattern is invalid, so don't mark it as an error
        removalPattern.className = "error";
        removalPattern.title = error;
        textOrthography.className = "error";
        textOrthography.value = error;
        textOrthography.title = error;
    }

}

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

    // default value for removalPattern
    var removalPattern = document.getElementById("removalPattern");
    removalPattern.value = "[\\p{Punct}&&[^~\\-:']]";
    
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
    
    // GET request to getTaskParameters retrieves the current task parameters, if any
    getText("getTaskParameters", function(e) {
        var parameters = new URLSearchParams(this.responseText);
        
        // set initial values of properties in the form above
        // (this assumes bean property names match input id's in the form above)
        for (const [key, value] of parameters) {
            document.getElementById(key).value = value;
        }
        
        testRemovalPattern();
    });
});

// this function detects when the user selects [add new layer]:
function changedLayer(select) {
    if (select.value == "[add new layer]") {
        var newLayer = prompt("Please enter the new layer ID", "orthography");
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

