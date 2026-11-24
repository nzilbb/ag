startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

// load table list
function listTables() {
  startLoading();
  getJSON("listTables", tableIds => {
    const tables = document.getElementById("tables");
    tables.innerHTML = ""; // clear any existing list
    for (var l in tableIds) {
      const tableId = tableIds[l];
      // we want to exclude lookup tables for now
      getJSON(resourceForFunction("readFields", tableId), (definitions) => {
        if (Object.keys(definitions).length > 1) { // multiple fields
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
        } // not a lookup table
      }); // readFields
    } // next table
    finishedLoading();
  });
}

function readFields(tableName) {
  return new Promise((resolve, reject)=>{
    getJSON(resourceForFunction("readFields", tableName), definitions => {
      
      // build a dictionary of definitions
      const childTablePromises = [];
      for (let definition of definitions) {
        if (definition.field) {
          fields[definition.field] = definition;
        }
      } // next field definition
      resolve();
    });
  });
} // readFields

listTables();
