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
package nzilbb.clan;

import java.util.Vector;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import nzilbb.ag.*;
import nzilbb.ag.util.SimpleTokenizer;
import nzilbb.ag.util.ConventionTransformer;
import nzilbb.ag.util.SpanningConventionTransformer;
import nzilbb.ag.serialize.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;

/**
 * Deserializer for CHAT files produced by CLAN.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class ChatDeserializer
  implements IStreamDeserializer
{
   // Attributes:
   protected Vector<String> warnings;

   
   /**
    * Name of the .cha file.
    * @see #getName()
    * @see #setName(String)
    */
   protected String name;
   /**
    * Getter for {@link #name}: Name of the .cha file.
    * @return Name of the .cha file.
    */
   public String getName() { return name; }
   /**
    * Setter for {@link #name}: Name of the .cha file.
    * @param newName Name of the .cha file.
    */
   public void setName(String newName) { name = newName; }

   
   /**
    * List of languages.
    * @see #getLanguages()
    * @see #setLanguages(Vector<String>)
    */
   protected Vector<String> languages;
   /**
    * Getter for {@link #languages}: List of languages.
    * @return List of languages.
    */
   public Vector<String> getLanguages() { return languages; }
   /**
    * Setter for {@link #languages}: List of languages.
    * @param newLanguages List of languages.
    */
   public void setLanguages(Vector<String> newLanguages) { languages = newLanguages; }
   
   /**
    * Map of participant IDs to properties.
    * @see #getParticipants()
    * @see #setParticipants(HashMap)
    */
   protected HashMap<String,HashMap<String,String>> participants;
   /**
    * Getter for {@link #participants}: Map of participant IDs to properties.
    * @return Map of participant IDs to properties.
    */
   public HashMap<String,HashMap<String,String>> getParticipants() { return participants; }
   /**
    * Setter for {@link #participants}: Map of participant IDs to properties.
    * @param newParticipants Map of participant IDs to properties.
    */
   public void setParticipants(HashMap<String,HashMap<String,String>> newParticipants) { participants = newParticipants; }

   
   /**
    * Transcript lines.
    * @see #getLines()
    * @see #setLines(Vector)
    */
   protected Vector<String> lines;
   /**
    * Getter for {@link #lines}: Transcript lines.
    * @return Transcript lines.
    */
   public Vector<String> getLines() { return lines; }
   /**
    * Setter for {@link #lines}: Transcript lines.
    * @param newLines Transcript lines.
    */
   public void setLines(Vector<String> newLines) { lines = newLines; }

   
   /**
    * Header lines
    * @see #getHeaders()
    * @see #setHeaders(Vector<String>)
    */
   protected Vector<String> headers;
   /**
    * Getter for {@link #headers}: Header lines
    * @return Header lines
    */
   public Vector<String> getHeaders() { return headers; }
   /**
    * Setter for {@link #headers}: Header lines
    * @param newHeaders Header lines
    */
   public void setHeaders(Vector<String> newHeaders) { headers = newHeaders; }

   
   /**
    * Name of media file.
    * @see #getMediaName()
    * @see #setMediaName(String)
    */
   protected String mediaName;
   /**
    * Getter for {@link #mediaName}: Name of media file.
    * @return Name of media file.
    */
   public String getMediaName() { return mediaName; }
   /**
    * Setter for {@link #mediaName}: Name of media file.
    * @param newMediaName Name of media file.
    */
   public void setMediaName(String newMediaName) { mediaName = newMediaName; }

   /**
    * Type of media.
    * @see #getMediaType()
    * @see #setMediaType(String)
    */
   protected String mediaType;
   /**
    * Getter for {@link #MediaType}: Type of media.
    * @return Type of media.
    */
   public String getMediaType() { return mediaType; }
   /**
    * Setter for {@link #MediaType}: Type of media.
    * @param newMediaType Type of media.
    */
   public void setMediaType(String newMediaType) { mediaType = newMediaType; }

   /**
    * List of names of transcribers.
    * @see #getTranscribers()
    * @see #setTranscribers(Vector<String>)
    */
   protected Vector<String> transcribers;
   /**
    * Getter for {@link #transcribers}: List of names of transcribers.
    * @return List of names of transcribers.
    */
   public Vector<String> getTranscribers() { return transcribers; }
   /**
    * Setter for {@link #transcribers}: List of names of transcribers.
    * @param newTranscribers List of names of transcribers.
    */
   public void setTranscribers(Vector<String> newTranscribers) { transcribers = newTranscribers; }
   
   /**
    * Map of layer IDs to layers.
    * @see #getIdToLayer()
    * @see #setIdToLayer(HashMap<String,Layer>)
    */
   protected HashMap<String,Layer> idToLayer;
   /**
    * Getter for {@link #idToLayer}: Map of layer IDs to layers.
    * @return Map of layer IDs to layers.
    */
   public HashMap<String,Layer> getIdToLayer() { return idToLayer; }
   /**
    * Setter for {@link #idToLayer}: Map of layer IDs to layers.
    * @param newIdToLayer Map of layer IDs to layers.
    */
   public void setIdToLayer(HashMap<String,Layer> newIdToLayer) { idToLayer = newIdToLayer; }


   /**
    * Layer ID for participant layer.
    * @see #getParticipantLayerId()
    * @see #setParticipantLayerId(String)
    */
   protected String participantLayerId;
   /**
    * Getter for {@link #participantLayerId}: Layer ID for participant layer.
    * @return Layer ID for participant layer.
    */
   public String getParticipantLayerId() { return participantLayerId; }
   /**
    * Setter for {@link #participantLayerId}: Layer ID for participant layer.
    * @param newParticipantLayerId Layer ID for participant layer.
    */
   public void setParticipantLayerId(String newParticipantLayerId) { participantLayerId = newParticipantLayerId; }
   
   /**
    * Layer ID for turn layer.
    * @see #getTurnLayerId()
    * @see #setTurnLayerId(String)
    */
   protected String turnLayerId;
   /**
    * Getter for {@link #turnLayerId}: Layer ID for turn layer.
    * @return Layer ID for turn layer.
    */
   public String getTurnLayerId() { return turnLayerId; }
   /**
    * Setter for {@link #turnLayerId}: Layer ID for turn layer.
    * @param newTurnLayerId Layer ID for turn layer.
    */
   public void setTurnLayerId(String newTurnLayerId) { turnLayerId = newTurnLayerId; }

   /**
    * Layer ID for utterance layer.
    * @see #getUtteranceLayerId()
    * @see #setUtteranceLayerId(String)
    */
   protected String utteranceLayerId;
   /**
    * Getter for {@link #utteranceLayerId}: Layer ID for utterance layer.
    * @return Layer ID for utterance layer.
    */
   public String getUtteranceLayerId() { return utteranceLayerId; }
   /**
    * Setter for {@link #utteranceLayerId}: Layer ID for utterance layer.
    * @param newUtteranceLayerId Layer ID for utterance layer.
    */
   public void setUtteranceLayerId(String newUtteranceLayerId) { utteranceLayerId = newUtteranceLayerId; }

   /**
    * Layer ID for word layer.
    * @see #getWordLayerId()
    * @see #setWordLayerId(String)
    */
   protected String wordLayerId;
   /**
    * Getter for {@link #wordLayerId}: Layer ID for word layer.
    * @return Layer ID for word layer.
    */
   public String getWordLayerId() { return wordLayerId; }
   /**
    * Setter for {@link #wordLayerId}: Layer ID for word layer.
    * @param newWordLayerId Layer ID for word layer.
    */
   public void setWordLayerId(String newWordLayerId) { wordLayerId = newWordLayerId; }

   /**
    * Layer ID for disfluency layer.
    * @see #getDisfluencyLayerId()
    * @see #setDisfluencyLayerId(String)
    */
   protected String disfluencyLayerId;
   /**
    * Getter for {@link #disfluencyLayerId}: Layer ID for disfluency layer.
    * @return Layer ID for disfluency layer.
    */
   public String getDisfluencyLayerId() { return disfluencyLayerId; }
   /**
    * Setter for {@link #disfluencyLayerId}: Layer ID for disfluency layer.
    * @param newDisfluencyLayerId Layer ID for disfluency layer.
    */
   public void setDisfluencyLayerId(String newDisfluencyLayerId) { disfluencyLayerId = newDisfluencyLayerId; }

   /**
    * Layer ID for expansion layer.
    * @see #getExpansionLayerId()
    * @see #setExpansionLayerId(String)
    */
   protected String expansionLayerId;
   /**
    * Getter for {@link #expansionLayerId}: Layer ID for expansion layer.
    * @return Layer ID for expansion layer.
    */
   public String getExpansionLayerId() { return expansionLayerId; }
   /**
    * Setter for {@link #expansionLayerId}: Layer ID for expansion layer.
    * @param newExpansionLayerId Layer ID for expansion layer.
    */
   public void setExpansionLayerId(String newExpansionLayerId) { expansionLayerId = newExpansionLayerId; }

   /**
    * Layer ID for completion layer.
    * @see #getCompletionLayerId()
    * @see #setCompletionLayerId(String)
    */
   protected String completionLayerId;
   /**
    * Getter for {@link #completionLayerId}: Layer ID for completion layer.
    * @return Layer ID for completion layer.
    */
   public String getCompletionLayerId() { return completionLayerId; }
   /**
    * Setter for {@link #completionLayerId}: Layer ID for completion layer.
    * @param newCompletionLayerId Layer ID for completion layer.
    */
   public void setCompletionLayerId(String newCompletionLayerId) { completionLayerId = newCompletionLayerId; }
   
   /**
    * Layer ID for Gems
    * @see #getGemLayerId()
    * @see #setGemLayerId(String)
    */
   protected String gemLayerId;
   /**
    * Getter for {@link #gemLayerId}: Layer ID for Gems
    * @return Layer ID for Gems
    */
   public String getGemLayerId() { return gemLayerId; }
   /**
    * Setter for {@link #gemLayerId}: Layer ID for Gems
    * @param newGemLayerId Layer ID for Gems
    */
   public void setGemLayerId(String newGemLayerId) { gemLayerId = newGemLayerId; }

   
   /**
    * Layer ID for transcriber graph attributes.
    * @see #getTranscriberLayerId()
    * @see #setTranscriberLayerId(String)
    */
   protected String transcriberLayerId;
   /**
    * Getter for {@link #transcriberLayerId}: Layer ID for transcriber graph attributes.
    * @return Layer ID for transcriber graph attributes.
    */
   public String getTranscriberLayerId() { return transcriberLayerId; }
   /**
    * Setter for {@link #transcriberLayerId}: Layer ID for transcriber graph attributes.
    * @param newTranscriberLayerId Layer ID for transcriber graph attributes.
    */
   public void setTranscriberLayerId(String newTranscriberLayerId) { transcriberLayerId = newTranscriberLayerId; }

   /**
    * Layer ID for graph language.
    * @see #getLanguagesLayerId()
    * @see #setLanguagesLayerId(String)
    */
   protected String languagesLayerId;
   /**
    * Getter for {@link #languagesLayerId}: Layer ID for graph language.
    * @return Layer ID for graph language.
    */
   public String getLanguagesLayerId() { return languagesLayerId; }
   /**
    * Setter for {@link #languagesLayerId}: Layer ID for graph language.
    * @param newLanguagesLayerId Layer ID for graph language.
    */
   public void setLanguagesLayerId(String newLanguagesLayerId) { languagesLayerId = newLanguagesLayerId; }

   /**
    * Required participant meta-data layers.
    * @see #getParticipantLayerIds()
    * @see #setParticipantLayerIds(HashSet)
    */
   protected HashMap<String,String> participantLayerIds;
   /**
    * Getter for {@link #participantLayerIds}: Required participant meta-data layers.
    * @return Required participant meta-data layers.
    */
   public HashMap<String,String> getParticipantLayerIds() { return participantLayerIds; }
   /**
    * Setter for {@link #participantLayerIds}: Required participant meta-data layers.
    * @param newParticipantLayerIds Required participant meta-data layers.
    */
   public void setParticipantLayerIds(HashMap<String,String> newParticipantLayerIds) { participantLayerIds = newParticipantLayerIds; }
   
   /**
    * Utterance tokenizer.  The default is {@link SimpleTokenizer}.
    * @see #getTokenizer()
    * @see #setTokenizer(IGraphTransformer)
    */
   protected IGraphTransformer tokenizer;
   /**
    * Getter for {@link #tokenizer}: Utterance tokenizer.
    * @return Utterance tokenizer.
    */
   public IGraphTransformer getTokenizer() { return tokenizer; }
   /**
    * Setter for {@link #tokenizer}: Utterance tokenizer.
    * @param newTokenizer Utterance tokenizer.
    */
   public void setTokenizer(IGraphTransformer newTokenizer) { tokenizer = newTokenizer; }

   
   // Methods:
   
   /**
    * Default constructor.
    */
   public ChatDeserializer()
   {
   } // end of constructor
   
   /**
    * Resete state.
    */
   public void reset()
   {
      warnings = new Vector<String>();
      languages = new Vector<String>();
      mediaName = null;
      mediaType = null;
      participants = new HashMap<String,HashMap<String,String>>();
      transcribers = new Vector<String>();
      lines = new Vector<String>();
      headers = new Vector<String>();
      idToLayer = new HashMap<String,Layer>();
      participantLayerId = null;
      turnLayerId = null;
      utteranceLayerId = null;
      wordLayerId = null;
      gemLayerId = null;
      transcriberLayerId = null;
      languagesLayerId = null;
      expansionLayerId = null;
      completionLayerId = null;
      disfluencyLayerId = null;
      participantLayerIds = new HashMap<String,String>();
   } // end of reset()

   // IStreamDeserializer methods:

   /**
    * Returns the deserializer's descriptor
    * @return The deserializer's descriptor
    */
   public SerializationDescriptor getDescriptor()
   {
      return new SerializationDescriptor("CLAN CHAT transcript", "0.1", "text/x-chat");
   }

   /**
    * Sets parameters for deserializer as a whole.  This might include database connection parameters, locations of supporting files, etc.
    * @param configuration The configuration for the deserializer. 
    * @throws DeserializerNotConfiguredException If the configuration is not sufficient for deserialization.
    */
   public void configure(ParameterSet configuration) throws DeserializerNotConfiguredException
   {
   }

   /**
    * Loads the serialized form of the graph, using the given set of named streams.
    * @param annotationStreams A list of named streams that contain all the transcription/annotation data required.
    * @param mediaStreams An optional (may be null) list of named streams that contain the media annotated by the <var>annotationStreams</var>.
    * @return A list of parameters that require setting before {@link IDeserializer#deserialize()} can be invoked. This may be an empty list, and may include parameters with the value already set to a workable default. If there are parameters, and user interaction is possible, then the user may be presented with an interface for setting/confirming these parameters, before they are then passed to {@link IDeserializer#setParameters(ParameterSet)}.
    * @throws Exception If the stream could not be loaded.
    */
   public ParameterSet load(NamedStream[] annotationStreams, NamedStream[] mediaStreams, Layer[] layers) throws Exception
   {
      ParameterSet parameters = new ParameterSet();

      // take the first stream, ignore all others.
      NamedStream cha = annotationStreams[0];
      setName(cha.getName());

      reset();
      for (Layer layer : layers) // TODO meta data for graph and participants
      {
	 idToLayer.put(layer.getId(), (Layer)layer.clone());
	 if (layer.containsKey("@participantLayer")) // TODO formalise this convention
	 {
	    setParticipantLayerId(layer.getId());
	 }
	 if (layer.containsKey("@turnLayer")) // TODO formalise this convention
	 {
	    setTurnLayerId(layer.getId());
	 }
	 if (layer.containsKey("@utteranceLayer")) // TODO formalise this convention
	 {
	    setUtteranceLayerId(layer.getId());
	 }
	 if (layer.containsKey("@wordLayer")) // TODO formalise this convention
	 {
	    setWordLayerId(layer.getId());
	 }
      } // next layer

      boolean disfluenciesFound = false;
      Pattern regexDisfluency = Pattern.compile("&\\p{Alnum}");
      boolean expansionsFound = false;
      Pattern regexExpansion = Pattern.compile("\\[: ");
      boolean completionsFound = false;
      Pattern regexCompletion = Pattern.compile("\\(\\p{Alnum}+\\)");
      boolean gemsFound = false;
      boolean transcribersFound = false;
      boolean languagesFound = false;

      // read stream line by line
      boolean inHeader = true;
      BufferedReader reader = new BufferedReader(new InputStreamReader(cha.getStream(), "UTF-8"));
      String line = reader.readLine();
      while (line != null)
      {
	 if (line.startsWith("@G") || line.startsWith("@Bg") || line.startsWith("*"))
	 {
	    inHeader = false;
	    if (!gemsFound && 
		(line.startsWith("@G") || line.startsWith("@Bg")))
	    {
	       gemsFound = true;
	    }
	 }

	 if (inHeader)
	 {
	    if (line.startsWith("@"))
	    { // new header
	       headers.add(line);
	    }
	    else
	    { // continuation of last header line
	       // remove the last header
	       String lastHeader = headers.remove(headers.size()-1);
	       // append this line to it
	       headers.add(lastHeader + " " + line.trim());
	    }
	 }
	 else
	 { // transcript line
	    lines.add(line);
	    if (!disfluenciesFound)
	    {
	       if (regexDisfluency.matcher(line).find())
	       {
		  disfluenciesFound = true;
	       }
	    }
	    if (!expansionsFound)
	    {
	       if (regexExpansion.matcher(line).find())
	       {
		  expansionsFound = true;
	       }
	    }
	    if (!completionsFound)
	    {
	       if (regexCompletion.matcher(line).find())
	       {
		  completionsFound = true;
	       }
	    }
	 }

	 line = reader.readLine();
      } // next line

      for (String header : headers)
      {
	 if (header.startsWith("@"))
	 { // @ line
	    int iColon = header.indexOf(':');
	    if (iColon >= 0)
	    { // it's a key/value pair
	       String value = header.substring(iColon + 1).trim();
	       if (header.startsWith("@Languages:"))
	       {
		  if (value.trim().length() > 0)
		  {
		     languagesFound = true;
		     StringTokenizer tokens = new StringTokenizer(value, ", ");
		     while (tokens.hasMoreTokens())
		     {
			languagesFound = true;
			languages.add(tokens.nextToken());
		     }
		  }
	       }
	       else if (header.startsWith("@Participants:"))
	       {
		  // something like:
		  // @Participants:	SUB 2001 Participant, EXA Investigator Investigator
		  StringTokenizer tokens = new StringTokenizer(value, ",");
		  while (tokens.hasMoreTokens())
		  {
		     String sParticipant = tokens.nextToken();
		     StringTokenizer participantTokens = new StringTokenizer(sParticipant.trim());
		     if (participantTokens.hasMoreTokens())
		     {
			String id = participantTokens.nextToken();
			// ensure they're in the participants map
			if (!participants.containsKey(id))
			{
			   participants.put(id, new HashMap<String,String>());
			}
			if (participantTokens.hasMoreTokens())
			{
			   String name = participantTokens.nextToken();
			   participants.get(id).put("name", name);
			   if (participantTokens.hasMoreTokens())
			   {
			      String role = participantTokens.nextToken();
			      participants.get(id).put("role", role);
			      participantLayerIds.put("role", null);
			   }
			}
		     }
		  }
	       }
	       else if (header.startsWith("@ID:"))
	       {		  
		  // @ID: language|corpus|code|age|sex|group|SES|role|education|custom|
		  String[] tokens = value.split("\\|");
		  String language = tokens.length<=0?"":tokens[0];
		  String corpus = tokens.length<=1?"":tokens[1];
		  String code = tokens.length<=2?"":tokens[2];
		  String age = tokens.length<=3?"":tokens[3];
		  String sex = tokens.length<=4?"":tokens[4];
		  String group = tokens.length<=5?"":tokens[5];
		  String SES = tokens.length<=6?"":tokens[6];
		  String role = tokens.length<=7?"":tokens[7];
		  String education = tokens.length<=8?"":tokens[8];
		  String custom = tokens.length<=9?"":tokens[9];
		  // ensure they're in the participants map
		  if (!participants.containsKey(code))
		  {
		     participants.put(code, new HashMap<String,String>());
		  }
		  // set the attribute values and make sure we ask for layers if there are values
		  if (language.length() > 0 && !participantLayerIds.containsKey("language"))
		  {
		     participantLayerIds.put("language", null);
		  }
		  participants.get(code).put("language", language);
		  if (corpus.length() > 0 && !participantLayerIds.containsKey("corpus"))
		  {
		     participantLayerIds.put("corpus", null);
		  }
		  participants.get(code).put("corpus", corpus);
		  if (age.length() > 0 && !participantLayerIds.containsKey("age"))
		  {
		     participantLayerIds.put("age", null);
		  }
		  participants.get(code).put("age", age);
		  if (sex.length() > 0 && !participantLayerIds.containsKey("sex"))
		  {
		     participantLayerIds.put("sex", null);
		  }
		  participants.get(code).put("sex", sex);
		  if (group.length() > 0 && !participantLayerIds.containsKey("group"))
		  {
		     participantLayerIds.put("group", null);
		  }
		  participants.get(code).put("group", group);
		  if (SES.length() > 0 && !participantLayerIds.containsKey("SES"))
		  {
		     participantLayerIds.put("SES", null);
		  }
		  participants.get(code).put("SES", SES);
		  if (role.length() > 0 && !participantLayerIds.containsKey("role"))
		  {
		     participantLayerIds.put("role", null);
		     //TODO participants.get(code).put("role", role);
		  }
		  participants.get(code).put("role", role);
		  if (education.length() > 0 && !participantLayerIds.containsKey("education"))
		  {
		     participantLayerIds.put("education", null);
		  }
		  participants.get(code).put("education", education);
		  if (custom.length() > 0 && !participantLayerIds.containsKey("custom"))
		  {
		     participantLayerIds.put("custom", null);
		  }
		  participants.get(code).put("custom", custom);
	       }
	       else if (header.startsWith("@Media:"))
	       {
		  StringTokenizer tokens = new StringTokenizer(value, ", ");
		  if (tokens.hasMoreTokens())
		  {
		     setMediaName(tokens.nextToken());
		     if (tokens.hasMoreTokens())
		     {
			setMediaType(tokens.nextToken());
		     }
		  }
	       }
	       else if (header.startsWith("@Transcriber:"))
	       {
		  transcribersFound = true;
		  transcribers.add(value);
	       }
	    } // it's a key/value pair
	 } // @ line
      } // next header

      if (getParticipantLayerId() == null)
      {
	 Parameter p = new Parameter("participantLayerId", "layerId", "Participant layer", "Layer for speaker/participant identification", true);
	 if (idToLayer.containsKey("participant"))
	 {
	    p.setValue("participant");
	 }
	 else if (idToLayer.containsKey("participants"))
	 {
	    p.setValue("participants");
	 }
	 else if (idToLayer.containsKey("who"))
	 {
	    p.setValue("who");
	 }
	 parameters.addParameter(p);
      }
      if (getTurnLayerId() == null)
      {
	 Parameter p = new Parameter("turnLayerId", "layerId", "Turn layer", "Layer for speaker turns", true);
	 if (idToLayer.containsKey("turn"))
	 {
	    p.setValue("turn");
	 }
	 else if (idToLayer.containsKey("turns"))
	 {
	    p.setValue("turns");
	 }
	 parameters.addParameter(p);
      }
      if (getUtteranceLayerId() == null)
      {
	 Parameter p = new Parameter("utteranceLayerId", "layerId", "Utterance layer", "Layer for speaker utterances", true);
	 if (idToLayer.containsKey("utterance"))
	 {
	    p.setValue("utterance");
	 }
	 else if (idToLayer.containsKey("utterances"))
	 {
	    p.setValue("utterances");
	 }
	 else if (idToLayer.containsKey("line"))
	 {
	    p.setValue("line");
	 }
	 else if (idToLayer.containsKey("lines"))
	 {
	    p.setValue("lines");
	 }
	 parameters.addParameter(p);
      }
      if (getWordLayerId() == null)
      {
	 Parameter p = new Parameter("wordLayerId", "layerId", "Word layer", "Layer for individual word tokens", true);
	 if (idToLayer.containsKey("transcript"))
	 {
	    p.setValue("transcript");
	 }
	 else if (idToLayer.containsKey("word"))
	 {
	    p.setValue("word");
	 }
	 else if (idToLayer.containsKey("words"))
	 {
	    p.setValue("words");
	 }
	 else if (idToLayer.containsKey("w"))
	 {
	    p.setValue("w");
	 }
	 parameters.addParameter(p);
      }
      if (disfluenciesFound)
      {
	 Parameter p = new Parameter("disfluencyLayerId", "layerId", "Disfluency layer", "Layer for disfluency annotations");
	 if (idToLayer.containsKey("disfluency"))
	 {
	    p.setValue("disfluency");
	 }
	 else if (idToLayer.containsKey("disfluencies"))
	 {
	    p.setValue("disfluencies");
	 }
	 parameters.addParameter(p);
      }
      if (expansionsFound)
      {
	 Parameter p = new Parameter("expansionLayerId", "layerId", "Expansion layer", "Layer for expansion annotations");
	 if (idToLayer.containsKey("expansion"))
	 {
	    p.setValue("expansion");
	 }
	 else if (idToLayer.containsKey("expansions"))
	 {
	    p.setValue("expansions");
	 }
	 parameters.addParameter(p);
      }
      if (completionsFound)
      {
	 Parameter p = new Parameter("completionLayerId", "layerId", "Completion layer", "Layer for completion annotations");
	 if (idToLayer.containsKey("completion"))
	 {
	    p.setValue("completion");
	 }
	 else if (idToLayer.containsKey("completions"))
	 {
	    p.setValue("completions");
	 }
	 parameters.addParameter(p);
      }
      if (gemsFound)
      {
	 Parameter p = new Parameter("gemLayerId", "layerId", "Gem layer", "Layer for gems");
	 if (idToLayer.containsKey("gem"))
	 {
	    p.setValue("gem");
	 }
	 else if (idToLayer.containsKey("gems"))
	 {
	    p.setValue("gems");
	 }
	 else if (idToLayer.containsKey("topic"))
	 {
	    p.setValue("topic");
	 }
	 else if (idToLayer.containsKey("topics"))
	 {
	    p.setValue("topics");
	 }
	 parameters.addParameter(p);
      }
      if (transcribersFound)
      {
	 Parameter p = new Parameter("transcriberLayerId", "layerId", "Transcriber layer", "Layer for transcriber name");
	 if (idToLayer.containsKey("transcriber"))
	 {
	    p.setValue("transcriber");
	 }
	 else if (idToLayer.containsKey("transcribers"))
	 {
	    p.setValue("transcribers");
	 }
	 parameters.addParameter(p);
      }
      if (languagesFound)
      {
	 Parameter p = new Parameter("languagesLayerId", "layerId", "Transcript language layer", "Layer for transcriber language");
	 if (idToLayer.containsKey("languages"))
	 {
	    p.setValue("languages");
	 }
	 else if (idToLayer.containsKey("language"))
	 {
	    p.setValue("language");
	 }
	 parameters.addParameter(p);
      }
      // participant meta data layers
      for (String attribute : participantLayerIds.keySet())
      {
	 Parameter p = new Parameter(attribute + "LayerId", "layerId", attribute + " layer", "Layer for " + attribute);
	 // if we have a layer called that
	 if (idToLayer.containsKey(attribute)
	     // and it's a child of the participant layer
	     && (idToLayer.get(attribute).getParentId().equals(getParticipantLayerId())
		 || idToLayer.get(attribute).getParentId().equals("who")))
	 {
	    p.setValue(attribute);
	 }
	 parameters.addParameter(p);
      }

      return parameters;
   }

   /**
    * Sets parameters for a given deserialization operation, after loading the serialized form of the graph. This might include mappings from format-specific objects like tiers to graph layers, etc.
    * @param parameters The configuration for a given deserialization operation.
    * @throws DeserializationParametersMissingException If not all required parameters have a value.
    */
   public void setParameters(ParameterSet parameters) throws DeserializationParametersMissingException
   {
      if (parameters.containsKey("participantLayerId"))
      {
	 setParticipantLayerId((String)parameters.get("participantLayerId").getValue());
      }
      if (parameters.containsKey("turnLayerId"))
      {
	 setTurnLayerId((String)parameters.get("turnLayerId").getValue());
      }
      if (parameters.containsKey("utteranceLayerId"))
      {
	 setUtteranceLayerId((String)parameters.get("utteranceLayerId").getValue());
      }
      if (parameters.containsKey("wordLayerId"))
      {
	 setWordLayerId((String)parameters.get("wordLayerId").getValue());
      }
      if (parameters.containsKey("disfluencyLayerId"))
      {
	 setDisfluencyLayerId((String)parameters.get("disfluencyLayerId").getValue());
      }
      if (parameters.containsKey("expansionLayerId"))
      {
	 setExpansionLayerId((String)parameters.get("expansionLayerId").getValue());
      }
      if (parameters.containsKey("completionLayerId"))
      {
	 setCompletionLayerId((String)parameters.get("completionLayerId").getValue());
      }
      if (parameters.containsKey("gemLayerId"))
      {
	 setGemLayerId((String)parameters.get("gemLayerId").getValue());
      }
      if (parameters.containsKey("transcriberLayerId"))
      {
	 setTranscriberLayerId((String)parameters.get("transcriberLayerId").getValue());
      }
      if (parameters.containsKey("languagesLayerId"))
      {
	 setLanguagesLayerId((String)parameters.get("languagesLayerId").getValue());
      }
      if (getParticipantLayerId() == null || getTurnLayerId() == null || getUtteranceLayerId() == null || getWordLayerId() == null)
      {
	 throw new DeserializationParametersMissingException();
      }
      for (String attribute : participantLayerIds.keySet())
      {
	 if (parameters.containsKey(attribute + "LayerId"))
	 {
	    participantLayerIds.put(attribute, ((String)parameters.get(attribute + "LayerId").getValue()));
	 }
      }
   }

   /**
    * Deserializes the serialized data, generating one or more {@link Graph}s.
    * <p>Many data formats will only yield one graph (e.g. Transcriber
    * transcript or Praat textgrid), however there are formats that
    * are capable of storing multiple transcripts in the same file
    * (e.g. AGTK, Transana XML export), which is why this method
    * returns a list.
    * @return A list of valid (if incomplete) {@link Graph}s. 
    * @throws DeserializerNotConfiguredException if the object has not been configured.
    * @throws DeserializationParametersMissingException if the parameters for this particular graph have not been set.
    * @throws DeserializationException if errors occur during deserialization.
    */
   public Graph[] deserialize() 
      throws DeserializerNotConfiguredException, DeserializationParametersMissingException, DeserializationException
   {
      Graph graph = new Graph();
      graph.setId(getName());
      graph.addLayer(idToLayer.get(getParticipantLayerId()));
      graph.addLayer(idToLayer.get(getTurnLayerId()));
      graph.addLayer(idToLayer.get(getUtteranceLayerId()));
      graph.addLayer(idToLayer.get(getWordLayerId()));
      if (getDisfluencyLayerId() != null)
      {
	 graph.addLayer(idToLayer.get(getDisfluencyLayerId()));
      }
      if (getExpansionLayerId() != null) graph.addLayer(idToLayer.get(getExpansionLayerId()));
      if (getCompletionLayerId() != null) graph.addLayer(idToLayer.get(getCompletionLayerId()));
      if (getGemLayerId() != null) graph.addLayer(idToLayer.get(getGemLayerId()));
      if (getTranscriberLayerId() != null) graph.addLayer(idToLayer.get(getTranscriberLayerId()));
      if (getLanguagesLayerId() != null) graph.addLayer(idToLayer.get(getLanguagesLayerId()));
      for (String attribute : participantLayerIds.keySet())
      {
	 String layerId = participantLayerIds.get(attribute);
	 if (layerId != null)
	 {
	    graph.addLayer(idToLayer.get(layerId));
	 }
      } // next participant layer 

      // graph meta data
      if (getTranscriberLayerId() != null)
      {
	 for (String transcriber : transcribers)
	 {
	    graph.createTag(graph, getTranscriberLayerId(), transcriber);
	 }
      }
      if (getLanguagesLayerId() != null)
      {
	 for (String language : languages)
	 {
	    graph.createTag(graph, getLanguagesLayerId(), language);
	 }
      }
      
      // participants
      for (String participantId : participants.keySet())
      {
	 HashMap<String,String> attributes = participants.get(participantId);
	 Annotation participant = new Annotation(
	    participantId, 
	    attributes.containsKey("name")?attributes.get("name"):participantId, 
	    getParticipantLayerId());
	 participant.setParentId(graph.getId());
	 graph.addAnnotation(participant);

	 // set the participant meta-data
	 for (String attribute : participantLayerIds.keySet())
	 {
	    String layerId = participantLayerIds.get(attribute);
	    if (layerId != null && attributes.containsKey(attribute))
	    {
	       graph.createTag(participant, layerId, attributes.get(attribute));
	    }
	 }
	 
      }

      // ensure we have an utterance tokenizer
      if (getTokenizer() == null)
      {
	 setTokenizer(new SimpleTokenizer(getUtteranceLayerId(), getWordLayerId()));
      }

      // regular expressions
      Pattern synchronisedPattern = Pattern.compile("^(.*)([0-9]+)_([0-9]+)$");

      // lines
      Annotation currentTurn = new Annotation(null, "", getTurnLayerId());
      Annotation lastUtterance = new Annotation(null, "", getUtteranceLayerId());
      Annotation gem = null;
      Anchor lastAnchor = new Anchor(null, 0.0, Constants.CONFIDENCE, Constants.CONFIDENCE_MANUAL);
      graph.addAnchor(lastAnchor);
      for (String line : getLines())
      {
	 if (line.startsWith("@"))
	 {
	    if (line.startsWith("@G") || line.startsWith("@Bg"))
	    {
	       if (gem != null)
	       {
		  gem.setEnd(lastAnchor);
		  graph.addAnnotation(gem);
	       }
	       if (getGemLayerId() != null)
	       {
		  String label = "gem";
		  int iColon = line.indexOf(':');
		  if (iColon >= 0)
		  { // it's a key/value pair
		     label = line.substring(iColon + 1).trim();
		  }
		  gem = new Annotation(null, label, getGemLayerId(), graph.getId());
	       }
	    }
	    else if (line.startsWith("@Eg") && gem != null)
	    {
	       gem.setEnd(lastAnchor);
	       graph.addAnnotation(gem);
	       gem = null;
	    }
	 }
	 else
	 {
	    if (line.startsWith("*"))
	    { // setting the speaker, ID is 3 characters
	       // something like:
	       // *SUB: ...
	       String participantId = line.substring(1, 4);
	       if (!participants.containsKey(participantId))
	       {
		  warnings.add("Undeclared participant: " + participantId);
		  
		  Annotation participant = new Annotation(participantId, participantId, getParticipantLayerId());
		  participant.setParentId(graph.getId());
		  graph.addAnnotation(participant);
	       } // undeclared participant
	       if (!participantId.equals(currentTurn.getLabel()))
	       { // new turn
		  if (currentTurn.getEndId() == null)
		  {
		     currentTurn.setEnd(lastAnchor);
		     currentTurn = new Annotation(null, participantId, getTurnLayerId());
		     currentTurn.setStartId(lastAnchor.getId());		  
		     graph.addAnnotation(currentTurn);
		  }
	       }
	       line = line.substring(5);
	    } // participant
	    
	    Annotation utterance = new Annotation(null, line, utteranceLayerId);
	    utterance.setStart(lastUtterance.getEnd());
	    Matcher synchronisedMatcher = synchronisedPattern.matcher(line);
	    if (synchronisedMatcher.matches())
	    {
	       utterance.setLabel(synchronisedMatcher.group(1).trim());
	       utterance.setStart(
		  graph.getOrCreateAnchorAt(
		     Double.parseDouble(synchronisedMatcher.group(2))
		     / 1000));
	       utterance.setEnd(
		  graph.getOrCreateAnchorAt(
		     Double.parseDouble(synchronisedMatcher.group(3))
		     / 1000));
	    }
	    graph.addAnnotation(utterance);
	    utterance.setParent(currentTurn);

	    // if a gem has jjust been started
	    if (gem != null && gem.getStartId() == null)
	    { // set the gem's start anchor ID
	       gem.setStartId(utterance.getStartId());
	    }
	    
	    lastUtterance = utterance;
	    lastAnchor = utterance.getEnd();
	 } // transcript line
      } // next line
      if (gem != null)
      {
	 gem.setEnd(lastAnchor);
	 graph.addAnnotation(gem);
      }
      if (currentTurn.getEndId() == null)
      {
	 currentTurn.setEndId(lastAnchor.getId());
      }

      // tokenize utterances into words
      try
      {
	 getTokenizer().transform(graph);
      }
      catch(TransformationException exception)
      {
	 throw new DeserializationException(exception);
      }

      try
      {
	  // disfluencies
	  ConventionTransformer transformer = new ConventionTransformer(getWordLayerId(), "&(\\w+)");
	  transformer.addDestinationResult(getWordLayerId(), "$1");
	  if (getDisfluencyLayerId() != null)
	  {
	     transformer.addDestinationResult(getDisfluencyLayerId(), "&");
	  }
	  transformer.transform(graph);
	  graph.commit();
	  
	  // expansions
	  SpanningConventionTransformer spanningTransformer = new SpanningConventionTransformer(
	     "word", "\\[:", "(.*)\\]", true, null, null, getExpansionLayerId(), null, "$1", true);	  
	  spanningTransformer.transform(graph);	
	  graph.commit();
	  
	  
	  // completions at the start
	  transformer = new ConventionTransformer(getWordLayerId(), "\\((\\p{Alnum}+)\\)(.+)");
	  transformer.addDestinationResult(getWordLayerId(), "$2");
	  if (getCompletionLayerId() != null)
	  {
	     transformer.addDestinationResult(getCompletionLayerId(), "$1$2");
	  }
	  transformer.transform(graph);	
	  graph.commit();
  
	  // completions at the end
	  transformer = new ConventionTransformer(getWordLayerId(), "(.+)\\((\\p{Alnum}+)\\)");
	  transformer.addDestinationResult(getWordLayerId(), "$1");
	  if (getCompletionLayerId() != null)
	  {
	     transformer.addDestinationResult(getCompletionLayerId(), "$1$2");
	  }
	  transformer.transform(graph);
	  graph.commit();
	  
	  Graph[] graphs = { graph };
	  return graphs;
      }
      catch(TransformationException exception)
      {
	 throw new DeserializationException(exception);
      }
   }

   /**
    * Returns any warnings that may have arisen during the last execution of {@link #deserialize()}.
    * @return A possibly empty lilayersst of warnings.
    */
   public String[] getWarnings()
   {
      return warnings.toArray(new String[0]);
   }

} // end of class ChatDeserializer
