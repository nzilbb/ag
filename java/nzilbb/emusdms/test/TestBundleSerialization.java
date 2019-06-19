//
// Copyright 2019 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.emusdms.test;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.SortedSet;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.List;
import java.util.Arrays;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.URL;
import nzilbb.configure.ParameterSet;
import nzilbb.configure.Parameter;
import nzilbb.editpath.MinimumEditPath;
import nzilbb.editpath.EditStep;
import nzilbb.ag.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.serialize.json.JSONSerialization;
import nzilbb.emusdms.*;

public class TestBundleSerialization
{
  @Test public void dbConfigAllSegements() throws Exception
  {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      
      new Layer("comment", "Comment").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false),
      
      new Layer("noise", "Noise")
      .setAlignment(2).setPeers(true).setPeersOverlap(false).setSaturated(false),
      
      new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      
      new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      
      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      
      new Layer("phone", "Phones").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true),
      
      new Layer("lexical", "Lexical").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("word").setParentIncludes(true),
      
      new Layer("pronounce", "Pronounce").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true));
    
    File dir = getDir();
    
    BundleSerialization serializer = new BundleSerialization();
    serializer.setJsonIndentFactor(2);

    // general configuration
    ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("config " + p);
    assertEquals("sampleRate", Integer.valueOf(16000), 
                 (Integer)configuration.get("sampleRate").getValue());
    assertEquals("corpusName", "corpus", 
                 (String)configuration.get("corpusName").getValue());
    assertNotNull("uuid", configuration.get("uuid").getValue());
    assertEquals("showPerspectivesSidebar", Boolean.TRUE, 
                 (Boolean)configuration.get("showPerspectivesSidebar").getValue());
    assertEquals("playback", Boolean.TRUE, 
                 (Boolean)configuration.get("playback").getValue());
    assertEquals("correctionTool", Boolean.TRUE, 
                 (Boolean)configuration.get("correctionTool").getValue());
    assertEquals("editItemSize", Boolean.TRUE, 
                 (Boolean)configuration.get("editItemSize").getValue());
    assertEquals("useLargeTextInputField", Boolean.TRUE, 
                 (Boolean)configuration.get("useLargeTextInputField").getValue());
    assertEquals("saveBundle", Boolean.TRUE, 
                 (Boolean)configuration.get("saveBundle").getValue());
    assertEquals("showHierarchy", Boolean.TRUE, 
                 (Boolean)configuration.get("showHierarchy").getValue());
    assertEquals("oneToManyRelationships", Boolean.TRUE, 
                 (Boolean)configuration.get("oneToManyRelationships").getValue());
    assertEquals("Right number of parameters: " + configuration,
                 11, configuration.size());

    // Praat-style...
    configuration.get("oneToManyRelationships").setValue(Boolean.FALSE);

    // customize configuration
    configuration.get("corpusName").setValue("actual_dbconfig");
    configuration.get("uuid").setValue("fc379152-0381-4efc-b4c1-e90d696f5373");
    configuration.get("showPerspectivesSidebar").setValue(Boolean.FALSE);
    configuration.get("correctionTool").setValue(Boolean.FALSE);
    configuration.get("useLargeTextInputField").setValue(Boolean.FALSE);
    configuration.get("showHierarchy").setValue(Boolean.FALSE);

    assertEquals(11, serializer.configure(configuration, schema).size());

    Vector<String> layers = new Vector<String>();
    layers.add("phone");
    // including a word tag layer forced the serializer to include the word layer
    layers.add("pronounce");
    // ...and the utterance layer is included anyway

    // serialize
    NamedStream[] streams = serializer.serializeSchema(schema, layers);
    assertEquals("No warnings: " + Arrays.asList(serializer.getWarnings()),
                 0, serializer.getWarnings().length);
    String json = serializer.getDbConfig(
      schema, layers, "Test", "fc379152-0381-4efc-b4c1-e90d696f5373",
      false, true, false, true, false, true, false);
    streams[0].save(dir);
    File actual = new File(dir, "actual_dbconfig.json");
    
    // test using diff
    String differences = diff(new File(dir, "expected_dbconfig.json"), actual);
    if (differences != null)
    {
      fail(differences);
    }
    else
    {
      actual.delete();
    }
  }
  
  @Test public void dbConfigOneToMany() throws Exception
  {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      
      new Layer("comment", "Comment").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false),
      
      new Layer("noise", "Noise")
      .setAlignment(2).setPeers(true).setPeersOverlap(false).setSaturated(false),
      
      new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      
      new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      
      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      
      new Layer("phone", "Phones").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true),
      
      new Layer("lexical", "Lexical").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("word").setParentIncludes(true),
      
      new Layer("pronounce", "Pronounce").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true));
    
    File dir = getDir();
    
    BundleSerialization serializer = new BundleSerialization();
    serializer.setJsonIndentFactor(2);

    // general configuration
    ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("config " + p);
    assertEquals("sampleRate", Integer.valueOf(16000), 
                 (Integer)configuration.get("sampleRate").getValue());
    assertEquals("corpusName", "corpus", 
                 (String)configuration.get("corpusName").getValue());
    assertNotNull("uuid", configuration.get("uuid").getValue());
    assertEquals("showPerspectivesSidebar", Boolean.TRUE, 
                 (Boolean)configuration.get("showPerspectivesSidebar").getValue());
    assertEquals("playback", Boolean.TRUE, 
                 (Boolean)configuration.get("playback").getValue());
    assertEquals("correctionTool", Boolean.TRUE, 
                 (Boolean)configuration.get("correctionTool").getValue());
    assertEquals("editItemSize", Boolean.TRUE, 
                 (Boolean)configuration.get("editItemSize").getValue());
    assertEquals("useLargeTextInputField", Boolean.TRUE, 
                 (Boolean)configuration.get("useLargeTextInputField").getValue());
    assertEquals("saveBundle", Boolean.TRUE, 
                 (Boolean)configuration.get("saveBundle").getValue());
    assertEquals("showHierarchy", Boolean.TRUE, 
                 (Boolean)configuration.get("showHierarchy").getValue());
    assertEquals("oneToManyRelationships", Boolean.TRUE, 
                 (Boolean)configuration.get("oneToManyRelationships").getValue());
    assertEquals("Right number of parameters: " + configuration,
                 11, configuration.size());

    // customize configuration
    configuration.get("corpusName").setValue("actual_dbconfig");
    configuration.get("uuid").setValue("fc379152-0381-4efc-b4c1-e90d696f5373");
    configuration.get("showPerspectivesSidebar").setValue(Boolean.FALSE);
    configuration.get("correctionTool").setValue(Boolean.FALSE);
    configuration.get("useLargeTextInputField").setValue(Boolean.FALSE);
    configuration.get("showHierarchy").setValue(Boolean.FALSE);

    assertEquals(11, serializer.configure(configuration, schema).size());

    Vector<String> layers = new Vector<String>();
    layers.add("phone");
    // including a word tag layer forced the serializer to include the word layer
    layers.add("pronounce");
    // ...and the utterance layer is included anyway

    // serialize
    NamedStream[] streams = serializer.serializeSchema(schema, layers);
    assertEquals("No warnings: " + Arrays.asList(serializer.getWarnings()),
                 0, serializer.getWarnings().length);
    String json = serializer.getDbConfig(
      schema, layers, "Test", "fc379152-0381-4efc-b4c1-e90d696f5373",
      false, true, false, true, false, true, false);
    streams[0].save(dir);
    File actual = new File(dir, "actual_dbconfig.json");
    
    // test using diff
    String differences = diff(new File(dir, "expected_onetomany_dbconfig.json"), actual);
    if (differences != null)
    {
      fail(differences);
    }
    else
    {
      actual.delete();
    }
  }
  
  @Test public void serialize_fragment_utterance_word_AllSegements() 
    throws Exception
  {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("noise", "Noise", 2, true, false, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("phone", "Phones", 2, true, false, true, "word", true),
      new Layer("tag", "Word Tags", 0, true, false, false, "word", true),
      new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
    final File dir = getDir();
    // access file
    NamedStream[] jsonStreams = { new NamedStream(new File(dir, "serialize_utterance_word.json")) };
    
    // deserialize graph from JSON
    JSONSerialization json = new JSONSerialization();
    json.configure(json.configure(new ParameterSet(), schema), schema);
    json.setParameters(json.load(jsonStreams, schema));
    Graph[] graphs = json.deserialize();

    // extract fragment
    String [] layerIds = { "utterance", "word", "phone", "tag" };
    Graph fragment = graphs[0].getFragment(graphs[0].getAnnotation("57"), layerIds);
    fragment.shiftAnchors(-214.822);
    assertEquals("serialize_utterance_word__214.822-218.290", fragment.getId());
    fragment.setMediaProvider(new IGraphMediaProvider() {
        public MediaFile[] getAvailableMedia() throws StoreException, PermissionException
        {
          return null;
        }
        public String getMedia(String trackSuffix, String mimeType) 
          throws StoreException, PermissionException
        {          
          try
          {
            return new File(dir, "silence.wav").toURI().toString();
          }
          catch(Exception exception)
          {
            throw new StoreException(exception);
          }
        }
        public IGraphMediaProvider providerForGraph(Graph graph) { return this; }
      });
    Graph[] fragments = { fragment };

    // create serializer
    BundleSerialization serializer = new BundleSerialization();
    serializer.setJsonIndentFactor(2);
      
    // general configuration
    ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("config " + p);
    assertEquals("sampleRate", Integer.valueOf(16000), 
                 (Integer)configuration.get("sampleRate").getValue());
    assertEquals("corpusName", "corpus", 
                 (String)configuration.get("corpusName").getValue());
    assertNotNull("uuid", configuration.get("uuid").getValue());
    assertEquals("showPerspectivesSidebar", Boolean.TRUE, 
                 (Boolean)configuration.get("showPerspectivesSidebar").getValue());
    assertEquals("playback", Boolean.TRUE, 
                 (Boolean)configuration.get("playback").getValue());
    assertEquals("correctionTool", Boolean.TRUE, 
                 (Boolean)configuration.get("correctionTool").getValue());
    assertEquals("editItemSize", Boolean.TRUE, 
                 (Boolean)configuration.get("editItemSize").getValue());
    assertEquals("useLargeTextInputField", Boolean.TRUE, 
                 (Boolean)configuration.get("useLargeTextInputField").getValue());
    assertEquals("saveBundle", Boolean.TRUE, 
                 (Boolean)configuration.get("saveBundle").getValue());
    assertEquals("showHierarchy", Boolean.TRUE, 
                 (Boolean)configuration.get("showHierarchy").getValue());
    assertEquals("oneToManyRelationships", Boolean.TRUE, 
                 (Boolean)configuration.get("oneToManyRelationships").getValue());
    assertEquals("Right number of parameters: " + configuration,
                 11, configuration.size());

    // Praat-style...
    configuration.get("oneToManyRelationships").setValue(Boolean.FALSE);
    
    //for (Parameter p : configuration.values()) System.out.println("config " + p);
    assertEquals(11, serializer.configure(configuration, schema).size());
    
    String[] needLayers = serializer.getRequiredLayers();
    assertEquals(0, needLayers.length);
    // assertEquals("utterance", needLayers[1]);
    
    // serialize
    NamedStream[] streams = serializer.serialize(fragments);
//    assertEquals("One warning: " + Arrays.asList(serializer.getWarnings()),
//                 1, serializer.getWarnings().length);
    streams[0].save(dir);
    File actual = new File(dir, fragment.getId()+".json");
    
    // test using diff
    String differences = diff(new File(dir, "expected_"+actual.getName()), actual);
    if (differences != null)
    {
      fail(differences);
    }
    else
    {
      actual.delete();
    }
  }

  @Test public void serialize_fragment_utterance_word_OneToMany() 
    throws Exception
  {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("noise", "Noise", 2, true, false, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("phone", "Phones", 2, true, false, true, "word", true),
      new Layer("tag", "Word Tags", 0, true, false, false, "word", true),
      new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
    final File dir = getDir();
    // access file
    NamedStream[] jsonStreams = { new NamedStream(new File(dir, "serialize_utterance_word.json")) };
    
    // deserialize graph from JSON
    JSONSerialization json = new JSONSerialization();
    json.configure(json.configure(new ParameterSet(), schema), schema);
    json.setParameters(json.load(jsonStreams, schema));
    Graph[] graphs = json.deserialize();

    // extract fragment
    String [] layerIds = { "utterance", "word", "phone", "tag" };
    Graph fragment = graphs[0].getFragment(graphs[0].getAnnotation("57"), layerIds);
    fragment.shiftAnchors(-214.822);
    assertEquals("serialize_utterance_word__214.822-218.290", fragment.getId());
    fragment.setId("onetomany_serialize_utterance_word__214.822-218.290");
    assertEquals("onetomany_serialize_utterance_word__214.822-218.290", fragment.getId());
    fragment.setMediaProvider(new IGraphMediaProvider() {
        public MediaFile[] getAvailableMedia() throws StoreException, PermissionException
        {
          return null;
        }
        public String getMedia(String trackSuffix, String mimeType) 
          throws StoreException, PermissionException
        {          
          try
          {
            return new File(dir, "silence.wav").toURI().toString();
          }
          catch(Exception exception)
          {
            throw new StoreException(exception);
          }
        }
        public IGraphMediaProvider providerForGraph(Graph graph) { return this; }
      });
    Graph[] fragments = { fragment };

    // create serializer
    BundleSerialization serializer = new BundleSerialization();
    serializer.setJsonIndentFactor(2);
      
    // general configuration
    ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("config " + p);
    assertEquals("sampleRate", Integer.valueOf(16000), 
                 (Integer)configuration.get("sampleRate").getValue());
    assertEquals("corpusName", "corpus", 
                 (String)configuration.get("corpusName").getValue());
    assertNotNull("uuid", configuration.get("uuid").getValue());
    assertEquals("showPerspectivesSidebar", Boolean.TRUE, 
                 (Boolean)configuration.get("showPerspectivesSidebar").getValue());
    assertEquals("playback", Boolean.TRUE, 
                 (Boolean)configuration.get("playback").getValue());
    assertEquals("correctionTool", Boolean.TRUE, 
                 (Boolean)configuration.get("correctionTool").getValue());
    assertEquals("editItemSize", Boolean.TRUE, 
                 (Boolean)configuration.get("editItemSize").getValue());
    assertEquals("useLargeTextInputField", Boolean.TRUE, 
                 (Boolean)configuration.get("useLargeTextInputField").getValue());
    assertEquals("saveBundle", Boolean.TRUE, 
                 (Boolean)configuration.get("saveBundle").getValue());
    assertEquals("showHierarchy", Boolean.TRUE, 
                 (Boolean)configuration.get("showHierarchy").getValue());
    assertEquals("oneToManyRelationships", Boolean.TRUE, 
                 (Boolean)configuration.get("oneToManyRelationships").getValue());
    assertEquals("Right number of parameters: " + configuration,
                 11, configuration.size());

    //for (Parameter p : configuration.values()) System.out.println("config " + p);
    assertEquals(11, serializer.configure(configuration, schema).size());
    
    String[] needLayers = serializer.getRequiredLayers();
    assertEquals(0, needLayers.length);
    // assertEquals("utterance", needLayers[1]);
    
    // serialize
    NamedStream[] streams = serializer.serialize(fragments);
//    assertEquals("One warning: " + Arrays.asList(serializer.getWarnings()),
//                 1, serializer.getWarnings().length);
    streams[0].save(dir);
    File actual = new File(dir, fragment.getId()+".json");
    
    // test using diff
    String differences = diff(new File(dir, "expected_"+actual.getName()), actual);
    if (differences != null)
    {
      fail(differences);
    }
    else
    {
      actual.delete();
    }
  }

  @Test public void deserialize_fragment_utterance_word_AllSegements() 
    throws Exception
  {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("noise", "Noise", 2, true, false, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("phone", "Phones", 2, true, false, true, "word", true),
      new Layer("tag", "Word Tags", 0, true, false, false, "word", true),
      new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
    final File dir = getDir();
    
    // deserialize graph from JSON
    BundleSerialization deserializer = new BundleSerialization();
    
    // general configuration
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("config " + p);
    assertEquals("sampleRate", Integer.valueOf(16000), 
                 (Integer)configuration.get("sampleRate").getValue());
    assertEquals("corpusName", "corpus", 
                 (String)configuration.get("corpusName").getValue());
    assertNotNull("uuid", configuration.get("uuid").getValue());
    assertEquals("showPerspectivesSidebar", Boolean.TRUE, 
                 (Boolean)configuration.get("showPerspectivesSidebar").getValue());
    assertEquals("playback", Boolean.TRUE, 
                 (Boolean)configuration.get("playback").getValue());
    assertEquals("correctionTool", Boolean.TRUE, 
                 (Boolean)configuration.get("correctionTool").getValue());
    assertEquals("editItemSize", Boolean.TRUE, 
                 (Boolean)configuration.get("editItemSize").getValue());
    assertEquals("useLargeTextInputField", Boolean.TRUE, 
                 (Boolean)configuration.get("useLargeTextInputField").getValue());
    assertEquals("saveBundle", Boolean.TRUE, 
                 (Boolean)configuration.get("saveBundle").getValue());
    assertEquals("showHierarchy", Boolean.TRUE, 
                 (Boolean)configuration.get("showHierarchy").getValue());
    assertEquals("oneToManyRelationships", Boolean.TRUE, 
                 (Boolean)configuration.get("oneToManyRelationships").getValue());
    assertEquals("Right number of parameters: " + configuration,
                 11, configuration.size());

    // Praat-style...
    configuration.get("oneToManyRelationships").setValue(Boolean.FALSE);
    
    // for (Parameter p : configuration.values()) System.out.println("config " + p);
    assertEquals(11, deserializer.configure(configuration, schema).size());
    
    NamedStream[] jsonBundles = {
      new NamedStream(new File(dir, "expected_serialize_utterance_word__214.822-218.290.json")) };

    ParameterSet parameters = deserializer.load(jsonBundles, schema);
    // for (Parameter p : parameters.values()) System.out.println("param " + p);
    assertEquals(4, parameters.size());
    assertEquals("level_phone", schema.getLayer("phone"), 
                 parameters.get("level_phone").getValue());
    assertEquals("level_word", schema.getLayer("word"), 
                 parameters.get("level_word").getValue());
    assertEquals("label_tag", schema.getLayer("tag"), 
                 parameters.get("label_tag").getValue());
    assertEquals("level_utterance", schema.getLayer("utterance"), 
                 parameters.get("level_utterance").getValue());

    deserializer.setParameters(parameters);
    
    // dserialize
    Graph[] graphs = deserializer.deserialize();
    assertEquals("No warnings: " + Arrays.asList(deserializer.getWarnings()),
                 0, deserializer.getWarnings().length);
    assertEquals("One graph: " + Arrays.asList(graphs),
                 1, graphs.length);

    Graph g = graphs[0];
    assertEquals("graph id", "serialize_utterance_word__214.822-218.290", g.getId());
    assertEquals("granularity", Double.valueOf(1.0/16000.0), g.getOffsetGranularity());

    // ensure schema structure is correct
    assertEquals("schema: utterance parent ", "turn", g.getLayer("utterance").getParentId());
    assertEquals("schema: word parent ", "turn", g.getLayer("word").getParentId());
    assertEquals("schema: phone parent ", "word", g.getLayer("phone").getParentId());
    assertEquals("schema: tag parent ", "word", g.getLayer("tag").getParentId());
    assertEquals("schema: turn layer", "turn", g.getSchema().getTurnLayerId());
    assertEquals("schema: utterance layer", "utterance", g.getSchema().getUtteranceLayerId());
    assertEquals("schema: word layer", "word", g.getSchema().getWordLayerId());

    g.shiftAnchors(214.822);

    // utterances
    Annotation[] annotations = g.list("utterance");
    assertEquals("utterance count", 1, annotations.length);
    assertEquals("utterance label", "participant", annotations[0].getLabel());
    assertNotNull("utterance parent is set",
               annotations[0].getParent());
    assertNull("utterance parent is unanchored (start)",
               annotations[0].getParent().getStart());
    assertNull("utterance parent is unanchored (end)",
               annotations[0].getParent().getEnd());
    assertEquals("utterance parent label",
                annotations[0].getLabel(), annotations[0].getParent().getLabel());

    // words
    Annotation[] words = g.list("word");
    String[] wordLabels = { "or", "some", "and", "there's", "nothing", "much", "on", "it", "or" };
    // use original offsets
    double[] wordStarts = { 214.822, 215.3, 215.5926666667, 215.978, 216.36333, 216.7486666667, 217.134, 217.5193333333, 217.9046666667 };
    double[] wordEnds = { 215.2073333333, 215.5926666667, 215.978, 216.36333, 216.7486666667, 217.134, 217.5193333333, 217.9046666667, 218.29 };
    assertEquals("word count", wordLabels.length, words.length);
    for (int i = 0; i < wordLabels.length; i++)
    {
      assertEquals("word label " + i, wordLabels[i], words[i].getLabel());
      assertEquals("word start " + i + " " + wordStarts[i] + " vs " + words[i].getStart().getOffset(),
                   0, g.compareOffsets(wordStarts[i], words[i].getStart().getOffset()));
      assertEquals("word end " + i + " " +  wordEnds[i] + " vs " + words[i].getEnd().getOffset(),
                   0, g.compareOffsets(wordEnds[i], words[i].getEnd().getOffset()));
      assertNotNull("word parent is set " + i,
                    words[i].getParent());
      assertNull("word parent is unanchored (start) " + i,
                 words[i].getParent().getStart());
      assertNull("word parent is unanchored (end) " + i,
                 words[i].getParent().getEnd());
    } // next annotation

    // tags
    Annotation[] tags = g.list("tag");
    assertEquals("tag count", 1, tags.length);
    assertEquals("tag label", "first-tag", tags[0].getLabel());
    assertTrue("tag word", tags[0].tags(words[1]));    
    assertEquals("tag parent", words[1], tags[0].getParent());    

    // phones
    annotations = g.list("phone");
    assertEquals("phone count", 5, annotations.length);
    String[] phoneLabels = { "$", "s", "V", "m", "n" };
    String[] parentLabels = { "or", "some", "some", "some", "and" };
    int[] phoneOrdinals = { 1, 1, 2, 3, 1 };
    assertEquals("phone count", phoneLabels.length, annotations.length);
    for (int i = 0; i < phoneLabels.length; i++)
    {
      assertEquals("phone label " + i, phoneLabels[i], annotations[i].getLabel());
      assertEquals("phone parent " + i, parentLabels[i], annotations[i].getParent().getLabel());
    } // next annotation

    // anchors
    for (Anchor a : g.getAnchors().values())
    {
      assertNotNull("no null offsets " + a.getId() + ":" + a.getOffset(),
                    a.getOffset());
      assertEquals("anchor set to manual confidence " + a.getId() + ":" + a.getOffset(),
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }
  }

  @Test public void deserialize_fragment_utterance_word_OneToMany() 
    throws Exception
  {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants", 0, true, true, true),
      new Layer("comment", "Comment", 2, true, false, true),
      new Layer("noise", "Noise", 2, true, false, true),
      new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
      new Layer("utterance", "Utterances", 2, true, false, true, "turn", true),
      new Layer("word", "Words", 2, true, false, false, "turn", true),
      new Layer("phone", "Phones", 2, true, false, true, "word", true),
      new Layer("tag", "Word Tags", 0, true, false, false, "word", true),
      new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
    final File dir = getDir();
    
    // deserialize graph from JSON
    BundleSerialization deserializer = new BundleSerialization();
    
    // general configuration
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("config " + p);
    assertEquals("sampleRate", Integer.valueOf(16000), 
                 (Integer)configuration.get("sampleRate").getValue());
    assertEquals("corpusName", "corpus", 
                 (String)configuration.get("corpusName").getValue());
    assertNotNull("uuid", configuration.get("uuid").getValue());
    assertEquals("showPerspectivesSidebar", Boolean.TRUE, 
                 (Boolean)configuration.get("showPerspectivesSidebar").getValue());
    assertEquals("playback", Boolean.TRUE, 
                 (Boolean)configuration.get("playback").getValue());
    assertEquals("correctionTool", Boolean.TRUE, 
                 (Boolean)configuration.get("correctionTool").getValue());
    assertEquals("editItemSize", Boolean.TRUE, 
                 (Boolean)configuration.get("editItemSize").getValue());
    assertEquals("useLargeTextInputField", Boolean.TRUE, 
                 (Boolean)configuration.get("useLargeTextInputField").getValue());
    assertEquals("saveBundle", Boolean.TRUE, 
                 (Boolean)configuration.get("saveBundle").getValue());
    assertEquals("showHierarchy", Boolean.TRUE, 
                 (Boolean)configuration.get("showHierarchy").getValue());
    assertEquals("oneToManyRelationships", Boolean.TRUE, 
                 (Boolean)configuration.get("oneToManyRelationships").getValue());
    assertEquals("Right number of parameters: " + configuration,
                 11, configuration.size());

    // for (Parameter p : configuration.values()) System.out.println("config " + p);
    assertEquals(11, deserializer.configure(configuration, schema).size());
    
    NamedStream[] jsonBundles = {
      new NamedStream(new File(dir, "expected_onetomany_serialize_utterance_word__214.822-218.290.json")) };

    ParameterSet parameters = deserializer.load(jsonBundles, schema);
    // for (Parameter p : parameters.values()) System.out.println("param " + p);
    assertEquals(4, parameters.size());
    assertEquals("level_phone", schema.getLayer("phone"), 
                 parameters.get("level_phone").getValue());
    assertEquals("level_word", schema.getLayer("word"), 
                 parameters.get("level_word").getValue());
    assertEquals("label_tag", schema.getLayer("tag"), 
                 parameters.get("label_tag").getValue());
    assertEquals("level_utterance", schema.getLayer("utterance"), 
                 parameters.get("level_utterance").getValue());

    deserializer.setParameters(parameters);
    
    // dserialize
    Graph[] graphs = deserializer.deserialize();
    assertEquals("No warnings: " + Arrays.asList(deserializer.getWarnings()),
                 0, deserializer.getWarnings().length);
    assertEquals("One graph: " + Arrays.asList(graphs),
                 1, graphs.length);

    Graph g = graphs[0];
    assertEquals("graph id", "onetomany_serialize_utterance_word__214.822-218.290", g.getId());
    assertEquals("granularity", Double.valueOf(1.0/16000.0), g.getOffsetGranularity());

    // ensure schema structure is correct
    assertEquals("schema: utterance parent ", "turn", g.getLayer("utterance").getParentId());
    assertEquals("schema: word parent ", "turn", g.getLayer("word").getParentId());
    assertEquals("schema: phone parent ", "word", g.getLayer("phone").getParentId());
    assertEquals("schema: tag parent ", "word", g.getLayer("tag").getParentId());
    assertEquals("schema: turn layer", "turn", g.getSchema().getTurnLayerId());
    assertEquals("schema: utterance layer", "utterance", g.getSchema().getUtteranceLayerId());
    assertEquals("schema: word layer", "word", g.getSchema().getWordLayerId());

    g.shiftAnchors(214.822);

    // utterances
    Annotation[] annotations = g.list("utterance");
    assertEquals("utterance count", 1, annotations.length);
    assertEquals("utterance label", "participant", annotations[0].getLabel());
    assertNotNull("utterance parent is set",
               annotations[0].getParent());
    assertNull("utterance parent is unanchored (start)",
               annotations[0].getParent().getStart());
    assertNull("utterance parent is unanchored (end)",
               annotations[0].getParent().getEnd());
    assertEquals("utterance parent label",
                annotations[0].getLabel(), annotations[0].getParent().getLabel());

    // words
    Annotation[] words = g.list("word");
    String[] wordLabels = { "or", "some", "and", "there's", "nothing", "much", "on", "it", "or" };
    // use original offsets
    double[] wordStarts = { 214.822, 215.3, 215.5926666667, 215.978, 216.36333, 216.7486666667, 217.134, 217.5193333333, 217.9046666667 };
    double[] wordEnds = { 215.2073333333, 215.5926666667, 215.978, 216.36333, 216.7486666667, 217.134, 217.5193333333, 217.9046666667, 218.29 };
    assertEquals("word count", wordLabels.length, words.length);
    for (int i = 0; i < wordLabels.length; i++)
    {
      assertEquals("word label " + i, wordLabels[i], words[i].getLabel());
      assertEquals("word start " + i + " " + wordStarts[i] + " vs " + words[i].getStart().getOffset(),
                   0, g.compareOffsets(wordStarts[i], words[i].getStart().getOffset()));
      assertEquals("word end " + i + " " +  wordEnds[i] + " vs " + words[i].getEnd().getOffset(),
                   0, g.compareOffsets(wordEnds[i], words[i].getEnd().getOffset()));
      assertNotNull("word parent is set " + i,
                    words[i].getParent());
      assertNull("word parent is unanchored (start) " + i,
                 words[i].getParent().getStart());
      assertNull("word parent is unanchored (end) " + i,
                 words[i].getParent().getEnd());
    } // next annotation

    // tags
    Annotation[] tags = g.list("tag");
    assertEquals("tag count", 1, tags.length);
    assertEquals("tag label", "first-tag", tags[0].getLabel());
    assertTrue("tag word", tags[0].tags(words[1]));    
    assertEquals("tag parent", words[1], tags[0].getParent());    

    // phones
    annotations = g.list("phone");
    assertEquals("phone count", 11, annotations.length);
    String[] phoneLabels = { "$", "s", "V", "m", "n",
                             // and the dummy ones
                             "?", "?", "?", "?", "?", "?" };
    String[] parentLabels = { "or", "some", "some", "some", "and",
                              "there's", "nothing", "much", "on", "it", "or"};
    int[] phoneOrdinals = { 1, 1, 2, 3, 1,
                            1, 1, 1, 1, 1, 1 };
    assertEquals("phone count", phoneLabels.length, annotations.length);
    for (int i = 0; i < phoneLabels.length; i++)
    {
      assertEquals("phone label " + i, phoneLabels[i], annotations[i].getLabel());
      assertEquals("phone parent " + i, parentLabels[i], annotations[i].getParent().getLabel());
    } // next annotation

    // anchors
    for (Anchor a : g.getAnchors().values())
    {
      assertNotNull("no null offsets " + a.getId() + ":" + a.getOffset(),
                    a.getOffset());
      assertEquals("anchor set to manual confidence " + a.getId() + ":" + a.getOffset(),
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }
  }

  /**
   * Diffs two files.
   * @param expected
   * @param actual
   * @return null if the files are the same, and a String describing differences if not.
   */
  public String diff(File expected, File actual)
  {
    StringBuffer d = new StringBuffer();
      
    try
    {
      // compare with what we expected
      Vector<String> actualLines = new Vector<String>();
      BufferedReader reader = new BufferedReader(new FileReader(actual));
      String line = reader.readLine();
      while (line != null)
      {
        actualLines.add(line);
        line = reader.readLine();
      }
      Vector<String> expectedLines = new Vector<String>();
      reader = new BufferedReader(new FileReader(expected));
      line = reader.readLine();
      while (line != null)
      {
        expectedLines.add(line);
        line = reader.readLine();
      }
      MinimumEditPath<String> comparator = new MinimumEditPath<String>();
      List<EditStep<String>> path = comparator.minimumEditPath(expectedLines, actualLines);
      for (EditStep<String> step : path)
      {
        switch (step.getOperation())
        {
          case CHANGE:
            d.append("\n"+expected.getPath()+":"+(step.getFromIndex()+1)+": Expected:\n" 
                     + step.getFrom() 
                     + "\n"+actual.getPath()+":"+(step.getToIndex()+1)+": Found:\n" + step.getTo());
            break;
          case DELETE:
            d.append("\n"+expected.getPath()+":"+(step.getFromIndex()+1)+": Deleted:\n" 
                     + step.getFrom()
                     + "\n"+actual.getPath()+":"+(step.getToIndex()+1)+": Missing");
            break;
          case INSERT:
            d.append("\n"+expected.getPath()+":"+(step.getFromIndex()+1)+": Missing" 
                     + "\n"+actual.getPath()+":"+(step.getToIndex()+1)+": Inserted:\n" 
                     + step.getTo());
            break;
        }
      } // next step
    }
    catch(Exception exception)
    {
      d.append("\n" + exception);
    }
    if (d.length() > 0) return d.toString();
    return null;
  } // end of diff()

  /**
   * Directory for text files.
   * @see #getDir()
   * @see #setDir(File)
   */
  protected File fDir;
  /**
   * Getter for {@link #fDir}: Directory for text files.
   * @return Directory for text files.
   */
  public File getDir() 
  { 
    if (fDir == null)
    {
      try
      {
        URL urlThisClass = getClass().getResource(getClass().getSimpleName() + ".class");
        File fThisClass = new File(urlThisClass.toURI());
        fDir = fThisClass.getParentFile();
      }
      catch(Throwable t)
      {
        System.out.println("" + t);
      }
    }
    return fDir; 
  }
  /**
   * Setter for {@link #fDir}: Directory for text files.
   * @param fNewDir Directory for text files.
   */
  public void setDir(File fNewDir) { fDir = fNewDir; }

  public static void main(String args[]) 
  {
    org.junit.runner.JUnitCore.main("nzilbb.emusdms.test.TestBundleSerialization");
  }
}
