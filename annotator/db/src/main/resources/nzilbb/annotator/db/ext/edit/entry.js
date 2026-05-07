import {
  ClassicEditor, Essentials,
  Paragraph, Heading, Bold, Italic, Underline, BlockQuote, Font, Link, AutoLink,
  HorizontalLine, List, Table, TableToolbar, Indent, IndentBlock 
} from 'ckeditor5';

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

// replace title with link to read-only page
document.getElementById("entry").innerText = "";
const viewLink = document.createElement("a");
viewLink.href = "../entry.html?t="+encodeURIComponent(table)
  +"&f="+encodeURIComponent(field)
  +"&e="+encodeURIComponent(entry);
viewLink.appendChild(document.createTextNode(entry));
document.getElementById("entry").appendChild(viewLink);

const fields = {}; // definitions for fields (type/validation etc.)
const childFields = {}; // definitions for child-table fields (key is child name)

function showForm(data) {
  if (data.DbTagger_error) {
    document.getElementById("error").innerText = data.DbTagger_error;
    return;
  }
  const attributes = document.getElementById("attributes");
  attributes.innerHTML = ""; // clear any existing list
  for (let name in fields) {
    let row = document.createElement("div");
    let label = document.createElement("label");
    label.appendChild(document.createTextNode(name));
    row.appendChild(label);
    if (fields[name].type != "child-table") { // regular field
      
      let value = document.createElement("span");
      value.id = `value-${name}`;
      value.className = "value";
      if (name == field) { // the ID is read-only
        let span = document.createElement("span");
        span.className = "read-only";
        span.id = `attribute-${name}`;
        span.appendChild(document.createTextNode(data[name]));
        value.appendChild(span);
      } else {
        createFieldInput(
          value, fields[name], (e)=>document.getElementById("btnSave").style.display = null,
          data[name]);
      }
      row.appendChild(value);
    } else { // child-table
      row = document.createElement("fieldset");
      label = document.createElement("legend");
      label.appendChild(document.createTextNode(name));
      row.appendChild(label);
      
      // get child field definitions
      const childTable = `${table}_${name}`;
      getJSON(resourceForFunction("readFields", childTable), definitions => {
        childFields[name] = {};
        // build a dictionary of definitions
        for (let definition of definitions) {
          if (definition.field) {
            childFields[name][definition.field] = definition;
          }
        } // next child field definition

        // get all field values for entry
        startLoading();
        getJSON(
          resourceForFunction("readChildEntries", table, field, entry, name), data => {
            finishedLoading();
            showChildForm(name, data);
          }); // readChildEntries
        
      }); // readFields
    }
    row.id = `row-${name}`;
    row.className = "field " + fields[name].type;
    row.title = name;
    attributes.appendChild(row);
  } // next field
  // hide save button
  document.getElementById("btnSave").style.display = "none";
}

function showChildForm(childField, data) {
  const value = document.getElementById(`row-${childField}`);
  const table = document.createElement("table");
  const thead = document.createElement("thead");
  let tr = document.createElement("tr");
  let showHeader = false; // if it's just a single field named after the parent-table field
  for (let name in childFields[childField]) {
    const th = document.createElement("th");
    th.appendChild(document.createTextNode(name));
    tr.appendChild(th);
    if (name != childField) showHeader = true;
  } // next field
  // add columns for save/delete buttons
  const thSave = document.createElement("th");
  thSave.className = "save-column";
  tr.appendChild(thSave);
  const thDelete = document.createElement("th");
  thDelete.className = "delete-column";
  tr.appendChild(thDelete);
  if (showHeader) {
    thead.appendChild(tr);
    table.appendChild(thead);
  }
  
  const tbody = document.createElement("tbody");
  tbody.id = `rows-${childField}`;
  for (let r in data) { // for each child row
    addChildRow(childField, tbody, data[r])
  } // next child row
  
  table.appendChild(tbody);
  const tfoot  = document.createElement("tfoot");
  tr = document.createElement("tr");
  let td = document.createElement("td");
  const btnCreate = document.createElement("button");
  const img = document.createElement("img");
  img.src = "../add.svg";
  img.alt= "➕";
  btnCreate.appendChild(img);
  btnCreate.addEventListener("click", function(e) {
    const firstInput = addChildRow(childField, tbody, {});
    if (firstInput) firstInput.focus();
  });
  td.appendChild(btnCreate);
  tr.appendChild(td);
  tfoot.appendChild(tr);
  table.appendChild(tfoot);
  value.appendChild(table);
}

