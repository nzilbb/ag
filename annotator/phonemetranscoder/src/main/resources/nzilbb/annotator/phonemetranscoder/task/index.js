// show spinner
startLoading();

// show annotator version
getVersion(version=>{
  document.getElementById("version").innerHTML = version;
});

var taskId = window.location.search.substring(1);

// first, get the layer schema
var schema = null;
getSchema(s => {
  schema = s;
  
  // populate layer input select options...          
  var sourceLayerId = document.getElementById("sourceLayerId");
  addLayerOptions(
    sourceLayerId, schema,
    // this is a function that takes a layer and returns true for the ones we want
    layer => layer.id == schema.wordLayerId
      || (layer.parentId == schema.wordLayerId && layer.id != "segment"));
  // force initial selection
  sourceLayerId.selectedIndex = 0;
  
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
  
  // populate layer destination select options...          
  var destinationLayerId = document.getElementById("destinationLayerId");
  addLayerOptions(
    destinationLayerId, schema,
    layer => (layer.parentId == schema.wordLayerId || layer.parentId == 'segment')
      && layer.alignment == 0);
  destinationLayerId.selectedIndex = 0;
  
  // GET request to getTaskParameters retrieves the current task parameters, if any
  getJSON("getTaskParameters", parameters => {
    try {
      document.getElementById("translation").selectedIndex = 0; // force selection
      if (parameters == null) {
        newMapping("", ""); // no parameters, start off with a blank one
        if (schema.layers[taskId]) { // there's a layer named after the task
          // select it
          destinationLayerId.value = taskId;
        } else if (/.+:.+/.test(taskId)) { // might be an 'auxiliary'?
          var layerId = taskId.replace(/:.+/,"");
          if (schema.layers[layerId]) { // there's a layer named after the task
            // select it
            destinationLayerId.value = layerId;
          }
        } else { // no existing layer
          // add one
          var layerOption = document.createElement("option");
          layerOption.appendChild(document.createTextNode(taskId));
          destinationLayerId.appendChild(layerOption);
          destinationLayerId.value = taskId;
        }
        changedLayer(destinationLayerId);
      } else {
        
        // set initial values of properties in the form
        document.getElementById("sourceLayerId").value = parameters.sourceLayerId;
        document.getElementById("transcriptLanguageLayerId").value
          = parameters.transcriptLanguageLayerId;
        document.getElementById("phraseLanguageLayerId").value
          = parameters.phraseLanguageLayerId;
        document.getElementById("language").value = parameters.language;
        document.getElementById("customDelimiter").value = parameters.customDelimiter;
        document.getElementById("copyCharacters").value = parameters.copyCharacters;
        destinationLayerId = document.getElementById("destinationLayerId");
        destinationLayerId.value = parameters.destinationLayerId;
        // if there's no option for that layer, add one
        if (destinationLayerId.value != parameters.destinationLayerId) {
          var layerOption = document.createElement("option");
          layerOption.appendChild(document.createTextNode(parameters.destinationLayerId));
          destinationLayerId.appendChild(layerOption);
          destinationLayerId.value = parameters.destinationLayerId;
        }
        document.getElementById("translation").value = parameters.translation
        // insert current mappings
        if (!parameters.custom) {
          newMapping("", ""); // no mappings, start off with a blank one
        } else {
          for (var mapping of parameters.custom) {
            newMapping(mapping.source, mapping.destination);
          } // next mapping
        }
        changedLayer(destinationLayerId, parameters.sourceLayerId);
      }
      changeTranslation();
    } finally {
      // hide spinner
      finishedLoading();
    }
  });
});

// destination layer changed
function changedLayer(select, previousSourceValue) {
  if (select.value == "[add layer]") {
    var newLayer = prompt( //  default is the task ID
      "Please enter the new layer ID", taskId);
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
  } else {
    // ensure the source layer is in the same scope as the destination layer
    var sourceLayerId = document.getElementById("sourceLayerId");
    if (!previousSourceValue) previousSourceValue = sourceLayerId.value;
    var destinationLayer = schema.layers[select.value];
    // clear existing options (except the first)
    while (sourceLayerId.options.length > 1) sourceLayerId.options.remove(1);
    if (destinationLayer && destinationLayer.parentId == "segment") {
      addLayerOptions(
        sourceLayerId, schema,
        layer => (layer.id == "segment" || layer.parentId == "segment")
          && layer.id != destinationLayer.id);
    } else { // word layer
      addLayerOptions(
        sourceLayerId, schema,
        layer => (layer.id == schema.wordLayerId
                  || (layer.parentId == schema.wordLayerId && layer.id != "segment"))
          && (!destinationLayer || layer.id != destinationLayer.id));
    }
    sourceLayerId.value = previousSourceValue;
    
  }
}

