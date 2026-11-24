startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

// parse parameters
const parameters = new URLSearchParams(window.location.search);
const t = parameters.get("t") || parameters.get("table");
const field = document.getElementById("field");
const query = document.getElementById("query");
const matches = document.getElementById("matches");
const hiddenTypes = ["child-table", "text", "html", "hidden","geo-location"];
const maxRows = 100;

// set title
document.getElementById("entries").innerText = `Search ${t}`;

const fields = {}; // definitions for table fields (key is table name)

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

readFields(t).then(()=>{
  const header = document.getElementById("header");
  header.append(document.createElement("th")); // column for edit button
  for (let name in fields) {
    const f = fields[name];
    if (f.type == "string") {
      const option = document.createElement("option");
      option.appendChild(document.createTextNode(name));
      field.appendChild(option);      
    }
    if (!hiddenTypes.includes(f.type)) {
      const th = document.createElement("th");
      th.className = name.replaceAll(" ", "-");
      if (f.hint) th.title = f.hint;
      th.appendChild(document.createTextNode(name));
      header.append(th);
    }
  } // next field
  finishedLoading();
});

function search() {
  const f = field.value;
  const q = query.value;
  if (q) {
    startLoading();
    getJSON(resourceForFunction(
      "readEntries", t, f, q, maxRows), (rows) => {
        if (rows.length == 1 && rows[0]["DbTagger_error"]) { // error
          document.getElementById("error").innerText = rows[0]["DbTagger_error"];
          // clear the error after a short delay
          window.setTimeout(()=>document.getElementById("error").innerText = "", 3000);
        } else { // rows returned
          
          // clear any previous data
          matches.innerHTML = "";
          
          // list the rows
          for (let row of rows) {
            const tr = document.createElement("tr");
            
            matches.appendChild(tr);
            let td = document.createElement("td");
            const a = document.createElement("a");
            a.target = row.serial;
            a.title = "Edit entry";
            a.href = `entry.html?t=${t}&f=${f}&e=${row[f]}`;
            const img = document.createElement("img");
            img.src = "../edit.svg";
            img.alt = "✎";
            a.appendChild(img);
            td.appendChild(a);
            tr.appendChild(td);
            
            for (let name in fields) {
              const f = fields[name];
              if (!hiddenTypes.includes(f.type)) {
                td = document.createElement("td");
                td.className = name.replaceAll(" ", "-");
                if (f.hint) td.title = f.hint;
                let value = row[name];
                if (value == null) value = "";
                if (f.type == "boolean") {
                  value = (value == "true"?"✔":value);
                }
                td.appendChild(document.createTextNode(value));
                tr.append(td);
              }
            } // next field
          } // next row
          if (rows.length == maxRows) {
            const tr = document.createElement("tr");
            
            matches.appendChild(tr);
            const td = document.createElement("td");
            td.title = `Maximum number of rows matched: ${maxRows}`;
            td.appendChild(document.createTextNode("..."));
            tr.appendChild(td);
          }
        } // rows returned
        finishedLoading();
      });
  } // q is not blank
}
  
document.getElementById("btn-search").addEventListener("click", search);
document.getElementById("query").addEventListener("keyup", (event)=>{
  if (event.code == "Enter") search();
});

