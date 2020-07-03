// TODO recap with example JavaScript code showing lifecycle of each webapp
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
 *  <li> <i> &hellip;/conf/&hellip; </i> - a subdirectory of the directory containing
 *       the Annotator class containing a general configuration webapp, if the annotator
 *       needs overall configuration. </li> 
 *  <li> <i> &hellip;/task/&hellip; </i> - a a subdirectory of the directory containing
 *       the Annotator class containing a task configuration webapp for defining
 *       annotation task parameters. </li> 
 * </ul>
 *
 * <p> e.g. a module implementing an Annotator class called <tt> org.fancy.Tagger </tt>
 * should be deployed in a .jar archive with a <q>nzilbb-ag-automation-Annotator</q>
 * manifest attribute with the value <q>org.fancy.Tagger</q>, and the following contents:
 * <ul>
 *  <li>org/fancy/Tagger.class</li>
 *  <li>org/fancy/info.html</li>
 *  <li>org/fancy/conf/index.html</li>
 *  <li>org/fancy/task/index.html</li>
 * </ul>
 * 
 * <p> Annotators may require access to the file system or a relational database in order
 * to function. In order to ensure the correct resources are made available, the class
 * that implements {@link Annotator} should also implement {@link UsesFileSystem} and/or
 * {@link UsesRelationalDatabase} as well.
 *
 * <h2> General Installation/Configuration </h2>
 *
 * <p> Many annotators require install-time setup processing, e.g. to allow the user to
 * specify URLs/credentials for web-services, upload lexicon files, etc. There are two
 * mechanisms for this:
 * <ol>
 *  <li> The annotator can provide a user interface for specifying configuration, in the
 * form of a browser-based web-application. </li>
 *  <li> The annotator implements {@link Annotator#install()} which completes any
 * install-time processing required.</li>
 * </ol>
 * <p> The configuration user interface is provided by packing a file called 
 * <q>index.html</q> and any other script/style-sheet files that might be required into
 * the deployed .jar archive, in a top-level directory called <i>conf</i>. 
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
 * <p> Once the configuration web-app is finished, it must make a request to the URL path: <br> 
 * <tt>install</tt>
 * &hellip; which will invoke the Annotator class's <code>install()</code> method.
 *
 * <p> If no <q>conf/index.html</q> file is provided in the .jar archive, 
 * <code>install()</code> is invoked as soon as the module is installed/upgraded.
 *
 * <p> The <code>install()</code> method is assumed to be synchronous (this method doesn't
 * return until it's complete) and long-running, so the Annotator class should provide an
 * indication of progress by calling {@link Annotator#setPercentComplete(Integer)} and
 * should regularly check {@link Annotator#isCancelling()} to determine if installation
 * should be stopped.
 *
 * <h2> Annotation Task Parameter Configuration </h2>
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
 * the deployed .jar archive, in a top-level directory called <tt>task</tt>. 
 * <p> The first thing the web app should do is make a GET request to
 * <tt>getTaskParameters</tt> in order to retrieve and interpret any existing parameter
 * configuration for the task
 * <p>It can also assume that it can communicate with the Annotator class by making
 * requests to its host, where the URL path is the name of the class method to call.
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
 */
package nzilbb.ag.automation;
