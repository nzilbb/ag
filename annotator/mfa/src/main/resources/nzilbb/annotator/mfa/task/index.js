startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});
getText("mfaVersion", version => {
    if (version) {
        document.getElementById("mfaVersion").innerHTML = version;
    } else {
        document.getElementById("mfaVersion").innerHTML = "Could not determine MFA version";
        document.getElementById("mfaVersion").className = "error";
    }
});

const taskId = window.location.search.substring(1);
let existingPhoneLayerId = null;

// get the layer schema
let schema = null;
getSchema(s => {
    schema = s;
    
    // populate layer input select options...
    
    const orthographyLayerId = document.getElementById("orthographyLayerId");
    addLayerOptions(
        orthographyLayerId, schema,
        // the word layer, or word tag layers
        layer => layer.id == schema.wordLayerId
            || (layer.parentId == schema.wordLayerId && layer.alignment == 0));
    // default value:
    if (schema.layers["orthography"]) {
        orthographyLayerId.value = "orthography";
    } else {
        orthographyLayerId.value = schema.wordLayerId;
    }
    
    const pronunciationLayerId = document.getElementById("pronunciationLayerId");
    addLayerOptions(
        pronunciationLayerId, schema,
        // the word layer, or word tag layers
        layer => layer.parentId == schema.wordLayerId && layer.alignment == 0);
    // default value:
    if (schema.layers["phonemes"]) {
        pronunciationLayerId.value = "phonemes";
    } else if (schema.layers["phonology"]) {
        pronunciationLayerId.value = "phonology";
    } else {
        // pick the first phonology-type layer
        for (let l in schema.layers) {
            const layer = schema.layers[l];
            if (layer.parentId == schema.wordLayerId
                && layer.alignment == 0
                && layer.type == "ipa"
                && layer.id != "pronounce") { // not the manual pronunciation tag layer
                pronunciationLayerId.value = layer.id;
                break;
            }
        } // next layer
    }

    const leftChannelParticipantLayerId = document.getElementById("leftChannelParticipantLayerId");
    addLayerOptions(
        leftChannelParticipantLayerId, schema,
        // the word layer, or word tag layers
        layer => layer.parentId == schema.root.id && layer.alignment == 0
        && layer.id != schema.participantLayerId && layer.id != "corpus" && layer.id != "episode");
    const rightChannelParticipantLayerId = document.getElementById("rightChannelParticipantLayerId");
    addLayerOptions(
        rightChannelParticipantLayerId, schema,
        // the word layer, or word tag layers
        layer => layer.parentId == schema.root.id && layer.alignment == 0
        && layer.id != schema.participantLayerId && layer.id != "corpus" && layer.id != "episode");

    // populate output layer select options

    const wordAlignmentLayerId = document.getElementById("wordAlignmentLayerId");
    addLayerOptions(
        wordAlignmentLayerId, schema,
        // word layer, or aligned turn children
        layer => layer.id == schema.wordLayerId
            || (layer.parentId == schema.turnLayerId && layer.alignment == 2
                && layer.id != schema.utteranceLayerId));
    // default value:
    wordAlignmentLayerId.value = schema.wordLayerId;
    
    const phoneAlignmentLayerId = document.getElementById("phoneAlignmentLayerId");
    addLayerOptions(
        phoneAlignmentLayerId, schema,
        // segment layer or aligned turn children
        layer => layer.id == "segment" || layer.id == "phone"
            || (layer.parentId == schema.turnLayerId && layer.alignment == 2
                && layer.id != schema.utteranceLayerId
                && layer.id != schema.wordLayerId));
    // default value:
    if (schema.layers["segment"]) {
        phoneAlignmentLayerId.value = "segment";
        existingPhoneLayerId = "segment";
    } else if (schema.layers["phone"]) {
        phoneAlignmentLayerId.value = "phone";
        existingPhoneLayerId = "phone";
    }
    
    const utteranceTagLayerId = document.getElementById("utteranceTagLayerId");
    addLayerOptions(
        utteranceTagLayerId, schema,
        // aligned turn children
        layer => layer.parentId == schema.turnLayerId && layer.alignment == 2
        // but not word nor utterance
            && layer.id != schema.wordLayerId && layer.id != schema.utteranceLayerId);
    
    // TODO const participantTagLayerId = document.getElementById("participantTagLayerId");
    // addLayerOptions(
    //     participantTagLayerId, schema,
    //     // participant attributes
    //     layer => layer.parentId == schema.participantLayerId && layer.alignment == 0);
    
  loadValidDictionaryNames()
    .then(loadValidAcousticModels)
    .then(getTaskParameters);
});

