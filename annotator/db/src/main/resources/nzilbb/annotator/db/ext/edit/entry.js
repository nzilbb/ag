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
let sourceTranscript = null;
let sourceAnnotation = null;
if (window.location.hash) { // there is a hash defined
  // it might define the source of link
  // i.e. something like: "#transcript.id%23annotation.id"
  const match = window.location.hash.match(/#(.*)%23(.*)$/);
  if (match) {
    sourceTranscript = match[1];
    sourceAnnotation = match[2];
    console.log(`linked from ${sourceTranscript}#${sourceAnnotation}`);
  }
}

let user = "";
// if we're running in LaBB-CAT, then user information is available via
// http://baseURL/labbcat/api/user
getJSON("../../../../api/user", response => {
  if (response && response.model && response.model.user) {
    user = response.model.user;
  }
});

// replace title with link to read-only page
document.getElementById("entry").innerText = "";
const viewLink = document.createElement("a");
viewLink.href = "../entry.html?t="+encodeURIComponent(table)
  +"&f="+encodeURIComponent(field)
  +"&e="+encodeURIComponent(entry)
  +(window.location.hash||"");
viewLink.appendChild(document.createTextNode(entry));
document.getElementById("entry").appendChild(viewLink);

const tableFields = {}; // definitions for table fields (key is table name)
const model = {}; // all rows (key is table name, value is array of entries for that table)
const btnSave = document.getElementById("btnSave");

function showForm(data) {
  if (data.DbTagger_error) {
    document.getElementById("error").innerHTML = `<p>${data.DbTagger_error}</p>`;
    return;
  }
  model[table] = [data]; // keep a track of the main entry
  const attributes = document.getElementById("attributes");
  attributes.innerHTML = ""; // clear any existing list
  const fields = tableFields[table];
  for (let name in fields) {
    if (fields[name].type == "hidden") continue;
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
          value, fields[name], (e) => {
            // show save button
            btnSave.style.display = null;
            // mark entry as edited
            data._toUpdate = (resolve, reject)=>{
              
              const data = {};
              const fields = tableFields[table];
              for (let name in fields) {
                const input = document.getElementById(`attribute-${name}`);    
                if (input) {      
                  if (name != field) { // not read only
                    if (!input.checkValidity()) { // not valid    
                      input.reportValidity();
                      reject();
                      return;
                    }
                    if (input.editor) {
                      data[name] = input.editor.getData();
                    } else if (input.type == "checkbox") {
                      data[name] = input.checked?input.value:"";
                    } else {
                      data[name] = input.value;
                    }
                  } // not read only
                } // input found
              } // next field
              getJSON(resourceForFunction(
                "updateEntry", table, field, entry, JSON.stringify(data)), newModel => {
                  if (newModel.DbTagger_error) {
                    reject(newModel.DbTagger_error);
                  } else {
                    resolve();
                  }
                });
            }
          }, data[name]);
      }
      row.appendChild(value);
      
      let hint = document.createElement("span");
      hint.id = `hint-${name}`;
      hint.className = "hint";
      hint.appendChild(document.createTextNode(fields[name].hint||""));
      row.appendChild(hint);
      
    } else { // child-table
      row = document.createElement("fieldset");
      label = document.createElement("legend");
      label.appendChild(document.createTextNode(name));
      row.appendChild(label);

      const placeHolder = document.createElement("div");
      placeHolder.className = "loading";
      row.appendChild(placeHolder);
      // get child field definitions
      getJSON(
        resourceForFunction("readChildEntries", table, field, entry, name), data => {
          row.replaceChild(
            showChildForm(1, table, field, ()=>entry, name, data),
            placeHolder);
        }); // readChildEntries
    } // child-table
    row.id = `row-${name}`;
    row.className = "field " + fields[name].type;
    row.title = name + (fields[name].hint?" - " + fields[name].hint:"");
    attributes.appendChild(row);
  } // next field
  // hide save button
  btnSave.style.display = "none";
}

