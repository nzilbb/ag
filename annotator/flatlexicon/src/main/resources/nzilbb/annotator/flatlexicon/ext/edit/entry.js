import {
  ClassicEditor, Essentials,
  Paragraph, Heading, Bold, Italic, Underline, BlockQuote, Font, Link, AutoLink,
  HorizontalLine, List, Table, TableToolbar, Indent, IndentBlock 
} from 'ckeditor5';

startLoading();

// show annotator version
getVersion(version => {
    document.getElementById("version").innerHTML = version;
});

// parse parameters
const parameters = new URLSearchParams(window.location.search);
const lexicon = parameters.get("l") || parameters.get("lexicon");
const field = parameters.get("f") || parameters.get("field");
const entry = parameters.get("e") || parameters.get("entry");
console.log(`lexicon "${lexicon}" field "${field}" entry "${entry}"`);

document.getElementById("entry").innerText = entry;

const fields = {}; // definitions for fields (type/validation etc.)

function showForm(data) {
  if (data.FlatLexiconTagger_error) {
    document.getElementById("error").innerText = data.FlatLexiconTagger_error;
    return;
  }
  const attributes = document.getElementById("attributes");
  attributes.innerHTML = ""; // clear any existing list
  for (let name in fields) {
    const row = document.createElement("div");
    row.className = "field";
    row.title = name;
    let label = document.createElement("label");
    label.appendChild(document.createTextNode(name));
    row.appendChild(label);
    
    let value = document.createElement("span");
    value.className = "value";
    if (name == field) { // the ID is read-only
      let span = document.createElement("span");
      span.className = "read-only";
      span.id = `attribute-${name}`;
      span.appendChild(document.createTextNode(data[name]));
      value.appendChild(span);
    } else {
      let input = document.createElement("input");

      // default text input
      input.type = "text";
      // refine type constraints
      if (fields[name].type == "integer") {
        input.type = "number";
        input.step = 1;
      } else if (fields[name].type == "number") {
        input.type = "number";
        input.step = 0.1;
      } else if (fields[name].type == "url") {
        input.type = "url";
        let a = document.createElement("a");
        a.className = "url-link";
        a.title = `Open ${name}`;
        a.target = name;
        a.href = "#";
        a.onclick = function(e) {
          if (input.value) {
            this.href = input.value;
            return true;
          } else {
            e.preventDefault();
            return false;
          }
        }
        a.appendChild(document.createTextNode("🡽"));
        value.appendChild(a);
      } else if (fields[name].type == "email") {
        input.type = "email";
      } else if (fields[name].type == "date") {
        input.type = "date";
      } else if (fields[name].type == "datetime") {
        input.type = "datetime-local";
      } else if (fields[name].type == "boolean" && !fields[name].validation) {
        fields[name].validation = "|false|true";
      } else if (fields[name].type == "text") {
        input = document.createElement("textarea");
      } else if (fields[name].type == "richtext") { 
        input = document.createElement("textarea"); // attach the editor later...
      }
      if (fields[name].validation) {
        // if it's just a list of alternative, e.g. "option1|option2|option3"
        if (/^\|?(\w+\|)+\w+/.test(fields[name].validation)) { // implement as a select instead
          input = document.createElement("select");
          for (let opt of fields[name].validation.split("|")) {
            const option = document.createElement("option");
            option.appendChild(document.createTextNode(opt));
            input.appendChild(option);
          } // next option
        } else if (fields[name].validation.split("<").length == 2) { // min<max
          const interval = fields[name].validation.split("<");
          if (interval[0]) input.min = interval[0];
          if (interval[1]) input.max = interval[1];
        } else {
          input.pattern = fields[name].validation;
        }
      }

      input.name = name;
      input.id = `attribute-${name}`;
      input.placeholder = name;
      input.value = data[name] || "";
      input.onkeyup = input.onchange = function(e) {
        // show save button
        document.getElementById("btnSave").style.display = null;
      }
      
      value.appendChild(input);

      if (fields[name].type == "richtext") {
        ClassicEditor
          .create( {
	    attachTo: input,
            licenseKey: 'GPL',
            plugins: [
              Essentials,
              Paragraph, Heading, Bold, Italic, Underline, BlockQuote, Font, Link, AutoLink,
              HorizontalLine, List, Table, TableToolbar, Indent, IndentBlock  ],
            toolbar: [
	      // 'undo', 'redo', '|',
              'bold', 'italic', 'underline', '|',
              'outdent', 'indent', 'bulletedList', 'numberedList', 'blockquote',
	      // 'fontSize', 'fontFamily', 'fontColor', '|',
              'link', 'insertTable', 'horizontalLine'
	    ],
            table: {
	      contentToolbar: [
	        'tableColumn',
	        'tableRow',
	        'mergeTableCells'
	      ]
            },
	    licenseKey: 'GPL'
	  } )
	  .then( editor => {
	    input.editor = editor;
            editor.model.document.on( 'change:data', () => {
              // show save button
              document.getElementById("btnSave").style.display = null;
            } );
	  } )
	  .catch( error => {
	    console.error( error );
	  } );        
      }
    }
    row.appendChild(value);
    attributes.appendChild(row);
  }
  // hide save button
  document.getElementById("btnSave").style.display = "none";
}

// get field definitions
getJSON(resourceForFunction("readFields", lexicon), definitions => {

  // build a dictionary of definitions
  for (let definition of definitions) {
    if (definition.field) {
      fields[definition.field] = definition;
    }
  } // next field definition

  // get all field values for entry
  getJSON(resourceForFunction("readEntry", lexicon, field, entry), data => {
    startLoading();
    showForm(data);
    finishedLoading();
  });

});

document.getElementById("btnSave").onclick = function (e) {
  document.getElementById("error").innerText = "";
  var fd = new FormData();
  fd.append("l", lexicon);
  fd.append("f", field);
  fd.append("e", entry);
  const data = {};
  for (let name in fields) {
    const input = document.getElementById(`attribute-${name}`);    
    if (input && input.value) {
      if (name != field // not read only
          && !input.checkValidity()) { // not valid    
        input.reportValidity();
        return;
      }
      if (input.editor) {
        data[name] = input.editor.getData();
      } else {
        data[name] = input.value;
      }
    }
  } // next field
  fd.append("data", JSON.stringify(data));
  startLoading();
  postForm("updateEntry", fd, function(e) {
    showForm(JSON.parse(this.responseText));
    finishedLoading();
  });
}
