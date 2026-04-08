
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
            checkGrammars();
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

function checkGrammars() {
    // show spinner
    startLoading();

    try {
        getJSON("availableGrammars", grammarsOptions => {
            var grammarsDiv = document.getElementById("grammars");
            grammarsDiv.innerHTML = "";
            if (!grammarsOptions || grammarsOptions.length == 0) {
                grammarsDiv.appendChild(document.createTextNode(
                    "No grammars have been installed yet."));
            } else {
                grammarsDiv.appendChild(document.createTextNode(
                    "The following grammars are installed:"));
                var ul = document.createElement("ul");
                grammarsDiv.appendChild(ul);
                for (m in grammarsOptions) {
                    var grammarsOption = grammarsOptions[m];
                    var li = document.createElement("li");
                    li.appendChild(document.createTextNode(grammarsOption + " "));
                    var a = document.createElement("a");
                    a.href = "downloadZip?" + grammarsOption + ".zip";
                    a.download = grammarsOption + ".zip";
                    a.title = "Download " + grammarsOption + ".zip"; // TODO i18n
                    var u = document.createElement("u");
                    u.appendChild(document.createTextNode("↓"));
                    a.appendChild(u);
                    li.appendChild(a);
                    ul.appendChild(li);
                } // next option
            }
        });
    } finally {
        // hide spinner
        finishedLoading();
    }
    
}
checkGrammars();
