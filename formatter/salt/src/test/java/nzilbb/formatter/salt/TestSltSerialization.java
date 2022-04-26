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

package nzilbb.formatter.salt;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.Vector;
import java.util.stream.Collectors;
import nzilbb.ag.*;
import nzilbb.ag.util.DefaultOffsetGenerator;
import nzilbb.ag.serialize.SerializationException;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.editpath.EditStep;
import nzilbb.editpath.MinimumEditPath;

public class TestSltSerialization {

  /** General deserialization, map some specific codes to specific layers. */
  @Test public void deserializeMapCodesToLayers()  throws Exception {
    Schema schema = new Schema(
      "participant", "turn", "utterance", "word",
      new Layer("transcript_language", "Language").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("transcript_doe", "Recording date").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("transcript_ca", "Current Age").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("transcript_context", "Context").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("transcript_subgroup", "Subgroup/Story").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("transcript_collect", "Collection Point").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("transcript_location", "Location").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      
      new Layer("comment", "Comments").setAlignment(Constants.ALIGNMENT_INSTANT)
      .setPeers(true).setPeersOverlap(true).setSaturated(false),
      new Layer("noise", "Noises").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false),
      
      new Layer("participant", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("main_participant", "Target Speaker").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("participant").setParentIncludes(true),
      new Layer("participant_id", "Participant ID").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("participant").setParentIncludes(true),
      new Layer("participant_gender", "Gender").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("participant").setParentIncludes(true),
      new Layer("participant_dob", "Birth Date").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("participant").setParentIncludes(true),
      new Layer("participant_ethnicity", "Ethnicity").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("participant").setParentIncludes(true),
      
      new Layer("turn", "Speaker Turn").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("participant").setParentIncludes(true),
      new Layer("utterance", "Lines").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),

      new Layer("pause", "Pauses").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("cunit", "C-Unit").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("parenthetical", "Parentheticals").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("repetition", "Repetitions").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("error", "Errors").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("EU", "Utterance Errors").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("code", "Non-error Codes").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("maze", "Mazes").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("sound_effect", "Verbal sound effects etc.")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("entity", "Proper Names").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("omission", "Omissions").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),

      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      
      new Layer("root", "Root form").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true),
      new Layer("bound_morpheme", "Bound Morphemes").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true),
      new Layer("partial_word", "Partial Words").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true),
      new Layer("redacted", "Redacted names").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true),
      new Layer("ep", "EP errors").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true),
      new Layer("lexical", "Lexical annotations").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true)
      );
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "test.slt")) };
    
    // create deserializer
    SltSerialization deserializer = new SltSerialization();
    
    // general configuration
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) {
    //   System.out.println("" + p.getName() + " = " + p.getValue());
    // }
    assertEquals("Correct number of configuration parameters", 28, configuration.size());
    assertEquals(schema.getLayer("cunit"),
                 configuration.get("cUnitLayer").getValue());
    assertEquals(schema.getLayer("main_participant"),
                 configuration.get("targetParticipantLayer").getValue());
    assertEquals(schema.getLayer("comment"),
                 configuration.get("commentLayer").getValue());
    assertEquals(schema.getLayer("parenthetical"),
                 configuration.get("parentheticalLayer").getValue());
    assertEquals(schema.getLayer("entity"),
                 configuration.get("properNameLayer").getValue());
    assertEquals(schema.getLayer("repetition"),
                 configuration.get("repetitionsLayer").getValue());
    assertEquals(schema.getLayer("root"),
                 configuration.get("rootLayer").getValue());
    assertEquals(schema.getLayer("error"),
                 configuration.get("errorLayer").getValue());
    assertEquals(schema.getLayer("sound_effect"),
                 configuration.get("soundEffectLayer").getValue());
    assertEquals(schema.getLayer("pause"),
                 configuration.get("pauseLayer").getValue());
    assertEquals(schema.getLayer("bound_morpheme"),
                 configuration.get("boundMorphemeLayer").getValue());
    assertEquals(schema.getLayer("maze"),
                 configuration.get("mazeLayer").getValue());
    assertEquals(schema.getLayer("partial_word"),
                 configuration.get("partialWordLayer").getValue());
    assertEquals(schema.getLayer("omission"),
                 configuration.get("omissionLayer").getValue());
    assertEquals(schema.getLayer("code"),
                 configuration.get("codeLayer").getValue());
    assertEquals(schema.getLayer("transcript_language"),
                 configuration.get("languageLayer").getValue());
    assertEquals(schema.getLayer("participant_id"),
                 configuration.get("participantIdLayer").getValue());
    assertEquals(schema.getLayer("participant_gender"),
                 configuration.get("genderLayer").getValue());
    assertEquals(schema.getLayer("participant_dob"),
                 configuration.get("dobLayer").getValue());
    assertEquals(schema.getLayer("transcript_doe"),
                 configuration.get("doeLayer").getValue());
    assertEquals(schema.getLayer("transcript_ca"),
                 configuration.get("caLayer").getValue());
    assertEquals(schema.getLayer("participant_ethnicity"),
                 configuration.get("ethnicityLayer").getValue());
    assertEquals(schema.getLayer("transcript_context"),
                 configuration.get("contextLayer").getValue());
    assertEquals(schema.getLayer("transcript_subgroup"),
                 configuration.get("subgroupLayer").getValue());
    assertEquals(schema.getLayer("transcript_collect"),
                 configuration.get("collectLayer").getValue());
    assertEquals(schema.getLayer("transcript_location"),
                 configuration.get("locationLayer").getValue());
    assertEquals("parseInlineConventions is true default",
                 Boolean.TRUE, configuration.get("parseInlineConventions").getValue());
    // change to day-first
    configuration.get("dateFormat").setValue("d/M/yyyy");

    // final configuration
    deserializer.configure(configuration, schema);
    
    // load the stream
    ParameterSet parameters = deserializer.load(streams, schema);
    // for (Parameter p : parameters.values()) {
    //   System.out.println("" + p.getName() + " = " + p.getValue());
    // }
    assertEquals("One parameter for each type of code, plus noise comments: "+parameters.keySet(),
                 7, parameters.size());
    assertEquals("REDACTED codes mapped to redacted layer",
                 schema.getLayer("redacted"), parameters.get("code_REDACTED").getValue());
    assertEquals("CENSORED mapped to default code layer",
                 schema.getLayer("code"), parameters.get("code_CENSOR").getValue());
    assertEquals("EP codes mapped to ep layer",
                 schema.getLayer("ep"), parameters.get("code_EP").getValue());
    assertEquals("LEXICAL codes mapped to lexical layer",
                 schema.getLayer("lexical"), parameters.get("code_LEXICAL").getValue());
    assertEquals("EU codes mapped to EU layer",
                 schema.getLayer("EU"), parameters.get("code_EU").getValue());
    assertEquals("EW mapped to default error layer",
                 schema.getLayer("error"), parameters.get("code_EW").getValue());
    assertEquals("NOISE mapped to noise layer",
                 schema.getLayer("noise"), parameters.get("comment_NOISE").getValue());

    // configure the deserialization
    deserializer.setParameters(parameters);
    
    // build the graph
    Graph[] graphs = deserializer.deserialize();
    assertEquals("conversion is 1-1", 1, graphs.length);
    Graph g = graphs[0];
    
    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
    
    assertEquals("test.slt", g.getId());

    // meta-data
    assertEquals("Language set", 1, g.all("transcript_language").length);
    assertEquals("Language correct", "en", g.first("transcript_language").getLabel());
    assertEquals("Doe set", 1, g.all("transcript_doe").length);
    assertEquals("Doe correct", "2021-03-30", g.first("transcript_doe").getLabel());
    assertEquals("Ca set", 1, g.all("transcript_ca").length);
    assertEquals("Ca correct", "4;4", g.first("transcript_ca").getLabel());
    assertEquals("Context set", 1, g.all("transcript_context").length);
    assertEquals("Context correct", "Nar", g.first("transcript_context").getLabel());
    assertEquals("Subgroup set", 1, g.all("transcript_subgroup").length);
    assertEquals("Subgroup correct", "TEST", g.first("transcript_subgroup").getLabel());
    assertEquals("Collect set", 1, g.all("transcript_collect").length);
    assertEquals("Collect correct", "1", g.first("transcript_collect").getLabel());
    assertEquals("Location not set", 0, g.all("transcript_location").length);
    
    // participants     
    assertEquals("Two participants", 2, g.all("participant").length);
    assertEquals("One target", 1, g.all("main_participant").length);
    Annotation child = g.first("main_participant").getParent();
    assertEquals("Target participant prefixed with value of Name header",
                 "ADAL-Child", child.getLabel());
    Annotation examiner = null;
    for (Annotation p : g.all("participant")) {
      if (!p.getId().equals(child.getId())) {
        examiner = p;
        break;
      }
    }
    assertNotNull("Examiner found", examiner);
    assertEquals("Examiner name prefixed with value of Name header",
                 "ADAL-Examiner", examiner.getLabel());
    
    // participant meta data
    assertEquals("ParticipantId correct",
                 "Ada-Lovelace", child.first("participant_id").getLabel());
    assertEquals("Gender correct",
                 "F", child.first("participant_gender").getLabel());
    assertEquals("Dob correct",
                 "1972-09-26", child.first("participant_dob").getLabel());
    assertEquals("Ethnicity correct",
                 "NZ European", child.first("participant_ethnicity").getLabel());
    assertNull("Examiner ParticipantId not set", examiner.first("participant_id"));
    assertNull("Examiner Gender not set", examiner.first("participant_gender"));
    assertNull("Examiner Dob not set", examiner.first("participant_dob"));
    assertNull("Examiner Ethnicity not set", examiner.first("participant_ethnicity"));

     // turns
    Annotation[] turns = g.all("turn");
    assertEquals(12, turns.length);

    // check turn timestamps
    double[] turnTimeStamps = {
      0, 29, 34.56, 39, 47, 52, 62, 63, 69, 71, 86, 93 };
    Annotation lastTurn = null;
    assertEquals("Examiner is first", "ADAL-Examiner", turns[0].getLabel());
    for (int t = 0; t < turns.length; t++) {
      assertEquals("start of turn " + t,
                   Double.valueOf(turnTimeStamps[t]), turns[t].getStart().getOffset());
      assertEquals("confidence of start of turn " + t + " " + turns[t].getStart(),
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL),
                   turns[t].getStart().getConfidence());
      if (lastTurn != null) {
        assertEquals("End of turn " + (t-1),
                     turns[t].getStart(), lastTurn.getEnd());
        assertNotEquals("Speaker has changed",
                        lastTurn.getLabel(), turns[t].getLabel());
      }
      lastTurn = turns[t];
    } // next turn
    // transcript has no end timestamp, so it should be 1s after the last known timestamp
    assertEquals("Dummy last timestamp (" + lastTurn.getStart() + "-" + lastTurn.getEnd() + ")",
                 Double.valueOf(94.0), lastTurn.getEnd().getOffset());
    assertEquals("Dummy last timestamp has low confidence",
                 Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC),
                 lastTurn.getEnd().getConfidence());
      
    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals(21, utterances.length);
    
    // check utterance timestamps and speakers
    double[] utteranceTimeStamps = {
      0, 5, 12, 16, 20, 24, 29, 34.56, 37, 39, 41, 46, 47, 52, 62, 63, 69, 71, 83, 86, 93 };
    String[] utteranceSpeakers = {
      "ADAL-Examiner", "ADAL-Examiner", "ADAL-Examiner", "ADAL-Examiner", "ADAL-Examiner", "ADAL-Examiner",
      "ADAL-Child",
      "ADAL-Examiner", "ADAL-Examiner",
      "ADAL-Child", "ADAL-Child", "ADAL-Child",
      "ADAL-Examiner",
      "ADAL-Child",
      "ADAL-Examiner",
      "ADAL-Child",
      "ADAL-Examiner",
      "ADAL-Child", "ADAL-Child",
      "ADAL-Examiner",
      "ADAL-Child"
    };
    Annotation lastUtterance = null;
    for (int u = 0; u < utterances.length; u++) {
      assertEquals("start of utterance " + u,
                   Double.valueOf(utteranceTimeStamps[u]), utterances[u].getStart().getOffset());
      if (u != 3 && u != 4) {
        assertEquals("confidence of start of utterance " + u,
                     Integer.valueOf(Constants.CONFIDENCE_MANUAL),
                     utterances[u].getStart().getConfidence());
      } else { // u == 3 or 4 are special cases:
        // it has the same start time as the previous utterance,
        // so the offset has been bumped forward and given lower confidence
        assertEquals("confidence of bumped start of utterance " + u,
                     Integer.valueOf(Constants.CONFIDENCE_DEFAULT),
                     utterances[u].getStart().getConfidence());
      }
      assertEquals("speaker of utterance " + u + " ("+utterances[u].getStart()+")",
                   utteranceSpeakers[u], utterances[u].getParent().getLabel());
      if (lastUtterance != null) {
        assertEquals("End of utterance " + (u-1) + " ("+utterances[u-1].getStart()+")",
                     utterances[u].getStart(), lastUtterance.getEnd());
      }
      lastUtterance = utterances[u];
    } // next utterance
    // transcript has no end timestamp, so it should be 1s after the last known timestamp
    assertEquals("Dummy last timestamp",
                 Double.valueOf(94.0), lastUtterance.getEnd().getOffset());
    assertEquals("Dummy last timestamp has low confidence",
                 Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC),
                 lastUtterance.getEnd().getConfidence());

    // words
    Annotation[] words = g.all("word");
    assertEquals("Word count correct", 177, words.length);

    // check utterance transcriptions
    String[] lines = {
      "I'm at Byron kindergarten on what date is it the 30th of March 2021?",
      "My name is _. and I'm here with _, doing the oral language assessment.",
      "Okay, so now Ada it's your turn to tell the story.",
      "You can look at the pictures when you're telling the story.",
      "So let's start at the beginning.",
      "What was the story about?",
      "Um the kids the kids, they quickly put their gumboots on.",
      "saved muddy bushes buses putting girl's wants goes shopping> running dropped helped aunty's stopped leaving coming its lift. Hugging Hunting can't can't carried eating gently gently girl's goes guys happening helping hugged hurried kid's learning let's says seeing tramping tried tweeting waiting?",
      "Anything else?",
      "And please go for a walk?",
      "You need to put your gumboots on.",
      "It's too dark^",
      "What happened in this one?",
      "And then it's heaps and heaps dark.",
      "What happened next?",
      "Schnitzel von Krumm s~ falled out the babies' nest.",
      "What happened next?",
      "They put them it back in the nest um.",
      "Bye bye little bird.",
      "Anything else that happened?",
      "_."
    };
    for (int u = 0; u < utterances.length; u++) {
      String transcription = Arrays.stream(utterances[u].all("word"))
        .map(word -> word.getLabel())
        .collect(Collectors.joining(" "));
      assertEquals("Transcription of utterance " + u + " ("+utterances[u].getStart()+")",
                   lines[u], transcription);
    }        
      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }

    // codes
    Annotation[] codes = g.all("code");
    assertEquals("Correct number of codes: " + Arrays.asList(codes),
                 1, codes.length);
    assertEquals("Censor label", "CENSOR", codes[0].getLabel());
    assertEquals("Censor code tags an utterance",
                 1, codes[0].tagsOn("utterance").length);
    assertEquals("Censor code tags the second utterance",
                 utterances[1].getId(), codes[0].tagsOn("utterance")[0].getId());

    codes = g.all("redacted");
    assertEquals("Correct number of redacted codes: " + Arrays.asList(codes),
                 2, codes.length);
    assertEquals("First Redacted label", "REDACTED", codes[0].getLabel());
    assertEquals("First Redacted code tags a word",
                 1, codes[0].tagsOn("word").length);
    assertEquals("First Redacted tags the 18th word: " + codes[0].tagsOn("word"),
                 words[17].getId(), codes[0].tagsOn("word")[0].getId());
    assertEquals("Second Redacted label", "REDACTED", codes[1].getLabel());
    assertEquals("Second Redacted code tags a word",
                 1, codes[1].tagsOn("word").length);
    assertEquals("Second Redacted tags the 23rd word: " + codes[1].tagsOn("word")[0],
                 words[22].getId(), codes[1].tagsOn("word")[0].getId());

    codes = g.all("lexical");
    assertEquals("Correct number of lexical codes: " + Arrays.asList(codes),
                 1, codes.length);
    assertEquals("lexical label", "#is#", codes[0].getLabel());
    assertEquals("lexical code tags a word",
                 1, codes[0].tagsOn("word").length);
    assertEquals("lexical code tags \"s~\": " + codes[0].tagsOn("word")[0],
                 "s~", codes[0].tagsOn("word")[0].getLabel());

    // errors
    Annotation[] errors = g.all("error");
    assertEquals("Correct number of error codes: " + Arrays.asList(errors),
                 1, errors.length);
    assertEquals("EW label", "EW", errors[0].getLabel());
    assertEquals("EW code tags a word",
                 1, errors[0].tagsOn("word").length);
    assertEquals("EW tags the word 'falled': " + Arrays.asList(errors[0].tagsOn("word")),
                 "falled", errors[0].tagsOn("word")[0].getLabel());

    errors = g.all("ep");
    assertEquals("EP label", "boy/z", errors[0].getLabel());
    assertEquals("EP code tags a word",
                 1, errors[0].tagsOn("word").length);
    assertEquals("EP tags the word \"girl's\": " + Arrays.asList(errors[0].tagsOn("word")),
                 "girl's", errors[0].tagsOn("word")[0].getLabel());

    errors = g.all("EU");
    assertEquals("Correct number of EU codes: " + Arrays.asList(errors),
                 2, errors.length);
    assertEquals("Utterance Error label", "EU", errors[0].getLabel());
    assertEquals("Utterance Error code tags an utterance",
                 1, errors[0].tagsOn("utterance").length);
    assertEquals("Utterance Error code tags the tenth utterance",
                 utterances[9].getId(), errors[0].tagsOn("utterance")[0].getId());
    assertEquals("Pre-terminator Utterance Error label", "EU", errors[1].getLabel());
    assertEquals("Pre-terminator Utterance Error code tags an utterance",
                 1, errors[1].tagsOn("utterance").length);
    assertEquals("Pre-terminator Utterance Error code tags the eleventh utterance",
                 utterances[10].getId(), errors[1].tagsOn("utterance")[0].getId());

    // root forms
    Annotation[] roots = g.all("root");
    assertEquals("Correct number of root forms: " + Arrays.asList(roots),
                 1, roots.length);
    assertEquals("root label", "fall", roots[0].getLabel());
    assertEquals("root tags a word",
                 1, roots[0].tagsOn("word").length);
    assertEquals("root tags the word 'falled': " + Arrays.asList(roots[0].tagsOn("word")),
                 "falled", roots[0].tagsOn("word")[0].getLabel());

    // bound morphemes
    Annotation[] boundMorphemes = g.all("bound_morpheme");
    assertEquals("Correct number of bound morphemes: " + Arrays.asList(boundMorphemes),
                 48, boundMorphemes.length);
    String[] morphemeLabels = {
      "kid/s", "gumboot/s",
      "save/ed", "mud/y", "bush/s", "bus/s", "put/ing", "girl/z", "want/3s", "go/3s", "shop/ing",
      "run/ing", "drop/ed", "help/ed", "aunty/z", "stop/ed", "leave/ing", "come/ing", "it/z",
      "lift/*ed",
      "Hug/ing", "Hunt/ing",
      "can/'t", "can/'t", // these errors corrected
      "carry/ed",
      "eat/ing", "gentle/y", "gentle/y", "girl/z",
      "go/3s", // error corrected
      "guy/s", "happen/ing", "help/ing",
      "hug/ed", "hurry/ed",
      "kid/z", // error corrected
      "learn/ing", "let/'us",
      "say/3s", // error corrected
      "see/ing", "tramp/ing",
      "try/ed", "tweet/ing", "wait/ing",
      "gumboot/s", "It/'s", "it/'s", "baby/s/z"
    };
    String[] wordLabels = {
      "kids,", "gumboots",
      "saved", "muddy", "bushes", "buses", "putting", "girl's", "wants", "goes", "shopping>",
      "running", "dropped", "helped", "aunty's", "stopped", "leaving", "coming", "its",
      "lift.",
      "Hugging", "Hunting", "can't", "can't", "carried",
      "eating", "gently", "gently", "girl's", "goes", "guys", "happening", "helping",
      "hugged", "hurried", "kid's", "learning", "let's", "says", "seeing", "tramping",
      "tried", "tweeting", "waiting?",
      "gumboots", "It's", "it's", "babies'"
    };
    for (int m = 0; m < boundMorphemes.length; m++) {
      assertEquals("label of bound morpheme " + m + ": " + boundMorphemes[m],
                   morphemeLabels[m], boundMorphemes[m].getLabel());
      assertEquals(
        "bound morpheme "+m+" tags a word: " + Arrays.asList(boundMorphemes[m].tagsOn("word")),
        1, boundMorphemes[m].tagsOn("word").length);
      assertEquals(
        "bound morpheme word label "+m+": " + Arrays.asList(boundMorphemes[m].tagsOn("word")),
        wordLabels[m], boundMorphemes[m].tagsOn("word")[0].getLabel());
    } // next bound morpheme

    // comments
    Annotation[] comments = g.all("comment");
    assertEquals("Correct number of comments: " + Arrays.asList(comments),
                 3, comments.length);
    assertEquals("first comment label",
                 "This is a plus line comment", comments[0].getLabel());
    assertEquals("first comment alignment",
                 Double.valueOf(29.0), comments[0].getStart().getOffset());
    assertEquals("second comment label",
                 "Bound morpheme tests", comments[1].getLabel());
    assertEquals("second comment alignment",
                 Double.valueOf(34.56), comments[1].getStart().getOffset());
    assertEquals("third comment label",
                 "in-situ comment", comments[2].getLabel());
    assertEquals("third comment preceding word: " + comments[2].getStart().endOf("word"),
                 "kids,", comments[2].getStart().endOf("word").iterator().next().getLabel());
    assertEquals("third comment following word: " + comments[2].getEnd().startOf("word"),
                 "they", comments[2].getEnd().startOf("word").iterator().next().getLabel());

    comments = g.all("noise");
    assertEquals("Correct number of noises: " + Arrays.asList(comments),
                 2, comments.length);
    assertEquals("noise label",
                 "click", comments[0].getLabel());
    assertEquals("noise start time: " + comments[0].getStart(),
                 Double.valueOf(46), comments[0].getStart().getOffset());
    assertEquals("noise label",
                 "bing", comments[1].getLabel());
    assertEquals("noise preceding word: " + comments[1].getStart().endOf("word"),
                 "to", comments[1].getStart().endOf("word").iterator().next().getLabel());
    assertEquals("noise following word: " + comments[1].getEnd().startOf("word"),
                 "put", comments[1].getEnd().startOf("word").iterator().next().getLabel());

    // parentheticals
    Annotation[] parentheticals = g.all("parenthetical");
    assertEquals("Correct number of parentheticals: " + Arrays.asList(parentheticals),
                 1, parentheticals.length);
    assertEquals("parenthetical label",
                 "((what... ...it))", parentheticals[0].getLabel());
    // assertEquals("parenthetical words: " + Arrays.asList(parentheticals[0].all("word")),
    //              "what date is it",
    //              Arrays.stream(parentheticals[0].all("word")) // TODO this should work!
    //              .map(word -> word.getLabel())
    //              .collect(Collectors.joining(" ")));
    assertEquals(
      "parenthetical first word: " + parentheticals[0].getStart().startOf("word"),
      "what", parentheticals[0].getStart().startOf("word").iterator().next().getLabel());
    assertEquals(
      "parenthetical last word: " + parentheticals[0].getEnd().endOf("word"),
      "it", parentheticals[0].getEnd().endOf("word").iterator().next().getLabel());
    assertEquals(
      "parenthetical parent: " + parentheticals[0].getParent(),
      parentheticals[0].getStart().startOf("word").iterator().next().getParent(),
      parentheticals[0].getParent());

    // mazes
    Annotation[] mazes = g.all("maze");
    assertEquals("Correct number of mazes: " + Arrays.asList(mazes),
                 3, mazes.length);
    assertEquals("first maze label",
                 "(Um... ...kids)", mazes[0].getLabel());
    assertEquals(
      "first maze first word: " + mazes[0].getStart().startOf("word"),
      "Um", mazes[0].getStart().startOf("word").iterator().next().getLabel());
    assertEquals(
      "first maze last word: " + mazes[0].getEnd().endOf("word"),
      "kids", mazes[0].getEnd().endOf("word").iterator().next().getLabel());
    assertEquals("second maze label",
                 "(them)", mazes[1].getLabel());
    assertEquals(
      "second maze first (and only) word: " + mazes[1].getStart().startOf("word"),
      "them", mazes[1].getStart().startOf("word").iterator().next().getLabel());
    assertEquals(
      "second maze last (and only) word: " + mazes[1].getEnd().endOf("word"),
      "them", mazes[1].getEnd().endOf("word").iterator().next().getLabel());
    assertEquals("third maze label",
                 "(um)", mazes[2].getLabel());
    assertEquals(
      "third maze first (and only) word: " + mazes[2].getStart().startOf("word"),
      "um.", mazes[2].getStart().startOf("word").iterator().next().getLabel());
    assertEquals(
      "thrid maze last (and only) word: " + mazes[2].getEnd().endOf("word"),
      "um.", mazes[2].getEnd().endOf("word").iterator().next().getLabel());

    // sound effects
    Annotation[] soundEffects = g.all("sound_effect");
    assertEquals("Correct number of sound effects: " + Arrays.asList(soundEffects),
                 1, soundEffects.length);
    assertEquals("sound effect label",
                 "yip_yip", soundEffects[0].getLabel());
    assertEquals(
      "sound effect starts at utterance start: "+soundEffects[0].getStart().startOf("utterance"),
      1, soundEffects[0].getStart().startOf("utterance").size());
    assertEquals(
      "sound effect folloring word: " + soundEffects[0].getEnd().startOf("word"),
      "Schnitzel", soundEffects[0].getEnd().startOf("word").iterator().next().getLabel());

    // proper names
    Annotation[] properNames = g.all("entity");
    assertEquals("Correct number of proper names: " + Arrays.asList(properNames),
                 1, properNames.length);
    assertEquals("proper names label",
                 "Schnitzel_von_Krumm", properNames[0].getLabel());
    assertEquals(
      "proper name first word: " + properNames[0].getStart().startOf("word"),
      "Schnitzel", properNames[0].getStart().startOf("word").iterator().next().getLabel());
    assertEquals(
      "proper name last word: " + properNames[0].getEnd().endOf("word"),
      "Krumm", properNames[0].getEnd().endOf("word").iterator().next().getLabel());

    // repetitions
    Annotation[] repetitions = g.all("repetition");
    assertEquals("Correct number of repetitions: " + Arrays.asList(repetitions),
                 1, repetitions.length);
    assertEquals("repetitions label",
                 "heaps", repetitions[0].getLabel());
    // assertEquals("repetition words: " + Arrays.asList(repetitions[0].all("word")),
    //              "heaps and heaps",
    //              Arrays.stream(repetitions[0].all("word")) // TODO this should work!
    //              .map(word -> word.getLabel())
    //              .collect(Collectors.joining(" ")));
    assertEquals(
      "repetition first word: " + repetitions[0].getStart().startOf("word"),
      "heaps", repetitions[0].getStart().startOf("word").iterator().next().getLabel());
    assertEquals(
      "repetition last word: " + repetitions[0].getEnd().endOf("word"),
      "heaps", repetitions[0].getEnd().endOf("word").iterator().next().getLabel());

    // pauses
    Annotation[] pauses = g.all("pause");
    assertEquals("Correct number of pauses: " + Arrays.asList(pauses),
                 3, pauses.length);
    assertEquals("first pause label", "01", pauses[0].getLabel());
    assertEquals("first pause time", Double.valueOf(41), pauses[0].getEnd().getOffset());
    assertEquals("second pause label", "02", pauses[1].getLabel());
    assertEquals("second pause previous word",
                 "s~", pauses[1].getStart().endOf("word").iterator().next().getLabel());
    assertEquals("second pause next word",
                 "falled", pauses[1].getEnd().startOf("word").iterator().next().getLabel());
    assertEquals("third pause label", "03", pauses[2].getLabel());
    assertEquals("third pause time", Double.valueOf(69), pauses[2].getStart().getOffset());

    // omissions
    Annotation[] omissions = g.all("omission");
    assertEquals("Correct number of omissions: " + Arrays.asList(omissions),
                 2, omissions.length);
    assertEquals("omission label", "of", omissions[0].getLabel());
    assertEquals("omission previous word",
                 "out", omissions[0].getStart().endOf("word").iterator().next().getLabel());
    assertEquals("omission next word",
                 "the", omissions[0].getEnd().startOf("word").iterator().next().getLabel());
    assertEquals("bound morpheme omission label", "they/'re", omissions[1].getLabel());

    // partial words
    Annotation[] partialWords = g.all("partial_word");
    assertEquals("Correct number of partial words: " + Arrays.asList(partialWords),
                 1, partialWords.length);
    assertEquals("partial word label", "s", partialWords[0].getLabel());
    assertEquals("partial word token",
                 "s~", partialWords[0].tagsOn("word")[0].getLabel());

    // C-Units
    Annotation[] cUnits = g.all("cunit");
    assertEquals(24, cUnits.length);
    String[] cUnitLabels = {
      "?", ".", ".", ".", ".", ".", "?", ".", ">", ".", "?", "?", "?", ".", "^", "?", ".", "?",
      ".", "?", ".", ".", "?", "."
    };
    for (int c = 0; c < cUnits.length; c++) {
      assertEquals("C-Unit "+c+" label",
                   cUnitLabels[c], cUnits[c].getLabel());
    }
  }

  /** Parse annotations, but don't map them to layers */
  @Test public void deserializeNoAnnotations()  throws Exception {
    // just a basic schema, nothing SALT-specific
    Schema schema = new Schema(
      "participant", "turn", "utterance", "word",      
      new Layer("participant", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("main_participant", "Target Speaker").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("participant").setParentIncludes(true),      
      new Layer("turn", "Speaker Turn").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("participant").setParentIncludes(true),
      new Layer("utterance", "Lines").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true)
      );
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "test.slt")) };
    
    // create deserializer
    SltSerialization deserializer = new SltSerialization();
    
    // general configuration
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) {
    //   System.out.println("" + p.getName() + " = " + p.getValue());
    // }
    assertEquals("Correct number of configuration parameters", 28, configuration.size());
    assertEquals(schema.getLayer("main_participant"),
                 configuration.get("targetParticipantLayer").getValue());
    assertNull(configuration.get("cUnitLayer").getValue());
    assertNull(configuration.get("commentLayer").getValue());
    assertNull(configuration.get("parentheticalLayer").getValue());
    assertNull(configuration.get("properNameLayer").getValue());
    assertNull(configuration.get("repetitionsLayer").getValue());
    assertNull(configuration.get("rootLayer").getValue());
    assertNull(configuration.get("errorLayer").getValue());
    assertNull(configuration.get("soundEffectLayer").getValue());
    assertNull(configuration.get("pauseLayer").getValue());
    assertNull(configuration.get("boundMorphemeLayer").getValue());
    assertNull(configuration.get("mazeLayer").getValue());
    assertNull(configuration.get("partialWordLayer").getValue());
    assertNull(configuration.get("omissionLayer").getValue());
    assertNull(configuration.get("codeLayer").getValue());
    assertNull(configuration.get("languageLayer").getValue());
    assertNull(configuration.get("participantIdLayer").getValue());
    assertNull(configuration.get("genderLayer").getValue());
    assertNull(configuration.get("dobLayer").getValue());
    assertNull(configuration.get("doeLayer").getValue());
    assertNull(configuration.get("caLayer").getValue());
    assertNull(configuration.get("ethnicityLayer").getValue());
    assertNull(configuration.get("contextLayer").getValue());
    assertNull(configuration.get("subgroupLayer").getValue());
    assertNull(configuration.get("collectLayer").getValue());
    assertNull(configuration.get("locationLayer").getValue());
    assertEquals("parseInlineConventions is true default",
                 Boolean.TRUE, configuration.get("parseInlineConventions").getValue());
    // change to day-first
    configuration.get("dateFormat").setValue("d/M/yyyy");

    // final configuration
    deserializer.configure(configuration, schema);
    
    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParameters.values()) {
    //   System.out.println("" + p.getName() + " = " + p.getValue());
    // }
    assertEquals("One parameter for each type of code, + noise comments: "
                 + defaultParameters.keySet(),
                 7, defaultParameters.size());
    assertNull("REDACTED codes not mapped",
               defaultParameters.get("code_REDACTED").getValue());
    assertNull("CENSORED code not mapped",
               defaultParameters.get("code_CENSOR").getValue());
    assertNull("EP codes not mapped",
               defaultParameters.get("code_EP").getValue());
    assertNull("LEXICAL codes not mapped",
               defaultParameters.get("code_LEXICAL").getValue());
    assertNull("EU codes not mapped",
               defaultParameters.get("code_EU").getValue());
    assertNull("EW mapped not mapped",
               defaultParameters.get("code_EW").getValue());
    assertNull("NOISE comments not mapped",
               defaultParameters.get("comment_NOISE").getValue());

    // configure the deserialization
    deserializer.setParameters(defaultParameters);
    
    // build the graph
    Graph[] graphs = deserializer.deserialize();
    assertEquals("conversion is 1-1", 1, graphs.length);
    Graph g = graphs[0];
    
    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }
    
    assertEquals("test.slt", g.getId());

    // participants     
    assertEquals("Two participants", 2, g.all("participant").length);
    assertEquals("One target", 1, g.all("main_participant").length);
    Annotation child = g.first("main_participant").getParent();
    assertEquals("Target participant prefixed with value of Name header",
                 "ADAL-Child", child.getLabel());
    Annotation examiner = null;
    for (Annotation p : g.all("participant")) {
      if (!p.getId().equals(child.getId())) {
        examiner = p;
        break;
      }
    }
    assertNotNull("Examiner found", examiner);
    assertEquals("Examiner name prefixed with file name value of Name header",
                 "ADAL-Examiner", examiner.getLabel());
    
     // turns
    Annotation[] turns = g.all("turn");
    assertEquals(12, turns.length);

    // check turn timestamps
    double[] turnTimeStamps = {
      0, 29, 34.56, 39, 47, 52, 62, 63, 69, 71, 86, 93 };
    Annotation lastTurn = null;
    assertEquals("Examiner is first", "ADAL-Examiner", turns[0].getLabel());
    for (int t = 0; t < turns.length; t++) {
      assertEquals("start of turn " + t,
                   Double.valueOf(turnTimeStamps[t]), turns[t].getStart().getOffset());
      assertEquals("confidence of start of turn " + t + " " + turns[t].getStart(),
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL),
                   turns[t].getStart().getConfidence());
      if (lastTurn != null) {
        assertEquals("End of turn " + (t-1),
                     turns[t].getStart(), lastTurn.getEnd());
        assertNotEquals("Speaker has changed",
                        lastTurn.getLabel(), turns[t].getLabel());
      }
      lastTurn = turns[t];
    } // next turn
    // transcript has no end timestamp, so it should be 1s after the last known timestamp
    assertEquals("Dummy last timestamp (" + lastTurn.getStart() + "-" + lastTurn.getEnd() + ")",
                 Double.valueOf(94.0), lastTurn.getEnd().getOffset());
    assertEquals("Dummy last timestamp has low confidence",
                 Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC),
                 lastTurn.getEnd().getConfidence());
      
    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals(21, utterances.length);
    
    // check utterance timestamps and speakers
    double[] utteranceTimeStamps = {
      0, 5, 12, 16, 20, 24, 29, 34.56, 37, 39, 41, 46, 47, 52, 62, 63, 69, 71, 83, 86, 93 };
    String[] utteranceSpeakers = {
      "ADAL-Examiner", "ADAL-Examiner", "ADAL-Examiner", "ADAL-Examiner", "ADAL-Examiner", "ADAL-Examiner",
      "ADAL-Child",
      "ADAL-Examiner", "ADAL-Examiner",
      "ADAL-Child", "ADAL-Child", "ADAL-Child",
      "ADAL-Examiner",
      "ADAL-Child",
      "ADAL-Examiner",
      "ADAL-Child",
      "ADAL-Examiner",
      "ADAL-Child", "ADAL-Child",
      "ADAL-Examiner",
      "ADAL-Child"
    };
    Annotation lastUtterance = null;
    for (int u = 0; u < utterances.length; u++) {
      assertEquals("start of utterance " + u,
                   Double.valueOf(utteranceTimeStamps[u]), utterances[u].getStart().getOffset());
      if (u != 3 && u != 4) {
        assertEquals("confidence of start of utterance " + u,
                     Integer.valueOf(Constants.CONFIDENCE_MANUAL),
                     utterances[u].getStart().getConfidence());
      } else { // u == 3 or 4 are special cases:
        // it has the same start time as the previous utterance,
        // so the offset has been bumped forward and given lower confidence
        assertEquals("confidence of bumped start of utterance " + u,
                     Integer.valueOf(Constants.CONFIDENCE_DEFAULT),
                     utterances[u].getStart().getConfidence());
      }
      assertEquals("speaker of utterance " + u + " ("+utterances[u].getStart()+")",
                   utteranceSpeakers[u], utterances[u].getParent().getLabel());
      if (lastUtterance != null) {
        assertEquals("End of utterance " + (u-1) + " ("+utterances[u-1].getStart()+")",
                     utterances[u].getStart(), lastUtterance.getEnd());
      }
      lastUtterance = utterances[u];
    } // next utterance
    // transcript has no end timestamp, so it should be 1s after the last known timestamp
    assertEquals("Dummy last timestamp",
                 Double.valueOf(94.0), lastUtterance.getEnd().getOffset());
    assertEquals("Dummy last timestamp has low confidence",
                 Integer.valueOf(Constants.CONFIDENCE_AUTOMATIC),
                 lastUtterance.getEnd().getConfidence());

    // words
    Annotation[] words = g.all("word");
    assertEquals(177, words.length);

    // check utterance transcriptions
    String[] lines = {
      "I'm at Byron kindergarten on what date is it the 30th of March 2021?",
      "My name is _. and I'm here with _, doing the oral language assessment.",
      "Okay, so now Ada it's your turn to tell the story.",
      "You can look at the pictures when you're telling the story.",
      "So let's start at the beginning.",
      "What was the story about?",
      "Um the kids the kids, they quickly put their gumboots on.",
      "saved muddy bushes buses putting girl's wants goes shopping> running dropped helped aunty's stopped leaving coming its lift. Hugging Hunting can't can't carried eating gently gently girl's goes guys happening helping hugged hurried kid's learning let's says seeing tramping tried tweeting waiting?",
      "Anything else?",
      "And please go for a walk?",
      "You need to put your gumboots on.",
      "It's too dark^",
      "What happened in this one?",
      "And then it's heaps and heaps dark.",
      "What happened next?",
      "Schnitzel von Krumm s~ falled out the babies' nest.",
      "What happened next?",
      "They put them it back in the nest um.",
      "Bye bye little bird.",
      "Anything else that happened?",
      "_."
    };

    // setting default anchor offsets ensures that the utterance words are found
    new DefaultOffsetGenerator().transform(g).commit();
    
    for (int u = 0; u < utterances.length; u++) {
      String transcription = Arrays.stream(utterances[u].all("word"))
        .map(word -> word.getLabel())
        .collect(Collectors.joining(" "));
      assertEquals("Transcription of utterance " + u + " ("+utterances[u].getStart()+")",
                   lines[u], transcription);
    }        
      
    // check all annotations have 'manual' confidence
    for (Annotation a : g.getAnnotationsById().values()) {
      assertEquals("Annotation has 'manual' confidence: " + a.getLayer() + ": " + a,
                   Integer.valueOf(Constants.CONFIDENCE_MANUAL), a.getConfidence());
    }
  }

  /** Ensure unaligned utterance boundaries generate a warning. */
  @Test public void deserializeUnalignedUtterance()  throws Exception {
    // just a basic schema, nothing SALT-specific
    Schema schema = new Schema(
      "participant", "turn", "utterance", "word",      
      new Layer("participant", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("main_participant", "Target Speaker").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("participant").setParentIncludes(true),      
      new Layer("turn", "Speaker Turn").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("participant").setParentIncludes(true),
      new Layer("utterance", "Lines").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true)
      );
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "unaligned-utterance.slt")) };
    
    // create deserializer
    SltSerialization deserializer = new SltSerialization();
    
    // general configuration
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) {
    //   System.out.println("" + p.getName() + " = " + p.getValue());
    // }
    // set configuration
    deserializer.configure(configuration, schema);
    
    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParameters.values()) {
    //   System.out.println("" + p.getName() + " = " + p.getValue());
    // }
    assertEquals("No stream-specific parameters", 0, defaultParameters.size());

    // configure the deserialization
    deserializer.setParameters(defaultParameters);
    
    // build the graph
    Graph[] graphs = deserializer.deserialize();
    assertEquals("conversion is 1-1", 1, graphs.length);
    Graph g = graphs[0];

    String[] warnings = deserializer.getWarnings();
    assertEquals("There are three warnings: " + Arrays.asList(warnings),
                 3, warnings.length);
    assertTrue("First warning about lack of utterance start time",
               warnings[0].startsWith("Utterance with unknown start time:"));
    assertTrue("Second warning about lack of utterance start time after simultaneous speech",
               warnings[1].startsWith("Utterance with unknown start time:"));
    assertEquals("Last warning about lack of overall end time",
                 "End time of transcript is unknown.", warnings[2]);
    // for (String warning : deserializer.getWarnings()) {
    //   System.out.println(warning);
    // }
    
    assertEquals("unaligned-utterance.slt", g.getId());
    
    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals("Correct number of utterances " + Arrays.asList(utterances),
                 10, utterances.length);
    
    // check utterance timestamps and speakers
    Double[] utteranceTimeStamps = {0.0, 5.0, 10.0, 12.0, null, 15.0, 24.0, 24.0, null, 27.0 };
    for (int u = 0; u < utterances.length; u++) {
      assertEquals("start of utterance " + u,
                   utteranceTimeStamps[u], utterances[u].getStart().getOffset());
    } // next utterance
  }

  /** Ensure simultaneous utterances are handled correctly. */
  @Test public void deserializeSimultaneousSpeech()  throws Exception {
    // just a basic schema, nothing SALT-specific
    Schema schema = new Schema(
      "participant", "turn", "utterance", "word",      
      new Layer("participant", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("main_participant", "Target Speaker").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("participant").setParentIncludes(true),      
      new Layer("turn", "Speaker Turn").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("participant").setParentIncludes(true),
      new Layer("utterance", "Lines").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true)
      );
    // access file
    NamedStream[] streams = { new NamedStream(new File(getDir(), "simultaneous-speech.slt")) };
    
    // create deserializer
    SltSerialization deserializer = new SltSerialization();
    
    // general configuration
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // for (Parameter p : configuration.values()) {
    //   System.out.println("" + p.getName() + " = " + p.getValue());
    // }
    // set configuration
    deserializer.configure(configuration, schema);
    
    // load the stream
    ParameterSet defaultParameters = deserializer.load(streams, schema);
    // for (Parameter p : defaultParameters.values()) {
    //   System.out.println("" + p.getName() + " = " + p.getValue());
    // }
    assertEquals("No stream-specific parameters", 0, defaultParameters.size());

    // configure the deserialization
    deserializer.setParameters(defaultParameters);
    
    // build the graph
    Graph[] graphs = deserializer.deserialize();
    assertEquals("conversion is 1-1", 1, graphs.length);
    Graph g = graphs[0];

    String[] warnings = deserializer.getWarnings();
    assertEquals("There are no warnings: " + Arrays.asList(warnings),
                 0, warnings.length);    
    assertEquals("simultaneous-speech.slt", g.getId());
    
    // utterances
    Annotation[] utterances = g.all("utterance");
    assertEquals("Correct number of utterances " + Arrays.asList(utterances),
                 7, utterances.length);
    
    // check utterance timestamps and speakers
    Double[] utteranceStarts = {0.0, 5.0, 10.0, 12.0, 12.0, 15.0, 24.0 };
    Double[] utteranceEnds = {5.0, 10.0, 12.0, 15.0, 15.0, 24.0, 29.0 };
    for (int u = 0; u < utterances.length; u++) {
      assertEquals("start of utterance " + u,
                   utteranceStarts[u], utterances[u].getStart().getOffset());
      assertEquals("end of utterance " + u,
                   utteranceEnds[u], utterances[u].getEnd().getOffset());
    } // next utterance
  }

  @Test public void serialize()  throws Exception {
    Schema schema = new Schema(
      "participant", "turn", "utterance", "word",
      new Layer("transcript_language", "Language").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("transcript_doe", "Recording date").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("transcript_ca", "Current Age").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("transcript_context", "Context").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("transcript_subgroup", "Subgroup/Story").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("transcript_collect", "Collection Point").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      new Layer("transcript_location", "Location").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true),
      
      new Layer("comment", "Comments").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(true).setSaturated(false),
      
      new Layer("participant", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("main_participant", "Target Speaker").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("participant").setParentIncludes(true),
      new Layer("participant_id", "Participant ID").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("participant").setParentIncludes(true),
      new Layer("participant_gender", "Gender").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("participant").setParentIncludes(true),
      new Layer("participant_dob", "Birth Date").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("participant").setParentIncludes(true),
      new Layer("participant_ethnicity", "Ethnicity").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("participant").setParentIncludes(true),
      
      new Layer("turn", "Speaker Turn").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("participant").setParentIncludes(true),
      new Layer("utterance", "Lines").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),

      new Layer("pause", "Pauses").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("cunit", "C-Unit").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("parenthetical", "Parentheticals").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("repetition", "Repetitions").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("error", "Errors").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("code", "Non-error Codes").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("maze", "Mazes").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("sound_effect", "Verbal sound effects etc.")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("entity", "Proper Names").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("omission", "Omissions").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),

      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      
      new Layer("root", "Root form").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true),
      new Layer("bound_morpheme", "Bound Morphemes").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true),
      new Layer("partial_word", "Partial Words").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true)
      );
    // load test file
    NamedStream[] inStreams = { new NamedStream(new File(getDir(), "test.slt")) };
    
    // create deserializer
    SltSerialization deserializer = new SltSerialization();
    
    // general configuration
    ParameterSet configuration = deserializer.configure(new ParameterSet(), schema);
    // change to date format day-first
    configuration.get("dateFormat").setValue("d/M/yyyy");
    deserializer.configure(configuration, schema);
    
    // load/configure the stream
    deserializer.setParameters(
      deserializer.load(inStreams, schema));
    
    // build the graph
    Graph[] graphs = deserializer.deserialize();    
    for (String warning : deserializer.getWarnings()) {
      System.out.println(warning);
    }

    // assume that deserialization worked (that's tested elsewhere)

    // but double-check codes
    Annotation[] codes = graphs[0].all("code");
    assertEquals("Correct number of codes: " + Arrays.asList(codes),
                 4, codes.length);
    Annotation[] errors = graphs[0].all("error");
    assertEquals("Correct number of error codes: " + Arrays.asList(errors),
                 5, errors.length);
    // System.out.println("errors: "+Arrays.stream(errors).map(e->e.getLabel()+"("+e.getStart()+"-"+e.getEnd()+")").collect(Collectors.joining(" ")));
    
    // add an underscore to a word, to test it's preserved in the output
    graphs[0].first("word").setLabel("I_m");
    
    // change gender of speaker to ensure it's standardized
    graphs[0].first("participant_gender").setLabel("male");
    
    // create new serializer
    SltSerialization serializer = new SltSerialization();

    // general configuration - same as above
    configuration = serializer.configure(new ParameterSet(), schema);
    assertEquals("Correct number of configuration parameters", 28, configuration.size());
    assertEquals(schema.getLayer("cunit"),
                 configuration.get("cUnitLayer").getValue());
    assertEquals(schema.getLayer("main_participant"),
                 configuration.get("targetParticipantLayer").getValue());
    assertEquals(schema.getLayer("comment"),
                 configuration.get("commentLayer").getValue());
    assertEquals(schema.getLayer("parenthetical"),
                 configuration.get("parentheticalLayer").getValue());
    assertEquals(schema.getLayer("entity"),
                 configuration.get("properNameLayer").getValue());
    assertEquals(schema.getLayer("repetition"),
                 configuration.get("repetitionsLayer").getValue());
    assertEquals(schema.getLayer("root"),
                 configuration.get("rootLayer").getValue());
    assertEquals(schema.getLayer("error"),
                 configuration.get("errorLayer").getValue());
    assertEquals(schema.getLayer("sound_effect"),
                 configuration.get("soundEffectLayer").getValue());
    assertEquals(schema.getLayer("pause"),
                 configuration.get("pauseLayer").getValue());
    assertEquals(schema.getLayer("bound_morpheme"),
                 configuration.get("boundMorphemeLayer").getValue());
    assertEquals(schema.getLayer("maze"),
                 configuration.get("mazeLayer").getValue());
    assertEquals(schema.getLayer("partial_word"),
                 configuration.get("partialWordLayer").getValue());
    assertEquals(schema.getLayer("omission"),
                 configuration.get("omissionLayer").getValue());
    assertEquals(schema.getLayer("code"),
                 configuration.get("codeLayer").getValue());
    assertEquals(schema.getLayer("transcript_language"),
                 configuration.get("languageLayer").getValue());
    assertEquals(schema.getLayer("participant_id"),
                 configuration.get("participantIdLayer").getValue());
    assertEquals(schema.getLayer("participant_gender"),
                 configuration.get("genderLayer").getValue());
    assertEquals(schema.getLayer("participant_dob"),
                 configuration.get("dobLayer").getValue());
    assertEquals(schema.getLayer("transcript_doe"),
                 configuration.get("doeLayer").getValue());
    assertEquals(schema.getLayer("transcript_ca"),
                 configuration.get("caLayer").getValue());
    assertEquals(schema.getLayer("participant_ethnicity"),
                 configuration.get("ethnicityLayer").getValue());
    assertEquals(schema.getLayer("transcript_context"),
                 configuration.get("contextLayer").getValue());
    assertEquals(schema.getLayer("transcript_subgroup"),
                 configuration.get("subgroupLayer").getValue());
    assertEquals(schema.getLayer("transcript_collect"),
                 configuration.get("collectLayer").getValue());
    assertEquals(schema.getLayer("transcript_location"),
                 configuration.get("locationLayer").getValue());
    assertEquals("parseInlineConventions is true default",
                 Boolean.TRUE, configuration.get("parseInlineConventions").getValue());
    // change to date format day-first
    configuration.get("dateFormat").setValue("d/M/yyyy");
    serializer.configure(configuration, schema);

    // TODO: LinkedHashSet<String> needLayers = new LinkedHashSet<String>(
    //   Arrays.asList(serializer.getRequiredLayers()));
    // assertEquals("Needed layers: " + needLayers,
    //              11, needLayers.size());
    // assertTrue(needLayers.contains("who"));
    // assertTrue(needLayers.contains("main_participant"));
    // assertTrue(needLayers.contains("scribe"));
    // assertTrue(needLayers.contains("transcript_language"));
    // assertTrue(needLayers.contains("turn"));
    // assertTrue(needLayers.contains("utterance"));
    // assertTrue(needLayers.contains("word"));
    // assertTrue(needLayers.contains("noise"));
    // assertTrue(needLayers.contains("participant_age"));
    // assertTrue(needLayers.contains("participant_gender"));
    // assertTrue(needLayers.contains("participant_language"));
    
    // now we serialize again...

    final Vector<SerializationException> exceptions = new Vector<SerializationException>();
    final Vector<NamedStream> outStreams = new Vector<NamedStream>();
    serializer.serialize(Arrays.spliterator(graphs), null,
                         stream -> outStreams.add(stream),
                         warning -> System.out.println(warning),
                         exception -> exceptions.add(exception));
    if (exceptions.size() > 0) {
      fail(exceptions.stream()
           .map(x -> {
               StringWriter sw = new StringWriter();
               PrintWriter pw = new PrintWriter(sw);
               x.printStackTrace(pw);
               return x.toString() + ": " + sw;
             })
           .collect(Collectors.joining("\n","","")));
    }

    // save stream with a different name
    outStreams.elementAt(0).setName("serialize-test.slt");    
    File dir = getDir();
    outStreams.elementAt(0).save(dir);
    
    // test using diff
    File result = new File(dir, "serialize-test.slt");
    String differences = diff(new File(dir, "expected_serialize-test.slt"), result);
    if (differences != null) {
      fail(differences);
    } else {
      result.delete();
    }
  }

  /** Test for graph that has inter-utterance gaps and overlapping speech */
  @Test public void serializeNonSaltStyleTranscript() throws Exception {
    Schema schema = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants")
      .setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("utterance", "Utterances")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true),
      new Layer("word", "Words")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true));
    
    Graph graph = new Graph()
      .setId("complex.txt")
      .setSchema(schema);
    graph.addAnchor(new Anchor("a0", 0.0, Constants.CONFIDENCE_MANUAL));
    graph.addAnchor(new Anchor("a5", 5.4321, Constants.CONFIDENCE_MANUAL)); // decimal
    graph.addAnchor(new Anchor("a10", 10.0, Constants.CONFIDENCE_MANUAL));
    graph.addAnchor(new Anchor("a15", 15.0, Constants.CONFIDENCE_MANUAL));
    graph.addAnchor(new Anchor("a20", 20.0, Constants.CONFIDENCE_MANUAL));
    graph.addAnchor(new Anchor("a25", 25.0, Constants.CONFIDENCE_MANUAL));
    graph.addAnchor(new Anchor("a30", 30.0, Constants.CONFIDENCE_MANUAL));
    graph.addAnchor(new Anchor("a35", 35.0, Constants.CONFIDENCE_MANUAL));

    // participants
    graph.addAnnotation(new Annotation("child", "John Smith", "who", "a0", "a35"));
    graph.addAnnotation(new Annotation("mother", "Mrs. Smith", "who", "a0", "a35"));
    // turns - all overlapping
    graph.addAnnotation(new Annotation("t1", "John Smith", "turn", "a0", "a10", "child"));
    graph.addAnnotation(new Annotation("t2", "Mrs. Smith", "turn", "a5", "a15", "mother"));
    // gap with no speech
    graph.addAnnotation(new Annotation("t3", "Mrs. Smith", "turn", "a20", "a30", "mother"));
    graph.addAnnotation(new Annotation("t4", "John SMith", "turn", "a25", "a35", "child"));
    
    // utterances
    graph.addAnnotation(new Annotation("u1", "John Smith", "utterance", "a0", "a5", "t1"));
    // u2 and u3 overlap:
    graph.addAnnotation(new Annotation("u2", "John Smith", "utterance", "a5", "a10", "t1"));
    graph.addAnnotation(new Annotation("u3", "Mrs. Smith", "utterance", "a5", "a15", "t2"));
    // gap 15.0-25.0
    // u4 and u5 overlap:
    graph.addAnnotation(new Annotation("u4", "Mrs. Smith", "utterance", "a20", "a30", "t3"));
    graph.addAnnotation(new Annotation("u5", "John SMith", "utterance", "a25", "a35", "t4"));

    // words

    // u1
    graph.addAnnotation(new Annotation("the", "The", "word",
                                       "a0", graph.addAnchor(new Anchor("a1", 1.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("quick", "'quick", "word", 
                                       "a1", graph.addAnchor(new Anchor("a2", 2.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("brown", "brown'", "word", 
                                       "a2", graph.addAnchor(new Anchor("a3", 3.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("fox", "fox", "word", 
                                       "a3", "a5",
                                       "t1"));

    // u2 ...
    graph.addAnnotation(new Annotation("jumps", "jumps -", "word", 
                                         "a5", graph.addAnchor(new Anchor("a6", 6.0)).getId(),
                                         "t1"));      
    graph.addAnnotation(new Annotation("over", "over", "word",
                                       "a6", graph.addAnchor(new Anchor("a7", 8.0)).getId(),
                                       "t1"));
    graph.addAnnotation(new Annotation("th~", "th~", "word",
                                       "a8", "a10",
                                       "t1"));
    // ... overlaps with u3
    graph.addAnnotation(new Annotation("the2", "the", "word", 
                                       "a5", graph.addAnchor(new Anchor("a8", 8.0)).getId(),
                                       "t2"));
    graph.addAnnotation(new Annotation("lazy", "lazy", "word", 
                                       "a8", graph.addAnchor(new Anchor("a12", 12.0)).getId(),
                                       "t2"));
    graph.addAnnotation(new Annotation("dog", "\"dog\"", "word", 
                                       "a12", graph.addAnchor(new Anchor("a14", 14.0)).getId(),
                                       "t2"));
    graph.addAnnotation(new Annotation(".", ".", "word", 
                                       "a14", "a15",
                                       "t2"));

    // gap

    // u4 ...
    graph.addAnnotation(new Annotation("one", "one", "word", 
                                       "a20", graph.addAnchor(new Anchor("a22", 22.0)).getId(),
                                       "t3"));
    graph.addAnnotation(new Annotation("two", "two", "word", 
                                       "a22", graph.addAnchor(new Anchor("a24", 24.0)).getId(),
                                       "t3"));
    graph.addAnnotation(new Annotation("three", "three", "word", 
                                       "a24", graph.addAnchor(new Anchor("a26", 26.0)).getId(),
                                       "t3"));
    graph.addAnnotation(new Annotation("four", "four", "word", 
                                       "a26", graph.addAnchor(new Anchor("a28", 28.0)).getId(),
                                       "t3"));
    graph.addAnnotation(new Annotation("five", "five", "word", 
                                       "a28", "a30",
                                       "t3"));

    // ... overlaps with u5
    graph.addAnnotation(new Annotation("six", "six", "word", 
                                       "a25", graph.addAnchor(new Anchor("a27", 27.0)).getId(),
                                       "t4"));
    graph.addAnnotation(new Annotation("seven", "seven", "word", 
                                       "a27", graph.addAnchor(new Anchor("a29", 29.0)).getId(),
                                       "t4"));
    graph.addAnnotation(new Annotation("eight", "eight", "word", 
                                       "a29", graph.addAnchor(new Anchor("a31", 31.0)).getId(),
                                       "t4"));
    graph.addAnnotation(new Annotation("nine", "nine", "word", 
                                       "a31", graph.addAnchor(new Anchor("a33", 33.0)).getId(),
                                       "t4"));
    graph.addAnnotation(new Annotation("ten", "ten", "word", 
                                       "a33", "a35",
                                       "t4"));    
    
    // create serializer
    SltSerialization serializer = new SltSerialization();
      
    // general configuration
    serializer.configure(
      serializer.configure(new ParameterSet(), schema), schema);
    
    LinkedHashSet<String> needLayers = new LinkedHashSet<String>(
      Arrays.asList(serializer.getRequiredLayers()));
    assertEquals("Needed layers: " + needLayers,
                 4, needLayers.size());
    assertTrue(needLayers.contains("who"));
    assertTrue(needLayers.contains("turn"));
    assertTrue(needLayers.contains("utterance"));
    assertTrue(needLayers.contains("word"));
    
    // serialize
    final Vector<SerializationException> exceptions = new Vector<SerializationException>();
    final Vector<NamedStream> outStreams = new Vector<NamedStream>();
    serializer.serialize(Utility.OneGraphSpliterator(graph), null,
                         stream -> outStreams.add(stream),
                         warning -> System.out.println(warning),
                         exception -> exceptions.add(exception));
    if (exceptions.size() > 0) {
      fail(exceptions.stream()
           .map(x -> {
               StringWriter sw = new StringWriter();
               PrintWriter pw = new PrintWriter(sw);
               x.printStackTrace(pw);
               return x.toString() + ": " + sw;
             })
           .collect(Collectors.joining("\n","","")));
    }

    // save stream with a different name
    outStreams.elementAt(0).setName("complex.slt");    
    File dir = getDir();
    outStreams.elementAt(0).save(dir);
    
    // test using diff
    File result = new File(dir, "complex.slt");
    String differences = diff(new File(dir, "expected_complex.slt"), result);
    if (differences != null) {
      fail(differences);
    } else {
      result.delete();
    }
  }
  
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
  public File getDir() { 
    if (fDir == null) {
      try {
        URL urlThisClass = getClass().getResource(getClass().getSimpleName() + ".class");
        File fThisClass = new File(urlThisClass.toURI());
        fDir = fThisClass.getParentFile();
      } catch(Throwable t) {
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
  
  public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nzilbb.formatter.salt.TestSltSerialization");
  }
}
