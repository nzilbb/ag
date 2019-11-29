//
// Copyright 2016-2019 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.ag.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.LinkedHashSet;
import java.util.Iterator;
import nzilbb.ag.*;

public class TestSchema
{
  @Test public void basicObjectInterrelation() 
  {
    Schema s = new Schema();

    s.addLayer(new Layer("topic", "Topics", Constants.ALIGNMENT_INTERVAL, 
                         true, // peers
                         false, // peersOverlap
                         false)); // saturated
    s.addLayer(new Layer("who", "Participants", Constants.ALIGNMENT_NONE, 
                         true, // peers
                         true, // peersOverlap
                         true)); // saturated
    s.addLayer(new Layer("turn", "Speaker turns", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         false, // peersOverlap
                         false, // saturated
                         "who", // parentId
                         true)); // parentIncludes
    s.addLayer(new Layer("utterance", "Utterances", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         false, // peersOverlap
                         true, // saturated
                         "turn", // parentId
                         true)); // parentIncludes
    s.addLayer(new Layer("word", "Words", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         false, // peersOverlap
                         false, // saturated
                         "turn", // parentId
                         true)); // parentIncludes
    s.addLayer(new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes

    s.setParticipantLayerId("who");
    s.setTurnLayerId("turn");
    s.setUtteranceLayerId("utterance");
    s.setWordLayerId("word");

    assertEquals("getLayer", "Topics", s.getLayer("topic").getDescription());
    assertEquals("getLayer", "Participants", s.getLayer("who").getDescription());
    assertEquals("getLayer", "Speaker turns", s.getLayer("turn").getDescription());
    assertEquals("getLayer", "Utterances", s.getLayer("utterance").getDescription());
    assertEquals("getLayer", "Words", s.getLayer("word").getDescription());
    assertEquals("getLayer", "Phones", s.getLayer("phone").getDescription());

    assertEquals("special layers", s.getLayer("who"), s.getParticipantLayer());
    assertEquals("special layers", s.getLayer("turn"), s.getTurnLayer());
    assertEquals("special layers", s.getLayer("utterance"), s.getUtteranceLayer());
    assertEquals("special layers", s.getLayer("word"), s.getWordLayer());
      
    assertEquals("hierarchy", s.getLayer("who"), s.getLayer("turn").getParent());
    assertEquals("hierarchy", s.getLayer("turn"), s.getLayer("word").getParent());
    assertEquals("hierarchy", s.getLayer("turn"), s.getLayer("utterance").getParent());
    assertEquals("hierarchy", s.getLayer("word"), s.getLayer("phone").getParent());

    assertEquals("graph layer", s.getRoot(), s.getLayer("graph"));

    assertEquals("hierarchy - top level", s.getRoot(), s.getLayer("who").getParent());
    assertEquals("hierarchy - top level", s.getRoot(), s.getLayer("topic").getParent());

    // don't let existing layers be replaced
    Layer originalLayer = s.getLayer("phone");
    Layer newLayer = new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL,
                               true, // peers
                               false, // peersOverlap
                               true, // saturated
                               "word", // parentId
                               true); // parentIncludes
    s.addLayer(newLayer);
    assertFalse("existing layers not replaced", s.getLayer("phone") == newLayer);
    assertTrue("existing layers not replaced", s.getLayer("phone") == originalLayer);
   
  }

  @Test public void arrayConstructor() 
  {
    Layer[] layers = {
      new Layer("topic", "Topics", Constants.ALIGNMENT_INTERVAL, 
                true, // peers
                false, // peersOverlap
                false), // saturated
      new Layer("who", "Participants", Constants.ALIGNMENT_NONE, 
                true, // peers
                true, // peersOverlap
                true), // saturated
      new Layer("turn", "Speaker turns", Constants.ALIGNMENT_INTERVAL,
                true, // peers
                false, // peersOverlap
                false, // saturated
                "who", // parentId
                true), // parentIncludes
      new Layer("utterance", "Utterances", Constants.ALIGNMENT_INTERVAL,
                true, // peers
                false, // peersOverlap
                true, // saturated
                "turn", // parentId
                true), // parentIncludes
      new Layer("word", "Words", Constants.ALIGNMENT_INTERVAL,
                true, // peers
                false, // peersOverlap
                false, // saturated
                "turn", // parentId
                true), // parentIncludes
      new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL,
                true, // peers
                false, // peersOverlap
                true, // saturated
                "word", // parentId
                true) // parentIncludes
    };
    Schema s = new Schema(layers, "who", "turn", "utterance", "word");
      
    assertEquals("getLayer", "Topics", s.getLayer("topic").getDescription());
    assertEquals("getLayer", "Participants", s.getLayer("who").getDescription());
    assertEquals("getLayer", "Speaker turns", s.getLayer("turn").getDescription());
    assertEquals("getLayer", "Utterances", s.getLayer("utterance").getDescription());
    assertEquals("getLayer", "Words", s.getLayer("word").getDescription());
    assertEquals("getLayer", "Phones", s.getLayer("phone").getDescription());
      
