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
public class VttToTextGrid
   extends GuiProgram
{
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
    * Whether to start processing immediately (true) or wait until the user presses "Convert" (false).
    * @see #getBatchMode()
    * @see #setBatchMode(Boolean)
    */
   protected Boolean batchMode = Boolean.FALSE;
   /**
    * Getter for {@link #batchMode}: Whether to start processing immediately (true) or wait until the user presses "Convert" (false).
    * @return Whether to start processing immediately (true) or wait until the user presses "Convert" (false).
    */
   public Boolean getBatchMode() { return batchMode; }
   /**
    * Setter for {@link #batchMode}: Whether to start processing immediately (true) or wait until the user presses "Convert" (false).
    * @param newBatchMode Whether to start processing immediately (true) or wait until the user presses "Convert" (false).
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
   
   /**
    * Default constructor.
    */
   public VttToTextGrid()
   {
      setDefaultWindowTitle("WebVTT to TextGrid converter");
      setDefaultWidth(800);
      setDefaultHeight(600);
   } // end of constructor
   
   public static void main(String argv[])
   {
      new VttToTextGrid().mainRun(argv);
   }

   public void init()
   {
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

      final FileNameExtensionFilter fileFilter = new FileNameExtensionFilter("WebVTT files", "vtt");
      btnAdd.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	       JFileChooser chooser = new JFileChooser();
	       chooser.setFileFilter(fileFilter);
	       chooser.setMultiSelectionEnabled(true);
	       
	       int returnVal = chooser.showOpenDialog(frame_);
	       if(returnVal == JFileChooser.APPROVE_OPTION)
	       {
		  if (verbose) System.out.println("Chosen file " + chooser.getSelectedFile().getName());
		  for (File file : chooser.getSelectedFiles())
		  {
		     if (verbose) System.out.println("Adding selected file " + file.getPath());
		     ((DefaultListModel)files.getModel()).add(files.getModel().getSize(), file);
		  }
	       }
	    }
	 });

      btnRemove.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
	       for (int i : files.getSelectedIndices())
	       {
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
	       if (!dtde.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor))
	       {
		  dtde.rejectDrag();
	       }
	    }
	    public void drop(DropTargetDropEvent dtde) 
	    {
	       try
	       {
		  if (dtde.getTransferable().isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor))
		  {
		     dtde.acceptDrop(dtde.getDropAction());
		     List droppedFiles = (java.util.List) dtde.getTransferable()
			.getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
		     ListIterator f = droppedFiles.listIterator();
		     while(f.hasNext())
		     {
			File file = (File)f.next();
			if (fileFilter.accept(file))
			{
			   if (verbose) System.out.println("Adding dropped file: " + file.getPath());
			   ((DefaultListModel)files.getModel()).add(files.getModel().getSize(), file);
			}
			else
			{
			   if (verbose) System.out.println("Dropped file incorrect type: " + file.getPath());
			}
		     } // next file
		     dtde.dropComplete(true);
		  } 
		  else // not a file list
		  {
		     dtde.rejectDrop();
		  }
	       }
	       catch(Exception e)
	       {
		  dtde.rejectDrop();
		  System.err.println("ERROR dropping file: " + e.getMessage());
		  e.printStackTrace(System.err);
	       }
	    }	    
	 });
      target.setActive(true);

   }

   public void start()
   {
      if (arguments.size() == 0)
      {
	 System.err.println("Nothing to do yet. (Try using --usage command line switch)");
      }
      for (String argument: arguments)
      {
      	 if (verbose) System.out.println("argument: " + argument);
      	 try
      	 {
      	    File file = new File(argument);
      	    if (verbose) System.out.println("file: " + file.getPath());
      	    ((DefaultListModel)files.getModel()).add(files.getModel().getSize(), file);
      	 }
      	 catch(Exception exception)
      	 {
      	    System.err.println("Error processing: " + argument + " : " + exception.getMessage());
      	    exception.printStackTrace(System.err);
      	 }
      } // next argument
      if (getBatchMode())
      {
	 convertFiles();
      }
   }
   
   /**
    * Converts the files in the <var>files</var> list.
    */
   public void convertFiles()
   {
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
   public void convertBatch(Vector<File> files)
   {
      progress.setMaximum(files.size());
      progress.setValue(0);
      progress.setString("");
      int f = 0;
      for (File inputFile: files)
      {
	 progress.setString(inputFile.getName());
	 try
	 {
	    if (!inputFile.exists())
	    {
	       System.err.println("Input file doesn't exist: " + inputFile.getPath());
	    }
	    else
	    {
	       convert(inputFile);
	    }
	 }
	 catch(Exception exception)
	 {
	    System.err.println("Error processing: " + inputFile.getPath() + " : " + exception.getMessage());
	    exception.printStackTrace(System.err);
	 }
	 progress.setValue(++f);
      } // next file
      progress.setString("Finished.");
   } // end of convertBatch()
   
   /**
    * Converts a file.
    * @param inputFile
    * @param outputFile
    * @throws Exception
    */
   public void convert(File inputFile)
      throws Exception
   {
      if (verbose) System.out.println("Converting " + inputFile.getPath());
      
      Schema schema = new Schema("who", "turn", "utterance", null,
	 new Layer("who", "Participants", 0, true, true, true),
	 new Layer("turn", "Speaker turns", 2, true, false, false, "who", true),
	 new Layer("utterance", "Utterances", 2, true, false, true, "turn", true));

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
      for (String warning : deserializer.getWarnings())
      {
	 System.out.println(warning);
      }
      
      // serialize...

      // create serializer
      TextGridSerialization serializer = new TextGridSerialization();
      if (verbose) System.out.println("Serializing with " + serializer.getDescriptor());
      
      // configure serializer
      ParameterSet configuration = serializer.configure(new ParameterSet(), schema);
      serializer.configure(configuration, schema);

      // serialize
      final Vector<SerializationException> exceptions = new Vector<SerializationException>();
      final Vector<NamedStream> outputStreams = new Vector<NamedStream>();
      serializer.serialize(Utility.OneGraphSpliterator(g), null,
                           stream -> outputStreams.add(stream),
                           warning -> { if (verbose) System.out.println(warning); },
                           exception -> exceptions.add(exception));
      for (NamedStream stream : outputStreams)
      {
	 File dir = inputFile.getParentFile();
	 if (dir == null) dir = new File(".");
	 stream.save(dir);
      }
      
      if (verbose) System.out.println("Finished " + inputFile.getPath());
      for (SerializationException x : exceptions) System.err.println(x.toString());
   } // end of convert()

   
} // end of class VttToTextGrid
