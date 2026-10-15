startLoading();

// show annotator version
getVersion(version => {
  document.getElementById("version").innerHTML = version;
});

// Code editor
let editor = null;

const taskId = window.location.search.substring(1);

// first, get the layer schema
let schema = null;
getSchema(s => {
  schema = s;
  
  // populate layer input select options...          
  const tokenLayerId = document.getElementById("tokenLayerId");
  addLayerOptions(
    tokenLayerId, schema,
    // this is a function that takes a layer and returns true for the ones we want
    layer => layer.id == schema.wordLayerId
      || layer.parentId == schema.wordLayerId);
  // default value:
  if (schema.layers["orthography"]) {
    tokenLayerId.value = "orthography";
  } else {
    tokenLayerId.value = schema.wordLayerId;
  }
  
  // populate the language layers...
  
  const transcriptLanguageLayerId = document.getElementById("transcriptLanguageLayerId");
  addLayerOptions(
    transcriptLanguageLayerId, schema,
    layer => layer.parentId == schema.root.id && layer.alignment == 0
      && /.*lang.*/.test(layer.id));
  // select the first one by default
  transcriptLanguageLayerId.selectedIndex = 1;
  
  const phraseLanguageLayerId = document.getElementById("phraseLanguageLayerId");
  addLayerOptions(
    phraseLanguageLayerId, schema,
    layer => layer.parentId == schema.turnLayerId && layer.alignment == 2
      && /.*lang.*/.test(layer.id));
  // select the first one by default
  phraseLanguageLayerId.selectedIndex = 1;
  
  // populate layer output select options...          
  const tagLayerId = document.getElementById("tagLayerId");
  addLayerOptions(
    tagLayerId, schema,
    layer => layer.parentId == schema.wordLayerId && layer.alignment == 0);
  tagLayerId.selectedIndex = 0;
  
  // GET request to getTaskParameters retrieves the current task parameters, if any
  getText("getTaskParameters", text => {
    try {
      if (!text) { // new task
        // set some sensible defaults
        document.getElementById("targetLanguagePattern").value = "en.*";
        
      } else { // there are parameters
        
        const parameters = new URLSearchParams(text);
        
        // set initial values of properties in the form above
        // (this assumes bean property names match input id's in the form above)
        for (const [key, value] of parameters) {
          try {
            document.getElementById(key).value = value;
          } catch (x) {
          }
        }
        // set the checkbox
        document.getElementById("firstVariantOnly").checked
          = parameters.get("firstVariantOnly");
      }
      
      // if there's no pronunciation layer defined
      if (tagLayerId.selectedIndex == 0) {
        // but there's a layer named after the task
        if (schema.layers[taskId]) {
          
          // select that layer by default
          tagLayerId.value = taskId;
        } else if (/.+:.+/.test(taskId)) { // might be an 'auxiliary'?
          const layerId = taskId.replace(/:.+/,"");
          if (schema.layers[layerId]) { // there's a layer named after the task
            // select it
            tagLayerId.value = layerId;
          }
        }
      } // no tag layer defined
      
      // if there's no option for the output layer, add one
      if (tagLayerId.value != taskId && !/.+:.+/.test(taskId)) {
        const layerOption = document.createElement("option");
        layerOption.appendChild(document.createTextNode(taskId));
        tagLayerId.appendChild(layerOption);
        tagLayerId.value = taskId;
      }
      
      editor = CodeMirror.fromTextArea(
        document.getElementById("sql"), { 
          mode: "text/x-mysql",
          indentWithTabs: true,
          smartIndent: true,
          lineNumbers: false,
          matchBrackets : true
        });
      editor.setSize(500, 300);
      editor.on("change", function(cm, change) {
        const sql = document.getElementById("sql");
        sql.value = cm.getValue();
        setOptionForSql(sql);
        deferredTestSql();
      });
      
      setOptionForSql(document.getElementById("sql"));
      testSql();
      
    } finally {
      finishedLoading();
    }
  });
});

