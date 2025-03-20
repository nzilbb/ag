//
// Copyright 2025 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.configure;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.Vector;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import nzilbb.ag.Layer;
import nzilbb.util.IO;

/** Unit tests for {@link Parameter} */
public class TestParameterSet {

  /** Test that serialization to JSON works. */
  @Test public void toJson() {

    ParameterSet ps = new ParameterSet();
    ps.addParameter(
      new Parameter(
        "labbcat_episode", String.class, "Episode",
        "The recording episode the transcript belongs to", false)
      .setValue("episode 1")
      );
    ps.addParameter(
      new Parameter(
        "labbcat_corpus", String.class, "Corpus",
        "The broad collection the transcript belongs to", true)
      .setPossibleValues(new Vector() {{ add("CC"); add("IA"); add("MU"); }})
      );
        Layer l1 = new Layer("l1", "First layer");
    Layer l2 = new Layer("l2", "Second layer");
    ps.addParameter(
      new Parameter(
        "layer", Layer.class, "Layer",
        "A layer parameter", true)
      .setPossibleValues(new Vector() {{ add(l1); add(l2); }})
      .setValue(l2)
      );
    assertEquals("JSON serialization",
                 "[{"
                 +"\"hint\":\"The recording episode the transcript belongs to\","
                 +"\"label\":\"Episode\","
                 +"\"name\":\"labbcat_episode\","
                 +"\"required\":false,"
                 +"\"type\":\"java.lang.String\","
                 +"\"value\":\"episode 1\""
                 +"},{"
                 +"\"hint\":\"The broad collection the transcript belongs to\","
                 +"\"label\":\"Corpus\","
                 +"\"name\":\"labbcat_corpus\","
                 +"\"required\":true,"
                 +"\"type\":\"java.lang.String\","
                 +"\"possibleValues\":[\"CC\",\"IA\",\"MU\"]"
                 +"},{"
                 +"\"hint\":\"A layer parameter\","
                 +"\"label\":\"Layer\","
                 +"\"name\":\"layer\","
                 +"\"required\":true,"
                 +"\"type\":\"nzilbb.ag.Layer\","
                 +"\"value\":\"l2\","
                 +"\"possibleValues\":[\"l1\",\"l2\"]"
                 +"}]",
                 ps.toJson().toString());
  }

  /** Test that serialization to JSON works. */
  @Test public void fromJson() {

    JsonArrayBuilder json = Json.createArrayBuilder()
      .add(Json.createObjectBuilder()
           .add("name", "labbcat_episode")
           .add("type", "java.lang.String")
           .add("label", "Episode")
           .add("hint", "The recording episode the transcript belongs to")
           .add("required", false)
           .add("value", "episode 1"))
      .add(Json.createObjectBuilder()
           .add("name", "threshold")
           .add("type", "java.lang.Double")
           .add("label", "Threshold")
           .add("hint", "A numeric threshold")
           .add("required", false)
           .add("possibleValues", Json.createArrayBuilder().add(1.0).add(1.5).add(2.0))
           .add("value", 1.5))
      .add(Json.createObjectBuilder()
           .add("name", "layer")
           .add("type", "nzilbb.ag.Layer")
           .add("label", "Layer")
           .add("hint", "A layer")
           .add("required", true)
           .add("possibleValues", Json.createArrayBuilder().add("l1").add("l2"))
           .add("value", "l3"));
    ParameterSet ps = new ParameterSet().fromJson(json.build());
    assertEquals("Number of parameters",
                 3, ps.size());
    Parameter p = ps.get("labbcat_episode");
    assertNotNull("labbcat_episode", p);
    assertEquals("Basic JSON deserialization: hint",
                 "The recording episode the transcript belongs to", p.getHint());
    assertEquals("Basic JSON deserialization: label",
                 "Episode", p.getLabel());
    assertEquals("Basic JSON deserialization: name",
                 "labbcat_episode", p.getName());
    assertFalse("Basic JSON deserialization: required", p.getRequired());
    assertEquals("Basic JSON deserialization: type",
                 String.class, p.getType());
    assertEquals("Basic JSON deserialization: value",
                 "episode 1", p.getValue());

    p = ps.get("threshold");
    assertNotNull("threshold", p);
    assertEquals("Numeric value with possibleValues: hint",
                 "A numeric threshold", p.getHint());
    assertEquals("Numeric value with possibleValues: label",
                 "Threshold", p.getLabel());
    assertEquals("Numeric value with possibleValues: name",
                 "threshold", p.getName());
    assertFalse("Numeric value with possibleValues: required", p.getRequired());
    assertEquals("Numeric value with possibleValues: type",
                 Double.class, p.getType());
    assertEquals("Numeric value with possibleValues: value",
                 Double.valueOf(1.5), (Double)p.getValue());
    assertNotNull("Numeric value with possibleValues: possibleValues",
                  p.getPossibleValues());
    assertEquals("Numeric value with possibleValues: number of possibleValues: "
                 +p.getPossibleValues(),
                 3, p.getPossibleValues().size());
    Iterator<Double> possibilities = (Iterator<Double>)p.getPossibleValues().iterator();
    assertEquals("Numeric value with possibleValues: possibility 1",
                 Double.valueOf(1.0), possibilities.next());
    assertEquals("Numeric value with possibleValues: possibility 2",
                 Double.valueOf(1.5), possibilities.next());
    assertEquals("Numeric value with possibleValues: possibility 3",
                 Double.valueOf(2.0), possibilities.next());

    p = ps.get("layer");
    assertNotNull("layer", p);
    assertEquals("Layer value with possibleValues: hint",
                 "A layer", p.getHint());
    assertEquals("Layer value with possibleValues: label",
                 "Layer", p.getLabel());
    assertEquals("Layer value with possibleValues: name",
                 "layer", p.getName());
    assertTrue("Layer value with possibleValues: required", p.getRequired());
    assertEquals("Layer value with possibleValues: type",
                 Layer.class, p.getType());
    assertEquals("Layer value with possibleValues: value",
                 "l3", ((Layer)p.getValue()).getId());
    assertNotNull("Layer value with possibleValues: possibleValues",
                  p.getPossibleValues());
    assertEquals("Layer value with possibleValues: number of possibleValues: "
                 +p.getPossibleValues(),
                 2, p.getPossibleValues().size());
    Iterator<Layer> possibleLayers = (Iterator<Layer>)p.getPossibleValues().iterator();
    assertEquals("Layer value with possibleValues: possibility 1",
                 "l1", ((Layer)possibleLayers.next()).getId());
    assertEquals("Layer value with possibleValues: possibility 2",
                 "l2", ((Layer)possibleLayers.next()).getId());
  }

  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.configre.TestParameterSet");
  }
}
