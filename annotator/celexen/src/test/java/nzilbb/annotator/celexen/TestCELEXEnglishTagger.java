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

package nzilbb.annotator.celexen;
	      
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
import nzilbb.sql.derby.DerbyConnectionFactory;

// IMPORTANT:
// These tests require access to the CELEX lexicon files in a zip file
// in the test resources directory, called "ENGLISH.zip"

public class TestCELEXEnglishTagger {

  static CELEXEnglishTagger annotator = new CELEXEnglishTagger();
  
  @BeforeClass
  public static void install() throws Exception {
    
    System.out.println("Installing lexicon if necessary...");
    
    // find the current directory
    File dir = dir();
    
    // set the schema
    annotator.setSchema(graph().getSchema());
    
    // set the working directory
    annotator.setWorkingDirectory(dir);
    
    // use derby for relational database
    annotator.setRdbConnectionFactory(new DerbyConnectionFactory(dir));

    annotator.getStatusObservers().add(s->System.out.println(s));

    // 'upload' the zip file
    File zip = new File(dir, "ENGLISH.zip");
    if (!zip.exists()) {
      fail("No ENGLISH.zip file found."
           +" To run tests, the CELEX's ENGLISH.zip file must be copied into"
           +" src/test/resources/nzilbb/annotator/celexen/");
    }
    String error = annotator.uploadLexicon(zip);
    if (error != null) {
      System.err.println("Lexicon could not be uploaded: " + error);
    }
    
    // set the annotator configuration, which will install the lexicon the first time (only)
    annotator.setConfig(annotator.getConfig());
    
    System.out.println("Lexicon installed.");
    annotator.getStatusObservers().clear();
  }
  
  public static File dir() throws Exception { 
    URL urlThisClass = TestCELEXEnglishTagger.class.getResource(
      TestCELEXEnglishTagger.class.getSimpleName() + ".class");
    File fThisClass = new File(urlThisClass.toURI());
    return fThisClass.getParentFile();
  }

