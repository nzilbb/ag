//
// Copyright 2020 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of nzilbb.ag.
//
//    nzilbb.ag is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 3 of the License, or
//    (at your option) any later version.
//
//    nzilbb.ag is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with nzilbb.ag; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package nzilbb.ag.automation.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.util.AnnotatorDescriptor;
import nzilbb.ag.automation.util.RequestRouter;
import nzilbb.util.IO;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;
import nzilbb.webapp.StandAloneWebApp;

/**
 * Utility for running a stand-alone webapp for configuring the parameters of an
 * annotation task.
 */
@ProgramDescription(value="Utility for configuring the parameters of an annotation task.")
public class StandAloneTaskConfiguration extends StandAloneWebApp {

   /** Command-line entrypoint */
   public static void main(String argv[]) {
      StandAloneTaskConfiguration application = new StandAloneTaskConfiguration();
      if (application.processArguments(argv)) {
         try {
            application.init();
            application.start();
         } catch(Exception exception) {
            System.err.println("Could not start: "+exception.toString());
         }
      }
   }

   /**
    * Whether to print debug tracing.
    * @see #getDebug()
    * @see #setDebug(Boolean)
    */
   protected Boolean debug = Boolean.FALSE;
   /**
    * Getter for {@link #debug}: Whether to print debug tracing.
    * @return Whether to print debug tracing.
    */
   public Boolean getDebug() { return debug; }
   /**
    * Setter for {@link #debug}: Whether to print debug tracing.
    * @param newDebug Whether to print debug tracing.
    */
   @Switch("Whether to print debug tracing")
   public StandAloneTaskConfiguration setDebug(Boolean newDebug) { debug = newDebug; return this; }

   /**
    * The name of either a .jar file, or a class (if it's on the classpath), which
    * implements the annotator. 
    * @see #getAnnotatorName()
    * @see #setAnnotatorName(String)
    */
   protected String annotatorName;
   /**
    * Getter for {@link #annotatorName}: The name of either a .jar file, or a class (if
    * it's on the classpath), which implements the annotator. 
    * @return The name of either a .jar file, or a class (if it's on the classpath), which
    * implements the annotator. 
    */
   public String getAnnotatorName() { return annotatorName; }
   /**
    * Setter for {@link #annotatorName}: The name of either a .jar file, or a class (if
    * it's on the classpath), which implements the annotator. 
    * @param newAnnotatorName The name of either a .jar file, or a class (if it's on the
    * classpath), which implements the annotator. 
    */
   @Switch(value="Name of annotator .jar file or class",compulsory=true)
   public StandAloneTaskConfiguration setAnnotatorName(String newAnnotatorName) { annotatorName = newAnnotatorName; return this; }

   /**
    * Descriptor for the annotator.
    * @see #getDescriptor()
    * @see #setDescriptor(AnnotatorDescriptor)
    */
   protected AnnotatorDescriptor descriptor;
   /**
    * Getter for {@link #descriptor}: Descriptor for the annotator.
    * @return Descriptor for the annotator.
    */
   public AnnotatorDescriptor getDescriptor() { return descriptor; }
   /**
    * Setter for {@link #descriptor}: Descriptor for the annotator.
    * @param newDescriptor Descriptor for the annotator.
    */
   public StandAloneTaskConfiguration setDescriptor(AnnotatorDescriptor newDescriptor) { descriptor = newDescriptor; return this; }

   /**
    * The annotator to configure.
    * @see #getAnnotator()
    * @see #setAnnotator(Annotator)
    */
   protected Annotator annotator;
   /**
    * Getter for {@link #annotator}: The annotator to configure.
    * @return The annotator to configure.
    */
   public Annotator getAnnotator() { return annotator; }
   /**
    * Setter for {@link #annotator}: The annotator to configure.
    * @param newAnnotator The annotator to configure.
    */
   public StandAloneTaskConfiguration setAnnotator(Annotator newAnnotator) { annotator = newAnnotator; return this; }
   
   /**
    * Router for sending requests to annotator.
    * @see #getRouter()
    * @see #setRouter(RequestRouter)
    */
   protected RequestRouter router;
   /**
    * Getter for {@link #router}: Router for sending requests to annotator.
    * @return Router for sending requests to annotator.
    */
   public RequestRouter getRouter() { return router; }
   /**
    * Setter for {@link #router}: Router for sending requests to annotator.
    * @param newRouter Router for sending requests to annotator.
    */
   public StandAloneTaskConfiguration setRouter(RequestRouter newRouter) { router = newRouter; return this; }

   /**
    * Identifier of the task to be configured.
    * @see #getAnnotationTaskId()
    * @see #setAnnotationTaskId(String)
    */
   protected String annotationTaskId;
   /**
    * Getter for {@link #annotationTaskId}: Identifier of the task to be configured.
    * @return Identifier of the task to be configured.
    */
   public String getAnnotationTaskId() { return annotationTaskId; }
   /**
    * Setter for {@link #annotationTaskId}: Identifier of the task to be configured.
    * @param newAnnotationTaskId Identifier of the task to be configured.
    */
   @Switch(value="Identifier of the task to be configured",compulsory=true)
   public StandAloneTaskConfiguration setAnnotationTaskId(String newAnnotationTaskId) { annotationTaskId = newAnnotationTaskId; return this; }

   /**
    * Working directory.
    * @see #getWorkingDir()
    * @see #setWorkingDir(File)
    */
   protected File workingDir = new File(".");
   /**
    * Getter for {@link #workingDir}: Working directory.
    * @return Working directory.
    */
   public File getWorkingDir() { return workingDir; }
   /**
    * Setter for {@link #workingDir}: Working directory.
    * @param newWorkingDir Working directory.
    */
   @Switch("Directory for working/config files (default is the current directory)")
   public StandAloneTaskConfiguration setWorkingDir(File newWorkingDir) { workingDir = newWorkingDir; return this; }
   
