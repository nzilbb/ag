
getVersion(version => { // <- a function to execute when we have a response
    document.getElementById("version").innerHTML = version;
});

function checkForm() {
    if (document.getElementById("mfaPath").value) {
        // we have mfaPath, so we don't need condaPath and mfaEnvironment
        document.getElementById("condaPathField").style.display = "none";
        document.getElementById("mfaEnvironmentField").style.display = "none";
    } else {
        inferMfaPath();
    }
}

function inferMfaPath() {
    const condaPath = document.getElementById("condaPath").value;
    const mfaEnvironment = document.getElementById("mfaEnvironment").value;
    if (condaPath && mfaEnvironment) {
        // ask the annotator for a suggestion
        getText(resourceForFunction("inferMfaPath", condaPath, mfaEnvironment), mfaPath => {
            if (mfaPath) {
                document.getElementById("mfaPath").value = mfaPath;
                checkForm();
            }
        });
    }
}

function validate() {
    if (!document.getElementById("mfaPath").value) {
        alert("The path to MFA must be set.");
        document.getElementById("mfaPath").focus();
        return false;
    }
    return true;
}

get("getConfig", function(e) {
    var parameters = new URLSearchParams(this.responseText);
    // set initial values of properties in the form above
    // (this assumes bean property names match input id's in the form)
    for (const [key, value] of parameters) {
        document.getElementById(key).value = value;
    }
    checkForm();
});

document.getElementById("mfaPath").onchange = function(e) { checkForm(); };
document.getElementById("condaPath").onkeyup = function(e) { inferMfaPath(); };
document.getElementById("mfaEnvironment").onkeyup = function(e) { inferMfaPath(); };
document.getElementById("setConfig").onsubmit = validate;