function changeTranslation() {
  const translation = document.getElementById("translation").value;
  // custom mappings form?
  let display = "none";
  // examples to display
  let sourceExample = "";
  let destinationExample = "";
  switch (translation) {
  case "DISC2CMU":
    sourceExample = "tr{nskrIpS@n";
    destinationExample = "T R AE2 N S K R IH2 P SH IH0 N";
    break;
  case "CMU2DISC":
    sourceExample = "T R AE2 N S K R IH1 P SH AH0 N ";
    destinationExample = "tr{nskrIpSVn";
    break;
  case "DISC2ARPAbet":
    sourceExample = "tr{nskrIpS@n";
    destinationExample = "T R AE2 N S K R IH2 P SH AX0 N";
    break;
  case "ARPAbet2DISC":
    sourceExample = "T R AE2 N S K R IH1 P SH AX0 N";
    destinationExample = "tr{nskrIpS@n";
    break;
  case "DISC2Unisyn":
    sourceExample = "\"tr{n-'skrIp-S@n";
    destinationExample = "~ t r a n . * s k r i p . sh @ n";
    break;
  case "Unisyn2DISC":
    sourceExample = "~ t r a n . * s k r i p . sh @ n";
    destinationExample = "\"tr{n-'skrIp-S@n";
    break;
  case "DISC2Kirshenbaum":
    sourceExample = "tr{nskrIpS@n";
    destinationExample = "tr&nskrIpS@n";
    break;
  case "Kirshenbaum2DISC":
    sourceExample = "tr&nskrIpS@n";
    destinationExample = "tr{nskrIpS@n";
    break;
  case "DISC2SAMPA":
    sourceExample = "str1n_";
    destinationExample = "streIndZ";
    break;
  case "SAMPA2DISC":
    sourceExample = "streIndZ";
    destinationExample = "str1n_";
    break;
  case "DISC2XSAMPA":
    sourceExample = "str1n_";
    destinationExample = "str\\eIndZ";
    break;
  case "XSAMPA2DISC":
    sourceExample = "str\\eIndZ";
    destinationExample = "str1n_";
    break;
  case "DISC2IPA":
    sourceExample = "tr{nskrIpS@n";
    destinationExample = "tɹænskɹɪpʃən";
    break;
  case "custom": 
    display = ""; // show mappings etc.
    break;
  }
  document.getElementById("mappingsDiv").style.display = display;
  document.getElementById("sourceExample").innerHTML = sourceExample;
  document.getElementById("destinationExample").innerHTML = destinationExample;

}

// ensure language regular expression is validated
var language = document.getElementById("language");
language.onkeyup = function() { validateRegularExpression(language); };

var lastMapping = null;
var mappingId = 1;

// Manage mappings

function newMapping(pattern, label) {
  
  var divMapping = document.createElement("div");
  divMapping.id = "mapping-"+mappingId;
  
  var sourceInput = document.createElement("input");
  sourceInput.id = "source-"+mappingId;
  sourceInput.type = "text";
  sourceInput.dataset.role = "pattern";
  sourceInput.className = "pattern";
  sourceInput.value = pattern;
  sourceInput.title = "Character or characters to match in source labels";
  sourceInput.placeholder = "Source Characters";
  sourceInput.style.width = "25%";
  sourceInput.style.textAlign = "center";
  sourceInput.onfocus = function() { lastMapping = this.parentNode; };
  
  var destinationInput = document.createElement("input");
  destinationInput.id = "destination-"+mappingId;
  destinationInput.type = "text";
  destinationInput.dataset.role = "label";
  destinationInput.className = "label";
  destinationInput.title = "Character or characters to copy into destination labels."
    +"\nLeaving this blank causes the source characters to be ignored.";
  destinationInput.placeholder = "Destination Characters";
  destinationInput.value = label;
  destinationInput.style.width = "25%";
  destinationInput.style.textAlign = "center";
  destinationInput.onfocus = function() { lastMapping = this.parentNode; };
    
  var arrow = document.createElement("span");
  arrow.innerHTML = " → ";
  
  divMapping.appendChild(sourceInput);
  divMapping.sourceInput = sourceInput;
  divMapping.appendChild(arrow);
  divMapping.appendChild(destinationInput);
  divMapping.destinationInput = destinationInput;

  document.getElementById("mappings").appendChild(divMapping);
  sourceInput.focus();
  
  enableRemoveButton();

  mappingId++;
  
  return false; // so form doesn't submit
}

function enableRemoveButton() {
  document.getElementById("removeButton").disabled = 
    document.getElementById("mappings").childElementCount <= 1;
}

function removeMapping() {
  if (lastMapping) { 
    document.getElementById("mappings").removeChild(lastMapping);
    lastMapping = null;
    enableRemoveButton();
  }
  return false; // so form doesn't submit
}

