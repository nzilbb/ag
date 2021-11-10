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

package nzilbb.annotator.labelmapper;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;
import nzilbb.ag.Anchor;
import nzilbb.ag.Annotation;
import nzilbb.ag.Change;
import nzilbb.ag.Constants;
import nzilbb.ag.Graph;
import nzilbb.ag.GraphMediaProvider;
import nzilbb.ag.Layer;
import nzilbb.ag.MediaFile;
import nzilbb.ag.PermissionException;
import nzilbb.ag.Schema;
import nzilbb.ag.StoreException;
import nzilbb.ag.automation.Dictionary;
import nzilbb.ag.automation.InvalidConfigurationException;
import nzilbb.ag.automation.UsesFileSystem;
import nzilbb.ag.automation.UsesRelationalDatabase;
import nzilbb.editpath.*;
import nzilbb.sql.derby.DerbyConnectionFactory;
import nzilbb.util.IO;

public class TestLabelMapper {

   public static LabelMapper newAnnotator() throws Exception {

     LabelMapper annotator = new LabelMapper();
     
     // find the current directory
     File dir = dir();
     
     // set the schema
     annotator.setSchema(graph().getSchema());
     
     // use derby for relational database
     annotator.setRdbConnectionFactory(new DerbyConnectionFactory(dir));
     
     // set the annotator configuration, which will install the lexicon the first time (only)
     annotator.setConfig(annotator.getConfig());

     return annotator;
   }
	 
   public static File dir() throws Exception { 
      URL urlThisClass = TestLabelMapper.class.getResource(
         TestLabelMapper.class.getSimpleName() + ".class");
      File fThisClass = new File(urlThisClass.toURI());
      return fThisClass.getParentFile();
   }

  /** Test overlap rate calculation. */
  @Test public void overlapRate() throws Exception {
    // annotations have to be in a graph to be linked to their anchors...
    Graph g = new Graph();
    Annotation a1to2 = g.addAnnotation(
      new Annotation().setLayerId("test")
      .setStart(g.getOrCreateAnchorAt(1.0))
      .setEnd(g.getOrCreateAnchorAt(2.0)));
    Annotation a15to25 = g.addAnnotation(
      new Annotation().setLayerId("test")
      .setStart(g.getOrCreateAnchorAt(1.5))
      .setEnd(g.getOrCreateAnchorAt(2.5)));
    Annotation a125to175 = g.addAnnotation(
      new Annotation().setLayerId("test")
      .setStart(g.getOrCreateAnchorAt(1.25))
      .setEnd(g.getOrCreateAnchorAt(1.75)));
    Annotation instantaneous = g.addAnnotation(
      new Annotation().setLayerId("test")
      .setStart(g.getOrCreateAnchorAt(1.5))
      .setEnd(g.getOrCreateAnchorAt(1.5)));
    Annotation a2to3 = g.addAnnotation(
      new Annotation().setLayerId("test")
      .setStart(g.getOrCreateAnchorAt(2.0))
      .setEnd(g.getOrCreateAnchorAt(3.0)));
    Annotation a3to4 = g.addAnnotation(
      new Annotation().setLayerId("test")
      .setStart(g.getOrCreateAnchorAt(3.0))
      .setEnd(g.getOrCreateAnchorAt(4.0)));
    Anchor nullOffset = g.addAnchor(new Anchor());
    Annotation nullStart = g.addAnnotation(
      new Annotation().setLayerId("test")
      .setStart(nullOffset)
      .setEnd(g.getOrCreateAnchorAt(1.5)));
    Annotation nullEnd = g.addAnnotation(
      new Annotation().setLayerId("test")
      .setStart(g.getOrCreateAnchorAt(1.5))
      .setEnd(nullOffset));
    assertEquals("First completely before the second",
                 0.0, LabelMapper.OverlapRate(a1to2, a3to4), 0.0);
    assertEquals("First completely after the second",
                 0.0, LabelMapper.OverlapRate(a3to4, a1to2), 0.0);
    assertEquals("First immediately before the second",
                 0.0, LabelMapper.OverlapRate(a1to2, a2to3), 0.0);
    assertEquals("First immediately after the second",
                 0.0, LabelMapper.OverlapRate(a2to3, a1to2), 0.0);
    assertEquals("First completely overlaps second",
                 1.0, LabelMapper.OverlapRate(a1to2, a1to2), 0.0);
    assertEquals("First embedded in the second",
                 0.5, LabelMapper.OverlapRate(a125to175, a1to2), 0.0);
    assertEquals("Second embedded in the first",
                 0.5, LabelMapper.OverlapRate(a1to2, a125to175), 0.0);
    assertEquals("Half overlap",
                 0.5/1.5, LabelMapper.OverlapRate(a15to25, a1to2), 0.0);
    assertEquals("First is instantaneous",
                 0.0, LabelMapper.OverlapRate(instantaneous, a1to2), 0.0);
    assertEquals("Second is instantaneous",
                 0.0, LabelMapper.OverlapRate(a1to2, instantaneous), 0.0);
    assertEquals("First has null start",
                 0.0, LabelMapper.OverlapRate(nullStart, a1to2), 0.0);
    assertEquals("First has null end",
                 0.0, LabelMapper.OverlapRate(nullEnd, a1to2), 0.0);
    assertEquals("Second has null start",
                 0.0, LabelMapper.OverlapRate(a1to2, nullStart), 0.0);
    assertEquals("Second has null end",
                 0.0, LabelMapper.OverlapRate(a1to2, nullEnd), 0.0);
  }   

