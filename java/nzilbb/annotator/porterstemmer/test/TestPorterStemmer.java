//
// Copyright 2020 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.annotator.porterstemmer.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Iterator;
import java.util.stream.Collectors;
import nzilbb.ag.Schema;
import nzilbb.ag.Graph;
import nzilbb.ag.Layer;
import nzilbb.ag.Anchor;
import nzilbb.ag.Annotation;
import nzilbb.ag.Constants;
import nzilbb.annotator.porterstemmer.PorterStemmer;

public class TestPorterStemmer {
   
   @Test public void transform() throws Exception {
      
      PorterStemmer annotator = new PorterStemmer();
      Schema schema = new Schema(
            "who", "turn", "utterance", "word",
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
      annotator.setSchema(schema);
      
      // stem to a new layer
      annotator.setTaskParameters("tokenLayerId=word&stemLayerId=stem");
      
      assertEquals("token layer",
                   "word", annotator.getTokenLayerId());
      assertEquals("stem layer",
                   "stem", annotator.getStemLayerId());
      assertNotNull("stem layer was created",
                    schema.getLayer(annotator.getStemLayerId()));
      assertEquals("stem layer child of word",
                    "word", schema.getLayer(annotator.getStemLayerId()).getParentId());
      assertEquals("stem layer no aligned",
                   Constants.ALIGNMENT_NONE,
                   schema.getLayer(annotator.getStemLayerId()).getAlignment());
      String[] layers = annotator.getRequiredLayers();
      assertEquals("1 required layer: "+Arrays.asList(layers),
                   1, layers.length);
      assertEquals("required layer correct "+Arrays.asList(layers),
                   "word", layers[0]);
      layers = annotator.getOutputLayers();
      assertEquals("1 output layer: "+Arrays.asList(layers),
                   1, layers.length);
      assertEquals("output layer correct "+Arrays.asList(layers),
                   "stem", layers[0]);

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
         .setParent(g.my("participant")));
      g.addAnnotation(
         new Annotation().setLayerId("utterance").setLabel("someone")
         .setStart(start).setEnd(end)
         .setParent(turn));
      
      Annotation firstWord
         = g.addAnnotation(new Annotation().setLayerId("word").setLabel("I")
                           .setStart(g.getOrCreateAnchorAt(10)).setEnd(g.getOrCreateAnchorAt(20))
                           .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("sang")
                      .setStart(g.getOrCreateAnchorAt(20)).setEnd(g.getOrCreateAnchorAt(30))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("and")
                      .setStart(g.getOrCreateAnchorAt(30)).setEnd(g.getOrCreateAnchorAt(40))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("walked")
                      .setStart(g.getOrCreateAnchorAt(40)).setEnd(g.getOrCreateAnchorAt(50))
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
      
      assertEquals("double check there are tokens: "+Arrays.asList(g.list("word")),
                   8, g.list("word").length);
      assertEquals("double check there are no stems: "+Arrays.asList(g.list("stem")),
                   0, g.list("stem").length);
      // run the annotator
      annotator.transform(g);
      List<String> stemLabels = Arrays.stream(g.list("stem"))
         .map(annotation->annotation.getLabel()).collect(Collectors.toList());
      assertEquals("one stem per token: "+stemLabels,
                   8, stemLabels.size());
      Iterator<String> stems = stemLabels.iterator();
      assertEquals("down-case",
                   "i", stems.next());
      assertEquals("doesn't handle irregular verbs",
                   "sang", stems.next());
      assertEquals("and", stems.next());
      assertEquals("handles regular verbs",
                   "walk", stems.next());
      assertEquals("about", stems.next());
      assertEquals("my", stems.next());
      assertEquals("handles compounds",
                   "blog-post", stems.next());
      assertEquals("lazili", stems.next());

      // add a word
      g.addAnnotation(new Annotation().setLayerId("word").setLabel("new")
                      .setStart(g.getOrCreateAnchorAt(90)).setEnd(g.getOrCreateAnchorAt(100))
                      .setParent(turn));

      // change a word
      firstWord.setLabel("we");
      
      // run the annotator again
      annotator.transform(g);
      stemLabels = Arrays.stream(g.list("stem"))
         .map(annotation->annotation.getLabel()).collect(Collectors.toList());
      assertEquals("one more stem: "+stemLabels,
                   9, stemLabels.size());
      stems = stemLabels.iterator();
      assertEquals("changed label not re-annotated",
                   "i", stems.next());
      assertEquals("previous stem unchanged", "sang", stems.next());
      assertEquals("previous stem unchanged", "and", stems.next());
      assertEquals("previous stem unchanged", "walk", stems.next());
      assertEquals("previous stem unchanged", "about", stems.next());
      assertEquals("previous stem unchanged", "my", stems.next());
      assertEquals("previous stem unchanged", "blog-post", stems.next());
      assertEquals("previous stem unchanged", "lazili", stems.next());
      assertEquals("new token has stem",
                   "new", stems.next());

   }

   public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nzilbb.annotator.porterstemmer.test.TestPorterStemmer");
   }
}
