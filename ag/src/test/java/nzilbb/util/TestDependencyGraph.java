//
// Copyright 2024 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
	      
import org.junit.*;
import static org.junit.Assert.*;

public class TestDependencyGraph {

  /** Tests generic dependency resolution of a simple, fully-connected dependency graph. */
  @Test public void basicResolution() throws Exception {
    DependencyGraph<String> g = new DependencyGraph<String>();
    DependencyNode<String> a = g.add(new DependencyNode<String>("a"));
    DependencyNode<String> b = g.add(new DependencyNode<String>("b"));
    DependencyNode<String> c = g.add(new DependencyNode<String>("c"));
    DependencyNode<String> d = g.add(new DependencyNode<String>("d"));
    DependencyNode<String> e = g.add(new DependencyNode<String>("e"));
    
    a.dependsOn(b);
    a.dependsOn(d);
    b.dependsOn(c);
    b.dependsOn(e);
    c.dependsOn(d);
    c.dependsOn(e);
    
    Collection<DependencyNode<String>> resolution = g.resolve();
    String resolutionString = resolution.stream()
      .map(node -> node.getProvider())
      .collect(Collectors.joining(" "));
    assertEquals("Simple case",
                 "d e c b a", resolutionString);    
  }
  
  /** Ensures indirect circular dependencies are detected. */
  @Test public void circularDependency() throws Exception {
    DependencyGraph<String> g = new DependencyGraph<String>();
    DependencyNode<String> a = g.add(new DependencyNode<String>("a"));
    DependencyNode<String> b = g.add(new DependencyNode<String>("b"));
    DependencyNode<String> c = g.add(new DependencyNode<String>("c"));
    DependencyNode<String> d = g.add(new DependencyNode<String>("d"));
    DependencyNode<String> e = g.add(new DependencyNode<String>("e"));
    
    a.dependsOn(b);
    a.dependsOn(d);
    b.dependsOn(c);
    b.dependsOn(e);
    c.dependsOn(d);
    c.dependsOn(e);
    // circular:
    d.dependsOn(b);
    
    try {
      Collection<DependencyNode<String>> resolution = g.resolve();
      String resolutionString = resolution.stream()
        .map(node -> node.getProvider())
        .collect(Collectors.joining(" "));
      fail("Circulare dependency, but resolution was: " + resolutionString);
    } catch(CircularDependencyException exception) {
      assertEquals("Circular exception node",
                   d, exception.getNode());
    }    
  }
  
  /** Tests generic dependency resolution of a diconnected dependency graph, with three
   * independent sub-graphs, one with a single unconnected node. */
  @Test public void disconnectedGraph() throws Exception {
    DependencyGraph<String> g = new DependencyGraph<String>();
    DependencyNode<String> a = g.add(new DependencyNode<String>("a"));
    DependencyNode<String> b = g.add(new DependencyNode<String>("b"));
    DependencyNode<String> c = g.add(new DependencyNode<String>("c"));
    DependencyNode<String> d = g.add(new DependencyNode<String>("d"));
    DependencyNode<String> e = g.add(new DependencyNode<String>("e"));
    
    DependencyNode<String> aa = g.add(new DependencyNode<String>("aa"));
    DependencyNode<String> bb = g.add(new DependencyNode<String>("bb"));
    DependencyNode<String> cc = g.add(new DependencyNode<String>("cc"));
    DependencyNode<String> dd = g.add(new DependencyNode<String>("dd"));
    DependencyNode<String> ee = g.add(new DependencyNode<String>("ee"));
    
    DependencyNode<String> aaa = g.add(new DependencyNode<String>("aaa"));
    
    a.dependsOn(b);
    a.dependsOn(d);
    b.dependsOn(c);
    b.dependsOn(e);
    c.dependsOn(d);
    c.dependsOn(e);

    aa.dependsOn(bb);
    aa.dependsOn(dd);
    bb.dependsOn(cc);
    bb.dependsOn(ee);
    cc.dependsOn(dd);
    cc.dependsOn(ee);

    Collection<DependencyNode<String>> resolution = g.resolve();
    String resolutionString = resolution.stream()
      .map(node -> node.getProvider())
      .collect(Collectors.joining(" "));
    assertEquals("Disconnected graph",
                 "d e c b a dd ee cc bb aa aaa", resolutionString);
    
  }

