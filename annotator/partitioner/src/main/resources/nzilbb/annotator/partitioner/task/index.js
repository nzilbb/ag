startLoading();

// show annotator version
getVersion(version => {
  document.getElementById("version").innerHTML = version;
});

const taskId = window.location.search.substring(1);

// first, get the layer schema
let schema = null;
getSchema(s => {
  schema = s;
  
  // populate layer input select options...
  const boundaryLayerId = document.getElementById("boundaryLayerId");
  addLayerOptions(
    boundaryLayerId, schema,
    // aligned layers from turn children up
    layer => layer.alignment == 2
      && layer.id != schema.wordLayerId
      && layer.parentId != schema.wordLayerId);
  // default value:
  boundaryLayerId.value = schema.turnLayerId;
  
  const tokenLayerId = document.getElementById("tokenLayerId");
  addLayerOptions(
    tokenLayerId, schema,
    // word layers
    layer => (layer.alignment != 0 // (not attribute layers)
              // span/phrase layers
              && (layer.parentId == schema.root.id
                  || layer.parentId == schema.turnLayerId
                  || layer.id == schema.turnLayerId))
      || layer.parentId == schema.wordLayerId); // or word layers
  // default value:
  if (schema.layers["orthography"]) {
    tokenLayerId.value = "orthography";
  } else {
    tokenLayerId.value = schema.wordLayerId;
  }
  
  // populate the transcript attribute layers...
  
  const excludeOnAttribute = document.getElementById("excludeOnAttribute");
  addLayerOptions(
    excludeOnAttribute, schema,
    layer => layer.parentId == schema.root.id && layer.alignment == 0
      && layer.id != schema.participantLayerId);
  // default value:
  if (schema.layers["transcript_type"]) {
    excludeOnAttribute.value = "transcript_type";
  }
    
  // populate layer output select options...          
  const destinationLayerId = document.getElementById("destinationLayerId");
  addLayerOptions(
    destinationLayerId, schema,
    // no system layers
    layer => layer.id != schema.root.id
      && layer.id != schema.participantLayerId
      && layer.id != schema.utteranceLayerId
      && layer.id != schema.turnLayerId
      && layer.id != schema.wordLayerId
      && layer.id != "segment"
    // no transcript attributes
      && !(layer.parentId == schema.root.id && layer.alignment == 0)
    // no participant attributes
      && !(layer.parentId == schema.participantLayerId && layer.alignment == 0));
  if (schema.layers[taskId]) {
    destinationLayerId.value = taskId;
  }
  
  // GET request to getTaskParameters retrieves the current task parameters, if any
  getText("getTaskParameters", text => {
    try {
      if (text) {
        const parameters = new URLSearchParams(text);
        
        // set initial values of properties in the form above
        // (this assumes bean property names match input id's in the form above)
        for (const [key, value] of parameters) {
          document.getElementById(key).value = value;
        }
        // set the checkboxes
        document.getElementById("leftOvers").checked = parameters.get("leftOvers");
      }
      // if there's no output layer defined
      if (destinationLayerId.selectedIndex <= 1) {
        if (!schema.layers[taskId]) { // there's  layer named after the task
          // so create one
          const layerOption = document.createElement("option");
          layerOption.appendChild(document.createTextNode(taskId));
          destinationLayerId.appendChild(layerOption);
        }
        // select that layer by default
        destinationLayerId.value = taskId;
      }
      changeExclusionAttribute(excludeOnAttribute);
      setNumTokensUnits(tokenLayerId);
      describePartitionLabels();
    } finally {
      finishedLoading();
    }
  });
});
// this function detects when the user selects [add new layer]:
function changedLayer(select) {
  if (select.value == "[add new layer]") {
    const newLayer = prompt("Please enter the new layer ID", taskId);
    if (newLayer) { // they didn't cancel
      // check there's not already a layer with that name
      for (let l in schema.layers) {
        const layer = schema.layers[l];
        if (layer.id == newLayer) {
          alert("A layer called "+newLayer+" already exists");
          select.selectedIndex = 0;
          return;
        }
      } // next layer
      // add the layer to the list
      const layerOption = document.createElement("option");
      layerOption.appendChild(document.createTextNode(newLayer));
      select.appendChild(layerOption);
      // select it
      select.selectedIndex = select.children.length - 1;
      describePartitionLabels();
    }
  } else {
    describePartitionLabels();
  }
}

