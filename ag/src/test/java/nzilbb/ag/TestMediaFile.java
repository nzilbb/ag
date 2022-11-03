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

package nzilbb.ag;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import javax.json.Json;
import javax.json.JsonObject;
import nzilbb.ag.MediaFile;

public class TestMediaFile {

  /** Ensure deserialization from JSON works. */
  @Test public void fromJson() {
    MediaFile a = new MediaFile(
      Json.createObjectBuilder()
      .add("mimeType", "audio/wav")
      .add("url", "https://somewhere/something.wav")
      .add("name", "Audio")
      .add("somethingElse", 100)
      .build());
    
      assertNull(a.getTrackSuffix());
      assertEquals("audio/wav", a.getMimeType());
      assertEquals("https://somewhere/something.wav", a.getUrl());
      assertEquals("Audio", a.getName());
  }
  
  /** Ensure serialization to JSON works. */
  @Test public void toJson() {
    MediaFile a = new MediaFile()
      .setTrackSuffix("_mic")
      .setMimeType("audio/wav")
      .setUrl("https://somewhere/something.wav")
      .setName("Audio");
    JsonObject json = a.toJson();
    assertEquals("_mic", json.getString("trackSuffix"));
    assertEquals("audio/wav", json.getString("mimeType"));
    assertEquals("https://somewhere/something.wav", json.getString("url"));
    assertEquals("Audio", json.getString("name"));
  }


  /** Ensure MIME type inference works for all file types, include unknown types. */
  @Test public void mimeTypeInference() {
    
    // test known types
    assertEquals("audio/wav", new MediaFile().setName("something.wav").getMimeType());
    assertEquals("audio/mpeg", new MediaFile().setName("something.mp3").getMimeType());
    assertEquals("audio/aiff", new MediaFile().setName("something.aif").getMimeType());
    assertEquals("audio/basic", new MediaFile().setName("something.au").getMimeType());
    assertEquals("audio/ogg", new MediaFile().setName("something.oga").getMimeType());
    assertEquals("audio/flac", new MediaFile().setName("something.flac").getMimeType());
    assertEquals("video/mp4", new MediaFile().setName("something.mp4").getMimeType());
    assertEquals("video/mpeg", new MediaFile().setName("something.mpg").getMimeType());
    assertEquals("video/avi", new MediaFile().setName("something.avi").getMimeType());
    assertEquals("video/quicktime", new MediaFile().setName("something.mov").getMimeType());
    assertEquals("audio/x-ms-wma", new MediaFile().setName("something.wma").getMimeType());
    assertEquals("video/x-ms-wmv", new MediaFile().setName("something.wmv").getMimeType());
    assertEquals("video/ogg", new MediaFile().setName("something.ogv").getMimeType());
    assertEquals("video/webm", new MediaFile().setName("something.webm").getMimeType());
    assertEquals("image/jpeg", new MediaFile().setName("something.jpg").getMimeType());
    assertEquals("image/gif", new MediaFile().setName("something.gif").getMimeType());
    assertEquals("image/png", new MediaFile().setName("something.png").getMimeType());

    // unknown types
    assertEquals("application/json", new MediaFile().setName("something.json").getMimeType());
    assertEquals("application/pm", new MediaFile().setName("something.pm").getMimeType());
  }

  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main("nzilbb.ag.TestMediaFile");
  }
}
