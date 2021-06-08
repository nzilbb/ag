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
package nzilbb.annotator.mor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringBufferInputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import javax.script.ScriptException;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.serialize.SerializationException;
import nzilbb.ag.serialize.SerializationParametersMissingException;
import nzilbb.ag.serialize.SerializerNotConfiguredException;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.util.Merger;
import nzilbb.ag.util.Normalizer;
import nzilbb.configure.ParameterSet;
import nzilbb.formatter.clan.ChatSerialization;
import nzilbb.util.Execution;
import nzilbb.util.IO;
import nzilbb.util.ISO639;

/**
 * Annotator that annotates words tags from the the Talk Bank 
 * <a href="https://talkbank.org/manuals/MOR.pdf"> MOR </a> 
 * morphosyntactic tagger.
 */
@UsesFileSystem
public class MorTagger extends Annotator {
  /** Get the minimum version of the nzilbb.ag API supported by the annotator.*/
  public String getMinimumApiVersion() { return "1.0.3"; }
  
  /**
   * Runs any processing required to uninstall the annotator.
   * <p> In this case, the table created in rdbConnectionFactory() is DROPped.
   */
  @Override
  public void uninstall() {
  }
  
  /**
   * Provides the overall configuration of the annotator. 
   * @return The overall configuration of the annotator, which will be passed to the
   * <i> config/index.html </i> configuration web-app, if any. This configuration may be
   * null, or a string that serializes the annotators configuration state in any encoding
   * the implementor prefers. The resulting string must be interpretable by the
   * <i> config/index.html </i> web-app. 
   * @see #setConfig(String)
   * @see #beanPropertiesToQueryString()
   */
  public String getConfig() {
    return null;
  }
  
  /**
   * The MOR executable file.
   * @return The MOR command-line program.
   */
  public File getMorExe() {
    return new File(getWorkingDirectory(), "mor"); // TODO or exe?
  } // end of getMorExe()

   
  /**
   * Builds the MOR, and installs the default english grammar.
   * <p> If there is no <i> mor </i> program file, the program is compiled from the source
   * code in the <i> unix-clan </i> directory. If no <i> unix-clan.zip </i> file
   * containing the source code has been uploaded by the user, it is downloaded from 
   * <a href="https://dali.talkbank.org/clan/unix-clan.zip">
   * https://dali.talkbank.org/clan/unix-clan.zip</a> and then <i> mor </i> is compiled. 
   * <p> If there are no other directories, then a default English grammar is downloaded from:
   * <a href="https://talkbank.org/morgrams/eng.zip">https://talkbank.org/morgrams/eng.zip</a>
   * and unzipped.
   * @throws InvalidConfigurationException
   * @see #getConfig()
   * @see #beanPropertiesFromQueryString(String)
   */ 
  public void setConfig(String config) throws InvalidConfigurationException {
    running = true;
    setPercentComplete(0);
    setStatus(""); // clear any residual status from the last run...

    try {

      // is the "mor" program present?
      File mor = getMorExe();
      if (!mor.exists()) {     
        // if not, is there a source code directory?
        File unixClan = new File(getWorkingDirectory(), "unix-clan");
        // will need these later...
        File src = new File(unixClan, "src");
        File makefile = new File(src, "makefile");
        
        if (!unixClan.exists()) {
          // if not, get source code
          File zip = new File(getWorkingDirectory(), "unix-clan.zip");
          if (!zip.exists()) { // download it
            String url = "https://dali.talkbank.org/clan/unix-clan.zip";
            setStatus("Downloading " + url);
            IO.SaveUrlToFile​(
              new URL(url), zip,
              new IntConsumer() { public void accept​(int value) {
                setPercentComplete(value / 5); }});
          }
          setStatus("Upzipping " + zip.getName());
          IO.Unzip(
            zip, getWorkingDirectory(),
            new IntConsumer() { public void accept​(int value) {
              setPercentComplete(20 + value / 5); }});
          if (!unixClan.exists()) {
            throw new InvalidConfigurationException(
              this, "Zip file '"+zip.getName()
              +"' does not contain source folder '"+unixClan.getName()+"'");
          }
          
          // uncomment ubuntu lines
          // (but only if we've just unzipped the source code, so that it's possible to
          //  manually edit the makefile to get things working)
          setStatus("Configuring makefile...");
          BufferedReader in = new BufferedReader(new FileReader(makefile));
          File editedMakefile = new File(src, "makefile-edited");
          PrintWriter out = new PrintWriter(new FileWriter(editedMakefile));
          String line = in.readLine();
          boolean uncommentNextLine = false;
          while (line != null) {
            if (line.contains("4.4.1-4ubuntu9")) {
              uncommentNextLine = true;
            } else if (uncommentNextLine) {
              if (line.startsWith("#")) line = line.substring(1);
              uncommentNextLine = false;
            }
            out.println(line);
            
            line = in.readLine();
          } // next line
          in.close();
          out.close();
          makefile.delete();
          editedMakefile.renameTo(makefile);
        } // unix-clan directory doesn't exist
        
        // build the source code

        setPercentComplete(40);
        // in src "make mor"
        setStatus("Running 'make mor'...");
        Execution make = new Execution()
          .setWorkingDirectory(src)
          .setExe("make")
          .arg("mor");
        make.run();
        setStatus(make.stdout());
        if (make.stderr().length() > 0) {
          setStatus("ERROR: " + make.stderr());
          // fall through to compiledMor check - maybe stderr output isn't fatal...
        }
        
        // copy mor to base directory
        File compiledMor = new File(new File(new File(unixClan, "unix"), "bin"), "mor");
        if (!compiledMor.exists()) {
          throw new InvalidConfigurationException(
            this, "Compilation failed to create "+compiledMor.getPath());
        }
        if (!compiledMor.renameTo(mor)) {
            throw new InvalidConfigurationException(
              this, "Could not install compiled '"+mor.getName()+"'");
        }
      } // mor didn't exist
      setPercentComplete(60);

      // is there at least one grammar?
      List<String> grammars = availableGrammars();
      if (grammars.size() <= 0) {
        // if not, download one
        File zip = new File(getWorkingDirectory(), "eng.zip");
        String url = "https://talkbank.org/morgrams/eng.zip";
        setStatus("Downloading " + url);
        IO.SaveUrlToFile​(
          new URL(url), zip,
          new IntConsumer() { public void accept​(int value) {
            setPercentComplete(60 + value / 5); }});
        // unzip it
        IO.Unzip(
          zip, getWorkingDirectory(),
          new IntConsumer() { public void accept​(int value) {
            setPercentComplete(80 + value / 4); }});
      }
      grammars = availableGrammars();
      if (grammars.size() <= 0) {
        throw new InvalidConfigurationException(
          this, "Could not install default grammar from 'eng.zip'");
      }
      
      setStatus("Finished.");
      setPercentComplete(100);
    } catch (IOException x) {
      throw new InvalidConfigurationException(this, x);
    } finally {
      running = false;
    }
  }
  
