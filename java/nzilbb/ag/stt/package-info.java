/**
 * These are classes and interfaces for supporting automated transcription modules.

<style type="text/css"> summary { font-weight: bold; } </style>

<p> Transcriber modules can be defined which automatically transcribe audio using Speech
  to Text (STT) software; either using locally installed software or libraries, or web
  services, etc. </p>
<p> Such a module must be packaged in a .jar file with the following: 
  <ul>
    <li> a class that implements the {@link Transcriber} interface, </li>
    <li> a manifest attribute called <q>nzilbb-ag-stt-Transcriber</q> whose value
      is the fully-qualified class name of the Transcrb=iber-implementing class, </li>
    <li> <i> info.html </i> - a file in the directory containing the Transcriber class
      containing a description of the transcriber, which is displayed to users before they
      install the module. </li> 
    <li> <i> &hellip;/config/&hellip; </i> - a subdirectory of the directory containing
      the Transcriber class, containing a general configuration web-app, if the transcriber
      needs overall configuration. </li> 
</ul> </p>

<p> e.g. a module implementing a Transcriber class called <tt> org.fancy.FancySTT </tt>
  should be deployed in a .jar archive with a <q>nzilbb-ag-stt-Transcriber</q>
  manifest attribute with the value <q>org.fancy.FancySTT</q>, and the following contents:
  <ul>
    <li>org/fancy/FancySTT.class</li>
    <li>org/fancy/info.html</li>
    <li>org/fancy/config/index.html</li>
</ul> </p>

<p> Transcribers will most likely need to interact with the user at installation time, in
  order to configure web service keys, paths to toolkits, etc. </p>

<details><summary> Web-app resources </summary>
  
  <p> The configuration web-app may required resources such as javascript files
    style-sheets, and images. These can be packaged into the .jar file with the
    associated <tt>index.html</tt> file. </p>
  
  <p> The web-app can also communicate with the Transcriber object by making HTTP
    requests to its host, where the URL path is the name of the class method to call.
    For GET requests, the query string contains the method parameter values, in order,
    seperated by commas. Whatever the given method returns is passed back as the response. </p>
  
  <p> For example if the Transcriber class has the method: <br>
    <code>public String setFoo(String foo, String bar)</code><br>
    &hellip; then the web-app can make the GET request: <br> 
    <tt>/setFoo?fooValue,barValue</tt> <br>
    &hellip; will get as a response whatever calling <code>setFoo("fooValue", "barValue")</code>
    returns. (Currently there can be up to five parameters) </p>
  
  <p> To upload a file, the web-app can either:
    <ul>
      <li> make a PUT request, where the body of the request is assumed to be the file contents
        (the corresponding Transcriber class method must take one File parameter), or</li>
      <li> make a mutipart POST request, where the body of the request has parts for each
        parameter (the parameters can include files and text parameters, and the
        corresponding Transcriber class method must take corresponding File or String
        parameters). </li>
    </ul>
  </p>
  
  <p> For example if the Transcriber class has the method: <br>
    <code>public String uploadModel(File model)</code><br>
    &hellip; then the web-app can make the PUT or POST request to <br>
    <tt>/uploadModel</tt> <br>
    &hellip; which will get as a response whatever calling
    <code>uploadModel(tempFileContainingContent)</code> returns. </p>

  <p> The web-app can also ask for information about the annotation layer schema by
    making a GET request to <tt>getSchema</tt> which will return the schema encoded as
    JSON, e.g.: 
  <pre id="schema">{
    "participantLayerId":"who",
    "turnLayerId":"turn",
    "utteranceLayerId":"utterance",
    "wordLayerId":"word",
    "episodeLayerId":null,
    "corpusLayerId":null,
    "root" : {
        "id":"graph", "parentId":null, "description":"The graph as a whole",
        "alignment":2,
        "peers":false, "peersOverlap":false, "parentIncludes":true, "saturated":true,
        "type":"string" },
    "layers" : {
        "graph" : {
            "id":"graph", "parentId":null, "description":"The graph as a whole",
            "alignment":2,
            "peers":false, "peersOverlap":false, "parentIncludes":true, "saturated":true,
            "type":"string" },
        "who" : {
            "id":"who", "parentId":"graph", "description":"Participants",
            "alignment":0,
            "peers":true, "peersOverlap":true, "parentIncludes":true, "saturated":true,
            "type":"string" },
        "turn" : {
            "id":"turn", "parentId":"who", "description":"Speaker turns",
            "alignment":2,
            "peers":true, "peersOverlap":false, "parentIncludes":true, "saturated":false,
            "type":"string" },
        "utterance" : {
            "id":"utterance", "parentId":"turn", "description":"Utterances",
            "alignment":2,
            "peers":true, "peersOverlap":false, "parentIncludes":true, "saturated":true,
            "type":"string" },
        "word" : {
            "id":"word", "parentId":"turn", "description":"Words",
            "alignment":2,
            "peers":true, "peersOverlap":false, "parentIncludes":true, "saturated":false,
            "type":"string" }
    }
}</pre>

  <p> The web-app can also assume they can load a javascript file called <tt>util.js</tt>,
    which implements various utility functions that save the web-app implementor from
    having to implement various common operations:</p>

    <details><summary>getSchema(<var title="Callback with one parameter; the schema">gotSchema</var>)</summary>
        <p> Get the layer schema being used. The callback should have one parameter, which
          is a schema object with the structure as shown in
          <a href="#schema"> the example above</a>. e.g. </p>
        <pre>getSchema(schema =&gt; {
    console.log("The word layer is: " + schema.wordLayerId);
    console.log("All the layers: ");
    for (var l in schema.layers) {
        var layer = schema.layers[l];
        console.log(layer.id + " : parent " + layer.parentId + " alignment " + layer.alignment);
        ... something more interesting
    } // next layer    
});</pre>
    </details>
    
    <details><summary>getVersion(<var title="Callback with one parameter; the version">gotVersion</var>)</summary>
      <p> Get the transcriber implementation version (i.e. the value of
        {@link Transcriber#getVersion()}) e.g. </p>
        <pre>getVersion(version =&gt; {
    document.getElementById("version").innerHTML = version;
});</pre>
    </details>
    
    <details><summary>getJSON(<var title="functionName?comma,separated,parameters">resource</var>,
          <var title="Callback with one parameter: the parsed JSON object">gotJSON</var>)</summary>
      <p> Makes a GET request for which the expected response is JSON. </p>
      <p> e.g. the following Javascript would get the layer schema: <br>
        <code> getJSON("getSchema", json =&gt; { schema = json; ... });</code></p>
    </details>
    
    <details><summary>getText(<var title="functionName?comma,separated,parameters">resource</var>,
          <var title="Callback with one parameter: the response string">gotText</var>)</summary>
      <p> Makes a GET request for which the expected response is plain text. </p>
      <p> e.g. the following Javascript would get the version of the transcriber: <br>
        <code> getText("getConfig", config =&gt; {
          console.log("Configuration string: " + config); ... });</code></p>
    </details>
    
    <details><summary>get(<var title="functionName?comma,separated,parameters">resource</var>,
          <var title="XMLHttpRequest load event handler">onload</var>,
          <var title="Value for Accept request header">contentType</var>)</summary>
      <p> Makes a GET request for which the expected response is <var> contentType </var>.</p>
      <p> e.g. the following Javascript would get the layer schema: <br>
        <code> get("getSchema", function(e) {
          schema = JSON.parse(this.responseText); ... }, "application/json");</code></p>      
    </details>
    
    <details><summary>resourceForFunction(<var title="Parameter value">functionName</var>,
        <var title="Parameter value">param</var> ...)</summary>
      <p> Builds a URL-encoded <var>resource</var> string that can be passed
        to <b> getJSON </b>, <b> getText </b>, or <b> get </b> which ensures that if the
        parameter's value contains a comma, it's not interpreted as two parameters
        instead. </p>
      <p> e.g. if your transcriber implements a Java method like <br>
        <code> String testLookup(String word, String encoding) </code> <br>
        ... then in the web-app, you can invoke this function using Javascript code
        something like: <br>
        <code> getText(resourceForFunction("testLookup", word, encoding), function(e) {
          var value = this.responseText; ... }) </code>
      </p>
    </details>
    
    <details><summary>addLayerOptions(<var>select</var>, <var>schema</var>, <var>layerPredicate</var>)</summary>
      <p> Populate a &lt;select&gt; element with layers for which a predicate is true. </p>
      <p> For example you may have a &lt;select&gt; tag something like: <br>
        <code> &lt;select id="sourceLayerId" name="sourceLayerId" required&gt;&lt;/select&gt; </code><br>
        ...which you want to be populated with word-tag layers - i.e. layers from the
        schema that
        <ul>
          <li> have the word token layer as the parent, and </li>
          <li> are not aligned (i.e. have alignement set to 0). </li>
        </ul>
      </p>
      <p> Once you have the layer schema, you can populate this input with options
        using: <br>
        <pre>addLayerOptions(
        document.getElementById("sourceLayerId"), schema,
        // this is a function that takes a layer and returns true for the ones we want
        layer =&gt; layer.parentId == schema.wordLayerId &amp;&amp; layer.alignment == 0);</pre>
      </p>
    </details>
    
    <details><summary>putFile(<var title="functionName">resource</var>,
          <var>file</var>,
          <var title="XMLHttpRequest load event handler">onUploaded</var>,
          <var title="XMLHttpRequest progress event handler">onProgress</var>,
          <var title="XMLHttpRequest error event handler">onFailed</var>)</summary>
        <p> Upload a file, using PUT, which does not send the file name, e.g.
          <code>putFile("uploadLexicon", input.files[0], function(e) {
            console.log(this.responseText);});</code>
        </p>
    </details>
    
    <details><summary>postForm(<var title="functionName">resource</var>,
          <var>formData</var>,
          <var title="XMLHttpRequest load event handler">onUploaded</var>,
          <var title="XMLHttpRequest progress event handler">onProgress</var>,
          <var title="XMLHttpRequest error event handler">onFailed</var>)</summary>
        <p> Make a multipart POST request, so multiple files and other parameters are supported
          e.g. </p>
        <code>
          var fd = new FormData(); <br>
          fd.append("file", input.files[0]); <br>
          postForm("uploadLexicon", fd, function(e) { console.log(this.responseText);});
        </code>
    </details>
    
    <details><summary>convertFormBodyToJSON(<var title="&lt;form&gt; element">form</var>,
          <var title="Java object for extra JSON attributes to set">body</var>)</summary>
        <p> Normally an HTML form sends its body with url-encoded parameters like this: <br>
          <tt> ?foo=value1&amp;bar=value2 </tt> </p>
        <p> If you want an HTML form to instead post a JSON-encoded object, like this: <br>
          <tt> {"foo":"value1","bar","value2"} </tt> <br>
          ...then call this function in the "onsubmit" event of the form, i.e.: <br>
          <code> &lt;form onsubmit="convertFormBodyToJSON(this)"&gt; </code> </p>
        <p> If you want to send some extra, more complex attributes in the body,
          (beyond the named input controls) use the second parameter for this, e.g. <br>
          <code>
            &lt;form
            onsubmit="convertFormBodyToJSON(this, { anArrayOfObjects:[{foo:1,bar:2},{foo:3,bar4}] })"&gt;
          </code>
    </details>
    
  
</details>  

<details><summary id="config"> General Installation/Configuration </summary>
  
  <p> Many transcribers require install-time setup processing, e.g. to allow the user to
    specify URLs/credentials for web-services, upload model files, etc. There are two
    mechanisms for this:
    <ol>
      <li> The transcriber can provide a user interface for specifying configuration, in the
        form of a browser-based web-application. </li>
      <li> The transcriber implements {@link Transcriber#setConfig(String)} which completes any
        install-time processing required.</li>
  </ol> </p>
  
  <p> The configuration user interface is provided by packing a file called 
    <q>index.html</q> and any other script/style-sheet files that might be required into
    the deployed .jar archive, in a subdirectory called <i>config</i>.  </p>
  
  <p> Once the configuration web-app is finished, it must make a POST request to the URL
    path: <br> 
    <tt>setConfig</tt>
    &hellip; which will invoke the Transcriber class's
    {@link Transcriber#setConfig(String) setConfig(config)} method. </p>
  
  <p> The <var>config</var> string passed into the <code>setConfig</code> method is the
    body of the POST request.  </p>
  
  <p> Below is a simple example of a <i>config/index.html</i> web-app that loads any
    current configuration from the transcriber, presents a form for the user to fill out,
  and posts the new configuration back to the transcriber:<pre>&lt;html&gt;&lt;head&gt;&lt;title&gt;Configure Transcriber&lt;/title&gt;
    &lt;script src="util.js" type="text/javascript"&gt;&lt;/script&gt;
  &lt;/head&gt;&lt;body&gt;&lt;h1&gt;Configure Transcriber&lt;/h1&gt;
    &lt;form method="POST" action="setConfig"&gt; &lt;!-- POST to setConfig --&gt;
      &lt;label for="foo"&gt;Setting for Foo&lt;/label&gt;
      &lt;input id="foo" name="foo" type="text"&gt; &lt;!-- an example bean property --&gt;
      &lt;input type="submit" value="Install"&gt;
    &lt;/form&gt;
    &lt;script type="text/javascript"&gt;
      // GET request to getConfig retrieves the current setup configuration, if any
      get("getConfig", function(e) {
          var parameters = new URLSearchParams(this.responseText);
          // set initial values of properties in the form above
          // (this assumes bean property names match input id's in the form above)
          for (const [key, value] of parameters) {
              document.getElementById(key).value = value;
          }
      });      
    &lt;/script&gt;    
  &lt;/body&gt;
&lt;/html&gt;</pre>

  <p> (This example is self-contained apart from <i>util.js</i> and assumes no web-app
    frameworks. The web-app can use JQuery, Angular, or any other frameworks or libraries,
    as long as they are bundled into the <i> config </i> directory with <i> index.html </i>) </p>
  
  <p> If no <q>config/index.html</q> file is provided in the .jar archive, 
    <code>setConfig(null)</code> is invoked as soon as the module is installed/upgraded. </p>
  
  <p> The {@link Transcriber#setConfig(String) setConfig(config)} method is assumed to be
    synchronous (this method doesn't return until it's complete) and long-running, so the
    Transcriber class should provide an indication of progress by calling
    {@link Transcriber#setPercentComplete(Integer)} and should regularly check
    {@link Transcriber#isCancelling()} to determine if installation should be stopped. </p>
</details>

<h2 id="util"> Utilities to Facilitate Development </h2> </p>

<p> <i>nzilbb.ag.jar</i> contains some utilities for displaying information and
  executing web-apps from the command line which may be useful during development of new
  transcribers: </p>

<h3 id="Info"> Info </h3>

<p>This utility displays the <i> info.html </i> page of a given transcriber, e.g.</p>

<p><tt> java -classpath nzilbb.ag.jar nzilbb.ag.stt.util.Info myjar.jar </tt></p>

<h3 id="ConfigApp"> ConfigApp </h3>

<p>This utility runs an transcriber's <i> config </i> web-app for installation-time
  overall configuration, e.g.</p>

<p><tt> java -classpath nzilbb.ag.jar nzilbb.ag.stt.util.ConfigApp myjar.jar </tt></p>
 
 */
package nzilbb.ag.stt;
