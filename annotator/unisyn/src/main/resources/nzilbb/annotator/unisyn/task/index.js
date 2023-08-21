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

  // populate syllable recovery phone layer options...
  var phoneLayerId = document.getElementById("phoneLayerId");
  addLayerOptions(
    phoneLayerId, schema,
    layer => layer.id != taskId
      && ((layer.parentId == schema.wordLayerId && layer.alignment == 2) // segment
          || (layer.parentId == "segment"))); // or segment tags
  
  // populate layer output select options...          
  var tagLayerId = document.getElementById("tagLayerId");
  addLayerOptions(
    tagLayerId, schema,
    layer => layer.parentId == schema.wordLayerId);
  tagLayerId.selectedIndex = 0;
  
  loadLexiconOptions(() => {
    
    // GET request to getTaskParameters retrieves the current task parameters, if any
    getText("getTaskParameters", text => {
      try {
        var parameters = new URLSearchParams(text);
        
        // set initial values of properties in the form above
        // (this assumes bean property names match input id's in the form above)
        for (const [key, value] of parameters) {
          document.getElementById(key).value = value;
        }
        // set the checkboxes
        document.getElementById("firstVariantOnly").checked
          = parameters.get("firstVariantOnly");
        document.getElementById("stripSyllStress").checked
          = parameters.get("stripSyllStress");
        // dis/enable checkboxes
        changedPhoneLayer(document.getElementById("phoneLayerId"), true);
        // if there's no utterance tag layer defined
        if (tagLayerId.selectedIndex == 0
            // but there's a layer named after the task
            && schema.layers[taskId]) {
          
          // select that layer by default
          tagLayerId.value = taskId;
        }
      } finally {
        finishedLoading();
      }
    });
  });
});

function loadLexiconOptions(onLoad) {
  // populate dictionary options
  getJSON("getLexiconIds", lexicons => {
    var lexicon = document.getElementById("lexicon");
    lexicon.innerHTML = "";
    var originalValue = lexicon.value;
    var option = document.createElement("option");
    option.disabled = true;
    option.appendChild(document.createTextNode("[select lexicon]"));
    lexicon.appendChild(option);
    
    for (l of lexicons) {
      option = document.createElement("option");
      option.appendChild(document.createTextNode(l));
      lexicon.appendChild(option);
    } // next dictionary
    if (!lexicons.length) {
      // no lexicons yet, so they'll want to upload a file
      document.getElementById("uploadForm").setAttribute("open",true);
    }
    if (originalValue) {
      lexicon.value = originalValue;
    } else {
      lexicon.selectedIndex = 0;
    }

    if (onLoad) onLoad();
  });
}

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

function selectFile() {
  console.log("selectFile...");
  var input = document.getElementById("file");
  console.log("input " + input);
  console.log("files " + input.files);
  if (input.files.length == 0) return;
  var file = input.files[0];
  console.log("file " + file);
  console.log("name " + file.name);
  document.getElementById("name").value = file.name
  document.getElementById("btnUploadLexicon").removeAttribute("disabled");
}

function uploadLexicon() {
  document.getElementById("uploadProgress").style.display = "";
  var uploadProgress = document.getElementById("progress");
  var input = document.getElementById("file");
  if (input.files.length == 0) return;
  var file = input.files[0];
  var fd = new FormData();
  fd.append("lexicon", document.getElementById("name").value);
  fd.append("file", file);
  postForm("loadLexicon", fd, function(e) {
    console.log("uploadResult " + this.responseText);
    uploadProgress.max = uploadProgress.max || 100;
    uploadProgress.value = uploadProgress.max;
    var result = this.responseText;
    if (!result) { // no error, upload succeeded
      document.getElementById("uploadResult").innerHTML = "<p>File uploaded.</p>";
      
      // now track load progress
      trackLexiconLoad();
    } else { // error
      document.getElementById("uploadResult").innerHTML
        = "<p class='error'>"+result+"</p>";
    }
  }, function(e) {
    console.log("uploadProgress " + e.loaded);
    if (e.lengthComputable) {
      uploadProgress.max = e.total;
      uploadProgress.value = e.loaded;
    }
  }, function(e) {
    console.log("uploadFailed " + this.responseText);
    uploadProgress.max = uploadProgress.max || 100;
    uploadProgress.value = uploadProgress.value || 1;
    document.getElementById("uploadResult").innerHTML
      = "<p class='error'>Upload failed: "+this.responseText+"</p>";
  });
}

function trackLexiconLoad() {
  // still running?
  getText("getRunning", running => {
    if (running == "true") {
      // keep tracking progress
      window.setTimeout(trackLexiconLoad, 1000);
    } else { // finished
      console.log("load finished.");
      loadLexiconOptions(() => {
        console.log("lexicon options loaded.");
        var lexicon = document.getElementById("lexicon");
        if (lexicon.selectedIndex <= 0) {
          // select the first dictionary for that lexicon
          var name = document.getElementById("name").value;
          for (var d in lexicon.options) {
            if (lexicon.options[d].value == name) {
              lexicon.selectedIndex = d;
              document.getElementById("uploadForm").removeAttribute("open");
              break;
            }
          } // next dictionary
        } // no dictionary already selected
      });
    }
    
    // get progress
    getText("getPercentComplete", percentComplete => {
      document.getElementById("progress").max = 100;
      document.getElementById("progress").value = parseInt(percentComplete);
    });
    
    // get status
    getText("getStatus", status => {
      document.getElementById("uploadResult").innerHTML = `<p>${status}</p>`;
    });
    
  });
}

function changedPhoneLayer(phoneLayerId, skipFieldUpdate) {
  document.getElementById("firstVariantOnly").disabled
    = document.getElementById("stripSyllStress").disabled
    = phoneLayerId.selectedIndex > 0;
  if (phoneLayerId.selectedIndex > 0 && !skipFieldUpdate) {
    document.getElementById("field").value = "pron_disc";
  }
}

document.getElementById("tagLayerId").onchange = function(e) {
    changedLayer(this); };
document.getElementById("file").onchange = selectFile;
document.getElementById("phoneLayerId").onchange = function(e) {
  changedPhoneLayer(this); };
document.getElementById("btnUploadLexicon").onclick = uploadLexicon;
