//
// Copyright 2021 New Zealand Institute of Language, Brain and Behaviour, 
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
package nzilbb.converter;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.filechooser.FileNameExtensionFilter;
import nzilbb.ag.Constants;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.serialize.GraphDeserializer;
import nzilbb.ag.serialize.GraphSerializer;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.formatter.elan.EAFSerialization;
import nzilbb.formatter.webvtt.VttSerialization;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;

/**
 * Converts ELAN .eaf files to WebVTT subtitles.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Converts ELAN .eaf files to WebVTT subtitles",arguments="file1.eaf file2.eaf ...")
public class EafToVtt extends Converter {

  // Attributes:
   
  /**
   * Regular rexpression to match ELAN tiers to ignore during conversion.
   * @see #getIgnoreTiers()
   * @see #setIgnoreTiers(String)
   */
  protected String ignoreTiers;
  /**
   * Getter for {@link #ignoreTiers}: Regular rexpression to match ELAN tiers to ignore
   * during conversion. 
   * @return Regular rexpression to match ELAN tiers to ignore during conversion.
   */
  public String getIgnoreTiers() { return ignoreTiers; }
  /**
   * Setter for {@link #ignoreTiers}: Regular rexpression to match ELAN tiers to ignore
   * during conversion. 
   * @param newIgnoreTiers Regular rexpression to match ELAN tiers to ignore during conversion.
   */
  @Switch("Comma-separated list of ELAN tiers to ignore during conversion")
  public EafToVtt setIgnoreTiers(String newIgnoreTiers) { ignoreTiers = newIgnoreTiers; return this; }
   
  // Methods:
   
  /**
   * Default constructor.
   */
  public EafToVtt() {
    setDefaultWindowTitle("ELAN to WebVTT converter");
    // don't use inline annotation conventions, by default
    setSwitch("useConventions","false");
    
    info = "All tiers will be interpreted as transcription of participant speech."
      +" If some tiers contain other annotations, use the --ignoreTiers command line switch"
      +" to exclude them from the conversion using a regular expression, e.g.:"
      +"\n --ignoreTiers=Noise|Topic"
      +"\n "
      +"\nELAN has no direct mechanism for marking non-speech annotations in their position"
      +" within the transcript text. However, this converter can assume the use of textual"
      +" conventions in various ways to make certain annotations: "
      +"\n - To tag a word with its pronunciation, enter the pronunciation in square brackets,"
      +" directly following the word (i.e. with no intervening space), e.g.: "
      +"\n …this was at Wingatui[wIN@tui]…"
      +"\n - To tag a word with its full orthography (if the transcript doesn't include it),"
      +" enter the orthography in round parentheses, directly following the word (i.e. with no"
      +" intervening space), e.g.: "
      +"\n …I can't remem~(remember)…"
      +"\n - To insert a noise annotation within the text, enclose it in square brackets"
      +" (surrounded by spaces so it's not taken as a pronunciation annotation), e.g.: "
      +"\n …sometimes me [laughs] not always but sometimes…"
      +"\n - To insert a comment annotation within the text, enclose it in curly braces"
      +" (surrounded by spaces), e.g.: "
      +"\n …beautifully warm {softly} but its…"
      +"\nTo enable these transcription conventions, use the --useConventions"
      +" command-line switch.";
  } // end of constructor
   
  public static void main(String argv[]) {
    new EafToVtt().mainRun(argv);
  }

  /**
   * Specify the schema to used by  {@link #convert(File)}.
   * @return The schema.
   */
  @Override
  public Schema getSchema() {
    Schema schema = super.getSchema();
    // include convention layers
    schema.addLayer(
      new Layer("noise", "Noises")
      .setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(false).setSaturated(false));
    schema.addLayer(
      new Layer("comment", "Comments").setAlignment(Constants.ALIGNMENT_INTERVAL)
      .setPeers(true).setPeersOverlap(true).setSaturated(false));
    schema.addLayer(
      new Layer("pronounce", "Pronunciation tags").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getWordLayerId()).setParentIncludes(true));
    schema.addLayer(
      new Layer("lexical", "Lexical tags").setAlignment(Constants.ALIGNMENT_NONE)
      .setPeers(false).setPeersOverlap(false).setSaturated(true)
      .setParentId(schema.getWordLayerId()).setParentIncludes(true));
    return schema;
  } // end of getSchema()

  /** File filter for identifying files of the correct type */
  protected FileNameExtensionFilter getFileFilter() {
    return new FileNameExtensionFilter("ELAN files", "eaf");
  }

  /**
   * Gets the deserializer that #convert(File) uses.
   * @return The deserializer to use.
   */
  public GraphDeserializer getDeserializer() {
    return new EAFSerialization();
  }

  /**
   * Gets the serializer that #convert(File) uses.
   * @return The serializer to use.
   */
  public GraphSerializer getSerializer() {
    return new VttSerialization();
  }
  
  /**
   * Un-map tiers that are matched by {@link #ignoreTiers}.
   * @param parameters The default parameters.
   * @return The new configuration.
   */
  public ParameterSet deserializationParameters(ParameterSet parameters) {
    if (ignoreTiers != null && ignoreTiers.length() > 0) {
      Pattern ignorePattern = Pattern.compile(ignoreTiers);
      // for each parameter
      for (Parameter p : parameters.values()) {
        // if it's a tier mapping
        if (p.getName().startsWith("tier")
            // and it matches ignoreTiers
            && ignorePattern.matcher(p.getLabel()).matches()) {
          // ignore this tier
          p.setValue(null);
        }
      } // next parameter
    } // ignoreTiers is set
    return parameters;
  } // end of deserializationConfiguration()

  /**
   * Specifies which layers should be given to the serializer. The default implementaion
   * returns only the "utterance" layer.
   * @return An array of layer IDs.
   */
  public String[] getLayersToSerialize() {
    String[] layers = { "utterance" };
    return layers;
  } // end of getLayersToSerialize()
      
  private static final long serialVersionUID = -1;
} // end of class EafToVtt
