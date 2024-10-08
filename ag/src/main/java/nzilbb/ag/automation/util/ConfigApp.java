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
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.sql.SQLException;
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

/**
 * Utility for running a stand-alone webapp for configuring an
 * {@link nzilbb.ag.automation.Annotator} installation.
 * <p> This can be run from the command like this:
 * <p><tt> java -classpath nzilbb.ag.jar nzilbb.ag.automation.util.ConfigApp myjar.jar </tt>
 */
@ProgramDescription(
   value="Utility for configuring an annotator installation.",
   arguments="name.of.annotator.class.or.jar")
public class ConfigApp extends AnnotatorWebApp {

   /** Command-line entrypoint */
   public static void main(String argv[]) {
      ConfigApp application = new ConfigApp();
      if (application.processArguments(argv)) {
         try {
            application.init();
            application.start();
         } catch(Exception exception) {
            System.err.println("Could not start: "+exception.toString());
            exception.printStackTrace(System.err);
         }
      }
   }

   /**
    * Setter for {@link #debug}: Whether to print debug tracing.
    * @param newDebug Whether to print debug tracing.
    */
   @Switch("Whether to print debug tracing")
   public ConfigApp setDebug(Boolean newDebug) { debug = newDebug; return this; }

   /**
    * Setter for {@link #workingDir}: Working directory.
    * @param newWorkingDir Working directory.
    */
   @Switch("Directory for working/config files (default is the current directory)")
   public ConfigApp setWorkingDir(File newWorkingDir) { workingDir = newWorkingDir; return this; } 

   /** Constructor */
   public ConfigApp() {
      setFinishedPath("setConfig");
      setSubdirectory("config");
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
    * @throws IOException If the annotator jar file cannot be opened.
    */
   @Override
   public void init() throws ClassNotFoundException, NoSuchMethodException,
      InvocationTargetException, IllegalAccessException, InstantiationException,
      ClassCastException, IOException, SQLException {
      super.init();
      
      if (!descriptor.hasConfigWebapp()) {
         throw new FileNotFoundException("Annotator has no 'config' web app.");
      }
      
      // set a response that will follow the progress of the installation
      finishedResponse = "<html><head><title>Installing...</title></head><body>"
         +"<progress id='p' value='0' max='100' style='width: 100%'>Installing...</progress>"
         +"<p id='m' style='text-align:center;'></p>"
         +"<script type='text/javascript'>"
         +"\nfunction p() {"
         +"\n  var request = new XMLHttpRequest();"
         +"\n  request.open('GET', 'getStatus');"
         +"\n  request.addEventListener('load', function(e) {"
         +"\n    var message = document.getElementById('m');"
         +"\n    message.innerHTML = this.responseText;"
         +"\n    var request = new XMLHttpRequest();"
         +"\n    request.open('GET', 'getPercentComplete');"
         +"\n    request.addEventListener('load', function(e) {"
         +"\n      var progress = document.getElementById('p');"
         +"\n      progress.value = this.responseText;"
         +"\n      if (progress.value < 100) window.setTimeout(p, 500);"
         +"\n      else document.getElementById('m').innerHTML = 'You can close this window.';"
         +"\n    }, false);"
         +"\n    request.send();"
         +"\n  }, false);"
         +"\n  request.send();"
         +"\n}"
         +"\np();"
         +"</script>"
         +"</body></html>";
      
   } // end of init()

   /**
    * Called when the web app is finished and the server has been stopped.
    * @param result The body of the /finished request.
    */
   @Override
   protected void finished(String result) {
      System.err.println("finished: " + result);
      try {
         annotator.setConfig(result);
         
         // The annotator has to do its own config persistence, so don't do this:
         // if (annotator.getWorkingDirectory() != null) {
         //   // save the configuration in a local file
         //   File f = new File(
         //     annotator.getWorkingDirectory(), annotator.getAnnotatorId() + ".cfg");
         //   if (result != null) {
         //     try {
         //       PrintWriter writer = new PrintWriter(f, "UTF-8");
         //       writer.write(result);
         //       writer.close();
         //     } catch(IOException x) {
         //       System.err.println("Could not write configuration file "+f.getPath()+": "+x);
         //       x.printStackTrace(System.err);
         //     }
         //   }
         // }

      } catch(InvalidConfigurationException exception) {
         System.err.println(""+exception);
         exception.printStackTrace(System.err);
      }
      try {
       Thread.sleep(1000); // give the browser a chance to get the last status
      } catch(Exception exception) {}
      System.exit(0);
   } // end of finished()

}
