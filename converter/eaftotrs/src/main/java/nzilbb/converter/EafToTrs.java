//
// Copyright 2020 New Zealand Institute of Language, Brain and Behaviour, 
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
import javax.swing.filechooser.FileNameExtensionFilter;
import nzilbb.ag.Constants;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.serialize.GraphDeserializer;
import nzilbb.ag.serialize.GraphSerializer;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.formatter.elan.EAFSerialization;
import nzilbb.formatter.transcriber.TranscriptSerialization;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;

/**
 * Converts ELAN .eaf files to Transcriber .trs files.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Converts ELAN .eaf files to Transcriber .trs transcripts",arguments="file1.eaf file2.eaf ...")
public class EafToTrs extends Converter {

   // Attributes:
   
   /**
    * A list of names of tiers to ignore.
    * @see #getTiersToIgnore()
    * @see #setTiersToIgnore(Set<String>)
    */
   protected Set<String> tiersToIgnore = new HashSet<String>();
   /**
    * Getter for {@link #tiersToIgnore}: A list of names of tiers to ignore.
    * @return A list of names of tiers to ignore.
    */
   public Set<String> getTiersToIgnore() { return tiersToIgnore; }
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public EafToTrs() {
      setDefaultWindowTitle("ELAN to Transcriber converter");
   } // end of constructor
   
   public static void main(String argv[]) {
      new EafToTrs().mainRun(argv);
   }

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
      return new TranscriptSerialization();
   }

   /**
    * Specify the schema to used by  {@link #convert(File)}.
    * @return The schema.
    */
   public Schema getSchema() {
      Schema schema = super.getSchema();
      // include topic layer
      schema.addLayer(
         new Layer("topic", "Topic")         
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false));
      return schema;
   } // end of getSchema()

   /**
    * Command-line-accessible setter for {@link #tiersToIgnore}: A list of names of tiers
    * to ignore.  
    * @param tiers A comma-separated list of ELAN tiers to ignore.
    */
   @Switch(value="A comma-separated list of ELAN tiers to ignore",compulsory=false)
   public EafToTrs setIgnoreTiers(String tiers)
   {
      if (tiers != null) {
         for (String tier : tiers.split(",")) tiersToIgnore.add(tier);
      }
      return this;
   }
   
   /**
    * Determine the final parameters for deserialization. Implementors can adjust the
    * default configuration before it's applied. This method is invoked once for each
    * input file.
    * @param defaultConfig
    * @return The new configuration.
    */
   public ParameterSet deserializationParameters(ParameterSet defaultConfig) {
      // ignore specified tiers
      for (Parameter p : defaultConfig.values()) {
         if (p.getName().startsWith("tier") && tiersToIgnore.contains(p.getLabel())) {
            p.setValue(null);
         }
      } // next parameter
      return defaultConfig;
   } // end of deserializationConfiguration()

   /**
    * Specifies which layers should be given to the serializer. The default implementaion
    * returns only the "utterance" layer.
    * @return An array of layer IDs.
    */
   public String[] getLayersToSerialize() {
      String[] layers = { "utterance", "topic" };
      return layers;
   } // end of getLayersToSerialize()
      
   private static final long serialVersionUID = -1;
} // end of class EafToTrs
