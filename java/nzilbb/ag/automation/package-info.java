/**
 * These are classes and interface for supporting automated annotation modules.
 * <p> Modules can be defined which perform specific annotation annotation tasks
 * automatically. Such a module must be packaged in a .jar file with the following:
 * <ul>
 *  <li> a class that implements the {@link Annotator} interface, </li>
 *  <li> a manifest attribute called <q>nzilbb-ag-automation-Annotator</q> whose value
 *       is the fully-qualified class name of the Annotator-implementing class, </li>
 *  <li> <i> info.html </i> - a file in the directory containing the Annotator class
 *       containing a description of the type of annotation tasks the annotator can do,
 *       which is displayed to users before they install the module. </li>
 *  <li> <i> &hellip;/config/&hellip; </i> - a subdirectory of the directory containing
 *       the Annotator class, containing a general configuration webapp, if the annotator
 *       needs overall configuration. </li> 
 *  <li> <i> &hellip;/task/&hellip; </i> - a subdirectory of the directory containing
 *       the Annotator class, containing a task configuration webapp for defining
 *       annotation task parameters. </li> 
 *  <li> <i> &hellip;/ext/&hellip; </i> - a subdirectory of the directory containing
 *       the Annotator class, containing a webapp providing any extra user interfaces that
 *       might be useful for the annotator, including post-annotation visualizations,
 *       lexicons or models, etc... </li>  
 * </ul>
 *
 * <p> e.g. a module implementing an Annotator class called <tt> org.fancy.Tagger </tt>
 * should be deployed in a .jar archive with a <q>nzilbb-ag-automation-Annotator</q>
 * manifest attribute with the value <q>org.fancy.Tagger</q>, and the following contents:
 * <ul>
 *  <li>org/fancy/Tagger.class</li>
 *  <li>org/fancy/info.html</li>
 *  <li>org/fancy/config/index.html</li>
 *  <li>org/fancy/task/index.html</li>
 * </ul>
 * 
 * <p> Annotators may require access to the file system or a relational database in order
 * to function. In order to ensure the correct resources are made available, the class
 * that implements {@link Annotator} should also implement {@link UsesFileSystem} and/or
 * {@link UsesRelationalDatabase} as well.
 *
 * <h2 id="config"> General Installation/Configuration </h2>
 *
 * <p> Many annotators require install-time setup processing, e.g. to allow the user to
 * specify URLs/credentials for web-services, upload lexicon files, etc. There are two
 * mechanisms for this:
 * <ol>
 *  <li> The annotator can provide a user interface for specifying configuration, in the
 * form of a browser-based web-application. </li>
 *  <li> The annotator implements {@link Annotator#setConfig(String)} which completes any
 * install-time processing required.</li>
 * </ol>
 * <p> The configuration user interface is provided by packing a file called 
 * <q>index.html</q> and any other script/style-sheet files that might be required into
 * the deployed .jar archive, in a top-level directory called <i>config</i>. 
 * <p> This web
 * application can assume that it can communicate with the Annotator class by making
 * requests to its host, where the URL path is the name of the class method to call.
 * For GET requests, the query string contains the method parameter values, in order,
 * seperated by commas. Whatever the given method returns is passed back as the response.
 * <p> For example if the Annotation class has the method: <br>
 * <code>public String setFoo(String foo, String bar)</code><br>
 * &hellip; then the web-app can make the GET request: <br> 
 * <tt>/setFoo?fooValue,barValue</tt> <br>
 * &hellip; will get as a response whatever calling <code>setFoo("fooValue", "barValue")</code>
 * returns.
 * <p> To upload a file, the web-app can make a POST request, where the body of the
 * request is assumed to be the file contents. The corresponding Annotator class method
 * must take one File parameter.
 * <p> For example if the Annotation class has the method: <br>
 * <code>public String uploadLexicon(File lexicon)</code><br>
 * &hellip; then the web-app can make the POST request to <br>
 * <tt>/uploadLexicon</tt> <br>
 * &hellip; which will get as a response whatever calling
 * <code>uploadLexicon(tempFileContainingContent)</code> returns.
 * <p> Once the configuration web-app is finished, it must make a POST request to the URL
 * path: <br> 
 * <tt>setConfig</tt>
 * &hellip; which will invoke the Annotator class's <code>setConfig(config)</code> method.
 * <p> The <var>config</var> string passed into the <code>setConfig</code> method is the
 * body of the POST request. 
 *
 * <p> Below is a simple example of a <i>config/index.html</i> webapp that loads any
 *  current configuration from the annotator, presents a form for the user to fill out,
 *  and posts the new configuration back to the annotator:<pre>&lt;html&gt;
 *  &lt;head&gt;&lt;title&gt;Configure Annotator&lt;/title&gt;&lt;/head&gt;&lt;body&gt;&lt;h1&gt;Configure Annotator&lt;/h1&gt;
 *    &lt;form method="POST" action="setConfig"&gt; &lt;!-- POST to setConfig --&gt;
 *        &lt;label for="foo"&gt;Setting for Foo&lt;/label&gt;
 *        &lt;input id="foo" name="foo" type="text"&gt; &lt;!-- an example bean property --&gt;
 *        &lt;input type="submit" value="Install"&gt;
 *    &lt;/form&gt;
 *    &lt;script type="text/javascript"&gt;
 *      function get(path, onload) { // make a GET request of the annotator
 *          var request = new XMLHttpRequest();
 *          request.open("GET", path);
 *          request.addEventListener("load", onload, false);
 *          request.send();
 *      }
 *      // GET request to getConfig retrieves the current setup configuration, if any
 *      get("getConfig", function(e) {
 *          var parameters = new URLSearchParams(this.responseText);
 *          // set initial values of properties in the form above
 *          // (this assumes bean property names match input id's in the form above)
 *          for (const [key, value] of parameters) {
 *            document.getElementById(key).value = value;
 *          }
 *      });      
 *    &lt;/script&gt;    
 *  &lt;/body&gt;
 *&lt;/html&gt;</pre>
 *
 * <p> If no <q>config/index.html</q> file is provided in the .jar archive, 
 * <code>setConfig(null)</code> is invoked as soon as the module is installed/upgraded.
 *
 * <p> The <code>setConfig(config)</code> method is assumed to be synchronous (this method doesn't
 * return until it's complete) and long-running, so the Annotator class should provide an
 * indication of progress by calling {@link Annotator#setPercentComplete(Integer)} and
 * should regularly check {@link Annotator#isCancelling()} to determine if installation
 * should be stopped.
 *
 * <h2 id="task"> Annotation Task Parameter Configuration </h2>
 *
 * <p> There are likely to be many different variations on the ways that a particular
 * annotator behaves, and so most annotators need the user to specify the parameters of a
 * given annotation task. For example an annotator that tags word tokens with entries from
 * a lexicon may be able to add phonemic transcription labels or morphological parse
 * labels. In order for the annotator to determine which annotation task to perform, the
 * user must set the task parameters, which are expressed using a String, which the
 * Annotator class can encode/interpret in whatever way is most convenient (e.g. it could
 * be serialization of Properties object, JSON, etc.). 
 * <p> There are two mechanisms for achieving this:
 * <ol>
 *  <li> The annotator can provide a user interface for specifying task parameters, in the
 * form of a browser-based web-application. </li>
 *  <li> The annotator implements {@link Annotator#setTaskParameters(String)} which
 * complete provides the annotator with the resulting configuration.</li>
 * </ol>
 * <p> The configuration user interface is provided by packing a file called 
 * <q>index.html</q> and any other script/style-sheet files that might be required into
 * the deployed .jar archive, in a subdirectory of the directory containing the annotator
 * class, called <tt>task</tt>.  
 * <p> Each annotation task is identified by an ID, which is passed as the query string
 * when the task configuration web app is run; e.g. if the annotation task ID is
 * <q>pos-tagging</q> then the web app will be started with the URL like
 * <tt>&hellip;/task/index.html?pos-tagging</tt> 
 * <p> The first thing the web app should do is make a GET request to
 * <tt>getTaskParameters</tt> with the annotation task ID as the query string, in order to
 * retrieve and interpret any existing parameter configuration for the task;
 * e.g. <tt>getTaskParameters?pos-tagging</tt> 
 * <p> If the task has been configured before, then the result of this request will be
 * text using encoding was used by the web app to save the task configuration last time.
 * <p> Simple task configuration parameters can be easily encoded as query string
 * parameters, which is automatically achieved by using an HTML form that POSTs to
 * <tt>setTaskParameters</tt> - below is an example of a <i>task/index.html</i> webapp
 * that loads any current configuration from the annotator, presents a form for the user
 * to fill out, and posts the new configuration back to the annotator:<pre>&lt;html&gt;
 *  &lt;head&gt;&lt;title&gt;Configure Annotation Task&lt;/title&gt;&lt;/head&gt;&lt;body&gt;&lt;h1&gt;Configure Annotation Task&lt;/h1&gt;
 *    &lt;form method="POST" action="setTaskParameters"&gt; &lt;!-- POST to setTaskParameters --&gt;
 *        &lt;label for="bar"&gt;Setting for Bar&lt;/label&gt;
 *        &lt;input id="bar" name="bar" type="text"&gt; &lt;!-- an example task parameter --&gt;
 *        &lt;input type="submit" value="Install"&gt;
 *    &lt;/form&gt;
 *    &lt;script type="text/javascript"&gt;
 *      function get(path, onload) { // make a GET request of the annotator
 *          var request = new XMLHttpRequest();
 *          request.open("GET", path);
 *          request.addEventListener("load", onload, false);
 *          request.send();
 *      }
 *      // GET request to getConfig retrieves the current setup configuration, if any
 *      get("getTaskParameters?"+window.location.search, function(e) {
 *          var parameters = new URLSearchParams(this.responseText);
 *          // set initial values of properties in the form above
 *          // (this assumes bean property names match input id's in the form above)
 *          for (const [key, value] of parameters) {
 *            document.getElementById(key).value = value;
 *          }
 *      });      
 *    &lt;/script&gt;    
 *  &lt;/body&gt;
 *&lt;/html&gt;</pre>
 *
 * <p> The web app can also assume that it can communicate with an instance of the Annotator class
 * by making requests to its host, where the URL path is the name of the class method to call.
 * For GET requests, the query string contains the method parameter values, in order,
 * seperated by commas. Whatever the given method returns is passed back as the response.
 * <p> For example if the Annotation class has the method: <br>
 * <code>public String testLookup(String word, String query)</code><br>
 * &hellip; then the web-app can make the GET request to <br>
 * <tt>/testQuery?test,phonology</tt> 
 * &hellip; to get as a response whatever calling <code>setFoo("test", "phonology")</code>
 * returns.
 * <p> Once the task configuration web-app is finished, it must make a POST request to the
 * URL path: <br>
 * <tt>setTaskParameters</tt>
 * &hellip; with the resulting parameter string as the body of
 * the request, which will invoke the Annotator class's <code>setTaskParameters(String)</code> 
 * method.
 *
 * <p> If no <q>task/index.html</q> file is provided in the .jar archive, 
 * <code>setTaskParameters(null)</code> is invoked as soon as the module is triggered for
 * an annotation task.
 *
 * <h2 id="ext"> Extending Beyond the Configuration / Task Parameter Interfaces </h2>
 *
 * <p> Some annotators perform analysis or data extraction beyond directly annotating the
 * transcripts, and may need to provide post-processing visualizations, or access to
 * analytics.
 *
 * <p> The annotator can provide a user interface for this by deploying a web app similar
 * to the <i><a href="#config"> config </a></i> and <i><a href="#task"> task </a></i> web
 * apps descibed above.  In this case, the web app should be deployed in a relative
 * subdirectory called <i>ext</i>. 
 *
 * <p> As such a user interface is open-ended, unlike the <i> config </i> web app (which
 * makes a request to <tt>setConfig</tt>) and the <i> task </i> web app (which makes a
 * request to <tt>setTaskParameters</tt>), no final request is required to 'save' the
 * results. However, if this is desired, the web app can make a request to: <tt>finished</tt> 
 *
 * <p> Otherwise, the <i> ext </i> web app can access the Annotator object using GET
 * requests, in a similar fashion to the other web apps.
 *
 * <h2 id="util"> Utilities to Facilitate Development </h2>
 *
 * <p> <i>nzilbb.ag.jar</i> contains some utilities for displaying information and
 * executing webapps from the command line which may be useful during development of new
 * annotators: </p>
 *
 * <h3 id="AnnotatorInfo"> AnnotatorInfo </h3>
 *
 * <p>This utility displays the <i> info.html </i> page of a given annotator, e.g.</p>
 *
 * <tt> java -classpath nzilbb.ag.jar nzilbb.ag.automation.util.AnnotatorInfo myjar.jar </tt>
 *
 * <h3 id="StandAloneAnnotatorConfiguration"> StandAloneAnnotatorConfiguration </h3>
 *
 * <p>This utility runs an annotator's <i> config </i> web app for installation-time
 * overall configuration, e.g.</p>
 *
 * <tt> java -classpath nzilbb.ag.jar nzilbb.ag.automation.util.ConfigApp myjar.jar </tt>
 *
 * <h3 id="StandAloneTaskConfiguration"> StandAloneTaskConfiguration </h3>
 *
 * <p>This utility runs an annotator's <i> task </i> web app for annotation task specific
 * configuration, e.g. </p>
 *
 * <tt> java -classpath nzilbb.ag.jar nzilbb.ag.automation.util.TaskApp --annotationTaskId=test myjar.jar </tt>
 *
 * <h3 id="StandAloneAnnotatorExtras"> StandAloneAnnotatorExtras </h3>
 *
 * <p>This utility runs an annotator's <i> ext </i> web app, if any, for post-processing
 * visualizations etc., e.g. </p>
 *
 * <tt> java -classpath nzilbb.ag.jar nzilbb.ag.automation.util.ExtApp myjar.jar </tt>
 *
 * <p>This utility </p>
 *
 */
package nzilbb.ag.automation;