function setNumTokensUnits(tokenLayerId) {
  const numTokensUnits = document.getElementById("numTokensUnits");
  if (tokenLayerId.selectedIndex == 0) {
    numTokensUnits.innerHTML = "seconds";
  } else {
    numTokensUnits.innerHTML = "tokens";
  }
}

function setExcludeValues() {
  // set the value of excludeOnAttributeValues from the include checkboxes
  // i.e. put in excludeOnAttributeValues all checkboxes that are *unticked*
  const excludedValues = [];
  const collection = document.getElementsByClassName("include-exclude");
  for (let i = 0; i < collection.length; i++) {
    const chk = collection[i];
    if (!chk.checked) excludedValues.push(chk.value);
  }
  const excludeOnAttributeValues = document.getElementById("excludeOnAttributeValues");
  excludeOnAttributeValues.value = excludedValues.join(",");
}

function setIncludeValues() {
  // set the include checkboxes from the value of excludeOnAttributeValues
  // i.e. tick all checkboxes *except* those in excludeOnAttributeValues
  const excludeOnAttributeValues = document.getElementById("excludeOnAttributeValues");
  const excludedValues = excludeOnAttributeValues.value.split(",");
  const collection = document.getElementsByClassName("include-exclude");
  for (let i = 0; i < collection.length; i++) {
    const chk = collection[i];
    chk.checked = !excludedValues.includes(chk.value);
    chk.onchange = setExcludeValues;
  }
}

function changeExclusionAttribute(excludeOnAttribute) {
  // create checkbox options to *include* values from validLabels
  const excludeOnAttributeLayer = schema.layers[excludeOnAttribute.value];
  // remove old options
  includeOnAttributeValues.innerHTML = "";
  if (excludeOnAttributeLayer) {
    const validLabels = excludeOnAttributeLayer.validLabels;
    if (validLabels && Object.keys(validLabels).length > 0) { // there are valid labels
      const includeOnAttributeValues = document.getElementById("includeOnAttributeValues");
      // add new options
      for (let label of Object.keys(validLabels).sort()) {
        let description = validLabels[label] != label?label:`${label} (validLabels[label])`;
        const valueInput = document.createElement("input");
        valueInput.type = "checkbox";
        valueInput.id = `chk-${label}`;        
        valueInput.value = label;
        valueInput.className = "include-exclude field";
        const valueLabel = document.createElement("label");
        valueLabel.className = "option";
        valueLabel.title = `Include '${description}' transcripts`;
        valueLabel.appendChild(valueInput);
        valueLabel.appendChild(document.createTextNode(label));
        includeOnAttributeValues.appendChild(valueLabel);
      } // next valid label
      setIncludeValues();
    } // there are valid labels
  } // the attribute layer is valid
}

function validate(e) {
  if (boundaryLayerId.value == destinationLayerId.value) {
    alert(`Partition Boundary Layer and Partition Layer can't be the same: ${destinationLayerId.value}`);
    destinationLayerId.focus();
    return false;
  }
  return true;
}

function describePartitionLabels() {
  const partitionLabels = document.getElementById("partitionLabels");
  if (!tokenLayerId.value) {
    partitionLabels.innerHTML = "duration in seconds";
  } else {
    const partitionLayer = schema.layers[destinationLayerId.value];
    if (partitionLayer
        && (partitionLayer.id == schema.wordLayerId
            || partitionLayer.parentId == schema.wordLayerId)) {
      partitionLabels.innerHTML = `copied from ${tokenLayerId.value}`;
    } else {
      partitionLabels.innerHTML = `number of ${tokenLayerId.value} tokens`;
    }
  }
}

document.getElementById("destinationLayerId").onchange = function(e) {
  changedLayer(this); };
document.getElementById("tokenLayerId").onchange = function(e) {
  describePartitionLabels();
  setNumTokensUnits(this);
};
document.getElementById("excludeOnAttribute").onchange = function(e) {
  changeExclusionAttribute(this); };
document.getElementById("form").onsubmit = validate;
