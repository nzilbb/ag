
getVersion(version => { // <- a function to execute when we have a response
    document.getElementById("version").innerHTML = version;
});

function selectFile(input) {
    document.getElementById("upload-progress").style.display = "";
    var uploadProgress = document.getElementById("progress");
    
    var fd = new FormData();
    fd.append("file", input.files[0]);
    postForm("uploadModels", fd, function(e) {
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
        availableModels();
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

var models = "";

function availableModels() {
    getJSON("availableModels", modelsOptions => {
        
        // if there are no models available, do nothing
        if (modelsOptions.length == 0) return;
        
        // if there's only one option, select it
        if (modelsOptions.length == 1) models = modelsOptions[0];
        
        // list options...
        selectModels = document.getElementById("models");
        // remove all current options
        selectModels.textContent = "";
        // populate the span with options
        for (m in modelsOptions) {
            var modelsOption = modelsOptions[m];
            var option = document.createElement("option");
            option.value=modelsOption
            if (modelsOption == models) {
                option.selected = true;
            }
            option.appendChild(document.createTextNode(modelsOption));
            selectModels.appendChild(option);
        }
    })
}

function validateConfig() {
    selectModels = document.getElementById("models");
    if (!selectModels.value) {
        alert("You must upload pre-trained DeepSpeech models above.");
        return false;
    }
    return true;
}

getText("getConfig", parameters => {
    var config = new URLSearchParams(parameters);
    models = config.get("models");
    var deepSpeechVersion = document.getElementById("deepSpeechVersion");
    if (!config.get("deepSpeechVersion")) {
        deepSpeechVersion.className = "error";
        deepSpeechVersion.innerHTML
            = "DeepSpeech is not available on this system. Please install DeepSpeech.";
    } else {
        deepSpeechVersion.className = "";
        deepSpeechVersion.innerHTML = config.get("deepSpeechVersion");
    }
    availableModels();
});