  /** LabelMapper does not support default parameters. */
  @Test public void defaultParameters() throws Exception {
    LabelMapper annotator = newAnnotator();
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    try { // use default configuration
      annotator.setTaskParameters(null);
      fail("Should fail with default configuration");
    } catch (InvalidConfigurationException x) {
    }
  }   
  
  /** Valid parameters are accepted. */
  @Test public void setValidParameters() throws Exception {
    LabelMapper annotator = newAnnotator();
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // layers are created as required
    annotator.setTaskParameters(
      "sourceLayerId=orthography"
      +"&splitLabels=char"
      +"&targetLayerId=phone"
      +"&comparator=OrthographyToDISC"
      +"&mappingLayerId=disc"); // nonexistent
    Layer layer = annotator.getSchema().getLayer("disc");
    assertNotNull("disc layer created", layer);
  }   
  
  /** Valid sub-mapping parameters are accepted. */
  @Test public void setValidSubMappingParameters() throws Exception {
    LabelMapper annotator = newAnnotator();
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // layers are created as required
    annotator.setTaskParameters(
      "sourceLayerId=orthography"
      +"&splitLabels="
      +"&targetLayerId=htkWord"
      +"&comparator=CharacterToCharacter"
      +"&mappingLayerId=wordComparison"
      +"&subSourceLayerId=phone"
      +"&subTargetLayerId=htkPhone"
      +"&subMappingLayerId=phoneComparison"
      +"&subComparator=DISCToDISC");
    Layer layer = annotator.getSchema().getLayer("wordComparison");
    assertNotNull("wordComparison layer created", layer);
    assertEquals("wordComparison parent", "turn", layer.getParentId());
    assertEquals("wordComparison type", Constants.TYPE_STRING, layer.getType());
    assertEquals("wordComparison alignment", Constants.ALIGNMENT_INTERVAL, layer.getAlignment());
    layer = annotator.getSchema().getLayer("phoneComparison");
    assertNotNull("phoneComparison layer created", layer);
    assertEquals("phoneComparison parent", "turn", layer.getParentId());
    assertEquals("phoneComparison type", Constants.TYPE_IPA, layer.getType());
    assertEquals("phoneComparison alignment", Constants.ALIGNMENT_INTERVAL, layer.getAlignment());
  }   
  
  /** Invalid parameters are rejected. */
  @Test public void setInvalidTaskParameters() throws Exception {
    LabelMapper annotator = newAnnotator();
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    try {
      annotator.setTaskParameters(
        "sourceLayerId=nonexistent"
        +"&splitLabels=char"
        +"&targetLayerId=phone"
        +"&comparator=OrthographyToDISC"
        +"&mappingLayerId=disc"); // nonexistent
      fail("Should fail with nonexistent sourceLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "sourceLayerId=orthography"
        +"&splitLabels=char"
        +"&targetLayerId=nonexistent"
        +"&comparator=OrthographyToDISC"
        +"&mappingLayerId=disc"); // nonexistent
      fail("Should fail with nonexistent targetLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "sourceLayerId=orthography"
        +"&splitLabels=char"
        +"&targetLayerId=phone"
        +"&comparator=" // no comparator
        +"&mappingLayerId=disc");
      fail("Should fail with no comparator");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "sourceLayerId=orthography"
        +"&splitLabels=invalid-value"
        +"&targetLayerId=phone"
        +"&comparator=OrthographyToDISC"
        +"&mappingLayerId=disc");
      fail("Should fail with no comparator");
    } catch (InvalidConfigurationException x) {
    }