function addChildRow(childField, rows, model) {
  let firstInput = null;
  
  const tr = document.createElement("tr");
  
  const tdSave = document.createElement("td");
  tdSave.className = "save-column";
  const btnSave = document.createElement("button");
  btnSave.className = "save";
  const img = document.createElement("img");
  const inputs = {};
  img.src = "../save.svg";
  img.alt= "💾";
  btnSave.appendChild(img);
  btnSave.style.display = "none"; // hidden to start with
  btnSave.addEventListener("click", function(e) {    
    // load data from inputs into model 
    for (let name in childFields[childField]) {
      if (inputs[name].type == "checkbox") {
        model[name] = inputs[name].checked?"true":"false";
      } else {
        model[name] = inputs[name].value;
      }
    }
    if (!model.serial) { // serial not set - it's a new row
      startLoading();
      getJSON(resourceForFunction(
        "createChildEntry", table, field, entry, childField, JSON.stringify(model)),
              newModel => {
                finishedLoading();
                if (newModel.DbTagger_error) {
                  document.getElementById("error").innerText = newModel.DbTagger_error;
                } else {
                  Object.assign(model, newModel); // should include serial
                  btnSave.style.display = "none"; // hide button again
                }
              });
    } else { // serial already set - it's an existing row
      startLoading();
      getJSON(resourceForFunction(
        "updateChildEntry", table, field, entry, childField, JSON.stringify(model)),
              newModel => {
                finishedLoading();
                if (newModel.DbTagger_error) {
                  document.getElementById("error").innerText = newModel.DbTagger_error;
                } else {
                  Object.assign(model, newModel); // should include serial
                  btnSave.style.display = "none"; // hide button again
                }
              });
    }
  });
  tdSave.appendChild(btnSave);
  
  const tdDelete = document.createElement("td");
  tdDelete.className = "delete-column";
  const btnDelete = document.createElement("button");
  const imgDelete = document.createElement("img");
  imgDelete.src = "../delete.svg";
  imgDelete.alt= "❌";
  btnDelete.appendChild(imgDelete);
  btnDelete.addEventListener("click", function(e) {
    if (confirm(
      `Are you sure you want to delete this ${childField}?\nThis cannot be undone.`)) {
      if (model.serial) { // serial not set - it's a new row
        startLoading();
        getJSON(resourceForFunction(
          "deleteChildEntry", table, field, entry, childField, JSON.stringify(model)),
                error => {
                  finishedLoading();
                  if (error) {
                    document.getElementById("error").innerText = error;
                  } else {
                    // remove the row from the table
                    tr.remove();
                    // esnure UI has time to remove the row
                    setTimeout(()=>alert(`${childField} deleted.`), 100);
                  }
                });
      } else { // a new row
        tr.remove();
        // esnure UI has time to remove the row
        setTimeout(()=>alert(`${childField} deleted.`), 100);
      }
    } // yes I'm sure
  });
  tdDelete.appendChild(btnDelete);
  
  for (let name in childFields[childField]) {
    const td = document.createElement("td");
    td.title = name;
    td.className = name;
    
    // create the UI component for the field value
    const input = createFieldInput(
      td, childFields[childField][name], (e)=>btnSave.style.display = null,
      model[name] || "");
    // ensure the save function can access the input
    inputs[name] = input;
    if (!firstInput) firstInput = input;
    
    tr.appendChild(td);
  } // next field

  // add save button at the end
  tr.appendChild(tdSave);
  tr.appendChild(tdDelete);
  rows.appendChild(tr);

  return firstInput; // in case we wat to focus on it
}

