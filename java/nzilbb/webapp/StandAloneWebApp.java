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
package nzilbb.webapp;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.InetSocketAddress;
import nzilbb.util.CommandLineProgram;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;
import nzilbb.util.IO;

@ProgramDescription(value="Utility for running stand-alone webapps")
public class StandAloneWebApp extends CommandLineProgram {
   
   public static void main(String argv[]) {
      StandAloneWebApp application = new StandAloneWebApp();
      if (application.processArguments(argv)) {
         application.start();
      }
   }

   /** HTTP server */
   protected HttpServer server;
   
   /**
    * Root directory of webapp.
    * @see #getRoot()
    * @see #setRoot(File)
    */
   protected File root;
   /**
    * Getter for {@link #root}: Root directory of webapp.
    * @return Root directory of webapp.
    */
   public File getRoot() { return root; }
   /**
    * Setter for {@link #root}: Root directory of webapp.
    * @param newRoot Root directory of webapp.
    */
   @Switch(value="Root folder of webapp",compulsory=true)
   public StandAloneWebApp setRoot(File newRoot) { root = newRoot; return this; }

   /**
    * Server port to listen on.
    * @see #getPort()
    * @see #setPort(Integer)
    */
   protected Integer port = Integer.valueOf(9000);
   /**
    * Getter for {@link #port}: Server port to listen on.
    * @return Server port to listen on.
    */
   public Integer getPort() { return port; }
   /**
    * Setter for {@link #port}: Server port to listen on.
    * @param newPort Server port to listen on.
    */
   @Switch("Server port to listen on (default is 9000)")
   public StandAloneWebApp setPort(Integer newPort) { port = newPort; return this; }

   /**
    * The URI path (excluding leading '/') for the request that terminates the web
    * app. Default is "finished". 
    * @see #getFinishedPath()
    * @see #setFinishedPath(String)
    */
   protected String finishedPath = "finished";
   /**
    * Getter for {@link #finishedPath}: The URI path (excluding leading '/') for the
    * request that terminates the web app. Default is "finished". 
    * @return The URI path (excluding leading '/') for the request that terminates the web
    * app. Default is "finished". 
    */
   public String getFinishedPath() { return finishedPath; }
   /**
    * Setter for {@link #finishedPath}: The URI path (excluding leading '/') for the
    * request that terminates the web app. Default is "finished". 
    * @param newFinishedPath The URI path (excluding leading '/') for the request that
    * terminates the web app. Default is "finished". 
    */
   @Switch("The URI pathfor the request that terminates the web app. Default is 'finished'")
   public StandAloneWebApp setFinishedPath(String newFinishedPath) { finishedPath = newFinishedPath; return this; }

   /**
    * The HTML document to show when /{@link #finishedPath} is called.
    * @see #getFinishedResponse()
    * @see #setFinishedResponse(String)
    */
   protected String finishedResponse = "<html><head><title>Finished</title></head>"
      +"<body>"
      +"<div style='display: table-cell; width: 99vw; height: 95vh; vertical-align: middle; text-align: center;'>"
      +"You can now close the browser window."
      +"</div>"
      +"<script type='text/javascript'>window.close();</script>"
      +"</body></html>";
   /**
    * Getter for {@link #finishedResponse}: The HTML document to show when /{@link
    * #finishedPath} is called. 
    * @return The HTML document to show when /{@link #finishedPath} is called.
    */
   public String getFinishedResponse() { return finishedResponse; }
   /**
    * Setter for {@link #finishedResponse}: The HTML document to show when 
    * /{@link #finishedPath} is called. 
    * @param newFinishedResponse The HTML document to show when /{@link #finishedPath} is called.
    */
   @Switch("The HTML document to show when /finishedPath is called")
   public StandAloneWebApp setFinishedResponse(String newFinishedResponse) { finishedResponse = newFinishedResponse; return this; }

   class ResourceHandler implements HttpHandler {
      File resource;
      String contentType;
      public ResourceHandler(File resource) {
         this.resource = resource;
         if (resource.getName().endsWith(".html"))
            contentType = "text/html";
         else if (resource.getName().endsWith(".js"))
            contentType = "text/javascript";
         else if (resource.getName().endsWith(".json"))
            contentType = "application/json";
         else if (resource.getName().endsWith(".css"))
            contentType = "text/css";
         else if (resource.getName().endsWith(".png"))
            contentType = "image/png";
         else if (resource.getName().endsWith(".jpg"))
            contentType = "image/jpeg";
         else if (resource.getName().endsWith(".svg"))
            contentType = "image/svg";
         else if (resource.getName().endsWith(".ico"))
            contentType = "image/vnd.microsoft.icon";
         else
            contentType = "application/octet-stream";
      }
      public void handle(HttpExchange he) throws IOException {
         System.out.println("Serve: " + resource.getName() + " " + contentType);
         he.getResponseHeaders().add("Content-Type", contentType);
         he.sendResponseHeaders(200, resource.length());
         IO.Pump(new FileInputStream(resource), he.getResponseBody());
      }
   }
   
   /**
    * Recursively adds resource handlers
    * @param context The URL path for the resource.
    * @param resource The resource to serve.
    */
   protected void recursivelyAddHandlers(String context, File resource) {
      if (resource.isDirectory()) {
         for (File f : resource.listFiles()) {
            recursivelyAddHandlers(context + "/" + f.getName(), f);
         } // next file
      } else {
         System.out.println("context " + context + " resource " + resource.getPath());
         server.createContext(context, new ResourceHandler(resource));
         if (resource.getName().equals("index.html")) {
            server.createContext(
               context.replaceAll("index\\.html$",""), new ResourceHandler(resource));
         }
      }
   } // end of resource()
   
   public void start() {
      try {
         // create web server
         server = HttpServer.create(new InetSocketAddress(port), 0);
         System.out.println("server started at " + port);
         recursivelyAddHandlers("", root);

         // handler for signalling the app is finished
         server.createContext("/" + finishedPath, new HttpHandler() {
               public void handle(HttpExchange he) throws IOException {
                  System.out.println("Stopping.");
                  String result = IO.InputStreamToString(he.getRequestBody());
                  if (result.length() == 0) result = he.getRequestURI().getQuery();
                  he.sendResponseHeaders(200, finishedResponse.length());
                  OutputStream os = he.getResponseBody();
                  os.write(finishedResponse.getBytes());
                  os.close();
                  new Thread(new Runnable() { public void run() {
                     server.stop(0); // this never seems to return??
                  } }).start();
                  try { Thread.sleep(500); } catch (Exception x) {}
                  finished(result);
               }});
         
         server.setExecutor(null);
         server.start();

         // open browser
         java.awt.Desktop.getDesktop().browse(new URI("http://localhost:" + port));
         
      } catch(Exception exception) {
         System.err.println("ERROR: " + exception);
         finished(null);
      }
   }
   
   /**
    * Called when the web app is finished and the server has been stopped.
    * @param result The body of the /finished request.
    */
   protected void finished(String result) {
      System.out.println(result);
      System.exit(0);
   } // end of finished()

}
