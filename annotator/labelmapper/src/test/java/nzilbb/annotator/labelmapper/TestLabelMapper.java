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

package nzilbb.annotator.labelmapper;
	      
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

public class TestLabelMapper {

  @Test public void defaultParameters() throws Exception {
    LabelMapper annotator = new LabelMapper();
    
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
    LabelMapper annotator = new LabelMapper();
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // layers are created as required
    annotator.setTaskParameters(
      "labelLayerId=orthography"
      +"&tokenLayerId=phone"
      +"&comparator=OrthographyToDISC"
      +"&mappingLayerId=disc"); // nonexistent
    Layer layer = annotator.getSchema().getLayer("disc");
    assertNotNull("disc layer created", layer);
  }   
  
  @Test public void setInvalidTaskParameters() throws Exception {
    LabelMapper annotator = new LabelMapper();
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    try {
      annotator.setTaskParameters(
        "labelLayerId=nonexistent"
        +"&tokenLayerId=phone"
        +"&comparator=OrthographyToDISC"
        +"&mappingLayerId=disc"); // nonexistent
      fail("Should fail with nonexistent labelLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "labelLayerId=orthography"
        +"&tokenLayerId=nonexistent"
        +"&comparator=OrthographyToDISC"
        +"&mappingLayerId=disc"); // nonexistent
      fail("Should fail with nonexistent tokenLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "labelLayerId=orthography"
        +"&tokenLayerId=phone"
        +"&comparator="
        +"&mappingLayerId=disc"); // nonexistent
      fail("Should fail with no comparator");
    } catch (InvalidConfigurationException x) {
    }
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
         new Layer("lang", "Phrase Language").setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false)
         .setParentId("turn").setParentIncludes(true),
         new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false)
         .setParentId("turn").setParentIncludes(true),
         new Layer("orthography", "Orthography").setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true)
         .setParentId("word").setParentIncludes(true),
         new Layer("phone", "Phones").setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(true)
         .setParentId("word").setParentIncludes(true));
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

      // TODO add phones
      return g;
  } // end of graph()

  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.annotator.labelmapper.TestLabelMapper");
  }
}
