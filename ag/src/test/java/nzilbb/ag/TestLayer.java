//
// Copyright 2015-2020 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.ag;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.json.JsonObject;
import nzilbb.ag.*;

public class TestLayer 
{
  @Test public void basicAttributes() 
  {
    Layer l = new Layer("word", "Words")
      .setParentId("turn")
      .setAlignment(2)
      .setPeers(true)
      .setPeersOverlap(false)
      .setParentIncludes(true)
      .setSaturated(false);
    assertEquals("word", l.getId());
    assertEquals("Words", l.getDescription());
    assertEquals("turn", l.getParentId());
    assertEquals(2, l.getAlignment());
    assertEquals(true, l.getPeers());
    assertEquals(false, l.getPeersOverlap());
    assertEquals(true, l.getParentIncludes());
    assertEquals(false, l.getSaturated());

    // attribute constructor
    l = new Layer("word", "Words", 2, true, false, false, "turn", true);
    assertEquals("word", l.getId());
    assertEquals("Words", l.getDescription());
    assertEquals("turn", l.getParentId());
    assertEquals(2, l.getAlignment());
    assertEquals(true, l.getPeers());
    assertEquals(false, l.getPeersOverlap());
    assertEquals(true, l.getParentIncludes());
    assertEquals(false, l.getSaturated());
  }

  @Test public void extendedAttributes() 
  {
    Layer l = new Layer();
    l.setId("word");
    l.setDescription("Words");
    l.setParentId("turn");
    l.setAlignment(2);
    l.setPeers(true);
    l.setPeersOverlap(false);
    l.setParentIncludes(true);
    l.setSaturated(false);
    l.put("foo", "bar");
    assertEquals("bar", l.get("foo"));
  }

  @Test public void objectAttributes() 
  {
    Layer l = new Layer();
    l.setId("word");
    int iStartHashCode = l.hashCode();
    l.setDescription("Words")
      .setParentId("turn")
      .setAlignment(2)
      .setPeers(true)
      .setPeersOverlap(false)
      .setParentIncludes(true)
      .setSaturated(false)
      .put("foo", "bar");
    int iEndHashCode = l.hashCode();
    assertEquals("Immutable hashcode:", iStartHashCode, iEndHashCode);
      
    assertTrue("Equality reflexive", l.equals(l));
    Layer l2 = new Layer();
    assertFalse("equals before id set:", l.equals(l2));
    l2.setId("word");
    l2.setDescription("Words");
    l2.setParentId("turn");
    l2.setAlignment(2);
    l2.setPeers(true);
    l2.setPeersOverlap(false);
    l2.setParentIncludes(true);
    l2.setSaturated(false);
    // no "foo" attribute, to ensure it doesn't contribute to equality
    assertTrue("id defines equality:", l.equals(l2));
    assertTrue("Equality is symmetric:", l2.equals(l));
      
    l2.setDescription("new description");
    assertTrue("description doesn't affect equality:", l.equals(l2));
    l2.setParentId("new parent");
    assertTrue("parent doesn't affect equality:", l.equals(l2));
    l2.setAlignment(1);
    assertTrue("alignment doesn't affect equality:", l.equals(l2));
    l2.setPeers(!l2.getPeers());
    assertTrue("peers doesn't affect equality:", l.equals(l2));
    l2.setPeersOverlap(!l2.getPeersOverlap());
    assertTrue("peersOverlap doesn't affect equality:", l.equals(l2));
    l2.setParentIncludes(!l2.getParentIncludes());
    assertTrue("parentIncludes doesn't affect equality:", l.equals(l2));
    l2.setSaturated(!l2.getSaturated());
    assertTrue("saturated doesn't affect equality:", l.equals(l2));
		  
    l2.setId("different id");
    assertFalse("Different id:", l.equals(l2));
    l2.setId(l.getId());
    assertTrue("Resetting attribute resets equality:", l.equals(l2));

    LinkedHashMap<String,Object> aMap = new LinkedHashMap<String,Object>();
    aMap.putAll(l);
    assertFalse("A map with the same attributes isn't equal:", l.equals(aMap));
  }

