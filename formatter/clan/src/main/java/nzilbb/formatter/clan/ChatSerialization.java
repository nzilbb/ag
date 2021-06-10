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
package nzilbb.formatter.clan;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Spliterator;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import nzilbb.ag.*;
import nzilbb.ag.serialize.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.util.AnnotationComparatorByAnchor;
import nzilbb.ag.util.ConventionTransformer;
import nzilbb.ag.util.SimpleTokenizer;
import nzilbb.ag.util.SpanningConventionTransformer;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.util.IO;
import nzilbb.util.ISO639;
import nzilbb.util.TempFileInputStream;

/**
 * Serialization for CHAT files produced by CLAN.
 * <p><em>NB</em> the current implementation is <em>not exhaustive</em>; it only covers:
 * <ul>
 *  <li> Time synchronization codes, including mid-line synchronization.
 *    <br> Overlapping utterances in the same speaker turn are handled as follows:
 *    <ul>
 *      <li> If overlap is partial, the start of the second utterance is set to the end of
 *           the first. </li> 
 *      <li> If overlap is total, the two utterances are chained together with a
 *           non-aligned anchor between them. </li> 
 *    </ul>
 *  </li>
 *  <li> Disfluency marking with &amp;+ - e.g. <tt>so &amp;+sund Sunday</tt> </li>
 *  <li> Non-standard form expansion - e.g. <tt>gonna [: going to]</tt> </li>
 *  <li> Incomplete word completion - e.g. <tt>dinner doin(g) all</tt> </li>
 *  <li> Acronym/proper name joining with _ - e.g. <tt>no T_V in my room</tt> </li>
 *  <li> Retracing - e.g. <tt>&lt;some friends and I&gt; [//] uh</tt> or <tt>and sit [//]
 *       sets him</tt> </li> 
 *  <li> Repetition/stuttered false starts - e.g. <tt>the &lt;picnic&gt; [/] picnic</tt>
 *       or <tt>the Saturday [/] in the morning</tt> </li> 
 *  <li> Errors - e.g. <tt>they've &lt;work up a hunger&gt; [* s:r]</tt> or <tt>they got
 *       [* m] to</tt> </li> 
 *  <li> Pauses - untimed, (e.g. <tt>(.)</tt>, <tt>(...)</tt>), 
 *       or timed (e.g. <tt>(0.15)</tt>, <tt>(2.)</tt>, <tt>(1:05.15)</tt>)  </li>
 *  <li> <tt>%mor</tt> line annottions </li>
 * </ul>
 * @author Robert Fromont robert@fromont.net.nz
 */
public class ChatSerialization implements GraphDeserializer, GraphSerializer {
   
