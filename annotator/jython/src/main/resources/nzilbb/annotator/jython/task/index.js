// show spinner
startLoading();

// show annotator version
getVersion(version=>{
  document.getElementById("version").innerHTML = version;
});

// Code editor
var editor = CodeMirror.fromTextArea(document.getElementById("script"), { lineNumbers: true });
editor.setSize(800, 600); // TODO can this be dynamic?

var taskId = window.location.search.substring(1);

// default new layer name
document.getElementById("newLayerId").value = taskId;

// first, get the layer schema
var schema = null;
getSchema(s => {
  schema = s;
  
  // set layer types for adding new layers
  document.getElementById("rootParent").value = schema.root.id;
  document.getElementById("turnParent").value = schema.turnLayerId;
  document.getElementById("wordParent").value = schema.wordLayerId;
  
  // GET request to getTaskParameters retrieves the current task parameters, if any
  getJSON("getTaskParameters", parameters => {
    try {
      if (parameters == null || parameters == "") {
        setScript(
          "# for each turn in the transcript\n"
	    +"for turn in transcript.all(\""+schema.turnLayerId+"\"):\n"
	    +"  if annotator.cancelling: break # cancelled by the user\n"
	    +"  # for each word\n"
	    +"  for word in turn.all(\""+schema.wordLayerId+"\"):\n"
	    +"    # change the following line to tag the word as desired \n"
	    +"    transcript.createTag(word, \""+taskId+"\", \"length: \" + str(len(word.label)))\n"
	    +"    log(\"Tagged word \" + word.label)\n");
      } else {
        
        // set initial values of properties in the form
        setScript(parameters.script);
      }
    } finally {
      // hide spinner
      finishedLoading();
    }
  });
});

// this function detects when the user selects [add new layer]
function changedLayer(select) {
  if (select.value == "[add new layer]") {
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
      var parentId = schema.wordLayerId;
      if (select.value == "[add new phrase layer]") {
        parentId = schema.turnLayerId;
      } else if (select.value == "[add new span layer]") {
        parentId = schema.root.id;
      }
      // select it
      select.selectedIndex = select.children.length - 1;
    } else {
      return false;
    }
  }
  return true;
}

function setScript(python) {
  python = addShebangs(python);
  var script = document.getElementById("script");
  script.value = python;
  editor.getDoc().setValue(python);
  checkShebangs(python, false);
}

