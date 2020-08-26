
// any annotator method can be called to interrogate the annotator
getText( // (the getText function is defined in util.js)
    "getVersion", // <- the URL to GET from
    function(e) { // <- a function to execute when we have a response
        document.getElementById("version").innerHTML = this.responseText;
    });

// getConfig gets the current setup configuration, if any
// in this case, it gets the values of all declared properties of the annotator
// encoded as an HTML query string
getText( // (the getText function is defined in util.js)
    "getConfig", // <- the URL to GET from
    function(e) { // <- a function to execute when we have a response
        // in this case, the properties of the annotator are encoded as an HTML query string
        // so we can use URLSearchParams to decode it
        var parameters = new URLSearchParams(this.responseText);
    });

function selectFile(input) {
    document.getElementById("upload-progress").style.display = "";
    var uploadProgress = document.getElementById("progress");
    
    var fd = new FormData();
    fd.append("file", input.files[0]);
    postForm("uploadLexicon", fd, function(e) {
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
        console.log("uploadFailed " + this.responseText);
        uploadProgress.max = uploadProgress.max || 100;
        uploadProgress.value = uploadProgress.value || 1;
        document.getElementById("upload-result").innerHTML
            = "<p class='error'>"+this.responseText+"</p>";
    });
}

