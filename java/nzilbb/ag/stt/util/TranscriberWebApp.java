//
// Copyright 2021 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag.stt.util;

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
import java.net.URL;
import nzilbb.ag.Constants;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.stt.Transcriber;
import nzilbb.ag.stt.InvalidConfigurationException;
import nzilbb.ag.stt.util.TranscriberDescriptor;
import nzilbb.ag.stt.util.RequestRouter;
import nzilbb.util.IO;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;
import nzilbb.webapp.StandAloneWebApp;

/**
 * Base class for collecting together common functionality of Annatotor web app
 * utilities.
 */
public class TranscriberWebApp extends StandAloneWebApp {

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
    * implements the transcriber. 
    * @see #getTranscriberName()
    */
   protected String transcriberName;
   /**
    * Getter for {@link #transcriberName}: The name of either a .jar file, or a class (if
    * it's on the classpath), which implements the transcriber. 
    * @return The name of either a .jar file, or a class (if it's on the classpath), which
    * implements the transcriber. 
    */
   public String getTranscriberName() { return transcriberName; }

   /**
    * Descriptor for the transcriber.
    * @see #getDescriptor()
    * @see #setDescriptor(TranscriberDescriptor)
    */
   protected TranscriberDescriptor descriptor;
   /**
    * Getter for {@link #descriptor}: Descriptor for the transcriber.
    * @return Descriptor for the transcriber.
    */
   public TranscriberDescriptor getDescriptor() { return descriptor; }
   /**
    * Setter for {@link #descriptor}: Descriptor for the transcriber.
    * @param newDescriptor Descriptor for the transcriber.
    */
   public TranscriberWebApp setDescriptor(TranscriberDescriptor newDescriptor) { descriptor = newDescriptor; return this; }

   /**
    * The transcriber to configure.
    * @see #getTranscriber()
    * @see #setTranscriber(Transcriber)
    */
   protected Transcriber transcriber;
   /**
    * Getter for {@link #transcriber}: The transcriber to configure.
    * @return The transcriber to configure.
    */
   public Transcriber getTranscriber() { return transcriber; }
   /**
    * Setter for {@link #transcriber}: The transcriber to configure.
    * @param newTranscriber The transcriber to configure.
    */
   public TranscriberWebApp setTranscriber(Transcriber newTranscriber) { transcriber = newTranscriber; return this; }

   /**
    * Router for sending requests to transcriber.
    * @see #getRouter()
    */
   protected RequestRouter router;
   /**
    * Getter for {@link #router}: Router for sending requests to transcriber.
    * @return Router for sending requests to transcriber.
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
    * Name of subdirectory of the web-app, relative to the directory of the Transcriber class.
    * @see #getSubdirectory()
    * @see #setSubdirectory(String)
    */
   protected String subdirectory = "";
   /**
    * Getter for {@link #subdirectory}: Name of subdirectory of the web-app, relative to
    * the directory of the Transcriber class. 
    * @return Name of subdirectory of the web-app, relative to the directory of the
    * Transcriber class. 
    */
   public String getSubdirectory() { return subdirectory; }
   /**
    * Setter for {@link #subdirectory}: Name of subdirectory of the web-app, relative to
    * the directory of the Transcriber class. 
    * @param newSubdirectory Name of subdirectory of the web-app, relative to the
    * directory of the Transcriber class. 
    */
   public TranscriberWebApp setSubdirectory(String newSubdirectory) { subdirectory = newSubdirectory; return this; }

   /** Constructor */
   public TranscriberWebApp() {
   }

   /**
    * Adds handlers which routes webapp resource requests to the Transcribers "conf" webapp,
    * and routes server requests to Transcriber.
    */
   protected void addHandlers() throws IOException {
      if (server == null) createServer();

      // add getSchema handler
      server.createContext("/getSchema", new HttpHandler() {
            public void handle(HttpExchange x) throws IOException {
               String json = transcriber.getSchema().toJson().toString();
               x.getResponseHeaders().add("Content-Type", "application/json");
               x.sendResponseHeaders(200, json.length());
               OutputStream os = x.getResponseBody();
               os.write(json.getBytes());
               os.close();
            }});      
      
      // add util.js handler, providing some useful javascript functions for webapp implementations
      server.createContext("/util.js", new HttpHandler() {
            public void handle(HttpExchange x) throws IOException {
               URL url = getClass().getResource("util.js");
               if (url != null) {
                  try {
                     x.getResponseHeaders().add("Content-Type", "text/javascript");
                     x.sendResponseHeaders(200, 0);
                     IO.Pump(url.openConnection().getInputStream(), x.getResponseBody());
                  } catch(IOException exception) {}
               }
               // if we got here, it went wrong
               x.sendResponseHeaders(404, -1);
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
                  if (debug) System.err.println("transcriber: " + uri);
                  // everything else is routed to the transcriber...
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
    * @throws ClassNotFoundException If the implemenation for {@link #transcriberName}
    * cannot be found. 
    * @throws NoSuchMethodException If the transcriber has no default constructor.
    * @throws IllegalAccessException If the transcriber's default constructor is not public.
    * @throws InvocationTargetException If the transcriber's constructor throws an exception.
    * @throws InstantiationException If the transcriber is an abstract class.
    * @throws ClassCastException If the transcriber does not extend {@link Transcriber}.
    * @throws IOException If the transcriber jar file cannot be opened.
    */
   public void init() throws ClassNotFoundException, NoSuchMethodException,
      InvocationTargetException, IllegalAccessException, InstantiationException,
      ClassCastException, IOException {

      if (arguments.size() == 0) {
         throw new ClassCastException(
            "No transcriber .jar file or class name provided.\nTry --usage.");
      }
      transcriberName = arguments.firstElement();

      // is the name a jar file name or a class name
      try { // try as a jar file
         descriptor = new TranscriberDescriptor(new File(transcriberName));
      } catch (Throwable notAJarName) { // try as a class name
         descriptor = new TranscriberDescriptor(transcriberName, getClass().getClassLoader());
      }
      
      transcriber = descriptor.getInstance();
      router = new RequestRouter(transcriber);

      // give the transcriber the resources it needs...
      
      transcriber.setSchema( // TODO make this configurable?
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
      
      File transcriberDir = new File(workingDir, transcriber.getTranscriberId());
      if (!transcriberDir.exists()) transcriberDir.mkdir();
      transcriber.setWorkingDirectory(transcriberDir);      
      
   } // end of init()
   
   /** Override this setter so it's not required as a command line switch */
   @Override public StandAloneWebApp setRoot(File newRoot) { return super.setRoot(newRoot); }
   
   /** Override this setter so it's not required as a command line switch */
   @Override public StandAloneWebApp setFinishedPath(String newFinishedPath) { return super.setFinishedPath(newFinishedPath); }
}