  /**
   * Takes a file to be used instead of the built-in copy of cmudict.txt
   * @param file The lexicon file.
   * @return null if upload was successful, an error message otherwise.
   */
  public String uploadZip(File file) {
    if (!file.getName().endsWith(".zip")) {
      return file.getName() + " is not a .zip file.";
    }

    try {
      IO.Unzip(file, getWorkingDirectory());
    } catch(IOException exception) {
      return "Could not unzip " + file.getName() + ": " + exception.getMessage();
    }
    return null;
  } // end of uploadLexicon()
  
  /**
   * Lists the grammars that are available for use.
   * @return A list of file names that can be selected.
   */
  public List<String> availableGrammars() {
    return Arrays.stream(
      getWorkingDirectory().listFiles(new FileFilter() {
          public boolean accept(File f) {
            return f.isDirectory() && !f.getName().equals("unix-clan");
          }
        })).map(f->f.getName())
      .collect(Collectors.toList());
  } // end of availableGrammars()
  
  /**
   * ID of the input layer containing word tokens.
   * @see #getTokenLayerId()
   * @see #setTokenLayerId(String)
   */
  protected String tokenLayerId;
  /**
   * Getter for {@link #tokenLayerId}: ID of the input layer containing word tokens.
   * @return ID of the input layer containing word tokens.
   */
  public String getTokenLayerId() { return tokenLayerId; }
  /**
   * Setter for {@link #tokenLayerId}: ID of the input layer containing word tokens.
   * @param newTokenLayerId ID of the input layer containing word tokens.
   */
  public MorTagger setTokenLayerId(String newTokenLayerId) {
    tokenLayerId = newTokenLayerId; return this; }

  /**
   * ID of the layer that determines the language of the whole transcript.
   * @see #getLanguagesLayerId()
   * @see #setLanguagesLayerId(String)
   */
  protected String languagesLayerId;
  /**
   * Getter for {@link #languagesLayerId}: ID of the layer that determines the
   * language of the whole transcript. 
   * @return ID of the layer that determines the language of the whole transcript.
   */
  public String getLanguagesLayerId() { return languagesLayerId; }
  /**
   * Setter for {@link #languagesLayerId}: ID of the layer that determines the
   * language of the whole transcript. 
   * @param newLanguagesLayerId ID of the layer that determines the language of
   * the whole transcript. 
   */
  public MorTagger setLanguagesLayerId(String newLanguagesLayerId) {
    if (newLanguagesLayerId != null // empty string means null
        && newLanguagesLayerId.trim().length() == 0) {
      newLanguagesLayerId = null;
    }
    languagesLayerId = newLanguagesLayerId;
    return this;
  }
  