    assertEquals("special layers", s.getLayer("who"), s.getParticipantLayer());
    assertEquals("special layers", s.getLayer("turn"), s.getTurnLayer());
    assertEquals("special layers", s.getLayer("utterance"), s.getUtteranceLayer());
    assertEquals("special layers", s.getLayer("word"), s.getWordLayer());
      
    assertEquals("hierarchy", s.getLayer("who"), s.getLayer("turn").getParent());
    assertEquals("hierarchy", s.getLayer("turn"), s.getLayer("word").getParent());
    assertEquals("hierarchy", s.getLayer("turn"), s.getLayer("utterance").getParent());
    assertEquals("hierarchy", s.getLayer("word"), s.getLayer("phone").getParent());
      
    assertEquals("graph layer", s.getRoot(), s.getLayer("graph"));

    assertEquals("hierarchy - top level", s.getRoot(), s.getLayer("who").getParent());
    assertEquals("hierarchy - top level", s.getRoot(), s.getLayer("topic").getParent());      
  }

  @Test public void collectionConstructor() 
  {
    Vector<Layer> layers = new Vector<Layer>();
    layers.add(new Layer("topic", "Topics", Constants.ALIGNMENT_INTERVAL, 
                         true, // peers
                         false, // peersOverlap
                         false)); // saturated
    layers.add(new Layer("who", "Participants", Constants.ALIGNMENT_NONE, 
                         true, // peers
                         true, // peersOverlap
                         true)); // saturated
    layers.add(new Layer("turn", "Speaker turns", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         false, // peersOverlap
                         false, // saturated
                         "who", // parentId
                         true)); // parentIncludes
    layers.add(new Layer("utterance", "Utterances", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         false, // peersOverlap
                         true, // saturated
                         "turn", // parentId
                         true)); // parentIncludes
    layers.add(new Layer("word", "Words", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         false, // peersOverlap
                         false, // saturated
                         "turn", // parentId
                         true)); // parentIncludes
    layers.add(new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes
    Schema s = new Schema(layers, "who", "turn", "utterance", "word");
      
    assertEquals("getLayer", "Topics", s.getLayer("topic").getDescription());
    assertEquals("getLayer", "Participants", s.getLayer("who").getDescription());
    assertEquals("getLayer", "Speaker turns", s.getLayer("turn").getDescription());
    assertEquals("getLayer", "Utterances", s.getLayer("utterance").getDescription());
    assertEquals("getLayer", "Words", s.getLayer("word").getDescription());
    assertEquals("getLayer", "Phones", s.getLayer("phone").getDescription());
      
    assertEquals("special layers", s.getLayer("who"), s.getParticipantLayer());
    assertEquals("special layers", s.getLayer("turn"), s.getTurnLayer());
    assertEquals("special layers", s.getLayer("utterance"), s.getUtteranceLayer());
    assertEquals("special layers", s.getLayer("word"), s.getWordLayer());
      
    assertEquals("hierarchy", s.getLayer("who"), s.getLayer("turn").getParent());
    assertEquals("hierarchy", s.getLayer("turn"), s.getLayer("word").getParent());
    assertEquals("hierarchy", s.getLayer("turn"), s.getLayer("utterance").getParent());
    assertEquals("hierarchy", s.getLayer("word"), s.getLayer("phone").getParent());
      
    assertEquals("graph layer", s.getRoot(), s.getLayer("graph"));

    assertEquals("hierarchy - top level", s.getRoot(), s.getLayer("who").getParent());
    assertEquals("hierarchy - top level", s.getRoot(), s.getLayer("topic").getParent());      
  }

  @Test public void ellispisConstructor() 
  {
    Schema s = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("topic", "Topics", Constants.ALIGNMENT_INTERVAL, 
                true, // peers
                false, // peersOverlap
                false), // saturated
      new Layer("who", "Participants", Constants.ALIGNMENT_NONE, 
                true, // peers
                true, // peersOverlap
                true), // saturated
      new Layer("turn", "Speaker turns", Constants.ALIGNMENT_INTERVAL,
                true, // peers
                false, // peersOverlap
                false, // saturated
                "who", // parentId
                true), // parentIncludes
      new Layer("utterance", "Utterances", Constants.ALIGNMENT_INTERVAL,
                true, // peers
                false, // peersOverlap
                true, // saturated
                "turn", // parentId
                true), // parentIncludes
      new Layer("word", "Words", Constants.ALIGNMENT_INTERVAL,
                true, // peers
                false, // peersOverlap
                false, // saturated
                "turn", // parentId
                true), // parentIncludes
      new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL,
                true, // peers
                false, // peersOverlap
                true, // saturated
                "word", // parentId
                true) // parentIncludes
      );
      
    assertEquals("getLayer", "Topics", s.getLayer("topic").getDescription());
    assertEquals("getLayer", "Participants", s.getLayer("who").getDescription());
    assertEquals("getLayer", "Speaker turns", s.getLayer("turn").getDescription());
    assertEquals("getLayer", "Utterances", s.getLayer("utterance").getDescription());
    assertEquals("getLayer", "Words", s.getLayer("word").getDescription());
    assertEquals("getLayer", "Phones", s.getLayer("phone").getDescription());
      
    assertEquals("special layers", s.getLayer("who"), s.getParticipantLayer());
    assertEquals("special layers", s.getLayer("turn"), s.getTurnLayer());
    assertEquals("special layers", s.getLayer("utterance"), s.getUtteranceLayer());
    assertEquals("special layers", s.getLayer("word"), s.getWordLayer());
      
    assertEquals("hierarchy", s.getLayer("who"), s.getLayer("turn").getParent());
    assertEquals("hierarchy", s.getLayer("turn"), s.getLayer("word").getParent());
    assertEquals("hierarchy", s.getLayer("turn"), s.getLayer("utterance").getParent());
    assertEquals("hierarchy", s.getLayer("word"), s.getLayer("phone").getParent());
      
    assertEquals("graph layer", s.getRoot(), s.getLayer("graph"));

    assertEquals("hierarchy - top level", s.getRoot(), s.getLayer("who").getParent());
    assertEquals("hierarchy - top level", s.getRoot(), s.getLayer("topic").getParent());      
  }

  @Test public void cloning() 
  {
    Schema s = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("topic", "Topics", Constants.ALIGNMENT_INTERVAL, 
                true, // peers
                false, // peersOverlap
                false), // saturated
      new Layer("who", "Participants", Constants.ALIGNMENT_NONE, 
                true, // peers
                true, // peersOverlap
                true), // saturated
      new Layer("turn", "Speaker turns", Constants.ALIGNMENT_INTERVAL,
                true, // peers
                false, // peersOverlap
                false, // saturated
                "who", // parentId
                true), // parentIncludes
      new Layer("word", "Words", Constants.ALIGNMENT_INTERVAL,
                true, // peers
                false, // peersOverlap
                false, // saturated
                "turn", // parentId
                true), // parentIncludes
      new Layer("utterance", "Utterances", Constants.ALIGNMENT_INTERVAL,
                true, // peers
                false, // peersOverlap
                true, // saturated
                "turn", // parentId
                true), // parentIncludes
      new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL,
                true, // peers
                false, // peersOverlap
                true, // saturated
                "word", // parentId
                true) // parentIncludes
      );
    Schema c = (Schema)s.clone();

    // check structure and relations
    assertEquals("getLayer", "Topics", c.getLayer("topic").getDescription());
    assertEquals("getLayer", "Participants", c.getLayer("who").getDescription());
    assertEquals("getLayer", "Speaker turns", c.getLayer("turn").getDescription());
    assertEquals("getLayer", "Utterances", c.getLayer("utterance").getDescription());
    assertEquals("getLayer", "Words", c.getLayer("word").getDescription());
    assertEquals("getLayer", "Phones", c.getLayer("phone").getDescription());      
    assertEquals("special layers", c.getLayer("who"), s.getParticipantLayer());
    assertEquals("special layers", c.getLayer("turn"), s.getTurnLayer());
    assertEquals("special layers", c.getLayer("utterance"), s.getUtteranceLayer());
    assertEquals("special layers", c.getLayer("word"), s.getWordLayer());      
    assertEquals("hierarchy", c.getLayer("who"), s.getLayer("turn").getParent());
    assertEquals("hierarchy", c.getLayer("turn"), s.getLayer("word").getParent());
    assertEquals("hierarchy", c.getLayer("turn"), s.getLayer("utterance").getParent());
    assertEquals("hierarchy", c.getLayer("word"), s.getLayer("phone").getParent());      
    assertEquals("graph layer", c.getRoot(), s.getLayer("graph"));
    assertEquals("hierarchy - top level", c.getRoot(), c.getLayer("who").getParent());
    assertEquals("hierarchy - top level", c.getRoot(), c.getLayer("topic").getParent());

    // check Layer objects are copies
    for (Layer layerCopy : c.getLayers().values())
    {
      assertTrue("copy: " + layerCopy.getId(), layerCopy != s.getLayer(layerCopy.getId()));
      if (!layerCopy.getId().equals("graph"))
      {
        assertTrue("parent copy: " + layerCopy.getId(),
                   layerCopy.getParent() != s.getLayer(layerCopy.getParentId()));
      }
    } // next layer

    // check child order is preserved
    Iterator<String> turnChildren = c.getLayer("turn").getChildren().keySet().iterator();
    assertEquals("word first", "word", turnChildren.next());
    assertEquals("utterance last", "utterance", turnChildren.next());

    // check root child order is preserved
    Iterator<String> rootChildren = c.getRoot().getChildren().keySet().iterator();
    assertEquals("topic first", "topic", rootChildren.next());
    assertEquals("who last", "who", rootChildren.next());

  }

  public static void main(String args[]) 
  {
    org.junit.runner.JUnitCore.main("nzilbb.ag.test.TestSchema");
  }
}
