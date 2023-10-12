//
// Copyright 2022-2023 New Zealand Institute of Language, Brain and Behaviour, 
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
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.stream.Collectors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import nzilbb.ag.Annotation;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.Transcriber;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.util.AnnotatorDescriptor;
import nzilbb.ag.serialize.GraphDeserializer;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.util.AnnotationComparatorByAnchor;
import nzilbb.annotator.orthography.OrthographyStandardizer;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.editpath.*;
import nzilbb.editpath.MinimumEditPathString;
import nzilbb.formatter.text.PlainTextSerialization;
import nzilbb.labbcat.LabbcatView;
import nzilbb.media.MediaCensor;
import nzilbb.media.MediaThread;
import nzilbb.media.ffmpeg.FfmpegCensor;
import nzilbb.util.CommandLineProgram;
import nzilbb.util.IO;
import nzilbb.util.ProgramDescription;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;
import nzilbb.util.Timers;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * A command line utility for automated evaluation of automatic Transcriber modules.
 * <p> This can be invoked from the command line something like this:
 * <br> <tt>java -jar nzilbb.transcriber.evaluate.jar nzilbb.transcriber.whisper.jar
 * /path/to/recordings/and/transcripts </tt>
 * <p>It can also compare automatic transcripts of transcribed recordings stored in a
 * LaBB-CAT instance, like this: 
 * <br> <tt> java -jar nzilbb.transcriber.evaluate.jar  nzilbb.transcriber.whisper.jar
 * --Labbcat=https://labbcat.canterbury.ac.nz/demo/
 * --transcripts=first('corpus').label == 'UC'</tt> 
 * <p> Either way, the utility produces two tab-separated outputs:
 * <ul>
 *  <li> To std out, a list of recordings with a word count and Word Error Rate (WER)</li>
 *  <li> To a `path....tsv` file, minimum edit paths for all words, including:
 *  <ul>
 *   <li> the word from the reference transcript </li>
 *   <li> the word from the automatic transcriber </li>
 *   <li> what the step does (insert +, delete -, change ~, or no change) </li>
 *   <li> the edit distance represented by the step</li>
 *  </ul></li>    
 *  <li> To a `utterance....tsv` file, utterance information including:
 *  <ul>
 *   <li> the start/end time of the utterance </li>
 *   <li> how many words it contains </li>
 *   <li> whether it's a reference or transcribed utterance ("ref" or "wav" respectively) </li>
 *  </ul></li>    
 * </ul>
 */
