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
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import nzilbb.ag.Anchor;
import nzilbb.ag.Annotation;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.automation.Dictionary;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.UsesRelationalDatabase;
import nzilbb.ag.util.DefaultOffsetGenerator;

public class TestMorTagger {
  
  static MorTagger annotator = new MorTagger();
  
  @BeforeClass
  public static void install() throws Exception {
    
    System.out.println("Installing mor/grammar if necessary...");
    
    // find the current directory
    File dir = dir();
    
    // set the schema
    annotator.setSchema(graph().getSchema());
    
    // set the working directory
    annotator.setWorkingDirectory(dir);
    
    // set the annotator configuration, which will install the models the first time (only)
    annotator.setConfig(annotator.getConfig());
    
    System.out.println("MOR installed.");
  }
  
  public static File dir() throws Exception { 
    URL urlThisClass = TestMorTagger.class.getResource(
      TestMorTagger.class.getSimpleName() + ".class");
    File fThisClass = new File(urlThisClass.toURI());
    return fThisClass.getParentFile();
  }

  /** The annotator works out of the box with not particular configuration */
  @Test public void defaultParameters() throws Exception {
    
    Graph g = graph();
    g.trackChanges();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // use default configuration
    annotator.setTaskParameters(null);
    
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("transcript language layer",
                 "transcript_language", annotator.getLanguagesLayerId());
    assertEquals("mor layer",
                 "mor", annotator.getMorLayerId());
    assertNotNull("mor layer was created",
                  schema.getLayer(annotator.getMorLayerId()));
    assertEquals("mor layer child of word",
                 "word", schema.getLayer(annotator.getMorLayerId()).getParentId());
    assertEquals("mor layer aligned",
                 Constants.ALIGNMENT_INTERVAL,
                 schema.getLayer(annotator.getMorLayerId()).getAlignment());
    assertEquals("mor layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getMorLayerId()).getType());
    assertTrue("mor layer allows peers", // contractions like "I'll" might have two tags
                schema.getLayer(annotator.getMorLayerId()).getPeers());
    assertTrue("mor layer saturated",
                schema.getLayer(annotator.getMorLayerId()).getSaturated());
    assertFalse("pos layer not saturated",
                schema.getLayer(annotator.getPartOfSpeechLayerId()).getSaturated());
    assertFalse("pos subcategory not layer saturated",
                schema.getLayer(annotator.getPartOfSpeechSubcategoryLayerId()).getSaturated());
    assertFalse("prefix layer not saturated",
                schema.getLayer(annotator.getPrefixLayerId()).getSaturated());
    assertFalse("stem layer not saturated",
                schema.getLayer(annotator.getStemLayerId()).getSaturated());
    assertFalse("fusional suffix layer not saturated",
                schema.getLayer(annotator.getFusionalSuffixLayerId()).getSaturated());
    assertFalse("suffix layer not saturated",
                schema.getLayer(annotator.getSuffixLayerId()).getSaturated());
    assertFalse("gloss layer not saturated",
                schema.getLayer(annotator.getGlossLayerId()).getSaturated());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("Required layers: "+requiredLayers,
                 5, requiredLayers.size());
    assertTrue("participants required "+requiredLayers,
               requiredLayers.contains("participant"));
    assertTrue("turns required "+requiredLayers,
               requiredLayers.contains("turn"));
    assertTrue("utterances required "+requiredLayers,
               requiredLayers.contains("utterance"));
    assertTrue("word required "+requiredLayers,
               requiredLayers.contains("word"));
    assertTrue("transcript_language required "+requiredLayers,
               requiredLayers.contains("transcript_language"));
    Set<String> outputLayers = Arrays.stream(annotator.getOutputLayers())
      .collect(Collectors.toSet());;
    assertEquals("All possibl output layers: "+outputLayers,
                 8, outputLayers.size());
    assertTrue("output layers include mor", outputLayers.contains("mor"));
    assertTrue("output layers include morPrefix", outputLayers.contains("morPrefix"));
    assertTrue("output layers include morPOS", outputLayers.contains("morPOS"));
    assertTrue("output layers include morPOSSubcategory",
               outputLayers.contains("morPOSSubcategory"));
    assertTrue("output layers include morStem", outputLayers.contains("morStem"));
    assertTrue("output layers include morFusionalSuffix",
               outputLayers.contains("morFusionalSuffix"));
    assertTrue("output layers include morSuffix", outputLayers.contains("morSuffix"));
    assertTrue("output layers include morGloss", outputLayers.contains("morGloss"));
    
    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "I'll", firstWord.getLabel());
      
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 9, g.all("word").length);
    assertEquals("double check there are no mores: "+Arrays.asList(g.all("mor")),
                 0, g.all("mor").length);
    // run the annotator
    annotator.getStatusObservers().add(s->System.out.println(s));
    annotator.transform(g);
    List<Annotation> morAnnotations = Arrays.stream(g.all("mor"))
      .collect(Collectors.toList());
    assertEquals("Correct number of tokens (disfluent w~ is skipped) "+morAnnotations,
                 9, morAnnotations.size());
    Iterator<Annotation> mors = morAnnotations.iterator();
    assertEquals("I'll", "pro:sub|I", mors.next().getLabel());
    assertEquals("I'll", "mod|will", mors.next().getLabel());
    assertEquals("sing", "v|sing", mors.next().getLabel());
    assertEquals("and", "coord|and", mors.next().getLabel());
    assertEquals("walk", "n|walk", mors.next().getLabel());
    assertEquals("about", "prep|about", mors.next().getLabel());
    assertEquals("my", "det:poss|my", mors.next().getLabel());
    assertEquals("blogging-morting", "?|bloggingmorting", mors.next().getLabel());
    assertEquals("lazily", "adv|laze&dadj-Y-LY", mors.next().getLabel());

