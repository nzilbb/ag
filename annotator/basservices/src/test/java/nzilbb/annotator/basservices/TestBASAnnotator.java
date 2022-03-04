//
// Copyright 2022 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.annotator.basservices;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
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
import nzilbb.util.IO;

public class TestBASAnnotator {

  static BASAnnotator annotator = new BASAnnotator();

  /** Infer the directory of the tests. */
  public static File dir() throws Exception { 
    URL urlThisClass = TestBASAnnotator.class.getResource(
      TestBASAnnotator.class.getSimpleName() + ".class");
    File fThisClass = new File(urlThisClass.toURI());
    return fThisClass.getParentFile();
  }

  
  /** Ensure default (null) task parameters return an error. */
  @Test public void defaultParameters() throws Exception {
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    //annotator.getStatusObservers().add(status->System.out.println(status));
    
    try { // use default configuration
      annotator.setTaskParameters(null);
      fail("Should fail with default configuration");
    } catch (InvalidConfigurationException x) {
    }
  }   

  /** Ensure valid MAUSBasic parameters don't raise errors, and change the schema when
   * appropriate.*/ 
  @Test public void MAUSBasicParameters() throws Exception {
    
    Graph g = graph();
    Schema schema = g.getSchema();
    // set phonemes layer to type=ipa so we can test the type is copied
    schema.getLayer("phonemes").setType(Constants.TYPE_IPA);
    annotator.setSchema(schema);

    // configure for system layer update
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&service=MAUSBasic"
      +"&phonemeEncoding=disc"
      +"&language=en-NZ"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&utteranceTagLayerId=mausBasic"
      +"&participantTagLayerId="
      +"&wordAlignmentLayerId=word"
      +"&phoneAlignmentLayerId=segment");
    
