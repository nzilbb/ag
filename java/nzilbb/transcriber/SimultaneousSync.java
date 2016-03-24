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
package nzilbb.transcriber;

import java.io.Writer;
import java.util.StringTokenizer;
import java.util.Enumeration;

/**
 * An utterance during simultaneous speech.
 * @author Robert Fromont
 */

public class SimultaneousSync
   extends Sync
{
   // Attributes:
   
   /**
    * Speaker ID of the speaker speaking this part of the simultaneous speech event
    */
   private String sWho = "";
   /**
    * Who accessor - Speaker ID of the speaker speaking this part of the simultaneous speech event
    * @return Speaker ID of the speaker speaking this part of the simultaneous speech event
    */
   public String getWho() { return sWho; }
   /**
    * Who mutator - Speaker ID of the speaker speaking this part of the simultaneous speech event
    * @param sNewWho Speaker ID of the speaker speaking this part of the simultaneous speech event
    */
   public void setWho(String sNewWho) { sWho = sNewWho; }
   
   /**
    * Constructor
    * @param sSpeakerId Speaker ID of the speaker
    * @param parent Sync to which this speakers speech belongs
    */
   public SimultaneousSync(String sSpeakerId, Sync parent)
   {
      super(parent.getTurn());
      setId(parent.getId());
      setTime(parent.getTime());
      setEndTime(parent.getEndTime());
      setText(""); // text to be set seperately
      
      setWho(sSpeakerId);
   } // end of constructor
   
   /**
    * Text-file representation of the object.
    * @param writer Writer for writing the object
    * @throws java.io.IOException
    */
   public void writeText(Writer writer)
      throws java.io.IOException
   {
      // deduce the 'nb' - the index in the Turn's speaker list
      StringTokenizer tokens = new StringTokenizer(getTurn().getSpeakerId());
      int iNb = 1;
      while (tokens.hasMoreTokens() && !tokens.nextToken().equals(getWho()))
      {
	 iNb++;
      } // next token
      
      // XML tag
      writer.write("\n<Who nb=\"" + iNb + "\"/>"); 
      
      String sTheText = "" + getText();
      int iLastPosition = 0;
      
      // insert any events
      Enumeration<Event> enEvents = vEvents.elements();
      while (enEvents.hasMoreElements())
      {
	 Event event = enEvents.nextElement();
	 writer.write(Transcript.xmlEscaped(
			 sTheText.substring(iLastPosition, event.getTextPosition())));
	 event.writeText(writer);
	 iLastPosition = event.getTextPosition();
      }
      if (sTheText.length() > iLastPosition)
      {
	 writer.write(Transcript.xmlEscaped(sTheText.substring(iLastPosition)).trim());
      }
   } // writeText
} // end of class SimultaneousSync