    mors = morAnnotations.iterator();
    assertNotNull("pro:sub|I start anchored", mors.next().getStart().getOffset());
    assertNotNull("mod|will start anchored", mors.next().getStart().getOffset());
    assertNotNull("v|sing start anchored", mors.next().getStart().getOffset());
    assertNotNull("coord|and start anchored", mors.next().getStart().getOffset());
    assertNotNull("n|walk start anchored", mors.next().getStart().getOffset());
    assertNotNull("prep|about start anchored", mors.next().getStart().getOffset());
    assertNotNull("det:poss|my start anchored", mors.next().getStart().getOffset());
    assertNotNull("?|bloggingmorting start anchored", mors.next().getStart().getOffset());
    assertNotNull("adv|laze&dadj-Y-LY start anchored", mors.next().getStart().getOffset());

    mors = morAnnotations.iterator();
    assertNotNull("pro:sub|I end anchored", mors.next().getEnd().getOffset());
    assertNotNull("mod|will end anchored", mors.next().getEnd().getOffset());
    assertNotNull("v|sing end anchored", mors.next().getEnd().getOffset());
    assertNotNull("coord|and end anchored", mors.next().getEnd().getOffset());
    assertNotNull("n|walk end anchored", mors.next().getEnd().getOffset());
    assertNotNull("prep|about end anchored", mors.next().getEnd().getOffset());
    assertNotNull("det:poss|my end anchored", mors.next().getEnd().getOffset());
    assertNotNull("?|bloggingmorting end anchored", mors.next().getEnd().getOffset());
    assertNotNull("adv|laze&dadj-Y-LY end anchored", mors.next().getEnd().getOffset());