    // layers are created as required
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&service=MAUSBasic"
      +"&phonemeEncoding=disc"
      +"&language=en-NZ"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&utteranceTagLayerId=utterance_bas"     // nonexistent
      +"&participantTagLayerId=participant_bas" // nonexistent
      +"&wordAlignmentLayerId=word_alignment"   // nonexistent
      +"&phoneAlignmentLayerId=phone");         // nonexistent
    Layer layer = annotator.getSchema().getLayer("utterance_bas");
    assertNotNull("utterance_bas layer created", layer);
    layer = annotator.getSchema().getLayer("participant_bas");
    assertNotNull("participant_bas layer created", layer);
    //TODO layer = annotator.getSchema().getLayer("word_alignment");
    // assertNotNull("word_alignment layer created", layer);
    layer = annotator.getSchema().getLayer("phone");
    assertNotNull("phone layer created", layer);
    assertEquals("phone layer type", Constants.TYPE_IPA, layer.getType());
    assertTrue("phone layer peers", layer.getPeers());
  }   

  /** Ensure that invalid task parameters generate errors. */
  @Test public void setInvalidTaskParameters() throws Exception {
    
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=nonexistent"
        +"&service=MAUSBasic"
        +"&phonemeEncoding=disc"
        +"&language=en-NZ"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&utteranceTagLayerId=mausBasic"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=segment");
      fail("Should fail with nonexistent orthographyLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=word"
        +"&service=MAUSBasic"
        +"&phonemeEncoding=disc"
        +"&language=en-NZ"
        +"&transcriptLanguageLayerId=nonexistent"
        +"&utteranceTagLayerId=mausBasic"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=segment");
      fail("Should fail with nonexistent transcriptLanguageLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=word"
        +"&service="
        +"&phonemeEncoding=disc"
        +"&language=en-NZ"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&utteranceTagLayerId=mausBasic"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=segment");
      fail("Should fail with no service");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=word"
        +"&service=invalid"
        +"&phonemeEncoding=disc"
        +"&language=en-NZ"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&utteranceTagLayerId=mausBasic"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=segment");
      fail("Should fail with invalid service");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=word"
        +"&service=MAUSBasic"
        +"&phonemeEncoding=disc"
        +"&language=en-NZ"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&utteranceTagLayerId=mausBasic"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId="
        +"&phoneAlignmentLayerId=segment");
      fail("Should fail with no wordAlignmentLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=word"
        +"&service=MAUSBasic"
        +"&phonemeEncoding=disc"
        +"&language=en-NZ"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&utteranceTagLayerId=mausBasic"
        +"&participantTagLayerId="
        +"&wordAlignmentLayerId=word"
        +"&phoneAlignmentLayerId=");
      fail("Should fail with no phoneAlignmentLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "orthographyLayerId=word"
        +"&service=G2P"
        +"&phonemeEncoding=disc"
        +"&language=en-NZ"
        +"&transcriptLanguageLayerId=transcript_language"
        +"&utteranceTagLayerId=mausBasic"
        +"&pronunciationLayerId=");
      fail("Should fail with no pronunciationLayerId");
    } catch (InvalidConfigurationException x) {
    }
  }

  /** Ensure MAUSBasic produces alignments. */ 
  @Test public void MAUSBasic() throws Exception {
    
    Graph f = fragment();
    Schema schema = f.getSchema();
    // set phonemes layer to type=ipa so we can test the type is copied
    schema.getLayer("phonemes").setType(Constants.TYPE_IPA);
    annotator.setSchema(schema);
    annotator.getStatusObservers().add(status->System.out.println(status));

    assertNotNull("fragment has a language",
                  f.sourceGraph().first("transcript_language"));
    assertEquals("fragment language correct",
                 "en-NZ", f.sourceGraph().first("transcript_language").getLabel());
    assertEquals("text is correct",
                 "saved up some money he bought property",
                 Arrays.stream(f.labels(schema.getWordLayerId()))
                 .collect(Collectors.joining(" ")).trim());
    
    // configure for system layer update
    annotator.setTaskParameters(
      "orthographyLayerId=word"
      +"&service=MAUSBasic"
      +"&phonemeEncoding=disc"
      +"&language=en-NZ"
      +"&transcriptLanguageLayerId=transcript_language"
      +"&utteranceTagLayerId=mausBasic"
      +"&participantTagLayerId="
      +"&wordAlignmentLayerId=word"
      +"&phoneAlignmentLayerId=segment");
    
    final Vector<Graph> results = new Vector<Graph>();
    annotator.transformFragments(
      Arrays.stream(new Graph[] { f }), graph -> { results.add(graph); });
    
    assertEquals("One utterance " + results, 1, results.size());
    Graph aligned = results.elementAt(0);
    assertTrue("Original graph is edited", f == aligned);
    
    Annotation[] words = aligned.all("word");
    assertEquals("Seven words " + Arrays.asList(words), 7, words.length);
    
    Annotation[] phones = aligned.all("segment");
    assertEquals("24 phones " + Arrays.asList(phones), 24, phones.length);
    String[] labels = {
      "s", "1", "v", "d",
      "@", "p",
      "s", "@", "m",
      "m", "@", "n", "i",
      "i",
      "b", "$", "t",
      "p", "r", "Q", "p", "@", "L", "i" };
    for (int p = 0; p < phones.length; p++) {      
      assertEquals("DISC phone label " + p, labels[p], phones[p].getLabel());
      if (p > 0) { // first phone might coincide with start and be CONFIDENCE_MANUAL
        assertEquals("Phone start confidence " + p + " " + phones[p].getStartId(),
                     Constants.CONFIDENCE_AUTOMATIC,
                     phones[p].getStart().getConfidence().intValue());
      }
      if (p < phones.length - 1) { // last phone might coincide with end and be CONFIDENCE_MANUAL
        assertEquals("Phone end confidence " + p + " " + phones[p].getEndId(),
                     Constants.CONFIDENCE_AUTOMATIC,
                     phones[p].getEnd().getConfidence().intValue());
      }
      // TODO if (p > 0) {
      //   assertEquals("Phone start shared with previous end " + p,
      //                phones[p-1].getEnd(), phones[p].getStart());
      // }
    } // next phone    
  }   
  
  /**
   * Returns a fragment for annotating.
   * @return The graph for testing with.
   */
  public static Graph fragment() throws Exception {
    Graph g = graph();
    Schema schema = g.getSchema();
    Graph f = g.getFragment(
      10.0, 20.0, (String[])schema.getLayers().keySet().toArray(new String[0]));
    return f;
  }
  
  /**
   * Returns a graph for annotating.
   * @return The graph for testing with.
   */
  public static Graph graph() throws Exception {
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
      new Layer("mausBasic", "MAUSBasic tag").setAlignment(Constants.ALIGNMENT_INTERVAL)
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
      .setType(Constants.TYPE_IPA));
    // annotate a graph
    Graph g = new Graph()
      .setSchema(schema);
    g.setId("Test.TextGrid");
    Anchor start = g.getOrCreateAnchorAt(10, Constants.CONFIDENCE_MANUAL);
    Anchor end = g.getOrCreateAnchorAt(20, Constants.CONFIDENCE_MANUAL);
    g.addAnnotation(
      new Annotation().setLayerId("transcript_language").setLabel("en-NZ")
      .setStart(start).setEnd(end));
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
    
    g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("saved")
      .setStart(g.getOrCreateAnchorAt(11, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(12, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn).setOrdinal(1));
    g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("up")
      .setStart(g.getOrCreateAnchorAt(12, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(13, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn).setOrdinal(2));
    g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("some")
      .setStart(g.getOrCreateAnchorAt(13, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(14, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn).setOrdinal(3));
    g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("money")
      .setStart(g.getOrCreateAnchorAt(14, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(15, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn).setOrdinal(4));
    g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("he")
      .setStart(g.getOrCreateAnchorAt(15, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(16, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn).setOrdinal(5));
    g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("bought")
      .setStart(g.getOrCreateAnchorAt(16, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(17, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn).setOrdinal(6));
    g.addAnnotation(
      new Annotation().setLayerId("word").setLabel("property")
      .setStart(g.getOrCreateAnchorAt(17, Constants.CONFIDENCE_DEFAULT))
      .setEnd(g.getOrCreateAnchorAt(18, Constants.CONFIDENCE_DEFAULT))
      .setParent(turn).setOrdinal(7));
    
    // access to test media
    final File tempWav = File.createTempFile("TestBASAnnotator-", ".wav");
    IO.Copy(new File(dir(), "test.wav"), tempWav);
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
    org.junit.runner.JUnitCore.main("nzilbb.annotator.basservices.TestBASAnnotator");
  }
}