// this function detects when the user selects [add new layer]:
function changedLayer(select) {
  if (select.value == "[add new layer]") {
    const newLayer = prompt("Please enter the new layer ID", taskId);
    if (newLayer) { // they didn't cancel
      // check there's not already a layer with that name
      for (let l in schema.layers) {
        const layer = schema.layers[l];
        if (layer.id == newLayer) {
          alert("A layer called "+newLayer+" already exists");
          select.selectedIndex = 0;
          return;
        }
      } // next layer
      // add the layer to the list
      const layerOption = document.createElement("option");
      layerOption.appendChild(document.createTextNode(newLayer));
      select.appendChild(layerOption);
      // select it
      select.selectedIndex = select.children.length - 1;
    }
  }
}

// Presets
const optionIdToSql = {
  optSyllablesFromPhonology: "SELECT PhonStrsDISC"
    +"\n FROM cxde_wordformphonology"
    +"\n WHERE BINARY REPLACE(REPLACE(REPLACE(PhonStrsDISC, '-',''), '\"',''),'\\'','') = BINARY ?"
    +"\n ORDER BY LOCATE('\'', PhonStrsDISC) DESC",
  optPhonology: "SELECT REPLACE(REPLACE(REPLACE(PhonStrsDISC, '-',''), '\"',''),'\\\'','')"
    +"\n FROM cxde_wordformphonology"
    +"\n INNER JOIN cxde_wordform ON cxde_wordformphonology.IdNum = cxde_wordform.IdNum"
    +"\n WHERE cxde_wordform.WordDia = ?",
  optSyllableCount: "SELECT CHAR_LENGTH(REPLACE(REPLACE(REPLACE(REPLACE(PhonCVBr, 'V',''), 'C',''), 'S',''),']',''))"
    +"\n FROM cxde_wordformphonology"
    +"\n INNER JOIN cxde_wordform"
    +"\n ON cxde_wordformphonology.IdNum = cxde_wordform.IdNum"
    +"\n WHERE cxde_wordform.WordDia = ?"
    +"\n ORDER BY cxde_wordform.Mann DESC, cxde_wordform.IdNum",
  optMorphology: "SELECT"
    +"\n COALESCE(CONCAT(COALESCE(cxde_lemmamorphologyparse.Imm,cxde_lemma.HeadDia,''), ' ', REPLACE(cxde_wordformmorphology.FlectTypeDescription, ',', CONCAT('|', COALESCE(cxde_lemmamorphologyparse.Imm,cxde_lemma.HeadDia,''), ' '))),'')"
    +"\n FROM cxde_wordformmorphology"
    +"\n INNER JOIN cxde_wordform ON cxde_wordformmorphology.IdNum = cxde_wordform.IdNum"
    +"\n INNER JOIN cxde_lemma ON cxde_wordform.IdNumLemma = cxde_lemma.IdNumLemma"
    +"\n INNER JOIN cxde_lemmamorphologyparse ON cxde_wordform.IdNumLemma = cxde_lemmamorphologyparse.IdNumLemma"
    +"\n WHERE cxde_wordform.WordDia = ?"
    +"\n ORDER BY cxde_lemma.Mann DESC",
  optSyntax: "SELECT cxde_wordclass.Label"
    +"\n FROM cxde_wordform"
    +"\n INNER JOIN cxde_lemma ON cxde_wordform.IdNumLemma = cxde_lemma.IdNumLemma"
    +"\n INNER JOIN cxde_lemmasyntax ON cxde_wordform.IdNumLemma = cxde_lemmasyntax.IdNumLemma"
    +"\n INNER JOIN cxde_wordclass ON cxde_lemmasyntax.ClassNum = cxde_wordclass.ClassNum"
    +"\n WHERE cxde_wordform.WordDia = ?"
    +"\n ORDER BY cxde_lemma.Mann DESC",
  optWordFormFrequency: "SELECT Mann"
    +"\n FROM cxde_wordform"
    +"\n WHERE cxde_wordform.WordDia = ?"
    +"\n ORDER BY Mann DESC",
  optLemmaFrequency: "SELECT cxde_lemma.Mann"
    +"\n FROM cxde_wordform"
    +"\n INNER JOIN cxde_lemma"
    +"\n ON cxde_wordform.IdNumLemma = cxde_lemma.IdNumLemma"
    +"\n WHERE cxde_wordform.WordDia = ?"
    +"\n ORDER BY cxde_lemma.Mann DESC"
};

