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

package nzilbb.annotator.cmudict;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
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
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.UsesRelationalDatabase;
import nzilbb.annotator.cmudict.CMUDictionaryTagger;
import nzilbb.annotator.cmudict.CMUDictionary;
import nzilbb.sql.derby.DerbyConnectionFactory;

public class TestCMUDictionary {

  static CMUDictionaryTagger annotator = new CMUDictionaryTagger();
   
  @BeforeClass
  public static void install() throws Exception {

    System.out.println("Installing lexicon if necessary...");

    // find the current directory
    File dir = dir();

    // set the schema
    annotator.setSchema(schema());

    // set the working directory
    annotator.setWorkingDirectory(dir);

    // use derby for relational database
    annotator.setRdbConnectionFactory(new DerbyConnectionFactory(dir));

    // set the annotator configuration, which will install the lexicon the first time (only)
    annotator.setConfig(annotator.getConfig());
      
    System.out.println("Lexicon installed.");
  }
	 
  public static File dir() throws Exception { 
    URL urlThisClass = TestCMUDictionary.class.getResource(
      TestCMUDictionary.class.getSimpleName() + ".class");
    File fThisClass = new File(urlThisClass.toURI());
    return fThisClass.getParentFile();
  }
   
  @Test public void basicInfo() throws Exception {
      
    CMUDictionary dict = (CMUDictionary)annotator.getDictionary("cmudict");
    List<String> entries = dict.lookup("TEST");
    assertFalse("Isn't read only",
                dict.isReadOnly());
    assertEquals("125K odd entries",
                 125074, dict.countAllKeys());
    assertEquals("No editable keys",
                 0, dict.countEditableKeys());
  }   
   
  @Test public void basicLookup() throws Exception {
      
    CMUDictionary dict = (CMUDictionary)annotator.getDictionary("cmudict");
    List<String> entries = dict.lookup("TEST");
    assertEquals("1 entry: " + entries, 1, entries.size());
    assertEquals("UPPERCASE",
                 "T EH1 S T", entries.iterator().next());
    assertEquals("lowercase",
                 "T EH1 S T", dict.lookup("test").iterator().next());
    assertEquals("more than one entry",
                 4, dict.lookup("Buenos-Aires").size());
    assertEquals("no entries",
                 0, dict.lookup("Fizzgig").size());
  }   
   
  @Test public void DISC() throws Exception {
      
    CMUDictionary dict = ((CMUDictionary)annotator.getDictionary("cmudict"))
      .setEncoding(CMUDictionary.Encoding.DISC);
    List<String> entries = dict.lookup("transcription");
    assertEquals("DSIC",
                 "tr{nskrIpSVn", entries.iterator().next());
  }   
   
  @Test public void pagination() throws Exception {
      
    CMUDictionary dict = (CMUDictionary)annotator.getDictionary("cmudict");
    Map<String, List<String>> page1 = dict.listAllEntries(0, 10);
    assertEquals("page 1 size",
                 10, page1.size());
    Map<String, List<String>> page2 = dict.listAllEntries(1, 10);
    assertEquals("page 2 size",
                 10, page2.size());
    assertNotEquals("pages are different",
                    page2.keySet(), page1.keySet());
  }   

  @Test public void aggregation() throws Exception {
      
    CMUDictionary dict = (CMUDictionary)annotator.getDictionary("cmudict");
    assertEquals("COUNT keys",
                 ""+dict.countAllKeys(), dict.aggregateKeys("COUNT"));
    assertEquals("MIN key",
                 "!exclamation-point", dict.aggregateKeys("MIN"));
    assertEquals("MAX key",
                 "}right-brace", dict.aggregateKeys("MAX"));
    assertEquals("COUNT entries",
                 "133854", dict.aggregateEntries("COUNT"));
    assertEquals("MIN entry",
                 "AA0 B AA0 T IY0 EH1 L OW0", dict.aggregateEntries("MIN"));
    assertEquals("MAX entry",
                 "ZH W EY1 D AO1 NG", dict.aggregateEntries("MAX"));
  }   

  @Test public void readWrite() throws Exception {
      
    CMUDictionary dict = (CMUDictionary)annotator.getDictionary("cmudict");
    assertEquals("lookup",
                 1, dict.lookup("test").size());
    assertEquals("lookupEditableEntry",
                 0, dict.lookupEditableEntry("test").size());

    try {
      // add an entry
      dict.add("floof","F L UW1 F");
      dict.add("floof","F L UH F"); // not UH1 because otherwise DISC->CMU doesn't match
      dict.add("floof","F L AW1 F");
      List<String> entries = dict.lookup("floof");
      assertEquals("3 entries: " + entries,
                   3, entries.size());
      Iterator<String> pron = entries.iterator();
      assertEquals("F L UW1 F", pron.next());
      assertEquals("F L UH F", pron.next());
      assertEquals("F L AW1 F", pron.next());
      assertEquals("one editable key",
                   1, dict.countEditableKeys());
      assertEquals("one editable entry",
                   1, dict.listEditableEntries().size());
      List<String> editableEntries = dict.lookupEditableEntry("floof");
      assertEquals("lookup and lookupEditableEntry return the same thing",
                   entries, editableEntries);

      // DISC encoding
      dict.setEncoding(CMUDictionary.Encoding.DISC);
      entries = dict.lookup("floof");
      pron = entries.iterator();
      assertEquals("fluf", pron.next());
      assertEquals("flUf", pron.next());
      assertEquals("fl6f", pron.next());

      dict.remove("floof", "flUf");
      entries = dict.lookup("floof");
      assertEquals("remove specific DISC-encoded entry - now 2 entries: " + entries,
                   2, entries.size());

      // back to CMU encoding
      dict.setEncoding(CMUDictionary.Encoding.CMU);
                     
      entries = dict.lookup("floof");
      pron = entries.iterator();
      assertEquals("remove specific CMU-encoded entry",
                   "F L UW1 F", pron.next());
      assertEquals("remove specific CMU-encoded entry",
                   "F L AW1 F", pron.next());

      dict.remove("floof");
      entries = dict.lookup("floof");
      assertEquals("remove all entries - now no entries: " + entries,
                   0, entries.size());
    } finally {
      try { dict.remove("floof"); } catch(Exception exception) {}
    }
  }   
   
  /**
   * Returns a graph for annotating.
   * @return The graph for testing with.
   */
  public static Schema schema() {
    return new Schema(
      "who", "turn", "utterance", "word",
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
      .setParentId("turn").setParentIncludes(true));
  } // end of schema()
   

  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.annotator.cmudict.TestCMUDictionary");
  }
}