   /**
    * Adds handlers which routes webapp resource requests to the Annotators "conf" webapp,
    * and routes server requests to Annotator.
    */
   protected void addHandlers() throws IOException {
      if (server == null) createServer();

      // add getTaskParameters handler
      server.createContext("/getTaskParameters", new HttpHandler() {
            public void handle(HttpExchange x) throws IOException {
               String parameters = "";
               // load previous parameters if any
               File f = new File(annotator.getAnnotatorId() + "-" + annotationTaskId + ".cfg");
               if (f.exists()) {
                  try {
                     parameters = IO.InputStreamToString(new FileInputStream(f));
                  } catch(IOException exception) {}
               }
               x.sendResponseHeaders(200, parameters.length());
               OutputStream os = x.getResponseBody();
               os.write(parameters.getBytes());
               os.close();
            }});      
      
      // all (not otherwise handled) requests:
      server.createContext("/", new HttpHandler() {
            public void handle(HttpExchange x) throws IOException {
               URI uri = x.getRequestURI();
               String path = uri.getPath();
               if (path.equals("/")) path = "/index.html";
               InputStream response = null;
               int status = 200;
               if (path.indexOf('.') > 0) {
                  // requests with a dot are taken to be resources for the webapp,
                  // e.g. index.html
                  if (debug) System.err.println("resource: " + uri);
                  x.getResponseHeaders().add("Content-Type", ContentTypeForName(path));
                  if (debug) System.err.println("getResource: conf"+path);
                  try {
                     response = descriptor.getResource("task"+path);
                  } catch(Throwable exception) {
                     if (debug) System.err.println("could not getResource: "+exception);
                  }
                  if (response == null) status = 404;
               } else {
                  if (debug) System.err.println("annotator: " + uri);
                  // everything else is routed to the annotator
                  try {
                     response = router.request(
                        x.getRequestMethod(), uri, x.getRequestHeaders().getFirst("Content-Type"),
                        x.getRequestBody());                     
                  } catch(RequestException exception) {
                     if (debug) System.err.println("RequestException: " + exception);
                     status = exception.getHttpStatus();
                     response = new ByteArrayInputStream(exception.getMessage().getBytes());
                  }
               }
               if (debug) {
                  System.err.println(
                     "response: " + status + (response==null?" no content":" with content"));
               }
               x.sendResponseHeaders(status, response == null?-1:0);
               if (response != null) {
                  IO.Pump(response, x.getResponseBody());
               }
            }});      
   } // end of addHandlers()

   /** Constructor */
   public StandAloneTaskConfiguration() {
      setFinishedPath("setTaskParameters");
   }

   /**
    * Initialize the application.
    * @throws ClassNotFoundException If the implemenation for {@link #annotatorName}
    * cannot be found. 
    * @throws NoSuchMethodException If the annotator has no default constructor.
    * @throws IllegalAccessException If the annotator's default constructor is not public.
    * @throws InvocationTargetException If the annotator's constructor throws an exception.
    * @throws InstantiationException If the annotator is an abstract class.
    * @throws ClassCastException If the annotator does not extend {@link Annotator}.
    */
   public void init() throws ClassNotFoundException, NoSuchMethodException,
      InvocationTargetException, IllegalAccessException, InstantiationException,
      ClassCastException, IOException {
      
      // is the name a jar file name or a class name
      try { // try as a jar file
         descriptor = new AnnotatorDescriptor(new File(annotatorName));
      } catch (Throwable notAJarName) { // try as a class name
         notAJarName.printStackTrace(System.err);
         descriptor = new AnnotatorDescriptor(annotatorName, getClass().getClassLoader());
      }
      if (!descriptor.hasTaskWebapp()) {
         throw new FileNotFoundException("Annotator has no 'task' web app.");
      }

      annotator = descriptor.getInstance();
      router = new RequestRouter(annotator);
      query = annotationTaskId;

      // set a response that will follow the progress of the installation
      finishedResponse = "<html><head><title>Task Configuration</title></head><body>"
         +"<p style='text-align: center;'><big>Thanks</big></p>"
         +"<p style='text-align: center;'>You can close this window.</p>"
         +"</body></html>";

      if (annotator instanceof UsesFileSystem) {
         ((UsesFileSystem)annotator).setWorkingDirectory(workingDir);
      }
   } // end of init()

   /**
    * Called when the web app is finished and the server has been stopped.
    * @param parameters The body of the /setTaskParameters request.
    */
   @Override
   protected void finished(String parameters) {
      try {
       annotator.setTaskParameters(parameters);
       // save result in file named after annotationTaskId
       try {
          File f = new File(annotator.getAnnotatorId() + "-" + annotationTaskId + ".cfg");
          FileWriter out = new FileWriter(f);
          out.write(parameters);
          out.close();
       } catch(IOException exception) {
          System.err.println(""+exception);
       }
      } catch(InvalidConfigurationException exception) {
         System.err.println(""+exception);
         exception.printStackTrace(System.err);
      }
      System.exit(0);
   } // end of finished()

   /** Override this setter so it's not required as a command line switch */
   @Override public StandAloneWebApp setRoot(File newRoot) { return super.setRoot(newRoot); }
   
   /** Override this setter so it's not required as a command line switch */
   @Override public StandAloneWebApp setFinishedPath(String newFinishedPath) { return super.setFinishedPath(newFinishedPath); }
}
