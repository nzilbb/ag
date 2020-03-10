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
import nzilbb.ag.serialize.SerializationException;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.configure.ParameterSet;
import nzilbb.elan.EAFSerialization;
import nzilbb.util.GuiProgram;
import nzilbb.util.IO;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;
import nzilbb.webvtt.VttSerialization;

/**
 * Converts WebVTT subtitle files to ELAN .eaf files.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Converts WebVTT subtitle files to ELAN files",arguments="file1.vtt file2.vtt ...")
public class VttToEaf extends Converter {
   
   /**
    * Default constructor.
    */
   public VttToEaf() {
      setDefaultWindowTitle("WebVTT to ELAN converter");
   } // end of constructor
   
   public static void main(String argv[]) {
      new VttToEaf().mainRun(argv);
   }

   /** File filter for identifying files of the correct type */
   protected FileNameExtensionFilter getFileFilter() {
      return new FileNameExtensionFilter("WebVTT files", "vtt");
   }

   /**
    * Converts a file.
    * @param inputFile
    * @throws Exception
    */
   public void convert(File inputFile) throws Exception {
      if (verbose) System.out.println("Converting " + inputFile.getPath());
      
      Schema schema = new Schema(
         "who", "turn", "utterance", null,
         new Layer("who", "Participants")
         .setAlignment(Constants.ALIGNMENT_NONE)
         .setPeers(true).setPeersOverlap(true).setSaturated(true),
	 new Layer("turn", "Speaker turns")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false)
         .setParentId("who").setParentIncludes(true),
	 new Layer("utterance", "Utterances")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(true)
         .setParentId("turn").setParentIncludes(true),
	 new Layer("word", "Words")
         .setAlignment(Constants.ALIGNMENT_INTERVAL)
         .setPeers(true).setPeersOverlap(false).setSaturated(false)
         .setParentId("turn").setParentIncludes(true));

      // deserialize...
      
      NamedStream[] streams = { new NamedStream(inputFile) };
      
      // create deserializer
      VttSerialization deserializer = new VttSerialization();
      if (verbose) System.out.println("Deserializing with " + deserializer.getDescriptor());

      // configure deserializer
      ParameterSet defaultConfig = deserializer.configure(new ParameterSet(), schema);
      deserializer.configure(defaultConfig, schema);

      // load the stream
      ParameterSet defaultParameters = deserializer.load(streams, schema);
      
      // configure the deserialization
      deserializer.setParameters(defaultParameters);
      
      Graph[] graphs = deserializer.deserialize();
      Graph g = graphs[0];     
      for (String warning : deserializer.getWarnings()) {
	 System.out.println(warning);
      }

      // strip extension off name
      g.setId(IO.WithoutExtension(g.getId()));

      // serialize...

      // create serializer
      EAFSerialization serializer = new EAFSerialization();
      if (verbose) System.out.println("Serializing with " + serializer.getDescriptor());
      
      // configure serializer
      ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
      serializer.configure(configuration, schema);

      // serialize
      final File dir = (inputFile.getParentFile() != null? inputFile.getParentFile()
                        : new File("."));
      String[] layers = { "utterance" };
      serializer.serialize(
         Utility.OneGraphSpliterator(g), layers,
         stream -> {
            try {
               stream.save(dir);
            } catch(IOException exception) {
               System.err.println(exception.toString());
            }
         },
         warning -> { if (verbose) System.out.println(warning); },
         exception -> System.err.println(exception.toString()));
      
      if (verbose) System.out.println("Finished " + inputFile.getPath());
   } // end of convert()

   private static final long serialVersionUID = -1;
} // end of class VttToEaf
