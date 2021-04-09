
getVersion(version => { // <- a function to execute when we have a response
    document.getElementById("version").innerHTML = version;
});

function selectFile(input) {
    document.getElementById("upload-progress").style.display = "";
    var uploadProgress = document.getElementById("progress");
    
    var fd = new FormData();
    fd.append("file", input.files[0]);
    postForm("uploadZip", fd, function(e) {
        console.log("uploadResult " + this.responseText);
        uploadProgress.max = uploadProgress.max || 100;
        uploadProgress.value = uploadProgress.max;
        var result = this.responseText;
        if (!result) { // no error, upload succeeded
            document.getElementById("upload-result").innerHTML = "<p>File uploaded.</p>";
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
        console.log("upload failed " + this.responseText);
        uploadProgress.max = uploadProgress.max || 100;
        uploadProgress.value = uploadProgress.value || 1;
        document.getElementById("upload-result").innerHTML
            = "<p class='error'>"+(this.responseText||"Upload failed.")+"</p>";
    });
}

getJSON("availableModels", modelsOptions => {
    var modelsDiv = document.getElementById("models");
    if (!modelsOptions || modelsOptions.length == 0) {
        modelsDiv.appendChild(document.createTextNode(
            "The Stanford POS Tagger implementation has not been installed yet."));
    } else {
        modelsDiv.appendChild(document.createTextNode(
            "The Stanford POS Tagger implement has already been installed with the following models:"));
        var ul = document.createElement("ul");
        modelsDiv.appendChild(ul);
        for (m in modelsOptions) {
            var modelsOption = modelsOptions[m];
            var li = document.createElement("li");
            li.appendChild(document.createTextNode(modelsOption));
            ul.appendChild(li);
        } // next option
    }
});
