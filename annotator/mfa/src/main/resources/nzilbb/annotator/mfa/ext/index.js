
getVersion(version => { // <- a function to execute when we have a response
    document.getElementById("version").innerHTML = version;
});
getText("mfaVersion", version => {
    if (version) {
        document.getElementById("mfaVersion").innerHTML = version;
    } else {
        document.getElementById("mfaVersion").innerHTML = "Could not determine MFA version";
        document.getElementById("mfaVersion").className = "error";
    }
});

function deleteLog(log) {
    getJSON(resourceForFunction("deleteLog",log), deleted => {
        listLogs();
    });
}

function listLogs() {
    // show spinner
    startLoading();
    
    try {
        getJSON("listLogs", logs => {
            var logsDiv = document.getElementById("logs");
            logsDiv.innerHTML = "";
            if (!logs || logs.length == 0) {
                var tr = document.createElement("tr");
                logsDiv.appendChild(tr);
                var td = document.createElement("td");
                td.appendChild(document.createTextNode("There are no alignment logs."));
                tr.appendChild(td);
            } else {
                for (l of logs) {
                    var tr = document.createElement("tr");
                    logsDiv.appendChild(tr);
                    var td = document.createElement("td");
                    var a = document.createElement("a");
                    a.href = resourceForFunction("downloadLog",l);
                    a.download = l;
                    a.title = "Download " + l; // TODO i18n
                    a.appendChild(document.createTextNode(l));
                    td.appendChild(a);
                    tr.appendChild(td);
                    td = document.createElement("td");
                    var button = document.createElement("button");
                    button.appendChild(document.createTextNode("❌"));
                    button.title = "Delete " + l; // TODO i18n
                    const log = l
                    button.onclick = function() { deleteLog(log); };
                    td.appendChild(button);
                    tr.appendChild(td);
                } // next option
            }
        });
    } finally {
        // hide spinner
        finishedLoading();
    }
}
listLogs();