    // sub-mapping settings
    try {
      annotator.setTaskParameters(
        "sourceLayerId=orthography"
        +"&splitLabels="
        +"&targetLayerId=htkWord"
        +"&comparator=CharacterToCharacter"
        +"&mappingLayerId=wordComparison"
        +"&subSourceLayerId=non-existent"
        +"&subTargetLayerId=htkPhone"
        +"&subMappingLayerId=phoneComparison"
        +"&subComparator=DISCToDISC");
      fail("Should fail with nonexistent subSourceLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "sourceLayerId=orthography"
        +"&splitLabels="
        +"&targetLayerId=htkWord"
        +"&comparator=CharacterToCharacter"
        +"&mappingLayerId=wordComparison"
        +"&subSourceLayerId=phone"
        +"&subTargetLayerId=non-existent"
        +"&subMappingLayerId=phoneComparison"
        +"&subComparator=DISCToDISC");
      fail("Should fail with nonexistent subTargetLayerId");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "sourceLayerId=htkWord" // sourceLayer is phrase, but subSourceLayer is not
        +"&splitLabels="
        +"&targetLayerId=orthography"
        +"&comparator=CharacterToCharacter"
        +"&mappingLayerId=wordComparison"
        +"&subSourceLayerId=phone"
        +"&subTargetLayerId=htkPhone"
        +"&subMappingLayerId=phoneComparison"
        +"&subComparator=DISCToDISC");
      fail("Should fail with phrase label layer and non-phrase sub-mapping label layer");
    } catch (InvalidConfigurationException x) {
    }
    try {
      annotator.setTaskParameters(
        "sourceLayerId=orthography"
        +"&splitLabels="
        +"&targetLayerId=htkWord"
        +"&comparator=CharacterToCharacter"
        +"&mappingLayerId=wordComparison"
        +"&subSourceLayerId=phone"
        +"&subTargetLayerId=htkPhone"
        +"&subMappingLayerId=phoneComparison"
        +"&subComparator=");
      fail("Should fail with no sub-mapping comparator");
    } catch (InvalidConfigurationException x) {
    }
  }
  
  /** Test mapping of phoneme word labels to phones. */
  @Test public void DISCToDISC() throws Exception {
    LabelMapper annotator = newAnnotator();
    // annotator.getStatusObservers().add(status->System.out.println(status));
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // layers are created as required
    annotator.setTaskParameters(
      "sourceLayerId=phonemes"
      +"&splitLabels=char"
      +"&targetLayerId=phone"
      +"&comparator=DISCToDISC"
      +"&mappingLayerId=disc"); // nonexistent
    Layer layer = annotator.getSchema().getLayer("disc");
    assertNotNull("disc layer created", layer);
    assertEquals("disc layer correct type", Constants.TYPE_IPA, layer.getType());
    assertEquals("disc layer correct parent", "phone", layer.getParentId());
    assertEquals("disc layer alignment", Constants.ALIGNMENT_NONE, layer.getAlignment());

    // create a 'pre-existing' tag to ensure it's deleted or changed
    layer.setPeers(true); // fool the API into allowing more than one tag
    g.all("phone")[0].createTag("disc", "to-delete");
    g.commit();

    g.trackChanges();
    annotator.transform(g);
    g.commit(); // remove destroyed annotations
    
    Annotation[] phones = g.all("phone");
    assertEquals("Right number of phones " + Arrays.asList(phones), 15, phones.length);
    String[] phoneLabels = { "@", "d","I","f","@", "r","H", "t",  "f","2","@","f","2","t","@" };
    String[] discLabels = {  "1", "d","I","f",null,"r@","n","t",  "f","2","r","f","2","L","@r" };
    for (int p = 0; p < phones.length; p++) {
      assertEquals("Phone label " + p, phoneLabels[p], phones[p].getLabel());
      Annotation[] tags = phones[p].all("disc");
      if (discLabels[p] != null) {
        assertEquals("One tag " + p + " " + Arrays.asList(tags), 1, tags.length);
        assertNotNull("Tagged " + p, phones[p].first("disc"));
        assertEquals("Tag label " + p, discLabels[p], phones[p].first("disc").getLabel());
        assertEquals("Tag confidence " + p,
                     Constants.CONFIDENCE_AUTOMATIC,
                     phones[p].first("disc").getConfidence().intValue());
      } else {
        assertEquals("No tag " + p, 0, tags.length);
      }
    }
    assertEquals("Right number of tags " + Arrays.asList(g.all("disc")),
                 14, g.all("disc").length);
  }   

  /** Test mapping of orthography to phones. */
  @Test public void OrthographyToDISC() throws Exception {
    LabelMapper annotator = newAnnotator();
    //annotator.getStatusObservers().add(status->System.out.println(status));
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // layers are created as required
    annotator.setTaskParameters(
      "sourceLayerId=orthography"
      +"&splitLabels=char"
      +"&targetLayerId=phone"
      +"&comparator=OrthographyToDISC"
      +"&mappingLayerId=letter"); // nonexistent
    Layer layer = annotator.getSchema().getLayer("letter");
    assertNotNull("letter layer created", layer);
    assertEquals("letter layer correct type", Constants.TYPE_STRING, layer.getType());
    assertEquals("letter layer correct parent", "phone", layer.getParentId());
    assertEquals("letter layer alignment", Constants.ALIGNMENT_NONE, layer.getAlignment());

    g.trackChanges();
    annotator.transform(g);
    
    Annotation[] phones = g.all("phone");
    assertEquals("Right number of phones " + Arrays.asList(phones), 15, phones.length);
    String[] phoneLabels = { "@", "d","I","f", "@","r","H", "t", "f","2", "@","f","2",  "t","@" };
    String[] letterLabels = {"a", "d","i","ff","e","re","n","t", "f","ir","e","f","igh","t","er"};
    for (int p = 0; p < phones.length; p++) {
      assertEquals("Phone label " + p, phoneLabels[p], phones[p].getLabel());
      assertNotNull("Tagged " + p, phones[p].first("letter"));
      assertEquals("Tag label " + p, letterLabels[p], phones[p].first("letter").getLabel());
      assertEquals("Tag confidence " + p,
                   Constants.CONFIDENCE_AUTOMATIC,
                   phones[p].first("letter").getConfidence().intValue());
    }
  }   

  /** Test mapping of orthography to phones. */
  @Test public void ARPAbetToDISC() throws Exception {
    LabelMapper annotator = newAnnotator();
    //annotator.getStatusObservers().add(status->System.out.println(status));
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // layers are created as required
    annotator.setTaskParameters(
      "sourceLayerId=cmudict"
      +"&splitLabels=space"
      +"&targetLayerId=phone"
      +"&comparator=ArpabetToDISC"
      +"&mappingLayerId=arpabet"); // nonexistent
    Layer layer = annotator.getSchema().getLayer("arpabet");
    assertNotNull("arpabet layer created", layer);
    assertEquals("arpabet layer correct type", Constants.TYPE_STRING, layer.getType());
    assertEquals("arpabet layer correct parent", "phone", layer.getParentId());
    assertEquals("arpabet layer alignment", Constants.ALIGNMENT_NONE, layer.getAlignment());

    g.trackChanges();
    annotator.transform(g);
    
    Annotation[] phones = g.all("phone");
    assertEquals("Right number of phones " + Arrays.asList(phones), 15, phones.length);
    String[] phoneLabels = {"@",   "d","I",  "f","@", "r",    "H","t", "f","2",  "@","f","2",  "t","@" };
    String[] arpabetLabels={"EY1", "D","IH1","F",null,"R AH0","N","T", "F","AY1","R","F","AY2","T","ER0"};
    for (int p = 0; p < phones.length; p++) {
      assertEquals("Phone label " + p, phoneLabels[p], phones[p].getLabel());
      if (arpabetLabels[p] != null) {
        assertNotNull("Tagged " + p, phones[p].first("arpabet"));
        assertEquals("Tag label " + p, arpabetLabels[p], phones[p].first("arpabet").getLabel());
        assertEquals("Tag confidence " + p,
                     Constants.CONFIDENCE_AUTOMATIC,
                     phones[p].first("arpabet").getConfidence().intValue());
      } else {
        assertNull("Not tagged " + p, phones[p].first("arpabet"));
      }
    }
  }   

  /** Test mapping of alternative alignments for comparison, where tokens are on a word layer. */
  @Test public void alternativeAlignmentMappingToWord() throws Exception {
    LabelMapper annotator = newAnnotator();
    //annotator.getStatusObservers().add(status->System.out.println(status));
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // layers are created as required
    annotator.setTaskParameters(
      "sourceLayerId=htkWord"
      +"&splitLabels="
      +"&targetLayerId=orthography"
      +"&comparator=CharacterToCharacter"
      +"&mappingLayerId=wordComparison"
      +"&subSourceLayerId=htkPhone"
      +"&subTargetLayerId=phone"
      +"&subMappingLayerId=phoneComparison"
      +"&subComparator=ArpabetToDISC");
    
    Layer layer = annotator.getSchema().getLayer("wordComparison");
    assertNotNull("word comparison layer created", layer);
    assertEquals("word comparison layer correct type", Constants.TYPE_STRING, layer.getType());
    assertEquals("word comparison layer correct parent", "word", layer.getParentId());
    assertEquals("word comparison layer alignment",
                 Constants.ALIGNMENT_NONE, layer.getAlignment());
    
    layer = annotator.getSchema().getLayer("phoneComparison");
    assertNotNull("phone comparison layer created", layer);
    assertEquals("phone comparison layer correct type", Constants.TYPE_STRING, layer.getType());
    assertEquals("phone comparison layer correct parent", "phone", layer.getParentId());
    assertEquals("phone comparison layer alignment",
                 Constants.ALIGNMENT_NONE, layer.getAlignment());

    g.trackChanges();
    annotator.transform(g);
    g.commit(); // remove destroyed annotations
    
    Annotation[] orthography = g.all("orthography");
    assertEquals("Right number of words " + Arrays.asList(orthography), 3, orthography.length);
    for (int o = 0; o < orthography.length; o++) {
      Annotation wordComparison = orthography[o].first("wordComparison");
      assertNotNull("Word mapped " + o, wordComparison);
      assertEquals("Word labels match " + o,
                   orthography[o].getLabel(),
                   wordComparison.getLabel().toLowerCase());
      assertTrue("Comparison is a tag " + o, wordComparison.tags(orthography[o]));
      assertEquals("Comparison has word parent " + o,
                   orthography[o].getParent(), wordComparison.getParent());
    }
    assertEquals("Right number of tags " + Arrays.asList(g.all("wordComparison")),
                 3, g.all("wordComparison").length);
    
    Annotation[] phone = g.all("phone");
    String[] expectedPhone = {
      "EY1", "D","IH1","F",null,"R","N","T", "F","AY1","R","F","AY2","T","ER0"};
    assertEquals("Right number of phones " + Arrays.asList(phone), 15, phone.length);
    for (int p = 0; p < phone.length; p++) {
      Annotation phoneComparison = phone[p].first("phoneComparison");
      if (expectedPhone[p] != null) {
        assertNotNull("Phone mapped " + p, phoneComparison);
        assertEquals("Phone labels match " + p,
                     expectedPhone[p],
                     phoneComparison.getLabel());
        assertTrue("Comparison is a tag " + p, phoneComparison.tags(phone[p]));
        assertEquals("Comparison has phone parent " + p,
                     phone[p], phoneComparison.getParent());
      } else { // not expecting a mapping
        assertNull("Phone not mapped " + p, phoneComparison);
      }
    }
    assertEquals("Right number of tags " + Arrays.asList(g.all("phoneComparison")),
                 14, g.all("phoneComparison").length); // one was not mapped
  }   

  /** Test mapping of alternative alignments for comparison, tokens are on phrase layer. */
  @Test public void alternativeAlignmentMappingToPhrase() throws Exception {
    LabelMapper annotator = newAnnotator();
    //annotator.getStatusObservers().add(status->System.out.println(status));
    
    Graph g = graph();
    Schema schema = g.getSchema();
    annotator.setSchema(schema);
    
    // layers are created as required
    annotator.setTaskParameters(
      "sourceLayerId=orthography"
      +"&splitLabels="
      +"&targetLayerId=htkWord"
      +"&comparator=CharacterToCharacter"
      +"&mappingLayerId=wordComparison"
      +"&subSourceLayerId=phone"
      +"&subTargetLayerId=htkPhone"
      +"&subMappingLayerId=phoneComparison"
      +"&subComparator=DISCToArpabet");
    Layer layer = annotator.getSchema().getLayer("wordComparison");
    assertNotNull("wordComparison layer created", layer);
    assertEquals("wordComparison parent", "turn", layer.getParentId());
    assertEquals("wordComparison type", Constants.TYPE_STRING, layer.getType());
    assertEquals("wordComparison alignment", Constants.ALIGNMENT_INTERVAL, layer.getAlignment());
    layer = annotator.getSchema().getLayer("phoneComparison");
    assertNotNull("phoneComparison layer created", layer);
    assertEquals("phoneComparison parent", "turn", layer.getParentId());
    assertEquals("phoneComparison type", Constants.TYPE_IPA, layer.getType());
    assertEquals("phoneComparison alignment", Constants.ALIGNMENT_INTERVAL, layer.getAlignment());

    g.trackChanges();
    annotator.transform(g);
    g.commit(); // remove destroyed annotations
    
    Annotation[] htkWord = g.all("htkWord");
    assertEquals("Right number of words " + Arrays.asList(htkWord), 3, htkWord.length);
    for (int w = 0; w < htkWord.length; w++) {
      Annotation wordComparison = htkWord[w].first("wordComparison");
      assertNotNull("Word mapped " + w, wordComparison);
      assertEquals("Word labels match " + w,
                   htkWord[w].getLabel().toLowerCase(),
                   wordComparison.getLabel());
      assertTrue("Comparison is a tag " + w, wordComparison.tags(htkWord[w]));
      assertEquals("Comparison has turn parent " + w,
                   htkWord[w].getParent(), wordComparison.getParent());
    }
    assertEquals("Right number of tags " + Arrays.asList(g.all("wordComparison")),
                 3, g.all("wordComparison").length);
    
    Annotation[] htkPhone = g.all("htkPhone");
    String[] expectedPhone = {
      "@", "d","I","f","r",null,"H","t", "f","2","@","f","2","t","@"};
    assertEquals("Right number of phones " + Arrays.asList(htkPhone), 15, htkPhone.length);
    for (int p = 0; p < htkPhone.length; p++) {
      Annotation phoneComparison = htkPhone[p].first("phoneComparison");
      if (expectedPhone[p] != null) {
        assertNotNull("Phone mapped " + p, phoneComparison);
        assertEquals("Phone labels match " + p,
                     expectedPhone[p],
                     phoneComparison.getLabel());
        assertTrue("Comparison is a tag " + p, phoneComparison.tags(htkPhone[p]));
        assertEquals("Comparison has turn parent " + p,
                     htkPhone[p].getParent(), phoneComparison.getParent());
      } else { // not expecting a mapping
        assertNull("Phone not mapped " + p, phoneComparison);
      }
    }
    assertEquals("Right number of tags " + Arrays.asList(g.all("phoneComparison")),
                 14, g.all("phoneComparison").length); // one was not mapped

    // check relational database content
    File dir = dir();

    // word mapping    
    InputStream stream = annotator.mappingToCsv("orthography", "htkWord");
    File csv = new File(dir(), "orthography-htkWord.csv");
    IO.SaveInputStreamToFile(stream, csv);
    String differences = diff(new File(dir, "expected_orthography-htkWord.csv"), csv);
    if (differences != null) {
      fail(differences);
    } else {
      csv.delete();
    }
    
    // phone mapping
    stream = annotator.mappingToCsv("phone", "htkPhone");
    csv = new File(dir(), "phone-htkPhone.csv");
    IO.SaveInputStreamToFile(stream, csv);
    differences = diff(new File(dir, "expected_phone-htkPhone.csv"), csv);
    if (differences != null) {
      fail(differences);
    } else {
      csv.delete();
    }

    Map<String,Double> summary = annotator.summarizeMapping("phone", "htkPhone");
    assertEquals("Summary: utteranceCount",
                 Double.valueOf(1), summary.get("utteranceCount"));
    assertEquals("Summary: stepCount",
                 Double.valueOf(16), summary.get("stepCount"));
    assertEquals("Summary: sourceCount",
                 Double.valueOf(15), summary.get("sourceCount"));
    assertEquals("Summary: targetCount",
                 Double.valueOf(15), summary.get("targetCount"));
    assertEquals("Summary: meanOverlapRate",
                 Double.valueOf(0.12932564330079857), summary.get("meanOverlapRate"));
  }   

  /**
   * Returns a graph for annotating.
   * @return The graph for testing with.
   */
  public static Graph graph() throws Exception {
      Schema schema = new Schema(
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
         new Layer("htkWord", "Alternative Word Alignments")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false)
         .setParentId("turn").setParentIncludes(true),
         new Layer("htkPhone", "Alternative Phone Alignments")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false)
         .setParentId("turn").setParentIncludes(true),
         new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false)
         .setParentId("turn").setParentIncludes(true),
         new Layer("orthography", "Orthography").setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true)
         .setParentId("word").setParentIncludes(true),
         new Layer("phonemes", "Phonemic transcription").setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true)
         .setParentId("word").setParentIncludes(true)
         .setType(Constants.TYPE_IPA),
         new Layer("cmudict", "CMU ARPAbet pronunciation").setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(false).setPeersOverlap(false).setSaturated(true)
         .setParentId("word").setParentIncludes(true),
         new Layer("phone", "Phones").setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(true)
         .setParentId("word").setParentIncludes(true)
         .setType(Constants.TYPE_IPA));
      // annotate a graph
      Graph g = new Graph()
        .setSchema(schema);
      g.setId("TestLabelMapper");
      
      Anchor start = g.getOrCreateAnchorAt(1);
      Anchor end = g.getOrCreateAnchorAt(100);
      g.addAnnotation(
         new Annotation().setLayerId("participant").setLabel("someone")
         .setStart(start).setEnd(end));
      Annotation turn = g.addAnnotation(
         new Annotation().setLayerId("turn").setLabel("someone")
         .setStart(start).setEnd(end)
         .setParent(g.first("participant")));
      g.addAnnotation(
         new Annotation().setLayerId("utterance").setLabel("someone")
         .setStart(start).setEnd(end)
         .setParent(turn));

      // words
      
      Annotation a = new Annotation().setLayerId("word").setLabel("A")
        .setStart(g.getOrCreateAnchorAt(10)).setEnd(g.getOrCreateAnchorAt(20)).setParent(turn);
      g.addAnnotation(a);
      Annotation different = new Annotation().setLayerId("word").setLabel("different")
        .setStart(g.getOrCreateAnchorAt(20)).setEnd(g.getOrCreateAnchorAt(30)).setParent(turn);
      g.addAnnotation(different);
      Annotation firefighter = new Annotation().setLayerId("word").setLabel("firefighter!")
        .setStart(g.getOrCreateAnchorAt(30)).setEnd(g.getOrCreateAnchorAt(40)).setParent(turn);
      g.addAnnotation(firefighter);

      // orthography
      
      g.addAnnotation(new Annotation().setLayerId("orthography").setLabel("a")
                      .setParent(a));
      g.addAnnotation(new Annotation().setLayerId("orthography").setLabel("different")
                      .setParent(different));
      g.addAnnotation(new Annotation().setLayerId("orthography").setLabel("firefighter")
                      .setParent(firefighter));

      // phonemes (DISC)
      
      g.addAnnotation(new Annotation().setLayerId("phonemes").setLabel("1")
                      .setParent(a));
      g.addAnnotation(new Annotation().setLayerId("phonemes").setLabel("dIfr@nt")
                      .setParent(different));
      g.addAnnotation(new Annotation().setLayerId("phonemes").setLabel("f2rf2L@r")
                      .setParent(firefighter));

      // phonemes (ARPAbet)
      
      g.addAnnotation(new Annotation().setLayerId("cmudict").setLabel("EY1")
                      .setParent(a));
      g.addAnnotation(new Annotation().setLayerId("cmudict").setLabel("D IH1 F R AH0 N T")
                      .setParent(different));
      g.addAnnotation(new Annotation().setLayerId("cmudict").setLabel("F AY1 R F AY2 T ER0")
                      .setParent(firefighter));

      // phones
      
      g.addAnnotation(new Annotation().setLayerId("phone").setLabel("@")
                      .setStart(g.getOrCreateAnchorAt(10)).setEnd(g.getOrCreateAnchorAt(20))
                      .setParent(a));

      g.addAnnotation(new Annotation().setLayerId("phone").setLabel("d")
                      .setStart(g.getOrCreateAnchorAt(20)).setEnd(g.getOrCreateAnchorAt(21))
                      .setParent(different));
      g.addAnnotation(new Annotation().setLayerId("phone").setLabel("I")
                      .setStart(g.getOrCreateAnchorAt(21)).setEnd(g.getOrCreateAnchorAt(22))
                      .setParent(different));
      g.addAnnotation(new Annotation().setLayerId("phone").setLabel("f")
                      .setStart(g.getOrCreateAnchorAt(22)).setEnd(g.getOrCreateAnchorAt(24))
                      .setParent(different));
      g.addAnnotation(new Annotation().setLayerId("phone").setLabel("@")
                      .setStart(g.getOrCreateAnchorAt(24)).setEnd(g.getOrCreateAnchorAt(26))
                      .setParent(different));
      g.addAnnotation(new Annotation().setLayerId("phone").setLabel("r")
                      .setStart(g.getOrCreateAnchorAt(26)).setEnd(g.getOrCreateAnchorAt(27))
                      .setParent(different));
      g.addAnnotation(new Annotation().setLayerId("phone").setLabel("H")
                      .setStart(g.getOrCreateAnchorAt(28)).setEnd(g.getOrCreateAnchorAt(29))
                      .setParent(different));
      g.addAnnotation(new Annotation().setLayerId("phone").setLabel("t")
                      .setStart(g.getOrCreateAnchorAt(29)).setEnd(g.getOrCreateAnchorAt(30))
                      .setParent(different));

      g.addAnnotation(new Annotation().setLayerId("phone").setLabel("f")
                      .setStart(g.getOrCreateAnchorAt(30)).setEnd(g.getOrCreateAnchorAt(31))
                      .setParent(firefighter));
      g.addAnnotation(new Annotation().setLayerId("phone").setLabel("2")
                      .setStart(g.getOrCreateAnchorAt(31)).setEnd(g.getOrCreateAnchorAt(33))
                      .setParent(firefighter));
      g.addAnnotation(new Annotation().setLayerId("phone").setLabel("@")
                      .setStart(g.getOrCreateAnchorAt(33)).setEnd(g.getOrCreateAnchorAt(35))
                      .setParent(firefighter));
      g.addAnnotation(new Annotation().setLayerId("phone").setLabel("f")
                      .setStart(g.getOrCreateAnchorAt(35)).setEnd(g.getOrCreateAnchorAt(37))
                      .setParent(firefighter));
      g.addAnnotation(new Annotation().setLayerId("phone").setLabel("2")
                      .setStart(g.getOrCreateAnchorAt(37)).setEnd(g.getOrCreateAnchorAt(38))
                      .setParent(firefighter));
      g.addAnnotation(new Annotation().setLayerId("phone").setLabel("t")
                      .setStart(g.getOrCreateAnchorAt(38)).setEnd(g.getOrCreateAnchorAt(39))
                      .setParent(firefighter));
      g.addAnnotation(new Annotation().setLayerId("phone").setLabel("@")
                      .setStart(g.getOrCreateAnchorAt(39)).setEnd(g.getOrCreateAnchorAt(40))
                      .setParent(firefighter));
      
      // alternative alignments
      
      g.addAnnotation(new Annotation().setLayerId("htkWord").setLabel("A")
                      .setStart(g.getOrCreateAnchorAt(11.5)).setEnd(g.getOrCreateAnchorAt(21.5))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("htkPhone").setLabel("EY1")
                      .setStart(g.getOrCreateAnchorAt(11.5)).setEnd(g.getOrCreateAnchorAt(21.5))
                      .setParent(turn));

      g.addAnnotation(new Annotation().setLayerId("htkWord").setLabel("DIFFERENT")
                      .setStart(g.getOrCreateAnchorAt(21.5)).setEnd(g.getOrCreateAnchorAt(31.5))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("htkPhone").setLabel("D")
                      .setStart(g.getOrCreateAnchorAt(21.5)).setEnd(g.getOrCreateAnchorAt(22.5))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("htkPhone").setLabel("IH1")
                      .setStart(g.getOrCreateAnchorAt(22.5)).setEnd(g.getOrCreateAnchorAt(23.5))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("htkPhone").setLabel("F")
                      .setStart(g.getOrCreateAnchorAt(23.5)).setEnd(g.getOrCreateAnchorAt(25.5))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("htkPhone").setLabel("R")
                      .setStart(g.getOrCreateAnchorAt(25.5)).setEnd(g.getOrCreateAnchorAt(27.5))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("htkPhone").setLabel("AH0")
                      .setStart(g.getOrCreateAnchorAt(27.5)).setEnd(g.getOrCreateAnchorAt(28.5))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("htkPhone").setLabel("N")
                      .setStart(g.getOrCreateAnchorAt(29.5)).setEnd(g.getOrCreateAnchorAt(30.5))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("htkPhone").setLabel("T")
                      .setStart(g.getOrCreateAnchorAt(30.5)).setEnd(g.getOrCreateAnchorAt(31.5))
                      .setParent(turn));

      g.addAnnotation(new Annotation().setLayerId("htkWord").setLabel("FIREFIGHTER")
                      .setStart(g.getOrCreateAnchorAt(31.5)).setEnd(g.getOrCreateAnchorAt(41.5))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("htkPhone").setLabel("F")
                      .setStart(g.getOrCreateAnchorAt(31.5)).setEnd(g.getOrCreateAnchorAt(32.5))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("htkPhone").setLabel("AY1")
                      .setStart(g.getOrCreateAnchorAt(32.5)).setEnd(g.getOrCreateAnchorAt(34.5))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("htkPhone").setLabel("R")
                      .setStart(g.getOrCreateAnchorAt(34.5)).setEnd(g.getOrCreateAnchorAt(36.5))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("htkPhone").setLabel("F")
                      .setStart(g.getOrCreateAnchorAt(36.5)).setEnd(g.getOrCreateAnchorAt(38.5))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("htkPhone").setLabel("AY2")
                      .setStart(g.getOrCreateAnchorAt(38.5)).setEnd(g.getOrCreateAnchorAt(39.5))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("htkPhone").setLabel("T")
                      .setStart(g.getOrCreateAnchorAt(39.5)).setEnd(g.getOrCreateAnchorAt(40.5))
                      .setParent(turn));
      g.addAnnotation(new Annotation().setLayerId("htkPhone").setLabel("ER0")
                      .setStart(g.getOrCreateAnchorAt(40.5)).setEnd(g.getOrCreateAnchorAt(41.5))
                      .setParent(turn));
      
      return g;
  } // end of graph()

  /**
   * Diffs two files.
   * @param expected
   * @param actual
   * @return null if the files are the same, and a String describing differences if not.
   */
  public String diff(File expected, File actual) {
    StringBuffer d = new StringBuffer();
    
    try {
      // compare with what we expected
      Vector<String> actualLines = new Vector<String>();
      BufferedReader reader = new BufferedReader(new FileReader(actual));
      String line = reader.readLine();
      while (line != null) {
        actualLines.add(line);
        line = reader.readLine();
      }
      Vector<String> expectedLines = new Vector<String>();
      reader = new BufferedReader(new FileReader(expected));
      line = reader.readLine();
      while (line != null) {
        expectedLines.add(line);
        line = reader.readLine();
      }
      MinimumEditPath<String> comparator = new MinimumEditPath<String>();
      List<EditStep<String>> path = comparator.minimumEditPath(expectedLines, actualLines);
      for (EditStep<String> step : path) {
        switch (step.getOperation()) {
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
    } catch(Exception exception) {
      d.append("\n" + exception);
    }
    if (d.length() > 0) return d.toString();
    return null;
  } // end of diff()
  
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.annotator.labelmapper.TestLabelMapper");
  }
}
