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
import org.w3c.dom.NodeList;
import java.io.Writer;
import java.util.Vector;
import java.util.StringTokenizer;

/**
 * Transcript - speaker turn.
 * @author Robert Fromont
 */
public class Turn
{

   // Attributes:
   
   /**
    * HTML Anchor ID of the turn
    */
   private String sId = "";
   /**
    * Id accessor - HTML Anchor ID of the turn
    * @return ID of the turn
    */
   public String getId() { return sId; }
   /**
    * Id mutator - HTML Anchor ID of the turn
    * @param sNewId ID of the turn
    */
   public void setId(String sNewId) { sId = sNewId; }
   
   /**
    * ID (within the transcript) of the speaker for this Turn
    */
   private String sSpeakerId = "";
   /**
    * SpeakerId accessor - ID (within the transcript) of the speaker for this Turn
    * @return ID (within the transcript) of the speaker for this Turn
    */
   public String getSpeakerId() { return sSpeakerId; }
   /**
    * SpeakerId mutator - ID (within the transcript) of the speaker for this Turn
    * @param sNewSpeakerId ID (within the transcript) of the speaker for this Turn
    */
   public void setSpeakerId(String sNewSpeakerId) { sSpeakerId = sNewSpeakerId; }
   
   /**
    * Start time for this turn
    */
   private String sStartTime = "";
   /**
    * StartTime accessor - Start time for this turn
    * @return Start time for this turn
    */
   public String getStartTime() { return sStartTime; }
   /**
    * StartTime mutator - Start time for this turn
    * @param sNewStartTime Start time for this turn
    */
   public void setStartTime(String sNewStartTime) { sStartTime = sNewStartTime; }
   
   /**
    * End time for this turn
    */
   private String sEndTime = "";
   /**
    * EndTime accessor - End time for this turn
    * @return End time for this turn
    */
   public String getEndTime() 
   { 
      if (sEndTime.length() == 0 && vSyncs.size() > 0)
      {
	 Sync sync = vSyncs.lastElement();
	 sEndTime = sync.getEndTime();
      }
      return sEndTime; 
   }
   /**
    * EndTime mutator - End time for this turn
    * @param sNewEndTime End time for this turn
    */
   public void setEndTime(String sNewEndTime) { sEndTime = sNewEndTime; }
   
   /**
    * Transcript of which this turn is a part
    */
   private Transcript tTranscript;
   /**
    * Transcript accessor - Transcript of which this turn is a part
    * @return Transcript of which this turn is a part
    */
   public Transcript getTranscript() { return tTranscript; }
   /**
    * Transcript mutator - Transcript of which this turn is a part
    * @param tNewTranscript Transcript of which this turn is a part
    */
   public void setTranscript(Transcript tNewTranscript) { tTranscript = tNewTranscript; }
   
   /**
    * Section of which this turn is a part
    */
   private Section secSection;
   /**
    * Section accessor - Section of which this turn is a part
    * @return Section of which this turn is a part
    */
   public Section getSection() { return secSection; }
   /**
    * Section mutator - Section of which this turn is a part
    * @param secNewSection Section of which this turn is a part
    */
   public void setSection(Section secNewSection) 
   {
      secSection = secNewSection; 
      setTranscript(secSection.getTranscript());
   }
   
   /**
    * Ordered list of syncronized transcript parts (A list of {@link nz.ac.canterbury.ling.transcriber.Sync} objects)
    */
   private Vector<Sync> vSyncs = new Vector<Sync>();
   /**
    * Syncs accessor - Ordered list of syncronized transcript parts (Sync objects)
    * @return Ordered list of syncronized transcript parts (A list of {@link nz.ac.canterbury.ling.transcriber.Sync} objects)
    */
   public Vector<Sync> getSyncs() { return vSyncs; }
   /**
    * Syncs mutator - Ordered list of syncronized transcript parts (A list of {@link nz.ac.canterbury.ling.transcriber.Sync} objects)
    * @param vNewSyncs Ordered list of syncronized transcript parts (A list of {@link nz.ac.canterbury.ling.transcriber.Sync} objects)
    */
   public void setSyncs(Vector<Sync> vNewSyncs) { vSyncs = vNewSyncs; }
   
   /**
    * Constructor
    * @param theSection Section to which this Turn belongs
    */
   public Turn(Section theSection)
   {
      setSection(theSection);
   }
   
