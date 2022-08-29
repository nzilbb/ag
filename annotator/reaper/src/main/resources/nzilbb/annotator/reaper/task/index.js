startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

const taskId = window.location.search.substring(1);

// get the layer schema
let schema = null;
getSchema(s => {
    schema = s;
    
    // populate output layer select options

    const f0LayerId = document.getElementById("f0LayerId");
    addLayerOptions(
        f0LayerId, schema,
        // instantaneous span lauers
        layer => layer.parentId == schema.root.id && layer.alignment == 1);

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
                document.getElementById("hilbertTransform").checked
                    = parameters.get("hilbertTransform");
                document.getElementById("suppressHighPassFilter").checked
                    = parameters.get("suppressHighPassFilter");
            }
            // if there's a layer named after the task
            if (schema.layers[taskId]) {
                
                // select that layer by default
                f0LayerId.value = taskId;
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

// add event handler
document.getElementById("f0LayerId").onchange = function(e) {
    changedLayer(this, taskId || f0); };
