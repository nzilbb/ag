//
// Copyright 2020 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.annotator.patterntagger;

import java.util.LinkedHashSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nzilbb.ag.*;
import nzilbb.ag.automation.Annotator;
import nzilbb.ag.automation.InvalidConfigurationException;

/**
 * Annotator that matches a source layer against specified regular expressions.  In each
 * case, if the expression matches, a specified string is written into the target layer.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class PatternTagger extends Annotator {

   public static final String COPY_FROM_LAYER_TEXT = "Copy from layer: ";

   class Mapping {
      Pattern pattern;
      String label;
      public Mapping(Pattern p, String l) {
         pattern = p;
         label = l;
      }
   }
   
   /** Get the minimum version of the nzilbb.ag API supported by the serializer.*/
   public String getMinimumApiVersion() { return "20200708.2018"; }

   /**
    * ID of the input layer containing word tokens.
    * @see #getSourceLayerId()
    * @see #setSourceLayerId(String)
    */
   protected String sourceLayerId;
   /**
    * Getter for {@link #sourceLayerId}: ID of the input layer containing labels to match against.
    * @return ID of the input layer containing labels to match against.
    */
   public String getSourceLayerId() { return sourceLayerId; }
   /**
    * Setter for {@link #sourceLayerId}: ID of the input layer containing labels to match against.
    * @param newSourceLayerId ID of the input layer containing labels to match against.
    */
   public PatternTagger setSourceLayerId(String newSourceLayerId) {
      sourceLayerId = newSourceLayerId; return this; }

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
   public PatternTagger setTranscriptLanguageLayerId(String newTranscriptLanguageLayerId) {
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
   public PatternTagger setPhraseLanguageLayerId(String newPhraseLanguageLayerId) {
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
   public PatternTagger setLanguage(String newLanguage) { language = newLanguage; return this; }

   /**
    * List of mappings from regular expressions to labels.
    */
   protected Vector<Mapping> mappings = new Vector<Mapping>();

   /**
    * Whether to delete existing annotation if there's no matching pattern.
    * @see #getDeleteOnNoMatch()
    * @see #setDeleteOnNoMatch(boolean)
    */
   protected boolean deleteOnNoMatch = false;
   /**
    * Getter for {@link #deleteOnNoMatch}: Whether to delete existing annotation if there's no 
    * matching pattern.
    * @return Whether to delete existing annotation if there's no matching pattern.
    */
   public boolean getDeleteOnNoMatch() { return deleteOnNoMatch; }
   /**
    * Setter for {@link #deleteOnNoMatch}: Whether to delete existing annotation if
    * there's no 
    * matching pattern.
    * @param newDeleteOnNoMatch Whether to delete existing annotation if there's no
    * matching 
    * pattern.
    */
   public PatternTagger setDeleteOnNoMatch(boolean newDeleteOnNoMatch) { deleteOnNoMatch = newDeleteOnNoMatch; return this; }
   
   /**
    * ID of the output layer.
    * @see #getDestinationLayerId()
    * @see #setDestinationLayerId(String)
    */
   protected String destinationLayerId;
   /**
    * Getter for {@link #destinationLayerId}: ID of the output layer.
    * @return ID of the output layer.
    */
   public String getDestinationLayerId() { return destinationLayerId; }
   /**
    * Setter for {@link #destinationLayerId}: ID of the output layer.
    * @param newDestinationLayerId ID of the output layer.
    */
   public PatternTagger setDestinationLayerId(String newDestinationLayerId) { destinationLayerId = newDestinationLayerId; return this; }

   /**
    * Sets the configuration for a given annotation task.
    * @param parameters The configuration of the annotator; a value of <tt> null </tt>
    * will apply the default task parameters, with {@link #sourceLayerId} set to the
    * {@link Schema#wordLayerId} and {@link #stemLayerId} set to <q>stem</q>.
    * @throws InvalidConfigurationException
    */
   public void setTaskParameters(String parameters) throws InvalidConfigurationException {
      if (schema == null)
         throw new InvalidConfigurationException(this, "Schema is not set.");
      
      if (parameters == null) { // there is no possible default parameter
         throw new InvalidConfigurationException(this, "Parameters not set.");         
      }

      // TODO parse JSON parameters
      
      // does the outputLayer need to be added to the schema?
      if (schema.getLayer(destinationLayerId) == null) {
         schema.addLayer(
            new Layer(destinationLayerId)
            .setAlignment(Constants.ALIGNMENT_NONE) // TODO or interval
            .setPeers(false)                        // TODO or true
            .setParentId(schema.getWordLayerId())   // TODO or turn
            .setType(Constants.TYPE_STRING));
      }
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
      if (sourceLayerId == null)
         throw new InvalidConfigurationException(this, "No input token layer set.");
      Vector<String> requiredLayers = new Vector<String>();
      requiredLayers.add(sourceLayerId);
      if (transcriptLanguageLayerId != null) requiredLayers.add(transcriptLanguageLayerId);
      if (phraseLanguageLayerId != null) requiredLayers.add(phraseLanguageLayerId);
      // TODO if we're copying from layers for labels, we need those layers too
      // TODO we may need to iterate by turn for phrase layers
      // TODO get source parent if it's a word layer - we one one peer per word if it's not aligned
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
      if (destinationLayerId == null)
         throw new InvalidConfigurationException(this, "Destination layer not set.");
      return new String[] { destinationLayerId };
   }
   
   /**
    * Transforms the graph. In this case, the graph is simply summarized, by counting all
    * tokens of each word type, and printing out the result to stdout.
    * @param graph The graph to transform.
    * @return The changes introduced by the tranformation.
    * @throws TransformationException If the transformation cannot be completed.
    */
   public Graph transform(Graph graph) throws TransformationException {
      
      Layer sourceLayer = graph.getSchema().getLayer(sourceLayerId);
      if (sourceLayer == null) {
         throw new InvalidConfigurationException(
            this, "Invalid input token layer: " + sourceLayerId);
      }
      Layer destinationLayer = graph.getSchema().getLayer(destinationLayerId);
      if (destinationLayer == null) {
         throw new InvalidConfigurationException(
            this, "Invalid output destination layer: " + destinationLayerId);
      }

      if (destinationLayer.getParentId().equals(schema.getWordLayerId())) { // token-based tagging

         // we match against individual word tokens and tag them with word-tags...
         
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
         
         // should we just tag everything?
         if (transcriptIsMainlyTarget && !thereArePhraseTags) {
            // process all tokens
            for (Annotation token : graph.all(sourceLayerId)) {
               matchToken(token);
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
                  matchToken(token);
               } // Spanish, so tag it
            } // next token
         } else if (thereArePhraseTags) {
            // process only the tokens phrase-tagged as Spanish
            for (Annotation phrase : graph.all(phraseLanguageLayerId)) {
               if (phrase.getLabel().matches(language)) {
                  for (Annotation token : phrase.all(sourceLayerId)) {
                     matchToken(token);
                  } // next token in the phrase
               } // Spanish phrase
            } // next phrase
         } // thereArePhraseTags
         
      } else { // span-based tagging

         boolean tagsLayer = sourceLayer.getAlignment() == Constants.ALIGNMENT_NONE
            && sourceLayer.getPeers();
         Layer sourceParentLayer = sourceLayer.getParent();
         
         int newAnnotationCount = 0;
         int updatedAnnotationCount = 0;

         Annotation[] turns = graph.all(schema.getTurnLayerId()); // TODO turn->parent
         for (int t = 0; t < turns.length; t++) {
            if (isCancelling()) break;
            Annotation turn = turns[t];
            
            // we're going to construct a parallel graph with character offsets
            Graph textGraph = new Graph();
            textGraph.setOffsetUnits(Constants.UNIT_CHARACTERS);
            textGraph.addLayer(new Layer("||", "Character-offset annotations",
                                         Constants.ALIGNMENT_INTERVAL, true, false, false,
                                         textGraph.getLayerId(), true));
            
            // get all the annotations in the source layer in range
            StringBuffer sText = new StringBuffer();
            Vector<Annotation> layer = new Vector<Annotation>();
            if (tagsLayer) { // tag layer, so we want one tag per parent
               for (Annotation parent : turn.all(sourceParentLayer.getId())) {
                  Annotation a = parent.first(sourceLayer.getId());
                  //if (debug) setStatus("token: " + parent + " #" + parent.getOrdinal() +  " - " + a);
                  if (a != null) layer.add(a);
               } // next parent
            } else { // just get the list directly from the turn
               for (Annotation a : turn.all(sourceLayer.getId())) layer.add(a);
            }
            if (layer.size() == 0) {
               setStatus("Skipping turn as there are no "+sourceParentLayer.getId()+" annotations: "
                         + turn);
               continue;
            }
            int ordinal = 1;
            for (Annotation source : layer) { // TODO create builder for this!
               if (isCancelling()) break;
               if (source.get("||") != null) continue; // should be impossible
               Anchor aTextStart = new Anchor(null, Double.valueOf(sText.length()));
               textGraph.addAnchor(aTextStart);
               sText.append(source.getLabel());
               Anchor aTextEnd = new Anchor(null, Double.valueOf(sText.length()));
               textGraph.addAnchor(aTextEnd);
               
               // create a parallel annotation that marks the extent of this annotation in sAnnotations
               Annotation anText = new Annotation(
                  null, source.getLabel(), "||",
                  aTextStart.getId(), aTextEnd.getId(),
                  textGraph.getId(), ordinal++);
               textGraph.addAnnotation(anText);
               // link corresponding annotations together
               anText.put("||", source);
               source.put("||", anText);
		     
               sText.append(" ");
            } // next annotation
            if (sText.length() == 0)
            {
               setStatus("Skipping turn " + turn + " as there's no text to match");
               continue;
            }
            
            // now that we've got some text to match against, start matching
            String sFinalText = sText.toString();
            //if (debug) setStatus("Matching against: " + sFinalText);
            Annotation[] textAnnotations = textGraph.all("||");
            for (Mapping mapping : mappings) {
               if (isCancelling()) break;
               Pattern pattern = mapping.pattern;
               Matcher matcher = pattern.matcher(sFinalText);
               while (matcher.find()) { // found a match 
                  String sMatch = sFinalText.substring(matcher.start(), matcher.end());
                  if (sMatch.length() > 40) {
                     sMatch = sMatch.substring(0, 15)
                        + "..." + sMatch.substring(sMatch.length() - 15);
                  }
                  setStatus("Match:" + sMatch + "("+matcher.start()+"-"+matcher.end()+")");
                  // find the start annotation
                  int a = 0;
                  Annotation anStart = textAnnotations[a];
                  for (a = 0; a < textAnnotations.length; a++) {
                     if (textAnnotations[a].getEnd().getOffset().intValue() >= matcher.start()) {
                        anStart = textAnnotations[a];
                        break;
                     }
                  } // next text annotation
                  // continue from there, looking for the end annotation
                  Annotation anEnd = anStart;
                  for (; a < textAnnotations.length; a++) {
                     if (textAnnotations[a].getStart().getOffset().intValue() >= matcher.end()) {
                        break;
                     }
                     anEnd = textAnnotations[a];
                  } // next text annotation
                  
                  // we're actually interested in the AG annotations
                  anStart = (Annotation)anStart.get("||");
                  anEnd = (Annotation)anEnd.get("||");
                  
                  //if (debug) setStatus("From " + anStart + " to " + anEnd);
			
                  // create an annotation on our layer that spans
                  // the start and end annotations
                  
                  Annotation annotation = null;
                  if (!deleteOnNoMatch) {
                     // if there's an annotation in the same  place, update that one instead
                     // so get the intersection of annotations that start and the start and
                     // and end at the end
                     LinkedHashSet<Annotation> linkingAnnotations = new LinkedHashSet<Annotation>(
                        anStart.getStart().startOf(destinationLayerId));
                     linkingAnnotations.retainAll(anEnd.getEnd().endOf(destinationLayerId));
                     if (linkingAnnotations.size() > 0) {
                        annotation = linkingAnnotations.iterator().next();
                        //if (debug) setStatus("Updating " + annotation);
                        updatedAnnotationCount++;
                     }
                  }
                  if (annotation == null) {
                     annotation = graph.createAnnotation(
                        anStart.getStart(), anEnd.getEnd(), destinationLayerId, mapping.label,
                        turn);
                     newAnnotationCount++;
                     //if (debug) setStatus("Added annotation: " + annotation);
                  }
                  if (annotation.getLabel().indexOf('$') >= 0) { // group substitution
                     annotation.setLabel(
                        sText.substring(matcher.start(), matcher.end())
                        .replaceAll(pattern.toString(), annotation.getLabel()));
                  }
                  annotation.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
                  
               } // next match
            } // next pattern
            
            setPercentComplete(t * 100 / turns.length);
         } // next turn

      } // span-based tagging
      
      return graph;
   }
   
   /**
    * Annotate the given token if appropriate.
    * @param token The token to match against patterns.
    * @return The new annotation, if any.
    */
   public Annotation matchToken(Annotation token) {
      String tagLabel = null;
      Matcher matcher = null;
      Annotation existingTag = token.first(destinationLayerId);

      // look for a matching pattern
      for (Mapping mapping : mappings) {
         if (isCancelling()) break;
         Pattern pattern = mapping.pattern;
         matcher = pattern.matcher(token.getLabel());
         if (matcher.matches()) {
            // found a match, so break out of the loop
            tagLabel = mapping.label;
            break;
         }
      } // next pattern
      
      if (tagLabel != null && tagLabel.length() != 0) {
         if (tagLabel.startsWith(COPY_FROM_LAYER_TEXT)) { // copy label from elsewhere
            String sCopyLayerId = tagLabel.substring(
               COPY_FROM_LAYER_TEXT.length());
            if (sCopyLayerId.equals(token.getLayerId())) {
               // no need to do anything fancy - we already know
               // what the representation is
                  tagLabel = token.getLabel();
            } else {
               Annotation labelSource = token.first(sCopyLayerId);
               if (labelSource != null) {
                  tagLabel = labelSource.getLabel();			   
               } // not the source layer
            }
         } else if (tagLabel.indexOf('$') >= 0) { // uses captured groups
            tagLabel = matcher.replaceAll(tagLabel);
         }
         if (existingTag != null)  { // existing tag to update
            existingTag.setLabel(tagLabel);
         } else { // not tagged yet
            existingTag = token.createTag(destinationLayerId, tagLabel);
         }
         existingTag.setConfidence(Constants.CONFIDENCE_AUTOMATIC);
         return existingTag;

      } else if (deleteOnNoMatch) {
         if (existingTag != null) existingTag.destroy();
      }
      return null;
   } // end of transcribe()
   
} // end of class PatternTagger