function loadValidDictionaryNames() {
  return new Promise((resolve, reject) => {
    // populate list of dictionary names
    getJSON("validDictionaryNames", names => {
      const dictionaryName = document.getElementById("dictionaryName");
      dictionaryName.innerHTML = '<option value="">[none]</option>';
      for (name of names) {
        var layerOption = document.createElement("option");
        layerOption.appendChild(document.createTextNode(name));
        dictionaryName.appendChild(layerOption);
      } // next name
      resolve();
    })
  });
}
function loadValidAcousticModels() {
  return new Promise((resolve, reject) => {
    // populate list of pretrained models names
    getJSON("validAcousticModels", names => {
      const modelsName = document.getElementById("modelsName");
      for (name of names) {
        var layerOption = document.createElement("option");
        layerOption.appendChild(document.createTextNode(name));
        modelsName.appendChild(layerOption);
      } // next name
      resolve();
    });
  });
}
function getTaskParameters() {    
    // GET request to getTaskParameters retrieves the current task parameters, if any
    getText("getTaskParameters", parameters => {
        try {
            if (!parameters) { // new task
                // set some sensible defaults
                getText("getOverlapThreshold", value => {
                    document.getElementById("overlapThreshold").value = value;
                });
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
                document.getElementById("ignoreAlignmentStatuses").checked
                    = parameters.get("ignoreAlignmentStatuses");
                document.getElementById("multilingualIPA").checked
                    = parameters.get("multilingualIPA");
                document.getElementById("noSpeakerAdaptation").checked
                    = parameters.get("noSpeakerAdaptation");
                document.getElementById("noCleanupOnFailure").checked
                    = parameters.get("noCleanupOnFailure");
                document.getElementById("usePostgres").checked
                    = parameters.get("usePostgres");
                document.getElementById("discOutput").checked
                    = parameters.get("discOutput");
                // either pronunciationLayerId or dictionaryName
                document.getElementById("pronunciationLayerId").disabled
                    = document.getElementById("multilingualIPA").disabled
                    = document.getElementById("dictionaryName").value != ""; 
                // no acoustic model means train/align, and speaker adaptation shouldn't be specified
                document.getElementById("noSpeakerAdaptation").disabled
                    = document.getElementById("modelsName").value == "";
            }
            // if there's no utterance tag layer defined
            if (utteranceTagLayerId.selectedIndex == 0
                // but there's a layer named after the task
                && schema.layers[taskId]
                // and it's not being otherwise used
                && wordAlignmentLayerId.value != taskId && phoneAlignmentLayerId != null) {
                
                // select that layer by default
                utteranceTagLayerId.value = taskId;
            }
        } finally {
            finishedLoading();
        }
    });
} // getTaskParameters

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

function selectDictFile(input) {
  if (!input.files[0]) return false;
  if (!/.*\.dict$/.test(input.files[0].name)) {
    alert(`${input.files[0].name} is not a dictionary file (.dict)`);
    return false;
  }
  
  document.getElementById("uploadDictProgress").style.display = "";
  const uploadProgress = document.getElementById("dictProgress");
  
  const fd = new FormData();
  fd.append("file", input.files[0]);
  postForm("uploadDictionary", fd, function(e) {
    console.log("uploadResult " + this.responseText);
    uploadProgress.max = uploadProgress.max || 100;
    uploadProgress.value = uploadProgress.max;
    var result = this.responseText;
    if (!result) { // no error, upload succeeded
      document.getElementById("uploadDictResult").innerHTML
        = "<p>Dictionary uploaded.</p>";
      // add new file to list
      const dictionaryName = document.getElementById("dictionaryName");
      const layerOption = document.createElement("option");
      const name = input.files[0].name.replace(/\.[^.]*$/,""); // remove extension
      layerOption.appendChild(document.createTextNode(name));
      dictionaryName.appendChild(layerOption);
      // select the uploaded file
      dictionaryName.value = name;
      // close the upload form
      document.getElementById("dictFileUpload").open = false;
    } else { // error
      document.getElementById("uploadDictResult").innerHTML
        = `<p class='error'>${result}</p>`;
    }
  }, function(e) {
    console.log("uploadProgress " + e.loaded);
    if (e.lengthComputable) {
      uploadProgress.max = e.total;
      uploadProgress.value = e.loaded;
    }
  }, function(e) {
    console.log("upload failed " + this.responseText);
    uploadProgress.max = uploadProgress.max || 100;
    uploadProgress.value = uploadProgress.value || 1;
    document.getElementById("uploadDictResult").innerHTML
      = "<p class='error'>"+(this.responseText||"Upload failed.")+"</p>";
  });
}

