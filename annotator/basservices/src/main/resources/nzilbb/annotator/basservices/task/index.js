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
    var orthographyLayerId = document.getElementById("orthographyLayerId");
    addLayerOptions(
        orthographyLayerId, schema,
        // this is a function that takes a layer and returns true for the ones we want
        layer => layer.id == schema.wordLayerId
            || (layer.parentId == schema.wordLayerId && layer.alignment == 0));
    // default value:
    if (schema.layers["orthography"]) {
        orthographyLayerId.value = "orthography";
    } else {
        orthographyLayerId.value = schema.wordLayerId;
    }
    
    // populate the language layer options    
    var transcriptLanguageLayerId = document.getElementById("transcriptLanguageLayerId");
    addLayerOptions(
        transcriptLanguageLayerId, schema,
        layer => layer.parentId == schema.root.id && layer.alignment == 0
            && /.*lang.*/.test(layer.id));
    // select the first one by default
    transcriptLanguageLayerId.selectedIndex = 1;
    
    // populate layer output select options...          
    var pronunciationLayerId = document.getElementById("pronunciationLayerId");
    addLayerOptions(
        pronunciationLayerId, schema,
        layer => layer.parentId == schema.wordLayerId && layer.alignment == 0);
    pronunciationLayerId.selectedIndex = 0;
    const wordAlignmentLayerId = document.getElementById("wordAlignmentLayerId");
    addLayerOptions(
        wordAlignmentLayerId, schema,
        // word layer, or aligned turn children
        layer => layer.id == schema.wordLayerId
            || (layer.parentId == schema.turnLayerId && layer.alignment == 2
                && layer.id != schema.utteranceLayerId));
    // default value:
    wordAlignmentLayerId.value = schema.wordLayerId;
    
    const phoneAlignmentLayerId = document.getElementById("phoneAlignmentLayerId");
    addLayerOptions(
        phoneAlignmentLayerId, schema,
        // segment layer or aligned turn children
        layer => layer.id == "segment" || layer.id == "phone"
            || (layer.parentId == schema.turnLayerId && layer.alignment == 2
                && layer.id != schema.utteranceLayerId
                && layer.id != schema.wordLayerId));
    // default value:
    if (schema.layers["segment"]) {
        phoneAlignmentLayerId.value = "segment";
        existingPhoneLayerId = "segment";
    } else if (schema.layers["phone"]) {
        phoneAlignmentLayerId.value = "phone";
        existingPhoneLayerId = "phone";
    }
    
    const utteranceTagLayerId = document.getElementById("utteranceTagLayerId");
    addLayerOptions(
        utteranceTagLayerId, schema,
        // aligned turn children
        layer => layer.parentId == schema.turnLayerId && layer.alignment == 2
        // but not word nor utterance
            && layer.id != schema.wordLayerId && layer.id != schema.utteranceLayerId);
    
    // TODO const participantTagLayerId = document.getElementById("participantTagLayerId");
    // addLayerOptions(
    //     participantTagLayerId, schema,
    //     // participant attributes
    //     layer => layer.parentId == schema.participantLayerId && layer.alignment == 0);
    
    // GET request to getTaskParameters retrieves the current task parameters, if any
    getText("getTaskParameters", text => {
        var parameters = new URLSearchParams(text);
        
        // set initial values of properties in the form above
        // (this assumes bean property names match input id's in the form above)
        for (const [key, value] of parameters) {
            try {
                document.getElementById(key).value = value;
            } catch (x) {
                console.log(`Unrecognized parameter: ${key}: ${x}`);
            }
        }
        // set the checkboxes
        document.getElementById("wordStress").checked
            = parameters.get("wordStress");
        document.getElementById("syllabification").checked
            = parameters.get("syllabification");
        if (parameters.get("service") == "G2P") {
            document.getElementById("service-G2P").checked = true;
        } else {
            document.getElementById("service-MAUSBasic").checked = true;
        }
        changeService();
        // if there's no utterance tag layer defined
        if (pronunciationLayerId.selectedIndex == 0
            // but there's a layer named after the task
            && schema.layers[taskId]) {
            
            // select that layer by default
            pronunciationLayerId.value = taskId;
        }
        loadLanguageOptions();
    });
});

