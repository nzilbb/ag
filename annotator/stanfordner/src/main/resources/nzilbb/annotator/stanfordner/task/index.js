startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

const defaultClassifier = "english.all.3class.distsim.crf.ser.gz"; // a good default
const taskId = window.location.search.substring(1);

// first, get the layer schema
let schema = null;
getSchema(s => {
    schema = s;
    
    // populate layer input select options...          
    const tokenLayerId = document.getElementById("tokenLayerId");
    addLayerOptions(
        tokenLayerId, schema,
        // this is a function that takes a layer and returns true for the ones we want
        layer => layer.id == schema.wordLayerId
            || (layer.parentId == schema.wordLayerId && layer.alignment == 0));
    tokenLayerId.value = schema.wordLayerId;
    
    const chunkLayerId = document.getElementById("chunkLayerId");
    addLayerOptions(
        chunkLayerId, schema,
        // this is a function that takes a layer and returns true for the ones we want
        layer => (layer.id == schema.turnLayerId || (layer.parentId == schema.turnLayerId))
            && layer.id != schema.wordLayerId);
    chunkLayerId.value = schema.turnLayerId;
    
    // populate the language layers...
    
    const transcriptLanguageLayerId = document.getElementById("transcriptLanguageLayerId");
    addLayerOptions(
        transcriptLanguageLayerId, schema,
        layer => layer.parentId == schema.root.id && layer.alignment == 0
            && /.*lang.*/.test(layer.id));
    // select the first one by default
    transcriptLanguageLayerId.selectedIndex = 1;
    
    const phraseLanguageLayerId = document.getElementById("phraseLanguageLayerId");
    addLayerOptions(
        phraseLanguageLayerId, schema,
        layer => layer.parentId == schema.turnLayerId && layer.alignment == 2
            && /.*lang.*/.test(layer.id));
    // select the first one by default
    phraseLanguageLayerId.selectedIndex = 1;
    
    // populate layer output select options...          
    const entityLayerId = document.getElementById("entityLayerId");
    addLayerOptions(
        entityLayerId, schema,
      layer => layer.parentId == schema.wordLayerId
        && (layer.alignment == 0 // entity layers are aligned
            || layer.id == taskId)); // ...but they might have set up the alignment wrong
    entityLayerId.selectedIndex = 0;

    // what classifiers are available
    loadClassifiers().then(()=>{

        // GET request to getTaskParameters retrieves the current task parameters, if any
        getText("getTaskParameters", text => {
            try {
                const parameters = new URLSearchParams("?"+text);
                
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
                            const select = document.getElementById("entityLayerId");
                            const layerOption = document.createElement("option");
                            layerOption.appendChild(document.createTextNode(value));
                            select.appendChild(layerOption);
                            // select it
                            select.selectedIndex = select.children.length - 1;
                        }
                    } // entityLayerId
                } // next parameter
                
                // if there's no entity layer defined
                if (entityLayerId.selectedIndex == 0 && taskId) {
                  // but there's a layer named after the task
                  if (!schema.layers[taskId]) {
                    // add an option for a layer named after the taskId
                    const layerOption = document.createElement("option");
                    layerOption.appendChild(document.createTextNode(taskId));
                    entityLayerId.appendChild(layerOption);
                  }
                  
                  // select that layer by default
                  entityLayerId.value = taskId;
                }
            } finally {
                finishedLoading();
            }
        });        
    });    
});

function loadClassifiers() {
  return new Promise((accept, reject)=> {
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
          const classifierOption = classifierOptions[m];
          const option = document.createElement("option");
          option.value=classifierOption
          if (classifierOption == defaultClassifier) {
            option.selected = true;
          }
          option.appendChild(document.createTextNode(classifierOption));
          selectClassifier.appendChild(option);
        } // next option
        // finally, option to upload a classifier file
        const uploadOption = document.createElement("option");
        uploadOption.value="";
        uploadOption.appendChild(document.createTextNode("[other classifier]"));
        selectClassifier.appendChild(uploadOption);
      }
      accept();
    }); // getJSON
  }); // Promise
} // loadClassifiers

// this function detects when the user selects [add new layer]:
function changedLayer(select) {
    if (select.value == "[add new layer]") {
        const newLayer = prompt("Please enter the new layer ID", "namedEntity");
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
        }
    }
}

function testPatterns(alertOnError) {
    const tokenExclusionPattern = document.getElementById("tokenExclusionPattern");
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
    const targetLanguagePattern = document.getElementById("targetLanguagePattern");
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

function changedClassifier(classifier) {
  if (!classifier.value) { // selected 'other classifier'
    // reveal upload form
    document.getElementById("uploadClassifier").style.display = null;
    document.getElementById("file").click();
  }
}

function selectFile() {
  console.log("selectFile...");
  const input = document.getElementById("file");
  document.getElementById("uploadProgress").style.display = null;
  document.getElementById("uploadResult").innerHTML = "";
  const uploadProgress = document.getElementById("progress");
  console.log("input " + input);
  console.log("files " + input.files);
  if (input.files.length == 0) return;
  const file = input.files[0];
  console.log("file " + file);
  console.log("name " + file.name);
  const fd = new FormData();
  fd.append("file", file);
  postForm("installClassifiers", fd, function(e) {
    console.log("uploadResult " + this.responseText);
    uploadProgress.max = uploadProgress.max || 100;
    uploadProgress.value = uploadProgress.max;
    const result = this.responseText;
    if (!result) { // no error, upload succeeded
      document.getElementById("uploadResult").innerHTML = "<p>File uploaded.</p>";
      alert("Classifier(s) installed.");
      
      // unset file value so the file isn't sent when we save config
      input.value = "";
      
      // hide the uploader
      document.getElementById("uploadClassifier").style.display = "none";
      
      // reload classifiers list
      loadClassifiers();
    } else { // error
      document.getElementById("uploadResult").innerHTML
        = `<p class='error'>${result}</p>`;
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
      = `<p class='error'>${this.responseText}</p>`;
  });
}

document.getElementById("tokenExclusionPattern").onkeyup = function(e) {
  testPatterns();
}
document.getElementById("targetLanguagePattern").onkeyup = function(e) {
  testPatterns();
}
document.getElementById("entityLayerId").onchange = function(e) {
  changedLayer(this);
}
document.getElementById("classifier").onchange = function(e) {
  changedClassifier(this);
}
document.getElementById("file").onchange = selectFile;