function selectModelsFile(input) {
  if (!input.files[0]) return false;
  if (!/.*\.zip$/.test(input.files[0].name)) {
    alert(`${input.files[0].name} is not an acoustic models file (.zip)`);
    return false;
  }
  
  document.getElementById("uploadModelsProgress").style.display = "";
  const uploadProgress = document.getElementById("modelsProgress");
  
  const fd = new FormData();
  fd.append("file", input.files[0]);
  postForm("uploadAcousticModels", fd, function(e) {
    console.log("uploadResult " + this.responseText);
    uploadProgress.max = uploadProgress.max || 100;
    uploadProgress.value = uploadProgress.max;
    var result = this.responseText;
    if (!result) { // no error, upload succeeded
      document.getElementById("uploadModelsResult").innerHTML
        = "<p>Acoustic models uploaded.</p>";
      // add new file as an option to the list
      const modelsName = document.getElementById("modelsName");
      const layerOption = document.createElement("option");
      const name = input.files[0].name.replace(/\.[^.]*$/,""); // remove extension
      layerOption.appendChild(document.createTextNode(name));
      modelsName.appendChild(layerOption);
      // select the uploaded file
      modelsName.value = name;
      // close the upload form
      document.getElementById("modelsFileUpload").open = false;
    } else { // error
      document.getElementById("uploadModelsResult").innerHTML
        = `<p class='error'>${result}</p>`;
    }
  }, function(e) {
    console.log("uploadProgress " + e.loaded);
    if (e.lengthComputable) {
      uploadProgress.max = e.total;
      uploadProgress.value = e.loaded;
    }
  }, function(e) {
    console.log("upload failed " + this.responseText);
    uploadProgress.max = uploadProgress.max || 100;
    uploadProgress.value = uploadProgress.value || 1;
    document.getElementById("uploadModelsResult").innerHTML
      = "<p class='error'>"+(this.responseText||"Upload failed.")+"</p>";
  });
}

// add event handlers
document.getElementById("wordAlignmentLayerId").onchange = function(e) {
    changedLayer(this, taskId + "Word"); };
document.getElementById("phoneAlignmentLayerId").onchange = function(e) {
    changedLayer(this, taskId + "Phone"); };
document.getElementById("utteranceTagLayerId").onchange = function(e) {
    changedLayer(this, taskId + "Time"); };
//TODO document.getElementById("participantTagLayerId").onchange = function(e) { changedLayer(this, "participant_" + taskId + "_time"); };
document.getElementById("dictFile").onchange = function(e) {
  selectDictFile(this); };
document.getElementById("modelsFile").onchange = function(e) {
  selectModelsFile(this); };

document.getElementById("form").onsubmit = function(e) {
    const wordAlignmentLayerId = document.getElementById("wordAlignmentLayerId");
    const phoneAlignmentLayerId = document.getElementById("phoneAlignmentLayerId");
    try {
        // wordAlignmentLayerId and phoneAlignmentLayerId must be both system layers, or neither
        if (existingPhoneLayerId
            && (wordAlignmentLayerId.value == schema.wordLayerId)
            != (phoneAlignmentLayerId.value == existingPhoneLayerId)) {
            alert(`For Word/Phone Alignment Layers, either both ${schema.wordLayerId} and ${existingPhoneLayerId} must be selected, or neither.`);
            wordAlignmentLayerId.focus();
            return false;
        }
        // either a pronunciationLayerId or a dictionaryName must be specified
        if (!document.getElementById("pronunciationLayerId").value
            && !document.getElementById("dictionaryName").value) {
            alert("You must select a Pronunciation Layer, or enter a Dictionary Name.");
            document.getElementById("pronunciationLayerId").focus();
            return false;
        }
        // if a dictionary is selected, models must also be selected
        if (document.getElementById("dictionaryName").value
           && !document.getElementById("modelsName").value) {
            alert("If you have selected a Dictionary Name, you must also select Pretrained Acoustic Models.");
            document.getElementById("modelsName").focus();
            return false;
        }
        return true;
    } catch (x) {
        alert(x);
        return false;
    }
}

document.getElementById("dictionaryName").onchange = function(e) {
  // either pronunciationLayerId or dictionaryName
  document.getElementById("pronunciationLayerId").disabled
  // if pronunciationLayerId, then phoneSet
    = document.getElementById("phoneSet").disabled
    = document.getElementById("dictionaryName").value != "";
}
document.getElementById("modelsName").onchange = function(e) {
  // no acoustic model means train/align, and speaker adaptation shouldn't be specified
  document.getElementById("noSpeakerAdaptation").disabled
    = document.getElementById("modelsName").value == "";
}
