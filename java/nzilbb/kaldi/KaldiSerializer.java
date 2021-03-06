//
// Copyright 2018 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.kaldi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.Vector;
import java.util.function.Consumer;
import nzilbb.ag.*;
import nzilbb.ag.serialize.ISerializer;
import nzilbb.ag.serialize.SerializationDescriptor;
import nzilbb.ag.serialize.SerializationException;
import nzilbb.ag.serialize.SerializationParametersMissingException;
import nzilbb.ag.serialize.SerializerNotConfiguredException;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.util.TempFileInputStream;

/**
 * Converter that generates Kaldi files from annotation graphs.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class KaldiSerializer
   implements ISerializer
{
   // Attributes:

   /** Static format so we don't keep creating and destroying one */
   private static DecimalFormat fmt = new DecimalFormat(
      // force the locale to something with . as the decimal separator
      "0.000", new DecimalFormatSymbols(Locale.UK));
   
   protected Vector<String> warnings;
   /**
    * Returns any warnings that may have arisen during the last execution of {@link #deserialize()}.
    * <p>{@link ISerializer} and {@link IDeserializer} method.
    * @return A possibly empty list of warnings.
    */
   public String[] getWarnings()
   {
      return warnings.toArray(new String[0]);
   }
   
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
    * Word token layer.
    * @see #getWordLayer()
    * @see #setWordLayer(Layer)
    */
   protected Layer wordLayer;
   /**
    * Getter for {@link #wordLayer}: Word token layer.
    * @return Word token layer.
    */
   public Layer getWordLayer() { return wordLayer; }
   /**
    * Setter for {@link #wordLayer}: Word token layer.
    * @param newWordLayer Word token layer.
    */
   public void setWordLayer(Layer newWordLayer) { wordLayer = newWordLayer; }

   /**
    * Layer for orthography word tags.
    * @see #getOrthographyLayer()
    * @see #setOrthographyLayer(Layer)
    */
   protected Layer orthographyLayer;
   /**
    * Getter for {@link #orthographyLayer}: Layer for orthography tags.
    * @return Layer for orthography tags.
    */
   public Layer getOrthographyLayer() { return orthographyLayer; }
   /**
    * Setter for {@link #orthographyLayer}: Layer for orthography tags.
    * @param newOrthographyLayer Layer for orthography tags.
    */
   public void setOrthographyLayer(Layer newOrthographyLayer) { orthographyLayer = newOrthographyLayer; }


   /**
    * Participant information layer.
    * @see #getParticipantLayer()
    * @see #setParticipantLayer(Layer)
    */
   protected Layer participantLayer;
   /**
    * Getter for {@link #participantLayer}: Participant information layer.
    * @return Participant information layer.
    */
   public Layer getParticipantLayer() { return participantLayer; }
   /**
    * Setter for {@link #participantLayer}: Participant information layer.
    * @param newParticipantLayer Participant information layer.
    */
   public void setParticipantLayer(Layer newParticipantLayer) { participantLayer = newParticipantLayer; }

   /**
    * Episode information layer.
    * @see #getEpisodeLayer()
    * @see #setEpisodeLayer(Layer)
    */
   protected Layer episodeLayer;
   /**
    * Getter for {@link #episodeLayer}: Episode information layer.
    * @return Episode information layer.
    */
   public Layer getEpisodeLayer() { return episodeLayer; }
   /**
    * Setter for {@link #episodeLayer}: Episode information layer.
    * @param newEpisodeLayer Episode information layer.
    */
   public void setEpisodeLayer(Layer newEpisodeLayer) { episodeLayer = newEpisodeLayer; }


   /**
    * Serialization marked for cancellation.
    * @see #getCancelling()
    * @see #setCancelling(boolean)
    */
   protected boolean cancelling = false;
   /**
    * Getter for {@link #cancelling}: Serialization marked for cancellation.
    * @return Serialization marked for cancellation.
    */
   public boolean getCancelling() { return cancelling; }
   /**
    * Setter for {@link #cancelling}: Serialization marked for cancellation.
    * @param newCancelling Serialization marked for cancellation.
    */
   public void setCancelling(boolean newCancelling) { cancelling = newCancelling; }

   // Methods:
   
   /**
    * Default constructor.
    */
   public KaldiSerializer()
   {
   } // end of constructor

   /**
    * Returns the deserializer's descriptor.
    * <p>{@link ISerializer} and {@link IDeserializer} method.
    * @return The deserializer's descriptor
    */
   public SerializationDescriptor getDescriptor()
   {
      return new SerializationDescriptor(
	 "Kaldi Files", "1.1", "text/x-kaldi-text", ".kaldi", "20200909.1954", getClass().getResource("icon.png"));
   }

   /**
    * Sets parameters for deserializer as a whole.  This might include database connection parameters, locations of supporting files, etc.
    * <p>When the deserializer is installed, this method should be invoked with an empty parameter
    *  set, to discover what (if any) general configuration is required. If parameters are
    *  returned, and user interaction is possible, then the user may be presented with an
    *  interface for setting/confirming these parameters.
    * <p>{@link ISerializer} and {@link IDeserializer} method.
    * @param configuration The configuration for the deserializer. 
    * @param schema The layer schema, definining layers and the way they interrelate.
    * @return A list of configuration parameters (still) must be set before {@link IDeserializer#setParameters()} can be invoked. If this is an empty list, {@link IDeserializer#setParameters()} can be invoked. If it's not an empty list, this method must be invoked again with the returned parameters' values set.
    */
   public ParameterSet configure(ParameterSet configuration, Schema schema)
   {
      setSchema(schema);
      setParticipantLayer(schema.getParticipantLayer());
      setTurnLayer(schema.getTurnLayer());
      setUtteranceLayer(schema.getUtteranceLayer());
      setWordLayer(schema.getWordLayer());

      // set any values that have been passed in
      for (Parameter p : configuration.values()) try { p.apply(this); } catch(Exception x) {}

      // create a list of layers we need and possible matching layer names
      LinkedHashMap<Parameter,List<String>> layerToPossibilities = new LinkedHashMap<Parameter,List<String>>();
      HashMap<String,LinkedHashMap<String,Layer>> layerToCandidates = new HashMap<String,LinkedHashMap<String,Layer>>();

      // do we need to ask for participant/turn/utterance/word layers?
      LinkedHashMap<String,Layer> possibleEpisodeLayers = new LinkedHashMap<String,Layer>();
      for (Layer top : schema.getRoot().getChildren().values())
      {
	 if (top.getAlignment() == Constants.ALIGNMENT_NONE)
	 {
	    possibleEpisodeLayers.put(top.getId(), top);
	 } // unaligned
      } // next possible participant layer
      
      LinkedHashMap<String,Layer> possibleParticipantLayers = new LinkedHashMap<String,Layer>();
      LinkedHashMap<String,Layer> possibleTurnLayers = new LinkedHashMap<String,Layer>();
      LinkedHashMap<String,Layer> possibleTurnChildLayers = new LinkedHashMap<String,Layer>();
      LinkedHashMap<String,Layer> wordTagLayers = new LinkedHashMap<String,Layer>();
      if (!(getParticipantLayer() == null || getTurnLayer() == null 
	    || getUtteranceLayer() == null || getWordLayer() == null))
      {
	 for (Layer top : schema.getRoot().getChildren().values())
	 {
	    if (top.getAlignment() == Constants.ALIGNMENT_NONE)
	    {
	       if (top.getChildren().size() != 0)
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
	 for (Layer turnChild : getTurnLayer().getChildren().values())
	 {
	    if (turnChild.getAlignment() == Constants.ALIGNMENT_INTERVAL)
	    {
	       possibleTurnChildLayers.put(turnChild.getId(), turnChild);
	    }
	 } // next possible word tag layer
	 for (Layer tag : getWordLayer().getChildren().values())
	 {
	    if (tag.getAlignment() == Constants.ALIGNMENT_NONE
		&& tag.getChildren().size() == 0)
	    {
	       wordTagLayers.put(tag.getId(), tag);
	    }
	 } // next possible word tag layer
      }
      if (getParticipantLayer() == null)
      {
	 layerToPossibilities.put(
	    new Parameter("participantLayer", Layer.class, "Participant layer", 
			  "Layer for speaker/participant identification", true), 
	    Arrays.asList("participant","participants","who","speaker","speakers"));
	 layerToCandidates.put("participantLayer", possibleParticipantLayers);
      }
      if (getTurnLayer() == null)
      {
	 layerToPossibilities.put(
	    new Parameter("turnLayer", Layer.class, "Turn layer", "Layer for speaker turns", true),
	    Arrays.asList("turn","turns"));
	 layerToCandidates.put("turnLayer", possibleTurnLayers);
      }
      if (getUtteranceLayer() == null)
      {
	 layerToPossibilities.put(
	    new Parameter("utteranceLayer", Layer.class, "Utterance layer", 
			  "Layer for speaker utterances", true), 
	    Arrays.asList("utterance","utterances","line","lines"));
	 layerToCandidates.put("utteranceLayer", possibleTurnChildLayers);
      }
      if (getWordLayer() == null)
      {
	 layerToPossibilities.put(
	    new Parameter("wordLayer", Layer.class, "Word layer", 
			  "Layer for individual word tokens", true), 
	    Arrays.asList("transcript","word","words","w"));
	 layerToCandidates.put("wordLayer", possibleTurnChildLayers);
      }

      LinkedHashMap<String,Layer> topLevelLayers = new LinkedHashMap<String,Layer>();
      for (Layer top : schema.getRoot().getChildren().values())
      {
	 if (top.getAlignment() == Constants.ALIGNMENT_INTERVAL)
	 { // aligned children of graph
	    topLevelLayers.put(top.getId(), top);
	 }
      } // next top level layer

      // other layers...

      layerToPossibilities.put(
	 new Parameter("episodeLayer", Layer.class, "Episode layer", "Episode"), 
	 Arrays.asList("episode","series","family"));
      layerToCandidates.put("episodeLayer", possibleEpisodeLayers);

      layerToPossibilities.put(
	 new Parameter("orthographyLayer", Layer.class, "Orthography layer", "Orthography tags"), 
	 Arrays.asList("orthography"));
      layerToCandidates.put("orthographyLayer", wordTagLayers);

      // add parameters that aren't in the configuration yet, and set possibile/default values
      for (Parameter p : layerToPossibilities.keySet())
      {
	 List<String> possibleNames = layerToPossibilities.get(p);
	 LinkedHashMap<String,Layer> candidateLayers = layerToCandidates.get(p.getName());
	 if (configuration.containsKey(p.getName()))
	 {
	    p = configuration.get(p.getName());
	 }
	 else
	 {
	    configuration.addParameter(p);
	 }
	 if (p.getValue() == null)
	 {
	    p.setValue(Utility.FindLayerById(candidateLayers, possibleNames));
	 }
	 p.setPossibleValues(candidateLayers.values());
      }

      return configuration;
   }   

   /**
    * Determines which layers, if any, must be present in the graph that will be serialized.
    * <p>{@link ISerializer} method.
    * @return A list of IDs of layers that must be present in the graph that will be serialized.
    * @throws SerializationParametersMissingException If not all required parameters have a value.
    */
   public String[] getRequiredLayers() throws SerializationParametersMissingException
   {
      Vector<String> requiredLayers = new Vector<String>();
      if (getEpisodeLayer() != null) requiredLayers.add(getEpisodeLayer().getId());
      if (getParticipantLayer() != null) requiredLayers.add(getParticipantLayer().getId());
      if (getUtteranceLayer() != null) requiredLayers.add(getUtteranceLayer().getId());
      if (getOrthographyLayer() != null) requiredLayers.add(getOrthographyLayer().getId());
      return requiredLayers.toArray(new String[0]);
   }

   // ISerializer methods   

   /**
    * Serializes the given graph, generating one or more {@link NamedStream}s.
    * <p>Many data formats will only yield one stream (e.g. Transcriber transcript or Praat
    *  textgrid), however there are formats that use multiple files for the same transcript
    *  (e.g. XWaves, EmuR), which is why this method returns a list. There are formats that
    *  are capable of storing multiple transcripts in the same file (e.g. AGTK, Transana XML
    *  export), which is why this method accepts a list.
    * @param graphs The graphs to serialize.
    * @param layerIds The IDs of the layers to include, or null for all layers.
    * @param consumer The object receiving the streams.
    * @param warnings The object receiving warning messages.
    * @return A list of named streams that contain the serialization in the given format. 
    * @throws SerializerNotConfiguredException if the object has not been configured.
    */
   public void serialize(Spliterator<Graph> graphs, String[] layerIds, Consumer<NamedStream> consumer, Consumer<String> warnings, Consumer<SerializationException> errors) 
      throws SerializerNotConfiguredException
   {
      graphCount = graphs.getExactSizeIfKnown();
      try
      {
	 // prepare streams for writing...
	 
	 File textFile = File.createTempFile(getClass().getSimpleName()+"-","-text");
	 final PrintWriter textWriter = new PrintWriter(textFile, "utf-8");
	 NamedStream textStream = new NamedStream(new TempFileInputStream(textFile), "text");
	 
	 File corpusFile = File.createTempFile(getClass().getSimpleName()+"-","-corpus.txt");
	 PrintWriter corpusWriter = new PrintWriter(corpusFile, "utf-8");
	 NamedStream corpusStream = new NamedStream(new TempFileInputStream(corpusFile), "corpus.txt");
	 
	 File segmentsFile = File.createTempFile(getClass().getSimpleName()+"-","-segments");
	 final PrintWriter segmentsWriter = new PrintWriter(segmentsFile, "utf-8");
	 NamedStream segmentsStream = new NamedStream(new TempFileInputStream(segmentsFile), "segments");
	 
	 File utt2spkFile = File.createTempFile(getClass().getSimpleName()+"-","-utt2spk");
	 final PrintWriter utt2spkWriter = new PrintWriter(utt2spkFile, "utf-8");
	 NamedStream utt2spkStream = new NamedStream(new TempFileInputStream(utt2spkFile), "utt2spk");
	 
	 File wordsFile = File.createTempFile(getClass().getSimpleName()+"-","-words.txt");
	 final PrintWriter wordsWriter = new PrintWriter(wordsFile, "utf-8");
	 NamedStream wordsStream = new NamedStream(new TempFileInputStream(wordsFile), "words.txt");
	 
	 File wavFile = File.createTempFile(getClass().getSimpleName()+"-","-wav.scp");
	 final PrintWriter wavWriter = new PrintWriter(wavFile, "utf-8");
	 NamedStream wavStream = new NamedStream(new TempFileInputStream(wavFile), "wav.scp");

	 String utt = getUtteranceLayer().getId();
	 String orthography = getOrthographyLayer().getId();
	 String speaker = getParticipantLayer().getId();
	 String episode = getEpisodeLayer().getId();

	 final SortedSet<String> words = new TreeSet<String>();
	 final SortedSet<String> wavs = new TreeSet<String>();

	 StringBuffer lastMediaName = new StringBuffer();
         graphs.forEachRemaining(graph -> {
               if (getCancelling()) return;
               String transcriptName = graph.getId().replaceAll("__[0-9.]+-[0-9.]+$","");
               String wavName = transcriptName.replaceAll("\\.[^.]+$","")+".wav";
               boolean firstWord = true;
               for (Annotation utterance : graph.list(utt))
               {
                  String speakerId = utterance.my(speaker).getId();
                  String utteranceId = speakerId + "-" + graph.getId();
                  textWriter.print(utteranceId);
                  for (Annotation token : utterance.list(orthography))
                  {
                     textWriter.print(" ");
                     textWriter.print(token.getLabel());
                     
                     if (firstWord)
                     {
                        firstWord = false;
                     }
                     else
                     {
                        corpusWriter.print(" ");
                     }
                     corpusWriter.print(token.getLabel());
                     
                     words.add(token.getLabel());
                  } // next word token
                  textWriter.println();
                  corpusWriter.println();
                  
                  segmentsWriter.println(
                     utteranceId
                     + " " + transcriptName
                     + " " + fmt.format(graph.getStart().getOffset())
                     + " " + fmt.format(graph.getEnd().getOffset()));
                  
                  utt2spkWriter.println(utteranceId + " " + utterance.my(speaker).getId());

                  if (!wavs.contains(transcriptName))
                  {
                     wavs.add(transcriptName);
                     wavWriter.println(transcriptName
                                       + " " + graph.my(episode).getLabel() + "/wav/" + wavName); // TODO just point to original files
                  }
               } // next utterance
               consumedGraphCount++;
            }); // next graph
         
	 if (getCancelling())
	 {
	    // close the streams
	    textWriter.close();
	    corpusWriter.close();
	    segmentsWriter.close();
	    utt2spkWriter.close();
	    wavWriter.close();
	    wordsWriter.close();
	 }
	 else
	 {
	    // close the streams
	    textWriter.close();
	    corpusWriter.close();
	    segmentsWriter.close();
	    utt2spkWriter.close();
	    wavWriter.close();
	    
	    // pass them to the consumer
	    consumer.accept(textStream);
	    consumer.accept(corpusStream);
	    consumer.accept(segmentsStream);
	    consumer.accept(utt2spkStream);
	    consumer.accept(wavStream);
	    
	    // finally, words (sorted)
	    for (String word : words) wordsWriter.println(word);
	    wordsWriter.close();
	    consumer.accept(wordsStream);
	 }
      }
      catch(Exception exception)
      {
	 errors.accept(new SerializationException(exception));
      }
   }

   private long graphCount = 0;
   private long consumedGraphCount = 0;
   /**
    * Determines how far through the serialization is.
    * @return An integer between 0 and 100 (inclusive), or null if progress can not be calculated.
    */
   public Integer getPercentComplete()
   {
      if (graphCount < 0) return null;
      return (int)((consumedGraphCount * 100) / graphCount);
   }

   /**
    * Cancel the serialization in course (if any).
    */
   public void cancel()
   {
      setCancelling(true);
   }
   
} // end of class KaldiSerializer
