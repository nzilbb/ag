
getVersion(version => { // <- a function to execute when we have a response
    document.getElementById("version").innerHTML = version;
});

function validate() {
    if (!document.getElementById("tou").checked) {
        alert("You must agree to the Terms of Usage.");
        document.getElementById("tou").focus();
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
});

document.getElementById("setConfig").onsubmit = validate;
