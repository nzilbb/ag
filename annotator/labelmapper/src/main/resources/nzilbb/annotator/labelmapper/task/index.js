// show spinner
startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

// original sub-mapping settings
var subLabelLayerId = null;
var subComparator = null;
var subMappingLayerId = null;
var subTokenLayerId = null;

// first, get the layer schema
var schema = null;
getSchema(s => {
    schema = s;
    
    // populate layer input select options...          
    var labelLayerId = document.getElementById("labelLayerId");
    addLayerOptions(
        labelLayerId, schema,
        // word layers
        layer => layer.id == schema.wordLayerId || layer.parentId == schema.wordLayerId
        // or phrase layers
            || (layer.alignment == 2 && layer.parentId == schema.turnLayerId
                && layer.id != schema.utteranceLayerId));
    var mappingLayerId = document.getElementById("mappingLayerId");
    labelLayerId.selectedIndex = 0; // force selection
    addLayerOptions(
        mappingLayerId, schema,
        // word layers
        layer => layer.id == schema.wordLayerId || layer.parentId == schema.wordLayerId
        // or segment layers
            || (schema.layers[layer.parentId]
                && schema.layers[layer.parentId].parentId == schema.wordLayerId)
        // or phrase layers
            || (layer.alignment == 2 && layer.parentId == schema.turnLayerId
                && layer.id != schema.utteranceLayerId));
    mappingLayerId.selectedIndex = 0; // force selection
    var tokenLayerId = document.getElementById("tokenLayerId");
    addLayerOptions(
        tokenLayerId, schema,
        // word layers
        layer => layer.id == schema.wordLayerId || layer.parentId == schema.wordLayerId
        // or phrase layers
            || (layer.alignment == 2 && layer.parentId == schema.turnLayerId
                && layer.id != schema.utteranceLayerId));
    tokenLayerId.selectedIndex = 0; // force selection

    document.getElementById("subMappingLayerId").selectedIndex = 0; // force selection

    // GET request to getTaskParameters retrieves the current task parameters, if any
    getText("getTaskParameters", text => {
        try {
            var parameters = new URLSearchParams(text);
            
            // set initial values of properties in the form above
            // (this assumes bean property names match input id's in the form above)
            for (const [key, value] of parameters) {
                try {
                    document.getElementById(key).value = value;
                } catch(x) {
                }
            }

            subLabelLayerId = parameters.get("subLabelLayerId");
            subComparator = parameters.get("subComparator");
            subTokenLayerId = parameters.get("subTokenLayerId");
            // check submapping if all of these are set
            document.getElementById("submapping").checked
                = subLabelLayerId && subComparator && subTokenLayerId;
            subMappingLayerId = parameters.get("subMappingLayerId");

            // set splitLabels value
            try {
                document.getElementById("splitLabels-"+parameters.get("splitLabels")).checked = true;
            } catch( x) {
                console.log(`Invalid splitLabels value: "${parameters.splitLabels}"`);
            }

            // setup initial UI
            setComparatorExamples(document.getElementById("comparator"));
            disenableSubMapping();
        } finally {
            // hide spinner
            finishedLoading();
        }
    });
});

