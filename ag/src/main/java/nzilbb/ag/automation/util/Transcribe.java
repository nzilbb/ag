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
package nzilbb.ag.automation.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import nzilbb.ag.Graph;
import nzilbb.ag.Constants;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.serialize.json.JSONSerialization;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.Transcriber;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.util.IO;
import nzilbb.configure.ParameterSet;
import nzilbb.util.ProgramDescription;
import nzilbb.util.CommandLineProgram;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;
import nzilbb.ag.serialize.util.Utility;

/**
 * Command line utility for manual testing of automatic Transcriber modules.
 * <p> This can be invoked from the command line something like this:
 * <br> <tt>java -cp nzilbb.ag.jar nzilbb.ag.automation.util.Transcribe nzilbb.transcriber.whisper.jar speech.wav </tt>
 */
@ProgramDescription(
   value="Utility for manual testing of automatic Transcriber modules",
   arguments="name.of.transcriber.class.or.jar audio.wav [audio.wav ...]")
public class Transcribe extends CommandLineProgram {
   
   public static void main(String argv[]) {
      Transcribe application = new Transcribe();
      if (application.processArguments(argv)) {
         application.start();
      }
   }
   
   /**
    * Print verbose output to stderr.
    * @see #getVerbose()
    * @see #setVerbose(boolean)
    */
   protected boolean verbose = false;
   /**
    * Getter for {@link #verbose}: Print verbose output to stderr
    * @return Print verbose output to stderr
    */
   public boolean getVerbose() { return verbose; }
   /**
    * Setter for {@link #verbose}: Print verbose output to stderr
    * @param newVerbose Print verbose output to stderr
    */
   @Switch("Whether to print debug tracing")
   public Transcribe setVerbose(boolean newVerbose) { verbose = newVerbose; return this; }
   
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
    * @see #setDescriptor(AnnotatorDescriptor)
    */
   protected AnnotatorDescriptor descriptor;
   /**
    * Getter for {@link #descriptor}: Descriptor for the transcriber.
    * @return Descriptor for the transcriber.
    */
   public AnnotatorDescriptor getDescriptor() { return descriptor; }
   /**
    * Setter for {@link #descriptor}: Descriptor for the transcriber.
    * @param newDescriptor Descriptor for the transcriber.
    */
   public Transcribe setDescriptor(AnnotatorDescriptor newDescriptor) { descriptor = newDescriptor; return this; }

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
   public Transcribe setTranscriber(Transcriber newTranscriber) { transcriber = newTranscriber; return this; }

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
   
   /** Constructor */
   public Transcribe() {
   }
   
   public void start() {
      Iterator<String> args = arguments.iterator();
      if (!args.hasNext()) {
         throw new ClassCastException(
            "No transcriber .jar file or class name provided.\nTry --usage.");
      }
      
      transcriberName = args.next();
      
      // is the name a jar file name or a class name
      try { // try as a jar file
         descriptor = new AnnotatorDescriptor(new File(transcriberName));
      } catch (Throwable notAJarName) { // try as a class name
         try {
            descriptor = new AnnotatorDescriptor(transcriberName, getClass().getClassLoader());
         } catch(Throwable exception) {
            System.err.println("Could not get transcriber: " + transcriberName);
         }
      }
      Annotator annotator = descriptor.getInstance();
      if (!(annotator instanceof Transcriber)) {
        System.err.println("Annotator: " + transcriberName + " is not a transcriber");
        return;
      }
      transcriber = (Transcriber)annotator;
      
      // give the transcriber the resources it needs...
      
      transcriber.setSchema( // TODO make this configurable?
         new Schema(
            "who", "turn", "utterance", "word",
            new Layer("who", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
            .setPeers(true).setPeersOverlap(true).setSaturated(true),
            new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
            .setPeers(true).setPeersOverlap(false).setSaturated(false)
            .setParentId("who").setParentIncludes(true),
            new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
            .setPeers(true).setPeersOverlap(false).setSaturated(true)
            .setParentId("turn").setParentIncludes(true),
            new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
            .setPeers(true).setPeersOverlap(false).setSaturated(false)
            .setParentId("turn").setParentIncludes(true)));
      
      File transcriberDir = new File(workingDir, transcriber.getAnnotatorId());
      if (!transcriberDir.exists()) transcriberDir.mkdir();
      transcriber.setWorkingDirectory(transcriberDir);      
      // TODO transcriber.getDiarizationRequired()

      if (verbose) transcriber.getStatusObservers().add(s->System.err.println(s));

      if (!args.hasNext()) {
         throw new NullPointerException(
            "No audio to transcribe.\nTry --usage.");
      }
      JSONSerialization serializer = new JSONSerialization();
      serializer.configure(
         serializer.configure(
            new ParameterSet(),
            transcriber.getSchema()),
         transcriber.getSchema());
      while (args.hasNext()) {
         final File audio = new File(args.next());
         if (verbose) System.err.println("Audio: " + audio.getPath());
         final Graph transcript = new Graph();
         transcript.setId(IO.WithoutExtension(audio));
         transcript.setSchema((Schema)transcriber.getSchema().clone());
         if (verbose) System.err.println("Transcript ID: " + transcript.getId());
         try {
            
            // transcribe the audio
            transcriber.transcribe(audio, transcript);

            // print the resulting annotation graph to stdout
            serializer.serialize(
               Utility.OneGraphSpliterator(transcript), null,
               stream -> {
                  if (verbose) System.err.println(stream.getName());
                  try {
                     IO.Pump(stream.getStream(), System.out, false);
                  } catch(IOException exception) {
                     System.err.println(
                        "Error saving " + transcript.getId() + ": " + exception);
                     exception.printStackTrace(System.err);
                  }
               },
               warning -> {
                  System.out.println("Error serializing " + transcript.getId() + ": " + warning);
               },
               exception -> {
                  System.err.println(
                     "Error serializing " + transcript.getId() + ": " + exception);
                  exception.printStackTrace(System.err);
               });
               
         } catch(Exception exception) {
            System.err.println("Error transcribing " + audio.getName() + ": " + exception);
            exception.printStackTrace(System.err);
         }
      }
      
   } // end of start()
}
