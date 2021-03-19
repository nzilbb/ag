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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import javax.swing.JFrame;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import nzilbb.ag.stt.Transcriber;
import nzilbb.util.CommandLineProgram;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;
import nzilbb.util.IO;

/**
 * Utility for displaying transcriber information.
 * <p> This can be run from the command like this:
 * <p><tt> java -classpath nzilbb.ag.jar nzilbb.ag.stt.util.Info myjar.jar </tt>
 */
@ProgramDescription(value="Utility for displaying automatic transcriber information",arguments="class.or.jar")
public class Info extends CommandLineProgram {
   /** Command-line entrypoint */
   public static void main(String argv[]) {
      Info application = new Info();
      if (application.processArguments(argv)) {
         try {
            application.start();
         } catch(Exception exception) {
            System.err.println("Could not start: "+exception.toString());
         }
      }
   }   

   /**
    * Whether to show a graphical user interface (true) or write info to stdout (false). 
    * Default is true.
    * @see #getGui()
    * @see #setGui(Boolean)
    */
   protected Boolean gui = Boolean.TRUE;
   /**
    * Getter for {@link #gui}: Whether to show a graphical user interface (true) or write
    * info to stdout (false). Default is true.  
    * @return Whether to show a graphical user interface (true) or write info to stdout
    * (false). Default is true. 
    */
   public Boolean getGui() { return gui; }
   /**
    * Setter for {@link #gui}: Whether to show a graphical user interface (true) or write
    * info to stdout (false). Default is true. 
    * @param newGui Whether to show a graphical user interface (true) or write info to
    * stdout (false). Default is true. 
    */
   @Switch("Whether to show a graphical user interface or not")
   public Info setGui(Boolean newGui) { gui = newGui; return this; }

   /**
    * The name of either a .jar file, or a class (if it's on the classpath), which
    * implements the transcriber. 
    * @see #getTranscriberName()
    * @see #setTranscriberName(String)
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
    * Setter for {@link #transcriberName}: The name of either a .jar file, or a class (if
    * it's on the classpath), which implements the transcriber. 
    * @param newTranscriberName The name of either a .jar file, or a class (if it's on the
    * classpath), which implements the transcriber. 
    */
   public Info setTranscriberName(String newTranscriberName) { transcriberName = newTranscriberName; return this; }

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
   public Info setDescriptor(TranscriberDescriptor newDescriptor) { descriptor = newDescriptor; return this; }

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
   public Info setTranscriber(Transcriber newTranscriber) { transcriber = newTranscriber; return this; }

   /** Start handling requests, and  */
   public void start() {

      if (arguments.size() == 0) {
         System.err.println("No transcriber .jar file or class name provided.\nTry --usage.");
         System.exit(1);
      }
      transcriberName = arguments.firstElement();
      
      // is the name a jar file name or a class name
      try { // try as a jar file
         descriptor = new TranscriberDescriptor(new File(transcriberName));
      } catch (Throwable notAJarName) { // try as a class name
         try {
            descriptor = new TranscriberDescriptor(transcriberName, getClass().getClassLoader());
         } catch(Throwable notAClassName) {
            if (gui) {
             try {
                JOptionPane.showConfirmDialog(
                   null, "Could not get transcriber: " + transcriberName, "Error",
                   JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
             } catch(Throwable exception) {
                gui = false;
             }
            }
            if (!gui) {
               System.err.println("Could not get transcriber: " + transcriberName);
            }
            return;
         }
      }

      // get an instance of the transcriber
      transcriber = descriptor.getInstance();

      if (gui) {
         try {            
            // window to display stuff in
            JFrame frame = new JFrame(
               transcriber.getTranscriberId() + " " + transcriber.getVersion());
            Toolkit toolkit = frame.getToolkit();
            Dimension screenSize = toolkit.getScreenSize();
            int width = 800;
            int height = 600;
            int top = (screenSize.height - height) / 2;
            int left = (screenSize.width - width) / 2;
            frame.setSize(width, height);
            frame.setLocation(left, top);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            // HTML viewer
            JEditorPane pane = new JEditorPane("text/html", ""+descriptor.getInfo());
            pane.setEditable(false);

            // add some styles to the html
            HTMLEditorKit kit = new HTMLEditorKit();
            //pane.setEditorKit(kit); // TODO this make the document go blank??
            StyleSheet styleSheet = kit.getStyleSheet();
            styleSheet.addRule("body { font-family:'DejaVu Sans', Arial, Helvetica, sans-serif; }");
            styleSheet.addRule("h1 { font-size: 17pt; color: #859044; text-align: center; }");
            styleSheet.addRule("h2 { font-size: 16pt; color: #859044; text-align: center; }");
            styleSheet.addRule("h2 { font-size: 15pt; color: #859044;  }");

            frame.getContentPane().setLayout(new BorderLayout());
            frame.getContentPane().add(new JScrollPane(pane), BorderLayout.CENTER);

            // show it
            frame.setVisible(true);
         } catch(Throwable t) {
            // if it all goes horribly wrong because we're headless or something...
            System.out.println(""+t);
            // print to stdout at least
            gui = false;
         }
      }
      if (!gui) {
         // output to different streams, in case the user wants just HTML
         System.err.println(transcriber.getTranscriberId() + " " + transcriber.getVersion());
         System.out.println(""+descriptor.getInfo());
      }
   }

}
