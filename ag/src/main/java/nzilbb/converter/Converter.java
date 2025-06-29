//
// Copyright 2020-2023 New Zealand Institute of Language, Brain and Behaviour, 
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
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import java.util.stream.Collectors;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import nzilbb.ag.*;
import nzilbb.ag.serialize.GraphDeserializer;
import nzilbb.ag.serialize.GraphSerializer;
import nzilbb.ag.serialize.SerializationException;
import nzilbb.ag.serialize.util.NamedStream;
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

  /** Errors preventing conversion, for reporting to the user. */
  protected Vector<String> errors = new Vector<String>();
  
  /** Warnings during conversion, for reporting to the user. */
  protected Vector<String> warnings = new Vector<String>();
   
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
   * File to write help information in markdown format.
   * @see #getHelpMarkDown()
   * @see #setHelpMarkDown(File)
   */
  protected File helpMarkDown;
  /**
   * Getter for {@link #helpMarkDown}: File to write help information in markdown format.
   * @return File to write help information in markdown format.
    */
  public File getHelpMarkDown() { return helpMarkDown; }
  /**
   * Setter for {@link #helpMarkDown}: File to write help information in markdown format.
   * @param newHelpMarkDown File to write help information in markdown format.
   */
  @Switch(value="File to write help information in markdown format",compulsory=false)
  public Converter setHelpMarkDown(File newHelpMarkDown) { helpMarkDown = newHelpMarkDown; return this; }
   
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

  /** General information about the converter, for displaying to the user with the --help info */
  protected String info = null;

  /** Source code URL */
  protected String sourceUrl = "https://github.com/nzilbb/ag/tree/master/converter";

  /** License URL */
  protected String licenseUrl = "https://www.gnu.org/licenses/agpl.txt";

  /** License Name */
  protected String licenseName = "GNU Affero General Public License v3.0 or later";  

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
   * Adjust the configuration of the deserializer. Implementors can adjust the
   * default configuration before it's applied. This method is invoked once for each
   * input file.
   * @param config The default configuration.
   * @return The new configuration.
   */
  public ParameterSet deserializerConfiguration(ParameterSet config) {
    return config;
  } // end of deserializerConfiguration()
  
  /**
   * Adjust the parameters for deserialization. Implementors can adjust the
   * default configuration before it's applied. This method is invoked once for each
   * input file.
   * @param parameters The default parameters.
   * @return The new configuration.
   */
  public ParameterSet deserializationParameters(ParameterSet parameters) {
    return parameters;
  } // end of deserializationParameters()
  
  /**
   * Adjust the configuration of the serializer. Implementors can adjust the
   * default configuration before it's applied. This method is invoked once for each
   * input file.
   * @param config The default configuration.
   * @return The new configuration.
   */
  public ParameterSet serializerConfiguration(ParameterSet config) {
    return config;
  } // end of serializerConfiguration()
  
  /**
   * Process the transcripts after they were deserialized, but before they're
   * serialized. Implementors can rename speakers, adjust meta-data, or change the graph
   * in any other way required before serialization. 
   * @param transcripts
   */
  public void processTranscripts(Graph[] transcripts) {
  } // end of processTranscripts()
   
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
    // ensure we can access parent, to look for media
    final File dir = (inputFile.getParentFile() != null? inputFile.getParentFile()
                      : new File("."));

    // look for media files
    String nameWithoutExtension = IO.WithoutExtension(inputFile.getName());
    Vector<MediaFile> mediaFiles = new Vector<MediaFile>();
    for (File f : dir.listFiles(new FilenameFilter() {
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

    Graph[] graphs = deserialize(streams, schema);
    
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
    // let the subclass adjust the config
    serializerConfig = serializerConfiguration(serializerConfig);
    // get setting from command line
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
    serializer.serialize(
      Arrays.spliterator(graphs), getLayersToSerialize(),
      stream -> {
        try {
          stream.save(dir);
        } catch(IOException exception) {
          System.err.println(exception.toString());
          errors.add(stream.getName() + ": " + exception.toString());
        }
      },
      warning -> {
        System.err.println(inputFile.getName() + ": " + warning);
        warnings.add(inputFile.getName() + ": " + warning);
      },
      exception -> {
        System.err.println(exception.toString());
        exception.printStackTrace(System.err);
        errors.add(inputFile.getName() + ": " + exception.toString());
      });
    
    if (verbose) System.out.println("Finished " + inputFile.getPath());
  } // end of convert()

  /**
   * Converts all files by deserializing all first, and then serializing all resulting
   * graphs at once. The default implementation uses a default schema, default settings 
   * for serializations, and serializes the "utterance" layer only. 
   * @param inputFiles
   * @throws Exception
   */
  public void convert(Vector<File> inputFiles) throws SerializationException, Exception {
    if (verbose) System.out.println("Converting " + inputFiles.size() + " files");
    if (inputFiles.size() == 0) return;

    final int fileCount = inputFiles.size() * 2;
    progress.setMaximum(fileCount);

    Schema schema = getSchema();

    Vector<Graph> allGraphs = new Vector<Graph>();
    
    for (File inputFile : inputFiles) {
      // look for media files
      String nameWithoutExtension = IO.WithoutExtension(inputFile.getName());
      Vector<MediaFile> mediaFiles = new Vector<MediaFile>();
      final File dir = (inputFile.getParentFile() != null? inputFile.getParentFile()
                        : new File("."));
      for (File f : dir.listFiles(new FilenameFilter() {
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
      
      // deserialize...
      
      Vector<NamedStream> streams = new Vector<NamedStream>();
      // add the transcript file
      streams.add(new NamedStream(inputFile));
      // ... and also any media we found
      for (MediaFile f : mediaFiles) {
        streams.add(new NamedStream(
                      new FileInputStream(f.getFile()), f.getName(), f.getMimeType()));
      }

      progress.setString(inputFile.getName());
      Graph[] graphs = deserialize(streams, schema);
      for (Graph graph : graphs) allGraphs.add(graph);
      progress.setValue(progress.getValue() + 1);
    } // next input file
    
    // serialize...

    // create serializer
    final GraphSerializer serializer = getSerializer();
    if (verbose) System.out.println("Serializing with " + serializer.getDescriptor());
      
    // configure serializer
    ParameterSet serializerConfig = serializer.configure(new ParameterSet(), schema);
    // let the subclass adjust the config
    serializerConfig = serializerConfiguration(serializerConfig);
    // get setting from command line
    configureFromCommandLine(serializerConfig, schema);
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
    final File dir = (inputFiles.elementAt(0).getParentFile() != null?
                      inputFiles.elementAt(0).getParentFile() : new File("."));
    progress.setString(inputFiles.elementAt(0).getName() + " ...");
    serializer.serialize(
      allGraphs.spliterator(), getLayersToSerialize(),
      stream -> {
        try {
          stream.save(dir);
          if (serializer.getPercentComplete() != null) {            
            progress.setValue(fileCount + ((fileCount * serializer.getPercentComplete())/100));
          }
        } catch(IOException exception) {
          System.err.println(exception.toString());
          errors.add(stream.getName() + ": " + exception.toString());
        }
      },
      warning -> {
        System.err.println(inputFiles.elementAt(0).getName() + "...: " + warning);
        warnings.add(inputFiles.elementAt(0).getName() + "...: " + warning);
      },
      exception -> {
        System.err.println(exception.toString());
        exception.printStackTrace(System.err);
        errors.add(inputFiles.elementAt(0).getName() + "...: " + exception.toString());
      });
    
    if (verbose) System.out.println("Finished " + inputFiles.elementAt(0).getPath() + "...");
  }

  /**
   * Deserializes a file to one or more graphs. The default implementation uses a default
   * schema. 
   * @param streams An input transcript and perhaps media streams.
   * @throws Exception
   */
  public Graph[] deserialize(Vector<NamedStream> streams, Schema schema)
    throws SerializationException, Exception {
    if (verbose) System.out.println("deserialize " + streams.size() + " streams");
    if (streams.size() == 0) return new Graph[0];
    
    // create deserializer
    GraphDeserializer deserializer = getDeserializer();
    if (verbose) System.out.println("Deserializing with " + deserializer.getDescriptor());

    // configure deserializer
    ParameterSet deserializerConfig = deserializer.configure(new ParameterSet(), schema);
    // let the subclass adjust the config
    deserializerConfig = deserializerConfiguration(deserializerConfig);
    // let the command line options take effect
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
    ParameterSet deserializationParameters = deserializer.load(
      streams.toArray(new NamedStream[0]), schema);
    // let the subclass adjust the parameters
    deserializationParameters = deserializationParameters(deserializationParameters);
    // let the command line adjuect the parameters
    configureFromCommandLine(deserializationParameters, schema);
    
    if (verbose) {
      if (deserializationParameters.size() == 0) {
        System.out.println("No deserialization parameters are required.");            
      } else {
        System.out.println("Deserialization parameters:");
        for (Parameter p : deserializationParameters.values()) {
          System.out.println("\t" + p.getName() + " = " + p.getValue());
        }
      }
    }

    // configure the deserialization
    deserializer.setParameters(deserializationParameters);
    
    Graph[] graphs = deserializer.deserialize();
    for (String warning : deserializer.getWarnings()) {
      System.err.println(streams.elementAt(0).getName() + ": " + warning);
      warnings.add(streams.elementAt(0).getName() + ": " + warning);
    }

    // strip extension off name
    for (Graph g : graphs) {
      g.setId(IO.WithoutExtension(g.getId()));
    }

    if (schema.getParticipantLayer() != null
        && schema.getTurnLayer() != null
        && schema.getUtteranceLayer() != null) {
      Normalizer normalizer = new Normalizer();
      for (Graph g : graphs) {
        normalizer.transform(g);
        g.commit();
      }
    }

    // let the subclass process the graphs before they're serialized
    processTranscripts(graphs);
    
    return graphs;
  }

  /**
   * Default constructor.
   */
  public Converter() {
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

    JMenuBar menuBar = new JMenuBar();
    frame_.setJMenuBar(menuBar);
    JMenu helpMenu = new JMenu("Help");
    menuBar.add(helpMenu);
    helpMenu.setMnemonic(KeyEvent.VK_H);
    JMenuItem about = new JMenuItem("About");
    helpMenu.add(about);
    about.setMnemonic(KeyEvent.VK_A);
    JMenuItem info = new JMenuItem("Information");
    helpMenu.add(info);
    info.setMnemonic(KeyEvent.VK_I);

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

    info.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // create a JTextArea
          JTextArea textArea = new JTextArea();
          textArea.setText(help());
          textArea.setCaretPosition(0);
          textArea.setEditable(false);
          JDialog dlg = new JOptionPane(new JScrollPane(textArea)).createDialog(
            frame_, frame_.getTitle());
          dlg.setSize(frame_.getSize());
          dlg.setLocation(frame_.getLocation());
          dlg.setVisible(true);
        }
      });

    about.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          String info = getClass().getPackage().getImplementationTitle()
            + " Version: " + getClass().getPackage().getImplementationVersion()
            + "\nInput: " + getDeserializer().getDescriptor()
            + "\nOutput: " + getSerializer().getDescriptor();
          
          // print version info
          System.out.println(info);

          // copy version info to clipboard
          StringSelection selection = new StringSelection(info);
          Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);

          // display version info
          JPanel aboutPanel = new JPanel(new java.awt.GridLayout(11, 1, 3, 3));
          aboutPanel.add(new JLabel(getClass().getPackage().getImplementationTitle(),
                                    SwingConstants.CENTER));
          aboutPanel.add(new JLabel(
                           "Version: " + getClass().getPackage().getImplementationVersion(),
                           SwingConstants.CENTER));
          aboutPanel.add(new JLabel("Input: " + getDeserializer().getDescriptor(),
                                    SwingConstants.CENTER));
          aboutPanel.add(new JLabel("Output: " + getSerializer().getDescriptor(),
                                    SwingConstants.CENTER));
          JLabel clipboardMessage = new JLabel(
            "(Version information has been copied to the clipboard)", SwingConstants.CENTER);
          clipboardMessage.setForeground(java.awt.Color.GRAY);
          aboutPanel.add(clipboardMessage);
          aboutPanel.add(new JLabel("Developed by:",
                                    SwingConstants.CENTER));
          aboutPanel.add(new JLabel(getClass().getPackage().getImplementationVendor(),
                                    SwingConstants.CENTER));
          aboutPanel.add(new JLabel("This is open source software:", SwingConstants.CENTER));
          aboutPanel.add(linkComponent(licenseName, licenseUrl));
          aboutPanel.add(new JLabel("Source Code:", SwingConstants.CENTER));
          aboutPanel.add(linkComponent(sourceUrl, sourceUrl));
          JDialog dlg = new JOptionPane(aboutPanel).createDialog(frame_, "About");
          dlg.setLocationRelativeTo(frame_);
          dlg.setVisible(true);
        }
      });

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
  
  /**
   * Creates a visual component that looks and works like a hyperlink.
   * @param label
   * @param url
   * @return A component that looks and works like a hyperlink.
   */
  protected Component linkComponent(String label, String url) {
    JLabel linkLabel = new JLabel(label, SwingConstants.CENTER);
    linkLabel.setForeground(java.awt.Color.BLUE);
    linkLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    linkLabel.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent e) {
          try {
            java.awt.Desktop.getDesktop().browse(new URI(url));
          } catch(Exception exception) {}
        }});
    return linkLabel;
  } // end of linkComponent()

  @SuppressWarnings("unchecked")
  public void start() {
      
    if (arguments.size() == 0 && !getHelp() && getHelpMarkDown() == null) {
      System.err.println("Nothing to do yet. (Try using --usage command line switch)");
    }
    if (getHelp()) {
      System.err.println(help());
      System.exit(1);
    }
    if (getHelpMarkDown() != null) {
      try (PrintStream md = new PrintStream(getHelpMarkDown())) {
        helpMarkdown(md);
        System.err.println("Wrote documentation to " + getHelpMarkDown().getPath());
      } catch (Exception x) {
        System.err.println("ERROR writing " + getHelpMarkDown().getPath());
        x.printStackTrace(System.err);
      }
      System.exit(0);
    }

    FileNameExtensionFilter fileFilter = getFileFilter();
    for (String argument: arguments) {
      if (verbose) System.out.println("argument: " + argument);
      try {
        File file = new File(argument);
        if (file.exists() && !file.isDirectory() && fileFilter.accept(file)) {
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
   * Generates help information.
   * @return The help information for this converter, including general info, and info
   * about the deserializer and serializer. 
   */
  public String help() {
    StringBuilder helpInfo = new StringBuilder();
    helpInfo.append(getClass().getSimpleName() + " ("+(v==null?"version unknown":v)+")");
    helpInfo.append("\n");
    @SuppressWarnings("unchecked")
      ProgramDescription myAnnotation 
      = (ProgramDescription)getClass().getAnnotation(ProgramDescription.class);
    if (myAnnotation != null) {
      helpInfo.append("\n");
      helpInfo.append(myAnnotation.value());
      helpInfo.append("\n");
    }
    // display general info, if there is any
    if (info != null) {
      helpInfo.append("\n");
      helpInfo.append(wrap(info));
      helpInfo.append("\n");
    }
    // display info about serialization parameters
    Schema schema = getSchema();
    GraphDeserializer deserializer = getDeserializer();
    helpInfo.append("\nDeserializing from " + deserializer.getDescriptor());
    helpInfo.append("\n");
    ParameterSet config = deserializer.configure(new ParameterSet(), schema);
    if (config.size() == 0) {
      helpInfo.append(" There are no configuration parameters for deserialization\n");      
    } else {
      helpInfo.append(" Command-line configuration parameters for deserialization:\n");
      for (Parameter p : config.values()) {
        helpInfo.append(
          wrap("\t--" + p.getName() + "=" + p.getType().getSimpleName() + "\t" + p.getHint()));
        helpInfo.append("\n");
      }
    }
    GraphSerializer serializer = getSerializer();
    helpInfo.append("\nSerializing to " + serializer.getDescriptor());
    helpInfo.append("\n");
    config = serializer.configure(new ParameterSet(), schema);
    if (config.size() == 0) {
      helpInfo.append(" There are no configuration parameters for serialization\n");
    } else {
      helpInfo.append(" Command-line configuration parameters for serialization:\n");
      for (Parameter p : config.values()) {
        helpInfo.append(
          wrap("\t--" + p.getName() + "=" + p.getType().getSimpleName() + "\t" + p.getHint()));
        helpInfo.append("\n");
      }
    }
    return helpInfo.toString();
  } // end of help()
   
  /**
   * Writes help information for this converter, including general info, and info
   * about the deserializer and serializer information in markdown format,
   * output to the given print stream.
   */
  public void helpMarkdown(PrintStream md) {    
    md.println("# " + getClass().getSimpleName() + (v==null?"":"("+v+")"));
    @SuppressWarnings("unchecked")
      ProgramDescription myAnnotation 
      = (ProgramDescription)getClass().getAnnotation(ProgramDescription.class);
    if (myAnnotation != null) {
      md.println();
      md.println(myAnnotation.value());
    }
    // display general info, if there is any
    if (info != null) {
      md.println();
      md.println(info.replaceAll("\n[^- ]", "\n$0"));
    }
    // display info about serialization parameters
    Schema schema = getSchema();
    md.println();
    GraphDeserializer deserializer = getDeserializer();
    md.println("## Deserializing from " + deserializer.getDescriptor());
    md.println();
    ParameterSet config = deserializer.configure(new ParameterSet(), schema);
    if (config.size() == 0) {
      md.println("There are no configuration parameters for deserialization.");
    } else {
      md.println("Command-line configuration parameters for deserialization:");
      md.println();
      md.println("|   |   |"); // markdown table
      md.println("|:--|:--|");
      for (Parameter p : config.values()) {
        md.println("| `--"+p.getName()+"=`*" + p.getType().getSimpleName()+"* | "+p.getHint()+" |");
      }
    }
    GraphSerializer serializer = getSerializer();
    md.println();
    md.println("## Serializing to " + serializer.getDescriptor());
    md.println();
    config = serializer.configure(new ParameterSet(), schema);
    if (config.size() == 0) {
      md.println("There are no configuration parameters for serialization.");
    } else {
      md.println("Command-line configuration parameters for serialization:");
      md.println();
      md.println("|   |   |"); // markdown table
      md.println("|:--|:--|");
      for (Parameter p : config.values()) {
        md.println("| `--"+p.getName()+"=`*" + p.getType().getSimpleName()+"* | "+p.getHint()+" |");
      }
    }
  } // end of helpMarkdown()
   
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
    errors = new Vector<String>();
    warnings = new Vector<String>();
    // if the cardinality is NToN (i.e. 1 input files produces 1 output file), do them one by one
    if (getSerializer().getCardinality() == GraphSerializer.Cardinality.NToN) {
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
    } else { // not NToN, so we have to process them all at once
      try {
        convert(files);
      } catch(SerializationException exception) {
        System.err.println(exception.getMessage());
        errors.add(exception.getMessage());
      } catch(Exception exception) {
        System.err.println(exception.toString());
        errors.add(exception.getMessage());
        exception.printStackTrace(System.err);
      }
    }
    progress.setString("Finished.");
    if (batchMode) {
      System.exit(0);
    } else { // GUI
      if (warnings.size() > 0) {
        // display warnings
        if (warnings.size() < 10) {
          // message dialog is fine
          JOptionPane.showMessageDialog(
            this, warnings.stream().collect(Collectors.joining("\n")),
            "Warning", JOptionPane.INFORMATION_MESSAGE);
        } else { // too many messages for a message dialog
          // create a scrollable text box
          JTextArea textArea = new JTextArea();
          textArea.setText(warnings.stream().collect(Collectors.joining("\n")));
          textArea.setCaretPosition(0);
          textArea.setEditable(false);
          JDialog dlg = new JOptionPane(
            new JScrollPane(textArea)).createDialog(frame_, "Warning");
          dlg.setSize(frame_.getSize());
          dlg.setLocation(frame_.getLocation());
          dlg.setVisible(true);
        }
      }
      if (errors.size() > 0) {
        // display errors 
        if (warnings.size() < 10) {
          // message dialog is fine
          JOptionPane.showMessageDialog(
            this, errors.stream().collect(Collectors.joining("\n")),
            "Error", JOptionPane.ERROR_MESSAGE);       
        } else { // too many messages for a message dialog
          // create a scrollable text box
          JTextArea textArea = new JTextArea();
          textArea.setText(errors.stream().collect(Collectors.joining("\n")));
          textArea.setCaretPosition(0);
          textArea.setEditable(false);
          JDialog dlg = new JOptionPane(
            new JScrollPane(textArea)).createDialog(frame_, "Warning");
          dlg.setSize(frame_.getSize());
          dlg.setLocation(frame_.getLocation());
          dlg.setVisible(true);
        }
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
