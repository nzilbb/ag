//
// Copyright 2020-2021 New Zealand Institute of Language, Brain and Behaviour, 
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
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import nzilbb.ag.*;
import nzilbb.ag.serialize.GraphDeserializer;
import nzilbb.ag.serialize.GraphSerializer;
import nzilbb.ag.serialize.SerializationException;
import nzilbb.ag.serialize.util.NamedStream;
import nzilbb.ag.serialize.util.Utility;
import nzilbb.ag.util.Normalizer;
import nzilbb.configure.Parameter;
import nzilbb.configure.ParameterSet;
import nzilbb.util.GuiProgram;
import nzilbb.util.IO;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;

/**
 * Base class for converters, which implements a number of common functions for
 * convenience.
 * @author Robert Fromont robert@fromont.net.nz
 */
public abstract class Converter extends GuiProgram {
   
  // Attributes:
   
  /**
   * Display help info about available serialization parameters.
   * @see #getHelp()
   * @see #setHelp(Boolean)
   */
  protected Boolean help = Boolean.FALSE;
  /**
   * Getter for {@link #help}: Display help info about available serialization parameters.
   * @return Display help info about available serialization parameters.
   */
  public Boolean getHelp() { return help; }
  /**
   * Setter for {@link #help}: Display help info about available serialization parameters.
   * @param newHelp Display help info about available serialization parameters.
   */
  @Switch(value="Display help info about available serialization parameters",compulsory=false)
  public Converter setHelp(Boolean newHelp) { help = newHelp; return this; }
   
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
  @Switch(value="Print verbose output",compulsory=false)
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
  public abstract GraphDeserializer getDeserializer();

  /**
   * Gets the serializer that {@link #convert(File)} uses.
   * @return The serializer to use.
   */
  public abstract GraphSerializer getSerializer();
   
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
   * Determine the final parameters for deserialization. Implementors can adjust the
   * default configuration before it's applied. This method is invoked once for each
   * input file.
   * @param defaultConfig
   * @return The new configuration.
   */
  public ParameterSet deserializationParameters(ParameterSet defaultConfig) {
    return defaultConfig;
  } // end of deserializationConfiguration()
  
