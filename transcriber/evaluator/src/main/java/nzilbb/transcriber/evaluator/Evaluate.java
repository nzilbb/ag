//
// Copyright 2022 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.transcriber.evaluator;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import nzilbb.ag.Annotation;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.Transcriber;
import nzilbb.ag.automation.util.AnnotatorDescriptor;
import nzilbb.ag.serialize.GraphDeserializer;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.annotator.orthography.OrthographyStandardizer;
import nzilbb.configure.ParameterSet;
import nzilbb.editpath.*;
import nzilbb.formatter.text.PlainTextSerialization;
import nzilbb.util.CommandLineProgram;
import nzilbb.util.IO;
import nzilbb.util.ProgramDescription;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * Command line utility for automated evaluation of automatic Transcriber modules.
 * <p> This can be invoked from the command line something like this:
 * <br> <tt>java -jar nzilbb.transcriber.evaluate.jar nzilbb.transcriber.whisper.jar /path/to/recordings/and/transcripts </tt>
 */
@ProgramDescription(value="Utility for automated evaluation of automatic Transcriber modules",
                    arguments="nzilbb.transcriber.mytranscriber.jar /path/to/wav/and/txt/files")
public class Evaluate extends CommandLineProgram {
   
  public static void main(String argv[]) {
    Evaluate application = new Evaluate();
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
  public Evaluate setVerbose(boolean newVerbose) { verbose = newVerbose; return this; }   

  /**
   * Parameter string for configuring OrthographyStandardizer.
   * @see #getOrthographyParameters()
   * @see #setOrthographyParameters(String)
   */
  protected String orthographyParameters;
  /**
   * Getter for {@link #orthographyParameters}: Parameter string for configuring
   * OrthographyStandardizer. 
   * @return Parameter string for configuring OrthographyStandardizer.
   */
  public String getOrthographyParameters() { return orthographyParameters; }
  /**
   * Setter for {@link #orthographyParameters}: Parameter string for configuring
   * OrthographyStandardizer. 
   * @param newOrthographyParameters Parameter string for configuring OrthographyStandardizer.
   */
  @Switch("Configuration for standardization, e.g. removalPattern=[\\p{Punct}&&[^~\\-:']]")
  public Evaluate setOrthographyParameters(String newOrthographyParameters) {
    orthographyParameters = newOrthographyParameters;
    if (orthographyParameters != null && orthographyParameters.trim().length() > 0) {
      // ensure layers are configured correctly
      orthographyParameters += "&tokenLayerId=word&orthographyLayerId=orthography";
    }
    return this;
  }

  /**
   * The name of a .jar file which implements the transcriber.
   * @see #getTranscriberJar()
   * @see #setTranscriberJar(String)
   */
  protected String transcriberJar;
  /**
   * Getter for {@link #transcriberJar}: The name of a .jar file which implements the transcriber.
   * @return The name of a .jar file which implements the transcriber.
   */
  public String getTranscriberJar() { return transcriberJar; }
  /**
   * Setter for {@link #transcriberJar}: The name of a .jar file which implements the transcriber.
   * @param newTranscriberJar The name of a .jar file which implements the transcriber.
   */
  public Evaluate setTranscriberJar(String newTranscriberJar) { transcriberJar = newTranscriberJar; return this; }
  
  /**
   * The Transcriber module to evaluate.
   * @see #getTranscriber()
   * @see #setTranscriber(Transcriber)
   */
  protected Transcriber transcriber;
  /**
   * Getter for {@link #transcriber}: The Transcriber module to evaluate.
   * @return The Transcriber module to evaluate.
   */
  public Transcriber getTranscriber() { return transcriber; }
  /**
   * Setter for {@link #transcriber}: The Transcriber module to evaluate.
   * @param newTranscriber The Transcriber module to evaluate.
   */
  public Evaluate setTranscriber(Transcriber newTranscriber) { transcriber = newTranscriber; return this; }
  
  /**
   * Path to a directory containing input audio files and corresponding plain text transcripts.
   * @see #getFiles()
   * @see #setFiles(String)
   */
  protected String files;
  /**
   * Getter for {@link #files}: Path to a directory containing input audio files and
   * corresponding plain text transcripts. 
   * @return Path to a directory containing input audio files and corresponding plain text
   * transcripts. 
   */
  public String getFiles() { return files; }
  /**
   * Setter for {@link #files}: Path to a directory containing input audio files and
   * corresponding plain text transcripts. 
   * @param newFiles Path to a directory containing input audio files and corresponding
   * plain text transcripts. 
   */
  public Evaluate setFiles(String newFiles) { files = newFiles; return this; }

  /**
   * Descriptor for the transcriber.
   * @see #getDescriptor()
   * @see #setDescriptor(TranscriberDescriptor)
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
  public Evaluate setDescriptor(AnnotatorDescriptor newDescriptor) { descriptor = newDescriptor; return this; }
  
  /**
   * The orthography standardizer.
   * @see #getStandardizer()
   * @see #setStandardizer(OrthographyStandardizer)
   */
  protected OrthographyStandardizer standardizer;
  /**
   * Getter for {@link #standardizer}: The orthography standardizer.
   * @return The orthography standardizer.
   */
  public OrthographyStandardizer getStandardizer() { return standardizer; }
  /**
   * Setter for {@link #standardizer}: The orthography standardizer.
   * @param newStandardizer The orthography standardizer.
   */
  public Evaluate setStandardizer(OrthographyStandardizer newStandardizer) { standardizer = newStandardizer; return this; }

  /**
   * Deserializer for parsing reference transcripts.
   * @see #getDeserializer()
   * @see #setDeserializer(GraphDeserializer)
   */
  protected GraphDeserializer serializer;
  /**
   * Getter for {@link #serializer}: Deserializer for parsing reference transcripts.
   * @return Deserializer for parsing reference transcripts.
   */
  public GraphDeserializer getDeserializer() { return serializer; }
  /**
   * Setter for {@link #serializer}: Deserializer for parsing reference transcripts.
   * @param newDeserializer Deserializer for parsing reference transcripts.
   */
  public Evaluate setDeserializer(GraphDeserializer newDeserializer) { serializer = newDeserializer; return this; }
  
  /**
   * Printer for edit path data.
   * @see #getCsv()
   * @see #setCsv(CSVPrinter)
   */
  protected CSVPrinter csv;
  /**
   * Getter for {@link #csv}: Printer for edit path data.
   * @return Printer for edit path data.
   */
  public CSVPrinter getCsv() { return csv; }
  /**
   * Setter for {@link #csv}: Printer for edit path data.
   * @param newCsv Printer for edit path data.
   */
  public Evaluate setCsv(CSVPrinter newCsv) { csv = newCsv; return this; }

  /** Constructor */
  public Evaluate() {
  }
   
  public void start() {
    if (arguments.size() == 0) {
      System.err.println("No transcriber .jar file specified.");
      return;
    }
    if (arguments.size() == 1) {
      System.err.println("No directory for wav/txt files specified.");
      return;
    }
    setTranscriberJar(arguments.elementAt(0));
    setFiles(arguments.elementAt(1));
    
    // is the name a jar file name or a class name
    try { // try as a jar file
      descriptor = new AnnotatorDescriptor(new File(transcriberJar));
    } catch (Throwable notAJarName) { // try as a class name      
      System.err.println("Not the name of a transcriber .jar file: " + transcriberJar);
      System.err.println(notAJarName.toString());
      return;
    }
    Annotator annotator = descriptor.getInstance();
    if (!(annotator instanceof Transcriber)) {
      System.err.println("Annotator: " + transcriberJar + " is not a transcriber");
      return;
    }
    setTranscriber((Transcriber)annotator);
      
    // give the transcriber the resources it needs...      
    transcriber.setSchema(
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
        .setParentId("turn").setParentIncludes(true),
        new Layer("orthography", "Orthography").setAlignment(Constants.ALIGNMENT_NONE)
        .setPeers(false).setPeersOverlap(false).setSaturated(true)
        .setParentId("word").setParentIncludes(true)));      
    // TODO transcriber.getDiarizationRequired()

    if (verbose) transcriber.getStatusObservers().add(s->System.err.println(s));

    File dir = new File(files);
    if (!dir.exists()) {
      System.err.println("Input files directory doesn't exist: " + files);
      return;
    }
    if (!dir.isDirectory()) {
      System.err.println("Input files directory isn't a directory: " + files);
      return;
    }

    File[] wavs = dir.listFiles(new FileFilter() {
        public boolean accept(File f) {
          if (!f.getName().toLowerCase().endsWith(".wav")) return false;
          File txt = new File(f.getParentFile(), IO.WithoutExtension(f) + ".txt");
          return txt.exists();
        }});
    if (wavs.length == 0) {
      System.err.println("Now .wav files with .txt transcripts found: " + files);
      return;
    }

    // standardize orthography
    setStandardizer(new OrthographyStandardizer());
    standardizer.setSchema(transcriber.getSchema());
    try {
      standardizer.setTaskParameters(orthographyParameters);
    } catch(InvalidConfigurationException x) {
      System.err.println("Invalid orthography parameters \""+orthographyParameters+"\": " + x);
      return;
    }
    
    // serializer for parsing reference transcripts
    setDeserializer(new PlainTextSerialization());
    ParameterSet configuration = serializer.configure(
      new ParameterSet(), transcriber.getSchema());

    CSVPrinter out = null; // stdout will be tab-separated values too
    File csvFile = new File(dir, "paths.tsv");
    try {
      setCsv(new CSVPrinter(
               new OutputStreamWriter(new FileOutputStream(csvFile), "UTF-8"),
               CSVFormat.EXCEL.withDelimiter('\t')));
      out = new CSVPrinter(
        new OutputStreamWriter(System.out, "UTF-8"),
        CSVFormat.EXCEL.withDelimiter('\t'));
    } catch (Exception x) {
      System.err.println("Could not write to \""+csvFile.getPath()+"\": " + x);
      return;
    }
    try {
      try {
        // add CSV headers
        csv.print("step");
        csv.print("txt");
        csv.print("rawReference");
        csv.print("reference");
        csv.print("wav");
        csv.print("rawWord");
        csv.print("word");
        csv.print("operation");
        csv.print("distance");
        csv.println();

        out.print("wav");
        out.print("WER");
        out.println();
      } catch (Exception x) {
        System.err.println("Could not write headers to \""+csvFile.getPath()+"\": " + x);
        return;
      }

      // transcribe recordings
      for (File wav : wavs) {
        File txt = new File(wav.getParentFile(), IO.WithoutExtension(wav) + ".txt");
        try {
          out.print(wav.getName());
          out.flush();
          double WER = evaluate(wav, txt);
          out.print(WER);
          out.println();
          out.flush();
        } catch(Exception exception) {
          System.err.println();
          System.err.println("Error transcribing " + wav.getName() + ": " + exception);
          exception.printStackTrace(System.err);
        }
      } // next av
    } finally {
      try {
        csv.close();
      } catch (Exception x) {
      }
    }
  } // end of start()
  
