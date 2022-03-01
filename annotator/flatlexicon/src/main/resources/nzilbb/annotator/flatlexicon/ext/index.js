startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

// load lexicon list
function listLexicons() {
    startLoading();
    getJSON("listLexicons", lexiconIds => {
        var lexicons = document.getElementById("lexicons");
        lexicons.innerHTML = ""; // clear any existing list
        for (var l in lexiconIds) {
            const lexiconId = lexiconIds[l];
            var tr = document.createElement("tr");
            lexicons.appendChild(tr);
            var td = document.createElement("td");
            tr.appendChild(td);
            td.appendChild(document.createTextNode(lexiconId));
            td = document.createElement("td");
            tr.appendChild(td);
            var button = document.createElement("button");            
            td.appendChild(button);
            button.title = `Delete ${lexiconId}`;
            button.appendChild(document.createTextNode("❌"));
            button.onclick = ()=>{deleteLexicon(lexiconId);};
        }
        finishedLoading();
    });
}

function deleteLexicon(lexiconId) {
    if (confirm(`Are you sure you want to delete the lexicon ${lexiconId}?`)) {
        getText(resourceForFunction("deleteLexicon", lexiconId), (error) => {
            if (error) alert(error);
            // refresh lexicon list
            listLexicons();
        }, "text/plain");
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
                    row.substring(firstDelimiter + 1)];
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
            document.getElementById("uploadForm").removeAttribute("open");
            listLexicons();
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

document.getElementById("file").onchange = selectFile;
document.getElementById("btnUploadLexicon").onclick = uploadLexicon;
document.getElementById("fieldDelimiter").onchange = showSample;
document.getElementById("quote").onchange = showSample;
document.getElementById("comment").onchange = showSample;
document.getElementById("skipFirstLine").onclick = showSample;

listLexicons();
