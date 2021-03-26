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

package nzilbb.ag.automation.util;
	      
import org.junit.*;
import static org.junit.Assert.*;

import java.net.URI;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import nzilbb.util.IO;
import nzilbb.ag.Constants;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.automation.util.RequestRouter;
import nzilbb.ag.automation.util.RequestException;
import nzilbb.ag.automation.example.minimal.MinimalExample;
import nzilbb.ag.automation.example.theworks.TheWorksExample;

public class TestRequestRouter {
   
   Schema schema
   = new Schema(
      "who", "turn", "utterance", "word",
      new Layer("who", "Participants").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(true).setPeersOverlap(true).setSaturated(true),
      new Layer("turn", "Speaker turns").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("who").setParentIncludes(true),
      new Layer("word", "Words").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false)
      .setParentId("turn").setParentIncludes(true),
      new Layer("utterance", "Utterances").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(true)
      .setParentId("turn").setParentIncludes(true));

   // enctypes:
   String urlencoded = "application/x-www-form-urlencoded";
   String multipart = "multipart/form-data";


   /**
    * Converts a String into an InputStream.
    * @param string
    * @return An InputStream containing the bytes of the given string.
    */
   public InputStream body(String string) {
      return new ByteArrayInputStream(string.toString().getBytes());
   } // end of body()
   
   @Test public void successes() throws Exception {
      
      TheWorksExample annotator = new TheWorksExample();
      annotator.setSchema(schema);
      RequestRouter router = new RequestRouter(annotator);
      
      assertEquals("Method with no parameters", annotator.getAnnotatorId(),
                   IO.InputStreamToString(
                      router.request("GET", "http://foo/bar/getAnnotatorId", null, null)));
      
      annotator.setReverse(null);
      assertNull("Method with no parameters that returns null",
                 router.request("GET", "http://foo/bar/getReverse", null, null));
      
      assertNull("Method with one Boolean parameter that returns void",
                 router.request("GET", "http://foo/bar/setReverse?true", null, null));
      
      assertEquals("Method with no parameters that returns Boolean", "true",
                   IO.InputStreamToString(
                      router.request("GET", "http://foo/bar/getReverse", null, null)));
      assertNull("POST Method with two int parameters",
                 router.request("POST", "http://foo/bar/setPadding", urlencoded, body("1,2")));
      assertEquals("Method with no parameters that returns int", "2",
                   IO.InputStreamToString(
                      router.request("GET", "http://foo/bar/getRightPadding", null, null)));
      
      assertNull("Method with a String parameter that includes a comma",
                 router.request("GET", "http://foo/bar/setPrefix?b4%20%2C%20after", null, null));
      assertEquals("Encoded entities are decoded",
                   "b4 , after", annotator.getPrefix());

      // TODO multipart requests
   }

   @Test public void failures() throws Exception {
      
      TheWorksExample annotator = new TheWorksExample();
      annotator.setSchema(schema);
      RequestRouter router = new RequestRouter(annotator);

      try {
         router.request("GET", "http://foo/bar/nonexistent", null, null);
         fail("Cannot call nonexistent method");
      } catch(RequestException exception) {
         assertEquals("Nonexistent failure status",
                      404, exception.getHttpStatus());
      }
      
      try {
         router.request("GET", "http://foo/bar/getAnnotatorId?something", null, null);
         fail("Too many parameters parameters");
      } catch(RequestException exception) {
         assertEquals("Too many parameters parameters failure status",
                      400, exception.getHttpStatus());
      }

      try {
         router.request("POST", "http://foo/bar/setPadding", urlencoded, body("1"));
         fail("Not enough parameters");
      } catch(RequestException exception) {
         assertEquals("Not enough parameters failure status",
                      400, exception.getHttpStatus());
      }

      try {
         router.request("GET", "http://foo/bar/setLeftPadding?something", null, null);
         fail("Invalid parameter");
      } catch(RequestException exception) {
         assertEquals("Nonexistent failure status",
                      400, exception.getHttpStatus());
      }
   }

   public static void main(String args[]) {
      org.junit.runner.JUnitCore.main("nzilbb.ag.automation.util.TestRequestRouter");
   }
}
