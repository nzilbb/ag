//
// Copyright 2016-2021 New Zealand Institute of Language, Brain and Behaviour, 
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
import java.util.Vector;
import java.util.Enumeration;
import java.io.Writer;

/**
 * Synchronized transcript text.
 * @author Robert Fromont
 */
public class Sync
{
   // Attributes:
   
   /**
    * An HTML Anchor ID for the Sync
    */
   private String sId = "";
   /**
    * Id accessor - An HTML Anchor ID for the Sync
    * @return An HTML Anchor ID for the Sync
    */
   public String getId() { return sId; }
   /**
    * Id mutator - An HTML Anchor ID for the Sync
    * @param sNewId An HTML Anchor ID for the Sync
    */
   public void setId(String sNewId) { sId = sNewId; }
   
   /**
    * Start time for the text
    */
   private String sTime = "";
   /**
    * Time accessor - Start time for the text
    * @return Start time for the text
    */
   public String getTime() { return sTime; }
   
   /**
    * End time for the text
    */
   private String sEndTime = "";
   /**
    * EndTime accessor - End time for the text
    * @return End time for the text
    */
   public String getEndTime() { return sEndTime; }
   
   /**
    * The synchronized text
    */
   private String sText = "";
   /**
    * Text accessor - The synchronized text
    * @return The synchronized text
    */
   public String getText() { return sText; }
   /**
    * Text mutator - The synchronized text.  Contiguous whitespace is collapsed to a single space - i.e. newlines, tabs, double-spaces etc.
    * @param sNewText The synchronized text
    */
   public void setText(String sNewText) { sText = sNewText.replaceAll("\\s+", " "); }
   
   // for simultaneous speech, keep a collection of SimultaneousSync objs
   
   /**
    * A collection of {@link SimultaneousSync} objects, if this Sync represents simultaneous speech (i.e. more than one speaker at once)
    */
   private Vector<SimultaneousSync> vSimultaneousSyncs = new Vector<SimultaneousSync>();
   /**
    * SimultaneousSyncs accessor - A collection of {@link SimultaneousSync} objects, if this Sync represents simultaneous speech (i.e. more than one speaker at once)
    * @return A collection of SimultaneousSync objects, if this Sync represents simultaneous speech (i.e. more than one speaker at once)
    */
   public Vector<SimultaneousSync> getSimultaneousSyncs() { return vSimultaneousSyncs; }
   /**
    * SimultaneousSyncs mutator - A collection of {@link SimultaneousSync} objects, if this Sync represents simultaneous speech (i.e. more than one speaker at once)
    * @param vNewSimultaneousSyncs A collection of {@link SimultaneousSync} objects, if this Sync represents simultaneous speech (i.e. more than one speaker at once)
    */
   public void setSimultaneousSyncs(Vector<SimultaneousSync> vNewSimultaneousSyncs) { vSimultaneousSyncs = vNewSimultaneousSyncs; }
   
   /*
    * The last SimultaneousSync object in use
    */
   private SimultaneousSync currentSimultaneousSync = null; 
   
   /**
    * Non-speech events that occur during the sync (which include comments, pronunciations, background noises, etc.) - a list of {@link Event} objects
    */
   protected Vector<Event> vEvents = new Vector<Event>();
   /**
    * Events accessor - Non-speech events that occur during the sync (which include comments, pronunciations, background noises, etc.) - a list of {@link Event} objects
    * @return Non-speech events that occur during the sync (which include comments, pronunciations, background noises, etc.)
    */
   public Vector<Event> getEvents() { return vEvents; }
   /**
    * Events mutator - Non-speech events that occur during the sync (which include comments, pronunciations, background noises, etc.) - a list {@link Event} objects
    * @param vNewEvents Non-speech events that occur during the sync (which include comments, pronunciations, background noises, etc.)
    */
   public void setEvents(Vector<Event> vNewEvents) { vEvents = vNewEvents; }
   
   /**
    * The turn to which this synchronized text belongs
    */
   private Turn tTurn;
   /**
    * Turn accessor - The turn to which this synchronized text belongs
    * @return The turn to which this synchronized text belongs
    */
   public Turn getTurn() { return tTurn; }
   /**
    * Turn mutator - The turn to which this synchronized text belongs
    * @param tNewTurn The turn to which this synchronized text belongs
    */
   public void setTurn(Turn tNewTurn) { tTurn = tNewTurn; }
   
