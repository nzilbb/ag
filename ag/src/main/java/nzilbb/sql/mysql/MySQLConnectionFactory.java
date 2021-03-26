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
package nzilbb.sql.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.DriverManager;
import nzilbb.sql.ConnectionFactory;

/**
 * A factory for supplying connections to MySQL databases.
 * @author Robert Fromont robert@fromont.net.nz
 */
public class MySQLConnectionFactory implements ConnectionFactory {

   // Attributes:
   
   /**
    * Connection URL.
    * @see #getConnectionUrl()
    * @see #setConnectionUrl(String)
    */
   protected String connectionUrl;
   /**
    * Getter for {@link #connectionUrl}: Connection URL.
    * @return Connection URL.
    */
   public String getConnectionUrl() { return connectionUrl; }
   /**
    * Setter for {@link #connectionUrl}: Connection URL.
    * @param newConnectionUrl Connection URL.
    */
   public MySQLConnectionFactory setConnectionUrl(String newConnectionUrl) { connectionUrl = newConnectionUrl; return this; }

   /**
    * Database user ID.
    * @see #getConnectionName()
    * @see #setConnectionName(String)
    */
   protected String connectionName;
   /**
    * Getter for {@link #connectionName}: Database user ID.
    * @return Database user ID.
    */
   public String getConnectionName() { return connectionName; }
   /**
    * Setter for {@link #connectionName}: Database user ID.
    * @param newConnectionName Database user ID.
    */
   public MySQLConnectionFactory setConnectionName(String newConnectionName) { connectionName = newConnectionName; return this; }

   /**
    * Database password.
    * @see #getConnectionPassword()
    * @see #setConnectionPassword(String)
    */
   protected String connectionPassword;
   /**
    * Getter for {@link #connectionPassword}: Database password.
    * @return Database password.
    */
   public String getConnectionPassword() { return connectionPassword; }
   /**
    * Setter for {@link #connectionPassword}: Database password.
    * @param newConnectionPassword Database password.
    */
   public MySQLConnectionFactory setConnectionPassword(String newConnectionPassword) { connectionPassword = newConnectionPassword; return this; }
   
   // Methods:
   
   /**
    * Constructor.
    * @param connectionUrl Connection URL.
    * @param connectionName Database user ID.
    * @param connectionPassword Database password.
    */
   public MySQLConnectionFactory(
      String connectionUrl, String connectionName, String connectionPassword) {
      this.connectionUrl = connectionUrl;
      this.connectionName = connectionName;
      this.connectionPassword = connectionPassword;
   } // end of constructor

   /**
    * Creates a new database connection.
    * @return A connected database connection.
    * @throws SQLException If there's a problem connecting to the database.
    */
   public Connection newConnection() throws SQLException {
      return DriverManager.getConnection(connectionUrl, connectionName, connectionPassword);
   }
   
   /**
    * Constructs an SQL translator appropriate for the type of connection made by 
    * {@link #newConnection()}. 
    * @return An SQL translator.
    */
   public MySQLTranslator newSQLTranslator() {
      return new MySQLTranslator();
   }

} // end of class MySQLConnectionFactory
