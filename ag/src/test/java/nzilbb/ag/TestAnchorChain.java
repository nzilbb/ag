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

package nzilbb.ag;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;
import nzilbb.ag.*;
import nzilbb.ag.util.*;
import nzilbb.util.Timers;

public class TestAnchorChain {
  
  /** Test all-null offsets are evenly spread through the duration. */
  @Test public void basicTraversal() {
    Graph g = new Graph();
    g.setId("my graph");

    g.setSchema(
      new Schema(
        "participant", "turn", "utterance", "word",
        new Layer("participant", "Participants")
        .setAlignment(Constants.ALIGNMENT_NONE)
        .setPeers(true).setPeersOverlap(true).setSaturated(true),
        new Layer("noise", "Noises")
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(true).setSaturated(false),
        new Layer("turn", "Speaker turns")
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false).setSaturated(false)
        .setParentId("participant").setParentIncludes(true),
        new Layer("utterance", "Utterance")
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false).setSaturated(true)
        .setParentId("turn").setParentIncludes(true),
        new Layer("language", "Phrase Language")
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false).setSaturated(false)
        .setParentId("turn").setParentIncludes(true),
        new Layer("word", "Words")
        .setAlignment(Constants.ALIGNMENT_INTERVAL)
        .setPeers(true).setPeersOverlap(false).setSaturated(false)
        .setParentId("turn").setParentIncludes(true)));
    
    g.addAnchor(new Anchor("turnStart", 0.0)); // turn start
    g.addAnchor(new Anchor("a0", 0.0)); // the
    g.addAnchor(new Anchor("a1", 1.0)); // quick
    g.addAnchor(new Anchor("a1.5", null)); // noise
    g.addAnchor(new Anchor("a2", null)); // brown
    g.addAnchor(new Anchor("a3", null)); // fox
    g.addAnchor(new Anchor("a4", null)); // jumps
    g.addAnchor(new Anchor("a5", null)); // over
    g.addAnchor(new Anchor("a6", null)); // a
    g.addAnchor(new Anchor("a7", null)); // perro
    g.addAnchor(new Anchor("a7.5", null)); // noise
    g.addAnchor(new Anchor("a8", 8.0)); // vago
    g.addAnchor(new Anchor("a9", 9.0)); // end of dog
    g.addAnchor(new Anchor("turnEnd", 9.0)); // turn end

    g.addAnnotation(
      new Annotation(
        "participant1", "john smith", "participant", "turnStart", "turnEnd", "my graph"));
    
    g.addAnnotation(
      new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "participant1"));
    g.addAnnotation(
      new Annotation("utt1", "john smith", "utterance", "turnStart", "turnEnd", "turn1"));
      
    g.addAnnotation(new Annotation("the", "the", "word", "a0", "a1", "turn1", 1));
    g.addAnnotation(new Annotation("quick", "quick", "word", "a1", "a1.5", "turn1", 2));
    g.addAnnotation(new Annotation("noise1", "noise1", "noise", "a1.5", "a2"));
    g.addAnnotation(new Annotation("brown", "brown", "word", "a2", "a3", "turn1", 3));
    g.addAnnotation(new Annotation("fox", "fox", "word", "a3", "a4", "turn1", 4));
    g.addAnnotation(new Annotation("jumps", "jumps", "word", "a4", "a5", "turn1", 5));
    g.addAnnotation(new Annotation("over", "over", "word", "a5", "a6", "turn1", 6));
    g.addAnnotation(new Annotation("a", "a", "word", "a6", "a7", "turn1", 7));
    g.addAnnotation(new Annotation("perro", "perro", "word", "a7", "a7.5", "turn1", 8));
    g.addAnnotation(new Annotation("noise2", "noise2", "noise", "a7.5", "a8"));
    g.addAnnotation(new Annotation("vago", "vago", "word", "a8", "a9", "turn1", 9));
    g.addAnnotation(new Annotation("Spanish", "es", "language", "a7", "a9", "turn1", 9));

    AnchorChain chain = AnchorChain.ChainForwardUntil(
      g.getAnchor("a5"), Arrays.asList("word"), null, a->a.getOffset() != null);    
    assertEquals("forward", "a6 a7 a7.5 a8",
                 chain.stream().map(a->a.getId()).collect(Collectors.joining(" ")));

    chain = AnchorChain.ChainBackwardUntil(
      g.getAnchor("a5"), Arrays.asList("word"), null, a->a.getOffset() != null);
    assertEquals("backward", "a1 a1.5 a2 a3 a4",
                 chain.stream().map(a->a.getId()).collect(Collectors.joining(" ")));
    
    chain = AnchorChain.ChainForwardUntil(
      g.getAnchor("a5"), Arrays.asList("word"), null, null);
    assertEquals("forward (no condition)", "a6 a7 a7.5 a8 a9",
                 chain.stream().map(a->a.getId()).collect(Collectors.joining(" ")));

    chain = AnchorChain.ChainBackwardUntil(
      g.getAnchor("a5"), Arrays.asList("word"), null, null);
    assertEquals("backward (no condition)", "a0 a1 a1.5 a2 a3 a4",
                 chain.stream().map(a->a.getId()).collect(Collectors.joining(" ")));

    chain = AnchorChain.ChainForwardUntil(
      g.getAnchor("a5"), Arrays.asList("language"), null, null);
    assertEquals("forward prioritising language", "a6 a7 a9",
                 chain.stream().map(a->a.getId()).collect(Collectors.joining(" ")));

    // with no preferred layers, it will follow the language path because "l" < "w"
    chain = AnchorChain.ChainForwardUntil(
      g.getAnchor("a5"), null, null, a->a.getOffset() != null);    
    assertEquals("forward (no preferred layers)", "a6 a7 a9",
                 chain.stream().map(a->a.getId()).collect(Collectors.joining(" ")));

    chain = AnchorChain.ChainBackwardUntil(
      g.getAnchor("a5"), null, null, a->a.getOffset() != null);
    assertEquals("backward (no preferred layers)", "a1 a1.5 a2 a3 a4",
                 chain.stream().map(a->a.getId()).collect(Collectors.joining(" ")));
    
  }
}
