//
// Copyright 2020-2025 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.ag.automation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Collection;
import java.util.TreeMap;
import java.util.Vector;
import java.util.stream.Collectors;
import nzilbb.ag.Annotation;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.GraphStore;
import nzilbb.ag.Layer;
import nzilbb.ag.PermissionException;
import nzilbb.ag.Schema;
import nzilbb.ag.StoreException;
import nzilbb.ag.TransformationException;
import nzilbb.ag.ql.QL;

/**
 * Base class for annotators that tag tokens based (almost)
 * exclusively on the labels from a specific input layer, not on context.
 * <p> This provides common implementation for taggers that perform
 * simple lexical lookups for tagging words with their pronunciations,
 * etc. and also those which generate pronunciations (or other tags)
 * based on spelling.
 * <p> The intention is that the derived class can provide a single
 * method ({@link #tagsFor(String)}) that maps an input label
 * (e.g. orthography) to one or more output labels
 * (e.g. pronunciations), and the rest of the processing
 * (identification of tokens, filtering by language, mass updates) is
 * handled by this base class in an efficient and standardized
 * way. Then this base class's implementations of
 * {@link GraphTransformer#transform(Graph)},
 * {@link #transformTranscripts(GraphStore,String)}, 
 * {@link #transformFragments(Stream,Consumer)}, and
 * {@link #tagAllInstances(GraphStore,String)} will handle the
 * annotation details.
 * <p> Implementations that require language filtering - i.e. only
 * tagging tokens in a specific language - need to ensure that
 * {@link #targetLanguagePattern} is set to identify which language,
 * along with {@link #transcriptLanguageLayerId} to specify a
 * transcript attribute that identifies the language of the transcript
 * as a whole, and {@link #phraseLanguageLayerId} to identify a
 * layer where phrase-specific language annotations might be present.
 * @author Robert Fromont robert@fromont.nz
 */
public abstract class LabelBasedTagger extends Annotator {
  
  /**
   * Determines what tag labels should apply on the tag layer for
   * tokens with the given label on the token layer.
   * This is the method that must be implemented by derived classes.
   * @param tokenLabel The label of the token(s) that must be tagged.
   * @return A list of tags, which may be empty.
   */
  public abstract Collection<String> tagsFor(String tokenLabel)
    throws DictionaryException;
  
  /**
   * A dictionary that might be used during calls to {@link
   * #tagsFor(String)}, which will be closed after tagging.
   * @see #getTaggingDictionary()
   * @see #setTaggingDictionary(Dictionary)
   */
  protected Dictionary taggingDictionary;
  /**
   * Getter for {@link #taggingDictionary}: A dictionary that might be
   * used during calls to {@link #tagsFor(String)}, which will be
   * closed after tagging. Subclasses can provide an implementation
   * for this that returns a dictionarty that the subclass can then
   * used during {@link #tagsFor(String)}.
   * @return A dictionary that might be used during calls to {@link #tagsFor(String)},
   * which will be closed after tagging.
   * @throws DictionaryException If the dictonary could not be instantiated.
   */
  public Dictionary getTaggingDictionary() throws DictionaryException { return taggingDictionary; }
  /**
   * Setter for {@link #taggingDictionary}: A dictionary that might be
   * used during calls to {@link #tagsFor(String)}, which will be
   * closed after tagging. 
   * @param newTaggingDictionary A dictionary that might be used
   * during calls to {@link #tagsFor(String)}, which will be closed after tagging.
   */
  public LabelBasedTagger setTaggingDictionary(Dictionary newTaggingDictionary) { taggingDictionary = newTaggingDictionary; return this; }
  
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
  public LabelBasedTagger setTokenLayerId(String newTokenLayerId) {
    tokenLayerId = newTokenLayerId; return this; }

