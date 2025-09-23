//
// Copyright 2025 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.annotator.stanfordner;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.*;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.sequences.DocumentReaderAndWriter;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Vector;
import java.util.function.IntConsumer;
import java.util.regex.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import nzilbb.ag.*;
import nzilbb.ag.automation.AllowsManualAnnotations;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.editpath.*;
import nzilbb.encoding.comparator.Orthography2OrthographyComparator;
import nzilbb.util.IO;

/**
 * Annotator that tags Named Entities according to the 
 * <a href="https://nlp.stanford.edu/software/CRF-NER.html">
 *  Stanford Named Entity Recognizer (NER)</a>.
 */
@UsesFileSystem @AllowsManualAnnotations
public class StanfordNERecognizer extends Annotator {
  /** Get the minimum version of the nzilbb.ag API supported by the annotator.*/
  public String getMinimumApiVersion() { return "1.2.5"; }
  
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
   * Downloads and installs the Named Entity Recognizer.
   * <p> If there is no .zip file from which to unpack the NER, it's downloaded
   * from <a href="https://nlp.stanford.edu/software/stanford-ner-4.2.0.zip">
   * https://nlp.stanford.edu/software/stanford-ner-4.2.0.zip</a>.
   * @throws InvalidConfigurationException
   * @see #getConfig()
   * @see #beanPropertiesFromQueryString(String)
   */ 
  public void setConfig(String config) throws InvalidConfigurationException {
    setRunning(true);
    setPercentComplete(0);
    setStatus(""); // clear any residual status from the last run...
    
    try {
      // is there a zip file that's newer than the "classifiers" subdirectory?
      boolean downloadZip = true;
      File classifiers = new File(getWorkingDirectory(), "classifiers");   
      File[] zips = getWorkingDirectory().listFiles(new FilenameFilter() {
          public boolean accept(File dir, String name) {
            return name.endsWith(".zip");
          }
        });
      setStatus(
        "There " + (zips.length==1?"is one zip file":"are " + zips.length + " zip files."));
      if (zips == null || zips.length == 0) {
        // download from https://nlp.stanford.edu/software/stanford-ner-4.2.0.zip
        File zip = new File(getWorkingDirectory(), "stanford-ner-4.2.0.zip");
        System.out.println("starting download...");
        setStatus("Downloading https://nlp.stanford.edu/software/stanford-ner-4.2.0.zip");
        IO.SaveUrlToFile​(
          new URL("https://nlp.stanford.edu/software/stanford-ner-4.2.0.zip"),
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
          "There is no .zip file containing the Stanford NER implementation. Please try again.");
        throw new InvalidConfigurationException(
          this,
          "There is no .zip file containing the Stanford NER implementation. Please try again.");
      }
      setStatus("Most recent .zip file: " + newestZip.getName());
      // should we unzip the recognizer implementation?
      if (!classifiers.exists() || newestZip.lastModified() > classifiers.lastModified()) {
        String error = installClassifiers(newestZip);
        if (error != null) throw new InvalidConfigurationException(this, error);
      }
      
      if (!classifiers.exists()) {
        setStatus("There are no classifiers. Please try again.");
        throw new InvalidConfigurationException(
          this, "There are no classifiers. Please try again.");
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
   * Takes a file to be used instead of the built-in copy of stanford-ner-n.n.n.zip
   * @param file The Stanford NER implementation file.
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
  } // end of uploadZip
  
  /**
   * Takes a file (.zip, .jar, or .crf.ser.gz) that contains alternative classifiers,
   * and unpacks/installs them.
   * @param file The file containing classifiers.
   * @return null if upload was successful, an error message otherwise.
   */
  public String installClassifiers(File file) {
    if (!file.getName().endsWith(".jar") && !file.getName().endsWith(".zip")
        && !file.getName().endsWith(".crf.ser.gz")) {
      return file.getName() + " is not a .jar, .zip, or .crf.ser.gz file.";
    }

    Vector<NamedStream> classifierStreams = new Vector<NamedStream>();
    if (file.getName().endsWith(".crf.ser.gz")) { // a single unzipped classifier
      try { classifierStreams.add(new NamedStream(file)); } catch(Exception exception) {}
    } else { // zip/jar file
      // look for .crf.ser.gz files in the file
      try {
        ZipFile zip = new ZipFile(file);
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement();
          if (entry.getName().endsWith(".crf.ser.gz")) {
            String[] path = entry.getName().split("/");
            classifierStreams.add(new NamedStream(
                                    zip.getInputStream(entry), path[path.length-1]));
          } // classifier
        } // next entry
      } catch(Exception exception) {
        return "Could not parse " + file.getName();
      }
    } // zip/jar file
    if (classifierStreams.size() == 0) {
      return "There were no classifiers in " + file.getName();
    }
    
    // install them
    File classifiers = new File(getWorkingDirectory(), "classifiers");
    if (!classifiers.exists()) classifiers.mkdir();
    for (NamedStream stream : classifierStreams) {
      try {
        IO.SaveInputStreamToFile​(stream.getStream(), new File(classifiers, stream.getName()));
      } catch(IOException exception) {
        // ensure all streams are closed
        for (NamedStream s : classifierStreams) {
          try { s.getStream().close(); } catch (Exception x) {}
        }
        return "Could not copy " + stream.getName() + " from " + file.getName();
      }
    }
    file.delete();
    return null;
  } // end of uploadClassifiers
  
  /**
   * Lists the classifier files that are available for use.
   * @return A list of file names that can be selected.
   */
  public List<String> availableClassifiers() {
    File classifiers = new File(getWorkingDirectory(), "classifiers");
    if (!classifiers.exists()) return new Vector<String>();
    return Arrays.asList(
      classifiers.list(new FilenameFilter() {
          public boolean accept(File dir, String name) {
            return name.endsWith(".crf.ser.gz");
          }
        }));
  } // end of availableClassifiers()
  
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
  public StanfordNERecognizer setTokenLayerId(String newTokenLayerId) {
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
  public StanfordNERecognizer setTokenExclusionPattern(String newTokenExclusionPattern) { tokenExclusionPattern = newTokenExclusionPattern; return this; }
  
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
  public StanfordNERecognizer setChunkLayerId(String newChunkLayerId) { chunkLayerId = newChunkLayerId; return this; }
  
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
  public StanfordNERecognizer setTranscriptLanguageLayerId(String newTranscriptLanguageLayerId) {
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
  public StanfordNERecognizer setPhraseLanguageLayerId(String newPhraseLanguageLayerId) {
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
  public StanfordNERecognizer setTargetLanguagePattern(String newTargetLanguagePattern) {
    if (newTargetLanguagePattern != null // empty string means null
        && newTargetLanguagePattern.trim().length() == 0) {
      newTargetLanguagePattern = null;
    }
    targetLanguagePattern = newTargetLanguagePattern;
    return this;
  }
  
  /**
   * Classifier to use for recognition.
   * @see #getClassifier()
   * @see #setClassifier(String)
   */
  protected String classifier;
  /**
   * Getter for {@link #classifier}: Classifier to use for recognition.
   * @return Classifier to use for recognition.
   */
  public String getClassifier() { return classifier; }
  /**
   * Setter for {@link #classifier}: Classifier to use for recognition.
   * @param newClassifier Classifier to use for recognition.
   */
  public StanfordNERecognizer setClassifier(String newClassifier) { classifier = newClassifier; return this; }
  
  /**
   * ID of the output layer.
   * @see #getEntityLayerId()
   * @see #setEntityLayerId(String)
   */
  protected String entityLayerId;
  /**
   * Getter for {@link #entityLayerId}: ID of the output layer.
   * @return ID of the output layer.
   */
  public String getEntityLayerId() { return entityLayerId; }
  /**
   * Setter for {@link #entityLayerId}: ID of the output layer.
   * @param newEntityLayerId ID of the output layer.
   */
  public StanfordNERecognizer setEntityLayerId(String newEntityLayerId) { entityLayerId = newEntityLayerId; return this; }
  
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
        && layer.getId().matches(".*(name|entity|place|person).*"));
      if (candidates.length > 0) {
        entityLayerId = candidates[0].getId();
      } else { // suggest adding a new one
        entityLayerId = "namedEntity";
      }
      
      // default model
      classifier = "english.all.3class.distsim.crf.ser.gz";
      
      // no exclusion pattern
      tokenExclusionPattern = "";
      
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

    // valid classifier
    if (classifier == null || classifier.trim().length() == 0)
      throw new InvalidConfigurationException(this, "No classifier selected.");
    File classifierFile = new File(
      new File(getWorkingDirectory(), "classifiers"), classifier);
    if (!classifierFile.exists())
      throw new InvalidConfigurationException(this, "Classifier not found: " + classifier);
    
    // does the outputLayer need to be added to the schema?
    Layer entityLayer = schema.getLayer(entityLayerId);
    if (entityLayer == null) {
      entityLayer = new Layer(entityLayerId)
        .setAlignment(Constants.ALIGNMENT_NONE)
        .setPeers(false).setPeersOverlap(false)
        .setParentIncludes(true)
        .setParentId(schema.getWordLayerId());
      schema.addLayer(entityLayer);
    } else {
      if (entityLayerId.equals(tokenLayerId)
          || entityLayerId.equals(transcriptLanguageLayerId)
          || entityLayerId.equals(phraseLanguageLayerId)) {
        throw new InvalidConfigurationException(
          this, "Invalid entity layer: " + entityLayerId);
      }
      // ensure layer properties are valid
      if (entityLayer.getType() != Constants.TYPE_STRING)
        entityLayer.setType(Constants.TYPE_STRING);
      if (entityLayer.getAlignment() != Constants.ALIGNMENT_NONE)
        entityLayer.setAlignment(Constants.ALIGNMENT_NONE);
      if (entityLayer.getPeers())
        entityLayer.setPeers(false);
      if (!entityLayer.getParentIncludes())
        entityLayer.setParentIncludes(true);
      if (schema.getWordLayerId() != null // word child layer
          && schema.getWordLayerId().equals(entityLayer.getParentId())) {
        if (entityLayer.getPeersOverlap())
          entityLayer.setPeersOverlap(false);
        if (!entityLayer.getSaturated())
          entityLayer.setSaturated(true);
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
    if (entityLayerId == null)
      throw new InvalidConfigurationException(this, "POS layer not set.");
    return new String[] { entityLayerId };
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
      Layer entityLayer = graph.getSchema().getLayer(entityLayerId);
      if (entityLayer == null) {
        throw new InvalidConfigurationException(this, "Invalid output ENTITY layer: " + entityLayerId);
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

        try {
          AbstractSequenceClassifier<CoreLabel> nerClassifier = CRFClassifier.getClassifier(
            new File(new File(getWorkingDirectory(), "classifiers"), classifier));
          DocumentReaderAndWriter<CoreLabel> readerAndWriter =
            nerClassifier.makePlainTextReaderAndWriter(); 
          
          // handle language selection...
          
          for (Annotation chunk : graph.all(chunkLayerId)) {
            if (isCancelling()) break;
            
            List<Annotation> tokens = Arrays.asList(chunk.all(tokenLayerId));
            
            // delete all existing tags before filtering out by pattern
            for (Annotation t : tokens) {            
              for (Annotation e: t.all(entityLayerId)) {
                // don't delete manual annotations
                if (Optional.ofNullable(e.getConfidence())
                    .orElse(Constants.CONFIDENCE_AUTOMATIC)
                    <= Constants.CONFIDENCE_AUTOMATIC) {
                  e.destroy();
                }
              } // next entity tag
            } // next token
            
            if (tokenExclusionPattern.length() > 0) {
              final Pattern exclude = Pattern.compile(tokenExclusionPattern);
              tokens = tokens.stream()
                .filter(t->!exclude.matcher(t.getLabel()).matches())
                .collect(Collectors.toList());
            }
            
            if (!transcriptIsMainlyTargetLang) { // transcript is wrong language
              // filter out tokens that aren't phrase-tagged in the target language
              final Pattern targetLanguage = Pattern.compile(targetLanguagePattern);
              tokens = tokens.stream().
                filter((token) -> {
                    Annotation phraseLanguage = token.first(phraseLanguageLayerId);
                    if (phraseLanguage == null) return false; // not tagged with a language
                    return targetLanguage.matcher(phraseLanguage.getLabel()).matches();
                  })
                .collect(Collectors.toList());
              
            } else if (thereArePhraseTags // there are phrase-based language tags
                       && targetLanguagePattern != null) { // and we care about language
              // filter out tokens that aren't phrase-tagged in another language
              final Pattern targetLanguage = Pattern.compile(targetLanguagePattern);
              tokens = tokens.stream()
                .filter((token) -> {
                    Annotation phraseLanguage = token.first(phraseLanguageLayerId);
                    if (phraseLanguage == null) return true; // not tagged with a language
                    return targetLanguage.matcher(phraseLanguage.getLabel()).matches();
                  })
                .collect(Collectors.toList());
            }
            
            setStatus("Tagging chunk "+chunk.getStart() + "-" + chunk.getEnd());
            if (tokens.size() > 0) {
              
              // if tokens contain spaces, these are eliminated by the parser
              // so create a space-stripped version of the label
              for (Annotation t : tokens) {
                t.put("@untagged", t.getLabel().replaceAll("\\s",""));
              } // next token
              
              String text = tokens.stream()
                .map(token -> token.getLabel())
                .collect(Collectors.joining(" "));
              // setStatus("text: " + text);
              
              List<List<CoreLabel>> sentences =
                nerClassifier.classifyRaw(text, readerAndWriter);
              // convert to annotations
              Vector<Annotation> classifiedTokens = new Vector<Annotation>();
              for (List<CoreLabel> sentence : sentences) {
                for (CoreLabel coreLabel : sentence) {
                  Annotation classified = new Annotation().setLabel(coreLabel.word());
                  classified.put("@coreLabel", coreLabel);
                  classifiedTokens.add(classified);
                } // next token
              } // next sentence

              // map original tokens to classified tokens
              MinimumEditPath<Annotation> mp = new MinimumEditPath<Annotation>(
                new Orthography2OrthographyComparator<Annotation>());
              List<EditStep<Annotation>> editPath
                = mp.minimumEditPath(tokens, classifiedTokens);
              // map one-original-to-many-classifier-tokens
              // because it may have tokensized the input into smaller parts
              Map<Annotation,List<Annotation>> map = mp.mapOneToMany(editPath);

              // now tag originals
              for (Annotation originalToken : map.keySet()) {
                for (Annotation classifierToken : map.get(originalToken)) {
                  CoreLabel label = (CoreLabel)classifierToken.get("@coreLabel");
                  String entityLabel = label.get(CoreAnnotations.AnswerAnnotation.class);
                  if (entityLabel != null && !entityLabel.equals("O") // is tagged an entity
                      // ... and is not already manually tagged
                      && originalToken.first(entityLayerId) == null) {
                    setStatus(originalToken + " → " + entityLabel);
                    // there shouldn't be more than one
                    graph.createTag(
                      originalToken, entityLayerId, entityLabel)
                      .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
                  }
                } // next classifier Token
              } // next originalToken
            } // there are tokens
          } // next chunk
        } catch (ClassNotFoundException x) {
          setStatus("Could not instantiate classifier " + classifier);
          throw new TransformationException(
            this, "Could not instantiate classifier " + classifier);
        } catch (IOException x) {
          setStatus("Could not load classifier " + classifier);
          throw new TransformationException(
            this, "Could not load classifier " + classifier);
        }
      } // there are possibly tokens in the right language
      
      return graph;
    } finally {
      setRunning(false);
    }
  }
}
