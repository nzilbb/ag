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

import javax.swing.filechooser.FileNameExtensionFilter;
import nzilbb.ag.Constants;
import nzilbb.ag.Layer;
import nzilbb.ag.Schema;
import nzilbb.ag.serialize.IDeserializer;
import nzilbb.ag.serialize.ISerializer;
import nzilbb.text.PlainTextSerialization;
import nzilbb.transcriber.TranscriptSerialization;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;

/**
 * Converts Transcriber .trs files to plain text.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Converts Transcriber .trs files to plain text files",arguments="file1.trs file2.trs ...")
public class TrsToText extends Converter {

   // Attributes:
   
   /**
    * Include meta-data (default is true).
    * @see #getMetaData()
    * @see #setMetaData(Boolean)
    */
   protected Boolean metaData = Boolean.TRUE;
   /**
    * Getter for {@link #metaData}: Include meta-data (default is true).
    * @return Include meta-data (default is true).
    */
   public Boolean getMetaData() { return metaData; }
   /**
    * Setter for {@link #metaData}: Include meta-data (default is true).
    * @param newMetaData Include meta-data (default is true).
    */
   @Switch(value="Include meta-data (default is true)",compulsory=false)
   public TrsToText setMetaData(Boolean newMetaData) { metaData = newMetaData; return this; }
   
   /**
    * Suppress all annotations and speaker names.
    * @see #getTextOnly()
    * @see #setTextOnly(Boolean)
    */
   protected Boolean textOnly = Boolean.FALSE;
   /**
    * Getter for {@link #textOnly}: Suppress all annotations and speaker names.
    * @return Suppress all annotations and speaker names.
    */
   public Boolean getTextOnly() { return textOnly; }
   /**
    * Setter for {@link #textOnly}: Suppress all annotations and speaker names.
    * @param newTextOnly Suppress all annotations and speaker names.
    */
   @Switch(value="Suppress all annotations and speaker names",compulsory=false)
   public TrsToText setTextOnly(Boolean newTextOnly) {
      textOnly = newTextOnly;
      if (textOnly) {
         // suppress participant names
         extraSwitches.put("participantFormat","");
      }
      return this;
   }

   // Methods:
   
   /**
    * Default constructor.
    */
   public TrsToText() {
      setDefaultWindowTitle("Transcriber to PDF converter");
   } // end of constructor
   
   public static void main(String argv[]) {
      new TrsToText().mainRun(argv);
   }

   /** File filter for identifying files of the correct type */
   protected FileNameExtensionFilter getFileFilter() {
      return new FileNameExtensionFilter("Transcriber files", "trs");
   }

   /**
    * Gets the deserializer that #convert(File) uses.
    * @return The deserializer to use.
    */
   public IDeserializer getDeserializer() {
      return new TranscriptSerialization();
   }

   /**
    * Gets the serializer that #convert(File) uses.
    * @return The serializer to use.
    */
   public ISerializer getSerializer() {
      return new PlainTextSerialization();
   }   
   
   /**
    * Specify the schema to used by  {@link #convert(File)}.
    * @return The schema.
    */
   public Schema getSchema() {
      Schema schema = super.getSchema();
      if (!textOnly) {
         // meta-data layers
         schema.addLayer(
            new Layer("scribe", "Scribe")         
            .setAlignment(Constants.ALIGNMENT_NONE)
            .setPeers(false).setPeersOverlap(true).setSaturated(true));
         schema.addLayer(
            new Layer("version", "Version")         
            .setAlignment(Constants.ALIGNMENT_NONE)
            .setPeers(false).setPeersOverlap(true).setSaturated(true));
         schema.addLayer(
            new Layer("versiondate", "Version Date")         
            .setAlignment(Constants.ALIGNMENT_NONE)
            .setPeers(false).setPeersOverlap(true).setSaturated(true));
         schema.addLayer(
            new Layer("program", "Program")         
            .setAlignment(Constants.ALIGNMENT_NONE)
            .setPeers(false).setPeersOverlap(true).setSaturated(true));
         schema.addLayer(
            new Layer("airdate", "Air Date")         
            .setAlignment(Constants.ALIGNMENT_NONE)
            .setPeers(false).setPeersOverlap(true).setSaturated(true));
         schema.addLayer(
            new Layer("language", "Language")         
            .setAlignment(Constants.ALIGNMENT_NONE)
            .setPeers(false).setPeersOverlap(true).setSaturated(true));
         
         // noise layer
         schema.addLayer(
            new Layer("noise", "Noise")         
            .setAlignment(Constants.ALIGNMENT_INTERVAL)
            .setPeers(true).setPeersOverlap(false).setSaturated(false));
         // comment layer
         schema.addLayer(
            new Layer("comment", "Comment")         
            .setAlignment(Constants.ALIGNMENT_INTERVAL)
            .setPeers(true).setPeersOverlap(false).setSaturated(false));
      }
      return schema;
   } // end of getSchema()
   
   /**
    * Specifies which layers should be given to the serializer. 
    * @return An array of layer IDs.
    */
   public String[] getLayersToSerialize() {
      if (textOnly) {
         String[] layers = { "utterance" };
         return layers;
      } else if (metaData) {
         String[] layers = {
            "utterance", "noise", "comment",
            "scribe", "version", "versiondate", "program", "airdate", "language" };
         return layers;
      } else {
         String[] layers = { "utterance", "noise", "comment" };
         return layers;
      }
   } // end of getLayersToSerialize()
   
   private static final long serialVersionUID = -1;
} // end of class TrsToText
