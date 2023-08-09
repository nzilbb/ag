startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

// load lexicon list
function listLexicons() {
  startLoading();
  getJSON("listLexicons", lexiconIds => {
    var lexicons = document.getElementById("lexicons");
    lexicons.innerHTML = ""; // clear any existing list
    for (var l in lexiconIds) {
      const lexiconId = lexiconIds[l];
      var tr = document.createElement("tr");
      lexicons.appendChild(tr);
      var td = document.createElement("td");
      tr.appendChild(td);
      td.appendChild(document.createTextNode(lexiconId));
      td = document.createElement("td");
      tr.appendChild(td);
      var button = document.createElement("button");            
      td.appendChild(button);
      button.title = `Delete ${lexiconId}`;
      button.appendChild(document.createTextNode("❌"));
      button.onclick = ()=>{deleteLexicon(lexiconId);};
    }
    finishedLoading();
  });
}

function deleteLexicon(lexiconId) {
  if (confirm(`Are you sure you want to delete the lexicon ${lexiconId}?`)) {
    getText(resourceForFunction("deleteLexicon", lexiconId), (error) => {
      if (error) alert(error);
      // refresh lexicon list
      listLexicons();
    }, "text/plain");
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
  document.getElementById("lexicon").value = file.name
  document.getElementById("btnUploadLexicon").removeAttribute("disabled");
}

function uploadLexicon() {
  document.getElementById("uploadProgress").style.display = "";
  var uploadProgress = document.getElementById("progress");
  var input = document.getElementById("file");
  if (input.files.length == 0) return;
  var file = input.files[0];
  var fd = new FormData();
  fd.append("lexicon", document.getElementById("lexicon").value);
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
      = "<p class='error'>"+this.responseText+"</p>";
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
      document.getElementById("uploadForm").removeAttribute("open");
      listLexicons();
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

document.getElementById("file").onchange = selectFile;
document.getElementById("btnUploadLexicon").onclick = uploadLexicon;

listLexicons();