   /**
    * Constructor
    * @param theSection Section to which this Turn belongs
    * @param turnNode XMLNode that defines this Turn and all the Syncs it contains
    */
   public Turn(Section theSection, Node turnNode)
   {
      setSection(theSection);
      
      try
      {
	 setSpeakerId(turnNode.getAttributes().getNamedItem("speaker").getNodeValue());
      } catch(Exception exception) {}
      try
      {
	 setStartTime(turnNode.getAttributes().getNamedItem("startTime").getNodeValue());
	 setIdFromTime();
      } catch(Exception exception) {}
      try
      {
	 setEndTime(turnNode.getAttributes().getNamedItem("endTime").getNodeValue());
      } catch(Exception exception) {}
      
      // traverse nodes
      NodeList children = turnNode.getChildNodes();
      Sync lastSync = new Sync(this, null); // previous sync
      Sync sync = new Sync(this, null); // current sync
      for (int j = 0; j < children.getLength(); j++)
      {
	 Node child = children.item(j);
	 if (child.getNodeType() == Node.ELEMENT_NODE
	     && child.getNodeName().equalsIgnoreCase("Sync"))
	 { // Sync node
	    lastSync = sync;
	    sync = new Sync(this, child);
	    lastSync.setEndTime(sync.getTime());
	    addSync(sync);
	    //System.out.println("start Sync: " + sync);
	 }
	 else if (child.getNodeType() == Node.ELEMENT_NODE
		  && child.getNodeName().equalsIgnoreCase("Who"))
	 { // Who node - multiple speakers
	    try
	    {
	       // there should be multiple speakers named
	       StringTokenizer names = new StringTokenizer(getSpeakerId(), " ");
	       int speakerIndex = Integer.parseInt(
		  child.getAttributes().getNamedItem("nb").getNodeValue());
	       // go to the indexed speaker
	       while (--speakerIndex > 0)
	       {
		  names.nextToken();
	       }
	       String strSpeaker = names.nextToken();
	       sync.addWho(strSpeaker);
	    }
	    catch (Exception ex)
	    {
	       System.out.println("Failed to parse speaker for Who: " + child.toString() + "\n" + ex.toString());
	    }
	 }
	 else if (child.getNodeType() == Node.ELEMENT_NODE
		  && child.getNodeName().equalsIgnoreCase("Event"))
	 { // Event node - non-speech event
	    Event event = new Event(sync, child);
	    //System.out.println("Event: " + event);
	 }
	 else if (child.getNodeType() == Node.ELEMENT_NODE
		  && child.getNodeName().equalsIgnoreCase("Comment"))
	 { // Event node - non-speech event
	    Comment comment = new Comment(sync, child);
	    //System.out.println("Comment: " + comment);
	 }
	 else if (child.getNodeType() == Node.TEXT_NODE)
	 { // text node
	    String strText = child.getNodeValue();
	    if (strText.trim().length() > 0)
	    {
	       sync.appendText(strText);
	       //System.out.println("Text: " + strText);
	       //System.out.println("Sync now: " + sync);
	    }
	 }
	 else
	 {
	    //System.out.println("Ignoring element: " + child.toString());
	 }
      } // next child    
      
      // set the end time of the last sync
      sync.setEndTime(getEndTime());
      
   } // end of constructor
      
   /**
    * Sets the ID based on the start time
    */
   protected String setIdFromTime()
   {
      setId("t" + getStartTime().replace('.', '_'));
      return getId();
   }
   
   /**
    * Adds a synchronized text-chunk to this turn.
    * @param sync
    */
   public Sync addSync(Sync sync)
   {
      if (!vSyncs.contains(sync))
      {
	 vSyncs.add(sync);
	 
	 // check start/end times
	 if (getStartTime().length() == 0)
	 {
	    setStartTime(sync.getTime());
	 }
	 else
	 {
	    if (sync.getTimeAsDouble() < getStartTimeAsDouble())
	    {
	       setStartTime(sync.getTime());
	    }
	 }
	 
	 if (getEndTime().length() == 0)
	 {
	    setEndTime(sync.getEndTime());
	 }
	 else
	 {
	    if (sync.getEndTimeAsDouble() > getEndTimeAsDouble())
	    {
	       setEndTime(sync.getEndTime());
	    }
	 }
      }
      return sync;
   } // end of addSync()
   
   /**
    * Converts the start time to a Double.
    * @return A double representing the start-time, or -1 if the start-time is not parse-able
    */
   public double getStartTimeAsDouble()
   {
      try
      {
	 return Double.parseDouble(getStartTime());
      }
      catch(Exception exception)
      {
	 return -1;
      }
   } // end of getStartTimeAsDouble()
   
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
    * text-file representation of the object.
    * @param writer Writer for writing the object
    * @throws java.io.IOException
    */
   public void writeText(Writer writer)
      throws java.io.IOException
   {
      if (vSyncs.size() > 0) // only if there are syncs
      {
	 // validate start/end times
	 for (Sync sync : vSyncs)
	 {
	    if (sync.getTimeAsDouble() < getStartTimeAsDouble())
	    {
	       setStartTime(sync.getTime());
	    }
	    if (sync.getEndTimeAsDouble() > getEndTimeAsDouble())
	    {
	       setEndTime(sync.getEndTime());
	    }
	 } // next speaker
	 
	 // XML header
	 writer.write( 
	    "\n<Turn speaker=\"" + Transcript.xmlEscaped(getSpeakerId()) + "\" "
	    + "startTime=\"" + getStartTime() + "\" "
	    + "endTime=\"" + getEndTime() + "\">"); 
	 // syncs
	 for (Sync sync : vSyncs)
	 {
	    sync.writeText(writer);
	 } // next speaker
	 
	 // close tags
	 writer.write("\n</Turn>");
      }
      
   } // writeText
   
   /**
    * String representation of the object.
    */
   public String toString()
   {
      return "Turn: " + getId() + "\nspeaker: " + getSpeakerId()
	 + "\nstartTime: " + getStartTime() + "\nendTime: " + getEndTime();
   } // end of toString()

} // end of class Turn
