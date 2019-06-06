//
// Copyright 2015-2016 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.ag.util.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.TreeSet;
import java.util.Iterator;
import nzilbb.ag.*;
import nzilbb.ag.util.*;

public class TestAnnotationComparators
{
  @Test public void byAnchor() 
  {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");

    g.addLayer(new Layer("turn", "Speaker turns", 2, true, true, false));
    g.addLayer(new Layer("word", "Words", 2, true, false, false, "turn", true));
    g.addLayer(new Layer("phone", "Phones", 2, true, false, true, "word", true));
    g.addLayer(new Layer("pos", "Part of speech", 0, false, false, true, "word", true));
    g.addLayer(new Layer("phrase", "Phrase structure", 2, true, true, false, "turn", true));

    g.addAnchor(new Anchor("a0", 0.0)); // turn start
    g.addAnchor(new Anchor("a1", 1.0)); // the & DT & D & NP
    g.addAnchor(new Anchor("a1.5", 1.5)); // @
    g.addAnchor(new Anchor("a2a", 2.0)); // zero-length start
    g.addAnchor(new Anchor("a2b", 2.0)); // zero-length end
    g.addAnchor(new Anchor("a2", 2.0)); // quick & A & k & AP
    g.addAnchor(new Anchor("a2.25", 2.25)); // w
    g.addAnchor(new Anchor("a2.5", 2.5)); // I
    g.addAnchor(new Anchor("a2.75", 2.75)); // k
    g.addAnchor(new Anchor("a3", 3.0)); // brown
    g.addAnchor(new Anchor("a4", 4.0)); // fox & N
    // unset offsets
    g.addAnchor(new Anchor("a?1", null)); // jumps
    g.addAnchor(new Anchor("a?2", null)); // over
    g.addAnchor(new Anchor("a5", 5.0)); // end of over
    g.addAnchor(new Anchor("a6", 6.0)); // turn end

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a6", "my graph"));
    g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
    g.addAnnotation(new Annotation("pos1", "DT", "pos", "a1", "a2", "word1"));
    g.addAnnotation(new Annotation("phone1", "D", "phone", "a1", "a1.5", "word1"));
    g.addAnnotation(new Annotation("phone2", "@", "phone", "a1.5", "a2", "word1"));
    g.addAnnotation(new Annotation("zerolength", "zerolength", "word", "a2a", "a2b", "turn1"));
    g.addAnnotation(new Annotation("word2", "quick", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("pos2", "A", "pos", "a2", "a3", "word2"));
    g.addAnnotation(new Annotation("phone3", "k", "phone", "a2", "a2.25", "word2"));
    g.addAnnotation(new Annotation("phone4", "w", "phone", "a2.25", "a2.5", "word2"));
    g.addAnnotation(new Annotation("phone5", "I", "phone", "a2.5", "a2.75", "word2"));
    g.addAnnotation(new Annotation("phone6", "k", "phone", "a2.75", "a3", "word2"));
    g.addAnnotation(new Annotation("word3", "brown", "word", "a3", "a4", "turn1"));
    g.addAnnotation(new Annotation("phrase1", "AP", "phrase", "a2", "a4", "turn1"));
    g.addAnnotation(new Annotation("word4", "fox", "word", "a4", "a?1", "turn1"));
    g.addAnnotation(new Annotation("pos3", "N", "pos", "a4", "a?1", "word4"));
    g.addAnnotation(new Annotation("phrase2", "NP", "phrase", "a1", "a4", "turn1"));
    g.addAnnotation(new Annotation("word5", "jumps", "word", "a?1", "a?2", "turn1"));
    g.addAnnotation(new Annotation("word6", "over", "word", "a?2", "a5", "turn1"));

    AnnotationComparatorByAnchor comparator = new AnnotationComparatorByAnchor();
    for (Annotation a1 : g.getAnnotationsById().values())
    {
      for (Annotation a2 : g.getAnnotationsById().values())
      {
        if (a1 != a2)
        {
          assertNotEquals("spurious equality", 0, comparator.compare(a1, a2));
        }
      }
    }

    TreeSet<Annotation> annotations = new TreeSet<Annotation>(comparator);
    annotations.addAll(g.getAnnotationsById().values());
    // System.out.println(""+annotations);
    Iterator<Annotation> order = annotations.iterator();
    assertEquals("earliest first", "turn1", order.next().getId());
    assertEquals("end order - phrase before first word", "phrase2", order.next().getId());
    assertEquals("layerId order - pos before word", "pos1", order.next().getId());
    assertEquals("layerId order - word after pos", "word1", order.next().getId());
    assertEquals("end order - phone after word", "phone1", order.next().getId());
    assertEquals("phone2", order.next().getId());
    assertEquals("zero-length between previous and next", "zerolength", order.next().getId());
    assertEquals("end order - phrase before first word", "phrase1", order.next().getId());
    assertEquals("layerId order - pos before word", "pos2", order.next().getId());
    assertEquals("layerId order - word after pos", "word2", order.next().getId());
    assertEquals("end order - phone after word", "phone3", order.next().getId());
    assertEquals("phone4", order.next().getId());
    assertEquals("phone5", order.next().getId());
    assertEquals("phone6", order.next().getId());
    assertEquals("word3", order.next().getId());
    assertEquals("layerId order - pos before word", "pos3", order.next().getId());
    assertEquals("layerId order - word after pos", "word4", order.next().getId());
    assertEquals("unkown offsets - use linking", "word5", order.next().getId());
    assertEquals("unkown offsets - use linking", "word6", order.next().getId());
    assertFalse(order.hasNext());
  }
  
  @Test public void peersByAnchor() 
  {
    Graph g = new Graph();
    g.setId("my graph");
    g.setCorpus("cc");

    g.addLayer(new Layer("turn", "Speaker turns", 2, true, true, false));
    g.addLayer(new Layer("word", "Words", 2, true, false, false, "turn", true));
    g.addLayer(new Layer("phone", "Phones", 2, true, false, true, "word", true));
    g.addLayer(new Layer("pos", "Part of speech", 0, false, false, true, "word", true));
    g.addLayer(new Layer("phrase", "Phrase structure", 2, true, true, false, "turn", true));

    g.addAnchor(new Anchor("a0", 0.0)); // turn start
    g.addAnchor(new Anchor("a1", 1.0)); // the & DT & D & NP
    g.addAnchor(new Anchor("a1.5", 1.5)); // @
    g.addAnchor(new Anchor("a2a", 2.0)); // zero-length start
    g.addAnchor(new Anchor("a2b", 2.0)); // zero-length end
    g.addAnchor(new Anchor("a2", 2.0)); // quick & A & k & AP
    g.addAnchor(new Anchor("a2.25", 2.25)); // w
    g.addAnchor(new Anchor("a2.5", 2.5)); // I
    g.addAnchor(new Anchor("a2.75", 2.75)); // k
    g.addAnchor(new Anchor("a3", 3.0)); // brown
    g.addAnchor(new Anchor("a4", 4.0)); // fox & N
    // unset offsets
    g.addAnchor(new Anchor("a?1", null)); // jumps
    g.addAnchor(new Anchor("a?2", null)); // over
    g.addAnchor(new Anchor("a5", 5.0)); // end of over
    g.addAnchor(new Anchor("a6", 6.0)); // turn end

    g.addAnnotation(new Annotation("turn1", "john smith", "turn", "a0", "a6", "my graph"));

    // last word added first
    g.addAnnotation(new Annotation("word6", "over", "word", "a?2", "a5", "turn1"));

    g.addAnnotation(new Annotation("pos1", "DT", "pos", "a1", "a2", "word1"));
    g.addAnnotation(new Annotation("phone1", "D", "phone", "a1", "a1.5", "word1"));
    g.addAnnotation(new Annotation("phone2", "@", "phone", "a1.5", "a2", "word1"));
    g.addAnnotation(new Annotation("zerolength", "zerolength", "word", "a2a", "a2b", "turn1"));
    g.addAnnotation(new Annotation("word2", "quick", "word", "a2", "a3", "turn1"));
    g.addAnnotation(new Annotation("pos2", "A", "pos", "a2", "a3", "word2"));
    g.addAnnotation(new Annotation("phone3", "k", "phone", "a2", "a2.25", "word2"));
    g.addAnnotation(new Annotation("phone4", "w", "phone", "a2.25", "a2.5", "word2"));
    g.addAnnotation(new Annotation("phone5", "I", "phone", "a2.5", "a2.75", "word2"));
    g.addAnnotation(new Annotation("phone6", "k", "phone", "a2.75", "a3", "word2"));

    // word4 before word3
    g.addAnnotation(new Annotation("word4", "fox", "word", "a4", "a?1", "turn1"));
    g.addAnnotation(new Annotation("word3", "brown", "word", "a3", "a4", "turn1"));

    g.addAnnotation(new Annotation("phrase1", "AP", "phrase", "a2", "a4", "turn1"));
    g.addAnnotation(new Annotation("pos3", "N", "pos", "a4", "a?1", "word4"));
    g.addAnnotation(new Annotation("phrase2", "NP", "phrase", "a1", "a4", "turn1"));
    g.addAnnotation(new Annotation("word5", "jumps", "word", "a?1", "a?2", "turn1"));

    // first word last
    g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));

    PeerAnnotationsByAnchor annotations = new PeerAnnotationsByAnchor(g.getAnnotation("turn1"), "word");
    // System.out.println(""+annotations);
    Iterator<Annotation> order = annotations.iterator();
    assertEquals("anchor order: "+annotations, "word1", order.next().getId());
    assertEquals("anchor order: "+annotations, "zerolength", order.next().getId());
    assertEquals("anchor order: "+annotations, "word2", order.next().getId());
    assertEquals("anchor order: "+annotations, "word3", order.next().getId());
    assertEquals("anchor order: "+annotations, "word4", order.next().getId());
    assertEquals("anchor order: "+annotations, "word5", order.next().getId());
    assertEquals("anchor order: "+annotations, "word6", order.next().getId());
    assertFalse(order.hasNext());
  }
  public static void main(String args[]) 
  {
    org.junit.runner.JUnitCore.main("nzilbb.ag.util.test.TestAnnotationComparators");
  }
}
