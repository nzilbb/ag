
getVersion(version => { // <- a function to execute when we have a response
    document.getElementById("version").innerHTML = version;
});

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

// are they a privileged user?
const url = "admin/index.html";
getText(url, (data) => {
  if (data) { // they can access the URL
    window.location = url;
  }
});
