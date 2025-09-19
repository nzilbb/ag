
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

getJSON("availableClassifiers", classifiersOptions => {
    var classifiersDiv = document.getElementById("classifiers");
    if (!classifiersOptions || classifiersOptions.length == 0) {
        classifiersDiv.appendChild(document.createTextNode(
            "The Stanford Named Entity Recognizer (NER) implementation has not been installed yet."));
    } else {
        classifiersDiv.appendChild(document.createTextNode(
            "The Stanford Named Entity Recognizer (NER) implement has already been installed with the following classifiers:"));
        var ul = document.createElement("ul");
        classifiersDiv.appendChild(ul);
        for (m in classifiersOptions) {
            var classifiersOption = classifiersOptions[m];
            var li = document.createElement("li");
            li.appendChild(document.createTextNode(classifiersOption));
            ul.appendChild(li);
        } // next option
    }
});