  @Test public void parentChild() 
  {
    Layer turn = new Layer();
    turn.setId("turn");
    turn.setDescription("Speaker turns");
    turn.setAlignment(2);
    turn.setPeers(true);
    turn.setPeersOverlap(true);
    turn.setParentIncludes(true);
    turn.setSaturated(false);

    Layer word = new Layer();
    word.setId("word");
    word.setDescription("Words");
    word.setParent(turn);
    word.setAlignment(2);
    word.setPeers(true);
    word.setPeersOverlap(false);
    word.setParentIncludes(true);
    word.setSaturated(false);

    assertEquals("turn", turn.getId());
    assertEquals("Speaker turns", turn.getDescription());
    assertNull(turn.getParentId());
    assertNull(turn.getParent());
    assertEquals(2, turn.getAlignment());
    assertEquals(true, turn.getPeers());
    assertEquals(true, turn.getPeersOverlap());
    assertEquals(true, turn.getParentIncludes());
    assertEquals(false, turn.getSaturated());

    assertEquals("word", word.getId());
    assertEquals("Words", word.getDescription());
    assertEquals("turn", word.getParentId());
    assertEquals(turn, word.getParent());
    assertEquals(word, turn.getChildren().get("word"));
    assertEquals(2, word.getAlignment());
    assertEquals(true, word.getPeers());
    assertEquals(false, word.getPeersOverlap());
    assertEquals(true, word.getParentIncludes());
    assertEquals(false, word.getSaturated());
  }

  @Test public void hierarchy() 
  {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");

    g.addLayer(new Layer("topic", "Topics", Constants.ALIGNMENT_INTERVAL, 
                         true, // peers
                         false, // peersOverlap
                         false)); // saturated
    g.addLayer(new Layer("who", "Participants", Constants.ALIGNMENT_NONE, 
                         true, // peers
                         true, // peersOverlap
                         true)); // saturated
    g.addLayer(new Layer("turn", "Speaker turns", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         false, // peersOverlap
                         false, // saturated
                         "who", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("utterance", "Utterances", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         false, // peersOverlap
                         true, // saturated
                         "turn", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("word", "Words", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         false, // peersOverlap
                         false, // saturated
                         "turn", // parentId
                         true)); // parentIncludes
    g.addLayer(new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL,
                         true, // peers
                         false, // peersOverlap
                         true, // saturated
                         "word", // parentId
                         true)); // parentIncludes

    LinkedHashSet<Layer> ancestors = g.getLayer("phone").getAncestors();
    assertTrue("ancestor parent", ancestors.contains(g.getLayer("word")));
    assertTrue("ancestor grandparent", ancestors.contains(g.getLayer("turn")));
    assertTrue("ancestor great grandparent", ancestors.contains(g.getLayer("who")));
    assertTrue("ancestor graph", ancestors.contains(g.getLayer("transcript"))); 
    assertFalse("ancestor not self", ancestors.contains(g.getLayer("phone")));
    assertFalse("ancestor not grandparet peer", ancestors.contains(g.getLayer("utterance")));
    assertFalse("ancestor not other top-level layer", ancestors.contains(g.getLayer("topic")));

    assertEquals("FirstCommonAncestor distal", g.getLayer("turn"), 
                 g.getLayer("phone").getFirstCommonAncestor(g.getLayer("utterance")));
    assertEquals("FirstCommonAncestor distal", g.getLayer("turn"), 
                 g.getLayer("utterance").getFirstCommonAncestor(g.getLayer("phone")));
    assertEquals("FirstCommonAncestor peer", g.getLayer("turn"), 
                 g.getLayer("word").getFirstCommonAncestor(g.getLayer("utterance")));
    assertEquals("FirstCommonAncestor peer", g.getLayer("turn"), 
                 g.getLayer("utterance").getFirstCommonAncestor(g.getLayer("word")));
    assertEquals("FirstCommonAncestor ancestor", g.getLayer("turn"), 
                 g.getLayer("word").getFirstCommonAncestor(g.getLayer("turn")));
    assertEquals("FirstCommonAncestor descendant", g.getLayer("turn"), 
                 g.getLayer("turn").getFirstCommonAncestor(g.getLayer("word")));
    assertEquals("FirstCommonAncestor unrelated", g.getLayer(), 
                 g.getLayer("word").getFirstCommonAncestor(g.getLayer("topic")));
    assertEquals("FirstCommonAncestor unrelated", g.getLayer(), 
                 g.getLayer("topic").getFirstCommonAncestor(g.getLayer("word")));
    assertEquals("FirstCommonAncestor reflexive", g.getLayer("word"), 
                 g.getLayer("word").getFirstCommonAncestor(g.getLayer("word")));
    assertNull("FirstCommonAncestor with null", 
               g.getLayer("phone").getFirstCommonAncestor(null));

    assertEquals("descendant depth - phone", 0, g.getLayer("phone").getDescendantDepth());
    assertEquals("descendant depth - word", 1, g.getLayer("word").getDescendantDepth());
    assertEquals("descendant depth - utterance", 0, g.getLayer("utterance").getDescendantDepth());
    assertEquals("descendant depth - turn", 2, g.getLayer("turn").getDescendantDepth());
    assertEquals("descendant depth - who", 3, g.getLayer("who").getDescendantDepth());
    assertEquals("descendant depth - topic", 0, g.getLayer("topic").getDescendantDepth());

    assertTrue("isAncestor", g.getLayer("word").isAncestor("turn"));
    assertTrue("isAncestor", g.getLayer("word").isAncestor("transcript"));
    assertFalse("isAncestor - peer", g.getLayer("word").isAncestor("utterance"));
    assertFalse("isAncestor - descendant", g.getLayer("word").isAncestor("phone"));
    assertFalse("isAncestor - reflexive", g.getLayer("word").isAncestor("word"));

    assertFalse("isDescendant", g.getLayer("word").isDescendant("turn"));
    assertFalse("isDescendant", g.getLayer("word").isDescendant("transcript"));
    assertFalse("isDescendant - peer", g.getLayer("word").isDescendant("utterance"));
    assertTrue("isDescendant - descendant", g.getLayer("word").isDescendant("phone"));
    assertFalse("isDescendant - reflexive", g.getLayer("word").isDescendant("word"));

    Set<Layer> descendants = g.getLayer("turn").getDescendants();
    assertTrue("descendants of turn: word", descendants.contains(g.getLayer("word")));
    assertTrue("descendants of turn: phone", descendants.contains(g.getLayer("phone")));
    assertTrue("descendants of turn: phone", descendants.contains(g.getLayer("utterance")));
    assertFalse("descendants of turn: not itself", descendants.contains(g.getLayer("turn")));
    assertFalse("descendants of turn: not who", descendants.contains(g.getLayer("who")));
    assertFalse("descendants of turn: not topic", descendants.contains(g.getLayer("topic")));
    assertFalse("descendants of turn: not graph", descendants.contains(g.getLayer("graph")));
  }

  @Test public void cloning() 
  {
     Layer l = new Layer("word", "Words")
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true)
        .setPeersOverlap(false)
        .setSaturated(false)
        .setParentId("turn")
        .setParentIncludes(true);
    l.getValidLabels().put("bar", "bar");
    l.put("foo", "foo");
    Layer c = (Layer)l.clone();
    assertEquals("word", c.getId());
    assertEquals("Words", c.getDescription());
    assertEquals(Constants.ALIGNMENT_INTERVAL, c.getAlignment());
    assertEquals(true, c.getPeers());
    assertEquals(false, c.getPeersOverlap());
    assertEquals(false, c.getSaturated());
    assertEquals("turn", c.getParentId());
    assertEquals(true, c.getParentIncludes());
    assertTrue(c.getValidLabels().containsKey("bar"));     
    assertFalse(c.containsKey("foo"));     
  }

