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
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
 * <p><em>NB</em> the current implementation is <em>not exhaustive</em>; it only covers:
 * <ul>
 *  <li>Time synchronization codes</li>
 *  <li>Disfluency marking with &amp; - e.g. <samp>so &amp;sund Sunday</samp></li>
 *  <li>Non-standard form expansion - e.g. <samp>gonna [: going to]</samp></li>
 *  <li>Incomplete word completion - e.g. <samp>dinner doin(g) all</samp></li>
 *  <li>Acronym/proper name joining with _ - e.g. <samp>no T_V in my room</samp> - TODO</li>
 * </ul>
 * @author Robert Fromont robert@fromont.net.nz
 */

public class ChatDeserializer
  implements IDeserializer
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


   /**
    * Participant layer.
    * @see #getParticipantLayer()
    * @see #setParticipantLayer(Layer)
    */
   protected Layer participantLayer;
   /**
    * Getter for {@link #participantLayer}: Participant layer.
    * @return Participant layer.
    */
   public Layer getParticipantLayer() { return participantLayer; }
   /**
    * Setter for {@link #participantLayer}: Participant layer.
    * @param newParticipantLayer Participant layer.
    */
   public void setParticipantLayer(Layer newParticipantLayer) { participantLayer = newParticipantLayer; }
   
   /**
    * Turn layer.
    * @see #getTurnLayer()
    * @see #setTurnLayer(Layer)
    */
   protected Layer turnLayer;
   /**
    * Getter for {@link #turnLayer}: Turn layer.
    * @return Turn layer.
    */
   public Layer getTurnLayer() { return turnLayer; }
   /**
    * Setter for {@link #turnLayer}: Turn layer.
    * @param newTurnLayer Turn layer.
    */
   public void setTurnLayer(Layer newTurnLayer) { turnLayer = newTurnLayer; }

   /**
    * Utterance layer.
    * @see #getUtteranceLayer()
    * @see #setUtteranceLayer(Layer)
    */
   protected Layer utteranceLayer;
   /**
    * Getter for {@link #utteranceLayer}: Utterance layer.
    * @return Utterance layer.
    */
   public Layer getUtteranceLayer() { return utteranceLayer; }
   /**
    * Setter for {@link #utteranceLayer}: Utterance layer.
    * @param newUtteranceLayer Utterance layer.
    */
   public void setUtteranceLayer(Layer newUtteranceLayer) { utteranceLayer = newUtteranceLayer; }

   /**
    * Word layer.
    * @see #getWordLayer()
    * @see #setWordLayer(Layer)
    */
   protected Layer wordLayer;
   /**
    * Getter for {@link #wordLayer}: Word layer.
    * @return Word layer.
    */
   public Layer getWordLayer() { return wordLayer; }
   /**
    * Setter for {@link #wordLayer}: Word layer.
    * @param newWordLayer Word layer.
    */
   public void setWordLayer(Layer newWordLayer) { wordLayer = newWordLayer; }

   /**
    * Disfluency layer.
    * @see #getDisfluencyLayer()
    * @see #setDisfluencyLayer(Layer)
    */
   protected Layer disfluencyLayer;
   /**
    * Getter for {@link #disfluencyLayer}: Disfluency layer.
    * @return Disfluency layer.
    */
   public Layer getDisfluencyLayer() { return disfluencyLayer; }
   /**
    * Setter for {@link #disfluencyLayer}: Disfluency layer.
    * @param newDisfluencyLayer Disfluency layer.
    */
   public void setDisfluencyLayer(Layer newDisfluencyLayer) { disfluencyLayer = newDisfluencyLayer; }

   /**
    * Expansion layer.
    * @see #getExpansionLayer()
    * @see #setExpansionLayer(Layer)
    */
   protected Layer expansionLayer;
   /**
    * Getter for {@link #expansionLayer}: Expansion layer.
    * @return Expansion layer.
    */
   public Layer getExpansionLayer() { return expansionLayer; }
   /**
    * Setter for {@link #expansionLayer}: Expansion layer.
    * @param newExpansionLayer Expansion layer.
    */
   public void setExpansionLayer(Layer newExpansionLayer) { expansionLayer = newExpansionLayer; }

   /**
    * Completion layer.
    * @see #getCompletionLayer()
    * @see #setCompletionLayer(Layer)
    */
   protected Layer completionLayer;
   /**
    * Getter for {@link #completionLayer}: Completion layer.
    * @return Completion layer.
    */
   public Layer getCompletionLayer() { return completionLayer; }
   /**
    * Setter for {@link #completionLayer}: Completion layer.
    * @param newCompletionLayer Completion layer.
    */
   public void setCompletionLayer(Layer newCompletionLayer) { completionLayer = newCompletionLayer; }
   
   /**
    * Gems
    * @see #getGemLayer()
    * @see #setGemLayer(Layer)
    */
   protected Layer gemLayer;
   /**
    * Getter for {@link #gemLayer}: Gems
    * @return Gems
    */
   public Layer getGemLayer() { return gemLayer; }
   /**
    * Setter for {@link #gemLayer}: Gems
    * @param newGemLayer Gems
    */
   public void setGemLayer(Layer newGemLayer) { gemLayer = newGemLayer; }

   
   /**
    * Transcriber graph attributes.
    * @see #getTranscriberLayer()
    * @see #setTranscriberLayer(Layer)
    */
   protected Layer transcriberLayer;
   /**
    * Getter for {@link #transcriberLayer}: Transcriber graph attributes.
    * @return Transcriber graph attributes.
    */
   public Layer getTranscriberLayer() { return transcriberLayer; }
   /**
    * Setter for {@link #transcriberLayer}: Transcriber graph attributes.
    * @param newTranscriberLayer Transcriber graph attributes.
    */
   public void setTranscriberLayer(Layer newTranscriberLayer) { transcriberLayer = newTranscriberLayer; }

   /**
    * Graph language.
    * @see #getLanguagesLayer()
    * @see #setLanguagesLayer(Layer)
    */
   protected Layer languagesLayer;
   /**
    * Getter for {@link #languagesLayer}: Graph language.
    * @return Graph language.
    */
   public Layer getLanguagesLayer() { return languagesLayer; }
   /**
    * Setter for {@link #languagesLayer}: Graph language.
    * @param newLanguagesLayer Graph language.
    */
   public void setLanguagesLayer(Layer newLanguagesLayer) { languagesLayer = newLanguagesLayer; }

   /**
    * Required participant meta-data layers.
    * @see #getParticipantLayers()
    * @see #setParticipantLayers(HashSet)
    */
   protected HashMap<String,Layer> participantLayers;
   /**
    * Getter for {@link #participantLayers}: Required participant meta-data layers.
    * @return Required participant meta-data layers.
    */
   public HashMap<String,Layer> getParticipantLayers() { return participantLayers; }
   /**
    * Setter for {@link #participantLayers}: Required participant meta-data layers.
    * @param newParticipantLayers Required participant meta-data layers.
    */
   public void setParticipantLayers(HashMap<String,Layer> newParticipantLayers) { participantLayers = newParticipantLayers; }
   
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
      participantLayer = null;
      turnLayer = null;
      utteranceLayer = null;
      wordLayer = null;
      gemLayer = null;
      transcriberLayer = null;
      languagesLayer = null;
      expansionLayer = null;
      completionLayer = null;
      disfluencyLayer = null;
      participantLayers = new HashMap<String,Layer>();
   } // end of reset()

   // IStreamDeserializer methods:

   /**
    * Returns the deserializer's descriptor
    * @return The deserializer's descriptor
    */
   public SerializationDescriptor getDescriptor()
   {
      return new SerializationDescriptor(
	 "CLAN CHAT transcript", "0.1", "text/x-chat", ".cha", getClass().getResource("icon.gif"));
   }

   /**
    * Sets parameters for deserializer as a whole.  This might include database connection parameters, locations of supporting files, etc.
    * <p>When the deserializer is installed, this method should be invoked with an empty parameter set, to discover what (if any) general configuration is required. If parameters are returned, and user interaction is possible, then the user may be presented with an interface for setting/confirming these parameters.  Once the parameters are set, this method can be invoked again with the required values, resulting in an empty parameter set being returned to confirm that nothing further is required.
    * @param configuration The configuration for the deserializer. 
    * @param schema The layer schema, definining layers and the way they interrelate.
    * @return A list of configuration parameters (still) must be set before {@link IDeserializer#setParameters()} can be invoked. If this is an empty list, {@link IDeserializer#setParameters()} can be invoked. If it's not an empty list, this method must be invoked again with the returned parameters' values set.
    * @throws DeserializerNotConfiguredException If the configuration is not sufficient for deserialization.
    */
   public ParameterSet configure(ParameterSet configuration, Schema schema) throws DeserializerNotConfiguredException
   {
      return new ParameterSet(); // TODO move layer discovery from load() to configure()
   }

   /**
    * Loads the serialized form of the graph, using the given set of named streams.
    * @param annotationStreams A list of named streams that contain all the transcription/annotation data required.
    * @param mediaStreams An optional (may be null) list of named streams that contain the media annotated by the <var>annotationStreams</var>.
    * @param schema The layer schema, definining layers and the way they interrelate.
    * @return A list of parameters that require setting before {@link IDeserializer#deserialize()} can be invoked. This may be an empty list, and may include parameters with the value already set to a workable default. If there are parameters, and user interaction is possible, then the user may be presented with an interface for setting/confirming these parameters, before they are then passed to {@link IDeserializer#setParameters(ParameterSet)}.
    * @throws Exception If the stream could not be loaded.
    */
   @SuppressWarnings({"rawtypes", "unchecked"})
   public ParameterSet load(NamedStream[] annotationStreams, NamedStream[] mediaStreams, Schema schema) throws Exception
   {
      ParameterSet parameters = new ParameterSet();

      // take the first stream, ignore all others.
      NamedStream cha = annotationStreams[0];
      setName(cha.getName());

      reset();
      setSchema(schema);
      setParticipantLayer(schema.getParticipantLayer());
      setTurnLayer(schema.getTurnLayer());
      setUtteranceLayer(schema.getUtteranceLayer());
      setWordLayer(schema.getWordLayer());

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
	    // is it the first line?
	    if (lines.size() == 0 
		// or the last line was time synchronized?
		|| lines.lastElement().endsWith("")
		// or the last line was a 'gem'?
		|| lines.lastElement().startsWith("@")
	       )
	    { // this is a new utterance
	       lines.add(line);
	    }
	    else
	    { // last line was not time-sycnchronized, so append this line to it
	       // remove the last line
	       String lastLine = lines.remove(lines.size()-1);
	       // append this line to it
	       lines.add(lastLine + " " + line);
	    }
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
			      participantLayers.put("role", null);
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
		  if (language.length() > 0 && !participantLayers.containsKey("language"))
		  {
		     participantLayers.put("language", null);
		  }
		  participants.get(code).put("language", language);
		  if (corpus.length() > 0 && !participantLayers.containsKey("corpus"))
		  {
		     participantLayers.put("corpus", null);
		  }
		  participants.get(code).put("corpus", corpus);
		  if (age.length() > 0 && !participantLayers.containsKey("age"))
		  {
		     participantLayers.put("age", null);
		  }
		  participants.get(code).put("age", age);
		  if (sex.length() > 0 && !participantLayers.containsKey("sex"))
		  {
		     participantLayers.put("sex", null);
		  }
		  participants.get(code).put("sex", sex);
		  if (group.length() > 0 && !participantLayers.containsKey("group"))
		  {
		     participantLayers.put("group", null);
		  }
		  participants.get(code).put("group", group);
		  if (SES.length() > 0 && !participantLayers.containsKey("SES"))
		  {
		     participantLayers.put("SES", null);
		  }
		  participants.get(code).put("SES", SES);
		  if (role.length() > 0 && !participantLayers.containsKey("role"))
		  {
		     participantLayers.put("role", null);
		     //TODO participants.get(code).put("role", role);
		  }
		  participants.get(code).put("role", role);
		  if (education.length() > 0 && !participantLayers.containsKey("education"))
		  {
		     participantLayers.put("education", null);
		  }
		  participants.get(code).put("education", education);
		  if (custom.length() > 0 && !participantLayers.containsKey("custom"))
		  {
		     participantLayers.put("custom", null);
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

      LinkedHashMap<String,Layer> possibleParticipantLayers = new LinkedHashMap<String,Layer>();
      LinkedHashMap<String,Layer> possibleTurnLayers = new LinkedHashMap<String,Layer>();
      LinkedHashMap<String,Layer> possibleTurnChildLayers = new LinkedHashMap<String,Layer>();
      LinkedHashMap<String,Layer> wordTagLayers = new LinkedHashMap<String,Layer>();
      LinkedHashMap<String,Layer> participantTagLayers = new LinkedHashMap<String,Layer>();
      if (getParticipantLayer() == null || getTurnLayer() == null 
	  || getUtteranceLayer() == null || getWordLayer() == null)
      {
	 for (Layer top : schema.getRoot().getChildren().values())
	 {
	    if (top.getAlignment() == Constants.ALIGNMENT_NONE)
	    {
	       if (top.getChildren().size() == 0)
	       { // unaligned childless children of graph
		  participantTagLayers.put(top.getId(), top);
	       }
	       else
	       { // unaligned children of graph, with children of their own
		  possibleParticipantLayers.put(top.getId(), top);
		  for (Layer turn : top.getChildren().values())
		  {
		     if (turn.getAlignment() == Constants.ALIGNMENT_INTERVAL
			 && turn.getChildren().size() > 0)
		     { // aligned children of who with their own children
			possibleTurnLayers.put(turn.getId(), turn);
			for (Layer turnChild : turn.getChildren().values())
			{
			   if (turnChild.getAlignment() == Constants.ALIGNMENT_INTERVAL)
			   { // aligned children of turn
			      possibleTurnChildLayers.put(turnChild.getId(), turnChild);
			      for (Layer tag : turnChild.getChildren().values())
			      {
				 if (tag.getAlignment() == Constants.ALIGNMENT_NONE)
				 { // unaligned children of word
				    wordTagLayers.put(tag.getId(), tag);
				 }
			      } // next possible word tag layer
			   }
			} // next possible turn child layer
		     }
		  } // next possible turn layer
	       } // with children
	    } // unaligned
	 } // next possible participant layer
      } // missing special layers
      else
      {
	 for (Layer tag : getWordLayer().getChildren().values())
	 {
	    if (tag.getAlignment() == Constants.ALIGNMENT_NONE
		&& tag.getChildren().size() == 0)
	    {
	       wordTagLayers.put(tag.getId(), tag);
	    }
	 } // next possible word tag layer
	 for (Layer tag : getParticipantLayer().getChildren().values())
	 {
	    if (tag.getAlignment() == Constants.ALIGNMENT_NONE
		&& tag.getChildren().size() == 0)
	    {
	       participantTagLayers.put(tag.getId(), tag);
	    }
	 } // next possible word tag layer
      }
      participantTagLayers.remove("main_participant");

      if (getParticipantLayer() == null)
      {
	 Parameter p = new Parameter("participantLayer", Layer.class, "Participant layer", "Layer for speaker/participant identification", true);
	 String[] possibilities = {"participant","participants","who","speaker","speakers"};
	 p.setValue(findLayerById(possibleParticipantLayers, possibilities));
	 p.setPossibleValues(possibleParticipantLayers.values());
	 parameters.addParameter(p);
      }
      if (getTurnLayer() == null)
      {
	 Parameter p = new Parameter("turnLayer", Layer.class, "Turn layer", "Layer for speaker turns", true);
	 String[] possibilities = {"turn","turns"};
	 p.setValue(findLayerById(possibleTurnLayers, possibilities));
	 p.setPossibleValues(possibleTurnLayers.values());
	 parameters.addParameter(p);
      }
      if (getUtteranceLayer() == null)
      {
	 Parameter p = new Parameter("utteranceLayer", Layer.class, "Utterance layer", "Layer for speaker utterances", true);
	 String[] possibilities = {"utterance","utterances","line","lines"};
	 p.setValue(findLayerById(possibleTurnChildLayers, possibilities));
	 p.setPossibleValues(possibleTurnChildLayers.values());
	 parameters.addParameter(p);
      }
      if (getWordLayer() == null)
      {
	 Parameter p = new Parameter("wordLayer", Layer.class, "Word layer", "Layer for individual word tokens", true);
	 String[] possibilities = {"transcript","word","words","w"};
	 p.setValue(findLayerById(possibleTurnChildLayers, possibilities));
	 p.setPossibleValues(possibleTurnChildLayers.values());
	 parameters.addParameter(p);
      }
      if (disfluenciesFound)
      {
	 Parameter p = new Parameter("disfluencyLayer", Layer.class, "Disfluency layer", "Layer for disfluency annotations");
	 String[] possibilities = {"disfluency","disfluencies"};
	 p.setValue(findLayerById(wordTagLayers, possibilities));
	 p.setPossibleValues(wordTagLayers.values());
	 parameters.addParameter(p);
      }
      if (expansionsFound)
      {
	 Parameter p = new Parameter("expansionLayer", Layer.class, "Expansion layer", "Layer for expansion annotations");
	 String[] possibilities = {"expansion","expansions"};
	 p.setValue(findLayerById(wordTagLayers, possibilities));
	 p.setPossibleValues(wordTagLayers.values());
	 parameters.addParameter(p);
      }
      if (completionsFound)
      {
	 Parameter p = new Parameter("completionLayer", Layer.class, "Completion layer", "Layer for completion annotations");
	 String[] possibilities = {"completion","completions"};
	 p.setValue(findLayerById(wordTagLayers, possibilities));
	 p.setPossibleValues(wordTagLayers.values());
	 parameters.addParameter(p);
      }
      if (gemsFound)
      {
	 LinkedHashMap<String,Layer> possibleLayers = new LinkedHashMap<String,Layer>();
	 for (Layer top : schema.getRoot().getChildren().values())
	 {
	    if (top.getAlignment() == Constants.ALIGNMENT_INTERVAL)
	    { // aligned children of graph
	       possibleLayers.put(top.getId(), top);
	    }
	 } // next top level layer
	 Parameter p = new Parameter("gemLayer", Layer.class, "Gem layer", "Layer for gems");
	 String[] possibilities = {"gem","gems","topic","topics"};
	 p.setValue(findLayerById(possibleLayers, possibilities));
	 p.setPossibleValues(possibleLayers.values());
	 parameters.addParameter(p);
      }
      LinkedHashMap<String,Layer> graphTagLayers = new LinkedHashMap<String,Layer>();
      for (Layer top : schema.getRoot().getChildren().values())
      {
	 if (top.getAlignment() == Constants.ALIGNMENT_NONE
	     && top.getChildren().size() == 0)
	 { // unaligned childless children of graph
	    graphTagLayers.put(top.getId(), top);
	 }
      } // next top level layer
      graphTagLayers.remove("corpus");
      graphTagLayers.remove("transcript_type");
      if (transcribersFound)
      {
	 Parameter p = new Parameter("transcriberLayer", Layer.class, "Transcriber layer", "Layer for transcriber name");
	 String[] possibilities = {"transcriber","transcribers","transcript_transcriber","transcript_transcribers", "scribe","scribes", "transcript_scribe","transcript_scribes"};
	 p.setValue(findLayerById(graphTagLayers, possibilities));
	 p.setPossibleValues(graphTagLayers.values());
	 parameters.addParameter(p);
      }
      if (languagesFound)
      {
	 Parameter p = new Parameter("languagesLayer", Layer.class, "Transcript language layer", "Layer for transcriber language");
	 String[] possibilities = {"transcript_language","transcript_languages","language","languages"};
	 p.setValue(findLayerById(graphTagLayers, possibilities));
	 p.setPossibleValues(graphTagLayers.values());
	 parameters.addParameter(p);
      }
      // participant meta data layers
      for (String attribute : participantLayers.keySet())
      {
	 Parameter p = new Parameter(attribute + "Layer", Layer.class, attribute + " layer", "Layer for " + attribute);
	 // if we have a layer called that
	 String[] possibilities = {"participant_"+attribute, attribute};
	 p.setValue(findLayerById(participantTagLayers, possibilities));
	 p.setPossibleValues(participantTagLayers.values());
	 parameters.addParameter(p);
      }

      return parameters;
   }

   
   /**
    * Tries to find a layer in the given map, using an ordered list of possible IDs.
    * @param possibleLayers
    * @param possibleIds
    * @return The first matching layer, or null if none matched.
    */
   public Layer findLayerById(LinkedHashMap<String,Layer> possibleLayers, String[] possibleIds)
   {
      HashSet<String> possibleLayerIds = new HashSet<String>();
      for (String id : possibleLayers.keySet()) possibleLayerIds.add(id.toLowerCase());
      for (String id : possibleIds)
      {
	 if (possibleLayerIds.contains(id.toLowerCase()))
	 {
	    return possibleLayers.get(id);
	 }
      }
      return null;
   } // end of findLayerById()


   /**
    * Sets parameters for a given deserialization operation, after loading the serialized form of the graph. This might include mappings from format-specific objects like tiers to graph layers, etc.
    * @param parameters The configuration for a given deserialization operation.
    * @throws DeserializationParametersMissingException If not all required parameters have a value.
    */
   public void setParameters(ParameterSet parameters) throws DeserializationParametersMissingException
   {
      if (parameters.containsKey("participantLayer"))
      {
	 setParticipantLayer((Layer)parameters.get("participantLayer").getValue());
      }
      if (parameters.containsKey("turnLayer"))
      {
	 setTurnLayer((Layer)parameters.get("turnLayer").getValue());
      }
      if (parameters.containsKey("utteranceLayer"))
      {
	 setUtteranceLayer((Layer)parameters.get("utteranceLayer").getValue());
      }
      if (parameters.containsKey("wordLayer"))
      {
	 setWordLayer((Layer)parameters.get("wordLayer").getValue());
      }
      if (parameters.containsKey("disfluencyLayer"))
      {
	 setDisfluencyLayer((Layer)parameters.get("disfluencyLayer").getValue());
      }
      if (parameters.containsKey("expansionLayer"))
      {
	 setExpansionLayer((Layer)parameters.get("expansionLayer").getValue());
      }
      if (parameters.containsKey("completionLayer"))
      {
	 setCompletionLayer((Layer)parameters.get("completionLayer").getValue());
      }
      if (parameters.containsKey("gemLayer"))
      {
	 setGemLayer((Layer)parameters.get("gemLayer").getValue());
      }
      if (parameters.containsKey("transcriberLayer"))
      {
	 setTranscriberLayer((Layer)parameters.get("transcriberLayer").getValue());
      }
      if (parameters.containsKey("languagesLayer"))
      {
	 setLanguagesLayer((Layer)parameters.get("languagesLayer").getValue());
      }
      if (getParticipantLayer() == null || getTurnLayer() == null || getUtteranceLayer() == null || getWordLayer() == null)
      {
	 throw new DeserializationParametersMissingException();
      }
      for (String attribute : participantLayers.keySet())
      {
	 if (parameters.containsKey(attribute + "Layer"))
	 {
	    participantLayers.put(attribute, ((Layer)parameters.get(attribute + "Layer").getValue()));
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
      // add layers to the graph
      // we don't just copy the whole schema, because that would imply that all the extra layers
      // contained no annotations, which is not necessarily true
      graph.addLayer((Layer)getParticipantLayer().clone());
      graph.getSchema().setParticipantLayerId(getParticipantLayer().getId());
      graph.addLayer((Layer)getTurnLayer().clone());
      graph.getSchema().setTurnLayerId(getTurnLayer().getId());
      graph.addLayer((Layer)getUtteranceLayer().clone());
      graph.getSchema().setUtteranceLayerId(getUtteranceLayer().getId());
      graph.addLayer((Layer)getWordLayer().clone());
      graph.getSchema().setWordLayerId(getWordLayer().getId());
      if (getDisfluencyLayer() != null) graph.addLayer((Layer)getDisfluencyLayer().clone());
      if (getExpansionLayer() != null) graph.addLayer((Layer)getExpansionLayer().clone());
      if (getCompletionLayer() != null) graph.addLayer((Layer)getCompletionLayer().clone());
      if (getGemLayer() != null) graph.addLayer((Layer)getGemLayer().clone());
      if (getTranscriberLayer() != null) graph.addLayer((Layer)getTranscriberLayer().clone());
      if (getLanguagesLayer() != null) graph.addLayer((Layer)getLanguagesLayer().clone());
      for (String attribute : participantLayers.keySet())
      {
	 Layer layer = participantLayers.get(attribute);
	 if (layer != null)
	 {
	    graph.addLayer((Layer)layer.clone());
	 }
      } // next participant layer 

      // graph meta data
      if (getTranscriberLayer() != null)
      {
	 for (String transcriber : transcribers)
	 {
	    graph.createTag(graph, getTranscriberLayer().getId(), transcriber);
	 }
      }
      if (getLanguagesLayer() != null)
      {
	 for (String language : languages)
	 {
	    graph.createTag(graph, getLanguagesLayer().getId(), language);
	 }
      }
      
      // participants
      for (String participantId : participants.keySet())
      {
	 HashMap<String,String> attributes = participants.get(participantId);
	 Annotation participant = new Annotation(
	    participantId, 
	    attributes.containsKey("name")?attributes.get("name"):participantId, 
	    getParticipantLayer().getId());
	 participant.setParentId(graph.getId());
	 graph.addAnnotation(participant);

	 // set the participant meta-data
	 for (String attribute : participantLayers.keySet())
	 {
	    Layer layer = participantLayers.get(attribute);
	    if (layer != null && attributes.containsKey(attribute))
	    {
	       graph.createTag(participant, layer.getId(), attributes.get(attribute));
	    }
	 }	 
      }

      // ensure we have an utterance tokenizer
      if (getTokenizer() == null)
      {
	 setTokenizer(new SimpleTokenizer(getUtteranceLayer().getId(), getWordLayer().getId()));
      }

      // regular expressions
      Pattern synchronisedPattern = Pattern.compile("^(.*)([0-9]+)_([0-9]+)$");

      // lines
      Annotation currentTurn = new Annotation(null, "", getTurnLayer().getId());
      Annotation lastUtterance = new Annotation(null, "", getUtteranceLayer().getId());
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
	       if (getGemLayer() != null)
	       {
		  String label = "gem";
		  int iColon = line.indexOf(':');
		  if (iColon >= 0)
		  { // it's a key/value pair
		     label = line.substring(iColon + 1).trim();
		  }
		  gem = new Annotation(null, label, getGemLayer().getId(), graph.getId());
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
		  
		  Annotation participant = new Annotation(participantId, participantId, getParticipantLayer().getId());
		  participant.setParentId(graph.getId());
		  graph.addAnnotation(participant);
	       } // undeclared participant
	       if (!participantId.equals(currentTurn.getLabel()))
	       { // new turn
		  if (currentTurn.getEndId() == null)
		  {
		     currentTurn.setEnd(lastAnchor);
		     currentTurn = new Annotation(null, participantId, getTurnLayer().getId());
		     currentTurn.setStartId(lastAnchor.getId());		  
		     currentTurn.setParentId(participantId);
		     graph.addAnnotation(currentTurn);
		  }
	       }
	       line = line.substring(5);
	    } // participant
	    
	    Annotation utterance = new Annotation(null, line, getUtteranceLayer().getId());
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

	    // if a gem has just been started
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
      currentTurn.setEndId(lastAnchor.getId());

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
	 ConventionTransformer transformer = new ConventionTransformer(getWordLayer().getId(), "&(\\w+)");
	 transformer.addDestinationResult(getWordLayer().getId(), "$1");
	 if (getDisfluencyLayer() != null)
	 {
	    transformer.addDestinationResult(getDisfluencyLayer().getId(), "&");
	 }
	 transformer.transform(graph);
	 graph.commit();
	 
	 // expansions
	 String expansionLayerId = getExpansionLayer()!=null?getExpansionLayer().getId():null;
	 SpanningConventionTransformer spanningTransformer = new SpanningConventionTransformer(
	    getWordLayer().getId(), "\\[:", "(.*)\\]", true, null, null, 
	    expansionLayerId, null, "$1", true);	  
	 spanningTransformer.transform(graph);	
	 graph.commit();
	 
	 // completions at the start
	 transformer = new ConventionTransformer(getWordLayer().getId(), "\\((\\p{Alnum}+)\\)(.+)");
	 transformer.addDestinationResult(getWordLayer().getId(), "$2");
	 if (getCompletionLayer() != null)
	 {
	    transformer.addDestinationResult(getCompletionLayer().getId(), "$1$2");
	 }
	 transformer.transform(graph);	
	 graph.commit();
	 
	 // completions at the end
	 transformer = new ConventionTransformer(getWordLayer().getId(), "(.+)\\((\\p{Alnum}+)\\)");
	 transformer.addDestinationResult(getWordLayer().getId(), "$1");
	 if (getCompletionLayer() != null)
	 {
	    transformer.addDestinationResult(getCompletionLayer().getId(), "$1$2");
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
