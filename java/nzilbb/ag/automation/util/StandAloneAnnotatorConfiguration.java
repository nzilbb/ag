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
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.util.AnnotatorDescriptor;
import nzilbb.ag.automation.util.RequestRouter;
import nzilbb.util.IO;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;
import nzilbb.webapp.StandAloneWebApp;

/**
 * Utility for running a stand-alone webapp for configuring an
 * {@link nzilbb.ag.automation.Annotator} installation.
 */
@ProgramDescription(value="Utility for configuring an annotator installation.")
public class StandAloneAnnotatorConfiguration extends StandAloneWebApp {

   /** Command-line entrypoint */
   public static void main(String argv[]) {
      StandAloneAnnotatorConfiguration application = new StandAloneAnnotatorConfiguration();
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
   public StandAloneAnnotatorConfiguration setDebug(Boolean newDebug) { debug = newDebug; return this; }

   /**
    * Annotator class name.
    * @see #getClassName()
    * @see #setClassName(String)
    */
   protected String className;
   /**
    * Getter for {@link #className}: Annotator class name.
    * @return Annotator class name.
    */
   public String getClassName() { return className; }
   /**
    * Setter for {@link #className}: Annotator class name.
    * @param newClassName Annotator class name.
    */
   @Switch(value="Annotator class name",compulsory=true)
   public StandAloneAnnotatorConfiguration setClassName(String newClassName) { className = newClassName; return this; }

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
   public StandAloneAnnotatorConfiguration setDescriptor(AnnotatorDescriptor newDescriptor) { descriptor = newDescriptor; return this; }

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
   public StandAloneAnnotatorConfiguration setAnnotator(Annotator newAnnotator) { annotator = newAnnotator; return this; }

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
   public StandAloneAnnotatorConfiguration setRouter(RequestRouter newRouter) { router = newRouter; return this; }

   /**
    * Adds handlers which routes webapp resource requests to the Annotators "conf" webapp,
    * and routes server requests to Annotator.
    */
   protected void addHandlers() throws IOException {
      if (server == null) createServer();
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
                     response = descriptor.getResource("conf"+path);
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
   public StandAloneAnnotatorConfiguration() {
      setFinishedPath("install");
   }

   /**
    * Initialize the application.
    * @throws ClassNotFoundException If the implemenation for {@link #className} cannot be found.
    * @throws NoSuchMethodException If the annotator has no default constructor.
    * @throws IllegalAccessException If the annotator's default constructor is not public.
    * @throws InvocationTargetException If the annotator's constructor throws an exception.
    * @throws InstantiationException If the annotator is an abstract class.
    * @throws ClassCastException If the annotator does not extend {@link Annotator}.
    */
   public void init() throws ClassNotFoundException, NoSuchMethodException,
      InvocationTargetException, IllegalAccessException, InstantiationException,
      ClassCastException {
      // TODO handle loading from a specified jar file
      descriptor = new AnnotatorDescriptor(getClassName(), getClass().getClassLoader());
      annotator = descriptor.getInstance();
      router = new RequestRouter(annotator);
   } // end of init()

   /**
    * Called when the web app is finished and the server has been stopped.
    * @param result The body of the /finished request.
    */
   protected void finished(String result) {
      System.err.println(result);
      System.exit(0);
   } // end of finished()

   /** Override this setter so it's not required as a command line switch */
   @Override public StandAloneWebApp setRoot(File newRoot) { return super.setRoot(newRoot); }
   
   /** Override this setter so it's not required as a command line switch */
   @Override public StandAloneWebApp setFinishedPath(String newFinishedPath) { return super.setFinishedPath(newFinishedPath); }
}