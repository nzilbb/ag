startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

// load lexicon list
function listLexicons() {
  startLoading();
  getJSON("listLexicons", lexiconIds => {
    var lexicons = document.getElementById("lexicons");
    lexicons.innerHTML = ""; // clear any existing list
    for (var l in lexiconIds) {
      const lexiconId = lexiconIds[l];
      var tr = document.createElement("tr");
      lexicons.appendChild(tr);
      var td = document.createElement("td");
      tr.appendChild(td);
      td.appendChild(document.createTextNode(lexiconId));
      
      td = document.createElement("td");
      tr.appendChild(td);
    }
    finishedLoading();
  });
}

listLexicons();

// are they a privileged user?
const url = "admin/index.html";
getText(url, (data) => {
  if (data) { // they can access the URL
    window.location = url;
  }
});
