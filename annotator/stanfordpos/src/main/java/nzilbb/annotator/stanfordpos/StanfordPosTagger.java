//
// Copyright 2021-2024 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.annotator.stanfordpos;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Vector;
import java.util.function.IntConsumer;
import java.util.regex.*;
import java.util.stream.Collectors;
import javax.script.ScriptException;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.Dictionary;
import nzilbb.ag.automation.DictionaryException;
import nzilbb.ag.automation.ImplementsDictionaries;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.UsesRelationalDatabase;
import nzilbb.encoding.ValidLabelsDefinitions;
import nzilbb.sql.ConnectionFactory;
import nzilbb.util.IO;

// TODO add option to tease apart POS labels
// TODO e.g. VBD = VB + past, VBG = VB + continuous, etc.

/**
 * Annotator that tags words with their part of speech (POS) according to the 
 * <a href="https://nlp.stanford.edu/software/tagger.html">Stanford POS Tagger</a>.
 */
@UsesFileSystem
public class StanfordPosTagger extends Annotator {
  /** Get the minimum version of the nzilbb.ag API supported by the annotator.*/
  public String getMinimumApiVersion() { return "1.2.1"; }
  
  /**
   * Runs any processing required to uninstall the annotator.
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
   * Downloads and installs the POS tagger.
   * <p> If there is no .zip file from which to unpack the POS tagger, it's downloaded
   * from <a href="https://nlp.stanford.edu/software/stanford-tagger-4.2.0.zip">
   * https://nlp.stanford.edu/software/stanford-tagger-4.2.0.zip</a>.
   * @throws InvalidConfigurationException
   * @see #getConfig()
   * @see #beanPropertiesFromQueryString(String)
   */ 
  public void setConfig(String config) throws InvalidConfigurationException {
    setRunning(true);
    setPercentComplete(0);
    setStatus(""); // clear any residual status from the last run...

    try {
      // is there a zip file that's newer than the "models" subdirectory?
      boolean downloadZip = true;
      File models = new File(getWorkingDirectory(), "models");   
      File[] zips = getWorkingDirectory().listFiles(new FilenameFilter() {
          public boolean accept(File dir, String name) {
            return name.endsWith(".zip");
          }
        });
      setStatus(
        "There " + (zips.length==1?"is one zip file":"are " + zips.length + " zip files."));
      if (zips == null || zips.length == 0) {
        // download from https://nlp.stanford.edu/software/stanford-tagger-4.2.0.zip
        File zip = new File(getWorkingDirectory(), "stanford-tagger-4.2.0.zip");
        System.out.println("starting download...");
        setStatus("Downloading https://nlp.stanford.edu/software/stanford-tagger-4.2.0.zip");
        IO.SaveUrlToFile​(
          new URL("https://nlp.stanford.edu/software/stanford-tagger-4.2.0.zip"),
          zip, new IntConsumer() { public void accept​(int value) {
            setPercentComplete(value / 2);
          }});
        zips = new File[] { zip };
      }
      setPercentComplete(50);
      File newestZip = null;
      for (File zip : zips) {
        if (newestZip == null || zip.lastModified() > newestZip.lastModified()) {
          newestZip = zip;
        }
      }
      if (newestZip == null) {
        setStatus(
          "There is no .zip file containing the Stanford Parser implementation. Please try again.");
        throw new InvalidConfigurationException(
          this,
          "There is no .zip file containing the Stanford Parser implementation. Please try again.");
      }
      setStatus("Most recent .zip file: " + newestZip.getName());
      // should we unzip the tagger implementation?
      if (!models.exists() || newestZip.lastModified() > models.lastModified()) {
        // unzip into a temporary directory
        File temp = new File(getWorkingDirectory(), "unzip");
        if (temp.exists()) {
          IO.RecursivelyDelete(temp);
        }
        temp.mkdir();
        setStatus("Unzipping models from " + newestZip.getName());
        IO.UnzipOnly(newestZip, temp, ".*/models/.*", new IntConsumer() {
            public void accept​(int value) {setPercentComplete(50 + value / 2); }});
        
        // move contents into the current directory
        File source = temp;
        // but if temp just contains a single directory, that's the source
        File[] contents = temp.listFiles();
        if (contents.length == 1 && contents[0].isDirectory()) {
          source = contents[0];
        }
        for (File s : source.listFiles()) {
          File d = new File(getWorkingDirectory(), s.getName());
          s.renameTo(d);
        } // next source file
        IO.RecursivelyDelete(temp);
      }
      
      if (!models.exists()) {
        setStatus("There are no models. Please try again.");
        throw new InvalidConfigurationException(this, "There are no models. Please try again.");
      }
      
      setStatus("Finished.");
      setPercentComplete(100);
    } catch (IOException x) {
      throw new InvalidConfigurationException(this, x);
    } finally {
      setRunning(false);
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
    
    File localFile = new File(getWorkingDirectory(), file.getName());
    if (!file.renameTo(localFile)) {
      try {
        IO.Copy(file, localFile);
      } catch(IOException exception) {
        return "Could not copy " + file.getName() + ": " + exception.getMessage();
      }
    }
    return null;
  } // end of uploadLexicon()
  
  /**
   * Lists the model files that are available for use.
   * @return A list of file names that can be selected.
   */
  public List<String> availableModels() {
    File models = new File(getWorkingDirectory(), "models");
    if (!models.exists()) return new Vector<String>();
    return Arrays.asList(
      models.list(new FilenameFilter() {
          public boolean accept(File dir, String name) {
            return name.endsWith(".tagger");
          }
        }));
  } // end of availableModels()
  
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
  public StanfordPosTagger setTokenLayerId(String newTokenLayerId) {
    tokenLayerId = newTokenLayerId; return this; }
  
  /**
   * Regular expression for excluding tokens.
   * @see #getTokenExclusionPattern()
   * @see #setTokenExclusionPattern(String)
   */
  protected String tokenExclusionPattern = "";
  /**
   * Getter for {@link #tokenExclusionPattern}: Regular expression for excluding tokens.
   * @return Regular expression for excluding tokens.
   */
  public String getTokenExclusionPattern() { return tokenExclusionPattern; }
  /**
   * Setter for {@link #tokenExclusionPattern}: Regular expression for excluding tokens.
   * @param newTokenExclusionPattern Regular expression for excluding tokens.
   */
  public StanfordPosTagger setTokenExclusionPattern(String newTokenExclusionPattern) { tokenExclusionPattern = newTokenExclusionPattern; return this; }
  
  /**
   * ID of the input layer that partitions the tokens into chunks for feeding to the tagger.
   * @see #getChunkLayerId()
   * @see #setChunkLayerId(String)
   */
  protected String chunkLayerId;
  /**
   * Getter for {@link #chunkLayerId}: ID of the input layer that partitions the tokens
   * into chunks for feeding to the tagger. 
   * @return ID of the input layer that partitions the tokens into chunks for feeding to
   * the tagger. 
   */
  public String getChunkLayerId() { return chunkLayerId; }
  /**
   * Setter for {@link #chunkLayerId}: ID of the input layer that partitions the tokens
   * into chunks for feeding to the tagger. 
   * @param newChunkLayerId ID of the input layer that partitions the tokens into chunks
   * for feeding to the tagger. 
   */
  public StanfordPosTagger setChunkLayerId(String newChunkLayerId) { chunkLayerId = newChunkLayerId; return this; }
  
  /**
   * ID of the layer that determines the language of the whole transcript.
   * @see #getTranscriptLanguageLayerId()
   * @see #setTranscriptLanguageLayerId(String)
   */
  protected String transcriptLanguageLayerId;
  /**
   * Getter for {@link #transcriptLanguageLayerId}: ID of the layer that determines the
   * language of the whole transcript. 
   * @return ID of the layer that determines the language of the whole transcript.
   */
  public String getTranscriptLanguageLayerId() { return transcriptLanguageLayerId; }
  /**
   * Setter for {@link #transcriptLanguageLayerId}: ID of the layer that determines the
   * language of the whole transcript. 
   * @param newTranscriptLanguageLayerId ID of the layer that determines the language of
   * the whole transcript. 
   */
  public StanfordPosTagger setTranscriptLanguageLayerId(String newTranscriptLanguageLayerId) {
    if (newTranscriptLanguageLayerId != null // empty string means null
        && newTranscriptLanguageLayerId.trim().length() == 0) {
      newTranscriptLanguageLayerId = null;
    }
    transcriptLanguageLayerId = newTranscriptLanguageLayerId;
    return this;
  }
  
  /**
   * ID of the layer that determines the language of individual phrases.
   * @see #getPhraseLanguageLayerId()
   * @see #setPhraseLanguageLayerId(String)
   */
  protected String phraseLanguageLayerId;
  /**
   * Getter for {@link #phraseLanguageLayerId}: ID of the layer that determines the
   * language of individual phrases. 
   * @return ID of the layer that determines the language of individual phrases.
   */
  public String getPhraseLanguageLayerId() { return phraseLanguageLayerId; }
  /**
   * Setter for {@link #phraseLanguageLayerId}: ID of the layer that determines the
   * language of individual phrases. 
   * @param newPhraseLanguageLayerId ID of the layer that determines the language of
   * individual phrases. 
   */
  public StanfordPosTagger setPhraseLanguageLayerId(String newPhraseLanguageLayerId) {
    if (newPhraseLanguageLayerId != null // empty string means null
        && newPhraseLanguageLayerId.trim().length() == 0) {
      newPhraseLanguageLayerId = null;
    }
    phraseLanguageLayerId = newPhraseLanguageLayerId;
    return this;
  }
  
  /**
   * Regular expression for specifying which language to tag the tokens of.
   * @see #getTargetLanguagePattern()
   * @see #setTargetLanguagePattern(String)
   */
  protected String targetLanguagePattern;
  /**
   * Getter for {@link #targetLanguagePattern}: Regular expression for specifying which
   * language to tag the tokens of. 
   * @return Regular expression for specifying which language to tag the tokens of.
   */
  public String getTargetLanguagePattern() { return targetLanguagePattern; }
  /**
   * Setter for {@link #targetLanguagePattern}: Regular expression for specifying which
   * language to tag the tokens of. 
   * @param newTargetLanguagePattern Regular expression for specifying which language to
   * tag the tokens of. 
   */
  public StanfordPosTagger setTargetLanguagePattern(String newTargetLanguagePattern) {
    if (newTargetLanguagePattern != null // empty string means null
        && newTargetLanguagePattern.trim().length() == 0) {
      newTargetLanguagePattern = null;
    }
    targetLanguagePattern = newTargetLanguagePattern;
    return this;
  }
  
  /**
   * Model to use for tagging.
   * @see #getModel()
   * @see #setModel(String)
   */
  protected String model;
  /**
   * Getter for {@link #model}: Model to use for tagging.
   * @return Model to use for tagging.
   */
  public String getModel() { return model; }
  /**
   * Setter for {@link #model}: Model to use for tagging.
   * @param newModel Model to use for tagging.
   */
  public StanfordPosTagger setModel(String newModel) { model = newModel; return this; }
  
  /**
   * ID of the output layer.
   * @see #getPosLayerId()
   * @see #setPosLayerId(String)
   */
  protected String posLayerId;
  /**
   * Getter for {@link #posLayerId}: ID of the output layer.
   * @return ID of the output layer.
   */
  public String getPosLayerId() { return posLayerId; }
  /**
   * Setter for {@link #posLayerId}: ID of the output layer.
   * @param newPosLayerId ID of the output layer.
   */
  public StanfordPosTagger setPosLayerId(String newPosLayerId) { posLayerId = newPosLayerId; return this; }
  
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

    targetLanguagePattern = null;

    if (parameters == null) { // apply default configuration
      
      if (schema.getLayer("orthography") != null) {
        tokenLayerId = "orthography";
      } else {
        tokenLayerId = schema.getWordLayerId();
      }
         
      chunkLayerId = schema.getTurnLayerId();
      
      try {
        // default transcript language layer
        Layer[] candidates = schema.getMatchingLayers(
          "layer.parentId == schema.root.id && layer.alignment == 0" // transcript attribute
          +" && /.*lang.*/.test(layer.id)"); // with 'lang' in the name
        if (candidates.length > 0) transcriptLanguageLayerId = candidates[0].getId();
        
        // default phrase language layer
        candidates = schema.getMatchingLayers(
          "layer.parentId == schema.turnLayerId" // child of turn
          +" && /.*lang.*/.test(layer.id)"); // with 'lang' in the name
        if (candidates.length > 0) phraseLanguageLayerId = candidates[0].getId();
        
        // default output layer
        candidates = schema.getMatchingLayers(
          "layer.parentId == schema.wordLayerId && layer.alignment == 0" // word tag
          +" && (/.*pos.*/.test(layer.id) || /.*part.*of.*speech.*/.test(layer.id))");
        if (candidates.length > 0) {
          posLayerId = candidates[0].getId();
        } else { // suggest adding a new one
          posLayerId = "pos";
        }

        // default model
        model = "english-caseless-left3words-distsim.tagger";

        // no exclusion pattern
        tokenExclusionPattern = "";
        
      } catch(ScriptException impossible) {}
      
    } else {
      beanPropertiesFromQueryString(parameters);
    }
    
    if (schema.getLayer(tokenLayerId) == null)
      throw new InvalidConfigurationException(this, "Token layer not found: " + tokenLayerId);
    if (schema.getLayer(chunkLayerId) == null)
      throw new InvalidConfigurationException(this, "Chunk layer not found: " + chunkLayerId);
    if (transcriptLanguageLayerId != null && schema.getLayer(transcriptLanguageLayerId) == null) 
      throw new InvalidConfigurationException(
        this, "Transcript language layer not found: " + transcriptLanguageLayerId);
    if (phraseLanguageLayerId != null && schema.getLayer(phraseLanguageLayerId) == null) 
      throw new InvalidConfigurationException(
        this, "Phrase language layer not found: " + phraseLanguageLayerId);
    if (tokenExclusionPattern != null && tokenExclusionPattern.length() > 0) {
      try {
        Pattern.compile(tokenExclusionPattern);
      } catch(PatternSyntaxException exception) {
        throw new InvalidConfigurationException(
          this, "Invalid token exclusion pattern: " + exception.getMessage(), exception);
      }
    }
    if ("".equals(targetLanguagePattern)) targetLanguagePattern = null;
    if (targetLanguagePattern != null) {
      try {
       Pattern.compile(targetLanguagePattern);
      } catch(PatternSyntaxException x) {
        throw new InvalidConfigurationException(
          this, "Invalid Target Language \""+targetLanguagePattern+"\": " + x.getMessage());
      }
    }

    // valid model
    if (model == null || model.trim().length() == 0)
      throw new InvalidConfigurationException(this, "No model selected.");
    File modelFile = new File(new File(getWorkingDirectory(), "models"), model);
    if (!modelFile.exists())
      throw new InvalidConfigurationException(this, "Model not found: " + model);
      
    // does the outputLayer need to be added to the schema?
    Layer posLayer = schema.getLayer(posLayerId);
    if (posLayer == null) {
      posLayer = new Layer(posLayerId)
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false)
        .setParentIncludes(true)
        .setParentId(schema.getWordLayerId());
      schema.addLayer(posLayer);
    } else {
      if (posLayerId.equals(tokenLayerId)
          || posLayerId.equals(transcriptLanguageLayerId)
          || posLayerId.equals(phraseLanguageLayerId)) {
        throw new InvalidConfigurationException(this, "Invalid POS layer: " + posLayerId);
      }
      // ensure layer properties are valid
      if (posLayer.getType() != Constants.TYPE_STRING)
        posLayer.setType(Constants.TYPE_STRING);
      if (posLayer.getAlignment() != Constants.ALIGNMENT_INTERVAL)
        posLayer.setAlignment(Constants.ALIGNMENT_INTERVAL);
      if (!posLayer.getPeers())
        posLayer.setPeers(true);
      if (!posLayer.getParentIncludes())
        posLayer.setParentIncludes(true);
      if (schema.getWordLayerId() != null // word child layer
          && schema.getWordLayerId().equals(posLayer.getParentId())) {
        if (posLayer.getPeersOverlap())
          posLayer.setPeersOverlap(false);
        if (!posLayer.getSaturated())
          posLayer.setSaturated(true);
      }
    }
    // ensure valid labels are set
    if (posLayer.getValidLabels().size() == 0) {
      // use richer 'validLabelsDefinition' for better UX in LaBB-CAT
      List<Map<String,Object>> validLabelsDefinition = generateValidLabelsDefinition();      
      // for LaBB-CAT:
      posLayer.put("validLabelsDefinition", validLabelsDefinition);
      // for general use
      posLayer.setValidLabels(
        ValidLabelsDefinitions.ValidLabelsFromDefinition(validLabelsDefinition));
    }
  }
  
  /**
   * Generates a validLabelsDefinition structure for ensuring there's a user-friendly
   * selector in LaBB-CAT with the Penn Treebank POS tag set.
   * @return A list of objects definining the Penn Treebank POS tag set.
   * @see https://web.stanford.edu/~jurafsky/slp3/old_oct19/8.pdf
   */
  private List<Map<String,Object>> generateValidLabelsDefinition() {
    List<Map<String,Object>> validLabelsDefinition = new Vector<Map<String,Object>>();
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "CC"); put("description", "coordinating conjunction e.g. and, but, or"); put("category", "MISC");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "CD"); put("description", "cardinal number e.g. one two"); put("category", "MISC");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "DT"); put("description", "determiner e.g. a the"); put("category", "DETERMINER");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "EX"); put("description", "existential 'there'"); put("category", "MISC");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "FW"); put("description", "foreign word  e.g. mea culpa"); put("category", "MISC");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "IN"); put("description", "preposition/subordin-conj e.g. of, in, by"); put("category", "MISC");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "JJ"); put("description", "adjective e.g. yellow"); put("category", "ADJECTIVAL");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "JJR"); put("description", "comparative adjective e.g. bigger"); put("category", "ADJECTIVAL");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "JJS"); put("description", "superlative adjective e.g. biggest"); put("category", "ADJECTIVAL");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "LS"); put("description", "list item marker e.g. 1, 2, One"); put("category", "MISC");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "MD"); put("description", "modal e.g. can, should"); put("category", "VERB");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "NN"); put("description", "singular or mass noun e.g. llama"); put("category", "NOUN");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "NNS"); put("description", "plural noun e.g. llamas"); put("category", "NOUN");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "NNP"); put("description", "singular proper noun e.g. IBM"); put("category", "NOUN");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "NNPS"); put("description", "plural proper noun e.g. Carolinas"); put("category", "NOUN");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "PDT"); put("description", "predeterminer e.g. all, both"); put("category", "DETERMINER");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "POS"); put("description", "possessive ending e.g. 's"); put("category", "ADJECTIVAL");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "PRP"); put("description", "personal pronoun e.g. I, you, he"); put("category", "NOUN");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "PRP$"); put("description", "possessive pronoun e.g. your, one's"); put("category", "ADJECTIVAL");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "RB"); put("description", "adverb e.g. quickly"); put("category", "ADVERBIAL");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "RBR"); put("description", "comparative adverb e.g. quicker"); put("category", "ADVERBIAL");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "RBS"); put("description", "superlative adverb e.g. quickest"); put("category", "ADVERBIAL");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "RP"); put("description", "particle e.g. up off"); put("category", "MISC");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "SYM"); put("description", "symbol e.g. =, %, &"); put("category", "SYMBOL");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "TO"); put("description", "'to'"); put("category", "MISC");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "UH"); put("description", "interjection e.g. ah, oops"); put("category", "MISC");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "VB"); put("description", "verb base form e.g. eat"); put("category", "VERB");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "VBD"); put("description", "verb past tense e.g. ate"); put("category", "VERB");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "VBG"); put("description", "verb gerund e.g. eating"); put("category", "VERB");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "VBN"); put("description", "verb past participle e.g. eaten"); put("category", "VERB");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "VBP"); put("description", "verb non-3sg present e.g. eat"); put("category", "VERB");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "VBZ"); put("description", "verb 3sg present e.g. eats"); put("category", "VERB");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "WDT"); put("description", "wh- determiner e.g. which, that"); put("category", "DETERMINER");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "WP"); put("description", "wh- pronoun e.g. what, who"); put("category", "NOUN");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "WP$"); put("description", "wh- possessive e.g. whose"); put("category", "ADJECTIVAL");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "WRB"); put("description", "wh- adverb e.g. how, where"); put("category", "ADVERBIAL");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "$"); put("description", "dollar sign e.g. $"); put("category", "SYMBOL");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "#"); put("description", "pound sign e.g. #"); put("category", "SYMBOL");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "“"); put("description", "left quote e.g. ‘ or “"); put("category", "PUNCTUATION");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "”"); put("description", "right quote e.g. ’ or ”"); put("category", "PUNCTUATION");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "("); put("description", "left parenthesis e.g. [, (, {, <"); put("category", "SYMBOL");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", ")"); put("description", "right parenthesis e.g. ], ), }, >"); put("category", "SYMBOL");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", ","); put("description", "comma"); put("category", "PUNCTUATION");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", "."); put("description", "end sentence punctuation e.g. . ! ?"); put("category", "PUNCTUATION");
    }});
    validLabelsDefinition.add(new HashMap<String,Object>() {{
      put("label", ":"); put("description", "mid-sentence punctuation e.g. : ; ... – -"); put("category", "PUNCTUATION");
    }});
    // ensure that in all cases, "display" is the same as "label", so that it appears in
    // LaBB-CAT's selector
    for (Map<String,Object> definition : validLabelsDefinition) {
      definition.put("display", definition.get("label"));
    }
    return validLabelsDefinition;
  } // end of generateValidLabelsDefinition()
  
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
    if (chunkLayerId == null)
      throw new InvalidConfigurationException(this, "No input chunking layer set.");
    Vector<String> requiredLayers = new Vector<String>();
    requiredLayers.add(tokenLayerId);
    requiredLayers.add(chunkLayerId);
    if (transcriptLanguageLayerId != null) requiredLayers.add(transcriptLanguageLayerId);
    if (phraseLanguageLayerId != null) requiredLayers.add(phraseLanguageLayerId);
    return requiredLayers.toArray(new String[0]);
  }
  
  /**
   * Determines which layers the annotator will create/update/delete annotations on.
   * @return A list of layer IDs. 
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public String[] getOutputLayers() throws InvalidConfigurationException {
    if (posLayerId == null)
      throw new InvalidConfigurationException(this, "POS layer not set.");
    return new String[] { posLayerId };
  }
  
  /**
   * Tag the transcript tokens with part-of-speech tags.
   * @param graph The graph to transform.
   * @return The changes introduced by the tranformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public Graph transform(Graph graph) throws TransformationException {
    setRunning(true);
    try {
      setStatus("Tagging " + graph.getId());
      
      Layer tokenLayer = graph.getSchema().getLayer(tokenLayerId);
      if (tokenLayer == null) {
        throw new InvalidConfigurationException(
          this, "Invalid input token layer: " + tokenLayerId);
      }
      Layer chunkLayer = graph.getSchema().getLayer(chunkLayerId);
      if (chunkLayer == null) {
        throw new InvalidConfigurationException(
          this, "Invalid input chunk layer: " + chunkLayerId);
      }
      Layer posLayer = graph.getSchema().getLayer(posLayerId);
      if (posLayer == null) {
        throw new InvalidConfigurationException(this, "Invalid output POS layer: " + posLayerId);
      }

      boolean transcriptIsMainlyTargetLang = true;
      if (transcriptLanguageLayerId != null && targetLanguagePattern != null) {
        Annotation transcriptLanguage = graph.first(transcriptLanguageLayerId);
        if (transcriptLanguage != null) {
          if (!transcriptLanguage.getLabel().matches(targetLanguagePattern)) { // not TargetLang
            transcriptIsMainlyTargetLang = false;
          }
        } else { // transcript has no language, but we target a language
          transcriptIsMainlyTargetLang = false;
        }
      }
      boolean thereArePhraseTags = false;
      if (phraseLanguageLayerId != null) {
        if (graph.first(phraseLanguageLayerId) != null) {
          thereArePhraseTags = true;
        }
      }

      if (!transcriptIsMainlyTargetLang && !thereArePhraseTags) {
        setStatus("There are no tokens in the target language ("+targetLanguagePattern+")");
      } else {
        // handle language selection...
        
        MaxentTagger tagger = new MaxentTagger(
          new File(new File(getWorkingDirectory(), "models"), model).getPath());
        
        for (Annotation chunk : graph.all(chunkLayerId)) {
          if (isCancelling()) break;
        
          Annotation[] tokens = chunk.all(tokenLayerId);
          
          // delete all existing tags before filtering out by pattern
          for (Annotation t : tokens) {            
            for (Annotation p: t.all(posLayerId)) {
              p.destroy();
            } // next pos tag
          } // next token
          
          if (tokenExclusionPattern.length() > 0) {
            final Pattern exclude = Pattern.compile(tokenExclusionPattern);
            tokens = Arrays.stream(tokens).filter(t->!exclude.matcher(t.getLabel()).matches())
              .toArray(Annotation[]::new);
          }

          if (!transcriptIsMainlyTargetLang) { // transcript is wrong language
            // filter out tokens that aren't phrase-tagged in the target language
            final Pattern targetLanguage = Pattern.compile(targetLanguagePattern);
            tokens = Arrays.stream(tokens).filter((token) -> {
                Annotation phraseLanguage = token.first(phraseLanguageLayerId);
                if (phraseLanguage == null) return false; // not tagged with a language
                return targetLanguage.matcher(phraseLanguage.getLabel()).matches();
              }).toArray(Annotation[]::new);
          } else if (thereArePhraseTags // there are phrase-based language tags
                     && targetLanguagePattern != null) { // and we care about language
            // filter out tokens that aren't phrase-tagged in another language
            final Pattern targetLanguage = Pattern.compile(targetLanguagePattern);
            tokens = Arrays.stream(tokens).filter((token) -> {
                Annotation phraseLanguage = token.first(phraseLanguageLayerId);
                if (phraseLanguage == null) return true; // not tagged with a language
                return targetLanguage.matcher(phraseLanguage.getLabel()).matches();
              }).toArray(Annotation[]::new);
          }
        
          setStatus("Tagging chunk "+chunk.getStart() + "-" + chunk.getEnd());
          if (tokens.length > 0) {
            
            // if tokens contain spaces, these are eliminated by the parser
            // so create a space-stripped version of the label
            for (Annotation t : tokens) {
              t.put("@untagged", t.getLabel().replaceAll("\\s",""));
            } // next token
            
            String text = Arrays.stream(tokens)
              .map(token -> token.getLabel())
              .collect(Collectors.joining(" "));
            List<List<HasWord>> sentences = MaxentTagger.tokenizeText(new StringReader(text));
            int t = 0;
            for (List<HasWord> sentence : sentences) {
              List<TaggedWord> taggedSentence = tagger.tagSentence(sentence);
              for (TaggedWord w : taggedSentence) {
                if (t >= tokens.length) {
                  throw new TransformationException(
                    this, "Too many tags for tokens. Last token: "
                    + tokens[t-1]
                    + ", next POS tag: " + w.tag() + " for word " + w.word());
                }
                Annotation token = tokens[t];
                // skip any tokens with blank labels
                while (token.getLabel().trim().length() == 0) {
                  token = tokens[++t];
                }
                
                graph.createSubdivision(token, posLayerId, w.tag())
                  .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
                // there can be more than one tag per token
                // e.g. "I'll" could be tagged "PRP" and "MD"
                // so we keep a track of how much of the current token has been tagged
                String untagged = (String)token.get("@untagged");              
                if (untagged.equals(w.word())) { // finished with this token
                  token.remove("@untagged");
                  t++;
                } else if (untagged.startsWith(w.word())) { // partial tag
                  token.put("@untagged", untagged.substring(w.word().length()));
                } else { // something's gone wrong!
                  throw new TransformationException(
                    this, "Chunk "+chunk.getStart() + "-" + chunk.getEnd()
                    + ": Unexpected word in result: \"" + w.word() + "\""
                    +" - was expecting something like: \"" + untagged + "\"."
                    +" - token: \"" + token.getLabel() + "\" - " + token.getId()
                    +" Tagged: " + taggedSentence.stream()
                    .map(tok->tok.word()+"->"+tok.tag())
                    .collect(Collectors.joining(" ")));
                }
              } // next token
            } // next sentence
          } // there are tokens
        } // next chunk
      } // there are possibly tokens in the right language
      
      return graph;
    } finally {
      setRunning(false);
    }
  }
}
