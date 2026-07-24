//
// Copyright 2026 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.annotator.tokiponapos;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
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
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.util.IO;
import nzilbb.editpath.*;
import nzilbb.util.Execution;
import nzilbb.encoding.comparator.Orthography2OrthographyComparator;

/**
 * Annotator that tags <a href="https://tokipona.org/">Toki Pona</a> words
 * with their part of speech (POS) according to the 
 * <a href="https://github.com/cubedhuang/ilo-nasin/">ilo-nasin</a> tagger.
 */
@UsesFileSystem
public class IloNasinTagger extends Annotator {
  /** Get the minimum version of the nzilbb.ag API supported by the annotator.*/
  public String getMinimumApiVersion() { return "1.4.0"; }

  /** Currently-running node process, if any. */
  Process process;
  /**
   * Cancels the current operation, if any.
   */
  @Override public void cancel() {
    if (process != null && process.isAlive()) {
      process.destroyForcibly();
    }
    super.cancel();
  }
  
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
   * Installs the POS tagger from npm.
   * @throws InvalidConfigurationException
   * @see #getConfig()
   * @see #beanPropertiesFromQueryString(String)
   */ 
  public void setConfig(String config) throws InvalidConfigurationException {
    setRunning(true);
    setPercentComplete(0);
    setStatus(""); // clear any residual status from the last run...
    
    try {
      String[] aFiles = {
        "ilo-nasin/index.js",
        "ilo-nasin/package.json",
        "ilo-nasin/package-lock.json" };
      for (String file : aFiles) {
        URL urlSource = getClass().getResource(file);
        File fDestination = getWorkingDirectory();
        String[] pathElements = file.split("/");
        for (String element : pathElements) {
          if (!fDestination.exists()) fDestination.mkdir();
          fDestination = new File(fDestination, element);
        } // next path element
        
        if (!fDestination.exists()) {
          setStatus("Unpacking " + file);
          InputStream isSource = urlSource.openStream();
          FileOutputStream osDestination = new FileOutputStream(fDestination);
          IO.Pump(isSource, osDestination);
        }
      } // next script
      setPercentComplete(10);

      // install node modeules
      setStatus("npm install...");
      File npm = Execution.Which("npm");
      if (npm == null) throw new InvalidConfigurationException(
        this, "'npm' not found.\nPlease install node and npm.");
      File iloNasinDir = new File(getWorkingDirectory(), "ilo-nasin");
      Execution npmInstall = new Execution()
        .setExe(npm).arg("install")
        .setWorkingDirectory(iloNasinDir)
        .addStderrObserver​(error -> setStatus(error))
        .addStdoutObserver​(message -> setStatus(message));
      npmInstall.run();
      if (npmInstall.stderr().length() > 0) {
        setStatus("WARNING: " + npmInstall.stderr());
      }

      setPercentComplete(55);
            
      setStatus("npm install node...");
      npmInstall = new Execution()
        .setExe(npm).arg("install").arg("node")
        .setWorkingDirectory(iloNasinDir)
        .addStderrObserver​(error -> setStatus(error))
        .addStdoutObserver​(message -> setStatus(message));
      npmInstall.run();
      if (npmInstall.stderr().length() > 0) {
        setStatus("WARNING: " + npmInstall.stderr());
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
  public IloNasinTagger setTokenLayerId(String newTokenLayerId) {
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
  public IloNasinTagger setTokenExclusionPattern(String newTokenExclusionPattern) { tokenExclusionPattern = newTokenExclusionPattern; return this; }
  
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
  public IloNasinTagger setChunkLayerId(String newChunkLayerId) { chunkLayerId = newChunkLayerId; return this; }
  
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
  public IloNasinTagger setTranscriptLanguageLayerId(String newTranscriptLanguageLayerId) {
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
  public IloNasinTagger setPhraseLanguageLayerId(String newPhraseLanguageLayerId) {
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
  public IloNasinTagger setTargetLanguagePattern(String newTargetLanguagePattern) {
    if (newTargetLanguagePattern != null // empty string means null
        && newTargetLanguagePattern.trim().length() == 0) {
      newTargetLanguagePattern = null;
    }
    targetLanguagePattern = newTargetLanguagePattern;
    return this;
  }
  
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
  public IloNasinTagger setPosLayerId(String newPosLayerId) { posLayerId = newPosLayerId; return this; }
  
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
      
      chunkLayerId = schema.getUtteranceLayerId();
      
      // default transcript language layer
      Layer[] candidates = schema.getMatchingLayers(
        layer -> schema.getRoot().getId().equals(layer.getParentId())
        && layer.getAlignment() == 0 // transcript attribute
        && layer.getId().matches(".*lang.*")); // with 'lang' in the name
      if (candidates.length > 0) transcriptLanguageLayerId = candidates[0].getId();
      
      // default phrase language layer
      candidates = schema.getMatchingLayers(
        layer -> schema.getTurnLayerId() != null
        && schema.getTurnLayerId().equals(layer.getParentId()) // child of turn
        && layer.getId().matches(".*lang.*")); // with 'lang' in the name
      if (candidates.length > 0) phraseLanguageLayerId = candidates[0].getId();
      
      // default output layer
      candidates = schema.getMatchingLayers(
        layer -> schema.getWordLayerId() != null
        && schema.getWordLayerId().equals(layer.getParentId())
        && layer.getAlignment() == 0 // word tag
        && layer.getId().matches(".*(pos|part.*of.*speech).*"));
      if (candidates.length > 0) {
        posLayerId = candidates[0].getId();
      } else { // suggest adding a new one
        posLayerId = "pos";
      }
      
      // no exclusion pattern
      tokenExclusionPattern = "";
      
    } else {
      beanPropertiesFromQueryString(parameters);
    }
    
    if (schema.getLayer(tokenLayerId) == null)
      throw new InvalidConfigurationException(
        this, "Token layer not found: " + tokenLayerId);
    if (schema.getLayer(chunkLayerId) == null)
      throw new InvalidConfigurationException(
        this, "Chunk layer not found: " + chunkLayerId);
    if (transcriptLanguageLayerId != null
        && schema.getLayer(transcriptLanguageLayerId) == null) 
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
    
    // does the outputLayer need to be added to the schema?
    Layer posLayer = schema.getLayer(posLayerId);
    if (posLayer == null) {
      posLayer = new Layer(posLayerId)
        // no alignment or peers by default
        .setAlignment(Constants.ALIGNMENT_NONE)
        .setPeers(false).setPeersOverlap(false)
        .setParentIncludes(true)
        .setSaturated(true)
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
      // tolerate layers that are aligned or have peers, we might not be the only annotator
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
      posLayer.getValidLabels().put("noun","noun");
      posLayer.getValidLabels().put("iverb","iverb");
      posLayer.getValidLabels().put("tverb","tverb");
      posLayer.getValidLabels().put("modifier","modifier");
      posLayer.getValidLabels().put("particle","particle");
      posLayer.getValidLabels().put("preposition","preposition");
      posLayer.getValidLabels().put("preverb","preverb");
      posLayer.getValidLabels().put("interjection_head","interjection_head");
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
        // TODO whatever we need to run ilo-nasin
        File iloNasinDir = new File(getWorkingDirectory(), "ilo-nasin");
        File node = new File(
          new File(new File(new File(iloNasinDir, "node_modules"), "node"), "bin"),
          java.lang.System.getProperty("os.name").startsWith("Windows")?"node.exe":"node");
        if (!node.exists()) throw new TransformationException(
          this, "'"+node.getPath()+"' not found.\nPlease install node and npm.");
        String[] argv = { node.getPath(), "index.js" };
        try {
          process = Runtime.getRuntime().exec(argv, null, iloNasinDir);
          try (PrintWriter stdin = new PrintWriter(process.getOutputStream())) {
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
              
              if (tokens.length > 0) {
                
                String text = Arrays.stream(tokens)
                  .map(token -> token.getLabel())
                  .collect(Collectors.joining(" "));
                //setStatus("Tagging chunk "+chunk.getStart() + "-" + chunk.getEnd() + " : " + text);
                // tag utterance
                stdin.println(text);
                stdin.flush();
                // get JSON response - structured like:
                // {
                //   "words": [
                //     { "word": { "index": 0, "text": "ona" }, "tag": "noun" },
                //     ...
                //   ],
                //   "error": null
                // }
                JsonObject json = Json.createReader(
                  new InputStreamReader(process.getInputStream())).readObject();
                //setStatus("JSON: " + json);
                // check error
                if (json.containsKey("error") && !json.isNull("error")) {
                  setStatus("Could not parse \""+text+"\": " + json.getString("error"));
                  // TODO add annotation to 'error' layer
                } else { // parse successful
                  // Convert JSON array of word tokens into a list of Annotations
                  List<Annotation> iloNasinTokens = json.getJsonArray("words")
                    .stream()
                    .map(jsonValue -> {
                        JsonObject taggedToken = (JsonObject)jsonValue;
                        Annotation iloNasinAnnotation = new Annotation()
                          .setLabel(taggedToken.getJsonObject("word").getString("text"));
                        iloNasinAnnotation.put("tag", taggedToken.getString("tag"));
                        return iloNasinAnnotation;
                      })
                    .collect(Collectors.toList());
                  // map ilo-nasin tokens to chunk tokens
                  MinimumEditPath<Annotation> mp = new MinimumEditPath<Annotation>(
                    new Orthography2OrthographyComparator<Annotation>());
                  List<EditStep<Annotation>> mapping = mp.minimumEditPath(
                    iloNasinTokens, Arrays.asList(tokens));
                  // minimise deletes by joining insert followed by delete
                  mapping = mp.collapse(mapping);
                  for (EditStep<Annotation> step : mapping) {
                    Annotation iloNasinToken = step.getFrom();
                    Annotation originalToken = step.getTo();
                    if (iloNasinToken != null && originalToken != null) {
                      String tag = (String)iloNasinToken.get("tag");
                      //setStatus(originalToken.getLabel() + " : " + tag);
                      originalToken.createTag(posLayerId, tag);
                    } // mapped
                  } // next step
                } // parse successful
              } // there are tokens
            } // next chunk
          } catch(Exception exception) {
            if (process != null && process.isAlive()) {
              process.destroyForcibly();
            }
            throw new TransformationException(this, exception);
          } finally {
            process = null;
          }
        } catch(Exception exception) {
          throw new TransformationException(this, exception);
        }
      } // there are possibly tokens in the right language
      if (isCancelling()) {
        setStatus("Cancelled.");
      } else {
        setStatus("Finished " + graph.getId());
      }
      
      return graph;
    } finally {
      setRunning(false);
    }
  }
}
