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
 * Non-speech event in transcript.
 * @author Robert Fromont
 */
public class Event
{
   // Attributes:
   
   /**
    * Position of the event in the Sync text
    */
   private int iTextPosition = 0;
   /**
    * TextPosition accessor - Position of the event in the Sync text
    * @return Position of the event in the Syn text
    */
   public int getTextPosition() { return iTextPosition; }
   /**
    * TextPosition mutator - Position of the event in the Sync text
    * @param iNewTextPosition Position of the event in the Syn text
    */
   public void setTextPosition(int iNewTextPosition) 
   { 
      iTextPosition = iNewTextPosition; 
      if (iTextPosition < 0) iTextPosition = 0;
   }
   
   /**
    * Word with which the event is immediately associated
    */
   private Word wWord;
   /**
    * Word accessor - Word with which the event is immediately associated
    * @return Word with which the event is immediately associated
    */
   public Word getWord() { return wWord; }
   /**
    * Word mutator - Word with which the event is immediately associated
    * @param wNewWord Word with which the event is immediately associated
    */
   public void setWord(Word wNewWord) 
   { 
      if (wWord != null) wWord.removeEvent(this);
      wWord = wNewWord; 
      if (wWord != null) wWord.addEvent(this);
   }
   
   /**
    * Speaker ID of the Event
    */
   private String sWho = "";
   /**
    * Who accessor - Speaker ID of the Event
    * @return Speaker ID of the Event
    */
   public String getWho() { return sWho; }
   /**
    * Who mutator - Speaker ID of the Event
    * @param sNewWho Speaker ID of the Event
    */
   public void setWho(String sNewWho) { sWho = sNewWho; }

   /**
    * Event description
    */
   private String sDescription = "";
   /**
    * Description accessor - Event description
    * @return Event description
    */
   public String getDescription() { return sDescription; }
   /**
    * Description mutator - Event description
    * @param sNewDescription Event description
    */
   public void setDescription(String sNewDescription) 
   { 
      sDescription = sNewDescription; 
      if (sDescription.length() > 255)
      {
	 sDescription = sDescription.substring(0,255);
      }
   }
   
   /**
    * Event type. Possible values are: noise, lexical, pronounce, language, entities, comment.
    */
   private String sType;
   /**
    * Type accessor - Event type. Possible values are: noise, lexical, pronounce, language, entities, comment.
    * @return Event type
    */
   public String getType() { return sType; }
   /**
    * Type mutator - Event type. Possible values are: noise, lexical, pronounce, language, entities, comment.
    * @param sNewType Event type
    */
   public void setType(String sNewType) { sType = sNewType; }
   
   /**
    * Extent of the event. Possible values are begin, end, previous, next, instantaneous.
    */
   private String sExtent;
   /**
    * Extent accessor - Extent of the event. Possible values are begin, end, previous, next, instantaneous.
    * @return Extent of the event
    */
   public String getExtent() { return sExtent; }
   /**
    * Extent mutator - Extent of the event. Possible values are begin, end, previous, next, instantaneous.
    * @param sNewExtent Extent of the event
    */
   public void setExtent(String sNewExtent) { sExtent = sNewExtent; }
   
   /**
    * Synch to which the event belongs
    */
   private Sync synSync;
   /**
    * Sync accessor - Synch to which the event belongs
    * @return Synch to which the event belongs
    */
   public Sync getSync() { return synSync; }
   /**
    * Sync mutator - Synch to which the event belongs
    * @param synNewSync Synch to which the event belongs
    */
   public void setSync(Sync synNewSync) { synSync = synNewSync; }
   
   /**
    * Constructor
    */
   public Event()
   {
   } // end of constructor
   
   /**
    * Constructor
    * @param sDesc Description/text of the event
    * @param sType Event type
    * @param sExtent Extent of the event
    */
   public Event(String sDesc, String sType, String sExtent)
   {
      setDescription(sDesc);
      setType(sType);
      setExtent(sExtent);
   } // end of constructor
   
   /**
    * Constructor
    * @param theSync Sync to which the event belongs
    * @param eventNode XML Node that defines the event
    */
   public Event(Sync theSync, Node eventNode)
   {
      setDescription(eventNode.getAttributes().getNamedItem("desc").getNodeValue());
      setType(eventNode.getAttributes().getNamedItem("type").getNodeValue());
      setExtent(eventNode.getAttributes().getNamedItem("extent").getNodeValue());
      if (theSync instanceof SimultaneousSync)
      {
	 SimultaneousSync sim = (SimultaneousSync) theSync;
	 setWho(sim.getWho());
      }
      else
      {
	 setWho(theSync.getTurn().getSpeakerId());
      }
      theSync.addEvent(this);
//	 System.out.println("new Event: " + this);
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
	 "\n<Event desc=\"" + Transcript.xmlEscaped(
	    getDescription().replaceAll("\\\"","'")) 
	 + "\" type=\"" + Transcript.xmlEscaped(
	    getType().replaceAll("\\\"","'")) 
	 + "\" extent=\"" + Transcript.xmlEscaped(
	    getExtent().replaceAll("\\\"","'")) + "\"/>\n"); 
   } // writeText
   
   /**
    * String representation of the object
    * @return String representation of the object
    */
   public String toString()
   {
      return getClass().getName() + ": "
	 + getDescription()
	 + " type: " + getType()
	 + " extent: " + getExtent()
	 + " textposition: " + getTextPosition()
	 + " word: " + getWord()
	 + " who: " + getWho()
	 + " sync: " + getSync(); 
   } // end of toString()
   
} // end of class Event