// https://xkcd.com/1421/ ...
var inputLayerPatterns = [
  new RegExp("\\.list\\(\"([^\"]+)\"\\)", "g"),
  new RegExp("\\.list\\('([^']+)'\\)", "g"),
  new RegExp("\\.all\\(\"([^\"]+)\"\\)", "g"),
  new RegExp("\\.all\\('([^']+)'\\)", "g"),
  new RegExp("\\.my\\(\"([^\"]+)\"\\)", "g"),
  new RegExp("\\.my\\('([^']+)'\\)", "g"),
  new RegExp("\\.first\\(\"([^\"]+)\"\\)", "g"),
  new RegExp("\\.first\\('([^']+)'\\)", "g"),
  new RegExp("\\.last\\(\"([^\"]+)\"\\)", "g"),
  new RegExp("\\.last\\('([^']+)'\\)", "g"),
  new RegExp("\\.getAnnotations\\(\"([^\"]+)\"\\)", "g"),
  new RegExp("\\.getAnnotations\\('([^']+)'\\)", "g"),
  new RegExp("\\.annotations\\(\"([^\"]+)\"\\)", "g"),
  new RegExp("\\.annotations\\('([^']+)'\\)", "g"),
  new RegExp("\\.includingAnnotationsOn\\(\"([^\"]+)\"\\)", "g"),
  new RegExp("\\.includingAnnotationsOn\\('([^']+)'\\)", "g"),
  new RegExp("\\.includedAnnotationsOn\\(\"([^\"]+)\"\\)", "g"),
  new RegExp("\\.includedAnnotationsOn\\('([^']+)'\\)", "g"),
  new RegExp("\\.midpointIncludingAnnotationsOn\\(\"([^\"]+)\"\\)", "g"),
  new RegExp("\\.midpointIncludingAnnotationsOn\\('([^']+)'\\)", "g"),
  new RegExp("\\.tagsOn\\(\"([^\"]+)\"\\)", "g"),
  new RegExp("\\.tagsOn\\('([^']+)'\\)", "g"),
  new RegExp("\\.getAncestor\\(\"([^\"]+)\"\\)", "g"),
  new RegExp("\\.getAncestor\\('([^']+)'\\)", "g"),
  new RegExp("\\.overlappingAnnotations\\([^)]+\"([^\"]+)\"\\)", "g"),
  new RegExp("\\.overlappingAnnotations\\([^)]+'([^']+)'\\)")
];
var outputLayerPatterns = [ 
  // Annotation.createTag(layerId, label)
  new RegExp("\\.createTag\\(\"([^\"]+)\",[^)]+\\)", "g"), 
  new RegExp("\\.createTag\\('([^']+)',[^)]+\\)", "g"),
  // Graph.createTag(annotation, layerId, label)
  new RegExp("transcript\\.createTag\\([^,]+,\\s*\"([^\"]+)\",[^)]+\\)", "g"), 
  new RegExp("transcript\\.createTag\\([^,]+,\\s*'([^']+)',[^)]+\\)", "g"),
  // Graph.createTag(annotation, layerId, label)
  new RegExp("transcript\\.createSubdivision\\([^,]+,\\s*\"([^\"]+)\",[^)]+\\)", "g"), 
  new RegExp("transcript\\.createSubdivision\\([^,]+,\\s*'([^']+)',[^)]+\\)", "g"),
  // Graph.addTag(annotation, layerId, label)
  new RegExp("\\.addTag\\([^,]+,\\s*\"([^\"]+)\",[^)]+\\)", "g"),
  new RegExp("\\.addTag\\([^,]+,\\s*'([^']+)',[^)]+\\)", "g"),
  // Graph.createSpan(from, to, layerId, label[, parent])
  new RegExp("\\.createSpan\\([^)\"]+,\\s*\"([^\"]+)\"[^)]*\\)", "g"), 
  new RegExp("\\.createSpan\\([^)']+,\\s*'([^']+)'[^)]*\\)", "g"), 
  // Graph.addSpan(from, to, layerId, label[, parent])
  new RegExp("\\.addSpan\\([^)\"]+,\\s*\"([^\"]+)\"[^)]*\\)", "g"), 
  new RegExp("\\.addSpan\\([^)']+,\\s*'([^']+)'[^)]*\\)", "g"), 
  // Graph.createAnnotation(from, to, layerId, label[, parent])
  new RegExp("\\.createAnnotation\\([^\"]+,\\s*\"([^\"]+)\"[^)]*\\)", "g"), 
  new RegExp("\\.createAnnotation\\([^']+,\\s*'([^']+)'[^)]*\\)", "g"), 
  // Graph.addAnnotation(from, to, layerId, label[, parent])
  new RegExp("\\.addAnnotation\\([^\"]+,\\s*\"([^\"]+)\"[^)]*\\)", "g"), 
  new RegExp("\\.addAnnotation\\([^']+,\\s*'([^']+)'[^)]*\\)", "g"), 
];

function addShebangs(script) {

  // look for input/output layers...
  
  var outputLayers = [];
  for (var p in outputLayerPatterns) {
    var match = outputLayerPatterns[p].exec(script);
    while (match) {
      outputLayers.push(match[1]);
      // try for another match
      match = outputLayerPatterns[p].exec(script);
    } // next match
  } // next pattern

  var inputLayers = [];
  for (var p in inputLayerPatterns) {
    var match = inputLayerPatterns[p].exec(script);
    while (match) {
      // only if it's not also an output layer (e.g. they might remove old annotations from there)
      if (outputLayers.indexOf(match[1]) < 0) {
        inputLayers.push(match[1]);
      }
      // try for another match
      match = inputLayerPatterns[p].exec(script);
    } // next match
  } // next pattern
  
  // ensure the script has corresponding shebangs
  // (processed in reverse order because shebangs are prepended to the script)
  outputLayers.reverse()
  for (l in outputLayers) {
    var shebang = `# outputLayer: ${outputLayers[l]}\n`;
    if (!script.includes(shebang)) { // shebang isn't there
      script = shebang + script;
    }
  }
  inputLayers.reverse()
  for (l in inputLayers) {
    var shebang = `# inputLayer: ${inputLayers[l]}\n`;
    if (!script.includes(shebang)) { // shebang isn't there
      script = shebang + script;
    }
  }
  return script;
}