   /**
    * Who accessor - Speaker ID of the speaker speaking this sync
    * @return Speaker ID of the speaker speaking this sync
    */
   public String getWho() { return tTurn.getSpeakerId(); }
   
   /**
    * A list of {@link Word}s that appear in the text
    */
   protected Vector<Word> vWords = null;
   
   /**
    * Constructor
    * @param theTurn Turn to which this Sync belongs
    */
   public Sync(Turn theTurn)
   {
      setTurn(theTurn);
   } // end of constructor
   
   /**
    * Constructor
    * @param theTurn Turn to which this Sync belongs
    * @param syncNode XML node that defines this Sync
    */
   public Sync(Turn theTurn, Node syncNode)
   {
      setTurn(theTurn);
      if (syncNode == null)
      {
	 setTime(getTurn().getStartTime());
      }
      else
      {
	 setTime(syncNode.getAttributes().getNamedItem("time").getNodeValue());
      }
      
      setIdFromTime();
   } // end of constructor
   
   /**
    * Time mutator - Start time for the text
    * @param sNewTime Start time for the text
    */
   public void setTime(String sNewTime)
   {
      sTime = sNewTime;
      setIdFromTime();
      for (SimultaneousSync sync : vSimultaneousSyncs)
      {
	 sync.setTime(sNewTime);
      } // next sub-sync
   } // end of setTime()
   
   /**
    * Sets the ID based on the start time
    */
   protected String setIdFromTime()
   {
      setId("s" + getTime().replace('.', '_'));
      return getId();
   }
   
   /**
    * EndTime mutator - End time for the text
    * @param newEndTime End time for the text
    */
   public void setEndTime(String newEndTime)
   {
      sEndTime = newEndTime;
      for (SimultaneousSync sync : vSimultaneousSyncs)
      {
	 sync.setEndTime(newEndTime);
      } // next sub-sync
   } // end of setEndTime()
   
   /**
    * For simultaneous speech
    * @param speakerId The new speaker for this simultaneous-speech Sync
    */
   public void addWho(String speakerId)
   {
      // add it to the text
      sText += " " + speakerId + ": ";
      
      // add a new simultaneous sync
      currentSimultaneousSync = new SimultaneousSync(speakerId, this);
      vSimultaneousSyncs.add(currentSimultaneousSync);
      
   }
   
   /**
    * Adds a non-speech event to the sync.
    * @param event
    */
   public void addEvent(Event event)
   {
      if (currentSimultaneousSync != null)
      {
	 // try to find the simultaneous sync with the correct speaker
	 SimultaneousSync target = currentSimultaneousSync;
	 for (SimultaneousSync thisSync : vSimultaneousSyncs)
	 {
	    if (thisSync.getWho().equals(event.getWho()))
	    {
	       target = thisSync;
	       break;
	    }
	 } // next simultaneous sync
	 target.addEvent(event);
      }
      else // not simultaneous speech
      {
	 // Set position in the text
	 if (getText().length() > 0)
	 {
	    if (getText().matches("\\\\s$"))
	    {
	       // ends with a space, so move the position back one
	       event.setTextPosition(getText().length()-1);
	    }
	    else
	    {
	       event.setTextPosition(getText().length());
	    }
	 }
	 
	 if (vWords != null && event.getWord() == null)
	 {
	    try
	    {
	       Word lastWord = vWords.lastElement();
	       event.setWord(lastWord); // TODO what about extent = next?
	    }
	    catch(java.util.NoSuchElementException ex)
	    {
	    }
	 }
	 vEvents.add(event);
	 event.setSync(this);
      }
      
   } // end of addEvent()
   
   /**
    * Adds text, appending to the current simultaneous speech sync if required.
    * @param strText
    */
   public void appendText(String strText)
   {
      sText += strText;
      sText = sText.replaceAll("\\s+", " ");
      if (currentSimultaneousSync != null)
      {
	 currentSimultaneousSync.appendText(strText);
      }
   } // end of appendText()
   