  // Attributes:
  private ISO639 iso639 = new ISO639(); // for standard ISO 639 language code processing
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
   * @see #setLanguages(Vector)
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
   * @see #setHeaders(Vector)
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
   * Getter for {@link #mediaType}: Type of media.
   * @return Type of media.
   */
  public String getMediaType() { return mediaType; }
  /**
   * Setter for {@link #mediaType}: Type of media.
   * @param newMediaType Type of media.
   */
  public void setMediaType(String newMediaType) { mediaType = newMediaType; }

  /**
   * List of names of transcribers.
   * @see #getTranscribers()
   * @see #setTranscribers(Vector)
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
   * Layer that marks target participants.
   * @see #getTargetParticipantLayer()
   * @see #setTargetParticipantLayer(Layer)
   */
  protected Layer targetParticipantLayer;
  /**
   * Getter for {@link #targetParticipantLayer}: Layer that marks target participants.
   * @return Layer that marks target participants.
   */
  public Layer getTargetParticipantLayer() { return targetParticipantLayer; }
  /**
   * Setter for {@link #targetParticipantLayer}: Layer that marks target participants.
   * @param newTargetParticipantLayer Layer that marks target participants.
   */
  public ChatSerialization setTargetParticipantLayer(Layer newTargetParticipantLayer) { targetParticipantLayer = newTargetParticipantLayer; return this; }
   
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
   * Layer for non-word events.
   * @see #getNonWordLayer()
   * @see #setNonWordLayer(Layer)
   */
  protected Layer nonWordLayer;
  /**
   * Getter for {@link #nonWordLayer}: Layer for non-word events.
   * @return Layer for non-word events.
   */
  public Layer getNonWordLayer() { return nonWordLayer; }
  /**
   * Setter for {@link #nonWordLayer}: Layer for non-word events.
   * @param newNonWordLayer Layer for non-word events.
   */
  public ChatSerialization setNonWordLayer(Layer newNonWordLayer) { nonWordLayer = newNonWordLayer; return this; }
   
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
   * Errors layer.
   * @see #getErrorsLayer()
   * @see #setErrorsLayer(Layer)
   */
  protected Layer errorsLayer;
  /**
   * Getter for {@link #errorsLayer}: Errors layer.
   * @return Errors layer.
   */
  public Layer getErrorsLayer() { return errorsLayer; }
  /**
   * Setter for {@link #errorsLayer}: Errors layer.
   * @param newErrorsLayer Errors layer.
   */
  public void setErrorsLayer(Layer newErrorsLayer) { errorsLayer = newErrorsLayer; }

  /**
   * Retracing layer.
   * @see #getRetracingLayer()
   * @see #setErrorsLayer(Layer)
   */
  protected Layer retracingLayer;
  /**
   * Getter for {@link #retracingLayer}: Retracing layer.
   * @return Retracing layer.
   */
  public Layer getRetracingLayer() { return retracingLayer; }
  /**
   * Setter for {@link #retracingLayer}: Retracing layer.
   * @param newRetracingLayer Retracing layer.
   */
  public void setRetracingLayer(Layer newRetracingLayer) { retracingLayer = newRetracingLayer; }

  /**
   * Repetitions layer.
   * @see #getRepetitionsLayer()
   * @see #setRepetitionsLayer(Layer)
   */
  protected Layer repetitionsLayer;
  /**
   * Getter for {@link #repetitionsLayer}: Repetitions layer.
   * @return Repetitions layer.
   */
  public Layer getRepetitionsLayer() { return repetitionsLayer; }
  /**
   * Setter for {@link #repetitionsLayer}: Repetitions layer.
   * @param newRepetitionsLayer Repetitions layer.
   */
  public void setRepetitionsLayer(Layer newRepetitionsLayer) { repetitionsLayer = newRepetitionsLayer; }


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
   * Linkage layer.
   * @see #getLinkageLayer()
   * @see #setLinkageLayer(Layer)
   */
  protected Layer linkageLayer;
  /**
   * Getter for {@link #linkageLayer}: Linkage layer.
   * @return Linkage layer.
   */
  public Layer getLinkageLayer() { return linkageLayer; }
  /**
   * Setter for {@link #linkageLayer}: Linkage layer.
   * @param newLinkageLayer Linkage layer.
   */
  public void setLinkageLayer(Layer newLinkageLayer) { linkageLayer = newLinkageLayer; }

  /**
   * C-Unit layer.
   * @see #getCUnitLayer()
   * @see #setCUnitLayer(Layer)
   */
  protected Layer cUnitLayer;
  /**
   * Getter for {@link #cUnitLayer}: C-Unit layer.
   * @return C-Unit layer.
   */
  public Layer getCUnitLayer() { return cUnitLayer; }
  /**
   * Setter for {@link #cUnitLayer}: C-Unit layer.
   * @param newCUnitLayer C-Unit layer.
   */
  public void setCUnitLayer(Layer newCUnitLayer) { cUnitLayer = newCUnitLayer; }
   
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
   * Layer for recording the date of the interaction.
   * @see #getDateLayer()
   * @see #setDateLayer(Layer)
   */
  protected Layer dateLayer;
  /**
   * Getter for {@link #dateLayer}: Layer for recording the date of the interaction.
   * @return Layer for recording the date of the interaction.
   */
  public Layer getDateLayer() { return dateLayer; }
  /**
   * Setter for {@link #dateLayer}: Layer for recording the date of the interaction.
   * @param newDateLayer Layer for recording the date of the interaction.
   */
  public ChatSerialization setDateLayer(Layer newDateLayer) { dateLayer = newDateLayer; return this; }

  /**
   * Layer for recording to location of the interaction.
   * @see #getLocationLayer()
   * @see #setLocationLayer(Layer)
   */
  protected Layer locationLayer;
  /**
   * Getter for {@link #locationLayer}: Layer for recording to location of the interaction.
   * @return Layer for recording to location of the interaction.
   */
  public Layer getLocationLayer() { return locationLayer; }
  /**
   * Setter for {@link #locationLayer}: Layer for recording to location of the interaction.
   * @param newLocationLayer Layer for recording to location of the interaction.
   */
  public ChatSerialization setLocationLayer(Layer newLocationLayer) { locationLayer = newLocationLayer; return this; }

  /**
   * Layer for noting the recording quality.
   * @see #getRecordingQualityLayer()
   * @see #setRecordingQualityLayer(Layer)
   */
  protected Layer recordingQualityLayer;
  /**
   * Getter for {@link #recordingQualityLayer}: Layer for noting the recording quality.
   * @return Layer for noting the recording quality.
   */
  public Layer getRecordingQualityLayer() { return recordingQualityLayer; }
  /**
   * Setter for {@link #recordingQualityLayer}: Layer for noting the recording quality.
   * @param newRecordingQualityLayer Layer for noting the recording quality.
   */
  public ChatSerialization setRecordingQualityLayer(Layer newRecordingQualityLayer) { recordingQualityLayer = newRecordingQualityLayer; return this; }

  /**
   * Layer for recording the room layout.
   * @see #getRoomLayoutLayer()
   * @see #setRoomLayoutLayer(Layer)
   */
  protected Layer roomLayoutLayer;
  /**
   * Getter for {@link #roomLayoutLayer}: Layer for recording the room layout.
   * @return Layer for recording the room layout.
   */
  public Layer getRoomLayoutLayer() { return roomLayoutLayer; }
  /**
   * Setter for {@link #roomLayoutLayer}: Layer for recording the room layout.
   * @param newRoomLayoutLayer Layer for recording the room layout.
   */
  public ChatSerialization setRoomLayoutLayer(Layer newRoomLayoutLayer) { roomLayoutLayer = newRoomLayoutLayer; return this; }

  /**
   * Layer for recording which tape, and where on that tape, the transcript corresponds to.
   * @see #getTapeLocationLayer()
   * @see #setTapeLocationLayer(Layer)
   */
  protected Layer tapeLocationLayer;
  /**
   * Getter for {@link #tapeLocationLayer}: Layer for recording which tape, and where on
   * that tape, the transcript corresponds to. 
   * @return Layer for recording which tape, and where on that tape, the transcript
   * corresponds to. 
   */
  public Layer getTapeLocationLayer() { return tapeLocationLayer; }
  /**
   * Setter for {@link #tapeLocationLayer}: Layer for recording which tape, and where on
   * that tape, the transcript corresponds to. 
   * @param newTapeLocationLayer Layer for recording which tape, and where on that tape,
   * the transcript corresponds to. 
   */
  public ChatSerialization setTapeLocationLayer(Layer newTapeLocationLayer) { tapeLocationLayer = newTapeLocationLayer; return this; }

  /**
   * Required participant meta-data layers.
   * @see #getParticipantLayers()
   * @see #setParticipantLayers(HashMap)
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
   * Layer for %mor annotations.
   * @see #getMorLayer()
   * @see #setMorLayer(Layer)
   */
  protected Layer morLayer;
  /**
   * Getter for {@link #morLayer}: Layer for %mor annotations.
   * @return Layer for %mor annotations.
   */
  public Layer getMorLayer() { return morLayer; }
  /**
   * Setter for {@link #morLayer}: Layer for %mor annotations.
   * @param newMorLayer Layer for %mor annotations.
    */
  public ChatSerialization setMorLayer(Layer newMorLayer) { morLayer = newMorLayer; return this; }
  
  /**
   * Split alternative MOR taggings into separate annotations.
   * @see #getSplitMorTagGroups()
   * @see #setSplitMorTagGroups(Boolean)
   */
  protected Boolean splitMorTagGroups = Boolean.TRUE;
  /**
   * Getter for {@link #splitMorTagGroups}: Split alternative MOR taggings into separate
   * annotations. 
   * @return Split alternative MOR taggings into separate annotations.
   */
  public Boolean getSplitMorTagGroups() { return splitMorTagGroups; }
  /**
   * Setter for {@link #splitMorTagGroups}: Split alternative MOR taggings into separate
   * annotations. 
   * @param newSplitMorTagGroups Split alternative MOR taggings into separate annotations.
   */
  public ChatSerialization setSplitMorTagGroups(Boolean newSplitMorTagGroups) { splitMorTagGroups = newSplitMorTagGroups; return this; }
  
  /**
   * Split MOR word groups into separate annotations. This is only supported when
   * {@link #getSplitMorTagGroups()} is true.
   * @see #getSplitMorWordGroups()
   * @see #setSplitMorWordGroups(Booelan)
   */
  protected Boolean splitMorWordGroups = Boolean.TRUE;
  /**
   * Getter for {@link #splitMorWordGroups}: Split MOR word groups into separate
   * annotations.This is only supported when {@link #getSplitMorTagGroups()} is true.
   * @return Split MOR word groups into separate annotations.
   */
  public Boolean getSplitMorWordGroups() { return splitMorWordGroups; }
  /**
   * Setter for {@link #splitMorWordGroups}: Split MOR word groups into separate annotations.
   * @param newSplitMorWordGroups Split MOR word groups into separate annotations.
   */
  public ChatSerialization setSplitMorWordGroups(Boolean newSplitMorWordGroups) { splitMorWordGroups = newSplitMorWordGroups; return this; }
  
  /**
   * Layer for prefixes in MOR tags.
   * @see #getMorPrefixLayer()
   * @see #setMorPrefixLayer(Layer)
   */
  protected Layer morPrefixLayer;
  /**
   * Getter for {@link #morPrefixLayer}: Layer for prefixes in MOR tags.
   * @return Layer for prefixes in MOR tags.
   */
  public Layer getMorPrefixLayer() { return morPrefixLayer; }
  /**
   * Setter for {@link #morPrefixLayer}: Layer for prefixes in MOR tags.
   * @param newMorPrefixLayer Layer for prefixes in MOR tags.
   */
  public ChatSerialization setMorPrefixLayer(Layer newMorPrefixLayer) { morPrefixLayer = newMorPrefixLayer; return this; }
  
  /**
   * Layer for parts-of-speech in MOR tags.
   * @see #getMorPartOfSpeechLayer()
   * @see #setMorPartOfSpeechLayer(Layer)
   */
  protected Layer morPartOfSpeechLayer;
  /**
   * Getter for {@link #morPartOfSpeechLayer}: Layer for parts-of-speech in MOR tags.
   * @return Layer for parts-of-speech in MOR tags.
   */
  public Layer getMorPartOfSpeechLayer() { return morPartOfSpeechLayer; }
  /**
   * Setter for {@link #morPartOfSpeechLayer}: Layer for parts-of-speech in MOR tags.
   * @param newMorPartOfSpeechLayer Layer for parts-of-speech in MOR tags.
   */
  public ChatSerialization setMorPartOfSpeechLayer(Layer newMorPartOfSpeechLayer) { morPartOfSpeechLayer = newMorPartOfSpeechLayer; return this; }
  
  /**
   * Layer for part-of-speech subcategories in MOR tags.
   * @see #getMorPartOfSpeechSubcategoryLayer()
   * @see #setMorPartOfSpeechSubcategoryLayer(Layer)
   */
  protected Layer morPartOfSpeechSubcategoryLayer;
  /**
   * Getter for {@link #morPartOfSpeechSubcategoryLayer}: Layer for part-of-speech
   * subcategories in MOR tags. 
   * @return Layer for part-of-speech subcategories in MOR tags.
   */
  public Layer getMorPartOfSpeechSubcategoryLayer() { return morPartOfSpeechSubcategoryLayer; }
  /**
   * Setter for {@link #morPartOfSpeechSubcategoryLayer}: Layer for part-of-speech
   * subcategories in MOR tags. 
   * @param newMorPartOfSpeechSubcategoryLayer Layer for part-of-speech subcategories in MOR tags.
   */
  public ChatSerialization setMorPartOfSpeechSubcategoryLayer(Layer newMorPartOfSpeechSubcategoryLayer) { morPartOfSpeechSubcategoryLayer = newMorPartOfSpeechSubcategoryLayer; return this; }
  
  /**
   * Layer for stems in MOR tags.
   * @see #getMorStemLayer()
   * @see #setMorStemLayer(Layer)
   */
  protected Layer morStemLayer;
  /**
   * Getter for {@link #morStemLayer}: Layer for stems in MOR tags.
   * @return Layer for stems in MOR tags.
   */
  public Layer getMorStemLayer() { return morStemLayer; }
  /**
   * Setter for {@link #morStemLayer}: Layer for stems in MOR tags.
   * @param newMorStemLayer Layer for stems in MOR tags.
   */
  public ChatSerialization setMorStemLayer(Layer newMorStemLayer) { morStemLayer = newMorStemLayer; return this; }
  
  /**
   * Layer for fusional suffixes in MOR tags.
   * @see #getMorFusionalSuffixLayer()
   * @see #setMorFusionalSuffixLayer(Layer)
   */
  protected Layer morFusionalSuffixLayer;
  /**
   * Getter for {@link #morFusionalSuffixLayer}: Layer for fusional suffixes in MOR tags.
   * @return Layer for fusional suffixes in MOR tags.
   */
  public Layer getMorFusionalSuffixLayer() { return morFusionalSuffixLayer; }
  /**
   * Setter for {@link #morFusionalSuffixLayer}: Layer for fusional suffixes in MOR tags.
   * @param newMorFusionalSuffixLayer Layer for fusional suffixes in MOR tags.
   */
  public ChatSerialization setMorFusionalSuffixLayer(Layer newMorFusionalSuffixLayer) { morFusionalSuffixLayer = newMorFusionalSuffixLayer; return this; }
  
  /**
   * Layer for (non-fusional) suffixes in MOR tags.
   * @see #getMorSuffixLayer()
   * @see #setMorSuffixLayer(Layer)
   */
  protected Layer morSuffixLayer;
  /**
   * Getter for {@link #morSuffixLayer}: Layer for (non-fusional) suffixes in MOR tags.
   * @return Layer for (non-fusional) suffixes in MOR tags.
   */
  public Layer getMorSuffixLayer() { return morSuffixLayer; }
  /**
   * Setter for {@link #morSuffixLayer}: Layer for (non-fusional) suffixes in MOR tags.
   * @param newMorSuffixLayer Layer for (non-fusional) suffixes in MOR tags.
   */
  public ChatSerialization setMorSuffixLayer(Layer newMorSuffixLayer) { morSuffixLayer = newMorSuffixLayer; return this; }
  
  /**
   * Layer for English glosses in MOR tags.
   * @see #getMorGlossLayer()
   * @see #setMorGlossLayer(Layer)
   */
  protected Layer morGlossLayer;
  /**
   * Getter for {@link #morGlossLayer}: Layer for English glosses in MOR tags.
   * @return Layer for English glosses in MOR tags.
   */
  public Layer getMorGlossLayer() { return morGlossLayer; }
  /**
   * Setter for {@link #morGlossLayer}: Layer for English glosses in MOR tags.
   * @param newMorGlossLayer Layer for English glosses in MOR tags.
   */
  public ChatSerialization setMorGlossLayer(Layer newMorGlossLayer) { morGlossLayer = newMorGlossLayer; return this; }
  
  /**
   * Layer for unfilled pause annotations.
   * @see #getPauseLayer()
   * @see #setPauseLayer(Layer)
   */
  protected Layer pauseLayer;
  /**
   * Getter for {@link #pauseLayer}: Layer for unfilled pause annotations.
   * @return Layer for unfilled pause annotations.
   */
  public Layer getPauseLayer() { return pauseLayer; }
  /**
   * Setter for {@link #pauseLayer}: Layer for unfilled pause annotations.
   * @param newPauseLayer Layer for unfilled pause annotations.
   */
  public ChatSerialization setPauseLayer(Layer newPauseLayer) { pauseLayer = newPauseLayer; return this; }

  /**
   * Whether to include utterance time codes when exporting transcripts.
   * @see #getIncludeTimeCodes()
   * @see #setIncludeTimeCodes(Boolean)
   */
  protected Boolean includeTimeCodes = Boolean.TRUE;
  /**
   * Getter for {@link #includeTimeCodes}: Whether to include utterance time codes when exporting transcripts.
   * @return Whether to include utterance time codes when exporting transcripts.
   */
  public Boolean getIncludeTimeCodes() { return includeTimeCodes; }
  /**
   * Setter for {@link #includeTimeCodes}: Whether to include utterance time codes when exporting transcripts.
   * @param newIncludeTimeCodes Whether to include utterance time codes when exporting transcripts.
   */
  public ChatSerialization setIncludeTimeCodes(Boolean newIncludeTimeCodes) { includeTimeCodes = newIncludeTimeCodes; return this; }
      
  /**
   * Utterance tokenizer.  The default is {@link SimpleTokenizer}.
   * @see #getTokenizer()
   * @see #setTokenizer(GraphTransformer)
   */
  protected GraphTransformer tokenizer;
  /**
   * Getter for {@link #tokenizer}: Utterance tokenizer.
   * @return Utterance tokenizer.
   */
  public GraphTransformer getTokenizer() { return tokenizer; }
  /**
   * Setter for {@link #tokenizer}: Utterance tokenizer.
   * @param newTokenizer Utterance tokenizer.
   */
  public void setTokenizer(GraphTransformer newTokenizer) { tokenizer = newTokenizer; }
   
  private long graphCount = 0;
  private long consumedGraphCount = 0;
  /**
   * Determines how far through the serialization is.
   * @return An integer between 0 and 100 (inclusive), or null if progress can not be calculated.
   */
  public Integer getPercentComplete() {
    if (graphCount < 0) return null;
    return (int)((consumedGraphCount * 100) / graphCount);
  }

  /**
   * Serialization marked for cancelling.
   * @see #getCancelling()
   * @see #setCancelling(boolean)
   */
  protected boolean cancelling;
  /**
   * Getter for {@link #cancelling}: Serialization marked for cancelling.
   * @return Serialization marked for cancelling.
   */
  public boolean getCancelling() { return cancelling; }
  /**
   * Setter for {@link #cancelling}: Serialization marked for cancelling.
   * @param newCancelling Serialization marked for cancelling.
   */
  public ChatSerialization setCancelling(boolean newCancelling) { cancelling = newCancelling; return this; }
   
  /**
   * Cancel the serialization in course (if any).
   */
  public void cancel() {
    setCancelling(true);
  }
   
  // Methods:
   
  /**
   * Default constructor.
   */
  public ChatSerialization() {
    participantLayers = new HashMap<String,Layer>();
    participantLayers.put("language", null);
    participantLayers.put("corpus", null);
    participantLayers.put("age", null);
    participantLayers.put("sex", null);
    participantLayers.put("group", null);
    participantLayers.put("SES", null);
    participantLayers.put("role", null);
    participantLayers.put("education", null);
    participantLayers.put("custom", null);
  } // end of constructor
   
  /**
   * Resete state.
   */
  public void reset() {
    warnings = new Vector<String>();
    languages = new Vector<String>();
    mediaName = null;
    mediaType = null;
    participants = new HashMap<String,HashMap<String,String>>();
    transcribers = new Vector<String>();
    lines = new Vector<String>();
    headers = new Vector<String>();
  } // end of reset()

  // IStreamDeserializer methods:

  /**
   * Returns the deserializer's descriptor
   * @return The deserializer's descriptor
   */
  public SerializationDescriptor getDescriptor() {
    return new SerializationDescriptor(
      "CLAN CHAT transcript", getClass().getPackage().getImplementationVersion(),
      "text/x-chat", ".cha", "1.0.0", getClass().getResource("icon.png"));
  }

  /**
   * Sets parameters for deserializer as a whole.  This might include database connection
   * parameters, locations of supporting files, etc.
   * <p>When the deserializer is installed, this method should be invoked with an empty parameter
   * set, to discover what (if any) general configuration is required. If parameters are returned,
   * and user interaction is possible, then the user may be presented with an interface for
   * setting/confirming these parameters. Once the parameters are set, this method can be
   * invoked again with the required values. 
   * @param schema The layer schema, definining layers and the way they interrelate.
   * @return A list of configuration parameters that must be set before
   * {@link GraphDeserializer#setParameters(ParameterSet)} can be invoked. If this is an
   * empty list, {@link GraphDeserializer#setParameters(ParameterSet)} can be invoked. 
   * If it's not an empty list,  this method must be invoked again with the returned
   * parameters' values set. 
   */
  public ParameterSet configure(ParameterSet configuration, Schema schema) {
    setSchema(schema);
    setParticipantLayer(schema.getParticipantLayer());
    setTurnLayer(schema.getTurnLayer());
    setUtteranceLayer(schema.getUtteranceLayer());
    setWordLayer(schema.getWordLayer());
    linkageLayer = null;
    cUnitLayer = null;
    gemLayer = null;
    transcriberLayer = null;
    languagesLayer = null;
    targetParticipantLayer = null;
    expansionLayer = null;
    errorsLayer = null;
    retracingLayer = null;
    repetitionsLayer = null;
    completionLayer = null;
    disfluencyLayer = null;
    nonWordLayer = null;
    morLayer = null;
    morPrefixLayer = null;
    morPartOfSpeechLayer = null;
    morPartOfSpeechSubcategoryLayer = null;
    morStemLayer = null;
    morFusionalSuffixLayer = null;
    morSuffixLayer = null;
    morGlossLayer = null;

    pauseLayer = null;

    if (configuration.size() > 0) {
      configuration.apply(this);
      for (String attribute : participantLayers.keySet()) {
        if (configuration.containsKey(attribute + "Layer")) {
          participantLayers.put(attribute, ((Layer)configuration.get(attribute + "Layer").getValue()));
        }
      }
    }

    LinkedHashMap<String,Layer> possibleParticipantLayers = new LinkedHashMap<String,Layer>();
    LinkedHashMap<String,Layer> possibleTurnLayers = new LinkedHashMap<String,Layer>();
    LinkedHashMap<String,Layer> possibleTurnChildLayers = new LinkedHashMap<String,Layer>();
    LinkedHashMap<String,Layer> wordTagLayers = new LinkedHashMap<String,Layer>();
    LinkedHashMap<String,Layer> wordChildLayers = new LinkedHashMap<String,Layer>();
    LinkedHashMap<String,Layer> participantTagLayers = new LinkedHashMap<String,Layer>();
    if (getParticipantLayer() == null || getTurnLayer() == null 
        || getUtteranceLayer() == null || getWordLayer() == null) {
      for (Layer top : schema.getRoot().getChildren().values()) {
        if (top.getAlignment() == Constants.ALIGNMENT_NONE) {
          if (top.getChildren().size() == 0) { // unaligned childless children of graph
            participantTagLayers.put(top.getId(), top);
          } else { // unaligned children of graph, with children of their own
            possibleParticipantLayers.put(top.getId(), top);
            for (Layer turn : top.getChildren().values()) {
              if (turn.getAlignment() == Constants.ALIGNMENT_INTERVAL
                  && turn.getChildren().size() > 0) {
                // aligned children of who with their own children
                possibleTurnLayers.put(turn.getId(), turn);
                for (Layer turnChild : turn.getChildren().values()) {
                  if (turnChild.getAlignment() == Constants.ALIGNMENT_INTERVAL) {
                    // aligned children of turn
                    possibleTurnChildLayers.put(turnChild.getId(), turnChild);
                    for (Layer tag : turnChild.getChildren().values()) {
                      if (tag.getAlignment() == Constants.ALIGNMENT_NONE) {
                        // unaligned children of word
                        wordTagLayers.put(tag.getId(), tag);
                      }
                      wordChildLayers.put(tag.getId(), tag);
                    } // next possible word tag layer
                  }
                } // next possible turn child layer
              }
            } // next possible turn layer
          } // with children
        } // unaligned
      } // next possible participant layer
    } else {
      for (Layer turnChild : getTurnLayer().getChildren().values()) {
        if (turnChild.getAlignment() == Constants.ALIGNMENT_INTERVAL) {
          possibleTurnChildLayers.put(turnChild.getId(), turnChild);
        }
      } // next possible word tag layer
      for (Layer tag : getWordLayer().getChildren().values()) {
        if (tag.getAlignment() == Constants.ALIGNMENT_NONE
            && tag.getChildren().size() == 0) {
          wordTagLayers.put(tag.getId(), tag);
        }
        wordChildLayers.put(tag.getId(), tag);
      } // next possible word tag layer
      for (Layer tag : getParticipantLayer().getChildren().values()) {
        if (tag.getAlignment() == Constants.ALIGNMENT_NONE
            && tag.getChildren().size() == 0) {
          participantTagLayers.put(tag.getId(), tag);
        }
      } // next possible word tag layer
    }
    LinkedHashMap<String,Layer> graphTagLayers = new LinkedHashMap<String,Layer>();
    LinkedHashMap<String,Layer> graphSpanLayers = new LinkedHashMap<String,Layer>();
    for (Layer top : schema.getRoot().getChildren().values()) {
      if (top.getChildren().size() == 0) {
        if (top.getAlignment() == Constants.ALIGNMENT_NONE) {
          graphTagLayers.put(top.getId(), top);
        } else {
          graphSpanLayers.put(top.getId(), top);
        }
      } // childless
    } // next top level layer
         
    if (getParticipantLayer() == null) {
      Parameter p = configuration.containsKey("participantLayer")?configuration.get("participantLayer")
        :configuration.addParameter(
          new Parameter("participantLayer", Layer.class, "Participant layer", "Layer for speaker/participant identification", true));
      String[] possibilities = {"participant","participants","who","speaker","speakers"};
      if (p.getValue() == null) p.setValue(
        Utility.FindLayerById(possibleParticipantLayers, Arrays.asList(possibilities)));
      p.setPossibleValues(possibleParticipantLayers.values());
    }
    if (getTurnLayer() == null) {
      Parameter p = configuration.containsKey("turnLayer")?configuration.get("turnLayer")
        :configuration.addParameter(
          new Parameter("turnLayer", Layer.class, "Turn layer", "Layer for speaker turns", true));
      String[] possibilities = {"turn","turns"};
      if (p.getValue() == null) p.setValue(
        Utility.FindLayerById(possibleTurnLayers, Arrays.asList(possibilities)));
      p.setPossibleValues(possibleTurnLayers.values());
    }
    if (getUtteranceLayer() == null) {
      Parameter p = configuration.containsKey("utteranceLayer")?configuration.get("utteranceLayer")
        :configuration.addParameter(
          new Parameter("utteranceLayer", Layer.class, "Utterance layer", "Layer for speaker utterances", true));
      String[] possibilities = {"utterance","utterances","line","lines"};
      if (p.getValue() == null) p.setValue(
        Utility.FindLayerById(possibleTurnChildLayers, Arrays.asList(possibilities)));
      p.setPossibleValues(possibleTurnChildLayers.values());
    }
    if (getWordLayer() == null) {
      Parameter p = configuration.containsKey("wordLayer")?configuration.get("wordLayer")
        :configuration.addParameter(
          new Parameter("wordLayer", Layer.class, "Word layer", "Layer for individual word tokens", true));
      String[] possibilities = {"transcript","word","words","w"};
      if (p.getValue() == null) p.setValue(
        Utility.FindLayerById(possibleTurnChildLayers, Arrays.asList(possibilities)));
      p.setPossibleValues(possibleTurnChildLayers.values());
    }
    Parameter pC = configuration.containsKey("cUnitLayer")?configuration.get("cUnitLayer")
      :configuration.addParameter(
        new Parameter("cUnitLayer", Layer.class, "C-Unit layer", "Layer for marking c-units"));
    String[] possibilitiesC = {"c-unit","cunit","sentence"};
    pC.setValue(Utility.FindLayerById(possibleTurnChildLayers, Arrays.asList(possibilitiesC)));
    pC.setPossibleValues(possibleTurnChildLayers.values());

    Parameter p = configuration.containsKey("disfluencyLayer")?configuration.get("disfluencyLayer")
      :configuration.addParameter(
        new Parameter("disfluencyLayer", Layer.class, "Disfluency layer", "Layer for disfluency annotations"));
    String[] possibilities_disfluency = {"disfluency","disfluencies"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(wordTagLayers, Arrays.asList(possibilities_disfluency)));
    p.setPossibleValues(wordTagLayers.values());

    p = configuration.containsKey("nonWordLayer")?configuration.get("nonWordLayer")
      :configuration.addParameter(
        new Parameter("nonWordLayer", Layer.class, "Non-word layer", "Layer for non-word noises"));
    String[] possibilities_nonword = {"noise","noises","nonword","background"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(graphSpanLayers, Arrays.asList(possibilities_nonword)));
    p.setPossibleValues(graphSpanLayers.values());

    p = configuration.containsKey("expansionLayer")?configuration.get("expansionLayer")
      :configuration.addParameter(
        new Parameter("expansionLayer", Layer.class, "Expansion layer", "Layer for expansion annotations"));
    String[] possibilities_expansion = {"expansion","expansions"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(wordTagLayers, Arrays.asList(possibilities_expansion)));
    p.setPossibleValues(wordTagLayers.values());

    p = configuration.containsKey("errorsLayer")?configuration.get("errorsLayer")
      :configuration.addParameter(
        new Parameter("errorsLayer", Layer.class, "Errors layer", "Layer for error  annotations"));
    String[] possibilities_error = {"error","error"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(possibleTurnChildLayers, Arrays.asList(possibilities_error)));
    p.setPossibleValues(possibleTurnChildLayers.values());

    p = configuration.containsKey("linkageLayer")?configuration.get("linkageLayer")
      :configuration.addParameter(
        new Parameter("linkageLayer", Layer.class, "Linkages layer", "Layer for linkage annotations"));
    String[] possibilities_linkage = {"linkage","linkages"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(possibleTurnChildLayers, Arrays.asList(possibilities_linkage)));
    p.setPossibleValues(possibleTurnChildLayers.values());

    p = configuration.containsKey("repetitionsLayer")?configuration.get("repetitionsLayer")
      :configuration.addParameter(
        new Parameter("repetitionsLayer", Layer.class, "Repetitions layer", "Layer for repetition annotations"));
    String[] possibilities_repetition = {"repetition","repetitions"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(possibleTurnChildLayers, Arrays.asList(possibilities_repetition)));
    p.setPossibleValues(possibleTurnChildLayers.values());

    p = configuration.containsKey("retracingLayer")?configuration.get("retracingLayer")
      :configuration.addParameter(
        new Parameter(
          "retracingLayer", Layer.class, "Retracing layer", "Layer for retracing annotations"));
    String[] possibilities_retrace = {"retrace","retracing","correction"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(possibleTurnChildLayers, Arrays.asList(possibilities_retrace)));
    p.setPossibleValues(possibleTurnChildLayers.values());

    p = configuration.containsKey("pauseLayer")?
      configuration.get("pauseLayer"):
      configuration.addParameter(
        new Parameter(
          "pauseLayer", Layer.class, "Pause Layer", "Layer for marking unfilled pauses"));
    String[] possibilities_pause = { "pause", "pauses", "silence" };
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(possibleTurnChildLayers, Arrays.asList(possibilities_pause)));
    p.setPossibleValues(possibleTurnChildLayers.values());

    p = configuration.containsKey("completionLayer")?configuration.get("completionLayer")
      :configuration.addParameter(
        new Parameter(
          "completionLayer", Layer.class, "Completion layer", "Layer for completion annotations"));
    String[] possibilities_completion = {"completion","completions"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(wordTagLayers, Arrays.asList(possibilities_completion)));
    p.setPossibleValues(wordTagLayers.values());

    p = configuration.containsKey("morLayer")?configuration.get("morLayer")
      :configuration.addParameter(
        new Parameter("morLayer", Layer.class, "MOR layer", "Layer for morphosyntactic tags"));
    String[] possibilities_mor = {"mor","morphosyntax"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(wordChildLayers, Arrays.asList(possibilities_mor)));
    p.setPossibleValues(wordChildLayers.values());
    
    p = configuration.containsKey("morPrefixLayer")?configuration.get("morPrefixLayer")
      :configuration.addParameter(
        new Parameter("morPrefixLayer", Layer.class, "MOR prefix layer",
                      "Layer for prefixes in MOR tags"));
    String[] possibilities_mor_prefix = {"morprefix","prefix"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(wordChildLayers, Arrays.asList(possibilities_mor_prefix)));
    p.setPossibleValues(wordChildLayers.values());
    
    p = configuration.containsKey("morPartOfSpeechLayer")?configuration.get("morPartOfSpeechLayer")
      :configuration.addParameter(
        new Parameter("morPartOfSpeechLayer", Layer.class, "MOR POS layer",
                      "Layer for parts of speech in MOR tags"));
    String[] possibilities_mor_pos = {"morpos","morpartofspeech", "pos", "partofspeech"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(wordChildLayers, Arrays.asList(possibilities_mor_pos)));
    p.setPossibleValues(wordChildLayers.values());

    p = configuration.containsKey("morPartOfSpeechSubcategoryLayer")
      ?configuration.get("morPartOfSpeechSubcategoryLayer")
      :configuration.addParameter(
        new Parameter("morPartOfSpeechSubcategoryLayer", Layer.class, "MOR POS Subcategory layer",
                      "Layer for subcategories of parts of speech in MOR tags"));
    String[] possibilities_mor_pos_sc = {"morpossubcategory","morpartofspeechsubcategory", "possubcategory", "partofspeechsubcategory"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(wordChildLayers, Arrays.asList(possibilities_mor_pos_sc)));
    p.setPossibleValues(wordChildLayers.values());

    p = configuration.containsKey("morStemLayer")?configuration.get("morStemLayer")
      :configuration.addParameter(
        new Parameter("morStemLayer", Layer.class, "MOR Stem layer",
                      "Layer for stems in MOR tags"));
    String[] possibilities_mor_stem = {"morstem","morroot","stem","root"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(wordChildLayers, Arrays.asList(possibilities_mor_stem)));
    p.setPossibleValues(wordChildLayers.values());

    p = configuration.containsKey("morFusionalSuffixLayer")
      ?configuration.get("morFusionalSuffixLayer")
      :configuration.addParameter(
        new Parameter("morFusionalSuffixLayer", Layer.class, "MOR Fusional Suffix layer",
                      "Layer for fusional suffixes in MOR tags"));
    String[] possibilities_mor_fs = {"morfusionalsuffix","fusionalsuffix"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(wordChildLayers, Arrays.asList(possibilities_mor_fs)));
    p.setPossibleValues(wordChildLayers.values());

    p = configuration.containsKey("morSuffixLayer")?configuration.get("morSuffixLayer")
      :configuration.addParameter(
        new Parameter("morSuffixLayer", Layer.class, "MOR Suffix layer",
                      "Layer for (non-fusional) suffixes in MOR tags"));
    String[] possibilities_mor_suffix = {"morsuffix","suffix"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(wordChildLayers, Arrays.asList(possibilities_mor_suffix)));
    p.setPossibleValues(wordChildLayers.values());

    p = configuration.containsKey("morGlossLayer")?configuration.get("morGlossLayer")
      :configuration.addParameter(
        new Parameter("morGlossLayer", Layer.class, "MOR Gloss layer",
                      "Layer for English glosses in MOR tags"));
    String[] possibilities_mor_gloss = {"morenglish","morgloss","gloss"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(wordChildLayers, Arrays.asList(possibilities_mor_gloss)));
    p.setPossibleValues(wordChildLayers.values());

    LinkedHashMap<String,Layer> possibleLayers = new LinkedHashMap<String,Layer>();
    for (Layer top : schema.getRoot().getChildren().values()) {
      if (top.getAlignment() == Constants.ALIGNMENT_INTERVAL) { // aligned children of graph
        possibleLayers.put(top.getId(), top);
      }
    } // next top level layer
    p = configuration.containsKey("gemLayer")?configuration.get("gemLayer")
      :configuration.addParameter(
        new Parameter("gemLayer", Layer.class, "Gem layer", "Layer for gems"));
    String[] possibilities_gem = {"gem","gems","topic","topics"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(possibleLayers, Arrays.asList(possibilities_gem)));
    p.setPossibleValues(possibleLayers.values());

    graphTagLayers.remove("corpus");
    graphTagLayers.remove("transcript_type");
    p = configuration.containsKey("transcriberLayer")?configuration.get("transcriberLayer")
      :configuration.addParameter(
        new Parameter("transcriberLayer", Layer.class, "Transcriber layer", "Layer for transcriber name"));
    String[] possibilities_transcriber = {"transcriber","transcribers","transcript_transcriber","transcript_transcribers", "scribe","scribes", "transcript_scribe","transcript_scribes"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(graphTagLayers, Arrays.asList(possibilities_transcriber)));
    p.setPossibleValues(graphTagLayers.values());

    p = configuration.containsKey("languagesLayer")?configuration.get("languagesLayer")
      :configuration.addParameter(
        new Parameter("languagesLayer", Layer.class, "Transcript language layer", "Layer for transcriber language"));
    String[] possibilities_language = {"transcript_language","transcript_languages","language","languages"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(graphTagLayers, Arrays.asList(possibilities_language)));
    p.setPossibleValues(graphTagLayers.values());
      
    p = configuration.containsKey("dateLayer")?configuration.get("dateLayer")
      :configuration.addParameter(
        new Parameter("dateLayer", Layer.class, "Date layer",
                      "Layer for date of the interaction"));
    String[] possibilities_date = {
      "transcriptairdate","airdate", "transcriptrecordingdate","recordingdate",
      "transcriptcreationdate","creationdate", "transcriptdate","date"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(graphTagLayers, Arrays.asList(possibilities_date)));
    p.setPossibleValues(graphTagLayers.values());
      
    p = configuration.containsKey("locationLayer")?configuration.get("locationLayer")
      :configuration.addParameter(
        new Parameter("locationLayer", Layer.class, "Location layer",
                      "Layer for location of the interaction"));
    String[] possibilities_location = { "transcriptlocation","location" };
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(graphTagLayers, Arrays.asList(possibilities_location)));
    p.setPossibleValues(graphTagLayers.values());
      