  @Test public void toJson() 
  {
     Layer l = new Layer("word", "Words")
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true)
        .setPeersOverlap(false)
        .setSaturated(false)
        .setParentId("turn")
        .setParentIncludes(true);
     l.getValidLabels().put("bar", "bar");
     l.put("foo", "foo");
     l.put("@not", "not");
     JsonObject j = l.toJson();
     assertEquals("word", j.getString("id"));
     assertEquals("Words", j.getString("description"));
     assertEquals(Constants.ALIGNMENT_INTERVAL, j.getJsonNumber("alignment").intValue());
     assertEquals(true, j.getBoolean("peers"));
     assertEquals(false, j.getBoolean("peersOverlap"));
     assertEquals(false, j.getBoolean("saturated"));
     assertEquals("turn", j.getString("parentId"));
     assertEquals(true, j.getBoolean("parentIncludes"));
     assertEquals("validLabels", true, j.containsKey("validLabels"));
     JsonObject jsonLabels = j.getJsonObject("validLabels");
     assertTrue("validLabels value", jsonLabels.containsKey("bar"));     
     assertTrue("cloneable entry included", j.containsKey("foo"));     
     assertFalse("transient entry excluded", j.containsKey("@not"));     
  }

  public static void main(String args[]) 
  {
    org.junit.runner.JUnitCore.main("nzilbb.ag.TestLayer");
  }
}
