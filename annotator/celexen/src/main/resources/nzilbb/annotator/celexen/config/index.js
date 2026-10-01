getVersion(version => { // <- a function to execute when we have a response
  document.getElementById("version").innerHTML = version;
});

getText("lexiconFileExists", value => {
  if (value == "true") {
    const fileMessage = document.createElement("li");
    fileMessage.innerHTML = "The lexicon file has already been uploaded.";    
    document.getElementById("messages").appendChild(fileMessage);
    document.getElementById("upload-form").className = "hidden";
    enableSubmit();
  }
  getText("lexiconDataExists", value => {
    const dataMessage = document.createElement("li");
    if (value == "true") {
      dataMessage.innerHTML = "The lexicon data has already been imported.";    
      document.getElementById("upload-form").className = "hidden";
      enableSubmit();
    } else {
      dataMessage.innerHTML = "The lexicon data has not yet been imported.";    
    }
    document.getElementById("messages").appendChild(dataMessage);
  });
});

getText("getDbConnectString", value => {
  document.getElementById("dbConnectString").value = value;
  enableSubmit();
});
getText("getDbUser", value => {
  document.getElementById("dbUser").value = value;
});
getText("getDbPassword", value => {
  document.getElementById("dbPassword").value = value;
});

function selectFile(input) {
  if (!input.files[0].name.endsWith(".zip")) {
    alert(`The file must be a zip file: ${input.files[0].name}`);
    input.files[0] = null;
    return;
  }
  document.getElementById("upload-progress").style.display = "";
  const uploadProgress = document.getElementById("progress");
  
  const fd = new FormData();
  fd.append("file", input.files[0]);
  postForm("uploadLexicon", fd, function(e) {
    console.log("uploadResult " + this.responseText);
    uploadProgress.max = uploadProgress.max || 100;
    uploadProgress.value = uploadProgress.max;
    const result = this.responseText;
    if (!result) { // no error, upload succeeded
      document.getElementById("upload-result").innerHTML = "<p>File uploaded.</p>";
      setTimeout(()=>{
        document.getElementById("upload-form").className = "hidden";
        enableSubmit();
      }, 2000);
    } else { // error
      document.getElementById("upload-result").innerHTML
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
    document.getElementById("upload-result").innerHTML
      = "<p class='error'>"+this.responseText+"</p>";
  });
}

function enableSubmit() {
  if (document.getElementById("dbConnectString").value
      || document.getElementById("upload-form").className == "hidden") {
    document.getElementById("submit").disabled = false;
  } else {
    document.getElementById("submit").disabled = true;
  }
}

document.getElementById("dbConnectString").onchange = enableSubmit;
