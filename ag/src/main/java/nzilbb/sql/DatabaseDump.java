//
// Copyright 2018-2026 New Zealand Institute of Language, Brain and Behaviour, 
// University of Canterbury
// Written by Robert Fromont - robert.fromont@canterbury.ac.nz
//
//    This file is part of LaBB-CAT.
//
//    LaBB-CAT is free software; you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation; either version 2 of the License, or
//    (at your option) any later version.
//
//    LaBB-CAT is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with LaBB-CAT; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package nzilbb.sql;

import java.sql.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.function.Consumer;
import java.text.SimpleDateFormat;
import nzilbb.util.CommandLineProgram;
import nzilbb.util.MonitorableTask;
import nzilbb.util.ProgramDescription;
import nzilbb.util.Switch;

/**
 * Java implementation of mysqldump - i.e. a utility to backup a whole database into
 * a file, including CREATE statements to set up databse schema, and INSERT statements
 * to insert data.
 * @author Robert Fromont robert@fromont.net.nz
 */
@ProgramDescription("Back up a MySQL database to stdout")
public class DatabaseDump extends CommandLineProgram implements MonitorableTask {
  
  private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
  private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
  private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
  
  /**
   * JDBC connector class name. Defaults to "com.mysql.cj.jdbc.Driver".
   * @see #getConnectorClass()
   * @see #setConnectorClass(String)
   */
  protected String connectorClass = "com.mysql.cj.jdbc.Driver";
  /**
   * Getter for {@link #connectorClass}: JDBC connector class name.
   * @return JDBC connector class name.
   */
  public String getConnectorClass() { return connectorClass; }
  /**
   * Setter for {@link #connectorClass}: JDBC connector class name.
   * @param newConnectorClass JDBC connector class name.
   */
  @Switch("JDBC connector class name (defaults to com.mysql.cj.jdbc.Driver)")
  public DatabaseDump setConnectorClass(String newConnectorClass) {
    connectorClass = newConnectorClass; return this; }
  
  /**
   * Connect string.
   * @see #getConnectString()
   * @see #setConnectString(String)
   */
  protected String connectString;
  /**
   * Getter for {@link #connectString}: Connect string.
   * @return Connect string.
   */
  public String getConnectString() { return connectString; }
  /**
   * Setter for {@link #connectString}: Connect string.
   * @param newConnectString Connect string.
   */
  @Switch(value="Connect string",compulsory=true)
  public DatabaseDump setConnectString(String newConnectString) {
    connectString = newConnectString; return this; }
  
  /**
   * Database username.
   * @see #getUsername()
   * @see #setUsername(String)
   */
   protected String username;
  /**
   * Getter for {@link #username}: Database username.
   * @return Database username.
   */
  public String getUsername() { return username; }
  /**
   * Setter for {@link #username}: Database username.
    * @param newUsername Database username.
    */
  @Switch(value="Database username",compulsory=true)
  public DatabaseDump setUsername(String newUsername) {
    username = newUsername; return this; }

  /**
   * Database password.
   * @see #getPassword()
   * @see #setPassword(String)
   */
  protected String password;
  /**
   * Getter for {@link #password}: Database password.
   * @return Database password.
   */
  public String getPassword() { return password; }
  /**
   * Setter for {@link #password}: Database password.
   * @param newPassword Database password.
    */
  @Switch(value="Database password",compulsory=true)
  public DatabaseDump setPassword(String newPassword) {
    password = newPassword; return this; }
  
  /**
   * Maximum number of rows per INSERT statement. Default is 50.
   * @see #getMaxRowsPerInsert()
   * @see #setMaxRowsPerInsert(Integer)
   */
  protected Integer maxRowsPerInsert = 50;
  /**
   * Getter for {@link #maxRowsPerInsert}: Maximum number of rows per INSERT statement.
   * Default is 50.
   * @return Maximum number of rows per INSERT statement.
   */
  public Integer getMaxRowsPerInsert() { return maxRowsPerInsert; }
  /**
   * Setter for {@link #maxRowsPerInsert}: Maximum number of rows per INSERT statement.
   * @param newMaxRowsPerInsert Maximum number of rows per INSERT statement.
   */
  @Switch("Maximum number of rows per INSERT statement. Default is 50.")
  public DatabaseDump setMaxRowsPerInsert(Integer newMaxRowsPerInsert) {
    maxRowsPerInsert = newMaxRowsPerInsert; return this; }

