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
import java.io.FileNotFoundException;
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
 * Base class for collecting together common functionality of Annatotor web app
 * utilities.
 */
public class AnnotatorWebApp extends StandAloneWebApp {

   /**
    * Whether to print debug tracing.
    * @see #getDebug()
    */
   protected Boolean debug = Boolean.FALSE;
   /**
    * Getter for {@link #debug}: Whether to print debug tracing.
    * @return Whether to print debug tracing.
    */
   public Boolean getDebug() { return debug; }

   /**
    * The name of either a .jar file, or a class (if it's on the classpath), which
    * implements the annotator. 
    * @see #getAnnotatorName()
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
   public AnnotatorWebApp setDescriptor(AnnotatorDescriptor newDescriptor) { descriptor = newDescriptor; return this; }

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
   public AnnotatorWebApp setAnnotator(Annotator newAnnotator) { annotator = newAnnotator; return this; }

   /**
    * Router for sending requests to annotator.
    * @see #getRouter()
    */
   protected RequestRouter router;
   /**
    * Getter for {@link #router}: Router for sending requests to annotator.
    * @return Router for sending requests to annotator.
    */
   public RequestRouter getRouter() { return router; }

   /**
    * Working directory.
    * @see #getWorkingDir()
    */
   protected File workingDir = new File(".");
   /**
    * Getter for {@link #workingDir}: Working directory.
    * @return Working directory.
    */
   public File getWorkingDir() { return workingDir; }
   
   /**
    * Name of subdirectory of the web-app, relative to the directory of the Annotator class.
    * @see #getSubdirectory()
    * @see #setSubdirectory(String)
    */
   protected String subdirectory = "";
   /**
    * Getter for {@link #subdirectory}: Name of subdirectory of the web-app, relative to
    * the directory of the Annotator class. 
    * @return Name of subdirectory of the web-app, relative to the directory of the
    * Annotator class. 
    */
   public String getSubdirectory() { return subdirectory; }
   /**
    * Setter for {@link #subdirectory}: Name of subdirectory of the web-app, relative to
    * the directory of the Annotator class. 
    * @param newSubdirectory Name of subdirectory of the web-app, relative to the
    * directory of the Annotator class. 
    */
   public AnnotatorWebApp setSubdirectory(String newSubdirectory) { subdirectory = newSubdirectory; return this; }

   /** Constructor */
   public AnnotatorWebApp() {
   }

   /**
    * Adds handlers which routes webapp resource requests to the Annotators "conf" webapp,
    * and routes server requests to Annotator.
    */
   protected void addHandlers() throws IOException {
      if (server == null) createServer();

      // add getSchema handler
      server.createContext("/getSchema", new HttpHandler() {
            public void handle(HttpExchange x) throws IOException {
               String json = new JSONObject(annotator.getSchema()).toString();
               x.getResponseHeaders().add("Content-Type", "application/json");
               x.sendResponseHeaders(200, json.length());
               OutputStream os = x.getResponseBody();
               os.write(json.getBytes());
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
                  if (debug) System.err.println("getResource: "+subdirectory+path);
                  try {
                     response = descriptor.getResource(subdirectory+path);
                  } catch(Throwable exception) {
                     if (debug) System.err.println("could not getResource: "+exception);
                  }
                  if (response == null) status = 404;
               } else {
                  if (debug) System.err.println("annotator: " + uri);
                  // everything else is routed to the annotator...
                  try {
                     response = router.request(
                        x.getRequestMethod(), uri, x.getRequestHeaders().getFirst("Content-Type"),
                        x.getRequestBody());
                     echoContentType(x);
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
   
   /**
    * If the request specifies an expected content type, set the reponse Content-Type to that.
    * @param x
    */
   public void echoContentType(HttpExchange x) {
      String accept = x.getRequestHeaders().getFirst("Accept");
      if (accept != null) {
         // Something like "*/*"
         // or "text/html, application/xhtml+xml, application/xml;q=0.9, */*;q=0.8" 
         // we'll take the first one
         String contentType = accept.split(",")[0].trim();
         // strip any q parameter
         contentType = contentType.split(";")[0].trim();
         // ignore */*
         if (!contentType.equals("*/*")) {
            x.getResponseHeaders().add("Content-Type", contentType);
         }
      }
   } // end of echoContentType()


   /**
    * Initialize the application.
    * @throws ClassNotFoundException If the implemenation for {@link #annotatorName}
    * cannot be found. 
    * @throws NoSuchMethodException If the annotator has no default constructor.
    * @throws IllegalAccessException If the annotator's default constructor is not public.
    * @throws InvocationTargetException If the annotator's constructor throws an exception.
    * @throws InstantiationException If the annotator is an abstract class.
    * @throws ClassCastException If the annotator does not extend {@link Annotator}.
    * @throws IOException If the annotator jar file cannot be opened.
    */
   public void init() throws ClassNotFoundException, NoSuchMethodException,
      InvocationTargetException, IllegalAccessException, InstantiationException,
      ClassCastException, IOException {

      if (arguments.size() == 0) {
         throw new ClassCastException(
            "No annotator .jar file or class name provided.\nTry --usage.");
      }
      annotatorName = arguments.firstElement();

      // is the name a jar file name or a class name
      try { // try as a jar file
         descriptor = new AnnotatorDescriptor(new File(annotatorName));
      } catch (Throwable notAJarName) { // try as a class name
         descriptor = new AnnotatorDescriptor(annotatorName, getClass().getClassLoader());
      }
      
      annotator = descriptor.getInstance();      
      router = new RequestRouter(annotator);

      // give the annotator the resources it needs...
      
      annotator.setSchema( // TODO make this configurable?
         new Schema(
            "who", "turn", "utterance", "word",
            new Layer("transcript_language", "Overall Language")
            .setAlignment(Constants.ALIGNMENT_NONE)
            .setPeers(false).setPeersOverlap(false).setSaturated(true),
            new Layer("who", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
            .setPeers(true).setPeersOverlap(true).setSaturated(true),
            new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
            .setPeers(true).setPeersOverlap(false).setSaturated(false)
            .setParentId("who").setParentIncludes(true),
            new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
            .setPeers(true).setPeersOverlap(false).setSaturated(true)
            .setParentId("turn").setParentIncludes(true),
            new Layer("language", "Phrase Language").setAlignment(Constants.ALIGNMENT_INTERVAL)
            .setPeers(true).setPeersOverlap(false).setSaturated(false)
            .setParentId("turn").setParentIncludes(true),
            new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
            .setPeers(true).setPeersOverlap(false).setSaturated(false)
            .setParentId("turn").setParentIncludes(true)));
      
      if (annotator instanceof UsesFileSystem) {
         ((UsesFileSystem)annotator).setWorkingDirectory(workingDir);
      }
   } // end of init()
   
   /** Override this setter so it's not required as a command line switch */
   @Override public StandAloneWebApp setRoot(File newRoot) { return super.setRoot(newRoot); }
   
   /** Override this setter so it's not required as a command line switch */
   @Override public StandAloneWebApp setFinishedPath(String newFinishedPath) { return super.setFinishedPath(newFinishedPath); }
}
