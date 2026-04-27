startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

// load table list
function listTables() {
    startLoading();
    getJSON("listTables", tableIds => {
        var tables = document.getElementById("tables");
        tables.innerHTML = ""; // clear any existing list
        for (var l in tableIds) {
            const tableId = tableIds[l];
            var tr = document.createElement("tr");
            tables.appendChild(tr);
            var td = document.createElement("td");
            tr.appendChild(td);
            td.appendChild(document.createTextNode(tableId));
            td = document.createElement("td");
            tr.appendChild(td);
            var link = document.createElement("a");
            td.appendChild(link);
            link.title = `${tableId} definition`;
            link.href = `fields.html?t=${tableId}`
            var img = document.createElement("img");
            img.src = "../edit.svg"
            img.alt = "✎";
            link.appendChild(img);
            td = document.createElement("td");
            tr.appendChild(td);
            var button = document.createElement("button");            
            td.appendChild(button);
            button.title = `Delete ${tableId}`;
            img = document.createElement("img");
            img.src = "../delete.svg"
            img.alt = "❌";
            button.appendChild(img);
            button.onclick = ()=>{deleteTable(tableId);};
        }
        if (!tableIds.length) {
            // no tables yet, so they'll want to upload a file
            document.getElementById("uploadForm").setAttribute("open",true);
        }
        finishedLoading();
    });
}

function deleteTable(tableId) {
    if (confirm(`Are you sure you want to delete the table ${tableId}?`)) {
        getText(resourceForFunction("deleteTable", tableId), (error) => {
            if (error) alert(error);
            // refresh table list
            listTables();
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
    document.getElementById("table").value = file.name
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
            var secondLine = csvRecordsArray.length < 2?csvRecordsArray[0]:csvRecordsArray[1];
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
            if (firstLine.match(/.*".*/) || secondLine.match(/.*".*/)) {
              document.getElementById("quote").value = "\"";
            }
            showSample();
            document.getElementById("btnUploadTable").removeAttribute("disabled");
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
    } else if (quote) {
      parseFields = row => {
        // ensure quoted delimiters don't split one field
        row = row.replaceAll(
          new RegExp(`(${quote}[^${quote}]*)${fieldDelimiter}([^${quote}]*${quote})`, "g"),
          // use \n in place of delimiter
          "$1\n$2");
        return row.split(fieldDelimiter)
        // change \n back into delimiter
          .map(field => field.replaceAll("\n",fieldDelimiter));
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
        input.title = "The name of this colum in the table";
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

function uploadTable() {
    document.getElementById("uploadProgress").style.display = "";
    var uploadProgress = document.getElementById("progress");
    var input = document.getElementById("file");
    if (input.files.length == 0) return;
    var file = input.files[0];
    var fd = new FormData();
    fd.append("table", document.getElementById("table").value);
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
    postForm("loadTable", fd, function(e) {
        console.log("uploadResult " + this.responseText);
        uploadProgress.max = uploadProgress.max || 100;
        uploadProgress.value = uploadProgress.max;
        var result = this.responseText;
        if (!result) { // no error, upload succeeded
            document.getElementById("uploadResult").innerHTML = "<p>File uploaded.</p>";

            // now track load progress
            trackTableLoad();
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

function trackTableLoad() {
    // still running?
    getText("getRunning", running => {
        if (running == "true") {
            // keep tracking progress
            window.setTimeout(trackTableLoad, 1000);
        } else { // finished
            console.log("load finished.");
            document.getElementById("uploadForm").removeAttribute("open");
            listTables();
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
document.getElementById("btnUploadTable").onclick = uploadTable;
document.getElementById("fieldDelimiter").onchange = showSample;
document.getElementById("quote").onchange = showSample;
document.getElementById("comment").onchange = showSample;
document.getElementById("skipFirstLine").onclick = showSample;

listTables();