function showChildForm(depth, parentTable, parentField, getParentEntry, childField, data, countId) {
  const table = document.createElement("table");
  const thead = document.createElement("thead");
  let tr = document.createElement("tr");
  let showHeader = false; // if it's just a single field named after the parent-table field
  const fields = tableFields[`${parentTable}_${childField}`];
  for (let name in fields) {
    if (fields[name].type == "hidden") continue;
    const th = document.createElement("th");
    th.className = fields[name].type;
    if (fields[name].hint) th.title = fields[name].hint;
    const span = document.createElement("span");
    span.appendChild(document.createTextNode(name));
    th.appendChild(span);
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
  if (!model[`${parentTable}_${childField}`]) model[`${parentTable}_${childField}`] = [];  
  for (let r in data) { // for each child row
     // keep track of the child entry
    model[`${parentTable}_${childField}`].push(data[r]);
    // add UI for it
    addChildRow(
      depth, parentTable, parentField, getParentEntry, childField, tbody, data[r], countId);
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
    const newEntry = {};
    model[`${parentTable}_${childField}`].push(newEntry);
    console.log(`adding new row to ${parentTable}_${childField}`);
    const firstInput = addChildRow(
      depth, parentTable, parentField, getParentEntry, childField, tbody,
      newEntry, countId);
    if (countId) {
      const count = document.getElementById(countId);
      if (count) {
        count.count++;
        count.innerHTML = `(${count.count})`;
      }
    }
    if (firstInput) firstInput.focus();    
  });
  td.appendChild(btnCreate);
  tr.appendChild(td);
  tfoot.appendChild(tr);
  table.appendChild(tfoot);
  return table;
}

