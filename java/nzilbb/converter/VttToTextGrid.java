//
// Copyright 2019 New Zealand Institute of Language, Brain and Behaviour, 
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

import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.List;
import java.util.ListIterator;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.filechooser.FileNameExtensionFilter;
import nzilbb.util.IO;
import nzilbb.util.GuiProgram;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;
import nzilbb.configure.ParameterSet;

import nzilbb.ag.*;
import nzilbb.ag.serialize.IDeserializer;
import nzilbb.ag.serialize.ISerializer;
import nzilbb.ag.serialize.SerializationException;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.webvtt.VttSerialization;
import nzilbb.praat.TextGridSerialization;

/**
 * Converts WebVTT subtitle files to Praat TextGrids.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription(value="Converts WebVTT subtitle files to Praat TextGrids",arguments="file1.vtt file2.vtt ...")
public class VttToTextGrid extends Converter {
   
   /**
    * Default constructor.
    */
   public VttToTextGrid() {
      setDefaultWindowTitle("WebVTT to TextGrid converter");
   } // end of constructor
   
   public static void main(String argv[]) {
      new VttToTextGrid().mainRun(argv);
   }

   /** File filter for identifying files of the correct type */
   protected FileNameExtensionFilter getFileFilter() {
      return new FileNameExtensionFilter("WebVTT files", "vtt");
   }

   /**
    * Gets the deserializer that #convert(File) uses.
    * @return The deserializer to use.
    */
   public IDeserializer getDeserializer() {
      return new VttSerialization();
   }

   /**
    * Gets the serializer that #convert(File) uses.
    * @return The serializer to use.
    */
   public ISerializer getSerializer() {
      return new TextGridSerialization();
   }

   private static final long serialVersionUID = -1;
} // end of class VttToTextGrid