   /**
    * Adds a word, appending to the current simultaneous speech sync if required.
    * @param newWord The word to append
    */
   public void appendWord(Word newWord)
   {
      if (currentSimultaneousSync != null)
      {
	 if (!currentSimultaneousSync.getWho().equals(newWord.getWho()))
	 { // change in speaker
	    addWho(newWord.getWho());
	 }
	 currentSimultaneousSync.appendWord(newWord);
      }
      else // not simultaneous speech
      {
	 if (vWords == null) vWords = new Vector<Word>();
	 vWords.addElement(newWord);
      }
      
      // append to the 'text' as well
      if (sText.length() > 0) sText += " ";
      sText += newWord.getRawOrthography();
   } // end of appendText()
   
   /**
    * Determines whether this sync contains simultaneous speech.
    * @return true if this is the parent of simultaneous speech nodes, false otherwise
    */
   public boolean isSimultaneousSpeech()
   {
      return (vSimultaneousSyncs.size() > 0);
   } // end of isSimultaneousSpeech()
   
   /**
    * Breaks the text into words, and returns a Vector of {@link Word} objects.  If this object represents simultaneous speech, then the words are returned in sequential order by speaker - i.e. all the words of one speaker are returned, then all the words of another speaker, etc...
    * @return ordered collection of Word objects
    */
   public Vector<Word> getWords()
   {
      if (!isSimultaneousSpeech())
      {
	 if (vWords == null)
	 {
	    vWords = new Vector<Word>();
	    if (getText().length() > 0)
	    {
	       String sText = getText()
		  .replaceAll("\\n", " ") // catch newlines
		  .replaceAll("\\t", " ") // catch tabs
		  + " "; // ensure last word is found
	       int iCurrentSeek = 0; // current point in the text
	       int iNextSpace = sText.indexOf(' '); // next word delimiter
	       Word lastWord = null; // for adding non-words to 
	       String sPrefixingNonWords = ""; // any non-words before words
	       boolean bAppendedEventsToLastWord = false;
	       
	       // events have to be linked to words, we already know what
	       // position in the text they relate to...
	       Enumeration<Event> enEvents = vEvents.elements();
	       Event currentEvent = null;
	       if (enEvents.hasMoreElements())
	       {
		  currentEvent = enEvents.nextElement();
	       }
	       // any 0-position events?
	       while (currentEvent != null
		      && !currentEvent.getExtent().equals("next") 
		      && currentEvent.getTextPosition() == 0)
	       {
		  currentEvent.setWord(null);		     
		  if (enEvents.hasMoreElements())
		  {
		     currentEvent = enEvents.nextElement();
		  }
		  else
		  {
		     currentEvent = null;
		  }
	       } // next sync-start event
	       
	       while (iNextSpace >= 0)
	       {
		  String sToken = sText.substring(iCurrentSeek, iNextSpace);
		  // if it's really a word, not "." or similar
		  if (sToken.replaceAll("[^\\p{javaLetter}\\p{javaDigit}_]", "").length() > 0
		      // if there's text after an event, it starts a new word
		      // otherwise order of instantaneous annotations is lost when
		      // converting to AG
		      || (bAppendedEventsToLastWord && sToken.trim().length() > 0))
		  {
		     if (sPrefixingNonWords.length() != 0)
		     {
			// handle any text that came before any words
			sToken = sPrefixingNonWords + sToken;
			sPrefixingNonWords = "";
		     }
		     lastWord = new Word(this, sToken);
		     vWords.addElement(lastWord);
		     bAppendedEventsToLastWord = false;
		  }
		  else // not really a word
		  {
		     // append it to the last word
		     if (lastWord != null)
		     {
			lastWord.setRawOrthography(lastWord.getRawOrthography() + " " + sToken);
		     }
		     else // save it for later
		     {
			sPrefixingNonWords += sToken + " ";
		     }
		  }
		  
		  // should an event be associated with this word?
		  while (currentEvent != null 
			 && lastWord != null
			 &&
			 (
			    (
			       !currentEvent.getExtent().equals("next") 
			       && currentEvent.getTextPosition() <= (iNextSpace+1)
			       )
			    ||
			    (
			       currentEvent.getExtent().equals("next") 
			       && currentEvent.getTextPosition() <= iCurrentSeek
			       )
			    )
		     )
		  {
		     // System.out.println("Event: " + currentEvent + " Sync: " + this + " last word: " + lastWord + " iCurrentSeek " + iCurrentSeek + " iNextSpace " + iNextSpace);
		     currentEvent.setWord(lastWord);
		     bAppendedEventsToLastWord = true;
		     if (enEvents.hasMoreElements())
		     {
			currentEvent = enEvents.nextElement();
		     }
		     else
		     {
			currentEvent = null;
		     }
		  } // next event for this word
		  
		  // seek to next word
		  iCurrentSeek = iNextSpace + 1;
		  iNextSpace = sText.indexOf(' ', iCurrentSeek); // next word delimiter
	       } // next word
	       
	       // check that it wasn't all non-words!
	       if (sPrefixingNonWords.length() != 0)
	       {
		  // add one big non-word
		  lastWord = new Word(this, sPrefixingNonWords);
		  vWords.addElement(lastWord);		     
	       }
	       
	       // all leftover events must be added to the last word
	       while (currentEvent != null)
	       {
		  currentEvent.setWord(lastWord);
		  if (enEvents.hasMoreElements())
		  {
		     currentEvent = enEvents.nextElement();
		  }
		  else
		  {
		     currentEvent = null;
		  }
	       } // next event for this word
	       
	    } // there is text
	 } // words vector not yet created
      }
      else // simultaneous speech
      {
	 vWords = new Vector<Word>();
	 // add the words from component SimultaneousSyncs
	 for (SimultaneousSync sync : vSimultaneousSyncs)
	 {
	    vWords.addAll(sync.getWords());
	 } // next sub-sync
      }
      return vWords;
   } // end of getWords()
   