let childRowsId = 0; // for generating unique IDs
function addChildRow(
  depth, parentTable, parentField, getParentEntry, childField, rows, model, countId) {
  let firstInput = null;
  const fields = tableFields[`${parentTable}_${childField}`];
  
  const tr = document.createElement("tr");
  childRowsId++;
  
  const inputs = {};
  function saveEntry(resolve, reject) {
    // load data from inputs into model 
    for (let name in fields) {
      if (inputs[name]) { // (e.g. grandchild-table has no inputs entry)
        if (inputs[name].type == "checkbox") {
          model[name] = inputs[name].checked?"true":"false";
        } else {
          model[name] = inputs[name].value;
        }
      }
    }
    if (!model.serial) { // serial not set - it's a new row
      getJSON(resourceForFunction(
        "createChildEntry", parentTable, parentField, getParentEntry(), childField, JSON.stringify(model)),
              newModel => {
                if (newModel.DbTagger_error
                    // if the error mentions a foreign key constraint it's probably the
                    // 'create child then delete parent' case, so let it pass
                    && !newModel.DbTagger_error.includes("_FK")) {
                  reject(newModel.DbTagger_error);
                } else {
                  Object.assign(model, newModel); // should include serial
                  resolve();
                }
              });
    } else { // serial already set - it's an existing row
      getJSON(resourceForFunction(
        "updateChildEntry", parentTable, parentField, getParentEntry(), childField,
        JSON.stringify(model)),
              newModel => {
                if (newModel.DbTagger_error) {
                  reject(newModel.DbTagger_error);
                } else {
                  Object.assign(model, newModel); // should include serial
                  resolve();
                }
              });
    }
  };
  if (!model.serial) { // it's a new entry, so save it even if there are no further changes
    btnSave.style.display = null;
    model._toUpdate = saveEntry;    
  }
  
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
      btnSave.style.display = null;
      model._toDelete = (resolve, reject)=>{
        if (model.serial) { // serial set - it's not a new row
          getJSON(resourceForFunction(
            "deleteChildEntry", parentTable, parentField, getParentEntry(), childField, JSON.stringify(model)),
                  error => {
                    finishedLoading();
                    if (error) {
                      reject(error);
                    } else {
                      resolve();
                    }
                  }); // deleteChildEntry
        } else { // added then deleted before saving
          resolve();
        }
      }; // _toDelete
      tr.remove();
      if (countId) { // update summary count
        const count = document.getElementById(countId);
        if (count) {
          count.count--;
          count.innerHTML = `(${count.count})`;
        }
      }
    } // yes I'm sure
  });
  tdDelete.appendChild(btnDelete);
  
  for (let name in fields) {
    const td = document.createElement("td");
    td.title = name;
    td.className = name + " " + fields[name].type;
    
    // determine the initial value
    let value = model[name] || "";
    if (!value && !model.serial) { // serial not set - it's a new row
      const defaultValue = fields[name].defaultValue;
      if (defaultValue) {
        if (/^".+"$/.test(defaultValue)) { // a literal
          value = defaultValue.slice(1,-1); // strip quotes
        } else if (defaultValue.toLowerCase() == "now") { // current time
          const now = new Date(); // this is in UTC
          now.setMinutes(now.getMinutes() - now.getTimezoneOffset()); // use this timezone
          if (fields[name].type == "date") {
            value = now.toISOString().slice(0,10); // date part only
          } else if (fields[name].type == "datetime") {
            value = now.toISOString().slice(0,16); // date to minutes only
          } else {
            value = now.toLocaleString();
          }
        } else if (defaultValue.toLowerCase() == "user") { // current user
          // we may or may not have this
          value = user;
        }
      }
    }
    
    if (fields[name].type != "child-table") { // regular field
      // create the UI component for the field value
      const input = createFieldInput(
        td, fields[name], (e)=> {
          btnSave.style.display = null;
          model._toUpdate = saveEntry; 
        }, value);
      if (fields[name].hint) input.title = fields[name].hint;
      // ensure the save function can access the input
      inputs[name] = input;
      if (!firstInput) firstInput = input;
    } else { // (grand)child-table
      const childTable = `${parentTable}_${childField}_${name}`;
      const countId = `count-${childTable}-${childRowsId}`;

      const placeHolder = document.createElement("div");
      placeHolder.className = "loading";
      td.appendChild(placeHolder);

      // get child field definitions      
      getJSON(
        resourceForFunction(
          "readChildEntries",
          `${parentTable}_${childField}`, "serial", model.serial||"", name),
        data => {
          const childForm = showChildForm(
            depth+1, `${parentTable}_${childField}`, "serial", ()=>model.serial||"",
            name, data, countId);
          let details = null;
          if (depth > 1) { // deeper than grandchild table
            // to avoid clutter, hide under a summary
            details = document.createElement("inline-details");
            const label = document.createElement("inline-summary");
            label.id = countId;
            label.onclick = ()=>{ details.toggleAttribute("open"); };
            details.appendChild(label);
            details.appendChild(childForm);
            td.replaceChild(details, placeHolder);
          } else {
            td.replaceChild(childForm, placeHolder);
          }
          if (depth > 0) { // deeper than grandchild table
            const count = document.getElementById(countId);
            if (count) {
              count.count = data.length; // add/delete causes updates later...
              count.innerHTML = `(${data.length})`;
            }
            if (details && data.length <= 4) {
              details.setAttribute("open", true);
            }
          }
        }); // readChildEntries
    } // (grand)child-table
    
    tr.appendChild(td);
  } // next field
  
  // add save button at the end
  tr.appendChild(tdDelete);
  rows.appendChild(tr);
  
  return firstInput; // in case we want to focus on it
}