  /**
   * Process the graphs after they were deserialized, but before they're
   * serialized. Implementors can rename speakers, adjust meta-data, or change the graph
   * in any other way required before serialization. 
   * @param graphs
   */
  public void processGraphs(Graph[] graphs) {
  } // end of processGraphs()

   
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
  public void convert(File inputFile) throws SerializationException, Exception {
    if (verbose) System.out.println("Converting " + inputFile.getPath());

    // look for media files
    String nameWithoutExtension = IO.WithoutExtension(inputFile.getName());
    Vector<MediaFile> mediaFiles = new Vector<MediaFile>();
    for (File f : inputFile.getParentFile().listFiles(new FilenameFilter() {
        public boolean accept(File mediaDir, String name) {
          if (name.startsWith(nameWithoutExtension)) {
            String lowercase = name.toLowerCase();
            for (String suffix : MediaFile.SuffixToMimeType().keySet()) {
              if (lowercase.endsWith(suffix)) return true;
            }
          }
          return false;
        }
      })) {
      mediaFiles.add(new MediaFile(f).setUrl(f.toURI().toString()));
    } // next file

    Schema schema = getSchema();

    // deserialize...
      
    Vector<NamedStream> streams = new Vector<NamedStream>();
    // add the transcript file
    streams.add(new NamedStream(inputFile));
    // ... and also any media we found
    for (MediaFile f : mediaFiles) {
      streams.add(new NamedStream(
                    new FileInputStream(f.getFile()), f.getName(), f.getMimeType()));
    }
      
    // create deserializer
    GraphDeserializer deserializer = getDeserializer();
    if (verbose) System.out.println("Deserializing with " + deserializer.getDescriptor());

    // configure deserializer
    ParameterSet deserializerConfig = deserializer.configure(new ParameterSet(), schema);
    configureFromCommandLine(deserializerConfig, schema);
    if (verbose) {
      if (deserializerConfig.size() == 0) {
        System.out.println("No deserializer configuration parameters are required.");
      } else {
        System.out.println("Deserializer configuration:");
        for (Parameter p : deserializerConfig.values()) {
          System.out.println("\t" + p.getName() + " = " + p.getValue());
        }
      }
    }
    deserializer.configure(deserializerConfig, schema);

    // load the stream
    ParameterSet defaultParameters = deserializer.load(
      streams.toArray(new NamedStream[0]), schema);
    configureFromCommandLine(defaultParameters, schema);

    // let the subclass adjust the config
    ParameterSet parameters = deserializationParameters(defaultParameters);

    if (verbose) {
      if (parameters.size() == 0) {
        System.out.println("No deserialization parameters are required.");            
      } else {
        System.out.println("Deserialization parameters:");
        for (Parameter p : parameters.values()) {
          System.out.println("\t" + p.getName() + " = " + p.getValue());
        }
      }
    }

    // configure the deserialization
    deserializer.setParameters(parameters);
      
    Graph[] graphs = deserializer.deserialize();
    for (String warning : deserializer.getWarnings()) {
      System.out.println(inputFile.getName() + ": " + warning);
    }

    // strip extension off name
    for (Graph g : graphs) {
      g.setId(IO.WithoutExtension(g.getId()));
    }

    Normalizer normalizer = new Normalizer();
    for (Graph g : graphs) {
      normalizer.transform(g);
      g.commit();
    }

    // let the subclass process the graphs before they're serialized
    processGraphs(graphs);
    
    // give serializer access to any media
    if (mediaFiles.size() > 0) {
      GraphMediaProvider provider = new GraphMediaProvider() {
          public MediaFile[] getAvailableMedia() throws StoreException, PermissionException {
            return mediaFiles.toArray(new MediaFile[0]);
          }
          public String getMedia(String trackSuffix, String mimeType) 
            throws StoreException, PermissionException {
            for (MediaFile file : mediaFiles) {
              if (file.getMimeType().equals(mimeType)) return file.getUrl();
            } // next file
            return null;
          }
          public GraphMediaProvider providerForGraph(Graph graph) { return null; }
        };
      for (Graph g : graphs) {
        g.setMediaProvider(provider);
      }
    }
    
    // serialize...

    // create serializer
    GraphSerializer serializer = getSerializer();
    if (verbose) System.out.println("Serializing with " + serializer.getDescriptor());
      
    // configure serializer
    ParameterSet serializerConfig = serializer.configure(new ParameterSet(), schema);
    configureFromCommandLine(serializerConfig, graphs[0].getSchema());
    if (verbose) {
      if (serializerConfig.size() == 0) {
        System.out.println("No serializer serializerConfig parameters are required.");
      } else {
        System.out.println("Serializer serializerConfig:");
        for (Parameter p : serializerConfig.values()) {
          System.out.println("\t" + p.getName() + " = " + p.getValue());
        }
      }
    }
    serializer.configure(serializerConfig, schema);

    // serialize
    final File dir = (inputFile.getParentFile() != null? inputFile.getParentFile()
                      : new File("."));
    serializer.serialize(
      Arrays.spliterator(graphs), getLayersToSerialize(),
      stream -> {
        try {
          stream.save(dir);
        } catch(IOException exception) {
          System.err.println(exception.toString());
        }
      },
      warning -> { System.out.println(inputFile.getName() + ": " + warning); },
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

    // allow extra command-line switches - they might be for configuring serializations
    extraSwitches = new HashMap<String,String>();
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

    files.setToolTipText("Drag/drop files to convert here");
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
            if (dtde.getTransferable().isDataFlavorSupported(
                  java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
              dtde.acceptDrop(dtde.getDropAction());
              List droppedFiles = (java.util.List) dtde.getTransferable()
                .getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
              ListIterator f = droppedFiles.listIterator();
              while(f.hasNext()) {
                File file = (File)f.next();
                if (file.exists()) {
                  if (fileFilter.accept(file)) {
                    if (verbose) System.out.println("Adding dropped file: " + file.getPath());
                    ((DefaultListModel)files.getModel()).add(files.getModel().getSize(), file);
                  } else {
                    if (verbose) System.out.println("Dropped file incorrect type: " + file.getPath());
                  }
                } else {
                  if (verbose) System.out.println("Dropped file doesn't exist: " + file.getPath());
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
    if (getHelp()) {
      System.err.println(getClass().getSimpleName() + " ("+(v==null?"version unknown":v)+")");
      // display info about serialization parameters
      Schema schema = getSchema();
      GraphDeserializer deserializer = getDeserializer();
      System.err.println();
      System.err.println("Deserializing from " + deserializer.getDescriptor());
      ParameterSet config = deserializer.configure(new ParameterSet(), schema);
      if (config.size() == 0) {
        System.err.println(" There are no configuration parameters for deserialization");
      } else {
        System.err.println(" Command-line configuration parameters for deserialization:");
        for (Parameter p : config.values()) {
          System.err.println(
            "\t--" + p.getName() + "=" + p.getType().getSimpleName() + "\t" + p.getHint());
        }
      }
      GraphSerializer serializer = getSerializer();
      System.err.println();
      System.err.println("Serializing to " + serializer.getDescriptor());
      config = serializer.configure(new ParameterSet(), schema);
      if (config.size() == 0) {
        System.err.println(" There are no configuration parameters for serialization");
      } else {
        System.err.println(" Command-line configuration parameters for serialization:");
        for (Parameter p : config.values()) {
          System.err.println(
            "\t--" + p.getName() + "=" + p.getType().getSimpleName() + "\t" + p.getHint());
        }
      }
         
      System.exit(1);
    }
         
    for (String argument: arguments) {
      if (verbose) System.out.println("argument: " + argument);
      try {
        File file = new File(argument);
        if (file.exists()) {
          if (verbose) System.out.println("file: " + file.getPath());
          ((DefaultListModel)files.getModel()).add(files.getModel().getSize(), file);
        } else {
          if (verbose) System.out.println("file doesn't exist: " + file.getPath());
        }
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
    Vector<String> errors = new Vector<String>();
    for (File inputFile: files) {
      progress.setString(inputFile.getName());
      try {
        if (!inputFile.exists()) {
          System.err.println("Input file doesn't exist: " + inputFile.getPath());
        } else {
          convert(inputFile);
        }
      } catch(SerializationException exception) {
        System.err.println(inputFile.getPath() + ": " + exception.getMessage());
        errors.add(inputFile.getName() + ": " + exception.getMessage());
      } catch(Exception exception) {
        System.err.println(inputFile.getPath() + ": " + exception.getMessage());
        errors.add(inputFile.getName() + ": " + exception.getMessage());
        exception.printStackTrace(System.err);
      }
      progress.setValue(++f);
    } // next file
    progress.setString("Finished.");
    if (batchMode) {
      System.exit(0);
    } else { // GUI
      if (errors.size() > 0) {
        // display errors 
        JOptionPane.showMessageDialog(
          this, errors.stream().collect(Collectors.joining("\n")),
          "Error", JOptionPane.ERROR_MESSAGE);       
      }
    }
  } // end of convertBatch()
   
  /**
   * Sets any parameter values that might have been specified on the the command line -
   * i.e. that are in the {@link GuiProgram#extraSwitches} map. 
   * @param parameters The parameters to configure.
   * @param schema The source of any layers specified.
   * @return The given parameter set.
   */
  public ParameterSet configureFromCommandLine(ParameterSet parameters, Schema schema) {
    for (Parameter p : parameters.values()) {
      if (extraSwitches.containsKey(p.getName())) {
        if (p.getType().equals(Layer.class)) {               
          p.setValue(schema.getLayer(extraSwitches.get(p.getName())));
        } else if (p.getType().equals(Integer.class)) {               
          p.setValue(Integer.valueOf(extraSwitches.get(p.getName())));
        } else if (p.getType().equals(Double.class)) {               
          p.setValue(Double.valueOf(extraSwitches.get(p.getName())));
        } else if (p.getType().equals(Boolean.class)) {               
          p.setValue(Boolean.valueOf(extraSwitches.get(p.getName())));
        } else {
          p.setValue(extraSwitches.get(p.getName()));
        }
      } // there is a value in extraSwitches
    } // next parameter
    return parameters;
  } // end of configureFromCommandLine()

  /**
   * Specify the value for an extra switch, which will be passed to the deserializer and
   * serializer. 
   * @param name The switch name.
   * @param value The value for the switch.
   * @return A reference to this object, so that calls can be stacked like a builder.
   */
   public Converter setSwitch(String name, String value) {
     extraSwitches.put(name, value);
     return this;
   } // end of setSwitch()

   
  private static final long serialVersionUID = -1;
} // end of class Converter