  /** Ensure a simple set of task parameters result in correct annotations. */
  @Test public void basicTagging() throws Exception {
    
    Graph g = graph();
    // tag the graph as being in New Zealand English
    g.addTag(g, "transcript_language", "en-NZ");
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // use specified configuration
    annotator.setTaskParameters(
      "tokenLayerId=word"
      +"&transcriptLanguageLayerId="   // no transcript language layer
      +"&phraseLanguageLayerId="       // no phrase language layer
      +"&targetLanguagePattern="       // no language pattern
      +"&firstVariantOnly=on"          // firstVariantOnly
      +"&tagLayerId=phonemes"
      +"&sql=SELECT%20PhonStrsDISC%20FROM%20cxen_wordformphonologypron"
      +"%20INNER%20JOIN%20cxen_wordformortho"
      +"%20ON%20cxen_wordformphonologypron.IdNum%20=%20cxen_wordformortho.IdNum"
      +"%20INNER%20JOIN%20cxen_wordform"
      +"%20ON%20cxen_wordformphonologypron.IdNum%20=%20cxen_wordform.IdNum"
      +"%20WHERE%20cxen_wordformortho.WordDia%20=%20?"
      +"%20ORDER%20BY%20cxen_wordformphonologypron.Variant,%20cxen_wordform.IdNum");
    
    assertEquals("token layer",
                 "word", annotator.getTokenLayerId());
    assertNull("transcript language layer",
               annotator.getTranscriptLanguageLayerId());
    assertNull("phrase language layer",
               annotator.getPhraseLanguageLayerId());
    assertNull("language pattern",
               annotator.getTargetLanguagePattern());
    assertEquals(
      "sql",
      "SELECT PhonStrsDISC FROM cxen_wordformphonologypron"
      +" INNER JOIN cxen_wordformortho"
      +" ON cxen_wordformphonologypron.IdNum = cxen_wordformortho.IdNum"
      +" INNER JOIN cxen_wordform"
      +" ON cxen_wordformphonologypron.IdNum = cxen_wordform.IdNum"
      +" WHERE cxen_wordformortho.WordDia = ?"
      +" ORDER BY cxen_wordformphonologypron.Variant, cxen_wordform.IdNum",
      annotator.getSql());
    assertEquals("pronunciation layer",
                 "phonemes", annotator.getTagLayerId());
    assertNotNull("pronunciation layer was created",
                  schema.getLayer(annotator.getTagLayerId()));
    Layer pronunciationLayer = schema.getLayer(annotator.getTagLayerId());
    assertEquals("pronunciation layer child of word",
                 "word", pronunciationLayer.getParentId());
    assertEquals("pronunciation layer not aligned",
                 Constants.ALIGNMENT_NONE,
                 pronunciationLayer.getAlignment());
    assertEquals("pronunciation layer type correct",
                 Constants.TYPE_IPA,
                 pronunciationLayer.getType());
    assertTrue("pronunciation layer has valid labels defined",
               pronunciationLayer.getValidLabels().size() > 0);
    assertTrue("valid labels are DISC",
               pronunciationLayer.getValidLabels().containsKey("@"));
    assertFalse("pronunciation layer disallows peers (firstVariantOnly=true)",
                schema.getLayer(pronunciationLayer.getId()).getPeers());
    Set<String> requiredLayers = Arrays.stream(annotator.getRequiredLayers())
      .collect(Collectors.toSet());
    assertEquals("1 required layer: "+requiredLayers,
                 1, requiredLayers.size());
    assertTrue("word required "+requiredLayers,
               requiredLayers.contains("word"));
    String outputLayers[] = annotator.getOutputLayers();
    assertEquals("1 output layer: "+Arrays.asList(outputLayers),
                 1, outputLayers.length);
    assertEquals("output layer correct "+Arrays.asList(outputLayers),
                 "phonemes", outputLayers[0]);

    Annotation firstWord = g.first("word");
    assertEquals("double check the first word is what we think it is: "+firstWord,
                 "I", firstWord.getLabel());
    
    assertEquals("double check there are tokens: "+Arrays.asList(g.all("word")),
                 9, g.all("word").length);
    assertEquals("double check there are no pronunciations: "+Arrays.asList(g.all("cmudict")),
                 0, g.all("phonemes").length);
    // run the annotator
    annotator.transform(g);
    List<String> pronLabels = Arrays.stream(g.all("phonemes"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("Correct number of tokens "+pronLabels,
                 8, pronLabels.size());
    Iterator<String> prons = pronLabels.iterator();
    assertEquals("'2", prons.next());
    assertEquals("'s{N", prons.next());
    assertEquals("First pronunciation only",
                 "'{nd", prons.next());
    assertEquals("Second pronunciation of 'and' skipped, hesitation tagged",
                 "w@", prons.next());
    assertEquals("'w$kt", prons.next());
    assertEquals("@-'b6t", prons.next());
    assertEquals("'m2", prons.next());
    assertEquals("blogging-posting skipped as it's not in the dictionary",
                 "'l1-zI-lI", prons.next());

    // add a word
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("new")
                    .setStart(g.getOrCreateAnchorAt(90)).setEnd(g.getOrCreateAnchorAt(100))
                    .setParent(g.first("turn")));
    
    // change a word
    firstWord.setLabel("we");
    
    // run the annotator again
    annotator.transform(g);
    pronLabels = Arrays.stream(g.all("phonemes"))
      .map(annotation->annotation.getLabel()).collect(Collectors.toList());
    assertEquals("one more pronunciation: "+pronLabels,
                 9, pronLabels.size());
    prons = pronLabels.iterator();
    assertEquals("changed label not re-annotated", // TODO do we really want this??
                 "'2", prons.next());
    assertEquals("previous pron unchanged", "'s{N", prons.next());
    assertEquals("previous pron unchanged", "'{nd", prons.next());
    assertEquals("previous pron unchanged", "w@", prons.next());
    assertEquals("previous pron unchanged", "'w$kt", prons.next());
    assertEquals("previous pron unchanged", "@-'b6t", prons.next());
    assertEquals("previous pron unchanged", "'m2", prons.next());
    assertEquals("previous pron unchanged", "'l1-zI-lI", prons.next());
    assertEquals("new token has first pronunciation",
                 "'nju", prons.next());
  }   

  /** Ensure that invalid task parameters fail as expected */
  @Test public void invalidTaskParameters() throws Exception {
      
    try {
      annotator.setTaskParameters(null);
      fail("'Default' parameters are invalid");
    } catch (InvalidConfigurationException x) {
      System.out.println(""+x);
    }
    try {
      annotator.setTaskParameters(
        // doesn't exist in the schema
        "tokenLayerId=orthography"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&targetLanguagePattern=en.*"
        +"&tagLayerId=phonemes"
        +"&firstVariantOnly=on"
        +"&sql=SELECT%20PhonStrsDISC%20FROM%20cxen_wordformphonologypron"
	+"%20INNER%20JOIN%20cxen_wordformortho"
	+"%20ON%20cxen_wordformphonologypron.IdNum%20=%20cxen_wordformortho.IdNum"
	+"%20INNER%20JOIN%20cxen_wordform"
	+"%20ON%20cxen_wordformphonologypron.IdNum%20=%20cxen_wordform.IdNum"
	+"%20WHERE%20cxen_wordformortho.WordDia%20=%20?"
	+"%20ORDER%20BY%20cxen_wordformphonologypron.Variant,%20cxen_wordform.IdNum");
      fail("Should fail with nonexistent tokenLayerId");
    } catch (InvalidConfigurationException x) {
      System.out.println(""+x);
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        // doesn't exist in the schema
        +"&transcriptLanguageLayerId=language"
        +"&phraseLanguageLayerId=lang"
        +"&targetLanguagePattern=en.*"
        +"&tagLayerId=phonemes"
        +"&firstVariantOnly=on"
        +"&sql=SELECT%20PhonStrsDISC%20FROM%20cxen_wordformphonologypron"
	+"%20INNER%20JOIN%20cxen_wordformortho"
	+"%20ON%20cxen_wordformphonologypron.IdNum%20=%20cxen_wordformortho.IdNum"
	+"%20INNER%20JOIN%20cxen_wordform"
	+"%20ON%20cxen_wordformphonologypron.IdNum%20=%20cxen_wordform.IdNum"
	+"%20WHERE%20cxen_wordformortho.WordDia%20=%20?"
	+"%20ORDER%20BY%20cxen_wordformphonologypron.Variant,%20cxen_wordform.IdNum");
      fail("Should fail with nonexistent transcriptLanguageLayerId");
    } catch (InvalidConfigurationException x) {
      System.out.println(""+x);
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        // doesn't exist in the schema
        +"&phraseLanguageLayerId=language"
        +"&targetLanguagePattern=en.*"
        +"&tagLayerId=phonemes"
        +"&firstVariantOnly=on"
        +"&sql=SELECT%20PhonStrsDISC%20FROM%20cxen_wordformphonologypron"
	+"%20INNER%20JOIN%20cxen_wordformortho"
	+"%20ON%20cxen_wordformphonologypron.IdNum%20=%20cxen_wordformortho.IdNum"
	+"%20INNER%20JOIN%20cxen_wordform"
	+"%20ON%20cxen_wordformphonologypron.IdNum%20=%20cxen_wordform.IdNum"
	+"%20WHERE%20cxen_wordformortho.WordDia%20=%20?"
	+"%20ORDER%20BY%20cxen_wordformphonologypron.Variant,%20cxen_wordform.IdNum");
      fail("Should fail with nonexistent phraseLanguageLayerId");
    } catch (InvalidConfigurationException x) {
      System.out.println(""+x);
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        // invalid pattern
        +"&targetLanguagePattern=["
        +"&tagLayerId=phonemes"
        +"&firstVariantOnly=on"
        +"&sql=SELECT%20PhonStrsDISC%20FROM%20cxen_wordformphonologypron"
	+"%20INNER%20JOIN%20cxen_wordformortho"
	+"%20ON%20cxen_wordformphonologypron.IdNum%20=%20cxen_wordformortho.IdNum"
	+"%20INNER%20JOIN%20cxen_wordform"
	+"%20ON%20cxen_wordformphonologypron.IdNum%20=%20cxen_wordform.IdNum"
	+"%20WHERE%20cxen_wordformortho.WordDia%20=%20?"
	+"%20ORDER%20BY%20cxen_wordformphonologypron.Variant,%20cxen_wordform.IdNum");
      fail("Should fail with invalid targetLanguagePattern");
    } catch (InvalidConfigurationException x) {
      System.out.println(""+x);
    }
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&targetLanguagePattern=en.*"
        // same as token layer
        +"&tagLayerId=word"
        +"&firstVariantOnly=on"
        +"&sql=SELECT%20PhonStrsDISC%20FROM%20cxen_wordformphonologypron"
	+"%20INNER%20JOIN%20cxen_wordformortho"
	+"%20ON%20cxen_wordformphonologypron.IdNum%20=%20cxen_wordformortho.IdNum"
	+"%20INNER%20JOIN%20cxen_wordform"
	+"%20ON%20cxen_wordformphonologypron.IdNum%20=%20cxen_wordform.IdNum"
	+"%20WHERE%20cxen_wordformortho.WordDia%20=%20?"
	+"%20ORDER%20BY%20cxen_wordformphonologypron.Variant,%20cxen_wordform.IdNum");  
      fail("Should fail with tagLayerId = tokenLayerId");
    } catch (InvalidConfigurationException x) {
      System.out.println(""+x);
    }

    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&targetLanguagePattern=en.*"
        +"&tagLayerId=DISC"
        +"&firstVariantOnly=on"); // no SQL       
      fail("Should fail with no SQL");
    } catch (InvalidConfigurationException x) {
      System.out.println(""+x);
    }

    // set firstVariantOnly = false for a layer that doesn't allow peers 
    annotator.getSchema().addLayer(
      new Layer("phonemes")
      .setAlignment(Constants.ALIGNMENT_NONE)
      // no peers allowed
      .setPeers(false)
      .setParentId(annotator.getSchema().getWordLayerId()));
    annotator.setTaskParameters(
      "tokenLayerId=word"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&phraseLanguageLayerId=lang"
      +"&targetLanguagePattern=en.*"
      +"&tagLayerId=phonemes"
      // all variants
      +"&firstVariantOnly=false"
      +"&sql=SELECT%20PhonStrsDISC%20FROM%20cxen_wordformphonologypron"
      +"%20INNER%20JOIN%20cxen_wordformortho"
      +"%20ON%20cxen_wordformphonologypron.IdNum%20=%20cxen_wordformortho.IdNum"
      +"%20INNER%20JOIN%20cxen_wordform"
      +"%20ON%20cxen_wordformphonologypron.IdNum%20=%20cxen_wordform.IdNum"
      +"%20WHERE%20cxen_wordformortho.WordDia%20=%20?"
      +"%20ORDER%20BY%20cxen_wordformphonologypron.Variant,%20cxen_wordform.IdNum");
    // no exception is thrown, but firstVariantOnly is now true
    assertTrue("firstVariantOnly has been corrected", annotator.getFirstVariantOnly());
  }   

  /** Ensure that short hesitations will be correctly tagged. */
  @Test public void hesitationToDISC() throws Exception {

    assertNull("Non-hesitation", annotator.hesitationToDISC("blog"));
    
    assertEquals("s~", "s@", annotator.hesitationToDISC("s~"));
    assertEquals("se~", "s@", annotator.hesitationToDISC("se~"));
    assertEquals("c~", "k@", annotator.hesitationToDISC("c~"));
    assertEquals("q~", "k@", annotator.hesitationToDISC("c~"));
    assertEquals("j~", "_@", annotator.hesitationToDISC("j~"));
    assertEquals("y~", "j@", annotator.hesitationToDISC("y~"));
    assertEquals("q~", "k@", annotator.hesitationToDISC("c~"));
    assertEquals("a~", "{", annotator.hesitationToDISC("a~"));
    assertEquals("e~", "E", annotator.hesitationToDISC("e~"));
    assertEquals("i~", "I", annotator.hesitationToDISC("i~"));
    assertEquals("o~", "Q", annotator.hesitationToDISC("o~"));
    assertEquals("u~", "V", annotator.hesitationToDISC("u~"));
    assertEquals("shi~", "S@", annotator.hesitationToDISC("shi~"));
    assertEquals("ph~", "f@", annotator.hesitationToDISC("ph~"));
    assertEquals("ng~", "N@", annotator.hesitationToDISC("ng~"));
    assertEquals("th~", "T@", annotator.hesitationToDISC("th~"));
    assertEquals("ch~", "J@", annotator.hesitationToDISC("ch~"));
    assertEquals("wh~", "hw@", annotator.hesitationToDISC("wh~"));
    assertEquals("gn~", "n@", annotator.hesitationToDISC("gn~"));
    assertEquals("kn~", "n@", annotator.hesitationToDISC("kn~"));
    assertEquals("pn~", "nj@", annotator.hesitationToDISC("pn~"));
    assertEquals("ps~", "s@", annotator.hesitationToDISC("ps~"));
    assertEquals("pt~", "t@", annotator.hesitationToDISC("pt~"));
    assertEquals("wr~", "r@", annotator.hesitationToDISC("wr~"));
  }   
    
  /** Test whole-layer generation uses GraphStore.tagMatchingAnnotations correctly,
   * including language filtering. */
  @Test public void transformTranscriptsWithLanguageFiltering() {
    GraphStoreHarness store = new GraphStoreHarness();
    Graph g = graph();
    Schema schema = g.getSchema();
    try {
      annotator.setTaskParameters(
        "tokenLayerId=word"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&phraseLanguageLayerId=lang"
        +"&targetLanguagePattern=en.*"
        +"&tagLayerId=phonemes"
        // all variants
        +"&firstVariantOnly=false"
        +"&sql=SELECT%20PhonStrsDISC%20FROM%20cxen_wordformphonologypron"
        +"%20INNER%20JOIN%20cxen_wordformortho"
        +"%20ON%20cxen_wordformphonologypron.IdNum%20=%20cxen_wordformortho.IdNum"
        +"%20INNER%20JOIN%20cxen_wordform"
        +"%20ON%20cxen_wordformphonologypron.IdNum%20=%20cxen_wordform.IdNum"
        +"%20WHERE%20cxen_wordformortho.WordDia%20=%20?"
        +"%20ORDER%20BY%20cxen_wordformphonologypron.Variant,%20cxen_wordform.IdNum");
      
      // call tagMatchingAnnotations
      annotator.transformTranscripts(store, null);
    } catch(Exception exception) {
      fail(""+exception);
    }

    // check the right calls were made to the graph store
    assertEquals("aggregateMatchingAnnotations operation",
                 "DISTINCT", store.aggregateMatchingAnnotationsOperation);
    assertEquals(
      "aggregateMatchingAnnotations expression",
      "layer.id == 'word'"
      +" && /en.*/.test(first('lang').label ?? first('transcript_language').label)",
      store.aggregateMatchingAnnotationsExpression);
    
    assertEquals("tagMatchingAnnotations num labels: " + store.tagMatchingAnnotationsLabels,
                 2, store.tagMatchingAnnotationsLabels.size());
    assertEquals(
      "tagMatchingAnnotations layerId quick",
      "'kwIk", store.tagMatchingAnnotationsLabels.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label ?? first('transcript_language').label)"
        +" && label == 'quick'"));
    assertEquals(
      "tagMatchingAnnotations layerId brown",
      "'br6n", store.tagMatchingAnnotationsLabels.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label ?? first('transcript_language').label)"
        +" && label == 'brown'"));
    
    assertEquals("tagMatchingAnnotations num layerIds: " + store.tagMatchingAnnotationsLayerIds,
                 2, store.tagMatchingAnnotationsLayerIds.size());
    assertEquals(
      "tagMatchingAnnotations layerId quick",
      "phonemes", store.tagMatchingAnnotationsLayerIds.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label ?? first('transcript_language').label)"
        +" && label == 'quick'"));
    assertEquals(
      "tagMatchingAnnotations layerId brown",
      "phonemes", store.tagMatchingAnnotationsLayerIds.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label ?? first('transcript_language').label)"
        +" && label == 'brown'"));
    
    assertEquals("tagMatchingAnnotations num confidences: "
                 + store.tagMatchingAnnotationsConfidences,
                 2, store.tagMatchingAnnotationsConfidences.size());
    assertEquals(
      "tagMatchingAnnotations layerId quick",
      Integer.valueOf(50), store.tagMatchingAnnotationsConfidences.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label ?? first('transcript_language').label)"
        +" && label == 'quick'"));
    assertEquals(
      "tagMatchingAnnotations layerId brown",
      Integer.valueOf(50), store.tagMatchingAnnotationsConfidences.get(
        "layer.id == 'word'"
        +" && /en.*/.test(first('lang').label ?? first('transcript_language').label)"
        +" && label == 'brown'"));
  }

  /** Test dictionary registration. */
  @Test public void dictionaryRegistration() throws Exception {
    List<String> ids = annotator.getDictionaryIds();
    assertEquals("there are five dictionaris: " + ids,
                 5, ids.size());
    assertTrue(ids.contains("Cobuild Frequency (wordform)"));
    assertTrue(ids.contains("Cobuild Frequency (lemma)"));
    assertTrue(ids.contains("Phonology (wordform)"));
    assertTrue(ids.contains("Morphology (wordform)"));
    assertTrue(ids.contains("Syntax (wordform)"));
    try {
      annotator.getDictionary(null);        
      fail("null dictionary ID is not supported");
    } catch (Exception x) {
      System.out.println(""+x);
    }
    try {
      annotator.getDictionary("Something else");        
      fail("Invalid query dictionary ID is not supported");
    } catch (Exception x) {
      System.out.println(""+x);
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
      new Layer("lang", "Phrase Language").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true));
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
      
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("I")
                    .setStart(g.getOrCreateAnchorAt(10)).setEnd(g.getOrCreateAnchorAt(20))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("sang")
                    .setStart(g.getOrCreateAnchorAt(20)).setEnd(g.getOrCreateAnchorAt(30))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("and")
                    .setStart(g.getOrCreateAnchorAt(30)).setEnd(g.getOrCreateAnchorAt(40))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("w~")
                    .setStart(g.getOrCreateAnchorAt(40)).setEnd(g.getOrCreateAnchorAt(45))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("walked")
                    .setStart(g.getOrCreateAnchorAt(45)).setEnd(g.getOrCreateAnchorAt(50))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("about")
                    .setStart(g.getOrCreateAnchorAt(50)).setEnd(g.getOrCreateAnchorAt(60))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("my")
                    .setStart(g.getOrCreateAnchorAt(60)).setEnd(g.getOrCreateAnchorAt(70))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("blogging-posting")
                    .setStart(g.getOrCreateAnchorAt(70)).setEnd(g.getOrCreateAnchorAt(80))
                    .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("word").setLabel("lazily")
                    .setStart(g.getOrCreateAnchorAt(80)).setEnd(g.getOrCreateAnchorAt(90))
                    .setParent(turn));
    return g;
  } // end of graph()

  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.annotator.celexen.TestCELEXEnglishTagger");
  }
}