  /**
   * ID of the output layer.
   * @see #getMorLayerId()
   * @see #setMorLayerId(String)
   */
  protected String morLayerId;
  /**
   * Getter for {@link #morLayerId}: ID of the output layer.
   * @return ID of the output layer.
   */
  public String getMorLayerId() { return morLayerId; }
  /**
   * Setter for {@link #morLayerId}: ID of the output layer.
   * @param newMorLayerId ID of the output layer.
   */
  public MorTagger setMorLayerId(String newMorLayerId) { morLayerId = newMorLayerId; return this; }

  // TODO allow splitting into further layers
  
  /**
   * Sets the configuration for a given annotation task.
   * @param parameters The configuration of the annotator; a value of <tt> null </tt>
   * will apply the default task parameters, with {@link #tokenLayerId} set to the
   * {@link Schema#wordLayerId} and {@link #stemLayerId} set to <q>stem</q>.
   * @throws InvalidConfigurationException
   */
  public void setTaskParameters(String parameters) throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");

    if (parameters == null) { // apply default configuration
      
      if (schema.getLayer("orthography") != null) {
        tokenLayerId = "orthography";
      } else {
        tokenLayerId = schema.getWordLayerId();
      }
         
      try {
        // default transcript language layer
        Layer[] candidates = schema.getMatchingLayers(
          "layer.parentId == schema.root.id && layer.alignment == 0" // transcript attribute
          +" && /.*lang.*/.test(layer.id)"); // with 'lang' in the name
        if (candidates.length > 0) languagesLayerId = candidates[0].getId();
        
        // default output layer
        candidates = schema.getMatchingLayers(
          "layer.parentId == schema.wordLayerId && layer.alignment == 0" // word tag
          +" && (/.*mor.*/.test(layer.id))");
        if (candidates.length > 0) {
          morLayerId = candidates[0].getId();
        } else { // suggest adding a new one
          morLayerId = "mor";
        }

      } catch(ScriptException impossible) {}
      
    } else {
      beanPropertiesFromQueryString(parameters);
    }
    
    if (schema.getLayer(tokenLayerId) == null)
      throw new InvalidConfigurationException(this, "Token layer not found: " + tokenLayerId);
    if (languagesLayerId != null && schema.getLayer(languagesLayerId) == null) 
      throw new InvalidConfigurationException(
        this, "Transcript language layer not found: " + languagesLayerId);
      
