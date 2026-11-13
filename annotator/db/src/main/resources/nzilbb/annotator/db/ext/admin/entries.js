startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

// parse parameters
const parameters = new URLSearchParams(window.location.search);
const t = parameters.get("t") || parameters.get("table");

let user = "";
// if we're running in LaBB-CAT, then user information is available via
// http://baseURL/labbcat/api/user
getJSON("../../../../api/user", response => {
  if (response && response.model && response.model.user) {
    user = response.model.user;
  }
});

// replace title with link to read-only page
document.getElementById("entries").innerText = `Entries for ${t}`;

const tableFields = {}; // definitions for table fields (key is table name)

let deferListing = null;
let filterInput = null;
let maxRows = 10;
function listing(input) { // ask the server for some matching rows
  filterInput = input;
  maxRows = parseInt(document.getElementById("max-rows").value);
  startLoading();
  getJSON(resourceForFunction(
    "readEntries", t, input.name, input.value||".*", maxRows), values => {
      if (values.length == 1 && values[0]["DbTagger_error"]) { // error
        document.getElementById("error").innerText = values[0]["DbTagger_error"];
        // clear the error after a short delay
        window.setTimeout(()=>document.getElementById("error").innerText = "", 3000);
      } else { // rows returned
        
        // clear any previous data
        document.getElementById("rows").innerHTML = "";
        
        // list the rows
        document.getElementById("rows").appendChild(
          showForm(0, t, "serial", ()=>t, "", values, "row-count"));
      }
      finishedLoading();
    });
} // listing

