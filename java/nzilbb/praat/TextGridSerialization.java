//
// Copyright 2004-2020 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.praat;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.function.Consumer;
import nzilbb.ag.*;
import nzilbb.ag.serialize.*;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.util.AnnotationComparatorByAnchor;
import nzilbb.ag.util.ConventionTransformer;
import nzilbb.ag.util.OrthographyClumper;
import nzilbb.ag.util.SimpleTokenizer;
import nzilbb.ag.util.SpanningConventionTransformer;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.util.IO;
import nzilbb.util.TempFileInputStream;
import nzilbb.util.Timers;

/**
 * Converter that converts Praat TextGrids to/from AnnotationGraphs
 * @author Robert Fromont robert@fromont.net.nz
 */
public class TextGridSerialization
   extends TextGrid
   implements IDeserializer, ISerializer
{
   // Attributes:     
   protected Vector<String> warnings;
   /**
    * Returns any warnings that may have arisen during the last execution of {@link #deserialize()} or  {@link #serialize(Graph[],String[])}.
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
    * Layer for lexical word tags.
    * @see #getLexicalLayer()
    * @see #setLexicalLayer(Layer)
    */
   protected Layer lexicalLayer;
   /**
    * Getter for {@link #lexicalLayer}: Layer for lexical word tags.
    * @return Layer for lexical word tags.
    */
   public Layer getLexicalLayer() { return lexicalLayer; }
   /**
    * Setter for {@link #lexicalLayer}: Layer for lexical word tags.
    * @param newLexicalLayer Layer for lexical word tags.
    */
   public void setLexicalLayer(Layer newLexicalLayer) { lexicalLayer = newLexicalLayer; }

   /**
    * Layer for pronounce events.
    * @see #getPronounceLayer()
    * @see #setPronounceLayer(Layer)
    */
   protected Layer pronounceLayer;
   /**
    * Getter for {@link #pronounceLayer}: Layer for pronounce events.
    * @return Layer for pronounce events.
    */
   public Layer getPronounceLayer() { return pronounceLayer; }
   /**
    * Setter for {@link #pronounceLayer}: Layer for pronounce events.
    * @param newPronounceLayer Layer for pronounce events.
    */
   public void setPronounceLayer(Layer newPronounceLayer) { pronounceLayer = newPronounceLayer; }

   /**
    * Layer for commentary.
    * @see #getCommentLayer()
    * @see #setCommentLayer(Layer)
    */
   protected Layer commentLayer;
   /**
    * Getter for {@link #commentLayer}: Layer for commentary.
    * @return Layer for commentary.
    */
   public Layer getCommentLayer() { return commentLayer; }
   /**
    * Setter for {@link #commentLayer}: Layer for commentary.
    * @param newCommentLayer Layer for commentary.
    */
   public void setCommentLayer(Layer newCommentLayer) { commentLayer = newCommentLayer; }

   /**
    * Layer for noise annotations.
    * @see #getNoiseLayer()
    * @see #setNoiseLayer(Layer)
    */
   protected Layer noiseLayer;
   /**
    * Getter for {@link #noiseLayer}: Layer for noise annotations.
    * @return Layer for noise annotations.
    */
   public Layer getNoiseLayer() { return noiseLayer; }
   /**
    * Setter for {@link #noiseLayer}: Layer for noise annotations.
    * @param newNoiseLayer Layer for noise annotations.
    */
   public void setNoiseLayer(Layer newNoiseLayer) { noiseLayer = newNoiseLayer; }

   /**
    * Whether to use text conventions for comment, noise, lexical, and pronounce annotations.
    * @see #getUseConventions()
    * @see #setUseConventions(Boolean)
    */
   protected Boolean bUseConventions = Boolean.TRUE;
   /**
    * Getter for {@link #bUseConventions}: Whether to use text conventions for comment, noise, lexical, and pronounce annotations.
    * @return Whether to use text conventions for comment, noise, lexical, and pronounce annotations.
    */
   public Boolean getUseConventions() { return bUseConventions; }
   /**
    * Setter for {@link #bUseConventions}: Whether to use text conventions for comment, noise, lexical, and pronounce annotations.
    * @param bNewTranscriptOnly Whether to use text conventions for comment, noise, lexical, and pronounce annotations.
    */
   public void setUseConventions(Boolean bNewUseConventions) { bUseConventions = bNewUseConventions; }
   
   /**
    * Short speaker names like "S1" should be prefixed with the transcript name during import.
    * @see #getRenameShortNumericSpeakers()
    * @see #setRenameShortNumericSpeakers(Boolean)
    */
   protected Boolean renameShortNumericSpeakers = Boolean.FALSE;
   /**
    * Getter for {@link #renameShortNumericSpeakers}: Short speaker names like "S1" should be prefixed with the transcript name during import.
    * @return Short speaker names like "S1" should be prefixed with the transcript name during import.
    */
   public Boolean getRenameShortNumericSpeakers() { return renameShortNumericSpeakers; }
   /**
    * Setter for {@link #renameShortNumericSpeakers}: Short speaker names like "S1" should be prefixed with the transcript name during import.
    * @param newRenameShortNumericSpeakers Short speaker names like "S1" should be prefixed with the transcript name during import.
    */
   public void setRenameShortNumericSpeakers(Boolean newRenameShortNumericSpeakers) { renameShortNumericSpeakers = newRenameShortNumericSpeakers; }

   // IStreamDeserializer methods:
   
   /**
    * Returns the deserializer's descriptor.
    * <p>{@link ISerializer} and {@link IDeserializer} method.
    * @return The deserializer's descriptor
    */
   public SerializationDescriptor getDescriptor()
   {
      return new SerializationDescriptor(
         "Praat TextGrid", "2.12", "text/praat-textgrid", ".textgrid", "20191031.1734",
         getClass().getResource("icon.png"));
   }
   
   /**
    * Graph ID.
    * @see #getId()
    * @see #setId(String)
    */
   protected String id;
   /**
    * Getter for {@link #id}: Graph ID.
    * @return Graph ID.
    */
   public String getId() { return id; }
   /**
    * Setter for {@link #id}: Graph ID.
    * @param newId Graph ID.
    */
   public void setId(String newId) { id = newId; }

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

   
   /**
    * Timers for measuring performance.
    * @see #getTimers()
    * @see #setTimers(Timers)
    */
   protected Timers timers;
   /**
    * Getter for {@link #timers}: Timers for measuring performance.
    * @return Timers for measuring performance.
    */
   public Timers getTimers() { return timers; }
   /**
    * Setter for {@link #timers}: Timers for measuring performance.
    * @param newTimers Timers for measuring performance.
    */
   public void setTimers(Timers newTimers) { timers = newTimers; }
   
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
   public TextGridSerialization setCancelling(boolean newCancelling) { cancelling = newCancelling; return this; }
   /**
    * Cancel the serialization in course (if any).
    */
   public void cancel()
   {
      setCancelling(true);
   }
   
   // Methods:
   
   /**
    * Constructor
    */
   public TextGridSerialization()
   {
   } // end of constructor
   
   // IDeserializer methods

   protected ParameterSet mappings;

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
      LinkedHashMap<String,Layer> possibleParticipantLayers = new LinkedHashMap<String,Layer>();
      LinkedHashMap<String,Layer> possibleTurnLayers = new LinkedHashMap<String,Layer>();
      LinkedHashMap<String,Layer> possibleTurnChildLayers = new LinkedHashMap<String,Layer>();
      LinkedHashMap<String,Layer> wordTagLayers = new LinkedHashMap<String,Layer>();
      if (getParticipantLayer() == null || getTurnLayer() == null 
          || getUtteranceLayer() == null || getWordLayer() == null)
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
         new Parameter("commentLayer", Layer.class, "Comment layer", "Commentary"), 
         Arrays.asList("comment","commentary","note","notes"));
      layerToCandidates.put("commentLayer", topLevelLayers);

      layerToPossibilities.put(
         new Parameter("noiseLayer", Layer.class, "Noise layer", "Noise annotations"), 
         Arrays.asList("noise","noises","backgroundnoise"));
      layerToCandidates.put("noiseLayer", topLevelLayers);

      layerToPossibilities.put(
         new Parameter("lexicalLayer", Layer.class, "Lexical layer", "Lexical tags"), 
         Arrays.asList("lexical"));
      layerToCandidates.put("lexicalLayer", wordTagLayers);

      layerToPossibilities.put(
         new Parameter("pronounceLayer", Layer.class, "Pronounce layer", 
                       "Manual pronunciation tags"), 
         Arrays.asList("pronounce"));
      layerToCandidates.put("pronounceLayer", wordTagLayers);

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

      if (!configuration.containsKey("renameShortNumericSpeakers"))
      {
         configuration.addParameter(
            new Parameter("renameShortNumericSpeakers", Boolean.class, 
                          "Rename Short Numeric Speakers",
                          "Short speaker names like 'S1' should be prefixed with the transcript name during import", true));
      }
      if (configuration.get("renameShortNumericSpeakers").getValue() == null)
      {
         configuration.get("renameShortNumericSpeakers").setValue(Boolean.TRUE);
      }

      if (!configuration.containsKey("useConventions"))
      {
         configuration.addParameter(
            new Parameter("useConventions", Boolean.class, 
                          "Use Annotation Conventions",
                          "Whether to use text conventions for comment, noise, lexical, and pronounce annotations", true));
      }
      if (configuration.get("useConventions").getValue() == null)
      {
         configuration.get("useConventions").setValue(Boolean.TRUE);
      }

      return configuration;
   }   

   /**
    * Loads the serialized form of the graph, using the given set of named streams.
    * <p>{@link IDeserializer} method.
    * @param streams A list of named streams that contain all the
    *  transcription/annotation data required, and possibly (a) stream(s) for the media annotated.
    * @param schema The layer schema, definining layers and the way they interrelate.
    * @return A list of parameters that require setting before {@link IDeserializer#deserialize()}
    * can be invoked. This may be an empty list, and may include parameters with the value already
    * set to a workable default. If there are parameters, and user interaction is possible, then
    * the user may be presented with an interface for setting/confirming these parameters, before
    * they are then passed to {@link IDeserializer#setParameters(ParameterSet)}.
    * @throws SerializationException If the graph could not be loaded.
    * @throws IOException On IO error.
    */
   @SuppressWarnings({"rawtypes", "unchecked"})
   public ParameterSet load(NamedStream[] streams, Schema schema) throws SerializationException, IOException
   {
      if (timers != null) timers.start("load");
      reset();
      setSchema(schema);

      // take the first stream, ignore all others.
      NamedStream textgrid = Utility.FindSingleStream(streams, ".textgrid", "text/praat-textgrid");
      if (textgrid == null) throw new SerializationException("No Praat TextGrid stream found");
      setId(textgrid.getName());
      
      // convert from UTF-16 to UTF-8 if necessary
      InputStream in = textgrid.getStream();
      ByteArrayOutputStream oBytes = new ByteArrayOutputStream();
      byte[] aBytes = new byte[1024];
      int iBytesRead = in.read(aBytes);
      while (iBytesRead >= 0)
      {
         oBytes.write(aBytes, 0, iBytesRead);
         iBytesRead = in.read(aBytes);
      } // next chunk	
      in.close();
      try
      { // first try as latin1
         ByteBuffer bytes = ByteBuffer.wrap(oBytes.toByteArray());
         CharsetDecoder utf8Decoder = Charset.forName("UTF-8").newDecoder();
         CharBuffer utf8Buffer = utf8Decoder.decode(bytes);
         readText(new BufferedReader(new CharArrayReader(utf8Buffer.array())));
      }
      catch (Throwable t)
      { // if that doesn't work, try utf...
         reset();
         try
         {
            ByteBuffer bytes = ByteBuffer.wrap(oBytes.toByteArray());	    
            // is there a byte-order mark for UTF-16? i.e. FEFF
            if (bytes.get(0) == (byte)0xFE && bytes.get(1) == (byte)0xFF)
            {
               Charset utf16 = Charset.forName("UTF-16");
               CharBuffer utf8Buffer = utf16.decode(bytes);
               readText(new BufferedReader(new CharArrayReader(utf8Buffer.array())));
            }
            else
            { // assume UTF-8
               Charset latin1 = Charset.forName("ISO-8859-1");
               CharBuffer utf8Buffer = latin1.decode(bytes);
               readText(new BufferedReader(new CharArrayReader(utf8Buffer.array())));
            }
	    
         }
         catch(ClassNotFoundException exception)
         {
            throw new SerializationException(exception);
         }
         catch(InstantiationException exception)
         {
            throw new SerializationException(exception);
         }
         catch(IllegalAccessException exception)
         {
            throw new SerializationException(exception);
         }
      }

      ParameterSet mappings = new ParameterSet();
      Vector<Layer> vIntervalLayers = new Vector<Layer>();
      Vector<Layer> vPointLayers = new Vector<Layer>();
      for (Layer layer : getSchema().getLayers().values())
      {
         if (!layer.getId().equals(getSchema().getRoot().getId())
             && !layer.getId().equals(getParticipantLayer().getId()))
         {
            switch (layer.getAlignment())
            {
               case 0: 
                  if (!layer.getParentId().equals(getSchema().getRoot().getId())
                      && !layer.getParentId().equals(getParticipantLayer().getId()))
                  { // not graph or participant tags
                     vIntervalLayers.add(layer); 
                  }
                  break; 
               case 2: vIntervalLayers.add(layer); break; 
               case 1: vPointLayers.add(layer); break; 
            }
         }
      } // next layer

      // map tiers to layers by name
      Layer ignore = new Layer();
      ignore.setId("[ignore tier]");      
      for (int t = 0; t < getTiers().size(); t++)
      {
         Tier tier = getTiers().elementAt(t);

         Parameter p = new Parameter(
            "tier"+t, Layer.class, tier.getName(),
            "Layer for tier called: " + tier.getName(), true);
         Vector<Layer> vPossiblLayers = new Vector<Layer>();
         vPossiblLayers.add(ignore);
         if (tier instanceof IntervalTier)
         { // interval layer?
            vPossiblLayers.addAll(vIntervalLayers);
         }
         else // TextTier
         {
            vPossiblLayers.addAll(vPointLayers);
         }	 
         // look for a layer with the same name
         String sName = tier.getName();
         if (sName.equalsIgnoreCase("lines")
             || sName.equalsIgnoreCase("utterances"))
         {
            sName = getUtteranceLayer().getId();
         }
         if (sName.equalsIgnoreCase("speakers")
             || sName.equalsIgnoreCase("speaker")
             || sName.equalsIgnoreCase("turns")
             || sName.equalsIgnoreCase("turn"))
         {
            sName = getTurnLayer().getId();
         }
         Layer layer = getSchema().getLayer(sName);
         if (layer == null)
         { // no exact match
            // try a prefix-match - i.e. "transcript - John Smith" should map to the "transcript" layer
            // ignore spaces too
            String sNameNoWhitespace = sName.replaceAll("\\s","");
            for (Layer mappableLayer : vPossiblLayers)
            {
               if (sNameNoWhitespace.startsWith(mappableLayer.getId().replaceAll("\\s","")))
               {
                  layer = mappableLayer;
                  break;
               }
            } // next layer
         }
         if (layer != null)
         { // there is a matching layer
            if (tier instanceof IntervalTier)
            { // interval layer?
               if (layer.getAlignment() != 1) p.setValue(layer);
            }
            else // TextTier
            { // point layer?
               if (layer.getAlignment() == 1) p.setValue(layer);
            }
         }
         else if (tier instanceof IntervalTier)
         { // no name match, assume it's a tier named after a speaker
            if (sName.equalsIgnoreCase("word") || sName.equalsIgnoreCase("words")
                && getWordLayer() != null)
            {
               // make the wordLayer the default
               p.setValue(getWordLayer());
            }
            else
            {
               // make the utteranceLayer the default
               p.setValue(getUtteranceLayer());
            }
         }
         p.setPossibleValues(vPossiblLayers);
         mappings.addParameter(p);
      } // next tier

      if (timers != null) timers.end("load");
      return mappings;
   }

   /**
    * Sets parameters for a given deserialization operation, after loading the serialized form of the graph. This might include mappings from format-specific objects like tiers to graph layers, etc.
    * <p>{@link IDeserializer} method.
    * @param parameters The configuration for a given deserialization operation.
    * @throws SerializationParametersMissingException If not all required parameters have a value.
    */
   public void setParameters(ParameterSet parameters) throws SerializationParametersMissingException
   {
      mappings = parameters;
      
      // check we've got enough mappings for turns and words
      // i.e. at least two of turn/utterance/transcription
      int iTurnLayerMapped = 0;
      int iUtteranceLayerMapped = 0;
      int iWordLayerMapped = 0;
      for (int t = 0; t < getTiers().size(); t++)
      {
         Tier tier = getTiers().elementAt(t);
         // is there a mapping for this tier?
         Layer layer = (Layer)mappings.get("tier"+t).getValue();
         if (layer != null)
         {
            if (layer.equals(getTurnLayer()))
            {
               iTurnLayerMapped++;
            }
            else if (layer.equals(getUtteranceLayer()))
            {
               iUtteranceLayerMapped++;
            }
            else if (layer.equals(getWordLayer()))
            {
               iWordLayerMapped++;
            }
         }
      }
      if (iTurnLayerMapped + iUtteranceLayerMapped + iWordLayerMapped == 0)
      {
         throw new SerializationParametersMissingException(
            "There are no turn, utterance, or word mappings");
      }
   }

   /**
    * Deserializes the serialized data, generating one or more {@link Graph}s.
    * <p>Many data formats will only yield one graph (e.g. Transcriber
    * transcript or Praat textgrid), however there are formats that
    * are capable of storing multiple transcripts in the same file
    * (e.g. AGTK, Transana XML export), which is why this method
    * returns a list.
    * <p>{@link IDeserializer} method.
    * @return A list of valid (if incomplete) {@link Graph}s. 
    * @throws SerializerNotConfiguredException if the object has not been configured.
    * @throws SerializationParametersMissingException if the parameters for this particular graph have not been set.
    * @throws SerializationException if errors occur during deserialization.
    */
   public Graph[] deserialize() 
      throws SerializerNotConfiguredException, SerializationParametersMissingException, SerializationException
   {
      if (timers != null) timers.start("deserialize");
      if (participantLayer == null) throw new SerializerNotConfiguredException("Participant layer not set");
      if (turnLayer == null) throw new SerializerNotConfiguredException("Turn layer not set");
      if (utteranceLayer == null) throw new SerializerNotConfiguredException("Utterance layer not set");
      if (wordLayer == null) throw new SerializerNotConfiguredException("Word layer not set");
      if (schema == null) throw new SerializerNotConfiguredException("Layer schema not set");

      // if there are errors, accumlate as many as we can before throwing SerializationException
      SerializationException errors = null;

      warnings = new Vector<String>();

      Graph graph = new Graph();
      graph.setId(getId());
      // creat the 0 anchor to prevent graph tagging from creating one with no confidence
      Anchor graphStart = graph.getOrCreateAnchorAt(0.0, Constants.CONFIDENCE_MANUAL);

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

      graph.setOffsetUnits(Constants.UNIT_SECONDS);
      graph.setOffsetGranularity(Constants.GRANULARITY_MILLISECONDS);

      boolean turnLayerMapped = false;
      boolean utteranceLayerMapped = false;
      boolean wordLayerMapped = false;
      double highestXmax = 0.0;
      for (int t = 0; t < getTiers().size(); t++)
      {
         Tier tier = getTiers().elementAt(t);
         highestXmax = Math.max(highestXmax, tier.getXmax());
         // is there a mapping for this tier?
         Layer layer = (Layer)mappings.get("tier"+t).getValue();
         if (layer != null && !layer.getId().equals("[ignore tier]"))
         {
            if (layer.getId().equals(getTurnLayer().getId()))
            {
               turnLayerMapped = true;
            }
            else if (layer.getId().equals(getUtteranceLayer().getId()))
            {
               utteranceLayerMapped = true;
            }
            else if (layer.getId().equals(getWordLayer().getId()))
            {
               wordLayerMapped = true;
            }
	    
            if (graph.getLayer(layer.getId()) == null)
            {
               graph.addLayer((Layer)layer.clone());
            }

         } // tier is mapped to a layer
      } // next tier

      if (!wordLayerMapped) 
      {
         // add convention layers, as we'll be breaking utterances into tokens
         if (lexicalLayer != null) graph.addLayer((Layer)lexicalLayer.clone());
         if (pronounceLayer != null) graph.addLayer((Layer)pronounceLayer.clone());
         if (commentLayer != null) graph.addLayer((Layer)commentLayer.clone());
         if (noiseLayer != null) graph.addLayer((Layer)noiseLayer.clone());	 
      }

      int iLastWordOrdinal = 0;
      
      // turn tiers of annotations into layers of annotations
      for (int t = 0; t < getTiers().size(); t++)
      {
         Tier tier = getTiers().elementAt(t);
         // is there a mapping for this tier?
         Layer layer = (Layer)mappings.get("tier"+t).getValue();
         if (layer != null && !layer.getId().equals("[ignore tier]"))
         {
            if (timers != null) timers.start("tier " + tier.getName());
            // what type of tier is it?
            if (tier instanceof IntervalTier)
            {
               if (layer.getAlignment() == Constants.ALIGNMENT_INSTANT)
               {
                  throw new SerializationException(
                     "Tier \"" + tier.getName() 
                     + "\" is an IntervalTier but layer " + layer.getId() + " is for points.");
               }
               IntervalTier intervals = (IntervalTier) tier;
	       
               ListIterator<Interval> itIntervals = intervals.getIntervals().listIterator();
               while (itIntervals.hasNext())
               {
                  Interval interval = itIntervals.next();
                  // ignore empty intervals...
                  if (interval.getText() == null || interval.getText().trim().length() == 0) continue;
		  
                  if (timers != null) timers.start("getOrCreateAnchorAt " + tier.getName());
                  Anchor start = graph.getOrCreateAnchorAt(
                     interval.getXmin(), Constants.CONFIDENCE_MANUAL);
                  Anchor end = graph.getOrCreateAnchorAt(
                     interval.getXmax(), Constants.CONFIDENCE_MANUAL);
                  if (timers != null) timers.end("getOrCreateAnchorAt " + tier.getName());
                  Annotation annotation = new Annotation(
                     null, interval.getText(), layer.getId(), start.getId(), end.getId());
                  annotation.put("@tier", tier);
                  annotation.setConfidence(Constants.CONFIDENCE_MANUAL);
                  graph.addAnnotation(annotation);
               } // next interval
            } // interval tier 
            else if (tier instanceof TextTier)
            {
               if (layer.getAlignment() != Constants.ALIGNMENT_INSTANT)
               {
                  throw new SerializationException(
                     "Tier \"" + tier.getName() 
                     + "\" is a TextTier but layer " + layer.getId() + " is not for points.");
               }
               TextTier points = (TextTier) tier;
               for (Point point : points.getPoints())
               {			
                  // ignore empty points
                  if (point.getMark() == null || point.getMark().trim().length() == 0) continue;

                  Anchor anchor = graph.getOrCreateAnchorAt(
                     point.getTime(), Constants.CONFIDENCE_MANUAL);
                  Annotation annotation = new Annotation(
                     null, point.getMark(), layer.getId(), anchor.getId(), anchor.getId());
                  annotation.put("@tier", tier);
                  annotation.setConfidence(Constants.CONFIDENCE_MANUAL);
                  graph.addAnnotation(annotation);
               } // next point
            } // point tier
            else
            {
               throw new SerializationException(
                  "Tier \"" + tier.getName() + "\" - Unknown tier type: " 
                  + tier.getClass().getName());
            }
            if (timers != null) timers.end("tier " + tier.getName());
         } // layer is mapped
      } // next tier

      Anchor graphEnd = graph.getOrCreateAnchorAt(highestXmax, Constants.CONFIDENCE_MANUAL);

      // ensure both turns and utterances exist, and parents are set
      // if (!turnLayerMapped && !utteranceLayerMapped && wordLayerMapped) TODO construct utterances
      if (wordLayerMapped && !turnLayerMapped && !utteranceLayerMapped)
      { // create utterances and turns from words	 
         // given there are no utterance/turn intervals, 
         // we assume that the tier name for words is the speaker name	 
         if (timers != null) timers.start("create turns/utterances");
         HashMap<String,Annotation> turnsByName = new HashMap<String,Annotation>();
         for (Annotation word : graph.list(wordLayer.getId()))
         {
            String participantName = ((Tier)word.get("@tier")).getName();
            // if the tier name is something like "transcript - foo"...
            if (participantName.startsWith(word.getLayerId()))
            { // ... strip off the prefix
               participantName = participantName
                  // strip off layer ID
                  .substring(word.getLayerId().length())
                  // trim spaces, dashes, etc.
                  .replaceAll("^\\W+","");
            }
            Annotation turn = turnsByName.get(participantName);
            if (turn == null)
            {
               // create turn 
               turn = new Annotation(
                  null, participantName, turnLayer.getId(), graphStart.getId(), graphEnd.getId());
               graph.addAnnotation(turn);
               turnsByName.put(participantName, turn);

               // create utterance
               Annotation utterance = new Annotation(turn);
               utterance.setLayerId(utteranceLayer.getId());
               utterance.setParentId(turn.getId());
               graph.addAnnotation(utterance);
            }
            // set parent of word
            word.setParent(turn);
         } // next turn
         if (timers != null) timers.end("create turns/utterances");
      } 
      else if (turnLayerMapped && !utteranceLayerMapped)
      { // create utterances from turns
         if (timers != null) timers.start("create utterances from turns");
         for (Annotation turn : graph.list(turnLayer.getId()))
         {
            Annotation utterance = new Annotation(turn);
            utterance.setLayerId(utteranceLayer.getId());
            utterance.setParentId(turn.getId());
            if (!wordLayerMapped) // no word layer
            { // ...which means the label must be the untokenized words
               // and so the turn's tier name must be the speaker name
               turn.setLabel(((Tier)turn.get("@tier")).getName());
            }
            graph.addAnnotation(utterance);
         } // next turn
         if (timers != null) timers.end("create utterances from turns");
      }
      else if (utteranceLayerMapped && !turnLayerMapped)
      { // create turns from utterances
         if (timers != null) timers.start("create turns from utterances");
         for (Annotation utterance : graph.list(utteranceLayer.getId()))
         {
            Annotation turn = new Annotation(utterance);
            turn.setLayerId(turnLayer.getId());
            // the utterance's tier name taken to be the speaker name
            turn.setLabel(((Tier)utterance.get("@tier")).getName());
            graph.addAnnotation(turn);
            // now turn will have an ID, and we can set it to be the parent of utterance
            utterance.setParent(turn);
         } // next utterance
         if (timers != null) timers.end("create turns from utterances");
      }
      else if (utteranceLayerMapped && turnLayerMapped)
      {
         // ensure utterance parent turns are set
         if (timers != null) timers.start("set utterance turns");
         for (Annotation utterance : graph.list(utteranceLayer.getId()))
         {
            Annotation[] possibleTurns = utterance.includingAnnotationsOn(turnLayer.getId());
            if (possibleTurns.length == 1)
            { // must be this one
               utterance.setParent(possibleTurns[0]);
            }
            else if (possibleTurns.length > 1)
            { // multiple possible turns
               // use the turn whose label is included in the utterance's tier name
               // e.g. the turn might be "John Smith" and the utterance tier might be "utterance - John Smith"
               Tier utteranceTier = (Tier)utterance.get("@tier");
               assert utteranceTier != null : "utteranceTier != null - " + utterance;
               Annotation turn = null;
               for (Annotation possibleTurn : possibleTurns)
               {
                  // is the label (the speaker) a part of the utterance's tier name?
                  if (utteranceTier.getName().indexOf(possibleTurn.getLabel()) >= 0)
                  {
                     // multiple parents could match 
                     // e.g. "sp1" and "Interviewer sp1" both are suffixes of "utterance - Interviewer sp1"
                     // so we go with the longest one
                     if (turn == null
                         || possibleTurn.getLabel().length() > turn.getLabel().length())
                     {
                        turn = possibleTurn;
                     } // longest match so far
                  } // label is a part of the tier name
               } // next possible turn
               if (turn != null) utterance.setParent(turn);
            } // multiple possible turns
         } // next utterance
         if (timers != null) timers.end("set utterance turns");
      } // both utterance and turn layers mapped

      // now we have turns with participant name labels, 
      // and utterances with parents, that maybe need tokenizing

      // ensure participants are set
      HashMap<String,Annotation> participantsByName = new HashMap<String,Annotation>();
      if (timers != null) timers.start("set turn participants");
      int ordinal = 1;
      for (Annotation turn : graph.list(turnLayer.getId()))
      {
         if (!participantsByName.containsKey(turn.getLabel()))
         { // create participant
            Annotation who = new Annotation(null, turn.getLabel(), participantLayer.getId());
            graph.addAnnotation(who);
            participantsByName.put(turn.getLabel(), who);
         }
         if (timers != null) timers.start("set turn parent");
         // set ordinal first, then don't allow appending in setParent, to save a performance hit
         turn.setOrdinal(participantsByName.get(turn.getLabel()).getAnnotations(turnLayer.getId()).size() + 1);
         turn.setParent(participantsByName.get(turn.getLabel()), false);
         if (timers != null) timers.end("set turn parent"); 
      } // next turn
      if (timers != null) timers.end("set turn participants");

      // have to track changes to be able to mark things for destrucion...
      graph.trackChanges(); 

      // join subsequent turns by the same speaker...
      // for each participant (assumed to be parent of turn)
      if (timers != null) timers.start("join subsequent turns");
      for (Annotation participant : graph.list(participantLayer.getId()))
      {
         TreeSet<Annotation> annotations = new TreeSet<Annotation>(new AnnotationComparatorByAnchor());
         annotations.addAll(participant.getAnnotations(turnLayer.getId()));
         Annotation[] turns = annotations.toArray(new Annotation[0]);
         // go back through all the turns, looking for a turn for the same speaker that is
         // joined to, or overlaps, this one	 
         for (int i = turns.length - 2; i >= 0; i--)
         {
            Annotation preceding = turns[i];
            Annotation following = turns[i + 1];
            if (preceding.getEnd().getOffset() != null && following.getStart().getOffset() != null
                && preceding.getEnd().getOffset() >= following.getStart().getOffset())
            {
               mergeTurns(preceding, following);
               following.destroy();
            }
         } // next preceding turn
      } // next turn parent
      graph.commit();
      if (timers != null) timers.end("join subsequent turns");
      
      // now we have participants,
      // and rationalized turns with participant name labels and parents, 
      // and utterances with parents, that maybe need tokenizing

      if (!wordLayerMapped)
      { // tokenize utterances and apply conventions
         if (timers != null) timers.start("tokenize");
         // ensure we have an utterance tokenizer
         if (getTokenizer() == null)
         {
            setTokenizer(new SimpleTokenizer(getUtteranceLayer().getId(), getWordLayer().getId()));
         }
         try
         {
            tokenizer.transform(graph);
         }
         catch(TransformationException exception)
         {
            if (errors == null) errors = new SerializationException();
            if (errors.getCause() == null) errors.initCause(exception);
            errors.addError(SerializationException.ErrorType.Tokenization, exception.getMessage());
         }
         graph.commit();
         if (timers != null) timers.end("tokenize");
         if (getUseConventions())
         {
            if (timers != null) timers.start("conventions");
            try
            {
               // word {comment comment} word
               SpanningConventionTransformer commentTransformer = new SpanningConventionTransformer(
                  getWordLayer().getId(), "\\{(.*)", "(.*)\\}", true, null, null, 
                  commentLayer==null?null:commentLayer.getId(), "$1", "$1", false, false);
               commentTransformer.transform(graph);
               graph.commit();
	       
               // word [noise noise] word
               SpanningConventionTransformer noiseTransformer = new SpanningConventionTransformer(
                  getWordLayer().getId(), "\\[(.*)", "(.*)\\]", true, null, null, 
                  noiseLayer==null?null:noiseLayer.getId(), "$1", "$1", false, false);
               noiseTransformer.transform(graph);
               graph.commit();
	       
               // word[pronounce]
               ConventionTransformer pronounceTransformer = new ConventionTransformer(
                  getWordLayer().getId(), "(.+)\\[(.*)\\](\\p{Punct}*)", "$1$3", 
                  pronounceLayer==null?null:pronounceLayer.getId(), "$2");
               pronounceTransformer.transform(graph);
               graph.commit();
	       
               // word(lexical)
               ConventionTransformer lexicalTransformer = new ConventionTransformer(
                  getWordLayer().getId(), "(.+)\\((.*)\\)(\\p{Punct}*)", "$1$3", 
                  lexicalLayer==null?null:lexicalLayer.getId(), "$2");
               lexicalTransformer.transform(graph);
               graph.commit();

               // run word[pronounce] again, in case some were masked by lexical tags:
               // word[pronounce](lexical)
               pronounceTransformer.transform(graph);
               graph.commit();

               // clump non-orthographic 'words' with real words
               OrthographyClumper clumper = new OrthographyClumper(wordLayer.getId());	  
               clumper.transform(graph);
               graph.commit();
            }
            catch(TransformationException exception)
            {
               if (errors == null) errors = new SerializationException();
               if (errors.getCause() == null) errors.initCause(exception);
               errors.addError(SerializationException.ErrorType.Tokenization, exception.getMessage());
            }
            if (timers != null) timers.end("conventions");
         } // apply transcription conventions
      } // tokenize utterances
      else
      { // word layer mapped
         if (timers != null) timers.start("set word turns");
         // ensure word parent turns are set
         for (Annotation word : graph.list(wordLayer.getId()))
         {
            if (word.getParent() == null)
            {
               Annotation[] possibleTurns = word.includingAnnotationsOn(turnLayer.getId());
               if (possibleTurns.length == 1)
               { // must be this one
                  word.setParent(possibleTurns[0]);
               }
               else if (possibleTurns.length > 1)
               { // multiple possible turns
                  // use the turn whose label is included in the utterance's tier name
                  // e.g. the turn might be "John Smith" and the utterance tier might be "utterance - John Smith"
                  Tier wordTier = (Tier)word.get("@tier");
                  assert wordTier != null : "wordTier != null - " + word;
                  Annotation turn = null;
                  for (Annotation possibleTurn : possibleTurns)
                  {
                     // is the label (the speaker) a part of the utterance's tier name?
                     if (wordTier.getName().indexOf(possibleTurn.getLabel()) >= 0)
                     {
                        // multiple parents could match 
                        // e.g. "sp1" and "Interviewer sp1" both are suffixes of "word - Interviewer sp1"
                        // so we go with the longest one
                        if (turn == null
                            || possibleTurn.getLabel().length() > turn.getLabel().length())
                        {
                           turn = possibleTurn;
                        } // longest match so far
                     } // label is a part of the tier name
                  } // next possible turn
                  if (turn != null) word.setParent(turn);
               } // multiple possible turns
            } // parent not set
            if (word.getParent() == null)
            { // parent still not set
               // maybe children don't quite line up with parents, so use midpoint-including instead
               Annotation[] possibleTurns = word.midpointIncludingAnnotationsOn(turnLayer.getId());
               if (possibleTurns.length == 1)
               { // must be this one
                  word.setParent(possibleTurns[0]);
               }
               else if (possibleTurns.length > 1)
               { // multiple possible turns
                  // use the turn whose label is included in the utterance's tier name
                  // e.g. the turn might be "John Smith" and the utterance tier might be "utterance - John Smith"
                  Tier wordTier = (Tier)word.get("@tier");
                  assert wordTier != null : "wordTier != null - " + word;
                  Annotation turn = null;
                  for (Annotation possibleTurn : possibleTurns)
                  {
                     // is the label (the speaker) a part of the utterance's tier name?
                     if (wordTier.getName().indexOf(possibleTurn.getLabel()) >= 0)
                     {
                        // multiple parents could match 
                        // e.g. "sp1" and "Interviewer sp1" both are suffixes of "word - Interviewer sp1"
                        // so we go with the longest one
                        if (turn == null
                            || possibleTurn.getLabel().length() > turn.getLabel().length())
                        {
                           turn = possibleTurn;
                        } // longest match so far
                     } // label is a part of the tier name
                  } // next possible turn
                  if (turn != null) word.setParent(turn);
               } // multiple possible turns
            } // parent still not set
         } // next utterance
         if (timers != null) timers.end("set word turns");
      } // word layer mapped

      // now we have participants,
      // and turns with participant name labels and parents, 
      // and utterances with parents
      // and words with parents

      // need to ensure that other required parents are set
      if (timers != null) timers.start("set parents");
      for (Annotation a : graph.getAnnotationsById().values())
      {
         if (a.getParentId() == null)
         {
            if (timers != null) timers.start("includingAnnotationsOn");
            Annotation[] possibleParents = a.includingAnnotationsOn(a.getLayer().getParentId());
            if (timers != null) timers.end("includingAnnotationsOn");
            if (possibleParents.length == 1)
            { // must be this one
               a.setParent(possibleParents[0]);
            }
            else if (possibleParents.length > 1)
            { // multiple possible parents
               // use the turn whose label is included in the utterance's tier name
               // e.g. the turn might be "John Smith" and the utterance tier might be "utterance - John Smith"
               Tier tier = (Tier)a.get("@tier");
               assert tier != null : "tier != null - " + a;
               Annotation parent = null;
               Annotation parentWho = null;
               for (Annotation possibleParent : possibleParents)
               {
                  // is the label (the speaker) a part of the utterance's tier name?
                  Annotation who = possibleParent.my("who");
                  if (who != null)
                  {
                     if (tier.getName().indexOf(who.getLabel()) >= 0)
                     {
                        // multiple parents could match 
                        // e.g. "sp1" and "Interviewer sp1" both are suffixes of "segment - Interviewer sp1"
                        // so we go with the longest one
                        if (parent == null
                            || who.getLabel().length() > parentWho.getLabel().length())
                        {
                           parent = possibleParent;
                           parentWho = who;
                        } // longest match so far
                     } // label is a part of the tier name
                  } // there is a speaker
               } // next possible parent
               if (parent != null) a.setParent(parent);
            } // multiple possible parents
         } // parent not set

         if (a.getParentId() == null)
         { // still not set
            // maybe children don't quite line up with parents, so use midpoint-including instead
            if (timers != null) timers.start("midpointIncludingAnnotationsOn");
            Annotation[] possibleParents = a.midpointIncludingAnnotationsOn(a.getLayer().getParentId());
            if (timers != null) timers.end("midpointIncludingAnnotationsOn");
            if (possibleParents.length == 1)
            { // must be this one
               a.setParent(possibleParents[0]);
            }
            else if (possibleParents.length > 1)
            { // multiple possible parents
               // use the turn whose label is included in the utterance's tier name
               // e.g. the turn might be "John Smith" and the utterance tier might be "utterance - John Smith"
               Tier tier = (Tier)a.get("@tier");
               assert tier != null : "tier != null - " + a;
               Annotation parent = null;
               for (Annotation possibleParent : possibleParents)
               {
                  // is the label (the speaker) a part of the utterance's tier name?
                  Annotation who = possibleParent.my("who");
                  if (who != null)
                  {
                     if (tier.getName().indexOf(who.getLabel()) >= 0)
                     {
                        // multiple parents could match 
                        // e.g. "sp1" and "Interviewer sp1" both are suffixes of "segment - Interviewer sp1"
                        // so we go with the longest one
                        if (parent == null
                            || possibleParent.getLabel().length() > parent.getLabel().length())
                        {
                           parent = possibleParent;
                        } // longest match so far
                     } // label is a part of the tier name
                  } // there is a speaker
               } // next possible parent
               if (parent != null) a.setParent(parent);
            } // multiple possible parents
         } // parent still not set

         if (a.getParentId() == null)
         { // parent STILL not set
            warnings.add("No parent for " + a.getLayerId() + ":" + a.getLabel()
                         + " ("+a.getStart()+"-"+a.getEnd()+")");
         } // parent STILL not set
      } // next annotation
      if (timers != null) timers.end("set parents");

      // ensure anchors are shared between children and parents where required
      if (timers != null) timers.start("anchor sharing");
      Vector<Layer> layers = graph.getLayersTopDown();
      // (bottom up to propagate changes from below)
      Collections.reverse(layers);
      for (Layer l : layers)
      {
         if (l.getSaturated() && l.getAlignment() == Constants.ALIGNMENT_INTERVAL)
         {
            for (Annotation parent : graph.list(l.getParentId()))
            {
               SortedSet<Annotation> children = parent.getAnnotations(l.getId());
               if (children.size() > 0)
               {
                  if (children.first().getStart() != null)
                     parent.setStart(children.first().getStart());
                  if (children.last().getEnd() != null)
                     parent.setEnd(children.last().getEnd());
               }
            } // next parent
         }
      }
      graph.commit();
      if (timers != null) timers.end("anchor sharing");

      // remove references to tiers
      if (timers != null) timers.start("remove tier references");
      for (Annotation a : graph.getAnnotationsById().values()) a.remove("@tier");
      if (timers != null) timers.end("remove tier references");

      // set end anchors of graph tags
      for (Annotation a : graph.list(getParticipantLayer().getId()))
      {
         a.setStartId(graphStart.getId());
         a.setEndId(graphEnd.getId());
      }

      if (errors != null) throw errors;

      // reset all change tracking
      if (graph.getTracker() != null)
      {
         graph.getTracker().reset();
         graph.setTracker(null);
      }

      Graph[] graphs = { graph };
      if (timers != null) timers.end("deserialize");
      return graphs;
   }

   /**
    * Moves all of the children of the following turn into the preceding turn, set the the end of the preceding to the end of the following, and marks the following for deletion.
    * @param preceding The preceding, surviving turn.
    * @param following The following turn, which will be deleted.
    * @return The changes for this merge.
    */
   public void mergeTurns(Annotation preceding, Annotation following)
   {
      // set anchor
      if (preceding.getEnd().getOffset() == null
          || following.getEnd().getOffset() == null
          || preceding.getEnd().getOffset() < following.getEnd().getOffset()) 
      {
         preceding.setEnd(following.getEnd());
      }

      // for each child layer
      for (String childLayerId : following.getAnnotations().keySet())
      {
         // move everything from following to preceding
         int ordinal = 1;
         if (preceding.getAnnotations().containsKey(childLayerId))
         {
            ordinal = preceding.getAnnotations().get(childLayerId).size() + 1;
         }
         for (Annotation child : following.annotations(childLayerId))
         {
            child.setParent(preceding);
            child.setOrdinal(ordinal++);
         } // next child annotation
      } // next child layer
   } // end of joinTurns()

   /**
    * Determines which layers, if any, must be present in the graph that will be serialized.
    * <p>{@link ISerializer} method.
    * @return A list of IDs of layers that must be present in the graph that will be serialized.
    * @throws SerializationParametersMissingException If not all required parameters have a value.
    */
   public String[] getRequiredLayers() throws SerializationParametersMissingException
   {
      if (bUseConventions != null && bUseConventions)
      {
         Vector<String> requiredLayers = new Vector<String>();
         if (getParticipantLayer() != null) requiredLayers.add(getParticipantLayer().getId());
         if (getTurnLayer() != null) requiredLayers.add(getTurnLayer().getId());
         if (getUtteranceLayer() != null) requiredLayers.add(getUtteranceLayer().getId());
         if (getWordLayer() != null) requiredLayers.add(getWordLayer().getId());
         if (getLexicalLayer() != null) requiredLayers.add(getLexicalLayer().getId());
         if (getPronounceLayer() != null) requiredLayers.add(getPronounceLayer().getId());
         if (getCommentLayer() != null) requiredLayers.add(getCommentLayer().getId());
         if (getNoiseLayer() != null) requiredLayers.add(getNoiseLayer().getId());
         return requiredLayers.toArray(new String[0]);
      }
      else
      {
         return new String[0];
      }
   }

   /**
    * Determines the cardinality between graphs and serialized streams.
    * @return {@link nzilbb.ag.serialize.ISerializer#Cardinality}.NtoN as there is one
    * stream produced for each graph to serialize.
    */
   public Cardinality getCardinality()
   {
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
    * @param consumer The object receiving the streams.
    * @param warnings The object receiving warning messages.
    * @return A list of named streams that contain the serialization in the given format. 
    * @throws SerializerNotConfiguredException if the object has not been configured.
    */
   public void serialize(Spliterator<Graph> graphs, String[] layerIds, Consumer<NamedStream> consumer, Consumer<String> warnings, Consumer<SerializationException> errors) 
      throws SerializerNotConfiguredException
   {
      graphCount = graphs.getExactSizeIfKnown();
      graphs.forEachRemaining(graph -> {
            if (getCancelling()) return;
            try
            {
               consumer.accept(serializeGraph(graph, layerIds));
            }
            catch(SerializationException exception)
            {
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
      throws SerializationException
   {
      SerializationException errors = null;

      HashSet<String> selectedLayers = new HashSet<String>();
      if (layerIds != null)
      {
         for (String l : layerIds) selectedLayers.add(l);
      }
      else
      {
         for (Layer l : graph.getSchema().getLayers().values()) selectedLayers.add(l.getId());
      }

      graph.setOffsetGranularity(Constants.GRANULARITY_MILLISECONDS);

      reset();
      // for each layer, top-down
      String participantLayerId = null;
      if (getParticipantLayer() != null) participantLayerId = getParticipantLayer().getId();
      for (Layer layer : graph.getLayersTopDown())
      {
         // skip "who" layer
         if (layer.equals(getParticipantLayer())) continue;
         // skip "who" tags layer
         if (layer.getParent().equals(getParticipantLayer())
             && layer.getAlignment() == Constants.ALIGNMENT_NONE) continue;
         // include if there's an utterance layer, skip the turn layer
         if (layer.equals(getTurnLayer()) && getUtteranceLayer() != null) continue;
         // skip layers that were not explicitly selected
         if (!selectedLayers.contains(layer.getId())) continue;
         boolean assignedToParticipant = layer.getAncestors().contains(getParticipantLayer());
         if (layer.getAlignment() == Constants.ALIGNMENT_INSTANT)
         { // layer of instants
            TreeMap<String,TextTier> tiers = new TreeMap<String,TextTier>();
            for (Annotation a : graph.list(layer.getId()))
            {
               if (a.getAnchored())
               {
                  // get tier for this participant
                  String who = "";
                  if (assignedToParticipant)
                  {
                     Annotation participant = a.my(participantLayerId);
                     if (participant != null) who = participant.getLabel();
                  }
                  if (!tiers.containsKey(who))
                  {
                     String tierName = layer.getId();
                     if (who.length() > 0) tierName += " - " + who;
                     tiers.put(who, new TextTier(tierName, 0, 0));
                  }
                  TextTier tier = tiers.get(who);

                  tier.addPoint(new Point(a.getLabel(), a.getStart().getOffset()));
                  tier.setXmax(Math.max(tier.getXmax(), a.getStart().getOffset()));
               }
            } // next annotation	    

            // ensure there's at least one tier for the layer
            if (tiers.size() == 0)
            {
               tiers.put("", new TextTier(layer.getId(), 0, 0));
            }

            // add tiers to the grid
            for (Tier tier : tiers.values())
            {
               addTier(tier);
            } // next tier
         } // layer of instants
         else
         { // layer of intervals
            TreeMap<String,Vector<IntervalTier>> tiers = new TreeMap<String,Vector<IntervalTier>>();
            for (Annotation a : graph.list(layer.getId()))
            {
               if (a.getAnchored())
               {
                  // get tier for this participant
                  String who = "";
                  if (assignedToParticipant)
                  {
                     Annotation participant = a.my(participantLayerId);
                     if (participant != null) who = participant.getLabel();
                  }
                  if (!tiers.containsKey(who))
                  {
                     String tierName = layer.getId();
                     if (who.length() > 0)
                     {
                        // for utterance tiers, just use the speaker name
                        if (layer.equals(getUtteranceLayer()))
                        {
                           tierName = who;
                        }
                        else
                        {
                           tierName += " - " + who;
                        }
                     }
                     Vector<IntervalTier> list = new Vector<IntervalTier>();
                     tiers.put(who, list);
                     list.add(new IntervalTier(tierName, 0, 0));
                  }
                  Vector<IntervalTier> tierList = tiers.get(who);
                  IntervalTier tier = tierList.firstElement();
                  double start = a.getStart().getOffset();
                  if (graph.compareOffsets(tier.getXmax(), start) > 0) // xmax > start
                  { // going backwards in time, so find/make a new tier
                     tier = null;
                     for (IntervalTier t : tierList)
                     {
                        if (graph.compareOffsets(t.getXmax(), start) <= 0) // xmax <= start
                        { // found a tier we can add to
                           tier = t;
                           break;
                        }
                     } // next tier in the list
                     if (tier == null)
                     { // add a new tier
                        String tierName = layer.getId();
                        if (who.length() > 0)
                        {
                           // for utterance tiers, just use the speaker name
                           if (layer.equals(getUtteranceLayer()))
                           {
                              tierName = who;
                           }
                           else
                           {
                              tierName += " - " + who;
                           }
                        }
                        tier = new IntervalTier(tierName, 0, 0);
                        tierList.add(tier);
                     } // add a new tier
                  }

                  // determine label
                  String label = a.getLabel();
                  if (layer.equals(getTurnLayer()) || layer.equals(getUtteranceLayer())
                      && getWordLayer() != null)
                  { // turn/utterances layers are filled by their word token labels
                     StringBuffer l = new StringBuffer();
                     for (Annotation token : a.list(getWordLayer().getId()))
                     {
                        if (l.length() > 0) l.append(" ");
                        l.append(token.getLabel()); // TODO transcript convention support
                     } // next token
                     label = l.toString();
                  } // turn/utterance layer

                  // double-check we don't need to adjust the start offset a smidgin
                  if (tier.getXmax() > start)
                  { // offsets are within offsetGranularity, but not equal
                     a.getStart().setOffset(tier.getXmax());
                  } // adjust offset

                  // add an interval to it
                  tier.addInterval(new Interval(label, a.getStart().getOffset(), a.getEnd().getOffset()));
                  tier.setXmax(Math.max(tier.getXmax(), a.getEnd().getOffset()));
               }
            } // next annotation

            // ensure there's at least one tier for the layer
            if (tiers.size() == 0)
            {
               Vector<IntervalTier> list = new Vector<IntervalTier>();
               list.add(new IntervalTier(layer.getId(), 0, 0));
               tiers.put("", list);
            }

            // add tiers to the grid
            for (Vector<IntervalTier> list : tiers.values())
            {
               for (Tier tier : list)
               {
                  addTier(tier);
               } // next tier
            } // next list of tiers
         } // layer of intervals
      } // next layer

      // ensure all tiers are correctly padded out to the end
      setXmax(graph.getSortedAnchors().last().getOffset());
      padIntervalTiersToXmax();

      if (errors != null) throw errors;

      try
      {
         // write the TextGrid to a temporary file
         File f = File.createTempFile(IO.SafeFileNameUrl(graph.getId()), ".TextGrid");
         FileOutputStream out = new FileOutputStream(f);	 
         PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "utf-8"));
         writeText(writer);
         writer.close();
         TempFileInputStream in = new TempFileInputStream(f);

         // return a named stream from the file
         String name = IO.SafeFileNameUrl(graph.getId());
         if (!name.toLowerCase().endsWith(".textgrid")) name += ".TextGrid";
         return new NamedStream(in, name);
      }
      catch(Exception exception)
      {
         errors = new SerializationException();
         errors.initCause(exception);
         errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
         throw errors;
      }      
   }

} // end of class TextGridSerialization
