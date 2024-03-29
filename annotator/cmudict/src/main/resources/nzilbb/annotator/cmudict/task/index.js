startLoading();

// show annotator version
getVersion(version => {
  document.getElementById("version").innerHTML = version;
});

const taskId = window.location.search.substring(1);

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
  var pronunciationLayerId = document.getElementById("pronunciationLayerId");
  addLayerOptions(
    pronunciationLayerId, schema,
    layer => layer.parentId == schema.wordLayerId && layer.alignment == 0);
  pronunciationLayerId.selectedIndex = 0;
  
  // GET request to getTaskParameters retrieves the current task parameters, if any
  getText("getTaskParameters", text => {
    try {
      var parameters = new URLSearchParams(text);
      
      // set initial values of properties in the form above
      // (this assumes bean property names match input id's in the form above)
      for (const [key, value] of parameters) {
        document.getElementById(key).value = value;
      }
      // set the checkbox
      document.getElementById("firstVariantOnly").checked
        = parameters.get("firstVariantOnly");
      // if there's no pronunciation layer defined
      if (pronunciationLayerId.selectedIndex == 0) {
        // but there's a layer named after the task
        if ( schema.layers[taskId]) {
          
          // select that layer by default
          pronunciationLayerId.value = taskId;
        } else if (/.+:.+/.test(taskId)) { // might be an 'auxiliary'?
          var layerId = taskId.replace(/:.+/,"");
          if (schema.layers[layerId]) { // there's a layer named after the task
            // select it
            pronunciationLayerId.value = layerId;
          }
        }
        // if there's no option for the output layer, add one
        if (pronunciationLayerId.value != taskId) {
          var layerOption = document.createElement("option");
          layerOption.appendChild(document.createTextNode(taskId));
          pronunciationLayerId.appendChild(layerOption);
          pronunciationLayerId.value = taskId;
        }
      } // no pronunciation layer defined
      // show correct encoding example
      changedEncoding(document.getElementById("encoding"));
    } finally {
      finishedLoading();
    }
  });
});

// this function detects when the user selects [add new layer]:
function changedLayer(select) {
  if (select.value == "[add new layer]") {
    var newLayer = prompt("Please enter the new layer ID", taskId);
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

// this function detects when the user changes the encoding, and shows an example:
function changedEncoding(select) {
  var example = "T R AE2 N S K R IH1 P SH AH0 N"; // CMU
  if (select.value == "DISC") {
    example = "tr{nskrIpS@n";
  }
  document.getElementById("encoding-example").innerHTML = example;
}

document.getElementById("pronunciationLayerId").onchange = function(e) {
  changedLayer(this); };
document.getElementById("encoding").onchange = function(e) {
  changedEncoding(this); };
