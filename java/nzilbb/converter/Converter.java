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
 * Base class for converters, which implements a number of common functions for
 * convenience.
 * @author Robert Fromont robert@fromont.net.nz
 */
public abstract class Converter extends GuiProgram {
   
   // Attributes:
   
   /**
    * Whether detailed verbose output is printed or not.
    * @see #getVerbose()
    * @see #setVerbose(Boolean)
    */
   protected Boolean verbose = Boolean.FALSE;
   /**
    * Getter for {@link #verbose}: Whether detailed verbose output is printed or not.
    * @return Whether detailed verbose output is printed or not.
    */
   public Boolean getVerbose() { return verbose; }
   /**
    * Setter for {@link #verbose}: Whether detailed verbose output is printed or not.
    * @param newVerbose Whether detailed verbose output is printed or not.
    */
   @Switch(value="Whether detailed verbose output is printed or not",compulsory=false)
   public void setVerbose(Boolean newVerbose) { verbose = newVerbose; }
   
   /**
    * Whether to start processing immediately (true) or wait until the user presses
    * "Convert" (false). 
    * @see #getBatchMode()
    * @see #setBatchMode(Boolean)
    */
   protected Boolean batchMode = Boolean.FALSE;
   /**
    * Getter for {@link #batchMode}: Whether to start processing immediately (true) or
    * wait until the user presses "Convert" (false). 
    * @return Whether to start processing immediately (true) or wait until the user
    * presses "Convert" (false). 
    */
   public Boolean getBatchMode() { return batchMode; }
   /**
    * Setter for {@link #batchMode}: Whether to start processing immediately (true) or
    * wait until the user presses "Convert" (false). 
    * @param newBatchMode Whether to start processing immediately (true) or wait until the
    * user presses "Convert" (false). 
    */
   @Switch(value="Start processing immediately, rather than waiting for the user to press Convert",compulsory=false)
   public void setBatchMode(Boolean newBatchMode) { batchMode = newBatchMode; }

   // UI
   protected JButton btnAdd = new JButton("+");
   protected JButton btnRemove = new JButton("-");
   protected JList<File> files = new JList<File>(new DefaultListModel<File>());
   protected JButton btnConvert = new JButton("Convert");
   protected JProgressBar progress = new JProgressBar();

   // Methods:

   /** File filter for identifying files of the correct type */
   protected abstract FileNameExtensionFilter getFileFilter();
   
   /**
    * Gets the deserializer that {@link #convert(File)} uses.
    * @return The deserializer to use.
    */
   public abstract IDeserializer getDeserializer();

   /**
    * Gets the serializer that {@link #convert(File)} uses.
    * @return The serializer to use.
    */
   public abstract ISerializer getSerializer();
   
   /**
    * Specifies which layers should be given to the serializer. The default implementaion
    * returns only the "utterance" layer.
    * @return An array of layer IDs.
    */
   public String[] getLayersToSerialize() {
      String[] layers = { "utterance" };
      return layers;
   } // end of getLayersToSerialize()
   
