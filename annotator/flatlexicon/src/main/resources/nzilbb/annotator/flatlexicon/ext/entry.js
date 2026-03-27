startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

// parse parameters
const parameters = new URLSearchParams(window.location.search);
const lexicon = parameters.get("l") || parameters.get("lexicon");
const field = parameters.get("f") || parameters.get("field");
const entry = parameters.get("e") || parameters.get("entry");
console.log(`lexicon "${lexicon}" field "${field}" entry "${entry}"`);

document.getElementById("entry").innerText = entry;

let fields = [];

function showForm(data) {
  if (data.FlatLexiconTagger_error) {
    document.getElementById("error").innerText = data.FlatLexiconTagger_error;
    return;
  }
  const attributes = document.getElementById("attributes");
  attributes.innerHTML = ""; // clear any existing list
  fields = Object.keys(data);
  for (let name of fields) {
    const row = document.createElement("div");
    row.className = "field";
    row.title = name;
    let label = document.createElement("label");
    label.appendChild(document.createTextNode(name));
    row.appendChild(label);
    
    let value = document.createElement("span");
    value.className = "value";
    if (name == field) { // the ID is read-only
      let span = document.createElement("span");
      span.className = "read-only";
      span.id = `attribute-${name}`;
      span.appendChild(document.createTextNode(data[name]));
      value.appendChild(span);
    } else {
      let input = document.createElement("input");
      input.type = "text"
      input.name = name;
      input.id = `attribute-${name}`;
      input.placeholder = name;
      input.value = data[name];
      input.onkeyup = input.onchange = function(e) {
        // show save button
        document.getElementById("btnSave").style.display = null;
      }
      value.appendChild(input);
    }
    row.appendChild(value);
    attributes.appendChild(row);
  }
  // hide save button
  document.getElementById("btnSave").style.display = "none";
}

// get all fields for entry
getJSON(`readEntry?${lexicon},${field},${entry}`, data => {
  startLoading();
  showForm(data);
  finishedLoading();
});

document.getElementById("btnSave").onclick = function (e) {
  document.getElementById("error").innerText = "";
  var fd = new FormData();
  fd.append("l", lexicon);
  fd.append("f", field);
  fd.append("e", entry);
  const data = {};
  for (let name of fields) {
    const input = document.getElementById(`attribute-${name}`);
    if (input && input.value) {
      data[name] = input.value;
    }
  } // next field
  fd.append("data", JSON.stringify(data));
  startLoading();
  postForm("updateEntry", fd, function(e) {
    showForm(JSON.parse(this.responseText));
    finishedLoading();
  });
}
