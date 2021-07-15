/**
 * These are classes and interfaces for supporting automated annotation modules.

<style type="text/css"> summary { font-weight: bold; } </style>

<p> Annotator modules can be defined which perform specific annotation tasks
  automatically. </p>
<p> Such a module must be packaged in a .jar file with the following: 
  <ul>
    <li> a class that implements the {@link Annotator} interface, </li>
    <li> a manifest attribute called <q>nzilbb-ag-automation-Annotator</q> whose value
      is the fully-qualified class name of the Annotator-implementing class, </li>
    <li> <i> info.html </i> - a file in the directory containing the Annotator class
      containing a description of the type of annotation tasks the annotator can do,
      which is displayed to users before they install the module. </li>
    <li> <i> &hellip;/config/&hellip; </i> - a subdirectory of the directory containing
      the Annotator class, containing a general configuration web-app, if the annotator
      needs overall configuration. </li> 
    <li> <i> &hellip;/task/&hellip; </i> - a subdirectory of the directory containing
      the Annotator class, containing a task configuration web-app for defining
      annotation task parameters. </li> 
    <li> <i> &hellip;/ext/&hellip; </i> - a subdirectory of the directory containing
      the Annotator class, containing a web-app providing any extra user interfaces that
      might be useful for the annotator, including post-annotation visualizations,
      lexicons or models, etc... </li>  
  </ul> </p>

<p> e.g. a module implementing an Annotator class called <tt> org.fancy.Tagger </tt>
  should be deployed in a .jar archive with a <q>nzilbb-ag-automation-Annotator</q>
  manifest attribute with the value <q>org.fancy.Tagger</q>, and the following contents:
  <ul>
    <li>org/fancy/Tagger.class</li>
    <li>org/fancy/info.html</li>
    <li>org/fancy/config/index.html</li>
    <li>org/fancy/task/index.html</li>
    <li>org/fancy/ext/index.html</li>
</ul> </p>

<p> Annotators may require access to the file system or a relational database in order
  to function. In order to ensure the correct resources are made available, the class
  that implements {@link Annotator} should be annotated with {@link UsesFileSystem} and/or
  {@link UsesRelationalDatabase} as well. </p>

<p> Annotators will most likely need to interact with the user
  <ul>
    <li> at installation time, </li>
    <li> when defining the details of an annotation task, or </li>
    <li> after annotation, for data visualization or export. </li>
  </ul>
  This user interaction can be implemented using the <i>config</i>, <i>task</i>,
  and <i>ext</i> web-apps, respectively. Each web-app is implemented with an HTML page
  that presents forms and interacts with the Annotator object as required.
</p>

<details><summary> Web-app resources </summary>
  
  <p> Configuration/visualization web-apps may required resources such as javascript files
    style-sheets, and images. These can be packaged into the .jar file with the
    associated <tt>index.html</tt> file. </p>
  
  <p> Web-apps can also communicate with the Annotator object by making HTTP
    requests to its host, where the URL path is the name of the class method to call.
    For GET requests, the query string contains the method parameter values, in order,
    seperated by commas. Whatever the given method returns is passed back as the response. </p>

  <p> For example if the Annotation class has the method: <br>
    <code>public String setFoo(String foo, String bar)</code><br>
    &hellip; then the web-app can make the GET request: <br> 
    <tt>/setFoo?fooValue,barValue</tt> <br>
    &hellip; will get as a response whatever calling <code>setFoo("fooValue", "barValue")</code>
    returns. (Currently there can be up to five parameters) </p>

  <p> To upload a file, the web-app can either:
    <ul>
      <li> make a PUT request, where the body of the request is assumed to be the file contents
        (the corresponding Annotator class method must take one File parameter), or</li>
      <li> make a mutipart POST request, where the body of the request has parts for each
        parameter (the parameters can include files and text parameters, and the
        corresponding Annotator class method must take corresponding File or String
        parameters). </li>
    </ul>
  </p>

  <p> For example if the Annotation class has the method: <br>
    <code>public String uploadLexicon(File lexicon)</code><br>
    &hellip; then the web-app can make the PUT or POST request to <br>
    <tt>/uploadLexicon</tt> <br>
    &hellip; which will get as a response whatever calling
    <code>uploadLexicon(tempFileContainingContent)</code> returns. </p>

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

  <p> Web-apps can also assume they can load a javascript file called <tt>util.js</tt>,
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
      <p> Get the annotator implementation version (i.e. the value of
        {@link Annotator#getVersion()}) e.g. </p>
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
      <p> e.g. the following Javascript would get the version of the annotator: <br>
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
      <p> e.g. if your annotator implements a Java method like <br>
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
    
    <details><summary>startLoading()</summary>
        <p> Cover the page with a &lt;div&dt; element with a spinner, to prevent the user
          from interacting with the page while data and settings are loaded. e.g. </p>
        <pre>// show spinner
startLoading();
// load the task settings
getText("getTaskParameters", text => {
    try {
        // ...process the settings, populate the form, etc...
    } finally {
        // hide spinner
        finishedLoading();
    }
});</pre>
    </details>
  
    <details><summary>finishedLoading()</summary>
        <p> Remove the &lt;div&dt; element spinner element previously create by
          <code>startLoading()</code>, to allow the user to interact with the page. e.g. </p>
        <pre>// show spinner
startLoading();
// load the task settings
getText("getTaskParameters", text => {
    try {
        // ...process the settings, populate the form, etc...
    } finally {
        // hide spinner
        finishedLoading();
    }
});</pre>
    </details>
</details>  

<p> There are three phases during which the annotator might do processing and/or require
  user interaction with a web-app:</p>

<details><summary id="config"> General Installation/Configuration </summary>
  
  <p> Many annotators require install-time setup processing, e.g. to allow the user to
    specify URLs/credentials for web-services, upload lexicon files, etc. There are two
    mechanisms for this:
    <ol>
      <li> The annotator can provide a user interface for specifying configuration, in the
        form of a browser-based web-application. </li>
      <li> The annotator implements {@link Annotator#setConfig(String)} which completes any
        install-time processing required.</li>
  </ol> </p>
  
  <p> The configuration user interface is provided by packing a file called 
    <q>index.html</q> and any other script/style-sheet files that might be required into
    the deployed .jar archive, in a subdirectory called <i>config</i>.  </p>
  
  <p> Once the configuration web-app is finished, it must make a POST request to the URL
    path: <br> 
    <tt>setConfig</tt>
    &hellip; which will invoke the Annotator class's
    {@link Annotator#setConfig(String) setConfig(config)} method. </p>
  
  <p> The <var>config</var> string passed into the <code>setConfig</code> method is the
    body of the POST request.  </p>
  
  <p> Below is a simple example of a <i>config/index.html</i> web-app that loads any
    current configuration from the annotator, presents a form for the user to fill out,
  and posts the new configuration back to the annotator:<pre>&lt;html&gt;&lt;head&gt;&lt;title&gt;Configure Annotator&lt;/title&gt;
    &lt;script src="util.js" type="text/javascript"&gt;&lt;/script&gt;
  &lt;/head&gt;&lt;body&gt;&lt;h1&gt;Configure Annotator&lt;/h1&gt;
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
  
  <p> The {@link Annotator#setConfig(String) setConfig(config)} method is assumed to be
    synchronous (this method doesn't return until it's complete) and long-running, so the
    Annotator class should provide an indication of progress by calling
    {@link Annotator#setPercentComplete(Integer)} and should regularly check
    {@link Annotator#isCancelling()} to determine if installation should be stopped. </p>
</details>

<details><summary id="task"> Annotation Task Parameter Configuration </summary>

  <p> There are likely to be many different variations on the ways that a particular
    annotator behaves, and so most annotators need the user to specify the parameters of a
    given annotation task. For example an annotator that tags word tokens with entries from
    a lexicon may be able to add phonemic transcription labels or morphological parse
    labels. In order for the annotator to determine which annotation task to perform, the
    user must set the task parameters, which are expressed using a String, which the
    Annotator class can encode/interpret in whatever way is most convenient (e.g. it could
    be serialization of Properties object, JSON, etc.).  </p>
  
  <p> There are two mechanisms for achieving this:
    <ol>
      <li> The annotator can provide a user interface for specifying task parameters, in the
        form of a browser-based web-application. </li>
      <li> The annotator implements {@link Annotator#setTaskParameters(String)} which
        complete provides the annotator with the resulting configuration.</li>
  </ol> </p>
  
  <p> The configuration user interface is provided by packing a file called 
    <q>index.html</q> and any other script/style-sheet files that might be required into
    the deployed .jar archive, in a subdirectory of the directory containing the annotator
    class, called <tt>task</tt>.   </p>
  
  <p> Each annotation task is identified by an ID, which is passed as the query string
    when the task configuration web-app is run; e.g. if the annotation task ID is
    <q>pos-tagging</q> then the web-app will be started with the URL like
    <tt>&hellip;/task/index.html?pos-tagging</tt>  </p>
  
  <p> The first thing the web-app should do is make a GET request to
    <tt>getTaskParameters</tt> with the annotation task ID as the query string, in order to
    retrieve and interpret any existing parameter configuration for the task;
    e.g. <tt>getTaskParameters?pos-tagging</tt>  </p>
  
  <p> If the task has been configured before, then the result of this request will be
    text using encoding was used by the web-app to save the task configuration last time. </p>
  
  <p> Simple task configuration parameters can be easily encoded as query string
    parameters, which is automatically achieved by using an HTML form that POSTs to
    <tt>setTaskParameters</tt> - below is an example of a <i>task/index.html</i> web-app
    that loads any current configuration from the annotator, presents a form for the user
    to fill out, and posts the new configuration back to the annotator:<pre>&lt;html&gt;&lt;head&gt;&lt;title&gt;Annotation Task Parameters&lt;/title&gt;
    &lt;script src="util.js" type="text/javascript"&gt;&lt;/script&gt;
  &lt;/head&gt;&lt;body&gt;&lt;h1&gt;Annotation Task Parameters&lt;/h1&gt;
    &lt;form method="POST" action="setTaskParameters"&gt; &lt;!-- POST to setTaskParameters --&gt;
      &lt;label for="bar"&gt;Setting for Bar&lt;/label&gt;
      &lt;input id="bar" name="bar" type="text"&gt; &lt;!-- an example task parameter --&gt;
      &lt;input type="submit" value="Install"&gt;
    &lt;/form&gt;
    &lt;script type="text/javascript"&gt;
    // GET request to getTaskParameters retrieves the current setup configuration, if any
    get("getTaskParameters?"+window.location.search, function(e) {
        var parameters = new URLSearchParams(this.responseText);
        // set initial values of properties in the form above
        // (this assumes bean property names match input id's in the form above)
        for (const [key, value] of parameters) {
            document.getElementById(key).value = value;
        }
    });      
    &lt;/script&gt;    
  &lt;/body&gt;
&lt;/html&gt;</pre> </p>

  <p> (This example is self-contained apart from <i> util.js </i> and assumes no web-app
    frameworks. The web-app can use JQuery, Angular, or any other frameworks or libraries,
    as long as they are bundled into the <i> task </i> directory with <i> index.html </i>) </p>
    
  <p> Once the task configuration web-app is finished, it must make a POST request to the
    URL path: <br>
    <tt>setTaskParameters</tt>
    &hellip; with the resulting parameter string as the body of
    the request, which will invoke the Annotator class's
    {@link Annotator#setTaskParameters(String) setTaskParameters(parameters)} method. </p>
  
  <p> If no <q>task/index.html</q> file is provided in the .jar archive, 
    <code>setTaskParameters(null)</code> is invoked as soon as the module is triggered for
    an annotation task. </p>
  
  <p>If the task parameters require the creation of a new layer, it should be added to
    the Schema that the annotator was given by {@link Annotator#setSchema(Schema)},
    e.g.</p>
  <pre>schema.addLayer(
    new Layer("pos", "Part of Speech")
    .setAlignment(Constants.ALIGNMENT_NONE)
    .setPeers(false)
  .setParentId(schema.getWordLayerId()));</pre>
</details>

<details><summary id="ext"> Extending Beyond the Configuration / Task Parameter Interfaces </summary>
  
  <p> Some annotators perform analysis or data extraction beyond directly annotating the
    transcripts, and may need to provide post-processing visualizations, or access to
    analytics. </p>
  
  <p> The annotator can provide a user interface for this by deploying a web-app similar
    to the <i><a href="#config"> config </a></i> and <i><a href="#task"> task </a></i> web
    apps descibed above.  In this case, the web-app should be deployed in a relative
    subdirectory called <i>ext</i>.  </p>
  
  <p> As such a user interface is open-ended, unlike the <i> config </i> web-app (which
    makes a request to <tt>setConfig</tt>) and the <i> task </i> web-app (which makes a
    request to <tt>setTaskParameters</tt>), no final request is required to 'save' the
    results. However, if this is desired, the web-app can make a request to: <tt>finished</tt>  </p>
  
  <p> Otherwise, the <i> ext </i> web-app can access the Annotator object using GET
    requests, in a similar fashion to the other web-apps. </p>
    
</details>

<h2 id="transcriber"> Transcribers </h2>

<p> {@link Transcriber}s are a specialized type of {@link Annotator} whose purpose is to
  transcribe audio and fill in the utterance and/or word layer of the annotation
  graph. </p>

<p> Implementation of a transcriber generally involves specifying a configuration webapp
  (as above), and implementing the {@link Transcriber#transcribe(File,Graph)} method; the
  base class generally provides default implementations for other Annotator methods. </p>

<h2 id="util"> Utilities to Facilitate Development </h2> </p>

<p> <i>nzilbb.ag.jar</i> contains some utilities for displaying information and
  executing web-apps from the command line which may be useful during development of new
  annotators: </p>

<h3 id="Info"> Info </h3>

<p>This utility displays the <i> info.html </i> page of a given annotator, e.g.</p>

<p><tt> java -classpath nzilbb.ag.jar nzilbb.ag.automation.util.Info myjar.jar </tt></p>

<h3 id="ConfigApp"> ConfigApp </h3>

<p>This utility runs an annotator's <i> config </i> web-app for installation-time
  overall configuration, e.g.</p>

<p><tt> java -classpath nzilbb.ag.jar nzilbb.ag.automation.util.ConfigApp myjar.jar </tt></p>

<h3 id="TaskApp"> TaskApp </h3>

<p>This utility runs an annotator's <i> task </i> web-app for annotation task specific
  configuration, e.g. </p>

<p><tt> java -classpath nzilbb.ag.jar nzilbb.ag.automation.util.TaskApp --annotationTaskId=test myjar.jar </tt></p>

<h3 id="ExtApp"> ExtApp </h3>

<p>This utility runs an annotator's <i> ext </i> web-app, if any, for post-processing
  visualizations etc., e.g. </p>

<p><tt> java -classpath nzilbb.ag.jar nzilbb.ag.automation.util.ExtApp myjar.jar </tt></p>

<h3 id="Transcribe"> Transcribe </h3>

<p>This utility, which works for {@link Transcriber}s only, uses the given transcriber to
  transcribe a given audio file, e.g.</p> 

<p><tt> java -classpath nzilbb.ag.jar nzilbb.ag.automation.util.Transcribe myjar.jar myrecording.wav </tt></p>
 
 */
package nzilbb.ag.automation;
