// show spinner
startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

const taskId = window.location.search.substring(1);

// original sub-mapping settings
var subSourceLayerId = null;
var subComparator = null;
// sub-mapping layer isn't required becase sub-mappings are tracked in the RDB
// var subMappingLayerId = null;
var subTargetLayerId = null;

// first, get the layer schema
var schema = null;
getSchema(s => {
    schema = s;
    
    // populate layer input select options...          
    var sourceLayerId = document.getElementById("sourceLayerId");
    addLayerOptions(
        sourceLayerId, schema,
        // word layers
        layer => layer.id == schema.wordLayerId || layer.parentId == schema.wordLayerId
        // or phrase layers
            || (layer.alignment == 2 && layer.parentId == schema.turnLayerId
                && layer.id != schema.utteranceLayerId));
    var mappingLayerId = document.getElementById("mappingLayerId");
    sourceLayerId.selectedIndex = 0; // force selection
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
    var targetLayerId = document.getElementById("targetLayerId");
    addLayerOptions(
        targetLayerId, schema,
        // word layers
        layer => layer.id == schema.wordLayerId || layer.parentId == schema.wordLayerId
        // or phrase layers
            || (layer.alignment == 2 && layer.parentId == schema.turnLayerId
                && layer.id != schema.utteranceLayerId));
    targetLayerId.selectedIndex = 0; // force selection

    // sub-mapping layer isn't required becase sub-mappings are tracked in the RDB
    // document.getElementById("subMappingLayerId").selectedIndex = 0; // force selection

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
            // fill out sub layer options
            changedSourceLayer(sourceLayerId);
            changedTargetLayer(targetLayerId);

            subSourceLayerId = parameters.get("subSourceLayerId");
            subComparator = parameters.get("subComparator");
            subTargetLayerId = parameters.get("subTargetLayerId");

            // set values that couldn't be set properly before because there were no options set
            document.getElementById("subSourceLayerId").value = subSourceLayerId;
            document.getElementById("subTargetLayerId").value = subTargetLayerId;
            
            // check submapping if all of these are set
            document.getElementById("submapping").checked
                = subSourceLayerId && subComparator && subTargetLayerId;
            // sub-mapping layer isn't required becase sub-mappings are tracked in the RDB
            // subMappingLayerId = parameters.get("subMappingLayerId");

            // set splitLabels value
            try {
                document.getElementById("splitLabels-"+parameters.get("splitLabels")).checked = true;
            } catch( x) {
                console.log("Invalid splitLabels value: \""+parameters.get("splitLabels")+"\"");
            }

            // if there's no utterance tag layer defined
            if (mappingLayerId.selectedIndex == 0
                // but there's a layer named after the task
                && schema.layers[taskId]
                // and there was no initial task configuration
                && !text) {
                
                // select that layer by default
                mappingLayerId.value = taskId;
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

function changedSourceLayer(select) {
    const sourceLayer = schema.layers[select.value];
    if (sourceLayer) {
        if (sourceLayer.parentId == schema.turnLayerId) { // phrase layer
            // they probably don't want to split labels
            document.getElementById("splitLabels-").checked = true;
        } else if (sourceLayer.parentId == schema.wordLayerId
                   && sourceLayer.alignment == 0) { // word tag layer
            // they probably want to split labels
            // TODO if type == IPA: char, otherwise: space
            document.getElementById("splitLabels-char").checked = true;
        }            
            
        defaultComparator();
        // TODO filter possible token layers
        
        // set possible sub-mapping label layers
        document.getElementById("subSourceLayerId").innerHTML
            = "<option disabled value=''>[select layer]</option>"; // remove existing options
        if (sourceLayer.parentId == schema.turnLayerId
            && sourceLayer.id != schema.wordLayerId) { // phrase layer
            // sub labels can only be from phrase layers
            addLayerOptions(
                document.getElementById("subSourceLayerId"), schema,
                layer => layer.alignment == 2 && layer.parentId == schema.turnLayerId
                    && layer.id != schema.wordLayerId && layer.id != schema.utteranceLayerId
                    && layer.id != sourceLayer.id);
        } else { // presumably a word or segment layer
            // sub labels can only be word layers
            addLayerOptions(
                document.getElementById("subSourceLayerId"), schema,
                layer => layer.alignment == 2 && layer.parentId == schema.wordLayerId
                    && layer.id != sourceLayer.id);        
        }
        document.getElementById("subSourceLayerId").value = subSourceLayerId;
    }
    
}
function changedTargetLayer(select) {
    defaultComparator();
    
    const targetLayer = schema.layers[select.value];
    if (targetLayer) {
        // set possible sub-mapping token/mapping layers
        document.getElementById("subTargetLayerId").innerHTML
            = "<option disabled value=''>[select layer]</option>"; // remove existing options
        if (targetLayer.parentId == schema.turnLayerId
            && targetLayer.id != schema.wordLayerId) { // phrase layer
            // sub tokens can only be from phrase layers
            addLayerOptions(
                document.getElementById("subTargetLayerId"), schema,
                layer => layer.alignment == 2 && layer.parentId == schema.turnLayerId
                    && layer.id != schema.wordLayerId && layer.id != schema.utteranceLayerId
                    && layer.id != targetLayer.id);
            // sub-mapping layer isn't required becase sub-mappings are tracked in the RDB
            // // sub mappings can only be to phrase layers
            // addLayerOptions(
            //     document.getElementById("subMappingLayerId"), schema,
            //     layer => layer.alignment == 2 && layer.parentId == schema.turnLayerId
            //         && layer.id != schema.wordLayerId && layer.id != schema.utteranceLayerId
            //         && layer.id != targetLayer.id);        
        } else { // presumably a segment layer
            // sub tokens can only be word layers
            addLayerOptions(
                document.getElementById("subTargetLayerId"), schema,
                layer => layer.alignment == 2 && layer.parentId == schema.wordLayerId
                    && layer.id != targetLayer.id);
            // sub-mapping layer isn't required becase sub-mappings are tracked in the RDB
            // // sub mappings can only be word layers
            // addLayerOptions(
            //     document.getElementById("subMappingLayerId"), schema,
            //     layer => layer.alignment == 2 && layer.parentId == schema.wordLayerId
            //         && layer.id != targetLayer.id);        
        }
        document.getElementById("subTargetLayerId").value = subTargetLayerId;
        // sub-mapping layer isn't required becase sub-mappings are tracked in the RDB
        // document.getElementById("subMappingLayerId").value = subMappingLayerId;
    } // targetLayer exists
}
function defaultComparator() {
    var sourceLayerId = document.getElementById("sourceLayerId");
    var targetLayerId = document.getElementById("targetLayerId");
    var sourceLayer = schema.layers[sourceLayerId.value];
    var targetLayer = schema.layers[targetLayerId.value];
    var defaultComparator = "OrthographyToArpabet";
    if (sourceLayer && targetLayer) {
        if (sourceLayer.parentId == schema.turnLayerId
            || targetLayer.parentId == schema.turnLayerId) {
            defaultComparator = "CharacterToCharacter";
            document.getElementById("splitLabels-").checked = true;
        } else if (sourceLayer.type == "ipa" && targetLayer.type == "ipa") {
            defaultComparator = "DISCToDISC";
            document.getElementById("splitLabels-char").checked = true;
        } else if (sourceLayer.type == "string" && targetLayer.type == "ipa") {
            if (sourceLayer.id.toLowerCase().includes("arpabet")
                || sourceLayer.id.toLowerCase().includes("cmu")) { // may be an ARPAbet layer
                defaultComparator = "ArpabetToDISC";
                document.getElementById("splitLabels-space").checked = true;
            } else {
                defaultComparator = "OrthographyToDISC";
                document.getElementById("splitLabels-space").checked = false;
            }
        } else if (sourceLayer.type == "ipa" && targetLayer.type == "string") {
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
    var exampleSource = "transcription"; // orthography
    var exampleMapping = "? ? ? ? ? ? ? ? ? ? ? ? ?"; // mapping
    var exampleTarget   = "t r a n s c r i p t i o n"; // orthography
    var exampleSubSource = "";
    var exampleSubMapping = "";
    var exampleSubTarget   = "";
    if (!submapping) { // simple mapping of one layer to the other
        switch (comparator.value) {
        case "OrthographyToDISC":
            exampleSource  = "transcription"; // orthography
            exampleMapping = "? ? ? ? ? ? ? ? ? ? ? ?"; // mapping
            exampleTarget  = "t r { n s k r I p S V n"; // DISC
            break;
        case "OrthographyToArpabet":
            exampleSource  = "transcription"; // orthography
            exampleMapping = "? ?  ?  ? ? ? ?  ?  ? ?   ?  ?"; // mapping
            exampleTarget  = "T R AE2 N S K R IH1 P SH AH0 N"; // ARPAbet
            break;
        case "DISCToDISC":
            exampleSource  = "tr{nskrIpSVn"; // DISC
            exampleMapping = "? ? ? ? ? ? ? ? ? ? ? ?"; // mapping
            exampleTarget  = "t r { n s k r I p S V n"; // DISC
            break;
        case "DISCToArpabet":
            exampleSource  = "tr{nskrIpSVn"; // DISC
            exampleMapping = "? ?  ?  ? ? ? ?  ?  ? ?   ?  ?"; // mapping
            exampleTarget  = "T R AE2 N S K R IH1 P SH AH0 N"; // ARPAbet
            break;
        case "ArpabetToDISC":
            exampleSource  = "T R AE2 N S K R IH1 P SH AH0 N"; // arpabet
            exampleMapping = "? ? ? ? ? ? ? ? ? ? ? ?"; // mapping
            exampleTarget  = "t r { n s k r I p S V n"; // DISC
            break;
        case "IPAToIPA":
            exampleSource  = "t ɹ æ n s k ɹ ɪ p ʃ ə n"; // IPA
            exampleMapping = "? ? ? ? ? ? ? ? ? ? ? ?"; // mapping
            exampleTarget  = "t ɹ æ n s k ɹ ɪ p ʃ ə n"; // IPA
            break;
        case "DISCToIPA":
            exampleSource  = "tr{nskrIpSVn"; // DISC
            exampleMapping = "? ? ? ? ? ? ? ? ? ? ? ?"; // mapping
            exampleTarget  = "t ɹ æ n s k ɹ ɪ p ʃ ə n"; // IPA
            break;
        case "IPAToDISC":
            exampleSource  = "t ɹ æ n s k ɹ ɪ p ʃ ə n"; // IPA
            exampleMapping = "? ? ? ? ? ? ? ? ? ? ? ?"; // mapping
            exampleTarget  = "t r { n s k r I p S V n"; // DISC
            break;
        }
    } else { // sub-mapping - two pairs of layers
        comparator = document.getElementById("subComparator");
        exampleSource  = "word            tokens"; // orthography
        exampleMapping = "?                ?";
        exampleTarget  = "word            tokens";
        switch (comparator.value) {
        case "DISCToDISC":
            exampleSubSource  = "w  3  d      t  5  k  @  n z"; // DISC
            exampleSubMapping = "?  ?  ?      ?  ?  ?  ?  ? ?"; // mapping
            exampleSubTarget  = "w  3  d      t  5  k  @  n z"; // DISC
            break;
        case "ArpabetToDISC":
            exampleSubSource  = "W ER1 D      T OW1 K AH0 N Z"; // arpabet
            exampleSubMapping = "?  ?  ?      ?  ?  ?  ?  ? ?"; // mapping
            exampleSubTarget  = "w  3  d      t  5  k  @  n z"; // DISC
            break;
        case "DISCToArpabet":
            exampleSubSource  = "w  3  d      t  5  k  @  n z"; // DISC
            exampleSubMapping = "?  ?  ?      ?  ?  ?  ?  ? ?"; // mapping
            exampleSubTarget  = "W ER1 D      T OW1 K AH0 N Z"; // arpabet
            break;
        case "IPAToIPA":
            exampleSubSource  = "w  ɜː  d      t  əʊ  k  ǝ  n z"; // IPA
            exampleSubMapping = "?  ?   ?      ?  ?   ?  ?  ? ?"; // mapping
            exampleSubTarget  = "w  ɜː  d      t  əʊ  k  ǝ  n z"; // IPA
            break;
        case "IPAToDISC":
            exampleSubSource  = "w ɜː d      t əʊ k ǝ n z"; // IPA
            exampleSubMapping = "?  ? ?      ? ? ? ? ? ?"; // mapping
            exampleSubTarget  = "w  3 d      t 5 k @ n z"; // DISC
            break;
        case "DISCToIPA":
            exampleSubSource  = "w 3 d      t  5 k @ n z"; // DISC
            exampleSubMapping = "? ?  ?      ?  ? ? ? ? ?"; // mapping
            exampleSubTarget  = "w ɜː d      t əʊ k ǝ n z"; // IPA
            break;
        }
    }
    document.getElementById("exampleSourceLabel").innerHTML = exampleSource;
    document.getElementById("exampleMappingLabel").innerHTML = exampleMapping;
    document.getElementById("exampleMapping").innerHTML = exampleMapping.replace(/\?/g,"↓");
    document.getElementById("exampleTargetLabel").innerHTML = exampleTarget;
    document.getElementById("exampleSubSourceLabel").innerHTML = exampleSubSource;
    // sub-mapping layer isn't required becase sub-mappings are tracked in the RDB
    // document.getElementById("exampleSubMappingLabel").innerHTML = exampleSubMapping;
    document.getElementById("exampleSubMapping").innerHTML = exampleSubMapping.replace(/\?/g,"↓");
    document.getElementById("exampleSubTargetLabel").innerHTML = exampleSubTarget;
}

function disenableSubMapping() { // disables or enables sub-mapping layers
    const submapping = document.getElementById("submapping");
    // sub-mapping only makes sense if there are multiple annotations on the primary label layer
    submapping.disabled = !document.getElementById("splitLabels-").checked
        || document.getElementById("comparator").value != "CharacterToCharacter";
    // layers can only be specified if submapping is selected
    document.getElementById("subSourceLayerId").disabled
        = document.getElementById("subComparator").disabled
    // sub-mapping layer isn't required becase sub-mappings are tracked in the RDB
    // = document.getElementById("subMappingLayerId").disabled
        = document.getElementById("subTargetLayerId").disabled
        = submapping.disabled || !submapping.checked;
    // hide mapping layer setting iff sub-mapping is selected 
    document.getElementById("mappingLayer").style.display 
        = (!submapping.disabled && submapping.checked)?"none":"";
    setComparatorExamples();
}

// add event handlers
document.getElementById("sourceLayerId").onchange = function(e) {
    changedSourceLayer(this); };
document.getElementById("mappingLayerId").onchange = function(e) {
    changedMappingLayer(this, window.location.search.substring(1)); };
// sub-mapping layer isn't required becase sub-mappings are tracked in the RDB
// document.getElementById("subMappingLayerId").onchange = function(e) {
//    changedMappingLayer(this, window.location.search.substring(1) + "Phone"); };
document.getElementById("targetLayerId").onchange = function(e) {
    changedTargetLayer(this); };
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
