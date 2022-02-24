startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

const taskId = window.location.search.substring(1);

// first, get the layer schema
var schema = null;
getSchema(s => {
    schema = s;
    
    // populate layer input select options...          
    var tokenLayerId = document.getElementById("tokenLayerId");
    addLayerOptions(
        tokenLayerId, schema,
        // this is a function that takes a layer and returns true for the ones we want
        layer => layer.id == schema.wordLayerId
            || (layer.parentId == schema.wordLayerId && layer.alignment == 0));
    // default value:
    if (schema.layers["orthography"]) {
        tokenLayerId.value = "orthography";
    } else {
        tokenLayerId.value = schema.wordLayerId;
    }
    
    // populate the language layers...
    
    var transcriptLanguageLayerId = document.getElementById("transcriptLanguageLayerId");
    addLayerOptions(
        transcriptLanguageLayerId, schema,
        layer => layer.parentId == schema.root.id && layer.alignment == 0
            && /.*lang.*/.test(layer.id));
    // select the first one by default
    transcriptLanguageLayerId.selectedIndex = 1;
    
    var phraseLanguageLayerId = document.getElementById("phraseLanguageLayerId");
    addLayerOptions(
        phraseLanguageLayerId, schema,
        layer => layer.parentId == schema.turnLayerId && layer.alignment == 2
            && /.*lang.*/.test(layer.id));
    // select the first one by default
    phraseLanguageLayerId.selectedIndex = 1;
    
    // populate layer output select options...          
    var tagLayerId = document.getElementById("tagLayerId");
    addLayerOptions(
        tagLayerId, schema,
        layer => layer.parentId == schema.wordLayerId && layer.alignment == 0);
    tagLayerId.selectedIndex = 0;

    loadDictionaryOptions(() => {
        
        // GET request to getTaskParameters retrieves the current task parameters, if any
        getText("getTaskParameters", text => {
            try {
                var parameters = new URLSearchParams(text);
                
                // set initial values of properties in the form above
                // (this assumes bean property names match input id's in the form above)
                for (const [key, value] of parameters) {
                    document.getElementById(key).value = value;
                }
                // set the checkbox
                document.getElementById("firstVariantOnly").checked
                    = parameters.get("firstVariantOnly");
                // if there's no utterance tag layer defined
                if (tagLayerId.selectedIndex == 0
                    // but there's a layer named after the task
                    && schema.layers[taskId]) {
                    
                    // select that layer by default
                    tagLayerId.value = taskId;
                }
            } finally {
                finishedLoading();
            }
        });
    });
});

function loadDictionaryOptions(onLoad) {
    // populate dictionary options
    getJSON("getDictionaryIds", dictionaries => {
        var dictionary = document.getElementById("dictionary");
        dictionary.innerHTML = "";
        var originalValue = dictionary.value;
        var option = document.createElement("option");
        option.disabled = true;
        option.appendChild(document.createTextNode("[select dictionary]"));
        dictionary.appendChild(option);
        
        for (d of dictionaries) {
            option = document.createElement("option");
            option.appendChild(document.createTextNode(d));
            dictionary.appendChild(option);
        } // next dictionary
        if (!dictionaries.length) {
            // no dictionaries yet, so they'll want to upload a file
            document.getElementById("uploadForm").setAttribute("open",true);
        }
        if (originalValue) {
            dictionary.value = originalValue;
        } else {
            dictionary.selectedIndex = 0;
        }

        if (onLoad) onLoad();
    });
}

// this function detects when the user selects [add new layer]:
function changedLayer(select) {
    if (select.value == "[add new layer]") {
        var newLayer = prompt("Please enter the new layer ID", taskId);
        if (newLayer) { // they didn't cancel
            // check there's not already a layer with that name
            for (var l in schema.layers) {
                var layer = schema.layers[l];
                if (layer.id == newLayer) {
                    alert("A layer called "+newLayer+" already exists");
                    select.selectedIndex = 0;
                    return;
                }
            } // next layer
            // add the layer to the list
            var layerOption = document.createElement("option");
            layerOption.appendChild(document.createTextNode(newLayer));
            select.appendChild(layerOption);
            // select it
            select.selectedIndex = select.children.length - 1;
        }
    }
}