// reverse lookup
const sqlToOptionId = {};
for (let optionId in optionIdToSql) {
  sqlToOptionId[optionIdToSql[optionId]] = optionId;
}

function setSqlForOption(opt) {
  const sql = document.getElementById("sql");
  if (opt) {
    if (opt.checked) {
      sql.value = optionIdToSql[opt.id];
      if (document.getElementById("firstVariantOnly").checked) {
        sql.value += "\nLIMIT 1";
      }
      document.getElementById('delimiters').disabled = opt.id != "optSyllablesFromPhonology";
    }
  } else { // no specific option, but still should set firstVariantOnly
    if (document.getElementById("firstVariantOnly").checked) {
      sql.value += "\nLIMIT 1";
    } else {
      sql.value = sql.value.replace(/\nLIMIT 1$/,"");
    }
  }
  editor.setValue(sql.value);    
  testSql();
}

function getOptionForSql(sql) {
  const optionId = sqlToOptionId[sql.value.replace(/\nLIMIT 1$/,"")];
  if (optionId) {
    return document.getElementById(optionId);
  } else {
    return null;
  }
}

function setOptionForSql(sql) {
  const option = getOptionForSql(sql);
  if (option) { // check the corresponding option
    option.checked = true;
    document.getElementById('delimiters').disabled
      = option.id != "optSyllablesFromPhonology";
  } else { // ensure all options are unticked
    for (let optionId in optionIdToSql) {
      document.getElementById(optionId).checked = false;
    }
  }
  document.getElementById("firstVariantOnly").checked = sql.value.match(/\nLIMIT 1$/)?true:false;

}

function setSqlForFirstOnly() {
   setSqlForOption(getOptionForSql(document.getElementById("sql")));
}

let testTimeout = null;
function deferredTestSql() {
  if (testTimeout) clearTimeout(testTimeout);
  testTimeout = setTimeout("testSql();", 1000);
}

function testSql() {
  testTimeout = null;
  const testWord = document.getElementById("test-word").value;
  const sql = document.getElementById("sql").value;
  document.getElementById("test-result").className = "loading";
  getJSON(resourceForFunction("testSql", testWord, sql), matches => {
    document.getElementById("test-result").className = null;
    const testResult = document.getElementById("test-result");
    // empty it
    while (testResult.firstChild) testResult.removeChild(testResult.firstChild);
    
    // results
    if (matches) {
      for (let match of matches) {
        const div = document.createElement("div");
        if (match.startsWith("ERROR:")) div.className = "error";
        div.appendChild(document.createTextNode(match));
        testResult.appendChild(div);
      } // next match
    }
  });
} // testSql

document.getElementById("tagLayerId").onchange = function(e) {
  changedLayer(this); };
document.getElementById("optPhonology").onclick = function(e) {
  setSqlForOption(this); };
document.getElementById("optSyllableCount").onclick = function(e) {
  document.getElementById('firstVariantOnly').checked = true;
  setSqlForOption(this);
};
document.getElementById("optMorphology").onclick = function(e) {
  setSqlForOption(this); };
document.getElementById("optSyntax").onclick = function(e) {
  setSqlForOption(this); };
document.getElementById("optWordFormFrequency").onclick = function(e) {
  setSqlForOption(this); };
document.getElementById("optLemmaFrequency").onclick = function(e) {
  setSqlForOption(this); };
document.getElementById("optSyllablesFromPhonology").onclick = function(e) {
  setSqlForOption(this);
  document.getElementById('delimiters').value = '-';
  document.getElementById('tokenLayerId').value = "segment"; // assuming it's there
};
document.getElementById("firstVariantOnly").onclick = function(e) {
  setSqlForFirstOnly(); };
document.getElementById("test-word").onkeyup = function(e) {
  deferredTestSql(); };

// add taskId to schema link so LaBB-CAT can instantiate the annotator
document.getElementById("schema-link-help").href += `?${taskId}`;
document.getElementById("schema-link").href += `?${taskId}`;

