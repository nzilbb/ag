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

package nzilbb.ag.sql.test;

import org.junit.*;
import static org.junit.Assert.*;

import nzilbb.ag.Constants;
import nzilbb.ag.Schema;
import nzilbb.ag.Layer;
import nzilbb.ag.ql.AGQLException;
import nzilbb.ag.sql.GraphAgqlToSql;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class TestGraphAgqlToSql
{
  
  /**
   * Return a plausible schema, including SQL attributes.
   * @return A test schema.
   */
  public Schema getSchema()
  {
    return new Schema(
      "who", "turn", "utterance", "transcript",
            
      (Layer)(new Layer("transcript_language", "Language").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true))
      .with("@class_id", "transcript").with("@attribute", "language"),
      
      (Layer)(new Layer("transcript_scribe", "Scribe").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true))
      .with("@class_id", "transcript").with("@attribute", "scribe"),
      
      (Layer)(new Layer("transcript_rating", "Ratings").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true))
      .with("@class_id", "transcript").with("@attribute", "rating"),
      
      (Layer)(new Layer("corpus", "Corpus").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true))
      .with("@layer_id", -100),
      
      (Layer)(new Layer("episode", "Episode").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true))
      .with("@layer_id", -50),
      
      (Layer)(new Layer("recording_date", "Recording Date").setAlignment(Constants.ALIGNMENT_NONE)
              .setPeers(false).setPeersOverlap(false).setSaturated(true).setParentId("episode"))
      .with("@layer_id", -200),
      
      (Layer)(new Layer("who", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true))
      .with("@layer_id", -2),
      
      (Layer)(new Layer("main_participant", "Main Participant").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true).setParentId("who"))
      .with("@layer_id", -3),
      
      (Layer)(new Layer("participant_gender", "Gender").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true).setParentId("who"))
      .with("@class_id", "speaker").with("@attribute", "gender"),
      
      (Layer)(new Layer("participant_age", "Age").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true).setParentId("who"))
      .with("@class_id", "speaker").with("@attribute", "age"),
      
      (Layer)(new Layer("comment", "Comment").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false))
      .with("@layer_id", 31),
      
      (Layer)(new Layer("noise", "Noise")
      .setAlignment(2).setPeers(true).setPeersOverlap(false).setSaturated(false))
      .with("@layer_id", 32),
      
      (Layer)(new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true))
      .with("@layer_id", 11),
      
      (Layer)(new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true))
      .with("@layer_id", 12),
      
      (Layer)(new Layer("transcript", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true))
      .with("@layer_id", 0),
      
      (Layer)(new Layer("orthography", "Orthography").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(false)
      .setParentId("word").setParentIncludes(true))
      .with("@layer_id", 2),
      
      (Layer)(new Layer("segments", "Phones").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true))
      .with("@layer_id", 1),
      
      (Layer)(new Layer("pronounce", "Pronounce").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId("word").setParentIncludes(true))
      .with("@layer_id", 23))

      .setEpisodeLayerId("episode")
      .setCorpusLayerId("corpus");
  } // end of getSchema()

  @Test public void idMatch() throws AGQLException
  {
    GraphAgqlToSql transformer = new GraphAgqlToSql(getSchema());
    GraphAgqlToSql.Query q = transformer.sqlFor(
      "label MATCHES \"Ada.+\"",
      "transcript.transcript_id, transcript.ag_id", null, null, null);
    assertEquals("SQL - label",
                 "SELECT transcript.transcript_id, transcript.ag_id FROM transcript"
                 +" WHERE transcript.transcript_id REGEXP 'Ada.+'"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count - label", 0, q.parameters.size());

    q = transformer.sqlFor(
      "id NOT MATCHES \"Ada.+\"",
      "transcript.transcript_id, transcript.ag_id", null, "id ASC", "LIMIT 1,1");
    assertEquals("SQL - id",
                 "SELECT transcript.transcript_id, transcript.ag_id FROM transcript"
                 +" WHERE transcript.transcript_id NOT REGEXP 'Ada.+'"
                 +" ORDER BY transcript.transcript_id LIMIT 1,1",
                 q.sql);
    assertEquals("Parameter count - id", 0, q.parameters.size());
    
    q = transformer.sqlFor(
      "my('graph').label NOT MATCHES \"Ada.+\"",
      "transcript.transcript_id, transcript.ag_id", null, "id ASC", "LIMIT 1,1");
    assertEquals("SQL - id",
                 "SELECT transcript.transcript_id, transcript.ag_id FROM transcript"
                 +" WHERE transcript.transcript_id NOT REGEXP 'Ada.+'"
                 +" ORDER BY transcript.transcript_id LIMIT 1,1",
                 q.sql);
    assertEquals("Parameter count - id", 0, q.parameters.size());
  }

  @Test public void emptyExpression() throws AGQLException
  {
    GraphAgqlToSql transformer = new GraphAgqlToSql(getSchema());
    GraphAgqlToSql.Query q = transformer.sqlFor(
      "", "transcript.transcript_id, transcript.ag_id", null, null, null);
    assertEquals("SQL - no userWhere",
                 "SELECT transcript.transcript_id, transcript.ag_id FROM transcript"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count - no userWhere", 0, q.parameters.size());

    q = transformer.sqlFor(
      "", "transcript.transcript_id", "transcript.annotated_by = 'user'", "label DESC", null);
    assertEquals("SQL - with userWhere",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE transcript.annotated_by = 'user'"
                 +" ORDER BY transcript.transcript_id DESC",
                 q.sql);
    assertEquals("Parameter count - id", 0, q.parameters.size());
  }

  @Test public void orderBy() throws AGQLException
  {
    GraphAgqlToSql transformer = new GraphAgqlToSql(getSchema());
    GraphAgqlToSql.Query q = transformer.sqlFor(
      "", "transcript.transcript_id, transcript.ag_id", null,
      "my(\"corpus\").label ASC, my(\"episode\").label DESC, ordinal ASC",
      null);
    assertEquals("SQL",
                 "SELECT transcript.transcript_id, transcript.ag_id FROM transcript"
                 +" ORDER BY transcript.corpus_name, (SELECT name"
                 +" FROM transcript_family"
                 +" WHERE transcript_family.family_id = transcript.family_id) DESC,"
                 +" transcript.family_sequence",
                 q.sql);
    assertEquals("Parameter count - no userWhere", 0, q.parameters.size());

  }

  @Test public void corpusLabel() throws AGQLException
  {
    GraphAgqlToSql transformer = new GraphAgqlToSql(getSchema());
    GraphAgqlToSql.Query q = transformer.sqlFor(
      "my(\"corpus\").label = \"CC\"",
      "transcript.transcript_id", null, null, null);
    assertEquals("SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE transcript.corpus_name = 'CC'"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());
  }

  @Test public void literalList() throws AGQLException
  {
    GraphAgqlToSql transformer = new GraphAgqlToSql(getSchema());
    GraphAgqlToSql.Query q = transformer.sqlFor(
      "my(\"corpus\").label IN (\"CC\", 'IA', 'MU', 'corpus', \"episode\")",
      "transcript.transcript_id", null, null, null);
    assertEquals("SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE transcript.corpus_name IN ( 'CC', 'IA', 'MU', 'corpus', 'episode')"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());
  }

  @Test public void corpusLabels() throws AGQLException
  {
    GraphAgqlToSql transformer = new GraphAgqlToSql(getSchema());
    GraphAgqlToSql.Query q = transformer.sqlFor(
      "'CC' IN labels('corpus')",
      "transcript.transcript_id", null, null, null);
    assertEquals("SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE 'CC' IN (SELECT transcript.corpus_name)"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());
  }

  @Test public void episodeLabel() throws AGQLException
  {
    GraphAgqlToSql transformer = new GraphAgqlToSql(getSchema());
    GraphAgqlToSql.Query q = transformer.sqlFor(
      "my(\"episode\").label = 'some-episode'",
      "transcript.transcript_id", null, null, null);
    assertEquals("SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE (SELECT name"
                 +" FROM transcript_family"
                 +" WHERE transcript_family.family_id = transcript.family_id) = 'some-episode'"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());
  }

  @Test public void whoLabels() throws AGQLException
  {
    GraphAgqlToSql transformer = new GraphAgqlToSql(getSchema());
    GraphAgqlToSql.Query q = transformer.sqlFor(
      "'someone' IN labels('who')",
      "transcript.transcript_id", null, null, null);
    assertEquals("Transcript attribute - SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE 'someone' IN"
                 +" (SELECT speaker.name"
                 +" FROM transcript_speaker"
                 +" INNER JOIN speaker"
                 +" ON transcript_speaker.speaker_number = speaker.speaker_number"
                 +" WHERE transcript_speaker.ag_id = transcript.ag_id)"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());

    q = transformer.sqlFor(
      "my('who').label = 'someone'",
      "transcript.transcript_id", null, null, null);
    assertEquals("Transcript attribute - SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE"
                 +" (SELECT speaker.name"
                 +" FROM transcript_speaker"
                 +" INNER JOIN speaker"
                 +" ON transcript_speaker.speaker_number = speaker.speaker_number"
                 +" WHERE transcript_speaker.ag_id = transcript.ag_id"
                 +" ORDER BY speaker.name LIMIT 1) = 'someone'"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());
  }

  @Test public void labels() throws AGQLException
  {
    GraphAgqlToSql transformer = new GraphAgqlToSql(getSchema());
    GraphAgqlToSql.Query q = transformer.sqlFor(
      "'10' IN labels('transcript_rating')",
      "transcript.transcript_id", null, null, null);
    assertEquals("Transcript attribute - SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE '10' IN"
                 +" (SELECT DISTINCT label"
                 +" FROM annotation_transcript USE INDEX(IDX_AG_ID_NAME)"
                 +" WHERE annotation_transcript.layer = 'rating'"
                 +" AND transcript_speaker.ag_id = transcript.ag_id)"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());

    q = transformer.sqlFor(
      "'NA' IN labels('participant_gender')",
      "transcript.transcript_id", null, null, null);
    assertEquals("Transcript attribute - SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE 'NA' IN"
                 +" (SELECT DISTINCT label"
                 +" FROM annotation_participant"
                 +" INNER JOIN transcript_speaker"
                 +" ON annotation_participant.speaker_number = transcript_speaker.speaker_number"
                 +" AND annotation_participant.layer = 'gender'"
                 +" WHERE transcript_speaker.ag_id = transcript.ag_id)"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());
    
    q = transformer.sqlFor(
      "'2019-06-17' IN labels('recording_date')",
      "transcript.transcript_id", null, null, null);
    assertEquals("Episode attribute - SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE"
                 +" '2019-06-17' IN (SELECT label"
                 +" FROM `annotation_layer_-200` annotation"
                 +" WHERE annotation.family_id = transcript.family_id)"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());

    q = transformer.sqlFor(
      "'bell' IN labels('noise')",
      "transcript.transcript_id", null, null, null);
    assertEquals("Annotation - SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE 'bell' IN (SELECT label"
                 +" FROM annotation_layer_32 annotation"
                 +" WHERE annotation.ag_id = transcript.ag_id)"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());
  }

  @Test public void attributeLabel() throws AGQLException
  {
    GraphAgqlToSql transformer = new GraphAgqlToSql(getSchema());
    GraphAgqlToSql.Query q = transformer.sqlFor(
      "my('transcript_scribe').label = 'someone'",
      "transcript.transcript_id", null, null, null);
    assertEquals("Transcript attribute - SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE"
                 +" (SELECT label"
                 +" FROM annotation_transcript USE INDEX(IDX_AG_ID_NAME)"
                 +" WHERE annotation_transcript.layer = 'scribe'"
                 +" AND annotation_transcript.ag_id = transcript.ag_id ORDER BY annotation_id"
                 +" LIMIT 1) = 'someone'"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());

    q = transformer.sqlFor(
      "my('participant_gender').label = 'NA'",
      "transcript.transcript_id", null, null, null);
    assertEquals("Participant attribute - SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE"
                 +" (SELECT label"
                 +" FROM annotation_participant"
                 +" INNER JOIN transcript_speaker"
                 +" ON annotation_participant.speaker_number = transcript_speaker.speaker_number"
                 +" AND annotation_participant.layer = 'gender'"
                 +" WHERE transcript_speaker.ag_id = transcript.ag_id"
                 +" ORDER BY annotation_id LIMIT 1) = 'NA'"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());

    q = transformer.sqlFor(
      "my('recording_date').label > '2019-06-17'",
      "transcript.transcript_id", null, null, null);
    assertEquals("Episode attribute - SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE"
                 +" (SELECT label"
                 +" FROM `annotation_layer_-200` annotation"
                 +" WHERE annotation.family_id = transcript.family_id"
                 +" ORDER BY annotation.ordinal LIMIT 1) > '2019-06-17'"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());

    q = transformer.sqlFor(
      "my('noise').label MATCHES '.*bell.*'",
      "transcript.transcript_id", null, null, null);
    assertEquals("Annotation - SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE"
                 +" (SELECT label"
                 +" FROM annotation_layer_32 annotation"
                 +" INNER JOIN anchor ON annotation.start_anchor_id = anchor.anchor_id"
                 +" WHERE annotation.ag_id = transcript.ag_id"
                 +" ORDER BY anchor.offset, annotation.annotation_id LIMIT 1) REGEXP '.*bell.*'"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());
  }

  @Test public void listLength() throws AGQLException
  {
    GraphAgqlToSql transformer = new GraphAgqlToSql(getSchema());
    GraphAgqlToSql.Query q = transformer.sqlFor(
      "list('transcript_rating').length > 10",
      "transcript.transcript_id", null, null, null);
    assertEquals("Transcript attribute - SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE (SELECT COUNT(*)"
                 +" FROM annotation_transcript USE INDEX(IDX_AG_ID_NAME)"
                 +" WHERE annotation_transcript.layer = 'rating'"
                 +" AND transcript_speaker.ag_id = transcript.ag_id) > 10"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());

    q = transformer.sqlFor(
      "list('participant_gender').length < 1",
      "transcript.transcript_id", null, null, null);
    assertEquals("Transcript attribute - SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE (SELECT COUNT(*)"
                 +" FROM annotation_participant"
                 +" INNER JOIN transcript_speaker"
                 +" ON annotation_participant.speaker_number = transcript_speaker.speaker_number"
                 +" AND annotation_participant.layer = 'gender'"
                 +" WHERE transcript_speaker.ag_id = transcript.ag_id) < 1"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());
    
    q = transformer.sqlFor(
      "list('recording_date').length > 0",
      "transcript.transcript_id", null, null, null);
    assertEquals("Episode attribute - SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE"
                 +" (SELECT COUNT(*)"
                 +" FROM `annotation_layer_-200` annotation"
                 +" WHERE annotation.family_id = transcript.family_id) > 0"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());

    q = transformer.sqlFor(
      "list('transcript').length > 100",
      "transcript.transcript_id", null, null, null);
    assertEquals("Annotation - SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE (SELECT COUNT(*)"
                 +" FROM annotation_layer_0 annotation"
                 +" WHERE annotation.ag_id = transcript.ag_id) > 100"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());
  }

  @Test public void annotators() throws AGQLException
  {
    GraphAgqlToSql transformer = new GraphAgqlToSql(getSchema());
    GraphAgqlToSql.Query q = transformer.sqlFor(
      "'someone' IN annotators('transcript_rating')",
      "transcript.transcript_id", null, null, null);
    assertEquals("Transcript attribute - SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE 'someone' IN"
                 +" (SELECT DISTINCT annotated_by"
                 +" FROM annotation_transcript USE INDEX(IDX_AG_ID_NAME)"
                 +" WHERE annotation_transcript.layer = 'rating'"
                 +" AND transcript_speaker.ag_id = transcript.ag_id)"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());

    q = transformer.sqlFor(
      "'someone' IN annotators('participant_gender')",
      "transcript.transcript_id", null, null, null);
    assertEquals("Transcript attribute - SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE 'someone' IN"
                 +" (SELECT DISTINCT annotated_by"
                 +" FROM annotation_participant"
                 +" INNER JOIN transcript_speaker"
                 +" ON annotation_participant.speaker_number = transcript_speaker.speaker_number"
                 +" AND annotation_participant.layer = 'gender'"
                 +" WHERE transcript_speaker.ag_id = transcript.ag_id)"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());
    
    q = transformer.sqlFor(
      "'someone' IN annotators('recording_date')",
      "transcript.transcript_id", null, null, null);
    assertEquals("Episode attribute - SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE 'someone' IN (SELECT DISTINCT annotated_by"
                 +" FROM `annotation_layer_-200` annotation"
                 +" WHERE annotation.family_id = transcript.family_id)"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());

    q = transformer.sqlFor(
      "'someone' IN annotators('noise')",
      "transcript.transcript_id", null, null, null);
    assertEquals("Annotation - SQL",
                 "SELECT transcript.transcript_id FROM transcript"
                 +" WHERE 'someone' IN (SELECT DISTINCT annotated_by"
                 +" FROM annotation_layer_32 annotation"
                 +" WHERE annotation.ag_id = transcript.ag_id)"
                 +" ORDER BY transcript.transcript_id",
                 q.sql);
    assertEquals("Parameter count", 0, q.parameters.size());
  }
  
  @Test public void invalidLayers() throws AGQLException
  {
    GraphAgqlToSql transformer = new GraphAgqlToSql(getSchema());
    try
    {
      GraphAgqlToSql.Query q = transformer.sqlFor(
        "my('invalid layer 1').label = 'NA'"
        + " AND list('invalid layer 2').length > 2"
        + " AND my('invalid layer 3').label = 'NA'"
        + " AND 'labbcat' NOT IN annotators('invalid layer 4')",
        "transcript.transcript_id", null, null, null);
      fail("sqlFor fails: " + q.sql);
    }
    catch(AGQLException exception)
    {
      assertEquals("Number of errors: " + exception.getErrors(), 4, exception.getErrors().size());
    }
  }

  @Test public void userWhereClause() throws AGQLException
  {
    GraphAgqlToSql transformer = new GraphAgqlToSql(getSchema());
    GraphAgqlToSql.Query q = transformer.sqlFor(
      "label MATCHES \"Ada.+\"",
      "transcript.transcript_id, transcript.ag_id",
      "(EXISTS (SELECT * FROM role"
        + " INNER JOIN role_permission ON role.role_id = role_permission.role_id" 
        + " INNER JOIN annotation_transcript access_attribute" 
        + " ON access_attribute.layer = role_permission.attribute_name" 
        + " AND access_attribute.label REGEXP role_permission.value_pattern"
        + " AND role_permission.entity REGEXP '.*t.*'"
        + " WHERE user_id = 'test'"
        + " AND access_attribute.ag_id = transcript.ag_id)"
        + " OR EXISTS (SELECT * FROM role"
        + " INNER JOIN role_permission ON role.role_id = role_permission.role_id" 
        + " AND role_permission.attribute_name = 'corpus'" 
        + " AND role_permission.entity REGEXP '.*t.*'"
        + " WHERE transcript.corpus_name REGEXP role_permission.value_pattern"
        + " AND user_id = 'test')"
        + " OR NOT EXISTS (SELECT * FROM role_permission))",
      null, null);
    assertEquals(
      "SQL - label",
      "SELECT transcript.transcript_id, transcript.ag_id FROM transcript"
      + " WHERE transcript.transcript_id REGEXP 'Ada.+'"
      + " AND (EXISTS (SELECT * FROM role"
      + " INNER JOIN role_permission ON role.role_id = role_permission.role_id" 
      + " INNER JOIN annotation_transcript access_attribute" 
      + " ON access_attribute.layer = role_permission.attribute_name" 
      + " AND access_attribute.label REGEXP role_permission.value_pattern"
      + " AND role_permission.entity REGEXP '.*t.*'"
      + " WHERE user_id = 'test'"
      + " AND access_attribute.ag_id = transcript.ag_id)"
      + " OR EXISTS (SELECT * FROM role"
      + " INNER JOIN role_permission ON role.role_id = role_permission.role_id" 
      + " AND role_permission.attribute_name = 'corpus'" 
      + " AND role_permission.entity REGEXP '.*t.*'"
      + " WHERE transcript.corpus_name REGEXP role_permission.value_pattern"
      + " AND user_id = 'test')"
      + " OR NOT EXISTS (SELECT * FROM role_permission))"
      + " ORDER BY transcript.transcript_id",
      q.sql);
    assertEquals("Parameter count - label", 0, q.parameters.size());
  }

  public static void main(String args[]) 
  {
    org.junit.runner.JUnitCore.main("nzilbb.ag.sql.test.TestGraphAgqlToSql");
  }

}
