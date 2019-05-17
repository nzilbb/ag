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

public class TestBundleSerializer
{
  @Test public void dbConfig() throws Exception
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
      new Layer("lexical", "Lexical", 0, true, false, false, "word", true),
      new Layer("pronounce", "Pronounce", 0, false, false, true, "word", true));
    File dir = getDir();
    
    BundleSerialization serializer = new BundleSerialization();
    serializer.setJsonIndentFactor(2);

    // general configuration
    ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
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
    assertEquals(10, configuration.size());

    // customize configuration
    configuration.get("corpusName").setValue("actual_dbconfig");
    configuration.get("uuid").setValue("fc379152-0381-4efc-b4c1-e90d696f5373");
    configuration.get("showPerspectivesSidebar").setValue(Boolean.FALSE);
    configuration.get("correctionTool").setValue(Boolean.FALSE);
    configuration.get("useLargeTextInputField").setValue(Boolean.FALSE);
    configuration.get("showHierarchy").setValue(Boolean.FALSE);

    assertEquals(10, serializer.configure(configuration, schema).size());

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
  
  @Test public void serialize_fragment_utterance_word() 
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
    double fragmentFrom = 214.822;
    double fragmentTo = 218.29; // exactly on the offset of the last anchor
    String [] layerIds = { "utterance", "word", "phone", "tag" };
    Graph fragment = graphs[0].getFragment(fragmentFrom, fragmentTo, layerIds);
    fragment.shiftAnchors(-fragmentFrom);
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
    // for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
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
    assertEquals(10, configuration.size());
    
     //for (Parameter p : configuration.values()) System.out.println("config " + p.getName() + " = " + p.getValue());
    assertEquals(10, serializer.configure(configuration, schema).size());
    
    String[] needLayers = serializer.getRequiredLayers();
    assertEquals(0, needLayers.length);
    // assertEquals("utterance", needLayers[1]);
    
    // serialize
    NamedStream[] streams = serializer.serialize(fragments);
    assertEquals("One warning: " + Arrays.asList(serializer.getWarnings()),
                 1, serializer.getWarnings().length);
    streams[0].save(dir);
    File actual = new File(dir, "serialize_utterance_word__214.822-218.290.json");
    
    // test using diff
    String differences = diff(new File(dir, "expected_serialize_utterance_word__214.822-218.290.json"),
                              actual);
    if (differences != null)
    {
      fail(differences);
    }
    else
    {
      actual.delete();
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
    org.junit.runner.JUnitCore.main("nzilbb.emusdms.test.TestBundleSerializer");
  }
}