  /**
   * Whether to log verbose messages to stderr or not.
   * @see #getVerbose()
   * @see #setVerbose(Boolean)
   */
  protected Boolean verbose = Boolean.FALSE;
  /**
   * Getter for {@link #verbose}: Whether to log verbose messages to stderr or not.
   * @return Whether to log verbose messages to stderr or not.
   */
  public Boolean getVerbose() { return verbose; }
  /**
   * Setter for {@link #verbose}: Whether to log verbose messages to stderr or not.
   * @param newVerbose Whether to log verbose messages to stderr or not.
   */
  @Switch("Log messages to stderr")
  public DatabaseDump setVerbose(Boolean newVerbose) {
    verbose = newVerbose; return this; }
   
  /**
   * Name of the table currently being exported.
   * @see #getCurrentTable()
   * @see #setCurrentTable(String)
   */
  protected String currentTable;
  /**
   * Getter for {@link #currentTable}: Name of the table currently being exported.
   * @return Name of the table currently being exported.
   */
  public String getCurrentTable() { return currentTable; }
  /**
   * Setter for {@link #currentTable}: Name of the table currently being exported.
   * @param newCurrentTable Name of the table currently being exported.
   */
  public DatabaseDump setCurrentTable(String newCurrentTable) {
    currentTable = newCurrentTable;
    for (Consumer<String> observer : currentTableObservers) observer.accept(currentTable);
    return this;
  }

  /**
   * Listeners for change in currentTable.
   * @see #getCurrentTableObservers()
   * @see #setCurrentTableObservers(List)
   */
  protected List<Consumer<String>> currentTableObservers = new Vector<Consumer<String>>();
  /**
   * Getter for {@link #currentTableObservers}: Listeners for change in currentTable.
   * @return Listeners for change in currentTable.
   */
  public List<Consumer<String>> getCurrentTableObservers() { return currentTableObservers; }
  
  /**
   * Percent complete.
   * @see #getPercentComplete()
   * @see #setPercentComplete(int)
   */
  protected int percentComplete = 0;
  /**
   * Getter for {@link #percentComplete}: Percent complete.
   * @return Percent complete.
   */
  public Integer getPercentComplete() { return percentComplete; }
  /**
   * Setter for {@link #percentComplete}: Percent complete.
   * @param newPercentComplete Percent complete.
   */
  public DatabaseDump setPercentComplete(Integer newPercentComplete) {
    percentComplete = newPercentComplete;
    for (Consumer<Integer> observer : progressObservers) {
      observer.accept(newPercentComplete);
    }
    return this;
  }
  
  /**
   * Listeners for change in percentComplete.
   * @see #getProgressObservers()
   * @see #setProgressObservers(List)
   */
  protected List<Consumer<Integer>> progressObservers = new Vector<Consumer<Integer>>();
  /**
   * Getter for {@link #progressObservers}: Listeners for change in percentComplete.
   * @return Listeners for change in percentComplete.
   */
  public List<Consumer<Integer>> getProgressObservers() { return progressObservers; }
  
  /**
   * Is currently running.
   * @see #getRunning()
   * @see #setRunning(boolean)
   */
  protected boolean running = false;
  /**
   * Getter for {@link #running}: Is currently running.
   * @return Is currently running.
   */
  public boolean getRunning() { return running; }
  
  /**
   * Default constructor.
   */
  public DatabaseDump() {
  } // end of constructor

  boolean isCancelling = false;
  /** Cancels the task. */
  public void cancel() {
    isCancelling = true;
  }

  /**
   * Attribute constructor.
   * @param connectorClass JDBC connector class name.
   * @param connectString Connect string.
   * @param username Database username.
   * @param password Database password.
   */
  public DatabaseDump(
    String connectorClass, String connectString, String username, String password) {
    setConnectorClass(connectorClass);
    setConnectString(connectString);
    setUsername(username);
    setPassword(password);
  } // end of constructor
  
  /**
   * Command line main entrypoint.
   */
  public static void main(String argv[]) {
    DatabaseDump application = new DatabaseDump();
    if (application.processArguments(argv)) {
      application.start();
    }
  }
  
  /**
   * Start execution.
   */
  public void start() {
    try {
      dump(System.out);
    } catch(Throwable t) {
      System.err.println(t.toString());
      t.printStackTrace(System.err);
    }
  }
  