let datalistId = 0; // for unique IDs
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
  } else if (fieldDefinition.type == "transcript-link") { // a link back to a source document
    const a = document.createElement("a");
    a.className = "transcript-link";
    a.title = `Open ${fieldDefinition.field}`;
    a.target = fieldDefinition.field;
    a.href = "#";
    a.addEventListener("click", function(e) {
      if (input.value.startsWith("http")) { // plain link
        this.href = input.value;
        return true;
      } else if (input.value.indexOf("#") > 0) { // can link to transcript
        // turn something like:
        // http://example.com/labbcat/annotator/ext/DbTagger/entry.html?t=x&f=x&e=x
        // into something like
        // http://example.com/labbcat/transcript?id=transcriptId#annotationId
        this.href = window.location.toString()
          .replace(/annotator\/ext\/DbTagger\/.*$/,"transcript?id=")
          +input.value;
        a.target = input.value.split("#")[0]; // target is transcript ID
        return true;
      } else { 
        if (!input.value // no value specified
            && sourceTranscript && sourceAnnotation) { // there is a current source
          // set the value with the current source
          input.value = `${sourceTranscript}#${sourceAnnotation}`;
          if (saveButtonHandler) saveButtonHandler();
        }
        e.preventDefault();
        return false;
      }
    });
    a.appendChild(document.createTextNode("🖹"));
    valueElement.appendChild(a);
  } else if (fieldDefinition.type == "cross-reference") { // a link to another entry
    const a = document.createElement("a");
    a.className = "cross-reference";
    a.title = `Open ${fieldDefinition.field}`;
    a.href = "#";
    a.addEventListener("click", function(e) {
      if (input.value) {
        this.href = document.URL.replace(/\?.*/,"")
          + "?t="+encodeURIComponent(table)
          + "&f="+encodeURIComponent(fieldDefinition.validation || field)
          + "&e="+encodeURIComponent(input.value)
        return true;
      } else { 
        e.preventDefault();
        return false;
      }
    });
    a.appendChild(document.createTextNode("↗"));
    valueElement.appendChild(a);

    // lookup as user types
    datalistId++;
    const datalist = document.createElement("datalist");
    datalist.id = `matches-${fieldDefinition.field}-${datalistId}`;
    valueElement.appendChild(datalist);
    const lookup = function() { // ask the server for some matching options
      if (input.value) {
        getJSON(resourceForFunction(
          "matches", table, fieldDefinition.validation || field, input.value), values => {
          if (values.length == 1 && values[0].startsWith("DbTagger.")) { // error
            document.getElementById("error").innerHTML = values[0];
            // clear the error after a short delay
            window.setTimeout(()=>document.getElementById("error").innerText = "", 3000);
          } else {
            datalist.innerHTML = ""; // clear any previous values
            values.forEach((value)=>{ // add each option to the datalist
              const option = document.createElement("option");
              option.value = value;
              datalist.appendChild(option);
            });
            if (values.includes(input.value)) {
              input.setCustomValidity("");
            } else {
              input.setCustomValidity("Please select a valid option from the list");
            }
          }
        });
      } // there is a value set
    }
    let deferLookup = null;
    input.setAttribute("list", datalist.id);
    input.addEventListener("input", function(e) {
      if (deferLookup) window.clearTimeout(deferLookup);
      deferLookup = window.setTimeout(lookup, 1000);
    });
    
  } else if (fieldDefinition.type == "email") {
    input.type = "email";
  } else if (fieldDefinition.type == "date") {
    input.type = "date";
  } else if (fieldDefinition.type == "datetime") {
    input.type = "datetime-local";
  } else if (fieldDefinition.type == "boolean") {
    input.type = "checkbox";
    input.value = fieldDefinition.validation||"true";
  } else if (fieldDefinition.type == "text") {
    input = document.createElement("textarea");
  } else if (fieldDefinition.type == "html") { 
    input = document.createElement("textarea"); // attach the editor later...
  }
  if (fieldDefinition.validation
      // except validation is repurposed for cross-references/boolean:
      && fieldDefinition.type != "cross-reference"
      && fieldDefinition.type != "boolean") {
    const lookupPattern = /^lookup:(.*)\.(.*)$/;
    // if it's just a list of alternative, e.g. "option1|option2|option3"
    if (/^\|?(.+\|)+.+/.test(fieldDefinition.validation)) { // implement as a select instead
      let optionsList = null;
      if (fieldDefinition.validation.endsWith("|*")) { // an open set of options
        // so the list of options is a suggestion but they can enter anything
        datalistId++;
        optionsList = document.createElement("datalist");
        optionsList.id = `matches-${fieldDefinition.field}-${datalistId}`;
        valueElement.appendChild(optionsList);
        input.setAttribute("list", optionsList.id);
      } else { // a closed set of options
        input = document.createElement("select");
        optionsList = input;
      }
      for (let opt of fieldDefinition.validation.split("|")) {
        if (optionsList.tagName.toLowerCase() == "datalist"
            && (opt == "*" || opt == "")) {
          // datalist ignores these options
          continue;
        }
        const option = document.createElement("option");
        option.appendChild(document.createTextNode(opt));
        optionsList.appendChild(option);
      } // next option
    } else if (lookupPattern.test(fieldDefinition.validation)) { // lookup other table

      const groups = lookupPattern.exec(fieldDefinition.validation);
      const lookupTable = groups[1];
      const lookupField = groups[2];
      
      // lookup as user types
      datalistId++;
      const datalist = document.createElement("datalist");
      datalist.id = `matches-${fieldDefinition.field}-${datalistId}`;
      datalist.optionValues = [];
      valueElement.appendChild(datalist);
      const lookup = function() { // ask the server for some matching options
        getJSON(resourceForFunction(
          "matches", lookupTable, lookupField, input.value||".*"), values => {
            if (values.length == 1 && values[0].startsWith("DbTagger.")) { // error
              document.getElementById("error").innerHTML = values[0];
              // clear the error after a short delay
              window.setTimeout(()=>document.getElementById("error").innerHTML = "",3000);
            } else {
              datalist.innerHTML = ""; // clear any previous values
              values.forEach((value)=>{ // add each option to the datalist
                const option = document.createElement("option");
                option.value = value;
                datalist.appendChild(option);
              });
              datalist.optionValues = values;
            }
          });
      }
      let deferLookup = null;
      input.setAttribute("list", datalist.id);
      input.addEventListener("input", function(e) {
        if (deferLookup) window.clearTimeout(deferLookup);
        // did it change because they selected one of the items?
        if (!datalist.optionValues.includes(input.value)) {
          deferLookup = window.setTimeout(lookup, 1000);
        }
      });
      if (!value) { // no value yet
        // lookup all values now
        lookup();
      }
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
    input.checked = value == (fieldDefinition.validation||"true");
  } else {
    input.value = value || "";
  }
  // show save button:
  input.addEventListener("input", saveButtonHandler);

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

function readFields(tableName) {
  return new Promise((resolve, reject)=>{
    getJSON(resourceForFunction("readFields", tableName), definitions => {
      
      // build a dictionary of definitions
      const fields = {};
      const childTablePromises = [];
      for (let definition of definitions) {
        if (definition.field) {
          fields[definition.field] = definition;
          if (fields[definition.field].type == "child-table") {
            // get the child table's definition
            childTablePromises.push(
              readFields(`${tableName}_${definition.field}`));
          }
        }
      } // next field definition
      tableFields[tableName] = fields;
      
      // resolve only after all child tables are resolved
      Promise.all(childTablePromises).then(()=>{
        resolve();
      });
    });
  });
} // readFields
                   
readFields(table).then(()=>{
  // get all field values for entry
  getJSON(resourceForFunction("readEntry", table, field, entry), data => {
    startLoading();
    showForm(data);
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

// tables must be saved in order, top down,
// so that parent rows exist before children are created, etc.
// so we have a queue of table name which we process one at a time
let tablesToSave = [];
function saveNextTable() {
  startLoading();
  const savingTable = tablesToSave.shift();
  console.log(`Saving table ${savingTable}`);
  const savePromises = [];
  for (let entry of model[savingTable]) {
    // is there an update required?
    let doUpdate = entry._toDelete || entry._toUpdate;
    if (doUpdate) {
      const promiseToUpdate = new Promise(doUpdate);
      savePromises.push(promiseToUpdate);
      promiseToUpdate.then(()=>{ // on success
        // unset the function entries
        delete entry._toDelete;
        delete entry._toUpdate;
      }, (error)=>{
        if (error) {
          document.getElementById("error").innerHTML += `<p>${error}</p>`;
        }
      });
    }
  } // next entry
  // once they're all finished saving...
  Promise.all(savePromises).then(()=>{
    if (tablesToSave.length > 0) { // there are more to save
      saveNextTable();
    } else { // finished saving
      finishedLoading();
      // re-enable save button
      btnSave.removeAttribute("disabled");
      // hide save button
      btnSave.style.display = "none";
    }
  }, (error)=>{ // a failure of some kind
    finishedLoading();
    // re-enable save button
    btnSave.removeAttribute("disabled");
    // leave the save button visible, maybe they can fix it
  });
}

btnSave.addEventListener("click", function (e) {
  document.getElementById("error").innerHTML = "";
  tablesToSave = Object.keys(model);
  // disable save button while saving
  btnSave.setAttribute("disabled", true);
  saveNextTable();
});

