//
// Copyright 2022 New Zealand Institute of Language, Brain and Behaviour, 
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
import nzilbb.formatter.praat.TextGridSerialization;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;

/**
 * Converts Praat .TextGrid files to ELAN .eaf files.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Converts Praat .TextGrid files to ELAN .eaf files",arguments="file1.TextGrid file2.TextGrid ...")
public class TextGridToEaf extends Converter {
  
  // Attributes:
  
  /**
   * Regular rexpression to match Praat tiers to ignore during conversion.
   * @see #getIgnoreTiers()
   * @see #setIgnoreTiers(String)
   */
  protected String ignoreTiers;
  /**
   * Getter for {@link #ignoreTiers}: Regular rexpression to match Praat tiers to ignore
   * during conversion. 
   * @return Regular rexpression to match Praat tiers to ignore during conversion.
   */
  public String getIgnoreTiers() { return ignoreTiers; }
  /**
   * Setter for {@link #ignoreTiers}: Regular rexpression to match Praat tiers to ignore
   * during conversion. 
   * @param newIgnoreTiers Regular rexpression to match Praat tiers to ignore during conversion.
   */
  @Switch("Comma-separated list of ELAN tiers to ignore during conversion")
  public TextGridToEaf setIgnoreTiers(String newIgnoreTiers) { ignoreTiers = newIgnoreTiers; return this; }
   
  // Methods:

  /**
   * Un-map tiers that are matched by {@link #ignoreTiers}, and map all other tiers to
   * "utterance".
   * @param parameters The default parameters.
   * @return The new configuration.
   */
  public ParameterSet deserializationParameters(ParameterSet parameters) {
    Schema schema = getSchema();
    Pattern ignorePattern = null;
    if (ignoreTiers != null && ignoreTiers.length() > 0) {
      ignorePattern = Pattern.compile(ignoreTiers);
    }
    // for each parameter
    for (Parameter p : parameters.values()) {
      // if it's a tier mapping
      if (p.getName().startsWith("tier")) {
        if (ignorePattern != null && ignorePattern.matcher(p.getLabel()).matches()) {
          // ignore this tier
          p.setValue(null);
        } else {
          // point tiers will have p.getValue() == null, and we don't want to map them to anything
          if (p.getValue() != null) { // otherwise, if it's mapped to any layer
            // make sure it's the "utterance" layer
            p.setValue(schema.getUtteranceLayer());
          }
        }
      }
    } // next parameter
    return parameters;
  } // end of deserializationConfiguration()

  /**
   * Default constructor.
   */
  public TextGridToEaf() {
    info = "Praat tiers are converted directly to ELAN tiers as-is, except for point tiers"
      +" which are not supported by ELAN.";
  } // end of constructor
  
  public static void main(String argv[]) {
    new TextGridToEaf().mainRun(argv);
  }
  
  /** File filter for identifying files of the correct type */
  protected FileNameExtensionFilter getFileFilter() {
    return new FileNameExtensionFilter("Praat TextGrids", "TextGrid");
  }
  
  /**
   * Gets the deserializer that #convert(File) uses.
   * @return The deserializer to use.
   */
  public GraphDeserializer getDeserializer() {
    return new TextGridSerialization()
       .setUseConventions(false);
  }
  
  /**
   * Gets the serializer that #convert(File) uses.
   * @return The serializer to use.
   */
  public GraphSerializer getSerializer() {
    return new EAFSerialization();
  }
  
  /**
   * Everything in the EAF file will have been interpreted as an utterance, so serialize
   * the utterance layer.
   * @return An array of layer IDs.
   */
  public String[] getLayersToSerialize() {
    return new String[] { getSchema().getUtteranceLayerId() };
  } // end of getLayersToSerialize()
  
  private static final long serialVersionUID = -1;
} // end of class TextGridToEaf
