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
package nzilbb.sql.derby;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import nzilbb.sql.ConnectionFactory;
import nzilbb.sql.mysql.MySQLTranslator;

/**
 * A factory for supplying connections to Derby file-based databases.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class DerbyConnectionFactory implements ConnectionFactory {

  // Attributes:

  /**
   * Location for the database files.
   * @see #getLocation()
   * @see #setLocation(File)
   */
  protected File location;
  /**
   * Getter for {@link #location}: Location for the database files.
   * @return Location for the database files.
   */
  public File getLocation() { return location; }
  /**
   * Setter for {@link #location}: Location for the database files.
   * @param newLocation Location for the database files.
   */
  public DerbyConnectionFactory setLocation(File newLocation) { location = newLocation; return this; }

  /**
   * Database name - default is "derby".
   * @see #getName()
   * @see #setName(String)
   */
  protected String name = "derby";
  /**
   * Getter for {@link #name}: Database name - default is "derby".
   * @return Database name.
   */
  public String getName() { return name; }
  /**
   * Setter for {@link #name}: Database name.
   * @param newName Database name.
   */
  public DerbyConnectionFactory setName(String newName) { name = newName; return this; }
   
  // Methods:
   
  /**
   * Constructor.
   * @param location Location for the database files.
   */
  public DerbyConnectionFactory(File location) {
    this.location = location;
  } // end of constructor

  /**
   * Creates a new database connection.
   * @return A connected database connection.
   * @throws SQLException If there's a problem connecting to the database.
   */
  public Connection newConnection() throws SQLException {
    if (!location.exists()) {
      throw new SQLException("Location doesn't exist: " + location.getPath());
    }
    String connectionURL = "jdbc:derby:"
      +location.getPath().replace('\\','/') +"/"+name+";create=true";
    return DriverManager.getConnection(connectionURL, null, null);
  }
   
  /**
   * Constructs an SQL translator appropriate for the type of connection made by 
   * {@link #newConnection()}. 
   * @return An SQL translator.
   */
  public MySQLTranslator newSQLTranslator() {
    return new DerbySQLTranslator();
  }

} // end of class DerbyConnectionFactory
