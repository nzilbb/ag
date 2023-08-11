startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

const lexiconId = window.location.search.substring(1);
document.getElementById("lexiconId").innerHTML = lexiconId;

// load phoneme mappings
function listMappings() {
  startLoading();
  getJSON(resourceForFunction("readDiscMappings", lexiconId), mappings => {
    const map = document.getElementById("map");
    map.innerHTML = ""; // clear any existing list
    document.getElementById("count").innerHTML = mappings.length;
    for (let m in mappings) {
      const mapping = mappings[m];
      addMappingRow(mapping.phoneme_orig, mapping.phoneme_disc, mapping.note);
    }
    finishedLoading();
  });
}

function addMappingRow(phoneme_orig, phoneme_disc, note) {
  const map = document.getElementById("map");
  const tr = document.createElement("tr");
  tr.id = `mapping-${phoneme_orig}`;
  map.appendChild(tr);
  let td = document.createElement("td");
  tr.appendChild(td);
  td.appendChild(document.createTextNode(phoneme_orig));
  
  td = document.createElement("td");
  td.className = "phoneme-orig";
  tr.appendChild(td);
  
  td = document.createElement("td");
  td.className = "arrow";
  td.appendChild(document.createTextNode("→"));
  
  td = document.createElement("td");
  td.className = "phoneme-disc";
  tr.appendChild(td);
  let input = document.createElement("input");
  input.id = `phoneme_disc-${phoneme_orig}`;
  td.appendChild(input);
  input.title = `DISC version of ${phoneme_orig}`;
  input.value = phoneme_disc;
  input.addEventListener("keydown", (e)=> {
    if (event.keyCode == 13) {
      saveNextChange();
    } else {
      registerChanges(phoneme_orig)
    }
  });
  
  td = document.createElement("td");
  td.className = "note";
  tr.appendChild(td);
  input = document.createElement("input");
  input.id = `note-${phoneme_orig}`;
  td.appendChild(input);
  input.title = "Note";
  input.value = note;
  input.addEventListener("keydown", (e)=> {
    if (event.keyCode == 13) {
      saveNextChange();
    } else {
      registerChanges(phoneme_orig)
    }
  });
  
  td = document.createElement("td");
  td.className = "delete";
  tr.appendChild(td);
  const button = document.createElement("button");            
  td.appendChild(button);
  button.title = `Delete mapping for ${phoneme_orig}`;
  button.appendChild(document.createTextNode("❌"));
  button.onclick = ()=>{deleteMapping(phoneme_orig);};
  
  return tr;
}

function deleteMapping(phoneme_orig) {
  if (confirm(`Are you sure you want to delete the mapping for ${phoneme_orig}?`)) {
    const tr = document.getElementById(`mapping-${phoneme_orig}`);
    if (tr.insert) { // newly inserted mapping that hasn't been saved yet
      // just remove the row
      document.getElementById(`mapping-${phoneme_orig}`).remove();
    } else { // remove from database
      startLoading();
      getText(resourceForFunction("deleteDiscMapping", lexiconId, phoneme_orig), (error) => {
        if (error) alert(error);
        // remove from the list
        document.getElementById(`mapping-${phoneme_orig}`).remove();
        finishedLoading();
      }, "text/plain");
    }
  }
}

const changedMappings = [];

function registerChanges(phoneme_orig) {
  if (!changedMappings.includes(phoneme_orig)) {
    changedMappings.push(phoneme_orig);
    document.getElementById("save").style.display = "";// show
  }
}

function saveNextChange() {
  if (changedMappings.length > 0) {
    startLoading();
    const phoneme_orig = changedMappings.pop();
    const tr = document.getElementById(`mapping-${phoneme_orig}`);
    const phoneme_disc = document.getElementById(`phoneme_disc-${phoneme_orig}`).value;
    const note = document.getElementById(`note-${phoneme_orig}`).value
    // currently, an error is thrown if note is a zero-length string, so we ensure it's not:
          || " ";
    getText(resourceForFunction(
      tr.insert?"createDiscMapping":"updateDiscMapping",
      lexiconId, phoneme_orig, phoneme_disc, note), (error) => {
        if (error) {
          alert(error);
        } else {
          tr.insert = false;
        }
        saveNextChange();
      }, "text/plain");
  } else { // no more changes to save
    document.getElementById("save").style.display = "none";// hide
    finishedLoading();
  }
}

function addNewMapping() {
  const phoneme_orig = prompt("Original Phoneme Label");
  if (phoneme_orig) {
    // check it's not already there
    if (document.getElementById(`mapping-${phoneme_orig}`)) {
      alert(`There is already a mapping for "${phoneme_orig}"`);
    } else {
      const tr = addMappingRow(phoneme_orig, "", "");
      tr.insert = true;
      registerChanges(phoneme_orig)
    }
    document.getElementById(`phoneme_disc-${phoneme_orig}`).focus();
  }
}

document.getElementById("save").onclick = saveNextChange;
document.getElementById("insert").onclick = addNewMapping;
listMappings();
