startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

// parse parameters
const parameters = new URLSearchParams(window.location.search);
const table = parameters.get("t") || parameters.get("table");
const parentTable = parameters.get("p") || parameters.get("parent");
if (parentTable) {
  document.getElementById("nav-back").href = `fields.html?t=${parentTable}`;
}
document.getElementById("table").innerText = table;

let fields = [];

function showForm(fields) {
  if (fields.length == 1 && fields[0].FlatTableTagger_error) {
    document.getElementById("error").innerText = data[0].FlatTableTagger_error;
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

  if (field.type == "child-table") {
    
    const type = document.createElement("span");
    type.className = "type";
    type.id = `type-${field.field}`;
    type.appendChild(document.createTextNode(field.type));
    row.appendChild(type);
    
    const link = document.createElement("a");
    link.className = "edit-link";
    link.title = `${field.field} definition`;
    link.href = `fields.html?p=${table}&t=${table}_${field.field}`
    const img = document.createElement("img");
    img.src = "../edit.svg"
    img.alt = "✎";
    link.appendChild(img);
    row.appendChild(link);

  } else { // regular field
    
    let type = document.createElement("select");
    type.className = "type";
    type.id = `type-${field.field}`;
    const types = [
      "string", "text", "html", "integer", "number", "boolean",
      "url", "email", "date", "datetime", "geo-location"];
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

    if (parentTable) { // only child tables can have new rows and thus default values
      let defaultValue = document.createElement("input");
      defaultValue.type = "text"
      defaultValue.id = `default-value-${field.field}`;
      defaultValue.placeholder = "Default Value";
      defaultValue.title = "Default value for new rows, e.g."
        +"\n - a literal fixed value (must be in quotes): \"option1\""
        +"\n - the current date/time: now"
        +"\n - the user's login ID (if any): user";
      defaultValue.value = field.defaultValue||"";
      defaultValue.onkeyup = defaultValue.onchange = function(e) {
        // show save button
        document.getElementById(`save-${field.field}`).disabled = false;
      }
      row.appendChild(defaultValue);
    }
    
    let validation = document.createElement("input");
    validation.type = "text"
    validation.id = `validation-${field.field}`;
    validation.placeholder = "Validation";
    validation.title = "Constraints, e.g."
      +"\n - \"0<10\" for values between 0 and 100 (inclusive)"
      +"\n - \"option1|option2|option3\" for a list of options"
      +"\n - \"[a-zA-Z0-9]*\" for only alphanumeric values";
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
  } // regular field (not child-table)
  
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
  const defaultValue = document.getElementById(`default-value-${field}`)?
        document.getElementById(`default-value-${field}`).value:"";
  const validation = document.getElementById(`validation-${field}`).value;
  startLoading();
  getText(resourceForFunction("updateField", table, field, type, defaultValue, validation),
          error => {
            document.getElementById("error").innerText = error;
            document.getElementById(`save-${field}`).disabled = true;
            finishedLoading();
          });
}

function deleteField(field) {
  if (confirm(
    `Are you sure you want to delete "${field}" and all data associated with it?\nThis action cannot be undone.`)) {
    startLoading();
    getText(resourceForFunction("deleteField", table, field), error => {
      document.getElementById("error").innerText = error;
      if (!error) {
        document.getElementById(`field-${field}`).remove();
      }
      finishedLoading();
    });
  }
}

// get all fields for entry
getJSON(resourceForFunction("readFields", table), data => {
  startLoading();
  showForm(data);
  finishedLoading();
});

document.getElementById("newFieldType").onchange = function(e) {
  if (this.value) { // type selected
    const type = this.value;
    this.value = ""; // unselect type
    const field = prompt(`Enter a name for the new ${type}`);
    if (field) {
      startLoading();
      getText(resourceForFunction("createField", table, field, type),
              error => {
                document.getElementById("error").innerText = error;
                if (!error) {
                  newField(attributes, {field: field, type: type});
                }
                finishedLoading();
              });
    }
  } // type selected
}
