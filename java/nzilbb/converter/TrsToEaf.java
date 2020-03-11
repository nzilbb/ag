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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import nzilbb.ag.*;
import nzilbb.ag.serialize.IDeserializer;
import nzilbb.ag.serialize.ISerializer;
import nzilbb.ag.serialize.SerializationException;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.configure.ParameterSet;
import nzilbb.elan.EAFSerialization;
import nzilbb.transcriber.TranscriptSerialization;
import nzilbb.util.GuiProgram;
import nzilbb.util.IO;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;

/**
 * Converts Transcriber .trs files to ELAN .eaf files.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Converts Transcriber .trs transcripts to ELAN .eaf files",arguments="file1.trs file2.trs ...")
public class TrsToEaf extends Converter {
   
   // Methods:
   
   /**
    * Default constructor.
    */
   public TrsToEaf() {
      setDefaultWindowTitle("Transcriber to ELAN converter");
      setDefaultWidth(800);
      setDefaultHeight(600);
   } // end of constructor
   
   public static void main(String argv[]) {
      new TrsToEaf().mainRun(argv);
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
      return new EAFSerialization();
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
    * Specifies which layers should be given to the serializer. The default implementaion
    * returns only the "utterance" layer.
    * @return An array of layer IDs.
    */
   public String[] getLayersToSerialize() {
      String[] layers = { "utterance", "topic" };
      return layers;
   } // end of getLayersToSerialize()
      
   private static final long serialVersionUID = -1;
} // end of class TrsToEaf
