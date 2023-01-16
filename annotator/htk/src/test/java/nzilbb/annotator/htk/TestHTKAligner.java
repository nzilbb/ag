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

package nzilbb.annotator.htk;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;
import nzilbb.ag.Anchor;
import nzilbb.ag.Annotation;
import nzilbb.ag.Change;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.GraphMediaProvider;
import nzilbb.ag.Layer;
import nzilbb.ag.MediaFile;
import nzilbb.ag.PermissionException;
import nzilbb.ag.Schema;
import nzilbb.ag.StoreException;
import nzilbb.ag.automation.Dictionary;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.UsesRelationalDatabase;
import nzilbb.sql.derby.DerbyConnectionFactory;
import nzilbb.util.IO;

public class TestHTKAligner {

  static HTKAligner annotator = new HTKAligner();
  
  @BeforeClass
  public static void install() throws Exception {
    
    System.out.println("Installing HTK if necessary...");
    
    // find the current directory
    File dir = dir();

    // set the schema
    annotator.setSchema(graph().getSchema());
    
    // set the working directory
    annotator.setWorkingDirectory(dir);
    
    // not setting the graph store, sorry
    
    // set the annotator configuration, which will install the lexicon the first time (only)
    annotator.setConfig(annotator.getConfig());
    
    //annotator.getStatusObservers().add(status->System.out.println(status));
    System.out.println("Installed.");
  }

  public static File dir() throws Exception { 
    URL urlThisClass = TestHTKAligner.class.getResource(
      TestHTKAligner.class.getSimpleName() + ".class");
    File fThisClass = new File(urlThisClass.toURI());
    return fThisClass.getParentFile();
  }
  
  @Test public void defaultParameters() throws Exception {
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    try { // use default configuration
      annotator.setTaskParameters(null);
      fail("Should fail with default configuration");
    } catch (InvalidConfigurationException x) {
    }
  }   
  
  @Test public void setValidParameters() throws Exception {
    
    Graph g = graph();
    Schema schema = g.getSchema();
    // set phonemes layer to type=ipa so we can test the type is copied
    schema.getLayer("phonemes").setType(Constants.TYPE_IPA);
    annotator.setSchema(schema);
    
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&pronunciationLayerId=phonemes"
      +"&noiseLayerId="
      +"&utteranceTagLayerId=htk"
      +"&participantTagLayerId="
      +"&wordAlignmentLayerId=word"
      +"&phoneAlignmentLayerId=segment"
      +"&scoreLayerId="
      +"&overlapThreshold=5"
      +"&cleanupOption=75"
      +"&noisePatterns=laugh.* unclear .*noise.*"
      +"&leftPattern="
      +"&rightPattern="
      +"&pauseMarkers=-");
    