    p = configuration.containsKey("recordingQualityLayer")?
      configuration.get("recordingQualityLayer") :configuration.addParameter(
        new Parameter("recordingQualityLayer", Layer.class, "Recording Quality layer",
                      "Layer for recording quality"));
    String[] possibilities_quality = {
      "transcriptrecordingquality","recordingquality", "transcriptquality","quality"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(graphTagLayers, Arrays.asList(possibilities_quality)));
    p.setPossibleValues(graphTagLayers.values());
      
    p = configuration.containsKey("roomLayoutLayer")?configuration.get("roomLayoutLayer")
      :configuration.addParameter(
        new Parameter("roomLayoutLayer", Layer.class, "Room Layout layer",
                      "Layer for room layout"));
    String[] possibilities_layout = {
      "transcriptroomlayout","roomlayout", "transcriptlayout","layout"};
    if (p.getValue() == null) p.setValue(
      Utility.FindLayerById(graphTagLayers, Arrays.asList(possibilities_layout)));
    p.setPossibleValues(graphTagLayers.values());
      
    p = configuration.containsKey("tapeLocationLayer")?configuration.get("tapeLocationLayer")
      :configuration.addParameter(
        new Parameter("tapeLocationLayer", Layer.class, "Tape Location layer",
                      "Layer for tape and location on the tape covered by the transcription"));
    String[] possibilities_tape_location = {
      "transcripttapelocation","tapelocation"};
    if (p.getValue() == null) {
      p.setValue(Utility.FindLayerById(
                   graphTagLayers, Arrays.asList(possibilities_tape_location)));
    }
    p.setPossibleValues(graphTagLayers.values());
      
