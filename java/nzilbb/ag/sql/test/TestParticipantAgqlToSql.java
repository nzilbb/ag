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
import nzilbb.ag.sql.ParticipantAgqlToSql;
import nzilbb.ag.sql.AGQLException;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class TestParticipantAgqlToSql
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
      
      (Layer)(new Layer("corpus", "Corpus").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true))
      .with("@layer_id", -100),
      
      (Layer)(new Layer("episode", "Episode").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true))
      .with("@layer_id", -50),
      
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
    ParticipantAgqlToSql transformer = new ParticipantAgqlToSql(getSchema());
    ParticipantAgqlToSql.Query q = transformer.sqlFor(
      "label MATCHES \"Ada.+\"", "speaker_number, name", null, "label");
    assertEquals("SQL",
                 "SELECT speaker_number, name FROM speaker"
                 +" WHERE speaker.name REGEXP 'Ada.+' ORDER BY speaker.name",
                 q.sql);
    assertEquals("Parameter count",
                 0, q.parameters.size());
  }

  public static void main(String args[]) 
  {
    org.junit.runner.JUnitCore.main("nzilbb.ag.sql.test.TestParticipantAgqlToSql");
  }

}
