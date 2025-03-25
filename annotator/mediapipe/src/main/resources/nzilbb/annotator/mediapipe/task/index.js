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
  
  // set sensible defaults for optionsby asking the annotator
  getText("getNumFaces", value =>
    document.getElementById("numFaces").value = value);
  getText("getMinFaceDetectionConfidence", value =>
    document.getElementById("minFaceDetectionConfidence").value = value);
  getText("getMinFacePresenceConfidence", value =>
    document.getElementById("minFacePresenceConfidence").value = value);
  getText("getMinTrackingConfidence", value =>
    document.getElementById("minTrackingConfidence").value = value);

  // get blendshape category list - categories can be mapped to layers
  getJSON("getBlendshapeCategories", categories => {
    const layerMappings = document.getElementById("layerMappings");
    for (category of categories) {
      var field = document.createElement("div");
      field.className = "field";
      field.title = `Layer for ${category}`;
      var label = document.createElement("label");
      label.for = category;
      label.appendChild(document.createTextNode(category));
      field.appendChild(label)
      var span = document.createElement("span");
      var select = document.createElement("select");
      select.id = category;
      select.name = category;
      var none = document.createElement("option");
      none.value = "";
      none.appendChild(document.createTextNode("[none]"));
      select.appendChild(none);
      var namesake = document.createElement("option");
      namesake.value = category;
      namesake.appendChild(document.createTextNode(category));
      select.appendChild(namesake);
      span.appendChild(select);
      field.appendChild(span);
      layerMappings.appendChild(field);
      
      // populate layer input select options...
      addLayerOptions(
        select, schema,
        // instantaneous top level layers
        layer => layer.alignment == 1 && layer.parentId == schema.root.id
      );

      var addNew = document.createElement("option");
      addNew.appendChild(document.createTextNode("[add new layer]"));
      select.appendChild(addNew);

      select.onchange = function(e) {
        changedLayer(this, this.id); };
      
    } // next name
    
    // GET request to getTaskParameters retrieves the current task parameters, if any
    getText("getTaskParameters", parameters => {
      try {
        if (!parameters) { // new task
          // default for score layers is to create them all
          for (category of categories) {
            document.getElementById(category).value = category;
          } // next category
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
        } // there are parameters
      } finally {
        finishedLoading();
      }
    }); // getTaskParameters
  }); // getBlendshapeCategories
}); // getSchema
                
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