    mors = morAnnotations.iterator();
    String[] wordLabels = {
      "I'll", "I'll", 
      "sing .", "and",
      "walk -", 
      "about",
      "my", 
      "blogging-morting",
      "lazily"
    };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("Tag " + i + " should tag " + wordLabels[i],
                   wordLabels[i], mors.next().first("word").getLabel());
    }
    assertEquals("I'll has two tags", 2, firstWord.all("mor").length);

    // prefixes
    Annotation[] prefixes = g.all("morPrefix");
    assertEquals("No prefixes "+Arrays.asList(prefixes),
                 0, prefixes.length);

    // POS
    morAnnotations = Arrays.stream(g.all("morPOS"))
      .collect(Collectors.toList());
    assertEquals("Correct number of POS tags "+morAnnotations,
                 9, morAnnotations.size());
    mors = morAnnotations.iterator();
    assertEquals("I'll", "pro", mors.next().getLabel());
    assertEquals("I'll", "mod", mors.next().getLabel());
    assertEquals("sing", "v", mors.next().getLabel());
    assertEquals("and", "coord", mors.next().getLabel());
    assertEquals("walk", "n", mors.next().getLabel());
    assertEquals("about", "prep", mors.next().getLabel());
    assertEquals("my", "det", mors.next().getLabel());
    assertEquals("blogging-morting", "?", mors.next().getLabel());
    assertEquals("lazily", "adv", mors.next().getLabel());

    // POS subcategory
    morAnnotations = Arrays.stream(g.all("morPOSSubcategory"))
      .collect(Collectors.toList());
    assertEquals("Correct number of POS subcategories "+morAnnotations,
                 2, morAnnotations.size());
    mors = morAnnotations.iterator();
    assertEquals("I'll", "sub", mors.next().getLabel());
    assertEquals("my", "poss", mors.next().getLabel());

    // stem
    morAnnotations = Arrays.stream(g.all("morStem"))
      .collect(Collectors.toList());
    assertEquals("Correct number of stems "+morAnnotations,
                 9, morAnnotations.size());
    mors = morAnnotations.iterator();
    assertEquals("I'll", "I", mors.next().getLabel());
    assertEquals("I'll", "will", mors.next().getLabel());
    assertEquals("sing", "sing", mors.next().getLabel());
    assertEquals("and", "and", mors.next().getLabel());
    assertEquals("walk", "walk", mors.next().getLabel());
    assertEquals("about", "about", mors.next().getLabel());
    assertEquals("my", "my", mors.next().getLabel());
    assertEquals("blogging-morting", "bloggingmorting", mors.next().getLabel());
    assertEquals("lazily", "laze", mors.next().getLabel());

    // fusional suffixes
    morAnnotations = Arrays.stream(g.all("morFusionalSuffix"))
      .collect(Collectors.toList());
    assertEquals("Correct number of fusional suffixes "+morAnnotations,
                 1, morAnnotations.size());
    mors = morAnnotations.iterator();
    assertEquals("lazily", "dadj", mors.next().getLabel());

    // suffixes
    morAnnotations = Arrays.stream(g.all("morSuffix"))
      .collect(Collectors.toList());
    assertEquals("Correct number of suffixes "+morAnnotations,
                 2, morAnnotations.size());
    mors = morAnnotations.iterator();
    assertEquals("lazily", "Y", mors.next().getLabel());
    assertEquals("lazily", "LY", mors.next().getLabel());

    // add a word
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("new")
                    .setStart(g.getOrCreateAnchorAt(90)).setEnd(g.getOrCreateAnchorAt(100))
                    .setParent(g.first("turn")));
    
    // change a word
    firstWord.setLabel("John");
    
    // run the annotator again
    annotator.transform(g);
    g.commit(); // have to commit to remove old tags
    List<String> morLabels = Arrays.stream(g.all("mor"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("- 1 + 1 = same number of mor as before: "+morLabels,
                 9, morLabels.size());
    Iterator<String> morLs = morLabels.iterator();
    assertEquals("John", "n:prop|John", morLs.next());
    assertEquals("sing", "v|sing", morLs.next());
    assertEquals("and", "coord|and", morLs.next());
    assertEquals("walk", "n|walk", morLs.next());
    assertEquals("about", "prep|about", morLs.next());
    assertEquals("my", "det:poss|my", morLs.next());
    assertEquals("blogging-morting", "?|bloggingmorting", morLs.next());
    assertEquals("lazily", "adv|laze&dadj-Y-LY", morLs.next());
    assertEquals("new", "adj|new", morLs.next());

    assertEquals("John has one tag", 1, firstWord.all("mor").length);

    // ensure all word children are inside the word bounds
    for (Annotation word : g.all("word")) {
      for (String l : word.getAnnotations().keySet()) {
        for (Annotation tag : word.getAnnotations().get(l)) {
          assertTrue(
            "word " + word.getId() + " ("+word.getStart()+"-"+word.getEnd()+")"
            +" contains "+l+" child "+tag.getId() + " ("+tag.getStart()+"-"+tag.getEnd()+")",
            word.includes(tag));
        } // next child 
      } // next child layer
    } // next word
  }

  /** Can configure the annotator to target only specific outputs (pos and stem) */
  @Test public void posStemOnly() throws Exception {
    
    Graph g = graph();
    g.trackChanges();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // use default configuration
    annotator.setTaskParameters(
      "tokenLayerId=word"
      +"&languagesLayerId=transcript_language"
      +"&morLayerId="
      +"&prefixLayerId="
      +"&partOfSpeechLayerId=part-of-speech"
      +"&partOfSpeechSubcategoryLayerId="
      +"&stemLayerId=stem"
      +"&fusionalSuffixLayerId="
      +"&suffixLayerId="
      +"&glossLayerId=");
    
    assertEquals("transcript language layer",
                 "transcript_language", annotator.getLanguagesLayerId());
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertNull("no mor layer",
               annotator.getMorLayerId());
    assertNull("no prefix layer",
               annotator.getPrefixLayerId());
    assertNull("no subcategory layer",
               annotator.getPartOfSpeechSubcategoryLayerId());
    assertNull("no fusionalSuffixLayerId layer",
               annotator.getFusionalSuffixLayerId());
    assertNull("no suffix layer",
               annotator.getSuffixLayerId());
    assertNull("no gloss layer",
               annotator.getGlossLayerId());
    assertEquals("pos layer",
                 "part-of-speech", annotator.getPartOfSpeechLayerId());
    assertNotNull("pos layer was created",
                  schema.getLayer(annotator.getPartOfSpeechLayerId()));
    assertEquals("pos layer child of word",
                 "word", schema.getLayer(annotator.getPartOfSpeechLayerId()).getParentId());
    assertEquals("pos layer aligned",
                 Constants.ALIGNMENT_INTERVAL,
                 schema.getLayer(annotator.getPartOfSpeechLayerId()).getAlignment());
    assertEquals("pos layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getPartOfSpeechLayerId()).getType());
    assertTrue("pos layer allows peers", // contractions like "I'll" might have two tags
                schema.getLayer(annotator.getPartOfSpeechLayerId()).getPeers());
    assertEquals("stem layer",
                 "stem", annotator.getStemLayerId());
    assertNotNull("stem layer was created",
                  schema.getLayer(annotator.getStemLayerId()));
    assertEquals("stem layer child of word",
                 "word", schema.getLayer(annotator.getStemLayerId()).getParentId());
    assertEquals("stem layer aligned",
                 Constants.ALIGNMENT_INTERVAL,
                 schema.getLayer(annotator.getStemLayerId()).getAlignment());
    assertEquals("stem layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getStemLayerId()).getType());
    assertTrue("stem layer allows peers", // contractions like "I'll" might have two tags
                schema.getLayer(annotator.getStemLayerId()).getPeers());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("Required layers: "+requiredLayers,
                 5, requiredLayers.size());
    assertTrue("participants required "+requiredLayers,
               requiredLayers.contains("participant"));
    assertTrue("turns required "+requiredLayers,
               requiredLayers.contains("turn"));
    assertTrue("utterances required "+requiredLayers,
               requiredLayers.contains("utterance"));
    assertTrue("word required "+requiredLayers,
               requiredLayers.contains("word"));
    assertTrue("transcript_language required "+requiredLayers,
               requiredLayers.contains("transcript_language"));
    Set<String> outputLayers = Arrays.stream(annotator.getOutputLayers())
      .collect(Collectors.toSet());;
    assertEquals("Stem and POS as output layers: "+outputLayers,
                 2, outputLayers.size());
    assertTrue("output layers include part-of-speech", outputLayers.contains("part-of-speech"));
    assertTrue("output layers include stem", outputLayers.contains("stem"));
    
    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "I'll", firstWord.getLabel());
      
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 9, g.all("word").length);
    assertEquals("double check there are no POSes: "+Arrays.asList(g.all("part-of-speech")),
                 0, g.all("part-of-speech").length);
    // run the annotator
    annotator.transform(g);
    List<Annotation> morAnnotations = Arrays.stream(g.all("part-of-speech"))
      .collect(Collectors.toList());
    assertEquals("Correct number of tokens (disfluent w~ is skipped) "+morAnnotations,
                 9, morAnnotations.size());
    Iterator<Annotation> mors = morAnnotations.iterator();
    assertEquals("I'll", "pro", mors.next().getLabel());
    assertEquals("I'll", "mod", mors.next().getLabel());
    assertEquals("sing", "v", mors.next().getLabel());
    assertEquals("and", "coord", mors.next().getLabel());
    assertEquals("walk", "n", mors.next().getLabel());
    assertEquals("about", "prep", mors.next().getLabel());
    assertEquals("my", "det", mors.next().getLabel());
    assertEquals("blogging-morting", "?", mors.next().getLabel());
    assertEquals("lazily", "adv", mors.next().getLabel());

    mors = morAnnotations.iterator();
    assertNotNull("pro:sub|I start anchored", mors.next().getStart().getOffset());
    assertNotNull("mod|will start anchored", mors.next().getStart().getOffset());
    assertNotNull("v|sing start anchored", mors.next().getStart().getOffset());
    assertNotNull("coord|and start anchored", mors.next().getStart().getOffset());
    assertNotNull("n|walk start anchored", mors.next().getStart().getOffset());
    assertNotNull("prep|about start anchored", mors.next().getStart().getOffset());
    assertNotNull("det:poss|my start anchored", mors.next().getStart().getOffset());
    assertNotNull("?|bloggingmorting start anchored", mors.next().getStart().getOffset());
    assertNotNull("adv|laze&dadj-Y-LY start anchored", mors.next().getStart().getOffset());

    mors = morAnnotations.iterator();
    assertNotNull("pro:sub|I end anchored", mors.next().getEnd().getOffset());
    assertNotNull("mod|will end anchored", mors.next().getEnd().getOffset());
    assertNotNull("v|sing end anchored", mors.next().getEnd().getOffset());
    assertNotNull("coord|and end anchored", mors.next().getEnd().getOffset());
    assertNotNull("n|walk end anchored", mors.next().getEnd().getOffset());
    assertNotNull("prep|about end anchored", mors.next().getEnd().getOffset());
    assertNotNull("det:poss|my end anchored", mors.next().getEnd().getOffset());
    assertNotNull("?|bloggingmorting end anchored", mors.next().getEnd().getOffset());
    assertNotNull("adv|laze&dadj-Y-LY end anchored", mors.next().getEnd().getOffset());

    mors = morAnnotations.iterator();
    String[] wordLabels = {
      "I'll", "I'll", 
      "sing .", "and",
      "walk -", 
      "about", 
      "my", 
      "blogging-morting",
      "lazily"
    };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("Tag " + i + " should tag " + wordLabels[i],
                   wordLabels[i], mors.next().first("word").getLabel());
    }
    assertEquals("I'll has two tags", 2, firstWord.all("part-of-speech").length);

    // stem
    morAnnotations = Arrays.stream(g.all("stem"))
      .collect(Collectors.toList());
    assertEquals("Correct number of stems "+morAnnotations,
                 9, morAnnotations.size());
    mors = morAnnotations.iterator();
    Annotation mor = mors.next();
    assertEquals("I'll", "I", mor.getLabel());
    assertEquals("intermediate anchor confidence",
                 (Integer)Constants.CONFIDENCE_DEFAULT, mor.getEnd().getConfidence());
    assertEquals("I'll", "will", mors.next().getLabel());
    assertEquals("sing", "sing", mors.next().getLabel());
    assertEquals("and", "and", mors.next().getLabel());
    assertEquals("walk", "walk", mors.next().getLabel());
    assertEquals("about", "about", mors.next().getLabel());
    assertEquals("my", "my", mors.next().getLabel());
    assertEquals("blogging-morting", "bloggingmorting", mors.next().getLabel());
    assertEquals("lazily", "laze", mors.next().getLabel());

    // ensure all word children are inside the word bounds
    for (Annotation word : g.all("word")) {
      for (String l : word.getAnnotations().keySet()) {
        for (Annotation tag : word.getAnnotations().get(l)) {
          assertTrue(
            "word " + word + " ("+word.getStart()+"-"+word.getEnd()+")"
            +" contains "+l+" child "+tag + " ("+tag.getStart()+"-"+tag.getEnd()+")",
            word.includes(tag));
        } // next child 
      } // next child layer
    } // next word

  }

  /** Configuring a different input layer works */
  @Test public void otherInputLayer() throws Exception {
    
    Graph g = graph();
    g.trackChanges();
    Schema schema = g.getSchema();
    schema.addLayer(new Layer("token", "Word tokens")
                    .setAlignment(Constants.ALIGNMENT_NONE)
                    .setPeers(false).setPeersOverlap(false).setSaturated(true)
                    .setParentId(schema.getWordLayerId()).setParentIncludes(true));

    // add tokens
    for (Annotation word : g.all(schema.getWordLayerId())) {
      if (!word.getLabel().endsWith("~")) { // skip hesitations
        g.createTag(word, "token", word.getLabel().replaceAll("\\W","").toLowerCase());
      }
    }
    
    annotator.setSchema(schema);
    
    // use default configuration
    annotator.setTaskParameters(
      "tokenLayerId=token"
      +"&languagesLayerId=transcript_language"
      +"&morLayerId="
      +"&prefixLayerId="
      +"&partOfSpeechLayerId=part-of-speech"
      +"&partOfSpeechSubcategoryLayerId="
      +"&stemLayerId=stem"
      +"&fusionalSuffixLayerId="
      +"&suffixLayerId="
      +"&glossLayerId=");
    
    assertEquals("token layer",
                 "token", annotator.getTokenLayerId());
    assertEquals("transcript language layer",
                 "transcript_language", annotator.getLanguagesLayerId());
    assertNull("no mor layer",
               annotator.getMorLayerId());
    assertNull("no prefix layer",
               annotator.getPrefixLayerId());
    assertNull("no subcategory layer",
               annotator.getPartOfSpeechSubcategoryLayerId());
    assertNull("no fusionalSuffixLayerId layer",
               annotator.getFusionalSuffixLayerId());
    assertNull("no suffix layer",
               annotator.getSuffixLayerId());
    assertNull("no gloss layer",
               annotator.getGlossLayerId());
    assertEquals("pos layer",
                 "part-of-speech", annotator.getPartOfSpeechLayerId());
    assertNotNull("pos layer was created",
                  schema.getLayer(annotator.getPartOfSpeechLayerId()));
    assertEquals("pos layer child of word",
                 "word", schema.getLayer(annotator.getPartOfSpeechLayerId()).getParentId());
    assertEquals("pos layer aligned",
                 Constants.ALIGNMENT_INTERVAL,
                 schema.getLayer(annotator.getPartOfSpeechLayerId()).getAlignment());
    assertEquals("pos layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getPartOfSpeechLayerId()).getType());
    assertTrue("pos layer allows peers", // contractions like "I'll" might have two tags
                schema.getLayer(annotator.getPartOfSpeechLayerId()).getPeers());
    assertEquals("stem layer",
                 "stem", annotator.getStemLayerId());
    assertNotNull("stem layer was created",
                  schema.getLayer(annotator.getStemLayerId()));
    assertEquals("stem layer child of word",
                 "word", schema.getLayer(annotator.getStemLayerId()).getParentId());
    assertEquals("stem layer aligned",
                 Constants.ALIGNMENT_INTERVAL,
                 schema.getLayer(annotator.getStemLayerId()).getAlignment());
    assertEquals("stem layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getStemLayerId()).getType());
    assertTrue("stem layer allows peers", // contractions like "I'll" might have two tags
                schema.getLayer(annotator.getStemLayerId()).getPeers());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("Required layers: "+requiredLayers,
                 6, requiredLayers.size());
    assertTrue("participants required "+requiredLayers,
               requiredLayers.contains("participant"));
    assertTrue("turns required "+requiredLayers,
               requiredLayers.contains("turn"));
    assertTrue("utterances required "+requiredLayers,
               requiredLayers.contains("utterance"));
    assertTrue("word required "+requiredLayers,
               requiredLayers.contains("word"));
    assertTrue("token required "+requiredLayers,
               requiredLayers.contains("token"));
    assertTrue("transcript_language required "+requiredLayers,
               requiredLayers.contains("transcript_language"));
    Set<String> outputLayers = Arrays.stream(annotator.getOutputLayers())
      .collect(Collectors.toSet());;
    assertEquals("Stem and POS as output layers: "+outputLayers,
                 2, outputLayers.size());
    assertTrue("output layers include part-of-speech", outputLayers.contains("part-of-speech"));
    assertTrue("output layers include stem", outputLayers.contains("stem"));
    
    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "I'll", firstWord.getLabel());
      
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 9, g.all("word").length);
    assertEquals("double check there are no POSes: "+Arrays.asList(g.all("part-of-speech")),
                 0, g.all("part-of-speech").length);
    // run the annotator
    annotator.transform(g);
    List<Annotation> morAnnotations = Arrays.asList(g.all("part-of-speech"));
    assertEquals("Correct number of tokens (disfluent w~ is skipped) "+morAnnotations,
                 8, morAnnotations.size());
    Iterator<Annotation> mors = morAnnotations.iterator();
    assertEquals("ill", "adv", mors.next().getLabel());
    assertEquals("sing", "v", mors.next().getLabel());
    assertEquals("and", "coord", mors.next().getLabel());
    assertEquals("walk", "n", mors.next().getLabel());
    assertEquals("about", "prep", mors.next().getLabel());
    assertEquals("my", "det", mors.next().getLabel());
    assertEquals("bloggingmorting", "?", mors.next().getLabel());
    assertEquals("lazily", "adv", mors.next().getLabel());

    mors = morAnnotations.iterator();
    String[] wordLabels = {
      "I'll", 
      "sing .", "and",
      "walk -", 
      "about", 
      "my", 
      "blogging-morting",
      "lazily"
    };
    for (int i = 0; i < wordLabels.length; i++) {
      assertEquals("Tag " + i + " should tag " + wordLabels[i],
                   wordLabels[i], mors.next().first("word").getLabel());
    }
    assertEquals("I'll (ill) has one tag", 1, firstWord.all("part-of-speech").length);

    // stem
    morAnnotations = Arrays.stream(g.all("stem"))
      .collect(Collectors.toList());
    assertEquals("Correct number of stems "+morAnnotations,
                 8, morAnnotations.size());
    mors = morAnnotations.iterator();
    assertEquals("I'll", "ill", mors.next().getLabel());
    assertEquals("sing", "sing", mors.next().getLabel());
    assertEquals("and", "and", mors.next().getLabel());
    assertEquals("walk", "walk", mors.next().getLabel());
    assertEquals("about", "about", mors.next().getLabel());
    assertEquals("my", "my", mors.next().getLabel());
    assertEquals("blogging-morting", "bloggingmorting", mors.next().getLabel());
    assertEquals("lazily", "laze", mors.next().getLabel());

    // ensure all word children are inside the word bounds
    for (Annotation word : g.all("word")) {
      for (String l : word.getAnnotations().keySet()) {
        for (Annotation tag : word.getAnnotations().get(l)) {
          assertTrue(
            "word " + word.getId() + " ("+word.getStart()+"-"+word.getEnd()+")"
            +" contains "+l+" child "+tag.getId() + " ("+tag.getStart()+"-"+tag.getEnd()+")",
            word.includes(tag));
        } // next child 
      } // next child layer
    } // next word
  }

  @Test public void setInvalidTaskParameters() throws Exception {
    
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        // doesn't exist in the schema:
        +"&languagesLayerId=language"
        +"&morLayerId=mor"
        +"&prefixLayerId=prefix"
        +"&partOfSpeechLayerId=part-of-speech"
        +"&partOfSpeechSubcategoryLayerId=subcategory"
        +"&stemLayerId=stem"
        +"&fusionalSuffixLayerId=suffix"
        +"&suffixLayerId=suffix"
        +"&glossLayerId=suffix");
      fail("Should fail with nonexistent languagesLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        // doesn't exist in the schema:
        "tokenLayerId=orth"
        +"&languagesLayerId=language"
        +"&morLayerId=mor"
        +"&prefixLayerId=prefix"
        +"&partOfSpeechLayerId=part-of-speech"
        +"&partOfSpeechSubcategoryLayerId=subcategory"
        +"&stemLayerId=stem"
        +"&fusionalSuffixLayerId=suffix"
        +"&suffixLayerId=suffix"
        +"&glossLayerId=suffix");
      fail("Should fail with nonexistent tokenLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&languagesLayerId=transcript_language"
        // same as token layer:
        +"&morLayerId=word"
        +"&prefixLayerId=prefix"
        +"&partOfSpeechLayerId=part-of-speech"
        +"&partOfSpeechSubcategoryLayerId=subcategory"
        +"&stemLayerId=stem"
        +"&fusionalSuffixLayerId=suffix"
        +"&suffixLayerId=suffix"
        +"&glossLayerId=suffix");
      fail("Should fail with morLayerId = word");
    } catch (InvalidConfigurationException x) {
    }
  }   
  
  /**
   * Returns a graph for annotating.
   * @return The graph for testing with.
   */
  public static Graph graph() {
    Schema schema = new Schema(
      "participant", "turn", "utterance", "word",
      new Layer("transcript_language", "Overall Language")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("participant", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("participant").setParentIncludes(true),
      new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true));
    // annotate a graph
    Graph g = new Graph()
      .setSchema(schema);
    g.setId("unit-test");
    Anchor start = g.getOrCreateAnchorAt(1, Constants.CONFIDENCE_MANUAL);
    Anchor end = g.getOrCreateAnchorAt(100, Constants.CONFIDENCE_MANUAL);
    g.addAnnotation(
      new Annotation().setLayerId("participant").setLabel("someone")
      .setStart(start).setEnd(end));
    g.addAnnotation(
      new Annotation().setLayerId("transcript_language").setLabel("en")
      .setStart(start).setEnd(end));
    Annotation turn = g.addAnnotation(
      new Annotation().setLayerId("turn").setLabel("someone")
      .setStart(start).setEnd(end)
      .setParent(g.first("participant")));
    g.addAnnotation(
      new Annotation().setLayerId("utterance").setLabel("someone")
      .setStart(start).setEnd(end)
      .setParent(turn));
    
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("I'll")
                    .setStart(g.getOrCreateAnchorAt(10, Constants.CONFIDENCE_MANUAL)).setEnd(
                      g.getOrCreateAnchorAt(20, Constants.CONFIDENCE_MANUAL))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("sing .") // dot!
                    .setStart(g.getOrCreateAnchorAt(20)).setEnd(
                      g.getOrCreateAnchorAt(30, Constants.CONFIDENCE_MANUAL))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("and")
                    .setStart(g.getOrCreateAnchorAt(30)).setEnd(
                      g.getOrCreateAnchorAt(40, Constants.CONFIDENCE_MANUAL))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("w~")
                    .setStart(g.getOrCreateAnchorAt(40)).setEnd(
                      g.getOrCreateAnchorAt(45, Constants.CONFIDENCE_MANUAL))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("walk -") // dash!
                    .setStart(g.getOrCreateAnchorAt(45)).setEnd(
                      g.getOrCreateAnchorAt(50, Constants.CONFIDENCE_MANUAL))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("about")
                    .setStart(g.getOrCreateAnchorAt(50)).setEnd(
                      g.getOrCreateAnchorAt(60, Constants.CONFIDENCE_MANUAL))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("my")
                    .setStart(g.getOrCreateAnchorAt(60)).setEnd(
                      g.getOrCreateAnchorAt(70, Constants.CONFIDENCE_MANUAL))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("blogging-morting")
                    .setStart(g.getOrCreateAnchorAt(70)).setEnd(
                      g.getOrCreateAnchorAt(80, Constants.CONFIDENCE_MANUAL))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("lazily")
                    .setStart(g.getOrCreateAnchorAt(80)).setEnd(
                      g.getOrCreateAnchorAt(90, Constants.CONFIDENCE_MANUAL))
                    .setParent(turn));
    return g;
  } // end of graph()
  
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.annotator.mor.TestMorTagger");
  }
}