   /**
    * Converts the start time to a Double.
    * @return A double representing the start-time, or -1 if the start-time is not parse-able
    */
   public double getTimeAsDouble()
   {
      try
      {
	 return Double.parseDouble(getTime());
      }
      catch(Exception exception)
      {
	 return -1;
      }
   } // end of getTimeAsDouble()
   
   /**
    * Converts the end time to a Double.
    * @return A double representing the end-time, or -1 if the end-time is not parse-able
    */
   public double getEndTimeAsDouble()
   {
      try
      {
	 return Double.parseDouble(getEndTime());
      }
      catch(Exception exception)
      {
	 return -1;
      }
   } // end of getEndTimeAsDouble()
   
   /**
    * Text-file representation of the object.
    * @param writer Writer for writing the object
    * @throws java.io.IOException
    */
   public void writeText(Writer writer)
      throws java.io.IOException
   {
      // XML tag
      writer.write("\n<Sync time=\"" + getTime() + "\"/>"); 
      if (isSimultaneousSpeech())
      {
	 // syncs
	 for (SimultaneousSync sync : vSimultaneousSyncs)
	 {
	    sync.writeText(writer);
	 } // next speaker
      }
      else
      {
	 String sTheText = "" + sText;
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
      }
   } // writeText
   
   /**
    * Returns a Vector whose only element is this Sync.  This seems silly, but turns out to be handy for rendering Syncs and SimultaneousSyncs using the same code in HTML.
    * @return A Vector containing this Sync
    */
   public Vector<Sync> getSyncInVector()
   {
      Vector<Sync> v = new Vector<Sync>();
      v.add(this);
      return v;
   } // end of getSyncInVector()
   
   /**
    * Text with xml-sensitive characters escaped.
    * @return text with xml-sensitive characters escaped
    */
   public String xmlEscapedText()
   {
      return Transcript.xmlEscaped(getText().trim());
   } // end of xmlEscapedText()
   
   /**
    * A string representation of the object.
    * @return A string representation of the object
    */
   public String toString()
   {
      return "Sync: " + getId() 
	 + (isSimultaneousSpeech()? "\n(simultaneous speech)":"")
	 + "\ntime: " + getTime() + "-" + getEndTime() 
	 + "\nturn: " + getTurn().getId()
	 + "\ntext: \"" + getText() + "\"";
   } // end of toString()
   
} // end of class Sync
