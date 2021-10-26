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
    layer = annotator.getSchema().getLayer("word_alignment");
    assertNotNull("word_alignment layer created", layer);
    layer = annotator.getSchema().getLayer("phone");
    assertNotNull("phone layer created", layer);
    layer = annotator.getSchema().getLayer("score");
    assertNotNull("score layer created", layer);
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

    final Vector<Graph> results = new Vector<Graph>();
    annotator.transformFragments(
      Arrays.stream(new Graph[] { g }), graph -> { results.add(graph); });
    
    assertEquals("One utterance " + results, 1, results.size());
    Graph aligned = results.elementAt(0);
    assertTrue("Original graph is edited", g == aligned);
    
    Annotation[] words = aligned.all("word");
    assertEquals("One word " + Arrays.asList(words), 1, words.length);
    Annotation word = words[0];
    assertEquals("Word label " + word, "statute", word.getLabel());
    
    Annotation[] phones = word.all("segment");
    assertEquals("Six phones " + Arrays.asList(phones), 6, phones.length);
    String[] labels = { "s", "t", "{", "J", "u", "t" };
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
    annotator.transformFragments(
      Arrays.stream(new Graph[] { g }), graph -> { results.add(graph); });

    assertEquals("One utterance " + results, 1, results.size());
    Graph aligned = results.elementAt(0);
    assertTrue("Original graph is edited", g == aligned);
    
    Annotation[] words = aligned.all("word");
    assertEquals("One word " + Arrays.asList(words), 1, words.length);
    Annotation word = words[0];
    assertEquals("Word label " + word, "statute", word.getLabel());

    // don't bother testing offsets, as they'll be rubbish
    Annotation[] phones = word.all("segment");
    assertEquals("Six phones " + Arrays.asList(phones), 6, phones.length);
    String[] labels = { "s", "t", "{", "J", "u", "t" };
    for (int p = 0; p < phones.length; p++) {      
      assertEquals("DISC phone label " + p, labels[p], phones[p].getLabel());
      assertNotNull("Scored " + p, phones[p].first("score"));
      if (p > 0) {
        assertEquals("Phone start shared with previous end " + p,
                     phones[p-1].getEnd(), phones[p].getStart());
      }
    } // next phone    
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
      .setParentId("word").setParentIncludes(true));
    schema.getLayer("segment").put("subtype", "D"); // DISC layer 
    // annotate a graph
    Graph g = new Graph()
      .setSchema(schema);
    g.setId("BREY00538.TextGrid");
    Anchor start = g.getOrCreateAnchorAt(10, Constants.CONFIDENCE_MANUAL);
    Anchor end = g.getOrCreateAnchorAt(14, Constants.CONFIDENCE_MANUAL);
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

    Graph fragment = g.getFragment(
      10.0, 14.0, (String[])schema.getLayers().keySet().toArray(new String[0]));
    fragment.setSchema(schema); // ensure subtype is available
    return fragment;
  } // end of graph()

  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.annotator.htk.TestHTKAligner");
  }
}