   /**
    * Specify the schema to used by  {@link #convert(File)}.
    * @return The schema.
    */
   public Schema getSchema() {
      return new Schema(
         "who", "turn", "utterance", "word",
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
   } // end of getSchema()

   /**
    * Converts a file. The default implementation uses a default schema, default settings
    * for serializations, and serializes the "utterance" layer only. 
    * @param inputFile
    * @throws Exception
    */
   public void convert(File inputFile) throws Exception {
      if (verbose) System.out.println("Converting " + inputFile.getPath());
      
      Schema schema = getSchema();

      // deserialize...
      
      NamedStream[] streams = { new NamedStream(inputFile) };
      
      // create deserializer
      IDeserializer deserializer = getDeserializer();
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
      ISerializer serializer = getSerializer();
      if (verbose) System.out.println("Serializing with " + serializer.getDescriptor());
      
      // configure serializer
      ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
      serializer.configure(configuration, schema);

      // serialize
      final File dir = (inputFile.getParentFile() != null? inputFile.getParentFile()
                        : new File("."));
      serializer.serialize(
         Utility.OneGraphSpliterator(g), getLayersToSerialize(),
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
   
   /**
    * Default constructor.
    */
   public Converter() {
      setDefaultWindowTitle("Converter");
      setDefaultWidth(800);
      setDefaultHeight(600);
   } // end of constructor
   
   @SuppressWarnings({"unchecked","rawtypes"})
   public void init() {
      
      interpretAppletParameters();

      // build UI
      frame_.getContentPane().setLayout(new BorderLayout());

      JPanel pnlEast = new JPanel(new FlowLayout());
      btnAdd.setToolTipText("Add a file to the list");
      pnlEast.add(btnAdd);
      btnRemove.setToolTipText("Remove selected files from the list");
      pnlEast.add(btnRemove);
      getContentPane().add(pnlEast, BorderLayout.EAST);

      files.setToolTipText("Drop/drop files to convert here");
      files.setCellRenderer(new DefaultListCellRenderer() {
	    public Component getListCellRendererComponent(
	       JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
	       super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
	       setText(((File)value).getName());
	       return this;
	    }
	 });
      getContentPane().add(new JScrollPane(files), BorderLayout.CENTER);

      JPanel pnlSouth = new JPanel(new BorderLayout());
      progress.setStringPainted(true);
      pnlSouth.add(progress, BorderLayout.CENTER);
      btnConvert.setToolTipText("Convert all files");
      pnlSouth.add(btnConvert, BorderLayout.EAST);
      getContentPane().add(pnlSouth, BorderLayout.SOUTH);

      // events
      
      frame_.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) { System.exit(0); }});

      final FileNameExtensionFilter fileFilter = getFileFilter();
      btnAdd.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	       JFileChooser chooser = new JFileChooser();
	       chooser.setFileFilter(fileFilter);
	       chooser.setMultiSelectionEnabled(true);
	       
	       int returnVal = chooser.showOpenDialog(frame_);
	       if(returnVal == JFileChooser.APPROVE_OPTION) {
		  if (verbose) System.out.println("Chosen file " + chooser.getSelectedFile().getName());
		  for (File file : chooser.getSelectedFiles()) {
		     if (verbose) System.out.println("Adding selected file " + file.getPath());
		     ((DefaultListModel)files.getModel()).add(files.getModel().getSize(), file);
		  }
	       }
	    }
	 });

      btnRemove.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	       for (int i : files.getSelectedIndices()) {
		  ((DefaultListModel)files.getModel()).remove(i);
	       }
	    }
	 });

      btnConvert.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) { convertFiles(); }
	 });

      DropTarget target = new DropTarget(files, new DropTargetAdapter() {
	    public void dragEnter(DropTargetDragEvent dtde) 
	    {
	       if (!dtde.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
		  dtde.rejectDrag();
	       }
	    }
	    public void drop(DropTargetDropEvent dtde) 
	    {
	       try {
		  if (dtde.getTransferable().isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
		     dtde.acceptDrop(dtde.getDropAction());
		     List droppedFiles = (java.util.List) dtde.getTransferable()
			.getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
		     ListIterator f = droppedFiles.listIterator();
		     while(f.hasNext()) {
			File file = (File)f.next();
			if (fileFilter.accept(file)) {
			   if (verbose) System.out.println("Adding dropped file: " + file.getPath());
			   ((DefaultListModel)files.getModel()).add(files.getModel().getSize(), file);
			} else {
			   if (verbose) System.out.println("Dropped file incorrect type: " + file.getPath());
			}
		     } // next file
		     dtde.dropComplete(true);
		  } else { // not a file list
		     dtde.rejectDrop();
		  }
	       } catch(Exception e) {
		  dtde.rejectDrop();
		  System.err.println("ERROR dropping file: " + e.getMessage());
		  e.printStackTrace(System.err);
	       }
	    }	    
	 });
      target.setActive(true);

   }

   @SuppressWarnings("unchecked")
   public void start() {
      
      if (arguments.size() == 0) {
	 System.err.println("Nothing to do yet. (Try using --usage command line switch)");
      }
      for (String argument: arguments) {
      	 if (verbose) System.out.println("argument: " + argument);
      	 try {
      	    File file = new File(argument);
      	    if (verbose) System.out.println("file: " + file.getPath());
      	    ((DefaultListModel)files.getModel()).add(files.getModel().getSize(), file);
      	 } catch(Exception exception) {
      	    System.err.println("Error processing: " + argument + " : " + exception.getMessage());
      	    exception.printStackTrace(System.err);
      	 }
      } // next argument
      if (getBatchMode()) {
	 convertFiles();
      }
   }
   
   /**
    * Converts the files in the <var>files</var> list.
    */
   public void convertFiles() {
      new Thread(new Runnable() {
	    public void run() {
	       btnConvert.setEnabled(false);
	       Vector<File> batch = new Vector<File>();
	       for (Object f : ((DefaultListModel)files.getModel()).toArray()) batch.add((File)f);
	       convertBatch(batch);
	       btnConvert.setEnabled(true);
	    }
	 }).start();
   } // end of convertFiles()
   
   /**
    * Converts a batch of files.
    * @param files
    */
   public void convertBatch(Vector<File> files) {
      progress.setMaximum(files.size());
      progress.setValue(0);
      progress.setString("");
      int f = 0;
      for (File inputFile: files) {
	 progress.setString(inputFile.getName());
	 try {
	    if (!inputFile.exists()) {
	       System.err.println("Input file doesn't exist: " + inputFile.getPath());
	    } else {
	       convert(inputFile);
	    }
	 } catch(Exception exception) {
	    System.err.println("Error processing: " + inputFile.getPath() + " : " + exception.getMessage());
	    exception.printStackTrace(System.err);
	 }
	 progress.setValue(++f);
      } // next file
      progress.setString("Finished.");
   } // end of convertBatch()
   
   private static final long serialVersionUID = -1;
} // end of class Converter