function moveMappingUp() {
  if (lastMapping) { 
    var mappings = document.getElementById("mappings");
    var previousMapping = lastMapping.previousSibling;
    if (previousMapping) {
      mappings.removeChild(lastMapping);
      mappings.insertBefore(lastMapping, previousMapping);
    }
  }
  return false; // so form doesn't submit
}

function moveMappingDown() {
  if (lastMapping) { 
    var mappings = document.getElementById("mappings");
    var nextMapping = lastMapping.nextSibling;
    if (nextMapping) {
      var nextNextMapping = nextMapping.nextSibling;
      mappings.removeChild(lastMapping);
      if (nextNextMapping) {
        mappings.insertBefore(lastMapping, nextNextMapping);
      } else {
        mappings.appendChild(lastMapping);
      }
    }
  }
  return false; // so form doesn't submit
}

function validateRegularExpression(input) {
  if (input.value.length == 0) {
    input.className = "";
    input.title = "";
  } else {
    try {        
      // test regular expression is valid
      new RegExp(input.value);
      // pattern is valid, so don't mark it as an error
      input.className = "";
      input.title = "";
    } catch(error) {
      // pattern is invalid, so don't mark it as an error
      input.className = "error";
      input.title = error;
    }
  }
}

function setTaskParameters(form) {
  
  // we use the convertFormBodyToJSON from util.js to send the form as JSON, but we want to
  // to add the mappings as an array of objects, so we add them to the parameters 
  // (convertFormBodyToJSON will take care of the rest of the form inputs)
  var parameters = {
    custom: []
  };
  var mappingDivs = document.getElementById("mappings").children;
  for (var m = 0; m < mappingDivs.length; m++) {
    var div = mappingDivs[m];
    if (div.sourceInput.value) { // ignore empty mapping rows
      parameters.custom.push({
        source: div.sourceInput.value,
        destination: div.destinationInput.value
      });
    }
  }
  
  return convertFormBodyToJSON(form, parameters);
}

function importMappings() {
  const input = document.getElementById("importButton");
  if (input.files.length == 0) return;
  const file = input.files[0];
  const reader = new FileReader();
  const component = this;  
  reader.onload = function() {  
    const data = reader.result;  
    records = data.split(/\r\n|\n/);
    // remove blank lines
    records = records.filter(l=>l.length>0);
    if (records.length == 0) {
      alert("File is empty: " + file.name);
    } else {
      
      let sourceInputs = document.getElementsByClassName("pattern");
      if (sourceInputs.length == 1 && !sourceInputs[0].value) {
        // there's only one empty mapping - i.e. they just started
        // so remove it
        lastMapping = sourceInputs[0].parentNode;
        removeMapping();
      }
      
      // get headers...
      const firstLine = records[0];
      // split the line into fields
      let delimiter = ",";
      if (firstLine.match(/.*\t.*/)) delimiter = "\t";
      else if (firstLine.match(/.;.*/)) delimiter = ";";
      let parseFields = row => row.split(fieldDelimiter);
      for (var r = 1; r < records.length; r++) {
        const record = records[r];
        let fields = record.split(delimiter);
        // strip quotes
        fields = fields.map(f=>f.replace(/^"(.*)"$/, "$1"));
        newMapping(fields[0], fields[1]);
      } // next mapping row
    } // not an empty file
    input.value = null;
  };
  reader.onerror = function () {  
    alert("Error reading " + file.name);
  };
  reader.readAsText(file);
  return false; // so the form doesn't submit
}
function exportMappings() {
  let tsv = "data:text/tsv;charset=utf-8,";
  tsv += "Pattern\tLabel";
  let sourceInputs = document.getElementsByClassName("pattern");
  for (let sourceInput of sourceInputs) {
    let destinationInput = document.getElementById(
      sourceInput.id.replace("source-", "destination-"));
    tsv += `\n${sourceInput.value}\t${destinationInput.value}`;
  } // mapping
  const encodedUri = encodeURI(tsv);
  const link = document.createElement("a");
  link.setAttribute("href", encodedUri);
  const now = new Date();
  link.setAttribute("download", `mappings-${taskId}.tsv`);
  link.style.display = "none";
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  return false; // so the form doesn't submit
}

// add event handlers
document.getElementById("addButton").onclick = e=>newMapping('','');
document.getElementById("upButton").onclick = e=>moveMappingUp();
document.getElementById("downButton").onclick = e=>moveMappingDown();
document.getElementById("removeButton").onclick = e=>removeMapping();
document.getElementById("importButton").onchange = e=>importMappings();
document.getElementById("exportButton").onclick = e=>exportMappings();
document.getElementById("destinationLayerId").onchange = function(e) { changedLayer(this); };
document.getElementById("translation").onchange = function(e) { changeTranslation(); };
document.getElementById("form").onsubmit = function(e) { setTaskParameters(this); };