    // layers are created as required
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&pronunciationLayerId=phonemes"
      +"&noiseLayerId="
      +"&utteranceTagLayerId=utterance_htk" // nonexistent
      +"&participantTagLayerId=participant_htk" // nonexistent
      +"&wordAlignmentLayerId=word_alignment"
      +"&phoneAlignmentLayerId=phone"
      +"&scoreLayerId=score"
      +"&overlapThreshold=5"
      +"&cleanupOption=75"
      +"&noisePatterns=laugh.* unclear .*noise.*"
      +"&leftPattern="
      +"&rightPattern="
      +"&pauseMarkers=-");
    Layer layer = annotator.getSchema().getLayer("utterance_htk");
    assertNotNull("utterance_htk layer created", layer);
    layer = annotator.getSchema().getLayer("participant_htk");
    assertNotNull("participant_htk layer created", layer);
    //TODO layer = annotator.getSchema().getLayer("word_alignment");
    // assertNotNull("word_alignment layer created", layer);
    layer = annotator.getSchema().getLayer("phone");
    assertNotNull("phone layer created", layer);
    assertEquals("phone layer type", Constants.TYPE_IPA, layer.getType());
    assertTrue("phone layer peers", layer.getPeers());
    layer = annotator.getSchema().getLayer("score");
    assertNotNull("score layer created", layer);
    assertEquals("score layer type", Constants.TYPE_NUMBER, layer.getType());
    assertTrue("score layer peers", layer.getPeers());
  }   
  
  @Test public void setInvalidTaskParameters() throws Exception {
    
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=orthography" // nonexistent
        +"&pronunciationLayerId=phonemes"
        +"&noiseLayerId="
        +"&utteranceTagLayerId=htk"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=segment"
        +"&scoreLayerId="
        +"&overlapThreshold=5"
        +"&cleanupOption=75"
        +"&noisePatterns=laugh.* unclear .*noise.*"
        +"&leftPattern="
        +"&rightPattern="
        +"&pauseMarkers=-");
      fail("Should fail with nonexistent orthographyLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=word"
        +"&pronunciationLayerId=phonology" // nonexistent
        +"&noiseLayerId="
        +"&utteranceTagLayerId=htk"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=segment"
        +"&scoreLayerId="
        +"&overlapThreshold=5"
        +"&cleanupOption=75"
        +"&noisePatterns=laugh.* unclear .*noise.*"
        +"&leftPattern="
        +"&rightPattern="
        +"&pauseMarkers=-");
      fail("Should fail with nonexistent pronunciationLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=word"
        +"&pronunciationLayerId=phonemes"
        +"&noiseLayerId=noise" // nonexistent
        +"&utteranceTagLayerId=htk"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=segment"
        +"&scoreLayerId="
        +"&overlapThreshold=5"
        +"&cleanupOption=75"
        +"&noisePatterns=laugh.* unclear .*noise.*"
        +"&leftPattern="
        +"&rightPattern="
        +"&pauseMarkers=-");
      fail("Should fail with nonexistent noiseLayerId");
    } catch (InvalidConfigurationException x) {
    }
  }
  
  @Test public void P2FA() throws Exception {
    annotator.setSessionName("P2FA");
    
    Graph f = fragment();
    Schema schema = f.getSchema();
    annotator.setSchema(schema);
    
    // layers are created as required
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&pronunciationLayerId=phonemes"
      +"&noiseLayerId="
      +"&utteranceTagLayerId=utterance_htk" // nonexistent
      +"&participantTagLayerId=participant_htk" // nonexistent
      +"&wordAlignmentLayerId=word"
      +"&phoneAlignmentLayerId=segment"
      +"&useP2FA=on"
      +"&scoreLayerId=score" // can't score with P2FA, so this should be ignored
      +"&overlapThreshold="
      +"&cleanupOption=100"
      +"&noisePatterns=laugh.* unclear .*noise.*"
      +"&leftPattern="
      +"&rightPattern="
      +"&pauseMarkers=-");
    Layer layer = annotator.getSchema().getLayer("utterance_htk");
    assertNotNull("utterance_htk layer created", layer);
    layer = annotator.getSchema().getLayer("participant_htk");
    assertNotNull("participant_htk layer created", layer);
    assertNull("no score layer created because we're using P2FA",
               annotator.getSchema().getLayer("score"));
    assertEquals("Main-Participant grouping",
                 "Transcript", annotator.getMainUtteranceGrouping());
    assertEquals("Other-Participant grouping",
                 "Transcript", annotator.getOtherUtteranceGrouping());

    final Vector<Graph> results = new Vector<Graph>();
    annotator.transformFragments(
      Arrays.stream(new Graph[] { f }), graph -> { results.add(graph); });
    
    assertEquals("One utterance " + results, 1, results.size());
    Graph aligned = results.elementAt(0);
    assertTrue("Original graph is edited", f == aligned);
    
    Annotation[] words = aligned.all("word");
    assertEquals("One word " + Arrays.asList(words), 1, words.length);
    Annotation word = words[0];
    assertEquals("Word label " + word, "statute", word.getLabel());
    
    Annotation[] phones = word.all("segment");
    assertEquals("Six phones " + Arrays.asList(phones), 6, phones.length);
    String[] labels = { "s", "t", "{", "J", "@", "t" };
    Double[] starts = { 11.1, 11.25, 11.28, 11.44, 11.620000000000001, 11.7 };
    for (int p = 0; p < phones.length; p++) {      
      assertEquals("DISC phone label " + p, labels[p], phones[p].getLabel());
      assertEquals("Phone start confidence " + p,
                   Constants.CONFIDENCE_AUTOMATIC,
                   phones[p].getStart().getConfidence().intValue());
      assertEquals("Phone end confidence " + p,
                   Constants.CONFIDENCE_AUTOMATIC,
                   phones[p].getEnd().getConfidence().intValue());
      assertEquals("Phone start " + p, starts[p], phones[p].getStart().getOffset());
      if (p > 0) {
        assertEquals("Phone start shared with previous end " + p,
                     phones[p-1].getEnd(), phones[p].getStart());
      }
    } // next phone    
    assertEquals("Last phone end", Double.valueOf(11.76), phones[5].getEnd().getOffset());
  }   

  @Test public void trainAndAlign() throws Exception {
    annotator.setSessionName("trainAndAlign");
    
    Graph f = fragment();
    Schema schema = f.getSchema();
    annotator.setSchema(schema);    
    
    // layers are created as required
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&pronunciationLayerId=phonemes"
      +"&noiseLayerId="
      +"&utteranceTagLayerId=utterance_htk" // nonexistent
      +"&participantTagLayerId=participant_htk" // nonexistent
      +"&wordAlignmentLayerId=word"
      +"&phoneAlignmentLayerId=segment"
      +"&scoreLayerId=score"
      +"&overlapThreshold="
      +"&cleanupOption=100"
      +"&noisePatterns=laugh.* unclear .*noise.*"
      +"&leftPattern="
      +"&rightPattern="
      +"&pauseMarkers=-");
    Layer layer = annotator.getSchema().getLayer("utterance_htk");
    assertNotNull("utterance_htk layer created", layer);
    layer = annotator.getSchema().getLayer("participant_htk");
    assertNotNull("participant_htk layer created", layer);
    layer = annotator.getSchema().getLayer("score");
    assertNotNull("score layer created", layer);
    assertEquals("Main-Participant grouping",
                 "Speaker", annotator.getMainUtteranceGrouping());
    assertEquals("Other-Participant grouping",
                 "Not Aligned", annotator.getOtherUtteranceGrouping());

    final Vector<Graph> results = new Vector<Graph>();
    // annotator.getStatusObservers().add(s -> System.out.println(s));
    annotator.transformFragments(
      Arrays.stream(new Graph[] { f }), graph -> { results.add(graph); });

    assertEquals("One utterance " + results, 1, results.size());
    Graph aligned = results.elementAt(0);
    assertTrue("Original graph is edited", f == aligned);
    
    Annotation[] words = aligned.all("word");
    assertEquals("One word " + Arrays.asList(words), 1, words.length);
    Annotation word = words[0];
    assertEquals("Word label " + word, "statute", word.getLabel());

    // don't bother testing offsets, as they'll be rubbish
    Annotation[] phones = word.all("segment");
    assertEquals("Six phones " + Arrays.asList(phones), 6, phones.length);
    String[] labels = { "s", "t", "{", "J", "@", "t" };
    for (int p = 0; p < phones.length; p++) {      
      assertEquals("DISC phone label " + p, labels[p], phones[p].getLabel());
      assertNotNull("Scored " + p, phones[p].first("score"));
      if (p > 0) {
        assertEquals("Phone start shared with previous end " + p,
                     phones[p-1].getEnd(), phones[p].getStart());
      }
    } // next phone    
  }   

  @Test public void graphTransform() throws Exception {
    annotator.setSessionName("graphTransform");
    //annotator.getStatusObservers().add(status->System.out.println(status));
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // layers are created as required
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&pronunciationLayerId=phonemes"
      +"&noiseLayerId="
      +"&utteranceTagLayerId=utterance_htk" // nonexistent
      +"&participantTagLayerId=participant_htk" // nonexistent
      +"&wordAlignmentLayerId=word"
      +"&phoneAlignmentLayerId=segment"
      +"&useP2FA=on"
      +"&scoreLayerId=score" // can't score with P2FA, so this should be ignored
      +"&overlapThreshold="
      +"&cleanupOption=100"
      +"&noisePatterns=laugh.* unclear .*noise.*"
      +"&leftPattern="
      +"&rightPattern="
      +"&pauseMarkers=-");
    Layer layer = annotator.getSchema().getLayer("utterance_htk");
    assertNotNull("utterance_htk layer created", layer);
    layer = annotator.getSchema().getLayer("participant_htk");
    assertNotNull("participant_htk layer created", layer);
    assertNull("no score layer created because we're using P2FA",
               annotator.getSchema().getLayer("score"));
    assertEquals("Main-Participant grouping",
                 "Transcript", annotator.getMainUtteranceGrouping());
    assertEquals("Other-Participant grouping",
                 "Transcript", annotator.getOtherUtteranceGrouping());

    g.trackChanges();
    annotator.transform(g);
        
    Annotation[] words = g.all("word");
    assertEquals("One word " + Arrays.asList(words), 1, words.length);
    Annotation word = words[0];
    assertEquals("Word label " + word, "statute", word.getLabel());
    
    Annotation[] phones = word.all("segment");
    assertEquals("Six phones " + Arrays.asList(phones), 6, phones.length);
    String[] labels = { "s", "t", "{", "J", "@", "t" };
    Double[] starts = { 11.1, 11.25, 11.28, 11.44, 11.620000000000001, 11.7 };
    for (int p = 0; p < phones.length; p++) {      
      assertEquals("DISC phone label " + p, labels[p], phones[p].getLabel());
      assertEquals("Phone is marked for addition " + p,
                   Change.Operation.Create, phones[p].getChange());
      assertEquals("Phone start confidence " + p,
                   Constants.CONFIDENCE_AUTOMATIC,
                   phones[p].getStart().getConfidence().intValue());
      assertEquals("Phone end confidence " + p,
                   Constants.CONFIDENCE_AUTOMATIC,
                   phones[p].getEnd().getConfidence().intValue());
      assertEquals("Phone start offset " + p,
                   starts[p], phones[p].getStart().getOffset());
      if (phones[p].getStart().isStartOn("word")) {
        assertEquals("Phone/word start is an update " + p,
                     Change.Operation.Update, phones[p].getStart().getChange());
      } else {
        assertEquals("Phone start is new " + p,
                     Change.Operation.Create, phones[p].getStart().getChange());
      }
      if (p > 0) {
        assertEquals("Phone start shared with previous end " + p,
                     phones[p-1].getEnd(), phones[p].getStart());
      }
    } // next phone    
    assertEquals("Last phone end", Double.valueOf(11.76), phones[5].getEnd().getOffset());
    assertEquals("word/phone start shared", word.getStart(), phones[0].getStart());
    assertEquals("word/phone end shared", word.getEnd(), phones[5].getEnd());
  }   

  @Test public void alignToPhraseLayers() throws Exception {
    annotator.setSessionName("alignToPhraseLayers");
    
    Graph f = fragment();
    Schema schema = f.getSchema();
    annotator.setSchema(schema);
    
    // layers are created as required
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&pronunciationLayerId=phonemes"
      +"&noiseLayerId="
      +"&utteranceTagLayerId=htk_utterance" // nonexistent
      +"&participantTagLayerId=htk_participant" // nonexistent
      +"&wordAlignmentLayerId=htk_word" // nonexistent
      +"&phoneAlignmentLayerId=htk_phone" // nonexistent
      +"&useP2FA=on"
      +"&scoreLayerId="
      +"&overlapThreshold="
      +"&cleanupOption=100"
      +"&noisePatterns=laugh.* unclear .*noise.*"
      +"&leftPattern="
      +"&rightPattern="
      +"&pauseMarkers=-");
    Layer layer = annotator.getSchema().getLayer("htk_utterance");
    assertNotNull("htk_utterance layer created", layer);
    layer = annotator.getSchema().getLayer("htk_participant");
    assertNotNull("htk_participant layer created", layer);
    layer = annotator.getSchema().getLayer("htk_word");
    assertNotNull("htk_word layer created", layer);
    assertEquals("htk_word phrase layer",
                 "turn", layer.getParentId());    
    assertFalse("htk_word not saturated",
                layer.getSaturated());    
    layer = annotator.getSchema().getLayer("htk_phone");
    assertNotNull("htk_phone layer created", layer);
    assertEquals("phone layer type", Constants.TYPE_STRING, layer.getType());
    assertEquals("htk_phone phrase layer",
                 "turn", layer.getParentId());    
    assertFalse("htk_phone not saturated",
                layer.getSaturated());    

    final Vector<Graph> results = new Vector<Graph>();
    annotator.transformFragments(
      Arrays.stream(new Graph[] { f }), graph -> { results.add(graph); });
    
    assertEquals("One utterance " + results, 1, results.size());
    Graph aligned = results.elementAt(0);
    assertTrue("Original graph is edited", f == aligned);
    
    Annotation[] words = aligned.all("word");
    assertEquals("One word " + Arrays.asList(words), 1, words.length);
    Annotation word = words[0];
    assertEquals("Word label " + word, "statute", word.getLabel());
    assertEquals("Word start not changed",
                 Double.valueOf(11.0), word.getStart().getOffset());
    assertEquals("Word start confidence not changed",
                 Constants.CONFIDENCE_DEFAULT, word.getStart().getConfidence().intValue());
    assertEquals("Word end not changed",
                 Double.valueOf(13.0), word.getEnd().getOffset());
    assertEquals("Word end confidence not changed",
                 Constants.CONFIDENCE_DEFAULT, word.getEnd().getConfidence().intValue());
    
    Annotation[] phones = word.all("segment");
    assertEquals("No phones " + Arrays.asList(phones), 0, phones.length);

    Annotation[] htk_words = aligned.all("htk_word");
    assertEquals("One HTK word " + Arrays.asList(htk_words), 1, htk_words.length);
    Annotation htk_word = htk_words[0];
    assertEquals("HTK Word label " + htk_word, "statute", htk_word.getLabel());
    
    Annotation[] htk_phones = aligned.all("htk_phone");
    assertEquals("Six HTK phones " + Arrays.asList(htk_phones), 6, htk_phones.length);
    String[] labels = { "S", "T", "AE1", "CH", "UW0", "T" };
    Double[] starts = { 11.1, 11.25, 11.28, 11.44, 11.620000000000001, 11.7 };
    for (int p = 0; p < htk_phones.length; p++) {      
      assertEquals("DISC phone label " + p, labels[p], htk_phones[p].getLabel());
      assertEquals("Phone start confidence " + p,
                   Constants.CONFIDENCE_AUTOMATIC,
                   htk_phones[p].getStart().getConfidence().intValue());
      assertEquals("Phone end confidence " + p,
                   Constants.CONFIDENCE_AUTOMATIC,
                   htk_phones[p].getEnd().getConfidence().intValue());
      assertEquals("Phone start " + p, starts[p], htk_phones[p].getStart().getOffset());
      if (p > 0) {
        assertEquals("Phone start shared with previous end " + p,
                     htk_phones[p-1].getEnd(), htk_phones[p].getStart());
      }
    } // next phone    
    assertEquals("Last phone end", Double.valueOf(11.76), htk_phones[5].getEnd().getOffset());
    assertEquals("word/phone start shared", htk_word.getStart(), htk_phones[0].getStart());
    assertEquals("word/phone end shared", htk_word.getEnd(), htk_phones[5].getEnd());
  }

  /**
   * Returns a fragment for annotating.
   * @return The graph for testing with.
   */
  public static Graph fragment() throws Exception {
    Graph g = graph();
    Schema schema = g.getSchema();
    Graph f = g.getFragment(
      10.0, 15.0, (String[])schema.getLayers().keySet().toArray(new String[0]));
    return f;
  }
  
  /**
   * Returns a graph for annotating.
   * @return The graph for testing with.
   */
  public static Graph graph() throws Exception {
    Schema schema = new Schema(
      "participant", "turn", "utterance", "word",
      new Layer("participant", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("participant").setParentIncludes(true),
      new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("htk", "HTK tag").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("phonemes", "Phonemic transcription").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true), // ARPABET layer
      new Layer("segment", "Phones").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true)
      .setType("ipa"));
    // annotate a graph
    Graph g = new Graph()
      .setSchema(schema);
    g.setId("BREY00538.TextGrid");
    Anchor start = g.getOrCreateAnchorAt(10, Constants.CONFIDENCE_MANUAL);
    Anchor end = g.getOrCreateAnchorAt(15, Constants.CONFIDENCE_MANUAL);
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
    
    Annotation statute = g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("statute")
      .setStart(g.getOrCreateAnchorAt(11, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(13, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn));
    g.addAnnotation(new Annotation().setLayerId("phonemes").setLabel("S T AE1 CH UW0 T")
                    .setParent(statute));

    // access to test media
    final File tempWav = File.createTempFile("TestHTKAligner-", ".wav");
    IO.Copy(new File(dir(), "BREY00538.wav"), tempWav);
    g.setMediaProvider(new GraphMediaProvider() {
        public MediaFile[] getAvailableMedia() throws StoreException, PermissionException {
          try {
            return new MediaFile[] { new MediaFile(tempWav) };
          } catch (Exception x) {
            throw new StoreException(x);
          }
        }
        public String getMedia(String trackSuffix, String mimeType) 
          throws StoreException, PermissionException {
          try {
            return getAvailableMedia()[0].getFile().toURL().toString();
          } catch (Exception x) {
            throw new StoreException(x);
          }
        }
        public GraphMediaProvider providerForGraph(Graph graph) {
          return this;
        }
      });

    return g;
  } // end of graph()

  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.annotator.htk.TestHTKAligner");
  }
}
