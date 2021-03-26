//
// Copyright 2016 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.formatter.transcriber;
import org.w3c.dom.Node;
import java.io.Writer;

/**
 * Non-speech comment in transcript.
 * @author Robert Fromont
 */
public class Comment extends Event
{
   /**
    * Constructor
    */
   public Comment(String sDesc) 
   {
      super();
      setDescription(sDesc);
      setType("comment");
      setExtent("instantaneous");
   }
   
   /**
    * Constructor
    * @param theSync The sync to which the comment belongs
    * @param eventNode XML node that defines the comment
    */
   public Comment(Sync theSync, Node eventNode) 
   {
      super();
      setDescription(eventNode.getAttributes().getNamedItem("desc").getNodeValue());
      setType("comment");
      setExtent("instantaneous");
      theSync.addEvent(this);
//	 System.out.println("new Comment: " + this);
   } // end of constructor
   
   /**
    * Text-file representation of the object.
    * @param writer Writer for writing the object
    * @throws java.io.IOException
    */
   public void writeText(Writer writer)
      throws java.io.IOException
   {
      // XML tag
      writer.write( 
	 "\n<Comment desc=\"" + Transcript.xmlEscaped(
	    getDescription().replaceAll("\\\"","'")) + "\"/>"); 
   } // writeText

} // end of class Event