    // does the outputLayer need to be added to the schema?
    Layer morLayer = schema.getLayer(morLayerId);
    if (morLayer == null) {
      schema.addLayer(
        new Layer(morLayerId)
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true)
        .setParentId(schema.getWordLayerId()));
    } else {
      if (morLayerId.equals(tokenLayerId)
          || morLayerId.equals(languagesLayerId)) {
        throw new InvalidConfigurationException(this, "Invalid MOR layer: " + morLayerId);
      }
    }
  }
  
  /**
   * Determines which layers the annotator requires in order to annotate a graph.
   * @return A list of layer IDs.
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public String[] getRequiredLayers() throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");
    if (tokenLayerId == null)
      throw new InvalidConfigurationException(this, "No input token layer set.");
    if (languagesLayerId == null)
      throw new InvalidConfigurationException(this, "No input transcript language layer set.");
    Vector<String> requiredLayers = new Vector<String>();
    requiredLayers.add(tokenLayerId);
    requiredLayers.add(languagesLayerId);
    return requiredLayers.toArray(new String[0]);
  }
  
  /**
   * Determines which layers the annotator will create/update/delete annotations on.
   * @return A list of layer IDs.
   * empty array.
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public String[] getOutputLayers() throws InvalidConfigurationException {
    if (morLayerId == null)
      throw new InvalidConfigurationException(this, "MOR layer not set.");
    return new String[] { morLayerId };
  }
  
  private ISO639 iso639 = new ISO639(); // for standard ISO 639 language code processing

  /**
   * Annotate the transcript tokens with morphosyntactic tags from MOR.
   * @param graph The graph to transform.
   * @return The changes introduced by the tranformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph transform(Graph graph) throws TransformationException {
    running = true;
    try {
      setStatus("Tagging " + graph.getId());
      setPercentComplete(0);
      
      Layer tokenLayer = graph.getSchema().getLayer(tokenLayerId);
      if (tokenLayer == null) {
        throw new InvalidConfigurationException(
          this, "Invalid input token layer: " + tokenLayerId);
      }
      Layer morLayer = graph.getSchema().getLayer(morLayerId);
      if (morLayer == null) {
        throw new InvalidConfigurationException(this, "Invalid output MOR layer: " + morLayerId);
      }
      Layer languagesLayer = graph.getSchema().getLayer(languagesLayerId);
      if (languagesLayer == null) {
        throw new InvalidConfigurationException(
          this, "Invalid languages layer: " + languagesLayerId);
      }

      // determine the grammar from the language
      final Annotation language = graph.first(languagesLayer.getId());
      if (language == null) {
        throw new InvalidConfigurationException(
          this, "Cannot determine grammar, no language set for " + graph.getId());
      }
      String iso639Alpha3 = iso639.alpha3(language.getLabel()).orElseThrow(
        ()->new InvalidConfigurationException(
          this, "Cannot determine grammar, language is not an ISO639 alpha-3 code: "
          + language.getLabel()));
      File grammar = new File(getWorkingDirectory(), iso639Alpha3);
      if (!grammar.exists()) {
        throw new InvalidConfigurationException(this, "Grammar not installed: " + iso639Alpha3);
      }
      
      // save the transcript in CHAT format
      ChatSerialization converter = new ChatSerialization();
      ParameterSet configuration = converter.configure(new ParameterSet(), schema);
      configuration.get("morLayer").setValue(morLayer);
      configuration.get("languagesLayer").setValue(languagesLayer);
      
      converter.configure(configuration, schema);
      final Vector<SerializationException> exceptions = new Vector<SerializationException>();
      final Vector<NamedStream> serializeStreams = new Vector<NamedStream>();
      String[] layers = { tokenLayerId, languagesLayerId };
      Graph[] graphs = { graph };
      try {
        converter.serialize(Arrays.spliterator(graphs), layers,
                            stream -> serializeStreams.add(stream),
                            warning -> setStatus(warning),
                            exception -> exceptions.add(exception));
      } catch(SerializerNotConfiguredException x) {
        throw new TransformationException(this, x);
      }
      if (exceptions.size() > 0) {
        Throwable firstException = null;
        for (SerializationException x : exceptions) {
          if (firstException != null) firstException = x;
          setStatus("ERROR: " + x.getMessage());
          throw new TransformationException(this, firstException);
        }
      }
      try {        
        File cha = File.createTempFile(graph.getId() + "-", ".cha");
        System.out.println("CHA: " + cha.getPath());
        IO.SaveInputStreamToFile(serializeStreams.elementAt(0).getStream(), cha);
        setPercentComplete(25);
        
        try {
          // run MOR on the CHAT file ...
          // weirdly, if we run "mor +Lgrammar .cha" the process hangs
          // so instead, we use -f, write the .cha to stdin, and read the result from stdout
          Execution mor = new Execution()
            .setExe(getMorExe())
            .arg("+L"+grammar.getPath())
            .arg("-f");
          // start the process in its own thread
          new Thread(mor).start();
          // wait until we've got a process 
          while (mor.getProcess() == null) try { Thread.sleep(500); } catch(Exception exception) {}
          // write the .cha file to stdin
          IO.Pump(new FileInputStream(cha), mor.getProcess().getOutputStream());
          // wait for mor to finish
          while (!mor.getFinished()) try { Thread.sleep(500); } catch(Exception exception) {}
          // the annotated version has been written to stdout...
          setStatus(mor.stderr()); // stderr has a whole bunch of non-error output
          setPercentComplete(50);
          
          // parse the CHAT file
          NamedStream[] deserializeStreams = {
            new NamedStream(new StringBufferInputStream(mor.stdout()), graph.getId()+".cha") };
          ParameterSet defaultParamaters = converter.load(deserializeStreams, graph.getSchema());
          converter.setParameters(defaultParamaters);
          graphs = converter.deserialize();
          for (String warning : converter.getWarnings()) setStatus(warning);
          Graph tagged = graphs[0];
          new Normalizer().transform(tagged);
          tagged.commit();
          setPercentComplete(75);
          
          // merge the changes into our graph
          Merger merger = new Merger(tagged);
          merger.transform(graph);
          
          setPercentComplete(100);
        } finally {
          cha.delete();
        }
      } catch (SerializationParametersMissingException x) {
        throw new TransformationException(this, x);
      } catch (SerializerNotConfiguredException x) {
        throw new TransformationException(this, x);
      } catch (SerializationException x) {
        throw new TransformationException(this, x);
      } catch (IOException x) {
        throw new TransformationException(this, x);
      }
      return graph;
    } finally {
      running = false;
    }
  }
}
