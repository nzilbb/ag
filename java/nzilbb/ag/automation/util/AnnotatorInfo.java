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
import nzilbb.ag.automation.Annotator;
import nzilbb.util.CommandLineProgram;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;
import nzilbb.util.IO;

/**
 * Utility for displaying annotator information.
 */
@ProgramDescription(value="Utility for running stand-alone webapps",arguments="class.or.jar")
public class AnnotatorInfo extends CommandLineProgram {
   /** Command-line entrypoint */
   public static void main(String argv[]) {
      AnnotatorInfo application = new AnnotatorInfo();
      if (application.processArguments(argv)) {
         try {
            application.start();
         } catch(Exception exception) {
            System.err.println("Could not start: "+exception.toString());
         }
      }
   }   

   /**
    * Whether to show a graphical user interface (true) or write info to stdout (false). Default is true.
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
   public AnnotatorInfo setGui(Boolean newGui) { gui = newGui; return this; }

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
   public AnnotatorInfo setAnnotatorName(String newAnnotatorName) { annotatorName = newAnnotatorName; return this; }

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
   public AnnotatorInfo setDescriptor(AnnotatorDescriptor newDescriptor) { descriptor = newDescriptor; return this; }

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
   public AnnotatorInfo setAnnotator(Annotator newAnnotator) { annotator = newAnnotator; return this; }

   /** Start handling requests, and  */
   public void start() {

      if (arguments.size() == 0) {
         System.err.println("No annotator .jar file or class name provided.\nTry --usage.");
         System.exit(1);
      }
      annotatorName = arguments.firstElement();
      
      // is the name a jar file name or a class name
      try { // try as a jar file
         descriptor = new AnnotatorDescriptor(new File(annotatorName));
      } catch (Throwable notAJarName) { // try as a class name
         try {
            descriptor = new AnnotatorDescriptor(annotatorName, getClass().getClassLoader());
         } catch(Throwable notAClassName) {
            if (gui) {
             try {
                JOptionPane.showConfirmDialog(
                   null, "Could not get annotator: " + annotatorName, "Error",
                   JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
             } catch(Throwable exception) {
                gui = false;
             }
            }
            if (!gui) {
               System.err.println("Could not get annotator: " + annotatorName);
            }
            return;
         }
      }

      // get an instance of the annotator
      annotator = descriptor.getInstance();

      if (gui) {
         try {            
            // window to display stuff in
            JFrame frame = new JFrame(annotator.getAnnotatorId() + " " + annotator.getVersion());
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
         System.err.println(annotator.getAnnotatorId() + " " + annotator.getVersion());
         System.out.println(""+descriptor.getInfo());
      }
   }

}