  /**
   * Dumps the given database into the given stream.
   * @param stream Print stream for receiving the dump.
   * @throws SQLException If a database error occurs.
   * @throws IOException If an IO error occurs.
   */
  public void dump(PrintStream stream) throws SQLException, IOException {
    setPercentComplete(0);
    setCurrentTable(null);
    running = true;
    
    try
    {
      stream.println("/* "+getConnectString()+" */");
      stream.println("/* "+dateTimeFormat.format(new java.util.Date())+" */");
      
      if (verbose) System.err.println("Connecting to database...");
      try { Class.forName(getConnectorClass()).newInstance(); }
      catch (Throwable t) { throw new SQLException(t); }
      Connection db = DriverManager.getConnection(
        getConnectString(), getUsername(), getPassword());
      
      if (verbose) System.err.println("Listing tables...");
      Vector<String> tables = new Vector<String>();
      try (PreparedStatement sql = db.prepareStatement("SHOW TABLES")) {
        try (ResultSet rs = sql.executeQuery()) {
          while (rs.next()) tables.add(rs.getString(1));
        } // rs.close();
      } //sql.close();
      if (verbose) System.err.println("There are "+tables.size()+" tables.");
      
      stream.println("/* "+tables.size()+" tables */");
      
      int soFar = 0;
      for (String table : tables) {
        if (isCancelling) break;
        setCurrentTable(table);
        if (verbose) System.err.println(table + " " + getPercentComplete() + "%");
        stream.println();
        stream.println("/* "+table+" */");
        
        // table creation...
        stream.println("DROP TABLE IF EXISTS `"+table+"`;");
        try (PreparedStatement sql = db.prepareStatement("SHOW CREATE TABLE `"+table+"`")) {
          try (ResultSet rs = sql.executeQuery()) {
            rs.next();
            stream.println(rs.getString(2) + ";");
          } // rs.close();
        } // sql.close();
        
        // data...
        int row = 0;
        int columnCount = 1;
        try (PreparedStatement sql = db.prepareStatement("SELECT * FROM `"+table+"`")) {
          try (ResultSet rs = sql.executeQuery()) {
            while (rs.next()) {
              if (isCancelling) break;
              if (row % getMaxRowsPerInsert() == 0) {
                if (row > 0) stream.println(";");
            
                stream.println("INSERT INTO `"+table+"` VALUES");
                
                // get the number of columns
                ResultSetMetaData meta = rs.getMetaData();
                columnCount = meta.getColumnCount();
              } else {
                stream.println(",");
              }
              stream.print("(");
              for (int c = 1; c <= columnCount; c++) {
                if (c > 1) stream.print(",");
                stream.print(escape(rs.getObject(c)));
              } // next column
              stream.print(")");
              row++;
            } // next row
            stream.println(";");
          } // rs.close();
        } // sql.close();	 
        
        setPercentComplete(100 * ++soFar / tables.size());	 
      } // next table
      setCurrentTable(null);
      
      if (verbose) System.err.println("Finished.");
      setPercentComplete(100);
    } finally {
      running = false;
    }
  } // end of dump()

  /**
   * Dumps the given database into the given file.
   * @param file File for receiving the dump.
   * @throws SQLException If a database error occurs.
   * @throws IOException If an IO error occurs.
   */
  public void dump(File file) throws SQLException, IOException {
    PrintStream stream = new PrintStream(file, "UTF-8");
    try {
      running = true;
      dump(stream);
    } finally {
      running = false;
    }
  } // end of dump()
  
  /**
   * SQL-escapes and encloses in quotes the given object.
   * @param o The object to express.
   * @return The given string, escaped and in quotes, or "NULL" if the object is null.
   */
  public String escape(Object o) {
    if (o == null) return "NULL";
    if (o instanceof String)
      return "'"+((String)o).replace("'","''").replace("\\","\\\\")+"'";
    if (o instanceof Integer) return o.toString();
    if (o instanceof Long) return o.toString();
    if (o instanceof Float) return o.toString();
    if (o instanceof Double) return o.toString();
    if (o instanceof Boolean) return ""+((Boolean)o?1:0);
    return "'"+o.toString().replace("'","''").replace("\\","\\\\")+"'";
  } // end of escape()
  
} // end of class DatabaseDump
