//
// Copyright 2015-2017 New Zealand Institute of Language, Brain and Behaviour, 
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

package nzilbb.ag.serialize;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import javax.json.Json;
import javax.json.JsonObject;
import nzilbb.ag.serialize.SerializationDescriptor;

public class TestSerializationDescriptor
{      
   @Test public void fromJson() {
      SerializationDescriptor a = new SerializationDescriptor(
         Json.createObjectBuilder()
         .add("mimeType", "text/plain")
         .add("icon", "https://somewhere/something.png")
         .add("name", "Name")
         .add("version", "0.0.0")
         .add("numberOfInputs", 2)
         .add("minimumApiVersion", "00000000.0000")
         .add("fileSuffixes", Json.createArrayBuilder().add("txt"))
         .build());
      
      assertEquals("text/plain", a.getMimeType());
      assertEquals("https://somewhere/something.png", a.getIcon().toString());
      assertEquals("Name", a.getName());
      assertEquals("0.0.0", a.getVersion());
      assertEquals("00000000.0000", a.getMinimumApiVersion());
      assertEquals(2, a.getNumberOfInputs());
      assertEquals(1, a.getFileSuffixes().size());
      assertEquals("txt", a.getFileSuffixes().firstElement());
   }
   
   @Test public void toJson() {
      SerializationDescriptor a = new SerializationDescriptor(
         "Name", "0.0.0", "text/plain", ".txt", "00000000.0000",
         null, 2);
      JsonObject json = a.toJson();
      assertEquals("Name", json.getString("name"));
      assertEquals("0.0.0", json.getString("version"));
      assertEquals("text/plain", json.getString("mimeType"));
      assertEquals("00000000.0000", json.getString("minimumApiVersion"));
      assertEquals(2, json.getInt("numberOfInputs"));
      assertEquals(".txt", json.getJsonArray("fileSuffixes").getString(0));
   }

   public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nzilbb.ag.serialize.TestSerializationDescriptor");
   }
}