@ProgramDescription(value="Utility for automated evaluation of automatic Transcriber modules.\nOutputs:\n\tpaths-...tsv file with word edit paths\n\tutterances-...tsv file with utterance partitioning info.",
                    arguments="nzilbb.transcriber.mytranscriber.jar [/path/to/wav/and/txt/files]")
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
   * URL to LaBB-CAT instance from which transcripts/audio should be downloaded.
   * @see #getLabbcat()
   * @see #setLabbcat(String)
   */
  protected String labbcat;
  /**
   * Getter for {@link #labbcat}: URL to LaBB-CAT instance from which transcripts/audio
   * should be downloaded. 
   * @return URL to LaBB-CAT instance from which transcripts/audio should be downloaded.
   */
  public String getLabbcat() { return labbcat; }
  /**
   * Setter for {@link #labbcat}: URL to LaBB-CAT instance from which transcripts/audio
   * should be downloaded. 
   * @param newLabbcat URL to LaBB-CAT instance from which transcripts/audio should be downloaded.
   */
  @Switch("URL to LaBB-CAT instance to download data from")
  public Evaluate setLabbcat(String newLabbcat) { labbcat = newLabbcat; return this; }

  /**
   * LaBB-CAT username.
   * @see #getUsername()
   * @see #setUsername(String)
   * @see #getLabbcat()
   */
  protected String username;
  /**
   * Getter for {@link #username}: LaBB-CAT username.
   * @return LaBB-CAT username.
   */
  public String getUsername() { return username; }
  /**
   * Setter for {@link #username}: LaBB-CAT username.
   * @param newUsername LaBB-CAT username.
   */
  @Switch("LaBB-CAT username")
  public Evaluate setUsername(String newUsername) { username = newUsername; return this; }
  
  /**
   * LaBB-CAT password.
   * @see #getPassword()
   * @see #setPassword(String)
   * @see #getLabbcat()
   */
  protected String password;
  /**
   * Getter for {@link #password}: LaBB-CAT password.
   * @return LaBB-CAT password.
   */
  public String getPassword() { return password; }
  /**
   * Setter for {@link #password}: LaBB-CAT password.
   * @param newPassword LaBB-CAT password.
   */
  @Switch("LaBB-CAT password")
  public Evaluate setPassword(String newPassword) { password = newPassword; return this; }
  
  /**
   * Expression identifying LaBB-CAT transcripts to download.
   * e.g. "first('corpus').label = 'CC'"
   * @see #getTranscripts()
   * @see #setTranscripts(String)
   * @see #getLabbcat()
   * @see https://nzilbb.github.io/labbcat-java/apidocs/nzilbb/labbcat/LabbcatView.html#getMatchingTranscriptIds(java.lang.String,java.lang.Integer,java.lang.Integer,java.lang.String)
   */
  protected String transcripts;
  /**
   * Getter for {@link #transcripts}: Expression identifying LaBB-CAT transcripts to download.
   * @return Expression identifying LaBB-CAT transcripts to download.
   */
  public String getTranscripts() { return transcripts; }
  /**
   * Setter for {@link #transcripts}: Expression identifying LaBB-CAT transcripts to download.
   * e.g. "first('corpus').label = 'CC'"
   * @param newTranscripts Expression identifying LaBB-CAT transcripts to download.
   */
  @Switch("LaBB-CAT transcripts to download, e.g. first('corpus').label = 'CC'")
  public Evaluate setTranscripts(String newTranscripts) { transcripts = newTranscripts; return this; }
  
  /**
   * Layer ID for annotations to include as a column in the output edit paths.
   * @see #getTag()
   * @see #setTag(String)
   */
  protected String tag;
  /**
   * Getter for {@link #tag}: Layer ID for annotations to include as a column in the
   * output edit paths. 
   * @return Layer ID for annotations to include as a column in the output edit paths.
   */
  public String getTag() {
    if (tag == null || tag.length() == 0) return null;
    return tag;
  }
  /**
   * Setter for {@link #tag}: Layer ID for annotations to include as a column in the
   * output edit paths. 
   * @param newTag Layer ID for annotations to include as a column in the output edit paths.
   */
  @Switch("LaBB-CAT layer to as a column in the output paths, e.g. language")
  public Evaluate setTag(String newTag) { tag = newTag; return this; }

  /**
   * How many transcripts to download from LaBB-CAT at once. 0 means download all at
   * once. Default is 10.
   * <p> When downloading from LaBB-CAT, transcripts are processed in batches to moderate
   * computing resources such as storage, and ensure that if the process is interrupted by
   * networking issues, at least partial results will be available.
   * @see #getBatchSize()
   * @see #setBatchSize(int)
   */
  protected int batchSize = 10;
  /**
   * Getter for {@link # batchSize}: How many transcripts to download from LaBB-CAT at
   * once. 0 means download all at once.
   * @return How many transcripts to download from LaBB-CAT at once.
   */
   public int getBatchSize() { return batchSize; }
   /**
    * Setter for {@link # batchSize}: How many transcripts to download from LaBB-CAT at once.
    * @param new batchSize How many transcripts to download from LaBB-CAT at once. 0 means
    * download all at once.
    */
  @Switch("How many transcripts to download from LaBB-CAT at once. 0 = all at once, default is 10.")
   public Evaluate setBatchSize(int newBatchSize) { batchSize = newBatchSize; return this; }
  
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
   * Path to ffmpeg, for silencing non-transcribed portions of recordings.
   * @see #getFfmpeg()
   * @see #setFfmpeg(String)
   */
  protected String ffmpeg;
  /**
   * Getter for {@link #ffmpeg}: Path to ffmpeg, for silencing non-transcribed portions of
   * recordings. 
   * @return Path to ffmpeg, for silencing non-transcribed portions of recordings.
   */
  public String getFfmpeg() { return ffmpeg; }
  /**
   * Setter for {@link #ffmpeg}: Path to ffmpeg, for silencing non-transcribed portions of
   * recordings. 
   * @param newFfmpeg Path to ffmpeg, for silencing non-transcribed portions of recordings.
   */
  @Switch("Path to ffmpeg, for silencing non-transcribed portions of recordings, e.g. /usr/bin")
  public Evaluate setFfmpeg(String newFfmpeg) { ffmpeg = newFfmpeg; return this; }
  
  /**
   * Audio filter for ffmpeg to process untranscribed portions of the audio.
   * @see #getUntranscribedFilter()
   * @see #setUntranscribedFilter(String)
   */
  protected String untranscribedFilter = "lowpass=f=1";
  /**
   * Getter for {@link #untranscribedFilter}: Audio filter for ffmpeg to process
   * untranscribed portions of the audio. 
   * @return Audio filter for ffmpeg to process untranscribed portions of the audio.
   */
  public String getUntranscribedFilter() { return untranscribedFilter; }
  /**
   * Setter for {@link #untranscribedFilter}: Audio filter for ffmpeg to process
   * untranscribed portions of the audio. 
   * @param newUntranscribedFilter Audio filter for ffmpeg to process untranscribed
   * portions of the audio. 
   */
  @Switch("Audio filter for ffmpeg to process untranscribed portions of the audio, e.g. lowpass=f=1")
  public Evaluate setUntranscribedFilter(String newUntranscribedFilter) { untranscribedFilter = newUntranscribedFilter; return this; }
  
  /**
   * Transcriber installation configuration, if any is required.
   * @see #getConfig()
   * @see #setConfig(String)
   */
  protected String config;
  /**
   * Getter for {@link #config}: Transcriber installation configuration, if any is required.
   * @return Transcriber installation configuration, if any is required.
   */
  public String getConfig() { return config; }
  /**
   * Setter for {@link #config}: Transcriber installation configuration, if any is required.
   * @param newConfig Transcriber installation configuration, if any is required.
   */
  @Switch("Transcriber installation configuration, if it requires any.")
  public Evaluate setConfig(String newConfig) { config = newConfig; return this; }

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
  
  /**
   * Printer for utterance data.
   * @see #getUtteranceCsv()
   * @see #setUtteranceCsv(CSVPrinter)
   */
  protected CSVPrinter utteranceCsv;
  /**
   * Getter for {@link #utteranceCsv}: Printer for utterance data.
   * @return Printer for utterance data.
   */
  public CSVPrinter getUtteranceCsv() { return utteranceCsv; }
  /**
   * Setter for {@link #utteranceCsv}: Printer for utterance data.
   * @param newUtteranceCsv Printer for utterance data.
   */
  public Evaluate setUtteranceCsv(CSVPrinter newUtteranceCsv) { utteranceCsv = newUtteranceCsv; return this; }

  /** Constructor */
  public Evaluate() {
  }
   
  public void start() {
    if (arguments.size() == 0) {
      System.err.println("No transcriber .jar file specified.");
      return;
    }
    if (arguments.size() == 1 && labbcat == null) {
      System.err.println("No directory for wav/txt files specified, nor LaBB-CAT URL.");
      return;
    }
    setTranscriberJar(arguments.elementAt(0));

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
    if (verbose) {
      System.err.println(transcriberJar + " implements "
                         + transcriber.getAnnotatorId()
                         + " (" + transcriber.getVersion() + ")");
    }
      
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
    if (transcriber.getDiarizationRequired()) {
      System.err.println(
        transcriber.getAnnotatorId() + " requires diarization, which is not currently supported.");
      return;
    }
    
    if (verbose) transcriber.getStatusObservers().add(s->System.err.println(s));

    // setWorkingDirectory?
    if (transcriber.getClass().isAnnotationPresent(UsesFileSystem.class)) {
      File transcriberDir = new File(transcriber.getAnnotatorId());
      if (!transcriberDir.exists()) transcriberDir.mkdir();
      if (verbose) System.err.println("Working directory: " + transcriberDir.getPath());
      transcriber.setWorkingDirectory(transcriberDir);
    }

    // config?
    if (config != null && config.trim().length() > 0) {
      if (verbose) System.err.println("Configuring transcriber: " + config);
      try {
        transcriber.setConfig(config);
      } catch(InvalidConfigurationException exception) {
        System.err.println(
          "Configuring transcriber: \"" + config + "\" - ERROR: " + exception.getMessage());
        return;
      }
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
    
    if (arguments.size() >= 2) {
      setFiles(arguments.elementAt(1));
      evaluateFromFileSystem();
    } else {
      evaluateFromLabbcat();
    }
  }
  
  /**
   * Evaluate transcription of recordings in the local directory specified by {@link #files}.
   */
  protected void evaluateFromFileSystem() {
    
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
      System.err.println("No .wav files with .txt transcripts found: " + files);
      return;
    }

    // serializer for parsing reference transcripts
    setDeserializer(new PlainTextSerialization());
    ParameterSet configuration = serializer.configure(
      new ParameterSet(), transcriber.getSchema());

    CSVPrinter out = null; // stdout will be tab-separated values
    // csv for word edit paths
    File csvFile = new File(dir, "paths-"+transcriber.getAnnotatorId()+".tsv");
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
    
    // csv for utterance data
    File utteranceCsvFile = new File(dir, "utterances-"+transcriber.getAnnotatorId()+".tsv");
    try {
      setUtteranceCsv(new CSVPrinter(
                        new OutputStreamWriter(new FileOutputStream(utteranceCsvFile), "UTF-8"),
                        CSVFormat.EXCEL.withDelimiter('\t')));
    } catch (Exception x) {
      System.err.println("Could not write to \""+utteranceCsvFile.getPath()+"\": " + x);
      return;
    }
    Timers timer = new Timers();
    final CSVPrinter finalOut = out;
    try {
      try {
        // add CSV headers
        csv.print("step");
        csv.print("txt");
        // (tag is not valid if evaluating from filesystem)
        tag = null;
        csv.print("rawReference");
        csv.print("reference");
        csv.print("operation");
        csv.print("word");
        csv.print("rawWord");
        csv.print("wav");
        csv.print("distance");
        csv.println();

        utteranceCsv.print("utt");
        utteranceCsv.print("txt");
        utteranceCsv.print("version"); // ref or wav
        utteranceCsv.print("start");
        utteranceCsv.print("end");
        utteranceCsv.print("wordCount");
        utteranceCsv.println();

        out.print("wav");
        out.print("duration");
        out.print("wordCount");
        out.print("WER");
        out.println();
      } catch (Exception x) {
        System.err.println("Could not write headers to \""+csvFile.getPath()+"\": " + x);
        return;
      }

      // transcribe recordings batch
      timer.start("Time to transcribe");
      transcriber.transcribeFragments(Arrays.stream(wavs), transcribed -> { 
          timer.end("Time to transcribe"); // stop timer while we evaluate...
          try {
            // ensure the schema has everything we expect
            transcribed.setSchema(transcriber.getSchema());
            // standarize orthography
            standardizer.transform(transcribed);
            File wav = new File(dir, IO.WithoutExtension(transcribed.getId()) + ".wav");
            File txt = new File(wav.getParentFile(), IO.WithoutExtension(wav) + ".txt");
            finalOut.print(wav.getName());
            finalOut.print(duration(wav));
            finalOut.flush();

            // load reference transcript
            NamedStream[] streams = { new NamedStream(txt) };
            // use default parameters for loading this file
            serializer.setParameters(
              serializer.load(streams, transcriber.getSchema()));
            // deserialize
            Graph[] graphs = serializer.deserialize();
            Graph reference = graphs[0];
            finalOut.print(reference.all("word").length);
            
            if (ffmpeg != null) {
              // cut out bits of the wav file that weren't transcribed, so we don't get
              // long periods of INSERT only from the transcriber.
              wav = silenceUntranscribedAudio(wav, reference);
              wav.deleteOnExit();
            }
            try {
              double WER = evaluate(wav, reference, transcribed);
              finalOut.print(WER);
              finalOut.println();
              finalOut.flush();
            } finally {
              if (ffmpeg != null) { // wav is an edited copy, so delete it
                wav.delete();
              }
            }
          } catch(Exception exception) {
            System.err.println();
            System.err.println("Error transcribing " + transcribed.getId() + ": " + exception);
            exception.printStackTrace(System.err);
          } finally {
            timer.start("Time to transcribe"); // start up timer again
          }
        });
      timer.end("Time to transcribe");
    } catch(Exception exception) {
      System.err.println();
      System.err.println("Error: " + exception);
      exception.printStackTrace(System.err);
    } finally {
      try {
        csv.close();
      } catch (Exception x) {
      }
      try {
        utteranceCsv.close();
      } catch (Exception x) {
      }
      System.err.println(
        "Time to transcribe: " + (((double)timer.getTotals().get("Time to transcribe")) / 1000.0));
    }
  } // end of evaluateFromFileSystem()
  
  /**
   * Evaluate transcription of recordings in the remote corpus specified by {@link #labbcat}.
   */
  public void evaluateFromLabbcat() {
    if (transcripts == null) {
      System.err.println("No 'transcripts' matching expression specified.");
      System.err.println("You must specify an expression to match transcripts from LaBB-CAT");
      System.err.println("e.g. \"transcripts=first('corpus').label = 'CC'\"");
      System.err.println("e.g. \"transcripts=labels('participant').includes('mop03-2b')\"");
      System.err.println("For more information, see:");
      System.err.println("https://nzilbb.github.io/labbcat-java/apidocs/nzilbb/labbcat/LabbcatView.html#getMatchingTranscriptIds(java.lang.String,java.lang.Integer,java.lang.Integer,java.lang.String)");
      return;
    }
    CSVPrinter out = null; // stdout will be tab-separated values too
    // csv for word edit paths
    File csvFile = new File(
      "paths-"
      +labbcat.replaceFirst("https?://","").replaceAll("\\W+","-")+"-"
      +transcriber.getAnnotatorId()+".tsv");
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
    // csv for utterance data
    File utteranceCsvFile = new File(
      "utterances-"
      +labbcat.replaceFirst("https?://","").replaceAll("\\W+","-")+"-"
      +transcriber.getAnnotatorId()+".tsv");
    try {
      setUtteranceCsv(new CSVPrinter(
                        new OutputStreamWriter(new FileOutputStream(utteranceCsvFile), "UTF-8"),
                        CSVFormat.EXCEL.withDelimiter('\t')));
    } catch (Exception x) {
      System.err.println("Could not write to \""+utteranceCsvFile.getPath()+"\": " + x);
      return;
    }
    final CSVPrinter finalOut = out;
    File dir = null;
    Timers timer = new Timers();
    try {
      try {
        // add CSV headers
        csv.print("step");
        csv.print("txt");
        if (getTag() != null) csv.print("tag");
        csv.print("rawReference");
        csv.print("reference");
        csv.print("operation");
        csv.print("word");
        csv.print("rawWord");
        csv.print("wav");
        csv.print("distance");
        csv.println();
        csv.flush();

        utteranceCsv.print("utt");
        utteranceCsv.print("txt");
        utteranceCsv.print("version"); // ref or wav
        utteranceCsv.print("start");
        utteranceCsv.print("end");
        utteranceCsv.print("wordCount");
        utteranceCsv.println();
        utteranceCsv.flush();

        out.print("wav");
        out.print("duration");
        out.print("wordCount");
        out.print("WER");
        out.println();
        out.flush();
      } catch (Exception x) {
        System.err.println("Could not write headers to \""+csvFile.getPath()+"\": " + x);
        return;
      }

      dir = Files.createTempDirectory("Evaluate").toFile();
      if (verbose) System.err.println("Downloading media to: " + dir.getPath());
      String[] layers = { "turn", "utterance", "word" };
      if (getTag() != null) {
        String[] layersWithTag = { "turn", "utterance", "word", getTag() };
        layers = layersWithTag;
      }

      // connect to LaBB-CAT
      LabbcatView corpus = new LabbcatView(labbcat, username, password);
      int transcriptCount = corpus.countMatchingTranscriptIds​(transcripts);
      if (verbose) System.err.println("Transcript count: " + transcriptCount);
      // get a list of transcript IDs
      Integer pageLength = null;
      int pageNumber = 0;
      if (batchSize > 0) pageLength = batchSize;
      String[] ids = corpus.getMatchingTranscriptIds​(transcripts, pageLength, pageNumber, null);
      int soFar = 0;
      while (ids.length > 0) { // for all the pages of results
        if (verbose) System.err.println("Batch " + pageNumber + "...");

        final HashMap<String,File> idToWav = new HashMap<String,File>();
        final HashMap<String,Graph> idToReference = new HashMap<String,Graph>();

        try {
          // for each transcript
          for (String id : ids) {
            try {
              
              // get speech recording
              File wav = corpus.getMediaFile​(id, "", "audio/wav", dir);
              if (wav == null || !wav.exists()) {
                System.err.println("No recording for " + id);
                continue;
              }
              wav.deleteOnExit();
              
              // get reference transcript
              Graph reference = corpus.getTranscript(id, layers);
              if (verbose) System.err.println(reference.getId() + "\t" + wav.getName());

              if (ffmpeg != null) {
                // cut out bits of the wav file that weren't transcribed, so we don't get
                // long periods of INSERT only from the transcriber.
                File newWav = silenceUntranscribedAudio(wav, reference);
                wav.delete();
                if (!newWav.renameTo(wav)) {
                  throw new Exception(
                    "Could not rename " + newWav.getPath() + " as " + wav.getPath());
                }
              }
              
              idToWav.put(IO.WithoutExtension(wav), wav);
              idToReference.put(IO.WithoutExtension(wav), reference);
            } catch(Exception exception) {
              System.err.println();
              System.err.println("Error transcribing " + id + ": " + exception);
              exception.printStackTrace(System.err);
            }
          } // next id
          
          // transcribe recordings batch
          timer.start("Time to transcribe"); // start timer
          transcriber.transcribeFragments(idToWav.values().stream(), transcribed -> { 
              timer.end("Time to transcribe"); // stop timer while we evaluate
              try {
                // ensure the schema has everything we expect
                transcribed.setSchema(transcriber.getSchema());
                // standarize orthography
                standardizer.transform(transcribed);
                
                File wav = idToWav.get(IO.WithoutExtension(transcribed.getId()));
                finalOut.print(wav.getName());
                finalOut.print(duration(wav));
                Graph reference = idToReference.get(IO.WithoutExtension(transcribed.getId()));
                finalOut.print(reference.all("word").length);
                
                // compare reference with transcription
                double WER = evaluate(wav, reference, transcribed);
                finalOut.print(WER);
                finalOut.println();
                finalOut.flush();
              } catch(Exception exception) {
                System.err.println();
                System.err.println("Error transcribing " + transcribed.getId() + ": " + exception);
                exception.printStackTrace(System.err);
              } finally {
                timer.start("Time to transcribe"); // start up timer again
              }
            }); 
          timer.end("Time to transcribe"); // stop timer while we get next batch
          soFar += ids.length;
          if (verbose) System.err.println("Transcribed " + soFar + " of " + transcriptCount);
        } finally {
          // delete downloaded files
          for (File wav : idToWav.values()) wav.delete();
        }
        
        // fetch next page
        ids = corpus.getMatchingTranscriptIds​(transcripts, pageLength, ++pageNumber, null);
      } // next page
      
    } catch (Exception x) {
      System.err.println();
      System.err.println("Error: " + x);
      x.printStackTrace(System.err);
    } finally {
      if (dir != null) dir.delete();
      try {
        csv.close();
      } catch (Exception x) {
      }
      try {
        utteranceCsv.close();
      } catch (Exception x) {
      }
    }
    System.err.println(
      "Time to transcribe: " + (((double)timer.getTotals().get("Time to transcribe")) / 1000.0));
  } // end of evaluateFromLabbcat()
  
  /**
   * Determines the duration of a wav file.
   * @param wav
   * @return Duration of the given wav file in seconds (to nearest ms).
   */
  public double duration(File wav) {
    if (wav != null && wav.exists()) {
      try {
        // determine the duration of the media file
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(wav);
        AudioFormat format = audioInputStream.getFormat();
        long frames = audioInputStream.getFrameLength();
        if (frames > 0) {
          double dur = ((double)frames) / format.getFrameRate();
          // round to nearest ms
          return (((int)(dur * 1000)+500))/1000.0;
        }
      } catch(Exception exception) {
        System.err.println("Could not get duration of " + wav.getName() + " ERROR: " + exception);
      }
    }
    return 0.0;
  } // end of duration()
  
  /**
   * Silences portions of the given audio file that are not transcribed, so that if the
   * contain speech, the Transcriber will not transcribe it.
   * <p> This prevents long runs of INSERTs in the path skewing the final WER.
   * @param wav The recording to edit.
   * @param transcript The reference transcript, which must have utterance annotations, which
   * are used to discern which parts of the audio are transcribed.
   * @return The edited version of the recording, which will be a different file
   * from <var>wav</var>.
   */
  public File silenceUntranscribedAudio(File wav, Graph transcript) throws Exception {
    if (verbose) {
      System.err.println("Silencing untranscribed portions of " + transcript);
    }
    // find all transcribed portions of the recording, by annotating all spans
    // during which there's at least one non-empty utterance
    transcript.getSchema().addLayer(
      new Layer("transcribed","transcribed")
      .setParentId(transcript.getSchema().getRoot().getId()).setSaturated(false)
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false));
    transcript.assignWordsToUtterances();
    SortedSet<Annotation> utterancesByAnchor
      = new TreeSet<Annotation>(new AnnotationComparatorByAnchor());
    for (Annotation utt : transcript.all(transcript.getSchema().getUtteranceLayerId())) {
      utterancesByAnchor.add(utt);
    }
    Annotation currentSpan = null;
    for (Annotation utt : Arrays.stream(
           transcript.all(transcript.getSchema().getUtteranceLayerId()))
           // anchors are set
           .filter(utt -> utt.getAnchored())
           // contains some words
           .filter(utt -> utt.containsKey("@words"))
           .filter(utt -> ((List<Annotation>)utt.get("@words")).size() > 0)
           // in anchor order
           .sorted(new AnnotationComparatorByAnchor())
           .collect(Collectors.toList())) {      
      if (currentSpan == null // the first one, or the current span ends before the utterance starts
          || currentSpan.getEnd().getOffset() < utt.getStart().getOffset()) {
        // start a new span
        currentSpan = transcript.createSpan(utt, utt, "transcribed", "transcribed", transcript);
      } else if (currentSpan.getEnd().getOffset() < utt.getEnd().getOffset()) {
        // current span ends before the utterance ends, so extend the span
        currentSpan.setEnd(utt.getEnd());
      }
    } // next utterance, by anchor
    if (currentSpan == null) {
      throw new Exception("There are no transcribed portions in " + transcript.getId());
    }

    // silence all the portions of the recording that aren't in a transcribed span
    Vector<Double> boundaries = new Vector<Double>();
    if (transcript.first("transcribed").getStart().getOffset() > 0.0) {
      // first silence starts at zero
      boundaries.add(0.0);
    }
    for (Annotation span : transcript.all("transcribed")) {
      boundaries.add(span.getStart().getOffset());
      boundaries.add(span.getEnd().getOffset());
    }
    ParameterSet config = new ParameterSet();
    config.addParameter(new Parameter("ffmpegPath", new File(ffmpeg)));
    config.addParameter(new Parameter("audioFilter", untranscribedFilter));
    config.addParameter(new Parameter("deleteSource", Boolean.FALSE));
    MediaCensor censor = new FfmpegCensor();
    censor.configure(config);
    
    // run the censor
    if (verbose) {
      System.err.println("Silencing intervals " + boundaries);
    }
    File silencedWav = File.createTempFile(wav.getName()+"-", ".wav");
    MediaThread thread
      = censor.start("audio/wav", wav, boundaries, silencedWav);
    thread.join();
    if (!silencedWav.exists()) {
      throw new Exception("Silencing file failed - ffmpeg output doesn't exist");
    }
    return silencedWav;
  } // end of silenceUntranscribedAudio()

  /**
   * Evaluate a single audio/reference-transcript pair.
   * @param wav The recording of speech.
   * @param txt The reference transcript.
   * @param transcribed The new transcript.
   * @return The word error rate (WER) of the pair.
   * @throws Exception
   */
  public double evaluate(File wav, Graph reference, Graph transcribed) throws Exception {
      
    // ensure orthography layer is present
    reference.getSchema().addLayer(
      (Layer)transcriber.getSchema().getLayer("orthography").clone());
    // standarize orthography
    standardizer.transform(reference);
    
    // get minimum edit path
    MinimumEditPath<Annotation> mapper = new MinimumEditPath<Annotation>(
      new DefaultEditComparator<Annotation>() {
        // compare each label using minimum edit path, using character error rate as
        // distance, to ensure similar labels are more likely to be matched than
        // dissimilar labels        
        MinimumEditPathString pathFinder = new MinimumEditPathString();
        public EditStep<Annotation> compare​(Annotation from, Annotation to) {
          List<EditStep<Character>> path = pathFinder.minimumEditPath(
            from.getLabel(), to.getLabel());          
          EditStep<Annotation> step = new EditStep<Annotation>(
            from, to, pathFinder.errorRate(path), EditStep.StepOperation.NONE);
          if (step.getStepDistance() > 0) {
            step.setOperation(EditStep.StepOperation.CHANGE);
          }
          return step;
        }});
    List<EditStep<Annotation>> path = mapper.minimumEditPath(
      Arrays.asList(reference.all("orthography")),
      Arrays.asList(transcribed.all("orthography")));

    if (verbose) { // output edit path
      System.err.println("\t"+reference.getId() + "\t\t" + wav.getName());
      System.err.println(path.stream().map(s->s.toString()).collect(Collectors.joining("\n")));
    }

    // write the path to the csv file
    int i = 0;
    // for calculating WER:
    int C = 0; // number of correct words
    int S = 0; // number of substitutions
    int D = 0; // number of deletions 
    int I = 0; // number of insertions
    for (EditStep<Annotation> step : path) {
      csv.print(""+(++i));
      csv.print(reference.getId());
      if (step.getFrom() == null) {
        if (getTag() != null) csv.print("");
        csv.print("");
        csv.print("");
      } else {
        if (getTag() != null) {
          Annotation tag = step.getFrom().first(getTag());
          csv.print(tag == null?"":tag.getLabel());
        }
        csv.print(step.getFrom().getParent().getLabel());
        csv.print(step.getFrom().getLabel());
      }
      csv.print(operation(step.getOperation()));
      if (step.getTo() == null) {
        csv.print("");
        csv.print("");
      } else {
        csv.print(step.getTo().getLabel());
        csv.print(step.getTo().getParent().getLabel());
      }
      csv.print(wav.getName());
      csv.print(step.getStepDistance());
      csv.println();
      switch(step.getOperation()) {
        case CHANGE: S++; break;
        case DELETE: D++; break;
        case INSERT: I++; break;
        default: C++; break;
      }
    }
    csv.flush();

    // write the utterance data
    int u = 1;
    for (Annotation utterance : reference.all("utterance")) {
      utteranceCsv.print(u++);
      utteranceCsv.print(reference.getId());
      utteranceCsv.print("ref"); // ref or wav
      utteranceCsv.print(utterance.getStart().getOffset());
      utteranceCsv.print(utterance.getEnd().getOffset());
      utteranceCsv.print(utterance.all("orthography").length);
      utteranceCsv.println();
    } // next utterance
    u = 1;
    for (Annotation utterance : transcribed.all("utterance")) {
      utteranceCsv.print(u++);
      utteranceCsv.print(reference.getId());
      utteranceCsv.print("wav"); // ref or wav
      utteranceCsv.print(utterance.getStart().getOffset());
      utteranceCsv.print(utterance.getEnd().getOffset());
      utteranceCsv.print(utterance.all("orthography").length);
      utteranceCsv.println();
    } // next utterance
      
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
      case CHANGE: return "~";
      case INSERT: return "+";
      default: return "";
    }
  } // end of operation()

}
