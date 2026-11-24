startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

// load table list
let editTableLink = null;
function listTables() {
  startLoading();
  getJSON("listTables", tableIds => {
    const tables = document.getElementById("tables");
    tables.innerHTML = ""; // clear any existing list
    const readFieldsPromises = [];
    for (var l in tableIds) {
      readFieldsPromises.push(
        readFields(tableIds[l]).then((tableId) => {
          if (tableId) { // multiple fields
            const tr = document.createElement("tr");
            tables.appendChild(tr);
            let td = document.createElement("td");
            tr.appendChild(td);
            td.appendChild(document.createTextNode(tableId));
            
            td = document.createElement("td");
            tr.appendChild(td);
            const a = document.createElement("a");
            td.appendChild(a);
            a.target = tableId;
            a.title = `${tableId} entries`;
            a.href = `search.html?t=${tableId}`;
            const img = document.createElement("img");
            img.src = "../search.svg"
            img.alt = "🔍";
            a.appendChild(img);
            editTableLink = a; // for clicking if there's only one
          } // not a lookup table
        }));
    } // next table
    Promise.all(readFieldsPromises).then(()=>{
      if (tables.childElementCount == 1) {
        // there's only one table
        document.location = editTableLink.href;
      }
    });
    finishedLoading();
  });
}

function readFields(tableName) {
  return new Promise((resolve, reject)=>{
    // we want to exclude lookup tables for now
    getJSON(resourceForFunction("readFields", tableName), (definitions) => {
      if (Object.keys(definitions).length > 1) { // multiple fields
        resolve(tableName);
      } else {
        resolve(null);
      }
      
    });
  });
} // readFields

listTables();
