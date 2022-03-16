//
// Copyright 2020-2022 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.annotator.javascript;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.script.*;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.util.IO;

/**
 * Annotator that executes a JavaScript script on each transcript.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class JavascriptAnnotator extends Annotator {
  /** Get the minimum version of the nzilbb.ag API supported by the serializer.*/
  public String getMinimumApiVersion() { return "1.0.6"; }

  /**
   * The Javascript script engine.
   */
  protected ScriptEngine engine;
   
  /**
   * Source layer ID, in the case of label mapping.
   * @see #getSourceLayerId()
   * @see #setSourceLayerId(String)
   */
  protected String sourceLayerId;
  /**
   * Getter for {@link #sourceLayerId}: Source layer ID, in the case of label mapping.
   * @return Source layer ID, in the case of label mapping.
   */
  public String getSourceLayerId() { return sourceLayerId; }
  /**
   * Setter for {@link #sourceLayerId}: Source layer ID, in the case of label mapping.
   * @param newSourceLayerId Source layer ID, in the case of label mapping.
   */
  public JavascriptAnnotator setSourceLayerId(String newSourceLayerId) { sourceLayerId = newSourceLayerId; return this; }

  /**
   * Destination layer ID, in the case of label mapping.
   * @see #getDestinationLayerId()
   * @see #setDestinationLayerId(String)
   */
  protected String destinationLayerId;
  /**
   * Getter for {@link #destinationLayerId}: Destination layer ID, in the case of label mapping.
   * @return Destination layer ID, in the case of label mapping.
   */
  public String getDestinationLayerId() { return destinationLayerId; }
  /**
   * Setter for {@link #destinationLayerId}: Destination layer ID, in the case of label mapping.
   * @param newDestinationLayerId Destination layer ID, in the case of label mapping.
   */
  public JavascriptAnnotator setDestinationLayerId(String newDestinationLayerId) { destinationLayerId = newDestinationLayerId; return this; }

  /**
   * JavaScript
   * @see #getScript()
   * @see #setScript(String)
   */
  protected String script;
  /**
   * Getter for {@link #script}: JavaScript
   * @return JavaScript
   */
  public String getScript() { return script; }
  /**
   * Setter for {@link #script}: JavaScript
   * @param newScript JavaScript
   */
  public JavascriptAnnotator setScript(String newScript) { script = newScript; return this; }

  /**
   * Wether it's a function that defines a 1:1 mapping of labels (true) or an arbitrary
   * script (false). 
   * @see #getLabelMapping()
   * @see #setLabelMapping(boolean)
   */
  protected boolean labelMapping = false;
  /**
   * Getter for {@link #labelMapping}: Wether it's a function that defines a 1:1 mapping
   * of labels (true) or an arbitrary script (false). 
   * @return Wether it's a function that defines a 1:1 mapping of labels (true) or an
   * arbitrary script (false). 
   */
  public boolean getLabelMapping() { return labelMapping; }
  /**
   * Setter for {@link #labelMapping}: Wether it's a function that defines a 1:1 mapping
   * of labels (true) or an arbitrary script (false). 
   * @param newLabelMapping Wether it's a function that defines a 1:1 mapping of labels
   * (true) or an arbitrary script (false). 
   */
  public JavascriptAnnotator setLabelMapping(boolean newLabelMapping) { labelMapping = newLabelMapping; return this; }
   
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
  public JavascriptAnnotator setTranscriptLanguageLayerId(String newTranscriptLanguageLayerId) {
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
  public JavascriptAnnotator setPhraseLanguageLayerId(String newPhraseLanguageLayerId) {
    if (newPhraseLanguageLayerId != null // empty string means null
        && newPhraseLanguageLayerId.trim().length() == 0) {
      newPhraseLanguageLayerId = null;
    }
    phraseLanguageLayerId = newPhraseLanguageLayerId;
    return this;
  }

  /**
   * Regular expression for the target language (ISO code), if any.
   * @see #getLanguage()
   * @see #setLanguage(String)
   */
  protected String language;
  /**
   * Getter for {@link #language}: Regular expression for the target language (ISO code), if any.
   * @return Regular expression for the target language (ISO code), if any.
   */
  public String getLanguage() { return language; }
  /**
   * Setter for {@link #language}: Regular expression for the target language (ISO code), if any.
   * @param newLanguage Regular expression for the target language (ISO code), if any.
   */
  public JavascriptAnnotator setLanguage(String newLanguage) { language = newLanguage; return this; }

  /**
   * Default constructor.
   */
  public JavascriptAnnotator() {
    ScriptEngineManager manager = new ScriptEngineManager();
    engine = manager.getEngineByExtension("js");
  } // end of constructor
   
  /**
   * Creates a layer for output.
   * @param layerId
   * @param parentId
   * @param alignment
   * @return The new schema including the layer.
   */
  public Schema newLayer(String layerId, String parentId, int alignment)
    throws InvalidConfigurationException {
    if (schema.getLayer(layerId) != null) {
      throw new InvalidConfigurationException(this, "Layer already exists: " + layerId);
    }
    schema.addLayer(
      new Layer(layerId)
      .setAlignment(alignment)
      .setParentId(parentId));
    return schema;
  } // end of newLayer()

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
      
    if (parameters == null) { // there is no possible default parameter
      throw new InvalidConfigurationException(this, "Parameters not set.");         
    }

    // parse JSON parameters...

    // set basic attributes
    JsonObject json = beanPropertiesFromJSON(parameters);

    // validation...
      
    // there must be at least one source layer
    String[] requiredLayers = getRequiredLayers();
    if (requiredLayers.length == 0) {
      throw new InvalidConfigurationException(this, "There are no source layers specified.");
    }
    // there must be at least one destination layer
    String[] outputLayers = getOutputLayers();
    if (getLabelMapping()) {
      // if the destination layer doesn't exist yet, create it
      Layer sourceLayer = schema.getLayer(sourceLayerId);
      if (sourceLayer == null) {
        throw new InvalidConfigurationException(
          this, "Invalid source layer: " + sourceLayerId);
      }
      Layer destinationLayer = schema.getLayer(destinationLayerId);
      if (destinationLayer == null) {
        if (sourceLayerId.equals(schema.getRoot().getId())
            || sourceLayerId.equals(schema.getWordLayerId())
            || sourceLayerId.equals(schema.getTurnLayerId())
            || sourceLayerId.equals(schema.getEpisodeLayerId())
            || sourceLayerId.equals(schema.getCorpusLayerId())
            || sourceLayerId.equals(schema.getParticipantLayerId())) {
          // 'special' layer, so the destination should be a child
          schema.addLayer(
            new Layer(destinationLayerId)
            .setAlignment(Constants.ALIGNMENT_NONE)
            .setPeers(false) // there will be one per parent
            .setParentId(sourceLayerId)
            .setType(Constants.TYPE_STRING));
        } else { // not a 'special' layer
          // the destination should shar the source layer's parent
          schema.addLayer(
            new Layer(destinationLayerId)
            .setAlignment(sourceLayer.getAlignment())
            .setPeers(sourceLayer.getPeers())
            .setParentId(sourceLayer.getParentId())
            .setType(Constants.TYPE_STRING));
        } // not a 'special' layer
      } // destination layer needs to be created
      // source and destination can't be the same
      if (sourceLayerId.equals(destinationLayerId)) {
        throw new InvalidConfigurationException(
          this, "Source and destination layers cannot be the dame: " + sourceLayerId);
      }

      // if transcript language layer is set, it must exist
      if (transcriptLanguageLayerId != null
          && schema.getLayer(transcriptLanguageLayerId) == null) {
        throw new InvalidConfigurationException(
          this, "Invalid transcript language layer: " + transcriptLanguageLayerId);
      }
      // if phrase language layer is set, it must exist
      if (phraseLanguageLayerId != null && schema.getLayer(phraseLanguageLayerId) == null) {
        throw new InvalidConfigurationException(
          this, "Invalid phrase language layer: " + phraseLanguageLayerId);
      }
      // if language setting is specified, it must be a valid regular expression
      if (language != null && language.length() > 0) {
        try {
          Pattern.compile(language);            
        } catch(PatternSyntaxException exception) {
          throw new InvalidConfigurationException(
            this, "Invalid language pattern \""+language+"\": " + exception.getMessage());
        }
      }
    } else { // full script
      // ensure all input layers exist
      for (String layerId : requiredLayers) {
        if (schema.getLayer(layerId) == null) {
          throw new InvalidConfigurationException(
            this, "Invalid required layer: " + layerId);
        }
      }
      // ensure all output layers exist
      for (String layerId : outputLayers) {
        if (schema.getLayer(layerId) == null) {
          schema.addLayer(
            new Layer(layerId)
            .setAlignment(Constants.ALIGNMENT_INTERVAL)
            .setPeers(true)
            .setParentId(schema.getRoot().getId())
            .setType(Constants.TYPE_STRING));
        }
      }
    } // full script
  }

  /**
   * Determines which layers the annotator requires in order to annotate a graph.
   * @return A list of layer IDs. In this case, the annotator only requires the schema's
   * word layer.
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public String[] getRequiredLayers() throws InvalidConfigurationException {
    if (schema == null)
      throw new InvalidConfigurationException(this, "Schema is not set.");
    HashSet<String> requiredLayers = new HashSet<String>();
    if (getLabelMapping()) {
      if (sourceLayerId == null)
        throw new InvalidConfigurationException(this, "No source layer is specified.");
      requiredLayers.add(sourceLayerId);
      // language layers
      if (transcriptLanguageLayerId != null) requiredLayers.add(transcriptLanguageLayerId);  
      if (phraseLanguageLayerId != null) requiredLayers.add(phraseLanguageLayerId);
    } else { // full script
      // look for patterns that access layers
      String[] patterns = { // https://xkcd.com/1421/
        "\\.list\\(\"([^\"]+)\"\\)",       "\\.list\\('([^']+)'\\)",
        "\\.all\\(\"([^\"]+)\"\\)",        "\\.all\\('([^']+)'\\)",
        "\\.my\\(\"([^\"]+)\"\\)",         "\\.my\\('([^']+)'\\)",
        "\\.first\\(\"([^\"]+)\"\\)",      "\\.first\\('([^']+)'\\)",
        "\\.last\\(\"([^\"]+)\"\\)",       "\\.last\\('([^']+)'\\)",
        "\\.getAnnotations\\(\"([^\"]+)\"\\)"
        ,                                  "\\.getAnnotations\\('([^']+)'\\)",
        "\\.annotations\\(\"([^\"]+)\"\\)","\\.annotations\\('([^']+)'\\)",
        "\\.includingAnnotationsOn\\(\"([^\"]+)\"\\)"
        ,                                  "\\.includingAnnotationsOn\\('([^']+)'\\)",
        "\\.includedAnnotationsOn\\(\"([^\"]+)\"\\)"
        ,                                  "\\.includedAnnotationsOn\\('([^']+)'\\)",
        "\\.midpointIncludingAnnotationsOn\\(\"([^\"]+)\"\\)"
        ,                                  "\\.midpointIncludingAnnotationsOn\\('([^']+)'\\)",
        "\\.tagsOn\\(\"([^\"]+)\"\\)",     "\\.tagsOn\\('([^']+)'\\)",
        "\\.getAncestor\\(\"([^\"]+)\"\\)","\\.getAncestor\\('([^']+)'\\)",
        "\\.overlappingAnnotations\\([^)]+\"([^\"]+)\"\\)"
        ,                                  "\\.overlappingAnnotations\\([^)]+'([^']+)'\\)"
      };
      for (String pattern : patterns) {
        Matcher matcher = Pattern.compile(pattern).matcher(script);
        while (matcher.find()) {
          requiredLayers.add(matcher.group(1));
        } // next match
      } // next pattern
    }
    return requiredLayers.toArray(new String[0]);
  }

  /**
   * Determines which layers the annotator will create/update/delete annotations on.
   * @return A list of layer IDs. In this case, the annotator has no task web-app for
   * specifying an output layer, and doesn't update any layers, so this method returns an
   * empty array.
   * @throws InvalidConfigurationException If {@link #setTaskParameters(String)} or 
   * {@link #setSchema(Schema)} have not yet been called.
   */
  public String[] getOutputLayers() throws InvalidConfigurationException {
    HashSet<String> outputLayers = new HashSet<String>();
    if (getLabelMapping()) {
      if (destinationLayerId == null)
        throw new InvalidConfigurationException(this, "No destination layer is specified.");
      outputLayers.add(destinationLayerId);
    } else { // full script
      // look for patterns that annotate layers
      String[] patterns = { // https://xkcd.com/1421/
        // Annotation.createTag(layerId, label)
        "\\.createTag\\(\"([^\"]+)\",[^)]+\\)", 
        "\\.createTag\\('([^']+)',[^)]+\\)",
        // Graph.createTag(annotation, layerId, label)
        "transcript\\.createTag\\([^,]+,\\s*\"([^\"]+)\",[^)]+\\)", 
        "transcript\\.createTag\\([^,]+,\\s*'([^']+)',[^)]+\\)",
        // Graph.addTag(annotation, layerId, label)
        "\\.addTag\\([^,]+,\\s*\"([^\"]+)\",[^)]+\\)",
        "\\.addTag\\([^,]+,\\s*'([^']+)',[^)]+\\)",
        // Graph.createSpan(from, to, layerId, label[, parent])
        "\\.createSpan\\([^)\"]+,\\s*\"([^\"]+)\"[^)]*\\)", 
        "\\.createSpan\\([^)']+,\\s*'([^']+)'[^)]*\\)", 
        // Graph.addSpan(from, to, layerId, label[, parent])
        "\\.addSpan\\([^)\"]+,\\s*\"([^\"]+)\"[^)]*\\)", 
        "\\.addSpan\\([^)']+,\\s*'([^']+)'[^)]*\\)", 
        // Graph.createAnnotation(from, to, layerId, label[, parent])
        "\\.createAnnotation\\([^\"]+,\\s*\"([^\"]+)\"[^)]*\\)", 
        "\\.createAnnotation\\([^']+,\\s*'([^']+)'[^)]*\\)", 
        // Graph.addAnnotation(from, to, layerId, label[, parent])
        "\\.addAnnotation\\([^\"]+,\\s*\"([^\"]+)\"[^)]*\\)", 
        "\\.addAnnotation\\([^']+,\\s*'([^']+)'[^)]*\\)", 
      };
      for (String pattern : patterns) {
        Matcher matcher = Pattern.compile(pattern).matcher(script);
        while (matcher.find()) {
          outputLayers.add(matcher.group(1));
        } // next match
      } // next pattern
    }
    return outputLayers.toArray(new String[0]);
  }
  
  /**
   * Determines whether the user has requested that processing be cancelled.
   * <p> This methos is implemented primarily to make it available to scripts, which
   * should be able to include lines like <code>if (annotator.cancelling) break;</code>
   * @return true if {@link #cancel()} has been called, false otherwise.
   */
  public boolean getCancelling() {
    return isCancelling();
  } // end of isCancelling()
   
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
      if (labelMapping) {
        mapLabels(graph);
      } else {
        executeScript(graph);
      }
    } finally {
      setRunning(false);
    }
    return graph;
  }
   
  /**
   * Annotate tokens by computing destination labels from source labels.
   * @param graph
   * @throws InvalidConfigurationException
   */
  protected void mapLabels(Graph graph) throws InvalidConfigurationException {
    // TODO push this into Annotator as a method that takes a UnaryOperator<Annotation>
    // what languages are in the transcript?
    boolean transcriptIsMainlyTarget = true;
    if (language != null && language.length() > 0) {
      if (transcriptLanguageLayerId != null) {
        Annotation transcriptLanguage = graph.first(transcriptLanguageLayerId);
        if (transcriptLanguage != null) {
          if (!transcriptLanguage.getLabel().matches(language)) {
            // not the target language
            transcriptIsMainlyTarget = false;
          }
        }
      }
    }
    boolean thereArePhraseTags = false;
    if (language != null && language.length() > 0) {
      if (phraseLanguageLayerId != null) {
        if (graph.first(phraseLanguageLayerId) != null) {
          thereArePhraseTags = true;
        }
      }
    }

    try {
      // should we just tag everything?
      if (transcriptIsMainlyTarget && !thereArePhraseTags) {
        // process all tokens
        for (Annotation token : graph.all(sourceLayerId)) {
          mapLabel(token);
        } // next token
      } else if (transcriptIsMainlyTarget) {
        // process all but the phrase-tagged tokens
            
        // tag the exceptions
        for (Annotation phrase : graph.all(phraseLanguageLayerId)) {
          if (!phrase.getLabel().matches(language)) { // not target
            for (Annotation token : phrase.all(sourceLayerId)) {
              // mark the token as an exception
              token.put("@notTarget", Boolean.TRUE);
            } // next token in the phrase
          } // non-Spanish phrase
        } // next phrase
            
        for (Annotation token : graph.all(sourceLayerId)) {
          if (token.containsKey("@notTarget")) {
            // while we're here, we remove the @notSpanish mark
            token.remove("@notTarget");
          } else { // Target language, so tag it
            mapLabel(token);
          } // Spanish, so tag it
        } // next token
      } else if (thereArePhraseTags) {
        // process only the tokens phrase-tagged as Spanish
        for (Annotation phrase : graph.all(phraseLanguageLayerId)) {
          if (phrase.getLabel().matches(language)) {
            for (Annotation token : phrase.all(sourceLayerId)) {
              mapLabel(token);
            } // next token in the phrase
          } // Spanish phrase
        } // next phrase
      } // thereArePhraseTags
    } catch (ScriptException exception) {
      throw new InvalidConfigurationException(this, exception);
    }
  } // end of mapLabels()
   
  /**
   * Annotate the given token if appropriate.
   * @param token The token to match against patterns.
   * @return The new annotation, if any.
   * @throws ScriptException When there's an error with the script.
   */
  public Annotation mapLabel(Annotation token) throws ScriptException {
    // compute the resulting label
    String escapedInputLabel = token.getLabel()
      .replaceAll("\\\\","\\\\\\\\") // escape backslashes
      .replaceAll("\\\"","\\\\\""); // escape quotes
    String js = "\""+escapedInputLabel+"\"." + script;
    try {
      String tagLabel = engine.eval(js).toString();
         
      Annotation existingTag = token.first(destinationLayerId);
      if (existingTag != null)  { // existing tag to update
        existingTag.setLabel(tagLabel);
      } else { // not tagged yet
        existingTag = token.createTag(destinationLayerId, tagLabel);
      }
      existingTag.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
      return existingTag;
    } catch(ScriptException exception) {
      System.out.println("JavascriptDataGenerator ERROR: " + exception);
      System.out.println("JavascriptDataGenerator SCRIPT: " + script);
      throw exception;
    }
  } // end of getLabel()

  /**
   * Annotate the graph by executing the script.
   * @param graph
   * @throws InvalidConfigurationException
   */
  protected void executeScript(Graph graph) throws InvalidConfigurationException {
      
    // create script engine
    ScriptEngineFactory factory = engine.getFactory();
    setStatus(
      factory.getEngineName() + " ("+factory.getEngineVersion()+") for " 
      + factory.getLanguageName() + " ("+factory.getLanguageVersion()+")");
    engine.put(
      ScriptEngine.FILENAME, IO.SafeFileNameUrl(Arrays.asList(getOutputLayers()).toString()) + ".js");
    ScriptContext context = engine.getContext();

    context.setAttribute("annotator", this, ScriptContext.ENGINE_SCOPE);
    context.setAttribute("transcript", graph, ScriptContext.ENGINE_SCOPE);

    try {
      // provide logging function
      engine.eval(
        "function log(message) {"
        +" annotator.setStatus(\""+graph.getId().replaceAll("\"", "\\\"")+": \" + message);"
        +" }"
        // also support calling console.log()
        +"\nvar console = {log:log}");
         
      setPercentComplete(25);
         
      // run script
      setStatus("Running script on " + graph.getId());
      // setStatus(script);
      try {
        engine.eval(script);
      } catch(ScriptException exception) {
        setStatus("Cancelling due to error: " + exception);
        cancel();
        throw new InvalidConfigurationException(this, exception);
      }
         
      setPercentComplete(75);
         
      // set confidences...
         
      if (!isCancelling()) {
            
        // for each annotation
        Set<String> destinationLayerIds = new HashSet<String>(Arrays.asList(getOutputLayers()));
        for (Annotation a : graph.getAnnotationsById().values()) {
          if (destinationLayerIds.contains(a.getLayerId())) {
            // mark output layer changes as CONFIDENCE_AUTOMATIC
            a.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
          } else {
            // cancel changes to non-output layers
            a.rollback();
          }
        } // next annotation
            
        // for each anchor
        for (Anchor a : graph.getAnchors().values()) {
          if (a.getChange() == Change.Operation.Create) {
            // mark new anchors as CONFIDENCE_AUTOMATIC
            a.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
          }
        } // next anchor
      } // not cancelling
    } catch (ScriptException exception) {
      throw new InvalidConfigurationException(this, exception);
    }      
  } // end of mapLabels()

} // end of class JavascriptAnnotator
