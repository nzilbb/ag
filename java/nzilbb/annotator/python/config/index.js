
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

getText("getConfig", parameters => {
    var config = new URLSearchParams(parameters);
    document.getElementById("jythonUrl").value = config.get("jythonUrl");
    if (config.get("jythonAvailable") != "true") {
        // have to download it
        document.getElementById("downloadJythonFalse").disabled = true;
        document.getElementById("downloadJythonTrue").checked = true;
    } else {
        document.getElementById("downloadJythonFalse").checked
            = config.get("jythonAvailable") == "true";
        document.getElementById("downloadJythonTrue").checked
            = config.get("jythonAvailable") != "true";
    }
    enableUrl();
});

function enableUrl(e) {
    document.getElementById("jythonUrl").disabled
        = document.getElementById("downloadJythonFalse").checked;
}

document.getElementById("downloadJythonFalse").onchange = enableUrl;
document.getElementById("downloadJythonTrue").onchange = enableUrl;
