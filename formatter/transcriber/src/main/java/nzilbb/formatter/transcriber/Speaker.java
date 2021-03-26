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
import java.util.Vector;

/**
 * A speaker in a particular transcript.  This class captures data about a speaker
 * specific to a transcript (e.g. their accent as marked in the .trs file, their ID within
 * the transcript, etc.), in addition to cross-transcript data (e.g. their name, and their
 * ONZE Miner database ID). 
 * @author Robert Fromont robert@fromont.net.nz
 */

public class Speaker
{
   // Attributes:

   /**
    * Name of the speaker
    */
   protected String sName = "";
   /**
    * Name accessor - Name of the speaker
    * @return Name of the speaker
    */
   public String getName() { return sName; }
   /**
    * Name mutator - Name of the speaker. Leading or trailing whhitespace is trimmed when the name is set.
    * @param sNewName Name of the speaker
    */
   public void setName(String sNewName) { if (sNewName != null) sName = sNewName.trim(); else sName = null; }
   
   /**
    * Constructor
    * @param theTranscript The transcript to which the speaker belongs
    */
   public Speaker(Transcript theTranscript)
   { 
      setTranscript(theTranscript);
   } // end of constructor
   
   /**
    * Constructor
    * @param theTranscript The transcript to which the speaker belongs
    * @param speakerNode The XML node that defines the speaker
    */
   public Speaker(Transcript theTranscript, Node speakerNode)
   {
      setTranscript(theTranscript);
      try
      {
	 setId(speakerNode.getAttributes().getNamedItem("id").getNodeValue());
      } catch(Exception exception) {}
      try
      {
	 setName(speakerNode.getAttributes().getNamedItem("name").getNodeValue());
      } catch(Exception exception) {}
      if (sName == null || sName.length() == 0)
      {
	 setName("[unknown]");
      }
      try
      {
	 setCheck(speakerNode.getAttributes().getNamedItem("check").getNodeValue());
      } catch(Exception exception) {}
      try
      {
	 setType(speakerNode.getAttributes().getNamedItem("type").getNodeValue());
      } catch(Exception exception) {}
      try
      {
	 setDialect(speakerNode.getAttributes().getNamedItem("dialect").getNodeValue());
      } catch(Exception exception) {}
      try
      {
	 setAccent(speakerNode.getAttributes().getNamedItem("accent").getNodeValue());
      } catch(Exception exception) {}
      try
      {
	 setScope(speakerNode.getAttributes().getNamedItem("scope").getNodeValue());
      } catch(Exception exception) {}
   } // end of constructor
   
   /**
    * ID of the speaker within the transcript
    */
   private String sId = "";
   /**
    * Id accessor - ID of the speaker within the transcript
    * @return ID of the speaker within the transcript
    */
   public String getId() { return sId; }
   /**
    * Id mutator - ID of the speaker within the transcript
    * @param sNewId ID of the speaker within the transcript
    */
   public void setId(String sNewId) { sId = sNewId; }
   
   /**
    * Check attribute from Transcriber
    */
   private String sCheck = "";
   /**
    * Check accessor - Check attribute from Transcriber
    * @return Check attribute from Transcriber
    */
   public String getCheck() { return sCheck; }
   /**
    * Check mutator - Check attribute from Transcriber
    * @param sNewCheck Check attribute from Transcriber
    */
   public void setCheck(String sNewCheck) { sCheck = sNewCheck; }

   /**
    * Type attribute from Transcriber
    */
   private String sType = "";
   /**
    * Type accessor - Type attribute from Transcriber
    * @return Type attribute from Transcriber
    */
   public String getType() { return sType; }
   /**
    * Type mutator - Type attribute from Transcriber
    * @param sNewType Type attribute from Transcriber
    */
   public void setType(String sNewType) { sType = sNewType; }
   
   /**
    * Dialect attribute from Transcriber
    */
   private String sDialect = "";
   /**
    * Dialect accessor - Dialect attribute from Transcriber
    * @return Dialect attribute from Transcriber
    */
   public String getDialect() { return sDialect; }
   /**
    * Dialect mutator - Dialect attribute from Transcriber
    * @param sNewDialect Dialect attribute from Transcriber
    */
   public void setDialect(String sNewDialect) { sDialect = sNewDialect; }
   
   /**
    * Accent attribute from Transcriber
    */
   private String sAccent = "";
   /**
    * Accent accessor - Accent attribute from Transcriber
    * @return Accent attribute from Transcriber
    */
   public String getAccent() { return sAccent; }
   /**
    * Accent mutator - Accent attribute from Transcriber
    * @param sNewAccent Accent attribute from Transcriber
    */
   public void setAccent(String sNewAccent) { sAccent = sNewAccent; }
   
   /**
    * Scope attribute from Transcriber
    */
   private String sScope = "";
   /**
    * Scope accessor - Scope attribute from Transcriber
    * @return Scope attribute from Transcriber
    */
   public String getScope() { return sScope; }
   /**
    * Scope mutator - Scope attribute from Transcriber
    * @param sNewScope Scope attribute from Transcriber
    */
   public void setScope(String sNewScope) { sScope = sNewScope; }
   
   /**
    * Transcript in which the speaker appears
    */
   private Transcript tTranscript;
   /**
    * Transcript accessor - Transcript in which the speaker appears
    * @return Transcript in which the speaker appears
    */
   public Transcript getTranscript() { return tTranscript; }
   /**
    * Transcript mutator - Transcript in which the speaker appears
    * @param tNewTranscript Transcript in which the speaker appears
    */
   public void setTranscript(Transcript tNewTranscript) { tTranscript = tNewTranscript; }
   
   // Methods
   
   /**
    * Text-file representation of the object.
    * @param writer Writer for writing the object
    * @throws java.io.IOException
    */
   public void writeText(Writer writer)
      throws java.io.IOException
   {
      // XML header
      writer.write( 
	 "\n<Speaker id=\"" + getId() + "\" "
	 + "name=\"" + Transcript.xmlEscaped(getName()) + "\" "
	 + "check=\"" + Transcript.xmlEscaped(getCheck()) + "\" "
	 + "type=\"" + Transcript.xmlEscaped(getType()) + "\" "
	 + "dialect=\"" + Transcript.xmlEscaped(getDialect()) + "\" "
	 + "accent=\"" + Transcript.xmlEscaped(getAccent()) + "\" "
	 + "scope=\"" + Transcript.xmlEscaped(getScope()) + "\"/>"); 
   } // writeText
   
   /**
    * String representation of th object.
    */
   public String toString()
   {
      return "Speaker: " + getId() + "\nname: " + getName()
	 + "\ncheck: " + getCheck() + "\ntype: " + getType() 
	 + "\ndialect: " + getDialect() + "\naccent: " + getAccent()
	 + "\nscope: " + getScope();
   } // end of toString()

} // end of class Speaker