  /**
   * Evaluate a single audio/reference-transcript pair.
   * @param wav The recording of speech.
   * @param txt The reference transcript.
   * @return The word error rate (WER) of the pair.
   * @throws Exception
   */
  public double evaluate(File wav, File txt) throws Exception {
    
    Graph transcript = new Graph();
    transcript.setId(IO.WithoutExtension(wav));
    transcript.setSchema((Schema)transcriber.getSchema().clone());
    
    // transcribe the audio
    transcriber.transcribe(wav, transcript);
    // standarize orthography
    standardizer.transform(transcript);
    
    // load reference transcript
    NamedStream[] streams = { new NamedStream(txt) };
    // use default parameters for loading this file
    serializer.setParameters(
      serializer.load(streams, transcriber.getSchema()));
    // deserialize
    Graph[] graphs = serializer.deserialize();
    Graph reference = graphs[0];
    // ensure orthorgraphy layer is present
    reference.setSchema((Schema)transcriber.getSchema().clone());
    // standarize orthography
    standardizer.transform(reference);

    // get minimum edit path
    MinimumEditPath<Annotation> mapper = new MinimumEditPath<Annotation>(
      new DefaultEditComparator<Annotation>(new EqualsComparator<Annotation>() {
          public int compare(Annotation a1, Annotation a2) {
            return a1.getLabel().compareTo(a2.getLabel());
          }
        }));
    List<EditStep<Annotation>> path = mapper.minimumEditPath(
      Arrays.asList(reference.all("orthography")),
      Arrays.asList(transcript.all("orthography")));
    // collapse subsequent delete/create steps into a single change step
    path = mapper.collapse(path);

    if (verbose) { // output edit path
      System.err.println("\t"+txt.getName() + "\t\t" + wav.getName());
      System.err.println(path.stream().map(s->s.toString()).collect(Collectors.joining("\n")));
    }

    // write the path to the csv file
    int i = 1;
    // for calculating WER:
    int C = 0; // number of correct words
    int S = 0; // number of substitutions
    int D = 0; // number of deletions 
    int I = 0; // number of insertions
    for (EditStep<Annotation> step : path) {
      csv.print(""+(++i));
      csv.print(txt.getName());
      if (step.getFrom() == null) {
        csv.print("");
        csv.print("");
      } else {
        csv.print(step.getFrom().getParent().getLabel());
        csv.print(step.getFrom().getLabel());
      }
      csv.print(wav.getName());
      if (step.getTo() == null) {
        csv.print("");
        csv.print("");
      } else {
        csv.print(step.getTo().getParent().getLabel());
        csv.print(step.getTo().getLabel());
      }
      csv.print(operation(step.getOperation()));
      csv.print(step.getStepDistance());
      csv.println();
      switch(step.getOperation()) {
        case CHANGE: S++; break;
        case DELETE: D++; break;
        case INSERT: I++; break;
        default: C++; break;
      }
    }

    // return word error rate
    return ((double)(S+D+I))/((double)(S+D+C));
  } // end of evaluate()
  
  /**
   * Converts an EditStep#StepOperation into a character code for saving in the csv file.
   * @param operation
   * @return Code representing the operation.
   */
  protected String operation(EditStep.StepOperation operation) {
    switch(operation) {
      case DELETE: return "-";
      case CHANGE: return "!";
      case INSERT: return "+";
      default: return " ";
    }
  } // end of operation()

}
