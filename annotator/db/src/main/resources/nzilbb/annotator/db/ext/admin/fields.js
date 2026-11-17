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
    
    const link = document.createElement("a");
    link.className = "edit-link";
    link.title = `${field.field} definition`;
    link.href = `fields.html?p=${table}&t=${table}_${field.field}`
    let img = document.createElement("img");
    img.src = "../edit.svg"
    img.alt = "✎";
    link.appendChild(img);
    row.appendChild(link);

    const type = document.createElement("span");
    type.className = "type";
    type.id = `type-${field.field}`;
    type.appendChild(document.createTextNode(field.type));
    row.appendChild(type);
    
    const orderLabel = document.createElement("label");
    orderLabel.className = "order-label";
    orderLabel.id = `order-label-${field.field}`;
    orderLabel.appendChild(document.createTextNode("Order of rows"));
    row.appendChild(orderLabel);
    const order = document.createElement("input");
    order.className = "order";
    order.id = `order-${field.field}`;
    orderLabel.setAttribute("for", order.id);
    order.placeholder = "Order rows were entered";
    order.title = "The order child table rows are listed";
    order.value = field.validation||"";
    order.addEventListener("input", function(e) {
      // show save button
      document.getElementById(`save-${field.field}`).disabled = false;
    });
    row.appendChild(order);
    
  } else { // regular field
    
    let type = document.createElement("select");
    type.className = "type";
    type.id = `type-${field.field}`;
    const types = [
      "string", "text", "html", "integer", "number", "boolean",
      "url", "email", "date", "datetime", "geo-location",
      "transcript-link", "cross-reference", "hidden"];
    for (let t of types) {
      const option = document.createElement("option");
      option.appendChild(document.createTextNode(t));
      type.appendChild(option);
    } // next possible type
    type.addEventListener("input", function(e) {
      // show save button
      document.getElementById(`save-${field.field}`).disabled = false;
    });
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
      defaultValue.addEventListener("input", function(e) {
        // show save button
        document.getElementById(`save-${field.field}`).disabled = false;
      });
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
    validation.addEventListener("input", function(e) {
      // show save button
      document.getElementById(`save-${field.field}`).disabled = false;
    });
    row.appendChild(validation);
    
    let hint = document.createElement("input");
    hint.type = "text"
    hint.id = `hint-${field.field}`;
    hint.placeholder = "Hint";
    hint.title = "Information to help users understand how to use the field.";
    hint.value = field.hint||"";
    hint.addEventListener("input", function(e) {
      // show save button
      document.getElementById(`save-${field.field}`).disabled = false;
    });
    row.appendChild(hint);
    
  } // regular field (not child-table)
  
  let btnSave = document.createElement("button");
  btnSave.id = `save-${field.field}`;
  btnSave.className = "save";
  btnSave.title = `Save changes to ${field.field}`;
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
  btnDelete.className = "delete";
  btnDelete.title = `Delete ${field.field}`;
  img = document.createElement("img");
  img.src = "../delete.svg";
  img.alt= "❌";
  btnDelete.appendChild(img);
  btnDelete.onclick = function(e) {
    deleteField(field.field);
  }
  row.appendChild(btnDelete);
  
  let btnMoveUp = document.createElement("button");
  btnMoveUp.id = `move-${field.field}`;
  btnMoveUp.className = "move-up";
  btnMoveUp.title = `Move ${field.field} up`;
  img = document.createElement("img");
  img.src = "../up.svg";
  img.alt= "↑";
  btnMoveUp.appendChild(img);
  btnMoveUp.onclick = function(e) {
    moveFieldUp(field.field);
  }
  row.appendChild(btnMoveUp);
  
  attributes.appendChild(row);
}

function saveField(field) {
  const typeSelect = document.getElementById(`type-${field}`);
  if (typeSelect && typeSelect.tagName == "select") { // ordinary field
    const type = typeSelect.value;
    const defaultValue = document.getElementById(`default-value-${field}`)?
          document.getElementById(`default-value-${field}`).value:"";
    const validation = document.getElementById(`validation-${field}`).value;
    const hint = document.getElementById(`hint-${field}`).value;
    startLoading();
    getText(resourceForFunction(
      "updateField", table, field, defaultValue, validation, hint, type),
            error => {
              document.getElementById("error").innerText = error;
              document.getElementById(`save-${field}`).disabled = true;
              finishedLoading();
            });
  } else { // child-table
    const order = document.getElementById(`order-${field}`).value;
    startLoading();
    getText(resourceForFunction(
      "updateField", table, field, "", order, "", "child-table"),
            error => {
              document.getElementById("error").innerText = error;
              document.getElementById(`save-${field}`).disabled = true;
              finishedLoading();
            });
  }
}

function moveFieldUp(field) {
  startLoading();
  getText(resourceForFunction("moveFieldUp", table, field),
          error => {
            document.getElementById("error").innerText = error;
            finishedLoading();
            if (!error) { // move successful
              // move it in the document
              const moveUp = document.getElementById(`field-${field}`);
              moveUp.parentElement.moveBefore(moveUp, moveUp.previousElementSibling);
            }
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
      getText(resourceForFunction("createField", table, field, "", "", "", type),
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