function createFieldInput(valueElement, fieldDefinition, saveButtonHandler, value) {
  let input = document.createElement("input");
  
  // default text input
  input.type = "text";
  // refine type constraints
  if (fieldDefinition.type == "integer") {
    input.type = "number";
    input.step = 1;
  } else if (fieldDefinition.type == "number") {
    input.type = "number";
    input.step = 0.1;
  } else if (fieldDefinition.type == "url") {
    input.type = "url";
    const a = document.createElement("a");
    a.className = "url-link";
    a.title = `Open ${fieldDefinition.field}`;
    a.target = fieldDefinition.field;
    a.href = "#";
    a.addEventListener("click", function(e) {
      if (input.value) {
        this.href = input.value;
        return true;
      } else {
        e.preventDefault();
        return false;
      }
    });
    a.appendChild(document.createTextNode("🡽"));
    valueElement.appendChild(a);
  } else if (fieldDefinition.type == "geo-location") {
    const a = document.createElement("a");
    a.className = "geo-link";
    a.title = `Open ${fieldDefinition.field}`;
    a.target = fieldDefinition.field;
    a.href = "#";
    a.addEventListener("click", function(e) {
      if (input.value) {
        let coords = input.value.split(",");
        if (input.value.indexOf(":") >= 0) { // shape
          // something like:
          // -43.607048, 172.467246 : -43.626186, 172.552390 : -43.681332, 172.552390
          
          // go to the point in the middle
          const shape = input.value.split(":")
                .map(coord=>coord.trim().split(",")
                     .map(s=>Number(s)));
          const latitudes = shape.map(coord=>coord[0]);
          const longitudes = shape.map(coord=>coord[1]);
          coords = [
            latitudes.reduce((a, b) => a + b) / latitudes.length,
            longitudes.reduce((a, b) => a + b) / longitudes.length
          ];
        } 
        this.href = `https://www.openstreetmap.org/?mlat=${coords[0]}&mlon=${coords[1]}#map=16/${coords[0]}/${coords[1]}`
        return true;
      } else {
        e.preventDefault();
        return false;
      }
    });
    a.appendChild(document.createTextNode("🌏"));
    valueElement.appendChild(a);
  } else if (fieldDefinition.type == "email") {
    input.type = "email";
  } else if (fieldDefinition.type == "date") {
    input.type = "date";
  } else if (fieldDefinition.type == "datetime") {
    input.type = "datetime-local";
  } else if (fieldDefinition.type == "boolean" && !fieldDefinition.validation) {
    input.type = "checkbox";
    input.value = "true";
  } else if (fieldDefinition.type == "text") {
    input = document.createElement("textarea");
  } else if (fieldDefinition.type == "html") { 
    input = document.createElement("textarea"); // attach the editor later...
  }
  if (fieldDefinition.validation) {
    // if it's just a list of alternative, e.g. "option1|option2|option3"
    if (/^\|?(\w+\|)+\w+/.test(fieldDefinition.validation)) { // implement as a select instead
      input = document.createElement("select");
      for (let opt of fieldDefinition.validation.split("|")) {
        const option = document.createElement("option");
        option.appendChild(document.createTextNode(opt));
        input.appendChild(option);
      } // next option
    } else if (fieldDefinition.validation.split("<").length == 2) { // min<max
      const interval = fieldDefinition.validation.split("<");
      if (interval[0]) input.min = interval[0];
      if (interval[1]) input.max = interval[1];
    } else {
      input.pattern = fieldDefinition.validation;
    }
  }
  
  input.name = fieldDefinition.field;
  input.id = `attribute-${fieldDefinition.field}`;
  input.placeholder = fieldDefinition.field;
  if (fieldDefinition.type == "boolean") { // it's a checkbox
    input.checked = value == "true";
  } else {
    input.value = value || "";
  }
  // show save button:
  input.addEventListener("keyup", saveButtonHandler);
  input.addEventListener("change", saveButtonHandler);

  valueElement.appendChild(input);
  
  if (fieldDefinition.type == "html") {
    ClassicEditor
      .create( {
	attachTo: input,
        licenseKey: 'GPL',
        plugins: [
          Essentials,
          Paragraph, Heading, Bold, Italic, Underline, BlockQuote, Font, Link, AutoLink,
          HorizontalLine, List, Table, TableToolbar, Indent, IndentBlock  ],
        toolbar: [
	  // 'undo', 'redo', '|',
          'bold', 'italic', 'underline', '|',
          'outdent', 'indent', 'bulletedList', 'numberedList', 'blockquote',
	  // 'fontSize', 'fontFamily', 'fontColor', '|',
          'link', 'insertTable', 'horizontalLine'
	],
        table: {
	  contentToolbar: [
	    'tableColumn',
	    'tableRow',
	    'mergeTableCells'
	  ]
        },
	licenseKey: 'GPL'
      } )
      .then( editor => {
	input.editor = editor;
        editor.model.document.on('change:data', saveButtonHandler);
      } )
      .catch( error => {
	console.error( error );
      });
  } // type == html
  return input;
}

// get field definitions
getJSON(resourceForFunction("readFields", table), definitions => {

  // build a dictionary of definitions
  for (let definition of definitions) {
    if (definition.field) {
      fields[definition.field] = definition;
    }
  } // next field definition

  // get all field values for entry
  getJSON(resourceForFunction("readEntry", table, field, entry), data => {
    startLoading();
    showForm(data);
    finishedLoading();
  });

});

document.getElementById("btnSave").addEventListener("click", function (e) {
  document.getElementById("error").innerText = "";
  var fd = new FormData();
  fd.append("t", table);
  fd.append("f", field);
  fd.append("e", entry);
  const data = {};
  for (let name in fields) {
    const input = document.getElementById(`attribute-${name}`);    
    if (input) {      
      if (name != field) { // not read only
        if (!input.checkValidity()) { // not valid    
          input.reportValidity();
          return;
        }
        if (input.editor) {
          data[name] = input.editor.getData();
        } else if (input.type == "checkbox") {
          data[name] = input.checked?"true":"false";
        } else {
          data[name] = input.value;
        }
      } // not read only
    } // input found
  } // next field
  fd.append("data", JSON.stringify(data));
  startLoading();
  postForm("updateEntry", fd, function(e) {
    showForm(JSON.parse(this.responseText));
    finishedLoading();
  });
});

window.addEventListener("beforeunload", (event) => { // before they navigate away
  // are there any save buttons visible?
  for (let btnSave of document.getElementsByClassName("save")) {
    if (btnSave.style.display != "none") {
      event.preventDefault();
      event.returnValue = true;
      return;
    } // button currently displayed
  } // next save button
});
