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

/**
 * Transcript - section - this is a collection of Turns, with an associated start/end time, and optionally a topic id.
 * @author Robert Fromont
 */
public class Section
{
   // Attributes:
   
   /**
    * ID of the section
    */
   protected String sId = "";
   /**
    * Id accessor - ID of the section
    * @return ID of the section
    */
   public String getId() { return sId; }
   /**
    * Id mutator - ID of the section
    * @param sNewId ID of the section
    */
   public void setId(String sNewId) { sId = sNewId; }
   
   /**
    * Section type
    */
   protected String sType = "report";
   /**
    * Type accessor - Section type
    * @return Section type
    */
   public String getType() { return sType; }
   /**
    * Type mutator - Section type
    * @param sNewType Section type
    */
   public void setType(String sNewType) { sType = sNewType; }
   
   /**
    * Topic ID for this section
    */
   protected String sTopic = "";
   /**
    * Topic accessor - Topic ID for this section
    * @return Topic ID for this section
    */
   public String getTopic() { return sTopic; }
   /**
    * Topic mutator - Topic ID for this section
    * @param sNewTopic Topic ID for this section
    */
   public void setTopic(String sNewTopic) { sTopic = sNewTopic; }
   
   /**
    * Start Time of the section
    */
   protected String sStartTime = "";
   /**
    * StartTime accessor - Start Time of the section
    * @return Start Time of the section
    */
   public String getStartTime() 
   { 
      // check for blank start time
      if (sStartTime.length() == 0 && vTurns.size() > 0)
      {
	 Turn turn = vTurns.firstElement();
	 setStartTime(turn.getStartTime());
      }
      return sStartTime; 
   }
   /**
    * StartTime mutator - Start Time of the section
    * @param sNewStartTime Start Time of the section
    */
   public void setStartTime(String sNewStartTime) { sStartTime = sNewStartTime; }
   
   /**
    * End time of the section
    */
   protected String sEndTime = "";
   /**
    * EndTime accessor - End time of the section
    * @return End time of the section
    */
   public String getEndTime() 
   {
      // check for blank end time
      if (sEndTime.length() == 0 && vTurns.size() > 0)
      {
	 Turn turn = vTurns.lastElement();
	 setEndTime(turn.getEndTime());
      }
      return sEndTime; 
   }
   /**
    * EndTime mutator - End time of the section
    * @param sNewEndTime End time of the section
    */
   public void setEndTime(String sNewEndTime) { sEndTime = sNewEndTime; }
   
   /**
    * The transcript to which the section belongs
    */
   protected Transcript tTranscript;
   /**
    * Transcript accessor - The transcript to which the section belongs
    * @return The transcript to which the section belongs
    */
   public Transcript getTranscript() { return tTranscript; }
   /**
    * Transcript mutator - The transcript to which the section belongs
    * @param tNewTranscript The transcript to which the section belongs
    */
   public void setTranscript(Transcript tNewTranscript) { tTranscript = tNewTranscript; }
   
   /**
    * Turns in this Section (A list of {@link Turn} objects)
    */
   protected Vector<Turn> vTurns = new Vector<Turn>();
   /**
    * Turns accessor
    * @return Turns in this Section (A list of {@link Turn} objects)
    */
   public Vector<Turn> getTurns() { return vTurns; }
   /**
    * Turns mutator
    * @param vNewTurns Turns in this Section (A list of {@link Turn} objects)
    */
   public void setTurns(Vector<Turn> vNewTurns) { if (vNewTurns != null) vTurns = vNewTurns; }
   
   /**
    * Constructor
    * @param theTranscript The transcript to which the section belongs
    */
   public Section(Transcript theTranscript)
   {
      setTranscript(theTranscript);
   }
   
