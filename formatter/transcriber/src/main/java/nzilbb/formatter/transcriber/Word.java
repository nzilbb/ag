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

import java.util.Vector;

/**
 * Collection of representations of a single word.
 * @author Robert Fromont
 */
public class Word
{
   // Attributes:
   
   /**
    * Sync to which this word belongs
    */
   private Sync synSync;
   /**
    * Sync accessor - Sync to which this word belongs
    * @return Sync to which this word belongs
    */
   public Sync getSync() { return synSync; }
   /**
    * Sync mutator - Sync to which this word belongs
    * @param synNewSync Sync to which this word belongs
    */
   public void setSync(Sync synNewSync) { synSync = synNewSync; }
   
   /**
    * Ordinal position of this word in the Sync text
    */
   private int iOrdinal = -1; // -1 = uninitialised
   /**
    * Ordinal accessor - Ordinal position of this word in the Sync text
    * @return Ordinal position of this word in the Sync text
    */
   public int getOrdinal() { return iOrdinal; }
   /**
    * Ordinal mutator - Ordinal position of this word in the Sync text
    * @param iNewOrdinal Ordinal position of this word in the Sync text
    */
   public void setOrdinal(int iNewOrdinal) { iOrdinal = iNewOrdinal; }
   
   public double number = -1;
   
   /**
    * The speaker ID of the speaker of this word
    */
   private String sWho = "";
   /**
    * Who accessor - The speaker ID of the speaker of this word
    * @return The speaker ID of the speaker of this word
    */
   public String getWho() { return sWho; }
   /**
    * Who mutator - The speaker ID of the speaker of this word
    * @param sNewWho The speaker ID of the speaker of this word
    */
   public void setWho(String sNewWho) { sWho = sNewWho; }
      
   /**
    * Orthography of the word as originally written in Transcriber
    */
   private String sRawOrthography;
   /**
    * RawOrthography accessor - Orthography of the word as originally written in Transcriber
    * @return Orthography of the word as originally written in Transcriber
    */
   public String getRawOrthography() { return sRawOrthography; }
   /**
    * RawOrthography mutator - Orthography of the word as originally written in Transcriber
    * @param sNewRawOrthography Orthography of the word as originally written in Transcriber
    */
   public void setRawOrthography(String sNewRawOrthography) { sRawOrthography = sNewRawOrthography; }
   
   /**
    * List of Events associated with this word
    */
   private Vector<Event> vEvents = new Vector<Event>();
   /**
    * Events accessor 
    * @return List of Events associated with this word
    */
   public Vector<Event> getEvents() { return vEvents; }
   /**
    * Events mutator
    * @param vNewEvents List of Events associated with this word
    */
   public void setEvents(Vector<Event> vNewEvents) { vEvents = vNewEvents; }
   
   /**
    * Constructor
    * @param theSync Sync to which the word belongs
    */
   public Word(Sync theSync)
   {
      setSync(theSync);
   } // end of constructor
   
   /**
    * Constructor
    * @param theSync Sync to which the word belongs
    * @param strOrthography Orthography of the word
    */
   public Word(Sync theSync, String strOrthography)
   {
      setSync(theSync);
      setRawOrthography(strOrthography);
   } // end of constructor
   
   /*
    * The cleaned-up orthography of the word
    * @return the word with spurious punctuation removed
    */
   public String getOrthography() 
   { 
      return getOrthography(getRawOrthography()); 
   }
   
   /**
    * The cleaned-up orthography of the word
    * @param sTranscription The raw transcription of the word
    * @return the word with spurious punctuation removed, or null if sTranscription is null
    */
   public static String getOrthography(String sTranscription) 
   { 
      if (sTranscription == null) return null;
      return sTranscription.toLowerCase()
//	    .replaceAll("[^'\\-a-z]","") // remove non-word characters
	 .replaceAll("\\s","") //collapse all space (there could be space because of appended non-words)
	 .replaceAll("[\\!\"#\\$%\\&\\(\\)\\*\\+\\,\\./:;<=>\\?@\\[\\\\\\]\\^_`\\{\\|\\}]","") // remove non-word characters (but leave '~' in)
	 .trim() // might be spaces left after stripping
	 .replaceAll("^[\\-']*", "") // remove leading hyphens/apostrophes
	 .trim() // might be spaces left after stripping
	 .replaceAll("[\\-']*$", "") // remove trailing hyphens/apostrophes
	 .trim(); // might be spaces left after stripping
   }   
   
   /**
    * Associates an Event with this Word.
    * @param event
    */
   public void addEvent(Event event)
   {
      if (!vEvents.contains(event))
      {
	 vEvents.add(event);
      }
   } // end of addEvent()
   
   /**
    * Dis-associates an Event with this Word.
    * @param event
    */
   public void removeEvent(Event event)
   {
      if (vEvents.contains(event))
      {
	 vEvents.remove(event);
      }
   } // end of addEvent()
   
   /**
    * String representation of the word.
    */
   public String toString()
   {
      return getRawOrthography();
   } // end of toString()
   
} // end of class Word
