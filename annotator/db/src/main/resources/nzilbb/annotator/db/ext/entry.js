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
document.getElementById("entry").innerText = entry;

let fields = {}; // definitions for fields (type/validation etc.)
const childFields = {}; // definitions for child-table fields (key is child name)

function showForm(data) {
  if (data.DbTagger_error) {
    document.getElementById("error").innerText = data.DbTagger_error;
    return;
  }
  const attributes = document.getElementById("attributes");
  attributes.innerHTML = ""; // clear any existing list
  for (let name in fields) {
    if (fields[name].type == "hidden") continue;
    
    let row = document.createElement("div");
    let label = document.createElement("label");
    label.appendChild(document.createTextNode(name));
    row.appendChild(label);
    
    if (fields[name].type != "child-table") { // regular field
      let value = document.createElement("span");
      value.className = "value";
      createFieldValue(value, fields[name], data[name]);
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
    if (data[name] && fields[name].type == "geo-location") {
      // add a map showing the location
      const mapdiv = document.createElement("div");
      mapdiv.id = `map-${name}`;
      mapdiv.className = "map";
      attributes.appendChild(mapdiv);
      const map = L.map(mapdiv);
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
      }).addTo(map);
      if (data[name].indexOf(":") >= 0) { // shape
        // something like:
        // -43.607048, 172.467246 : -43.626186, 172.552390 : -43.681332, 172.552390
        const shape = data[name].split(":")
              .filter(coord=>coord.includes(",")) // discard invalid points
              .map(coord=>coord.trim().split(",")
                   .map(s=>Number(s)));
        const latitudes = shape.map(coord=>coord[0]);
        const longitudes = shape.map(coord=>coord[1]);
        const bounds = [[
          latitudes.reduce((a, b) => Math.min(a, b)),
          longitudes.reduce((a, b) => Math.min(a, b))
        ],[
          latitudes.reduce((a, b) => Math.max(a, b)),
          longitudes.reduce((a, b) => Math.max(a, b))
        ]];
        map.fitBounds(bounds);
        L.polygon(shape).addTo(map);
      } else { // point        
        const coords = data[name].split(",").map(s=>Number(s));
        const targetCoordinates = L.latLng(coords[0], coords[1]);
        map.setView(targetCoordinates, 16);
        L.marker(targetCoordinates).addTo(map);
      }
    }
  }
  enableEditLink();
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
  value.appendChild(table);
}

function addChildRow(childField, rows, model) {
  const tr = document.createElement("tr");    
  for (let name in childFields[childField]) {
    if (childFields[childField][name].type == "hidden") continue;
    const td = document.createElement("td");
    td.title = name;
    td.className = name + " " + childFields[childField][name].type;    
    // create the UI component for the field value
    createFieldValue(
      td, childFields[childField][name], model[name] || "");
    tr.appendChild(td);
  } // next field
  rows.appendChild(tr);
}

function createFieldValue(valueElement, fieldDefinition, value) {
  let span = document.createElement("span");
  span.className = fieldDefinition.type;
  span.id = `attribute-${fieldDefinition.field}`;
  if (value) {
    if (fieldDefinition.type == "url") {
      const a = document.createElement("a");
      a.href = value;
      a.target = fieldDefinition.field;
      a.appendChild(document.createTextNode(value));
      span.appendChild(a);
    } else if (fieldDefinition.type == "email") {
      const a = document.createElement("a");
      a.href = "mailto:" + encodeURIComponent(value);
      a.target = fieldDefinition.field;
      a.appendChild(document.createTextNode(value));
      span.appendChild(a);
    } else if (fieldDefinition.type == "geo-location") {
      // a map <div> will be added separately
    } else if (fieldDefinition.type == "transcript-link") {
      if (value.indexOf("#") > 0 || value.startsWith("http")) { // can link somewhere
        const a = document.createElement("a");
        if (value.startsWith("http")) { // just an http link
          a.href = value;
          a.target = fieldDefinition.field;
        } else if (value.indexOf("#") > 0) {
          // turn something like:
          // http://example.com/labbcat/annotator/ext/DbTagger/entry.html?t=x&f=x&e=x
          // into something like
          // http://example.com/labbcat/transcript?id=transcriptId#annotationId
          a.href = window.location.toString()
            .replace(/annotator\/ext\/DbTagger\/.*$/,"transcript?id=")
            +value;
          a.target = value.split("#")[0]; // target is transcript ID
        }
        a.appendChild(document.createTextNode(value));
        span.appendChild(a);
      } else { // not a link, just text
        span.appendChild(document.createTextNode(value));
      }
    } else if (fieldDefinition.type == "cross-reference") {
      if (value) { // can link somewhere
        const a = document.createElement("a");
        a.href = document.URL.replace(/\?.*/,"")
          + "?t="+encodeURIComponent(table)
          + "&f="+encodeURIComponent(fieldDefinition.validation || field)
          + "&e="+encodeURIComponent(value)
        a.appendChild(document.createTextNode(value));
        span.appendChild(a);
      }
    } else if (fieldDefinition.type == "html") {
      span.innerHTML = value;
    } else if (fieldDefinition.type == "boolean") {
      if (value == "true") span.innerHTML = "✔";
    } else if (fieldDefinition.type == "date") {
      if (value) {
        const date = new Date(value);
        date.setMinutes(date.getMinutes() + date.getTimezoneOffset()); // use this timezone
        span.innerHTML = date.toLocaleDateString();
      }
    } else if (fieldDefinition.type == "datetime") {
      if (value) {
        const date = new Date(value);
        date.setMinutes(date.getMinutes() + date.getTimezoneOffset()); // use this timezone
        span.innerHTML = date.toLocaleString();
      }
    } else {
      span.appendChild(document.createTextNode(value));
    }
  }
  valueElement.appendChild(span);
  return span;
}

function enableEditLink() {
  const editUrl = "edit/entry.html"+window.location.search+(window.location.hash||"");
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
  });
});