function checkShebangs(script, allowCancel) {
  var invalidInputLayers = [];
  var inputShebangPattern = new RegExp("^#[!|]? inputLayer:(.*)$", "gm");
  var match = inputShebangPattern.exec(script);
  while (match) {
    var layerId = match[1].trim();
    if (!schema.layers[layerId]) {
      invalidInputLayers.push(layerId);
    }
    match = inputShebangPattern.exec(script);
  } // next match

  var invalidOutputLayers = [];
  var outputShebangPattern = new RegExp("^#[!|]? outputLayer:(.*)$", "gm");
  match = outputShebangPattern.exec(script);
  while (match) {
    var layerId = match[1].trim();
    if (!schema.layers[layerId] && layerId != taskId) {
      invalidOutputLayers.push(layerId);
    }
    match = outputShebangPattern.exec(script);
  } // next match

  var warning = "";
  if (invalidInputLayers.length > 0) {
    warning += "The following input layers are invalid:\n"
      + invalidInputLayers.join("\n");
  }
  if (invalidOutputLayers.length > 0) {
    if (warning) warning += "\n";
    warning += "The following output layers are invalid:\n"
      + invalidOutputLayers.join("\n");
  }
  if (warning) {
    if (allowCancel) {
      return confirm(warning);
    } else {
      alert(warning);
    }
  }
  return true;
}

function loadScript() {
  var reader = new FileReader();
  var file = document.getElementById("loadLocalScript").files[0];
  reader.readAsText(file);
  reader.onload = function(event) {
    try {
      var txt = event.target.result;
      setScript(txt);
    } catch(exception) {
      alert("Unable to parse " + file.fileName + ": " + exception); 
    }
  };
  reader.onerror = function() {
    alert("Unable to read " + file.fileName);
  }; 
}

function saveScript() {
  try {
    var script = addShebangs(editor.getDoc().getValue());
    if (!checkShebangs(script, true)) {
      return false;
    }

    var scriptAsBlob = new Blob([script], { type:'text/plain' }); // TODO text/python
    var downloadLink = document.createElement("a");
    // name the file after the task by default
    var fileName = taskId + ".py";
    try {
      fileName = document.getElementById("loadLocalScript").files[0].name;
    } catch(x) {
    }
    downloadLink.download = fileName;
    downloadLink.style.display = "none";
    downloadLink.innerHTML = "Download File";
    downloadLink.href = (window.URL||window.webkitURL).createObjectURL(scriptAsBlob);
    document.body.appendChild(downloadLink);
    downloadLink.click();
  } catch(X) { alert(X); }
}

function setTaskParameters(form) {

  // validate script
  var pySource = document.getElementById("script");
  if (pySource.value.length == 0) {
    alert("You have not entered a script.");
    pySource.focus();
    return false;
  }

  if (!checkShebangs(pySource.value, true)) {
    return false;
  }
  return convertFormBodyToJSON(form);
}

function newLayer() {
  document.getElementById("addingLayer").style.display = "";
  document.getElementById("newLayerButton").style.display = "none";
  enableLayerAlignment();
}

function addLayer() {
  var newLayerId = document.getElementById("newLayerId").value;
  var newLayerParentId = document.getElementById("newLayerParentId").value;
  var newLayerAlignment = document.getElementById("newLayerAlignment").value;
  if (newLayerParentId != schema.wordLayerId && newLayerAlignment == "0") {
    // only word layers can be tags, so make it an interval by default
    newLayerAlignment = "2";
  }
  
  // check the new layer doesn't already exist
  if (schema.layers[newLayerId]) {
    alert("Layer already exists: " + newLayerId);
    return null;
  }

  // create the layer
  getJSON(resourceForFunction("newLayer", newLayerId, newLayerParentId, newLayerAlignment), s=>{
    // we get the new schema back
    schema = s;

    // finished adding, allow a new layer to be created
    document.getElementById("addingLayer").style.display = "none";
    document.getElementById("newLayerButton").style.display = "";
  });
}

function enableLayerAlignment() {
  var newLayerAlignment = document.getElementById("newLayerAlignment");
  var newLayerAlignment0 = document.getElementById("newLayerAlignment0");
  var newLayerParentId = document.getElementById("newLayerParentId")
  if (newLayerParentId.value == schema.wordLayerId) {
    newLayerAlignment0.disabled = false;
    newLayerAlignment.value = "0"; // tag by default
  } else {  // span/phrase layer
    newLayerAlignment0.disabled = true;
    newLayerAlignment.value = "2"; // interval by default
  }
}

// add event handlers
document.getElementById("loadLocalScript").onchange = function(e) { loadScript(); };
document.getElementById("saveLocalScript").onclick = function(e) { saveScript(); return false; };
document.getElementById("newLayerButton").onclick = function(e) { newLayer(); return false; };
document.getElementById("addLayerButton").onclick = function(e) { addLayer(); return false; };
document.getElementById("newLayerParentId").onchange = function(e) { enableLayerAlignment(); };
document.getElementById("form").onsubmit = function(e) { return setTaskParameters(this); };