  /**
   * ID of the layer that determines the language of the whole transcript.
   * @see #getTranscriptLanguageLayerId()
   * @see #setTranscriptLanguageLayerId(String)
   * @see #getTargetLanguagePattern()
   * @see #setTargetLanguagePattern(String)
   */
  protected String transcriptLanguageLayerId;
  /**
   * Getter for {@link #transcriptLanguageLayerId}: ID of the layer that determines the
   * language of the whole transcript. 
   * @return ID of the layer that determines the language of the whole transcript.
   * @see #getTargetLanguagePattern()
   * @see #setTargetLanguagePattern(String)
   */
  public String getTranscriptLanguageLayerId() { return transcriptLanguageLayerId; }
  /**
   * Setter for {@link #transcriptLanguageLayerId}: ID of the layer that determines the
   * language of the whole transcript. 
   * @param newTranscriptLanguageLayerId ID of the layer that determines the language of
   * the whole transcript. 
   * @see #getTargetLanguagePattern()
   * @see #setTargetLanguagePattern(String)
   */
  public LabelBasedTagger setTranscriptLanguageLayerId(String newTranscriptLanguageLayerId) {
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
   * @see #getTranscriptLanguageLayerId()
   * @see #setTranscriptLanguageLayerId(String)
   * @see #getTargetLanguagePattern()
   * @see #setTargetLanguagePattern(String)
   */
  protected String phraseLanguageLayerId;
  /**
   * Getter for {@link #phraseLanguageLayerId}: ID of the layer that determines the
   * language of individual phrases. 
   * @return ID of the layer that determines the language of individual phrases.
   * @see #getTranscriptLanguageLayerId()
   * @see #setTranscriptLanguageLayerId(String)
   * @see #getTargetLanguagePattern()
   * @see #setTargetLanguagePattern(String)
   */
  public String getPhraseLanguageLayerId() { return phraseLanguageLayerId; }
  /**
   * Setter for {@link #phraseLanguageLayerId}: ID of the layer that determines the
   * language of individual phrases. 
   * @param newPhraseLanguageLayerId ID of the layer that determines the language of
   * individual phrases. 
   * @see #getTranscriptLanguageLayerId()
   * @see #setTranscriptLanguageLayerId(String)
   * @see #getTargetLanguagePattern()
   * @see #setTargetLanguagePattern(String)
   */
  public LabelBasedTagger setPhraseLanguageLayerId(String newPhraseLanguageLayerId) {
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
   * @see #getPhraseLanguageLayerId()
   * @see #setPhraseLanguageLayerId(String)
   * @see #getTranscriptLanguageLayerId()
   * @see #setTranscriptLanguageLayerId(String)
   */
  protected String targetLanguagePattern;
  /**
   * Getter for {@link #targetLanguagePattern}: Regular expression for specifying which
   * language to tag the tokens of. 
   * @return Regular expression for specifying which language to tag the tokens of.
   * @see #getPhraseLanguageLayerId()
   * @see #setPhraseLanguageLayerId(String)
   * @see #getTranscriptLanguageLayerId()
   * @see #setTranscriptLanguageLayerId(String)
   */
  public String getTargetLanguagePattern() { return targetLanguagePattern; }
  /**
   * Setter for {@link #targetLanguagePattern}: Regular expression for specifying which
   * language to tag the tokens of. 
   * @param newTargetLanguagePattern Regular expression for specifying which language to
   * tag the tokens of. 
   * @see #getTranscriptLanguageLayerId()
   * @see #setTranscriptLanguageLayerId(String)
   * @see #getPhraseLanguageLayerId()
   * @see #setPhraseLanguageLayerId(String)
   */
  public LabelBasedTagger setTargetLanguagePattern(String newTargetLanguagePattern) {
    if (newTargetLanguagePattern != null // empty string means null
        && newTargetLanguagePattern.trim().length() == 0) {
      newTargetLanguagePattern = null;
    }
    targetLanguagePattern = newTargetLanguagePattern;
    return this;
  }

  /**
   * ID of the output layer.
   * @see #getTagLayerId()
   * @see #setTagLayerId(String)
   */
  protected String tagLayerId;
  /**
   * Getter for {@link #tagLayerId}: ID of the output layer.
   * @return ID of the output layer.
   */
  public String getTagLayerId() { return tagLayerId; }
  /**
   * Setter for {@link #tagLayerId}: ID of the output layer.
   * @param newTagLayerId ID of the output layer.
   */
  public LabelBasedTagger setTagLayerId(String newTagLayerId) {
    tagLayerId = newTagLayerId; return this; }  

  /**
   * In some cases, mass-update expressions used by
   * {@link #tagAllInstances(GraphStore,String)} and
   * {@link #transformTranscripts​(GraphStore,String)} need to use the
   * 'exact match' operator <tt>===</tt>. If this is the case, the subclass
   * should set this to <tt>true</tt> 
   * @see #getExactMatch()
   */
  protected Boolean exactMatch = Boolean.FALSE;

  // overrides for annotation...

  /**
   * Transforms the graph. In this case, the graph is simply summarized, by counting all
   * tokens of each word type, and printing out the result to stdout.
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
      Layer tagLayer = graph.getSchema().getLayer(tagLayerId);
      if (tagLayer == null) {
        throw new InvalidConfigurationException(
          this, "Invalid output tag layer: " + tagLayerId);
      }
         
      // what languages are in the transcript?
      boolean transcriptIsMainlyTargetLang = true;
      if (transcriptLanguageLayerId != null && targetLanguagePattern != null) {
        Annotation transcriptLanguage = graph.first(transcriptLanguageLayerId);
        if (transcriptLanguage != null) {
          if (!transcriptLanguage.getLabel().matches(targetLanguagePattern)) { // not TargetLang
            transcriptIsMainlyTargetLang = false;
          }
        }
      }
      boolean thereArePhraseTags = false;
      if (phraseLanguageLayerId != null) {
        if (graph.first(phraseLanguageLayerId) != null) {
          thereArePhraseTags = true;
        }
      }

      TreeMap<String,Vector<Annotation>> toAnnotate = new TreeMap<String,Vector<Annotation>>();
      // should we just tag everything?
      if (transcriptIsMainlyTargetLang && !thereArePhraseTags) {
        // process all tokens
        for (Annotation token : graph.all(tokenLayerId)) {
          // tag only tokens that are not already tagged
          if (token.first(tagLayerId) == null) { // not tagged yet
            registorForAnnotation(token, toAnnotate);
          } // not tagged yet
        } // next token
      } else if (transcriptIsMainlyTargetLang) {
        // process all but the phrase-tagged tokens
            
        // tag the exceptions
        for (Annotation phrase : graph.all(phraseLanguageLayerId)) {
          if (targetLanguagePattern != null
              && !phrase.getLabel().matches(targetLanguagePattern)) { // not TargetLang
            for (Annotation token : phrase.all(tokenLayerId)) {
              // mark the token as an exception
              token.put("@notTargetLang", Boolean.TRUE);
            } // next token in the phrase
          } // non-TargetLang phrase
        } // next phrase
            
        for (Annotation token : graph.all(tokenLayerId)) {
          if (token.containsKey("@notTargetLang")) {
            // while we're here, we remove the @notTargetLang mark
            token.remove("@notTargetLang");
          } else { // TargetLang, so tag it
            // tag only tokens that are not already tagged
            if (token.first(tagLayerId) == null) { // not tagged yet
            registorForAnnotation(token, toAnnotate);
            } // not tagged yet
          } // TargetLang, so tag it
        } // next token
      } else if (thereArePhraseTags) {
        // process only the tokens phrase-tagged as TargetLang
        for (Annotation phrase : graph.all(phraseLanguageLayerId)) {
          if (phrase.getLabel().matches(targetLanguagePattern)) {
            for (Annotation token : phrase.all(tokenLayerId)) {
              // tag only tokens that are not already tagged
              if (token.first(tagLayerId) == null) { // not tagged yet
                registorForAnnotation(token, toAnnotate);
              } // not tagged yet
            } // next token in the phrase
          } // TargetLang phrase
        } // next phrase
      } // thereArePhraseTags
         
      try {
        try (Dictionary dictionary = getTaggingDictionary()) {
          int t = 0;
          int typeCount = toAnnotate.size();
          setPercentComplete(0);
          for (String type : toAnnotate.keySet()) { // for each type
            if (isCancelling()) break;
            boolean found = false;
            for (String entry : tagsFor(type)) {
              if (entry.length() == 0) continue; // no blank labels
              
              if (!found) setStatus("Tagging: " + type); // (log this only once)
              found = true;
              for (Annotation token : toAnnotate.get(type)) {
                token.createTag(tagLayerId, entry)
                  .setConfidence(Constants.CONFIDENCE_AUTOMATIC);
              }
            } // next entry
            setPercentComplete(++t * 100 / typeCount);
            
          } // next type
          if (!isCancelling()) setPercentComplete(100);
        } finally { // close dictionary if any
          taggingDictionary = null; // ensure new calls to getTaggingDictionary create a new one
        }
      } catch (DictionaryException x) {
        throw new TransformationException(this, x);
      }
      return graph;
    } finally {
      setRunning(false);
    }
  }
  
  /**
   * Registers a token for annotation.
   * @param token
   * @param toAnnotate
   */
  private void registorForAnnotation(
    Annotation token, TreeMap<String,Vector<Annotation>> toAnnotate) {
    if (!toAnnotate.containsKey(token.getLabel())) {
      toAnnotate.put(token.getLabel(), new Vector<Annotation>());
    }
    toAnnotate.get(token.getLabel()).add(token);
  } // end of registorForAnnotation()

  /**
   * For subclasses that implement the {@link ImplementsDictionaries}
   * interface, this tags all instances of the given word in the given
   * graph store, using the dictionary specified by current task
   * configuration (i.e. the dictionary returned by {@link #getTaggingDictionary()}).
   * @param store The graph store.
   * @param tokenLabel The label of the token.
   * @return The number of tags created.
   * @throws DictionaryException, TransformationException, InvalidConfigurationException,
   * StoreException 
   */
  public int tagAllInstances(GraphStore store, String tokenLabel)
    throws DictionaryException, TransformationException, InvalidConfigurationException,
    StoreException {
    try (Dictionary dictionary = getTaggingDictionary()) {
      
      StringBuilder languageExpression = new StringBuilder();
      if (targetLanguagePattern != null
          && (phraseLanguageLayerId != null || transcriptLanguageLayerId != null)) {
        languageExpression.append(" && /").append(targetLanguagePattern).append("/.test(");
        if (phraseLanguageLayerId != null) {
          languageExpression.append("first('").append(QL.Esc(phraseLanguageLayerId))
            .append("').label");
          if (transcriptLanguageLayerId != null) {
            languageExpression.append(" ?? "); // add coalescing operator
          }
        }
        if (transcriptLanguageLayerId != null) {
          languageExpression.append("first('").append(QL.Esc(transcriptLanguageLayerId))
            .append("').label");
        }
        languageExpression.append(")");
      } // add language condition
      
      store.deleteMatchingAnnotations(
        "layerId = '"+QL.Esc(tagLayerId)+"'"
        +languageExpression
        +" && first('"+QL.Esc(tokenLayerId)+"').label "
        +(exactMatch?"===":"==") // === is slower, so we don't use it unless necessary
        +" '"+QL.Esc(tokenLabel)+"'");
      
      String tokenExpression = "layerId = '"+QL.Esc(tokenLayerId)+"'"
        +languageExpression
        +" && label "
        +(exactMatch?"===":"==") // === is slower, so we don't use it unless necessary
        +" '"+QL.Esc(tokenLabel)+"'";
      int count = 0;
      for (String tag : tagsFor(tokenLabel)) {
        if (tag.length() == 0) continue; // no blank labels
        store.tagMatchingAnnotations(
          tokenExpression, tagLayerId, tag, Constants.CONFIDENCE_AUTOMATIC);
        count++;
      } // next entry
      return count;
    } catch(PermissionException x) {
      throw new TransformationException(this, x);
    } finally {
      taggingDictionary = null; // ensure new calls to getTaggingDictionary create a new one
    }
  }
  
  /**
   * Transforms all graphs from the given graph store that match the given graph expression.
   * <p> This implementation uses
   * {@link GraphStoreQuery#aggregateMatchingAnnotations(String,String)}
   * and {@link GraphStore#tagMatchingAnnotations​(String,String,String,Integer)}
   * to optimize tagging transcripts en-masse.
   * @param store The graph to store.
   * @param expression An expression for identifying transcripts to update, or null to transform
   * all transcripts in the store.
   * @return The changes introduced by the tranformation.
   * @throws TransformationException If the transformation cannot be completed.
   */
  public void transformTranscripts​(GraphStore store, String expression)
    throws TransformationException, InvalidConfigurationException, StoreException,
    PermissionException {
    
    setRunning(true);
    try {
      setPercentComplete(0);
      Layer tokenLayer = schema.getLayer(tokenLayerId);
      if (tokenLayer == null) {
        throw new InvalidConfigurationException(
          this, "Invalid input token layer: " + tokenLayerId);
      }
      Layer tagLayer = schema.getLayer(tagLayerId);
      if (tagLayer == null) {
        throw new InvalidConfigurationException(
          this, "Invalid output tag layer: " + tagLayerId);
      }    
      
      StringBuilder labelExpression = new StringBuilder();
      labelExpression.append("layer.id == '").append(QL.Esc(tokenLayer.getId())).append("'");
      if (targetLanguagePattern != null
          && (phraseLanguageLayerId != null || transcriptLanguageLayerId != null)) {
        labelExpression.append(" && /").append(targetLanguagePattern).append("/.test(");
        if (phraseLanguageLayerId != null) {
          labelExpression.append("first('").append(QL.Esc(phraseLanguageLayerId))
            .append("').label");
          if (transcriptLanguageLayerId != null) {
            labelExpression.append(" ?? "); // add coalescing operator
          }
        }
        if (transcriptLanguageLayerId != null) {
          labelExpression.append("first('").append(QL.Esc(transcriptLanguageLayerId))
            .append("').label");
        }
        labelExpression.append(")");
      } // add language condition
      
      if (expression != null && expression.trim().length() > 0) {
        labelExpression.append(" && [");
        String[] ids = store.getMatchingTranscriptIds(expression);
        if (ids.length == 0) {
          setStatus("No matching transcripts");
          setPercentComplete(100);
          return;
        } else {
          labelExpression.append(
            Arrays.stream(ids)
            // quote and escape each ID
            .map(id->"'"+id.replace("'", "\\'")+"'")
            // make a comma-delimited list
            .collect(Collectors.joining(",")));
          labelExpression.append("].includes(graphId)");
        }
      }
      setStatus("Getting distinct token labels...");
      String[] distinctWords = store.aggregateMatchingAnnotations(
        exactMatch?"DISTINCT BINARY":"DISTINCT", labelExpression.toString());
      setStatus("There are "+distinctWords.length+" distinct token labels");
      int w = 0;
      try (Dictionary dictionary = getTaggingDictionary()) {
        // for each label
        for (String word : distinctWords) {
          setStatus(word+"...");
          if (isCancelling()) break;
          for (String tag : tagsFor(word)) {
            if (isCancelling()) break;
            if (tag.length() == 0) continue; // no blank labels
            StringBuilder tokenExpression = new StringBuilder(labelExpression);
            tokenExpression.append(" && label ")
              .append(exactMatch?"===":"==") // === is slower, so we don't use it unless necessary
              .append(" '").append(QL.Esc(word)).append("'");
            setStatus(word+" → "+tag);
            store.tagMatchingAnnotations(
              tokenExpression.toString(), tagLayerId, tag, Constants.CONFIDENCE_AUTOMATIC);
          }
        } // next entry
        setPercentComplete((++w * 100) / distinctWords.length);
      } // next word
      if (isCancelling()) {
        setStatus("Cancelled.");
      } else {
        setPercentComplete(100);
        setStatus("Finished.");
      }
    } catch(DictionaryException x) {
      setStatus(x.getMessage());
      throw new TransformationException(this, x);
    } finally {
      taggingDictionary = null; // ensure new calls to getTaggingDictionary create a new one
      setRunning(false);
    }
  }
  
} // end of class LabelBasedTagger