var csvRecordsArray = null;
function selectFile() {
    console.log("selectFile...");
    var input = document.getElementById("file");
    console.log("input " + input);
    console.log("files " + input.files);
    if (input.files.length == 0) return;
    var file = input.files[0];
    console.log("file " + file);
    console.log("name " + file.name);
    document.getElementById("lexicon").value = file.name
    const reader = new FileReader();
    const component = this;
    reader.onload = function() {  
        const csvData = reader.result;  
        csvRecordsArray = csvData.split(/\r\n|\n/);
        // remove blank lines
        csvRecordsArray = csvRecordsArray.filter(l=>l.length>0);
        if (csvRecordsArray.length == 0) {
            alert("File is empty: " + file.name);
        } else {
            // get headers...
            var firstLine = csvRecordsArray[0];
            console.log("First line: " + firstLine);
            // split the line into fields
            let delimiter = ",";
            if (firstLine.startsWith(";;;")) { // most likely the CMU dictionary
                document.getElementById("comment").value = ";";
                document.getElementById("skipFirstLine").checked = false;
                delimiter = " - ";
            } else if (firstLine.match(/.*\t.*/)) delimiter = "\t";
            else if (firstLine.match(/.;.*/)) delimiter = ";";
            else if (firstLine.match(/. .*/)) delimiter = " ";
            document.getElementById("fieldDelimiter").value = delimiter;
            if (firstLine.match(/.*".*/)) document.getElementById("quote").value = "\"";
            showSample();
            document.getElementById("btnUploadLexicon").removeAttribute("disabled");
        }
    };
    reader.onerror = function () {  
        alert("Error reading " + file.name);
    };
    reader.readAsText(file);
}

function showSample() {
    var fieldDelimiter = document.getElementById("fieldDelimiter").value;
    var quote = document.getElementById("quote").value;
    var comment = document.getElementById("comment").value;
    var skipFirstLine = document.getElementById("skipFirstLine").checked;
    var sample = document.getElementById("sample");
    sample.innerHTML = "<caption>Data sample</caption>";
    firstLine = csvRecordsArray[0];
    var parseFields = row => row.split(fieldDelimiter);
    if (fieldDelimiter == " - ") {
        // only split on the first occurrence of the delimiter
        parseFields = row => {
            var firstDelimiter = row.indexOf(" ");
            if (fieldDelimiter < 0) {
                return [row, ""];
            } else {
                return [
                    row.substring(0, firstDelimiter),
                    row.substring(firstDelimiter + fieldDelimiter.length)];
            }
        }
    }
    var stripQuotes = field => field;
    if (quote) {
        stripQuotes = field => field.replace(new RegExp("^" + quote + "(.*)" + quote + "$"), "$1");
    }
    var fields = parseFields(firstLine);
    var fieldCount = fields.length;
    // add a line of headers
    var tr = document.createElement("tr");
    sample.appendChild(tr);
    for (var c = 0; c < fieldCount; c++) {
        var th = document.createElement("th");
        tr.appendChild(th);
        var input = document.createElement("input");
        th.appendChild(input);
        input.className = "fieldHeader";
        input.type = "text";
        input.title = "The name of this colum in the lexicon";
        if (skipFirstLine) {
            input.value = stripQuotes(fields[c]);
        } else {
            if (c == 0) {
                input.value = "Word";
            } else {
                if (fieldCount == 2) {
                    input.value = "Entry";
                } else {
                    input.value = "field"+c;
                }
            }
        }
    } // next column
    
    // show some rows
    var startRow = skipFirstLine?1:0;
    for (var r = startRow; r < csvRecordsArray.length && sample.childElementCount < 10; r++) {
        console.log(csvRecordsArray[r]);
        // ignore comment line
        if (comment && csvRecordsArray[r].startsWith(comment)) continue;
        tr = document.createElement("tr");
        sample.appendChild(tr);
        var row = parseFields(csvRecordsArray[r]);
        for (var c = 0; c < fieldCount; c++) {
            var td = document.createElement("td");
            tr.appendChild(td);
            var value = stripQuotes(row[c]);
            td.appendChild(document.createTextNode(value));
        } // next column
    } // next row
    tr = document.createElement("tr");
    sample.appendChild(tr);
    var td = document.createElement("td");
    tr.appendChild(td);
    td.setAttribute("colspan", fieldCount);
    td.appendChild(document.createTextNode("... "+csvRecordsArray.length+" rows ..."));
}

function uploadLexicon() {
    document.getElementById("uploadProgress").style.display = "";
    var uploadProgress = document.getElementById("progress");
    var input = document.getElementById("file");
    if (input.files.length == 0) return;
    var file = input.files[0];
    var fd = new FormData();
    fd.append("lexicon", document.getElementById("lexicon").value);
    var fieldDelimiter = document.getElementById("fieldDelimiter").value || ",";
    fd.append("fieldDelimiter", fieldDelimiter);
    fd.append("quote", document.getElementById("quote").value);
    fd.append("comment", document.getElementById("comment").value);
    var fieldNames = "";
    var fieldHeaders = document.getElementsByClassName("fieldHeader");
    for (var f = 0; f < fieldHeaders.length; f++) {
        var header = fieldHeaders[f].value || "field" + (f+1);
        if (fieldNames) fieldNames += fieldDelimiter;
        fieldNames += header;
    }
    fd.append("fieldNames", fieldNames);
    fd.append("skipFirstLine", document.getElementById("skipFirstLine").checked);
    fd.append("file", file);
    postForm("loadLexicon", fd, function(e) {
        console.log("uploadResult " + this.responseText);
        uploadProgress.max = uploadProgress.max || 100;
        uploadProgress.value = uploadProgress.max;
        var result = this.responseText;
        if (!result) { // no error, upload succeeded
            document.getElementById("uploadResult").innerHTML = "<p>File uploaded.</p>";

            // now track load progress
            trackLexiconLoad();
        } else { // error
            document.getElementById("uploadResult").innerHTML
                = "<p class='error'>"+result+"</p>";
        }
    }, function(e) {
        console.log("uploadProgress " + e.loaded);
        if (e.lengthComputable) {
            uploadProgress.max = e.total;
            uploadProgress.value = e.loaded;
        }
    }, function(e) {
        console.log("uploadFailed " + this.responseText);
        uploadProgress.max = uploadProgress.max || 100;
        uploadProgress.value = uploadProgress.value || 1;
        document.getElementById("uploadResult").innerHTML
            = "<p class='error'>"+this.responseText+"</p>";
    });
}

function trackLexiconLoad() {
    // still running?
    getText("getRunning", running => {
        if (running == "true") {
            // keep tracking progress
            window.setTimeout(trackLexiconLoad, 1000);
        } else { // finished
            console.log("load finished.");
            loadDictionaryOptions(() => {
                console.log("dictionary options loaded.");
                var dictionary = document.getElementById("dictionary");
                if (dictionary.selectedIndex <= 0) {
                    // select the first dictionary for that lexicon
                    var lexicon = document.getElementById("lexicon").value;
                    for (var d in dictionary.options) {
                        if (dictionary.options[d].value.startsWith(lexicon)) {
                            dictionary.selectedIndex = d;
                            document.getElementById("uploadForm").removeAttribute("open");
                            break;
                        }
                    } // next dictionary
                } // no dictionary already selected
            });
        }
        
        // get progress
        getText("getPercentComplete", percentComplete => {
            document.getElementById("progress").max = 100;
            document.getElementById("progress").value = parseInt(percentComplete);
        });
        
        // get status
        getText("getStatus", status => {
            document.getElementById("uploadResult").innerHTML = `<p>${status}</p>`;
        });
        
    });
}

document.getElementById("tagLayerId").onchange = function(e) {
    changedLayer(this); };
document.getElementById("file").onchange = selectFile;
document.getElementById("btnUploadLexicon").onclick = uploadLexicon;
document.getElementById("fieldDelimiter").onchange = showSample;
document.getElementById("quote").onchange = showSample;
document.getElementById("comment").onchange = showSample;
document.getElementById("skipFirstLine").onclick = showSample;
