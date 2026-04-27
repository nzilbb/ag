startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

// parse parameters
const parameters = new URLSearchParams(window.location.search);
const table = parameters.get("t") || parameters.get("table");
const field = parameters.get("f") || parameters.get("field");
const entry = parameters.get("e") || parameters.get("entry");
console.log(`table "${table}" field "${field}" entry "${entry}"`);

document.getElementById("entry").innerText = entry;

let fields = {}; // definitions for fields (type/validation etc.)

function showForm(data) {
  if (data.FlatTableTagger_error) {
    document.getElementById("error").innerText = data.FlatTableTagger_error;
    return;
  }
  const attributes = document.getElementById("attributes");
  attributes.innerHTML = ""; // clear any existing list
  for (let name in fields) {
    const row = document.createElement("div");
    row.className = "field";
    row.title = name;
    let label = document.createElement("label");
    label.appendChild(document.createTextNode(name));
    row.appendChild(label);
    
    let value = document.createElement("span");
    value.className = "value";
    let span = document.createElement("span");
    span.className = fields[name].type;
    span.id = `attribute-${name}`;
    if (data[name]) {
      if (fields[name].type == "url") {
        const a = document.createElement("a");
        a.href = data[name];
        a.target = name;
        a.appendChild(document.createTextNode(data[name]));
        span.appendChild(a);
      } else if (fields[name].type == "email") {
        const a = document.createElement("a");
        a.href = "mailto:" + data[name];
        a.target = name;
        a.appendChild(document.createTextNode(data[name]));
        span.appendChild(a);
      } else if (fields[name].type == "html") {
        span.innerHTML = data[name];
      } else {
        span.appendChild(document.createTextNode(data[name]));
      }
    }
    value.appendChild(span);
    row.appendChild(value);
    attributes.appendChild(row);
  }
}

function enableEditLink() {
  const editUrl = "edit/entry.html"+window.location.search;
  getText(editUrl, (data) => {
    if (data) { // they can access the URL
      const editLink = document.getElementById("edit-link");
      editLink.href = editUrl;
      editLink.style.display = null;
    }
  });
}

// get field definitions
getJSON(resourceForFunction("readFields", table), definitions => {
  
  // build a dictionary of definitions
  for (let definition of definitions) {
    if (definition.field) {
      fields[definition.field] = definition;
    }
  } // next field definition
  
  // get all fields for entry
  getJSON(resourceForFunction("readEntry", table, field, entry), data => {
    startLoading();
    showForm(data);
    finishedLoading();
    enableEditLink();
  });
});

