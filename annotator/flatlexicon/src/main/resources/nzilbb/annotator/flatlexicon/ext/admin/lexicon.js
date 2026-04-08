startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

// parse parameters
const parameters = new URLSearchParams(window.location.search);
const lexicon = parameters.get("l") || parameters.get("lexicon");
console.log(`lexicon "${lexicon}"`);

document.getElementById("lexicon").innerText = lexicon;

let fields = [];

function showForm(fields) {
  if (fields.length == 1 && fields[0].FlatLexiconTagger_error) {
    document.getElementById("error").innerText = data[0].FlatLexiconTagger_error;
    return;
  }
  const attributes = document.getElementById("attributes");
  attributes.innerHTML = ""; // clear any existing list
  for (let field of fields) {
    newField(attributes, field);
  } // next field
}

function newField(attributes, field) {
  const row = document.createElement("div");
  row.className = "field";
  row.id = `field-${field.field}`;
  row.title = field.field;
  let label = document.createElement("label");
  label.appendChild(document.createTextNode(field.field));
  row.appendChild(label);
  
  let type = document.createElement("select");
  type.className = "type";
  type.id = `type-${field.field}`;
  const types = [
    "string", "text", "richtext", "integer", "number", "boolean", "geo-location"];
  for (let t of types) {
    const option = document.createElement("option");
    option.appendChild(document.createTextNode(t));
    type.appendChild(option);
  } // next possible type
  type.onkeyup = type.onchange = function(e) {
    // show save button
    document.getElementById(`save-${field.field}`).disabled = false;
  }
  type.value = field.type||"string";
  row.appendChild(type);
  let validation = document.createElement("input");
  validation.type = "text"
  validation.id = `validation-${field.field}`;
  validation.placeholder = "Validation";
  validation.value = field.validation||"";
  validation.onkeyup = validation.onchange = function(e) {
    // show save button
    document.getElementById(`save-${field.field}`).disabled = false;
  }
  row.appendChild(validation);
  let btnSave = document.createElement("button");
  btnSave.id = `save-${field.field}`;
  btnSave.disabled = true;
  let img = document.createElement("img");
  img.src = "../save.svg";
  img.alt= "💾";
  btnSave.appendChild(img);
  btnSave.onclick = function(e) {
    saveField(field.field);
  }
  row.appendChild(btnSave);
  let btnDelete = document.createElement("button");
  btnDelete.id = `delete-${field.field}`;
  img = document.createElement("img");
  img.src = "../delete.svg";
  img.alt= "❌";
  btnDelete.appendChild(img);
  btnDelete.onclick = function(e) {
    deleteField(field.field);
  }
  row.appendChild(btnDelete);
  attributes.appendChild(row);
}

function saveField(field) {
  const type = document.getElementById(`type-${field}`).value;
  const validation = document.getElementById(`validation-${field}`).value;
  startLoading();
  getText(resourceForFunction("updateField", lexicon, field, type, validation), error => {
    document.getElementById("error").innerText = error;
    document.getElementById(`save-${field}`).disabled = true;
    finishedLoading();
  });
}

function deleteField(field) {
  if (confirm(
    `Are you sure you want to delete "${field}" and all data associated with it?\nThis action cannot be undone.`)) {
    startLoading();
    getText(resourceForFunction("deleteField", lexicon, field), error => {
      document.getElementById("error").innerText = error;
      if (!error) {
        document.getElementById(`field-${field}`).remove();
      }
      finishedLoading();
    });
  }
}

// get all fields for entry
getJSON(resourceForFunction("readFields", lexicon), data => {
  startLoading();
  showForm(data);
  finishedLoading();
});

document.getElementById("btnNewField").onclick = function(e) {
  const field = prompt("Enter a name for the new field");
  if (field) {
    startLoading();
    getText(resourceForFunction("createField", lexicon, field),
      error => {
        document.getElementById("error").innerText = error;
        if (!error) {
          newField(attributes, {field: field});
        }
        finishedLoading();
      });
  }
}