// load the current language options from clarin, for populating the dropdown options
function loadLanguageOptions() {
    const url = "https://clarin.phonetik.uni-muenchen.de/BASWebServices/BAS_Webservices.cmdi.xml";
    const request = new XMLHttpRequest();
    request.overrideMimeType("text/xml");
    request.addEventListener("error", ()=>{ finishedLoading(); });
    request.addEventListener("abort", ()=>{ finishedLoading(); });
    request.addEventListener("load", function() {
        finishedLoading();
        
        cmd = this.responseXML;
        // the document has a namespace defined, so we have to implement a resolver
        var resolver = p=>"http://www.clarin.eu/cmd/";
        // the xpath expressions need to include a namespace, our resolver doesn't care what
        // so we use x:...

        // first MAUSBasic options:
        let optionsXpath = "//x:Operation[x:Name='runMAUSBasic']/x:Input/x:Parameter[x:Name='LANGUAGE']/x:Values/x:ParameterValue";
        let options = cmd.evaluate(
            optionsXpath, cmd, resolver, XPathResult.ORDERED_NODE_ITERATOR_TYPE)
        let thisOption = options.iterateNext();
        if (thisOption) { // there are values
            const forceLanguageMAUSBasic = document.getElementById("forceLanguageMAUSBasic");
            const orginalValue = forceLanguageMAUSBasic.value;
            forceLanguageMAUSBasic.innerHTML = ""; // remove current items
            let option = document.createElement("option");
            option.value = "";
            option.appendChild(document.createTextNode("Use transcript language"));
            forceLanguageMAUSBasic.appendChild(option)
            while (thisOption) {
                const value = cmd.evaluate(
                    "x:Value", thisOption, resolver, XPathResult.STRING_TYPE).stringValue;
                const description = cmd.evaluate(
                    "x:Description", thisOption, resolver, XPathResult.STRING_TYPE).stringValue;
                option = document.createElement("option");
                option.value = value;
                option.appendChild(document.createTextNode(description||value));
                forceLanguageMAUSBasic.appendChild(option)
                
                thisOption = options.iterateNext();
            } // next option
            forceLanguageMAUSBasic.value = orginalValue;
        } // there are values

        // now G2P options:
        optionsXpath = "//x:Operation[x:Name='runG2P']/x:Input/x:Parameter[x:Name='LANGUAGE']/x:Values/x:ParameterValue";
        options = cmd.evaluate(
            optionsXpath, cmd, resolver, XPathResult.ORDERED_NODE_ITERATOR_TYPE)
        thisOption = options.iterateNext();
        if (thisOption) { // there are values
            const forceLanguageG2P = document.getElementById("forceLanguageG2P");
            const orginalValue = forceLanguageG2P.value;
            forceLanguageG2P.innerHTML = ""; // remove current items
            let option = document.createElement("option");
            option.value = "";
            option.appendChild(document.createTextNode("Use transcript language"));
            forceLanguageG2P.appendChild(option)
            while (thisOption) {
                const value = cmd.evaluate(
                    "x:Value", thisOption, resolver, XPathResult.STRING_TYPE).stringValue;
                const description = cmd.evaluate(
                    "x:Description", thisOption, resolver, XPathResult.STRING_TYPE).stringValue;
                option = document.createElement("option");
                option.value = value;
                option.appendChild(document.createTextNode(description||value));
                forceLanguageG2P.appendChild(option)
                
                thisOption = options.iterateNext();
            } // next option
            forceLanguageG2P.value = orginalValue;
        } // there are values
    });
    request.open("GET", url);
    request.send();
}


// this function detects when the user selects [add new layer]:
function changedLayer(select, defaultNewLayerName) {
    if (select.value == "[add new layer]") {
        const newLayer = prompt("Please enter the new layer ID", defaultNewLayerName);
        if (newLayer) { // they didn't cancel
            // check there's not already a layer with that name
            for (let l in schema.layers) {
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

// ensure the right options are visible according to the service selected
function changeService() {
    const g2p = document.getElementById("service-G2P").checked;
    for (let div of document.getElementsByClassName("g2p")) {
        div.style.display = g2p?"":"none";
    } // next g2p element
    for (let div of document.getElementsByClassName("mausbasic")) {
        div.style.display = g2p?"none":"";
    } // next mausbasic element
    if (g2p) {
        // phonemeEncoding is required
        document.getElementById("pronunciationLayerId").setAttribute("required","");
    } else {
        // phonemeEncoding isn't required
        document.getElementById("pronunciationLayerId").removeAttribute("required");
    }
}

function validateRegularExpression(input) {
    if (input.value.length == 0) {
        input.className = "";
        input.title = "";
    } else {
        try {        
            // test regular expression is valid
            new RegExp(input.value);
            // pattern is valid, so don't mark it as an error
            input.className = "";
            input.title = "";
        } catch(error) {
            // pattern is invalid, so don't mark it as an error
            input.className = "error";
            input.title = error;
        }
    }
}

function validateParameters() {
    if (document.getElementById("targetLanguagePattern").value
        && !document.getElementById("transcriptLanguageLayerId").value) {
        alert("If you want to target a specific language,"
              +" you must select a layer that specifies the transcript's language");
        document.getElementById("transcriptLanguageLayerId").focus();
        return false;
    }
    if (document.getElementById("service-MAUSBasic").checked
        && !document.getElementById("forceLanguageMAUSBasic").value
        && !document.getElementById("transcriptLanguageLayerId").value) {
        alert("If you don't select a language to assume,"
              +" you must select a layer that specifies the transcript's language");
        document.getElementById("transcriptLanguageLayerId").focus();
        return false;
    }
    if (document.getElementById("service-G2P").checked
        && !document.getElementById("forceLanguageG2P").value
        && !document.getElementById("transcriptLanguageLayerId").value) {
        alert("If you don't select a language to assume,"
              +" you must select a layer that specifies the transcript's language");
        document.getElementById("transcriptLanguageLayerId").focus();
        return false;
    }
    return true;
}

document.getElementById("pronunciationLayerId").onchange = function(e) {
    changedLayer(this, taskId + "Pronunciation"); };
document.getElementById("wordAlignmentLayerId").onchange = function(e) {
    changedLayer(this, taskId + "Word"); };
document.getElementById("phoneAlignmentLayerId").onchange = function(e) {
    changedLayer(this, taskId + "Phone"); };
document.getElementById("utteranceTagLayerId").onchange = function(e) {
    changedLayer(this, taskId + "Time"); };
document.getElementById("service-G2P").onchange = changeService;
document.getElementById("service-MAUSBasic").onchange = changeService;
document.getElementById("targetLanguagePattern").onkeyup = function(e) {
    validateRegularExpression(this);
}
document.getElementById("taskParameters").onsubmit = validateParameters;