let childRowsId = 0; // for generating unique IDs
function addRow(
  depth, parentTable, parentField, getParentEntry, childField, rows, model, countId) {
  let firstInput = null;
  const fields = tableFields[childField?`${parentTable}_${childField}`:parentTable];
  
  const tr = document.createElement("tr");
  childRowsId++;
  
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
      startLoading();
      if (!childField) { // top level table
        getJSON(resourceForFunction(
          "createEntry",
          parentTable, JSON.stringify(model)),
                newModel => {
                  finishedLoading();
                  if (newModel.DbTagger_error) {
                    document.getElementById("error").innerText = newModel.DbTagger_error;
                  } else {
                    Object.assign(model, newModel); // should include serial
                    btnSave.style.display = "none"; // hide button again
                  }
                });
      } else { // child table
        getJSON(resourceForFunction(
          "createChildEntry",
          parentTable, parentField, getParentEntry(), childField, JSON.stringify(model)),
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
    } else { // serial already set - it's an existing row
      startLoading();
      if (!childField) { // top level table
        getJSON(resourceForFunction(
          "updateEntry", t, "serial", model.serial, JSON.stringify(model)),
                newModel => {
                  finishedLoading();
                  if (newModel.DbTagger_error) {
                    document.getElementById("error").innerText = data.DbTagger_error;
                    return;
                  } else {
                    Object.assign(model, newModel); // should include serial
                    btnSave.style.display = "none"; // hide button again
                  }
                });
      } else { // child table
        getJSON(resourceForFunction(
          "updateChildEntry", parentTable, parentField, getParentEntry(), childField, JSON.stringify(model)),
                newModel => {
                  finishedLoading();
                  if (newModel.DbTagger_error) {
                    document.getElementById("error").innerText = newModel.DbTagger_error;
                  } else {
                    Object.assign(model, newModel); // should include serial
                    btnSave.style.display = "none"; // hide button again
                  }
                });
      } // child table
    } // saving (serial set)
  });
  tdSave.appendChild(btnSave);
  
  const tdDelete = document.createElement("td");
  tdDelete.className = "delete-column";
  const btnDelete = document.createElement("button");
  const imgDelete = document.createElement("img");
  imgDelete.src = "../delete.svg";
  imgDelete.alt= "❌";
  btnDelete.appendChild(imgDelete);
  if (childField) { // child table
    btnDelete.addEventListener("click", function(e) {
      if (confirm(
        `Are you sure you want to delete this ${childField}?\nThis cannot be undone.`)) {
        if (model.serial) { // serial set - it's not a new row
          startLoading();
          getJSON(resourceForFunction(
            "deleteChildEntry", parentTable, parentField, getParentEntry(), childField, JSON.stringify(model)),
                  error => {
                    finishedLoading();
                    if (error) {
                      document.getElementById("error").innerText = error;
                    } else {
                      // remove the row from the table
                      tr.remove();
                      // ensure UI has time to remove the row
                      setTimeout(()=>alert(`${childField} deleted.`), 100);
                      if (countId) { // update summary count
                        const count = document.getElementById(countId);
                        if (count) {
                          count.count--;
                          count.innerHTML = `(${count.count})`;
                        }
                      }                    
                    }
                  });
        } else { // a new row
          tr.remove();
          // esnure UI has time to remove the row
          setTimeout(()=>alert(`${childField} deleted.`), 100);
          if (countId) { // update summary count
            const count = document.getElementById(countId);
            if (count) {
              count.count--;
              count.innerHTML = `(${count.count})`;
            }
          }                    
        }
      } // yes I'm sure
    });
  } else { // top level table
    btnDelete.addEventListener("click", function(e) {
      if (confirm(`Are you sure you want to delete this row?\nThis cannot be undone.`)) {
        if (model.serial) { // serial set - it's not a new row
          startLoading();
          getJSON(resourceForFunction(
            "deleteEntry", parentTable, parentField, model.serial),
                  error => {
                    finishedLoading();
                    if (error) {
                      document.getElementById("error").innerText = error;
                    } else {
                      // remove the row from the table
                      tr.remove();
                      // ensure UI has time to remove the row
                      setTimeout(()=>alert("Row deleted."), 100);
                      if (countId) { // update summary count
                        const count = document.getElementById(countId);
                        if (count) {
                          count.count--;
                          count.innerHTML = `(${count.count})`;
                        }
                      }                    
                    }
                  });
        } else { // a new row
          tr.remove();
          // esnure UI has time to remove the row
          setTimeout(()=>alert("Row deleted."), 100);
          if (countId) { // update summary count
            const count = document.getElementById(countId);
            if (count) {
              count.count--;
              count.innerHTML = `(${count.count})`;
            }
          }                    
        }
      } // yes I'm sure
    });
  } // top level table
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
        td, fields[name], (e)=>btnSave.style.display = null, value);
      if (fields[name].hint) input.title = fields[name].hint;
      // ensure the save function can access the input
      inputs[name] = input;
      if (!firstInput) firstInput = input;
    } else { // (grand)child-table
      const childTable = `${parentTable}_${childField}_${name}`;
      const countId = `count-${childTable}-${childRowsId}`;
      let values = td;
      if (depth > 1) { // deeper than grandchild table
        // to avoid clutter, hide under a summary
        values = document.createElement("inline-details");
        const label = document.createElement("inline-summary");
        label.id = countId;
        label.onclick = ()=>{ values.toggleAttribute("open"); };
        values.appendChild(label);
        td.appendChild(values);
      }
      
      // function for loading rows
      const readChildEntries = function() {
        // get all field values for entry
        startLoading();
        getJSON(
          resourceForFunction(
            "readChildEntries",
            `${parentTable}_${childField}`, "serial", model.serial||"", name),
          data => {
            finishedLoading();
            values.appendChild(
              showForm(
                depth+1, `${parentTable}_${childField}`, "serial", ()=>model.serial||"",
                name, data, countId));
            if (depth > 0) { // deeper than grandchild table
              const count = document.getElementById(countId);
              if (count) {
                count.count = data.length; // add/delete causes updates later...
                count.innerHTML = `(${data.length})`;
              }
              if (data.length <= 4) {
                values.setAttribute("open", true);
              }
            }
          }); // readChildEntries
      };
      
      // get child field definitions      
      if (!tableFields[childTable]) { // we don't have the table definition yet
        getJSON(resourceForFunction("readFields", childTable), definitions => {
          const fields = {};
          // build a dictionary of definitions
          for (let definition of definitions) {
            if (definition.field) {
              fields[definition.field] = definition;
            }
          } // next child field definition
          tableFields[childTable] = fields;

          readChildEntries();
        }); // readFields
      } else { // already have the table definitions
        readChildEntries();
      }
    } // (grand)child-table
    
    tr.appendChild(td);
  } // next field

  // add save button at the end
  tr.appendChild(tdSave);
  tr.appendChild(tdDelete);
  rows.appendChild(tr);

  return firstInput; // in case we want to focus on it
} // addRow

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
          + "?t="+encodeURIComponent(t)
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
    const datalist = document.createElement("datalist");
    datalist.id = `matches-${fieldDefinition.field}`;
    valueElement.appendChild(datalist);
    const lookup = function() { // ask the server for some matching options
      if (input.value) {
        getJSON(resourceForFunction(
          "matches", t, fieldDefinition.validation || field, input.value), values => {
          if (values.length == 1 && values[0].startsWith("DbTagger.")) { // error
            document.getElementById("error").innerText = values[0];
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
        optionsList = document.createElement("datalist");
        optionsList.id = `matches-${fieldDefinition.field}`;
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
      const datalist = document.createElement("datalist");
      datalist.id = `matches-${fieldDefinition.field}`;
      datalist.optionValues = [];
      valueElement.appendChild(datalist);
      const lookup = function() { // ask the server for some matching options
        getJSON(resourceForFunction(
          "matches", lookupTable, lookupField, input.value||".*"), values => {
            if (values.length == 1 && values[0].startsWith("DbTagger.")) { // error
              document.getElementById("error").innerText = values[0];
              // clear the error after a short delay
              window.setTimeout(()=>document.getElementById("error").innerText = "",3000);
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
  
  return input;
}

function showForm(depth, parentTable, parentField, getParentEntry, childField, data, countId) {
  console.log(`showForm(${depth}, ${parentTable}, ${parentField}, ${getParentEntry}, ${childField}, ${data}, ${countId})`);
  const table = document.createElement("table");
  const thead = document.createElement("thead");
  let tr = document.createElement("tr");
  let showHeader = false; // if it's just a single field named after the parent-table field
  console.log(childField?`${parentTable}_${childField}`:parentTable);
  const fields = tableFields[childField?`${parentTable}_${childField}`:parentTable];
  console.log("fields " + JSON.stringify(fields));
  for (let name in fields) {
    if (fields[name].type == "hidden") continue;
    const th = document.createElement("th");
    th.className = fields[name].type;
    if (fields[name].hint) th.title = fields[name].hint;
    if (childField) {
      const span = document.createElement("span");
      span.appendChild(document.createTextNode(name));
      th.appendChild(span);
    } else { // top level table
      // add filter boxes, one per field
      const input = document.createElement("input");
      input.type = "text";
      input.className = "filter";
      input.id = `filter-${name}`;
      input.name = name;
      input.placeholder = name;
      if (!filterInput) {
        filterInput = input;
      } else if (filterInput.id == input.id) { // this was the filter field
        // maintain filter value
        input.value = filterInput.value;
        input.setAttribute("autofocus", true);
        filterInput = input;
      }
      // event to trigger filter
      input.addEventListener("input", function(e) {
        if (deferListing) window.clearTimeout(deferListing);
        deferListing = window.setTimeout(()=>listing(input), 1000);
      });
      th.appendChild(input);
    }
    tr.appendChild(th);
    if (name != childField) showHeader = true;
  } // next field
  // add columns for save/delete buttons
  const thSave = document.createElement("th");
  thSave.id = "max-rows-label";
  thSave.className = "save-column";
  thSave.appendChild(document.createTextNode("Rows"));
  tr.appendChild(thSave);
  const thDelete = document.createElement("th");
  thDelete.className = "delete-column";
  const maxRowsInput = document.createElement("input");
  maxRowsInput.id = "max-rows";
  maxRowsInput.type = "number";
  maxRowsInput.step = 1;
  maxRowsInput.min = 1;
  maxRowsInput.value = maxRows;
  maxRowsInput.addEventListener("input", function(e) {
    console.log("maxRowsInput " + filterInput);
    if (deferListing) window.clearTimeout(deferListing);
    deferListing = window.setTimeout(()=>listing(filterInput), 1000);
  });
  thDelete.appendChild(maxRowsInput);
  tr.appendChild(thDelete);
  console.log(`showHeader ${showHeader}`);
  if (showHeader) {
    thead.appendChild(tr);
    table.appendChild(thead);
  }
  
  const tbody = document.createElement("tbody");
  tbody.id = childField?`rows-${parentTable}_${childField}`:`rows-${parentTable}`;
  for (let r in data) { // for each child row
    addRow(
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
    if (!getParentEntry()) {
      alert("Please save this row first");
    } else {
      const firstInput = addRow(
        depth, parentTable, parentField, getParentEntry, childField, tbody, {}, countId);
      if (countId) {
        const count = document.getElementById(countId);
        if (count) {
          count.count++;
          count.innerHTML = `(${count.count})`;
        }
      }
      if (firstInput) firstInput.focus();
        
    }
  });
  td.appendChild(btnCreate);
  tr.appendChild(td);
  if (!childField) { // top level table
    // add row count
    td = document.createElement("td");
    let span = document.createElement("span");
    span.id = "row-count";
    td.appendChild(span);
    span = document.createElement("span");
    span.appendChild(document.createTextNode("rows"));
    td.appendChild(span);
  }
  tfoot.appendChild(tr);
  table.appendChild(tfoot);
  return table;
}

// get field definitions
getJSON(resourceForFunction("readFields", t), definitions => {
  
  // build a dictionary of definitions
  const fields = {};
  let firstField = null;
  for (let definition of definitions) {
    if (definition.field) {
      fields[definition.field] = definition;
      if (!firstField) firstField = definition.field;
    }
  } // next field definition
  tableFields[t] = fields;

  getJSON(resourceForFunction(
    "readEntries", t, firstField, ".*", maxRows), values => {
      if (values.length == 1 && values[0]["DbTagger_error"]) { // error
        document.getElementById("error").innerText = values[0]["DbTagger_error"];
        // clear the error after a short delay
        window.setTimeout(()=>document.getElementById("error").innerText = "", 3000);
      } else { // rows returned
        
        // list the rows
        document.getElementById("rows").appendChild(
          showForm(0, t, "serial", ()=>t, "", values, "row-count"));
      }
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
