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
import nzilbb.ag.Constants;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.util.AnnotatorDescriptor;
import nzilbb.ag.automation.util.RequestRouter;
import nzilbb.util.IO;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;
import nzilbb.webapp.StandAloneWebApp;
import org.json.JSONObject;

/**
 * Utility for running a stand-alone webapp for configuring the parameters of an
 * annotation task.
 * <p> This can be run from the command like this:
 * <p><tt> java -classpath nzilbb.ag.jar nzilbb.ag.automation.util.TaskApp --annotationTaskId=test myjar.jar </tt>
 */
@ProgramDescription(
   value="Utility for configuring the parameters of an annotation task.",
   arguments="name.of.annotator.class.or.jar taskId")
public class TaskApp extends AnnotatorWebApp {

   /** Command-line entrypoint */
   public static void main(String argv[]) {
      TaskApp application = new TaskApp();
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
    * Setter for {@link #debug}: Whether to print debug tracing.
    * @param newDebug Whether to print debug tracing.
    */
   @Switch("Whether to print debug tracing")
   public TaskApp setDebug(Boolean newDebug) { debug = newDebug; return this; }

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
   public TaskApp setAnnotationTaskId(String newAnnotationTaskId) { annotationTaskId = newAnnotationTaskId; return this; }

   /**
    * Setter for {@link #workingDir}: Working directory.
    * @param newWorkingDir Working directory.
    */
   @Switch("Directory for working/config files (default is the current directory)")
   public TaskApp setWorkingDir(File newWorkingDir) { workingDir = newWorkingDir; return this; }
   
   /**
    * Adds handlers which routes webapp resource requests to the Annotators "conf" webapp,
    * and routes server requests to Annotator.
    */
   protected void addHandlers() throws IOException {
      super.addHandlers();

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
               echoContentType(x);
               x.sendResponseHeaders(200, parameters.length());
               OutputStream os = x.getResponseBody();
               os.write(parameters.getBytes());
               os.close();
            }});      
      
   } // end of addHandlers()

   /** Constructor */
   public TaskApp() {
      setFinishedPath("setTaskParameters");
      setSubdirectory("task");
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
      super.init();
      
      if (arguments.size() < 2) {
         throw new ClassCastException("No taskId provided.\nTry --usage.");
      }
      annotationTaskId = arguments.elementAt(1);
      setQuery(annotationTaskId);
      
      if (!descriptor.hasTaskWebapp()) {
         throw new FileNotFoundException("Annotator has no 'task' web app.");
      }

      // set a response that will follow the progress of the installation
      finishedResponse = "<html><head><title>Task Configuration</title></head><body>"
         +"<p style='text-align: center;'><big>Thanks</big></p>"
         +"<p style='text-align: center;'>You can close this window.</p>"
         +"</body></html>";

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