function changedLabelLayer(select) {
    const labelLayer = schema.layers[select.value];
    if (labelLayer) {
        if (labelLayer.parentId == schema.turnLayerId) { // phrase layer
            // they probably don't want to split labels
            document.getElementById("splitLabels-").checked = true;
        } else if (labelLayer.parentId == schema.wordLayerId
                   && labelLayer.alignment == 0) { // word tag layer
            // they probably want to split labels
            // TODO if type == IPA: char, otherwise: space
            document.getElementById("splitLabels-char").checked = true;
        }            
    }
            
    defaultComparator();
    // TODO filter possible token layers

    // set possible sub-mapping label layers
    document.getElementById("subLabelLayerId").innerHTML
        = "<option disabled value=''>[select layer]</option>"; // remove existing options
    if (labelLayer.parentId == schema.turnLayerId
        && labelLayer.id != schema.wordLayerId) { // phrase layer
        // sub labels can only be from phrase layers
        addLayerOptions(
            document.getElementById("subLabelLayerId"), schema,
            layer => layer.alignment == 2 && layer.parentId == schema.turnLayerId
                && layer.id != schema.wordLayerId && layer.id != schema.utteranceLayerId
                && layer.id != labelLayer.id);
    } else { // presumably a word or segment layer
        // sub labels can only be word layers
        addLayerOptions(
            document.getElementById("subLabelLayerId"), schema,
            layer => layer.alignment == 2 && layer.parentId == schema.wordLayerId
                && layer.id != labelLayer.id);        
    }
    document.getElementById("subLabelLayerId").value = subLabelLayerId;
}
function changedTokenLayer(select) {
    defaultComparator();
    
    const tokenLayer = schema.layers[select.value];
    // set possible sub-mapping token/mapping layers
    document.getElementById("subTokenLayerId").innerHTML
        = "<option disabled value=''>[select layer]</option>"; // remove existing options
    if (tokenLayer.parentId == schema.turnLayerId
        && tokenLayer.id != schema.wordLayerId) { // phrase layer
        // sub tokens can only be from phrase layers
        addLayerOptions(
            document.getElementById("subTokenLayerId"), schema,
            layer => layer.alignment == 2 && layer.parentId == schema.turnLayerId
                && layer.id != schema.wordLayerId && layer.id != schema.utteranceLayerId
                && layer.id != tokenLayer.id);        
        // sub mappings can only be to phrase layers
        addLayerOptions(
            document.getElementById("subMappingLayerId"), schema,
            layer => layer.alignment == 2 && layer.parentId == schema.turnLayerId
                && layer.id != schema.wordLayerId && layer.id != schema.utteranceLayerId
                && layer.id != tokenLayer.id);        
    } else { // presumably a segment layer
        // sub tokens can only be word layers
        addLayerOptions(
            document.getElementById("subTokenLayerId"), schema,
            layer => layer.alignment == 2 && layer.parentId == schema.wordLayerId
                && layer.id != tokenLayer.id);        
        // sub mappings can only be word layers
        addLayerOptions(
            document.getElementById("subMappingLayerId"), schema,
            layer => layer.alignment == 2 && layer.parentId == schema.wordLayerId
                && layer.id != tokenLayer.id);        
    }
    document.getElementById("subTokenLayerId").value = subTokenLayerId;
    document.getElementById("subMappingLayerId").value = subMappingLayerId;
}
function defaultComparator() {
    var labelLayerId = document.getElementById("labelLayerId");
    var tokenLayerId = document.getElementById("tokenLayerId");
    var labelLayer = schema.layers[labelLayerId.value];
    var tokenLayer = schema.layers[tokenLayerId.value];
    var defaultComparator = "OrthographyToArpabet";
    if (labelLayer && tokenLayer) {
        if (labelLayer.type == "ipa" && tokenLayer.type == "ipa") {
            defaultComparator = "DISCToDISC";
            document.getElementById("splitLabels-char").checked = true;
        } else if (labelLayer.type == "string" && tokenLayer.type == "ipa") {
            if (labelLayer.id.toLowerCase().includes("arpabet")
                || labelLayer.id.toLowerCase().includes("cmu")) { // may be an ARPAbet layer
                defaultComparator = "ArpabetToDISC";
                document.getElementById("splitLabels-space").checked = true;
            } else {
                defaultComparator = "OrthographyToDISC";
                document.getElementById("splitLabels-space").checked = false;
            }
        } else if (labelLayer.type == "ipa" && tokenLayer.type == "string") {
            defaultComparator = "DISCToArpabet";
            document.getElementById("splitLabels-char").checked = true;
        }
    }
    document.getElementById("comparator").value = defaultComparator;

    // comparator also affects enablement of sub-mapping
    disenableSubMapping();
}
// this function detects when the user selects [add new layer]:
function changedMappingLayer(select, defaultNewLayerId) {
    if (select.value == "[add new layer]") {
        var newLayer = prompt("Please enter the new layer ID", defaultNewLayerId);
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

// this function detects when the user changes the comparator, and shows an example:
function setComparatorExamples() {
    var comparator = document.getElementById("comparator");
    var submapping = !document.getElementById("submapping").disabled
        && document.getElementById("submapping").checked;
    // CharacterToCharacter by default
    var exampleLabel = "transcription"; // orthography
    var exampleMapping = "? ? ? ? ? ? ? ? ? ? ? ? ?"; // mapping
    var exampleToken   = "t r a n s c r i p t i o n"; // orthography
    var exampleSubLabel = "";
    var exampleSubMapping = "";
    var exampleSubToken   = "";
    if (!submapping) { // simple mapping of one layer to the other
        switch (comparator.value) {
        case "OrthographyToDISC":
            exampleLabel = "transcription"; // orthography
            exampleMapping = "? ? ? ? ? ? ? ? ? ? ? ?"; // mapping
            exampleToken   = "t r { n s k r I p S V n"; // DISC
            break;
        case "ArpabetToDISC":
            exampleLabel = "T R AE2 N S K R IH1 P SH AH0 N"; // arpabet
            exampleMapping = "? ? ? ? ? ? ? ? ? ? ? ?"; // mapping
            exampleToken   = "t r { n s k r I p S V n"; // DISC
            break;
        case "OrthographyToArpabet":
            exampleLabel = "transcription"; // orthography
            exampleMapping = "? ?  ?  ? ? ? ?  ?  ? ?   ?  ?"; // mapping
            exampleToken   = "T R AE2 N S K R IH1 P SH AH0 N"; // ARPAbet
            break;
        case "DISCToDISC":
            exampleLabel = "tr{nskrIpSVn"; // DISC
            exampleMapping = "? ? ? ? ? ? ? ? ? ? ? ?"; // mapping
            exampleToken   = "t r { n s k r I p S V n"; // DISC
            break;
        case "DISCToArpabet":
            exampleLabel = "tr{nskrIpSVn"; // DISC
            exampleMapping = "? ?  ?  ? ? ? ?  ?  ? ?   ?  ?"; // mapping
            exampleToken   = "T R AE2 N S K R IH1 P SH AH0 N"; // ARPAbet
            break;
        }
    } else { // sub-mapping - two pairs of layers
        comparator = document.getElementById("subComparator");
        exampleLabel   = "word            tokens"; // orthography
        exampleMapping = "?                ?";
        exampleToken   = "word            tokens";
        switch (comparator.value) {
        case "ArpabetToDISC":
            exampleSubLabel   = "W ER1 D      T OW1 K AH0 N Z"; // arpabet
            exampleSubMapping = "?  ?  ?      ?  ?  ?  ?  ? ?"; // mapping
            exampleSubToken   = "w  3  d      t  5  k  @  n z"; // DISC
            break;
        case "DISCToDISC":
            exampleSubLabel   = "w  3  d      t  5  k  @  n z"; // DISC
            exampleSubMapping = "?  ?  ?      ?  ?  ?  ?  ? ?"; // mapping
            exampleSubToken   = "w  3  d      t  5  k  @  n z"; // DISC
            break;
        case "DISCToArpabet":
            exampleSubLabel   = "w  3  d      t  5  k  @  n z"; // DISC
            exampleSubMapping = "?  ?  ?      ?  ?  ?  ?  ? ?"; // mapping
            exampleSubToken   = "W ER1 D      T OW1 K AH0 N Z"; // arpabet
            break;
        }
    }
    document.getElementById("exampleLabelLabel").innerHTML = exampleLabel;
    document.getElementById("exampleMappingLabel").innerHTML = exampleMapping;
    document.getElementById("exampleMapping").innerHTML = exampleMapping.replace(/\?/g,"↓");
    document.getElementById("exampleTokenLabel").innerHTML = exampleToken;
    document.getElementById("exampleSubLabelLabel").innerHTML = exampleSubLabel;
    document.getElementById("exampleSubMappingLabel").innerHTML = exampleSubMapping;
    document.getElementById("exampleSubMapping").innerHTML = exampleSubMapping.replace(/\?/g,"↓");
    document.getElementById("exampleSubTokenLabel").innerHTML = exampleSubToken;
}

function disenableSubMapping() { // disables or enables sub-mapping layers
    const submapping = document.getElementById("submapping");
    // sub-mapping only makes sense if there are multiple annotations on the primary label layer
    submapping.disabled = !document.getElementById("splitLabels-").checked
        || document.getElementById("comparator").value != "CharacterToCharacter";
    // layers can only be specified if submapping is selected
    document.getElementById("subLabelLayerId").disabled
        = document.getElementById("subComparator").disabled
        = document.getElementById("subMappingLayerId").disabled
        = document.getElementById("subTokenLayerId").disabled
        = submapping.disabled || !submapping.checked;
    setComparatorExamples();
}

// add event handlers
document.getElementById("labelLayerId").onchange = function(e) {
    changedLabelLayer(this); };
document.getElementById("mappingLayerId").onchange = function(e) {
    changedMappingLayer(this, window.location.search.substring(1)); };
document.getElementById("subMappingLayerId").onchange = function(e) {
    changedMappingLayer(this, window.location.search.substring(1) + "Phone"); };
document.getElementById("tokenLayerId").onchange = function(e) {
    changedTokenLayer(this); };
document.getElementById("comparator").onchange = function(e) {
    setComparatorExamples();
    disenableSubMapping();
};
document.getElementById("submapping").onchange = function(e) {
    disenableSubMapping(); };
document.getElementById("subComparator").onchange = function(e) {
    setComparatorExamples(); };
document.getElementById("splitLabels-").onchange = function(e) {
    return disenableSubMapping(); };
document.getElementById("splitLabels-char").onchange = function(e) {
    return disenableSubMapping(); };
document.getElementById("splitLabels-space").onchange = function(e) {
    return disenableSubMapping(); };
