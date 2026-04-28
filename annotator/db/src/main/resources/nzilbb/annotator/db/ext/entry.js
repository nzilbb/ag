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
    row.className = "field " + fields[name].type;
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
        a.href = "mailto:" + encodeURIComponent(data[name]);
        a.target = name;
        a.appendChild(document.createTextNode(data[name]));
        span.appendChild(a);
      } else if (fields[name].type == "geo-location") {
        const a = document.createElement("a");
        let coords = data[name].split(",");
        if (data[name].indexOf(":") >= 0) { // shape
          // something like:
          // -43.607048, 172.467246 : -43.626186, 172.552390 : -43.681332, 172.552390
          
          // go to the point in the middle
          const shape = data[name].split(":")
                .map(coord=>coord.trim().split(",")
                     .map(s=>Number(s)));
          const latitudes = shape.map(coord=>coord[0]);
          const longitudes = shape.map(coord=>coord[1]);
          coords = [
            latitudes.reduce((a, b) => a + b) / latitudes.length,
            longitudes.reduce((a, b) => a + b) / longitudes.length
          ];
        } 
        //a.href = "https://www.google.com/maps?q=" + encodeURIComponent(data[name]);
        a.href = `https://www.openstreetmap.org/?mlat=${coords[0]}&mlon=${coords[1]}#map=16/${coords[0]}/${coords[1]}`;
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
              .map(coord=>coord.trim().split(",")
                   .map(s=>Number(s)));
        const latitudes = shape.map(coord=>coord[0]);
        const longitudes = shape.map(coord=>coord[1]);
        const centre = [
          latitudes.reduce((a, b) => a + b) / latitudes.length,
          longitudes.reduce((a, b) => a + b) / longitudes.length
        ];
        const targetCoordinates = L.latLng(centre[0], centre[1]);
        map.setView(targetCoordinates, 14);
        L.polygon(shape).addTo(map);
      } else { // point        
        const coords = data[name].split(",").map(s=>Number(s));
        const targetCoordinates = L.latLng(coords[0], coords[1]);
        map.setView(targetCoordinates, 16);
        L.marker(targetCoordinates).addTo(map);
      }
    }
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