    // target participant layer
    p = configuration.containsKey("targetParticipantLayer")?configuration.get("targetParticipantLayer")
      :configuration.addParameter(
        new Parameter("targetParticipantLayer", Layer.class, "Target participant layer",
                      "Layer for identifying target participants"));
    // if we have a layer called that
    String[] possibilities_target_participant = {
      "main_participant", "main", "target", "participant_target"};
    if (p.getValue() == null) {
      p.setValue(Utility.FindLayerById(
                   participantTagLayers, Arrays.asList(possibilities_target_participant)));
    }
    p.setPossibleValues(participantTagLayers.values());
      
    // participant meta data layers
    for (String attribute : participantLayers.keySet()) {
      p = configuration.containsKey(attribute + "Layer")?configuration.get(attribute + "Layer")
        :configuration.addParameter(
          new Parameter(attribute + "Layer", Layer.class, attribute + " layer", "Layer for " + attribute));
      // if we have a layer called that
      String[] possibilities_participant = {"participant_"+attribute, attribute};
      // for sex we'll include gender layers as possibilities
      if (attribute.equals("sex")) {
        String[] possibilities_sex_gender
          = {"participant_"+attribute, attribute, "participant_gender", "gender"};
        possibilities_participant = possibilities_sex_gender;
      }
      if (p.getValue() == null) p.setValue(
        Utility.FindLayerById(participantTagLayers, Arrays.asList(possibilities_participant)));
      p.setPossibleValues(participantTagLayers.values());
    }
      
    Parameter includeTimeCodes = configuration.get("includeTimeCodes");
    if (includeTimeCodes == null) {
      includeTimeCodes = new Parameter(
        "includeTimeCodes", Boolean.class, "Include Time Codes",
        "Include utterance sychronization information when exporting transcripts");
      configuration.addParameter(includeTimeCodes);
    }
    if (includeTimeCodes.getValue() == null) {
      includeTimeCodes.setValue(this.includeTimeCodes);
    }

    Parameter splitMorTagGroups = configuration.get("splitMorTagGroups");
    if (splitMorTagGroups == null) {
      splitMorTagGroups = new Parameter(
        "splitMorTagGroups", Boolean.class, "Split MOR Tag Groups",
        "Split alternative MOR taggings into separate annotations");
      configuration.addParameter(splitMorTagGroups);
    }
    if (splitMorTagGroups.getValue() == null) {
      splitMorTagGroups.setValue(this.splitMorTagGroups);
    }
    
    Parameter splitMorWordGroups = configuration.get("splitMorWordGroups");
    if (splitMorWordGroups == null) {
      splitMorWordGroups = new Parameter(
        "splitMorWordGroups", Boolean.class, "Split MOR Word Groups",
        "Split MOR word morphemes (clitics, components of compounds ) into separate annotations. This is only supported when Split MOR Tag Groups is also enabled.");
      configuration.addParameter(splitMorWordGroups);
    }
    if (splitMorWordGroups.getValue() == null) {
      splitMorWordGroups.setValue(this.splitMorWordGroups);
    }

