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

import java.util.Vector;
import java.util.HashMap;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import nzilbb.ag.*;
import nzilbb.ag.serialize.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;

/**
 * Deserializer for trs files produced with Transcriber.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class TranscriptDeserializer
   extends Transcript
   implements IDeserializer
{
   // Attributes:
   protected Vector<String> warnings;

   /**
    * Layer schema.
    * @see #getSchema()
    * @see #setSchema(Schema)
    */
   protected Schema schema;
   /**
    * Getter for {@link #schema}: Layer schema.
    * @return Layer schema.
    */
   public Schema getSchema() { return schema; }
   /**
    * Setter for {@link #schema}: Layer schema.
    * @param newSchema Layer schema.
    */
   public void setSchema(Schema newSchema) { schema = newSchema; }
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public TranscriptDeserializer()
   {
   } // end of constructor
   
   // IStreamDeserializer methods:
   
   /**
    * Returns the deserializer's descriptor
    * @return The deserializer's descriptor
    */
   public SerializationDescriptor getDescriptor()
   {
      return new SerializationDescriptor(
	 "Transcriber transcript", "1.42", "text/xml-transcriber", ".trs", getClass().getResource("icon.png"));
   }

   /**
    * Sets parameters for deserializer as a whole.  This might include database connection parameters, locations of supporting files, etc.
    * <p>When the deserializer is installed, this method should be invoked with an empty parameter
    *  set, to discover what (if any) general configuration is required. If parameters are
    *  returned, and user interaction is possible, then the user may be presented with an
    *  interface for setting/confirming these parameters.  Unlike the
    *  {@link #load(NamedStream[],Schema)} method, this always returns th}e required parameters, 
    *  whether or not they are fulfilled.
    * @param configuration The configuration for the deserializer. 
    * @param schema The layer schema, definining layers and the way they interrelate.
    * @return A list of configuration parameters (still) must be set before {@link IDeserializer#setParameters()} can be invoked. If this is an empty list, {@link IDeserializer#setParameters()} can be invoked. If it's not an empty list, this method must be invoked again with the returned parameters' values set.
    */
   public ParameterSet configure(ParameterSet configuration, Schema schema)
   {
      return new ParameterSet(); // TODO configuration for topic, comment, noise, language, lexical, pronounce, entity layers
   }

   /**
    * Loads the serialized form of the graph, using the given set of named streams.
    * @param streams A list of named streams that contain all the
    *  transcription/annotation data required, and possibly (a) stream(s) for the media annotated.
    * @param schema The layer schema, definining layers and the way they interrelate.
    * @return A list of parameters that require setting before {@link IDeserializer#deserialize()} can be invoked. This may be an empty list, and may include parameters with the value already set to a workable default. If there are parameters, and user interaction is possible, then the user may be presented with an interface for setting/confirming these parameters, before they are then passed to {@link IDeserializer#setParameters(ParameterSet)}.
    * @throws SerializationException If the graph could not be loaded.
    * @throws IOException On IO error.
    */
   @SuppressWarnings({"rawtypes", "unchecked"})
   public ParameterSet load(NamedStream[] streams, Schema schema) throws SerializationException, IOException
   {
      reset();

      ParameterSet parameters = new ParameterSet();

      // take the first stream, ignore all others.
      NamedStream trs = Utility.findSingleStream(streams, ".trs", "text/xml-transcriber");
      if (trs == null) throw new SerializationException("No Transciber transcript stream found");
      setId(trs.getName());
      try
      {
	 load(trs.getStream());
      }
      catch(ParserConfigurationException exception)
      {
	 throw new SerializationException(exception);
      }
      catch(SAXException exception)
      {
	 throw new SerializationException(exception);
      }

      setSchema(schema);

      return parameters;
   }

   /**
    * Sets parameters for a given deserialization operation, after loading the serialized form of the graph. This might include mappings from format-specific objects like tiers to graph layers, etc.
    * @param parameters The configuration for a given deserialization operation.
    * @throws SerializationParametersMissingException If not all required parameters have a value.
    */
   public void setParameters(ParameterSet parameters) throws SerializationParametersMissingException
   {
      // TODO
   }

   /**
    * Deserializes the serialized data, generating one or more {@link Graph}s.
    * <p>Many data formats will only yield one graph (e.g. Transcriber
    * transcript or Praat textgrid), however there are formats that
    * are capable of storing multiple transcripts in the same file
    * (e.g. AGTK, Transana XML export), which is why this method
    * returns a list.
    * @return A list of valid (if incomplete) {@link Graph}s. 
    * @throws SerializerNotConfiguredException if the object has not been configured.
    * @throws SerializationParametersMissingException if the parameters for this particular graph have not been set.
    * @throws SerializationException if errors occur during deserialization.
    */
   public Graph[] deserialize() 
      throws SerializerNotConfiguredException, SerializationParametersMissingException, SerializationException
   {
      validate();

      Graph graph = new Graph();
      graph.setId(getId());
      // creat the 0 anchor to prevent graph tagging from creating one with no confidence
      graph.getOrCreateAnchorAt(0.0, Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC);

      // add layers to the graph
      // we don't just copy the whole schema, because that would imply that all the extra layers
      // contained no annotations, which is not necessarily true
      graph.addLayer((Layer)schema.getParticipantLayer().clone());
      graph.getSchema().setParticipantLayerId(schema.getParticipantLayerId());
      graph.addLayer((Layer)schema.getTurnLayer().clone());
      graph.getSchema().setTurnLayerId(schema.getTurnLayerId());
      graph.addLayer((Layer)schema.getUtteranceLayer().clone());
      graph.getSchema().setUtteranceLayerId(schema.getUtteranceLayerId());
      graph.addLayer((Layer)schema.getWordLayer().clone());
      graph.getSchema().setWordLayerId(schema.getWordLayerId());
      String[] otherLayers = {
	 "topic", "language", "lexical", "pronounce", "entities", "comment", "noise", 
	 "transcript_scribe", "transcript_version", "transcript_program", "transcript_air_date", "transcript_language",
	 "participant_check", "participant_type", "participant_dialect", "participant_accent", "participant_scope"
      }; // TODO do this with configuration
      for (String layerId : otherLayers)
      {
	 if (schema.getLayer(layerId) != null)
	 {
	    graph.addLayer((Layer)schema.getLayer(layerId).clone());
	 }
      } // next other layer

      // attributes
      if (getScribe().length() > 0 && graph.getLayer("transcript_scribe") != null)
      {
	 graph.createTag(graph, "transcript_scribe", getScribe());
      }
      if (getVersion().length() > 0 && graph.getLayer("transcript_version") != null)
      {
	 graph.createTag(graph, "transcript_version", getVersion());
      }
      if (getProgram().length() > 0 && graph.getLayer("transcript_program") != null)
      {
	 graph.createTag(graph, "transcript_program", getProgram());
      }
      if (getAirDate().length() > 0 && graph.getLayer("transcript_air_date") != null)
      {
	 graph.createTag(graph, "transcript_air_date", getAirDate());
      }
      if (getLanguage().length() > 0 && graph.getLayer("transcript_language") != null)
      {
	 graph.createTag(graph, "transcript_language", getLanguage());
      }

      // participants
      for (Speaker speaker : getSpeakers())
      {
	 Annotation participant = new Annotation(
	    speaker.getId(), 
	    speaker.getName(), 
	    schema.getParticipantLayerId());
	 participant.setParentId(graph.getId());
	 graph.addAnnotation(participant);

	 if (speaker.getCheck().length() > 0 && graph.getLayer("participant_check") != null)
	 {
	    graph.createTag(participant, "participant_check", speaker.getCheck());
	 }
	 if (speaker.getType().length() > 0 && graph.getLayer("participant_type") != null)
	 {
	    graph.createTag(participant, "participant_type", speaker.getType());
	 }
	 if (speaker.getDialect().length() > 0 && graph.getLayer("participant_dialect") != null)
	 {
	    graph.createTag(participant, "participant_dialect", speaker.getDialect());
	 }
	 if (speaker.getAccent().length() > 0 && graph.getLayer("participant_accent") != null)
	 {
	    graph.createTag(participant, "participant_accent", speaker.getAccent());
	 }
	 if (speaker.getScope().length() > 0 && graph.getLayer("participant_scope") != null)
	 {
	    graph.createTag(participant, "participant_scope", speaker.getScope());
	 }
      }

      // graph
      HashMap<String,Annotation> htStartedEvents = new HashMap<String,Annotation>();
      
      // for each section
      for (Section section : getSections())
      {
	 Annotation anTopic = null;
	 String sTopic = section.getTopicName();
	 if (sTopic != null && sTopic.length() > 0)
	 {
	    anTopic = new Annotation(null, sTopic, "topic");
	    anTopic.setStart(
	       graph.getOrCreateAnchorAt(
		  section.getStartTimeAsDouble(), Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC));
	    anTopic.setEnd(
	       graph.getOrCreateAnchorAt(
		  section.getEndTimeAsDouble(), Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC));
	    graph.addAnnotation(anTopic);
	 }
	 
	 // for each turn
	 for (Turn turn : section.getTurns())
	 {
	    // could be simultaneous speech so we need a speakerId lookup
	    HashMap<String, Annotation> htTurnAnnotations = new HashMap<String, Annotation>();
	    
	    // turn.getSpeakerId() is something like "spk1" or "spk2 spk1"
	    for (String sSpeakerId : turn.getSpeakerId().split(" "))
	    {
	       String label = sSpeakerId;
	       if (getSpeaker(sSpeakerId) != null) label = getSpeaker(sSpeakerId).getName();
	       Annotation anTurn = new Annotation(null, label, schema.getTurnLayerId());
	       anTurn.setParentId(graph.getAnnotation(sSpeakerId).getId());
	       anTurn.setStart(
		  graph.getOrCreateAnchorAt(
		     turn.getStartTimeAsDouble(), Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC));
	       anTurn.setEnd(
		  graph.getOrCreateAnchorAt(
		     turn.getEndTimeAsDouble(), Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC));
	       htTurnAnnotations.put(sSpeakerId, anTurn);
	       graph.addAnnotation(anTurn);
	    }
	    
	    // for each sync
	    for(Sync sync : turn.getSyncs())
	    {
	       Vector<Sync> vSubSyncs = new Vector<Sync>();
	       if (sync.isSimultaneousSpeech())
	       {
		  // whole bunch of syncs at once
		  vSubSyncs.addAll(sync.getSimultaneousSyncs());
	       }
	       else
	       {
		  // just one sync - this one
		  vSubSyncs.addElement(sync);
	       }
	       
	       for (Sync thisSync : vSubSyncs)
	       {
		  // lookup the turn annotation
		  Annotation anTurn = htTurnAnnotations.get(thisSync.getWho());
		  Annotation anLine = new Annotation(
		     null, anTurn.getLabel(), schema.getUtteranceLayerId());
		  anLine.setParentId(anTurn.getId());
		  anLine.setStart(
		     graph.getOrCreateAnchorAt(
			thisSync.getTimeAsDouble(), Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC));
		  graph.addAnnotation(anLine);
		  
		  // force Events to assign to their given Words where poss.
		  thisSync.getWords();
		  
		  Vector<Annotation> vWordlessEvents = new Vector<Annotation>();
		  
		  // events that have no word
		  for (Event event : thisSync.getEvents())
		  {
		     if (event.getWord() == null)
		     {
			Annotation anEvent = new Annotation();
			if (event.getType().equals("noise"))
			{
			   anEvent.setLayerId("noise");
			}
			else if (event.getType().equals("lexical"))
			{
			   vWordlessEvents.add(anEvent);
			   anEvent.setLayerId("lexical");
			}
			else if (event.getType().equals("pronounce"))
			{
			   vWordlessEvents.add(anEvent);
			   anEvent.setLayerId("pronounce");
			}
			else if (event.getType().equals("language"))
			{
			   anEvent.setLayerId("language");
			}
			else if (event.getType().equals("entities"))
			{
			   anEvent.setParentId(anTurn.getId());
			   anEvent.setLayerId("entity");
			}
			else if (event.getType().equals("comment"))
			{
			   anEvent.setParentId(graph.getId());
			   anEvent.setLayerId("comment");
			}
			anEvent.setLabel(event.getDescription());
			
			if (event.getExtent().equals("begin"))
			{
			   // set the start anchor
			   anEvent.setStartId(anLine.getStartId());
			   // and save it until we find the end...
			   htStartedEvents.put(
			      event.getType() + ":" + event.getDescription(), anEvent);
			}
			else if (event.getExtent().equals("end"))
			{
			   // ditch this anchor, use the one created at the
			   // beginning instead:
			   anEvent = htStartedEvents.remove(
			      event.getType() + ":" + event.getDescription());
			   if (anEvent != null)
			   {
			      anEvent.setEnd(anLine.getStart());
			      graph.addAnnotation(anEvent);
			   }
			}
			else // put it at the beginning of the line
			{
			   anEvent.setStart(anLine.getStart());
			   anEvent.setEnd(anLine.getStart());
			   graph.addAnnotation(anEvent);
			}
			
		     }  // event has no word
		  } // next sync event
		  
		  // words
		  Annotation lastWord = null;
		  for (Word word : thisSync.getWords())
		  {
		     Annotation anWord = new Annotation(null, word.getRawOrthography(), schema.getWordLayerId());
		     anWord.setParentId(anTurn.getId());
		     graph.addAnnotation(anWord);
		     if (lastWord == null) anWord.setStartId(anLine.getStartId());
		     lastWord = anWord;
		     
		     // line-start events that need a word set?
		     for (Annotation anEvent : vWordlessEvents)
		     {
			anEvent.setParentId(anWord.getId());
		     } // next wordless event
		     vWordlessEvents.clear();
		     
		     // events assigned to this word
		     for (Event event : word.getEvents())
		     {
			Annotation anEvent = new Annotation();
			if (event.getType().equals("noise"))
			{
			   anEvent.setLayerId("noise");
			   anEvent.setParentId(graph.getId());
			}
			else if (event.getType().equals("lexical"))
			{
			   anEvent.setLayerId("lexical");
			   anEvent.setParentId(anWord.getId());
			}
			else if (event.getType().equals("pronounce"))
			{
			   anEvent.setLayerId("pronounce");
			   anEvent.setParentId(anWord.getId());
			}
			else if (event.getType().equals("language"))
			{
			   anEvent.setLayerId("language");
			   anEvent.setParentId(anTurn.getId());
			}
			else if (event.getType().equals("entities"))
			{
			   anEvent.setParentId(anTurn.getId());
			   anEvent.setLayerId("entities");
			}
			else if (event.getType().equals("comment"))
			{
			   anEvent.setParentId(anTurn.getId());
			   anEvent.setLayerId("comment");
			}
			anEvent.setLabel(event.getDescription());
			if (event.getExtent().equals("begin"))
			{
			   // set the start anchor
			   anEvent.setStart(anWord.getEnd());
			   // and save it until we find the end...
			   htStartedEvents.put(
			      event.getType() + ":" + event.getDescription(), anEvent);
			}
			else if (event.getExtent().equals("end"))
			{
			   // ditch this anchor, use the one created at the
			   // beginning instead:
			   anEvent = htStartedEvents.remove(
			      event.getType() + ":" + event.getDescription());
			   if (anEvent != null)
			   {
			      anEvent.setEnd(anWord.getEnd());
			      graph.addAnnotation(anEvent);
			   }
			}
			else if (event.getExtent().equals("instantaneous"))
			{
			   anEvent.setStart(anWord.getEnd());
			   anEvent.setEnd(anWord.getEnd());
			   graph.addAnnotation(anEvent);
			}
			else // wrap around the current word
			{
			   anEvent.setStart(anWord.getStart());
			   anEvent.setEnd(anWord.getEnd());
			   graph.addAnnotation(anEvent);
			}
		     }
		  } // next word
		  
		  anLine.setEnd(
		     graph.getOrCreateAnchorAt(
			thisSync.getEndTimeAsDouble(), Constants.CONFIDENCE, Constants.CONFIDENCE_AUTOMATIC));
		  
		  if (lastWord != null)
		  {
		     lastWord.setEnd(anLine.getEnd());
		  }
	       } // next sub-sync
	    } // next sync
	 } // next turn	 
      } // next section
      
      // check for leftover events
      if (htStartedEvents.size() != 0)
      {
	 Anchor lastAnchor = graph.getEnd();
	 for (Annotation anEvent : htStartedEvents.values())
	 {
	    anEvent.setEnd(lastAnchor);
	    graph.addAnnotation(anEvent);
	 } // next event
      }

      Graph[] graphs = { graph };
      return graphs;
   }

   /**
    * Returns any warnings that may have arisen during the last execution of {@link #deserialize()}.
    * @return A possibly empty lilayersst of warnings.
    */
   public String[] getWarnings()
   {
      return warnings.toArray(new String[0]);
   }

   /**
    * Validates the input and returns a list of errors that would
    * prevent the input from being converted into an {@link AnnotationGraph}
    * when {@link #toAnnotationGraphs(LinkedHashMap)} is called.
    * <p>This implementation checks for simultaneous speaker turns that have the same speaker mentioned more than once, speakers that have the same name, and mismatched start/end events.
    * @return A list of errors, which will be empty if there were no validation errors.
    */
   public Vector<String> validate()
   {     
      warnings = new Vector<String>();
      // check there are speakers
      if (getSpeakers().size() == 0)
      {
	 warnings.addElement("Transcript contains  no speakers.");
      }
      
      // check for two speakers with the same name
      Vector<String> vSpeakers = new Vector<String>();
      for (Speaker speaker : getSpeakers())
      {
	 if (vSpeakers.contains(speaker.getName()))
	 {
	    warnings.addElement("Transcript contains more than one speaker called \"" + speaker.getName() +"\".");
	 }
	 else
	 {
	    vSpeakers.addElement(speaker.getName());
	 }
      } // next speaker
      
      // check for simultaneous speech turns where the speakers are the same
      // and for mismatched start/end events
      
      // for each turn
      HashMap<String,Event> htStartedEvents = new HashMap<String,Event>();
      for (Turn turn : getTurns())
      {
	 // for each sync
	 for (Sync sync : turn.getSyncs())
	 {
	    if (sync.isSimultaneousSpeech())
	    {
	       // check for speaker speaking at the same time as themselves
	       Vector<String> vSimSpeakers = new Vector<String>();
	       for (SimultaneousSync sim : sync.getSimultaneousSyncs())
	       {
		  if (vSimSpeakers.contains(sim.getWho()))
		  {
		     warnings.addElement(
			"Simultaneous speech at " + sync.getTime()
			+"s contains the same speaker more than once.");
		  }
		  else
		  {
		     vSimSpeakers.addElement(sim.getWho());
		  }
	       } // next sync
	       if (vSimSpeakers.size() < 2)
	       {
		  warnings.addElement(
		     "Simultaneous speech at " + sync.getTime()
		     +"s doesn't contain multiple speakers.");
	       }
	    } // simultaneous speech
	    else if (sync.getWho().indexOf(' ') >= 0)
	    {
	       warnings.addElement(
		  "Simultaneous speech at " + sync.getTime()
		  +"s doesn't contain multiple speakers.");
	    }
	    
	    // check events
	    for (Event event : sync.getEvents())
	    {		  
	       if (event.getExtent().equals("begin"))
	       {
		  // and save it until we find the end...
		  htStartedEvents.put(
		     event.getType() + ":" + event.getDescription(),
		     event);
	       }
	       else if (event.getExtent().equals("end"))
	       {
		  // does this have a beginning?
		  if (htStartedEvents.containsKey(
			 event.getType()
			 + ":" + event.getDescription()))
		  {
		     htStartedEvents.remove(
			event.getType()
			+ ":" + event.getDescription());
		  }
		  else
		  {
		     warnings.add(
			"End event after " + sync.getTime()
			+"s has no start tag: " + event);
		  }
	       }
	    }
	 } // next sync
      } // next turn
      
      // leftover start events?
      for(String sKey : htStartedEvents.keySet())
      {
	 Event event = htStartedEvents.get(sKey);
	 String sMessage = "Start event has no end tag: " + event;
	 if (event.getWord() != null)
	 {
	    sMessage = "Start event after " 
	       + event.getWord().getSync().getTime()
	       +"s has no end tag: " + event;
	 }
	 warnings.add(sMessage);
      } // next leftover start event
      return warnings;
   }

} // end of class TranscriptDeserializer
