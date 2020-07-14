//
// Copyright 2019-2020 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.latex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Spliterator;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.Vector;
import java.util.function.Consumer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
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
 * Serializer to LaTeX files.
 * @author Robert Fromont robert@fromont.net.nz
 */

public class LatexSerializer
   implements GraphSerializer
{
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
   public LatexSerializer setParticipantLayer(Layer newParticipantLayer) { participantLayer = newParticipantLayer; return this; }

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
   public LatexSerializer setTurnLayer(Layer newTurnLayer) { turnLayer = newTurnLayer; return this; }

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
   public LatexSerializer setUtteranceLayer(Layer newUtteranceLayer) { utteranceLayer = newUtteranceLayer; return this; }

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
   public LatexSerializer setWordLayer(Layer newWordLayer) { wordLayer = newWordLayer; return this; }

   /**
    * Layer for orthography.
    * @see #getOrthographyLayer()
    * @see #setOrthographyLayer(Layer)
    */
   protected Layer orthographyLayer;
   /**
    * Getter for {@link #orthographyLayer}: Layer for orthography.
    * @return Layer for orthography.
    */
   public Layer getOrthographyLayer() { return orthographyLayer; }
   /**
    * Setter for {@link #orthographyLayer}: Layer for orthography.
    * @param newOrthographyLayer Layer for orthography.
    */
   public LatexSerializer setOrthographyLayer(Layer newOrthographyLayer) { orthographyLayer = newOrthographyLayer; return this; }
   
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
   public LatexSerializer setNoiseLayer(Layer newNoiseLayer) { noiseLayer = newNoiseLayer; return this; }

   /**
    * Tex code to include in preamble.
    * @see #getTexPreamble()
    * @see #setTexPreamble(String)
    */
   protected String texPreamble = "";
   /**
    * Getter for {@link #texPreamble}: Tex code to include in preamble.
    * @return Tex code to include in preamble.
    */
   public String getTexPreamble() { return texPreamble; }
   /**
    * Setter for {@link #texPreamble}: Tex code to include in preamble.
    * @param newTexPreamble Tex code to include in preamble.
    */
   public LatexSerializer setTexPreamble(String newTexPreamble) { texPreamble = newTexPreamble; return this; }

   /**
    * Tex to insert before first turn.
    * @see #getTexBeginTranscript()
    * @see #setTexBeginTranscript(String)
    */
   protected String texBeginTranscript = "\\begin{description}";
   /**
    * Getter for {@link #texBeginTranscript}: Tex to insert before first turn.
    * @return Tex to insert before first turn.
    */
   public String getTexBeginTranscript() { return texBeginTranscript; }
   /**
    * Setter for {@link #texBeginTranscript}: Tex to insert before first turn.
    * @param newTexBeginTranscript Tex to insert before first turn.
    */
   public LatexSerializer setTexBeginTranscript(String newTexBeginTranscript) { texBeginTranscript = newTexBeginTranscript; return this; }

   /**
    * TeX command for formatting a speech turn.
    * @see #getTexTurnCommand()
    * @see #setTexTurnCommand(String)
    */
   protected String texTurnCommand = "\\item[#1:] #2";
   /**
    * Getter for {@link #texTurnCommand}: TeX command for formatting a speech turn.
    * @return TeX command for formatting a speech turn.
    */
   public String getTexTurnCommand() { return texTurnCommand; }
   /**
    * Setter for {@link #texTurnCommand}: TeX command for formatting a speech turn.
    * @param newTexTurnCommand TeX command for formatting a speech turn.
    */
   public LatexSerializer setTexTurnCommand(String newTexTurnCommand) { texTurnCommand = newTexTurnCommand; return this; }

   /**
    * TeX to insert after last turn.
    * @see #getTexEndTranscript()
    * @see #setTexEndTranscript(String)
    */
   protected String texEndTranscript = "\\end{description}";
   /**
    * Getter for {@link #texEndTranscript}: TeX to insert after last turn.
    * @return TeX to insert after last turn.
    */
   public String getTexEndTranscript() { return texEndTranscript; }
   /**
    * Setter for {@link #texEndTranscript}: TeX to insert after last turn.
    * @param newTexEndTranscript TeX to insert after last turn.
    */
   public LatexSerializer setTexEndTranscript(String newTexEndTranscript) { texEndTranscript = newTexEndTranscript; return this; }

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
   public LatexSerializer setCancelling(boolean newCancelling) { cancelling = newCancelling; return this; }
   /**
    * Cancel the serialization in course (if any).
    */
   public void cancel()
   {
      setCancelling(true);
   }

   // Methods:
   
   /**
    * Default constructor.
    */
   public LatexSerializer()
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
         "LaTeX Document", "0.1", "application/x-tex", ".tex", "20200710.1904",
         getClass().getResource("icon.png"));
   }

   /**
    * Sets parameters for deserializer as a whole.  This might include database connection
    * parameters, locations of supporting files, etc. 
    * <p>When the deserializer is installed, this method should be invoked with an empty parameter
    *  set, to discover what (if any) general configuration is required. If parameters are
    *  returned, and user interaction is possible, then the user may be presented with an
    *  interface for setting/confirming these parameters.  
    * @param configuration The configuration for the deserializer. 
    * @param schema The layer schema, definining layers and the way they interrelate.
    * @return A list of configuration parameters (still) must be set before {@link
    * GraphDeserializer#setParameters(ParameterSet)} can be invoked. If this is an empty list,
    * {@link GraphDeserializer#setParameters(ParameterSet)} can be invoked. If it's not an
    * empty list, this method must be invoked again with the returned parameters' values
    * set. 
    */
   public ParameterSet configure(ParameterSet configuration, Schema schema)
   {
      setTurnLayer(schema.getTurnLayer());
      setUtteranceLayer(schema.getUtteranceLayer());
      setWordLayer(schema.getWordLayer());
      setParticipantLayer(schema.getParticipantLayer());

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
               if (top.getChildren().size() > 0)
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

      // other layers...

      layerToPossibilities.put(
         new Parameter("noiseLayer", Layer.class, "Noise layer", "Background noises"), 
         Arrays.asList("noise","noises","background","backgroundnoise"));
      layerToCandidates.put("noiseLayer", topLevelLayers);

      layerToPossibilities.put(
         new Parameter("orthographyLayer", Layer.class, "Orthography layer", "Orthography"), 
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

      if (!configuration.containsKey("texPreamble"))
      {
         configuration.addParameter(
            new Parameter("texPreamble", String.class, 
                          "Preamble",
                          "TeX code to include in the preamble, e.g. \"\\usepackage{lineno, blindtext}\"", true));
      }
      if (configuration.get("texPreamble").getValue() == null)
      {
         configuration.get("texPreamble").setValue(texPreamble);
      }
      
      if (!configuration.containsKey("texBeginTranscript"))
      {
         configuration.addParameter(
            new Parameter("texBeginTranscript", String.class, 
                          "Begin Transcript",
                          "TeX to insert before the first turn, e.g. \"\\begin{description}\"", true));
      }
      if (configuration.get("texBeginTranscript").getValue() == null)
      {
         configuration.get("texBeginTranscript").setValue(texBeginTranscript);
      }
 
      if (!configuration.containsKey("texTurnCommand"))
      {
         configuration.addParameter(
            new Parameter("texTurnCommand", String.class, 
                          "Turn Command",
                          "TeX command for formatting a speech turn, e.g. \"\\item[#1:] #2\" - #1 = Speaker ID, #2 = Turn Text", true));
      }
      if (configuration.get("texTurnCommand").getValue() == null)
      {
         configuration.get("texTurnCommand").setValue(texTurnCommand);
      }
 
      if (!configuration.containsKey("texEndTranscript"))
      {
         configuration.addParameter(
            new Parameter("texEndTranscript", String.class, 
                          "End Transcript",
                          "TeX to insert after the last turn, e.g. \"\\end{description}\"", true));
      }
      if (configuration.get("texEndTranscript").getValue() == null)
      {
         configuration.get("texEndTranscript").setValue(texEndTranscript);
      }
 
      return configuration;
   }

   // GraphSerializer methods

   /**
    * Determines which layers, if any, must be present in the graph that will be serialized.
    * @return A list of IDs of layers that must be present in the graph that will be serialized.
    * @throws SerializationParametersMissingException If not all required parameters have a value.
    */
   public String[] getRequiredLayers() throws SerializationParametersMissingException
   {
      Vector<String> requiredLayers = new Vector<String>();
      if (getParticipantLayer() != null) requiredLayers.add(getParticipantLayer().getId());
      if (getTurnLayer() != null) requiredLayers.add(getTurnLayer().getId());
      if (getUtteranceLayer() != null) requiredLayers.add(getUtteranceLayer().getId());
      if (getWordLayer() != null) requiredLayers.add(getWordLayer().getId());
      return requiredLayers.toArray(new String[0]);
   } // getRequiredLayers()

   /**
    * Determines the cardinality between graphs and serialized streams.
    * <p>The cardinatlity of this deseerializer is NToN.
    * @return {@link nzilbb.ag.serialize.GraphSerializer#Cardinality}.NToN.
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
    * @param consumer The consumer receiving the streams.
    * @param warnings A consumer for (non-fatal) warning messages.
    * @param errors A consumer for (fatal) error messages.
    * @throws SerializerNotConfiguredException if the object has not been configured.
    */
   public void serialize(Spliterator<Graph> graphs, String[] layerIds, Consumer<NamedStream> consumer, Consumer<String> warnings, Consumer<SerializationException> errors) 
      throws SerializerNotConfiguredException
   {
      // TODO maybe serialize a list of graph fragments into a single document?
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
    * @return A named stream that contains the LaTeX file. 
    * @throws SerializationException if errors occur during deserialization.
    */
   protected NamedStream serializeGraph(Graph graph, String[] layerIds) 
      throws SerializationException
   {
      SerializationException errors = null;
      
      LinkedHashSet<String> selectedLayers = new LinkedHashSet<String>();
      LinkedHashSet<String> tagLayers = new LinkedHashSet<String>();
      if (layerIds != null)
      {
         for (String l : layerIds)
         {
            Layer layer = graph.getSchema().getLayer(l);
            if (layer != null)
            {
               selectedLayers.add(l);
               if (layer.getParentId().equals(getWordLayer().getId()))
               {
                  tagLayers.add(l);
               }
            }
         } // next layeyId
      }
      else
      {
         for (Layer l : graph.getSchema().getLayers().values()) selectedLayers.add(l.getId());
      }

      try
      {
         // write the text to a temporary file
         File f = File.createTempFile(graph.getId(), ".tex");
         PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f), "utf-8"));

         Schema schema = graph.getSchema();

         writer.println("\\documentclass{article}");
         writer.println("\\usepackage[utf8]{inputenc}");
         writer.println(getTexPreamble() + "");
         writer.println("\\pagestyle{plain}");
         writer.println("\\setlength{\\parindent}{0pt}");
         writer.println("\\setlength{\\parskip}{1ex plus 0.5ex minus 0.2ex}");
         writer.println("% command for speaker turns...");
         writer.println("% copy this into your preamble if you copy parts of the transcript into another document:");
         writer.println("\\newcommand{\\turn}[2]{");
         writer.println(getTexTurnCommand());
         writer.println("}");
         writer.println("\\begin{document}");
         writer.println("\\begin{center}\\textbf{" + TexSafeText(graph.getId()) + "}\\end{center}");      
         if (tagLayers.size() > 1)
         {
            writer.println();
            writer.println("\\textbf{LAYERS:}");
            writer.println();
            String firstLayerId = orthographyLayer != null? orthographyLayer.getId():wordLayer.getId();
            writer.write(firstLayerId);
            for (String layerId : tagLayers)
            {
               // skip orthography layer
               if (layerId.equals(firstLayerId)) continue;
               
               writer.write("\\_"); // use _ as a delimiter between layers
               writer.write(layerId);
            } // next layer
            writer.println();
            writer.println();
            writer.println("\\textbf{TRANSCRIPT:}");
         } // multiple layers selected
         
         writer.println();
         writer.println(getTexBeginTranscript());

         // for each utterance...
         Annotation currentParticipant = null;
         StringBuffer turnText = new StringBuffer();
         boolean firstUtterance = true;
         
         // order utterances by anchor so that simultaneous speech comes out in utterance order
         TreeSet<Annotation> utterancesByAnchor
            = new TreeSet<Annotation>(new AnnotationComparatorByAnchor());
         for (Annotation u : graph.all(utteranceLayer.getId())) utterancesByAnchor.add(u);
         TreeSet<Annotation> noisesByAnchor
            = new TreeSet<Annotation>(new AnnotationComparatorByAnchor());
         if (noiseLayer != null)
         {
            // list all anchored noises
            for (Annotation n : graph.all(noiseLayer.getId())) if (n.getAnchored()) noisesByAnchor.add(n);
         }
         Iterator<Annotation> noises = noisesByAnchor.iterator();
         Annotation nextNoise = noises.hasNext()?noises.next():null;
         
         for (Annotation utterance : utterancesByAnchor)
         {
            if (cancelling) break;
            // is the participant changing?
            Annotation participant = utterance.first(getParticipantLayer().getId());
            if (participant != currentParticipant)
            { // participant change
               if (currentParticipant != null)
               {
                  writer.println();
                  writer.println("\\turn{"+TexSafeText(currentParticipant.getLabel())+"}{"+turnText+"}");
               }
               currentParticipant = participant;
	       turnText = new StringBuffer();
               firstUtterance = true;
            } // participant change
	    if (!firstUtterance) turnText.append("\\\\\n");

	    boolean firstWord = true;
            for (Annotation token : utterance.all(getWordLayer().getId()))
            {
	       // space precedes all but the first word
	       if (firstWord) { firstWord = false; } else { turnText.append(" "); }

               // is there a noise to insert?
               while (nextNoise != null
                      && token.getStart() != null
                      && token.getStart().getOffset() != null
                      && token.getStart().getOffset() >= nextNoise.getStart().getOffset())
               {
                  turnText.append("\\textit{[" + TexSafeText(nextNoise.getLabel()) + "]} ");
                  nextNoise = noises.hasNext()?noises.next():null;
               } // next noise
               
               Annotation orthography = token;
               if (orthographyLayer != null && selectedLayers.contains(orthographyLayer.getId()))
               {
                  orthography = token.first(orthographyLayer.getId());
                  if (orthography == null) orthography = token;
               }
               turnText.append(orthography.getLabel());
               // add tags
               for (String layerId : tagLayers)
               {
                  if (!layerId.equals(token.getLayerId())
                      && (orthographyLayer == null || !layerId.equals(orthographyLayer.getId())))
                  {
                     turnText.append("\\_");
                     Annotation tag = token.first(layerId);
                     if (tag != null)
                     {
                        turnText.append(tag.getLabel());
                     }
                  }
               } // next selected layer 
            } // next token
	    if (firstUtterance) firstUtterance = false;

            // is there a noise to append to the end of the line?
            while (nextNoise != null
                   && utterance.getEnd() != null
                   && utterance.getEnd().getOffset() != null
                   && utterance.getEnd().getOffset() >= nextNoise.getStart().getOffset())
            {
                  turnText.append("\\textit{[" + TexSafeText(nextNoise.getLabel()) + "]} ");
                  nextNoise = noises.hasNext()?noises.next():null;
            } // next noise
         } // next utterance
         if (currentParticipant != null)
         {
            writer.println();
            writer.println("\\turn{"+TexSafeText(currentParticipant.getLabel())+"}{"+turnText+"}\n");
         }
         writer.println();
         writer.println(getTexEndTranscript());
         writer.println();
         writer.println("\\end{document}");      
         writer.close();

         TempFileInputStream in = new TempFileInputStream(f);
         
         // return a named stream from the file
         return new NamedStream(in, IO.SafeFileNameUrl(graph.getId()) + ".tex");
      }
      catch(Exception exception)
      {
         errors = new SerializationException();
         errors.initCause(exception);
         errors.addError(SerializationException.ErrorType.Other, exception.getMessage());
         throw errors;
      }      
   }
   
   /**
    * Escapes the given string to ensure that TeX special characters are correctly displayed.
    * @param s
    * @return The given string with special characters escaped for TeX.
    */
   public static String TexSafeText(String s)
   {
      return s.replace("\\","\\textbackslash ")
	 .replace("#","\\# ")
	 .replace("$","\\\\$ ")
	 .replace("%","\\% ")
	 .replace("{","\\{ ")
	 .replace("}","\\} ")
	 .replace("^","\\^{} ")
	 .replace("&","\\& ")
	 .replace("_","\\_")
	 .replace("<","\\textless ")
	 .replace(">","\\textgreater ")
	 .replace("~","\\~{} ");
   } // end of TexSafeText()

} // end of class LatexSerializer
