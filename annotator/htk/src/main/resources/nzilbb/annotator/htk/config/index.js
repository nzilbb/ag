
getVersion(version => { // <- a function to execute when we have a response
    document.getElementById("version").innerHTML = version;
});

function getConfig() {
    get("getConfig", function(e) {
        var parameters = new URLSearchParams(this.responseText);
        // set initial values of properties in the form above
        // (this assumes bean property names match input id's in the form)
        for (const [key, value] of parameters) {
            try {
                document.getElementById(key).value = value;
            } catch (x) {
                console.log(key + ": " + x);
            }
        }

        if (document.getElementById("htkPath").value) {
            document.getElementById("attemptInstallation").style.display = "none";
        } else {
            document.getElementById("attemptInstallation").style.display = "";
        }
    });
}

function installHtk() {
    const htkUserId = document.getElementById("htkUserId").value;
    if (!htkUserId) {
        alert(
            "To attempt installation of HTK, you must specify the user ID for the HTK website.");
        document.getElementById("htkUserId").focus();
        return;
    }
    const htkPassword = document.getElementById("htkPassword").value;
    if (!htkPassword) {
        alert(
            "To attempt installation of HTK, you must specify the password for the HTK website.");
        document.getElementById("htkPassword").focus();
        return;
    }
    document.getElementById("installHtkStatus").className = "";
    document.getElementById("submit").setAttribute("disabled","");
    document.getElementById("installHtk").setAttribute("disabled","");
    get(resourceForFunction("installHtk", htkUserId, htkPassword));
    // track status so the user gets feedback
    setTimeout(trackInstallation, 500);
    return false;
}

function trackInstallation() {
    // show progress and status
    document.getElementById("installHtkProgress").style.display = "";
    document.getElementById("installHtkStatus").style.display = "";

    // find out whether installation is still running
    getText("getRunning", running => {
        if (running == "true") {
            // keep tracking progress
            setTimeout(trackInstallation, 500);
        } else {
            document.getElementById("submit").removeAttribute("disabled");
            document.getElementById("installHtk").removeAttribute("disabled");
            // did it work?
            getConfig();
        }
        
        // get progress
        getText("getPercentComplete", percentComplete => {
            document.getElementById("installHtkProgress").value = parseInt(percentComplete);
        });
        
        // get status
        getText("getStatus", status => {
            document.getElementById("installHtkStatus").innerHTML = status;
        });        
    });    
}

getConfig();
document.getElementById("installHtk").onclick = installHtk;