   /**
    * Constructor
    * @param theTranscript The transcript to which the section belongs
    * @param sectionNode XML node that defins the section and all the Turns that it contains
    */
   public Section(Transcript theTranscript, Node sectionNode)
   {
      setTranscript(theTranscript);
      try
      {
	 setType(sectionNode.getAttributes().getNamedItem("type").getNodeValue());
      } catch(Exception exception) {}
      try
      {
	 setTopic(sectionNode.getAttributes().getNamedItem("topic").getNodeValue());
      } catch(Exception exception) {}
      try
      {
	 setStartTime(sectionNode.getAttributes().getNamedItem("startTime").getNodeValue());
	 setIdFromTime();
      } catch(Exception exception) {}
      try
      {
	 setEndTime(sectionNode.getAttributes().getNamedItem("endTime").getNodeValue());
      } catch(Exception exception) {}
      
      // traverse nodes
      NodeList children = sectionNode.getChildNodes();
      for (int j = 0; j < children.getLength(); j++)
      {
	 Node child = children.item(j);
	 if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("Turn"))
	 { // Turn node
	    Turn turn = new Turn(this, child);
	    addTurn(turn);
	 }
	 else
	 {
	    //System.out.println("Ignoring element: " + child.toString());
	 }
      } // next child
      
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
    * Add the given turn to the section, adjusting the section's start/end times if appropriate.
    * @param turn The Turn to add
    * @return The Turn object added
    */
   public Turn addTurn(Turn turn)
   {
      if (!vTurns.contains(turn))
      {
	 vTurns.add(turn);
	 
	 // check start/end times
	 if (getStartTime().length() == 0)
	 {
	    setStartTime(turn.getStartTime());
	 }
	 else
	 {
	    if (turn.getStartTimeAsDouble() < getStartTimeAsDouble())
	    {
	       setStartTime(turn.getStartTime());
	    }
	 }
	 
	 if (getEndTime().length() == 0)
	 {
	    setEndTime(turn.getEndTime());
	 }
	 else
	 {
	    if (turn.getEndTimeAsDouble() > getEndTimeAsDouble())
	    {
	       setEndTime(turn.getEndTime());
	    }
	 }
      }
      return turn;
   } // end of addTurn()
   
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
      catch(Throwable exception)
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
      catch(Throwable exception)
      {
	 return -1;
      }
   } // end of getEndTimeAsDouble()
   
   /**
    * Looks up the name of the topic, using the ID.
    * @return The topic name, or the topic ID if the name cannotbe determined
    */
   public String getTopicName()
   {
      try
      {
	 String sName = getTranscript().getTopics().getProperty(getTopic());
	 if (sName == null)
	 {
	    return getTopic();
	 }
	 else
	 {
	    return sName;
	 }
      }
      catch (Throwable x)
      {
	 return getTopic();
      }
   } // end of getTopicName()
   
   /**
    * text-file representation of the object.
    * @param writer Writer for writing the object
    * @throws java.io.IOException
    */
   public void writeText(Writer writer)
      throws java.io.IOException
   {
      // turns
      for (Turn turn : getTurns())
      {
	 if (turn.getStartTimeAsDouble() < getStartTimeAsDouble())
	 {
	    setStartTime(turn.getStartTime());
	 }
	 if (turn.getEndTimeAsDouble() > getEndTimeAsDouble())
	 {
	    setEndTime(turn.getEndTime());
	 }
      } // next turn
      
      // XML header
      writer.write( 
	 "\n<Section type=\"" + Transcript.xmlEscaped(getType()) + "\" "
	 + (getTopic().length()>0? "topic=\"" + getTopic() + "\" " : "" )
	 + "startTime=\"" + getStartTime() + "\" "
	 + "endTime=\"" + getEndTime() + "\">"); 
      // turns
      for (Turn turn : getTurns())
      {
	 turn.writeText(writer);
      } // next turn
      
      // close tags
      writer.write("\n</Section>");
      
   } // writeText
   
   /**
    * Basic String representation of the object.
    */
   public String toString()
   {
      return "Section: " + getId()
	 + "\ntopic: " + getTopic()
	 + "\nstartTime: " + getStartTime()
	 + "\nendTime: " + getEndTime();
   } // end of toString()

} // end of class Section
