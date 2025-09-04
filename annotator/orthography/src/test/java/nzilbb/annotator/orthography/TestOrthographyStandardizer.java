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

package nzilbb.annotator.orthography;
	      
import org.junit.*;
import static org.junit.Assert.*;

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
import nzilbb.ag.TrackedMap;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.annotator.orthography.OrthographyStandardizer;

public class TestOrthographyStandardizer {

  /** Test parameter validation */
  @Test public void invalidTaskParameters() throws Exception {
    Graph g = graph();
    Schema schema = g.getSchema();
    OrthographyStandardizer annotator = new OrthographyStandardizer();
    annotator.setSchema(schema);

    try {
      annotator.setTaskParameters(
        "{"
        // specified as blank
        +"\"tokenLayerId\":\"\","
        +"\"orthographyLayerId\":\"orthography\","
        +"\"lowerCase\":\"on\","
        +"\"exactMatch\":\"on\","
        +"\"replacements\":{\"\\\\s\":\"\"},"
        +"\"filters\":{\"main_participant\":\"\",\"lang\":\"en\"}"
        +"}");
      fail("Should fail with unset orthographyLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "{"
        +"\"tokenLayerId\":\"word\","
        // specified as blank
        +"\"orthographyLayerId\":\"\","
        +"\"lowerCase\":\"on\","
        +"\"exactMatch\":\"on\","
        +"\"replacements\":{\"\\\\s\":\"\"},"
        +"\"filters\":{\"main_participant\":\"\",\"lang\":\"en\"}"
        +"}");
      fail("Should fail with unset orthographyLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "{"
        // doesn't exist in the schema:
        +"\"tokenLayerId\":\"nonexistent\","
        +"\"orthographyLayerId\":\"orthography\","
        +"\"lowerCase\":\"on\","
        +"\"exactMatch\":\"on\","
        +"\"replacements\":{\"\\\\s\":\"\"},"
        +"\"filters\":{\"main_participant\":\"\",\"lang\":\"en\"}"
        +"}");
      fail("Should fail with nonexistent tokenLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "{"
        +"\"tokenLayerId\":\"word\","
        // output same as input:
        +"\"orthographyLayerId\":\"word\","
        +"\"lowerCase\":\"on\","
        +"\"exactMatch\":\"on\","
        +"\"replacements\":{\"\\\\s\":\"\"},"
        +"\"filters\":{\"main_participant\":\"\",\"lang\":\"en\"}"
        +"}");
      fail("Should fail with tokenLayerId == orthographyLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "{"
        +"\"tokenLayerId\":\"word\","
        +"\"orthographyLayerId\":\"orthography\","
        +"\"lowerCase\":\"on\","
        +"\"exactMatch\":\"on\","
        // blank regexp:
        +"\"replacements\":{\"\":\"\"},"
        +"\"filters\":{\"main_participant\":\"\",\"lang\":\"en\"}"
        +"}");
      fail("Should fail with blank replacement regexp");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "{"
        +"\"tokenLayerId\":\"word\","
        +"\"orthographyLayerId\":\"orthography\","
        +"\"lowerCase\":\"on\","
        +"\"exactMatch\":\"on\","
        // invalid regexp:
        +"\"replacements\":{\"[\":\"\"},"
        +"\"filters\":{\"main_participant\":\"\",\"lang\":\"en\"}"
        +"}");
      fail("Should fail with invalid replacement regexp");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "{"
        +"\"tokenLayerId\":\"word\","
        +"\"orthographyLayerId\":\"orthography\","
        +"\"lowerCase\":\"on\","
        +"\"exactMatch\":\"on\","
        +"\"replacements\":{\"\\\\s\":\"\"},"
        // invalid layer:
        +"\"filters\":{\"nonexistent\":\"\",\"lang\":\"en\"}"
        +"}");
      fail("Should fail with invalid filter layer");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "{"
        +"\"tokenLayerId\":\"word\","
        +"\"orthographyLayerId\":\"orthography\","
        +"\"lowerCase\":\"on\","
        +"\"exactMatch\":\"on\","
        +"\"replacements\":{\"\\\\s\":\"\"},"
        // input layer:
        +"\"filters\":{\"nonexistent\":\"\",\"word\":\"en\"}"
        +"}");
      fail("Should fail with invalid filter layer");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "{"
        +"\"tokenLayerId\":\"word\","
        +"\"orthographyLayerId\":\"orthography\","
        +"\"lowerCase\":\"on\","
        +"\"exactMatch\":\"on\","
        +"\"replacements\":{\"\\\\s\":\"\"},"
        // output layer:
        +"\"filters\":{\"nonexistent\":\"\",\"orthography\":\"en\"}"
        +"}");
      fail("Should fail with invalid filter layer");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "{"
        +"\"tokenLayerId\":\"word\","
        +"\"orthographyLayerId\":\"orthography\","
        +"\"lowerCase\":\"on\","
        +"\"exactMatch\":\"on\","
        +"\"replacements\":{\"\\\\s\":\"\"},"
        // invalid regexp:
        +"\"filters\":{\"main_participant\":\"[\",\"lang\":\"en\"}"
        +"}");
      fail("Should fail with invalid filter regexp");
    } catch (InvalidConfigurationException x) {
    }
    // can't validate for repeated filter layer, sorry
  }   


  /** Test normal configuration works. */
  @Test public void transform() throws Exception {

    Graph g = graph();
    Schema schema = g.getSchema();
    OrthographyStandardizer annotator = new OrthographyStandardizer();
    annotator.setSchema(schema);
      
    // stem to a new layer
    annotator.setTaskParameters(
      "{\"tokenLayerId\":\"word\",\"orthographyLayerId\":\"orth\",\"lowerCase\":\"on\"}");
      
    assertTrue("lowerCase true",
               annotator.getLowerCase());
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("orthography layer",
                 "orth", annotator.getOrthographyLayerId());
    assertNotNull("orthography layer was created",
                  schema.getLayer(annotator.getOrthographyLayerId()));
    assertEquals("orthography layer child of word",
                 "word", schema.getLayer(annotator.getOrthographyLayerId()).getParentId());
    assertEquals("orthography layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getOrthographyLayerId()).getAlignment());
    assertEquals("orthography layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getOrthographyLayerId()).getType());
    String[] layers = annotator.getRequiredLayers();
    assertEquals("1 required layer: "+Arrays.asList(layers),
                 1, layers.length);
    assertEquals("required layer correct "+Arrays.asList(layers),
                 "word", layers[0]);
    layers = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(layers),
                 1, layers.length);
    assertEquals("output layer correct "+Arrays.asList(layers),
                 "orth", layers[0]);
      
    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "“'Why", firstWord.getLabel());
      
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 10, g.all("word").length);
    assertEquals("double check there are no orthographies: "+Arrays.asList(g.all("orth")),
                 0, g.all("ortho").length);
   
    // run the annotator
    annotator.transform(g);
    List<String> orthographyLabels = Arrays.stream(g.all("orth"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("one orthography per token: "+orthographyLabels,
                 9, orthographyLabels.size());
    Iterator<String> orthographies = orthographyLabels.iterator();
    assertEquals("down-case",
                 "why", orthographies.next());
    assertEquals("internal apostrophes",
                 "hasn't", orthographies.next());
    assertEquals("accented characters",
                 "inés", orthographies.next());
    assertEquals("hesitations are retained",
                 "d~", orthographies.next());
    assertEquals("dashes removed",
                 "got", orthographies.next());
    assertEquals("her", orthographies.next());
    assertEquals("internal hyphens retained",
                 "x-ray", orthographies.next());
    assertEquals("punctuation stripped",
                 "yet", orthographies.next());
    assertEquals("Hyphen-only omitted, Emoji conserved",
                 "😉", orthographies.next());

    // add a word
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("new")
                    .setStart(g.getOrCreateAnchorAt(95)).setEnd(g.getOrCreateAnchorAt(100))
                    .setParent(g.first("turn")));

    // change a word
    firstWord.setLabel("Por qué");
      
    // run the annotator again
    annotator.transform(g);
    orthographyLabels = Arrays.stream(g.all("orth"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("one more orthography: "+orthographyLabels,
                 10, orthographyLabels.size());
    orthographies = orthographyLabels.iterator();
    assertEquals("changed label re-annotated",
                 "porqué", orthographies.next());
    assertEquals("previous orthography unchanged", "hasn't", orthographies.next());
    assertEquals("previous orthography unchanged", "inés", orthographies.next());
    assertEquals("previous orthography unchanged", "d~", orthographies.next());
    assertEquals("previous orthography unchanged", "got", orthographies.next());
    assertEquals("previous orthography unchanged", "her", orthographies.next());
    assertEquals("previous orthography unchanged", "x-ray", orthographies.next());
    assertEquals("previous orthography unchanged", "yet", orthographies.next());
    assertEquals("previous orthography unchanged", "😉", orthographies.next());
    assertEquals("new token has orthography",
                 "new", orthographies.next());

  }

  /** Test lowerCase=false works. */
  @Test public void noCaseChange() throws Exception {

    Graph g = graph();
    Schema schema = g.getSchema();
    OrthographyStandardizer annotator = new OrthographyStandardizer();
    annotator.setSchema(schema);
      
    // stem to a new layer
    annotator.setTaskParameters(
      "{\"orthographyLayerId\":\"orth\",\"lowerCase\":\"false\"}");
      
    assertFalse("lowerCase false",
                annotator.getLowerCase());
    assertEquals("omitted token layer defaults to word layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("orthography layer",
                 "orth", annotator.getOrthographyLayerId());
    assertNotNull("orthography layer was created",
                  schema.getLayer(annotator.getOrthographyLayerId()));
    assertEquals("orthography layer child of word",
                 "word", schema.getLayer(annotator.getOrthographyLayerId()).getParentId());
    assertEquals("orthography layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getOrthographyLayerId()).getAlignment());
    assertEquals("orthography layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getOrthographyLayerId()).getType());
    String[] layers = annotator.getRequiredLayers();
    assertEquals("1 required layer: "+Arrays.asList(layers),
                 1, layers.length);
    assertEquals("required layer correct "+Arrays.asList(layers),
                 "word", layers[0]);
    layers = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(layers),
                 1, layers.length);
    assertEquals("output layer correct "+Arrays.asList(layers),
                 "orth", layers[0]);
      
    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "“'Why", firstWord.getLabel());
      
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 10, g.all("word").length);
    assertEquals("double check there are no orthographies: "+Arrays.asList(g.all("orth")),
                 0, g.all("ortho").length);
   
    // run the annotator
    annotator.transform(g);
    List<String> orthographyLabels = Arrays.stream(g.all("orth"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("one orthography per token: "+orthographyLabels,
                 9, orthographyLabels.size());
    Iterator<String> orthographies = orthographyLabels.iterator();
    assertEquals("no down-case",
                 "Why", orthographies.next());
    assertEquals("internal apostrophes",
                 "hasn't", orthographies.next());
    assertEquals("accented characters",
                 "Inés", orthographies.next());
    assertEquals("hesitations are retained",
                 "d~", orthographies.next());
    assertEquals("dashes removed",
                 "got", orthographies.next());
    assertEquals("her", orthographies.next());
    assertEquals("internal hyphens retained",
                 "X-ray", orthographies.next());
    assertEquals("punctuation stripped",
                 "yet", orthographies.next());
    assertEquals("Hyphen-only omitted, Emoji conserved",
                 "😉", orthographies.next());

  }

  /** Test default (null) paramters work. */
  @Test public void defaultParameters() throws Exception {
      
    Graph g = graph();
    Schema schema = g.getSchema();
    OrthographyStandardizer annotator = new OrthographyStandardizer();
    annotator.setSchema(schema);
      
    // use default configuration
    annotator.setTaskParameters(null);
      
    assertTrue("lowerCase true",
               annotator.getLowerCase());
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("orthography layer",
                 "orthography", annotator.getOrthographyLayerId());
    assertNotNull("orthography layer was created",
                  schema.getLayer(annotator.getOrthographyLayerId()));
    assertEquals("orthography layer child of word",
                 "word", schema.getLayer(annotator.getOrthographyLayerId()).getParentId());
    assertEquals("orthography layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getOrthographyLayerId()).getAlignment());
    assertEquals("orthography layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getOrthographyLayerId()).getType());
    String[] layers = annotator.getRequiredLayers();
    assertEquals("1 required layer: "+Arrays.asList(layers),
                 1, layers.length);
    assertEquals("required layer correct "+Arrays.asList(layers),
                 "word", layers[0]);
    layers = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(layers),
                 1, layers.length);
    assertEquals("output layer correct "+Arrays.asList(layers),
                 "orthography", layers[0]);
      
    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "“'Why", firstWord.getLabel());
      
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 10, g.all("word").length);
    assertEquals("double check there are no orthographies: "+Arrays.asList(g.all("orthography")),
                 0, g.all("porterorthography").length);
   
    // run the annotator
    annotator.transform(g);
    List<String> orthographyLabels = Arrays.stream(g.all("orthography"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("one orthography per token: "+orthographyLabels,
                 9, orthographyLabels.size());
    Iterator<String> orthographies = orthographyLabels.iterator();
    assertEquals("down-case",
                 "why", orthographies.next());
    assertEquals("internal apostrophes",
                 "hasn't", orthographies.next());
    assertEquals("accented characters",
                 "inés", orthographies.next());
    assertEquals("hesitations are retained",
                 "d~", orthographies.next());
    assertEquals("dashes removed",
                 "got", orthographies.next());
    assertEquals("her", orthographies.next());
    assertEquals("internal hyphens retained",
                 "x-ray", orthographies.next());
    assertEquals("punctuation stripped",
                 "yet", orthographies.next());
    assertEquals("Hyphen-only omitted, Emoji conserved",
                 "😉", orthographies.next());

    // add a word
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("new")
                    .setStart(g.getOrCreateAnchorAt(95)).setEnd(g.getOrCreateAnchorAt(100))
                    .setParent(g.first("turn")));

  }

  /** Test blank removalPattern works. */
  @Test public void noRemovalPattern() throws Exception {
      
    Graph g = graph();
    Schema schema = g.getSchema();
    OrthographyStandardizer annotator = new OrthographyStandardizer();
    annotator.setSchema(schema);
      
    // use specific configuration
    annotator.setTaskParameters(
      "{\"tokenLayerId\":\"word\","
      +"\"replacements\":{"
      +"\"\\\\s\":\"\","
      +"\"’\":\"'\","
      +"\"“\":\"\\\"\","
      +"\"”\":\"\\\"\","
      +"\"—\":\"-\","
      +"\"^[-']+\":\"\","
      +"\"[-']+$\":\"\""
      +"}}");
      
    assertFalse("lowerCase false",
               annotator.getLowerCase());
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertEquals("omitted orthography layer defaults to \"orthography\"",
                 "orthography", annotator.getOrthographyLayerId());
    assertNotNull("orthography layer was created",
                  schema.getLayer(annotator.getOrthographyLayerId()));
    assertEquals("orthography layer child of word",
                 "word", schema.getLayer(annotator.getOrthographyLayerId()).getParentId());
    assertEquals("orthography layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getOrthographyLayerId()).getAlignment());
    assertEquals("orthography layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getOrthographyLayerId()).getType());
    String[] layers = annotator.getRequiredLayers();
    assertEquals("1 required layer: "+Arrays.asList(layers),
                 1, layers.length);
    assertEquals("required layer correct "+Arrays.asList(layers),
                 "word", layers[0]);
    layers = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(layers),
                 1, layers.length);
    assertEquals("output layer correct "+Arrays.asList(layers),
                 "orthography", layers[0]);
      
    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "“'Why", firstWord.getLabel());
      
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 10, g.all("word").length);
    assertEquals("double check there are no orthographies: "+Arrays.asList(g.all("orthography")),
                 0, g.all("porterorthography").length);
   
    // run the annotator
    annotator.transform(g);
    List<String> orthographyLabels = Arrays.stream(g.all("orthography"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("one orthography per token: "+orthographyLabels,
                 9, orthographyLabels.size());
    Iterator<String> orthographies = orthographyLabels.iterator();
    assertEquals("no down-case",
                 "\"'Why", orthographies.next());
    assertEquals("internal apostrophes",
                 "hasn't", orthographies.next());
    assertEquals("accented characters",
                 "Inés", orthographies.next());
    assertEquals("hesitations are retained",
                 "d~", orthographies.next());
    assertEquals("dashes removed",
                 "got", orthographies.next());
    assertEquals("her", orthographies.next());
    assertEquals("internal hyphens retained",
                 "X-ray", orthographies.next());
    assertEquals("punctuation stripped",
                 "yet?'\"", orthographies.next());
    assertEquals("Hyphen-only omitted, Emoji conserved",
                 "😉", orthographies.next());

  }

  /** Ensure that filtering in by a pattern on another layer works. */
  @Test public void filterIn() throws Exception {
    
    Graph g = graph();
    Schema schema = g.getSchema();
    OrthographyStandardizer annotator = new OrthographyStandardizer();
    annotator.setSchema(schema);
    
    // filter out tokens not within matching 'lang' annotations
    annotator.setTaskParameters(
      "{\"tokenLayerId\":\"word\","
      +"\"orthographyLayerId\":\"orth\","
      +"\"lowerCase\":\"on\","
      +"\"filters\":{\"lang\":\"f.*\"}"
      +"}");
      
    assertEquals("only one filter " + annotator.getFilters(),
                 1, annotator.getFilters().size());
    assertEquals("filter correct " + annotator.getFilters(),
                 "f.*", annotator.getFilters().get("lang"));
    assertTrue("lowerCase true", annotator.getLowerCase());
    assertEquals("token layer", "word", annotator.getTokenLayerId());
    assertEquals("orthography layer", "orth", annotator.getOrthographyLayerId());
    assertNotNull("orthography layer was created",
                  schema.getLayer(annotator.getOrthographyLayerId()));
    assertEquals("orthography layer child of word",
                 "word", schema.getLayer(annotator.getOrthographyLayerId()).getParentId());
    assertEquals("orthography layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getOrthographyLayerId()).getAlignment());
    assertEquals("orthography layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getOrthographyLayerId()).getType());
    String[] layers = annotator.getRequiredLayers();
    assertEquals("2 required layers: "+Arrays.asList(layers),
                 2, layers.length);
    assertEquals("required input layer correct "+Arrays.asList(layers),
                 "word", layers[0]);
    assertEquals("required filter layer correct "+Arrays.asList(layers),
                 "lang", layers[1]);
    layers = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(layers),
                 1, layers.length);
    assertEquals("output layer correct "+Arrays.asList(layers),
                 "orth", layers[0]);
      
    // run the annotator
    annotator.transform(g);
    List<Annotation> orthographyAnnotations = Arrays.stream(g.all("orth"))
      .collect(Collectors.toList());
    assertEquals("two orthography tokens: "+orthographyAnnotations,
                 2, orthographyAnnotations.size());
    Iterator<Annotation> orthographies = orthographyAnnotations.iterator();
    assertEquals("first word in matching phrase",
                 "inés", orthographies.next().getLabel());
    assertEquals("last word in matching phrase",
                 "d~", orthographies.next().getLabel());

    // reconfigure annotator to filter out those tokens, but filter in others
    annotator.setTaskParameters(
      "{\"tokenLayerId\":\"word\","
      +"\"orthographyLayerId\":\"orth\","
      +"\"lowerCase\":\"on\","
      +"\"filters\":{\"lang\":\"emoji\"}"
      +"}");
    
    // run the annotator again
    annotator.transform(g);
    orthographyAnnotations = Arrays.stream(g.all("orth")).collect(Collectors.toList());
    assertEquals("one more orthography: "+orthographyAnnotations,
                 3, orthographyAnnotations.size());
    orthographies = orthographyAnnotations.iterator();
    assertTrue("first one deleted",
               TrackedMap.Destroyed(orthographies.next()));
    assertTrue("second one deleted",
               TrackedMap.Destroyed(orthographies.next()));
    assertEquals("Emoji added", "😉", orthographies.next().getLabel());

  }

  /** Ensure that filtering out tokens by another layer works. */
  @Test public void filterOut() throws Exception {
    
    Graph g = graph();
    Schema schema = g.getSchema();
    OrthographyStandardizer annotator = new OrthographyStandardizer();
    annotator.setSchema(schema);
    
    // filter out tokens not within matching 'lang' annotations
    annotator.setTaskParameters(
      "{\"tokenLayerId\":\"word\","
      +"\"orthographyLayerId\":\"orth\","
      +"\"lowerCase\":\"on\","
      +"\"filters\":{\"lang\":\"\"}"
      +"}");
      
    assertEquals("only one filter " + annotator.getFilters(),
                 1, annotator.getFilters().size());
    assertEquals("filter correct " + annotator.getFilters(),
                 "", annotator.getFilters().get("lang"));
    assertTrue("lowerCase true", annotator.getLowerCase());
    assertEquals("token layer", "word", annotator.getTokenLayerId());
    assertEquals("orthography layer", "orth", annotator.getOrthographyLayerId());
    assertNotNull("orthography layer was created",
                  schema.getLayer(annotator.getOrthographyLayerId()));
    assertEquals("orthography layer child of word",
                 "word", schema.getLayer(annotator.getOrthographyLayerId()).getParentId());
    assertEquals("orthography layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 schema.getLayer(annotator.getOrthographyLayerId()).getAlignment());
    assertEquals("orthography layer type correct",
                 Constants.TYPE_STRING,
                 schema.getLayer(annotator.getOrthographyLayerId()).getType());
    String[] layers = annotator.getRequiredLayers();
    assertEquals("2 required layers: "+Arrays.asList(layers),
                 2, layers.length);
    assertEquals("required input layer correct "+Arrays.asList(layers),
                 "word", layers[0]);
    assertEquals("required filter layer correct "+Arrays.asList(layers),
                 "lang", layers[1]);
    layers = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(layers),
                 1, layers.length);
    assertEquals("output layer correct "+Arrays.asList(layers),
                 "orth", layers[0]);
      
    // run the annotator
    annotator.transform(g);
    List<String> orthographyLabels = Arrays.stream(g.all("orth"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("one orthography per token: "+orthographyLabels,
                 6, orthographyLabels.size());
    Iterator<String> orthographies = orthographyLabels.iterator();
    assertEquals("down-case",
                 "why", orthographies.next());
    assertEquals("internal apostrophes",
                 "hasn't", orthographies.next());
    assertEquals("dashes removed",
                 "got", orthographies.next());
    assertEquals("her", orthographies.next());
    assertEquals("internal hyphens retained",
                 "x-ray", orthographies.next());
    assertEquals("punctuation stripped",
                 "yet", orthographies.next());
  }

  /** Test whole-layer generation uses GraphStore.tagMatchingAnnotations correctly */
  @Test public void transformTranscripts() {
    GraphStoreHarness store = new GraphStoreHarness();
    Schema schema = schema();
    OrthographyStandardizer annotator = new OrthographyStandardizer();
    annotator.setSchema(schema);
    try {
      annotator.setTaskParameters(null);

      // call tagMatchingAnnotations
      annotator.transformTranscripts(store, null);
    } catch(Exception exception) {
      fail(""+exception);
    }

    // check the right calls were made to the graph store
    assertEquals("aggregateMatchingAnnotations operation",
                 "DISTINCT", store.aggregateMatchingAnnotationsOperation);
    assertEquals("aggregateMatchingAnnotations expression",
                 "layer.id == 'word'", store.aggregateMatchingAnnotationsExpression);
    
    assertEquals("tagMatchingAnnotations num labels: " + store.tagMatchingAnnotationsLabels,
                 2, store.tagMatchingAnnotationsLabels.size());
    assertEquals("tagMatchingAnnotations layerId foo back-slash: "
                 + store.tagMatchingAnnotationsLabels,
                 "foo", store.tagMatchingAnnotationsLabels.get(
                   "layer.id == 'word' && label == 'Foo\\\\'"));
    assertEquals("tagMatchingAnnotations layerId bar: " + store.tagMatchingAnnotationsLabels,
                 "bar", store.tagMatchingAnnotationsLabels.get(
                   "layer.id == 'word' && label == ' \\'bar\\''"));
    
    assertEquals("tagMatchingAnnotations num layerIds: " + store.tagMatchingAnnotationsLayerIds,
                 2, store.tagMatchingAnnotationsLayerIds.size());
    assertEquals("tagMatchingAnnotations layerId foo back-slash",
                 "orthography", store.tagMatchingAnnotationsLayerIds.get(
                   "layer.id == 'word' && label == 'Foo\\\\'"));
    assertEquals("tagMatchingAnnotations layerId bar",
                 "orthography", store.tagMatchingAnnotationsLayerIds.get(
                   "layer.id == 'word' && label == ' \\'bar\\''"));
    
    assertEquals("tagMatchingAnnotations num confidences: "
                 + store.tagMatchingAnnotationsConfidences,
                 2, store.tagMatchingAnnotationsConfidences.size());
    assertEquals("tagMatchingAnnotations layerId foo back-slash",
                 Integer.valueOf(50), store.tagMatchingAnnotationsConfidences.get(
                   "layer.id == 'word' && label == 'Foo\\\\'"));
    assertEquals("tagMatchingAnnotations layerId bar",
                 Integer.valueOf(50), store.tagMatchingAnnotationsConfidences.get(
                   "layer.id == 'word' && label == ' \\'bar\\''"));    
  }
   
  /** Test whole-layer generation, including filters, uses
   * GraphStore.tagMatchingAnnotations correctly */
  @Test public void transformTranscriptsWithFilters() {
    GraphStoreHarness store = new GraphStoreHarness();
    Schema schema = schema();
    OrthographyStandardizer annotator = new OrthographyStandardizer();
    annotator.setSchema(schema);
    try {
      annotator.setTaskParameters(
        "{\"tokenLayerId\":\"word\","
        +"\"orthographyLayerId\":\"orthography\","
        +"\"lowerCase\":\"on\","
        +"\"filters\":{\"main_participant\":\"\",\"lang\":\"en\"}"
        +"}");
      
      // call tagMatchingAnnotations
      annotator.transformTranscripts(store, null);
    } catch(Exception exception) {
      fail(""+exception);
    }
    
    // check the right calls were made to the graph store
    assertEquals("aggregateMatchingAnnotations operation",
                 "DISTINCT", store.aggregateMatchingAnnotationsOperation);
    assertEquals("aggregateMatchingAnnotations expression",
                 "layer.id == 'word'"
                 +" && all('main_participant').length == 0"
                 +" && /en/.test(first('lang').label)",
                 store.aggregateMatchingAnnotationsExpression);
    
    assertEquals("tagMatchingAnnotations num labels: " + store.tagMatchingAnnotationsLabels,
                 2, store.tagMatchingAnnotationsLabels.size());
    assertEquals("tagMatchingAnnotations layerId foo back-slash: "
                 + store.tagMatchingAnnotationsLabels,
                 "foo", store.tagMatchingAnnotationsLabels.get(
                   // TODO update these when exclusion works via AGQL
                   "layer.id == 'word'"
                   +" && all('main_participant').length == 0"
                   +" && /en/.test(first('lang').label)"
                   +" && label == 'Foo\\\\'"));
    assertEquals("tagMatchingAnnotations layerId bar: " + store.tagMatchingAnnotationsLabels,
                 "bar", store.tagMatchingAnnotationsLabels.get(
                   "layer.id == 'word'"
                   +" && all('main_participant').length == 0"
                   +" && /en/.test(first('lang').label)"
                   +" && label == ' \\'bar\\''"));
    
    assertEquals("tagMatchingAnnotations num layerIds: " + store.tagMatchingAnnotationsLayerIds,
                 2, store.tagMatchingAnnotationsLayerIds.size());
    assertEquals("tagMatchingAnnotations layerId foo back-slash",
                 "orthography", store.tagMatchingAnnotationsLayerIds.get(
                   "layer.id == 'word'"
                   +" && all('main_participant').length == 0"
                   +" && /en/.test(first('lang').label)"
                   +" && label == 'Foo\\\\'"));
    assertEquals("tagMatchingAnnotations layerId bar",
                 "orthography", store.tagMatchingAnnotationsLayerIds.get(
                   "layer.id == 'word'"
                   +" && all('main_participant').length == 0"
                   +" && /en/.test(first('lang').label)"
                   +" && label == ' \\'bar\\''"));
    
    assertEquals("tagMatchingAnnotations num confidences: "
                 + store.tagMatchingAnnotationsConfidences,
                 2, store.tagMatchingAnnotationsConfidences.size());
    assertEquals("tagMatchingAnnotations layerId foo back-slash",
                 Integer.valueOf(50), store.tagMatchingAnnotationsConfidences.get(
                   "layer.id == 'word'"
                   +" && all('main_participant').length == 0"
                   +" && /en/.test(first('lang').label)"
                   +" && label == 'Foo\\\\'"));
    assertEquals("tagMatchingAnnotations layerId bar",
                 Integer.valueOf(50), store.tagMatchingAnnotationsConfidences.get(
                   "layer.id == 'word'"
                   +" && all('main_participant').length == 0"
                   +" && /en/.test(first('lang').label)"
                   +" && label == ' \\'bar\\''"));

    assertNull("not calling deleteMatchingAnnotations any more",
                 store.deleteMatchingAnnotationsExpression);    
  }
   
  /**
   * Returns a layer schema for testing.
   * @return A valid schema.
   */
  public Schema schema() {
    return new Schema(
      "who", "turn", "utterance", "word",
      new Layer("transcript_language", "Overall Language")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("participant", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("main_participant", "Main speakers").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("participant").setParentIncludes(true),
      new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("participant").setParentIncludes(true),
      new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("lang", "Phrase Language").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true));
  }
  
  /**
   * Returns a graph for annotating.
   * @return The graph for testing with.
   */
  public Graph graph() {
    Schema schema = schema();
    // annotate a graph
    Graph g = new Graph()
      .setSchema(schema);
    Anchor start = g.getOrCreateAnchorAt(1);
    Anchor end = g.getOrCreateAnchorAt(100);
    g.addAnnotation(
      new Annotation().setLayerId("participant").setLabel("someone")
      .setStart(start).setEnd(end));
    Annotation turn = g.addAnnotation(
      new Annotation().setLayerId("turn").setLabel("someone")
      .setStart(start).setEnd(end)
      .setParent(g.first("participant")));
    g.addAnnotation(
      new Annotation().setLayerId("utterance").setLabel("someone")
      .setStart(start).setEnd(end)
      .setParent(turn));
      
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("“'Why")
                    .setStart(g.getOrCreateAnchorAt(10)).setEnd(g.getOrCreateAnchorAt(20))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("hasn’t")
                    .setStart(g.getOrCreateAnchorAt(20)).setEnd(g.getOrCreateAnchorAt(30))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("'Inés'")
                    .setStart(g.getOrCreateAnchorAt(30)).setEnd(g.getOrCreateAnchorAt(40))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("d~")
                    .setStart(g.getOrCreateAnchorAt(40)).setEnd(g.getOrCreateAnchorAt(50))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("got —")
                    .setStart(g.getOrCreateAnchorAt(50)).setEnd(g.getOrCreateAnchorAt(60))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("her")
                    .setStart(g.getOrCreateAnchorAt(60)).setEnd(g.getOrCreateAnchorAt(70))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("X-ray")
                    .setStart(g.getOrCreateAnchorAt(70)).setEnd(g.getOrCreateAnchorAt(80))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("yet?'”")
                    .setStart(g.getOrCreateAnchorAt(80)).setEnd(g.getOrCreateAnchorAt(85))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("—")
                    .setStart(g.getOrCreateAnchorAt(85)).setEnd(g.getOrCreateAnchorAt(90))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("😉")
                    .setStart(g.getOrCreateAnchorAt(90)).setEnd(g.getOrCreateAnchorAt(95))
                    .setParent(turn));

    g.addAnnotation(new Annotation().setLayerId("lang").setLabel("fr")
                    .setStart(g.getOrCreateAnchorAt(30)).setEnd(g.getOrCreateAnchorAt(50))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("lang").setLabel("emoji")
                    .setStart(g.getOrCreateAnchorAt(90)).setEnd(g.getOrCreateAnchorAt(95))
                    .setParent(turn));
    
    return g;
  } // end of graph()

  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.annotator.orthography.TestOrthographyStandardizer");
  }
}