    return configuration;
  }

  /**
   * Loads the serialized form of the graph, using the given set of named streams.
   * @param streams A list of named streams that contain all the transcription/annotation
   * data required. 
   * @param schema The layer schema, definining layers and the way they interrelate.
   * @return A list of parameters that require setting before 
   * {@link GraphDeserializer#deserialize()} can be invoked. This may be an empty list, and may
   * include parameters with the value already set to a workable default. If there are
   * parameters, and user interaction is possible, then the user may be presented with an
   * interface for setting/confirming these parameters, before they are then passed to
   * {@link GraphDeserializer#setParameters(ParameterSet)}. 
   * @throws SerializationException If the graph could not be loaded.
   * @throws IOException On IO error.
   * @throws SerializerNotConfiguredException If the configuration is not sufficient for
   * deserialization. 
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public ParameterSet load(NamedStream[] streams, Schema schema)
    throws IOException, SerializationException, SerializerNotConfiguredException {
    // take the first cha stream, ignore all others.
    NamedStream cha = null;
    for (NamedStream stream : streams) {	 
      if (stream.getName().toLowerCase().endsWith(".cha") 
          || "text/x-chat".equals(stream.getMimeType())) {
        cha = stream;
        break;
      }
    } // next stream
    if (cha == null) throw new SerializationException("No CHAT stream found");
    setName(cha.getName());

    reset();

    // read stream line by line
    boolean inHeader = true;
    BufferedReader reader = new BufferedReader(new InputStreamReader(cha.getStream(), "UTF-8"));
    String line = reader.readLine();
    while (line != null) {
      if (line.startsWith("@G") || line.startsWith("@Bg") || line.startsWith("*")) {
        inHeader = false;
      }

      if (inHeader) {
        if (line.startsWith("@")) { // new header
          headers.add(line);
        } else { // continuation of last header line
          // remove the last header
          String lastHeader = headers.remove(headers.size()-1);
          // append this line to it
          headers.add(lastHeader + " " + line.trim());
        }
      } else if (!line.equals("@End")) { // transcript line
        // is it the first line?
        if (lines.size() == 0 
            // or this line starts with a speaker ID
            || line.startsWith("*")
            // or the last line was time synchronized?
            || lines.lastElement().endsWith("")
            // or the line is a 'gem'?
            || line.startsWith("@")
            // or the line is a dependent tier?
            || line.startsWith("%")
          ) { // this is a new utterance
          lines.add(line);
        } else { // last line was not time-synchronized, so append this line to it
          // remove the last line
          String lastLine = lines.remove(lines.size()-1);
          // append this line to it
          lines.add(lastLine + " " + line);
        }
      }

      line = reader.readLine();
    } // next line

    return new ParameterSet(); // everything is in configure()
  }
   
  /**
   * Sets parameters for a given deserialization operation, after loading the serialized form of the graph. This might include mappings from format-specific objects like tiers to graph layers, etc.
   * @param parameters The configuration for a given deserialization operation.
   * @throws SerializationParametersMissingException If not all required parameters have a value.
   */
  public void setParameters(ParameterSet parameters)
    throws SerializationParametersMissingException {
    if (getParticipantLayer() == null || getTurnLayer() == null
        || getUtteranceLayer() == null || getWordLayer() == null) {
      throw new SerializationParametersMissingException();
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
   * @throws SerializerNotConfiguredException if the object has not been configured.
   * @throws SerializationParametersMissingException if the parameters for this particular
   * graph have not been set. 
   * @throws SerializationException if errors occur during deserialization.
   */
  public Graph[] deserialize() 
    throws SerializerNotConfiguredException, SerializationParametersMissingException,
    SerializationException {
    // if there are errors, accumlate as many as we can before throwing SerializationException
    SerializationException errors = null;

    Graph graph = new Graph();
    graph.setId(getName());
    // add layers to the graph
    // we don't just copy the whole schema, because that would imply that all the extra layers
    // contained no annotations, which is not necessarily true
    graph.addLayer((Layer)participantLayer.clone());
    graph.getSchema().setParticipantLayerId(participantLayer.getId());
    graph.addLayer((Layer)turnLayer.clone());
    graph.getSchema().setTurnLayerId(turnLayer.getId());
    graph.addLayer((Layer)utteranceLayer.clone());
    graph.getSchema().setUtteranceLayerId(utteranceLayer.getId());
    graph.addLayer((Layer)wordLayer.clone());
    graph.getSchema().setWordLayerId(wordLayer.getId());
    if (disfluencyLayer != null) graph.addLayer((Layer)disfluencyLayer.clone());
    if (nonWordLayer != null) graph.addLayer((Layer)nonWordLayer.clone());
    if (expansionLayer != null) graph.addLayer((Layer)expansionLayer.clone());
    if (errorsLayer != null) graph.addLayer((Layer)errorsLayer.clone());
    if (repetitionsLayer != null) graph.addLayer((Layer)repetitionsLayer.clone());
    if (retracingLayer != null) graph.addLayer((Layer)retracingLayer.clone());
    if (completionLayer != null) graph.addLayer((Layer)completionLayer.clone());
    if (morLayer != null) graph.addLayer((Layer)morLayer.clone());
    if (morPrefixLayer != null) graph.addLayer((Layer)morPrefixLayer.clone());
    if (morPartOfSpeechLayer != null) graph.addLayer((Layer)morPartOfSpeechLayer.clone());
    if (morPartOfSpeechSubcategoryLayer != null) graph.addLayer((Layer)morPartOfSpeechSubcategoryLayer.clone());
    if (morStemLayer != null) graph.addLayer((Layer)morStemLayer.clone());
    if (morFusionalSuffixLayer != null) graph.addLayer((Layer)morFusionalSuffixLayer.clone());
    if (morSuffixLayer != null) graph.addLayer((Layer)morSuffixLayer.clone());
    if (morGlossLayer != null) graph.addLayer((Layer)morGlossLayer.clone());
    if (gemLayer != null) graph.addLayer((Layer)gemLayer.clone());
    if (linkageLayer != null) graph.addLayer((Layer)linkageLayer.clone());
    if (cUnitLayer != null) graph.addLayer((Layer)cUnitLayer.clone());
    if (transcriberLayer != null) graph.addLayer((Layer)transcriberLayer.clone());
    if (languagesLayer != null) graph.addLayer((Layer)languagesLayer.clone());
    if (dateLayer != null) graph.addLayer((Layer)dateLayer.clone());
    if (locationLayer != null) graph.addLayer((Layer)locationLayer.clone());
    if (roomLayoutLayer != null) graph.addLayer((Layer)roomLayoutLayer.clone());
    if (recordingQualityLayer != null) graph.addLayer((Layer)recordingQualityLayer.clone());
    if (tapeLocationLayer != null) graph.addLayer((Layer)tapeLocationLayer.clone());
    if (pauseLayer != null) graph.addLayer((Layer)pauseLayer.clone());
    for (String attribute : participantLayers.keySet()) {
      Layer layer = participantLayers.get(attribute);
      if (layer != null) {
        graph.addLayer((Layer)layer.clone());
      }
    } // next participant layer 

    // graph meta data
    for (String header : headers) {
      if (header.startsWith("@")) { // @ line
        int iColon = header.indexOf(':');
        if (iColon >= 0) { // it's a key/value pair
          String value = header.substring(iColon + 1).trim();
          if (header.startsWith("@Languages:")) {
            if (value.trim().length() > 0) {
              StringTokenizer tokens = new StringTokenizer(value, ", ");
              while (tokens.hasMoreTokens()) {
                String lang = tokens.nextToken(); // ISO639 alpha-2 code if possible
                languages.add(iso639.alpha2(lang).orElse(lang));
              }
            }
          } else if (header.startsWith("@Participants:")) {
            // something like:
            // @Participants:	SUB 2001 Participant, EXA Investigator Investigator
            StringTokenizer tokens = new StringTokenizer(value, ",");
            while (tokens.hasMoreTokens()) {
              String sParticipant = tokens.nextToken();
              StringTokenizer participantTokens = new StringTokenizer(sParticipant.trim());
              if (participantTokens.hasMoreTokens()) {
                String id = participantTokens.nextToken();
                // ensure they're in the participants map
                if (!participants.containsKey(id)) {
                  participants.put(id, new HashMap<String,String>());
                }
                if (participantTokens.hasMoreTokens()) {
                  String name = participantTokens.nextToken();
                  participants.get(id).put("name", name);
                  if (participantTokens.hasMoreTokens()) {
                    String role = participantTokens.nextToken();
                    participants.get(id).put("role", role);
                  }
                }
              }
            }
          } else if (header.startsWith("@ID:")) {
            // @ID: language|corpus|code|age|sex|group|SES|role|education|custom|
            String[] tokens = value.split("\\|");
            String language = tokens.length<=0?"":tokens[0];
            // ISO639 alpha-2 code if possible
            language = iso639.alpha2(language).orElse(language);
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
            if (!participants.containsKey(code)) {
              participants.put(code, new HashMap<String,String>());
            }
            // set the attribute values and make sure we ask for layers if there are values
            participants.get(code).put("language", language);
            participants.get(code).put("corpus", corpus);
            participants.get(code).put("age", age);
            participants.get(code).put("sex", sex);
            participants.get(code).put("group", group);
            participants.get(code).put("SES", SES);
            participants.get(code).put("role", role);
            participants.get(code).put("education", education);
            participants.get(code).put("custom", custom);
          } else if (header.startsWith("@Media:")) {
            StringTokenizer tokens = new StringTokenizer(value, ", ");
            if (tokens.hasMoreTokens()) {
              setMediaName(tokens.nextToken());
              if (tokens.hasMoreTokens()) {
                setMediaType(tokens.nextToken());
              }
            }
          } else if (header.startsWith("@Transcriber:")) {
            transcribers.add(value);
          } else if (header.startsWith("@Location:")) {
            if (value.trim().length() > 0 && locationLayer != null) {
              graph.createTag(graph, locationLayer.getId(), value);
            }
          } else if (header.startsWith("@Recording Quality:")) {
            if (value.trim().length() > 0 && recordingQualityLayer != null) {
              graph.createTag(graph, recordingQualityLayer.getId(), value);
            }
          } else if (header.startsWith("@Room Layout:")) {
            if (value.trim().length() > 0 && roomLayoutLayer != null) {
              graph.createTag(graph, roomLayoutLayer.getId(), value);
            }
          } else if (header.startsWith("@Tape Location:")) {
            if (value.trim().length() > 0 && tapeLocationLayer != null) {
              graph.createTag(graph, tapeLocationLayer.getId(), value);
            }
          } else if (header.startsWith("@Date:")) {
            if (value.trim().length() > 0 && dateLayer != null) {
              SimpleDateFormat chatDateFormat
                // use UK locale because it doesn't expect a dot after a month abbreviation
                = new SimpleDateFormat("dd-MMM-yyyy",Locale.UK);
              SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd");
              try { // parse as CHAT format
                Date date = chatDateFormat.parse(value.toLowerCase());
                // but save with ISO format
                graph.createTag(graph, dateLayer.getId(), isoDateFormat.format(date));
              } catch(ParseException exception) {
                warnings.add(
                  "Could not parse date \""+value+"\": " + exception.getMessage() + "--" +chatDateFormat.format(new Date()));
                graph.createTag(graph, dateLayer.getId(), value);
              }
            }
          } 
        } // it's a key/value pair
      } // @ line
    } // next header

    if (getTranscriberLayer() != null) {
      for (String transcriber : transcribers) {
        graph.createTag(graph, getTranscriberLayer().getId(), transcriber);
      }
    }
    if (getLanguagesLayer() != null) {
      for (String language : languages) {
        graph.createTag(graph, getLanguagesLayer().getId(), language);
      }
    }
      
    // participants
    for (String participantId : participants.keySet()) {
      HashMap<String,String> attributes = participants.get(participantId);
      Annotation participant = new Annotation(
        participantId, 
        attributes.containsKey("name")?attributes.get("name"):participantId, 
        getParticipantLayer().getId());
      participant.setParentId(graph.getId());
      graph.addAnnotation(participant);
      // set the participant meta-data
      for (String attribute : participantLayers.keySet()) {
        Layer layer = participantLayers.get(attribute);
        if (layer != null && attributes.containsKey(attribute)) {
          graph.createTag(participant, layer.getId(), attributes.get(attribute));
        }
      }	 
    }

    // ensure we have an utterance tokenizer
    if (getTokenizer() == null) {
      setTokenizer(new SimpleTokenizer(getUtteranceLayer().getId(), getWordLayer().getId()));
    }

    // regular expressions
    Pattern lineSplitPattern = Pattern.compile("([0-9]+)_([0-9]+)");
    Pattern synchronisedPattern = Pattern.compile("^(.*)([0-9]+)_([0-9]+)$");
    Pattern specialTerminatorsPattern = Pattern.compile(
      "^.*((\\+\\.\\.\\.)|(\\+\\.\\.\\?)|(\\+!\\?)|(\\+/\\.)|(\\+/\\?)|(\\+//\\.)|(\\+//\\?)|(\\+\\.)|(\\+\"/\\.)|(\\+\"\\.))$");
    Pattern basicTerminatorsPattern = Pattern.compile("^.*([.?!,;:])$");
    Pattern speakerPattern = Pattern.compile("^\\*(\\p{Alnum}+):(.+)$");

    // any lines that contain mid-line synchronisation codes are split into two
    Vector<String> syncLines = new Vector<String>();
    for (String line : getLines()) {
      Matcher matcher = lineSplitPattern.matcher(line);
      // nibble off matching parts
      int begin = 0;
      while (matcher.find()) {
        syncLines.add(line.substring(begin, matcher.end()));
        begin = matcher.end();
      } // next match
      if (begin < line.length() - 1) {
        syncLines.add(line.substring(begin));
      }
    }

    // utterances
    Annotation currentTurn = new Annotation(null, "", getTurnLayer().getId());
    Annotation lastUtterance = new Annotation(null, "", getUtteranceLayer().getId());
    Annotation gem = null;
    Annotation cUnit = null;
    Anchor lastAnchor = new Anchor(null, 0.0, Constants.CONFIDENCE_MANUAL);
    graph.addAnchor(lastAnchor);
    Anchor lastAlignedAnchor = null;
    boolean tierUnsupportedWarning = false; // only warn about ignoring tiers once
    boolean parsingMor = morLayer != null
      || (splitMorTagGroups && splitMorWordGroups
          && (morPrefixLayer != null
              || morPrefixLayer != null
              || morPartOfSpeechLayer != null
              || morPartOfSpeechSubcategoryLayer != null
              || morStemLayer != null
              || morFusionalSuffixLayer != null
              || morSuffixLayer != null
              || morGlossLayer != null
            ));
    graph.addLayer(new Layer("@mor", "MOR Spans")
                   .setAlignment(Constants.ALIGNMENT_NONE)
                   .setPeers(false).setPeersOverlap(false).setSaturated(true)
                   .setParentId(turnLayer.getId()).setParentIncludes(true));
    for (String line : syncLines) {
      if (line.startsWith("%mor:")) {
        // morphosyntactic tags
        // remember this line for after the previous turn has been tokenized into words
        String morLine = line.startsWith("%mor:")?line.substring("%mor:".length()):line;
        morLine = morLine.trim();
        Annotation mor = currentTurn.first("@mor");
        if (mor != null) { // append to the previous tags
          mor.setLabel(mor.getLabel() + " " + morLine);
        } else {
          mor = graph.createTag(lastUtterance, "@mor", morLine);
        }
        
      } else if (line.startsWith("%")) { // dependent tier
        if (!tierUnsupportedWarning) {
          warnings.add(
            "Dependent tiers other than %mor are not currently parsed; ignoring lines like \""
            +line+"\"");
        }
        
      } else if (line.startsWith("@")) { // gem
        if (line.startsWith("@G") || line.startsWith("@Bg")) {
          if (gem != null) {
            gem.setEnd(lastAnchor);
            graph.addAnnotation(gem);
          }
          if (getGemLayer() != null) {
            String label = "gem";
            int iColon = line.indexOf(':');
            if (iColon >= 0) { // it's a key/value pair
              label = line.substring(iColon + 1).trim();
            }
            gem = new Annotation(null, label, getGemLayer().getId(), graph.getId());
          }
        } else if (line.startsWith("@Eg") && gem != null) {
          gem.setEnd(lastAnchor);
          graph.addAnnotation(gem);
          gem = null;
        }
        
      } else { // transcript line
        Matcher speakerMatcher = speakerPattern.matcher(line);
        if (speakerMatcher.matches()) { // setting the speaker, ID is 3 characters
          if (cUnit != null && getCUnitLayer() != null) { // add last c-unit
            cUnit.setEnd(lastAnchor);
            // try to match first the multi-character terminators, then the single-character ones
            Matcher terminator = specialTerminatorsPattern.matcher(lastUtterance.getLabel());
            if (!terminator.matches()) {
              terminator = basicTerminatorsPattern.matcher(lastUtterance.getLabel());
            }
            if (terminator.matches()) {
              // set the c-unit label as the terminator
              cUnit.setLabel(terminator.group(1));
              // strip the terminator off the utterance
              lastUtterance.setLabel(lastUtterance.getLabel().substring(0, terminator.start(1)));
            }
            graph.addAnnotation(cUnit);
            cUnit = null;
          } // c-unit to add
          // something like:
          // *SUB: ...
          String participantId = speakerMatcher.group(1);
          if (!participants.containsKey(participantId)) {
            warnings.add("Undeclared participant: " + participantId);
		  
            Annotation participant = new Annotation(
              participantId, participantId, getParticipantLayer().getId());
            participant.setParentId(graph.getId());
            graph.addAnnotation(participant);
          } // undeclared participant
          if (!participantId.equals(currentTurn.getLabel())) { // new turn
            if (currentTurn.getEndId() == null) {
              currentTurn.setEnd(lastAnchor);
            }
            currentTurn = new Annotation(null, participantId, getTurnLayer().getId());
            currentTurn.setStartId(lastAnchor.getId());		  
            currentTurn.setParentId(participantId);
            graph.addAnnotation(currentTurn);
          }
          line = speakerMatcher.group(2);
        } // participant
	    
        Annotation utterance = new Annotation(null, line, getUtteranceLayer().getId());
        utterance.setStart(lastUtterance.getEnd());
        if (cUnit == null && getCUnitLayer() != null) {
          cUnit = new Annotation(null, "", getCUnitLayer().getId());
          cUnit.setStart(lastUtterance.getEnd());
          cUnit.setParent(currentTurn);
        }
        Matcher synchronisedMatcher = synchronisedPattern.matcher(line);
        if (synchronisedMatcher.matches()) {
          utterance.setLabel(synchronisedMatcher.group(1).trim());
          Anchor start = graph.getOrCreateAnchorAt(
            Double.parseDouble(synchronisedMatcher.group(2))
            / 1000, Constants.CONFIDENCE_MANUAL);
          if (cUnit != null && (cUnit.getStartId() == null 
                                || cUnit.getStartId().equals(utterance.getStartId()))) {
            cUnit.setStart(start);
          }
          // avoid creating instantaneous utterances
          avoidInstantaneousUtterance(start, lastUtterance, graph);
          utterance.setStart(start);
          Anchor end = graph.getOrCreateAnchorAt(
            Double.parseDouble(synchronisedMatcher.group(3))
            / 1000, Constants.CONFIDENCE_MANUAL);
          utterance.setEnd(end);
          lastAlignedAnchor = end;
	       
          if (start.getOffset() > end.getOffset()) { // start and end in reverse order
            String message = "Utterance start is after end: " 
              + synchronisedMatcher.group(2) + "_" + synchronisedMatcher.group(3);
            if (errors == null) errors = new SerializationException(message);
            errors.addError(SerializationException.ErrorType.Alignment, message);
          } else {
            checkAlignmentAgainstLastUtterance(
              utterance, start, end, cUnit, lastUtterance, currentTurn, utteranceLayer, graph);
          }
          if (currentTurn.all(getUtteranceLayer().getId()).length == 0) {
            // first utterance of this turn
            currentTurn.setStart(start);
          }
          currentTurn.setEnd(end);
        } // synchronised utterance
        graph.addAnnotation(utterance);
        utterance.setParent(currentTurn);

        // if a gem has just been started
        if (gem != null && gem.getStartId() == null) { // set the gem's start anchor ID
          gem.setStartId(utterance.getStartId());
        }
	    
        lastUtterance = utterance;
        lastAnchor = utterance.getEnd();
      } // transcript line
    } // next line
    if (gem != null) {
      gem.setEnd(lastAnchor);
      graph.addAnnotation(gem);
    }
    if (cUnit != null && getCUnitLayer() != null) { // add last c-unit
      cUnit.setEnd(lastAnchor);
      // try to match first the multi-character terminators, then the single-character ones
      Matcher terminator = specialTerminatorsPattern.matcher(lastUtterance.getLabel());
      if (!terminator.matches()) {
        terminator = basicTerminatorsPattern.matcher(lastUtterance.getLabel());
      }
      if (terminator.matches()) {
        // set the c-unit label as the terminator
        cUnit.setLabel(terminator.group(1));
        // strip the terminator off the utterance
        lastUtterance.setLabel(lastUtterance.getLabel().substring(0, terminator.start(1)));
      }
      graph.addAnnotation(cUnit);
      cUnit = null;
    } // c-unit to add
    if (lastAnchor.getOffset() == null && lastAlignedAnchor != null) {
      if (lastAlignedAnchor == lastUtterance.getStart()) {
        avoidInstantaneousUtterance(lastAlignedAnchor, lastUtterance, graph);
        lastUtterance.getEnd().moveEndingAnnotations(lastAlignedAnchor);
        lastAnchor = lastUtterance.getEnd();
      } else {
        lastAnchor.setOffset(lastAlignedAnchor.getOffset());
      }
    }
    currentTurn.setEndId(lastAnchor.getId());

    // tokenize utterances into words
    try {
      getTokenizer().transform(graph);
    } catch(TransformationException exception) {
      if (errors == null) errors = new SerializationException(exception.getMessage());
      if (errors.getCause() == null) errors.initCause(exception);
      errors.addError(SerializationException.ErrorType.Tokenization, exception.getMessage());
    }

    try {
      graph.trackChanges();
         
      // split linkages?
      if (getLinkageLayer() != null) { 
        SimpleTokenizer linkageSplitter 
          = new SimpleTokenizer(getWordLayer().getId(), getLinkageLayer().getId(), "_", true);
        linkageSplitter.transform(graph);
        graph.commit();
      }

      // annotated spans marked by <...>
      graph.addLayer(new Layer("@span", "Annotation Spans", Constants.ALIGNMENT_INTERVAL, true, true, false, getTurnLayer().getId(), true));
      SpanningConventionTransformer spanningTransformer = new SpanningConventionTransformer(
        getWordLayer().getId(), "<(.*)", "(.*)>(.*)", false, "$1", "$1$2", 
        "@span", "-", "$1", "$1", false);	  
      spanningTransformer.transform(graph);	
      graph.commit();

      // pauses
      ConventionTransformer transformer = new ConventionTransformer(
        // e.g. (.) (...) (0.15) (2.) (1:05.15)
        getWordLayer().getId(), "\\(([0-9:]*\\.+[0-9]*)\\)");
      if (pauseLayer != null) {
        transformer.addDestinationResult(pauseLayer.getId(), "$1");
      }
      transformer.transform(graph).commit();      
      graph.commit();

      // disfluencies...
      // before we transform disfluencies, we'll tag them becaus we might need to know
      // whether they've been skipped by mor
      for (Annotation word : graph.all(getWordLayer().getId())) {
        if (word.getLabel().startsWith("&+")) word.put("@skippedByMor", Boolean.TRUE);
      } // next token
      // now strip off the &+...
      transformer = new ConventionTransformer(
        getWordLayer().getId(), "&\\+(\\w+)");
      transformer.addDestinationResult(getWordLayer().getId(), "$1~");
      if (getDisfluencyLayer() != null) {
        transformer.addDestinationResult(getDisfluencyLayer().getId(), "&+");
      }
      transformer.transform(graph);
      graph.commit();
	 
      // expansions
      String expansionLayerId = getExpansionLayer()!=null?getExpansionLayer().getId():null;
      spanningTransformer = new SpanningConventionTransformer(
        getWordLayer().getId(), "\\[:", "(.*)\\]", true, null, null, 
        expansionLayerId, null, "$1", true);	  
      spanningTransformer.transform(graph);	
      graph.commit();

      // errors
      String errorsLayerId = getErrorsLayer()!=null?getErrorsLayer().getId():null;
      spanningTransformer = new SpanningConventionTransformer(
        getWordLayer().getId(), "\\[\\*", "(.*)\\](.*)", true, null, "$2", 
        errorsLayerId, "", "$1", true, true);	  
      spanningTransformer.transform(graph);	
      if (errorsLayerId != null) {
        // if they coincide with the ends of spans, then they annotate the span
        for (Annotation error : graph.all(errorsLayerId)) {
          LinkedHashSet<Annotation> endingSpans = error.getEnd().endOf("@span");
          if (endingSpans.size() > 0) {
            Annotation span = endingSpans.iterator().next();
            error.setStart(span.getStart());
          }
        } // next error
      } // errorsLayerId set
      graph.commit();

      // repetitions
      String repetitionsLayerId = getRepetitionsLayer()!=null?getRepetitionsLayer().getId():null;
      spanningTransformer = new SpanningConventionTransformer(
        getWordLayer().getId(), "\\[/\\].*", "\\[/\\]", true, null, null, 
        repetitionsLayerId, "/", "", true, true);	  
      spanningTransformer.transform(graph);	
      if (repetitionsLayerId != null) {
        // if they coincide with the ends of spans, then they annotate the span
        for (Annotation repetition : graph.all(repetitionsLayerId)) {
          LinkedHashSet<Annotation> endingSpans = repetition.getEnd().endOf("@span");
          if (endingSpans.size() > 0) {
            Annotation span = endingSpans.iterator().next();
            repetition.setStart(span.getStart());
          }
        } // next error
      } // repetitionsLayerId set
      graph.commit();
	 
      // retracing
      String retracingLayerId = getRetracingLayer()!=null?getRetracingLayer().getId():null;
      spanningTransformer = new SpanningConventionTransformer(
        getWordLayer().getId(), "\\[//\\]", "\\[//\\]", true, null, null, 
        retracingLayerId, "//", "", true, true);	  
      spanningTransformer.transform(graph);	
      if (retracingLayerId != null) {
        // if they coincide with the ends of spans, then they annotate the span
        for (Annotation retrace : graph.all(retracingLayerId)) {
          LinkedHashSet<Annotation> endingSpans = retrace.getEnd().endOf("@span");
          if (endingSpans.size() > 0) {
            Annotation span = endingSpans.iterator().next();
            retrace.setStart(span.getStart());
          }
        } // next error
      } // retracingLayerId set
      graph.commit();
	 
      // completions at the start and at the end
      transformer = new ConventionTransformer(getWordLayer().getId(), "\\((\\p{Alnum}+)\\)(.+)\\((\\p{Alnum}+)\\)");
      transformer.addDestinationResult(getWordLayer().getId(), "$2");
      if (getCompletionLayer() != null) {
        transformer.addDestinationResult(getCompletionLayer().getId(), "$1$2$3");
      }
      transformer.transform(graph);	
      graph.commit();
	 
      // completions at the start
      transformer = new ConventionTransformer(getWordLayer().getId(), "\\((\\p{Alnum}+)\\)(.+)");
      transformer.addDestinationResult(getWordLayer().getId(), "$2");
      if (getCompletionLayer() != null) {
        transformer.addDestinationResult(getCompletionLayer().getId(), "$1$2");
      }
      transformer.transform(graph);	
      graph.commit();
	 
      // completions at the end
      transformer = new ConventionTransformer(getWordLayer().getId(), "(.+)\\((\\p{Alnum}+)\\)");
      transformer.addDestinationResult(getWordLayer().getId(), "$1");
      if (getCompletionLayer() != null) {
        transformer.addDestinationResult(getCompletionLayer().getId(), "$1$2");
      }
      transformer.transform(graph);
      graph.commit();

      // remove temporary span annotations
      for (Annotation span : graph.all("@span")) {
        span.destroy();
      } // next span	
      graph.commit();

      // match %mor annotations with their tokens
      for (Annotation morLine : graph.all("@mor")) {
        if (parsingMor) {
          String[] morTags = morLine.getLabel().split(" ");
          Annotation[] tokens = morLine.getParent().all(wordLayer.getId());
          // tag each token
          int t = 0;
          for (int m = 0; m < morTags.length && t < tokens.length; m++, t++) {
            
            Annotation token = tokens[t];
            // skip partial words, which are not tagged by mor
            while (token.containsKey("@skippedByMor") && t < tokens.length-1) {
              token = tokens[++t];
            }
            if (token.containsKey("@skippedByMor")) {
              String message = "There are more %mor tags ("+Arrays.asList(morTags)
                +") than real tokens ("
                +Arrays.stream(tokens).map(tk->tk.getLabel()).collect(Collectors.toList())+")"; 
              if (errors == null) errors = new SerializationException(message);
              errors.addError(SerializationException.ErrorType.Tokenization, message);
                continue;
            }
            
            String morTag = morTags[m].trim();
            
            if (!splitMorTagGroups) { // one big fat complex label
              if (morLayer != null) {
                graph.createTag(token, morLayer.getId(), morTag);
              }
            } else { // splitMorTagGroups
              String[] morTagGroup = morTag.split("\\^");
              if (!splitMorWordGroups) { // several complex labels
                if (morLayer != null) {
                  for (String tag : morTagGroup) {
                    graph.createTag(token, morLayer.getId(), tag);
                  } // next tag group
                }
              } else { // splitMorWordGroups
                // word groups are structures: preclitic$word~postclitic
                // e.g. dámelo: "vimpsh|da-2S&IMP~pro:clit|1S~pro:clit|OBJ&MASC=give"
                // - vimpsh|da-2S&IMP
                // - pro:clit|1S
                // - pro:clit|OBJ&MASC=give
                // or overall+component+component
                // e.g. angelfish: "n|+n|angel+n|fish"
                // - n|
                // - n|angel
                // - n|fish
                for (String wordGroup : morTagGroup) {
                  // tag groups are temporally sequential, so each one is chained
                  // across the duration of the word
                  String[] words = wordGroup.split("[$+~]");
                  Annotation lastTag = null;
                  Vector<Annotation> tags = new Vector<Annotation>();
                  
                  // first, tease out the words and sort out their anchors
                  for (String subword : words) {
                    Annotation tag = new Annotation()
                      .setLabel(subword)
                      .setParentId(token.getId());
                    if (lastTag == null) {
                      tag.setStartId(token.getStartId());
                    } else { // chain the end of the last subword the start of this subword
                      tag.setStartId(graph.addAnchor(new Anchor()).getId());
                      lastTag.setEndId(tag.getStartId());
                    }
                    tag.setEndId(token.getEndId());                      
                    if (morLayer != null) {
                      tag.setLayerId(morLayer.getId());
                      graph.addAnnotation(tag);
                    }
                    lastTag = tag;
                    tags.add(tag);
                  } // next subword
                  
                  // now parse the parts of each word
                  for (Annotation tag : tags) {
                    String label = tag.getLabel();
                    
                    // tags at the start...
                    
                    // prefixes
                    int lastHash = label.lastIndexOf('#');
                    if (lastHash >= 0) { // there are prefixes
                      if (morPrefixLayer != null) {
                        String[] prefixes = label.substring(0, lastHash).split("#");
                        for (String prefix : prefixes) {
                          addUniqueAnnotation(
                            graph, morPrefixLayer, prefix,
                            token.getId(), tag.getStartId(), tag.getEndId());
                        } // next prefix
                      } // morPrefixLayer is set
                      // remove prefixes
                      label = label.substring(lastHash+1);
                    }
                    
                    // part-of-speech
                    int bar = label.indexOf('|');
                    if (bar >= 0) {
                      String pos = label.substring(0,bar);
                      String[] subcategories = pos.split(":");
                      pos = subcategories[0];
                      // part of speech
                      addUniqueAnnotation(
                        graph, morPartOfSpeechLayer, pos, token.getId(),
                        tag.getStartId(), tag.getEndId());
                      // subcategories
                      for (int s = 1; s < subcategories.length; s++) {
                        String subcategory = subcategories[s];
                        addUniqueAnnotation(
                          graph, morPartOfSpeechSubcategoryLayer, subcategory,
                          token.getId(), tag.getStartId(), tag.getEndId());
                      } // next subcategory
                      // remove part-of-speech
                      label = label.substring(bar+1);
                    }
                    
                    // get tags of the end...
                    
                    // English gloss
                    int equals = label.indexOf('=');
                    if (equals >= 0) {
                      if (morGlossLayer != null) {
                        String gloss = label.substring(equals+1);
                        addUniqueAnnotation(
                          graph, morGlossLayer, gloss,
                          token.getId(), tag.getStartId(), tag.getEndId());
                      } // morGlossLayer is set
                      // remove gloss
                      label = label.substring(0, equals);
                    }
                    
                    // Suffixes
                    int firstDash = label.indexOf('-');
                    if (firstDash >= 0) { // there are suffixes
                      if (morSuffixLayer != null) {
                        String[] suffixes = label.substring(firstDash+1).split("-");
                        for (String suffix : suffixes) {
                          addUniqueAnnotation(
                            graph, morSuffixLayer, suffix,
                            token.getId(), tag.getStartId(), tag.getEndId());
                        } // next suffix
                      } // morSuffixLayer is set
                      // remove suffixes
                      label = label.substring(0, firstDash);
                    }
                    
                    // Fusional suffixes
                    int firstAmpersand = label.lastIndexOf('&');
                    if (firstAmpersand >= 0) { // there are suffixes
                      if (morFusionalSuffixLayer != null) {
                        String[] suffixes = label.substring(firstAmpersand+1).split("&");
                        for (String suffix : suffixes) {
                          addUniqueAnnotation(
                            graph, morFusionalSuffixLayer, suffix,
                            token.getId(), tag.getStartId(), tag.getEndId());
                        } // next suffix
                      } // morFusionalSuffixLayer is set
                      // remove suffixes
                      label = label.substring(0, firstAmpersand);
                    }
                    
                    // whatever's left is the stem
                    addUniqueAnnotation(
                      graph, morStemLayer, label,
                      token.getId(), tag.getStartId(), tag.getEndId());
                    
                  } // next tag
                } // next word group
              } // splitMorWordGroups
            } // splitMorTagGroups
          } // next token
          if (t < tokens.length) {
            String message = "There are fewer %mor tags ("+Arrays.asList(morTags)
              +") than tokens ("
              +Arrays.stream(tokens).map(tk->tk.getLabel()).collect(Collectors.toList())+") + "+t; 
            if (errors == null) errors = new SerializationException(message);
            errors.addError(SerializationException.ErrorType.Tokenization, message);
          }
        } // %mor
        morLine.destroy();
      } // next mor 
        
      // set all annotations to manual confidence
      for (Annotation a : graph.getAnnotationsById().values()) {
        a.setConfidence(Constants.CONFIDENCE_MANUAL);
        // also remove mor-related tags
        if (a.containsKey("@skippedByMor")) a.remove("@skippedByMor");
      }

    } catch(TransformationException exception) {
      if (errors == null) errors = new SerializationException(exception.getMessage());
      if (errors.getCause() == null) errors.initCause(exception);
      errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
    }
    if (errors != null) throw errors;
      
    // reset all change tracking
    graph.getTracker().reset();
    graph.setTracker(null);

    Graph[] graphs = { graph };
    return graphs;
  }

  
   /**
    * Adds the given annotation, but only if there isn't one already there with the same
    * layer, label, parent, and anchors. 
    * @param graph
    * @param layer
    * @param label
    * @param parentId
    * @param startId
    * @param endId
    */
  private void addUniqueAnnotation(
    Graph graph, Layer layer, String label, String parentId, String startId, String endId) {
    if (layer != null && label.length() > 0) {
      for (Annotation child : graph.getAnnotation(parentId).getAnnotations(layer.getId())) {
        if (child.getStartId().equals(startId)
            && child.getEndId().equals(endId)
            && child.getLabel().equals(label)) {
          return;
        }
      } // next child
      // if we got this far, there's no other child like this
      graph.addAnnotation(new Annotation().setLayerId(layer.getId())
                          .setLabel(label)
                          .setParentId(parentId)
                          .setStartId(startId).setEndId(endId));
    }
  } // end of addUniqueAnnotation()


  /**
   * Avoid the special case of a non-syncrhonised utterance sandwiched between two
   * synchronised ones, where the third starts and the time the first ends. 
   * <p> e.g. 511.784 ...penultimateUtterance... 514.337 ...lastUtterance... 514.337
   * ...utterance... 517.092 
   * @param start The next start anchor.
   * @param lastUtterance The last utterance.
   * @param graph The annotation graph.
   */
  public void avoidInstantaneousUtterance(Anchor start, Annotation lastUtterance, Graph graph) {
    if (lastUtterance.getStart() == start 
        && lastUtterance.getEnd().getOffset() == null) { // only for unaligned middle utterances
      Anchor middleAnchor = graph.addAnchor(new Anchor());
      // can we reach back a further utterance
      LinkedHashSet<Annotation> endingAtStart = start.endOf(getUtteranceLayer().getId());
      Annotation penultimateUtterance = null;
      if (endingAtStart.size() > 0) {
        penultimateUtterance = endingAtStart.iterator().next();
        if (penultimateUtterance.getStart().getOffset() != null) {
          middleAnchor.setOffset(
            penultimateUtterance.getStart().getOffset() 
            + ((start.getOffset() - penultimateUtterance.getStart().getOffset()) /2));
          middleAnchor.setConfidence(Constants.CONFIDENCE_DEFAULT);
        } // penultimateUtterance has a start offset
      } // there is a penultimate utterance
      warnings.add("Utterance at " + lastUtterance.getStart().getOffset() 
                   + "-" + lastUtterance.getEnd().getOffset() + " is instantaneous"
                   + ": using " + middleAnchor + " as start time.");
      // move linking annotations to the middle anchor
      start.moveEndingAnnotations(middleAnchor);
      start.moveStartingAnnotations(middleAnchor);
    }
  } // end of avoidInstantaneousUtterance()

  /**
   * Check the alignment of the given utterance against the given last utterance, to
   * ensure there are no gaps if there shouldn't be, and that the anchors are in the right
   * order. 
   * @param utterance Current utterance, which is not assumed to be in the graph yet.
   * @param start The utterance's start anchor, which is assumed to be in the graph.
   * @param end The utterance's end anchor, which is assumed to be in the graph.
   * @param cUnit The current c-unit annotation, which is not assumed to be in the graph.
   * @param lastUtterance Last utterance, which is assumed to be in the graph.
   * @param currentTurn Current turn, which is assumed to be in the graph, and the parent
   * of <var></var>utterance 
   * @param utteranceLayer The utterance layer definition.
   * @param graph The annotation graph.
   */
  public void checkAlignmentAgainstLastUtterance(
    Annotation utterance, Anchor start, Anchor end, Annotation cUnit, Annotation lastUtterance,
    Annotation currentTurn, Layer utteranceLayer, Graph graph) {
    // is this utterance in the same turn as the last one?
    Anchor lastEnd = lastUtterance.getEnd();
    if (currentTurn.getId().equals(lastUtterance.getParentId()) && lastEnd != null) {
      if (lastEnd.getOffset() == null) { // last utterance was not aligned
        // set its end time to the last of this one (and bring related annotions along)
        lastUtterance.getEnd().moveEndingAnnotations(start);
      } else { // we can check the alignment against the last one
        if (utteranceLayer.getSaturated()) {
          // check for a gap between this one and the last one
          if (start.getOffset() - lastEnd.getOffset() > 0.01) {
            // add an empty filler utterance
            graph.addAnnotation(
              new Annotation(null, "", getUtteranceLayer().getId(), 
                             lastEnd.getId(), start.getId(), currentTurn.getId()));
          } // gap between last utterance and this one
        } // utterance layer should be saturated
	    
        if (!utteranceLayer.getPeersOverlap()) { // check there's no overlap
          if (start.getOffset() < lastEnd.getOffset()) {
            // this utterance starts before the last one ends
            if (end.getOffset() > lastEnd.getOffset()) { // partial overlap
              warnings.add("Utterance at " + start + "-" + end 
                           + " overlaps previous at " + lastUtterance.getStart() + "-" + lastEnd
                           + ": using " + lastEnd + " as start time.");
              // use the later time
              if (cUnit != null && cUnit.getStartId().equals(start.getId())) {
                cUnit.setStartId(lastEnd.getId());
              }
              utterance.setStartId(lastEnd.getId());
            } else { // full overlap - this should probably be an error, but instead:
		     // chain this utterance to the last one with an unaligned anchor
              Anchor middleAnchor = graph.addAnchor(
                new Anchor(null, lastEnd.getOffset() + ((start.getOffset() - lastEnd.getOffset())/2), 
                           Constants.CONFIDENCE_DEFAULT));
              warnings.add("Utterance at " + start + "-" + end 
                           + " completely overlaps previous at " + lastUtterance.getStart() + "-" + lastEnd
                           + ": using " + middleAnchor +" as end time of first and start time of second.");
              // set end
              // (and bring related annotions along)
              lastUtterance.getEnd().moveEndingAnnotations(middleAnchor);
              // set start
              if (cUnit != null && cUnit.getStartId().equals(start.getId())) {
                // bring the c-unit along 
                // (can't use moveStartingAnnotations() because utterance isn't in graph)
                cUnit.setStart(middleAnchor);
              }
              utterance.setStart(middleAnchor);
            }
          } // this utterance starts before the last one ends
        } // check there's no overlap
      } // lastEnd offset is set
    } // utterance the same turn as the last one
  } // end of checkAlignmentAgainstLastUtterance()

  /**
   * Returns any warnings that may have arisen during the last execution of
   * {@link #deserialize()}.
   * @return A possibly empty lilayersst of warnings.
   */
  public String[] getWarnings() {
    return warnings.toArray(new String[0]);
  }
   
  // GraphSerializer methods
   
  /**
   * Determines which layers, if any, must be present in the graph that will be serialized.
   * @return A list of IDs of layers that must be present in the graph that will be serialized.
   * @throws SerializationParametersMissingException If not all required parameters have a value.
   */
  public String[] getRequiredLayers() throws SerializationParametersMissingException {
    Vector<String> requiredLayers = new Vector<String>();
    if (getParticipantLayer() != null) requiredLayers.add(getParticipantLayer().getId());
    if (getTargetParticipantLayer() != null) requiredLayers.add(getTargetParticipantLayer().getId());
    if (getTurnLayer() != null) requiredLayers.add(getTurnLayer().getId());
    if (getUtteranceLayer() != null) requiredLayers.add(getUtteranceLayer().getId());
    if (getWordLayer() != null) requiredLayers.add(getWordLayer().getId());
    if (getDisfluencyLayer() != null) requiredLayers.add(getDisfluencyLayer().getId());
    if (getNonWordLayer() != null) requiredLayers.add(getNonWordLayer().getId());
    if (getExpansionLayer() != null) requiredLayers.add(getExpansionLayer().getId());
    if (getErrorsLayer() != null) requiredLayers.add(getErrorsLayer().getId());
    if (getRetracingLayer() != null) requiredLayers.add(getRetracingLayer().getId());
    if (getRepetitionsLayer() != null) requiredLayers.add(getRepetitionsLayer().getId());
    if (getCompletionLayer() != null) requiredLayers.add(getCompletionLayer().getId());
    if (getLinkageLayer() != null) requiredLayers.add(getLinkageLayer().getId());
    if (getCUnitLayer() != null) requiredLayers.add(getCUnitLayer().getId());
    if (getGemLayer() != null) requiredLayers.add(getGemLayer().getId());
    if (getTranscriberLayer() != null) requiredLayers.add(getTranscriberLayer().getId());
    if (getLanguagesLayer() != null) requiredLayers.add(getLanguagesLayer().getId());
    if (getMorLayer() != null) requiredLayers.add(getMorLayer().getId());
    for (String key : participantLayers.keySet()) {
      Layer layer = participantLayers.get(key);
      if (layer != null) {
        requiredLayers.add(layer.getId());
      }
    }
    return requiredLayers.toArray(new String[0]);
  } // getRequiredLayers()
   
  /**
   * Determines the cardinality between graphs and serialized streams.
   * <p>The cardinality of this deseerializer is NToN.
   * @return {@link nzilbb.ag.serialize.GraphSerializer#Cardinality}.NToN.
   */
  public Cardinality getCardinality() {
    return Cardinality.NToN;
  }

  /**
   * Serializes the given series of graphs, generating one or more {@link NamedStream}s.
   * <p>Many data formats will only yield one stream per graph (e.g. Transcriber
   * transcript or Praat textgrid), however there are formats that use multiple files for
   * the same transcript (e.g. XWaves, EmuR), and others still that will produce one
   * stream from many Graphs (e.g. CSV).
   * <p>The method is synchronous in the sense that it should not return until all graphs
   * have been serialized.
   * @param graphs The graphs to serialize.
   * @param layerIds The IDs of the layers to include, or null for all layers.
   * @param consumer The consumer receiving the streams.
   * @param warnings A consumer for (non-fatal) warning messages.
   * @param errors A consumer for (fatal) error messages.
   * @throws SerializerNotConfiguredException if the object has not been configured.
   */
  public void serialize(
    Spliterator<Graph> graphs, String[] layerIds, Consumer<NamedStream> consumer,
    Consumer<String> warnings, Consumer<SerializationException> errors) 
    throws SerializerNotConfiguredException {
    graphCount = graphs.getExactSizeIfKnown();
    graphs.forEachRemaining(graph -> {
        if (getCancelling()) return;
        try {
          consumer.accept(serializeGraph(graph, layerIds));
        } catch(SerializationException exception) {
          errors.accept(exception);
        }
        consumedGraphCount++;
      }); // next graph
  }

  /**
   * Serializes the given graph, generating a {@link NamedStream}.
   * @param graph The graph to serialize.
   * @return A named stream that contains the TextGrid. 
   * @throws SerializationException if errors occur during deserialization.
   */
  protected NamedStream serializeGraph(Graph graph, String[] layerIds)
    throws SerializationException {
    SerializationException errors = null;
      
    LinkedHashSet<String> selectedLayers = new LinkedHashSet<String>();
    if (layerIds != null) {
      for (String l : layerIds) {
        Layer layer = graph.getSchema().getLayer(l);
        if (layer != null) {
          selectedLayers.add(l);
        }
      } // next layeyId
    } else {
      for (Layer l : graph.getSchema().getLayers().values()) selectedLayers.add(l.getId());
    }
      
    try {
      // write the text to a temporary file
      File f = File.createTempFile(graph.getId(), ".cha");
      PrintWriter writer = new PrintWriter(
        new OutputStreamWriter(new FileOutputStream(f), "utf-8"));
      writer.println("@Begin");

      Schema schema = graph.getSchema();

      // convert stutters X~ to &+X
      if (disfluencyLayer == null) {
        new ConventionTransformer(
          wordLayer.getId(), "(.+)~", "&+$1", null, null)
          .transform(graph);
      } else { // prefix words with &+
        for (Annotation disfluency : graph.all(disfluencyLayer.getId())) {
          Annotation word = disfluency.first(wordLayer.getId());
          if (word != null) {
            word.setLabel("&+"+word.getLabel().replaceAll("~$",""));
          }
        }
      }
         
      // meta-data first
      if (languagesLayer != null) {
        Annotation[] annotations = graph.all(languagesLayer.getId());
        StringBuilder languages = new StringBuilder();
        for (Annotation a : annotations) {
          if (languages.length() == 0) {
            languages.append("@Languages:\t");
          } else {
            languages.append(",");
          }
          languages.append(iso639.alpha3(a.getLabel()).orElse(""));
        }
        if (languages.length() > 0) writer.println(languages);
      }
         
      // participants
      StringBuilder participantsHeader = new StringBuilder();
      HashMap<String,Annotation> participants = new HashMap<String,Annotation>();
      // @Participants header
      int subCount = 0;
      int intCount = 0;
      // annotate each participant with their code and role
      graph.getSchema().addLayer(
        new Layer("@code","SpeakerId").setAlignment(Constants.ALIGNMENT_NONE)
        .setPeers(false).setParentId(participantLayer.getId()));
      if (participantLayers.get("role") == null) {
        graph.getSchema().addLayer(
          new Layer("@role","Role").setAlignment(Constants.ALIGNMENT_NONE)
          .setPeers(false).setParentId(participantLayer.getId()));
        participantLayers.put("role", graph.getSchema().getLayer("@role"));
      }
      for (Annotation participant : graph.all(participantLayer.getId())) {
        String speakerId = null;
        String role = null;
        Annotation roleTag = participant.first(participantLayers.get("role").getId());
        if (roleTag != null) {
          role = roleTag.getLabel();
          subCount++;
          speakerId = participant.getLabel().substring(0,3).toUpperCase();
          if (participants.containsKey(speakerId)) {
            speakerId = participant.getLabel().substring(0,2).toUpperCase()+subCount;
          }
        } else if (targetParticipantLayer != null
                   && participant.first(targetParticipantLayer.getId()) != null) {
          role = "Participant";
          subCount++;
          if (!participants.containsKey("SUB")) {
            speakerId = "SUB";
          } else {
            speakerId = "SU"+subCount;
          }
        } else if (graph.getLabel().contains(participant.getLabel())) {
          // name of the participant is part of the graph name; probably a target
          role = "Participant";
          subCount++;
          if (!participants.containsKey("SUB")) {
            speakerId = "SUB";
          } else {
            speakerId = "SU"+subCount;
            }
        } else {
          role = "Investigator";
          intCount++;
          if (!participants.containsKey("INT")) {
            speakerId = "INT";
          } else {
            speakerId = "IN"+subCount;
          }
        }
        participant.createTag("@code", speakerId);
        participant.createTag(participantLayers.get("role").getId(), role);
        participants.put(speakerId, participant);
        if (participantsHeader.length() == 0) {
          participantsHeader.append("@Participants:\t");
        } else {
          participantsHeader.append(",");
        }
        participantsHeader.append(speakerId);
        participantsHeader.append(" ");
        participantsHeader.append(participant.getLabel().replace(' ', '_'));
        participantsHeader.append(" ");
        participantsHeader.append(role);
      } // next participant
      writer.println(participantsHeader);
      // @ID headers
      participantLayers.put("@code", graph.getSchema().getLayer("@code"));
      String[] fields = {
        "language","corpus","@code","age","sex","group","SES","role","education","custom"};
      for (Annotation participant : graph.all(participantLayer.getId())) {
        // @ID: language|corpus|code|age|sex|group|SES|role|education|custom|
            
        writer.print("@ID:\t");
        for (String field : fields) {               
          Layer layer = participantLayers.get(field);
          if (layer != null) {
            Annotation annotation = participant.first(layer.getId());
            if (annotation != null) {
              String value = annotation.getLabel();
              // standardize value...
              if (field.equals("sex")) {
                value = value.toLowerCase();
                if (!value.matches("[fwgmb]") // not something we recgonize
                    && layer.getValidLabels().containsKey(annotation.getLabel())) {
                  // try the label description?
                  value = layer.getValidLabels().get(annotation.getLabel())
                    .toLowerCase();
                }
                if (value.startsWith("f") // female?
                    || value.startsWith("w") // woman?
                    || value.startsWith("g")) { // girl?
                  value = "female";
                } else if (value.startsWith("m") // male? man?
                           || value.startsWith("b")) { // boy?
                  value = "male";
                } else {
                  value = ""+annotation.getLabel()+" " + layer.getValidLabels().get(annotation.getLabel());
                }
              } else if (field.equals("language")) { 
                value = iso639.alpha3(value).orElse("");
              } else if (field.equals("corpus")) {
                // one lowercase word
                value = value.toLowerCase().replaceAll("\\s","_");
              } else if (field.equals("group")) {
                // single word
                value = value.replaceAll("\\s","_");
              }
              writer.print(value);
            }
          }
          writer.print("|");
        } // next field
        writer.println();
      } // next participant
      // remove these again, in case this object is re-used...
      participantLayers.remove("@code");
      if (participantLayers.get("role").getId().equals("@role")) {
        participantLayers.put("role", null);
      }

      // more metadata
      if (dateLayer != null) {
        Annotation annotation = graph.first(dateLayer.getId());
        if (annotation != null) {
          writer.print("@Date:\t");
          SimpleDateFormat chatDateFormat
            // use UK locale because it doesn't expect a dot after a month abbreviation
            = new SimpleDateFormat("dd-MMM-yyyy",Locale.UK);
          SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd");
          try { // parse as ISO format
            Date date = isoDateFormat.parse(annotation.getLabel());
            // but save with CHAT format
            writer.println(chatDateFormat.format(date));
          } catch(ParseException exception) {
            warnings.add(
              "Could not parse date \""+annotation.getLabel()+"\": " + exception.getMessage());
            writer.println(annotation.getLabel());
          }
        } // there is an annotation
      } // layer is set
      if (locationLayer != null) {
        Annotation annotation = graph.first(locationLayer.getId());
        if (annotation != null) {
          writer.print("@Location:\t");
          writer.println(annotation.getLabel());
        } // there is an annotation
      } // layer is set
      if (recordingQualityLayer != null) {
        Annotation annotation = graph.first(recordingQualityLayer.getId());
        if (annotation != null) {
          writer.print("@Recording Quality:\t");
          writer.println(annotation.getLabel());
        } // there is an annotation
      } // layer is set
      if (roomLayoutLayer != null) {
        Annotation annotation = graph.first(roomLayoutLayer.getId());
        if (annotation != null) {
          writer.print("@Room Layout:\t");
          writer.println(annotation.getLabel());
        } // there is an annotation
      } // layer is set
      if (tapeLocationLayer != null) {
        Annotation annotation = graph.first(tapeLocationLayer.getId());
        if (annotation != null) {
          writer.print("@Tape Location:\t");
          writer.println(annotation.getLabel());
        } // there is an annotation
      } // layer is set
      if (transcriberLayer != null) {
        Annotation annotation = graph.first(transcriberLayer.getId());
        if (annotation != null) {
          writer.print("@Transcriber:\t");
          writer.println(annotation.getLabel());
        } // there is an annotation
      } // layer is set

      // get noises if needed
      TreeSet<Annotation> noisesByAnchor
        = new TreeSet<Annotation>(new AnnotationComparatorByAnchor());
      if (nonWordLayer != null) {
        // list all anchored noises
        for (Annotation n : graph.all(nonWordLayer.getId())) {
          if (n.getAnchored()) noisesByAnchor.add(n);
        }
      }
      Iterator<Annotation> nonWords = noisesByAnchor.iterator();
      Annotation nextNonWord = nonWords.hasNext()?nonWords.next():null;

      // for each utterance...
      Annotation currentParticipant = null;
         
      // order utterances by anchor so that simultaneous speech comes out in utterance order
      TreeSet<Annotation> utterancesByAnchor
        = new TreeSet<Annotation>(new AnnotationComparatorByAnchor());
      for (Annotation u : graph.all(getUtteranceLayer().getId())) utterancesByAnchor.add(u);

      Vector<String> morTags = new Vector<String>();
      for (Annotation utterance : utterancesByAnchor) {
        if (cancelling) break;
        // is the participant changing?
        Annotation participant = utterance.first(getParticipantLayer().getId());
        if (participant != currentParticipant) { // participant change

          // print %mor line?
          if (printMorLine(writer, morTags)) {
            writer.println();
          }
          
          // now change participant
          currentParticipant = participant;
          Object[] participantLabel = { currentParticipant.getLabel() }; 
          writer.print("*" + participant.first("@code").getLabel() + ":");
        } // participant change

        String delimiter = "\t";
        boolean printedSomething = false;
        for (Annotation token : utterance.all(getWordLayer().getId())) {
          printedSomething = true;
          writer.print(delimiter); // tab if it's the first word, space otherwise
          delimiter = " ";
               
          // is there a non-word to insert?
          while (nextNonWord != null
                 && token.getStart() != null
                 && token.getStart().getOffset() != null
                 && token.getStart().getOffset() >= nextNonWord.getStart().getOffset()) {
            String nonWord = standardNonWordLabel(nextNonWord.getLabel());
            if (nonWord != null) writer.print(nonWord + " ");
            nextNonWord = nonWords.hasNext()?nonWords.next():null;
          } // next noise
          writer.print(token.getLabel());

          if (morLayer != null && selectedLayers.contains(morLayer.getId())) {
            // are there MOR tags?
            Annotation[] mor = token.all(morLayer.getId());
            if (mor.length > 0) {
              // concatenate them together delimited by ^
              morTags.add(Arrays.stream(mor)
                          .map(m->m.getLabel())
                          .collect(Collectors.joining("^")));
            } // there are MOR tags
          } // morLayer set
        } // next token
        if (morTags.size() > 0) { // mark a line break in the %mor line
          morTags.add(null);
        }
            
        // is there a non-word to append to the end of the line?
        while (nextNonWord != null
               && utterance.getEnd() != null
               && utterance.getEnd().getOffset() != null
               && utterance.getEnd().getOffset() >= nextNonWord.getStart().getOffset()) {
          String nonWord = standardNonWordLabel(nextNonWord.getLabel());
          if (nonWord != null) {
            writer.print(delimiter + nonWord);
            delimiter = " ";
            printedSomething = true;
          }
          nextNonWord = nonWords.hasNext()?nonWords.next():null;
        } // next noise

        if (includeTimeCodes
            && utterance.getStart().getOffset() != null
            && utterance.getEnd().getOffset() != null) {
          // time code
          writer.print(delimiter);
          writer.print("");
          int ms = (int)(utterance.getStart().getOffset() * 1000);
          writer.print("" + ms);
          writer.print("_");
          ms = (int)(utterance.getEnd().getOffset() * 1000);
          writer.print("" + ms);
          writer.println("");
        } else if (printedSomething) {
          writer.println();
        }
      } // next utterance
      
      // print last %mor line?
      printMorLine(writer, morTags);
      writer.println("@End");
      writer.close();

      TempFileInputStream in = new TempFileInputStream(f);

      graph.trackChanges();
      for (Annotation code : graph.all("@code")) code.destroy();
      participantLayer.getChildren().remove("@code");
      graph.getSchema().getLayers().remove("@code");
      for (Annotation role : graph.all("@role")) role.destroy();
      participantLayer.getChildren().remove("@role");
      graph.getSchema().getLayers().remove("@role");
      graph.commit();

      // return a named stream from the file
      String streamName = graph.getId();
      if (!IO.Extension(streamName).equals("cha")) {
        streamName = IO.WithoutExtension(streamName) + ".cha";
      }
      return new NamedStream(in, IO.SafeFileNameUrl(streamName));
    } catch(Exception exception) {
      errors = new SerializationException(exception.getMessage());
      errors.initCause(exception);
      errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
      throw errors;
    }      
  }
  
  /**
   * Print a %mor line with the given tags.
   * @param writer
   * @param morTags
   * @return true if a %mor line was printed, false otherwise.
   * @throws IOException
   */
  private boolean printMorLine(PrintWriter writer, Vector<String> morTags) throws IOException {
    // print %mor line?
    if (morTags.size() > 0) {
      writer.print("%mor:");
      int maxCol = 90;
      int col = 0;
      String delimiter = "\t";
      for (String mor : morTags) {
        if (mor == null // line break matching main line
            || col + mor.length() + 1 > maxCol) { // line wrap
          writer.println();
          delimiter = "\t";
          col = -1;
          if (mor == null) continue;
        } 
        writer.print(delimiter);
        writer.print(mor);
        col += mor.length() + 1;
        delimiter = " ";
      } // next mor tag
      morTags.clear();
      return true;
    }
    return false;
  } // end of printMorLine()
  
  private static String[] standardNonWords = {
    "belches","coughs","cries","gasps","groans","growls","hisses","hums","laughs","moans",
    "mumbles","pants","grunts","roars","sneezes","sighs","sings","squeals","whines",
    "whistles","whimpers","yawns","yells","vocalizes"
  };
  /**
   * Determines whether the given non-word annotation label can be converted to a
   * standardized non-word annotation. If so, the standard annotation is returned. 
   * @param rawLabel
   * @return A standard non-word annotation, or null.
   */
  public String standardNonWordLabel(String rawLabel) {
    if (rawLabel == null) return null;
    for (String standardNonWord : standardNonWords) {
      if (rawLabel.equals(standardNonWords)
          || rawLabel.equals(standardNonWord.replaceAll("s$", ""))
          || rawLabel.equals(standardNonWord.replaceAll("es$", ""))
          || rawLabel.equals(standardNonWord.replaceAll("ies$", "y"))
          || rawLabel.equals(standardNonWord.replaceAll("s$", "ing"))
          || rawLabel.equals(standardNonWord.replaceAll("es$", "ing"))
          || rawLabel.equals(standardNonWord.replaceAll("yes$", "ying"))
          || rawLabel.equals(standardNonWord.replaceAll("ms$", "mming"))) {
        return "&="+standardNonWord;
      }
    }
    return null;
  } // end of standardNonWordLabel()

} // end of class ChatSerialization
