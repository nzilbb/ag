startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

// load table list
function listTables() {
    startLoading();
    getJSON("listTables", tableIds => {
        var tables = document.getElementById("tables");
        tables.innerHTML = ""; // clear any existing list
        for (var l in tableIds) {
            const tableId = tableIds[l];
            var tr = document.createElement("tr");
            tables.appendChild(tr);
            var td = document.createElement("td");
            tr.appendChild(td);
            td.appendChild(document.createTextNode(tableId));
            td = document.createElement("td");
            tr.appendChild(td);
        }
        if (!tableIds.length) {
            // no tables yet, so they'll want to upload a file
            document.getElementById("uploadForm").setAttribute("open",true);
        }
        finishedLoading();
    });
}

listTables();

// are they a privileged user?
const url = "admin/index.html";
getText(url, (data) => {
  if (data) { // they can access the URL
    window.location = url;
  }
});
