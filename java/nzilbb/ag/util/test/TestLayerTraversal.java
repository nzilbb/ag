//
// Copyright 2015 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of LaBB-CAT.
//
//    LaBB-CAT is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 2 of the License, or
//    (at your option) any later version.
//
//    LaBB-CAT is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with LaBB-CAT; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//

package nzilbb.ag.util.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

import nzilbb.ag.util.*;
import nzilbb.ag.*;

public class TestLayerTraversal
{
   @Test public void depthFirstTraversal() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      g.setCorpus("cc");

      g.addLayer(new Layer("turn", "Speaker turns", 2, true, true, false));
      g.addLayer(new Layer("word", "Words", 2, true, false, false, "turn", true));
      g.addLayer(new Layer("phone", "Phones", 2, true, false, true, "word", true));

      g.addAnchor(new Anchor("turnStart", 0.0));
      g.addAnchor(new Anchor("a1", 1.0));
      g.addAnchor(new Anchor("a1.5", 1.5));
      g.addAnchor(new Anchor("a2", 2.0));
      g.addAnchor(new Anchor("a2.25", 2.25));
      g.addAnchor(new Anchor("a2.5", 2.5));
      g.addAnchor(new Anchor("a2.75", 2.75));
      g.addAnchor(new Anchor("a3", 3.0));
      g.addAnchor(new Anchor("a4", 4.0));
      g.addAnchor(new Anchor("a5", 5.0));
      g.addAnchor(new Anchor("turnEnd", 6.0));

      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "my graph"));

      g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("word2", "quick", "word", "a2", "a3", "turn1"));
      g.addAnnotation(new Annotation("word3", "brown", "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("word4", "fox", "word", "a4", "a5", "turn1"));

      g.addAnnotation(new Annotation("phone1", "D", "phone", "a1", "a1.5", "word1"));
      g.addAnnotation(new Annotation("phone2", "@", "phone", "a1.5", "a2", "word1"));
      g.addAnnotation(new Annotation("phone3", "k", "phone", "a2", "a2.25", "word2"));
      g.addAnnotation(new Annotation("phone4", "w", "phone", "a2.25", "a2.5", "word2"));
      g.addAnnotation(new Annotation("phone5", "I", "phone", "a2.5", "a2.75", "word2"));
      g.addAnnotation(new Annotation("phone6", "k", "phone", "a2.75", "a3", "word2"));

      LayerTraversal<StringBuffer> t = new LayerTraversal<StringBuffer>(new StringBuffer(), g)
	 {
	    protected void pre(Annotation annotation)
	    {
	       result.append(annotation.getLabel() + " {");
	    }
	    protected void post(Annotation annotation)
	    {
	       result.append("} ");
	    }
	 };
      
      assertEquals("john smith {the {D {} @ {} } quick {k {} w {} I {} k {} } brown {} fox {} } ", t.getResult().toString());
      
   }

   @Test public void traversalIncludingExceptions() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      g.setCorpus("cc");

      g.addLayer(new Layer("turn", "Speaker turns", 2, true, true, false));
      g.addLayer(new Layer("word", "Words", 2, true, false, false, "turn", true));
      // no phone layer

      g.addAnchor(new Anchor("turnStart", 0.0));
      g.addAnchor(new Anchor("a1", 1.0));
      g.addAnchor(new Anchor("a1.5", 1.5));
      g.addAnchor(new Anchor("a2", 2.0));
      g.addAnchor(new Anchor("a2.25", 2.25));
      g.addAnchor(new Anchor("a2.5", 2.5));
      g.addAnchor(new Anchor("a2.75", 2.75));
      g.addAnchor(new Anchor("a3", 3.0));
      g.addAnchor(new Anchor("a4", 4.0));
      g.addAnchor(new Anchor("a5", 5.0));
      g.addAnchor(new Anchor("turnEnd", 6.0));

      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnEnd", "my graph"));

      g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("word2", "quick", "word", "a2", "a3", "turn1"));
      g.addAnnotation(new Annotation("word3", "brown", "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("word4", "fox", "word", "a4", "a5", "turn1"));

      g.addAnnotation(new Annotation("phone1", "D", "phone", "a1", "a1.5", "word1"));
      g.addAnnotation(new Annotation("phone2", "@", "phone", "a1.5", "a2", "word1"));
      g.addAnnotation(new Annotation("phone3", "k", "phone", "a2", "a2.25", "word2"));
      g.addAnnotation(new Annotation("phone4", "w", "phone", "a2.25", "a2.5", "word2"));
      g.addAnnotation(new Annotation("phone5", "I", "phone", "a2.5", "a2.75", "word2"));
      g.addAnnotation(new Annotation("phone6", "k", "phone", "a2.75", "a3", "word2"));

      LayerTraversal<StringBuffer> t = new LayerTraversal<StringBuffer>(new StringBuffer(), g)
	 {
	    protected void pre(Annotation annotation)
	    {
	       result.append(annotation.getLabel() + " {");
	    }
	    protected void post(Annotation annotation)
	    {
	       result.append("} ");
	    }
	    protected void except(Annotation annotation)
	    {
	       result.append("(");
	       result.append(annotation.getLabel());
	       result.append(") ");
	    }
	 };
      
      // as there's no phone layer, the traversal treats the phones as exceptions and adds them to the end
      assertEquals("john smith {the {} quick {} brown {} fox {} } (D) (@) (k) (w) (I) (k) ", t.getResult().toString());
   }

   @Test public void breadthFirstTraversal() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      g.setCorpus("cc");

      g.addLayer(new Layer("turn", "Speaker turns", 2, true, true, false));
      g.addLayer(new Layer("word", "Words", 2, true, false, false, "turn", true));
      g.addLayer(new Layer("phone", "Phones", 2, true, false, true, "word", true));

      g.addAnchor(new Anchor("turnStart", 0.0));
      g.addAnchor(new Anchor("a1", 1.0));
      g.addAnchor(new Anchor("a1.5", 1.5));
      g.addAnchor(new Anchor("a2", 2.0));
      g.addAnchor(new Anchor("a2.25", 2.25));
      g.addAnchor(new Anchor("a2.5", 2.5));
      g.addAnchor(new Anchor("a2.75", 2.75));
      g.addAnchor(new Anchor("a2", 2.0));
      g.addAnchor(new Anchor("a3", 3.0));
      g.addAnchor(new Anchor("a4", 4.0));
      g.addAnchor(new Anchor("a5", 5.0));
      g.addAnchor(new Anchor("turnMiddle", 6.0));
      g.addAnchor(new Anchor("a7", 7.0));
      g.addAnchor(new Anchor("a8", 8.0));
      g.addAnchor(new Anchor("a9", 9.0));
      g.addAnchor(new Anchor("a10", 10.0));
      g.addAnchor(new Anchor("a11", 11.0));
      g.addAnchor(new Anchor("turnEnd", 12.0));

      g.addAnnotation(new Annotation("turn1", "john smith", "turn", "turnStart", "turnMiddle"));

      g.addAnnotation(new Annotation("word1", "the", "word", "a1", "a2", "turn1"));
      g.addAnnotation(new Annotation("word2", "quick", "word", "a2", "a3", "turn1"));
      g.addAnnotation(new Annotation("word3", "brown", "word", "a3", "a4", "turn1"));
      g.addAnnotation(new Annotation("word4", "fox", "word", "a4", "a5", "turn1"));

      g.addAnnotation(new Annotation("phone1", "D", "phone", "a1", "a1.5", "word1"));
      g.addAnnotation(new Annotation("phone2", "@", "phone", "a1.5", "a2", "word1"));
      g.addAnnotation(new Annotation("phone3", "k", "phone", "a2", "a2.25", "word2"));
      g.addAnnotation(new Annotation("phone4", "w", "phone", "a2.25", "a2.5", "word2"));
      g.addAnnotation(new Annotation("phone5", "I", "phone", "a2.5", "a2.75", "word2"));
      g.addAnnotation(new Annotation("phone6", "k", "phone", "a2.75", "a3", "word2"));

      g.addAnnotation(new Annotation("turn2", "jane doe", "turn", "turnMiddle", "turnEnd"));

      g.addAnnotation(new Annotation("word5", "jumps", "word", "a7", "a8", "turn2"));
      g.addAnnotation(new Annotation("word6", "over", "word", "a8", "a9", "turn2"));
      g.addAnnotation(new Annotation("word7", "the", "word", "a9", "a10", "turn2"));
      g.addAnnotation(new Annotation("word8", "dog", "word", "a10", "a11", "turn2"));

      LayerTraversal<StringBuffer> t = new LayerTraversal<StringBuffer>(new StringBuffer(), g, true)
	 {
	    protected void pre(Annotation annotation)
	    {
	       result.append(annotation.getLabel() + " ");
	    }
	    protected void post(Annotation annotation)
	    {
	       result.append(annotation.getId() + " ");
	    }
	 };
      
      assertEquals("john smith jane doe the quick brown fox jumps over the dog D @ k w I k phone1 phone2 phone3 phone4 phone5 phone6 word1 word2 word3 word4 word5 word6 word7 word8 turn1 turn2 ", t.getResult().toString());
      
   }

   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.util.test.TestLayerTraversal");
   }
}
