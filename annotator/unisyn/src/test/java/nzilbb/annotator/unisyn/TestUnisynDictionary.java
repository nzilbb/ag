//
// Copyright 2023 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.annotator.unisyn;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import nzilbb.ag.Anchor;
import nzilbb.ag.Annotation;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.automation.Dictionary;
import nzilbb.ag.automation.DictionaryException;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.UsesRelationalDatabase;
import nzilbb.sql.derby.DerbyConnectionFactory;

public class TestUnisynDictionary {
  
  static UnisynTagger annotator = new UnisynTagger();
  
  @BeforeClass
  public static void install() throws Exception {

    // output all statuses
    //annotator.getStatusObservers().add(s->System.out.println(s));

    System.out.println("Installing lexicon...");

    // find the current directory
    File dir = dir();

    // set the schema
    annotator.setSchema(schema());

    // use derby for relational database
    annotator.setRdbConnectionFactory(new DerbyConnectionFactory(dir));

    // load a-z.csv as lexicon
    File file = new File(dir, "test.unisyn");
    String error = annotator.loadLexicon("TestUnisynDictionary", file);
    if (error.length() > 0) {
      fail(error);
    }
    // loading is in a separate thread
    while (annotator.getRunning()) {
      try {Thread.sleep(100);} catch(Exception exception) {}
    }
    
    System.out.println("Lexicon installed.");
  }

  @AfterClass static public void deleteLexicon() {
    try {
      annotator.deleteLexicon("TestUnisynDictionary");
    } catch(SQLException exception) {
      System.err.println("deleteLexicon: " + exception);
    }
  }
	 
  public static File dir() throws Exception { 
    URL urlThisClass = TestUnisynDictionary.class.getResource(
      TestUnisynDictionary.class.getSimpleName() + ".class");
    File fThisClass = new File(urlThisClass.toURI());
    return fThisClass.getParentFile();
  }


  /** Ensure that dictionary operations work with pron_orig configuration. */
  @Test public void pron_orig() throws Exception {

    Dictionary dictionary = annotator.getDictionary("TestUnisynDictionary:wordform->pron_orig");
    List<String> entries = dictionary.lookup("quick");
    assertEquals("one entry returned - " + entries,
                 1, entries.size());
    assertEquals("entry correct", "{ k w * i k }", entries.get(0));

    entries = dictionary.lookup("the");
    assertEquals("multiple entries - " + entries, 3, entries.size());
    assertEquals("first entry", "{ dh @ }", entries.get(0));
    assertEquals("second entry", "{ dh @ }", entries.get(1));
    assertEquals("third entry", "{ dh * @ }", entries.get(2));

    // not editable
    entries = dictionary.lookupEditableEntry("the");
    assertEquals("no editable entries for 'the' - " + entries, 0, entries.size());
    assertEquals("no editable entries at all", 0, dictionary.countEditableKeys());

    // can add/remove entry
    entries = dictionary.lookup("fox");
    assertEquals("no entries - " + entries, 0, entries.size());
    
    dictionary.add("fox", "{ f * aa k s }");
    entries = dictionary.lookup("fox");
    assertEquals("one entry - " + entries, 1, entries.size());
    assertEquals("only entry", "{ f * aa k s }", entries.get(0));
    // it counts as editable
    entries = dictionary.lookupEditableEntry("fox");
    assertEquals("one entry - " + entries, 1, entries.size());
    assertEquals("only entry", "{ f * aa k s }", entries.get(0));
    assertEquals("one editable entry overall", 1, dictionary.countEditableKeys());
    
    dictionary.remove("fox", "{ f * aa k s }");
    entries = dictionary.lookup("fox");
    assertEquals("no entries - " + entries, 0, entries.size());
    assertEquals("no editable entries overall", 0, dictionary.countEditableKeys());

    // removing a nonexistent entry doesn't throw exceptions
    dictionary.remove("fox", "{ f * aa k s }");

    // can't remove an original entry
    try {
      dictionary.remove("quick", "{ k w * i k }");
      fail("Should not be able to remove 'quick'");
    } catch(DictionaryException exception) {
    }
  }   

  /** Ensure that dictionary operations work with pron_disc configuration. */
  @Test public void pron_disc() throws Exception {

    // dictionary works
    Dictionary dictionary = annotator.getDictionary("TestUnisynDictionary:wordform->pron_disc");
    List<String> entries = dictionary.lookup("quick");
    assertEquals("one entry returned - " + entries,
                 1, entries.size());
    assertEquals("entry correct", "kw'Ik", entries.get(0));
    
    entries = dictionary.lookup("the");
    assertEquals("multiple entries - " + entries, 3, entries.size());
    assertEquals("first entry", "D@", entries.get(0));
    assertEquals("second entry", "D@", entries.get(1));
    assertEquals("third entry", "D'@", entries.get(2));

    // can add/remove entry
    entries = dictionary.lookup("fox");
    assertEquals("no entries - " + entries, 0, entries.size());
    
    dictionary.add("fox", "\"fQks");
    entries = dictionary.lookup("fox");
    assertEquals("one entries - " + entries, 1, entries.size());
    assertEquals("only entry", "\"fQks", entries.get(0));
    
    dictionary.remove("fox", "\"fQks");
    entries = dictionary.lookup("fox");
    assertEquals("no entries - " + entries, 0, entries.size());

  }   

  /**
   * Returns a graph for annotating.
   * @return The graph for testing with.
   */
  public static Schema schema() {
    return new Schema(
      "participant", "turn", "utterance", "word",
      new Layer("transcript_language", "Overall Language")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("participant", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("participant").setParentIncludes(true),
      new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("lang", "Phrase Language").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("orthography", "Orthography").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true),
      new Layer("unisyn", "Pronunciation").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true)
      .setParentId("word").setParentIncludes(true),
      new Layer("phone", "Speech sounds").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true));
  } // end of schema()

  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.annotator.flatlexicon.TestUnisynDictionary");
  }
}