  /** Simulates old-style 'layer manager' resolution. In this case:
   * <ul>
   *  <li> Dependencies are determined by layers generated. </li>
   *  <li> A layer can have a primary manager and several auxiliary managers. </li>
   *  <li> For each layer, the primary manager must run first, then the auxiliaries in order. </li>
   *  <li> Some layers have no layer manager, but can nevertheless be generated by another
   *       layer's manager - e.g. HTK generates its own layer plus the 'segment' layer. </li>
   * </ul>
   */
  @Test public void layerManagerGraph() throws Exception {

    final Integer LAYER_WORD = 0;
    final Integer LAYER_SEGMENT = 1;
    final Integer LAYER_ORTHOGRAPHY = 2;
    final Integer LAYER_UTTERANCE = 12;
    final Integer LAYER_PRON = 30;
    final Integer LAYER_PHONOLOGY = 50;
    final Integer LAYER_HTK = 75;
    final Integer LAYER_ARPABET = 100;
    final Integer LAYER_PARTICIPANT = -2;
    final Integer LAYER_MAIN_PARTICIPANT = -3;

    // Layer Manager configurations as they come out of the database
    LayerManagerConfig word = new LayerManagerConfig("word", LAYER_WORD, null, null);
    LayerManagerConfig segment = new LayerManagerConfig("segment", LAYER_SEGMENT, null, null);
    LayerManagerConfig utterance
      = new LayerManagerConfig("utterance", LAYER_UTTERANCE, null, null);
    LayerManagerConfig orthography
      = new LayerManagerConfig("orthography", LAYER_ORTHOGRAPHY, null, "Orth Standardizer"); 
    orthography.dependsOnLayers.add(LAYER_WORD);
    LayerManagerConfig pron = new LayerManagerConfig("pron", LAYER_PRON, null, null);
    LayerManagerConfig phonology
      = new LayerManagerConfig("phonology", LAYER_PHONOLOGY, null, "CELEX");
    phonology.dependsOnLayers.add(LAYER_ORTHOGRAPHY);
    LayerManagerConfig phonologyPron =
      new LayerManagerConfig("phonology", LAYER_PHONOLOGY, 1, "Pattern Matcher"); // aux
    phonologyPron.dependsOnLayers.add(LAYER_ORTHOGRAPHY);
    phonologyPron.dependsOnLayers.add(LAYER_PRON);
    // ARPABET depends indirectly on HTK, but is defined before
    LayerManagerConfig arpabet
      = new LayerManagerConfig("arpabet", LAYER_ARPABET, null, "Character Mapper");
    arpabet.dependsOnLayers.add(LAYER_SEGMENT);
    LayerManagerConfig htk = new LayerManagerConfig("htk", LAYER_HTK, null, "HTKAligner");
    // required layers
    htk.dependsOnLayers.add(LAYER_WORD);
    htk.dependsOnLayers.add(LAYER_ORTHOGRAPHY);
    htk.dependsOnLayers.add(LAYER_UTTERANCE);
    htk.dependsOnLayers.add(LAYER_PHONOLOGY);
    // test that non-temporal layer dependencies don't cause problems
    htk.dependsOnLayers.add(LAYER_PARTICIPANT);
    htk.dependsOnLayers.add(LAYER_MAIN_PARTICIPANT);
    // output layers
    htk.generatesLayers.add(LAYER_SEGMENT); // also generates segment layer

    // add them all to a list
    LinkedHashSet<LayerManagerConfig> configurations
      = new LinkedHashSet<LayerManagerConfig>();
    configurations.add(word);
    configurations.add(segment);
    configurations.add(utterance);
    configurations.add(orthography);
    configurations.add(pron);
    configurations.add(phonology);
    configurations.add(phonologyPron);
    configurations.add(arpabet);
    configurations.add(htk);
    
    // keep track of configurations by layer_id
    LinkedHashMap<Integer, LinkedHashSet<DependencyNode<LayerManagerConfig>>> idToNodes
      = new LinkedHashMap<Integer, LinkedHashSet<DependencyNode<LayerManagerConfig>>>();

    // load them up
    for (LayerManagerConfig config : configurations) {
      // create a graph node
      DependencyNode<LayerManagerConfig> node = new DependencyNode<LayerManagerConfig>(config);
      // add to layer_id map
      if (!idToNodes.containsKey(config.layerId)) {
        idToNodes.put(config.layerId, new LinkedHashSet<DependencyNode<LayerManagerConfig>>());
      }
      // make sure it depends on all previous configs on the same layer
      LinkedHashSet<DependencyNode<LayerManagerConfig>> layerConfigs
        = idToNodes.get(config.layerId);
      for (DependencyNode<LayerManagerConfig> otherNode : layerConfigs) {
        node.dependsOn(otherNode);
      }
      idToNodes.get(config.layerId).add(node);
    }

    // by now we have loaded all configurations into idToNodes

    // ensure that node interdependencies are set, and add to dependency graph
    DependencyGraph<LayerManagerConfig> graph = new DependencyGraph<LayerManagerConfig>();
    for (Integer layerId : idToNodes.keySet()) {
      for (DependencyNode<LayerManagerConfig> node : idToNodes.get(layerId)) {
        graph.add(node);
        LayerManagerConfig config = node.getProvider();
        
        // make sure it depends on all its input layers
        for (Integer needsLayerId : config.dependsOnLayers) { // for each layer dependency
          if (idToNodes.containsKey(needsLayerId)) { // only layer_ids we have info about
            for (DependencyNode<LayerManagerConfig> needsNode : idToNodes.get(needsLayerId)) {
              node.dependsOn(needsNode);
            } // next config for this dependency layer
          } // needsLayerId is known
        } // next dependency layer

        // make sure all the generated layers are registered with dependent configurations
        for (Integer generatesLayerId : config.generatesLayers) { // for each layer generated
          if (!generatesLayerId.equals(layerId)) { // exclude self references
            for (DependencyNode<LayerManagerConfig> dependentNode : idToNodes.get(generatesLayerId)) {
              dependentNode.dependsOn(node);
            } // next config for this dependency layer
          } // (not self)
        } // next dependency layer
        
      } // next config for the layer
    } // next layer_id

    Collection<DependencyNode<LayerManagerConfig>> resolutionNodes = graph.resolve();
    List<String> res = resolutionNodes.stream()
      .map(node -> node.getProvider().toString())
      .collect(Collectors.toList());
    Iterator<String> i = res.iterator();

    assertEquals("unexpected order: " + res, "word(0-null)", i.next());
    assertEquals("unexpected order: " + res, "orthography(2-null) Orth Standardizer", i.next());
    assertEquals("unexpected order: " + res, "utterance(12-null)", i.next());
    assertEquals("unexpected order: " + res, "phonology(50-null) CELEX", i.next());
    assertEquals("unexpected order: " + res, "pron(30-null)", i.next());
    assertEquals("unexpected order: " + res, "phonology(50-1) Pattern Matcher", i.next());
    assertEquals("unexpected order: " + res, "htk(75-null) HTKAligner", i.next());
    assertEquals("unexpected order: " + res, "segment(1-null)", i.next());
    assertEquals("unexpected order: " + res, "arpabet(100-null) Character Mapper", i.next());
  }

  class LayerManagerConfig {
    
    String layerName;
    Integer layerId;
    Integer auxiliary;
    String key() {
      return ""+layerId+"-"+auxiliary;
    }
    String implementation;
    
    Set<Integer> dependsOnLayers = new TreeSet<Integer>();
    Set<Integer> generatesLayers = new TreeSet<Integer>();
    
    public LayerManagerConfig(
      String layerName,
      Integer layerId,
      Integer auxiliary,
      String implementation) {
      this.layerName = layerName;
      this.layerId = layerId;
      this.auxiliary = auxiliary;
      this.implementation = implementation;
      generatesLayers.add(layerId);
    }
    @Override public String toString() {
      return layerName + "("+key()+")" + (implementation==null?"":" "+implementation);
    }
  }
  
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.util.TestDependencyGraph");
  }
}
