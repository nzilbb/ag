//
// Copyright 2016-2020 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.formatter.transcriber;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FileFilter;
import java.util.Vector;
import nzilbb.util.CommandLineProgram;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;

/**
 * Utility for checking and correcting the structure of transcriber transcripts.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Utility for checking and correcting the structure of transcriber transcripts",arguments="first.trs [ second.trs ...]")
public class Validator extends CommandLineProgram {
   
   public static void main(String argv[]) {
      Validator application = new Validator();
      application.setUsage(argv.length == 0);
      if (application.processArguments(argv)) {
         application.start();
      }
   }

   // Attributes
   
   /**
    * Print all validation messages.
    * @see #getVerbose()
    * @see #setVerbose(Boolean)
    */
   protected Boolean verbose = Boolean.FALSE;
   /**
    * Getter for {@link #verbose}: Print all validation messages.
    * @return Print all validation messages.
    */
   public Boolean getVerbose() { return verbose; }
   /**
    * Setter for {@link #verbose}: Print all validation messages.
    * @param newVerbose Print all validation messages.
    */
   @Switch("Print all validation messages")
   public Validator setVerbose(Boolean newVerbose) { verbose = newVerbose; return this; }

   /**
    * Whether to replace the original file or create a copy.
    * @see #getReplace()
    * @see #setReplace(Boolean)
    */
   protected Boolean replace = Boolean.FALSE;
   /**
    * Getter for {@link #replace}: Whether to replace the original file or create a copy.
    * @return Whether to replace the original file or create a copy.
    */
   public Boolean getReplace() { return replace; }
   /**
    * Setter for {@link #replace}: Whether to replace the original file or create a copy.
    * @param newReplace Whether to replace the original file or create a copy.
    */
   @Switch("Replace original file instead of creating a copy in a subfolder called 'valid'")
   public Validator setReplace(Boolean newReplace) { replace = newReplace; return this; }

   // Methods:   

   public void start() {
      for (String argument: arguments) {
  	 File f = new File(argument);
         if (!f.exists()) {
            System.err.println("Transcript not found: " + argument);
            continue;
         }
         if (!f.getName().endsWith(".trs") && !f.isDirectory()) {
            System.err.println("Ignoring non transcript: " + argument);
            continue;
         }         

         processFile(f);
      } // next argument
   }
   
   /**
    * Processes the given file. If it's a transcript, it's validated. If it's a directory,
    * it's recursively traversed. 
    * @param f Transcript or directory to process.
    */
   public void processFile(File f) {
      if (f.isDirectory()) {
         // traverse directory
         for (File file : f.listFiles(new FileFilter() {
               public boolean accept(File pathname) {
                  return pathname.getName().endsWith(".trs") || pathname.isDirectory();
               }
            })) {
            processFile(file);
         }
      } else {
         validateTranscript(f);
      }
   } // end of processFile()

   
   /**
    * Validate the given transcript.
    * @param trs The transcript to process.
    */
   public void validateTranscript(File trs) {
         Transcript transcript = new Transcript();
         try {
            transcript.load(new FileInputStream(trs));
            
            Vector<String> errors = transcript.validationErrors();
            if (verbose && errors != null) {
               for (String error : errors) {
                  System.out.println(trs.getPath() + " : " + error);
               } // next error
            }
            if (!replace) { // create a copy
               File validDir = new File(trs.getParentFile(), "valid");
               if (!validDir.exists() && !validDir.mkdir()) {
                  System.err.println(
                     "Could not create directory for " + trs.getPath() + ": " + validDir.getPath());
                  return;
               }
               trs = new File(validDir, trs.getName());
            }
            FileWriter writer = new FileWriter(trs);
            transcript.writeText(writer);
            writer.close();
            System.out.println(trs.getPath());
         } catch (Exception x) {
            System.err.println("Error processing " + trs.getPath() + ": " + x);
            x.printStackTrace(System.err);
         }
   } // end of validateTranscript()

} // end of class Validator
