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

import java.util.Comparator;
import nzilbb.ag.util.*;
import nzilbb.ag.*;

public class TestLayerHierarchyTraversal
{
   @Test public void basicTraversal() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      g.setCorpus("cc");

      g.addLayer(new Layer("turn", "Speaker turns", 2, true, true, false));
      g.addLayer(new Layer("word", "Words", 2, true, false, false, "turn", true));
      g.addLayer(new Layer("phone", "Phones", 2, true, false, true, "word", true));

      LayerHierarchyTraversal<StringBuffer> t = new LayerHierarchyTraversal<StringBuffer>(new StringBuffer(), g)
	 {
	    protected void pre(Layer layer)
	    {
	       result.append("<" + layer.getId() + ">");
	    }
	    protected void post(Layer layer)
	    {
	       result.append("</" + layer.getId() + ">");
	    }
	 };
      
      assertEquals("<turn><word><phone></phone></word></turn>", t.getResult().toString());
      
   }

   @Test public void peerOrder() 
   {
      Graph g = new Graph();
      g.setId("my graph");
      g.setCorpus("cc");

      // add layers in a different order that the traversal should take
      // traversal order should be 
      // * by height, (turn before word)
      // * and among peers, ALIGNMENT_NONE are first (orthography before syllable)
      // * and then peers=false are first, (orthogrphy before pos)
      // * then childless peers are first (utterance before word)
      // so the traversal order should be:
      // turn utterance parse entity word orthography pos syllable phone
      g.addLayer(new Layer("turn", "Speaker turns", Constants.ALIGNMENT_INTERVAL, 
			   true, // peers
			   true, // peersOverlap
			   false)); // saturated
      g.addLayer(new Layer("word", "Words", Constants.ALIGNMENT_INTERVAL, 
			   true, // peers
			   false, // peersOverlap
			   false,  // saturated
			   "turn", // parent
			   true)); // parentIncludes
      g.addLayer(new Layer("utterance", "Utterances", Constants.ALIGNMENT_INTERVAL, 
			   true, // peers
			   false, // peersOverlap
			   true,  // saturated
			   "turn", // parent
			   true)); // parentIncludes
      g.addLayer(new Layer("entity", "Name entity", Constants.ALIGNMENT_INTERVAL, 
			   true, // peers
			   false, // peersOverlap
			   false,  // saturated
			   "turn", // parent
			   true)); // parentIncludes
      g.addLayer(new Layer("parse", "Syntactic Parse", Constants.ALIGNMENT_INTERVAL, 
			   true, // peers
			   true, // peersOverlap
			   false,  // saturated
			   "turn", // parent
			   true)); // parentIncludes
      g.addLayer(new Layer("dependency", "Syntactic Dependency", Constants.ALIGNMENT_INTERVAL, 
			   false, // peers
			   false, // peersOverlap
			   false,  // saturated
			   "word", // parent
			   false)); // parentIncludes
      g.addLayer(new Layer("syllable", "Syllables", Constants.ALIGNMENT_INTERVAL, 
			   true, // peers
			   false, // peersOverlap
			   true, // saturated
			   "word", // parent
			   true)); // parentIncludes
      g.addLayer(new Layer("phone", "Phones", Constants.ALIGNMENT_INTERVAL, 
			   true, // peers
			   false, // peersOverlap
			   true, // saturated
			   "syllable", // parent
			   true)); // parentIncludes
      g.addLayer(new Layer("orthography", "Orthography", Constants.ALIGNMENT_NONE, 
			   false, // peers
			   false, // peersOverlap
			   true, // saturated
			   "word", // parent
			   true)); // parentIncludes
      g.addLayer(new Layer("pos", "Part of Speech", Constants.ALIGNMENT_NONE, 
			   true, // peers
			   false, // peersOverlap
			   true, // saturated
			   "word", // parent
			   true)); // parentIncludes

      // top down
      LayerHierarchyTraversal<StringBuffer> topDown = new LayerHierarchyTraversal<StringBuffer>(new StringBuffer(), g)
	 {
	    protected void pre(Layer layer)
	    {
	       result.append(layer.getId() + " ");
	    }
	 };
      
      assertEquals("top down", 
		   "turn utterance parse entity word dependency orthography pos syllable phone ", 
		   topDown.getResult().toString());
      
      // bottom up
      LayerHierarchyTraversal<StringBuffer> bottomUp = new LayerHierarchyTraversal<StringBuffer>(
	 new StringBuffer(), 
	 new Comparator<Layer>() { 
	    public int compare(Layer l1, Layer l2) { 
	       return -LayerHierarchyTraversal.defaultComparator.compare(l1,l2); 
	    } },
	 g)
	 {
	    protected void post(Layer layer)
	    {
	       result.append(layer.getId() + " ");
	    }
	 };
      assertEquals("bottom up", 
		   "phone syllable pos orthography dependency word entity parse utterance turn ", 
		   bottomUp.getResult().toString());
      
   }

   public static void main(String args[]) 
   {
      org.junit.runner.JUnitCore.main("nzilbb.ag.util.test.TestLayerHierarchyTraversal");
   }
}
